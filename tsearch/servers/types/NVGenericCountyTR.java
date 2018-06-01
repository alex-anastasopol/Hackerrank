package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author vladb
 *
 */
public class NVGenericCountyTR extends TSServer {

	private static final long serialVersionUID = 1L;

	public NVGenericCountyTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_PARCEL:	
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}

				StringBuilder footer = new StringBuilder();
				String nextLink = processFooter(rsResponse, footer);
				parsedResponse.setNextLink(nextLink);
				parsedResponse.setFooter(footer.toString());
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				if (rsResponse.indexOf("Secured Tax Inquiry Detail for Parcel") > -1) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				
				if(viParseID == ID_DETAILS) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					if (isInstrumentSaved(accountId.toString().trim(), null, data)){
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parsedResponse.setResponse(details);
				} else {
					String filename = accountId + ".html";
					smartParseDetails(Response,details);
					
					msSaveToTSDFileName = filename;
					parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					parsedResponse.setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
				
				break;
		}
	}

	private String getDetails(String rsResponse, StringBuilder accountId) {
		
		StringBuilder details = new StringBuilder();
		
		Matcher m = Pattern.compile("Parcel\\s*#\\s*(([0-9A-Z]|-)+)").matcher(rsResponse);
		if(m.find()) {
			accountId.append(m.group(1));
		}
		if(rsResponse.toLowerCase().indexOf("<html") < 0) { // page from memory
			return rsResponse;
		}
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag mainTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"), false)
				.elementAt(0);
			mainTable.setAttribute("id", "mainTable");
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			for(int i = 0; i < tables.size(); i++) {
				TableTag table = (TableTag) tables.elementAt(i);
				table.setAttribute("border", "1");
			}
			details.append(mainTable.toHtml().replaceAll("(?is)<input[^>]*>", "") // do some cleaning
					.replaceAll("(?is)<form[^>]*?name=\"History\"[^>]*>.*?</form>", ""));
			
			//get payment history
			Form form = new SimpleHtmlParser(mainTable.toHtml()).getForm("History");
			Map<String, String> params = form.getParams();
			
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
					HashCountyToIndex.ANY_COMMUNITY, miServerID);
			String siteLink = dataSite.getLink();
        	
			String history = requestPage(siteLink + form.action, params);
        	
			// get first history page
			form = new SimpleHtmlParser(history).getForm("SetTxYr");
			params = form.getParams();
			params.put("sbegyr", "1900");

			history = requestPage(siteLink + form.action, params);
			
			htmlParser = org.htmlparser.Parser.createParser(history, null);
			nodeList = htmlParser.parse(null);
			
			TableTag historyTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("name", "Buttons"), true)
				.elementAt(0).getParent().getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), false)
				.elementAt(1);
			
			NodeList allRows = new NodeList();
			TableRow[] rows = historyTable.getRows();
			for(TableRow row : rows) {
				String rowText = row.toPlainTextString().toLowerCase();
				if(rowText.indexOf("more...") < 0 && rowText.indexOf("bottom") < 0) {
					allRows.add(row);
				}
			}
			
			// get the other history pages
			while(historyTable.toPlainTextString().indexOf("More...") > -1) {
				form = new SimpleHtmlParser(history).getForm("Buttons");
				params = form.getParams();
				params.put("CGIOption", "Page Down");
				
				history = requestPage(siteLink + form.action, params);
				
				htmlParser = org.htmlparser.Parser.createParser(history, null);
				nodeList = htmlParser.parse(null);
				
				historyTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("name", "Buttons"), true)
					.elementAt(0).getParent().getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), false)
					.elementAt(1);
				
				rows = historyTable.getRows();
				for(TableRow row : rows) {
					String rowText = row.toPlainTextString().toLowerCase();
					if(rowText.indexOf("more...") < 0 && rowText.indexOf("bottom") < 0 && rowText.indexOf("yr/typ") < 0) {
						allRows.add(row);
					}
				}
			}
			
			details.append("<table id=\"taxHistTable\" border=\"1\" align=\"center\">");
			details.append("<tr><th colspan=\"5\">Secured Tax Billing & Payment History</th></tr>");
			details.append(allRows.toHtml());
			details.append("</table>");
			
		} catch (Exception e) {
			logger.error("ERROR while getting details");
		}
		
		return details.toString();
	}
	
	private String requestPage(String siteLink, Map<String, String> params) {
		
		HTTPRequest reqP = new HTTPRequest(siteLink);
    	reqP.setMethod(HTTPRequest.POST);
    	for(Map.Entry<String, String> entry : params.entrySet()) {
    		reqP.setPostParameter(entry.getKey(), entry.getValue());
    	}
    	
    	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
    	String page = null;
    	try {
    		HTTPResponse resP = site.process(reqP);
    		page = resP.getResponseAsString();
    	} finally {
			HttpManager.releaseSite(site);
		}
    	
    	return page;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = extractIntermediaryTable(nodeList);
			
			if(interTable == null) {
				return intermediaryResponse;
			}
			
			TableRow[] rows = interTable.getRows();
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				String rowText = row.toPlainTextString().toLowerCase().replaceAll("(?i)&nbsp;", " ");
				
				// skip unuseful rows
				if(rowText.indexOf("search results") > -1
						|| rowText.indexOf("prior year taxes owed") > -1
						|| StringUtils.isEmpty(rowText)) {
					
					continue;
				}
				// parse header
				if(rowText.indexOf("parcel #") > -1) {
					parsedResponse.setHeader("<table border=\"1\" width=\"80%\" align=\"center\">" + 
							row.toHtml().replaceAll("(?is)<td", "<th").replaceAll("(?is)</td", "</th").replaceAll("(?is)<br>", " "));
					continue;
				}
				// process links
				StringBuilder rowHtml = new StringBuilder();
				String link = processIntermediaryLink(row, rowHtml);
				
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml.toString());
				currentResponse.setOnlyResponse(rowHtml.toString());
				currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap m = parseSpecificIntermediaryRow(row);
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				
				DocumentI document = (TaxDocumentI)bridge.importData();				
				currentResponse.setDocument(document);
				
				intermediaryResponse.add(currentResponse);
			}
			
			parsedResponse.setFooter("</table>");
			outputTable.append(table);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		return ro.cst.tsearch.servers.functions.NVGenericCountyTR.parseAndFillResultMap(response, detailsHtml, resultMap);
	}
	
	protected ResultMap parseSpecificIntermediaryRow(TableRow row) {
		// default parsing
		return ro.cst.tsearch.servers.functions.NVGenericCountyTR.parseIntermediaryRow(row);
	}

	protected String processFooter(String rsResponse, StringBuilder footer) {
		// implement me in the derived class
		return null;
	}
	
	protected String processIntermediaryLink(TableRow row, StringBuilder rowHtml) {
		
		LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
		if(linkTag == null) {
			return null;
		}
		String link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.extractLink().trim().replaceAll("\\s", "%20");
		linkTag.setLink(link);
		rowHtml.append(row.toHtml());
		
		return link;
	}

	protected TableTag extractIntermediaryTable(NodeList nodeList) {
		// implement me in the derived class
		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		
		// search by account number
		if(hasPin()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO).replaceAll("-", "");
			module.getFunction(0).forceValue(pin);
			module.addFilter(pinFilter);
			modules.add(module);
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L, F M;;","L, F;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}

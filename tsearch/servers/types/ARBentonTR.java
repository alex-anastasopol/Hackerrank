
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
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
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
public class ARBentonTR extends TSServer {

	private static final long serialVersionUID = 1L;
	private static final String FORM_NAME = "aspnetForm";
	private static int seq = 0;
	
	protected synchronized static int getSeq(){
		return seq++;
	}

	public ARBentonTR(String rsRequestSolverName, String rsSitePath,
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
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_SUBDIVISION_NAME:
				
				// no result
				if (rsResponse.indexOf("No data was found") > -1) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}

				parsedResponse.setFooter("</table>");
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				if (rsResponse.indexOf("Tax Summary") > -1) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder accountId = new StringBuilder();
				StringBuilder year = new StringBuilder();
				String details = getDetails(rsResponse, accountId, year);
				
				if(viParseID == ID_DETAILS) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					data.put("year", year.toString());
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
	
	@SuppressWarnings("unchecked")
	private String getDetails(String detailsPage, StringBuilder accountId, StringBuilder year) {

		StringBuilder details = new StringBuilder();
		
		try {
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(detailsPage, null);
			NodeList nodeList = parser.parse(null);
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), false);
			
			for(int i = 0; i < tables.size(); i++) {
				TableTag table = (TableTag)tables.elementAt(i);
				String tableText = table.toPlainTextString();
				String subName = "";
				if(tableText.indexOf("Parcel information") > -1) {
					Matcher m = Pattern.compile("(?is).*?([\\d-]+)\\s+(\\d{4}).*").matcher(tableText);
					if(m.find()) {
						accountId.append(m.group(1));
						year.append(m.group(2));
					}
					if(detailsPage.toLowerCase().indexOf("<html") < 0) { // if from memory
						return detailsPage;
					}
					Map<String, String> subdivisions = (Map<String, String>) mSearch.getAdditionalInfo(getCurrentServerName() + ":subdivisions:");
					subName = subdivisions.get(accountId.toString());
				}
				details.append(table.toHtml().replaceAll("(?is)<div[^>]*>", "<div>")
						.replaceAll("(?is)(onclick|onmouseout|onmouseover)=\"[^\"]*\"", "")
						.replaceAll("(?is)<input[^>]*>", "")); // perform some cleaning
				if(StringUtils.isNotEmpty(subName)) {
					details.append("<p>Subdivision: " + subName + "</p>");
				}
				details.append("<br>");
			}
			
		} catch (Exception e) {
			logger.error("ERROR while getting details");
//			e.printStackTrace();
		}
		
		return details.toString();
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String page, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView1"), true).elementAt(0);
			
			if(interTable == null) {
				return intermediaryResponse;
			}
			
			Form form = new SimpleHtmlParser(page).getForm(FORM_NAME);
			Map<String, String> params = form.getParams();
			int seq = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			TableRow[] rows = interTable.getRows();
			
			Map<String, String> subdivisions = new HashMap<String, String>();
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				String rowText = row.toPlainTextString().toLowerCase().replaceAll("(?i)&nbsp;", " ");
				
				// parse header
				if(rowText.indexOf("account") > -1) {
					parsedResponse.setHeader("<table border=\"1\" width=\"80%\" align=\"center\">" + 
							row.toHtml().replaceAll("(?is)<tr[^>]*>", "<tr>").replaceAll("(?is)<th[^>]*>", "<th>")
							.replaceAll("(?is)<a[^>]*>(.*?)</a>", "$1")); // perform some cleaning
					continue;
				}
				
				// process links
				StringBuilder rowHtml = new StringBuilder();
				String link = processIntermediaryLink(row, rowHtml, form.action, seq);
				
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml.toString());
				currentResponse.setOnlyResponse(rowHtml.toString());
				currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap m = ro.cst.tsearch.servers.functions.ARBentonTR.parseIntermediaryRow(row);
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				
				DocumentI document = (TaxDocumentI)bridge.importData();				
				currentResponse.setDocument(document);
				
				intermediaryResponse.add(currentResponse);
				
				//save subdivision name
				subdivisions.put(row.getColumns()[0].toPlainTextString(), row.getColumns()[3].toPlainTextString().replaceAll("&nbsp;", ""));
			
			}
			mSearch.setAdditionalInfo(getCurrentServerName() + ":subdivisions:", subdivisions);
			outputTable.append(page);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	private String processIntermediaryLink(TableRow row, StringBuilder rowHtml, String formAction, int seq) {
		
		String link = CreatePartialLink(TSConnectionURL.idPOST) + "/" + formAction + "?" + "seq=" + seq + "&";
		
		Pattern p = Pattern.compile("(?is)__doPostBack[(]'([^']*)','([^']*)'[)]");
		Matcher m = p.matcher(row.toHtml());
		
		if(m.find()) {
			link += "__EVENTTARGET=" + m.group(1) + "&";
			link += "__EVENTARGUMENT=" + m.group(2) + "&";
		}
		
		link = link.replace("$", "@");
		rowHtml.append(row.toHtml().replaceFirst("(?is)<td>((?:\\d|-)+)</td>", "<td><a href=\"" + link + "\">$1</a></td>").replace("@", "$")
				.replaceAll("(?is)<tr[^>]*>", "<tr>"));
		link = link.replace("@", "$");
		
		return link;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		return ro.cst.tsearch.servers.functions.ARBentonTR.parseAndFillResultMap(response, detailsHtml, resultMap);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);
		
		// search by account number
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(1).setSaKey(SearchAttributes.LD_PARCELNO);
			module.addFilter(taxYearFilter);
			modules.add(module);
		}
		
		// search by Address
		if(hasStreet() && hasStreetNo()){
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module );
			module.clearSaKeys();
			String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
			String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
			String key = strNo + " " + strName;
			module.getFunction(1).forceValue(key);
			
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			module.addFilter(taxYearFilter);
			modules.add(module);
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addFilter(taxYearFilter);
			
			module.setIteratorType(1,FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;","L F;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}

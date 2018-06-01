/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.TableColumn;
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
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
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

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author vladb
 */
public class COLarimerTR extends TSServer {

	private static final long serialVersionUID = 1L;

	public COLarimerTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		if (rsResponse.indexOf("Number must only contain numbers") > -1) {
			parsedResponse.setError("Improper format! Please change your search criteria and try again.");
			return;
		}
		
		if (rsResponse.indexOf("Only one of the specific property fields can be entered") > -1) {
			parsedResponse.setError("Only one criteria should be set! Please change your search criteria and try again.");
			return;
		}
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
				
				// no result
				if (rsResponse.indexOf("NO MATCHING RECORDS FOUND FOR THE ENTERED CRITERIA") > -1) {
					parsedResponse.setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				if (rsResponse.indexOf("NO MATCHING RECORDS FOUND FOR THE ENTERED CRITERIA") > -1) {
					parsedResponse.setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				
				if(rsResponse.indexOf("Assessor Property Information") > -1) {
					ParseResponse(sAction, Response, ID_DETAILS);
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				parsedResponse.setHeader("<table border=\"1\" width=\"100%\">" + extractHeader(rsResponse));
				parsedResponse.setFooter("</table>");
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_SEARCH_BY_PARCEL:
				
				if(rsResponse.indexOf("Assessor Property Search Results") > -1) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				} else {
					ParseResponse(sAction, Response, ID_DETAILS);
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				
				if(viParseID == ID_DETAILS || viParseID == ID_SEARCH_BY_PARCEL) {
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
	
					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
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
			case ID_GET_LINK:
				
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
				
		}
	}
	
	private String getDetails(String page, StringBuilder accountId) {

		StringBuilder details = new StringBuilder();
		
		try {
			
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = parser.parse(null);
			
			// get property info
			Div div1 = (Div) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "numbers"), true).elementAt(0);
			Matcher m = Pattern.compile("Schedule Number:\\s*(\\w+)").matcher(div1.toPlainTextString());
			if(m.find()) {
				accountId.append(m.group(1));
			}
			if(page.indexOf("<html") < 0) { // page from memory
				return page;
			}
			details.append(div1.toHtml());
			
			NodeList divs = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "section"), true);
			for(int i = 0; i < divs.size(); i++) {
				Div div = (Div) divs.elementAt(i);
				String divText = div.toPlainTextString(); 
				if(divText.indexOf("General Information") > -1
						|| divText.indexOf("Sales Information") > -1
						|| divText.indexOf("Value Information") > -1) {
					
					TableTag table = (TableTag) div.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
					table.setAttribute("border", "1");
					table.setAttribute("width", "100%");
					
					details.append(div.toHtml());
				}
			}
			
			// get tax info for current year
//			linkTag = (LinkTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "info"), true)
//				.extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
			
			LinkTag linkTag = null;
			nodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "info"), true)
					.extractAllNodesThatMatch(new TagNameFilter("a"), true);
			if (nodeList.size() > 0) {
				for (int idx = 0; idx < nodeList.size(); idx ++) {
					linkTag = (LinkTag) nodeList.elementAt(idx);
					if (linkTag.getLink().indexOf("/treasurer/query") > -1) {
						break;
					}
				}
			}
			
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
					HashCountyToIndex.ANY_COMMUNITY, miServerID);
			String taxLink = dataSite.getLink() + linkTag.extractLink().replaceFirst("/", "");
			String taxPage = getLinkContents(taxLink);
			
			m = Pattern.compile("(?is)<h2>\\s*\\d+ Property Taxes Payable In \\d+\\s*</h2>").matcher(taxPage);
			if(m.find()) {
				details.append(m.group());
			} else if(taxPage.indexOf("NO MATCHING RECORDS FOUND FOR THE ENTERED CRITERIA") > -1) {
				details.append("<h2>No Tax Information</h2>");
				return details.toString().replaceAll("(?is)<a[^>]*>(.*?)</a>", "$1"); // remove links
			}
			
			parser = org.htmlparser.Parser.createParser(taxPage, null);
			nodeList = parser.parse(null);
			
			divs = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "section"), true);
			for(int i = 0; i < divs.size(); i++) {
				Div div = (Div) divs.elementAt(i);
				if(div.toPlainTextString().indexOf("Property Information") > -1) { // general info is already in details
					continue;
				}
				TableTag table = (TableTag) div.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
				table.setAttribute("border", "1");
				table.setAttribute("width", "100%");
				
				details.append(div.toHtml());
			}
			
			// get tax info for previous years
			SelectTag selTag = (SelectTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("name", "taxyear"), true)
				.elementAt(0);
			OptionTag[] opList = selTag.getOptionTags();
			
			Form form = new SimpleHtmlParser(taxPage).getForm("TRE_Query");
			String link = dataSite.getLink() + "treasurer/query/" + form.action;
			
			for(int i = 1; i < opList.length; i++) {
				String opName = opList[i].getOptionText();
				
				HTTPRequest reqP = new HTTPRequest(link);
		    	reqP.setMethod(HTTPRequest.POST);
		    	reqP.setPostParameter("taxyear", opName);
		    	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		    	String taxInfoPage = null;
	        	try {
	        		HTTPResponse resP = site.process(reqP);
	        		taxInfoPage = resP.getResponseAsString();
	        	} finally {
					HttpManager.releaseSite(site);
				}	
	        	
	        	org.htmlparser.Parser parser1 = org.htmlparser.Parser.createParser(taxInfoPage, null);
				NodeList nodeList1 = parser1.parse(null);
				
				divs = nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("class", "section"), true);
				for(int j = 0; j < divs.size(); j++) {
					Div div = (Div) divs.elementAt(j);
					if(div.toPlainTextString().indexOf("Payment Information") > -1) { // general info is already in details
						
						TableTag table = (TableTag) div.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
						table.setAttribute("border", "1");
						table.setAttribute("width", "100%");
						table.setAttribute("id", "oldTax");
						
						details.append("<h3>Year " + opName + "</h3>");
						details.append(div.toHtml());
						break;
					}
					
				}
			}
			
			return details.toString().replaceAll("(?is)<a[^>]*>(.*?)</a>", "$1"); // remove links
			
		} catch (Exception e) {
			logger.error("error while getting details", e);
		}
		
		return null;
	}
	
	private String extractHeader(String page) {
		
		String header = "";

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "stdtable"), true)
				.elementAt(0);
			
			TableRow[] rows = interTable.getRows();
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				
				if((row.toPlainTextString().toUpperCase().indexOf("OWNER NAME") > -1)) {
					
					header = row.toHtml().replaceAll("<a[^>]*>(.*?)</a>", "$1");  // remove links
					break;
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return header;
	}
	
	@SuppressWarnings({"rawtypes" })
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		
		resultMap.put("OtherInformationSet.SrcType","TR");
		detailsHtml = detailsHtml.replaceAll("<br>", "\n");
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			String owner = "";
			String address = "";
			String legal = "";
			
			Div div1 = (Div) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "numbers"), true).elementAt(0);
			Matcher m = Pattern.compile("Schedule Number:\\s*(\\w+)").matcher(div1.toPlainTextString());
			if(m.find()) {
				resultMap.put("PropertyIdentificationSet.ParcelID", m.group(1));
			}
			m = Pattern.compile("Parcel Number:\\s*([\\d-]+)").matcher(div1.toPlainTextString());
			if(m.find()) {
				resultMap.put("PropertyIdentificationSet.ParcelID2", m.group(1));
			}
			m = Pattern.compile("(\\d+) Property Taxes Payable In \\d+").matcher(detailsHtml);
			if(m.find()) {
				resultMap.put("TaxHistorySet.Year", m.group(1));
			}
			
			NodeList divs = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "section"), true);
			for(int i = 0; i < divs.size(); i++) {
				Div div = (Div) divs.elementAt(i);
				TableTag table = (TableTag) div.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
				if(div.toPlainTextString().indexOf("General Information") > -1) {
					NodeList cells = table.getChildren().extractAllNodesThatMatch(new TagNameFilter("td"), true);
					for(int j = 0; j < cells.size(); j++) {
						String content = cells.elementAt(j).toPlainTextString();
						if(content.indexOf("Owner Name") > -1) {
							owner = content.replaceAll("Owner Name\\s*&amp;\\s*Address", "")
								.replaceAll("\\s*\n\\s*", "\n").trim();
						} else if(content.indexOf("Property Address") > -1) {
							address = content.replace("Property Address", "").replaceAll("\\s*\n\\s*", "\n").trim();
							resultMap.put("PropertyIdentificationSet.AddressOnServer", address);
						} else if(content.indexOf("Subdivision") > -1) {
							m = Pattern.compile("(?is)Subdivision #:\\s*\\d+\\s*-\\s*(.*?)(,|\\s-|$)").matcher(content);
							if(m.find()) {
								String subdivName = m.group(1).trim();
								subdivName = subdivName.replaceFirst("(?is)(.*)(?:\\d+\\s*(?:ST|ND|RD|TH)|FIRST|SECOND|THIRD|FOURTH)\\s+SUBDIVISION", "$1");
								subdivName = subdivName.replaceFirst("(?is)(.*)\\s+SUB(?:DIVISION)?.*", "$1");
								resultMap.put("PropertyIdentificationSet.SubdivisionName", subdivName);
							}
						} else if(content.indexOf("Legal Description") > -1) {
							legal = content.replace("Legal Description", "").trim();
						}
					}
				} else if(div.toPlainTextString().indexOf("Sales Information") > -1) {
					TableRow[] rows = table.getRows();
					List<List> body = new ArrayList<List>();
					for(TableRow row : rows) {
						if(row.toPlainTextString().indexOf("Reception") > -1) {
							continue;
						}
						List<String> line = new ArrayList<String>();
						TableColumn[] cols = row.getColumns();
						for(TableColumn col : cols) {
							line.add(col.toPlainTextString().replaceAll("[$,]", "").trim());
						}
						body.add(line);
					}
					if(body != null && body.size() > 0) {
						ResultTable resultTable = new ResultTable();
						String[] header = {"InstrumentNumber", "SalesPrice", "DocumentType", "InstrumentDate"};
						resultTable = GenericFunctions2.createResultTable(body, header);
						resultMap.put("SaleDataSet", resultTable);
					}
				} else if(div.toPlainTextString().indexOf("Value Information") > -1) {
					TableRow[] rows = table.getRows();
					for(TableRow row : rows) {
						if(row.toPlainTextString().indexOf("Value Type") > -1) {
							continue;
						}
						if(row.toPlainTextString().indexOf("Improvement") > -1) {
							resultMap.put("PropertyAppraisalSet.ImprovementAppraisal", 
									row.getColumns()[3].toPlainTextString().replaceAll("[$,]", "").trim());
						} else if(row.toPlainTextString().indexOf("Land") > -1) {
							resultMap.put("PropertyAppraisalSet.LandAppraisal", 
									row.getColumns()[3].toPlainTextString().replaceAll("[$,]", "").trim());
						} else if(row.toPlainTextString().indexOf("Totals") > -1) {
							resultMap.put("PropertyAppraisalSet.TotalAppraisal", 
									row.getColumns()[1].toPlainTextString().replaceAll("[$,]", "").trim());
							resultMap.put("PropertyAppraisalSet.TotalAssessment", 
									row.getColumns()[2].toPlainTextString().replaceAll("[$,]", "").trim());
						}
					}
				} else if(div.toPlainTextString().indexOf("Payment Information") > -1) {
					TableRow[] rows = table.getRows();
					@SuppressWarnings("unused")
					double priorDelinq = 0d;
					String baseAmount = "";
					for(TableRow row : rows) {
						if(row.toPlainTextString().indexOf("Total Tax Liability") > -1) {
							if(StringUtils.isEmpty(table.getAttribute("id"))) { // table for current year
								baseAmount = row.getColumns()[1].toPlainTextString().replaceAll("[$,]", "").trim();
								resultMap.put("TaxHistorySet.BaseAmount", baseAmount);
							}
						} else if(row.toPlainTextString().indexOf("Property Balance") > -1) {
							String amount = row.getColumns()[1].toPlainTextString().replaceAll("[$,]", "").trim();
							if(StringUtils.isEmpty(table.getAttribute("id"))) { // table for current year
								resultMap.put("TaxHistorySet.TotalDue", amount);
								resultMap.put("TaxHistorySet.AmountPaid", GenericFunctions.sum(baseAmount + "+-" + amount, searchId));
							} else { // prev years tax tables
								if(!amount.matches("^0[.]0+")) {
									priorDelinq += Double.valueOf(amount);
								}
							}
						}
					}
				}
			}
			
			ro.cst.tsearch.servers.functions.COLarimerTR.parseNames(resultMap, owner, true);
			ro.cst.tsearch.servers.functions.COLarimerTR.parseAddress(resultMap, address);
			ro.cst.tsearch.servers.functions.COLarimerTR.parseLegalSummary(resultMap, legal);
			
		} catch (Exception e) {
			logger.error("Error while parsing details", e);
		}
		
		return null;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "stdtable"), true)
				.elementAt(0);
			
			if(interTable == null) {
				return intermediaryResponse;
			}
			
			TableRow[] rows = interTable.getRows();
			ArrayList<String> accountList = new ArrayList<String>();
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				
				if(row.toPlainTextString().indexOf("Owner Name") > -1) {	// skip the headers row
					continue;
				}
				
				String account = row.getColumns()[0].toPlainTextString().trim();
				if(accountList.contains(account)) { // skip duplicates
					continue;
				}
				accountList.add(account);
				
				// process links
				LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
				String linkStart = CreatePartialLink(TSConnectionURL.idGET);
				String link = linkStart + "/assessor/query/" + linkTag.extractLink();
				linkTag.setLink(link);
				
				row.removeAttribute("onmouseout");
				row.removeAttribute("onmouseover");
				
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
				currentResponse.setOnlyResponse(row.toHtml());
				currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap resultMap = ro.cst.tsearch.servers.functions.COLarimerTR.parseIntermediaryRow(row);
				Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
				
				DocumentI document = (TaxDocumentI)bridge.importData();				
				currentResponse.setDocument(document);
				
				intermediaryResponse.add(currentResponse);
			}
			outputTable.append(table);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
	
        if(module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX){
        	String pin = module.getFunction(1).getParamValue();
        	pin = pin.replaceAll("[A-Z]", ""); // pin can contain only numbers

           	module.getFunction(1).setParamValue(pin);
          
        }
        return super.SearchBy(module, sd);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		
		// search by account number as Schedule Number
		if(hasPin()){
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
				if (pin.length() < 9){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();			
				// Schedule Number can contain only numbers
				module.getFunction(1).forceValue(pin.replaceAll("[A-Z]", ""));
				modules.add(module);
			} else {
				// search by account number as Parcel Number
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).forceValue(pin.replaceAll("-", ""));
				modules.add(module);
			}
		}
		
		// search by account number as Serial Number 	
		if(hasPin()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			module.getFunction(2).forceValue(pin);
			modules.add(module);
		}
		
		// search by Address
		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		if(!isEmpty(strName) && !isEmpty(strNo)){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(3).forceValue(strName);
			module.getFunction(1).forceValue(strNo);
			module.addFilter(addressFilter);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			modules.add(module);			
		}
		
		// search by name - filter by address
		String name = getSearchAttribute(SearchAttributes.OWNER_LCF_NAME);
		if(hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.getFunction(0).forceValue(name);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.addFilter(addressFilter);
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L, F M;;","L, F;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}

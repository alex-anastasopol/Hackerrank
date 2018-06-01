
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author vladb
 *
 */
public class MDPrinceGeorgesTR extends TSServer {

	
	private static final long serialVersionUID = 1L;
	private static final String FORM_NAME = "Form1";

	public MDPrinceGeorgesTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_ADDRESS:
				// no result
				if (rsResponse.contains("NO RECORD ON CONTROL BLOCK FOR THIS ACCOUNT NUMBER")) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				outputTable.append(viParseID);
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				parsedResponse.setHeader("<table style='border-collapse: collapse' border='2' width='80%' align='center'>" +
						"<tr><th></th><th>ST NO</th><th>SFX</th><th>STREET NAME</th><th>TYPE</th><th>DIR</th><th>UNIT</th>" +
						"<th>OWNER NAME</th><th>DS</th><th>ACCTNO</th></tr>");
				if(StringUtils.isEmpty(parsedResponse.getFooter())) {
					parsedResponse.setFooter("</table>");
				}
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_SEARCH_BY_PARCEL:
				// no result
				if (rsResponse.contains("NO RECORD ON CONTROL BLOCK FOR THIS ACCOUNT NUMBER")) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
            	parsedResponse.setHeader("");
				parsedResponse.setFooter("");
				
				outputTable = new StringBuilder();
				outputTable.append(viParseID);
				smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());

				if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
					String intermediaryDetailsTable = getIntermediaryDetails(rsResponse);
					String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST")
							+ "<div align='center'>" + intermediaryDetailsTable + "</div>" 
							+ "<table style='border-collapse: collapse' border='2' width='80%' align='center'>"
							+ "<tr><th>" + SELECT_ALL_CHECKBOXES + "</th><th></th><th>Year</th><th>Bill Type</th><th>Current Amount Due</th></tr>";
					String footer = "\n</table><br>";
					Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
	            	
					if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
	            		footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer)numberOfUnsavedDocument);
	            	} else {
	            		footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
	            	}
					
					parsedResponse.setHeader(header);
					parsedResponse.setFooter(footer);
				}
				
				break;
			case ID_GET_LINK:
				if(rsResponse.contains("Year") && rsResponse.contains("Bill Type") && rsResponse.contains("Current Amount Due")) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);
				} else if(rsResponse.contains("DISTRICT") && rsResponse.contains("DATA AS OF")) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
				}
				break;
			case ID_DETAILS:
				StringBuilder accountId = new StringBuilder();
				StringBuilder year = new StringBuilder();
				String details = getDetails(rsResponse, accountId, year);
				
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CNTYTAX");
				data.put("year", year.toString());
				if(details.contains("THIS ACCOUNT IS IN TAX SALE")) {
					data.put("dataSource", "TU");
				} else {
					data.put("dataSource", "TR");
				}
				
				if (isInstrumentSaved(accountId.toString().trim(), null, data, false)){
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);
				
				break;
			case ID_SAVE_TO_TSD:
				accountId = new StringBuilder();
				year = new StringBuilder();
				details = "";
				
				if(rsResponse.contains("DISTRICT") && rsResponse.contains("DATA AS OF")) {
					details = getDetails(rsResponse, accountId, year);
				} else if(rsResponse.contains("Year") && rsResponse.contains("Bill Type") && rsResponse.contains("Current Amount Due")) {
					// searching by address in automatic
					details = getFirstYearDetails(Response, accountId, year);
				}
				
				String filename = accountId + ".html";
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				parsedResponse.setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
				break;
		}
	}

	private String getDetails(String rsResponse, StringBuilder accountId, StringBuilder year) {
		StringBuilder details = new StringBuilder();
		
		Matcher m = Pattern.compile("REAL PROPERTY TAX INFORMATION FOR FY (\\d{2})").matcher(rsResponse);
		if(m.find()) {
			int _year = Integer.parseInt(m.group(1));
			if(_year < 20) {
				_year = 2000 + _year;
			} else {
				_year = 1900 + _year;
			}
			year.append(_year);
		}
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table1"), true).elementAt(0)
					.getParent().getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), false);
			
			for(int i = 0; i < tableList.size(); i++) {
				TableTag table = (TableTag) tableList.elementAt(i);
				String tableText = table.toPlainTextString();
				
				if(table.getAttribute("id") != null) {
					if(table.getAttribute("id").equals("tblpayon") || table.getAttribute("id").equals("tblinstruct")) {
						continue;
					}
				}
				
				if(tableText.contains("ACCOUNT NUMBER")) {
					accountId.append(table.getRow(0).getColumns()[1].toPlainTextString().trim());
					
					if(!rsResponse.contains("<HTML>")) {
						return rsResponse;
					}
				}
				
				details.append(table.toHtml().replaceAll("(?is)<a [^>]*>.*?</a>", ""));
			}
			
			@SuppressWarnings("unchecked")
			TreeMap<String, List<String>> yearLinkMap = (TreeMap<String, List<String>>) mSearch.getAdditionalInfo("TR_" + accountId);
			Map<String, List<String>> _yearLinkMap = yearLinkMap.descendingMap();
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			
			for(Map.Entry<String, List<String>> entry : _yearLinkMap.entrySet()) {
				String _year = entry.getKey();
				String _link = dataSite.getLink() + entry.getValue().get(0).replaceFirst(".*?&Link=", "");
				String[] linkParts = _link.split("[?&]");
				
				if(_year.compareTo(year.toString()) >= 0) {
					continue;
				}
				
				details.append("<h3>Year " + _year + "</h3>");
				
				HTTPRequest reqP = new HTTPRequest(linkParts[0]);
				for(int i = 1; i < linkParts.length; i++) {
					String part = linkParts[i];
					String[] tokens = part.split("=");
					reqP.setPostParameter(tokens[0], tokens.length > 1 ? tokens[1] : "");
				}
		    	reqP.setMethod(HTTPRequest.POST);
		    	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		    	String taxInfoPage = "";
	        	
		    	try {
	        		HTTPResponse resP = site.process(reqP);
	        		taxInfoPage = resP.getResponseAsString();
	        	} finally {
					HttpManager.releaseSite(site);
				}
		    	
		    	if(StringUtils.isNotEmpty(taxInfoPage)) {
		    		htmlParser = org.htmlparser.Parser.createParser(taxInfoPage, null);
		    		nodeList = htmlParser.parse(null);
		    		
		    		tableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table1"), true).elementAt(0)
							.getParent().getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), false);
		    		boolean foundPropDescTable = false;
					
					for(int i = 0; i < tableList.size(); i++) {
						TableTag table = (TableTag) tableList.elementAt(i);
						String tableText = table.toPlainTextString();
						
						if(tableText.contains("OCCUPANCY")) {
							foundPropDescTable = true;
							continue;
						}
						if(!foundPropDescTable) {
							continue;
						}
						if(table.getAttribute("id") != null) {
							if(table.getAttribute("id").equals("tblpayon") || table.getAttribute("id").equals("tblinstruct")) {
								continue;
							}
						}
						
						details.append(table.toHtml().replaceAll("(?is)<a [^>]*>.*?</a>", ""));
					}
		    	}
			}
		} catch(Exception e) {
			logger.error("Error while parsing intermediary results", e);
		}
		
		return "<div id='myData'>" + details.toString() + "</div>";
	}
	
	/** 
	 * this is called when searching by address in automatic
	 */
	private String getFirstYearDetails(ServerResponse response, StringBuilder accountId, StringBuilder year) {
		String lastYearPage = "";
		String rsResponse = response.getResult();
		StringBuilder outputTable = new StringBuilder();
		outputTable.append(ID_SEARCH_BY_PARCEL);
		String accountNo = "";
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node node = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "acctnum"), true).elementAt(0);
			if(node != null) {
				accountNo = node.toPlainTextString().trim();
			}
			
			// we need this to obtain the yearLink map
			smartParseIntermediary(response, rsResponse, outputTable);
			
			@SuppressWarnings("unchecked")
			TreeMap<String, List<String>> yearLinkMap = (TreeMap<String, List<String>>) mSearch.getAdditionalInfo("TR_" + accountNo);
			Map<String, List<String>> _yearLinkMap = yearLinkMap.descendingMap();
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			// get first entry
			Map.Entry<String, List<String>> entry = _yearLinkMap.entrySet().iterator().next();
			
			String _link = dataSite.getLink() + entry.getValue().get(0).replaceFirst(".*?&Link=", "");
			String[] linkParts = _link.split("[?&]");
			
			HTTPRequest reqP = new HTTPRequest(linkParts[0]);
			for(int i = 1; i < linkParts.length; i++) {
				String part = linkParts[i];
				String[] tokens = part.split("=");
				reqP.setPostParameter(tokens[0], tokens.length > 1 ? tokens[1] : "");
			}
	    	reqP.setMethod(HTTPRequest.POST);
	    	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
	    	String taxInfoPage = "";
        	
	    	try {
        		HTTPResponse resP = site.process(reqP);
        		taxInfoPage = resP.getResponseAsString();
        	} finally {
				HttpManager.releaseSite(site);
			}
	    	
	    	if(StringUtils.isNotEmpty(taxInfoPage)) {
	    		lastYearPage = taxInfoPage;
	    	}
		} catch (Exception e) {
			logger.error("ERROR while parsing details");
		}
		
		return getDetails(lastYearPage, accountId, year);
	}

	private String getIntermediaryDetails(String rsResponse) {
		String detailsHtml = "";
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node table = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table1"), true).elementAt(0);
			detailsHtml = table.toHtml().replaceAll("(?is)<a [^>]*>.*?</a>", "");
		} catch(Exception e) {
			logger.error("Error while parsing intermediary results", e);
		}
		
		return detailsHtml;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String page, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();
		int viParseID = Integer.parseInt(outputTable.toString());
		outputTable.setLength(0);
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Form form = new SimpleHtmlParser(page).getForm(FORM_NAME);
			Map<String, String> params = form.getParams();
			int seq = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);

			if(viParseID == ID_SEARCH_BY_ADDRESS) {
				TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "dgPremise"), true).elementAt(0);
				if(interTable == null) {
					return intermediaryResponse;
				}
				
				TableRow[] rows = interTable.getRows();
				
				for(int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					String rowText = row.toPlainTextString();
					
					if(rowText.contains("Prev") || rowText.contains("Next")){
						NodeList links = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						
						for(int j = 0; j < links.size(); j++) {
							LinkTag linkTag = (LinkTag) links.elementAt(j);
							if(linkTag.toPlainTextString().contains("Prev") || linkTag.toPlainTextString().contains("Next")) {
								processIntermediaryLink(linkTag, form.action, seq);
							}
							if(linkTag.toPlainTextString().contains("Next")) {
								parsedResponse.setNextLink(linkTag.getLink());
							}
						}
						
						parsedResponse.setFooter(row.toHtml() + "</table>");
						break;
					}
					
					LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
					if(linkTag == null) {
						continue;
					}
					processIntermediaryLink(linkTag, form.action, seq);
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(linkTag.getLink(), linkTag.getLink(), TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = ro.cst.tsearch.servers.functions.MDPrinceGeorgesTR.parseIntermediaryRow(row);
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
				
				outputTable.append(interTable.toHtml());
				
			} else {
				TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "dgSummary"), true).elementAt(0);
				if(interTable == null) {
					return intermediaryResponse;
				}
				
				String accountNo = "";
				int numberOfUncheckedElements = 0;
				TreeMap<String, List<String>> yearLinkMap = new TreeMap<String, List<String>>();
				TableRow[] rows = interTable.getRows();
				
				Node node = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "acctnum"), true).elementAt(0);
				if(node != null) {
					accountNo = node.toPlainTextString().trim();
				}
				
				for(int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					String rowText = row.toPlainTextString();
					ParsedResponse currentResponse = new ParsedResponse();
					
					// if years table is split on 2 pages, we concatenate them
					if(rowText.contains("Prev") || rowText.contains("Next")){
						NodeList linkList = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(linkList.size() - 1);
						if(linkTag.toPlainTextString().contains("Prev")) {
							break;
						}
						
						DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
						processIntermediaryLink(linkTag, form.action, seq);
						String _link = dataSite.getLink() + linkTag.getLink().replaceFirst(".*?&Link=", "");
						String[] linkParts = _link.split("[?&]");
						
						HTTPRequest reqP = new HTTPRequest(linkParts[0]);
						for(int j = 1; j < linkParts.length; j++) {
							String part = linkParts[j];
							String[] tokens = part.split("=");
							reqP.setPostParameter(tokens[0], tokens.length > 1 ? tokens[1] : "");
						}
				    	reqP.setMethod(HTTPRequest.POST);
				    	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				    	String taxHistoryPage = "";
			        	
				    	try {
			        		HTTPResponse resP = site.process(reqP);
			        		taxHistoryPage = resP.getResponseAsString();
			        	} finally {
							HttpManager.releaseSite(site);
						}
				    	
				    	htmlParser = org.htmlparser.Parser.createParser(taxHistoryPage, null);
						nodeList = htmlParser.parse(null);
				    	interTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "dgSummary"), true).elementAt(0);
				    	
				    	if(interTable == null) {
							return intermediaryResponse;
						}
				    	
				    	rows = interTable.getRows();
				    	i = 0;
				    	
				    	form = new SimpleHtmlParser(taxHistoryPage).getForm(FORM_NAME);
						params = form.getParams();
						seq = getSeq();
						mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						
				    	continue;
					}
					
					if(row.getColumnCount() != 4 || rowText.contains("Current Amount Due")) {
						continue;
					}
					
					LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
					if(linkTag == null) {
						continue;
					}
					processIntermediaryLink(linkTag, form.action, seq);
					
					ResultMap map = ro.cst.tsearch.servers.functions.MDPrinceGeorgesTR.parseIntermediaryRow(row);
					map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNo);
					String year = (String) map.get(TaxHistorySetKey.YEAR.getKeyName());
					String balance = row.getColumns()[3].toPlainTextString().replaceAll(",", "").trim();
					List<String> rowInfoList = new ArrayList<String>();
					rowInfoList.add(linkTag.getLink());
					rowInfoList.add(balance);
					yearLinkMap.put(year, rowInfoList);
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					data.put("year", year);
					data.put("dataSource", (String) map.get(OtherInformationSetKey.SRC_TYPE.getKeyName()));
					
					String checkBox = "checked";
					
					if (isInstrumentSaved(accountNo, null, data, false) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
		    			checkBox = "saved";
		    		} else {
		    			numberOfUncheckedElements++;
		    			
		    			if(linkTag != null) {
			    			checkBox = "<input type='checkbox' name='docLink' value='" + linkTag.getLink() + "'>";
							currentResponse.setPageLink(new LinkInPage(linkTag.getLink(), linkTag.getLink(), TSServer.REQUEST_SAVE_TO_TSD));
						}
		    		}
					
					String rowHtml = row.toHtml().replaceFirst("(?is)<tr([^>]*)>", "<tr$1><td align='center'>"+ checkBox + "</td>");
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					
					Bridge bridge = new Bridge(currentResponse, map, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
				
				mSearch.setAdditionalInfo("TR_" + accountNo, yearLinkMap);
				outputTable.append(interTable.toHtml());
				SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			}
		} catch(Exception e) {
			logger.error("Error while parsing intermediary results", e);
		}
		
		return intermediaryResponse;
	}
	
	private void processIntermediaryLink(LinkTag linkTag, String formAction, int seq) {
		
		String link = CreatePartialLink(TSConnectionURL.idPOST) + "/" + formAction + "?" + "seq=" + seq + "&";
		
		Pattern p = Pattern.compile("(?is)__doPostBack[(]'([^']*)','([^']*)'[)]");
		Matcher m = p.matcher(linkTag.getLink());
		
		if(m.find()) {
			link += "__EVENTTARGET=" + m.group(1).replace("$", ":") + "&";
			link += "__EVENTARGUMENT=" + m.group(2).replace("$", ":") + "&";
		}
		
		linkTag.setLink(link);
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		if(detailsHtml.contains("REAL PROPERTY TAX INFORMATION")) {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		} else if(detailsHtml.contains("THIS ACCOUNT IS IN TAX SALE")) {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TU");
		}
		
		try {
			String address = "";
			String owner = "";
			String legal = "";
			boolean firstTaxYear = true;
			boolean firstTaxDesc = true;
			double delinqAmount = 0.0;
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table1"), true).elementAt(0)
					.getParent().getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), false);
			
			for(int i = 0; i < tableList.size(); i++) {
				TableTag table = (TableTag) tableList.elementAt(i);
				String tableText = table.toPlainTextString();
				TableRow[] rows = table.getRows();
				
				if(tableText.contains("REAL PROPERTY TAX INFORMATION")) {
					Matcher m = Pattern.compile("REAL PROPERTY TAX INFORMATION FOR FY (\\d{2})").matcher(tableText);
					if(m.find()) {
						int year = Integer.parseInt(m.group(1));
						if(year < 20) {
							year = 2000 + year;
						} else {
							year = 1900 + year;
						}
						resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), String.valueOf(year));
					}
				} else if(tableText.contains("TAX SALE REDEMPTION")) {
					Matcher m = Pattern.compile("FY(\\d{2}) TAX SALE REDEMPTION").matcher(tableText);
					if(m.find()) {
						int year = Integer.parseInt(m.group(1));
						if(year < 20) {
							year = 2000 + year;
						} else {
							year = 1900 + year;
						}
						resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), String.valueOf(year));
					}
				} else if(tableText.contains("ACCOUNT NUMBER")) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), table.getRow(0).getColumns()[1].toPlainTextString().trim());
				} else if(tableText.contains("OWNER:") && tableText.contains("PROPERTY ADDRESS:")) {
					owner = rows[1].getColumns()[0].toPlainTextString().trim();
					address = rows[4].getColumns()[0].toPlainTextString().replaceFirst("^0+", "").trim();
					
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				} else if(tableText.contains("PROPERTY DESCRIPTION")) {
					legal = rows[1].getColumns()[1].toPlainTextString().trim();
				} else if(tableText.contains("OCCUPANCY:") && tableText.contains("ASSESSMENT:")) {
					// the property description table
					for(TableRow row : rows) {
						String prevCol = "";
						for(TableColumn col : row.getColumns()) {
							if(prevCol.contains("PHASE")) {
								String phase = col.toPlainTextString().trim();
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
							} else if(prevCol.contains("SUBNAME:")) {
								String subdivision = col.toPlainTextString().replaceAll(">|-", "").trim();
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
							} else if(prevCol.contains("SECTION:")) {
								String section = col.toPlainTextString().replaceFirst("^0+", "").trim();
								resultMap.put(PropertyIdentificationSetKey.SECTION.getKeyName(), section);
							} else if(prevCol.contains("LOT:")) {
								String lot = col.toPlainTextString().trim();
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
							} else if(prevCol.contains("BLOCK:")) {
								String block = col.toPlainTextString().trim();
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
							} else if(prevCol.contains("UNIT")) {
								String unit = col.toPlainTextString().trim();
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
							} else if(prevCol.contains("ASSESSMENT:")) {
								String assessment = col.toPlainTextString().replace(",", "").trim();
								resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessment);
							}
							prevCol = col.toPlainTextString().trim();
						}
					}
				} else if(tableText.contains("PAYMENT RECEIVED") && tableText.contains("REFUND DATE")) {
					// the tax description table
					if(!firstTaxDesc) {
						firstTaxYear = false;
					}
					if(!firstTaxYear) {
						continue;
					}
					
					for(TableRow row : rows) {
						if(row.getColumnCount() == 8) {
							String col0 = row.getColumns()[0].toPlainTextString().trim();
							String col7 = row.getColumns()[7].toPlainTextString().replace(",", "").trim();
							
							if(col0.equals("TOTAL")) {
								resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), col7);
							} if(col0.equals("PAYMENT RECEIVED")) {
								resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), col7);
								String col1 = row.getColumns()[1].toPlainTextString().trim();
								if(col1.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
									resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), col1);
								}
							}
						}
					}
					
					firstTaxDesc = false;
				} else if(tableText.contains("BAL DUE IF")) {
					// table present only if there is an amount due
					String prevRowText = "";
					
					for(TableRow row : rows) {
						if(prevRowText.contains("BAL DUE IF")) {
							if(row.getColumnCount() >= 5) {
								String amountDue = row.getColumns()[4].toPlainTextString().replace(",", "");
								if(firstTaxYear) {
									resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
								} else {
									try {
										delinqAmount += Double.parseDouble(amountDue);
									} catch (Exception e) {
										logger.error("Error while parsing details", e);
									}
								}
								break;
							}
						}
						
						prevRowText = row.toPlainTextString().trim();
					}
				} else if(tableText.contains("TOTAL AMOUNT DUE IF PAID IN")) {
					// table present only if the account is in tax sale
					String prevRowText = "";
					
					for(TableRow row : rows) {
						if(prevRowText.contains("TOTAL AMOUNT DUE IF PAID IN")) {
							if(row.getColumnCount() >= 6) {
								String baseAmount = row.getColumns()[2].toPlainTextString().replace(",", "");
								String amountDue = row.getColumns()[5].toPlainTextString().replace(",", "");
								if(firstTaxYear) {
									resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
									resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
								} else {
									try {
										delinqAmount += Double.parseDouble(amountDue);
									} catch (Exception e) {
										logger.error("Error while parsing details", e);
									}
								}
								break;
							}
						}
						
						prevRowText = row.toPlainTextString().trim();
					}
				}
				
			}
			
			resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), "" + delinqAmount);
			ro.cst.tsearch.servers.functions.MDPrinceGeorgesTR.parseOwners(resultMap, owner);
			ro.cst.tsearch.servers.functions.MDPrinceGeorgesTR.parseLegal(resultMap, legal);
		} catch(Exception e) {
			logger.error("Error while parsing details", e);
		}
		
		
		return null;
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
			module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			module.addFilter(taxYearFilter);
			modules.add(module);
		}
		
		// search by Address
		if(hasStreet() && hasStreetNo()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
			module.getFunction(1).setSaKey(SearchAttributes.P_STREETNAME);

			module.addFilter(addressFilter);

			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}

	/**
	 * we save all years that have balance > 0 as separate docs
	 */
	@Override
	public void addAdditionalDocuments(DocumentI doc, ServerResponse Response) {
		if(!(mSearch.getSearchType() == Search.AUTOMATIC_SEARCH)) {
			return;
		}
		
		TaxDocumentI taxDoc = (TaxDocumentI) doc;
		String accountId = taxDoc.getInstno();
		String year = taxDoc.getYear() + "";
		
		try {
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			@SuppressWarnings("unchecked")
			TreeMap<String, List<String>> yearLinkMap = (TreeMap<String, List<String>>) mSearch.getAdditionalInfo("TR_" + accountId);
			Map<String, List<String>> _yearLinkMap = yearLinkMap.descendingMap();
			
			for(Map.Entry<String, List<String>> entry : _yearLinkMap.entrySet()) {
				String _year = entry.getKey();
				String _link = dataSite.getLink() + entry.getValue().get(0).replaceFirst(".*?&Link=", "");
				String[] linkParts = _link.split("[?&]");
				
				double balance = 0d;
				try {
					balance = Double.parseDouble(entry.getValue().get(1));
				} catch (NumberFormatException e) {
					logger.error("ERROR while saving additional docs", e);
				}
				
				if(_year.compareTo(year.toString()) >= 0 || balance == 0.0) {
					continue;
				}
				
				HTTPRequest reqP = new HTTPRequest(linkParts[0]);
				for(int i = 1; i < linkParts.length; i++) {
					String part = linkParts[i];
					String[] tokens = part.split("=");
					reqP.setPostParameter(tokens[0], tokens.length > 1 ? tokens[1] : "");
				}
				reqP.setMethod(HTTPRequest.POST);
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				String taxInfoPage = "";
				
				try {
					HTTPResponse resP = site.process(reqP);
					taxInfoPage = resP.getResponseAsString();
				} finally {
					HttpManager.releaseSite(site);
				}
				
				if(StringUtils.isNotEmpty(taxInfoPage)) {
					String sAction = "/" + _link.replaceFirst("(?is).*?[^/]/([^/])", "$1");
					super.solveHtmlResponse(sAction, ID_SAVE_TO_TSD, "SaveToTSD", Response, taxInfoPage);
				}
				
				break;
			}
		} catch (ServerResponseException e) {
			logger.error("ERROR while saving additional docs", e);
		}
	}
}

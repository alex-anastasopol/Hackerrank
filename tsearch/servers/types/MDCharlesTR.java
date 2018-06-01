package ro.cst.tsearch.servers.types;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.MultipleYearIterator;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.ServletServerComm;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class MDCharlesTR extends TSServer {


	private static final long serialVersionUID = 4940198725194257741L;

	public MDCharlesTR(long searchId) {
		super(searchId);
	}
	
	public MDCharlesTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String rsResponse, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		if (rsResponse.contains("No records exist for search criteria. Please try again.")) {
				return intermediaryResponse;
		} else
		try {
			HtmlParser3 htmlParser = new HtmlParser3(rsResponse);	
			NodeList nodeList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("table"),true);
			String type = "";
			
			if (nodeList != null) {
				TableTag table = null;
				if (nodeList.size() > 2) {
					table = (TableTag) nodeList.elementAt(2);
				}
				
				if (table == null) {
					return intermediaryResponse;
					
				} else {
					if (table.getRows().length == 1) {
						TableRow[] rows  = table.getRows();
						
						TableTag linksTable = (TableTag) rows[0].getColumns()[0].getChildren().elementAt(1).getChildren().elementAt(3);
						linksTable.setAttribute("id","\"linksTable\"");
						
						TableTag resultsTable = (TableTag) rows[0].getColumns()[0].getChildren().elementAt(1).getChildren().elementAt(3);
						resultsTable.setAttribute("id", "\"intermResTable\"");
						rows = resultsTable.getRows();
						
						type = resultsTable.getRow(0).getColumns()[1].getChildren().elementAt(0).getChildren().elementAt(2).toHtml().trim();
						int size = rows.length;
						
						for (int i = 1; i < size; i++) {
							TableRow row = rows[i];
							if(row.getColumnCount() == 2) {
								ParsedResponse currentResponse = new ParsedResponse();
		
								LinkTag linkTag = ((LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
										
								String link = CreatePartialLink(TSConnectionURL.idGET) + "/treas/taxes/acctinquiry/" + 
											linkTag.extractLink().trim().replaceAll("\\s", "%20");
										
								linkTag.setLink(link);
		//								
		//								String link = CreatePartialLink(TSConnectionURL.idPOST) + "/realproperty/" + action   
		//										+ (action.contains("?") ? "&" : "?") + "__EVENTTARGET=" + tmpLnk + "&seq=" + seq;
		//								
		//								linkTag.setLink(link);
		
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
								currentResponse.setOnlyResponse(row.toHtml());
								currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
										
								ResultMap map = parseIntermediaryRow(row, type, searchId); 
								if (StringUtils.isEmpty((String) map.get(TaxHistorySetKey.YEAR.getKeyName()))) {
									DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
									int taxYear = Integer.parseInt(site.getPayDate().toString()
											.replaceAll("(?is)(?:.*)(?:[A-Z]+T|WIB)\\s*(\\d{4})", "$1"));
									map.put(TaxHistorySetKey.YEAR.getKeyName(), Integer.toString(taxYear));
								}
								
								Bridge bridge = new Bridge(currentResponse, map, searchId);
								DocumentI document = (TaxDocumentI)bridge.importData();				
								
								currentResponse.setDocument(document);
										
								intermediaryResponse.add(currentResponse);		
							}
						}
					}
					
				}
			}
			
			String header = "<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + 
					"<tr><th>Property Number</th> <th> " + type + "</th></tr>";
			StringBuilder footer =  getPrevAndNextLinks (response, htmlParser); 
			
			response.getParsedResponse().setHeader(header);
			response.getParsedResponse().setFooter(footer + "</table>");
		
			outputTable.append(rsResponse);
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	
	public StringBuilder getPrevAndNextLinks (ServerResponse response, HtmlParser3 htmlParser) {
		String url = response.getLastURI().toString().replaceFirst("(?is)https?://[^/]+(/[^\\$]+)", "$1");
		url = url.replaceFirst("(?is)\\?[^\\$]+", "");
		
		if(!url.contains("addressList.jsp")) {
			url = "/treas/taxes/acctinquiry/addressList.jsp";
		}
		
		String linkN = "";
		int indexN = 0;
		boolean addF = false;
		String linkP = "";
		int indexP = 0;
		boolean addP = false;
		String linkF = "";
		int indexF = 0;
		boolean addN = false;
		String linkL = "";
		int indexL = 0;
		boolean addL = false;
		
		String links = "";
		String tmp = "";
		
		StringBuilder footer = new StringBuilder("<tr><td colspan=\"2\"> </br> &nbsp; &nbsp;");
		
		// create links for Next/Prev, First/Last options
		NodeList nodeList = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true); 						 				
			
		if (nodeList.size() > 4) {					
			TableTag tableTag = (TableTag) nodeList.elementAt(4);
			
			if (tableTag.getRowCount() == 1) { 
			TableRow row = tableTag.getRow(0);
			if (row.getColumnCount() > 4) {
//				column[0] = First;  column[1] = Prev;  column[2]=Next;  column[3]=Last;
				
				for (int i=0; i < row.getColumnCount()-1; i++) {
					String s = row.getColumns()[i].getChildren().elementAt(0).toHtml();
					s = s.replaceFirst("(?is)<a\\s*href[^\"]+\"[^\\?]+(\\?[^\"]+).*", "$1").trim();
					tmp = s;
					
					if (i ==0) { //link for FIRST
						linkF = CreatePartialLink(TSConnectionURL.idGET) + url + s;
						indexF = 1;
						addF = true;
					} else if(i == 1) {//link for PREV
						linkP = CreatePartialLink(TSConnectionURL.idGET) + url + s;
						indexP = Integer.parseInt(tmp.replaceFirst("(?is).*hdnPageNumber=(\\d+).*", "$1"));
						addP = true;
					} else if(i == 2) {//link for NEXT
						linkN = CreatePartialLink(TSConnectionURL.idGET) + url + s;
						indexN = Integer.parseInt(tmp.replaceFirst("(?is).*hdnPageNumber=(\\d+).*", "$1"));
						addN = true;
					} else if(i == 3) {//link for LAST
						linkL = CreatePartialLink(TSConnectionURL.idGET) + url + s;
						indexL = Integer.parseInt(tmp.replaceFirst("(?is).*hdnPageNumber=(\\d+).*", "$1"));
						addL = true;
					}
				}
			}
			
//			if (indexP >= indexF && indexN <= indexL && indexF < indexL) {
//				addF = true;
//				addP = true;
//				addN = true;
//				addL = true;
//				
//			} else 
			if (indexF < indexL)  {
				 if (indexN > indexL) {
					addN = false;
				 }
				 if (indexP < indexF) {
					addP = false;
				 }
			
			} else if (indexF == indexL) {
				addP = false;
				addN = false;
				addL = false;
			} 
			
			
			 
			if (StringUtils.isNotEmpty(linkF)) {
				if (addF) {
					links = links + "<a href=\"" + linkF + "\"> First </a> &nbsp; &nbsp;";
				}
			}
			if (StringUtils.isNotEmpty(linkP)) {
				if (addP) {
					links = links + "<a href=\"" + linkP + "\"> Prev </a> &nbsp; &nbsp;";
				}
			}
			if (StringUtils.isNotEmpty(linkN)) {
				if (addN) {
					links = links + "<a href=\"" + linkN + "\"> Next </a> &nbsp; &nbsp;";
					response.getParsedResponse().setNextLink("<a href=\"" + linkN + "\">Next</a>");
				}
			}	
			if (StringUtils.isNotEmpty(linkL)) {
				if (addL) {
					links = links + "<a href=\"" + linkL + "\"> Last </a> &nbsp; &nbsp;";
				}
			}
			
			footer.append(links);
			footer.append("</td> </tr>");
			}
		}
		
		return footer;
	}
	
	
	public static ResultMap parseIntermediaryRow(TableRow row, String type, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 2) {					
			String apn = cols[0].getFirstChild().getFirstChild().toHtml().trim();
			
			if (StringUtils.isNotEmpty(apn)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), apn);
			}
			
			if ("Name".equals(type)) {
				String owners = cols[1].getFirstChild().getFirstChild().toHtml().replaceAll("\\b&nbsp;\\s*", " ").trim();
				if (StringUtils.isNotEmpty(owners)) {
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owners);
					try {
						parseNames(resultMap, searchId);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else if ("Address".equals(type)) {
				String address = cols[1].getFirstChild().getFirstChild().toHtml().replaceAll("\\b&nbsp;\\s*", " ").trim();
				if (StringUtils.isNotEmpty(address)) {
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
					parseAddress(resultMap, searchId);
				}
			}
		}
		
		return resultMap;
	}
	
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// 	no result returned - error message on official site
		if (rsResponse.contains("Error on loading page")) {
			Response.getParsedResponse().setError("Official site not functional");
    		return;
    	} else if (rsResponse.indexOf("No records exist for search criteria. Please try again.") > -1) {
			Response.getParsedResponse().setError(TSServer.NO_DATA_FOUND);
			return;
		}
		
		switch (viParseID) {			
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_NAME:
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
	            }			
				break;
				
			case ID_SEARCH_BY_PARCEL:
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:						
				StringBuilder accountId = new StringBuilder();
				String details;
				if (rsResponse.contains("ownerInfoTable")) {
					details = rsResponse;
				} else {
					details = getDetails(rsResponse, accountId);
				}
				
				String filename = accountId + ".html";
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					
					if (isInstrumentSaved(accountId.toString(),null,data)){
						details += CreateFileAlreadyInTSD();
					
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);					
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
					
				} else {
					smartParseDetails(Response,details);
					
					msSaveToTSDFileName = filename;
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					Response.getParsedResponse().setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();					
				}
				break;
				
			case ID_GET_LINK :
				if (rsResponse.contains("Amount Due")) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				
				break;
				
			default:
				break;
		}
	}	
	
	
	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				return rsResponse;
			
			} else {
				try {
					StringBuilder details = new StringBuilder();
					HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
					details.append("<html>");
					
					NodeList nodeList = htmlParser.getNodeList()
							.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					
					
					
					if(nodeList.size() == 0) {
						return rsResponse;
					
					} else {
						if (nodeList.size()> 2) {
							details.append("<div align = \"center\"> <h2> Property Tax Account Inquiry </h2> </div> <br>");
							
							TableTag tmpTable = (TableTag) nodeList.elementAt(3);
							tmpTable.setAttribute("id", "\"ownerInfoTable\"");
							if (tmpTable.getRowCount() > 1) {
								TableRow row =  tmpTable.getRow(1);
								if (row.getColumnCount() > 1) {
									TableColumn col = row.getColumns()[1];
									String apn = col.getChildrenHTML().trim();							
									if (StringUtils.isNotEmpty(apn)) {
										accountId.append(apn);		
									}
								}
							}
							details.append(tmpTable.toHtml().replaceFirst("(?is)\\(see\\s*notes\\s*below\\)", "")
									+ "<br> <br>");
							
							LinkTag legalInfo = (LinkTag) htmlParser.getNodeList()
									.extractAllNodesThatMatch(new TagNameFilter("div"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("align", "center")).elementAt(0)
									.getChildren().elementAt(1);
							
							if (legalInfo != null) {
								String url = legalInfo.toHtml().replaceAll("(?is)<a[^/]+(/[^\\']+)'[^\\\"]+\">.*", "$1");
								url = url.trim().replaceAll("\\s", "%20");

								DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
								String serverHomeLink = site.getServerHomeLink();
								url = serverHomeLink + "treas/taxes/acctinquiry" + url;
									
								String rsp = getLinkContents(url);
								HtmlParser3 htmlLD = new HtmlParser3(rsp);
								tmpTable = (TableTag) htmlLD.getNodeList()
										.extractAllNodesThatMatch(new TagNameFilter("table"), true)
										.elementAt(0);
								
								if (tmpTable != null) {
									tmpTable.setAttribute("id","\"legalDescTable\"");
									tmpTable.setAttribute("border","\"1\"");
									
									details.append("<div align = \"center\"> <b> Legal Description: </b> </div>");
									details.append(tmpTable.toHtml().
											replaceFirst("(?is)(?is)<tr>\\s*<td>\\s*\\s*<div[^>]+>\\s*<input[^>]+>\\s*</div>\\s*</td>\\s*</tr>", ""));
								}
							}
							
							tmpTable = (TableTag) nodeList.elementAt(4);
							tmpTable.setAttribute("id", "\"taxInfoTable\"");
							details.append("<br> <br> <div align = \"center\"> <b> Tax information: </b> </div>");
							details.append(tmpTable.toHtml()
									.replaceAll("(?is)\\s*<div align=\"right\">\\s*<a[^>]+>([^<]+)</a>\\s*</div>", "$1"));
							
							details.append("<br> </html>");
						}
					
						return details.toString();
					}
					
				} catch (Throwable t){
					logger.error("Error while parsing details page data", t);
				}
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return rsResponse;
	}

	
	public void parseDetails(String detailsHtml, ResultMap map, long searchId) {
		parseAddress(map, searchId);
		parseNames(map, searchId);
		parseTaxes(detailsHtml, map, searchId);
	}
	
	
	public static void parseAddress(ResultMap map, long searchId) {
		String address = (String) map.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isNotEmpty(address)) {
//			String regExp = "(?is)\\s*(\\d+(?:\\s*-\\s*\\d+)?)?\\s*([A-Z\\s'-]+)\\s*(\\b(?:ST(?:REET)?|DR(?:IVE)?|CT|R(?:OA)?D|L(?:A)?N(?:E)?|PL(?:ACE)?|AVE?(?:NUE)?)\\b)?\\s*";
//			Pattern p = Pattern.compile(regExp);
//			Matcher m = p.matcher(address);
			String strNo = "";
			String strName = "";
//			String strSuffix = "";
//			if (m.find()) {
//				String[] s = StringFormats.parseAddress(address);
//				if (s != null) {
//					strNo = s[0].trim();
//					
//					regExp = "(?is)(.*)(\\b(?:ST(?:REET)?|DR(?:IVE)?|CT|R(?:OA)?D|L(?:A)?N(?:E)?|PL(?:ACE)?|AVE?(?:NUE)?)\\b)";
//					p = Pattern.compile(regExp);
//					m = p.matcher(s[1]);
//					if (m.find()){
//						strName = m.group(1);
//						strSuffix = m.group(2);
//					} else {
//						strName = s[1].trim();
//					}
//				}
//			}
			
			strNo = StringFormats.StreetNo(address);
			strName = StringFormats.StreetName(address);
			
			if (StringUtils.isNotEmpty(strNo)) {
				map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), strNo);
			}
			if (StringUtils.isNotEmpty(strName)) {
				map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), strName);
			}
//			if (StringUtils.isNotEmpty(strSuffix)) {
//				map.put(PropertyIdentificationSetKey.SUFFIX.getKeyName(), strSuffix);
//			}
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		String unparsedName = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		List<List> body = new ArrayList<List>();
		
		if(StringUtils.isNotEmpty(unparsedName)) {
			unparsedName = unparsedName.replaceAll("\\s*-\\s*(ETAL),?\\s*", " $1 - ");
			
			unparsedName = unparsedName.replaceAll("(?is)\\b(A|F)KA\\b", " - @@@FML@@@");
			unparsedName = unparsedName.replaceAll("(?is)\\s+-\\s+ETAL", " ETAL - ");
			
			String[] mainTokens = unparsedName.split("\\s*<br>\\s*");
											
			for (int i = 0; i < mainTokens.length; i++) {
				String currentToken = mainTokens[i];
				String[] names = currentToken.split("<br>");
					
				if (currentToken.matches("(?is)[\\w'-]+\\s*,\\s*(?:[A-Z]\\b\\s*\\w+|\\w+\\s*[A-Z])\\s*(?:II|I|SR|JR)?\\s*&\\s*(?:\\w+\\s*[A-Z]?\\s[A-Z]\\w+)")) {  
					//Property# 09004947: JUDGE ELLA & PATRICIA A DEMARTINI;   Property# 08014981: NELSON KEVIN M & WENDY D SCOTT  
					names = StringFormats.parseNameDesotoRO(currentToken);
				
				} else {
					//Property# 06305326: NELSON RONALD K & RACHEL L;    Property# 38920005000: BERG, ALEX W TRS
					//Property# 09008438: TIPPETT GILBERT A SR ;         Property# 08025207: NIMMERRICHTER PAUL R 
					names = StringFormats.parseNameNashville(currentToken, true);	
				}
					
				String[] types = GenericFunctions.extractAllNamesType(names);
				String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
					
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);												
			}
			
			try {
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void parseTaxes(String detailsHtml, ResultMap map, long searchId) {
		String baseAmount = "";
		String amountDue = "";
		String taxYear = "";
		String amountPaid = "";
		String receiptNo = "";
		double priorDelinq = 0;
		double ad = 0;
		
		HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
		NodeList nodeList = htmlParser.getNodeList();
		
		TableTag table = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id","taxInfoTable"), true).elementAt(0);
		ResultTable receipts = new ResultTable();
		Map<String, String[]> tmpMap = new HashMap<String, String[]>();
		String[] header = { "ReceiptAmount", "ReceiptNumber" };
		List<List<String>> bodyRT = new ArrayList<List<String>>();
		NumberFormat formatter = new DecimalFormat("#.##");	
		
		if (table != null) {
			TableRow[] rows = table.getRows();
			if (rows.length > 1) {
				for (int i = 1; i < rows.length; i++ ) {
					TableColumn[] cols = rows[i].getColumns();
					if (cols.length == 8) {
						if (i == 1) { // taxes for current year
							taxYear = cols[0].getChildrenHTML().trim();
							if (StringUtils.isNotEmpty(taxYear)) {
								map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
							}
							
							baseAmount = cols[4].getChildren().elementAt(1).getChildren().toHtml().replaceAll("(?is)&nbsp;", "").trim();
							if (StringUtils.isNotEmpty(baseAmount)) {
								map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
							}
							
							amountDue = cols[7].getChildren().elementAt(1).getChildren().toHtml().replaceAll("(?is)&nbsp;", "").trim();
							if (StringUtils.isNotEmpty(amountDue)) {
								map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
							}
						}
						
						try {
							receiptNo = cols[3].getChildren().elementAt(1).getChildren().toHtml().replaceAll("(?is)&nbsp;", "").trim();
							amountPaid = cols[6].getChildren().elementAt(1).getChildren().toHtml().replaceAll("(?is)&nbsp;", "").trim();
							amountDue = cols[7].getChildren().elementAt(1).getChildren().toHtml().replaceAll("(?is)&nbsp;", "").trim();
							ad = Double.parseDouble(amountDue);
							
							if (StringUtils.isNotEmpty(amountPaid)) {
								if (i == 1) {
									map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
								}
								map.put(TaxHistorySetKey.RECEIPT_AMOUNT.getKeyName(), amountPaid);
							}
							if (StringUtils.isNotEmpty(receiptNo)) {
								map.put(TaxHistorySetKey.RECEIPT_NUMBER.getKeyName(), receiptNo);
							}
							List<String> paymentRow = new ArrayList<String>();
							if (!"0.00".equals(amountPaid) && StringUtils.isNotEmpty(receiptNo)) {
								paymentRow.add(amountPaid);
								paymentRow.add(receiptNo);
								bodyRT.add(paymentRow);
							} else if ("0.00".equals(amountPaid) && ad != 0) {
								if (i > 1) {
									priorDelinq +=  ad;
									priorDelinq = Double.valueOf(priorDelinq);
								}
							}
								
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		} 

		tmpMap.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
		tmpMap.put("ReceiptNumber", new String[] { "ReceiptNumber", "" });
		try {
			receipts.setHead(header);
			receipts.setMap(tmpMap);
			receipts.setBody(bodyRT);
			receipts.setReadOnly();
			map.put("TaxHistorySet", receipts);	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		map.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), formatter.format(priorDelinq));
	}
	
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse rsResponse, String detailsHtml, ResultMap map) {
		try {	
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
			
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList list = htmlParser.getNodeList();
			
			TableTag table = (TableTag) list.extractAllNodesThatMatch
					(new HasAttributeFilter("id","ownerInfoTable"), true).elementAt(0);
			if (table != null) {
				if (table.getRowCount() == 5) {
					TableRow row =  table.getRow(0);  // OWNER NAME
					if (row.getColumnCount() > 1) {
						TableColumn col = row.getColumns()[1];
						String names = col.getChildrenHTML().trim();							
						if (StringUtils.isNotEmpty(names)) {
							map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), names);	
						}
					}
					
					row =  table.getRow(1); // PROPERTY NO
					if (row.getColumnCount() > 1) {
						TableColumn col = row.getColumns()[1];
						String apn = col.getChildrenHTML().trim();							
						if (StringUtils.isNotEmpty(apn)) {
							map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), apn);	
						}
					}
					
					row =  table.getRow(3); // ADDRESS
					if (row.getColumnCount() > 1) {
						TableColumn col = row.getColumns()[1];
						String address = col.getChildrenHTML().trim();							
						if (StringUtils.isNotEmpty(address)) {
							map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);	
						}
					}
					
					row =  table.getRow(4); // TOTAL DUE 
					if (row.getColumnCount() > 1) {
						TableColumn col = row.getColumns()[1];
						String amountDue = col.getChildrenHTML().trim();
						amountDue = amountDue.replaceAll("(?is)(?:</?b>|<(?:/font>|font[^>]+>))", "").trim();
						if (StringUtils.isNotEmpty(amountDue)) {
							map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);	
						}
					}
				}
			}
			
			//LEGAL DESCRIPTION
			table = (TableTag) list.extractAllNodesThatMatch
					(new HasAttributeFilter("id","legalDescTable"), true).elementAt(0);
			boolean hasLot = false;
			boolean hasBlock = false;
			TableRow row = null;
			TableColumn col = null;
			
			int posForMap = 0;
			int posForGrid = 0;
			int posForParcel = 0;
			
			if (table != null) {
				int size= table.getRowCount();
				int index = size - 1;
				
				row = table.getRow(index); 
				col = row.getColumns()[0];
				if (size == 5) {
					if (col.getChildrenHTML().contains("BLOCK")) {
						hasBlock = true;
					}
					row = table.getRow(index-1); 
					col = row.getColumns()[0];
					if (col.getChildrenHTML().contains("LOT")) {
						hasLot = true;
					}
				} else if (size > 5 && size <= 9) { 
					index = size - 1;
					String info = col.getChildrenHTML();
					while (!info.contains("BLOCK"))
					{
						index --;
						row = table.getRow(index); 
						col = row.getColumns()[0];
						info = col.getChildrenHTML();
					}
					if (index > 0) {
						hasLot = true;
						hasBlock = true;
					}
				} else {
					hasLot = true;
					hasBlock = true;
				}
				
				String lot = "(?is)\\s*<div[^>]+>\\s*LOT\\s*=?\\s*([^<]+)?</div>";
				String block = "(?is)\\s*<div[^>]+>\\s*BLOCK\\s*=?\\s*([^<]+)?</div>";
				String parcel = "(?is)\\s*<div[^>]+>\\s*PARCEL\\s*=?\\s*([^<]+)?</div>";
				String grid = "(?is)\\s*<div[^>]+>\\s*GRID\\s*=?\\s*([^<]+)?</div>";
				String mapp = "(?is)\\s*<div[^>]+>\\s*MAP\\s*=?\\s*([^<]+)?</div>";
				Pattern p;
				Matcher m;
				
				if (hasBlock && hasLot) {
					row = table.getRow(index);  //Block
					col = row.getColumns()[0];
					p = Pattern.compile(block);
					m = p.matcher(col.getChildrenHTML());
					if (m.find()) {
						block = m.group(1)!= null ? m.group(1).trim() : "";
						if (StringUtils.isNotEmpty(block)) {
							map.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
						}
					} 
					
					row = table.getRow(index-1);  //Lot
					col = row.getColumns()[0];
					p = Pattern.compile(lot);
					m = p.matcher(col.getChildrenHTML());
					if (m.find()) {
						lot = m.group(1)!= null ? m.group(1).trim() : "";
						if (StringUtils.isNotEmpty(lot)) {
							map.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
						}
					}
					
//					posForMap = size - 5;
//					posForGrid = size - 4;
//					posForParcel = size - 3;
					posForParcel = index - 2;
					posForGrid = index - 3;
					posForMap = index - 4;
					
					
				} else {
//					posForMap = size - 3;
//					posForGrid = size - 2;
//					posForParcel = size - 1;
					posForParcel = index;
					posForGrid = index - 1;
					posForMap = index - 2;
				}
				
				
				row = table.getRow(posForParcel);  //PARCEL 
				col = row.getColumns()[0];
				p = Pattern.compile(parcel);
				m = p.matcher(col.getChildrenHTML());
				if (m.find()) {
					parcel = m.group(1)!= null ? m.group(1).trim().replaceFirst("(?is)\\b0+", "") : "";
				}
				
				row = table.getRow(posForGrid);  //GRID 
				col = row.getColumns()[0];
				p = Pattern.compile(grid);
				m = p.matcher(col.getChildrenHTML());
				if (m.find()) {
					grid = m.group(1)!= null ? m.group(1).trim().replaceFirst("(?is)\\b0+", "") : "";
				}
				
				row = table.getRow(posForMap);  //MAP
				col = row.getColumns()[0];
				p = Pattern.compile(mapp);
				m = p.matcher(col.getChildrenHTML());
				if (m.find()) {
					mapp = m.group(1)!= null ? m.group(1).trim().replaceFirst("(?is)\\b0+", "") : "";
					if (StringUtils.isNotEmpty(mapp) || StringUtils.isNotEmpty(grid) || StringUtils.isNotEmpty(parcel)) {
						map.put(PropertyIdentificationSetKey.PARCEL_ID_MAP.getKeyName(), mapp + "-" + grid + "-" + parcel);
					}
				}
				
				String subdivName = "";
				
				for (int i=0; i < posForMap; i++) {
					row = table.getRow(i); 
					col = row.getColumns()[0];
					subdivName += col.getChildren().elementAt(1).getFirstChild().toHtml().trim() + " ";
				}
				
				
				if (subdivName.matches("(?is).*\\bPH(?:ASE)?\\b\\s*(\\d+).*")) {
					String phase = subdivName.replaceFirst("(?is).*\\s*PH(?:ASE)?\\s*(\\d+).*", "$1").trim();
					if (StringUtils.isNotEmpty(phase)) {
						map.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
						subdivName = subdivName.replaceFirst("(?is)\\s*PH(?:ASE)?\\s*\\d+\\s*-?", " ");
					}
				}
				
				
				subdivName = subdivName.replaceAll("(?is)\\bUNIT\\b\\s*(?:[A-Z-])?\\s*\\d+", " ");
				subdivName = subdivName.replaceAll("(?is)ACCOUNT DELETED COMBINED TO \\d+", " ");
				subdivName = subdivName.replaceAll("(?is)PLA?T\\s*-?\\d+=?", " ");
				subdivName = subdivName.replaceAll("(?is)\\s*\\bPARCEL\\b\\s*[\\dA-Z-]+\\s*"," ");
				subdivName = subdivName.replaceAll("(?is)\\s*P/O[A-Z\\s]+(?:\\d+[A-Z]-)?[\\d\\.]+\\s*ACS?\\s*", " ");
				subdivName = subdivName.replaceAll("(?is)\\s*(?:IMPS(?:[\\d\\.]+|" + 
							"PARCEL[\\d\\.-=\\s]+|" + 
							"(?:(?:P/O)?\\s*(?:LOTS?|L))\\s*-?\\s*[\\d\\.,\\s=&]+)\\s*" +
							"((?:BLK|B-)\\s*[A-Z\\d]\\s*)?\\s*" +
							"((?:SEC|S-)\\s*[A-Z\\d-]+\\s*)?)" +
							"(?:\\s*\\b(?:AC|[\\w\\s]+NEIGHBORHOOD|ALL)\\b\\s*(?:L(?:O?T)?\\s*-?\\d+)?)?","$1 $2 ");
				
				if (subdivName.matches("(?is).*\\b(?:BLDG|BUILDING)\\b\\s*([A-Z]?\\d+).*")) {
					String bldg = subdivName.replaceFirst("(?is).*\\b(?:BLDG|BUILDING)\\b\\s*([A-Z]?\\d+).*", "$1").trim();
					if (StringUtils.isNotEmpty(bldg)) {
						map.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg.trim());
						subdivName = subdivName.replaceFirst("(?is)\\s*\\b(?:BLDG|BUILDING)\\b\\s*([A-Z]?\\d+)\\s*", " ");
					}
				}
				
				if (subdivName.matches("(?is).*\\b(?:BLK|B-)\\b\\s*(?:[A-Z]|\\d+)\\s*\\s*.*")) {
					String blk = subdivName.replaceFirst("(?is).*\\b(?:BLK|B-)\\b\\s*([A-Z]|\\d+).s*", "$1").trim();
					if (StringUtils.isNotEmpty(blk)) {
						map.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk.trim());
						subdivName = subdivName.replaceFirst("(?is)\\s*\\b(?:BLK|B-)\\b\\s*([A-Z]|\\d+)\\s*", " ");
					}
				}
				
				if (subdivName.matches("(?is).*\\b(?:SEC|S-)\\b\\s*([A-Z\\d-])+.*")) {
					String sec= subdivName.replaceFirst("(?is).*(?:SEC|S-)\\s*([A-Z\\d-])+.*", "$1").trim();
					if (StringUtils.isNotEmpty(sec)) {
						map.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec.trim());
						subdivName = subdivName.replaceFirst("(?is)\\s*\\b(?:SEC|SC|S-)\\s*([A-Z\\d-])+\\s*", " ");
					}
				}

				if (subdivName.contains("ST CHARLES SUB")) {
					if (subdivName.matches("(?is)\\s*[A-Z\\s']+\\s*\\bST\\b\\s*CHARLES\\s*\\bSUB\\b\\s*")  || 
						subdivName.matches("(?is)\\s*[\\dA-Z\\s'-]+(?:NBH|NEIBURHUD)\\s*\\bST\\b\\s*CHARLES\\s*\\bSUB\\b\\s*(?:-[A-Z]+)\\s*")) {
							subdivName = "ST CHARLES";
							map.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivName.trim());
							subdivName = "";
					}
				}
				
				
				subdivName = subdivName.replaceAll("(?is)-?\\s*[\\d\\.]+\\s*ACS?", " ");
				subdivName = subdivName.replaceAll("(?is)\\s*\\bOFF\\b\\s*RT\\s*\\d+"," ");
				subdivName = subdivName.replaceAll("(?is)\\s*[A-Z\\d'/\\s]+\\s*(?:R(?:OA)?D|ST(?:REET)?|AVE?(?:NUE)?)\\s*(?:FMA)?(?:[A-Z\\s]+\\bTRACK\\b)?", " ");
				subdivName = subdivName.replaceAll("(?is)\\s*PL(?:ACE|T)?\\s*\\d+", " ");
				subdivName = subdivName.replaceAll("(?is)\\s*\\b(?:LO?T|BL(?:OC)?K|S(?:EC(?:TION)?)?)\\b\\s*(?:[A-Z\\.-])?\\d+", " ");
				subdivName = subdivName.replaceAll("(?is)\\s*IMPS\\s*LO?T\\s*\\d+\\s*", " ");
				subdivName = subdivName.replaceAll("(?is)\\s*\\b(IND HD|Indian Head|(?:TOWN OF )?LA PLATA|Port Tobacco(?:\\s*Village)?)\\s*", " ");
				
				if (subdivName.matches("(?is)\\s*([^\\$]+)\\s*(?:\\bSUB\\b).*")) {
					subdivName = subdivName.replaceFirst("(?is)\\s*([^\\$]+)\\s*(?:\\bSUB\\b).*", "$1");
				} else if (subdivName.matches("(?is)\\s*(?:[A-Z\\s]+ADDN?)?\\s*([^\\$]+)\\s*")) {
					subdivName = subdivName.replaceFirst("(?is)\\s*(?:[A-Z\\s]+ADDN?)?\\s*([^\\$]+)\\s*", "$1");
				} else if (subdivName.matches("(?is)\\s*([A-Z\\s]+)\\s*\\bIND\\b\\s*H(?:EA)?D.*")) {
					subdivName = subdivName.replaceFirst("(?is)\\s*([A-Z\\s]+)\\s*\\bIND\\b\\s*H(?:EA)?D\\s*", "$1");
				}
				
				if (StringUtils.isNotEmpty(subdivName)) {
					map.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivName.trim());
				}
			}
			
			parseDetails(detailsHtml, map, searchId);
			
			if (StringUtils.isEmpty((String) map.get(TaxHistorySetKey.YEAR.getKeyName()))) {
				DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
				int taxYear = Integer.parseInt(site.getPayDate().toString().replaceAll("(?is)(?:.*)(?:[A-Z]+T|WIB)\\s*(\\d{4})", "$1"));
				map.put(TaxHistorySetKey.YEAR.getKeyName(), Integer.toString(taxYear));
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return null;	
	}
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
			
		
		
		
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX)); 
			moduleList.add(module);
		}
		
		if(hasStreet()) {
			FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId , 0.8d , true);
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX)); 
			module.clearSaKeys();
			
			module.setSaKey(2, SearchAttributes.P_STREETNAME);
			
			if (hasStreetNo()) {
				module.setSaKey(0, SearchAttributes.P_STREETNO);
			}
			if (StringUtils.isNotEmpty(getSearchAttribute(SearchAttributes.P_STREETDIRECTION))) {
				module.setSaKey(1, SearchAttributes.P_STREETDIRECTION);
			}
			
			module.setSaKey(2, SearchAttributes.P_STREETNAME);
			
//			if (StringUtils.isNotEmpty(getSearchAttribute(SearchAttributes.P_STREETSUFIX))) {
//				module.setSaKey(3, SearchAttributes.P_STREETSUFIX);
//			}
			module.addFilter(addressFilter);
			moduleList.add(module);		
		}
		
		if (hasOwner()) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX)); 
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
//			module.setSaKey(0, SearchAttributes.OWNER_LNAME);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
//			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] {"L F;;","L M;;"}));
			
			moduleList.add(module);		
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
}

package ro.cst.tsearch.servers.types;

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

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class MDHarfordTR extends TSServer {

	private static final long serialVersionUID = 1L;
	
	public MDHarfordTR (long searchId) {
		super(searchId);
	}
	
	
	public MDHarfordTR (String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

//	private static String[] citites = { "Aberdeen Proving Ground", "Bel Air North", "Bel Air South", "Bel Air", "Edgewood", "Fallston", "Jarrettsville", "Joppatowne", "Perryman", "Pleasant Hills", "Riverside",
//		"Abingdon", "Belcamp", "Cardiff", "Churchville", "Darlington", "Gunpowder", "Castleton", "Dublin", "Forest Hill", "Hickory", "Level", "Norrisville", "Pylesville", "Whiteford", "White Hall" };
	
	
	public String splitAddress (String s) {
		String address = s;
		String cities = "(Bountiful|Centerville|Clearfield|Clinton|Farmington|Fruit Heights|Kaysville|Layton|Salt Lake|"
				+ "Weber|Sunset|Syracuse|West Bountiful|West Point|Woods Cross)";
		// get the city
		String cityRegEx = "(?is)(NORTH|EAST|WEST)? " + cities + "( CITY)?$";

		String city = RegExUtils.getFirstMatch(cityRegEx, address, 0);
		address = address.replaceAll(city, "");
		
		return address;
	}
	
	
//	protected void loadDataHash(HashMap<String, String> data, String taxYear) {
	protected void loadDataHash(HashMap<String, String> data) {
		if(data != null) {
			data.put("type","CNTYTAX");
//			data.put("year", taxYear);
		}
	}
	
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		if (table.contains("NO RECORD FOUND FOR")) {
			return intermediaryResponse;
			
		} else
		try {
			HtmlParser3 htmlParser = new HtmlParser3(table);	
			NodeList nodeList = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true);
						
			if(nodeList.size() == 0) {
				return intermediaryResponse;
			}
			
			String header = ""; 
			if (table.contains("Search results are LIMITED to 50 records.")) {
				NodeList tmp = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"), true);
				if (tmp.size() >= 3) {
					header += tmp.elementAt(1).toHtml().trim();
					header += "<font color=\"red\">" + tmp.elementAt(2).toHtml().replaceFirst("(?is)\\s*<a[^>]+>([^<]+)</a>\\s*", "$1");
					header += "</font> <br>";
				}
				SearchLogger.info(header, searchId);
			}
							
			
			TableTag tableTag = (TableTag) nodeList.elementAt(0);
			TableRow[] rows  = tableTag.getRows();
			
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 3) {
					ParsedResponse currentResponse = new ParsedResponse();
					TableColumn col = row.getColumns()[0];
					String adrValue = col.getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0).getChildren()
							.elementAt(0).toHtml().trim();
					col.removeChild(3);
					col.getChildren().elementAt(2).setText(adrValue);
					
					col = row.getColumns()[2];
					LinkTag linkTag = ((LinkTag)col.getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					String link = linkTag.extractLink().trim().replaceAll("\\s", "%20");
					String url = "";
					String district = "district=";
					String index = "index=";
					
					String regExp = "(?is)[^?]+\\?\\s*PID=(\\d{2})(\\d{6})";
					Matcher m  = Pattern.compile(regExp).matcher(link);
					
					if (m.find()) {
						district += m.group(1);
						index += m.group(2);
						url = "TaxBill.aspx?" + district + "&" + index;
					}
					
					link = CreatePartialLink(TSConnectionURL.idGET) +  url;
					
					linkTag.setLink(link);
					
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap map = parseIntermediaryRow(row, searchId);
					Bridge bridge = new Bridge(currentResponse, map, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);					
				}
			}
					
			response.getParsedResponse().setHeader(header + "<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n" + 
					"<tr>  <th>Street Address</th> <th>Property ID</th> <th>View Bills</th>  </tr>");
			response.getParsedResponse().setFooter("</table>");
		
			outputTable.append(table);
						
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}

	
	public ResultMap parseIntermediaryRow(TableRow row, long searchId) {
			ResultMap resultMap = new ResultMap();
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			
			TableColumn[] cols = row.getColumns();
			if(cols.length == 3) {	
				String address = cols[0].toPlainTextString().trim();
				
				if (StringUtils.isNotEmpty(address)) {
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				}
				
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),cols[1].toPlainTextString().trim());
			}
			return resultMap;
		}


	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		// 	no result returned - error message on official site
		if (rsResponse.contains("An unexpected error has occurred:")) {
			Response.getParsedResponse().setError("Official site not functional");
    		return;
    	} else if (rsResponse.indexOf("NO RECORD FOUND FOR") > -1) {
			Response.getParsedResponse().setError("Please check your information and try again.");
			return;
    	} else if (!rsResponse.contains("Records Returned for Search Result")) {
    		if (rsResponse.indexOf("<table") == -1) { //there is just one result
    			if (rsResponse.indexOf("<IFRAME") == -1) {
    				Response.getParsedResponse().setError("No results! Change your search criteria");
    				return;
    			}
    		}    			
		}
		
		switch (viParseID) {			
			case ID_SEARCH_BY_ADDRESS:		
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
	            }			
				break;
				
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_PROP_NO:
				StringBuilder accountId = new StringBuilder();
				String details = getPageDetails(rsResponse, accountId);
				String filename = accountId + ".html";
				String taxYear = "";
//				NodeList list = new HtmlParser3(details).getNodeList();	
//				taxYear = list.extractAllNodesThatMatch(new HasAttributeFilter("id", "lblBillingPeriodText"),true).elementAt(0).toHtml().trim();
//				taxYear = taxYear.replaceFirst("(?is)<span[^>]+>(\\d{4})[^>]+>", "$1");
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					//loadDataHash(data, taxYear);
					loadDataHash(data);
					if (!"Cannot return details!".equals(details)) {
						if (isInstrumentSaved(accountId.toString(),null,data)){
							details += CreateFileAlreadyInTSD();
						
						} else {
							mSearch.addInMemoryDoc(sSave2TSDLink, details);					
							details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
						}
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
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
				
			default:
				break;
		}
	}	

	
	protected String createLink (String typeOfDetails, String accountID, String year) {
		String url = "http://bills.harfordcountymd.gov/";
		String aspxPage = "";
		
		if (typeOfDetails.contains("REAL ESTATE TAXES")) {
			//real estate tax info
			aspxPage = "TaxBillDetails.aspx"; 
			aspxPage = aspxPage + "?AccountNumber=" + accountID  + "&cycle=1&BillingYear=" + year;
			
		} else if (typeOfDetails.contains("BENEFIT ASSESSMENT")) {
			//benefit assessment info
			aspxPage = "AssessmentsTaxDetails.aspx"; 
			aspxPage = aspxPage + "?AccountNumber=" + accountID + "&cycle=1&BillingYear=" + year;
			
		} else if (typeOfDetails.contains("WATER SEWER")) {
			//water sewer usage info
			aspxPage = "WaterSewer.aspx"; 
			aspxPage = aspxPage + "?AccountNumber=" + accountID;
		}
		
		url =  url + aspxPage;
		
		return url;
	}
	
	
	
	protected void appendInfoFromLink (StringBuilder sb, TableTag table, String year) {
		TableTag tb = table;
		int size = tb.getRowCount()-1;
		
		String link = "";
		
		if (size >= 1) {
			//extract links
			for (int i = 0; i < size; i ++) {
				TableRow row = tb.getRow(i+1);
				TableColumn col = row.getColumns()[0];
				String info = "";
				
				if (col != null) {
					String accountID = col.getChildrenHTML().trim();
					accountID = accountID.replaceAll("([A-Z])\\s*-(\\d{2})\\s*-\\s*(\\d{6})\\s*", "$1-$2-$3");
					
					if (StringUtils.isNotEmpty(accountID)) {
						String billDescription = row.getColumns()[1].getChildrenHTML().trim();
						
						link = createLink(billDescription, accountID, year);
					}
					
					String content = getLinkContents(link);
					
					if (link.contains("TaxBillDetails")) {
						info = getRealTaxes(content);
						
					} else if (link.contains("AssessmentsTaxDetails")) {
						info = getUserBenefitTaxes(content);
						
					} else if (link.contains("WaterSewer")) {
						info = getWaterSewerTaxes(content);
					}
					
					sb.append("<tr><td> <br>");
					sb.append(info);
					sb.append("</td></tr>");
				}
			}
		}
		
		return;
	}
	
	protected String getPageDetails(String rsResponse, StringBuilder accountId) {
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				return rsResponse;
			
			} else {
				try {
					StringBuilder details = new StringBuilder();	
					HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
					
					NodeList nodeList = htmlParser.getNodeList()
							.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "screendump"), true);				
					
					if(nodeList.size() == 0) {
						nodeList = htmlParser.getNodeList();
						
						if (nodeList.size() == 0) {
							return rsResponse;
						
						} else if (nodeList.size() == 5) {
							nodeList = nodeList.elementAt(3).getChildren().elementAt(4).getChildren()
									.extractAllNodesThatMatch(new TagNameFilter("iframe"));
							String redirectLink = nodeList.toHtml().trim();
							redirectLink = redirectLink.replaceFirst("(?is)<iframe[^/]+//([^\\\"]+)\\\">", "http://$1");
							
							if (StringUtils.isNotEmpty(redirectLink)) {
								String rsp = getLinkContents(redirectLink);
								rsResponse = getPageDetails(rsp, accountId);
							}
						}
					
					} else {						
						NodeList detailsList = nodeList.elementAt(0).getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("table"), true); 
						
						if (detailsList.size() == 0) {
							details.append("</td></tr> </table> </br>");							
							return details.toString();
						}
						else {
							TableTag detailsTable = (TableTag) detailsList.elementAt(0);
							String info = "";
							
							if (detailsTable != null) {
								detailsTable.setAttribute("id", "\"apnInfoTable\"");
								
								String apn = detailsTable.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "lblPropertyIdText"), true)
										.elementAt(0).toHtml().trim();
								apn = apn.replaceFirst("(?is)<span[^>]+>\\s*(\\d{2})\\s*-\\s*(\\d{6})\\s*</span>", "$1-$2");
								accountId.append(apn);
								
								info = detailsTable.toHtml();
								info = info.replaceAll("(?is)<div>\\s*<font[^>]+>\\s*<a[^>]+>[^<]+</a>\\s*</font>\\s*</div>", "");
								info = info.replaceAll("(?is)(?:<tr>\\s*<td[^>]+\\s*[^<]+<div[^>]+>\\s*)?<a[^>]+>\\s*(?:<img[^>]+>|<b>[^<]+</b>)\\s*</a>(?:\\s*</div>\\s*</td>\\s*</tr>)?", "");
								info = info.replaceAll("(?is)<span[^>]+>([^<]+)\\s*</span>", "$1");
								details.append(info);
								details.append("</td></tr>");	
							}
							
							detailsTable = (TableTag) detailsList.elementAt(6);
							if (detailsTable != null) {
								info = detailsTable.toHtml();
								info = info.replaceAll("(?is)<script[^!]+!--[^>]+>\\s*</script>","");
								info = info.replaceFirst("(?is)\\s*<tr>\\s*<td[^>]+>\\s*<div\\s*id=\\\"TaxFooter\\\"\\s*>.*</tr>","");
								info = info.replaceAll("(?is)\\s*<a[^>]+>(Bill Stub)\\s*</a>","$1");
								info = info.replaceAll("(?is)\\s*<a[^>]+>(Pay Bill)\\s*</a>","$1");
								info = info.replaceAll("(?is)onmousedown=\\\"[^\\\"]+\\\"\\s","");
								info = info.replaceAll("(?is)<a [^:]+:__doPostBack[^>]+>\\s*(View Details)\\s*</a>", "$1");
								info = info.replaceFirst("(?is)(<table id\\s*=\\s*\\\"grdTaxCurr_DXMainTable\\\")[^>]+>","$1 cellspacing=\"1\" cellpadding=\"1\" aliagn=\"left\" style=\"width:100%;\">");
								info = info.replaceFirst("(?is)(<table id\\s*=\\s*\\\"grdTaxesPrev_DXMainTable\\\")[^>]+>","$1 cellspacing=\"1\" cellpadding=\"1\" aliagn=\"left\" style=\"width:100%;\">");
								info = info.replaceAll("(?is)([A-Z])\\s*-\\s*(\\d{2})\\s*-\\s*(\\d{6})", "$1-$2-$3");
								
								details.append("<tr><td>" + info + "</td></tr>");
								
								//Details for Current Tax Year
								String year = ((TableTag) detailsList.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdTaxCurr_DXTitle")).elementAt(0))
										.getRow(0).getColumns()[0].getChildrenHTML().trim();
								year = year.replaceFirst("(?is)\\s*(\\d{4})(?:\\s*|&nbsp;)PROPERTY BILLS", "$1");
								
								TableTag tmpTable = (TableTag) detailsList.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdTaxCurr_DXMainTable")).elementAt(0);
								
								//append details from links / current tax year table
								appendInfoFromLink(details, tmpTable, year);
								

								//Details for Previous Tax Year
								year = ((TableTag) detailsList.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdTaxesPrev_DXTitle")).elementAt(0))
										.getRow(0).getColumns()[0].getChildrenHTML().trim();
								year = year.replaceFirst("(?is)\\s*(\\d{4})(?:\\s*|&nbsp;)PROPERTY BILLS", "$1");
								
								details.append("<tr><td colspan=\"5\"></br><div align=\"center\" id=\"taxHistoryTable\"><h2> <b> :: TAX HISTORY :: </b> </h2></div> </td></tr>");	
								
								tmpTable = (TableTag) detailsList.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdTaxesPrev_DXMainTable")).elementAt(0);
								
								//append details from links / current tax year table
								appendInfoFromLink(details, tmpTable, year);
							}

							details.append("</table> </br></br>");
						
							return details.toString();
						}
					}
					
				} catch (Throwable t){
					logger.error("Error while parsing details page data", t);
					if (rsResponse != null) {
						return "Cannot return details!";
					}
				}
			}
		
		return rsResponse;
	}

	
	private String getWaterSewerTaxes(String rspWaterSewer) {
		String response = rspWaterSewer;
		response = response.replaceFirst("(?is).*(<div id=\"screendump\">)", "$1");
		response = response.replaceFirst("(?is)\\s*</form>\\s*</body>\\s*</html>","");
		response = response.replaceFirst("(?is)<div[^>]+>\\s*<input[^>]+>\\s*</div>","");
		response = response.replaceFirst("(?is)(<fieldset>\\s*<legend[^>]+>\\s*<b>\\s*(?:<img[^>]+>)?[^/]+/b>\\s*</legend>\\s*<table[^>]+)","$1" + " id=\"currentYearAssessmentsTaxDetails\"");
		response = response.replaceAll("(?is)<script[^>]+>[^_]+[^<]+</script>","");
		response = response.replaceAll("(?is)<script[^>]+>\\s*</script>", "");
		response = response.replaceAll("(?is)<input[^>]+>","");
		response = response.replaceAll("(?is)(?:<td id=\\\"(?:tdCredit|tdCheck|tdCoupon)\\\">\\s*)?<a [^>]+>\\s*<img[^>]+>\\s*</a>(?:\\s*</td>\\s*)?","");
		response = response.replaceFirst("(?is)(?:(?:<span[^>]+>\\s*)?<a[^>]+>\\s*)?<img[^>]+>\\s*(?:</a>\\s*(?:</span>)?)?","");
		response = response.replaceAll("(?is) by the Bank\\.", "@");
		response = response.replaceAll("(?is)\\s*<div[^>]+>\\s*<div[^>]+?>\\s*<html>\\s*<head>\\s*<[^/]+/[^>]+>[^>]+>[^@]+@\\s*</div>\\s*</td>\\s*</tr>\\s*</table>\\s*</body>\\s*</html>\\s*</div>\\s*</div>\\s*","");
		response = response.replaceFirst("(?is)<div>\\s*<font[^>]+>\\s*<a[^>]+>Link to[^<]+</a>\\s*</font>\\s*</div>", "");
		response = response.replaceAll("(?is)If (?:the )?property owner[^<]+<div[^>]+>\\s*<a [^>]+>[^/]+/b>\\s*</a>\\s*</div>","");
		response = response.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1");
		response = response.replaceAll("(?is)<tr>\\s*<td[^>]+>\\s*<div id=\\\"TaxFooter\\\">.*(</table>)","$1");
		return response;
	}


	private String getUserBenefitTaxes(String rspUserBenef) {
		String response = rspUserBenef;
		response = response.replaceFirst("(?is).*(<div id=\"screendump\">)", "$1");
		response = response.replaceFirst("(?is)\\s*</form>\\s*</body>\\s*</html>","");
		response = response.replaceFirst("(?is)<div[^>]+>\\s*<input[^>]+>\\s*</div>","");
		response = response.replaceFirst("(?is)(<fieldset>\\s*<legend[^>]+>\\s*<b>\\s*(?:<img[^>]+>)?[^/]+/b>\\s*</legend>\\s*<table[^>]+)","$1" + " id=\"currentYearAssessmentsTaxDetails\"");
		response = response.replaceFirst("(?is)(<legend[^<]+<b>[^>]+>\\s*\\d+ )([^>]+>\\s*</legend>)", "$1" + "USER BENEFIT ASSESSMENT INFORMATION" + "$2");
		response = response.replaceAll("(?is)<script[^>]+>[^_]+[^<]+</script>","");
		response = response.replaceAll("(?is)<script[^>]+>\\s*</script>", "");
		response = response.replaceAll("(?is)<input[^>]+>","");
		response = response.replaceAll("(?is)(?:<td id=\\\"(?:tdCredit|tdCheck|tdCoupon)\\\">\\s*)?<a [^>]+>\\s*<img[^>]+>\\s*</a>(?:\\s*</td>\\s*)?","");
		response = response.replaceFirst("(?is)(?:(?:<span[^>]+>\\s*)?<a[^>]+>\\s*)?<img[^>]+>\\s*(?:</a>\\s*(?:</span>)?)?","");
		response = response.replaceAll("(?is) by the Bank\\.", "@");
		response = response.replaceAll("(?is)\\s*<div[^>]+>\\s*<div[^>]+?>\\s*<html>\\s*<head>\\s*<[^/]+/[^>]+>[^>]+>[^@]+@\\s*</div>\\s*</td>\\s*</tr>\\s*</table>\\s*</body>\\s*</html>\\s*</div>\\s*</div>\\s*","");
		response = response.replaceFirst("(?is)<div>\\s*<font[^>]+>\\s*<a[^>]+>Link to[^<]+</a>\\s*</font>\\s*</div>", "");
		response = response.replaceAll("(?is)If (?:the )?property owner[^<]+<div[^>]+>\\s*<a [^>]+>[^/]+/b>\\s*</a>\\s*</div>","");
		response = response.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1");
		response = response.replaceAll("(?is)<tr>\\s*<td[^>]+>\\s*<div id=\\\"TaxFooter\\\">.*(</table>)","$1");
		return response;
	}


	private String getRealTaxes(String rspRealTaxes) {
		String response = rspRealTaxes;
		String tableID = "";
		
		String currentTaxYear = FormatDate.getDateFormat(FormatDate.PATTERN_yyyy).format(dataSite.getPayDate());
		 //in SS we have Tax Year Mode: PD_YEAR+1
		currentTaxYear = currentTaxYear + "+1";
		currentTaxYear = GenericFunctions1.sum(currentTaxYear, searchId);
		
		
		response = response.replaceAll("(?is)<script[^>]+>[^_]+[^<]+</script>","");
		response = response.replaceAll("(?is)<script[^>]+>\\s*</script>", "");
		response = response.replaceAll("(?is)<input[^>]+>","");
		response = response.replaceFirst("(?is).*(<div id=\"screendump\">)", "$1");
	
		if (response.contains(currentTaxYear + " REAL ESTATE TAXES TAX INFORMATION")) 
			tableID = "currentTaxYearInfo";
		else 
			tableID = "previousTaxYearInfo";
		
		response = response.replaceFirst("(?is)(<fieldset>\\s*<legend[^>]+>\\s*<b>\\s*(?:<img[^>]+>)?[^/]+/b>\\s*</legend>[^/]+/div>\\s*<table[^>]+)","$1" + " id=\"" + tableID +"\"");
		response = response.replaceFirst("(?is)\\s*</form>\\s*</body>\\s*</html>","");
		response = response.replaceFirst("(?is)(?:(?:<span[^>]+>\\s*)?<a[^>]+>\\s*)?<img[^>]+>\\s*(?:</a>\\s*(?:</span>)?)?","");
		response = response.replaceAll("(?is) by the Bank\\.", "@");
		response = response.replaceAll("(?is)\\s*<div[^>]+>\\s*<div[^>]+?>\\s*<html>\\s*<head>\\s*<[^/]+/[^>]+>[^>]+>[^@]+@\\s*</div>\\s*</td>\\s*</tr>\\s*</table>\\s*</body>\\s*</html>\\s*</div>\\s*</div>\\s*","");
		response = response.replaceFirst("(?is)<div>\\s*<font[^>]+>\\s*<a[^>]+>Link to[^<]+</a>\\s*</font>\\s*</div>", "");
		response = response.replaceAll("(?is)If (?:the )?property owner[^<]+<div[^>]+>\\s*<a [^>]+>[^/]+/b>\\s*</a>\\s*</div>","");
		response = response.replaceAll("(?is)(?:<td id=\\\"(?:tdCredit|tdCheck|tdCoupon)\\\">\\s*)?<a [^>]+>\\s*<img[^>]+>\\s*</a>(?:\\s*</td>\\s*)?","");
		response = response.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1");
		response = response.replaceAll("(?is) <tr>\\s*<td[^>]+>\\s*<div id=\\\"TaxFooter\\\">.*(</table>)","$1");
		
		//rename table with tax payments
		response = response.replaceFirst("(?is)(<table id=\")grdPayment_DXMainTable", "$1" + "realEstateTaxPayments");
		
		return response;
	}


	protected String getTaxHistory(String extractedLink) {
		String taxHistoryHtml = getLinkContents(extractedLink);
		taxHistoryHtml = Tidy.tidyParse(taxHistoryHtml, null);
		
		try {
			HtmlParser3 htmlParser = new HtmlParser3(taxHistoryHtml);
						
			taxHistoryHtml = "<div> <span> :: Real Property Taxes :: </span> </div>";
			
			NodeList divList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("div"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "tvrSection"), false)
					.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("form"),false)
					.elementAt(0).getChildren();											
			
			if(divList.size() > 88) {
				divList.remove(88);
				divList.remove(58);
			}
			
			String htmlDetails = divList.toHtml().replaceAll("(?is)<input[^>]+>", "");
			taxHistoryHtml = taxHistoryHtml + htmlDetails;
			
		} catch (Exception e) {
			logger.error("Error while getting getTaxHistory " + extractedLink, e);
		}
		
		return taxHistoryHtml;
	}


	@Override
	protected Object parseAndFillResultMap(ServerResponse rsResponse, String detailsHtml, ResultMap map) {
		try {
			String accountID = "";
			String names = "";
			String address = ""; 
			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
			
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList list = htmlParser.getNodeList();	
			
			accountID = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(list, "Property ID:"),"",true).trim();
			accountID = accountID.replaceFirst("(?is)(<span[^>]+>\\s*(\\d{2})\\s*-\\s*(\\d{6})\\s*</span>)", "$1-$2");
			accountID = accountID.replaceFirst("(?is)(\\d{2})\\s*-\\s*(\\d{6})", "$1-$2");
			
			TableTag tmpTable = (TableTag)list.extractAllNodesThatMatch(new HasAttributeFilter("id", "apnInfoTable"), true).elementAt(0);
			tmpTable = (TableTag) tmpTable.getRows()[0].getColumns()[0].getChildren().elementAt(1);
			
			names = tmpTable.getRows()[1].getColumns()[1].getChildrenHTML().trim();
			names = names.replaceAll("(?is)<table[^>]+>(.*)</table>", "$1");
			names = names.replaceAll("(?is)</?tr>(\\s*<td>)?", "");
			names = names.replaceAll("(?is)\\s*</td>", ";").trim();
			
			address = ((TableTag)(tmpTable.getRows()[2].getColumns()[1].getChildren().elementAt(1)))
					.getRows()[0].getColumns()[0].getChildrenHTML().trim();
			
			if (StringUtils.isNotEmpty(accountID)) {
				map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountID);
			}
			
			if (StringUtils.isNotEmpty(names)) {
				map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),names);		
			}
			parseNames(map, searchId);
			
			if (StringUtils.isNotEmpty(address)) {
				map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
				map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			}
			
			String taxYear = "";
			taxYear = list.extractAllNodesThatMatch(new HasAttributeFilter("id", "lblBillingPeriodText"),true).elementAt(0).toHtml().trim();
			taxYear = taxYear.replaceFirst("(?is)<span[^>]+>(\\d{4})[^>]+>", "$1");
			
			if (StringUtils.isNotEmpty(taxYear)) {
				map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
			} else {
				taxYear = FormatDate.getDateFormat(FormatDate.PATTERN_yyyy).format(dataSite.getPayDate());
				 //in SS we have Tax Year Mode: PD_YEAR+1
				taxYear = taxYear + "+1";
				taxYear = GenericFunctions1.sum(taxYear, searchId);
				map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
			}
			
			TableTag tmp = (TableTag) list.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "grdTaxBill_DXMainTable")).elementAt(0);
			
			if (tmp != null) {
				TableRow[] rows = tmp.getRows();
				String baseAmt = "";
				String amountDue = "";
				String amountPaid = "";
				boolean withInstallments = false;
				int noOfRows = rows.length;
				
				if (noOfRows > 4) {
					TableRow row = rows[0];
					TableColumn col = row.getColumns()[0];
					if (col.getChildrenHTML().contains("1st Semi-Annual Installment")) {
						//BA should be calculated as a sum of values from first 2 rows
						withInstallments = true;
					}
					
					if (row.getColumnCount() == 3) {
						col = row.getColumns()[2];
						baseAmt =  col.getChildrenHTML().trim().replaceAll(",", "");
						
						if (withInstallments) {
							row = rows[1];
							col = row.getColumns()[2]; 
							String secondInst = col.getChildrenHTML().trim().replaceAll(",", "");
							baseAmt = baseAmt + "+" + secondInst;
							baseAmt = GenericFunctions.sum(baseAmt, searchId);
						}
					}
					
					if (StringUtils.isNotEmpty(baseAmt)) {
						map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmt);
					}

					row = rows[noOfRows-2]; // for AP
					col = row.getColumns()[0];
					if (col.getChildrenHTML().contains("Total Payments")) {
						col = row.getColumns()[2];
						amountPaid = col.getChildrenHTML().trim().replaceAll(",", "");
					}
					
					if (StringUtils.isNotEmpty(amountPaid)) {
						map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
					}
					
					row = rows[noOfRows-1]; // for AD
					col = row.getColumns()[0];
					if (col.getChildrenHTML().contains("TOTAL DUE")) {
						col = row.getColumns()[2];
						amountDue = col.getChildrenHTML().trim().replaceAll(",", "");
					}
					
					if (StringUtils.isNotEmpty(amountDue)) {
						map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
					}
				}
			}
				
			
			// parsing tax info from receipt table	
			
			ResultTable receipts = new ResultTable();
			Map<String, String[]> tmpMap = new HashMap<String, String[]>();
			String[] header = { "ReceiptAmount", "ReceiptDate" };
			List<List<String>> bodyRT = new ArrayList<List<String>>();
			
			list = list.extractAllNodesThatMatch(new HasAttributeFilter("id", "realEstateTaxPayments"), true);
			if (list != null) {
				String sumPaid = "";
				String lastPayment = "";
				for (int idx=0; idx < list.size(); idx++) {
					tmp = (TableTag) list.elementAt(idx);
					if (tmp != null) {
						TableRow[] rows = tmp.getRows();
						if (rows.length > 1) {
							for (int i=1; i < rows.length; i++) {
								String paidValue = "";
								String receiptDate = "";
									
								if (rows[i].getColumnCount() == 3) {
									TableColumn col = rows[i].getColumns()[0];
									receiptDate = col.getChildrenHTML().trim();
									lastPayment = receiptDate;
										
									col = rows[i].getColumns()[2];
									paidValue = col.getChildrenHTML().trim().replaceAll("[\\$,]", "");
										
									List<String> paymentRow = new ArrayList<String>();
									if (!"0.00".equals(paidValue) && StringUtils.isNotEmpty(receiptDate)) {
										paymentRow.add(paidValue);
										paymentRow.add(receiptDate);
										bodyRT.add(paymentRow);
										if (idx == 0) {
											if (StringUtils.isEmpty(sumPaid)) 
												sumPaid = paidValue;
											else 
												sumPaid = sumPaid + "+" + paidValue;
											sumPaid = GenericFunctions.sum(sumPaid, searchId);
										}
									}
								}
							}
						}
					}
					
					if (StringUtils.isNotEmpty(sumPaid) && idx == 0) {
						map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), sumPaid);
						map.put(TaxHistorySetKey.DATE_PAID.getKeyName(), lastPayment);
					}
				}
				
				tmpMap.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
				tmpMap.put("ReceiptDate", new String[] { "ReceiptDate", "" });
				
				try {
					receipts.setHead(header);
					receipts.setMap(tmpMap);
					receipts.setBody(bodyRT);
					receipts.setReadOnly();
					map.put("TaxHistorySet", receipts);	
				} catch (Exception e) {
					e.printStackTrace();
				}
					
			}
			
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return null;	
	}
	
	
	public void parseNames(ResultMap resultMap, long searchId) {
		String unparsedName = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		List<List> body = new ArrayList<List>();
		
		if(StringUtils.isNotEmpty(unparsedName)) {
			unparsedName = unparsedName.replaceAll("\\s*-\\s*(ETAL),?\\s*", " $1 - ");
			unparsedName = unparsedName.replaceAll("\\s*&\\s*WF;", ";");
			unparsedName = unparsedName.replaceAll("(?is)\\b(A|F)KA\\b", " - @@@FML@@@");
			unparsedName = unparsedName.replaceAll("(?is)\\s+-\\s+ETAL", " ETAL - ");
			unparsedName = unparsedName.replaceAll(";;", "");
			String[] mainTokens = unparsedName.split("\\s*;\\s*");
											
			for (int i = 0; i < mainTokens.length; i++) {
				String currentToken = mainTokens[i];
				String[] names = StringFormats.parseNameWilliamson(currentToken, true);	
					
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


	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId, true, numberOfYearsAllowed, true);
		
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.addFilter(taxYearFilter);
			moduleList.add(module);
		}
		if(hasStreet() && hasStreetNo()) {	
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX)); 
			module.addFilter(taxYearFilter);
			module.addFilter(addressFilter);
			moduleList.add(module);		
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
}

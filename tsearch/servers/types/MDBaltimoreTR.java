package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableHeader;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class MDBaltimoreTR extends TSServer {

	private static final long serialVersionUID = 1L;

	
	public MDBaltimoreTR(long searchId) {
		super(searchId);
	}
	
	
	public MDBaltimoreTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	
	protected void loadDataHash(HashMap<String, String> data) {
		if(data != null) {
			data.put("type","CNTYTAX");
		}
	}
	
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		if (table.contains("We could not locate any parcels that match")) {
			return intermediaryResponse;
			
		} else
		try {
			HtmlParser3 htmlParser = new HtmlParser3(table);	
			NodeList nodeList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "tvrDataGrid"), true);
						
			if(nodeList.size() == 0) {
				return intermediaryResponse;
			}
			
			TableTag tableTag = (TableTag) nodeList.elementAt(0);
			TableRow[] rows  = tableTag.getRows();
			
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 2) {
					ParsedResponse currentResponse = new ParsedResponse();
					LinkTag linkTag = ((LinkTag)row.getColumns()[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					
					String link = CreatePartialLink(TSConnectionURL.idGET) +  
						linkTag.extractLink().trim().replaceAll("\\s", "%20");
					
					linkTag.setLink(link);
					
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap map = ro.cst.tsearch.servers.functions.MDBaltimoreTR.parseIntermediaryRow(row, searchId); 
					Bridge bridge = new Bridge(currentResponse, map, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);					
				}
			}
			

			StringBuilder footer =  getPrevAndNextLinks (response,htmlParser, "frmSearchResults"); 
					
			response.getParsedResponse().setHeader("<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + "<tr><th>Street Address</th><th>Parcel ID</th></tr>");
			response.getParsedResponse().setFooter(footer + "</table>");
		
			outputTable.append(table);
						
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	
	public StringBuilder getPrevAndNextLinks (ServerResponse response, HtmlParser3 htmlParser, String sourceTag) {
		String url = "";
		String linkN = "";
		String linkP = "";
		String links = "";
		StringBuilder footer = new StringBuilder("<tr><td colspan=\"2\"> </br> &nbsp; &nbsp;");
		HashMap<String,String> paramsOfReq = htmlParser.getListOfPostParams(sourceTag); 

		// create links for Next/Prev buttons
		NodeList nodeList = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true); 						 				
			
		if (nodeList.size() > 0) {					
			TableTag tableTag = (TableTag) nodeList.elementAt(1);
			if (tableTag.getRowCount() == 1) { // Next button exists
			TableRow row = tableTag.getRow(0);
			NodeList buttons = row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);

			int pageNo = Integer.parseInt(paramsOfReq.get("PageNumber"));
			url = response.getLastURI().toString().replaceFirst("(?is)https://[^/]+(/obftax[^\\n]+)", "$1");
			
			for (int i=0; i < buttons.size()-1; i++) {
				String s = buttons.elementAt(i).toHtml();
				if (s.contains("id=\"cmdNext\"") && !s.toUpperCase().contains("DISABLED")) {
					linkN = CreatePartialLink(TSConnectionURL.idPOST) + url + "?PageNumber=" + (pageNo + 1)
							+ "&Action=AddressSearch&" + "ParcelAddress=" + paramsOfReq.get("ParcelAddress") + "&ParcelType=RE";
				}

				if (s.contains("id=\"cmdPrevious\"") && !s.toUpperCase().contains("DISABLED")) {
					linkP = CreatePartialLink(TSConnectionURL.idPOST) + url + "?PageNumber=" + (pageNo - 1)
							+ "&Action=AddressSearch&" + "ParcelAddress=" + paramsOfReq.get("ParcelAddress") + "&ParcelType=RE";
				}							
			}
			
			if (StringUtils.isNotEmpty(linkP)) {
				links = links + "<a href=\"" + linkP + "\"> Prev </a> &nbsp; &nbsp;";
			}
			 
			if (StringUtils.isNotEmpty(linkN)) {
				links = links + "<a href=\"" + linkN + "\"> Next </a> &nbsp; &nbsp;";
				response.getParsedResponse().setNextLink("<a href=\"" + linkN + "\">Next</a>");
			}	
			
			footer.append(links);
			
			}
		}
		
		return footer;
	}
	

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// 	no result returned - error message on official site
		if (rsResponse.contains("An unexpected error has occurred:")) {
			Response.getParsedResponse().setError("Official site not functional");
    		return;
    	} else if (rsResponse.indexOf("No results found for query") > -1) {
			Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
			return;
		} else if (rsResponse.indexOf("We could not locate any parcels that match") > -1) {
			Response.getParsedResponse().setError("We could not locate any parcels that match the address information you specified.");
			return;
		} else if (rsResponse.indexOf("The Parcel ID you entered is not valid") > -1) {
			Response.getParsedResponse().setError("The Parcel ID you entered is not valid!");
			return;
		} else if (rsResponse.indexOf("The Parcel ID you entered could not be found.") > -1) {
			Response.getParsedResponse().setError("The Parcel ID you entered could not be found.  Please set a valid Parcel ID.");
			return;
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
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				String filename = accountId + ".html";
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					loadDataHash(data);
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
				ParseResponse(sAction, Response, rsResponse.contains("Account Information")
															? ID_DETAILS
															: ID_SEARCH_BY_ADDRESS);
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
					NodeList nodeList = htmlParser.getNodeList()
							.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "tvrBody"), true);				
					
					if(nodeList.size() == 0) {
						return rsResponse;
					
					} else {						
						NodeList detailsList = nodeList.elementAt(0).getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("div"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("class", "tvrSection"), true);  // get table with PID, Name and Address info

						TableTag personalInfo = (TableTag) detailsList.elementAt(0).getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
						String apn = personalInfo.getRows()[0].getColumns()[0].getChildrenHTML(); 
								
						accountId.append(apn);
						
						personalInfo.setAttribute("id", "\"personalInfoTable\"");
						
						details.append("<table border = \"1\">");					
						details.append("<tr align=\"center\"><td colspan=\"5\"></br><b> Real Property Tax Search Results: </b></td></tr>");					
						details.append("<tr><td colspan=\"5\">");
						
						details.append(personalInfo.toHtml().replaceAll("(?is)<input[^>]+>", "").replaceAll("(?is)<th(\\s[^>]+)?", "<th" + "$1" + " align=\"right\"" ));
						details.append("</br>");
						
						detailsList = nodeList.elementAt(0).getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("table"),true) 
								.extractAllNodesThatMatch(new HasAttributeFilter("class", "tvrDataGrid")); // get table with Tax info
						
						if (detailsList.size() == 0) {
							details.append("</td></tr> </table> </br>");							
							return details.toString();
						}
						else {
							TableTag taxInfo = (TableTag) detailsList.elementAt(0);
							NodeList links = taxInfo.getChildren()
									.extractAllNodesThatMatch(new TagNameFilter("td"), true)
									.extractAllNodesThatMatch(new TagNameFilter("a"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("title", "View Details"), true); // links from "View Details" for last 4 tax years
												
							TableRow[] trs = taxInfo.getRows();
							//remove first line, with <th>
							TableHeader[] ths = trs[0].getHeaders();
							for (int i=1; i < ths.length; i++) {
								int theIndex = -1;
								for (int childIndex = 0; childIndex < ths[i].getChildCount(); childIndex++) {
									if (ths[i].getChild(childIndex).toHtml().contains("input")) {
										theIndex = childIndex;
										break;
									}
								}
								
								if(theIndex >= 0) {
									ths[i].removeChild(theIndex);
								}						
							}
							
							//remove links from table	
							for (int i = 1; i <= trs.length-3; i++) {
								TableColumn[] cols = trs[i].getColumns();
								
								int myIndex = -1;
								int colLength = cols.length;
								if (colLength >= 4) {
									for (int indexChild = 0; indexChild < trs[i].getChildCount(); indexChild++) {
										if (trs[i].getChild(indexChild).equals(cols[colLength-1])) {
											myIndex = indexChild;
											break;
										}
									}
									
									if(myIndex >= 0) {
										trs[i].removeChild(myIndex);
									}
								}
							}
							
							taxInfo.setAttribute("id", "\"mainTable\"");
							details.append(taxInfo.toHtml().replaceAll("(?is)<input[^>]+>", ""));
							
							details.append("<tr align=\"center\"><td colspan=\"5\"></br><b> TAX HISTORY: </b></td></tr>");								
							
							//add Tax History to Details Page
							DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
							String serverHomeLink = site.getServerHomeLink();
							
							for (int i=0; i < links.size(); i++) {
								LinkTag link = (LinkTag) links.elementAt(i);
								details.append("<tr><td><table id=\"histTable" + (i+1) + "\"><tr><td colspan=\"4\">")
									   .append(getTaxHistory(serverHomeLink + "obftax/" + link.extractLink()))
									   .append("</td></tr></table></td></tr>");
								
								if (i < links.size() - 1) {
									details .append("<tr><td></br></td><tr>");
								}								  
							}						
							
							details.append("</table> </br>");
						
							return details.toString();
						}
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

	
	protected String getTaxHistory(String extractedLink) {
		String taxHistoryHtml = getLinkContents(extractedLink);
		taxHistoryHtml = Tidy.tidyParse(taxHistoryHtml, null);
		
		try {
			HtmlParser3 htmlParser = new HtmlParser3(taxHistoryHtml);
						
			taxHistoryHtml = "<div> <span> :: Real Property Taxes :: </span> </div>";
			
			NodeList divList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("div"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "tvrSection"), false)
					.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("form"),false).elementAt(0).getChildren();											
			
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
			String legalDesc  = "";			
			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
			
			detailsHtml = detailsHtml.replaceAll("(?is)<th([^>]*>)", "<td$1").replaceAll("(?is)</th>", "</td>");
			detailsHtml = detailsHtml.replaceAll("", "");
			
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList list = htmlParser.getNodeList();		
			
			accountID = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(list, "Parcel ID:"),"",true).trim();
			names = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(list, "Owner Name:"),"",true).trim();
			address = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(list, "Parcel/Situs Address:"),"",true).trim();
			
			map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountID);
			map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),names.trim());		
			
			if (StringUtils.isNotEmpty(address)) {
				map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
				map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			}
			
			TableTag tmp = (TableTag) list.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "histTable1")).elementAt(0);
			
			if (tmp != null) {
				TableColumn col = tmp.getRow(0).getColumns()[0];
				
				tmp = (TableTag) col.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(1);
				col = tmp.getRow(0).getColumns()[0];			
				
				legalDesc = col.getChildrenHTML().trim();
				
				if (StringUtils.isNotEmpty(legalDesc)) {
					map.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDesc);
				}	
				
				ro.cst.tsearch.servers.functions.MDBaltimoreTR.parseTaxes(list, map, searchId);
				ro.cst.tsearch.servers.functions.MDBaltimoreTR.parseLegalSummary(map, searchId);
			
			} else {
				@SuppressWarnings("deprecation")
				int taxYear = Integer.parseInt(dataSite.getPayDate().toString().replaceAll("(?is)(?:.*)EEST\\s*(\\d{4})", "$1")) + 1;
				map.put(TaxHistorySetKey.YEAR.getKeyName(), Integer.toString(taxYear));
			}
			
			ro.cst.tsearch.servers.functions.MDBaltimoreTR.parseNames(map, searchId);
		
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
		
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		
		if(hasStreet() && hasStreetNo()) {	
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX)); 
			module.addFilter(addressFilter);
			moduleList.add(module);		
		}
		
		if(hasStreet() && hasStreetNo()) {	
			//e.g: 18-H CEDAR;  18-A MAIN;  4-E MAIN; 1 W ELM; 	241 S MAIN	
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX)); 
			String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
			strNo = strNo.replaceAll("(?is)(\\d+)[\\s*-?][A-Z]", "$1");
			module.clearSaKey(0);
			module.forceValue(0, strNo);
			module.addFilter(addressFilter);
			moduleList.add(module);		
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
}

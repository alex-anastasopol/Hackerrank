package ro.cst.tsearch.servers.types;

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
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;


public class MOStLouisCityTR extends TSServer {
 
	private static final long serialVersionUID = 1L;
	
	
	public MOStLouisCityTR(long searchId) {
		super(searchId);
	}

	public MOStLouisCityTR(String rsRequestSolverName, String rsSitePath,String rsServerID, String rsPrmNameLink, long searchId,int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId, miServerID);
	}
	
	
	protected void loadDataHash(HashMap<String, String> data) {
		if(data != null) {
			data.put("type","CNTYTAX");
		}
	}
	
	public String getAllAddress(TableRow[] rows, boolean multipleAdr, boolean multipleResultsAtAdrSearch) {
		String addresses = "<font style=\"color:blue; bold: true\">Primary address:</font> ";
		
		if (rows.length > 1) {
			TableRow row = rows[1];
			if (row.getColumnCount() > 1) {
				if (multipleAdr) {
					if (multipleResultsAtAdrSearch) {
						addresses += row.getColumns()[1].getChildren().elementAt(0).toHtml().replaceAll("&nbsp;","").trim()
								+ ";<br> <font style=\"color:green;\">Related addresses:</font> ";
					} else {
						addresses += row.getColumns()[1].getChildren().elementAt(1).getFirstChild().toHtml().replaceAll("&nbsp;","").trim()
								+ ";<br> <font style=\"color:green;\">Related addresses:</font> ";
					}
					
				} else {
					addresses += row.getColumns()[1].getChildren().elementAt(0).getFirstChild().toHtml().replaceAll("&nbsp;","").trim()
						+ ";<br> <font style=\"color:green;\">Related addresses:</font> ";
				}
				
				
				for (int i=2; i < rows.length; i++) {
					addresses += rows[i].getColumns()[1].getChildrenHTML().replaceAll("&nbsp;","").trim() + ";<br> " ;
				}
			}
		}
		addresses = addresses.replaceAll("(?is)<a href[^>]+>([^<]+)</a>", "$1");
		
		return addresses;
	}
	
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String rsResponse, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		boolean multipleAdr = false;
		boolean multipleResultsAtAdrSearch = false;
		
		if (rsResponse.indexOf("<caption>Total records:") > -1) {
			//we have multiple addresses for same Parcel#
			multipleAdr = true;
		} else if (rsResponse.matches("(?is).*The address\\s*<b>[^/]+/b>\\s*found\\s*\\d+\\s*record\\(s\\).*")) {
			//we have intermediate results when searching by address
			String regex = "(?is).*data vertical_table\\s*[^\"]+\"[^>]+>(\\d+)[^\"]+\"[^>]+>(\\d+).*";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(rsResponse);
			if (m.find()) {
				if (m.group(1).equals(m.group(2))) {
					multipleResultsAtAdrSearch = true;
					multipleAdr = true;
				}
			}
		}
		
		if (rsResponse.contains("No record was found") || rsResponse.contains("No record was found near that address.")) {
				return intermediaryResponse;
		} else
		try {
			HtmlParser3 htmlParser = new HtmlParser3(rsResponse);	
			//see div with id "content_full" - and from here extract table 
			//    class='data vertical_table' --> for general intermediate results
			//    class='data' --> for docs with multiple addresses (just one link in intermediate results)
			NodeList nodeList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("div"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "content_full"), true);
			
			if (nodeList != null) {
				nodeList = nodeList.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
				
				if (nodeList.size() == 0) {
					return intermediaryResponse;
					
				} else {
					// we have no multiple addresses
					NodeList tmpNodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "data vertical_table"), true);
					if(tmpNodeList.size() == 0) {
						// we have multiple address
						tmpNodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "data"), true);
						if(tmpNodeList.size() == 0) {
							return intermediaryResponse;
						} else {
							nodeList = tmpNodeList;
						}
					} else {
						nodeList = tmpNodeList;
					}
				}
				
				TableTag tableTag = (TableTag) nodeList.elementAt(0);
				TableRow[] rows  = tableTag.getRows();
			    
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
					if(row.getColumnCount() == 3) {
						ParsedResponse currentResponse = new ParsedResponse();
						LinkTag linkTag = ((LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
						
						String link = CreatePartialLink(TSConnectionURL.idGET) +  
							linkTag.extractLink().trim().replaceAll("\\s", "%20");
						
						linkTag.setLink(link);

						if (multipleAdr) {
							String address = "";
							if (multipleResultsAtAdrSearch) {
								address =  getAllAddress(rows, true, true);
							} else {
								address =  getAllAddress(rows, true, false);
							}
							 
							row.getColumns()[1].getFirstChild().setText(address);
							
							if (multipleResultsAtAdrSearch) {
								row.getColumns()[2].removeChild(0);
							} else {
								row.getColumns()[1].removeChild(1);
								row.getColumns()[2].removeChild(1);
							}
						} 
						
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
						currentResponse.setOnlyResponse(row.toHtml());
						currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
						
						ResultMap map = ro.cst.tsearch.servers.functions.MOStLouisCityTR.parseIntermediaryRow(row, searchId); 
						if (StringUtils.isEmpty((String) map.get(TaxHistorySetKey.YEAR.getKeyName()))) {
							DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
							int taxYear = Integer.parseInt(site.getPayDate().toString().replaceAll("(?is)(?:.*)(?:[A-Z]+T|WIB)\\s*(\\d{4})", "$1"));
							map.put(TaxHistorySetKey.YEAR.getKeyName(), Integer.toString(taxYear));
						}
						Bridge bridge = new Bridge(currentResponse, map, searchId);
						
						DocumentI document = (TaxDocumentI)bridge.importData();				
						currentResponse.setDocument(document);
						
						intermediaryResponse.add(currentResponse);		
						if (multipleAdr) {
							//there is only one valid result in intermediate results page, with multiple addresses
							break;
						}
					}
				}
			}
					
			
			response.getParsedResponse().setHeader("<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" 
					+ "<tr><th>ParcelId</th><br><th>Address</th><br><th>Owner Name</th></tr>");
			response.getParsedResponse().setFooter("</table>");
		
			outputTable.append(rsResponse);
						
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
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
							.extractAllNodesThatMatch(new HasAttributeFilter("class", "content_padding"), true);				
					
					if(nodeList.size() == 0) {
						return rsResponse;
					
					} else {	
						NodeList detailsList = nodeList.elementAt(0).getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("class", "data vertical_table"), false);
						
//						if (detailsList.size() > 6) {
//							details.append("<table id=\"mainTable\" border=\"1\"> <tr><td>");
//							
//							// get table with Boundary & Geography
//							TableTag table = (TableTag) detailsList.elementAt(0);
//							if (table != null) {
//								table.setAttribute("id", "\"boundaryTable\"");
//								String htmlAsString = table.toHtml().trim();
//								htmlAsString = htmlAsString.replaceFirst("(?is)<table\\s*([^>]+)>", "<table $1> " +
//										" <caption> <h4> <b> Boundary &amp; Geography </b> </h4> </caption>");
//								htmlAsString = htmlAsString.replaceAll("(?is)<\\s*th\\s*>([^<]+)</\\s*th>", "<td align=\"left\"> <b> $1 </b> </td>");
//								htmlAsString = htmlAsString.replaceAll("(?is)(?:--\\s*)?<a[^>]+>([^<]+)</a>", "$1");
//								String apn = table.getRows()[1].getColumns()[0].getChildrenHTML(); 
//								accountId.append(apn);
//								details.append(htmlAsString + "<br>");
//								details.append("</td></tr>");
//							}
//							
//							// get table with Property Information
//							table = (TableTag) detailsList.elementAt(2);
//							if (table != null) {
//								table.setAttribute("id", "\"ownerInfoTable\"");
//								String htmlAsString = table.toHtml().trim();
//								htmlAsString = htmlAsString.replaceFirst("(?is)(<caption>)\\s*([^<]+)(</caption>)", "$1 <h4><b> $2 </b></h4> $3");
//								htmlAsString = htmlAsString.replaceAll("(?is)<\\s*th\\s*>([^<]+)</\\s*th>", "<td align=\"left\"> <b> $1 </b> </td>");
//								details.append("<tr><td>");
//								details.append(htmlAsString);
//								details.append("</td></tr>");
//							}
//							
//							// get table with Parcel Info
//							table = (TableTag) detailsList.elementAt(3);
//							if (table != null) {
//								table.setAttribute("id", "\"parcelInfoTable\"");
//								String htmlAsString = table.toHtml().trim();
//								htmlAsString = htmlAsString.replaceFirst("(?is)(<caption>)\\s*([^<]+)(</caption>)", "$1 <h4><b> $2 </b></h4> $3");
//								htmlAsString = htmlAsString.replaceAll("(?is)<\\s*th\\s*>([^<]+)</\\s*th>", "<td align=\"left\"> <b> $1 </b> </td>");
//								htmlAsString = htmlAsString.replaceAll("(?is)\\s*(<b>\\s*Redevelopment code\\s*:\\s*</b>\\s*</td>\\s*<td>[^<]+)<a[^<]+</a>\\s+", "$1");
////								String apn = table.getRows()[0].getColumns()[0].getChildrenHTML(); 
////								accountId.append(apn);
//								if (htmlAsString.contains("*alternate addresses associated with this parcel")) {
//									String address = "";
//									String url = table.getRows()[1].getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0).getText();
//									url = url.replaceAll("(?is)<?a[^/]+/([^\"]+)\"(?:>)?", "$1");
//									url = url.trim().replaceAll("\\s", "%20");
//
//									DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
//									String serverHomeLink = site.getServerHomeLink();
//									url = serverHomeLink + url;
//										
//									String rsp = getLinkContents(url);
//										
//									HtmlParser3 htmlPrs = new HtmlParser3(rsp);
//										
//									NodeList tmp = htmlPrs.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"),true)
//											.extractAllNodesThatMatch(new HasAttributeFilter("id", "content_full"), true)
//											.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
//											.extractAllNodesThatMatch(new HasAttributeFilter("class", "data"), true);
//									if (tmp != null) {
//										TableTag tmpTable = (TableTag) tmp.elementAt(0);
//										TableRow[] rows  = tmpTable.getRows();
//										if (rows != null) {
//											if (rows.length > 1) {
//												TableRow row = rows[1];
//												if (row.getColumnCount() > 1) {
//													String text = "";
//													int childCount = row.getColumns()[1].getChildren().size();
//													if (childCount > 1) {
//														text = row.getColumns()[1].getChildren().elementAt(1).getFirstChild().toHtml();
//														if (text != null) {
//															address =  getAllAddress(rows, true, false);
//														}
//													} else if (childCount == 1) {
//														text = row.getColumns()[1].getChildren().elementAt(0).toHtml();
//														if (text != null) {
//															if (text.contains("/addressSearch/index.cfm?PrimaryAddress=true")) {
//																address =  getAllAddress(rows, false, true);
//															} else {
//																address =  getAllAddress(rows, true, true);
//															}
//														}
//													}
//												}
//											}	
//										}
//									}
//										
//									htmlAsString = htmlAsString.replaceFirst("(?is)\\s*(Property address:)\\s*[^\\\"]+\\\"[^<]+</a>", "$1 </b> </td> <td> " + address);
//								}
//								
//								details.append("<tr><td>");
//								details.append(htmlAsString);
//								details.append("</td></tr>");
//							}
//							
//							// get table with Legal Description
//							table = (TableTag) detailsList.elementAt(4);
//							if (table != null) {
//								table.setAttribute("id", "\"legalDescriptionTable\"");
//								String htmlAsString = table.toHtml().trim();
//								htmlAsString = htmlAsString.replaceFirst("(?is)(<caption>)\\s*([^<]+)(</caption>)", "$1 <h4><b> $2 </b></h4> $3");
//								htmlAsString = htmlAsString.replaceAll("(?is)<\\s*th\\s*>([^<]+)</\\s*th>", "<td align=\"left\"> <b> $1 </b> </td>");
//								details.append("<tr><td>");
//								details.append(htmlAsString);
//								details.append("</td></tr>");
//							}
//							
//							// get table with Cross References
//							table = (TableTag) detailsList.elementAt(6);
//							if (table != null) {
//								table.setAttribute("id", "\"crossRefTable\"");
//								table.getRows()[0].removeChild(0);
//								String htmlAsString = table.toHtml().trim();
//								htmlAsString = htmlAsString.replaceFirst("(?is)(<table[^>]+>)\\s*<tr>", "$1 <caption> <h4><b> Sales </b></h4> </caption> <tr>");
//								htmlAsString = htmlAsString.replaceAll("(?is)(?is)<\\s*th[^>]*>([^<]+)</\\s*th>", "<td colspan=\"2\"> <b> $1 </b> </td>");
//								htmlAsString = htmlAsString.replaceFirst("(?is)(Book Location)\\s*(\\(Recorder of Deeds Data\\))", "$1 <br> $2");
//								details.append("<tr><td>");
//								details.append(htmlAsString);
//								details.append("<br><br></td></tr>");
//							}
//						}
						
						if (detailsList.size() > 6) {
							details.append("<table id=\"mainTable\" border=\"1\"> <tr><td>");
							
							// get table with Basic information and PID
							TableTag table = (TableTag) detailsList.elementAt(0);
							if (table != null) {
								table.setAttribute("id", "\"boundaryTable\"");
								String htmlAsString = table.toHtml().trim();
								htmlAsString = htmlAsString.replaceFirst("(?is)<table\\s*([^>]+)>", "<table $1> " +
										"<caption> <h4> <b> Basic Info </b> </h4> </caption>");
								htmlAsString = htmlAsString.replaceAll("(?is)<\\s*th\\s*>([^<]+)</\\s*th>", "<td align=\"left\"> <b> $1 </b> </td>");
								htmlAsString = htmlAsString.replaceAll("(?is)(?:--\\s*)?<a[^>]+>([^<]+)</a>", "$1");
								details.append(htmlAsString + "<br>");
								details.append("</td></tr>");
							}
							
							// get table with Property Info
							table = (TableTag) detailsList.elementAt(1);
							if (table != null) {
								table.setAttribute("id", "\"ownerInfoTable\"");
								String htmlAsString = table.toHtml().trim();
								htmlAsString = htmlAsString.replaceFirst("(?is)(<caption>)\\s*([^<]+)(</caption>)", "$1 <h4><b> $2 </b></h4> $3");
								htmlAsString = htmlAsString.replaceAll("(?is)<\\s*th\\s*>([^<]+)</\\s*th>", "<td align=\"left\"> <b> $1 </b> </td>");
								htmlAsString = htmlAsString.replaceAll("(?is)\\s*(<b>\\s*Redevelopment code\\s*:\\s*</b>\\s*</td>\\s*<td>[^<]+)<a[^<]+</a>\\s+", "$1");
								String apn = table.getRows()[4].getColumns()[0].getChildrenHTML(); 
								accountId.append(apn);
								if (htmlAsString.contains("*alternate addresses associated with this parcel")) {
									String address = "";
									String url = table.getRows()[5].getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0).getText();
									url = url.replaceAll("(?is)<?a[^/]+/([^\"]+)\"(?:>)?", "$1");
									url = url.trim().replaceAll("\\s", "%20");

									DataSite site = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), getServerID());
									String serverHomeLink = site.getServerHomeLink();
									serverHomeLink = serverHomeLink.replaceFirst("http", "https");
									url = serverHomeLink + url;
										
									String rsp = getLinkContents(url);
										
									HtmlParser3 htmlPrs = new HtmlParser3(rsp);
										
									NodeList tmp = htmlPrs.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"),true)
											.extractAllNodesThatMatch(new HasAttributeFilter("id", "content_full"), true)
											.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
											.extractAllNodesThatMatch(new HasAttributeFilter("class", "data"), true);
									if (tmp != null) {
										TableTag tmpTable = (TableTag) tmp.elementAt(0);
										TableRow[] rows  = tmpTable.getRows();
										if (rows != null) {
											if (rows.length > 1) {
												TableRow row = rows[1];
												if (row.getColumnCount() > 1) {
													String text = "";
													int childCount = row.getColumns()[1].getChildren().size();
													if (childCount > 1) {
														text = row.getColumns()[1].getChildren().elementAt(1).getFirstChild().toHtml();
														if (text != null) {
															address =  getAllAddress(rows, true, false);
														}
													} else if (childCount == 1) {
														text = row.getColumns()[1].getChildren().elementAt(0).toHtml();
														if (text != null) {
//															if (text.contains("/addressSearch/index.cfm?PrimaryAddress=true")) {
															if (text.contains("/data/address-search/index.cfm?PrimaryAddress=true")) {
																address =  getAllAddress(rows, false, true);
															} else {
																address =  getAllAddress(rows, true, true);
															}
														}
													}
												}
											}	
										}
									}
										
									htmlAsString = htmlAsString.replaceFirst("(?is)\\s*(Property address:)\\s*[^\\\"]+\\\"[^<]+</a>", "$1 </b> </td> <td> " + address);
								}
								
								details.append("<tr><td>");
								details.append(htmlAsString);
								details.append("</td></tr>");
							}
							
							// get table with Parcel Information
							table = (TableTag) detailsList.elementAt(2);
							if (table != null) {
								table.setAttribute("id", "\"parcelInfoTable\"");
								String htmlAsString = table.toHtml().trim();
								htmlAsString = htmlAsString.replaceFirst("(?is)(<caption>)\\s*([^<]+)(</caption>)", "$1 <h4><b> $2 </b></h4> $3");
								htmlAsString = htmlAsString.replaceAll("(?is)<\\s*th\\s*>([^<]+)</\\s*th>", "<td align=\"left\"> <b> $1 </b> </td>");
								details.append("<tr><td>");
								details.append(htmlAsString);
								details.append("</td></tr>");
							}
							table = (TableTag) detailsList.elementAt(3);
							if (table != null) {
								String htmlAsString = table.toHtml().trim();
								htmlAsString = htmlAsString.replaceAll("(?is)<\\s*th\\s*>([^<]+)</\\s*th>", "<td align=\"left\"> <b> $1 </b> </td>");
								details.append("<tr><td>");
								details.append(htmlAsString);
								details.append("</td></tr>");
							}
							
							// get table with Assessment Information
							table = (TableTag) detailsList.elementAt(4);
							if (table != null) {
								table.setAttribute("id", "\"assessmentInfoTable\"");
								String htmlAsString = table.toHtml().trim();
								htmlAsString = htmlAsString.replaceFirst("(?is)(<caption>)\\s*([^<]+)(</caption>)", "$1 <h4><b> $2 </b></h4> $3");
								htmlAsString = htmlAsString.replaceAll("(?is)<\\s*th\\s*>([^<]+)</\\s*th>", "<td align=\"left\"> <b> $1 </b> </td>");
								details.append("<tr><td>");
								details.append(htmlAsString);
								details.append("</td></tr>");
							}
							
							// get table with Sales Information
							table = (TableTag) detailsList.elementAt(5);
							if (table != null) {
								table.setAttribute("id", "\"salesInfoTable\"");
								String htmlAsString = table.toHtml().trim();
								htmlAsString = htmlAsString.replaceFirst("(?is)(<table\\s*([^>]+)>)", "$1 <caption> <h4><b> Sales Information </b></h4> </caption>");
								htmlAsString = htmlAsString.replaceAll("(?is)<\\s*th\\s*>([^<]+)</\\s*th>", "<td align=\"left\"> <b> $1 </b> </td>");
								details.append("<tr><td>");
								details.append(htmlAsString);
								details.append("</td></tr>");
							}
							// Sales History
							table = (TableTag) nodeList.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("class", "data shortenTable"), false).elementAt(0);
							if (table != null) {
								table.setAttribute("id", "\"salesHistoryTable\"");
								String htmlAsString = table.toHtml().trim();
								htmlAsString = htmlAsString.replaceFirst("(?is)(<caption>)\\s*([^<]+)(</caption>)", "$1 <h4><b> $2 </b></h4> $3");
								htmlAsString = htmlAsString.replaceAll("(?is)<\\s*th\\s*>([^<]+)</\\s*th>", "<td align=\"left\"> <b> $1 </b> </td>");
								htmlAsString = htmlAsString.replaceAll("(?is)<font[^>]+>([^<]+)</font>","$1");
								details.append("<tr><td>");
								details.append(htmlAsString);
								details.append("</td></tr>");
							}
						}
						
						
						//Tax History
						detailsList = nodeList.elementAt(0).getChildren()
								.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("class", "data"), false);
						TableTag table = (TableTag)detailsList.elementAt(0);
						if (table != null) {
							table.setAttribute("id", "taxHistoryTable");
							table.removeAttribute("class");
							details.append("<tr><td>");
							String htmlTaxTable = table.toHtml().trim();
							if (htmlTaxTable.contains("Pay online using Official Payments Website")) {
								htmlTaxTable = htmlTaxTable.replaceFirst("(?is)<p>\\s*<a[^>]+>Pay online using[^/]+/a>[^/]+/p>", "");
							}
							details.append(htmlTaxTable);
							details.append("</td></tr>");
						}
						
						details.append("</table></br>");
						
						String htmlResponse = details.toString();
						htmlResponse = htmlResponse.replaceAll("(?is)<caption>\\s*(?:<h4>\\s*<b>\\s*)?", "<tr id=\"caption\"> <td> <font style=\"color:red;font-size:14px;bold:true\">");
						htmlResponse = htmlResponse.replaceFirst("(?is)(<tr id=\\\"caption\\\">)\\s*<td>\\s*([^>]+>Payment history for each of the last three \\(3\\) years[^>]+>\\s*</td>\\s*</tr>)", 
								"$1 <td colspan=\"8\"> $2");
						htmlResponse = htmlResponse.replaceAll("(?is)(?:</b>\\s*</h4>\\s*)?</caption>", "</font> </td> </tr>");
						htmlResponse = htmlResponse.replaceAll("(?is)<a[^>]+>([^<]*)</a>", "$1");
						
						return htmlResponse;
						
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
	
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse rsResponse, String detailsHtml, ResultMap map) {
		try {	
			String accountID = "";			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
			
			detailsHtml = detailsHtml.replaceAll("(?is)<tr id=\\\"caption\\\">\\s*<td>\\s*([^/]+/font>)\\s*</td>\\s*</tr>","<caption> $1 </caption>");
			
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList list = htmlParser.getNodeList();	
			
			TableTag table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id","boundaryTable"), true).elementAt(0);
			if (table != null) {
				String ncbNo = "";
				if (table.getRowCount() > 2) {
					TableRow row = table.getRows()[2];
					if (row.getColumnCount() == 2) {
						ncbNo = row.getColumns()[1].getChildrenHTML().trim();
						if (StringUtils.isNotEmpty(ncbNo)) {
							ncbNo = ncbNo.replaceAll("\\s{2,}", " ");
							if (ncbNo.matches("(?is)\\d+\\.00")) {
								ncbNo = ncbNo.replaceFirst("(?is)(\\d+)\\.00", "$1");
							}
							map.put(PropertyIdentificationSetKey.NCB_NO.getKeyName(), ncbNo);
						}
					}
					
					row = table.getRows()[1];
					if (row.toHtml().contains("Parcel") && row.getColumnCount() == 2) {
						accountID = row.getColumns()[1].getChildrenHTML().trim();
						if (StringUtils.isNotEmpty(accountID)) {
							map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountID);
						}
					}
				}
			}

			table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id","ownerInfoTable"), true).elementAt(0);
			if (table != null) {
				//extract Owner info
				if (table.getRowCount() > 0) {
					TableRow row = table.getRow(0);
					if (row.getColumns()[0].toHtml().contains("Owner") && row.getColumnCount() > 1) {
						String names = row.getColumns()[1].getChildrenHTML().trim();
						if (StringUtils.isNotEmpty(names)) {
							map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), names);
						}
						
					} else {
						//row[0],col[1] = zip code
						if (StringUtils.isNotEmpty(row.getColumns()[1].getChildrenHTML().trim())) {
							map.put(PropertyIdentificationSetKey.ZIP.getKeyName(), row.getColumns()[1].getChildrenHTML().trim());
						}
						row = table.getRow(1);
						if (row.getColumns()[0].toHtml().contains("Owner") && row.getColumnCount() > 1) {
							String names = row.getColumns()[1].getChildrenHTML().trim();
							if (StringUtils.isNotEmpty(names)) {
								map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), names);
							}
						}
						
						if (table.getRowCount() > 5) {
							row = table.getRow(5);
							if (row.getColumns()[0].toHtml().contains("Property address")) {
								String address = row.getColumns()[1].getChildrenHTML().trim();
								if (StringUtils.isNotEmpty(address)) {
									if (address.contains("Primary address:") && address.contains("Related addresses:")) {
										address = address.replaceAll("(?is)</?br\\s*>", "");
										address = address.replaceAll("(?is)<font[^>]+>([^<]+)</font>\\s*([^<]+)", "$1 $2");
									} else {
										address = address.replaceAll("(?is)</?br\\s*>", " ");
									}
									
									map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
								}
							}
						}
					}
				}
			}
			
			table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id","parcelInfoTable"), true).elementAt(0);
			if (table != null) {
				if (table.getRowCount() > 1) {
					TableRow row = table.getRow(0);
					
					if (row.getColumnCount() == 2 && row.getColumns()[0].toHtml().contains("Condominium")) {
						if (table.getRowCount() > 4 && table.getRow(4).toHtml().contains("Property description")) {
							row = table.getRow(4);
							if (row.getColumnCount() > 0) {
								String legal = row.getColumns()[0].getChildrenHTML().trim();
								if (StringUtils.isNotEmpty(legal)) {
									map.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
								}
							}
						}
						
					} else {
						if (row.getColumnCount() > 1) {
							//extract ParcelNo
							accountID = row.getColumns()[1].getChildrenHTML().trim();
							if (StringUtils.isNotEmpty(accountID)) {
								map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountID);
							}
						}
						row = table.getRow(1);
						if (row.getColumnCount() > 1) {
							//extract Address
							String address = row.getColumns()[1].getChildrenHTML().trim();
							if (StringUtils.isNotEmpty(address)) {
								if (address.contains("Primary address:") && address.contains("Related addresses:")) {
									address = address.replaceAll("(?is)</?br\\s*>", "");
									address = address.replaceAll("(?is)<font[^>]+>([^<]+)</font>\\s*([^<]+)", "$1 $2");
								} else {
									address = address.replaceAll("(?is)</?br\\s*>", " ");
								}
								
								map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
							}
						}
					}
				}
			}
			
			table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id","legalDescriptionTable"), true)
					.elementAt(0);
			if (table != null) {
				if (table.getRowCount() > 0) {
					TableRow row = table.getRow(0);
					if (row.getColumnCount() > 0) {
						String legal = row.getColumns()[0].getChildrenHTML().trim();
						if (StringUtils.isNotEmpty(legal)) {
							map.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
						}
					}
				}
			}
			
			table = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id","crossRefTable"), true)
					.elementAt(0);
			if (table != null) {
				if (table.getRowCount() > 0) {
					TableRow[] rows = table.getRows();
					List<List> body = new ArrayList<List>();
					List<String> line = new ArrayList<String>();
					for (int i = 1; i < rows.length; i++) {
						TableColumn[] columns = rows[i].getColumns();
						if (columns.length == 2) {
							String key = columns[0].getChildrenHTML().trim();
							String value = columns[1].getChildrenHTML().replaceAll("&nbsp;", "").trim();
							if (StringUtils.isNotEmpty(value)) {
								if(key.contains("Sales date:")) {
									line.add(value);
								} else if(key.contains("Sales price:")) {
									line.add(value);
								} else if(key.contains("Daily date:")) {
									line.add(value);
								} else if(key.contains("Book number:")) {
									if (!"0".equals(value) && !"0000".equals(value) && !"CVN".equals(value)) {
										line.add(value);
									}
								} else if(key.contains("Page number:")) {
									if (!"0".equals(value)) {
										line.add(value);
									}
								}
							} else {
								line.add("");
							}
						} 
					}
					if (line != null) {
						body.add(line);
					}
					// adding all cross references - should contain transfer table
					if (body != null && body.size() > 0) {
						ResultTable rt = new ResultTable();
						String[] header = { "RecordedDate", "SalesPrice", "InstrumentDate", "Book", "Page"};
						rt = GenericFunctions2.createResultTable(body, header);
						map.put("SaleDataSet", rt);
					}
				}
			}
				
			
			ro.cst.tsearch.servers.functions.MOStLouisCityTR.parseDetails(list, map, searchId);
			
			if (StringUtils.isEmpty((String) map.get(TaxHistorySetKey.YEAR.getKeyName()))) {
				int taxYear = Integer.parseInt(dataSite.getPayDate().toString().replaceAll("(?is)(?:.*)(?:[A-Z]+T|WIB)\\s*(\\d{4})", "$1"));
				map.put(TaxHistorySetKey.YEAR.getKeyName(), Integer.toString(taxYear));
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return null;	
	}
	
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// 	no result returned - error message on official site
		if (rsResponse.contains("The web site you are accessing has experienced an unexpected error.")) {
			Response.getParsedResponse().setError("Official site not functional");
    		return;
		} else if (rsResponse.contains("There is a problem with the resource you are looking for, and it cannot be displayed.")) {
			Response.getParsedResponse().setError("Error page received from official site (500 - Internal server error)");
    		return;
		} else if (rsResponse.indexOf("No record found!") > -1) {
			Response.getParsedResponse().setError("No record found! Go back and change your search criteria ");
			return;
		} else if (rsResponse.indexOf("No record was found near that address") > -1) {
			Response.getParsedResponse().setError("No record found! Change your address and search again ");
			return;
		} else if (rsResponse.indexOf("No records found for the address") > -1) {
			Response.getParsedResponse().setError("No record found! Change your address and search again ");
			return;
		}
		
		switch (viParseID) {			
//			case ID_SEARCH_BY_ADDRESS:	
//			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_MODULE19: 
				StringBuilder outputTable = new StringBuilder();
				if ((rsResponse.indexOf("<caption>Total records:") > -1)
						|| (rsResponse.matches("(?is).*The address\\s*<b>[^/]+/b>\\s*found\\s*\\d+\\s*record\\(s\\).*"))) {
					Collection<ParsedResponse>	smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
					
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
						parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
		            }			
					break;
				}				
				
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:					
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
				
			case ID_GET_LINK :System.out.println(rsResponse);
				ParseResponse(sAction, Response, 
						(rsResponse.contains("parcelId") || rsResponse.contains("parcelid"))
												? ID_DETAILS
												: ID_SEARCH_BY_ADDRESS);
				break;
				
			default:
				break;
		}
	}
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		if(hasPin()) {
			//module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX)); 			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			String pid = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			module.clearSaKey(0);
			module.forceValue(0, pid);
			moduleList.add(module);
		}
		
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );
		
		if(hasStreet() && hasStreetNo()) {	
			//e.g: 1200-1202 N MARKET ST;  1500-1506 S GRAND BLVD;  1 W PINE COURT	
			//module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX)); 
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
			strNo = strNo.replaceAll("(?is)(\\d+)(?:\\s*-?(?:\\d+|[A-Z])?)", "$1");
			String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
			module.clearSaKey(0);
			module.forceValue(0, strNo + " " + strName);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			moduleList.add(module);		
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	

}

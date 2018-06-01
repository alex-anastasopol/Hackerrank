package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
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
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

import com.gargoylesoftware.htmlunit.html.HTMLParser;
import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

public class FLMiamiDadeAO extends TSServer {
		
	private static final long serialVersionUID = -2654769757179990231L;

	public FLMiamiDadeAO(long searchId) {
		super(searchId);
	}
	
	public FLMiamiDadeAO(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	
		
	protected String getHTMLIntermediaries(String response) {
		JSONObject jsonObject;
		Object resultsObj;
		JSONArray jsonResults = new JSONArray();
		int totalRecords = 0;
		StringBuilder intermHtmlTable = new StringBuilder();
		int numberOfResults = 0;
		int resultLength = 0;

		try {
			jsonObject = (new JSONObject(response));
			if ((resultsObj = jsonObject.get("MinimumPropertyInfos")) instanceof JSONArray) {
				jsonResults = (JSONArray) resultsObj;
			}

			totalRecords = jsonObject.getInt("Total");
			if (totalRecords == 0) {
				return "";
			}

			intermHtmlTable.append("<div id=\"resultsCount\">Number of results: " + totalRecords + "</div>");

			numberOfResults = jsonResults.length();
			//JSONArray jsonResult = jsonResults;
			JSONObject jsonElem = null;
			TreeMap<Integer, HashMap<String,String>> intermResultsMap = new TreeMap<Integer, HashMap<String,String>>();
			final TreeMap<Integer,String> mapByAdr = new TreeMap<Integer,String>();
			
			resultLength = jsonResults.length();
			if (resultLength > 0) {
				intermHtmlTable.append("<table id=\"intermediaries\" border=\"1\" cellpadding=\"0\" cellspacing=\"0\" width=\"95%\"><tr>" +
						"<th>Folio</th>" +
						"<th>Subdivision</th>" +
						"<th>Owner</th>" +
						"<th>Address</th>" +
						"</tr>");
			}
			
			String baseLink = "/PApublicServiceProxy/PaServicesProxy.ashx?Operation=GetPropertySearchByFolio&clientAppName=PropertySearch&folioNumber=";
			String folioNo = "";
			String name = "";
			String address = "";
			String subdiv = "";
			String link = "";
			
			for (int i = 0; i < numberOfResults; i++) {
				jsonElem = (JSONObject) jsonResults.get(i);
				resultLength = jsonElem.length();
				
				folioNo = (String) jsonElem.get("Strap");
				name = (String) jsonElem.get("Owner1");
				if (StringUtils.isNotEmpty((String) jsonElem.get("Owner2"))) {
					name += "<br>" + (String) jsonElem.get("Owner2");
				}
				if (StringUtils.isNotEmpty((String) jsonElem.get("Owner3"))) {
					name += "<br>" + (String) jsonElem.get("Owner3");
				}
				address = (String) jsonElem.get("SiteAddress");
				subdiv = (String) jsonElem.get("SubdivisionDescription");
				link = baseLink + folioNo.replaceAll("-", "");
				link = CreatePartialLink(TSConnectionURL.idGET) + link;
				
				if (StringUtils.isNotEmpty(folioNo)) {
					HashMap<String,String> rowInfo=  new HashMap<String, String>();
					rowInfo.put("FolioNo", folioNo);
					rowInfo.put("Owners", name);
					rowInfo.put("Address", address);
					rowInfo.put("Subdiv", subdiv);
					rowInfo.put("detPgLink", link);
					
					intermResultsMap.put(i, rowInfo);
					mapByAdr.put(i,address);
					
				}
			}
				Comparator<Integer> comparator = new Comparator<Integer>() {
					public int compare(Integer o1, Integer o2) {
						return mapByAdr.get(o1).compareTo(mapByAdr.get(o2));
					}
				};
				
				final TreeMap<Integer, String> sortedMapByAdr = new TreeMap<Integer,String>(comparator);
				sortedMapByAdr.putAll(mapByAdr);

				for ( Entry<Integer, String> e : sortedMapByAdr.entrySet()) {
					Integer key = e.getKey();
//					System.out.println(e.getKey() + ": " + e.getValue());
					HashMap<String,String> row = intermResultsMap.get(key);
					Iterator<String> it = row.keySet().iterator();
					while (it.hasNext()) {
						String k = it.next();
						if ("FolioNo".equals(k)) {
							folioNo = row.get(k);
						} else if ("Address".equals(k)) {
							address = row.get(k);
						} else if ("Owners".equals(k)) {
							name = row.get(k);
						} else if ("Subdiv".equals(k)) {
							subdiv = row.get(k);
						} else if ("detPgLink".equals(k)) {
							link = row.get(k);
						}
					}
						
					intermHtmlTable.append(
						"<tr>" +
							"<td><a href=\"" + link + "\">" + folioNo + "</a></td>" +	
							"<td>" + subdiv + "</td>" +
							"<td>" + name + "</td>" +
							"<td>" + address + "</td>" +
						"</tr>");
				}	

			intermHtmlTable.append("</table>");
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return intermHtmlTable.toString();
	}
	
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		if (rsResponse.indexOf("The correct format is \\\"Address\\\" without city, state, or zip code.") > -1) {
			Response.getParsedResponse().setError("Invalid address entered.");
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (rsResponse.indexOf("\"Total\":0") > -1) {
			Response.getParsedResponse().setError("No data found.");
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		switch (viParseID) {
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_ADDRESS:
				String intermediariesHtml = getHTMLIntermediaries(rsResponse);
				if (intermediariesHtml.isEmpty()) {
					parsedResponse.setError("<font color=\"red\">No results found</font>");
					return;
				}

				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, intermediariesHtml, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
	            }
				
				break;	
				
			case ID_SEARCH_BY_PARCEL:
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				StringBuilder serialNumber = new StringBuilder();
				//String details = getDetails(rsResponse, serialNumber);
				String details = getDetailsFromJSONObj(rsResponse, serialNumber);
				String filename = serialNumber + ".html";
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","ASSESSOR");
					data.put("dataSource","AO");
					if (isInstrumentSaved(serialNumber.toString(),null,data)){
						details += CreateFileAlreadyInTSD();
					}
					else {
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
				ParseResponse(sAction, Response, ID_DETAILS);
				break;	
				
			default:
				break;
		}
	}	
		
	
	protected String getDetailsFromJSONObj(String rsResponse, StringBuilder parcelNumber) {
		String details = "";
		StringBuilder detPgTable = new StringBuilder();
		JSONObject jsonObject = null;
		JSONObject jsonElem = null;
		JSONArray jsonArrayElem = null;
		Object resultsObj;
		try {
			if (rsResponse.contains("<table ")) {
				HtmlParser3 parser = new HtmlParser3(rsResponse);
				NodeList list = parser.getNodeList().extractAllNodesThatMatch(new HasAttributeFilter("id", "folioNo"), true);
				if (list != null) {
					String folioNo = list.elementAt(0).getChildren().toHtml().trim();
					if (StringUtils.isNotEmpty(folioNo)) {
						parcelNumber.append(folioNo);
					}
				}
				
				return rsResponse;
			}
			
			jsonObject = (new JSONObject(rsResponse));
			//Property Information
			jsonElem = (JSONObject) jsonObject.get("PropertyInfo");
			if (jsonElem != null) {
				detPgTable.append("<br><br> <table id=\"propInfo\" border=\"1\" width=\"650\" cellpadding=\"5\">" +
					"<tr style=\"background-color:grey; height:20px; font:bold; color:white;\">" +
						"<td colspan=\"2\"> PROPERTY INFORMATION </td>" +
					"</tr>");
				String info = jsonElem.getString("FolioNumber");
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Folio:</b> </td>" +
						"<td align=\"left\" id=\"folioNo\">" + info + "</td>" +
					"</tr>");
				if (StringUtils.isNotEmpty(info)) {
					parcelNumber.append(info);
				}
				
				info = jsonElem.getString("SubdivisionDescription");
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Sub-Division:</b> </td>" +
						"<td align=\"left\" id=\"subdivName\">" + info + "</td>" +
					"</tr>");
			}
			
			if ((resultsObj = jsonObject.get("SiteAddress")) instanceof JSONArray) {
				jsonArrayElem = (JSONArray) resultsObj;
				if (!jsonArrayElem.isNull(0)) {
					jsonElem = (JSONObject) jsonArrayElem.get(0);
					if (jsonElem != null) {
						String info = jsonElem.getString("Address");
						detPgTable.append("<tr>" +
								"<td align=\"left\"> <b>Property Address:</b> </td>" +
								"<td align=\"left\" id=\"propAddress\">" + info + "</td>" +
							"</tr>");
					}
				}
			}
			
			if ((resultsObj = jsonObject.get("OwnerInfos")) instanceof JSONArray) {
				jsonArrayElem = (JSONArray) resultsObj;
				if (!jsonArrayElem.isNull(0)) {
					String names = "";
					for (int i=0; i<jsonArrayElem.length(); i++) {
						jsonElem = (JSONObject) jsonArrayElem.get(i);
						String info = jsonElem.getString("Name");
						if (StringUtils.isNotEmpty(info)) {
							names += info;
							if (jsonArrayElem.length() > 1) {
								names += "<br>";
							}
						}
					}
					if (StringUtils.isNotEmpty(names)) {
						detPgTable.append("<tr>" +
								"<td align=\"left\"> <b>Owner:</b> </td>" +
								"<td align=\"left\" id=\"ownerInfo\">" + names + "</td>" +
							"</tr>");
					}
				}
			}
			
			jsonElem = (JSONObject) jsonObject.get("MailingAddress");
			if (jsonElem != null) {
				String info = jsonElem.getString("Address1"); 
				if (StringUtils.isNotEmpty(jsonElem.getString("Address2"))) {
					info += "<br> " + jsonElem.getString("Address2");
					if (StringUtils.isNotEmpty(jsonElem.getString("Address3"))) {
						info += "<br> " + jsonElem.getString("Address3");
					}
				}
				
				detPgTable.append("<tr cellpading=\"1\">" +
						"<td align=\"left\"> <b>Mailing Address:</b> </td>" +
						"<td align=\"left\" id=\"mailingAdr\">" + info);
				 info = jsonElem.getString("City");
				 if (StringUtils.isNotEmpty(info)) {
					 detPgTable.append("<br>" + info);
				 }
				 info = jsonElem.getString("State");
				 if (StringUtils.isNotEmpty(info)) {
					 detPgTable.append(", " + info);
				 }
				 info = jsonElem.getString("ZipCode");
				 if (StringUtils.isNotEmpty(info)) {
					 detPgTable.append(" " + info);
				 }
				detPgTable.append("</td> </tr>");
			}
			
			jsonElem = (JSONObject) jsonObject.get("PropertyInfo");
			if (jsonElem != null) {
				String info = jsonElem.getString("PrimaryZone"); 
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Primary Zone:</b> </td>" +
						"<td align=\"left\">" + info);
				info = jsonElem.getString("PrimaryZoneDescription");
				detPgTable.append(" " + info + "</td> </tr>");
				
				info = jsonElem.getString("DORCode");
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Primary Land Use:</b> </td>" +
						"<td align=\"left\">" + info);
				info = jsonElem.getString("DORDescription");
				detPgTable.append(" " + info + "</td> </tr>");
				
				info = jsonElem.getString("BedroomCount");
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Beds / Baths / Half:</b> </td>" +
						"<td align=\"left\">" + info);
				info = jsonElem.getString("BathroomCount");
				detPgTable.append(" / " + info);
				info = jsonElem.getString("HalfBathroomCount");
				detPgTable.append(" / " + info + "</td> </tr>");
				
				info = jsonElem.getString("FloorCount");
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Floors:</b> </td>" +
						"<td align=\"left\">" + info  + "</td> </tr>");
				
				info = jsonElem.getString("UnitCount");
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Living Units:</b> </td>" +
						"<td align=\"left\">" + info  + "</td> </tr>");
				
				info = jsonElem.getString("BuildingGrossArea");
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Actual Area:</b> </td>" +
						"<td align=\"left\">" + (!"-1".equals(info) ? info + " Sq.Ft": "")  + "</td> </tr>");
				
				info = jsonElem.getString("BuildingHeatedArea");
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Living Area:</b> </td>" +
						"<td align=\"left\">" + (!"-1".equals(info) ? info + " Sq.Ft": "")  + "</td> </tr>");
				
				info = jsonElem.getString("BuildingEffectiveArea");
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Adjusted Area:</b> </td>" +
						"<td align=\"left\">" + (!"-1".equals(info) ? info + " Sq.Ft": "")  + "</td> </tr>");
				
				info = jsonElem.getString("LotSize");
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Lot size:</b> </td>" +
						"<td align=\"left\">" + (!"-1".equals(info) ? info + " Sq.Ft": "")  + "</td> </tr>");
				
				info = jsonElem.getString("YearBuilt");
				detPgTable.append("<tr>" +
						"<td align=\"left\"> <b>Year Built</b> </td>" +
						"<td align=\"left\">" + info + "</td></tr>");
				detPgTable.append("<tr border=\"0\"> <td colspan=\"2\"> <br><br><br><br> </td> </tr>");
			}
			
			
			//Assessment Information
			detPgTable.append("<tr style=\"background-color:grey; height:20px; font:bold; color:white;\">" +
						"<td colspan=\"2\">ASSESSMENT INFORMATION </td>" +
					"</tr>");
			jsonElem = (JSONObject) jsonObject.get("Assessment");
			if (jsonElem != null) {
				List<String> row1 = new ArrayList<String>();
				List<String> row2 = new ArrayList<String>();
				List<String> row3 = new ArrayList<String>();
				List<String> row4 = new ArrayList<String>();
				List<String> row5 = new ArrayList<String>();
				List<String> row6 = new ArrayList<String>();
				if ((resultsObj = jsonElem.get("AssessmentInfos")) instanceof JSONArray) {
					jsonArrayElem = (JSONArray) resultsObj;
					detPgTable.append("<tr> <td colspan=\"2\"> <table border=\"0\"  width=\"650\" cellpadding=\"5\" id=\"assessmentTable\">");
					row1.add("Year");
					row2.add("Land Value");
					row3.add("Building Value");
					row4.add("Extra Feature Value");
					row5.add("Market Value");
					row6.add("Assessed Value");
					
					for (int i=0; i<jsonArrayElem.length(); i++) {
						jsonElem = (JSONObject) jsonArrayElem.get(i);
						if (jsonElem != null) {
							String info = jsonElem.getString("Year");
							row1.add(info);
							info = jsonElem.getString("LandValue");
							row2.add("$" + info);
							info = jsonElem.getString("BuildingOnlyValue");
							row3.add("$" + info);
							info = jsonElem.getString("ExtraFeatureValue");
							row4.add("$" + info);
							info = jsonElem.getString("TotalValue");
							row5.add("$" + info);
							info = jsonElem.getString("AssessedValue");
							row6.add("$" + info);
						}
					}
				}
				List<String> row = new ArrayList<String>();
				for (int i=0; i<6; i++) {
					switch (i) {
						case 0:
							row = row1;
							break;
						case 1:
							row = row2;
							break;
						case 2:
							row = row3;
							break;
						case 3:
							row = row4;
							break;
						case 4:
							row = row5;
							break;
						case 5:
							row = row6;
							break;
					}
						
					detPgTable.append("<tr>");
					detPgTable.append("<td> <b>" + row.get(0) + " </b> </td>" +
							"<td> " + (i==0 ? "<b>" : "") + row.get(1) + (i==0 ? "</b>" : "") +"</td> " + 
							"<td> " + (i==0 ? "<b>" : "") + row.get(2) + (i==0 ? "</b>" : "") + "</td> " +
							"<td> " + (i==0 ? "<b>" : "") + row.get(3) + (i==0 ? "</b>" : "") + "</td>");
					detPgTable.append("</tr>");
				}
				detPgTable.append("</table> <tr border=\"0\"> <td colspan=\"2\"> <br><br><br><br> </td> </tr>");
			}
				
			
			//Benefits Information
			detPgTable.append("<tr style=\"background-color:grey; height:20px; font:bold; color:white;\">" +
					"<td colspan=\"2\">BENEFITS INFORMATION </td> </tr>");
			jsonElem = (JSONObject) jsonObject.get("Benefit");
			if (jsonElem != null) {
				List<String> row1 = new ArrayList<String>();
				List<String> row2 = new ArrayList<String>();
				List<String> row3 = new ArrayList<String>();
				List<String> row4 = new ArrayList<String>();
				if ((resultsObj = jsonElem.get("BenefitInfos")) instanceof JSONArray) {
					jsonArrayElem = (JSONArray) resultsObj;
					detPgTable.append("<tr> <td colspan=\"2\"> <table border=\"0\"  width=\"650\" cellpadding=\"5\">");
					
					row1.add("Benefit");
					row1.add("Type");
					row2.add("Save Our Homes Cap");
					row2.add("Assessment Reduction");
					row3.add("Homestead");
					row3.add("Exemption");
					row4.add("Second Homestead");
					row4.add("Exemption");
							
					if (jsonArrayElem.length() > 0) {
						for (int i=0; i<jsonArrayElem.length(); i++) {
							jsonElem = (JSONObject) jsonArrayElem.get(i);
							String info = "";
							if ("Assessment Reduction".equals(jsonElem.getString("Type"))) {
								info = jsonElem.getString("TaxYear");
								row1.add(info);
								info = jsonElem.getString("Value");
								row2.add("$" + info);
							} else if ("Homestead".equals(jsonElem.getString("Description"))) {
								info = jsonElem.getString("Value");
								row3.add("$" + info);
							}  else if ("Second Homestead".equals(jsonElem.getString("Description"))) {
								info = jsonElem.getString("Value");
								row4.add("$" + info);
							}
						}
						
						if (row1.size() < 5) {
							int firstYear = 0;
							if (row1.size() == 2) {
								jsonElem = (JSONObject) jsonArrayElem.get(0);
								String info = jsonElem.getString("TaxYear");
								firstYear = Integer.parseInt(info);
								row1.add(info);
							} else {
								firstYear = Integer.parseInt(row1.get(2));
							}
							
							int decreaseYear = 1;
							for (int j=row1.size(); j<5; j++) {
								row1.add(Integer.toString(firstYear-decreaseYear));
								decreaseYear ++;
							}
						}
						if (row2.size() < 5) {
							for (int j=row2.size(); j<5; j++) {
								row2.add(" ");
							}
						}
						if (row3.size() < 5) {
							for (int j=row3.size(); j<5; j++) {
								row3.add(" ");
							}
						}
						if (row4.size() < 5) {
							for (int j=row4.size(); j<5; j++) {
								row4.add(" ");
							}
						}
						
						List<String> row = new ArrayList<String>();
						for (int i=0; i<4; i++) {
							switch (i) {
								case 0:
									row = row1;
									break;
								case 1:
									row = row2;
									break;
								case 2:
									row = row3;
									break;
								case 3:
									row = row4;
									break;
							}
								
							detPgTable.append("<tr>");
							detPgTable.append("<td> <b>" + row.get(0) + " </b> </td>" +
									"<td> " + (i==0 ? "<b>" : "") + row.get(1) + (i==0 ? "</b>" : "") +"</td> " + 
									"<td> " + (i==0 ? "<b>" : "") + row.get(2) + (i==0 ? "</b>" : "") + "</td> " +
									"<td> " + (i==0 ? "<b>" : "") + row.get(3) + (i==0 ? "</b>" : "") + "</td>");
							detPgTable.append("</tr>");
						}
					} else {
						detPgTable.append("<tr> <td> <b> " + row1.get(0) + "</b> </td> <td> <b> " + row1.get(1) + " </b> </td> " + 
								"<td> <b> 2013 </b> </td> <td> <b> 2012 </b> </td> <td> <b> 2011</b> </td>" +
							"</tr>");
					}
					
					detPgTable.append("<tr> <td colspan=\"5\"> Note: Not all benefits are applicable to all Taxable Values (i.e. County, School Board, City, Regional). " +
							"</td> </tr> </table>");
						
					detPgTable.append(" </td> </tr>");
					detPgTable.append("<tr border=\"0\"> <td colspan=\"2\"> <br><br><br><br> </td> </tr>");
				}
			}
			
			
			//Taxable Value Information
			detPgTable.append("<tr style=\"background-color:grey; height:20px; font:bold; color:white;\">" +
					"<td colspan=\"2\"> TAXABLE VALUE INFORMATION </td> </tr>");
			jsonElem = (JSONObject) jsonObject.get("Taxable");
			if (jsonElem != null) {
				HashMap<Integer, List<String>> map = new HashMap<Integer, List<String>>();
				if ((resultsObj = jsonElem.get("TaxableInfos")) instanceof JSONArray) {
					jsonArrayElem = (JSONArray) resultsObj;
					detPgTable.append("<tr> <td colspan=\"2\"> <table border=\"0\"  width=\"650\" cellpadding=\"5\">");
					
					for (int i=0; i<jsonArrayElem.length(); i++) {
						jsonElem = (JSONObject) jsonArrayElem.get(i);
						List<String> yearInfo = new ArrayList<String>();
						String info = jsonElem.getString("Year");
						yearInfo.add((StringUtils.isNotEmpty(info)) ? info : "");
						info = jsonElem.getString("CountyExemptionValue");
						yearInfo.add((StringUtils.isNotEmpty(info)) ? ("$" + info) : "");
						info = jsonElem.getString("CountyTaxableValue");
						yearInfo.add((StringUtils.isNotEmpty(info)) ? ("$" + info) : "");
						info = jsonElem.getString("SchoolExemptionValue");
						yearInfo.add((StringUtils.isNotEmpty(info)) ? ("$" + info) : "");
						info = jsonElem.getString("SchoolTaxableValue");
						yearInfo.add((StringUtils.isNotEmpty(info)) ? ("$" + info) : "");
						info = jsonElem.getString("CityExemptionValue");
						yearInfo.add((StringUtils.isNotEmpty(info)) ? ("$" + info) : "");
						info = jsonElem.getString("CityTaxableValue");
						yearInfo.add((StringUtils.isNotEmpty(info)) ? ("$" + info) : "");
						info = jsonElem.getString("RegionalExemptionValue");
						yearInfo.add((StringUtils.isNotEmpty(info)) ? ("$" + info) : "");
						info = jsonElem.getString("RegionalTaxableValue");
						yearInfo.add((StringUtils.isNotEmpty(info)) ? ("$" + info) : "");
						
						map.put(i, yearInfo);
					}
					
					StringBuffer row = new StringBuffer();
					row.append("<td> <b> " + map.get(0).get(0) + " </b> </td>");
					row.append("<td> <b> " + map.get(1).get(0) + " </b> </td>");
					row.append("<td> <b> " + map.get(2).get(0) + " </b> </td>");
					detPgTable.append("<tr> <td> </td> " + row.toString() + "</tr>");
					detPgTable.append("<tr style=\"background-color:lightgrey;\"> <td colspan=\"4\"> COUNTY </td> </tr>");
					row = new StringBuffer();
					row.append("<td> " + map.get(0).get(1) + "</td>");
					row.append("<td> " + map.get(1).get(1) + "</td>");
					row.append("<td> " + map.get(2).get(1) + "</td>");
					detPgTable.append("<tr> <td> Exemption Value </td> " + row.toString() + "</tr>");
					row = new StringBuffer();
					row.append("<td> " + map.get(0).get(2) + "</td>");
					row.append("<td> " + map.get(1).get(2) + "</td>");
					row.append("<td> " + map.get(2).get(2) + "</td>");
					detPgTable.append("<tr> <td> Taxable Value </td> " + row.toString() + "</tr>");
					
					detPgTable.append("<tr style=\"background-color:lightgrey;\"> <td colspan=\"4\"> SCHOOL BOARD </td> </tr>");
					row = new StringBuffer();
					row.append("<td> " + map.get(0).get(3) + "</td>");
					row.append("<td> " + map.get(1).get(3) + "</td>");
					row.append("<td> " + map.get(2).get(3) + "</td>");
					detPgTable.append("<tr> <td> Exemption Value </td> " + row.toString() + "</tr>");
					row = new StringBuffer();
					row.append("<td> " + map.get(0).get(4) + "</td>");
					row.append("<td> " + map.get(1).get(4) + "</td>");
					row.append("<td> " + map.get(2).get(4) + "</td>");
					detPgTable.append("<tr> <td> Taxable Value </td> " + row.toString() + "</tr>");
					
					detPgTable.append("<tr style=\"background-color:lightgrey;\"> <td colspan=\"4\"> CITY </td> </tr>");
					row = new StringBuffer();
					row.append("<td> " + map.get(0).get(5) + "</td>");
					row.append("<td> " + map.get(1).get(5) + "</td>");
					row.append("<td> " + map.get(2).get(5) + "</td>");
					detPgTable.append("<tr> <td> Exemption Value </td> " + row.toString() + "</tr>");
					row = new StringBuffer();
					row.append("<td> " + map.get(0).get(6) + "</td>");
					row.append("<td> " + map.get(1).get(6) + "</td>");
					row.append("<td> " + map.get(2).get(6) + "</td>");
					detPgTable.append("<tr> <td> Taxable Value </td> " + row.toString() + "</tr>");
					
					detPgTable.append("<tr style=\"background-color:lightgrey;\"> <td colspan=\"4\"> REGIONAL </td> </tr>");
					row = new StringBuffer();
					row.append("<td> " + map.get(0).get(7) + "</td>");
					row.append("<td> " + map.get(1).get(7) + "</td>");
					row.append("<td> " + map.get(2).get(7) + "</td>");
					detPgTable.append("<tr> <td> Exemption Value </td> " + row.toString() + "</tr>");
					row = new StringBuffer();
					row.append("<td> " + map.get(0).get(8) + "</td>");
					row.append("<td> " + map.get(1).get(8) + "</td>");
					row.append("<td> " + map.get(2).get(8) + "</td>");
					detPgTable.append("<tr> <td> Taxable Value </td> " + row.toString() + "</tr>");
					
					detPgTable.append("</table>");
				}
				detPgTable.append("<tr border=\"0\"> <td colspan=\"2\"> <br><br><br><br> </td> </tr>");
			}
			
			
			//Full Legal Description 
			jsonElem = (JSONObject) jsonObject.get("LegalDescription");
			if (jsonElem != null) {
				detPgTable.append("<tr style=\"background-color:grey; height:20px; font:bold; color:white;\">" +
						"<td colspan=\"2\"> FULL LEGAL DESCRIPTION </td>" +
					"</tr>");
				String info = jsonElem.getString("Description");
				info = info.replaceAll("(?is)\\b\\s*\\|\\s*\\b", "<br>");
				detPgTable.append("<tr>" +
						"<td align=\"left\" id=\"legalDesc\" colspan=\"2\">" + info + "</td>" +
					"</tr>");
			}
			detPgTable.append("<tr border=\"0\"> <td colspan=\"2\"> <br><br><br><br> </td> </tr>");
			
			//Sales Info
			detPgTable.append("<tr style=\"background-color:grey; height:20px; font:bold; color:white;\">" +
					"<td colspan=\"2\"> SALES INFORMATION </td> </tr>");
			if ((resultsObj = jsonObject.get("SalesInfos")) instanceof JSONArray) {
					jsonArrayElem = (JSONArray) resultsObj;
					if (jsonArrayElem.length() > 0) {
						detPgTable.append("<tr> <td colspan=\"2\"> <table border=\"0\"  width=\"650\" cellpadding=\"5\" id=\"sales\">");
						detPgTable.append("<tr align=\"center\"> <th> Previous Sale </th> <th> Price </th> <th> OR Book-Page </th> <th> Qualification Description </th> </tr>");
						
						for (int i=0; i<jsonArrayElem.length(); i++) {
							jsonElem = (JSONObject) jsonArrayElem.get(i);
							String date = jsonElem.getString("DateOfSale");
							String price = jsonElem.getString("SalePrice");
							String book = jsonElem.getString("OfficialRecordBook");
							String page = jsonElem.getString("OfficialRecordPage");
							String info = jsonElem.getString("QualificationDescription");
							detPgTable.append("<tr align=\"center\"> " +
									"<td> " + date + "</td> " +
									"<td> $" + price + "</td> " +
									"<td> " + book + " - " + page + "</td> " +
									"<td> " + info + "</td> " +
								"</tr>");
						}
						detPgTable.append("</table> ");
					}
				}
			detPgTable.append("</table> <br><br>");
			
			details = detPgTable.toString();
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return details;
	}
			
	
	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			String details = "";
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<html")){
				NodeList parcelIDNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("font"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "resultsprop"));
				if (parcelIDNodes.size()>1) {
					parcelNumber.append(parcelIDNodes.elementAt(1).getParent().toPlainTextString().trim());
				}
				return rsResponse;
			}
			
			NodeList parcelIDNodes = nodeList.extractAllNodesThatMatch(new TagNameFilter("font"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "resultsprop"));
			if (parcelIDNodes.size()>1) {
				parcelNumber.append(parcelIDNodes.elementAt(1).getParent().toPlainTextString().trim());
			}
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "200"));
			
			if (tables.size()>3) {
				TableTag table = (TableTag)tables.elementAt(3);
				table.removeChild(0);
				table.removeChild(0);
				table.removeChild(table.getChildCount()-1);
				details = table.toHtml();
			}
			
			details = details.replaceAll("(?is)width='200'", "width='50%'");
			
			//get additional sales
			String link = "";
			Matcher ma = Pattern.compile("(?is)href='javascript:ShowPreviousSales\\(&quot;([^&]+)&quot;,&quot;([^&]+)&quot;\\);'").matcher(details);
			if (ma.find()) {
				link = getBaseLink().replaceFirst("propmap.asp?+","ShowPreviousSales.asp?folio=" + ma.group(1) + "&address=" + ma.group(2));
				String addSales = getLinkContents(link);
				
				org.htmlparser.Parser htmlParser2 = org.htmlparser.Parser.createParser(addSales, null);
				NodeList nodeList2 = htmlParser2.parse(null);
				NodeList tableNodes = nodeList2.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "760"))
						.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"));
				if (tableNodes.size()>0) {
					TableTag table = (TableTag)tableNodes.elementAt(0);	//remove first row
					table.removeChild(0);
					String newTable = "<tr><td align=\"center\"><b>Sales History</b>" + 
							table.toHtml().replaceFirst("(?is)<table", "<table id=\"sales\"")
							.replaceFirst("(?is)width='760'", "width='100%'")
							.replaceAll("(?is)nowrap='true'", "") + "</td></tr>";
					int index = details.toLowerCase().lastIndexOf("</table>"); 
					details = details.substring(0, index) + newTable + details.substring(index); 		
				}
			}
			
			details = details.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1");
			details = details.replaceAll("(?is)View Additional Sales", "&nbsp;");
			
			return details;
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		TableTag resultsTable = null;
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			
			if (mainTable.size()>0) {
				resultsTable = (TableTag)mainTable.elementAt(0);
			}
			
			if (resultsTable != null && resultsTable.getRowCount()>0) {
				TableRow[] rows  = resultsTable.getRows();
				int len = rows.length;
				for (int i = 1; i < len; i++) {
					TableRow row = rows[i];
					if (row.getColumnCount() == 4) {
						String link = "";
						String htmlRow = row.toHtml();
						
						LinkTag linkTag = ((LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
						link = linkTag.extractLink().trim().replaceAll("\\s", "%20");
						
						ParsedResponse currentResponse = new ParsedResponse();
						
						currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
										
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
						currentResponse.setOnlyResponse(htmlRow);
						
						ResultMap m = ro.cst.tsearch.servers.functions.FLMiamiDadeAO.parseIntermediaryRow(row, searchId );
						Bridge bridge = new Bridge(currentResponse, m, searchId);
						
						DocumentI document = (AssessorDocumentI)bridge.importData();				
						currentResponse.setDocument(document);
						
						intermediaryResponse.add(currentResponse);
					}
					 
				}
				
				//String headerRow = resultsTable.getRow(0).toPlainTextString().replaceAll("(?is)Search Results:", "");
				
//				String header = "<table align=\"center\"><tr><td align=\"center\">" + headerRow + 
//						"</td></tr><tr><td>";
				String header = "<table id=\"intermediaries\" border=\"1\" cellpadding=\"0\" cellspacing=\"0\" width=\"95%\"><tr>" +
						"<th>Folio</th>" +
						"<th>Subdivision</th>" +
						"<th>Owner</th>" +
						"<th>Address</th>" +
						"</tr>";
				String footer = "</table>";
				
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter(footer);
												
				outputTable.append(mainTable.toHtml());
			}
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
//	@SuppressWarnings("rawtypes")
//	@Override
//	protected Object parseAndFillResultMap(ServerResponse response,	String detailsHtml, ResultMap resultMap) {
//		try {
//			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"AO");
//			
//			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
//			NodeList nodeList = htmlParser.parse(null);
//			
//			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
//					.extractAllNodesThatMatch(new HasAttributeFilter("bgcolor", "#FFFFFF"));
//			TableTag summaryTable = null;
//			TableTag propertyTable = null;
//			TableTag assessmentTable = null;
//			TableTag saleTable = null;
//			for (int i=0;i<tables.size();i++) {
//				if (tables.elementAt(i).toPlainTextString().contains("Folio No")) {
//					summaryTable = (TableTag)tables.elementAt(i);
//				} else if (tables.elementAt(i).toPlainTextString().contains("Legal Description")) {
//					propertyTable = (TableTag)tables.elementAt(i);
//				} else if (tables.elementAt(i).toPlainTextString().contains("Assessed Value")) {
//					assessmentTable = (TableTag)tables.elementAt(i);
//				} else if (tables.elementAt(i).toPlainTextString().contains("Sale Date")) {
//					saleTable = (TableTag)tables.elementAt(i);
//				}
//			}
//			
//			if (summaryTable!=null) {
//				if (summaryTable.getRowCount()>0) {
//					TableRow row = summaryTable.getRow(0);
//					if (row.getColumnCount()>1) {
//						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), 
//								row.getColumns()[1].toPlainTextString().trim());
//					}
//				}
//				if (summaryTable.getRowCount()>1) {
//					TableRow row = summaryTable.getRow(1);
//					if (row.getColumnCount()>1) {
//						resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), 
//								row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
//					}
//				}
//				if (summaryTable.getRowCount()>2) {
//					TableRow row = summaryTable.getRow(2);
//					if (row.getColumnCount()>1) {
//						resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), 
//								row.getColumns()[1].toHtml().replaceAll("(?is)</?td[^>]*>", "").
//								replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)&nbsp;", "").trim());
//					}
//				}
//			}
//			
//			if (propertyTable!=null) {
//				int rowNumber = propertyTable.getRowCount();
//				if (rowNumber>0) {
//					TableRow row = propertyTable.getRow(rowNumber-1);
//					if (row.getColumnCount()>1) {
//						resultMap.put(/*PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName()*/"tmpFullLegalDescr", 
//								row.getColumns()[1].toPlainTextString().trim());
//					}
//				}
//			}
//			
//			if (assessmentTable!=null) {
//				if (assessmentTable.getRowCount()>0) {
//					TableRow row = assessmentTable.getRow(0);
//					if (row.getColumnCount()>1) {
//						resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), 
//								row.getColumns()[1].toPlainTextString().trim());
//					}
//				}
//				if (assessmentTable.getRowCount()>1) {
//					TableRow row = assessmentTable.getRow(1);
//					if (row.getColumnCount()>1) {
//						resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), 
//								row.getColumns()[1].toPlainTextString().replaceAll("[,$]", "").trim());
//					}
//				}
//				if (assessmentTable.getRowCount()>2) {
//					TableRow row = assessmentTable.getRow(2);
//					if (row.getColumnCount()>1) {
//						resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), 
//								row.getColumns()[1].toPlainTextString().replaceAll("[,$]", "").trim());
//					}
//				}
//				if (assessmentTable.getRowCount()>3) {
//					TableRow row = assessmentTable.getRow(3);
//					if (row.getColumnCount()>1) {
//						resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), 
//								row.getColumns()[1].toPlainTextString().replaceAll("[,$]", "").trim());
//					}
//				}
//				if (assessmentTable.getRowCount()>4) {
//					TableRow row = assessmentTable.getRow(4);
//					if (row.getColumnCount()>1) {
//						resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), 
//								row.getColumns()[1].toPlainTextString().replaceAll("[,$]", "").trim());
//					}
//				}
//			}
//			
//			TableTag salesTable = null;
//			NodeList salesTables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
//					.extractAllNodesThatMatch(new HasAttributeFilter("id", "sales"));
//			if (salesTables.size()>0) {
//				salesTable = (TableTag)salesTables.elementAt(0);
//			}
//			
//			if (salesTable!=null) {
//				List<List> tablebody = null;
//				ResultTable salesHistory = new ResultTable();
//				tablebody = new ArrayList<List>();
//				List<String> list;
//				for (int i=1;i<salesTable.getRowCount(); i++)
//				{
//					TableRow row = salesTable.getRow(i);
//					if (row.getColumnCount()>=3) {
//						list = new ArrayList<String>();
//						list.add(row.getColumns()[0].toPlainTextString().trim());								//date
//						list.add(row.getColumns()[1].toPlainTextString().replaceAll("[,$]", "").trim());		//price
//						String bookPage = row.getColumns()[2].toPlainTextString().trim();
//						String[] split = bookPage.split("-");
//						if (split.length==2) {
//							split[0] = split[0].replaceFirst("^0+", "");
//							split[1] = split[1].replaceFirst("^0+", "");
//							list.add(split[0]);
//							list.add(split[1]);
//						}
//						tablebody.add(list);
//					}
//				}
//				
//				String[] header = {SaleDataSetKey.RECORDED_DATE.getShortKeyName(), SaleDataSetKey.SALES_PRICE.getShortKeyName(), 
//						SaleDataSetKey.BOOK.getShortKeyName(), SaleDataSetKey.PAGE.getShortKeyName()};
//				salesHistory = GenericFunctions2.createResultTable(tablebody, header);
//				if (salesHistory != null && tablebody.size()>0){
//					resultMap.put("SaleDataSet", salesHistory);
//				}
//			} else {
//				if (saleTable!=null) {
//					String recordedDate = "";
//					String salesPrice = "";
//					String book = "";
//					String page = "";
//					if (saleTable.getRowCount()>0) {
//						TableRow row = saleTable.getRow(0);
//						if (row.getColumnCount()>1) {
//							recordedDate = row.getColumns()[1].toPlainTextString().trim();
//						}
//					}
//					if (saleTable.getRowCount()>1) {
//						TableRow row = saleTable.getRow(1);
//						if (row.getColumnCount()>1) {
//							salesPrice = row.getColumns()[1].toPlainTextString().replaceAll("[,$]", "").trim();
//						}
//					}
//					if (saleTable.getRowCount()>2) {
//						TableRow row = saleTable.getRow(2);
//						if (row.getColumnCount()>1) {
//							String bookPage = row.getColumns()[1].toPlainTextString().trim();
//							String[] split = bookPage.split("-");
//							if (split.length==2) {
//								split[0] = split[0].replaceFirst("^0+", "");
//								split[1] = split[1].replaceFirst("^0+", "");
//								if (split[0].length()>0) {
//									book = split[0];
//								}
//								if (split[1].length()>0) {
//									page = split[1];
//								}
//							}
//						}
//					}
//					List<List> tablebody = null;
//					ResultTable salesHistory = new ResultTable();
//					tablebody = new ArrayList<List>();
//					List<String> list = new ArrayList<String>();
//					list.add(recordedDate);
//					list.add(salesPrice);
//					list.add(book);
//					list.add(page);
//					tablebody.add(list);
//					String[] header = {SaleDataSetKey.RECORDED_DATE.getShortKeyName(), SaleDataSetKey.SALES_PRICE.getShortKeyName(), 
//							SaleDataSetKey.BOOK.getShortKeyName(), SaleDataSetKey.PAGE.getShortKeyName()};
//					salesHistory = GenericFunctions2.createResultTable(tablebody, header);
//					if (salesHistory != null && tablebody.size()>0){
//						resultMap.put("SaleDataSet", salesHistory);
//					}
//				}
//			}
//			
//			ro.cst.tsearch.servers.functions.FLMiamiDadeAO.parseNames(resultMap, searchId);
//			ro.cst.tsearch.servers.functions.FLMiamiDadeAO.parseAddress(resultMap);
//			ro.cst.tsearch.servers.functions.FLMiamiDadeAO.parseLegalSummary(resultMap, searchId);
//		
//		} catch (Exception e) {
//			logger.error("Error while getting details", e);
//		}
//		return null;
//	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,	String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"AO");
		
			if (StringUtils.isEmpty(detailsHtml)) {
				return null;
			}
			
			HtmlParser3 parser = new HtmlParser3(detailsHtml);
			NodeList list = parser.getNodeList();
			
			if (list != null) {
				NodeList tables =  list.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (tables.size() > 0) {
					TableTag mainTable = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "propInfo")).elementAt(0) ;
					if (mainTable != null) {
						Node nodeInfo = mainTable.getChildren()
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "folioNo"), true)
								.elementAt(0);
						if (nodeInfo != null) {
							String folioNo = nodeInfo.getFirstChild().getText().trim();
							if (StringUtils.isNotEmpty(folioNo)) {
								resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), folioNo);
							}
						}
						
						nodeInfo = mainTable.getChildren()
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "propAddress"), true)
								.elementAt(0);
						if (nodeInfo != null) {
							String address = nodeInfo.getFirstChild().getText().trim();
							address = address.replaceAll("(?is)\\s{2,}", " ");
							if (StringUtils.isNotEmpty(address)) {
								resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
							}
						}
						
						nodeInfo = mainTable.getChildren()
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "ownerInfo"), true)
								.elementAt(0);
						if (nodeInfo != null) {
							String owners = nodeInfo.getFirstChild().getText().trim();
							if (StringUtils.isNotEmpty(owners)) {
								resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owners);
							}
						}
						
						nodeInfo = mainTable.getChildren()
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "legalDesc"), true)
								.elementAt(0);
						if (nodeInfo != null) {
							String legal = nodeInfo.getChildren().toHtml().trim();
							legal = legal.replaceAll("(?is)\\|", "<br>").replaceAll("\\s{2,}", " ");
							if (StringUtils.isNotEmpty(legal)) {
								resultMap.put("tmpFullLegalDescr", legal);
							}
						}
						
						nodeInfo = mainTable.getChildren()
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "assessmentTable"), true)
								.elementAt(0);
						if (nodeInfo != null) {
							TableTag tbl = (TableTag) nodeInfo;
							if (tbl.getRowCount() > 5) {
								String info = tbl.getRows()[1].getColumns()[1].getChildrenHTML().trim();
								if (StringUtils.isNotEmpty(info)) {
									resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), info.replaceAll("[,$]", "").trim());
								}
								info = tbl.getRows()[3].getColumns()[1].getChildrenHTML().trim();
								if (StringUtils.isNotEmpty(info)) {
									resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), info.replaceAll("[,$]", "").trim());
								}
								info = tbl.getRows()[4].getColumns()[1].getChildrenHTML().trim();
								if (StringUtils.isNotEmpty(info)) {
									resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), info.replaceAll("[,$]", "").trim());
								}
								info = tbl.getRows()[5].getColumns()[1].getChildrenHTML().trim();
								if (StringUtils.isNotEmpty(info)) {
									resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), info.replaceAll("[,$]", "").trim());
								}
							}
						}
						
						TableTag salesTable = null;
						nodeInfo = mainTable.getChildren()	
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "sales"), true)
								.elementAt(0);
						if (nodeInfo != null) {
							salesTable = (TableTag)nodeInfo;
							List<List> tablebody = null;
							ResultTable salesHistory = new ResultTable();
							tablebody = new ArrayList<List>();
							List<String> listOfRef;
							
							for (int i=1; i<salesTable.getRowCount(); i++) {
								TableRow row = salesTable.getRow(i);
								if (row.getColumnCount()>=3) {
									listOfRef = new ArrayList<String>();
									listOfRef.add(row.getColumns()[0].toPlainTextString().trim());								//date
									listOfRef.add(row.getColumns()[1].toPlainTextString().replaceAll("[,$]", "").trim());		//price
									String bookPage = row.getColumns()[2].toPlainTextString().trim();
									String[] split = bookPage.split("-");
									if (split.length==2) {
										split[0] = split[0].replaceFirst("^0+", "");
										split[1] = split[1].replaceFirst("^0+", "");
										listOfRef.add(split[0]);
										listOfRef.add(split[1]);
									}
									tablebody.add(listOfRef);
								}
							}
							
							String[] header = {SaleDataSetKey.RECORDED_DATE.getShortKeyName(), SaleDataSetKey.SALES_PRICE.getShortKeyName(), 
									SaleDataSetKey.BOOK.getShortKeyName(), SaleDataSetKey.PAGE.getShortKeyName()};
							salesHistory = GenericFunctions2.createResultTable(tablebody, header);
							if (salesHistory != null && tablebody.size()>0){
								resultMap.put("SaleDataSet", salesHistory);
							}
						}
					}
				}
				
			}
			
			ro.cst.tsearch.servers.functions.FLMiamiDadeAO.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.FLMiamiDadeAO.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.FLMiamiDadeAO.parseLegalSummary(resultMap, searchId);
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO_GENERIC_AO);
			moduleList.add(module);
		}

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);

		if (hasStreetNo() && hasStreet()) {
			FilterResponse nameFilter = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(1, SearchAttributes.P_STREET_NO_NAME);
				module.addFilter(addressFilter);
				module.addFilter(nameFilter);
				moduleList.add(module);
			}
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(1, SearchAttributes.P_STREETNAME);
				module.addFilter(addressFilter);
				module.addFilter(nameFilter);
				moduleList.add(module);
			}
		}

		if (hasOwner()) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,	searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(2,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L;F;", "L;M;" }));
			moduleList.add(module);
		}

		serverInfo.setModulesForAutoSearch(moduleList);
	}
}

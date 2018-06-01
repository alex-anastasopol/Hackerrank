package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
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
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
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
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class COEagleTR extends TSServer {
	
	
	protected static final Pattern TRANFERS_BOOK_PAGE_PATTERN = 
		Pattern.compile("\\s*B:?\\s*(\\d+)\\s*P:?\\s*(\\d+)\\s*");
	
	private static ArrayList<String> cities = new ArrayList<String>();
	static {
		cities.add("AVON");
		cities.add("BASALT");
		cities.add("EAGLE");
		cities.add("EAGLE-VAIL");
		cities.add("EDWARDS");
		cities.add("EL JEBEL");
		cities.add("GYPSUM");
		cities.add("MINTURN");
		cities.add("RED CLIFF");
		cities.add("VAIL");
		cities.add("GILMAN");
	}
	
	private static final long serialVersionUID = 1L;
	
	public COEagleTR(long searchId) {
		super(searchId);
	}
	
	public COEagleTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
			// no result
			if (rsResponse.indexOf("No results found for query") > -1) {
				Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
				return;
			}
			
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

			StringBuilder accountId = new StringBuilder();
			String details = getDetails(rsResponse, accountId);
			String filename = accountId + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(accountId.toString(),null,data)){
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse( details );
				
			} else {
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse( details );
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
			}
			
			break;
		case ID_GET_LINK :
			ParseResponse(sAction, Response, rsResponse.contains("Sort Search Results")
														? ID_SEARCH_BY_NAME
														: ID_DETAILS);
			break;
		default:
			break;
		}
		
		
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if(data != null) {
			data.put("type","CNTYTAX");
		}
	}

	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				NodeList headerList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("h1"), true);
				if(headerList.size() == 0) {
					return null;
				} else {
					String account = headerList.elementAt(0).toPlainTextString()
						.replace("Account:", "")
						.replace("&nbsp;", "")
						.replaceAll("\\s+", "");
					accountId.append(account);	
				}
				return rsResponse;
			}
			
			NodeList tableAccountSummaryList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "middle"),true)
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "accountSummary"));
			if(tableAccountSummaryList.size() != 1) {
				return null;
			}
			NodeList headerList = nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "middle"),true)
				.extractAllNodesThatMatch(new TagNameFilter("h1"), true);
			if(headerList.size() == 0) {
				return null;
			} else {
				String account = headerList.elementAt(0).toPlainTextString()
					.replace("Account:", "")
					.replace("&nbsp;", "")
					.replaceAll("\\s+", "");
				accountId.append(account);	
			}
			
			TableTag mainTable = (TableTag) tableAccountSummaryList.elementAt(0);
			int posLastHeader = -1;
			int posLastRow = -1;
			TableRow[] rows = mainTable.getRows();
			
			for (int i = 0; i < rows.length; i++) {
				if(posLastHeader < 0) {
					if(rows[i].toPlainTextString().trim().equals("Images")) {
						posLastHeader = mainTable.findPositionOf(rows[i]);
					}
				} else {
					posLastRow = mainTable.findPositionOf(rows[i]);
					break;
				}
			}
			
			if(posLastRow > 0) {
				mainTable.removeChild(posLastRow);
			}
			if(posLastHeader > 0) {
				mainTable.removeChild(posLastHeader);
			}
			details.append("<table align=\"center\" border=\"1\"><tr><td align=\"center\">")
				.append(headerList.elementAt(0).toHtml())
				.append("</td></tr><tr><td align=\"center\">")
				.append(mainTable.toHtml().replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"))
				.append("</td></tr>");
			
			details.append("<tr><td align=\"center\">")
			   .append(getTaxHistory(accountId.toString()))
			   .append("</td></tr>");	
			
			details.append("</table>");
			return details.toString();
			
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	
	protected String getTaxHistory(String accountID) {
		//for Bill:            https://propertytax.eaglecounty.us/PropertyTaxSearch/TaxAccount/Bills/<xxx>
		//for Payment History: https://propertytax.eaglecounty.us/PropertyTaxSearch/TaxAccount/BillHistory/<xxx>, <xxx> = AccountNo
		
		String url = "https://propertytax.eaglecounty.us/PropertyTaxSearch/TaxAccount/Bills/";
		String link = url + accountID;
		String currentTaxInfo = "";
		String taxHistoryHtml = "";
		String taxInfo = "<div id=\"taxInfo\" align=\"center\">";
		
		try {
			String response = getLinkContents(link);
			HtmlParser3 htmlInfo = new HtmlParser3(response);
			NodeList list = htmlInfo.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "formContainer"));
			if (list != null) {
				list = list.extractAllNodesThatMatch(new TagNameFilter("div"),true)
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "ctrlHolder"));
				if (list.size() > 0) {
					for (int i=0; i < list.size(); i++) {
						currentTaxInfo += list.elementAt(i).toHtml();
						currentTaxInfo = currentTaxInfo.replaceFirst("(?is)\\s*<div[^>]+>\\s*", "");
						currentTaxInfo = currentTaxInfo.replaceFirst("(?is)\\s*</div>\\s*", "");
						if (i == 0) {
							currentTaxInfo = currentTaxInfo.replaceFirst("(?is)<ul[^>]+>", "<table id=\"currentTaxes\"> <tr> <td colspan = \"7\" align = \"center\"> " +
									"<br><br> <h3> <font style=\"color: darkblue;\"> " +
									"TAX INFORMATION FOR ACCOUNT NO " + accountID + " </font> </h3> </td> </tr> <tr>");
							currentTaxInfo = currentTaxInfo.replaceFirst("(?is)</ul>", "</tr>");
							currentTaxInfo = currentTaxInfo.replaceAll("(?is)<li ([^>]+>\\s*)<label>([^<]+)</label>\\s*</li>", "<td " + "$1" + "$2" + "</td>");
						} else {
							currentTaxInfo = currentTaxInfo.replaceAll("(?is)\\s*<input[^>]+>","");
							currentTaxInfo = currentTaxInfo.replaceFirst("(?is)<ul[^>]+>", "<tr>");
							currentTaxInfo = currentTaxInfo.replaceFirst("(?is)</ul>", "</tr>");
							currentTaxInfo = currentTaxInfo.replaceAll("(?is)<li ([^>]+>\\s*)<label>([^<]+)</label>\\s*</li>", "<td " + "$1" + "$2" + "</td>");
							currentTaxInfo = currentTaxInfo.replaceAll("(?is)<li ([^>]+>\\s*)\\s*</li>", "<td " + "$1" + " " + "</td>");
							if (currentTaxInfo.contains("Where are my ") && currentTaxInfo.contains("taxes going?")) {
								currentTaxInfo = currentTaxInfo.replaceFirst("(?is)<ul[^>]+>Where are my[^\\?]+\\?\\s*</ul>\\s*<ul[^>]+>.*</ul>", "</table>");
							}
						}
					}
					
					currentTaxInfo = currentTaxInfo.replaceFirst("(?is)<td[^>]+>\\s*Pay[^/]+/td>\\s*</tr>", "</tr>");
					currentTaxInfo = currentTaxInfo.replaceFirst("(?is)<td[^>]+>\\s*</td>\\s*</tr>", "</tr>");
					currentTaxInfo = currentTaxInfo.replaceFirst("(?is)<li[^>]+>\\s*<[^>]+>\\s*(?:Full )?payment[^>]+>\\s*</li>", "");
					
				}
			}
			
			url = "https://propertytax.eaglecounty.us/PropertyTaxSearch/TaxAccount/BillHistory/";
			link = url + accountID;
			taxHistoryHtml = "<table id=\"paymentTaxHistory\"> <tr> <td colspan = \"7\" align = \"center\"> " +
									"<br> <h3> <font style=\"color: darkblue;\"> " +
									"PAYMENT TAX HISTORY FOR ACCOUNT NO " + accountID + " </font> </h3> </td> </tr>";
			
			response = getLinkContents(link);
			htmlInfo = new HtmlParser3(response);
			list = htmlInfo.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "formContainer"))
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "alternate"), true);
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					String tmp = list.elementAt(i).toHtml();
					tmp = tmp.replaceFirst("(?is)<ul[^>]+>", "<tr>");
					tmp = tmp.replaceFirst("(?is)</ul>", "</tr>");
					tmp = tmp.replaceAll("(?is)<li ([^>]+>\\s*)<label>([^<]+)</label>\\s*</li>", "<td " + "$1" + "$2" + "</td>");
					tmp = tmp.replaceAll("(?is)<li ([^>]+>\\s*)\\s*</li>", "<td " + "$1" + " " + "</td>");
					taxHistoryHtml += tmp;
				}
				taxHistoryHtml += "</table>";
			}
			
			taxInfo += currentTaxInfo + taxHistoryHtml + "</div>";
			
		} catch (Exception e) {
			logger.error("Error while getting getTaxHistory from:" + link, e);
		}
		
		return taxInfo;
	}
	

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id","searchResultsTable"), true);
			
			if(mainTableList.size() != 1) {
				return intermediaryResponse;
			}
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows  = tableTag.getRows();
			String footer = "";
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 3) {
	
					LinkTag linkTag = ((LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					
					String link = CreatePartialLink(TSConnectionURL.idGET) + "/assessor/taxweb/" + 
						linkTag.extractLink().trim().replaceAll("\\s", "%20");
					
					linkTag.setLink(link);
					
					// remove link from second column - Task8601
					linkTag = ((LinkTag)row.getColumns()[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					NodeList linkChildren = linkTag.getChildren();
					linkTag.getParent().setChildren(linkChildren);
					
					if(row.getChildCount() == 7) {
						row.removeChild(5);
					}
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml().replaceAll("(?is)fontcolor", "font color"));
					currentResponse.setOnlyResponse(row.toHtml().replaceAll("(?is)fontcolor", "font color"));
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = ro.cst.tsearch.servers.functions.COEagleTR.parseIntermediaryRow( row, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				} 
			}
			footer = processLinks(response,nodeList);
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + "<tr><th>Account#</th><th>Summary</th></tr>");
			response.getParsedResponse().setFooter(footer + "</table>");

			
			outputTable.append(table);
			
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,	String detailsHtml, ResultMap resultMap) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList headerList = nodeList.extractAllNodesThatMatch(new TagNameFilter("h1"), true);
			String accountId = null;
			if(headerList.size() == 0) {
				return null;
			} else {
				accountId = headerList.elementAt(0).toPlainTextString()
					.replace("Account:", "")
					.replace("&nbsp;", "")
					.replaceAll("\\s+", "");
					
			}
			
			resultMap.put("PropertyIdentificationSet.ParcelID", accountId);
			resultMap.put("OtherInformationSet.SrcType","TR");
			
			NodeList someNodeList = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new TagNameFilter("td"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "40%"))
				.extractAllNodesThatMatch(new TagNameFilter("tr"), true);
			
			String ownerName = "";
			String inCareOfName = "";
			
			for (int i = 0; i < someNodeList.size(); i++) {
				TableRow row = (TableRow) someNodeList.elementAt(i);
				String plainText = row.toPlainTextString().trim();
				if(plainText.startsWith("Parcel Number")) {
					resultMap.put("PropertyIdentificationSet.ParcelIDParcel",
							plainText.replace("Parcel Number", "").trim().replaceAll("-", ""));
				} else if(plainText.startsWith("Situs Address")) {
					String[] address = StringFormats.parseAddress(
							plainText.replace("Situs Address", "").trim());
					resultMap.put("PropertyIdentificationSet.StreetNo", address[0].replaceAll("^0+", ""));
					resultMap.put("PropertyIdentificationSet.StreetName", address[1]);
				} else if(plainText.startsWith("Tax Area")) {
					String taxArea = plainText.replace("Tax Area", "").trim();
					taxArea = taxArea.replaceAll("(?is)[^\\-]*-([^\\-]*)-.*", "$1").replaceAll("(?is)/", "-");
					taxArea = taxArea.replaceAll("(?is)\\(\\s*TOWN\\s*\\).*", "").trim();
					if(cities.contains(taxArea.toUpperCase())) {
						resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), taxArea);
					}
				} else if(plainText.startsWith("Owner Name")) {
					ownerName = plainText.replace("Owner Name", "").trim();
				} else if (plainText.startsWith("In Care Of Name")){
					inCareOfName = plainText.replace("In Care Of Name", "").trim();
				} else if(plainText.startsWith("Legal Summary")) {
					plainText = row.getChildrenHTML();
					plainText = plainText.replaceAll("<br>", " ");
					plainText = plainText.replaceAll("</?td>", "");
					plainText = plainText.replaceAll("</?b>", "");
					resultMap.put("PropertyIdentificationSet.PropertyDescription", 
							plainText.replace("Legal Summary", "").replaceAll("\\s{2,}", " ").trim());
				} else if(plainText.startsWith("Primary Taxable")) {
					resultMap.put("PropertyAppraisalSet.TotalAssessment", 
							plainText.replace("Primary Taxable", "").replaceAll("[,$]","").trim());
				} else if(plainText.startsWith("Actual")) {
					resultMap.put("PropertyAppraisalSet.TotalAppraisal", 
							plainText.replaceAll("Actual \\(\\d+\\)", "").replaceAll("[,$]","").trim());
				}
			}
			
			if (inCareOfName.length()!=0)
				ownerName += " InCareOfName " + inCareOfName;
			resultMap.put("PropertyIdentificationSet.NameOnServer",	ownerName.trim());
			
			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			List<String> line = null;
			
			NodeList tableAccountSummaryList = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "accountSummary"));
			if(tableAccountSummaryList.size() > 0) {
				TableTag mainTable = (TableTag) tableAccountSummaryList.elementAt(0);				
				TableRow[] rows = mainTable.getRows();
				
				for (int i = 0; i < rows.length; i++) {
					if(rows[i].toPlainTextString().trim().equals("Transfers")) {
						i++;
						if (i < rows.length) {
							NodeList tranferRows = rows[i].getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"), true);
							
							for (int j = 1; j < tranferRows.size(); j++) {
								TableColumn[] columns = ((TableRow)tranferRows.elementAt(j)).getColumns();
								if(columns.length == 4) {
									line = new ArrayList<String>();
									line.add(columns[0].toPlainTextString().replaceAll("[,$]", "").trim());
									
									String recordedDate = columns[1].toPlainTextString().trim();
									String instrumentNumber = columns[2].toPlainTextString().trim();
									if(recordedDate.isEmpty() && instrumentNumber.length() >= 9) {
										recordedDate = instrumentNumber.substring(0, 4);
									}
									line.add(recordedDate);
									line.add(instrumentNumber);
									
									String bookPage = columns[3].toPlainTextString().trim();
									Matcher matcher = TRANFERS_BOOK_PAGE_PATTERN.matcher(bookPage);
									if(matcher.find()) {
										line.add(matcher.group(1).replaceAll("^0+", ""));
										line.add(matcher.group(2).replaceAll("^0+", ""));
									} else {
										line.add("");
										line.add("");
									}
									
									line.add("TRANSFER");
									body.add(line);
								}
							}
						}
					}
				}
			}
			
			nodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxInfo"), true);
			
			ro.cst.tsearch.servers.functions.COEagleTR.parseLegalSummary(resultMap, body, searchId);
			ro.cst.tsearch.servers.functions.COEagleTR.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.COEagleTR.parseTaxes(nodeList, resultMap, searchId);
			
			//adding all cross references - should contain transfer table and info parsed from legal description
			if(body != null && body.size() > 0) {
				ResultTable rt = new ResultTable();
				String[] header = {"SalesPrice", "InstrumentDate", "InstrumentNumber", "Book", "Page" , "DocumentType"};
				rt = GenericFunctions2.createResultTable(body, header);
				resultMap.put("SaleDataSet", rt);
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	protected String processLinks(ServerResponse response, NodeList nodeList) {
		StringBuilder footer = new StringBuilder("<tr><td colspan=\"2\">");
		NodeList divList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "middle"));
		if(divList.size() > 0	) {
			Div middleDiv = (Div) divList.elementAt(0);
			boolean foundTable = false;
			for (int i = 0; i < middleDiv.getChildCount(); i++) {
				Node node = middleDiv.getChild(i);
				if (node instanceof TableTag) {
					foundTable = true;
					continue;
				}
				if(!foundTable) {
					continue;
				}
				if (node instanceof LinkTag) {
					LinkTag link = (LinkTag) node;
					link.setLink(CreatePartialLink(TSConnectionURL.idGET) + "/assessor/taxweb/" + 
							link.extractLink().trim().replaceAll("\\s", "%20"));
					footer.append(link.toHtml());
					
					if(link.toPlainTextString().contains("Next")) {
						response.getParsedResponse().setNextLink("<a href=" + link.extractLink()+ ">Next</a>");
					}
				} else {
					footer.append(node.toPlainTextString());
				}
				
			}
		} else {
			return "";
		}
		return footer + "</td></tr>";
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			moduleList.add(module);
		}
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(1, getSearchAttribute(SearchAttributes.LD_PARCELNO).replaceAll("-", ""));
			moduleList.add(module);
		}
		if(hasPinParcelNo()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(1, getSearchAttribute(SearchAttributes.LD_PARCELNO_PARCEL).replaceAll("-", ""));
			moduleList.add(module);
		}
		boolean hasOwner = hasOwner();
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		if(hasStreet()) {
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module );
			
			if(hasStreetNo()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(11, SearchAttributes.P_STREETNO);
				module.setSaKey(13, SearchAttributes.P_STREETNAME);
				module.addFilter(addressFilter);
				module.addFilter(nameFilterHybrid);
				moduleList.add(module);
			}
			if(hasOwner) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				module.setSaKey(13, SearchAttributes.P_STREETNAME);
				module.addFilter(nameFilterHybrid);
				module.addFilter(addressFilter);
				module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"})
						);
				moduleList.add(module);
			}
		}
		if(hasOwner) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"})
					);
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}

}

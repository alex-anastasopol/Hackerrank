package ro.cst.tsearch.servers.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ObjectUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
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
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         Feb 15, 2011
 */

public class FLMarionTR extends TSServer {
	private static final long serialVersionUID = 1L;

//	private static ArrayList<String> cities	= new ArrayList<String>();
	private static String ALL_CITIES = "";
	static {
//		cities.add("CITY OF ANTHONY");
//		cities.add("CITY OF ALTOONA");
//		cities.add("CITY OF BELLEVIEW");
//		cities.add("CITY OF CITRA");
//		cities.add("MARION UNIN CORP COUNTY");
//		cities.add("CITY OF DUNNELLON");
//		cities.add("CITY OF FORT MCCOY");
//		cities.add("CITY OF MICANOPY");
//		cities.add("CITY OF OCALA");
//		cities.add("CITY OF OCKLAWAHA");
//		cities.add("CITY OF REDDICK");
//		cities.add("CITY OF SUMMERFIELD");
//		cities.add("CITY OF SILVER SPRINGS");
//		cities.add("CITY OF UMATILLA");
//		cities.add("VILLAGES OF MARION");
//		cities.add("CITY OF WEIRSDALE");
//		cities.add("CITY OF WILLISTON");
		try {
			String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments");
			File folder = new File(folderPath);
			if (!folder.exists() || !folder.isDirectory()) {
				throw new RuntimeException("The folder [" + folderPath	+ "] does not exist. Module Information not loaded!");
			}
			ALL_CITIES = FileUtils.readFileToString(new File(folderPath	+ File.separator + "FLMarionTRAllCities.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public FLMarionTR(long searchId) {
		super(searchId);
	}

	public FLMarionTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}

	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}

	String[] postParameters = { "__VIEWSTATE", "__EVENTVALIDATION" };

	public void setAction(String action) {
		synchronized (this) {
		}
	}

	private HashMap<String,String> getCities() {
		HashMap<String,String> allCities = new HashMap<String, String>();
		String text = ALL_CITIES;
		Matcher m = Pattern.compile("<option value=\"([A-Z]+)\">([^<]+)</option>").matcher(text);
		while (m.find()) {
			allCities.put(m.group(2), m.group(1)); // e.g: (CITY OF ANTHONY, AN)
		}
		
		return allCities;
	}
	
	
	@Override
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {
		String serverResult = response.getResult();

		boolean dataNotFound = serverResult.contains("Some account data is not viewable or is unavailable at this time") ||
					serverResult.contains("Tax Roll Data Unavailable") ||
					serverResult.contains("Your search returned no results");
		if (dataNotFound) {
			response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
			return;
		}

		response.setResult(cleanServerResult(response.getResult()));
		serverResult = response.getResult();
		switch (viParseID) {
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_INTERMEDIARY:
			StringBuilder outputTable = new StringBuilder();
			setAction(action);
			Collection<ParsedResponse> intermediary = smartParseIntermediary(response, serverResult, outputTable);
			if (intermediary.size() > 0) {
				response.getParsedResponse().setResultRows(new Vector<ParsedResponse>(intermediary));
				response.getParsedResponse().setOnlyResponse(outputTable.toString());
				response.getParsedResponse().setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,outputTable.toString());
			}
			break;
		case ID_SAVE_TO_TSD:
		case ID_DETAILS:

			StringBuilder accountId = new StringBuilder();
			StringBuilder year = new StringBuilder();
			serverResult = getDetails(serverResult, accountId, year);
			response.setResult(serverResult);
			String accountNumber = accountId.toString();
			String filename = accountNumber + ".html";
			if (viParseID == ID_DETAILS) {
				String originalLink = "http://www.mariontax.com"
						+ action.replace("?", "&") + "&"
						+ response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");
				data.put("year", year.toString());

				if (isInstrumentSaved(accountNumber, null, data) || isInstrumentSaved(accountNumber.replaceAll("[\\s-]+", ""), null,	data)) {
					serverResult += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, serverResult);

					serverResult = addSaveToTsdButton(serverResult, sSave2TSDLink, ID_DETAILS);
				}

				response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				response.getParsedResponse().setResponse(serverResult);

			} else {
				smartParseDetails(response, serverResult);

				msSaveToTSDFileName = filename;
				response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				response.getParsedResponse().setResponse(serverResult);

				msSaveToTSDResponce = serverResult + CreateFileAlreadyInTSD();
			}
			break;
		case ID_GET_LINK:
			if (serverResult.contains("Tax Roll Search Results")) {
				ParseResponse(action, response, ID_INTERMEDIARY);
			} else if (serverResult.contains("Tax Roll Property Summary")) {
				ParseResponse(action, response, ID_DETAILS);
			}
			break;

		}
	}

	private String cleanServerResult(String result) {
		// clean "Add to carts"
		result = result.replaceAll("(?is)<a (id=\"lnkCart.?\"|href='javascript:__doPostBack\\(\"lnkCart).*?</a>", "");
		result = result.replaceAll("(?is)<a (?:href|id)=['\\\"](?:javascript:__doPostBack\\(\\\")?(?:[A-Z_\\d\\:]+lnkCart\\d+)[^>]+>\\s*(?:<img[^>]+>\\s*)?</a>", "");
		result = result.replaceAll("(?is)<div id='divCart\\d+'[^>]*>Add(?:\\s|&nbsp;)*to(?:\\s|&nbsp;)*Cart(?:\\s|&nbsp;)*</div>(?:\\s*</a>)?", "");
		
		result = result.replaceAll("(?is)<input type=\"checkbox\" id=\"chkPayOnline.*?</label>", "");
		result = result.replaceAll("(?is)<input[A-Z\\\"=\\d\\s]*\\s*id=\\\"chkPayOnline\\d+[^>]*>(?:\\s*<label[^>]*>Pay(?:\\s|&nbsp;)+online\\s*</label>)?", "");
		result = result.replaceAll("(?is)(?:<input |<label)[A-Z'\\\"=\\d\\s]+chkPayOnline[^>]*>(?:Pay(?:\\s|&nbsp;)+online)?(?:\\s*</label>)?", "");

		// <span title='Pay online'><input type='checkbox' name='chkPayOnline13'></span>
		result = result.replaceAll("<span title='Pay online'><input type='checkbox' name='chkPayOnline[^>]*></span>", "");

		// clean <td>ýý</td>
		result = result.replaceAll("<td>..</td>", "<td> </td>");

		// clean from details links to Tax bill, Certificate etc
		result = result.replaceAll("(?is)<a title=\"(Regular tax bill|Installment payments|Tax certificate).*?</a>", "");
		result = result.replaceAll("(?is)<a id=\"lnkAcctBill2\".*?</a>", "");
		result = result.replaceAll("(?im)<a href='PropertyDetails.aspx\\?Acctno=.*?(Prior Payments Due) - View Detail</a>", "$1");

		// clean from links New Search
		result = result.replaceAll("(?is)<a id=\"lnkSearch.*?</a>", "");
		result = result.replaceAll("(?is)<a id=\"lnkHelp.*?</a>", "");
		result = result.replaceAll("(?is)<a [\\s=A-Z\\\"_\\d]*href\\s*=\\\"javascript:openHelp[^>]+>\\s*Help\\s*</a>", "");
		result = result.replaceAll("(?is)<a id=\"lnkSummary.*?</a>", "");

		return result;
	}

	protected String getDetails(String serverResult, StringBuilder accountId, StringBuilder year) {
		try {
			HtmlParser3 parser = new HtmlParser3(serverResult);
			NodeList root = parser.getNodeList();
			NodeList tables = HtmlParser3.getTag(root, new TableTag(), true);

			// get tbl3
			TableTag tbl3 = getReceipt(tables.toHtml(), "tbl3");

			// get account & year
			if (tbl3 != null) {
				NodeList y = tbl3.getChildren().extractAllNodesThatMatch(new TagNameFilter("span"),	true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_lblDetTaxYear"), false);
				String currentyear = "";
				if (y.size() > 0) {
					currentyear = y.elementAt(0).toPlainTextString().replace("&nbsp;", "").trim();
				}
				String account = "";
				y = tbl3.getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_lblDetTaxParcel"), false);
				if (y.size() > 0) {
					account = y.elementAt(0).toPlainTextString().replace("&nbsp;", " ").trim();
				}
				accountId.append(account);
				year.append(currentyear);
			}

			// if from memory
			if (!serverResult.contains("<html")) {
				return serverResult;
			}

			// get all the year links, sort by year
//			TableTag taxHistoryTable = (TableTag) tables.elementAt(1);
			Vector<TableTag> receipts = new Vector<TableTag>();
			NodeList tmpList = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_tblSummary"), true);
			if (tmpList != null) {
				TableTag taxHistoryTable = (TableTag) tmpList.elementAt(0);
				TableRow[] taxYearRows = taxHistoryTable.getRows();
				List<String> linkList = new LinkedList<String>();

				for (int i = 1; i < taxYearRows.length; i++) {
					NodeList children = taxYearRows[i].getChildren();
					Node firstChild = children.elementAt(1).getFirstChild();
					if (firstChild instanceof LinkTag) {
						LinkTag link = (LinkTag) firstChild;
						String uri = link.getLink();
						linkList.add(uri);
					}
				}
				
				String resp = "";

				// get Bills table for each link
				for (String string : linkList) {
					resp = getLinkContents("http://www.mariontax.com/itm/" + string.replace("&amp;", "&"));
					TableTag tbl5 = getReceipt(resp, "tbl5");
					TableTag get_year = getReceipt(resp, "tbl3");

					TableRow get_y = null;
					if (get_year != null) {
						//
						get_y = get_year.getRow(1);
						TableColumn col_y = get_y.getColumns()[1];
						col_y.setChildren(new NodeList(col_y.getChild(0)));
						col_y.setAttribute("align", "center");
						get_y.setChildren(new NodeList(col_y));
					}
					if (get_y != null) {
						NodeList nodes = tbl5.getChildren();
						NodeList new_nodes = new NodeList(get_y);
						new_nodes.add(nodes);
						tbl5.setChildren(new_nodes);
					}
					if (tbl5 != null)
						receipts.add(tbl5);
				}
			}
			
			// get current year receipt
			/*
			 * TableTag tbl5 = getReceipt(tables.toHtml(), "tbl5"); if (tbl5 !=
			 * null) receipts.add(tbl5);
			 */

			// get tbl1
			TableTag tbl1 = getReceipt(tables.toHtml(), "tbl1");
			// get summary
			TableTag tblSummary = getReceipt(tables.toHtml(), "_ctl0_ContentPlaceHolder1_tblSummary");
			// get tbl4
			TableTag tbl4 = getReceipt(tables.toHtml(), "tbl4");

			// make result
			String response = "";

			if (tbl1 != null)
				response += tbl1.toHtml() + "\n";
			if (tblSummary != null) {
				response += tblSummary.toHtml().replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1")
						.replaceAll("<b>Balance Due</b></td><td>..</td>", "<b>Balance Due</b></td><td> </td>") + "\n";
			}
			if (tbl3 != null) {
				response += tbl3.toHtml().replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1") + "\n";
			}
			if (tbl4 != null) {
				response += tbl4.toHtml() + "\n";
			}
			// attach receipts
			for (TableTag t : receipts) {
				response += t.toHtml() + "\n";
			}

			StringBuilder details = new StringBuilder();

			details.append("<table align=\"center\" border=\"1\"><tr><td align=\"center\">");
			details.append("<style>.colorfixedtext{ font: 9pt courier new; color: black; background-color: #d5d5d5; }  .fixedtext { font: 9pt courier new; color: black; background-color: #ffffff; }</style>");
			details.append(response);
			details.append("</td></tr><tr><td align=\"center\">").append("</td></tr>");
			details.append("</table>");
			return details.toString();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static TableTag getReceipt(String resp, String id) {
		if (resp == null || resp.equals(""))
			return null;

		HtmlParser3 parser = new HtmlParser3(resp);
		NodeList root = parser.getNodeList();
		NodeList tables = HtmlParser3.getTag(root, new TableTag(), true);

		NodeList table = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", id), false);
		if (table != null && table.size() == 0)
			return null;
		else {
			TableTag t = (TableTag) table.elementAt(0);
			// t.removeAttribute("id");
			return t;
		}
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			// parse and store params for search
			String serverResult = response.getResult();

			String viewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", serverResult);
			String eventValidation = StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"", serverResult);
			Map<String, String> params = new HashMap<String, String>();
			params.put("__VIEWSTATE", viewState);
			params.put("__EVENTVALIDATION", eventValidation);

			HtmlParser3 parser = new HtmlParser3(response.getResult());

			NodeList tableTags = HtmlParser3.getTag(parser.getNodeList(),new TableTag(), true);	// get the result table
			TableTag responseTable = (TableTag) tableTags.extractAllNodesThatMatch(new HasAttributeFilter("class", "TableStyle"), true).elementAt(0);

			if (responseTable == null)
				return intermediaryResponse;

			TableRow[] rows = responseTable.getRows();

			response.getParsedResponse().setHeader(" <table cellspacing=\"2\" cellpadding=\"2\" width=\"95%\" align=\"center\" border=\"1\"> ");
			int typeOfParse = 0;
			if (serverResult.contains("Physical Property Address")) {
				typeOfParse = 2;
			}
			for (int i = 2; i < rows.length; i = i + 4) {
				intermediaryResponse.add(parseRow(rows, i, typeOfParse));
			}

			NodeList tag = HtmlParser3.getTag(parser.getNodeList(), new FormTag(), true);
			FormTag formTag = (FormTag) tag.elementAt(0);

			int size = tableTags.size();
			TableTag linkTable = (TableTag) tableTags.elementAt(size - 2);
			outputTable.append(responseTable.toHtml());

			String processLinks = "";

			String nextLink = "", prevLink = "";

			NodeList linkTags = HtmlParser3.getTag(linkTable.getChildren(),	new LinkTag(), true);

			LinkTag linkTagNext = (LinkTag) linkTags.elementAt(0);
			LinkTag linkTagPrevious = (LinkTag) linkTags.elementAt(1);
			String nextEventTarget = "lnkNext";
			String prevEventTarget = "lnkPrevious";
			boolean isPrevLinkDisabled = "disabled".equals(linkTagPrevious.getAttribute("disabled"));
			boolean isNextLinkDisabled = "disabled".equals(linkTagNext.getAttribute("disabled"));

			// is there are less than 6 results per page no need for next & previous
			if (rows.length < 25) {
				isPrevLinkDisabled = true;
				isNextLinkDisabled = true;
			}

			String parameters = "";
			parameters = formTag.getFormLocation();
			parameters = parameters.substring(parameters.indexOf("?") + 1).replace("&amp;", "&");
			nextLink = CreatePartialLink(TSConnectionURL.idPOST) + "/itm/PropertySummary.aspx?" + parameters
					+ "&__EVENTTARGET=" + nextEventTarget + "&seq=" + seq;
			prevLink = CreatePartialLink(TSConnectionURL.idPOST) + "/itm/PropertySummary.aspx?" + parameters
					+ "&__EVENTTARGET=" + prevEventTarget + "&seq=" + seq;

			linkTagNext.setLink(nextLink);
			linkTagPrevious.setLink(prevLink);
			if (isNextLinkDisabled) {
				linkTagNext.setAttribute("disabled", "disabled");
				linkTagNext.removeAttribute("href");
			}
			if (isPrevLinkDisabled) {
				linkTagPrevious.setAttribute("disabled", "disabled");
				linkTagPrevious.removeAttribute("href");
			}

			processLinks = linkTable.toHtml();
			processLinks = processLinks.replaceAll("(?is)<div id=['\\\"]divCart\\d+['\\\"][^>]*>\\s*Add(?:\\s|&nbsp;)*to(?:\\s|&nbsp;)*Cart(?:\\s|&nbsp;)*</div>(?:\\s*</a>)?", "");
			processLinks = processLinks.replaceAll("(?is)<a (?:href|id)=['\\\"](?:javascript:__doPostBack\\(\\\")?(?:[A-Z_\\d\\:]+lnkCart\\d+)[^>]+>\\s*(?:<img[^>]+>\\s*)?</a>", "");

			int seq = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);

			outputTable.append(processLinks);
			response.getParsedResponse().setFooter("</table>" + processLinks);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error while parsing intermediary data", e);
		}
		return intermediaryResponse;
	}

	private ParsedResponse parseRow(TableRow[] rows, int i, int typeOfParse) {
		TableRow row =null;

		Node ownerName = null;
		Node accountNumber = null;
		ParsedResponse currentResponse = new ParsedResponse();
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		for (int idx=i; idx < i+4; idx++) {
			row = rows[idx];
			TableColumn col = null;
			Object defaultValue = new TextNode("");
			NodeList tmpList = null;
			
			/*if (idx == i) { //tax year info
				if (row.getColumnCount() == 1) {
					tmpList = row.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_lblTaxYear"), true);
					if (tmpList != null) {
						if (tmpList.elementAt(0).toPlainTextString().trim().matches("\\d{4}")) {
							resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), tmpList.elementAt(0).toPlainTextString().trim());
						}
					}
				}
				
			} else*/ if (idx == i) { //parcelId and Owner info
				if (row.getColumnCount() == 4) {
					col = row.getColumns()[1];
					tmpList = col.getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
					if (tmpList != null) {
						ownerName = tmpList.elementAt(0);
					}
					
					col = row.getColumns()[3];
					tmpList = col.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
					if (tmpList != null) {
						accountNumber = tmpList.elementAt(0);
						accountNumber = (Node) ObjectUtils.defaultIfNull(accountNumber, defaultValue);
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNumber.toPlainTextString().trim());
						
						// rewrite the link
						LinkTag linkTag = null;
						
						if (accountNumber instanceof LinkTag) {
							linkTag = (LinkTag) accountNumber;
							String atsLink = CreatePartialLink(TSConnectionURL.idGET) + "/itm/"
									+ linkTag.getLink();
							atsLink = atsLink.replaceAll("&amp;", "&");
							linkTag.setLink(atsLink);
							currentResponse.setPageLink(new LinkInPage(atsLink, atsLink, TSServer.REQUEST_SAVE_TO_TSD));
						}
					}
				}
				
			} else if (idx == i+1) { //address info
				if (row.getColumnCount() == 4) {
					col = row.getColumns()[1];
					String tmpOwnerName = "";
					String address = "";
					
					tmpList = col.getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
					if (tmpList != null) {
						Node physicalAddress = tmpList.elementAt(0);
						physicalAddress = (Node) ObjectUtils.defaultIfNull(physicalAddress, defaultValue);
						
						address = physicalAddress.toPlainTextString().trim();
						address = address.replaceFirst("(?is)1\\s+\\bVILL\\b", "VILLAGES OF MARION");
						tmpOwnerName = ownerName.toPlainTextString();
						if (typeOfParse == ID_SEARCH_BY_ADDRESS) {
							String temp = tmpOwnerName;
							tmpOwnerName = address;
							address = temp;
						}
						
						address = address.replace("&nbsp;", " ");
						
						for (String key : getCities().keySet()) {
							String city = key;
							String shortCityName = city.replaceFirst("(?is)(?:CITY|VILLAGES) OF ([A-Z\\s]+)", "$1");
							if (address.contains(city) || address.toUpperCase().contains(city) ||
								address.contains(shortCityName) || address.toUpperCase().contains(shortCityName)) {
									resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.toUpperCase());
									address = address.replaceAll("(?is)(?:(?:CITY|VILLAGES) OF\\s+|1\\s+)?" + shortCityName + "\\s*", "");
									address = address.replaceAll("(?is)\\s*" + city + "\\s*", "");
									break;
							}
						}
						
						resultMap.put("tmpOwnerName", tmpOwnerName);
						resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
					}
					
					// parse name
					ro.cst.tsearch.servers.functions.FLMarionTR.parseName(resultMap, tmpOwnerName, accountNumber.toPlainTextString().trim().contains("P"));
					
					// parse address
					ro.cst.tsearch.servers.functions.FLMarionTR.parseAddress(resultMap, address);
				}
				
			} else if (idx == i+2) { //legal info
				if (row.getColumnCount() == 2) {
					col = row.getColumns()[1];
					
					Node legalDescription = col;
					legalDescription = (Node) ObjectUtils.defaultIfNull(legalDescription, defaultValue);
					String tmpLegalDescription = legalDescription.toPlainTextString().replaceAll("&nbsp;", "").trim();
					resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), tmpLegalDescription);

					// parse legal
					ro.cst.tsearch.servers.functions.FLMarionTR.parseLegalDescription(resultMap, tmpLegalDescription);
				}
			}
		}
		
		
		String rowHtml = "<tr> <td> " + rows[i].toHtml() + rows[i+1].toHtml() + rows[i+2].toHtml() + rows[i+3].toHtml() + "</td> </tr>";
		rowHtml = cleanServerResult(rowHtml);
		currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
		currentResponse.setOnlyResponse(rowHtml);

		resultMap.removeTempDef();
		Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
		DocumentI document = null;
		try {
			document = bridge.importData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		currentResponse.setDocument(document);
		return currentResponse;
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		String rsResponse = response.getResult();
		if (!StringUtils.isNotEmpty(rsResponse)) {
			return null;
		}

		try {
			HtmlParser3 parser = new HtmlParser3(rsResponse);
			NodeList root = parser.getNodeList();
			NodeList tables = root.extractAllNodesThatMatch(new TagNameFilter("table"), false);
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			// get account address subdivision
			TableTag tbl1 = getReceipt(tables.toHtml(), "tbl1");
//			ro.cst.tsearch.servers.functions.FLMarionTR.parseDetailsGeneralData(tbl1, resultMap, cities);
			ro.cst.tsearch.servers.functions.FLMarionTR.parseDetailsGeneralData(tbl1, resultMap, getCities());

			// parse legal
			String tmpLegalDescription = getLegalDescription(parser);
			resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), tmpLegalDescription);
			ro.cst.tsearch.servers.functions.FLMarionTR.parseLegalDescription(resultMap, tmpLegalDescription);

			// parse names
			List<String> ownerInformation = getOwnerInformation(parser);

			// isPP = isPersonalProperty
			boolean isPP = ((String) resultMap.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName())).contains("P");

			String tmpNames = org.apache.commons.lang.StringUtils.join(ownerInformation.toArray(), " AND ");
			ro.cst.tsearch.servers.functions.FLMarionTR.parseName(resultMap, tmpNames, isPP);;

			// tax history set
			ro.cst.tsearch.servers.functions.FLMarionTR.parseTaxHistorySet(parser.getNodeList(), resultMap);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private String getLegalDescription(HtmlParser3 parser) {
		TableTag legaldescrTable = (TableTag) HtmlParser3.getNodeByID(
				"Table2a", parser.getNodeList(), true);
		if (legaldescrTable != null) {
			TableRow[] rows = legaldescrTable.getRows();
			StringBuilder buildLegal = new StringBuilder();
			for (TableRow tableRow : rows) {
				Node firstChild = tableRow.getChild(1);
				buildLegal.append(firstChild.toPlainTextString().replaceAll(
						"&nbsp;", " "));
			}
			String legal = buildLegal.toString().replaceAll("\\s+", " ")
					.replaceAll("(\\d) (\\.*\\d)", "$1$2").trim();

			legal = legal.replaceAll("L\\s*O\\s*T", "LOT");
			legal = legal.replaceAll("L\\s*O\\s*T\\s*S", "LOTS");
			legal = legal.replaceAll("P\\s*L\\s*A\\s*T B\\s*O\\s*O\\s*K",
					"PLAT BOOK");
			legal = legal.replaceAll("P\\s*A\\s*G\\s*E", "PAGE");
			legal = legal.replaceAll("B\\s*L\\s*K", "BLK");
			legal = legal.replaceAll("U\\s*N\\s*I\\s*T", "UNIT");
			legal = legal.replaceAll("P\\s*H\\s*A\\s*S\\s*E", "PHASE");

			return legal;
		}
		return "";
	}

	private List<String> getOwnerInformation(HtmlParser3 parser) {
		TableTag ownerInfoTable = (TableTag) HtmlParser3.getNodeByID("Table2b",
				parser.getNodeList(), true);

		TableRow[] rows = ownerInfoTable.getRows();
		List<String> linkedList = new LinkedList<String>();
		int i = 0;
		while (i < rows.length - 2) {
			Node firstChild = rows[i].getChild(1).getFirstChild();
			String plainTextString = firstChild.toPlainTextString();
			plainTextString = plainTextString.replaceAll("&nbsp;", " ").trim();
			if (!plainTextString.contains(" BLD "))
				linkedList.add(plainTextString);
			i++;
		}
		return linkedList;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		// TR pin Rdddd-ddd-ddd (most cases)
		if (hasPin()) {
			String parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			//real estate
			if (parcelno.length() == 13) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();

				module.forceValue(0, parcelno);
				moduleList.add(module);
			}
			
			// or personal property
			if (parcelno.contains("P") && parcelno.length() == 7 || parcelno.length() == 9) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.forceValue(0, parcelno);
				moduleList.add(module);
			}
			
			// NB pin : 1579500706
			// R15795-007-06
			// R9010-0012-10
			// R2260-172-024

			parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			// R15795-007-06 = 5 3 2
			if (parcelno.length() == 10) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();

				parcelno = "R" + parcelno.substring(0, 5) + "-"
						+ parcelno.substring(5, 8) + "-"
						+ parcelno.substring(8);

				module.forceValue(0, parcelno);
				moduleList.add(module);
			}

			parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			// R9010-0012-10 = 4 4 2
			if (parcelno.length() == 10) {
				module = new TSServerInfoModule(
						serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();

				parcelno = "R" + parcelno.substring(0, 4) + "-"
						+ parcelno.substring(4, 8) + "-"
						+ parcelno.substring(8);

				module.forceValue(0, parcelno);
				moduleList.add(module);
			}

			// R2260-172-024 = 4 3 3
			parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO);

			if (parcelno.length() == 10) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();

				parcelno = "R" + parcelno.substring(0, 4) + "-"
						+ parcelno.substring(4, 7) + "-"
						+ parcelno.substring(7);

				module.forceValue(0, parcelno);
				moduleList.add(module);
			}

			// R11679-006012 = 5 6
			parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO);

			if (parcelno.length() == 10) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();

				parcelno = "R" + parcelno.substring(0, 5) + "-"
						+ parcelno.substring(5);

				module.forceValue(0, parcelno);
				moduleList.add(module);
			}

			// R18161102-033 = 8 3
			parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO);

			if (parcelno.length() == 10) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();

				parcelno = "R" + parcelno.substring(0, 8) + "-" + parcelno.substring(8);

				module.forceValue(0, parcelno);
				moduleList.add(module);
			}

			// P => 6 or 8
			parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			if (parcelno.length() == 6 || parcelno.length() == 8) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();

				parcelno = "P" + parcelno.replaceAll("-", "");

				module.forceValue(0, parcelno);
				moduleList.add(module);
			}

		}

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
		FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
		
		if (hasStreet() && hasStreetNo()) {
			boolean putCity = false;
			String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
			String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
			String cityName = getSearch().getSa().getAtribute(SearchAttributes.P_CITY);
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.addFilterForNextType(FilterResponse.TYPE_ADDRESS_FOR_NEXT);
			module.addFilter(addressFilter);
			module.getFunction(1).forceValue(streetNo);
			module.getFunction(2).forceValue(streetName);
			module.getFunction(3).forceValue(" ");
			
			if (StringUtils.isNotEmpty(cityName)) {
				for (String key : getCities().keySet()) {
					if (cityName.toUpperCase().equals(key) || key.contains(cityName.toUpperCase())) {
						module.getFunction(6).forceValue(getCities().get(key));
						putCity = true;
						break;
					} 
				}
				if (!putCity) {
					module.getFunction(6).forceValue(" ");
				}

			} else {
				module.getFunction(6).forceValue(" ");
			}
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			moduleList.add(module);
		}
		
		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }));
			moduleList.add(module);
		}
		
		

		serverInfo.setModulesForAutoSearch(moduleList);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd)
			throws ServerResponseException {

		// add punctuation to pin if necessary
		if (module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX) {
			String pin = module.getFunction(0).getParamValue();
			if (!pin.contains("-")) {
				if (pin.length() == 10) {
					pin = "R" + pin.substring(0, 4) + "-" + pin.substring(4, 7)
							+ "-" + pin.substring(7);
				}
			}

			module.getFunction(0).setParamValue(pin);

		}
		return super.SearchBy(module, sd);
	}
}

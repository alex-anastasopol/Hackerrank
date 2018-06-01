package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class OHFairfieldTR extends TSServer {

	private static final long serialVersionUID = 2147331228089176346L;
	
	private static int seq = 0;
	
	//details pages to be added (all but Sketch page); Valuation Data appears in Base Data
	public static final String[] PAGES = {"1", /*"2",*/ "3", "5", "6", "7", "8", "9", "10"};	
	public static final String[] PAGES_TITLES = {"Land Data", /*"Valuation Data",*/ "Sales Data", "Tax Data", "Improvements Data", 
		                                         "Permit Data", "Residential Data", "Agricultural Data", "Commercial Data"};	
	public static final String[][] PAGES_TABLES_IDS = {
													   {"ctl00_ContentPlaceHolder1_Land_gvDataLand", "ctl00_ContentPlaceHolder1_Land_fvDataLandTotals"},
		                                               /*{"ctl00_ContentPlaceHolder1_Valuation_fvDataValuation"},*/
		                                               {"ctl00_ContentPlaceHolder1_Sales_gvDataSales"},
		                                               {"ctl00_ContentPlaceHolder1_Tax_fvDataTax", "ctl00_ContentPlaceHolder1_Tax_fvDataSpecials", 
		                                            	   "ctl00_ContentPlaceHolder1_Tax_gvDataPayments"},
		                                               {"ctl00_ContentPlaceHolder1_Improvements_gvDataImprovements",  
		                                            		   "ctl00_ContentPlaceHolder1_Improvements_fvDataLandTotals"},
		                                               {"ctl00_ContentPlaceHolder1_Permit_gvDataPermits"},
		                                               {"ctl00_ContentPlaceHolder1_Residential_fvDataResidential"},
		                                               {"ctl00_ContentPlaceHolder1_Agricultural_gvDataAgricultural", 
		                                            	   "ctl00_ContentPlaceHolder1_Agricultural_fvDataLandTotals"},
		                                               {"ctl00_ContentPlaceHolder1_Commercial_fvDataCommercial", 
		                                            		   "ctl00_ContentPlaceHolder1_Commercial_gvDataCommercialFeatures", 
		                                            		   "ctl00_ContentPlaceHolder1_Commercial_gvDataCommercialConstruction"}
		                                              };
	public static final String[][] PAGES_TABLES_LABELS = {
		 												  {"Land", "Land Totals"},
		                                                  /*{"Valuation"},*/
		                                                  {"Sales"},
		                                                  {"Property Tax", "Special Assessments", "Payments"},
		                                                  {"Improvements", "Improvements Totals"},
		                                                  {"Permit"},
		                                                  {"Residential"},
		                                                  {"Agricultural", "Agricultural Totals"}, 
		                                                  {"Commercial", "Commercial Features", "Commercial Construction"}
		                                                 };

	protected synchronized static int getSeq() {
		return seq++;
	}
	
	public OHFairfieldTR(long searchId) {
		super(searchId);
	}

	public OHFairfieldTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_INSTRUMENT_NO: 				//Map Number Search
		
			if (rsResponse.indexOf("No results") > -1) {
				Response.getParsedResponse().setError("No results found.");
				return;
			}

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
					Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(
						smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,
						outputTable.toString());
			}
			
			int seq = getSeq();
			Map<String, String> params = new HashMap<String, String>(); 
			String eventTarget = "";
			Matcher matcher1 = Pattern.compile("(?is)id=\"__EVENTTARGET\" value=\"(.*?)\"").matcher(rsResponse);
			if (matcher1.find()) 
				eventTarget = matcher1.group(1);
			String eventArgument = "";
			Matcher matcher2 = Pattern.compile("(?is)id=\"__EVENTARGUMENT\" value=\"(.*?)\"").matcher(rsResponse);
			if (matcher2.find()) 
				eventArgument = matcher2.group(1);
			Matcher matcher3 = Pattern.compile("(?is)id=\"__VIEWSTATE\" value=\"(.*?)\"").matcher(rsResponse);
			String viewState = "";
			if (matcher3.find()) 
				viewState = matcher3.group(1);
			params.put("__EVENTTARGET", eventTarget);
			params.put("__EVENTARGUMENT", eventArgument);
			params.put("__VIEWSTATE", viewState);
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);

			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber);
			String filename = serialNumber + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&"
						+ Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST)
						+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(serialNumber.toString(), null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink,
							ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			break;

		case ID_GET_LINK:
			if (rsResponse.contains("Results per page"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);
			else
				ParseResponse(sAction, Response, ID_DETAILS);
			break;

		default:
			break;
		}

	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}

	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {

			StringBuilder details = new StringBuilder();

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);

			/* If from memory - use it as is */
			if (!rsResponse.toLowerCase().contains("<html")) {
				NodeList parcelList = nodeList.extractAllNodesThatMatch(
						new TagNameFilter("span"), true).extractAllNodesThatMatch(
						new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataProfile_ParcelLabel"));
				String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
				parcelNumber.append(parcelID);

				return rsResponse;
			}

			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataProfile_ParcelLabel"));
			String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
			parcelNumber.append(parcelID);

			details.append("<h3>Base Data</h3>");
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataProfile"));
			if (tables.size() > 0)
				details.append(tables.elementAt(0).toHtml()).append("<br>");
			
			String table1 = "";
			tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataMailingAddress"));
			if (tables.size() > 0)
				table1 = tables.elementAt(0).toHtml();
			String table2 = "";
			tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataGeographic"));
			if (tables.size() > 0)
				table2 = tables.elementAt(0).toHtml();
			details.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\"><tr>" +
					"<td style=\"width=50%;\"><div><b>Mailing Address</b></div>" + table1 + "</td>" +
					"<td style=\"width=50%;\"><div><b>Taxing District</b></div>" + table2 + "</td></tr><table><br>");
			
			tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataLegal"));
			if (tables.size() > 0)
				details.append("<div><b>Legal</b></div>").append(tables.elementAt(0).toHtml()).append("<br>");
			
			tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataValuation"));
			if (tables.size() > 0)
				details.append("<div><b>Valuation</b></div>").append(tables.elementAt(0).toHtml()).append("<br>");
			
			tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataTaxCredits"));
			if (tables.size() > 0)
				details.append("<div><b>Tax Credits</b></div>").append(tables.elementAt(0).toHtml()).append("<br>");
			
			tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataCustomNotes"));
			if (tables.size() > 0)
				details.append("<div><b>Notes</b></div>").append(tables.elementAt(0).toHtml());
			
			String moreDetails = getMoreDetails(rsResponse, parcelNumber);
			
			details.append(moreDetails);
			
			return details.toString().replaceAll("(?is)<a.*?>([^<]+)</a>", "$1");

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	protected String getMoreDetails(String rsResponse, StringBuilder parcelNumber) {
		
		StringBuilder sb = new StringBuilder();
		String year = "";
		
		try {
			
			Matcher matcher3 = Pattern.compile("(?is)id=\"__VIEWSTATE\" value=\"(.*?)\"").matcher(rsResponse);
			String viewState = "";
			if (matcher3.find()) 
				viewState = matcher3.group(1);
			
			for (int i=0;i<PAGES.length; i++) {
				int seq = getSeq();
				Map<String, String> params = new HashMap<String, String>();
				params.put("__EVENTTARGET", "ctl00$ContentPlaceHolder1$mnuData");
				params.put("__EVENTARGUMENT", PAGES[i]);
				params.put("__VIEWSTATE", viewState);
				params.put("ctl00$ContentPlaceHolder1$smData", "ctl00$ContentPlaceHolder1$upData|ctl00$ContentPlaceHolder1$mnuData");
				params.put("ctl00$tbSearchBox", "Enter Parcel, Owner, or Address");
				params.put("ctl00$tbSavePropertyAs", "");
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
				
				String contents = getLinkContents("http://realestate.co.fairfield.oh.us/Data.aspx?ParcelID=" + 
						parcelNumber.toString() + "?seq=" + seq);
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(contents, null);
				NodeList nodeList = htmlParser.parse(null);
				sb.append("<h3>" + PAGES_TITLES[i] + "</h3>");
				
				if ("5".equals(PAGES[i])) {			//Tax Page
					Matcher matcher = Pattern.compile("(?is)<option selected=\"selected\" value=\"(\\d+)\">").matcher(contents);
					if (matcher.find()) {
						year = matcher.group(1);
						sb.append("<br>").append("<b>Tax Year&nbsp;").append(year).append("</b><br>&nbsp;");
					} 
						
				}
				
				for (int j=0;j<PAGES_TABLES_IDS[i].length;j++) {
					NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", PAGES_TABLES_IDS[i][j]));
					if (tables.size() > 0) {
						String tableContent = tables.elementAt(0).toHtml();
						if (i==0 && j== 0)
							tableContent = tableContent.replaceAll("(?is)<td>", "<td align=\"center\">");
						if ("5".equals(PAGES[i])) 
							tableContent = tableContent.replaceAll("(?is)<table", "<table year=\"" + year + "\"");					
						sb.append("<div><b>" + PAGES_TABLES_LABELS[i][j] +"</b></div>").append(tableContent).append("<br>");
					}
					if ("5".equals(PAGES[i]) && j==1) {		//Tax Page, Special Assessments 
						NodeList select = nodeList.extractAllNodesThatMatch(new TagNameFilter("select"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Tax_fvDataSpecials_ddlDataSpecials"));
						if (select.size()>0)	
							sb.append(getTaxSpecialAssessments(i, j, contents, parcelNumber, year, select.elementAt(0).toHtml()));
					}
				}
				
				if ("5".equals(PAGES[i])) {			//for Tax Page, take all years
					NodeList select = nodeList.extractAllNodesThatMatch(new TagNameFilter("select"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Tax_ddlTaxYear"));
					if (select.size()>0)	
						sb.append(getTaxPages(i, contents, parcelNumber, select.elementAt(0).toHtml()));
				}
			}	
		} catch (ParserException e) {
			logger.error("Error while getting details", e);
		}
		
		return sb.toString().replaceAll("(?is)<select", "<select disabled=\"disabled\">");
	}
	
	protected String getTaxPages(int i, String contents, StringBuilder parcelNumber, 
			String selectContent) throws ParserException {
		
		Matcher matcher3 = Pattern.compile("(?is)id=\"__VIEWSTATE\" value=\"(.*?)\"").matcher(contents);
		String viewState = "";
		if (matcher3.find()) 
			viewState = matcher3.group(1);
		
		StringBuilder sb = new StringBuilder();
		String tableContent = "";
		
		Matcher matcher = Pattern.compile("(?is)<option value=\"(\\d+)\">").matcher(selectContent);
		while (matcher.find()) {
			int seq = getSeq();
			Map<String, String> params = new HashMap<String, String>();
			params.put("__EVENTTARGET", "ctl00$ContentPlaceHolder1$Tax$ddlTaxYear");
			params.put("__EVENTARGUMENT", "");
			params.put("__VIEWSTATE", viewState);
			params.put("__LASTFOCUS", "");
			params.put("ctl00$tbSearchBox", "Enter Parcel, Owner, or Address");
			params.put("ctl00$tbSavePropertyAs", "");
			params.put("ctl00$ContentPlaceHolder1$Tax$ddlTaxYear", matcher.group(1));
			params.put("ctl00$ContentPlaceHolder1$Tax$fvDataSpecials$ddlDataSpecials", "0");
			
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			String result = getLinkContents("http://realestate.co.fairfield.oh.us/Data.aspx?ParcelID=" + 
					parcelNumber.toString() + "?seq=" + seq);
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(result, null);
			NodeList nodeList = htmlParser.parse(null);
			sb.append("<br>").append("<b>Tax Year&nbsp;").append(matcher.group(1)).append("</b><br>&nbsp;");
			for (int j=0;j<PAGES_TABLES_IDS[i].length;j++) {
				if (j==1)									//Special Assessments are the same in all the years
					continue;								//so they are displayed only in the first (most recent) year
				NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", PAGES_TABLES_IDS[i][j]));
				tableContent = "";
				if (tables.size() > 0) {
					tableContent = tables.elementAt(0).toHtml();
					tableContent = tableContent.replaceAll("(?is)<table", "<table year=\"" + matcher.group(1) + "\"");
					sb.append("<div><b>" + PAGES_TABLES_LABELS[i][j] +"</b></div>").append(tableContent).append("<br>");
				}
				/*if (j==1) {								//Special Assessments
					NodeList select = nodeList.extractAllNodesThatMatch(new TagNameFilter("select"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", 
							"ctl00_ContentPlaceHolder1_Tax_fvDataSpecials_ddlDataSpecials"));
					if (select.size()>0)	
						sb.append(getTaxSpecialAssessments(i, j, result, parcelNumber, matcher.group(1), select.elementAt(0).toHtml()));
				}*/
			}
		}
		return sb.toString();
	}
	
	protected String getTaxSpecialAssessments(int i, int j, String contents, StringBuilder parcelNumber, 
			String year, String selectContent) throws ParserException {
		
		Matcher matcher3 = Pattern.compile("(?is)id=\"__VIEWSTATE\" value=\"(.*?)\"").matcher(contents);
		String viewState = "";
		if (matcher3.find()) 
			viewState = matcher3.group(1);
		
		StringBuilder sb = new StringBuilder();
		String tableContent = "";
		
		Matcher matcher = Pattern.compile("(?is)<option value=\"(\\d+)\">").matcher(selectContent);
		while (matcher.find()) {
			int seq = getSeq();
			Map<String, String> params = new HashMap<String, String>();
			params.put("__EVENTTARGET", "ctl00$ContentPlaceHolder1$Tax$fvDataSpecials$ddlDataSpecials");
			params.put("__EVENTARGUMENT", "");
			params.put("__VIEWSTATE", viewState);
			params.put("__LASTFOCUS", "");
			params.put("ctl00$tbSearchBox", "Enter Parcel, Owner, or Address");
			params.put("ctl00$tbSavePropertyAs", "");
			params.put("ctl00$ContentPlaceHolder1$Tax$ddlTaxYear", year);
			params.put("ctl00$ContentPlaceHolder1$Tax$fvDataSpecials$ddlDataSpecials", matcher.group(1));
			params.put("ctl00$ContentPlaceHolder1$smData", 
					"ctl00$ContentPlaceHolder1$upData|ctl00$ContentPlaceHolder1$Tax$fvDataSpecials$ddlDataSpecials");
			
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			String result = getLinkContents("http://realestate.co.fairfield.oh.us/Data.aspx?ParcelID=" + 
					parcelNumber.toString() + "?seq=" + seq);
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(result, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", PAGES_TABLES_IDS[i][j]));
			tableContent = "";
			if (tables.size() > 0) {
				tableContent = tables.elementAt(0).toHtml();
				sb.append(tableContent).append("<br>");
			}
		}
		return sb.toString();
	}
	
	protected String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		TableTag resultsTable = null;
		String header = "";
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_gvSearchResults"), true);

			if (mainTable.size() != 0)
				resultsTable = (TableTag) mainTable.elementAt(0);

			// if there are results
			if (resultsTable != null && resultsTable.getRowCount() != 0) {
				
				TableRow[] rows = resultsTable.getRows();

				// row 0 is the header
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];

					String htmlRow = row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();

					String link = "";
					if (row.getColumnCount()==5) {														//table row
						String parcelID = row.getColumns()[0].toPlainTextString().trim();
						link = "http://realestate.co.fairfield.oh.us/Data.aspx?ParcelID=" + parcelID;
						link = CreatePartialLink(TSConnectionURL.idGET) + link;
						htmlRow = htmlRow.replaceAll("(?is)href=\"[^\"]+\"", "href=" + link);
					} else {																			//previous/next row
						link = "http://realestate.co.fairfield.oh.us/Results.aspx?";
						
						Matcher matcher1 = Pattern.compile("(?is)action=\"Results.aspx\\?([^\"]+)\"").matcher(table);
						if (matcher1.find()) {
							String param = matcher1.group(1).replaceAll("&amp;", "&"); 
							link += param + "&";
						} 
						
						String column = row.getColumns()[0].toHtml();
						Matcher matcher2 = Pattern.compile("(?is)id=\"__VIEWSTATE\" value=\"(.*?)\"").matcher(table);
						String viewState = "";
						if (matcher2.find()) 
							viewState = matcher2.group(1);
						Matcher matcher3 = Pattern.compile("(?is)<a href=\"javascript:__doPostBack\\('([^']+)','([^']+)'\\)\">").matcher(column);
						String eventTarget = "";
						String eventArgument = "";
						while (matcher3.find()) {
							eventTarget = matcher3.group(1);
							eventArgument = matcher3.group(2);
							Map<String, String> params = new HashMap<String, String>(); 
							int seq = getSeq();
							params.put("__VIEWSTATE", viewState);
							params.put("__EVENTTARGET", eventTarget);
							params.put("__EVENTARGUMENT", eventArgument);
							mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
							htmlRow = htmlRow.replace(matcher3.group(0), 
									"<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + link + "seq=" + seq + "\">");
						} 
						
					}
										
					currentResponse.setPageLink(new LinkInPage(link, link,	TSServer.REQUEST_SAVE_TO_TSD));

					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
					currentResponse.setOnlyResponse(htmlRow);

					ResultMap m = ro.cst.tsearch.servers.functions.OHFairfieldTR.parseIntermediaryRow(row, searchId);
					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);

				}

				header = "<table cellspacing=\"0\" border=\"border=0\"><tr>"
						+ "<th align=\"left\">Parcel</th>"
						+ "<th align=\"left\">Owner</th>"
						+ "<th align=\"left\">Property Address</th>"
						+ "<th align=\"left\">Land Use</th>"
						+ "<th align=\"left\">Acres</th>"
						+ "</tr>";
				
				Matcher matcher = Pattern.compile("(?is)<select.*?>.*?</select>").matcher(table);
				if (matcher.find()) {
					header = "Results per page:&nbsp;" + 
						matcher.group(0).replaceFirst("(?is)<select", "<select disabled=\"disabled\"")
						+ "<br><br>" + header;
				}
					
				
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter("</table>");

				outputTable.append(table);
			}

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
	
		try {
						
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
				
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
							
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataProfile_ParcelLabel"));
			if (parcelList.size()>0) {
				String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			}
						
			NodeList ownerList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataProfile_OwnerLabel"));
			if (ownerList.size()>0) {
				String owner = ownerList.elementAt(0).toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
			}
			
			NodeList addressList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataProfile_AddressLabel"));
			if (addressList.size()>0) {
				String address = addressList.elementAt(0).toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			}
			
			String legal = "";
			NodeList legalList1 = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", 
						"ctl00_ContentPlaceHolder1_Base_fvDataLegal_LegalDescriptionLine1Label"));
			if (legalList1.size()>0) {
				legal += legalList1.elementAt(0).toPlainTextString().trim();
				
			}
			NodeList legalList2 = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", 
					"ctl00_ContentPlaceHolder1_Base_fvDataLegal_LegalDescriptionLine2Label"));
			if (legalList2.size()>0) {
				legal += " " + legalList2.elementAt(0).toPlainTextString().trim();
			
			}
			if (legal.length()>0)
				resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
			
			NodeList rngTwpSecList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", 
					"ctl00_ContentPlaceHolder1_Base_fvDataLegal_RangeTownshipSectionLabel"));
			if (rngTwpSecList.size()>0) {
				String rts = rngTwpSecList.elementAt(0).toPlainTextString().trim();
				Matcher matcher = Pattern.compile("([^-]+)-([^-]+)-([^-]+)").matcher(rts);
				if (matcher.find()) {
					String r = matcher.group(1).trim();
					String t = matcher.group(2).trim();
					String s = matcher.group(3).trim();
					if (!"0".equals(r))
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), r);
					if (!"0".equals(t))
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), t);
					if (!"0".equals(s))
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), s);
				}
			}
			
			CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
			int commId = currentInstance.getCommunityId();
			Date payDate = HashCountyToIndex.getPayDate(commId, miServerID);
			String year = payDate.toString().replaceAll(".*?\\s+(\\d{4})", "$1").trim();
			int taxYearPD = Integer.parseInt(year) - 1;
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), Integer.toString(taxYearPD));
			
			ResultTable payments = new ResultTable();			//tax history table
			List<List> tablebody = new ArrayList<List>();
			List<String> list;
			
			//get tables
			NodeList taxesList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", 
						"ctl00_ContentPlaceHolder1_Tax_gvDataPayments"));
			if (taxesList != null)
			{
				String date = "";
				String amount = "";
				
				for (int i=0;i<taxesList.size();i++)
				{
					TableTag table = (TableTag)taxesList.elementAt(i);
					for (int j=1;j<table.getRowCount();j++) {
						list = new ArrayList<String>();
						date = table.getRow(j).getColumns()[0].toPlainTextString().trim();
						amount = table.getRow(j).getColumns()[1].toPlainTextString()
							.replaceAll("[\\$\\(\\),]", "").trim();
						list = new ArrayList<String>();
						list.add(date);
						list.add(amount);
						tablebody.add(list);
					}
				}	
			}
											
			String[] header = {TaxHistorySetKey.RECEIPT_DATE.getShortKeyName(), TaxHistorySetKey.RECEIPT_AMOUNT.getShortKeyName()};
			payments = GenericFunctions2.createResultTable(tablebody, header);
			if (payments != null){
				resultMap.put("TaxHistorySet", payments);
			}
			
			ResultTable sales = new ResultTable();				//sales table
			List<List> tablebodysales = new ArrayList<List>();
			List<String> listsales;
			
			NodeList salesList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Sales_gvDataSales"));
			if (salesList.size() >0 )
			{
				TableTag salesTable = (TableTag) salesList.elementAt(0);
				int rowsNumber = salesTable.getRowCount();
				String date = "";
				String price = "";
				String grantor = "";			//seller
				String grantee = "";			//buyer
				String bookPage = "";
				String[] bookAndPage = {"", ""};
				
				for (int i=1; i<rowsNumber; i++)			//row 0 is the header
				{
					TableRow row = salesTable.getRow(i);
					date = row.getColumns()[0].toPlainTextString().trim();
					price = row.getColumns()[1].toPlainTextString().replaceAll("[\\$,]", "") .trim();
					grantor = row.getColumns()[2].toPlainTextString().trim();
					grantee = row.getColumns()[3].toPlainTextString().trim();
					bookPage = row.getColumns()[4].toPlainTextString().trim();
					listsales = new ArrayList<String>();
					listsales.add(date);
					listsales.add(price);
					listsales.add(grantor);
					listsales.add(grantee);
					bookAndPage = bookPage.split("/");
					if (bookAndPage.length==2) {
						listsales.add(bookAndPage[0]);
						listsales.add(bookAndPage[1]);
					} else {
						listsales.add("");
						listsales.add("");
					}
					tablebodysales.add(listsales);
				}
				
				String[] headersales = {SaleDataSetKey.RECORDED_DATE.getShortKeyName(), SaleDataSetKey.SALES_PRICE.getShortKeyName(),
						SaleDataSetKey.GRANTOR.getShortKeyName(), SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), 
						SaleDataSetKey.BOOK.getShortKeyName(), SaleDataSetKey.PAGE.getShortKeyName()};
				sales = GenericFunctions2.createResultTable(tablebodysales, headersales);
				if (sales != null){
					resultMap.put("SaleDataSet", sales);
				}
			}
						
			ro.cst.tsearch.servers.functions.OHFairfieldTR.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.OHFairfieldTR.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.OHFairfieldTR.parseLegalSummary(resultMap);
			ro.cst.tsearch.servers.functions.OHFairfieldTR.parseTaxes(nodeList, resultMap, searchId);
			ro.cst.tsearch.servers.functions.OHFairfieldTR.parseTaxInstallments(nodeList, resultMap);
			ro.cst.tsearch.servers.functions.OHFairfieldTR.parsePropertyAppraisalSet(nodeList, resultMap);
		
		} catch (ParserException e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		if (hasPin()) {
			FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.LD_PARCELNO_GENERIC_TR);
			module.addFilter(pinFilter);
			moduleList.add(module);
		}

		boolean hasOwner = hasOwner();
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);

		if (hasStreet()) {
			FilterResponse nameFilter = NameFilterFactory.getHybridNameFilter(
					SearchAttributes.OWNER_OBJECT, searchId, module);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.P_STREETNO);
			module.setSaKey(3, SearchAttributes.P_STREETNAME);
			module.addFilter(addressFilter);
			module.addFilter(nameFilter);
			moduleList.add(module);
		}

		if (hasOwner) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,	searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L;F;", "L;M;" }));
			moduleList.add(module);
		}

		serverInfo.setModulesForAutoSearch(moduleList);
	}
}

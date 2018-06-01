package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableHeader;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.token.AddressAbrev;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.functions.TNGenericTR;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class TNGenericCountyTR extends TSServerAssessorandTaxLike {

	static final long serialVersionUID = 10000000;

	private boolean downloadingForSave = false;
	private String specificCnty = "Franklin";

	public void setServerID(int ServerID) {
		super.setServerID(ServerID);
		setSpecificCounty(getDataSite().getCountyName().replaceAll(" ", ""));
	}

	public TNGenericCountyTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	protected void setSpecificCounty(String cntyName) {
		specificCnty = cntyName;
	}

	protected String getSpecificCntySrvName() {
		if (specificCnty.equalsIgnoreCase("Trousdale"))
			return "Wilson";
		else if (specificCnty.equalsIgnoreCase("Moore"))
			return "Coffee";
		else if (specificCnty.equalsIgnoreCase("Hamblen"))
			return "Greene";

		return specificCnty;
	}

	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);

		setSpecificCounty(getDataSite().getCountyName().replaceAll("\\s+", ""));

		msiServerInfoDefault.setServerAddress("www." + getSpecificCntySrvName().toLowerCase() + ".tennesseetrustee.org/");
		msiServerInfoDefault.setServerIP("www." + getSpecificCntySrvName().toLowerCase() + ".tennesseetrustee.org/");
		msiServerInfoDefault.setServerLink("https://www." + getSpecificCntySrvName().toLowerCase() + ".tennesseetrustee.org");

		String controlMap = "";
    	String group = "";
    	String parcel = "";
    	String si = "";
		if (hasPin()) {
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
        	Matcher ma1 = Pattern.compile("([^-]+)-([^-]+)").matcher(pin);
        	Matcher ma2 = Pattern.compile("([^-]+)-([^-]+)-([^-]+)").matcher(pin);
        	Matcher ma3 = Pattern.compile("(\\d{3}[A-Z]?)([A-Z]?)(\\d{3}\\.\\d{2})").matcher(pin);
        	Matcher ma4 = Pattern.compile("([^-]+)-([^-]*)-\\1-([^-]+)-[^-]*-([^-]+)").matcher(pin);
        	Matcher ma5 = Pattern.compile("([^\\s]+) ([^\\s]*) ([^\\s]+) ([^\\s]+)").matcher(pin);
        	Matcher ma6 = Pattern.compile("(\\d{3}[A-Z]?)([A-Z]?)(\\d{5})(\\d{3})").matcher(pin);
        	if (ma1.matches()) {			//PIN from NB with dashes without group
        		controlMap = ma1.group(1);
        		parcel = ma1.group(2);
        	} else if (ma2.matches()) {		//PIN from NB with dashes with group
        		controlMap = ma2.group(1);
        		group = ma2.group(2);
        		parcel = ma2.group(3);
        	} else if (ma3.matches()) {		//PIN from NB without dashes
        		controlMap = ma3.group(1);
        		group = ma3.group(2);
        		parcel = ma3.group(3);
        	} else if (ma4.matches()) {		//PIN from AO
        		controlMap = ma4.group(1);
        		group = ma4.group(2);
        		parcel = ma4.group(3);
        		si = ma4.group(4);
        	} else if (ma5.matches()) {		//PIN from TR without si
        		controlMap = ma5.group(1);
        		group = ma5.group(2);
        		parcel = ma5.group(3);
        		si = ma5.group(4);
        	} else if (ma6.matches()) {		//PIN from TR with si
        		controlMap = ma6.group(1);
        		group = ma6.group(2);
        		parcel = ma6.group(3);
        		si = ma6.group(4);
        	}
        	
        	parcel = parcel.replace(".", "").replaceAll("\\A0+", "");
        	
        	if (si.matches("\\A0+$")) {
        		si = "";
        	}
				
			if (tsServerInfoModule != null) {
				PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
				for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					String paramName = htmlControl.getCurrentTSSiFunc().getParamName();
					if ("search[6]".equals(paramName)) {
						htmlControl.setDefaultValue(controlMap);
					} else if ("search[8]".equals(paramName)) {
						htmlControl.setDefaultValue(group);
					} else if ("search[9]".equals(paramName)) {
						htmlControl.setDefaultValue(parcel);
					} else if ("search[11]".equals(paramName)) {
						htmlControl.setDefaultValue(si);
					}
				}
			}
		}

		setModulesForAutoSearch(msiServerInfoDefault);

		return msiServerInfoDefault;
	}

	protected String getResultsTable() {
		String dataInfo = "";
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			String link = "https://www." + getSpecificCntySrvName().toLowerCase() + ".tennesseetrustee.org/hits.php";
			dataInfo = ((ro.cst.tsearch.connection.http2.TNGenericCountyTR) site).getPage(link);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		return dataInfo;
	}

	protected void loadDataHash(HashMap<String, String> data, String year) {
		if (data != null) {
			data.put("type", "CNTYTAX");
			data.put("year", year);
		}
	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:

			String dataInfo = "";

			dataInfo = getResultsTable();

			if (dataInfo.indexOf("iTotalRecords") == -1) {
				Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
				return;
			}
			
			int receiptYearIndex = -1;
			
			String tableHeader = "<table id=\"hit_list\" border=\"1\" cellspacing=\"5\">";
			try {

				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
				NodeList nodeList = htmlParser.parse(null);
				TableTag table = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "hit_list"), true).elementAt(0);
				table.setAttribute("border", "1");
				table.removeAttribute("cellspacing");
				table.setAttribute("cellspacing", "5");
				
				TableRow[] rows = table.getRows();;
				if (rows != null && rows.length > 0){
					TableColumn[] columns = rows[0].getColumns();
					if (columns != null){
						if (columns.length == 0){
							TableHeader[] headers = rows[0].getHeaders();
							for (int i = 0; i < headers.length; i++) {
								if (headers[i].toPlainTextString().toLowerCase().contains("year")){
									receiptYearIndex = i;
									break;
								}
							}
						} else{
							for (int i = 0; i < columns.length; i++) {
								if (columns[i].toPlainTextString().toLowerCase().contains("year")){
									receiptYearIndex = i;
									break;
								}
							}
						}
					}
				}
				tableHeader = table.toHtml().trim();
				tableHeader = tableHeader.replaceAll("(?is)</?tbody[^>]*>", "")
						.replaceAll("(?is)</?thead[^>]*>", "")
						.replaceAll("(?is)</table[^>]*>", "")
						.replaceAll("(?is)\\bPay\\b", "View Details");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			dataInfo = getTableFromDataTable(dataInfo, receiptYearIndex);

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, dataInfo, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}
			
			parsedResponse.setHeader(tableHeader);
			parsedResponse.setFooter("</table>");

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			break;

		case ID_DETAILS:
			if (!rsResponse.matches("(?is).*Property\\s*Tax\\s*Data.*")) {
				rsResponse = "<table><th><b>No records found or Official site error.</b></th></table>";
				Response.getParsedResponse().setOnlyResponse(rsResponse);
				return;
			}

			// get detailed document addressing code
			String query = Response.getQuerry();
			String year = StringUtils.extractParameterFromUrl(query, "year");
			
			if(!rsResponse.toLowerCase().contains("savetotsd")) { // if from memory
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList nodeList = htmlParser.parse(null);
					Div divMain = (Div) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "main"), true).elementAt(0);
					if (divMain != null) {
						rsResponse = divMain.toHtml();
						rsResponse = rsResponse.replaceAll("(?is)<img[^>]*>", "");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (StringUtils.isNotEmpty(year) && !rsResponse.contains("Receipt Year:")){
					rsResponse = rsResponse.replaceFirst("(?is)(<tr[^>]*>\\s*<td[^>]*>Receipt Number:</td>)", "<tr><td>Receipt Year:</td><td>" + year + "</td></tr>$1");
				}
			}

			
			String keyCode = StringUtils.extractParameterFromUrl(sAction, "id");

			String pid = keyCode;
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
				NodeList mainList = htmlParser.parse(null);

				pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Property ID:"), "", true).replaceAll("\\s+", "");

				// maybe it is a city
				if (year.equals("")) {
					NodeList nodes = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("class", "info_box"), true);
					if (nodes.size() > 0) {
						TableTag t = new TableTag();
						String rcptNr = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Receipt Number:"), "", true).trim();
						for (int i = 0; i < nodes.size(); i++)
							if (nodes.elementAt(i).toHtml().contains("Tax History")) {
								t = (TableTag) nodes.elementAt(i);
								break;
							}
						for (int i = 0; i < t.getRowCount(); i++) {
							if(t.getRow(i).toHtml().contains(rcptNr) && t.getRow(2).getColumnCount() > 0){
								year = t.getRow(i).getColumns()[0].toPlainTextString();
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if ((!downloadingForSave)) {
				String qry = Response.getRawQuerry();
				qry = "dummy=" + keyCode + "&" + qry;
				String originalLink = sAction + "&" + qry;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data, year);

				if (isInstrumentSaved(pid, null, data)) {
					rsResponse += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, rsResponse);
					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
			} else {// for html
				msSaveToTSDFileName = keyCode + ".html";

				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);

				msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();

				if (rsResponse.matches("(?is).*Click\\s*here\\s*to\\s*pay\\s*property\\s*taxes.*")) {
					parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS);
				} else {
					smartParseDetails(Response, rsResponse);
				}

				// set the last transfer date for this search
				// we take instrument or recorded date of the last transfer from
				// AO ,
				// it apears that this date on CT is not reliable
				// InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().setLastTransferDate(
				// Response.getParsedResponse() );
			}

			break;
		case ID_GET_LINK:
			if (rsResponse.matches("(?is).*Your\\s*Search\\s*Returned.*"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			else if (rsResponse.matches("(?is).*Property\\s*Tax\\s*Data.*")) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
				rsResponse = "<table><th><b>No records found or Official site error.</b></th></table>";
				Response.getParsedResponse().setOnlyResponse(rsResponse);
				return;
			}
			break;
		case ID_SAVE_TO_TSD:
			if (sAction.equals("/PropertySearch.aspx"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			else {// on save
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
			}
			break;
		default:
			break;

		}
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);

			TableTag mainTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "results"), true).elementAt(0);

			if (mainTable == null) {
				return intermediaryResponse;
			}

			TableRow[] rows = mainTable.getRows();

			for (TableRow row : rows) {
				if (row.getColumnCount() > 10) {

					TableColumn[] cols = row.getColumns();
					NodeList aList = cols[11].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);

					String link = ((LinkTag) aList.elementAt(0)).extractLink();
					String rowHtml = row.toHtml();

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap resultMap = parseIntermediary(row, searchId);
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					resultMap.removeTempDef();

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}
			outputTable.append(table);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	public String getTableFromDataTable(String input, int receiptYearIndex) {

		if (StringUtils.isEmpty(input)) {
			return "";
		}

		input = input.replaceAll("(?is)\\]{2}", "]").replaceAll("(?is)\\[{2}", "[");
		StringBuffer outTable = new StringBuffer("<table id=\"results\" >");
		Pattern EACH_ROW_PAT = Pattern.compile("(?is)\\[([^\\]]+)");
		Matcher mat = EACH_ROW_PAT.matcher(input);

		while (mat.find()) {
			StringBuffer eachRowBuff = new StringBuffer();
			String eachRow = mat.group(1);
			String[] cellseachRow = eachRow.split("\\\"\\s*,\\s*\\\"");
			eachRowBuff.append("<tr>");
			String link = "";
			for (int i = 0; i < cellseachRow.length; i++) {
				if (cellseachRow[i].contains("title")) {
					// on Rutherford the title id is on the name cell, on other
					// counties it is on the Add To Cart cell witch on
					// Rutherford is missing
					link = "<a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/taxcard.php?id="
							+ StringUtils.extractParameter(cellseachRow[i], "(?is)title\\s*=\\s*'([^']+)");
					if (receiptYearIndex > -1){
						link += "&year=" + cellseachRow[receiptYearIndex];
					}
					
					link	+= "\">View</a>";
					String content = cellseachRow[i];
					content = content.replaceAll("\\\"+", "").replaceAll("(?is)<center[^>]*>.*<\\\\?/center>", "").trim();
					if (StringUtils.isNotEmpty(content)) {
						eachRowBuff.append("<td>").append(content).append("</td>");
					}
				} else {
					eachRowBuff.append("<td>").append(cellseachRow[i].replaceAll("\\\"+", "")).append("</td>");
					if (cellseachRow[i].trim().toLowerCase().contains("year")){
						receiptYearIndex = i;
					}
				}
			}
			eachRowBuff.append("<td>").append(link).append("</td>").append("</tr>");
			outTable.append(eachRowBuff);
		}
		outTable.append("</table");

		return outTable.toString();
	}

	public ResultMap parseIntermediary(TableRow row, long searchId)
			throws Exception {

		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");

		TableColumn[] cols = row.getColumns();
		StringBuffer parcelID = new StringBuffer();
		for (int i = 0; i < cols.length; i++) {
			String contents = cols[i].getStringText().trim();
			switch (i) {
			case 0:
				if (StringUtils.isNotEmpty(contents)) {
					resultMap.put("PropertyIdentificationSet.OwnerLastName", contents);
				}
				break;
			case 1:
				if (StringUtils.isNotEmpty(contents)) {
					resultMap.put("tmpPropAddr", contents);
					resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(contents));
					resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(contents));
				}
				break;
			case 2:
				if (StringUtils.isNotEmpty(contents)) {
					resultMap.put("PropertyIdentificationSet.ParcelIDMap", contents);
					parcelID.append(contents).append(" ");
				}
				break;
			case 3:
				if (StringUtils.isNotEmpty(contents)) {
					resultMap.put("PropertyIdentificationSet.ParcelIDGroup", contents);
					parcelID.append(contents).append(" ");
				}
				break;
			case 4:
				if (StringUtils.isNotEmpty(contents)) {
					resultMap.put("PropertyIdentificationSet.ParcelIDParcel", contents);
					parcelID.append(contents).append(" ");
				}
				break;
			case 5:
				if (StringUtils.isNotEmpty(contents)) {
					resultMap.put("tmpSI", contents);
					parcelID.append(contents);
				}
				break;
			case 6:
				if (StringUtils.isNotEmpty(contents)) {
					resultMap.put("PropertyIdentificationSet.PropertyDescription", contents);
				}
				break;
			case 7:
				if (StringUtils.isNotEmpty(contents)) {
					resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", contents);
				}
				break;
			case 8:
				if (StringUtils.isNotEmpty(contents)) {
					resultMap.put("TaxHistorySet.ReceiptDate", contents);
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), contents);
				}
				break;

			default:
				break;
			}
		}
		resultMap.put("PropertyIdentificationSet.ParcelID", parcelID.toString().trim());

		GenericFunctions.ownerTNGenericTR(resultMap, searchId);
		GenericFunctions.legalTNGenericTR(resultMap, searchId);

		return resultMap;

	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map) {

		try {
			map.put("OtherInformationSet.SrcType", "TR");

			detailsHtml = detailsHtml.replaceAll("</td>\\s*(<tr[^>]*>)", "</td></tr>$1");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			String pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Property ID:"), "", true).trim();
			map.put("PropertyIdentificationSet.ParcelID", pid);

			String year = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Receipt Year:"), "", true).trim();
			map.put(TaxHistorySetKey.YEAR.getKeyName(), year);
			
			String salesPrice = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Last Sold Price:"), "", true).trim();
			salesPrice = salesPrice.replaceAll("[\\$,]", "");
			map.put("SaleDataSet.SalesPrice", salesPrice);

			String ownerName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Owner:"), "", true).trim();
			String coOwnerName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Co-Owner/Agent:"), "", true).trim();
			
			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			String owner = "";
			if ("rutherford".equalsIgnoreCase(crtCounty)) {
				if (ownerName.matches("(?is).+\\bET(UX|VIR)\\b.+")) {	//T7370
					owner = ownerName + " & " + coOwnerName;
				} else  {
					owner = ownerName + "  " + coOwnerName;
				}
			} else {
				owner = ownerName + "  " + coOwnerName;
			}
				
			
			map.put("PropertyIdentificationSet.OwnerLastName", owner);

			String propAddress = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Property Address:"), "", true).trim();
			map.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(propAddress));
			map.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(propAddress));

			String assessedValue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Assessed Value"), "", true).trim();
			assessedValue = assessedValue.replaceAll("[\\$,]", "");
			map.put("PropertyAppraisalSet.TotalAssessment", assessedValue);

			String totalAppraissal = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Total Property"), "", true).trim();
			totalAppraissal = totalAppraissal.replaceAll("[\\$,]", "");
			map.put("PropertyAppraisalSet.TotalAppraisal", totalAppraissal);

			String book = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Book:"), "", true).trim();
			map.put("SaleDataSet.Book", book.replaceAll("(?is)\\A0+", ""));

			String page = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Page:"), "", true).trim();
			map.put("SaleDataSet.Page", page.replaceAll("(?is)\\A0+", ""));

			String reicptNumber = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Receipt Number:"), "", true).trim();

			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			boolean taxHist = false, currentTax = false;
			for (int k = (tables.size() - 1); k < tables.size(); k--) {
				if (tables.elementAt(k).toHtml().contains("Tax History")) {
					List<List> body = new ArrayList<List>();
					List<String> line = null;
					String tmpPriorBaseAmt = "", tmpPriorAmtPaid = "", tmpPriorDue = "", taxYear = "";
					NodeList rows = tables.elementAt(k).getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"));
					for (int i = 2; i < rows.size(); i++) {
						NodeList tdList = rows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
						if (tdList.size() == 5) {
							if (reicptNumber.trim().equals(tdList.elementAt(1).toPlainTextString().trim()) && !currentTax) {
								taxYear = tdList.elementAt(0).toPlainTextString().trim();
								if (taxYear.equals(year)) {
									map.put("TaxHistorySet.BaseAmount", tdList.elementAt(2).toPlainTextString().replaceAll("[\\$,]", "").trim());
									map.put("TaxHistorySet.AmountPaid", tdList.elementAt(3).toPlainTextString().replaceAll("[\\$,]", "").trim());
									map.put("TaxHistorySet.DatePaid", tdList.elementAt(4).toPlainTextString().trim());
									taxHist = true;
									currentTax = true;
								}
							}
							if (taxHist
									&& !tdList.elementAt(4).toPlainTextString()
											.trim().equalsIgnoreCase("UnPaid")) {
								line = new ArrayList<String>();
								line.add(tdList.elementAt(1).toPlainTextString().trim());
								line.add(tdList.elementAt(3).toPlainTextString().replaceAll("[\\$,]", "").trim());
								line.add(tdList.elementAt(4).toPlainTextString().trim());
								body.add(line);
								tmpPriorBaseAmt += "+" + tdList.elementAt(2).toPlainTextString().replaceAll("[\\$,]", "").trim();
								tmpPriorAmtPaid += "+" + tdList.elementAt(3).toPlainTextString().replaceAll("[\\$,]", "").trim();
							} else {
								if (currentTax && (StringUtils.isNotEmpty(taxYear) && !taxYear.trim().equals(tdList.elementAt(0).toPlainTextString().trim()))) {
									tmpPriorDue += "+" + tdList.elementAt(2).toPlainTextString().replaceAll("[\\$,]", "").trim();
								}
							}
						}
					}
					tmpPriorBaseAmt = tmpPriorBaseAmt.replaceAll("\\A\\+[^\\+]+\\+", "");
					tmpPriorAmtPaid = tmpPriorAmtPaid.replaceAll("\\A\\+[^\\+]+\\+", "");
					map.put("tmpPriorAmtPaid", tmpPriorAmtPaid);
					map.put("tmpPriorBaseAmt", tmpPriorBaseAmt);
					map.put("tmpPriorDue", GenericFunctions.sum(tmpPriorDue, searchId));

					if (!body.isEmpty()) {
						ResultTable rt = new ResultTable();
						String[] header = { "ReceiptNumber", "ReceiptAmount", "ReceiptDate" };
						rt = GenericFunctions2.createResultTable(body, header);
						map.put("TaxHistorySet", rt);
					}
					break;
				}
			}

			TNGenericTR.ownerTNGenericTR(map, searchId);
			TNGenericTR.taxTNGenericTR(map, searchId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		boolean emptyPid = "".equals(sa.getAtribute(SearchAttributes.LD_PARCELNO));
		String streetDir = sa.getAtribute(SearchAttributes.P_STREETDIRECTION_ABBREV);
		String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);
		String streetNo = sa.getAtribute(SearchAttributes.P_STREETNO);
		String streetSuffix = sa.getAtribute(SearchAttributes.P_STREETSUFIX);
		TSServerInfoModule m;
		
		String streetNameExtended = streetName;
		String streetDirectionExtended = "";
		
		if (StringUtils.isNotEmpty(streetSuffix)) {
			streetSuffix = Normalize.translateSuffix(streetSuffix.toUpperCase());
		}
		if (StringUtils.isNotEmpty(streetDir)) {
			streetName = streetDir + " " + streetName;
			streetDirectionExtended = AddressAbrev.getFullDirectionFromAbbreviation(streetDir);
		}
		if (StringUtils.isNotEmpty(streetSuffix)) {
			streetName += " " + streetSuffix;
			streetNameExtended += " " + AddressAbrev.getFullSuffixFromAbbreviation(streetSuffix);
		}
		if (StringUtils.isNotEmpty(streetDirectionExtended)) {
			streetNameExtended += " " + streetDirectionExtended;
		}
		if (StringUtils.isNotEmpty(streetNo)) {
			streetName += " " + streetNo;
			streetNameExtended += " " + streetNo;
		}
		boolean emptySubdiv = "".equals(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
		boolean emptySubdivBlk = "".equals(sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK));
		boolean emptySubdivLot = "".equals(sa.getAtribute(SearchAttributes.LD_LOTNO));
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
		

		TaxYearFilterResponse frYr = new TaxYearFilterResponse(searchId, true, numberOfYearsAllowed, true);

		if (!emptyPid) {// parcel
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			
			m.getFunction(0).setSaKey("");// 0. LastName
			m.getFunction(1).setSaKey("");// 1. Address
			m.getFunction(8).setSaKey("");// 8. Subdivision
			m.getFunction(9).setSaKey("");// 9. Subdivision Block
			m.getFunction(10).setSaKey("");// 10. Subdivision Lot

			boolean found = false;
			String controlMap = "";
	    	String group = "";
	    	String parcel = "";
	    	String si = "";
			if (hasPin()) {
				String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
	        	Matcher ma1 = Pattern.compile("([^-]+)-([^-]+)").matcher(pin);
	        	Matcher ma2 = Pattern.compile("([^-]+)-([^-]+)-([^-]+)").matcher(pin);
	        	Matcher ma3 = Pattern.compile("(\\d{3}[A-Z]?)([A-Z]?)(\\d{3}\\.\\d{2})").matcher(pin);
	        	Matcher ma4 = Pattern.compile("([^-]+)-([^-]*)-\\1-([^-]+)-[^-]*-([^-]+)").matcher(pin);
	        	Matcher ma5 = Pattern.compile("([^-]+)-([^-]*)-([^-]+)-([^-]+)-[^-]*-([^-]+)").matcher(pin);
	        	Matcher ma6 = Pattern.compile("([^\\s]+) ([^\\s]*) ([^\\s]+) ([^\\s]+)").matcher(pin);
	        	Matcher ma7 = Pattern.compile("(\\d{3}[A-Z]?)([A-Z]?)(\\d{5})(\\d{3})").matcher(pin);
	        	if (ma1.matches()) {			//PIN from NB with dashes without group
	        		controlMap = ma1.group(1);
	        		parcel = ma1.group(2);
	        		found = true;
	        	} else if (ma2.matches()) {		//PIN from NB with dashes with group
	        		controlMap = ma2.group(1);
	        		group = ma2.group(2);
	        		parcel = ma2.group(3);
	        		found = true;
	        	} else if (ma3.matches()) {		//PIN from NB without dashes
	        		controlMap = ma3.group(1);
	        		group = ma3.group(2);
	        		parcel = ma3.group(3);
	        		found = true;
	        	} else if (ma4.matches()) {		//PIN from AO
	        		controlMap = ma4.group(1);
	        		group = ma4.group(2);
	        		if (pin.matches("([^-]+)-([^-]*)-\\1-([^-]+)--([^-]+)")) {
	        			parcel = ma4.group(3);
	        		} else {
	        			parcel = ma4.group(4);
	        		}
	        		found = true;
	        	} else if (ma5.matches()) {		//PIN from AO with first control map different from the second (e.g. 009D-A-005M-017.00--000)
	        		controlMap = ma5.group(3);
	        		group = ma5.group(2);
	        		parcel = ma5.group(4);
	        		found = true;
	        	} else if (ma6.matches()) {		//PIN from TR without si
	        		controlMap = ma6.group(1);
	        		group = ma6.group(2);
	        		parcel = ma6.group(3);
	        		si = ma6.group(4);
	        		found = true;
	        	} else if (ma7.matches()) {		//PIN from TR with si
	        		controlMap = ma7.group(1);
	        		group = ma7.group(2);
	        		parcel = ma7.group(3);
	        		si = ma7.group(4);
	        		found = true;
	        	}
	        	
	        	parcel = parcel.replace(".", "").replaceAll("\\A0+", "");
	        	
	        	if (si.matches("\\A0+$")) {
	        		si = "";
	        	}
			
				if (found) {
					
					m.getFunction(4).setData(controlMap);
					sa.setAtribute(SearchAttributes.LD_PARCELNO_MAP, controlMap);
	
					m.getFunction(5).setData(group);
					sa.setAtribute(SearchAttributes.LD_PARCELNO_GROUP, group);
	
					parcel = parcel.replaceAll("\\.", "");
					m.getFunction(6).setData(parcel);
					sa.setAtribute(SearchAttributes.LD_PARCELNO_PARCEL, parcel);
	
					if (si.length()!=0) {
						m.getFunction(7).setData(si);
					}
				}
			}	
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(frYr);
			l.add(m);
		}

		if (hasStreet()) {// address
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS_RUTH);
			
			m.getFunction(0).setSaKey("");// 0. LastName
			m.getFunction(4).setSaKey("");// 4. Map
			m.getFunction(4).setData("");// 4. Map
			m.getFunction(5).setSaKey("");// 5. Group
			m.getFunction(5).setData("");// 5. Group
			m.getFunction(6).setSaKey("");// 6. Parcel ID
			m.getFunction(7).setData("");// 7. Special Interest
			m.getFunction(8).setSaKey("");// 8. Subdivision
			m.getFunction(9).setSaKey("");// 9. Subdivision Block
			m.getFunction(10).setSaKey("");// 10. Subdivision Lot

			m.getFunction(1).setSaKey("");// 1. PropertyAddress
			m.getFunction(1).setData(streetName);
			
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(frYr);
			l.add(m);
			
			if (!streetNameExtended.equals(streetName)) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS_RUTH);
				
				m.getFunction(0).setSaKey("");// 0. LastName
				m.getFunction(4).setSaKey("");// 4. Map
				m.getFunction(4).setData("");// 4. Map
				m.getFunction(5).setSaKey("");// 5. Group
				m.getFunction(5).setData("");// 5. Group
				m.getFunction(6).setSaKey("");// 6. Parcel ID
				m.getFunction(7).setData("");// 7. Special Interest
				m.getFunction(8).setSaKey("");// 8. Subdivision
				m.getFunction(9).setSaKey("");// 9. Subdivision Block
				m.getFunction(10).setSaKey("");// 10. Subdivision Lot

				m.getFunction(1).setSaKey("");// 1. PropertyAddress
				m.getFunction(1).setData(streetNameExtended);			//WHITEHALL ROAD WEST 169
				
				m.addFilter(addressFilter);
				m.addFilter(nameFilterHybrid);
				m.addFilter(frYr);
				l.add(m);
			}

		}

		if (!emptySubdiv || !emptySubdivBlk || !emptySubdivLot) {// address
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			
			m.getFunction(0).setSaKey("");// 0. LastName
			m.getFunction(1).setSaKey("");// 1. Address
			m.getFunction(4).setSaKey("");// 4. Map
			m.getFunction(4).setData("");// 4. Map
			m.getFunction(5).setSaKey("");// 5. Group
			m.getFunction(5).setData("");// 5. Group
			m.getFunction(6).setSaKey("");// 6. Parcel ID
			m.getFunction(7).setData("");// 7. Special Interest

			m.getFunction(8).setSaKey(SearchAttributes.LD_SUBDIV_NAME);// 8.
																		// Subdivision
																		// Name
			m.getFunction(9).setSaKey(SearchAttributes.LD_SUBDIV_BLOCK);// 9.
																		// Subdivision
																		// Block
			m.getFunction(10).setSaKey(SearchAttributes.LD_LOTNO);// 10.
																	// Subdivision
																	// Lot

			m.getFunction(8).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE);

			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(frYr);

			l.add(m);
		}

		if (hasOwner()) {// owner
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addFilter(frYr);

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] { "L F;;", "L f;;", "L F M;;", "L M;;", "L m;;" });
			m.addIterator(nameIterator);

			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);
	}

	protected String getFileNameFromLink(String url) {
		String keyCode = "File";
		if (url.contains("RCT_NO=")) {
			keyCode = org.apache.commons.lang.StringUtils.substringBetween(url, "RCT_NO=", "&");
		}

		return keyCode + ".html";
	}
}

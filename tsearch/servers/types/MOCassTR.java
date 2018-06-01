package ro.cst.tsearch.servers.types;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.Span;
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
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.MultipleYearIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.tsrindex.server.UtilForGwtServer;

/**
 * @author vladb
  */

public class MOCassTR extends TSServerAssessorandTaxLike {

	public static final long serialVersionUID = 10000000L;
	private static final String FORM_NAME = "aspnetForm";
	private static int seq = 0;
	
	private static HashMap<String, Integer> taxYears = new HashMap<String, Integer>();
	
	public MOCassTR(long searchId) {
		super(searchId);
	}

	public MOCassTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	protected synchronized static int getSeq(){
		return seq++;
	}
	
	
	private void getTaxYears(String county) {
		if (taxYears.containsKey("lastTaxYear" + county)  && taxYears.containsKey("firstTaxYear" + county))
			return;
		
		// Get official site html response.
		String response = getLinkContents(getDataSite().getLink());
		if(response != null) {
			HtmlParser3 parser = new HtmlParser3(response);
			NodeList selectList = parser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("select"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "ctl00$mainContent$cboYears"));
			if(selectList == null || selectList.size() == 0) {
				// Unable to find the tax year select input.
				logger.error("Unable to parse tax years!");
				return;
			}
			
			// Get the first and last tax years.
			SelectTag selectTag = (SelectTag) selectList.elementAt(0);
			OptionTag[] options = selectTag.getOptionTags();
			try {
				taxYears.put("lastTaxYear"+county, Integer.parseInt(options[0].getValue().trim()));
				taxYears.put("firstTaxYear"+county, Integer.parseInt(options[options.length - 1].getValue().trim()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Generate a <select> input corresponding to the tax years
	 * @param id
	 * @param name
	 * @return
	 */
	public String getYearSelect(String id, String name){
		
//		getTaxYears();
		
		String county = dataSite.getCountyName();
		getTaxYears(county);
		int lastTaxYear = taxYears.get("lastTaxYear" + county);
		int firstTaxYear = taxYears.get("firstTaxYear" + county);
		if (lastTaxYear <= 0 || firstTaxYear <= 0) {
			// No valid tax years.
			// This is going to happen when official site is down or it's going to change its layout.
			lastTaxYear = 2012;
			firstTaxYear = 2008;
		}
		
		// Generate input.
		StringBuilder select  = new StringBuilder("<select id=\"" + id + "\" name=\"" + name + "\" size=\"1\">\n");
		for (int i = lastTaxYear; i >= firstTaxYear; i--){
			select.append("<option ");
			select.append(i == lastTaxYear ? " selected " : "");
			select.append("value=\"" + i + "\">" + i + "</option>\n");
		}
		select.append("</select>");
		
		return select.toString();
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		if(tsServerInfoModule != null && tsServerInfoModule.getFunctionCount()>4) {
			tsServerInfoModule.getFunction(4).setHtmlformat(getYearSelect("param_0_4", "param_0_4"));
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		if(tsServerInfoModule != null && tsServerInfoModule.getFunctionCount()>3) {
			tsServerInfoModule.getFunction(3).setHtmlformat(getYearSelect("param_2_3", "param_2_3"));
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null && tsServerInfoModule.getFunctionCount()>3) {
			tsServerInfoModule.getFunction(3).setHtmlformat(getYearSelect("param_1_3", "param_1_3"));
			
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PROP_NO_IDX);
		if(tsServerInfoModule != null && tsServerInfoModule.getFunctionCount()>3) {
			tsServerInfoModule.getFunction(3).setHtmlformat(getYearSelect("param_15_3", "param_15_3"));
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_PARCEL:	
			case ID_SEARCH_BY_PROP_NO:
				// no result
				if (rsResponse.indexOf("No records found with matching criteria") > -1) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				// parse and store parameters on search
				Form form = new SimpleHtmlParser(rsResponse).getForm(FORM_NAME);
				Map<String, String> params = form.getParams();
				params.remove("__EVENTTARGET");
				params.remove("__EVENTARGUMENT");
				int seq = getSeq();
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
				
				StringBuilder outputTable = new StringBuilder();
				outputTable.append(String.valueOf(seq)); // use outputTable to transmit seq
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				if (rsResponse.replaceAll(" +", " ").indexOf("CASS COUNTY **** REAL ESTATE **** TAX") > -1) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				
				if(viParseID == ID_DETAILS) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
						
					// get taxYear
					String taxYear = "";
					Matcher m = Pattern.compile("TAX\\s+YEAR:\\s*(\\d+)").matcher(details);
					if(m.find()) {
						taxYear = m.group(1);
					}
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type", "CNTYTAX");
					data.put("year", taxYear);
					if (isInstrumentSaved(accountId.toString().trim(), null, data)){
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					parsedResponse.setResponse(details);
				} else {
					String filename = accountId + ".html";
					smartParseDetails(Response,details);
					
					msSaveToTSDFileName = filename;
					parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					parsedResponse.setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
				
				break;
		}
	}
	
	private String getDetails(String page, StringBuilder accountId) {
		
		StringBuilder details = new StringBuilder();
		
		try {
			page = page.replaceAll("(?is)<table ", "<table border=\"1\"");
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node header = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_lblHeader"), true)
				.elementAt(0);
			Node ownerInfo = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_lblName"), true)
				.elementAt(0);
			Node taxInfo = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_lblTaxInfo"), true)
				.elementAt(0);
			Node propDescTab = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_lblProperty"), true)
				.elementAt(0);
			Node taxTab = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_lblTaxes"), true)
				.elementAt(0);
			
			Matcher m = Pattern.compile("ACCT\\s*#:\\s*(\\d+)").matcher(taxInfo.toPlainTextString());
			if(m.find()) {
				accountId.append(m.group(1));
			}
			
			details.append("<table align=\"center\" border=\"1\">");
			details.append("<tr><td colspan=\"2\">" + header.toHtml() + "</td></tr>");
			details.append("<tr><td>" + ownerInfo.toHtml() + "</td>");
			details.append("<td>" + taxInfo.toHtml() + "</td></tr>");
			details.append("<tr><td>" + propDescTab.toHtml() + "</td>");
			details.append("<td>" + taxTab.toHtml() + "</td></tr>");
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			for(int i = tableList.size() - 1; i >= 0; i--) { // the deliquency table is probably at the end of the page
				Node tableNode = tableList.elementAt(i);
				if(tableNode.toPlainTextString().indexOf("Delinquency Schedule") > -1) {
					details.append("<tr><td colspan=\"2\">" + tableNode.toHtml() + "</td></tr>");
					break;
				}
			}
			
			details.append("</table>");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return details.toString();
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		 String seq = outputTable.toString();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
				
			NodeList mainTableList = nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_mainContent_dgrdResults"), true);
			Form form = new SimpleHtmlParser(table).getForm(FORM_NAME);
		
			if(mainTableList.size() != 1) {
				return intermediaryResponse;
			}
			
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows  = tableTag.getRows();
		
			for (int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 6) { // process ordinary rows
					if(row.toPlainTextString().indexOf("MAILING ADDRESS") > -1) {
						continue;
					}
					
					LinkTag linkTag = ((LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					String link = createLink(linkTag.getLink(), form.action, seq);
					linkTag.setLink(link);
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap resultMap = null;
					resultMap = ro.cst.tsearch.servers.functions.MOCassTR.parseIntermediaryRow(row, searchId);
					
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				} else if(row.getColumnCount() == 1) { // process navigation links
					LinkTag prev = null;
					LinkTag next = null;				
					boolean foundCurrent = false;
					NodeList pages = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("td"), true)
						.elementAt(0).getChildren();
					for(int j = 0; j < pages.size(); j++) {
						Node page = pages.elementAt(j);
						if(page instanceof Span) {
							foundCurrent = true;
						} else if(page instanceof LinkTag) {
							if(!foundCurrent) {
								prev = (LinkTag) page;
							} else {
								next = (LinkTag) page;
								break;
							}
						}
					}
					
					String footer = "<tr><td colspan=\"6\" align=\"center\">";
					String prevLink = "";
					String nextLink = "";
					if(prev != null) {
						prevLink += "<a href=\"" + createLink(prev.getLink(), form.action, seq) + "\">Prev</a>&nbsp;&nbsp;&nbsp;";
					}
					if(next != null) {
						nextLink += "<a href=\"" + createLink(next.getLink(), form.action, seq) + "\">Next</a>";
					}
					footer += prevLink + nextLink + "</td></tr></table>";
					response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\">" +
							"<tr><th>Details</th><th>ACCT</th><th>NAME</th>" +
							"<th>MAILING ADDRESS</th><th>ORIGINAL TAX DUE</th><th>DATE PAID</th></tr>");
					response.getParsedResponse().setFooter(footer);
					response.getParsedResponse().setNextLink(nextLink);
				}
			}
		
			outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	private String createLink(String originalLink, String formAction, String seq) {
		try {
			Pattern pattern = Pattern.compile("__doPostBack\\('([^']*)','([^']*)'\\)");
			Matcher matcher = pattern.matcher(originalLink);
			String target = "";
			String argument = "";
			if (matcher.find()) {
				target = matcher.group(1);
				argument = matcher.group(2);
			}
			String linkStart = CreatePartialLink(TSConnectionURL.idPOST);
			String link = linkStart + "/" + URLEncoder.encode(formAction,"UTF-8") + 
				"&seq=" + seq +
				"&__EVENTTARGET=" + target + 
				"&__EVENTARGUMENT=" + argument;
			return link;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String page, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			
			String properPage = page.replaceAll("(?is)<br[ /]*>", "\n");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(properPage, null);
			NodeList nodeList = htmlParser.parse(null);
		
			String ownerInfo = "";
			String addr = "";
			String legal = "";
			
			Node ownerInfoNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_lblName"), true)
				.elementAt(0);
			Node taxInfoNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_lblTaxInfo"), true)
				.elementAt(0);
			Node propDescNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_lblProperty"), true)
				.elementAt(0);
			Node taxNode = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_lblTaxes"), true)
				.elementAt(0);
		
			// get ownerInfo
			ownerInfo = ownerInfoNode.toPlainTextString()
				.replaceAll(" +", " ").replaceAll("&nbsp;", "").trim();
			
			// get taxInfo
			String taxInfo = taxInfoNode.toPlainTextString();			
			Matcher m = Pattern.compile("TAX\\s+YEAR:\\s*(\\d+)").matcher(taxInfo);
			if(m.find()) {
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), m.group(1));
			}
			m = Pattern.compile("ACCT\\s*#:\\s*(\\d+)").matcher(taxInfo);
			if(m.find()) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), m.group(1));
			}
			if(taxInfo.indexOf("NOT PAID") > -1) {
				m = Pattern.compile("TOTAL\\s+DUE:\\s*[$]([\\d.,]+)").matcher(taxInfo);
				if(m.find()) {
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), m.group(1).replaceAll(",", ""));
				}
			} else {
				m = Pattern.compile("TOTAL\\s+PAID:\\s*[$]([\\d.,]+)").matcher(taxInfo);
				if(m.find()) {
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), m.group(1).replaceAll(",", ""));
				}
				m = Pattern.compile("PAID\\s+ON:\\s*([\\d-]+)").matcher(taxInfo);
				if(m.find()) {
					resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), m.group(1));
				}
			}
			
			// get property info
			TableTag propDescTable = (TableTag) propDescNode.getChildren()
				.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			TableRow[] rows = propDescTable.getRows();
			for(int i = 0; i < rows.length; i++) {
				String key = rows[i].toPlainTextString();
				
				if(key.startsWith("Map Number:")) {
					String val = rows[++i].toPlainTextString().trim();
					val = val.replaceAll("[^\\d]", "");
					val = org.apache.commons.lang.StringUtils.leftPad(val, 18, '0');
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), val);
				} else if(key.startsWith("Situs Address:")) {
					addr = rows[++i].toPlainTextString().trim();
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr);
				} else if(key.indexOf("SEC:") > 0 || key.indexOf("Book/Page:") > 0) {
					m = Pattern.compile("SEC:\\s*(\\d+)\\s*TWP:\\s*(\\d+)\\s*RNG:\\s*(\\d+)").matcher(key);
					if(m.find()) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), m.group(1));
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), m.group(2));
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), m.group(3));
					}
					m = Pattern.compile("Book/Page:\\s*(\\d+)/(\\d+)").matcher(key);
					if(m.find()) {
						List<List> body = new ArrayList<List>();
						List<String> line = new ArrayList<String>();
			
						line.add(m.group(1));
						line.add(m.group(2));
						line.add("");
						body.add(line);	
						
						String[] header = { "Book", "Page", "InstrumentNumber" };
						ResultTable rt = GenericFunctions2.createResultTable(body, header);
						resultMap.put("CrossRefSet", rt);
					}
				} else if(key.startsWith("Legal Description:")) {
					legal = rows[++i].toPlainTextString().trim();
				} else if(key.startsWith("Subdivision")) {
					String subBlock = rows[++i].toPlainTextString().trim();
					String[] parts = subBlock.split("\\s+");
					String sub = "";
					for(int j = 0; j < parts.length - 1; j++) {
						sub += parts[j] + " ";
					}
					if(!parts[parts.length - 1].matches("\\d+[+-]?|-")) {
						sub += parts[parts.length - 1];
					}
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), sub);
				} else if(key.startsWith("TOTAL ASSESSED:")) {
					String val = rows[i].getColumns()[1].toPlainTextString().replaceAll("[$,]", "").trim();
					resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), val);
				}
			}
			
			TableTag taxTable = (TableTag) taxNode.getChildren()
				.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			rows = taxTable.getRows();
			for(int i = 0; i < rows.length; i++) {
				if(rows[i].getColumnCount() == 3) {
					String key = rows[i].getColumns()[0].toPlainTextString().trim();
					String val = rows[i].getColumns()[2].toPlainTextString().replaceAll("[$,]", "").trim();
					if(key.startsWith("Tax Amount:")) {
						resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), val);
					}
				}
			}
			
			ro.cst.tsearch.servers.functions.MOCassTR.parseNames(resultMap, ownerInfo, true);
			ro.cst.tsearch.servers.functions.MOCassTR.parseLegalDescription(resultMap, legal);
			ro.cst.tsearch.servers.functions.MOCassTR.parseAddress(resultMap, addr);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		
		FilterResponse cityFilter = CityFilterFactory.getCityFilterDefault(searchId);

		if (hasPin()) {
			//Search by Pin
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			if (pin.length()<18) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(3, SearchAttributes.CURRENT_TAX_YEAR);
				module.forceValue(4, pin);
				module.setMutipleYears(true);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				MultipleYearIterator yearIterator = (MultipleYearIterator) ModuleStatesIteratorFactory.getMultipleYearIterator(module, searchId, numberOfYearsAllowed, getCurrentTaxYear());
				module.addIterator(yearIterator);
				modules.add(module);
				
			} else if (pin.matches("\\d{18}")) {	//search with Map #
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PROP_NO_IDX));
				module.clearSaKeys();
				module.setSaKey(3, SearchAttributes.CURRENT_TAX_YEAR);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				module.setMutipleYears(true);
				
				MultipleYearIterator yearIterator = (MultipleYearIterator) ModuleStatesIteratorFactory.getMultipleYearIterator(module, searchId, numberOfYearsAllowed, getCurrentTaxYear());
				module.addIterator(yearIterator);
				
				module.forceValue(7, pin);
				modules.add(module);
			}
		}
		
		String pin2 = getSearchAttribute(SearchAttributes.LD_PARCELNO2);
		if (!StringUtils.isEmpty(pin2)) {
			if (pin2.matches("\\d{18}")) {	//search with Map #
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PROP_NO_IDX));
				module.clearSaKeys();
				module.setSaKey(3, SearchAttributes.CURRENT_TAX_YEAR);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				module.setMutipleYears(true);
				
				MultipleYearIterator yearIterator = (MultipleYearIterator) ModuleStatesIteratorFactory.getMultipleYearIterator(module, searchId, numberOfYearsAllowed, getCurrentTaxYear());
				module.addIterator(yearIterator);
				
				module.forceValue(7, pin2);
				modules.add(module);
			}
		}
		
		if (hasStreetNo() && hasStreet()) {
			//Search by Property Address
			String address = streetNo + " " + streetName;
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(3, SearchAttributes.CURRENT_TAX_YEAR);
			module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
			module.setMutipleYears(true);
			
			module.getFunction(6).forceValue(address);
			module.addFilter(cityFilter);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			
			MultipleYearIterator yearIterator = (MultipleYearIterator) ModuleStatesIteratorFactory.getMultipleYearIterator(module, searchId, numberOfYearsAllowed, getCurrentTaxYear());
			module.addIterator(yearIterator);
			
			modules.add(module);
		}
		
		if (hasOwner()){
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setSaKey(4, SearchAttributes.CURRENT_TAX_YEAR);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			module.setMutipleYears(true);
			
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(numberOfYearsAllowed, module, searchId, new String[] {"L, F M;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent,
			boolean forceOverritten) {
		ADD_DOCUMENT_RESULT_TYPES result =  super.addDocumentInATS(response, htmlContent, forceOverritten);
		try {
			if(result.equals(ADD_DOCUMENT_RESULT_TYPES.ADDED)) {
				UtilForGwtServer.uploadDocumentToSSF(searchId, response.getParsedResponse().getDocument());
			}
		} catch (Exception e) {
			logger.error("Error while saving index for " + searchId, e);
		}
		return result;
	}
}
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.parser.SimpleHtmlParser.Input;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
 */

public abstract class FLGenericPacificBlueTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave;
	private static HashMap<String, Integer> taxYears = new HashMap<String, Integer>();

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");

	private static final Pattern OPTION_PAT = Pattern.compile("(?is)<option[^>]+>(\\d+)");
	private static final Pattern FULL_LEGAL_PAT = Pattern.compile("(?is)var\\s+FULLLEGAL\\s*=\\s*'([^']+)");
	
	private static final String LEVY_ADDRESS = "https://www.lctax.org/ptaxweb/";
	private static final String MANATEE_ADDRESS = "https://secure.taxcollector.com/ptaxweb/";

	public FLGenericPacificBlueTR(long searchId) {
		super(searchId);
	}

	public FLGenericPacificBlueTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {

		// add dashes to pin if necessary
		if (module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX) {
			String pin = module.getFunction(0).getParamValue();
			pin = pin.replaceAll("\\p{Punct}", "").replaceAll("\\s+", "");

			module.getFunction(0).setParamValue(pin);

		}
		return super.SearchBy(module, sd);
	}

	/** 
	 * 	 Get tax year range from official site,
	**/
	private void getTaxYears(String county) {
		if (taxYears.containsKey("lastTaxYear" + county)  && taxYears.containsKey("firstTaxYear" + county))
			return;
		// Get official site html response
		
		String link = getDataSite().getLink();
		String response = getLinkContents(link + "editPropertySearch.do");
		
		if(org.apache.commons.lang.StringUtils.isNotEmpty(response)) {
			HtmlParser3 parser = new HtmlParser3(response);
			NodeList selectList = parser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("select"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "taxYear"));
			if(selectList == null || selectList.size() == 0) {
				// Unable to find the tax year select input.
				logger.error("Unable to parse tax years!");
				taxYears.put("lastTaxYear" +county, getCurrentTaxYear());
				taxYears.put("firstTaxYear"+county, 1990);
				return;
			}			
			// Get the first and last tax years.
			SelectTag selectTag = (SelectTag) selectList.elementAt(0);
			OptionTag[] options = selectTag.getOptionTags();
			try {
				//options[0] = All Yrs.
				taxYears.put("lastTaxYear"+county, Integer.parseInt(options[1].getValue().trim()));
				taxYears.put("firstTaxYear"+county, Integer.parseInt(options[options.length - 1].getValue().trim()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	protected int getCurrentTaxYear(){
		try {
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			Calendar cal = Calendar.getInstance();
			if (dataSite != null) {
				cal.setTime(dataSite.getPayDate());
			}
			return cal.get(Calendar.YEAR);
			
		} catch (Exception e) {
			return -1;
		}
	}
	
	/**
	 * Generate a <select> input corresponding to the tax years
	 * @param id
	 * @param name
	 * @return
	 */
	public String getYearSelect(String id, String name){
		String county = dataSite.getCountyName();
		
		int lastTaxYear = -1;
		try {
			lastTaxYear = taxYears.get("lastTaxYear" + county);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int firstTaxYear = -1;
		try {
			firstTaxYear = taxYears.get("firstTaxYear" + county);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (lastTaxYear <= 0 || firstTaxYear <= 0) {
			// No valid tax years.  This is going to happen when official site is down or it's going to change its layout.
			lastTaxYear = getCurrentTaxYear();
			firstTaxYear = 1990;
		}
			
		// Generate select 
		StringBuilder select  = new StringBuilder("<select id=\"" + id + "\" name=\"" + name + "\" size=\"1\">\n");
		select.append("<option value=\"ALL\" selected>All Yrs.</option>\n");
		for (int i = lastTaxYear; i >= firstTaxYear; i--){
			select.append("<option ");
			//select.append(i == lastTaxYear ? " selected " : "");
			select.append("value=\"" + i + "\">" + i + "</option>\n");
		}
		select.append("</select>");
			
		return select.toString();
	}
	
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		getTaxYears(dataSite.getCountyName());
		// Generate the select input corresponding to the tax years.
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(1).setHtmlformat(getYearSelect("param_0_1", "param_0_1"));
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(1).setHtmlformat(getYearSelect("param_1_1", "param_1_1"));
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(1).setHtmlformat(getYearSelect("param_2_1", "param_2_1"));
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(1).setHtmlformat(getYearSelect("param_3_1", "param_3_1"));
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(1).setHtmlformat(getYearSelect("param_4_1", "param_4_1"));
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.BGN_END_DATE_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(1).setHtmlformat(getYearSelect("param_5_1", "param_5_1"));
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String address = getSearch().getSa().getAtribute(SearchAttributes.P_STREET_NO_NAME);
		String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter(searchId, 0.8d);
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);

		if (hasPin()) {
			// Search by PIN
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin);
			module.addFilter(pinFilter);
			module.addFilter(new TaxYearFilterResponse(searchId));
			modules.add(module);

		}

		if (hasStreet()) {
			// Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(address);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(new TaxYearFilterResponse(searchId));
			modules.add(module);
		}

		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
		if (hasOwner()) {
			// Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.clearSaKeys();
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(new TaxYearFilterResponse(searchId));
			if ("levy".equalsIgnoreCase(crtCounty))
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			else 
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(
					module, searchId, new String[] { "L;F;" });
			module.addIterator(nameIterator);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;
		String contents = "";
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {

		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_MODULE27:	

			StringBuilder outputTable = new StringBuilder();

			if (rsResponse.indexOf("Nothing found to display") != -1) {
				Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
				return;
			}
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
				NodeList mainList = htmlParser.parse(null);
				contents = HtmlParser3.getNodeByID("currentTableObject", mainList, true).toHtml();
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, contents, outputTable);

				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case ID_DETAILS:

			String details = "",
			pid = "",
			taxYear = "";
			details = getDetails(rsResponse);

			pid = getParser().parseParcelID(details).trim(); 
//				StringUtils.extractParameter(details, "(?is)PROPERTY\\s+ID\\s*#\\s*:(?:\\s*</b>)?\\s*(\\d+)");
			taxYear = StringUtils.extractParameter(details, "(?is)TAX\\s+YEAR\\s*:(?:\\s*</b>\\s*[nbsp;&]+)\\s*(\\d+)");
			
			if ((!downloadingForSave)) {

				String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + pid + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");
				data.put("year", taxYear);

				if (isInstrumentSaved(pid, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}
				
				//test
//				ResultMap map = new ResultMap();
//				getParser().parseDetails(details, searchId, map);
				
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), details, Parser.NO_PARSE);
			} else {
				Response.setResult(details);
				details = details.replaceAll("</?a[^>]*>", "");
				smartParseDetails(Response, details);
				msSaveToTSDFileName = pid + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();

			}

			break;

		case ID_GET_LINK:
			if (sAction.indexOf("action=detail") != -1) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			}

			break;

		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			break;
		}
	}

	protected String getDetails(String response) {

		// if from memory - use it as is
		if (!response.toLowerCase().contains("<html")) {
			return response;
		}

		Form frm = new SimpleHtmlParser(response).getForm("propertySearchWebForm");
		String contents = "";

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tables.size() > 3) {
				for (int i = 0; i < tables.size() - 1; i++) {
					if (tables.elementAt(i).toHtml().toLowerCase().contains("property detail")) {
						contents = tables.elementAt(i).toHtml();
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		contents = contents.replaceAll("(?is)</?body[^>]*>", "");
		contents = contents.replaceAll("(?is)</?form[^>]*>", "");
		contents = contents.replaceAll("(?is)(?is)<script.*?</script>", "");
		contents = contents.replaceAll("(?is)<input[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		contents = contents.replaceAll("(?is)<div[^>]*>\\s*</div>", "");
		contents = contents.replaceAll("(?is)(<select)\\s+", "$1 disabled=\"true\"");
		contents = contents.replaceAll("(?is)Navigate to the payment section below to view/print receipts\\.", "");
		
		contents = contents.replaceAll("(?is)(currentTableObject4)", "$1CurrentYear");

		Matcher legalMat = FULL_LEGAL_PAT.matcher(response);
		if (legalMat.find()) {
			contents = contents.replaceAll("(?is)(LEGAL DESCRIPTION:\\s*(?:</b>)?\\s*(?:<br\\s*>)?).*?Full\\s+legal\\s*</a>", "$1"
					+ legalMat.group(1));
		}
		if (frm != null) {

			String contentsPaym = "<b>Payment History</b>";
			String contentsPriorDelinq = "<b>Prior Due</b><br><br>";
			HtmlParser3 parser = new HtmlParser3(response);
			String select = parser.getNodeByAttribute("name", "taxYear", true).toHtml();
			Matcher mat = OPTION_PAT.matcher(select);
			String link = getBaseLink() + frm.action;
			link = link.replaceAll("(?is)/ptaxweb/?(/ptaxweb/)", "$1");
			boolean selected = false;
			while (mat.find()) {
				if (mat.group(0).contains("selected")) {
					selected = true;
					continue;
				}
				if (selected) {
					HTTPRequest req = new HTTPRequest(link, HTTPRequest.POST);
					req.setPostParameter("action", "detailByNewYear");
					req.setPostParameter("taxYear", mat.group(1));
					for (Input inp : frm.inputs) {
						if (inp.name.contains("searchField")) {
							req.setPostParameter(inp.name, inp.value);
						}
						if (inp.name.contains("propertyId")) {
							req.setPostParameter(inp.name, inp.value);
						}
						if (inp.name.contains("property.accountNumber")) {
							req.setPostParameter(inp.name, inp.value);
						}
						if (inp.name.contains("property.onInstallmentPlan")) {
							req.setPostParameter(inp.name, inp.value);
						}
						if (inp.name.contains("property.fromWatchList")) {
							req.setPostParameter(inp.name, inp.value);
						}
						if (inp.name.contains("searchValue")) {
							req.setPostParameter(inp.name, inp.value);
						}
					}
					HTTPResponse res = null;
					HttpSite site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(),
							searchId);
					try {
						res = site.process(req);
					} finally {
						HttpManager.releaseSite(site);
					}
					String resp = res.getResponseAsString();
					parser = new HtmlParser3(resp);
					String status = HtmlParser3.getValueFromNextCell(parser.getNodeList(), "STATUS:", "", false);
					if (StringUtils.isNotEmpty(status) && status.trim().contains("Unpaid")){
						String year = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(parser.getNodeList(), "TAX YEAR:"), "", true);
						String delinqTable = HtmlParser3.getValueFromAbsoluteCell(1, 0, 
												HtmlParser3.findNode(parser.getNodeList(), "Amount Due if Received by:"), "", true);
						if (StringUtils.isNotEmpty(delinqTable)){
							delinqTable = delinqTable.replaceAll("(?is)(<table[^>]*>\\s*<tr[^>]*>)", "$1<td>" + year + "</td>")
													.replaceAll("(?is)(</tr[^>]*>\\s*<tr[^>]*>)", "$1<td>" + status.trim() + "</td>")
													.replaceAll("(?is)(<table)([^>]*>)", "$1 id=\"idPriorDue\" width=\"750\">");
							contentsPriorDelinq += "<br><br>" + delinqTable + "<br><br>";
						}
					} else {
						String paymTable = parser.getNodeById("currentTableObject4").toHtml();
						contentsPaym += "<br><br>" + paymTable + "<br><br>";
					}
				}
			}
			contents += contentsPriorDelinq.replaceAll("(?is)<td[^>]*>", "<td width=\"20%\">");
			contents += contentsPaym;
		}
		
		String pin = getParser().parseParcelID(contents); 
		String linkForAssessor = getLinkForAssessorPage(pin);
		String assessorPage = "";
		contents = addAppraissalDataToContents(contents, linkForAssessor, assessorPage);
		
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
		if ("levy".equalsIgnoreCase(crtCounty))
			contents = contents.replaceAll("(?is)<a class=\"action\".*?>.*?</a>", "");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");

		return contents.trim();
	}

	
	protected abstract String addAppraissalDataToContents(String contents, String linkForAssessor, String assessorPage);
	protected abstract String getLinkForAssessorPage(String pin);
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
			
			table = table.replaceAll("\n", "").replaceAll("\r", "");

			Map<String, String> paramsLink = new HashMap<String, String>();

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(null);
			NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tableList.size() > 0) {
				TableTag mainTable = (TableTag) tableList.elementAt(0);
				TableRow[] rows = mainTable.getRows();
				TableRow headerRow = rows[0];
				if ("manatee".equalsIgnoreCase(crtCounty)) {
					headerRow.removeChild(0);
					headerRow.removeChild(0);
				}
				headerRow.removeChild(headerRow.getChildCount() - 1);
				headerRow.removeChild(headerRow.getChildCount() - 1);
				String tableHeader = headerRow.toHtml();
				
				for (TableRow row : rows) {
					if (row.getColumnCount() > 1) {
						if ("manatee".equalsIgnoreCase(crtCounty)) {
							row.removeChild(0);
							row.removeChild(0);
						}
						row.removeChild(row.getChildCount() - 1);
						row.removeChild(row.getChildCount() - 1);

						TableColumn[] cols = row.getColumns();
						int actionColumnIndex = getActionColumnIndex();
						boolean alteredRow = false;
						if (cols.length==actionColumnIndex) {
							alteredRow = true;
							actionColumnIndex--;
						}	
						if ("manatee".equalsIgnoreCase(crtCounty))
							actionColumnIndex = 1;
						NodeList aList = cols[actionColumnIndex].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						if (!"manatee".equalsIgnoreCase(crtCounty)) {
							if (rows[0].toHtml().toLowerCase().contains("mortgage")) {// for
								aList = cols[11].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
								if (cols.length == 13){
									aList = cols[12].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
								}
							}
						}
						int index = 0;
						if (alteredRow)
							index = 1;
						if (aList.size() > index) {
							String parcel = ((LinkTag) aList.elementAt(index)).extractLink();
							String link = CreatePartialLink(TSConnectionURL.idGET) + parcel;
							if (aList.size() >= 3) {	//for Franklin and Levy there are 3 links, for Manatee there are 4 links
								String linkAO = ((LinkTag) aList.elementAt(2)).extractLink();
								String pin = getPinFromLink(linkAO);
								if (StringUtils.isNotEmpty(pin)) {
									paramsLink.put("assessorLink:" + pin, linkAO);
								}
							}
							if (!"manatee".equalsIgnoreCase(crtCounty))
								row.removeChild(row.getChildCount() - 1);
							String rowHtml = row.toHtml();
							if ("manatee".equalsIgnoreCase(crtCounty))
								rowHtml = rowHtml.replaceAll("(?is)<a[^>]+>(\\d+)</a>", "<aaa href=\"" + link + "\">$1</a>");
							if ("levy".equalsIgnoreCase(crtCounty) || "manatee".equalsIgnoreCase(crtCounty)) {
								rowHtml = rowHtml.replaceAll("(?is)<a\\s.*?View&nbsp;amount&nbsp;paid.*?</a>", "View&nbsp;amount&nbsp;paid");
								rowHtml = rowHtml.replaceAll("(?is)<a\\s.*?View&nbsp;payment&nbsp;date.*?</a>", "View&nbsp;payment&nbsp;date");
								String address = "";
								if ("levy".equalsIgnoreCase(crtCounty))
									address = LEVY_ADDRESS;
								else
									address = MANATEE_ADDRESS;
								rowHtml = rowHtml.replaceAll("(?is)<a\\s.*?href=\\\"(.*?)\\\".*?>\\s*View&nbsp;amount&nbsp;due\\s*</a>",
										"<a href=\"" + address + "$1\">View&nbsp;amount&nbsp;due</a>");
								rowHtml = rowHtml.replaceAll("(?is)<a\\s.*?\\:\\s*Redeemed.*?>\\s*Click for Details\\s*</a>", "Redeemed");
								rowHtml = rowHtml.replaceAll("(?is)<a\\s.*?\\:\\s*\\$([\\d,\\.]+).*?>\\s*Click for Details\\s*</a>", "$1");
								rowHtml = rowHtml.replaceAll("View&nbsp;amount&nbsp;paid", "<a href=\"" + link + "\"> View&nbsp;amount&nbsp;paid </a>");
								rowHtml = rowHtml.replaceAll("View&nbsp;payment&nbsp;date", "<a href=\"" + link + "\"> View&nbsp;payment&nbsp;date </a>");
							} else {
								if (!"manatee".equalsIgnoreCase(crtCounty))
									rowHtml = rowHtml.replaceAll("(?is)</?a[^>]*>", "");
							}
							if ("manatee".equalsIgnoreCase(crtCounty))
								rowHtml = rowHtml.replaceAll("(?is)<aaa href=", "<a href=");
							if (!"manatee".equalsIgnoreCase(crtCounty))
								rowHtml = rowHtml.replaceAll("(?is)</tr>", "<td align=\"center\"><a href=\"" + link + "\"> VIEW </a></td></tr>");
							rowHtml = rowHtml.replaceAll("(?is)<td class=\"col(Center|Right)\">", "<td align=\"center\">");
							ParsedResponse currentResponse = new ParsedResponse();
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
							currentResponse.setOnlyResponse(rowHtml);
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

							ResultMap m = parseIntermediaryRow(row, searchId, miServerID);
							m.removeTempDef();
							Bridge bridge = new Bridge(currentResponse, m, searchId);

							DocumentI document = (TaxDocumentI) bridge.importData();
							currentResponse.setDocument(document);

							intermediaryResponse.add(currentResponse);
						}
					}
				}
				mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsDetails:", paramsLink);

				String result = response.getResult();
				result = result.replaceAll("(?is)name\\s*=([A-Z]+)\\s+", "name=\"$1\" ");

				response.getParsedResponse().setHeader(
						"&nbsp;&nbsp;&nbsp;" + "<br><br>" + table.substring(table.indexOf("<table"), table.indexOf(">") + 1)
								+ tableHeader.replaceAll("(?is)</?a[^>]*>", ""));
				response.getParsedResponse().setFooter("</table><br><br>");

				outputTable.append(table);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	protected abstract String getPinFromLink(String linkAO);
	
	protected int getActionColumnIndex() {
		int actionColumnIndex = 10;
		return actionColumnIndex;
	}

	protected ResultMap parseIntermediaryRow(TableRow row, long searchId, int miServerId) throws Exception {
		return getParser().parseIntermediaryRow(row, searchId);
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		getParser().parseDetails(detailsHtml, searchId, map);
		return null;
	}

	/**
	 * get file name from link
	 */
	@Override
	protected String getFileNameFromLink(String link) {
		String fileName = "File.html";
		Matcher dummyMatcher = dummyPattern.matcher(link);
		if (dummyMatcher.find()) {
			fileName = dummyMatcher.group(1);
			fileName = fileName + ".html";
		}
		return fileName;
	}
	
	protected abstract ro.cst.tsearch.servers.functions.FLGenericPacificBlueTR getParser();

}
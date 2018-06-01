package ro.cst.tsearch.servers.types;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author mihaib
 */

public class TNRutherfordTR extends TSServerAssessorandTaxLike {

	public static final long				serialVersionUID	= 10000000L;
	private boolean							downloadingForSave;

	private static final Pattern			dummyPattern		= Pattern
																		.compile("&dummy=([0-9]+)&");

	public static final Map<String, String>	parameters;
	static {
		parameters = new HashMap<String, String>();
		parameters.put("ctl00$MainContent$SkeletonCtrl_8$txtFirstName", "");
		parameters.put("ctl00$MainContent$SkeletonCtrl_8$txtLastName", "");
		parameters.put("ctl00$MainContent$SkeletonCtrl_8$txtDayPhone", "");
		parameters.put("ctl00$MainContent$SkeletonCtrl_8$txtEmail", "");
		parameters.put("ctl00$MainContent$SkeletonCtrl_8$txtAddress", "");
		parameters.put("ctl00$MainContent$SkeletonCtrl_8$txtCity", "");
		parameters.put("ctl00$MainContent$SkeletonCtrl_8$drpState", "AL");
		parameters.put("ctl00$MainContent$SkeletonCtrl_8$txtZip", "");
	}

	public TNRutherfordTR(long searchId) {
		super(searchId);
	}

	public TNRutherfordTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String streetNo = org.apache.commons.lang.StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_STREETNO));
		String streetDir = org.apache.commons.lang.StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_STREETDIRECTION_ABBREV));
		String streetName = org.apache.commons.lang.StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_STREETNAME));
		String streetSu = org.apache.commons.lang.StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_STREETSUFIX));
		GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter(searchId, 0.8d);
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
		((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
		
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId, true, numberOfYearsAllowed, true);
		

		if (hasPin()) {
			// Search by PIN
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.getFunction(3).forceValue("All");
			module.addFilter(taxYearFilter);
			modules.add(module);

		}

		if (hasStreet()) {
			String firstArrangementOfAddress = "";
			if (StringUtils.isNotEmpty(streetNo)) {
				firstArrangementOfAddress = streetNo + " ";
			}
			if (StringUtils.isNotEmpty(streetDir)) {
				firstArrangementOfAddress += streetDir + " " + streetName + " "
						+ org.apache.commons.lang.StringUtils.defaultString(Normalize.translateSuffix(streetSu));
			} else {
				firstArrangementOfAddress += streetName + " "
						+ org.apache.commons.lang.StringUtils.defaultString(Normalize.translateSuffix(streetSu.trim()));
			}
			firstArrangementOfAddress = firstArrangementOfAddress.trim();
			
			// Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(firstArrangementOfAddress);
			module.getFunction(1).forceValue("Property Address");
			module.getFunction(2).forceValue("Both");
			module.getFunction(3).forceValue("All");
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(taxYearFilter);
			modules.add(module);

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.getFunction(3).forceValue("All");
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(taxYearFilter);
			modules.add(module);
		}

		if (hasOwner()) {
			// Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(taxYearFilter);
			module.getFunction(3).forceValue("All");
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.
					getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
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

			StringBuilder outputTable = new StringBuilder();

			if (rsResponse.indexOf("No Records Found") != -1) {
				Response.getParsedResponse().setError("<font color=\"red\">No results found.</font>");
				return;
			} else if (rsResponse.indexOf("Too many records returned") != -1) {
				Response.getParsedResponse().setError("<font color=\"red\">Refine the search. Too many results.</font>");
				return;
			} else if (rsResponse
					.indexOf("Too many records returned. Please narrow your search by referring to the Search Tips box") != -1) {
				Response.getParsedResponse()
						.setError("<font color=\"red\">Too many records returned. Please narrow your search by referring to the Search Tips box.</font>");
				return;
			}
			try {
				NodeList mainList = new HtmlParser3(Tidy.tidyParse(rsResponse, null)).getNodeList();
				Node n;
				if ((n = HtmlParser3.getNodeByID("ctl00_MainContent_SkeletonCtrl_8_pnlSearch", mainList, true)) != null)
					contents = n.toHtml();
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

			if (StringUtils.isEmpty(details)) {
				parsedResponse.setError("<font color=\"red\">Official Site Error! Please try again!");
				return;
			}

			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
				NodeList mainList = htmlParser.parse(null);
				NodeList pinList = mainList
						.extractAllNodesThatMatch(new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_8_lblAccountNumber"));
				if (pinList.size() > 0) {
					Node pinNode = pinList.elementAt(0);
					if (pinNode != null) {
						pid = pinNode.toPlainTextString().trim();
					}
				} else {
					String queryCache = StringEscapeUtils
							.unescapeHtml(URLDecoder.decode(Response.getQuerry() + "&", "UTF-8"));
					Matcher m = Pattern.compile("(?is)tmpAccountNo=([^&]*)").matcher(queryCache);
					if (m.find()) {
						pid = URLDecoder.decode(m.group(1), "UTF-8");
					}
				}
				Node taxYearNode = HtmlParser3.getNodeByID("ctl00_MainContent_SkeletonCtrl_8_lblTaxYear", mainList, true);

				if (taxYearNode!=null) {
					taxYear = taxYearNode.toPlainTextString().trim();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			// // add account number to page
			// if (StringUtils.isNotEmpty(pid)) {
			// details += "<p><b> Account Number: </b>" + pid + "</p>";
			// }

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
			if (initialResponse.contains("Search For Additional Records")) {
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

	private static int	seq	= 0;

	protected synchronized static int getSeq() {
		return seq++;
	}

	protected String getDetails(String response) {

		// if from memory - use it as is
		if (!response.toLowerCase().contains("<html") && !response.contains("updatePanel|MainContent_SkeletonCtrl_8_upTab|")) {
			return response;
		}

		String contents = "";
		String pin = "";

		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(response);
			NodeList mainList = htmlParser3.getNodeList();
			Node contentsNode = HtmlParser3.getNodeByID("vOverview", mainList, true);
			if (contentsNode != null) {
				contents = contentsNode.toHtml();
			}
			NodeList pinList = mainList
					.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_SkeletonCtrl_8_lblAccountNumber"));

			if (pinList.size() > 0) {
				Node pinNode = pinList.elementAt(0);
				if (pinNode != null) {
					pin = pinNode.toPlainTextString().trim();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// get tax history
		if (StringUtils.isNotEmpty(pin)) {

			HTTPRequest req = new HTTPRequest(getBaseLink() + "/default.aspx", HTTPRequest.POST);
			req.setPostParameter("ctl00$MainContent$SkeletonCtrl_8$txtSearchParam", pin);
			req.setPostParameter("ctl00$MainContent$SkeletonCtrl_8$drpTaxYear", "All");
			req.setPostParameter("ctl00$MainContent$SkeletonCtrl_8$drpStatus", "Both");
			req.setPostParameter("ctl00$MainContent$SkeletonCtrl_8$drpSearchParam", "Account Number");
			req.setPostParameter("ctl00$MainContent$SkeletonCtrl_8$btnSearch", "Search");

			HTTPResponse res = null;
			HttpSite site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);
			try {
				res = site.process(req);
			} finally {
				HttpManager.releaseSite(site);
			}

			String resp = res.getResponseAsString();

			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
				NodeList mainList = htmlParser.parse(null);
				Node tableNode = HtmlParser3.getNodeByID("ctl00_MainContent_SkeletonCtrl_8_gvRecords", mainList, true);
				String table = "";
				if(tableNode!=null){
					table = tableNode.toHtml();
				}
				Map<String, String> params1 = ro.cst.tsearch.connection.http2.TNRutherfordTR.isolateParams(resp, "aspnetForm");

				params1.put("ctl00$MainContent$SkeletonCtrl_8$drpTaxYear", req.getPostFirstParameter("ctl00$MainContent$SkeletonCtrl_8$drpTaxYear"));
				params1.put("ctl00$MainContent$SkeletonCtrl_8$drpSearchParam", req.getPostFirstParameter("ctl00$MainContent$SkeletonCtrl_8$drpSearchParam"));
				params1.put("ctl00$MainContent$SkeletonCtrl_8$txtSearchParam", req.getPostFirstParameter("ctl00$MainContent$SkeletonCtrl_8$txtSearchParam"));
				params1.put("ctl00$MainContent$SkeletonCtrl_8$drpStatus", req.getPostFirstParameter("ctl00$MainContent$SkeletonCtrl_8$drpStatus"));

				// save params
				int seq1 = getSeq();
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq1, params1);

				htmlParser = org.htmlparser.Parser.createParser(table, null);
				NodeList mainTableList = htmlParser.parse(null);
				NodeList tableList = mainTableList.extractAllNodesThatMatch(
						new TagNameFilter("table"), true);
				ArrayList<String> links = new ArrayList<String>();
				if (tableList.size() > 0) {
					TableTag mainTable = (TableTag) tableList.elementAt(0);
					mainTable.removeAttribute("id");
					mainTable.setAttribute("id", "tblTaxHistoryView");
					TableRow[] rows = mainTable.getRows();

					for (TableRow row : rows) {
						if (row.getColumnCount() == 12) {
							TableColumn[] cols = row.getColumns();

							NodeList inputList = cols[11].getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);
							if (inputList.size() > 0) {
								InputTag input = (InputTag) inputList.elementAt(0);
								String name = input.getAttribute("name");
								String value = input.getAttribute("value");
								links.add(getBaseLink() + "/default.aspx?seq=" + seq1 + "&" + name + "=" + value);
							}
						}
					}

				}
				if (!links.isEmpty()) {
					String tableTaxHistory = "<table id=\"tblTaxHistoryView\" border=\"1\" cellpadding=\"0\" cellspacing=\"0\" style=\"width: 100%\">"
							+ "<tr><td>Tax Year</td><td>Receipt</td><td>Paid Date</td><td>Paid Amount</td><td>Payment Status</td></tr>";

					for (String link : links) {

						String[] parts = link.split("[\\?&]");

						if (parts.length == 3) {
							String seq = link.split("[\\?&]")[1];
							String param1 = link.split("[\\?&]")[2];

							if (seq.contains("="))
								seq = seq.split("=")[1];

							req = new HTTPRequest(getBaseLink() + "/default.aspx", HTTPRequest.POST);
							req.setPostParameter("seq", seq);
							if (param1.contains("="))
								req.setPostParameter(param1.split("=")[0], param1.split("=")[1]);

							res = null;
							site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);
							try {
								res = site.process(req);
							} finally {
								HttpManager.releaseSite(site);
							}

							resp = res.getResponseAsString();
							htmlParser = org.htmlparser.Parser.createParser(resp, null);
							mainList = htmlParser.parse(null);

							try {
								Node paidDateNode = HtmlParser3.getNodeByID("ctl00_MainContent_SkeletonCtrl_8_lblDatePaid", mainList,true);
								String paidDate = "";	
								if(paidDateNode!=null){
									paidDate = paidDateNode.toPlainTextString().trim();
									}	
								Node amountPaidNode = HtmlParser3.getNodeByID("ctl00_MainContent_SkeletonCtrl_8_lblPaidAmount", mainList,true);
								String amountPaid = "";	
								if(amountPaidNode!=null){
									amountPaid = amountPaidNode.toPlainTextString().trim();
									}	
								Node receiptNode = HtmlParser3.getNodeByID("ctl00_MainContent_SkeletonCtrl_8_lblReceiptNumber", mainList,true);
								String receipt = "";	
								if(receiptNode!=null){
									receipt = receiptNode.toPlainTextString().trim();
									}	
								Node paymentStatusNode = HtmlParser3.getNodeByID("ctl00_MainContent_SkeletonCtrl_8_lblPaymentStatus", mainList,true);
								String paymentStatus = "";	
								if(paymentStatusNode!=null){
									paymentStatus = paymentStatusNode.toPlainTextString().trim();
									}
								Node taxYearNode = HtmlParser3.getNodeByID("ctl00_MainContent_SkeletonCtrl_8_lblTaxYear", mainList,true);
								String taxYear = "";	
								if(taxYearNode!=null){
									taxYear = taxYearNode.toPlainTextString().trim();
									}
								tableTaxHistory += "<tr><td>" + taxYear
										+ "</td><td>" + receipt
										+ "</td><td>" + paidDate
										+ "</td><td>" + amountPaid
										+ "</td><td>" + paymentStatus
										+ "</td></tr>";
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					tableTaxHistory += "</table>";
					if (StringUtils.isNotEmpty(tableTaxHistory)) {
						contents += "<h2>Tax History</h2>";
						contents += "<br>" + tableTaxHistory;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		contents = contents.replaceAll("(?is)</?body[^>]*>", "");
		contents = contents.replaceAll("(?is)</?form[^>]*>", "");
		contents = contents.replaceAll("(?is)(?is)<script.*?</script>", "");
		contents = contents.replaceAll("(?is)<input[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		contents = contents.replaceAll("(?is)<div[^>]*>\\s*</div>", "");
		contents = contents.replaceAll("(?is)(<select)\\s+", "$1 disabled=\"true\"");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)(<!--<\\s*/?\\s*br\\s*/?\\s*>)\\s*(<[^>]+>)", "$1" + "   -->  " + "$2");

		return contents.trim();
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {

			table = table.replaceAll("\n", "").replaceAll("\r", "");

			Map<String, String> params = ro.cst.tsearch.connection.http2.TNRutherfordTR.isolateParams(response.getResult(), "aspnetForm");

			// save params
			int seq = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);

			HtmlParser3 htmlParser3 = new HtmlParser3(table);
			Node mainTableNode = htmlParser3.getNodeById("ctl00_MainContent_SkeletonCtrl_8_gvRecords");

			if (mainTableNode!=null) {
				TableTag mainTable = (TableTag) mainTableNode;
				TableRow[] rows = mainTable.getRows();
				TableRow headerRow = rows[0];
				String tableHeader = headerRow.toHtml();

				for (TableRow row : rows) {
					if (row.getColumnCount() > 11) {

						TableColumn[] cols = row.getColumns();
						NodeList inputList = cols[11].getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);

						if (inputList.size() > 0) {
							InputTag input = (InputTag) inputList.elementAt(0);
							String name = input.getAttribute("name");
							String value = input.getAttribute("value");
							String link = CreatePartialLink(TSConnectionURL.idPOST) + "/taxes/default.aspx?seq=" + seq + "&" + name + "=" + value;

							String accountNo = "";

							if (cols[3].getChildren() != null) {
								accountNo = cols[3].getChildren().toHtml()
										.replaceAll("(?ism)value=\"([^\"]+)\"", ">$1<")
										.replaceAll("(?ism)<[^>]*>", "")
										.trim();
							}
							String rowHtml = row.toHtml() + "";
							rowHtml = rowHtml.replaceAll("(?is)<img[^>]+>", "");
							rowHtml = rowHtml.replaceAll("(?is)\\$", "@@@");
							link = link.replaceAll("(?is)\\$", "@@@");
							rowHtml = rowHtml.replaceAll("(?is)<input[^>]+value=\"([A-Z]*\\d+)\"[^>]+>", "$1");
							rowHtml = rowHtml.replaceAll("(?is)<input[^>]+>", "<a href=\"" + link + "\"> VIEW </a>");
							rowHtml = rowHtml.replaceAll("(?is)@@@", "\\$");
							link = link.replaceAll("(?is)@@@", "\\$");

							ResultMap m = new ResultMap();
							String ownerName = cols[0].getChildren().toHtml().trim();
							if (StringUtils.isNotEmpty(ownerName)) {
								m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ownerName);
							}
							String taxYear = cols[1].getChildren().toHtml().trim();
							if (StringUtils.isNotEmpty(taxYear)) {
								m.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
							}
							if (StringUtils.isNotEmpty(accountNo)) {
								m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNo);
							}
							String address = cols[8].getChildren().toHtml().trim();
							if (StringUtils.isNotEmpty(address)) {
								address = address.replaceAll("(?is)(.*?)\\s+(\\d+)$", "$2 $1");
								m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
								m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
							}
							String city = cols[9].getChildren().toHtml().trim();
							if (StringUtils.isNotEmpty(city)) {
								city = city.replaceAll("(?is)\\bN/A\\b", "");
								m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.trim());
							}
							parseNames(m);
							m.removeTempDef();
							m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

							ParsedResponse currentResponse = new ParsedResponse();
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
							currentResponse.setOnlyResponse(rowHtml);
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

							Bridge bridge = new Bridge(currentResponse, m, searchId);

							DocumentI document = (TaxDocumentI) bridge.importData();
							currentResponse.setDocument(document);

							intermediaryResponse.add(currentResponse);
						}
					}
				}

				String result = response.getResult();
				result = result.replaceAll("(?is)name\\s*=([A-Z]+)\\s+", "name=\"$1\" ");
				String mainTableString = mainTable.toHtml();
				response.getParsedResponse().setHeader("&nbsp;&nbsp;&nbsp;<br><br>"
								+ mainTableString.substring(mainTableString.indexOf("<table"), mainTableString.indexOf(">") + 1)
								+ tableHeader.replaceAll("(?is)</?a[^>]*>", ""));
				response.getParsedResponse().setFooter("</table><br><br>");

				outputTable.append(table);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", "")
				.replaceAll("(?is)&amp;", "&").replaceAll("(?is)<th\\b", "<td")
				.replaceAll("(?is)</th\\b", "</td")
				.replaceAll("(?is)</?b>", "")
				.replaceAll("(?is)</?font[^>]*>", "");
		map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			NodeList mainList = htmlParser3.getNodeList();
			String pin = "";

			Node pinNode = htmlParser3.getNodeById("ctl00_MainContent_SkeletonCtrl_8_lblAccountNumber");
			if (pinNode != null) {
				pin = pinNode.toPlainTextString().trim();
			} else {
				Matcher m = Pattern.compile("Account Number: ([A-Z]\\d+)").matcher(detailsHtml);
				if (m.find()) {
					pin = m.group(1);
				}
			}

			if (StringUtils.isNotEmpty(pin)) {
				map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pin);
			}
			String address = "";
			Node addressNode = htmlParser3.getNodeById("ctl00_MainContent_SkeletonCtrl_8_lblPropertyAddress");
			if (addressNode != null) {
				address = addressNode.toPlainTextString().trim();
				if (StringUtils.isNotEmpty(address)) {
					map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
					map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				}
			}

			String ownerName = "";
			Node ownerNameNode = htmlParser3.getNodeById("ctl00_MainContent_SkeletonCtrl_8_lblOwnerName");
			
			if (ownerNameNode != null) {
				ownerName = ownerNameNode.toPlainTextString().trim();
				if (StringUtils.isNotEmpty(ownerName)) {
					map.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ownerName);
				}
			}
			
			String taxYear = "";
			Node taxYearNode = htmlParser3.getNodeById("ctl00_MainContent_SkeletonCtrl_8_lblTaxYear");
			if (taxYearNode != null) {
				taxYear = taxYearNode.toPlainTextString().trim();
				if (StringUtils.isNotEmpty(taxYear)) {
					map.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
				}
			}
			String baseAmount = "";
			Node baseAmountNode = htmlParser3.getNodeById("ctl00_MainContent_SkeletonCtrl_8_lblBaseAmount");
			
			if (baseAmountNode != null) {
				baseAmount = baseAmountNode.toPlainTextString().replaceAll("[ $,-]", "").trim();
				if (StringUtils.isNotEmpty(baseAmount)) {
					map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
				}
			}

			String amountPaid = "";
			Node amountPaidNode = htmlParser3.getNodeById("ctl00_MainContent_SkeletonCtrl_8_lblPaidAmount");
			
			if (amountPaidNode != null) {
				amountPaid = amountPaidNode.toPlainTextString().replaceAll("[ $,-]", "").trim();
				if (StringUtils.isNotEmpty(amountPaid)) {
					map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
				}
			}
			
			String amountDue = "";
			Node amountDueNode = htmlParser3.getNodeById("ctl00_MainContent_SkeletonCtrl_8_lblTotalDue");
			
			if (amountDueNode != null) {
				amountDue = amountDueNode.toPlainTextString().replaceAll("[ $,-]", "").trim();
				if (StringUtils.isNotEmpty(amountDue)) {
					map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
				}
			}

			String datePaid = "";
			Node datePaidNode = htmlParser3.getNodeById("ctl00_MainContent_SkeletonCtrl_8_lblDatePaid");
			
			if (datePaidNode != null) {
				datePaid = datePaidNode.toPlainTextString().trim();
				if (StringUtils.isNotEmpty(datePaid)) {
					map.put(TaxHistorySetKey.DATE_PAID.getKeyName(), datePaid);
				}
			}

			NodeList taxHistTableList = mainList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "tblTaxHistoryView"));
			if (taxHistTableList.size() > 0)
			{
				String taxHistTable = taxHistTableList.elementAt(0).toHtml().trim();
				List<List<String>> taxTable = HtmlParser3.getTableAsList(taxHistTable, false);
				List<List> bodyTaxes = new ArrayList<List>();
				List<String> line = null;
				StringBuilder priorDelinquentSB = new StringBuilder("0.00");
				for (List list : taxTable) {
					line = new ArrayList<String>();
					if (list.size() > 4
							&& list.get(4).toString().trim().toLowerCase().equals("paid")) {
						line.add(list.get(1).toString().trim());
						line.add(list.get(2).toString().trim());
						line.add(list.get(3).toString().replaceAll("[\\$,]", "").trim());
						bodyTaxes.add(line);
						if (StringUtils.isNotEmpty(taxYear)) {
							int currentTaxYear = Integer.parseInt(taxYear.trim());
							if (StringUtils.isNotEmpty(list.get(0).toString().trim())) {
								int currentLineYear = Integer.parseInt(list.get(0).toString().trim());
								if (currentTaxYear > currentLineYear
										&& list.get(4).toString().trim().toLowerCase().equals("unpaid")) {
									priorDelinquentSB.append("+").append(list.get(3).toString().replaceAll("[\\$,]", "").trim());
								}
							}
						}
					}
				}

				if (bodyTaxes != null) {
					if (!bodyTaxes.isEmpty()) {
						ResultTable rt = new ResultTable();
						String[] header = { "ReceiptNumber", "ReceiptDate", "ReceiptAmount" };
						rt = GenericFunctions2.createResultTable(bodyTaxes, header);
						map.put("TaxHistorySet", rt);
					}
				}
				map.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), GenericFunctions2.sum(priorDelinquentSB.toString(), searchId));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			parseNames(map);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	
	public static void parseNames(ResultMap m) throws Exception {
		
		String s = "";
		s = (String) m.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());

		if (StringUtils.isEmpty(s)) {
			return;
		}
		
		s = s.replaceAll("&amp;", "&");
		s = s.replaceAll("\\bETA\\b", "ETAL");
		s = s.replaceAll("\\bMRS\\b", "");
		s = s.replaceAll("[\\(\\)]+", "");
		s = s.replaceAll("\\bATT(ENTIO)?N:?\\s*:?\\b", "");
		s = s.replaceAll("\\s+\\(LE\\)\\s+", " ");
		s = s.replaceAll("(?is)\\A\\s*<\\s*br\\s*/?\\s*>", "");
		s = s.replaceAll("(?is)\\s*<\\s*br\\s*/?\\s*>\\s*(ETVIR|ETAL|ETUX)\\b", " $1");
		s = s.replaceAll("(?is)\\b(ETVIR|ETUX)\\s*<\\s*br\\s*/?\\s*>\\s*", "$1 ");
		s = s.replaceAll("(?is)E\\s*<\\s*br\\s*/?\\s*>\\s*", "ETUX ");
		s = s.replaceAll("(?is)\\b\\s*LE\\s*\\b", " LIFE ESTATE ");

		String[] owners;
		owners = s.split("(?is)<\\s*br\\s*/?\\s*>");
		

		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];
			ow = ow.replaceAll("U/D/T", "");
			if (NameUtils.isCompany(ow)) {
				names[2] = ow;
			} else {
				String patt = "(?is)^\\s*C\\s*/\\s*O\\s+";
				if (i>0 || ow.matches(patt + ".+")) {
					ow = ow.replaceFirst(patt, "");
					names = StringFormats.parseNameDesotoRO(ow, true);
				} else {
					names = StringFormats.parseNameNashville(ow, true);
				}
			}

			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions1.extractNameSuffixes(names);

			if (ow.matches(StringEscapeUtils.escapeJava(".*" + names[2] + " I"))) {// WILLIAM L PATTERSON I; escape for ex. C\O LEE ROBERTS JR I ;
				names[1] = names[1].replaceAll("(.*)I\\z", "$1").trim();
				suffixes[0] = "I";
			}

			GenericFunctions1.addOwnerNames(names, suffixes[0], suffixes[1],
					type, otherType, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions1.storeOwnerInPartyNames(m, body, true);
	}

}
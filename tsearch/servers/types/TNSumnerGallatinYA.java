package ro.cst.tsearch.servers.types;

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
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
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
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class TNSumnerGallatinYA extends TSServer {

	private static final long	serialVersionUID	= -7276556168222510753L;
	public static final String	FORM_NAME			= "aspnetForm";

	public TNSumnerGallatinYA(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {

		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult().replaceAll("&nbsp;", " ").replaceAll("&\\s*amp;", "&");
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:

			// no results found
			if (rsResponse.contains("No Parcels found based on specified search criteria")) {
				parsedResponse.setError("No results found for your query! Please change your search criteria and try again.");
				return;
			}

			if (rsResponse.contains("Bill Year")) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				if (smartParsedResponses.size() == 0) {
					return;
				}

				parsedResponse.setHeader("<table style='border-collapse: collapse' border='2' width='80%' align='center'>" + parsedResponse.getHeader());
				parsedResponse.setFooter("</table>");
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}

			break;
		case ID_GET_LINK:
			if (rsResponse.contains("Bill Year")) {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
		case ID_DETAILS:
			if (rsResponse.contains("No Parcels found based on specified search criteria")) {
				parsedResponse.setError("No results found for your query! Please change your search criteria and try again.");
				return;
			}
		case ID_SAVE_TO_TSD:
			// get accoundId and year
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			String year = "";
			String accountId = "";
			Node node = htmlParser3.getNodeById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_CategoryLabel");
			if (node != null) {
				accountId = node.toPlainTextString().replaceAll("\\s+", "");
			}
			node = htmlParser3.getNodeById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_FiscalYearLabel");
			if (node != null) {
				year = node.toPlainTextString().trim();
			}

			String details = getDetails(rsResponse, year);
			if (StringUtils.isEmpty(details)) {
				parsedResponse.setError("Site error.");
				return;
			}

			if (viParseID == ID_DETAILS) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				data.put("year", year);

				if (isInstrumentSaved(accountId, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);
			} else {
				String filename = accountId + ".html";
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				parsedResponse.setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			break;
		}
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String page, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();

		try {
			String siteType = getDataSite().getSiteTypeAbrev();
			HtmlParser3 htmlParser = new HtmlParser3(page);
			NodeList nodeList = htmlParser.getNodeList();

			Form form = new SimpleHtmlParser(page).getForm(FORM_NAME);
			Map<String, String> params = form.getParams();
			int seq = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);

			TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(
					new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_MolGridView1"), true)
					.elementAt(0);
			if (interTable == null) {
				return intermediaryResponse;
			}

			TableRow[] rows = interTable.getRows();
			for (int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				String rowText = row.toPlainTextString();

				if (rowText.contains("Bill Type")) {
					// table header
					parsedResponse.setHeader(row.toHtml().replaceAll("(?is)<a [^>]*>(.*?)</a>", "$1"));
					continue;
				}

				LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
				if (linkTag == null) {
					continue;
				}
				processIntermediaryLink(linkTag, form.action, seq);

				String htmlRow = removeWrongSpaces(row.toHtml());

				String detailsLink = linkTag.getLink();
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
				currentResponse.setOnlyResponse(htmlRow);
				currentResponse.setPageLink(new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD));

				ResultMap m = ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.parseIntermediaryRow(htmlRow);
				m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), siteType);
				Bridge bridge = new Bridge(currentResponse, m, searchId);

				DocumentI document = (TaxDocumentI) bridge.importData();
				currentResponse.setDocument(document);

				intermediaryResponse.add(currentResponse);
			}
			outputTable.append(page);
		} catch (Exception e) {
			logger.error("Error while parsing intermediary results", e);
		}

		return intermediaryResponse;
	}

	/*
	 * remove all spaces at certain indexes (multiples of 15) e.g. "331 EAST EASTLA ND STREET" or "REGULAR/ORIGINA L - REAL ESTA TE"
	 */
	protected String removeWrongSpaces(String htmlRow) {

		StringBuilder correctedRow = new StringBuilder();
		String tempHtmlRow = htmlRow.replaceAll("(?is)\\s*</?tr[^>]*>\\s*", "")
				.replaceAll("(?is)(</td>)", "$1@@@@@");
		String[] rowColumns = tempHtmlRow.split("\\s*@@@@@\\s*");
		if (rowColumns.length >= 6) {
			correctedRow.append("<tr>");

			// process address cell
			rowColumns[0] = rowColumns[0].replaceAll("(?is)(<td[^>]*>[^<]{15})\\s+([^<]*</td>)", "$1$2");

			// process owner cell
			rowColumns[2] = rowColumns[2].replaceAll("(?is)(<td[^>]*>[^<]{15})\\s+(\\w{1,4}\\b[^<]*</td>)", "$1$2");

			// sometimes these split spaces are right where a space should be, so we correct these errors here
			// e.g. "ALLEN CHARLES H IV", "GREEN HERBERT WETUX"
			rowColumns[2] = rowColumns[2].replaceFirst(
					"(?is)\\b([A-Z])(I{1,3}|I{0,1}V|VI{1,3}|I{0,1}X|XI{1,3}|ET\\s*(?:UX|AL)|JR|SR|DR|\\d+(?:ST|ND|RD|TH))\\s*(</td>)",
					"$1 $2$3");

			// process bill type cell
			rowColumns[5] = rowColumns[5].replaceAll("(?is)(<td[^>]*>[^<]{30})\\s+([^<]*</td>)", "$1$2")
					.replaceAll("(?is)(<td[^>]*>[^<]{15})\\s+([^<]*</td>)", "$1$2");

			for (int i = 0; i < rowColumns.length; i++) {
				correctedRow.append(rowColumns[i]);
			}
			correctedRow.append("</tr>");
		} else {
			correctedRow.append(htmlRow);
		}
		return correctedRow.toString();
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		String city = org.apache.commons.lang.StringUtils.defaultString(getSearchAttribute(SearchAttributes.P_CITY).toUpperCase()).trim();
		String dataSiteCity = org.apache.commons.lang.StringUtils.defaultString(getDataSite().getCityName().toUpperCase()).trim();
		if (StringUtils.isEmpty(city) || (StringUtils.isNotEmpty(city) && !city.startsWith(dataSiteCity))) {
			return;
		}

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);

		if (hasPin()) {
			String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO_GENERIC_TR).trim().replaceAll("\\.|--", "");
			String pidEquivalent = "";
			Pattern pattern = Pattern.compile("^(\\w+)-(\\w)-\\1-(\\w+)(.+)");
			Matcher matcher = pattern.matcher(pid);
			if (matcher.find()) {// equiv for PRI/AO/TR pid (e.g. for 126K-C-126K-011.00--000 or 164L-B-014.00-
				// the equiv pid would be 126K-C-01100000 and 164L-B-01400 respectively
				pidEquivalent = matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3) + matcher.group(4).replaceAll("[.-]", "");
			} else if (pid.matches("\\w+-\\w+-\\w+") || pid.matches(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.YA_PID_PATTERN)) {
				// it's already the correct pid format for YA (Gallatin)
				pidEquivalent = pid;
			}

			if (StringUtils.isNotEmpty(pidEquivalent)) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, pidEquivalent);
				module.addFilter(taxYearFilter);
				modules.add(module);
			}
		}

		// search by Address
		if (hasStreet() && hasStreetNo()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
			module.getFunction(1).setSaKey(SearchAttributes.P_STREETNAME);

			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			module.addFilter(taxYearFilter);

			modules.add(module);
		}

		// search by Owner
		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			module.addFilter(taxYearFilter);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			module.addIterator(nameIterator);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	private void processIntermediaryLink(LinkTag linkTag, String formAction, int seq) {
		String processedLink = CreatePartialLink(TSConnectionURL.idPOST) + formAction + "&" + "seq=" + seq + "&";
		String initialLink = StringEscapeUtils.unescapeHtml(linkTag.getLink());
		Pattern p = Pattern.compile("(?is)__doPostBack[(]'([^']*)','([^']*)'[)]");
		Matcher m = p.matcher(initialLink);
		if (m.find()) {
			processedLink += "__EVENTTARGET=" + m.group(1) + "&";
			processedLink += "__EVENTARGUMENT=" + m.group(2) + "&";
		} else {
			initialLink = initialLink.replaceAll("\"", "'");
			p = Pattern.compile("(?is)_PostBackOptions[(]'([^']*)',\\s*'([^']*)'");
			m = p.matcher(initialLink);
			if (m.find()) {
				processedLink += "__EVENTTARGET=" + m.group(1);
			}
		}
		linkTag.setLink(processedLink);
	}

	private String getDetails(String response, String year) {
		StringBuilder details = new StringBuilder();
		try {

			// if the response is from memory
			if (!response.toLowerCase().contains("<html")) {
				return response;
			}
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			HtmlParser3 htmlParser3 = new HtmlParser3(response);

			NodeList nodeList = htmlParser3.getNodeList();
			NodeList linkList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "submenu"), true)
					.extractAllNodesThatMatch(new TagNameFilter("a"), true);

			for (int i = 0; i < linkList.size(); i++) {
				LinkTag linkTag = (LinkTag) linkList.elementAt(i);
				String link = linkTag.getLink();

				if (link.contains("ContactUs.aspx")) {
					break;
				}

				String htmlPage = getLinkContents(dataSite.getLink().replaceFirst("(?is)(.*?[^/])/[^/].*", "$1") + link);
				htmlParser3 = new HtmlParser3(htmlPage);
				NodeList nodeList1 = htmlParser3.getNodeList();

				if (link.contains("ViewBill")) {
					TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("id", "BillDetailTable"), true).elementAt(0);
					details.append("<h4 align=\"center\">View Bill</h4>");
					if (table != null) {
						table.setAttribute("class", ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.ALL_BILLS_DETAILS_CLASS);
						details.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table));
					}

					table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("class", "datatable"), true).elementAt(0);
					if (table != null) {
						table.setAttribute("class", ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.ALL_BILLS_CLASS);
						details.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table));
					}
				} else if (link.contains("TaxCharges")) {
					TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(
							new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_TaxChargesTable"), true).elementAt(0);
					details.append("<h4 align=\"center\">Charges</h4>");
					if (table != null) {
						details.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table));
					}

					table = (TableTag) nodeList1.extractAllNodesThatMatch(
							new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_TaxExemptionsTable"), true).elementAt(0);
					if (table != null) {
						details.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table));
					}

					table = (TableTag) nodeList1.extractAllNodesThatMatch(
							new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_TotalTaxTable"), true).elementAt(0);
					if (table != null) {
						details.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table));
					}
				} else if (link.contains("ParcelDetail")) {
					TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("id", "ParcelTable"), true).elementAt(0);
					if (table != null) {
						details.append("<h4 align=\"center\">Property Detail</h4>");
						details.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table));
					}
				} else if (link.contains("OwnerInformation")) {
					TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("class", "informationtable"), true).elementAt(0);
					if (table != null) {
						details.append("<h4 align=\"center\">Owner Information</h4>");
						details.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table, "OwnerTable"));
					}
				} else if (link.contains("Assessments")) {
					TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(new HasAttributeFilter("class", "datatable"), true).elementAt(0);
					if (table != null) {
						details.append("<h4 align=\"center\">Assessment</h4>");
						details.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table, "AssessmentTable"));
					}
				} else if (link.contains("AssessmentHistory")) {

					TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(
							new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_AssessmentHistoryGrid"), true).elementAt(0);
					if (table != null) {
						details.append("<h4 align=\"center\">Assessment History</h4>");
						details.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table));
					}
				} else if (link.contains("TaxRates")) {

					TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(
							new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_MolGridView1"), true).elementAt(0);
					if (table != null) {
						details.append("<h4 align=\"center\">Tax Rates</h4>");
						details.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table));
					}
				} else if (link.contains("AllBills")) {
					details.append(getAllBills(year, dataSite, htmlPage, nodeList1));
				}
			}
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}

		return details
				.toString()
				.replaceAll("(?is)<caption[^>]*>\\s*(<span[^>]*>(\\s|&nbsp;)*</span>)?\\s*(View\\s+Bill|Tax\\s+Charges|Assessment\\s+Values)\\s*</caption>", "")
				.replaceAll("(?is)<input[^>]*\\bvalue=\"(\\d+/\\d+/\\d+)\"[^>]*>", "$1");
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.parseAndFillResultMap(detailsHtml, resultMap, logger);
		return null;
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CITYTAX");
		}
	}

	protected String getAllBills(String year, DataSite dataSite, String htmlPage, NodeList nodeList1) {
		StringBuilder allBillsSB = new StringBuilder();

		TableTag table = (TableTag) nodeList1.extractAllNodesThatMatch(
				new HasAttributeFilter("id", "ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_BillsRepeater_ctl00_BillsGrid"), true)
				.elementAt(0);
		if (table == null) {
			return allBillsSB.toString();
		}
		TableRow[] rows = table.getRows();
		int billIndex = 0;
		boolean addedHeader = false; 
		
		Form form = new SimpleHtmlParser(htmlPage).getForm(FORM_NAME);
		Map<String, String> params = form.getParams();
		int seq = getSeq();
		mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);

		for (TableRow row : rows) {
			if (row.toPlainTextString().contains("View Bill")) {
				LinkTag billLinkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
				if (billLinkTag != null) {
					billIndex++;
					String billYear = row.getColumns()[2].toPlainTextString().trim();
					if (!year.isEmpty() && billYear.equals(year)) {
						// skip the current year
						continue;
					}

					processIntermediaryLink(billLinkTag, form.action, seq);
					String billLink = dataSite.getLink().replaceFirst("(?is)(.*?[^/])/[^/]*$", "$1") + "/"
							+ billLinkTag.getLink().replaceFirst(".*?&Link=", "");
					String[] linkParts = billLink.split("[?&]");

					HTTPRequest reqP = new HTTPRequest(linkParts[0]);
					for (int j = 1; j < linkParts.length; j++) {
						String part = linkParts[j];
						String[] tokens = part.split("=");
						reqP.setPostParameter(tokens[0], tokens.length > 1 ? tokens[1] : "");
					}
					reqP.setMethod(HTTPRequest.POST);
					HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
					String billPage = "";

					try {
						HTTPResponse resP = site.process(reqP);
						billPage = resP.getResponseAsString();
					} finally {
						HttpManager3.releaseSite(site);
					}

					if (StringUtils.isNotEmpty(billPage)) {
						HtmlParser3 htmlParser3 = new HtmlParser3(billPage);
						NodeList nodeList2 = htmlParser3.getNodeList();

						TableTag table2 = (TableTag) nodeList2.extractAllNodesThatMatch(new HasAttributeFilter("id", "BillDetailTable"), true)
								.elementAt(0);
						if (table2 != null) {
							if (!addedHeader) {
								allBillsSB.append("<h4 align=\"center\">All Bills</h4>");
								addedHeader = true;
							}
							table2.setAttribute("class", ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.ALL_BILLS_DETAILS_CLASS);
							table2.removeAttribute("id");
							allBillsSB.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table2));
						}

						table2 = (TableTag) nodeList2.extractAllNodesThatMatch(new HasAttributeFilter("class", "datatable"), true)
								.elementAt(0);
						if (table2 != null) {
							table2.setAttribute("class", ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.ALL_BILLS_CLASS);
							allBillsSB.append(ro.cst.tsearch.servers.functions.TNSumnerGallatinYA.tableToHtml(table2));
						}
					}
				}
			}
		}
		return allBillsSB.toString();
	}
}

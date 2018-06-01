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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.StringFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Tidy;

public class CAOrangeTR extends TSServer {

	private static final long	serialVersionUID	= 1L;

	public CAOrangeTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	protected static String				amountPattern				= "([^\\d.\\(\\)]+|\\.$)";
	protected static String				amountAbsoluteValuePattern	= "\\(([\\d.]+)\\)";

	private static ArrayList<String>	cities						= new ArrayList<String>();
	static {
		cities.add("Anaheim");
		cities.add("Aliso Viejo");
		cities.add("Balboa");
		cities.add("Balboa Island");
		cities.add("Buena Park");
		cities.add("Brea");
		cities.add("Capistrano Beach");
		cities.add("Corona del Mar");
		cities.add("Costa Mesa");
		cities.add("Coto de Caza");
		cities.add("Cypress");
		cities.add("Dana Point");
		cities.add("Dove Canyon");
		cities.add("El Toro");
		cities.add("Foothill Ranch");
		cities.add("Fountain Valley");
		cities.add("Fullerton");
		cities.add("Garden Grove");
		cities.add("Huntington Beach");
		cities.add("Irvine");
		cities.add("La Habra");
		cities.add("La Palma");
		cities.add("Ladera Ranch");
		cities.add("Laguna Beach");
		cities.add("Laguna Hills");
		cities.add("Laguna Niguel");
		cities.add("Laguna Woods");
		cities.add("Lake Forest");
		cities.add("Los Alamitos");
		cities.add("Midway City");
		cities.add("Mission Viejo");
		cities.add("Modjeska");
		cities.add("Newport Beach");
		cities.add("Newport Coast");
		cities.add("Orange");
		cities.add("Orange County");
		cities.add("Placentia");
		cities.add("Rancho Santa Margarita");
		cities.add("San Clemente");
		cities.add("San Juan Capistrano");
		cities.add("Santa Ana");
		cities.add("Seal Beach");
		cities.add("Silverado");
		cities.add("South Laguna");
		cities.add("Stanton");
		cities.add("Sunset Beach");
		cities.add("Trabuco Canyon");
		cities.add("Tustin");
		cities.add("Villa Park");
		cities.add("Westminster");
		cities.add("Yorba Linda");
	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		rsResponse = StringEscapeUtils.unescapeHtml(rsResponse.replaceAll("&nbsp;", " "))
				.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

		ParsedResponse parsedResponse = Response.getParsedResponse();

		mSearch.setAdditionalInfo("viParseID", viParseID);
		if (rsResponse.indexOf("The page cannot be displayed") > -1) {
			parsedResponse.setError("No Results Found!");
			parsedResponse.setResponse("");
			return;
		}
		int localParseID = viParseID;

		if (viParseID != ID_SAVE_TO_TSD && rsResponse.matches("(?is)Property Tax Information\\s+<br/>Fiscal Year \\d{4}\\s*-\\s*\\d{2,4}")) {
			localParseID = ID_DETAILS;
		}

		if (rsResponse.contains("click on the corresponding Find button below")) {
			parsedResponse.setError("No Results Found!");
			return;
		}

		switch (localParseID) {
		case ID_SEARCH_BY_ADDRESS:
		case ID_INTERMEDIARY:

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}
			if (smartParsedResponses.size() == 0) {
				return;
			}
			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			String details = getDetails(Response, rsResponse, accountId);

			if (details.isEmpty()) {
				return;
			}

			String accountName = accountId.toString();
			if (viParseID != ID_SAVE_TO_TSD) {

				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);

				if (isInstrumentSaved(accountName, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}
				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = accountName + ".html";
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				parsedResponse.setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;
		case ID_GET_LINK:
			Pattern pat = Pattern.compile("(?is)Search\\s+Results:[\\d\\s-]*");
			Matcher mat = pat.matcher(rsResponse);
			if (mat.find()) {
				ParseResponse(sAction, Response, ID_INTERMEDIARY);
			}
			else {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
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

	protected String getDetails(ServerResponse response, String rsResponse, StringBuilder accountId) {
		try {
			String testAccountId = "";

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {

				testAccountId = RegExUtils.getFirstMatch("(?is)<a[^>]*detail_sec.asp[^\"]*reqparcel[^\"]*\"[^>]*>\\s*([\\d-]+)\\s*</a>", rsResponse, 1);
				if (StringUtils.isNotEmpty(testAccountId)) {
					accountId.append(testAccountId);
				} else {
					testAccountId = RegExUtils.getFirstMatch("(?is)<td[^>]*>Parcel\\s+Number.*?:?\\s*([\\d-]+)\\s*</td>", rsResponse, 1);
					if (StringUtils.isNotEmpty(testAccountId)) {
						accountId.append(testAccountId);
					}
				}
				return rsResponse;
			}

			StringBuilder detailsSB = new StringBuilder();
			String detailsL1Contents = "";
			String detailsL1 = "";
			String detailsL2Contents1 = "";
			String detailsL2Contents2 = "";
			String tcrefLinkContents = "";
			String details = "";
			String link = "";
			String testString = "";
			NodeList nodes = null;
			NodeList div = null;

			String dataSiteLink = dataSite.getLink();
			dataSiteLink = dataSiteLink.substring(0, dataSiteLink.lastIndexOf("/") + 1);

			testString = RegExUtils.getFirstMatch("(?is)\\s+(on\\s+the\\s+parcel\\s+below\\s+to\\s+select\\s+a\\s+bill)\\s*:?", rsResponse, 1);
			if (StringUtils.isNotEmpty(testString)) {
				link = RegExUtils.getFirstMatch("(?is)<a[^>]*(detail_sec.asp[^\"]*reqparcel[^\"]*)\"[^>]*>", rsResponse, 1);
				if (StringUtils.isNotEmpty(link)) {
					detailsL1Contents = getLinkContents(dataSiteLink + link);

					// get accountID
					if (accountId.length() == 0) {
						testAccountId = RegExUtils.getFirstMatch("(?is)\\s*<td [^>]*>Parcel\\s+No.\\s*</td>\\s*<td[^>]*>\\s*([\\d-]+)", detailsL1Contents, 1);
						if (StringUtils.isNotEmpty(testAccountId)) {
							accountId.append(testAccountId);
						}
					}

					detailsL1 = RegExUtils.getFirstMatch("(?is)<!--\\s*BEGIN:\\s*Col\\s*2\\s*-->(.+?)<!--\\s*END:\\s*Col\\s*2\\s*-->", detailsL1Contents, 1)
							.trim();
					if (StringUtils.isEmpty(detailsL1)) {
						detailsL1 = RegExUtils.getFirstMatch(
								"(?is)(<div[^>]*class=\"edtdiv html-width-default\"[^>]*>\\s*<table[^>]*>.*?</table>\\s*</div>)", detailsL1Contents, 1).trim();
					}

					if (StringUtils.isNotEmpty(detailsL1)) {
						detailsL1 = detailsL1.replaceFirst("(?is)(^<td[^>]*>|</td>$)", "");
						detailsSB.append("<tr id=\"detailsL1Row\"><td>" + detailsL1 + "</td></tr>");
					}

					// get first details L2(Click here for detailed infromation..)
					link = RegExUtils.getFirstMatch("(?is)<a [^>]*'(tax_details.asp[^>']*)'[^>]*>.*?Click\\s+here\\s+for\\s+detailed\\s+information.*?</a>",
							detailsL1, 1);
					if (StringUtils.isNotEmpty(link)) {
						detailsL2Contents1 = getLinkContents(dataSiteLink + link);

						nodes = new HtmlParser3(detailsL2Contents1).getNodeList();
						nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);
						TableTag detailsInfo = (TableTag) nodes.elementAt(0);
						detailsInfo.setAttribute("width", "100%");
						if (nodes.size() > 0) {
							detailsSB.append("<tr><td><b>Detailed Information</b></td></tr><tr id=\"detailsL2Row\"><td>"
									+ detailsInfo.toHtml() + "</td></tr>");
						}
					}
				}

				// get tdn details..
				testString = RegExUtils.getFirstMatch(
						"(?is)(Please\\s+select\\s+the\\s+tax\\s+record\\s+by\\s+clicking\\s+on\\s+the\\s+Tax\\s+Default\\s+N)", rsResponse, 1);
				if (StringUtils.isNotEmpty(testString)) {
					String tdnLink = RegExUtils.getFirstMatch("(?is)<a[^>]*href=\"(web_tdn_plan\\.asp\\?.*?)\"", rsResponse, 1);
					if (StringUtils.isNotEmpty(tdnLink)) {
						String tdnLinkContents = getLinkContents(dataSiteLink + tdnLink);
						String tdnParcelLinksPat = "(?is)<a[^>]*(?:'|\")(py_details.asp[^>]*TDN=[^'\"]*)(?:'|\")[^>]*>([\\s\\d-]+)(?:\\.\\d+)?</a>";

						if (accountId.length() == 0) {
							testAccountId = RegExUtils.getFirstMatch(tdnParcelLinksPat, tdnLinkContents, 2);
							if (StringUtils.isNotEmpty(testAccountId)) {
								accountId.append(testAccountId);
							}
						}

						nodes = new HtmlParser3(Tidy.tidyParse(tdnLinkContents, null)).getNodeList();
						div = nodes.extractAllNodesThatMatch(new HasAttributeFilter("class", "edtdiv html-width-default"), true);
						detailsSB.append("<tr class=\"tdnDetails\"><td>" + div.toHtml() + "</td></tr>");

						Pattern tdnParcelLinksPattern = Pattern.compile(tdnParcelLinksPat);
						Matcher tdnParcelLinksMatcher = tdnParcelLinksPattern.matcher(tdnLinkContents);

						int i = 0;
						while (tdnParcelLinksMatcher.find()) {
							i = i + 1;
							String detailTDNLinkContents = getLinkContents(dataSiteLink + tdnParcelLinksMatcher.group(1));

							nodes = new HtmlParser3(detailTDNLinkContents).getNodeList();
							nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);
							TableTag detailsTTag = (TableTag) nodes.elementAt(0);
							detailsTTag.setAttribute("width", "100%");

							if (nodes.size() > 0) {
								detailsSB.append("<tr class=\"tdnDetails\"><td>" + detailsTTag.toHtml() + "</td></tr>");
							}
						}
					}
				}

				// tcref or tax year and assessment no direct search
				testString = RegExUtils.getFirstMatch("(ACCOUNT\\s+SUMMARY)", rsResponse, 1);
				if (StringUtils.isNotEmpty(testString)) {
					nodes = new HtmlParser3(Tidy.tidyParse(rsResponse, null)).getNodeList();
					div = nodes.extractAllNodesThatMatch(new HasAttributeFilter("class", "edtdiv html-width-default"), true);
					detailsSB.append("<tr class=\"tcrefDetails\"><td>" + div.toHtml() + "</td></tr>");
				}

				// tcref on search by parcel; from intermediary
				testString = RegExUtils.getFirstMatch(
						"(?is)(Please\\s+select\\s+the\\s+tax\\s+record\\s+by\\s+clicking\\s+on\\s+the\\s+Tax\\sCollector\\s+Reference\\s+)", rsResponse, 1);
				if (StringUtils.isNotEmpty(testString)) {
					link = RegExUtils.getFirstMatch(
							"(?is)<a href=\"[^\"]*(tcref_list.asp.*?)\"[^>]*>.*?</a>", rsResponse, 1);
					if (StringUtils.isNotEmpty(link)) {
						tcrefLinkContents = getLinkContents(dataSiteLink + link);
						nodes = new HtmlParser3(Tidy.tidyParse(tcrefLinkContents, null)).getNodeList();
						div = nodes.extractAllNodesThatMatch(new HasAttributeFilter("class", "edtdiv html-width-default"), true);
						detailsSB.append("<tr class=\"tcrefDetails\"><td>" + div.toHtml() + "</td></tr>");
					}
				}

				// get supplemental results
				// from here it's possible that nothing's found anymore.. site changed and some modules don't seem to work(TCREF)
				Pattern p = Pattern.compile("(?is)<nobr[^>]*>\\s*<a[^>]*href=\"(detail_sup.asp[^\"]*)\"[^>]*>.*?</a>\\s*</nobr>");
				Matcher m = p.matcher(rsResponse);

				while (m.find()) {
					detailsL2Contents2 = getLinkContents(dataSiteLink + m.group(1));

					// get accountID
					if (accountId.length() == 0) {
						testAccountId = RegExUtils.getFirstMatch("(?is)\\s*<td[^>]*>Parcel\\s+No.\\s*</td>\\s*<td[^>]*>\\s*([\\d-]+)", detailsL2Contents2, 1);
						if (StringUtils.isNotEmpty(testAccountId)) {
							accountId.append(testAccountId);
						}
					}

					nodes = new HtmlParser3(Tidy.tidyParse(detailsL2Contents2, null)).getNodeList();
					div = nodes.extractAllNodesThatMatch(new HasAttributeFilter("class", "edtdiv html-width-default"), true);
					if (div.size() > 0) {
						detailsSB.append("<tr><td><b>Supplemental Details</b></td></tr>");
						detailsSB.append("<tr><td id=\"detailsSupplemental\">" + div.toHtml() + "</td></tr>");
					}
				}
			}

			if (StringUtils.isNotEmpty(detailsSB.toString())) {
				details = "<table id=\"detailsCAOrangeTR\" width = \"710px\" align=\"center\" border=\"1px\">" + detailsSB.toString() + "</table>";
			}

			details = details.replaceAll("&nbsp;", " ")
					.replaceAll("((?is)class=\\\".*?\\\")|((?is)class='.*?')", "class=\"details\"")
					.replaceAll("(?is)<a[^>]*>\\s*close\\s*</a>", "")
					.replaceAll("(?is)</?a[^>]*>", "")
					.replaceAll("(?is)view\\s+(original|\\d{4})\\s+bill", "")
					.replaceFirst("(?is)\\(see\\s+bill\\s+Disclaimer\\)", "")
					.replaceAll("(?is)<input[^>]*>", "")
					.replaceAll("(?is)<i>\\s*click\\s+here\\s+for\\s+detail\\s*.*?</i>", "")
					.replaceAll("(?is)<b>\\s*Click\\s+on\\s+one\\s+of\\s+the\\s+button\\s+selections\\s+below.*?</b>", "")
					.replaceAll("(?is)<\\s*img[^>]*>", "");

			details = StringEscapeUtils.unescapeHtml(details)
					.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

			return details;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse serverResponse, String rsResponse, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);

			NodeList tableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "edtdiv html-width-default"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"), true);

			if (tableList.size() == 0) {
				return intermediaryResponse;
			}

			TableTag tableTag = (TableTag) tableList.elementAt(0);
			TableRow pagingRow = tableTag.getRow(5);
			TableRow contentRow = tableTag.getRow(6);
			NodeList innerTable = contentRow.searchFor("Parcel", false);
			innerTable = innerTable.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			TableRow[] rows = ((TableTag) innerTable.elementAt(0)).getRows();

			int numberOfPages = 0;
			Pattern pat = Pattern.compile("(?is).*Page\\s+(\\d*)\\s+of\\s+(\\d*).*");
			Matcher mat = pat.matcher(pagingRow.toHtml());

			if (mat.find()) {
				if (mat.group(2).length() > 0) {
					numberOfPages = Integer.parseInt(mat.group(2));
				}

				if (numberOfPages > 1) {
					int nextPage = Integer.parseInt(mat.group(1)) + 1;
					if (nextPage <= numberOfPages) {
						pat = Pattern.compile("(?is)<a href=\"([^>\"]*)\"[^>]*>\\s*" + nextPage + "\\s*</a>");
						mat = pat.matcher(pagingRow.toHtml());
						if (mat.find()) {
							String nextLink = CreatePartialLink(TSConnectionURL.idGET) + mat.group(1).replaceAll("%", "").replaceAll("\\s", "%20");
							parsedResponse.setNextLink("<a href=" + nextLink + ">Next</a>");
						}
					}
				}
			}

			NodeList links = pagingRow.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
			String headerPaging = "";
			for (int i = 0; i < links.size(); i++) {
				LinkTag linkTagPaging = (LinkTag) links.elementAt(i);
				String linkPaging = CreatePartialLink(TSConnectionURL.idGET) + linkTagPaging.extractLink().trim().replaceAll("%", "").replaceAll("\\s", "%20");
				linkTagPaging.setLink(linkPaging);
			}

			headerPaging = pagingRow.toHtml().replaceAll("((?is)class=\\\".*?\\\")|((?is)class='.*?')", "class=\"intermediary\"");
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				LinkTag linkTag = ((LinkTag) row.getColumns()[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
				String link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.extractLink().trim().replaceAll("\\s", "%20");
				linkTag.setLink(link);

				ResultMap m = parseIntermediaryRow(row, searchId);
				m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

				String addr = StringUtils.defaultString(((String) m.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName()))).trim();
				addr = URLEncoder.encode(addr, "UTF-8");
				if (addr.length() > 0) {
					link += "&intermediaryAddress=" + addr;
				}
				linkTag.setLink(link);

				String rowString = row.toHtml()
						.replaceAll("((?is)class=\\\".*?\\\")|((?is)class='.*?')", "")
						.replaceAll("(?is)<td[^>]*>\\s*</td>", "")
						.replaceAll("((?is)width=\".*?\")|((?is)width='.*?')", "");

				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowString);
				currentResponse.setOnlyResponse(rowString);

				currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

				Bridge bridge = new Bridge(currentResponse, m, searchId);
				DocumentI document = (TaxDocumentI) bridge.importData();
				currentResponse.setDocument(document);
				intermediaryResponse.add(currentResponse);
			}

			parsedResponse.setHeader(headerPaging +
					"<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n"
					+ "<tr><th>Property or Business Address</th><th>Parcel No.</th></tr>");
			parsedResponse.setFooter("</table>");
			outputTable.append(tableList.toHtml());

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {

			String baseAmount = "";
			String totalDue = "";
			String amountPaid = "";
			String priorDelinquent = "";
			String taxYear = "";
			String land = "";
			String improvements = "";
			String taxRateArea = "";
			String lastDatePaid = "";

			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

			detailsHtml = detailsHtml.replaceAll("(?s)<class\\b[^>]*>", "")
					.replaceAll("(?is)</?(center|font)[^>]*>", "")
					.replaceAll("(?s)<!--.*?-->", "");
			detailsHtml = Tidy.tidyParse(detailsHtml, null);
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			NodeList nodeList = htmlParser3.getNodeList();

			Node detailsNode = htmlParser3.getNodeById("detailsCAOrangeTR");
			Node detailsL2Node = htmlParser3.getNodeById("detailsL2Row");

			if (detailsNode != null) {

				// get parcelId
				String parcelId = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Parcel No"), "", true).trim();

				if (StringUtils.isEmpty(parcelId) && detailsL2Node != null) {
					parcelId = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(detailsL2Node.getChildren(), "Parcel No"), "", true);
					parcelId = RegExUtils.getFirstMatch("([\\d-]+)\\s*$", parcelId, 1);
				}

				if (StringUtils.isNotEmpty(parcelId)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelId);
				}

				// get address
				String address = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Property Location"), "", true);
				if (StringUtils.isNotEmpty(address)) {
					for (String city : cities) {
						if (StringUtils.containsIgnoreCase(address, city)) {
							resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.toUpperCase());
							address = address.replaceFirst("(?is)\\s*" + city + "\\s*", "");
							break;
						}
					}
					String[] addressArray = StringFormats.parseAddress(address);
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), addressArray[0]);
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), addressArray[1]);
				}

				// get legal description
				Pattern ldPat = Pattern.compile("(?is)<td[^>]*>Legal\\s*Description\\s*</td>\\s*<td [^>]*>(.*?)</td>");
				Matcher ldMat = ldPat.matcher(nodeList.toHtml());
				while (ldMat.find()) {
					ro.cst.tsearch.servers.functions.CAOrangeTR.parseLegalSummary(resultMap, ldMat.group(1));
				}

				// get base amount
				baseAmount = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(nodeList, "Total Net Taxable Value"), "", true)
						.replaceAll(amountPattern, "");
				baseAmount = replaceParanthesisWithMinus(baseAmount);
				if (StringUtils.isNotEmpty(baseAmount)) {
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
				}

				// get amount due
				totalDue = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(nodeList, "Total Due and Payable"), "", true)
						.replaceAll(amountPattern, "");
				totalDue = replaceParanthesisWithMinus(totalDue);
				if (StringUtils.isNotEmpty(totalDue)) {
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
				}

				// get prior delinquent
				NodeList delinquentNodeList = nodeList.extractAllNodesThatMatch(new RegexFilter("(?is)Total\\s+tax\\s+amount\\s+due\\s+now"), true);
				if (delinquentNodeList.size() > 0) {
					priorDelinquent = delinquentNodeList
							.elementAt(0)
							.getParent()
							.getParent()
							.getFirstChild()
							.getNextSibling()
							.getNextSibling()
							.getNextSibling()
							.getNextSibling()
							.getNextSibling()
							.getNextSibling()
							.getNextSibling()
							.toHtml()
							.replaceAll("(?is)\\s*<[^>]*>\\s*", "")
							.replaceAll(amountPattern, "");

					priorDelinquent = replaceParanthesisWithMinus(priorDelinquent);
					if (StringUtils.isNotEmpty(priorDelinquent)) {
						resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
					}
				}

				// get land
				land = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Land"), "", true).replaceAll(amountPattern, "");
				land = replaceParanthesisWithMinus(land);
				if (StringUtils.isNotEmpty(land)) {
					resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
				}

				// get improvements
				improvements = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Improvement"), "", true)
						.replaceAll(amountPattern, "");
				improvements = replaceParanthesisWithMinus(improvements);
				if (StringUtils.isNotEmpty(improvements)) {
					resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvements);
				}

				// get tax year
				NodeList yearNode = nodeList.extractAllNodesThatMatch(new StringFilter("FISCAL YEAR"), true);
				if (yearNode.size() > 0) {
					taxYear = RegExUtils.getFirstMatch("(?is).*?FISCAL\\s+YEAR\\s+(\\d{4})", yearNode.toHtml(), 1);
				} else {
					yearNode = nodeList.extractAllNodesThatMatch(new RegexFilter("(?is)Tax\\s*Year\\s*:"), true);
					if (yearNode.size() > 0) {
						taxYear = yearNode
								.elementAt(yearNode.size() - 1)
								.toHtml()
								.replaceAll("(?is)<[^>]*>", "");
						taxYear = RegExUtils.getFirstMatch("(?is)Tax\\s*Year\\s*:?\\s*(\\d{4})\\s*", taxYear, 1);
					}
				}

				if (StringUtils.isNotEmpty(taxYear)) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
				}

				// get BA, AD, AP from tcref link
				NodeList baTCRefNode = nodeList.extractAllNodesThatMatch(new RegexFilter("(?is)base\\s+tax\\s+amount"), true);
				if (baTCRefNode.size() > 0) {
					String baTCRef = baTCRefNode
							.elementAt(baTCRefNode.size() - 1)
							.getParent()
							.getParent()
							.getLastChild()
							.getPreviousSibling()
							.getPreviousSibling()
							.getPreviousSibling()
							.toHtml()
							.replaceAll("(?is)<[^>]*>", "")
							.replaceAll(amountPattern, "");
					baTCRef = replaceParanthesisWithMinus(baTCRef);

					if (StringUtils.isNotEmpty(baTCRef) && resultMap.get(TaxHistorySetKey.BASE_AMOUNT.getKeyName()) == null) {
						resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baTCRef);
					}
				}

				// get TOTAL_DUE and AMOUNT paid from ACCOUNT SUMMARY table
				NodeList accountSummaryTable = nodeList.extractAllNodesThatMatch(new RegexFilter("(?s)^\\s*Tax\\s+Year\\s*$"), true);
				if (accountSummaryTable.size() > 0) {
					amountPaid = accountSummaryTable
							.elementAt(0)
							.getParent()
							.getParent()
							.getParent()
							.getLastChild()
							.getPreviousSibling()
							.getLastChild()
							.getPreviousSibling()
							.getPreviousSibling()
							.getPreviousSibling()
							.toHtml()
							.replaceAll("(?is)<[^>]*>", "")
							.replaceAll(amountPattern, "");
					amountPaid = replaceParanthesisWithMinus(amountPaid);
					if (StringUtils.isNotEmpty(amountPaid)) {
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
					}

					totalDue = accountSummaryTable
							.elementAt(0)
							.getParent()
							.getParent()
							.getParent()
							.getLastChild()
							.getPreviousSibling()
							.getLastChild()
							.getPreviousSibling()
							.toHtml()
							.replaceAll("(?is)<[^>]*>", "")
							.replaceAll(amountPattern, "");
					totalDue = replaceParanthesisWithMinus(totalDue);
					if (StringUtils.isNotEmpty(totalDue)) {
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
					}
				}

				NodeList adTCRefNode = nodeList.extractAllNodesThatMatch(new RegexFilter("(?is)balance\\s+due"), true);
				if (adTCRefNode.size() > 0) {
					String adTCRef = adTCRefNode
							.elementAt(adTCRefNode.size() - 1)
							.getParent()
							.getParent()
							.getLastChild()
							.getPreviousSibling()
							.getPreviousSibling()
							.getPreviousSibling()
							.toHtml()
							.replaceAll("(?is)<[^>]*>", "")
							.replaceAll(amountPattern, "");
					adTCRef = replaceParanthesisWithMinus(adTCRef);

					if (StringUtils.isNotEmpty(adTCRef) && resultMap.get(TaxHistorySetKey.TOTAL_DUE.getKeyName()) == null) {
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), adTCRef);
					}
				}

				NodeList apTCRefNode = nodeList.extractAllNodesThatMatch(new RegexFilter("(?is)Previously\\s+paid\\s+amount"), true);
				if (apTCRefNode.size() > 0) {
					String apTCRef = apTCRefNode
							.elementAt(apTCRefNode.size() - 1)
							.getParent()
							.getParent()
							.getLastChild()
							.getPreviousSibling()
							.getPreviousSibling()
							.getPreviousSibling()
							.toHtml()
							.replaceAll("(?is)<[^>]*>", "")
							.replaceAll(amountPattern, "");
					apTCRef = replaceParanthesisWithMinus(apTCRef);
					if (StringUtils.isNotEmpty(apTCRef) && resultMap.get(TaxHistorySetKey.AMOUNT_PAID.getKeyName()) == null) {
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), apTCRef);
					}
				}

				// get tax rate area
				taxRateArea = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Tax Rate Area"), "", true).trim();
				if (StringUtils.isNotEmpty(taxRateArea)) {
					resultMap.put(PropertyIdentificationSetKey.AREA_CODE.getKeyName(), taxRateArea);
				}

				// parse installments
				String instAP = "";
				String instAD = "";
				String instStatus = "";
				String instBA = "";

				List<String> line = new ArrayList<String>();
				List<List> bodyInstallments = new ArrayList<List>();
				NodeList[] inst = new NodeList[2];
				inst[0] = nodeList.extractAllNodesThatMatch(new RegexFilter("(?is)First\\sInstallment"), true);
				inst[1] = nodeList.extractAllNodesThatMatch(new RegexFilter("(?is)Second\\sInstallment"), true);
				NodeList totalAmtPaidNode = nodeList.extractAllNodesThatMatch(new RegexFilter("(?is)Total\\sAmt\\sPaid"), true);

				Map<String, String[]> map = new HashMap<String, String[]>();
				ResultTable installmentsRT = new ResultTable();
				String[] installmentsHeader = {
						TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(),
						TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
						TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(),
						TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
						TaxInstallmentSetKey.STATUS.getShortKeyName(),
				};
				installmentsRT.setHead(installmentsHeader);

				for (String s : installmentsHeader) {
					map.put(s, new String[] { s, "" });
				}

				if (totalAmtPaidNode.size() > 0) {
					amountPaid = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(nodeList, "Total Amt Paid"), "", true).replaceAll(
							amountPattern, "");
					amountPaid = replaceParanthesisWithMinus(amountPaid);
					if (StringUtils.isNotEmpty(amountPaid) && resultMap.get(TaxHistorySetKey.AMOUNT_PAID.getKeyName()) == null) {
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
					}
				}

				int rowOffset = -2;
				for (int i = 0; i < 2; i++) {
					if (i == 1) {
						rowOffset = -1;
					}
					line = new ArrayList<String>();

					// inst Name
					line.add((i == 0 ? "First" : "Second") + " Installment");
					
					// inst base amount
					if (StringUtils.isNotEmpty(baseAmount) && instBA.isEmpty()) {
						Double baseAmountD = Double.parseDouble(baseAmount);
						double instBA_D = ((baseAmountD * 100) / 2) / 100;
						instBA = String.valueOf(instBA_D);
					}
					line.add(instBA);

					// inst Amount Paid
					instAP = HtmlParser3.getValueFromAbsoluteCell(rowOffset, 3, HtmlParser3.findNode(nodeList, "Total Amt Paid"), "", true).replaceAll(
							amountPattern, "");
					instAP = replaceParanthesisWithMinus(instAP);
					line.add(instAP);

					// inst Total Due
					instAD = HtmlParser3.getValueFromAbsoluteCell(rowOffset, 3, HtmlParser3.findNode(nodeList, "Total Due and Payable"), "", true).replaceAll(
							amountPattern, "");
					instAD = replaceParanthesisWithMinus(instAD);
					line.add(instAD);

					// inst Status
					instStatus = HtmlParser3.getValueFromAbsoluteCell(rowOffset, 2, HtmlParser3.findNode(nodeList, "Total Due and Payable"), "", true).trim();

					line.add(instStatus);

					// get last date paid
					String tmpLastDatePaid = HtmlParser3.getValueFromAbsoluteCell(rowOffset, 1, HtmlParser3.findNode(nodeList, "Total Amt Paid"), "", true)
							.trim();
					if (tmpLastDatePaid.matches("\\d+/\\d+/\\d+")) {
						lastDatePaid = tmpLastDatePaid;
					}

					if (line.size() == installmentsHeader.length) {
						bodyInstallments.add(line);
					}
				}

				if (!bodyInstallments.isEmpty()) {
					installmentsRT.setHead(installmentsHeader);
					installmentsRT.setBody(bodyInstallments);
					installmentsRT.setMap(map);
					installmentsRT.setReadOnly();
					resultMap.put("TaxInstallmentSet", installmentsRT);
				}
			}

			if (StringUtils.isNotEmpty(lastDatePaid)) {
				resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), lastDatePaid);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m = null;

		// search by parcel ID
		if (hasPin()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			m.clearSaKeys();
			m.setSaKey(0, SearchAttributes.LD_PARCELNO);
			l.add(m);
		}

		// search by address
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
		FilterResponse city = CityFilterFactory.getCityFilter(searchId, 0.8d);
		if (hasStreet()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(0).setSaKey(SearchAttributes.P_STREET_FULL_NAME_EX);
			m.getFunction(1).setSaKey(SearchAttributes.P_CITY);
			m.addFilter(addressFilter);
			m.addFilter(city);
			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);
	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();

		try {
			TableColumn[] cols = row.getColumns();
			if (cols.length == 3) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), cols[1].toPlainTextString().trim());

				NodeList internalColumns = cols[0].getChildren();

				String fullAddress = internalColumns.elementAt(0).toHtml().trim();
				for (String city : cities) {
					if (StringUtils.containsIgnoreCase(fullAddress, city)) {
						resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
						fullAddress = fullAddress.replaceFirst("(?is)\\s*" + city + "\\s*", "");
						break;
					}
				}

				String[] address = StringFormats.parseAddress(fullAddress);
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), address[0]);
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), address[1]);
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), fullAddress);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultMap;
	}

	protected static String replaceParanthesisWithMinus(String amount) {
		amount = org.apache.commons.lang.StringUtils.defaultString(amount);
		if (amount.contains("(")) {
			amount = "-" + RegExUtils.getFirstMatch(amountAbsoluteValuePattern, amount, 1);
		}
		return amount;
	}
}

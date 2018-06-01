package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.taglibs.standard.tag.common.core.SetSupport;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.reports.comparators.IntegerComparator;
import ro.cst.tsearch.search.filter.parser.name.NameParser;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.functions.MOPlatteTR.LotParser;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.RegExUtils;

public class TXWiseTR extends ParseClassWithExceptionTreatment {

	private static TXWiseTR _instance = null;

	private static MessageFormat linkFormat;

	public static MessageFormat getLinkFormat() {
		return linkFormat;
	}

	public static void setLinkFormat(MessageFormat linkFormat) {
		TXWiseTR.linkFormat = linkFormat;
	}

	private TXWiseTR() {
		super(_instance);
	}

	public static TXWiseTR getInstance() {
		if (_instance == null) {
			_instance = new TXWiseTR();
		}
		return _instance;
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		HtmlParser3 parser = new HtmlParser3(response);
		NodeList tableTag = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), false);
		TableTag resultTable = (TableTag) tableTag.elementAt(0);
		if (resultTable != null) {

			TableRow[] rows = resultTable.getRows();

			int nameColumn = 0;
			int parcelColumn = 2;
			int statementNoCol = 4;
			int rollYearColumn = 6;
			int amntDueColumn = 8;
			int lastColumn = 10;
			String startLink = createPartialLink(format, TSConnectionURL.idGET);

			for (int i = 1; i < rows.length; i++) {
				NodeList cells = rows[i].getChildren();

				Node parcelColumnNode = cells.elementAt(parcelColumn);
				String parcelId = "";
				if (parcelColumnNode != null) {
					parcelId = StringUtils.defaultIfEmpty(parcelColumnNode.toPlainTextString(), "").trim();
				}

				if (StringUtils.isNotEmpty(parcelId)) {
					ResultMap resultMap = new ResultMap();
					resultMap.put(PropertyIdentificationSet.PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelId);

					Node nameNode = cells.elementAt(nameColumn);
					String name = nameNode.toPlainTextString();
					resultMap.put(PropertyIdentificationSet.PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
					_instance.parseName(name, resultMap);

					String statementNumber = cells.elementAt(statementNoCol).toPlainTextString();
					resultMap.put(TaxHistorySetKey.RECEIPT_NUMBER.getKeyName(), statementNumber);

					String rollYear = cells.elementAt(rollYearColumn).toPlainTextString();
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), rollYear);

					String amountDue = cells.elementAt(amntDueColumn).toPlainTextString();
					resultMap.put(TaxHistorySetKey.RECEIPT_AMOUNT.getKeyName(), ro.cst.tsearch.utils.StringUtils.cleanAmount(amountDue));

					ParsedResponse currentResponse = new ParsedResponse();
					Node lastColumnNode = cells.elementAt(lastColumn);
					String detailActionID = "";
					Node spanTag = nameNode.getFirstChild();

					if (spanTag instanceof Span) {
						detailActionID = ((Span) spanTag).getAttribute("id");
						LinkTag detailLinkTag = new LinkTag();
						String regEx = detailActionID + ".*url:(.*)\"";
						String url = startLink + RegExUtils.getFirstMatch(regEx, response, 1);
						detailLinkTag.setLink(url);
						detailLinkTag.setChildren(new NodeList(new org.htmlparser.nodes.TextNode("View Details")));
						currentResponse.setPageLink(new LinkInPage(url, url, TSServer.REQUEST_SAVE_TO_TSD));
						lastColumnNode.setChildren(new NodeList(detailLinkTag));
					}

					String rowHtml = rows[i].toHtml();
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					createDocument(searchId, currentResponse, resultMap);
					intermediaryResponse.add(currentResponse);
				}
			}

			String tableHeader = rows[0].getChildren().toHtml();

			String formAction = RegExUtils.getFirstMatch("name=\"actionForm\" value=\"(.*?)\"", response, 1);
			if (serverResponse != null) {
				String tableFooter = createNextLinks(serverResponse, rows, format, formAction);
				serverResponse.getParsedResponse().setHeader("<table>" + tableHeader);
				serverResponse.getParsedResponse().setFooter(tableFooter + "</table>");
			}
		}

		return intermediaryResponse;
	}

	protected String constructLink(String baseLink, String page, String newQuerry, String linkText, boolean paintLink) {
		String link = "";
		String searchLink = ro.cst.tsearch.connection.http2.FLPalmBeachTR.getSearchLink();
		if (paintLink) {
			link = MessageFormat.format("<a href=\"" + baseLink + searchLink + newQuerry + "\">{1}</a>", page, linkText);
		} else {
			link = linkText;
		}
		return link;
	}

	/**
	 * @param serverResponse
	 * @param rows
	 * @param nextLink
	 * @return
	 */
	public String createNextLinks(ServerResponse serverResponse, TableRow[] rows, MessageFormat format, String formAction) {
		String startLink = createPartialLink(format, TSConnectionURL.idPOST);
		String tableFooter = rows[rows.length - 1].getChildren().toHtml();
		NodeList currentPageNode = rows[rows.length - 1].getChildren().extractAllNodesThatMatch(
				new HasAttributeFilter("id", "resultForm:tableEx1:deluxe1__pagerText"), true);

		String paramGoText = "resultForm:tableEx1:goto1__pagerGoText";
		String paramPageButton = "resultForm:tableEx1:goto1__pagerGoButton";

		
		boolean tableEx2 = false;
		if (currentPageNode==null || currentPageNode.size() ==0 ){
			currentPageNode = rows[rows.length - 1].getChildren().extractAllNodesThatMatch(
					new HasAttributeFilter("id", "resultForm:tableEx2:deluxe1__pagerText"), true);
			tableEx2 = true;
			paramGoText = "resultForm:tableEx2:goto1__pagerGoText";
			paramPageButton = "resultForm:tableEx2:goto1__pagerGoButton";
		}


		HashMap<String, String> parametersMap = new HashMap<String, String>();
		parametersMap.put(paramPageButton, "Go");
		parametersMap.put("resultForm", "resultForm");
		ro.cst.tsearch.connection.http2.TXWiseTR txWiseTR = new ro.cst.tsearch.connection.http2.TXWiseTR();

		if (currentPageNode != null && currentPageNode.size() > 0) {
			String currentPage = currentPageNode.toHtml();
			currentPage = RegExUtils.getFirstMatch("Page (\\d+)", currentPage, 1);
			int currentPageInt = 1;
			if (StringUtils.isNumeric(currentPage)) {
				currentPageInt = Integer.valueOf(currentPage);
			}
			// action="/tax/propertyAddressAll.faces"

			String searchLink = formAction;
			// txWiseTR.getDetailNextFormAction();
			String url = startLink + searchLink;

			String firstPageLink = createLink(paramGoText, parametersMap, url, 1, "First");
			String previousPageLink = createLink(paramGoText, parametersMap, url, (currentPageInt <= 2) ? 1 : currentPageInt - 1,
					"Previous");
			String maxNoOfPages = RegExUtils.getFirstMatch("of (\\d+)", currentPageNode.toHtml(), 1);
			int maxNoOfPagesInt = 0;

			if (NumberUtils.isDigits(maxNoOfPages)) {
				maxNoOfPagesInt = Integer.valueOf(maxNoOfPages);
			}
			String nextPagePageLink = createLink(paramGoText, parametersMap, url, (currentPageInt < maxNoOfPagesInt) ? (currentPageInt + 1)
					: maxNoOfPagesInt, "Next");
			String lastPageLink = createLink(paramGoText, parametersMap, url, maxNoOfPagesInt, "Last");

			StringBuilder tf = new StringBuilder();
			String pageId = "pagerFirst";
			String tableExNumber = tableEx2 ?"2":"1";
			String divFormatToReplace = "(?is)(?is)<input id=\"resultForm:tableEx" + tableExNumber + ":deluxe1__%s.*?/>";// "(?is)<div id=\"resultForm:tableEx1:deluxe1__%s_TWISTIE.*?</div>";
			tableFooter = tableFooter.replaceAll(String.format(divFormatToReplace, pageId), firstPageLink + "&nbsp;&nbsp;");
			pageId = "pagerPrevious";
			tableFooter = tableFooter.replaceAll(String.format(divFormatToReplace, pageId), previousPageLink + "&nbsp;&nbsp;");
			pageId = "pagerNext";
			String nextLink = RegExUtils.getFirstMatch("(?is)href=\"(.*?)\">", nextPagePageLink, 1);
			if (currentPageInt < maxNoOfPagesInt) {
				serverResponse.getParsedResponse().setNextLink("<a href='" + nextLink + "' />");
			}

			tableFooter = tableFooter.replaceAll(String.format(divFormatToReplace, pageId), "&nbsp;&nbsp;" + nextPagePageLink
					+ "&nbsp;&nbsp;");
			pageId = "pagerLast";
			tableFooter = tableFooter.replaceAll(String.format(divFormatToReplace, pageId), lastPageLink);
			tableFooter = tableFooter.replaceFirst("(?is)<td><span.*</span></span>", "");
		}
		return tableFooter;
	}

	/**
	 * @param paramGoText
	 * @param parametersMap
	 * @param url
	 * @param pageNumber
	 */
	public String createLink(String paramGoText, HashMap<String, String> parametersMap, String url, int pageNumber, String linkText) {
		parametersMap.put(paramGoText, "" + pageNumber);
		url = ro.cst.tsearch.utils.StringUtils.addParametersToUrl(url + "?", parametersMap);
		String link = String.format("<a href=\"" + url + "\">%1$s</a>", linkText);
		return link;
	}

	private static final Map<String, String> mapSiteHtmlIdToATSKeySet = new HashMap<String, String>();

	static {
		mapSiteHtmlIdToATSKeySet.put("form1:textGdst_roll_year1", TaxHistorySetKey.YEAR.getKeyName());
		mapSiteHtmlIdToATSKeySet.put("form1:textGdst_statment_no1", TaxHistorySetKey.RECEIPT_NUMBER.getKeyName());
		mapSiteHtmlIdToATSKeySet.put("form1:textGdsa_owner_name1", PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());

		mapSiteHtmlIdToATSKeySet.put("form1:textGdsa_loc_street_no1", PropertyIdentificationSetKey.STREET_NO.getKeyName());
		mapSiteHtmlIdToATSKeySet.put("form1:textGdsa_loc_street_name1", PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		mapSiteHtmlIdToATSKeySet.put("form1:textGdsa_owner_addr11", "tmpAddress1");
		mapSiteHtmlIdToATSKeySet.put("form1:textGdsa_owner_addr21", "tmpAddress2");
		// mapSiteHtmlIdToATSKeySet.put("form1:textGdsa_owner_addr41",
		// PropertyIdentificationSetKey.CITY.getKeyName());
		mapSiteHtmlIdToATSKeySet.put("form1:textGdsa_legal_desc1", PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		mapSiteHtmlIdToATSKeySet.put("form1:textGdst_market_val1", PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName());
		mapSiteHtmlIdToATSKeySet.put("form1:textGdst_total_due2", TaxHistorySetKey.TOTAL_DUE.getKeyName());
		mapSiteHtmlIdToATSKeySet.put("form1:textGdsa_geo_number1", PropertyIdentificationSetKey.PARCEL_ID.getKeyName());

	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {

		HtmlParser3 parser3 = new HtmlParser3(response);
		NodeList localNodeList = parser3.getNodeList();
		Set<Entry<String, String>> entrySet = mapSiteHtmlIdToATSKeySet.entrySet();
		for (Entry<String, String> entry : entrySet) {
			String value = HtmlParser3.getNodeValueByID(entry.getKey(), localNodeList, true);
			resultMap.put(entry.getValue(), value);
		}

		saveTestDataToFiles(resultMap);

		parseName("", resultMap);
		parseAddress("", resultMap);
		parseLegalDescription("", resultMap);
		setTaxData(response, resultMap);
	}

	@Override
	public void parseLegalDescription(String legalDescription, ResultMap resultMap) {
		// set abstract number
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		legal = legal.replaceAll("&amp;", "&");

		String absNo = RegExUtils.getFirstMatch("(ABST:)?(A-\\s*\\d+)", legal, 2);
		absNo = absNo.replaceAll("\\s+", "");
		resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo);

		legal = parseBlock(legal, resultMap);
		legal = parsePhase(legal, resultMap);
		legal = parseUnit(legal, resultMap);
		legal = parsePhase(legal, resultMap);
		legal = parseTract(legal, resultMap);
		legal = parseLot(legal, resultMap);
		setSubdivisionName(legal, resultMap);

	}

	public String setSubdivisionName(String legal, ResultMap resultMap) {
		String subdivision = RegExUtils.getFirstMatch("SUBD:(.*)(?:(?:(UNIT)|(,)|(PH)|(BLK)))", legal, 1);
		subdivision = subdivision.trim();

		while (subdivision.endsWith(",")) {
			subdivision = subdivision.substring(0, subdivision.length() - 1);
		}
		if (StringUtils.isEmpty(subdivision) && !(legal.startsWith("A-") || legal.startsWith("ABST:"))) {
			subdivision = RegExUtils.getFirstMatch("^(.*?)(?:(U\\d+)?,)", legal, 1);
		}
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
		return legal;
	}

	public String parsePhase(String text, ResultMap resultMap) {
		String regEx = "PH\\s+(\\d+)";
		String phase = RegExUtils.getFirstMatch(regEx, text, 1);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		// resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(),
		// );
		return text.replaceAll(regEx, "");
	}

	public String parseLot(String text, ResultMap resultMap) {
		// case 1 LOT 4 BLK 2 // A-850 T &amp; PRR CO, FYKE PROPERTIES, LOT PT 9
		// case 2 LOT 49 & 50
		// case 3 SUBD: ORIGINAL TOWN PARADISE, BLK: 11, LOT: 19-24

		text = text.replaceAll("LOT PT", "LOT");
		String lotEnumRegEx = "LOTS?:?\\s(\\s?(\\d+)\\s?(\\+|&|AND)?\\s?)+";
		String lotIntervalRegEx = "LOT:?\\s+(\\d+)\\s*(-)\\s*(\\d+)";

		List<String> lotEnumeration = RegExUtils.getMatches(lotEnumRegEx, text, 0);
		// case 2
		String result = "";
		if (!RegExUtils.matches(lotIntervalRegEx, text)) {
			for (String string : lotEnumeration) {
				result += " " + string.replaceAll("&|\\+|-|:|PT", " ").replaceAll("LOTS?", "");
			}
		} else {
			List<String> lotInterval = RegExUtils.getFirstMatch(lotIntervalRegEx, text, 1, 3);
			if (lotInterval.size() == 2) {
				String i1 = lotInterval.get(0);
				String i2 = lotInterval.get(1);
				result += i1 + "-" + i2;
			}
		}
		StringBuffer lots = new StringBuffer("");
		lots.append(" " + result);
		// String[] strings = lots.toString().split("\\s+");
		// Arrays.sort(strings, new IntegerComparator());
		// String lotValues =
		// ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("",
		// strings).trim();

		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots.toString());
		return text.replaceAll(lotEnumRegEx, "");
	}

	public String parseBlock(String text, ResultMap resultMap) {
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		String regEx = "BLK:?\\s(\\w+)";
		String string = RegExUtils.getFirstMatch(regEx, legal, 1);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), string);
		return text.replaceAll(regEx, "");
	}

	public String parseTract(String text, ResultMap resultMap) {
		String regEx = "TRACT\\s(PT )?(\\d+)";
		String string = RegExUtils.getFirstMatch(regEx, text, 1);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), string);
		return text.replaceAll(regEx, "");
	}

	@Override
	public void setTaxData(String text, ResultMap resultMap) {
		HtmlParser3 parser3 = new HtmlParser3(text);
		Node taxTable = HtmlParser3.getNodeByID("form1:tableEx1", parser3.getNodeList(), true);
		StringBuilder baseAmount = new StringBuilder();
		String baseAmountKey = "Current Levy";
		String amountDueKey = "Amount Due";
		StringBuilder amountDue = new StringBuilder();
		String amountPaidKey = "Amount Paid";
		StringBuilder amountPaid = new StringBuilder();
		String lastPaidDate = "";
		String assessedValue = HtmlParser3.getValueFromAbsoluteCell(0, 2, 
				HtmlParser3.findNode(parser3.getNodeList(), "Market Value", true), "", false);
		if (StringUtils.isNotEmpty(assessedValue)) {
			assessedValue = assessedValue.replaceAll(",", "").trim();
			resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessedValue);
		}
		if (taxTable instanceof TableTag) {
			List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap((TableTag) taxTable);
			for (HashMap<String, String> h : tableAsListMap) {
				String taxType = h.get("Entity");
				// if ("WISE COUNTY".equals(taxType)) {
				baseAmount
						.append(ro.cst.tsearch.utils.StringUtils.cleanAmount(StringUtils.defaultIfEmpty(h.get(baseAmountKey), "0")) + "+");
				amountDue.append(ro.cst.tsearch.utils.StringUtils.cleanAmount(StringUtils.defaultIfEmpty(h.get(amountDueKey), "0")) + "+");
				amountPaid
						.append(ro.cst.tsearch.utils.StringUtils.cleanAmount(StringUtils.defaultIfEmpty(h.get(amountPaidKey), "0")) + "+");
				lastPaidDate = h.get("Last Pay Date");
				// }
			}
		}

		String amountPaidSum = GenericFunctions.sum(amountPaid.toString(), -1);
		if (Double.valueOf(amountPaidSum).doubleValue() > 0) {
			resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaidSum);
			// resultMap.put(TaxHistorySetKey.RECEIPT_DATE.getKeyName(),
			// amountPaidSum);
		}

		resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), GenericFunctions.sum(baseAmount.toString(), -1));
		String amountDueDirectlyParsed = (String) resultMap.get(TaxHistorySetKey.TOTAL_DUE.getKeyName());
		if (StringUtils.isNotEmpty(amountDueDirectlyParsed)) {
			amountDueDirectlyParsed = ro.cst.tsearch.utils.StringUtils.cleanAmount(amountDueDirectlyParsed);
			resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDueDirectlyParsed);
		} else {
			resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), GenericFunctions.sum(amountDue.toString(), -1));
		}

		if (RegExUtils.matches("\\d{8,8}", lastPaidDate)) {
			List<String> matches = RegExUtils.getMatches("(\\d{4,4})(\\d{2,2})(\\d{2,2})", lastPaidDate);
			if (matches.size() == 3) {
				lastPaidDate = matches.get(1) + "/" + matches.get(2) + "/" + matches.get(0);
			}
		}
		if (StringUtils.isNotEmpty(lastPaidDate)) {
			resultMap.put(TaxHistorySetKey.RECEIPT_DATE.getKeyName(), lastPaidDate);
			resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), lastPaidDate);
		}
		Node taxDelinquentTable = HtmlParser3.getNodeByID("resultForm:tableEx1", parser3.getNodeList(), true);
		String year = (String) resultMap.get(TaxHistorySetKey.YEAR.getKeyName());
		int y = 2100;
		if (NumberUtils.isDigits(year)) {
			y = Integer.valueOf(year);
		}

		if (taxDelinquentTable instanceof TableTag) {
			List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap((TableTag) taxDelinquentTable);
			BigDecimal sum = new BigDecimal(0);
			for (HashMap<String, String> hashMap : tableAsListMap) {
				String cy = hashMap.get("Roll Year");
				String amnt = hashMap.get("Amount Due");
				amnt = ro.cst.tsearch.utils.StringUtils.cleanAmount(amnt);
				if (NumberUtils.isNumber(cy)){
					int curRowYearIntValue = Integer.valueOf(cy).intValue();;
					if (NumberUtils.isNumber(amnt) && curRowYearIntValue < y ) {
						sum = sum.add(new BigDecimal(amnt));
					}
				}
			}
			resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), sum.equals(BigDecimal.ZERO) ? "" : sum.toPlainString());
		}
	}

	@Override
	public void parseName(String name, ResultMap resultMap) {
		String nameOnServer = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		String secondLineName = (String) resultMap.get("tmpAddress2");
		nameOnServer = StringUtils.defaultIfEmpty(nameOnServer, "").trim();
		if (secondLineName != null)
			secondLineName = secondLineName.trim();
		if (StringUtils.isNotEmpty(secondLineName)) {
			String address = StringUtils.defaultIfBlank((String) resultMap.get("tmpAddress1"), "");
			secondLineName = StringUtils.defaultIfEmpty(address, "").trim();
		}

		if (StringUtils.isNotEmpty(nameOnServer)) {
			// nameOnServer = nameOnServer.replace("&", "AND");
			List body = new ArrayList();
			nameOnServer = nameOnServer.replace("&amp;", "&");
			if (NameUtils.isCompany(nameOnServer)) {
				parseName(resultMap, nameOnServer, body);
			} else if (nameOnServer.contains("&")) {
				String[] split = nameOnServer.split("&");
				if (split != null && split.length == 2){
					if (LastNameUtils.isNotLastName(split[1])){
						String lastName = RegExUtils.getFirstMatch("^\\w*\\b", nameOnServer, 0);
						parseName(resultMap, split[0], body);
						parseName(resultMap, lastName + " " + split[1], body);
					}else{
						parseName(resultMap, split[0], body);
						parseName(resultMap, "%" + split[1], body);
					}
				}
			}else{
				parseName(resultMap, nameOnServer, body);
			}

			if (StringUtils.isNotEmpty(secondLineName)) {
				if (!secondLineName.startsWith("%")) {
					secondLineName = "%" + secondLineName;
				}
				secondLineName = secondLineName.replaceAll(" & ", " AND ");
				parseName(resultMap, secondLineName, body);
			}

		}
	}

	/**
	 * @param resultMap
	 * @param nameOnServer
	 * @param body
	 */
	private void parseName(ResultMap resultMap, String nameOnServer, List body) {
		nameOnServer = StringUtils.defaultIfEmpty(nameOnServer.replaceAll("ET AL", "ETUX"), "").trim();
		nameOnServer = nameOnServer.replaceAll("ET UX", "ETUX");
		
		nameOnServer = nameOnServer.replaceAll("\\b(\\w*)(/)(\\w*)\\b", "$1 AND $3");
		
		nameOnServer = nameOnServer.replaceAll("\\s+", " ").trim();
		nameOnServer = nameOnServer.replace("&amp;", "&");
		nameOnServer = nameOnServer.replace("FAMILY TR", "FAMILY TRUST");
		
		if (nameOnServer.contains("%")) {
			nameOnServer = nameOnServer.replace(" & ", " AND ");
			String[] split = nameOnServer.split("%");
			ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, split[0], body);
			String secondSplit = split[1];
			if (StringUtils.isNotEmpty(secondSplit)){
				if (RegExUtils.matches("\\w\\s\\w\\s\\w*\\s\\w*\\sAND\\s\\w\\s\\w*\\s\\w*", secondSplit)){
					String[] split2 = secondSplit.split("AND");
					ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, split2[0], body);
					ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, split2[1], body);
				}else{
					ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, secondSplit, body);
				}
			}
		} else {
			ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, nameOnServer, body);
		}
	}

	public String parseUnit(String text, ResultMap resultMap) {
		String regEx = "(UNIT|#)\\s?(\\w+)";
		String unit = RegExUtils.getFirstMatch(regEx, text, 2);
		if (StringUtils.isEmpty(unit)) {
			unit = RegExUtils.getFirstMatch("U(\\d+),", text, 1);
		}
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
		return text.replaceAll(regEx, "");
	}

	@Override
	public void parseAddress(String address, ResultMap resultMap) {
		String addressOnServer = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		String addressNumberOnServer = (String) resultMap.get(PropertyIdentificationSetKey.STREET_NO.getKeyName());

		addressOnServer = addressOnServer.replaceAll("\\*BNK 13\\*", "");

		boolean numberISInItsOwnPlace = !addressOnServer.contains(addressNumberOnServer);
		;
		if (numberISInItsOwnPlace) {
			addressOnServer = addressNumberOnServer + " " + addressOnServer;
		}

		address = addressOnServer;
		String streetName = StringFormats.StreetName(address).trim();
		String streetNo = StringFormats.StreetNo(address);
		streetNo = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(streetNo);

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);

		// String cityZIP = (String)
		// resultMap.get(PropertyIdentificationSetKey.CITY.getKeyName());
		//
		// List<String> matches =
		// RegExUtils.getMatches("(\\w+)\\s*,\\s*TX\\s*(\\d+-?(\\d+)?)",
		// cityZIP);
		// if (matches.size() == 3){
		// resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(),
		// matches.get(0));
		// resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(),
		// matches.get(1));
		// }
	}

}

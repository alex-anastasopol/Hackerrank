package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.name.RomanNumber;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * 
 * @author george oprina
 * 
 */

public class UTWeberTR {

	private static final Pattern LEGAL_CROSS_REF_INSTRUMENT_PATTERN = Pattern
			.compile("(?:\\s|^)(R?\\d+)\\s+([A-Z]+)\\s+(\\d\\d-\\d\\d-\\d\\d)(?:\\s|$)");
	private static final Pattern LEGAL_CROSS_REF_BOOK_PAGE_PATTERN = Pattern
			.compile("BK-(\\d+)\\s+PG-(\\d+)\\s+([A-Z]*)\\s*((?:\\d\\d-\\d\\d-\\d\\d)?)(?:\\s|$)");
	private static final Pattern LEGAL_PHASE_PATTERN = Pattern
			.compile("PHASE\\s+([^\\s])");
	private static final Pattern LEGAL_TRACT_PATTERN = Pattern
			.compile("\\bTRACT\\s+([^,])");
	
	private static final Pattern LEGAL_BLOCK_PATTERN = Pattern
	.compile("\\sBLOCK\\s+([^\\s])");

	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		String unparsedName = (String) resultMap
				.get("PropertyIdentificationSet.NameOnServer");
		List<List> body = new ArrayList<List>();
		if (StringUtils.isNotEmpty(unparsedName)) {
			unparsedName = unparsedName.replaceAll(" WF ", " ");
			unparsedName = unparsedName.replaceAll(" HUS ", " ");
			unparsedName = unparsedName.replace(" AND ", " & ");
			String[] mainTokens = unparsedName.split("&");

			if (mainTokens.length == 1) {
				if (mainTokens[0].contains(",")) {
					String[] names = StringFormats.parseNameNashville(
							mainTokens[0], true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions
							.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions
							.extractNameSuffixes(names);
					GenericFunctions.addOwnerNames(names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				} else {
					String currentToken = mainTokens[0];
					String[] tks = currentToken.split(" ");
					currentToken = "";
					int len = 0;
					if (tks.length > 2)
						len = 3;
					if (tks.length > 3)
						len = 4;

					for (int i = 0; i < len; i++) {
						currentToken += tks[i] + " ";
					}

					String[] names = StringFormats.parseNameNashville(
							currentToken, true);
					if (!currentToken.contains("LLC")
							&& !currentToken.contains("TRUST"))
						if (!currentToken.contains(",")) {
							String aux = names[2];
							names[2] = names[0];
							names[0] = aux;
							if (!names[1].equals("")) {
								aux = names[1];
								names[1] = names[2];
								names[2] = aux;
							}
						}
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions
							.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions
							.extractNameSuffixes(names);
					GenericFunctions.addOwnerNames(names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
			} else {
				if (mainTokens[0].contains(",")) {
					for (int i = 0; i < mainTokens.length; i++) {
						String currentToken = mainTokens[i];
						String[] names = StringFormats.parseNameNashville(
								currentToken, true);
						if (!currentToken.contains(",")) {
							String aux = names[2];
							names[2] = names[0];
							names[0] = aux;
							if (!names[1].equals("")) {
								aux = names[1];
								names[1] = names[2];
								names[2] = aux;
							}
						}
						String[] type = GenericFunctions
								.extractAllNamesType(names);
						String[] otherType = GenericFunctions
								.extractAllNamesOtherType(names);
						String[] suffixes = GenericFunctions
								.extractNameSuffixes(names);
						GenericFunctions.addOwnerNames(names, suffixes[0],
								suffixes[1], type, otherType,
								NameUtils.isCompany(names[2]),
								NameUtils.isCompany(names[5]), body);
					}
				} else {
					for (int i = 0; i < mainTokens.length; i++) {
						String currentToken = mainTokens[i];
						if (i > 0) {
							currentToken = currentToken.substring(0,
									currentToken.lastIndexOf(' '));
							String[] tks = currentToken.split(" ");
							currentToken = "";
							if (tks.length > 3) {
								for (int j = 0; j < 4; j++) {
									currentToken += tks[j] + " ";
								}
							}

						} else {
							String[] tks = currentToken.split(" ");
							if (tks.length != 3) {
								String[] tks_aux = unparsedName.split(" ");
								if (tks_aux[4].length() == 1)
									currentToken += tks_aux[5];
								else
									currentToken += tks_aux[4];
							}
						}
						String[] names = StringFormats.parseNameNashville(
								currentToken, true);
						if (names[1].equals("")) {
							String aux = names[2];
							names[2] = names[0];
							names[0] = aux;
						} else {
							String aux = names[2];
							names[2] = names[0];
							names[0] = aux;
							aux = names[1];
							names[1] = names[2];
							names[2] = aux;
						}
						String[] type = GenericFunctions
								.extractAllNamesType(names);
						String[] otherType = GenericFunctions
								.extractAllNamesOtherType(names);
						String[] suffixes = GenericFunctions
								.extractNameSuffixes(names);
						GenericFunctions.addOwnerNames(names, suffixes[0],
								suffixes[1], type, otherType,
								NameUtils.isCompany(names[2]),
								NameUtils.isCompany(names[5]), body);
					}
				}
			}

			try {
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static void parseLegalSummary(ResultMap resultMap, List<List> body,
			long searchId) {
		String legalDescription = (String) resultMap
				.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legalDescription)) {
			return;
		}
		legalDescription += " ";
		String legalDescriptionFake = "FAKE "
				+ legalDescription.toUpperCase().replaceAll(":", " ");

		legalDescriptionFake = GenericFunctions
				.replaceNumbers(legalDescriptionFake);
		if (body == null) {
			body = new ArrayList<List>();
		}
		List<String> line = null;
		Matcher matcher = LEGAL_CROSS_REF_INSTRUMENT_PATTERN
				.matcher(legalDescription);
		while (matcher.find()) {
			line = new ArrayList<String>();
			line.add("");
			line.add(matcher.group(3));
			line.add(matcher.group(1));
			line.add("");
			line.add("");
			line.add(matcher.group(2));
			body.add(line);
			legalDescription = legalDescription.replace(matcher.group(), " ");
			matcher.reset(legalDescription);
		}
		matcher = Pattern
				.compile(
						"BK-(\\d+)\\s+PG-(\\d+)\\s+([A-Z]*)\\s*((?:\\d\\d-\\d\\d-\\d\\d)?)(?:\\s|$)")
				.matcher(legalDescription);
		matcher = LEGAL_CROSS_REF_BOOK_PAGE_PATTERN.matcher(legalDescription);
		while (matcher.find()) {
			line = new ArrayList<String>();
			line.add("");
			line.add(matcher.group(4));
			line.add("");
			line.add(matcher.group(1).replaceAll("^0+", ""));
			line.add(matcher.group(2).replaceAll("^0+", ""));
			line.add(matcher.group(3));
			body.add(line);
			legalDescription = legalDescription.replace(matcher.group(), " ");
			matcher.reset(legalDescription);
		}

		String lot = LegalDescription.extractLotFromText(legalDescriptionFake);
		if (StringUtils.isNotEmpty(lot)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber",
					Roman.normalizeRomanNumbers(lot));
		}
		String unit = LegalDescription
				.extractUnitFromText(legalDescriptionFake);
		if (StringUtils.isNotEmpty(unit)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionUnit",
					Roman.normalizeRomanNumbers(unit));
		}

		matcher = LEGAL_PHASE_PATTERN.matcher(legalDescription);
		if (matcher.find()) {
			try {
				resultMap.put("PropertyIdentificationSet.SubdivisionPhase",
						matcher.group(1));
				legalDescription = legalDescription.replace(matcher.group(),
						" ");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		matcher = LEGAL_TRACT_PATTERN.matcher(legalDescription);
		if (matcher.find()) {
			resultMap.put("PropertyIdentificationSet.SubdivisionTract",
					matcher.group(1));
			legalDescription = legalDescription.replace(matcher.group(), " ");
		}
		matcher = LEGAL_BLOCK_PATTERN.matcher(legalDescription);
		if (matcher.find()) {
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock",
					matcher.group(1).replace(",",""));
			legalDescription = legalDescription.replace(matcher.group(), " ");
		}

	}

	public static void parseTaxes(NodeList nodeList, ResultMap resultMap,
			long searchId) {
		NodeList tableList = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(
						new HasAttributeFilter("border", "0"), true)
				.extractAllNodesThatMatch(
						new HasAttributeFilter("align", "left"), true)
				.extractAllNodesThatMatch(
						new HasAttributeFilter("width", "100%"), true);

		// get year
		String year = HtmlParser3.getValueFromAbsoluteCell(0, 0,
				HtmlParser3.findNode(nodeList, "Date"), "", true);
		year = year.substring(year.lastIndexOf("/") + 1);
		resultMap.put("TaxHistorySet.Year", year.trim());

		// get baseamount
		TableTag mainTable = (TableTag) tableList.elementAt(0);
		TableRow[] rows = mainTable.getRows();
		String basea = "";
		basea = rows[6].getChildren().elementAt(5).toHtml()
				.replaceAll("(?ism)<[^>]*>([^<]*)<[^>]*>", "$1").trim();
		basea = basea.replaceAll("[$,]", "");
		resultMap.put("TaxHistorySet.BaseAmount", basea);

		// get amountpaid
		String amountp = "";
		amountp = rows[8].getChildren().elementAt(5).toHtml()
				.replaceAll("(?ism)<[^>]*>([^<]*)<[^>]*>", "$1")
				.replaceAll("[$,]", "").trim();
		resultMap.put("TaxHistorySet.AmountPaid", amountp.trim());

		// get total due
		String totald = "";
		totald = rows[9].getChildren().elementAt(5).toHtml()
				.replaceAll("(?ism)<[^>]*>([^<]*)<[^>]*>", "$1")
				.replaceAll("[$,]", "").trim();
		resultMap.put("TaxHistorySet.TotalDue", totald.trim());

		// get delinquent
		NodeList sectableList = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("th"), true)
				.extractAllNodesThatMatch(
						new HasAttributeFilter("align", "right"), true)
				.extractAllNodesThatMatch(
						new HasAttributeFilter("scope", "col"), true);
		String delinquent = sectableList.toHtml()
				.replaceAll("(?ism)<[^>]*>([^<]*)<[^>]*>", "$1")
				.replaceAll("<[^>]*>", "").replaceAll("Grand Total:", "")
				.replaceAll("&nbsp;", "").replaceAll("[$,]", "").trim();

		resultMap.put("TaxHistorySet.PriorDelinquent", delinquent);

		// to do
		NodeList newtableList = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("table"), true);

		if (newtableList.size() > 1) {
			TableTag taxTable = (TableTag) newtableList.elementAt(0);
			TableTag taxTable1 = (TableTag) newtableList.elementAt(0);
			for (int i = 0; i < newtableList.size(); i++) {
				if (newtableList
						.elementAt(i)
						.toHtml()
						.contains(
								"<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"full-borderlightgray\">")) {
					taxTable = (TableTag) newtableList.elementAt(i);
					taxTable1 = (TableTag) newtableList.elementAt(i + 1);
				}
			}

			try {
				// TableTag receiptTable = (TableTag) tableList.elementAt(1);
				// rows = receiptTable.getRows();
				rows = taxTable.getRows();

				ResultTable receipts = new ResultTable();
				Map<String, String[]> map = new HashMap<String, String[]>();
				String[] header = { "ReceiptAmount", "ReceiptDate" };
				List<List<String>> bodyRT = new ArrayList<List<String>>();

				for (int i = 1; i < rows.length; i++) {
					TableColumn[] smallReceiptColumns = rows[i].getColumns();
					if (smallReceiptColumns.length == 4) {
						List<String> paymentRow = new ArrayList<String>();
						paymentRow.add(smallReceiptColumns[2]
								.toPlainTextString().trim()
								.replaceAll("[$,]", ""));
						paymentRow.add(smallReceiptColumns[0]
								.toPlainTextString().trim());
						bodyRT.add(paymentRow);
					}
				}

				rows = taxTable1.getRows();
				for (int i = 0; i < rows.length; i++) {
					TableColumn[] smallReceiptColumns = rows[i].getColumns();
					if (smallReceiptColumns.length == 4) {
						List<String> paymentRow = new ArrayList<String>();
						paymentRow.add(smallReceiptColumns[2]
								.toPlainTextString().trim()
								.replaceAll("[$,]", ""));
						paymentRow.add(smallReceiptColumns[0]
								.toPlainTextString().trim());
						bodyRT.add(paymentRow);
					}
				}

				map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
				map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
				receipts.setHead(header);
				receipts.setMap(map);
				receipts.setBody(bodyRT);
				receipts.setReadOnly();
				resultMap.put("TaxHistorySet", receipts);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		// TODO Auto-generated method stub

		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");

		TableColumn[] cols = row.getColumns();
		if (cols.length == 3) {
			resultMap.put("PropertyIdentificationSet.ParcelID", cols[0]
					.toPlainTextString().replaceAll("-", "").trim());
			resultMap.put("PropertyIdentificationSet.ParcelIDParcel", cols[0]
					.toPlainTextString().replaceAll("-", "").trim());

			resultMap.put("PropertyIdentificationSet.NameOnServer", cols[1]
					.toPlainTextString().trim());

			String[] address = StringFormats.parseAddressShelbyAO(cols[2]
					.toPlainTextString().trim());

			resultMap.put("PropertyIdentificationSet.StreetNo", address[0]);
			resultMap.put("PropertyIdentificationSet.StreetName", address[1]);

			parseNames(resultMap, searchId);

		}
		resultMap.removeTempDef();
		return resultMap;
	}

	public static String parseDate(String taxHistoryHtml) {
		String res = "";
		String regex = "(\\d\\d).[A-Z][A-Z][A-Z].(\\d\\d)";
		Pattern p = Pattern.compile(regex);
		res = taxHistoryHtml;
		Matcher m = p.matcher(res);
		while (m.find()) {
			String date = m.group();
			String day = date.substring(0, date.indexOf("-"));
			String month = date.substring(date.indexOf("-") + 1,
					date.lastIndexOf("-"));
			String year = date.substring(date.lastIndexOf("-") + 1);
			date = StringFormats.MonthNo(day + "-" + month + "-20" + year);
			res = res.replace(m.group(), date);
		}
		return res;
	}
}

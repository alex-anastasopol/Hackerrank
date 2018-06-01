package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.SpecialAssessmentSet.SpecialAssessmentSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 16, 2012
 */

public class OHFranklinTR {
	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo) {
		ResultMap m = new ResultMap();
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

		TableColumn[] cols = row.getColumns();

		if (cols.length == 4) {
			String parcel = "";
			String addr = "";
			String owner = "";
			String legal = "";

			switch (additionalInfo) {
			case 2:
			case 3:
				parcel = cols[0].toPlainTextString().trim();
				addr = cols[1].toPlainTextString().trim();
				owner = cols[2].toPlainTextString().trim();
				legal = cols[3].toPlainTextString().trim();
				break;
			case 1:
				owner = cols[0].toPlainTextString().trim();
				parcel = cols[1].toPlainTextString().trim();
				addr = cols[2].toPlainTextString().trim();
				legal = cols[3].toPlainTextString().trim();
				break;
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(parcel)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(owner)) {
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(owner)) {
					List<String> names = new ArrayList<String>();
					names.add(owner);
					parseNames(m, names, "");
				}
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(addr)) {
				parseAddress(m, addr);
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(legal)) {
				parseLegal(m, legal);
			}
		}
		return m;
	}

	public static void parseNames(ResultMap m, List<String> all_names, String auxString) {
		try {
			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();

			StringBuffer nameOnServer = new StringBuffer();

			for (String n : all_names) {
				n = n.toUpperCase().trim().replaceAll("\\s+", " ");
				n = n.replaceAll("\\sAND\\s", " & ");
				n = n.replaceAll("(?is)\\bCO\\s*(TRUSTEES?)\\b", "$1");
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(n)) {
					nameOnServer.append(n + " & ");
					String[] names = StringFormats.parseNameNashville(n, true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
			}
			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), StringUtils.strip(nameOnServer.toString()).replaceAll("\\&$", ""));
			if (body.size() > 0)
				GenericFunctions.storeOwnerInPartyNames(m, body, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAddress(ResultMap m, String addr) {
		addr = StringUtils.strip(StringUtils.defaultString(addr));

		if (StringUtils.isEmpty(addr))
			return;

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr.replaceAll("\\s+", " "));

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(addr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(addr)));
	}

	public static void parseLegal(ResultMap m, String legal) {
		if (StringUtils.isEmpty(legal))
			return;

		String legalDes = legal;

		m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDes);

		Pattern LEGAL_LOT = Pattern.compile("\\bLOT ?(\\d+)\\b");

		Matcher matcher = LEGAL_LOT.matcher(legalDes);

		if (matcher.find()) {
			String lot = matcher.group(1);
			if (StringUtils.isNotEmpty(lot)) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			}
			legalDes = legalDes.replaceFirst(matcher.group(), " ");
		}
	}

	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

			NodeList nodes = new HtmlParser3(detailsHtml
					.replaceAll("(?ism)&nbsp;", " ")
					.replaceAll("(?ism)</?center>", "")
					.replaceAll("(?ism)</?font[^>]*>", "")
					).getNodeList();

			NodeList tables = nodes.extractAllNodesThatMatch(new HasAttributeFilter("id", "detailsTable"), true);

			if (tables.size() == 0) {
				return null;
			}

			String parcel = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(tables, "Parcel ID"), "", false).trim();
			if (StringUtils.isNotEmpty(parcel)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
			}

			String owner = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Current Owner(s)"), "", false).trim();
			if (StringUtils.isNotEmpty(owner)) {
				List<String> names = new ArrayList<String>();
				names.add(owner);
				parseNames(resultMap, names, "");
			}

			String addr = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(tables, "Address"), "", false).trim().replaceAll("\\s+", " ");
			if (StringUtils.isNotEmpty(addr)) {
				parseAddress(resultMap, addr);
			}

			Node n = getNodeByTypeAttributeDescription(nodes, "table", "bordercolor", "Yellow", new String[] { "Total Owed", "Total Paid", "Balance Due" },
					true);

			if (n != null) {
				TableTag taxes = (TableTag) n;
				TableRow[] rows = taxes.getRows();

				double amountPaid = 0;
				double amountDue = 0;
				double priorDelinquent = 0;
				double baseAmount = 0;

				for (int i = 0; i < rows.length; i++) {
					TableRow r = rows[i];
					if (r.getColumnCount() == 7) {
						if (r.getColumns()[0].toPlainTextString().contains("Net Total")) {
							String hf1 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
							String hf2 = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
							double hf1d = StringUtils.isEmpty(hf1) ? 0 : Double.parseDouble(hf1);
							double hf2d = StringUtils.isEmpty(hf2) ? 0 : Double.parseDouble(hf2);

							baseAmount = hf1d + hf2d;
						}

						if (r.getColumns()[0].toPlainTextString().contains("Total Paid")) {
							String hf1 = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
							String hf2 = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
							double hf1d = StringUtils.isEmpty(hf1) ? 0 : Double.parseDouble(hf1);
							double hf2d = StringUtils.isEmpty(hf2) ? 0 : Double.parseDouble(hf2);

							amountPaid = hf1d + hf2d;
						}

						if (r.getColumns()[0].toPlainTextString().contains("Balance Due")) {
							String prior = r.getColumns()[1].toPlainTextString().replaceAll("[ $,-]", "");
							String hf2 = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
							double priord = StringUtils.isEmpty(prior) ? 0 : Double.parseDouble(prior);
							double hf2d = StringUtils.isEmpty(hf2) ? 0 : Double.parseDouble(hf2);

							if (hf2d - priord > 0)
								amountDue = hf2d - priord;

							priorDelinquent = priord;
						}
					}
				}

				resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), Double.toString(baseAmount).replaceAll("(\\d+\\.\\d{2})\\d+", "$1"));
				resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), Double.toString(amountPaid).replaceAll("(\\d+\\.\\d{2})\\d+", "$1"));
				resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), Double.toString(amountDue).replaceAll("(\\d+\\.\\d{2})\\d+", "$1"));
				resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), Double.toString(priorDelinquent).replaceAll("(\\d+\\.\\d{2})\\d+", "$1"));

			}

			Date dateNow = new Date();
			DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
			int currentYear = Calendar.getInstance().get(Calendar.YEAR);

			int taxYear = currentYear;
			Date payDate = df.parse("12/15/" + currentYear);
			if (dateNow.before(payDate)) {
				taxYear = currentYear - 1;
			}

			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), String.valueOf(taxYear));

			// related docs
			if (n != null) {
				TableTag t = (TableTag) n;
				TableRow[] rows = t.getRows();

				String[] rcptHeader = { TaxHistorySetKey.RECEIPT_DATE.getShortKeyName(), TaxHistorySetKey.RECEIPT_AMOUNT.getShortKeyName() };
				List<List<String>> rcptBody = new ArrayList<List<String>>();
				List<String> rcptRow = new ArrayList<String>();
				ResultTable resT = new ResultTable();
				Map<String, String[]> rcptMap = new HashMap<String, String[]>();
				for (String s : rcptHeader) {
					rcptMap.put("TaxHistorySet." + s, new String[] { s, "" });
				}

				for (int i = 1; i < rows.length; i++) {
					TableRow r = rows[i];
					if (r.getColumnCount() == 7 && r.getColumns()[0].toPlainTextString().trim().matches("\\d{2}/\\d{2}/\\d{2}")) {
						String hf1 = r.getColumns()[4].toPlainTextString().replaceAll("[ $,-]", "");
						String hf2 = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
						double hf1d = StringUtils.isEmpty(hf1) ? 0 : Double.parseDouble(hf1);
						double hf2d = StringUtils.isEmpty(hf2) ? 0 : Double.parseDouble(hf2);

						String date = r.getColumns()[0].toPlainTextString().trim();
						String amount = Double.toString(hf1d + hf2d);

						rcptRow = new ArrayList<String>();
						rcptRow.add(date);
						rcptRow.add(amount.replaceAll("(\\d+\\.\\d{1,2})\\d+", "$1"));
						rcptBody.add(rcptRow);
					}
				}

				if (rcptBody.size() > 0) {
					resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), rcptBody.get(0).get(0));
							
					resT.setHead(rcptHeader);
					resT.setMap(rcptMap);
					resT.setBody(rcptBody);
					resT.setReadOnly();
					resultMap.put("TaxHistorySet", resT);
				}
			}

			// compute assessment taxes from SpecialAssessmentSet
			if (n != null) {
				String[][] sa = new String[4][3];

				TableTag t = (TableTag) n;
				TableRow[] rows = t.getRows();

				boolean sa0 = false;
				boolean sa1 = false;
				boolean sa2 = false;
				boolean sa3 = false;

				for (int i = 1; i < rows.length; i++) {
					TableRow r = rows[i];
					if (r.getColumnCount() == 7) {
						if (r.getColumns()[0].toPlainTextString().trim().equals("Charge")) {
							sa[0][0] = r.getColumns()[1].toPlainTextString().replaceAll("[ $,-]", "");
							sa[0][1] = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
							sa[0][2] = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
							sa0 = true;
						}
						if (r.getColumns()[0].toPlainTextString().trim().equals("Penalty / Int")) {
							sa[1][0] = r.getColumns()[1].toPlainTextString().replaceAll("[ $,-]", "");
							sa[1][1] = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
							sa[1][2] = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
							sa1 = true;
						}
						if (r.getColumns()[0].toPlainTextString().trim().equals("Paid")) {
							sa[2][0] = r.getColumns()[1].toPlainTextString().replaceAll("[ $,-]", "");
							sa[2][1] = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
							sa[2][2] = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
							sa2 = true;
						}
						if (r.getColumns()[0].toPlainTextString().trim().equals("Owed")) {
							sa[3][0] = r.getColumns()[1].toPlainTextString().replaceAll("[ $,-]", "");
							sa[3][1] = r.getColumns()[3].toPlainTextString().replaceAll("[ $,-]", "");
							sa[3][2] = r.getColumns()[5].toPlainTextString().replaceAll("[ $,-]", "");
							sa3 = true;
						}
					}
				}

				if (sa0 && sa1 && sa2 && sa3) {

					BigDecimal saTotalDelinquent = new BigDecimal(0);
					BigDecimal saPriorDelinquent = new BigDecimal(0);
					BigDecimal saTotalPaid = new BigDecimal(0);
					BigDecimal saTotalDue = new BigDecimal(0);
					BigDecimal saBaseAmount = new BigDecimal(0);

					// prior delinquent
					saPriorDelinquent = saPriorDelinquent.add(new BigDecimal(sa[3][0]));

					// base amount
					saBaseAmount = saBaseAmount.add(new BigDecimal(sa[0][1])).add(new BigDecimal(sa[0][2]));

					// total paid
					saTotalPaid = saTotalPaid.add(new BigDecimal(sa[2][1])).add(new BigDecimal(sa[2][2]));

					// total due
					saTotalDue = saTotalDue.add(new BigDecimal(sa[3][1])).add(new BigDecimal(sa[3][2]));

					// total delinquent
					if (Double.parseDouble(sa[1][1]) != 0) {
						saTotalDelinquent = saTotalDelinquent.add(new BigDecimal(sa[3][1]));
					}
					if (Double.parseDouble(sa[1][2]) != 0) {
						saTotalDelinquent = saTotalDelinquent.add(new BigDecimal(sa[3][2]));
					}
					saTotalDelinquent = saTotalDelinquent.add(saPriorDelinquent);

					resultMap.put(SpecialAssessmentSetKey.PRIOR_DELINQUENT.getKeyName(), saPriorDelinquent.toString().replaceAll("(\\d+\\.\\d{2})\\d+", "$1"));
					resultMap.put(SpecialAssessmentSetKey.AMOUNT_PAID.getKeyName(), saTotalPaid.toString().replaceAll("(\\d+\\.\\d{2})\\d+", "$1"));
					resultMap.put(SpecialAssessmentSetKey.TOTAL_DUE.getKeyName(), saTotalDue.toString().replaceAll("(\\d+\\.\\d{2})\\d+", "$1"));
					resultMap.put(SpecialAssessmentSetKey.BASE_AMOUNT.getKeyName(), saBaseAmount.toString().replaceAll("(\\d+\\.\\d{2})\\d+", "$1"));
					resultMap.put(SpecialAssessmentSetKey.YEAR.getKeyName(), String.valueOf(taxYear));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Node getNodeByTypeAttributeDescription(NodeList nl, String type, String attributeName, String attributeValue, String[] description,
			boolean recursive) {
		NodeList returnList = null;

		if (!StringUtils.isEmpty(attributeName))
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
					new HasAttributeFilter(attributeName, attributeValue), recursive);
		else
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive);

		for (int i = returnList.size() - 1; i >= 0; i--) {
			boolean flag = true;
			for (String s : description) {
				if (!StringUtils.containsIgnoreCase(returnList.elementAt(i).toHtml(), s)) {
					flag = false;
					break;
				}
			}
			if (flag)
				return returnList.elementAt(i);
		}

		return null;
	}
}

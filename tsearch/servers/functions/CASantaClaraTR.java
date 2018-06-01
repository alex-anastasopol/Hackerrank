package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.Tidy;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 26, 2012
 */

public class CASantaClaraTR {

	public static ResultMap parseIntermediaryRow(TableRow row, Integer viParseId) {
		ResultMap m = new ResultMap();
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

		TableColumn[] tcs = row.getColumns();

		if (tcs.length == 3) {
			// parcel
			String parcel = tcs[1].toPlainTextString().trim();

			if (StringUtils.isNotEmpty(parcel)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel.replaceAll("[^\\d]", ""));
				m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), parcel);
			}

			// address
			String address = tcs[2].toPlainTextString();

			if (StringUtils.isNotEmpty(parcel))
				parseAddress(m, address);

		}

		return m;
	}

	public static void parseAddress(ResultMap m, String addr) {
		addr = StringUtils.strip(StringUtils.defaultString(addr));

		if (StringUtils.isEmpty(addr))
			return;

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr.replaceAll("\\s+", " "));

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(addr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(addr)));
	}

	public static void parseTaxes(ResultMap m, NodeList nodes) {
		try {
			TableTag t = (TableTag) nodes.elementAt(0);

			if (t.getRowCount() == 2 && t.getRows()[1].getColumns()[0].getChildCount() > 1 && t.getRows()[1].getColumns()[0].childAt(1) instanceof TableTag) {
				TableTag taxesTable = (TableTag) t.getRows()[1].getColumns()[0].childAt(1);
				TableRow[] rows = taxesTable.getRows();

				String[] rcptHeader = { TaxHistorySetKey.YEAR.getShortKeyName(), TaxHistorySetKey.BASE_AMOUNT.getShortKeyName(),
						TaxHistorySetKey.AMOUNT_PAID.getShortKeyName() };
				List<List<String>> rcptBody = new ArrayList<List<String>>();
				List<String> rcptRow = new ArrayList<String>();
				ResultTable resT = new ResultTable();
				Map<String, String[]> rcptMap = new HashMap<String, String[]>();
				for (String s : rcptHeader) {
					rcptMap.put("TaxHistorySet." + s, new String[] { s, "" });
				}

				String year = "";

				for (int i = 1; i < rows.length; i++) {
					TableRow r = rows[i];
					TableColumn[] cols = r.getColumns();

					if (cols.length >= 8) {
						String baseAmount = "";
						String amountPaid = "";

						if (cols.length == 9) {
							year = cols[0].toPlainTextString().split("-")[0];

							baseAmount = cols[3].toPlainTextString().replaceAll("[^\\d.]", "");
							amountPaid = cols[7].toPlainTextString().replaceAll("[^\\d.]", "");

						} else if (cols.length == 8) {
							baseAmount = cols[2].toPlainTextString().replaceAll("[^\\d.]", "");
							amountPaid = cols[6].toPlainTextString().replaceAll("[^\\d.]", "");
						}
						rcptRow = new ArrayList<String>();
						rcptRow.add(year);
						rcptRow.add(baseAmount);
						rcptRow.add(amountPaid);
						rcptBody.add(rcptRow);
					}
				}

				if (rcptBody.size() > 0) {
					resT.setHead(rcptHeader);
					resT.setMap(rcptMap);
					resT.setBody(rcptBody);
					resT.setReadOnly();
					m.put("TaxHistorySet", resT);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			NodeList nodes = new HtmlParser3(Tidy.tidyParse(detailsHtml.replaceAll("(?ism)(</?)th", "$1td"), null)).getNodeList();

			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			if (tables.size() > 0) {
				NodeList summaryTList = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "SummaryTable"));

				if (summaryTList.size() > 0) {
					String apn = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summaryTList, "Parcel Number (APN)"), "", false).trim();

					if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(apn)) {
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), apn.replaceAll("[^\\d]", ""));
					}

					String year = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summaryTList, "Fiscal Year, Tax Rate Area"), "", false)
							.trim();
					if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(year)) {
						resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year.split("-")[0]);
					}
					
					String taxRateArea = HtmlParser3
							.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(summaryTList, "Fiscal Year, Tax Rate Area"), "", false)
							.trim();
					if (StringUtils.isNotEmpty(taxRateArea)) {
						resultMap.put(PropertyIdentificationSetKey.AREA_CODE.getKeyName(), taxRateArea);
					}

					String address = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(summaryTList, "Property Address"), "", false).trim();
					if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(address)) {
						parseAddress(resultMap, address);
					}

					String cityZip = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(summaryTList, "Property Address"), "", false).trim();
					if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(cityZip) && cityZip.contains(",")) {
						resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), cityZip.split(",")[0]);
						resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), cityZip.split(",")[1].replaceAll("[^\\d]", ""));
					}

					// get BA, AD
					NodeList nodesAux = summaryTList.extractAllNodesThatMatch(new HasAttributeFilter("width", "800"), true);

					if (nodesAux.size() > 0) {

						TableTag t = (TableTag) nodesAux.elementAt(0);

						if (t.getRowCount() >= 3 && t.getRow(0).getColumnCount() == 10) {

							String baseAmount1 = t.getRow(1).getColumns()[2].toPlainTextString().replaceAll("[^\\d.]", "");
							String baseAmount2 = t.getRow(2).getColumns()[1].toPlainTextString().replaceAll("[^\\d.]", "");

							String amountDue1 = t.getRow(1).getColumns()[6].toPlainTextString().replaceAll("[^\\d.]", "");
							String amountDue2 = t.getRow(2).getColumns()[5].toPlainTextString().replaceAll("[^\\d.]", "");

							String status1 = t.getRow(1).getColumns()[9].toPlainTextString().trim().toUpperCase();
							String status2 = t.getRow(2).getColumns()[8].toPlainTextString().trim().toUpperCase();

							String penalty1 = t.getRow(1).getColumns()[3].toPlainTextString().replaceAll("[^\\d.]", "");
							String penalty2 = t.getRow(2).getColumns()[2].toPlainTextString().replaceAll("[^\\d.]", "");

							String amountPaid1 = "";
							String amountPaid2 = "";

							String delinquentAmount = "";

							if (t.toHtml().contains("Delinquent") && t.getRowCount() > 3) {
								delinquentAmount = t.getRow(3).getColumns()[6].toPlainTextString().replaceAll("[^\\d.]", "");
							}
							
							String lastDatePaid = "";
							lastDatePaid = t.getRow(2).getColumns()[7].toPlainTextString().trim();
							if (!lastDatePaid.matches("\\d+/\\d+/\\d+")) {
								lastDatePaid = t.getRow(1).getColumns()[8].toPlainTextString().trim();
							}
							if (StringUtils.isNotEmpty(lastDatePaid)) {
								resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), lastDatePaid);
							}

							double ba = 0;
							double ad = 0;
							double ap = 0;
							double da = 0;

							if (StringUtils.isNotEmpty(baseAmount1)) {
								ba += Double.parseDouble(baseAmount1);
							}

							if (StringUtils.isNotEmpty(baseAmount2)) {
								ba += Double.parseDouble(baseAmount2);
							}

							if (StringUtils.isNotEmpty(amountDue1) && status1.contains("PAID")) {
								amountPaid1 = amountDue1;
								ap += Double.parseDouble(amountDue1);
							} else if (StringUtils.isNotEmpty(amountDue1) && status1.contains("DUE")) {
								ad += Double.parseDouble(amountDue1);
							}

							if (StringUtils.isNotEmpty(amountDue2) && status2.contains("PAID")) {
								amountPaid2 = amountDue2;
								ap += Double.parseDouble(amountDue2);
							} else if (StringUtils.isNotEmpty(amountDue1) && status2.contains("DUE")) {
								ad += Double.parseDouble(amountDue2);
							}

							if (StringUtils.isNotBlank(delinquentAmount)) {
								da += Double.parseDouble(delinquentAmount);
							}

							resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), Double.toString(ba));
							resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), Double.toString(ap));
							resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), Double.toString(ad));
							resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), Double.toString(da));

							// parse tax installment set
							List<List> bodyInstallments = new ArrayList<List>();

							Map<String, String[]> map = new HashMap<String, String[]>();
							ResultTable installmentsRT = new ResultTable();
							String[] installmentsHeader = {
									TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(),
									TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
									TaxInstallmentSetKey.PENALTY_AMOUNT.getShortKeyName(),
									TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
									TaxInstallmentSetKey.STATUS.getShortKeyName(),
									TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName()
							};
							installmentsRT.setHead(installmentsHeader);

							for (String s : installmentsHeader) {
								map.put(s, new String[] { s, "" });
							}

							List<String> inst1Line = new ArrayList<String>();
							List<String> inst2Line = new ArrayList<String>();

							// inst name
							inst1Line.add("First Installment");
							inst2Line.add("Second Installment");

							// inst base amount
							inst1Line.add(baseAmount1);
							inst2Line.add(baseAmount2);

							// inst penalty
							inst1Line.add(penalty1);
							inst2Line.add(penalty2);

							// inst amount due
							if (!status1.equals("PAID")) {
								inst1Line.add(amountDue1);
							} else {
								inst1Line.add("0.0");
							}
							if (!status2.equals("PAID")) {
								inst2Line.add(amountDue2);
							} else {
								inst2Line.add("0.0");
							}

							// inst status
							inst1Line.add(status1);
							inst2Line.add(status2);

							// inst amount paid
							inst1Line.add(amountPaid1);
							inst2Line.add(amountPaid2);

							if (inst1Line.size() == installmentsHeader.length) {
								bodyInstallments.add(inst1Line);
							}
							if (inst2Line.size() == installmentsHeader.length) {
								bodyInstallments.add(inst2Line);
							}

							if (!bodyInstallments.isEmpty()) {
								installmentsRT.setHead(installmentsHeader);
								installmentsRT.setBody(bodyInstallments);
								installmentsRT.setMap(map);
								installmentsRT.setReadOnly();
								resultMap.put("TaxInstallmentSet", installmentsRT);
							}
						}
					}
				}

				NodeList detailsList = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "details"));

				if (detailsList.size() > 0) {
					String land = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(detailsList, "Land"), "", false).replaceAll("[^\\d.]", "");
					String improvements = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(detailsList, "Improvements"), "", false)
							.replaceAll("[^\\d.]", "");
					String totalAppraisal = HtmlParser3
							.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(detailsList, "Total Land and Improvements"), "", false)
							.replaceAll("[^\\d.]", "");
					String totalAssesment = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(detailsList, "Total Value"), "", false)
							.replaceAll("[^\\d.]", "");

					if (StringUtils.isNotEmpty(land)) {
						resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
					}

					if (StringUtils.isNotEmpty(improvements)) {
						resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvements);
					}

					if (StringUtils.isNotEmpty(totalAppraisal)) {
						resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), totalAppraisal);
					}

					if (StringUtils.isNotEmpty(totalAssesment)) {
						resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAssesment);
					}
				}

				NodeList historyList = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "history"));

				if (historyList.size() > 0) {
					parseTaxes(resultMap, historyList);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}

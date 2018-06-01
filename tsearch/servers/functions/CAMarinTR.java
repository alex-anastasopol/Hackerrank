package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.htmlparser.Node;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class CAMarinTR {

	public static ResultMap parseIntermediaryRow(TableRow row) {
		ResultMap resultMap = new ResultMap();
		try {
			TableColumn[] cols = row.getColumns();
			if (cols.length >= 6) {
				String year = StringUtils.extractParameter(cols[0].toPlainTextString(), "\\s*(.*?)\\s*/\\d+\\s*");
				String taxBillNr = cols[1].toPlainTextString().trim();
				String names = cols[2].toPlainTextString().trim();
				ro.cst.tsearch.extractor.xml.GenericFunctions2.parseName(resultMap, names);
				if (!year.isEmpty()) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
				}
				if (!taxBillNr.isEmpty()) {
					resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), taxBillNr);
				}
				if (!names.isEmpty()) {
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), names);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultMap;
	}

	@SuppressWarnings({ "rawtypes", "unused" })
	public static void parseAndFillResultMap(String detailsHtml, ResultMap resultMap, long searchId) {
		try {
			int tdOffset = 1;

			String value = "";
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			NodeList nodeList = htmlParser3.getNodeList();

			// get owner(s)
			value = getValueForLabel(nodeList, "Owner", tdOffset);
			if (!value.isEmpty()) {
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), value);
				ro.cst.tsearch.extractor.xml.GenericFunctions2.parseName(resultMap, value);
			}

			// get parcel id
			value = getValueForLabel(nodeList, "Parcel Number", tdOffset).replaceAll("[^\\d-]", "");
			if (!value.isEmpty()) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), value);
			}

			// get get bill no
			value = getValueForLabel(nodeList, "Bill Number", tdOffset);
			if (!value.isEmpty()) {
				resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), value);
			}

			// get tax rate area
			value = getValueForLabel(nodeList, "Tax Rate Area", tdOffset);
			if (!value.isEmpty()) {
				resultMap.put(PropertyIdentificationSetKey.AREA_CODE.getKeyName(), value);
			}

			// get tax roll year
			value = getValueForLabel(nodeList, "Tax Roll Year", tdOffset);
			if (!value.isEmpty()) {
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), value);
			}

			// get land
			value = getValueForLabel(nodeList, "Land Value", tdOffset).replaceAll("[^\\d.]", "");
			if (!value.isEmpty()) {
				resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), value);
			}

			// get improvement
			value = getValueForLabel(nodeList, "Improvement", tdOffset).replaceAll("[^\\d.]", "");
			if (!value.isEmpty()) {
				resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), value);
			}

			// get home exemption
			String homeExemption = getValueForLabel(nodeList, "Home Exemption", tdOffset).replaceAll("[^\\d.]", "");

			// get other exemption
			String otherExemptions = getValueForLabel(nodeList, "Other Exemption", tdOffset).replaceAll("[^\\d.]", "");

			if (!homeExemption.isEmpty() || !otherExemptions.isEmpty()) {
				resultMap.put(TaxHistorySetKey.TAX_EXEMPTION_AMOUNT.getKeyName(), GenericFunctions.sum(homeExemption + "+" + otherExemptions, searchId));
			}

			// get base amount
			value = getValueForLabel(nodeList, "Total Tax", tdOffset).replaceAll("[^\\d.]", "");
			if (!value.isEmpty()) {
				resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), value);
			}

			// get amount paid
			value = getValueForLabel(nodeList, "Amound Paid", tdOffset).replaceAll("[^\\d.]", "");
			if (!value.isEmpty()) {
				resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), value);
			}

			// get total due
			value = getValueForLabel(nodeList, "Total Due", tdOffset).replaceAll("[^\\d.]", "");
			if (!value.isEmpty()) {
				resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), value);
			}

			Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
			TableTag installmentsTable = null;
			List<List> installmentsBody = new ArrayList<List>();

			String[] amountPaid = { "", "" };
			ResultTable resultTable = new ResultTable();

			String[] installmentsStatus = new String[2]; // get statuses for installments from intermediary row(through details)
			Node installment1Node = htmlParser3.getNodeById("installment1Status");
			if (installment1Node != null) {
				installmentsStatus[0] = installment1Node.toPlainTextString().toUpperCase();
			}
			Node installment2Node = htmlParser3.getNodeById("installment2Status");
			if (installment2Node != null) {
				installmentsStatus[1] = installment2Node.toPlainTextString().toUpperCase();
			}
			String[] installmentsHeader = {
					TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
					TaxInstallmentSetKey.PENALTY_AMOUNT.getShortKeyName(),
					TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
					TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(),
					TaxInstallmentSetKey.STATUS.getShortKeyName(),
					TaxInstallmentSetKey.TAX_BILL_TYPE.getShortKeyName()
			};

			for (String s : installmentsHeader) {
				installmentsMap.put(s, new String[] { s, "" });
			}

			String billType = "";
			billType = getValueForLabel(nodeList, "Bill Type", tdOffset);

			for (int i = 0; i < 2; i++) {
				List<String> installmentsRow = new ArrayList<String>();

				if (i == 0) {
					tdOffset = 3;
				} else {
					tdOffset = 2;
				}

				// get base amount per installment
				value = getValueForLabel(nodeList, "Total Tax", tdOffset).replaceAll("[^\\d.]", "");
				if (!value.isEmpty()) {
					installmentsRow.add(value);
				} else {
					installmentsRow.add("");
				}

				// get penalty per installment
				value = getValueForLabel(nodeList, "Penalty", tdOffset).replaceAll("[^\\d.]", "");
				if (!value.isEmpty()) {
					installmentsRow.add(value);
				} else {
					installmentsRow.add("");
				}
				// get total due per installment
				value = getValueForLabel(nodeList, "Total Due", tdOffset).replaceAll("[^\\d.]", "");
				if (!value.isEmpty()) {
					installmentsRow.add(value);
				} else {
					installmentsRow.add("");
				}

				// get amount paid per installment
				amountPaid[i] = getValueForLabel(nodeList, "Amound Paid", tdOffset).replaceAll("[^\\d.]", "");
				if (!amountPaid[i].isEmpty()) {
					installmentsRow.add(amountPaid[i]);
				} else {
					installmentsRow.add("");
				}

				installmentsRow.add(installmentsStatus[i]);
				installmentsRow.add(billType);

				if (installmentsRow.size() == installmentsHeader.length) {
					installmentsBody.add(installmentsRow);
				}
			}

			if (!installmentsBody.isEmpty()) {
				resultTable.setHead(installmentsHeader);
				resultTable.setMap(installmentsMap);
				resultTable.setBody(installmentsBody);
				resultTable.setReadOnly();
				resultMap.put("TaxInstallmentSet", resultTable);
			}

			// get receipts values and dates
			List<List> bodyHist = new ArrayList<List>();
			resultTable = new ResultTable();
			String[] header = { "ReceiptAmount", "ReceiptDate" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
			map.put("ReceiptDate", new String[] { "ReceiptDate", "" });

			for (int i = 0; i < 2; i++) {
				List<String> receiptRow = new ArrayList<String>();
				if (i == 0) {
					tdOffset = 3;
				} else {
					tdOffset = 2;
				}

				// get receipt value
				if (!amountPaid[i].isEmpty()) {
					receiptRow.add(amountPaid[i]);
				} else {
					receiptRow.add("");
				}

				// get receipt date
				value = getValueForLabel(nodeList, "Paid Date", tdOffset).replaceAll("[^\\d/]", "");
				if (!value.isEmpty() && value.matches("\\d+/\\d+/\\d+")) {
					receiptRow.add(value);
				} else {
					receiptRow.add("");
				}

				if (receiptRow.size() == header.length) {
					bodyHist.add(receiptRow);
				}
			}

			if (!bodyHist.isEmpty()) {
				resultTable.setHead(header);
				resultTable.setBody(bodyHist);
				resultTable.setMap(map);
				resultTable.setReadOnly();
				resultMap.put("TaxHistorySet", resultTable);
			}

			// -------------------
			// tis-delq date,?
			// remarks example ?
			// bill type?
			// bill date?
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getValueForLabel(NodeList nodeList, String label, int tdOffset) {
		String value = "";
		try {
			Node node = (Node) HtmlParser3.findNode(nodeList, label);
			if (node != null) {
				node = HtmlParser3.getFirstParentTag(node, TableRow.class);
				if (node != null) {
					TableRow row = (TableRow) node;
					int columnCount = row.getColumnCount();
					if (columnCount > 0) {
						node = (Node) row.getColumns()[columnCount - tdOffset];
						if (node != null) {
							value = node.toPlainTextString().trim();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ArrayList<List> removeDuplicates(ArrayList<List> list) {
		ArrayList<List> newList = new ArrayList<List>();
		for (List l : list) {
			// l.set(0, "");
			if (!newList.contains(l)) {
				newList.add(l);
			}
		}
		return newList;
	}

}

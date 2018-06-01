package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.htmlparser.Node;
import org.htmlparser.Text;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class CASantaCruzTR {

	@SuppressWarnings("rawtypes")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap resultMap, Search search, DataSite dataSite) {
		try {
			detailsHtml = detailsHtml.replaceAll("<th\\b", "<td");
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			NodeList nodeList = htmlParser3.getNodeList();

			resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");

			// get parcel ID; there is a special behavior for extra details rows - add S(for Supplemental) or E(for Escaped) to
			// the end of the instr no, followed by a step-by-step incremented index
			boolean isMainDoc = true;
			int extraDetailsIndex = 1;// extra doc pid will look like e.g. 029-163-54_S1 or 029-163-54-E4
			String parcelID = "";
			String letterToAdd = "";

			// get parcel ID
			Text parcelText = HtmlParser3.findNode(nodeList, "Parcel/Account #");

			if (parcelText != null) {
				parcelID = parcelText.toHtml().trim();
				parcelID = StringUtils.extractParameter(parcelID, "(?is)Parcel/Account\\s*#\\s*([\\d-\\s]+)$");
			}
			Node mainDoc = htmlParser3.getNodeById("mainDetailsRow");

			if (mainDoc != null) {// if it's the main details doc
				if (StringUtils.isNotEmpty(parcelID)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
				}
			} else {// if it's and extra details doc
				isMainDoc = false;

				if (search.getAdditionalInfo("extraDetailsIndex") != null) {
					extraDetailsIndex = Integer.parseInt(search.getAdditionalInfo("extraDetailsIndex").toString());
				}

				// get extra doc type: supplemental or escaped
				NodeList extraDocTables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "TaxBillContainer"), true);
				if (extraDocTables.size() > 0) {
					Node extraDocTable1 = extraDocTables.elementAt(0);
					TableTag extraDocTableT1 = (TableTag) extraDocTable1;
					if (extraDocTableT1.getRowCount() > 0) {
						TableRow firstRow = extraDocTableT1.getRows()[0];
						if (firstRow.getColumnCount() > 0) {
							String tableTitle = firstRow.getColumns()[0].toPlainTextString().trim();
							if (org.apache.commons.lang.StringUtils.containsIgnoreCase(tableTitle, "Supplemental Tax")) {
								letterToAdd = "S";
							} else if (org.apache.commons.lang.StringUtils.containsIgnoreCase(tableTitle, "Escaped Assessment Tax")) {
								letterToAdd = "E";
							}
						}
					}
				}

				if (!letterToAdd.isEmpty()) {
					// modify parcel only if it's a supplemental or escaped details doc
					// otherwise, if it's a main doc from a prev year (for e.g. 076-192-15), leave it the way it is !!
					parcelID = parcelID + "_" + letterToAdd + extraDetailsIndex;
					extraDetailsIndex++;
					search.setAdditionalInfo("extraDetailsIndex", String.valueOf(extraDetailsIndex));
				}

				if (StringUtils.isNotEmpty(parcelID)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
				}
			}

			// get street no, street name and city
			NodeList situsAddressRows = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "situsAddressRow"), true);
			if (situsAddressRows.size() > 0) {
				Node situsAddressRow = situsAddressRows.elementAt(0);

				if (situsAddressRow != null) {
					String situsAddress = situsAddressRow.toPlainTextString().replaceFirst("(?is)(Situs|Mailing)\\s+Address", "").trim();
					if (StringUtils.extractParameter(situsAddress, "(?is)((?:\\bAIRPORT\\s+HANGAR\\b)|(?:HARBOR\\s+SLIP))").isEmpty()) {
						// if the address contains AIRPORT HANGAR, it's not an address !! e.g. 505-940-09, with situs address '0 AIRPORT HANGAR Z-6,'

						String streetNo = StringUtils.extractParameter(situsAddress, "^\\s*(\\d+)");
						situsAddress = situsAddress.replaceFirst("\\s*" + streetNo + "\\s*", "");
						streetNo = streetNo.replaceFirst("^0+", "");

						String city = StringUtils.extractParameter(situsAddress, ".*?,(.*)$").trim();
						situsAddress = situsAddress.replaceFirst("\\s*,\\s*" + city + "\\s*", "");

						if (StringUtils.isNotEmpty(streetNo)) {
							resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
						}

						if (StringUtils.isNotEmpty(situsAddress)) {
							String unitPattern = "\\b((\\w{1,2}\\s?-\\s??)?\\d+)$";
							if (!StringUtils.extractParameter(situsAddress, unitPattern).isEmpty()) {
								situsAddress = situsAddress.replaceFirst(unitPattern, "#$1");// sometimes unit is not recognized because it doesn't have #
																								// appended
							}
							resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), situsAddress);
						}

						if (StringUtils.isNotEmpty(city)) {
							if (org.apache.commons.lang.StringUtils.containsIgnoreCase(city, "BCH")) {
								// e.g. pid: 045-193-14 -> city - 'LA SELVA BCH'; transform 'bch' in 'beach'
								city = city.replaceFirst("\\bBCH\\b", "BEACH");
							}

							resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
						}

					} else {// it's not real estate
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "");
					}
				}
			}

			Node detailsL1 = null;
			Node detailsL2 = null;
			NodeList detailsL1Children = null;
			NodeList detailsL2Children = null;
			String baseAmount = "";
			String totalDue = "";
			String amountPaid = "";

			if (isMainDoc) {
				detailsL1 = htmlParser3.getNodeById("details0L1");
				detailsL2 = htmlParser3.getNodeById("details0L2");
			} else {
				NodeList detailsL1Nodes = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "extraDetailsL1"), true);
				if (detailsL1Nodes.size() > 0) {
					detailsL1 = detailsL1Nodes.elementAt(0);
				}
				NodeList detailsL2Nodes = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "extraDetailsL2"), true);
				if (detailsL2Nodes.size() > 0) {
					detailsL2 = detailsL2Nodes.elementAt(0);
				}
			}

			if (detailsL1 != null) {
				detailsL1Children = detailsL1.getChildren();
			}

			if (detailsL2 != null) {
				detailsL2Children = detailsL2.getChildren();

				// get tax rate area
				Text taxRateAreaText = HtmlParser3.findNode(detailsL2Children, "Tax Rate Area");
				if (taxRateAreaText != null) {
					String taxRateArea = taxRateAreaText.toPlainTextString().trim();
					taxRateArea = StringUtils.extractParameter(taxRateArea, "(?is)Tax\\s+Rate\\s+Area\\s*(\\d+)\\b");
					if (StringUtils.isNotEmpty(taxRateArea)) {
						resultMap.put(PropertyIdentificationSetKey.AREA_CODE.getKeyName(), taxRateArea);
					}
				}

				// get land and improvements
				String land = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(detailsL2Children, "LAND"), "", true)
						.replaceAll("[^\\d.]", "");
				if (StringUtils.isNotEmpty(land)) {
					resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
				}
				String improvements = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(detailsL2Children, "IMPROVEMENT"), "", true)
						.replaceAll("[^\\d.]", "");
				if (StringUtils.isNotEmpty(improvements)) {
					resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvements);
				}

				// get base amount
				baseAmount = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(detailsL2Children, "TOTAL TAXES"), "", true)
						.replaceAll("[^\\d.]", "");
				if (StringUtils.isNotEmpty(baseAmount)) {
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
					totalDue = baseAmount;
				}

				// get exemptions
				String exemptions = "";
				int offsetRow = -1;
				String testIfOtherValueRow = HtmlParser3.getValueFromAbsoluteCell(offsetRow, 0,
						HtmlParser3.findNode(detailsL2Children, "NET TAXABLE VALUE"), "", true).trim();

				while (offsetRow > -7 && StringUtils.extractParameter(testIfOtherValueRow, "(?is)(^LESS\\s+EXEMPTION)").isEmpty()) {
					exemptions += HtmlParser3
							.getValueFromAbsoluteCell(offsetRow, 1, HtmlParser3.findNode(detailsL2Children, "NET TAXABLE VALUE"), "", true)
							.replaceAll("[^\\d.]", "") + "+";
					testIfOtherValueRow = HtmlParser3.getValueFromAbsoluteCell(--offsetRow, 0,
							HtmlParser3.findNode(detailsL2Children, "NET TAXABLE VALUE"), "", true).trim();
				}

				if (StringUtils.isNotEmpty(exemptions)) {
					exemptions = exemptions.replaceFirst("\\+$", "");
					resultMap.put(TaxHistorySetKey.TAX_EXEMPTION_AMOUNT.getKeyName(), GenericFunctions1.sum(exemptions, search.getSearchID()));
				}
			}

			String[] installmentName = { "First Installment", "Second Installment" };
			String[] installmentStatus = { "", "" };
			String[] installmentBaseAmount = { "", "" };
			String[] installmentPenalty = { "", "" };
			String[] installmentAmount = { "", "" };

			String[] installmentDatePaid = { "", "" };

			// get installements' date paid to determine payment status
			TableTag installmentsTableT = HtmlParser3.getFirstTag(detailsL1Children, TableTag.class, true);
			int noOfInstallmentRows = 2;
			if (installmentsTableT != null) {
				if (installmentsTableT.getRowCount() == 3) {
					noOfInstallmentRows = 1;
				}
			}
			for (int i = 0; i < noOfInstallmentRows; i++) {

				installmentDatePaid[i] = HtmlParser3.getValueFromAbsoluteCell(i + 1, 0, HtmlParser3.findNode(detailsL1Children, "Paid Date"), "", true);
				if (StringUtils.isNotEmpty(installmentDatePaid[i])) {
					installmentDatePaid[i] = installmentDatePaid[i].replaceAll("\\s+", "");
					if (installmentDatePaid[i].matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
						installmentStatus[i] = "PAID";
					} else {
						if (!StringUtils.extractParameter(installmentDatePaid[i], "(?is)(NotPaid)").isEmpty()) {
							installmentStatus[i] = "UNPAID";
						} else if (!StringUtils.extractParameter(installmentDatePaid[i], "(?is)(PastDue)").isEmpty()) {
							installmentStatus[i] = "PASTDUE";
						}

						installmentDatePaid[i] = "";
					}
				}
			}

			// get installment amount; it's due or paid amount - depends if paid date is present
			installmentAmount[0] = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(detailsL1Children, "Amount"), "", true)
					.replaceAll("[^\\d.]", "");
			if (noOfInstallmentRows == 2) {
				installmentAmount[1] = HtmlParser3.getValueFromAbsoluteCell(2, 0, HtmlParser3.findNode(detailsL1Children, "Amount"), "", true)
						.replaceAll("[^\\d.]", "");
			}

			boolean atLeastOneDelqInst = installmentStatus[0].equals("PASTDUE") || installmentStatus[1].equals("PASTDUE");
			Node installmentsL2Table = HtmlParser3.getNodeByTypeAttributeDescription(detailsL2Children, "table", "", "",
					new String[] { "FIRST INSTALLMENT", "SECOND INSTALLMENT" }, true);
			if (installmentsL2Table != null) {
				NodeList installmentsL2TableChildren = installmentsL2Table.getChildren();

				// if installment due/paid amount(s) empty(in L1 table value could be e.g 'Defaulted'), get it/them again from L2 installments table
				if (StringUtils.isEmpty(installmentAmount[0])) {
					if (atLeastOneDelqInst) {
						installmentAmount[0] = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(installmentsL2TableChildren, "TOTAL"), "", true)
								.replaceAll("[^\\d.]", "");

					} else {
						installmentAmount[0] = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(installmentsL2TableChildren, "TAX AMOUNT"), "",
								true).replaceAll("[^\\d.]", "");
					}
				}

				if (StringUtils.isEmpty(installmentAmount[1])) {
					if (atLeastOneDelqInst) {
						installmentAmount[1] = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(installmentsL2TableChildren, "TOTAL"), "", true)
								.replaceAll("[^\\d.]", "");
					} else {
						installmentAmount[1] = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(installmentsL2TableChildren, "TAX AMOUNT"), "",
								true).replaceAll("[^\\d.]", "");
					}
				}

				// get installments penalty amount
				installmentPenalty[0] = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(installmentsL2TableChildren, "PENALTY"), "", true)
						.replaceAll("[^\\d.]", "");

				installmentPenalty[1] = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(installmentsL2TableChildren, "PENALTY"), "", true)
						.replaceAll("[^\\d.]", "");

				// get installments base amount
				installmentBaseAmount[0] = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(installmentsL2TableChildren, "TAX AMOUNT"), "",
						true).replaceAll("[^\\d.]", "");
				installmentBaseAmount[1] = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(installmentsL2TableChildren, "TAX AMOUNT"), "",
						true).replaceAll("[^\\d.]", "");

				// if at least one installment not paid, get total due differently
				if (atLeastOneDelqInst) {
					totalDue = HtmlParser3.getValueFromAbsoluteCell(0, 3, HtmlParser3.findNode(installmentsL2TableChildren, "TOTAL"), "", true)
							.replaceAll("[^\\d.]", "");
				}
			}

			// get total due, amount paid, delinquent amount
			if (StringUtils.isNotEmpty(totalDue) && noOfInstallmentRows > 0) {

				double totaldueD = Double.parseDouble(totalDue);
				double installmentAmount1D = 0.0;
				double installmentAmount2D = 0.0;
				if (StringUtils.isNotEmpty(installmentAmount[0])) {
					installmentAmount1D = Double.parseDouble(installmentAmount[0]);
				}
				if (StringUtils.isNotEmpty(installmentAmount[1])) {
					installmentAmount2D = Double.parseDouble(installmentAmount[1]);
				}

				double amountPaidD = 0.0;

				if (installmentStatus[0].equals("PAID")) {
					totaldueD = totaldueD - installmentAmount1D;
					amountPaidD += installmentAmount1D;
				}

				if (installmentStatus[1].equals("PAID")) {
					totaldueD = totaldueD - installmentAmount2D;
					amountPaidD += installmentAmount1D;
				}

				totalDue = String.valueOf(totaldueD);
				amountPaid = String.valueOf(amountPaidD);
			}

			if (StringUtils.isNotEmpty(amountPaid)) {
				resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
			}

			// get tax year
			Text taxYearText = HtmlParser3.findNode(detailsL2Children, "Tax Bill");
			if (taxYearText != null) {
				String taxYear = taxYearText.toPlainTextString().trim();
				taxYear = StringUtils.extractParameter(taxYear, "^(\\d+)\\b");

				if (StringUtils.isNotEmpty(taxYear) && taxYear.matches("\\d+")) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);

					// get amount due/delinquent amount
					if (StringUtils.isNotEmpty(totalDue)) {
						int currentYearInt = dataSite.getCurrentTaxYear();
						int taxYearInt = Integer.parseInt(taxYear);
						if (currentYearInt > taxYearInt && (installmentStatus[0].equals("PASTDUE") || installmentStatus[1].equals("PASTDUE"))) {
							resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), totalDue);
						} else {
							resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
						}
					}
				}
			}

			// get tax installments
			Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
			List<List> installmentsBody = new ArrayList<List>();
			ResultTable resultTable = new ResultTable();

			String[] installmentsHeader = {
					TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(),
					TaxInstallmentSetKey.STATUS.getShortKeyName(),
					TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
					TaxInstallmentSetKey.PENALTY_AMOUNT.getShortKeyName(),
					TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
					TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName()
			};

			for (String s : installmentsHeader) {
				installmentsMap.put(s, new String[] { s, "" });
			}

			for (int i = 0; i < noOfInstallmentRows; i++) {
				List<String> installmentsRow = new ArrayList<String>();

				// get installment name
				installmentsRow.add(installmentName[i]);

				// get installment status
				if (installmentStatus[i].equals("PASTDUE")) {
					installmentsRow.add("UNPAID");
				} else {
					installmentsRow.add(installmentStatus[i]);
				}
				// get base amount per installment
				installmentsRow.add(installmentBaseAmount[i]);

				// get penalty per installment
				installmentsRow.add(installmentPenalty[i]);

				// get total due per installment
				if (installmentStatus[i].equals("PAID")) {
					installmentsRow.add("");
				} else {
					installmentsRow.add(installmentAmount[i]);
				}

				// get amount paid per installment
				if (installmentStatus[i].equals("PAID")) {
					installmentsRow.add(installmentAmount[i]);
				} else {
					installmentsRow.add("");
				}

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

			for (int i = 0; i < noOfInstallmentRows; i++) {
				List<String> receiptRow = new ArrayList<String>();

				if (installmentStatus[i].equals("PAID")) {
					receiptRow.add(installmentAmount[i]);
					receiptRow.add(installmentDatePaid[i]);
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

			if (noOfInstallmentRows == 1) {
				resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), installmentDatePaid[0]);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

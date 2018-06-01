package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class CAKernTR {
	private static ArrayList<String>	cities	= new ArrayList<String>();
	static {
		cities.add("BAKERSFIELD");
		cities.add("ARVIN");
		cities.add("BORON");
		cities.add("BUTTONWILLOW");
		cities.add("CALIFORNIA CITY");
		cities.add("CALIF CITY");
		cities.add("CHINA LAKE");
		cities.add("CHINA LAKE NWC");
		cities.add("DELANO");
		cities.add("EDWARDS");
		cities.add("EDWARDS AFB");
		cities.add("FELLOWS");
		cities.add("FRAZIER PARK");
		cities.add("GLENNVILLE");
		cities.add("GRAPEVINE");
		cities.add("INYOKERN");
		cities.add("KEENE");
		cities.add("KERNVILLE");
		cities.add("LAMONT");
		cities.add("LAKE ISABELLA");
		cities.add("LANCASTER");
		cities.add("LEBEC");
		cities.add("LOST HILLS");
		cities.add("MARICOPA");
		cities.add("MCFARLAND");
		cities.add("MC FARLAND");
		cities.add("MCKITTRICK");
		cities.add("METTLER");
		cities.add("MOJAVE");
		cities.add("NORTH OF THE RIVER");
		cities.add("OILDALE");
		cities.add("PALMDALE");
		cities.add("POND");
		cities.add("RANDSBURG");
		cities.add("RIDGECREST");
		cities.add("ROSAMOND");
		cities.add("SHAFTER");
		cities.add("TAFT");
		cities.add("TEHACHAPI");
		cities.add("TWIN OAKS");
		cities.add("WASCO");
		cities.add("WELDON");
	}

	public static void parseAndFillResultMap(String detailsHtml, ResultMap resultMap, long searchId) {
		try {
			String parcelId = "";
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);

			// get parcelId
			Node mainDoc = htmlParser3.getNodeById("mainDetailsRow");
			if (mainDoc != null) {// for main doc - ATN
				htmlParser3 = new HtmlParser3(mainDoc.toHtml());
				Node parcelIdNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblAtnOrFileType");
				if (parcelIdNode != null) {
					parcelIdNode = HtmlParser3.getFirstParentTag(parcelIdNode, TableRow.class);

					if (parcelIdNode != null) {
						NodeList parcelIdNodes = HtmlParser3.getTag(parcelIdNode.getChildren(), TableColumn.class, true);

						if (parcelIdNodes.size() > 1) {
							parcelIdNode = parcelIdNodes.elementAt(1);

							if (parcelIdNode != null) {
								parcelId = parcelIdNode.toPlainTextString().replaceAll("[^\\d-]", "");
							}
						}
					}
				}

			} else {// for extra docs - bill no
				Node parcelIdNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblBillNumber");
				if (parcelIdNode != null) {
					parcelId = parcelIdNode.toPlainTextString().replaceAll("[^\\d-]", "");
				}
			}

			if (StringUtils.isNotEmpty(parcelId)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelId);
			}

			// get year
			String year = "";
			Node yearNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblTaxYear");
			if (yearNode != null) {
				year = yearNode.toPlainTextString();
				year = StringUtils.extractParameter(year, "(?is)fiscal\\s+year\\s*(\\d{4})\\s*-\\s*\\d+");
				if (StringUtils.isNotEmpty(year)) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
				}
			}
			
			Node addressLabel = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblAddressLabel");
			boolean addressIsLegalDescription = false;
			if (addressLabel != null && !StringUtils.extractParameter(addressLabel.toPlainTextString(), "(?is)(Legal\\s+Description)").isEmpty()) {
				addressIsLegalDescription = true;
			}

			// get address(street no, street name, city) or legal(s/t/r, lot)
			Node addressNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblAddress");
			if (addressNode != null) {
				String fullAddressOrLegal = addressNode.toPlainTextString().trim();
				String[] addressTokens = addressNode.toHtml().replaceAll("(?is)</?span[^>]*>", "").split("<br>");
				if (StringUtils.isNotEmpty(fullAddressOrLegal)) {
					if (addressIsLegalDescription) {
						resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), fullAddressOrLegal);

						String section = StringUtils.extractParameter(fullAddressOrLegal, "(?is)\\bSECTION\\b\\s*(\\w+)\\b");
						if (StringUtils.isNotEmpty(section)) {
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
						}

						String township = StringUtils.extractParameter(fullAddressOrLegal, "(?is)\\bTOWNSHIP\\b\\s*(\\w+)\\b");
						if (StringUtils.isNotEmpty(township)) {
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), township);
						}
						String range = StringUtils.extractParameter(fullAddressOrLegal, "(?is)\\bRANGE\\b\\s*(\\w+)\\b");
						if (StringUtils.isNotEmpty(range)) {
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), range);
						}
						String lot = StringUtils.extractParameter(fullAddressOrLegal, "(?is)\\bLO?TS?\\b\\s*([\\w-]+)\\b");
						if (StringUtils.isNotEmpty(lot)) {
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
						}
					} else {
						resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), fullAddressOrLegal);
						String streetNumber = StringUtils.extractParameter(addressTokens[0], "^\\s*(\\d+)\\b");
						if (StringUtils.isNotEmpty(streetNumber)) {
							resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNumber);
						}
						String streetName = StringUtils.extractParameter(addressTokens[0], "^\\s*\\d+\\b\\s*(.*)\\s*");
						if (StringUtils.isNotEmpty(streetNumber)) {
							resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
						}

						String city = "";
						for (String cityTest : cities) {
							if (org.apache.commons.lang.StringUtils.containsIgnoreCase(fullAddressOrLegal, cityTest)) {
								city = cityTest;
								break;
							}
						}

						if (StringUtils.isEmpty(city)) {// some places are not in the 'cities' list
							city = StringUtils.extractParameter(addressTokens[addressTokens.length - 1], "^\\s*(.*?)\\s*,");
						}

						if (StringUtils.isNotEmpty(city)) {
							resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
						}
					}
				}
			}

			// get land
			Node landNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblLand");
			if (landNode != null) {
				String land = landNode.toPlainTextString().replaceAll("[^\\d.]+", "");
				if (StringUtils.isNotEmpty(land)) {
					resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
				}
			}

			// get improvements
			Node improvementsNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblImprovements");
			if (improvementsNode != null) {
				String improvements = improvementsNode.toPlainTextString().replaceAll("[^\\d.]+", "");
				if (StringUtils.isNotEmpty(improvements)) {
					resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvements);
				}
			}

			// get total assessment
			Node totalAssessmentNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblNetAssessedValue");
			if (totalAssessmentNode != null) {
				String totalAssessment = totalAssessmentNode.toPlainTextString().replaceAll("[^\\d.]+", "");
				if (StringUtils.isNotEmpty(totalAssessment)) {
					resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAssessment);
				}
			}

			// get tax exemption amount
			Node exemptionsNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblExemptions");
			if (exemptionsNode != null) {
				String exemptions = exemptionsNode.toPlainTextString().replaceAll("[^\\d.]+", "");
				if (StringUtils.isNotEmpty(exemptions)) {
					resultMap.put(TaxHistorySetKey.TAX_EXEMPTION_AMOUNT.getKeyName(), exemptions);
				}
			}

			// get total due
			Node totalDueNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblTotalAmountDue");
			if (totalDueNode != null) {
				String totalDue = totalDueNode.toPlainTextString().replaceAll("[^\\d.]+", "");
				if (StringUtils.isNotEmpty(totalDue)) {
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
				}
			}

			// get total billed - base amount
			Node baseAmountNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblTotalAmountBilled");
			if (baseAmountNode != null) {
				String baseAmount = baseAmountNode.toPlainTextString().replaceAll("[^\\d.]+", "");
				if (StringUtils.isNotEmpty(baseAmount)) {
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
				}
			}

			// get bill number
			String taxBillNo = "";
			Node taxBillNoNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblBillNumber");
			if (taxBillNoNode != null) {
				taxBillNo = taxBillNoNode.toPlainTextString().trim();
				if (StringUtils.isNotEmpty(taxBillNo)) {
					resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), taxBillNo);
				}
			}

			// get tax rate area
			Node taxAreaNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_lblBillNumber");
			if (taxAreaNode != null) {
				String taxArea = taxAreaNode.toPlainTextString().trim();
				if (StringUtils.isNotEmpty(taxArea)) {
					resultMap.put(PropertyIdentificationSetKey.AREA_CODE.getKeyName(), taxArea);
				}
			}

			String[] installmentName = new String[2];
			String[] installmentStatus = new String[2];
			String[] installmentAmountDue = new String[2];
			String[] installmentDatePaid = new String[2];
			String[] installmentAmountPaid = new String[2];
			String totalPaid = "";

			// parse tax installment set
			Node installmentsNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_tblInstallments");
			if (installmentsNode != null) {

				if (installmentsNode instanceof TableTag) {
					TableRow[] rows = ((TableTag) installmentsNode).getRows();
					if (rows.length > 0) {

						Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
						List<List<String>> installmentsBody = new ArrayList<List<String>>();
						ResultTable resultTable = new ResultTable();
						String[] installmentsHeader = {
								TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(),
								TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
								TaxInstallmentSetKey.STATUS.getShortKeyName(),
								TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(),
								// TaxInstallmentSetKey.PENALTY_AMOUNT.getShortKeyName(),
						};

						for (String s : installmentsHeader) {
							installmentsMap.put(s, new String[] { s, "" });
						}
						int instNo = 0;
						for (int i = 1; i < rows.length; i++) {
							if (rows[i].getColumnCount() >= 5 && instNo < 2) {
								List<String> installmentsRow = new ArrayList<String>();
								TableColumn[] cols = rows[i].getColumns();

								installmentName[instNo] = cols[1].toPlainTextString().trim();
								if (StringUtils.isNotEmpty(installmentName[instNo])) {
									installmentsRow.add(installmentName[instNo]);
								} else {
									installmentsRow.add("");
								}

								installmentAmountDue[instNo] = cols[3].toPlainTextString().replaceAll("[^\\d.]", "");
								if (StringUtils.isNotEmpty(installmentAmountDue[instNo])) {
									installmentsRow.add(installmentAmountDue[instNo]);
								} else {
									installmentsRow.add("");
								}

								installmentStatus[instNo] = cols[4].toPlainTextString();
								installmentStatus[instNo] = installmentStatus[instNo].replaceFirst("(?is)^\\s*(Paid|Unpaid)\\b.*", "$1").toUpperCase();
								if (StringUtils.isNotEmpty(installmentStatus[instNo])) {
									installmentsRow.add(installmentStatus[instNo]);
								} else {
									installmentsRow.add("");
								}

								if (installmentStatus[instNo].matches("(?i)Unpaid")) {
									installmentAmountPaid[instNo] = "0.0";
								} else {
									Pattern pattern = Pattern.compile("(?is)^.*?\\$\\s*([\\d.,]+)\\s+on\\s+([\\d/]+)");
									Matcher matcher = pattern.matcher(cols[4].toPlainTextString());
									if (matcher.find()) {
										installmentAmountPaid[instNo] = matcher.group(1).replaceAll("[^\\d.]", "");
										installmentDatePaid[instNo] = matcher.group(2).trim();
									}
								}

								if (StringUtils.isNotEmpty(installmentAmountPaid[instNo])) {
									installmentsRow.add(installmentAmountPaid[instNo]);
									totalPaid += installmentAmountPaid[instNo] + "+";
								} else {
									installmentsRow.add("");
								}

								if (installmentsRow.size() == installmentsHeader.length) {
									installmentsBody.add(installmentsRow);
								}
							}
							instNo++;
						}

						if (!installmentsBody.isEmpty()) {
							resultTable.setHead(installmentsHeader);
							resultTable.setMap(installmentsMap);
							resultTable.setBody(installmentsBody);
							resultTable.setReadOnly();
							resultMap.put("TaxInstallmentSet", resultTable);
						}
					}
				}

				// get total amount paid
				if (StringUtils.isNotEmpty(totalPaid)) {
					totalPaid = totalPaid.substring(0, totalPaid.length() - 1);
					totalPaid = GenericFunctions.sum(totalPaid, searchId);
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), totalPaid);
				}

			}

			// get receipts values and dates
			List<List<String>> bodyHist = new ArrayList<List<String>>();
			ResultTable resultTable = new ResultTable();
			String[] header = { "ReceiptAmount", "ReceiptDate" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
			map.put("ReceiptDate", new String[] { "ReceiptDate", "" });

			for (int i = 0; i < 2; i++) {// add for current year
				List<String> receiptRow = new ArrayList<String>();

				if (StringUtils.isNotEmpty(installmentAmountPaid[i])) {
					receiptRow.add(installmentAmountPaid[i]);
				}

				if (StringUtils.isNotEmpty(installmentDatePaid[i])) {
					receiptRow.add(installmentDatePaid[i]);
				}

				if (receiptRow.size() == header.length) {
					bodyHist.add(receiptRow);
				}
			}

			// receipts values and dates for prev years(only on main doc)
			Node prevYearsBillsNode = htmlParser3.getNodeById("ctl00_ContentPlaceHolder1_tblBills");
			if (prevYearsBillsNode != null && prevYearsBillsNode instanceof TableTag) {
				TableRow[] rows = ((TableTag) prevYearsBillsNode).getRows();
				if (rows.length > 0) {
					for (int i = 1; i < rows.length; i++) {
						if (rows[i].getColumnCount() >= 5) {
							List<String> receiptRow = new ArrayList<String>();
							TableColumn[] cols = rows[i].getColumns();

							String status = cols[4].toPlainTextString();
							status = status.replaceFirst("(?is)^\\s*(Paid|Unpaid)\\b.*", "$1");

							if (status.matches("(?i)Paid")) {
								Pattern pattern = Pattern.compile("(?is)^.*?\\$\\s*([\\d.,]+)\\s+on\\s+([\\d/]+)");
								Matcher matcher = pattern.matcher(cols[4].toPlainTextString());
								if (matcher.find()) {
									String amountPaid = matcher.group(1).replaceAll("[^\\d.]", "");
									String datePaid = matcher.group(2).trim();
									receiptRow.add(amountPaid);
									receiptRow.add(datePaid);
									if (receiptRow.size() == header.length) {
										bodyHist.add(receiptRow);
									}
								}

							}
						}
					}
				}
			}

			if (!bodyHist.isEmpty()) {
				resultTable.setHead(header);
				resultTable.setBody(bodyHist);
				resultTable.setMap(map);
				resultTable.setReadOnly();
				resultMap.put("TaxHistorySet", resultTable);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ResultMap parseIntermediaryRow(TableRow row) {

		ResultMap resultMap = new ResultMap();
		try {
			TableColumn[] cols = row.getColumns();
			if (cols.length >= 2) {
				String number = cols[0].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(number)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), number);
				}
				String address = cols[1].toPlainTextString().trim();
				String city = "";
				for (String cityTest : cities) {
					if (org.apache.commons.lang.StringUtils.containsIgnoreCase(address, cityTest)) {
						city = cityTest;
						break;
					}
				}

				String streetNo = StringUtils.extractParameter(address, "^\\s*(\\d+)\\b\\s+");
				String streetName = StringUtils.extractParameter(address, "^\\s*\\d+\\b\\s+(.+)$");

				if (city.isEmpty()) {
					city = StringUtils.extractParameter(streetName, "(?is)\\w+\\s+"
							+ "(?:AL|AV|BL|CH|CI|CO|CT|DR|EX|FY|GR|HY|LN|PK|PL|PT|PZ|RD|SQ|ST|TL|TR|WY)" // after the street suffix is the city name
							+ "\\s+(.*)");
				}

				if (StringUtils.isNotEmpty(city)) {
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
					streetName = streetName.replaceFirst("(?is)" + city, "").trim();
				}

				if (StringUtils.isNotEmpty(streetNo)) {
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
				}
				if (StringUtils.isNotEmpty(streetName)) {
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultMap;
	}
}

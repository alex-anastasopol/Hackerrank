package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

public class TNSumnerGallatinYA {
	public static final String		YA_PID_PATTERN					= "^(\\w+)-(\\w)-(\\w{6,})(\\1)$";
	public static final String		ALL_BILLS_CLASS					= "allBillsDataTables";
	public static final String		ALL_BILLS_DETAILS_CLASS			= "allBillsDetailsTable";
	protected static final String	AMOUNT_PATTERN					= "[^\\d.\\(\\)]+";
	protected static final String	AMOUNT_ABSOLUTE_VALUE_PATTERN	= "\\(([\\d.]+)\\)";
	protected static final String	HYPHEN							= "-";
	protected static final String	SPACES							= "\\s+";

	private static String[] splitAndFixLines(String[] lines, Vector<String> companyExpr) {
		ArrayList<String> newLines = new ArrayList<String>();
		try {
			String prevLast = "";
			Pattern trusteePattern = Pattern
					.compile("(?is)(.*?)(?:-|\\b)(?:[,(-]\\s*)?(?:AS\\s+)?(?:\\bCO-?\\s*)?(?<!BOARD\\sOF\\s)(?:T(?:(?:RU?)?S)?TEE?S?)(?:\\s*(?:[)]|OF))?\\b(.*)");
			Pattern nameSuffix = Pattern.compile("(?is)(.*?),?\\s*\\b(JR|SR|DR|[IV]{2,}|[III]{2,}|\\d+(?:ST|ND|RD|TH))\\b(.*)");
			boolean trustee = false;
			for (String line : lines) {
				if (StringUtils.isEmpty(line)) {
					continue;
				}
				if (trustee) {
					newLines.add("TRUSTEE");
					trustee = false;
				}
				if (COFremontAO.isCompany(line, companyExpr)) {
					String[] parts = line.split("&|AND");
					if (parts.length == 2) {
						// GEASEY LUTHER T JR &
						// SHIRLEY F LIFE EST & PAMELA L
						if (!COFremontAO.isCompany(parts[0], companyExpr)) {
							if (parts[0].trim().matches("[A-Z]{2,}(\\s+[A-Z])?")) {
								if (COFremontAO.isFirstName(parts[0].trim().split(SPACES)[0], new Vector<String>())) {
									newLines.add(prevLast + " " + parts[0].trim());
									newLines.add(parts[1].trim());
									continue;
								}
							}
						} else if (!COFremontAO.isCompany(parts[1], companyExpr)) {
							if (parts[1].trim().matches("[A-Z]{2,}(\\s+[A-Z])?")) {
								if (COFremontAO.isFirstName(parts[1].trim().split(SPACES)[0], new Vector<String>())) {
									newLines.add(parts[0].trim());
									newLines.add(prevLast + " " + parts[1].trim());
									continue;
								}
							}
						}
					}
					newLines.add(line);
				} else {
					Matcher m = trusteePattern.matcher(line);
					if (m.matches()) {
						line = m.replaceFirst(m.group(1) + m.group(2));
						trustee = true;
					}

					m = nameSuffix.matcher(line);
					if (m.matches()) {
						if (StringUtils.isNotEmpty(m.group(3)) && !m.group(3).contains("&")) {
							line = m.group(1) + m.group(3) + " " + m.group(2);
						}
					}

					String[] parts = line.split("&|\\bAND\\b");
					for (String part : parts) {
						part = part.trim();
						if (part.matches("WF")) {
							// wife
							continue;
						}
						String firstToken = part.split(SPACES)[0];
						if (part.contains(prevLast) && !firstToken.contains(prevLast)) {
							newLines.add("[FML]" + part);
						} else {
							newLines.add(part);
						}
					}

					String firstToken = line.split(SPACES)[0];
					if (!COFremontAO.isFirstName(firstToken, new Vector<String>())) {
						prevLast = firstToken;
					}
				}
			}

			if (trustee) {
				newLines.add("TRUSTEE");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newLines.toArray(new String[newLines.size()]);
	}

	public static void parseOwners(ResultMap resultMap, String ownerName) {
		if (StringUtils.isEmpty(ownerName)) {
			return;
		}

		Vector<String> companyExpr = new Vector<String>();
		ownerName = ownerName.replaceFirst("\n$", "");
		ownerName = ownerName.replaceAll("(?is)\\bET\\s*(AL|UX|VIR)\\b\\n?", "\nET$1\n");
		ownerName = ownerName.replaceAll("(?is)(-SOLE|\\bJ/T)\\b", "");
		String[] lines = ownerName.split("\n");
		// for(int i=0;i<lines.length;i++){//e.g. GREEN J TREVOR ETUX should be GREEN TREVOR J ETUX
		// lines[i] = lines[i].replaceFirst("(?s)^\\s*(\\w{2,})\\s+(\\w)\\s+(\\w{2,})\\s*(.*)", "$1 $3 $2 $4");
		// }
		lines = splitAndFixLines(lines, companyExpr);

		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, false, COFremontAO.ALL_NAMES_LF, -1);
	}

	public static ResultMap parseIntermediaryRow(String htmlRow) {
		ResultMap map = new ResultMap();
		try {
			htmlRow = htmlRow.replaceAll("(?is)\\s*<(/?tr|td)[^>]*>\\s*", "");
			String[] columns = htmlRow.split("(?is)</td>");
			if (columns.length >= 5) {
				String pid = columns[3].replaceAll(SPACES, "");
				if (!pid.isEmpty()) {
					map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid);
				}
				String year = columns[4].trim();
				if (!year.isEmpty()) {
					map.put(TaxHistorySetKey.YEAR.getKeyName(), year);
				}
				String unit = columns[1].trim();
				if (!unit.isEmpty()) {
					unit = " # " + unit;
				}
				String address = columns[0].trim() + unit;
				address = address.replaceFirst("^\\s*0+\\s*", "");// for addresses like "0 744 NORTHVIEW AVENUE"
				if (!address.isEmpty()) {
					map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
					map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				}

				parseOwners(map, columns[2].trim());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	public static String tableToHtml(TableTag table) {
		return tableToHtml(table, null);
	}

	public static String tableToHtml(TableTag table, String id) {
		String tableHtml = "";
		if (table != null) {
			table.setAttribute("style", "border-collapse: collapse");
			table.setAttribute("border", "2");
			table.setAttribute("width", "80%");
			table.setAttribute("align", "center");
			if (id != null) {
				table.setAttribute("id", id);
			}
			tableHtml = table.toHtml().replaceAll("<a [^>]*>(.*?)</a>", "$1")
					.replaceAll("View (state assessment data|payments/adjustments)", "");
		}
		return tableHtml;
	}

	public static void parseAndFillResultMap(String detailsHtml, ResultMap resultMap, Logger logger) {

		try {
			String owner = "";
			String address = "";
			String accountId = "";
			String year = "";
			String billNo = "";
			boolean firstYear = true;
			double priorDelinquentD = 0d;

			detailsHtml = detailsHtml.replaceAll("(?is)<br\\s*/?>", "\n");
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			NodeList nodeList = htmlParser3.getNodeList();

			Node accountIdNode = htmlParser3.getNodeById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_CategoryLabel");
			if (accountIdNode != null) {
				accountId = accountIdNode.toPlainTextString().replaceAll(SPACES, "");
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountId);
			}
			Node yearNode = htmlParser3.getNodeById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_FiscalYearLabel");
			if (yearNode != null) {
				year = yearNode.toPlainTextString().trim();
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
			}
			Node billNode = htmlParser3.getNodeById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_BillNumberLabel");
			if (billNode != null) {
				billNo = billNode.toPlainTextString().trim();
				resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), billNo);
			}

			// get tax tables for current year and previous ones
			NodeList billTableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class",
					ALL_BILLS_CLASS), true);
			NodeList billDetailTableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class",
					ALL_BILLS_DETAILS_CLASS), true);

			if (billTableList != null && billDetailTableList != null && billTableList.size() == billDetailTableList.size()) {
				for (int i = 0; i < billTableList.size(); i++) {
					TableTag billTable = (TableTag) billTableList.elementAt(i);
					TableRow[] rows = billTable.getRows();

					double penaltiesD = 0d;
					double baseAmountD = 0d;
					double totalDueD = 0d;
					String amountPaid = "";

					for (TableRow row : rows) {
						int columnCount = row.getColumnCount();
						if (firstYear && columnCount > 0 && row.getColumns()[0].toPlainTextString().contains("Interest and Penalties")) {
							try {
								String penalties = row.getColumns()[1].toPlainTextString().replaceAll("[^\\d.]", "").trim();
								if (StringUtils.isNotEmpty(penalties)) {
									penaltiesD = Double.parseDouble(penalties);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (row.toPlainTextString().contains("TOTAL")) {
							try {
								if (firstYear) {// get base amount
									String baseAmount = row.getColumns()[1].toPlainTextString().replaceAll(AMOUNT_PATTERN, "");
									baseAmount = replaceParanthesesWithMinus(baseAmount);
									baseAmountD = Double.parseDouble(baseAmount);

									// get amount paid
									amountPaid = row.getColumns()[2].toPlainTextString().replaceAll(AMOUNT_PATTERN, "");
									amountPaid = replaceParanthesesWithMinus(amountPaid);
								}

								// get amount due
								String totalDue = row.getColumns()[columnCount - 1].toPlainTextString().replaceAll(AMOUNT_PATTERN, "");
								totalDue = replaceParanthesesWithMinus(totalDue);
								totalDueD = Double.parseDouble(totalDue);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}

					if (firstYear) {
						resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), "" + (baseAmountD - penaltiesD));
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), "" + totalDueD);
						firstYear = false;
					} else {
						priorDelinquentD += totalDueD;
					}
				}
			} else {
				logger.error("Error while parsing details");
			}

			TableTag parcelTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ParcelTable"), true).elementAt(0);
			TableRow[] rows = parcelTable.getRows();

			for (TableRow row : rows) {
				if (row.getColumnCount() == 1) {
					String rowText = row.toPlainTextString();
					String col = row.getColumns()[0].toPlainTextString().trim();

					if (rowText.contains("Location")) {
						address = col.trim();
						if (StringUtils.isNotEmpty(address)) {
							resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
							resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
							resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
						}
						// legal = col;
					} else if (rowText.contains("Book/Page")) {
						String book = col.replaceFirst("/.*", "").replaceFirst("^0+", "");
						String page = col.replaceFirst(".*/", "").replaceFirst("^0+", "");
						resultMap.put(CrossRefSetKey.BOOK.getKeyName(), book);
						resultMap.put(CrossRefSetKey.PAGE.getKeyName(), page);
					}
				}
			}

			TableTag ownerTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "OwnerTable"), true).elementAt(0);
			rows = ownerTable.getRows();

			for (TableRow row : rows) {
				if (row.getColumnCount() == 1) {
					String rowText = row.toPlainTextString();
					String col = row.getColumns()[0].toPlainTextString().trim();

					if (rowText.contains("Name")) {
						owner += col + "\n";
					}
				}
			}

			TableTag assessmentTable = (TableTag) htmlParser3.getNodeById("AssessmentTable");
			rows = assessmentTable.getRows();

			for (TableRow row : rows) {
				if (row.getColumnCount() == 1) {
					String rowText = row.toPlainTextString();
					String col = row.getColumns()[0].toPlainTextString().trim();

					if (rowText.contains("Land")) {
						String land = col.replaceAll(AMOUNT_PATTERN, "");
						if (StringUtils.isNotEmpty(land)) {
							resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
						}
					} else if (rowText.contains("Total")) {
						String totalAssessment = col.replaceAll(AMOUNT_PATTERN, "");
						if (StringUtils.isNotEmpty(totalAssessment)) {
							resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAssessment);
						}
					}
				}
			}

			resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), "" + priorDelinquentD);
			parseOwners(resultMap, owner);
		} catch (Exception e) {
			logger.error("Error while parsing details", e);
		}

		return;
	}

	// public static String getPIDEquivalent(String accountId) {
	// accountId = accountId.replaceAll("\\s+", "");
	// Pattern pat = Pattern.compile(YA_PID_PATTERN);
	// Matcher mat = pat.matcher(accountId);
	// if (mat.matches()) {
	// // e.g. turn 126K-C-01100000126K into 126K-C-126K-01100000 or 126K-C-126K-011.00--000
	// accountId = mat.group(1) + HYPHEN + mat.group(2) + HYPHEN + mat.group(4) + HYPHEN + mat.group(3);
	// }
	// return accountId;
	// }

	protected static String replaceParanthesesWithMinus(String amount) {
		amount = org.apache.commons.lang.StringUtils.defaultString(amount);
		if (amount.contains("(")) {
			amount = HYPHEN + StringUtils.extractParameter(amount, AMOUNT_ABSOLUTE_VALUE_PATTERN);
		}
		return amount;
	}
}

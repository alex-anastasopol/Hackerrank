package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
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
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class CASanLuisObispoTR {

	protected static String	amountPattern				= "[^\\d.\\(\\)]+";
	protected static String	amountAbsoluteValuePattern	= "\\(([\\d.]+)\\)";
	public static String	yearAndBillPattern			= "(?is)<td\\b[^>]*>\\s*Bill\\s*#\\s*:?\\s*(\\d{4})/\\d{2}\\s+([\\d,.-]+)\\s*</td>";
	protected static String	datePattern					= "\\d+/\\d+/\\d+";

	@SuppressWarnings("rawtypes")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap resultMap, long searchId) {
		try {
			String parcelId = "";
			String taxYear = "";
			String baseAmount = "";
			String amountPaid = "";
			String totalDue = "";
			String names = "";
			String legal = "";
			String priorDelinquent = "";

			boolean isMainDetailsRow = true;

			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml.replaceAll("(?is)(</?t)h\\b", "$1d"));
			NodeList nodeList = htmlParser3.getNodeList();

			// determine if main or extra details row and get its html contents
			Node detailsRow = htmlParser3.getNodeById("mainDetailsRow");
			if (detailsRow == null) {
				isMainDetailsRow = false;
				nodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "extraDetailsRows"), true);
				if (nodeList.size() > 0) {
					detailsRow = nodeList.elementAt(0);
				}
			}

			if (detailsRow != null) {

				// get assessment # / bill number as parcelId
				String detailsRowHtml = detailsRow.toHtml();
				htmlParser3 = new HtmlParser3(detailsRowHtml);

				parcelId = RegExUtils.getFirstMatch(yearAndBillPattern,
						detailsRowHtml, 2);
				if (StringUtils.isNotEmpty(parcelId)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelId);
				}

				// get tax year
				taxYear = RegExUtils.getFirstMatch(yearAndBillPattern,
						detailsRowHtml, 1);
				if (StringUtils.isNotEmpty(taxYear)) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
				}

				Node detailsL1Node = null;
				Node detailsL2Node = null;
				NodeList detailsL1Nodes = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "detailsL1"), true);
				if (detailsL1Nodes.size() > 0) {
					detailsL1Node = detailsL1Nodes.elementAt(0);
				}
				NodeList detailsL2Nodes = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "detailsL2"), true);
				if (detailsL2Nodes.size() > 0) {
					detailsL2Node = detailsL2Nodes.elementAt(0);
				}

				if (detailsL1Node != null) {

					// get names
					NodeList detailsL1NodeChildren = detailsL1Node.getChildren();
					if (detailsL1NodeChildren.size() > 0) {
						NodeList assesseeNodes = detailsL1NodeChildren
								.extractAllNodesThatMatch(new RegexFilter("(?s)(Assessee\\s+Information\\s*)"), true);
						if (assesseeNodes.size() > 0) {
							Node nameTable = HtmlParser3.getFirstParentTag(assesseeNodes.elementAt(0), TableTag.class);
							do {// get next table, which contains the names
								nameTable = nameTable.getNextSibling();
							} while (nameTable != null && !(nameTable instanceof TableTag));

							if (nameTable != null) {
								names = nameTable.toPlainTextString().trim()
										.replaceAll("(\r\n)+", "$1");// to separate names
								if (StringUtils.isNotEmpty(names)) {
									resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), names);
								}
							}
						}

						// get legal description
						NodeList legalNodes = detailsL1NodeChildren
								.extractAllNodesThatMatch(new RegexFilter("(?s)(Property\\s+Information\\s*)"), true);
						if (legalNodes.size() > 0) {
							Node legalTable = HtmlParser3.getFirstParentTag(legalNodes.elementAt(0), TableTag.class);

							if (legalTable != null) {
								legal = legalTable.toPlainTextString()
										.replaceAll("(?is)(Property\\s+Information|Assessment\\s+Number.*)", "")
										.replaceAll("(?s)\\s+", " ")
										.trim();

								if (StringUtils.isNotEmpty(legal)) {
									resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
								}
							}
						}

						Node installmentsTable = HtmlParser3.getNodeByID("tblInstallmentInfo2", detailsL1NodeChildren, true);
						if (installmentsTable == null) {
							installmentsTable = HtmlParser3.getNodeByTypeAttributeDescription(detailsL1NodeChildren, "table", "", "",
									new String[] { "First Installment", "Second Installment", "Total" }, true);
						}
						if (installmentsTable != null && installmentsTable instanceof TableTag) {
							TableRow[] rows = ((TableTag) installmentsTable).getRows();
							if (rows.length > 11) {
								// get tax installment set
								Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
								List<List> installmentsBody = new ArrayList<List>();
								ResultTable resultTable = new ResultTable();

								String[] installmentsHeader = {
										TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(),
										TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
										TaxInstallmentSetKey.PENALTY_AMOUNT.getShortKeyName(),
										TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(),
										TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
										TaxInstallmentSetKey.STATUS.getShortKeyName()
								};
								for (String s : installmentsHeader) {
									installmentsMap.put(s, new String[] { s, "" });
								}

								String[] instBA = { "", "" };
								String[] instAD = { "", "" };
								String[] instAP = { "", "" };
								String[] instPenalty = { "", "" };
								String[] instStatus = { "", "" };
								String[] instDatePaid = { "", "" };

								for (int i = 0; i < rows.length; i++) {
									TableColumn[] columns = rows[i].getColumns();
									String titleCol = columns[0].toPlainTextString();
									if (columns.length > 2 && !RegExUtils.getFirstMatch("(?i)(Tax\\s+Amount)", titleCol, 1).isEmpty()) {

										// get total base amount
										if (columns.length > 3) {
											baseAmount = columns[3].toPlainTextString().replaceAll(amountPattern, "");
											baseAmount = replaceParanthesesWithMinus(baseAmount);
											if (StringUtils.isNotEmpty(baseAmount)) {
												resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
											}
										}

										instBA[0] = columns[1].toPlainTextString().replaceAll(amountPattern, "");
										instBA[0] = replaceParanthesesWithMinus(instBA[0]);
										instBA[1] = columns[2].toPlainTextString().replaceAll(amountPattern, "");
										instBA[1] = replaceParanthesesWithMinus(instBA[1]);

									} else if (columns.length > 2 && !RegExUtils.getFirstMatch("(?i)(Penalty)", titleCol, 1).isEmpty()) {

										instPenalty[0] = columns[1].toPlainTextString().replaceAll(amountPattern, "");
										instPenalty[0] = replaceParanthesesWithMinus(instPenalty[0]);
										instPenalty[1] = columns[2].toPlainTextString().replaceAll(amountPattern, "");
										instPenalty[1] = replaceParanthesesWithMinus(instPenalty[1]);

									} else if (columns.length > 2 && !RegExUtils.getFirstMatch("(?i)(Amount\\s+Paid)", titleCol, 1).isEmpty()) {

										instAP[0] = columns[1].toPlainTextString().replaceAll(amountPattern, "");
										instAP[0] = replaceParanthesesWithMinus(instAP[0]);
										instAP[1] = columns[2].toPlainTextString().replaceAll(amountPattern, "");
										instAP[1] = replaceParanthesesWithMinus(instAP[1]);

										if (columns.length > 3) {
											// get total amount paid
											amountPaid = columns[3].toPlainTextString().replaceAll(amountPattern, "");
											amountPaid = replaceParanthesesWithMinus(amountPaid);
											if (StringUtils.isNotEmpty(amountPaid)) {
												resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
											}
										}
									} else if (columns.length > 2 && !RegExUtils.getFirstMatch("(?i)(Date\\s+Paid)", titleCol, 1).isEmpty()) {
										instDatePaid[0] = columns[1].toPlainTextString().trim();
										instDatePaid[1] = columns[2].toPlainTextString().trim();
									} else if (columns.length > 2 && !RegExUtils.getFirstMatch("(?i)(Balance)", titleCol, 1).isEmpty()) {
										instAD[0] = columns[1].toPlainTextString().replaceAll(amountPattern, "");
										instAD[0] = replaceParanthesesWithMinus(instAD[0]);
										instAD[1] = columns[2].toPlainTextString().replaceAll(amountPattern, "");
										instAD[1] = replaceParanthesesWithMinus(instAD[1]);

										if (columns.length > 3) {
											// get total balance due
											totalDue = columns[3].toPlainTextString().replaceAll(amountPattern, "");
											totalDue = replaceParanthesesWithMinus(totalDue);
											if (StringUtils.isNotEmpty(totalDue)) {
												resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
											}
										}
									}

								}

								for (int i = 0; i < 2; i++) {
									List<String> installmentsRow = new ArrayList<String>();
									// get installment name
									installmentsRow.add((i == 0 ? "First" : "Second") + " Installment");

									// get base amount
									installmentsRow.add(instBA[i]);

									// get penalty
									installmentsRow.add(instPenalty[i]);

									// get amount paid
									installmentsRow.add(instAP[i]);

									// get total due(balance)
									installmentsRow.add(instAD[i]);

									// get status
									if (instDatePaid[i].matches(datePattern)) {
										instStatus[i] = "PAID";
									} else {
										instStatus[i] = "UNPAID";
									}

									installmentsRow.add(instStatus[i]);

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

								List<List> bodyHist = new ArrayList<List>();
								resultTable = new ResultTable();
								String[] header = { "ReceiptAmount", "ReceiptDate" };
								Map<String, String[]> map = new HashMap<String, String[]>();
								map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
								map.put("ReceiptDate", new String[] { "ReceiptDate", "" });

								for (int i = 0; i < 2; i++) {
									List<String> receiptRow = new ArrayList<String>();
									if (instStatus[i].equals("PAID")) {

										// get receipt value
										if (StringUtils.isNotEmpty(instAP[i])) {
											receiptRow.add(instAP[i]);
										} else {
											receiptRow.add("");
										}

										// get receipt date
										if (instDatePaid[i].matches(datePattern)) {
											receiptRow.add(instDatePaid[i]);
										} else {
											receiptRow.add("");
										}

										if (receiptRow.size() == header.length) {
											bodyHist.add(receiptRow);
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

								// get last paid date
								String paidDate = "";
								if (instDatePaid[1].matches(datePattern)) {
									paidDate = instDatePaid[1];
								} else if (instDatePaid[0].matches(datePattern)) {
									paidDate = instDatePaid[0];
								}

								if (StringUtils.isNotEmpty(paidDate)) {
									resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), paidDate);
								}
							}

						}
					}
				}

				if (detailsL2Node != null) {

					// get tax rate area
					int columnOffset = -1;
					if (!isMainDetailsRow) {
						columnOffset = 0;
					}
					String taxRateArea = HtmlParser3.getValueFromAbsoluteCell(1, columnOffset, HtmlParser3.findNode(nodeList, "TAX-RATE AREA"), "", true)
							.trim();

					if (StringUtils.isNotEmpty(taxRateArea)) {
						resultMap.put(PropertyIdentificationSetKey.AREA_CODE.getKeyName(), taxRateArea);
					}

					// get land
					String land = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "LAND"), "", true).replaceAll(amountPattern, "");
					if (StringUtils.isNotEmpty(land)) {
						resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
					}

					// get improvements
					String improvements = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "IMPROVEMENT"), "", true).replaceAll(
							amountPattern, "");
					if (StringUtils.isNotEmpty(improvements)) {
						resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), land);
					}
				}

				// parse prior delinquent

				NodeList priorDelqNodes = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "priorDelinquent"), true);
				if (priorDelqNodes.size() > 0) {
					Node priorDelqNode = priorDelqNodes.elementAt(0);
					if (priorDelqNode != null) {
						priorDelqNode = HtmlParser3.getNodeByTypeAttributeDescription(priorDelqNode.getChildren(), "table", "", "",
								new String[] { "If Paid in the Month of", "Total Payoff Amount", "Link to Details Page" }, true);

						if (priorDelqNode != null) {
							TableRow[] rows = ((TableTag) priorDelqNode).getRows();

							for (int i = rows.length - 1; i >= 0; i--) {
								if (rows[i].getColumnCount() >= 3) {
									String tempPD = rows[i].getColumns()[2].toPlainTextString().trim();
									if (!tempPD.replaceAll(amountPattern, "").isEmpty()) {
										priorDelinquent = tempPD.replaceAll(amountPattern, "");
									}
									if (tempPD.isEmpty()) {
										break;
									}
								}
							}
						}
					}
				}

				if (StringUtils.isNotEmpty(priorDelinquent)) {
					resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
				}

				parseNames(resultMap);
				parseLegal(resultMap);

				String addressUnit = "";
				if ((addressUnit = (String) resultMap.get("tmpAddressUnit")) != null) {// if an address unit was found in legal
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), "#" + addressUnit);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static String replaceParanthesesWithMinus(String amount) {
		amount = org.apache.commons.lang.StringUtils.defaultString(amount);
		if (amount.contains("(")) {
			amount = "-" + StringUtils.extractParameter(amount, amountAbsoluteValuePattern);
		}
		return amount;
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

				// parse names
				String nameOnServer = cols[1].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(nameOnServer)) {
					resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer);
				}

				parseNames(resultMap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	protected static void parseNames(ResultMap resultMap) {

		String ownersSTR = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(ownersSTR)) {
			return;
		}

		try {
			ownersSTR = ownersSTR.replaceFirst("(?is)(\\s*\\bET\\.?\\s*AL\\.?\\b)(\\s*TR(?:USTEE)?S\\s*)$", "$2$1");
			ownersSTR = ownersSTR.replaceAll("%", "");
			String[] owners = ownersSTR.split("\r\n");
			ArrayList<List> body = new ArrayList<List>();
			List<String> all_names = new ArrayList<String>();
			for (int i = 0; i < owners.length; i++) {
				all_names.add(owners[i]);
			}

			String[] names = { "", "", "", "", "", "" };
			String[] suffixes = { "", "" };
			String[] type = { "", "" };
			String[] otherType = { "", "" };

			for (int i = 0; i < all_names.size(); i++) {
				String name = all_names.get(i);
				name = name.toUpperCase().trim().replaceAll("\\s+", " ");
				if (NameUtils.isCompany(name)) {// test if company; if treated normally it's parsed wrong
					names[2] = name;
				} else if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(name)) {
					if (i == 0) {
						names = StringFormats.parseNameNashville(name, true);
					} else {
						names = StringFormats.parseNameDesotoRO(name, true);
					}
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
				}

				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), body);
			}

			if (body.size() > 0) {
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseLegal(ResultMap resultMap) {
		String legalDes = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(legalDes)) {
			return;
		}

		try {
			legalDes = legalDes.toUpperCase();

			Pattern LEGAL_LOT = Pattern.compile("\\s*LO?TS?\\s*[A-Z-]*(\\d+\\b\\s*((?:[&,-]|TO)?\\s*\\d+\\b\\s*)*)\\s*");
			Pattern LEGAL_TRACT = Pattern.compile("\\s*TR(?:ACT)? ?([\\d-]+)\\s*");
			Pattern LEGAL_UNIT = Pattern.compile("\\s*UNIT\\b(?:\\s*NO\\b)?\\s*(\\w+)\\s*");
			Pattern LEGAL_BLOCK = Pattern.compile("\\s*\\b(BLO?C?K?S?)\\s*([\\d]+[A-Z]?|[A-Z])\\b\\s*");
			Pattern ADDRESS_UNIT = Pattern.compile("\\s*\\b(?:APT|APARTMENT)\\s*([\\w]{1,3})\\b\\s*");

			// get lot
			Matcher matcher = LEGAL_LOT.matcher(legalDes);
			StringBuffer lotsSB = new StringBuffer();
			while (matcher.find()) {
				String lot = matcher.group(1).replaceAll("\\s*,\\s*", " ").replaceFirst("\\s*TO\\s*", "-");
				lot = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(lot);
				lotsSB.append(lot.trim() + " ");
				legalDes = legalDes.replaceFirst(matcher.group(), " ");
			}

			if (!lotsSB.toString().isEmpty()) {
				String lots = lotsSB.toString().replaceAll("\\s*&\\s*", " ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots);
			}

			// get block
			String block = "";
			matcher = LEGAL_BLOCK.matcher(legalDes);
			while (matcher.find()) {
				block += " " + matcher.group(2);
				legalDes = legalDes.replaceFirst(matcher.group(), " ");
			}

			block = block.replaceAll("\\s*&\\s*", " ").trim();

			if (block.length() != 0) {
				block = LegalDescription.cleanValues(block, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
			}

			// get tract
			matcher = LEGAL_TRACT.matcher(legalDes);
			if (matcher.find()) {
				String[] garbageValues = { "\\s+", "-\\d*" };
				String tract = LegalDescription.cleanGarbage(garbageValues, matcher.group(1));
				legalDes = legalDes.replace(matcher.group(), " ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
			}

			// get unit
			matcher = LEGAL_UNIT.matcher(legalDes);
			if (matcher.find()) {
				String unit = matcher.group(1);
				legalDes = legalDes.replace(matcher.group(), " ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit.trim());
			}

			// get address unit
			matcher = ADDRESS_UNIT.matcher(legalDes);
			if (matcher.find()) {
				String addressUnit = matcher.group(1);
				legalDes = legalDes.replace(matcher.group(), " ");
				resultMap.put("tmpAddressUnit", addressUnit.trim());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

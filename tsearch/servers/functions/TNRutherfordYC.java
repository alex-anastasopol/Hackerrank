package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 30, 2011
 */

public class TNRutherfordYC {
	public static void putSearchType(ResultMap resultMap, String type) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), type);
	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		putSearchType(resultMap, "YC");

		TableColumn[] cols = row.getColumns();

		if (cols.length >= 3) {
			// parcel
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
					cols[0].toPlainTextString().trim());
			resultMap.put(
					PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getKeyName(),
					cols[0].toPlainTextString().replace(" ", "").trim());

			// lot
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
					.getKeyName(), cols[1].toPlainTextString().trim());

			// address
			parseAddress(resultMap, cols[2].toPlainTextString().trim());

			// name
			if (cols.length == 4)
				parseNames(resultMap, cols[3].toPlainTextString().trim());

		}

		return resultMap;
	}

	public static void parseNames(ResultMap resultMap, String name) {
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
				name);

		try {
			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			String[] names = StringFormats.parseNameNashville(name, true);
			String[] type = GenericFunctions.extractAllNamesType(names);
			String[] otherType = GenericFunctions
					.extractAllNamesOtherType(names);
			String[] suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
					type, otherType, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);

			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAddress(ResultMap resultMap, String address) {
		resultMap.put(
				PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),
				address);

		String[] addr = StringFormats.parseAddressShelbyAO(address);
		if (addr.length == 2) {
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
					addr[0]);
			resultMap.put(
					PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
					addr[1]);
		}
	}

	public static void parseLegalDescription(ResultMap resultMap,
			String legal_des) {
		// in this case legal is contained in subdivision
		legal_des = legal_des.toUpperCase();

		// get section
		Pattern LEGAL_SECTION = Pattern.compile("SEC(TION)* ([^\\s]*)");
		Matcher m = LEGAL_SECTION.matcher(legal_des);
		if (m.find()) {
			String section = m.group(2);
			legal_des = legal_des.replaceAll(m.group(), "");
			legal_des = legal_des.replaceAll("\\s+", " ").trim();
			if(ro.cst.tsearch.utils.Roman.isRoman(section)){
				//maybe roman number
				try {
					section = ro.cst.tsearch.utils.Roman.parseRoman(section)+"";
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			resultMap.put(
					PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),
					section);
		}

		// get phase
		Pattern LEGAL_PHASE = Pattern.compile("(PH(ASE)*|PAHSE) ([^\\s]*)");
		m = LEGAL_PHASE.matcher(legal_des);
		if (m.find()) {
			String phase = m.group(3);
			legal_des = legal_des.replaceAll(m.group(), "");
			legal_des = legal_des.replaceAll("\\s+", " ").trim();
			resultMap.put(
					PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(),
					phase);
			
		}
		
		// get subdivision
		String subdivision = legal_des.replace("SUBDIVISION NAME:", "").trim();

		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),
				subdivision);
	}

	public static String cleanLegal(String legal_des) {
		return legal_des;
	}

	public static void parseTaxes(ResultMap resultMap, NodeList taxNodeTag) {
		try {
			if (taxNodeTag.size() == 3) {
				TableTag apprised = (TableTag) taxNodeTag.elementAt(0);
				TableTag assesed = (TableTag) taxNodeTag.elementAt(1);
				TableTag history = (TableTag) taxNodeTag.elementAt(2);

				if (apprised.getRowCount() > 1
						&& apprised.getRow(1).getColumnCount() > 8) {
					// get year
					String year = "";
					year = apprised.getRow(1).getColumns()[0]
							.toPlainTextString().replace(" ", "");
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);

					// get land
					String land = apprised.getRow(1).getColumns()[2]
							.toPlainTextString().replaceAll("[ $,-]", "");

					resultMap
							.put(PropertyAppraisalSetKey.LAND_APPRAISAL
									.getKeyName(), land);

					// get improvement
					String improvement = apprised.getRow(1).getColumns()[4]
							.toPlainTextString().replaceAll("[ $,-]", "");

					resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL
							.getKeyName(), improvement);

					// get apprisal
					String apprisal = apprised.getRow(1).getColumns()[8]
							.toPlainTextString().replaceAll("[ $,-]", "");

					resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL
							.getKeyName(), apprisal);

				}

				if (assesed.getRowCount() > 1
						&& assesed.getRow(1).getColumnCount() > 8) {
					// get assesed
					String assesed_val = assesed.getRow(1).getColumns()[4]
							.toPlainTextString().replaceAll("[ $,-]", "");

					resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT
							.getKeyName(), assesed_val);

					String ba = assesed.getRow(1).getColumns()[8]
							.toPlainTextString().replaceAll("[ $,-]", "");

					resultMap
							.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), ba);
				}
				// get receipts
				String[] rcpt_header = { "Tax Year", "ReceiptNumber",
						"ReceiptAmount", "AmountPaid", "ReceiptDate" };
				List<List<String>> rcpt_body = new ArrayList<List<String>>();
				List<String> rcpt_row = new ArrayList<String>();
				ResultTable receipts = new ResultTable();
				Map<String, String[]> rcpt_map = new HashMap<String, String[]>();
				rcpt_map.put(TaxHistorySetKey.YEAR.getKeyName(), new String[] {
						"Tax Year", "" });
				rcpt_map.put(TaxHistorySetKey.RECEIPT_NUMBER.getKeyName(),
						new String[] { "ReceiptNumber", "" });
				rcpt_map.put(TaxHistorySetKey.RECEIPT_AMOUNT.getKeyName(),
						new String[] { "ReceiptAmount", "" });
				rcpt_map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(),
						new String[] { "AmountPaid", "" });
				rcpt_map.put(TaxHistorySetKey.RECEIPT_DATE.getKeyName(),
						new String[] { "ReceiptDate", "" });

				TableRow[] rows = null;

				// extract receipts ?is
				TableTag rcpt_t = history;
				if (rcpt_t != null) {
					rows = rcpt_t.getRows();
					if (rows.length > 1) {
						// get amount paid
						String ap = rcpt_t.getRow(1).getColumns()[6]
								.toPlainTextString().replaceAll("[ $,-]", "");

						// get total due
						String td = rcpt_t.getRow(1).getColumns()[4]
								.toPlainTextString().replaceAll("[ $,-]", "");

						// get current year
						String current_year = rcpt_t.getRow(1).getColumns()[0]
								.toPlainTextString().replaceAll("[ $,-]", "");

						double amount_paid = Double.valueOf(ap);

						double prior_delinquent = 0;

						for (int j = 2; j < rows.length; j++) {
							String year = rcpt_t.getRow(j).getColumns()[0]
									.toPlainTextString().replaceAll("[ $,-]",
											"");

							if (year.equals(current_year)) {
								ap = rcpt_t.getRow(j).getColumns()[6]
										.toPlainTextString().replaceAll(
												"[ $,-]", "");
								amount_paid += Double.valueOf(ap);
							}
							String pd = rcpt_t.getRow(j).getColumns()[4]
									.toPlainTextString().replaceAll("[ $,-]",
											"");
							if (!rcpt_t.getRow(j).getColumns()[0]
									.toPlainTextString().replaceAll(" ", "")
									.equals(current_year)
									&& rcpt_t.getRow(j).getColumns()[8]
											.toPlainTextString()
											.replaceAll("[ $,-]", "")
											.equals("UNPAID")) {
								prior_delinquent += Double.valueOf(pd);
							}
						}

						resultMap.put(
								TaxHistorySetKey.AMOUNT_PAID.getKeyName(),
								amount_paid + "");

						resultMap.put(
								TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(),
								prior_delinquent + "");

						String date_paid = rcpt_t.getRow(1).getColumns()[8]
								.toPlainTextString().replaceAll("-", "/")
								.replace(" ", "");

						date_paid = date_paid.replace("UNPAID", "");

						resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(),
								date_paid);

						if (!date_paid.equals(""))
							td = "0.00";

						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(),
								td);

						for (int i = 1; i < rows.length; i++) {
							rcpt_row = new ArrayList<String>();
							TableRow r = rows[i];
							TableColumn[] cols = r.getColumns();
							if (cols.length == 9) {
								rcpt_row.add(cols[0].toPlainTextString()
										.replaceAll("[ $,-]", ""));
								rcpt_row.add(cols[2].toPlainTextString()
										.replaceAll("[ $,-]", ""));
								rcpt_row.add(cols[4].toPlainTextString()
										.replaceAll("[ $,-]", ""));
								rcpt_row.add(cols[6].toPlainTextString()
										.replaceAll("[ $,-]", ""));
								rcpt_row.add(cols[8].toPlainTextString()
										.replace("-", "/")
										.replaceAll("[ $,-]", ""));
								rcpt_body.add(rcpt_row);
							}
						}
					}
				}

				receipts.setHead(rcpt_header);
				receipts.setMap(rcpt_map);
				receipts.setBody(rcpt_body);
				receipts.setReadOnly();
				resultMap.put("TaxHistorySet", receipts);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param nodes
	 *            to search trough
	 * @param name
	 *            of table
	 * @return table or null
	 */

	public static TableTag getTableFromList(NodeList nodes, String name) {
		TableTag table = null;
		return table;
	}

	public static String get_name_addr_Row(String name_addr, int typeOfSearch,
			TableRow tableRow, String resultsUrl) {
		String rows = "<tr>\n";

		String col = "<td align=\"LEFT\">" + name_addr + "</td>";

		String other_cols = "";

		if (tableRow.getColumnCount() > 0) {
			String pin = tableRow.getChild(0).toPlainTextString()
					.replaceAll("\\s+", " ").trim();
			String child = tableRow.getColumns()[0].toHtml();
			String value = child
					.replaceAll(
							"(?i)[^<]*<input NAME=\"EName\" type=\"RADIO\" value=\"([^\"]*)\"[^>]*>[^<]*(</td>)",
							"$1");
			value = value.replace("<", "").replaceAll("\\s+", "+");
			;
			child = child
					.replaceAll(
							"(?i)<input NAME=\"EName\" type=\"RADIO\" value=\"([^\"]*)\"[^>]*>[^<]*(</td>)",
							"<a href=\"" + resultsUrl + "?EName=" + value
									+ "\">" + pin + "</a>$2");
			other_cols += child + "\n";
			int i = 0;
			for (TableColumn c : tableRow.getColumns()) {
				if (i > 0)
					other_cols += c.toHtml() + "\n";
				i++;
			}
		}

		if (typeOfSearch == 1)
			// search by name
			rows += other_cols + col + "\n" + "</tr>";
		else
			// search by address
			rows += other_cols + "\n" + "</tr>";

		return rows.replaceAll("\\s+", " ").trim();
	}
}

package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
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
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 15, 2011
 */

public class ARCarrollTR {
	public static void putSearchType(ResultMap resultMap, String type) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), type);
	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();

		putSearchType(resultMap, "TR");
		TableColumn[] cols = row.getColumns();

		if (cols.length == 7) {
			// parcel
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
					cols[0].toPlainTextString().trim());
			resultMap.put(
					PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getKeyName(),
					cols[0].toPlainTextString().replace("-", "").trim());

			// year
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), cols[1]
					.toPlainTextString().trim());

			// owner
			parseNames(resultMap, cols[2].toPlainTextString().trim());

			// book ?

			// address
			parseAddress(resultMap, cols[4].toPlainTextString().trim());

			// legal
			parseLegalDescription(resultMap, cols[5].toHtml());

			// tax payer ?

		}

		return resultMap;
	}

	public static void parseNames(ResultMap resultMap, String name) {

		name = name.toUpperCase();
		name = name.replace(" TST", " TRUSTEE");
		name = name.replace(" TSTS", " TRUSTEES");
		name = name.replaceAll("\\d/\\d", " ");
		name = name.replaceAll("\\d", " ");
		name += " ";

		// Parcel#: 001-03165-004 Owner: JOHNSON DANA (CALLAWAY)
		if (name.contains("(") && name.contains(")")) {
			String first_name = name.substring(name.indexOf(" "),
					name.indexOf("("));
			String second_name = name.substring(name.indexOf("(") + 1,
					name.indexOf(")"));

			name = name.replaceAll("\\(.*\\)", " ");
			name += " % " + second_name + " " + first_name;
		}

		// 001-01828-000 GOBBLER CEMETERY WEBB/HOWERTON/WILSON TRUSTEES
		String regex = "[A-Z]+/[A-Z]+/[A-Z]+\\sTRUSTEES";
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(name);
		while (matcher.find()) {
			name = name.replaceFirst(matcher.group(),
					" % "
							+ matcher.group().replace("/", " TRUSTEE % ")
									.replace("TRUSTEES", "TRUSTEE"));
		}

		if (!NameUtils.isCompany(name) && !name.contains(" INC")
				&& !name.contains(" TRUST ") && !name.contains("/TRUST ")) {
			name = name.replace("\\", " & ").replace("/", " & ");

			name = StringUtils.strip(name.replaceAll("\\s+", " "));
			if (name.lastIndexOf("&") == name.length() - 1
					&& name.lastIndexOf("&") != -1)
				name = name.substring(0, name.length() - 1);
		}
		name = StringUtils.strip(name.replaceAll("\\s+", " "));

		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
				name);

		if (name.contains("...")) {
			if (name.lastIndexOf("&") != -1) {
				name = name.substring(0, name.lastIndexOf("&"));
				name = StringUtils.strip(name.replaceAll("\\s+", " "));
			} else
				return;
		}

		try {
			String[] owners = name.split("%");

			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();

			for (String s : owners) {
				s = StringUtils.strip(s);

				String[] names = null;

				String last_name = s.substring(0, s.indexOf(" "));

				if (LastNameUtils.isLastName(last_name) || s.contains("TRUST")) {
					names = StringFormats.parseNameNashville(s, true);
				} else {
					names = StringFormats.parseNameFML(s, new Vector<String>(),
							false, true);
				}

				String[] type = GenericFunctions.extractAllNamesType(names);
				String[] otherType = GenericFunctions
						.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractNameSuffixes(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
						type, otherType, NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), body);
			}
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAddress(ResultMap resultMap, String address) {
		address = address.toUpperCase();
		address = address.replaceAll("-", " ");
		address = address.replaceAll("\\([^\\)]*\\)", " ").trim();
		address = StringUtils.strip(address.replaceAll("\\s+", " ")) + " ";
		address = address.replaceAll("(\\d+)([A-Z]) ", "$1 $2").trim();
		address = StringUtils.strip(address.replaceAll("\\s+", " ").replace(
				"#", ""));

		if (address.contains("NO ADDRESS") || address.contains("NULL"))
			return;

		resultMap.put(
				PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),
				address);

		// S HWY 221
		// HWY 221 SO
		String addr_parts[] = address.split(" ");
		if (addr_parts.length == 3) {
			if ((Normalize.isDirectional(addr_parts[0])
					&& Normalize.isSuffix(addr_parts[1]) && Normalize
					.isCompositeNumber(addr_parts[2]))
					|| (Normalize.isDirectional(addr_parts[2])
							&& Normalize.isSuffix(addr_parts[0]) && Normalize
							.isCompositeNumber(addr_parts[1]))) {
				resultMap.put(
						PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
						address);
				return;
			}
		}

		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
				StringFormats.StreetNo(address));
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
				StringFormats.StreetName(address));
	}

	public static void parseSTR(ResultMap resultMap, String str) {
		if (!str.equals("")) {
			String s_t_r[] = str.split("-");
			if (s_t_r.length == 3) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION
						.getKeyName(), s_t_r[0]);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP
						.getKeyName(), s_t_r[1]);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE
						.getKeyName(), s_t_r[2]);
			}
		}
	}

	public static void parseLegalDescription(ResultMap resultMap,
			String legal_des) {
		legal_des = cleanLegal(legal_des);

		resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER
				.getKeyName(), legal_des);

		Pattern LEGAL_S_T_R = Pattern.compile("(\\d+-\\d+-\\d+)");
		Pattern LEGAL_LOT = Pattern.compile("LOT:* (\\d+-*\\d*)+");
		Pattern LEGAL_BLOCK = Pattern.compile("BLOCK:* (\\d+)+");
		Pattern LEGAL_SUBDIV = Pattern.compile("SUBDIVISION:* (.*)+ LOT:");
		Pattern LEGAL_UNIT = Pattern.compile("UNIT:*\\s*([^\\s]*)");

		Matcher matcher = LEGAL_LOT.matcher("");

		String lot = (String) resultMap
				.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
						.getKeyName());
		String block = (String) resultMap
				.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK
						.getKeyName());
		String subdiv = (String) resultMap
				.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName());
		String unit = (String) resultMap
				.get(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName());
		String str = (String) resultMap
				.get(PropertyIdentificationSetKey.SUBDIVISION_SECTION
						.getKeyName())
				+ "-"
				+ (String) resultMap
						.get(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP
								.getKeyName())
				+ "-"
				+ (String) resultMap
						.get(PropertyIdentificationSetKey.SUBDIVISION_RANGE
								.getKeyName());

		// get S-T-R
		matcher = LEGAL_S_T_R.matcher(legal_des);
		if (matcher.find() && (str == null || str.equals(""))) {
			str = matcher.group(1);
			legal_des = legal_des.replaceFirst(matcher.group(), " ");
			str = str.replaceAll("\\s+", " ").trim();
			parseSTR(resultMap, str);
		}

		// get lot
		matcher = LEGAL_LOT.matcher(legal_des);
		if (matcher.find() && (lot == null || lot.equals(""))) {
			lot = matcher.group(1);
			legal_des = legal_des.replace(matcher.group(), " ");
			lot = lot.replaceAll("\\s+", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
					.getKeyName(), lot);
		}

		// get block
		matcher = LEGAL_BLOCK.matcher(legal_des);
		if (matcher.find() && (block == null || block.equals(""))) {
			block = matcher.group(1);
			legal_des = legal_des.replace(matcher.group(), " ");
			block = block.replaceAll("\\s+", " ").trim();
			resultMap
					.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK
							.getKeyName(), block);
		}

		// get subdiv
		matcher = LEGAL_SUBDIV.matcher(legal_des);
		if (matcher.find() && (subdiv == null || subdiv.equals(""))) {
			subdiv = matcher.group(1);
			legal_des = legal_des.replace(matcher.group(), " ");
			subdiv = subdiv.replaceAll("\\s+", " ").trim();
			resultMap.put(
					PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),
					subdiv);
		}

		// get unit
		if (unit == null || unit.equals("")) {
			// get it from legal
			matcher = LEGAL_UNIT.matcher(legal_des);
			if (matcher.find()) {
				unit = matcher.group(1);
				if (unit.contains("-"))
					unit = unit.substring(0, unit.indexOf('-'));
				unit = unit.replaceAll("\\s+", " ").trim();
			}
			resultMap.put(
					PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(),
					unit);
		}

	}

	public static String cleanLegal(String legal_des) {
		String clean = legal_des.replace("...", " ").replace("--", " ");

		clean = clean.replaceAll("<[^>]*>", " ");

		clean = StringUtils.strip(clean.replaceAll("\\s+", " "));

		clean = clean.replaceAll("(\\d+-\\d+-\\d+) (\\d+-\\d+-\\d+)", "$1");

		clean = clean.replaceAll("(?is)\\b(LOT)S?:?\\s*(\\d+)\\s*,?\\s*ALL\\s*(\\d+)", "$1 $2-$3");
		
		clean = StringUtils.strip(clean.replaceAll("\\s+", " "));
		
		return clean.toUpperCase();
	}

	public static void parseTaxes(ResultMap resultMap, NodeList taxNodeTag) {
		try {
			NodeList nodes = taxNodeTag;
			nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("bgcolor", "#cccccc"), false);

			String[] rcpt_header = { "ReceiptNumber", "Tax Year", "ReceiptDate", "ReceiptAmount", "AmountPaid" };
			List<List<String>> rcpt_body = new ArrayList<List<String>>();
			List<String> rcpt_row = new ArrayList<String>();
			ResultTable receipts = new ResultTable();
			Map<String, String[]> rcpt_map = new HashMap<String, String[]>();
			rcpt_map.put(TaxHistorySetKey.RECEIPT_NUMBER.getKeyName(),
					new String[] { "ReceiptNumber", "" });
			rcpt_map.put(TaxHistorySetKey.YEAR.getKeyName(), new String[] {
					"Tax Year", "" });
			rcpt_map.put(TaxHistorySetKey.RECEIPT_DATE.getKeyName(),
					new String[] { "ReceiptDate", "" });
			rcpt_map.put(TaxHistorySetKey.RECEIPT_AMOUNT.getKeyName(),
					new String[] { "ReceiptAmount", "" });
			rcpt_map.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(),
					new String[] { "AmountPaid", "" });

			TableRow[] rows = null;

			String date_paid = "";

			// extract receipts ?is
			TableTag rcpt_t = getTableFromList(nodes, "Receipts");
			if (rcpt_t != null) {
				rows = rcpt_t.getRows();
				if (rows.length > 1) {
					for (int i = 1; i < rows.length; i++) {
						rcpt_row = new ArrayList<String>();
						TableRow r = rows[i];
						TableColumn[] cols = r.getColumns();
						if (cols.length == 8) {
							rcpt_row.add(cols[0].toPlainTextString());
							rcpt_row.add("");
							rcpt_row.add(cols[2].toPlainTextString());
							rcpt_row.add(cols[6].toPlainTextString()
									.replaceAll("[$,-]", ""));
							rcpt_row.add("");
							rcpt_body.add(rcpt_row);

							if (i == rows.length - 1) {
								date_paid = cols[2].toPlainTextString();
							}
						}
					}
				}
			}

			// extract hitorical rcpt
			TableTag his_rcpt = getTableFromList(nodes, "Historical Receipts");
			if (his_rcpt != null) {
				rows = his_rcpt.getRows();
				if (rows.length > 1) {
					for (int i = 1; i < rows.length; i++) {
						rcpt_row = new ArrayList<String>();
						TableColumn[] cols = rows[i].getColumns();
						if (cols.length == 7) {
							rcpt_row.add(cols[0].toPlainTextString());
							rcpt_row.add(cols[1].toPlainTextString());
							rcpt_row.add(cols[2].toPlainTextString());
							rcpt_row.add(cols[3].toPlainTextString()
									.replaceAll("[$,-]", ""));
							rcpt_row.add(cols[4].toPlainTextString()
									.replaceAll("[$,-]", ""));
							rcpt_body.add(rcpt_row);
						}
					}
				}
				
				receipts.setHead(rcpt_header);
				receipts.setMap(rcpt_map);
				receipts.setBody(rcpt_body);
				receipts.setReadOnly();
				resultMap.put("TaxHistorySet", receipts);
			}
			

			// extract tax info 
			// TableTag tax_info = getTableFromList(nodes, "Tax Information");
			TableTag tax_info = getTableFromList(nodes, "Property Information");
			if (tax_info != null) {
				String baseAmt = HtmlParser3.getValueFromNextCell(new NodeList(tax_info), "Total Mandatory", "", false)
						.replaceAll("\\s+", " ").replaceAll("[$,-]", "").trim();
				if (StringUtils.isNotEmpty(baseAmt)) 
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmt);
				
				String amtPaid = HtmlParser3.getValueFromNextCell(new NodeList(tax_info), "Tax Paid", "", false)
						.replaceAll("\\s+", " ").replaceAll("[$,-]", "").trim();
				if (StringUtils.isNotEmpty(amtPaid)) 
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amtPaid);
				
				String amtDue = HtmlParser3.getValueFromNextCell(new NodeList(tax_info),"Balance",  "", false)
						.replaceAll("\\s+", " ").replaceAll("[$,-]", "").trim();
				if (StringUtils.isNotEmpty(amtDue)) 
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amtDue);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static TableTag getTableFromList(NodeList nodes, String name) {
		TableTag table = null;
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.elementAt(i).toHtml().contains(name)) {
				table = (TableTag) nodes.elementAt(i);
				break;
			}
		}
		return table;
	}
}

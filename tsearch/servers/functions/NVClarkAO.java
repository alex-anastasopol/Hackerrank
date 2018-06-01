package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;

/**
 * 
 * @author Oprina George
 * 
 *         May 16, 2011
 */

public class NVClarkAO {

	public static void putSearchType(ResultMap resultMap, String type) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), type);
	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();

		putSearchType(resultMap, "AO");
		TableColumn[] cols = row.getColumns();

		if (cols.length == 4) {
			String name1 = cols[0].toPlainTextString().toUpperCase()
					.replace("&NBSP;", " ").trim();

			String name2 = cols[1].toPlainTextString().toUpperCase()
					.replace("&NBSP;", " ").trim();

			String full_name = name1 + "&&" + name2;

			parseNames(resultMap, full_name);

			String parcel = cols[3].toPlainTextString().replaceAll("[^\\d,^-]",
					"");

			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
					parcel);
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(),
					parcel.replace("-", ""));
		}

		if (cols.length == 3) {
			String address = cols[0].toPlainTextString();

			parseAddress(resultMap, address);

			String city = cols[1].toPlainTextString();

			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);

			String parcel = cols[2].toPlainTextString().replaceAll("[^\\d,^-]",
					"");

			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
					parcel.replace("-", ""));
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(),
					parcel);
		}

		return resultMap;
	}

	public static void parseNames(ResultMap resultMap, String name) {
		// name1&&name2...
		try {
			String unparsedName = StringUtils.strip(name);
			
			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			if (StringUtils.isNotEmpty(unparsedName)) {
				// parseNameNashville
				unparsedName = unparsedName.replaceFirst("\\bFAM TR\\b", "FAMILY TRUST")
					.replaceFirst("\\bREV LIV TR\\b", "REVOCABLE LIVING TRUST")
					.replaceFirst("\\bFAM LIV TR\\b", "FAMILY LIVING TRUST")
					.replaceFirst("\\bREVOCABLE LIVING TR\\b", "REVOCABLE LIVING TRUST")
					.replaceFirst("\\bREV TR\\b", "REVOCABLE TRUST")
					.replaceFirst("\\bFAMILY LIVING TR\\b", "FAMILY LIVING TRUST")
					.replaceFirst("\\bLIVING TR\\b", "LIVING TRUST")
					.replaceFirst("\\bFAMILY TR\\b", "FAMILY TRUST");
					
				unparsedName = unparsedName.replaceAll("\\s+", " ");
				unparsedName = unparsedName.replaceAll("(?ism)comments:[^&]*",""); //bug 7213
				unparsedName = unparsedName.replaceAll("(?ism)\\&\\&*\\s*$","");
				
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), unparsedName.replaceAll("&&", " & "));

				String[] tokens = unparsedName.split("&&");
				for (int i = 0; i < tokens.length; i++) {
					if(StringUtils.isNotEmpty(tokens[i])){
						String[] names = StringFormats.parseNameNashville(
								tokens[i].replaceAll("\\s*&\\s*"," & "), true);
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
				}
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void parseAddress(ResultMap resultMap, String address) {
		String addr = address;
		addr = addr.replaceAll("#(\\d+)", " $1");
		addr = addr.replaceAll("\\bUT\\s*(\\d+)", " $1");

		resultMap.put(
				PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),
				addr);

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
				StringFormats.StreetName(addr));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
				StringFormats.StreetNo(addr));

	}

	public static void parseLegalDescription(ResultMap resultMap,
			String legal_des) {

		Pattern LEGAL_SEC = Pattern.compile("SEC\\s*([^\\s]+)");
		Pattern LEGAL_TWP = Pattern.compile("TWP\\s*([^\\s]+)");
		Pattern LEGAL_RNG = Pattern.compile("RNG\\s*([^\\s]+)");
		Pattern LEGAL_LOT = Pattern.compile("LOT\\s*([^\\s]+)");
		Pattern LEGAL_BLOCK = Pattern.compile("BLOCK\\s*([^\\s]+)");
		Pattern LEGAL_TRACT = Pattern.compile("TRACT\\s*#?\\s*([^\\s]+)");
		Pattern LEGAL_PLAT_BOOK_PAGE = Pattern.compile("PLAT\\s+BOOK\\s*([^\\s]+)\\s+PAGE\\s*([^\\s]+)");
		Pattern LEGAL_PARCEL_MAP = Pattern.compile("\\bPARCEL\\s+MAP\\s*([^-]+)\\s*-\\s*([^\\s]+)");
		Pattern LEGAL_PARCEL_MAP2 = Pattern.compile("\\bPARCEL\\s+MAP\\s+FILE\\s*([^\\s]+)\\s+PAGE\\s+([^\\s]+)");
		Pattern LEGAL_BLDG = Pattern.compile("BLDG\\s*([^\\s]+)");
		Pattern LEGAL_UNIT = Pattern.compile("[-|\\s]UNIT\\s*([^\\s]+)");
		Pattern LEGAL_PHASE = Pattern.compile("[-|\\s]PHASE\\s*([^\\s]+)");

		Pattern[] PATTERNS = { LEGAL_SEC, LEGAL_TWP, LEGAL_RNG, LEGAL_LOT,
				LEGAL_BLOCK, LEGAL_TRACT, LEGAL_PLAT_BOOK_PAGE, LEGAL_BLDG, LEGAL_UNIT,
				LEGAL_PHASE, LEGAL_PARCEL_MAP, LEGAL_PARCEL_MAP2 };

		Matcher matcher = null;

		legal_des = legal_des.replaceAll("[\\)\\(]+", "");
		
		String legal = legal_des;

		String[] legal_vector = legal.split("<br/>");

		for (int i = 0; i < legal_vector.length; i++) {
			legal_vector[i] = legal_vector[i].replaceAll("<[^>]*>", "");
			legal_vector[i] = " "
					+ StringUtils
							.strip(legal_vector[i].replaceAll("\\s+", " "));
		}

		legal = legal.replaceAll("<[^>]*>", "").replaceAll("[\\)\\(]+", "");
		legal = StringUtils.strip(legal.replaceAll("\\s+", " "));

		resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER
				.getKeyName(), legal);

		// get subdivision name
		if (!(legal_vector[0].contains("PLAT")
				|| legal_vector[0].contains("BOOK")
				|| legal_vector[0].contains("PAGE")
				|| legal_vector[0].contains("LOT")
				|| legal_vector[0].contains("BLOCK")
				|| legal_vector[0].contains("SEC")
				|| legal_vector[0].contains("TWP")
				|| legal_vector[0].contains("RNG") 
				|| legal_vector[0].contains("BLDG"))) {
			if (legal_vector[0].contains("UNIT")) {
				matcher = LEGAL_UNIT.matcher(legal_vector[0]);
				if (matcher.find()) {
					String unit = matcher.group(1);
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT
							.getKeyName(), unit);
					legal_vector[0] = legal_vector[0].replace(matcher.group(), "");
				}
			}
			if (legal_vector[0].contains("PHASE")) {
				matcher = LEGAL_PHASE.matcher(legal_vector[0]);
				if (matcher.find()) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), matcher.group(1));
					legal_vector[0] = legal_vector[0].replace(matcher.group(), "");
				}
			}
			if (legal_vector[0].contains("TRACT")) {
				matcher = LEGAL_TRACT.matcher(legal_vector[0]);
				if (matcher.find()) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), matcher.group(1));
					legal_vector[0] = legal_vector[0].replace(matcher.group(), "");
				}
			}
			legal_vector[0] = StringUtils.strip(legal_vector[0]);
			resultMap.put(
					PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),
					legal_vector[0]);
			legal_vector[0] = "";
		}

		String lot = "";
		for (String s : legal_vector) {
			for (Pattern p : PATTERNS) {
				matcher = p.matcher(s);
				if (matcher.find()) {
					if (p.equals(LEGAL_UNIT)) {
						resultMap.put(
								PropertyIdentificationSetKey.SUBDIVISION_UNIT
										.getKeyName(), matcher.group(1));
					}
					if (p.equals(LEGAL_PHASE)) {
						resultMap.put(
								PropertyIdentificationSetKey.SUBDIVISION_PHASE
										.getKeyName(), matcher.group(1));
					}
					if (p.equals(LEGAL_PLAT_BOOK_PAGE)) {
						resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(),
								matcher.group(1));
						resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(),
								matcher.group(2));
						resultMap.put(PropertyIdentificationSetKey.PLAT_DESC.getKeyName(), "PL");
					}
					if (p.equals(LEGAL_PARCEL_MAP) || p.equals(LEGAL_PARCEL_MAP2)) {//When exists PArcel Map, then these values will be put on plat book and page
						resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(),
								"PM" + matcher.group(1));
						resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(),
								matcher.group(2));
						resultMap.put(PropertyIdentificationSetKey.PLAT_DESC.getKeyName(), "PM");
					}
					
					if (p.equals(LEGAL_LOT)) {
						lot += " " + matcher.group(1);
					}
					if (p.equals(LEGAL_BLDG)) {
						resultMap.put(
								PropertyIdentificationSetKey.SUBDIVISION_BLDG
										.getKeyName(), matcher.group(1));
					}
					if (p.equals(LEGAL_BLOCK)) {
						resultMap.put(
								PropertyIdentificationSetKey.SUBDIVISION_BLOCK
										.getKeyName(), matcher.group(1));
					}
					if (p.equals(LEGAL_TRACT)) {
						resultMap.put(
								PropertyIdentificationSetKey.SUBDIVISION_TRACT
										.getKeyName(), matcher.group(1));
					}
					if (p.equals(LEGAL_SEC)) {
						resultMap
								.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION
										.getKeyName(), matcher.group(1));
					}
					if (p.equals(LEGAL_TWP)) {
						resultMap
								.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP
										.getKeyName(), matcher.group(1));
					}
					if (p.equals(LEGAL_RNG)) {
						resultMap.put(
								PropertyIdentificationSetKey.SUBDIVISION_RANGE
										.getKeyName(), matcher.group(1));
					}
				}
			}
		}
		
		if (lot.length() > 0){
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
	}

	public static void parseSaleDataSet(ResultMap resultMap, NodeList current,
			NodeList history) {
		TableTag current_t = null;
		TableTag history_t = null;

		String currentParcelNo = (String) resultMap.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
		if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(currentParcelNo)){
			currentParcelNo = currentParcelNo.replaceAll("\\p{Punct}", "");
		}
		if (current.size() > 0)
			current_t = (TableTag) current.elementAt(0);

		if (history.size() > 0)
			history_t = (TableTag) history.elementAt(0);

		ResultTable sale_doc = new ResultTable();
		String[] sale_doc_header = {
				SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(),
				SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName() };
		
		Map<String, String[]> doc_map = new HashMap<String, String[]>();
		doc_map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(),
				new String[] { SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), "" });
		doc_map.put(SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(), new String[] {
				SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(), "" });

		List<List<String>> doc_body = new ArrayList<List<String>>();

		if (current_t != null) {
			TableRow r = current_t.getRow(1);
			
			String parcelNo = r.getColumns()[0].toPlainTextString();
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(parcelNo) 
					&& currentParcelNo.equals(parcelNo.replaceAll("\\p{Punct}", "").trim())){
				
				String instr_no = r.getColumns()[2].toPlainTextString();
				instr_no = instr_no.replaceAll("<[^>]*>", "");
				instr_no = StringUtils.strip(instr_no.replaceAll("\\s+", " "));
				
				String instr_date = r.getColumns()[3].toPlainTextString();
				instr_date = instr_date.replaceAll("<[^>]*>", "");
				instr_date = StringUtils.strip(instr_date.replaceAll("\\s+", " "));
				
				if (instr_no.contains(":")){
					if (ro.cst.tsearch.utils.StringUtils.isEmpty(instr_date)){
						instr_date = instr_no.substring(0, instr_no.indexOf(":"));
					}
					instr_no = instr_no.substring(instr_no.indexOf(":") + 1, instr_no.length());
					instr_no = StringUtils.stripStart(instr_no, "0");
				}
	
				List<String> doc_row = Arrays.asList(new String[] { instr_no, instr_date });
				doc_body.add(doc_row);
			}
		}

		if (history_t != null) {
			TableRow[] rows = history_t.getRows();

			for (int i = 1; i < rows.length; i++) {
				String instr_no = rows[i].getColumns()[2].toPlainTextString();
				
				String parcelNo = rows[i].getColumns()[0].toPlainTextString();
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(parcelNo) 
						&& currentParcelNo.equals(parcelNo.replaceAll("\\p{Punct}", "").trim())){
					
					instr_no = instr_no.replaceAll("<[^>]*>", "");
					instr_no = StringUtils.strip(instr_no.replaceAll("\\s+", " "));
					
					String instr_date = rows[i].getColumns()[3].toPlainTextString();
					instr_date = instr_date.replaceAll("<[^>]*>", "");
					instr_date = StringUtils.strip(instr_date.replaceAll("\\s+", " "));
					
					if (instr_no.contains(":")){
						if (ro.cst.tsearch.utils.StringUtils.isEmpty(instr_date)){
							instr_date = instr_no.substring(0, instr_no.indexOf(":"));
						}
						
						instr_no = instr_no.substring(instr_no.indexOf(":") + 1, instr_no.length());
						instr_no = StringUtils.stripStart(instr_no, "0");
					}
	
					List<String> doc_row = Arrays.asList(new String[] { instr_no, instr_date });
					doc_body.add(doc_row);
				}
			}
		}

		try {
			sale_doc.setHead(sale_doc_header);
			sale_doc.setMap(doc_map);
			sale_doc.setBody(doc_body);
			sale_doc.setReadOnly();
		} catch (Exception e) {
			e.printStackTrace();
		}

		resultMap.put("SaleDataSet", sale_doc);

	}
}

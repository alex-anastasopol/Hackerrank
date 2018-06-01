package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
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
 * parsing fuctions for ILDuPAgeTR.
 * 
 * @author mihaib
 */

public class ILDuPageTR {
	
	@SuppressWarnings("rawtypes")
	public static void nameILDuPageTR(ResultMap m, long searchId) {
		String ownerAndAddress = (String) m.get("tmpName");
		if (!ownerAndAddress.isEmpty()) {
			String[] lines = ownerAndAddress.split("   ");
			String owner = "";
			int namedepth = depth(lines);
			owner += lines[0];
			for (int i = 1; i < namedepth - 1; i++)
				if (!isAddress(lines[i].trim()))
					owner += "\n" + lines[i];
			owner = owner.trim();
			m.put("PropertyIdentificationSet.NameOnServer", owner);

			if (StringUtils.isNotEmpty(owner) && owner.contains("C/O")) {
				String[] split = owner.split("C/O");
				if (split.length == 2) {
					List body = new ArrayList();
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(m,
							split[0], body);
					ParseNameUtil.putNamesInResultMapFromDeSotoParse(m,
							split[1], body);
				} else {
					parseNames(m, -1l);
				}
			} else if (StringUtils.isNotEmpty(owner) && owner.contains("\n")) {
				String[] split = owner.split("\n");
				if (split.length == 2) {
					List body = new ArrayList();
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(m,
							split[0], body);
					ParseNameUtil.putNamesInResultMapFromDeSotoParse(m,
							split[1], body);
				} else {
					parseNames(m, -1l);
				}
			} else {
				parseNames(m, -1l);
			}

		}
	}

	private static int depth(String[] lines) // line with
												// "City StateAbbreviation ZIPCode"
												// format
	{
		String[] tokens;
		int i;
		for (i = 1; i < lines.length; i++) {
			tokens = lines[i].split(" ");
			int length = tokens.length;
			if (length >= 3
					&& Normalize.isStateAbbreviation(tokens[length - 2])
					&& tokens[length - 1].matches("\\d+"))
				return i;
		}
		return i;
	}

	private static boolean isAddress(String current) // contains PO BOX or
														// begins with number or
														// contains a street
														// name
	{
		if (current.matches("(?is)(.*)P\\s?O BOX\\s(\\d+)"))
			return true;
		if (current.matches("(\\d+)\\s(.*)"))
			if (current.trim().matches("(?is)\\d+(.*)\\b(ST(?:REET)?)\\b(.*)")) {
				return true;
			} else if (!NameUtils.isCompany(current))
				return true;
			else
				return false;
		
		String[] tokens = current.split(" ");
		int number = tokens.length;
		for (int i = 1; i < number; i++)
			if (Normalize.isSuffix(tokens[i]) && tokens[i - 1].matches("\\w+"))
				return true;

		if (current.contains("UNIT"))
			return true;
		if (current.contains(" IL "))
			return true;

		return false;
	}

	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {

		String owner = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
		owner = owner.replaceAll("(?is)ATTN\\s*:?(.*)", "");

		// for "MC COMBS III, ASHTON & E" replace spaces in last name in order
		// to be correctly parsed
		Matcher ma = Pattern
				.compile(
						"((?i)^(MAC|VAN(?:\\s+DE[N|R]?)?|O|ST|MC|DE(?: LA)?|DI|EL|DEL) (\\w|-)+)(\\s\\w+,)(.*)")
				.matcher(owner);
		if (ma.matches())
			owner = ma.group(1).replaceAll("\\s", "DUMMYSPACE") + ma.group(4)
					+ ma.group(5);
		if (!NameUtils.isCompany(owner))
			owner = owner.replaceAll("\\d+", "");

		owner = owner.replaceAll("\\s+,", ",");
		owner = owner.replaceAll("\\s{3,}", "\n");
		if (StringUtils.isEmpty(owner))
			return;
		String[] ownerRows = owner.split("\n");
		StringBuffer stringOwnerBuff = new StringBuffer();
		for (String row : ownerRows) {
			if (!NameUtils.isCompany(row) && row.trim().matches("\\d+\\s+.*")) {
				break;
			} else if (LastNameUtils.isNoNameOwner(row)) {
				break;
			} else {
				if (!StringUtils.isBlank(row)) {
					stringOwnerBuff.append(row + "\n");
				}
			}
		}
		String stringOwner = stringOwnerBuff.toString();
		stringOwner = stringOwner.replaceAll("\n$", "");
		String[] nameLines = stringOwner.split("\n");

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes = { "", "" }, type, otherType;
		StringBuffer nameOnServerBuff = new StringBuffer();
		for (int i = 0; i < nameLines.length; i++) {
			String ow = nameLines[i];
			names = StringFormats.parseNameNashville(ow, true);
			names[2] = names[2].replaceAll("DUMMYSPACE", " ");
			names[5] = names[5].replaceAll("DUMMYSPACE", " ");
			if (!NameUtils.isCompany(ow))
				suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
					type, otherType, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
			nameOnServerBuff.append("/").append(ow);
		}
		String nameOnServer = nameOnServerBuff.toString();
		nameOnServer = nameOnServer.replaceFirst("/", "");
		resultMap.put("PropertyIdentificationSet.NameOnServer", nameOnServer);
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// String[] a = StringFormats.parseNameNashville(owner, true);

		/*
		 * resultMap.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		 * resultMap.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		 * resultMap.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		 * resultMap.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		 * resultMap.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		 * resultMap.put("PropertyIdentificationSet.SpouseLastName", a[5]);
		 */
	}

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

			// street number
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
					cols[1].toPlainTextString().trim());

			// street direction + street name + unit
			resultMap.put(
					PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
					cols[2].toPlainTextString().trim() + " " + cols[3].toPlainTextString().trim() + " " + cols[4].toPlainTextString().trim());

			// city
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(),
					cols[5].toPlainTextString().trim());

			// zip
			resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(),
					cols[6].toPlainTextString().trim());

		}

		return resultMap;
	}

	public static void parseAddress(ResultMap resultMap, String address) {
		String addr = org.apache.commons.lang.StringUtils.strip(address.replaceAll("\\s+", " "));

		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr);

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
				StringFormats.StreetName(addr));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
				StringFormats.StreetNo(addr));
	}

	public static void parseLegalDescription(ResultMap resultMap,
			String legal_des) {
		String legal_des_aux = legal_des.replaceAll("(?ism)\\s+", " ");

		resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER
				.getKeyName(), legal_des_aux);

		// get unit
		Pattern LEGAL_UNIT = Pattern.compile("\\s*(UNIT|APT)\\s*([^\\s]+)");

		Matcher matcher = LEGAL_UNIT.matcher(legal_des_aux);

		if (matcher.find()) {
			resultMap.put(
					PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(),
					matcher.group(2));
			legal_des_aux = legal_des_aux.replaceAll(matcher.group(), "");
			legal_des_aux = legal_des_aux.replaceAll(LEGAL_UNIT.toString(), "");
		}
		// get city
		if (legal_des_aux.indexOf(" IL ") >= 0) {
			String city = legal_des_aux.substring(0,
					legal_des_aux.indexOf(" IL "));
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(),
					StringUtils.strip(city));

			// get zip
			String zip = legal_des_aux.substring(legal_des_aux.indexOf(" IL "));
			resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(),
					StringUtils.strip(zip.replaceAll("[^\\d]", "")));
		}else{
			String[] split = legal_des_aux.split(",");
			if (split!=null && split.length ==2){
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(),
						StringUtils.strip(split[0].trim()));
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(),
						split[1].trim());
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static void parseTaxes(ResultMap resultMap, NodeList taxNodeTag) {
		try {
			// get year
			String year = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(taxNodeTag, "First Due:"), "", true);
			year = StringUtils.strip(year.replaceAll("\\s+", " "));
			year = year.substring(year.lastIndexOf("/")).replace("/", "");

			int y = Integer.valueOf(year) - 1;

			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), y + "");

			// get base amount
			String base_am = HtmlParser3.getValueFromAbsoluteCell(0, 1,	
					HtmlParser3.findNode(taxNodeTag, "Total Base Tax"), "",	true).replaceAll("[\\s,$,-]", "");

			resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), base_am);

			// Installment table
			TableTag installment = (TableTag) taxNodeTag
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_pageContent_ctl00_tblResults"), true)
					.elementAt(0);

			TableRow[] rows = installment.getRows();

			// get amount paid & amount due
			double ap = 0;
			double td = 0;

			List<String> line = new ArrayList<String>();
			List<List> bodyRT = new ArrayList<List>();
			
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 5) {
					String totalRow = row.getColumns()[3].toPlainTextString().replace("&nbsp;", "").replaceAll("[\\s,$,-]", "");
					if (row.getColumns()[4].toPlainTextString().contains("/")) {
						ap += Double.valueOf(totalRow);
						line = new ArrayList<String>();
						line.add(totalRow);
						line.add(row.getColumns()[4].toPlainTextString().trim());
						bodyRT.add(line);
					} else {
						
						if (StringUtils.isNotBlank(totalRow)) {
							td += Double.valueOf(totalRow);
						}
					}
				}
			}

			resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), ap + "");
			resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), td + "");

			// get prior receipts

			String[] rcpt_header = { /*"Year",*/ "ReceiptAmount", "ReceiptDate" };
			ResultTable receipts = new ResultTable();

			

			// add rows
			NodeList prior_years = taxNodeTag
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("id", "ctl00_pageContent_ctl00_tblResults")), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "SearchTable"), true);

			for (int i = 0; i < prior_years.size(); i++) {
				TableTag t = (TableTag) prior_years.elementAt(i);
				TableRow[] t_r = t.getRows();
				for (int k = 1; k < t_r.length; k++) {
					line = new ArrayList<String>();
					//line.add(prior_year);
					line.add(t_r[k].getColumns()[1].toPlainTextString().replaceAll("[\\s,$,-]", ""));
					line.add(t_r[k].getColumns()[2].toPlainTextString().replaceAll("[\\s,$,-]", ""));
					bodyRT.add(line);
				}
			}
			
//			TableRow[] prior_rows = prior_years.getRows();
//
//			if (prior_rows.length == 1 && prior_rows[0].getColumnCount() == 3) {
//				TableRow r = prior_rows[0];
//
//				// the 3 tables
//				for (int i = 0; i < r.getColumnCount(); i++) {
//					//String prior_year = r.getColumns()[i].toHtml().replaceAll(
//					//		"(?ism).*<h2>Prior Year (\\d+) Taxes</h2>.*", "$1");
//
//					TableTag t = null;
//					for (int j = 0; j < r.getColumns()[i].getChildCount(); j++) {
//						if (r.getColumns()[i].getChild(j) instanceof TableTag) {
//							t = (TableTag) r.getColumns()[i].getChild(j);
//							break;
//						}
//					}
//
//					// make the 2 rows
//					if (t != null) {
//						TableRow[] t_r = t.getRows();
//						for (int k = 1; k < t_r.length; k++) {
//							line = new ArrayList<String>();
//							//line.add(prior_year);
//							line.add(t_r[k].getColumns()[1].toPlainTextString().replaceAll("[\\s,$,-]", ""));
//							line.add(t_r[k].getColumns()[2].toPlainTextString().replaceAll("[\\s,$,-]", ""));
//							bodyRT.add(line);
//						}
//					}
//				}
//			}

			receipts = GenericFunctions1.createResultTableFromList(rcpt_header, bodyRT);

			resultMap.put("TaxHistorySet", receipts);
			
			List<List<String>> installments= HtmlParser3.getTableAsList(installment, false);
			List<List> bodyInstallments = new ArrayList<List>();
			if (installments != null && installments.size() > 0){
				for (List<String> list : installments) {
					if (list.size() == 5){
						line = new ArrayList<String>();
						line.add(list.get(0).trim());
						line.add(list.get(1).trim().replace("&nbsp;", "").replaceAll("[\\$,]", ""));
						line.add(list.get(2).trim().replace("&nbsp;", "").replaceAll("[\\$,]", ""));
						if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(list.get(4))){
							line.add(list.get(3).trim().replace("&nbsp;", "").replaceAll("[\\$,]", ""));
							line.add("0.00");
							line.add("PAID");
						} else {
							line.add("0.00");
							line.add(list.get(3).trim().replace("&nbsp;", "").replaceAll("[\\$,]", ""));
							line.add("UNPAID");
						}
						bodyInstallments.add(line);
					}
				}
				if (!bodyInstallments.isEmpty()){
					String [] header = {"InstallmentName", "BaseAmount", "PenaltyAmount", "AmountPaid", "TotalDue", "Status"};				   
					Map<String,String[]> map = new HashMap<String,String[]>();
					map.put("InstallmentName", new String[]{"InstallmentName", ""});
					map.put("BaseAmount", new String[]{"BaseAmount", ""});
					map.put("PenaltyAmount", new String[]{"PenaltyAmount", ""});
					map.put("AmountPaid", new String[]{"AmountPaid", ""});
					map.put("TotalDue", new String[]{"TotalDue", ""});
					map.put("Status", new String[]{"Status", ""});
					
					ResultTable installmentsRT = new ResultTable();	
			   	    installmentsRT.setHead(header);
			   	    installmentsRT.setBody(bodyInstallments);
			   	    installmentsRT.setMap(map);
			   	    resultMap.put("TaxInstallmentSet", installmentsRT);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

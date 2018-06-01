package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.SimpleNodeIterator;

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

/**
 * 
 * 
 * @author Oprina George
 * 
 *         Feb 15, 2011
 */

public class FLMarionTR {
	public static void parseName(ResultMap resultMap, String tmpOwnerName, boolean isPP) {
		String s = new String(tmpOwnerName.replaceAll(" AND ", " & ").replaceAll("\\s+", " ").trim());
		resultMap.put("PropertyIdentificationSet.NameOnServer", s);

		// tmpOwnerName = tmpOwnerName.replaceAll("C/O", "");
		tmpOwnerName = tmpOwnerName.replaceAll("AS (TRUSTEE)", " $1");
		tmpOwnerName = tmpOwnerName.replaceAll("CO (TRUSTEE)", " $1");
		tmpOwnerName = tmpOwnerName.replaceAll("CO-(TRUSTEE)", " $1");
		tmpOwnerName = tmpOwnerName.replaceAll("(TRUSTEE)", " $1");
		tmpOwnerName = tmpOwnerName.replaceAll("ATTY", "");
		tmpOwnerName = tmpOwnerName.replaceAll("MR & MRS", "");
		// tmpOwnerName = tmpOwnerName.replaceAll("C/O ", ""); // R2100-054-025

		// tmpOwnerName = tmpOwnerName.replace("&", "AND");//:)
		List<String> ownerList = Arrays.asList(tmpOwnerName.split("\\bAND\\b"));

		List body = new ArrayList();
		boolean flag = false;
		for (String name : ownerList) {
			name = name.replaceAll("\\s+", " ").trim();// GenericFunctions2.cleanOwnerNameFromPrefix(name);
			// name = name.replace(" TRS", "");//" TRUSTEES");
			flag = false;
			if (name.contains("C/O ")) {
				flag = true;
				name = name.replaceAll("C/O ", "");
			}
			if (name.contains("&")) {
				String[] aux = name.split("&");
				if (aux.length == 2) {
					if (aux[0].split(" ").length == 1
							&& !NameUtils.isCompany(name)) {
						// invert names and parse desoto
						String aux1 = aux[0];
						aux[0] = aux[1];
						aux[1] = aux1;
						flag = true;
						name = StringUtils.join(aux, " & ")
								.replaceAll("\\s+", " ").trim();
					}
				}
			}
			if ((!isPP && !flag) || (isPP && !flag && (name.split(" ").length == 3 || NameUtils.isCompany(name)))) {
				String[] names = StringFormats.parseNameNashville(name, true);
				String[] type = GenericFunctions.extractAllNamesType(names);
				String[] otherType = GenericFunctions
						.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractNameSuffixes(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			} else {
				// name = name.replace(" & ", " AND ").trim();
				
				if (name.contains(" TRS")) {
					name = name.replace(" TRS", "");
					name += " TRS";
				}
				
				String[] parsedName = null;
				
				if (name.equals(ownerList.get(0).trim()) && name.split(" ").length == 2) {
					parsedName = StringFormats.parseNameNashville(name, true);
				} else {
					parsedName = StringFormats.parseNameDesotoRO(name);
				}
				
				String[] type = GenericFunctions.extractAllNamesType(parsedName);
				String[] otherType = GenericFunctions.extractAllNamesOtherType(parsedName);
				String[] suffixes = GenericFunctions.extractNameSuffixes(parsedName);
				GenericFunctions.addOwnerNames(parsedName, suffixes[0],	suffixes[1], type, otherType, NameUtils.isCompany(parsedName[2]), NameUtils.isCompany(parsedName[5]), body);
			}
		}

		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
			;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void parseAddress(ResultMap resultMap, String addressOnServer) {
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),	addressOnServer);
		// clean

		if (addressOnServer.length() > 5) {
			String aux = addressOnServer.substring(addressOnServer.length() - 5);
			if (aux.matches(" \\d [A-Z][A-Z]"))
				addressOnServer = addressOnServer.substring(0, addressOnServer.length() - 4).trim();
		}

		String streetName = StringFormats.StreetName(addressOnServer);
		String streetNo = StringFormats.StreetNo(addressOnServer);

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);

	}

	public static void parseLegalDescription(ResultMap resultMap, String tmpLegalDescription) {
		if (tmpLegalDescription.equals(""))
			return;

		String legalDescriptionFake = tmpLegalDescription.replace(" .", ".");
		legalDescriptionFake = legalDescriptionFake.replaceFirst("(?is)MHP?\\b\\s+ATTACH(?:ME\\s*NTS)?", "");

		// patterns
		Pattern LEGAL_SEC_PATTERN = Pattern.compile("\\bSEC\\s+(\\d+[^\\s]*)");
		Pattern LEGAL_TWP_PATTERN = Pattern.compile("\\bTWP\\s+(\\d+[^\\s]*)");
		Pattern LEGAL_RGE_PATTERN = Pattern.compile("\\bRGE\\s+(\\d+[^\\s]*)");
		Pattern LEGAL_LOT_PATTERN = Pattern.compile("\\bLOT\\s+([^\\s]*)");
		Pattern LEGAL_LOTS_PATTERN = Pattern.compile("\\bLOTS\\s+([^\\s]*)");
		Pattern LEGAL_BOOK_PATTERN = Pattern.compile("\\bPLAT BOOK\\s+([^\\s]*)");
		Pattern LEGAL_PAGE_PATTERN = Pattern.compile("\\bPAGE\\s+([^\\s]*)");
		Pattern LEGAL_BLK_PATTERN = Pattern.compile("\\bBLK\\s+([^\\s]*)");
		Pattern LEGAL_UNIT_PATTERN = Pattern.compile("\\bUNIT\\s+([^\\s,-]*)");
		Pattern LEGAL_TRACT_PATTERN = Pattern.compile("\\bTRACT\\s+([^\\s,-]*)");

		Pattern LEGAL_NAME_PATTERN1 = Pattern.compile("PAGE \\d+(.*)BLK");
		Pattern LEGAL_NAME_PATTERN2 = Pattern.compile("PAGE \\d+(.*)SEC ");
		Pattern LEGAL_NAME_PATTERN3 = Pattern.compile("PAGE \\d+(.*)UNIT");
		Pattern LEGAL_NAME_PATTERN4 = Pattern.compile("PAGE \\d+(.*)PHASE");
		Pattern LEGAL_NAME_PATTERN5 = Pattern.compile("PLAT BOOK U\\s*NR(?:EC(?:ORDED)?)?\\b\\s+(.*)(?:TRACT|BLK)");
		Pattern LEGAL_NAME_PATTERN6 = Pattern.compile("RGE \\d+([\\sA-Z]+)(?:TRACT|BLK|LOT)");

		// get subdiv name
		Pattern[] name = { LEGAL_NAME_PATTERN1, LEGAL_NAME_PATTERN2, LEGAL_NAME_PATTERN3, LEGAL_NAME_PATTERN4, LEGAL_NAME_PATTERN5, LEGAL_NAME_PATTERN6 };

		Matcher matcher = LEGAL_NAME_PATTERN1.matcher(legalDescriptionFake);
		String[] subdiv_name = new String[name.length];
		String div_name = "";

		for (int i = 0; i < name.length; i++) {
			matcher = name[i].matcher(legalDescriptionFake);
			if (matcher.find()) {
				subdiv_name[i] = matcher.group(1);
			}
			if (!StringUtils.isEmpty(subdiv_name[i]))
				div_name = subdiv_name[i];
		}

		// get shortest div name
		for (int i = 0; i < name.length; i++) {
			if (subdiv_name[i] != null && !div_name.equals("")
					&& !subdiv_name[i].equals("")
					&& subdiv_name[i].length() < div_name.length())
				div_name = subdiv_name[i];
		}
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), StringUtils.strip(div_name));

		legalDescriptionFake = legalDescriptionFake.replaceFirst("(?is)PLAT BOOK U\\s*NR(?:EC(?:ORDED)?)?\\b\\s+(?:PAGE\\s*\\d+)?", "");
		
		// get sec
		matcher = LEGAL_SEC_PATTERN.matcher(legalDescriptionFake);
		Vector<String> sections = new Vector<String>();
		while (matcher.find()) {
			sections.add(matcher.group(1));
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
		}
		
		if (sections.size() > 0) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sections.elementAt(0));
		}
		if (sections.size() > 1) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sections.elementAt(1));
		}

		// get twp
		String twp = "";
		matcher = LEGAL_TWP_PATTERN.matcher(legalDescriptionFake);
		if (matcher.find()) {
			twp = matcher.group(1);
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), twp);
		}

		// get range
		String rge = "";
		matcher = LEGAL_RGE_PATTERN.matcher(legalDescriptionFake);
		if (matcher.find()) {
			rge = matcher.group(1);
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), rge);
		}

		// get tract
		String tract = "";
		matcher = LEGAL_TRACT_PATTERN.matcher(legalDescriptionFake);
		if (matcher.find()) {
			tract = matcher.group(1);
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(tract));
		}
		
		// get plat book
		String book = "";
		matcher = LEGAL_BOOK_PATTERN.matcher(legalDescriptionFake);
		if (matcher.find()) {
			book = matcher.group(1);
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(book));
		}

		// get plat page
		String page = "";
		matcher = LEGAL_PAGE_PATTERN.matcher(legalDescriptionFake);
		if (matcher.find()) {
			page = matcher.group(1);
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(page));
		}

		// get blk
		String blk = "";
		matcher = LEGAL_BLK_PATTERN.matcher(legalDescriptionFake);
		if (matcher.find()) {
			blk = matcher.group(1);
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk);
		}

		// get lot or lots
		String lot = "";
		boolean lot_f = false;
		matcher = LEGAL_LOT_PATTERN.matcher(legalDescriptionFake);
		if (matcher.find()) {
			lot = matcher.group(1);
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
			lot_f = true;
		}

		if (!lot_f) {
			// get lots
			matcher = LEGAL_LOTS_PATTERN.matcher(legalDescriptionFake);
			Vector<String> lots = new Vector<String>();
			while (matcher.find()) {
				lots.add(matcher.group(1));
				legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			}
			if (lots.size() > 0) {
				// parse lots
				String lot_s = "";
				for (String s : lots) {
					lot_s += s + ".";
				}
					
				lot_s = lot_s.replaceAll("\\.+", ".").replace(".", " ").trim().replace(" ", ", ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot_s);
			}
		}

		// get unit
		String unit = "";
		matcher = LEGAL_UNIT_PATTERN.matcher(legalDescriptionFake);
		if (matcher.find()) {
			unit = matcher.group(1);
			legalDescriptionFake = legalDescriptionFake.replace(matcher.group(), " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
		}

		//get phase
		String phase = LegalDescription	.extractPhaseFromText(legalDescriptionFake);
		if (phase.length() > 0) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		}
	}

//	public static void parseDetailsGeneralData(Node generalDataTableTag, ResultMap resultMap, ArrayList<String> cities) {
	public static void parseDetailsGeneralData(Node generalDataTableTag, ResultMap resultMap, HashMap<String, String> citiesMap) {
		if (generalDataTableTag instanceof TableTag) {
			TableRow[] rows = ((TableTag) generalDataTableTag).getRows();
			NodeList tmpList = null;
			// account number
			tmpList =  rows[1].getColumns()[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
			Node span = null;
			if (tmpList != null) {
				span = tmpList.elementAt(0);
				String accNumber = span.toPlainTextString().replaceAll("&nbsp;", " ").trim();
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accNumber);
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getKeyName(), accNumber.replace("-", ""));
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), accNumber.replace("-", ""));
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID3.getKeyName(), accNumber.replace("-", "").replace("R", "").replace("P", ""));
			}

			// type
			tmpList =  rows[1].getColumns()[3].getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
			if (tmpList != null) {
				span = tmpList.elementAt(0);
				String propertyType = span.toPlainTextString().replaceAll("&nbsp;", " ").trim();
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), propertyType);
			}
			

			// address
			tmpList = rows[2].getColumns()[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
			if (tmpList != null) {
				span = tmpList.elementAt(0);
				String address = span.toPlainTextString().replaceAll("&nbsp;", " ").trim();
				//for (String city : cities) {
				for (String key : citiesMap.keySet()) {
					String city = key;
					String shortCityName = city.replaceFirst("(?is)(?:CITY|VILLAGES) OF ([A-Z\\s]+)", "$1");
					if (address.contains(city) || address.toUpperCase().contains(city) ||
						address.contains(shortCityName) || address.toUpperCase().contains(shortCityName)) {
							resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.toUpperCase());
							address = address.replaceAll("(?is)(?:(?:CITY|VILLAGES) OF\\s+|1\\s+)?" + shortCityName + "\\s*", "");
							address = address.replaceAll("(?is)\\s*" + city + "\\s*", "");
							break;
					}
				}
				parseAddress(resultMap, address);
			}
			

			// status
			tmpList = rows[2].getColumns()[3].getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
			if (tmpList != null) {
				span = tmpList.elementAt(0);
			}
			
			// sec/twn/rng
			tmpList = rows[3].getColumns()[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
			if (tmpList != null) {
				span = tmpList.elementAt(0);
				String secTwnRng = span.toPlainTextString().replaceAll("&nbsp;", " ").trim().replaceAll("\\s+", " ");
				String[] strings = StringUtils.split(secTwnRng);
				if (strings.length == 3) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), strings[0].trim());
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), strings[1].trim());
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), strings[2].trim());
				}
			}
			

			// subdivision
			tmpList = rows[3].getColumns()[3].getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
			if (tmpList != null) {
				span = tmpList.elementAt(0);
				String subdivision = span.toPlainTextString().replaceAll("&nbsp;", " ").trim();
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION.getKeyName(), subdivision);
			}
		}
	}

	public static void parseTaxHistorySet(NodeList taxNodeTag, ResultMap resultMap) {
		int currentYear = 0;

		NodeList taxDetailsNodelist = HtmlParser3.getNodesByID("tbl5", taxNodeTag, true);
		SimpleNodeIterator iterator = taxDetailsNodelist.elements();

		String datePattern = "\\d{2}/\\d{2}/\\d{4}";
		String receiptPattern = "\\d+\\s\\d{4}\\s\\d+\\.\\d+";
		LinkedList<HashMap<String, String>> taxData = new LinkedList<HashMap<String, String>>();

		while (iterator.hasMoreNodes()) {
			Node tableTag = iterator.nextNode();
//			Node testChild = tableTag.getChildren().elementAt(0).getFirstChild();
			TableTag taxTbl = (TableTag)tableTag;
			if (taxTbl.getRowCount() >= 4) {
				Node testChild = taxTbl.getRow(4);
				if (testChild != null) {
					//String yearKey = testChild.toPlainTextString().trim();

					// get last color filled text
					//NodeList class_color = tableTag.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "colorfixedtext"), true);
					NodeList class_color = tableTag.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "PropColorFixedText"), true);
					Node n = class_color.elementAt(class_color.size() - 1).getParent();

					NodeList class_fixed = tableTag.getChildren();
					NodeList elems = new NodeList();
					int cnt = -1;

					for (int i = 0; i < class_fixed.size(); i++) {
						if (class_fixed.elementAt(i).equals(n)) {
							cnt = i;
						}
					}

					if (cnt != -1) {
						for (int i = cnt + 1; i < class_fixed.size(); i++) {
							if (class_fixed.elementAt(i) instanceof TableRow) {
								elems.add(class_fixed.elementAt(i));
							}
						}

						for (int i = 0; i < elems.size(); i++) {
							Node elem = elems.elementAt(i);
							if (elem != null) {
								NodeList children = elem.getChildren();
								String text = children.elementAt(1).getFirstChild().toPlainTextString().replaceAll("&nbsp;", " ");

								Pattern pattern = Pattern.compile(datePattern);
								Matcher matcher = pattern.matcher(text);
								String toFind = "";
								if (matcher.find()) {
									toFind = matcher.group();
								}
								String receiptDate = toFind;
								
								Pattern receiptCompile = Pattern.compile(receiptPattern);

								matcher = receiptCompile.matcher(text);
								if (matcher.find()) {
									toFind = matcher.group();
								}
								String receiptNumber = toFind;
								
								int lastIndexOf = text.lastIndexOf("$");
								String paymentAmount = "";
								if (lastIndexOf > -1) {
									paymentAmount = text.substring(lastIndexOf);
								}

								HashMap<String, String> valuesMap = new HashMap<String, String>();
								valuesMap.put("ReceiptNumber", receiptNumber);
								valuesMap.put("ReceiptAmount", paymentAmount.replaceAll(",", "").replaceAll("\\$", ""));
								valuesMap.put("ReceiptDate", receiptDate);
								String year = receiptDate.substring(receiptDate.lastIndexOf("/") + 1).trim();
								if (NumberUtils.isNumber(year) && year.matches("\\d{4}")) {
									valuesMap.put("Year", year);
									taxData.add(valuesMap);
								}
							}
						}
					}
				}
			}
			
			
		}

		// this table is the one for the selected year
		Node currentAccountDetails = HtmlParser3.getNodeByID("tbl3", taxNodeTag, true);
		Node taxYear = HtmlParser3.getNodeByID("_ctl0_ContentPlaceHolder1_lblDetTaxYear", currentAccountDetails.getChildren(), true);

		if (NumberUtils.isNumber(taxYear.getFirstChild().toPlainTextString())) {
			currentYear = Integer.valueOf(taxYear.getFirstChild().toPlainTextString());
		}

		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), "" + currentYear);
		Node currentValuesAndExemptions = HtmlParser3.getNodeByID("Table2c", taxNodeTag, true);

		TableRow[] table2c_r = ((TableTag) currentValuesAndExemptions).getRows();
		String assessedValue = "";
		for (TableRow r : table2c_r) {
			if (r.toPlainTextString().contains("ASSESSMENT") || r.toPlainTextString().contains("ASSESSED")) {
				assessedValue = r.toPlainTextString();
				break;
			}
		}
		assessedValue = assessedValue.replaceAll("&nbsp;", " ").replaceAll("ASSESSMENT", "").replace("ASSESSED", "");
		assessedValue = StringUtils.strip(assessedValue).replaceAll(",", "").replaceAll("\\$", "");

		resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessedValue);

		Node taxesAndFeesLevied = HtmlParser3.getNodeByID("Table2d", taxNodeTag, true);

		TableRow[] table2d_r = ((TableTag) taxesAndFeesLevied).getRows();
		String baseAmount = "";
		String sp_asmt = "";
		String currentYearDue = "";
		for (TableRow r : table2d_r) {
			if (r.toPlainTextString().contains("TAXES")) {
				baseAmount = r.toPlainTextString();
				// break;
			}
			if (r.toPlainTextString().replace("&nbsp;", " ").contains("SP. ASMT")) {
				sp_asmt = r.toPlainTextString();
				// break;
			}
			if (r.toPlainTextString().contains("TOTAL")) {
				currentYearDue = r.toPlainTextString();
				// break;
			}
		}
		baseAmount = baseAmount.replaceAll("&nbsp;", " ").replaceAll("TAXES", "");
		baseAmount = StringUtils.strip(baseAmount).replaceAll("[,\\$]", "").trim();

		sp_asmt = sp_asmt.replaceAll("&nbsp;", " ").replaceAll("SP. ASMT", "");
		sp_asmt = StringUtils.strip(sp_asmt).replaceAll("[,\\$]", "").trim();

		currentYearDue = currentYearDue.replaceAll("&nbsp;", " ").replaceAll("TOTAL", "");
		currentYearDue = StringUtils.strip(currentYearDue).replaceAll("[,\\$]", "").trim();

		double ba = 0;
		double sp = 0;
		if (NumberUtils.isNumber(baseAmount)) {
			ba = Double.valueOf(baseAmount);
		}
		if (NumberUtils.isNumber(sp_asmt)) {
			sp = Double.valueOf(sp_asmt);
		}
		
		//resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), ba + sp + "");
		resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), String.format("%.2f",  ba + sp));
		resultMap.put(TaxHistorySetKey.CURRENT_YEAR_DUE.getKeyName(), currentYearDue);
		resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), currentYearDue);

		TableTag tableTag = (TableTag) taxDetailsNodelist.elementAt(taxDetailsNodelist.size() - 1);

		if (tableTag != null) {
			TableRow[] rs = tableTag.getRows();
			TableRow r = null; // last receipt
			if (rs.length > 0)
				r = rs[rs.length - 1];

			int index = 0;

			for (int i = 0; i < rs.length; i++) {
				if (rs[i].toHtml().contains("_ctl0_ContentPlaceHolder1_lblPaymentHeading")) {
					index = i;
				}
			}

			String s = null;
			double amp = 0;

			for (int i = index + 1; i < rs.length; i++) {
				if (rs[i].toHtml().contains("PropFixedText")) {
					s = rs[i].toPlainTextString().replace("&nbsp;", " ").replaceAll("\\s+", " ");
					int lastIndexOf = s.lastIndexOf("$");
					if (lastIndexOf > -1) {
						amp += Double.valueOf(s.substring(lastIndexOf).replaceAll("[,\\$]", "").replace(" ", ""));
					}
				}
			}
			if (s != null) {
				resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amp + "");
				String receiptDate = ro.cst.tsearch.utils.StringUtils.parseByRegEx(s, datePattern, 0);
				resultMap.put(TaxHistorySetKey.RECEIPT_DATE.getKeyName(), StringUtils.defaultIfEmpty(receiptDate, ""));
			}

		}

		// flag = true if Total was found => no need to calculate TotalDue again for current year
		boolean flag = false;
		if (currentYearDue.equals(""))
			flag = true;

		calculateDelinquentAmount(taxNodeTag, resultMap, currentYear, flag);
		populateTaxHistorySetResultTable(resultMap, taxData, currentYear);
	}

	private static Map<String, String> calculateDelinquentAmount(NodeList taxNodeTag, ResultMap resultMap, int currentYear, boolean flag) {
		TableTag table = (TableTag) HtmlParser3.getNodeByID("_ctl0_ContentPlaceHolder1_tblSummary", taxNodeTag, true);
		TableRow[] rows = table.getRows();
		double priorDelinquent = 0;
		Map<String, String> map = new HashMap<String, String>();

		for (TableRow tableRow : rows) {
			if (tableRow.getColumnCount() > 6) {
				TableColumn[] col = tableRow.getColumns();

				String amount = col[6].toPlainTextString().replace(",", "").trim();

				String thisYear = col[0].toPlainTextString().trim();

				if (StringUtils.isNotEmpty(amount) && NumberUtils.isNumber(amount)) {
					if (StringUtils.isNumeric(thisYear)) {
						if (Integer.valueOf(thisYear) < currentYear) {
							priorDelinquent += Double.valueOf(amount);
						}
						if (flag && Integer.valueOf(thisYear) == currentYear) {
							resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), Double.valueOf(amount) + "");
						}
					}
				}
			}
		}
		resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent + "");
		return map;
	}

	private static void populateTaxHistorySetResultTable(ResultMap resultMap,
			LinkedList<HashMap<String, String>> taxData, int requestedTaxYear) {
		ResultTable receipts = new ResultTable();
		Map<String, String[]> tempMap = new HashMap<String, String[]>();
		String[] header = { "ReceiptNumber", "ReceiptAmount", "ReceiptDate" };
		List<List<String>> bodyRT = new ArrayList<List<String>>();

		tempMap.put("ReceiptNumber", new String[] { "ReceiptNumber", "" });
		tempMap.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
		tempMap.put("ReceiptDate", new String[] { "ReceiptDate", "" });

		for (HashMap<String, String> m : taxData) {
			List<String> paymentRow = new ArrayList<String>();
			paymentRow.add(m.get("ReceiptNumber"));
			paymentRow.add(m.get("ReceiptAmount"));
			paymentRow.add(m.get("ReceiptDate"));
			bodyRT.add(paymentRow);
		}
		try {
			receipts.setHead(header);
			receipts.setMap(tempMap);
			receipts.setBody(bodyRT);
			receipts.setReadOnly();
			resultMap.put("TaxHistorySet", receipts);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

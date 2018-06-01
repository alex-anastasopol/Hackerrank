package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.date.DateInterval;
import ro.cst.tsearch.utils.date.MonthFormat;

public class FLLeonTR {

	public static void parseDetailsGeneralData(Node generalDataTableTag, ResultMap resultMap) {
		if (generalDataTableTag instanceof TableTag) {
			TableRow[] rows = ((TableTag) generalDataTableTag).getRows();

			Node span = rows[1].getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_lblHdrPropertyNumber"), true).elementAt(0);
			
			String accNumber = span.toPlainTextString().replaceAll("&nbsp;", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accNumber);

			span = rows[1].getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_lblHdrPropertyType"), true).elementAt(0);
			String propertyType = span.toPlainTextString().replaceAll("&nbsp;", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), propertyType);
			
			span = rows[2].getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_lblHdrLocn"), true).elementAt(0);
			String address = span.toPlainTextString().replaceAll("&nbsp;", " ").trim();
			parseAddress(resultMap, address);

			span = rows[3].getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_lblHdrSecTwnRng"), true).elementAt(0);
			String secTwnRng = span.toPlainTextString().replaceAll("&nbsp;", " ").trim().replaceAll("\\s+", " ");
			String[] strings = StringUtils.split(secTwnRng);
			if (strings.length == 3) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), strings[0].trim());
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), strings[1].trim());
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), strings[2].trim());
			}

			span = rows[3].getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_lblHdrSubdiv"), true).elementAt(0);
			String subdivision = span.toPlainTextString().replaceAll("&nbsp;", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
		}
	}

	public static void parseSaleDataSetSet(NodeList taxNodeTag, ResultMap resultMap) {

	}

	public static void parseTaxHistorySet(NodeList taxNodeTag, ResultMap resultMap) {
		int currentYear = 0;

		NodeList taxDetailsNodelist = HtmlParser3.getNodesByID("tbl5", taxNodeTag, true);
		SimpleNodeIterator iterator = taxDetailsNodelist.elements();

		String datePattern = "\\d{2}/\\d{2}/\\d{4}";
		String receiptPattern = "\\d+\\s\\d{4}\\s\\d+\\.\\d+";
		// String amountPattern = "(?<=\\$)(\\d+|).\\d+";
		LinkedList<HashMap<String, String>> taxData = new LinkedList<HashMap<String, String>>();

		while (iterator.hasMoreNodes()) {
			Node tableTag = iterator.nextNode();
			Node testChild = tableTag.getChildren().elementAt(0).getFirstChild();
			if (testChild != null) {
				String yearKey = testChild.toPlainTextString();
				
				NodeList nodes = tableTag.getChildren();
				
				if(nodes.size()>=8){
					for (int j = 8; j < nodes.size(); j++) {

						Node elem = nodes.elementAt(j);

						if (elem != null && elem instanceof TableRow) {
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
							if (NumberUtils.isNumber(yearKey)) {
								valuesMap.put("Year", yearKey);
								taxData.add(valuesMap);
							}
						}
					}
				}
			} else {// this table is the one for the selected year
				Node currentAccountDetails = HtmlParser3.getNodeByID("tbl3", taxNodeTag, true);
				Node taxYear = HtmlParser3.getNodeByID("_ctl0_ContentPlaceHolder1_lblDetTaxYear", currentAccountDetails.getChildren(), true);

				if (NumberUtils.isNumber(taxYear.getFirstChild().toPlainTextString())) {
					currentYear = Integer.valueOf(taxYear.getFirstChild().toPlainTextString());
				}

				resultMap.put("TaxHistorySet.Year", "" + currentYear);
				Node currentValuesAndExemptions = HtmlParser3.getNodeByID("Table2c", taxNodeTag, true);
				String assessedValue = currentValuesAndExemptions.getChildren().elementAt(1).getChildren().elementAt(1).getFirstChild()
						.toPlainTextString();
				String amountPattern = "((?is)\\d*,?\\d*\\.?\\d*)";// "\\d+,?\\d+\\.?\\d+";
				// (?im)\\d*,?\\d*\\.?\\d*
				assessedValue = assessedValue.replaceAll("&nbsp;", " ").replaceAll("ASSESSMENT", "");
				assessedValue = StringUtils.strip(assessedValue);
				assessedValue = simplePatternMatching(amountPattern, assessedValue).replaceAll(",", "").replaceAll("\\$", "");

				resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessedValue);

				Node taxesAndFeesLevied = HtmlParser3.getNodeByID("Table2d", taxNodeTag, true);
				String baseAmount = taxesAndFeesLevied.getChildren().elementAt(1).getChildren().elementAt(1).getFirstChild()
						.toPlainTextString();
				// look for interest and advertisment taxes that should be
				// removed from baseamount
				SimpleNodeIterator taxesRows = taxesAndFeesLevied.getChildren().elements();
				BigDecimal interestsAndAdvertismentTaxes = new BigDecimal(0);
				while (taxesRows.hasMoreNodes()) {
					Node nextNode = taxesRows.nextNode();
					if (nextNode instanceof TableRow) {
						String value = ro.cst.tsearch.utils.StringUtils.cleanHtml(nextNode.toPlainTextString());
						if (value.contains("INT.") || value.contains("ADV. FEE") || value.contains("INT. ADV")) {
							value = value.replaceAll("INT.*(?=%)", "");// \\b((?is)\\d*,?\\d*\\.?\\d*)\\b
							value = simplePatternMatching("\\b[0-9]{1,3}(,[0-9]{3})*(\\.[0-9]+)?\\b|\\.[0-9]+\\b", value);// \\b[0-9]{1,3}(,[0-9]{3})*(\\.[0-9]+)?\\b|\\.[0-9]+\\b
							if (NumberUtils.isNumber(value)) {
								interestsAndAdvertismentTaxes = interestsAndAdvertismentTaxes.add(new BigDecimal(value));
							}
						}
					}
				}

				baseAmount = StringUtils.strip(baseAmount);
				baseAmount = baseAmount.replaceAll("&nbsp;", " ").replaceAll("TAXES", "");
				baseAmount = StringUtils.strip(baseAmount);
				baseAmount = simplePatternMatching(amountPattern, baseAmount).replaceAll(",", "").replaceAll("\\$", "");
				
				// resultMap.put("TaxHistorySet.BaseAmount", baseAmount);
				String currentYearDue = taxesAndFeesLevied.getLastChild().getPreviousSibling().toPlainTextString();
				currentYearDue = ro.cst.tsearch.utils.StringUtils.cleanHtml(currentYearDue.replaceAll(",", "").replaceAll("\\$", "")
						.replaceAll("TOTAL", ""));
				currentYearDue = simplePatternMatching(amountPattern, currentYearDue);

				HtmlParser3.getNodeByID("Table2d", taxNodeTag, true);
				baseAmount = currentYearDue;
				if (NumberUtils.isNumber(currentYearDue)) {
					BigDecimal a = new BigDecimal(currentYearDue);
					BigDecimal b = interestsAndAdvertismentTaxes;
					baseAmount = "" + (a.subtract(b));
				}
				resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
				
//				resultMap.put("TaxHistorySet.CurrentYearDue", currentYearDue);
//				resultMap.put("TaxHistorySet.TotalDue", currentYearDue);
//				if (tableTag.getChildren().size() >= 8) {
//					String amountPaid = tableTag.getChildren().elementAt(7).toPlainTextString();
//					String receiptDate = ro.cst.tsearch.utils.StringUtils.parseByRegEx(amountPaid, datePattern, 0);
//					resultMap.put("TaxHistorySet.ReceiptDate", StringUtils.defaultIfEmpty(receiptDate, ""));
//					int lastIndexOf = amountPaid.lastIndexOf("$");
//					if (lastIndexOf > -1) {
////						if (amountPaid.contains("Full")) {
//							// only for payment Full
//							resultMap.put("TaxHistorySet.CurrentYearDue", "");
//							resultMap.put("TaxHistorySet.TotalDue", "");
////						}
//						amountPaid = amountPaid.substring(lastIndexOf).replaceAll(",", "").replaceAll("\\$", "");
//						resultMap.put("TaxHistorySet.AmountPaid", StringUtils.strip(amountPaid));
//						
//						
//					}
//				} else {
//					resultMap.put("TaxHistorySet.AmountPaid", "");
//
//					currentYearDue = getAmountPaidForAGivenDate(tableTag, currentYear, Calendar.getInstance());
//					// amount due is put only if the current date is in the
//					// given intervals
//					// if it is delinquent then amount due is taken from TOTAL
//					if (StringUtils.isNotEmpty(currentYearDue)) {
//						resultMap.put("TaxHistorySet.TotalDue", currentYearDue);
//					}
//				}

				// TO DO : priorDelinquent
				// resultMap.put("TaxHistorySet.PriorDelinquent",priorDelinquent+"");

			}
		}
		
		NodeList tblSummary = taxNodeTag.extractAllNodesThatMatch(new TagNameFilter("table"),true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_tblSummary"));
		
		if(tblSummary.size() > 0){
			TableTag tSum = (TableTag) tblSummary.elementAt(0);
			TableRow[] r = tSum.getRows();
			
			if(r.length > 0){
				TableRow cYear = r[r.length-1];
				if(cYear.getColumnCount() > 6 && cYear.getColumns()[0].toPlainTextString().equals(currentYear+"")){
					String amountPaid = cYear.getColumns()[5].toPlainTextString().replaceAll("[$,-]", "").trim(); 
					String amountDue =  cYear.getColumns()[6].toPlainTextString().replaceAll("[$,-]", "").trim(); 
					if(StringUtils.isNotEmpty(amountPaid)){
						//get amount date
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
						
						NodeList taxNodes = taxNodeTag.extractAllNodesThatMatch(new TagNameFilter("table"),true).extractAllNodesThatMatch(new HasAttributeFilter("id","tbl5"));
						
						if(taxNodes.size()>0){
							TableTag t = (TableTag) taxNodes.elementAt(taxNodes.size()-1);
							
							TableRow[] rs = t.getRows();
							
							if(rs.length>=5 && rs[0].toPlainTextString().trim().equals(currentYear+"")){
								String date = rs[rs.length-1].toPlainTextString().trim();
								date = date.replaceAll("(?ism)(\\d{1,2}/\\d{1,2}/\\d{4}).*", "$1");
								if(date.length()<=10)
									resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), date);
							}
						}
					}
					if(StringUtils.isNotEmpty(amountDue)){
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
					}
				}
			}
		}
		
		calculateDelinquentAmount(taxNodeTag, resultMap, currentYear);
		populateTaxHistorySetResultTable(resultMap, taxData, currentYear);
	}

	public static String getAmountPaidForAGivenDate(Node tableTag, int year, Calendar currentDate) {
		NodeList children = tableTag.getChildren();
		String amountPaid = "";
		if (children.size() == 9) {
			String intervals = ro.cst.tsearch.utils.StringUtils.cleanHtml(children.elementAt(1).toPlainTextString());
			intervals = intervals.replaceAll("IF PAID BY", "");
			List<String> monthIntervals = RegExUtils.parseValuesForRegEx(intervals, "\\w{3}\\s*?\\d{1,2}-\\s?\\w{3}\\s*?\\d{1,2}", false);
			List<DateInterval> dates = new ArrayList<DateInterval>();
			for (String string : monthIntervals) {
				String[] split = string.split("-");
				if (split.length == 2) {
					String minValue = split[0];

					// if (!NumberUtils.isNumber(year)) {
					// year = "" + Calendar.getInstance().get(Calendar.YEAR);
					// }

					String monthRegex = "[A-Z]+";
					String dayRegex = "\\d{1,2}";

					Calendar minDate = Calendar.getInstance();
					minDate.set(Calendar.YEAR, Integer.valueOf(year));
					MonthFormat month = MonthFormat.getMonthByName(RegExUtils.parseValuesForRegEx(minValue, monthRegex).trim());
					minDate.set(Calendar.MONTH, month.value() - 1);
					String day = RegExUtils.parseValuesForRegEx(minValue, dayRegex).trim();
					minDate.set(Calendar.DAY_OF_MONTH, Integer.valueOf(day));

					String maxValue = split[1];
					Calendar maxDate = Calendar.getInstance();
					maxDate.set(Calendar.YEAR, Integer.valueOf(year));
					month = MonthFormat.getMonthByName(RegExUtils.parseValuesForRegEx(maxValue, monthRegex).trim());
					maxDate.set(Calendar.MONTH, month.value() - 1);
					day = RegExUtils.parseValuesForRegEx(maxValue, dayRegex).trim();
					maxDate.set(Calendar.DAY_OF_MONTH, Integer.valueOf(day));

					DateInterval dateInterval = new DateInterval(minDate, maxDate);
					dates.add(dateInterval);
				}
			}

			// put the proper year in date intervals
			boolean encounteredJanuary = false;
			int yearToPut = year;
			for (int j = dates.size() - 1; j >= 0; j--) {
				DateInterval dateInterval = dates.get(j);
				// intervals are only on month periods
				encounteredJanuary = (dateInterval.getIntervalStart().get(Calendar.MONTH) == MonthFormat.JANUARY.value() - 1);
				if (encounteredJanuary) {
					yearToPut = yearToPut - 1;
					encounteredJanuary = false;
				}
				dateInterval.getIntervalStart().set(Calendar.YEAR, yearToPut);
				dateInterval.getIntervalEnd().set(Calendar.YEAR, yearToPut);
			}

			String amountsString = ro.cst.tsearch.utils.StringUtils.cleanHtml(children.elementAt(3).toPlainTextString());
			// replace PLEASE PAY
			amountsString = amountsString.replaceAll("PLEASE PAY", "");

			// replaceDelinquentDate
			amountsString = amountsString.replaceAll("\\b[A-Z]+\\s*\\d{1,2}", "");

			List<String> amounts = RegExUtils.parseValuesForRegEx(amountsString, "\\b[0-9]{1,3}(,[0-9]{3})*(\\.[0-9]+)?\\b|\\.[0-9]+\\b",
					false);

			if (amounts.size() == dates.size()) {
				// Calendar currentDate = Calendar.getInstance();
				for (int i = 0; i < dates.size(); i++) {
					DateInterval interval = dates.get(i);
					if (interval.isInInterval(currentDate)) {
						amountPaid = amounts.get(i);
					}
				}
			}

		}
		return amountPaid;
	}

	private static Map<String, String> calculateDelinquentAmount(NodeList taxNodeTag, ResultMap resultMap, int currentYear) {
		TableTag table = (TableTag) HtmlParser3.getNodeByID("_ctl0_ContentPlaceHolder1_tblSummary", taxNodeTag, true);
		TableRow[] rows = table.getRows();
		double priorDelinquent = 0;
		Map<String, String> map = new HashMap<String, String>();

		for (TableRow tableRow : rows) {
			if (tableRow.getChildren().size() > 6) {
				Node child = tableRow.getChild(7);
				String amount = child.toPlainTextString().replace(",", "").trim();
				String typeOfCertificate = tableRow.getChild(2).toPlainTextString().trim();
				String thisYear = tableRow.getChild(1).toPlainTextString().trim();
				String amountPaid = tableRow.getChild(6).toPlainTextString().replace(",", "").trim();

				if (StringUtils.isNotEmpty(amount) && NumberUtils.isNumber(amount)) {
					if (StringUtils.isNumeric(thisYear)) {
						if (Integer.valueOf(thisYear) < currentYear) {
							priorDelinquent += Double.valueOf(amount);
						}
					}
				}
				map.put("" + thisYear + "_" + typeOfCertificate, amountPaid);
			}
		}
		resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent + "");
		return map;
	}

	public static String simplePatternMatching(String pattern, String stringToParse) {
		Pattern compile = Pattern.compile(pattern);
		Matcher matcher = compile.matcher(StringUtils.strip(stringToParse));
		String returnedValue = "";
		if (matcher.find()) {
			returnedValue = matcher.group();
		}
		return returnedValue;
	}

	private static void populateTaxHistorySetResultTable(ResultMap resultMap, LinkedList<HashMap<String, String>> taxData,
			int requestedTaxYear) {
		ResultTable receipts = new ResultTable();
		Map<String, String[]> tempMap = new HashMap<String, String[]>();
		String[] header = { "ReceiptNumber", "ReceiptAmount", "ReceiptDate" };
		List<List<String>> bodyRT = new ArrayList<List<String>>();

		tempMap.put("ReceiptNumber", new String[] { "ReceiptNumber", "" });
		tempMap.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
		tempMap.put("ReceiptDate", new String[] { "ReceiptDate", "" });
		//int currentYear = 0;
		for (HashMap<String, String> m : taxData) {
			List<String> paymentRow = new ArrayList<String>();
			//currentYear = Integer.valueOf(m.get("Year"));
			// if (currentYear < requestedTaxYear) {
			paymentRow.add(m.get("ReceiptNumber"));
			paymentRow.add(m.get("ReceiptAmount"));
			paymentRow.add(m.get("ReceiptDate"));
			bodyRT.add(paymentRow);
			// }
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

	public static void parseName(ResultMap resultMap, String tmpOwnerName) {
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), tmpOwnerName);

		// tmpOwnerName = tmpOwnerName.replaceAll("C/O", "");
		tmpOwnerName = tmpOwnerName.replaceAll("AS (TRUSTEE)", " $1");
		tmpOwnerName = tmpOwnerName.replaceAll("CO (TRUSTEE)", " $1");
		tmpOwnerName = tmpOwnerName.replaceAll("CO-(TRUSTEE)", " $1");
		tmpOwnerName = tmpOwnerName.replaceAll("(TRUSTEE)", " $1");
		tmpOwnerName = tmpOwnerName.replaceAll("ATTY", "");
		tmpOwnerName = tmpOwnerName.replaceAll("\\bD\\.?B\\.?A\\.?\\b", "");
		
		if (tmpOwnerName.contains("WILSON ALEXANDER BERNARD WILSON")){//1528206560010
			tmpOwnerName = tmpOwnerName.replace("WILSON ALEXANDER BERNARD WILSON", "WILSON ALEXANDER AND WILSON BERNARD");
		}
		// tmpOwnerName = tmpOwnerName.replace("&", "AND");:)
		List<String> ownerList = Arrays.asList(tmpOwnerName.split("\\bAND\\b"));
		parseName(resultMap, ownerList);
	}

	@SuppressWarnings("rawtypes")
	public static void parseName(ResultMap resultMap, List<String> tmpOwnerName) {
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), StringUtils.join(tmpOwnerName, " "));
		List body = new ArrayList<String>();
		for (String name : tmpOwnerName) {
 			name = GenericFunctions2.cleanOwnerNameFromPrefix(name);
 			name = reviewNamesFormatForMultipleLastNames(name);
			//if (name.matches(regex))
			// C/O ANDREW E & JUDITH W ANDERSON TRUSTEE
			if (name.contains("C/O")) {
				name = name.replaceAll("C/O", "");
				if (name.contains("&")) {
					String[] tmp = name.split("&");
					if (tmp.length == 2) {
						String[] tmp1 = tmp[1].split(" ");
						String lastName = tmp1[tmp1.length - 1];
						name = tmp[0] + " " + lastName + " &" + tmp[1];
					}
				}
				Pattern compile = Pattern.compile("\\d+");
				Matcher matcher = compile.matcher(name);
				boolean matches = matcher.find();// Pattern.matches("\\d+",
				//digits present = > it is a company
				if(matches){
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, name, body);
				}else{
					ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, name, body);
				}
			} else {
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, name, body);
			}
		}
	}

	private static String reviewNamesFormatForMultipleLastNames(String names) {
		String correctedNames = names;
		correctedNames = correctedNames.replaceAll("(?is)(?:\\bREP\\b\\s+)?\\bPERS\\b\\s+(?:\\bREP\\b)?", "");
		return correctedNames;
	}

	public static void parseAddress(ResultMap resultMap, String addressOnServer) {
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addressOnServer);
		// clean
		addressOnServer = addressOnServer.replaceAll("(?is)\\b(TAL|POR|ALE|OAK|CRA|PEM|MIR|HAV|LAK|HOP|DET|HOU|LUT|ROY|MON|CHA|QUI|WIC|JAC|MIA|CAL|FOR|NIC|SUM|HOL|ORL|KIS|TAM|PAN|MAR|WIN|TUN)\\b", "");
		String streetName = StringFormats.StreetName(addressOnServer);
		String streetNo = StringFormats.StreetNo(addressOnServer);

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);

	}

	public static void parseLegalDescription(ResultMap resultMap, String tmpLegalDescription) {
		// clean
		tmpLegalDescription = tmpLegalDescription.replaceAll("(?is)(OR|OF)", "").replaceAll("\\bRUN\\b", "");

		// first take de Book Page
		tmpLegalDescription = setBookPage(resultMap, tmpLegalDescription);
		setLot(resultMap, tmpLegalDescription);

		tmpLegalDescription = setBlock(resultMap, tmpLegalDescription);

		tmpLegalDescription = setSection(resultMap, tmpLegalDescription);

		tmpLegalDescription = setUnit(resultMap, tmpLegalDescription);

		tmpLegalDescription = setTract(resultMap, tmpLegalDescription);

		tmpLegalDescription = setPhase(resultMap, tmpLegalDescription);

		tmpLegalDescription = setBuilding(resultMap, tmpLegalDescription);
	}

	public static String setBuilding(ResultMap resultMap, String tmpLegalDescription) {
		String buildingRegEx = "(?is)(BUILDING|BLDG)\\s*(\\w)";
		String parseByRegEx = ro.cst.tsearch.utils.StringUtils.parseByRegEx(tmpLegalDescription, buildingRegEx, 2);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), parseByRegEx);
		return tmpLegalDescription.replaceAll(buildingRegEx, "");
	}

	private static String setPhase(ResultMap resultMap, String tmpLegalDescription) {
		String parseBy = "(?is)\\bPHASE\\s?([A-Z0-9]+-?\\w?)";
		String[] findRepeatedOcurrence = ro.cst.tsearch.utils.StringUtils.findRepeatedOcurrence(tmpLegalDescription, " PHASE", parseBy, 1);
		String phase = StringUtils.join(findRepeatedOcurrence);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		return tmpLegalDescription.replaceAll(parseBy, "");
	}

	private static String setTract(ResultMap resultMap, String tmpLegalDescription) {
		String parseBy = "(?is)\\bTRACT\\s?([A-Z0-9]+-?\\w?)";
		String[] findRepeatedOcurrence = ro.cst.tsearch.utils.StringUtils.findRepeatedOcurrence(tmpLegalDescription, " TRACT", parseBy, 1);
		String tract = StringUtils.join(findRepeatedOcurrence);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
		return tmpLegalDescription.replaceAll(parseBy, "");
	}

	private static String setUnit(ResultMap resultMap, String tmpLegalDescription) {
		String parseBy = "(?is)\\bUNIT\\s?([A-Z0-9]+-?\\d?)";
		String[] findRepeatedOcurrence = ro.cst.tsearch.utils.StringUtils.findRepeatedOcurrence(tmpLegalDescription, " UNIT", parseBy, 1);
		String unit = StringUtils.join(findRepeatedOcurrence);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
		return tmpLegalDescription.replaceAll(parseBy, "");
	}

	private static String setBlock(ResultMap resultMap, String tmpLegalDescription) {
		/*
		 * Pattern pattern = Pattern.compile("\\b(BLOCK|BLK)(\\s?\\w+)");
		 * Matcher matcher = pattern.matcher(tmpLegalDescription); String block
		 * = ""; if (matcher.find()){ block = matcher.group(2); }
		 * resultMap.put("PropertyIdentificationSet.SubdivisionBlock", block);
		 * tmpLegalDescription = matcher.replaceAll("");
		 */
		String blockRegEx = "\\b(BLOCK|BLK)(\\s?\\w+)";
		String[] findRepeatedOcurrence = ro.cst.tsearch.utils.StringUtils
				.findRepeatedOcurrence(tmpLegalDescription, "BLOCK", blockRegEx, 2);
		String block = StringUtils.join(findRepeatedOcurrence, " ").replaceAll("\\s+", " ");
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		return tmpLegalDescription.replaceAll(blockRegEx, "");
	}

	private static String setSection(ResultMap resultMap, String tmpLegalDescription) {
		String sectionRegEx = "(?is)SECTS? ([\\d+\\s]+\\b)";
		String successionOFValues = getSuccessionOFValues(tmpLegalDescription, "SECT", sectionRegEx, 1);
		String val = successionOFValues.replaceAll("\\s+", " ");
		if (StringUtils.isNotEmpty(val)) {
			String currValue = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName());
			if (StringUtils.isEmpty(currValue)) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), val);
			}
		}
		return tmpLegalDescription;
	}

	private static String setLot(ResultMap resultMap, String tmpLegalDescription) {
		if (tmpLegalDescription.contains("LOT")) {
			int lotIndex = tmpLegalDescription.indexOf("LOTS");
			String regExLots = "(?is)LOTS ([\\d+\\s]+)";
			if (lotIndex >= 0) {
				String lots = getSuccessionOFValues(tmpLegalDescription, lotIndex, regExLots);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots.replaceAll("\\s+", " "));
			}
			String regex = "(?is)\\bLOT (\\d*\\w{1,1})";// "(?is)\\bLOT (\\w+)";
			findRepeatedOcurrence(resultMap, tmpLegalDescription, regex);

		}
		return tmpLegalDescription;
	}

	/**
	 * Finds values like "SECTS 2 4  6" or "LOT 32 4". Any additional characters
	 * between target values should be removed. You have to pass a greedy regex:
	 * like (?is)UNIT\s+([\d+|\w{1,2}\-\d\s]+) for UNIT I-1 & G-5 in order to
	 * return I-1 G-5
	 * 
	 * @param string
	 * @param marker
	 *            is the value that indicates a succesion of values like: LTS,
	 *            SECTS, LOTS
	 * @param regEx
	 * @param groupIndex
	 *            TODO
	 * @return
	 */
	public static String getSuccessionOFValues(String string, String marker, String regEx, int groupIndex) {
		int lotIndex = string.indexOf(marker);
		String lots = "";
		if (lotIndex >= 0) {
			String lotToParse = string.substring(lotIndex);
			lotToParse = lotToParse.replace("&", " ");
			String regex = regEx;
			Pattern compile = Pattern.compile(regex);
			Matcher matcher = compile.matcher(lotToParse);
			if (matcher.find()) {
				lots = matcher.group(groupIndex).trim();
			}
		}
		return lots;
	}

	public static String getSuccessionOFValues(String tmpLegalDescription, int lotIndex, String regExLots) {
		String lotToParse = tmpLegalDescription.substring(lotIndex);
		lotToParse = lotToParse.replace("&", " ");
		String regex = regExLots;
		Pattern compile = Pattern.compile(regex);
		Matcher matcher = compile.matcher(lotToParse);
		String lots = "";
		if (matcher.find()) {
			lots = matcher.group(1).trim();
		}
		return lots;
	}

	private static void findRepeatedOcurrence(ResultMap resultMap, String tmpLegalDescription, String regex) {
		int[] matches = ro.cst.tsearch.utils.StringUtils.getMatches(tmpLegalDescription, " LOT");
		for (int i = 0; i < matches.length; i++) {
			if (matches[i] > 0) {
				String lotToParse = tmpLegalDescription.substring(matches[i]);
				Pattern compile = Pattern.compile(regex);
				Matcher matcher = compile.matcher(lotToParse);
				String lot = "";
				if (matcher.find()) {
					lot = matcher.group(1);
				}
				String savedLots = (String) resultMap.get("PropertyIdentificationSet.SubdivisionLot");
				if (savedLots != null) {
					lot = savedLots + " " + lot;
				}
				resultMap.put("PropertyIdentificationSet.SubdivisionLot", lot);
			}
		}
	}

	// public static String[] findRepeatedOcurrence(String stringToSearch,
	// String marker, String regexToParseBy, int groupToKeep) {
	// int[] matches =
	// ro.cst.tsearch.utils.StringUtils.getMatches(stringToSearch, marker);
	// String[] result = new String[matches.length];
	// for (int i = 0; i < matches.length; i++) {
	// if (matches[i] > 0) {
	// String lotToParse = stringToSearch.substring(matches[i]);
	// Pattern compile = Pattern.compile(regexToParseBy);
	// Matcher matcher = compile.matcher(lotToParse);
	// String lot = "";
	// if (matcher.find()) {
	// lot = matcher.group(groupToKeep);
	// }
	// result[i] = lot;
	// }
	// }
	// return result;
	// }

	private static String setBookPage(ResultMap resultMap, String tmpLegalDescription) {
		Pattern compile = Pattern.compile("\\w{1,4}/\\w{2,5}");
		Matcher matcher = compile.matcher(tmpLegalDescription);
		List<String> bookPageList = new ArrayList<String>();

		while (matcher.find()) {
			String value = matcher.group();
			bookPageList.add(value);

		}
		// remove what was found from the original String
		tmpLegalDescription = matcher.replaceAll("");

		buildCrossRefSet(resultMap, bookPageList);
		return tmpLegalDescription;
	}

	@SuppressWarnings("rawtypes")
	public static void buildCrossRefSet(ResultMap resultMap, List<String> bookPageList) {
		List<List> body = new ArrayList<List>();
		for (int i = 0; i < bookPageList.size(); i++) {
			ArrayList<String> list = new ArrayList<String>();
			String[] string = bookPageList.get(i).split("/");
			if (string.length == 2) {
				list.add("");
				list.add(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(string[0]));
				list.add(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(string[1]));
				body.add(list);
			}
		}

		ResultTable rt = new ResultTable();
		String[] header = { "InstrumentNumber", "Book", "Page" };
		rt = GenericFunctions2.createResultTable(body, header);
		resultMap.put("CrossRefSet", rt);
	}

	public static void parseSaleDataSet(NodeList nodeList, ResultMap map) {
		Node recentSalesDiv = HtmlParser3.getNodeByID("recentSales", nodeList, true);
		if (recentSalesDiv != null) {
			NodeList tag = HtmlParser3.getTag(recentSalesDiv.getChildren(), new TableTag(), true);
			List<HashMap<String, String>> saleDataSet = new ArrayList<HashMap<String, String>>();
			if (tag.size() > 2) {
				TableTag htmlSaleDataSet = (TableTag) tag.elementAt(2);
				TableRow[] rows = htmlSaleDataSet.getRows();

				for (TableRow tableRow : rows) {
					NodeList columns = HtmlParser3.getTag(tableRow.getChildren(), new TableColumn(), true);
					if (columns.size() == 5) {
						HashMap<String, String> hashMap = new HashMap<String, String>();
						String colContents = columns.elementAt(0).toPlainTextString();
						hashMap.put("InstrumentDate", colContents);
						colContents = columns.elementAt(1).toPlainTextString().replaceAll(",", "").replaceAll("\\$", "");
						hashMap.put("SalesPrice", colContents);
						colContents = StringUtils.strip(columns.elementAt(2).toPlainTextString().replaceAll("&nbsp;", " "));
						Pattern compile = Pattern.compile("\\d+");
						Matcher matcher = compile.matcher(colContents);
						String defaultStr = "";
						while (matcher.find()) {
							String book = StringUtils.defaultIfEmpty(matcher.group(), defaultStr);
							if (book.startsWith("0")){
								book = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(book);
							}
							hashMap.put("Book", book);
							if (matcher.find()) {
								book = StringUtils.defaultIfEmpty(matcher.group(), defaultStr);
								if (book.startsWith("0")){
									book = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(book);
								}
								hashMap.put("Page", book);
							}
						}
						colContents = columns.elementAt(4).toPlainTextString().trim();
						hashMap.put("DocumentType", colContents);
						saleDataSet.add(hashMap);
					}
				}
			}
			ResultBodyUtils.buildSaleDataSet(map, saleDataSet);
		}
	}
}

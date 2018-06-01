package ro.cst.tsearch.servers.functions;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.reports.comparators.IntegerComparator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;

import com.stewart.ats.base.document.DocumentI;

public class MOPlatteTR extends ParseClass {
	private MOPlatteTR() {
	}

	private static MOPlatteTR instance_ = null;

	public static MOPlatteTR getInstance() {
		if (instance_ == null) {
			instance_ = new MOPlatteTR();
		}
		return instance_;
	}

	@Override
	public void setTract(String text, ResultMap resultMap) {
		String tract = RegExUtils.getFirstMatch("TRACT (\\w+)", text, 1);
		resultMap.put("PropertyIdentificationSet.SubdivisionTract", tract);
	}

	@Override
	public void setBlock(String text, ResultMap resultMap) {
		String block = RegExUtils.getFirstMatch("BLK (\\w+)", text, 1);
		resultMap.put("PropertyIdentificationSet.SubdivisionBlock", block);
	}

	@Override
	public void setLot(String text, ResultMap resultMap) {
		// case 1 LOT 4 BLK 2 // SOPF 1.0409 621.05LOT 296 & W 10.01' OF LOT 297
		// case 2 LOTS 3 THRU 7 BLK 20
		// case 3 CPF 0.3172 34.14W 70 FT LOTS 7 8 9 10 11 12 BLK 7
		// case 4 LOTS 49 & 50

		List<String> lotEnumeration = RegExUtils.getMatches("LOTS?\\s(\\s?(\\d+)\\s?(-|\\+|&|AND)?)+", text, 0);

		String result = "";
		for (String string : lotEnumeration) {
			result += " " + string.replaceAll("&|\\+|-", " ").replaceAll("LOTS?", "");
		}

		List<String> lotInterval = RegExUtils.getFirstMatch(LotParser.INTERVAL_LOT_PARSE_PATTERN, text, 1, 3);

		StringBuffer lots = new StringBuffer("");
		if (lotInterval.size() == 2) {
			int prevLotNumber = 0;
			int nextLotNumber = -1;
			String max = lotInterval.get(1);
			String min = lotInterval.get(0);
			if (StringUtils.isNumeric(min) && StringUtils.isNumeric(max)) {
				prevLotNumber = new Integer(min).intValue();
				nextLotNumber = new Integer(max).intValue();
			}

			for (int k = prevLotNumber; k <= nextLotNumber; k++) {
				boolean containsK = lots.toString().matches("\\b" + k + "\\b");
				if (!containsK) {
					lots.append(MessageFormat.format("{0} ", k));
				}
			}
		}

		lots.append(" " + result);
		String[] strings = lots.toString().split("\\s+");
		Arrays.sort(strings, new IntegerComparator());
		String lotValues = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("", strings).trim();

		resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", lotValues);

	}

	@Override
	public void parseLegalDescription(String legalDescription, ResultMap resultMap) {
		String rawLegalDescription = RegExUtils.getFirstMatch("(?is)Property Description(.*?)Land", legalDescription, 1);
		resultMap.put("PropertyIdentificationSet.LegalDescriptionOnServer", rawLegalDescription);
		setLot(rawLegalDescription, resultMap);
		setTract(rawLegalDescription, resultMap);
		setBlock(rawLegalDescription, resultMap);
	}

	@Override
	public void setSecTwnRng(String text, ResultMap resultMap) {
		String match = RegExUtils.getFirstMatch("SEC,\\s*TWN,\\s*RNG:\\s*((\\d+)-(\\d+)-(\\d+))", text, 1);
		String[] split = match.split("-");
		if (split.length == 3) {
			int i = 0;
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", split[i]);
			resultMap.put("PropertyIdentificationSet.SubdivisionTownship", split[++i]);
			resultMap.put("PropertyIdentificationSet.SubdivisionRange", split[++i]);
		}
	}

	@Override
	public void parseAddress(String addressOnServer, ResultMap resultMap) {
		addressOnServer = addressOnServer.replaceAll("-\\s\\d+", "");
		String streetName = StringFormats.StreetName(addressOnServer).trim();
		String streetNo = StringFormats.StreetNo(addressOnServer);

		resultMap.put("PropertyIdentificationSet.StreetName", streetName);
		resultMap.put("PropertyIdentificationSet.StreetNo", streetNo);

	}

	@Override
	public void setTaxData(String text, ResultMap resultMap) {
		if (StringUtils.isNotEmpty(text)) {
			if (text.contains("AMOUNT PAID:")) {// normal
												// case
				List<String> firstMatch = RegExUtils.getFirstMatch("([0-9-/]+)\\s+([0-9\\.,]+)\\s+(\\d+)(?=DATE:)", text, 1, 2, 3);
				int i = 0;

				resultMap.put("TaxHistorySet.DatePaid", firstMatch.get(i++));
				resultMap.put("TaxHistorySet.AmountPaid", ro.cst.tsearch.utils.StringUtils.cleanAmount(firstMatch.get(i++)));
				resultMap.put("TaxHistorySet.ReceiptNumber", firstMatch.get(i++));
				// get base amount
				String firstMatch2 = RegExUtils.getFirstMatch("\\s*PAID\\s*([0-9\\.,]+)", text, 1);
				resultMap.put("TaxHistorySet.BaseAmount", ro.cst.tsearch.utils.StringUtils.cleanAmount(firstMatch2));

			} else {// not paid case
					// get base amount
				String match = RegExUtils.getFirstMatch("Total Due By:\\s[0-9/]+\\s+([0-9\\.,]+)", text, 1);
				resultMap.put("TaxHistorySet.BaseAmount", ro.cst.tsearch.utils.StringUtils.cleanAmount(match));

				match = RegExUtils.getFirstMatch("Pay this total prior to.*_([0-9\\,.]+)", text, 1);
				resultMap.put("TaxHistorySet.TotalDue", ro.cst.tsearch.utils.StringUtils.cleanAmount(match));

				String amountDue = getAmountDue(text);
				resultMap.put("TaxHistorySet.TotalDue", amountDue);
				
			}
			
			String delinquentYears = RegExUtils.getFirstMatch("TAX DISTRICT#:\\s+(?:\\d+)\\s+((?:\\d{4}\\s+)+)", text, 1);
			resultMap.put("tmpDelinquentYears", delinquentYears.trim());
			
		}
	}
	
	public String getAmountDue(String text) {
		String amountDue = "0.0";
		
		String delinquentDateString = RegExUtils.getFirstMatch("Pay this total prior to(.*):.*_([0-9\\,.]+)", text, 1).trim();

		DateFormat dateFormat = new SimpleDateFormat("MMMMM d, y");
		Date delinquentDate = null;
		try {
			delinquentDate = dateFormat.parse(delinquentDateString);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		Calendar currentTime = Calendar.getInstance();
		Calendar delinquentCalendar = Calendar.getInstance();
		delinquentCalendar.setTime(delinquentDate);
		if (delinquentDate != null && currentTime.after(delinquentCalendar)) {	// the
																				// amount
																				// due
																				// is
																				// with
																				// penalties
			List<String> matches = RegExUtils.getMatches(
					"\\b(January|February|March|April|May|June|July|August|Sept-Dec)\\b\\s*([0-9\\.\\,]+)", text, 0);
			// calculate amount due, depending on the current time
			int i = currentTime.get(Calendar.MONTH);

			String[] monthName = { "January", "February", "March", "April", "May", "June", 
					               "July", "August", "Sept-Dec", "Sept-Dec", "Sept-Dec", "Sept-Dec" };
			Calendar cal = Calendar.getInstance();
			String month = monthName[cal.get(Calendar.MONTH)];
			for (String string : matches) {
				if (!string.endsWith(",") && string.contains(month)) {
					String match = RegExUtils.getFirstMatch("([0-9\\,.]+)", string, 0);
					amountDue = ro.cst.tsearch.utils.StringUtils.cleanAmount(match);
					break;
				}
			}
		}
		
		return amountDue;
	}

	@Override
	public void setAppraisalData(String text, ResultMap resultMap) {
		String match = RegExUtils.getFirstMatch("([0-9\\.,]+)TOTAL VALUATION", text, 1);
		resultMap.put("PropertyAppraisalSet.TotalAssessment", match);
	}

	@Override
	public void parseName(String name, ResultMap resultMap) {
		// try to get the name from pdf response which is supposed to be more
		// complete
		String pdfResponse = (String) resultMap.get("tmpPdf");
		name = name.replace("&amp;", "&");
		String pdfName = name;
		if (StringUtils.isNotEmpty(pdfResponse)) {
			pdfName = RegExUtils.getFirstMatch("(?is)(" + pdfName + ".*?)(?=PHONE\\:)", pdfResponse, 1);
			if (pdfName.contains(name)) {
				name = pdfName.trim();
			}
		}

		name = name.replace("%", "");
		// name = name.replaceAll("\\bTRUST\\b", "");
		name = name.replace("FAMILY TRUS", "TRUST");

		// case 1 : SMITH, ALLYN TRUST & SMITH, PAMELA
		// case 2 : SMITH, ANDREW K
		// case 3 : SMITH, ANDREW Q & KIM K

		String[] splitByAnd = name.split("&");
		List body = new ArrayList<List>();
		if (splitByAnd.length == 2 && !NameUtils.isCompany(name)) {
			if (splitByAnd[1].contains(",")) {
				int i = 0;
				
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, splitByAnd[i], body);
				i++;
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, splitByAnd[i], body);
			} else {
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, name, null);
			}
		} else {
			if (NameUtils.isCompany(name) && splitByAnd.length == 1) {
				String[] ret = { "", "", name, "", "", "" };
				String[] suffixes = GenericFunctions.extractNameSuffixes(ret);
				ParseNameUtil.putNamesInResultMap(resultMap, null, ret, suffixes);
				resultMap.put("PropertyIdentificationSet.OwnerLastName", "");
			} else {
				if (splitByAnd.length == 2) {
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, splitByAnd[0], body);
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, splitByAnd[1], body);
				} else {
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, name, null);
				}

			}

		}
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat link) {
		// get the result body
		String resultBody = RegExUtils.getFirstMatch("(?is)<div id=\"main\".*?</div>", response, 0);
		// remove the unnecessary descriptor table
		String matchString = "RECEIPTS/STATEMENTS USE ADOBE ACROBAT FOR A VIEWER, PLEASE DOWNLOAD THE FREE VERSION IF YOU NEED IT.";
		resultBody = resultBody.replaceAll("(?is)<table cellspacing=\"3\" cellpadding=\"2\" width = \"100%\">.*?" + matchString
				+ "*?</table>", "");
		resultBody = resultBody.replaceAll("</?form>", "");
		// change the succession of tables to a succession of table rows
		List<String> list = RegExUtils.getMatches("(?is)<table cellspacing=\"3\" cellpadding=\"2\" width = \"100%\">(.*?)</table>",
				resultBody, 1);

		// create links from Print button
		String newTable = "<table>" + StringUtils.join(list, "") + "</table>";
		List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap(newTable);

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		String tableHeader = list.get(0);
		for (int i = 0; i < tableAsListMap.size(); i++) {
			ParsedResponse currentResponse = new ParsedResponse();
			String currentHtmlRow = list.get(i + 1);

			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, currentHtmlRow);
			currentResponse.setOnlyResponse(currentHtmlRow);
			ResultMap resultMap = new ResultMap();

			Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
			DocumentI document = null;
			try {
				document = bridge.importData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			currentResponse.setDocument(document);
			intermediaryResponse.add(currentResponse);

		}
		serverResponse.getParsedResponse().setHeader("<table>" + tableHeader);
		serverResponse.getParsedResponse().setFooter("</table>");
		return intermediaryResponse;

	}

	static class LotParser {
		public static final int SIMPLE_LOT_PARSE = 4;
		public static final int INTERVAL_LOT_PARSE = 2;
		public static final int ENUMERATION_LOT_PARSE = 1;

		public static final String SIMPLE_LOT_PARSE_PATTERN = "LO?T\\s(\\w+)";
		public static final String ENUMERATION_LOT_PARSE_PATTERN = "LOTS?\\s(\\s?(\\d+)\\s?(-|\\+|&|AND|THRU)?)+";
		public static final String INTERVAL_LOT_PARSE_PATTERN = "LOTS?\\s+(\\d+)\\s+(THRU)\\s+(\\d+)";

		public static List<String> parse(String text, int typeOfParse, boolean repetitionParse) {
			switch (typeOfParse) {
			case SIMPLE_LOT_PARSE:
				break;

			case ENUMERATION_LOT_PARSE:
				break;

			case INTERVAL_LOT_PARSE:
				break;

			case SIMPLE_LOT_PARSE + INTERVAL_LOT_PARSE:
				break;

			case SIMPLE_LOT_PARSE + ENUMERATION_LOT_PARSE:
				break;

			case INTERVAL_LOT_PARSE + ENUMERATION_LOT_PARSE:
				break;
			}

			return null;
		}
	}

}

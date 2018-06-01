package ro.cst.tsearch.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * @see http://www.rgagnon.com/javadetails/java-0426.html
 */
public class NumberUtils {

	private static final String[] tensNames = { "", " ten", " twenty", " thirty", " forty", " fifty", " sixty", " seventy", " eighty",
			" ninety" };

	private static final String[] numNames = { "", " one", " two", " three", " four", " five", " six", " seven", " eight", " nine", " ten",
			" eleven", " twelve", " thirteen", " fourteen", " fifteen", " sixteen", " seventeen", " eighteen", " nineteen" };

	private static String convertLessThanOneThousand(int number) {
		String soFar;

		if (number % 100 < 20) {
			soFar = numNames[number % 100];
			number /= 100;
		} else {
			soFar = numNames[number % 10];
			number /= 10;

			soFar = tensNames[number % 10] + soFar;
			number /= 10;
		}
		if (number == 0)
			return soFar;
		return numNames[number] + " hundred" + soFar;
	}

	public static String convertNumberToEnglishWordsList(String numberEnumeration, String regExSeparator) {
		StringBuilder returnString = new StringBuilder();
		numberEnumeration = numberEnumeration.replaceAll(",|\\s+|\\.", " ");
		numberEnumeration = numberEnumeration.replaceAll("\\s+", " ");
		String[] split = numberEnumeration.split(regExSeparator);
		for (String string : split) {
			String lettersRegEx = "([A-Za-z-]+)";
			if (RegExUtils.matches(lettersRegEx, string)) {
				ArrayList<String> splits = StringUtils.splitIncludeDelimiter(string, lettersRegEx, false);
				addNumberLettersToString(returnString, splits.toArray(new String[] {}), false);
			} else {
				addNumberLettersToString(returnString, new String[] { string }, false);
			}
		}
		String string = returnString.toString();
		int lastIndexOf = string.lastIndexOf(",");
		string = string.substring(0, lastIndexOf);
		if (string.startsWith(",")) {
			string = string.substring(1);
		}
		return string;
	}

	private static void addNumberLettersToString(StringBuilder returnString, String[] strings, boolean addCommaAfterNumeber) {
		StringBuilder wordConstruct = new StringBuilder("");
		StringBuilder originalNumber = new StringBuilder("");
		for (String string : strings) {
			if (string.startsWith("-")) { // if it is a interval
				string = string.replaceFirst("-", "");
				wordConstruct.append(" to ");
			}
			if (org.apache.commons.lang.math.NumberUtils.isDigits(string)) {
				String englishWords = convertNumberToEnglishWords(Long.valueOf(string).longValue());
				wordConstruct.append(englishWords);
				// returnString.append(englishWords + " " + string);
				if (addCommaAfterNumeber) {
					returnString.append(",");
				}
			} else {
				wordConstruct.append(" " + string);
			}
		}
		String join = org.apache.commons.lang.StringUtils.join(strings);
		if (StringUtils.isNotEmpty(join)){
			originalNumber.append("\"" + join +"\"");
		}
		returnString.append(wordConstruct + /*" " + originalNumber + */",");
	}

	public static String convertNumberToEnglishWords(long number) {
		// 0 to 999 999 999 999
		if (number == 0) {
			return "zero";
		}

		String snumber = Long.toString(number);
		// pad with "0"
		String mask = "000000000000";
		DecimalFormat df = new DecimalFormat(mask);
		snumber = df.format(number);

		// XXXnnnnnnnnn
		int billions = Integer.parseInt(snumber.substring(0, 3));
		// nnnXXXnnnnnn
		int millions = Integer.parseInt(snumber.substring(3, 6));
		// nnnnnnXXXnnn
		int hundredThousands = Integer.parseInt(snumber.substring(6, 9));
		// nnnnnnnnnXXX
		int thousands = Integer.parseInt(snumber.substring(9, 12));

		String tradBillions;
		switch (billions) {
		case 0:
			tradBillions = "";
			break;
		case 1:
			tradBillions = convertLessThanOneThousand(billions) + " billion ";
			break;
		default:
			tradBillions = convertLessThanOneThousand(billions) + " billion ";
		}
		String result = tradBillions;

		String tradMillions;
		switch (millions) {
		case 0:
			tradMillions = "";
			break;
		case 1:
			tradMillions = convertLessThanOneThousand(millions) + " million ";
			break;
		default:
			tradMillions = convertLessThanOneThousand(millions) + " million ";
		}
		result = result + tradMillions;

		String tradHundredThousands;
		switch (hundredThousands) {
		case 0:
			tradHundredThousands = "";
			break;
		case 1:
			tradHundredThousands = "one thousand ";
			break;
		default:
			tradHundredThousands = convertLessThanOneThousand(hundredThousands) + " thousand ";
		}
		result = result + tradHundredThousands;

		String tradThousand;
		tradThousand = convertLessThanOneThousand(thousands);
		result = result + tradThousand;

		// remove extra spaces!
		return result.replaceAll("^\\s+", "").replaceAll("\\b\\s{2,}\\b", " ");
	}

}

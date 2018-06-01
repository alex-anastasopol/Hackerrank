package ro.cst.tsearch.search.name;

import java.util.regex.Pattern;

/**
 * RomanNumber
 * 
 * Utility class for handling roman numbers.
 * 
 * @author catalinc
 */
public class RomanNumber {

	/**
	 * Roman number symbol table.
	 */
	static char romanSymbols[] = { 'I', 'V', 'X', 'L', 'C', 'D', 'M' };
	/**
	 * Roman number symbol values.
	 */
	static int romanSymbolValues[] = { 1, 5, 10, 50, 100, 500, 1000 };

	/**
	 * Parses roman number into a decimal value.
	 * 
	 * @return decimal value for roman number.
	 * 
	 * @throws NumberFormatException if input string does not represents a roman number.
	 */
	public static int parse(String romanNumber) {		
		if(romanNumber == null) 
			throw new IllegalArgumentException("input string cannot be null");
		
		romanNumber = romanNumber.trim();
		int result = 0;
		int lastValue = 0;
		int lastSymbolNr = 0;
		int i;
		for (i = 0; i < romanNumber.length(); i++) {
			char ch = Character.toUpperCase(romanNumber.charAt(i));
			int j;
			for (j = 0; j < romanSymbols.length && ch != romanSymbols[j]; j++);
			if (j == romanSymbols.length)
				throw new NumberFormatException(
					"Not an admittable symbol:" + ch);
			if (lastSymbolNr < j)
				result -= lastValue * 2;
			result += romanSymbolValues[j];
			lastValue =
				(lastSymbolNr == j ? lastValue : 0) + romanSymbolValues[j];
			lastSymbolNr = j;
		}

		if (i == 0)
			throw new NumberFormatException("Empty String:" + romanNumber);
		else
			return result;
	}
	
	/**
	 * Test if input string denotes a roman number.
	 * 
	 * @param s Input string.
	 * 
	 * @return boolean
	 */
	public static boolean isRomanNumber(String s) {
		try {			
			parse(s);
			return true;			
		} catch(NumberFormatException nfe) {
			return false;
		} catch(IllegalArgumentException ile) {
			return false;
		}
	}
	
	public static String convertToRomanNumber(String s) {
		try {			
			return String.valueOf(parse(s));			
		} catch(NumberFormatException nfe) {
			return s;
		} catch(IllegalArgumentException ile) {
			return s;
		}
	}
	
	/**
	 * Test for roman number except M,D,C,L
	 */
	public static boolean isATSRomanNumber(String s) {
		if(Pattern.compile("(?i)[MDCL]").matcher(s).find()){
			return false;
		}
		return isRomanNumber(s);
	}
}

package ro.cst.tsearch.search.address2;
//package newest; //debug

import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Support for various ranges (1-3)/list(1,2,3) matches
 * - number in range
 * - number in list
 * - list in list
 * - range in range
 * - list in range
 * 
 * All the numbers are considered composite numbers.
 *
 * TODO: write tests for the "false" conditions, such as "is NOT number range"
 */
public class Ranges {
	
	public static final Pattern RANGE_PATTERN = Pattern.compile("(^" + Normalize.COMPOSITE_NUMBER + "-" + Normalize.COMPOSITE_NUMBER + "$)");
	public static final Pattern LETTER_RANGE = Pattern.compile("(^([A-Z]+)([-])([A-Z]+)$)");
	public static final Pattern LIST_PATTERN = Pattern.compile("(^" + Normalize.COMPOSITE_NUMBER + "(," + Normalize.COMPOSITE_NUMBER + ")+" + "$)");
	
	/**
	 * Check if the given string is a range of composite numbers
	 * @return true if the given string is a range of the form number-number
	 */
	public static final boolean isNumberRange(String str) {
		return RANGE_PATTERN.matcher(str).matches();
	}
	
	/**
	 * Check if the given string is a list of composite numbers
	 * @return true if the given string is a range of the form number,number(,number...)
	 */
	public static final boolean isNumberList(String str) {
		return LIST_PATTERN.matcher(str).matches();
	}
	
	/**
	 * Internal representation of a range of the form "COMPOSITE_NUMBER-COMPOSITE_NUMBER".
	 * Stores the endpoints as separate integer part and letter part.
	 * If one or more endpoints have letters, then hasLetter will be true. If just one of the
	 * endpoints has letter, the other one will be set to the same value.
	 */
	private static class Range {
		public int start = -1;
		public int end = -1;
		public String lstart = null;
		public String lend = null;
		public boolean hasLetter = false;
		
		/**
		 * Try to create a range from the given candidate string.
		 *
		 * @throw IllegalArgumentException if we can't construct a range
		 */
		public Range(String candidate) {
			Matcher candidateRangeTokens = RANGE_PATTERN.matcher(candidate);
			if (candidateRangeTokens.matches()) {
				try {
					start = Integer.parseInt(candidateRangeTokens.group(2));
					end   = Integer.parseInt(candidateRangeTokens.group(4));
					
					lstart = candidateRangeTokens.group(3); 
					boolean startExist = !(lstart == null || "".equals(lstart));
					lend   = candidateRangeTokens.group(5);
					boolean endExist = !(lend == null || "".equals(lend));
					
					if (startExist || endExist) {
						hasLetter = true;
						
						lstart = startExist ? lstart : lend;
						lend   = endExist   ? lend   : lstart;
					}
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid range format " + e);
				}
			} else {
				throw new IllegalArgumentException("Invalid range format, can't split");
			}
		}
	}
	
	/**
	 * Internal representation of a list of the form "COMPOSITE_NUMBER,(COMPOSITE_NUMBER)+".
	 * Stores the elements as strings in a list, not splitting up the composite numbers
	 */
	private static class Seq {
		public HashSet<String> elements = null;
		
		/**
		 * Try to create a list from the candidate string.
		 *
		 * @throws IllegalArgumentException if we can't construct a list
		 */
		public Seq(String list) {
			String parts[] = list.split(",");
			elements = new HashSet<String>();
			for (int i = 0; i < parts.length; i++) {
				if (Normalize.isCompositeNumber(parts[i])) {
					elements.add(parts[i]);
				} else {
					throw new IllegalArgumentException("Parameter not a valid list, found: " + parts[i]);
				}
			}
		}
	}
	
	/**
	 * @param range the range to check, in the form of "composite number-composite number"
	 * @param number the number to check in the range
	 */
	private static final boolean isNumberInRange(Range procRange, String number) {
		Matcher numberTokens = Pattern.compile("(^" + Normalize.COMPOSITE_NUMBER + "$)").matcher(number);
		if (numberTokens.matches()) {
			int cand  = Integer.parseInt(numberTokens.group(2));
			
			if (procRange.start == cand || (procRange.start < cand && cand <= procRange.end)) {
				String lcand = numberTokens.group(3);
				if (lcand == null || "".equals(lcand) || !procRange.hasLetter) {
					return true;
				} else if (lcand.compareTo(procRange.lstart) >= 0 && lcand.compareTo(procRange.lend) <= 0) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}
	
	/**
	 * @param range the range to check, in the form of "composite number-composite number"
	 * @param number the number to check in the range
	 */
	public static final boolean isNumberInRange(String range, String number) {
		try {
			Range procRange = new Range(range);
			return isNumberInRange(procRange, number);
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * @param list the list to check, in the form of "composite number,composite number[,composite number...]
	 * @param number the number to check in the list
	 */
	public static final boolean isNumberInList(String list, String number) {
		try {
			Matcher numberTokens = Pattern.compile("(^" + Normalize.COMPOSITE_NUMBER + "$)").matcher(number);
			Seq procList = new Seq(list);
			
			if (numberTokens.matches() && procList.elements.contains(number)) {
				return true;
			} else {
				return false;
			}
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	/**
	 * @param candidate the list to test against
	 * @param reference the list which needs to be in the candidate for the test to pass
	 */
	public static final boolean isListInList(String candidate, String reference) {
		try {
			Seq cand = new Seq(candidate);
			Seq ref  = new Seq(reference);
			
			Iterator i = ref.elements.iterator();
			while (i.hasNext()) {
				if (!cand.elements.contains(i.next())) {
					return false;
				}
			}
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	/**
	 * @param candidate the range to test against
	 * @param reference the list which needs to be in the candidate for the test to pass
	 */
	public static final boolean isListInRange(String candidate, String reference) {
		try {
			Seq ref = new Seq(reference);
			Range cand = new Range(candidate);
			
			Iterator i = ref.elements.iterator();
			while (i.hasNext()) {
				if (!isNumberInRange(cand, (String)i.next())) {
					return false;
				}
			}
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	/**
	 * @param candidate the range to test against
	 * @param reference the range which needs to be in the candidate for the test to pass
	 */
	public static final boolean isRangeInRange(String candidate, String reference) {
		try {
			
			if(candidate.equalsIgnoreCase(reference)) {
				return true;
			}
			Range cand = new Range(candidate);
			Range ref = new Range(reference);
			
			if ( (cand.start== ref.start && ref.end == cand.end) 
					|| (cand.start<= ref.start && ref.end <= cand.end && cand.start<= cand.end && ref.start <= ref.end)) {
				if (cand.hasLetter && ref.hasLetter) {
					if (ref.lstart.compareTo(cand.lstart) >= 0 && cand.lend.compareTo(ref.lend) >= 0 &&
							ref.lstart.compareTo(ref.lend) <= 0 && cand.lstart.compareTo(cand.lend) <= 0) {
						return true;
					}
				}
				return true;
			} 
			return false;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	/**
	 * @return true if the given string is a range of the form letter-letter
	 */
	public static final boolean isLetterRange(String str) {
		return LETTER_RANGE.matcher(str).matches();
	}
	
	/**
	 * @param range The range to check, of the form "letter-letter"
	 * @param letter The letter to check in the range
	 *
	 * @return true if the check passes
	 */
	public static final boolean isLetterInRange(String range, String letter) {
		Matcher rangeTokens = LETTER_RANGE.matcher(range);
		if (rangeTokens.matches()) {
			if (letter.compareTo(rangeTokens.group(2)) >= 0 && letter.compareTo(rangeTokens.group(4)) <= 0) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Very simple system for running a couple of tests on the functions described in this class
	 */
	public static void main(String [] argv) {
		int passed = 0;
		int failed = 0;
		
		// range tests
		String tests6[] = {"1-10", "1a-10z", "1a-10", "1-10z"};
		for (int i = 0; i < tests6.length; i++) {
			if (!isNumberRange(tests6[i])) {
				System.out.println("Failed: " + tests6[i] + " is not a range");
				failed++;
			} else {
				passed++;
			}
		}
		
		// list tests
		String tests7[] = {"1,2", "1a,2b", "1a,2", "1,2a", "1,2,3", "1a,2b,3", "1a,2,3", "1,2a,3", "1,2,3", "1a,2b,3c", "1a,2,3c", "1,2a,3c"};
		for (int i = 0; i < tests7.length; i++) {
			if (!isNumberList(tests7[i])) {
				System.out.println("Failed: " + tests7[i] + " is not a list");
				failed++;
			} else {
				passed++;
			}
		}
		
		// number in range
		String tests[][] = { {"1-10", "5"}, {"1-10", "1"}, {"1-10", "5"},
				{"1a-10z", "5k"}, {"1a-10z", "1a"}, {"1a-10z", "10z"},
				{"1a-10", "5a"}, {"1a-10", "8a"}, {"1-10a", "10a"}};
		for (int i = 0; i < tests.length; i++) {
			if (!isNumberInRange(tests[i][0], tests[i][1])) {
				System.out.println("Failed: " + tests[i][1] + " in " + tests[i][0]);
				failed++;
			} else {
				passed++;
			}
		}
		
		// number in list 
		String tests1[][] = { {"1,10", "1"}, {"1,10", "10"},
				{"1,2,3", "1"}, {"1,2,3", "2"}, {"1,2,3", "3"},
				{"1,2,3,4", "1"}, {"1,2,3,4", "2"}, {"1,2,3,4", "3"}, {"1,2,3,4", "4"},
				{"1,1a,1b,1c,1d", "1"}, {"1,1a,1b,1c,1d", "1a"}, {"1,1a,1b,1c,1d", "1b"}, {"1,1a,1b,1c,1d", "1c"}, {"1,1a,1b,1c,1d", "1d"},
				{"1a,2a,3a,4a", "1a"}, {"1a,2a,3a,4a", "2a"}, {"1a,2a,3a,4a", "3a"}, {"1a,2a,3a,4a", "4a"},
				{"1a,2a,3a", "1a"}, {"1a,2a,3a", "2a"}, {"1a,2a,3a", "3a"},
				{"1a,2,3", "1a"}, {"1,2a,3", "2a"}, {"1,2,3a", "3a"},
				{"1,2a,3a", "1"}, {"1a,2,3a", "2"}, {"1a,2a,3", "3"}};
		for (int i = 0; i < tests1.length; i++) {
			if (!isNumberInList(tests1[i][0], tests1[i][1])) {
				System.out.println("Failed: " + tests1[i][1] + " in " + tests1[i][0]);
				failed++;
			} else {
				passed++;
			}
		}
		
		// list in list
		String tests3[][] = { {"1,10", "1,10"}, {"1,10", "10,1"},
				{"1,2,3", "1,2"}, {"1,2,3", "1,3"}, {"1,2,3", "2,3"}, {"1,2,3", "3,2"}};
		for (int i = 0; i < tests3.length; i++) {
			if (!isListInList(tests3[i][0], tests3[i][1])) {
				System.out.println("Failed: " + tests3[i][1] + " in " + tests3[i][0]);
				failed++;
			} else {
				passed++;
			}
		}
		
		// list in range
		String tests4[][] = { {"1-10", "1,10"}, {"1-10", "10,1"},
				{"1-3", "1,2"}, {"1-3", "1,3"}, {"1-3", "2,3"}, {"1-3", "3,2"}};
		for (int i = 0; i < tests4.length; i++) {
			if (!isListInRange(tests4[i][0], tests4[i][1])) {
				System.out.println("Failed: " + tests4[i][1] + " in " + tests4[i][0]);
				failed++;
			} else {
				passed++;
			}
		}
		
		// range in range
		String tests2[][] = { {"1-10", "5-8"}, {"1-10", "1-10"}, {"1-10", "1-5"}, {"1-10", "5-10"},
				{"1a-10z", "5k-8t"}, {"1a-10z", "1a-10z"}, {"1a-10z", "1a-5k"}, {"1a-10z", "5k-10z"},
				{"1a-10", "5a-8"}, {"1a-10", "5-8a"}, {"1-10a", "5a-8"}, {"1-10a", "5-8a"},
				{"1a-10z", "5k-8"}, {"1a-10z", "5-8k"},
				{"1a-10a", "5a-8"}, {"1a-10a", "5-8a"} };
		for (int i = 0; i < tests2.length; i++) {
			if (!isRangeInRange(tests2[i][0], tests2[i][1])) {
				System.out.println("Failed: " + tests2[i][1] + " in " + tests2[i][0]);
				failed++;
			} else {
				passed++;
			}
		}
		
		// letter in letter range
		String tests5[][] = { {"A-Z", "M"}, {"A-A", "A"}, {"A-Z", "A"}, {"A-Z", "Z"} };
		for (int i = 0; i < tests5.length; i++) {
			if (!isLetterInRange(tests5[i][0], tests5[i][1])) {
				System.out.println("Failed: " + tests5[i][1] + " in " + tests5[i][0]);
				failed++;
			} else {
				passed++;
			}
		}
		System.out.println(passed + " tests passed, " + failed + " tests failed.");
	}
}

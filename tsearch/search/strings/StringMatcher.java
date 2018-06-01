package ro.cst.tsearch.search.strings;

import java.util.ArrayList;
import org.apache.log4j.Category;

/**
 * StringMatcher
 *
 */
public class StringMatcher {

	protected static final Category logger= Category.getInstance(StringMatcher.class.getName());

	/** @return an array of adjacent letter pairs contained in the input string */
	private static String[] letterPairs(String str) {
		int numPairs = str.length() - 1;
		String[] pairs = new String[numPairs];
		for (int i = 0; i < numPairs; i++) {
			pairs[i] = str.substring(i, i + 2);
		}
		return pairs;
	}

	/** @return an ArrayList of 2-character Strings. */
	private static ArrayList wordLetterPairs(String str) {
		ArrayList allPairs = new ArrayList();
		// Tokenize the string and put the tokens/words into an array 
		String[] words = str.split("\\s+");
		// For each word
		for (int w = 0; w < words.length; w++) {
			// Find the pairs of characters
			String[] pairsInWord = letterPairs(words[w]);
			for (int p = 0; p < pairsInWord.length; p++) {
				allPairs.add(pairsInWord[p]);
			}
		}
		return allPairs;
	}

	/** @return lexical similarity value in the range [0,1] */
	public static double score(String str1, String str2) {
		ArrayList pairs1 = wordLetterPairs(str1.toUpperCase());
		ArrayList pairs2 = wordLetterPairs(str2.toUpperCase());
		int intersection = 0;
		int union = pairs1.size() + pairs2.size();
		for (int i = 0; i < pairs1.size(); i++) {
			Object pair1 = pairs1.get(i);
			for (int j = 0; j < pairs2.size(); j++) {
				Object pair2 = pairs2.get(j);
				if (pair1.equals(pair2)) {
					intersection++;
					pairs2.remove(j);
					break;
				}
			}
		}
		return (2.0 * intersection) / union;
	}
	
	public static void testCase(String ref, String cand) {
		logger.info("Comparing [" + ref + "] vs [" + cand + "] score=" 
			+ score(ref,cand));
	}
	
	public static void main(String[] args) {
		testCase("ggggggg","gggggg        jfajsk            dfaf afa");
	}
}

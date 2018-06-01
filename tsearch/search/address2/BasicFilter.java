package ro.cst.tsearch.search.address2;

/**
 * Basic matcher for strings. Can score two strings, or two sets of strings.
 * For the latter, the function looks for the best possible match (and thus might not take tokens in order)
 */
public class BasicFilter {

	/**
	 * Prevent instantiation
	 */
	private BasicFilter() {}

	/**
	 * Fnd the SIZE of the longest common subsequence. 
	 * If the actual sequence is needed, return String and uncomment the last part of code
	 *
	 * This function is currently used in all string scoring methods
	 */
	protected static int subseq(String a, String b) {
		// check if one of the strings are empty
		if((a.length() == 0) || (b.length() == 0))
			return 0;
		// create the table for the longest common subsequence
		int [][] L = new int[a.length()+1][b.length()+1];
		for (int i = 0; i <a.length(); i++) L[i][b.length()] = 0;
		for (int i = 0; i <b.length(); i++) L[a.length()][i] = 0;

		for (int i = a.length() - 1; i >= 0; i--)
			for (int j = b.length() - 1; j >= 0; j--) {
				if (a.charAt(i)== b.charAt(j)) L[i][j] = 1 + L[i+1][j+1];
				else L[i][j] = L[i+1][j]> L[i][j+1]? L[i+1][j]:L[i][j+1];
			}
		// l[0][0] now has the length of the longest common subsequence
		return L[0][0];
		/* uncomment this for the actual sequence 
			String seq = new String();
			int i = 0;
			int j = 0;
			while (i < a.length() && j < b.length()) {
				if (a.charAt(i)==b.charAt(j)) {
		    			seq = seq + a.charAt(i);
		    			i++; j++;
				} else if (L[i+1][j] >= L[i][j+1]) i++;
				else j++;
    			}
			return seq;
		*/
	}

	/**
	 * Fitness function that determines the relation between the two tokens.
	 * Uses the subsequence function but also experiments with giving more to the strings 
	 * that have common beginnings.
	 */
	public static double score(String token1, String token2) {
		// if one of the tokens is empty then they match
		if((token1.length()==0)||(token2.length()==0))
				return 1.0;
		// currently use the formula W1 * common / token1 + W2 * common / token2;
		double w1 = 0.5, w2 = 0.5;
		double common = subseq(token1, token2);
		int maxlen = (token1.length() > token2.length()) ? token1.length() : token2.length();
		int minlen = (token1.length() > token2.length()) ? token2.length() : token1.length();
		// calculate the number of equal consecutive characters
		int consec;
		for (consec = 0; consec < minlen; consec++) {
			if (token1.charAt(consec) != token2.charAt(consec)) break;
		}
		double inter = w1*common/(double)token1.length() + w2*common/(double)token2.length();
		return inter + consec / maxlen * (1 - inter); // if this is too "eager", tune it down with a number < 1
	}

	/**
	 * Suggest a treshold for automatic rejection/passing. Currently not very useful
	 */
	public static double suggestTreshold(int size) {
		if (size < 5) return 0.8;
		return (size - 4) * 0.03 + 0.8;
	}

	/**
	 * Overloaded score that looks over the array of tokens and returns the best score it can find
	 */
	public static double score(String [] name1, String [] name2) {
		// go through all the tokens in the first name, and try to find correspondent
		// first "name" is always considered the reference, the second one the candidate
		// the tokens are considered in the order of decreasing importance, left to right (increasing #)
		double max = 0;
		for (int i = 0; i < name1.length; i++) {
			double localMax = -1, cand;
			int maxPos = 0;
			for (int j = 0; j < name2.length; j++)
				if ((cand = score(name1[i], name2[j])) > localMax) { localMax = cand; maxPos = j; }
			// we give a weight to these, based on whether or not they matched in place (penalty)
			if (i != maxPos) localMax *= 3/4;
			// now adjust the score a little based on the position in the string of tokens
			// follow squares
			// * max += localMax * (name1.length - i) * (name1.length - i);
			// above function turned out to be too sensitive to ordering of tokens
			max += localMax;
		}
		// normalize the score
		max /= name1.length;
		// * max /= name1.length * (name1.length + 1) * (2 * name1.length + 1) / 6;
		// penalize difference between the number of tokens in the two names
		int absDiff = name1.length - name2.length; absDiff = absDiff > 0? absDiff:-absDiff;
		return max / (double)name1.length - 0.1*(double)absDiff;
	}

	public static void main(String args[]) {
		String tests[][] = {
			{"BLUE RIDGE HEIGHTS", "BLUE RIDGE"}, 
			{"BLUE RIDGE HEIGHTS", "BLUE RIDGE MOUNTAIN"}, 
			{"BLUE RIDGE HEIGHTS", "BLUE RIDGE HEIGHTS"}};
		for (int i = 0; i < tests.length; i++) {
			System.out.println(tests[i][0] + " compare to " + tests[i][1] + " = " + BasicFilter.score(tests[i][0], tests[i][1]));
		}
	}
}

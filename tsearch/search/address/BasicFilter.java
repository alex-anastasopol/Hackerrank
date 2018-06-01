package ro.cst.tsearch.search.address;

import java.io.Serializable;
import org.apache.log4j.Category;

/**
 * Basic string matcher.
 */
public class BasicFilter implements Serializable {
	
	protected static final Category logger= Category.getInstance(BasicFilter.class.getName());
    static final long serialVersionUID = 10000000;

	/**
	 * Default constructor.
	 *
	 */ 
	public BasicFilter() {}

	/**
	 *  Find the LENGTH of the longest common subsequence.
	 */
	public int subseq(String a, String b) {
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
	}

	/**
	 * Fitness function that determines the relation between the two tokens
	 * Uses the above function but also experiments with giving more to the strings that have common beginnings.
	 */
	public double score(String token1, String token2) {
		// currently use the formula W1 * common / token1 + W2 * common / token2;
		double w1 = 0.5, w2 = 0.5;
		double common = subseq(token1, token2);
		// calculate the number of equal consecutive characters
		int consec;
		for (consec = 0; consec < (token1.length() > token2.length()?token2.length():token1.length()); consec++)
			if (token1.charAt(consec) != token2.charAt(consec)) break;
		double inter = w1*common/(double)token1.length() + w2*common/(double)token2.length();
		return inter; //+ consec * 0.2 * (1 - inter); // increased the weight to 0.2 to give more to stuff matching from beginning
	}

	public double suggestTreshold(int size) {
		if (size < 5) return 0.8;
		return (size - 4) * 0.03 + 0.8;
	}

	/**
	 * Overloaded score that looks over the array of tokens and returns the best score it could squeeze
	 * Search name is considered to be the first one
	 */
	public double score(String [] name1, String [] name2) {
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
	
	 public static void testCase(String s1, String s2) {
	 	BasicFilter bf = new BasicFilter();
	 	logger.info("Matching [" + s1 + "] vs [" + s2 + "] score =[" + bf.score(s1,s2) + "]");
	 }
	
	public static void main(String[] args) {
		testCase("HILLSBORO 24Z", "HILLSBORO 12A");
	}
}

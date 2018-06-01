package ro.cst.tsearch.search.name;

/**
 * SubSeqMatcher
 *
 */
public class SubSeqMatcher {
	/**
	 *  Find the lenght of the longest common subsequence.
	 */
	public int subseq(String a, String b) {
		int [][] L = new int[a.length()+1][b.length()+1];
		for (int i = 0; i <a.length(); i++) L[i][b.length()] = 0;
		for (int i = 0; i <b.length(); i++) L[a.length()][i] = 0;

		for (int i = a.length() - 1; i >= 0; i--)
			for (int j = b.length() - 1; j >= 0; j--) {
				if (a.charAt(i)== b.charAt(j)) L[i][j] = 1 + L[i+1][j+1];
				else L[i][j] = L[i+1][j]> L[i][j+1]? L[i+1][j]:L[i][j+1];
			}
		return L[0][0];
	}
	
	/**
	 * Fitness function that determines the relation between the two tokens
	 * Uses the above function but also experiments with giving more to the strings that have common beginnings.
	 */
	public double score(String token1, String token2) {
		if(token1.equals("") && token2.equals(""))
			return 1.0;
		double w1 = 0.5, w2 = 0.5;
		double common = subseq(token1, token2);
		if(common == 0.0)
			return 0.0;
		double inter = w1*common/(double)token1.length() + w2*common/(double)token2.length();
		return inter;
	}	

}

/*
 * Created on Sep 4, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter.matchers.algorithm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class MatchToken {

	protected static final Category logger = Category.getInstance(MatchToken.class.getName());
	
	
	private String ref ;
	private int pos = -1;
	private boolean penilizeDifferentPositions = false;
	
	private List cands = new ArrayList();
	private List scores = new ArrayList();
	private List commonSeqs = new ArrayList();
	
	private BigDecimal bestScore = ATSDecimalNumberFormat.ZERO;
	private int bestScoreIdx = -1;
	
	public MatchToken(String ref){
		this.ref = ref;
	}

	public MatchToken(String ref, int pos, boolean panilizeDifferentPositions){
		//logger.debug("new MatchToken("+ ref + ")" );
		this.ref = ref;
		this.pos = pos;
		this.penilizeDifferentPositions = panilizeDifferentPositions;
	}
	

	protected void match(List candTokens) {
		for (int candIdx = 0; candIdx < candTokens.size(); candIdx++) {
			String candToken = (String) candTokens.get(candIdx);
			match(candToken, candIdx);
		}
	}
	

	public void match(String cand, int candPos) {

		String seq = commonSubseq(ref, cand) ;
		//logger.debug("matchWithCandidate("+ ref + ", "+cand+", common = " + seq + ")" );
		BigDecimal candScore = getScoreOneToken(ref, cand, seq, pos, candPos, penilizeDifferentPositions);
		//logger.debug("common("+ ref + "," + cand +") is [" + seq +"]; score:" + candScore);

		cands.add(cand);
		scores.add(candScore);
		commonSeqs.add(seq);
		
		if (candScore.compareTo(bestScore)>=0){
			bestScore = candScore;
			bestScoreIdx = scores.size()-1;
		}
	}

	public BigDecimal getBestScore(){
		return getBestScore(new ArrayList());
	}

	public BigDecimal getBestScore(List alreadyMatched){
		////logger.debug("alreadyMatched =" + alreadyMatched);
		int localBestScoreIdx = getBestScoreIdx(alreadyMatched); 

		if (localBestScoreIdx>-1){
			////logger.debug("best match for " + ref + " is " + cands.get(localBestScoreIdx) + 
			//		"; common = [" + commonSeqs.get(localBestScoreIdx)	+ "]; score = " + scores.get(localBestScoreIdx));
			return (BigDecimal) scores.get(localBestScoreIdx);
		}else{
			return ATSDecimalNumberFormat.ZERO;
		}
	}


	public String getBestMatch() {
		return getBestMatch(new ArrayList());
	}

	public String getBestMatch(List alreadyMatched) {
		int localBestScoreIdx = getBestScoreIdx(alreadyMatched); 

		if (localBestScoreIdx>-1){
			return (String) cands.get(localBestScoreIdx);
		}else{
			return null;
		}
	}

	private int getBestScoreIdx(List alreadyMatched) {
		if (alreadyMatched.size()==0){
			return bestScoreIdx;
		}
		int localBestScoreIdx = -1; 
		BigDecimal bestScore = ATSDecimalNumberFormat.ZERO;

		for(int i=0; i<cands.size(); i++){
			if (!contains (alreadyMatched,(String) cands.get( i))){
				BigDecimal score = (BigDecimal) scores.get(i);
				if (bestScore.compareTo(score)<=0){
					localBestScoreIdx = i;
					bestScore = score;
				}
			}	
		}

		return localBestScoreIdx;
	}



	private boolean contains(List alreadyMatched, String string) {//methoda contains din List nu este buna,
		//pt ca putem avea stringuri care sint identice, dar reprezinta tokeni dif. ex john e ~ e e
		for (Iterator iter = alreadyMatched.iterator(); iter.hasNext();) {
			String token = (String) iter.next();
			if (token == string) return true;
		}
		return false;
	}

	protected static BigDecimal getScoreOneToken(String refToken, String candToken, String seq, int refPos, int candPos, boolean penilizeDifferentPositions) {
		BigDecimal ref = BigDecimal.valueOf(refToken.length()).setScale(2);
		BigDecimal cand = BigDecimal.valueOf(candToken.length()).setScale(2);
		BigDecimal subseq = BigDecimal.valueOf(seq.length()).setScale(2);
		BigDecimal consec = BigDecimal.valueOf(firstDiffCharIdx(refToken, candToken)).setScale(2);
		
		BigDecimal wRef = (new BigDecimal("0.5"));
		BigDecimal wCand = (new BigDecimal("0.5"));
		
		/*
		if (consec.compareTo(ref) == 0 ){
			wRef = new BigDecimal("0.8");
			wCand = new BigDecimal("0.2");
		}

		if (consec.compareTo(cand) == 0 ){
			wRef = new BigDecimal("0.2");
			wCand = new BigDecimal("0.8");
		}*/

	
		BigDecimal localScore1 = subseq.multiply(wRef).divide(ref, BigDecimal.ROUND_HALF_EVEN);
		////logger.debug("common * wRef = " + localScore1);
		
		BigDecimal localScore2 = subseq.multiply(wCand).divide(cand, BigDecimal.ROUND_HALF_EVEN);
		////logger.debug("common * wCand = " + localScore2);
		
		localScore1 = localScore1.add(localScore2);
		////logger.debug("local = (common * wRef)/ref + (common * wCand)/cand= " + localScore1);

		BigDecimal wPrefix = new BigDecimal("0.7");
		BigDecimal penalPrefix = consec;
		if (subseq.compareTo(ATSDecimalNumberFormat.ZERO)>0) {
			penalPrefix = penalPrefix.divide(subseq, BigDecimal.ROUND_HALF_EVEN);
		}
		penalPrefix = penalPrefix.multiply( wPrefix);
		penalPrefix = penalPrefix.add(ATSDecimalNumberFormat.ONE.subtract(wPrefix));
		////logger.debug("penalPrefix = ((1-wPrefix) + wPrefix* prefix/common) = " + penalPrefix);

		BigDecimal localScore = localScore1.multiply(penalPrefix);
		//logger.debug("local * penalPrefix = " + localScore);

		if ((penilizeDifferentPositions)&&(refPos != candPos)){
			//localScore = localScore.multiply(new BigDecimal("0.75"));
		}
		//logger.debug("local * penalizare pozitie = " + localScore);
		return localScore;
	}

	
	protected static String commonSubseq(String a, String b) {
		
		// create the table for the longest common subsequence
		int [][] L = new int[a.length()+1][b.length()+1];
		for (int i = 0; i <a.length(); i++) L[i][b.length()] = 0;
		for (int i = 0; i <b.length(); i++) L[a.length()][i] = 0;

		for (int i = a.length() - 1; i >= 0; i--)
			for (int j = b.length() - 1; j >= 0; j--) {
				if (a.charAt(i)== b.charAt(j)) L[i][j] = 1 + L[i+1][j+1];
				else L[i][j] = max(L[i+1][j], L[i][j+1]);
			}
		// l[0][0] now has the length of the longest common subsequence
		
		String seq = new String();
		int i = 0;
		int j = 0;
		while (i < a.length() && j < b.length()) {
			if (a.charAt(i)==b.charAt(j)) {
					seq = seq + a.charAt(i);
					i++; j++;
			} else if (L[i+1][j] >= L[i][j+1]) {
				i++;
			}else {
				j++; 
			}
		}
		
		return seq;
	}

	protected static int max(int n1, int n2) {
		if (n1 > n2) return n1;
		return n2;
	}

	protected static int min(int n1, int n2) {
		if (n1 > n2) return n2;
		return n1;
	}


	protected static int firstDiffCharIdx(String refToken, String candToken) {
		int consec;
		for (consec = 0; consec < (min(refToken.length(), candToken.length())); consec++){
			if (refToken.charAt(consec) != candToken.charAt(consec)) break;
		}
		return consec;
	}


	public String toString(){
		return "MatchToken(" + ref + ")";
	}


}

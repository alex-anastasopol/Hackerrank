/*
 * Created on Jul 17, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter.matchers.algorithm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class RegisterNameMatchAlgorithm extends MatchAlgorithm{

	protected static final Category logger = Category.getInstance(RegisterNameMatchAlgorithm.class.getName());
	

	public RegisterNameMatchAlgorithm (long searchId){
		super(searchId);
	}


	protected List getLocalScores() {
		//logger.debug("starting getLocalScores " );
		List localScores = new ArrayList();
		
		List localMatches = new ArrayList(matches);
		Collections.sort(localMatches, new BestScoreMatchTokenComparator());
		
		List alreadyMatched = new ArrayList();
		
		for (int i=0; i < localMatches.size(); i++){
			MatchToken mt = (MatchToken) localMatches.get(i);
			String bestMatch = mt.getBestMatch(alreadyMatched);
			BigDecimal bestScore = mt.getBestScore(alreadyMatched);
			//logger.debug(" for " + mt  + " alreadyMatched = " + alreadyMatched + " best match = " + bestMatch + " best score = " + bestScore);
			if (bestMatch != null){
				localScores.add(bestScore);
				alreadyMatched.add(bestMatch);
			}
		}
		//BigDecimal av = new BigDecimal(1);
		//av = av.divide(ZERO, BigDecimal.ROUND_HALF_EVEN);

		return localScores;
	}
	
	public BigDecimal getScore() {				
		
		// verify if candidate concatenated tokens are equal with the reference concatenated tokens - bug #1966
		StringBuilder ref = new StringBuilder();
		int refLen = refTokens.size();
		for (int refIdx = 0; refIdx < refLen; refIdx ++){
			ref.append((String) refTokens.get(refIdx));			
		}
		StringBuilder cand = new StringBuilder();
		int candLen = candTokens.size();
		for (int candIdx = 0; candIdx < candLen; candIdx ++){
			cand.append((String) candTokens.get(candIdx));			
		}
		
		
		if (ref.toString().equals(cand.toString())) {
			if(ref.toString().equals(""))	//if both are empty we signal this
				return ATSDecimalNumberFormat.NA;
			return new BigDecimal(0.999);
			
		}
		
		return super.getScore();
	}

}

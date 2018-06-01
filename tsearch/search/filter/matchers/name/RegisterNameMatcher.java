/*
 * Created on Jul 17, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter.matchers.name;

import org.apache.commons.lang.*;
import java.math.BigDecimal;

import org.apache.log4j.Category;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.search.tokenlist.TokenList;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class RegisterNameMatcher extends NameMatcher{
	protected static final Category logger = Category.getInstance(RegisterNameMatcher.class.getName());

	private static BigDecimal weightOwnerLastName = ATSDecimalNumberFormat.ONE; //new BigDecimal(5);  
	private static BigDecimal weightOwnerFirstName = ATSDecimalNumberFormat.ONE; //new BigDecimal(5);  
	private static BigDecimal weightSpouseLastName = new BigDecimal("0.3"); //new BigDecimal(1);  
	private static BigDecimal weightSpouseFirstName = ATSDecimalNumberFormat.ONE; //new BigDecimal(3);
	
	public RegisterNameMatcher(long searchId){
		super(searchId);
		
		matcherType = MatchAlgorithm.TYPE_REGISTER_NAME;
	}
	
	/**
	 * This function compares 2 husband-wife pairs of names
	 * first it uses old algoritm. if the result is not veryhigh then
	 * it tries to account for incorrect tokenization by concatenating all words.
	 * if the new result is pretty high, then it is returned, otherwise original
	 * algorithm's result is returned
	 * @param refOwner name token list of reference husband
	 * @param refSpouse name token list of reference wife
	 * @param candOwner name token list of candidate husband
	 * @param candSpouse name token list of candidate wife
	 * @return - result of scoring
	 */
	public BigDecimal getScore(NameTokenList refOwnerOriginal, NameTokenList refSpouseOriginal, 
			NameTokenList candOwnerOriginal, NameTokenList candSpouseOriginal) {
		
		NameTokenList refOwner = new NameTokenList(refOwnerOriginal);
		NameTokenList refSpouse = new NameTokenList(refSpouseOriginal);
		NameTokenList candOwner = new NameTokenList(candOwnerOriginal);
		NameTokenList candSpouse = new NameTokenList(candSpouseOriginal);
		
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		boolean ignoreMiddleName = search.getSa().isIgnoreOwnerMiddleName(); 
		if(ignoreMiddleName) {
			//this is for CR2109 - ignore middle name
			refOwner.clearMiddleName();
			refSpouse.clearMiddleName();
			candOwner.clearMiddleName();
			candSpouse.clearMiddleName();
		}
		
		
		// compute with original algorithm
		double returnScore = getOriginalScore(refOwner, refSpouse, candOwner, candSpouse).doubleValue();
		
		// do not try the alternative algorithm if result is already high
		if(returnScore > 0.95){
			if(ignoreMiddleName)
				return new BigDecimal(returnScore * weightIgnoreMiddleName);
			else 
				return new BigDecimal(returnScore);
		}

		// glue together all token fields
		String refHusband  = TokenList.getString(refOwner.getList()); 
		String refWife     = TokenList.getString(refSpouse.getList());
		String candHusband = TokenList.getString(candOwner.getList()); 
		String candWife    = TokenList.getString(candSpouse.getList());
		
		// compute husband score
		double husbandsScore = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT , refHusband, candHusband,searchId)).getScore().doubleValue();		
		// try to match husband initials
		if(matchInitial && husbandsScore < 0.95){
			double newScore = computeScoreMatchInitials(refHusband, candHusband);
			if(newScore > husbandsScore){
				husbandsScore = newScore;
			}
		}		
		// compute wife score
		double wifeScore     = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT , refWife,    candWife   ,searchId)).getScore().doubleValue();
		// compute cross-scores
		double crossScore1   = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT , refHusband, candWife  ,searchId )).getScore().doubleValue();
		double crossScore2   = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT , refWife,    candHusband,searchId)).getScore().doubleValue();
		
		// clear results of comparisons with empty strings
		if(refHusband.length() == 0){
			husbandsScore = 0;
			crossScore1 = 0;
		}
		if(candHusband.length() == 0){
			husbandsScore = 0;
			crossScore2 = 0;
		}
		if(refWife.length() == 0){
			wifeScore = 0;
			crossScore2 = 0;
		}
		if(candWife.length() == 0){
			wifeScore = 0;
			crossScore1 = 0;
		}
		
		// compute new score as maximum value of comparisons
		double newScore = (husbandsScore>wifeScore)?husbandsScore:wifeScore;
		if(crossScore1 > newScore){
			newScore = crossScore1;
		}
		if(crossScore2 > newScore){
			newScore = crossScore2;
		}
		
		// make new score never reach 1.0
		newScore = newScore - 0.01;

		// update the result if necessary
		if(newScore > returnScore){
			if(newScore > 0.90){
				//nameMatchFixLogger.logString("RegisterNameMatcher: comparing {" + candHusband + "}{" + candWife + "} vs {" + refHusband + "}{" + refWife + "} :" + "oldScore = " + returnScore + " newScore = " + newScore);				
				returnScore = newScore;
			}
		}
		
		// try to "glue" together the individual words inside tokens
		if(newScore < 0.90){
			NameTokenList refOwner1   = refOwner.collapse();
			NameTokenList refSpouse1  = refSpouse.collapse();
			NameTokenList candOwner1  = candOwner.collapse();
			NameTokenList candSpouse1 = candSpouse.collapse();
			
			newScore = getOriginalScore(refOwner1, refSpouse1, candOwner1, candSpouse1).doubleValue();
			if(newScore > 0.90){
				returnScore = newScore;
			}
		}
		
		// return score
		if(ignoreMiddleName)
			return new BigDecimal(returnScore * weightIgnoreMiddleName);
		else 
			return new BigDecimal(returnScore);
	}

	private BigDecimal getOriginalScore(NameTokenList refOwner, NameTokenList refSpouse, NameTokenList candOwner, NameTokenList candSpouse) {
		BigDecimal score = ATSDecimalNumberFormat.ZERO;
		BigDecimal weightSum = ATSDecimalNumberFormat.ZERO;
		
		String refOwnerLast = TokenList.getString(refOwner.getLastName());
		String refOwnerFirst = TokenList.getString(refOwner.getFirstName()) 
							+ " " +  TokenList.getString(refOwner.getMiddleName());
							
		String refSpouseLast = TokenList.getString(refSpouse.getLastName());
		String refSpouseFirst = TokenList.getString(refSpouse.getFirstName()) 
							+ " " +  TokenList.getString(refSpouse.getMiddleName());
							
		String candOwnerLast = TokenList.getString(candOwner.getLastName());
		String candOwnerFirst = TokenList.getString(candOwner.getFirstName()) 
							+ " " +  TokenList.getString(candOwner.getMiddleName());
							
		String candSpouseLast = TokenList.getString(candSpouse.getLastName());
		String candSpouseFirst = TokenList.getString(candSpouse.getFirstName()) 
							+ " " +  TokenList.getString(candSpouse.getMiddleName());
							
		
		//logger.debug ("Matching [" + refOwnerLast + " " + refOwnerFirst + " & " + refSpouseLast + " " + refSpouseFirst 
		//		+ "] vs [" + candOwnerLast + " " + candOwnerFirst + " & " + candSpouseLast + " " + candSpouseFirst+ "]");
		
		BigDecimal refOwnerLastCandOwnerLast = (MatchAlgorithm.getInstance(matcherType, refOwnerLast, candOwnerLast,searchId)).getScore(); 
		//BigDecimal refSpouseLastCandSpouseLast = (new MatchAlgorithm(refSpouseLast, candSpouseLast)).getScore(); 
		//logger.debug ("refSpouseLastCandSpouseLast = " + refSpouseLastCandSpouseLast);


		BigDecimal refOwnerFirstCandOwnerFirst = (MatchAlgorithm.getInstance(matcherType, refOwnerFirst, candOwnerFirst,searchId)).getScore(); 
		//BigDecimal refSpouseFirstCandSpouseFirst = (new MatchAlgorithm(refSpouseFirst, candSpouseFirst)).getScore(); 

		BigDecimal refOwnerFirstCandSpouseFirst = (MatchAlgorithm.getInstance(matcherType, refOwnerFirst, candSpouseFirst,searchId)).getScore(); 
		//BigDecimal refSpouseFirstCandOwnerFirst = (new MatchAlgorithm(refSpouseFirst, candOwnerFirst)).getScore(); 

		// company
		if(StringUtils.isStringBlank(refOwnerFirst)) {
			BigDecimal scoreComp = MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT,refOwnerLast,candOwnerLast + " " + candOwnerFirst,searchId).getScore();
			if(scoreComp.compareTo(new BigDecimal("0.65")) > 0) {
					return ATSDecimalNumberFormat.ONE;
			}
		}

		if (refOwnerLastCandOwnerLast.doubleValue()<0.9){
			return ATSDecimalNumberFormat.ZERO; 
		}
		

		BigDecimal score1 = refOwnerFirstCandOwnerFirst;
		//logger.debug ("score1 = " + score1 );
		BigDecimal score2 = refOwnerFirstCandSpouseFirst;
		//logger.debug ("score2 = " + score2 );
		weightSum = weightSum.add(weightOwnerFirstName);  

		BigDecimal scoreFirstName = score1.max(score2);
		
		if(candOwnerFirst.trim().equals(""))
			scoreFirstName = score2;
		if(candSpouseFirst.trim().equals(""))
			scoreFirstName = score1;
		
		//logger.debug ("scoreFirstNames = " + scoreFirstName );
		score = score.add(scoreFirstName);			
		

		BigDecimal fScore = score.divide(weightSum, BigDecimal.ROUND_HALF_EVEN); 
		//logger.debug ("SCORE = " + score + "/" + weightSum + " = " + fScore);
		//logger.debug ("SCORE = " + fScore);
		return fScore;					
	}





}

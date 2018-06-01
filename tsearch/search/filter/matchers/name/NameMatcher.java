package ro.cst.tsearch.search.filter.matchers.name;

import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.log4j.Category;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.search.tokenlist.TokenList;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.InstanceManager;
//import ro.cst.tsearch.utils.LowPriorityFileLogger;
import ro.cst.tsearch.utils.StringUtils;

public class NameMatcher implements Serializable {
    static final long serialVersionUID = 10000000;
    
	protected static final Category logger = Category.getInstance(NameMatcher.class.getName());

	private static BigDecimal weightOwnerLastName = ATSDecimalNumberFormat.ONE; //new BigDecimal(5);  
	private static BigDecimal weightOwnerFirstName = ATSDecimalNumberFormat.ONE; //new BigDecimal(5);  
	private static BigDecimal weightSpouseLastName = new BigDecimal("0.3"); //new BigDecimal(1);  
	private static BigDecimal weightSpouseFirstName = ATSDecimalNumberFormat.ONE; //new BigDecimal(3);
	
	public static double weightIgnoreMiddleName = 0.95;
	
	protected int matcherType = MatchAlgorithm.TYPE_DEFAULT;
	
	protected long searchId = -1;
	public  NameMatcher(long searchId){
		this.searchId = searchId;
	}
	
	protected boolean matchInitial = false;
	public void setMatchInitial(boolean matchInitial){
		this.matchInitial = matchInitial;
	}
	
	//protected static LowPriorityFileLogger nameMatchFixLogger = new LowPriorityFileLogger("tslogs/name-match-fixes.txt", 100);
	
	/**
	 * compute the score by considering matching parts of words with whole words,
	 * starting with the 2nd word - last name
	 */
	protected double computeScoreMatchInitials(String refName, String candName){
		// split name strings
		String [] refTokens = refName.split(" ");
		String [] candTokens = candName.split(" ");
		
		// if same number of tokens && more than last name
		if(refTokens.length == candTokens.length && candTokens.length > 1){
			
			// check prefix match for all words except first one
			boolean tryMatch = false;
			for(int i=1; i<refTokens.length; i++){		
				if(refTokens[i].equals(candTokens[i])){ 
					continue; 
				}
				if(refTokens[i].startsWith(candTokens[i])){
					refTokens[i] = candTokens[i];
					tryMatch = true;
				} else if(candTokens[i].startsWith(refTokens[i])){
					candTokens[i] = refTokens[i];
					tryMatch = true;
				}				
			}
			
			// if any token has changed then try again
			if(tryMatch){
				String ref = StringUtils.join(refTokens, " ");
				String cand = StringUtils.join(candTokens, " ");
				double score = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT , ref, cand,searchId)).getScore().doubleValue();
				return score;
			}				
		}
		return 0;
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
		
		// compute husband scores
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
		double crossScore1   = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT , refHusband, candWife   ,searchId)).getScore().doubleValue();
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
		
		// make empty address always match another empty address
		if((refHusband.length()+candHusband.length())== 0){
			husbandsScore = 1;
		}
		if((refWife.length()+candWife.length())== 0){
			wifeScore = 1;
		}
		if((refHusband.length()+candWife.length())== 0){
			crossScore1 = 1;
		}
		if((refWife.length()+candHusband.length())== 0){
			crossScore2 = 1;
		}
		
		// decide which set of comparisons is better
		double s1,s2;
		if((crossScore1 + crossScore2)>(husbandsScore+wifeScore)){
			s1 = crossScore1;
			s2 = crossScore2;
		}else{
			s1 = husbandsScore;
			s2 = wifeScore;			
		}
		
		// compute new score as average of best set
		// make new score never reach 1.0
		double newScore = (s1 + s2)/2 - 0.01;

		// update the result if necessary
		if(newScore > returnScore){
			if(newScore > 0.90){
				//nameMatchFixLogger.logString("NameMatcher: comparing {" + candHusband + "}{" + candWife + "} vs {" + refHusband + "}{" + refWife + "} :" + "oldScore = " + returnScore + " newScore = " + newScore);                    						
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
							
		
		BigDecimal refOwnerLastCandOwnerLast = (MatchAlgorithm.getInstance(matcherType , refOwnerLast, candOwnerLast,searchId)).getScore(); 
		BigDecimal refSpouseLastCandSpouseLast = (MatchAlgorithm.getInstance(matcherType ,refSpouseLast, candSpouseLast,searchId)).getScore(); 
		//logger.debug ("refSpouseLastCandSpouseLast = " + refSpouseLastCandSpouseLast);


		BigDecimal refOwnerFirstCandOwnerFirst = (MatchAlgorithm.getInstance(matcherType ,refOwnerFirst, candOwnerFirst,searchId)).getScore(); 
		BigDecimal refSpouseFirstCandSpouseFirst = (MatchAlgorithm.getInstance(matcherType ,refSpouseFirst, candSpouseFirst,searchId)).getScore(); 

		BigDecimal refOwnerFirstCandSpouseFirst = (MatchAlgorithm.getInstance(matcherType ,refOwnerFirst, candSpouseFirst,searchId)).getScore(); 
		BigDecimal refSpouseFirstCandOwnerFirst = (MatchAlgorithm.getInstance(matcherType ,refSpouseFirst, candOwnerFirst,searchId)).getScore(); 

		// company
		if(StringUtils.isStringBlank(refOwnerFirst) || NameUtils.isCompany(refOwner)) {
			BigDecimal scoreComp = MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT,refOwnerLast,candOwnerLast + " " + candOwnerFirst,searchId).getScore();
			if(scoreComp.compareTo(new BigDecimal("0.65")) > 0) {
					return ATSDecimalNumberFormat.ONE;
			}
		}

		if (refOwnerLastCandOwnerLast.doubleValue()<0.9){
			return ATSDecimalNumberFormat.ZERO; 
		}
		
		/*else{
			score = refOwnerLastCandOwnerLast;
			weightSum = weightSum.add(weightOwnerLastName);  
		}*/
		//logger.debug ("initial score = " + score );

		if (!StringUtils.isStringBlank(refSpouseLast)&&(!refSpouseLast.equalsIgnoreCase(refOwnerLast))){
			score = score.add(refSpouseLastCandSpouseLast.multiply(weightSpouseLastName));
			weightSum = weightSum.add(weightSpouseLastName);  
			if (logger.isDebugEnabled())
				logger.debug ("score spouse last name = " + score );
		}


		BigDecimal score1 = refOwnerFirstCandOwnerFirst;
		if (logger.isDebugEnabled())
			logger.debug ("score1 = " + score1 );
		BigDecimal score2 = refOwnerFirstCandSpouseFirst;
		if (logger.isDebugEnabled())
			logger.debug ("score2 = " + score2 );
		weightSum = weightSum.add(weightOwnerFirstName);  

		if (!StringUtils.isStringBlank(refSpouseFirst)){
			score1 = score1.add(refSpouseFirstCandSpouseFirst);
			//score1 = score1.multiply(new BigDecimal("0.2"));

			if (logger.isDebugEnabled())
				logger.debug ("score1 = " + score1 );


			score2 = score2.add(refSpouseFirstCandOwnerFirst);
			//score2 = score2.multiply(new BigDecimal("0.2"));

			if (logger.isDebugEnabled())
				logger.debug ("score2 = " + score2 );

			weightSum = weightSum.add(weightSpouseFirstName);  
		}
		
		BigDecimal scoreFirstName = score1.max(score2);
		if (logger.isDebugEnabled())
			logger.debug ("scoreFirstNames = " + scoreFirstName );
		score = score.add(scoreFirstName);			
		

		BigDecimal fScore = score.divide(weightSum, BigDecimal.ROUND_HALF_EVEN);
		if (logger.isDebugEnabled())
			logger.debug ("SCORE = " + score + "/" + weightSum + " = " + fScore);
		//logger.debug ("SCORE = " + fScore);
		//return fScore;
		// (fscore * 3 + last_score) / 4
		return fScore.add(fScore).add(fScore).add(refOwnerLastCandOwnerLast).divide(new BigDecimal(4));
	}

	public void setMatcherType(int i) {
		matcherType = i;
	}

}

/*
 * Created on Jul 17, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter.matchers.algorithm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Category;

import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.MatchEquivalents;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class MatchAlgorithm {

	protected static final Category logger = Category.getInstance(MatchAlgorithm.class.getName());

	
	public static final int TYPE_DEFAULT = 1;
	public static final int TYPE_REGISTER_NAME = 2;
	public static final int TYPE_REGISTER_NAME_NA = 3;
	public static final int TYPE_LOT = 4;
	public static final int TYPE_LOT_ZERO_IGNORE = 5;
	public static final int TYPE_SUBDIV_NA_DESOTO = 6;
	public static final int TYPE_IGNORE_DIFF_NO_TOKENS = 7;
	public static final int TYPE_REGISTER_NAME_IGNORE_DIFF_NO_TOKENS = 8;
	public static final int TYPE_DEFAULT_NA = 9;
	public static final int TYPE_GENERIC_MATCH = 10;


	
	protected List refTokens = new ArrayList();
	protected List candTokens = new ArrayList();
	protected List matches = new ArrayList();
	
	protected BigDecimal finalScore = ATSDecimalNumberFormat.ZERO;
	protected boolean wasMatched = false;
	protected boolean panilizeDifferentPositions = true; 
	protected boolean shouldPenilizeDiffNoTokens = true; 

	protected List ignorePatterns = new ArrayList();
	protected boolean NAResultFlag = false;


	protected long searchId=-1;
	public MatchAlgorithm(long searchId){
		this.searchId = searchId;
	}
	
	public static MatchAlgorithm getInstance (int type, String ref, String cand,long searchId){
		if (type == TYPE_REGISTER_NAME){
			MatchAlgorithm ma = new RegisterNameMatchAlgorithm(searchId);
			ma.init(ref, cand);
			return ma;
		}else if (type == TYPE_REGISTER_NAME_NA){
			MatchAlgorithm ma = new RegisterNameMatchAlgorithm(searchId);
			ma.setNAResultFlag(true);
			ma.init(ref, cand);
			return ma;
		}else if (type == TYPE_REGISTER_NAME_IGNORE_DIFF_NO_TOKENS){
			MatchAlgorithm ma = new RegisterNameMatchAlgorithm(searchId);
			ma.setPenilizeDiffNoTokensFlag(false);
			ma.init(ref, cand);
			return ma;
		}else if (type == TYPE_SUBDIV_NA_DESOTO){
			MatchAlgorithm ma = new RegisterNameMatchAlgorithm(searchId);
			ma.setNAResultFlag(true);
			ma.addIgnorePattern(Pattern.compile("0+"));
			ma.addIgnorePattern(Pattern.compile(".*MINOR LOT.*",Pattern.CASE_INSENSITIVE));
			ma.init(ref, cand);
			return ma;
		}else if (type == TYPE_LOT){
			MatchAlgorithm ma = new LotMatchAlgorithm(searchId);
			ma.setPenilizeDifferentPositions(false);
			ma.setNAResultFlag(true);
			ma.init(ref, cand);
			return ma;
		}else if (type == TYPE_LOT_ZERO_IGNORE){
			MatchAlgorithm ma = new LotMatchAlgorithm(searchId);
			ma.addIgnorePattern(Pattern.compile("0"));
			ma.setPenilizeDifferentPositions(false);
			ma.setNAResultFlag(true);
			ma.init(ref, cand);
			return ma;
		}else if (type == TYPE_IGNORE_DIFF_NO_TOKENS){
			MatchAlgorithm ma = new MatchAlgorithm(searchId);
			ma.setPenilizeDiffNoTokensFlag(false);
			ma.init(ref, cand);
			return ma;
		}else if (type == TYPE_DEFAULT_NA){
			MatchAlgorithm ma = new MatchAlgorithm(searchId);
			ma.setNAResultFlag(true);
			ma.init(ref, cand);
			return ma;
		}else if (type == TYPE_GENERIC_MATCH){
			MatchAlgorithm ma = new GenericMatchAlgorithm(searchId);
			ma.addIgnorePattern(Pattern.compile("0"));
			ma.setPenilizeDifferentPositions(false);
			ma.setNAResultFlag(true);
			ma.init(ref, cand);
			return ma;
		}else { 
			MatchAlgorithm ma = new MatchAlgorithm(searchId);
			ma.init(ref, cand);
			return ma;
		}
	}

	protected void init (String ref, String cand){
		if (logger.isDebugEnabled())
			logger.debug("MATCH " + ref  + " with " + cand);
		
		if (StringUtils.matchPatterns(ignorePatterns, ref)){
			ref = "";
		}
		if (StringUtils.matchPatterns(ignorePatterns, cand)){
			cand = "";
		}
		
		if (StringUtils.isStringBlank(ref)){
			finalScore = ATSDecimalNumberFormat.ONE;
			if (NAResultFlag){
				finalScore = ATSDecimalNumberFormat.NA;
			}
			wasMatched = true;
			return;
		}
		ref = ref.toUpperCase();
		ref = MatchEquivalents.getInstance(searchId).getEquivalent( ref );
        
		
		if (StringUtils.isStringBlank(cand)){
			finalScore = ATSDecimalNumberFormat.ZERO;
			if (NAResultFlag){
				finalScore = ATSDecimalNumberFormat.NA;
			}
			wasMatched = true;
			return;
		}
		cand = cand.toUpperCase();
		if (ref.matches(".*[\\s]R[I]?DG[E]?[\\s].*") || 
			ref.matches("R[I]?DG[E]?[\\s].*") ||
			ref.matches(".*[\\s]R[I]?DG[E]?"))
		{
			if (cand.matches("(.*)(R[I]?DG[E]?)(.*)") &&
				!cand.matches(".*[\\s]R[I]?DG[E]?[\\s].*") && 
				!cand.matches("R[I]?DG[E]?[\\s].*") &&
				!cand.matches(".*[\\s]R[I]?DG[E]?"))
			{
				cand = cand.replaceFirst("(.*)(R[I]?DG[E]?)(.*)", "$1"+" "+"$2"+" "+"$3");
				cand = cand.trim();
				cand = cand.replaceAll("[\\s]+", " ");
			}
		}
		cand = MatchEquivalents.getInstance(searchId).getEquivalent( cand );
        
		refTokens = new ArrayList(StringUtils.splitString(ref)); 
		candTokens = new ArrayList(StringUtils.splitString(cand));

		removeIgnorePatterns(refTokens,ignorePatterns);
		removeIgnorePatterns(candTokens,ignorePatterns);

		// elliminate empty strings from refTokens, candTokens
		if(refTokens.size() == 1 && "".equals(refTokens.get(0))){
			refTokens = new ArrayList();
		}
		if(candTokens.size() == 1 && "".equals(candTokens.get(0))){
			candTokens = new ArrayList();
		}
		
		if (refTokens.size()==0){
			finalScore = ATSDecimalNumberFormat.ONE;
			if (NAResultFlag){
				finalScore = ATSDecimalNumberFormat.NA;
			}
			wasMatched = true;
			return;
		}

		if (candTokens.size()==0){
			finalScore = ATSDecimalNumberFormat.ZERO;
			if (NAResultFlag){
				finalScore = ATSDecimalNumberFormat.NA;
			}
			wasMatched = true;
			return;
		}

	}
	
	public BigDecimal getScore() {

		if (!wasMatched){
			for (int refIdx = 0; refIdx < refTokens.size(); refIdx ++){
				String ref = (String) refTokens.get(refIdx);
				matchOneToken(ref, refIdx);
			}
			
			finalScore = computeFinalScore();
		}
		
		if (logger.isDebugEnabled())
			logger.debug("score " + finalScore);
		return finalScore; 
	}

	protected void matchOneToken(String ref, int refIdx) {
		MatchToken refToken = new MatchToken(ref, refIdx,panilizeDifferentPositions);
		refToken.match(candTokens);
		matches.add(refToken);
	}

	protected BigDecimal computeFinalScore() {
		BigDecimal score = averageSum(getLocalScores());
		if (shouldPenilizeDiffNoTokens){
			score = score.multiply(penalizeDiffNoTokens());
		}
		if (logger.isDebugEnabled())
			logger.debug("final score = " + score);
		return score;
	}

	protected List getLocalScores() {
		List localScores = new ArrayList();
		for (int i=0; i < matches.size(); i++){
			localScores.add(((MatchToken) matches.get(i)).getBestScore());
		}
		return localScores;
	}

	protected static BigDecimal averageSum(List operands) {
		BigDecimal av = ATSDecimalNumberFormat.ZERO;
		//BigDecimal sumWeights = ATSDecimalNumberFormat.ZERO;
		
		for (int i=0; i < operands.size(); i++){
			//BigDecimal weight = BigDecimal.valueOf(((String) refTokens.get(i)).length()).setScale(2);
			//BigDecimal weight2 = BigDecimal.valueOf(((String) commonSubseqs.get(i)).length()).setScale(2);
			//weight = weight2.divide(weight, BigDecimal.ROUND_HALF_EVEN);
			//score = score.add(weight.multiply((BigDecimal) scores.get(i)));
			av = av.add((BigDecimal) operands.get(i));
			//sumWeights = sumWeights.add(weight);
		}
		if (operands.size()>0){ 
			av = av.divide(new BigDecimal(operands.size()), BigDecimal.ROUND_HALF_EVEN);
		}else{
			av = ATSDecimalNumberFormat.NA;
		}
		if (logger.isDebugEnabled())
			logger.debug(" averageSum = " + av);
		return av;
	}

	public static BigDecimal maxNo(List components) {
		BigDecimal max = ATSDecimalNumberFormat.NA;
		
		for (int i=0; i < components.size(); i++){
			BigDecimal component = (BigDecimal) components.get(i);
			if (component.compareTo(max)>0){
				max = component;
			}
		}
		
		return max;
	}

	protected BigDecimal penalizeDiffNoTokens() {
		BigDecimal wNoDifTokens = new BigDecimal("0.7");
		
		BigDecimal n1 = new BigDecimal(refTokens.size()).setScale(2);
		BigDecimal n2 = new BigDecimal(candTokens.size()).setScale(2);
		if (refTokens.size()>candTokens.size()){
			BigDecimal n = n1;
			n1 = n2;
			n2 = n;
		}
		BigDecimal tokenNoRation = n1.divide(n2,BigDecimal.ROUND_HALF_EVEN);

		BigDecimal penalty = tokenNoRation.multiply( wNoDifTokens);
		penalty = penalty.add(ATSDecimalNumberFormat.ONE.subtract(wNoDifTokens));
		if (logger.isDebugEnabled())
			logger.debug("penalizeDiffNoTokens = (0.3 + 0.7* nr min token/nr max tokeni) = " + penalty);
		return penalty;
	}

	protected void removeIgnorePatterns(List tokens, List patterns){
		for (Iterator iter = tokens.iterator(); iter.hasNext();) {
			String token = (String) iter.next();
			if (StringUtils.matchPatterns(patterns, token)){
				iter.remove();
			}
		}
	}

	/**
	 * @param b
	 */
	public void setPenilizeDiffNoTokensFlag(boolean b) {
		shouldPenilizeDiffNoTokens = b;
	}

	/**
	 * @param b
	 */
	public void setNAResultFlag(boolean b) {
		NAResultFlag = b;
	}

	/**
	 * @param b
	 */
	public void setPenilizeDifferentPositions(boolean b) {
		panilizeDifferentPositions = b;
	}

	public void addIgnorePattern(Pattern p){
		ignorePatterns.add(p);
	}
}


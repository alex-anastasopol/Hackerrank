package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.search.filter.matchers.subdiv.SubdivMatcher;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.MatchEquivalents;
import ro.cst.tsearch.utils.StringCleaner;

/**
 * @author radu bacrau
 *
 */
public class SubdivisionNameFilterResponse2 extends FilterResponse {

	private static final long serialVersionUID = 100000000L;
	
    protected static final Category logger = Category.getInstance(SubdivisionNameFilterResponse.class.getName());
    protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + SubdivisionNameFilterResponse.class.getName());
    
    int matchAlgorithm  = MatchAlgorithm.TYPE_SUBDIV_NA_DESOTO;
    
    public SubdivisionNameFilterResponse2(long searchId){
        super(searchId);
        threshold = new BigDecimal("0.6");
        strategyType = STRATEGY_TYPE_HYBRID;
    }

    public BigDecimal getScoreOneRow(ParsedResponse row) {

    	String refName = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);    	
    	
    	if("".equals(refName)){
    		return ATSDecimalNumberFormat.ONE;
    	}
    	
    	BigDecimal finalScore = ATSDecimalNumberFormat.ZERO;
    	
    	List<String> candidates = getCandidates(row);
    	
    	for(String candName :  candidates){
    		    		
    		BigDecimal score = calculateScore(refName,candName);
            if(score.compareTo(finalScore) > 0){
            	finalScore = score;
            }
    	}
    	
        if (logger.isDebugEnabled()){
            logger.debug(" final score match subdiv = " + finalScore);
        }            
        IndividualLogger.info( " final score match subdiv = " + finalScore ,searchId);
        return finalScore;
        
    }
        
    public BigDecimal calculateScore(String refName,String candName) {
    	String ref = MatchEquivalents.getInstance(searchId).getEquivalent(refName);
		String cand = MatchEquivalents.getInstance(searchId).getEquivalent(candName);
		
		BigDecimal score = (MatchAlgorithm.getInstance(matchAlgorithm, ref, cand,searchId)).getScore();    		
 		
        if (logger.isDebugEnabled()){
            logger.debug(" score match subdiv " + candName + " is valid = " + score);
        }            
        IndividualLogger.info( " score match subdiv " + candName + " is valid = " + score,searchId );
        return score;
    }
    
    public void setMatchAlgorithm(int matchAlgorithm) {
    	this.matchAlgorithm = matchAlgorithm;
    }

    public int getMatchAlgorithm() {
        return matchAlgorithm ;
    }
    
    public List<String> getCandidates(ParsedResponse row) {
    	return getCandidateSubdivisions(row);
    }
    
    @Override
    public String getFilterName(){
    	return "Filter by Subdivision";
    }
    
    @Override
	public String getFilterCriteria(){
    	return "Subdivision='" + sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME) + "'";    	
    }
    
    public List<String> getCandidateSubdivisions(ParsedResponse row) {
    	List<String> cand = new ArrayList<String>();
    	for(int i=0; i<row.getPropertyIdentificationSetCount(); i++){
    		
    		String candName = row.getPropertyIdentificationSet(i).getAtribute("SubdivisionName");
    		
    		PropertyIdentificationSet pisCand = row.getPropertyIdentificationSet(i);
    		pisCand.setAtribute("SubdivisionName", StringCleaner.cleanString( stringCleanerId, candName));
    		cand.add(candName);
    	}
    	return cand;	
	}
}

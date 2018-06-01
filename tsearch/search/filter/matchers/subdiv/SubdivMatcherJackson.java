package ro.cst.tsearch.search.filter.matchers.subdiv;

import java.math.BigDecimal;

import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.StringCleaner;

public class SubdivMatcherJackson extends SubdivMatcher {

	private static final long serialVersionUID = 1000000000000L;
	private boolean beginsWithSubdiv = false;

	public SubdivMatcherJackson(long searchId){
		super(searchId);
		this.searchId = searchId;
	}
	
	public int validOneSubdivNameSecPhase(PropertyIdentificationSet pisRef, PropertyIdentificationSet pisCand) {
		int validSec = validPisAtt ("SubdivisionSection",pisRef, pisCand, MatchAlgorithm.TYPE_GENERIC_MATCH, "0.8");
		int validPhase = validPisAtt ("SubdivisionPhase",pisRef, pisCand, MatchAlgorithm.TYPE_GENERIC_MATCH, "0.8");
		int validSubdivName = validSubdivisionName ( pisRef, pisCand, subdivMatchAlgorithm, "0.78" );

		if ((validSec == 0)||(validPhase == 0)||(validSubdivName == 0)){
			return 0;
		}else if ((validSec == 1)||(validPhase == 1)||(validSubdivName == 1)){
			return 1;
		}else {
			return -1;
		}
	}
	
	public void setBeginsWithSubdiv(boolean flag){
		beginsWithSubdiv = flag;
	}
    
    public int validSubdivisionName( PropertyIdentificationSet pisRef, PropertyIdentificationSet pisCand, int matcherType, String threshold )
    {
        String ref = StringCleaner.cleanString(StringCleaner.JACKSON_SUBDIV, pisRef.getAtribute("SubdivisionName") );
        String cand = StringCleaner.cleanString(StringCleaner.JACKSON_SUBDIV, pisCand.getAtribute("SubdivisionName") );
        BigDecimal score = (MatchAlgorithm.getInstance(matcherType, ref, cand,searchId)).getScore();
        IndividualLogger.info( "comparing SubdivisionName... [" + ref + "] with [" + cand + "] = " + score,searchId );
        
        if (score == ATSDecimalNumberFormat.NA){
            return -1;
        }else if (score.compareTo(new BigDecimal(threshold))>=0){
            return 1;
        }else {
        	if (beginsWithSubdiv && cand.startsWith(ref + " ")){ // ref="PINES" and cand="PINES PR O" should match (bug #2816)         			
        		return 1; 
        	}
        	return 0;
        }
    }
}

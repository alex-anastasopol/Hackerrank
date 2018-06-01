package ro.cst.tsearch.search.filter.fornext;


import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.RegisterNameFilterResponse;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.IndividualLogger;

public class RegisterNameFilterResponseForNext extends RegisterNameFilterResponse
{
	public static final long serialVersionUID = 1000000L;


	protected static final Category logger = Category.getInstance(RegisterNameFilterResponseForNext.class.getName());
	
	protected String refLastName = null;   // reference last name - from search criteria
	protected String refFirstName =  null; // reference first name - from search criteria

	/**
	 * constructor
	 */
    public RegisterNameFilterResponseForNext(String lastName, String firstName,long searchId)
    {
        super("", null,searchId);
        refLastName = lastName.trim().toLowerCase();
        refFirstName = firstName.trim().toLowerCase();
    }
    
    /**
     * if any row has the score < threshold the NOT_PERFECT_MATCH_WARNING_FIRST error is set
     * and the next link append results will be disabled
     */
    protected void analyzeResult(ServerResponse sr, Vector rez) throws ServerResponseException
    {           
        if( rez.size() != sr.getParsedResponse().getInitialResultsCount() )
        {
            //filtered --> stop, do not go to next results
            sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST);
        }
    }
	
	/**
	 * returns 1 if last, first are not empty and they match what we asked for
	 * @param ntl
	 * @return
	 */
	protected double getScore(NameTokenList ntl){
        if(!refFirstName.equals("")){ // normal search: last + first name
			String candFirstName = ntl.getFirstNameAsString().trim().toLowerCase();
			String candLastName = ntl.getLastNameAsString().trim().toLowerCase();
			if((candFirstName.length()==0) && (refFirstName.length() != 0)){
				return 0.00; // reference has first name but candidate does not have
			}
			if(candLastName.length() == 0){
				return 0.00; // candidate does not have last name
			}
			if(!candLastName.startsWith(refLastName)){
				return 0.00; // last name not what we asked for
			}
			if(refFirstName.length() != 0){
				if(!candFirstName.startsWith(refFirstName) && ! refFirstName.startsWith( candFirstName )){
					return 0.00; // first name exists for reference, but not what we asked for
				}
			}
			return 1.00;
        }else{ // subdivision search - only last name
			String cand = (ntl.getLastNameAsString().trim() + " " + ntl.getFirstNameAsString().trim()).trim().toLowerCase();
			if(cand.length()==0){
				return 0.00; // empty candidate
			}
			if(!cand.startsWith(refLastName)){
				return 0.00; // last name not what we asked for
			}
			return 1.00;
        	
        }
	}
	
	/**
	 * compute the scores for 1 row
	 */
	public BigDecimal getScoreOneRow( ParsedResponse row) {
		List candNames = getCandNames(row);
		if (candNames == null){ //information not available=> score = one
			return ATSDecimalNumberFormat.ONE;
		}

		double maxScore = 0.0;
		for(Iterator it = candNames.iterator(); it.hasNext();){
			NameTokenList[] cand = (NameTokenList[])it.next();
			double crtMaxScore = 0.00;
			for(int i=0; i<cand.length; i++){
				if(!cand[i].isEmpty())
				{
					double crtScore = getScore(cand[i]);
					if(crtScore == 1.00){
						IndividualLogger.info("RO filter for next : final Score = " + crtScore ,searchId);
						return new BigDecimal(crtScore);
					}
					if(crtScore > crtMaxScore){
						crtMaxScore = crtScore;
					}
				}
			}
			if(crtMaxScore > maxScore){
				maxScore = crtMaxScore;
			}
		}
		
        IndividualLogger.info("RO filter for next : final Score = " + maxScore ,searchId);
        IndividualLogger.info("",searchId);
        
        return new BigDecimal(maxScore);
	}
	
	@Override
	public String getFilterCriteria(){		
		return "Last='" + refLastName + "', First='" + refFirstName + "'";
    }
}
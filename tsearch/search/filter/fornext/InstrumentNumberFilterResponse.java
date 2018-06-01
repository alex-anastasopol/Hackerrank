/*
 * Created on Mar 18, 2004
 *
 */
package ro.cst.tsearch.search.filter.fornext;

import java.math.BigDecimal;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.Log;

/**
 * Instrument number filtering for next.
 * 
 * @author catalinc
 *
 */
public class InstrumentNumberFilterResponse extends FilterResponse
{
	/**
	 * Main log category.
	 */
	protected static final Category logger = Category.getInstance(InstrumentNumberFilterResponse.class.getName());
	/**
	 * Details log category.
	 */
	protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + InstrumentNumberFilterResponse.class.getName());

	/**
	 * Reference instrument number.
	 */
	protected String instrumentNumber;

	/**
	 * Constructor
	 * 
	 * @param instrumentlNumber Candidate instrument number to be matched.
	 */
	public InstrumentNumberFilterResponse(String instrNumber,long searchId)
	{
		super(searchId);
		this.instrumentNumber = instrNumber;
		
		threshold = new BigDecimal("0.99");
	}

	/**
	 * Gets candidate instrument number.
	 * 
	 * @param row Parsed response.
	 * 
	 * @return parcel number as string.
	 */
	protected String getCandidateInstrumentNumber(ParsedResponse row)
	{
		return (String) row.getSaleDataSet(0).getAtribute("InstrumentNumber");
	}

	/**
	 * Score response for instrument number.
	 * 
	 * @see ro.cst.tsearch.search.filter.FilterResponse#getScoreOneRow(ro.cst.tsearch.servers.response.ParsedResponse)
	 */
	public BigDecimal getScoreOneRow(ParsedResponse row) 
	{
		
		BigDecimal score = 
			(MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT, instrumentNumber, getCandidateInstrumentNumber(row),searchId)).getScore();

		loggerDetails.debug("match ["+instrumentNumber+"] vs [" + getCandidateInstrumentNumber(row) + "] score=" + score);
		
		return score;	
	}

	protected void analyzeResult(ServerResponse sr, Vector rez) throws ServerResponseException
    {           
        if( rez.size() != sr.getParsedResponse().getInitialResultsCount() )
        {
            //filtered --> stop, do not go to next results
            sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST);
        }
    }
	
    @Override
    public String getFilterCriteria(){
    	return "instrument = " + instrumentNumber;
    }


}

/*
 * Created on Mar 18, 2004
 *
 */
package ro.cst.tsearch.search.filter.fornext;

import java.math.BigDecimal;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.Log;

/**
 * Parcel number filter for next.
 * 
 * @author catalinc
 *
 */
public class ParcelNumberFilterResponseForNext extends FilterResponse
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4702838345404074535L;
	/**
	 * Main log category.
	 */
	protected static final Category logger = Logger.getLogger(ParcelNumberFilterResponseForNext.class);
	/**
	 * Details log category.
	 */
	protected static final Category loggerDetails = Logger.getLogger(Log.DETAILS_PREFIX + ParcelNumberFilterResponseForNext.class.getName());

	/**
	 * Parcel number.
	 */
	protected String parcelNumber;
	protected boolean cleanParcelNumber;

	/**
	 * Constructor
	 * 
	 * @param instrumentlNumber Parcel number to be matched.
	 */
	public ParcelNumberFilterResponseForNext(String parcelNumber,long searchId)
	{
		super(searchId);
		this.parcelNumber = parcelNumber;
		
		threshold = new BigDecimal("0.99");
	}
	
	public ParcelNumberFilterResponseForNext(String parcelNumber,long searchId, boolean cleanParcelNumber)
	{
		super(searchId);
		this.parcelNumber = parcelNumber;
		this.cleanParcelNumber = cleanParcelNumber;
		
		threshold = new BigDecimal("0.99");
	}

	/**
	 * Gets candidate parcel number.
	 * 
	 * @param row Parsed response.
	 * 
	 * @return parcel number as string.
	 */
	protected String getCandidateParcelNumber(ParsedResponse row)
	{
		return (String) row.getPropertyIdentificationSet(0).getAtribute("ParcelID");
	}

	/**
	 * Score response for parcel number.
	 * 
	 * @see ro.cst.tsearch.search.filter.FilterResponse#getScoreOneRow(ro.cst.tsearch.servers.response.ParsedResponse)
	 */
	public BigDecimal getScoreOneRow(ParsedResponse row) 
	{
		
		BigDecimal parcelNumberScore = 
			(MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT, parcelNumber, getCandidateParcelNumber(row),searchId)).getScore();

		if (cleanParcelNumber){
			parcelNumberScore = 
				(MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT, parcelNumber.replaceAll("\\p{Punct}", "").replaceAll("\\s+", ""), 
												getCandidateParcelNumber(row).replaceAll("\\p{Punct}", "").replaceAll("\\s+", ""),searchId)).getScore();
		}
		loggerDetails.debug("match ["+parcelNumber+"] vs [" + getCandidateParcelNumber(row) + "] score=" + parcelNumberScore);
		
		return parcelNumberScore;	
	}
	
	@Override
	public String getFilterCriteria(){
    	return "Parcel Number = '" + parcelNumber + "'";
    }
}

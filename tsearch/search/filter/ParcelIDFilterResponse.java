package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;

import org.apache.log4j.Category;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * ParcelIDFilterResponse.
 */
@SuppressWarnings("serial")
public class ParcelIDFilterResponse extends FilterResponse {

	protected String[] rejectPatterns = new String[0];
		
	public void setRejectPatterns(String [] patterns){
		rejectPatterns = patterns;
	}

	protected boolean checkOnlyPatterns = false;
	
	public void setCheckOnlyPatterns(boolean checkOnlyPatterns){
		this.checkOnlyPatterns = checkOnlyPatterns; 
	}
	
	/**
	 * Main log category.
	 */
	protected static final Category logger = Category.getInstance(ParcelIDFilterResponse.class.getName());
	/**
	 * Details log category.
	 */
	protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + ParcelIDFilterResponse.class.getName());
	/**
	 * Parcel number.
	 */
	protected String parcelNumber;

	/**
	 * Constructor
	 * 
	 * @param instrumentlNumber Parcel number to be matched.
	 */
	public ParcelIDFilterResponse(String saKey,long searchId)
	{
		super(searchId);
		parcelNumber = getRefParcelNumber(saKey);
		
		threshold = new BigDecimal("0.80");
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
	 * 
	 * @return reference parcelID.
	 */
	protected String getRefParcelNumber(String saKey) {
		return sa.getAtribute(saKey);
	}

	/**
	 * Score response for parcel number.
	 * 
	 * @see ro.cst.tsearch.search.filter.FilterResponse#getScoreOneRow(ro.cst.tsearch.servers.response.ParsedResponse)
	 */
	public BigDecimal getScoreOneRow(ParsedResponse row) 
	{
		BigDecimal score = getScoreOneRowInternal(row);
		loggerDetails.debug("match ["+parcelNumber+"] vs [" + getCandidateParcelNumber(row) + "] score=" + score);		
        IndividualLogger.info( "match ["+parcelNumber+"] vs [" + getCandidateParcelNumber(row) + "] score=" + score,searchId );
		return score;
	}
	
	protected BigDecimal getScoreOneRowInternal(ParsedResponse row) 
	{
		// check patterns
		if(rejectPatterns != null && rejectPatterns.length >0){
			String cand = getCandidateParcelNumber(row);		
			boolean rejected = false;
			for(String pattern: rejectPatterns){
				if(cand.matches(pattern)){
					rejected = true;
					break;
				}					
			}
			if(rejected){
				return ATSDecimalNumberFormat.ZERO;
			}
			if(checkOnlyPatterns){
				return ATSDecimalNumberFormat.ONE;
			}
		}
		
		//no reference
		if(StringUtils.isStringBlank(parcelNumber)){
			return ATSDecimalNumberFormat.ONE;
		}	

		// compute score without prefix
		String cand1 = getCandidateParcelNumber(row); 
		BigDecimal score1 = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_IGNORE_DIFF_NO_TOKENS, parcelNumber, cand1, searchId)).getScore();
		if(score1.compareTo(ATSDecimalNumberFormat.ONE) ==0){
			return score1;
		}
		
		// try to add prefix
		String cand2 = sa.getAtribute(SearchAttributes.LD_PARCELNO_PREFIX) + cand1;
		if(cand2.equals(cand1)){
			return score1;
		} 
		
		// compute score with prefix
		BigDecimal score2 = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_IGNORE_DIFF_NO_TOKENS, parcelNumber, cand2, searchId)).getScore();
		return score1.max(score2);
	}

	@Override
    public String getFilterName(){
    	return "Filter by PIN";
    }
	
	@Override
	public String getFilterCriteria(){
		String retVal = "";
		if(rejectPatterns != null && rejectPatterns.length > 0){
			retVal += " Reject '";
			for(String pattern : rejectPatterns){
				retVal += pattern + " ";
			}
			retVal = retVal.trim() + "'";
		}
		if(!checkOnlyPatterns){
			 retVal = "PIN='" + parcelNumber + "'" + retVal;
		}
		return retVal;
	}

}

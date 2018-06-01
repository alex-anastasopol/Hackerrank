package ro.cst.tsearch.search.filter.fornext;
import java.math.BigDecimal;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;
/**
 * ScoreFilterResponse.
 */
public class ScoreFilterResponse extends FilterResponse
{
	protected static final Category logger =
		Category.getInstance(ScoreFilterResponse.class.getName());
	protected static final Category loggerDetails =
		Category.getInstance(
			Log.DETAILS_PREFIX + ScoreFilterResponse.class.getName());
	public ScoreFilterResponse(long searchId)
	{
		super(searchId);
		this.strategyType = STRATEGY_TYPE_BEST_RESULTS;
	}
	public BigDecimal getScoreOneRow(ParsedResponse row)
	{
		String s ="";
			try{
			s=(String)
				(
					(PropertyIdentificationSet)row
						.getPropertyIdentificationSet()
						.get(
						0)).getAtribute(
				"Score");
			}
		catch(Exception e){
			
		}
		s = (StringUtils.isStringBlank(s) ? "0.0" : s);
		double sc = 0.0;
		try
		{
			sc = (double)Double.parseDouble(s.trim()) / 100.0;
		}
		catch (Exception e)
		{
			sc = 0.0;
		}
		if (logger.isDebugEnabled())
			logger.debug("Parsed score =[" + sc + "]");
		return new BigDecimal(sc);
	}
	protected void analyzeResult(ServerResponse sr, Vector rez)
	throws ServerResponseException
	{
	    if (strategyType == STRATEGY_TYPE_BEST_RESULTS) {
	    	
	    	for (int i = 0; i < rez.size(); i++)
			{
				String row = ((ParsedResponse)rez.elementAt(i)).getResponse();
				if (bestScore.compareTo((BigDecimal)scores.get(row)) != 0)
				{
					sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST);
				}
			}
	    	
            /*if ( rez.size() >= 1 && bestScore.compareTo(new BigDecimal("0.40")) < 0 ) {
                sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST);
            }*/
            
        } else {
            return;
        }
    }

}

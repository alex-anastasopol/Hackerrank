/*
 * Created on Jun 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter.fornext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DesotoRegSubdivNameFilterResponseForNext extends FilterResponse{
	protected static final Category logger = Category.getInstance(DesotoRegSubdivNameFilterResponseForNext.class.getName());
	protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + DesotoRegSubdivNameFilterResponseForNext.class.getName());
	
	private String refSubdiv ="";

	public DesotoRegSubdivNameFilterResponseForNext(String refSubdiv,long searchId){
		super(searchId);
		setForNext(true);
		List tokens = StringUtils.splitString(refSubdiv);
		if (tokens.size()>0){
			String last = (String) tokens.get(tokens.size()-1);
			if ((tokens.size()>1)&&(last.length()<3)){//daca am cel putin doi 
				//tokeni din care ultimul este initiala
				//pot sa renunt la ultimul pt matchuire
				refSubdiv = StringUtils.join(tokens.subList(0, tokens.size()-1), " "); 
			}
		}
		this.refSubdiv=refSubdiv;
		threshold = new BigDecimal("0.90");
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		String g = row.getSaleDataSet(0).getAtribute("Grantee");
		if (StringUtils.isEmpty(g)){
			g = row.getSaleDataSet(0).getAtribute("Grantor");
		}
		
		BigDecimal score =  getScoreOneSubdiv(g);
		
		return score;
	}


	private BigDecimal getScoreOneSubdiv(String candSubdiv) {
		
		MatchAlgorithm matcher = MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_REGISTER_NAME_NA, refSubdiv, StringFormats.SubdivisionNashvilleAO(candSubdiv),searchId);
		matcher.setPenilizeDiffNoTokensFlag(false);
		BigDecimal score = matcher.getScore();
		return score;
	}

	@Override
	public String getFilterCriteria() {
		return "Subdivision name = " + refSubdiv;
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

	
}

/*
 * Created on Jun 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
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
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DesotoRegBookPageFilterResponseForNext extends FilterResponse{
	protected static final Category logger = Category.getInstance(DesotoRegBookPageFilterResponseForNext.class.getName());
	protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + DesotoRegBookPageFilterResponseForNext.class.getName());
	
	
	private String book ="";
	private String page ="";
	private boolean disableNextLinkOnFilteredResults = false;
    
    public DesotoRegBookPageFilterResponseForNext(String book, String page, boolean disableNextLinkOnFilteredResults,long searchId){
        super(searchId);
    	this.book = book;
        this.page = page;
        this.disableNextLinkOnFilteredResults = disableNextLinkOnFilteredResults;
        threshold = new BigDecimal("0.99");        
    }
    
	public DesotoRegBookPageFilterResponseForNext(String book, String page,long searchId){
		super(searchId);
		this.book = book;
		this.page = page;
		threshold = new BigDecimal("0.99");
	}

	public BigDecimal getScoreOneRow(ParsedResponse row) {
		
		String candBook = (String) row.getSaleDataSet(0).getAtribute("Book");
		String candPage = (String) row.getSaleDataSet(0).getAtribute("Page");

		BigDecimal scoreBook = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_LOT , book, candBook,searchId)).getScore();
		BigDecimal scorePage = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_LOT , page, candPage,searchId)).getScore();

		loggerDetails.debug(" score match [" +	book +	"] vs [" +	candBook + "]= " + scoreBook);
		loggerDetails.debug(" score match [" +	page +	"] vs [" +	candPage + "]= " + scorePage);
		
		return scoreBook.min( scorePage);
	}

    protected void analyzeResult(ServerResponse sr, Vector rez) throws ServerResponseException
    {
        if( disableNextLinkOnFilteredResults )
        {
            if( rez.size() != sr.getParsedResponse().getInitialResultsCount() )
            {
                //filtered --> stop, do not go to next results
                sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST);
            }
        }
    }
    
    @Override
    public String getFilterCriteria(){
    	return "book = " + book + ", page = " + page;
    }
	
}

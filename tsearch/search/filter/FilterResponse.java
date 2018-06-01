package ro.cst.tsearch.search.filter;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;
import static ro.cst.tsearch.utils.StringUtils.matchPatterns;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.monitor.FilterResponseTime;
import ro.cst.tsearch.propertyInformation.Family;
import ro.cst.tsearch.search.filter.fornext.DesotoRegBookPageFilterResponseForNext;
import ro.cst.tsearch.search.filter.fornext.DesotoRegSubdivCodeLotFilterResponseForNext;
import ro.cst.tsearch.search.filter.fornext.DesotoRegSubdivNameFilterResponseForNext;
import ro.cst.tsearch.search.filter.fornext.InstrumentNumberFilterResponse;
import ro.cst.tsearch.search.filter.fornext.ParcelNumberFilterResponseForNext;
import ro.cst.tsearch.search.filter.fornext.RegisterNameFilterResponseForNext;
import ro.cst.tsearch.search.filter.fornext.ScoreFilterResponse;
import ro.cst.tsearch.search.filter.fornext.SubdivNameAndLotForNext;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.search.filter.matchers.name.RegisterNameMatcher;
import ro.cst.tsearch.search.filter.matchers.subdiv.SubdivMatcher;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterForNext;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterResponse2;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.filter.newfilters.name.TransferNameFilter;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.SearchLogger;

/**
 * @author elmarie
 */
public class FilterResponse implements Serializable {
	
	static final long serialVersionUID = 10000000;
	protected static final Category logger =
		Logger.getLogger(FilterResponse.class);
	private static final Category loggerDetails =
		Logger.getLogger(Log.DETAILS_PREFIX + FilterResponse.class.getName());
	public static final int TYPE_DEFAULT = -1;
	public static final int TYPE_REGISTER_NAME = 1;
	public static final int TYPE_ASSESSOR_ADDRESS = 3;
	public static final int TYPE_ASSESSOR_ADDRESS_HIGH_PASS = 7;
	public static final int TYPE_BALDWIN_ADDRESS_FOR_NEXT = 8;
	public static final int TYPE_ASSESSOR_ADDRESS2 = 9;
	public static final int TYPE_ASSESSOR_ADDRESS2_HIGH_PASS = 11;
	public static final int TYPE_ASSESSOR_PARCEL_ID = 12;
	public static final int TYPE_ASSESSOR_PARCEL_ID_ADDRESS = 13;
	public static final int TYPE_ASSESSOR_PARCEL_ID_ADDRESS_BEST_RESULTS = 16;
	public static final int TYPE_ADDRESS_FOR_NEXT = 22;
	public static final int TYPE_PARCEL_FOR_NEXT = 23;
	public static final int TYPE_CLEANED_PARCEL_FOR_NEXT = 24;//without spaces or/and punctuation signs
	public static final int TYPE_BOOK_PAGE_FOR_NEXT = 32;
	public static final int TYPE_INSTRUMENT_FOR_NEXT = 33;
	public static final int TYPE_SUBDIV_NAME_FOR_NEXT = 34;
	public static final int TYPE_SUBDIV_CODE_FOR_NEXT = 35;
	public static final int TYPE_ASSESSOR_SUBDIV_NAME_AND_LOT_FOR_NEXT = 36;
	public static final int TYPE_SCORE_FOR_NEXT = 37;
    public static final int TYPE_REGISTER_NAME_FOR_NEXT = 45;
    public static final int TYPE_BOOK_PAGE_FOR_NEXT_CROSREF = 47;
    public static final int TYPE_REGISTER_NAME_FOR_NEXT_OH_FRANKLIN_PR = 48;
    
    public static final int TYPE_LEGAL = 60;
    public static final int TYPE_REJECT_ALREADY_PRESENT = 61;
        
    public static final int TYPE_ASSESSOR_CITY = 63;					//B1594
    
    public static final int TYPE_REJECT_NON_REALESTATE = 66;

    public static final int  TYPE_REGISTER_NAME_MO_JACKSON = 67; 
    
    public static final int  TYPE_NAME = 69;
    public static final int TYPE_LEGAL_WITHOUT_PLAT = 71;
    public static final int  TYPE_NAME_FOR_GRANTTE_IS_SUBDIVISION_SMALL_THRESHOLD =72;
    
    public static final int TYPE_MOCLAYTR_PARCEL_ID = 73;
    public static final int TYPE_TAX_YEAR = 74;
    public static final int TYPE_CITY = 75;
    
    public static final int TYPE_ADDRESS_REGISTER = 76;
    public static final int TYPE_DOCTYPE_BUYER = 77;
    public static final int TYPE_DOCTYPE_SUBDIVISION_IS_GRANTOR_OR_GRANTEE = 78;
    public static final int TYPE_REJECT_NON_UNIQUE = 79;
    public static final int TYPE_ASSESSOR_PARCEL_ID_ADDRESS_HIBRID = 80;
    public static final int TYPE_NAME_OF_SUBDIVISION_SMALL_THRESHOLD = 81;
    public static final int TYPE_TRANSFER_NAME = 82;
    
    public static final int TYPE_REJECT_ALREADY_PRESENT_MIOAKLAND = 84;
    
    
    public static final int TYPE_MULTIPLE_PIN = 87;
    public static final int TYPE_DATEFILTERFORGOBACK = 88;
    public static final int TYPE_LEGAL_SECTION = 89;
    public static final int TYPE_BOOK_PAGE_FOR_NEXT_GENERIC = 90;
    public static final int TYPE_INSTRUMENT_FOR_NEXT_GENERIC = 91;
    public static final int TYPE_REGISTER_NAME_FOR_NEXT_IGNORE_MIDDLE = 92;
    
	public static final int STRATEGY_TYPE_BEST_RESULTS = 1;
	public static final int STRATEGY_TYPE_HIGH_PASS = 2;
	public static final int STRATEGY_TYPE_HYBRID = 3;
	public static final int STRATEGY_TYPE_LOW_PASS_FILTER = 4;
	
	
	protected SearchAttributes sa;
	protected String saKey = SearchAttributes.NO_KEY;
	protected Map scores = new HashMap();
	protected BigDecimal bestScore = ATSDecimalNumberFormat.ZERO;
	protected List invalidPatterns = new ArrayList();
	protected BigDecimal threshold = ATSDecimalNumberFormat.ZERO;
	protected int strategyType = STRATEGY_TYPE_HIGH_PASS;
	protected Vector skippedRez = new Vector();
	protected int filterForNextFollowLinkLimit = -1;
    
    protected int stringCleanerId = -1;
    protected long searchId = -1;
    private boolean saveInvalidatedInstruments = false;
    
    private transient Set<String> modulesAppliedFor = new HashSet<String>();
	
	public FilterResponse(long searchId)
	{
		this.searchId = searchId;
		try{
			if( searchId > 0) {
				this.sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
			}
		
		}
		catch(Exception e){
			
		}
		this.saKey = SearchAttributes.NO_KEY;
	}
	public FilterResponse(String key,long searchId)
	{
		this(searchId);
		this.searchId = searchId;
		this.saKey = key;
	}
	public FilterResponse(SearchAttributes sa1, String key,long searchId)
	{
		this.searchId = searchId;
		this.sa = sa1;
		this.saKey = key;
	}
	public void filterResponse(ServerResponse sr)
		throws ServerResponseException
	{
		loggerDetails.debug("Start filter.");
		long t0 = System.currentTimeMillis();		
		ParsedResponse pr = sr.getParsedResponse();
		Vector rows = pr.getResultRows();
        pr.setInitialResultsCount( rows.size() );
		if (rows.size() == 0)
		{
			return;
		}
		try {
			addInModulesAppliedFor(getSearch().getSearchRecord().getModule().getUniqueKey());
		} catch(Exception e) {
			logger.error("Cannot addInModulesAppliedFor", e);
		}
		if(isInitAgain()){
			init();
		}				
		computeScores(rows);
				
		Vector rez = filterRows(rows);
		
		if(!this.getClass().getSimpleName().equals("FilterResponse") ){
			sr.setFiltered(true);
		}

		pr.setResultRows(rez);
		pr.setSkippedRows(skippedRez);
		
	
		analyzeResult(sr, rez);
		long t1 = System.currentTimeMillis();
		loggerDetails.debug("Stop filter: " + (t1 - t0) + " ms.");
		FilterResponseTime.update(t1 - t0);
	}
	
	/* (non-Javadoc)
	 * @see ro.cst.tsearch.search.filter.FilterResponseInterface#computeScores(java.util.Vector)
	 */
	public void computeScores(Vector rows)
	{
		for (int i = 0; i < rows.size(); i++)
		{
            IndividualLogger.info("Processing result " + i + " of total " + rows.size(),searchId);
			ParsedResponse row = (ParsedResponse)rows.get(i);
			BigDecimal score = null;
			if(rows.size() == 1 && isSkipUnique()){
				score = ATSDecimalNumberFormat.ONE;
			} else if(rows.size() > getMinRowsToActivate()){
				score = getScoreOneRow(row);
			} else {
				score = ATSDecimalNumberFormat.ONE;
			}
			scores.put(row.getResponse(), score);
			if (score.compareTo(bestScore) > 0)
			{
				bestScore = score;
			}
			IndividualLogger.info("ROW SCORE:" + score,searchId);
			//logger.debug("\n\n ROW SCORE : [" + score + "]\nROW HTML: [" + row.getResponse() + "]\n");
		}
	}
	
	@SuppressWarnings("unchecked")
	protected Vector filterRows(Vector rows) {
		if(strategyType == STRATEGY_TYPE_HYBRID){
			return getHybridResults(rows, threshold);
		} else if (strategyType == STRATEGY_TYPE_BEST_RESULTS){
			return getBestResults(rows);
		} else  if (strategyType == STRATEGY_TYPE_LOW_PASS_FILTER) {
			return lowPassFilter(rows,threshold);
		}
		else {
			return highPassFilter(rows, threshold);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected Vector getHybridResults(Vector rows, BigDecimal _threshold){
		
		String id = System.nanoTime() + "_filter";
		StringBuilder toLog = new StringBuilder();
//		SearchLogger.info("<div id=\"" + id + "\">", searchId);
		toLog.append("<div id=\"").append(id).append("\">");
		
		IndividualLogger.info( "getHybridResults filter strategy", searchId );
        if( _threshold.floatValue() > 0 )
        {
            IndividualLogger.info( "Eliminating rows with score < " + _threshold,searchId );
        }
        
        toLog.append(
				"<br/><span class='filter'>"
						+ /*getFilterName()*/ "Filter" + (isForNext()?" for next":"")
						+ "</span> by <span class='criteria'>"
						+ getFormattedFilterCriteria()
						+ "</span> on <span class='number'>" + rows.size()
						+ "</span> results, retaining all documents with the highest score between all with scores greater(or equals) than <span class='number'>"
						+ ATSDecimalNumberFormat.format(_threshold)
						+ "</span> :");
        
//        SearchLogger.info(
//				"<br/><span class='filter'>"
//						+ /*getFilterName()*/ "Filter" + (isForNext()?" for next":"")
//						+ "</span> by <span class='criteria'>"
//						+ getFormattedFilterCriteria()
//						+ "</span> on <span class='number'>" + rows.size()
//						+ "</span> results, retaining all documents with the highest score between all with scores greater(or equals) than <span class='number'>"
//						+ ATSDecimalNumberFormat.format(_threshold)
//						+ "</span> :", searchId);        	

        //SearchLogger.info(createCollapsibleHeader(), searchId);
        toLog.append("<div>");
        //SearchLogger.info("<div>", searchId);
        toLog.append("<table border='1' cellspacing='0' width='99%'>" +
        		"<tr><th>No</th><th>Score</th>" +
        		"<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th>" + 
        		"</tr>");
//        SearchLogger.info("<table border='1' cellspacing='0' width='99%'>" +
//        		"<tr><th>No</th><th>Score</th>" +
//        		"<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th>" + 
//        		"</tr>", searchId);

            
		Vector rez = new Vector();
		boolean hasRejected = false;
		boolean hasPassed = false;
		for (int i = 0; i < rows.size(); i++)
		{
			ParsedResponse pr = ((ParsedResponse)rows.elementAt(i));
			String row = pr.getResponse();
			BigDecimal score = (BigDecimal)scores.get(row);
			if ( bestScore.compareTo(score) == 0 && bestScore.compareTo(_threshold) >= 0){
				rez.add(rows.elementAt(i));
                IndividualLogger.info( "Result OK - " + row ,searchId);
            	toLog.append(logRow(i, pr, (BigDecimal)scores.get(row), true, searchId));
            	hasPassed = true;
			} else {
			    skippedRez.add(rows.elementAt(i));
                IndividualLogger.info( "Result skipped - " + row,searchId );
                toLog.append(logRow(i, pr, (BigDecimal)scores.get(row), false, searchId));
            	hasRejected = true;
			}
		}
		toLog.append("</table></div><span class='filter'>Status:</span> retained <span class='number'>"
						+ rez.size()
						+ "</span> best matches with score = <span class='number'>"
						+ ATSDecimalNumberFormat.format(bestScore) + "</span> and score greater than "
						+ "<span class='number'>" + ATSDecimalNumberFormat.format(_threshold) + "</span>"
						+ "</span> from a total of <span class='number'>"
						+ rows.size() + "</span> results.<br/>");
//		SearchLogger.info(
//				"</table></div><span class='filter'>Status:</span> retained <span class='number'>"
//						+ rez.size()
//						+ "</span> best matches with score = <span class='number'>"
//						+ ATSDecimalNumberFormat.format(bestScore) + "</span> and score greater than "
//						+ "<span class='number'>" + ATSDecimalNumberFormat.format(_threshold) + "</span>"
//						+ "</span> from a total of <span class='number'>"
//						+ rows.size() + "</span> results.<br/>", searchId);
		

		String newId = id + (hasRejected ? "_rejected" : "_passed");
		String style = hasRejected ? "" : "none";
		toLog.append("</div><script language=\"JavaScript\">var dv=document.getElementById(\"" + id + "\"); dv.id = \"" + newId + "\"; dv.style.display=\"" + style + "\";</script>");
//		SearchLogger.info("</div><script language=\"JavaScript\">var dv=document.getElementById(\"" + id + "\"); dv.id = \"" + newId + "\"; dv.style.display=\"" + style + "\";</script>", searchId);
		
		SearchLogger.info(toLog.toString(), searchId);
		
		return rez;
	}
	
	@SuppressWarnings("unchecked")
	protected Vector getBestResults(Vector rows)
	{
		String id = System.nanoTime() + "_filter";
		StringBuilder toLog = new StringBuilder();
        toLog.append("<div id=\"").append(id).append("\">");
//		SearchLogger.info("<div id=\"" + id + "\">", searchId);
		
        IndividualLogger.info( "getBestResults filter strategy",searchId );
        
        toLog.append("<br/><span class='filter'>Filter").append((isForNext()?" for next":""))
        	.append("</span> by <span class='criteria'>")
        	.append(getFormattedFilterCriteria())
        	.append("</span> on <span class='number'>")
        	.append(rows.size())
        	.append("</span> results, retaining only best matches:");
//        SearchLogger.info(
//        		"<br/><span class='filter'>"
//				+ /*getFilterName()*/ "Filter" + (isForNext()?" for next":"")
//				+ "</span> by <span class='criteria'>"
//				+ getFormattedFilterCriteria() + "</span> on <span class='number'>" + rows.size() + "</span> results, retaining only best matches:",
//				searchId);        	

        //SearchLogger.info(createCollapsibleHeader(), searchId);
        toLog.append("<div>");
//        SearchLogger.info("<div>", searchId);
        toLog.append("<table border='1' cellspacing='0' width='99%'>")
        	.append("<tr><th>No</th><th>Score</th>")
        	.append("<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th></tr>");
//        SearchLogger.info("<table border='1' cellspacing='0' width='99%'>" +
//        		"<tr><th>No</th><th>Score</th>" +
//        		"<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th>" + 
//        		"</tr>", searchId);
        
		Vector rez = new Vector();
		boolean hasRejected = false;
		boolean hasPassed = false;
		if (bestScore.compareTo(ATSDecimalNumberFormat.ZERO) > 0)
		{
			for (int i = 0; i < rows.size(); i++)
			{
				ParsedResponse pr = ((ParsedResponse)rows.elementAt(i));
				String row = pr.getResponse();
				if (bestScore.compareTo((BigDecimal)scores.get(row)) == 0)
				{
					rez.add(rows.elementAt(i));
                    IndividualLogger.info( "Result OK - " + row ,searchId);
	            	toLog.append(logRow(i, pr, (BigDecimal)scores.get(row), true, searchId));
	            	hasPassed = true;
				}
				else
				{
				    skippedRez.add(rows.elementAt(i));
                    IndividualLogger.info( "Result skipped - " + row, searchId );
                    toLog.append(logRow(i, pr, (BigDecimal)scores.get(row), false, searchId));
	            	hasRejected = true;
				}
				
			}
		}
		toLog.append("</table></div><span class='filter'>Status:</span> retained <span class='number'>")
			.append(rez.size())
			.append("</span> best matches with score = <span class='number'>")
			.append(ATSDecimalNumberFormat.format(bestScore))
			.append("</span> from a total of <span class='number'>")
			.append(rows.size())
			.append("</span> results.<br/>");
//		SearchLogger.info(
//			"</table></div><span class='filter'>Status:</span> retained <span class='number'>"
//					+ rez.size()
//					+ "</span> best matches with score = <span class='number'>"
//					+ ATSDecimalNumberFormat.format(bestScore)
//					+ "</span> from a total of <span class='number'>"
//					+ rows.size() + "</span> results.<br/>", searchId);
		
		String newId = id + (hasRejected ? "_rejected" : "_passed");
		String style = hasRejected ? "" : "none";
		toLog.append("</div><script language=\"JavaScript\">var dv=document.getElementById(\"" + id + "\"); dv.id = \"" + newId + "\"; dv.style.display=\"" + style + "\";</script>");
//		SearchLogger.info("</div><script language=\"JavaScript\">var dv=document.getElementById(\"" + id + "\"); dv.id = \"" + newId + "\"; dv.style.display=\"" + style + "\";</script>", searchId);
		SearchLogger.info(toLog.toString(), searchId);
		return rez;
	}
	@SuppressWarnings("unchecked")
	protected Vector highPassFilter(Vector rows, BigDecimal _threshold)
	{
		Vector rez = new Vector();        
        
        if( _threshold.floatValue() > 0 )
        {
            IndividualLogger.info( "Eliminating rows with score < " + _threshold,searchId );
        }
        
        // do not log the default filter - it doesn't do anything interesting and it is called every time
        boolean logIt = !getClass().getSimpleName().equals("FilterResponse");
        String id = "";
        StringBuilder toLog = new StringBuilder();
        if(logIt){    		
        	id = System.nanoTime() + "_filter";
        	toLog.append("<div id=\"").append(id).append("\">");
//    		SearchLogger.info("<div id=\"" + id + "\">", searchId);
    		
    		if(_threshold.compareTo(ATSDecimalNumberFormat.ONE) == 0) {
    			toLog.append("<br/><span class='filter'>Filter").append((isForNext()?" for next":""))
    				.append("</span> by <span class='criteria'>")
    				.append(getFormattedFilterCriteria())
    				.append("</span> on <span class='number'>").append(rows.size())
    				.append("</span> results, retaining only scores equal to <span class='number'>")
    				.append(ATSDecimalNumberFormat.format(_threshold)).append("</span> :");
//	            SearchLogger.info(
//					"<br/><span class='filter'>"
//	    					+ /*getFilterName()*/ "Filter" + (isForNext()?" for next":"")
//							+ "</span> by <span class='criteria'>"
//							+ getFormattedFilterCriteria()
//							+ "</span> on <span class='number'>" + rows.size()
//							+ "</span> results, retaining only scores equal to <span class='number'>"
//							+ ATSDecimalNumberFormat.format(_threshold)
//							+ "</span> :", searchId);        	
    		} else {
    			toLog.append("<br/><span class='filter'>Filter").append((isForNext()?" for next":""))
    				.append("</span> by <span class='criteria'>")
    				.append(getFormattedFilterCriteria())
    				.append("</span> on <span class='number'>").append(rows.size())
    				.append("</span> results, retaining only scores greater than or equal to <span class='number'>")
    				.append(ATSDecimalNumberFormat.format(_threshold)).append("</span> :");
//    			SearchLogger.info(
//    					"<br/><span class='filter'>"
//    	    					+ /*getFilterName()*/ "Filter" + (isForNext()?" for next":"")
//    							+ "</span> by <span class='criteria'>"
//    							+ getFormattedFilterCriteria()
//    							+ "</span> on <span class='number'>" + rows.size()
//    							+ "</span> results, retaining only scores greater than or equal to <span class='number'>"
//    							+ ATSDecimalNumberFormat.format(_threshold)
//    							+ "</span> :", searchId);        
    		}
            
            //SearchLogger.info(createCollapsibleHeader(), searchId);
    		toLog.append("<div>");
//            SearchLogger.info("<div>", searchId);
    		toLog.append("<table border='1' cellspacing='0' width='99%'>")
    			.append("<tr><th>No</th><th>Score</th>")
    			.append("<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th></tr>");
//            SearchLogger.info("<table border='1' cellspacing='0' width='99%'>" +
//            		"<tr><th>No</th><th>Score</th>" +
//            		"<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th>" + 
//            		"</tr>", searchId);
                   	
        }
		boolean hasRejected = false;
		boolean hasPassed = false;
		for (int i = 0, no = rows.size(); i < no; i++)
		{
			ParsedResponse pr = (ParsedResponse)rows.get(i);
			String row = pr.getResponse();
			if (_threshold.compareTo((BigDecimal)scores.get(row)) <= 0)
			{
				rez.add(rows.elementAt(i));
				if(logIt){ 
					toLog.append(logRow(i, pr, (BigDecimal)scores.get(row), true, searchId)); 
				}
				hasPassed = true;
			}
            else
            {
                IndividualLogger.info( "Row eliminated - score = " + (BigDecimal)scores.get(row) + " parsed response = |" + row + "|",searchId );
                if(logIt){ 
                	toLog.append(logRow(i, pr, (BigDecimal)scores.get(row), false, searchId));
                }
                hasRejected = true;
            }
		}
		if(logIt){
			if(_threshold.compareTo(ATSDecimalNumberFormat.ONE) == 0) {
				toLog.append("</table></div><span class='filter'>Filter Status:</span> retained <span class='number'>")
					.append(rez.size())
					.append("</span> documents with score equal to <span class='number'>")
					.append(ATSDecimalNumberFormat.format( _threshold))
					.append("</span> from a total of <span class='number'>")
					.append(rows.size()).append("</span> results.<br/>");
//				SearchLogger.info(
//						"</table></div><span class='filter'>Filter Status:</span> retained <span class='number'>"
//								+ rez.size()
//								+ "</span> documents with score equal to <span class='number'>"
//								+ ATSDecimalNumberFormat.format( _threshold)
//								+ "</span> from a total of <span class='number'>"
//								+ rows.size() + "</span> results.<br/>", searchId);
			} else {
				toLog.append("</table></div><span class='filter'>Filter Status:</span> retained <span class='number'>")
					.append(rez.size())
					.append("</span> documents with score greater than or equal to <span class='number'>")
					.append(ATSDecimalNumberFormat.format( _threshold))
					.append("</span> from a total of <span class='number'>")
					.append(rows.size()).append("</span> results.<br/>");
//				SearchLogger.info(
//						"</table></div><span class='filter'>Filter Status:</span> retained <span class='number'>"
//								+ rez.size()
//								+ "</span> documents with score greater than or equal to <span class='number'>"
//								+ ATSDecimalNumberFormat.format( _threshold)
//								+ "</span> from a total of <span class='number'>"
//								+ rows.size() + "</span> results.<br/>", searchId);
			}
		}
		

		if(logIt){
			String newId = id + (hasRejected ? "_rejected" : "_passed");
			String style = hasRejected ? "" : "none";
			toLog.append("</div><script language=\"JavaScript\">var dv=document.getElementById(\"")
				.append(id).append("\"); dv.id = \"").append(newId).append("\"; dv.style.display=\"")
				.append(style).append("\";</script>");
//			SearchLogger.info("</div><script language=\"JavaScript\">var dv=document.getElementById(\"" + id + "\"); dv.id = \"" + newId + "\"; dv.style.display=\"" + style + "\";</script>", searchId);
			SearchLogger.info(toLog.toString(), searchId);
		}
		return rez;
	}
	protected Vector lowPassFilter(Vector rows, BigDecimal _threshold)
	{
		Vector rez = new Vector();        
        
        if( _threshold.floatValue() > 0 )
        {
            IndividualLogger.info( "Eliminating rows with score > " + _threshold,searchId );
        }
        
        // do not log the default filter - it doesn't do anything interesting and it is called every time
        boolean logIt = !getClass().getSimpleName().equals("FilterResponse");
        String id = "";
        StringBuilder toLog = new StringBuilder();
        if(logIt){        
    		id = System.nanoTime() + "_filter";
    		toLog.append("<div id=\"").append(id).append("\">");
//    		SearchLogger.info("<div id=\"" + id + "\">", searchId);
    		toLog.append("<br/><span class='filter'>Filter").append((isForNext()?" for next":""))
    			.append("</span> by <span class='criteria'>")
    			.append(getFormattedFilterCriteria())
    			.append("</span> on <span class='number'>").append(rows.size())
    			.append("</span> results, retaining only scores lesser than <span class='number'>")
    			.append(ATSDecimalNumberFormat.format(_threshold)).append("</span> :");
//            SearchLogger.info(
//				"<br/><span class='filter'>"
//    					+ /*getFilterName()*/ "Filter" + (isForNext()?" for next":"")
//						+ "</span> by <span class='criteria'>"
//						+ getFormattedFilterCriteria()
//						+ "</span> on <span class='number'>" + rows.size()
//						+ "</span> results, retaining only scores lesser than <span class='number'>"
//						+ ATSDecimalNumberFormat.format(_threshold)
//						+ "</span> :", searchId);        	
            
            //SearchLogger.info(createCollapsibleHeader(), searchId);
    		toLog.append("<div>");
//            SearchLogger.info("<div>", searchId);
    		toLog.append("<table border='1' cellspacing='0' width='99%'>")
    			.append("<tr><th>No</th><th>Score</th>")
    			.append("<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th></tr>");
//            SearchLogger.info("<table border='1' cellspacing='0' width='99%'>" +
//            		"<tr><th>No</th><th>Score</th>" +
//            		"<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th>" + 
//            		"</tr>", searchId);
                   	
        }
		boolean hasRejected = false;
		boolean hasPassed = false;
		for (int i = 0, no = rows.size(); i < no; i++)
		{
			ParsedResponse pr = (ParsedResponse)rows.get(i);
			String row = pr.getResponse();
			if (_threshold.compareTo((BigDecimal)scores.get(row)) > 0)
			{
				rez.add(rows.elementAt(i));
				if(logIt){ 
					toLog.append(logRow(i, pr, (BigDecimal)scores.get(row), true, searchId)); 
				}		
				hasPassed = true;
			}
            else
            {
                IndividualLogger.info( "Row eliminated - score = " + (BigDecimal)scores.get(row) + " parsed response = |" + row + "|",searchId );
                if(logIt){ 
                	toLog.append(logRow(i, pr, (BigDecimal)scores.get(row), false, searchId)); 
                }
                hasRejected = true;
            }
		}
		if(logIt){
			toLog.append("</table></div><span class='filter'>Filter Status:</span> retained <span class='number'>")
				.append(rez.size())
				.append("</span> documents with score lesser than <span class='number'>")
				.append(ATSDecimalNumberFormat.format( _threshold))
				.append("</span> from a total of <span class='number'>")
				.append(rows.size()).append("</span> results.<br/>");
//			SearchLogger.info(
//					"</table></div><span class='filter'>Filter Status:</span> retained <span class='number'>"
//							+ rez.size()
//							+ "</span> documents with score lesser than <span class='number'>"
//							+ ATSDecimalNumberFormat.format( _threshold)
//							+ "</span> from a total of <span class='number'>"
//							+ rows.size() + "</span> results.<br/>", searchId);
			String newId = id + (hasRejected ? "_rejected" : "_passed");
			String style = hasRejected ? "" : "none";
			toLog.append("</div><script language=\"JavaScript\">var dv=document.getElementById(\"")
				.append(id).append("\"); dv.id = \"").append(newId)
				.append("\"; dv.style.display=\"").append(style).append("\";</script>");
//			SearchLogger.info("</div><script language=\"JavaScript\">var dv=document.getElementById(\"" + id + "\"); dv.id = \"" + newId + "\"; dv.style.display=\"" + style + "\";</script>", searchId);
			SearchLogger.info(toLog.toString(), searchId);
		}
		return rez;
	}
	protected void analyzeResult(ServerResponse sr, Vector rez)
		throws ServerResponseException
	{
	/*	if (strategyType == STRATEGY_TYPE_BEST_RESULTS)
		{
			if ((rez.size()	== 1) // daca am unic rezultat pe AO, atunci nu mai validez, 
			        				// il iau pe asta si trec mai departe
			  && (bestScore.compareTo(new BigDecimal("0.65")) < 0)
			 * 
			 
				)
			{
				bestScore = new BigDecimal("1.00");
				// setez rez unic cu scor maxim
				//   sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING);
				// throw new ServerResponseException(sr);               
			}
//			 am mai multe rezultate dar scoruri mai mici
			else if ((rez.size() > 1)
			    //&& (bestScore.compareTo(new BigDecimal("0.65")) < 0 ||
			      &&  bestScore.compareTo(new BigDecimal("0.65")) < 0)
			        //) 
			{
			   sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING);			  
			   throw new ServerResponseException(sr);    
			}
			
		}
		else
		{
			return;
		}*/
	    
	    // revenire la solutia initiala (vezi e-mail TM 17/05/2005) 
	    if (strategyType == STRATEGY_TYPE_BEST_RESULTS) {
            if (((rez.size() >1)&& (bestScore.compareTo(new BigDecimal("0.65")) < 0))||
            	((rez.size()==1)&& (bestScore.compareTo(new BigDecimal("0.5"))<0)) /*&&
                    (bestScore.compareTo(new BigDecimal("0.00")) > 0)*/) {
                
                sr.setBestScore(bestScore); // setez in ServerResponse scorul maxim
                sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING);
                throw new ServerResponseException(sr);
            }
        } /*else 
        if ((bestScore.compareTo(new BigDecimal("0.50")) < 0))
        {
            sr.setBestScore(bestScore); // setez in ServerResponse scorul maxim
            sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING);
            throw new ServerResponseException(sr);
        }    */    
        else
        {
            return;
        }
	}
	public static FilterResponse getInstance(int type, String saKey, SearchAttributes saForComparison, int cleanerId,long searchId, TSServerInfoModule module)
	{
        FilterResponse fr = null;
        
		switch (type)
		{
			
			case TYPE_ASSESSOR_PARCEL_ID_ADDRESS :
				{
					fr = new ParcelIDAddressFilterResponse(searchId);
				}
                break;
			case TYPE_ASSESSOR_PARCEL_ID_ADDRESS_BEST_RESULTS :
				{
					fr = new ParcelIDAddressFilterResponse(searchId);
					fr.setStrategyType(STRATEGY_TYPE_BEST_RESULTS);
					fr.setThreshold(new BigDecimal("0.65"));
				}
                break;
			case TYPE_ASSESSOR_PARCEL_ID_ADDRESS_HIBRID :
			{
				fr = new ParcelIDAddressFilterResponse(searchId);
				fr.setStrategyType(STRATEGY_TYPE_HYBRID);
				fr.setThreshold(new BigDecimal("0.9"));
			}
            break;
			case TYPE_ASSESSOR_PARCEL_ID :
				{
					fr = new ParcelIDFilterResponse(
						SearchAttributes.LD_PARCELNO,searchId);
				}
                break;
			case TYPE_MOCLAYTR_PARCEL_ID:
			{
				ParcelIDFilterResponse pf = new ParcelIDFilterResponse(SearchAttributes.LD_PARCELNO, searchId);
				pf.setCheckOnlyPatterns(true);
				pf.setRejectPatterns(new String[]{"00.*"});				
				fr = pf;
				break;
			}
			case TYPE_REGISTER_NAME_MO_JACKSON :
			{
				fr =
					new RegisterNameFilterResponse(
						saKey,
						new RegisterNameMatcher(searchId),searchId);
				fr.setThreshold(new BigDecimal("0.75"));
			}
            break;
            case TYPE_NAME:
            {
            	fr = new GenericNameFilter(saKey,searchId,false,module, true);
            	fr.setThreshold(new BigDecimal(0.93));
            }
            break;
            case TYPE_TRANSFER_NAME:
            {
            	fr = new TransferNameFilter(saKey,searchId,false,module);
            	fr.setStrategyType(STRATEGY_TYPE_LOW_PASS_FILTER);
            	fr.setThreshold(new BigDecimal(0.90));
            }
            break;
            case TYPE_NAME_OF_SUBDIVISION_SMALL_THRESHOLD:
            {
            	fr = new GenericNameFilter(saKey,searchId,true,module, false);
            	((GenericNameFilter)fr).setUseSubdivisionNameAsCandidat(true);
            	fr.setThreshold(new BigDecimal(0.65));
            }
            break;	
            case TYPE_NAME_FOR_GRANTTE_IS_SUBDIVISION_SMALL_THRESHOLD:
            {
            	fr = new GenericNameFilter(saKey,searchId,true,module, false);
            	fr.setThreshold(new BigDecimal(0.75));
            }
    		break;
			case TYPE_REGISTER_NAME :
				{
					fr = new RegisterNameFilterResponse(
						saKey,
						new RegisterNameMatcher(searchId),searchId);
				}
                break;
			case TYPE_ASSESSOR_ADDRESS :
			case TYPE_ASSESSOR_ADDRESS2 :
				{
					fr = new AddressFilterResponse2(saKey,searchId);
				}
                break;
			case TYPE_ASSESSOR_CITY :
				fr = new CityFilterResponse(saKey,searchId);
				fr.setThreshold(new BigDecimal("0.75"));
				fr.setStrategyType( FilterResponse.STRATEGY_TYPE_BEST_RESULTS );
				break;
			case TYPE_ASSESSOR_ADDRESS_HIGH_PASS :
			case TYPE_ASSESSOR_ADDRESS2_HIGH_PASS :
				{
					fr = new AddressFilterResponse2(saKey,searchId);
					fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
					fr.setThreshold(new BigDecimal("0.85"));
				}
                break;
			case TYPE_LEGAL:
			{
				fr = new GenericLegal(saKey, searchId);
				fr.setThreshold(new BigDecimal("0.7"));
			}
			break;
			case TYPE_LEGAL_SECTION:
			{				
				fr = new GenericLegal(saKey, searchId);				
				((GenericLegal)fr).disableAll();
				((GenericLegal)fr).setEnableSection(true);
				fr.setThreshold(new BigDecimal("0.7"));	
			}
			break;
			case TYPE_LEGAL_WITHOUT_PLAT:
			{
				fr = new GenericLegal(saKey, searchId);
				((GenericLegal)fr).setEnableSectionJustForUnplated(false);
				fr.setThreshold(new BigDecimal("0.7"));
				((GenericLegal)fr).setEnablePlatBook(false);
				((GenericLegal)fr).setEnablePlatPage(false);
			}
			break;
			case TYPE_REJECT_ALREADY_PRESENT:
			{
				fr = new RejectAlreadyPresentFilterResponse(saKey, searchId);
				fr.setThreshold(new BigDecimal("0.95"));
			}
			break;
			case TYPE_REJECT_ALREADY_PRESENT_MIOAKLAND:
			{
				RejectAlreadyPresentFilterResponse filter = new RejectAlreadyPresentFilterResponse(saKey, searchId);
				filter.setUseInstr(false); // instrument alone is not enough				
				fr = filter;
				fr.setThreshold(new BigDecimal("0.95"));				
			}
			break;
			case TYPE_REJECT_NON_REALESTATE:
			{
				fr = new RejectNonRealEstate(saKey, searchId);
				fr.setThreshold(new BigDecimal("0.65"));
			}
			break;
			case TYPE_TAX_YEAR:
			{
				fr =  new TaxYearFilterResponse(searchId); 
				fr.setThreshold(new BigDecimal("0.95"));
			}
			break;
			case TYPE_CITY:
			{
				fr =  new CityFilterResponse(searchId); 
				fr.setThreshold(new BigDecimal("0.60"));			
			}
			break;
			case TYPE_ADDRESS_REGISTER:
			{
				fr = new AddressFilterResponse2(saKey,searchId);
				fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
				fr.setThreshold(new BigDecimal("0.8"));
			}
			break;
			case TYPE_DOCTYPE_BUYER:
			{
				fr = new DocTypeSimpleFilter(searchId);
				fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
				fr.setThreshold(new BigDecimal("0.8"));
			}
			break;
			case TYPE_DOCTYPE_SUBDIVISION_IS_GRANTOR_OR_GRANTEE:
			{
				fr = new DocTypeSimpleFilter(searchId);
				String []docTypes = { "RESTRICTION"	,	"PLAT",		"EASEMENT" ,"CCER" };
				((DocTypeSimpleFilter)fr).setDocTypes(docTypes);
				fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
				fr.setThreshold(new BigDecimal("0.8"));
			}
			break;
			case TYPE_REJECT_NON_UNIQUE:
			{
				fr = new RejectNonUniqueFilterResponse(searchId);				
			}
			break;
			case TYPE_MULTIPLE_PIN:
			{
				fr = new MultiplePinFilterResponse(searchId);
			}
			break;
			default :
				{
					fr = new FilterResponse(saKey,searchId);
				}
            break;
		}
        
        if( saForComparison != null )
        {
            fr.sa = saForComparison;
        }
        
        fr.stringCleanerId = cleanerId;
        
        return fr;
	}

	
	public static FilterResponse getInstanceForNext(int type,
			TSServerInfoModule module, long searchId) {
		FilterResponse filter = getInstanceForNextInternal(type, module,
				searchId);
		filter.setForNext(true);
		return filter;
	}
	
	
	public static FilterResponse getInstanceForNextInternal(
		int type,
		TSServerInfoModule module,long searchId)
	{
		switch (type)
		{
			case TYPE_ADDRESS_FOR_NEXT :
				{
					String streetName = module.getFunction(1).getParamValue();
					//logger.debug("streetName =["+streetName+"]");
					//return new DesotoAssStNameFilterResponseForNext(streetName);
					return new AddressFilterForNext(streetName,searchId);
				}
			/*
			case TYPE_BALDWIN_ADDRESS_FOR_NEXT :
				{
					String streetName = module.getFunction(1).getParamValue();
					return new BaldwinAssessorAddresFilterForNext(streetName);
				}
			*/
			case TYPE_PARCEL_FOR_NEXT :
				{
					String parcelNumber = module.getFunction(0).getParamValue();
					if (logger.isDebugEnabled())
						logger.debug("parcel number =[" + parcelNumber + "]");
					return new ParcelNumberFilterResponseForNext(parcelNumber,searchId);
				}
			case TYPE_CLEANED_PARCEL_FOR_NEXT :
			{
				String parcelNumber = module.getFunction(0).getParamValue();
				if (logger.isDebugEnabled())
					logger.debug("parcel number =[" + parcelNumber + "]");
				return new ParcelNumberFilterResponseForNext(parcelNumber,searchId, true);
			}
			case TYPE_SUBDIV_NAME_FOR_NEXT :
				{
					String subdivName = module.getFunction(0).getParamValue();
					return new DesotoRegSubdivNameFilterResponseForNext(subdivName,searchId);
				}
			case TYPE_SUBDIV_CODE_FOR_NEXT :
				{
					String subdivCode = module.getFunction(0).getParamValue();
					String lotNo = module.getFunction(1).getParamValue();
					return new DesotoRegSubdivCodeLotFilterResponseForNext(
						subdivCode,
						lotNo,searchId);
				}
			case TYPE_BOOK_PAGE_FOR_NEXT :
				{
					String book = module.getFunction(0).getParamValue();
					String page = module.getFunction(1).getParamValue();
					return new DesotoRegBookPageFilterResponseForNext(
						book,
						page,searchId);
				}
			case TYPE_BOOK_PAGE_FOR_NEXT_GENERIC :
			{
				// find book and page values
				String book = "";
				String page = "";
				for(int i=0; i<module.getFunctionCount(); i++){
					TSServerInfoFunction function = module.getFunction(i);
					Set<String> candNames = new HashSet<String>();
					candNames.add(function.getName().toLowerCase());
					candNames.add(function.getParamName().toLowerCase());
					candNames.add(function.getLabel().toLowerCase());					
					if(candNames.contains("book")){
						book = function.getParamValue();
					} else if(candNames.contains("page")){
						page = function.getParamValue();
					}				
				}
				// create filter
				return new DesotoRegBookPageFilterResponseForNext(book, page, searchId);
			}
            case TYPE_BOOK_PAGE_FOR_NEXT_CROSREF:
            {
                String book = module.getFunction(0).getParamValue();
                String page = module.getFunction(1).getParamValue();
                return new DesotoRegBookPageFilterResponseForNext(
                    book,
                    page,
                    true,searchId);                
            }
			case TYPE_INSTRUMENT_FOR_NEXT :
				{
					String instrNo = module.getFunction(0).getParamValue();
					if (logger.isDebugEnabled())
						logger.debug("instrNo =[" + instrNo + "]");
					return new InstrumentNumberFilterResponse(instrNo,searchId);
				}
			case TYPE_INSTRUMENT_FOR_NEXT_GENERIC:
			{
				// indetify instrument value
				String instr = "";
				for(int i=0; i<module.getFunctionCount(); i++){
					TSServerInfoFunction function = module.getFunction(i);
					String [] refs = new String[]{function.getName().toLowerCase(),  function.getParamName().toLowerCase(), function.getLabel().toLowerCase()};
					String [] cands = new String[]{"inst", "instr", "instrument"};
					for(String ref: refs){
						for(String cand: cands){
							if(cand.equals(ref)){
								instr = function.getParamValue();
								break;
							}
						}						
					}
				}
				// create the filter
				return new InstrumentNumberFilterResponse(instr, searchId);
			}				
			case TYPE_ASSESSOR_SUBDIV_NAME_AND_LOT_FOR_NEXT :
				{
					String subdivName = module.getFunction(0).getParamValue();
					String subdivLot = module.getFunction(1).getParamValue();
					return new SubdivNameAndLotForNext(subdivName, subdivLot,searchId);
				}
			case TYPE_SCORE_FOR_NEXT :
				{
					return new ScoreFilterResponse(searchId);
				}
			case TYPE_REGISTER_NAME_FOR_NEXT:
				{
					//String lastName = module.getFunction(0).getParamValue();
					//String firstName = module.getFunction(1).getParamValue();
					
					return ((FilterResponse)new NameFilterForNext(module.getSaObjKey(),searchId,module,false));
				}
			case TYPE_REGISTER_NAME_FOR_NEXT_IGNORE_MIDDLE:
			{
				GenericNameFilter genericNameFilter = (GenericNameFilter)new NameFilterForNext(module.getSaObjKey(),searchId,module,false);
				genericNameFilter.setIgnoreMiddleOnEmpty(true);
				return genericNameFilter;
			}	
			case TYPE_REGISTER_NAME_FOR_NEXT_OH_FRANKLIN_PR:
			{
				/* 
				   please note that we use only first name and last name
				   not also the middle name for next filtering
				   middle appears only when encountering a MCN case
				*/
				String name = module.getFunction(0).getParamValue();
				Pattern p = Pattern.compile("([^,]*),\\s+([^ ]*)");
				Matcher m = p.matcher(name);
				String firstName, lastName;
				if(m.find()){
					lastName = m.group(1);
					firstName = m.group(2);
				} else {
					lastName = name;
					firstName = "";
				}
				return new RegisterNameFilterResponseForNext(lastName, firstName,searchId);
			}				
			default :
				return new FilterResponse(SearchAttributes.NO_KEY,searchId);
		}
	}
	public static FilterResponse getInstance(int type, SearchAttributes saForComparison, int cleanerId,long searchId)
	{
		return getInstance(type, SearchAttributes.NO_KEY, saForComparison, cleanerId,searchId, null);
	}
	protected void printRows(ParsedResponse pr)
	{
		Vector v = pr.getResultRowsAsStrings();
		for (int i = 0; i < v.size(); i++)
		{
			String link = (String)v.get(i);
			if (logger.isDebugEnabled())
				logger.debug("row = " + link);
		}
	}
	public BigDecimal getScoreOneRow(ParsedResponse row)
	{
		return ATSDecimalNumberFormat.ONE;
		//return matchOwnersName((String) row.getPropertyIdentificationSet(0).getAtribute("OwnerLastName"));
	}
	
	/* (non-Javadoc)
	 * @see ro.cst.tsearch.search.filter.FilterResponseInterface#setStrategyType(int)
	 */
	public void setStrategyType(int i)
	{
		strategyType = i;
	}
	
	public int getStrategyType (){
		return strategyType;
	}
	public void setThreshold(BigDecimal decimal)
	{
		threshold = decimal;
	}
	public BigDecimal getThreshold()
	{
		return threshold;
	}
	
	//	////////////////////////////////////////////////////////////
	protected boolean matchInvalidPattern(String cand)
	{
		return matchPatterns(invalidPatterns, cand);
	}
	
    
    /**
     * logs the action of filtering a row of intermediate results
     * @param index row index
     * @param currentRow row contents
     * @param score row score
     * @param passed true for accepted, false for rejected
     * @return 
     */
    public String logRow(int index, ParsedResponse pr, BigDecimal score, boolean passed, long searchId){
    	pr.setSearchId(searchId);
    	String doc = pr.getTsrIndexRepresentation();    	
    	doc = doc.replaceAll("\\s{2,}", " ");
    	StringBuilder sb = new StringBuilder();
    	String id = String.valueOf(System.nanoTime()) + (passed ? "_passed" : "_rejected");
    	String style = passed ? " style='display:none'" : "";
    	
    	sb.append("<tr class='row" + ((index%2)+1) + "' id='" + id + "'" + style + "><td>");
    	sb.append(index + 1);
    	sb.append("</td><td class='" + (passed?"passed":"failed") + "'>");
		sb.append(ATSDecimalNumberFormat.format(score));
    	sb.append("</td>");
    	sb.append(doc);
    	sb.append("</tr>");     	
//    	SearchLogger.info(sb.toString(), searchId);
    	if(!passed) {
    		TSInterface tsInterface = pr.getTsInterface();
    		if(tsInterface != null) {
    			tsInterface.saveInvalidatedDocumentForRestoration(this, pr);
    		}
    		
    	}
    	return sb.toString();
    }

    public static String logRow(int index, ParsedResponse pr, long searchId){
    	pr.setSearchId(searchId);
    	String doc = pr.getTsrIndexRepresentation();    	
    	doc = doc.replaceAll("\\s{2,}", " ");
    	StringBuilder sb = new StringBuilder();
    	String id = String.valueOf(System.nanoTime()) + "_passed";
    	String style = " style='display:none'";
    	sb.append("<tr class='row").append((index%2)+1).append("' id='").append(id).append("'").append(style).append("><td>");
    	sb.append(index + 1);
    	sb.append("</td>");
    	sb.append(doc);
    	sb.append("</tr>");     	
    	return sb.toString();
    }

    private String getFormattedFilterCriteria(){
    	
    	String criteria = getFilterCriteria();
    	if(getMinRowsToActivate() > 1){
    		criteria += " if at least " + getMinRowsToActivate() + " results";
    	}
    	if(isSkipUnique()){
    		criteria += " - let unique results pass";
    	}
    	if(isEmpty(criteria)){
    		criteria = "(empty)";
    	}
    	return criteria;
    	
    }

    /* (non-Javadoc)
	 * @see ro.cst.tsearch.search.filter.FilterResponseInterface#getFilterName()
	 */
    public String getFilterName(){
    	return this.getClass().getSimpleName();
    }
    
    /* (non-Javadoc)
	 * @see ro.cst.tsearch.search.filter.FilterResponseInterface#getFilterCriteria()
	 */
    public String getFilterCriteria(){
    	return "criteria";
    }
 
    public static void logRows(Vector<ParsedResponse> rows, long searchId){
		
		String id = System.nanoTime() + "_filter";
		
		StringBuilder toLog = new StringBuilder();
		toLog.append("<div id=\"").append(id).append("\">");
		
//		SearchLogger.info("<div id=\"" + id + "\">", searchId);
		
		int size = 0;
    	for (ParsedResponse row : rows) {
			Boolean possibleNavigationRow = (Boolean)row.getAttribute(ParsedResponse.SERVER_NAVIGATION_LINK);
			if(!Boolean.TRUE.equals(possibleNavigationRow)) {
				size ++;
			} 
		}
    	
    	if (size!=0) {
//    		SearchLogger.info("<br/><span class='filter'>" + "Total"
//    				+ "</span> of <span class='number'> " + size
//    				+ "</span> <span class='rtype'>intermediate</span> results: ", searchId);
    		toLog.append("<br/><span class='filter'>Total</span> of <span class='number'> ").append(size)
    			.append("</span> <span class='rtype'>intermediate</span> results: ")
    			.append("<div>")
    			.append("<table border='1' cellspacing='0' width='99%'>")
    			.append("<tr><th>No</th><th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th>")
    			.append("</tr>");

        	//SearchLogger.info(createCollapsibleHeader(), searchId);
//        	SearchLogger.info("<div>", searchId);
//    		SearchLogger.info(
//    				"<table border='1' cellspacing='0' width='99%'>"
//    				+ "<tr><th>No</th><th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th>"
//    				+ "</tr>", searchId);

    		boolean hasRejected = false;
    		for (int i = 0, n = rows.size(); i < n; i++){
    			ParsedResponse pr = (ParsedResponse)rows.get(i);
    			Boolean possibleNavigationRow = (Boolean)pr.getAttribute(ParsedResponse.SERVER_NAVIGATION_LINK);
    			if(!Boolean.TRUE.equals(possibleNavigationRow)) {
    				pr.setSearchId(searchId);
    				toLog.append(logRow(i, pr, searchId));
    			}
    		}
        	
    		toLog.append("</table></div>");
//    		SearchLogger.info("</table></div>", searchId);
    		
    		String newId = id + (hasRejected ? "_rejected" : "_passed");
    		String style = hasRejected ? "" : "none";
    		
    		toLog.append("</div><script language=\"JavaScript\">var dv=document.getElementById(\"")
    			.append(id)
    			.append("\"); dv.id = \"")
    			.append(newId)
    			.append("\"; dv.style.display=\"")
    			.append(style)
    			.append("\";</script>");
    			
//    		SearchLogger.info("</div><script language=\"JavaScript\">var dv=document.getElementById(\"" + id + "\"); dv.id = \"" + newId + "\"; dv.style.display=\"" + style + "\";</script>", searchId);
    		SearchLogger.info(toLog.toString(), searchId);
    	}
    			
    }

    /**
     * Flag to be used in logging
     */
    private transient boolean forNext = false;

	/* (non-Javadoc)
	 * @see ro.cst.tsearch.search.filter.FilterResponseInterface#setForNext(boolean)
	 */
	public void setForNext(boolean forNext) {
		this.forNext = forNext;
	}

	/* (non-Javadoc)
	 * @see ro.cst.tsearch.search.filter.FilterResponseInterface#isForNext()
	 */
	public boolean isForNext() {
		return forNext;
	}
	
	public DocsValidator getValidator() {
		return new DocsValidator(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext(),this);
	}
	
	protected Search getSearch(){
		return InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
	}
	
	protected String getSearchAttribute(String name){
		return getSearch().getSa().getAtribute(name);
	}
    
	private int minRowsToActivate = 0;	
	public void setMinRowsToActivate(int minRowsToActivate){
		this.minRowsToActivate = minRowsToActivate;
	}
	
	public int getMinRowsToActivate(){
		return minRowsToActivate;
	}

	private boolean skipUnique = false;
	
	public boolean isSkipUnique(){
		return skipUnique;
	}
	
	public void setSkipUnique(boolean skipUnique){
		this.skipUnique = skipUnique;
	}
	
	public void init(){}
	
	private boolean initAgain = false;
	
	public void setInitAgain(boolean initAgain){
		this.initAgain = initAgain;
	}
	
	public boolean isInitAgain(){
		return initAgain;
	}
	/**
	 * @return the saveInvalidatedInstruments
	 */
	public boolean isSaveInvalidatedInstruments() {
		return saveInvalidatedInstruments;
	}
	/**
	 * @param saveInvalidatedInstruments the saveInvalidatedInstruments to set
	 */
	public void setSaveInvalidatedInstruments(boolean saveInvalidatedInstruments) {
		this.saveInvalidatedInstruments = saveInvalidatedInstruments;
	}
	public int getFilterForNextFollowLinkLimit() {
		return filterForNextFollowLinkLimit;
	}
	/**
	 * Default is -1 which means this is NOT a filterForNext<br>
	 * 0 means that this is a filterForNext and all documents must pass<br>
	 * Above 0 means that this is a filterForNext and if more documents than this limit fails this filter will fail  
	 * @param filterForNextFollowLinkLimit 
	 */
	public void setFilterForNextFollowLinkLimit(int filterForNextFollowLinkLimit) {
		this.filterForNextFollowLinkLimit = filterForNextFollowLinkLimit;
	}
	
	public void addInModulesAppliedFor(String module) {
		modulesAppliedFor.add(module);
	}
	
	public boolean isInModulesAppliedFor(String module) {
		return modulesAppliedFor.contains(module);
	}
	
}
package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class RejectAlreadySavedDocumentsForUpdateFilter extends FilterResponse {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final long originalSearchId;
	private Search search = null;

	/**
	 * @param searchId
	 */
	public RejectAlreadySavedDocumentsForUpdateFilter(long searchId) {
		super(searchId);
		originalSearchId = getSearch().getSa().getOriginalSearchId();
		if (originalSearchId != -1 && search == null){
			search = SearchManager.getSearchFromDisk(originalSearchId);
		}
		setThreshold(new BigDecimal("0.95"));
	}
	
	@Override
    public String getFilterName(){
    	return "Filter Out Existing Documents";
    }
    
	@Override
	public String getFilterCriteria(){
    	return "Drop Already Saved Documents in Original Search(Only for Update)";
    }
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		
		DocumentI document = row.getDocument();
		if(document != null && search != null) {
			DocumentsManagerI managerI = search.getDocManager();
			try {
				managerI.getAccess();
				if(managerI.getDocumentsWithInstrumentsFlexible(false, document.getInstrument()).size() >= 1) {
					return ATSDecimalNumberFormat.ZERO;
				}
			} catch (Throwable t) {
				logger.error("Error computing score for RejectAlreadySavedDocumentsForUpdateFilter", t);
			} finally {
				managerI.releaseAccess();
			}
		}
		return ATSDecimalNumberFormat.ONE;
		
	}
}

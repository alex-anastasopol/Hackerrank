package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class RejectAlreadySavedDocumentsFilterResponse extends FilterResponse {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private boolean ignoreDocumentCategory = false;
	private boolean ignoreDocumentSubcategory = false;

	/**
	 * @param searchId
	 */
	public RejectAlreadySavedDocumentsFilterResponse(long searchId) {
		super(searchId);
		setThreshold(new BigDecimal("0.95"));
	}
	
	@Override
    public String getFilterName(){
    	return "Filter Out Existing Documents";
    }
    
	@Override
	public String getFilterCriteria(){
    	return "Drop Already Saved Documents";
    }
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		
		
		DocumentI document = row.getDocument();
		if(document != null) {
			Search search = getSearch();
			DocumentsManagerI managerI = search.getDocManager();
			try {
				managerI.getAccess();
				
				InstrumentI clone = document.getInstrument().clone();
				
				clone = formatInstrument(clone);
				
				if(isIgnoreDocumentCategory()) {
					clone.setDocType("");
					clone.setDocSubType("");
				} else if (isIgnoreDocumentSubcategory()) {
					clone.setDocSubType("");
				}
				
				if(managerI.getDocumentsWithInstrumentsFlexible(false,clone).size() >= 1) {
					return ATSDecimalNumberFormat.ZERO;
				}
			} catch (Throwable t) {
				logger.error("Error computing score for RejectAlreadySavedDocumentsFilterResponse", t);
			} finally {
				managerI.releaseAccess();
			}
		}
		return ATSDecimalNumberFormat.ONE;
		
	}

	public boolean isIgnoreDocumentCategory() {
		return ignoreDocumentCategory;
	}

	public void setIgnoreDocumentCategory(boolean ignoreDocumentCategory) {
		this.ignoreDocumentCategory = ignoreDocumentCategory;
	}

	public boolean isIgnoreDocumentSubcategory() {
		return ignoreDocumentSubcategory;
	}

	public void setIgnoreDocumentSubcategory(boolean ignoreDocumentSubcategory) {
		this.ignoreDocumentSubcategory = ignoreDocumentSubcategory;
	}
	
	protected InstrumentI formatInstrument(InstrumentI instrNo){
		
		return instrNo;
	}
	
}

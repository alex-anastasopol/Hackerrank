package ro.cst.tsearch.search.filter.newfilters.misc;

import java.math.BigDecimal;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.PriorFileDocumentI;

public class NoImageNoExceptionFilter extends FilterResponse {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NoImageNoExceptionFilter(long searchId) {
		super(searchId);
		setThreshold(ATSDecimalNumberFormat.ONE);
	}
	
	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		DocumentI document = row.getDocument();
		if (document instanceof PriorFileDocumentI) {
			PriorFileDocumentI priorFileDocument = (PriorFileDocumentI) document;
			if( priorFileDocument.hasImage() || 
					StringUtils.isNotEmpty(priorFileDocument.getExceptions())) {
				return ATSDecimalNumberFormat.ONE;
			}
		}
		return ATSDecimalNumberFormat.ZERO;
	}
	
	@Override
    public String getFilterName(){
    	return "Filter Documents with no image and no exceptions";
    }
	
	public String getFilterCriteria() {
		return "Drop All Documents with no image and no exceptions";
	}

}

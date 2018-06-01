package ro.cst.tsearch.search.filter.newfilters.misc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ParsedResponseDateComparator;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class CrossReferenceToInvalidatedFilter extends FilterResponse {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected String []docTypes = {"RELEASE"};

	public CrossReferenceToInvalidatedFilter(long searchId) {
		super(searchId);
		setThreshold(ATSDecimalNumberFormat.ONE);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected Vector filterRows(Vector rows) {
		Collections.sort(rows,Collections.reverseOrder(new ParsedResponseDateComparator()));
		return super.filterRows(rows);
	}
	
	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		DocumentI document = row.getDocument();
		DocumentsManagerI manager = getSearch().getDocManager();
		if(document != null) {
			if(document.isOneOf(docTypes)){
				Set<InstrumentI> references = document.getParsedReferences();
				boolean foundInvalidated = false;
				boolean foundValidated = false;
				for (InstrumentI reference : references) {
					if(StringUtils.isNotBlank(reference.getInstno())) {
						if(getSearch().getSa().isInvalidatedInstrument(reference.getInstno())) {
							foundInvalidated = true;
						} 
					}
					if(!foundInvalidated && StringUtils.isNotBlank(reference.getBook()) && StringUtils.isNotBlank(reference.getPage())) {
						if(getSearch().getSa().isInvalidatedInstrument(reference.getBook(), reference.getPage())) {
							foundInvalidated = true;
						} 
					}
					try{
						manager.getAccess();
						if(manager.getRegisterDocuments(reference, true).size()>0){
							foundValidated = true;
						}
					} finally{
						manager.releaseAccess();
					}
					
				}
				if(foundInvalidated && !foundValidated) {
					return ATSDecimalNumberFormat.ZERO;
				}
				
			} 
		}
		return ATSDecimalNumberFormat.ONE;
	}
	
	@Override
    public String getFilterName(){
    	return "Filter Releases Of Invalidated Documents";
    }
	
	public String getFilterCriteria() {
		return "Drop All Releases Of Invalidated Documents";
	}
	

}

package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.Vector;

import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

public class RejectNonUniqueFilterResponse extends FilterResponse {

	private static final long serialVersionUID = -3272196039508586839L;

	public RejectNonUniqueFilterResponse(long searchId) {
		super(searchId);
		setThreshold(ATSDecimalNumberFormat.ONE);
	}	
	
	private boolean isUnique = true;
	
	public void computeScores(@SuppressWarnings("rawtypes") Vector rows){
		isUnique = (rows.size() == 1);
		super.computeScores(rows);
	}
	
	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		return isUnique ? ATSDecimalNumberFormat.ONE
				: ATSDecimalNumberFormat.ZERO;
	}

    @Override
    public String getFilterName(){
    	return "Filter out non unique results";
    }
    
	@Override
	public String getFilterCriteria(){
		return "Result=Unique";  
	}
	
}

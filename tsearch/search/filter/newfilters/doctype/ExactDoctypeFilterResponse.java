package ro.cst.tsearch.search.filter.newfilters.doctype;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

public class ExactDoctypeFilterResponse extends FilterResponse {

	private static final long serialVersionUID = 8768121997013188135L;
	private Set<String> rejectedSet = new HashSet<String>();
	
	public void addRejected(String docType){
		rejectedSet.add(docType.toUpperCase());
	}

	public ExactDoctypeFilterResponse(long searchId){
		super(searchId);
		setThreshold(new BigDecimal("0.95"));
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {	
		try{
			String docType = row.getSaleDataSet(0).getAtribute("DocumentType").trim().toUpperCase();
			if(rejectedSet.contains(docType)){
				return ATSDecimalNumberFormat.ZERO;
			} else {
				return ATSDecimalNumberFormat.ONE;
			}
        } catch (Exception e){
        	e.printStackTrace();
        	return ATSDecimalNumberFormat.ONE;	
        }		
	}
	
    public String getFilterCriteria(){
    	return "Reject: " + rejectedSet;
    }
    
    public String getFilterName(){
    	return "DocType";
    }
}

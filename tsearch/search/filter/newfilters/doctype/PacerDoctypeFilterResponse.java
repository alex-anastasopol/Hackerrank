package ro.cst.tsearch.search.filter.newfilters.doctype;

import java.math.BigDecimal;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.ParcelIDFilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

public class PacerDoctypeFilterResponse extends FilterResponse {

	private static final long serialVersionUID = 8796151006593768814L;

	/**
     * Main log category.
     */
    protected static final Category logger = Category.getInstance(ParcelIDFilterResponse.class.getName());

    /**
     * Details log category.
     */
    protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + ParcelIDFilterResponse.class.getName());
    
    protected Vector<String> doctypesToPass = new Vector<String>();

    /**
     * Constructor
     */
    public PacerDoctypeFilterResponse(long searchId){
        super(searchId);
        setThreshold(new BigDecimal("0.9"));
        doctypesToPass.add(".*bke");
        doctypesToPass.add(".*dce");
        doctypesToPass.add("06ca");
    }
    
    public PacerDoctypeFilterResponse(long searchId, Vector<String> doctypesToPass) {
    	super(searchId);
    	setThreshold(new BigDecimal("0.9"));
    	if(doctypesToPass != null) {
    		this.doctypesToPass.addAll(doctypesToPass);
    	} else {
    		this.doctypesToPass.add(".*bke");
    		this.doctypesToPass.add(".*dce");
    		this.doctypesToPass.add("06ca");
    	}
    }
    
    /**
     * Score response for docType. 
     * @see ro.cst.tsearch.search.filter.FilterResponse#getScoreOneRow(ro.cst.tsearch.servers.response.ParsedResponse)
     */
    public BigDecimal getScoreOneRow(ParsedResponse row) {
    	
    	// check that we have sale data set
    	if(row.getSaleDataSetsCount() == 0){
    		return ATSDecimalNumberFormat.ONE;
    	}
    	
    	// check that we have doc type
    	String docType = row.getSaleDataSet(0).getAtribute("DocumentType");
    	if(StringUtils.isEmpty(docType)){
    		return ATSDecimalNumberFormat.ONE;
    	}
    	
    	// compute score
        docType = docType.toLowerCase().trim();
        BigDecimal score = ATSDecimalNumberFormat.ZERO;
        
        for (String doctypeToPass : doctypesToPass) {
			if(docType.matches("^" + doctypeToPass + "$"))
				return ATSDecimalNumberFormat.ONE;
		}
        
        return score;
    }
    
    @Override
    public String getFilterName(){
    	return "Filter by Court Type";
    }
    
	@Override
	public String getFilterCriteria(){
		if(doctypesToPass.size() > 0) {
			String filterCriteria = " Court='";
			for (String doctypeToPass : doctypesToPass) {
				filterCriteria += doctypeToPass + ",";
			}
			return filterCriteria.substring(0, filterCriteria.length() - 1) + "'";
		} else {
			return " Court=''";
		}		
	}
}

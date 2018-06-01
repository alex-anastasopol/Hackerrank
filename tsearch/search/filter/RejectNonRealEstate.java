package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.Vector;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

public class RejectNonRealEstate extends FilterResponse {

	/**
	 * 
	 */
	static final long serialVersionUID = 10000000;
	
	/**
	 * @param searchId
	 */
	public RejectNonRealEstate(long searchId) {
		super(searchId);
	}

	/**
	 * @param key
	 * @param searchId
	 */
	public RejectNonRealEstate(String key, long searchId) {
		super(key, searchId);
	}

	/**
	 * @param sa1
	 * @param key
	 * @param searchId
	 */
	public RejectNonRealEstate(SearchAttributes sa1, String key, long searchId) {
		super(sa1, key, searchId);
	}
	
	@Override
	@SuppressWarnings("unchecked") // for Vector<PropertyIdentificationSet>
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		
		for(PropertyIdentificationSet pis: (Vector<PropertyIdentificationSet>)row.getPropertyIdentificationSet()){
			if(pis.getAtribute("PropertyType").toLowerCase().contains("real estate")){
				return  ATSDecimalNumberFormat.ONE;
			}
		}
		return  ATSDecimalNumberFormat.ZERO;
	}
	
	@Override
    public String getFilterName(){
    	return "Filter by Property Type";
    }
    
    @Override
	public String getFilterCriteria(){
    	return "Type='Real Estate'";
    }
    
}

package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterResponse2;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

/**
 * @author radu bacrau
 *
 */
public class SubdivisionOrAddressFilterResponse extends FilterResponse {

	private static final long serialVersionUID = 100000000L;
	
	protected AddressFilterResponse2 addressFilter;
	protected SubdivisionNameFilterResponse2 subdivisionFilter; 
    
    public SubdivisionOrAddressFilterResponse(long searchId){
        super(searchId);
        
        threshold = new BigDecimal("0.7");
        strategyType = STRATEGY_TYPE_HIGH_PASS;
                
        addressFilter = new AddressFilterResponse2("",searchId);
        addressFilter.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		
		subdivisionFilter = new SubdivisionNameFilterResponse2(searchId);
    }
    
    public SubdivisionOrAddressFilterResponse(long searchId, BigDecimal threshold){
        super(searchId);
        
        this.threshold = threshold;
        strategyType = STRATEGY_TYPE_HIGH_PASS;
                
        addressFilter = new AddressFilterResponse2("",searchId);
        addressFilter.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		
		subdivisionFilter = new SubdivisionNameFilterResponse2(searchId);
    }

    public BigDecimal getScoreOneRow(ParsedResponse row) {
   	
    	BigDecimal score=new BigDecimal(1);
    	BigDecimal max=new BigDecimal(0);
    	
    	String subdivRefName = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);    	
    	
    	if(addressFilter.isEmptyRefAddress() && StringUtils.isEmpty(subdivRefName)) {
    		return ATSDecimalNumberFormat.ONE;
    	}
    	
    	try {
    		List<String> allCandidates = getAllCandidates(row);
    		if(allCandidates.isEmpty()) {
    			return ATSDecimalNumberFormat.ONE; 
    		}
    		for(String str : allCandidates ) {
    			   			
    			ro.cst.tsearch.search.address2.StandardAddress tokAddr = new ro.cst.tsearch.search.address2.StandardAddress(str);
    			if(!addressFilter.isEmptyRefAddress() && !StringUtils.isEmpty(subdivRefName)) {
    		    	BigDecimal addressScore= addressFilter.calculateScore(str);
    		    	BigDecimal subdivScore= subdivisionFilter.calculateScore(subdivRefName, str);
    		    	score = new BigDecimal(Math.max(addressScore.doubleValue(),subdivScore.doubleValue()));
    		    	if (score.compareTo(max)>0) {
    		    		max = score;
    		    	}
    		    	continue;
    	    	}
    			if(StringUtils.isNotEmpty(tokAddr.getAddressElement(StandardAddress.STREET_NUMBER))
						|| StringUtils.isNotEmpty(tokAddr.getAddressElement(StandardAddress.STREET_SUFFIX)) )
				 {
    				BigDecimal newScore = addressFilter.calculateScore(str);
    				score = new BigDecimal(Math.max(score.doubleValue(),newScore.doubleValue()));
    				if (score.compareTo(max)>0) {
    		    		max = score;
    		    	}
    				continue;
				 }
    			
    			
    			if(addressFilter.isEmptyRefAddress() && StringUtils.isEmpty(subdivRefName)) {
            		return ATSDecimalNumberFormat.ONE;
            	}
    			
    		}
    		
    		return max;
    			    	
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return ATSDecimalNumberFormat.ONE;
    }
    
    public List<String> getAllCandidates(ParsedResponse row) {
    	
    	List<String> allCandidates = new ArrayList<String>();
    	allCandidates.addAll(subdivisionFilter.getCandidateSubdivisions(row));
    	allCandidates.addAll(addressFilter.getCandidateAddress(row));
    	
    	Iterator<String> it = allCandidates.iterator();
    	while(it.hasNext()) {
    		if(StringUtils.isEmpty(it.next())) {
    			it.remove();
    		}
    	}
    	
    	return allCandidates;
    }
    
    @Override
    public String getFilterName(){
    	return "Filter by Subdivision or Address";
    }
    
    @Override
	public String getFilterCriteria(){
    	return subdivisionFilter.getFilterCriteria() + " or " + addressFilter.getFilterCriteria();
    }
}

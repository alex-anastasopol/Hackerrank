package ro.cst.tsearch.search.filter.newfilters.address;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.address.AddressI;

public class GenericMultipleAddressFilter extends FilterResponse {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected List<GenericAddressFilter> filterList = new ArrayList<GenericAddressFilter>();
	protected Set<String> addressesFound = new HashSet<String>();

	public GenericMultipleAddressFilter(long searchId) {
		super(searchId);
		setThreshold(new BigDecimal(0.8));
		
		GenericAddressFilter filter = new GenericAddressFilter(searchId);
		filter.setMarkIfCandidatesAreEmpty(true);
		filter.init();
		String refAddress = filter.getRefAddress().toString();
		if(StringUtils.isNotEmpty(refAddress.replace(":", ""))) {
			addressesFound.add(filter.getRefAddress().toString());
			filterList.add(filter);
		}
	}
	
	@Override
	public void init() {
		//nothing to do
	}
	
	public void addNewFilterFromAddress(AddressI address) {
		if(address != null) {
			GenericAddressFilter filter = new GenericAddressFilter(searchId);
			filter.init(address.shortFormString());
			String refAddress = filter.getRefAddress().toString();
			if(StringUtils.isNotEmpty(refAddress.replace(":", "")) && !addressesFound.contains(refAddress)) {
				addressesFound.add(refAddress);
				filter.setMarkIfCandidatesAreEmpty(true);
				filterList.add(filter);
			}
			
		}
	}
	
	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		BigDecimal result = ATSDecimalNumberFormat.NA;
		for (GenericAddressFilter filter : filterList) {
			BigDecimal resultFilterLegal = filter.getScoreOneRow(row);
			if(resultFilterLegal.doubleValue() >= getThreshold().doubleValue()) {
				return resultFilterLegal;
			} else if (resultFilterLegal == ATSDecimalNumberFormat.NA) {
				//ignore
			} else {	//less that the threshold
				if(result.doubleValue() < resultFilterLegal.doubleValue()) {
					result = resultFilterLegal;
				}
			}
		}
		if(result.equals(ATSDecimalNumberFormat.NA)) {
			return ATSDecimalNumberFormat.ONE;
		}
		return result;
	}
	
	@Override
    public String getFilterName(){
    	return "Filter by Address(Multiple Address Filter)";
    }
	
	@Override
	public String getFilterCriteria(){

		String retVal = null;
		for (GenericAddressFilter filter : filterList) {
			if(retVal == null) {
				retVal = filter.getFilterCriteria();
			} else {
				retVal += " or " + filter.getFilterCriteria();
			}
		}
    	
		if(retVal != null) {
			return retVal;
		}
		
    	return "Address='<no address to test against>'";
    }

}

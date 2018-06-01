package ro.cst.tsearch.search.filter.newfilters.address;

import java.math.BigDecimal;

import ro.cst.tsearch.search.filter.FilterResponse;

public class AddressFilterFactory {

	
	public static FilterResponse getAddressHighPassFilter(long searchId,double score){
		FilterResponse fr = new AddressFilterResponse2("",searchId);
		fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		fr.setThreshold(new BigDecimal(score));
		return fr;
	}
	
	public static GenericAddressFilter getGenericAddressHighPassFilter(long searchId,double score){
		GenericAddressFilter fr = new GenericAddressFilter("",searchId);
		fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		fr.setThreshold(new BigDecimal(score));
		return fr;
	}
	
	
	public static FilterResponse getAddressHybridFilter(long searchId,double score) {
		return getAddressHybridFilter(searchId,score,false);
	}
	
	public static FilterResponse getAddressHybridFilter(long searchId,double score, boolean ignoreUnitOnEmpty) {
		AddressFilterResponse2 fr = new AddressFilterResponse2("",searchId);
		fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HYBRID);
		fr.setIgnoreUnitOnEmpty(ignoreUnitOnEmpty);
		fr.setThreshold(new BigDecimal(score));
		return fr;
	}
	
	public static FilterResponse getAddressHybridFilter(long searchId,double score, boolean ignoreUnitOnEmpty, boolean ignoreUnitOnUniqueResult) {
		AddressFilterResponse2 fr = new AddressFilterResponse2("",searchId);
		fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HYBRID);
		fr.setIgnoreUnitOnEmpty(ignoreUnitOnEmpty);
		fr.setIgnoreUnitOnUniqueResult(ignoreUnitOnUniqueResult);
		fr.setThreshold(new BigDecimal(score));
		return fr;
	}
	
	
}

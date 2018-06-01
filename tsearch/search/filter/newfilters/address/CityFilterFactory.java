package ro.cst.tsearch.search.filter.newfilters.address;

import java.math.BigDecimal;

import ro.cst.tsearch.search.filter.CityFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;

public class CityFilterFactory {
	
	public static FilterResponse getCityFilter(long searchId,double score) {
		FilterResponse fr = new CityFilterResponse(searchId); 
		fr.setThreshold(new BigDecimal(score));
		return fr;
	}
	public static FilterResponse getCityFilterDefault(long searchId) {
		FilterResponse fr = new CityFilterResponse(searchId); 
		fr.setThreshold(new BigDecimal(0.6));
		return fr;
	}
}

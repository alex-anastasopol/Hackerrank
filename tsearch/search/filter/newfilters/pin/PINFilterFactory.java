package ro.cst.tsearch.search.filter.newfilters.pin;

import ro.cst.tsearch.bean.SearchAttributes;

public class PINFilterFactory {

	
	public static  PinFilterResponse getDefaultPinFilter(long searchId){
		PinFilterResponse filter = new PinFilterResponse(SearchAttributes.LD_PARCELNO, searchId);
		return filter;
	}

	public static  PinFilterResponse getPinFilter(long searchId,boolean startWith,boolean ignoreZeroes){
		PinFilterResponse filter = new PinFilterResponse(SearchAttributes.LD_PARCELNO, searchId);
		filter.setStartWith(startWith);
		filter.setIgNoreZeroes(ignoreZeroes);
		return filter;
	}
	
	public static  PinFilterResponse getPinFilter(long searchId,String key,boolean startWith,boolean ignoreZeroes){
		PinFilterResponse filter = new PinFilterResponse(key, searchId);
		filter.setStartWith(startWith);
		filter.setIgNoreZeroes(ignoreZeroes);
		return filter;
	}
	
}


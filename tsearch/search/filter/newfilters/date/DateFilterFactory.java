package ro.cst.tsearch.search.filter.newfilters.date;

import java.math.BigDecimal;
import java.util.List;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class DateFilterFactory {
	
	public static FilterResponse getDateFilterForGoBack(String saKey,long searchId,
			TSServerInfoModule module){
		DateFilterForGoBack fr = new DateFilterForGoBack (saKey,searchId,module);
		fr.setThreshold(new BigDecimal(0.9));
		fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		return fr;
	}
}

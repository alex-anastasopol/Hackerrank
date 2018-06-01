package ro.cst.tsearch.search.filter.newfilters.doctype;

import java.math.BigDecimal;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;

public class DoctypeFilterFactory {

	public static FilterResponse getDoctypeBuyerFilter(long searchId){
		DocTypeSimpleFilter fr = new DocTypeSimpleFilter(searchId);
		fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		fr.setThreshold(new BigDecimal("0.8"));
		return fr;
	}
	
	public static DocTypeAdvancedFilter getDoctypeFilterForGeneralIndexOwnerNameSearch(long searchId){
		DocTypeAdvancedFilter fr = new DocTypeAdvancedFilter(searchId);
		return fr;
	}
	
	public static DocTypeAdvancedFilter getDoctypeFilterForGeneralIndexBuyerNameSearch(long searchId){
		DocTypeAdvancedFilter fr = new DocTypeAdvancedFilter(searchId);
		fr.setDocTypesWithGoodAssociations(new String[]{DocumentTypes.LIEN});
		fr.setDocTypesForGoodDocuments(new String[0]);
		return fr;
	}
	
	//public static FilterResponse getLienFilterByTTL(long searchId){
		//FilterResponse fr = new LienFilterByTTL(searchId);
		//fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		//fr.setThreshold(new BigDecimal("0.8"));
		//return fr;
	//}
	
	
	
	public static FilterResponse getDoctypeSubdivisionIsGranteeFilter(long searchId){
		FilterResponse fr = new DocTypeSimpleFilter(searchId);
		String []docTypes = { "RESTRICTION"	,	"PLAT",		"EASEMENT" , "CCER" };
		((DocTypeSimpleFilter)fr).setDocTypes(docTypes);
		fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		fr.setThreshold(new BigDecimal("0.8"));
		return fr;
	}
	
	public static FilterResponse getDoctypeFilter(long searchId,double score,String doctypes[],int strategy){
		FilterResponse fr = new DocTypeSimpleFilter(searchId);
		((DocTypeSimpleFilter)fr).setDocTypes(doctypes);
		fr.setStrategyType(strategy);
		fr.setThreshold(new BigDecimal(score));
		return fr;
	}
	
}

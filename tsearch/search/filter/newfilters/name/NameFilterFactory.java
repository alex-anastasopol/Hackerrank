package ro.cst.tsearch.search.filter.newfilters.name;

import java.math.BigDecimal;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.info.TSServerInfoModule;


public class NameFilterFactory {

	public static final double NAME_FILTER_THRESHOLD = 0.93d;
	public static final double NAME_FILTER_THRESHOLD_FOR_HYBRID = 0.66d;
	public static final double NAME_FILTER_THRESHOLD_FOR_SUBDIVISION = 0.65d;
	public static final double NAME_FILTER_THRESHOLD_FOR_GRANTEE_IS_SUBDIVISION = 0.75d;
	
	public static FilterResponse getDefaultNameFilter(String saKey,long searchId,
			TSServerInfoModule module){
		GenericNameFilter fr = new GenericNameFilter(saKey,searchId,false,module, true);
		fr.setThreshold(new BigDecimal(NAME_FILTER_THRESHOLD));
		return fr;
	}
	
	public static FilterResponse getDefaultNameFilterNoSinonims(String saKey,long searchId,
			TSServerInfoModule module){
		GenericNameFilter fr = new GenericNameFilter(saKey,searchId,false,module, true);
		fr.setThreshold(new BigDecimal(NAME_FILTER_THRESHOLD));
		fr.setUseSynonymsForCandidates(false);
		return fr;
	}
	
	public static FilterResponse getHybridNameFilter(String saKey,long searchId, TSServerInfoModule module){ 
		GenericNameFilter fr = new GenericNameFilter(saKey,searchId,false,module, true);
		fr.setThreshold( new BigDecimal( NAME_FILTER_THRESHOLD_FOR_HYBRID ) );
		fr.setPondereLast(2);
		//fr.setIgnoreMiddleOnEmpty(true);
		fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HYBRID);
		fr.setSkipUnique(true);
		fr.setUseSynonymsForCandidates(false);
		return fr;
	}
	
	public static FilterResponse getDefaultSynonimNameFilter(String saKey,long searchId,
			TSServerInfoModule module){
		SynonimNameFilter fr = new SynonimNameFilter(saKey,searchId,false,module, true);
		fr.setThreshold(new BigDecimal(NAME_FILTER_THRESHOLD));
		return fr;
	}
	public static FilterResponse getDefaultSynonimNameInfoPinCandidateFilter(String saKey,long searchId,
			TSServerInfoModule module){
		SynonimNameInfoPinCandidateFilter fr = new SynonimNameInfoPinCandidateFilter(saKey,searchId,false,module, true);
		fr.setThreshold(new BigDecimal(NAME_FILTER_THRESHOLD));
		return fr;
	}
	
	public static FilterResponse getDefaultNameFilter(String saKey,long searchId,
			TSServerInfoModule module, boolean initAgain){
		GenericNameFilter fr = new GenericNameFilter(saKey,searchId,false,module, true);
		fr.setThreshold(new BigDecimal(NAME_FILTER_THRESHOLD));
		fr.setInitAgain(initAgain);
		return fr;
	}
	
	public static FilterResponse getDefaultNameFilterForSubdivision(long searchId){
		GenericNameFilter fr = new GenericNameFilter("",searchId,true,null , false);
		((GenericNameFilter)fr).setUseSubdivisionNameAsCandidat(true);
		fr.setThreshold(new BigDecimal(NAME_FILTER_THRESHOLD_FOR_SUBDIVISION));
		return fr;
	}
	
	public static FilterResponse getNameFilterForSubdivisionWithCleaner(long searchId, int cleaner){
		GenericNameFilter fr = new GenericNameFilter("",searchId,true,null , false, cleaner);
		((GenericNameFilter)fr).setUseSubdivisionNameAsCandidat(true);
		fr.setThreshold(new BigDecimal(NAME_FILTER_THRESHOLD_FOR_SUBDIVISION));
		return fr;
	}	
	
	public static FilterResponse getDefaultNameFilterForGranteeIsSubdivision(String saKey,long searchId,
			TSServerInfoModule module){
		GenericNameFilter fr = new GenericNameFilter(saKey,searchId,true,module, false);
    	fr.setThreshold(new BigDecimal(NAME_FILTER_THRESHOLD_FOR_GRANTEE_IS_SUBDIVISION));
		return fr;
	}
	
	
	public static FilterResponse getNameFilterWithScore(String saKey,long searchId,
			TSServerInfoModule module,double score){
		GenericNameFilter fr = new GenericNameFilter(saKey,searchId,false,module, false);
		fr.setThreshold(new BigDecimal(score));
		return fr;
	}
	
	public static FilterResponse getNameFilterForSubdivisionWithScore(long searchId, double score){
		GenericNameFilter fr = new GenericNameFilter("", searchId, true, null, false);
		((GenericNameFilter)fr).setUseSubdivisionNameAsCandidat(true);
		fr.setThreshold(new BigDecimal(score));
		return fr;
	}
	
	public static FilterResponse getNameFilterForGranteeIsSubdivisionWithScore (String saKey,long searchId,
			TSServerInfoModule module,double score){
		GenericNameFilter fr = new GenericNameFilter(saKey,searchId,true,module, false);
    	fr.setThreshold(new BigDecimal(score));
		return fr;
	}
	
	public static FilterResponse getNameFilter(String saKey,long searchId,
			TSServerInfoModule module,double score,double pondereFirst,double pondereMiddle,double pondereLast){
		GenericNameFilter fr = new GenericNameFilter(saKey,searchId,false,module, false);
		fr.setThreshold(new BigDecimal(score));
		return fr;
	}
	
	public static FilterResponse getDefaultTransferNameFilter(long searchId,double score,TSServerInfoModule module){
		FilterResponse fr = new TransferNameFilter(SearchAttributes.GB_MANAGER_OBJECT,searchId,false,module);
		fr.setStrategyType(FilterResponse.STRATEGY_TYPE_LOW_PASS_FILTER);
		fr.setThreshold(new BigDecimal(score));
		return fr;
	}

	public static FilterResponse getPerfectlyMatchNameFilter(String saKey,long searchId,
			TSServerInfoModule module){
		PerfectlyMatchNamesFilter fr = new PerfectlyMatchNamesFilter(saKey, searchId, false, module, true);
		fr.setThreshold(new BigDecimal(NAME_FILTER_THRESHOLD));
		return fr;
	}
	
	public static FilterResponse getNameFilterIgnoreMiddleOnEmpty(String saKey,long searchId,
			TSServerInfoModule module){
		GenericNameFilter fr = (GenericNameFilter) getDefaultNameFilter(saKey,searchId,module);
		fr.setIgnoreMiddleOnEmpty(true);
		return fr;
	}
}

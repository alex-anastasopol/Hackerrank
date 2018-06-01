package ro.cst.tsearch.search.filter.newfilters.legal;

import java.math.BigDecimal;

import ro.cst.tsearch.search.filter.FilterResponse;


public class LegalFilterFactory {
	
	public static FilterResponse getDefaultLegalFilter(long searchId){
		FilterResponse fr = new GenericLegal("", searchId);
		((GenericLegal)fr).setEnableSectionJustForUnplated(false);
		fr.setThreshold(new BigDecimal("0.7"));
		return fr;
	}
	
	public static FilterResponse getDefaultLegalFilter(long searchId, boolean ignoreLotAndBlockForPreferentialDoctype){
		FilterResponse fr = new GenericLegal("", searchId);
		((GenericLegal)fr).setEnableSectionJustForUnplated(false);
		((GenericLegal)fr).setIgnoreLotAndBlockForPreferentialDoctype(ignoreLotAndBlockForPreferentialDoctype);
		fr.setThreshold(new BigDecimal("0.7"));
		return fr;
	}
	public static FilterResponse getDefaultLotFilter(long searchId){
		FilterResponse fr = new GenericLegal("", searchId);
		((GenericLegal)fr).disableAll();
		((GenericLegal)fr).setEnableLot(true);
		
		fr.setThreshold(new BigDecimal("0.7"));
		return fr;
	}
	
	public static FilterResponse getDefaultSubLotFilter(long searchId){
		FilterResponse fr = new GenericLegal("", searchId);
		((GenericLegal)fr).disableAll();
		((GenericLegal)fr).setEnableSubLot(true);
		
		fr.setThreshold(new BigDecimal("0.7"));
		return fr;
	}
	
	public static FilterResponse getDefaultBlockFilter(long searchId){
		FilterResponse fr = new GenericLegal("", searchId);
		((GenericLegal)fr).disableAll();
		((GenericLegal)fr).setEnableBlock(true);
		
		fr.setThreshold(new BigDecimal("0.7"));
		return fr;
	}
	
	public static FilterResponse getDefaultUnitFilter(long searchId){
		FilterResponse fr = new GenericLegal("", searchId);
		((GenericLegal)fr).disableAll();
		((GenericLegal)fr).setEnableUnit(true);
		
		fr.setThreshold(new BigDecimal("0.7"));
		return fr;
	}
	
	public static FilterResponse getLegalFilter(long searchId,double score, boolean enableSection,
			boolean enableTownship,boolean enableRange ,boolean enableLot , 
			boolean enableBlock,boolean enablePlatBook, 
			boolean enablePlatPage,boolean enableUnit,
			boolean enableSectionJustForUnplated){
		
		FilterResponse fr = new GenericLegal("", searchId);
		((GenericLegal)fr).setEnableSectionJustForUnplated(false);
		((GenericLegal)fr).setEnablePlatBook(enablePlatBook);
		((GenericLegal)fr).setEnablePlatPage(enablePlatPage);
		fr.setThreshold(new BigDecimal(score));
		
		return fr;
	}	
	
}

package ro.cst.tsearch.search.filter.newfilters.misc;

import java.math.BigDecimal;
import java.util.Set;
import java.util.Vector;
import org.apache.commons.lang.StringUtils;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;

	public class PropertyTypeFilter extends FilterResponse {

		boolean refIsPlated;
		boolean refIsSectional;
		
		public PropertyTypeFilter(long searchId) {
			super(searchId);
			setThreshold(new BigDecimal(0.8));
			setInitAgain(true);
		}
		
		public void init() {
			Search global = getSearch();
			SearchAttributes sa = global.getSa();
			
			String lot = sa.getAtribute(SearchAttributes.LD_LOTNO);
			String block = sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			String platBook = sa.getAtribute(SearchAttributes.LD_BOOKNO);
			String platPage = sa.getAtribute(SearchAttributes.LD_PAGENO);
			
			String sec = sa.getAtribute(SearchAttributes.LD_SUBDIV_SEC);
			String rg = sa.getAtribute(SearchAttributes.LD_SUBDIV_RNG);
			String tw = sa.getAtribute(SearchAttributes.LD_SUBDIV_TWN);
			
			refIsPlated = StringUtils.isNotBlank(lot+block+platBook+platPage);
			
			Set<LegalStruct> dataSet = (Set<LegalStruct>)global.getAdditionalInfo("TS_LOOK_UP_DATA");
			boolean onePlated = false;
			boolean oneSectional = false;
			if(dataSet != null) {
				for (LegalStruct struct : dataSet) {
					if(struct.isPlated()){
						onePlated = true;
					}
					if(struct.isSectional()||struct.isArb()){
						oneSectional = true;
					}
				}
			}
			
			refIsPlated = refIsPlated || (onePlated&&!oneSectional);
			
			refIsSectional =  !refIsPlated && StringUtils.isNotBlank(sec+tw+rg);
			
			refIsSectional = refIsSectional || (!onePlated&&oneSectional);
		}

		private static final long serialVersionUID = -1877359332961695340L;
	
		@SuppressWarnings("unchecked") // for Vector<PropertyIdentificationSet>
		public BigDecimal getScoreOneRow(ParsedResponse row) {
			
			double finalScore = 1.00d;
			
			boolean candIsPlatedOnDT = false;
			boolean candIsSectionalOnDT = false;
			
			// compute the actual score
			for(PropertyIdentificationSet pis : (Vector<PropertyIdentificationSet>) row.getPropertyIdentificationSet()){
				
				// check if empty
				if(pis == null) { 
					continue; 
				}			
				
				String section  = pis.getAtribute("SubdivisionSection");
			    String township = pis.getAtribute("SubdivisionTownship");
			    String range    = pis.getAtribute("SubdivisionRange");
			    String lot      = pis.getAtribute("SubdivisionLotNumber");
			    String block    = pis.getAtribute("SubdivisionBlock");
			    String platBook = pis.getAtribute("PlatBook"); 
	            String platNo 	= pis.getAtribute("PlatNo");
	            String unit 	= pis.getAtribute("SubdivisionUnit");
	            String qo		= pis.getAtribute("QuarterOrder");
	            String qv		= pis.getAtribute("QuarterValue");
	            String arb 		= pis.getAtribute("ARB");
	            
	            if(StringUtils.isBlank(lot)){
	            	lot = unit;
	            }
	            
	            boolean isPropertyPlated =    !(StringUtils.isBlank(platBook) || StringUtils.isBlank(platNo)) &&  StringUtils.isBlank(section) && StringUtils.isBlank(township) && StringUtils.isBlank(range); 
	            boolean isPropertySectional = !(StringUtils.isBlank(section) && StringUtils.isBlank(township) && StringUtils.isBlank(range)&& StringUtils.isBlank(arb))&& !isPropertyPlated;
	            
	            candIsPlatedOnDT = candIsPlatedOnDT || isPropertyPlated;
	            candIsSectionalOnDT = candIsSectionalOnDT || isPropertySectional;
			}		
			
			if(  (refIsPlated && candIsSectionalOnDT && !candIsPlatedOnDT) 
					|| 
					(refIsSectional && candIsPlatedOnDT && !candIsSectionalOnDT ) ){
				finalScore = 0.00d;
			}
			
			return new BigDecimal(finalScore);
		}	
	
		@Override
	    public String getFilterName(){
	    	return "Filter by Property Type ";
	    }
		
		@Override
		public String getFilterCriteria(){		
	    	return "(PLATED/UNPLATED) but pass ALL when diferent type of legals found";
	    }
		
	}
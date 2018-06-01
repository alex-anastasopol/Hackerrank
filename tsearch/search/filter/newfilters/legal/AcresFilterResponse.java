package ro.cst.tsearch.search.filter.newfilters.legal;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

public class AcresFilterResponse extends FilterResponse{

		
		/**
		 * Serialization 
		 */
		private static final long serialVersionUID = 5643403685740313495L;
		
		/**
		 * Main log category.
		 */
		protected static final Category logger = Category.getInstance(AcresFilterResponse.class.getName());
		
		/**
		 * Details log category.
		 */
		protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + AcresFilterResponse.class.getName());
		/**
		 * Parcel number.
		 */
		protected String parcelNumber;

		/**
		 * Constructor
		 * 
		 * @param instrumentlNumber Parcel number to be matched.
		 */
		public AcresFilterResponse(String saKey, long searchId) {
			super(saKey, searchId);
			threshold = new BigDecimal("0.90");
		}
		
		/**
		 * Obtain candidate PINs 
		 * @param row
		 * @return candidate PINs
		 */
		@SuppressWarnings("unchecked")
		protected Set<String> getCandAcres(ParsedResponse row){
			
			Set<String> acres = new HashSet<String>();
			
			if(row.getPropertyIdentificationSet() == null || row.getPropertyIdentificationSetCount() == 0){
				return acres;
			}
			
			for(PropertyIdentificationSet pis: (Vector<PropertyIdentificationSet>)row.getPropertyIdentificationSet()){
				String a = pis.getAtribute("Acres");
				if(!StringUtils.isEmpty(a)){
					acres.add(a.trim());
				}
			}
			
			return acres;
		}
		
		/**
		 * Obtain reference PIN
		 * @return
		 */
		protected String getRefAcres(){
			return sa.getAtribute(saKey).trim();
		}
		
		/**
		 * Score response for parcel number.
		 * 
		 * @see ro.cst.tsearch.search.filter.FilterResponse#getScoreOneRow(ro.cst.tsearch.servers.response.ParsedResponse)
		 */
		public BigDecimal getScoreOneRow(ParsedResponse row){
			double score = getScoreOneRowInternal(row);
			loggerDetails.debug("match [" + getRefAcres() + "] vs [" + getCandAcres(row) + "] score=" + score);		
	        IndividualLogger.info( "match [" + getRefAcres()+ "] vs [" + getCandAcres(row) + "] score=" + score,searchId );
			return new BigDecimal(score);
		}
		
		/**
		 * Compute score for one row only 
		 * @param row
		 * @return
		 */
		private double getScoreOneRowInternal(ParsedResponse row){
			
			// no reference - score 1
			String refAcres = getRefAcres();
			if(StringUtils.isEmpty(refAcres)){
				return 1.0d;
			}
			double refAcresDouble = -2;
			try {
				refAcresDouble = Double.parseDouble(refAcres);
			} catch (NumberFormatException e) {}
			
			// no candidate - score 1
			Set<String> candAcres = getCandAcres(row);
			if(candAcres.size() ==0 ){
				return 1.0d;
			}		
			
			for(String cAcres:candAcres){
				double cAcresDouble = 0;
				try {
					cAcresDouble = Double.parseDouble(cAcres);
				} catch (NumberFormatException e) {}
				if (cAcresDouble == refAcresDouble){
					return 1.0d;
				}
			}
			// nothing found - score 0
			return 0.0d;
			
		}
		
		@Override
	    public String getFilterName(){
	    	return "Filter by Acres";
	    }
		
		@Override
		public String getFilterCriteria(){
			return "Acres=" + getRefAcres();
		}
}

/**
 * 
 */
package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.response.GranteeSet;
import ro.cst.tsearch.servers.response.GrantorSet;
import ro.cst.tsearch.servers.response.InfSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import static ro.cst.tsearch.utils.StringUtils.*;


/**
 * @author radu bacrau
 *
 */
public class ILCookLACombinedFilterResponse extends FilterResponse {

	static final long serialVersionUID = 10000000;
	
	private static final Logger logger = Logger.getLogger(FilterResponse.class);
	
	private FilterResponse nameFilterOwner;
	private FilterResponse nameFilterBuyer;
	
	/**
	 * @param key
	 * @param searchId
	 */
	public ILCookLACombinedFilterResponse(long searchId) {
		
		super(searchId);
		
		sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		
		GenericNameFilter nameFilter1 = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
        nameFilter1.setIgnoreMiddleOnEmpty(true);
        nameFilterOwner = nameFilter1;

		GenericNameFilter nameFilter2 = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, searchId, null);
        nameFilter2.setIgnoreMiddleOnEmpty(true);
        nameFilterBuyer = nameFilter2;
        
		setThreshold(new BigDecimal("0.93"));
	}
	
	private boolean usePins = false;
	
	public void setUsePins(boolean usePins){
		this.usePins = usePins;
	}
	
	private boolean useNames = false;
	
	public void setUseNames(boolean useNames){
		this.useNames = useNames;
	}

	// refPin, refLastNames are computed only once since the filter instance is not reused
	private Set<String> refLastNames = null;
	private Set<String> refPins = null;
	
	@Override
	@SuppressWarnings("unchecked") // for Vector<PropertyIdentificationSet>
	public BigDecimal getScoreOneRow(ParsedResponse row) {		
		
		// prepare reference PINs and last names
		if(refPins == null || refLastNames == null){
					
			// search attributes
			SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();

			// determine ref PIN
			refPins = new HashSet<String>();				
			for(String pin: sa.getPins(-1)){
				pin = pin.replaceAll("[^0-9]","");
				refPins.add(pin);
				if(pin.length() == 10) { // Bug 6865
					refPins.add(pin + "0000");
				}
			}
			
			// determine ref last names
			refLastNames = new HashSet<String>();				
			for(PartyI party: new PartyI[] {sa.getOwners(), sa.getBuyers()} ){
				for(NameI name: party.getNames()){
					String last = name.getLastName();
					if(!StringUtils.isEmpty(last)){
						refLastNames.add(last);
					}
				}
			}
		}
		
		double finalScore;
		String srcType = row.getOtherInformationSet().getAtribute("SrcType");
		if("PI".equals(srcType)){			
			
			// create vector with all grantors and grantees
			Vector<InfSet> grs = new Vector<InfSet>();
			grs.addAll((Vector<GranteeSet>)row.getGrantorNameSet());
			grs.addAll((Vector<GrantorSet>)row.getGranteeNameSet());
			
			// check last name of all grantors and  grantees		
			if(refLastNames.size() > 0 && grs.size() > 0 && containsLastNames(grs, refLastNames)){
				finalScore = 1.0d;
				if(logger.isInfoEnabled()) logger.info("PI document score = 1.00 because last name match!");
				IndividualLogger.info("PI document score = 1.00 because last name match!", searchId);
			} else if(refPins.size() == 0){
				finalScore = 1.0d;
				if(logger.isInfoEnabled()) logger.info("PI document score = 1.00 because reference PIN is empty!");
				IndividualLogger.info("PI document score = 1.00 because reference PIN is empty!", searchId);
			} else {
				
				boolean foundBadPin = false;
				boolean foundGoodPin = false;
				
				for(PropertyIdentificationSet pis: (Vector<PropertyIdentificationSet>)row.getPropertyIdentificationSet()){
					String candPin = pis.getAtribute("ParcelID");			
					if(!isEmpty(candPin)){
						candPin = candPin.replaceAll("[^0-9]","");
						foundGoodPin = refPins.contains(candPin);
						if(foundGoodPin){
							break; 
						}
						foundBadPin = true;							
					}
				}			
				
				if(foundGoodPin || !foundBadPin){
					finalScore = 1.00d;
					if(logger.isInfoEnabled()) logger.info("PI document score = 1.00 because of no PIN mismatch");	
					IndividualLogger.info("PI document score = 1.00 because of no PIN mismatch", searchId);					
				} else {
					finalScore = 0.00d;					
					if(logger.isInfoEnabled()) logger.info("PI document score = 0.00 because of PIN mismatch!");	
					IndividualLogger.info("PI document score = 0.00 because of PIN mismatch!", searchId);
				}
			}
			
		} else if("GI".equals(srcType)){
			
			// compute name match
			finalScore = 0.00d;
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			if(search.getSa().getOwners().size() != 0){
				double newScore = nameFilterOwner.getScoreOneRow(row).doubleValue();
				if(newScore > finalScore){ finalScore = newScore; }
			}
			if(finalScore < 0.95d && search.getSa().getBuyers().size() != 0){
				double newScore = nameFilterBuyer.getScoreOneRow(row).doubleValue();
				if(newScore > finalScore){ finalScore = newScore; }
			}
			if(logger.isInfoEnabled()) logger.info("GI document score:" + finalScore);
			IndividualLogger.info("GI document score:" + finalScore, searchId);
			
		} else {
			
			// leave SG go through
			if(logger.isInfoEnabled()) logger.info("SG document default score = 1.00");
			IndividualLogger.info("SG document default score = 1.00", searchId);
			finalScore = 1.00d;
		}
		
		return new BigDecimal(finalScore);
	}
	
	private final static double NAME_THRESHOLD = 0.60d;

	/**
	 * Check if a list of name infsets contains any of a list of reference last names
	 * @param grs
	 * @param refLastNames
	 * @return
	 */
	private boolean containsLastNames(Vector<InfSet> grs, Set<String> refLastNames){
		
		// no reference -> MATCH
		if(refLastNames.isEmpty()){
			return true;
		}
		
		double maxScore = 0.00;
		boolean foundCand = false;
		for(InfSet infSet: grs){
			String [] lastNames = new String [] {infSet.getAtribute("OwnerLastName"), infSet.getAtribute("SpouseLastName"), infSet.getAtribute("OwnerFirstName"), infSet.getAtribute("SpouseFirstName")};
			for(String last:  lastNames){
				if(isEmpty(last)){ continue; }
				foundCand = true;
				for(String refLast: refLastNames){
					double newScore = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_REGISTER_NAME_IGNORE_DIFF_NO_TOKENS, refLast, last,searchId)).getScore().doubleValue();
					maxScore = (newScore > maxScore) ? newScore : maxScore;
					if(maxScore >NAME_THRESHOLD){break;}
					String refLast1 = refLast.toUpperCase().replaceAll("[^A-Z0-9]", "");
					String last1 = last.toUpperCase().replaceAll("[^A-Z0-9]", "");
					newScore = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_REGISTER_NAME, refLast1, last1, searchId)).getScore().doubleValue();
					maxScore = (newScore > maxScore) ? newScore : maxScore;
					if(maxScore >NAME_THRESHOLD){break;}
				}
				if(maxScore > NAME_THRESHOLD){break;}
			}
			if(maxScore > NAME_THRESHOLD){ break;}
		}		
		
		// no candidates -> MATCH
		if(!foundCand){
			return true;
		}
		
		return (maxScore > NAME_THRESHOLD);
		
	}
	
	@Override
    public String getFilterName(){
		String retVal = "Filter by ";
		if(usePins){
			retVal += "PIN";
		}
		if(useNames){
			if(usePins){
				retVal +=", ";
			}
			retVal += "Name ";
		}
    	return "PIN, Name";
    }
	
	@Override
	public String getFilterCriteria(){
				
		String retVal = "";
		
		if(usePins){
			retVal += "PIN='" + refPins + "' ";
		}
		
		if(useNames){
			String owners = nameFilterOwner.getFilterCriteria();
			String buyers = nameFilterBuyer.getFilterCriteria();
			retVal += "Owners " + owners + " Buyers " + buyers;
		}
		
		return retVal;
	}

}

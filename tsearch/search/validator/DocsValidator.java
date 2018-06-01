package ro.cst.tsearch.search.validator;

import java.io.Serializable;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.search.filter.matchers.subdiv.SubdivMatcher;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterResponse2;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 */
public class DocsValidator implements Serializable {
	
	FilterResponse fr = null;
    
    static final long serialVersionUID = 10000000;
	
	public static final Category logger = Logger.getLogger(DocsValidator.class);
	
	protected Search search;
	
	private boolean onlyIfNotFiltered = false;
	
	public static int TYPE_ALWAYS_TRUE 			= 1;
	public static int TYPE_DEFAULT 				= 2;
	public static int TYPE_REGISTER 			= 3;
	public static int TYPE_BUYER_NAME_REGISTER 	= 5;
	public static int TYPE_NO_VALIDATION		= 11;
    
	public DocsValidator (Search search){
		if (logger.isDebugEnabled())
			logger.debug("new " + this.getClass().getName());
		this.search = search;
		
	}
	
	public DocsValidator (Search search,FilterResponse fr){		
		this(search);
		this.fr = fr;
		this.search = search;
	}

	/* (non-Javadoc)
	 * @see ro.cst.tsearch.search.validator.DocsValidator#isValid(ro.cst.tsearch.servers.response.ServerResponse)
	 */
	public boolean isValid(ServerResponse response) {
		if(fr!=null){
			if(fr.isInitAgain()) {
				fr.init();
			}
			double scoreRow = fr.getScoreOneRow(response.getParsedResponse()).doubleValue();
			double threshold = fr.getThreshold().doubleValue();
			if(fr.getStrategyType()==FilterResponse.STRATEGY_TYPE_LOW_PASS_FILTER)
				return  scoreRow <= threshold;
			else
				return  scoreRow >= threshold;
			
		}
		return true;
	}

    
	public static DocsValidator getInstance(int type, Search search, DocsValidator dv,long searchId){
		if (type == TYPE_REGISTER) {
			return new RegisterDocsValidator(search);
		} else if(type == TYPE_NO_VALIDATION) {
		    return new DocsValidator(search);	  
		} else if ((type == TYPE_DEFAULT)&&(dv != null)) {
		    return dv;
		}
			
		else { //TYPE_ALWAYS_TRUE
		    return new RegisterDocValidatorParentSite(search);
		}
	}
	
	
	public static boolean validLot( ServerResponse response, SearchAttributes sa ,long searchId){
	
	///added the code fom LotVAlidator.java it semes that this validator is smarter

		String reflot= StringFormats.ReplaceIntervalWithEnumeration(sa.getAtribute(SearchAttributes.LD_LOTNO));
		
		if( reflot == null || "".equals( reflot ) ) { 
			return true;
		}

		String refLotList[] = reflot.split("[ ,]");
		
		boolean foundGood = false;
		boolean foundAny = false;
		
		for (PropertyIdentificationSet pis : (Vector<PropertyIdentificationSet>) response.getParsedResponse().getPropertyIdentificationSet()){
			String candLot = pis.getAtribute("SubdivisionLotNumber");
			if(StringUtils.isEmpty(candLot)){
				continue;
			}
			foundAny = true;
			String candlotInterval = StringFormats.ReplaceIntervalWithEnumeration(candLot);				
			String candLotList[] = candlotInterval.split( "[ ,]" );
				
			for(int i = 0 ; i < refLotList.length ; i++){
				for(int j = 0 ; j < candLotList.length ; j ++){
					candLotList[j] = candLotList[j].replaceAll( "^(0+)(.*)$" , "$2" );
					refLotList[i] = refLotList[i].replaceAll( "^(0+)(.*)$" , "$2" );
					if(refLotList[i].equals(candLotList[j])){
						foundGood = true;
						break;
					}
				}
			}
		}
		
		if(foundAny)
			return foundGood;
		else
			return true;	//valid if we cannot decide
		
	}
	
    public String getValidatorName(){
    	if(fr==null){
    		return this.getClass().getSimpleName();
    	}
    	return fr.getFilterCriteria();
    }

	public FilterResponse getFilter() {
		return fr;
	}

	public boolean isOnlyIfNotFiltered() {
		return onlyIfNotFiltered;
	}

	public void setOnlyIfNotFiltered(boolean onlyIfNotFiltered) {
		this.onlyIfNotFiltered = onlyIfNotFiltered;
	}
}

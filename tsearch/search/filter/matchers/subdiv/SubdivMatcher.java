package ro.cst.tsearch.search.filter.matchers.subdiv;

import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.log4j.Category;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.MatchEquivalents;
import ro.cst.tsearch.utils.SearchLogger;

/**
 * @author elmarie
 */
public class SubdivMatcher implements Serializable {
    
    static final long serialVersionUID = 10000000;

	private static final Category logger = Category.getInstance(SubdivMatcher.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + SubdivMatcher.class.getName());

	protected int lotMatchAlgorithm = MatchAlgorithm.TYPE_LOT_ZERO_IGNORE;
	protected int subdivMatchAlgorithm = MatchAlgorithm.TYPE_REGISTER_NAME_NA;

	
	public BigDecimal getScore(PropertyIdentificationSet ref, PropertyIdentificationSet cand) {
		int score = validSubdivision(ref,cand); 
		if (logger.isDebugEnabled())
			logger.debug(" this subdiv is valid = " + score);
		
		if (score==-1 || score==1 ){
			return ATSDecimalNumberFormat.ONE;
		}else{
			return ATSDecimalNumberFormat.ZERO;
		}
	}

	protected long searchId=-1;
	protected transient Search search = null;
	protected transient String p1 = null;
	protected transient String p2 = null;
	protected transient String siteName = "";
	protected transient boolean allowSubdivisionNameTest = true;
	public SubdivMatcher(long searchId){
		this.searchId = searchId;
		loadExtraFields(searchId);
		
	}
	
	protected void loadExtraFields(long searchId){
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		search = currentInstance.getCrtSearchContext();
		if(search != null ) {
			p1 = search.getP1();
	        p2 = search.getP2();
	        try{
	        	siteName = TSServersFactory.getTSServerName(currentInstance.getCommunityId(), p1, p2);
	        }
	        catch(Exception e){
	        	e.printStackTrace();
	        	siteName = "";
	        }
			if(siteName.startsWith("TN"))
				allowSubdivisionNameTest = false;
		} else {
			logger.warn("SubdivMatcher - loadExtraFields: current instance is null for searchId = " + searchId);
		}
	}
	
	public int validSubdivision(PropertyIdentificationSet pisRef, PropertyIdentificationSet pisCand) {
		int validSubdivName = validSubdivNameSecPhaseOrCode(pisRef, pisCand);
		int validLot = validPisAtt ("SubdivisionLotNumber",pisRef, pisCand, lotMatchAlgorithm, "0.8");
		int validUnit = validPisAtt("SubdivisionUnit",pisRef,pisCand,MatchAlgorithm.TYPE_GENERIC_MATCH,"0.90");
		int validBlock = validPisAtt("SubdivisionBlock",pisRef,pisCand,MatchAlgorithm.TYPE_GENERIC_MATCH,"0.90");
		
		if (validSubdivName == 0 || validLot == 0 || validUnit == 0 || validBlock == 0){
			return 0;
		} else if (validSubdivName == 1 || validLot == 1 || validUnit == 1 || validBlock == 1){
			return 1;
		} else {
			return -1;
		}
	}
	
	
	private String subdivisionNameThreshold = "0.7";
	public void setSubdivisionNameThreshold(String newThreshold){
		subdivisionNameThreshold = newThreshold;
	}

	public int validOneSubdivNameSecPhase(PropertyIdentificationSet pisRef, PropertyIdentificationSet pisCand) {
		int validSec = validPisAtt ("SubdivisionSection",pisRef, pisCand, MatchAlgorithm.TYPE_GENERIC_MATCH, "0.8");
		int validPhase = validPisAtt ("SubdivisionPhase",pisRef, pisCand, MatchAlgorithm.TYPE_GENERIC_MATCH, "0.8");
		int validSubdivName = -1;
		
        
		if(search == null)	//to be extra sure
			loadExtraFields(searchId);
		
		if(allowSubdivisionNameTest)
			validSubdivName = validPisAttMatchEquivalents ("SubdivisionName",pisRef, pisCand, subdivMatchAlgorithm, "0.6");
		else {
			SearchLogger.info("Subdivision name test not used for " + siteName + "<br>", searchId);
			IndividualLogger.info("Subdivision name test not used for " + siteName + "<br>", searchId);
		}

		if ((validSec == 0)||(validPhase == 0)||(validSubdivName == 0)){
			return 0;
		}else if ((validSec == 1)||(validPhase == 1)||(validSubdivName == 1)){
			return 1;
		}else {
			return -1;
		}
	}

	public int validSubdivNameSecPhaseOrCode(PropertyIdentificationSet pisRef, PropertyIdentificationSet pisCand) {
		int validSubdivNameSecPhase = validOneSubdivNameSecPhase(pisRef, pisCand);
		
		int validCode = validPisAtt ("SubdivisionCode",pisRef, pisCand, subdivMatchAlgorithm, "0.8");

		if ((validSubdivNameSecPhase == 1)||(validCode== 1)){
			return 1;
		}else if ((validSubdivNameSecPhase == 0)||(validCode== 0)){
			return 0;
		}else {
			return -1;
		}
	}
	
	private int validString (String ref, String cand,  int matcherType, String threshold){
		
		BigDecimal score = (MatchAlgorithm.getInstance(matcherType, ref, cand,searchId)).getScore();
		loggerDetails.debug("comparing " +  "... [" + ref + "] with [" + cand + "] = " + score);
		IndividualLogger.info( "comparing " + "... [" + ref + "] with [" + cand + "] = " + score ,searchId);
		
		if (score == ATSDecimalNumberFormat.NA){
			return -1;
		}else if (score.compareTo(new BigDecimal(threshold))>=0){
			return 1;
		}else {
			return 0;
		}
	}
	
	protected int validPisAtt (String pisAttrib, PropertyIdentificationSet pisRef, PropertyIdentificationSet pisCand,  int matcherType, String threshold){
		String ref = pisRef.getAtribute(pisAttrib);
		String cand = pisCand.getAtribute(pisAttrib);
		cand  = GenericFunctions1.replaceNumbers(cand);
		//boolean equal = StringUtils.compareMultipleInts(lotNo, lotNo1);
		BigDecimal score = (MatchAlgorithm.getInstance(matcherType, ref, cand,searchId)).getScore();
		loggerDetails.debug("comparing " + pisAttrib + "... [" + ref + "] with [" + cand + "] = " + score);
		IndividualLogger.info( "comparing " + pisAttrib + "... [" + ref + "] with [" + cand + "] = " + score,searchId );
		
		if (score == ATSDecimalNumberFormat.NA){
			return -1;
		}else if (score.compareTo(new BigDecimal(threshold))>=0){
			return 1;
		}else {
			return 0;
		}
	}

	/**
	 * function applies MatchEquivalents prior to match algorithm
	 * @param pisAttrib
	 * @param pisRef
	 * @param pisCand
	 * @param matcherType
	 * @param threshold
	 * @return
	 */
	protected int validPisAttMatchEquivalents( String pisAttrib, PropertyIdentificationSet pisRef, PropertyIdentificationSet pisCand,  int matcherType, String threshold ){
		String ref = MatchEquivalents.getInstance(searchId).getEquivalent( pisRef.getAtribute(pisAttrib) );
		String cand = MatchEquivalents.getInstance(searchId).getEquivalent( pisCand.getAtribute(pisAttrib) );
		
		//boolean equal = StringUtils.compareMultipleInts(lotNo, lotNo1);
		BigDecimal score = (MatchAlgorithm.getInstance(matcherType, ref, cand,searchId)).getScore();
		loggerDetails.debug("comparing " + pisAttrib + "... [" + ref + "] with [" + cand + "] = " + score);
		IndividualLogger.info( "comparing " + pisAttrib + "... [" + ref + "] with [" + cand + "] = " + score ,searchId);
		
		if (score == ATSDecimalNumberFormat.NA){
			return -1;
		}else if (score.compareTo(new BigDecimal(threshold))>=0){
			return 1;
		}else {
			
			if(ref!=null&&cand!=null){
				int min = Math.min(ref.length(), cand.length());
				if(min>4){
					if(ref.contains(cand)||cand.contains(ref)){
						return -1;
					}
				}
			}
			
			return 0;
		}		
	}

	public void setLotMatchAlgorithm(int i) {
		lotMatchAlgorithm = i;
	}

	public void setSubdivMatchAlgorithm(int i) {
		subdivMatchAlgorithm = i;
	}

}

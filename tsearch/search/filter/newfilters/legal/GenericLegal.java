package ro.cst.tsearch.search.filter.newfilters.legal;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.GeneralIntervalEnumeration;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.property.PropertyI;

/**
 * 	@author radu bacrau
 *	@author cristi stochina
 */
public class GenericLegal extends FilterResponse {


	// used to get rid of the compiler warning 
	protected static final long serialVersionUID = 13214324435L;
	
	// logger
	protected static final Logger logger = Logger.getLogger(GenericLegal.class);

	protected boolean caseSensitive = false;
	protected boolean doNotIntervalExpand = false;
	
	public GenericLegal(long searchId) {
		super(searchId);	
		setThreshold(new BigDecimal("0.7"));
	}

	public GenericLegal(String key, long searchId) {
		super(key, searchId);		
		setThreshold(new BigDecimal("0.7"));
	}

	public GenericLegal(SearchAttributes sa1, String key, long searchId) {
		super(sa1, key, searchId);
		setThreshold(new BigDecimal("0.7"));
	}
	
	// cache reference values, not to load them each time
	protected String refSection = "";
	protected String refTownship = "";
	protected String refRange = "";
	protected String refLot = "";
	protected String refSubLot = "";
	protected String refBlock = "";
	protected String refPlatBook="";
	protected String refPlatPage="";
	protected String refUnit="";
	protected String refPhase="";
	protected String refArb="";
	protected String refDistrict = "";
	protected String refAbs = "";
	
	// reference values cached/loaded
	protected boolean refLoaded = false;
	
	// control if a certain field is used or not
	protected boolean enableSection = true;
	protected boolean enableTownship = true;
	protected boolean enableRange = true;
	protected boolean enableLot = true;
	protected boolean enableSubLot = true;
	protected boolean enableBlock = true;
	protected boolean enableArb = true;
	protected boolean enablePlatBook = true;
	protected boolean enablePlatPage = true;
	protected boolean enableUnit = true;
	protected boolean enablePhase = true;
	protected boolean enableSectionJustForUnplated = false;
	protected boolean enableLotUnitFullEquivalence = false;
	protected boolean enableDistrict = false;
	protected boolean enableAbs = false;
	
	protected boolean markIfCandidatesAreEmpty = false;
	
	protected HashSet<String> ignoreLotWhenServerDoctypeIs = null;
	
	protected boolean ignoreLotAndBlockForPreferentialDoctype = false;
	protected Set<String> preferentialDoctypes = new HashSet<String>(){
		private static final long serialVersionUID = 1L;
	{
		add(DocumentTypes.PLAT);
		add(DocumentTypes.EASEMENT);
		add(DocumentTypes.RESTRICTION);
	}};
	
	
	public void disableAll(){
		enableSection = false;
		enableTownship = false;
		enableRange = false;
		enableLot = false;
		enableSubLot = false;
		enableBlock = false;
		enablePlatBook = false;
		enablePlatPage = false;
		enableUnit = false;
		enablePhase = false;
		enableSectionJustForUnplated = false;
		enableArb = false;
		enableAbs = false;
	}
	
	protected static Pattern patNumbers = Pattern.compile("[0-9]+[a-zA-Z]*");
	
	/**
	 * Compute the score of matching integer value candidate with integer or interval reference
	 * If we cannot understand one of them, consider match 
	 * @param refValue 
	 * @param caseSensitive
	 * @param doNotExpandInterval 
	 * @param candValue
	 * @return 1.0 in case of match, 0.0 in case of not match
	 */
	public static double computeScoreInternal(String element, String candVal, String refValue, boolean caseSensitive, boolean doNotExpandInterval){
		
		// empty ref or cand => match
		if(isEmpty(refValue) || isEmpty(candVal) || candVal.equals(refValue)){
			return 1.0;
		}
		
		if( "platBook".equalsIgnoreCase(element)  ){
			if(candVal.toLowerCase().startsWith("p") && candVal.length()>1 ){
				candVal = candVal.substring(1);
			}
			if(refValue.toLowerCase().startsWith("p") && refValue.length()>1 ){
				refValue = refValue.substring(1);
			}
		}
		
		if(!caseSensitive){
			candVal  = candVal.toUpperCase();
			refValue = refValue.toUpperCase();
		}
		
		candVal = candVal.trim();
		candVal = candVal.replaceAll("\\s*-\\s*", "-");
		refValue = refValue.replaceAll("\\s*-\\s*", "-");
		Set<String> cands = new LinkedHashSet<String>();
		cands.add( candVal ) ;
		
		String []allCands = candVal.split("[ ,]");		
		
		// cands.addAll(Arrays.asList(allCands));
		
		for(String cand: allCands){
			
			if(doNotExpandInterval) {
				cands.add(cand);
			} else {
				for(LotInterval interval: (Vector<LotInterval>)LotMatchAlgorithm.prepareLotInterval(cand)){
					cands.addAll(interval.getLotList(500));
				}
			}
		}
		
		// pre-process
		if(!doNotExpandInterval) {
			refValue = refValue.trim().replaceAll("[^A-z0-9]","");
			if(refValue.startsWith("0")){
				refValue  = refValue.replaceAll("([0]+)([A-z1-9][A-z0-9]*)", "$2");
			}
		}
		double max = 0.0d;	
		
		for(String candValue: cands){			
			
			if(doNotExpandInterval) {
				//no processing
				
			} else {
				candValue = candValue.trim().replaceAll("[^A-z0-9]","");
				if(candValue.startsWith("0")){
					candValue = candValue.replaceAll("([0]+)([A-z1-9][A-z0-9]*)", "$2");
				}
			}
			
			// equal strings => match
			if(candValue.equals(refValue)){
				max = Math.max(max, 1.0);
			}
			else if (candValue.startsWith(refValue)){	
				String temp = candValue.substring(refValue.length(),candValue.length());
				Matcher mat 	= patNumbers.matcher(temp);
				if(!mat.find()){
					max = Math.max(max, 0.8);
				}
			} else if (refValue.startsWith(candValue)){
				String temp = refValue.substring(candValue.length(),refValue.length());
				Matcher mat 	= patNumbers.matcher(temp);
				if(!mat.find()){
					max = Math.max(max, 0.8);
				}
			} else if (refValue.endsWith(candValue)){
				int refLength = refValue.length();
				int candLength = candValue.length();
				//the part in front of the candidate string
				String firstPart = refValue.substring(0, refLength - candLength);
				//the candidate
				String secondPart = refValue.substring(refLength - candLength);
				
				if(secondPart.matches("\\d+") && firstPart.matches("[a-zA-Z]+")) {
					max = Math.max(max, 0.8);
				}
			} else if (candValue.endsWith(refValue)){
				int refLength = refValue.length();
				int candLength = candValue.length();
				//the part in front of the candidate string
				String firstPart = candValue.substring(0, candLength - refLength);
				//the candidate
				String secondPart = candValue.substring(candLength - refLength);
				
				if(secondPart.matches("\\d+") && firstPart.matches("[a-zA-Z]+")) {
					max = Math.max(max, 0.8);
				}
			}
		}
		
		return max;
	}
	
	
	/**
	 * Compute the score of matching integer value candidate with integer or interval reference
	 * If we cannot understand one of them, consider match
	 * @param fieldName name of the field, to be used for logging 
	 * @param candValue integer
	 * @param refValue integer or interval like 3-5
	 * @return 1.0 in case of match, 0.0 in case of not match
	 */
	protected double computeScore(String fieldName, String candValue, String refValue){
		double score = computeScoreInternal(fieldName, candValue, refValue, caseSensitive, isDoNotIntervalExpand());
		//IndividualLogger.info("Comparing " + fieldName + " cand=" + candValue + " ref=" + refValue + ". score =" + score, searchId);
		return score;		
	}
	
	@Override
	@SuppressWarnings("unchecked") // for Vector<PropertyIdentificationSet>
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		
		loadRefLegal();
		
		if(row == null) {
			return ATSDecimalNumberFormat.ONE;
		}
		
		String a[] ={"X", "L", "C", "D", "M"};
		
		Set<String> refLotSet = new HashSet<String>();
		if(refLot!=null){
			if(isDoNotIntervalExpand()) {
				refLotSet.add(refLot);
			} else {
				Vector<LotInterval> lots = LotMatchAlgorithm.prepareLotInterval(refLot);
				for (int i = 0; i < lots.size(); i++) {
					refLotSet.addAll(lots.elementAt(i).getLotList());
				}
			}
		}
		
		Set<String> refPlatPageSet = new HashSet<String>();
		if(refPlatPage!=null){
			Vector<LotInterval> platPages = LotMatchAlgorithm.prepareLotInterval(refPlatPage);
			for (int i = 0; i < platPages.size(); i++) {
				refPlatPageSet.addAll(platPages.elementAt(i).getLotList());
			}
		}

		double finalScore = 0.00d;
		
		List<String> refBlocks = new GeneralIntervalEnumeration(refBlock).getEnumerationList();
		
		boolean foundSomething = false;
		
		Vector<PropertyIdentificationSet> propertiesToAnalyze = null;
		if(!row.getPropertyIdentificationSet().isEmpty()) {
			propertiesToAnalyze = (Vector<PropertyIdentificationSet>) row.getPropertyIdentificationSet();
		} else {
			Object possibleVector = row.infVectorSets.get("PropertyIdentificationSet");
			if (possibleVector instanceof Vector) {
				try {
					propertiesToAnalyze = (Vector<PropertyIdentificationSet>) possibleVector;
				} catch (Exception e) {
					logger.error("Error while reading possibleVector", e);
				}
				
			}
		}
		if(propertiesToAnalyze == null) {
			propertiesToAnalyze = new Vector<PropertyIdentificationSet>();
		}
		
		
		List<GenericLegalStruct> legals = new ArrayList<GenericLegal.GenericLegalStruct>();
		
		for(PropertyIdentificationSet pis : propertiesToAnalyze){
			GenericLegalStruct genericLegalStruct = new GenericLegalStruct();
			if(genericLegalStruct.loadFromPis(pis)) {
				legals.add(genericLegalStruct);
			}
		}
		
		if(legals.isEmpty()) {
			DocumentI document = row.getDocument();
			if(document != null && document.getProperties() != null) {
				for (PropertyI property : document.getProperties()) {
					GenericLegalStruct genericLegalStruct = new GenericLegalStruct();
					if(genericLegalStruct.loadFromProperty(property)) {
						legals.add(genericLegalStruct);
					}
				}
			}
		}
		
		// compute the actual score
		for(GenericLegalStruct genericLegalStruct : legals){
			
			// check if empty
			if(genericLegalStruct.isEmpty()) { 
				continue; 
			}			
			
			String section  = genericLegalStruct.section;
		    String township = genericLegalStruct.township;
		    String range    = genericLegalStruct.range;
		    String lot      = genericLegalStruct.lot;
		    String subLot   = genericLegalStruct.subLot;
		    String block    = genericLegalStruct.block;
		    String lotThrough      = genericLegalStruct.lotThrough;
		    String blockThrough    = genericLegalStruct.blockThrough;
		    String platBook = genericLegalStruct.platBook; 
            String platNo 	= genericLegalStruct.platNo;
            String unit 	= genericLegalStruct.unit;
            String phase	= genericLegalStruct.phase;
            String arb      = genericLegalStruct.arb;
            String unitAddress = genericLegalStruct.unitAddress;
            String district = genericLegalStruct.district;
            String abs = genericLegalStruct.abs;
            
            if(ignoreLotWhenServerDoctypeIs != null) {
            	String serverDoctype = null;
            	try {
            		serverDoctype = ((ro.cst.tsearch.servers.response.SaleDataSet)row.getSaleDataSet().get(0)).getAtribute("DocumentType").trim();
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            	if(StringUtils.isNotEmpty(serverDoctype) && ignoreLotWhenServerDoctypeIs.contains(serverDoctype)) {
            		//SearchLogger.info("Ignoring lot since this document is a " + serverDoctype + "<br>", searchId);
            		lot = "";
            		lotThrough = "";
            	}
            }
            
            if (ignoreLotAndBlockForPreferentialDoctype){
            	String serverDoctype = null;
            	try {
            		serverDoctype = ((ro.cst.tsearch.servers.response.SaleDataSet)row.getSaleDataSet().get(0)).getAtribute("DocumentType").trim();
            		serverDoctype = DocumentTypes.getDocumentCategory(serverDoctype, searchId);
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            	if(StringUtils.isNotEmpty(serverDoctype) && preferentialDoctypes.contains(serverDoctype)) {
            		lot = "";
            		lotThrough = "";
            		block = "";
            		blockThrough = "";
            	}
            }
            

            if("ALL".equalsIgnoreCase(lot) || "ALL".equalsIgnoreCase(lotThrough)){
            	lot = "";
            	lotThrough = "";
            }
            
            if("ALL".equalsIgnoreCase(block)|| "ALL".equalsIgnoreCase(blockThrough)){
            	block = "";
            	blockThrough = "";
            }
            
            
            if(StringUtils.isNotEmpty(lotThrough)) {
            	if(StringUtils.isEmpty(lot)) {
            		lot = lotThrough;
            	} else {
            		if(!lot.equals(lotThrough)) {
            			lot = lot + "-" + lotThrough;
            		}
            	}
            }
            
            if(StringUtils.isNotEmpty(blockThrough)) {
            	if(StringUtils.isEmpty(block)) {
            		block = blockThrough;
            	} else {
            		if(!block.equals(blockThrough)) {
            			block = block + "-" + blockThrough;
            		}
            	}
            }
            
            if(StringUtils.isEmpty(unit)) {
            	unit = unitAddress;
            	unitAddress = "";
            }
            
			// check if anything relevant
            if( 	enableSection 	&& !StringUtils.isEmpty(section) 	&& !StringUtils.isEmpty(refSection)	||
	            	enableTownship 	&& !StringUtils.isEmpty(township) 	&& !StringUtils.isEmpty(refTownship)||
	            	enableRange 	&& !StringUtils.isEmpty(range) 		&& !StringUtils.isEmpty(refRange)	||
	            	enableLot 		&& !StringUtils.isEmpty(lot) 		&& !StringUtils.isEmpty(refLot)		||
	            	enableSubLot 	&& !StringUtils.isEmpty(subLot) 	&& !StringUtils.isEmpty(refSubLot)	||
	            	enableBlock 	&& !StringUtils.isEmpty(block) 		&& !StringUtils.isEmpty(refBlock) 	||
	            	enablePlatBook 	&& !StringUtils.isEmpty(platBook) 	&& !StringUtils.isEmpty(refPlatBook)||
	            	enablePlatPage 	&& !StringUtils.isEmpty(platNo) 	&& !StringUtils.isEmpty(refPlatPage)||
	            	enableUnit 		&& (!StringUtils.isEmpty(unit) || !StringUtils.isEmpty(unitAddress)) 	&& !StringUtils.isEmpty(refUnit)||
	            	enablePhase 	&& !StringUtils.isEmpty(phase) 	&& !StringUtils.isEmpty(refPhase)||
	            	(
	            			(enableUnit || enableLot) && !enableLotUnitFullEquivalence &&
	            			(	(	
	            					(!StringUtils.isEmpty(unit) || !StringUtils.isEmpty(unitAddress)) && !StringUtils.isEmpty(refLot)
	            				)
	            				||
	            				(
	            					!StringUtils.isEmpty(refUnit) 	&& !StringUtils.isEmpty(lot)
	            				) 
	            			)  
	            	) ||
	            	enableDistrict	&&  !StringUtils.isEmpty(district) 	&& !StringUtils.isEmpty(refDistrict)||
	            	enableAbs	&&  !StringUtils.isEmpty(abs) 	&& !StringUtils.isEmpty(refAbs)||
	            	enableArb		&&	StringUtils.isNotEmpty(arb)		&&	StringUtils.isNotEmpty(refArb)
            		){
	            foundSomething = true;
            } else {
            	continue;
            }

            boolean findLotZero = false;
            boolean findGoodLot = false;
            
            for(String cur : refLotSet){
            	if(cur.replaceAll("[0]+", "0").equals("0") ){
            		findLotZero = true;
            	}
            	else{
            		findGoodLot = true;
            	}
            }
            
            HashSet<String> allLots = new HashSet<String>();
            allLots.add(lot);
            if(!StringUtils.isEmpty(lot) ){
            	if(lot.startsWith("L") && lot.length()>=2 ){
            		 allLots.add(lot.substring(1));
            	}
            }
            
            HashSet<String> allBlocks = new HashSet<String>();
            allBlocks.add(block);
            if(!StringUtils.isEmpty(block) ){
            	if(block.startsWith("B") && block.length()>=2 ){
            		allBlocks.add(block.substring(1));
            	}
            }
            
		    double lotScore = 0;
		    for(String lot1:allLots){
			    for(String cur : refLotSet){
			    	double scor =0.0d;
			    	if( findGoodLot && findLotZero ){ //we do not consider 0 lot like nothing
			    		scor = computeScore( "lot", lot1,   cur.replaceAll( "^(0+)(.+)$" , "$2" )  );
			    	}
			    	else{
			    		scor = computeScore( "lot", lot1,   cur.replaceAll( "^(0+)(.*)$" , "$2" )  );
			    	}
			    	if( lotScore<scor ){
			    		lotScore = scor;
			    	}
			    }
		    }
		    
		    double subLotScore = computeScore( "sublot", subLot, refSubLot);
		    
			double blockScore = computeScore("block", block, refBlock);
			for (String block1 : allBlocks) {
				for (String blockElement : refBlocks) {
					double scor = computeScore("block", block1, blockElement);
					if (blockScore < scor) {
						blockScore = scor;
					}
				}
			}
		    
		    double platBookScore     = computeScore("platBook",      platBook,      refPlatBook);
		    
		    double platPageScore = 0;
		    for(String current : refPlatPageSet){
		    	double scor = computeScore("platPage", platNo, current);
			    if( platPageScore<scor ){
			    	platPageScore = scor;
			    }
			}
		    
		    double sectionScore=1.0;
		    double townhshipScore =1.0;
		    double rangeScore     =1.0;
		    
		    if(enableSectionJustForUnplated){
			    if( StringUtils.isEmpty(platBook) && StringUtils.isEmpty(platNo) ){
				    sectionScore   = computeScore("section",  section,  refSection);
				    townhshipScore = computeScore("township", township, refTownship);
				    rangeScore     = computeScore("range",    range,    refRange);
			    }
		    }
		    else{
		    	sectionScore   = computeScore("section",  section,  refSection);
			    townhshipScore = computeScore("township", township, refTownship);
			    rangeScore     = computeScore("range",    range,    refRange);
		    }
		    
		    double unitScore = computeScore("unit",unit,   refUnit  );
		    
		    boolean hasRefUnit = !StringUtils.isEmpty(refUnit);
		    boolean hasRefLot = !StringUtils.isEmpty(refLot);
		    boolean hasRefSubLot = !StringUtils.isEmpty(refSubLot);
		    boolean hasRefPP =  !StringUtils.isEmpty(refPlatPage);
		    boolean hasRefPB =  !StringUtils.isEmpty(refPlatBook);
		    boolean hasRefBlock=  !StringUtils.isEmpty(refBlock);
		    boolean hasRefSec=  !StringUtils.isEmpty(refSection);
		    boolean hasRefTw=  !StringUtils.isEmpty(refTownship);
		    boolean hasRefRg=  !StringUtils.isEmpty(refRange);
		    
		    boolean hasCandUnit = !StringUtils.isEmpty(unit);
		    boolean hasCandLot = !StringUtils.isEmpty(lot);
		    boolean hasCandSubLot = !StringUtils.isEmpty(subLot);
		    boolean hasCandPP =  !StringUtils.isEmpty(platNo);
		    boolean hasCandPB =  !StringUtils.isEmpty(platBook);
		    boolean hasCandBlock = !StringUtils.isEmpty(block);
		    boolean hasCandSec=  !StringUtils.isEmpty(section);
		    boolean hasCandTw=  !StringUtils.isEmpty(township);
		    boolean hasCandRg=  !StringUtils.isEmpty(range);
		    
		    //for common situations like condominiums where you have lot at unit or viceversa
		    double unitLotScore = 0.0d;
		    if( hasRefUnit && hasCandLot ){
		    	if( !hasRefLot && !hasCandUnit && !enableLotUnitFullEquivalence){
		    		unitLotScore  =  computeScore("unitlot",  lot,  refUnit);
		    		unitScore =unitLotScore;
		    	}
		    }
		    
		   //for common situations like condominiums where you have lot at unit or viceversa
		    double lotUnitScore = 0.0d;
		    if( hasRefLot && hasCandUnit ){
		    	if(!hasRefUnit && !hasCandLot && !enableLotUnitFullEquivalence){
		    		lotUnitScore  = computeScore("unitlot",  unit,  refLot);
			    	lotScore = lotUnitScore;
		    	}
		    }
		    
		    if(StringUtils.isNotEmpty(unitAddress)) {
		    	double unitAddressScore = computeScore("unit",unitAddress,   refUnit  );
			    double lotAddressScore = 0;
			    boolean hasCandUnitAddress = !StringUtils.isEmpty(unitAddress);
			    
			    
			    //for common situations like condominiums where you have lot at unit or viceversa
			    unitLotScore = 0.0d;
			    if( hasRefUnit && hasCandLot ){
			    	if( !hasRefLot && !hasCandUnitAddress && !enableLotUnitFullEquivalence){
			    		unitLotScore  =  computeScore("unitlot",  lot,  refUnit);
			    		unitScore =unitLotScore;
			    	}
			    }
			    
			   //for common situations like condominiums where you have lot at unit or viceversa
			    lotUnitScore = 0.0d;
			    if( hasRefLot && hasCandUnit ){
			    	if(!hasRefUnit && !hasCandLot && !enableLotUnitFullEquivalence){
			    		lotUnitScore  = computeScore("unitlot",  unitAddress,  refLot);
			    		lotAddressScore = lotUnitScore;
			    	}
			    }
			    
			    
			    if (unitScore < unitAddressScore) {
			    	unitScore = unitAddressScore;
			    }
			    if (lotScore < lotAddressScore) {
			    	lotScore = lotAddressScore;
			    }
		    }
		    
		    phase = ro.cst.tsearch.extractor.xml.GenericFunctions1.replaceNumbers(phase);
		    
		    phase = Roman.normalizeRomanNumbersExceptTokens(phase, a);
		    double phaseScore = computeScore("phase", phase,   refPhase );
		    
		    double arbScore = computeScore("arb", arb,   refArb);
		    
		    district = ro.cst.tsearch.extractor.xml.GenericFunctions1.replaceNumbers(district);
		    district = Roman.normalizeRomanNumbersExceptTokens(district, a);
		    double districtScore = computeScore("district", district,   refDistrict);
		    
		    double absScore = computeScore("abs", abs,   refAbs);
		    
		    double sum = 0, cnt = 0;
		    double min = 1.0;
		    
		    if(enableSection){  sum += sectionScore;   cnt += 1; if(sectionScore < min)  { min = sectionScore;} }
		    if(enableTownship){ sum += townhshipScore; cnt += 1; if(townhshipScore < min){ min = townhshipScore;} }
		    if(enableRange){    sum += rangeScore;     cnt += 1; if(rangeScore < min)    { min = rangeScore;} }
		    if(enableLot){      sum += lotScore;       cnt += 1; if(lotScore < min)      { min = lotScore;} }
		    if(enableSubLot){   sum += subLotScore;    cnt += 1; if(subLotScore < min)   { min = subLotScore;} }
		    if(enableBlock){    sum += blockScore;     cnt += 1; if(blockScore < min)    { min = blockScore;} }
		    if(enablePlatBook){sum+=platBookScore; cnt += 1; if(platBookScore < min)    { min = platBookScore;}  }
		    if(enablePlatPage){sum+=platPageScore; cnt+=1;  if(platPageScore < min)    { min = platPageScore;}  }
		    if(enableUnit){sum+=unitScore; cnt+=1;  if(unitScore < min)    { min = unitScore;}  }
		    if(enablePhase){sum+=phaseScore; cnt+=1;  if(phaseScore< min)    { min = phaseScore;}  }
		    if(enableArb){sum+=arbScore; cnt+=1;  if(arbScore< min)    { min = arbScore;}  }
		    if(enableDistrict){sum += districtScore; cnt += 1; if (districtScore < min)		{min = districtScore;}  }
		    if(enableAbs){sum += absScore; cnt += 1; if (absScore < min)		{min = absScore;}  }
		    
		    if(cnt == 0){ finalScore = 1.0; break; }		    
		    double score = (  sum / cnt  +  min  ) / 2;
		    if(score > finalScore){ finalScore = score; }		    
		    if(finalScore > 0.99){ break; }		    
		}		
		
		BigDecimal finalResult = new BigDecimal(finalScore);
		
		if(!foundSomething){
			finalResult = ATSDecimalNumberFormat.ONE;
			if(markIfCandidatesAreEmpty) {
				finalResult = ATSDecimalNumberFormat.NA;
			}
		}
		
		if(isSaveInvalidatedInstruments() && finalScore < getThreshold().doubleValue()) {
			if(row.getDocument() != null ) {
				getSearch().getSa().addInvalidatedInstrument(row.getDocument().getInstrument());
			}
		}

		return finalResult;
	}
	
	protected void loadRefLegal() {
		
		// load the reference values
		if(!refLoaded){			
			refSection  = sa.getAtribute(SearchAttributes.LD_SUBDIV_SEC);
			refTownship = sa.getAtribute(SearchAttributes.LD_SUBDIV_TWN);
			refRange    = sa.getAtribute(SearchAttributes.LD_SUBDIV_RNG);
			refLot      = sa.getAtribute(SearchAttributes.LD_LOTNO);
			refSubLot   = sa.getAtribute(SearchAttributes.LD_SUBLOT);
			refBlock    = sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			refPlatBook = sa.getAtribute(SearchAttributes.LD_BOOKNO);
		    refPlatPage = sa.getAtribute(SearchAttributes.LD_PAGENO);
		    refUnit 	= sa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT);
		    refPhase 	= sa.getAtribute(SearchAttributes.LD_SUBDIV_PHASE);
		    refArb      = sa.getAtribute(SearchAttributes.ARB);
		    refDistrict = sa.getAtribute(SearchAttributes.LD_DISTRICT).replaceFirst("^0+", "");
		    refAbs		= sa.getAtribute(SearchAttributes.LD_ABS_NO);
		    
		    if( StringUtils.isEmpty( refUnit )){
				 refUnit 	= sa.getAtribute(SearchAttributes.P_STREETUNIT_CLEANED);
			}
		    
			refLoaded   = true;
		}
		
	}

	public boolean isEnableBlock() {
		return enableBlock;
	}

	public void setEnableBlock(boolean enableBlock) {
		this.enableBlock = enableBlock;
	}

	public boolean isEnableLot() {
		return enableLot;
	}

	public void setEnableLot(boolean enableLot) {
		this.enableLot = enableLot;
	}
	
	public boolean isEnableSubLot() {
		return enableSubLot;
	}

	public void setEnableSubLot(boolean enableSubLot) {
		this.enableSubLot = enableSubLot;
	}

	public boolean isEnableUnit() {
		return enableUnit;
	}

	public void setEnableUnit(boolean enableUnit) {
		this.enableUnit = enableUnit;
	}

	public boolean isEnablePlatBook() {
		return enablePlatBook;
	}

	public void setEnablePlatBook(boolean enablePlatBook) {
		this.enablePlatBook = enablePlatBook;
	}

	public boolean isEnablePlatPage() {
		return enablePlatPage;
	}

	public void setEnablePlatPage(boolean enablePlatPage) {
		this.enablePlatPage = enablePlatPage;
	}

	public boolean isEnableRange() {
		return enableRange;
	}

	public void setEnableRange(boolean enableRange) {
		this.enableRange = enableRange;
	}

	public boolean isEnableSection() {
		return enableSection;
	}

	public void setEnableSection(boolean enableSection) {
		this.enableSection = enableSection;
	}

	public boolean isEnableSectionJustForUnplated() {
		return enableSectionJustForUnplated;
	}

	public void setEnableSectionJustForUnplated(boolean enableSectionJustForUnplated) {
		this.enableSectionJustForUnplated = enableSectionJustForUnplated;
	}

	public boolean isEnablePhase() {
		return enablePhase;
	}

	public void setEnablePhase(boolean enablePhase) {
		this.enablePhase = enablePhase;
	}
	
	public boolean isEnableTownship() {
		return enableTownship;
	}

	public void setEnableTownship(boolean enableTownship) {
		this.enableTownship = enableTownship;
	}
	
	@Override
    public String getFilterName(){
    	return "Filter by Legal";
    }
	
	@Override
	public String getFilterCriteria(){

		String retVal = "";
		
		loadRefLegal();

    	if(enableSection && !isEmpty(refSection)){
    		retVal += "Sec: " + refSection + " ";
    	}
    	if(enableTownship && !isEmpty(refTownship)){
    		retVal += "Twn: " + refTownship + " ";
    	}
    	if(enableRange && !isEmpty(refRange)){
    		retVal += "Rng: " + refRange + " ";
    	}
    	if(enableLot && !isEmpty(refLot)){
    		retVal += "Lot" + (isDoNotIntervalExpand() && refLot.contains("-")?"(not an interval)":"") + 
    				": " + refLot + " ";
	    }
    	
    	if(enableSubLot && !isEmpty(refSubLot)){
    		retVal += "SubLot: " + refSubLot + " ";
	    }
    	
	    if(enableBlock && !isEmpty(refBlock)){
	    	retVal += "Blk: " + refBlock + " ";
	    }
    	if(enablePlatBook && !isEmpty(refPlatBook)){
    		retVal += "Pbk: " + refPlatBook + " ";
    	}
    	if(enablePlatPage && !isEmpty(refPlatPage)){
    		retVal += "Ppg: " + refPlatPage + " ";
    	}
    	if(enableUnit && !isEmpty(refUnit)){
    		retVal += "Unit: " + refUnit + " ";
    	}
    	if(enablePhase && !isEmpty(refPhase)){
    		retVal += "Phase: " + refPhase + " ";
    	}
    	if(enableArb && !isEmpty(refArb)){
    		retVal += "ARB: " + refArb + " ";
    	}
    	if(enableDistrict && !isEmpty(refDistrict)){
    		retVal += "District: " + refDistrict + " ";
    	}
    	if(enableAbs && !isEmpty(refAbs)){
    		retVal += "ABS: " + refAbs + " ";
    	}
    	
		String enabledCriteria = 
				(enableSection ? "Sec," : "") +
				(enableTownship ? "Twn," : "") +
				(enableRange ? "Rng," : "") +
				(enableLot ? "Lot," : "") +
				(enableSubLot ? "SubLot," : "") +
				(enableBlock ? "Blk," : "") +
				(enablePlatBook ? "Pbk," : "") +
				(enablePlatPage ? "Ppg," : "") +
				(enableUnit ? "Unit," : "") +
				(enablePhase ? "Phase," : "") +
				(enableArb ? "ARB," : "") +
				(enableDistrict ? "District," : "") +
				(enableAbs ? "ABS" : "");
		if (!enabledCriteria.isEmpty()) {
			enabledCriteria = "(" + enabledCriteria.replaceFirst(",$", "") + ")";
		}
		
		if (retVal.trim().isEmpty()) {
			return "Legal" + enabledCriteria + "='No Legal Available To Test - All Documents Pass'";
		}
    	
    	String ret = "Legal='" + retVal.trim() + "'";
    	
    	if (ignoreLotAndBlockForPreferentialDoctype){
    		ret += " (Ignore LOT and BLOCK for doctypes: " + preferentialDoctypes.toString() + ")";
    	}
    	return ret;
    }
	
	public static boolean hasLegal(SearchAttributes sa){
		String criteria = 
			sa.getAtribute(SearchAttributes.LD_SUBDIV_SEC) +
			sa.getAtribute(SearchAttributes.LD_SUBDIV_TWN) +
			sa.getAtribute(SearchAttributes.LD_SUBDIV_RNG) +
			sa.getAtribute(SearchAttributes.LD_LOTNO) +
			sa.getAtribute(SearchAttributes.LD_SUBLOT) +
			sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK) +
			sa.getAtribute(SearchAttributes.LD_PAGENO) +
			sa.getAtribute(SearchAttributes.LD_BOOKNO);
		return !StringUtils.isEmpty(criteria);
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public static void main(String argv[]){
		
		/*System.out.println("Score 17 171 = "+computeScoreInternal("17", "171"));
		System.out.println("Score 171 17 = "+computeScoreInternal("171", "17"));
		System.out.println("Score 17E 17W = "+computeScoreInternal("17E", "17W"));
		System.out.println("Score 17E 17 = "+computeScoreInternal("17E", "17"));
		System.out.println("Score 17 17E = "+computeScoreInternal("17", "17E"));
		
		System.out.println("Score 171 17E = "+computeScoreInternal("171", "17E"));
		System.out.println("Score 17E 171 = "+computeScoreInternal("17E", "171"));
		System.out.println("Score 171 172 = "+computeScoreInternal("171", "172"));
		System.out.println("Score 17 17 = "+computeScoreInternal("17", "17"));
		System.out.println("Score 21 211 = "+computeScoreInternal("21", "211"));
	
		System.out.println("Score 21 211   211 = "+computeScoreInternal("21 211", "211"));
		System.out.println("Score 21 211   210 = "+computeScoreInternal("21 211", "210"));
		System.out.println("Score 1 4   14 = "+computeScoreInternal("1 4", "14"));
		System.out.println("Score 0 5   4 = "+computeScoreInternal("0 5", "4"));
		System.out.println("Score 0 5   5 = "+computeScoreInternal("0 5", "5"));
		System.out.println("Score 1 3-5 7   1 = "+computeScoreInternal("1 3-5 7", "1"));
		System.out.println("Score 1 3-5 7   2 = "+computeScoreInternal("1 3-5 7", "2"));
		System.out.println("Score 1 3-5 7   3 = "+computeScoreInternal("1 3-5 7", "3"));
		System.out.println("Score 1 3-5 7   4 = "+computeScoreInternal("1 3-5 7", "4"));
		System.out.println("Score 1 3-5 7   5 = "+computeScoreInternal("1 3-5 7", "5"));
		System.out.println("Score 1 3-5 7   6 = "+computeScoreInternal("1 3-5 7", "6"));
		System.out.println("Score 1 3-5 7   7 = "+computeScoreInternal("1 3-5 7", "7"));
		System.out.println("Score 1 3-5 7   9 = "+computeScoreInternal("1 3-5 7", "8"));
		
	
		System.out.println("Score 21,211   211 = "+computeScoreInternal("21,211", "211"));
		
		System.out.println("Score 21 21E   211 = "+computeScoreInternal("21 21E", "211"));
		
		System.out.println("Score 17 17E   17D = "+computeScoreInternal("17 17E", "17D"));
		
		System.out.println("Score 17 17-E   17E = "+computeScoreInternal("17 17-E", "17E"));
		System.out.println("Score 2A-2C   2A = "+computeScoreInternal("2A-2C", "2A"));
		System.out.println("Score 2A-2C   2B = "+computeScoreInternal("2A-2C", "2B"));
		System.out.println("Score 2A-2C   2C = "+computeScoreInternal("2A-2C", "2C"));
		System.out.println("Score 2A-2C   2D = "+computeScoreInternal("2A-2C", "2D"));
		System.out.println("Score 2A-2C   2 = "+computeScoreInternal("2A-2C", "2"));*/
		//System.out.println("Score 1PB   1 = "+computeScoreInternal("", "36E", "37", false, false));
		System.out.println("Score 20   20E = "+computeScoreInternal("", "29", "37", false, false));
		
		//System.out.println("Score 171 17 = "+computeScoreInternal("","171", "17", false, false));
		//System.out.println("Score 17E 17W = "+computeScoreInternal("","17E", "17W", false, false));
		//System.out.println("Score 17E 17 = "+computeScoreInternal("","17E", "17", false, false));
		//System.out.println("Score 17 17E = "+computeScoreInternal("","17", "17E", false, false));
		System.out.println("Score E17 17 = "+computeScoreInternal("","E17", "17", false, false));
		//System.out.println("Score 17 E17 = "+computeScoreInternal("","17", "E17", false, false));
		System.out.println("Score 117 B105 = "+computeScoreInternal("","117", "B105", false, false));
	}

	/**
	 * @return the noIntervalExpand
	 */
	public boolean isDoNotIntervalExpand() {
		return doNotIntervalExpand;
	}
	
	/**
	 * @param noIntervalExpand the noIntervalExpand to set
	 */
	public void setDoNotIntervalExpand(boolean doNotIntervalExpand) {
		this.doNotIntervalExpand = doNotIntervalExpand;
	}
	
	public boolean isEnableLotUnitFullEquivalence() {
		return enableLotUnitFullEquivalence;
	}

	public void setEnableLotUnitFullEquivalence(boolean enableLotUnitFullEquivalence) {
		this.enableLotUnitFullEquivalence = enableLotUnitFullEquivalence;
	}
	
	public boolean isEnableArb() {
		return enableArb;
	}

	public void setEnableArb(boolean enableArb) {
		this.enableArb = enableArb;
	}
	
	public boolean isEnableDistrict() {
		return enableDistrict;
	}

	public void setEnableDistrict(boolean enableDistrict) {
		this.enableDistrict = enableDistrict;
	}

	public boolean isEnableAbs() {
		return enableAbs;
	}

	public void setEnableAbs(boolean enableAbs) {
		this.enableAbs = enableAbs;
	}

	public boolean isMarkIfCandidatesAreEmpty() {
		return markIfCandidatesAreEmpty;
	}

	public void setMarkIfCandidatesAreEmpty(boolean markIfCandidatesAreEmpty) {
		this.markIfCandidatesAreEmpty = markIfCandidatesAreEmpty;
	}

	public HashSet<String> getIgnoreLotWhenServerDoctypeIs() {
		return ignoreLotWhenServerDoctypeIs;
	}

	public void setIgnoreLotWhenServerDoctypeIs(
			HashSet<String> ignoreLotWhenServerDoctypeIs) {
		this.ignoreLotWhenServerDoctypeIs = ignoreLotWhenServerDoctypeIs;
	}

	public boolean isContainedIn(GenericLegal legalFilterToCheck) {
		
		//check lot, this is a little bit tricky because we need to check derivations for lot
		if(isEnableLot()) {
			if(StringUtils.isNotEmpty(refLot)) {
				if(StringUtils.isEmpty(legalFilterToCheck.refLot)) {
					return false;
				} else {
					
					Set<String> refLotSet = new HashSet<String>();
					if(refLot!=null){
						if(isDoNotIntervalExpand()) {
							refLotSet.add(refLot);
						} else {
							Vector<LotInterval> lots = LotMatchAlgorithm.prepareLotInterval(refLot);
							for (int i = 0; i < lots.size(); i++) {
								refLotSet.addAll(lots.elementAt(i).getLotList());
							}
						}
					}
					
					Set<String> refLotSetToCheck = new HashSet<String>();
					if(legalFilterToCheck.refLot!=null){
						if(legalFilterToCheck.isDoNotIntervalExpand()) {
							refLotSetToCheck.add(legalFilterToCheck.refLot);
						} else {
							Vector<LotInterval> lots = LotMatchAlgorithm.prepareLotInterval(legalFilterToCheck.refLot);
							for (int i = 0; i < lots.size(); i++) {
								refLotSetToCheck.addAll(lots.elementAt(i).getLotList());
							}
						}
					}
					
					if(!refLotSet.equals(refLotSetToCheck)) {
						return false;
					}
					
				}
			}
		}
		
		//check block, easy for now
		if(!checkElementForInclusion(isEnableBlock(), refBlock, legalFilterToCheck.refBlock)) {
			return false;
		}
		
		//check PlatBook, easy for now		
		if(!checkElementForInclusion(isEnablePlatBook(), refPlatBook, legalFilterToCheck.refPlatBook)) {
			return false;
		}
		
		//check PlatPage, easy for now		
		if(!checkElementForInclusion(isEnablePlatPage(), refPlatPage, legalFilterToCheck.refPlatPage)) {
			return false;
		}
		
		//check Section, easy for now		
		if(!checkElementForInclusion(isEnableSection(), refSection, legalFilterToCheck.refSection)) {
			return false;
		}
		
		//check Township, easy for now		
		if(!checkElementForInclusion(isEnableTownship(), refTownship, legalFilterToCheck.refTownship)) {
			return false;
		}
		
		//check Range, easy for now		
		if(!checkElementForInclusion(isEnableRange(), refRange, legalFilterToCheck.refRange)) {
			return false;
		}
		
		//check Arb, easy for now		
		if(!checkElementForInclusion(isEnableArb(), refArb, legalFilterToCheck.refArb)) {
			return false;
		}
		
		//check District, easy for now
		if (!checkElementForInclusion(isEnableDistrict(), refDistrict, legalFilterToCheck.refDistrict)){
			return false;
		}
		
		//check Abs, easy for now
		if (!checkElementForInclusion(isEnableAbs(), refAbs, legalFilterToCheck.refAbs)){
			return false;
		}
		
		return true;
	}
	
	boolean checkElementForInclusion(boolean doCheck, String element1, String element2) {
		if(doCheck) {
			if(StringUtils.isNotEmpty(element1)) {
				if(StringUtils.isEmpty(element2)) {
					return false;
				} else {
					if(!element1.equalsIgnoreCase(element2)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	class GenericLegalStruct {
		String section  	= "";
	    String township 	= "";
	    String range    	= "";
	    String lot      	= "";
	    String subLot   	= "";
	    String block    	= "";
	    String lotThrough	= "";
	    String blockThrough	= "";
	    String platBook 	= ""; 
        String platNo 		= "";
        String unit 		= "";
        String phase		= "";
        String arb			= "";
        String unitAddress 		= "";
        String district 	= "";
        String abs 	= "";
        
        public boolean loadFromPis(PropertyIdentificationSet pis) {
        	if(pis == null) {
        		return false;
        	}
        	
        	section  = pis.getAtribute("SubdivisionSection");
		    township = pis.getAtribute("SubdivisionTownship");
		    range    = pis.getAtribute("SubdivisionRange");
		    lot      = pis.getAtribute("SubdivisionLotNumber");
		    subLot   = pis.getAtribute(PropertyIdentificationSetKey.SUB_LOT.getShortKeyName());
		    block    = pis.getAtribute("SubdivisionBlock");
		    lotThrough		= pis.getAtribute("SubdivisionLotThrough");
		    blockThrough	= pis.getAtribute("SubdivisionBlockThrough");
		    platBook		= pis.getAtribute("PlatBook"); 
            platNo	= pis.getAtribute("PlatNo");
            unit	= pis.getAtribute("SubdivisionUnit");
            phase	= pis.getAtribute("SubdivisionPhase");
            arb		= pis.getAtribute("ARB");
            district 		= pis.getAtribute("District").replaceFirst("^0+", ""); 
            
            try {
	            String address = "";
	            if(StringUtils.isNotEmpty(pis.getAtribute("StreetNo"))) {
	            	address = pis.getAtribute("StreetNo");
	            }
	            if(StringUtils.isNotEmpty(pis.getAtribute("StreetName"))) {
	            	address += " " + pis.getAtribute("StreetName");
	            }
	            
	            if(!address.trim().isEmpty()) {
	            	StandardAddress candAddress = new StandardAddress(address);
	            	unitAddress = candAddress.getAddressElement(StandardAddress.STREET_SEC_ADDR_RANGE);
	            }
            

            } catch (Exception e) {
				logger.error("Error while tring to parse address");
			}
            
            return true;
        }
        
        public boolean isEmpty() {
			return org.apache.commons.lang.StringUtils.isBlank(section)
					&& org.apache.commons.lang.StringUtils.isBlank(township)
					&& org.apache.commons.lang.StringUtils.isBlank(range)
					&& org.apache.commons.lang.StringUtils.isBlank(lot)
					&& org.apache.commons.lang.StringUtils.isBlank(subLot)
					&& org.apache.commons.lang.StringUtils.isBlank(block)
					&& org.apache.commons.lang.StringUtils.isBlank(lotThrough)
					&& org.apache.commons.lang.StringUtils.isBlank(blockThrough)
					&& org.apache.commons.lang.StringUtils.isBlank(platBook)
					&& org.apache.commons.lang.StringUtils.isBlank(platNo)
					&& org.apache.commons.lang.StringUtils.isBlank(unit)
					&& org.apache.commons.lang.StringUtils.isBlank(phase)
					&& org.apache.commons.lang.StringUtils.isBlank(arb)
					&& org.apache.commons.lang.StringUtils.isBlank(district)
					&& org.apache.commons.lang.StringUtils.isBlank(abs);
		}

		public boolean loadFromProperty(PropertyI property) {
        	if(property == null) {
        		return false;
        	}
        	
        	if(property.getLegal() != null) {
        		LegalI legal = property.getLegal();
        		if(legal.hasSubdividedLegal()) {
        			SubdivisionI subdivision = legal.getSubdivision();
        			lot = org.apache.commons.lang.StringUtils.defaultString(subdivision.getLot());
        			lotThrough = org.apache.commons.lang.StringUtils.defaultString(subdivision.getLotThrough());
        			subLot = org.apache.commons.lang.StringUtils.defaultString(subdivision.getSubLot());
        			block = org.apache.commons.lang.StringUtils.defaultString(subdivision.getBlock());
        			blockThrough = org.apache.commons.lang.StringUtils.defaultString(subdivision.getBlockThrough());
        			platBook = org.apache.commons.lang.StringUtils.defaultString(subdivision.getPlatBook());
        			platNo = org.apache.commons.lang.StringUtils.defaultString(subdivision.getPlatPage());
        			unit = org.apache.commons.lang.StringUtils.defaultString(subdivision.getUnit());
        			phase = org.apache.commons.lang.StringUtils.defaultString(subdivision.getPhase());
        			district = org.apache.commons.lang.StringUtils.defaultString(subdivision.getDistrict()).replaceFirst("^0+", "");
        			
        		}
        		if(legal.hasTownshipLegal()) {
        			TownShipI townShipI = legal.getTownShip();
        			section = org.apache.commons.lang.StringUtils.defaultString(townShipI.getSection());
        			township = org.apache.commons.lang.StringUtils.defaultString(townShipI.getTownship());
        			range = org.apache.commons.lang.StringUtils.defaultString(townShipI.getRange());
        			arb = org.apache.commons.lang.StringUtils.defaultString(townShipI.getArb());
        			abs = org.apache.commons.lang.StringUtils.defaultString(townShipI.getAbsNumber());
        		}
        		if(property.hasAddress()) {
        			AddressI addressI = property.getAddress();
        			unitAddress = org.apache.commons.lang.StringUtils.defaultString(addressI.getIdentifierNumber());
        		}
        	}
        	
        	return true;
        }
	}
	
	public boolean getIgnoreLotAndBlockForPreferentialDoctype() {
		return ignoreLotAndBlockForPreferentialDoctype;
	}

	public void setIgnoreLotAndBlockForPreferentialDoctype(boolean ignoreLotAndBlockForPreferentialDoctype) {
		this.ignoreLotAndBlockForPreferentialDoctype = ignoreLotAndBlockForPreferentialDoctype;
	}
	
}

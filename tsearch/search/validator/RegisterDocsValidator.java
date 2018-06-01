/*
 * Created on Nov 17, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.validator;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.propertyInformation.Address;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.search.filter.matchers.subdiv.SubdivMatcher;
import ro.cst.tsearch.servers.ServletServerComm;
import ro.cst.tsearch.servers.response.InfSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class RegisterDocsValidator extends DocsValidator {

	
	private static final long serialVersionUID = -8944545180090771488L;
	private static final Category logger = Category.getInstance(RegisterDocsValidator.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + RegisterDocsValidator.class.getName());
	
	private SubdivMatcher subdivMatcher =null;
	private String addressMatcher = null; // not actually used. remained here so that searches can be de-serialized
	
	private boolean matchAddress = true; // used to enable/disable the address match in validAddresOrSubdivision()
	
	protected long searchId=-1;
	
	public RegisterDocsValidator(Search search) {
		super(search);
		this.searchId = search.getID();
		subdivMatcher= new SubdivMatcher(searchId);
	}

	public boolean isValid(ServerResponse response) {
		int searchType = search.getSearchType();
		boolean searchFinished = (new Boolean ((String)search.getSa().getAtribute(SearchAttributes.SEARCHFINISH))).booleanValue();

		boolean valid = true;
		if (searchType == Search.PARENT_SITE_SEARCH
			&& searchFinished) {
			valid = isValidOnParentSite(response);
		} else {
			valid = isValidOnAutomatic(response);
		}
		
		/*if (valid){
			removeTooManyCrossRef(response);
		}*/
		return valid;
	}


	protected boolean isValidOnAutomatic(ServerResponse response) {
		boolean valid = validDoc(response);
		loggerDetails.debug("validAutomaticSearch = " + valid);
        IndividualLogger.info( "validAutomaticSearch = " + valid ,searchId);
		return valid;
	}

	protected boolean isValidOnParentSite(ServerResponse response) {
		/*boolean validDate = validDate(response, search.getSa());
		if (logger.isDebugEnabled())
			logger.debug(" validParentSite : validDate" + " = " + validDate);*/
		return  true; // always valid on parent site
	}

	protected boolean validDate(ServerResponse response, SearchAttributes sa) {
		boolean valid = true;
		if (response.getParsedResponse().getSaleDataSet().size() > 0) {
			String date = response.getParsedResponse().getSaleDataSet(0).getAtribute("RecordedDate").trim();
			String documentType = response.getParsedResponse().getSaleDataSet(0).getAtribute("DocumentType").trim();
			if(!StringUtils.isEmpty(documentType)){
				if (!DocumentTypes.checkDocumentType(documentType, DocumentTypes.PLAT_INT, null, searchId))
					valid = validDate(date, sa);
			} else 
				valid = validDate(date, sa);
		}
		return valid;
	}

	protected boolean validDate(String docDate, SearchAttributes sa) {
		DateFormat dfShort = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
		DateFormat dfMedium = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US);
		Date docD = new Date();
		Date fromD = new Date();
		Date toD = new Date();
		try {
			fromD = dfMedium.parse(sa.getAtribute(SearchAttributes.FROMDATE));
			loggerDetails.debug("fromDate = " + fromD);
            IndividualLogger.info( "fromDate = " + fromD ,searchId);
		} catch (ParseException e1) {
            IndividualLogger.info( " error parsing from date " + e1,searchId );
			logger.error(" error parsing from date ", e1);
			e1.printStackTrace();
		}
		try {
			toD = dfMedium.parse(sa.getAtribute(SearchAttributes.TODATE));
			loggerDetails.debug("toDate = " + toD);
            IndividualLogger.info( "toDate = " + toD ,searchId);
		} catch (ParseException e2) {
			logger.error(" error parsing to date ", e2);
            IndividualLogger.info( " error parsing to date " + e2 ,searchId);
			e2.printStackTrace();
		}
		try {
			loggerDetails.debug("docDate = " + docDate);
            IndividualLogger.info( "docDate = " + docDate ,searchId);
			docD = dfShort.parse(docDate);
			loggerDetails.debug("docDate parsed = " + docD);
            IndividualLogger.info( "docDate parsed = " + docD ,searchId);
		} catch (ParseException e) {
			//logger.error(" error at parsing the data document", e);
			//e.printStackTrace();
			docD = toD;
		}
		return (docD.compareTo(fromD) >= 0) && (docD.compareTo(toD) <= 0);
	}


	/**
	 * Method ValidDoc.
	 * @param result
	 * @param sa
	 * @param i
	 * @return boolean
	 */
	protected boolean validDoc(ServerResponse result) {
		SearchAttributes sa = search.getSa();
		
		loggerDetails.debug(" sa = " + sa);
        IndividualLogger.info( " sa = " + sa ,searchId);

		boolean validAddressAndLot = validAddresOrSubdivision(result, sa);
		
		if (matchAddress) {
			IndividualLogger.info( "Valid address and legal description: " + validAddressAndLot,searchId );        
			if (logger.isDebugEnabled())
				logger.debug("Addresses and legal descriptions are equal: " + validAddressAndLot);
		} else {
			IndividualLogger.info( "Valid legal description: " + validAddressAndLot ,searchId);        
			if (logger.isDebugEnabled())
				logger.debug("Legal descriptions are equal: " + validAddressAndLot);			
		}


		boolean validDate = validDate(result, sa);
        IndividualLogger.info( "Valid date (if plat date is not tested): " + validDate ,searchId);
		if (logger.isDebugEnabled())
			logger.debug("Data is valid (if plat date is not tested): " + validDate);

		boolean bValid = validAddressAndLot && validDate;
		//work around it will be ok only if a not valid doc is found before his link is found
	//	bValid = bValid && search.checkRemovedInstr(result.getResult());

		String instrNo = "";
		if (result.getParsedResponse().getSaleDataSetsCount() > 0) {
			instrNo = result.getParsedResponse().getSaleDataSet(0).getAtribute("InstrumentNumber").trim();
		}

		if (!bValid) {
	        IndividualLogger.info( " rejected file: " + instrNo,searchId );
			if (logger.isDebugEnabled())
				logger.debug(" rejected file: " + instrNo);
			if (!instrNo.equals("")) {
				search.addRemovedInstr(instrNo);
			}
		} else {
	        IndividualLogger.info( " valid file: " + instrNo,searchId );
			if (logger.isDebugEnabled())
				logger.debug(search.getSearchID() + " valid file: " + instrNo);
		}

		return bValid;
	}
	


	protected boolean validAddresOrSubdivision(ServerResponse result, SearchAttributes sa) {
		List allCandPis = getAllPisFromDoc(result);
		List allCandSa = getSaFromDoc(result);
		String	docType="";
		int validAddress = -1;
		;
		try {
		docType = ((InfSet) ((Vector) result.getParsedResponse().infVectorSets.get("SaleDataSet")).get(0)).getAtribute("DocumentType");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (matchAddress&&!DocumentTypes.checkDocumentType(docType, DocumentTypes.PLAT_INT, InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext(), searchId)){ 
			validAddress = validAddress(allCandSa,  sa);
			IndividualLogger.info( "Address is valid = " + validAddress ,searchId);
		} 
	
		int validSubdiv = validSubdivision(allCandPis , sa);
        IndividualLogger.info( "Subdivision is valid = " + validSubdiv ,searchId);
		
		boolean bValid = true;
		if ((validAddress == 1)||(validSubdiv == 1)) {
			bValid = true;
		} else if ((validAddress == 0)||((validSubdiv == 0))) {
			bValid = false;
		} else {// -1 and -1
			bValid = true;
		}

		return bValid;
	}

	protected int validAddress(List allCandSa , SearchAttributes saRef){


		int rez = -1;
				
		for (int i = 0; i < allCandSa.size(); i++) {
			SearchAttributes saCand = (SearchAttributes) allCandSa.get(i);

			Address a = saRef.getPropertyAddress();
			Address a1 = saCand.getPropertyAddress();
			boolean equal = a.flexibleEquals(a1,searchId);
			loggerDetails.debug("comparing " + a + " with " + a1 + " = " + equal);
            IndividualLogger.info( "comparing " + a + " with " + a1 + " = " + equal ,searchId);
            
			if ((equal) && (a.hasStreetName()) && (a1.hasStreetName())) {
				rez = 1;
				break;
			} else if (!equal) {
				rez = 0;




			}
		}
		if (logger.isDebugEnabled())
			logger.debug("adresele sint  egale = " + rez);
		return rez;
	}

/* Din pacate validarea cu matchuitor de adresa nu prea merge 
 * ex. goose creek dr ~ goose creek => goose creek ~ goose = scor mic 
 
	protected int validAddress(List allCandPis , SearchAttributes saRef){
		BigDecimal threshold = new BigDecimal("0.8");

		//if the ref address is Wolf Trace Lane, the refStreet name will remain Wolf, such that
		// if the cand address is Wolf Trace to allow a perfect match
		// tbd: the directions/suffix matching
		AddressTokenList refAddress = new AddressTokenList(
												saRef.getAtribute(SearchAttributes.P_STREETNAME), 
												//saRef.getAtribute(SearchAttributes.P_STREETDIRECTION), 
												//saRef.getAtribute(SearchAttributes.P_STREETSUFIX), 
												saRef.getAtribute(SearchAttributes.P_STREETNO)
											);

		int rez = -1;
		for (int i = 0; i < allCandPis.size(); i++) {
			PropertyIdentificationSet pisCand = (PropertyIdentificationSet) allCandPis.get(i);

			AddressTokenList candAddress =new AddressTokenList(
														pisCand.getAtribute("StreetName"), 
														pisCand.getAtribute("StreetNo")
										); 
																						
			BigDecimal score = addressMatcher.getScore(refAddress , candAddress); 
						
			if (!StringUtils.isStringBlank(refAddress.getStreetNameAsString())&&!StringUtils.isStringBlank(candAddress.getStreetNameAsString())){
				if (score.compareTo(threshold)>=0){
					rez = 1;
					break;
				}else{
					rez = 0;
				}
			}
		}
		
		logger.debug("adresele sint  egale = " + rez);
		return rez;
	}
*/
	public void setSubdivisionNameThreshold(String newThreshold){
		subdivMatcher.setSubdivisionNameThreshold(newThreshold);
	}
	
	protected int validSubdivision(List allCandPis , SearchAttributes saRef) {
		PropertyIdentificationSet pisRef = FillPisFromSa4Register(saRef);
		int rez = -1;
		for (int i = 0; i < allCandPis.size(); i++) {
			PropertyIdentificationSet pisCand = (PropertyIdentificationSet) allCandPis.get(i);
			int validOne = subdivMatcher.validSubdivision(pisRef, pisCand);
			loggerDetails.debug( " this subdiv is valid = " + validOne);
            IndividualLogger.info( " this subdiv is valid = " + validOne,searchId );
			if (validOne == 1){
				rez = 1;
				break;
			}else if (validOne == 0){
				rez = 0;
			}
		}

		if (logger.isDebugEnabled())
			logger.debug("subdivizions are equal = " + rez);
		return rez; 
	}


	private int isBestScoreValid(List allScores, String threshold) {
		BigDecimal maxScore = MatchAlgorithm.maxNo(allScores);
		int rez;
		if (maxScore == ATSDecimalNumberFormat.NA) {
			rez = -1; //nu a gasit nici un candidate cu care sa se compare
		} else {
			if (maxScore.compareTo(new BigDecimal(threshold)) > 0) {
				rez = 1;
			} else {
				rez = 0;
			}
		}
		return rez;
	}


	protected List getSaFromDoc(ServerResponse result) {
		List rez = new ArrayList();
		int countPIS = result.getParsedResponse().getPropertyIdentificationSetCount();
		for (int i = 0; i < countPIS; i++) {
			SearchAttributes sa1 = new SearchAttributes(Search.SEARCH_NONE);
			PropertyIdentificationSet pis = result.getParsedResponse().getPropertyIdentificationSet(i);
			if (validPis(pis)){ 
				FillSaFromPIS4Register(sa1, pis);
				loggerDetails.debug(" sa1 = " + sa1);
				rez.add(sa1);
			}
		}
		return rez;		
	}

	protected List getAllPisFromDoc(ServerResponse result) {
		List rez = new ArrayList();
		int countPIS = result.getParsedResponse().getPropertyIdentificationSetCount();
		for (int i = 0; i < countPIS; i++) {
			PropertyIdentificationSet pis = result.getParsedResponse().getPropertyIdentificationSet(i);
			if (validPis(pis)){ 
				rez.add(pis);
			}
		}
		return rez;		
	}

	protected boolean validPis (PropertyIdentificationSet pis){
		return isRealPis(pis);
	}
	
	
	public static boolean isRealPis(PropertyIdentificationSet pis){ 
		if (pis.getAtribute("City").equalsIgnoreCase("*grantor*") ||
				pis.getAtribute("City").equalsIgnoreCase("*grantee*")){//special flag to denote
			// there is a grantor that might be a subdivision
			return false;
		}else{
			return true;
		}
	}

	public static boolean isRealPis2(PropertyIdentificationSet pis, ServerResponse sr){ 
		if (pis.getAtribute("City").equalsIgnoreCase("*grantor*") ||
				pis.getAtribute("City").equalsIgnoreCase("*grantee*")){//special flag to denote
		    ///manareala low level ca sa se incadreze corect plat-urile la davidson ro
		    ParsedResponse pr=sr.getParsedResponse();
		    if(pr!=null)
		    {
		        SaleDataSet sds=(pr.getSaleDataSetsCount()>0) ? pr.getSaleDataSet(0):null;
		        if(sds!=null)
		        {
		            String type=(String)sds.getAtribute("DocumentType");
		            if(type==null || (type!=null && type.trim().length()==0))
		             sds.setAtribute("DocumentType","PLAT");
		        }
		    }
			// there is a grantor that might be a subdivision
			return false;
		}else{
			return true;
		}
	}


	protected boolean validDocType(ServerResponse response, int[] docTypes) {
		boolean valid = true;
		if (response.getParsedResponse().getSaleDataSet().size() > 0) {
			String docType = response.getParsedResponse().getSaleDataSet(0).getAtribute("DocumentType").trim();
			valid = isOfDocType(docType, docTypes,searchId);
			if (logger.isDebugEnabled())
				logger.debug("document type = " + docType + " is valid = " + valid);
	        IndividualLogger.info( "document type = " + docType + " is valid = " + valid ,searchId);
		}
		
        IndividualLogger.info( "validating doc type = " + valid,searchId );
		return valid;
	}

	protected void changeDocType(ServerResponse response, String newDocType) {
		if (response.getParsedResponse().getSaleDataSet().size() > 0) {
			String docType = response.getParsedResponse().getSaleDataSet(0).getAtribute("DocumentType").trim();
			loggerDetails.debug("found doc type = " + docType);
	        IndividualLogger.info( "found doc type = " + docType,searchId );
			if (StringUtils.isStringBlank(docType)){
				response.getParsedResponse().getSaleDataSet(0).setAtribute("DocumentType", newDocType);
			}
		}
	}

	protected static void FillSaFromPIS4Register(SearchAttributes sa, PropertyIdentificationSet pis) {
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.P_STREETNO, "StreetNo");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.P_STREET_FULL_NAME, "StreetName");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.P_CITY, "City");
		//ServletServerComm.addFromIS(sa, pis, SearchAttributes.P_STATE, "State");
		/*if (!ServletServerComm.addFromPIS(sa, pis, SearchAttributes.P_CITY, "City"))
			sa.setAtribute(SearchAttributes.P_CITY, defaultCity);*/
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.P_ZIP, "Zip");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBDIVISION, "Subdivision");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_LOTNO, "SubdivisionLotNumber");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_BLOCK, "SubdivisionBlock");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_UNIT, "SubdivisionUnit");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_TRACT, "SubdivisionTract");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_NAME, "SubdivisionName");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_SEC, "SubdivisionSection");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_TWN, "SubdivisionTownship");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_RNG, "SubdivisionRange");		
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_CODE, "SubdivisionCode");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_PHASE, "SubdivisionPhase");
	}

	public static PropertyIdentificationSet FillPisFromSa4Register(SearchAttributes sa) {
		PropertyIdentificationSet pis = new PropertyIdentificationSet ();
		ServletServerComm.addToIS(sa, pis, SearchAttributes.P_STREETNO, "StreetNo");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.P_STREETNAME, "StreetName");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.P_CITY, "City");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.P_ZIP, "Zip");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.LD_SUBDIVISION, "Subdivision");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.LD_LOTNO, "SubdivisionLotNumber");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.LD_SUBDIV_BLOCK, "SubdivisionBlock");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.LD_SUBDIV_UNIT, "SubdivisionUnit");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.LD_SUBDIV_TRACT, "SubdivisionTract");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.LD_SUBDIV_NAME, "SubdivisionName");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.LD_SUBDIV_SEC, "SubdivisionSection");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.LD_SUBDIV_TWN, "SubdivisionTownship");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.LD_SUBDIV_RNG, "SubdivisionRange");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.LD_SUBDIV_CODE, "SubdivisionCode");
		ServletServerComm.addToIS(sa, pis, SearchAttributes.LD_SUBDIV_PHASE, "SubdivisionPhase");
		
		return pis;
	}
	

	
	public static boolean isOfDocType (String docType, int[] types,long searchId){
		boolean rez = false;
		for (int i = 0; i < types.length; i++) {
			if (DocumentTypes.checkDocumentType(docType, types[i],null,searchId)){
				rez = true;
				break;
			}
		}
		return rez;
	}

	/**
	 * @param matcher
	 */
	public void setSubdivMatcher(SubdivMatcher matcher) {
		subdivMatcher = matcher;
	}

	public void setMatchAddress (boolean value) {
		matchAddress = value;
	}
}

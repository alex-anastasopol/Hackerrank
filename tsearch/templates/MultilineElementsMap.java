package ro.cst.tsearch.templates;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.StringUtils;

public class MultilineElementsMap extends HashMap<String ,String>{
	
	private static final long serialVersionUID = 936347347834L;
	public static final int ELEMENT_TYPE = 1;
	public static final int SUBELEMENT_TYPE = -1;
	
	public static final String CONDOMINIUM = "CONDOMINIUM";
	public static final String LOT_NO = "LOT_NO";
	public static final String UNIT_NO = "UNIT_NO";
	public static final String SUBDIVISION = "SUBDIVISION";
	
	public static final String LOT = "LOT";
	public static final String SUBLOT = "SUBLOT";
	public static final String BLOCK = "BLOCK";
	public static final String SECTION = "SECTION";
	public static final String PHASE = "PHASE";
	public static final String UNIT = "UNIT";
	
	public static final String CITY = "CITY";
	
	public static final String CONSIDERATION_AMOUNT = "CONSIDERATION_AMOUNT";
	public static final String DOCUMENT_NO = "DOCUMENT_NO";
	public static final String DEED_OF_TRUST_DOCUMENT_NO = "DEED_OF_TRUST_DOCUMENT_NO";
	public static final String BOOK_NO = "BOOK_NO";
	public static final String BOOK_TYPE = "BOOK_TYPE";
	public static final String PAGE_NO = "PAGE_NO";
	public static final String INSTRUMENT_NO = "INSTRUMENT_NO";
	public static final String COUNTER = "COUNTER";
	public static final String GUID = "GUID";
	public static final String CONTOR_TYPE_1 = "CONTOR_TYPE_1";
	public static final String CONTOR_TYPE_2 = "CONTOR_TYPE_2";
	public static final String SERVER_DOCUMENT_TYPE = "SERVER_DOCUMENT_TYPE";
	public static final String GRANTOR = "GRANTOR";
	public static final String GRANTOR_FML = "GRANTOR_FML";
	public static final String INSTRUMENT_DATE = "INSTRUMENT_DATE";
	public static final String FILLED_DATE = "FILLED_DATE";
	
	public static final String BOOK_PAGE_INSTNO_AK = "BOOK_PAGE_INSTNO_AK";
	
	public static final String RECEIPT_NUMBER = "RECEIPT_NUMBER";
	public static final String RECEIPT_DATE = "RECEIPT_DATE";
	public static final String RECEIPT_AMOUNT = "RECEIPT_AMOUNT";
	public static final String PARCEL_NO = "PARCEL_NO";
	
	public static final String ASSOCIATION_NAME = "ASSOCIATION_NAME";
	public static final String BOARD_MEMBER_NAME = "BOARD_MEMBER_NAME";
	public static final String BOARD_MEMBER_PHONE = "BOARD_MEMBER_PHONE";
	public static final String HOA_NAME = "HOA_NAME";
	public static final String MASTER_HOA = "MASTER_HOA";
	public static final String ADD_HOA = "ADD_HOA";
	public static final String HOA_PLAT_BOOK = "HOA_PLAT_BOOK";
	public static final String HOA_PLAT_PAGE = "HOA_PLAT_PAGE";
	public static final String HOA_DECL_BOOK = "HOA_DECL_BOOK";
	public static final String HOA_DECL_PAGE = "HOA_DECL_PAGE";
	
	public static final String PRIOR_LD = "PRIOR_LD";
	public static final String PRIOR_EXCEPTIONS = "PRIOR_EXCEPTIONS";
	public static final String PRIOR_REQUIREMENTS = "PRIOR_REQUIREMENTS";
	public static final String ADDITIONAL_REQUIREMENTS = "ADDITIONAL_REQUIREMENTS";
	
	//START currentOwners/currentBuyers sub-elements
	public static final String FIRST="FIRST";
	public static final String MIDDLE="MIDDLE";
	public static final String LAST="LAST";
	public static final String SUFFIX="SUFFIX";
	public static final String MIDDLE_INITIAL="MIDDLE_INITIAL";
	public static final String BUSINESS_NAME = "BUSINESS_NAME";
	public static final String FIRST_BUSINESS_NAME = "FIRST_BUSINESS_NAME";
	
	/**
	 * can only take values from {@link PartyType} enumeration
	 */
	public static final String PARTY_TYPE = "PARTY_TYPE";
	
	public static enum PartyType{
		BUSINESS("business"),
		INDIVIDUAL("individual");
		
		String partyType;
		
		PartyType (String partyType){
			this.partyType = partyType;
		}
		
		public static String getPartyTypeString(boolean isCompany){
			if(isCompany)
				return BUSINESS.getPartyType();
			else 
				return INDIVIDUAL.getPartyType();
		}
		
		public String getPartyType(){
			return partyType;
		}
	}
	
	// END currentOwners/currentBuyers sub-elements
	
	public static final String GRANTEE_TR = "GRANTEE_TR";
	public static final String GRANTEE_TRUSTEE = "GRANTEE_TRUSTEE";
	public static final String GRANTEE = "GRANTEE";
	public static final String GRANTEE_FML = "GRANTEE_FML";
	public static final String GRANTEE_LANDER = "GRANTEE_LANDER";
	public static final String GRANTEE_LENDER = "GRANTEE_LENDER";
	public static final String GRANTEE_LANDER_FML = "GRANTEE_LANDER_FML";
	public static final String MORTGAGE_AMOUNT 		= "MORTGAGE_AMOUNT";
	public static final String LOAN_NO 				= "LOAN_NO";
	public static final String MORTGAGE_AMOUNT_FREE_FORM	= "MORTGAGE_AMOUNT_FREE_FORM";
	
	public static final String ASSESSOR = "ASSESSOR";
	public static final String CITYTAX = "CITYTAX";
	public static final String COUNTYTAX = "COUNTYTAX";
//	public static final String SFPRIORFILES = "SFPRIORFILES";
	public static final String OTHERFILES = "OTHERFILES";
	public static final String PATRIOTS = "PATRIOTS";
	
	public static final String ASSIGNMENTS = "ASSIGNMENTS";
	public static final String APPOINTMENTS = "APPOINTMENTS";
	public static final String SUBORDINATIONS = "SUBORDINATIONS";
	public static final String MODIFICATIONS = "MODIFICATIONS";
	public static final String RQNOTICE= "RQNOTICE";
	public static final String MISCELLANEOUS= "MISCELLANEOUS";
	public static final String SUBSTITUTION= "SUBSTITUTION";
	public static final String ASSUMPTION = "ASSUMPTION";
	public static final String RELEASES = "RELEASES";
	public static final String RECEIPTS = "RECEIPTS";
	public static final String INSTALLMENTS = "INSTALLMENTS";
	public static final String SA_INSTALLMENTS = "SA_INSTALLMENTS";
	public static final String ALL_REFERENCES = "ALL_REFERENCES";
	public static final String LEGAL = "LEGAL";
	public static final String ADDRESS = "ADDRESS";
	public static final String RESTRICTIONS = "RESTRICTIONS";
	public static final String CCERS = "CCERS";
	public static final String HOAS = "HOAS";
	
	public static final String DATASOURCES = "DATASOURCES";
	
	public static final String STATEMENTS = "STATEMENTS";
	
	public static final String BP_INST_DOC = "BP_INST_DOC";
	public static final String BP_DOC_INST = "BP_DOC_INST";
	public static final String DOC_BP_INST = "DOC_BP_INST";
	public static final String DOC_INST_BP = "DOC_INST_BP";
	public static final String INST_DOC_BP = "INST_DOC_BP";
	public static final String INST_BP_DOC = "INST_BP_DOC";
	
	public static final String BP_INST_DOC1 = "BP_INST_DOC1";
	public static final String BP_DOC_INST1 = "BP_DOC_INST1";
	public static final String DOC_BP_INST1 = "DOC_BP_INST1";
	public static final String DOC_INST_BP1 = "DOC_INST_BP1";
	public static final String INST_DOC_BP1 = "INST_DOC_BP1";
	public static final String INST_BP_DOC1 = "INST_BP_DOC1";
	
	public static final String BP_INST_DOC2 = "BP_INST_DOC2";
	public static final String BP_INST_CO = "BP_INST_CO";
	public static final String BP_INST_TX = "BP_INST_TX";
	public static final String BP_INST_OH = "BP_INST_OH";
	
	
	public static final String BP_INST_DOC_ALL = "BP_INST_DOC_ALL";
	
	public static final String MONTH 	= "MONTH";
	public static final String YEAR 	= "YEAR";
	public static final String AMOUNT 	= "AMOUNT";
	
	public static final String DOCUMENT_ID = "DOCUMENT_ID";
	public static final String NOTE = "NOTE";
	public static final String SSN4 = "SSN4";
	public static final String DOCUMENT_TYPE = "DOCUMENT_TYPE";
	public static final String DOCUMENT_SUBTYPE = "DOCUMENT_SUBTYPE";
	public static final String DOCUMENT_REMARKS = "DOCUMENT_REMARKS";
	public static final String DATE_ORDER_ASC = "DATE_ORDER_ASC";
	public static final String MONTHS_BACK = "MONTHS_BACK";
	public static final String SEPARATOR = "SEPARATOR";
	public static final String DATE_ORDER_DESC = "DATE_ORDER_DESC";
	public static final String TSR_INDEX_ORDER = "TSR_INDEX_ORDER";
	public static final String FIRST_ORDER = "FIRST_ORDER";
	public static final String LAST_ORDER = "LAST_ORDER";
	
	public static final String LAST_OWNER_VALID = "LAST_OWNER_VALID";
	
	public static final String UNDEFINED = "UNDEFINED";
	public static final String UNRELATED = "UNRELATED";
	public static final String DISABLE_BOILER_FEATURE = "DISABLE_BOILER_FEATURE";
	
	public static final String BOOK_NV = "BOOK_NV";
	
	public static final String INSTRUMENT_NV = "INSTRUMENT_NV";
	
	public static final String CONTOR_TYPE_3 = "CONTOR_TYPE_3";
	
	public static final String DEFAULTIFNOMATCH = "DEFAULTIFNOMATCH";
	
	public static final String INSTRUMENT_STCKC = "INSTRUMENT_STCKC";
	
	public static final String APPRAISED_VALUE_LAND = "APPRAISED_VALUE_LAND";
	public static final String APPRAISED_VALUE_IMPROVEMENTS = "APPRAISED_VALUE_IMPROVEMENTS";
	public static final String APPRAISED_VALUE_TOTAL = "APPRAISED_VALUE_TOTAL";
	public static final String TOTAL_ASSESSMENT = "TOTAL_ASSESSMENT";
	public static final String EXEMPTION_AMOUNT = "EXEMPTION_AMOUNT";
	public static final String BASE_AMOUNT = "BASE_AMOUNT";
	public static final String BASE_AMOUNT_CITY = "BASE_AMOUNT_CITY";
	public static final String FOUND_DELINQUENT = "FOUND_DELINQUENT";
	public static final String TOTAL_DELINQUENT = "TOTAL_DELINQUENT";
	public static final String AMOUNT_PAID = "AMOUNT_PAID";
	public static final String AMOUNT_DUE = "AMOUNT_DUE";
	public static final String DATE_PAID = "DATE_PAID";
	public static final String TAX_VOLUME = "TAX_VOLUME";
	public static final String SALE_DATE = "SALE_DATE";
	public static final String SALE_NO = "SALE_NO";
	public static final String BILL_NUMBER = "BILL_NUMBER";
	public static final String RESEARCH_REQUIRED = "RESEARCH_REQUIRED";
	public static final String SPLIT_PAYMENT_AMOUNT = "SPLIT_PAYMENT_AMOUNT";
	public static final String PAY_DATE = "PAY_DATE";
	public static final String DUE_DATE = "DUE_DATE";
	public static final String TOTAL_INSTALL = "TOTAL_INSTALL";						//total number of installments
	public static final String SA_TOTAL_INSTALL = "SA_TOTAL_INSTALL";				//total number of special assessment installments
	
	public static final String INSTALL_NO = "INSTALL_NO";							//current installment number
	public static final String HAS_HOMESTEAD_EXEMPTION = "HAS_HOMESTEAD_EXEMPTION";
	public static final String PENALTY_AMOUNT = "PENALTY_AMOUNT";
	public static final String HOMESTEAD_EXEMPTION = "HOMESTEAD_EXEMPTION";
	public static final String STATUS = "STATUS";
	public static final String YEAR_DESCRIPTION = "YEAR_DESCRIPTION";
	public static final String BILL_TYPE = "BILL_TYPE";
	
	public static final String TOTAL_ESTIMATED_TAXES = "TOTAL_ESTIMATED_TAXES";
	
	public static final String DISTRICT = "DISTRICT";
	
	public static final String FINAL_PAYMENT = "FINAL_PAYMENT";
	public static final String PREPAID_PRINCIPAL = "PREPAID_PRINCIPAL";
	public static final String CURRENT_DUE = "CURRENT_DUE";
	public static final String TOTAL_PAYOFF = "TOTAL_PAYOFF";
	public static final String DUE_DATES_AM = "DUE_DATES_AM";
	public static final String INITIAL_PRINCIPAL = "INITIAL_PRINCIPAL";
	
	public static final String DATASOURCE = "DATASOURCE";
	public static final String DATASOURCE_DESCRIPTION = "DATASOURCE_DESCRIPTION";
	public static final String DATASOURCE_EFFECTIVE_START_DATE = "DATASOURCE_EFFECTIVE_START_DATE";
	public static final String DATASOURCE_RUN_TYPE = "DATASOURCE_RUN_TYPE";
	
	/**
	 * Fields available for "abstractors" multiline tag:<br>
	 * <b>LAST_NAME</b><br>
	 * <b>FIRST_NAME</b><br>
	 * <b>LOGIN</b><br>
	 * <b>PROCESSED_TIME_SECONDS</b><br>
	 * <b>PROCESSED_TIME_FORMATTED</b>
	 * @author Andrei
	 *
	 */
	public enum ABSTRACTORS {
		LAST_NAME,
		FIRST_NAME,
		LOGIN,
		PROCESSED_TIME_SECONDS,
		PROCESSED_TIME_FORMATTED
	}
	
	public static final String BPCODE = "BPCODE";
	
	private static final List<String[]> vecSpecialElements = new ArrayList<String[]>();
	
	public static boolean isInVecSpecialElements(final String str){
		for(String[] arraystr:vecSpecialElements){
			for(String key:arraystr){
				if(key.equalsIgnoreCase(str)){
					return true;
				}
			}
		}
		return false;
	}
	
	//contains the sub-elements available for each multiline tag 
	private static final HashMap<String,String[]> multilineTagSubElements = new HashMap<String,String[]>();
	
	private static final List<String[]> vecSpecialSubElements = new ArrayList<String[]>();
	
	//asocierea intre un tag si elementele speciale pe care le are
	private static final HashMap<String,String[]> mapAssociatedElements= new HashMap<String,String[]>();
	
	//asocierea intre un element special si subelementele speciale pe care le accepta
	private static final HashMap<String,String[]> mapAssociatedSubElements= new HashMap<String,String[]>();
	
	public static HashMap<String,String[]> getMapAssociatedSubElements() {
		return mapAssociatedSubElements;
	}
	
	/* cand se introduce aici un tag sa nu se uite sa se adauge in vecSpecialElements o lista 
	 de elemente speciale pe care le poate contine*/
	 public static final  String[] allSpecialElements = {
		 											AddDocsTemplates.documents, 			
		 											AddDocsTemplates.parcels,				
		 											AddDocsTemplates.amountsToRedeemFor,
		 											AddDocsTemplates.currentOwners,         
		 											AddDocsTemplates.currentBuyers, 
		 											AddDocsTemplates.ssfStatements,
		 											AddDocsTemplates.dataSources,
		 											AddDocsTemplates.abstractors
		 											};
	 
	 /* cand se introduce aici un tag sa nu se uite sa se adauge in vecSpecialSubElements o lista 
	 de elemente speciale pe care le poate contine.
	 Daca subelementul se mapeaza direct pe o categorie, se trece aceasta categorie ca valoare in hashmap  
	 */	
	 public static final  LinkedHashMap<String, String> allSpecialSubElements = new LinkedHashMap<String, String>();
	 public static final String DOCUMENT_IMAGE_MARKER = "_I";
	 public static final String DOCUMENT_PDF_MARKER = "_P";
	
	 
	 static { 	
		 
		 multilineTagSubElements.put(AddDocsTemplates.documents, new String[]{LEGAL,
				 															  ASSIGNMENTS,
				 															  APPOINTMENTS,
				 															  SUBORDINATIONS,
				 															  MODIFICATIONS,
				 															  RQNOTICE,
				 															  MISCELLANEOUS,
				 															  SUBSTITUTION,
				 															  ASSUMPTION,
				 															  HOAS,
				 															  RELEASES,
				 															  ALL_REFERENCES,
				 															  RECEIPTS,
				 															  INSTALLMENTS,
				 															  SA_INSTALLMENTS,
				 															  ADDRESS});
		 multilineTagSubElements.put(AddDocsTemplates.parcels, new String[]{});
		 multilineTagSubElements.put(AddDocsTemplates.amountsToRedeemFor, new String[]{});
		 multilineTagSubElements.put(AddDocsTemplates.currentOwners, new String[]{});
		 multilineTagSubElements.put(AddDocsTemplates.currentBuyers, new String[]{});
		 multilineTagSubElements.put(AddDocsTemplates.ssfStatements, new String[]{});
		 multilineTagSubElements.put(AddDocsTemplates.dataSources, new String[]{});
		 multilineTagSubElements.put(AddDocsTemplates.abstractors, new String[]{});
		 
		 final LinkedHashMap<String, String> tempMap = new LinkedHashMap<String, String>();
		 
		
		 
		 tempMap.put(MultilineElementsMap.ASSIGNMENTS, DocumentTypes.ASSIGNMENT);
		 tempMap.put(MultilineElementsMap.APPOINTMENTS, DocumentTypes.APPOINTMENT);
		 tempMap.put(MultilineElementsMap.SUBORDINATIONS, DocumentTypes.SUBORDINATION);
		 tempMap.put(MultilineElementsMap.MODIFICATIONS, DocumentTypes.MODIFICATION);
		 tempMap.put(MultilineElementsMap.RQNOTICE, DocumentTypes.RQNOTICE);
		 tempMap.put(MultilineElementsMap.MISCELLANEOUS, DocumentTypes.MISCELLANEOUS);
		 tempMap.put(MultilineElementsMap.SUBSTITUTION, DocumentTypes.SUBSTITUTION);
		 tempMap.put(MultilineElementsMap.ASSUMPTION, DocumentTypes.ASSUMPTION);
		 tempMap.put(MultilineElementsMap.RELEASES, DocumentTypes.RELEASE);
		 tempMap.put(MultilineElementsMap.ALL_REFERENCES, "");
		 tempMap.put(MultilineElementsMap.RECEIPTS, "");
		 tempMap.put(MultilineElementsMap.INSTALLMENTS, "");
		 tempMap.put(MultilineElementsMap.SA_INSTALLMENTS, "");
		 tempMap.put(MultilineElementsMap.LEGAL, "");
		 
		 tempMap.put(MultilineElementsMap.RESTRICTIONS, DocumentTypes.RESTRICTION);
		 tempMap.put(MultilineElementsMap.CCERS, DocumentTypes.CCER);
		 tempMap.put(MultilineElementsMap.HOAS, "");
		 tempMap.put(MultilineElementsMap.DEFAULTIFNOMATCH, "");
		 tempMap.put(MultilineElementsMap.ADDRESS, "");
		 
		 for(Map.Entry<String, String> entry:tempMap.entrySet()){
			 String origKey = entry.getKey();
			 String value = entry.getValue();
			 String key = origKey;
			 
			 if(key.endsWith("S")){
				 key = key.substring(0, key.length()-1);
			 }else{
				 key = key + "S";
			 }
			 
			 allSpecialSubElements.put(origKey, value);
			 allSpecialSubElements.put(key, value);
		 }
		 
		 
		 //mai intai elementele speciale		
		 String[] documents={
				CONTOR_TYPE_1,
				CONTOR_TYPE_2,
				SERVER_DOCUMENT_TYPE,
				GRANTOR,
				INSTRUMENT_DATE,
				FILLED_DATE,
				GRANTEE,
				GRANTEE_LENDER,
				GRANTEE_LANDER,
				GRANTEE_TRUSTEE,
				GRANTEE_TR,
				BOOK_NO,
				BOOK_TYPE,
                PAGE_NO,
                INSTRUMENT_NO,
                DOCUMENT_NO,
                
                BP_INST_DOC,
                INSTRUMENT_STCKC,
                BP_DOC_INST,
                INST_DOC_BP,
                INST_BP_DOC,
                DOC_INST_BP,
                DOC_BP_INST,
                
                BP_INST_DOC1,
                BP_DOC_INST1,
                INST_DOC_BP1,
                INST_BP_DOC1,
                DOC_INST_BP1,
                DOC_BP_INST1,
                
                BP_INST_DOC2,
                BP_INST_CO,
                BP_INST_TX,
                BP_INST_OH,
                
                BP_INST_DOC_ALL,
                
                MORTGAGE_AMOUNT,
                MORTGAGE_AMOUNT_FREE_FORM,
                LOAN_NO,
                CONSIDERATION_AMOUNT,
                
                DOCUMENT_ID,
                DOCUMENT_TYPE,
                DOCUMENT_SUBTYPE,
                DOCUMENT_REMARKS,
                
                DATE_ORDER_ASC,
                DATE_ORDER_DESC,
                TSR_INDEX_ORDER,
                FIRST_ORDER,
                LAST_ORDER,
                LAST_OWNER_VALID,
                NOTE,
                SSN4,
                
                DISABLE_BOILER_FEATURE,
                BOOK_PAGE_INSTNO_AK,
                BOOK_NV,
                INSTRUMENT_NV,
                CONTOR_TYPE_3,

                
                
                APPRAISED_VALUE_LAND,
            	APPRAISED_VALUE_IMPROVEMENTS,
            	APPRAISED_VALUE_TOTAL,
            	TOTAL_ASSESSMENT,
            	EXEMPTION_AMOUNT,
            	BASE_AMOUNT,
            	BASE_AMOUNT_CITY,
            	FOUND_DELINQUENT,
            	TOTAL_DELINQUENT,
            	AMOUNT_PAID,
            	AMOUNT_DUE,
            	DATE_PAID,
            	TAX_VOLUME,
            	SALE_DATE,
            	SALE_NO,
            	BILL_NUMBER,
            	RESEARCH_REQUIRED,
            	SPLIT_PAYMENT_AMOUNT,
            	PAY_DATE,
            	DUE_DATE,
            	HAS_HOMESTEAD_EXEMPTION,
            	TOTAL_INSTALL,
            	SA_TOTAL_INSTALL,
            	
            	TOTAL_ESTIMATED_TAXES,
            	

                DEFAULTIFNOMATCH,
                
                PRIOR_LD,
                PRIOR_EXCEPTIONS,
                PRIOR_REQUIREMENTS,
                
                DISTRICT,
                FINAL_PAYMENT,
                PREPAID_PRINCIPAL,
                CURRENT_DUE,
                TOTAL_PAYOFF,
                DUE_DATES_AM,
                INITIAL_PRINCIPAL,
                
                BPCODE
		 };
		  
		 ArrayList<String> newDocuments = new ArrayList<String>();
		 for( int i=0;i<documents.length;i++ ){
			 newDocuments.add( documents[i] );
			 newDocuments.add( documents[i]+DOCUMENT_IMAGE_MARKER );
			 newDocuments.add( documents[i]+DOCUMENT_PDF_MARKER );
		 }
		 
		 
		 
		 vecSpecialElements.add(newDocuments.toArray(new String[newDocuments.size()]));
		 
		 String []parcels = {
				 MultilineElementsMap.PARCEL_NO
		 };vecSpecialElements.add(parcels);
		 
		 String []amountsToRedeemFor = {
				 MultilineElementsMap.AMOUNT,
				 MultilineElementsMap.MONTH,
				 MultilineElementsMap.YEAR
		 };vecSpecialElements.add(amountsToRedeemFor);
		 
		 String []currentOwners={
				 FIRST,
				 MIDDLE,
				 MIDDLE_INITIAL,
	             LAST,
				 SUFFIX,
				 COUNTER,
				 GUID,
				 BUSINESS_NAME,
				 FIRST_BUSINESS_NAME,
				 PARTY_TYPE
		 };
		 vecSpecialElements.add(currentOwners);
		 vecSpecialElements.add(currentOwners);//mai adaug odata ptr buyers care are aceleasi subelelemente		 		 
		 vecSpecialElements.add(new String[]{STATEMENTS});
		 String[] datasources = {
				 DATASOURCE,
				 DATASOURCE_DESCRIPTION,
				 DATASOURCE_EFFECTIVE_START_DATE,
				 DATASOURCE_RUN_TYPE
		 }; vecSpecialElements.add(datasources);
		 
		 
		ABSTRACTORS[] abstractorsValues = ABSTRACTORS.values();
		String[] abstractors = new String[abstractorsValues.length];
		for (int i = 0; i < abstractorsValues.length; i++) {
			abstractors[i] = abstractorsValues[i].toString();
		}
		vecSpecialElements.add(abstractors);
		 	 
		 //efectuez maparea intre taguri si elementele speciale care pot sa apara
		 for(int i=0;i<allSpecialElements.length;i++){
				mapAssociatedElements.put( allSpecialElements[i],	vecSpecialElements.get(i) );
		 }
		 
		 // ****************** ---- urmeaza sub_elementele speciale -----------------------------
		 String assignments[] = {
				 GRANTEE,
				 GRANTOR,
				 GRANTEE_FML,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             INSTRUMENT_STCKC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	                
	             BP_INST_DOC1,
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             DEFAULTIFNOMATCH
		 };
		 vecSpecialSubElements.add(assignments);
		 
		 String appointments[] ={
				 GRANTEE,
				 GRANTOR,
				 GRANTEE_FML,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	             BP_INST_DOC1,
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             INSTRUMENT_STCKC,
	             DEFAULTIFNOMATCH
		 };
		 vecSpecialSubElements.add(appointments);
		 
		 // new subelements
		 String subordination[]={
				 GRANTEE,
				 GRANTOR,
				 GRANTEE_FML,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	             BP_INST_DOC1,
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             INSTRUMENT_STCKC,
	             DEFAULTIFNOMATCH
		 };
		 vecSpecialSubElements.add(subordination);
		 
		 
		 String modification[]={
				 GRANTOR,
				 GRANTOR_FML,
				 GRANTEE,
				 GRANTEE_FML,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	             BP_INST_DOC1,
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             INSTRUMENT_STCKC,
	             DEFAULTIFNOMATCH
		 };
		 vecSpecialSubElements.add(modification);
		 
		 String requestNoticeSale[]={
				 DEED_OF_TRUST_DOCUMENT_NO,
				 GRANTOR,
				 GRANTOR_FML,
				 GRANTEE,
				 GRANTEE_FML,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	             BP_INST_DOC1,
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             INSTRUMENT_STCKC,
	             DEFAULTIFNOMATCH
		 };
		 vecSpecialSubElements.add(requestNoticeSale);
		 
		 String miscellaneous[]={
				 GRANTOR,
				 GRANTEE,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	             BP_INST_DOC1,
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             INSTRUMENT_STCKC,
	             DEFAULTIFNOMATCH
		 };
		 vecSpecialSubElements.add(miscellaneous);
		 
		 String substitution[]={
				 GRANTOR,
				 GRANTEE,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	             BP_INST_DOC1,
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             INSTRUMENT_STCKC,
	             DEFAULTIFNOMATCH
		 };
		 vecSpecialSubElements.add(substitution);
		 
		 String assumption[]={
				 GRANTOR,
				 GRANTEE,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	             BP_INST_DOC1,	
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             INSTRUMENT_STCKC,
	             DEFAULTIFNOMATCH
		 };
		 vecSpecialSubElements.add(assumption);
		 
		 String releases[]={
				 GRANTOR,
				 GRANTEE,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	             BP_INST_DOC1,	
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             INSTRUMENT_STCKC,
	             DEFAULTIFNOMATCH
		 };
		 vecSpecialSubElements.add(releases);
		 
		 String all_references[]={
				 GRANTOR,
				 GRANTEE,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	             BP_INST_DOC1,	
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             INSTRUMENT_STCKC,
	             DEFAULTIFNOMATCH
		 };
		 vecSpecialSubElements.add(all_references);
		 
		 String receipts[]={
				 MultilineElementsMap.RECEIPT_NUMBER,
				 MultilineElementsMap.RECEIPT_DATE,
				 MultilineElementsMap.RECEIPT_AMOUNT,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	             BP_INST_DOC1,	
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             INSTRUMENT_STCKC,
	             DEFAULTIFNOMATCH
		 };vecSpecialSubElements.add(receipts);
		 
		 String installments[]={
				 MultilineElementsMap.INSTALL_NO,
				 MultilineElementsMap.BASE_AMOUNT,
				 MultilineElementsMap.AMOUNT_PAID,
				 MultilineElementsMap.AMOUNT_DUE,
				 MultilineElementsMap.PENALTY_AMOUNT,
				 MultilineElementsMap.HOMESTEAD_EXEMPTION,
				 MultilineElementsMap.STATUS,
				 MultilineElementsMap.YEAR_DESCRIPTION,
				 MultilineElementsMap.BILL_TYPE
		 };vecSpecialSubElements.add(installments);
		   
		   vecSpecialSubElements.add(installments);			//added again for Special Assessment Installments
		 
		 String legal[]={
				 LOT,
				 SUBLOT,
				 BLOCK,
				 SECTION,
				 PHASE,
				 UNIT,
				 SUBDIVISION,
				 CONDOMINIUM
		 };vecSpecialSubElements.add(legal);
		 
		 
		 
		 String restrictions[] = {
				 GRANTEE,
				 GRANTOR,
				 GRANTEE_FML,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	                
	             BP_INST_DOC1,
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             DEFAULTIFNOMATCH,
	             INSTRUMENT_STCKC
		 };
		 vecSpecialSubElements.add(restrictions);
		 
		 String ccers[] = {
				 GRANTEE,
				 GRANTOR,
				 GRANTEE_FML,
				 INSTRUMENT_DATE,
				 FILLED_DATE,
				 BOOK_NO,
				 BOOK_TYPE,
				 PAGE_NO,
	             INSTRUMENT_NO,
	             DOCUMENT_NO,
	             BP_INST_DOC,
	             BP_DOC_INST,
	             INST_DOC_BP,
	             INST_BP_DOC,
	             DOC_INST_BP,
	             DOC_BP_INST,
	                
	             BP_INST_DOC1,
	             BP_DOC_INST1,
	             INST_DOC_BP1,
	             INST_BP_DOC1,
	             DOC_INST_BP1,
	             DOC_BP_INST1,
	             
	             BP_INST_DOC2,
	             BP_INST_CO,
	             BP_INST_TX,
	             BP_INST_OH,
	             BP_INST_DOC_ALL,
	             SERVER_DOCUMENT_TYPE,
	             DOCUMENT_ID,
	             DOCUMENT_TYPE,
	             DOCUMENT_REMARKS,
	             DOCUMENT_SUBTYPE,
	             NOTE,
	             SSN4,
	             BOOK_PAGE_INSTNO_AK,
	             BOOK_NV,
	             INSTRUMENT_NV,
	             CONTOR_TYPE_3,
	             INSTRUMENT_STCKC,
	             DEFAULTIFNOMATCH
		 };
		 
		 vecSpecialSubElements.add(ccers);
		 
		 String hoas[]={
				 MultilineElementsMap.ASSOCIATION_NAME,
				 MultilineElementsMap.HOA_NAME,
				 MultilineElementsMap.MASTER_HOA,
				 MultilineElementsMap.ADD_HOA,
				 MultilineElementsMap.HOA_PLAT_BOOK,
				 MultilineElementsMap.HOA_PLAT_PAGE,
				 MultilineElementsMap.HOA_DECL_BOOK,
				 MultilineElementsMap.HOA_DECL_PAGE,
				 
		 };
		 vecSpecialSubElements.add(hoas);
		 
		 String defaultifnomatch[]={
				 DEFAULTIFNOMATCH
		 };
		 vecSpecialSubElements.add(defaultifnomatch);
		 
		 String address[]={
				 CITY
		 };
		 vecSpecialSubElements.add(address);
		 
		 		 
		 for(int j=0; j<vecSpecialSubElements.size(); j++) {
			 String[] subElements = vecSpecialSubElements.get(j);
			 ArrayList<String> newSubElements = new ArrayList<String>();
			 for( int i=0; i<subElements.length; i++){
				 newSubElements.add(subElements[i]);
				 newSubElements.add(subElements[i] + DOCUMENT_IMAGE_MARKER);
				 newSubElements.add(subElements[i] + DOCUMENT_PDF_MARKER);
			 }		 
			 vecSpecialSubElements.set(j, newSubElements.toArray(new String[newSubElements.size()]));
		 }
		 
		 //		efectuez maparea intre elementele speciale si subelementele speciale care pot sa apara
		 int i = 0;
		 int j = 0;
		 for(String subelement : allSpecialSubElements.keySet()) {
				mapAssociatedSubElements.put(subelement,vecSpecialSubElements.get(i));
				j++;
				if(j%2==0){
					i++;
				}
		 }
		
	 }
	                             
	
	//------------------- campurile obiectului ---------------------------------
	private int curentType = ELEMENT_TYPE;
	private String referedString = "";
	private boolean autoConvertEmpty = false;
	private boolean autoConvertNull = false;
	private String defaultString = "";
	
	//------------------- functiile obiectului ---------------------------------------
	public MultilineElementsMap (int type,String referedElement, String initializeAllFieldsWith) throws MultilineElementsMapException{
		super();
		if(type != MultilineElementsMap.ELEMENT_TYPE && type != MultilineElementsMap.SUBELEMENT_TYPE){
			throw new MultilineElementsMapException("The type must be on of the: MultilineElementsMap.SUBELEMENT_TYPE or MultilineElementsMap.ELEMENT_TYPE");
		}
		this.curentType = type;
		this.referedString = referedElement;
		
		if(initializeAllFieldsWith != null) {
			for(int i=0; i<allSpecialElements.length ; i++) {
				if(allSpecialElements[i].equals(referedElement)) {
					for(String tag : vecSpecialElements.get(i) ) {
						put(tag,initializeAllFieldsWith);
					}
					break;
				}
			}
		}
	}
	
	public MultilineElementsMap (int type,String referedElement) throws MultilineElementsMapException{
		this(type,referedElement, null);
	}

	/**
	 * When this is true, the put(key,value) operation will put a default string instead of an empty value
	 * @return
	 */
	public boolean isAutoConvertEmpty() {
		return autoConvertEmpty;
	}

	public void setAutoConvertEmpty(boolean autoConvertEmpty) {
		this.autoConvertEmpty = autoConvertEmpty;
	}
	
	/**
	 * When this is true, the get(key) operation will return a default string if no key exists.  
	 * @return
	 */
	public boolean isAutoConvertNull() {
		return autoConvertNull;
	}

	public void setAutoConvertNull(boolean autoConvertNull) {
		this.autoConvertNull = autoConvertNull;
	}

	public boolean isGoodKeyForReferedElement(String key){
		String[] associated = null;
		if(this.curentType == MultilineElementsMap.ELEMENT_TYPE){
			associated = mapAssociatedElements.get(this.referedString);
		
		}
		else{
			associated = mapAssociatedSubElements.get(this.referedString);
		}
		if(associated !=null){
			for(int i=0;i<associated.length;i++){
				if (key.contains(AddDocsTemplates.KEY_SEPARATOR_FOR_TAG)){
					key = key.substring(0, key.indexOf(AddDocsTemplates.KEY_SEPARATOR_FOR_TAG));
				}
				if(associated[i].equals(key)){
					return true;
				}
			}
		}
		return false;
	}
	
	public String put(String key,String value) throws MultilineElementsMapException {
		return putValue(key,value,true);
	}
	
	public String putUnchanged(String key,String value) throws MultilineElementsMapException {
		return putValue(key,value,false);
	}
	
	public String putValue(String key,String value, boolean doExtraProcessing) throws MultilineElementsMapException {
		
		if(this.curentType == MultilineElementsMap.ELEMENT_TYPE){
			if(!this.isGoodKeyForReferedElement(key)){
				throw new MultilineElementsMapException("You must use just the special elements coresponding to the curent tag...\nCan't use element = "+key+ " with tag = "+this.referedString);
			}
		}
		else{ //este de tip map pt SUBELEMENT
			if(!this.isGoodKeyForReferedElement(key)){
				throw new MultilineElementsMapException("You must use just the special subelements coresponding to the curent special elements...\nCan't use special subelement = "+key+ " with special elements = "+this.referedString);
			}
		}
		
		if(doExtraProcessing) {
			if(isAutoConvertEmpty()) {
				if(StringUtils.isEmpty(value)) {
					value = defaultString; 
				}
			}
		}
		return super.put(key, value);
	}
	
	@Override
	public String get(Object key) {		
		if(isAutoConvertNull() && super.get(key)==null) { 
			return defaultString;
		}
		else {
			return super.get(key); 
		}
	}
	
	//-------------------- functiile clasei -----------------------------------
	public static final boolean isDefinedAsMultilineTag(String tag){
		
		for(int i=0; i<allSpecialElements .length ;i++){
			if(tag.equals( allSpecialElements[i] )) {
				return true;
			}
		}
		return false;
	}

	public int getCurentType() {
		return curentType;
	}

	public void setCurentType(int curentType) {
		this.curentType = curentType;
	}

	public String getDefaultString() {
		return defaultString;
	}

	public void setDefaultString(String defaultString) {
		this.defaultString = defaultString;
	}
	
	public static boolean multilineTagContainsElement(String multilineTag, String element, boolean isForThirdLevel) {
		String[] list = null;
		if (isForThirdLevel) {
			list = multilineTagSubElements.get(multilineTag);
		} else {
			list = mapAssociatedElements.get(multilineTag);
		}
		if (list!=null) {
			for (int i=0;i<list.length;i++) {
				if (element.equals(list[i])) {
					return true;
				}
			} 
		}
		return false;
	}
	
	public static boolean multilineTagContainsElementSubElement(String multilineTag, String element, String subElement) {
		String[] list1 = multilineTagSubElements.get(multilineTag);
		if (list1!=null) {
			for (int i=0;i<list1.length;i++) {
				if (element.equals(list1[i])) {
					String[] list2 =  mapAssociatedSubElements.get(element);
					if (list2!=null) {
						for (int j=0;j<list2.length;j++) {
							if (subElement.equals(list2[j])) {
								return true;
							}
						}
					}
					return false;
				}
			} 
		}
		return false;
	}

	public static HashMap<String, Class<?>> DOCUMENTS_TAGS_MAP = new HashMap<String, Class<?>>();
	
	static{
		DOCUMENTS_TAGS_MAP = new HashMap<String, Class<?>>();
		for(DocumentsTags dts : DocumentsTags.values()){
			DOCUMENTS_TAGS_MAP.put(dts.getName(), dts.getClassType());
		}
	}
	
	enum DocumentsTags {
//		CONTOR_TYPE_1 (MultilineElementsMap.CONTOR_TYPE_1, String.class),
//		CONTOR_TYPE_2 (MultilineElementsMap.CONTOR_TYPE_2, String.class),
//		SERVER_DOCUMENT_TYPE (MultilineElementsMap.SERVER_DOCUMENT_TYPE, String.class),
//		GRANTOR (MultilineElementsMap.GRANTOR, String.class),
//		INSTRUMENT_DATE (MultilineElementsMap.INSTRUMENT_DATE, Date.class),
//		FILLED_DATE (MultilineElementsMap.FILLED_DATE, Date.class),
//		GRANTEE (MultilineElementsMap.GRANTEE, String.class),
//		GRANTEE_LENDER (MultilineElementsMap.GRANTEE_LENDER, String.class),
//		GRANTEE_LANDER (MultilineElementsMap.GRANTEE_LANDER, String.class),
//		GRANTEE_TRUSTEE(MultilineElementsMap.GRANTEE_TRUSTEE, String.class),
//		GRANTEE_TR (MultilineElementsMap.GRANTEE_TR, String.class),
//		BOOK_NO (MultilineElementsMap.BOOK_NO, String.class),
//		BOOK_TYPE (MultilineElementsMap.BOOK_TYPE, String.class),
//      PAGE_NO (MultilineElementsMap.PAGE_NO, String.class),
//      INSTRUMENT_NO (MultilineElementsMap.INSTRUMENT_NO, String.class),
//      DOCUMENT_NO (MultilineElementsMap.DOCUMENT_NO, String.class),
        
//        BP_INST_DOC,
//        INSTRUMENT_STCKC,
//        BP_DOC_INST,
//        INST_DOC_BP,
//        INST_BP_DOC,
//        DOC_INST_BP,
//        DOC_BP_INST,
//        
//        BP_INST_DOC1,
//        BP_DOC_INST1,
//        INST_DOC_BP1,
//        INST_BP_DOC1,
//        DOC_INST_BP1,
//        DOC_BP_INST1,
//        
//        BP_INST_DOC2,
//        BP_INST_CO,
//		  BP_INST_TX,
//		  BP_INST_OH,
//        
//        BP_INST_DOC_ALL,
//        
//        MORTGAGE_AMOUNT,
//        MORTGAGE_AMOUNT_FREE_FORM,
//        LOAN_NO,
//        CONSIDERATION_AMOUNT,
//        
//        DOCUMENT_ID,
//        DOCUMENT_TYPE,
//        DOCUMENT_SUBTYPE,
//        DOCUMENT_REMARKS,
//        
//        DATE_ORDER_ASC,
//        DATE_ORDER_DESC,
//        TSR_INDEX_ORDER,
//        FIRST_ORDER,
//        LAST_ORDER,
//        LAST_OWNER_VALID,
//        NOTE,
//        SSN4,
//        
//        DISABLE_BOILER_FEATURE,
//        BOOK_PAGE_INSTNO_AK,
//        BOOK_NV,
//        INSTRUMENT_NV,
//        CONTOR_TYPE_3,

        
        
        APPRAISED_VALUE_LAND (MultilineElementsMap.APPRAISED_VALUE_LAND, double.class),
    	APPRAISED_VALUE_IMPROVEMENTS (MultilineElementsMap.APPRAISED_VALUE_IMPROVEMENTS, double.class),
    	APPRAISED_VALUE_TOTAL (MultilineElementsMap.APPRAISED_VALUE_TOTAL, double.class),
    	TOTAL_ASSESSMENT (MultilineElementsMap.TOTAL_ASSESSMENT, double.class),
    	EXEMPTION_AMOUNT (MultilineElementsMap.EXEMPTION_AMOUNT, double.class),
    	BASE_AMOUNT (MultilineElementsMap.BASE_AMOUNT, double.class),
    	BASE_AMOUNT_CITY (MultilineElementsMap.BASE_AMOUNT_CITY, double.class),
    	FOUND_DELINQUENT (MultilineElementsMap.FOUND_DELINQUENT, double.class),
    	TOTAL_DELINQUENT (MultilineElementsMap.TOTAL_DELINQUENT, double.class),
    	AMOUNT_PAID (MultilineElementsMap.AMOUNT_PAID, double.class),
    	AMOUNT_DUE (MultilineElementsMap.AMOUNT_DUE, double.class),
    	DATE_PAID (MultilineElementsMap.DATE_PAID, String.class),
    	TAX_VOLUME (MultilineElementsMap.TAX_VOLUME, int.class),
    	SALE_DATE (MultilineElementsMap.SALE_DATE, String.class),
    	SALE_NO (MultilineElementsMap.SALE_NO, String.class),
    	BILL_NUMBER (MultilineElementsMap.BILL_NUMBER, String.class),
    	RESEARCH_REQUIRED (MultilineElementsMap.RESEARCH_REQUIRED, boolean.class),
    	SPLIT_PAYMENT_AMOUNT (MultilineElementsMap.SPLIT_PAYMENT_AMOUNT, double.class),
    	PAY_DATE (MultilineElementsMap.PAY_DATE, String.class),
    	DUE_DATE (MultilineElementsMap.DUE_DATE, String.class),
    	HAS_HOMESTEAD_EXEMPTION (MultilineElementsMap.HAS_HOMESTEAD_EXEMPTION, boolean.class),
        TOTAL_INSTALL (MultilineElementsMap.TOTAL_INSTALL, int.class),
        SA_TOTAL_INSTALL (MultilineElementsMap.SA_TOTAL_INSTALL, int.class);
    	
//    	TOTAL_ESTIMATED_TAXES,
//    	
//
//        DEFAULTIFNOMATCH,
//        
//        PRIOR_LD,
//        PRIOR_EXCEPTIONS,
//        PRIOR_REQUIREMENTS,
//        
//        DISTRICT,
//        FINAL_PAYMENT,
//        PREPAID_PRINCIPAL,
//        CURRENT_DUE,
//        TOTAL_PAYOFF,
//        DUE_DATES_AM,
//        INITIAL_PRINCIPAL
        
        private String name;
        private Class<?> classType; 
        
        DocumentsTags(String name, Class<?> classType){
			this.name = name;
			this.classType = classType;
		}
        
        public String getName() { 
        	return name; 
        }
        
        public Class<?> getClassType() { 
        	return classType; 
        }
};
}




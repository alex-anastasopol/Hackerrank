package ro.cst.tsearch.servers.response;

public class PropertyIdentificationSet extends InfSet {

  
	private static final long serialVersionUID = -6513318866536085364L;

	public PropertyIdentificationSet() {
        super(new String[]{"ParcelID", 
        					"ParcelID2", 		// used only with RedVision (Florida)
        					"ParcelIDNDB", 		// used only with RedVision (Florida)
        					"ParcelID2_ALTERNATE", // used only with RedVision (Florida)
        					"ParcelID3", 			// used only with DataTrace (Florida)
        					"ARB",       		// currently used only with NDB Florida
        					"QuarterOrder",       // currently used only with NDB Florida
        					"QuarterValue",       // currently used only with NDB Florida
        					"StreetName", 
        					"StreetNo", 
        					"City", 
        					"State", 
        					"Zip", 
     						"AddressOnServer", 		// full property address, as it appears on document server (not tokenized)
                           "OwnerFirstName", 
                           "OwnerMiddleName", 
                           "OwnerLastName", 
                           "SpouseFirstName", 
                           "SpouseMiddleName", 
                           "SpouseLastName",
                           "Suffix",
                           "NameOnServer",		// full name of owner(s), as it appears on document server (not tokenized)
                           "SubdivisionName",
                           "SubdivisionNo",
                           "SubdivisionLotNumber", 
                           "PlatBook", 
                           "PlatNo", 			//un fel de page (PlatPage)
                           "PlatInstr",
                           "PlatInstrYear",
                           "PlanNo",   			//populate with value just for Wayne but unread
                           "MunicipalJurisdiction", 
                           "Subdivision", 
                           "SubdivisionPhase", 
                           "SubdivisionUnit", 
                           "SubdivisionCode", 
                           "SubdivisionBlock", 
                           "SubdivisionTract", 
                           "LegalDescriptionOnServer", // legal description as it appears on document server (not tokenized) - one value for each legal description
                           "YearBuilt", 
                           "SubdivisionCond",	//isCondo flag. If there is something in this field it means we have a condo
                           "BookPageRefs",   //used just for Rutherford and Montgomery wiil be eliminated
                           "LegalRef",		 //used for Rutherford ,Montgomery , Sumner legal
                           "Score",			 //used for site score in the PAST
                           "SubdivisionSection", 
                           "SubdivisionTownship",
                           "SubdivisionRange" , 
                           "SubdivisionBldg" ,
                           "ParcelIDParcel" , 
                           "ParcelIDMap", 
                           "ParcelIDGroup",
                           "ParcelIDCondo",
                           "County", 
                           "ParentParcelID",
                           "PropertyDescription",	// legal description as it appears on document server (not tokenized) - one value for the document (if doc hase multiple legal, the first one will be recorded in PropertyDescription
                           "Acres",					//used on TNSumner
                           "CondominiumPlatBook",	//unread
                           "CondominiumPlatPage",  //unread
                           "PartialLegal",	//used now just for Cook
                           "PropertyType",
                           "GeoNumber",
                           "AreaCode",	// needed in California DT tax 
                           "AreaName",	// needed in California DT tax 
                           "District",   //needed for TNSumner
                           "Addition",
                           "SubdivisionParcel",//ARPulaskiTS, reused for DuvalDT GenericDasl
                           //Thru needed for DT
                           "ThruSections",
                           "ThruRange",
                           "ThruQuartersOrder",
                           "ThruQuartersValue",
                           "ThruParcel",
                           "Area",//FlDuvalDT
                           "ThruArea",
                           "ThruTownship",
                           "PlatDesc",
                           //LA
                           "SubdivisionBlockThrough",
                           "SubdivisionLotThrough",
                           "Acreage",//A number used for SanAntonio counties. It is used in combination with the tract number. 
                           "NcbNo",
                           "AbsNo",//Used for Texas
                           "SubLot",
                           "Status",//for deleted entries; need onFLHenrnandoTR
                           "Section",	//This must be used in ro.cst.tsearch.bean.SearchAttributes.LD_SECTION (it's not related to township)
                           "OwnerZipCode"
                     });
    }
	
	public enum PropertyIdentificationSetKey{
		PARCEL_ID("ParcelID"), 
		PARCEL_ID2("ParcelID2"), 		// used only with RedVision (Florida)
		PARCEL_ID_NDB("ParcelIDNDB"), 		// used only with RedVision (Florida)
		PARCEL_ID2_ALTERNATE("ParcelID2_ALTERNATE"), // used only with RedVision (Florida)
		PARCEL_ID3("ParcelID3"), 			// used only with DataTrace (Florida)
		ARB("ARB"),       		// currently used only with NDB Florida
		QUARTER_ORDER("QuarterOrder"),       // currently used only with NDB Florida
		QUARTER_VALUE("QuarterValue"),       // currently used only with NDB Florida
		STREET_NAME("StreetName"), 
		STREET_NO("StreetNo"), 
		CITY("City"), 
		STATE("State"), 
		ZIP("Zip"), 
		ADDRESS_ON_SERVER("AddressOnServer"), 		// full property address, as it appears on document server (not tokenized)
		OWNER_FIRST_NAME("OwnerFirstName"), 
		OWNER_MIDDLE_NAME("OwnerMiddleName"), 
		OWNER_LAST_NAME("OwnerLastName"), 
		SPOUSE_FIRST_NAME("SpouseFirstName"), 
		SPOUSE_MIDDLE_NAME("SpouseMiddleName"), 
		SPOUSE_LAST_NAME("SpouseLastName"),
		SUFFIX("Suffix"),
		NAME_ON_SERVER("NameOnServer"),		// full name of owner(s), as it appears on document server (not tokenized)
		SUBDIVISION_NAME("SubdivisionName"),
		SUBDIVISION_NO("SubdivisionNo"),
		SUBDIVISION_LOT_NUMBER("SubdivisionLotNumber"), 
		PLAT_BOOK("PlatBook"), 
		PLAT_NO("PlatNo"), 			//un fel de page (PlatPage)
		PLAT_INSTR("PlatInstr"),
		PLAT_INSTR_YEAR("PlatInstrYear"), 
		PLAN_NO("PlanNo"),   			//populate with value just for Wayne but unread
		MUNICIPAL_JURISDICTION("MunicipalJurisdiction"), 
		SUBDIVISION ("Subdivision"), 
		SUBDIVISION_PHASE("SubdivisionPhase"), 
		SUBDIVISION_UNIT("SubdivisionUnit"), 
		SUBDIVISION_CODE("SubdivisionCode"), 
		SUBDIVISION_BLOCK("SubdivisionBlock"), 
		SUBDIVISION_TRACT("SubdivisionTract"), 
		LEGAL_DESCRIPTION_ON_SERVER("LegalDescriptionOnServer"), // legal description as it appears on document server (not tokenized) - one value for each legal description
		YEAR_BUILT("YearBuilt"), 
		SUBDIVISION_COND("SubdivisionCond"),	//isCondo flag. If there is something in this field it means we have a condo
		BOOK_PAGE_REFS("BookPageRefs"),   //used just for Rutherford and Montgomery wiil be eliminated
		LEGAL_REF("LegalRef"),		 //used for Rutherford ,Montgomery , Sumner legal
		SCORE("Score"),			 //used for site score in the PAST
		SUBDIVISION_SECTION("SubdivisionSection"), 
		SUBDIVISION_TOWNSHIP("SubdivisionTownship"),
		SUBDIVISION_RANGE("SubdivisionRange"), 
		SUBDIVISION_BLDG("SubdivisionBldg") ,
		PARCEL_ID_PARCEL("ParcelIDParcel"), 
		PARCEL_ID_MAP("ParcelIDMap"), 
		PARCEL_ID_GROUP("ParcelIDGroup"),
		PARCEL_ID_CONDO("ParcelIDCondo"),
		COUNTY("County"), 
		PARENT_PARCEL_ID ("ParentParcelID"),
		PROPERTY_DESCRIPTION("PropertyDescription"),	// legal description as it appears on document server (not tokenized) - one value for the document (if doc hase multiple legal, the first one will be recorded in PropertyDescription
		ACRES("Acres"),					//used on TNSumner
		CONDOMINIUM_PLAT_BOOK("CondominiumPlatBook"),	//unread
		CONDOMINIUM_PLAT_PAGE("CondominiumPlatPage"),  //unread
		PARTIAL_LEGAL("PartialLegal"),	//used now just for Cook
		PROPERTY_TYPE("PropertyType"),
		GEO_NUMBER ("GeoNumber"),
		AREA_CODE("AreaCode"),	// needed in California DT tax 
		AREA_NAME ("AreaName"),	// needed in California DT tax 
		DISTRICT ("District"),   //needed for TNSumner
		ADDITION ("Addition"),
		SUBDIVISION_PARCEL("SubdivisionParcel"),//ARPulaskiTS, reused for DuvalDT GenericDasl
       //Thru needed for DT
		THRU_SECTIONS("ThruSections"),
		THRU_RANGE ("ThruRange"),
		THRU_QUARTERS_ORDER("ThruQuartersOrder"),
		THRU_QUARTERS_VALUE ("ThruQuartersValue"),
		THRU_PARCEL("ThruParcel"),
		AREA("Area"),//FlDuvalDT
		THRU_AREA("ThruArea"),
		THRU_TOWNSHIP("ThruTownship"),
		PLAT_DESC("PlatDesc"),
       //LA
		SUBDIVISION_BLOCK_THROUGH ("SubdivisionBlockThrough"),
		SUBDIVISION_LOT_THROUGH("SubdivisionLotThrough"),
		ACREAGE("Acreage"),//A number used for SanAntonio counties. It is used in combination with the tract number. 
		NCB_NO("NcbNo"),
		ABS_NO("AbsNo"),//Used for Texas
		SUB_LOT("SubLot"),
		STATUS("Status"),
		SECTION("Section"),
		OWNER_ZIP_CODE("OwnerZipCode");
		
		String keyName;
		String shortKeyName;
		PropertyIdentificationSetKey(String keyName){
			this.shortKeyName = keyName;
			this.keyName = "PropertyIdentificationSet."+keyName;
		}
       
		public String getKeyName() {
			return keyName;
		}
		public String getShortKeyName() {
			return shortKeyName;
		}
	}
}

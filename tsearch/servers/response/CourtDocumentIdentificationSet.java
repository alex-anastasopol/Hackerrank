/*
 * Created on Jul 9, 2005
 */
package ro.cst.tsearch.servers.response;

/**
 * @author vladp
 */
public class CourtDocumentIdentificationSet extends InfSet {
    
	private static final long serialVersionUID = -6246067377117396788L;

	public CourtDocumentIdentificationSet() {
        super(
        	new String[]{
        		"PartyName", 	//	unused yet in the new structures
        		"PartyType",  	//	unused yet in the new structures
        		"CaseNumber", 	//	use  SaleDataSet.instrumentNumber instead
        		"CaseStyle",  	//	unused in the new structures
        		"CaseType",   	//	replaced by SaleDataSet.DocumentType
                "FillingDate",	//  replaced by SaleDataSet.RecordedDate
                "AddressOnFile", // just parsed not used  yet
                "Circuit",		// just parsed not used  yet
                "County",		// just parsed not used  yet
                "Location" , 	// just parsed not used  yet
                "Disposition",		//just parsed not used yet
                "DispositionDate", 	//just parsed not used yet
                "JudgeName"			//just parsed not used yet
        	}
        );
    }
	
	public enum  CourtDocumentIdentificationSetKey {
		PARTY_NAME("PartyName"),
		PARTY_TYPE("PartyType"),
		CASE_NUMBER("CaseNumber"),
		CASE_STYLE("CaseStyle"),
		CASE_TYPE("CaseType"),
		FILLING_DATE("FillingDate"),
		ADDRESS_ON_FILE("AddressOnFile"),
		CIRCUIT("Circuit"),
		COUNTY("County"),
		LOCATION("Location"),
		DISPOSITION("Disposition"),
		DISPOSITION_DATE("DispositionDate"),
		JUDGE_NAME("JudgeName");
		
		String keyName;
		String shortKeyName;
		CourtDocumentIdentificationSetKey(String keyName) {
			this.shortKeyName = keyName;
			this.keyName = "CourtDocumentIdentificationSet." + keyName;
		}

		public String getKeyName() {
			return keyName;
		}
		
		public String getShortKeyName() {
			return shortKeyName;
		}
	}

}



package ro.cst.tsearch.servers.response;

public class SaleDataSet extends InfSet {

	private static final long serialVersionUID = 9216517100435126528L;

	public SaleDataSet() {
        super(
        		new String[]{
        				"Grantor",  
        				"Grantee",  
        				"InstrumentNumber", 
        				"DocumentNumber", 
        				"Book",  
                        "Page",  
                        "RecordedDate",  
                        "InstrumentDate", 
                        "RecordedTime" , 
                        "PreparedDate",  //should be removed it is equals with instrumentDate
                        "DocumentType",  
                        "CrossRefInstrument",  //should be removed in 5.0 it used just to set chapters crossreff
                        "PropertyClass",  //parsed but unused
                        "SalesPrice", 	  //parsed but unused
                        "Docket", 		  //used just internal in GenericFunction
                        "AdInstrNo", 	//shoud be removed in 5.0 , pentru Rutherford si Montgomery...s-a facut o exceptie,  
                        				//in campul de instrNo s-a pus B_P si instrumentul se tine in SaleDateSet/AdInstrNo                       
                        "Book_Page",	//should be removed in 5.0
                        
                        "GranteeTR", 	//shoud be removed in 5.0 -- all need to read from GranteeSet, GrantorSet
                        "GranteeLander", //shoud be removed in 5.0-- all need to read from GranteeSet, GrantorSet
                        //we need to add a field in GranteeSet to specify if it is Trustee or Lander
                        
                        "MortgageAmount", 
                        "DocumentTypeOriginal", //should be removed in 5.0
                        "DocTypeAbbrev", 	
                        "DocSubtype", 
                        "ConsiderationAmount", 
                        "Remarks", 			//used just for RV in old file AddeskRemarks that will be replaced in 5.0
                        "BookType", 
                        "InstrCode", // needed for transaction history on  FLCitrusTR
                        "SearcherExtensions",
                        "FinanceBook",
                        "FinancePage",
                        "FinanceInstrumentNumber",
                        "FinanceRecordedDate",
                        "OeRating"
                     });
    }
	
	public enum  SaleDataSetKey {
		GRANTOR("Grantor"),
		GRANTEE("Grantee"),
		INSTRUMENT_NUMBER("InstrumentNumber"),
		DOCUMENT_NUMBER("DocumentNumber"),
		BOOK("Book"),
		PAGE("Page"),
		RECORDED_DATE("RecordedDate"),
		INSTRUMENT_DATE("InstrumentDate"),
		RECORDED_TIME("RecordedTime"),
		PREPARED_DATE("PreparedDate"),
		DOCUMENT_TYPE("DocumentType"),
		CROSS_REF_INSTRUMENT("CrossRefInstrument"),
		PROPERTY_CLASS("PropertyClass"),
		SALES_PRICE("SalesPrice"),
		DOCKET("Docket"),
		AD_INSTR_NO("AdInstrNo"),
		BOOK_PAGE("Book_Page"),
		GRANTEE_TR("GranteeTR"),
		GRANTEE_LANDER("GranteeLander"),
		MORTGAGE_AMOUNT("MortgageAmount"),
		DOCUMENT_TYPE_ORIGINAL("DocumentTypeOriginal"),
		DOC_TYPE_ABBREV("DocTypeAbbrev"),
		DOC_SUBTYPE("DocSubtype"),
		CONSIDERATION_AMOUNT("ConsiderationAmount"),
		REMARKS("Remarks"),
		BOOK_TYPE("BookType"),
		INSTR_CODE("InstrCode"),
		SEARCHER_EXTENSIONS("SearcherExtensions"),
		OE_RATING("OeRating"),
		FINANCE_INST_NO("FinanceInstrumentNumber"),
		FINANCE_RECORDED_DATE("FinanceRecordedDate"),
		FINANCE_BOOK("FinanceBook"),
		FINANCE_PAGE("FinancePage");
				
		String keyName;
		String shortKeyName;
		
		SaleDataSetKey(String keyName) {
			this.shortKeyName = keyName;
			this.keyName = "SaleDataSet." + keyName;
		}
		/**
		 * @return "SaleDataSet.Field"
		 */
		public String getKeyName() {
			return keyName;
		}
		public String getShortKeyName() {
			return shortKeyName;
		}
	}

	
}

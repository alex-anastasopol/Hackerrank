package ro.cst.tsearch.servers.response;

public class CrossRefSet extends InfSet {

	private static final long serialVersionUID = -1855082346289475773L;

	public CrossRefSet() {
        super(
        		new String[]{
        				"InstrumentNumber", 
        				"Book", 
        				"Page", 
        				"DocumentNumber",
        				"DocumentType",
        				"DocSubtype",
        				"Book_Page",
        				"Book_Page_Type",
        				"Instrument_Ref_Type", 
        				"CrossRefType",
        				"CrossRefSource",
        				"Year",
        				"Month",
        				"Day",
        				}
        		);
    }
	
	public enum  CrossRefSetKey {
		INSTRUMENT_NUMBER("InstrumentNumber"),
		BOOK("Book"),
		PAGE("Page"),
		DOCUMENT_NUMBER("DocumentNumber"),
		DOCUMENT_TYPE("DocumentType"),
		DOC_SUBTYPE("DocSubtype"),
		BOOK_PAGE("Book_Page"),
		BOOK_PAGE_TYPE("Book_Page_Type"),
		INSTRUMENT_REF_TYPE("Instrument_Ref_Type"),
		CROSS_REF_TYPE("CrossRefType"),
		CROSS_REF_SOURCE("CrossRefSource"),
		YEAR("Year"),
		MONTH("Month"),
		DAY("Day");
		
		String keyName;
		String shortKeyName;
		CrossRefSetKey(String keyName) {
			this.shortKeyName = keyName;
			this.keyName = "CrossRefSet." + keyName;
		}

		public String getKeyName() {
			return keyName;
		}
		public String getShortKeyName() {
			return shortKeyName;
		}
	}

}

package ro.cst.tsearch.servers.response;

public class TaxRedemptionSet extends InfSet {
	
	private static final long serialVersionUID = -1317511353337287297L;

	public TaxRedemptionSet() {	// used with California DataTrace taxes
        super(new String[]{
        		"Month", 			
        		"Year",  
        		"Amount" 			
        		});
	}
	
	public enum  TaxRedemptionSetKey {
		MONTH("Month"),
		YEAR("Year"),
		AMOUNT("Amount");
		
		String keyName;

		TaxRedemptionSetKey(String keyName) {
			this.keyName = "TaxRedemptionSet." + keyName;
		}

		public String getKeyName() {
			return keyName;
		}
	}

}

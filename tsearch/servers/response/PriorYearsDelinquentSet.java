package ro.cst.tsearch.servers.response;

public class PriorYearsDelinquentSet extends InfSet {
	
	private static final long serialVersionUID = -5929011155777585986L;

	public PriorYearsDelinquentSet() {	// used with California DataTrace taxes
        super(new String[]{
        		"TaxPeriod", 			
        		"Installment",  
        		"TaxBillType",
        		"AmountDue"
        		});
	}
	
	public enum  PriorYearsDelinquentSetKey {
		TAX_PERIOD("TaxPeriod"),
		INSTALLMENT("Installment"),
		TAX_BILL_TYPE("TaxBillType"),
		AMOUNT_DUE("AmountDue");
		
		String keyName;

		PriorYearsDelinquentSetKey(String keyName) {
			this.keyName = "PriorYearsDelinquentSet." + keyName;
		}

		public String getKeyName() {
			return keyName;
		}
	}

}

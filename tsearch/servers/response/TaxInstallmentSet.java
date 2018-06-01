package ro.cst.tsearch.servers.response;

public class TaxInstallmentSet extends InfSet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7641173129072884525L;

	public TaxInstallmentSet() {
        super(new String[]{
        		"InstallmentName",
                "BaseAmount", 		//base amount for current installment
                "AmountPaid",       //amount paid for current installment
                "TotalDue",			//amount due for current installment
                "PenaltyAmount",
                "HomesteadExemption",
                "Status",
                "TaxYearDescription",
                "TaxBillType"});		
	}
	
	public enum  TaxInstallmentSetKey {
		INSTALLMENT_NAME("InstallmentName"),
		BASE_AMOUNT("BaseAmount"),
		AMOUNT_PAID("AmountPaid"),
		TOTAL_DUE("TotalDue"),
		PENALTY_AMOUNT("PenaltyAmount"),
		HOMESTEAD_EXEMPTION("HomesteadExemption"),
		STATUS("Status"),
		TAX_YEAR_DESCRIPTION("TaxYearDescription"),
		TAX_BILL_TYPE("TaxBillType");
		
		String keyName;
		String shortKeyName;

		TaxInstallmentSetKey(String keyName) {
			this.shortKeyName = keyName;
			this.keyName = "TaxInstallmentSet." + keyName;
			
		}

		public String getKeyName() {
			return keyName;
		}
		
		public String getShortKeyName() {
			return shortKeyName;
		}
		
	}

}

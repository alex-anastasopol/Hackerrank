package ro.cst.tsearch.servers.response;

public class SpecialAssessmentSet extends InfSet {
	
	private static final long serialVersionUID = -707791753174602278L;

	public SpecialAssessmentSet() {
        super(new String[] {"Year", "InstallmentName", "TotalDue", "CurrentYearDue", "AmountPaid", 
        					"BaseAmount", "PriorDelinquent", "PenaltyAmount", "Status"});
	}		
	
	public enum  SpecialAssessmentSetKey {
		YEAR("Year"),
		TOTAL_DUE("TotalDue"),
		CURRENT_YEAR_DUE("CurrentYearDue"),
		AMOUNT_PAID("AmountPaid"),
		BASE_AMOUNT("BaseAmount"),
		INSTALLMENT_NAME("InstallmentName"),
		PENALTY_AMOUNT("PenaltyAmount"),
		STATUS("Status"),
		PRIOR_DELINQUENT("PriorDelinquent");
		
		String keyName;

		SpecialAssessmentSetKey(String keyName) {
			this.keyName = "SpecialAssessmentSet." + keyName;
		}

		public String getKeyName() {
			return keyName;
		}
	}

}

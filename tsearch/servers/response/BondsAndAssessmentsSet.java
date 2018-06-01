package ro.cst.tsearch.servers.response;

public class BondsAndAssessmentsSet extends InfSet {
	
	private static final long serialVersionUID = 4330950700808167952L;

	public BondsAndAssessmentsSet() {	// used with California DataTrace taxes
        super(new String[]{ 			
        		"BondEntityNumber",
        		"District",
        		"BondSeries",
        		"ImprovementOf",
        		"RecordedFromDate"
        		});
	}

	public enum BondsAndAssessmentsSetKey {
		BOND_ENTITY_NUMBER("BondEntityNumber"), DISTRICT("District"), BOND_SERIES("BondSeries"), IMPROVEMENT_OF("ImprovementOf"), RECORDED_FROM_DATE(
				"RecordedFromDate");

		String keyName;

		BondsAndAssessmentsSetKey(String keyName) {
			this.keyName = "BondsAndAssessmentsSet." + keyName;
		}

		public String getKeyName() {
			return keyName;
		}
	}
}

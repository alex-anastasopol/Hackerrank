package ro.cst.tsearch.servers.response;

public class PropertyAppraisalSet extends InfSet {

	private static final long serialVersionUID = -3887457093770116008L;

	public PropertyAppraisalSet(){
        super(new String[]{ 
        					"LandAppraisal", 
        					"ImprovementAppraisal", 
        					"TotalAppraisal", 		//real value
                           	"TotalAssessment", 		//estimated value ( < than appraisal)
                           }); 
	}
	
	public enum  PropertyAppraisalSetKey {		
		LAND_APPRAISAL("LandAppraisal"),
		IMPROVEMENT_APPRAISAL("ImprovementAppraisal"),
		TOTAL_APPRAISAL("TotalAppraisal"),
		TOTAL_ASSESSMENT("TotalAssessment");

		String keyName;
		String shortKeyName;

		PropertyAppraisalSetKey(String keyName) {
			this.keyName = "PropertyAppraisalSet." + keyName;
			this.shortKeyName = keyName;
		}

		public String getKeyName() {
			return keyName;
		}
		
		public String getShortKeyName() {
			return shortKeyName;
		}
	}

}

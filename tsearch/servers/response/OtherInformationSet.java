package ro.cst.tsearch.servers.response;

public class OtherInformationSet extends InfSet {

	private static final long serialVersionUID = 7009429939329022733L;

	public OtherInformationSet() {
        super(
        		new String[]{
	                "FileNumber",  		// 	used just for email order parsing
	                "Comment",			// 	used just for email order parsing
	                "AgentName", 		// 	used just for email order parsing
	                "AgentCompany", 	// 	used just for email order parsing
	                "ResearchRequired", // 	used just for ILCook TU and LA tax sites
	                "SrcType", 			// 	document SRC Type, data source
	                "Amount",			//	parsed but unused
	                "Remarks",			//	put stupid stuff here		
	                "GoBackIsDone"		//	used in GB to determined if a Transfer was analyzed or not
               }
        	);
    }
	
	public enum  OtherInformationSetKey {
		FILE_NUMBER("FileNumber"),
		COMMENT("Comment"),
		AGENT_NAME("AgentName"),
		AGENT_COMPANY("AgentCompany"),
		RESEARCH_REQUIRED("ResearchRequired"),
		SRC_TYPE("SrcType"),
		AMOUNT("Amount"),
		REMARKS("Remarks"),
		GO_BACK_IS_DONE("GoBackIsDone");
		
		String keyName;
		String shortKeyName;

		OtherInformationSetKey(String keyName) {
			this.shortKeyName = keyName;
			this.keyName = "OtherInformationSet." + keyName;
		}

		public String getKeyName() {
			return keyName;
		}
		public String getShortKeyName() {
			return shortKeyName;
		}
	}

}

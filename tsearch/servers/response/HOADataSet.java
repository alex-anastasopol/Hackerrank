package ro.cst.tsearch.servers.response;

public class HOADataSet extends InfSet {

	private static final long serialVersionUID = 9216517100435126528L;

	public HOADataSet() {
        super(
        		new String[]{
        				"AssocName",  
        				"BoardMemberName"  

                        //will be added later if needed
                     });
    }
	
	public enum  HOADataSetKey {
		ASSOC_NAME("AssocName"),
		BOARD_MEMBER_NAME("BoardMemberName");
		
		
		String keyName;
		String shortKeyName;
		
		HOADataSetKey(String keyName) {
			this.shortKeyName = keyName;
			this.keyName = "HOADataSet." + keyName;
		}
		public String getKeyName() {
			return keyName;
		}
		public String getShortKeyName() {
			return shortKeyName;
		}
	}

	
}

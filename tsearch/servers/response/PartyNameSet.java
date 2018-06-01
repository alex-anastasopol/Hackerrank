package ro.cst.tsearch.servers.response;

public class PartyNameSet extends InfSet {
	
	private static final long serialVersionUID = -1220434905756687146L;

	public PartyNameSet() {
        super(new String[]{	"FirstName", 
        					"MiddleName", 
        					"LastName",
                           	"Suffix",
                           	"Type",
                           	"OtherType",
                           	"isCompany"
                           	});
    }

	public enum  PartyNameSetKey {		
		FIRST_NAME("FirstName"),
		MIDDLE_NAME("MiddleName"),
		LAST_NAME("LastName"),
		SUFFIX("Suffix"),
		TYPE("Type"),
		OTHER_TYPE("OtherType"),
		IS_COMPANY("isCompany");
		
		String keyName;
		String shortKeyName;
		PartyNameSetKey(String keyName) {
			this.keyName = "PartyNameSet." + keyName;
			this.shortKeyName = keyName;
		}

		public String getKeyName() {
			return keyName;
		}
		
		public String getShortKeyName() {
			return shortKeyName;
		}
	}

	
	public NameSet toNameSet() {
		NameSet owner = new NameSet();
		owner.setAtribute("OwnerFirstName", getAtribute("FirstName"));
		owner.setAtribute("OwnerMiddleName", getAtribute("MiddleName"));
		owner.setAtribute("OwnerLastName", getAtribute("LastName"));
		owner.setAtribute("SpouseFirstName", "");
		owner.setAtribute("SpouseMiddleName", "");
		owner.setAtribute("SpouseLastName", "");
		return owner;
	}
	
   		
}

package ro.cst.tsearch.servers.response;

import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;

public class NameSet extends InfSet {

    private static final long serialVersionUID = 7935417813925427961L;

	public NameSet() {
        super(new String[]{"OwnerFirstName", "OwnerMiddleName", "OwnerLastName", 
                           "SpouseFirstName", "SpouseMiddleName", "SpouseLastName",
                           "BussinessID", "isLander"});
    }
    
    public PartyI toParty(PType type){
    	PartyI party = new Party(type);
    	for(String pref: new String[]{"Owner", "Spouse"}){
    		String last = getAtribute(pref + "LastName");
    		String first = getAtribute(pref + "FirstName");
    		String middle = getAtribute(pref + "MiddleName");
    		if(!StringUtils.isEmpty(last) || !StringUtils.isEmpty(first) || !StringUtils.isEmpty(middle)){
    			NameI name = new Name(first, middle, last);
    			party.add(name);
    		}
    	}
    	return party;
    }
    
    public enum  NameSetKey {
    	OWNER_FIRST_NAME("OwnerFirstName"),
    	OWNER_MIDDLE_NAME("OwnerMiddleName"),
    	OWNER_LAST_NAME("OwnerLastName"),
    	SPOUSE_FIRST_NAME("SpouseFirstName"),
    	SPOUSE_MIDDLE_NAME("SpouseMiddleName"),
    	SPOUSE_LAST_NAME("SpouseLastName"),
    	BUSSINESS_ID("BussinessID"),
    	IS_LANDER("isLander");
    	
		String keyName;

		NameSetKey(String keyName) {
			this.keyName = "NameSet." + keyName;
		}

		public String getKeyName() {
			return keyName;
		}
	}

}

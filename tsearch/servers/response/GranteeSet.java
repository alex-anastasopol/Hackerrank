package ro.cst.tsearch.servers.response;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;

public class GranteeSet extends NameSet {

	private static final long serialVersionUID = 2738555672366421188L;

	public PartyI toParty(){
    	PartyI party = super.toParty(PType.GRANTEE);
    	return party;
    }
	
}


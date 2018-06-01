package ro.cst.tsearch.servers.response;

import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;


public class GrantorSet extends NameSet {

	private static final long serialVersionUID = -8508957484700396166L;
	
	public PartyI toParty(){
    	PartyI party = super.toParty(PType.GRANTOR) ;
    	return party;
    }
	
}


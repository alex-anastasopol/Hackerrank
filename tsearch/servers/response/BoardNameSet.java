package ro.cst.tsearch.servers.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;

public class BoardNameSet extends InfSet {

    private static final long serialVersionUID = 793541781397961L;

	public BoardNameSet() {
        super(new String[]{"AssocName", "MemberName", "MemberLastName", "MemberFirstName", "MemberMiddleName", "MemberPhone"});
    }
    
    public PartyI toParty(PType type){
    	PartyI party = new Party(type);
    	for(String pref: new String[]{"Member"}){
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
    
    public static void addBoardMemberInfo(String assocName, String unparsedName, String names[], String phoneNumber, List<List<String>> body){
    	
    	List<String> line;       
		if (names[2].length() != 0){
			line = new ArrayList<String>();
			line.add(assocName);
			line.add(unparsedName);
			line.add(names[0]);
			line.add(names[1]);
			line.add(names[2]);
			line.add(phoneNumber);
			
			body.add(line);
		}
    }
    
    public static ResultTable storeMemberInSet(ArrayList<List<String>> names) throws Exception {
    	
    	ResultTable rt = new ResultTable();
    	
    	String[] header = {"AssocName", "MemberName", "MemberFirstName", "MemberMiddleName", "MemberLastName", "MemberPhone"};

    	Map<String,String[]> map = new HashMap<String,String[]>();
    	map.put("AssocName", new String[]{"AssocName", ""});
    	map.put("MemberName", new String[]{"MemberName", ""});
		map.put("MemberFirstName", new String[]{"MemberFirstName", ""});
		map.put("MemberMiddleName", new String[]{"MemberMiddleName", ""});
		map.put("MemberLastName", new String[]{"MemberLastName", ""});
 		map.put("MemberPhone", new String[]{"MemberPhone", ""});
 		
 		String[][] body = new String[names.size()][header.length];
    	for (int i = 0; i < names.size(); i++){
    		body[i][0] = (String)names.get(i).get(0);
    		body[i][1] = (String)names.get(i).get(1);
    		body[i][2] = (String)names.get(i).get(2);
    		body[i][3] = (String)names.get(i).get(3);
    		body[i][4] = (String)names.get(i).get(4);
    		body[i][5] = (String)names.get(i).get(5);
    	}
    	
    	rt.setHead(header);
    	rt.setMap(map);
    	rt.setBody(body);
    	rt.setReadOnly();
    	return rt;
    }
    
    public enum  NameSetKey {
    	ASSOC_NAME("AssocName"),
    	MEMBER_NAME("MemberName"),
    	MEMBER_PHONE("MemberPhone"),
    	MEMBER_LAST_NAME("MemberLastName"),
    	MEMBER_FIRST_NAME("MemberFirstName"),
    	MEMBER_MIDDLE_NAME("MemberMiddleName");
    	
		String keyName;

		NameSetKey(String keyName) {
			this.keyName = "NameSet." + keyName;
		}

		public String getKeyName() {
			return keyName;
		}
	}

}

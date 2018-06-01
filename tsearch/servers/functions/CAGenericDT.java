package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stewart.ats.base.name.Name;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.FirstNameEquivalents;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * B3607
 * @author Florin Cazacu
 *
 */
public class CAGenericDT {
	public static void fixGrantorGranteeSetDT(ResultMap m, long searchId) throws Exception {
		int prefixLenght = 4;
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String serverType = (String) m.get("OtherInformationSet.SrcType");

		ResultTable grantorSet = (ResultTable) m.get("GrantorSet");
		ResultTable granteeSet = (ResultTable) m.get("GranteeSet");
		List<List> bodyGrantor = new ArrayList<List>();
		List<List> bodyGrantee = new ArrayList<List>();
		
		if (grantorSet != null){//B 4649; Last: A First: SYLVIA or First: SYLVIA A 
			String body[][] = grantorSet.getBodyRef();
			if (body.length != 0){
				for(int i = 0; i < body.length; i++){
					
					if (body[i][1].length() == 1 && body[i][2].length() == 0 && i > 0){
						body[i][3] = (body[i][3] + " " + body[i][1]).trim();
						body[i][1] = body[i-1][1];
					} else if (body[i][1].length() == 0 && body[i][2].length() > 1 && body[i][3].length() == 0 && i > 0){ // B 4768
						body[i][1] = body[i-1][1];
					} else if (LastNameUtils.isNotLastName(body[i][1]) && NameUtils.isNotCompany(body[i][1]) && i > 0 && StringUtils.isEmpty(body[i][3])
							&& body[i][1].length() <= 4 && !body[i][1].equals(body[i-1][1])){// to be checked in time
						body[i][3] = body[i][1];
						body[i][1] = body[i-1][1];
					}
					
					if (body[i][1].matches(".*\\bEST\\s+OF$")){ // B 4942
						body[i][1] = body[i][1].replaceAll("(.*\\bEST)\\s+OF$", "$1");
					}					
				}
				//or First: SYLVIA A
				for(int i = 0; i < body.length; i++){
					if (body[i][2].matches("\\s*\\w+\\s+.*")){
						body[i][3] = (body[i][3] + " " + body[i][2].replaceAll("\\s*(\\w+)\\s+(.*)", "$2")).trim();
						body[i][2] = body[i][2].replaceAll("\\s*(\\w+)\\s+(.*)", "$1");
					}
				}
				//B 4706
				for(int i = 0; i < body.length; i++){
					if (body[i][2].length() > 0 && body[i][3].length() == 0){
						if (FirstNameEquivalents.needEquivalent(body[i][2])){
							Name newName = FirstNameEquivalents.getEquivalent(body[i][2]);
							if (!newName.isEmpty()){
								body[i][2] = newName.getFirstName();
								body[i][3] = newName.getMiddleName();
							}
						}
					}
				}
				
				grantorSet.setReadOnly(false);
				grantorSet.setBody(body);
				grantorSet.setReadOnly();
			}
		}
		if (granteeSet != null){//B 4649
			String body[][] = granteeSet.getBodyRef();
			if (body.length != 0){
				for(int i = 0; i < body.length; i++){
								
					if (body[i][1].length() == 1 && body[i][2].length() == 0 && i > 0){
						body[i][3] = (body[i][3] + " " + body[i][1]).trim();
						body[i][1] = body[i-1][1];
					} else if (body[i][1].length() == 0 && body[i][2].length() > 1 && body[i][3].length() == 0 && i > 0){ // B 4768
						body[i][1] = body[i-1][1];
					} else if (LastNameUtils.isNotLastName(body[i][1]) && NameUtils.isNotCompany(body[i][1]) && i > 0 && StringUtils.isEmpty(body[i][3])
							&& body[i][1].length() <= 4 && !body[i][1].equals(body[i-1][1])){// to be checked in time
						body[i][3] = body[i][1];
						body[i][1] = body[i-1][1];
					}
					
					if (body[i][1].matches(".*\\bEST\\s+OF$")){ // B 4942
						body[i][1] = body[i][1].replaceAll("(.*\\bEST)\\s+OF$", "$1");
					}
				}
				//or First: SYLVIA A
				for(int i = 0; i < body.length; i++){
					if (body[i][2].matches("\\s*\\w+\\s+.*")){
						body[i][3] = (body[i][3] + " " + body[i][2].replaceAll("\\s*(\\w+)\\s+(.*)", "$2")).trim();
						body[i][2] = body[i][2].replaceAll("\\s*(\\w+)\\s+(.*)", "$1");
					}
				}
				
				//B 4706
				for(int i = 0; i < body.length; i++){
					if (body[i][2].length() > 0 && body[i][3].length() == 0){
						if (FirstNameEquivalents.needEquivalent(body[i][2])){
							Name newName = FirstNameEquivalents.getEquivalent(body[i][2]);
							if (!newName.isEmpty()){
								body[i][2] = newName.getFirstName();
								body[i][3] = newName.getMiddleName();
							}
						}
					}
				}
				
				granteeSet.setReadOnly(false);
				granteeSet.setBody(body);
				granteeSet.setReadOnly();
			}
		}
		
		if ("DT".equals(serverType)){
			if (grantorSet != null) {
				String body[][] = grantorSet.getBodyRef();
	
				int len = body.length;
				if (len != 0) {
					for (int i = 0; i < len; i++) {
						if ("San Mateo".equals(crtCounty))
						{
							String s = body[i][0];
							String[] names = {"","","","","",""};
							if (s.matches("(?is).*(TRUSTE?| TI|TITLE)"))
							{
								m.put("GrantorSet.OwnerLastName", s);
								m.put("GrantorSet.OwnerFirstName", "");
								m.put("GrantorSet.OwnerMiddleName", "");
								grantorSet = (ResultTable) m.get("GrantorSet");
							}
							else
							{
								if (s.indexOf(' ')!= -1)
								{
								String initials = s.substring(0, s.indexOf(' '));
								if (initials.matches("[A-Z]{2}"))
								{
									if ((!"JR".equals(initials)) || (!"SR".equals(initials)))
									{
										initials = initials.substring(0,1) + " " + initials.substring(1);
										s = s.replaceFirst("\\A[A-Z]{2} ", initials + " ");
									}
								}
								}
								names = StringFormats.parseNameDesotoRO(s);
								names = fixNamesWithNoSpaces(names);
								
								m.put("GrantorSet.OwnerFirstName", names[0]);
								m.put("GrantorSet.OwnerMiddleName", names[1]);
								m.put("GrantorSet.OwnerLastName", names[2]);
								grantorSet = (ResultTable) m.get("GrantorSet");
							}
						}
						String curLine = grantorSet.getItem("all", "", i);
						if (curLine.equals("-")){
							return;
						}
						boolean isGrantee = curLine.matches(".*\\bTEE\\b.*");
						String curLineNew = CADTClean(curLine);
						String names[] = { "", "", "", "", "", "" };
						if (curLineNew.contains("&")) {
							if (NameUtils.isCompany(curLine)){
								names[2] = curLine.replaceAll("^(TOR|TEE)\\s+", "");
							} else {
								names = StringFormats.parseNameNashville(curLineNew);
							}
						} else if (curLine.equals(curLineNew) || curLine.equals("-")) {
							names[0] = grantorSet.getItem("OwnerFirstName", "", i);
							names[1] = grantorSet.getItem("OwnerMiddleName", "", i);
							names[2] = grantorSet.getItem("OwnerLastName", "", i);
						} else if (curLineNew.length() > 0) {
							names = StringFormats.parseNameWilliamson(curLineNew);
						}
						if (curLineNew.length() > 0 || curLine.equals("-")) {
							if (!isGrantee) {
								addOwnerNamesGS(names, bodyGrantor, curLine);
							} else {
								addOwnerNamesGS(names, bodyGrantee, curLine);
							}
						}
					}
				}
			}
	
			if (granteeSet != null) {
				String body[][] = granteeSet.getBodyRef();
				int len = body.length;
				if (len != 0) {
					for (int i = 0; i < len; i++) {
						if ("San Mateo".equals(crtCounty))
						{
							String s = body[i][0];
							String[] names = {"","","","","",""};
							if (s.matches("(?is).*(TRUSTE?| TI|TITLE)")){
								m.put("GrantorSet.OwnerFirstName", "");
								m.put("GrantorSet.OwnerMiddleName", "");
								m.put("GranteeSet.OwnerLastName", s);
								granteeSet = (ResultTable) m.get("GranteeSet");
							}
							else
							{
								if (s.indexOf(' ')!= -1)
								{
								String initials = s.substring(0, s.indexOf(' '));
								if (initials.matches("[A-Z]{2}"))
								{
									if ((!"JR".equals(initials)) || (!"SR".equals(initials)))
									{
										initials = initials.substring(0,1) + " " + initials.substring(1);
										s = s.replaceFirst("\\A[A-Z]{2} ", initials + " ");
									}
								}
								}
								names = StringFormats.parseNameDesotoRO(s);
								names = fixNamesWithNoSpaces(names);
								m.put("GranteeSet.OwnerFirstName", names[0]);
								m.put("GranteeSet.OwnerMiddleName", names[1]);
								m.put("GranteeSet.OwnerLastName", names[2]);
								granteeSet = (ResultTable) m.get("GranteeSet");
							}
						}
						String curLine = granteeSet.getItem("all", "", i);
						if (curLine.equals("-")){
							return;
						}
						boolean isGrantor = curLine.matches(".*\\bTOR\\b.*");
						String curLineNew = CADTClean(curLine);
	
						String names[] = { "", "", "", "", "", "" };
						if (curLineNew.contains("&")) {
							if (NameUtils.isCompany(curLine)){
								names[2] = curLine.replaceAll("^(TOR|TEE)\\s+", "");
							} else {						
								names = StringFormats.parseNameNashville(curLineNew);
							}
						} else if (curLine.equals(curLineNew) || curLine.equals("-")) {
							names[0] = granteeSet.getItem("OwnerFirstName", "", i);
							names[1] = granteeSet.getItem("OwnerMiddleName", "", i);
							names[2] = granteeSet.getItem("OwnerLastName", "", i);
						} else if (curLineNew.length() > 0) {
							names = StringFormats.parseNameWilliamson(curLineNew);
						}
						if (curLineNew.length() > 0  || curLine.equals("-")) {
							if (!isGrantor) {
								addOwnerNamesGS(names, bodyGrantee, curLine);
							} else {
								addOwnerNamesGS(names, bodyGrantor, curLine);
							}
						}
					}
				}
			}
			
			//eliminate the names that are last names only and prefixes of another name from the set.
			// the name needs to have min prefixLength characters.
			bodyGrantee = removePrefixes(bodyGrantee, prefixLenght);
			bodyGrantor = removePrefixes(bodyGrantor, prefixLenght);
	
			addToSet(m, "GrantorSet", bodyGrantor);
			addToSet(m, "GranteeSet", bodyGrantee);
		}
		
	}

	public static List<List> removePrefixes(List<List> body, int prefixLenght) {
		List<List> body2 = new ArrayList<List>();
		if (body.size() > 1) {
			for (int i = 0; i < body.size(); i++) {
				List<String> line = body.get(i);
				if (line.get(1).length() > 0 || line.get(2).length() > 0) {
					body2.add(line);
				} else {
					boolean add = true;
					if (line.get(3).length() >= prefixLenght) {
						for (int j = 0; j < body.size() && add; j++) {
							if (i == j) {
								continue;
							}
							List<String> line2 = body.get(j);
							if (line2.get(3).startsWith(line.get(3))) {
								add = false;
							}
						}
					}
					if (add) {
						body2.add(line);
					}
				}
			}
		} else {
			body2 = body;
		}
		return body2;

	}

	public static void addToSet(ResultMap m, String key, List body) {
		if (body.size() > 0) {
			String[] header = { "all", "OwnerLastName", "OwnerFirstName",
					"OwnerMiddleName" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("OwnerFirstName", new String[] { "OwnerFirstName", "" });
			map.put("OwnerMiddleName", new String[] { "OwnerMiddleName", "" });
			map.put("OwnerLastName", new String[] { "OwnerLastName", "" });
			ResultTable res = new ResultTable();
			try {
				res.setHead(header);
				res.setBody(body);
				res.setMap(map);
				m.put(key, res);
			} catch (Exception e) {
				e.printStackTrace();
				// TODO: handle exception
			}
		} else {
			try {
				m.remove(key);
			} catch (Exception e) {
				e.printStackTrace();
				// TODO: handle exception
			}
		}

	}

	public static void addOwnerNamesGS(String names[], List<List> body,
			String all) {

		List<String> line;
		if (names[2].length() != 0) {
			line = new ArrayList<String>();
			line.add(all);
			line.add(names[2]);
			line.add(names[0]);
			line.add(names[1]);
			body.add(line);
		}
		if (names[5].length() != 0) {
			line = new ArrayList<String>();
			line.add(all);
			line.add(names[5]);
			line.add(names[3]);
			line.add(names[4]);
			body.add(line);
		}
	}

	public static String CADTClean(String s) {
		s = s.replaceAll(">AC\\b", "");
		s = s.replaceAll(">HWCP\\b", "");
		s = s.replaceAll(">.*", "");
		s = s.replaceAll("TRUSTEE?", "");
		s = s.replaceAll("^\\d( |\\-)([A-Z]+)?\\d{3,}.*", "");
		//s = s.replaceAll("[A-Z]?\\d{2,}.*", ""); B 4607
		s = s.replaceAll("TOO MANY NAMES.*", "");
		s = s.replaceAll("\\b(TOR|TEE)\\b", "");
		s = s.replaceAll("\\bAS (COMMUNITY|SEPARATE) PROPERTY\\b", "");
		s = s.replaceAll("^CORPORATION$", "");
		s = s.replaceAll("\\bMARRIED WOMAN\\b", "");
		s = s.replaceAll("\\s{2,}", " ");
		if (s.matches("[\\d\\-]+")){
			s = "";
		}
		
		return s.trim();
	}
	
	/**
	 * B4037
	 * @param names
	 * @return
	 */
	public static String[] fixNamesWithNoSpaces(String[] names){
		if (names.length >= 3 &&
			(names[0]+names[1]).length() == 0 
			&& !NameFactory.getInstance().isLast(names[2])
			&& names[2].length() > 2
			&& NameFactory.getInstance().isLast(names[2].substring(2)))
		{
				names[0] = names[2].substring(0, 1); 
				names[1] = names[2].substring(1, 2);
				names[2] = names[2].substring(2);
		}
		return names;
	}
}

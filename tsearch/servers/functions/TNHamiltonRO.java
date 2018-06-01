package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.StringUtils;

public class TNHamiltonRO {
	
	@SuppressWarnings("rawtypes")
	public static void parseName(ResultMap m, long searchId) throws Exception {
		
		String grantor = (String) m.get("SaleDataSet.Grantor");
		String grantee = (String) m.get("SaleDataSet.Grantee");
	
		if(StringUtils.isEmpty(grantee) && StringUtils.isNotEmpty((String) m.get("SaleDataSet.GranteeLander"))) {
			grantee = (String) m.get("SaleDataSet.GranteeLander");
		}
		grantor = StringUtils.prepareStringForHTML(grantor);
		grantee = StringUtils.prepareStringForHTML(grantee);
		grantor = NameCleaner.cleanFreeformName(grantor);
		grantee = NameCleaner.cleanFreeformName(grantee);
		
		ArrayList<List> grantorList = new ArrayList<List>();
		ArrayList<List> granteeList = new ArrayList<List>();
		
		parseNameInner(m, grantor, grantorList, searchId, false);
		parseNameInner(m, grantee, granteeList, searchId, true);
				
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantorList, true));
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(granteeList, true));
		
		GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		
		GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNameInner(ResultMap m, String name, ArrayList<List> namesList, long searchId, boolean isGrantee) {
		
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;
				
		name = name.replaceAll("(?is)\\b[FAN]\\s*/\\s*K\\s*/\\s*A\\b", "\n");
		name = name.replaceAll("(?is)\\bADM\\b", "");//means ADMINISTRATOR
		name = name.replaceAll("(?is)\\bSU[CB]\\s+(TRUSTEE)\\b", " $1");
		
		String[] nameItems = name.split("\\s*/\\s*");
		for (int i = 0; i < nameItems.length; i++){
			nameItems[i] = nameItems[i].replaceAll("(?is)\\s+(TR(?:USTEE)?S?)(,.*)", "$2 $1");
			names = StringFormats.parseNameNashville(nameItems[i], true);
						
			type = GenericFunctions.extractNameType(names);
			otherType = GenericFunctions.extractNameOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(nameItems[i], names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), namesList);
				
		}
	}
	
}

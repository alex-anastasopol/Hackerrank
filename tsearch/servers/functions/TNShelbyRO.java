package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.StringUtils;

public class TNShelbyRO {
	
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
		
		String instrNo = (String) m.get("SaleDataSet.InstrumentNumber");

		name = NameCleaner.cleanNameAndFix(name, new Vector<String>(), true);
		if (!name.matches(".*\\bAND\\s+CO(MPANY)?.*")){
			name = name.replaceAll("\\bAND\\b", ",");
			name = name.replaceAll("\\s{2,}", ", ");
		}
		
		String[] nameItems = name.split("\n");
		for (int i = 0; i < nameItems.length; i++){
			String[] nameLinesItems = nameItems[i].split(",");
			if (nameLinesItems.length == 1){
				names = StringFormats.parseNameNashville(nameLinesItems[0], true);
				names = NameCleaner.removeUnderscore(names);
				name = NameCleaner.removeUnderscore(name);
				
				suffixes = GenericFunctions.extractNameSuffixes(names);
				type = GenericFunctions.extractNameType(names);
				otherType = GenericFunctions.extractNameOtherType(names);
				GenericFunctions.addOwnerNames(nameItems[i], names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), namesList);
			} else {
				for (int j = 0; j < nameLinesItems.length; j++){
					if (isGrantee){
						if (j == 0){
							names = StringFormats.parseNameNashville(nameLinesItems[j], true);
						} else {
							if (StringUtils.isNotEmpty(instrNo) && org.apache.commons.lang.StringUtils.isAlpha(instrNo.substring(0, 1))){
								names = StringFormats.parseNameDesotoRO(nameLinesItems[j], true);
							} else {
								names = StringFormats.parseNameNashville(nameLinesItems[j], true);
							}
						}
					} else {
						names = StringFormats.parseNameNashville(nameLinesItems[j], true);
					}
					names = NameCleaner.removeUnderscore(names);
					name = NameCleaner.removeUnderscore(name);
					
					suffixes = GenericFunctions.extractNameSuffixes(names);
					type = GenericFunctions.extractNameType(names);
					otherType = GenericFunctions.extractNameOtherType(names);
					GenericFunctions.addOwnerNames(nameLinesItems[j], names, suffixes[0], suffixes[1], type, otherType, 
													NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), namesList);
				}
			}
		}

	}
	
}

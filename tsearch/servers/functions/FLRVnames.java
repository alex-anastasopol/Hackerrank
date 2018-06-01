package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.StringUtils;

public class FLRVnames {
	
	public static void nameParseFLRV(ResultMap m, long searchId) throws Exception{
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		@SuppressWarnings("rawtypes")
		ArrayList<List> grantor = new ArrayList<List>();
		@SuppressWarnings("rawtypes")
		ArrayList<List> grantee = new ArrayList<List>();
		
		String grantorString = (String) m.get("SaleDataSet.Grantor");
		
		if (StringUtils.isNotEmpty(grantorString)){
		
			String[] gtors = grantorString.split("/");
			
			for (int i = 0; i < gtors.length; i++){
				gtors[i] = gtors[i].trim();;
				gtors[i] = FLCleanRV(gtors[i]);
				
				gtors[i] = StringFormats.cleanNameMERS(gtors[i]);
				
				if (gtors[i].contains("&")) {
					if (NameUtils.isCompany(gtors[i])) {
						names[2] = gtors[i];
					} else {
						names = StringFormats.parseNameNashville(gtors[i]);
					}
				} else {
					names = StringFormats.parseNameNashville(gtors[i], true);
				}
				
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				
				GenericFunctions.addOwnerNames(gtors[i], names, suffixes[0],
												suffixes[1], type, otherType,
												NameUtils.isCompany(names[2]),
												NameUtils.isCompany(names[5]), grantor);
			}
			m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
		}
		
		String granteeString = (String) m.get("SaleDataSet.Grantee");
		if (StringUtils.isNotEmpty(granteeString)){
			String[] gtee = granteeString.split("/");
			
			for (int i = 0; i < gtee.length; i++){
				gtee[i] = gtee[i].trim();
				gtee[i] = FLCleanRV(gtee[i]);
				gtee[i]=StringFormats.cleanNameMERS(gtee[i]);
				
				if (gtee[i].contains("&")) {
					if (NameUtils.isCompany(gtee[i])) {
						names[2] = gtee[i];
					} else {
						names = StringFormats.parseNameNashville(gtee[i]);
					}
				} else {
					names = StringFormats.parseNameNashville(gtee[i], true);
				}
				
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				
				GenericFunctions.addOwnerNames(gtee[i], names, suffixes[0],
						suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), grantee);
			}
			m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		}
	}
	
	public static String FLCleanRV(String s) {
			
		s = s.replaceAll("\\b(APPLICANT|DEC(EASED)?|FKA|AKA|NKA|DECSD|MBA|IND|IEF|AD INS|MRS)\\b", "");
		s = s.replaceAll("\\b(& |CO )(TRUSTEE)\\b", " $2");
		s = s.replaceAll("\\bGDN(SHIP)?\\b", "");
		s = s.replaceAll("\\bPET\\b", "");
		s = s.replaceAll("\\b(CO )?PER REP\\b", "");
		s = s.replaceAll("\\b(CO )?PER\\b", "");
		s = s.replaceAll("A( |R)RESTATED", "");
		s = s.replaceAll("\\.", " ");
		s = s.replaceAll("[\\{\\}]+", " ");
		s = s.replaceAll("\\b(NA|STF)\\b", "");
		s = s.replaceAll("\\bINRE:.*", "");
		s = s.replaceAll("\\s{2,}", " ");
		
		return s.trim();
	}

}

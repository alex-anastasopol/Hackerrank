package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class FLDeSotoTR {

	    public static void partyNamesFLDeSotoTR(ResultMap m, long searchId)
				throws Exception {
	    	
			int i;
			String owner = (String) m.get("tmpOwnerInfo");
			if (StringUtils.isEmpty(owner)) {
				return;
			}
			owner = owner.replaceAll("-CONT-", "");
		    Vector<String> excludeCompany = new Vector<String>();
		    excludeCompany.add("PAT");
		    excludeCompany.add("ST");
		    
		    Vector<String> extraCompany = new Vector<String>();
		    extraCompany.add("BUZBY");
		    
			List<List> body = new ArrayList<List>();
			owner = owner.replaceAll("\\s{2,}", " ");
			owner = owner.toUpperCase();
			String[] lines = owner.split("@@");
			if (lines[0].contains("1ST INTEGRITY INVESTMENTS LLC")){
				lines = lines;
			}
			if (lines.length <= 2){
				parseNameInterFLDeSotoTR(m, searchId);
				return;
			}

			// clean address out of mailing address
			String[] lines2 = GenericFunctions.removeAddressFLTR(lines,
					excludeCompany, extraCompany, 2, 30);
			
			//lines2 = NameCleaner.splitName(lines2, excludeCompany);
			for (i = 0; i < lines2.length; i++) {
				String[] names = { "", "", "", "", "", "" };
				boolean isCompany = false;
				// 1 = "L, FM & WFWM"
				// 2 = "F M L"
				// 3 = "F & WF L
				int nameFormat = 1;
				
				if(lines2[i].contains("& CHILDREN")){
					lines2[i] = lines2[i].replace("& CHILDREN", "").trim();
				}
				
				// C/O - I want it before clean because I don't clean company names
				if (lines2[i].matches("(?i).*?c/o.*")
						||lines2[i].matches("^\\s*%.*")) {
					lines2[i] = lines2[i].replaceAll("(?i)c/o\\s*", "");
					lines2[i] = lines2[i].replaceFirst("^\\s*%\\s*", "");
					nameFormat = 2;
				}
				lines2[i] = lines2[i].replaceFirst("\\s*&\\s*$", "");
				lines2[i] = lines2[i].replaceAll("\\(HUSBAND\\)", "");
				lines2[i] = NameCleaner.paranthesisFix(lines2[i]);

				String curLine = NameCleaner.cleanNameAndFix(lines2[i],
						new Vector<String>(), true);
				//curLine = cleanOwnerNameFLBayTR(curLine);
				
				if (NameUtils.isCompany(curLine, excludeCompany, true)) {
					// this is not a typo, we don't know what to clean in companies'
					// names
					names[2] = lines2[i].replaceAll("^AGENT FOR\\s+", "");
					isCompany = true;
				} else {
					Vector<Integer> commasIndexes = StringUtils.indexesOf(curLine, ',');
					Vector<Integer> ampIndexes = StringUtils.indexesOf(curLine, '&');
					//remove suffix
					String curLine2 = GenericFunctions.nameSuffix3.matcher(curLine).replaceAll("");
					if (ampIndexes.size() == 1){
						if (GenericFunctions.FWFL1.matcher(curLine2).matches()
							 ||GenericFunctions.FWFL2.matcher(curLine2).matches()){
								nameFormat = 3;
							} else if (GenericFunctions.FML.matcher(curLine2).matches()
										|| GenericFunctions.FMML.matcher(curLine2).matches()){
								//MELANIE J THOMPSON
								nameFormat = 2;
							}
					}					
					
					switch (nameFormat) {
					case 1:
						names = StringFormats.parseNameLFMWFWM(curLine,
								new Vector<String>(), true, true);
						break;
					case 2:
						names = StringFormats.parseNameDesotoRO(curLine, true);
						break;
					case 3:
						names = StringFormats.parseNameNashville(curLine, true);
						break;
					}
				}

				String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
				if (!isCompany) {
					names = NameCleaner.tokenNameAdjustment(names);
					if (nameFormat != 1 
						|| !names[2].equals(names[5]) && names[2].length() > 0 && names[5].length() > 0){
						names = NameCleaner.lastNameSwap(names);
						
					}
					names = NameCleaner.removeUnderscore(names);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractAllNamesSufixes(names);
					
				}
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
			/*
			 * done multiowner parsing
			 */
		}
	    
	    public static void parseNameInterFLDeSotoTR(ResultMap m, long searchId) throws Exception {

	    	String owner = (String) m.get("tmpOwnerInfo");
	    	
	    	if (StringUtils.isEmpty(owner))
	    		return;
	    	List<List> body = new ArrayList<List>();
	    	String nameOnServer = "";
	    	String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
	    	String[] lines = owner.split("@@");
	    	for (String line : lines) {
	    		String[] a = StringFormats.parseNameNashville(owner, true);
			
	    		types = GenericFunctions.extractAllNamesType(a);
			    otherTypes = GenericFunctions.extractAllNamesOtherType(a);
			    suffixes = GenericFunctions.extractAllNamesSufixes(a);
			    
			    GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], types, otherTypes, 
			    		NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
			    nameOnServer += " & " + line;
	    	}
			
	    	GenericFunctions.storeOwnerInPartyNames(m, body, true);
	    	
	    	nameOnServer = nameOnServer.replaceAll("(?is)\\A\\s*&\\s*", "");
	    	String[] a = StringFormats.parseNameNashville(nameOnServer, true);
		    
	    	m.put(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName(), a[0]);
		    m.put(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName(), a[1]);
		    m.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), a[2]);
		    m.put(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName(), a[3]);
		    m.put(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName(), a[4]);
		    m.put(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName(), a[5]);
		    
		    m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer);
		    
		    
		    
		    
			
		    

		}
}

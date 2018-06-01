package ro.cst.tsearch.servers.functions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address.Normalize;

public class MOJacksonEP {
    public static void parcelIDJacksonAO(ResultMap m,long searchId)throws Exception{
        
        String pid = (String) m.get("PropertyIdentificationSet.ParcelID");
        String s0= pid.substring(0,2);
        String pid2 = "";
        if (s0.equals("JA")){
            String s1= pid.substring(2,4);
            String s2= pid.substring(4,7);
            String s3= pid.substring(7,9);
            String s4= pid.substring(9,11);
            String s5= pid.substring(11,13);
            char s6= pid.charAt(13);
            String s7= pid.substring(14,16);
            String s8= pid.substring(16,19);
         
            pid2 = s1+"-"+s2+"-"+s3+"-"+s4+"-"+s5+"-"+s6+"-"+s7+"-"+s8;
            m.put("PropertyIdentificationSet.ParcelID",pid2);
        }
             
    }
    
    public static void legalMOJacksonAO (ResultMap m,long searchId)throws Exception{
    	
        String tmp = (String) m.get("PropertyIdentificationSet.Subdivision");
        if (tmp == null) {
        	tmp = "";
        }
		//cleaning
        tmp = tmp.replaceAll("\\s{2,}", " ");        
		tmp = tmp.replaceAll("\\bFTLOT\\b", "FT LOT"); 
		tmp = tmp.replaceAll("/LOT\\b", "/ LOT"); 					//Jackson CiT PID JA32430141800000000
		tmp = tmp.replaceAll("(\\bBLK \\d+)([A-Z]{2,})", "$1 $2");  //Jackson CiT PID JA32430141800000000
                
//      Lot Number
		String lot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
		if (lot == null) {
			lot = "";
		}
		lot = lot.replaceAll("\\s{2,}", " ");
		// if no lot info was parsed from legal descr table, try to parse lot from legal descr field 
		if (lot.length() == 0) {
			lot = LegalDescription.extractLotFromText(tmp, false, true); // false because the letters must remain
			//lot = LegalDescription.extractLotFromText(tmp);
			
		// if lot info was parsed from legal descr table, normalize lot 	
		} else {
			// if lot string contains multiple lots, prefix the string with LOTS else prefix with LOT
			if(lot.matches("\\d+\\b.*\\d+\\b.*")) {
				lot = "LTS " + lot;
			} else {
				lot = "LT " + lot;
			}
			lot  = LegalDescription.extractLotFromText(lot);
		}
		
		m.put("PropertyIdentificationSet.SubdivisionLotNumber",lot);
		
		// block parser
		String block = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
		String block2 = tmp.replaceAll("(?is).*\\bBL(?:OC)?K\\s*(\\d+(\\s*&\\s*\\d+)*|[A-Z])\\b.*", "$1");
		if (!block2.equals(tmp)){
			block = (block != null)? block + " " + block2 : block2;
			block = block.trim().replaceAll("\\s*&\\s*", " "); // fix for bug #1178, APN=JA30240021400000000 								
			block = StringFormats.RemoveDuplicateValues(block);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		
		// Subdivision name
		String subdiv = (String) m.get("PropertyIdentificationSet.SubdivisionName");
		if (subdiv == null) {
			subdiv = "";
		}
		subdiv = subdiv.replaceAll("\\s{2,}", " ");
		
		// extract subdivision name from legal descr table if it exists
		if (subdiv.length()>0) {
			// search subdiv for " - " string; the number of appearaces must be odd; 
			// subdiv string is expected to be like "<sname1> - <sname2>" where sname1 is similar (not necessery equal) to sname2
			String s[] = subdiv.split(" - ");
			subdiv = "";			
			int cnt = s.length - 1;
			int i = cnt/2;
			int j = cnt%2;
			if (j==1) {
				// subdivision will be the last half of the subdiv string
				for (int k=i+1;k<s.length;  k++) {
					subdiv = subdiv+s[k];
				}	
				
				//cleaning
				if (subdiv.endsWith("STREET")) subdiv = ""; //3031 BELL ST - in this case it's better to parse subdivision name from legal descr
				if (subdiv.endsWith("& CO")) subdiv = "";  // 825 INDEPENDENCE - idem as prev
				if (subdiv.indexOf("UNIT")>0) subdiv = ""; // WESTRIDGE CONDOMINIUMS subdivision - idem as prev 
				if (subdiv.startsWith("PLAT OF ")) subdiv = subdiv.replaceFirst("PLAT OF ", ""); //1724 VINE ST	 
			}	
		}
		
		// extract subdivision name from legal description field		
		if (subdiv.length() == 0 && tmp.length()>0) {
			
			//cleanup
			tmp = tmp.replaceAll("(.+) ADDITION", "$1");
			tmp = tmp.replaceAll("(\\d)OF\\b", "$1 OF"); //APN=JA29140021300000000
			tmp = tmp.replaceAll("(\\d)FT\\b", "$1 FT"); //APN=JA29140021300000000
			tmp = tmp.replaceAll("\\bOFOF\\b", "OF"); //APN=JA29140021300000000
			tmp = tmp.replaceAll("\\bRES(URVEY)?( OF)?\\b", ""); // APN=JA12740020900000000
			tmp = tmp.replaceAll(
    				"(\\d+ST|\\d+ND|\\d+RD|\\d+TH|FIRST|SECOND|THIRD|FOURTH|FIFTH|SIXTH|SEVENTH|\\WEIGHTH?|NINTH|" +
    				"TENTH|TWELFTH|TWENTIETH|EIGHTY-FIFTH|FIFTY-SEVENTH|FIFTY-NINTH) (REPLATS?|REPL?|PLA?TS?|PL|ADD(ITION)?)", ""); //APN=JA48630035000000000, JA29140021900000000 
			
			// class "E" legal descr: SEC nr-nr-nr [cardinal point]nr/nr subdivision [stopper (..) | end of string]
			Pattern p = Pattern.compile("(?is)\\bSEC (\\d+)(?:\\-|\\s(?:TWP\\s)?)(\\d+)(?:\\-|\\s(?:RNG\\s)?)(\\d+)\\s?"); // fix for bug #2479,  APN:JA65820092300000000
			Matcher mat = p.matcher(tmp);
			if (mat.find()){
				m.put("PropertyIdentificationSet.SubdivisionSection", mat.group(1));
				m.put("PropertyIdentificationSet.SubdivisionTownship", mat.group(2));
				m.put("PropertyIdentificationSet.SubdivisionRange", mat.group(3));
				tmp = tmp.replace(mat.group(0), "");
				tmp = tmp.replaceAll("\\b[SEWN]{1,2}\\s*\\d+[/\\.]\\d+\\s?&?\\s?", "");
				tmp = tmp.replaceAll("\\b[WENS] OF RR\\b(\\s?&)?", ""); //APN=CL0960100010180001
				
			} else {			
				// class D legal descr: /? subdivision [stopper (..) | end of string]
				if (tmp.matches("^\\s*/\\s*.+")) {
					tmp = tmp.replaceFirst("^\\s*/\\s*", "");
					
				} else {			
					// class A legal descr: [address string] subdivision stopper (..) 
					String streetNo = (String) m.get("PropertyIdentificationSet.StreetNo");
					String streetName = (String) m.get("tmpStreetName");
					String streetSuffix = (String) m.get("tmpStreetType");
					if (streetNo == null){
						streetNo = "";
					} else {
						streetNo = streetNo.trim();
					}
					if (streetName == null){
						streetName = "";
					} else {
						streetName = streetName.trim();
					}
					if (streetSuffix == null){
						streetSuffix = "";
					} else {
						streetSuffix = streetSuffix.trim();
					}
		
					// search if property address is at the beginning of legal description string (street suffix may miss from legal) 
					if (!streetName.equals("NO ADDRESS") && streetName.length()>0) {				
						String legalNorm = Normalize.normalizeString(tmp);
						String suffixNorm = Normalize.normalizeString(streetSuffix);
						String streetNorm = Normalize.normalizeString(streetName);
						Boolean noStreetNo = (streetNo.length() == 0) || (streetNo.equals("0"));
						legalNorm = legalNorm.replaceAll("-", " ");
						streetNorm = streetNorm.replaceAll("-", " "); // needed when address contains "-" and legal doesn't or viceversa - ex. bug #657 SNI-A-BAR
						String addr = "";
						int indexName = legalNorm.length();
						int indexNo = indexName;
						if (noStreetNo){
							addr = streetNorm+" "+suffixNorm;
						} else {
							addr = streetNo+" "+streetNorm+" "+suffixNorm;
						}
						Boolean start = legalNorm.startsWith(addr); 
						if(!start) {
							if (!noStreetNo){
								addr = streetNorm+" "+suffixNorm;						
								indexName = legalNorm.indexOf(addr);
								indexNo = legalNorm.indexOf(streetNo);
								start = (indexName > -1) && (indexNo < indexName) && (indexNo > -1);
							}
							//search without street prefix
							if(!start) {
								if (noStreetNo){
									addr = streetNorm;
								} else {
									addr = streetNo+" "+streetNorm;
								}
								start = legalNorm.startsWith(addr);
								if (!start) {
									if (!noStreetNo) {
										addr = streetNorm;
										indexName = legalNorm.indexOf(addr);
										indexNo = legalNorm.indexOf(streetNo);
										start = (indexName > -1) && (indexNo < indexName) && (indexNo > -1);								
									}
								}
							}
						}
						if(start){
							//count how many address tokens have been identified at the beginning of normalized legal descr    
							int cnt = legalNorm.substring(0, legalNorm.indexOf(addr)+addr.length()).split("\\s").length-1;
							// remove the same number of tokens from the beginning of original legal description string
							indexName = -1;
							for (int i=0; i<cnt; i++){
								indexName = tmp.indexOf(" ", indexName+1);  
							}
							tmp = tmp.substring(indexName+1);
							tmp = tmp.replaceFirst("^[^/]+?\\b", "");
						}
					}

					tmp = tmp.replaceAll("\\bVAC\\b", "");					
					tmp = tmp.trim();
										
					if(tmp.startsWith("/") || tmp.startsWith("LOT")) {
						//class C legal descr: [address string] / (..) lot [lot value] block [block value] subdivision	 [stopper (..) | end of string]
						if(tmp.matches(".*BL(?:OC)?K.*")){
							Matcher ma = Pattern.compile("\\bLOT.+\\bBL(?:OC)?K\\s*(\\d+(\\s*&\\s*\\d+)*|[A-Z])\\b").matcher(tmp+"\\b.+");
							if(ma.find()){
								tmp = tmp.substring(ma.end());
							}
						
						// class B legal descr: [address string] / lot [lot value] subdivision  [stopper (..) | end of string]
						} else if(tmp.matches(".*\\bLOTS?\\b.+")){
							// remove exception notes 
							tmp = tmp.replaceAll("(\\bEXC?\\s*)?(\\bTHE\\s)?\\b[WENS]\\s\\d+( \\d+)?([\\./]\\d+)?'?\\s(FT OF|FT|OF)(\\sRR)?\\b(\\s?&)?", "");
							tmp = tmp.replaceAll("\\s{2,}", " ");
							tmp = tmp.replaceAll("\\bEXC?\\sPR?TS?\\s(TAKEN\\s)?(IN|FOR|FROM)\\s\\w+", "");
							String lotPatt = "LOTS?\\s\\d+(?:(?:\\s*(?:-|&|TO|TH(?:RU)?)\\s*|\\s)\\d+)*";
							// if nothing after lot, extract subdivision as the string before lot //e.g APN=JA28340071000000000 on JacksonEP (bug #2266)
							tmp = tmp.replaceFirst("\\s*/\\s*(.+) "+lotPatt+"\\s*$", "$1");
							// if multiple "LOT value" substrings, remove the last one
							tmp = tmp.replaceFirst("(?is).*(\\bLOTS?\\b.+?)(\\bALL)?\\s*(\\bOF)?\\s+"+lotPatt, "$1");
							// extract subdivision after "LOT value"
							tmp = tmp.replaceAll("(?is).*\\b"+lotPatt+"(.+)", "$1");
															
							// extract subdivision after LOT
							tmp = tmp.replaceAll("(?is).*\\bLOTS?\\s(\\D+)", "$1");
						}
					}
				}
			}
			
			// extract subdivision name until token
			String stoppers[] = {
				"\\bLOT\\b", "\\bLOTS\\b", "\\bPRT\\b", "\\bTH\\b", "\\bALL\\b", "\\bBLK\\b", "\\bBLOCK\\b", "\\bTR\\b", "\\bTRACT\\b", "\\bSUB\\b",
				"\\bUNIT\\b",
				"\\bTHE \\d+\\b",
				"-\\s*[A-Z]",
				"\\b[WENS]\\s\\d+( \\d+)?([\\./]\\d+)?'?(\\s(FT|TO|OF|LOT)\\b|,)?"};			
	        int []indexes = new int[stoppers.length];		
			for (int i=0; i<stoppers.length;i++) {
				Matcher ma = Pattern.compile(stoppers[i]).matcher(tmp);
				if(ma.find()) {
					indexes[i] = ma.start();
				} else {
					indexes[i] = indexes[i]= tmp.length() +1;
				}
			}
			
			int index = tmp.length();
			for (int i=0; i<stoppers.length;i++) {
			    if (indexes[i]<index) {
			    	index = indexes[i];
			    }
			}
	        
			if (index > -1) {
				subdiv = tmp.substring(0, index);			
			    subdiv = subdiv.trim();
			} else {
				subdiv = tmp;
			}
			subdiv = subdiv.replaceAll("\\s*/\\s*", "");
			subdiv = subdiv.replaceFirst("\\s*\\b\\d+(ST|ND|RD|TH)\\s*$", ""); // fix for bug #3010
			//subdiv = subdiv.replaceFirst("(?is)(.*)BEING ([A-Z]+).*", "$1$2");  //fix for B3425
			subdiv = subdiv.trim();
		}
		m.put("PropertyIdentificationSet.SubdivisionName", subdiv);		
    }    
}

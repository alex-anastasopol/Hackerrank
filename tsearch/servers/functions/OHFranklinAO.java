package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address.Normalize;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class OHFranklinAO {
	
    protected static String subdivStopperFranklinAO = "\\b(EXT?(ENSION)?|RES|SUB|PLAT|ALL|SEC(TION)?|BLO?C?K|LOTS?)(\\s*\\d+)?\\b.*";
    protected static String subdivFranklinAO = "(.+)\\s" + subdivStopperFranklinAO;
    protected static Pattern subdivFranklinAOPattern = Pattern.compile(subdivFranklinAO);
    
    public static void parseNameOHFranklinAO(ResultMap m,long searchId) throws Exception 
    {
       List<List> body = new ArrayList<List>();
       String[] names = {"", "", "", "", "", ""};
       String[] types, otherTypes;
       String nameString = (String)m.get("tmpOwnerName");
       
       //#### this is for OHFranklinTR
       String nameString1 = (String)m.get("tmpOwnerName1");
       String nameString2 = (String)m.get("tmpOwnerName2");
       if (nameString == null && !StringUtils.isEmpty(nameString1)){
    	   nameString = nameString1 + "    " + nameString2;
       }
       //#####
       
       if(nameString != null) {
       
    	   nameString = nameString.replaceFirst("(?is)\\s{4,}(ET)\\s*(AL|UX|VIR)(\\s{4,})", " $1$2$3");
	       String[] owners = nameString.split("\\s{4,}");
	       String name = "";
	       for (int i = 0; i < owners.length; i++){
	    	   if (!owners[i].matches("(?is)\\s*\\d.*") && !owners[i].matches("(?is)\\s*PO\\s*BOX.*")){
	    		   if (owners[i].endsWith("&")){
	    			   name += owners[i] + " "; //SMITH ALFRED E &\n\nREBECCA M 570-152979-00 
	    		   } else {
	    			   name += owners[i] + " & "; //SMITH ALFRED W  \n\nSMITH BARBARA K   560-211374-00 
	    		   }
	    	   } else break;
	       }
	       name = name.replaceAll("(?is)\\s*&\\s*$", "");;
	       m.put("PropertyIdentificationSet.OwnerLastName", name);
	       
	       if (name.matches("(?is).*CO\\s*&\\s*\\Z"))
	    		   name = name.replaceFirst("(?is)(.*CO).*", "$1");
	       
	       boolean isCompany = false;
	       if (NameUtils.isCompany(name))
	       {
	    	   names[2] = name;
	    	   types = GenericFunctions.extractAllNamesType(names);
	    	   otherTypes = GenericFunctions.extractAllNamesOtherType(names);
	    	   GenericFunctions.addOwnerNames(names, "", "", types, otherTypes, NameUtils.isCompany(names[2]),false, body);
	    	   isCompany= true;
	       }else {
	    	   names = StringFormats.parseNameNashville(name);
			   String[] tmpS = GenericFunctions.extractSuffix(names[1]);
			   names[1] = tmpS[0];
			   String suffix = tmpS[1];
			   tmpS = GenericFunctions.extractSuffix(names[4]);
			   names[4] = tmpS[0];
			   String suffix2 = tmpS[1];
			   names = NameCleaner.middleNameFix(names);
			   
			   types = GenericFunctions.extractAllNamesType(names);
	    	   otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			   GenericFunctions.addOwnerNames(names, suffix, suffix2, types, otherTypes, isCompany, false, body);
		   }
		   
	       GenericFunctions.storeOwnerInPartyNames(m, body, true);
       
       }
    }
    
    protected static String cleanLegalOHFranklinAO(String legal){
    	if (!StringUtils.isEmpty(legal)){
    		legal = legal.replaceAll("&?\\s*\\b\\d+\\s?FT( [SWEN\\s]+ \\d+)?\\b", ""); // fix for bug #2590
    	}
    	return legal;
    }
    
    public static void legalFranklinAO(ResultMap m,long searchId) throws Exception {
    	
    	String leg = (String) m.get("tmpLegal");
    	String[] lines = leg.split("\\s{4,}");
    	String legal1 = "";
    	String legal2 = "";
    	String legal3 = "";
    	String legal4 = "";
    	for (int i = 0; i < lines.length; i++){
    		if( i == 0) {
    			legal1 = lines[i];
    		} else if (i == 1){
    			legal2 = lines[i];
    		}else if (i == 2){
    			legal3 = lines[i];
    		}else if (i == 3){
    			legal4 = lines[i];
    		}
    	}

    	String legal = "";
    	if (legal1 != null && legal1.length() >0){
    		legal = legal + legal1;
    	}
    	if (legal2 != null && legal2.length() >0){
    		legal = legal + "/@/" + legal2;
    	}
    	if (legal3 != null && legal3.length() >0){
    		legal = legal + "/@/" + legal3;
    	}
    	if (legal4 != null && legal4.length() >0){
    		legal = legal + "/@/" + legal4;
    	}
    	
    	legal1 = cleanLegalOHFranklinAO(legal1);
    	legal2 = cleanLegalOHFranklinAO(legal2);
    	legal3 = cleanLegalOHFranklinAO(legal3);
    	legal4 = cleanLegalOHFranklinAO(legal4);
    	
    	String unit = null;
    	boolean unitSetAsLot = false;

		String lotPattFull = "\\b(?:LOTS?\\s*|L\\s+)(\\d+(?:[\\s-&]+\\d+)?)";  // L is for Lot extracting for the search from Bug 2502
		
    	if (legal.length() >0){
    		
    		//extract lot
    		String lot = "";
    		Pattern p = Pattern.compile(lotPattFull);
    		Matcher match = p.matcher(legal);
    		while (match.find()) {
    			lot = lot + " " + match.group(1);
    		}
    		if (lot.matches("(?is)(.*)(\\d{2,})\\s*-\\s*(\\d{1})(.*)")) //B   BELLAIRE LOT 82-3 e de fapt BELLAIRE LOT 82-83
    		{
    			lot = lot.replaceFirst("(.*)(\\d+)(\\d)\\s*-\\s*(\\d{1})(.*)", "$1 $2$3-$2$4 $5");
    		}
    		lot = lot.replaceAll("\\s*&\\s*", " ");
    		lot = lot.replaceAll("\\s{2,}", " ").trim();
    		lot = StringFormats.RemoveDuplicateValues(lot);  
    		if (lot.matches("(?is).*(?:\\s\\d+\\s*-\\s*\\d\\d+|\\A\\d\\s*-\\s*\\d).*"))
    		{
    			String aux = lot;
    			aux = aux.replaceFirst(".*(\\s\\d+\\s*-\\s*\\d\\d+|\\A\\d\\s*-\\s*\\d).*", "$1");
    			String p1 = aux.substring(0,aux.indexOf('-'));
    			String p2 = aux.substring(aux.indexOf('-')+1);
    			int cont1 = 0, cont2 = 0;
    			aux = lot;
    			String[] s = aux.split("\\s");
    			for (int i=0; i<s.length; i++)
    			{
    				if (s[i].equals(p1)) cont1 ++;
    				else
    					if (s[i].equals(p2)) cont2 ++;
    			}
    			if (cont1 >= 1)
    			{
    				String expReg ="\\s" + p1+ "\\s"; 
    				lot = lot.replaceAll(expReg, " ");
    				expReg = "\\A" + p1+ "\\s";
    				lot = lot.replaceFirst(expReg, " ");
    				expReg = "\\s" + p1+ "\\Z";
    				lot = lot.replaceFirst(expReg, " ");
    			}
    			else
        			if (cont2 >= 1)
        			{
        				String expReg ="\\s" + p2+ "\\s"; 
        				lot = lot.replaceAll(expReg, " ");
        				expReg = "\\A" + p2+ "\\s";
        				lot = lot.replaceFirst(expReg, " ");
        				expReg = "\\s" + p2+ "\\Z";
        				lot = lot.replaceFirst(expReg, " ");
        			}
    			lot = lot.replaceAll("\\s{2,}", "");	
    		}
    		if (lot.length() > 0){
    			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
    		}
    		
    		//extract unit
    		p = Pattern.compile(".*\\bUNIT\\s+(\\d+(?:-\\d+|[-\\s]?[A-Z])?)\\b.*");
    		match = p.matcher(legal);
    		if (match.find()) {
	    		unit = match.group(1);
    		}
    		
    		//extract section
    		String section = "";
    		p = Pattern.compile(".*\\bSEC(?:TION)?\\s*#?(\\d+).*");
    		match = p.matcher(legal);
    		if (match.find()) {
	    		section = match.group(1);	    		
    		}
    		//extract sec-twn-rng
    		p = Pattern.compile(".*\\bR([-\\d]+) T([-\\d]+) S([-\\d]+)\\b.*");
    		match = p.matcher(legal);
    		if (match.find()) {    			
    			m.put("PropertyIdentificationSet.SubdivisionTownship", match.group(2));
    			m.put("PropertyIdentificationSet.SubdivisionRange", match.group(1));
    			section = section + " " + match.group(3);
    			section = section.trim();
    		} else{
    			p = Pattern.compile(".*\\bR([-?\\d]+) T([-?\\d]+)\\b.*");
    			match = p.matcher(legal);
    			if (match.find()) {    			
        			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), match.group(1));
        			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), match.group(2));
    			}
    		}
    		
    		if (section != null && section.length() > 0){ 
    			m.put("PropertyIdentificationSet.SubdivisionSection", section);
    		}    		
    		
    		// extract phase
    		p = Pattern.compile(".*\\bPH(?:ASE)?\\s*(\\d+)\\b.*");
    		match = p.matcher(legal);
    		if (match.find()){
    			m.put("PropertyIdentificationSet.SubdivisionPhase", match.group(1));
    		}
    		
    		// extract block
    		p = Pattern.compile(".*\\bBL(?:OC)?K\\s*(\\d+|\\b[A-Z])\\b.*");
    		match = p.matcher(legal);
    		if (match.find()){
    			m.put("PropertyIdentificationSet.SubdivisionBlock", match.group(1));
    		}
    		
    		legal = legal.replaceAll("/@/", " ");
        	legal = legal.replaceAll("\\s{2,}", " ");
        	m.put("PropertyIdentificationSet.Subdivision", legal);
        	m.put("PropertyIdentificationSet.PropertyDescription", legal);
    	} 

    	//check if legal starts with the address
		boolean startsWithAddr = false;
		
		String condoToken = "\\b(CONDOMINIUM|CONDO?|CON)S?\\b";
		
		String streetNo = (String) m.get("PropertyIdentificationSet.StreetNo");
		String streetName = (String) m.get("PropertyIdentificationSet.StreetName");
		
		//usually if address is included in legal description, it appears in the first line, but there are some exceptions, where address is contained in line 2
		// check first the exceptions : PID 240-005708-00 (bug #962)
		if (legal2 != null) {
			//starts with street no 
    		if (streetNo != null && legal2.startsWith(streetNo)){
    			startsWithAddr = true;
    		} else {
    			// contain street name, possible normalized
    			if (streetName != null) {
    				if (legal2.matches(".*"+streetName+".*")){
    					startsWithAddr = true;
	    			} else{
	    				String legalNorm = Normalize.normalizeString(legal2);
	    				String streetNorm = Normalize.normalizeString(streetName);
	    				if (legalNorm.matches(".*\\b"+streetNorm+"\\b.*")){
	    					startsWithAddr = true;
	    				}
	    			}
    			}
    		}
		}
		if (startsWithAddr) {
			String tmp = legal1;
			legal1 = legal2;
			legal2 = tmp;
		} else {
			//check the regular cases, i.e. when address is included in the first line of legal description
	    	if (legal1 != null){    		    		
	    		// starts with street no 
	    		if (streetNo != null && legal1.startsWith(streetNo)){
	    			startsWithAddr = true;
	    		} else {
	    			// contain street name, possible normalized
	    			if (streetName != null) {
	    				if (legal1.matches(".*"+streetName+".*")){
	    					startsWithAddr = true;
		    			} else{
		    				String legalNorm = Normalize.normalizeString(legal1);
		    				String streetNorm = Normalize.normalizeString(streetName);
		    				if (legalNorm.matches(".*\\b"+streetNorm+"\\b.*")){
		    					startsWithAddr = true;
		    				} else { // contains at least one token from street name - ex. PID 040-006023-00 contains only the street suffix
		    					String [] strArray = streetNorm.split(" ");
		    					if (legalNorm.matches("[A-Z]{2,} "+strArray[0]+"\\b.*")){ // fix for bug #913 - PID 025-013039-00 
		    						startsWithAddr = false;								  // (the first street name token is present as second word in legal 1)
		    					} else {
			    					for (int i=0; i<strArray.length; i++) {
			    						if (legalNorm.matches(".*\\b"+strArray[i]+"\\b.*")){
			    							startsWithAddr = true;
			    							break;
			    						}	    							
			    					}
		    					}
		    				}
		    			}
	    				if (startsWithAddr && legal2 != null && legal2.matches(condoToken+".*")){
	    					startsWithAddr = false;
	    				}
	    			}
	    		}
	    	}
		}
    		
    	// extract subdivision and condominium    		
		String condo = "";		
		String condoPattern = "(.*?)\\s+"+condoToken+".*";
		boolean matchedCondoPattern = legal.matches(condoPattern);
		boolean isCondoByCode = false;
		if(!matchedCondoPattern) {
			String tmpLandCode = (String)m.get("tmpLandCode");
			if(!StringUtils.isEmpty(tmpLandCode) &&
					(tmpLandCode.contains("[550]") ||
					tmpLandCode.contains("[551]") ||
					tmpLandCode.contains("[552]") ||
					tmpLandCode.contains("[553]"))) {
				isCondoByCode = true;
			}
		}
		if (matchedCondoPattern || isCondoByCode) { // class D legal - contains condominium
			if (startsWithAddr){
				if (legal2 != null && legal3 != null)
					condo = legal2 + " " + legal3;
			} else {
				if (legal1 != null && legal2 != null)
					condo = legal1 + " " + legal2;
			}
			if(matchedCondoPattern) {
				condo = condo.replaceFirst(condoPattern, "$1");
			}
			if (condo.length() > 0) {
				m.put("PropertyIdentificationSet.SubdivisionCond", condo);
				m.put("PropertyIdentificationSet.SubdivisionName", condo);
			} else {
				m.put("PropertyIdentificationSet.SubdivisionCond", legal);
			}
			if (unit != null && unit.length() > 0){								// fix for bug #913 - PID 025-013039-00
				m.put("PropertyIdentificationSet.isCondo", "true");
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", unit);
				unitSetAsLot = true;
			}
		} else {						// class A, B or C legal
    		String subdiv = "";
    		String lot = "";
			String subdivCand1 = "";
			String subdivCand2 = "";
    		
    		String lotPattern = ".*\\bLOTS?\\b.*";
    		
			if (startsWithAddr){ 			// class A and C legal - subdiv is located at line 2 or 3, depends on where lot info is located 
    			if (legal3 != null && legal3.matches(lotPattern)){
    				if (legal2 != null && !legal2.startsWith("ENTRY")){
    					legal2 = legal2.replaceFirst("\\b\\d+(\\.\\d+)?+X\\d+(\\.\\d+)?FT [SWEN]{1,2} COR\\b", "").trim(); // fix for bug #3008
    					if (legal3.matches("[A-Z]+ LOTS?\\b.+"))	// e.g. PID=010-079722-00 (bug #2590)
    						subdivCand1 = legal2 + " " + legal3;
    					else if (legal2.length() != 0){
    						subdivCand1 = legal2;
    					} else {
    						subdivCand1 = legal3;
    					}
    				}
    			} else if (legal2 != null && legal2.matches(lotPattern)){
    				if (legal3 != null)
    					subdivCand1 = legal3;
    				Pattern p = Pattern.compile(lotPattFull + " (.+)");	// fix for bug #1380 
					Matcher match = p.matcher(legal2);
					if (match.find()){
						subdivCand1 = match.group(2);    					
    				}
    			} else {
    				//line 2 and line 3 does not contain a "LOT" token => check if line 3 contains a number
    				if (legal3 != null){
    		    		// a lot value can be present in line 3 at the beginning (PID 070-009579-00, bug #1159) ...  
    					boolean foundLot = false;
    					Pattern p = Pattern.compile("(\\d+)\\s+BLO?C?K\\s+(\\d+)");
    					Matcher match = p.matcher(legal3);
    					if (match.find()){
    						foundLot = true; 
    						m.put("PropertyIdentificationSet.SubdivisionBlock", match.group(2));
    					} else { // ... or at the end (PID 240-004418-00 bug #968, PID 010-148347-00 bug #2829)
		    				p = Pattern.compile(".*\\b(\\d+)\\s*$");
		    	    		match = p.matcher(legal3);
		    	    		if (match.find()) {
		    	    			foundLot = true;
		    	    		}
    					}
    					if (foundLot){
		    		    	m.put("PropertyIdentificationSet.SubdivisionLotNumber", match.group(1));
	    		    		if (legal2 != null && !legal2.startsWith("ENTRY")) {
	    		    			subdivCand1 = legal2 + " " + legal3;
	    		    			subdivCand2 = legal3;
	    		    		} else 
	    		    			subdivCand1 = legal3;    		    			
						} else {
	    					subdivCand1 = legal3;							
		    				if (legal2 != null && !legal2.startsWith("ENTRY") && !legal2.trim().matches("(?is)R\\d+\\s+T\\d+.*")){  // PID 240-005199-00
		    					subdivCand2 = legal2;
		    					lot = legal2.replaceFirst(".+? (\\d*)$", "$1"); // PID 060-000509-00 (bug #2269)
		    				}
		    			}
    				} else {
    					if (legal2 != null && !legal2.startsWith("ENTRY"))  // PID 240-005199-00
	    					subdivCand1 = legal2;
    				}
    			}
			} else {						// class B legal - subdiv is located at line 1 or line 1 + line 2, depends on where lot info is located
				if (legal2 != null && legal2.matches(lotPattern)){
					subdivCand1 = legal1;
				} else if (legal3 != null && legal3.matches(lotPattern)){
					if (legal1 != null)
						subdivCand1 = legal1;
					if (legal2 != null && legal2.length() > 0)
						subdivCand1 = subdivCand1 + " " + legal2;
				} else {					// legal could not be classified as type A, B, C or D => parsed subdiv from line1 + line2 + line3 
					if (legal1 != null)
						subdivCand1 = legal1;
					if (legal2 != null && legal2.length() > 0)
						subdivCand1 = subdivCand1 + " " + legal2;
					if (legal3 != null && legal3.length() > 0)
						subdivCand1 = subdivCand1 + " " + legal3;
				}
			}
			
			subdiv = parseSubdivFromLegalFranklinAO(subdivCand1.trim(),searchId);
			if (subdiv.length() == 0){
				subdiv = parseSubdivFromLegalFranklinAO(subdivCand2.trim(),searchId);
				if (lot.matches("\\d+"))
					m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			}
			
        	if (subdiv.length() != 0){
        		m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
        	}
		}  
		
		if (unit != null && unit.length() > 0 && !unitSetAsLot){
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}
		
		String conveyNo = (String) m.get("tmpConveyanceNo");
		List<List> bodyCR = new ArrayList<List>();
		List<String> lineCR = null;
		if (!StringUtils.isEmpty(conveyNo)){
			String[] instr = conveyNo.split("@@");
			for (int i = 0; i < instr.length; i++){
				lineCR = new ArrayList<String>();
				lineCR.add("");
				lineCR.add("");
				lineCR.add(instr[i]);
				bodyCR.add(lineCR);
			}
			
			if (!bodyCR.isEmpty()){		  		   		   
			   String [] header = {"Book", "Page", "InstrumentNumber"};		   
			   Map<String,String[]> map = new HashMap<String,String[]>();		   
			   map.put("Book", new String[]{"Book", ""});
			   map.put("Page", new String[]{"Page", ""});
			   map.put("InstrumentNumber", new String[]{"InstrumentNumber", ""});
				   
			   ResultTable cr = new ResultTable();	
			   cr.setHead(header);
			   cr.setBody(bodyCR);
			   cr.setMap(map);		   
			   m.put("CrossRefSet", cr);
			}
		}
    } 
    
    protected static String parseSubdivFromLegalFranklinAO(String subdivCand,long searchId){
    	String subdiv = "";
    	subdivCand = subdivCand.trim();
    	if (subdivCand != null && subdivCand.length()>0){
    		Pattern p = Pattern.compile("(.*?)(\\b(R|S|T)[-\\d\\.\\s]+)+.*");	//PID 610-157032-00
    		Matcher ma = p.matcher(subdivCand);
			if (!ma.find()){   													
				if (!subdivCand.matches(".*\\d+(\\.\\d+)?\\s+AC(RE)?S?.*")		//PID 540-198454-00
					&& !subdivCand.matches("\\d[\\-\\d]+")						//PID 540-198454-00				
					&& !subdivCand.matches(".*\\bVMS \\d+.*")					//PID 040-006276-00
					&& !subdivCand.matches(".*\\bL\\.\\d+\\b.*")				//PID 010-109619-00
					&& !subdivCand.matches("SEE\\b.*\\bFOR\\b.*")				//PID 010-078129-00
					&& !subdivCand.matches("RESUB\\b.*")){						//PID 110-004500-00					
									
		    		Matcher match = subdivFranklinAOPattern.matcher(subdivCand);
		    		if (match.find()) {
		    			subdiv = match.group(1);	    			
		    		} else if (!subdivCand.matches(subdivStopperFranklinAO)){
		    			subdiv = subdivCand;
		    		}
				}
			} else {	// fix for bug #2547, PID 185-000579-00
				subdiv = ma.group(1);
			}			
		}
    	
    	subdiv = subdiv.replaceAll("(?:(N|S|E|W){2})?\\s*L(OTS?)?\\s*\\d+(?:-\\d+)?","");  //BUG 3267
    	subdiv = subdiv.replaceFirst("(?i)(.+?)(\\s+\\bNO\\.?)?\\s+=?#?\\d+(\\s+\\d+)*$", "$1"); //bug #761; PID 560-196831-00, 110-004500-00
    	subdiv = subdiv.replace("BROOKSHIRE PARK", "SCHURTZ BROOKSHIRE PARK"); //bug #761
    	subdiv = subdiv.replaceFirst("L 153 7.5AL VAC", " ");  // bug 2502
    	subdiv = subdiv.replaceFirst("(?:[A-Z\\s-]+)?(?:\\d\\.?)+\\s*\\b(?:N|S|E|W)(?:N|S|E|W)?\\b\\s*(.*)","$1");  //B3610, PID 010-017913-00 , 010-093680-00
    	
    	return subdiv;
    }
}

package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * parsing fuctions for FLPascoTR.
 * 
 * @author mihaib
 */

public class FLPinellasTR {
	
	private static String[] CITIES = {"Belleair Beach", "Belleair Bluffs", "Belleair Shore", "Belleair", "Clearwater", "Dunedin", "Gulfport", "Indian Rocks Beach", 
		"Indian Shores", "Kenneth City", "Largo", "Madeira Beach", "North Redington Beach", "Oldsmar", "Pinellas Park", "Redington Beach", "Redington Shores", "Safety Harbor", 
		"Seminole", "South Pasadena", "St Pete Beach", "St Petersburg", "Tarpon Springs", "Treasure Island", "Bay Pines", "East Lake", "Feather Sound", "Gandy", "Harbor Bluffs", 
		"Palm Harbor", "Ridgecrest", "South Highpoint", "Tierra Verde", "West Lealman", "East Lealman"};
	
	 public static void legalTokenizerFLPinellasTR(ResultMap m, String legal) throws Exception {
		 
		   if(StringUtils.isEmpty(legal)) {
			   String addr = (String) m.get("tmpAddress");
			   if(addr.matches(".*\\bLOT\\s+\\d+\\b.*")) {
				   legal = addr.replaceAll("(.*)\\bLOT\\b", "LOT");
			   }
		   }
		 
		   //initial cleanup legal description
		   legal = legal.replaceAll("\\(UNREC(ORDED)?\\)\\s*", "");
		   legal = legal.replaceAll("\\bPT OF\\s+", "");
		   legal = legal.replaceAll("\\bAKA\\s+", "");
		   legal = legal.replaceAll("\\bCOR? OF\\b\\s+", "");	   
		   legal = legal.replaceAll("\\bCO-OP\\b\\s*", "");	 
		   legal = legal.replaceAll("\\bTHAT PART OF\\b\\s*", "");
		   legal = legal.replaceAll("\\b\\d+\\s+FT\\b", "");
		   legal = GenericFunctions.replaceNumbers(legal);
		   
		   // extract subdivision name
		   String subdiv = "";
		   Pattern p = Pattern.compile("(?s)(.*?)(\\s+|\\s*-\\s*|\\b)(SEC|UNIT|APT|PHASE|LOTS?|BL(?:OC)?K|CONDO|(\\d+(ST|ND|RD|TH) )?(PARTIAL )?REP(LAT)?|SUB|TR(?:ACT)?|(\\d+(ST|ND|RD|TH) )?ADD|(RE-?)?REVISED|PLAT|[SENW]{1,2}('LY)? \\d+([\\./]\\d+)?(\\s*FT)?|(BEG )?\\d+([\\./]\\d+)?\\s*FT)\\b.*");
		   Matcher ma = p.matcher(legal);
		   if (ma.find()){
			   subdiv = ma.group(1).trim();
			   
			   // cleanup subdivision name		   
			   subdiv = subdiv.replaceAll(", THE\\s*$", "");
			   subdiv = subdiv.replaceAll(", PROP OF\\s*$", "");
			   subdiv = subdiv.replaceAll(" PART OF\\s*$", "");
			   subdiv = subdiv.replaceAll(", (CITY|TOWN) OF\\s*$", "");
			   subdiv = subdiv.replaceAll(",\\s*$", "");		   
			   subdiv = subdiv.replaceAll("( NO\\.?)? \\d+\\b\\s*", "").trim();
			   subdiv = subdiv.replace("\n", " ");
			   
			   if (subdiv.length() != 0){		   
				   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				   if (legal.matches(".*\\bCONDO\\b.*"))
					   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }
		   } 
		   
		   // additional legal description cleanup before extracting the rest of the legal elements  
		   legal = legal.replaceAll("\\bNO\\b\\.?\\s*", "");
		   legal = legal.replaceAll("\"", "");
		   legal = legal.replaceAll("\\s*\\bAND\\b\\s*", ",");
		   legal = legal.replaceAll("\\s*&\\s*", ",");
		   legal = legal.replaceAll("\\s*,\\s*", ",");	  
		   legal = legal.replaceAll("\\b\\d+[\\./]\\d+\\b\\s*", "");
		   legal = legal.replaceAll("\\.", "");
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		   	  		   
		   String commonPattern = "(?:\\s+|-)(\\d+(-?[A-Z])?|[A-Z](-?\\d+)?)\\b";	   	   	  
		   // extract section from legal description
		   p = Pattern.compile("\\bSEC" + commonPattern);
		   ma = p.matcher(legal);
		   if (ma.find()){
			   String sec = ma.group(1);		   
			   String secFromPid = (String) m.get("PropertyIdentificationSet.SubdivisionSection");
			   if (secFromPid != null && secFromPid.length() != 0)
				   sec = sec + " " + secFromPid;
			   sec = LegalDescription.cleanValues(sec, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionSection", sec);
		   }
		   	  	   	   	  
		   // extract building # from legal description
		   p = Pattern.compile("\\bBLDG" + commonPattern);
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		   }
		   	   
		   // extract phase from legal description
		   p = Pattern.compile("\\bPHASE" + commonPattern);
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1));
		   }
		   
		   // extract unit from legal description
		   StringBuilder unit = new StringBuilder(); // can have multiple occurrences
		   p = Pattern.compile("\\b(?:UNIT|APT)" + commonPattern);
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   unit.append(" ").append(ma.group(1));
		   }	   
		   if (unit.length() != 0){
			   m.put("PropertyIdentificationSet.SubdivisionUnit", LegalDescription.cleanValues(unit.toString().trim(), false, true));
		   }
		   
		   String lotFromPid = "";
		   String blockFromPid = "";
		   String pid = (String) m.get("PropertyIdentificationSet.ParcelID");
		   //String pidO = "";
		   if (pid != null && pid.length() != 0){
			   blockFromPid = pid.replaceFirst("\\w+/\\w+/\\w+/\\w+/(\\w+)/\\w+", "$1").replaceFirst("^0*(\\d+)", "$1");
			   lotFromPid = pid.replaceFirst("\\w+/\\w+/\\w+/\\w+/\\w+/(\\w+)", "$1").replaceFirst("^0*(\\d+)", "$1");
			   pid = pid.replace("/", "");
			   //pidO = pid.replaceAll("/", "");
			   m.put("PropertyIdentificationSet.ParcelID", pid);
			   m.put("PropertyIdentificationSet.ParcelID2", pid);
			   m.put("PropertyIdentificationSet.ParcelID3", pid);
		   }
		   
		   // extract lot from legal description
		   StringBuilder lot = new StringBuilder(); // can have multiple occurrences
		   p = Pattern.compile("\\bLOTS? (\\d+[A-Z]*(,\\d+[A-Z]*)*)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   lot.append(" ").append(ma.group(1));
		   }	   
		   p = Pattern.compile("\\bLOT ([A-Z])\\b");
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   lot.append(" ").append(ma.group(1));
		   }
		   if (lot.length() != 0){
			   String lotStr = lot.toString().trim();
//			   // verify if the lot extracted from PID, having the last zero removed, is already present in the lots list extracted 
//			   // from the legal description; if it's not, then add the lot extracted from PID to the list of lots
//			   if (lotFromPid.length() != 0){
//				   String lotTrimmed = lotFromPid.replaceFirst("(\\d+)0$", "$1");
//				   if (!lotStr.matches(".*\\b" + lotTrimmed + "\\b.*"))
//					   lotStr = lotStr + " " + lotFromPid;
//			   } // commented - requested by Cristi for bug #2315
			   lotStr = LegalDescription.cleanValues(lotStr, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lotStr);
		   } /*else {
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lotFromPid);
		   }*/
		   
		   // extract block from legal description
		   StringBuilder block = new StringBuilder(); // can have multiple occurrences
		   p = Pattern.compile("(?<=\\bBL(?:OC)?KS?)\\s+(\\d+|[A-Z])(\\s*(?:-|THRU)\\s*(\\d+|[A-Z]))?(?:[&,; ]+|$)");
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   block.append(" ").append(ma.group(1));
			   if(ma.group(3) != null) {
					block.append("-").append(ma.group(3));
			   }
			   block.append(",");
			   legal = ma.replaceFirst(" ");
			   ma.reset(legal);
		   }
		   if (block.length() != 0){
			   String blockStr = block.toString().trim();
//			   if (blockFromPid.length() != 0){
//				   blockStr = blockStr + " " + blockFromPid;
//			   } // commented - requested by Cristi for bug #2315
			   blockStr = LegalDescription.cleanValues(blockStr, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionBlock", blockStr);
		   } /*else if (!blockFromPid.equals("0")){
			   m.put("PropertyIdentificationSet.SubdivisionBlock", blockFromPid);
		   }*/
		   
		   // extract tract from legal description
		   StringBuilder tract = new StringBuilder(); // can have multiple occurrences ?
		   p = Pattern.compile("\\bTR(?:ACTS?)? (([A-Z]|\\d+)(,([A-Z]|\\d+))*)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   tract.append(" ").append(ma.group(1));
		   }
		   if (tract.length() != 0){
			   m.put("PropertyIdentificationSet.SubdivisionTract", LegalDescription.cleanValues(tract.toString().trim(), false, true));
		   }
	   }

	 public static void parseAddressFLPinellasTR(ResultMap m, long searchId) throws Exception {
			
			String address = (String) m.get("tmpAddress");
			
			if (StringUtils.isEmpty(address))
				return;

			address = address.replaceAll(",", "");
			address = address.replaceAll("\\s+", " ");
			address = address.replaceAll("^0+", "");			
						
			address = address.replaceAll(",", "");
			address = address.replaceAll("\\s+", " ");			
			for (int i = 0; i < CITIES.length; i++){
				if (address.toUpperCase().contains(CITIES[i].toUpperCase())){
					m.put("PropertyIdentificationSet.City", CITIES[i]);
//					address = address.toUpperCase().replace(CITIES[i].toUpperCase(), ""); // T42396
					break;
				}
			}
			
			// additional cleaning: remove lot, subdivision name ...
			address = address.replaceFirst("(?is)\\bLOT\\s+\\d+.*", "");
			
			m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address.trim()));
			m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address.trim()));			
			
		}
	 
	 public static void parseNames(ResultMap resultMap, String ownerCell) {
		 
		Vector<String> companyExpr = new Vector<String>();
		Vector<String> excludeWords = new Vector<String>();
		excludeWords.add("GULF");
		excludeWords.add("ST");
		
		String[] lines = ownerCell.split("\n");
		String lastLine = lines[lines.length - 1].trim();
		String[] lastLines = new String[0];
		boolean lastLineIsName = false;
		if(lastLine.matches("[A-Z]+\\s*,[A-Z\\s]+")) {
			lastLine = fixLines(lastLine);
			lastLines = lastLine.split("\\s*\n\\s*");
			lastLineIsName = true;
		}
		
		String properInfo = fixLines(ownerCell);
		lines = properInfo.split("\\s*\n\\s*");
		
		String[] linesBeforeAddr = GenericFunctions.removeAddressFLTR(lines, excludeWords, new Vector<String>(), 2, Integer.MAX_VALUE, true);
		ArrayList<String> nameLines_ = new ArrayList<String>();
		for(int i = 0; i < linesBeforeAddr.length; i++) {
			nameLines_.add(linesBeforeAddr[i]);
		}
		if(lastLineIsName) {
			for(int i = 0; i < lastLines.length; i++) {
				nameLines_.add(lastLines[i]);
			}
		}
		String[] nameLines = nameLines_.toArray(new String[linesBeforeAddr.length + lastLines.length]);
		
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = cleanName(nameLines[i]);
		}
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = nameLines[i].replaceAll("\\A\\s*&\\s*", "").replaceAll("\\s*&\\s*\\z", "").trim();
		}
		
		COFremontAO.genericParseNames(resultMap, nameLines, null, companyExpr, 
				false, COFremontAO.ALL_NAMES_LF, -1);
	 }
	 
	 private static String fixLines(String info) {
		 String properInfo = info;
		 properInfo = properInfo.trim().replaceFirst("(?is)\n\\s*\n.*", "").trim(); // fix for the case when "prior taxes due" is present in the owner info
		 properInfo = properInfo.trim().replaceAll("(?is)\\bTRE\\b", "\nTRUSTEE\n").trim(); 
		 properInfo = properInfo.replaceAll("\\b(?:[,(]\\s*)?(?:AS\\s+)?(?:\\bCO-?\\s*)?(?<!BOARD\\sOF\\s)(T(?:(?:RU?)?S)?TEE?S?)(\\s*([)]|OF))?\\b", "\n$1\n");
		 properInfo = properInfo.replaceAll("\\bET\\s*AL\\b", "\n$0\n");
		 properInfo = properInfo.replaceAll("\\bET\\s*UX\\b", "\n$0\n");		 
		 properInfo = properInfo.replaceAll("\\bET\\s*VIR\\b", "\n$0\n");		 
		 
		 return properInfo;
	 }

	private static String cleanName(String name) {
		 
		String properName = name;
		properName = properName.replaceAll("-?JTWROS", ""); // Joint Tenants with Right of Survivorship
		properName = properName.replaceAll("H&W|W&H|H/W", "");
		properName = properName.replaceAll("\\bT/C\\b", "");
		properName = properName.replaceAll("\\bR/S\\b", "");
		properName = properName.replaceAll("(?is)\\bPOA\\b", "");
		properName = properName.replaceAll("C/O", "");
		properName = properName.replaceAll("TRE", "TRUSTEE");
		properName = properName.replaceAll("ATTN", "");
		
		properName = properName.replaceAll("[0-9]*%", "&");
		properName = COLarimerTR.genericCleanName(properName, new Vector<String>(), "");
		 
		return properName;
	 }
}

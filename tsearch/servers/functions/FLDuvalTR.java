package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLDuvalTR {

		public static Pattern twoWords = Pattern.compile("\\w+\\s+\\w+(\\s" + GenericFunctions.nameSuffixString + ")?");
		public static Pattern oneWord = Pattern.compile("(& )?(\\w+)");
		public static Pattern oneWordSuffix = Pattern.compile("(\\w+)\\s" + GenericFunctions.nameSuffixString);
		public static Pattern initialWord = Pattern.compile("\\w{1}\\s\\w+");
		public static Pattern fourWords = Pattern.compile("(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)");
		public static Pattern threefourWords = Pattern.compile("(\\w+)(\\s\\w+)?\\s(\\w+)\\s(\\w+)");

		public static void partyNamesFLDuvalTR(ResultMap m, long searchId) throws Exception {
			int i;
			String owner = (String) m.get("tmpOwnerAddress");
			if (StringUtils.isEmpty(owner)) {
				return;
			}
			
		    Vector<String> excludeCompany = new Vector<String>();
		    excludeCompany.add("PAT");
		    excludeCompany.add("ST");
		    excludeCompany.add("THE"); //too many addresses bite on it
		    
		    //extra words of NameUtils.isCompanyNamesOnly, useful to merge lines
		    Vector<String> extraCompany = new Vector<String>();
		    extraCompany.add("GWYNNE");
		    extraCompany.add("BRAKE");
		    extraCompany.add("LIFE");
		    extraCompany.add("ESTATE");
		    extraCompany.add("ET");
		    extraCompany.add("AL");
		    extraCompany.add("REAL");
		    extraCompany.add("OF");
		    extraCompany.add("JEHOVAHS");
		    extraCompany.add("WITNE");
		    extraCompany.add("JACKSONVILLE");
		    extraCompany.add("BEACH");
		    extraCompany.add("DANEILA");
		    extraCompany.add("FELTS");
		    
			List<List> body = new ArrayList<List>();
			owner = owner.replaceAll("\n\\s*", "\n");
			owner = owner.toUpperCase();
			String[] lines = owner.split("\n");
			if (lines.length <= 2)
				return;

			// clean address out of mailing address
			String[] lines2 = GenericFunctions.removeAddressFLTR(lines,
					excludeCompany, extraCompany, 2, 30);
			
			lines2 = FLDuvalTR.mergeNamesFLDuvalTR(lines2, " ");
			boolean ampEnd = false;
			String nameOnServer = "";
			for (i = 0; i < lines2.length; i++) {
				if (i==0 && lines2[i].endsWith("&")){
					ampEnd = true;
				}
				String[] names = { "", "", "", "", "", "" };
				boolean isCompany = false;
				// 1 = "L, FM & WFWM"
				// 2 = "F M L"
				// 3 = "F & WF L
				int nameFormat = 1;
				// C/O - I want it before clean because I don't clean company names
				if (lines2[i].matches("(?i).*?c/o.*")
						||lines2[i].matches("^\\s*%.*")) {
					lines2[i] = lines2[i].replaceAll("(?i)c/o\\s*", "");
					lines2[i] = lines2[i].replaceFirst("^\\s*%\\s*", "");
					if (lines2[i].matches("\\w+\\s\\w{1} & \\w+ \\w{1} (TRST)")){
						if (i>0){
							Matcher ma = oneWord.matcher(lines2[i-1]);
							if (ma.find()){
								lines2[i] = lines2[i] + " " + ma.group(2);
							}
						}
					}

					if (StringUtils.indexesOf(lines2[i], '&').size() == 1){
						nameFormat = 3;
					} else {
						nameFormat = 2;
					}
				}
				lines2[i] = lines2[i].replaceFirst("\\s*&\\s*$", "");
				lines2[i] = lines2[i].replaceAll("\\(HUSBAND\\)", "");
				lines2[i] = NameCleaner.paranthesisFix(lines2[i]);

				String curLine = NameCleaner.cleanNameAndFixNoScotish(lines2[i],
						new Vector<String>(), true);
				curLine = cleanOwnerFLDuvalTR(curLine);
				
				nameOnServer += curLine + " & ";
				
				if (NameUtils.isCompany(curLine, excludeCompany, true)) {
					// this is not a typo, we don't know what to clean in companies'
					// names
					lines2[i] = cleanCompanyNameFLDuvalTR(lines2[i]);
					names[2] = lines2[i].replaceAll("^AGENT FOR\\s+", "");
					isCompany = true;
				} else {
					if (i>0){
						Matcher ma = threefourWords.matcher(curLine);
						if (ma.matches() && ampEnd){
							curLine = ma.replaceAll("$4 $1$2 $3");
						} else {
							ma = GenericFunctions.FMLFML.matcher(curLine);
							if (ma.matches()){
								curLine = ma.group(1);
								lines2[i] = ma.group(5);
								i--;
								nameFormat = 2;
							}
						}
						
					}
					

					switch (nameFormat) {
					case 1:
						names = StringFormats.parseNameLFMWFWM(curLine,
								excludeCompany, true, true);
						break;
					case 2:
						names = StringFormats.parseNameDesotoRO(curLine, true);
						break;
					case 3:
						names = StringFormats.parseNameFMWFWML(curLine, excludeCompany, true);
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
					
					suffixes = GenericFunctions.extractAllNamesSufixes(names);
				}
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
			/*
			 * done multiowner parsing
			 */			
			nameOnServer = nameOnServer.replaceAll("\\s+&\\s*$", "");
			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer);
		}
	
		public static String cleanCompanyNameFLDuvalTR(String s){
			s = s.replaceAll("(?is)\\b(ET) (AL|UX|VIR)\\b", "$1$2");
			s = s.replaceAll("^AGENT FOR\\s+", "");
			s = NameCleaner.removeUnderscore(s);
			s = s.replaceAll("-PTS\\s*", "");
			return s.trim();
			
		}
		
		/**
		 * it will also NameCleaner.fixLikeScotishNames
		 * @param a
		 * @param separator
		 * @return
		 */
		public static String[] mergeNamesFLDuvalTR(String[] a, String separator){
			if (a.length==0){
				return a;
			}
			String[] tempStr = new String[a.length];
			int j = 0;
			int i;
			tempStr[0] = NameCleaner.fixScotishLikeNames(a[0]);

			for (i = 1; i < tempStr.length; i++) {
					boolean merge = false;
					a[i] = NameCleaner.fixScotishLikeNames(a[i]);
					if ((a[i-1].endsWith("&") && twoWords.matcher(a[i]).matches())
						|| (oneWord.matcher(a[i]).matches())
						|| (initialWord.matcher(a[i]).matches())
						|| oneWordSuffix.matcher(a[i]).matches()
						|| (a[i-1].endsWith("LIFE") && a[i].startsWith("ESTATE"))){
						merge = true;
					}
					if (merge) {
						tempStr[i - j - 1] += separator + a[i];
						j++;
					} else {
						tempStr[i - j] = a[i];
					}

			}
			
			for(i = a.length-j; i< a.length; i++){
				tempStr[i] = "";
			}
			return tempStr;
		}		
		   
	   protected static String cleanOwnerFLDuvalTR(String s){
		   s = s.toUpperCase();
	       s = s.replaceFirst("\\bH/W\\b", "");			 
	       s = s.replaceAll("\\bC/O \\d+ .*", "");				//e.g. WILSON ADRIAN H TRUSTEE C/O 5822 GERANIUM RD       
	       s = s.replaceAll("\\bC/O\\b", "&");					//e.g. WILSON AGNES TYSON C/O AGNES TYSON WILSON 
	       s = s.replaceAll("\\bB/(E|M)\\b", "");					//e.g  BROWN MAGGIE R B/E
	       s = s.replaceAll("\\bPOSR\\b", "");					//e.g. BROWN MARGARETTA TRUST C/O MARGARETTA Y BROWN POSR
	       s = s.replaceAll(", SMC$", "");				//COBBLE BOBBY JOE, SMC
	       s = s.replaceAll("\\s*&$", "");        
	       s = s.replace(',', ' ');								//e.g. SMITH JOHN V, JR
	       s = s.replaceAll("\\s{2,}", " ").trim();
	       
	       // particular cases duval
	       if (s.startsWith("SMITH ALLEN PAULA") || 
	    		   s.startsWith("SMITH COMAN")){
	    	   s = s.replaceFirst("(\\w+)\\s(\\w+)\\s(.*)", "$1_$2 $3");
	       }
	       s = s.replaceAll("____PTS\\s*", "");
	       s = s.replaceAll("\\s+V.P$", "");
	       s = s.replaceAll("\\s+HEIR(S)?", "");
	       s = s.replaceAll("\\s*/\\s*", " & ");
	       return s;
	   }

	   public static void stdFLDuvalTR(ResultMap m,long searchId) throws Exception {
		   
		String s = (String) m.get("tmpOwnerName");

		if (StringUtils.isEmpty(s))
			return;

		// cleanup
		s = cleanOwnerFLDuvalTR(s);
		s = cleanCompanyNameFLDuvalTR(s);
		String owner = s;
		String coowner = "";
		boolean parseCoowner = false;
		if (s.contains("&")) {
			coowner = s.substring(s.indexOf("&") + 1).trim();
			if (coowner.matches("\\w+ \\w{2,}.*") || // e.g. SMITH JOHN &
														// KIMBERLY BUTLER
					coowner.matches("\\w+ \\w \\w{2,}.*")) { // e.g. SMITH JOHN
																// R & DENISE A
																// TUSKEY
				owner = s.substring(0, s.indexOf("&") - 1);
				parseCoowner = true;
			}
		}
		String[] a = StringFormats.parseNameNashville(owner, true);
		if (parseCoowner) {
			String[] b = StringFormats.parseNameDesotoRO(coowner, true);
			a[3] = b[0];
			a[4] = b[1];
			a[5] = b[2];
		}
		String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
		List<List> body = new ArrayList<List>();
		type = GenericFunctions.extractAllNamesType(a);
		otherType = GenericFunctions.extractAllNamesOtherType(a);
		suffixes = GenericFunctions.extractAllNamesSufixes(a);

		GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], type,
				otherType, NameUtils.isCompany(a[2]),
				NameUtils.isCompany(a[5]), body);

		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		m.put(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName(), a[0]);
		m.put(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName(), a[1]);
		m.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), a[2]);
		m.put(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName(), a[3]);
		m.put(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName(), a[4]);
		m.put(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName(), a[5]);
		
	   }
	   public static void legalFLDuvalTR(ResultMap m, String legal) throws Exception {
		   
		   //initial corrections and cleanup of legal description	   
		   legal = GenericFunctions.replaceNumbers(legal);
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers	   
		   legal = legal.replaceAll("\\bUNI T\\b", "UNIT");
		   legal = legal.replaceAll("\\bS/ DPT\\b", "S/D PT");
		   legal = legal.replaceAll("\\bS/ D\\b", "S/D");
		   legal = legal.replaceAll("\\bPTS?\\b", "");	   
		   legal = legal.replaceAll("\\bRECD\\b", "");
		   legal = legal.replaceAll("\\bNO\\b", "");
		   legal = legal.replaceAll("\\bGOVT\\b", "");
		   legal = legal.replaceAll("\\b[SENW]{1,2}\\d+\\s*/\\s*\\d+,?( OF)?\\b", "");
		   legal = legal.replaceAll("\\b[EWNS]?\\s*\\d*.?\\d+\\s*FT( OF)?\\b", "");
		   legal = legal.replaceAll("\\d*\\.\\d+%?", "");	   	  
		   legal = legal.replaceAll("\\s{2,}", " ").trim();
		   	   	   	   	   	   
		   // extract and remove sec-twn-rng from legal description
		   List<List> body = new ArrayList<List>();
		   Pattern p = Pattern.compile("\\b([\\d,]+)\\s*-\\s*([\\dNSEW]+)\\s*-\\s*([\\dNSEW]+)\\b");
		   Matcher ma = p.matcher(legal);	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();
			   line.add(ma.group(1).replaceFirst("^0+(.+)", "$1").replace(',', ' '));
			   line.add(ma.group(2));
			   line.add(ma.group(3));
			   body.add(line);
			   legal = legal.replace(ma.group(0), ""); 
		   }
		   // extract and remove section from legal description
		   p = Pattern.compile("\\bSEC (\\d+(?:-?[A-Z])?|[A-Z])\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();
			   line.add(ma.group(1).replaceFirst("^0+(.+)", "$1"));
			   line.add("");
			   line.add("");
			   body.add(line);		   
			   legal = legal.replace(ma.group(0), "");
		   }
		   if (!body.isEmpty()){
			   String [] header = {"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"};
			   
			   Map<String,String[]> map = new HashMap<String,String[]>();
			   map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
			   map.put("SubdivisionTownship", new String[]{"SubdivisionTownship", ""});
			   map.put("SubdivisionRange", new String[]{"SubdivisionRange", ""});
			   
			   ResultTable pis = new ResultTable();	
			   pis.setHead(header);
			   pis.setBody(body);
			   pis.setMap(map);
			   m.put("PropertyIdentificationSet", pis);
			   legal = legal.replaceAll("\\s{2,}", " ").trim();
		   }
		   	   
		   // extract and remove plat book & page from legal description
		   p = Pattern.compile("(?<!\\b(?:O/R|BKS?|,)\\s*)\\b(\\d+)-(\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.PlatBook", ma.group(1).replaceFirst("^0+(.+)", "$1"));
			   m.put("PropertyIdentificationSet.PlatNo", ma.group(2).replaceFirst("^0+(.+)", "$1"));
			   legal = legal.replace(ma.group(0), "");
			   legal = legal.replaceAll("\\s{2,}", " ").trim();		   
		   } 	   
		   	   	   	   
		   // additional cleaning before extracting lot, block, unit, phase etc.
		   legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		   legal = legal.replaceAll("\\b(\\d+[A-Z]?)\\s*\\bTO\\b\\s*(\\d+[A-Z]?)\\b", "$1-$2");	   

		   // extract lot from legal description
		   String lot = ""; // can have multiple occurrences
		   p = Pattern.compile("\\bLO?TS? ((?:\\d+[A-Z]?\\b[,\\s-]*)+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   lot = lot + " " + ma.group(1).replaceFirst("[,\\s-]+$", "");
			   legal = legal.replaceFirst(ma.group(0), "LOT ");
		   }
		   lot = lot.trim();
		   if (lot.length() != 0){
			   lot = LegalDescription.cleanValues(lot, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		   }
		   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   	   	   	   
		   // extract block from legal description
		   String block = "";
		   p = Pattern.compile("\\bBL(?:OC)?K (\\d+[A-Z]?|[A-Z])\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   block = block + " " + ma.group(1);
			   legal = legal.replaceFirst(ma.group(0), "BLK ");
		   }
		   block = block.trim();
		   if (block.length() != 0){
			   block = LegalDescription.cleanValues(block, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		   }	   
		   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   
		   // extract unit from legal description
		   p = Pattern.compile("\\bUNIT (\\d+(?:(?:-|\\s)?[A-Z])?)(?:\\s|,|$)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   legal = legal.replaceFirst(ma.group(0), "UNIT ");
			   m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1).replaceFirst("^0+(.+)", "$1").replaceAll("\\s", ""));
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   	   	   
		   // extract phase from legal description
		   p = Pattern.compile("\\bPHASE (\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   legal = legal.replaceFirst(ma.group(0), "PHASE ");
			   m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).replaceFirst("^0+(.+)", "$1"));
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract tract from legal description
		   p = Pattern.compile("\\bTRACTS? ((?:(?:\\d+|[A-Z])[\\s,-]?\\b)+)(?:\\s|$)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   legal = legal.replaceFirst(ma.group(0), "TRACT ");
			   m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(1).replace(',', ' ').replaceFirst("[\\s-]+$", "").replaceFirst("^0+(.+)", "$1"));
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract building #
		   p = Pattern.compile("\\bBLDG ([A-Z]|\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   legal = legal.replaceFirst(ma.group(0), "BLDG ");
			   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   	   	 	   	   
		   // extract cross refs from legal description
		   List<List> bodyCR = new ArrayList<List>();
		   p = Pattern.compile("(?is)\\bO/R (?:BKS? )?([\\d-,\\s]+)\\b");
		   ma = p.matcher(legal);	      	   
		   while (ma.find()){
			   String ors[] = ma.group(1).split(",");
			   for (int i=0; i<ors.length; i++){
				   List<String> line = new ArrayList<String>();			   
				   line.add(ors[i].replaceFirst("(\\d+)\\s*-\\s*(\\d+)", "$1").trim());
				   line.add(ors[i].replaceFirst("(\\d+)\\s*-\\s*(\\d+)", "$2").trim());
				   line.add("OR");
				   bodyCR.add(line);
			   }
		   } 
		   p = Pattern.compile("\\bD BKS? ((?:\\d+\\s*-\\s*\\d+,?)+)\\b");
		   ma.usePattern(p);
		   ma.reset();	      	   
		   while (ma.find()){
			   String ds[] = ma.group(1).replaceFirst("\\s*,\\s*$", "").split(",");
			   for (int i=0; i<ds.length; i++){
				   List<String> line = new ArrayList<String>();		   
				   line.add(ds[i].replaceFirst("(\\d+)\\s*-\\s*(\\d+)", "$1"));
				   line.add(ds[i].replaceFirst("(\\d+)\\s*-\\s*(\\d+)", "$2"));
				   line.add("D");
				   bodyCR.add(line);
			   }
		   }
		   if (!bodyCR.isEmpty()){		  		   		   
			   String [] header = {"Book", "Page", "Book_Page_Type"};		   
			   Map<String,String[]> map = new HashMap<String,String[]>();		   
			   map.put("Book", new String[]{"Book", ""});
			   map.put("Page", new String[]{"Page", ""});
			   map.put("Book_Page_Type", new String[]{"Book_Page_Type", ""});
			   
			   ResultTable cr = new ResultTable();	
			   cr.setHead(header);
			   cr.setBody(bodyCR);
			   cr.setMap(map);		   
			   m.put("CrossRefSet", cr);
		   }
		   
		   // extract subdivision name
		   // first perform additional cleaning 
		   p = Pattern.compile("\\b(\\d+)-(\\d+)\\b");
		   ma = p.matcher(legal);	   
		   while (ma.find()){
			   legal = legal.replace(ma.group(0), "");  //e.g. PIN=049133-0000 , legal=04-91-04913 3511 ACACIA ST 12-9 12-10 BILTMORE E 47.4FT LOT 18,W 26.3FT LOT 19 BLK 31
		   }
		   legal = legal.replaceAll("\\s{2,}", " ").trim();
		   legal = legal.replaceFirst("^(\\d+(ST|ND|RD|TH) )?ADDN TO\\s+", "");
		   legal = legal.replaceFirst("^(S/D|SUBDIVISION)\\s+", "");
		   
		   String subdiv = "";
		   p = Pattern.compile("(.*?)\\s*\\b(?:UNIT|LO?TS?|O/R|S/D|SUBDIVISION|(?:\\d+(?:ST|ND|RD|TH) )?ADDN|TRACTS?|PHASE|(?:A )?CONDO(?:MINIUM)?S?|BL(?:OC)?K|R/P)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   subdiv = ma.group(1);		   
		   } 
		   if (subdiv.length() == 0) {	  
			   p = Pattern.compile(".*\\b(?:LOT|BLK) (.+?) (?:LOT)\\b.*");  //e.g. 07-89-07892 3533 PHYLLIS ST 6-89 S/D LOT 12 BLK 71 EDGEWOOD LOTS 7,8
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   subdiv = ma.group(1);
			   }   
		   }		   
		   if (subdiv.length() != 0){
			   subdiv = subdiv.replaceFirst(",$", "");
			   subdiv = subdiv.replaceFirst("\\s\\d+$", "").trim();
			   if (subdiv.length() != 0){
				   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				   if (legal.matches(".*\\b(CONDO(MINIUM)?S?)\\b.*"))
					   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }
		   }	   
	   }	   
}

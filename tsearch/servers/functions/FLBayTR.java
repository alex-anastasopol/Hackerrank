package ro.cst.tsearch.servers.functions;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.extractor.xml.GenericFunctions;

public class FLBayTR {
    public static void ownerNameTokenizerFLBayTR (ResultMap m, String owner) throws Exception {
    	
    	owner = owner.replaceAll("\\s{2,}", " ");
    	owner = owner.toUpperCase();
    	String[] lines = owner.split("@@");
    	
    	String names[] = {"", "", "", "", "", ""}; 
    	String coowner[] = {"", "", "", "", "", ""};
    	  
    	// extract the owner(s) from the 1st line
    	boolean lineEndsWithAnd = lines[0].endsWith("&");
    	lines[0] = cleanOwnerNameFLBayTR(lines[0]);
    	lines[0] = lines[0].replaceFirst("\\s*\\b(?:AS )?(?:CO-)?(TRUSTEES?)\\s*$", " $1");
    	lines[0] = lines[0].replaceFirst("\\s*,\\s*$", "");    	    
    	//b4287
    	lines[0] = lines[0].replaceFirst("(\\d+.*AVEN?U?E?)|(\\d+.*CT)", "");
    	lines[1] = lines[1].replaceFirst("(?i)(PANAMA CITY|OKEECHOBEE),\\sFL\\s\\d*-?\\d*", "");
    	//apply LFM name tokenizer on the first line - has the format LN, FN (MN|MI)? (& Spouse FN (MN|MI)?)? 
		if (NameUtils.isCompany(lines[0])){
			names[2] = lines[0];
		} else {
			names = StringFormats.parseNameNashville(lines[0], true);
			
			// sometimes spouse first name and middle initial is present on the 2nd line - e.g. PID 30167-422-000: MILLER, BENJAMIN K JR &| SHEREESA L   
			if ((names[5].length() == 0) && (lines.length == 4) && lines[1].matches("[A-Z]+ [A-Z]") && lineEndsWithAnd){
				lines[0] = lines[0] + " & " + lines[1];
				lines[1] = "";
				names = StringFormats.parseNameNashville(lines[0], true);
			}
						
			// sometimes spouse last name is on the 2nd line - e.g. PID 02762-000-000: BROWN, MICHAEL & SANDRA GAIL | BIGBIE  
	    	if (lines.length == 4){
	    		Pattern p = Pattern.compile("(.+) & ([A-Z]+(?: [A-Z]+)?)");
	    		Matcher ma = p.matcher(lines[0]);
	    		if (ma.matches() && lines[1].matches("[A-Z-]+")){ 
	    			lines[0] = ma.group(1);
	    			lines[1] = ma.group(2) + " " + lines[1];
	    			names = StringFormats.parseNameNashville(lines[0], true); 	//L, FM format
	    		} 
	    	}			
		}

		// extract the co-owner(s) from 2nd and 3rd lines, if they exist (2nd and 3rd lines may contain address info)
		if (names[5].length() == 0){
			int lastLineWithOwner = 0;
			
			if (lines.length == 4 && lines[3].matches(".*\\b[A-Z]{2} [\\d-]+\\b.*")){
				if (!GenericFunctions.isStreetAddress(lines[1])){
					if (GenericFunctions.isStreetAddress(lines[2]))
						lastLineWithOwner = 1;
					else if (GenericFunctions.isStreetAddress(lines[3]))
						lastLineWithOwner = 2;
				}
				if (lastLineWithOwner > 0){
					coowner = extractCoOwnerNameFLBayTR(lines[1]);
					
					// check if there is an owner name on the 3rd line (e.g. PID 40000-250-017)
					if ((coowner[2].length() == 0) && (lastLineWithOwner == 2)){
						coowner = extractCoOwnerNameFLBayTR(lines[2]);				
					}
					if (coowner[2].length() != 0){
						names[3] = coowner[0];
						names[4] = coowner[1];
						names[5] = coowner[2]; // spouse from 2nd and co-owner from 3rd line are currently ignored - don't have support in InfSet
					}
				}
			}					
		}
		    	    
		m.put("PropertyIdentificationSet.OwnerFirstName", names[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", names[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", names[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", names[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", names[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", names[5]);
		
		List<List> body = new ArrayList<List>();
		String[] types = GenericFunctions.extractAllNamesType(names);
		String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
		String[] suffixes = GenericFunctions.extractNameSuffixes(names);
		
		GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
										NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
    }
    
    public static String cleanOwnerNameFLBayTR(String line){
    	//line = line.replaceAll("[\\.\\(\\)]", " ");
    	line = line.replaceAll("\\bC/P\\b", "C/O");    	
    	line = line.replaceFirst("([A-Z\\s-]+, [A-Z\\s-]+)\\s*(?:\\bAND\\b|\\bOR\\b|/)\\s*([A-Z\\s-]+)", "$1 & $2");
    	line = line.replaceFirst(",? ESQUIRE\\s*$", "");
    	line = line.replaceFirst("\\bMRS\\s*$", "");
    	line = line.replaceFirst(" WIFE\\b", "");
    	line = StringFormats.unifyNameDelim(line, true);
    	line = line.replaceAll("\\s{2,}", " ");
    	line = line.replaceFirst("&/OR", "");
    	line = line.replaceFirst(", A MINOR", "");
    	line = line.replaceFirst(",\\s*JR$", " JR");
    	line = line.replaceFirst(",\\s*SR\\.*$", " SR");
    	line = line.replaceFirst(",\\s*IV\\s", " IV");
    	line = line.replaceAll("\\.( )?", " ");
    	return line.trim();
    }
    
    protected static String cleanCoOwnerNameFLBayTR(String line){
    	line = cleanOwnerNameFLBayTR(line);
    	line = line.replaceFirst("\\b(?:CO-)?(TRUSTEES?)\\b\\s*", " $1");
    	line = line.replaceFirst("^(AND\\b|&|%|C/O\\b)\\s*", "");
    	line = line.replaceFirst("^OF\\b\\s*", "");		// PID 36280-000-000,  BROWN, DANIEL W TRUSTEE| OF JEAN K BROWN REVOCABLE| LIFE INSURANCE TRUST| 904 WESTOVER DRV BIRMINGHAM, AL 35209-5246
    	line = line.replaceFirst("\\s*,\\s*$", ""); 
    	return line.trim();
    }
    
    
    public static String[] extractCoOwnerNameFLBayTR(String line){
    	String coowner[] = {"", "", "", "", "", ""};
		String nameSuff = "";
		line = cleanCoOwnerNameFLBayTR(line);
		if (line.length() != 0){
			if (NameUtils.isCompany(line)){
				coowner[2] = line;
			} else { 
				if (line.contains(",")){ //L, F M format
					coowner = StringFormats.parseNameNashville(line, true); 	
				} else { 	//SF SM SL format					
					// if owner name is OF OMI? & SF SM? SL, replace it with OF SL & SF (e.g. PID 14148-197-000, MILLER, ANDREW H &| BRAD H & MARYANN MILLER)
				   Pattern p = Pattern.compile("^([A-Z]+(?: [A-Z])?) & (.+ )([A-Z'-]+)$");
				   Matcher ma = p.matcher(line);
				   if (ma.find()){
					   line = ma.group(1) + " " + ma.group(3) + " & " + ma.group(2);
				   }
					coowner = StringFormats.parseNameDesotoRO(line, true);	
				}				  
			}
		}
		return coowner;
    }
    
    public static void partyNamesFLBayTR(ResultMap m, long searchId)
			throws Exception {
    	
		int i;
		String owner = (String) m.get("tmpOwnerInfo");
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		String prevLastName = "";
	    Vector<String> excludeCompany = new Vector<String>();
	    excludeCompany.add("PAT");
	    excludeCompany.add("ST");
	    
	    Vector<String> extraCompany = new Vector<String>();
	    extraCompany.add("BUZBY");
	    
		List<List> body = new ArrayList<List>();
		owner = owner.replaceAll("\\s{2,}", " ");
		owner = owner.toUpperCase();
		String[] lines = owner.split("@@");
		if (lines.length <= 2)
			return;
		// clean address out of mailing address
		String[] lines2 = GenericFunctions.removeAddressFLTR(lines,
				excludeCompany, extraCompany, 2, 30);
		lines2 = NameCleaner.splitName(lines2, excludeCompany);

		for (i = 0; i < lines2.length; i++) {
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
			}
			lines2[i] = lines2[i].replaceFirst("\\s*&\\s*$", "");
			lines2[i] = NameCleaner.paranthesisFix(lines2[i]);
			
			String curLine = NameCleaner.cleanNameAndFix(lines2[i],
					new Vector<String>(), true);
			curLine = cleanOwnerNameFLBayTR(curLine);
			if (curLine.matches("&\\s+\\w+\\s+\\w{1}") && !prevLastName.equals("")){
				curLine = curLine.replaceFirst("&\\s", prevLastName + ", ");
			}
			Vector<Integer> ampIndexes = StringUtils.indexesOf(curLine, '&');
			Vector<Integer> commasIndexes = StringUtils.indexesOf(curLine, ',');

			if (NameUtils.isCompany(curLine, excludeCompany, true)) {
				// this is not a typo, we don't know what to clean in companies'
				// names
				names[2] = lines2[i].replaceAll("^AGENT FOR\\s+", "");
				isCompany = true;
			} else {
				
				// find out the format of c/o name
				if (commasIndexes.size() == 0) {
					//C/O TERRANCE B & NANCY N WYATT
					if (GenericFunctions.FWFL.matcher(curLine).matches()){
						nameFormat = 3;		
					} else {
						nameFormat = 2;
					}
				} else {
					if (commasIndexes.size() == 2){
						if (ampIndexes.size() == 0 && !curLine.matches(".*,\\s*\\w{1,2}$")){
							curLine = curLine.replaceFirst("\\s*,\\s*", "@@@").replaceFirst("\\s*,\\s*", " & ").replaceFirst("@@@", ", ");
							curLine = curLine.replaceFirst("(.*), (.*) &", "$2 $1 &");
							String[] n = curLine.split("\\s&\\s");
							curLine = n[0];
							lines2[i] = n[1];
							i--;
							
							nameFormat = 3;
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

			String[] suffixes = { "", "" }, types = {"", ""}, otherTypes = {"", ""};
			String[] maiden = { "", ""};
			if (!isCompany) {
				names = NameCleaner.tokenNameAdjustment(names);
				if (nameFormat != 1 
					|| !names[2].equals(names[5]) && names[2].length() > 0 && names[5].length() > 0){
					names = NameCleaner.lastNameSwap(names);
					
				}
				names = NameCleaner.removeUnderscore(names);
				types = GenericFunctions.extractAllNamesType(names);
				otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				maiden = NameCleaner.removeUnderscore(maiden);
			}
			prevLastName = names[2];
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
					NameUtils.isCompany(names[2], excludeCompany, true), NameUtils
							.isCompany(names[5], excludeCompany, true), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		/*
		 * done multiowner parsing
		 */
	}


    
    
    public static void legalTokenizerFLBayTR(ResultMap m, String legal) throws Exception {
	  	   
  	   // initial cleanup and correction of the legal description
     	legal = legal.replaceAll("(?<=[A-Z])\\.(?!\\d)", " ");
     	legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) SEC\\b", "SEC $1");
     	legal = legal.replaceAll("\\b(COR|\\d+)(BLK)(?= \\d)", "$1 $2");
     	legal = legal.replaceAll("([A-Z])(SUBD(?:IV)?)\\b", "$1 $2");
     	legal = legal.replaceAll("\\b(?:US )?GOVT? (LOTS?)\\b", "$1");
     	legal = legal.replaceAll("\\b(BLK \\d+)([A-Z]{2,})", "$1 $2");
     	legal = legal.replaceAll("\\bCOTS(?= \\d+\\b)", "LOTS");
     	legal = legal.replaceAll("(?<![A-Z])#\\s*(?=\\d|[A-Z]-?\\d)", "");
     	legal = legal.replaceAll("\\bINC\\b", "");
     	
     	legal = GenericFunctions.replaceNumbers(legal);
     	String origLegal = legal;
     	String[] exceptionTokens = {"I", "M", "C", "L", "D"};
     	legal = legal.replace("-", "__");
   	   	legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
   	   	legal = legal.replace("__", "-");
   	   	
     	legal = legal.replaceAll("\\sTHRU\\s", "-");
     	legal = legal.replaceAll("(?<=\\d) TO (?=\\d)", "-");
     	legal = legal.replaceAll("\\s{2,}", " ");
   	   	
   	   	// extract and remove sec-twn-rng from legal description
   	   	List<List> body = new ArrayList<List>();
   	   	Pattern p = Pattern.compile("^(\\d+(?:/\\d+)*)[-\\s](\\d+[SWEN]?)[-\\s](\\d+[SWEN]?)\\b(?:[A-Z\\s]*(?:\\s*-\\s*)?[\\d\\.]+[A-Z]{0,2}\\s*-\\s*\\(?(?:\\bMAP )?\\d+[A-Z]?\\d*\\)?)?");  	   	
   	   	Matcher ma = p.matcher(legal);	   
  	    if (ma.find()){
  	    	List<String> line = new ArrayList<String>();
  	    	line.add(ma.group(1).replaceAll("/", " "));
  	    	line.add(ma.group(2));
  	    	line.add(ma.group(3));
  	    	body.add(line);
  	    	legal = legal.replace(ma.group(0), ""); 
  	    }
  	    legal = legal.replaceFirst("\\s*-\\s*\\d+\\.\\d+[A-Z]?\\s*-", "");
  	    // extract and replace section from legal description
  	    p = Pattern.compile("\\bSEC(?:TION)? (\\d+)\\b(?![\\.'/])");
  	    ma = p.matcher(legal);
  	    if (ma.find()){
  	    	List<String> line = new ArrayList<String>();
  	    	line.add(ma.group(1));
  	    	line.add("");
  	    	line.add("");
  	    	body.add(line);		   
  	    	legal = legal.replace(ma.group(0), "SEC ");
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
  	    
  	    // extract and remove cross refs from legal description
   	   	List<List> bodyCR = new ArrayList<List>();
   	   	p = Pattern.compile("\\b(ORB|DB)?[\\s\\.]?(\\d+)\\s?P(?:GS?)?[\\s\\.]?(\\d+(?:\\s*[&,/-]\\s*\\d+)*)\\b");
   	   	ma = p.matcher(legal);
   	   	Pattern pTemp = Pattern.compile("((\\d+)\\d)\\s*&\\s*(\\d)");
   	   	Matcher maTemp;
   	   	while (ma.find()){
   	   		List<String> line = new ArrayList<String>();			   
    			line.add(ma.group(2)); 		// book
    			String page = ma.group(3); 	// page
    			maTemp = pTemp.matcher(page);			// ORB 1473 P 1378 & 9 must be transformed in ORB 1473 P 1378 & 1379 (PID 03281-000-000)
    			if (maTemp.matches()){
    				page = maTemp.group(1) + " " + maTemp.group(2) + maTemp.group(3);
    			}
    			page = page.replaceAll("\\s*[&,/]\\s*", " ");
    			line.add(page);
    			String bType = ma.group(1);
    			if (bType == null) 
    				bType = "ORB";
    			line.add(bType);		// book type
    			bodyCR.add(line);
    			legal = legal.replace(ma.group(0), ""); 
   	   	}
   	   	p = Pattern.compile("\\b(ORB|DB)\\s?(\\d+) (\\d+(?:\\s*[&,/-]\\s*\\d+)*)\\b");
 	   	ma = p.matcher(legal);
 	   	while (ma.find()){
 	   		List<String> line = new ArrayList<String>();			   
  			line.add(ma.group(2)); 		// book
  			String page = ma.group(3); 	// page
  			maTemp = pTemp.matcher(page);			
  			if (maTemp.matches()){
  				page = maTemp.group(1) + " " + maTemp.group(2) + maTemp.group(3);
  			}
  			page = page.replaceAll("\\s*[&,/]\\s*", " ");
  			line.add(page);
  			line.add(ma.group(1));		// book type
  			bodyCR.add(line);
  			legal = legal.replace(ma.group(0), ""); 
 	   	}
 	   	p = Pattern.compile("(?is)\\b(OR\\s*(\\d+)/(\\d+))\\b");
	   	ma = p.matcher(legal);
	   	while (ma.find()){
	   		List<String> line = new ArrayList<String>();			   
  			line.add(ma.group(2)); 		// book
  			line.add(ma.group(3)); 	// page
  			String bType = "OR";
			line.add(bType);		// book type
  			bodyCR.add(line);
  			legal = legal.replace(ma.group(0), ""); 
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
   	   	legal = legal.replaceFirst("\\s*\\bORB( \\d+)?\\s*$", "");
   	   	legal = legal.replaceAll("\\s{2,}", " ");
  	   	   	   
   	   	// extract and replace lot from legal description
   	   	String lot = ""; // can have multiple occurrences
   	   	p = Pattern.compile("\\bL(?:OTS? |-)(\\d+[A-Z]?(?:\\s*[,&-]*\\s*\\d+)*|[A-Z](?:\\d+|[IVX]+)?(?:-\\d+)?(?:\\s*[&,]\\s*[A-Z])*)\\b(?!')");
   	   	ma = p.matcher(legal);
   	   	while (ma.find()){
   	   		String lotTemp = ma.group(1);
   	   		lotTemp = lotTemp.replaceAll("\\s*[,&]\\s*", " ");
   	   		lot = lot + " " + lotTemp;
   	   		legal = legal.replace(ma.group(0), "LOT ");
   	   	}
   	   	lot = lot.trim();
   	   	if (lot.length() != 0){
   	   		lot = LegalDescription.cleanValues(lot, false, true);
   	   		m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
   	   	}
   	   	legal = legal.trim().replaceAll("\\s{2,}", " ");
  	   	   	   	   
  	   // extract and replace block from legal description - extract from original legal description
  	   String block = "";
  	   String blkPattern = "\\bB(?:LK\\s?|-)(\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b(?!')";
  	   p = Pattern.compile(blkPattern);
  	   ma = p.matcher(origLegal);
  	   while (ma.find()){
  		   block = block + " " + ma.group(1);
  		   legal = legal.replaceFirst(blkPattern, "BLK ");
  	   } 
  	   p = Pattern.compile("\\bB(\\d+)\\b");
 	   ma = p.matcher(legal);
 	   while (ma.find()){
 		   block = block + " " + ma.group(1);
 		   legal = legal.replace(ma.group(0), "BLK ");
 	   } 
  	   block = block.trim();
  	   if (block.length() != 0){
  		   block = LegalDescription.cleanValues(block, false, true);
  		   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
  		   legal = legal.trim().replaceAll("\\s{2,}", " ");
  	   }	   
  	   
  	   // extract and replace unit from legal description
  	   String unit = "";
  	   p = Pattern.compile("\\b(?:U(?:NIT |-|#)|APT )(\\d+(?:-?[A-Z])?(?:-\\d+[A-Z]?)?|[A-Z](?:-?\\d+)?(\\s*&\\s*[A-Z](?:-?\\d+)?)*(?:-\\d+)?)\\b");
  	   ma = p.matcher(legal);
  	   while (ma.find()){
  		   unit = unit + " " + ma.group(1).replaceAll("\\s*&\\s", " ");
  		   legal = legal.replace(ma.group(0), "UNIT "); 		   
  	   }
  	   unit = unit.trim();
  	   if (unit.length() != 0){
  		  unit = LegalDescription.cleanValues(unit, false, true);
  		  m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
 		  legal = legal.trim().replaceAll("\\s{2,}", " ");
  	   }
  	   	   
  	   // extract and replace building # from legal description
  	   p = Pattern.compile("\\bBLDG (\\d+|[A-Z](?:-?\\d+)?)\\b");
  	   ma = p.matcher(legal);
  	   if (ma.find()){
  		   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
  		   legal = legal.replace(ma.group(0), "BLDG ");
  		   legal = legal.trim().replaceAll("\\s{2,}", " ");
  	   }
  	   
  	   // extract and replace phase from legal description
  	   p = Pattern.compile("\\bPH(?:ASE)? (\\d+(?:-?[A-Z])?|I)\\b");
  	   ma = p.matcher(legal);
  	   if (ma.find()){
  		   m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).replaceFirst("\\bI", "1"));
  		   legal = legal.replace(ma.group(0), "PHASE ");
  		   legal = legal.trim().replaceAll("\\s{2,}", " ");
  	   }
  	   
  	   // extract and replace tract from legal description
  	   String tract = "";
  	   p = Pattern.compile("\\bTRACT (\\d+)\\b");
  	   ma = p.matcher(legal);
  	   while (ma.find()){ 		
  		   tract = tract + " " + ma.group(1); 		   
  		   legal = legal.replace(ma.group(0), "TRACT ");
  	   }
  	   tract = tract.trim();
  	   if (tract.length() != 0){
  		   tract = LegalDescription.cleanValues(tract, true, true);
  		   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
 		   legal = legal.trim().replaceAll("\\s{2,}", " ");
  	   } 	    	   
  	   	   	 	   	    	
  	   legal = legal.replaceAll("\\bRE[\\s-]?PLAT\\b", "PLAT");
  	   legal = legal.replaceAll("\\b(RE[-\\s]?)?SUB(-?D(IV(ISION)?)?)?\\b", "SUBD");
  	   legal = legal.replaceAll("\\bCONDOMINIUMS?\\b", "CONDO");
  	   legal = legal.replaceAll("\\bUNR(E?C(ORDED)?)?\\b", "UNREC");
  	   
  	   // extract subdivision name - only if lot, block or unit was extracted, or the legal contains PLAT, SUBD or CONDO token
  	   if (lot.length() != 0 || block.length() != 0 || unit.length() != 0 || legal.matches(".*\\b(PLAT|SUBD|CONDO|CO BEG)\\b.*")){
  	   
 	 	   // first perform additional cleaning 
 	 	   legal = legal.replaceAll("\\s*\\b(FOR|TO) PO(B|R)\\b", "");
 	 	   legal = legal.replaceAll("\\bCORRECTED\\b\\s*", "");
 	 	   legal = legal.replaceAll("\\b(BEG|COM|COR)\\b.*?(\\s*\\b(LOT|BLK|UNIT))+\\b", "");
 	 	   legal = legal.replaceAll(".*\\b(KNOWN|KWN) AS\\b.*?(\\s*\\b(LOT|BLK))+\\b", "");
 	 	   legal = legal.replaceAll("(\\s*& )?(LESS )?(?<!/)(?<!\\bTH )\\b([SWEN] )?[SWEN]{1,2}(LY)?\\s?[\\d\\.]+'( & [\\d\\.]+')?( TO [SWEN] R/W\\b.*)?", "");
 	 	   legal = legal.replaceAll("\\b(TH (CONT )?[SWEN]{1,2})\\s?[\\d\\.]+'", "$1");
 	 	   legal = legal.replaceAll("\\b[SWEN]{1,2}\\s*\\d+[/\\.]\\d+( OF)?\\b", "");
 	 	   legal = legal.replaceAll("^\\d+(?:ST|ND|RD|TH) (ADD(ITION)?|AMENDED( PLAT)?)( TO)?\\s+", "");
 	 	   legal = legal.replaceAll("^(PLAT|SUBD)( OF)?\\s+", "");	 	   
 	 	   legal = legal.replaceAll("\\s{2,}", " ").trim();
 	 	   
 	 	   String subdiv = "";
 	 	   p = Pattern.compile("(.*?)\\s*(?:(\\bOF )?\\b(?:LOT|BLK|(?:IN )?SEC|UNIT|PLAT|SUBD|(?:\\d+(?:ST|ND|RD|TH) )?ADD(?:ITION)?|TH\\s*(?:\\bCONT\\b\\s*)?(?:\\b[SWEN](?:LY)?|\\bLFT \\d+|$)|UNREC|(?:(?<!-)A )?CONDO|PHASE|TRACT)\\b.*|\\d+'|$)");
 	 	   ma = p.matcher(legal);
 	 	   if (ma.find()){
 	 		   subdiv = ma.group(1);		   
 	 	   } 
 	 	   if (subdiv.length() == 0) {	  
 	 		   p = Pattern.compile("LOT (.*) UNREC");  //PID 07365-010-000, 32788-000-000
 	 		   ma.usePattern(p);
 	 		   ma.reset();
 	 		   if (ma.matches()){
 	 			   subdiv = ma.group(1);
 	 		   }   
 	 	   }
 	 	   if (subdiv.length() == 0) {	  
 	 		   p = Pattern.compile("LOT BLK (.*)");  //PID 0195-000-000
 	 		   ma.usePattern(p);
 	 		   ma.reset();
 	 		   if (ma.matches()){
 	 			   subdiv = ma.group(1);
 	 		   }   
 	 	   }
 	 	   if (subdiv.length() == 0) {	  
 	 		   p = Pattern.compile("(.+ CO) BEG\\b.*");  //PID 08461-000-000, 08468-000-000
 	 		   ma.usePattern(p);
 	 		   ma.reset();
 	 		   if (ma.matches()){
 	 			   subdiv = ma.group(1);
 	 		   }   
 	 	   }
 	 	   subdiv = subdiv.trim();
 	 	   if (subdiv.length() != 0){
 	 		   subdiv = subdiv.replaceFirst("\\s*\\b[SWEN]{1,2}LY\\b.*", "");
 	 		   subdiv = subdiv.replaceFirst("\\s*\\b\\d+(?:ST|ND|RD|TH) AD\\s*$", "");
 	 		   subdiv = subdiv.replaceFirst("\\s*\\bM(AP)?[\\s-]*\\d+\\s*$", "");
 	 		   subdiv = subdiv.replaceFirst("\\s*\\b(M(AP)?[\\s-]*)?\\d+-?[A-Z]\\d*-?\\s*$", "");
 	 		   subdiv = subdiv.replaceFirst("\\s*\\(?[A-Z]\\.\\d+\\)?\\s*,?$", "");
 	 		   subdiv = subdiv.replaceFirst("\\s*\\(?\\d+(\\.\\d+)?(-[A-Z])?\\)?\\s*,?$", "");
 	 		   subdiv = subdiv.replaceFirst("\\s*\\b(M(AP)?[\\s-]*)?\\d+-?[A-Z]\\d*-?\\s*$", "");
 	 		   subdiv = subdiv.replaceFirst("^\\s*THE\\s*$", "");
 	 		   subdiv = subdiv.replaceFirst("^\\s*-\\s*", "");
 	 		   subdiv = subdiv.replaceFirst("\\s*(\\b(I|ORIG|BEG|TH)\\b|[-,\\(])\\s*$", "");	 		   
 	 		   subdiv = subdiv.trim();
 	 		   if (subdiv.length() != 0){
 	 			   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
 	 			   if (legal.matches(".*\\bCONDO\\b.*"))
 	 				   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
 	 		   }
 	 	   }
  	   }
     }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

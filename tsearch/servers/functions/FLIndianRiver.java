package ro.cst.tsearch.servers.functions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLIndianRiver {
	public static void legalTokenizerFLIndianRiverTR(ResultMap m, String legal) throws Exception {
		   
		   //initial corrections and cleanup of legal description
		   legal = legal.replaceAll("\\bTR ACT\\b", "TRACT");
		   legal = legal.replaceAll("\\bLO T(S?)\\b", "LOT$1");
		   legal = legal.replaceAll("\\bLOT S(?= \\d)", "LOTS");
		   legal = legal.replaceAll("\\bBLD G\\b", "BLDG");
		   legal = legal.replaceAll("\\bTHR U\\b", "THRU");
		   legal = legal.replaceAll("\\b(HGHLDS)(SEBASTIAN)\\b", "$1 $2");
		   legal = legal.replaceAll("(\\d+)[}=](\\d+)", "$1-$2");
		   legal = legal.replaceAll("\\b(\\d{2}\\s*&\\s*\\d) (\\d)\\b", "$1$2");
		   legal = legal.replaceAll("\\b(BK \\d+) (\\d+,? P[PG])\\b", "$1$2");
		   legal = legal.replaceAll("\\bPP (\\d+) (\\d+)\\b", "PP $1$2");
		   legal = legal.replaceAll("\\b([SWEN]{1,2} \\d+) (\\d+ )", "$1$2");	   
		   legal = legal.replaceAll("\\b(\\d+)(PB[SI])\\b", "$1 $2");
		   legal = legal.replaceAll("\\bLOT (\\d) (\\d)\\b", "LOT $1$2");
		   legal = legal.replaceAll("\\b(\\d{2,}) (\\d,\\s*\\d)\\b", "$1$2");
		   legal = legal.replaceAll("\\b(\\d+{3,},\\s*\\d{1,2}) (\\d{1,2},)", "$1$2");
		   
		   legal = legal.replaceAll("(\\d) TO (\\d)", "$1-$2");
		   legal = legal.replaceAll("\\sTHRU\\s", "-");
		   legal = legal.replaceAll("(\\d)\\s+-", "$1-");
		   legal = legal.replaceAll("-\\s+(\\d)", "-$1");
		    	   	   	   	   
		   legal = legal.replaceAll("\\bNO\\.?\\b", "");
		   legal = legal.replaceAll("\\b[SWEN]{1,2}\\s*(\\d+ )?\\d*[\\./]?\\d+(\\s*FT)?(\\s*OF)?\\b", "");
		   legal = legal.replaceAll("\\b\\d+ FT [SWEN]( & [SWEN])?\\b", "");
		   legal = legal.replaceAll("\\b[SWEN]{1,2}LY (\\d+ )?\\d*[\\./]?\\d+\\b", "");
		   legal = legal.replaceAll("\\b(PART|PORTION) OF\\b", "");
		   legal = legal.replaceAll("\\bAS IN\\b", "");
		   legal = legal.replaceAll("\\bBEING( A)?\\b", "");
		   legal = legal.replaceAll("\\bGOVT?\\b", "");
		   legal = legal.replaceAll("#", "");
		   legal = legal.replaceAll("\\b\\d+\\.\\s*\\d+\\b", "");
		   legal = GenericFunctions.replaceNumbers(legal);

		   String legalTemp = legal;
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers	   

		   legal = legal.replaceAll("\\s{2,}", " ").trim();
		   
		   // extract and remove lot from legal description	   
		   String lot = ""; // can have multiple occurrences
		   Pattern p = Pattern.compile("\\bLOTS? (\\d+(?:\\s*[&,\\s-]\\s*\\d+)*)\\b");
		   Matcher ma = p.matcher(legal);
		   while (ma.find()){
			   lot = lot + " " + ma.group(1).replaceAll("[,&]", " ");
			   legalTemp = legalTemp.replace(ma.group(0), "LOT ");
		   }
		   p = Pattern.compile("\\bLOT ([A-Za-z])\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   lot = lot + " " + ma.group(1).replaceAll("[,&]", " ");
			   legalTemp = legalTemp.replace(ma.group(0), "LOT ");
		   }
		   lot = lot.replaceAll("\\s{2,}", " ").trim();
		   if (lot.length() != 0){
			   lot = LegalDescription.cleanValues(lot, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		   }
		   	   	   	   
		   // extract and remove block from legal description
		   String block = ""; // can have multiple occurrences
		   p = Pattern.compile("\\bBLK (\\d+[A-Z]?|[A-Z]{1,2}(?:-?\\d+)?)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   block = block + " " + ma.group(1);
			   legalTemp = legalTemp.replace(ma.group(0), "BLK ");
		   }
		   block = block.trim();
		   if (block.length() != 0){
			   block = LegalDescription.cleanValues(block, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		   }	   
		   
		   // extract and remove unit from legal description
		   String unit = ""; // can have multiple occurrences
		   p = Pattern.compile("\\b(?:UNIT|APT) (\\d+[A-Z]?|[A-Z](?:-?\\d+)?)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   unit = unit + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");
			   legalTemp = legalTemp.replace(ma.group(0), "UNIT ");
		   }
		   p = Pattern.compile("\\bU (\\d+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   unit = unit + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");
			   legalTemp = legalTemp.replace(ma.group(0), "UNIT ");
		   }
		   
		   unit = unit.trim();
		   if (unit.length() != 0){
			   unit = LegalDescription.cleanValues(unit, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		   }

		   // extract and remove building # from legal description
		   p = Pattern.compile("\\bBLDG (\\d+[A-Z]?|[A-Z](?:-?\\d+)?)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   legalTemp = legalTemp.replace(ma.group(0), "BLDG ");
			   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		   }
		   
		   // extract and remove phase from legal description
		   String patt = "(?:\\d+(?:-?[A-Z])?|I)";
		   p = Pattern.compile("\\bPH(?:ASES?)? ("+patt+"(\\s*[&,\\s]\\s*"+patt+")*)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   legalTemp = legalTemp.replace(ma.group(0), "PHASE ");
			   String phase = ma.group(1).replaceAll("[,&]", " ");
			   phase = phase.replaceAll("\\s{2,}", " ");
			   if (phase.equals("I"))  // phase is always a number  
				   phase = "1";
			   m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		   }
		   
		   // extract and remove tract from legal description
		   p = Pattern.compile("\\bTR(?:ACT)? (\\d+[A-Z]?)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   legalTemp = legalTemp.replace(ma.group(0), "TRACT ");
			   m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(1));
		   }
		   	   
		   // extract and remove plat book & page from legal description
		   List<List> body = new ArrayList<List>();  // can have multiple occurrences
		   p = Pattern.compile("\\bPB[IS] (\\d+)-(\\d+[A-Z]?(?:\\s*[&,]\\s*\\d+[A-Z]?)*)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();
			   line.add(ma.group(1).replaceFirst("^0+(.+)", "$1"));
			   line.add(ma.group(2).replaceAll("[,&]", " ").replaceAll("\\s{2,}", " ").replaceAll("\\b0+(\\d+)", "$1"));
			   body.add(line);
			   legalTemp = legalTemp.replace(ma.group(0), "PBI ");		   
		   } 	   
		   
		   if (!body.isEmpty()){
			   String [] header = {"PlatBook", "PlatNo"};
			   
			   Map<String,String[]> map = new HashMap<String,String[]>();
			   map.put("PlatBook", new String[]{"PlatBook", ""});
			   map.put("PlatNo", new String[]{"PlatNo", ""});
			   
			   ResultTable pis = new ResultTable();	
			   pis.setHead(header);
			   pis.setBody(body);
			   pis.setMap(map);
			   m.put("PropertyIdentificationSet", pis);
		   }
		   	 	   
		   // extract cross refs from legal description
		   List<List> bodyCR = new ArrayList<List>();
		   p = Pattern.compile("\\b(OR|R|D)? BK (\\d+),? P[PG] (\\d+)\\b");
		   ma = p.matcher(legal);	      	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();			   
			   line.add(ma.group(2).replaceFirst("^0+(.+)", "$1"));
			   line.add(ma.group(3).replaceAll("\\b0+(\\d+)", "$1"));
			   line.add(ma.group(1) == null? "OR":ma.group(1));
			   bodyCR.add(line);
			   legalTemp = legalTemp.replace(ma.group(0), "OR ");
		   } 
		   p = Pattern.compile("\\b(\\d+)/(\\d+)\\b");
		   ma = p.matcher(legal);	      	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();			   
			   line.add(ma.group(1).replaceFirst("^0+(.+)", "$1"));
			   line.add(ma.group(2).replaceAll("\\b0+(\\d+)", "$1"));
			   line.add("OR");
			   bodyCR.add(line);
			   legalTemp = legalTemp.replace(ma.group(0), "OR ");
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
		   String subdiv = "";
		   legalTemp = legalTemp.replaceAll("[\\(\\)]", "");
		   legalTemp = legalTemp.replaceAll("\\s{2,}", " ").trim();	   
		   p = Pattern.compile("(.*?),?\\b\\s*(SUB(DIVISION)?|UNIT|PH(ASES?)?|LOTS?|BLK|(RE)?PLAT|CONDO(MINIUM)?|S/D|U \\d+|OR|BLDGS?|\\d+(ST|ND|RD|TH) ADD|ADD(?!')|PARCEL|APT)\\b.*");
		   ma = p.matcher(legalTemp);
		   if (ma.find()){
			   subdiv = ma.group(1);
		   }	   
		  
		   if (subdiv.matches(".*\\bAS DESC IN\\b.*")) 	   //Account#=787550  Legal=PORTION OF SW1/4 OF SW1/4 AS IN R BK 211 PP 531 BEING LOT 32 OF CREEKWOOD II (OR BK 661 PP 1109)
			   subdiv = "";

		   if (subdiv.length() == 0 && lot.length() != 0){
			   p = Pattern.compile(".*\\b(?:(?:LOTS?|BLK|OR|UNIT) )+(.*)\\s*\\b(OR)");  
			   ma = p.matcher(legalTemp);
			   if (ma.find()){
				   subdiv = ma.group(1);
			   }	   
		   }
		   
		   if (subdiv.length() != 0){
			   // remove last token from subdivision name if it is a number (as roman or arabic)
			   p = Pattern.compile("(.*)\\b(\\w+)$");
			   ma = p.matcher(subdiv);
			   if (ma.find()){
				   String lastToken = ma.group(2); 
				   lastToken = Roman.normalizeRomanNumbersExceptTokens(lastToken, exceptionTokens); 
				   if (lastToken.matches("\\d+")){
					   subdiv = ma.group(1);
					   subdiv = subdiv.replaceFirst("\\s*#$", "");
				   }
			   }
			   subdiv = subdiv.replaceFirst("\\s*\\b\\d+$", "");
			   subdiv = subdiv.replaceFirst("\\s*-\\s*$", "");
			   subdiv = subdiv.replaceFirst("^OF\\b\\s*", "");
			   subdiv = subdiv.replaceFirst("\\s*,\\s*$", "");
			   subdiv = subdiv.replaceFirst("^\\s*,\\s*", "");
			   subdiv = subdiv.replaceFirst("\\bLESS( A)?\\b", "");
			   subdiv = subdiv.replaceFirst(".*\\bF/K/A (.+)", "$1"); // fix for bug #3023
			   subdiv = subdiv.replaceAll("\\s{2,}", " ").trim();
			   if (subdiv.length() != 0){
				   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				   if (legalTemp.matches(".*\\bCONDO(MINIUM)?\\b.*"))
					   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }
		   }
		}
	
	public static void stdFLIndianRiverTR(ResultMap m, long searchId) throws Exception {

		String owners = (String) m.get("tmpOwner");
		if (StringUtils.isEmpty(owners))
			return;

		owners = owners.replaceAll("(?is)\\s*&\\s*$", "");
		owners = owners.replaceAll("\\(.\\)", "");
		owners = owners.replaceAll("(?is)[\\(\\)]", " ");
		owners = owners.replaceAll("(?is)CO-?(TRS?)", "$1");
		owners = owners.replaceAll("(?is)\\s+\\*\\s+", " & ");
		owners = owners.replaceAll("\\d+/\\d+", "");
		owners = owners.replaceAll("(?is)C/O", "");
		owners = owners.replaceAll("(?is)\\bTOK\\b", "");
		owners = owners.replaceAll("(?is)[\\(\\)]+", "");
		String[] lines = owners.split("(?i)<\\s*br\\s*/?\\s*>");
		// the owner and co-owner name are stored in the first 1 or 2 lines and
		// the last 2 lines contains the mailing address

		String[] a = { "", "", "", "", "", "" };
		String[] b = { "", "", "", "", "", "" };
		String owner = cleanOwnerFLIndianRiverTR(lines[0]);
		
		owner = fixNames(owner);
				
		if (NameUtils.isCompany(owner))
			a[2] = owner;
		else {
			a = StringFormats.parseNameNashville(owner, true);
		}
		int lineForMailingAddress = 1;

		// extract co-owner from second line if not already extracted from the
		// first line
		if ((a[5].length() == 0) && (lines.length >= 4)) {
			String coowner = cleanOwnerFLIndianRiverTR(lines[1]);
			b = StringFormats.parseNameDesotoRO(coowner, true);
			a[3] = b[0];
			a[4] = b[1];
			a[5] = b[2];
			lineForMailingAddress += 1;
		}

		/*m.put(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName(), a[0]);
		m.put(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName(), a[1]);
		m.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), a[2]);
		m.put(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName(), a[3]);
		m.put(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName(), a[4]);
		m.put(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName(), a[5]);*/
		
		String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
		List<List> body = new ArrayList<List>();
		type = GenericFunctions.extractAllNamesType(a);
		otherType = GenericFunctions.extractAllNamesOtherType(a);
		suffixes = GenericFunctions.extractAllNamesSufixes(a);

		GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], type,
				otherType, NameUtils.isCompany(a[2]),
				NameUtils.isCompany(a[5]), body);

		GenericFunctions.storeOwnerInPartyNames(m, body, true);

	}
	
	public static String fixNames(String s) {
				
		String nameSuffixString = "(?:\\sJR|SR|DR|[IV]{2,}|[III]{2,}|\\d+(?:ST|ND|RD|TH))?";
		s = s.replaceAll("\\s{2,}", " ");
		
		//e.g. SMITH JOHN GLENN WILLIE MAE, WILSON JOHN W LINDA M, WILD OSCAR J ASHIMA SAHAI, OLIVE PATRICIA A MATTHEW J JR
		Matcher matcher = Pattern.compile("\\A([\\w-']+\\s[\\w-']+\\s[\\w-']+" +  nameSuffixString 
				                        + ")(\\s[\\w-']+\\s[\\w-']+" + nameSuffixString + ")\\z").matcher(s);
		if (matcher.find()) {
			String s1 = matcher.group(1);
			String s2 = matcher.group(2);
			if (!s1.matches(".*" + GenericFunctions1.nameTypeString + "\\s*\\z") && !s2.matches(".*" + GenericFunctions1.nameTypeString + "\\s*\\z")) {
				s =  s1 + " &" + s2;
			}
		}
		
		matcher.reset();
		//e.g. LEIGHTON ARMANDO DIANE
		matcher = Pattern.compile("\\A([\\w-']+)\\s([\\w-']+)\\s([\\w-']+)\\z").matcher(s);
		if (matcher.find()) {
			String first1 = matcher.group(2);
			String first2 = matcher.group(3);
			if ((NameFactory.getInstance().isMaleOnly(first1)&&NameFactory.getInstance().isFemaleOnly(first2))|| 
				(NameFactory.getInstance().isMaleOnly(first2)&&NameFactory.getInstance().isFemaleOnly(first1))) {
				s = matcher.group(1) + " "  + matcher.group(2) + " & " +  matcher.group(3);
			}
		}
		
		matcher.reset();
		//e.g. CROSS JOHN WILLIE ROXIE FAYE SMITH
		matcher = Pattern.compile("\\A([\\w-']+\\s[\\w-']+\\s[\\w-']+" +  nameSuffixString 
                + ")\\s([\\w-']+)+\\s([\\w-']+)+\\s([\\w-']+)+(" + nameSuffixString + ")\\z").matcher(s);
		if (matcher.find()) {
			String s1 = matcher.group(1);
			String s2 = matcher.group(2);
			String s3 = matcher.group(3);
			String s4 = matcher.group(4);
			String s5 = matcher.group(5);
			if (!s1.matches(".*" + GenericFunctions1.nameTypeString + "\\s*\\z") && !s4.matches(".*" + GenericFunctions1.nameTypeString + "\\s*\\z")) {
				if (FirstNameUtils.isFirstName(s2)&&!LastNameUtils.isLastName(s2)&&
						LastNameUtils.isLastName(s4)&&!FirstNameUtils.isFirstName(s4)) {
						s = s1 + " & " + s4 + " " + s2 + " " + s3 + " " + s5;
					}
			}
		}
		
		return s;
	}
   
	public static String cleanOwnerFLIndianRiverTR(String s){
		s = s.toUpperCase();
		s = s.replaceAll("([\\d\\.]+)?%", "").trim();
		s = s.replaceFirst("&\\z", "");
		s = s.replaceFirst("\\bIRA\\z", "");
		s = s.replaceFirst("\\bOR\\z", "");
		s = s.replaceAll("\\*", "");
		s = s.replaceFirst("\\s*&\\s*$", "");
		s = s.replaceAll("\\(.*?\\)", "");
		s = s.replaceFirst("DDS MS PA", "");
		s = s.replaceFirst("^FBO\\b", "");
		s = s.replaceFirst("(?is)CO-?(TRS?)", "$1");
		s = s.replaceAll("\\s{2,}", " ").trim();
		return s;
	}
	
	/**
	 * These pattern should be removed from cleanOwnerFLIndianRiverTR when 
	 * legacy names structures will be removed
	 * @param s
	 * @return
	 */
	public static String preCleanName(String s){
		s = s.replaceFirst("(TRS) OF ", " $1");
		s = s.replaceFirst("^FBO\\b", "C/O ");
		s = s.replaceAll("(?is)\\bTOK\\b", "");
		s = s.replaceAll("(?is)\\(.\\)", "");
		s = s.replaceAll("(?is)[\\(\\)]+", "");
		s = s.replaceAll("\\s+\\*\\s+", " & ");
		
		return s.trim();
	}
	
	public static void partyNamesFLIndianRiverTR(ResultMap m, long searchId){
		   String owners = (String) m.get("tmpOwner");
		   if (StringUtils.isEmpty(owners))
			   return;		
		   //saving examples
		   /*
		   String o = owners;
		   String filename = "flindianrivertr";
		   String contain = ""; //if the same function is used for inter, then what substring should be in o to save it
	        if (m.get("saving") == null){
			      if (o.contains(contain)){  
			        try {
			            FileWriter fw = new FileWriter(new File("/home/danut/Desktop/work/parsing/" + filename + "/" + filename + ".html"),true);
			             fw.write(o);
			            fw.write("\n==============================================================\n");
			            fw.close();
			        }
			        catch (IOException e) {
			            e.printStackTrace();
			        }
			      }
		        
		        }		
		   // done saving
	        */
			ArrayList<String> lines = new ArrayList<String>();

			int i;
			String[] lTmp = owners.split("\\n");
			for(String s:lTmp){
				lines.add(preCleanName(s));
			}
			Vector<String> excludeCompany = new Vector<String>();
			excludeCompany.add("PAT");
			excludeCompany.add("ST");
			
			Vector<String> extraCompany = new Vector<String>();
			
			Vector<String> extraInvalid = new Vector<String>();
 			
			List<List> body = new ArrayList<List>();
			if (lines.size() <= 2)
				return;
			// clean address out of mailing address

			ArrayList<String> lines2 = GenericFunctions.removeAddressFLTR2(lines,
					excludeCompany, extraCompany, extraInvalid, 2, 30);

			for (i = 0; i < lines2.size(); i++) {
				String[] names = { "", "", "", "", "", "" };
				boolean isCompany = false;
				// 1 = "L, FM & WFWM"
				// 2 = "F M L"
				// 3 = "F & WF L
				int nameFormat = 1;
				// C/O - I want it before clean because I don't clean company names
				String ln = lines2.get(i);
				ln = ln.replaceFirst("&\\z", "");
				if (ln.matches("(?i).*?c/o.*")
						||ln.matches("^\\s*%.*")) {
					ln = ln.replaceAll("(?i)c/o\\s*", "");
					ln = ln.replaceFirst("^\\s*%\\s*", "");
					nameFormat = 2;
				}
				if (ln.matches("(?is).*\\bATTN:.*")) {
					ln = ln.replaceFirst("(?is).*\\bATTN:", "");
					nameFormat = 2;
				}
				
				String curLine = NameCleaner.cleanNameAndFix(ln, new Vector<String>(), true);

				curLine = cleanOwnerFLIndianRiverTR(curLine);
				Vector<Integer> ampIndexes = StringUtils.indexesOf(curLine, '&');
				Vector<Integer> commasIndexes = StringUtils.indexesOf(curLine, ',');
				
				if(curLine.matches("\\w+ \\w+ \\w \\w+ \\w")){
					curLine = curLine.replaceAll("(\\w+ \\w+ \\w) (\\w+ \\w)","$1 & $2");
				}
				
				if (curLine.matches("\\w+ \\w+ \\w+ \\w+ \\w \\w+")) {			//e.g. WOODRUFF ROBERT DEAN JANICE S SMITH
					Matcher matcher = Pattern.compile("(\\w+ \\w+ \\w+) (\\w+) (\\w) (\\w+)").matcher(curLine);
					if (matcher.find()) {
						String s1 = matcher.group(2);
						String s2 = matcher.group(4);
						if (FirstNameUtils.isFirstName(s1) && !LastNameUtils.isLastName(s1)
						 && LastNameUtils.isLastName(s2) && !FirstNameUtils.isFirstName(s2))	{
							curLine = matcher.group(1) + "  & " + matcher.group(4) + " " + matcher.group(2) + " " + matcher.group(3); 
						}
					}
					nameFormat = 4;
				}
				
				String oldCurLine = curLine;
				curLine = fixNames(curLine);
				if (oldCurLine.compareTo(curLine)!=0) {
					nameFormat = 4;
				}
				
				if (NameUtils.isCompany(curLine, excludeCompany, true)) {
					// this is not a typo, we don't know what to clean in companies'
					// names
					names[2] = ln;
					isCompany = true;
				} else {

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
					case 4:
						names = StringFormats.parseNameNashville(curLine, true);
						break;
					}
				}
			
				String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
				if (!isCompany) {
					names = NameCleaner.tokenNameAdjustment(names);
					suffixes = GenericFunctions.extractAllNamesSufixes(names);
					names = NameCleaner.removeUnderscore(names);
				}
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
			try {
				GenericFunctions.storeOwnerInPartyNames(m, body, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			/*
			 * done multiowner parsing
			 */	        

	}
	

	
}

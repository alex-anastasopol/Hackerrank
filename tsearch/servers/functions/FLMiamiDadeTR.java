package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class FLMiamiDadeTR {
		public static String space = "I_I_I_I_I_";
	
	   public static Pattern lastName = Pattern.compile("(\\w+) & .* (\\w+)");
	   public static Pattern firstInitial = Pattern.compile("\\w{2,}(\\s\\w{1})?(\\s\\w{1})?");
	   public static Pattern firstInitialInitial = Pattern.compile("(\\w{2,})\\s(\\w{1})\\s(\\w{1})");
	   
	   public static Pattern twoWords = Pattern.compile("(\\w+)\\s+(\\w+)");
	   public static Pattern extreme = Pattern.compile("(\\w+)\\s(.*)\\s(\\w+)");
	   public static Pattern lastTwoInitial = Pattern.compile("(\\w{1})\\s(\\w{1})$");
	   public static Pattern sixWords = Pattern.compile("((\\w+)\\s(\\w+)\\s(\\w+))\\s((\\w+)\\s(\\w+)\\s(\\w+))");
	   		

	   public static void legalFLMiamiDadeTR(ResultMap m, long searchId) throws Exception {
		   String line1 = (String) m.get("tmpLegalL1");
		   String line2 = (String) m.get("tmpLegalL2");
		   String line3 = (String) m.get("tmpLegalL3");
		   String line4 = (String) m.get("tmpLegalL4");
		   String fullLegal = (String) m.get("tmpFullLegalDescr");

		   String legal = "";
		   if (fullLegal != null && fullLegal.length() != 0)
			   legal = fullLegal;
		   
		   if (line1 == null)
			   line1 = "";
		   if (line2 == null)
			   line2 = "";
		   if (line3 == null)
			   line3 = "";
		   if (line4 == null)
			   line4 = "";
		   if (legal.length() == 0){
			   legal = line1 + " " + line2 + " " + line3 + " " + line4;
			   legal = legal.replaceAll("\\s{2,}", " ");
			   legal = legal.trim();
		   }
		   if (legal.length() == 0)
			   return;
		   
		   m.put("PropertyIdentificationSet.PropertyDescription", legal);
		   line1 = cleanFLMiamiDadeTRLegalLines(line1);
		   line2 = cleanFLMiamiDadeTRLegalLines(line2);
		   
		   legal = legal.replaceAll("[&,]", " ");
		   legal = legal.replaceAll("ONE", "1");
		   legal = legal.replaceAll("TWO", "2");
		   legal = legal.replaceAll("THREE", "3");
		   legal = legal.replaceAll("\\bNO\\b\\s*", "");
		   
		   String subdiv = "";
		   
		   Pattern p;
		   Matcher ma;
		   if (line1.matches("[\\d-]+ \\d+ \\d+\\b.*")){
			   p = Pattern.compile("(.*?)\\s(PB|SEC(TION)?|(RE)?SUB|UNIT)\\b");
			   ma = p.matcher(line2);
			   if (ma.find())
				   subdiv = ma.group(1);
		   } else {
			   p = Pattern.compile("(.*?)\\s(PB|SEC(TION)?|CONDO|UNIT|(RE)?SUB)\\b");
			   ma = p.matcher(line1);
			   if (ma.find()){
				   subdiv = ma.group(1);
			   } else if (line2.startsWith("PB ") || (line2.contains(" PB ") && line2.startsWith("LOT"))){
				   subdiv = line1.replaceFirst("(.*?)\\s(SEC(TION)?|CONDO|UNIT|(RE)?SUB)\\b.*", "$1");
			   }
		   }
		   if (subdiv.length() != 0){
			   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			   if (legal.contains("CONDO"))
				   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		   }
			   	   
		   // extract section, township and range from legal description
		   p = Pattern.compile("^([\\d-]+) (\\d+) (\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));
			   m.put("PropertyIdentificationSet.SubdivisionTownship", ma.group(2));
			   m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(3));
		   } else {
			   p = Pattern.compile("\\bSEC(?:TION)?\\s+(\\w+)");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));
			   }
	   		}
		   	   
		   // extract unit from legal description
		   p = Pattern.compile("\\bUNIT\\s+((([A-Z]|\\d+)-)?\\d+)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1));
		   }
		   
		   // extract block from legal description
		   p = Pattern.compile("\\bBLK\\s+([-\\w]+)");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionBlock", ma.group(1));
		   }
		   
	       // extract lot from legal description
		   String lot = ""; // can have multiple occurrences
		   p = Pattern.compile("\\bLOTS?\\s+([-\\d\\s]+)");
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   lot = lot + " " + ma.group(1);
		   }
		   lot = lot.trim();
		   if (lot.length() != 0){
			   lot = LegalDescription.cleanValues(lot, true, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		   }
		   
		   // extract plat book & page from legal description
		   p = Pattern.compile("\\bPB\\s+([A-Z]|\\d+)-(\\d+)");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			   m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
		   }
	   }
	   
	   
	   public static void legalFLMiamiDadeTR(ResultMap map, String legal) throws Exception {
//		   legal =(String) map.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		   legal = legal.replaceAll("[&,]", " ");
		   legal = legal.replaceAll("\\s+", " ");
		   legal = legal.replaceAll("ONE", "1");
		   legal = legal.replaceAll("TWO", "2");
		   legal = legal.replaceAll("THREE", "3");
		   legal = legal.replaceAll("\\bNO\\b\\s*", "");
		   legal = legal.replaceAll("(?is)UNDIV\\s*[\\d\\.%\\s]+\\bINT\\b", "");
		   legal = legal.replaceAll("(?is)\\s*(?:\\bIN\\b)?\\s*COMMON ELEMENTS?\\s*(?:\\bOFF\\b)?\\s*", " ");
//		   legal = legal.replaceAll("(?is)(?:THE)?\\s*(?:N(?:ORTH)?|S(?:OUTH)?|W(?:EST)?|E(?:AST)?)\\s*\\bTOWER\\b\\s*", "");
		   legal = legal.replaceAll("(?is)\\bLOT SIZE\\s(SITE VAL(UE)?|[\\d\\.]+\\s*X\\s*\\d+|\\.?\\d+(?:\\sAC)?)\\s", "");
		   legal = legal.replaceAll("(?is)SQ(UARE)?\\s+F(EE)?T\\s+(F/A/U\\s*[\\d-]+\\s*|&\\s*)?", "");
		   legal = legal.replaceAll("(?is)F/?A/?U\\s*[\\d-\\s]+\\s*", "");
		   legal = legal.replaceAll("(?is)(?:&?\\s*PROP\\s*)?\\bINT IN (COMMON AREAS?|&?\\s*TO COM(MON)? ELEM(ENTS?)?\\s*)", "");
		   legal = legal.replaceAll("(?is)(COC\\s*\\d+\\s*-\\s*\\d+)\\s+\\d{2}\\s+\\d{4}\\s+\\d{1,2}", "$1");
		   legal = legal.replaceAll("(?is)(OR\\s*\\d+\\s*-\\s*\\d+)\\s+\\d+\\s+\\d+", "$1");
		   legal = legal.replaceAll("(?is)\\s*LESS\\s*[A-Z]?[\\d\\.]+\\s*FT\\s*", " ");
		   
		   if (legal.contains(" SEC") || legal.contains(" SECTION")) {
			   String section = legal;
			   section = section.replaceFirst("(?is).*\\bSEC(TION)?\\b\\s+(\\w)\\s+.*", "$1").trim();
			   if (StringUtils.isNotEmpty(section)) {
				   map.put(PropertyIdentificationSetKey.SECTION.getKeyName(), section);
			   }
			   legal = legal.replaceFirst("(?is)(.*)\\s*\\bSEC(?:TION)?\\b\\s+\\w(.*)", "$1" + "$2");
		   }
		   
		   String subdiv = "";
		   
		   String regExp = "(?is)(?:\\d+\\s*&\\s*)?(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(.*)\\bPB\\b\\s+(\\d+)\\s*-\\s*(\\d+).*";
		   Matcher m = Pattern.compile(regExp).matcher(legal);
		   
		   if (m.find()) { 
			   // extract section, township and range from legal description
			   String sct = m.group(1).trim();
			   String twn = m.group(2).trim();
			   String rng = m.group(3).trim();
			   if (StringUtils.isNotEmpty(sct))
				   map.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sct);
			   if (StringUtils.isNotEmpty(twn))
				   map.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), sct);
			   if (StringUtils.isNotEmpty(rng))
				   map.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), sct);

			   subdiv = m.group(4).trim();
			   if (subdiv.contains(" CONDO")) {
				   subdiv = subdiv.replaceFirst("(?is)\\bCONDO(MINIUM)?\\b", "").trim();
				   if (StringUtils.isNotEmpty(subdiv))
					   map.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			   } else {
				   subdiv = subdiv.replaceFirst("(?is)\\b(?:RE)?SUB\\b", "").trim();
				   if (StringUtils.isNotEmpty(subdiv))
					   map.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
			   }
			   
			   // extract plat book & page from legal description
			   String pb = m.group(5).trim();
			   String pp = m.group(6).trim();
			   if (StringUtils.isNotEmpty(pb))
				   map.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pb);
			   if (StringUtils.isNotEmpty(pb))
				   map.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pp);
			   
			   legal = legal.replaceFirst("(?is)(?:\\d+\\s*&\\s*)?\\d+\\s+\\d+\\s+\\d+\\s+.*\\bPB\\b\\s+\\d+\\s*-\\s*\\d+(.*)","$1");
			   
		   } else {
			   regExp = "(?is)(.*)\\bPB\\b\\s+(\\d+)\\s*-\\s*(\\d+).*";
			   m = Pattern.compile(regExp).matcher(legal);
			   if (m.find()) {
				   subdiv = m.group(1).trim();
				   if (subdiv.contains(" CONDO")) {
					   subdiv = subdiv.replaceFirst("(?is)\\bCONDO(MINIUM)?\\b", "").trim();
					   if (StringUtils.isNotEmpty(subdiv))
						   map.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
				   } else {
					   subdiv = subdiv.replaceFirst("(?is)\\b(?:RE)?SUB\\b", "").trim();
					   if (StringUtils.isNotEmpty(subdiv))
						   map.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
				   }
				   
				   // extract plat book & page from legal description
				   String pb = m.group(2).trim().replaceFirst("0+(\\d+)", "$1");
				   String pp = m.group(3).trim().replaceFirst("0+(\\d+)", "$1");
				   if (StringUtils.isNotEmpty(pb))
					   map.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pb);
				   if (StringUtils.isNotEmpty(pb))
					   map.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pp);
				   
				   legal = legal.replaceFirst("(?is).*\\bPB\\b\\s+\\d+\\s*-\\s*\\d+(.*)", "$1").trim();
				   if (legal.matches("\\w+\\s*-\\s*\\d+.*")) {
					  legal = legal.replaceFirst("\\w+\\s*-\\s*\\d+(.*)", "$1").trim();					   
				   }
			   
			   } else {
				   regExp = "(?is)(.*)\\bUNIT\\b\\s+((?:[A-Z])?-?\\d+-?(?:[A-Z])?).*";
				   m = Pattern.compile(regExp).matcher(legal);
				   if (m.find()) {
					   subdiv = m.group(1).trim();
					   if (subdiv.contains(" CONDO")) {
						   subdiv = subdiv.replaceFirst("(?is)\\bCONDO(MINIUM)?\\b", "").trim();
						   if (StringUtils.isNotEmpty(subdiv))
							   map.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
					   } else {
						   subdiv = subdiv.replaceFirst("(?is)\\b(?:RE)?SUB\\b", "").trim();
						   if (StringUtils.isNotEmpty(subdiv))
							   map.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
					   }
					   
					   // extract unit from legal description
					   String unit = m.group(2).trim();
					   if (StringUtils.isNotEmpty(unit))
						   map.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
					   
					   legal = legal.replaceFirst("(?is).*\\bUNIT\\b\\s+(?:[A-Z])?-?\\d+-?(?:[A-Z])?(.*)", "$1").trim(); 
				   }
			   }
		   }
		   
		  // extract lot from legal description --> can have multiple occurrences
		   if (legal.contains(" LOT") || legal.contains("LOT ")) {
			   String lot = "";
			   regExp = "\\bLOTS?\\b\\s*([A-Z]?[\\d-\\s]+).*";
			  
			   m.usePattern(Pattern.compile(regExp));
			   m.reset();
			   while (m.find()){
				   lot = lot + " " + m.group(1).trim();
			   }
			   lot = lot.trim();
			   if (lot.length() != 0){
				   lot = LegalDescription.cleanValues(lot, true, true);
				   map.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
			   }
			   legal = legal.replaceAll("\\bLOTS?\\b\\s*([A-Z]?[\\d-\\s]+)", "").trim();
			   legal = legal.replaceAll("&", " ");
			   legal = legal.replaceAll("\\s+", " ");
		   }
		   
		   // extract block from legal description
		   if (legal.contains("BLOCK ") || legal.contains("BLK ")) {
			   String blk = legal;
			   blk = blk.replaceFirst("(?is)\\s*\\bBL(?:OC)?K\\b\\s*([-\\w]+).*", "$1").trim();
			   if (StringUtils.isNotEmpty(blk))
				   map.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk);
			   legal = legal.replaceFirst("(?is)\\s*\\bBL(?:OC)?K\\b\\s+[-\\w]+(.*)", "$1").trim();
		   }
		   
		   
		if (StringUtils.isNotEmpty(legal)) {

			List<List> body = new ArrayList<List>();
			List<String> line = null;

			m = Pattern.compile("(?is)0*(\\d+)\\s*-\\s*0*(\\d+)").matcher(legal);
			while (m.find()) {
				line = new ArrayList<String>();
				line.add(m.group(1));
				line.add(m.group(2));
				body.add(line);
			}

			if (body != null) {
				ResultTable rt = new ResultTable();
				String[] header = { "Book", "Page" };
				rt = GenericFunctions2.createResultTable(body, header);
				map.put("SaleDataSet", rt);
			}
		}
		   
		   
		   
		   /*
		   
		   if (legal.contains("OR ") || legal.contains("COC ") || legal.contains("REC ")) {
			   //extract cross references
			   List<List> body = new ArrayList<List>();
			   List<String> line = null;
			   String bk = "";
			   String pg = "";
			   String tmp = legal;
			  
			   if (tmp.contains("OR ") && tmp.contains("REC ")) {
				   if (tmp.matches("(?is).*\\bREC\\b.*\\bOR\\b.*"))
					   tmp = tmp.replaceFirst("(?is).*\\bREC\\b", "REC");
				   else if (tmp.matches(".*\\bOR\\b.*\\bREC\\b.*"))
					   tmp = tmp.replaceFirst("(?is).*(\\bOR\\b.*)", "$1");
					   
				   tmp = tmp.replaceAll("(?is)\\b(?:REC|OR)\\b\\s*(\\d+\\s*-\\s*\\d+)", "$1 ");
				   tmp = tmp.replaceFirst("(?is)\\s*\\bCOC\\b\\s+\\d+\\s*-\\s*\\d+.*", "").trim();
				   
			   } else if (tmp.contains("OR ")){
					   tmp = tmp.replaceFirst("(?is)\\bOR\\b\\s+(\\d+\\s*-\\s*\\d+).*", "$1").trim();
					   
			   } else if (tmp.contains("REC ")){
				   tmp = tmp.replaceFirst("(?is)\\bREC\\b\\s+(\\d+\\s*-\\s*\\d+).*", "$1").trim();
				   
			   } else 
				   tmp = "";
			   if (legal.contains("COC ")) {
				   String tmp2 = legal;
				   tmp2 = tmp2.replaceFirst("(?is).*\\bCOC\\b\\s+(\\d+\\s*-\\s*\\d+).*", " $1").trim();
				   tmp += " " + tmp2.trim();
				   tmp = tmp.trim();
			   }
				   
			   if (StringUtils.isNotEmpty(tmp)) {
				   regExp = "(?is)[^\\d]*0*(\\d+)\\s*-\\s*0*(\\d+)(.*)";
				   m = Pattern.compile(regExp).matcher(tmp);
				   while (m.find()) {
					   bk = m.group(1).trim();
					   pg = m.group(2).trim();
					   if (StringUtils.isNotEmpty(bk) && StringUtils.isNotEmpty(pg)) {
						   line = new ArrayList<String>();
						   line.add(bk);
						   line.add(pg);
						   body.add(line);
					   } 
					   tmp = m.group(3).trim();
				   }
			   }
			   
			   if (body != null){
					ResultTable rt = new ResultTable();
					String[] header = { "Book", "Page" };
					rt = GenericFunctions2.createResultTable(body, header);
					map.put("SaleDataSet", rt);
				}
		   }
		   
		   */
		   
		   return;
	   }
	   
	   public static void parseNamesFLMiamiDadeTR(ResultMap map, long searchId) throws Exception {
		   String fullName = (String) map.get("tmpOwner");
		   if (fullName == null || fullName.length() == 0 || fullName.contains("REFERENCE ONLY"))
			   return;	
		   
		   	fullName = fullName.replaceAll("(?is)\\n", "<br>");
		   	fullName = fullName.replaceAll("\\s+", " ");
			ArrayList<String> lines = new ArrayList<String>();
			int i;
			String [] tmp = fullName.split("<br>");
			int noOfLines = tmp.length;
//			boolean foundAdr = false;
			if (noOfLines >=3) {
				for (int j=0; j <= noOfLines-1; j++) {
					String key = "tmpMailL" + (j+1);
					String val = tmp[j];
					//if (val.matches("\\d+\\s+.*") || foundAdr) {
//						val = "";
//						foundAdr = true;
//					}
					map.put(key, val);
					if (j==3 && noOfLines>4) {
						for (int ii=4; ii < noOfLines; ii++) {
							//if (!tmp[ii].matches("\\d+\\s+.*") && !foundAdr)
							if (!tmp[ii].matches("\\d+\\s+.*"))
								val += " " + tmp[ii];
						}
						map.put(key, val);
					}
				}
			}
			
			String tmpAddress;
			tmpAddress = (String) map.get("tmpMailL1");
			if (!StringUtils.isEmpty(tmpAddress)){
				lines.add(tmpAddress);
			}
			tmpAddress = (String) map.get("tmpMailL2");
			if (!StringUtils.isEmpty(tmpAddress)){
				lines.add(tmpAddress);
			}
			tmpAddress = (String) map.get("tmpMailL3");
			if (!StringUtils.isEmpty(tmpAddress)){
				lines.add(tmpAddress);
			}
			tmpAddress = (String) map.get("tmpMailL4");
			if (!StringUtils.isEmpty(tmpAddress)){
				lines.add(tmpAddress);
			}

			Vector<String> excludeCompany = new Vector<String>();
			excludeCompany.add("PAT");
			excludeCompany.add("ST");
			
			Vector<String> extraWordsMerge = new Vector<String>();
			
			List<List> body = new ArrayList<List>();
			if (lines.size() <= 2)
				return;
			// clean address out of mailing address
			ArrayList<String> lines2 = removeAddressFLMiamiDadeTR(fullName, lines, extraWordsMerge);
			for (int j=1; j<=4; j++)
				map.remove("tmpMailL" +j);
			for (i = 0; i < lines2.size(); i++) {
				String[] names = { "", "", "", "", "", "" };
				boolean isCompany = false;
				int nameFormat = 3;
				// 1 = "L, FM & WFWM"
				// 2 = "F M L"
				// 3 = "F & WF L
				
				// C/O - I want it before clean because I don't clean company names
				String ln = lines2.get(i);
				if (ln.matches("(?i).*?c/o.*") || ln.matches("^\\s*%.*")) {
					ln = ln.replaceAll("(?i)c/o\\s*", "");
					ln = ln.replaceFirst("^\\s*%\\s*", "");
				} else if (ln.matches("(?is)[\\w\\s]+\\bTRS\\b")) {
					ln = ln.replaceFirst("\\bTRS\\b", "TRUST");
				}
				
				String curLine = "";
				if (ln.matches("(?is)[\\d\\s\\w]+\\b(LLC|L.L.C.|L L C|INC|CORP|LTD)\\b"))
					curLine = ln;
				else
					curLine = NameCleaner.cleanNameAndFix(ln, extraWordsMerge, true);
				curLine = cleanNameFLMiamiDadeTR(curLine);
				
				if (NameUtils.isCompany(curLine, excludeCompany, true)) {
					// this is not a typo, we don't know what to clean in companies names
					ln = ln.replaceAll("^OF ", "");
					names[2] = ln.replaceAll("(DTD|UTD|TAD)?\\s+\\d+(-|/)\\d+(-|/)\\d+", "");
					isCompany = true;
				} else {
					String curLineTmp = fixNameFLMiamiDadeTR(curLine);
					ArrayList<String> splittedLine = splitNamesFLMiamiDadeTR(curLineTmp, curLine);
					
					for(int ii = 1; ii<splittedLine.size(); ii++){
						lines2.add(splittedLine.get(ii));
					}
					curLine = splittedLine.get(0);

					switch (nameFormat) {
					case 1:
						names = StringFormats.parseNameLFMWFWM(curLine,	excludeCompany, true, true);
						break;
					case 2:
						names = StringFormats.parseNameDesotoRO(curLine, true);
						
						break;
					case 3:
						names = StringFormats.parseNameFMWFWML(curLine, excludeCompany, true);
						break;
					}
				}
			
				String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
				String[] maiden = { "", ""};
				if (!isCompany) {
					names = NameCleaner.tokenNameAdjustment(names);
					if (nameFormat != 1 || !names[2].equals(names[5]) && names[2].length() > 0 && names[5].length() > 0){
						names = NameCleaner.lastNameSwap(names);
						
					}
					names = revertSpace(names);
					types = GenericFunctions.extractAllNamesType(names);
					otherTypes = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractAllNamesSufixes(names);
					maiden = NameFactory.getInstance().extractMaiden(names);
					names = NameCleaner.removeUnderscore(names);
					maiden = NameCleaner.removeUnderscore(maiden);
				}

				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
						isCompany, false, body);
			}
			GenericFunctions.storeOwnerInPartyNames(map, body, true);
			/*
			 * done multiowner parsing
			 */		   
	   }
	   
	   /**
	    * Swap names like LOUVENIA SMITH &H JOSEPH
	    */
	   public static String fixNameFLMiamiDadeTR(String s){
		   if (s.matches(".* &(\\s+)?W .*") || s.matches(".* &(\\s+)?H .*")){
			   String[] n = s.split("\\s+&(\\s+)?(?:H|W)\\s+");
			   n[0] = n[0].replaceAll("(\\w+)\\s(" + GenericFunctions.nameSuffixString + ")$", "$2 $1");
			   Matcher ma = firstInitialInitial.matcher(n[1]);
			   if (ma.matches()){
				   s = ma.group(1) + " " + ma.group(2) + space + ma.group(3);
			   } else {
				   s = n[1];
			   }
			   ma = extreme.matcher(n[0]);
			   if (ma.matches()){
				   s += " & " + ma.group(1) + " " + ma.group(2).replaceAll(" ", space) + " " +ma.group(3);
			   } else {
				   s += " & " + n[0];
			   }
			   
			   s = s.replaceAll("( &)+", " &");
			   
		   }
		   return s;
	
	   }
	   
	   public static String[] revertSpace(String[] names){
		   String [] tmps = new String[names.length];
		   for(int i = 0; i<names.length; i++){
			   tmps[i] = names[i].replaceAll(space, " ");
		   }
		   return tmps;
	   }
	   
	   public static String cleanNameFLMiamiDadeTR(String s){
		   s = s.replaceAll("\\s+JTRS", "");
		   s = s.replaceFirst("\\bTRS\\b", "TRUST");
		   s = s.replaceFirst("^REM(:)?\\s", "");
		   s = s.replaceAll("\\s*\\(.*?\\)$", "");
		   s = s.replaceAll("^MR ", "");
		   s = s.replaceAll("\\s*REPSOLD\\s*", "");		   
		   s = s.replaceAll("\\s*A/D\\s*", "");
		   s = s.replaceAll("\\s*&([^WH\\s])", " & $1");
		   s = s.replaceAll("\\s+AS CUSTODIAN\\s*", "");
		   s = s.replaceAll("\\s+ETLAS\\s*", "");
		   s = s.replaceAll("\\s*&(W|H)?$", "");
		   
		   if (s.equals("HELEN J WALRATH & THOMAS E")){
			   s = s.replaceAll(" & THOMAS E", "");
		   }
		   return s;
	   }
	   
	   protected static String cleanFLMiamiDadeTRLegalLines(String line){
		   line = line.replaceAll("\\bREV( PLAT)?( OF)?\\s*", "");
		   line = line.replaceAll("\\bREPLAT\\s*", "");
		   line = line.replaceAll("\\bPORT(ION)? OF\\s*", "");
		   line = line.replaceAll("(\\b\\d+(ST|ND|RD|TH) )?\\bAMD PL(AT)?( OF)?\\b\\s*", ""); 
		   line = line.replaceAll("\\b((FEC|\\d+(ST|ND|RD|TH)) )?ADDN?( TO?)?\\b\\s*", "");
		   return line.trim();
	   }
	   
		public static ArrayList<String> removeAddressFLMiamiDadeTR(String fullName, ArrayList<String> a, Vector<String> mergeNames) {
			ArrayList<String> tmpStr = new ArrayList<String>();
			int j = 0;
			for(int i=0; i<a.size(); i++){
				String s = a.get(i);
				if (NameCleaner.isValidName(s) && fullName.contains(s)){
						if (i> 0  && (NameUtils.isCompanyNamesOnly(s, mergeNames)
										|| s.startsWith("&W")
										|| firstWordIsLastOnly(s) && s.contains(" & "))){
							j++;
							tmpStr.set(i-j, tmpStr.get(i-j) + " " + s);
						} else if (!s.matches("\\d+\\s+.*") && !s.matches(".*(,\\s*[A-Z]{2}\\s*)?[\\d-]+")){
							tmpStr.add(s);
						} else if (s.matches("(?is)[\\d\\s\\w]+(LLC|L.L.C.|L L C)")) {
							tmpStr.add(s);
						}
				} else {
					break;
				}
			}
			return tmpStr;
		}	  
		
		
		public static boolean firstWordIsLastOnly(String s){
			String[] names = s.split(" & ");
			return NameFactory.getInstance().isLastOnly(names[0]);
		}
		
	public static ArrayList<String> splitNamesFLMiamiDadeTR(String sFixed, String sNormal) {
		Vector<Integer> ampIndexes = StringUtils.indexesOf(sFixed, " & ");
		ArrayList<String> n = new ArrayList<String>();
		n.add(sFixed);
		if (!ampIndexes.isEmpty()
				&& ampIndexes.get(ampIndexes.size() - 1) >= sFixed.length() - 2) {
			// remove the last amp (maria ioana & vasile &)
			ampIndexes.remove(ampIndexes.size() - 1);
		}
		if (ampIndexes.size() == 1) {
			String names2[] = sFixed.split("\\s*&\\s*");

			// CHARLES W SMITH &W EMILY ANN
			boolean dontSplit = false;
			if (!sFixed.equals(sNormal)) {
				// we fixed &W &H
				Matcher ma = twoWords.matcher(names2[0]);
				if (ma.matches()) {
					if (NameFactory.getInstance().isSameSex(ma.group(1),
							ma.group(2))
							|| !NameFactory.getInstance().isLast(ma.group(2))) {
						dontSplit = true;
					}

				}
			}

			if (!firstInitial.matcher(names2[0]).matches()
					&& !firstInitial.matcher(names2[1]).matches() && !dontSplit 
					&& !names2[0].equals("ANNIE LOU")) {
				n.set(0, names2[0]);
				n.add(names2[1]);
			}
		} else if (ampIndexes.size() == 0){
			Matcher ma = sixWords.matcher(sFixed);
			if (ma.matches()){
				n.set(0, ma.group(1));
				n.add(ma.group(5));
				
			}
		} else if (ampIndexes.size() == 2){
			String[] names = sFixed.split("\\s*&\\s*");
			if (names[0].matches("\\w{1} \\w{1}")
					|| names[0].matches("\\w+")){
				n.set(0, names[0] + " & " + names[1]);
				n.add(names[2]);
			} else if (names[0].matches("\\w+\\s\\w+\\s\\w+") 
					|| (names[0].matches("\\w{1} \\w{2,}") 
							&& names[1].matches("\\w{1} \\w{2,}") 
							&& names[2].matches("\\w{1} \\w{2,}"))){
				n.set(0, names[0]);
				n.add(names[1]);
				n.add(names[2]);
			} else if (names[0].matches("\\w{1} \\w+")){
			
				n.set(0, names[1] + " & " + names[0]);
				n.add(names[2]);
			} else if(names[0].matches("\\w{2,} \\w{2,}")){
				String[] n2 = names[0].split("\\s");
				n.set(0,names[0]);
				for (int index=1; index< names.length; index++){
					if (names[index].matches("\\w+(\\s" + GenericFunctions.nameSuffixString + ")?")){
						n.add(names[index] + " " + n2[1]);
					} else {
						n.add(names[index]);
					}
				}
			}
			
		}
		
		return n;
	}

}

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
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLAlachuaTR {
	
	public static void partyNamesFLAlachuaTR(ResultMap m, long searchId) throws Exception {
		   
		int i;
		String owner = (String) m.get("tmpMaillingAddress");
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		owner = owner.replaceFirst("Mailing\\sAddress\\s*", "");
	    Vector<String> excludeCompany = new Vector<String>();
	    excludeCompany.add("PAT");
	    excludeCompany.add("ST");
	    excludeCompany.add("(?i)\\bD F\\b");
	    
	    Vector<String> extraCompany = new Vector<String>();
	    extraCompany.add("BUZBY");
	    
		List<List> body = new ArrayList<List>();
		//owner = owner.replaceAll("\\s{2,}", " ");
		owner = owner.toUpperCase();
		String[] lines = owner.split("\n");
		if (lines.length <= 2)
			return;

		// clean address out of mailing address
		String[] lines2 = GenericFunctions.removeAddressFLTR(lines,
				excludeCompany, extraCompany, 2, 30);
		lines2 = NameCleaner.mergeNames(lines2, " ");
		for (i = 0; i < lines2.length; i++) {
			String[] names = { "", "", "", "", "", "" };
			boolean isCompany = false;
			// 1 = "L, FM & WFWM"
			// 2 = "F M L"
			// 3 = "F & WF L
			// 4 = "L & L & L"
			int nameFormat = 1;
			// C/O - I want it before clean because I don't clean company names
			if (lines2[i].matches("(?i).*?c/o.*")
					||lines2[i].matches("^\\s*%.*")) {
				lines2[i] = lines2[i].replaceAll("(?i)c/o\\s*", "");
				lines2[i] = lines2[i].replaceFirst("^\\s*%\\s*", "");
				nameFormat = 2;
				//C/O TERRANCE B & NANCY N WYATT
				if (GenericFunctions.FWFL.matcher(lines2[i]).matches()){
					nameFormat = 3;	
					
				}				
			}
			lines2[i] = lines2[i].replaceFirst("\\s*&\\s*$", "");
			lines2[i] = NameCleaner.paranthesisFix(lines2[i]);

			String curLine = NameCleaner.cleanNameAndFix(lines2[i],
					new Vector<String>(), true);
			
			curLine = addAmpersandIfNecessary(curLine);
			
			//particular words
			curLine = cleanNameFLAlachuaTR(curLine);
			Vector<Integer> ampIndexes = StringUtils.indexesOf(curLine, '&');
			if (ampIndexes.size() > 0){
				lines2[i] = curLine;
				lines2 = splitNameAlachua(lines2, excludeCompany);
				curLine = lines2[i];
				

			}
			if (NameUtils.isCompany(curLine, excludeCompany, false)) {
				// this is not a typo, we don't know what to clean in companies'
				// names
				names[2] = cleanCompanyName(lines2[i]);
				isCompany = true;
			} else {
				
				switch (nameFormat) {
				case 1:
					names = StringFormats.parseNameLFMWFWM(curLine,
							excludeCompany, false, true);
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
				if (nameFormat != 1 
					|| !names[2].equals(names[5]) && names[2].length() > 0 && names[5].length() > 0){
					names = NameCleaner.lastNameSwap(names);
					
				}
				types = GenericFunctions.extractAllNamesType(names);
				otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractAllNamesSufixes(names);
				maiden = NameFactory.getInstance().extractMaiden(names);
				maiden = NameCleaner.removeUnderscore(maiden);
			} 
			names = NameCleaner.removeUnderscore(names);

			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
					NameUtils.isCompany(names[2], excludeCompany, true), NameUtils
							.isCompany(names[5], excludeCompany, true), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		/*
		 * done multiple owner parsing
		 */
		}

	protected static String addAmpersandIfNecessary(String nameLine) {
		if (!nameLine.contains("&")) {
			String[] tokens = nameLine.trim().split("\\s+");
			if (tokens.length >= 3) {// e.g. "SMITH JOHN WALTER JR ATHENA M" for pid 05917 009 000
				String lastTkn = tokens[tokens.length - 1];
				String lastButOneTkn = tokens[tokens.length - 2];

				// if the last or last but one token is first name then add an ampersand before it
				if (tokens.length >= 4 && lastButOneTkn.length() > 1
						&& (FirstNameUtils.isFemaleName(lastButOneTkn) || FirstNameUtils.isMaleName(lastButOneTkn))) {
					nameLine = nameLine.trim().replaceFirst("\\s*\\b" + lastButOneTkn + "\\s+" + lastTkn, " & " + lastButOneTkn + " " + lastTkn);
				} else if ((lastTkn.length() > 1 && (FirstNameUtils.isFemaleName(lastTkn) || FirstNameUtils.isMaleName(lastTkn)))) {
					nameLine = nameLine.trim().replaceFirst(lastTkn + "$", " & " + lastTkn);
				}
			}
		}
		return nameLine;
	}
	
	public static String cleanCompanyName(String s){
		s = s.replaceAll("^AGENT FOR\\s+", "");
		s = s.replaceFirst("(LIFE ES(\\w+)?)\\s*&\\s*\\w+$", "$1");
		return s;
	}
	public static String cleanNameFLAlachuaTR(String s){
		s = s.replaceFirst("SUCCESSOR", "");
		s = s.replaceFirst("\\s+OR\\s+", " & ");
		s = s.replaceFirst("\\s+GDN\\b", "");
		s = s.replaceAll("\\s+HEIRS\\s*", "");
		s = s.replaceFirst("BETSY LOU", "BETSY_LOU");
		s = s.replaceFirst("(?i)\\s*\\bH/W\\b\\s*", "");
		return s.trim();
	}
	
	public static void legalFLAlachuaTR(ResultMap m, String legal) throws Exception{
		// initial corrections and cleanup of legal description
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		// convert roman numbers
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); 
		
		legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		legal = legal.replaceAll("\\bCO-OP\\b", "");
		legal = legal.replaceAll("\\b\\d+(ST|ND|RD|TH) ADD( TO)?\\b", "");
		legal = legal.replaceAll("\\b(THE )?[NWSE]{1,2}(LY)? [\\d\\./\\s]+(\\s*\\bFT)?(\\s*\\bOF)?\\b", "");
		legal = legal.replaceAll("(?is)\\bUN(?:ITS?)?\\b(?:\\s+NO.?)?\\s+([\\dA-Z-]+)", "UNIT_$1 ");
		legal = legal.replaceAll("(\\b(?:R/)?[NSEW](?:[\\d\\s&\\.]/?(?:FT|OF|DEG)?)+)","");
		legal = legal.replaceAll("\\bFT( OF)?\\b", "");
		legal = legal.replaceAll("\\bRESUB( OF)?\\b", "");
		legal = legal.replaceAll("\\b(\\d+(ST|ND|RD|TH) )?REPLAT( OF)?\\b", "");
		legal = legal.replaceAll("\\b(REVISED|AMENDED) PLAT( OF)?\\b", "");
		legal = legal.replaceAll("(?is)&?\\s*\\b(?:UNDIVIDED|COMMON ELEMENTS)\\b\\s+(?:\\bINT\\b(?:\\s+\\bIN\\b)?)?", " ");
		legal = legal.replaceAll(",\\s*SUBJ TO [^,]+,?", "");
		legal = legal.replaceAll("\\b(\\d+)\\s*([A-Z])&([A-Z])\\b", "$1$2/$1$3");
		legal = legal.replaceAll("\\bS/D\\b", "SUBDIV");
		legal = legal.replaceAll("AKA \\d+\\s*","");
		legal = legal.replaceAll("\\bORD.+FKA\\s*MILL\\s*POND","");
		legal = legal.replaceAll("TOGETHER WITH EASEMENT ","");
		legal = legal.replaceAll(" DEED APPEARS IN ERROR","");
		legal = legal.replace(",", "");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		  String legalTemp = legal;

		  	// extract lot from legal description
		  String lot = ""; // can have multiple occurrences
		  Pattern p = Pattern.compile("\\b((?:LOT)S?|(?:LT)S?)\\s*((?:\\d+[A-Z]?[-\\s,&]+)+|(?:[A-Z]?[\\d\\s,&-]+(?:[A-Z]?\\d+)+)+|[\\dA-Z]+)\\s*");
		  Matcher ma = p.matcher(legal);
	  	  while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		  }
		  lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		  if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		  }
		  legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		  legal = legalTemp;

		  	// extract S/T/R
		  List<List<String>> bodyPis = new ArrayList<List<String>>();
		  p = Pattern.compile("SEC (\\d+(?:/\\d+)*) T (\\d+(?:\\s?[NSEW])?) R (\\d+(?:\\s?[NSEW])?(?:/\\d+)?)");
		  ma = p.matcher(legal);
		  while (ma.find()) {
			  	String sec = ma.group(1).replace("/", " ");
			  	String twn = ma.group(2).replaceAll("\\s+", "");
			  	String rng = ma.group(3).replaceAll("\\s+", "");
			  	List<String> line = new ArrayList<String>(3);
			  	line.add(sec);
			  	line.add(twn);
			  	line.add(rng);
			  	bodyPis.add(line);
			  	legalTemp = legalTemp.replaceFirst(ma.group(0), "SEC ");
		  }
		  p = Pattern.compile("\\bSEC(?:TIONS?|S)?\\s*(?:([\\d\\s,&]+)[-](\\d+)[-](\\d+))");
		  ma = p.matcher(legal);
		  while (ma.find()) {
			  String sec = ma.group(1).replace(" & ", " ");
			  String twn = ma.group(2).replaceAll("\\s+", "");
			  String rng = ma.group(3).replaceAll("\\s+", "");
			  List<String> line = new ArrayList<String>(3);
			  line.add(sec);
			  line.add(twn);
			  line.add(rng);
			  bodyPis.add(line);
			  legalTemp = legalTemp.replaceFirst(ma.group(0), "SEC ");
		  }
		  legal = legalTemp;

		  if (bodyPis.size() == 1) {
			  List<String> line = bodyPis.get(0);
			  m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), line.get(0));
			  m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), line.get(1));
			  m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), line.get(2));
		  } 
		  else 
			  if (bodyPis.size() > 1) {
				  	String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };
				  	Map<String, String[]> map = new HashMap<String, String[]>();
				  	map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });
				  	map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
				  	map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });
				  	ResultTable pis = new ResultTable();
				  	pis.setHead(header);
				  	pis.setBody(bodyPis);
				  	pis.setMap(map);
				  	m.put("PropertyIdentificationSet", pis);
			  }

		  	// extract plat book & page from legal description
		  p = Pattern.compile("\\b(?:PB|BK|CB)\\s*((?:[A-Z]-)?\\d+)\\s*PG\\s*(\\d+)-?(?:\\d+)?([\\s\\d]+)?\\b");
		  ma.usePattern(p);
		  if (ma.find()) {
			  	m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ma.group(1));
			  	m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ma.group(2)+ ma.group(3));
				legalTemp = legalTemp.replaceFirst(ma.group(0), "PL ");
		  } else {
			ma.reset();
			p = Pattern.compile("\\b(?:PB|BK|CB)\\s*([A-Z\\d]+)\\s*-\\s*(\\d+)\\b");
			ma.usePattern(p);
			if (ma.find()) {
			  	m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ma.group(1));
			  	m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ma.group(2));
			}
		  }
		  legal = legalTemp;

		  	// extract block from legal description
		  String block = "";
		  p = Pattern.compile("\\bB(?:K|LK?|LOCKS?)\\s(([\\d\\s,&-]+|[-A-Z](?:\\d+-?[A-Z](?:\\d+)?)?[\\s,])+)");
		  ma = p.matcher(legal);
		  while (ma.find()) {
			  block = block + " " + ma.group(1);
			  legalTemp = legalTemp.replaceFirst(ma.group(0), " BLK ");
		  }
		  block = block.replaceAll("\\s*&\\s*", " ").trim();
		  if (block.length() != 0) {
			  block = LegalDescription.cleanValues(block, false, true);
			  m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		  }
		  legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		  legal = legalTemp;

		  	// extract unit from legal description
		  p = Pattern.compile("\\b(UNIT(?:S|\\s*NO\\.?)?)\\s*_?((?:[\\d\\s&]+[-]?[A-Z]?(?:[\\d,]+)?\\s)+|[A-Z/\\d]+)");
		  ma = p.matcher(legal);
		  String tmp_unit="";
		  while (ma.find()) {
			  tmp_unit += ma.group(2).replaceAll("\\s*&\\s*"," ");
			  legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1)+" ");
			  legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			  legal = legalTemp;
		  }
		  if (tmp_unit!= "")
		  {
			  //tmp_unit = StringFormats.RemoveDuplicateValues(tmp_unit);
			  tmp_unit = LegalDescription.cleanValues(tmp_unit, false, true);
			  m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), tmp_unit);
		  }
		  	// extract building #
		  p = Pattern.compile("\\b(B(?:UILDING|LDG?))\\s*([A-Z]|\\d+)\\b");
		  ma = p.matcher(legal);
		  if (ma.find()) {
			  legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			  m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), ma.group(2));
			  legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			  legal = legalTemp;
		  }

		  	// extract phase from legal description
		  p = Pattern.compile("\\b(PH(?:ASES?)?)\\s*([\\d,\\s&-]+[A-Z]?\\s|(?:[-A-Z,\\d])+)");
		  ma = p.matcher(legal);
		  if (ma.find()) {
			  legalTemp = legalTemp.replaceFirst(ma.group(0).trim(), ma.group(1) + " ");
			  m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), ma.group(2).replaceAll("\\s*&\\s*"," "));
			  legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			  legal = legalTemp;
		  }

		     //extract Tract from legal description
		  p = Pattern.compile("\\b(TR(?:ACTS?)?)\\s*((?:(?:(?:\\d+)?[A-Z](?:\\d+)?|\\d+)[\\s,&-]+)+)");
		  ma = p.matcher(legal);
		  if (ma.find())
		  {
			  legalTemp = legalTemp.replaceFirst(ma.group(0).trim(), ma.group(1) + " ");
			  m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(2).replaceAll("\\s*&\\s*"," "));
			  legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			  legal = legalTemp;
		  }
		  	// extract section from legal description
		  /*   \b(SEC(?:TIONS?|S)?)\s*(\d+\s*&\s*(\d+[-](\d+)[-](\d+)))   am cazuri de forma SEC xx & xx-yy-zz, unde yy=TWP si zz=RANGE
		   * (SEC(?:TIONS?|S)?)\s*(?:([\d\s,&]+)[-](\d+)[-](\d+))   -> pt cazuri de genul xx-xx-xx si xx & xx-yy-zz
		  */
		  p = Pattern.compile("\\b(SEC(?:TIONS?|S)?)\\s*((?:[\\d\\s,]+\\s)+)");  
		  ma = p.matcher(legal);
		  if (ma.find())
		  {
			  legalTemp = legalTemp.replaceFirst(ma.group(0),ma.group(1) + " ");
			  m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),ma.group(2));
			
	  	      legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			  legal = legalTemp;
		  }
		  	// extract cross refs from legal description
		  List<List> bodyCR = new ArrayList<List>();
		  p = Pattern.compile("\\b(OR|DB)\\s+([A-Z]-\\d+|\\d+(?:-\\d+)?)(\\s?/\\s?\\d+(?:-\\d+)?)\\b");
		  ma = p.matcher(legal);
		  while (ma.find()) {
			  List<String> line = new ArrayList<String>();
			  line.add(ma.group(2));
			  String tmp = (ma.group(3)).replace("/", " ");
			  line.add(tmp);
			  String type = ma.group(1);
			  if (!"DB".equals(type)) {
				  type = "";
			  }
			  line.add(type);
			  bodyCR.add(line);
			  legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		  }
		
		  if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "Book_Page_Type" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("Book_Page_Type", new String[] { "Book_Page_Type", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
		  }
		  legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		  legal = legalTemp;
		
	      legal = "@" + legal;    
		  legal = legal.replaceAll("LT AND LTS", "LT");
		  legal = legal.replaceAll("RG\\s*((?:[-/A-Z\\s]+?[\\d\\s]+)+)","");
		  legal = legal.replaceAll("OR LESS OR", "OR");
		  legal = legal.replaceAll("(OR )+", "OR ");
		  legal = legal.replaceAll("LOTS?\\s*AND\\s*LOTS?","");
		  legal = legal.replaceAll("OR\\s*&\\s*OR","");
		  legal = legal.replaceAll("\\bDEG \\d+ MIN \\d+\\b", "");
		  legal = legal.replaceAll("SHEET [0-9A-Z]+", "");
		  legal = legal.replaceAll("\\bCASE \\d+(-\\d+)*\\b", "");	
		  legal = legal.replaceAll("\\bCP-\\d+(-\\d+)*\\b", "");		
		  legal = legal.replaceAll("\\bSUBDIV\\b", "S/D");
		  legal = legal.replaceAll("\\bPB\\s*(?:\\d+|[-/A-Z\\d]+)", "PB");
		  legal = legal.replaceAll("((?:(?:ALSO\\s*)?\\bCOM\\b\\s*(?:INT)?)?(?:\\b[NSEW]{2}\\s)?(?:\\bCOR\\b)?)","");
		  legal = legal.replaceAll("DEG\\s*", "");
		  legal = legal.replaceAll("SEC", "");
		  legal = legal.replaceAll("POB", "");
		  legal = legal.replaceAll(" OR", "");
		  legal = legal.replaceAll("\\((.+)\\)","$1");
		  legal = legal.replaceAll("(?:ALONG\\s*[NSEW]?\\s*)?(?:[A-Z]/[A-Z]\\s*(?:TO\\s*SLY\\s*)?)?LINE[\\d\\.\\s]+?","");
		  
		  String subdiv = legal.replaceAll("(.+?)\\s*(?:PH(?:ASES?)?|UNITS?|PB|LOTS?).*", "$1");
		  
		  legal = legal.replaceAll("(.+?)\\s*(?:PH(?:ASES?)?|UNITS?|PB|LOTS?).*", "$1");		  
		  legal = legal.replaceAll("\\b(LOT|LT|LTS|OR|DB|PB|BLK|PH|BLDG|UNITS?|PH(ASE)?|PL|SEC|TRACT|POB)\\b", "@");
		  legal = legal.replaceAll("CA \\d+", "@");
		  legal = legal.replaceAll("@ RE S/D OF @", "@");
		  legal = legal.replaceAll("@ AND [^@]+ OF @", "@");
		  legal = legal.replaceAll("^S\\d+/\\d+ OF @", "@");
		  legal = legal.replaceAll("\\(\\d+\\)\\s+", "");
		  legal = legal.replaceAll("LESS MINERAL RIGHTS", " ");
		  legal = legal.replaceAll("(&|ALSO)? ((\\d+/\\d+)|(\\d*\\.\\d+%))? (INT IN)? COMMON (ELEMENTS|AREA)", " ");
		  legal = legal.replaceAll("@\\s*(AND|&) (ALL)?@", "@");
		  legal = legal.replaceAll("\\s{2,}", " ");
		  legal = legal.replaceAll("TRUSTEES FOR[^@]+@", "@");
		  legal = legal.replaceAll("AN UNDIV (SHARE OF)?","@");
		  legal = legal.replaceAll("(AT )?MILL (POND)?","");
		  legal = legal.replaceAll("PER (CITY OF)?","");
		  
		  legal = (" " + legal).replaceAll("( @)+", " @").trim();

		  //String subdiv = "";
		  //p = Pattern.compile("^@ ([^@]+) @$");
		  p = Pattern.compile("(?:(.+[^@])\\s*@)?(.+[^@])\\s*@");
		  ma = p.matcher(legal);
		  if (ma.matches()) {
			  if (ma.group(1)!= null)
				  subdiv = ma.group(1) + ma.group(2);
			  else 
				  subdiv = ma.group(2);
			subdiv = subdiv.replaceAll("&", "");
		  }
		  subdiv = subdiv.replaceAll("@","");
		  subdiv = subdiv.replaceAll("\\s{2}", "");
		  if (subdiv.length() != 0) {
			subdiv = subdiv.replaceAll("(.+) PL\\s+ALSO.*", "$1").trim();
			subdiv = subdiv.replaceAll("(RE )?S/D", "").trim();
			subdiv = subdiv.replaceAll("ADDN\\s*.*", "").trim();
			subdiv = subdiv.replaceAll("(LESS\\s*)?[0-9](ST|ND|TH) ADDN( TO)?", "").trim();
			subdiv = subdiv.replaceFirst("(.+) CONDO?(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceFirst("^\\d+\\s", "");
			if ("LESS".equals(subdiv)) {
				subdiv = "";
			}
			//subdiv = subdiv.replaceFirst("^OF ", "");
			//subdiv = subdiv.replaceFirst("(.+)\\s*OF\\b","$1");
			subdiv = subdiv.replaceAll("-*(OF)?\\s*(\\b(ALSO\\s+)?(THE\\s*)*)?\\s*(OF|ALSO)?\\s*\\b([NEWS]|NE|NW|SE|SW)\\s*[\\d/-]+\\s*(\\bLESS\\b|(RIVER\\s+&?\\s*)?CR\\b\\s\\d*(TH|RD|ST)?\\s*)?","");
			subdiv = subdiv.replaceFirst("(AKA\\s+)?PARCEL(\\s+[\\d-/]+)", "");
			subdiv = subdiv.replaceFirst("\\(?LESS(\\s*\\d*\\s*\\b(CR)?\\b\\s*)", "");
			if (StringUtils.isNotEmpty(subdiv)) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
			}
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*")){
				m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		  }
		  if (legal.contains("@"))
			  legal = legal.replaceAll("@", "");
	  	}
	  
		/**
		 * Split names line Davis & smith & last each on lines
		 * @param lines - strings of names
		 */
		public static String[] splitNameAlachua(String line){
			//JESS E SMITH & BETSY ARNOLD
			//WRIGHT, JAMES & SMITH, NELSON
			String[] tmps = {line};
			tmps = line.split("\\s*&\\s*");
			int i;
			for (i = 0; i<tmps.length; i++){
				String[] suffixes = GenericFunctions.extractSuffix(tmps[i].trim());
				if (!suffixes[0].matches("(\\w|-)+")){
					break;
				}
			}
			if (i != tmps.length){
				tmps = new String[1];
				tmps[0] = line;
			}
			return tmps;
		}
		
		/**
		 * Split names line JESS E SMITH & BETSY ARNOLD in two lines
		 * @param lines - strings of names
		 */
		public static String[] splitNameAlachua(String[] lines, Vector<String> excludeCompanyWords){
			
			Vector<String> tmpLines = new Vector<String>();
			String[] tmpL;
			for (String s: lines){
				if (!NameUtils.isCompany(s, excludeCompanyWords, false)){
					String[] tmps = splitNameAlachua(s);
					for( String s1:tmps){
						tmpLines.add(s1);
					}
				} else {
					tmpLines.add(s);
				}
			}
			tmpL = new String[tmpLines.size()];
			for(int i = 0; i< tmpLines.size(); i++){
				tmpL[i] = tmpLines.get(i);
			}
			return tmpL;
		}			
			  
}

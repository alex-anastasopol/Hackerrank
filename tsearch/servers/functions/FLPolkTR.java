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
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
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

public class FLPolkTR 
  {
	public static void extractLegalElemsFLPolk(String legal, ResultMap m) throws Exception {
		
		legal = legal.replaceAll("\\bPG PG(?= \\d)", "PG");
		legal = legal.replaceAll("\\b(PB \\d+) PB(?= \\d)", "$1 PG");
		legal = legal.replaceAll("\\b(PB \\d+) &G(?= \\d)", "$1 PG");
		legal = legal.replaceAll("\\b[SWEN]{1,2}(LY)? (SIDE|LINE|ALONG)( OF)?( [SWEN]{1,2})?\\b", "");				
		legal = legal.replaceAll("\\b[SWEN]{1,2}(LY)? \\d+PT\\d+( FT)?( OF)?\\b", "");	
		legal = legal.replaceAll("\\b\\d+(/\\d+(ST|ND|RD|TH)?|%) INT\\b", "");
		legal = legal.replaceAll("\\s(THRU|THUR)\\s", "-");
		legal = legal.replaceAll("(?<=\\d )TO(?= \\d)", "-");
		legal = legal.replaceAll("\\s*-\\s*", "-");
		legal = legal.replaceAll("\\b(NO|NUMBER)\\.?\\b", "");
		legal = legal.replace("#", "");
		legal = legal.replaceAll("\\b(A )?PT OF\\b", "");
		legal = legal.replaceAll("\\b([SWEN]{1,2}(LY)? )?\\d+ DEG \\d+ MIN \\d+ SEC\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2}(LY)?\\s*(?![\\d\\s/&]+TP?\\s*\\d+[SWEN]? RG?\\s*\\d+[SWEN]?)\\s*\\d+( \\d+)?([/\\.]\\d+)?(?!\\s*RG?\\s*\\d+[SWEN]?\\b)(\\s*\\bFT( [SWEN]{1,2})?)?(\\s*\\bOF)?\\b", "");
		legal = legal.replaceAll("\\b\\d+(\\.\\d+)? FT( OF)?\\b", "");
		legal = legal.replaceAll("\\s*[&,]\\s*", " ");
		legal = legal.replaceAll("\\bAND\\b", " ");
		legal = legal.replaceAll("\\bALL\\b", " ");
		legal = legal.replaceAll("\\bA/K/A\\b", " ");
		legal = legal.replaceAll("\\b(\\d+)(?:ST|ND|RD|TH) (UNIT)\\b(?! \\d)", "$2 $1");
		legal = legal.replaceAll("\\s{2,}", " ").trim();
		   
		
		List<List> body = new ArrayList<List>();			   
		// extract plat book & page from legal description
		String bpPatt = "[\\s-]?(\\d+[A-Z]?)(?: PGS?[-\\s]?|-)(\\d+(?:-?[A-Z])?(?:[/\\s-]*\\d+[A-Z]?)*)\\b(?!/)";
		Pattern p = Pattern.compile("\\bPB" + bpPatt);
		Matcher ma = p.matcher(legal);
		while (ma.find()){ 
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			String page = ma.group(2);
			page = page.replaceFirst("\\b(\\d+) \\d+/\\d+\\b.*", "$1");
			page = page.replace('/', '-');
			String[] pages = page.split("-|\\s+");
			if (pages.length == 2) {
				if (pages[0].matches("\\d+") && pages[1].matches("\\d+")) {
					int page1 = Integer.parseInt(pages[0]);
					int page2 = Integer.parseInt(pages[1]);
					if (page1 + 1 == page2) {
						page = pages[0];
					}
				}
			} else if (pages.length > 2) {
				page = StringFormats.ReplaceEnumerationWithInterval(page);
			}
			line.add(page);
			body.add(line);
			legal = legal.replace(ma.group(0), "");
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
		
		// extract condominium plat book & page from legal description
		body = new ArrayList<List>();
		p = Pattern.compile("\\b(?:CONDO? BK|C\\s?B)" + bpPatt);
		ma = p.matcher(legal);
		while (ma.find()){
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			String page = ma.group(2);
			page = page.replaceFirst("\\b(\\d+) \\d+/\\d+\\b.*", "$1");
			page = page.replace('/', '-');
			line.add(page);
			body.add(line);
			legal = legal.replace(ma.group(0), "");
		}
		if (!body.isEmpty()){
			String [] header = {"CondominiumPlatBook", "CondominiumPlatPage"};			   
			Map<String,String[]> map = new HashMap<String,String[]>();
			map.put("CondominiumPlatBook", new String[]{"CondominiumPlatBook", ""});
			map.put("CondominiumPlatPage", new String[]{"CondominiumPlatPage", ""});			   
			ResultTable pis = new ResultTable();	
			pis.setHead(header);
			pis.setBody(body);
			
			ResultTable pisPlat = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisPlat != null){
				pis = ResultTable.joinHorizontal(pis, pisPlat);
				map.putAll(pisPlat.getMapRefference());
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}
		   	 
		// extract sec-twn-rng from legal description			
		body = new ArrayList<List>();
		p = Pattern.compile("\\b(?:SEC(?:TION)?S? |S\\s*)?(\\d+(?:[\\s/]+\\d+)*) (?:TOWNSHIP |TWN |TP?\\s*)(\\d+[SWNE]?) (?:RANGE |RNG |RG?\\s*)(\\d+[SWNE]?)\\b");
		ma.usePattern(p);
		ma.reset();   
		while (ma.find()){
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replace('/', '-'));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line); 
			legal = legal.replace(ma.group(0), "");
		}
		p = Pattern.compile("\\bSEC(?:TION)?S? (\\d+(?:[\\s/]+\\d+)?) (\\d+[SWNE]?) (\\d+[SWNE]?)\\b");
		ma = p.matcher(legal);   
		while (ma.find()){
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replace('/', '-'));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
			legal = legal.replace(ma.group(0), "");
		}
		// extract section from legal description
		p = Pattern.compile("\\bSEC(?:TION)?S? (?!\\d+(?: \\d+)*\\sT\\d+)(\\d+(?:\\s\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()){
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
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
			
			ResultTable pisPlat = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisPlat != null){
				pis = ResultTable.joinHorizontal(pis, pisPlat);
				map.putAll(pisPlat.getMapRefference());
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}	 
		
		// extract lot from legal description
		String lotPattern = "\\bLOTS?[\\s-]((?:\\d+[A-Z]?|[A-Z]{1,2}-?\\d+|[A-Z])\\b(?:\\s*-?\\s*(?:\\d+|\\b[A-Z]{1,2}-?\\d+|\\b[A-Z])\\b)*)\\b(?!/)";			
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile(lotPattern);
		ma = p.matcher(legal);
		while (ma.find()){
			lot = lot + " " + ma.group(1).replaceFirst("[\\s-]+$", "");
			legal = legal.replace(ma.group(0), "");
		}
		lot = lot.trim();
		if (lot.length() != 0){
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		   	   	   	   
		// extract block from legal description
		String block = "";
		p = Pattern.compile("\\bBL(?:OC)?KS? ([A-Z]{1,2}|\\d+(?: \\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()){
			block = block + " " + ma.group(1);
			legal = legal.replace(ma.group(0), "");
		}
		block = block.trim();
		if (block.length() != 0){
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}	     	   	  	   
		   
		// extract unit from legal description
		p = Pattern.compile("\\b(?:UNITS?|APT|APARTMENT) (\\d+(?: \\d+)*|[A-Z]{1,2}(?:-?\\d+(?:-?[A-Z])?)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1));
			legal = legal.replace(ma.group(0), "");
		}
	   	   	   
		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPHASES? (\\d+(?:-?[A-Z]\\d*)?(?: \\d+)*|[A-Z]|([IVX]+[A-Z]?(?:\\s*&\\s*[IVX]+)*))\\b");
		ma = p.matcher(legal);
		while (ma.find()){
			String phaseTemp = ma.group(1);
			phaseTemp = phaseTemp.replaceAll("\\s*&\\s*", " ");
			phaseTemp = phaseTemp.replaceAll("\\b([IVX]+)", "$1 --"); 
			phaseTemp = Roman.normalizeRomanNumbers(phaseTemp);
			phaseTemp = phaseTemp.replaceAll(" --", "");
			phase = phase + " " + phaseTemp;
			legal = legal.replace(ma.group(0), "");
		}
		phase = phase.trim();
		if (phase.length() != 0){
			phase = LegalDescription.cleanValues(phase, false, true);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}
	   
		// extract building #
		p = Pattern.compile("\\bBLDG ([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			legal = legal.replace(ma.group(0), "");
		}
		
		// extract tract 
		String tract = "";  
		p = Pattern.compile("\\bTR(?:ACTS?)? (\\d+( \\d+)*|[A-Z](?: [A-Z])*(?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()){
			tract = tract + " " + ma.group(1);	
			legal = legal.replace(ma.group(0), "");
		}
		tract = tract.trim();
		if (tract.length() != 0){
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}			   		   		   
	}	
	
	public static void legalTokenizerLEGALFLPolkRV(ResultMap m, String legal) throws Exception {
		
		//initial corrections and cleanup of legal description	
		legal = legal.replaceAll("\\bPLAT BOOK-(\\d+) PAGE-(\\d+)\\b", "PB $1 PG $2");
		legal = legal.replaceAll("\\bPLAT BOOK\\b", "PB");			
		legal = legal.replaceAll("\\bPG (\\d+ PGS? \\d)", "PB $1");	
		legal = legal.replaceAll("\\bTW0\\b", "TWO");
		legal = legal.replaceAll("\\bLESS\\b", "");
		legal = legal.replaceAll("\\bR/W\\b", "");
		legal = legal.replace("---", "");
		  	   
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		legal = legal.replaceAll("\\b5 (ADD(?:ITION)?)\\b", "V $1");
		
		extractLegalElemsFLPolk(legal, m);
	}  	 
	
	   public static void extractCrossRefsFLPolkTR(ResultMap m, String legalTemp) throws Exception {
		   
		   // extract cross refs from legal description
		   List<List> bodyCR = new ArrayList<List>();
		   Pattern p = Pattern.compile("\\bOR\\s*(\\d+)(?:-|\\s+PGS?\\s+)(\\d+)\\b");
		   Matcher ma = p.matcher(legalTemp);      	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();			   
			   line.add(ma.group(1));
			   line.add(ma.group(2));
			   line.add("OR");
			   bodyCR.add(line);
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
	   }

	   public static String legalCleanupFLPolkTR2(String legalTemp){
		   String lotPattern = "\\bLOTS? ((?:\\d+|[A-Z]-?\\d+|[A-Z])\\b(?:\\s*-?\\s*(?:\\d+|\\b[A-Z]-?\\d+|\\b[A-Z])\\b)*)";
		   legalTemp = legalTemp.replaceAll(lotPattern, ""); 
		   legalTemp = legalTemp.replaceAll("\\bGOV\\b", "");
		   legalTemp = legalTemp.replaceFirst("^BEG\\b.+", "");
		   legalTemp = legalTemp.replaceFirst("^[SWEN]{1,2}\\s*\\d+.+", "");
		   legalTemp = legalTemp.replaceFirst("^ALL THAT PART .*", "");
		   legalTemp = legalTemp.replaceAll("\\(.+\\)", "");
		   legalTemp = legalTemp.replaceAll("\\b(OF )?UNRE(C(ORDED)?)?\\b", "");
		   legalTemp = legalTemp.replace("*****DEED APPEARS IN ERROR*****", "");
		   legalTemp = legalTemp.replaceAll("\\s{2,}", " ").trim();
		   return legalTemp;
	   }
	   

	   public static void legalTokenizerFinalFLPolkTR(ResultMap m, String legal) throws Exception {
		   legal = legalCleanupFLPolkTR(legal);
		   
		   // extract the subdivision name
		   // first perform additional cleaning needed only for subdivision parsing
		   String legalTemp = legalCleanupFLPolkTR2(legal);
		   		   
		   String subdiv = "";
		   Pattern p = Pattern.compile("^(.*?)\\s*-?\\s*\\b(PB|PHASE|UNIT|(RE)?SUB|(\\d+(ST|ND|RD|TH) )?ADD(ITION)?|NEIGHBORHOOD \\d+|VILLAGE \\d+|DESC|CONDO(MINIUM)?)\\b");
		   Matcher ma = p.matcher(legalTemp);
		   if (ma.find()){
			   subdiv = ma.group(1);		   
		   } else {
			   p = Pattern.compile("^(.+?\\s*\\bVILLAGE)\\b.*"); //PID 072827-821601-001970
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   subdiv = ma.group(1);
			   }
		   }
		   
			String address = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpAddress"));
			if (!address.isEmpty() && subdiv.contains(address)) {
				subdiv = subdiv.replaceFirst(address, "").trim().replaceAll("\\s{2}", " ");
			}
			
		   if (subdiv.length() != 0){
			   if (subdiv.matches("\\w+ CI?TY")){
				   m.put("PropertyIdentificationSet.City", subdiv.replaceFirst("(\\w+) CI?TY", "$1"));
			   } else {
				   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				   if (legal.matches(".*\\bCONDO(MINIUM)?\\b.*"))
					   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }
		   }		   	   	   	   	 
		   extractLegalElemsFLPolk(legal, m);
		   	   	 	   	   
		   extractCrossRefsFLPolkTR(m, legalTemp);		   	   	   
	   }
	   
	   public static void legalTokenizerIntermediateFLPolkTR(ResultMap m, String legal) throws Exception {		   
		   legal = legalCleanupFLPolkTR(legal);
		   
		   String legalTemp = legalCleanupFLPolkTR2(legal);
		   		   	   	   	   	   	 
		   extractLegalElemsFLPolk(legal, m);
		   	   	 	   	   
		   extractCrossRefsFLPolkTR(m, legalTemp);		   	   	   
	   }
	    
	   public static String legalCleanupFLPolkTR(String legal){
		   
		   legal = legal.replaceFirst("(?is)\\bLegal\\s+Description\\b", "");
		   
		   //initial corrections and cleanup of legal description	
		   legal = legal.replaceAll("\\bBL K ([A-Z]\\b|\\d)", "BLK $1");
		   legal = legal.replaceAll("\\bP (GS?) (?=\\d)", "P$1 ");
		   legal = legal.replaceAll("\\bPG S (?=\\d)", "PGS ");
		   legal = legal.replaceAll("\\bP B (?=\\d+)", "PB ");
		   legal = legal.replaceAll("\\b([TR]\\d+) ([SWNE])\\b", "$1$2");
		   legal = legal.replaceAll("\\bUN IT\\b", "UNIT");
		   legal = legal.replaceAll("\\bU NIT\\b", "UNIT");
		   legal = legal.replaceAll("\\bL (OTS?)\\b", "L$1");
		   legal = legal.replaceAll("\\bLO (TS?)\\b", "LO$1");
		   legal = legal.replaceAll("\\bLOT S (\\d+\\s*[-&])", "LOTS $1");
		   legal = legal.replaceAll("\\b(LOT \\d+) (0)\\b", "$1$2");
		   legal = legal.replaceAll("\\b(LOTS?) (\\d+) \\2\\b", "$1 $2$2");
		   legal = legal.replaceAll("\\bSE C (?=\\d)", "SEC ");
		   legal = legal.replaceAll("\\bS EC (?=\\d)", "SEC ");
		   legal = legal.replaceAll("\\bF LA\\b", "FLA");
		   legal = legal.replaceAll("\\bACRE S\\b", "ACRES");
		   legal = legal.replaceAll("\\bVI LLAGE\\b", "VILLAGE");
		   legal = legal.replaceAll("\\bPH ASE", "PHASE");
		   legal = legal.replaceAll("\\bPLAT BOOK-(\\d+) PAGE-(\\d+)\\b", "PB $1 PG $2");
		   legal = legal.replaceAll("\\bPLAT BOOK\\b", "PB");
		   legal = legal.replaceAll("\\bPB (\\d+) (\\d+ PGS?\\b)", "PB $1$2");
		   legal = legal.replaceAll("\\bPG (\\d+ PGS? \\d)", "PB $1");
		   legal = legal.replaceAll("\\b(\\d+-)(\\d+) \\2\\b", "$1$2$2");
		   legal = legal.replaceAll("\\b(T\\d+ R\\d+) (\\d+)\\b", "$1$2");
		   legal = legal.replaceAll("\\bCONDOMIN IUM\\b", "CONDOMINIUM");
		   legal = legal.replaceAll("\\bRE SORT\\b", "RESORT");
		   legal = legal.replaceAll("\\bL ESS\\b", "LESS");
		   legal = legal.replaceAll("\\bLESS\\b", "");
		   legal = legal.replaceAll("\\bR/W\\b", "");
		   legal = legal.replaceAll("\\bFT I N SEC\\b", "FT IN SEC");
		   legal = legal.replaceAll("\\bTW0\\b", "TWO");
		   legal = legal.replace("---", "");
		  	   
		   legal = GenericFunctions.replaceNumbers(legal);
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		   legal = legal.replaceAll("\\b5 (ADD(?:ITION)?)\\b", "V $1");	   	   	 
		   
		   legal = legal.replaceAll("\\s{2,}", " ").trim();
		   return legal;
	   }

	public static void parseAddress(ResultMap m, String address) {
		String tmpAdr = address;
		if (tmpAdr != null) {
			if (tmpAdr.matches("(?is)(.+) \\d+$")) {
				if (!tmpAdr.matches("(?is)(.+)\\bUN(?:IT)?\\s*(\\d+)$")) {
					tmpAdr = tmpAdr.replaceFirst("(?is)(.+) \\d+$", "$1").trim();
				} 
			}
			m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(tmpAdr.trim()));
			m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(tmpAdr.trim()));
		}
	}   
  }
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
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameNormalizer;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLStJohnsTR {
	public static int counter =0;
	public static Pattern LcommaFM = Pattern.compile("\\w+\\s*,\\s*\\w+\\s\\w+( " + GenericFunctions.nameSuffixString + ")?");
	public static Pattern FMi = Pattern.compile("\\w{2,} \\w");
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesFLStJohnsTR(ResultMap m, long searchId) throws Exception {
		String owner = (String) m.get("tmpOwnerAddress");
		if (StringUtils.isEmpty(owner)) {
			return;
		}		
		   //saving examples
		/*
			boolean save = false;
		   String o = owner;
		   String filename = "flstjohnstr";
		   String contain = ""; //if the same function is used for inter, then what substring should be in o to save it
	        if (m.get("saving") == null && save){
			      if (o.contains(contain)){  
			    	  String[] s = owner.split("@@");
			    	  //if (s.length>2 && (s[0].endsWith("&") || s[0].endsWith("AND") || s[1].matches("(&)?( )?\\w+ \\w+( \\w+)?( \\w+)?"))){
				        try {
				            FileWriter fw = new FileWriter(new File("/home/danut/Desktop/work/parsing/" + filename + "/" + filename + ".html"),true);
				             fw.write(o);
				            fw.write("\n==============================================================\n");
				            fw.close();
				        }
				        catch (IOException e) {
				            e.printStackTrace();
				        }
			    	  //}
			      }
		        
		        }		
		   // done saving
		*/
		ArrayList<String> lines = new ArrayList<String>();

		int i;
		String[] lTmp = owner.split("\\s{2,}");
		for(String s:lTmp){
			lines.add(s);
		}
		
		Vector<String> excludeCompany = new Vector<String>();
		excludeCompany.add("PAT");
		excludeCompany.add("ST");
		excludeCompany.add("SELF");
		
		Vector<String> extraCompany = new Vector<String>();
		extraCompany.add("JOHNS");
		extraCompany.add("FOREST");
		extraCompany.add("BLVD");
		
		Vector<String> extraInvalid = new Vector<String>();
		extraInvalid.add("CHESTER PK");
		extraInvalid.add(".*VILLAGE \\d+.*");
		extraInvalid.add(".*PMB \\d+.*");
			
		List<List> body = new ArrayList<List>();
		if (lines.size() <= 2)
			return;

		ArrayList<String> lines2 = GenericFunctions.removeAddressFLTR2(lines,
				excludeCompany, extraCompany, extraInvalid, 2, 100);
		
		lines2 = mergeNamesAndPreClean(lines2);
		for (i = 0; i < lines2.size(); i++) {
			String[] names = { "", "", "", "", "", "" };
			boolean isCompany = false;
			// 1 = "L, FM & WFWM"
			// 2 = "F M L"
			// 3 = "F & WF L
			int nameFormat = 1;
			// C/O - I want it before clean because I don't clean company names
			String ln = lines2.get(i);
			if (ln.matches("(?i).*?c/o.*")
					||ln.matches("^\\s*%.*")) {
				ln = ln.replaceAll("(?i)c/o\\s*", "");
				ln = ln.replaceFirst("^\\s*%\\s*", "");
				if (ln.contains("&") && !GenericFunctions.LFMWFWM.matcher(ln).matches()){
					nameFormat = 3;
				} else if (GenericFunctions.FMiL.matcher(ln).matches()){
					nameFormat = 2;
				} else {
					String[] tmpS = ln.split("( |" + NameCleaner.fixScotishLikeNames("-") + ")");
					if (NameFactory.getInstance().isLastOnly(tmpS[tmpS.length-1])
							|| (tmpS.length == 2 
								 && NameFactory.getInstance().isFirstMiddleOnly(tmpS[0])
								 && NameFactory.getInstance().isLast(tmpS[tmpS.length-1]))){
						nameFormat = 2;
					} 
				}
			}
			
			String curLine = NameCleaner.cleanNameAndFixNoScotish(ln, new Vector<String>(), true);
			if (NameUtils.isCompany(curLine, excludeCompany, true)) {
				// this is not a typo, we don't know what to clean in companies'
				// names
				names[2] = cleanCompanyName(ln);
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
				}
			}
		
			String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
			//String[] maiden = { "", ""};
			if (!isCompany) {
				names = NameCleaner.tokenNameAdjustment(names);
				suffixes = GenericFunctions.extractAllNamesSufixes(names);
				//maiden = NameFactory.getInstance().extractMaiden(names);
				names = NameCleaner.removeUnderscore(names);
				//maiden = NameCleaner.removeUnderscore(maiden);
			}
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			   
			   GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);

	}
	
	public static ArrayList<String> mergeNamesAndPreClean(ArrayList<String> a){
		if (a.size() == 0) return a;
		ArrayList<String> tmpS = new ArrayList<String>();
		tmpS.add(preCleanName(NameCleaner.fixScotishLikeNames(a.get(0))));
		for(int i=1; i<a.size(); i++){
			String s = preCleanName(NameCleaner.fixScotishLikeNames(a.get(i)));
			if (FMi.matcher(s).matches()){
				tmpS.set(i-1, tmpS.get(i-1) + " " + s);
			} else {
				tmpS.add(s);
			}
		}
		return tmpS;
	}

	public static String cleanCompanyName(String s) {
		s = NameCleaner.removeUnderscore(s);
		return s;
	}

	public static void stdFLStJohnsTR(ResultMap m, long searchId)
			throws Exception {
		String ownerAddr = (String) m.get("tmpOwnerName");
		if (StringUtils.isEmpty(ownerAddr))
			return;

		// separate owner of co-owner
		String[] lines = ownerAddr.split("\\s(?=%)");

		extractOwnersFLStJohnsTR(lines, m, 2);
	}

	@SuppressWarnings("rawtypes")
	protected static void extractOwnersFLStJohnsTR(String[] lines, ResultMap m, int minlines) throws Exception {
		   
		   String[] a = {"", "", "", "", "", ""};
		   String[] b = {"", "", "", "", "", ""};
		   String ownerCleaned = cleanOwnerFLStJohnsTR(lines[0]);
		   int lineForMailingAddress = 1;
		   if (NameUtils.isCompany(ownerCleaned))
			   a[2] = ownerCleaned;
		   else {		   
			   int idx = ownerCleaned.indexOf('&');
			   if (idx > 0){
				   String coowner = ownerCleaned.substring(idx + 1).trim();
				   coowner = StringFormats.unifyNameDelim(coowner, true);
				   // coowner from line1 may be specified with full name - e.g. 063390-0010 WILSON JEFFREY L,LINDA M CRANE
				   Pattern p = Pattern.compile("[A-Z]{2,} [A-Z]+ ([A-Z]{2,})");
				   Matcher ma = p.matcher(coowner);
				   if (ma.find()){
					   if(!NameNormalizer.isNameSuffix(ma.group(1))){
						   ownerCleaned = ownerCleaned.substring(0, idx).trim();
						   b = StringFormats.parseNameDesotoRO(coowner, true);					   
					   }
				   }
			   }
			   a = StringFormats.parseNameNashville(ownerCleaned, true);
			   if (b[2].length() != 0){
				   a[3] = b[0];
				   a[4] = b[1];
				   a[5] = b[2];
			   }			   
		   }
		   // extract co-owner from second line if not already extracted from the first line 
		   if ((a[5].length() == 0) && (lines.length >= minlines)){ 
			   String coowner = "";
			   boolean isFML = true;
			   if (lines[1].startsWith("%")){
				   coowner = lines[1];
				   int idx = coowner.indexOf(',');
				   if (idx > 0){
					   coowner = coowner.substring(0, idx);
					   isFML = false;
				   }
			   }
			   if (coowner.length() != 0){
				   if (isFML)
					   b = StringFormats.parseNameDesotoRO(cleanOwnerFLStJohnsTR(coowner), true);
				   else
					   b = StringFormats.parseNameNashville(cleanOwnerFLStJohnsTR(coowner), true); 
				   a[3] = b[0];
				   a[4] = b[1];
				   a[5] = b[2];
				   lineForMailingAddress++;
			   }		   
		   }
		   
		   m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
	       m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
	       m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
	       m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
	       m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
	       m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	       
	       List<List> body = new ArrayList<List>();
		   String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		   types = GenericFunctions.extractAllNamesType(a);
		   otherTypes = GenericFunctions.extractAllNamesOtherType(a);
		   suffixes = GenericFunctions.extractAllNamesSufixes(a);
		   
		   GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], types, otherTypes, 
					NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
		
		   GenericFunctions.storeOwnerInPartyNames(m, body, true);
	       
	   }
	   
	   protected static String preCleanName(String s){
		   s = s.toUpperCase();
		   s = s.replace("*", "");
		   s = s.replaceAll("____$", "");
	       //s = s.replaceAll("%", "");
	       if (!s.contains("&")){
		       if (LcommaFM.matcher(s).matches()){
		    	   s = s.replaceAll("\\s*,\\s*", " ");
		       } else {
		    	   s = s.replaceAll("\\s*,\\s*", " & ");
		       }
	       }
	       s = s.replaceFirst(" FE G$", " FE_G");
	       s = s.replaceFirst("\\b(DECD|DECEASED)\\b", "");
	       s = s.replaceAll("\\bMR\\b", "");
	       s = s.replaceAll("\\s{2,}", " ").trim();
	       
	       return s;
	   }
	   
	   protected static String cleanOwnerFLStJohnsTR(String s){
		   s = s.toUpperCase();
		   s = s.replace("*", "");
	       s = s.replaceAll("%", "");
	       s = s.replace(",", " & ");
	       s = s.replaceAll("\\s{2,}", " ").trim();
	       return s;
	   }
	   
	   public static void legalFLStJohnTRFinal(ResultMap m, long searchId) throws Exception {
		   // extract sec-twn-rng from geo number
		   String geo = (String) m.get("tmpGeoNumber");
		   
		   if (geo != null){
			   String sec = geo.replaceFirst("(\\d{2})\\d+-.+", "$1").replaceFirst("^0+(\\d+)", "$1");
			   String twn = geo.replaceFirst("\\d{2}(\\d{2})\\d+-.+", "$1").replaceFirst("^0+(\\d+)", "$1");
			   String rng = geo.replaceFirst("\\d{2}\\d{2}(\\d{2})-.+", "$1").replaceFirst("^0+(\\d+)", "$1");
			   m.put("PropertyIdentificationSet.SubdivisionSection", sec);
			   m.put("PropertyIdentificationSet.SubdivisionTownship", twn);
			   m.put("PropertyIdentificationSet.SubdivisionRange", rng);
		   }
		   String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");
		   
		   if (StringUtils.isEmpty(legal)){
			   String legal2 = (String)m.get("tmpProp"); 
			   if (StringUtils.isEmpty(legal2)){
				   return;
			   } else {
				   legal = legal2;
				   m.put("PropertyIdentificationSet.PropertyDescription", legal2);
			   }
		   }
		   legal = parseSTRFromBeginning(m, legal);
		   legalFLStJohnTR(m, legal);
	   }
	   
	   @SuppressWarnings("rawtypes")
	public static void legalFLStJohnTR(ResultMap m, String legal) throws Exception {
		   	  
		   //initial cleanup of legal description
		   legal = legal.replaceFirst("^\\d*[/\\.]?\\d+\\s*Acres\\s*", "");
		   legal = legal.replaceAll("\\bP(AR)?T( OF| IN)?\\b", "");
		   legal = legal.replaceAll("\\d*\\.?\\d+ X \\d*\\.?\\d+\\s*FT( OF)?\\b", "");
		   
		   
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   
		   
		   legal = legal.replaceAll("\\b[SWEN]{1,2}\\d*[/\\.]?\\d+(\\s*FT\\b)?(\\s*\\bOF)?\\b", "");
		   legal = legal.replaceAll("\\d*[/\\.]?\\d+\\s*FT(\\s*\\bOF)?\\b", "");
		   legal = legal.replaceAll("\\b(\\d+)(?:ST|ND|RD|TH) (PHASE|UNIT|BLDG|BLK)", "$2 $1");
		   legal = legal.replaceAll("\\s{2,}", " ").trim();
		   
		   // extract the subdivision name
		   // first perform additional cleaning needed only for subdivision parsing
		   String legalTemp = legal;
		   
		   legalTemp = legalTemp.replaceAll("\\([^\\)]*\\)", "");
		   legalTemp = legalTemp.replaceAll("\\s{2,}", " ").trim();
		   
		   String subdiv = "";
		   Pattern p = Pattern.compile("^(?:[\\d/\\s,&-]+[A-Z]?\\s)?\\b(.*?)\\s*-?\\s*\\b(?:U(?:NI)?T|BLDG|LOTS?|PHASES?|CONDO|SUBD?|PARCELS?|(?:\\d+(?:ST|ND|RD|TH) )?ADDN|RESUB|BLK|GL|SECS?|LYING|BOUNDED)\\b.*");
		   Matcher ma = p.matcher(legalTemp);
		   if (ma.find()){
			   subdiv = ma.group(1);		   
			   subdiv = subdiv.replaceAll("\\bUNREC\\b", "");
			   subdiv = subdiv.replaceAll("\\bPLAT\\b", "");
			   subdiv = subdiv.replaceAll("\\bMAP\\b", "");
			   subdiv = subdiv.replaceAll("\\b[\\d,\\s&-]+$", "");
			   subdiv = subdiv.replaceAll("^[\\d,\\s&-]+\\b", "");
			   subdiv = subdiv.replaceAll("\\s*&\\s*$", "");
			   if (subdiv.matches(".+ TR(ACT)?")){ // acc# 118050-0000 legal=1-1 (3) DANCY TR E1/2 OF W1/2 OF LOT 1 BLK 104 OR925/454 & 1109/1842 (Q/C)  
				   subdiv = "";
			   }
			   subdiv = subdiv.replaceFirst("(.*) OR\\s*\\d+.*", "$1");
		   }
		   if (subdiv.length() == 0){
			   p = Pattern.compile(".*\\b(?:U(?:NI)?T|TRACT|PARCEL|LOT|SEC)S? [&\\s\\d]+[A-Z]?(?: OF)? (.*?)(?: OF)? UNREC\\b.*"); //
			   ma = p.matcher(legal);
			   if (ma.find()){
				   subdiv = ma.group(1);
				   if (subdiv.contains("LYING") || subdiv.trim().equals("OF"))
					   subdiv = "";
			   }
		   }
		   if (subdiv.length() != 0){
			   subdiv = subdiv.replaceFirst("(.+) CONDO(MINIUM)?", "$1");
			   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			   if (legal.matches(".*\\bCONDO\\b.*")){
				   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }
		   }
		   	
		   legal = GenericFunctions.replaceNumbers(legal); //Bug 2334;  on account number 186216-0010  we have subdivision name  FOUR WINDS
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		   
		   
		   // additional cleaning before extracting the rest of the legal description elems
		   legal = legal.replaceAll("\\s*[&,]\\s*", " ");
		   legal = legal.replaceAll("\\sTHRU\\s", "-");
		   legal = legal.replaceAll("\\s*-\\s*", "-");
		   legal = legal.replaceAll("(\\d+/\\d+) ([A-Z]/[A-Z]) ((?:OR)?\\s?\\d+/\\d+)", "$1($2) $3");
		   legal = legal.replaceAll("\\s{2,}", " ").trim();
		   
		   // extract lot from legal description
		   String lot = ""; // can have multiple occurrences
		   String lotPatt = "\\d+(\\-?[A-Z])?";
		   p = Pattern.compile("\\bLOTS? ("+lotPatt+"(?:[\\s-]+"+lotPatt+")*)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   lot = lot + " " + ma.group(1).replaceFirst("[\\s-]+$", "");
		   }
		   lot = lot.trim();
		   if (lot.length() != 0){
			   lot = LegalDescription.cleanValues(lot, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		   }
		   	   	   	   
		   // extract block from legal description
		   String block = "";
		   p = Pattern.compile("\\bBLK ([A-Z]|\\d+(?:-?[A-Z])?)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   block = block + " " + ma.group(1);
		   }
		   block = block.trim();
		   if (block.length() != 0){
			   block = LegalDescription.cleanValues(block, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		   }	   

		   // extract plat book & page from legal description
		   p = Pattern.compile("^\\(?(\\d+)(/\\d+[A-Z]?(?:[\\s-]+\\d+[A-Z]?)*|\\s*-\\s*\\d+[A-Z]?(?:\\s\\d+[A-Z]?)*)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){ 
			   m.put("PropertyIdentificationSet.PlatBook", ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			   m.put("PropertyIdentificationSet.PlatNo", ma.group(2).replaceFirst("^[/-]", "").replaceFirst("^0+(\\d+)", "$1"));
		   }	   

		   // extract section from legal description - add it to the one extracted from GEONumber, if available
		   String sec = (String) m.get("PropertyIdentificationSet.SubdivisionSection");
		   if (sec == null)
			   sec = "";
		   p = Pattern.compile("\\bSECS? (\\d+(?: \\d+)*)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   sec = sec + " " + ma.group(1);
		   }
		   sec = sec.trim();
		   if (sec.length() != 0){
			   sec = LegalDescription.cleanValues(sec, true, true);
			   m.put("PropertyIdentificationSet.SubdivisionSection", sec);
		   }
		   
		   // extract unit from legal description
		   p = Pattern.compile("\\bU(?:NI)?T (\\d+(?:\\-?([A-Z]|\\d+))?|[A-Z]\\-?\\d+|[A-Z])\\b");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1));
		   }
		   	   	   
		   // extract phase from legal description
		   p = Pattern.compile("\\bPHASES? (\\d+(?: \\d+)*|[A-Z])\\b");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   String phase = LegalDescription.cleanValues(ma.group(1), false, true);
			   m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		   }
		   
		   // extract building # from legal description
		   p = Pattern.compile("\\bBLDG ([A-Z]\\-?\\d+|\\d+(?:\\-?[A-Z])?)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		   }
		   	   	
		   // extract tract from legal description
		   String tract = "";
		   p = Pattern.compile("\\bTR(?:ACTS?)? (\\d+(?: \\d+)?)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   tract = tract + " " + ma.group(1);
		   }
		   tract = tract.trim();
		   if (tract.length() != 0){
			   tract = LegalDescription.cleanValues(tract, true, true);
			   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		   }
		   
		   // extract cross refs from legal description
		   List<List> bodyCR = new ArrayList<List>();
		   String crPatt = "\\s*\\d+\\s*/\\s*\\d+([-\\s]+\\d+\\b(?!/))?\\s*(\\([^\\(]+\\))?";
		   p = Pattern.compile("\\bOR" + crPatt +"(\\s*\\b(OR)?" + crPatt + ")*");
		   ma.usePattern(p);
		   ma.reset();      	   
		   while (ma.find()){
			   String cbp = ma.group(0).replaceAll("\\s*/\\s*", "/").replaceAll("OR\\s+", "OR");
			   String ors[] = cbp.split("\\s(?=(OR)?\\s*\\d+/\\d+)");
			   for (int i=0; i<ors.length; i++){
				   List<String> line = new ArrayList<String>();			   
				   line.add(ors[i].replaceFirst("(?:OR\\s?)?(\\d+)/(\\d+.*)", "$1").replaceFirst("^0+(\\d+)", "$1").trim());
				   line.add(ors[i].replaceFirst("(?:OR\\s?)?(\\d+)/(\\d+.*)", "$2").replaceFirst("\\([^\\(]+\\)", "").replaceFirst("^0+(\\d+)", "$1").trim());
				   line.add("OR");
				   bodyCR.add(line);
			   } 
		   } 
		   p = Pattern.compile("\\bDB(\\d+)/(\\d+)\\b");
		   ma.usePattern(p);
		   ma.reset();      	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();			   
			   line.add(ma.group(1).trim());
			   line.add(ma.group(2).trim());
			   line.add("DB");
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
	   
	   public static void legalTokenizerFLStJohnTRInterm(ResultMap m, String legal) throws Exception {
		   legal = parseSTRFromBeginning(m, legal);
		   legalFLStJohnTR(m, legal);
	}

	private static String parseSTRFromBeginning(ResultMap m, String legal) {
		// extract sec-twn-rng from the beginning of the legal description
		Pattern p = Pattern.compile("^(\\d+)-(\\d+)-(\\d+) (.+)$");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			String sec = ma.group(1).replaceFirst("^0+(\\d+)", "$1");
			String twn = ma.group(2).replaceFirst("^0+(\\d+)", "$1");
			String rng = ma.group(3).replaceFirst("^0+(\\d+)", "$1");
			if (m.get(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName()) == null) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec);
			}
			if (m.get(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName()) == null) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), twn);
			}
			if (m.get(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName()) == null) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), rng);
			}
			legal = ma.group(4);
		}
		return legal;
	}
}

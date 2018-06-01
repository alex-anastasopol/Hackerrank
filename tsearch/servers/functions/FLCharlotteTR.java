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
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLCharlotteTR {
	static Pattern abrv = Pattern.compile("&\\s*(\\w{2,4})\\s+[^A-Z\\s]");
	static Pattern abrv2 = Pattern.compile("&\\s*(\\w{2,4})\\s*$");
	static Pattern abrv3 = Pattern.compile("(&\\s)(\\w)(\\w)(\\s+\\w{2,})(\\s" + GenericFunctions.nameSuffixString + ")?$"); //JOHNSON WILLIAM A & KM TELFAIR

	@SuppressWarnings("rawtypes")
	public static void partyNamesFLCharlotteTR(ResultMap m, long searchId) throws Exception {
		   
       String owner = (String) m.get("tmpOwnerAddress");
       int i;
       if (owner == null || owner.length() == 0)
    	   return;
       
        owner = cleanOwnerFLCharlotteTR(owner);
	    Vector<String> excludeCompany = new Vector<String>();
	    excludeCompany.add("PAT");
	    excludeCompany.add("ST");
	    excludeCompany.add("NA");
	    excludeCompany.add("(?i)\\bD F\\b");
	    
	    Vector<String> extraCompany = new Vector<String>();
	    extraCompany.add("BUZBY");
	    
		List<List> body = new ArrayList<List>();
		//owner = owner.replaceAll("\\s{2,}", " ");
		owner = owner.toUpperCase();
//		String[] lines = owner.split("\\s{2,}");
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
			boolean isCO = false;
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
				isCO = true;
				//nameFormat = 2;
				//C/O TERRANCE B & NANCY N WYATT
				if (GenericFunctions.FWFL.matcher(lines2[i]).matches()){
					nameFormat = 3;	
					
				}				
			}
			lines2[i] = lines2[i].replaceFirst("\\s*&\\s*$", "");
			lines2[i] = NameCleaner.paranthesisFix(lines2[i]);

			String curLine = NameCleaner.cleanNameAndFix(lines2[i],
					new Vector<String>(), true);
			curLine = removeABV(curLine);
			//particular words

			if (NameUtils.isCompany(curLine, excludeCompany, false)) {
				// this is not a typo, we don't know what to clean in companies'
				// names
				names[2] = lines2[i].replaceAll("^AGENT FOR\\s+", "");
				isCompany = true;
			} else {
					Vector<Integer> indexes = StringUtils.indexesOf(curLine, '&');
					if (indexes.size()==1){
					Matcher suf = GenericFunctions.nameSuffix.matcher(curLine);
					String curLine2, tmpSuf = "";
					if (suf.matches()){
						curLine2 = suf.group(1);
						tmpSuf = curLine.substring(curLine2.length()).trim();
					} else {
						curLine2 = curLine;
					}
					suf = GenericFunctions.nameSuffix3.matcher(curLine2);
					if (suf.find()){
						curLine2 = suf.replaceAll("");
					}
					curLine2 = curLine2.replaceAll("-", "____");
					Matcher ma = GenericFunctions.LFMWFWL1.matcher(curLine2);
					Matcher ma2 = GenericFunctions.LFMWFWL2.matcher(curLine2);
					
					if (ma.matches()
							&& ((ma.group(2) == null && NameFactory.getInstance().isLastOnly(ma.group(3)))
									|| (ma.group(2) != null && (NameFactory.getInstance().isLast(ma.group(3)) 
																|| (ma.group(3).length() >=NameFactory.MIN_PREFIX_LEN 
																		&& NameFactory.getInstance().isLastPrefix(ma.group(3))))))){
						String s[] = curLine.split("\\s*&\\s*");
						curLine = s[0];
						lines2[i] = s[1];
						i--;
					} else if (ma2.matches()
							
							&& !NameFactory.getInstance().isSameSex(ma2.group(6), ma2.group(4))
							&& ((ma2.group(4) == null
										&&	NameFactory.getInstance().isLastOnly(ma2.group(6))
										&&	NameFactory.getInstance().isFirstMiddleOnly(ma2.group(4)))
								  ||
								(ma2.group(4) != null
										//&& !curLine.startsWith(ma2.group(6))
											&&	(NameFactory.getInstance().isLast(ma2.group(6)) 
													|| (ma2.group(6).length() >= NameFactory.MIN_PREFIX_LEN && NameFactory.getInstance().isLastPrefix(ma2.group(6))))
											&&	NameFactory.getInstance().isFirstMiddle(ma2.group(4))))) 
								  {
						String s[] = curLine.split("\\s*&\\s*");
						curLine = s[0];
						lines2[i] = ma2.group(6).trim() + " " +ma2.group(3).trim() + " " + tmpSuf ;
						i--;					
						
					}
				} else{
					if (indexes.size() == 2){
						String s[] = curLine.split("\\s*&\\s*");
						curLine = s[0] + " & " + s[1];
						lines2[i] = s[2];
						i--;
					}
				}
				switch (nameFormat) {
				case 1:
					names = StringFormats.parseNameLFMWFWM(curLine,
							excludeCompany, false, true);
					break;
				case 2:
					names = StringFormats.parseNameDesotoRO(curLine, true);
					break;
				case 3:
					names = StringFormats.parseNameFMWFWML(curLine, excludeCompany);
					break;
				}
			}

			String[] suffixes = { "", "" };
			String[] type = { "", "" };
			String[] otherType = { "", "" };
			if (!isCompany) {
				names = NameCleaner.tokenNameAdjustment(names);
				//if (nameFormat != 1 
				//	|| !names[2].equals(names[5]) && names[2].length() > 0 && names[5].length() > 0){
				//	names = NameCleaner.lastNameSwap(names);
				//	
				//}
				if (names[0].length() <= 1 && names[1].length()<=1){
					names[0] = "";
					names[1] = "";
					names[2] = "";
				}
				if (names[3].length() <= 1 && names[4].length()<=1){
					names[3] = "";
					names[4] = "";
					names[5] = "";
				}
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractAllNamesSufixes(names);
				if (isCO){
					names = NameCleaner.lastNameSwap2(names);
				}
				names = NameCleaner.removeUnderscore(names);
				
			}
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2], excludeCompany, true), NameUtils
							.isCompany(names[5], excludeCompany, true), body);
		}
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
		/*
		 * done multiple owner parsing
		 */
		}	
	
	 
	   protected static String cleanOwnerFLCharlotteTR(String s){
		   s = s.toUpperCase();
		   //s = s.replaceFirst(" & \\w{1} & ", " & ");
		   s = s.replaceFirst("\\bLAKESHORE OF CC\\b", "");
		   s = s.replaceFirst("\\sSLAUGHTER -", "");
		   s = s.replaceFirst("\\sAS(\\s+TRUSTEE) FOR\\s" , "$1");
		   s = s.replaceFirst("\\sJO\n", "\n");
	       s = s.replaceFirst("\\b\\s*\\(H&W\\)", "");
	       s = s.replaceFirst("(\\s)+(\\w{1})\\s?-\\s?(\\w+|&)", "$1$2 $3");
	       s = s.replaceFirst("\\s\\w{1}\\s\\w{1}\\s\\w{1}\n", "\n");
	       s = s.replaceFirst("\\b\\s*\\(F/D\\)", "");
	       //s = s.replaceFirst("\\bC/O\\b", "");
	       s = s.replaceFirst("\\bL/E\\b","");
	       s = s.replaceFirst("%3", "&");
	       s = s.replaceFirst("(?is)\n\\s*OF\\s+", "\n");
	       s = s.replaceFirst("(?is)\\b-?POA\\b", "");
	       s = s.replaceFirst(" OR ", "& ");
	       s = s.replaceFirst("\\bCO\\s*-\\s*(TRS)\\b", " $1");
	       s = s.replaceFirst("\\s+SM\\n", "\n");
	       s = s.replaceAll("\\bEST?\\b", "");
	       //s = s.replaceAll("\\s+-\\s+", " ");
	       //s = s.replaceAll("\\([^\\)]+\\)", "");				
	       //s = s.replaceAll("\\s{2,}", " ").trim();
	       return s;
	   }
	   
	   public static String removeABV(String s){
		   s = s.replaceAll("\\s*&\\s*", " & ");
		   Matcher ma = abrv.matcher(s);
		   while (ma.find()){
			   if (!NameFactory.getInstance().isName(ma.group(1))){
				   s = s.substring(0, ma.start()).trim();
				   break;
				   
			   }
		   }
		   ma = abrv2.matcher(s);
		   if (ma.find() && !NameFactory.getInstance().isName(ma.group(1))){
			   s = ma.replaceAll("").trim();
		   }
		   ma = abrv3.matcher(s);
		   if (ma.find() && !NameFactory.getInstance().isName(ma.group(2) + ma.group(3))){
			   s = ma.replaceFirst("$1$2 $3$4$5");
		   }
		   return s.trim();
	   }
	   
		@SuppressWarnings("rawtypes")
		public static void legalFLCharlotteTR(ResultMap m, String legal) throws Exception {
			   //initial corrections and cleanup of legal description	   
			   legal = GenericFunctions.replaceNumbers(legal);
			   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
			   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
			   
			   legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
			   legal = legal.replaceFirst("(?is)\\b[A-Z]{2,3}\\b\\s*(?:\\d+\\s+\\d+\\s+\\d+\\s+)?", "");
			   legal = legal.replaceAll("\\bCO-OP\\b", "");
			   legal = legal.replaceAll("\\b\\d+(ST|ND|RD|TH) ADD( TO)?\\b", "");
			   legal = legal.replaceAll("(N|W|S|E|SE|SW|NE|NW)?\\s*[\\d\\.]+\\s*FT\\s*(OF)?\\b", "");
			   legal = legal.replaceAll("\\bFT( OF)?\\b", "");
			   legal = legal.replaceAll("\\bRESUB( OF)?\\b", "");
			   legal = legal.replaceAll("\\b(\\d+(ST|ND|RD|TH) )?REPLAT( OF)?\\b", "");
			   legal = legal.replaceAll("\\b(REVISED|AMENDED) PLAT( OF)?\\b", "");
			   legal = legal.replaceAll(",\\s*SUBJ TO [^,]+,?", "");
			   legal = legal.replaceAll("(\\d+)\\s*/\\s*(\\d+)", "$1" +"/" + "$2");
			   legal = legal.replaceAll("LT\\s*(\\d)(\\d{3}/\\d+)", "LT" + "$1" + " " + "$2");
			   legal = legal.replaceAll("(-[A-Z])(\\d+/)(\\d+)", "$1" + " " + "$2" + "$3");
			   legal = legal.replaceAll("( TRA) (CT)", "$1" + "$2");
			   legal = legal.replaceAll("UNREC(ORDED)?", "");
			   
			   legal = legal.replace(",", "");
			   legal = legal.replaceAll("\\s{2,}", " ").trim();
			   legal = legal.replaceAll("\\s*NO\\s*LEGAL\\s*DESCRIPTION\\s*", "");
			   legal = legal.replaceFirst("(?is)\\A[A-Z]{3,}\\s*[0\\s]+[\\d-]+", "");

			   String legalTemp = legal;
			   
			   // extract lot from legal description
			   String lot = ""; // can have multiple occurrences
			   Pattern p = Pattern.compile("\\b(LT)S?\\s*([\\d&\\s]+\\s+|(?:\\d+-[A-Z]))\\b");
			   Matcher ma = p.matcher(legal);
			   while (ma.find()){
				   lot = lot + " " + ma.group(2);
				   legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
			   }
			   lot = lot.replaceAll("\\s*&\\s*", " ").trim();
			   if (lot.length() != 0){
				   lot = LegalDescription.cleanValues(lot, false, true);
				   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			   }
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
			   	   	   	   
			   // extract block from legal description
			   String block = "";
			   p = Pattern.compile("\\b(BLK)S?\\s*([A-Z\\d]+)\\b");
			   ma = p.matcher(legal);
			   while (ma.find()){
				   block = block + " " + ma.group(2);
				   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1)+" ");
			   }
			   block = block.replaceAll("\\s*&\\s*", " ").trim();
			   if (block.length() != 0){
				   block = LegalDescription.cleanValues(block, false, true);
				   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			   }	   
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
			   	   
			   // extract unit from legal description
			   String unit = "";
			   p = Pattern.compile("\\b(UN)I?T?\\s*([\\d-A-Z]+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   unit = unit +" " + ma.group(2);
				   unit = unit.trim();
				   //legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				   legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
				   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);//ma.group(2));
				   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				   legal = legalTemp;
			   }
			   
			   // extract building #
			   String bldg="";
			   p = Pattern.compile("\\b(BLDG)\\s*([A-Z]|\\d+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   bldg = bldg + " " + ma.group(2);
				   bldg = bldg.trim();
				   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
				   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				   legal = legalTemp;
			   }
			   	   
			   // extract phase from legal description
			   String phase = "";
			   p = Pattern.compile("\\b(PH)A?S?\\s*E?\\s*(\\d+|[A-Z]+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   phase = phase + " " + ma.group(2);
				   phase = phase.trim();
				   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				   m.put("PropertyIdentificationSet.SubdivisionPhase", phase); 
				   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				   legal = legalTemp;
			   }
			   
			   // extract and remove tract from legal description
			   p = Pattern.compile("\\bTRACT (\\d+)");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   legal = legal.replaceFirst(ma.group(0), "TRACT ");
				   m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(1));
				   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   }
			   
			   // extract section from legal description
			   String sec = "";
			   p = Pattern.compile("\\b(SEC)\\s*(\\d+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   sec = sec + " " +ma.group(2);
				   sec = sec.trim();
				   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
				   m.put("PropertyIdentificationSet.SubdivisionSection", sec);
				   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				   legal = legalTemp;
			   }
			   
			   //	 extract plat book & page from legal description
			   p = Pattern.compile("\\bPB(\\d+)\\s*-\\s*(\\d+)\\b");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
				   m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
			   }
			   
			   // extract cross refs from legal description
			   List<List> bodyCR = new ArrayList<List>();
			   p = Pattern.compile("\\b(?:-?[A-Z]{2,3}\\s*)?(\\d+)[/-](\\d+(?:-[A-Z]{2})?)\\b");
			   ma = p.matcher(legal);	      	   
			   while (ma.find()){
				   List<String> line = new ArrayList<String>();		   
				   line.add(ma.group(1));
				   line.add(ma.group(2));
				   line.add("");
				   if(!COGunnisonAO.isDuplicateRef(m, line)) {
					   bodyCR.add(line);
				   }
				   legalTemp = legalTemp.replaceFirst("(?is)\\b(?:-?[A-Z]{2,3}\\s*)?(\\d+)[/-](\\d+(?:-[A-Z]{2})?)\\b", "");		   
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
			   legalTemp = legalTemp.replaceFirst("(?is)\\w+/\\w+-\\w+&\\w+", "");
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
			   	  	
			   String subdiv = legal;
			   p = Pattern.compile(".*\\b(?:LOT|BLK|BLOCK|UNIT|PHASE|BLDG) (.+?) (?:UNIT|PHASE|SEC(?! OF\\b)|ORI?)\\b.*");
			   ma = p.matcher(subdiv);
			   if (ma.find()){
				   subdiv = ma.group(1);		   
			   } else {
				   p = Pattern.compile(".*\\b(\\s*[A-Z]{2,3}+\\d?\\s+)([\\dA-Z]+)\\s+([A-Z\\d]+)\\s+([A-Z\\d]+) (.+?) (?:SEC|LT|BLK|PH|UN|UNIT|BLDG|TRACT|A POR|PLAN)");
				   ma.usePattern(p);
				   ma.reset();
				   if (ma.find()){
					   subdiv = ma.group(5);
				   }   
			   } 
			   
			   if (subdiv.length() != 0){
				   subdiv = subdiv.replaceAll("(.+) (\\d+)", "$1");
				   subdiv = subdiv.replaceFirst("(.+) CONDO(MINIUM)?.*", "$1");
				   subdiv = subdiv.replaceFirst("(.+) SUB(DIVISION)?.*", "$1");
				   subdiv = subdiv.replaceFirst("(.+) SEC(TION)?.*", "$1");
				   subdiv = subdiv.replaceAll("(?is)\\b(?:LOT|BLK|BLOCK|UNIT|PHASE|BLDG)\\b", "");
				   m.put("PropertyIdentificationSet.SubdivisionName", subdiv.replaceAll("\\bOF CHAR CNTY$", ""));
				   if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
					   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }	
		   }
}

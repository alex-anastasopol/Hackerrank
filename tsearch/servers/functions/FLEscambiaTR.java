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
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLEscambiaTR {
	
	public static Pattern twoWords = Pattern.compile("\\w+\\s+\\w+(\\s" + GenericFunctions.nameSuffixString + ")?(\\s*&)?");
	public static Pattern oneWord = Pattern.compile("(& )?(\\w+)(\\s+\\w{1})?(\\s+(FOR|&|!!!!!))?");
	public static Pattern oneWordSuffix = Pattern.compile("(\\w+)\\s" + GenericFunctions.nameSuffixString);
	public static Pattern initialWord = Pattern.compile("\\w{1}\\s\\w+");
	public static Pattern fourWords = Pattern.compile("(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)");
	public static Pattern threefourWords = Pattern.compile("(\\w+)(\\s\\w+)?\\s(\\w+)\\s(\\w+)");
	public static Pattern husbandWife = Pattern.compile("(\\w+)\\s\\w+\\s&\\s(\\w+)\\s(\\w+)");
	public static Pattern companySuffixProblem = Pattern.compile("(LLC|INC)\\s*/");

	public static Pattern threeWords = Pattern.compile("(\\w+)\\s(\\w+)\\s(\\w+)");

	
	public static void legalFLEscambiaTR(ResultMap m, String legal) throws Exception {
		// initial corrections and cleanup of legal description
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		// convert roman numbers
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); 

		legal = legal.replaceAll("(?is)\\s+\\d+\\s+\\d+/100\\s*FT", "");//that means ex: 234.40 feet
		
		legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		legal = legal.replaceAll("\\bCO-OP\\b", "");
		legal = legal.replaceAll("\\b\\d+(ST|ND|RD|TH) ADD( TO)?\\b", "");
		legal = legal.replaceAll("\\b(THE )?[NWSE]{1,2}(LY)? [\\d\\./\\s]+(\\s*\\bFT)?(\\s*\\bOF)?\\b", "");
		legal = legal.replaceAll("\\bFT( OF)?\\b", "");
		legal = legal.replaceAll("\\bRESUB( OF)?\\b", "");
		legal = legal.replaceAll("\\b(\\d+(ST|ND|RD|TH) )?REPLAT( OF)?\\b", "");
		legal = legal.replaceAll("\\b(REVISED|AMENDED) PLAT( OF)?\\b", "");
		legal = legal.replaceAll(",\\s*SUBJ TO [^,]+,?", "");
		legal = legal.replaceAll("\\b(\\d+)\\s*([A-Z])&([A-Z])\\b", "$1$2/$1$3");
		legal = legal.replaceAll("\\bS/D\\b", "SUBDIV");
		legal = legal.replace(",", "");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("\\b((?:LOT)S?|(?:LT)S?) ([\\d\\s&-]+|\\d+[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract S/T/R
		List<List<String>> bodyPis = new ArrayList<List<String>>();
		p = Pattern
				.compile("SEC (\\d+(?:/\\d+)*) T (\\d+(?:\\s?[NSEW])?) R (\\d+(?:\\s?[NSEW])?(?:/\\d+)?)");
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
			legalTemp = legalTemp.replaceFirst(ma.group(0), "SEC");
		}
		p = Pattern.compile("SEC (\\d+)-(\\d+(?:\\s?[NSEW])?)-(\\d+(?:\\s?[NSEW])?)");
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
			legalTemp = legalTemp.replaceFirst(ma.group(0), "SEC");
		}
		legal = legalTemp;

		if (bodyPis.size() == 1) {
			List<String> line = bodyPis.get(0);
			m.put("PropertyIdentificationSet.SubdivisionSection", line.get(0));
			m.put("PropertyIdentificationSet.SubdivisionTownship", line.get(1));
			m.put("PropertyIdentificationSet.SubdivisionRange", line.get(2));
		} else if (bodyPis.size() > 1) {
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
		p = Pattern.compile("\\bPB\\s+(\\d+)\\s+P\\s+((?:\\d+[A-Z]?/?\\s*)+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2).replace("/", " "));
			legalTemp = legalTemp.replaceFirst(ma.group(0), "PL ");
		}
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("\\bBL(?:OC)?KS? ((?:\\b[A-Z]\\b|\\d+|&|\\s)+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legalTemp = legalTemp.replaceFirst(ma.group(0), "BLK ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		p = Pattern.compile("\\b(UNIT) (?:NO |# ?)?((?:[A-Z]-?)?\\d+[A-Z]?(?:-\\d+)?)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract building #
		p = Pattern.compile("\\b(BLDG) ([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract phase from legal description
		p = Pattern.compile("\\b(PH)(?:ASES?)? ([\\d\\s&-]+|\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0).trim(), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(2).replaceAll("\\s*&\\s*",
					" "));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		p = Pattern.compile("\\b(SEC) (\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern
				.compile("\\b(?:PLAT )?(OR|DB)\\s+((?:\\d+[A-Z]?/?)+)\\s+P\\s+((?:\\d+[A-Z]?/?)+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3).replace("/", " "));
			String type = ma.group(1);
			if (!"DB".equals(type)) {
				type = "";
			}
			line.add(type);
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		p = Pattern.compile("\\b(ORI) ([A-Z\\d]+)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(2));
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
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
		
		legal = legal.replaceAll("LT AND LTS", "LT");
		legal = legal.replaceAll("OR LESS OR", "OR");
		legal = legal.replaceAll("(OR )+", "OR ");
		legal = legal.replaceAll("\\bDEG \\d+ MIN \\d+\\b", "");
		legal = legal.replaceAll("SHEET [0-9A-Z]+", "");
		legal = legal.replaceAll("\\bCASE \\d+(-\\d+)*\\b", "");	// fix for bug #2453
		legal = legal.replaceAll("\\bCP-\\d+(-\\d+)*\\b", "");		// fix for bug #2453
		legal = legal.replaceAll("\\bSUBDIV\\b", "S/D");
		legal = legal.replaceAll("\\b(LOT|LT|LTS|OR|DB|BLK|PH|BLDG|UNIT|PH|PL|SEC|TRACT)\\b", "@");
		legal = legal.replaceAll("CA \\d+", "@");
		legal = legal.replaceAll("@ RE S/D OF @", "@");
		legal = legal.replaceAll("@ AND [^@]+ OF @", "@");
		legal = legal.replaceAll("^S\\d+/\\d+ OF @", "@");
		legal = legal.replaceAll("\\(\\d+\\)\\s+", "");
		legal = legal.replaceAll("LESS MINERAL RIGHTS", " ");
		legal = legal.replaceAll("(&|ALSO) ((\\d+/\\d+)|(\\d*\\.\\d+%)) INT IN COMMON ELEMENTS", " ");
		legal = legal.replaceAll("@ AND (ALL)?@", "@");
		legal = legal.replaceAll("\\s{2,}", " ");
		legal = legal.replaceAll("TRUSTEES FOR[^@]+@", "@");
		legal = (" " + legal).replaceAll("( @)+", " @").trim();
		
		String subdiv = "";
		p = Pattern.compile("^@ ([^@]+) @$");
		ma = p.matcher(legal);
		if (ma.matches()) {
			subdiv = ma.group(1);
		}
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceAll("(RE )?S/D", "").trim();
			subdiv = subdiv.replaceAll("\\s*ADDN","").trim();
			subdiv = subdiv.replaceAll("(LESS\\s*)?[0-9](ST|ND|TH) ADDN( TO)?", "").trim();
			subdiv = subdiv.replaceFirst("(.+) CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceFirst("^\\d+\\s", "");
			if ("LESS".equals(subdiv)) {
				subdiv = "";
			}
			subdiv = subdiv.replaceFirst("^OF ", "");
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*")){
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			}
		}
	}
	
	public static void partyNamesFLEscambiaTR(ResultMap m, long searchId) throws Exception {
		   /*
        if (m.get("saving") == null){
	      //if (owner.contains("Mailing Address")){  
	        try {
	            FileWriter fw = new FileWriter(new File("/home/danut/Desktop/work/parsing/flescambiatr/flescambiatr.html"),true);
	             fw.write(owner);
	            fw.write("\n==============================================================\n");
	            fw.close();
	        }
	        catch (IOException e) {
	            e.printStackTrace();
	        }
	      //}
        
        }
        */
			ArrayList<String> lines = new ArrayList<String>();
			int i;
			String owner = (String) m.get("tmpOwnerAddress");
			if (StringUtils.isEmpty(owner)) {
				return;
			}
			
			if (owner.contains("Mailing Address")) {
				owner = owner.replaceFirst("(?is)Mailing Address\\s+", "");
				owner = owner.replaceFirst("(?is)\\bFor\\b\\s+", "");
				owner = owner.replaceAll("(?is)\\s{2,}","@@");
			}
			
			String prevLastName = "";
			Boolean prevWasCompany = false;
			Vector<String> extraInvalid = new Vector<String>();
			extraInvalid.add("^\\d+\\s+HWY.*");
			extraInvalid.add(".*TRISTAN TOWERS.*");
			extraInvalid.add("UNIT.*\\d+(-)?\\w");
			extraInvalid.add("LEASED EQ.*");
			extraInvalid.add(".*\\d+.*(AVE|DR|ST|RD|CIRC)");
			
			Vector<String> excludeCompany = new Vector<String>();
			excludeCompany.add("PAT");
			excludeCompany.add("ST");
			excludeCompany.add("WORK");
			
			Vector<String> extraCompany = new Vector<String>();
			extraCompany.add("GENERAL");
			extraCompany.add("FLORIDA");
			
			
			
			List<List> body = new ArrayList<List>();
			
			String[] tmpLines = owner.split("@@");
			for (String tmpS:tmpLines){
				lines.add(preCleanOwnerFLEscambiaTR(tmpS));
			}
			if (lines.size() <= 2)
				return;
			
			// clean address out of mailing address
			ArrayList<String> lines2 = GenericFunctions.removeAddressFLTR2(lines,
					excludeCompany, extraCompany, extraInvalid, 2, 30);

			lines2 = mergeNamesFLEscambiaTR(lines2, " ");
			if (lines2.size()>2){
				lines2 = fixNameFLEscambiaTR(lines2, lines2.toString());
			} else if (lines2.size()==2){
				//	JOHNSON DRYWALL & PAINTING\nINC/JOHNSON CARL
				Matcher ma = companySuffixProblem.matcher(lines2.get(1));
				if (ma.find()){
					lines2.set(0, lines2.get(0) + " " +ma.group(1));
					lines2.set(1, ma.replaceFirst("/")); // this is LFM
				}
			}
			
			//TAYLOR CARLTON, MICHAEL,\nBRUCE, ROBERT
			if (owner.contains(",")){
				lines2 = splitNamesFLEscambiaTR(lines2);
			}
			for (i = 0; i < lines2.size(); i++) {
				if (lines2.get(i).equals("")){
					continue;
				}
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
					nameFormat = 2;
				}

				ln = NameCleaner.paranthesisFix(ln);
				ln = ln.replaceAll("\\s+&\\s*$", "");
				String curLine = NameCleaner.cleanNameAndFixNoScotish(ln, new Vector<String>(), true);
				
				curLine = NameCleaner.removeExpressions(curLine, NameCleaner.LIFEESTATE).trim();
				curLine = cleanOwnerFLEscambiaTR(curLine);
				if (curLine.equals("")){
					continue;
				}
				
				if (NameUtils.isCompany(curLine, excludeCompany, true)) {
					// this is not a typo, we don't know what to clean in companies'
					// names
					names[2] = cleanCompanyNames(ln);
					isCompany = true;
					prevWasCompany = true;
				} else {
			
					
					if (i == 1 && lines2.size()==2){
						if ((lines2.get(0).endsWith("&") || prevWasCompany)
								&& (GenericFunctions.FMiL.matcher(curLine).matches())){
							//SMITH CRAIG A &\nCAROLYN M SCHUSTER
							nameFormat = 2;
						} else if (prevWasCompany && twoWords.matcher(curLine).matches() && !lines2.get(1).startsWith("/")){
							nameFormat = 2;
						} else if (lines2.get(1).startsWith("C/O") && GenericFunctions.LFMi.matcher(curLine).matches()){
							//TAYLOR LAWRENCE\nC/O VANDERPOL CATHERINE L
							nameFormat = 1;
						}
					}
					if (oneWord.matcher(curLine).matches()){
						curLine = prevLastName + " " + curLine;
					}
					switch (nameFormat) {
					case 1:
						names = StringFormats.parseNameLFMWFWM(curLine,
								excludeCompany, true, true);
						break;
					case 2:
						names = StringFormats.parseNameFML(curLine, excludeCompany, true, true);
						break;
					case 3:
						names = StringFormats.parseNameFMWFWML(curLine, excludeCompany, true);
						break;
					}
				}
				
				//SMITH CHARLES G &\nBEATE MARTHA
				if (prevLastName.length()>0){
					for(int ii = 0;ii<names.length; ii+=3){
						if (names[ii+1].length() == 0
								&& NameFactory.getInstance().isFirstMiddleOnly(names[ii+2])){
							names[ii+1] = names[ii];
							names[ii] = names[ii+2];
							names[ii+2] = prevLastName;
						} 
					}
				}
				//TAYLOR JAMES D &\nTAMI KACACHOS-TAYLOR
				if (lines2.size() == 2 && names[5].equals("")
						&& names[0].contains("____") && !names[2].contains("____") && names[1].equals("")){
					names[1] = names[2];
					names[2] = names[0];
					names[0] = names[1];
					names[1] = "";
				}
				
				String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
				
				if (!isCompany) {
					names = NameCleaner.tokenNameAdjustment(names);

					suffixes = GenericFunctions.extractAllNamesSufixes(names);
					names = NameCleaner.removeUnderscore(names);
					
					if (names[5].length() == 0){
						prevLastName = names[2];
					} else {
						prevLastName = names[5];
					}
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
        
	}
	
	
	public static ArrayList<String> splitNamesFLEscambiaTR(ArrayList<String> a){
		ArrayList<String> tmpN = new ArrayList<String>();
		for (int i = 0; i < a.size(); i++){
			String[] split = a.get(i).split("\\s*,\\s*");
			for (int j = 0; j < split.length; j++){
				tmpN.add(split[j]);
			}
		}
		return tmpN;
	}
	
	/**
	 * Splits names separated by \\d+/\\d+<br>
	 * It also NameCleaner.fixLikeScotishNames
	 * @param a - lines of names
	 * @param separator
	 * @return
	 */
	public static ArrayList<String> fixNameFLEscambiaTR(ArrayList<String> a, String fullString){
		ArrayList<String> tmpStr = new ArrayList<String>();

		int newIndex = 0;
		tmpStr.add("");
		int i = -1;
		if (StringUtils.indexesOf(fullString, "&").size() > 1 
				|| StringUtils.indexesOf(fullString, "!!!!!").size() >0){
			for(i = 0; i< a.size(); i++){
				String tmpS = a.get(i).replaceAll("!!!!! &", "&");
				String[] splits;
				if (i == 0 && husbandWife.matcher(tmpS).matches()){
					tmpStr.set(0, tmpS);
					tmpStr.add("");
					newIndex++;
					continue;
				} else {
					splits = tmpS.split("\\s*(!!!!!|&)\\s*");
				}
				
				if (tmpS.startsWith("C/O")){
					tmpStr.add(tmpS);
					newIndex++;
					continue;
				}
				if (splits[0].matches("(TRUSTEES )?FOR .*")){
					tmpStr.add(a.get(i));
					break;
				}
				if (splits[0].startsWith(tmpStr.get(newIndex))){
					tmpStr.set(newIndex, splits[0]);
				} else {
					tmpStr.set(newIndex, tmpStr.get(newIndex) + " " + splits[0]);
				}
	
	
				for(int j = 1; j<splits.length; j++){
					tmpStr.add(splits[j]);
					newIndex++;
				}
				int n = splits.length-1;
				if (splits[n].matches(".*LIFE EST(ATE)?")
						|| splits[n].endsWith("TRUESTEES")
						|| splits[n].endsWith("TRUSTEES")
						||a.get(i).endsWith("&")
						|| a.get(i).endsWith("!!!!!")
						|| threeWords.matcher(splits[n]).matches()){
						newIndex++;
						tmpStr.add("");
				}
				if (splits[n].endsWith(" FOR")){
					break;
				}
			}
		} 
		for(i++; i<a.size(); i++){
			tmpStr.add(a.get(i));
		}
		return tmpStr;
	}
	
	public static String preCleanOwnerFLEscambiaTR(String s){
		s = s.replaceAll("\\b\\d+/\\d+( INT)?", "!!!!!");
		s = s.replaceAll("\\b\\d+(\\s+)?%\\s*(INT\\b\\s*)?", "");
		s = s.replaceAll("(/)?RENTAL.*", "");
		//s = s.replaceAll(" & AHERN$", "");
		s = s.replaceFirst("^KAY K$", "KAY_K");
		s = s.replaceFirst("DEGRADO C/MARINER$", "");
		s = s.replaceFirst("/WINDEMERE", "");
		//s = s.replaceFirst("^LLC /", "");
		s = s.replaceFirst("(?is)\\s*&\\s*\\z", "");
		s = s.replaceFirst("(?is)(?:CO)?\\s*-?\\s*TRUSTEES?", "");
		return s.trim();
	}
	
	/**
	 * it will also NameCleaner.fixLikeScotishNames
	 * @param a
	 * @param separator
	 * @return
	 */
	public static ArrayList<String> mergeNamesFLEscambiaTR(ArrayList<String> a, String separator){
		if (a.size()==0){
			return a;
		}
		ArrayList<String> tempStr = new ArrayList<String>();
		int j = 0;
		int i;
		tempStr.add(NameCleaner.fixScotishLikeNames(a.get(0)));

		for (i = 1; i < a.size(); i++) {
				boolean merge = false;
				String prev = a.get(i-1);
				String current = a.get(i);
				current = NameCleaner.fixScotishLikeNames(current);
				if (prev.endsWith(current)){
					a.remove(i);
					i--;
					continue;
				}
				
				if (prev.endsWith(" FOR") || current.startsWith("FOR ")){
					tempStr.add(current);
					i++;
					break;
				}
				//TAYLOR GEORGE W III &\nANNA DAVIS
				//46 out of 56 examples had to be merged
				if ((prev.matches(".*(&|AND)$") && twoWords.matcher(current).matches() && a.size() == 2 && !current.contains("____"))
					|| (oneWord.matcher(current).matches())
					|| (initialWord.matcher(current).matches())
					|| oneWordSuffix.matcher(current).matches()
					|| prev.equals("PHILLIPS SANDRA ANNE")){
					merge = true;
				}
				if (merge) {
					tempStr.set(i - j - 1, tempStr.get(i-j-1) + separator + current);
					j++;
				} else {
					tempStr.add(current);
				}

		}
		//after for should be only companies names
		for(;i<a.size(); i++){
			if (a.get(i).startsWith("FOR ")){
				tempStr.add(a.get(i));
			} else {
				tempStr.set(tempStr.size()-1, tempStr.get(tempStr.size()-1) + " " + a.get(i));
			}
		}
		
		return tempStr;
	}	
	
	public static String cleanOwnerFLEscambiaTR(String s){
		s = s.replaceAll("!!!!!", "");
		s = s.replaceFirst("\\s+OF$", "");
		s = s.replaceFirst("\\bFOR\\b", "");
		s = s.replaceFirst("^& ", "");
		//s = s.replaceAll("\\s+\\d+/\\d+ INT", "");
		return s.trim();
	}
	
	public static String cleanCompanyNames(String s){
		s = s.replaceAll("!!!!!", "");
		s = s.replaceFirst("^(TRUSTEES )?FOR\\s+", " $1");
		s = NameCleaner.removeUnderscore(s);
		return s.trim();
	}
}

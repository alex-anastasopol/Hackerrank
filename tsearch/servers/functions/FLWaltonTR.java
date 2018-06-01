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

public class FLWaltonTR {
	public static Pattern doubleSuffix = Pattern.compile("\\b(" + GenericFunctions.nameSuffixString + ")\\s*&\\s*(" + GenericFunctions.nameSuffixString + ")$"); 
	public static Pattern endsWithWordInitial = Pattern.compile(".*&\\s*\\w{2,}\\s+\\w");
	public static Pattern twoWords = Pattern.compile("\\w+\\s+\\w+(\\s" + GenericFunctions.nameSuffixString + ")?(\\s*&)?");
	public static Pattern twoWordsNoInitial = Pattern.compile("\\w{2,}\\s+\\w{2,}(\\s" + GenericFunctions.nameSuffixString + ")?(\\s*&)?");
	public static Pattern oneWord = Pattern.compile("(& )?(\\w+)(\\s+\\w{1})?(\\s+(FOR|&|!!!!!))?");
	public static Pattern oneWordSuffix = Pattern.compile("(\\w+)\\s" + GenericFunctions.nameSuffixString);
	public static Pattern initialWord = Pattern.compile("\\w{1}\\s\\w+");
	public static Pattern wordInitial = Pattern.compile("\\w{2,}\\s\\w{1}");
	public static Pattern initialWordSuffix = Pattern.compile("\\w{1}\\s\\w+(\\s" + GenericFunctions.nameSuffixString + ")?");
	public static Pattern initialWordSuffix2 = Pattern.compile("(\\w+(\\s" + GenericFunctions.nameSuffixString + ") )&(.*)");
	public static Pattern fourWords = Pattern.compile("(\\w+)\\s(\\w+)\\s(\\w+)\\s(\\w+)");
	public static Pattern threefourWords = Pattern.compile("(\\w+)(\\s\\w+)?\\s(\\w+)\\s(\\w+)");
	public static Pattern husbandWife = Pattern.compile("(\\w+)\\s\\w+\\s&\\s(\\w+)\\s(\\w+)");
	public static Pattern companySuffixProblem = Pattern.compile("(LLC|INC)\\s*/");

	public static Pattern threeWords = Pattern.compile("(\\w+)\\s(\\w+)\\s(\\w+)");

	
	@SuppressWarnings("rawtypes")
	public static void stdFLWaltonTR(ResultMap m, long searchId) throws Exception {
		   
		   String owners = (String) m.get("tmpOwnerAddress");
		   if (StringUtils.isEmpty(owners))
			   return;
		   
		   owners = owners.replaceFirst("(?s)(.*)\\s*\\bUNKNOWN\\b.*", "$1");
		   String[] lines = owners.split("\\s{2,}");
		   // the owner and co-owner name are stored in the first 1 or 2 lines and the last 2 lines contains the mailing address
	
		   String owner = lines[0];			   
		   String a[] = ownerNameFLWaltonTR(owner);
		   
		   if ((lines.length > 3) && (a[5].length() == 0)){
			   for (int i=1; i<lines.length-2; i++){
				   owner = owner + " " + lines[i];
			   }
		   }
		   a = ownerNameFLWaltonTR(owner);
		   ;
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
	
	public static String[] ownerNameFLWaltonTR(String owner){
		
		   boolean isFML = !owner.contains(",") && !owner.contains(";");
		   String spouse = "";
		   Pattern p = Pattern.compile("(.*?)\\s*\\b(?:AS )?(TRUSTEES?) (?:OF|FOR)\\b\\s*(.*)");
		   Matcher ma = p.matcher(owner);
		   if (ma.find()){
			   owner = ma.group(1) + " " + ma.group(2);
			   spouse = ma.group(3).replaceFirst("^THE$", "");
		   }
		   
		   owner = cleanOwnerFLWaltonTR(owner);
		   owner = StringFormats.unifyNameDelim(owner, true);
		   if (spouse.length() != 0){
			   spouse = cleanOwnerFLWaltonTR(spouse);
			   spouse = StringFormats.unifyNameDelim(spouse, true);
		   }
		   owner = owner.replaceFirst("(?is)([A-Z\\s]+) ([\\d]+.*|PO.*)", "$1");  //for B2374
		   // if L1 contains more than 2 owners, keep only the first 2, because ATS doesn't have support for more than 2 owner		  
		   owner = owner.replaceFirst("(.*? & .*?) &.*", "$1");
		   
		   // parse line1 as LFM
		   String[] a = {"", "", "", "", "", ""};
		   String[] b = {"", "", "", "", "", ""};
		   a = StringFormats.parseNameNashville(owner, true);
		   // parse spouse from L1
		   boolean spouseUpdate = false;
		   if (spouse.length() == 0){
			   spouse = owner.replaceFirst(".* & (.*)", "$1");			   
		   if (!spouse.equals(owner) && !spouse.contains("TRUST")){
			   if (isFML && (spouse.matches("[A-Z]+ [A-Z] [A-Z-]{3,}") || spouse.matches("[A-Z]{2,} [A-Z-]{3,}"))){ // spouse is in FML format
					   b = StringFormats.parseNameDesotoRO(spouse, true);
					   spouseUpdate = true;
				   }
			   }
		   } else {
			   b = StringFormats.parseNameNashville(spouse, true);
			   spouseUpdate = true;
		   }
		   if (spouseUpdate){
			   a[3] = b[0];
			   a[4] = b[1];
			   a[5] = b[2];			   
		   }
		   return a;
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesFLWaltonTR(ResultMap m, long searchId) throws Exception {
		   //saving examples
		   String owners = (String) m.get("tmpOwnerAddress");
		   if (StringUtils.isEmpty(owners))
			   return;
		   /*
		boolean save = false;
	   String o = owners;
	   String filename = "flwaltontr";
	   String contain = ""; //if the same function is used for inter, then what substring should be in o to save it
     if (m.get("saving") == null && save){
		      if (o.contains(contain)){  
		    	  //String[] s = o.split("@@");
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

		String prevLastName = "";
		//Boolean prevWasCompany = false;
		Vector<String> extraInvalid = new Vector<String>();
		extraInvalid.add("^\\d+\\s+HWY.*");
		extraInvalid.add(".*TRISTAN TOWERS.*");
		extraInvalid.add("UNIT.*\\d+(-)?\\w");
		extraInvalid.add("LEASED EQ.*");
		extraInvalid.add(".*\\d+.*(AVE|DR|ST|STREET)");
		extraInvalid.add(".* ST. ANDREW");
		extraInvalid.add(".*IPSWICH.*");
		//extraInvalid.add("LIVING TRUSTS");
		//extraInvalid.add("REVOC TRUST");
		extraInvalid.add("CROWN COLONY");
		extraInvalid.add("CHICAGO ILL");
		//extraInvalid.add("CO TRUSTEES OF REVOCABLE TRUST");
		
		Vector<String> excludeCompany = new Vector<String>();
		excludeCompany.add("PAT");
		excludeCompany.add("ST");
		excludeCompany.add("WORK");
		
		Vector<String> extraCompany = new Vector<String>();
		extraCompany.add("ONE");
		extraCompany.add("GENERAL");
		extraCompany.add("GAINESVILLE");
		extraCompany.add("FLORIDA");
		extraCompany.add("TALLAHASSEE");
		extraCompany.add("LIABILITY");
		extraCompany.add("ATMORE");
		extraCompany.add("SHARING");
		extraCompany.add("PLAN");
		extraCompany.add("III");
		extraCompany.add("REVOC");
		extraCompany.add("PROPERITES");
		
		List<List> body = new ArrayList<List>();
		
		
		if (owners.contains("REPS")){
			owners = owners.replaceAll("&", "!!!!!");
		}
		owners = owners.replaceAll("\\s+(?:AS )?(?:CO[\\s-])?(TRUSTEEE?S?)(?: OF)?(?: FOR)?\\b", " $1");
		String[] tmpLines = owners.split("\\s{2,}");
		for (String tmpS:tmpLines){
			if (!tmpS.contains("UNKNOWN")){
				lines.add(NameCleaner.fixScotishLikeNames(preCleanOwnerFLWaltonTR(tmpS)));
			}
		}
		if (lines.size() == 0)
			return;
		ArrayList<String> lines2 = new ArrayList<String>();
		// clean address out of mailing address
		if (!owners.contains("UNKNOWN")){
			lines2 = GenericFunctions.removeAddressFLTR2(lines,
				excludeCompany, extraCompany, extraInvalid, 2, 30);
		} else {
			lines2 = lines;
		}
		//where we have NEW OWNER, we ignore what is before NEW OWNER
		if (owners.contains("NEW OWNER")){
			int ii;
			for (ii = 0; ii <= lines2.size(); ii++){
				if (lines2.get(ii).contains("NEW OWNER")){
					break;
				} else {
					lines2.set(ii, "");
				}
			}
			if (lines2.size()> ii){
				lines2.set(ii, lines2.get(ii).replaceAll("(\\*\\*\\s*)?NEW OWNER(S)?(\\s*\\*\\*)?(:)?\\s*", ""));
				if (lines2.get(ii).trim().equals("C/O") && lines2.size() == ii+2){
					lines2.set(ii+1, "C/O " + lines2.get(ii+1));
					lines2.remove(ii);
				}
			}
		}

		if (lines2.size()>2 || lines2.toString().contains("!!!!!")){
			
			lines2 = fixNameFLWaltonTR(lines2, lines2.toString());

		} 
		
		if (lines2.size()==2){
			String line1, line2;
			line1 = lines2.get(0);
			line2 = lines2.get(1);
			String[] splits1 = line1.split(" ");
			String[] splits2 = line2.split(" ");
			//	JOHNSON DRYWALL & PAINTING\nINC/JOHNSON CARL
			Matcher ma = companySuffixProblem.matcher(line2);
			if (ma.find()){
				lines2.set(0, line1 + " " +ma.group(1));
				lines2.set(1, ma.replaceFirst("/")); // this is LFM
			} else if (initialWordSuffix.matcher(line2).matches()
					//UNELL BONNIE M & DOUGLAS\nG BABCOCK
					||oneWord.matcher(line2).matches()
					//SMITH DAROLD & GAIL HANCOCK\nSMITH
					|| oneWordSuffix.matcher(line2).matches()
					//SMITH PEGGY LOU MCNEILL &\nALTO FRANK SMITH
					|| (line1.endsWith("&")
						&& splits1[0].equals(splits2[splits2.length-1]))
					//CHAMBERS CHESTER J & LOUISE\nHARRISON CHAMBERS
					|| (twoWords.matcher(line2).matches()
					 	&& line1.indexOf('&') != -1
					 	&& line1.lastIndexOf('&') != line1.length()-1
					 	&& !endsWithWordInitial.matcher(line1).matches()
					 	&& NameFactory.getInstance().isLast(line2.split(" ")[0])
					 	&& NameFactory.getInstance().isLastOnly(line2.split(" ")[1])
					 	&& !GenericFunctions.nameSuffix.matcher(line1).matches())
					){
				String join = " & ";
				if (lines2.get(0).contains("&")){
					join = " ";
				} else  if (line1.endsWith("_")){
					join = "";
				}
				lines2.set(0, lines2.get(0) + join + lines2.get(1));
				lines2.remove(1);
			} else if( GenericFunctions.LFMWFWL3.matcher(line1).matches()
						&& line2.matches("\\w{2,} & \\w \\w{2,}")){
				String[] sss = line1.split("\\s*&\\s*");
				lines2.set(0, sss[0]);
				lines2.set(1, sss[1] + " " + line2);
			} else if(line2.matches("\\w{1}\\s+&.*")){
				String[] sss = line2.split("\\s+&\\s+");
				lines2.set(0, line1 + " " + sss[0]);
				lines2.set(1, lines2.get(1).replaceFirst("\\w{1}\\s+&", ""));
				
				
			}
		} else if (lines2.size() == 3){
			if (initialWordSuffix.matcher(lines2.get(2)).matches()){
				lines2.set(1, lines2.get(1) + " " + lines2.get(2));
				lines2.remove(2);
			}
		} else if(lines2.size()==1){
			lines2.set(0, lines2.get(0).replaceAll("!!!!!", "&"));
		}
		;
		for (i = 0; i < lines2.size(); i++) {
			if (lines2.get(i).equals("")){
				continue;
			}
			String[] doubleSuff = {"", ""}; 
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

				//C/O TERRANCE B & NANCY N WYATT
				if (GenericFunctions.FWFL.matcher(ln).matches()){
					nameFormat = 3;	
				}  else {
					nameFormat = 2;
				}
			}

			ln = NameCleaner.paranthesisFix(ln);
			String curLine = NameCleaner.cleanNameAndFixNoScotish(ln, new Vector<String>(), true);
			
			curLine = cleanOwnerFLWaltonTR2(curLine);
			if (curLine.equals("")){
				continue;
			}
			
			if (NameUtils.isCompany(curLine, excludeCompany, true)) {
				// this is not a typo, we don't know what to clean in companies'
				// names
				
				names[2] = cleanCompanyName(ln);
				if (NameUtils.isCompanyNamesOnly(curLine, new Vector<String>())){
					names[2] = "";
				}
				//names[2] = ln;
				isCompany = true;
				//prevWasCompany = true;
			} else {
				if (!prevLastName.equals("")
					&& (oneWordSuffix.matcher(curLine).matches() 
					|| oneWord.matcher(curLine).matches()
					|| (twoWords.matcher(curLine).matches()
						&& NameFactory.getInstance().isSameSexOnly(curLine.split(" ")[0], curLine.split(" ")[1])
						&& !curLine.startsWith(prevLastName)))){
					curLine = prevLastName + " " + curLine;
				} else if (!prevLastName.equals("") 
						&& !curLine.contains("&")
						&& curLine.endsWith(prevLastName) 
						&& !NameFactory.getInstance().isLastOnly(curLine.split(" ")[0])){
					//DAVIDSON MALCOLM ROLFS &\nDAISEY FAY KELLY DAVIDSON
					nameFormat = 2;
				} else if (GenericFunctions.FWFL2.matcher(curLine).matches()){
					nameFormat = 3;
				} else {
				
					Matcher ma = GenericFunctions.FMiL.matcher(curLine);
					if (ma.matches()){
						if ((!NameFactory.getInstance().isLastOnly(ma.group(1))
								|| !NameFactory.getInstance().isFirstMiddle(ma.group(3)))
							&& i>0){
							nameFormat = 2;
						}
					}
					
				} 
				//THOMASON GLEN E JR & SR
				Matcher doubleS = doubleSuffix.matcher(curLine);
				if (doubleS.find()){
					doubleSuff[0] = doubleS.group(1);
					doubleSuff[1] = doubleS.group(4);
					curLine = doubleS.replaceAll("").trim();

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
			
			String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
			//String[] maiden = { "", ""};
			if (!isCompany) {
				if (owners.contains(",") || owners.contains("REPS")
					|| (i>0
						&&lines2.get(i-1).endsWith("&"))){
					names = NameCleaner.lastNameSwap(names);
				}
				suffixes = GenericFunctions.extractAllNamesSufixes(names);
				names = NameCleaner.tokenNameAdjustment(names);

				//maiden = NameFactory.getInstance().extractMaiden(names);
				names = NameCleaner.removeUnderscore(names);
				//maiden = NameCleaner.removeUnderscore(maiden);
				if (names[5].length() == 0){
					prevLastName = names[2];
				} else {
					prevLastName = names[5];
				}
				if (!doubleSuff[0].equals("") && !doubleSuff[1].equals("")){
					suffixes[0] += " " +doubleSuff[0];
					suffixes[1] += " " +doubleSuff[1];
					suffixes[0] = suffixes[0].trim();
					suffixes[1] = suffixes[1].trim();
					names[3] = names[0];
					names[4] = names[1];
					names[5] = names[2];
				}
			}
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			   
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		/*
		 * done multiowner parsing
		 */
	}
	
	public static String cleanOwnerFLWaltonTR2(String s){
		s = s.replaceAll("\\____$", "");
		if (!s.contains("DIOCESE")){
			s = s.replaceAll("\\b(?:AS )?(TRUSTEEE?S?)(?: OF(?: THE)?)?(?: FOR)?\\b", " $1 ").trim();
			s = s.replaceAll("\\bOF\\b", "");
		}
		s = s.replaceAll("\\s+AS\\b", "");
		s = s.replaceAll("\\s+THE\\b", "");
		
		s = s.replaceAll("\\bFOR$", "");
		s = s.replaceAll("^UNDER\\b", "");
		s = s.replaceAll("\\bDR(\\.|\\b)", "");
		s = s.replaceAll(";", " & ");
		s = s.replaceAll("!!!!!", "");
		s = s.replaceAll("\\^", "&");
		//s = s.replaceAll("^&\\s*", "");
		s = s.replaceAll("\\bAND\\b", "&");
		s = s.replaceAll("\\s{2,}", " ");
		return s.trim();
	}
	
	public static boolean shouldBeSplit(String s){
		String[] split = s.split("&|!!!!!");
		for(int i = 0;i<split.length; i++){
			if (split[i].split("\\s+").length>2){
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Splits names separated by \\d+/\\d+<br>
	 * It also NameCleaner.fixLikeScotishNames
	 * @param a - lines of names
	 * @param separator
	 * @return
	 */
	public static ArrayList<String> fixNameFLWaltonTR(ArrayList<String> a, String fullString){
		ArrayList<String> tmpStr = new ArrayList<String>();

		int index = 0;
		tmpStr.add("");
		int i = -1;
		String prevTmps = "";
		if (/*StringUtils.indexesOf(fullString, "&").size() > 1 
				|| */StringUtils.indexesOf(fullString, "!!!!!").size() >0){
			for(i = 0; i< a.size(); i++){
				String tmpS = a.get(i).replaceAll("!!!!!( ?&|;)", "!!!!!");
				String[] splits;
				String splitString = "!!!!!";
				if (!NameUtils.isCompany(tmpS) 
						&& shouldBeSplit(tmpS) 
						&& !prevTmps.equals("") &&!prevTmps.endsWith("!!!!!")
						&& !tmpS.startsWith("C/O")){
					splitString = "(!!!!!|&)";
				}
				splits = tmpS.split("\\s*" + splitString + "\\s*");
				if (tmpS.matches("^(C/O|TRUSTEES).*")
					|| (splits.length>1 
						&& twoWordsNoInitial.matcher(splits[0]).matches()
						&& twoWordsNoInitial.matcher(tmpStr.get(index)).matches()
						&& !initialWordSuffix.matcher(splits[0]).matches())
					|| GenericFunctions.FMiL.matcher(tmpStr.get(index)).matches()
					|| (!prevTmps.matches(".*(&|!!!!!).*") && !tmpS.matches(".*(&|!!!!!).*"))
					|| (!prevTmps.equals("") && tmpS.startsWith(prevTmps.split("\\s+")[0])
							&& !prevTmps.matches(".*(&|!!!!!).*"))){
					tmpStr.add("");
					index++;
				}
				if (initialWordSuffix.matcher(tmpS).matches()){
					index--;
				}
				tmpStr.set(index, tmpStr.get(index) + " " + splits[0]);
				index++;
				
				
				for (int ii = 1; ii<splits.length; ii++){
					tmpStr.add(splits[ii]);
					index++;
				}
				if ((!tmpS.matches(".*(!!!!!|&|;|EST)$"))
					|| tmpStr.get(index-1).startsWith("THE")){
					index--;
				} else {
					tmpStr.add(" ");
				}
				prevTmps = tmpS;
			}
		} else {
			tmpStr = a;
		}
		return tmpStr;
	}
	
	public static ArrayList<String> splitNamesFLWaltonTR(ArrayList<String> a){
		ArrayList<String> tmpN = new ArrayList<String>();
		for (int i = 0; i < a.size(); i++){
			String[] split = a.get(i).split("\\s*,\\s*");
			for (int j = 0; j < split.length; j++){
				tmpN.add(split[j]);
			}
		}
		return tmpN;
	}
	
		public static String cleanCompanyName(String s){
			s = NameCleaner.removeUnderscore(s);
			s = s.replaceAll("^UNDER\\b", "");
			s = s.replaceAll("\\s*OF$", "");
			s = s.replaceAll("^\\s*OF\\b", "");
			s = s.replaceAll("!!!!!", "");
			s = s.replaceAll("(&|AND)$", "");
			s = s.replaceAll("^AND\\b", "");
			s = s.replaceAll("\\s{2,}", "");
			return s.trim();
		}
	
		/**
		 * it will also NameCleaner.fixLikeScotishNames
		 * @param a
		 * @param separator
		 * @return
		 */
		public static ArrayList<String> mergeNamesFLWaltonTR(ArrayList<String> a, String separator){
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
					if (/*(prev.matches(".*(&|AND)$") && twoWords.matcher(current).matches() && a.size() == 2 && !current.contains("____"))
						||*/ (oneWord.matcher(current).matches())
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
	public static String preCleanOwnerFLWaltonTR(String s){
		//s = s.replaceAll("\\bEST(ATE)?( OF)?\\b", "&");
		s = s.replaceAll("\"", "");
		//s = s.replaceAll("\\-$", "");
		s = s.replaceAll("^ODOM\\b", "");
		s = s.replaceAll("\\bD/B/A\\b", "");
		s = s.replaceAll("\\bD\\.M\\.D\\.", "");
		s = s.replaceAll("\\bJTWRO(S)?\\b", "");
		s = s.replaceAll("\\bH&W\\b", "");
		s = s.replaceAll("\\bOF TRUSTS$", "");
		s = s.replaceAll("\\bPERS REPRESENTATIVES\\b", "!!!!!");
		//s = s.replaceAll("\\bREV\\b", "");
		Matcher ma = initialWordSuffix2.matcher(s);
		if (ma.matches()){
			s = ma.replaceFirst("$1 !!!!! $4");
		}
		if (StringUtils.indexesOf(s, "&").size()>1 
			&& !s.endsWith("&")
			&& !s.startsWith("&")){
			s = s.replaceAll("&", "!!!!!");
			
		}
		s = s.replaceAll("QUALIFIED PERSONAL RESID TRUST", "");
		s = s.replaceAll(", (LLC|INC)\\b", " $1 ");
		s = s.replaceAll("\\bCPT\\b", "");
		if (!NameUtils.isCompany(s)){
			s = s.replaceAll("\\bAND( T)?\\b", "!!!!!");
		}
		if (s.startsWith("&") ){
			s = s.replaceAll("&", "!!!!!");
		}
		if (s.endsWith(",")){
			s = s.substring(0, s.length()-1);
			s = s.replaceAll("&", "!!!!!");
			
		}
		if (s.endsWith("&")
			&& StringUtils.indexesOf(s, '&').size()>1){
			s = s.substring(0, s.length()-1) + "!!!!!";
		}
		if (!s.contains("TRUST CO")){
			s = s.replaceAll("\\b(TRUST|LC)( &)?\\b", "$1 !!!!!");
		}
		s = s.replaceAll("(,|;|\\bAKA\\b)", "!!!!!");
		s = s.replaceAll("\\bMR(\\.)?\\b", "");
		s = s.replaceAll("\\bMS\\b", "");
		s = s.replaceAll("\\bAS C/?O\\b-?", "");
		s = s.replaceAll("\\bA MINOR\\b", "");
		s = s.replaceAll("\\bGUARDIAN(S)?( OF| FOR)?\\b", "!!!!!");
		//s = s.replaceAll("\\bC/O\\b", "&");
		s = s.replaceAll("\\bAS BISHOP OF\\b", "");
		s = s.replaceAll("\\bHEIRS( OF)?\\b", "");
		s =s.replaceAll("\\bTHEIR\\b", "");
		s = s.replaceAll("CO (TRUSTEES) OF REVOCABLE TRUST", " $1 ");
		if (!s.contains("DIOCESE")){
			s = s.replaceAll("\\b(?:AS )?(?:CO )?(TRUSTEEE?S?)(?: OF)?(?: FOR)?\\b", " $1 !!!!!");
		}
		s = s.replaceAll("\\s*AS JOINT TENANTS\\s*", "");
		//s = s.replaceAll("(\\b)JR AND\\b", "$1JR !!!!!");
		s = s.replaceAll("\\B\\+\\B", "&");
		s = s.replaceAll("\\(?\\d+/\\d+( ?INT)?\\)?", "!!!!!");
		s = s.replaceAll("\\bEACH\\b", "");
		s = s.replaceAll("\\(?[\\d\\.]+(\\s+)?%\\s*(INT\\b\\s*)?\\)?", "!!!!!");
		///s = s.replaceAll("\\b\\d+/\\d+(\\s*INT)?\\b", "");
		//s = s.replaceAll(";", " & ");
		//s = s.replaceAll(",", " & ");	
		s = s.replaceAll("([A-Z-]+ [A-Z]+ [A-Z-]+ & [A-Z-]+ [A-Z]+ [A-Z-]+) CO\\b", "$1");
		s = s.replaceFirst("^&\\s*", "");
		s = s.replaceFirst("^CO\\-", "");
		s = s.replaceFirst("\\bPERS(ONAL)? REP(S)?\\b", "");
		s = s.replaceAll("\\s{2,}", " ").trim();		
		return s;
	}
		
	public static String cleanOwnerFLWaltonTR(String s) {

		s = s.replaceAll("\\bAS C/?O\\b-?", "");
		s = s.replaceAll("\\bAS (TRUSTEES?)", " $1");
		s = s.replaceAll("\\bC/O\\b", "&");
		s = s.replaceAll("\\bHEIRS( OF)?\\b", "&");
		s = s.replaceAll("\\b\\d+/\\d+(\\s*INT)?\\b", "");
		s = s.replaceAll(";", " & ");
		s = s.replaceAll(",", " & ");	
		s = s.replaceAll("([A-Z-]+ [A-Z]+ [A-Z-]+ & [A-Z-]+ [A-Z]+ [A-Z-]+) CO\\b", "$1");
		s = s.replaceAll("\\s{2,}", " ").trim();
		return s;		
	}

	@SuppressWarnings("rawtypes")
	public static void legalTokenizerFLWaltonTR(ResultMap m, String legal) throws Exception {		
		   //initial cleanup of legal description
		   legal = legal.replaceFirst("^\\d*[/\\.]?\\d+\\s*Acres\\s*", "");
		   legal = legal.replaceAll("\\b[SWEN]{1,2}\\s*(\\d+ )?\\d*[\\./]?\\d+(\\s*FT)?(\\s*OF)?\\b", "");
		   legal = legal.replaceAll("\\b[SWEN]{1,2} OF\\b", "");
		   legal = legal.replaceAll("\\d*\\.\\d+", "");
		   legal = legal.replaceAll("\\bLONG DESC\\b", "");
		   legal = legal.replaceAll("\\bBEING\\b", "");
		   legal = legal.replaceAll("\\bRECD IN\\b", "");
		   legal = legal.replaceAll("\\bCOR( OF)?\\b", "");
		   legal = legal.replaceAll("\\bBEG AS( [SWEN]{1,2})?\\b", "");
		   legal = legal.replaceAll("\\bLYING\\b", "");
		   legal = legal.replace("(", "");
		   legal = legal.replace(")", "");
		   legal = legal.replaceAll("\\bPRCL \\d+ OF\\b", "PARCEL");
		   legal = legal.replaceAll("(\\d+)\\s*h\\s*(\\d+)", "$1-$2");
		   legal = legal.replaceAll("\\bPHI\\b", "PH I");
		   legal = legal.replaceAll("(\\d+)\\s*\\+\\s*(\\d+)", "$1 $2");
		   legal = legal.replaceAll("\\bPARCOR\\b", "PARC OR");	  
		   legal = legal.replaceAll("\\bLESS( RD)?( ROW)?\\b", "");
		   legal = legal.replaceAll("\\bS/[DB]\\b", "SUBDIV");
		   legal = GenericFunctions.replaceNumbers(legal);	
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics
		
		   legal = legal.replaceAll("\\sTHRU\\s", "-");	    
		   legal = legal.replaceAll("(\\d)\\s+-", "$1-");
		   legal = legal.replaceAll("-\\s+(\\d)", "-$1");
		   legal = legal.replaceAll("\\+", "&");	
		   legal = legal.replaceAll("\\b(\\d+) AND (\\d+)\\b", "$1 & $2");
		   legal = legal.replaceAll("\\s{2,}", " ").trim();
		   	   	   
		   // extract and remove lot from legal description	
		   String patt="(?:\\d+|[A-Z](?=[\\s&$-]))";
		   String lot = ""; // can have multiple occurrences	   
		   Pattern p = Pattern.compile("\\bLOTS? ("+patt+"(?:\\s*[&\\s-]\\s*"+patt+")*)\\b");
		   Matcher ma = p.matcher(legal);
		   while (ma.find()){
			   String lotTemp = ma.group(1);
			   lotTemp = lotTemp.replace('&', ' ');
			   if (lotTemp.matches("\\d+(-\\d+){2,}"))
				   lotTemp = lotTemp.replace('-', ' ');
			   lot = lot + " " + lotTemp;
			   legal = legal.replaceFirst(ma.group(0), "LOT ");
		   }
		   lot = lot.replaceAll("\\s{2,}", " ").trim();
		   if (lot.length() != 0){
			   lot = LegalDescription.cleanValues(lot, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract cross refs from legal description
		   //extract cross refs  before extracting sec-twn-rng because of B2377
		   patt = "(\\d+)(?:\\s*[/-]\\s*| PG? )(\\d+(?:(?: TO |-)\\d+)?)";
		   p = Pattern.compile("\\bOR\\s*("+patt+"(?: & "+patt+")*)\\b");
		   ma = p.matcher(legal);
		   List<List> bodyCR = new ArrayList<List>();
		   while (ma.find()){
			   Pattern p2 = Pattern.compile(patt);
			   Matcher ma2 = p2.matcher(ma.group(1));
			   while (ma2.find()){
				   List<String> line = new ArrayList<String>();		   
				   line.add(ma2.group(1).replaceFirst("^0+(.+)", "$1"));
				   line.add(ma2.group(2).replaceAll("\\b0+(\\d+)", "$1").replace(" TO ", "-"));
				   bodyCR.add(line);
			   }
			   legal = legal.replace(ma.group(0), "OR ");		   
		   } 
		   
		   if (!bodyCR.isEmpty()){		  		   		   
			   String [] header = {"Book", "Page"};		   
			   Map<String,String[]> map = new HashMap<String,String[]>();		   
			   map.put("Book", new String[]{"Book", ""});
			   map.put("Page", new String[]{"Page", ""});
			   
			   ResultTable cr = new ResultTable();	
			   cr.setHead(header);
			   cr.setBody(bodyCR);
			   cr.setMap(map);		   
			   m.put("CrossRefSet", cr);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }	  
		   
		   // extract and remove sec-twn-rng from legal description
		   String sec = "";
		   boolean foundSTR = false;
		   p = Pattern.compile("(?<!\\bLOTS )\\b(\\d+)-(\\d+[SWEN]?)-(\\d+[SWEN]?)\\b");
		   ma = p.matcher(legal);
		   foundSTR = ma.find();
		   if (!foundSTR){
			   p = Pattern.compile("SEC (\\d+) (\\d+[SWEN]?) (\\d+[SWEN]?)\\b");
			   ma = p.matcher(legal);
			   foundSTR = ma.find();			   
		   }
		   if (foundSTR){
			   sec = sec + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");		   
			   m.put("PropertyIdentificationSet.SubdivisionTownship", ma.group(2));
			   m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(3));
			   legal = legal.replaceFirst(ma.group(0), "SEC ");
		   }
		   
		   p = Pattern.compile("\\bSEC (\\d+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   sec = sec + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");
			   legal = legal.replaceFirst(ma.group(0), "SEC ");
		   }	
		   sec = sec.trim();
		   if (sec.length() != 0){		   
			   sec = LegalDescription.cleanValues(sec, true, true);
			   m.put("PropertyIdentificationSet.SubdivisionSection", sec);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and remove block from legal description
		   String blk = "";
		   p = Pattern.compile("\\bBLK ([A-Z]|\\d+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   blk = blk + " " + ma.group(1);
			   legal = legal.replaceFirst(ma.group(0), "BLK ");		   
		   }
		   if (blk.length() != 0){
			   blk = LegalDescription.cleanValues(blk, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionBlock", blk);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and remove unit from legal description
		   String unit = ""; // can have multiple occurrences
		   patt = "(?:\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)";
		   p = Pattern.compile("\\b(?:UNIT|APT) ("+patt+"(?:[/-]"+patt+")*)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   unit = unit + " " + ma.group(1);		   
			   legal = legal.replaceFirst(ma.group(0), "UNIT ");		   
		   }
		   if (unit.length() != 0){
			   unit = LegalDescription.cleanValues(unit, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		
		   // extract and remove building # from legal description
		   p = Pattern.compile("\\bBLDG (\\d+|[A-Z](?:-?\\d+)?)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			   legal = legal.replaceFirst(ma.group(0), "BLDG ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and remove phase from legal description
		   p = Pattern.compile("\\bPH(?:ASE)?S? (\\d+[A-Z]?|[A-Z])\\b");
		   ma = p.matcher(legal);
		   String phase = "";		   
		   if (ma.find()){
			   phase = ma.group(1);
			   if ("I".equals(phase)){
				   phase = "1";
			   }
			   m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			   legal = legal.replaceFirst(ma.group(0), "PHASE ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   	   	   
		   // extract and remove plat book & page from legal description
		   p = Pattern.compile("\\bPB (\\d+)(?:\\s*-\\s*| PG )(\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.PlatBook", ma.group(1).replaceFirst("^0+(.+)", "$1"));
			   m.put("PropertyIdentificationSet.PlatNo", ma.group(2).replaceFirst("^0+(.+)", "$1"));
			   legal = legal.replace(ma.group(0), "PB ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   } 	   
		   	   	 	   	   	   
		   // extract subdivision name
		   String subdiv = "";	   
		   String patt1 = "\\b(?:(?:UNIT|LOT|BLK|OR|PHASE|BLDG|PARCEL(?: \\d+)?)\\b(?:\\s?,)?(?: OF)?\\s*)";
		   String patt2 = "\\b(?:(?:PHASE|DESC AS|OR|UNIT|BLK|PB)\\.?\\b|,|$)";
		   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   String legalTemp = legal;
		   legal = legal.replaceFirst("(.+?) SUBDIV\\b.*", "$1");	  
		   if (legal.matches(".*\\bCONDO(MINIUM)?.*")){
			   p = Pattern.compile(".*"+patt1+"\\s*(.*?)\\s*\\b(?:A )?CONDO(?:MINIUM)?\\b.*");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   subdiv =  ma.group(1).trim();
			   }
		   }
		   if (subdiv.length() == 0 && legalTemp.matches(".*\\b(LOTS?|UNIT|PARCEL|BLK)\\b.*")){
			   p = Pattern.compile("(.*?)"+patt1+"+$");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   subdiv = ma.group(1).trim();
			   }
			   p = Pattern.compile("(.*?)\\s*UNRECD?\\b\\.?.*");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   subdiv = ma.group(1).trim();
				   p = Pattern.compile(patt1+"+\\s*(.*)");		   
				   ma = p.matcher(subdiv);
				   if (ma.find()){
					   subdiv = ma.group(1).trim();				   
				   }
			   }		   
			   p = Pattern.compile(patt1+"+\\s*(.+?)\\s*"+patt2+".*");
			   if (subdiv.length() == 0){		   
				   ma = p.matcher(legal);			   
			   } else {		   
				   ma = p.matcher(subdiv);
			   }
			   if (ma.find()){
				   subdiv = ma.group(1).trim();
			   } else {
				   p = Pattern.compile(patt1+"*\\s*(.*?)\\s*"+patt2+".*");
				   ma.usePattern(p);
				   ma.reset();
				   if (ma.find()){
					   subdiv = ma.group(1).trim();
				   }
			   }
			   p = Pattern.compile(patt1+"+\\s*(.*)");
			   ma = p.matcher(subdiv);
			   if (ma.find()){
				   subdiv = ma.group(1).trim();
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
			   subdiv = subdiv.replaceFirst("\\s*\\bDECLARATION OF\\s*$", "");
			   subdiv = subdiv.replaceFirst("^\\s*\\d+(ST|ND|RD|TH)\\.? ADD\\.? TO\\b\\s*", "");
			   subdiv = subdiv.replaceFirst("\\s\\d+(?:ST|ND|RD|TH)\\.? ADD\\b\\.?.*", "");
			   subdiv = subdiv.replaceFirst("\\s*\\bADD TO .*$", "");
			   subdiv = subdiv.replaceFirst("\\s*\\bDEG \\d+.*$", "");
			   subdiv = subdiv.replaceFirst("\\s*\\bAND [^\\s]+ FRACTIONAL .*$", "");		   
			   subdiv = subdiv.replaceFirst("^\\s*AT\\b", "");
			   subdiv = subdiv.replaceFirst("\\bAT\\s*$", "");
			   subdiv = subdiv.replaceFirst("^\\s*OF\\b", "");		   
			   subdiv = subdiv.replaceAll("\\bREVISED PLAT\\b", "");
			   subdiv = subdiv.replaceFirst("\\bDESC IN\\s*$", "");
			   subdiv = subdiv.replaceFirst("& INT IN COMMON AREA.*$", "");
			   subdiv = subdiv.replaceFirst("\\bON THE SURVEY.*$", "");		   
			   subdiv = subdiv.replaceFirst("\\bREPLAT\\b", "");
			   subdiv = subdiv.replaceAll("\\bUNRECD?\\b\\.?", "");
			   subdiv = subdiv.replaceFirst("\\bAND$", "");
			   subdiv = subdiv.replaceAll("\\bFT( TO)?\\b", "");
			   subdiv = subdiv.replaceAll("\\bPOB\\b", "");
			   subdiv = subdiv.replaceAll("\\s{2,}", " ").trim();
			   if (subdiv.length() != 0){
				   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				   if (legal.matches(".*\\bCONDO(MINIUM)?\\b.*"))
					   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }
		   }
		}
	
	public static void legalFLWaltonTR(ResultMap m, long searchId) throws Exception {
		   
		   String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");
		   if (StringUtils.isEmpty(legal))
			   return;
		   legalTokenizerFLWaltonTR(m, legal);
		}
	
	
		 	
}

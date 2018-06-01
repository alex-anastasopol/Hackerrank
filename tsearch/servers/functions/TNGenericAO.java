package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class TNGenericAO {
	private static final String	notAvailablePattern	= "(?i)N\\s*/?\\s*(?:A|R)|/(?:A|R)?";
	   public static void ownerTNGenericAO(ResultMap m, long searchId) throws Exception {
		   
		   String ownerNameAddr = (String) m.get("tmpOwnerNameAddress");
		   if (ownerNameAddr == null || ownerNameAddr.length() == 0)
			   return;
		   
		   Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
	       DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, search.getSearchType() == Search.PARENT_SITE_SEARCH);
	       String stateCountySiteType = search.getSa().getStateCounty() + dataSite.getSiteTypeAbrev();
		   
		   // owner name and address are on consecutive lines, separated by <br>, transformed into \s\s
		   String[] lines = ownerNameAddr.split("\\s{2}");
		   String temp = "" ;
		   if ("TNWarrenTR".equals(stateCountySiteType) || "TNMadisonTR".equals(stateCountySiteType)) {
			   temp = ownerNameAddr;	//there is only the name for TNWarrenTR and TNMadisonTR
		   } else {
			   Matcher poBox = GenericFunctions.poBox.matcher(lines[1]);
			// PMB=Private mailbox, PSC=Postal Service Center mailbox
			if (lines[1].matches("\\s*\\d+(.*)") || lines[1].matches("(?i)PMB\\s+\\d+(.*)") ||
					lines[1].matches("(?i)PO\\s+BX\\s+\\d+(.*)") || poBox.find() || lines[1].matches("(?i)\\s*PSC\\s*\\d*\\s+BO?X.*")) {
				   temp = lines[0];
			   } else {
				   if ((lines[0].endsWith("TRUSTEE") && !lines[1].matches("\\A\\s*%.*")) || 
					   (lines[0].matches("(?is)[\\w+\\s]{3,}") && lines[1].matches("(?is)[\\w+\\s]{2,}"))){
					   		temp = lines[0] + " @@ " + lines[1];
				   } else {
					   temp = lines[0] + " " + lines[1];
				   }
			   }
			
				
		   }
		   String s = temp;
		   s = s.replaceAll("\\s+\\(?LE\\)?\\s+", " ");
		   s = s.replaceAll("ET\\s+UX", "ETUX");
		   s = s.replaceAll("ET\\s+AL", "ETAL");
		   s = s.replaceAll("ETAK", "ETAL");
//		   s = s.replaceAll("(?is)\\s*&\\s*WIFE\\b", " AND");
//		   s = s.replaceAll("(?is)\\bWIFE\\b", "");
		   
		   s = s.replaceAll("(?is)\\A\\s*(\\w+\\s+\\w+\\s+ETUX\\s+\\w+)\\s+(LIVING\\s+TR(?:UST)?)\\b", "$1 @@ $1 $2");//SMITH TOMMY ETUX ALBERTA LIVING TRUST
		   
		   s = s.replaceAll("\\b(.+)\\s+(LIVING\\s+TR)\\s+(TRUSTEES)\\b", "$1 $3 @@ $1 $2");//SMITH HAROLD D ETUX DESSIE LIVING TR TRUSTEES
		   s = s.replaceAll("\\bETUX\\s+(\\w+\\s+LIVING\\s+TR(?:UST)?)\\b", " #@# $1");//SMITH HAROLD D ETUX DESSIE LIVING TR TRUSTEES
		   
		   s = s.replaceAll("(.+)\\s+(ETUX)\\s+((?:REVOCABLE\\s+)?LIVING\\s+TRUST)\\b", "$1 $2 @@ $1 $3");//SMITH THOMAS A ETUX  REVOCABLE LIVING TRUST

		   s = s.replaceAll("\\b(TRUSTEE\\s+OF\\s+THE.*)\\s+TR\\b", "$1 TRUST");
		   s = s.replaceAll("\\b(TRUSTEE)\\s+OF\\s+THE\\s+", "$1 @@ ");
		   
		   s = s.replaceAll("\\bTR\\s+", " TR @@ ");
		   s = s.replaceAll("\\b(LIVING)\\s+TR\\b", " $1 TRUST"); 
		   s = s.replaceAll("%", " @@ ");
		   s = s.replaceAll("(?is)\\bD\\s*/\\s*B\\s*/\\s*A\\b", " @@ ");
		   s = s.replaceAll("\\s+", " ");

		   s = s.replaceAll("C/O", "@@");
		   if (s.matches("\\s*(.+)\\s*&\\s+([A-Z]+)\\s+([A-Z]{1})?\\s+&\\s+(.+)")) {
			   s = s.replaceAll("\\s*&\\s+([A-Z]+)\\s+([A-Z]{1})?\\s+&", " & $1 $2 &");
			   s = s.replaceAll("&", "@@");
		   }
		   
		   s = s.replaceAll("\\b(ETAL\\s+\\w+)\\s+&\\s+(\\w+\\s+\\w+\\s*$)", " $1 @@ $2");
		   s = s.replaceFirst("\\bTR(?:USTEE)?\\b(\\s*@@\\s*)&?(.*?\\bTR(?:USTEE)?\\b)", "$1$2");
		   
		   Matcher matchWifeHusband = Pattern.compile("(?is)(\\w+)([^@]+)@@\\s+\\b(?:AND|&)\\b\\s+\\b(?:WIFE|HUSBAND)\\b\\s+([-'\\w\\s\\.]+)").matcher(s);
		   if (matchWifeHusband.find()) {
			  // TN Anderson, WILSON DONALD RAY @@ AND WIFE ROSA LEE; 
			  // proper format should be: ALLEN MICHAEL T @@ AND WIFE LISA H ALLEN
			  String lastN = matchWifeHusband.group(1).trim();
			  String wifeHusbandN = matchWifeHusband.group(3).trim();
			  if (wifeHusbandN.contains(lastN)) {
				  s = s.replaceFirst("\\s+\\bAND\\b\\s+\\b(?:WIFE|HUSBAND)\\b\\s+", " ");
			  } else {
				  wifeHusbandN += " " + lastN;
				  s = lastN + " " + matchWifeHusband.group(2).trim() + " @@ " + wifeHusbandN; 
			  }
		   }
		   s = s.replaceAll("(?is)\\s*&\\s*(?:WIFE|HUSBAND)\\b", " AND");
		   s = s.replaceAll("(?is)\\b(?:WIFE|HUSBAND)\\b", "");
		   
		   if (s.matches("(?is)(.+)&\\s*(.+(?:\\bJOINT?\\b)?\\s+\\bLIV(?:ING)?\\b\\s+\\bTR(?:UST)?\\b)")) { //task 9311
			   s = s.replaceFirst("(?is)(.+)&\\s*(.+(?:\\bJOINT?\\b)?\\s+\\bLIV(?:ING)?\\b\\s+\\bTR(?:UST)?\\b)", "$1 @@ $2");
			   s = s.replaceFirst("(?is)\\bLIV(?:ING)?\\b\\s+\\bTR\\b", "LIVING TRUST");
		   }
		   
		   if ("TNWarrenTR".equals(stateCountySiteType) || "TNMadisonTR".equals(stateCountySiteType) || "TNMauryAO".equals(stateCountySiteType)) {
			   if (!NameUtils.isCompany(s)) {
		   			s = s.replaceAll(" & ", " @@ ");
		   	   }
		   } else {
			   if (!s.contains("ETAL")) {
		   			s = s.replaceAll(" & ", " @@ "); //for SumnerAO
		   	   }
		   }
		 //TNGreene: 099B-A-099F-012.00--000
		 //TNCarter: 028K-B-028K-015.00--000
		   if (s.matches("(?is)(\\w+)\\s(\\w+(?:\\s+\\w)?)\\s+(\\w+(?:\\s+\\w)?)\\s+(\\1)")){
			   s = s.replaceAll("(?is)(\\w+)\\s(\\w+(?:\\s+\\w)?)\\s+(\\w+(?:\\s+\\w)?)\\s+(\\1)", "$1 $2 @@ $3 $4"); 
		   }
		   
		   s = s.replaceFirst("(MILLER)\\s+ETAL\\s+(MILLER)","$1 & $2 ");//TNMontgomeryAO: MILLER & MILLER PROPERTIES LLC
		   String[] owners ;
		   String[] own = null;
		   owners = s.split("@@");
		  
		@SuppressWarnings("rawtypes")
		List<List> body = new ArrayList<List>();
		   String[] names = {"", "", "", "", "", ""};
		   String[] suffixes, type, otherType;
				
		   String ln="";
				
		   for (int i=0; i<owners.length; i++){
			   String ow = owners[i];

			   if (i == 0){
				   names = StringFormats.parseNameNashville(ow, true);
				   if (NameUtils.isNotCompany(names[2])){
					   ln = names[2];
					}
				} 
			   // SMITH CHRISTY MICHELLE & LENORD H ETUX NELLIE L
			   if (ow.matches("&\\s*([A-Z]{1,})\\s+([A-Z]{1,})\\s+ETUX\\s+([A-Z]{1,})\\s+([A-Z]{1,})")) {
				   ow = ow.replaceAll("(.+)(\\s+ETUX.*)", "& $1 " + ln + "$2");
			   }
			   if ((i > 0) && (!temp.contains("%"))) {
				   if (ow.matches("\\s*([A-Z]{1,})\\s+([A-Z]{1,})\\s+&\\s+([A-Z]{1,})\\s+([A-Z]{1,}(\\s+[A-Z]{1,})?)")) {
					   own = ow.split("&");
					   try {
						   for (int j = 0; j < own.length; j++) {
							   ow = own[j];
							   if (j == 0) {
								   names = StringFormats.parseNameDesotoRO(ow, true);
							   } else {
								   String[] name = StringFormats.parseNameDesotoRO(ow, true);
								   names[3] = name[0];
								   names[4] = name[1];
								   names[5] = name[2];
								   if (names[2].length()==1) {	//J R & TERESA WILLIAMS 
									   names[1] = names[2];
									   names[2] = names[5];
								   }
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (ow.matches("\\s*([A-Z]{1,})\\s+&\\s+([A-Z]{1,})\\s+([A-Z]{1,})")) {
						ow = ow.replaceAll("([A-Z]{1,})\\s+&\\s+([A-Z]{1,})\\s+([A-Z]{1,})", "$2 $3 & $1");
						names = StringFormats.parseNameDesotoRO(ow, true);
					} else if (!NameUtils.isCompany(ow) && ow.matches("\\s*([A-Z]+)\\s+&\\s+([A-Z]+)")) {
						String[] coOwner = ow.split("&");
						names[0] = "";
						names[2] = coOwner[0].trim();
						names[5] = coOwner[1].trim();
					} else {
						if (NameUtils.isCompany(ow)) {
							names = new String[6];
							names[2] = ow.trim();
							names[0] = names[1] = names[3] = names[4] = names[5] = "";  
						} else {
							String[] split = ow.replaceFirst("(?is)(ETUX|ETAL|EVIR)", "").trim().split("\\s+");
							int len = split.length;
							//JACKSON CHERYL J
							if (len>2 && split[len-1].length()==1 && split[0].length()>1 && LastNameUtils.isLastName(split[0])) {
								names = StringFormats.parseNameNashville(ow, true);
							} else {
								names = StringFormats.parseNameDesotoRO(ow, true);
							}
						}
					}
						
				} else {
					names = StringFormats.parseNameNashville(ow, true);
				}
			   if (!StringUtils.isEmpty(ln) && names[2].length() == 1 && names[1].length() == 0 && ow.matches("\\s*\\b\\w{2,}\\s+\\w\\s*\\b")){
				   names[1] = names[2];
				   names[2] = ln;
			   }
			   if (i > 0 && names[0].length()>0 && names[1].length()==0 && 
					   (names[2].equalsIgnoreCase("ETAL") || (LastNameUtils.isNotLastName(names[2]) && FirstNameUtils.isFirstName(names[2])) )){
				   names[1] = names[2];
				   names[2] = ln;
				}
			   if (i > 0 && names[2].length()==1 ){
				   names[1] += " " + names[2];
				   names[1] = names[1].trim();
				   names[2] = ln;
				}
			   if (i > 0 && LastNameUtils.isNotLastName(names[2]) && LastNameUtils.isLastName(names[0]) && NameUtils.isNotCompany(names[2])){
				   String aux = names[2];
				   names[2] = names[0];
				   names[0] = aux;
				}
			   if (i > 0 && NameUtils.isNotCompany(names[2]) && names[0].length()==0 && names[1].length()==0){//DAVIDSON DONALD LEE & GRACE
				   names[0] = names[2];
				   names[2] = ln;
				}
			   
			   //TNMooreAO 043--043-042.00--000 
			   if (i > 0 && ow.trim().matches("(?is)\\w+\\s+[A-Z]\\s+\\w+") 
					   && names[0].length() == 1 && LastNameUtils.isLastName(names[1]) && FirstNameUtils.isFirstName(names[2])){
				   names = StringFormats.parseNameDesotoRO(ow, true);
			   }
				names[2] = names[2].replaceAll("#@#", "AND");
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
		        GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
		   
		   
		   // the name is on the 1st line and possible on the 2nd => needs to identify what kind of info we have on the 2nd line: spouse name or Address
		   /*int idx = -1;
		   for (int i=lines.length-1; i>=0; i--){
			   if (lines[i].matches(".*,\\s*[A-Z]{2}\\b.*")){
				   idx = i-2;
				   break;
			   }
		   }
		   String l1 = cleanNameTNGenericAO(lines[0]);
		   String l2 = "";

		   boolean l2HasSpouse = false;
		   if (idx == 1){ // we have co-owner
			   l2HasSpouse = lines[1].matches("\\s*ETUX\\b.+");
			   l2 = cleanNameTNGenericAO(lines[1]);
		   }

		   String address = lines[idx+1].trim();	   
		   String city = lines[idx+2].trim();

		   boolean addLN = false;
		   boolean useLFN = false;
		   if (l2.length() == 0){		// no co-owner
			   l1 = l1.replaceFirst("\\s*&$", "");
		   } else {
			   String all = l1 + " " + l2;
			   if ((l2.matches("\\w+") && !l1.contains("&")) || ((all.contains("LIVING") || all.contains("REVOCABL")) && all.contains("TRUST"))){
				   l1 = all;
				   l2 = "";
			   // if L1 contains owner name and spouse, then ignore L2, because ATS currently doesn't support more than 2 owners
			   } else {
				   Pattern p = Pattern.compile("(.+) & (.+)");
				   Matcher ma = p.matcher(l1);
				   if (ma.matches() && !ma.group(2).matches("\\bL/?E\\b")){		 				   
					   l2 = l2.replaceAll("\\bC/O\\b", "&");
					   
					   // if L2 contains AND inside => parse L1+(L2 until AND) as L F M (e.g. L1=WOODY KIMBERLY AND HOLDER  and  L2=ALBERT & JOYCE)		   
					   if (l2.matches("([A-Z]{2,}(?: [A-Z])?) & [A-Z]{2,}(?: [A-Z])?")){
						   l1 = ma.group(1);
						   l2 = ma.group(2) + " " + l2;
						   useLFN = true;
					   } else if (l2.matches("([A-Z] )?[A-Z]{2,} & [A-Z]{2,}( [A-Z])? [A-Z]{2,}")){ //(e.g. L1=JACKSON HAROLD & JOHN S	L2=COPELAND C/O KOJAC DCS )
						   l1 = ma.group(1);
						   l2 = ma.group(2) + " " + l2;
					   } else {
						   l1 = l1.replaceFirst("\\s+L/?E$", "");
						   if (l2.matches("\\w+")){				// L1=JACKSON CLEVE J & YVONNE C and L2=ROBINSON 
							   l1 = ma.group(1);
							   if (l2.matches("\\w")){			// L1=JACKSON HOWARD B & LAUREN and L2=E
								   addLN = true;
							   }
							   l2 = ma.group(2) + " " + l2;						   
//						   } else {								// don't ignore the 3rd owner, bug #2746
//							   l2 = "";
						   }
					   }
				   } else if (l2.matches(".*\\bC/O\\b.*") || l1.endsWith("C/O")){
					   l2 = l2.replaceFirst("^&\\s*", "");
					   l2 = l2.replaceFirst("\\bC/O\\b\\s*", "");
					   l1 = l1.replaceFirst("&? ?C/O$", "");
					   l1 = l1.replaceFirst("\\s*&$", "");
				   } else if (l1.endsWith(" L/E") || l1.endsWith(" LE")){
					   l1 = l1.replaceFirst("\\s*L/?E$", "");
					   l2 = l2.replaceFirst("\\s+RE(M|V)$", "");
					   l2 = l2.replaceFirst("^RE(M|V)\\s+", "");
					   l2 = l2.replaceFirst("\\s+AND( OTHERS)?$", "");
					   l2 = l2.replaceFirst("^AND\\s*", "");
					   
					   // if L2 contain 2 owners, extract only one, because ATS currently doesn't support more than 2 owners
					   p = Pattern.compile("([A-Z]{2,}(?: [A-Z])? )& [A-Z]{2,}(?: [A-Z])? ([A-Z]{2,})");
					   ma = p.matcher(l2);
					   if (ma.find()){
						   l2 = ma.group(1) + ma.group(2);
					   }				   
				   // if L2 starts with AND HUSBAND or WIFE or simply AND, then add owner last name from L1 to spouse name in L2, if not already contained 	   
				   } else {			   
					   p = Pattern.compile("& (?:(?:HIS )?WIFE|(?:HER )?HUSBAND) (.+)");
					   ma = p.matcher(l2);
					   if (ma.find()){				   
						   l2 = ma.group(1);
						   if (!l2.matches(".*\\b[A-Z]{2,}( [A-Z])? [A-Z]{2,}\\b.*")){
							   addLN = true;
						   }
					   } else if (l1.endsWith("&") || l2.startsWith("&")){
						   l1 = l1.replaceFirst("\\s*&$", "");
						   l2 = l2.replaceFirst("^&\\s*", "");
						   if (l2.equals("OTHERS")){
							   l2 = ""; 
						   } else if (l2HasSpouse && !l2.matches(".*\\b[A-Z]{2,} [A-Z] [A-Z]{2,}\\b.*")){ //bug #2744, L1=GREEN DANIEL R 	L2=ETUX MARY ANN 
							   addLN = true;
						   } else if (!l2.matches(".*\\b[A-Z]{2,}( [A-Z])? [A-Z]{2,}\\b.*")){
							   addLN = true;
						   }
					   } else if (l2.matches("[A-Z]+ & [A-Z]{2,}( [A-Z]+)?")){ // L1=JACKSON LLOYD and L2=DONALD & REGINA					   
						   l1 = l1 + " " + l2;
						   l2 = "";
					   }
				   }
			   }		   
		   }
		   
		   // apply L F M name tonenizer for L1
		   String names[] = StringFormats.parseNameNashville(l1);
		   if (names[5].length() == 0 && names[1].contains(names[2])){        // fix for Lake County, Main ST 400, owner TURNER ELSIE TURNER BILL
			   l1 = l1.replaceAll(names[2], "& " + names[2]).replaceFirst("^&", "");
			   names = StringFormats.parseNameNashville(l1);
		   }   	   
		   m.put("PropertyIdentificationSet.OwnerFirstName", names[0]);
	       m.put("PropertyIdentificationSet.OwnerMiddleName", names[1]);
	       m.put("PropertyIdentificationSet.OwnerLastName", names[2]);
	       m.put("PropertyIdentificationSet.SpouseFirstName", names[3]);
	       m.put("PropertyIdentificationSet.SpouseMiddleName", names[4]);
	       m.put("PropertyIdentificationSet.SpouseLastName", names[5]);
	       
	       String oln = names[2];
	       // apply F M L name tokenizer for L2
	       if ((l2.length() != 0) && (names[5].length() == 0)){
	    	   names = null;
	    	   String suffix = "";
	    	   if (useLFN){
	    		   names = StringFormats.parseNameNashville(l2);
	    	   } else {
		    	   if (addLN && !l2.contains(" " + oln)){
		    		   l2 = l2 + " " + oln;
		    	   }
		    	   Pattern p = Pattern.compile("(.+)\\s+(JR|SR|[IV]+)$");
				   Matcher ma = p.matcher(l2);			   
				   if (ma.find()){
					   l2 = ma.group(1);
					   suffix = " " + ma.group(2);
				   }
		    	   names = StringFormats.parseNameDesotoRO(l2);			   
	    	   }
	    	   if (names != null){
	    		   m.put("PropertyIdentificationSet.SpouseFirstName", names[0]);
			       m.put("PropertyIdentificationSet.SpouseMiddleName", (names[1] + suffix).trim());
			       m.put("PropertyIdentificationSet.SpouseLastName", names[2]);
	    	   }
	       }*/
	      
	   }

	   public static void legalTNGenericAO(ResultMap m, long searchId) throws Exception {
		   
		   String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		   
		   String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		   String additionalDescr = (String)m.get("tmpAdditionalDescr");
		   if (legal == null || legal.length() == 0){		   
			   if (additionalDescr != null && additionalDescr.length() != 0)
				   m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), additionalDescr);
			   return;
		   }
		   
		   String lot = (String) m.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName());
		   if (lot == null)
			   lot = "";
		   lot = lot.replaceAll("(?is)\\bNOTE\\b", "").trim();
		   
		   String block = (String) m.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName());
		   if (block == null)
			   block = "";
		   
		   String platBook = (String) m.get(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName());
		   if (platBook == null)
			   platBook = "";
		   
		   String platPage = (String) m.get(PropertyIdentificationSetKey.PLAT_NO.getKeyName());
		   if (platPage == null)
			   platPage = "";		  
		   
		   // extract subdivision/condominium name 
		   String pattern1 = "(.*?)(\\b(S/D|SUBD?|SUBDIVISION|S\\s*D|RE\\s?SUB?|BLOCK|LOTS?|UNIT|PH(ASE)?|SEC(?:T(?:ION)?)?|CONDO(MINIUM)?S?|BK|TRACT)\\b)";
		   String pattern2 = "(.*?)(\\b(S/D|SUBD?|SUBDIVISION|S\\s*D|RE\\s?SUB?|BLOCK|LOTS?|UNIT|PH(ASE)?|SEC(?:T(?:ION)?)?|CONDO(MINIUM)?S?|BK|TRACT)\\b|(#|\\bNO)?\\s*\\d).*";
		   String subdivName = "";
		   if ("robertson".equalsIgnoreCase(crtCounty)||"mcminn".equalsIgnoreCase(crtCounty)) {
			   subdivName =  legal.replaceFirst(pattern1, "$1");
		   } else {
			   subdivName =  legal.replaceFirst(pattern2, "$1");
		   }
		   subdivName = subdivName.replaceFirst("[-\\.,]+\\s*$", "");
		   subdivName = subdivName.replaceFirst("^SD\\s+", "");
		   subdivName = subdivName.trim();
		   
		   legal = legal.replaceAll("(\\w)\\s+([-&,])\\s+(\\w)", "$1$2$3");
		   legal = legal.replaceAll("\\bTWO\\b", "2");
		   legal = legal.replaceAll("\\bONE\\b", "1");
		   legal = GenericFunctions.replaceNumbers(legal);
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersIfLastExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics
		   legal = legal.replaceAll("(?:\\bNO|#)\\s*(\\d)", "$1");
		   
		   // extract lot # from legal descr, if not already extracted from Lot field
		   Pattern p = Pattern.compile("\\bLOTS?((?:\\s+[-&,\\w]+)+)\\b");
		   Matcher ma = p.matcher(legal);
		   if(lot.length() == 0){
			   if (ma.find())
				   lot = ma.group(1);
			   else if (additionalDescr != null && additionalDescr.length() != 0){			   
				   Matcher ma2 = p.matcher(additionalDescr);
				   if (ma2.find())
					   lot = ma2.group(1);
			   }
		   }	   
		   lot = lot.replaceAll("[-,&]$", "");
		   lot = lot.replaceAll("\\bPTS?\\.?", "").replaceAll("\\bOF\\b", "").trim();
		   	   
		   // extract block # from legal descr, if not already extracted from Block field	   
		   if(block.length() == 0){
			   p = Pattern.compile("\\bBLOCK\\s+([-&,\\w]+)\\b");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find())
				   block = ma.group(1);
		   }
		  	   
		   
		   boolean specialCaseSection = false;
		   String phase = ""; 
		   
		   // extract section from legal description
		   String section = "";
		   p = Pattern.compile("\\b(SECT|SEC|SC|S )\\s*([-&,\\d]+[A-Z]?)\\b"); // B3826, B4014
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find())
		   {
			   section = ma.group(2);
			   legal = legal.replaceFirst(ma.group(0),"");
			   subdivName = legal;
		   }
		   else {
			   //let's try and find a case like this TRIPLE CROWN 2 S1B-2
			   p = Pattern.compile("\\bS(\\d+\\w*).*");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()) {
				   section = ma.group(1);
				   specialCaseSection = true;
			   }
			   else {
				   //when no "SECT" is available.... check B2047
				   String tempLegal = legal.replace(subdivName, "").trim();
				   p = Pattern.compile("^(\\d+\\w*)\\b+.*");
				   Matcher tempMa = p.matcher(tempLegal);
				  
				   if(tempMa.find()){
					   String value = tempMa.group(1);
					   if(additionalDescr!=null && additionalDescr.contains("UNIT")){
						   phase = value;
					   } else{
						   if (!value.matches("(?is)\\b(\\d)(?:ST|ND|RD|TH)\\b")){
							   section = value;
						   }
					   }
				   }
			   }
		   }
		   
		   // extract unit from legal description
		   String unit = "";
		   p = Pattern.compile("\\bUNIT\\s+([-&,\\w]+)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find())
			   unit = ma.group(1);
		   
		   // extract phase from legal description
		   
		   p = Pattern.compile("\\bPH(?:ASE)?\\s*+([-&,\\w]+)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find())
		   {
			   phase = ma.group(1).replaceAll("^[-]+", "");
			   legal = legal.replaceFirst(ma.group(0),"");
			   subdivName = subdivName.replaceFirst(ma.group(0),"");	
		   }
		   else{
			   //when no "PH... " is available.... check B2047
			   if(!legal.contains(subdivName) || (!section.equals("") && specialCaseSection)){
				   //this happens for examples like: WILLOUGHBY STATION X
				   //or when we have the section and we need the phase
				   String tempSubdivName = legal.replaceFirst("(.*?)(\\b(S/D|SUBD?|SUBDIVISION|SD|RE\\s?SUB?|BLOCK|LOTS?|UNIT|PH(ASE)?|SECT?|CONDO(MINIUM)?S?|BK|TRACT)\\b|(#|\\bNO)?\\s*\\d).*", "$1");
				   tempSubdivName = tempSubdivName.replaceFirst("[-\\.,]+\\s*$", "");
				   tempSubdivName = tempSubdivName.replaceFirst("^SD\\s+", "");
				   tempSubdivName = tempSubdivName.trim();
				   
				   subdivName = tempSubdivName;	//this should eliminate the cases where the phase is written in roman numbers
				   
				   String tempSection = legal.replace(tempSubdivName, "").trim();
				   p = Pattern.compile("^(\\d+\\w*)\\b+.*");
				   Matcher tempMa = p.matcher(tempSection);
				  
				   if(tempMa.find()) 
					   phase = tempMa.group(1);
			   }
		   }
		   
		   // extract tract from legal description
		   String tract = ""; 
		   p = Pattern.compile("\\bTRACT\\s+([-&,\\w]+)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find())
			   tract = ma.group(1);
		   
		   // extract plat book & page from legal description
		   if(platBook.length() == 0){
			   p = Pattern.compile("\\bBK\\s+(\\w+)");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   platBook = ma.group(1);
			   } else{
				   p = Pattern.compile("\\bPB\\s*(\\w+)");
				   ma.usePattern(p);
				   ma.reset();
				   if (ma.find()){
					   platBook = ma.group(1);
				   }
			   }
		   }
		   if(platPage.length() == 0){
			   p = Pattern.compile("\\bPG\\s+(\\w+)");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   platPage = ma.group(1);
			   }  else{
				   p = Pattern.compile("\\bP\\s*(\\d+)");
				   ma.usePattern(p);
				   ma.reset();
				   if (ma.find()){
					   platPage = ma.group(1);
				   }
			   }
		   }
		   
		   // trim zeros from the beginning of the legal descriptors
		   lot = lot.replaceFirst("^0+(\\w)", "$1"); 
		   block = block.replaceFirst("^0+(\\w)", "$1");
		   section = section.replaceFirst("^0+(\\w)", "$1");
		   unit = unit.replaceFirst("^0+(\\w)", "$1");
		   phase = phase.replaceFirst("^0+(\\w)", "$1");
		   tract = tract.replaceFirst("^0+(\\w)", "$1");
		   
		   lot = lot.replace("&", ",").replace(",", ", ");
		   block = block.replace("&", ",").replace(",", ", ");
		   section = section.replace("&", ",").replace(",", ", ");
		   unit = unit.replace("&", ",").replace(",", ", ");
		   phase = phase.replace("&", ",").replace(",", ", ");
		   tract = tract.replace("&", ",").replace(",", ", ");
		   
		   // save extracted values in InfSets
		   if (subdivName.length() != 0){
			   //subdivName = subdivName.replaceAll("(?is)\\bAT\\s+.*", "");
			   subdivName = subdivName.replaceAll("(?is)(\\bPB|&)\\s*$", "");
			   
			   subdivName = subdivName.trim();
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivName);
			   if (legal.matches(".*\\bCONDO(MINIUM)?S?\\b.*"))
				   m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdivName);
		   }
			if (lot.length() != 0) {
				if (lot.trim().matches("(?i)LIST")) {
					Pattern lotPattern = Pattern.compile("(?is)\\bLO?TS?\\s*(\\d+\\s*(?:(?:[,\\s&-])\\d*)*)");
					Matcher lotMatcher = lotPattern.matcher(StringUtils.transformNull(additionalDescr));
					String lotFromAdditional = "";
					if (lotMatcher.find()) {
						lotFromAdditional = lotMatcher.group(1).replaceAll("[&,]", " ").replaceAll("\\s{2,}", " ").trim();
					}
					if (!lotFromAdditional.isEmpty()) {
						if (lotFromAdditional.contains("-")) {
							lotFromAdditional = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(lotFromAdditional);
						}
					} else {
						lot = lot.replaceFirst("(?i)LIST", "");
					}
					m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lotFromAdditional);
				} else {
					m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
				}
			}
		   if (block.length() != 0){
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		   }	   
		   if (section.length() != 0){
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
		   }
		   if (unit.length() != 0){
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
		   }
		   if (phase.length() != 0){
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		   }
		   if (tract.length() != 0){
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
		   }
		   if (platBook.length() != 0){
			   if (platBook.trim().matches(notAvailablePattern)) {				  
				   m.remove(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName());
			   }
			   else {
				m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), platBook);
			   }
		   }
		   if (platPage.length() != 0){
			   if (platPage.trim().matches(notAvailablePattern)) {
				   m.remove(PropertyIdentificationSetKey.PLAT_NO.getKeyName());
			   }
			   else {
				   m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), platPage);
			   }
		   }
	   }	
 
	   public static void addressTNGenericAO(ResultMap resultMap, long searchId) {

			String address = (String) resultMap.get("tmpPropertyAddress");
				
			if (StringUtils.isEmpty(address))
				return;

			address = address.replaceAll("\\bOFF\\s*$", "");
			address = address.replaceAll("\\b(HWY)\\s+(\\d+)([NSWE])\\s*$", "$1 $2 $3");
			if (address.matches("(?is)\\b(HWY|CO\\s+RO?A?D)\\s+(\\d+)\\s*-\\s*(\\d+)\\s*$")){
				address = address.replaceAll("(?is)\\b(HWY|CO\\s+RO?A?D)\\s+(\\d+)\\s*-\\s*(\\d+)\\s*$", "$3 $1 $2");
			}
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			
			address = address.replaceAll("\\b(\\d+)\\s*&\\s*\\d+\\s*$", "$1").trim();
			
			resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
			
		}
	  
	@SuppressWarnings("deprecation")
	public static String cleanNameTNGenericAO(String name){
		   name = name.replaceAll("\\d/\\d INT\\b", "");
		   name = name.replaceAll("\\bMRS\\.?\\s*", "");
		   name = name.replaceAll("[{}]", "");
		   name = name.trim();
		   name = name.replaceAll("\\s{2,}", " ");
		   name = StringFormats.unifyNameDelimWithoutRemove(name);
		   return name;
	   }
	   
	   
	   public static String[][] cleanupBookPage (String[][] body) {
		   String[][] cleanedBody = body;
		   
		   int len = cleanedBody.length;
			if (len != 0) {
				for (int i = 0; i < len; i++){
					if (cleanedBody[i][4].matches(notAvailablePattern)) {
						cleanedBody[i][4] = "";
					} else {
						cleanedBody[i][4] = org.apache.commons.lang.StringUtils.stripStart(cleanedBody[i][4], "0");
					}
					if (cleanedBody[i][6].matches(notAvailablePattern)) {
						cleanedBody[i][6] = "";
					} else {
						cleanedBody[i][6] = org.apache.commons.lang.StringUtils.stripStart(cleanedBody[i][6], "0");
					}
				}
			}
			
		   return cleanedBody;
	   }
	   
	   public static void bookPageTNWilsonAO(ResultMap m,long searchId) throws Exception {
		   
		   ResultTable pageSet = (ResultTable) m.get("SaleDataSet");
		   List<List<String>> bodyPage = new ArrayList<List<String>>();
		   String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		   String page2 = "";
		   
		   if (pageSet != null){
			   String body[][] = pageSet.getBodyRef();
			   body = cleanupBookPage(body);
				//B3855
			   GenericFunctions.removeDuplicateElements(body, 4, 6, 12);  //body[x][4]=book, body[x][6]=page;
				int len = body.length;
				if (len != 0){
					for (int i = 0; i < len; i++){
						String page = body[i][6];
						List<String> line = new ArrayList<String>();

						if (page.contains("&")){
							page2 = page;
							page = page.replaceAll("(\\d{1,})(\\d{1})&(\\d{1})","$1$2");
							page2 = page2.replaceAll("(\\d{1,})(\\d{1})&(\\d{1})","$1$3");
							body[i][6] = page;
							
	    	        		for (int j = 0; j<13; j++){
	    	        			if (j == 6){
	    	        				line.add(page);
	    	        			} else{
	    	        				line.add(body[i][j]);
	    	        			}
	    	        		}
	    	       
	    	        		bodyPage.add(line);
	    	        		line = new ArrayList<String>();
	    	        		for (int j = 0; j < 13; j++){
	    	        			if (j == 6){
	    	        				line.add(page2);
	    	        			} else{
	    	        				line.add(body[i][j]);
	    	        			}
	    	        		}
	    	        		bodyPage.add(line);
						} else {  
							if (!("".equals(body[i][0]) && "".equals(body[i][4]) && "".equals(body[i][6]))){
				        		for (int j = 0; j < 13; j++)//{
				        			if (j == 4 || j == 6){
				        				if (crtCounty.equals("Montgomery")){//task 9177
				        					line.add(body[i][j].replaceFirst("^V", "").replaceFirst("^0+", ""));
				        				} else if (crtCounty.equals("Anderson") || crtCounty.equals("Union")){
				        					line.add(body[i][j].replaceFirst("[-]+", ""));
				        				} else{
				        					line.add(body[i][j].replaceFirst("^0+", ""));
				        				}
				        			} else {
				        				line.add(body[i][j]);
				        			}
				        		//}
				        		bodyPage.add(line);
							}
						}
					}
				}  
		   }
		   if (!bodyPage.isEmpty()){
		        String [] header = {"Sale Date", " ", "Price", " ", "Deed Book", " ", "Page"," ", "Vac/Imp"," ", "Type Instrument"," ", "Qualification"};
		        Map<String,String[]> map = new HashMap<String,String[]>();
				map.put("InstrumentDate", new String[]{"Sale Date", ""});
				map.put("SalesPrice", new String[]{"Price", ""});
				map.put("Book", new String[]{"Deed Book", ""});
				map.put("Page", new String[]{"Page", ""});
				map.put("DocumentType", new String[]{"Type Instrument", ""});
				
				ResultTable rt = new ResultTable();
				rt.setHead(header);
				rt.setBody(bodyPage);
				rt.setMap(map);
				m.put("SaleDataSet", rt);
		   }
	   }
}

package ro.cst.tsearch.servers.functions;

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
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;


public class FLSumterTR {
	
//	   public static String cleanOwnerFLSumterTR(String s){
//		   /* s e pe mai multe linii, prima/primele 2(sau 3) sunt numele ownerilor, urmatoarele adresa 
//		    * (care incepe fie cu [nr], fie cu [PO BOX si numar/ sau BOX si numar] (PO poate sa fie si P O, si P.O.),fie cu [RT sau RR, urmat de un numar BOX]
//		    *  , fie cu LOT urmat de nr
//		    * */
//		   if (s.matches("(?is).*\\s*((?:(?:(?:RT|RR)\\s*\\d+\\s*)?(?:\\s*P\\s*O)?\\s*BOX)|LOT)\\s*(?:[A-Z]|\\d+(?:\\s*[A-Z])?)\\s*.*") || 
//				   s.matches("(?is).*BOX\\s*\\d+.*"))
//		     s = s.replaceFirst("(?is)(?:(?:(?:(?:RT|RR)\\s*\\d+\\s*)?(?:\\s*P\\s*O)?\\s*BOX)|LOT)\\s*(?:[A-Z]|\\d+(?:\\s*[A-Z])?).+","");
//		   if (!s.matches("(?is).*(\\(?\\d+/\\d+\\s*(?:INT)?\\)?).*")) 
//			 s = s.replaceFirst("(?is)\\d+.*","");
//		   else
//			 s = s.replaceFirst("(?is)(.*\\(?\\d+/\\d+\\s*(?:INT)?\\)?[^\\d]+).*","$1");   
//			   
//		   s = s.replaceAll("\\bDBA\\b",", ");
//		   s = s.replaceAll("\\bTRUST\\b\\s*&\\s*([A-Z]{3,}\\s*+[^\\d]+)","TRUST , $1"); //SMITH DEBRA A REVOC TRUST &   SANDRA L LEATHERMAN  REVOCABLE TRUST 
//		   s = s.replaceFirst("\\b(ET)\\s+(AL|UX|VIR)\\b","$1$2");
//		   s = s.replaceFirst("\\b\\(?JT?\\s*(?:/?W?/?)?RO?S\\s*\\)?\\b","");
//		   //Pin:D17H091, ANDERSON DOUGLAS H TRUSTEE 1/2  ANDERSON JANET G TRUSTEE 1/2 
//		   if (s.matches("(?is)(.*)(\\(?\\d+/\\d+\\s*(?:INT)?\\)?\\s*[A-Z]{3,}\\s*[A-Z]+.*)"))
//			   s = s.replaceFirst("(.*)(\\(?\\d+/\\d+\\s*(?:INT)?\\)?\\s*[A-Z]{3,}\\s*[A-Z]+.*)","$1, $2");
//		   s = s.replaceAll("\\(?\\d+/\\d+\\s*(?:INT)?\\)?","");
//		   s = s.replaceAll("\\s*\\(?\\bH/W\\b\\)?","");
//		   s = s.replaceAll("\\b&?\\s*WIFE\\b","");
//		   s = s.replaceAll("(?is)\\bMOORE\\s*\\*\\s*",", ");
//		  
//		   if (!s.contains("(LE)"))		//doar (LE) ramane, posibil sa apara si (TIC)-in caz de buguri adaug aici
//			   s = s.replaceAll("\\(.*\\)", "");
//		   //cazuri de exceptie
//		   s = s.replaceFirst("\\bATTN:",", ATTN:");//PIN:XRR4665, SMITH RAILROAD COMPANY   ATTN: DANIEL ANALT  => "ATTN: DANIEL ANALT" apare la Last
//		   s = s.replaceFirst("\\bGERRETZ,\\s*",",GERRETZ "); //PIN:F30B161, THOMAS LLOYD ROBERT & GERRETZ,	APRIL L (TIC) 
//		   s = s.replaceFirst("\\bLOUISE,\\s*",",LOUISE ");//PIN:D16E088, SMITH FRED O & LOUISE, CHANDRA  (JTWROS)
//		   s = s.replaceFirst("&\\s*BOAN\\b",", BOAN"); //PIN:G05-038, THOMAS NATHAN L JR&ANGELA DAWN  & BOAN JOSHUA (TIC)
//		   s = s.replaceFirst("(?is)\\s*MAE\\s*INEZ\\s*PHILLIP"," MAE, PHILLIP INEZ"); //PIN:T09-019, WILLIAMS AMOS & JULIA MAE  INEZ PHILLIP (JTWROS)
//		   s = s.replaceFirst("BARBARA\\s*(.*PARK)","BARBARA, $1"); //PIN:N09A060, SMITH CHARLES SR & BARBARA   SUMTER GARDENS TRAILER PARK
//		   s = s.replaceFirst("\\s*&\\s*\\b(KELAITA)\\b,",", $1"); //B3730
//		   
//		   s = s.replaceAll("\\s,\\s*\\Z", "");
//		   s = s.trim();
//		   return s;
//	   }

//	   public static String exchangeNames(String s1, String s2) throws Exception {
//		  String s = s2 + " " + s1;
//		  return s; 
//	   }	   
	   
//	   public static String partyNamesTokenizerFLSumterTR(String s) throws Exception {
//		   s = s.replaceAll("\\s{3,}"," ");	
//		   s = s.replaceAll("\\n"," ");
//			
//		   s = s.replaceAll("%\\s*([A-Z']+\\s*(?:[A-Z]{1,}\\s)?)\\s*([A-Z]{3,})", ", $2 $1");
//		   //PIN:D35F032, SMITH VINCENT DEPAUL & MARY  JANE EDDY- 
//		   s = s.replaceFirst("([A-Z'-]{3,})(\\s*[^&]+&\\s*)([A-Z]{3,}(?:\\s*(?:[A-Z]{1}|[A-Z]{3,})))\\s*([A-Z]+\\s*)-\\s*\\Z","$1$2$1 $3 $4");
//		   //PIN:G26AA009, SMITH BEATRICE K  C/O P. DAVID SMITH 
//		   if (s.matches("(?is).*C/O\\s*((?:[A-Z]{1}\\.?|[A-Z]{3,})?\\s*[A-Z]{3,})\\s*([A-Z'-]{3,}).*")){
//			   String s1=s, s2=s;
//			   s1 = s1.replaceFirst("(?is).*C/O\\s*((?:[A-Z]{1}\\.?|[A-Z]{3,})?\\s*[A-Z]{3,})\\s*([A-Z'-]{3,}).*", "$1");
//			   s2 = s2.replaceFirst("(?is).*C/O\\s*((?:[A-Z]{1}\\.?|[A-Z]{3,})?\\s*[A-Z]{3,})\\s*([A-Z'-]{3,}).*", "$2");
//			   s1 = exchangeNames(s1, s2);
//			   s = s.replaceFirst("C/O\\s*[^,&]+",", " + s1);
//		   }
//		   if ((s.contains(" FKA ")) || (s.contains(" F/K/A ")) || (s.contains(" AKA ")) || (s.contains(" A/K/A "))){
//			   String first = s, afterFKA = s;
//			   //caz de exceptie -> PIN:D10A017, SMITH LYNDA JOY A/K/A  MERKEL LINDA JOY (LE) (al doilea nume are LINDA cu "I", nu cu "Y"
//			   afterFKA = afterFKA.replaceFirst("LINDA","LYNDA");   
//			   first = first.replaceFirst("(?is)[A-Z]{3,}\\s*(.+)\\s(?:FKA|F/K/A|AKA|A/K/A)\\s*(.*)","$1");
//			   afterFKA = afterFKA.replaceFirst("(?is)[A-Z]{3,}\\s*(.+)\\s(?:FKA|F/K/A|AKA|A/K/A)\\s*(.*)","$2");
//			   afterFKA = afterFKA.replaceAll(first, "");
//			   afterFKA = afterFKA.replaceFirst("(?is)\\s*([A-Z]{3,})\\s*(\\s.+)", "$1 " + first + "$2");
//			   s = s.replaceFirst("(?is)(.+)\\s(?:FKA|F/K/A|AKA|A/K/A)\\s*(.+)","$1 , " + afterFKA);
//		   }
//           if (s.matches("(?is)(.+)\\bAND\\b(.+)")){
//        	   String s1 = s;
//        	   boolean b = false;
//        	   s1 = s1.replaceAll("(?is)(.+)\\bAND\\b(.+)","$2"); //Pin:D29C019, SMITH LEE ANN FKA LEE ANN KEIN   AND JUDY SMITH (J/ROS)  
//        	   if (s1.matches("[A-Z]{3,}\\s*[A-Z](?:[A-Z])?\\s*[A-Z]{3,}")){
//        		   s1 = s1.replaceFirst("([A-Z]{3,}\\s*[A-Z](?:[A-Z])?)\\s*([A-Z]{3,})", "$2 $1");
//        		   b = true;
//        	   }
//        	   else
//        		   if (s1.matches("(?is)\\s*[A-Z]{3,}\\s*[A-Z]{3,}\\s*.*")){	
//        			   String p1 = s1;
//            		   p1 = p1.replaceFirst("([A-Z]{3,})\\s*([A-Z]{3,})", "$2");
//            		   if (!FirstNameUtils.isFirstName(p1)){
//        			   	   s1 = s1.replaceFirst("([A-Z]{3,})\\s*([A-Z]{3,})", "$2 $1");
//        			   	   b = true;
//            		   }
//        		   }
//        	   if (b) 
//        		   s = s.replaceFirst("\\bAND\\b([^&]+)",", " + s1);
//        	   else
//        		   s = s.replaceFirst("\\bAND\\b", ", ");
//           }
//           s = s.replaceAll("\\s*&\\s*([A-Z]{3,}\\s+[A-Z]{3,}\\s+[A-Z]{1}\\s*(?:JR|SR|II|III|IV)?)",", $1");
//           
//           if (s.indexOf(',') == -1 && s.matches("(?is).+&\\s*[A-Z]{3,}\\s*\\Z")){ //PIN:N16E023, SMITH EDWARD & LILLIAN
//    		   // pin D15D429 : KENT JEFFERY V & GERALYN M CHARLES R & PHYLLIS (Bug 5549)
//    		   Matcher m = Pattern.compile("(\\w{2,})(\\s+\\w{2,}\\s+\\w\\s+&\\s+\\w{2,}\\s+\\w)(\\s+\\w{2,}\\s+\\w\\s+&\\s+\\w{2,})")
//    		   		.matcher(s); 
//    		   if(m.find()){
//    			   if(NameUtils.isNotCompany(s)){
//    				   s = m.replaceFirst("$1$2,$1$3");
//    			   }
//    		   }
//        	  
//        	   return s;
//           }
//           else
//           if (!s.matches("(?is).*((?:&|,)\\s*[A-Z]{3,}\\s+[A-Z]{3,}\\s{1,3}[A-Z]{1}\\b).*")){
//        	   String name1 = s, name2 = s;
//        	   name1 = name1.replaceFirst("[^,]+,","");
//        	   name1 = name1.replaceAll("(?is)[^&]+&\\s*([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s*|[^&A-Z][^A-Z&]).*","$1");
//        	   name2 = name2.replaceFirst("[^,]+,","");
//        	   name2 = name2.replaceAll("(?is)[^&]+&\\s*([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s*|[^&A-Z][^A-Z&]).*","$2");
//        	   //orice afara de cazuri ca: Pin:T09-039, WILLIAMS CURTIS JR & DARRELL  WILLIAMS & JACQUELYN CHILDS
//        	   if (!s.matches("(?is).*(&\\s*([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s*|[^&A-Z][^A-Z&])){2,}.*")){
//	        	   if (LastNameUtils.isLastName(name1)){
//	        		   name1 = ", " + name1;
//	        		   s = s.replaceAll("&\\s*([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s*|[^&A-Z][^A-Z&])", name1 + " " + name2);
//	        	   }
//	        	   else
//	        		   if (LastNameUtils.isLastName(name2)){
//	        			   name1 = ", " + exchangeNames(name1,name2);
//	        			   s = s.replaceAll("&\\s*([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s*|[^&A-Z][^A-Z&])", name1);
//	        		   }
//        	   }
//        	   else {
//        		   name1 = s;
//        		   name1 = name1.replaceFirst(".*(&\\s*([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s+|[^&A-Z][^A-Z&]).*)","$1");
//        		   while (name1!="") {
//        			   String s1 = name1;
//        			   s1 = s1.replaceFirst("\\s*&\\s*(([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s+|[^&A-Z][^A-Z&]|\\Z))(.*)", "$1");
//        			   name1 = name1.replaceFirst("\\s*&\\s*([A-Z]{3,}\\s+[A-Z]{3,}(?:\\s+|[^&A-Z][^A-Z&]|\\Z))(.*)", "$2");
//        			   String p1=s1,p2=s1;
//        			   p1 = p1.replaceAll("(?:\\s*&\\s*)?([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s+|[^&A-Z][^A-Z&]|\\Z).*", "$1");
//        			   p2 = p2.replaceAll("(?:\\s*&\\s*)?([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s+|[^&A-Z][^A-Z&]|\\Z).*", "$2");
//        			   if (LastNameUtils.isLastName(p1) && LastNameUtils.isLastName(p2)) {  //gen BENJAMIN CURTIS
//        				   s1 = s1.replaceAll("(?:\\s*&\\s*)?([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s*|[^&A-Z][^A-Z&]|\\Z).*",", " + p2 + " " + p1);
//        			   }
//        			   else if (LastNameUtils.isLastName(p1)) {
//    	        		   p1 = ", " + p1;
//    	        		   s1 = s1.replaceAll("(?:\\s*&\\s*)?([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s*|[^&A-Z][^A-Z&]|\\Z).*", p1 + " " + p2);
//    	        	   }
//    	        	   else if (LastNameUtils.isLastName(p2)) {
//    	        			   p1 = exchangeNames(p1,p2);
//    	        			   s1 = s1.replaceAll("(?:\\s*&\\s*)?([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s*|[^&A-Z][^A-Z&]|\\Z).*",", "+ p1);
//    	        		   }
//        			  s = s.replaceFirst("(?is)&\\s*([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s*|[^&A-Z][^A-Z&])", s1);
//        			 // name1 = name1.replaceFirst("(?is).*&\\s*([A-Z]{3,}\\s+[A-Z]{3,}(?:\\s+|[^&A-Z][^A-Z&]|\\Z))(.*)", "$2");
//        			  if (!name1.matches("(?is).*&\\s*(([A-Z]{3,})\\s+([A-Z]{3,})(?:\\s*|[^&A-Z][^A-Z&]|\\Z))(.*)"))
//        				  name1= "";
//        			 	  
//        		   }
//        	   }
//           }
//           
//           //Pin:N18-082, SMITH TIMMY L.,DEWEY L., & RUBY M (JTWROS)
//           s = s.replaceAll(",\\s*&\\s*([A-Z]{3,}\\s*(?:(?:JR|SR)|[A-Z]{1}\\.?\\s*(?:JR|SR)?))", " & $1");
//           s = s.replaceAll(",\\s*([A-Z]{3,}\\s[A-Z]{1}\\.?(?:\\s*JR|\\s*SR)?)\\s*([,&])","& $1 $2"); 
//           
//		   return s;
//	   }
	   
//	@SuppressWarnings("rawtypes")
//	public static void partyNamesFLSumterTR(ResultMap m, String s) throws Exception {
//		   
//		String[] owners = s.split(",");  //ownerii sunt separati prin virgula 
//		List<List> body = new ArrayList<List>();
//		String[] names = {"", "", "", "", "", ""};
//		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
//		   
//		for (int i=0; i<owners.length; i++){
//			boolean lifeEst = false, containsAttn = false, containsRev=false;
//			String ow = owners[i], aux="";
//			if (ow.contains("ATTN:")) { //PIN:XRR4665, SMITH RAILROAD COMPANY   ATTN: DANIEL ANALT  => "ATTN: DANIEL ANALT" apare la Last
//				names[2] = owners[i];	
//				containsAttn = true;
//			}
//			if (ow.matches(".*\\b((?:LIFE\\s*)?ESTATE)\\b\\s*") 
//					|| ow.matches("(?is).*\\(LE\\)\\s*") || ow.matches("(?is).*\\bREV(?:OC)?\\b.*")) {
//				aux = ow;
//				aux = aux.replaceAll(".*\\b((?:LIFE\\s*)?ESTATE)\\b","$1");
//				aux = aux.replaceAll("(\\(LE\\))","$1");
//				aux = aux.replaceAll("\\b(REV(?:OC)?)\\b","$1");
//				lifeEst = true;
//				if (ow.matches("(?is).*\\bREV(?:OC)?\\b.*")){
//					containsRev = true;
//				}
//		  	}
//			if (lifeEst){ 				// in parseNameNashville se stergea ESTATE sau LIFE ESTATE
//				names[2] = owners[i];
//		  	} else if (!containsAttn) {
//		  		if (ow.contains(" DIRKS")){   //B3730
//		  			ow = ow.replaceFirst("(.*) DIRKS","DIRKS $1");
//		  		}
//		  		names = StringFormats.parseNameNashville(ow, true);
//		  	}
//			if (ow.matches("(.+)\\s*&\\s*[A-Z]{3,}\\s*[A-Z]{3,}\\s*") && !ow.matches("(.+)\\s*&\\s*[A-Z]{3,}\\s*\\Z")) {
//				names[4] = names[3];
//				names[3] = names[5];
//				names[5] = names[2];
//		  	} 
//	
//			types = GenericFunctions.extractAllNamesType(names);
//			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
//			suffixes = GenericFunctions.extractNameSuffixes(names);        
//			if (containsRev){
//				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, true, NameUtils.isCompany(names[5]), body);
//			} else if (!ow.contains("ATTN:")){
//				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
//			} else {
//				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, false, NameUtils.isCompany(names[5]), body);
//			}	
//		}
//		GenericFunctions.storeOwnerInPartyNames(m, body, true);
//		}

	 public static void parseNames(ResultMap resultMap, String ownerCell) {
		Vector<String> companyExpr = new Vector<String>();
		Vector<String> excludeWords = new Vector<String>();
		excludeWords.add("GULF");
		excludeWords.add("ST");
		
		String[] lines = ownerCell.split("\n");
		String lastLine = lines[lines.length - 1].trim();
		String[] lastLines = new String[0];
		boolean lastLineIsName = false;
		if(lastLine.matches("[A-Z]+\\s*,[A-Z\\s]+")) {
			lastLine = fixLines(lastLine);
			lastLines = lastLine.split("\\s*\n\\s*");
			lastLineIsName = true;
		}
		
		String properInfo = fixLines(ownerCell);
		lines = properInfo.split("\\s*\n\\s*");
		
		String[] linesBeforeAddr = GenericFunctions.removeAddressFLTR(lines, excludeWords, new Vector<String>(), 2, Integer.MAX_VALUE, true);
		ArrayList<String> nameLines_ = new ArrayList<String>();
		for(int i = 0; i < linesBeforeAddr.length; i++) {
			nameLines_.add(linesBeforeAddr[i]);
		}
		if(lastLineIsName) {
			for(int i = 0; i < lastLines.length; i++) {
				nameLines_.add(lastLines[i]);
			}
		}
		String[] nameLines = nameLines_.toArray(new String[linesBeforeAddr.length + lastLines.length]);
		
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = cleanName(nameLines[i]);
		}
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = nameLines[i].replaceAll("\\A\\s*&\\s*", "").replaceAll("\\s*&\\s*\\z", "").trim();
			if (nameLines[i].indexOf("&") > 0) {// e.g. MIRANDA MIGUEL A & CARMEN C for Account No. G16ED018
				String[] names = nameLines[i].split("&");
				if (names[0].trim().replaceAll("[^\\w\\s]", "").matches("\\w{2,}\\s+(\\w\\s+\\w{2,}|\\w{2,}\\s+\\w)")
						&& names[1].trim().replaceAll("[^\\w\\s]", "").matches("\\w{2,}\\s+\\w")) {
					nameLines[i] = nameLines[i] + "[LFM]";
				}
			}
		}
		
		COFremontAO.genericParseNames(resultMap, nameLines, null, companyExpr, 
				false, COFremontAO.ALL_NAMES_LF, -1);
	 }
	 
	 private static String fixLines(String info) {
		 String properInfo = info;
		 properInfo = properInfo.trim().replaceFirst("(?is)\n\\s*\n.*", "").trim(); // fix for the case when "prior taxes due" is present in the owner info
		 properInfo = properInfo.trim().replaceAll("(?is)\\bTRE\\b", "\nTRUSTEE\n").trim(); 
		 properInfo = properInfo.replaceAll("\\b(?:[,(]\\s*)?(?:AS\\s+)?(?:\\bCO-?\\s*)?(?:<!BOARD\\sOF\\s)(T(?:(?:RU?)?S)?TEE?S?)(\\s*([)]|OF))?\\b", "\n$1\n");
		 properInfo = properInfo.replaceAll("\\bET\\s*AL\\b", "\n$0\n");
		 properInfo = properInfo.replaceAll("\\bET\\s*UX\\b", "\n$0\n");		 
		 properInfo = properInfo.replaceAll("\\bET\\s*VIR\\b", "\n$0\n");		 
		 
		 return properInfo;
	 }	 
	 
	private static String cleanName(String name) {
		String properName = name;
		properName = properName.replaceAll("-?JTWROS", ""); // Joint Tenants with Right of Survivorship
		properName = properName.replaceAll("H&W|W&H|H/W", "");
		properName = properName.replaceAll("\\bT/C\\b", "");
		properName = properName.replaceAll("\\bR/S\\b", "");
		properName = properName.replaceAll("(?is)\\bPOA\\b", "");
		properName = properName.replaceAll("C/O", "");
		properName = properName.replaceAll("TRE", "TRUSTEE");
		properName = properName.replaceAll("ATTN", "");

		properName = properName.replaceAll("[0-9]*%", "&");
		properName = COLarimerTR.genericCleanName(properName, new Vector<String>(), "");

		return properName;
	}	 
	
	public static void parseLegal(ResultMap m, String legal) throws Exception {
		if (StringUtils.isEmpty(legal)) {
			String addr = (String) m.get("tmpAddress");
			if (addr.matches(".*\\bLOT\\s+\\d+\\b.*")) {
				legal = addr.replaceAll("(.*)\\bLOT\\b", "LOT");
			}
		}

		// initial cleanup legal description
		legal = legal.replaceAll("(?is)\\b[NS](?:[EW])?\\s*\\d+\\s*/\\s*\\d+(?:\\s*\\bOF\\b)?", "").trim();
		legal = legal.replaceAll("\\(UNREC(ORDED)?\\)\\s*", "");
		legal = legal.replaceAll("\\bPT OF\\s+", "");
		legal = legal.replaceAll("\\bAKA\\s+", "");
		legal = legal.replaceAll("\\bCOR? OF\\b\\s+", "");
		legal = legal.replaceAll("\\bCO-OP\\b\\s*", "");
		legal = legal.replaceAll("\\bTHAT PART OF\\b\\s*", "");
		legal = legal.replaceAll("\\b\\d+\\s+FT\\b", "");
		legal = GenericFunctions.replaceNumbers(legal);

		if (StringUtils.isEmpty(legal)) {
			return;
		}
		
		String lotFromPid = "";
		String blockFromPid = "";
		String pid = (String) m.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
		// String pidO = "";
		if (pid != null && pid.length() != 0) {
			blockFromPid = pid.replaceFirst("\\w+/\\w+/\\w+/\\w+/(\\w+)/\\w+", "$1").replaceFirst("^0*(\\d+)", "$1");
			lotFromPid = pid.replaceFirst("\\w+/\\w+/\\w+/\\w+/\\w+/(\\w+)", "$1").replaceFirst("^0*(\\d+)", "$1");
			pid = pid.replace("/", "");
			// pidO = pid.replaceAll("/", "");
			m.put("PropertyIdentificationSet.ParcelID", pid);
			m.put("PropertyIdentificationSet.ParcelID2", pid);
			m.put("PropertyIdentificationSet.ParcelID3", pid);
		}
		
		// extract lot from legal description
		StringBuilder lot = new StringBuilder(); // can have multiple occurrences
		Pattern p = Pattern.compile("\\bLOTS? (\\d+[A-Z]*(,\\d+[A-Z]*)*)\\b");
		Matcher ma = p.matcher(legal);
		
		while (ma.find()) {
			lot.append(" ").append(ma.group(1));
			legal = legal.replaceFirst(ma.group(), "");
		}
		p = Pattern.compile("\\bLOT ([A-Z])\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot.append(" ").append(ma.group(1));
			legal = legal.replaceFirst(ma.group(), "");
		}
		if (lot.length() != 0) {
			String lotStr = lot.toString().trim();
			lotStr = LegalDescription.cleanValues(lotStr, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lotStr);
		} 

		// extract block from legal description
		StringBuilder block = new StringBuilder(); // can have multiple occurrences
		p = Pattern.compile("(?<=\\bBL(?:OC)?KS?)\\s+(\\d+|[A-Z])(\\s*(?:-|THRU)\\s*(\\d+|[A-Z]))?(?:[&,; ]+|$)");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			block.append(" ").append(ma.group(1));
			if (ma.group(3) != null) {
				block.append("-").append(ma.group(3));
			}
			block.append(",");
			legal = legal.replaceFirst("\\bBL(?:OC)?KS?\\b\\s*(?<=\\bBL(?:OC)?KS?)\\s+(\\d+|[A-Z])(\\s*(?:-|THRU)\\s*(\\d+|[A-Z]))?(?:[&,; ]+|$)", "");
			//legal = ma.replaceFirst(" ");
			ma.reset(legal);
		}
		if (block.length() != 0) {
			String blockStr = block.toString().trim();
			blockStr = LegalDescription.cleanValues(blockStr, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blockStr);
		}

		// extract tract from legal description
		StringBuilder tract = new StringBuilder(); // can have multiple occurrences
		p = Pattern.compile("\\bTR(?:ACTS?)? (([A-Z]|\\d+)(,([A-Z]|\\d+))*)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			tract.append(" ").append(ma.group(1));
		}
		if (tract.length() != 0) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), LegalDescription.cleanValues(tract.toString().trim(), false, true));
		}
		
		// extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?s)(.*?)(\\s+|\\s*-\\s*|\\b)(SEC|UNIT|APT|PHASE|LOTS?|BL(?:OC)?K|CONDO|(\\d+(ST|ND|RD|TH) )?(PARTIAL )?REP(LAT)?|SUB|TR(?:ACT)?|(\\d+(ST|ND|RD|TH) )?ADD|(RE-?)?REVISED|PLAT|[SENW]{1,2}('LY)? \\d+([\\./]\\d+)?(\\s*FT)?|(BEG )?\\d+([\\./]\\d+)?\\s*FT)\\b.*");
		ma = p.matcher(legal);
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			subdiv = ma.group(1).trim();
		} else {
			p = Pattern.compile("(?s)(.*?)(?:\\s+|\\s*-\\s*|\\b)(PB\\s*[\\d-]+\\s+PG\\s*[\\d-]+[A-Z]?\\b).*");
			ma = p.matcher(legal);
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				subdiv = ma.group(1).trim();
				String platBook = ma.group(2).trim();
				platBook = platBook.replaceFirst("(?s)PB\\s*([\\d-s]+)\\s+PG\\s*([\\d-]+[A-Z]?\\b)\\s*", "$1").trim();
				String platPage = ma.group(2).trim();
				platPage = platPage.replaceFirst("(?s)PB\\s*([\\d-s]+)\\s+PG\\s*([\\d-]+[A-Z]?\\b)\\s*", "$2").trim();
				platPage = platPage.replaceFirst("(?s)(\\d+)\\s*-\\s*\\1[A-Z]\\b", "$1");
				if (StringUtils.isNotEmpty(platBook) && StringUtils.isNotEmpty(platPage)) {
					m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), platBook);
					m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), platPage);
				}
			}
		}
		
		if (StringUtils.isNotEmpty(subdiv)) {
			// cleanup subdivision name
			subdiv = subdiv.replaceAll(", THE\\s*$", "");
			subdiv = subdiv.replaceAll("(?is)\\b(?:FROM|COM AT) THE \\b[NWSE]{1,2}\\b(?:\\s+\\bTHE\\b)?", "");
			subdiv = subdiv.replaceAll("(?is)\\bBEG(?:INNING)?\\b\\s+(?:[\\d\\.]+\\s+)?(?:[NSEW])?\\s+\\bOF\\b\\s+\\b[NSEW]{1,2}\\b(?:(?:[NSEW/\\d\\s]|OF)*)?", "");
			subdiv = subdiv.replaceAll("(?is)(?:\\s*RUN|THENCE)*\\s*", "");
			subdiv = subdiv.replaceAll(", PROP OF\\s*$", "");
			subdiv = subdiv.replaceAll(" PART OF\\s*$", "");
			subdiv = subdiv.replaceAll(", (CITY|TOWN) OF\\s*$", "");
			subdiv = subdiv.replaceAll(",\\s*$", "");
			subdiv = subdiv.replaceAll("( NO\\.?)? \\d+\\b\\s*", "").trim();
			subdiv = subdiv.replace("\n", " ");

			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
			if (legal.matches(".*\\bCONDO\\b.*")) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
			}
		}

		// additional legal description cleanup before extracting the rest of
		// the legal elements
		legal = legal.replaceAll("\\bNO\\b\\.?\\s*", "");
		legal = legal.replaceAll("\"", "");
		legal = legal.replaceAll("\\s*\\bAND\\b\\s*", ",");
		legal = legal.replaceAll("\\s*&\\s*", ",");
		legal = legal.replaceAll("\\s*,\\s*", ",");
		legal = legal.replaceAll("\\b\\d+[\\./]\\d+\\b\\s*", "");
		legal = legal.replaceAll("\\.", "");
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		String commonPattern = "(?:\\s+|-)(\\d+(-?[A-Z])?|[A-Z](-?\\d+)?)\\b";
		// extract section from legal description
		p = Pattern.compile("\\bSEC" + commonPattern);
		ma = p.matcher(legal);
		if (ma.find()) {
			String sec = ma.group(1);
			String secFromPid = (String) m.get(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName());
			if (secFromPid != null && secFromPid.length() != 0)
				sec = sec + " " + secFromPid;
			sec = LegalDescription.cleanValues(sec, false, true);
			m.put("PropertyIdentificationSet.SubdivisionSection", sec);
		}

		// extract building # from legal description
		p = Pattern.compile("\\bBLDG" + commonPattern);
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), ma.group(1));
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPHASE" + commonPattern);
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), ma.group(1));
		}

		// extract unit from legal description
		StringBuilder unit = new StringBuilder(); // can have multiple
													// occurrences
		p = Pattern.compile("\\b(?:UNIT|APT)" + commonPattern);
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			unit.append(" ").append(ma.group(1));
		}
		if (unit.length() != 0) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), LegalDescription.cleanValues(unit.toString().trim(), false, true));
		}
		
		if (legal.contains(" PG ") && (legal.contains("OR ") || legal.contains("BK "))) {
			List<List> bodyCR = new ArrayList<List>();
			List<String> line = new ArrayList<String>();
			p = Pattern.compile("(?is)\\bOR\\b(?:\\s+\\bBK\\b)?\\s*(\\d+)\\s+\\bPG\\b\\s*([\\d-]+[A-Z]?\\b)");
			ma.usePattern(p);
			ma.reset();
			while (ma.find()) {
				String platBook = ma.group(1).trim();
				String platPage = ma.group(2).trim();
				platPage = platPage.replaceFirst("(?s)(\\d+)\\s*-\\s*\\1[A-Z]\\b", "$1");
				if (StringUtils.isNotEmpty(platBook) && StringUtils.isNotEmpty(platPage)) {
					line = new ArrayList<String>();
					line.add(platBook);
					line.add(platPage);
					bodyCR.add(line);
				}
			}
			
			if (!bodyCR.isEmpty()){
				String[] header = { "Book", "Page" };
				ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
				m.put("CrossRefSet", rt);
			}	
		}
	}
  }
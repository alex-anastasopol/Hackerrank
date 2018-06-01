package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLSantaRosaTR 
  {
	   public static String cleanOwnerFLSantaRosaTR(String s)
	   {
		   /* s e pe mai multe linii, prima/primele 2(sau 3) sunt numele ownerilor, urmatoarele adresa 
		    * (care incepe fie cu [nr], fie cu [PO BOX si numar] (PO poate sa fie si P O, si P.O.),fie cu [RT numar BOX] */
		   
		   if (s.matches("([A-Z\\s&-]+)(\\b19\\d{2}\\b\\s*(?:[A-Z\\s-']+\\s*)?)((PO|P O|P.O.|RT \\d+)\\s*BOX [A-Z\\d\\s-,]+)"))
		   {  // vezi PIN-urile: 292S28544600E000480 sau 292S28544600F000240
			   s = s.replaceFirst("([A-Z\\s&-]+)(\\b19\\d{2}\\b\\s*(?:[A-Z\\s-']+\\s*)?)((PO|P O|P.O.|RT \\d+)\\s*BOX [A-Z\\d\\s-,]+)","$1$2");
		   }
		   else
			   if (s.matches("([A-Z\\s&-]+)(\\(.*\\))\\s*((?:(PO|P O|RT \\d+)\\s*BOX|\\d+) [A-Z\\d\\s-,]+)"))
				   s = s.replaceFirst("([A-Z\\s&-]+)(\\(.*\\))\\s*((?:(PO|P O|P.O.|RT \\d+)\\s*BOX|\\d+) [A-Z\\d\\s-,]+)", "$1 $2");   
			   else
				   s = s.replaceFirst("\\s*(?:(?:P\\s*O|P.O.|RT\\s*\\d+)\\s*BOX)?\\s*\\d+\\s*[A-Z\\d-\\s,]+","");
		   
		   s = s.replaceFirst("\\b(ET)\\s+(AL|UX|VIR)","$1$2");
		   s = s.replaceFirst("\\bAS PERSONAL\\b","");
		   s = s.replaceFirst("\\bESQ\\b","");
		   
		   s = s.replaceAll("(?:AS)?( TRUSTEES?)\\s*\\Z","$1");
		   s = s.replaceFirst("(?:AS)? (TRUSTEE) OF\\s*([A-Z\\s-']+\\s[A-Z]+)"," $1 & $2");		//PIN: 0001452201
		   s = s.replaceAll("AS (TRUSTEES?)\\s*([A-Z][A-Z\\s-]+)"," $1 & $2");	
		   s = s.replaceFirst("(TRUSTEES) OF\\s*([\\dA-Z\\s-]+)"," $1 & $2");
		   s = s.replaceFirst("%\\s([A-Z]+)","C/O $1");  //pe FL Polk, PIN:000000-000028-425100 -> SMITH & SLATER GROVE  INC  % MARY NELL SMITH
		   
		   //cazuri de exceptie unde lipsesc delimitatori de owneri
		   s = s.replaceFirst("((?:REVOCABLE|LIVING) TRU(?:ST)?)\\s*([A-Z\\s-']+\\s[A-Z]+)","$1 & $2");        //PIN: 191N270000036020000
		   if (!(s.contains(" JR") || s.contains(" SR") || s.contains(" II") || s.contains(" III") || 
			 s.contains(" IV ")  || s.contains("ESTATE") || s.contains("REVOCABLE TRS") || s.contains("REVOCABLE TRUS") || s.contains("REVOCABLE TRUST")))
		   {
			   s = s.replaceFirst("([A-Z]+\\s[A-Z]+\\s[A-Z]+)\\s*([A-Z]+\\s[A-Z]\\s[A-Z]+)","$1 & $2");		//PIN: 0001450193
		   }
		   s = s.replaceFirst("ESTATE(?: OF)?\\s*(C/O[A-Z\\s-']+\\s[A-Z]+)","ESTATE & $1");		        //PIN: 342N28057000X000052
		   if (!s.matches("([A-Z\\s-'\\d+&]+)\\s*\\((TR|TRUST|(?:LIVING)?ESTATE|COMPANY|JR|SR|I|II|III|IV)\\)\\s*([A-Z\\s-'\\d+&]+)"))
			   //PIN: 183N260000002050000
		      s = s.replaceFirst("\\(.*\\)","");
		   
		   if (s.matches("([A-Z\\s\\d-'&]+)\\s*C/O\\s([A-Z]+)\\s([A-Z]|[A-Z]{2,})\\s([A-Z]+).*") && !(s.contains("JR")))  //342N28057000X000052
			   s = s.replaceFirst("(.*)\\s*C/O\\s([A-Z]+)\\s([A-Z]|[A-Z]{2,})\\s([A-Z]+)(.*)","$1 & $4 $2 $3 $5");
		   else
			   if (s.matches(".*\\s*C/O\\s([A-Z]+)\\s([A-Z]+).*"))  //PIN: 401N280090399000090
				   s = s.replaceFirst("\\s*C/O\\s([A-Z]+)\\s([A-Z]+)"," & $2 $1");
		   //s = s.replaceFirst("&\\s*\\b(ETUX|ETAL)\\b\\s*\\Z","");
		   if (s.matches("([A-Z\\d-&'\\s]+)\\bINC\\b\\s*([A-Z\\d-&'\\s]+)"))
		   {
			   if (!s.contains("DEPARTMENT"))    //FL Polk TR, PIN: 000000-000020-024370
				   s = s.replaceFirst("([A-Z\\d-&'\\s]+)\\bINC\\b\\s*([A-Z\\d-&'\\s]+)","$1 INC & $2");
		   }
           if (s.matches(".*\\s*&\\s*[A-Z]{2,}\\s[A-Z]\\s[A-Z]{3,}.*"))
        	   s = s.replaceFirst("\\s*&\\s*([A-Z]{2,}\\s[A-Z]\\s)([A-Z]{3,})"," & $2 $1");		//PIN: 212S27181700B000420
           
           if (s.matches("([A-Z]{2,}+\\s&\\s[A-Z\\s\\d-']+\\sPARTNERSHIP)\\s*([A-Z\\s\\d-']+PARTNERSHIP)")) // pt POLK - > PIN: 000000-000040-122254
        	   s = s.replaceFirst("([A-Z]{2,}+\\s&\\s[A-Z\\s\\d-']+\\sPARTNERSHIP)\\s*([A-Z\\s\\d-']+PARTNERSHIP)", "$1 & $2");
           
           s = s.replaceAll("([A-Z]+)\\s*-\\s*([A-Z]+)","$1-$2");
		   s = s.replaceAll("(\\s*&\\s*){2,}"," & ");
		   s = s.replaceAll("\\s{2,}"," ");
		   
		   return s.trim();
	   }
	   
	   public static void partyNamesTokenizerFLSantaRosaTR(ResultMap m, String s) throws Exception {
		   boolean goOn = true;		//daca am companie si alti owneri, atunci setez pe true flag-ul,parsez aparte compania si continui cu parsarea ownerilor
		   String own = s;
		  
		   String[] owners;
		   
		   List<List> body = new ArrayList<List>();
		   String[] names = {"", "", "", "", "", ""};
		   String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		  //test for companies
		   if (own.matches("\\A[A-Z]{2,}+\\s&\\s([A-Z\\s\\d-']+\\s\\b(?:(?:LIFE\\s)?ESTATE|INC|(?:LIVING\\s)?TRUST))\\b\\Z")
			  || own.matches("\\A[A-Z]+\\s*&\\s*[A-Z]+\\Z"))  // cazul SMITH & LANE 
		   {
			   goOn = false;
			   names[2] = own;
			   types = GenericFunctions.extractAllNamesType(names);
			   otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			   GenericFunctions.addOwnerNames(names, "", "", types, otherTypes, true, false, body);
		   }           
		   else
			 {
			   String compania = "";
			   if (own.matches("([A-Z]{2,}+\\s&\\s[A-Z\\s\\d-']+\\sPARTNERSHIP)\\s&\\s([A-Z\\s\\d-']+PARTNERSHIP)")) // pt POLK - > PIN: 000000-000040-122254
				 {
				   compania = own;
				   compania = compania.replaceFirst("([A-Z]{2,}+\\s&\\s[A-Z\\s\\d-']+\\sPARTNERSHIP)\\s&\\s([A-Z\\s\\d-']+PARTNERSHIP)", "$1");
				   own = own.replaceFirst("([A-Z]{2,}+\\s&\\s[A-Z\\s\\d-']+\\sPARTNERSHIP)\\s&\\s([A-Z\\s\\d-']+PARTNERSHIP)", "$2");
			     }
			   else
			      if (own.matches("\\A([A-Z]{2,}+\\s&\\s[A-Z\\s\\d-']+\\s\\b(?:(?:LIFE\\s)?ESTATE|INC|(?:LIVING\\s)?TRUST))\\b\\s*&\\s*([A-Z\\s\\d-'&]+)"))
				   {
			    	   compania = own;
					   compania = compania.replaceFirst("\\A([A-Z]{2,}+\\s&\\s[A-Z\\s\\d-']+\\s\\b(?:(?:LIFE\\s)?ESTATE|INC|(?:LIVING\\s)?TRUST))\\b\\s*&\\s*([A-Z\\s\\d-'&]+)","$1");
					   own = own.replaceFirst("([A-Z]{2,}+\\s&\\s[A-Z\\s\\d-']+\\s\\b(?:(?:LIFE\\s)?ESTATE|INC|(?:LIVING\\s)?TRUST))\\b\\s*&\\s*([A-Z\\s\\d-'&]+)","$2");
				   }
			   else
				  if (own.matches("\\A([A-Z]{2,}+\\s&\\s[A-Z\\s\\d-']+)\\s*&\\s*([A-Z\\d-]{2,}\\s[A-Z]{2,})"))  //pe FL Polk: SMITH & SIMPSON & SIMPSON KATHERN
				  {
					  compania = own;
					  compania = compania.replaceFirst("\\A([A-Z]{2,}+\\s&\\s[A-Z\\s\\d-']+)\\s*&\\s*([A-Z\\d-]{2,}\\s[A-Z]{2,})", "$1");
					  own = own.replaceFirst("\\A([A-Z]{2,}+\\s&\\s[A-Z\\s\\d-']+)\\s*&\\s*([A-Z\\d-]{2,}\\s[A-Z]{2,})","$2");
				  }
				  else
					// pe FL Polk: SMITH ALTON & NORMA LIVING TRU & C/O CROUSE & ASSOC;  SMITH ALTON C LIVING TRUST C/O AVERY PROPERTIES 
					  if (own.matches("\\A([A-Z\\d\\s&-']+)\\s*C/O\\s*([A-Z']{2,}\\s*&?\\s*[A-Z'\\.]{2,})\\Z") && 
							  NameUtils.isCompany(own.substring(own.indexOf("C/O ")+4)))   
					  {
						  compania = own;
						  compania = compania.replaceFirst("([A-Z\\d\\s&-']+)\\s*C/O\\s*([A-Z']{2,}\\s*&\\s*[A-Z'\\.]{2,})\\Z","$2");
						  own = own.replaceFirst("([A-Z\\d\\s&-']+)\\s*C/O\\s*([A-Z']{2,}\\s*&\\s*[A-Z'\\.]{2,})\\Z", "$1");
					  }
			   if (compania != "")
			   {
				   names[2] = compania;
				   types = GenericFunctions.extractAllNamesType(names);
				   otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				   GenericFunctions.addOwnerNames(names, "", "", types, otherTypes, true, false, body);
				   goOn = true;
			   }
			 }
		   if (own.matches("\\A[A-Z\\d-']{2,}(?:(?:\\s*[A-Z]{2,})?\\s&\\s[A-Z]{2,}|[A-Z\\d\\s-']+)\\s(?:LIVING\\s)?TRU(?:ST)?"))  //pt FL Polk, SMITH ALTON & NORMA LIVING TRU C/O CROUSE & ASSOC
		   {
			   names[2] = own;
			   types = GenericFunctions.extractAllNamesType(names);
			   otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			   GenericFunctions.addOwnerNames(names, "", "", types, otherTypes, true, false, body);
			   goOn = false;
		   }
		   own = own.replaceFirst("\\A(.*)\\s*&\\s*\\Z","$1");
		   
		   String county = (String) m.get(PropertyIdentificationSetKey.COUNTY.getKeyName());
		   if (org.apache.commons.lang.StringUtils.isNotEmpty(county) && "polk".equalsIgnoreCase(county)){
			 //342723-013009-000750 - WILEN CESAR WILEN VERONICA 
			   //342723-013009-000760 - GITTENS EZRA R GITTENS CAMILLA 
			   //342723-013009-000240 - HERRERA ROBERT W HERRERA EVELY 
			   //342723-013009-000300 - DEVICTORIA JOELSON L DEVICTORIA MAGDA L 
			   //342723-013009-001520 - MABE HORACE EUGENE JR MABE AUDREY 
			   if (NameUtils.isNotCompany(own)){
				   own = own.replaceAll("(?is)([A-Z]+)\\s+([A-Z]+(?:\\s+[A-Z]+)?(?:\\s+[A-Z]{1,2})?)\\s+(\\1)\\s*", "$1 $2 & $3 ");
				   if (!own.contains("&")){
					   own = own.replaceFirst("(?is)([A-Z]+)\\s+([A-Z]+(?:\\s+[A-Z])?)\\s+([A-Z]{2,}\\s+[A-Z]+)", "$1 $2 & $3");
				   }
			   }
		   }
		   owners = own.split("&");
		   if (goOn)
		   {
				String prev_ow_last_name = "";
				for (int i=0; i<owners.length; i++)
				{
					String next_ow = "";
					boolean lifeEst = false;
					Pattern pa;
					Matcher ma;
					
					String ow = owners[i];
					if (i < owners.length-1) next_ow = owners[i+1];
					if (ow.matches(".*\\b((?:LIFE\\s*)?ESTATE|(?:REVOCABLE\\s)?(?:TRUST|TRU|TRUS))\\b\\s*")) lifeEst = true;
			  		if (lifeEst) 				// in parseNameNashville se stergea ESTATE sau LIFE ESTATE
			  			names[2] = owners[i];
			  		else
			  			names = StringFormats.parseNameNashville(ow, true);
					pa = Pattern.compile("\\bTR\\b");
					ma = pa.matcher(names[2]);
					if (i != owners.length-1)
					{
					if (ma.find())
						prev_ow_last_name = names[2].substring(0,names[2].indexOf(' '));
					else
						prev_ow_last_name = names[2];
					}
					if (next_ow != "")
					{
						if (next_ow.contains(prev_ow_last_name))  //e.g.: PID:062800370000  ->  SMITH JEFFREY C & ANNE STEINBOCK SMITH
						{
							//prev_ow_last_name = names[2];
							next_ow = next_ow.replaceFirst("([A-Z\\s]+)\\s" + prev_ow_last_name, prev_ow_last_name + "$1");
							owners[i+1] = next_ow;
						}
						else
						{
							//pt cazul in care am middle-uri diferite de initiala (de tipul MARGIE MARIE) testez daca ambele sunt prenume
							pa = Pattern.compile("[A-Z]{3,}\\s[A-Z]{3,}\\s*");
							ma = pa.matcher(next_ow);
							if (ma.find())
							{
								String first1 = "", first2 = "";
								next_ow = next_ow.trim();
								first1 = next_ow.substring(0,next_ow.indexOf(' '));
								first2 = next_ow.substring(next_ow.indexOf(' ')+1);
								if (LastNameUtils.isLastName(first2))
								{
									next_ow = first2 + " " + first1;
									owners[i+1] = next_ow;
								}
								if ((FirstNameUtils.isFemaleName(first1) && FirstNameUtils.isFemaleName(first2)) ||
									(FirstNameUtils.isMaleName(first1) && FirstNameUtils.isMaleName(first2)))
								{
									//cazuri particulare: SMITH ALLEN PAYNE & CONNIE PEARL (PIN: 315N280000002010000 ) => PEARL e Last!
									if (!first2.equals("PEARL"))
									{
										next_ow = prev_ow_last_name + " " + next_ow;
										owners[i+1] = next_ow;
									}
									else 
									{
										owners[i+1] = first2 + " " + first1;
									}
								}
								
							}
							pa = Pattern.compile("[A-Z]+\\s[A-Z]\\s(TR|JR|SR)");  // cazul: SMITH HAROLD E TR & ELIZABETH D TR   cand la ELIZABETH Last name ar trebui sa fie SMITH
							ma = pa.matcher(next_ow);
							if (ma.find())
							{
								next_ow = prev_ow_last_name + next_ow;
								owners[i+1] = next_ow;							
							}
							else 
							{
							pa = Pattern.compile("[A-Z]+\\s[A-Z][A-Z]+(?:\\s*([A-Z]|[A-Z]+))?");  // SMITH EDWARD JR & MITTEL MARIE H & STACHURA CARLA MITTEL & PATRICAI M
							ma =pa.matcher(next_ow);
							if (!ma.find())
							{
								pa = Pattern.compile("\\A\\s*[A-Z]+(:?\\s[A-Z])?\\s*(:?TR|III|JR|)?\\s*\\Z");
								ma = pa.matcher(next_ow);
								if (ma.find()) 
								{
									//prev_ow_last_name = names[2];
									next_ow = prev_ow_last_name + next_ow;
									owners[i+1] = next_ow;
								}	
							}
							}
						}
					}	
					else //suntem pe ultimul owner si putem avea un caz de genul: SMITH HAROLD E TR & ELIZABETH D TR   
					{
						pa = Pattern.compile("\\A\\s*[A-Z]+\\s[A-Z]\\s*(:?TR|III|JR|)?\\s*\\Z");
						ma = pa.matcher(next_ow);
						if (ma.find()) 
						{
							owners[i] = prev_ow_last_name + owners[i];
						}
					}
				types = GenericFunctions.extractAllNamesType(names);
				otherTypes = GenericFunctions.extractAllNamesOtherType(names);
		  		suffixes = GenericFunctions.extractNameSuffixes(names);        
		  		GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
		  									NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		  	  }
		   }
			
		   GenericFunctions.storeOwnerInPartyNames(m, body, true);		   		   
	   }
	   
	  	public static String cleanupSantaRosaLegal(String inputLegal){
			
			String legal = inputLegal;
			legal = legal.replaceAll("\\bTW0\\b", "TWO");
			
			// initial corrections and cleanup of legal description			
			legal = GenericFunctions1.replaceOnlyNumbers(legal);
			legal = Roman.normalizeRomanNumbersExceptTokens(legal, new String[]{ "I", "M", "C", "L", "D" }); 
					
			legal = legal.replaceAll("\\bNO(?= \\d)", "");
			legal = legal.replaceAll("\\b(\\d+) (0\\d*)(?=\\s|$)", "$1$2");		
			legal = legal.replaceAll("\\b\\d+/\\d+ INT\\b", "");		
							
			// PARCEL # 1
			legal = legal.replaceAll("PARCEL (#\\s?)?\\d+", "");
			
			// E 100 FT OF
			legal = legal.replaceAll("\\b([NSEW]{1,2} )?\\d+(\\.\\d+)? FT( OF)?\\b", "");
			
			// E2 OF NE4 OF NW4 
			legal = legal.replaceAll("\\b([NSEW]+\\s?\\d+ )?OF ([NSEW]+\\s?\\d+( (ST|ND|RD|TH))?)\\b", "");
			
			// 2.50 MIN AC IN  ;  MIN AC
			legal = legal.replaceAll("(\\d+(\\.\\d+)? )?MIN AC (?:IN)?", "");
			
			// OR 188 PG 891 & OR 6 04 PG 247
			// OR 2463 PG 364 & IN OR 2467 PG 2076
			legal = legal.replaceAll(" & (IN )?OR \\b", " OR ");	

			// 100 & 200
			legal = legal.replaceAll(" & ", " ");
			
			// PGS
			legal = legal.replaceAll("\\bPGS\\b", "PG");
			
			//LOTS 20 TO 30
			legal = legal.replaceAll("\\b(\\d+) TO (\\d+)\\b", "$1-$2");

			// PGS 450 & 45 1
			// PGS 730 & 73 3
			// PGS 804 & 80 6
			// PGS 450 & 45 1		
			legal = legal.replaceAll("(?<!-)\\b(\\d\\d) (\\d)\\b(?!-)", "$1$2");

			// OR 6 04 PG 247
			legal = legal.replaceAll("\\bOR (\\d+) (\\d+) PG\\b", "OR $1$2 PG");

			// OR 2469 PG 851 7
			// OR 874 PG 2 58
			// OR 1937 PG 108 3
			legal = legal.replaceAll("\\bOR (\\d+) PG (\\d{2,}) (\\d)\\b", "OR $1 PG $2$3");
			legal = legal.replaceAll("\\bOR (\\d+) PG (\\d) (\\d{2,})\\b", "OR $1 PG $2$3");

			// BL K 5
			// B LK B
			legal = legal.replaceAll("\\bB\\s?L\\s?K\\b", "BLK");

			// LOTS 7 & 8
			// LO T 82
			legal = legal.replaceAll("\\bL\\s?O\\s?T(\\s?S)?\\b", "LOT");
			
			legal = legal.replaceAll("\\bU\\s?N\\s?I\\s?T(\\s?S)?\\b", "UNIT");
			legal = legal.replaceAll("\\bS\\s?E\\s?C\\b", "SEC");	

			// AS DES IN O R 441 PG 194
			// A S DES IN OR 345 PG 501
			// AS DE S IN OR 1841 PG 1389
			// PROPERTY DES IN OR 284 PG 583
			// DES IN O R 202 PG 228
			legal = legal.replaceAll("\\b(PROPERTY )?(A\\s?S )?D\\s?E\\s?S I\\s?N O\\s?R\\b", "OR");
			
			//LESS 33/39TH INTEREST
			//LESS 4/5 MIN RGHTS
			//LESS ALL MINERAL RIGHTS
			//LESS 1/2 MINERAL RIGHTS
			//ALL MIN RIGHTS IN
			//ALL MINERAL RIGHT S IN
			//LESS ALL MINERA L RIGHTS
			//LESS ALL MINERAL RIGHTS
			legal = legal.replaceAll("(LESS )?(ALL|\\d+/\\d+(ST|ND|RD|TH)?) (((MIN|M\\s?I\\s?N\\s?E\\s?R\\s?A\\s?L) RI?GHT\\s?S?( IN)?)|INT(EREST)?)", "");
			
			// INCL LOT 32
			legal = legal.replaceAll("\\bINCL (?:LOT|BLK) (\\d+)\\b", "$1");
			
			legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
			legal = legal.replaceAll("\\b\\d+(ST|ND|RD|TH) ADD( TO)?\\b", "");		
			legal = legal.replace(",", "");
			// 1/16 OF    BEING OR 1110 PG 162  
			legal = legal.replaceAll("(?:\\d+)/(:?\\d+)\\s*OF\\s*(:?BEING)?", "");
			legal = legal.replaceAll("\\s{2,}", " ").trim();
			
			return legal;
		}
	  	
		public static String extractLegalElemsFLSantaRosa(ResultMap m, String legal) throws Exception {	
			
			legal = cleanupSantaRosaLegal(legal);
			
			// extract lot from legal description
			String lot = "";
			Pattern p = Pattern.compile("\\b(LO?TS?) (\\d+(?:-?[A-Z])?(?:[\\s&-]+\\d+)*)\\b");
			Matcher ma = p.matcher(legal);
			while (ma.find()) {
				lot = lot + " " + ma.group(2);
				legal = legal.replaceFirst(ma.group(0), "LOT ");
			}
			lot = lot.replaceAll("\\s*&\\s*", " ").trim();
			lot = lot.replaceAll("\\b(\\d+)-(\\d+)-(\\d+)\\b", "$1 $2 $3");
			if (lot.length() != 0) {
				lot = LegalDescription.cleanValues(lot, false, true);
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			}
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		
			// extract block from legal description
			String block = "";
			p = Pattern.compile("\\bBL(?:OC)?KS? ((?:\\b[A-Z]\\b|\\d+|&|\\s)+)\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				block = block + " " + ma.group(1);
				legal = legal.replaceFirst(ma.group(0), "BLK ");
			}
			block = block.replaceAll("\\s*&\\s*", " ").trim();
			if (block.length() != 0) {
				block = LegalDescription.cleanValues(block, false, true);
				m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			}
			legal = legal.trim().replaceAll("\\s{2,}", " ");
			
	 	    // extract phase from legal description
			String phase = "";
			p = Pattern.compile("\\b(PH)A?S?E?S?+\\s*([,\\dA-Z-]+(?:\\s*[,\\s]\\s*\\d+)*)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				String phaseTmp = ma.group(2);
				phaseTmp = phaseTmp.replaceAll("\\s*,\\s*", " ");
				phase = phase + " " + Roman.normalizeRomanNumbers(phaseTmp);
				legal = legal.replaceFirst(ma.group(0), ma.group(1) + " ");
				m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
				legal = legal.trim().replaceAll("\\s{2,}", " ");
			}
			
			// extract unit from legal description
			String unit = "";
			p = Pattern.compile("\\b(UNIT)\\s*([\\d]+|[A-Z])\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				unit = unit + " " + Roman.normalizeRomanNumbers(ma.group(2));
				legal = legal.replaceFirst(ma.group(0), "UNIT ");
				m.put("PropertyIdentificationSet.SubdivisionUnit", unit);// ma.group(2));
				legal = legal.trim().replaceAll("\\s{2,}", " ");
			}
			
			// extract building from legal description
			String bldg = "";
			p = Pattern.compile("\\b(BUILDING|BLDG)\\s*(\\d+[A-Z]?|[A-Z]\\d*)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				bldg = ma.group(2);
				legal = legal.replaceFirst(ma.group(0), "BLDG ");
				m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
				legal = legal.trim().replaceAll("\\s{2,}", " ");
			}
			
			// extract tract from legal description
			String tract = "";
			   p = Pattern.compile("\\b(TRACT) (\\d+|[A-Z])\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   tract = ma.group(2);
				   legal = legal.replaceFirst(ma.group(0), ma.group(1));
				   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
				   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   }
			
			// extract and replace sec-twn-rng from legal description
	  	   	List<List> bodySTR = new ArrayList<List>();
	 	    p = Pattern.compile("(?<!-)\\b0*(\\d+)-0*(\\d+[SWEN])-0*(\\d+[SWEN]?)\\b(?!-)");
	 	    ma = p.matcher(legal);
	 	    while (ma.find()){
	 	    	List line = new ArrayList<String>();
	 	    	line.add(ma.group(1));
	 	    	line.add(ma.group(2));
	 	    	line.add(ma.group(3));
	 	    	bodySTR.add(line);		   
	 	    	legal = legal.replace(ma.group(0), "");
	 	    }
	 	    p = Pattern.compile("\\bSEC 0*(\\d+)(?:-| TWP )0*(\\d+[SWEN]?)(?:-| RG )0*(\\d+[SWEN]?)\\b");
	 	    ma = p.matcher(legal);
	 	    while (ma.find()){
	 	    	List line = new ArrayList<String>();
	 	    	line.add(ma.group(1));
	 	    	line.add(ma.group(2));
	 	    	line.add(ma.group(3));
	 	    	bodySTR.add(line);		   
	 	    	legal = legal.replace(ma.group(0), "");
	 	    }
	 	    if (!bodySTR.isEmpty()){
	 		   String [] header = {"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"};
	 		   
	 		   Map<String,String[]> map = new HashMap<String,String[]>();
	 		   map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
	 		   map.put("SubdivisionTownship", new String[]{"SubdivisionTownship", ""});
	 		   map.put("SubdivisionRange", new String[]{"SubdivisionRange", ""});
	 		   
	 		   ResultTable pis = new ResultTable();	
	 		   pis.setHead(header);
	 		   pis.setBody(bodySTR);	 		   
			   pis.setMap(map);		   
	 		   m.put("PropertyIdentificationSet", pis);
	 		   legal = legal.replaceAll("\\s{2,}", " ").trim();
	 	    }
	 	    p = Pattern.compile("\\bSEC(?:TION)?\\s?(\\d+)\\b");
	 	    ma = p.matcher(legal);
	 	    if (ma.find()){
	 	    	m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));
	 	    	legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
	 	    	legal = legal.trim().replaceAll("\\s{2,}", " ");
	 	    }
	 	    String remarks = (String) m.get("SaleDataSet.Remarks");
			
			if (remarks != null)
				legalTokenizerRemarksFLSantaRosaRV(m, remarks);
			
			return legal;
		}

		public static void legalFLSantaRosaTR(ResultMap m, String legal) throws Exception {
			
			legal = extractLegalElemsFLSantaRosa(m, legal);			
			String legalTemp = legal;
			
			// extract crossreferences
			List<List<String>> bodyCr = new ArrayList<List<String>>();
			Pattern p = Pattern.compile("\\bOR (\\d+(?:[ -]?\\d+)*) PG (\\d+(?:[ -]?\\d+)*)\\b");
			Matcher ma = p.matcher(legal);
			while(ma.find()){
				List<String> line = new ArrayList<String>();
				line.add(ma.group(1));
				line.add(ma.group(2));
				bodyCr.add(line);
				legalTemp = legalTemp.replaceFirst(ma.group(0), "OR");
			}
			legal = legalTemp;
			if (!bodyCr.isEmpty()) {
				String[] header = { "Book", "Page"};
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("Book", new String[] { "Book", "" });
				map.put("Page", new String[] { "Page", "" });
				ResultTable cr = new ResultTable();
				cr.setHead(header);
				cr.setBody(bodyCr);
				cr.setMap(map);
				m.put("CrossRefSet", cr);
			}	        			
			
			legal = legal.replaceAll("\\b(UNIT|BLK|LOT|OR|PH)\\b", "@");
			legal = legal.replaceAll("@ (AS DES|LESS|IN PARK) @", "@");
			legal = legal.replaceAll("(REVISED|REPLAT)[^@]*@", "@");
			legal = legal.replaceAll("\\s{2,}", " ");
			legal = (" " + legal).replaceAll("( @)+", " @").trim();
			legal = legal.replaceAll("\\(THE\\)", "");
			legal = legal.replaceAll("\\bUNREC\\b", "");
			legal = legal.replaceAll("LIFE ESTATE IN:", "");

			// set subdivision
			String subdiv = "";
			p = Pattern.compile("^([^@]+) @$");
			ma = p.matcher(legal);
			if (ma.matches()) {
				subdiv = ma.group(1);
			}
			if(subdiv.startsWith("BEG ") || subdiv.contains("POB") || subdiv.endsWith(" COR")){
				subdiv = "";
			}
			if (subdiv.length() != 0) {
				subdiv = subdiv.replaceAll("(RE )?S/D", "").trim();
				subdiv = subdiv.replaceAll("(LESS\\s*)?\\d+(ST|ND|RD|TH) ADDN( TO)?", "").trim();
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

			//TestParser.append("d:/legalz.txt", legal);
		}
		
		   public static String legalTokenizerRemarksFLSantaRosaRV(ResultMap m, String legal) throws Exception{
			   
			   //initial corrections and cleanup of legal description
			   legal = legal.replaceAll("(?<=#)\\s", "");
			   legal = legal.replaceAll("\\bPTS?\\b", "");
			   legal = legal.replaceAll("(?<=\\d )AND(?= \\d)", "&");
			   legal = legal.replaceAll("\\b(BLOCKS?+)([A-Z])\\b", "$1 $2");
			   legal = legal.replaceAll("\\bL\\s?O\\s?T\\b", "LOT");
			   legal = legal.replaceAll("\\b(OR \\d+) PH(?=\\s?\\d+)\\b", "$1 PG");		   
			   legal = GenericFunctions.replaceNumbers(legal);		   
			   legal = legal.replaceAll("\\s{2,}", " ").trim();	   
			   
			   // extract and remove section, township and range from legal description
			   //List<List> body = getSTRFromMap(m); //first add sec-twn-rng extracted from XML specific tags, if any (for DT use)
			   List<List> body = GenericFunctions2.goToGetSTRFromMap(m); //first add sec-twn-rng extracted from XML specific tags, if any (for DT use)
			   Pattern p = Pattern.compile("\\b(\\d+(?:\\s*[&,/]\\s*\\d+)*)-+(\\d+[SWEN])-+(\\d+[SWEN]?)\\b");
			   Matcher ma = p.matcher(legal);
			   while (ma.find()){		 
				   List<String> line = new ArrayList<String>();
				   line.add(ma.group(1).replaceAll("\\b0+(\\d)", "$1").replaceAll("\\s*[&,/]\\s*", " "));
				   line.add(ma.group(2));
				   line.add(ma.group(3));
				   body.add(line);			   
				   legal = legal.replace(ma.group(0), " SEC ");
				   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   }
			   //saveSTRInMap(m, body);
			   GenericFunctions2.goToSaveSTRInMap(m, body);
			   p = Pattern.compile("\\bSECT(?:ION)? [#\\s]*(\\d+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));
				   legal = legal.replace(ma.group(0), " SEC ");
				   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   }
			   
			   // extract and replace phase from legal description
			   String phase = "";
			   p = Pattern.compile("\\bPH(?:ASE)? [#\\s]*(\\d+|[IVX]+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   phase = ma.group(1);
				   phase = phase.replaceAll("\\s*&\\s*", " ");
				   phase = phase.replaceAll("\\b([IVX]+)", "$1 --"); 
				   phase = Roman.normalizeRomanNumbers(phase);
				   phase = phase.replaceAll(" --", "");
				   if (StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionPhase")))
					   m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", " PHASE ");
				   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   }
			   
			   // extract and remove cross refs from legal description - phase 1
			   List<List> bodyCR = new ArrayList<List>();
			   p = Pattern.compile("\\b(\\d{9,}) OR (\\d+)/(\\d+)\\b");
			   ma = p.matcher(legal);   	   
			   while (ma.find()){
				   List<String> line = new ArrayList<String>();		   
				   line.add(ma.group(2));
				   line.add(ma.group(3));
				   line.add(ma.group(1));
				   line.add("");
				   bodyCR.add(line);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");		   
			   }	  
			   p = Pattern.compile("\\b(?:OR|RR|CORR) (\\d+)/(\\d+)\\b");
			   ma = p.matcher(legal);   	   
			   while (ma.find()){
				   List<String> line = new ArrayList<String>();
				   line.add(ma.group(1));
				   line.add(ma.group(2));
				   line.add("");
				   line.add("");
				   bodyCR.add(line);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");		   
			   }	  
			   p = Pattern.compile("\\bOR (\\d+) PG\\s?(\\d+)\\b");
			   ma = p.matcher(legal);   	   
			   while (ma.find()){
				   List<String> line = new ArrayList<String>();
				   line.add(ma.group(1));
				   line.add(ma.group(2));
				   line.add("");
				   line.add("");
				   bodyCR.add(line);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");		   
			   }	  
			   legal = legal.trim().replaceAll("\\s{2,}", " ");	
			   
			// extract and replace unit from legal description
			   String unit = "";
			   p = Pattern.compile("\\bUNIT [#\\s]*([IVX]+|\\d+(?:-?[A-Z])?|[A-Z]\\d*)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   unit = ma.group(1);
				   unit = unit.replaceAll("\\b([IVX]+)", "$1 --"); 
				   unit = Roman.normalizeRomanNumbers(unit);
				   unit = unit.replaceAll(" --", "");			   
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", " UNIT ");
			   }
			   p = Pattern.compile("\\bUNIT [#\\s]*(?:WK|WEEK) (\\d+)-\\d+\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   unit = unit + " " + ma.group(1);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", " UNIT ");
			   }		   
			   p = Pattern.compile("\\bUNIT (?:WK|WEEK) [#\\s]*\\d+ (?:PARCEL|PRC) (\\d+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   unit = unit + " " + ma.group(1);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", " UNIT ");
			   }
			   unit = unit.trim();
			   if (unit.length() != 0){
				   unit = LegalDescription.cleanValues(unit, false, true);
				   if (StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionUnit")))
					   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);		  
				   legal = legal.trim().replaceAll("\\s{2,}", " ");		   
			   }
			   	   
			   // extract and replace building # from legal description
			   String bldg = "";
			   p = Pattern.compile("\\b(?:BLDG|BUILDING) [#\\s]*([A-Z](?:-?(?:\\d+|[IVX]+))?|\\d+(?:-?[A-Z])?)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   bldg = ma.group(1);
				   bldg = bldg.replaceAll("([IVX]+\\b)", "-- $1"); 
				   bldg = Roman.normalizeRomanNumbers(bldg);
				   bldg = bldg.replaceAll("-- ", "");
				   if (StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionBldg")))
					   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", " BLDG ");
				   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   }
			   
			   // extract and replace plat b&p from legal description
			   p = Pattern.compile("\\bMB (\\d+) PG (\\d+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
				   m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", " PB ");
				   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   }	
			   		  		   
			   // extract and replace lot from legal description		   
			   String lot = "";
			   p = Pattern.compile("\\bLO?TS? [#\\s]*(\\d+[A-Z]?(?:\\s*[&,;-]\\s*\\d+[A-Z]?)*|[A-Z]{1,2})\\b(?!/)");
			   ma = p.matcher(legal);
			   while (ma.find()){
				   lot = lot + " " + ma.group(1).replaceAll("\\s*[&,;]\\s*", " ");
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }		   
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   		   
			   // extract and replace block from legal description
			   String block = ""; 
			   p = Pattern.compile("\\bBL(?:OC)?KS? [#\\s]*([A-Z]|\\d+(?:,\\d+)*)\\b(?!/)");
			   ma = p.matcher(legal);
			   while (ma.find()){
				   block = block + " " + ma.group(1).replace(',', ' ');
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");   		  
			   }
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   
			   // extract and replace lot/block from legal description
			   p = Pattern.compile("(?<![/\\.#])\\b(\\d+(?:[,&\\s-]+\\d{1,2})+)/(\\d+\\b|\\?|[A-Z]\\b)");
			   ma = p.matcher(legal);
			   while (ma.find()){
				   lot = lot + " " + ma.group(1).replaceAll("\\s*[&,]\\s*", " ");
				   String tempBlock = ma.group(2);
				   if (!tempBlock.equals("?")){
					   block = block + " " + tempBlock;
				   }
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }
			   p = Pattern.compile("\\bLOTS? [#\\s]*(\\d+)/(\\d+)\\b");
			   ma = p.matcher(legal);
			   while (ma.find()){
				   lot = lot + " " + ma.group(1);
				   block = block + " " + ma.group(2);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }
			   p = Pattern.compile("(?<![/#])\\b(\\d{1,2})/(\\d+{1,3})\\b"); // cannot make the difference between lot/block and book/page in some situations; I'll limit the lot value at 99
			   ma = p.matcher(legal);
			   while (ma.find()){
				   lot = lot + " " + ma.group(1);
				   block = block + " " + ma.group(2);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }
			   p = Pattern.compile("(?<![/#])\\b(\\d+[A-Z])/(\\d+)\\b");
			   ma = p.matcher(legal);
			   while (ma.find()){
				   lot = lot + " " + ma.group(1);
				   block = block + " " + ma.group(2);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }
			   p = Pattern.compile("\\b(\\d+[A-Z]?)/([A-Z]\\b|\\?)");
			   ma = p.matcher(legal);
			   while (ma.find()){
				   lot = lot + " " + ma.group(1);
				   String tempBlock = ma.group(2);
				   if (!tempBlock.equals("?")){
					   block = block + " " + tempBlock;
				   }
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }		   
			   lot = lot.trim();
			   if (lot.length() != 0){
				   if (StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionLotNumber")))
					   m.put("PropertyIdentificationSet.SubdivisionLotNumber", LegalDescription.cleanValues(lot, false, true));
			   }
			   block = block.trim();
			   if (block.length() != 0){
				   if (StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionBlock")))
					   m.put("PropertyIdentificationSet.SubdivisionBlock", LegalDescription.cleanValues(block, false, true));
			   }
			   
			   // extract and remove cross refs from legal description - phase 2
			   p = Pattern.compile("\\b(\\d+)/(\\d+)\\b");
			   ma = p.matcher(legal);   	   
			   while (ma.find()){
				   List<String> line = new ArrayList<String>();
				   line.add(ma.group(1));
				   line.add(ma.group(2));
				   line.add("");
				   line.add("");
				   bodyCR.add(line);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");		   
			   }
			   //GenericFunctions2.saveCRInMap(m, bodyCR);
			   GenericFunctions2.goToSaveCRInMap(m,bodyCR);
			   return legal;
		   }		
}
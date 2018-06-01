package ro.cst.tsearch.search.address2;
//package newest; //debug

//current implementation does not contain city - for a complete list of address elements look at AddressInterface
//maybe make named constants for the address parts in the interface class to make it more clear
//make sure this treats the highways, rural and so on right
//add in the exceptions at the ???

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.search.address.AddressInterface;
import ro.cst.tsearch.utils.LowPriorityFileLogger;

public class StandardAddress implements AddressInterface {
	
	static final long serialVersionUID = 10000000L;
	
	private String addressString;      // input string for the address
	private String [] addressWords;    // words from which the address is constructed
	private String addressPattern;     // pattern of the address
	private String [] addressElements; // tokens of the address
	
	private static LowPriorityFileLogger addressLogger =  new LowPriorityFileLogger(ServerConfig.getTsLogsPathPrefix() + "tslogs/created-addresses-new.txt",100);
	
	// basic constructor that builds the address out of the given string
	public StandardAddress(String inputAddressString) {
		
		addressString = inputAddressString; // make a copy of the input string				
		addressElements = new String[7]; // init the address elements
		
		// normalize the address
		String normalAddress = Normalize.trim(inputAddressString);
		
		Matcher poBox = Pattern.compile("PO BOX [\\d]+$").matcher(normalAddress);
		if(poBox.matches()){
			addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NAME] = normalAddress;
			return;
		}
		
		Matcher poBoxUnion = Pattern.compile("(.+) (PO BOX)").matcher(normalAddress);//B 3131
		if (poBoxUnion.matches()) {
			addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NAME] = poBoxUnion.group(2);
			return;
		}
		
		if(normalAddress.matches("(?is)\\d+(?:ST|ND|RD|TH|)\\b")) {  //FLMarionTR, StrName = 155TH
			addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NAME] = normalAddress;
			return;
		}
		
		//OHFranklinTR (Task 9934): 1481 TWENTY FOURTH AV;  1454-1456 E TWENTY FOURTH AVENUE; TWENTY FOURTH AVENUE AVE
		String strNoAndNameAsCompositeNumber = "(?is)((?:[\\d\\s-]+)?(?:\\s*[NSEW]{1,2})?)\\s+([A-Z]+)\\s+((?:[A-Z]|\\d)+(?:ST|ND|RD|TH))(\\s+[A-Z]+)?(?:\\s+([A-Z]+))?";
		String strNameAsCompositeNumber = "(?is)([A-Z]+)\\s+((?:[A-Z]|\\d)+(?:ST|ND|RD|TH))(\\s+[A-Z]+)?(?:\\s+([A-Z]+))?";
		Matcher streetMatcher = Pattern.compile(strNoAndNameAsCompositeNumber).matcher(inputAddressString);
		
		if (streetMatcher.matches()) {
			if (Normalize.isCompositeNumber(streetMatcher.group(2)) && Normalize.isCompositeNumber(streetMatcher.group(3))) {
				if (streetMatcher.group(1) != null) {
					if (streetMatcher.group(1).matches("(?is)([\\d\\s-]+)(\\s*[NSEW]{1,2})")) {
						String tmp = streetMatcher.group(1).trim();
						addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_PREDIRECTIONAL] = tmp.replaceFirst("(?is)[\\d\\s-]+([NSEW]{1,2})", "$1").trim();
						addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NUMBER] = tmp.replaceFirst("(?is)([\\d\\s-]+).*", "$1").trim();
						
					} else if (streetMatcher.group(1).matches("(?is)([\\d\\s-]+)")) {
						addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NUMBER] = streetMatcher.group(1).trim();
					} else if (streetMatcher.group(1).matches("(?is)(\\s*[NSEW]{1,2})")) {
						addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_PREDIRECTIONAL] = streetMatcher.group(1).trim();
					}
				}
				
				//addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NAME] = streetMatcher.group(2) + " " + streetMatcher.group(3);
				if (streetMatcher.group(4) != null) {
					addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NAME] = streetMatcher.group(2) + " " + streetMatcher.group(3) + " " + streetMatcher.group(4);
				} else {
					addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NAME] = streetMatcher.group(2) + " " + streetMatcher.group(3);
				}
				
				if (streetMatcher.group(5) != null) {
					if (Normalize.isSuffix(streetMatcher.group(4).trim()) || Normalize.isSpecialSuffix(streetMatcher.group(4).trim())) {
						addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_SUFFIX] = streetMatcher.group(4).trim();
					}
				}
				return;
			}
		} else {
			streetMatcher = Pattern.compile(strNameAsCompositeNumber).matcher(inputAddressString);
			if (streetMatcher.matches()) {
				if (Normalize.isCompositeNumber(streetMatcher.group(1)) && Normalize.isCompositeNumber(streetMatcher.group(2))) {
//					addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NAME] = streetMatcher.group(1) + " " + streetMatcher.group(2);
					if (streetMatcher.group(3) != null) {
						addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NAME] = streetMatcher.group(1) + " " + streetMatcher.group(2) + " " + streetMatcher.group(3);
					} else {
						addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NAME] = streetMatcher.group(1) + " " + streetMatcher.group(2);
					}
					
					
					if (streetMatcher.group(4) != null) {
						if (Normalize.isSuffix(streetMatcher.group(3).trim()) || Normalize.isSpecialSuffix(streetMatcher.group(3).trim())) {
							addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_SUFFIX] = streetMatcher.group(3).trim();
						}
					}
					return;
				}
			}
		}
		
		
		//710 S MILL ST #9-B
		if (inputAddressString.matches("(?is).*?#\\d+-[A-Z]$") && normalAddress.matches("(?is).*?#\\s+\\d+\\s+[A-Z]$")){
			normalAddress = inputAddressString.replaceAll("#\\s*", "# ");
		} else if (inputAddressString.matches("(?is).*?#\\d+[A-Z]$")) {
			normalAddress = inputAddressString.replaceAll("#", "# ");
		}
			
		// if an empty address was provided, then an empty address is returned
		if(normalAddress.equalsIgnoreCase("")){
			addressLogger.logString("|" + addressString + "| -> |" + this + "|");
			return;
		}
			
		// make a copy of normalAddress, not to interfere with the original algorithm
		String address = normalAddress;
		
		/*
		 * Split current address into words
		 */
		if(address.indexOf("\"") == -1){
			// if no quotes, then simply split it
			addressWords = address.split(" ");
		}else{
			// replace spaces with question marks
			Matcher rest =  Pattern.compile("([^\"]*)\"([^\"]+)\"(.*)").matcher(address);
			if (rest.matches()) {
				address = rest.group(1) + rest.group(2).trim().replace(' ','?') + rest.group(3);
			} 
			
			
			// split on space boundaries
			addressWords = address.split(" ");
			// replace back the question marks with spaces into words
			for(int i=0; i<addressWords.length; i++)
				addressWords[i] = addressWords[i].replace('?',' ');
			// and also into the address
			address =  address.replace('?',' ');
		}
		
		/*
		 * Build the pattern representing current address
		 */
		StringBuilder addressPatternBuilder = new StringBuilder("");
		for(int i=0; i<addressWords.length; i++){
			String word = addressWords[i];
			if(Normalize.isDirectional(word)){addressPatternBuilder.append('D');}
			else if(Normalize.isIstateSuffix(word)){addressPatternBuilder.append('t');}
			else if(Normalize.isSpecialSuffix(word)){addressPatternBuilder.append('s');}
			else if(Normalize.isSuffix(word)){ addressPatternBuilder.append('S');}
			else if(Normalize.isIdentifier(word)){addressPatternBuilder.append('U');}
			else if(word.compareTo("#") == 0){addressPatternBuilder.append('U');}
			else if(Normalize.isCompositeNumber(word)){addressPatternBuilder.append('n');}
			else if(Ranges.isNumberRange(word)){addressPatternBuilder.append('N');}
			else if(Ranges.isLetterRange(word)){
				char letter = 'X';
				Matcher mat = Ranges.LETTER_RANGE.matcher(word);
				if(mat.matches()){
					if((mat.group(2).length()<=3)&&(mat.group(4).length()<=3))
						letter = 'L';
				}
				addressPatternBuilder.append(letter);
			}
			else if(((word.length()==1) && Character.isLetter(word.charAt(0)))
					|| (word.length()==2 && addressPatternBuilder.length() > 0 && addressPatternBuilder.charAt(addressPatternBuilder.length() - 1) == 'U')){	//65 BEACH LN #BB
				char letter = 'l';
				// if we have smthg like "I 256" treat the 'I' as an interstate
				if((word.charAt(0) == 'I') && (i != (addressWords.length-1)))
					if(Normalize.isCompositeNumber(addressWords[i+1]))
						letter = 't';
				// append the letter for current word
				addressPatternBuilder.append(letter);
			}
			else if(word.indexOf(' ')!= -1){addressPatternBuilder.append('X');}
			//ILDuPage street numbers
			else if (word.matches("^\\d+[NEWS]+\\d+$")){addressPatternBuilder.append('n');}
			else if (i==0 && word.matches("^[NEWS]+\\d+$")) {addressPatternBuilder.append('n');}
			else if(word.matches(".*[0-9]+.*")){addressPatternBuilder.append('c');}
			else{addressPatternBuilder.append('X');}
		}
		addressPattern = addressPatternBuilder.toString();
		
		addressPattern = addressPattern.replaceFirst("^nll([Ss])$", "nXX$1");
		
		//debug
		//println("Pattern = |" + addressPattern +"|");		
		/*
		 * pre-process the pattern: replace consecutive S-es with X-es followed by an S
		 */
		//debug
		//String oldAddressPattern = addressPattern;  // save a copy for debug purposes
		for(int i=0; i<(addressPattern.length()-1); i++){
			if( ((addressPattern.charAt(i)=='S') || (addressPattern.charAt(i)=='s')) &&
					((addressPattern.charAt(i+1)=='S') || (addressPattern.charAt(i+1)=='s')) ){
				addressPattern = addressPattern.substring(0,i) + "X" + addressPattern.substring(i+1,addressPattern.length());
			}
		}
		
		Pattern pCnty = Pattern.compile("(n?D?)XSn([l|D]?)"); // "CR 208" or "2545 County Road" 208 (fix for bug #2341) or 250 CR 90 E (FL Flagler TR)
		Matcher maCnty = pCnty.matcher(addressPattern);
		if( addressWords.length > 1 ) {
			boolean extraConditionForCase_nDXSn = false;
			if (addressWords.length>=4) {
				extraConditionForCase_nDXSn = ("COUNTY".equals(addressWords[2]) && "ROAD".equals(addressWords[3]))
					|| ("US".equals(addressWords[2]) && "HIGHWAY".equals(addressWords[3]))
					|| ("US".equals(addressWords[1]) && "HIGHWAY".equals(addressWords[2]));
			}
			if(("COUNTY".equals(addressWords[0]) || "COUNTY".equals(addressWords[1]) || "CO".equals(addressWords[0]) || "CO".equals(addressWords[1])
					|| extraConditionForCase_nDXSn 		
			) && maCnty.matches()){
				addressPattern = maCnty.group(1) + "XXX" + maCnty.group(2);// Texas AtascosaAO "1460  CR 422 A" : 00324-00-000-000300
			} 
		}
		
		Pattern pCnty1 = Pattern.compile("(n)nXS([l|D]?)"); // 13650 49 COUNTY ROAD (Bug #5479)  
		Matcher maCnty1 = pCnty1.matcher(addressPattern);
		if( addressWords.length > 2 ){
			if("COUNTY".equals(addressWords[2]) && maCnty1.matches() ){
				addressPattern = maCnty1.group(1) + "XXX" + maCnty1.group(2);
			} 
		}
		
		Pattern pFm = Pattern.compile("(n?D?)Xn(l?)"); // "13725 E FM 2790" , "3165  FM 1099", "FM 1099 " On Texas
		Matcher maFm = pFm.matcher(addressPattern);
		if( addressWords.length > 1 ){
			boolean onLineTwo = false;
			if (addressWords.length > 2){
				if ("FM".equals(addressWords[2])){
					onLineTwo = true;
				}
			}
			if(("FM".equals(addressWords[0]) || "FM".equals(addressWords[1]) || onLineTwo) && maFm.matches()){
				addressPattern = maFm.group(1) + "XX" + maFm.group(2);
			} 
		}
		
		Pattern pState = Pattern.compile("(n?D?)X?SnX?"); // 5601 STATE HIGHWAY 361 //B 4829
		Matcher maState = pState.matcher(addressPattern);
		if( addressWords.length > 2 ){
			if(("STATE".equals(addressWords[0]) || "STATE".equals(addressWords[1]) || "STATE".equals(addressWords[2])) && maState.matches())
			{
				addressPattern = maState.group(1) + "XXX";
			} else if(("HIGHWAY".equals(addressWords[0]) || "HIGHWAY".equals(addressWords[1]) || 
					("HIGHWAY".equals(addressWords[2])&&!"OLD".equals(addressWords[1])&&!"NEW".equals(addressWords[1]))) && maState.matches()){
				addressPattern = maState.group(1) + "XX";
				//if (maState.groupCount() > 1){
					//addressPattern += maState.group(2);
				//}
			}
			
			if (addressPattern.endsWith("X")) {   //16951 NE HIGHWAY 27 ALT (FL Levy TR PIN# 04262-000-00)
				if ("ALT".equals(addressWords[addressWords.length-1]))
					addressPattern += "X";
			}
		}
		
		Pattern pHWY = Pattern.compile("(n?D?)X?SnX?(D?)"); // 958 HWY 221 N, HWY 221, HWY 221 SO //Bug 6465
		Matcher maHWY = pHWY.matcher(addressPattern);       // 800 STATE HIGHWAY 40 ALT W  
		if( addressWords.length >= 2 ){
			if (maHWY.matches()) {
				int highwayIndex = addressPattern.indexOf("S");
				if (highwayIndex>-1 && "HIGHWAY".equals(addressWords[highwayIndex])) {
					String additionalAddressPattern = "XX";
					int beforeIndex = addressPattern.indexOf("X");		//for STATE or NEW
					if (beforeIndex>-1 && beforeIndex<highwayIndex)
						additionalAddressPattern += "X";
					int afterIndex = addressPattern.lastIndexOf("X");	//FOR ALT
					if (afterIndex>-1 && afterIndex>highwayIndex && "ALT".equals(addressWords[afterIndex]))
						additionalAddressPattern += "X";
					addressPattern = maHWY.group(1) + additionalAddressPattern + maHWY.group(2);
				}
			}
		}
		
		Pattern pRD = Pattern.compile("(n?D?)X?(S|s)n(U(?:D|L|l))?"); // 2058 ROAD 76 (CA Tulare)
		Matcher maRD = pRD.matcher(addressPattern);
		if( addressWords.length > 2 ) {
			boolean ctOrStRoad = false;
			if (maRD.matches()) {
				if("STATE".equals(addressWords[0]) || "STATE".equals(addressWords[1]) || "STATE".equals(addressWords[2]) ||
				   "COUNTY".equals(addressWords[0]) || "COUNTY".equals(addressWords[1]) || "COUNTY".equals(addressWords[2])) {
						ctOrStRoad = true;
				}
				
				if(ctOrStRoad && ("ROAD".equals(addressWords[1]) || "ROAD".equals(addressWords[2])) && maRD.matches()) {
					addressPattern = maRD.group(1) + "XXX";
					
				} else if(("ROAD".equals(addressWords[0]) || "ROAD".equals(addressWords[1]) || "ROAD".equals(addressWords[2]) && maRD.matches())
						|| (Normalize.isSuffix(addressWords[0]) || Normalize.isSuffix(addressWords[1])))
				{
					addressPattern = maRD.group(1) + "XX";
					if (maRD.group(3) != null) {
						String unit = maRD.group(3);
						if ("UD".equals(unit)) {
							unit = "UL";
						}
						addressPattern += unit;
					}
				}
			}
		}
		
		Pattern pCountyState = Pattern.compile("([nN]?)(D?)(X)([nN])([sS]?D?)");	//task 9163
		Matcher maCountyState = pCountyState.matcher(addressPattern);   			//16443 COUNTY 8, MN Fillmore PRI, R-29.0269.000
		if (maCountyState.matches()) {												//29255 COUNTY 40 BLVD, MN Goodhue PRI, 28.135.0010
			int index = 0;															//27089 STATE 34, MN Hubbard PRI, 20.36.00711
			if (!"".equals(maCountyState.group(1))) {
				index++;
			}
			if (!"".equals(maCountyState.group(2))) {
				index++;
			}
				
			if ("COUNTY".equals(addressWords[index])||"STATE".equals(addressWords[index])) {
				addressPattern = maCountyState.group(1) + maCountyState.group(2) + maCountyState.group(3) + "X" + maCountyState.group(5);
			}
		}
		
		if( addressPattern.startsWith("nSXX") && addressWords.length > 2 && "VISTA".equals(addressWords[1]) && "DEL".equals(addressWords[2])) {
			addressPattern = addressPattern.replaceFirst("nSXX", "nXXX");
		
		} else if (addressPattern.equals("nSX") && "VIA".equals(addressWords[1])) {
			addressPattern = addressPattern.replaceFirst("nSX", "nXX");
		}
		
		boolean partialMatch = false;
		boolean matched = false;
		/*
		 * Try to isolate unit and number
		 */
		if(extractPattern(
				"(.*)(U)([nNlLc])(.*)",
				new int[] {-1,5,6,-1}))	{partialMatch = true;}
		else if(extractPattern(
				"(.*)(U)([nNlLc]?)$",
				new int[] {-1,5,6}))	{partialMatch = true;}
		else if(extractPattern(
				"((?=.*[nN].*[nNlLc])(?=.*X)[nNXsS]+)(U?)([nNlLc])$",
				new int[] {-1,5,6}))	{partialMatch = true;}
		else if(extractPattern( // 1002 E NORTHFIELD UNIT J-102
				"([nN])(D?)(X)(U?)([nNlLc])$",
				new int[] {-1,-1,-1,5,6}))	{partialMatch = true;}
		
		// if unit was recognized, then replace all 'U' with 'X'
		if(partialMatch){
			addressPattern = addressPattern.replace('U','X');
			//debug
			//println("Partial match: [" + addressString + "] -> [" + addressElements[5] + " " + addressElements[6] + "]" );
			//println("    Old address pattern: [" + oldAddressPattern + "]" );
			//println("    New address pattern: [" + addressPattern + "]" );
			//println("    New address : [" + Normalize.implode(" ",addressWords) + "]" );
			
		}
		/*
		 * pre-process the pattern: treat consecutive pre-directions and post-directions
		 * there could be a problem if the last word is a directional letter but in fact it means an apartment number
		 */		
		int firstIndexS = addressPattern.indexOf('D');
		int lastIndexS = addressPattern.lastIndexOf('D');
		if(firstIndexS != lastIndexS){ // there are at least two occurences of 'D'
			int firstIndexD = addressPattern.indexOf("DD");		
			int lastIndexD = addressPattern.lastIndexOf("DD");			
			// assume we start clearing all but first
			int startClear = firstIndexS+1; 
			// if we have a double sequence then we might leave alone also the 2nd direction
			if(firstIndexS == firstIndexD){
				int firstDirType = Normalize.directionType(addressWords[firstIndexS]);
				int secondDirType = Normalize.directionType(addressWords[firstIndexS+1]);
				if((firstDirType == 1) && (secondDirType == 2))
					startClear += 1;
			}
			// assume we end clearing at last but one directional
			int stopClear = lastIndexS-1; 
			// if we have a double sequence at the end, we might want to leave it like it is
			if(lastIndexD == (lastIndexS-1)){
				int firstDirType = Normalize.directionType(addressWords[lastIndexD]);
				int secondDirType = Normalize.directionType(addressWords[lastIndexD+1]);
				if((firstDirType == 1) && (secondDirType == 2))
					stopClear -= 1;				
			}
			// turn 'D' into 'X'
			if(stopClear >= startClear){				
				StringBuilder newPattern = new StringBuilder(addressPattern.substring(0,startClear));
				for(int i=startClear; i<=stopClear; i++)
					if(addressPattern.charAt(i) == 'D')
						newPattern.append('X');
					else		
						newPattern.append(addressPattern.charAt(i));
				newPattern.append(addressPattern.substring(stopClear+1,addressPattern.length()));
				//if(!newPattern.toString().equals(addressPattern)){
				//	System.out.println("Curious pattern found: [" + addressString +"] [" + Normalize.implode(" ",addressWords) + "] [" +  addressPattern + "][" + newPattern +"]");
				//}
				addressPattern =  newPattern.toString();
			}
		}
		
		/*
		 * Try to match the rest of the address as a whole
		 */
		if(matchPattern( // 123 W J W THOMPSON DRIVE N
				"([Nn]?)(D?)(X*[lD]*X+)([Ss]?)(D?)",
				new int[] {0,1,2,3,4})) { matched = true; }
		else if(matchPattern( // 77 N 20 GRAND CT E
				"([Nn]?)(D?)([Nn]?X+)([Ss]?)(D?)",				
				new int[] {0,1,2,3,4})) { matched = true; }
		else if(matchPattern( // 123 W A N THOMPSON DRIVE BV E
				"([Nn])(D?)((?=[lDsSU]*X)[XlDSsU]*)([Ss])(D?)",				
				new int[] {0,1,2,3,4})) { matched = true; }
		else if(matchPattern(	// 123 W 3RD STREET N
				"([Nn])(D?)([n])([Ss])(D?)",
				new int[] {0,1,2,3,4})) { matched = true; }
		else if(matchPattern( // 123 N DRIVE 100 E
				"(n)(D?)([stS])([nN])(D?)",
				new int[] {0,1,3,2,4})) { matched = true;}
		else if(matchPattern(	// 3RD STREET W 144
				"(D?)(n)([s])(D?)([nN]?)",
				new int[] {1,2,3,4,0})) {
			/*
			 * Treat the case where the street name was mismatched with street number.
			 */
			// if there is no direction but we have street name and street number
			if((addressElements[4] == null)&&(addressElements[0] != null)&&(addressElements[2] != null)){ 		
				// if street name and number are not something like 3rd, 4th, etc
				if(Normalize.isPlainNumber(addressElements[0]) && Normalize.isPlainNumber(addressElements[2])){
					int streetNumber = Integer.parseInt(addressElements[0]);
					int streetName = Integer.parseInt(addressElements[2]);
					// if both street name and number are plain numbers, but street name is significantly bigger, then switch them
					if(streetName > (streetNumber + 200)){
						String tmp = addressElements[0];
						addressElements[0] = addressElements[2];
						addressElements[2] = tmp;
					}
					// if street number is not plain number, but street name is, then switch them
				}else if(!Normalize.isPlainNumber(addressElements[0]) && Normalize.isPlainNumber(addressElements[2])){
					String tmp = addressElements[0];
					addressElements[0] = addressElements[2];
					addressElements[2] = tmp;
				}
			}
			matched = true;
		}
		else if(matchPattern( // (task 9321) 124 WEST LN, TN Anderson PRI, 096H-A-020.00
				"([nN])(D)([sS])",new int[] {0,2,3})) { matched = true; }
		else if(matchPattern( // 123 N DRIVE 100 E
				"(n)(D?)([stS])",new int[] {0,1,2,3,4})) { matched = true; } //fix bug 3283
		else if(matchPattern( // 123 E WEST STREET NORTH
				"([nN])(D?)(D|(?<=D)(?:(?=D[sS]{2}+)D[sS]))([sS]?)(D?)",
				new int[] {0,1,2,3,4})) { matched = true; }
		else if(matchPattern( // STREET 4 W 155
				"([sS])(n)(D?)([nN])",
				new int[] {3,2,4,0})) 	{ matched = true; }
		else if(matchPattern( // MARY STREET 120
				"((?=X)[Xl]+)([sS]?)([nN]?)",
				new int[] {2,3,0})) 	{ matched = true; }
		else if(matchPattern( // 123 MARY STREET ANABELLE
				"(n?)(X+)([sS])(X?)",
				new int[] {0,2,3,2})) 	{ matched = true; }
		else if(matchPattern( // INTERSTATE HIGHWAY 240
				"([tS])(S?)(n)",
				new int[] {3,-1,2})) 	{ matched = true; }
		else if(matchPattern( // 1610 MOUNT MORIAH
				"([nN])([Ss]?)(X)",
				new int[] {0,3,2})) 	{ matched = true; }
		else if(matchPattern( // 2204 NW 4TH STREET PL (fix for bug #1325)
				"([nN])(D)([nN])(X)([Ss])",
				new int[] {0,1,2,2,3})) 	{ matched = true; }
		else if(addressWords.length >= 3 && addressWords[2].equals("HIGHWAY") && matchPattern( // 4099 NEW HIGHWAY 96 W (fix for bug #1435)
				"([nN])(X)(S)(n)(D?)",
				new int[] {0,2,2,2,1})) 	{ matched = true; }
		else if(addressWords.length == 3 && addressWords[1].equals("HIGHWAY") && matchPattern( // 105-107 HWY 19E
				"(N)(S)(n)",
				new int[] {0,2,2})) 	{ matched = true; }
		else if(matchPattern( // 1915 VALLEY - fix for bug #2511
				"(n)(S)",
				new int[] {0,2})) 	{ matched = true; }		
		else if(matchPattern("(n)(S)(c)(D)", new int[]{0, 3, 2, 1}) //877 HIGHWAY A1A N  several forms on fl brevard
				|| matchPattern("(n)(D)(S)(c)", new int[]{0, 1, 3, 2})
				|| matchPattern("(n)(D)(S)(c)(n)", new int[]{0, 1, 3, 2, 6})
				|| matchPattern("(n)(D)(S)(c)(U)(n)", new int[]{0, 1, 3, 2, 5, 6})){matched = true;	}
		else if ((addressWords.length >= 4 && (addressWords[1].equals("COURT") || addressWords[2].equals("COURT"))) && 
				(matchPattern("(n)(D?)(S)(X)(X)", new int[] { 0, 1, 2, 2, 2 }))  // COURT OF NATCHEZ / 229 WEST COURT OF // SHOREWOOD - fix for bug #4848
				|| ((addressWords.length >= 4 && addressWords[3].equals("ROUTE")) && matchPattern( // 20475 W IL ROUTE 173 - fix for bug #4848
						"(n)(D)(X)(S)(n)", new int[] { 0, 1, 2, 2, 2 }))) {
			matched = true;
		} else if(addressWords.length >= 3 && addressWords[2].equals("HIGHWAY") && matchPattern("(n)(D?)(S)(n)(S)", new int[]{0, 1, 2, 2, 3})) { //7018 S HIGHWAY 161 HW #14   JACKSONVILLE, AR 72076
			matched = true;
		}
		else if(addressWords.length >= 3 && addressWords[1].equals("STATE") && addressWords[2].equals("HIGHWAY") && matchPattern("(D)(X?)(S)(n)(S)", new int[]{1, 2, 2, 2, 3})) { //SW STATE HWY 361 RD 361
			matched = true;
		}else if(addressWords.length >= 3 && addressWords[1].equals("STATE") && addressWords[2].equals("ROUTE") && matchPattern("(D)(X)(S)(l)", new int[]{1, 2, 2, 2})) { //E STATE ROUTE A
			matched = true;
		} else if(addressWords.length >= 3 && addressWords[2].equals("INTERSTATE") && matchPattern("(n)(D)(t)(n)(X)(S)(D)", new int[]{0, 1, 2, 2, 2, 2, 4})) { //4260 E INTERSTATE 20 SERVICE RD S # 4260
			matched = true;
		} else if(addressWords.length >= 2 && addressWords[0].equals("RIVER") && matchPattern("(S)(X)(S)", new int[]{2, 2, 3,})) { //RIVER OAK RUN
			matched = true;
		} else if(addressWords.length >= 2 && addressWords[0].equals("RIVER") && matchPattern("(S)(X)(S)", new int[]{2, 2, 3,})) { //RIVER OAK RUN
				matched = true;
		} else if(matchPattern( // 1107 30 STREET ROAD #B6 GREELEY 80631, Bug #5479
				"(n)(n)(X)(S)",
				new int[] {0,2,2,3}) && addressWords[2].equals("STREET")) {
			matched = true;
		} else if(matchPattern(	//5500 AVENUE G, FL Marion PRI, 02555-000-00
				"([nN])([sS])(l)", new int[] {0,2,2})) { matched = true; }
		else if (addressWords.length>4 && "HIGHWAY".equals(addressWords[1]) && ("OLD".equals(addressWords[4]) || "NEW".equals(addressWords[4])) && 
				matchPattern("(n)(S)(n)(U)(X)", new int[] {0,2,2,-1,2})) {	//3625 HIGHWAY 31E OLD, TN Sumner PRI, 025-016.00
			String[] split = addressElements[2].split(" ");
			if (split.length==3) {
				addressElements[2] = split[2] + " " + split[0] + " " + split[1];
			}
			matched = true; 
		} else if (addressWords.length>3 && "HIGHWAY".equals(addressWords[2]) && ("OLD".equals(addressWords[3]) || "NEW".equals(addressWords[3])) && 
				matchPattern("(n)(n)(S)(X)", new int[] {0,2,2,2})) {	//31E HWY OLD 3625, TN Sumner AO, 025--025-016.00--000
			String[] split = addressElements[2].split(" ");				//3625 31E HWY OLD, TN Sumner TR, 025--025-016.00--000
			if (split.length==3) {
				addressElements[2] = split[2] + " " + split[1] + " " + split[0];
			}
			matched = true; 
		}
				
		if(matched){
			/*
			 *  perform some cleanup
			 */
			//addd the '#' sign for 2nd unit designator when there is a unit range but no 2n unit designator
			if((addressElements[6] != null) && (addressElements[5] == null))
				addressElements[5] = "#";
			// address number and/or secondary range have "th" and "rd"
			addressElements[0] = removeNumberExpansion(addressElements[0]);
			addressElements[6] = removeNumberExpansion(addressElements[6]);
			// if "DR" was wrongfully turned into "DOCTOR" then reverse the decision
			if(addressElements[2] != null)
				if(addressElements[2].startsWith("DRIVE") && addressString.matches(".* DR[.]? .*")){
					//debug
					//println("Found DOCTOR: | " + addressString + " | "+ this + "|");
					addressElements[2] = addressElements[2].replaceFirst("DRIVE ","DR ");
				}
			//debug
			//println("|" + addressString + "|" + addressPattern + "|" + matchedPattern + "| -> MATCHED!");			
			//patternMatched = true;
			addressLogger.logString("|" + addressString + "| -> |" + this + "| *");
			return;
		}else{
			//debug
			//println("|" + addressString + "|" + Normalize.implode(" ",addressWords) + "|" + addressPattern +"| -> Did NOT match!");
			//System.out.println("|" + addressString + "|" + Normalize.implode(" ",addressWords) + "|" + addressPattern +"| -> Did NOT match!");
			if(partialMatch){
				// clean up so that the original algorithm can go as usual
				for(int i=0;i<7;i++)
					addressElements[i] = null;
			}
			//patternMatched = false;
		}
		/*
		 * The address was not succesfully matched with the new algorithm 
		 */		
		// split the address up to fit it in the elements
		String [] parts = normalAddress.split(" ");;
		
		int elementIndex = 0;
		int index = 0;
		while (elementIndex < 7 && index < parts.length) {
			// see what element we are processing
			// System.out.println("Working on \"" + parts[index] + "\" @ " + elementIndex);
			
			switch (elementIndex) {
			case 0: // we are processing primary address number
				if ((Normalize.isCompositeNumber(parts[index]) || Ranges.isNumberRange(parts[index])) &&
						(index + 1 >= parts.length || !Normalize.isSuffix(parts[index+1]) || addressElements[2] != null)) {
					
					if (addressElements[elementIndex] == null) {
						// all is good, put it in
						// no append because we can only have one street number ???
						addressElements[elementIndex] = parts[index];
						index++; elementIndex++;		// increase the indices
					} else {
						// bump it up to apt
						elementIndex = 6;
					}
				} else  {
					elementIndex++;				// increase just the elementIndex
				}
				break;
			case 1:	// we are processing the predirection
				if (Normalize.isDirectional(parts[index])) {	// it must be a direction to match
					// put in
					addressElements[elementIndex] = parts[index];
					index++; elementIndex++;		// increase the indices
				} else {
					// not the directional we were expecting, move on
					elementIndex++;
				}
				break;
			case 2:	// street name
				int index2 = index;
				
				parts[index] = StringUtils.defaultIfEmpty(parts[index], " ");
				
				// separate the quoted strings case from the other stop cases, to keep the logic simple
				if (parts[index].charAt(0) == '\"') {
					String quoted = "";
					while (index < parts.length) {
						if ("".equals(quoted)) quoted = parts[index];
						else quoted = quoted.concat(" " + parts[index]);
						index++;
						if (parts[index-1].charAt(parts[index-1].length() - 1) == '\"') break;
					}
					quoted = quoted.substring(1, quoted.length() - 1).trim();
					if (addressElements[2] == null) addressElements[2] = quoted;
					else addressElements[2] = addressElements[2].concat(" " + quoted);
					elementIndex++;
				} else {
					// the stop cases here have to be the beginning cases for the statements below, or the
					// method might leave address parts out, or just fail
					while (index2 < parts.length &&
							!Normalize.isSuffix(parts[index2]) &&		// case 3 suffix
							!Normalize.isDirectional(parts[index2]) &&		// case 4 directional
							parts[index2].compareTo("#") != 0 &&			// case 5 sec. ident
							parts[index2].length() != 1 &&
							!Normalize.isIdentifier(parts[index2]) &&
							!Normalize.isCompositeNumber(parts[index2]) &&
							!Ranges.isNumberRange(parts[index2]) &&
							!Ranges.isLetterRange(parts[index2]) &&		// case 6 sec range
							parts[index2].charAt(0) != '\"')	// stop when we get begin token for quoted section
						
						index2++;
					// theoretically all we got in there is the main address now
					if (index != index2) {
						String addressMain = parts[index];
						for (int i = index + 1; i < index2; i++)
							addressMain = addressMain.concat(" " + parts[i]);
						if (addressElements[2] == null) addressElements[2] = addressMain;
						else addressElements[2] = addressElements[2].concat(" " + addressMain);
					}
					else if(Normalize.isCompositeNumber(parts[index2])){	//B2276
						//cases like 20216 E 14TH TERRACE CT N, only 14TH is the street name
						if (addressElements[2] == null) addressElements[2] = parts[index];
						else addressElements[2] = addressElements[2].concat(" " + parts[index]);
						index2++;
					} else if(Normalize.isDirectional(parts[index2])){	//B4522
						//cases like 	200 W SOUTH ST 8B, SOUTH is the street name
						if (addressElements[2] == null && addressElements[0] != null && addressElements[1] != null) {
							addressElements[2] = parts[index];
						} else if(addressElements[2] == null) {
							addressElements[2] = parts[index];
						} else {
							addressElements[2] = addressElements[2].concat(" " + parts[index]);
						}
						index2++;
					}
					// increment the indices
					elementIndex++; index = index2;
				}
				break;
			case 3:	// we are processing the suffix
				if (Normalize.isSuffix(parts[index])) {	// it must be a suffix to match
					// see if the next one is also a suffix, and if so, count this one as part of the
					// main address name
					if (index + 1 < parts.length && Normalize.isSuffix(parts[index+1])) {
						while (index + 1 < parts.length && Normalize.isSuffix(parts[index+1])) {
							// as long as we have one more suffix left, put in main address part
							if (addressElements[2]==null) addressElements[2] = parts[index];
							else addressElements[2] = addressElements[2].concat(" " + parts[index]);
							index++;
						}
						// we know we have suffix next to us
						elementIndex = 3;
					} else {
						// put in, taking care if we already have one
						if (addressElements[elementIndex] != null) {
							// clearly a mistake, add this suffix at the end of name
							if (addressElements[2]==null) addressElements[2] =
								addressElements[elementIndex];
							else addressElements[2] = addressElements[2].concat(
									" " + addressElements[elementIndex]);
							addressElements[elementIndex] = null;
						}
						addressElements[elementIndex] = parts[index];
						index++; elementIndex++;		// increase the indices
					}
				} else {
					// not the suffix we were expecting, move on
					elementIndex++;
				}
				break;
			case 4:	// we are processing the postdirectional
				// maybe make a case here that if the main address part is "Street" put on at the beginning ???
				if (Normalize.isDirectional(parts[index])) {	// it must be a direction to match
					// put in, again taking care if there already is one
					if (addressElements[elementIndex] != null) {
						if (addressElements[2]==null) addressElements[2] =
							addressElements[elementIndex];
						else addressElements[2] = addressElements[2].concat(
								" " + addressElements[elementIndex]);
						addressElements[elementIndex] = null;
					}
					addressElements[elementIndex] = parts[index];
					index++; elementIndex++;		// increase the indices
				} else {
					// not the directional we were expecting, move on
					elementIndex++;
				}
				break;
			case 5:	// we are processing the secondary address info
				if (Normalize.isIdentifier(parts[index])||
						parts[index].compareTo("#") == 0) {	// it must be an identifier to match
					// put in
					if (addressElements[elementIndex] != null) {
						if (addressElements[2]==null) addressElements[2] = addressElements[elementIndex];
						else addressElements[2] = addressElements[2].concat(" " + addressElements[elementIndex]);
						addressElements[elementIndex] = null;
					}
					addressElements[elementIndex] = parts[index];
					index++; elementIndex++;		// increase the indices
					/*
					 addressElements[elementIndex++] = parts[index++];
					 // copy the secondary address range as well, if it is there
					  if (index < parts.length) {
					  // if it already exists, replace it with this one - since now we have a better idea
					   // of apartament number, since it comes right after APT identifier
					    if (addressElements[elementIndex] != null) {
					    if (addressElements[2] == null) addressElements[2] = addressElements[elementIndex];
					    else addressElements[2] = addressElements[2].concat(" " + addressElements[elementIndex]);
					    addressElements[elementIndex] = parts[index];
					    index++;
					    } else {
					    addressElements[elementIndex] = parts[index++];
					    }
					    }
					    // we don't have any more elements or parts
					     elementIndex=2; // we don't know what comes next, give up and send it to address name
					     */
				} else {
					// not the secondary address info we were expecting
					// try to pass it on to the secondary address range
					elementIndex = 6; // go to sec addr info
				}
				break;
			case 6: // secondary address range, see if it number or is single letter. if not, put at name
				if (Normalize.isCompositeNumber(parts[index]) || Ranges.isNumberRange(parts[index]) ||
						parts[index].length() == 1 || Ranges.isLetterRange(parts[index])) {
					
					if (addressElements[0] == null &&
							(Normalize.isCompositeNumber(parts[index]) || Ranges.isNumberRange(parts[index]))) {
						addressElements[0] = parts[index];
					} else if (addressElements[6] == null) {
						addressElements[6] = parts[index];
					} else {
						if (addressElements[2] == null) addressElements[2] = addressElements[6];
						else addressElements[2] = addressElements[2].concat(" " + addressElements[6]);
						addressElements[6] = parts[index];
					}
					
					// continue reprocessing at predirection
					index++; elementIndex = 1;
				} else {
					// we just let the main case handle the logic
					elementIndex = 2;	// so go back to address name
				}
				break;
			}
		}
		// check the special cases that would be a pain to model in the structure above
		
		// address has no number but it can be found at the end
		if (addressElements[0] == null && addressElements[2] != null) {
			String [] nameparts = addressElements[2].split(" ");
			// can only do this case if we have more than one element, and it is a pure number
			if (nameparts.length > 1 && nameparts[nameparts.length -1].matches("(^([0-9]*)$)")) {
				addressElements[0] = nameparts[nameparts.length -1];
				String newName = new String(nameparts[0]);
				for (int i = 1; i < nameparts.length -1; i++)
					newName = newName.concat(" " + nameparts[i]);
				addressElements[2] = newName;
			}
		}
		
		// address has number and secondary range but no name
		// move number to street number
		if (addressElements[0] != null && addressElements[2] == null && addressElements[6] != null && addressElements[5] == null) {
			addressElements[2] = addressElements[6];
			addressElements[6] = null;
		}
		
		// we have secondary address range, but no type
		if (addressElements[6] != null && addressElements[5] == null) {
			addressElements[5] = "#";
		}
		
		// address number and/or secondary range have "th" and "rd"
		addressElements[0] = removeNumberExpansion(addressElements[0]);
		addressElements[6] = removeNumberExpansion(addressElements[6]);
		
		/*
		 * We have suffix but no street name --> name = suffix ; suffix = null
		 */
		if( addressElements[2] == null && addressElements[3] != null ){
			addressElements[2] = addressElements[3];
			addressElements[3] = null;
		}
		
		// if street name is an ordinal number(FIRST, SECOND, THIRD etc.) and it's the last token of the address,
		// it's wrongly turned into a number(1,2,3 etc); here we fix that; e.g. FL Gilchrist TR - PIN 171015-00490015-0120, task 9859
		if (Normalize.isPlainNumber(addressElements[2])) {
			String lastAddrToken = inputAddressString.split(" ")[inputAddressString.split(" ").length - 1];
			boolean foundNextTokenAsNumber = false;
			for (int i = 3; i < addressElements.length; i++) {
				if (Normalize.isNumber(addressElements[i])) {
					foundNextTokenAsNumber = true;
					break;
				}
			}

			if (!foundNextTokenAsNumber && Normalize.isNumber(lastAddrToken) && Normalize.convertWordToNumber(lastAddrToken).equals(addressElements[2])) {
				addressElements[2] = lastAddrToken;
			}
		}
		
		// if there is a suffix, but no street name, then the suffix is the street name
		/*
		 if((addressElements[2] == null) && (addressElements[3] != null)){
		 String tmp = addressElements[2];
		 addressElements[2] = addressElements[3];
		 addressElements[3] = tmp;
		 }
		 */
		// if there is a pre-direction but no street name, then put the predirection as street name
		/*
		 if((addressElements[2] == null) && (addressElements[1] != null)){
		 String tmp = addressElements[2];
		 addressElements[2] = addressElements[1];
		 addressElements[1] = tmp;
		 }
		 */
		
		// all the parts are "fine" now
		
		// log the newly constructed address
		addressLogger.logString("|" + addressString + "| -> |" + this + "|");
		return;		
	}
	
	public String [] getAddressElements(){                  // get address in terms of the defined elements, in standard order
		
		return addressElements; // simply return the address elements
	}
	
	public String [] getAddressElements(String perm){       // get a specific combination of address elements
		// return the address elements in terms of the specific arrangement
		String [] ret = new String[perm.length()];
		for (int i = 0; i < perm.length(); i++)
			ret[i] = addressElements[perm.charAt(i) - '0'];
		return ret;
	}
	
	public String getAddressElement(int elementNumber){     // get specific element
		return addressElements[elementNumber];
	}
	
	public String getAddress(){                             // get the entire address with all known elements as one string
		String ret = new String();
		int i = 0;
		for (; i < 7; i++) if (addressElements[i] != null) break;
		ret = addressElements[i];
		for (i++; i < 7; i++) if (addressElements[i] != null) ret = ret.concat(" " + addressElements[i]);
		return ret;
	}
	
	public String getAddressFormatted(){
		StringBuilder result = new StringBuilder();
		result.append("Street Number: [").append(StringUtils.defaultString(addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NUMBER])).append("]\n");
		result.append("Predirection: [").append(StringUtils.defaultString(addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_PREDIRECTIONAL])).append("]\n");
		result.append("Street Name: [").append(StringUtils.defaultString(addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_NAME])).append("]\n");
		result.append("Suffix: [").append(StringUtils.defaultString(addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_SUFFIX])).append("]\n");
		result.append("Postdirection: [").append(StringUtils.defaultString(addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_POSTDIRECTIONAL])).append("]\n");
		result.append("Unit Identifier: [").append(StringUtils.defaultString(addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_SEC_ADDR_IDENT])).append("]\n");
		result.append("Unit: [").append(StringUtils.defaultString(addressElements[ro.cst.tsearch.search.address.StandardAddress.STREET_SEC_ADDR_RANGE])).append("]\n");
		
		return result.toString();
	}
	
	public String getAddressMod(){                             // get the entire address with all known elements as one string
		String ret;
		int i = 0;
		ret = addressElements[i] == null ? "" : addressElements[i];
		for (i++; i < 7; i++) ret = ret.concat(":" + (addressElements[i] == null ? "" : addressElements[i]));
		return ret;
	}
	
	public String toString() {
		return getAddressMod();
	}
		
	/**
	 * get rid of TH, ST, ND, RD from the end of the number
	 */
	private String removeNumberExpansion(String number) {
		return number == null ? null : number.replaceAll("^([0-9]*)(ST|ND|RD|TH)$", "$1");
	}
	
	public String getAddrInputString() {
		return addressString;
	}
	
	private boolean extractPattern(String pattern, int[] reorder){
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(addressPattern);
		if(m.matches()){
			// extract the matched tokens, update the addressPattern, compute new number of words
			int pos = 0;
			int newAddressWordCount = 0;
			String newAddressPattern = "";
			for(int i=0; i<m.groupCount(); i++){
				int newPos = reorder[i];
				String word = "";
				int gsize = m.group(i+1).length();
				if(gsize>0){
					if(newPos != -1){
						for(int j=0; j<gsize; j++){
							word += addressWords[pos]; addressWords[pos++]="";
							if(j!=(gsize-1)) word += " ";
						}
						if(addressElements[newPos]!= null)
							addressElements[newPos] += (" " + word);
						else
							addressElements[newPos] = word;
					}else{
						newAddressWordCount += gsize;
						newAddressPattern += addressPattern.substring(pos,pos+gsize);
						pos += gsize;
					}
				}
			}
			addressPattern = newAddressPattern;
			
			// update the addressWords
			String [] newAddressWords = new String[newAddressWordCount];
			int crtWord = 0;
			for(int i=0; i<addressWords.length; i++){
				if(addressWords[i].length() != 0){
					newAddressWords[crtWord++] = addressWords[i];
				}
			}
			addressWords = newAddressWords;
			
			return true; // matched
		}
		else{
			return false; // did not match
		}
	}
	
	private boolean matchPattern(String pattern, int[] reorder){
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(addressPattern);
		if(m.matches()){
			// extract tokens and put them in the right places
			int pos = 0;
			for(int i=0; i<m.groupCount(); i++){
				int newPos = reorder[i];
				String word = "";
				int gsize = m.group(i+1).length();
				if(gsize>0){
					if(newPos != -1){
						for(int j=0; j<gsize; j++){
							word += addressWords[pos++];
							if(j!=(gsize-1)) word += " ";
						}
						if(addressElements[newPos]!= null)
							addressElements[newPos] += (" " + word);
						else
							addressElements[newPos] = word;
					}else{
						pos += gsize;
					}
				}
			}		
			//debug
			//matchedPattern = pattern; 
			return true; // matched
		}
		else{
			return false; // did not match
		}
	}
	
	
	/* //debug
	public static boolean doPrint = false;
	static boolean patternMatched = false;
	private String matchedPattern = null;
	public static void println(String message){
		if(doPrint)
			System.out.println(message);
	}		
		
	public static boolean getPatternMatched(){
		return patternMatched;
	}
	*/
	public static void main(String[] args) {
		
		//123 N DRIVE 100 E
		//958 HWY 221 N
		StandardAddress shortAddress = new StandardAddress("18836-18839 VISTA DEL CANON #G");
		System.out.println(shortAddress.getAddressFormatted());
		
		/*
		String[] addresses = {
				"1995 FM 3006",
				"1995 FM 3006",
				"OAK HILL",
				"OAK HILL DR",
				"WHISPERING WILLOW",
				"WHISPERING WILLOW STR",
				"RIVER FIELD",
				"RIVER FIELD RD",
				"RIDGE CREEK",
				"RIDGE CREEK AVE",
				"CREST WOOD",
				"CREST WOOD LN",
				"RIVER HILL",
				"RIVER HILL TER",
				"FOX TRAIL",
				"FOX TRAIL STR",
				"BAKER GROVE",
				"BAKER GROVE WAY"
		};
		
		for (int i = 0; i < addresses.length; i++) {
			StandardAddress shortAddress = new StandardAddress(addresses[i]);
			StandardAddress longAddress = new StandardAddress(addresses[i+1]);
			System.out.println(addresses[i] + " -> " + StringUtils.join(shortAddress.addressElements, " : "));
			System.out.println(addresses[i+1] + " -> " + StringUtils.join(longAddress.addressElements, " : "));
			
			
			shortAddress.addressElements[0] = null;
			shortAddress.addressElements[1] = null;
			shortAddress.addressElements[3] = null;
			shortAddress.addressElements[4] = null;
			shortAddress.addressElements[5] = null;
			shortAddress.addressElements[6] = null;
			
			longAddress.addressElements[0] = null;
			longAddress.addressElements[1] = null;
			longAddress.addressElements[3] = null;
			longAddress.addressElements[4] = null;
			longAddress.addressElements[5] = null;
			longAddress.addressElements[6] = null;
			
			ro.cst.tsearch.search.address.AddressMatcher addressMatcher = new ro.cst.tsearch.search.address.AddressMatcher(shortAddress);
		
			System.out.println("Score is: " + new BigDecimal(addressMatcher.matches(longAddress)));
			
			i++;
		}
		
		*/
		
	}
}

package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.NumberUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.reports.comparators.IntegerComparator;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;

import com.Ostermiller.util.ArrayHelper;

public class FLColumbiaTR {

	public static final String ACCOUNT_NUMBER_REG_EX = "(?is)[A-Z]\\d+-\\d{3}";
	
	public static final String[] CITIES = {"LAKE CITY", "FORT WHITE", "FT WHITE", "FT WHT",			//incorporated 
		                                   "FIVE POINTS", "WATERTOWN",								//unincorporated
		                                   "HIGH SPRINGS", "WELLBORN"};								//other

	public static void parseAndFillResultMap(ResultMap resultMap, String rowFromIntermediaryResponse) {
		// get the account number -- name
		String rawAccountNumber = RegExUtils.getFirstMatch("(?is)<a.*>(.*)</A>", rowFromIntermediaryResponse, 1).trim();

		String accountNumber = RegExUtils.getFirstMatch(ACCOUNT_NUMBER_REG_EX, rawAccountNumber, 0).trim();
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNumber);

		if (accountNumber.startsWith("R")) {
			resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");
		}

		// get the 2 tables from every row
		List<String> tables = RegExUtils.getMatches("(?is)<TABLE WIDTH=\"98%\".*?</TABLE>", rowFromIntermediaryResponse, 0);

		if (tables.size() == 2) {
			String table = tables.get(0);
			parseIdentificationDetails(resultMap, table);

			// get Legal
			table = tables.get(1);

			parseLegalDescription(resultMap, table);
		}

		// get the asssesed year
		List<String> matches = RegExUtils.getMatches("(?is)Assessed Year.*?(\\d{4,4}).*?<br>", rowFromIntermediaryResponse, 1);
		if (matches.size() == 1) {
			String assesedYear = matches.get(0);
		}

		// get Paid
		String paid = RegExUtils.getFirstMatch("(?is)Paid:\\s*(\\w{1} )", rowFromIntermediaryResponse, 1).trim();

		// get Geo Number
		String geoNumber = RegExUtils.getFirstMatch("(?is)GEO Number:.*?<FONT.*?>(.*?)(?=</FONT>)", rowFromIntermediaryResponse, 1).trim();
		resultMap.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), geoNumber);

		// test
		/*
		 * String testFilesPath = "D:\\work\\FLColumbiaTR\\"; String uniqueID =
		 * (String) resultMap.get("PropertyIdentificationSet.ParcelID");
		 * 
		 * FileUtils.appendToTextFile(testFilesPath + "name.txt", uniqueID +
		 * ": " + resultMap.get("PropertyIdentificationSet.NameOnServer"));
		 * FileUtils.appendToTextFile(testFilesPath + "mailing_address.txt",
		 * uniqueID + ": " +
		 * resultMap.get("PropertyIdentificationSet.AddressOnServer"));
		 * FileUtils.appendToTextFile(testFilesPath + "legal_description.txt",
		 * uniqueID + ": " +
		 * resultMap.get("PropertyIdentificationSet.LegalDescriptionOnServer") +
		 * "\r\n");
		 */

		// end test

	}

	public static void parseIdentificationDetails(ResultMap resultMap, String table) {
		List<String> fonts = RegExUtils.getMatches("(?is)<FONT.*?>(.*?)</FONT>", table, 1);
		int index = 0;
		if (fonts.size() >= 2) {
			// get the name
			String rawName = fonts.get(index).replaceAll("<BR>", "").trim();
			parseName(resultMap, rawName);

			// get the address-- this is the mailing address
			String rawAddress = fonts.get(++index).replaceAll("<BR>", "").trim();
			parseAddress(resultMap, rawAddress);

			if (fonts.size() > index + 1) {
				// get the city , state zip
				String rawState = fonts.get(++index).trim();
			}
		}
	}

	public static void parseLegalDescription(ResultMap resultMap, String table) {
		
		String legalOnServer = table;// fonts.get(0).trim();
		resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalOnServer);
		setSubdivision(resultMap, legalOnServer);
		setUnit(resultMap, legalOnServer);
		setBlock(resultMap, legalOnServer);
		setLot(resultMap, legalOnServer);
		setPhase(resultMap, legalOnServer);
		setCrossReferences(resultMap, legalOnServer);
	}

	public static void parseName(ResultMap resultMap, String tmpOwnerName) {

		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), tmpOwnerName);

		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\n\n\t\n", "<br>");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\+", "&");
		
		String[] ownerRows = tmpOwnerName.split("(?is)<br>");
		String stringOwner = "";
		for (String row : ownerRows){
			if (row.trim().matches("(C/O\\s+)?\\d+\\s+.*")){
				break;
			} else if (row.trim().toLowerCase().contains("box")){
				break;
			} else if (row.trim().toLowerCase().startsWith("pmb")){
				break;
			} else if (row.trim().toLowerCase().matches(".*\\bp o\\b.*")){
				break;
			} else {
				stringOwner += row.trim() + "<br>";
			}
		}
		tmpOwnerName = stringOwner.replaceAll("(?is)&nbsp;<br>", "");
		
		boolean isPortion = false;
		String oldTmpOwnerName = tmpOwnerName;
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\(\\s*\\d+/\\d+\\s*INT?\\s*\\)", "");
		if (!oldTmpOwnerName.equals(tmpOwnerName)) {
			isPortion = true;
		}
		
		tmpOwnerName = tmpOwnerName.replaceAll("\\(?JTWRS\\)?", " & ");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\bOF(\\s+THE)?\\s*<br>", "<br>");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)<br>\\s*\\bOF(\\s+THE)?\\b", "<br>");
		tmpOwnerName = tmpOwnerName.replaceAll("\\s*<br>\\s*", " & ");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\s*&?\\s*D\\S*B\\s*A\\b", "@@@");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\s*&\\s*C/O\\s*", " C/O ");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\s*&\\s*&\\s*", " & ");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\s*&\\s*$", "");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\A\\s*TRUSTEES\\s+OF\\s+", "");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\s+(MOUNT)\\s*&\\s*(\\w+)\\s*$", " $1 $2");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\s+(CHURCH OF)\\s*&\\s*(\\w+(?:\\s+\\w+)?(?:\\s+\\w+)?)\\s*$", " $1 $2");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\s*&\\s*(\\w+ CHURCH)\\s*", " $1 ");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\s*&\\s*(CEMETERY)\\s*", " $1 ");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\s*&?\\s*AS\\s+(TRUSTEES?)\\s*", " $1 ");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\s*&\\s*(ASSOCIATION\\s+INC)\\b", " $1");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)\\bMRS\\b", "");
		tmpOwnerName = tmpOwnerName.replaceAll("(?is)%", " C/O ");
		
		if (!NameUtils.isCompany(tmpOwnerName)) {
			tmpOwnerName = tmpOwnerName.replaceAll("\\d+", "");
		}
		
		// tmpOwnerName = tmpOwnerName.replace("&", "AND");
		// String[] split = tmpOwnerName.split("\\bAND\\b" );

		boolean containsCareOf = tmpOwnerName.contains("C/O");
		tmpOwnerName = tmpOwnerName.replaceAll(" OR ", " AND ");
		Object[] split = null;

		if (containsCareOf) {
			split = tmpOwnerName.split("C/O");
		} else {
			split = tmpOwnerName.split("\\s*&\\s*");
		}

		List body = new ArrayList<List>();

		boolean isCompany = false;
		boolean isCompanySecondItem = false;
		if (split.length >= 2 && !containsCareOf) {
			// test to see if it starts like a company name e.g. FAMILY &
			// COSMETIC DENTISTRY OF FLORIDA %SMILE DESIGN
			// if the first string of the array is a single word=> it is a
			// company
			String firstPart = ((String) split[0]).trim();

			isCompany = isCompany(isCompany, firstPart);
			if (isCompany) {
				split = new String[] { tmpOwnerName };
			} else {
				isCompanySecondItem = NameUtils.isCompany(((String) split[1]).trim());
			}
		}
		
		if (split.length==3) {
			String s0 = (String)split[0];
			String s1 = (String)split[1];
			String s2 = (String)split[2];
			if (s2.trim().split(" ").length==1) {
				if (!LastNameUtils.isLastName(s1) && FirstNameUtils.isFirstName(s1) &&	//WILSON SIDNEY A & DONNA & KELL (Account# R03817-112)
						!FirstNameUtils.isFirstName(s2) && LastNameUtils.isLastName(s2)) {
						split = new Object[2];
						split[0] = s0;
						split[1] = s2 + " " + s1;
						tmpOwnerName = (String)split[0] + " & " + s2 + " " + s1;
					}
			}
		}

		boolean isAlreadyAdded = false;
		
		if (!isCompany) {
			if (split.length == 2 && !containsCareOf) {
				if (isCompany || isCompanySecondItem) {
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, "" + split[0], body);
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, "" + split[1], body);
				} else {
					String secondName = ("" + split[1]).trim();
					secondName = secondName.replaceAll("(?is)\\bD\\s*B\\s*A\\b", "").trim();
					if (LastNameUtils.isLastName("" + split[1]) 
							|| RegExUtils.matches("(?is)\\A\\b\\w+(\\s+\\w{1,1})?(?:\\s+(?:TRUSTEES?|ETAL|ETUX|ETVIR))?\\b$", secondName)) {
						ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, tmpOwnerName, body);
					} else {
						ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, ("" + split[0]).replaceAll("(?is)\\bD\\s*B\\s*A\\b", ""), body);
						ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, ("" + split[1]).replaceAll("(?is)\\bD\\s*B\\s*A\\b", ""), body);
					}
				}
				isAlreadyAdded = true;
			}

			if (split.length == 3 || (split.length == 2 && containsCareOf)) {

				if (containsCareOf) {
					String[] split2 = ((String) split[0]).split("\\s*&\\s*");

					// if (isCompany(isCompany, "" + split[0])){
					split = ArrayHelper.cat(split2, new Object[] { split[1] });
					// }

					// ELIANO'S COFFEE C/O SCOTT STEWART
					if (split.length == 2) {
						if (split[0].toString().contains("@@@")){
							String[] otherSPlit = split[0].toString().split("@@@");
							for (String string : otherSPlit) {
								ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, string.trim(), body);
							}
						} else {
							ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, "" + split[0], body);
						}
						if (NameUtils.isCompany("" + split[1])) {
							String[] parsedName = new String[] { "", "", "", "", "", "" };
							parsedName[2] = "" + split[1];
							String[] suffixes = GenericFunctions.extractNameSuffixes(parsedName);
							String[] types = GenericFunctions.extractNameSuffixes(parsedName);
							String[] otherTypes = GenericFunctions.extractNameSuffixes(parsedName);
							GenericFunctions.addOwnerNames(parsedName, suffixes[0], suffixes[1], types, otherTypes, NameUtils.isCompany(parsedName[2]),
									NameUtils.isCompany(""), body);
							try {
								GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							if (split[1].toString().trim().matches("(?is)(\\w+)\\s*&\\s*(\\w+\\s+\\w+)")) { 
								if (stringOwner.matches(".*%\\s*" + split[1].toString().trim() + ".*")) {
									String name = split[1].toString().trim();
									if (name.matches("(?is)^(\\w+)\\s*&\\s*(\\w+)\\s+(\\w+)$")) {
										name = name.replaceAll("(?is)^(\\w+)\\s*&\\s*(\\w+)\\s+(\\w+)$", " $1 $3 & $2");
										split[1] = name.trim();
									}
								} else {
									String name = split[1].toString().trim();
									name = name.replaceAll("(?is)(\\w+)\\s*&\\s*(\\w+\\s+\\w+)$", " $2 & $1");
									split[1] = name.trim();
								}
							}
							String s1 = (String) split[1];
							s1 = s1.trim();
							if (s1.matches("[A-Z]+\\s+[A-Z]+\\s+[A-Z]")) {
								ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, s1, body);
							} else {
								ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, s1, body);
							}
						}
						isAlreadyAdded = true;	
					}
				}

				if (split.length == 3) {
					// SHELTON CORINE & JOSEPH FENNELL & ARETHA FENNELL TURNER
					// ETAL
					// test to see how the names are combined ; if the last 2
					// are related then parse with DeSotto
					String[] secondName = ((String) split[1]).split("\\s");
					boolean theLast2NamesAreRelated = false;
					for (String string : secondName) {
						if (((String) split[2]).contains(string)) {
							if (string.length() > 1) {// initials doesn't count:
														// MCCANS BILL &BOBBY J
														// MERRITT &LUTHER J
														// MOORE
								theLast2NamesAreRelated = true;
							}

						}
					}
					if (!theLast2NamesAreRelated) {
						if (isPortion) {
							ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, (String) split[0], body);
							ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, (String) split[1], body);
						} else {
							ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, split[0] + " AND " + split[1], body);
						}
						if (!containsCareOf) {
							ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, (String) split[2], body);
							// ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap,
							// (String) split[2], body);
						} else {
							ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, (String) split[2], body);
						}
					} else {
						ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, "" + split[0], body);
						ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, split[1] + " & " + split[2], body);
					}
					isAlreadyAdded = true;
				} else if (split.length == 5) {
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, (String) split[0] + "  & " + (String) split[1], body);
					String name23 = "";
					if (((String)split[2]).contains(" ")) {
						name23 = split[2] + " & " + split[3];
					} else {
						name23 = split[2] + " AND " + split[3];
					}
					ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, name23, body);
					ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, (String) split[4], body);
					isAlreadyAdded = true;
				}
			}
		}
		// deal with the simple case: GREEN RHONDA B
		if (split.length == 1) {
			ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, "" + split[0], body);
		} else if (!isAlreadyAdded && split.length > 2){//PARKER GREGORY S & PAMELA R & HALL GALE PARKER & HUGH JAMES
			split = tmpOwnerName.replaceFirst("(?is)(&\\s*\\w+\\s+\\w{1,})\\s*&", "$1@@#@@").split("@@#@@");
			for (int i = 0; i < split.length; i++) {
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, "" + split[i], body);
			}
		}
	}

	public static boolean isCompany(boolean isCompany, String firstPart) {
		if (!RegExUtils.matches("\\s*", firstPart)) {
			isCompany = true;
		}
		return isCompany;
	}

	public static void parseAddress(ResultMap resultMap, String addressOnServer) {
		
		if (addressOnServer.matches("(?is)^\\s*LOT.*") ||		//not an address, e.g. LOT 22 WEST BLK COL E/W (Account Number R03755-025) 
			addressOnServer.matches("(?is).*S/D\\s*$")	) {		//ANCIENT OAKS S/D (Account Number R03752-205)  
			return;
		}
		
		addressOnServer = addressOnServer.replaceAll("^\\s*-+\\s*$", "");
		addressOnServer = addressOnServer.replaceAll("\\*LIFE\\s*$", "");	//845 NORRIS SW LAKE CITY *LIFE (Account Number R03480-004) 
		
		if (StringUtils.isEmpty(addressOnServer)) {
			return;
		}
		
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addressOnServer);

		String[] split = StringFormats.parseCityFromAddress(addressOnServer, CITIES);
		resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), split[0]);
		addressOnServer = split[1];
		
		String streetName = StringFormats.StreetName(addressOnServer);
		String streetNo = StringFormats.StreetNo(addressOnServer);

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
	}

	public static String setBlock(ResultMap resultMap, String tmpLegalDescription) {
		String blockRegEx = "\\b(BLOCK|BLK)(\\s?\\w+)";
		String[] findRepeatedOcurrence = ro.cst.tsearch.utils.StringUtils
				.findRepeatedOcurrence(tmpLegalDescription, "BLOCK", blockRegEx, 2);

		String block = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("", StringUtils.stripAll(findRepeatedOcurrence));

		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		return tmpLegalDescription.replaceAll(blockRegEx, "");
	}

	public static String setUnit(ResultMap resultMap, String tmpLegalDescription) {
		String blockRegEx = "\\b(UNIT)(\\s?\\w+)";
		String[] findRepeatedOcurrence = ro.cst.tsearch.utils.StringUtils.findRepeatedOcurrence(tmpLegalDescription, "UNIT", blockRegEx, 2);

		String block = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("", StringUtils.stripAll(findRepeatedOcurrence));

		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), block);
		return tmpLegalDescription.replaceAll(blockRegEx, "");
	}

	public static String setPhase(ResultMap resultMap, String tmpLegalDescription) {
		String phaseRegEx = "\\sPHA?SE?\\s*(\\d+)";
		List<String> list = RegExUtils.getMatches(phaseRegEx, tmpLegalDescription, 1);
		String phase = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("",
				StringUtils.stripAll((String[]) list.toArray(new String[] {})));

		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		return tmpLegalDescription;
	}

	public static String setLot(ResultMap resultMap, String tmpLegalDescription) {
		// case 1 : LOT 21 ARBOR GREENE AT EMERALD LAKES & LOTS 24 & 25 ARBOR
		// GREENE AT EMERALD LAKES PHS 2 & LOT 98
		// case 2 : LOT 9 & LOT 10
		// case 3 : LOTS 1, 2, 3, 4 BLOCK D
		// case 4 : LOTS 9, 10, 11, 12, 13, 14 & 15 BLOCK 15
		// case 5 : LOTS 54 THRU 74 BLOCK B
		// case 6 : LOTS 7,8,17,18,19,20, 21,22 & 23
		// case 7 : LOT 1 EX RD & E1/2 LOT 2 BLOCK 1 LAKE VILLAS
		// case 8 : LOTS 1, 2, 3 & 4 BLOCK C PINEWOOD S/D AND A STRIP OF LAND
		// case 9 : ADJACENT TO W SIDE OF LOTS DESC

		// deal with LOT 4 fasdfas LOT 3
		String lotRegEx = "\\b(LOT)(\\s?\\w+)";
		String[] findRepeatedOcurrence = ro.cst.tsearch.utils.StringUtils.findRepeatedOcurrence(tmpLegalDescription, "LOT ", lotRegEx, 2);

		String result = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("", findRepeatedOcurrence);

		// deal with LOTS 2 , 3, 4 & 5
		tmpLegalDescription = tmpLegalDescription.replaceAll("(\\d)(,)", "$1 ");
		String regExLots = "(?is)LOTS ([\\d+\\s]+)";
		String values = ro.cst.tsearch.utils.StringUtils.getSuccessionOFValues(tmpLegalDescription, "LOTS", regExLots);
		String[] lotSuccession = values.split(" ");
		result = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString(result, lotSuccession);

		String lotIntervalDelimiters = "(THRU)";
		String lotInterval = ro.cst.tsearch.utils.StringUtils.parseByRegEx(tmpLegalDescription, "(?is)LOTS\\s*(\\d+\\s"
				+ lotIntervalDelimiters + "\\s*?\\d+)", 1);
		result = enumerationFromInterval(result, lotInterval);

		String[] strings = result.split("\\s+");
		Arrays.sort(strings, new IntegerComparator());
		String lotValues = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("", strings).trim();

		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lotValues);
		return tmpLegalDescription;
	}

	public static String enumerationFromInterval(String result, String lotInterval) {
		if (StringUtils.isNotEmpty(lotInterval)) {
			String[] split = lotInterval.split("THRU");
			if (split.length == 2) {
				String l1 = split[0];
				String l2 = split[1];
				int i1 = 0;
				int i2 = 0;
				if (NumberUtils.isDigits(l1.trim()) && NumberUtils.isDigits(l2.trim())) {
					i1 = Integer.valueOf(l1.trim());
					i2 = Integer.valueOf(l2.trim());
					StringBuilder lotSuccesionFromInterval = new StringBuilder();
					for (int i = i1; i <= i2; i++) {
						lotSuccesionFromInterval.append(MessageFormat.format("{0} ", i));
					}
					result = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString(result,
							new String[] { lotSuccesionFromInterval.toString() });
				} else {
					result += lotInterval;
				}

			}
		}
		return result;
	}

	public static String setCrossReferences(ResultMap resultMap, String tmpLegalDescription) {
		String ld = setSecTwnRng(resultMap, tmpLegalDescription);

		String[] header = new String[] { "Book", "Page" };

		// case 1: 5.00 Acres W1/2 OF NW1/4 OF NW1/4 OF NE1/4 EX RD R/W. ORB
		// 403-632, 714-258, DC MABLE WESTPHAL 971-2687. PROB#05-158CP 1059-1075
		// THRU 1086, WD 1145-2210,

		// case 2: .37 Acres LOTS 6 & 7 BLOCK 2 GRANDVIEW VILLAGE UNIT 1. ORB
		// 648-528 697-128, LIFE ESTATE TO JONATHAN L WILLIAMS & PAULINE B
		// WILLIAMS ORB 699-817, PROB#03-01-CP 972-2534 THRU

		// case 3: 06-4S-17 0100/0100 .15 Acres LOT 1 BLOCK 1 GRANDVIEW VILLAGE
		// S/D UNIT 1. ORB 668-465, PROB #01-88-CP ORB 925-1754 THRU 1768,
		// 930-1051 THRU 1057, WD 1089- 2568,

		// remove PROB #01-88-CP
		ld = ld.replaceAll("(?is)PROB\\s#\\d+-\\d+-\\w+", "");

		List<HashMap<String, String>> sourceSet = new LinkedList<HashMap<String, String>>();

		// treat special case : 1037-2720, 2721
		List<String> intervalList = RegExUtils.getMatches("(?is)\\d+-\\d+\\s*?,\\s*?\\d+(?=,|\\.)", ld, 0);
		List<String> bookPageListAsExpected = new ArrayList<String>();

		// R08019-101: 06-4S-17 0100/0100 .15 Acres LOT 1 BLOCK 1 GRANDVIEW
		// VILLAGE S/D UNIT 1. ORB 668-465, PROB #01-88-CP ORB 925-1754 THRU
		// 1768, 930-1051 THRU 1057, WD 1089- 2568,
		// List<String> intervalList2 =
		// RegExUtils.getMatches("(?is)\\d+-\\d+\\s*?THRU\\s*?\\d+(?=,|\\.)",
		// ld, 0);
		// intervalList.addAll(intervalList2);

		// build the list for the found intervals
		/*
		 * proved that intervals aren't necessary. the first value is enough for
		 * (String interval : intervalList) { String[] intervalLimits =
		 * interval.split(",|THRU|-");
		 * 
		 * if (intervalLimits.length == 3) { // bring intervalLimits to expected
		 * form intervalLimits = new String[] { "" + intervalLimits[0] + "-" +
		 * intervalLimits[1], intervalLimits[2] }; }
		 * 
		 * if (intervalLimits.length == 2) { String[] split =
		 * intervalLimits[0].split("-"); if (split.length == 2) { String book =
		 * split[0].trim(); String start = split[1].trim(); String end =
		 * split[1].trim();
		 * 
		 * if (StringUtils.isNumeric(start) && StringUtils.isNumeric(end)) { for
		 * (int i = Integer.valueOf(start); i <= Integer.valueOf(end); i++) {
		 * bookPageListAsExpected.add("" + book + "-" + i); } } } } }
		 */

		// normal interval form
		intervalList.addAll(RegExUtils.getMatches("(?is)\\d+\\s?-\\d+", ld, 0));
		bookPageListAsExpected.addAll(intervalList);
		// put them in the set
		if (intervalList.size() > 0) {
			for (String bp : bookPageListAsExpected) {
				HashMap<String, String> map = new HashMap<String, String>();
				String[] bookPage = bp.split("-");
				if (bookPage.length == 2) {
					map.put("Book", ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(bookPage[0].trim()));
					map.put("Page", ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(bookPage[1].trim()));
					sourceSet.add(map);
				}
			}
		}

		ResultBodyUtils.buildInfSet(resultMap, sourceSet, header, CrossRefSet.class);
		return tmpLegalDescription;
	}

	public static String setSubdivision(ResultMap resultMap, String tmpLegalDescription) {
		String regEx = "(BLOCK|COR OF|LOTS)(.*?)(S/D|ADD|SUBDIVISION|PHS|UNIT)"; // "(BLOCK|COR OF|LOTS)(.*?)(?=(S/D|ADD|SUBDIVISION|PHS)|UNIT)";
		String string = tmpLegalDescription;
		boolean matched = false;
		while (RegExUtils.matches(regEx, string)) {
			string = RegExUtils.getFirstMatch(regEx, string, 0);
			string = string.replaceAll(regEx, "$2 $3");
			matched = true;
		}
		if (matched) {
			regEx = "([A-Z]{2,}\\s*)+(?=(S/D|ADD|SUBDIVISION|PHS|UNIT))";
			string = StringUtils.defaultIfEmpty(RegExUtils.getFirstMatch(regEx, string, 0), "").trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), string);
		}

		return tmpLegalDescription;
	}

	public static String setSecTwnRng(ResultMap resultMap, String tmpLegalDescription) {
		// remove this kind of legals : " 5413/ ROBIN BUCK  512-419-6600"; don't
		// know what the numbers represent
		String regEx = "\\d{4}/\\s+\\w+\\s*\\w+\\s*\\d+-\\d+-\\d+";
		tmpLegalDescription = tmpLegalDescription.replaceAll(regEx, "");

		String secTwnRngRegEx = "\\d+-\\w+-\\d+(?=\\s*\\d+/\\d+)";
		String match = RegExUtils.getFirstMatch(secTwnRngRegEx, tmpLegalDescription, 0);

		if (StringUtils.isNotEmpty(match) && !match.equals("00-00-00")) {
			String[] split = match.split("-");
			if (split.length == 3) {
				int i = 0;
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), split[i]);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), split[++i]);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), split[++i]);
			}
		}

		return tmpLegalDescription.replaceAll(secTwnRngRegEx, "");
	}
}

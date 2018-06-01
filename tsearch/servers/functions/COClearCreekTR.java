package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class COClearCreekTR {
	
	public static Pattern  nameFMFMLPattern = Pattern.compile("(?ism)\\w+\\s+\\w\\s+&\\s+(?:\\w\\s?)*\\s(\\w+)");

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 3) {
			String[] col0 = cols[0].toPlainTextString().trim().split("\\s+");
			resultMap.put("PropertyIdentificationSet.ParcelID",col0[1]);
			resultMap.put("TaxHistorySet.Year", col0[2].replaceAll("[()]", ""));
			
			String ownerName = cols[1].toPlainTextString().trim().replace("&", "\n");
			
			try {
				parseNames(resultMap, ownerName, null, false, searchId);
				parseAddress(resultMap, cols[2].toPlainTextString().trim(), searchId);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return resultMap;
	}
	
	public static void parseNames(ResultMap resultMap, String ownerName, boolean hasAddress) {
		
		if(ownerName.matches("\\s*")) {
			return;
		}
		
		Vector<String> excludeWords = new Vector<String>();
		String[] nameLines = ownerName.split("\n");
		
		if(hasAddress) {
			// remove last line if it has the form [city] CO [zip]
			// removeAddressFLTR fails to remove it because of "CO"
			if(nameLines[nameLines.length - 1].matches(".*\\sCO\\s+\\d{5}(-\\d+)?")) {
				String[] _nameLines = new String[nameLines.length - 1];
				for(int i = 0; i < nameLines.length - 1; i++) {
					_nameLines[i] = nameLines[i];
				}
				nameLines = _nameLines;
			}
			nameLines = GenericFunctions.removeAddressFLTR(nameLines,
					excludeWords, new Vector<String>(), 0, Integer.MAX_VALUE);
		}
		
		String onlyNames = "";
		for(String line : nameLines) {
			onlyNames += line + "\n";
		}
		
		try { 
			parseNames(resultMap, onlyNames.trim(), null, false, -1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, String ownerName, List<List> namesList, boolean firstNameIsCompany, long searchId) throws Exception {
	
		if(ownerName.matches("\\s*")) {
			return;
		}
		
		ownerName = NameCleaner.cleanNameNew(ownerName);
		ownerName = ownerName.replaceAll("[0-9]*%", "");
		ownerName = ownerName.replaceAll("\\s*\n\\s*([A-Z]\\s+[A-Z])\\s*$", " and $1");//Pitkin 273512454003
		String[] nameLines = ownerName.split("\n");
		
		if(namesList == null) {
			namesList = new ArrayList<List>();
		}
		
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
		String namesWithoutCompanies = "";
		for(int i=0; i < nameLines.length; i++) {
			String name = nameLines[i].trim();
			if (NameUtils.isCompany(name, new Vector<String>(), true)) {
				if(i == 0) {
					firstNameIsCompany = true;
				}
				names[2] = name;
				
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]), NameUtils
								.isCompany(names[5]), namesList);
			} else {
				namesWithoutCompanies += " & " + name;
			}
		}
		namesWithoutCompanies = namesWithoutCompanies.replaceFirst("&", "").trim();
		String previousLast;
		if(StringUtils.isNotEmpty(namesWithoutCompanies)) {
			String[] namesArray = namesWithoutCompanies.split("&");
//			if(namesArray.length>3) {
				previousLast = "";
				for(int i=0; i < namesArray.length; i++) {
					String eachName = namesArray[i].trim();
					if(StringUtils.isEmpty(eachName)) {
						continue;
					}
					if(eachName.trim().matches("\\A\\s*(\\w{2,}(\\s+\\w)?)\\s*$|((\\w\\s+)*\\w{2,})")) {
						eachName = eachName + " " + previousLast;	
					}

					if(i == 0 && !firstNameIsCompany) {
						if (eachName.trim().matches("(?is)\\A\\w+\\s+[A-Z]\\.?\\s+\\w+\\s*,.*")){
							eachName = eachName.replaceAll("(?is)\\s*,\\s*", " ");
							names = StringFormats.parseNameDesotoRO(eachName, true);
							
							type = GenericFunctions.extractAllNamesType(names);
							otherType = GenericFunctions.extractAllNamesOtherType(names);
							suffixes = GenericFunctions.extractNameSuffixes(names);
						} else {
							names = StringFormats.parseNameNashville(eachName, true);
							
							type = GenericFunctions.extractAllNamesType(names);
							otherType = GenericFunctions.extractAllNamesOtherType(names);
							suffixes = GenericFunctions.extractNameSuffixes(names);
						}
					} else {
						if (eachName.split(" ").length==2 && LastNameUtils.isLastName(eachName.split(" ")[0]) && 
							!FirstNameUtils.isFirstName(eachName.split(" ")[0]) &&
							FirstNameUtils.isFirstName(eachName.split(" ")[1]) &&
							!LastNameUtils.isLastName(eachName.split(" ")[1])) {
								names = StringFormats.parseNameNashville(eachName, true);
						} else {
							names = StringFormats.parseNameFML(eachName, new Vector<String>(), true, true);
						}
						
						type = GenericFunctions.extractAllNamesType(names);
						otherType = GenericFunctions.extractAllNamesOtherType(names);
						suffixes = GenericFunctions.extractNameSuffixes(names);
						
						if (NameUtils.isNotCompany(eachName)){
							String[] tokens = eachName.split("\\s+"); 
							if (tokens.length == 3 && LastNameUtils.isLastName(tokens[0])  
									&& LastNameUtils.isLastName(tokens[1])){
								names = StringFormats.parseNameNashville(eachName.trim().replaceAll("(\\w+)\\s+(\\w+)\\s+(\\w+)", "$1-$2 $3"), true);//Pitkin 273718404705
								names[2] = names[2].replaceAll("-", " ");
								
								type = GenericFunctions.extractAllNamesType(names);
								otherType = GenericFunctions.extractAllNamesOtherType(names);
								suffixes = GenericFunctions.extractNameSuffixes(names);
								
							} else if (eachName.trim().matches("(?is)\\A\\w+\\s+\\w\\s+\\w\\s+and\\s+\\w\\s+\\w$")){//Pitkin 273512454003
								names = StringFormats.parseNameNashville(eachName, true);
								
								type = GenericFunctions.extractAllNamesType(names);
								otherType = GenericFunctions.extractAllNamesOtherType(names);
								suffixes = GenericFunctions.extractNameSuffixes(names);
								
							}
							else if (LastNameUtils.isLastName(tokens[0]) && FirstNameUtils.isFirstName(tokens[1])){//PitkinAO R008380
								names = StringFormats.parseNameNashville(eachName, true);
								
								type = GenericFunctions.extractAllNamesType(names);
								otherType = GenericFunctions.extractAllNamesOtherType(names);
								suffixes = GenericFunctions.extractNameSuffixes(names);
								
							}
						}
					}
					
//					names = NameCleaner.tokenNameAdjustment(names);
					names = NameCleaner.removeUnderscore(names);
					
					if(!LastNameUtils.isLastName(names[2])) {
						if(names[1].equals("") && FirstNameUtils.isFirstName(names[2])) { //MARY ANN
							names[1] = names[2];
							names[2] = previousLast;
						} else if(names[1].matches("[A-Z]\\s+[A-Z]{2,}")) { //PEGGY A OGLE T
							String[] middleLast = names[1].split("\\s+");
							names[1] = middleLast[0];
							names[2] = middleLast[1];
						} else if(!FirstNameUtils.isFirstName(names[0]))
							if(names[1].matches("[A-Z]{2,}\\s+[A-Z]")) { // LA GRECA KENNETH W
								String[] firstMiddle = names[1].split("\\s+");
								names[2] += " " + names[0];
								names[0] = firstMiddle[0];
								names[1] = firstMiddle[1];
							} else if(names[0].length() == 1 && names[1].matches("[A-Z]{2,}\\s+[A-Z]{2,}")) { // Marlynn K La Greca
								String[] firstLast = names[1].split("\\s+");
								names[1] = names[0];
								names[0] = firstLast[0];
								names[2] = firstLast[1] + " " + names[2];
						} 
						if (StringUtils.isEmpty(names[1]) && previousLast.length()!=0){
							names[1] = names[2];
							names[2] = previousLast;
						}
					}
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]), NameUtils
									.isCompany(names[5]), namesList);
					
					previousLast = names[2].replace(" ", "_");
				}
				GenericFunctions.storeOwnerInPartyNames(resultMap, namesList, true);
//			} else {
//				GenericFunctions2.parseName(resultMap, namesWithoutCompaines, namesList, 0);
//			}
		}else {
			GenericFunctions.storeOwnerInPartyNames(resultMap, namesList, true);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseLegalSummary(ResultMap resultMap, long searchId) {
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if (legalDescription == null) {
			return;
		}
		
		if(legalDescription.matches("\\s*")) {
			return;
		}
		
		//fix for accountNo R004287 --> Lot: 5 AND:- Lot: 6 & E 51/2 FT L7
		legalDescription = legalDescription.replaceAll("(?is)(Lot:\\s+[^&]+&)\\s*\\b[NSEW]\\b\\s*\\d+\\s*/\\s*\\d+\\s*\\bFT\\b\\s+L([\\d+])", "$1 $2"); 
		
		String[] tokens = legalDescription.split("\\s+");
		String subdivision = "";
		String block = "";
		String lot = "";
		
		// extract subdivision name, block and lot
		for(int i=0; i < tokens.length ; i++) {
			if(tokens[i].toUpperCase().startsWith("SUBDIVISION:")) {
				while(tokens[++i].matches("[A-Z&]+") &&
						!tokens[i].matches("SP|PT|RV|PARCEL|AMD")) {
					subdivision += tokens[i] + " ";
				}
				i--;
				
			} else if (tokens[i].toUpperCase().startsWith("BLOCK:")) {
				while(tokens[++i].matches("[-\\d&,\\s]+") &&
						!tokens[i].matches("Lot:")) {
					block +=  tokens[i] + " ";
				}
				i--;
				
			} else if (tokens[i].toUpperCase().startsWith("LOT:")) {
				while(tokens[++i].matches("[\\d\\s&,]+(?:-?\\s*[A-Z])?") &&
						!tokens[i].matches("SP|PT|RV|PARCEL|AMD")) {
					lot +=  tokens[i] + " ";
				}
				i--;
				
			} else if(tokens[i].toUpperCase().startsWith("MINE:")) {
				break;
			} 
		}
		
		// extract cross-refs
		Matcher m = Pattern.compile("\\b(\\d+)/(\\d+)\\b").matcher(legalDescription);
		List<List> body = new ArrayList<List>();
		while(m.find()) {
			List<String> line = new ArrayList<String>();
			line.add(m.group(1));
			line.add(m.group(2));
			line.add("");
			line.add("");
			body.add(line);
			legalDescription = m.replaceFirst("");
			m.reset(legalDescription);
		}
		if(body.size() > 0) {
			try {
				GenericFunctions2.saveCRInMap(resultMap, body);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision.trim());
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), 
				LegalDescription.cleanValues(block.trim(), false, true));
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), 
				LegalDescription.cleanValues(lot.trim().replaceAll("&", ""), false, true));
		
		COWeldTR.extractInfoIntoMap(resultMap, legalDescription, "([\\d\\.,]+)\\s*\\bACRES[A-Z]+\\s+(\\d+)[-\\s](\\d+)[-\\s](\\d+)\\b",
				PropertyIdentificationSetKey.QUARTER_VALUE.getKeyName(),
				PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),
				PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(),
				PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName());

		String strPattern = "\\b(\\d+)[-\\s](\\d+)[-\\s](\\d+)\\b";
		if (legalDescription.indexOf("S: ") > -1 && legalDescription.indexOf("T: ") > -1 && legalDescription.indexOf("R: ") > -1) {
			strPattern = "\\bS:\\s+(\\d+)\\s+\\bT:\\s+(\\d+)\\s+\\bR:\\s+(\\d+)";
		}
		COWeldTR.extractInfoIntoMap(resultMap, legalDescription, strPattern,
				PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),
				PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(),
				PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName());
		
		COWeldTR.extractInfoIntoMap(resultMap, legalDescription, "\\bU(?:nit:?\\s*)?(\\d+)",
				PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName());
	}
	
	public static void parseAddress(ResultMap resultMap, String address, long searchId) {
		if(address.matches("\\s*")) {
			return;
		}
		
		String[] addressArray = address.split("\\s+");
		String city = "";
		String newAddress = "";
		int newLength = addressArray.length;
		if(!addressArray[newLength - 1].matches("[0-9]+")) {
			city = addressArray[newLength - 1];
			newLength--;
		} else {
			city = addressArray[newLength - 2];
			newLength -= 2;
		}
		
		if(addressArray[newLength - 1].matches("[0-9]+") && addressArray[newLength - 2].matches("[0-9]+")
			&& (Normalize.isSuffix(addressArray[newLength - 3]) || Normalize.isDirectional(addressArray[newLength - 3]))
			&& !address.trim().toUpperCase().startsWith("COUNTY ROAD")) {
			
			newAddress += addressArray[newLength - 2] + " ";
			for(int i = 0; i < newLength - 2; i++) {
				newAddress += addressArray[i] + " ";
			}
			newAddress += "#" + addressArray[newLength - 1];
		} else {
		
			int numbersCount = 0;
			for(int i=0; i < newLength; i++) {
				if(addressArray[i].matches("[0-9]+")) {
					numbersCount++;
					if(numbersCount == 2) {
						newAddress = addressArray[i] + " " + newAddress;
					} else if(numbersCount == 3){
						newAddress += " #" + addressArray[i];
					} else {
						newAddress += " " + addressArray[i];
					}
				} else {
					newAddress += addressArray[i] + " ";
				}
			}
		}
		
		resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(newAddress.trim()));
		resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(newAddress.trim()));
		
		if(city.toUpperCase().equals("DUMONT")) {
			resultMap.put("PropertyIdentificationSet.City", "Dumont");
		} else if(city.toUpperCase().equals("EMPIRE")) {
			resultMap.put("PropertyIdentificationSet.City", "Empire");
		} else if(city.toUpperCase().equals("GT")) {
			resultMap.put("PropertyIdentificationSet.City", "Georgetown");
		} else if(city.toUpperCase().equals("IS")) {
			resultMap.put("PropertyIdentificationSet.City", "Idaho Springs");
		} else if(city.toUpperCase().equals("SP")) {
			resultMap.put("PropertyIdentificationSet.City", "Silver Plume");
		} else if(city.toUpperCase().equals("LAWSON")) {
			resultMap.put("PropertyIdentificationSet.City", "Lawson");
		} else if(city.toUpperCase().equals("DOWNIEVILLE")) {
			resultMap.put("PropertyIdentificationSet.City", "Downieville");
		} else if(city.toUpperCase().equals("EG")) {
			resultMap.put("PropertyIdentificationSet.City", "Evergreen");
		}

	}

}
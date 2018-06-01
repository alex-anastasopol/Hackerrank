package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class COPitkinAO {
	
	private static final String[] PITKIN_CITIES = {
			"ASHCROFT",
			"ASPEN",
			"BASALT",
			"EMMA",
			"LENADO",
			"MEREDITH",
			"NAST",
			"NORRIE",
			"PLACITA",
			"REDSTONE",
			"SNOWMASS",
			"SNOWMASS VILLAGE",
			"THOMASVILLE",
			"WINGO",
			"WOODY CREEK"
	};
	
	private static final String[] ROUTT_CITIES = {
			"Battle Creek",
			"Clark",
			"Columbine",
			"Dunckley",
			"Elkhead",
			"Glen Eden",
			"Haybro",
			"Hayden",
			"Keystone",
			"McGregor",
			"Milner",
			"Mount Harris",
			"Mystic",
			"Oak Creek",
			"Pagoda",
			"Phippsburg",
			"Steamboat Springs",
			"Toponas",
			"Trapper",
			"Yampa"
	};
	
	public static ResultMap parseIntermediaryRow(String crtCounty, TableRow row, TableRow additionalRow, long searchId) {
			
		ResultMap resultMap = new ResultMap();
		
		if (crtCounty.equalsIgnoreCase("Pitkin")){
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
		} else {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		}
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 3) {
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),cols[2].toPlainTextString().trim());
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), cols[0].toPlainTextString().trim());
			if(cols[0].toPlainTextString().trim().startsWith("R")) {
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");	
				}
			
			try {
				parseNames(resultMap, searchId);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		cols = additionalRow.getColumns();
		if(cols.length == 2) {
			String address = cols[0].toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			if (crtCounty.equalsIgnoreCase("Pitkin")){
				parseAddress(resultMap, searchId);
			} else if (crtCounty.equalsIgnoreCase("Routt")){
				parseAddressCORouttTR(resultMap, searchId);
			}
		}
		
		return resultMap;
	}
	
	public static void parseAddress(ResultMap resultMap, long searchId) {
		
		String address = (String)resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		
		address = address.replaceAll("(?is)\\b(NORTH|SOUTH|EAST|WEST) SIDE\\b", "\"$1 SIDE\"");
		
		if (ro.cst.tsearch.utils.StringUtils.isEmpty(address.trim()))
			return;
		
		String[] addressArray = address.split("\\s+");
		String city = "";
		String properAddress = "";
		boolean foundCity = false;
		for(int i = addressArray.length - 1; i >= 0; i--) {
			if(!foundCity) {
				if(Normalize.isSuffix(addressArray[i]) 
						|| Normalize.isDirectional(addressArray[i])
						|| addressArray[i].matches("[0-9]+")) {
					if(addressArray[i].equals("CREEK") && addressArray[i-1].equals("WOODY")
							|| addressArray[i].equals("VILLAGE") && addressArray[i-1].equals("SNOWMASS")){
						city += addressArray[i];
					} else {
						properAddress = addressArray[i] + " " + properAddress;
						foundCity = true;
					}
				} else {
					city = addressArray[i] + " " + city;
					if(isPitkinCity(city.trim())) {
						foundCity = true;
					}
				}
			} else {
				properAddress = addressArray[i] + " " + properAddress;
			}
		}
		
		String[] streetNameArray = StringFormats.StreetName(properAddress.trim()).split("\\s+");
		String streetName = "";
		
		for(int i = streetNameArray.length - 1; i >= 0; i--) {
			if(streetName.indexOf("\"") < 0
					&& !Normalize.isSuffix(streetNameArray[i]) 
					&& !Normalize.isDirectional(streetNameArray[i])) {
				streetName = streetNameArray[i] + "\" " + streetName; 
			} else if(streetName.indexOf("\"") >= 0 
					&& Normalize.isDirectional(streetNameArray[i])){
				streetName = streetNameArray[i] + " \"" + streetName;
			} else {
				streetName = streetNameArray[i] + " " + streetName;
			}
		}
		if(streetName.indexOf("\"") > -1 && streetName.indexOf("\"") == streetName.lastIndexOf("\"")) {
			streetName = "\"" + streetName;
		}
		//if a property is a condo and the address has no unit, then the unit from legal is added to the address
		String condo = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName());
		if (StringUtils.isNotEmpty(condo)){
			String unit = (String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName());
			if (StringUtils.isNotEmpty(unit)){
				if (!streetName.trim().matches("\\d+$")){
					streetName += " #" + unit;
				}
			}
		}
		streetName = streetName.replaceAll("\\s+", " ");
		streetName = streetName.replaceAll("\"+", "\"");
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(properAddress.trim()));
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		if(!city.equals("")){
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.trim());
		}
	}
	
	public static void parseAddressCORouttTR(ResultMap resultMap, long searchId) {

		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
			
		if (StringUtils.isEmpty(address))
			return;
			
		String city = "";
		for (int i = 0; i < ROUTT_CITIES.length; i++){
			if (address.toLowerCase().contains(ROUTT_CITIES[i].toLowerCase())){
				city = ROUTT_CITIES[i];
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				address = address.replaceAll(city.toUpperCase() + ".*", "");
				break;
			}
		}
			
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address.trim()));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address.trim()));
		
	}
	
	private static boolean isPitkinCity(String cityName) {
		for(int i = 0; i < PITKIN_CITIES.length; i++) {
			if(cityName.equals(PITKIN_CITIES[i])) {
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseLegalSummary(ResultMap resultMap, long searchId) throws Exception {
		
		String legal = (String)resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if(legal.matches("\\s*")) {
			return;
		}
		
		legal = legal.replaceAll("(?is)(Lot:)\\s*([A-Z])\\s+AND\\s+[NSWE]+\\s+HALF\\s+OF", "$1 $2 $1 ");
		legal = legal.replaceAll("(?is)\\s+THRU\\s*:\\s*-\\s*Lot:\\s*", "-");
		legal = legal.replaceAll("(?is)\\b([A-Z])\\s+THRU\\s+([A-Z])\\b", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(Lot:)\\s+Thru\\b", "$1 ");	
		legal = legal.replaceAll("(?is)&quot;", "\"");
		legal = legal.replaceAll("(?is)(\\d+)\\s+(\\d+\\s*-\\s*\\d+\\s*-\\s*\\d+)\\b", "$1 ; $2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");
		legal = legal.replaceAll("(?is)\\b([A-Z])\\s+AND\\s+([A-Z])\\b", "$1 & $2");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		String legalTemp = legal;
					
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)(Lots?)\\s*:?\\s*([\\d\\s&-]+-?[A-Z]?|[A-Z]-?(?:\\d+|[A-Z])?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}

		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)(Blo?c?k)\\s*:?\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)(Unit)\\s*:?\\s*((?:[A-Z]{1,2})?-?[\\d\\s&]+-?(?:[A-Z]{1,2})?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

	
		// extract tract from legal description
		p = Pattern.compile("(?is)(Tr(?:act)?)\\s*:?\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(1));
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)(Bldg)\\s*\\.?\\s*(\\d?[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		//extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("(?is)(Plat)\\s+BK\\s*(\\d+)\\s+PG\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = pb + " " + ma.group(2);
			pg = pg + " " + ma.group(3);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pb.trim());
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pg.trim());
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		//section/township/range
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {

			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(2).trim());
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(3).trim());
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(4).trim());
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(BK)\\s*(\\d+)\\s+PGS?\\s*([\\d-\\s&]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String book = ma.group(2).trim();
			String page = ma.group(3).trim();
			page = page.replaceAll("[\\s&]+", "-");
			if (page.contains("-")){
				String[] pages = page.split("-");
				for (String pag : pages){
					List<String> line = new ArrayList<String>();
					line.add(book);
					line.add(pag);
					line.add("");
					bodyCR.add(line);
				}
			} else {
				List<String> line = new ArrayList<String>();
				line.add(page);
				line.add(book);
				line.add("");
				bodyCR.add(line); 
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		p = Pattern.compile("(?is)(DOC|REC)\\s*#?\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(2));
			bodyCR.add(line); 
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			resultMap.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;	
	}

	@SuppressWarnings({"rawtypes" })
	public static void parseNames(ResultMap resultMap, long searchId) throws Exception {
		
		String ownerName = (String)resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		
		if(ownerName == null)
			return;
		
		String possibleName = (String)resultMap.get("tmpPossibleName");
		if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(possibleName)
				&& !possibleName.matches("\\d+(-\\d+)?\\s+.*")	// R014435
				&& !possibleName.toLowerCase().contains("box")
				&& !possibleName.toLowerCase().contains("flat")) {

			ownerName += " & " + possibleName;
		}
		
		ownerName = ownerName.replaceAll("&\\s*C/O\\b", "\n");
		
		//Routt
		ownerName = ownerName.replaceAll("[\\(\\)]+", "").replaceAll("\\bJTD?\\b", "").replaceAll("\\bATTY\\.?\\b", "")
								.replaceAll("\\bM\\.?D\\.?\\b", "").replaceAll("(?is)\\bC/O\\b", "");
		//
		ownerName = ownerName.replaceAll("\\b[0-9]*%", "");
		ownerName = ownerName.replaceAll("\\b[0-9]+/[0-9]+\\b", "");
		ownerName = ownerName.replaceAll("&\\s*&", "&");
		ownerName = ownerName.replaceAll("(?is)\\s&\\s*([A-Z]+\\s+[A-Z]\\.)\\s*$", " and $1");
		ownerName = ownerName.replaceAll("(?is)\\b(TRUSTEES?)\\s*&\\s*", "$1 and ");
		ownerName = ownerName.replaceAll("(?is)\\bDATED\\s*$", "");
		ownerName = ownerName.replaceAll("(?is)\\bUND.*?INT\\b", "");
		ownerName = ownerName.replaceAll("(?is)\\bCITY\\sATTORNEY\\b", "").trim();
		
		List<List/*<String>*/> namesList = new ArrayList<List/*<String>*/>();
		
		if (NameUtils.isCompany(ownerName, new Vector<String>(), true)
				|| ownerName.matches(".*\\bCONDO\\b.*")) { // BUTERA DUPLEX CONDO 
			
			String[] names = { "", "", "", "", "", "" };
			String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
			if (ownerName.trim().matches("(?is)\\A[A-Z]+,?\\s+[A-Z]+(\\s+[A-Z\\.]+)?\\s*&.*")){
				names = StringFormats.parseNameNashville(ownerName, true);
				
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), namesList);
			} else if (ownerName.trim().matches("(?is).*,?\\s+&\\s+OF\\s+THE.*") || ownerName.trim().matches("(?is).*\\bTRUSTEES?\\s+OF\\s*&.*")){//267800002 Routt
				ownerName = ownerName.replaceAll(",\\s*&\\s*OF\\s+THE", " and ");
				ownerName = ownerName.replaceAll("(?is)\\b(TRUSTEES?)\\s+OF\\s*&", "$1 and ").replaceAll("(?is)\\s*,\\s*", " ");
				names = StringFormats.parseNameDesotoRO(ownerName, true);
				
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), namesList);
			} else if (ownerName.trim().matches(".*?\n.*")) {
				int index = ownerName.indexOf("\n");
				String owner1 = ownerName.substring(0,index).trim();
				String owner2 = ownerName.substring(index+1).trim();
				
				names = StringFormats.parseNameNashville(owner1, true);
				
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]) , NameUtils.isCompany(names[5]), namesList);
				
				names = StringFormats.parseNameNashville(owner2, true);
				
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]) , NameUtils.isCompany(names[5]), namesList);
				
				
			} else {
				names = StringFormats.parseNameNashville(ownerName, true);
				
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]) , NameUtils.isCompany(names[5]), namesList);
			}
			
			GenericFunctions.storeOwnerInPartyNames(resultMap, namesList, true);
		} else { 
			ownerName = ownerName.replaceAll("&", "\n");
			COClearCreekTR.parseNames(resultMap, ownerName, namesList, false, searchId);
		}
		
	}
}

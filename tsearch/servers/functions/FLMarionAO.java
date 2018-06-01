package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.reports.comparators.IntegerComparator;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLMarionAO {

	public static final String[] CITIES = {"BELLEVIEW", "DUNNELLON", "MCINTOSH", "OCALA", "REDDICK",			//incorporated
		                                   "SUMMERFIELD", "FORT MCCOY", "MARION OAKS", "SILVER SPRINGS SHORES",	//unincorporated
		                                   "SALT SPRINGS", "ANTHONY", "THE VILLAGES", "ORANGE SPRINGS",
		                                   "MICANOPY", "VILLAGES OF MARION", "SILVER SPRINGS"};					//other	

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
				
		String rowString = row.toHtml();
		
		Matcher ma1 = Pattern.compile("(?is)<a.*?>(.*?)</a>").matcher(rowString);
		if (ma1.find())
		{
			String parcelID = ma1.group(1);
			Matcher ma2 = Pattern.compile("[12]\\s([\\d-]+)").matcher(rowString);
			if (ma2.find())
			{
				parcelID = ma2.group(1);
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			}
		}
		
		Matcher ma3 = Pattern.compile("(?is)<font color=#000000>(.*)<a").matcher(rowString);
		if (ma3.find())
		{
			String nameAndAddress = ma3.group(1);
			nameAndAddress = nameAndAddress.trim();
			if (nameAndAddress.charAt(0)=='I' || nameAndAddress.charAt(0)=='H') nameAndAddress = nameAndAddress.substring(1);
			int i;
			for (i = 0; i<nameAndAddress.length(); i++) 
				if (Character.isDigit(nameAndAddress.charAt(i))) break;
			
			String name = nameAndAddress.substring(0, i).trim();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
			parseNames(resultMap, searchId);
			
			String address = nameAndAddress.substring(i).trim();
			if (address.contains("ET AL"))					//e.g. "1  ET AL-MARTY SMITH & ANN CRA"
				address = "";
			else if (address.contains("&"))					//e.g. "1  T-TONY & S-SHARON"
				address = "";
			else
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			parseAddress(resultMap);
		}
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner))
			   return;
		
		owner = owner.replaceAll("\\bET\\s+AL\\b", "ETAL");
		owner = owner.replaceAll("\\bTRUSTEE\\b", "TR");
		owner = owner.replaceAll("\\bSUC\\b", "");
		owner = owner.replaceAll("\\bRVBL$", "RVBL TRUST");
		owner = owner.replaceAll("\\bREV\\sTRU$", "REV TRUST");
		owner = owner.replaceAll("\\bDCLR(TN\\sOF\\sTRSUT)?$", "DCLRTN OF TRUST");
		owner = owner.replaceAll("\\bREVOCAB$", "REVOCABLE TRUST");
		owner = owner.replaceAll("C/O", "");
		
		String[] ownerRows = owner.split("\r");
		StringBuffer stringOwnerBuff = new StringBuffer();
		for (String row : ownerRows){
			if (row.trim().matches("\\d+\\s+.*")){
				break;
			} else if (LastNameUtils.isNoNameOwner(row)) {
				break;
			} else {
				stringOwnerBuff.append(row + "\n");
			}
		}
		String stringOwner = stringOwnerBuff.toString();
		stringOwner = stringOwner.replaceAll("\n$", "");
		String[] nameLines = stringOwner.split("\n");

		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		StringBuffer nameOnServerBuff = new StringBuffer();
		for (int i=0; i < nameLines.length; i++){
			String ow = nameLines[i];
			names = StringFormats.parseNameNashville(ow, true);
			if (!NameUtils.isCompany(ow)) suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			nameOnServerBuff.append("/").append(ow);
		}
		String nameOnServer = nameOnServerBuff.toString();
		nameOnServer = nameOnServer.replaceFirst("/", "");
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer);
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void parseAddress(ResultMap resultMap) {
		
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(address))
			return;
		
		parseAddress(resultMap, address);
	}
	
	public static void parseAddress(ResultMap resultMap, String origAddress)  {
		
		String addressAndCity[] = StringFormats.parseCityFromAddress(origAddress, CITIES);
		if (StringUtils.isNotEmpty(addressAndCity[0])) {
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), addressAndCity[0]);
		}
		origAddress = addressAndCity[1];
		
		origAddress = origAddress.replaceAll("(?is)CORNER OF", "");			//e.g. "1  CORNER OF 329 & 135TH"
		String[] address = StringFormats.parseAddress(origAddress);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), address[1]);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), address[0]);
	}
	
	public static void parseLegalSummary(ResultMap resultMap)
	{
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		legalDescription += " ";	//we need this to be sure all regular expressions match 
		
		List<String> section = RegExUtils.getMatches("\\bSEC\\s(.+?)\\s", legalDescription, 1);
		for (int i=0;i<section.size();i++) if (section.get(i).contains("-")) section.remove(i);  
		if (section.size()>0)
		{
			Collections.sort(section);
			int k = 1;
			for (int i = 1; i < section.size(); i++)		//remove duplicates
			{
				if (! equalsIgnoreLeadingZeroes(section.get(i), section.get(i-1)))
					section.set(k++,section.get(i));
			}
			String subdivisionSection = "";
			for (int i=0; i<k; i++) subdivisionSection += " " + StringUtils.removeLeadingZeroes(section.get(i));
			subdivisionSection = subdivisionSection.trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), subdivisionSection);
		}
		
		Matcher matcherTownship = Pattern.compile("\\bTWP\\s(\\d+)").matcher(legalDescription);
		if (matcherTownship.find())
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), matcherTownship.group(1));
		
		Matcher matcherRange = Pattern.compile("\\bRGE\\s(\\d+)").matcher(legalDescription);
		if (matcherRange.find())
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), matcherRange.group(1));
				
		Matcher matcherTract = Pattern.compile("\\bTRACT\\s(\\d+)").matcher(legalDescription);
		if (matcherTract.find())
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), matcherTract.group(1));
				
		Matcher matcherBook = Pattern.compile("\\bPLAT\\sBOOK\\s(.+?)\\s").matcher(legalDescription);
		if (matcherBook.find() && !matcherBook.group(1).matches("UNR(EC)?"))
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), StringUtils.removeLeadingZeroes(matcherBook.group(1)));
		
		Matcher matcherPage = Pattern.compile("\\bPAGE\\s(\\d+)").matcher(legalDescription);
		if (matcherPage.find())
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), StringUtils.removeLeadingZeroes(matcherPage.group(1)));
		
		String blkPatt = "\\bBL?K\\s(.+?)\\s";
		Matcher matcherBlk = Pattern.compile(blkPatt).matcher(legalDescription);
		if (matcherBlk.find()) {
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			String block = matcherBlk.group(1);
			block = Roman.normalizeRomanNumbersExceptTokens(block, exceptionTokens);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		
		String allLots = "";
		List<String> lot = RegExUtils.getMatches("\\bLOT\\s(.+?)\\s", legalDescription, 1);
		if (lot.size()>0)
		{
			Collections.sort(lot);
			int k = 1;
			for (int i = 1; i < lot.size(); i++)		//remove duplicates
			{
				if (! lot.get(i).equals(lot.get(i-1)))
					lot.set(k++,lot.get(i));
			}
			for (int i=0; i<k; i++) allLots += " " + lot.get(i);
			allLots = allLots.trim();
		}
		
		List<String> lots = RegExUtils.getMatches("\\bLOTS\\s(.+?)\\s", legalDescription, 1);
		if (lots.size()>0)
		{
			List<String> separatedLots = new ArrayList<String>();
			for (int i=0; i<lots.size(); i++)
			{
				String[] afterSplit = lots.get(i).split("\\.");
				separatedLots.addAll(Arrays.asList(afterSplit));
			}
			Collections.sort(separatedLots);
			int k = 1;
			for (int i = 1; i < separatedLots.size(); i++)		//remove duplicates
			{
				if (! separatedLots.get(i).equals(separatedLots.get(i-1)))
					separatedLots.set(k++,separatedLots.get(i));
			}
			for (int i=0; i<k; i++) allLots += " " + separatedLots.get(i);
			allLots = allLots.trim();
		}
		
		String[] strings = allLots.toString().split("\\s+");
		Arrays.sort(strings, new IntegerComparator());
		String lotValues = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("", strings).trim();
		
		if (lotValues.length() != 0)
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lotValues);
		
		Matcher matcherUnit = Pattern.compile("\\bUNIT\\s(\\d+)").matcher(legalDescription);
		if (matcherUnit.find())
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), matcherUnit.group(1));
		
		String subdivisionName = "";
		String lines[] = legalDescription.split("\r\n");
		if (lines.length >= 2)
		{
			String line = lines[1];
			if (isSubdivisionName(line))
				subdivisionName = extractSubdivisionName(line);
		}
		if (subdivisionName.length()==0 && lines.length >= 3)
		{
			String line = lines[2];
			if (isSubdivisionName(line))
				subdivisionName = extractSubdivisionName(line);
		}
		if (subdivisionName.length()==0 && lines.length >= 4)
		{
			String line = lines[3];
			if (isSubdivisionName(line))
				subdivisionName = extractSubdivisionName(line);
		}
		if (subdivisionName.length()!=0) {
			subdivisionName = subdivisionName.replaceAll(blkPatt, "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
		}
			
	}
	
	public static boolean equalsIgnoreLeadingZeroes(String string1, String string2)
	{
		while (string1.startsWith("0")) string1 = string1.substring(1);
		while (string2.startsWith("0")) string2 = string2.substring(1);
		return string1.equals(string1);
	}
	
	public static boolean isSubdivisionName(String string)
	{
		if (string.matches("(?is)^PLAT\\sBOOK\\s(.*)")) return false;
		if (string.matches("(?is)(.*)\\bOF\\s(N|S|E|W|NE|NW|SE|SW)\\s(.*)")) return false;
		if (string.matches("(?is)(.*)\\bLOT\\s(\\d+)(.*)")) return false;
		if (string.matches("(?is)(.*)((\\b[0-9]+)?\\.)?[0-9]+\\sFT\\b(.*)")) return false;
		if (string.matches("(?is)(.*)\\bHEIRS\\sOF\\b(.*)")) return false;
		if (string.matches("(?is)(.*)\\bTRACT\\s(\\d+)(.*)")) return false;
		if (string.matches("(?is)(.*)\\bMORE\\sFULLY\\b(.*)")) return false;
		if (string.matches("(?is)(.*)\\bROAD\\sPURPOSES\\b(.*)")) return false;
		if (string.matches("(?is)(.*)\\bSEC\\s(\\d+)(.*)")) return false;
		if (string.matches("(?is)(.*)\\bDESC(RIBED)?\\sAS\\b(.*)")) return false;
		if (string.matches("(?is)(.*)\\bEXC\\b(.*)")) return false;
		
		if (string.matches("(.*)\\bLOTS\\s([\\d\\.]+)(.*)")) return true;
		return true;
	}
	
	public static String extractSubdivisionName(String line)
	{
		if (line.contains("UNREC SUB"))
			return line.substring(0, line.indexOf("UNREC SUB")).trim();
		else if (line.contains("UNREC"))
			return line.substring(0, line.indexOf("UNREC")).trim();
		else if (line.contains("UNRE"))
			return line.substring(0, line.indexOf("UNRE")).trim();
		else if (line.contains("SUB"))
			return line.substring(0, line.indexOf("SUB")).trim();
		else if (line.contains("UNIT"))
			return line.substring(0, line.indexOf("UNIT")).trim();
		
		else if (line.contains("LOTS"))
		{
			Matcher matcher = Pattern.compile("\\bLOTS\\s([\\d\\.]+)\\s(.*)").matcher(line);
			if (matcher.find())
				return matcher.group(2).trim();
			else return "";
		}
		return line;
	}
	
}

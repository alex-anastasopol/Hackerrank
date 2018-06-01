package ro.cst.tsearch.servers.functions;

/**
*
* implements the following counties from NV: Clark (including City of North Las Vegas, City of Las Vegas and City of Henderson), 
* Douglas and Washoe (including City of Reno)
*
*/

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class NVGenericCornerAM {
		
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AM");
				
		TableColumn[] cols = row.getColumns();
		
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), cols[0].toPlainTextString().trim());
		String streetNo = cols[1].toPlainTextString().replaceAll("\\b0+(\\d+)\\b", "$1").trim();	//remove leading zeroes
		if (streetNo.equals("0")) streetNo = "";
		if (streetNo.length()!=0) resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		String streetName = cols[2].toPlainTextString().replaceAll("&nbsp;", "").trim();
		if (streetName.length()!=0) resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		
		String district = cols[4].toPlainTextString().replaceAll("\\s", "");
		String parcelID = cols[3].toPlainTextString().trim();
		if (district.length()!=0) parcelID += "_" + district;
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
		
		parseNames(resultMap, searchId);
				
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner))
			   return;
		
		owner = owner.replaceAll("(?is)\\b(\\d{4}\\s)TR\\b", "$1TRUST");
		owner = owner.replaceAll("(?is)\\bTRET\\b", "TR");
		owner = owner.replaceAll("(?is)\\bTR\\sAGMT\\b", "TRUST AGMT");
		owner = owner.replaceAll("(?is)\\bLIV\\sTR\\b", "LIV TRUST");
		owner = owner.replaceAll("(?is)\\bTR\\s\\w{4}", "TR");
		if (!NameUtils.isCompany(owner) && owner.contains("&")) owner = owner.replaceAll("(?is)\\bTR\\b", "TRUSTEE");
				
		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes = { "", "" }, type = { "", "" }, otherType;
						
		names = StringFormats.parseNameNashville(owner, true);
		if (!NameUtils.isCompany(owner)) suffixes = GenericFunctions.extractNameSuffixes(names);
		if (!NameUtils.isCompany(owner)) type = GenericFunctions.extractAllNamesType(names);
		otherType = GenericFunctions.extractAllNamesOtherType(names);
		GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
			NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		
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
		
		origAddress = origAddress.replaceAll(",", "");
		String streetNo = StringFormats.StreetNo(origAddress);
		String streetName = StringFormats.StreetName(origAddress);
		if (streetNo.equals("0")) streetNo = "";
		if (streetName.equals("0")) streetName = "";
		//for an address like "000000 , ,"
		if (streetNo.length()==0 && streetName.length()==0) resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), "");
		else 
		{
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
			resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		}
	}
		
	public static void parseLegalSummary(ResultMap resultMap)
	{
		
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		legalDescription += " ";	//we need this to be sure all regular expressions match 
		
		String lotexpr = "(?is)LOT\\s(\\w+)";
		String subdivisionLot = "";
		Matcher matcher = Pattern.compile(lotexpr).matcher(legalDescription);
		if (matcher.find()) subdivisionLot = matcher.group(1).trim();
		else 
		{
			lotexpr = "(?is)LOTS((\\s*[^\\s]+\\s*&)+\\s*[^\\s])";
			matcher = Pattern.compile(lotexpr).matcher(legalDescription);
			if (matcher.find()) subdivisionLot = matcher.group(1).replaceAll("&", "").replaceAll("\\s{2,}", " ").trim();
		}
		if (subdivisionLot.length() != 0) 
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), subdivisionLot);
		
		String subdivisionexpr = "(?i)(.+?)[-\\s]+(?:U(?:NI)?T|PH(?:ASE)?|PLAT|VG|TRACT|PARCEL|TRACT|SUB|FILE|(?:\\s\\d+)|\\w-\\w-\\w)";
		String subdivisionName = "";
		matcher = Pattern.compile(subdivisionexpr).matcher(legalDescription);
		if (matcher.find()) subdivisionName = matcher.group(1);
		subdivisionName = subdivisionName.replaceFirst("(.*?)\\w+-\\w+-\\w", "$1").trim();
		subdivisionName = subdivisionName.replaceFirst("(.*?)\\d+\\z", "$1").trim();
		if (subdivisionName.toLowerCase().indexOf("pod")!=0) subdivisionName = subdivisionName.replaceFirst("(?i)(.*?)\\bPOD\\b(.*)", "$1").trim();
		if (subdivisionName.length()<3) subdivisionName = "";
		if (subdivisionName.matches("(?i)PARCEL\\s\\w+")) subdivisionName = "";
		//if the possible subdivision name contains "half" of "pod" find the next possible subdivision name
		if (subdivisionName.toLowerCase().contains("half") || subdivisionName.toLowerCase().indexOf("pod")>0)
		{
			if  (matcher.find(matcher.end())) subdivisionName = matcher.group(1);
			subdivisionName = subdivisionName.replaceFirst("(?:.*?)\\bAT\\b(.*)", "$1").trim();
			subdivisionName = subdivisionName.replaceFirst("\\A\\w{1,5}-(.*)", "$1").trim();
		}
		int atIndex = subdivisionName.toLowerCase().indexOf(" at ");
		if (atIndex >-1 && (subdivisionName.toLowerCase().indexOf("parcel")>-1 || subdivisionName.toLowerCase().indexOf("pod ")>-1))
		{
			subdivisionName = subdivisionName.substring(atIndex+" at ".length(), subdivisionName.length());
		}
		if (subdivisionName.length() != 0) 
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
				
		String subdnoexpr = "(?is)SUB\\s(\\w+)";
		String subdivisionNo = "";
		matcher = Pattern.compile(subdnoexpr).matcher(legalDescription);
		if (matcher.find()) subdivisionNo = matcher.group(1).trim();
		if (subdivisionNo.length() != 0) 
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NO.getKeyName(), subdivisionNo);
		
		String blockexpr = "(?is)BLOCK\\s(\\w+)";
		String subdivisionBlock = "";
		matcher = Pattern.compile(blockexpr).matcher(legalDescription);
		if (matcher.find()) subdivisionBlock = matcher.group(1).trim();
		if (subdivisionBlock.length() != 0) 
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), subdivisionBlock);
		
		String unitexpr = "(?is)U(?:NI)?T\\s#?([^\\s]+)";
		String subdivisionUnit = "";
		matcher = Pattern.compile(unitexpr).matcher(legalDescription);
		if (matcher.find()) subdivisionUnit = GenericFunctions1.replaceOnlyNumbers(matcher.group(1).trim());	//e.g. TWO -> 2
		if (subdivisionUnit.length() != 0) 
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), subdivisionUnit);
		
		String bldgexpr = "(?is)BLDG\\s(\\w+)";
		String subdivisionBldg = "";
		matcher = Pattern.compile(bldgexpr).matcher(legalDescription);
		if (matcher.find()) subdivisionBldg = matcher.group(1).trim();
		if (subdivisionBldg.length() != 0) 
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), subdivisionBldg);
				
		String phaseexpr = "(?is)PH(?:ASE)?\\s(\\w+)";
		String subdivisionPhase = "";
		matcher = Pattern.compile(phaseexpr).matcher(legalDescription);
		if (matcher.find()) subdivisionPhase = matcher.group(1).trim();
		if (subdivisionPhase.length() != 0) 
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), subdivisionPhase);
		
		String tractexpr = "(?is)TRACT\\s(\\w+)";
		String subdivisionTract = "";
		matcher = Pattern.compile(tractexpr).matcher(legalDescription);
		if (matcher.find()) subdivisionTract = matcher.group(1).trim();
		if (subdivisionTract.length() != 0) 
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), subdivisionTract);
		
		String booknoexpr = "(?is)PLAT\\sBOOK\\s(\\d+)\\sPAGE\\s(\\d+)";
		String platBook = "";
		String platNo = "";
		matcher = Pattern.compile(booknoexpr).matcher(legalDescription);
		if (matcher.find()) 
		{
			platBook = matcher.group(1).trim();
			platNo = matcher.group(2).trim();
		}
		if (platBook.length() != 0 && platNo.length() != 0)
		{
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), platBook);
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), platNo);
		}	
	}
}

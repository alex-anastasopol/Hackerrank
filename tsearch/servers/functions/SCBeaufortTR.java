package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class SCBeaufortTR {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
				
		TableColumn[] cols = row.getColumns();
		String parcelID;
		String address;
		String name;
		String alternateID;
		String legal;
		
		if (row.getColumnCount() == 3)  //search by name, search by address
		{
			parcelID = cols[0].toPlainTextString().replaceAll("[(&nbsp;)\\s]", ""); 
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			address = cols[1].toPlainTextString().replaceAll("&nbsp;", "").trim();
			if (address.length() != 0 )
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			name = cols[2].toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
			parseNames(resultMap, searchId);
			parseAddress(resultMap);
		}
		
		else if (row.getColumnCount() == 4 && cols[1].toHtml().contains("listlink") )  //search by alternate ID
		{
			parcelID = cols[0].toPlainTextString().replaceAll("[(&nbsp;)\\s]", ""); 
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			alternateID = cols[1].toPlainTextString().trim(); 
			resultMap.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), alternateID);
			address = cols[2].toPlainTextString().replaceAll("&nbsp;", "").trim();
			if (address.length() != 0 )
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			name = cols[3].toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
			parseNames(resultMap, searchId);
			parseAddress(resultMap);
		}
		
		else if (row.getColumnCount() == 4 && cols[1].toHtml().contains("listdata") )  //search by legal description
		{
			parcelID = cols[0].toPlainTextString().replaceAll("[(&nbsp;)\\s]", ""); 
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			legal = cols[1].toPlainTextString().replaceAll("&nbsp;", "").trim(); 
			resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
			address = cols[2].toPlainTextString().replaceAll("&nbsp;", "").trim();
			if (address.length() != 0 )
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			name = cols[3].toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
			parseNames(resultMap, searchId);
			parseAddress(resultMap);
			parseLegalSummary(resultMap);
		}

		else if (row.getColumnCount() == 13 )  //search by sales
		{
			parcelID = cols[0].toPlainTextString().trim(); 
			if (parcelID.matches("\\w\\w\\w\\w\\s\\w\\w\\w\\s\\w\\w\\w\\s\\w\\w\\w\\w\\s\\w\\w\\w\\w"))	//if is a valid parcel ID
			{																							//(sometimes a row contains an address in Parcel ID column 
				parcelID = parcelID.replaceAll("[(&nbsp;)\\s]", "");									//and the other columns don't contain anything)
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
				name = cols[1].toPlainTextString().trim();
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
				parseNames(resultMap, searchId);
			}
		}
		
		return resultMap;
	}
	
	//returns true if the two parameters are first names of opposite sex and false otherwise
	public static boolean namesOppositeSex(String name1, String name2)
	{
		return (FirstNameUtils.isMaleName(name1) && FirstNameUtils.isFemaleName(name2)) ||
		       (FirstNameUtils.isFemaleName(name1) && FirstNameUtils.isMaleName(name2));
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner))
			   return;
		
		owner = owner.replaceAll("%", "&");
		owner = owner.replaceAll("#", "");
		owner = owner.replaceAll("(?is)\\bTRUS\\b", "TRUST");
		owner = owner.replaceAll("(?is)\\b(TRUSTEES?)\\b", "TR");
		owner = owner.replaceAll("(?is)\\bCO-TRUST?\\b", "TR");
		owner = owner.replaceAll("(?is)\\bJTR?O?S?\\b", "");
		owner = owner.replaceAll("(?is)\\bETA\\b", "");
		owner = owner.replaceAll("(?is)\\s{2,}", " ");
		if (NameUtils.isCompany(owner)) owner = owner.replaceAll("&", "_____");
		owner = owner.replaceAll("F/K/A", "A/K/A");
		
		String newowner = owner;
		
		if (owner.indexOf("&")==-1)		//split names if not already split with &
		{
			newowner = "";
			int splitPosition = -1;
			String parts[] = owner.split("\\s");
			int len = parts.length;
			
			//split names (e.g. WELSH JOHN M GENEVA C => WELSH, JOHN M  & GENEVA C)
			if (NameUtils.isNotCompany(owner) && !owner.contains("C/O") && !owner.contains("A/K/A"))
			{
				//if the name ends with TR
				if (parts[len-1].equalsIgnoreCase("TR")) len--;
				//M G WAY => WAY M G
				if (len>=2 && LastNameUtils.isLastName(parts[len-1]) && !LastNameUtils.isLastName(parts[len-2]))		
				{
					String tmp = parts[len-1];
					for (int i=len-1;i>0;i--) parts[i]=parts[i-1];
					parts[0] = tmp;
	  			}
				//& after TR
				if (len>=3 && parts[2].equalsIgnoreCase("tr")) splitPosition = 3;
				//& after TR
				else if (len>=4 && parts[3].equalsIgnoreCase("tr")) splitPosition = 4;
				//& after a suffix
				else if (len>=4 && parts[3].matches(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameSuffixString)) splitPosition = 4;
				//& after a suffix
				else if (len>=3 && parts[2].matches(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameSuffixString)) splitPosition = 3;
				//& before a last name
				else if (len>=6 && LastNameUtils.isLastName(parts[4]) && !LastNameUtils.isLastName(parts[3])) splitPosition = 4;
				//& before a last name
				else if (len>=5 && LastNameUtils.isLastName(parts[3]) && !LastNameUtils.isLastName(parts[2])) splitPosition = 3;
				//& before a last name
				else if (len>=4 && LastNameUtils.isLastName(parts[2])) splitPosition = 2;
				else if (len>=3)
				{
					//& after an initial
					if (parts[2].length() == 1 && len > 3) splitPosition = 3;
					//& before a last name
					else if (LastNameUtils.isLastName(parts[2])) splitPosition = 2;
					//& between two surnames of opposite sex
					else if (namesOppositeSex(parts[2], parts[1]))
					{
						splitPosition = 2;
						if (len>=5 && FirstNameUtils.isFirstName(parts[3]) && FirstNameUtils.isFirstName(parts[4])) splitPosition = 3;
					} 	
				}
				//& between first names of opposite sex
				else if (len>=4 && namesOppositeSex(parts[3], parts[2]))
				{
					splitPosition = 3;
				}
								
				newowner += parts[0] + ",";
				for (int i=1;i<parts.length;i++) 
					if (i==splitPosition)
					{
						//, after a last name
						if (!FirstNameUtils.isFirstName(parts[i]) && LastNameUtils.isLastName(parts[i])) newowner += " & " + parts[i] + ",";
						else newowner += " & " + parts[i];
					}
					else newowner += " " + parts[i];
				
				//if the second name contains TR there is a third name after TR
				parts = newowner.split("\\s");
				newowner = "";
				int index = 2;
				int i;
				for (i=index;i<parts.length-1;i++) if (parts[i].equals("&")) break;
				index = i+1;
				for (i=index;i<parts.length-1;i++) if (parts[i].equalsIgnoreCase("tr")) break;
				index = i+1;
				for (i=0;i<parts.length;i++) 
					if (i==index) newowner += " & " + parts[i];
					else newowner += " " + parts[i];
			}
			
			else 
			{	//for a company after TRUST may be a person
				boolean done = false;
				for (int i=1;i<parts.length-3;i++)
					if (parts[i].equalsIgnoreCase("TRUST")) 
					{
						splitPosition = i+1;
						done = true;
						break;
					}
				
				if (done)
				{
					for (int i=0;i<parts.length;i++) 
						if (i==splitPosition) newowner += " & " + parts[i];
						else newowner += " " + parts[i];
				}
				else
				{	//after a person with TR may be a company
					for (int i=1;i<parts.length-1;i++)
						if (parts[i].equalsIgnoreCase("TR"))
						{
							splitPosition = i+1;
							break;
						}
					for (int i=0;i<parts.length;i++) 
						if (i==splitPosition) newowner += " & " + parts[i];
						else newowner += " " + parts[i];
				}
			}
			
			newowner = newowner.replaceAll("(?s)\\&\\s*\\&", "&");
			newowner = newowner.trim();
		}
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		
		boolean done = false;
		String diferrentNames[] = newowner.split("\\&");
		if (diferrentNames.length==2)
		{
			diferrentNames[0] = diferrentNames[0].replaceAll("_____", "&");
			diferrentNames[1] = diferrentNames[1].replaceAll("_____", "&");
			
			int numberOfCompanies = 0;
			if (NameUtils.isCompany(diferrentNames[0])) numberOfCompanies++;
			if (NameUtils.isCompany(diferrentNames[1])) numberOfCompanies++;
			if (numberOfCompanies==1) //one company and one person
			{
				names = StringFormats.parseNameNashville(diferrentNames[0], true);
				if (!NameUtils.isCompany(newowner)) suffixes = GenericFunctions.extractNameSuffixes(names);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				
				names = StringFormats.parseNameNashville(diferrentNames[1], true);
				if (!NameUtils.isCompany(newowner)) suffixes = GenericFunctions.extractNameSuffixes(names);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				
				done = true;
			}
		}
		
		diferrentNames = newowner.split("C/O");
		if (diferrentNames.length==2)
		{
			diferrentNames[0] = diferrentNames[0].replaceAll("_____", "&");
			diferrentNames[1] = diferrentNames[1].replaceAll("_____", "&");
			
			names = StringFormats.parseNameNashville(diferrentNames[0], true);
			if (!NameUtils.isCompany(newowner)) suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
			NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			
			names = StringFormats.parseNameDesotoRO(diferrentNames[1], true);
			if (!NameUtils.isCompany(newowner)) suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
			NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				
			done = true;
		}
				
		diferrentNames = newowner.split("A/K/A");
		if (diferrentNames.length==2)
		{
			names = StringFormats.parseNameNashville(diferrentNames[0], true);
			if (!NameUtils.isCompany(newowner)) suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
			NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			
			if (!LastNameUtils.isNoNameOwner(diferrentNames[1]))
			{
				names = StringFormats.parseNameDesotoRO(diferrentNames[1], true);
				if (!NameUtils.isCompany(newowner)) suffixes = GenericFunctions.extractNameSuffixes(names);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
				NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
				
			done = true;
		}
		
		String names1 = newowner;
		String names2 = "";
		if (!done)
		{
			String possibleNames[] = newowner.split("\\&");
			if (possibleNames.length>2)							//more than two names
			{
				int ampersandIndex = newowner.indexOf("&");
				ampersandIndex = newowner.indexOf("&", ampersandIndex+1);
				names1 = newowner.substring(0, ampersandIndex);		//first set of names
				names2 = newowner.substring(ampersandIndex+1);		//second set of names
			}
			
			newowner = names1.replaceAll("_____", "&");
			names = StringFormats.parseNameNashville(newowner, true);
			if (!NameUtils.isCompany(newowner)) suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
				NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			
			if (names2.length()!=0)
			{
				String separatedNames[] = names2.split("\\s");
				String lastName = "";
				if (!LastNameUtils.isLastName(separatedNames[0]))		//if the second set of names doesn't have a last name
				{
					lastName = names[5];
					if (lastName.length()==0) lastName = names[2];
				}
				names2 = lastName + names2;
				newowner = names2.replaceAll("_____", "&");
				names = StringFormats.parseNameNashville(newowner, true);
				if (!NameUtils.isCompany(newowner)) suffixes = GenericFunctions.extractNameSuffixes(names);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
		}
		
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void parseAddress(ResultMap resultMap) {
		
		String address = (String) resultMap.get("PropertyIdentificationSet.AddressOnServer");
		if (address==null) return;
		if (address.endsWith(",")) address = address.substring(0, address.length()-1);
		if (StringUtils.isEmpty(address))
			return;
		
		parseAddress(resultMap, address);
	}
	
	public static void parseAddress(ResultMap resultMap, String origAddress)  {
		
		Matcher matcher = Pattern.compile("(?i)\\bLOT\\s(NO\\s|)([\\w-]{1,4})(\\s|,)").matcher(origAddress);
		if (matcher.find())							//e.g. 20 ISLAND TANK RD LOT 34, BEAUFORT 
		{
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), matcher.group(2));
			origAddress = origAddress.replaceAll(("(?i)(.*)\\bLOT\\s(NO\\s|)([\\w-]{1,4})(\\\\s|,)(.*)"), "$1 $4 $5");
		}
		
		String streetNumberAndName = origAddress;
		String city = "";
		int commaPosition = origAddress.indexOf(",");
		if (commaPosition != -1)						//split in address and city (e.g. 100 MAIN ST, BEAUFORT)
		{
			streetNumberAndName = origAddress.substring(0, commaPosition).trim();
			city = origAddress.substring(commaPosition+1).trim();
		}
		city = city.replaceAll("(?i)City\\sof\\s(.+)", "$1");
		city = city.replaceAll("(?i)Town\\sof\\s(.+)", "$1");
		
		if (city.length() != 0)
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(streetNumberAndName));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(streetNumberAndName));
		
	}
		
	public static void parseLegalSummary(ResultMap resultMap)
	{
		
	String legalDescription = (String) resultMap.get("PropertyIdentificationSet.LegalDescriptionOnServer");
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		if (legalDescription.matches("\\A\\d.+"))
			return;
		
		legalDescription += " *";	//we need this to be sure all regular expressions match 
		
		String lotexpr1 = "(?i)\\bLOT\\s(NO\\s|)([\\w-]{1,4})\\s";
		String lotexpr2 = "(?i)\\b(LTS?|LOTS)\\s((\\d[\\w-]{0,3}\\s?(&|/)?\\s?)+)";
		List<String> lot = RegExUtils.getMatches(lotexpr1, legalDescription, 2);
		String subdivisionLot = "";
		for (int i=0; i<lot.size(); i++) subdivisionLot += " " + lot.get(i);
		lot = RegExUtils.getMatches(lotexpr2, legalDescription, 2);
		for (int i=0; i<lot.size(); i++) subdivisionLot += " " + lot.get(i).replaceAll("[&/]", " ").trim();
		subdivisionLot = subdivisionLot.trim();
		if (subdivisionLot.length() != 0) {
			subdivisionLot = LegalDescription.cleanValues(subdivisionLot, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), subdivisionLot);
		}
		
		String subdivisionexpr = "(BL(OC)?K\\s(\\w{1,3})\\s|(PB|B(OO)?K)\\s?(\\d+\\s)PA?G?E?\\s?(\\d+)\\s)?(.*?)(BLO?C?K|SEC|TRACT|PLAT?|POR\\s|BLDG|SPLIT|S/D|#|\\*)";
		String subdivisionName = "";
		Matcher matcher = Pattern.compile("(?i)((\\d[NSEW]\\d[NSEW\\s]\\s)|(TWNHS\\s\\w\\sBLDG\\s[^\\s]+\\s)|(UNIT\\s[^\\s]+\\s)|(PH(ASE)?\\s([\\w-]+)))" + subdivisionexpr).matcher(legalDescription);
		while (matcher.find())
		{	
			subdivisionName = matcher.group(15).replaceFirst("(?i)\\s*\\bAKA\\b.*", "").replaceAll("\\(.*?\\)", "").trim()
				.replaceFirst("(?i)(.*)\\sBLOCK", "$1").replaceFirst("(?i)(.*?)\\s(PB|B(OO)?K)(\\s|\\d).*", "$1")
				.replaceFirst("(?i)\\bLOT\\s(NO\\s|)([\\w-]{1,4})\\s", "").replaceFirst("(?i)(.*?)\\s\\d(.*)", "$1");
			if (subdivisionName.toLowerCase().contains("changed by")) subdivisionName = "";
			if (subdivisionName.length()!=0) break;
		}	
		if (subdivisionName.length()!=0)
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
		else 
			{
				matcher = Pattern.compile(lotexpr1 + subdivisionexpr).matcher(legalDescription);
				while (matcher.find())
				{
					subdivisionName = matcher.group(10).replaceFirst("(?i)\\s*\\bAKA\\b.*", "").replaceAll("\\(.*?\\)", "").trim()
						.replaceFirst("(?i)(.*)\\sBLOCK", "$1").replaceFirst("(?i)(.*?)\\s(PB|B(OO)?K)(\\s|\\d).*", "$1")
						.replaceFirst("(?i)\\bLOT\\s(NO\\s|)([\\w-]{1,4})\\s", "").replaceFirst("(?i)(.*?)\\s\\d(.*)", "$1");
					if (subdivisionName.toLowerCase().contains("changed by")) subdivisionName = "";
					if (subdivisionName.length()!=0) break;
				}
				if (subdivisionName.length()!=0)
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
				else 
				{
					matcher = Pattern.compile(lotexpr2 + subdivisionexpr).matcher(legalDescription);
					while (matcher.find())
					{
						subdivisionName = matcher.group(12).replaceFirst("(?i)\\s*\\bAKA\\b.*", "").replaceAll("\\(.*?\\)", "").trim()
							.replaceFirst("(?i)(.*)\\sBLOCK", "$1").replaceFirst("(?i)(.*?)\\s(PB|B(OO)?K)(\\s|\\d).*", "$1")
							.replaceFirst("(?i)\\bLOT\\s(NO\\s|)([\\w-]{1,4})\\s", "").replaceFirst("(?i)(.*?)\\s\\d(.*)", "$1");
						if (subdivisionName.toLowerCase().contains("changed by")) subdivisionName = "";
						if (subdivisionName.length()!=0) break;
					}
					if (subdivisionName.length()!=0)
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivisionName);
				}
		}
		
		
		List<String> blk = RegExUtils.getMatches("(?is)\\bBLO?C?K\\s(\\w{1,3})\\s", legalDescription, 1);
		String subdivisionBlock = "";
		for (int i=0; i<blk.size(); i++) subdivisionBlock += " " + blk.get(i);
		subdivisionBlock = subdivisionBlock.trim();
		if (subdivisionBlock.length() != 0) {
			subdivisionBlock = LegalDescription.cleanValues(subdivisionBlock, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), subdivisionBlock);
		}
		
		List<String> unit = RegExUtils.getMatches("(?is)\\bUNIT\\s([^\\s]+)", legalDescription, 1);
		String subdivisionUnit = "";
		for (int i=0; i<unit.size(); i++) subdivisionUnit += " " + unit.get(i);
		subdivisionUnit = subdivisionUnit.trim();
		if (subdivisionUnit.length() != 0) {
			subdivisionUnit = LegalDescription.cleanValues(subdivisionUnit, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), subdivisionUnit);
		}
		
		List<String> bldg = RegExUtils.getMatches("(?is)\\bBLDG\\s([^\\s]+)", legalDescription, 1);
		String subdivisionBldg = "";
		for (int i=0; i<bldg.size(); i++) subdivisionBldg += " " + bldg.get(i);
		subdivisionBldg = subdivisionBldg.replaceAll("\\*", "").trim();
		if (subdivisionBldg.length() != 0) {
			subdivisionBldg = LegalDescription.cleanValues(subdivisionBldg, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), subdivisionBldg);
		}
		
		List<String> phase = RegExUtils.getMatches("(?is)\\bPH(ASE)?\\s([\\w-]+)", legalDescription, 2);
		String subdivisionPhase = "";
		for (int i=0; i<phase.size(); i++) subdivisionPhase += " " + phase.get(i);
		phase = RegExUtils.getMatches("(?is)\\bPHASES\\s(([\\w-]{1,3}+\\s)+)", legalDescription, 2);
		for (int i=0; i<phase.size(); i++) subdivisionPhase += " " + phase.get(i).trim();
		subdivisionPhase = subdivisionPhase.trim();
		if (subdivisionPhase.length() != 0) {
			subdivisionPhase = LegalDescription.cleanValues(subdivisionPhase, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), subdivisionPhase);
		}
		
		List<String> tract = RegExUtils.getMatches("(?is)\\bTRACT\\s(([\\w-])+)\\s", legalDescription, 1);
		String subdivisionTract = "";
		for (int i=0; i<tract.size(); i++) 
			if (!"plat".equals(tract.get(i).trim().toLowerCase())) subdivisionTract += " " + tract.get(i);
		subdivisionTract = subdivisionTract.trim();
		if (subdivisionTract.length() != 0) {
			subdivisionTract = LegalDescription.cleanValues(subdivisionTract, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), subdivisionTract);
		}
		
		List<String> section = RegExUtils.getMatches("(?is)\\bSEC\\s(\\d+)\\s", legalDescription, 1);
		String subdivisionSection = "";
		for (int i=0; i<section.size(); i++) subdivisionSection += " " + section.get(i);
		subdivisionSection = subdivisionSection.trim();
		if (subdivisionSection.length() != 0) {
			subdivisionSection = LegalDescription.cleanValues(subdivisionSection, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), subdivisionSection);
		}
				
		List<String> townshipRange = RegExUtils.getMatches("(?is)\\bSEC\\s\\d+\\s(\\w{4,4})", legalDescription, 1);
		String subdivisionTownship = "";
		String subdivisionRange = "";
		for (int i=0; i<townshipRange.size(); i++)
		{
			subdivisionTownship += " " + townshipRange.get(i).subSequence(0, 2);
			subdivisionRange += " " + townshipRange.get(i).subSequence(2, 4);	
		} 
		List<String> township = RegExUtils.getMatches("(?is)\\bTWNHS\\s(\\w)\\s", legalDescription, 1);
		for (int i=0; i<township.size(); i++) subdivisionTownship += " " + township.get(i);
		List<String> range = RegExUtils.getMatches("(?is)\\bRNG\\s(\\w+)\\s", legalDescription, 1);
		for (int i=0; i<range.size(); i++) subdivisionRange += " " + range.get(i);
		township = RegExUtils.getMatches("(?is)\\bTP\\s(\\w+)\\s", legalDescription, 1);
		for (int i=0; i<township.size(); i++) subdivisionTownship += " " + township.get(i);
		subdivisionTownship = subdivisionTownship.trim();
		subdivisionRange = subdivisionRange.trim();
		if (subdivisionTownship.length() != 0) {
			subdivisionTownship = LegalDescription.cleanValues(subdivisionTownship, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), subdivisionTownship);
		}
		if (subdivisionRange.length() != 0) {
			subdivisionRange = LegalDescription.cleanValues(subdivisionRange, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), subdivisionRange);
		}
		
		List<String> book1 = RegExUtils.getMatches("(?is)\\b(PB|B(OO)?K)\\s?(\\d+\\s)PA?G?E?\\s?(\\d+)", legalDescription, 3);
		List<String> no1 = RegExUtils.getMatches("(?is)\\b(PB|B(OO)?K)\\s?(\\d+\\s)PA?G?E?\\s?(\\d+)", legalDescription, 4);
		String platBookPage = "";
		for (int i=0; i<book1.size(); i++) platBookPage += " " + book1.get(i).trim() + "&" + no1.get(i).trim();
		platBookPage = platBookPage.trim();
		if (platBookPage.length() != 0)
		{
			platBookPage = LegalDescription.cleanValues(platBookPage, false, true);
			String[] values = platBookPage.split("\\s");
			
			ResultTable platTable = new ResultTable();			
			@SuppressWarnings("rawtypes")
			List<List> tablebodyPlat = new ArrayList<List>();
			List<String> list;
			for (int i=0; i<values.length; i++)			
			{
				String[] bookAndPage = values[i].split("&");
				list = new ArrayList<String>();
				list.add(bookAndPage[0]);
				list.add(bookAndPage[1]);
				tablebodyPlat.add(list);
			}
			String[] headerPlat = {PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), PropertyIdentificationSetKey.PLAT_NO.getShortKeyName()};
			platTable = GenericFunctions2.createResultTable(tablebodyPlat, headerPlat);
			if (platTable != null){
				resultMap.put("PropertyIdentificationSet", platTable);
			}
		}
		
		List<String> book2 = RegExUtils.getMatches("(?is)\\bDB(\\d+)(\\s|)PG?(\\d+)", legalDescription, 1);
		List<String> no2 = RegExUtils.getMatches("(?is)\\bDB(\\d+)(\\s|)PG?(\\d+)", legalDescription, 3);
		String crossBookPage = "";
		for (int i=0; i<book2.size(); i++) crossBookPage += " " + book2.get(i) + "&" + no2.get(i);
		book2 = RegExUtils.getMatches("(?is)\\bDB(\\d+)/(\\d+)", legalDescription, 1);
		no2 = RegExUtils.getMatches("(?is)\\bDB(\\d+)/(\\d+)", legalDescription, 2);
		for (int i=0; i<book2.size(); i++) crossBookPage += " " + book2.get(i) + "&" + no2.get(i);
		crossBookPage = crossBookPage.trim();
		if (crossBookPage.length() != 0)
		{
			crossBookPage = LegalDescription.cleanValues(crossBookPage, false, true);
			String[] values = crossBookPage.split("\\s");
			
			ResultTable crossRef = new ResultTable();			//cross references table
			@SuppressWarnings("rawtypes")
			List<List> tablebodyRef = new ArrayList<List>();
			List<String> list;
			for (int i=0; i<values.length; i++)			
			{
				String[] bookAndPage = values[i].split("&");
				list = new ArrayList<String>();
				list.add(bookAndPage[0]);
				list.add(bookAndPage[1]);
				tablebodyRef.add(list);
			}
			String[] headerRef = {CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName()};
			crossRef = GenericFunctions2.createResultTable(tablebodyRef, headerRef);
			if (crossRef != null){
				resultMap.put("CrossRefSet", crossRef);
			}
		}
	}
	
	public static void parseTaxes(NodeList nodeList, ResultMap resultMap, long searchId) {
		
		String year = "";
		String baseAmount = "0.0";
		String totalDue = "0.0";
		String priorDelinquent = "0.0";
		String amountPaid = "0.0";
				
		NodeList yearList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "Taxes"))
			.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"))
			.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"));
		if (yearList!= null && yearList.size()>=3)
		{
			TableTag yearTable = (TableTag)yearList.elementAt(2);	
			year = yearTable.getRow(0).getColumns()[0].toPlainTextString().replaceFirst("(?is)Tax Year:", "").trim();
		}
				
		NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "Taxes"));
		if (tableList!=null)
			baseAmount = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tableList, "Total Net Tax:"), "", true)
				.replaceAll("(?is)</?font.*?>", "").replaceAll("(?is)</?b>", "").replaceAll("[,(&nbsp;)]", "").trim(); 
		
		tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "tabsummary"))
			.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("bordercolor", "#C0C0C0"));
		if (tableList!= null && tableList.size()>=1)
		{
			TableTag taxesTable = (TableTag)tableList.elementAt(0);	
			if (taxesTable.getRowCount()>=5 && 
					taxesTable.toHtml().contains("Installment") && 
					taxesTable.getRow(3).getColumnCount()>5)
				totalDue = taxesTable.getRow(3).getColumns()[5].toPlainTextString().replaceAll("[\\$,(&nbsp;)]", "").trim();
		}
		
		tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "Taxes"))
			.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "1"))
			.extractAllNodesThatMatch(new HasAttributeFilter("bordercolor", "#C0C0C0"));
		if (tableList!= null && tableList.size()>=3)
		{
			TableTag taxesTable = (TableTag)tableList.elementAt(2);	
			if (!taxesTable.toHtml().contains("NO DELINQUENT TAXES"))
				priorDelinquent = taxesTable.getRow(taxesTable.getRowCount()-1).getColumns()[1].toPlainTextString().replaceAll("[\\$,(&nbsp;)]", "").trim();
		}
		
		if(priorDelinquent.equals("0.0")){
			priorDelinquent = HtmlParser3
					.getValueFromAbsoluteCell(
							0,
							1,
							HtmlParser3.findNode(nodeList, "Total Delinquent:"),
							"", true).replaceAll("\\s+","").replaceAll("<[^>]*>", "")
					.replace("&nbsp;", "").replaceAll("[$-,]", "");
		}
		
		if(StringUtils.isEmpty(priorDelinquent))
			priorDelinquent = "0.0";
		
		tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("id", "Overview2"))
			.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "1"));
		if (tableList!= null && tableList.size()>=3)
		{
			TableTag taxesTable = (TableTag)tableList.elementAt(2);	
			for (int i=1;i<taxesTable.getRowCount();i++)
				if (taxesTable.getRow(i).getColumns()[0].toPlainTextString().trim().equals(year))
				{
					amountPaid = taxesTable.getRow(i).getColumns()[5].toPlainTextString().replaceAll("[\\$,(&nbsp;)]", "").trim();
					break;
				}
		}
		
		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
		resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
		resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
		resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
		resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
	}
}

package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class UTUtahTR {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		
		TableColumn[] cols = row.getColumns();
		if (cols.length == 6)						//name search
		{
			String name = cols[0].toPlainTextString();
			resultMap.put("PropertyIdentificationSet.NameOnServer",name);
			parseNames(resultMap, searchId);
			String serial = cols[1].toPlainTextString().replaceAll(":", "");
			resultMap.put("PropertyIdentificationSet.ParcelID",serial);
			String taxDistrict = cols[3].toPlainTextString().replaceAll("[()]", "");
			resultMap.put("PropertyIdentificationSet.District",taxDistrict);
			String propertyAddress = cols[5].toPlainTextString();
			resultMap.put("PropertyIdentificationSet.AddressOnServer",propertyAddress);
			parseAddress(resultMap);
			String year = cols[4].toPlainTextString().trim();
			int dashPosition = year.indexOf("-");
			if (dashPosition != -1) year = year.substring(dashPosition+1);
			int dotsPosition = year.indexOf("...");
			if (dotsPosition != -1) 
			{
				java.util.Calendar calendar = java.util.Calendar.getInstance(); 
				year = Integer.toString(calendar.get(java.util.Calendar.YEAR)); 
			}
			year = year.replaceAll("NV", "");
			resultMap.put("TaxHistorySet.Year",year);
		}
		else if (cols.length == 4)					//address search
		{
			String serial = cols[0].toPlainTextString().replaceAll(":", "");
			resultMap.put("PropertyIdentificationSet.ParcelID",serial);
			String propertyAddress = cols[1].toPlainTextString();
			resultMap.put("PropertyIdentificationSet.AddressOnServer",propertyAddress);
			parseAddress(resultMap);
			String year = cols[2].toPlainTextString().trim();
			int dashPosition = year.indexOf("-");
			if (dashPosition != -1) year = year.substring(dashPosition+1);
			int dotsPosition = year.indexOf("...");
			if (dotsPosition != -1) 
			{
				java.util.Calendar calendar = java.util.Calendar.getInstance(); 
				year = Integer.toString(calendar.get(java.util.Calendar.YEAR)); 
			}	
			year = year.replaceAll("NV", "");
			resultMap.put("TaxHistorySet.Year",year);
		}
		else if (cols.length == 11)					//serial number search
		{
			String name = cols[0].toPlainTextString();
			resultMap.put("PropertyIdentificationSet.NameOnServer",name);
			parseNames(resultMap, searchId);
			String serial = cols[2].getChildren().elementAt(1).getChildren().elementAt(3).getText();
			serial = serial.substring(37,serial.length()-4).replaceAll(":", "");
			int zeroesNumber = 9 - serial.length();
			for (int i=0; i<zeroesNumber; i++) serial = "0" + serial;
			resultMap.put("PropertyIdentificationSet.ParcelID",serial);
			String taxDistrict = cols[4].toPlainTextString().trim();
			resultMap.put("PropertyIdentificationSet.District",taxDistrict);
			String year = cols[10].toPlainTextString().trim();
			int dashPosition = year.indexOf("-");
			if (dashPosition != -1) year = year.substring(dashPosition+1);
			int dotsPosition = year.indexOf("...");
			if (dotsPosition != -1) 
			{
				java.util.Calendar calendar = java.util.Calendar.getInstance(); 
				year = Integer.toString(calendar.get(java.util.Calendar.YEAR)); 
			}
			year = year.replaceAll("NV", "");
			resultMap.put("TaxHistorySet.Year",year);
		}

		resultMap.removeTempDef();
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		String owner = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
		if (StringUtils.isEmpty(owner))
			   return;
		
		List<String> book = RegExUtils.getMatches("BOOK\\s(\\d+)", owner, 1);
		List<String> page = RegExUtils.getMatches("PAGE\\s(\\d+)", owner, 1);
		if (book.size()>0 || page.size()>0)
		{
			if (book.get(0).length()>0) resultMap.put("CrossRefSet.BOOK", book.get(0));
			if (page.get(0).length()>0) resultMap.put("CrossRefSet.PAGE", page.get(0));
			return;
		}	
		owner = owner.replaceAll(";\\s*JT\\s*", " ");
		owner = owner.replaceAll("\\(ET\\s+AL\\)", "ETAL");
		owner = owner.replaceAll("\\bET\\s+AL\\b", "ETAL");
		owner = owner.replaceAll("\\s*;\\s*", " ");
		owner = owner.replaceAll("\\bTIC\\b", "");
		int commaPos1, commaPos2;
		commaPos1 = owner.indexOf(",");
		commaPos2 = owner.lastIndexOf(",");
		if (commaPos1!=-1 && commaPos1 == commaPos2)		//for a name written like "SMITH MABLE W L, JEAN D"
		{													//it should be "SMITH MABLE W L& JEAN D"
			String part1 = owner.substring(0, commaPos1);
			String[] tokens = part1.split("\\s+");
			if (tokens.length > 2) owner = owner.replace(",", "&");
		}
		String[] ownerRows = owner.split("\n");
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
			ow = clean(ow);
			names = StringFormats.parseNameNashville(ow, true);
			String middlenames[] = names[1].split(" ");			//if a last name is put as a middle name
			for (int j=0; j<middlenames.length; j++) 			//example: JOHNSON, JANA LEE SMITH
				if ( LastNameUtils.isLastName(middlenames[j]) && !FirstNameUtils.isFirstName(middlenames[j]))
				{
					names[2] = middlenames[j] + " " + names[2];
					middlenames[j] = "";
				}
			String middlename ="";
			for (int j=0; j<middlenames.length; j++) middlename += " " + middlenames[j];
			middlename = middlename.trim();
			names[1] = middlename;
			if (!NameUtils.isCompany(ow)) suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			nameOnServerBuff.append("/").append(ow);
		}
		String nameOnServer = nameOnServerBuff.toString();
		nameOnServer = nameOnServer.replaceFirst("/", "");
		resultMap.put("PropertyIdentificationSet.NameOnServer", nameOnServer);
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String[] a = StringFormats.parseNameNashville(owner, true);
		String middlenames[] = a[1].split(" ");			//if a last name is put as a middle name
		for (int j=0; j<middlenames.length; j++) 		//example: JOHNSON, JANA LEE SMITH
			if ( LastNameUtils.isLastName(middlenames[j]) && !FirstNameUtils.isFirstName(middlenames[j]))
			{
				a[2] = middlenames[j] + " " + a[2];
				middlenames[j] = "";
			}
		String middlename ="";
		for (int j=0; j<middlenames.length; j++) middlename += " " + middlenames[j];
		middlename = middlename.trim();
		a[1] = middlename;
		resultMap.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		resultMap.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		resultMap.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		resultMap.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		resultMap.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		resultMap.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	}
	
	private static String clean(String str) {
		if(StringUtils.isEmpty(str)) return "";
		return 
			str .trim()
				.replaceAll("<br>", "\n")
				.replaceAll("\\b[\\d/]+\\s+INT(?:\\s+EA)?", "&")
				.replaceAll("\\b[W|H]\\s*&\\s*[H|W]\\b", "")
				.replaceAll("\\b[A-Z]\\s*/\\s*[A-Z]\\b", "")
				.replaceAll("(?is)\\bJTWRS\\b", "")
				.replaceAll("(?is)\\bPR\\b", "")
				.replaceAll("(?is)\\b-?POA\\b", "")
				.replaceAll("(?is)\\A\\s*OF\\s+", "")
				.replaceAll("(?is)\\bTRE\\b", "TRUSTEE")
				.replaceAll("(?is)\\bCO[-|\\s+](TR(?:USTEE)?S?)\\b", "$1");
	}
	
	public static void parseAddress(ResultMap resultMap) {
		
		String address = (String) resultMap.get("PropertyIdentificationSet.AddressOnServer");
		if (StringUtils.isEmpty(address))
			return;
		
		parseAddress(resultMap, address);
	}
	
	public static void parseAddress(ResultMap resultMap, String origAddress)  {
		
		String streetNo ="", streetName = "", city = ""; 
		
		int index = origAddress.indexOf("-"); 
		if ( index == -1)						//only city name
		{
			city = origAddress;
		}
		else 									//format: street_no street_name - city
		{
			city = origAddress.substring(index+1).trim();
			String streetnoname = origAddress.substring(0, index).trim();
			int spaceposition = streetnoname.indexOf(" ");
			if (spaceposition != -1)
			{
				streetNo = streetnoname.substring(0, spaceposition).trim();
				streetName = streetnoname.substring(spaceposition).trim();
			}
			
		}
						
		resultMap.put("PropertyIdentificationSet.StreetName",streetName);
		resultMap.put("PropertyIdentificationSet.StreetNo",streetNo);
		resultMap.put("PropertyIdentificationSet.City",city);
		
	}

	public static void parseLegalSummary(ResultMap resultMap)
	{
		
		String legalDescription = (String) resultMap.get("PropertyIdentificationSet.LegalDescriptionOnServer");
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		legalDescription += " ";	//we need this to be sure all regular expressions match 
		
		List<String> tmplot = RegExUtils.getMatches("(LOT(\\s|)((\\d+),)+(\\d+)\\s)|(LOT\\s(\\d+)(,|\\s))|(LOTS((\\s|)(\\d+)(\\s|)(&|,|\\s))+)", legalDescription, 0);
		if (tmplot.size()>0)
		{
			List<String> lot = new ArrayList<String>();
			for (int i=0; i<tmplot.size(); i++)
			{
				String[] splitted = tmplot.get(i).replaceAll("LOT(S|)", "") .split("\\s|,|&");
				for (int j=0;j<splitted.length;j++) if (splitted[j].trim().length()!=0) lot.add(splitted[j]);
			}	
			Collections.sort(lot);
			int k = 1;
			for (int i = 1; i < lot.size(); i++)				//remove duplicates
			{
				if (! lot.get(i).equals(lot.get(i-1)))
					lot.set(k++,lot.get(i));
			}
			String subdivisionLotNumber = "";
			for (int i=0; i<k; i++) subdivisionLotNumber += " " + lot.get(i);
			subdivisionLotNumber = subdivisionLotNumber.trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", subdivisionLotNumber);
		}
		
		List<String> plat = RegExUtils.getMatches("PLAT(\\s|)(.*?)(,|\\s)", legalDescription, 2);
		if (plat.size()>0)
		{
			for (int i=0; i<plat.size(); i++) plat.set(i, plat.get(i).replaceAll("\"", ""));
			Collections.sort(plat);
			int k = 1;
			for (int i = 1; i < plat.size(); i++)				//remove duplicates
			{
				if (! plat.get(i).equals(plat.get(i-1)))
					plat.set(k++,plat.get(i));
			}
			String platBook = "";
			for (int i=0; i<k; i++) platBook += " " + plat.get(i);
			platBook = platBook.trim();
			resultMap.put("PropertyIdentificationSet.PlatBook", platBook);
		}
		
		List<String> subdivision = RegExUtils.getMatches(",(.*?)SUB(\\s|\\.|;|D(\\s|\\.|;)|DV(\\s|\\.|;)|DIVISION(\\s|\\.|;))", legalDescription, 1);
		if (subdivision.size()>0)
		{
			for (int i=0; i<subdivision.size(); i++) 
				{
					int lastComma = subdivision.get(i).lastIndexOf(",");
					if (lastComma != -1) subdivision.set(i, subdivision.get(i).substring(lastComma +1));
				}
			Collections.sort(subdivision);
			int k = 1;
			for (int i = 1; i < subdivision.size(); i++)		//remove duplicates
			{
				if (! subdivision.get(i).equals(subdivision.get(i-1)))
					subdivision.set(k++,subdivision.get(i));
			}
			String subdivisionName = "";
			for (int i=0; i<k; i++) subdivisionName += " " + subdivision.get(i);
			subdivisionName = subdivisionName.trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionName", subdivisionName);
		}
		
		List<String> blk = RegExUtils.getMatches("BLK(\\s|)(\\d+),", legalDescription, 2);
		if (blk.size()>0)
		{
			Collections.sort(blk);
			int k = 1;
			for (int i = 1; i < blk.size(); i++)			//remove duplicates
			{
				if (! blk.get(i).equals(blk.get(i-1)))
					blk.set(k++,blk.get(i));
			}
			String subdivisionBlock = "";
			for (int i=0; i<k; i++) subdivisionBlock += " " + blk.get(i);
			subdivisionBlock = subdivisionBlock.trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock", subdivisionBlock);
		}
		
		List<String> section = RegExUtils.getMatches("SEC(TION|\\.|)\\s(\\d+)(,|\\s)", legalDescription, 2);
		if (section.size()>0)
		{
			Collections.sort(section);
			int k = 1;
			for (int i = 1; i < section.size(); i++)		//remove duplicates
			{
				if (! section.get(i).equals(section.get(i-1)))
					section.set(k++,section.get(i));
			}
			String subdivisionSection = "";
			for (int i=0; i<k; i++) subdivisionSection += " " + section.get(i);
			subdivisionSection = subdivisionSection.trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", subdivisionSection);
		}
		
		List<String> township = RegExUtils.getMatches("SEC(TION|\\.|)\\s(\\d+)(,|\\s)\\s?T(OWNSHIP|)(.*?)(,|\\s)R(|ANGE)", legalDescription, 5);
		if (township.size()>0)
		{
			for (int i = 0; i < township.size(); i++) township.set(i, township.get(i).replaceAll(",", "").replaceAll(" ", ""));
			Collections.sort(township);
			int k = 1;
			for (int i = 1; i < township.size(); i++)		//remove duplicates
			{
				if (! township.get(i).equals(township.get(i-1)))
					township.set(k++,township.get(i));
			}
			String subdivisionTownship = "";
			for (int i=0; i<k; i++) subdivisionTownship += " " + township.get(i);
			subdivisionTownship = subdivisionTownship.trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionTownShip", subdivisionTownship);
		}
		
		List<String> range1 = RegExUtils.getMatches("SEC(TION|\\.|)\\s(\\d+)(,|\\s)\\s?T(OWNSHIP|)(.*?)(,|\\s)R(|ANGE)(\\s)?(\\d)(\\s)?([A-Z])", legalDescription, 9);
		List<String> range2 = RegExUtils.getMatches("SEC(TION|\\.|)\\s(\\d+)(,|\\s)\\s?T(OWNSHIP|)(.*?)(,|\\s)R(|ANGE)(\\s)?(\\d)(\\s)?([A-Z])", legalDescription, 11);
		List<String> range = new ArrayList<String>();
		for (int i=0; i<range1.size(); i++) range.add(range1.get(i) + range2.get(i));
		if (range.size()>0)
		{
			Collections.sort(range);
			int k = 1;
			for (int i = 1; i < range.size(); i++)
			{
				if (! range.get(i).equals(range.get(i-1)))	//remove duplicates
					range.set(k++,range.get(i));
			}
			String subdivisionRange = "";
			for (int i=0; i<k; i++) subdivisionRange += " " + range.get(i);
			subdivisionRange = subdivisionRange.trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionRange", subdivisionRange);
		}
		
	}
	
	public static void parseTaxes(NodeList nodeList, ResultMap resultMap, long searchId) {
		
		String year = "";
		double baseAmount = 0.0;
		double totalDue = 0.0;
		double priorDelinquent = 0.0;
		double amountPaid = 0.0;
		
		NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("name", "LastYearTaxTable"));
		
		if (tableList.size() == 1)
		{
			TableTag taxes = (TableTag) tableList.elementAt(0);
			TableRow[] rows = taxes.getRows();
			
			for (int i = 0; i < rows.length; i++)
			{
				String row = rows[i].toPlainTextString();
				if (row.contains("Tax Year:")) year = row.replaceAll("(Serial.*Year: )", "").trim();
				else if (row.contains("General Taxes:"))
				{
					String baseAmountString = row.replaceAll("&nbsp;", "");
					baseAmountString = baseAmountString.substring(baseAmountString.indexOf("$")+1).trim();
					baseAmount = Double.parseDouble(baseAmountString.replaceAll(",", ""));
				}
				else if (row.contains("Tax Balance:"))
				{
					String totalDueString = row.replaceAll("&nbsp;", "");
					totalDueString = totalDueString.substring(totalDueString.indexOf("$")+1).trim();
					totalDue = Double.parseDouble(totalDueString.replaceAll(",", ""));
				}
				else if (row.contains("Payments:"))
				{
					String amountPaidString = row.replaceAll("&nbsp;", "");
					amountPaidString = amountPaidString.substring(amountPaidString.indexOf("$")+1).trim();
					amountPaid = Double.parseDouble(amountPaidString.replaceAll(",", ""));
				}
			}
			
			NodeList delinquentTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "497"));
			
			if (delinquentTableList.size() == 1)
			{
				TableTag delinquent = (TableTag) delinquentTableList.elementAt(0);
				TableRow lastRow = delinquent.getRow(delinquent.getRowCount()-1);
				String sum = lastRow.getColumns()[1].toPlainTextString().replaceAll(",", "").replaceAll("\\$", "");
				priorDelinquent = Double.parseDouble(sum);
			}	
								
			resultMap.put("TaxHistorySet.Year", year);
			resultMap.put("TaxHistorySet.BaseAmount", Double.toString(baseAmount));
			resultMap.put("TaxHistorySet.TotalDue", Double.toString(totalDue));
			resultMap.put("TaxHistorySet.PriorDelinquent", Double.toString(priorDelinquent));
			resultMap.put("TaxHistorySet.AmountPaid", Double.toString(amountPaid));
			
		}
					
	}
}

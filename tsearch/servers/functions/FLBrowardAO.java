package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class FLBrowardAO {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
		
		String pin = row.getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pin);
		
		if (row.getColumnCount()==2) {	//subdivision name intermediary result
			String subdName = row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdName);
		} else {						//other intermediary result
			String owner = row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", "<br>").trim();
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
			
			String address = row.getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			
			parseNames(resultMap, searchId);
			parseAddress(resultMap);
		}
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		
		owner = owner.replaceAll("(?is)\\bH/E\\b", "");
		owner = owner.replaceAll("(?is)\\(BLDG\\)", "");
		owner = owner.replaceAll("(?is)\\(LAND\\)", "");
		owner = owner.replaceAll("(?is)\\bTRSTEE(S?)\\b", "TR$1");
		owner = owner.replaceFirst("^\\s*<br>", "");
		
		String[] owners = owner.split("(?is)<br>");		
		
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type = {"", ""}, otherType;
		List<List> body = new ArrayList<List>();
		
		for (String ow: owners) {
			ow = ow.replaceFirst("^\\s*&", "");
			ow = ow.replaceFirst("&\\s*$", "");
			
			if (ow.indexOf(",")==-1) {	//not a person name
				if (!NameUtils.isCompany(ow)) {
					ow = ow.replaceFirst("(.*?)\\bTR\\s*$", "$1TRUST");
				}
			}
			
			names = StringFormats.parseNameNashville(ow, true);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			if (!NameUtils.isCompany(ow)) {
				type = GenericFunctions.extractAllNamesType(names);
			}
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void parseAddress(ResultMap resultMap) {
		
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(address)) {
			return;
		}
		
		int index = address.indexOf(",");
		if (index>-1) {
			String city = address.substring(index+1).trim();
			address = address.substring(0, index).trim();
			city = city.replaceAll("(?is)\\bUNINCORPORATED\\b", "");
			if (!StringUtils.isEmpty(city)) {
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
			}
		}
		address = address.replaceFirst("(?is)\\b(\\d+-[A-Z]+)\\s*\\z", "#$1");
		address = address.replaceFirst("(?is)\\b([A-Z]+-\\d+)\\s*\\z", "#$1");
		
		String streetName = StringFormats.StreetName(address);
		String streetNo = StringFormats.StreetNo(address);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		
	}
	
	public static void parseLegalSummary(ResultMap resultMap, long searchId) {
		try {
			
			String legalDescription = (String)resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
			if (StringUtils.isEmpty(legalDescription)) {
				return;
			}
			
			//TWO 104-34 -> TWO FAKE 104-34
			//otherwise, this would be interpreted as section-township-range (2-104-34)
			Matcher ma = Pattern.compile("([\\w-]+)\\s+(\\d+-\\d+)").matcher(legalDescription);
			while (ma.find()) {
				if (Normalize.isCompositeNumber(ma.group(1))) {
					legalDescription = legalDescription.replace(ma.group(0), ma.group(1) + " FAKE " + ma.group(2));
				}
			}
			legalDescription = legalDescription.replaceAll("(?is)\\b(SEC)\\s+(\\d+-\\d+)", "$1 FAKE $2");
			legalDescription = legalDescription.replaceAll("(?is)\\bLOT\\s+\\d+\\.\\d+", "");
			resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDescription);
			
			//the legal descriptions from AO and TR are very similar
			FLGenericGrantStreet3TR.legalFLBrowardTR(resultMap, searchId);
			
			//add what was missed
			extractValues(legalDescription, resultMap, "(?is)\\bLO?TS?\\s+([-\\d\\s&,]+)", 1, 
					PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), false);
			extractValues(legalDescription, resultMap, "(?is)\\bLO?T\\s+([A-Z]+\\d+)", 1, 
					PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), false);
			extractValues(legalDescription, resultMap, "(?is)\\bBL?K\\s+(\\d+|[A-Z])", 1, 
					PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), false);
			extractValues(legalDescription, resultMap, "(?is)\\bBL?KS?\\s+([-\\d\\s&,]+)", 1, 
					PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), false);
			extractValues(legalDescription, resultMap, "(?is)\\bTR(?:ACT)?\\s+(\\d+|[A-Z](?:-\\d+)?)", 1, 
					PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), false);
			extractValues(legalDescription, resultMap, "(?is)\\bSEC\\s+(\\d+|[A-Z])\\b", 1, 
					PropertyIdentificationSetKey.SECTION.getKeyName(), false);
			extractValues(legalDescription, resultMap, "(?is)\\bBLDG\\s+((?:(?:\\d+|[A-Z])(?:-\\d+)?)(?:,(?:(?:\\d+|[A-Z])(?:-\\d+)?))*)", 1, 
					PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), false);
			
			List<String> book = RegExUtils.getMatches("(?is)\\bOR\\s+(\\d+)/(\\d+)\\b", legalDescription, 1);
			List<String> page = RegExUtils.getMatches("(?is)\\bOR\\s+(\\d+)/(\\d+)\\b", legalDescription, 2);
			ResultTable crossRef = new ResultTable();
			@SuppressWarnings("rawtypes")
			List<List> tablebodyRef = new ArrayList<List>();
			for (int i=0; i<book.size(); i++) {
				List<String> list;
				list = new ArrayList<String>();
				list.add(book.get(i).replaceAll("\\A0+", ""));
				list.add(page.get(i).replaceAll("\\A0+", ""));
				tablebodyRef.add(list);
			}
			String[] headerRef = {CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName()};
			crossRef = GenericFunctions2.createResultTable(tablebodyRef, headerRef);
			if (crossRef != null && tablebodyRef.size()!=0){
				resultMap.put("CrossRefSet", crossRef);
			}
			
			//make corrections
			String subdName = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName());
			if (!StringUtils.isEmpty(subdName)) {
				subdName = subdName.replaceAll("(?is)\\bFAKE\\s*\\z", "");
				subdName = subdName.replaceAll("(?is)\\bPLAT\\s*\\z", "");
				subdName = subdName.replaceAll("(?is)\\(BLDG.*?\\)\\s*\\z", "");
				subdName = subdName.replaceAll("(?is),.\\s*\\z", "");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdName);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void extractValues(String legalDescription, ResultMap resultMap, String regEx, int group, String key, boolean onlyNumbers) {
		StringBuilder sb = new StringBuilder();
		
		String presentValue = (String)resultMap.get(key);
		if (presentValue==null) {
			presentValue = "";
		}
		List<String> value = RegExUtils.getMatches(regEx, legalDescription, group);
		for (int i=0; i<value.size(); i++) {
			sb.append(value.get(i).replaceAll("&", " ")).append(" ");
		}
		String newValue = sb.toString().trim();
		if (newValue.length() != 0) {
			if (!key.equals(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName())) {
				newValue = presentValue + " " +  newValue;
				newValue = LegalDescription.cleanValues(newValue, onlyNumbers, true);
			} else {
				newValue = newValue.replaceAll(",", " ");
				newValue = StringFormats.RemoveDuplicateValues(newValue);
			}
			resultMap.put(key, newValue);
		}
	}
}

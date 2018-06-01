package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class GenericSunbiz {

	public static void parseAndFillResultMap(ResultMap resultMap) {
		String nameOnServer = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
		if (StringUtils.isNotEmpty(nameOnServer)){
			setName(resultMap, nameOnServer); 
		}
		String addressOnServer = (String) resultMap.get("PropertyIdentificationSet.AddressOnServer");
														 
		if (StringUtils.isNotEmpty(addressOnServer)){
			setAddress(resultMap, addressOnServer);
		}
	}
	
	public static void setName(ResultMap resultMap, String name) {
		ArrayList<List> namesBody = new ArrayList<List>();
		name = GenericFunctions2.cleanOwnerNameFromPrefix(name);
		String[] names = {"", "", name, "", "", ""};
		String[] suffixes = {"", ""};
		
		ParseNameUtil.putNamesInResultMap(resultMap, namesBody, names , suffixes );
		
		if (StringUtils.isNotEmpty(name)){
				GenericFunctions.addOwnerNames(name, names, namesBody);
		}
		resultMap.put("SaleDataSet.Grantor", name);
		try {
			resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(namesBody));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void setAddress(ResultMap resultMap, String address) {
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),address.replaceAll("<[^>]*>","").replaceAll("\\s+", " ").trim());
		
		String[] split = address.split("<br/?>");
		
		String streetNameNo = "";
		String moreGeneralData = "";
		
		if (split.length>=2){
			streetNameNo = org.apache.commons.lang.StringUtils.trimToEmpty(split[0]).replaceAll("<[^>]*>","").replaceAll("\\s+", " ").trim();
			moreGeneralData = org.apache.commons.lang.StringUtils.trimToEmpty(split[1]).replaceAll("<[^>]*>","").replaceAll("\\s+", " ").trim();
		}
		
		if (!streetNameNo.equals("NONE")){
			String streetName = StringFormats.StreetName(streetNameNo);
			String streetNo = StringFormats.StreetNo(streetNameNo);
			resultMap.put("PropertyIdentificationSet.StreetName", streetName);
			resultMap.put("PropertyIdentificationSet.StreetNo", streetNo);
		}
		
		Pattern pattern = Pattern.compile("(.*\\s)([A-Z]{2,2})\\s(\\d+)(\\s[A-Z]{2,2})?");
		Matcher matcher = pattern.matcher(moreGeneralData);
		
		if (matcher.find()){
			resultMap.put("PropertyIdentificationSet.City", matcher.group(1).replace(",",""));
			resultMap.put("PropertyIdentificationSet.State", matcher.group(2));
			resultMap.put("PropertyIdentificationSet.Zip", matcher.group(3));
		}
		
	}
	
}

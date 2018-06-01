package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.RegExUtils;

public class ParseNameUtil {
	/**
	 * Before calling this function a proper cleaning should be done.
	 * Last,FirstMiddle or LastFirstMiddle or Company
	 * @param resultMap
	 * @param tmpOwnerName
	 * @param body
	 */
	public static String[] putNamesInResultMapFromNashvilleParse(ResultMap resultMap, String tmpOwnerName, List body) {
		String[] parsedName = StringFormats.parseNameNashville(tmpOwnerName, true);
		String[] suffixes = GenericFunctions.extractNameSuffixes(parsedName);
		putNamesInResultMap(resultMap, body, parsedName, suffixes);
		return parsedName;
	}
	
	
	/**
	 * Parses "First Middle Last" or "First Middle Last AND First Middle Last", or "First MI? AND First MI? Last".
	 * Before calling this function a proper cleaning should be done. 
	 * @param resultMap
	 * @param tmpOwnerName
	 * @param body
	 */
	public static void putNamesInResultMapFromDeSotoParse(ResultMap resultMap, String tmpOwnerName, List body) {
		tmpOwnerName = tmpOwnerName.trim();
		if (RegExUtils.matches("\\w*\\s*\\w{1,}?\\s*AND\\s\\w*\\s(\\w{1,}\\s)?(\\w*)", tmpOwnerName.trim())) {
			String[] split = tmpOwnerName.split("AND");
			if (split.length == 2) {
				String[] split2 = split[1].split("\\s");
				String lastName = split2[split2.length-1];
				String newName = split[0] + " " + lastName;
				String[] parsedName = StringFormats.parseNameDesotoRO(newName);
				String[] suffixes = GenericFunctions.extractNameSuffixes(parsedName);
				putNamesInResultMap(resultMap, body, parsedName, suffixes);
				tmpOwnerName = split[1];
			}
		}
		String[] parsedName = StringFormats.parseNameDesotoRO(tmpOwnerName);
		String[] suffixes = GenericFunctions.extractNameSuffixes(parsedName);
		putNamesInResultMap(resultMap, body, parsedName, suffixes);
	}
	
	public static void putCompanyInResultMap(ResultMap resultMap, List body,String  companyName) {
		String[] parsedName= new String[] {"", "",companyName, "", "", ""};
		String[] suffixes = new String[] {"", "", "", "", ""};
		putNamesInResultMap(resultMap, body, parsedName, suffixes);
	}
	
	public static void putNamesInResultMap(ResultMap resultMap, List body, String[] parsedName, String[] suffixes) {
		if (body == null) {
			body = new ArrayList<List>();
		}
		String[] type = GenericFunctions.extractAllNamesType(parsedName); 
		String[] otherType = GenericFunctions.extractAllNamesOtherType(parsedName);;
		GenericFunctions.addOwnerNames(parsedName, suffixes[0], suffixes[1], type, otherType,
				NameUtils.isCompany(parsedName[2]), NameUtils.isCompany(parsedName[5]), body);

		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (body != null) {
			String currentOwnerLastName = (String) resultMap.get("PropertyIdentificationSet.OwnerLastName");
			String lastAddedName = "";
			if (body.size() > 0) {
				List<String> list = (List<String>) body.get(body.size() - 1);
				// construct the name
				
				for (String s : list) {
					lastAddedName += StringUtils.defaultIfEmpty(s, "")+" ";
				}
			}
			currentOwnerLastName = StringUtils.defaultIfEmpty(currentOwnerLastName, "") + lastAddedName;
		}
	}

}

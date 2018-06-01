package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

public class TNMontgomeryTR {
	

	public static ResultMap parseIntermediaryRow(TableRow row) {
		
		ResultMap resultMap = new ResultMap();
		TableColumn[] columns = row.getColumns();
		if (columns.length > 5) {
			resultMap.put("tmpOwner", columns[1].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
			resultMap.put("tmpAddress", columns[3].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
			resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(),
					columns[4].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(),
					columns[5].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());

			parseNames(resultMap);
			parseAddress(resultMap);
		}
		return resultMap;
		
	}

	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap) {
		
		String owner = (String) resultMap.get("tmpOwner");
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
		
		owner = owner.replaceAll("(?is)(,\\s*)?ATTORNEY AT LAW\\b", "");
		owner = owner.replaceAll("(?is)\\bMRS\\b", "");
		owner = owner.replaceAll("(?is)\\bC/O\\b", "");
		owner = owner.replaceAll("(?is),(\\s*ET(?:UX|VIR|AL))\\b\\s*$", "$1");
		owner = owner.replaceAll("(?is),(\\s*ET(?:UX|VIR|AL))\\b", "$1");
		owner = owner.replaceAll("%", ",");
		owner = owner.replaceAll("(?is)(.*?)(TRUSTEE)\\s+OF\\s+THE(.*?)TRS?", "$1$2$3TRUST");
		owner = owner.replaceAll("(?is)\\bOF\\s*,", " OF ");
		owner = owner.replaceAll("(?is),((?:\\s*[A-Z-']+\\s+)?\\s*(?:LLC|INC|BUREAU))\\b", " $1,");
		
		//LOPEZ-FIGUEROA JUAN P LOPEZ-FIGUEROA BLANCO MIRI (PIN  064D-A-064D-097.00--000)
		owner = owner.replaceFirst("(?is)^\\s*([A-Z-]{2,})\\s+([A-Z-]+(?:\\s+[A-Z-]+(?:\\s+[A-Z-]+)?)?)\\s+\\1\\s+([A-Z-]+(?:\\s+[A-Z-]+)?)\\s*$", "$1 $2 & $1 $3");
		
		if (owner.split("&").length == 2) {// e.g. 'James Patrick E & James Indira' was parsed wrong
			owner = owner.replaceAll("\\s+", " ");
			String[] name = owner.split("\\s*&\\s*");

			if (name[0].split(" ")[0].equalsIgnoreCase(name[1].split(" ")[0])) {
				owner = owner.replaceFirst("&", ",");
			}
		}
		
		if (owner.matches("(?is)[A-Z\\s.]+")) {
			// task 9207 - e.g. ALLEN MICHAEL EUGENE ALLEN CAROL L; ALLEN MICHAEL EUGENE JR. ALLEN CAROL L; ALLEN MICHAEL ALLEN CAROL L
			String[] token = owner.split("\\s+");
			if (token.length >= 5 && LastNameUtils.isLastName(token[0])){
				if (LastNameUtils.isLastName(token[3])) {
					owner = owner.replaceFirst("(" + token[0] + ".*?)" + "(" + token[3] + ")", "$1, $2");
				}
				else if (LastNameUtils.isLastName(token[2])) {
					owner = owner.replaceFirst("(" + token[0] + ".*?)" + "(" + token[2] + ")", "$1, $2");
				}
				else if (LastNameUtils.isLastName(token[4])) {
					owner = owner.replaceFirst("(" + token[0] + ".*?)" + "(" + token[4] + ")", "$1, $2");
				}
			}
		}
		
		String[] owners = owner.split(",");		
		
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		List<List> body = new ArrayList<List>();
		
		NameFactory nameFactory = NameFactory.getInstance();

		for (String ow: owners) {
			ow = ow.replaceAll("(?is)\\bDBA\\b", "");
			ow = ow.trim();
			names = StringFormats.parseNameNashville(ow, true);
			if (!NameUtils.isCompany(ow)) {
				if ( ow.matches("(?is).*?\\bATTN\\b.*")  || names[2].length()==1 ||
					 (FirstNameUtils.isFirstName(names[2]) && !nameFactory.isLast(names[2])) ||
					 (nameFactory.isLast(names[0]) && !FirstNameUtils.isFirstName(names[0]) && 
					  !nameFactory.isLast(names[2])) ) {
					ow = ow.replaceAll("(?is)\\bATTN\\b", "");
					//ARTHUR & JUANITA JONES => ARTHUR JONES & JUANITA
					ow = ow.replaceAll("(?is)([A-Z-']+)\\s*&\\s*([A-Z-']+)\\s+([A-Z-']+)", "$1 $3 & $2");
					names = StringFormats.parseNameDesotoRO(ow, true);	//DENNIS VAN WORMER
				}
				if (names[1].endsWith("-")) {	//HARRIS MANDY N USSERY- => USSERY-HARRIS MANDY N
					Matcher ma = Pattern.compile("(.*?)([A-Z]+-)$").matcher(names[1]);
					if (ma.find()) {
						names[1] = ma.group(1).trim();
						names[2] = ma.group(2) + names[2];
					}
				}
			}
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
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
		
		String address = (String) resultMap.get("tmpAddress");
		if(StringUtils.isEmpty(address)) {
			return;
		}
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
		address = address.replaceAll("\\bS(UI)?TE\\s*", "#");
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address).trim());
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address).trim());
	}
	
}

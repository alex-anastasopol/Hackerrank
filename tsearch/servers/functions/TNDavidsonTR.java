package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class TNDavidsonTR {

	@SuppressWarnings("unchecked")
	public static void partyNamesTNDavidsonTR(ResultMap m, long searchId) throws Exception {

		String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
		String c = (String) m.get("tmpCoOwner");

		if (StringUtils.isEmpty(s))
			return;
		
		if (c != null && c.toLowerCase().contains("c/o")) {
			c = c.replaceAll("(?is)(?:C/O\\s*)+", " C/O ");
			c = c.replaceAll("(?is)[^/]+(c/o[^\\r\\n]+).*", "$1");//8724 SAWYER BROWN RD C/O H SMITH\r\nNASHVILLE TN 37221
			s = s + " " + c;
		}
		s = s.replaceAll("(?is)c/o\\s+c/o", "&");
		s = s.replaceAll("(?is)c/o\\s+([A-Z]+)\\s+([A-Z]+)$", "c/o $2, $1");
		s = s.replaceAll("MRS", "");
		s = s.replaceAll("(?is)c/o", "&");
		s = s.replaceAll("\\.;", ". &");
		s = s.replaceAll("(?is)(\\.)(\\w+)", "$1 $2");
		s = s.replaceAll("\\.,\\s+(CYNTHIA)", ". & $1"); // BROWN, JAMES M., CYNTHIA L.& MAXINE C.
		s = s.replaceAll("\\.,", ". ");
		s = s.replaceAll("(?is)\\s+ET\\s*AL?\\b", " ETAL");
		s = s.replaceAll("(?is)\\s+ET\\s*UX\\b", " ETUX");
		s = s.replaceAll("&\\s+&", "&");
		s = s.replaceAll("(?is)\\A([^&]+)\\s*&\\s*(\\w+(?:\\s+\\w+\\.?)?\\s*\\(?LE'?S?\\)?)", "$1 @@@ $2");
		s = s.replaceAll("(?is)\\.?\\s*,\\s*\\bTRU\\b\\s*", " ");
		s = s.replaceAll(",\\s*(TRU(?:ST(?:EES|EE|E)?)?)\\s*", " $1 ");
        s = s.replaceAll("(?is)(.*)\\d+(?:YR|RD|TH)\\s*QUALIFIED\\s*PERSON", "$1 ");
        s = s.replaceAll("(?is)(.*)D/B/A\\s*(.*)", "$1 & $2");
        
		String[] owners;
		String lastName = "";
		if (s.matches("(?is)[A-Z]+\\s*,\\s*[A-Z]+\\s*&\\s*[A-Z]+") || 
			s.matches("(.*(?:FAMILY\\s+)?TRUST),\\s*THE") ||
			s.matches("(?is)(.*)\\bTRUST\\b\\s*")){
			owners = new String[]{s};
		} else{
			owners = s.split("&");
		}

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i].trim();
			
			if (ow.contains("@@@")){
				ow = ow.replaceAll("@@@", "&");
			}
			if (ow.matches("\\s*([A-Z]+)\\s+AND\\s+([A-Z]+)\\s+([A-Z]+)")) { // SMITH, M.H.& NORENE S.(LE'S)& PUGH,D.L.
																				// c/o DAVID AND NICOLE PUGH
				ow = ow.replaceAll("\\s*([A-Z]+)\\s+AND\\s+([A-Z]+)\\s+([A-Z]+)", "$3, $2 AND $1");
			}
			if ((ow.matches("\\s*([A-Z]+)\\s+([A-Z\\.]+)\\s+([A-Z]+)"))
					&& (!ow.contains(","))) {// SMITH MICHAEL J.& DONNA L. DWORAK
				if (!ow.contains(" TRUST") && !NameUtils.isCompany(ow)) {
					ow = ow.replaceAll("\\s*([A-Z]+)\\s+([A-Z\\.]+)\\s+([A-Z]+)", "$3, $1 $2");
				}
			}
			if (ow.matches("\\s*([A-Z]+)\\s+([A-Z\\.]+)\\s+([A-Z]+)\\s+(TRUST(?:EES|EE|E))\\s*")) {
				ow = ow.replaceAll("\\s*([A-Z]+)\\s+([A-Z\\.]+)\\s+([A-Z]+)\\s+(TRUST(?:EES|EE|E))\\s*", "$3, $1 $2 $4");
			}
			if (ow.matches("(.*TRUST),\\s*THE")) {
				ow = ow.replaceAll("(.*TRUST),\\s*THE", "THE $1");
			}
			if (ow.matches("(.*\\bLLC\\b\\s*),\\s*THE")) {
				ow = ow.replaceAll("(.*\\bLLC\\b\\s*),\\s*THE", "$1");
			}
			
			if (i == 0) {
				if (NameUtils.isCompany(ow)){
					names[2] = ow;
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					GenericFunctions.addOwnerNames(names, "", "", type, otherType,
							true, false, body);
					continue;
				} else {
					names = StringFormats.parseNameNashville(ow, true);
					lastName = names[2];
					if (names[1].endsWith("-")) {// 15005002600 JOHNSON, ROBIN TATE-
						names[2] = names[1] + names[2];
						names[1] = "";
					}
				}
			}
			if (i > 0) {
				if (!ow.contains("%")) {
					names = StringFormats.parseNameNashville(ow, true);
					if (ow.contains(",")) {
						lastName = names[2];
					}
					if ((!ow.contains(",")) && (!NameUtils.isCompany(ow))) {
						if (!names[0].isEmpty()) {
							if (names[1].isEmpty()) {
								names[1] = names[0];
							} else {
								names[1] = names[0] + " " + names[1];
							}
						}
						names[0] = names[2];
						names[2] = lastName;
					}
				} else {
					names = StringFormats.parseNameDesotoRO(ow, true);
				}
			}
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
	}

	public static void parseAddressTNDavidsonTR (ResultMap m, long searchId) throws Exception {
		String address = (String) m.get("tmpPropAddr");
	   	
		if(StringUtils.isEmpty(address))
			return;
		
		if (StringUtils.isNotEmpty(address)) {
			m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			
			Matcher cityMat = Pattern.compile("(?is)(.*\\s+\\b(?:STREET|ST|DRIVE|DR|ROAD|RD|CIRCLE|CIR|AVENUE|AVE|COURT|CT|LN|LANE|BLVD|CV|HGWY|CR)\\b)\\s+([\\w\\s]+)\\s*$")
					.matcher(address);

			if (cityMat.matches()) {
				String city = cityMat.group(2).trim();
				address = cityMat.group(1).trim();
				if (StringUtils.isNotEmpty(city)) {
					m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				}
			}
			
			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
		}
	}
	
	public static void taxDavidsonTR(ResultMap m, long searchId) throws Exception {// county

		String amountsPaid = GenericFunctions.sum((String) m.get("TaxHistorySet.AmountPaid"), searchId);
		m.put("TaxHistorySet.AmountPaid", amountsPaid);
	}
	
	public static void cleanSubdivDavidsonTR(ResultMap m,long searchId) throws Exception{
	        
		//pt parcel ID 10602004800
		String tmp = (String) m.get("PropertyIdentificationSet.SubdivisionName");
	        
		if (tmp==null) {
			tmp="";
		}
	    
		tmp = tmp.replaceAll("L\\s\\d\\s\\dND\\sREV\\sL\\s[\\d-]*\\s+&\\sL\\s\\d\\s","");
		tmp = tmp.replaceAll("-?\\b(\\d+(ST|ND|RD|TH)\\s)?REV\\b", "");  // fix for bug #2198
		m.put("PropertyIdentificationSet.SubdivisionName", tmp.trim());
	}

}

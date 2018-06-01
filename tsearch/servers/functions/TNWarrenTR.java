package ro.cst.tsearch.servers.functions;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

public class TNWarrenTR {
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put("tmpOwner", row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
		resultMap.put("tmpAddress", row.getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
		resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), 
			row.getColumns()[3].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), 
			row.getColumns()[4].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
		
		parseNames(resultMap, searchId);
		parseAddress(resultMap);
		
		return resultMap;
		
	}
	
	public static void parseNames(ResultMap resultMap, long searchId) {
		String owner = (String) resultMap.get("tmpOwner");
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
		
		owner = owner.replaceAll("(?is)\\bDR\\.?", "");		//Doctor
		owner = owner.replaceAll("(?is)\\bDVM\\b", "");		//Doctor of Veterinary Medicine
		owner = owner.replaceAll("(?is)\\bADMIN\\b", "");	//Administrator
		owner = owner.replaceAll("(?is)\\bCPA\\b", "");		//Certified Public Accountant
		owner = owner.replaceAll("(?is)\\bESQ\\b", "");		//Esquire
		owner = owner.replaceAll("(?is)\\bPRES\\b", "");	//President
		owner = owner.replaceAll("\\bC/0\\b", "C/O");
		owner = owner.replaceAll("\\bC\\./O\\b", "C/O");
		owner = owner.replaceAll(",\\s*$", "");
		owner = owner.replaceAll("(?is)\\bEUTX\\b", "ETUX");
		owner = owner.replaceAll("(?is)\\bETAL\\s+ATN\\s*:?\\s+\\b", "ETAL ");
		owner = owner.replaceAll("(?is)\\b(ETAL)\\s+(TRUSTEES)\\b", "$2 $1");
		owner = owner.replaceAll("(?is)\\bDBA:?", "@@");
		owner = owner.replaceAll("(?is)\\bATTN:?\\s+", "@@ ");
		owner = owner.replaceAll("(?is)^(.+?SAW MILL)\\s+(.+)", "$1 @@ $2");
		owner = owner.replaceAll("(?is)\\b(ET(?:AL|UX|VIR))\\s*C/O\\b", "$1");
		
		if (!NameUtils.isCompany(owner)) {
			owner = owner.replaceAll("(?is)\\bAND\\b", "@@");
		}
		
		//PACK CARL LIFE ESTATE ETUX JOANNA
		owner = owner.replaceAll("(?is)^([A-Z'-]+)(\\s+.*?\\b(?:LIFE\\s+EST(?:ATE)?|TRUST))(\\s+ET(?:UX|VIR))(\\s+.*)", "$1$2$3 @@$1$4");
		
		owner = owner.replaceAll("%", "@@");
		owner = owner.replaceAll("(?is)\\b(ET(?:UX|VIR|AL))\\b", "$1 @@");
				
		if (NameUtils.isNotCompany(owner)) {
			//LINDSEY WILMA M RICKY J SMITH
			owner = owner.replaceAll("(?is)^([A-Z'-]+)\\s+([A-Z'-]+)\\s+([A-Z])\\s+([A-Z'-]+)\\s+([A-Z])\\s+([A-Z'-]+)$", "$1 $2 $3 @@ $4 $5 $6");
		}
		
		owner = owner.replaceAll("@@\\s*@@", "@@");
		
		resultMap.put("tmpOwnerNameAddress", owner);
		try {
			ro.cst.tsearch.servers.functions.TNGenericAO.ownerTNGenericAO(resultMap, searchId);
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
		address = address.replaceAll("\\bS(UI)?TE\\s*(\\d)", "#$2");
		address = address.replaceAll("(\\d+)\\s*&\\s*(\\d+)\\s*$", "$1-$2");
		
		String directions = "\\b(NE|SE|NW|SW|N|S|E|W)\\s+OF\\b";
		address = address.replaceAll("(?is)^\\s*" + directions, "$1");
		address = address.replaceAll("(?is)(.*?)\\s*\\(\\s*" + directions + "\\s*\\)\\s*$", "$2 $1");
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address).trim());
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address).trim());
	}

}

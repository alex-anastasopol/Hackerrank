package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class FLMiamiDadeAO {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");
		
		if (row.getColumnCount() == 4) {
			String instrNo = row.getColumns()[0].getFirstChild().getFirstChild().getText().trim();
			if (StringUtils.isNotEmpty(instrNo)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), instrNo);
			}
			
			String owners = row.getColumns()[2].getChildrenHTML();
			if (StringUtils.isNotEmpty(owners)) {
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owners);
				parseNames(resultMap, searchId);
			}
			
			String address = row.getColumns()[3].getChildrenHTML();
			if (StringUtils.isNotEmpty(owners)) {
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				parseAddress(resultMap);
			}
		}
		
//		if (row.getColumnCount()>0) {
//			Node node = row.getColumns()[0].getFirstChild();
//			if (node instanceof TableTag) {
//				TableTag table = (TableTag)node;
//				if (table.getRowCount()>0) {
//					TableRow tr = table.getRow(0);
//					if (tr.getColumnCount()>1) {
//						String pin = tr.getColumns()[1].toPlainTextString().replaceAll("(?is)Folio:", "").trim();
//						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pin);
//					}
//				}
//				if (table.getRowCount()>1) {
//					TableRow tr = table.getRow(1);
//					if (tr.getColumnCount()>0) {
//						String owner = tr.getColumns()[0].toPlainTextString().replaceAll("(?is)Owner:", "").trim();
//						resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName().replaceAll("(?is)&nbsp;", ""), owner);
//						parseNames(resultMap, searchId);
//					}
//				}
//				if (table.getRowCount()>2) {
//					TableRow tr = table.getRow(2);
//					if (tr.getColumnCount()>0) {
//						String address = tr.getColumns()[0].toPlainTextString().replaceAll("(?is)Address:", "").trim();
//						resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName().replaceAll("(?is)&nbsp;", ""), address);
//						parseAddress(resultMap);
//					}
//				}
//			}
//		}
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		
		if (owner.matches(".*\\bREFERENCE\\s*(ONLY|FOLIO)\\b.*")) {
			return;
		}
		
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		
		owner = owner.replaceAll("\\s{2,}", "<br>");
		owner = owner.replaceAll("(?is)\\bATTN\\b", "<br>");
		owner = owner.replaceAll("(?is)<br>\\s*<br>", "<br>");
		String[] split = owner.split("(?is)<br>");
		StringBuilder sb = new StringBuilder();
		sb.append(split[0].trim());
		
		for (int i=1;i<split.length;i++) {
			if (split[i].trim().length()==0) {
				break;
			}
			String[] spl = split[i].split("\\s+");
			if (spl[0].matches("\\d+(-(\\d+)?)?")) {	//street number or ZIP code
				break;
			}
			if (split[i].matches("(?is).*\\bPO\\s+BOX\\b.*")) {
				break;
			}
			sb.append("@@@").append(split[i].trim());
		}
		
		String newOwner = sb.toString();
		newOwner = newOwner.replaceAll("@@@", " & ");
		
		newOwner = newOwner.replaceAll("(?is)&\\s*W\\b", "&");	//& WIFE
		newOwner = newOwner.replaceAll("(?is)&\\s*H\\b", "&");	//& HUSBAND
		newOwner = newOwner.replaceAll("(?is)\\(JTRS\\b", "JTRS");
		newOwner = newOwner.replaceAll("(?is)\\b8ORGE\\b", "GEORGE");
		
		String[] owners = newOwner.split("[&%]");
		int len = owners.length;
		int index = 0;
		for (int i=1;i<len;i++) {
			if (owners[i].matches("(?is).*\\bJ?TRS.*")) {
				index = i;
				break;
			}
		}
		if (index>0) {
			for (int i=0;i<index;i++) {
				owners[i] += " TR";
			}
		}
		for (int i=0;i<len;i++) {
			owners[i] = owners[i].replaceAll("(?is)\\bJ?TRS\\b", "TR");
			owners[i] = owners[i].replaceAll("(?is)\\bTR\\s+TR\\b", "TR");
		}
		
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type = {"", ""}, otherType;
		String[] names2 = {"", "", "", "", "", ""};
		String[] suffixes2 = {"", ""} , type2 = {"", ""}, otherType2;
		List<List> body = new ArrayList<List>();
		
		String ln = "";
		ArrayList<String> incompleteNames = new ArrayList<String>();
		
		for (int i=0;i<len;i++) {
			String ow = owners[i].trim();
			if (ow.length()==0) {
				continue;
			}
			names = StringFormats.parseNameDesotoRO(ow, true);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			if (!hasLastName(names)) {
				if (ln.length()==0) {
					incompleteNames.add(ow);
				} else {
					String newName = ow + " " + ln;
					names2 = StringFormats.parseNameDesotoRO(newName, true);
					suffixes2 = GenericFunctions.extractNameSuffixes(names2);
					type2 = GenericFunctions.extractAllNamesType(names2);
					otherType2 = GenericFunctions.extractAllNamesOtherType(names2);
					GenericFunctions.addOwnerNames(names2, suffixes2[0], suffixes2[1], type2, otherType2,
							NameUtils.isCompany(names2[2]), NameUtils.isCompany(names2[5]), body);
				}
			} else {
				ln = names[2];
				if (i>0) {
					for (int j=0;j<incompleteNames.size();j++) {
						String newName = incompleteNames.get(j);
						newName += " " + names[2];
						names2 = StringFormats.parseNameDesotoRO(newName, true);
						suffixes2 = GenericFunctions.extractNameSuffixes(names2);
						type2 = GenericFunctions.extractAllNamesType(names2);
						otherType2 = GenericFunctions.extractAllNamesOtherType(names2);
						GenericFunctions.addOwnerNames(names2, suffixes2[0], suffixes2[1], type2, otherType2,
								NameUtils.isCompany(names2[2]), NameUtils.isCompany(names2[5]), body);
					}
					incompleteNames.clear();
				}
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
	
	public static boolean hasLastName(String[] names) {
		if (names.length<3) {
			return false;
		}
		if (names[0].length()==0 && names[1].length()==0 && names[2].length()>0 && !NameUtils.isCompany(names[2])) {
			return false;
		}
		if (names[2].length()<2) {
			return false;
		}
		if (FirstNameUtils.isFirstName(names[2]) && !LastNameUtils.isLastName(names[2])) {
			return false;
		}
		return true;
	}
	
	public static void parseAddress(ResultMap resultMap) {
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(address)) {
			return;
		}
		Matcher match = Pattern.compile("(?is)([^,]+),\\s*([A-Z\\s]+),\\s*FL\\s+([\\d-\\s]+)\\s*").matcher(address);
		if (match.find()) {
			String city = match.group(2).trim();
			String zip = match.group(3).trim();
			if (StringUtils.isNotEmpty(city)) {
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
			}
			if (StringUtils.isNotEmpty(zip)) {
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
			}
			address = match.group(1).trim();
		}
		
		String phasePatt = "(?is)\\bPH-(\\d+)";		//11930 N BAYSHORE DR PH-8, Folio 0622280341180
		Matcher ma = Pattern.compile(phasePatt).matcher(address);
		if (ma.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), ma.group(1));
			address = address.replaceAll(phasePatt, "").trim();
		}
		
		address = address.replaceFirst("(?is)\\b([A-Z]+-?\\d+)\\s*\\z", "#$1");	//680 NE 64 ST A305
		address = address.replaceFirst("(?is)\\b(\\d+-[A-Z]+)\\s*\\z", "#$1");	//1620 NW 4 AVE 13-A
		
		String streetName = StringFormats.StreetName(address);
		String streetNo = StringFormats.StreetNo(address);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseLegalSummary(ResultMap resultMap, long searchid) {
		try {
			String legalDescription = (String)resultMap.get("tmpFullLegalDescr");
			if (StringUtils.isEmpty(legalDescription)) {
				return;
			}
			
			resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDescription);
			
			//the legal descriptions from AO and TR are very similar
			ro.cst.tsearch.servers.functions.FLMiamiDadeTR.legalFLMiamiDadeTR(resultMap, searchid);
			
			//add what was missed
			
			extractValues(legalDescription, resultMap, "(?i)\\bUNIT\\s+([A-Z0-9-]+)", 1, PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), false);
			
			extractValues(legalDescription, resultMap, "(?i)\\bSEC(?:TION)?\\s+([A-Z]+)", 1, PropertyIdentificationSetKey.SECTION.getKeyName(), false);
			
			extractValues(legalDescription, resultMap, "(?i)\\bBLDG\\s+(\\d+)", 1, PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), false);
			
			String subdPatt = "(CONDO|SEC(TION)?|LOT|PB|UNIT)\\b";
			String pbPatt = "\\bPB\\s+\\d+-\\d+";
			Matcher matcher2 = Pattern.compile("(?i)(.+?)" + subdPatt).matcher(legalDescription);
			if (matcher2.find()) {
				String subdName = matcher2.group(1);
				subdName = subdName.replaceAll("\\d+(-\\d+)?\\s+\\d+\\s+\\d+", "");			//STR
				subdName = subdName.replaceAll("(?i)\\d*\\.\\d+\\s+AC\\b", "");				//acres
				subdName = subdName.replaceAll("\\s*@\\s*", " AT ");
				subdName = subdName.trim();
				if (!StringUtils.isEmpty(subdName)) {
					if (subdName.endsWith(" OF")) {
						if (matcher2.find()) {
							String next = matcher2.group(1).trim().replaceFirst("^\\d+-\\d+", "");
							subdName+= " " + next.trim();
						}
					}
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdName);
					if (legalDescription.matches("(?).*\\bCONDO.*")) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdName);
					}
				} else {	//39 .7 AC PB 46-13 J G HEADS F
					Matcher matcher3 = Pattern.compile("(?i)" + pbPatt + "(.+?)" + subdPatt).matcher(legalDescription);
					if (matcher3.find()) {
						subdName = matcher3.group(1);
						subdName = subdName.replaceAll("\\s*@\\s*", " AT ");
						subdName = subdName.trim();
						if (!StringUtils.isEmpty(subdName)) {
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdName);
							if (legalDescription.matches("(?).*\\bCONDO.*")) {
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdName);
							}
						}
					}
				}
			}
						
			String patt = "(?is)\\b(?:OR|REC|COC)\\s+(\\d+)-(\\d+)\\b";
			List<String> book = RegExUtils.getMatches(patt, legalDescription, 1);
			List<String> page = RegExUtils.getMatches(patt, legalDescription, 2);
			ResultTable crossRef = (ResultTable) resultMap.get("SaleDataSet");
			if (crossRef==null) {
				List<List> tablebody = null;
				ResultTable salesHistory = new ResultTable();
				tablebody = new ArrayList<List>();
				List<String> list;
				for (int i=0;i<book.size(); i++) {
					list = new ArrayList<String>();
					list.add(book.get(i).replaceFirst("^0+", ""));
					list.add(page.get(i).replaceFirst("^0+", ""));
					if (!tablebody.contains(list)) {
						tablebody.add(list);
					}
				}
				
				String[] header = {SaleDataSetKey.BOOK.getShortKeyName(), SaleDataSetKey.PAGE.getShortKeyName()};
				salesHistory = GenericFunctions2.createResultTable(tablebody, header);
				if (salesHistory != null && tablebody.size()>0){
					resultMap.put("SaleDataSet", salesHistory);
				}
			} else {	//add to the current table
				String[][] body = crossRef.getBody();
				List<List> bodyList = new ArrayList<List>();
				for (int i=0;i<body.length;i++) {
					bodyList.add(Arrays.asList(body[i]));
				}
				List<String> list;
				String bk = "";
				String pg = "";
				for (int i=0;i<book.size(); i++)
				{
					bk = book.get(i).replaceFirst("^0+", "");
					pg = page.get(i).replaceFirst("^0+", "");
					if (!isAlreadyInBody(body, bk, pg)) {
						list = new ArrayList<String>();
						list.add("");
						list.add("");
						list.add(bk);
						list.add(pg);
						bodyList.add(list);
					}
				}
				crossRef.setBody(bodyList);
				resultMap.put("SaleDataSet", crossRef);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static boolean isAlreadyInBody(String[][] body, String book, String page) {
		for (int i=0;i<body.length;i++) {
			if (body[i].length==4 && book.equals(body[i][2]) && page.equals(body[i][3])) {
				return true;
			}
		}
		return false;
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
			newValue = presentValue + " " +  newValue;
			newValue = StringFormats.RemoveDuplicateValues(newValue);
			resultMap.put(key, newValue);
		}
	}
}

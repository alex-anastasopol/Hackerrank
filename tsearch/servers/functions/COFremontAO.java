package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class COFremontAO {
	
	public static int ALL_NAMES_LF = 1;
	public static int FIRST_NAME_LF = 2;
	public static int ALL_NAMES_FL = 3;
	
	private static Pattern specialSuffixPattern1 = Pattern.compile("(?is)([A-Z]{2,})\\s+(II|III|IV)\\s+([A-Z]{2,}[A-Z ]*)$"); // CO Gunnison AO
	private static Pattern trusteePattern = Pattern.compile("(?is)\\s*T(?:(?:RU?)?S)?TEE?S?\\s*");
//	private static Pattern FMPattern = Pattern.compile("((\\w+(\\s+\\w)*)|((\\w\\s+)*\\w+))(\\s+(JR|SR|[IV]{2})\\b)?");

	// thirdCol in { address, legal }
	public static ResultMap parseIntermediaryRow(TableRow row, String thirdCol, long searchId) {
			
			ResultMap resultMap = new ResultMap();
			resultMap.put("OtherInformationSet.SrcType", "AO");
			resultMap.put("PropertyIdentificationSet.ParcelID", row.getColumns()[0].toPlainTextString()
					.replace("&nbsp;", "").trim());
			
			try {
				if(row.getColumnCount() == 3) {
					
					parseNames(resultMap, row.getColumns()[1].toPlainTextString().replace("&nbsp;", "").trim(), false);
					if(thirdCol.equals("address")) {
						parseAddress(resultMap, row.getColumns()[2].toPlainTextString().replace("&nbsp;", "").trim());
					} else {
						parseLegalSummary(resultMap, row.getColumns()[2].toPlainTextString().replace("&nbsp;", "").trim());
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
	
			return resultMap;
	}
	
	public static void parseNames(ResultMap resultMap, String ownerName, boolean hasAddress) throws Exception {
		
		if(ownerName == null || ownerName.matches("\\s*")) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("WRAY-MCKINSTRY POST 13");
		companyExpr.add("AMERICAN");
		companyExpr.add("DEVELOPMENTAL");
		
		// merge company names split on 2 lines
		ownerName = ownerName.replaceAll("(ADAMS\\s+MICHAEL\\s+P\\s+JR\\s+IRR\\s+SUPPORT)\\s*(TRUST)", "$1 $2");
		ownerName = ownerName.replaceAll("(BLACK\\s+RANCH\\s+FAMILY\\s+LIMITED)\\s*(PARTNERSHIP)", "$1 $2");  
		
		String[] nameLines = splitName(ownerName, companyExpr);
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = cleanName(nameLines[i]);
		}
		Vector<String> excludeWords = new Vector<String>();
		// TAX DEPARTMENT-7TH FLOOR
		excludeWords.add("TAX"); 
		excludeWords.add("DEPARTMENT");
		excludeWords.add("7TH");
		excludeWords.add("FLOOR");
		
		String[] properLines = nameLines;
		if(hasAddress) {
			properLines = GenericFunctions.removeAddressFLTR(nameLines,
					excludeWords, new Vector<String>(), 2, Integer.MAX_VALUE);
		}
		
		genericParseNames(resultMap, properLines, null, companyExpr, 
				false, ALL_NAMES_LF, -1);
		
		return;
	}
	
	private static String[] splitName(String name, Vector<String> companyExpr) {
		
//		String[] lines = name.split("(\\s*&\\s*\n\\s*)|(\\s*\n\\s*&\\s*)|(\\s*\n\\s*)");
		String[] lines = name.split("<br>");
		ArrayList<String> newLines = new ArrayList<String>();
		
		for(String line:lines) {
			if(line.indexOf("&") > -1 || line.indexOf("/") > -1) {
				if(isCompany(line, companyExpr)) {
					newLines.add(line);
				} else {
					line = cleanName(line);
					String[] parts = line.split("(\\s*&\\s*)|(/)");
					for(String part:parts) {
						newLines.add(part);
					}
				}
			} else {
				newLines.add(line);
			}
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}
	
	@SuppressWarnings("rawtypes")
	public static void genericParseNames(ResultMap resultMap, String[] nameLines, List<List> namesList, 
			Vector<String> companyExpr, boolean mergeLines, int mode, long searchId) {
		
		genericParseNames(resultMap, nameLines, namesList, companyExpr,
				new Vector<String>(), new Vector<String>(), mergeLines, mode, searchId);
	}
	
	@SuppressWarnings("rawtypes")
	public static void genericParseNames(ResultMap resultMap, String[] nameLines, List<List> namesList, 
			Vector<String> companyExpr, Vector<String> excludeLast, Vector<String> excludeFirst, boolean mergeLines, int mode, long searchId) {
		
		genericParseNames(resultMap, null, nameLines, namesList, companyExpr, 
				excludeLast, excludeFirst, mergeLines, mode, searchId);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void genericParseNames(ResultMap resultMap, String set, String[] nameLines, List<List> namesList, 
			Vector<String> companyExpr, Vector<String> excludeLast, Vector<String> excludeFirst, boolean mergeLines, int mode, long searchId) {
		
		if(namesList == null) {
			namesList = new ArrayList<List>();
		}
		
		String previousLast = "";
		for(int i = 0; i < nameLines.length ; i++) {
			
			if(StringUtils.isEmpty(nameLines[i])) {
				continue;
			}
			
			String[] names = { "", "", "", "", "", "" };
			String[] suffixes = { "", "" };
			String[] types = { "", "" };
			String[] otherTypes = { "", "" };
			String name = nameLines[i].trim();
			boolean parseFML = false;
			boolean parseLFM = false;
//			name = cleanName(name);
			if(name.indexOf("[FML]") > -1) {
				name = name.replace("[FML]", "").trim();
				parseFML = true;
			} else if(name.indexOf("[LFM]") > -1) {
				name = name.replace("[LFM]", "").trim();
				parseLFM = true;
			}
			Matcher m = trusteePattern.matcher(name);
			if(m.matches()) { // if trustee(s)
				String type = m.group();
				if(name.matches("(?is).*S")) {
					type = "TRUSTEES";
				} else {
					type = "TRUSTEE";
				}
				for(int j = namesList.size() - 1; j >= 0 ; j--) {
					List<String> nameInfo = namesList.get(j);
					if(nameInfo.get(2).equals(NameCleaner.removeUnderscore(previousLast))) {
						nameInfo.set(4, type); // set type
						if(!name.matches("(?is).*S")) {
							break;
						}
					} else {
						if(NameUtils.isCompany(nameInfo.get(2)) && StringUtils.isEmpty(previousLast)) {
							nameInfo.set(4, type); // set type
						}
						break;
					}
				}				
			} else if(name.matches("(?is)ET\\s*AL?|ET\\s*UX|ET\\s*VIR")) { // if etal/etux/etvir
				List<String> nameInfo = namesList.get(namesList.size() - 1);
				nameInfo.set(5, name); // set otherType				
			} else if(isCompany(name, companyExpr)) {
				names[2] = name;
//				types = GenericFunctions.extractAllNamesType(names);
//				otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				if(StringUtils.isEmpty(set)) {
					GenericFunctions.addOwnerNames(names, "", "", types, otherTypes, true, false, namesList);
				} else {
					String all = (String) resultMap.get("tmpAll");
					if(StringUtils.isNotEmpty(all)) {
						GenericFunctions.addOwnerNames(all, names, "", "", types, otherTypes, true, false, namesList);
					}
				}
				previousLast = "";
			} else {
				if(!previousLast.equals("") && name.matches("(([\\w-]+(\\s+\\w[.]?)*)|((\\w[.]?\\s+)*[\\w-]+))(\\s+(JR|SR|[IV]{2})\\b)?")) {
					String[] parts = name.split("\\s+");
					if(parts.length > 1) {
						for(String part : parts) {
							if(part.length() > 1 && isLastName(part, excludeLast) && !isFirstName(part, excludeFirst)) { // W W ALLRED
								previousLast = "";
							}
						}
					}
					if(mode == FIRST_NAME_LF) {
						name = name + " " + previousLast;	
					} else {
						name = previousLast + " " + name;
					}
				}				
				if((i == 0 || mode == ALL_NAMES_LF || parseLFM) && !parseFML && mode != ALL_NAMES_FL) {
					m = specialSuffixPattern1.matcher(name);
					if(m.find() && isLastName(m.group(1), excludeLast)) { // WHITE IV JAMES ROYAL (COGunnisonAO R040712)
						name = m.replaceFirst("$1 $3 $2");
					}
					names = StringFormats.parseNameNashville(name, true);
					if(name.indexOf(",") < 0 && !isLastName(names[2], excludeLast) && (isFirstName(names[2], excludeFirst) || names[2].length() == 1)) {
						if(isFirstName(names[0], excludeFirst) && names[1].length() <= 1 && name.indexOf(previousLast) < 0) { // MARY ANN, VICKIE LEIGH S
							names[1] = names[0] + " " + names[1];
							names[0] = names[2];
							names[2] = previousLast;
						} else if(!parseLFM && (isLastName(names[0], excludeLast) || isLastName(names[1], excludeLast))) {
							if(!FirstNameUtils.isMaleName(names[2]) || !FirstNameUtils.isFemaleName(names[0]) 
									|| (FirstNameUtils.isMaleName(names[0]) && !FirstNameUtils.isFemaleName(names[0]))) { // MARK SANDRA LEE (Mark is last name in this case)
								names = StringFormats.parseNameDesotoRO(name, true);
							} else if(isLastName(names[1], excludeLast) && !isFirstName(names[1], excludeFirst)) {
								names = StringFormats.parseNameDesotoRO(name, true);
							}
						} else if(names[1].matches("DE\\s+LA")) {
							names[1] = names[0];
							names[0] = names[2];
							names[2] = previousLast;
						}
					} else if(previousLast.equals(names[0]) // spouse last name is composed (SCHULZ STEVEN A AND ATWOOD SCHULZ LINETTE)
							&& isLastName(names[0], excludeLast) && !isFirstName(names[0], excludeFirst)) {
						names[2] += " " + names[0];
						names[0] = names[1];
						names[1] = "";
					} else if(!isFirstName(names[0], excludeFirst) && isLastName(names[0], excludeLast)) {
						if(isFirstName(names[2], excludeFirst) && names[1].length() == 0) { // FELIX CHESHIRE
							names = StringFormats.parseNameDesotoRO(name, true);
						}
					} else if (name.trim().matches("(?is)\\bATTN.*")){
						name = name.replaceAll("(?is)\\bATTN\\s*:?\\s*", "");
						names = StringFormats.parseNameDesotoRO(name, true);
					}
					m = Pattern.compile("(?i)(\\b|\\s)(DE(\\s+LA)?)\\b").matcher(names[1]);
					if(m.find()) {
						names[1] = m.replaceFirst("");
						names[2] = m.group(2) + " " + names[2];
					}
				} else { 
					names = StringFormats.parseNameDesotoRO(name, true);
				}
				
				names[2] = names[2].replace("_", " ");
				
				types = GenericFunctions.extractAllNamesType(names);
				otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);

				if(StringUtils.isEmpty(set)) {
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, false, false, namesList);
				} else {
					String all = (String) resultMap.get("tmpAll");
					if(StringUtils.isNotEmpty(all)) {
						GenericFunctions.addOwnerNames(all, names, suffixes[0], suffixes[1], types, otherTypes, false, false, namesList);
					}
				}
				
				previousLast = names[2].replace(" ", "_");
			}
		}
		try {
			if(StringUtils.isEmpty(set)) {
				GenericFunctions.storeOwnerInPartyNames(resultMap, namesList, true);
			} else {
				ArrayList<List> namesArrayList = new ArrayList<List>(namesList);
				resultMap.put(set, GenericFunctions.storeOwnerInSet(namesArrayList, true));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return;
	}
	
	public static void parseAddress(ResultMap resultMap, String address) {
		
		String addr = address;
		
		Matcher m = Pattern.compile("(.*) (\\d+)(-\\w+)?\\s*\\z").matcher(addr);
		if(m.find()) {
			addr = m.replaceFirst("$2 $1").trim();
			if(m.group(3) != null) {  // address unit
				addr += m.group(3).replace("-", " ");
			}
		}
		
		String strNo = StringFormats.StreetNo(addr).trim();
		String strName = StringFormats.StreetName(addr).trim();
		
		resultMap.put("PropertyIdentificationSet.StreetNo", strNo);
		resultMap.put("PropertyIdentificationSet.StreetName", strName);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void parseLegalSummary(ResultMap resultMap, String legalDesc) {
		
		String legal = legalDesc;
		legal = legal.replaceAll("(?is)\\bDESC\\s*(AS\\s+F ?OLL?S?)?:.*", "");
		
//		String sec = "";
		String twn = "";
		String rng = "";
		String lot = "";
		String blk = "";
		String unit = "";
		String phase = "";
		
		// extract subdivision name
		Matcher m = Pattern.compile("(?is)(?:(?:UNIT|BLDG|BLOCK|BLK|LOTS?|THRU|TR|SEC)\\s*[\\w-]+\\s+)+(?:OF\\s+)?([A-Z-\\s&']{2,}?)" +
				"(CONDOS?\\b|CONDOMINIUM|SUB\\b|ADD\\b|ESTATES|RANCH|CITY|PLAT|P[.]?U[.]?D[.]?|LODGE|FIL\\s+#\\s*[A-Z\\d]+|$)")
			.matcher(legal);
		if(m.find()) {
			String subName = m.group(1) + m.group(2);
			resultMap.put("PropertyIdentificationSet.SubdivisionName", subName.trim());
		}
		
		// extract sec-twn-rng
		List<List> body = new ArrayList<List>();
		m = Pattern.compile("\\bSEC(?:T|S)?\\s+(\\d+(?:[&/ ]*\\d+)*)\\s*-\\s*(\\d+[NWSE]{0,2})\\s*-\\s*(\\d+[NWSE]{0,2})\\W+").matcher(legal);
		while(m.find()) {
			String[] sections = m.group(1).split("[/&]");
			twn = m.group(2);
			rng = m.group(3);
			for(String sec:sections) {
				if(!sec.matches("\\s*")) {
					boolean foundDuplicate = false;
					for(int i = 0; i < body.size(); i++){
						List<String> list = (List<String>) body.get(i);
						if(sec.equals(list.get(0)) && twn.equals(list.get(1)) && rng.equals(list.get(2))) {
							foundDuplicate = true;
							break;
						}
					}
					if(!foundDuplicate) {
						List<String> line = new ArrayList<String>();
						line.add(sec);
						line.add(twn);
						line.add(rng);
						body.add(line);
					}
				}
			}
		}
		if(body.size() > 0) {
			try {
				GenericFunctions2.saveSTRInMap(resultMap, body);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// extract lots
		m = Pattern.compile("\\bLO?T\\s+([A-Z0-9]+)\\b").matcher(legal);
		while(m.find()) {
			lot += m.group(1) + ",";
		}
		
		m = Pattern.compile("(?<=\\bLO?TS)\\s*(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?[&,+/ ]+").matcher(legal);
		while(m.find()) {
			lot += m.group(1);
			if(m.group(3) != null) {
				lot += "-" + m.group(3);
			}
			lot += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(lot.length() > 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		
		// extract block
		m = Pattern.compile("\\bBL(?:OC)?K\\s+(\\d+|[A-Z])\\b").matcher(legal);
		while(m.find()) {
			blk += m.group(1) + ",";
		}
		
		m = Pattern.compile("(?<=\\bBLKS)\\s*(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?[&,+/; ]+").matcher(legal);
		while(m.find()) {
			blk += m.group(1);
			if(m.group(3) != null) {
				blk += "-" + m.group(3);
			}
			blk += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(blk.length() > 0) {
			blk = LegalDescription.cleanValues(blk, false, true);
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock", blk);
		}
		
		// extract unit
		m = Pattern.compile("\\bUNIT\\s+(\\w+)").matcher(legal);
		while(m.find()) {
			unit += m.group(1) + ",";
		}
		m = Pattern.compile("\\bUNITS\\s+(\\w+)\\s+(?:AND|&)\\s+(\\w+)\\s+").matcher(legal);
		while(m.find()) {
			unit += m.group(1) + "," + m.group(2);
		}
		if(unit.length() > 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			resultMap.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}
		
		//extract phase
		m = Pattern.compile("\\bPHASE\\s*(\\w+)").matcher(legal);
		while(m.find()) {
			try{
				phase += Roman.parseRoman(m.group(1)) + ",";
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		if(phase.length() > 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			resultMap.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}
	}
	
	private static String cleanName(String name) {
		
		String properName = name;
		properName = properName.replaceAll("\\bTRUSTEE(\\s+OF)\\b", "");  // TRUSTEE OF THE BERGER RANCH TRUST
		properName = NameCleaner.cleanName(properName);
		properName = properName.replaceAll("\\bET\\s*AL\\b", "");
		properName = properName.replaceAll("(?is)\\bATTN\\b\\s*:\\s*[\\w&\\s]+", "");
		properName = properName.replaceAll("\\bC/O\\b", "");
		properName = properName.replaceAll(" {2,}", " ");
		properName = properName.replaceAll("(UND)?\\s*1/2\\s*INT?\\b", "");
		properName = properName.replaceAll("\\bET\\s*AL\\b", "");
		properName = properName.replaceFirst("(?is)\\s+\\bP\\s*\\.?\\s*O\\s*\\.?\\b\\s*BOX\\b\\s*[^\\n]+", "");
		properName = properName.replaceFirst("(?is)\\bNKA\\b\\s+", " ");
		
		properName = properName.trim();
		
		return properName;
	}
	
	public static boolean isCompany(String name, Vector<String> companyExpr) { // companyExpr could also be a pattern
		
		for(int i = 0; i < companyExpr.size(); i++) {
			String expr = companyExpr.get(i);
			if(name.matches("(?is).*" + expr + ".*")) {
				return true;
			}
		}
		
		return NameUtils.isCompany(name, new Vector<String>(), true);
	}
	
	public static boolean isLastName(String name, Vector<String> excludeLast) {
		
		for(int i = 0; i < excludeLast.size(); i++) {
			String expr = excludeLast.get(i);
			if(name.indexOf(expr) > -1) {
				return false;
			}
		}
		
		return LastNameUtils.isLastName(name);
	}
	
	public static boolean isFirstName(String name, Vector<String> excludeFirst) {
		
		for(int i = 0; i < excludeFirst.size(); i++) {
			String expr = excludeFirst.get(i);
			if(name.indexOf(expr) > -1) {
				return false;
			}
		}
		
		return FirstNameUtils.isFirstName(name);
	}
}

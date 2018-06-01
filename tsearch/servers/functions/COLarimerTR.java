/**
 * 
 */
package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;

/**
 * @author vladb
 */
public class COLarimerTR {

	private static Pattern cityZipPattern = Pattern.compile("(.*?)(\\d+-\\d+)");
	
	public static ResultMap parseIntermediaryRow(TableRow row) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length >= 10) {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), cols[0].toPlainTextString().trim());
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), cols[6].toPlainTextString().trim());
			
			parseNames(resultMap, cols[2].toPlainTextString().trim(), false);
			parseAddress(resultMap, cols[5].toPlainTextString().trim());
		}
		
		
		return resultMap;
	}

	public static void parseNames(ResultMap resultMap, String ownerInfo, boolean hasAddress) {
		
		String owner = ownerInfo;
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		
		owner = fixLines(owner);
		String[] lines = owner.split("\\s*\n\\s*");
		if(hasAddress) {
			lines = GenericFunctions.removeAddressFLTR(lines,
					new Vector<String>(), new Vector<String>(), 2, Integer.MAX_VALUE);
		}
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		lines = splitOwner(lines);
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, 
				false, COFremontAO.ALL_NAMES_LF, -1);
	}

	public static void parseAddress(ResultMap resultMap, String address) {
		String addr = address;
		String regExp = "(?is)(\\d+[\\w\\d\\s-'&]+)\\s((?:[A-Z]\\s*)?\\d+)";
		
		if(StringUtils.isEmpty(addr) || addr.trim().length() < 2) {
			return;
		}
		
		String[] lines = addr.split("\n");
		if (lines.length > 0) {
			Matcher matcher = Pattern.compile(regExp).matcher(lines[0]);
			if (matcher.find() && !lines[0].matches("(?is)(\\d+[\\w\\d\\s-'&]+)\\s((?:[A-Z]\\s*)?\\d+)(?:TH|ST|RD|ND).*")) {
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(matcher.group(1)).trim());
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(matcher.group(1) + "#" + matcher.group(2)).trim());
			} else {
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(lines[0]).trim());
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(lines[0]).trim());
			}
		}
		
		if(lines.length > 1) {
			Matcher m = cityZipPattern.matcher(lines[1]);
			if(m.find()) {
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), m.group(1).trim());
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), m.group(2));
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void parseLegalSummary(ResultMap resultMap, String legalDesc) {
		String legal = legalDesc;
		
		String sec = "";
		String lot = "";
		String blk = "";
		String unit = "";
		String phase = "";
		String tract = "";
		
		// extract section/township/range
		List<List> body = new ArrayList<List>();
		Matcher m = Pattern.compile("(?:\\bSECS?|SECT|SECTIONS?|\\s)\\s*(\\d{1,3}(?:\\s*(?:[&,]|THRU)\\s*\\d+)*)-" +
				"T?(\\d+[NWSE]{0,2})-R?(\\d+[NWSE]{0,2})").matcher(legal);
		while(m.find()) {
			String crtSec = m.group(1).replaceAll("\\s*[&;]\\s*", ",");
			String[] secs = crtSec.split(",");
			for(String s : secs) {
				String[] vals = s.split("-");
				int fromVal = Integer.valueOf(vals[0].trim());
				int toVal = Integer.valueOf(vals[vals.length - 1].trim());
				for(int i = fromVal; i <= toVal; i++) {
					String section = String.valueOf(i);
					String township = m.group(2);
					String range = m.group(3);
					boolean foundDuplicate = false;
					for(int j = 0; j < body.size(); j++){
						List<String> list = (List<String>) body.get(j);
						if(section.equals(list.get(0)) && township.equals(list.get(1)) && range.equals(list.get(2))) {
							foundDuplicate = true;
							break;
						}
					}
					if(!foundDuplicate) {
						List<String> line = new ArrayList<String>();
						line.add(section);
						line.add(township);
						line.add(range);
						body.add(line);
					}
				}
			}
			
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		m = Pattern.compile("(?<=\\bSECS?|SECT|SECTIONS?)\\s*(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?[&, ]+").matcher(legal);
		while(m.find()) {
			sec += m.group(1);
			if(m.group(3) != null) {
				sec += "-" + m.group(3);
			}
			sec += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(sec.length() > 0) {
			sec = LegalDescription.cleanValues(sec, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec);
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
		
		m = Pattern.compile("(?<=\\bLO?TS)\\s*(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?[&, ]+").matcher(legal);
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
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		
		// extract block
		m = Pattern.compile("(?<=\\bBL(?:OC)?KS?)\\s+(\\d+|\\d*[A-Z])(\\s*(?:-|THRU)\\s*(\\d+|\\d*[A-Z]))?(?:[&,+; ]+|$)").matcher(legal);
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
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk);
		}
		
		// extract unit
		m = Pattern.compile("\\bUNIT\\s+([\\w-]+)").matcher(legal);
		while(m.find()) {
			unit += m.group(1) + ",";
		}
		m = Pattern.compile("(?<=\\bUNITS)\\s+(\\d+[A-Z]?|[A-Z])(\\s*(?:-|THRU)\\s*(\\d+[A-Z]?|[A-Z]))?(?:[&,+ ]+|$)").matcher(legal);
		while(m.find()) {
			unit += m.group(1);
			if(m.group(3) != null) {
				unit += "-" + m.group(3);
			}
			unit += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(unit.length() > 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
		}
		
		//extract phase
		m = Pattern.compile("\\bPHASE\\s*(\\w+)").matcher(legal);
		while(m.find()) {
			if(m.group(1).matches("\\d+")) {
				phase += m.group(1);
			} else {
				try{
					phase += Roman.parseRoman(m.group(1)) + ",";
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		if(phase.length() > 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		}
		
		// extract tract
		m = Pattern.compile("(?<=\\bTRACTS?)\\s+(\\d+|[A-Z])(\\s*(?:-|THRU|AND)\\s*(\\d+|[A-Z]))?(?:[&,; ]+|$)").matcher(legal);
		while(m.find()) {
			tract += m.group(1);
			if(m.group(3) != null) {
				tract += "-" + m.group(3);
			}
			tract += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(tract.length() > 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
		}
	
		
	}
	
	public static String[] splitOwner(String[] lines) {

		ArrayList<String> newLines = new ArrayList<String>();
		
		for(String line:lines) {
			String[] parts = line.split("(?<=\\D)/(?=\\D|$)");
			if(parts.length == 2 && NameUtils.isNotCompany(parts[0])
					&& parts[1].matches("(?is).*\\b(TRT|TRST|TRUST)\\b.*")) {
				newLines.add(line);
			} else if (parts.length > 1 && parts[0].trim().indexOf(" ") < 0 
					&& NameUtils.isCompany(parts[1])){ // GLEASON/WESTSIDE INVESTMENTS, LLC (.50) ; BERTHOUD PARTNERS FIRST TOWER, LLC (.50)
				newLines.add(line);
			} else {			
				for(String part : parts) {
					if(StringUtils.isNotEmpty(part)) {
						newLines.add(part.trim());
					}
				}
			}
			
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}

	// cleans a single name
	public static String cleanName(String name) {
		
		String properName = name;
		properName = properName.replaceAll("(?is)\\d+-\\d+-\\d+", "");
		properName = properName.replaceAll("(?is)NO\\s+\\d+", "");
		properName = properName.replaceAll("(?is),\\s*$", "");
		
		properName = genericCleanName(properName, new Vector<String>(), "/");
		
		
		return properName;
	}
	
	// cleans a single name
	public static String genericCleanName(String name, Vector<String> otherExpr, String separator) {
		
		String properName = name;
		properName = properName.replaceAll(" {2,}", " ").trim();
		properName = properName.replaceAll("\\A,\\s*", "");
		
		
		properName = properName.replaceAll("(?is)ATTORNEY AT LAW", "");
		properName = properName.replaceAll("(?is)ESQ", ""); // Esquire (courtesy title placed after an attorney's surname)
		properName = properName.replaceAll("(?is)[.]\\d+( EACH)?", "");
		properName = properName.replaceAll("(?is)\\d+/\\d+\\s*INT\\b", "");
		properName = properName.replaceAll("(?is)DECLARATION OF TRUST", "");
		properName = properName.replaceAll("(?is)OFFICE ADMINISTRATION", "");
		properName = properName.replaceAll("(?is)\\bFBO\\b", "");
		properName = properName.replaceAll("(?is)\\bFOR\\s+THE\\s+BENEFIT\\s+OF\\b", "");
		properName = properName.replaceAll("(?is)\\bIRA\\b", "");
		properName = properName.replaceAll("(?is)\\bPMB\\s+\\d+", "");
		properName = properName.replaceAll("(?is)\\b(CO\\s*)?CUSTODIAN\\b", "");
		properName = properName.replaceAll("(?is)\\bHEIRS(\\s+OF)?\\b", "");
		properName = properName.replaceAll("(?is)\\b(AS\\s+)JT(RS)?", "");
		properName = properName.replaceAll("(?is)\\b(AS\\s+)TIC\\b", "");
		properName = properName.replaceAll("\\bPR\\b", ""); // Personal Representative
		
		// replaces with separator
		properName = properName.replaceAll("(?is)[(].*?[)]", separator); // ADAMS, DONALD K (1/2 INT)
		properName = properName.replaceAll("(?is)\\bAKA\\b", separator);
		properName = properName.replaceAll("(?is)\\bC/O\\b", separator);
		properName = properName.replaceAll("(?is)(,\\s*)?PERS?[.]?\\s+REP[.]?(\\s+OF)?\\b", separator); // Personal Representative
		
		return properName.trim();
	}
	
	private static String fixLines(String owner) {
		
		String properOwner = owner;
		properOwner = properOwner.replaceAll("(?is)(?:,\\s*)?(?:AS|SUCCESSOR)?\\s*(?:CO-?)?(?<!BOARD\\sOF\\s)" +
				"(TRUSTEES?|TSTES?)\\s*(?:,|OF)?", "\n$1\n"); // isolate TRUSTEE
		properOwner = properOwner.replaceAll("(?is)\n\\s*OF\\b", " OF");
		properOwner = properOwner.replaceAll(",\\s*(JR|SR|[IV]{2})\\b", " $1");
		properOwner = properOwner.replaceAll("[(].*?[)]", "");		
		properOwner = properOwner.replaceAll("(?is)\n\\s*LLC\\b", " LLC");
		properOwner = properOwner.replaceAll("(?is)\\b(FIRST)\\s*\n\\s*(TOWER)\\b", "$1 $2");
		properOwner = properOwner.replaceAll(";", "\n");
		
		Matcher m = Pattern.compile("(?is)THE([\\w-\\s]+?)\n([\\w-\\s]+?)TRUST").matcher(properOwner);
		while(m.find()) { // THE JIM AND MARGIE BECKER \n AB LIVING TRUST
			if(NameUtils.isNotCompany(m.group(1))) {
				properOwner = m.replaceFirst("THE$1$2TRUST");
				m.reset(properOwner);
			}
		}
		
		m = Pattern.compile("(?is)/(\\w+)\\s*,\\s*(\\w+)\\s*,").matcher(properOwner);
		while(m.find()) { // KENNETH/RHONDA, COX, JOSEPH
			if(LastNameUtils.isNotLastName(m.group(1)) && FirstNameUtils.isFirstName(m.group(1)) 
					&& LastNameUtils.isLastName(m.group(2))) {
				properOwner = m.replaceFirst("/$1/$2,");
				m.reset(properOwner);
			}
		}
		
		properOwner = properOwner.replaceAll("\\s*\n\\s*", "\n");
		
		return properOwner;
	}
}

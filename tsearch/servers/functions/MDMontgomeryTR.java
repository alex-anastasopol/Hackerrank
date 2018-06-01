package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class MDMontgomeryTR {

	public static ResultMap parseIntermediaryRow(TableRow row) {
		ResultMap resultMap = new ResultMap();
			
		if(row.getColumnCount() == 6) {
			if(row.getColumns()[1].toPlainTextString().trim().equals("TLR")) {
				resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TU");
			} else {
				resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			}
			
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), row.getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim());
		}
		
		if(row.getColumnCount() == 3) {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			
			String address = row.getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
			address = address.replaceFirst("([\\d-]+)$", "#$1"); // mark the unit
			
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
			resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			
			String legal = row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
			ro.cst.tsearch.servers.functions.MDMontgomeryTR.parseLegal(resultMap, legal);
			
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), row.getColumns()[2].toPlainTextString().trim());
		}
		
		return resultMap;
	}
	
	public static void parseOwners(ResultMap resultMap, String ownerName) {
		String owner = ownerName;
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		
		owner = owner.replaceAll("\\b(?:[,(]\\s*)?(?:AS\\s+)?(?:\\bCO-?\\s*)?(?<!BOARD\\sOF\\s)(T(?:(?:RU?)?S)?TEE?S?)(\\s*([)]|OF))?\\b", "\n$1\n");
		owner = owner.replaceAll("(?is)\\bET\\s*(AL|UX|VIR)\\b", "\nET$1\n");
		String[] lines = owner.split("\n");
		lines = splitAndFixLines(lines, companyExpr);
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, false, COFremontAO.FIRST_NAME_LF, -1);
	}
	
	private static String[] splitAndFixLines(String[] lines, Vector<String> companyExpr) {
		ArrayList<String> newLines = new ArrayList<String>();
		
		for(String line : lines) {
			boolean trustee = false;
			if(line.matches(".*\\s+TR$")) {
				line = line.replaceFirst("(.*)\\s+TR$", "$1");
				trustee = true;
			}
			
			if(COFremontAO.isCompany(line, companyExpr)) {
				newLines.add(line);
			} else {
				String[] parts = line.split("(?is)&|\\bAND\\b");
				for(String part : parts) {
					newLines.add(part.trim());
				}
			}
			
			if(trustee) {
				newLines.add("TRUSTEE");
			}
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}
	
	public static void parseLegal(ResultMap resultMap, String legalDesc) {
		String legal = legalDesc;
		
		if(StringUtils.isEmpty(legal)) {
			return;
		}

		String lot = "";
		String blk = "";
		String unit = "";
		String phase = "";
		String tract = "";
		String sec = "";
		
		// extract lots
		Matcher m = Pattern.compile("(?is)(?<=\\b(?:OUT)?LO?TS?)\\s*(?:PT\\s*)?(\\d+[A-Z]?|\\s+[A-Z])(\\s*(?:-|THRU)\\s*(\\d+[A-Z]?|[A-Z]))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			lot += m.group(1).trim();
			if(m.group(3) != null) {
				lot += "-" + m.group(3);
			}
			lot += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(lot.length() > 0) {
			String oldLot = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName());
			if(oldLot != null) {
				lot = oldLot + "," + lot;
			}
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		
		// extract block
		m = Pattern.compile("(?is)\\bBL(?:OC)?K\\s+(\\w+)\\b").matcher(legal);
		while(m.find()) {
			blk += m.group(1) + ",";
			legal = m.replaceFirst("");
			m.reset(legal);
		}
		m = Pattern.compile("(?is)(?<=\\bBL(?:OC)?KS?)\\s+(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&,; ]+|-|$)").matcher(legal);
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
			blk = (String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName()) 
					+ "," + LegalDescription.cleanValues(blk, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk);
		}
		
		// extract unit
		m = Pattern.compile("(?is)(?<=\\bUNITS?)\\s+(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&,; ]+|-|$)").matcher(legal);
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
		m = Pattern.compile("(?is)(?<=\\bPH(?:ASE)?)\\s+(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&,; ]+|-|$)").matcher(legal);
		while(m.find()) {
			phase += m.group(1);
			if(m.group(3) != null) {
				phase += "-" + m.group(3);
			}
			phase += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(phase.length() > 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		}
		
		// extract tract
		m = Pattern.compile("(?is)(?<=\\bTRACTS?)\\s+(\\d+|[A-Z])(\\s*(?:-|THRU|AND)\\s*(\\d+|[A-Z]))?(?:[&,; ]+|-|$)").matcher(legal);
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
		
		//extract phase
		m = Pattern.compile("(?is)(?<=\\bSEC(?:TION)?)\\s+(\\d+[A-Z]?)(\\s*(?:-|THRU)\\s*(\\d+[A-Z]?))?(?:[&,; ]+|-|$)").matcher(legal);
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
		
	}
}

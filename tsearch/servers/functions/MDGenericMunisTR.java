/**
 * 
 */
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
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author Vladimir
 *
 */
public class MDGenericMunisTR {
	
	public static ResultMap parseIntermediaryRow(TableRow row) {
		ResultMap map = new ResultMap();
		map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), row.getColumns()[3].toPlainTextString().trim());
		map.put(TaxHistorySetKey.YEAR.getKeyName(), row.getColumns()[4].toPlainTextString().trim());
		
		String address = row.getColumns()[0].toPlainTextString().trim() + 
				" # " + row.getColumns()[1].toPlainTextString().trim();
		map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
		map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
		
		ro.cst.tsearch.servers.functions.MDGenericMunisTR.parseOwners(map, row.getColumns()[2].toPlainTextString().replaceAll("(?is)&amp;", "&").trim());
		
		return map;
	}

	public static void parseOwners(ResultMap resultMap, String ownerName) {
		String owner = ownerName;
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		
		owner = owner.replaceAll("(?is)\\bET\\s*(AL|UX|VIR)\\b", "\nET$1\n");
		owner = owner.replaceAll("(?is)-SOLE\\b", "");
		owner = owner.replaceAll("(?is)\\bJ/T\\b", "");
		String[] lines = owner.split("\n");
		lines = splitAndFixLines(lines, companyExpr);
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, false, COFremontAO.ALL_NAMES_LF, -1);
	}

	private static String[] splitAndFixLines(String[] lines, Vector<String> companyExpr) {
		ArrayList<String> newLines = new ArrayList<String>();
		String prevLast = "";
		Pattern trusteePattern = Pattern.compile("(?is)(.*?)(?:-|\\b)(?:[,(-]\\s*)?(?:AS\\s+)?(?:\\bCO-?\\s*)?(?<!BOARD\\sOF\\s)(?:T(?:(?:RU?)?S)?TEE?S?)(?:\\s*(?:[)]|OF))?\\b(.*)");
		Pattern nameSuffix = Pattern.compile("(?is)(.*?),?\\s*\\b(JR|SR|DR|[IV]{2,}|[III]{2,}|\\d+(?:ST|ND|RD|TH))\\b(.*)");
		boolean trustee = false;
		
		for(String line : lines) {
			if(StringUtils.isEmpty(line)) {
				continue;
			}
			if(trustee) {
				newLines.add("TRUSTEE");
				trustee = false;
			}
			if(COFremontAO.isCompany(line, companyExpr)) {
				String[] parts = line.split("&|AND");
				if(parts.length == 2) {
					// GEASEY LUTHER T JR &
					// SHIRLEY F LIFE EST & PAMELA L
					if(!COFremontAO.isCompany(parts[0], companyExpr)) {
						if(parts[0].trim().matches("[A-Z]{2,}(\\s+[A-Z])?")) {
							if(COFremontAO.isFirstName(parts[0].trim().split("\\s+")[0], new Vector<String>())) {
								newLines.add(prevLast + " " + parts[0].trim());
								newLines.add(parts[1].trim());
								continue;
							}
						}
					} else if(!COFremontAO.isCompany(parts[1], companyExpr)) {
						if(parts[1].trim().matches("[A-Z]{2,}(\\s+[A-Z])?")) {
							if(COFremontAO.isFirstName(parts[1].trim().split("\\s+")[0], new Vector<String>())) {
								newLines.add(parts[0].trim());
								newLines.add(prevLast + " " + parts[1].trim());
								continue;
							}
						}
					} 
				}
				newLines.add(line);
			} else {
				Matcher m = trusteePattern.matcher(line);
				if(m.matches()) {
					line = m.replaceFirst(m.group(1) + m.group(2));
					trustee = true;
				}
				
				m = nameSuffix.matcher(line);
				if(m.matches()) {
					if(StringUtils.isNotEmpty(m.group(3)) && !m.group(3).contains("&")) {
						line = m.group(1) + m.group(3) + " " + m.group(2);
					}
				}
				
				String[] parts = line.split("&|\\bAND\\b");
				for(String part : parts) {
					part = part.trim();
					if(part.matches("WF")) {
						//wife
						continue;
					}
					String firstToken = part.split("\\s+")[0];
					if(part.contains(prevLast) && !firstToken.contains(prevLast)) {
						newLines.add("[FML]" + part);
					} else {
						newLines.add(part);
					}
				}
				
				String firstToken = line.split("\\s+")[0];
				if(!COFremontAO.isFirstName(firstToken, new Vector<String>())) {
					prevLast = firstToken;
				}
			}
		}
		
		if(trustee) {
			newLines.add("TRUSTEE");
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}

	public static void parseLegal(ResultMap resultMap, String legalDesc) {
		String legal = legalDesc;
		
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		if(StringUtils.isEmpty(legal)) {
			return;
		}

		String lot = "";
		String blk = "";
		String sec = "";
		String unit = "";
		
		// extract lots
		Matcher m = Pattern.compile("(?is)(?<=\\b(?:PT)?LO?TS?)\\s*(\\d+[A-Z]?)(\\s*(?:-|THRU|TO)\\s*(\\d+[A-Z]?))?(?:[&; ]+|-|$)").matcher(legal);
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
		m = Pattern.compile("(?is)\\bBL?(?:OC)?K\\s+(\\w+)\\b").matcher(legal);
		while(m.find()) {
			blk += m.group(1) + ",";
			legal = m.replaceFirst("");
			m.reset(legal);
		}
		m = Pattern.compile("(?is)(?<=\\bBL?(?:OC)?KS?)\\s+(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&,; ]+|-|$)").matcher(legal);
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
		
		// extract section
		m = Pattern.compile("(?is)\\bSE?CT?(?:ION)?(\\s+[A-Z]+|\\s*\\d+[A-Z]?)\\b").matcher(legal);
		while(m.find()) {
			sec += m.group(1) + ",";
			legal = m.replaceFirst("");
			m.reset(legal);
		}
		if(sec.length() > 0) {
			sec = LegalDescription.cleanValues(sec, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), blk);
		}
		
		// extract unit
		m = Pattern.compile("(?is)(?<=\\bUNITS?)\\s+(\\d+[A-Z]*)(\\s*(?:-|THRU)\\s*(\\d+[A-Z]*))?(?:[&,; ]+|-|$)").matcher(legal);
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
	}

}

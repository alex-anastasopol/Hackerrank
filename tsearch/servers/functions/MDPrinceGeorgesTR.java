/**
 * 
 */
package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class MDPrinceGeorgesTR {

	public static ResultMap parseIntermediaryRow(TableRow row) {
		ResultMap resultMap = new ResultMap();
		
		if(row.getColumnCount() == 4) {
			if(row.getColumns()[2].toPlainTextString().trim().equals("TAX SALE DETAILS")) {
				resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TU");
			} else {
				resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			}
			
			String taxPeriod = row.getColumns()[1].toPlainTextString().trim();
			if(taxPeriod.matches("\\d{2}/\\d{2}")) {
				String year = taxPeriod.substring(3);
				year = year.replaceFirst("^0", "");
				int _year = Integer.parseInt(year);
				
				if(_year < 20) {
					_year = 2000 + _year;
				} else {
					_year = 1900 + _year;
				}
				
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), String.valueOf(_year));
			}
		}
		if(row.getColumnCount() == 10) {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), 
					row.getColumns()[1].toPlainTextString().replaceFirst("^0+", "").replaceAll("(?is)&nbsp;", "").trim());
			resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), 
					row.getColumns()[3].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim() + " " + 
					row.getColumns()[4].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), row.getColumns()[9].toPlainTextString().trim());
			
			String owner = row.getColumns()[7].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim();
			parseOwners(resultMap, owner);
		}
		
		return resultMap;
	}

	public static void parseOwners(ResultMap resultMap, String ownerName) {
		String owner = ownerName;
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("NVR MS CAVALIER OAK CREEK OWNE");
		companyExpr.add("SAXONY SQ &KNGSLY HALL HMWN AS");
		
		owner = owner.replaceAll("\\b(?:[,(]\\s*)?(?:AS\\s+)?(?:\\bCO-?\\s*)?(?<!BOARD\\sOF\\s)(T(?:(?:RU?)?S)?TEE?S?)(\\s*([)]|OF))?\\b", "\n$1\n");
		owner = owner.replaceAll("(?is)\\bET\\s*(AL|UX|VIR)\\b", "\nET$1\n");
		String[] lines = owner.split("\n");
		lines = splitAndFixLines(lines, companyExpr);
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, false, COFremontAO.FIRST_NAME_LF, -1);
	}
	
	private static String[] splitAndFixLines(String[] lines, Vector<String> companyExpr) {
		ArrayList<String> newLines = new ArrayList<String>();
		
		for(String line : lines) {
			if(COFremontAO.isCompany(line, companyExpr)) {
				newLines.add(line);
			} else {
				line = line.replaceAll("(?is)\\bHRS\\b", "");
				String[] parts = line.split("(?is)&|\\bAND\\b");
				for(String part : parts) {
					newLines.add(part.trim());
				}
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
		
		// extract lots
		Matcher m = Pattern.compile("(?is)(?<=\\b(?:OUT)?LO?TS?)\\s*(?:PT\\s*)?(\\d+[A-Z]?|\\s+[A-Z])(\\s*(?:-|THRU|TO)\\s*(\\d+[A-Z]?|[A-Z]))?(?:[&,.; ]+|-|$)").matcher(legal);
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
			if(StringUtils.isNotEmpty(oldLot)) {
				lot = oldLot + "," + lot;
			}
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
	}
}

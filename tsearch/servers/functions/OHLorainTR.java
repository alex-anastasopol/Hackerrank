
package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class OHLorainTR {

	public static ResultMap parseIntermediaryRow(TableRow row) {
		ResultMap resultMap = new ResultMap();
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), row.getColumns()[0].toPlainTextString().trim());
		
		parseNames(resultMap, row.getColumns()[1].toPlainTextString());
		parseAddress(resultMap, row.getColumns()[2].toPlainTextString());
		
		return resultMap;
	}

	public static void parseNames(ResultMap resultMap, String ownerNames) {
		if(StringUtils.isEmpty(ownerNames)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		
		String owners = ownerNames;
		owners = fixLines(owners);
		
		String[] lines = owners.split("\n");
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		lines = splitAndFixLines(lines, companyExpr);
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, false, COFremontAO.ALL_NAMES_LF, -1);
	}

	private static String fixLines(String owner) {
		String properOwner = owner;
		properOwner = properOwner.replaceAll("\\b(?:[,(]\\s*)?(?:AS\\s+)?(?:\\bCO-?\\s*)?(?<!BOARD\\sOF\\s)(T(?:(?:RU?)?S)?TEE?S?)(\\s*([)]|OF(?: THE)?))?\\b", "\n$1\n");
		properOwner = properOwner.replaceAll("(?is)\\bET\\s*(AL|UX|VIR)\\b", "\nET$1\n");
		properOwner = properOwner.replaceAll("(?is)\\bOF THE\\b", "\n");
		properOwner = properOwner.replaceAll("(?is)\\b(TRUST)\\b", "$1\n");
		properOwner = properOwner.replaceAll("\\s*\n\\s*", "\n").trim();
		
		return properOwner;
	}
	
	private static String cleanName(String name) {
		String properName = name;
		properName = properName.replaceAll("@[(]\\d+[)]", "");
		properName = properName.replaceAll("(?is)\\bFKA\\b.*(&|$)", "$1");
		properName = properName.replaceAll(" {2,}", " ");
		properName = properName.trim();
		
		return properName;
	}
	
	private static String[] splitAndFixLines(String[] lines, Vector<String> companyExpr) {
		ArrayList<String> newLines = new ArrayList<String>();
		Pattern p = Pattern.compile("(?is)(\\w+)\\s+(\\w+)\\s+(.*)\\bTRUST");
		String trustLast = "";
		String trustFirst = "";
		
		for(String line : lines) {
			if(COFremontAO.isCompany(line, companyExpr)) {
				Matcher m = p.matcher(line);
				if(m.matches()) {
					// WHITE CLARA TRUST CLARA TRUSTEE
					if(LastNameUtils.isLastName(m.group(1)) && FirstNameUtils.isFirstName(m.group(2))) {
						trustLast = m.group(1);
						trustFirst = m.group(2);
					}
				}
				String[] parts = line.split("(?is)&|\\bAND\\b", 2);
				if(parts.length == 2) { // BLACK RIV DEV CO & UNI CIR LAND CO
					if(COFremontAO.isCompany(parts[0], companyExpr) && COFremontAO.isCompany(parts[1], companyExpr)) {
						newLines.add(parts[0].trim());
						newLines.add(parts[1].trim());
						continue;
					}
				}
				newLines.add(line);
			} else {
				String[] parts = line.split("(?is)&|\\bAND\\b");
				for(String part : parts) {
					if(!StringUtils.isEmpty(part)) {
						part = part.trim();
						// WHITE CLARA TRUST CLARA TRUSTEE
						if(part.startsWith(trustFirst) && !part.contains(trustLast)) {
							part = trustLast + " " + part;
							trustLast = "";
							trustFirst = "";
						}
						newLines.add(part);
					}
				}
			}
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}
	
	public static void parseAddress(ResultMap resultMap, String locationAddress) {
		String[] address = locationAddress.trim().split("\\s*\n\\s*\n\\s*");
		String streetNo = "";
		String streetName = "";
		String cityStateZip = "";
		
		if(address[0].matches("\\d+")) {
			streetNo = address[0].replaceAll("\\s+", " ");
			streetName = address[1].replaceAll("\\s+", " ");
			if(address.length > 2) {
				cityStateZip = address[2].replaceAll("\\s+", " ");
			}
		} else {
			streetName = address[0].replaceAll("\\s+", " ");
			if(address.length > 1) {
				cityStateZip = address[1].replaceAll("\\s+", " ");
			}
		}
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		
		if(!"".equals(cityStateZip) && cityStateZip.matches("\\w+\\s+OH\\s+\\d+")) {
			String[] cityStateZipArray = cityStateZip.split("\\s+");
			resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), cityStateZipArray[0]);
			resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), cityStateZipArray[2]);
		}
	}

	public static void parseLegal(ResultMap resultMap, String legal) {
		if(StringUtils.isEmpty(legal)) {
			return;
		}
		
		String sub = "";
		String phase = "";
		
		// extract subdivision
		Matcher m = Pattern.compile("^(?is)([A-Z\\s]*?)\\b(?:(CONDO(?:MINIUM)?S?|ESTATES|SUBD?[.]?(?:IVISION)?|ADD)|(FRNT|\\d+|EX|FROM|AC))(\\b|\\d)").matcher(legal);
		if(m.find()) {
			sub = m.group(1);
			if(m.group(2) != null) {
				sub += m.group(2);
			}
			sub = sub.trim();
			if(!sub.matches("[A-Z]?")) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), sub);
			}
		}
		
		//extract phase
		m = Pattern.compile("\\bPH(?:ASE)?\\s*(\\d+)").matcher(legal);
		while(m.find()) {
			phase += m.group(1) + ",";
		}
		if(phase.length() > 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		}
	}
}

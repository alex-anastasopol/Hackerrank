/**
 * 
 */
package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

/**
 * @author vladb
 *
 */
public class TNSumnerTR {
	

	public static ResultMap parseIntermediaryRow(TableRow row) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put("TaxHistorySet.Year", row.getColumns()[5].toPlainTextString().replace("&nbsp;", "").trim());
		resultMap.put("tmpOwner", row.getColumns()[1].toPlainTextString().replace("&nbsp;", "").trim());
		resultMap.put("tmpAddress", row.getColumns()[3].toPlainTextString().replace("&nbsp;", "").trim());
		resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), 
			row.getColumns()[4].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
		
		parseNames(resultMap);
		parseAddress(resultMap);
		
		return resultMap;
		
	}

	public static void parseNames(ResultMap resultMap) {
		
		String owner = (String) resultMap.get("tmpOwner");
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("PERSONAL");
		
		if(owner.matches("(\\w+)\\s+(\\w+)\\s+\\2A?\\s+\\w+")) {
			owner = owner.replaceAll("(\\w+)\\s+(\\w+)\\s+(\\w+)\\s+(\\w+)", "$1 $2 AND $3 $4");
		}
		Matcher ma1 = Pattern.compile("(?s)(\\w+)\\s+(\\w+)\\s+(\\w+)\\s+(\\w+)(?:\\s+(\\w+))?").matcher(owner);		//MANN JASON EDWARD JENNIFER DAWN
		if (ma1.matches()) {
			String p1 = ma1.group(1);
			String p2 = ma1.group(2);
			String p3 = ma1.group(3);
			String p4 = ma1.group(4);
			String p5 = StringUtils.defaultString(ma1.group(5));
			NameFactory nf =  NameFactory.getInstance();
			if (nf.isMale(p2)&&nf.isMale(p3)&&nf.isFemale(p4)&&nf.isFemale(p5)) {
				owner = p1 + " " + p2 + " " + p3 + " AND " + p4 + " " + p5 + " " + p1;  
			} else if (nf.isFemale(p2)&&nf.isFemale(p3)&&nf.isMale(p4)&&nf.isMale(p5)) {
				owner = p1 + " " + p2 + " " + p3 + " AND " + p4 + " " + p5 + " " + p1;  
			} else if (nf.isMale(p1) && nf.isMale(p2) && (nf.isFemale(p3) || nf.isFemale(p4))) {
				// e.g. WALKER JEREMY TATE RAELENE for PID 099L-B-014.00 (should be WALKER JEREMY TATE ETUX RAELENE)
				owner = p1 + " " + p2 + " " + p3 + " ETUX " + p4;
			}
		}
		Matcher ma2 = Pattern.compile("(?s)(\\w+)\\s+(\\w+)\\s+(\\w+)").matcher(owner);							//RANDOLPH RICKY LISA
		if (ma2.matches()) {
			String p1 = ma2.group(1);
			String p2 = ma2.group(2);
			String p3 = ma2.group(3);
			NameFactory nf =  NameFactory.getInstance();
			if (nf.isMaleOnly(p2)&&nf.isFemaleOnly(p3)) {
				owner = p1 + " " + p2 + " AND " + p3 + " " + p1;  
			} else if (nf.isFemaleOnly(p2)&&nf.isMaleOnly(p3)) {
				owner = p1 + " " + p2 + " AND " + p3 + " " + p1;  
			}
		}
		
 		owner = fixNames(owner);

		// e.g. YOUNG ELVIE E YOUNG EVA for PID 126O-C-030.00
		owner = owner.replaceFirst("(?is)^\\s*([A-Z-]{2,})\\s+([A-Z-]+(?:\\s+[A-Z-]+)?)\\s+\\1\\s+([A-Z-]+(?:\\s+[A-Z-]+)?)\\s*$", "$1 $2 & $1 $3");
		if (owner.split("&").length == 2) {
			owner = owner.replaceAll("\\s+", " ");
			String[] name = owner.split("\\s*&\\s*");

			if (name[0].split(" ")[0].equalsIgnoreCase(name[1].split(" ")[0])) {
				owner = owner.replaceFirst("&", "\n[LFM]");
			}
		}

		// e.g. YOUNGBLOOD ROBERT J JANICE M for PID 159E-C-012.00
		if (owner.matches("(?i)\\s*([A-Z-.]+(\\s+|$)){5,6}")) {
			owner = owner.replaceFirst("(([A-Z-.]+\\s+){3})", "$1\n");
		}

		String[] lines = owner.split("\n");
		lines = splitAndFixOwner(lines);
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, 
				false, COFremontAO.FIRST_NAME_LF, -1);
	}
	
	private static String fixNames(String names) {
		
		String properNames = names;
		properNames = properNames.replace("%", "\n");
		properNames = properNames.replaceAll("\\bC/O\\b", "\n");
		properNames = properNames.replaceAll("\\bA/?K/?A\\b", "\n");
		properNames = properNames.replaceAll("\\bDRIVER:", "\n"); // S C JOHNSON & SON INC DRIVER: G SMITH
		properNames = properNames.replaceAll("ATTN?:?\\s+.*", "");
		properNames = properNames.replaceAll("\\bTRS\\b", "TRUSTEES");
		properNames = properNames.replaceAll("\\bCO-TR\\b", "TRUSTEE");
		properNames = properNames.replaceAll("\\bTR\\b", "TRUSTEE");
		properNames = properNames.replaceAll("\\bCO\\s*-?\\s*(T(?:(?:RU?)?S)?TEE?S?)\\b", "$1"); // prevent parsing CO TRUSTEE as company
		properNames = properNames.replaceAll("(?is)(?:,\\s*)?(?:AS|SUCCESSOR)?\\s*(?:CO-?)?(?<!BOARD\\sOF\\s)" +
				"(T(?:(?:RU?)?S)?TEE?S?)\\s*(?:,|OF|FOR)?\\b", "\n$1\n"); // isolate TRUSTEE
		properNames = properNames.replaceAll("\\b(ET\\s*(AL|UX|VIR))\\b", "\n$1\n");
		properNames = properNames.replaceFirst("(?is)^(\\w{2,}\\s+\\w+(?:\\s+\\w+)?\\s+(?:JR|SR|II|III|IV))\\s+(\\w+(?:\\s+\\w+)?)$", "$1 & $2");
		
		
		return properNames;
	}
	
	private static String cleanName(String name) {
		
		String properName = name;
		properName = COLarimerTR.genericCleanName(properName, new Vector<String>(), "");
		return properName;
	}
	
	public static String[] splitAndFixOwner(String[] lines) {

		ArrayList<String> newLines = new ArrayList<String>();
		
		for(String line:lines) {
			if(StringUtils.isEmpty(line)) {
				continue;
			}
			String[] parts = line.split("&|\\bAND\\b");
			if(NameUtils.isCompany(line)) {
				if(line.matches("(?is).*\\bBANK\\b.*")) {
					for(String part : parts) {
						if(!StringUtils.isEmpty(part)) {
							newLines.add(part);
						}
					}
				} else {
					newLines.add(line);
				}
			} else {
				for(String part : parts) {
					if(StringUtils.isNotEmpty(part)) {
						newLines.add(part);
					}
				}
			}
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}
	
	public static void parseAddress(ResultMap resultMap) {
		
		String address = (String) resultMap.get("tmpAddress");
		if(StringUtils.isEmpty(address)) {
			return;
		}
		
		resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address).trim());
		resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address).trim());
	}
	
}

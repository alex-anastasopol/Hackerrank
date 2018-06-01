/**
 * 
 */
package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class FLGulfTR {

	public static void parseNames(ResultMap resultMap, boolean hasAddress, long searchId) {
		
		String owner = (String) resultMap.get("tmpOwnerInfo");
		
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("LAGOON");
		companyExpr.add("PARKWAY");
		
		owner = owner.replaceAll("@@", "\n");
		owner = fixNames(owner);
		String[] lines = owner.split("\\s*\n\\s*");
		if(hasAddress) {
			lines = GenericFunctions.removeAddressFLTR(lines,
					new Vector<String>(), new Vector<String>(), 1, Integer.MAX_VALUE);
		}
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		lines = splitAndFixOwner(lines);
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, 
				false, COFremontAO.FIRST_NAME_LF, -1);
	}

	private static String[] splitAndFixOwner(String[] lines) {
	
		ArrayList<String> newLines = new ArrayList<String>();
		
		for(String line:lines) {
			if(StringUtils.isEmpty(line)) {
				continue;
			}
			line = line.trim();
			if(line.indexOf(" ") < 0 && newLines.size() > 0 && !line.matches("T(?:(?:RU?)?S)?TEE?S?|ETAL|ETUX|ETVIR")) { // name split on 2 rows
				String prevLine = newLines.remove(newLines.size() - 1);
				line = prevLine + " " + line;
				newLines.add(line);
				continue;
			}
			if(NameUtils.isCompany(line)) {
				String[] parts = line.split("&|\\bAND\\b|\\bOR\\b");
				if(parts.length == 2 && NameUtils.isNotCompany(parts[0]) && parts[1].trim().matches("TREASURE")) {
					newLines.add(parts[0]);
					newLines.add("_" + parts[1].trim());
				} else {
					newLines.add(line);
				}
			} else {
				String[] parts = line.split("&|\\bAND\\b|\\bOR\\b");
				for(String part : parts) {
					if(StringUtils.isEmpty(part)) {
						continue;
					}
					newLines.add(part);
				}
			}
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}

	private static String fixNames(String names) {
		
		String properNames = names;
		properNames = properNames.replaceAll("ET\\s*(AL|UX|VIR)", "\n$0\n");
		properNames = properNames.replaceAll("\\b(?:[,(]\\s*)?(?:AS\\s+)?(?:\\bCO-?\\s*)?(?<!BOARD\\sOF\\s)(T(?:(?:RU?)?S)?TEE?S?)(\\s*([)]|OF))?\\b", "\n$1\n");
		properNames = properNames.replaceAll("\n\\s*(REV(OCABLE)?\\s+TRUST)", " $1");
		properNames = properNames.replaceAll("\n(\\s*COMPANY +LLC\\s*\n)", " $1");
		properNames = properNames.replaceAll("\\bTHE\\s*\n", "THE ");
		
		
		return properNames;
	}
	
	private static String cleanName(String name) {
		
		String properName = name;
		properName = properName.replaceAll("\\A\\s*&\\s*", "");
		properName = properName.replaceAll("\\s*&\\s*\\z", "");
		if(NameUtils.isNotCompany(properName)) {
			properName = properName.replaceAll("[/#]\\d{5}(TR)?", ""); //MELAINE C SHAVER/38177 ET AL, FERRELL MONTY C IRA#34036TR
		}
		properName = COLarimerTR.genericCleanName(properName, new Vector<String>(), "&");
		
		return properName;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void parseLegalSummary(ResultMap resultMap, long searchId) {
		
		String legal = (String) resultMap.get("PropertyIdentificationSet.PropertyDescription");
		
		if(StringUtils.isEmpty(legal)) {
			return;
		}
		
		legal = legal.replaceAll("\\s*\n\\s*", " ");
//		legal = legal.replaceAll(",\\s*UNREC[.]", "");
		legal = legal.replaceAll("\\d*[.]\\d+\\s+ACRES", "");
		legal = legal.replace(";", " ;");
		legal = GenericFunctions1.replaceNumbers(legal);
		
		String sub = "";
		String lot = "";
		String blk = "";
		String unit = "";
		String phase = "";
		String tract = "";
		
		// extract section/township/range
		List<List> body = new ArrayList<List>();
		Matcher m = Pattern.compile("S\\s*(\\d+(?:\\s*(?:[&,-]|THRU)\\s*\\d+)*)\\s+" +
				"T\\s*(\\d+)\\s*R\\s*(\\d+)").matcher(legal);
		while(m.find()) {
			String crtSec = m.group(1).replaceAll("\\s*&\\s*", ",");
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
		if(body.size() > 0) {
			try {
				GenericFunctions2.saveSTRInMap(resultMap, body);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// extract subdivision name
		m = Pattern.compile("\\A([A-Z\\s.,'&]+?)(?:(CONDO(?:MINIUM)?S?|ESTATES|SUBD?(?:IVISION)?)|(PB|LOT|PHASE|TRACT|UNIT|BEING|[,(]\\s*UNREC))").matcher(legal);
		if(m.find()) {
			sub = m.group(1);
			if(m.group(2) != null) {
				sub += m.group(2);
			} 
		}
		if(!StringUtils.isEmpty(sub)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionName", sub.trim());
		}
		
		// extract lots
		m = Pattern.compile("(?<=\\bLO?TS?)\\s+([A-Z]?-?\\d+)(\\s*(?:-|THRU)\\s*([A-Z]?-?\\d+))?(?:[&, ]+|$)").matcher(legal);
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
		m = Pattern.compile("(?<=\\bBL(?:OC)?KS?)\\s+([A-Z]?-?\\d+|[A-Z])(\\s*(?:-|THRU)\\s*([A-Z]?-?\\d+|[A-Z]))?(?:[&, ]+|$)").matcher(legal);
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
		m = Pattern.compile("\\bUNIT\\s+(?:#|NO[.]?)?\\s*([\\w-]+)").matcher(legal);
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
			resultMap.put("PropertyIdentificationSet.SubdivisionUnit", unit);
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
			resultMap.put("PropertyIdentificationSet.SubdivisionPhase", phase);
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
			resultMap.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}
	
		// extract cross refs from legal description
		try {
		   List<List> bodyCR = new ArrayList<List>();
		   m = Pattern.compile("\\b(?:ORB?)\\s*(\\d+)\\s*(?:PGS?|/)\\s*(\\d+[A-Z]?)(\\s*(?:-|THRU|&)\\s*(\\d+[A-Z]?))?(?:[&, ]+|$)").matcher(legal);
		   
		   while (m.find()){
			   try {
				   int fromPage = Integer.valueOf(m.group(2));
				   int toPage = fromPage;
				   
				   if(m.group(4) != null) {
					   String fromPageStr = m.group(2);
					   String toPageStr = m.group(4);
					   toPageStr = fromPageStr.substring(0, fromPageStr.length() - toPageStr.length()) + toPageStr; // ORB 137/851-55
					   toPage = Integer.valueOf(toPageStr);
				   }
				   if(toPage < fromPage) {
					   int aux = toPage;
					   toPage = fromPage;
					   fromPage = aux;
				   }
				   for(int i = fromPage; i <= toPage; i++) {
					   List<String> line = new ArrayList<String>();		   
					   line.add(m.group(1));
					   line.add(String.valueOf(i));
					   line.add("");
					   bodyCR.add(line);
				   }
				   
			   } catch (Exception e) {
//				   e.printStackTrace();
				   List<String> line = new ArrayList<String>();		   
				   line.add(m.group(1));
				   line.add(m.group(2));
				   line.add("");
				   bodyCR.add(line);
			   }
			   
			   legal = m.replaceFirst("ORB " + m.group(1) + "PG ");
			   m.reset(legal);
		   } 
		   
			try {
				Matcher m1 = Pattern.compile("\\b(?:PB)\\s*(\\d+)\\s*(?:PGS?|/)\\s*(\\d+[A-Z]?)(\\s*(?:-|THRU|&)\\s*(\\d+[A-Z]?))?(?:[&, ]+|$)").matcher(legal);
				if (m1.find()) {
					resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), m1.group(1).trim());
					resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), m1.group(2).replace("&", ", ").replaceAll("(?ism)THRU","-").replaceAll("\\s+"," ").trim());
					legal = legal.replace(m1.group()," ").replaceAll("\\s+"," ").trim();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		   
		   if (!bodyCR.isEmpty()) {		  		   		   
			   String [] header = {"Book", "Page", "InstrumentNumber"};		   
			   Map<String,String[]> map = new HashMap<String,String[]>();		   
			   map.put("Book", new String[]{"Book", ""});
			   map.put("Page", new String[]{"Page", ""});
			   map.put("InstrumentNumber", new String[]{"InstrumentNumber", ""});
			   
			   ResultTable cr = new ResultTable();	
			   cr.setHead(header);
			   cr.setBody(bodyCR);
			   cr.setMap(map);		   
			   resultMap.put("CrossRefSet", cr);
		   }
		   
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void parseAddressGulf(ResultMap m, long searchId){
//		String addr = (String) m.get("tmpAddress");
		
//		if(org.apache.commons.lang.StringUtils.isNotBlank(addr)){
//			m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr.replaceAll("\\s+", " ").trim());
//	
//			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), org.apache.commons.lang.StringUtils.defaultString(StringFormats.StreetNo(addr)));
//			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), org.apache.commons.lang.StringUtils.defaultString(StringFormats.StreetName(addr)));
//		}
	}
}

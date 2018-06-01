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
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class FLWakullaTR {
	
	private static Pattern pidPattern = Pattern.compile("(\\w{2})(\\w{2})(\\w{3})(\\w{3})(\\w{5})(\\w{3})");
	
	public static void parseNames(ResultMap resultMap, boolean hasAddress, long searchId) {
		
		String owner = (String) resultMap.get("tmpOwnerInfo");
		
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("CITYBANK");
		
		Vector<String> excludeWords = new Vector<String>();
		excludeWords.add("800");
		excludeWords.add("STATE");
		excludeWords.add("HWY");
		excludeWords.add("121");
		excludeWords.add("BYPASS");
		excludeWords.add("SUITE");

		owner = owner.replaceAll("@@\\s*\\d+\\s+[A-Z]+[^@]*", "");//00-00-007-000-06303-000wakulla tr
		owner = owner.replaceAll("@@", "\n");
		owner = owner.replaceAll("(?is)(,\\s*[A-Z]+\\s+[A-Z]+\\s*&\\s*[A-Z]+)\\s*,", "$1 ");//00-00-007-000-06303-000wakulla tr
		owner = fixNames(owner);
		String[] lines = owner.split("\\s*\n\\s*");
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
			if(NameUtils.isCompanyNamesOnly(lines[i], new Vector<String>())) {
				lines[i] = lines[i] + "_"; // prevent some strange behavior of the remove address function
			}
		}
		if(hasAddress) {
			lines = GenericFunctions.removeAddressFLTR(lines,
					excludeWords, new Vector<String>(), 1, Integer.MAX_VALUE);
		}
		for(int i = 0; i < lines.length; i++) {
			lines[i] = NameCleaner.removeUnderscore(lines[i]);
		}
		
		lines = processName(lines, companyExpr, "[&,]|\\bAND\\b");
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, 
				false, COFremontAO.ALL_NAMES_LF, -1);
	}

	//merge people names split on 2 lines, split company names placed on the same line
	public static String[] processName(String[] lines, Vector<String> companyExpr, String separatorRegex) {

		ArrayList<String> newLines = new ArrayList<String>();
		String crtLine = "";
		
		for(String line:lines) {
			if(StringUtils.isEmpty(line)) {
				continue;
			}
			line = " " + line + " ";
			String[] parts = line.split(separatorRegex);
			String trusteeRegEx = "\\s*(T(?:(?:RU?)?S)?TEE?S?|ETAL|ETUX|ETVIR)\\s*";
			if(line.matches(trusteeRegEx)) {
				newLines.add(crtLine);
				newLines.add(line);
				crtLine = "";
			}
			else if(COFremontAO.isCompany(line, companyExpr)) {
				if(crtLine.matches("\\w{2,}(\\s+\\w)*") && line.matches(".*\\sTRUST\\s*")) {
					newLines.add(crtLine + line);
				} else if (line.matches("\\s*(REVOCABLE\\s+)?(LIVING\\s+)?TRUST\\s*")) {
					for(int i = newLines.size() - 1; i >= 0; i--) {
						if(!COFremontAO.isCompany(newLines.get(i), companyExpr) 
								&& !newLines.get(i).matches(trusteeRegEx)) {
							newLines.add(newLines.get(i) + line);
							break;
						}
					}
				} else if (line.trim().indexOf(" ") < 0
						&& !newLines.get(newLines.size() - 1).matches(trusteeRegEx)) {
					newLines.add(newLines.remove(newLines.size() - 1) + line);
				} else {
					if(crtLine.length() > 0) {
						newLines.add(crtLine);
					}
					newLines.add(line);
				}
				crtLine = "";
			} else {
				String firstPart = parts[0].trim();
				if((firstPart.matches("\\w{2,}(\\s+\\w)*") && crtLine.length() > 0) || crtLine.matches("\\w{2,}(\\s+\\w)*")) {
					newLines.add(crtLine + " " + firstPart);
					crtLine = "";
				} else {
					if(crtLine.length() > 0) {
						newLines.add(crtLine);
					}
					crtLine = firstPart;
				}
				for(int i = 1; i < parts.length; i++) {
					if(crtLine.length() > 0) {
						newLines.add(crtLine);
					}
					crtLine = parts[i];
				}			
			}
		}
		if(crtLine.length() > 0) {
			newLines.add(crtLine);
		}else{		// 00-00-121-080-11995-000 JOHNSON GALEN B & KAREN K OF	JOHNSON TRUST
			if (newLines.size() == 1){
				String string = newLines.get(0);
				String[] split = string.split(separatorRegex);
				if (split.length >2){
					split = string.split("\\bOF\\b");
					newLines.remove(0);
					for (String string2 : split) {
						newLines.add(string2);
					}
				}
			}
		}
	
	return newLines.toArray(new String[newLines.size()]);
}

	private static String fixNames(String names) {
		
		String properNames = names;
		properNames = properNames.replaceAll("\\b(FOR\\s+)THE\\s+BENEFIT\\s+OF\\b", "");
		properNames = properNames.replaceAll("ET\\s*(AL|UX|VIR)", "\n$0\n");
		properNames = properNames.replaceAll("\\b(?:[,(]\\s*)?(?:AS\\s+)?(?:\\bCO-?\\s*)?(?<!BOARD\\sOF\\s)(T(?:(?:RU?)?S)?TEE?S?)(\\s*([)]|OF|FOR))?\\b", "\n$1\n");
//		properNames = properNames.replaceAll("\n\\s*(REV(OCABLE)?)\\s+(TRUST)", " $1 $2");
		properNames = properNames.replaceAll("\n(\\s*COMPANY +LLC\\s*\n)", " $1");
		properNames = properNames.replaceAll("\\bTHE\\s*\n", "THE ");
		properNames = properNames.replaceAll("\\s+OF\\s+", " OF ");
		properNames = properNames.replaceAll("(INTERNAL)\\s+(IMPROVEMENT)", "$1 $2"); //BOARD OF TRUSTEES OF INTERNAL IMPROVEMENT TRUST FUND OF THE STATE OF FLORIDA
		properNames = properNames.replaceAll("(?is)\\bUNDER.*DECLARA.?TION.*NO[.]\\s*\\d+", ""); // FISHER GRAY F AS TRUSTEE UNDERTHE FISHER DECLARARTION OF NO.1
		properNames = properNames.replaceAll("\n\\s*(BAPTIST\\s+CHURCH)", " $1");
		properNames = properNames.replaceAll("\\bDBA\\b", "AND");
		properNames = properNames.replaceAll("\\bBOARD\\sOF\\sTRU?ST(|EES?)\\s(OF|FOR)\\b", "");
							
		Matcher m = Pattern.compile("(?is)THE\\s.*?\\sTRUST(\\s+DA?TE?D\\s+[\\d/]+)?(\\s+AGREEMENT)?").matcher(properNames);
		if(m.find()) {
			properNames = m.replaceFirst(m.group().replaceAll("\\s+", " "));
		}
		
		return properNames;
	}
	
	private static String cleanName(String name) {
		
		String properName = name;
		properName = properName.replaceAll("\\A\\s*&\\s*", "");
		properName = properName.replaceAll("\\s*&\\s*\\z", "");
		properName = properName.replaceAll("\\s*/\\s*\\z", "");
		properName = properName.replaceAll("\\d+%\\s*IN\\b", "");
		properName = properName.replaceAll("(?is)[(].*?[)]", " ");
		properName = properName.replaceAll("[(]", " ");
		properName = properName.replaceAll("[)]", " ");
		properName = properName.replaceAll("\\bJTRS\\b", "");
		properName = properName.replaceAll("\\bAS\\s+JTFRS\\b", "");
		
		properName = properName.replaceAll("\\bC/O\\b", "[FML]");
		
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
		String pid = (String) resultMap.get("PropertyIdentificationSet.ParcelID");
		Matcher m = Pattern.compile("(\\w{2})-(\\w{2})-(\\w{3})-(\\w{3})-(\\w{5})-(\\w{3})").matcher(pid);
		List<List> body = new ArrayList<List>();
		// if sec-twn-rng are contained in pid
		if(m.matches()) {
			if(!m.group(1).equals("00") || !m.group(2).equals("00")) {
				List<String> line = new ArrayList<String>();
				line.add(m.group(1).replaceAll("^0+", ""));
				line.add(m.group(2).replaceAll("^0+", ""));
				line.add(m.group(3).replaceAll("^0+", ""));
				body.add(line);
			}
		}
		
		m = Pattern.compile("(\\d+(?:\\s*(?:[&,-]|THRU)\\s*\\d+)*)" +
				"[-\\s]+(\\d+[NWSE]{1,2})[-\\s]+(\\d+[NWSE]{1,2})").matcher(legal);
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
		
		// extract unit like 1ST UNIT
		m = Pattern.compile("(\\d+)(?:ST|ND|TH)\\s+UNIT\\b").matcher(legal);
		while(m.find()) {
			unit += m.group(1) + ",";
			legal = m.replaceFirst("");
		}
		
		// extract subdivision name
		m = Pattern.compile("\\A([A-Z\\s.,'&]+?)(?:(CONDO(?:MINIUM)?S?|ESTATES|SUBD?[.]?(?:IVISION)?)|(PB|LOT|SECTION|BLOCK|PHASE|TRACT|UNIT|BEING|[,(]\\s*UNREC))").matcher(legal);
		if(m.find()) {
			sub = m.group(1);
			if(m.group(2) != null) {
				sub += m.group(2);
			} 
		}
		if(StringUtils.isEmpty(sub)) {
			m = Pattern.compile("\\b(?:LOTS?|OF)([A-Z\\s.,'&]+?)(SUBD?(?:IVIS(?:ION)?)?)").matcher(legal);
			if(m.find()) {
				sub = m.group(1)+m.group(2);
			}
		}
		if(!StringUtils.isEmpty(sub)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionName", sub.trim());
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
		   m = Pattern.compile("\\b(?:OR|DB)\\s*(\\d+)\\s*-?\\s*(?:P[.]?|-)\\s*(\\d+[A-Z]?)(\\s*(?:-|THRU)\\s*(\\d+[A-Z]?))?(?:[&, ]+|$)").matcher(legal);      	   
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
			   
			   legal = m.replaceFirst("OR " + m.group(1) + "P ");
			   m.reset(legal);
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
}

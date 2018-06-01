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


import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 */
public class FLWashingtonTR {
	
	private static Pattern trusteePattern = Pattern.compile("(?is)(.*?)(?:,\\s*)?(?:AS\\s+)?" +
			"(T(?:(?:RU?)?S)?TEE?S?)(?:\\s+FOR\\s)?(.*)");

	public static ResultMap parseIntermediaryRow(TableRow row) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		resultMap.put("PropertyIdentificationSet.ParcelID", row.getColumns()[0].toPlainTextString()
				.replaceAll("&nbsp;", "").trim());
		
		parseNames(resultMap, row.getColumns()[1].toPlainTextString().replaceAll("&nbsp;", "").trim(), false);
		
		return resultMap;
	}

	public static void parseNames(ResultMap resultMap, String ownerInfo, boolean hasAddress) {
		
		String owner = ownerInfo;
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		
		owner = fixNames(owner);
		String[] lines = owner.split("\\s*\n\\s*");
		if(hasAddress) {
			lines = GenericFunctions.removeAddressFLTR(lines,
					new Vector<String>(), new Vector<String>(), 2, Integer.MAX_VALUE);
		}
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		lines = splitAndFixOwner(lines);
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanAndFix(lines[i]);
		}
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, 
				false, COFremontAO.FIRST_NAME_LF, -1);
		
	}
	
	private static String cleanAndFix(String ownerName) {
		
		String name = ownerName.trim();
		if(NameUtils.isNotCompany(name)) {
			Matcher m = Pattern.compile("(\\w)\\s+(\\w{2,})(\\s+\\w{2,}\\s+\\w)").matcher(name);
			if(m.matches()) {
				if(LastNameUtils.isLastName(m.group(1) + m.group(2))) {
					name = m.replaceFirst("$1$2$3");
				}
			}
		} else {
			Matcher m = Pattern.compile(".*\\s(19|20)\\d{2}-.*MTG\\s+CERT").matcher(name); // SABR 2004-OP1 MTG CERT
			if(m.find()) {
				name = "";
			}
		}
		name = cleanName(name);
		
		return name;
	}
	
	private static String cleanName(String name) {
		
		String properName = name;
		properName = properName.replaceAll("%", "&");
		properName = properName.replaceAll("\\bICG\\b", "");
		properName = properName.replaceAll("\\bTRST[.]", "TRST");
		properName = properName.replaceAll("\\bCO-?\\s*(T(?:(?:RU?)?S)?TEE?S?)", "$1");
		properName = properName.replaceAll("\\A\\s*&\\s*", "");
		properName = properName.replaceAll("\\s*&\\s*\\z", "");
		properName = COLarimerTR.genericCleanName(properName, new Vector<String>(), "&");
		
		return properName;	
	}

	private static String fixNames(String ownerInfo) {
		
		String owner = ownerInfo;
		owner = owner.replaceAll("(?is)(T((RU?)?S)?TEE?S?)(?:\\s+FOR\\b)", "$1\n");
		owner = owner.replaceAll("(?is)\\b(ET\\s*AL)\\b", "\n$1\n"); // isolate ETAL
		owner = owner.replaceAll("/?CUSTOD(IAN)?/?", ""); // EQUITY TRUST CUSTODIAN/TRUSTEE
		if(owner.matches("(?is).*BISHOP.*DIOCESE.*")) {
			owner = owner.replaceAll("(?is)BISHOP", "");
		}
		
		return owner;
	}
	
	// SMITH MERWIN G TRUSTEE ESTATE will be split in SMITH MERWIN G ESTATE and SMITH MERWIN G TRUSTEE
	public static String[] splitAndFixOwner(String[] lines) {

		ArrayList<String> newLines = new ArrayList<String>();
		Matcher m;
		
		for(String line:lines) {
			if(StringUtils.isEmpty(line)) {
				continue;
			}
			String[] parts = line.split("&");
			if(NameUtils.isCompany(line)) {
				m = trusteePattern.matcher(line);
				ArrayList<String> tmpLines = new ArrayList<String>();
				if(m.find() && !line.matches("(?is).*B(OAR)?D\\s+OF\\s+TRUSTEES.*")) {
					tmpLines.add(m.group(1) + m.group(3)); // remove TRUSTEE from company name
					for(String part : parts) {
						if(StringUtils.isEmpty(part)) {
							continue;
						}
 						Matcher m1 = trusteePattern.matcher(part);
						if(m1.find() && !part.matches("(?is).*B(OAR)?D\\s+OF\\s+TRUSTEES.*")) {
							if(NameUtils.isNotCompany(m1.group(1))) {
								tmpLines.add(m1.group(1));
								tmpLines.add(m1.group(2));							
							}
						}
					}
					newLines.addAll(tmpLines);
					if(tmpLines.size() == 1) {
						newLines.add(m.group(2));
					}
				} else {
					newLines.add(line);					
				}
				
			} else {
				for(String part : parts) {
					if(StringUtils.isEmpty(part)) {
						continue;
					}
					m = trusteePattern.matcher(part);
					if(m.find() && !part.matches("(?is).*B(OAR)?D\\s+OF\\s+TRUSTEES.*")) {
						newLines.add(m.group(1) + m.group(3));
						newLines.add(m.group(2));
					} else {
						newLines.add(part);
					}
				}
			}
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}
	
	@SuppressWarnings({ "unchecked" })
	public static void parseLegalSummary(ResultMap resultMap, String legalSummary) {
			
		String legal = legalSummary;
		legal = legal.replaceAll("\\s*\n\\s*", " ");
		
		String sub = "";
		String lot = "";
		String blk = "";
		String unit = "";
		String phase = "";
		String tract = "";
		
		// extract section/township/range
		Matcher m = Pattern.compile("(?is)\\A(\\d+)\\s+(\\d+)\\s+(\\d+)\\s").matcher(legal);
		if(m.find()) {
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", m.group(1));
			resultMap.put("PropertyIdentificationSet.SubdivisionTownship", m.group(2));
			resultMap.put("PropertyIdentificationSet.SubdivisionRange", m.group(3));
		}
		
		// extract subdivision name
		m = Pattern.compile("(?is)\\A([A-Z\\s]+)UNIT").matcher(legal);
		if(m.find()) {
			sub = m.group(1);
		}
		if(StringUtils.isEmpty(sub)) {
			
			m = Pattern.compile("(?is)(?:(?:LOTS?|TRACT|BLK)\\s+(?:\\d*[A-Z]?-?\\d+)\\s*(?:[&,]\\s*\\d*[A-Z]?-?\\d+\\s*)*(?:,\\s*)?)+" +
				"([A-Z\\d\\s]*?[A-Z]{2}[A-Z\\d\\s]*?)(ADD(ITION)?|ESTATES|UNREC|UNIT|LESS|SUB(DI?V?(ISION)?)?|[$])")
				.matcher(legal);
			if(m.find()) {
				String part1 = m.group(1).replaceAll("UNREC|OF", "");
				String part2 = m.group(2).replaceAll("UNREC|UNIT|LESS", "");
				if(!StringUtils.isEmpty(part1)) {
					sub = part1 + part2;
				}
			}
		}
		if(!StringUtils.isEmpty(sub)) {
			resultMap.put("PropertyIdentificationSet.SubdivisionName", sub.trim());
		}
		
		// extract lots
		m = Pattern.compile("(?<=\\bLO?TS?)\\s+([A-Z]?-?\\d+)(\\s*(?:-|THRU)\\s*([A-Z]?-?\\d+)(?:\\s+INC)?)?(?:[&, ]+|$)").matcher(legal);
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
		m = Pattern.compile("\\bUNIT\\s+#?([\\w-]+)").matcher(legal);
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
		   m = Pattern.compile("\\b(?:ORB?)\\s*(\\d+)\\s+(?:P)?\\s*(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&, ]+|$)").matcher(legal);      	   
		   while (m.find()){
			   
			   int fromPage = Integer.valueOf(m.group(2));
			   int toPage = fromPage;
			   
			   if(m.group(4) != null) {
				   toPage = Integer.valueOf(m.group(4));
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
				   if(!COGunnisonAO.isDuplicateRef(resultMap, line)) {
					   bodyCR.add(line);
				   }
			   }
			   legal = m.replaceFirst("OR" + m.group(1) + " ");
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


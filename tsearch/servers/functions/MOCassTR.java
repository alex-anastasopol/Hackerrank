package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;


/**
 * @author vladb
*/

public class MOCassTR {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 6) {
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), cols[1].toPlainTextString().trim());
			
			parseNames(resultMap, cols[2].toPlainTextString().trim(), false);
		}
		return resultMap;
	}
		
	public static void parseNames(ResultMap resultMap, String ownerInfo, boolean hasAddress) {
		
		String owner = ownerInfo;
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("BANKLIBERTY");
		
		String[] lines = owner.split("\\s*\n\\s*");
		if(hasAddress) {
			lines = GenericFunctions2.removeAddressFLTR(lines, new Vector<String>(), new Vector<String>(), 2, Integer.MAX_VALUE, true);
		}
		lines = fixAndSplit(lines);
		
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, 
				false, COFremontAO.FIRST_NAME_LF, -1);
	}
	
	private static String cleanName(String name) {
		
		String properName = name.trim();
		properName = properName.replaceAll("[-]$", "");
		
		properName = COLarimerTR.genericCleanName(properName, new Vector<String>(), "");
		
		return properName;
	}

	private static String[] fixAndSplit(String[] lines) {
		
		ArrayList<String> properLines = new ArrayList<String>();
		String mergedLines = "";
		for(String line : lines) {
			mergedLines += line + " ";
		}
		
		mergedLines = mergedLines.replaceAll("\\bTR\\b", "\nTRUSTEE\n");
		mergedLines = mergedLines.replaceAll("\\bET\\s*(AL|UX|VIR)\\b", "\nET$1\n");
		
		String[] tmpLines = mergedLines.split("\\s*(%|\n)\\s*");
		for(String line : tmpLines) {
			if(NameUtils.isCompany(line)) {
				properLines.add(line.trim());
			} else {
				String[] parts = line.split("&");
				for(String part : parts) {
					if(!part.matches("\\s*WF\\s*")) { // WF means wife
						properLines.add(part.trim());
					}
				}
			}
		}
		
		return properLines.toArray(new String[properLines.size()]);
	}

	@SuppressWarnings("rawtypes")
	public static void parseLegalDescription(ResultMap resultMap, String legalDescription) {
		
		String legal = legalDescription;
		if(StringUtils.isEmpty(legal)) {
			return;
		}
	
		String lot = "";
		String blk = "";
		String unit = "";
		String phase = "";
		String tract = "";
		Matcher m = null;
		
		// extract cross refs
		m = Pattern.compile("(\\d+)/(\\d+)").matcher(legal);
		List<List> bodyCR = new ArrayList<List>();
		ResultTable cr = (ResultTable) resultMap.get("CrossRefSet");
		if(cr != null) {
			String[][] oldBody = cr.getBody();
			for(String[] row : oldBody) {
				if(row.length == 3) {
					List<String> line = new ArrayList<String>();
					line.add(row[0]);
					line.add(row[1]);
					line.add(row[2]);
					bodyCR.add(line);
				}
			}
		}
		while(m.find()) {
			List<String> line = new ArrayList<String>();
			line.add(m.group(1));
			line.add(m.group(2));
			line.add("");
			bodyCR.add(line);
			
			legal = m.replaceFirst("");
			m.reset(legal);
		}
		if (!bodyCR.isEmpty()){		  		   		   
			String [] header = {"Book", "Page", "InstrumentNumber"};		   
			Map<String,String[]> map = new HashMap<String,String[]>();		   
			map.put("Book", new String[]{"Book", ""});
			map.put("Page", new String[]{"Page", ""});
			map.put("InstrumentNumber", new String[]{"InstrumentNumber", ""});
			
			try {
				cr = new ResultTable();	
				cr.setHead(header);
				cr.setBody(bodyCR);
				cr.setMap(map);		   
				resultMap.put("CrossRefSet", cr);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		// extract lots
		m = Pattern.compile("(?<=\\bLO?TS?)\\s*(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&,; ]+|$)").matcher(legal); // LT34
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
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock", blk);
		}
		
		// extract unit
		m = Pattern.compile("\\bUNIT\\s+(\\w+)\\s+(\\d+)?").matcher(legal);
		while(m.find()) {
			if(m.group(1).matches("[A-Z]") && m.group(2) != null) {
				unit += m.group(1) + " " + m.group(2) + ",";
			} else {
				unit += m.group(1) + ",";
			}
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
		
	}

	public static void parseAddress(ResultMap resultMap, String address) {
		
		String addr = address;
		if(StringUtils.isEmpty(addr)) {
			return;
		}
		
		String[] lines = addr.split("\\s*\n\\s*");
		if(lines.length > 0) {
			String line1 = lines[0].trim(); // street name and number
			
			if(!StringUtils.isEmpty(line1) && !line1.equals("UNKNOWN")) {
				String strNo = StringFormats.StreetNo(line1).trim();
				String strName = StringFormats.StreetName(line1).trim();
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), strNo);
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), strName);
			}
			
			if(lines.length > 1) {
				String line2 = lines[1].trim(); // city and zip
				
				if(!StringUtils.isEmpty(line2)) {
					Matcher m = Pattern.compile("([A-Z\\s]+)\\s*,\\s*MO\\s*(\\d+)").matcher(line2);
					if(m.matches()) {
						resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), m.group(1));
						resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), m.group(2));
					}
				}
			}
		}
	}
	
}

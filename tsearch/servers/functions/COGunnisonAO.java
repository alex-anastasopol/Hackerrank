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
import org.htmlparser.tags.TableColumn;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 */
public class COGunnisonAO {
	
	private static final Pattern lotEnumWithLettersPattern =  Pattern
		.compile("(?<=\\bLO?TS?)\\s+(\\d+|\\d*[A-Z])(\\s*(?:-|THRU)\\s*" +
			"(\\d+|\\d*[A-Z]))?(?:\\s*,\\s*ALL\\s*|[&,+;) ]+|$)+");
	private static final Pattern lotEnumOnlyNumbersPattern = Pattern
		.compile("(?<=\\bLO?TS?)\\s+(\\d+)(\\s*(?:-|THRU)\\s*" +
			"(\\d+))?(?:\\s*,\\s*ALL\\s*|[&,+;) ]+|$)+");
	
	public final static String[] CITIES = {"ALMONT", "CRESTED BUTTE", "CRYSTAL", "GUNNISON", "MARBLE",
		"MOUNT CRESTED BUTTE" ,"OHIO CITY", "PARLIN", "PITKIN", "PITTSBURG", 
		"POWDERHORN", "SAPINERO", "SOMERSET", "TINCUP" };

	public static ResultMap parseIntermediaryRow(TableRow row, TableRow header, long searchId) {
	
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "AO");

		for(int i = 0; i < header.getColumnCount(); i++) {
			TableColumn cell = header.getColumns()[i];
			if(cell.toPlainTextString().matches("(?is).*Account\\s+Number.*")) {
				String pin = row.getColumns()[i].toPlainTextString().replace("&nbsp;", "").trim();
				pin = pin.replaceFirst("\\s+.+", "").trim();
				resultMap.put("PropertyIdentificationSet.ParcelID", pin);
			} else if(cell.toPlainTextString().matches("(?is).*Owner.*")) {
				parseNames(resultMap, row.getColumns()[i].toPlainTextString().replace("&nbsp;", "").trim());
			} else if( (cell.toPlainTextString().matches("(?is).*Address.*") && !cell.toPlainTextString().matches("(?is).*Mailing\\s+Address.*")) ||
					   (cell.toPlainTextString().matches("(?is).*Situs.*")) ) {
				String address = row.getColumns()[i].toPlainTextString().replace("&nbsp;", "").trim();
				address = address.replaceFirst("^,$", "");
				parseAddress(resultMap, address);
			} else if(cell.toPlainTextString().matches("(?is).*Legal\\s+Information.*")) {
				parseLegalSummary(resultMap, row.getColumns()[i].toPlainTextString().replace("&nbsp;", "").trim());
			} else if(cell.toPlainTextString().matches("(?is).*Subdivision.*")) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), 
					row.getColumns()[i].toPlainTextString().replace("&nbsp;", "").trim());
			} else if(cell.toPlainTextString().matches("(?is).*Condo.*")) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), 
					row.getColumns()[i].toPlainTextString().replace("&nbsp;", "").trim());
			} else if(cell.toPlainTextString().matches("(?is).*Unit.*")) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), 
					row.getColumns()[i].toPlainTextString().replace("&nbsp;", "").replaceAll("(?is)\\bUNIT\\b", "").trim().replaceAll("^#", ""));
			}
		}
		
		return resultMap;
	}
	
	public static void parseNames(ResultMap resultMap, String name) {
		
		String ownerName = name;
		if(StringUtils.isEmpty(ownerName)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		
		ownerName = ownerName.replaceAll("\\s*\n\\s*", "\n").trim();
		String[] nameLines = ownerName.split("(?is)<br>");
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = cleanName(nameLines[i]);
		}
		
		COFremontAO.genericParseNames(resultMap, nameLines, null, companyExpr, false, COFremontAO.ALL_NAMES_LF, -1);
	}
	
	@SuppressWarnings("deprecation")
	private static String cleanName(String name) {
		
		String properName = name;
		properName = NameCleaner.cleanName(properName);
		
		return properName.trim();
	}
	
	public static void parseAddress(ResultMap resultMap, String address) {
		
		if (StringUtils.isEmpty(address)) {
			return;
		}
		
		String addr = address;
		
		String split[] = addr.split(",");
		if (split.length==2) {
			addr = split[0].trim();
			String city = split[1].trim();
			city = city.replaceAll("\\bMT\\b", "MOUNT");
			for (int i=0;i<CITIES.length;i++) {
				if (city.equalsIgnoreCase(CITIES[i])) {
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), CITIES[i]);
					break;
				}
			}
		}
		
		addr = addr.replaceAll("(?is)\\bUNIT\\s*", "#");
		
		String strNo = StringFormats.StreetNo(addr).trim();
		String strName = StringFormats.StreetName(addr).trim();
		
		resultMap.put("PropertyIdentificationSet.StreetNo", strNo);
		resultMap.put("PropertyIdentificationSet.StreetName", strName);
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
		String bldg = "";
		String platBook = "";
		String platPage = "";
		
		// extract section/township/range
		List<List> body = new ArrayList<List>();
		Matcher m = Pattern.compile("(?:\\bSECS?|SECT|SECTIONS?)\\s+(\\d+(?:\\s*(?:[&,;-]|THRU)\\s*\\d+)*)(?:[,\\s]+|$)" +
				"(\\d+[NWSE]{1,2})?(\\d+[NWSE]{1,2})?").matcher(legal);
		while(m.find()) {
			String crtSec = m.group(1).replaceAll("\\s*[&;]\\s*", ",");
			if(!StringUtils.isEmpty(m.group(2)) && !StringUtils.isEmpty(m.group(3))) { // has township and range
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
			} else {
				sec += crtSec + ",";
			}
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(sec.length() > 0) {
			sec = LegalDescription.cleanValues(sec, false, true);
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", sec);
		}
		if(body.size() > 0) {
			try {
				GenericFunctions2.saveSTRInMap(resultMap, body);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// extract lots
		m = lotEnumWithLettersPattern.matcher(legal);
		int prevStart = -1;
		while(m.find()) {
			if(prevStart != m.start(1)) { // new lot enumeration (R003087)
				if(m.group(1).matches("\\d+")) {
					m = lotEnumOnlyNumbersPattern.matcher(legal);
				} else {
					m = lotEnumWithLettersPattern.matcher(legal);
				}
				m.find();
			}
			prevStart = m.start(1);
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
		
		// extract building
		m = Pattern.compile("(?is)\\bBLDG\\s+(\\d+|[A-Z])").matcher(legal);
		while(m.find()) {
			bldg += m.group(1);
			bldg += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(bldg.length() > 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
		}
		
		// extract plat book, plat page
		m = Pattern.compile("(?is)\\bB(\\d+)\\s+P(\\d+)").matcher(legal);
		if (m.find()) {
			platBook = m.group(1);
			platPage = m.group(2);
			legal = m.replaceFirst(" ");
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), platBook);
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), platPage);
		}
		
		// extract cross refs from legal description
		try {
		   List<List> bodyCR = new ArrayList<List>();
		   m = Pattern.compile("\\bB(\\d+)\\s+P?(\\d+)(\\s*-\\s*(\\d+))?(?:[&, ]+|$)").matcher(legal);      	   
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
				   if(!isDuplicateRef(resultMap, line)) {
					   bodyCR.add(line);
				   }
			   }
			   legal = m.replaceFirst("B" + m.group(1) + " ");
			   m.reset(legal);
		   } 
		   
		   m = Pattern.compile("#(\\d{6})").matcher(legal);
		   while(m.find()) {
			   List<String> line = new ArrayList<String>();		   
			   line.add("");
			   line.add("");
			   line.add(m.group(1));
			   if(!isDuplicateRef(resultMap, line)) {
				   bodyCR.add(line);
			   }
		   }
		   
		   if (!bodyCR.isEmpty()){		  		   		   
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
	
	public static boolean isDuplicateRef(ResultMap resultMap, List<String> line) {
		
		ResultTable resultTable = (ResultTable) resultMap.get("SaleDataSet");
		if(resultTable == null) {
			return false;
		}
		String[][] body = resultTable.getBody();
		String[] head = resultTable.getHead();
		
		int instrInd = -1;
		int bookInd = -1;
		int pageInd = -1;
		
		for(int i = 0; i < head.length; i++) {
			if(head[i].equals("InstrumentNumber")) {
				instrInd = i;
			} else if(head[i].equals("Book")) {
				bookInd = i;
			} else if(head[i].equals("Page")) {
				pageInd = i;
			} 
		}
		
		for(String[] _line : body) {
			if(instrInd >= 0 && !_line[instrInd].equals("") && _line[instrInd].equals(line.get(2))) {
				return true;
			}
			if(bookInd >= 0 && pageInd >= 0 
					&& !_line[bookInd].equals("") && _line[bookInd].equals(line.get(0)) 
					&& !_line[pageInd].equals("") && _line[pageInd].equals(line.get(1))) {
				return true;
			}
		}
		
		return false;
	}
}

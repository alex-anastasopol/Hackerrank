/**
 * 
 */
package ro.cst.tsearch.servers.functions;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableRow;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;

/**
 * @author vladb
 *
 */
public class COSanMiguelAO {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "AO");
		resultMap.put("PropertyIdentificationSet.ParcelID", row.getColumns()[0].toPlainTextString()
				.replace("&nbsp;", "").trim());
		resultMap.put("PropertyIdentificationSet.NameOnServer", row.getColumns()[2].toPlainTextString()
				.replace("&nbsp;", "").trim());
		resultMap.put("PropertyIdentificationSet.AddressOnServer", row.getColumns()[3].toPlainTextString()
				.replace("&nbsp;", "").trim());
		
		parseNames(resultMap, null);
		parseAddress(resultMap);
		
		return resultMap;
	}

	public static void parseNames(ResultMap resultMap, String ownerName) {
		
		String name;
		if(ownerName == null) {
			name = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
		} else {
			name = ownerName;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("AMENDED");
		
		name = cleanNames(name);
		String[] nameLines = splitName(name);
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = fixName(nameLines[i]);
		}
		
		COFremontAO.genericParseNames(resultMap, nameLines, null, companyExpr, 
				false, COFremontAO.ALL_NAMES_LF, -1);
	
	}
	
	private static String cleanNames(String name) {
		
		String properName = name;
		properName = properName.replaceAll("\\b(AS\\s+)?JOIN?\\b", "");
		properName = properName.replaceAll("\\b(AS\\s+)?TENANTS\\b", "");
		properName = properName.replaceAll("\\bAS\\s+TRU\\b", "");
		properName = properName.replaceAll("\\bAS\\s+JO\\b", "");
		properName = properName.replaceAll("\\bAS\\b", "");
		properName = properName.replaceAll("\\bJT\\b", "");
		properName = properName.replaceAll("\\b(AS\\s+)?JTWROS?\\b", "");
		properName = properName.replaceAll("\\bSM\\b", "");
		properName = properName.replaceAll("\\bTC\\b", "");
		properName = properName.replaceAll("\\bRA\\b", "");
		properName = properName.replaceAll("\\bEAC\\b", "");
		properName = properName.replaceAll("\\bMARRIED\\b", "");
		properName = properName.replaceAll("\\bAN\\s+INDIVIDUAL\\s+PERSON\\b", "");
		properName = properName.replaceAll("\\bUNDER REV TR A(GMT)?\\b", "");  // CHALK JOHN D III TTE UNDER REV TR AGMT O
		properName = properName.replaceAll("\\b(AND|OR)\\s+((HER|HIS)\\s+)?SUCC(ESSOR)?S?\\b", ""); // CHAPMAN GAIL L OR HER SUCCESSORS AS TRU
		properName = properName.replaceAll("\\bOR\\s+SU\\b", ""); // COHN JOHN M AND FRANCES B TRUSTEES OR SU
		
		properName = properName.replaceAll("\\bATTN\\b", "&");	
		properName = properName.replaceAll("\\bCARE\\s+OF\\b", "&");
		properName = properName.replaceAll("\\bC\\s*O\\s+(?!LLC\\b)", " & ");  // SMITH WESLEY AND ANNA C O NIELSEN HELEN
		properName = properName.replaceAll("\\b(AS\\s+)?T((RU?)?S)?TEE?S?(\\s+OF)?\\b", "&");
		
		properName = properName.replaceAll("\\bAND\\s+AND\\b", "AND");
		properName = properName.replaceAll("&\\s+&", "&");
		properName = properName.replaceAll("\\s{2,}", " ");
		
		return properName.trim();
	}
	
	private static String[] splitName(String names) {
		
		Matcher m = Pattern.compile("(.*?)AND\\s+([A-Z]{2,})\\s+[A-Z]?\\s+TRUST\\b").matcher(names);
		if(m.find()) {
			if(NameUtils.isNotCompany(m.group(1)) && NameUtils.isNotCompany(m.group(2))) {
				return new String[] {names};
			}
		}
		
		String[] lines = names.split("\\bAND\\b|&");
		
		return lines;
	}
	
	private static String fixName(String name) {
		
		String properName = name.trim();
		if(properName.length() <= 1) {
			return "";
		}
		if(NameUtils.isNotCompany(properName)) {
			
			Matcher m = Pattern.compile("([A-Z])\\s+([A-Z]{2,})\\s+([A-Z]{2,})\\s+([A-Z]{2,})").matcher(properName);
			if(m.find()) {
				if(LastNameUtils.isLastName(m.group(2)) // E BARCLAY SMITH JO
						&& LastNameUtils.isLastName(m.group(3)) 
						&& FirstNameUtils.isFirstName(m.group(4))) {
						
					properName = m.replaceFirst("$2-$3 $4 $1");
				}
			}
			m = Pattern.compile("([A-Z]{2,})\\s+([A-Z]{2,})\\s+([A-Z]{2,})\\s+([A-Z])").matcher(properName);
			if(m.find()) {
				if(FirstNameUtils.isFemaleName(m.group(1)) // BRENDA STEWART SMITH A
						&& LastNameUtils.isLastName(m.group(2)) 
						&& LastNameUtils.isLastName(m.group(3))) {
						
					properName = m.replaceFirst("$2-$3 $1 $4");
				}
			}
		} else {
			
			if(properName.indexOf(" ") < 0) {
				return "";
			}
			properName = properName.replaceFirst("\\b(LLC|LTD|LP)\\s+A(\\s+\\w+)?(\\s+(LL?C?|LT?D?|LP?)\\b|\\s*$)", "$1");
		}
		
		return properName.trim();
	}

	public static void parseAddress(ResultMap resultMap) {
		
		String address = (String) resultMap.get("PropertyIdentificationSet.AddressOnServer");
		
		if(StringUtils.isEmpty(address)) {
			return;
		}
		
		String[] parts = address.split("\\s*,\\s*");
		String onlyAddr = parts[0];
		String city = "";
		if(address.indexOf(",") > -1) {
			city = parts[1];
		}
		if(!StringUtils.isEmpty(onlyAddr)) {
			resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(onlyAddr).trim());
			resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(onlyAddr).trim());
		}
		if(!StringUtils.isEmpty(city)) {
			resultMap.put("PropertyIdentificationSet.City", city.trim());
		}
	}

	@SuppressWarnings("unchecked")
	public static void parseLegalSummary(ResultMap resultMap) {

		String legal = (String) resultMap.get("PropertyIdentificationSet.PropertyDescription");
		if(StringUtils.isEmpty(legal)) {
			return;
		}
		
//		String sec = "";
		String twn = "";
		String rng = "";
		String lot = "";
		String blk = "";
		String unit = "";
		String phase = "";
		
		// extract sec-twn-rng
		List<List> body = new ArrayList<List>();
		Matcher m = Pattern.compile("\\bS(?:EC)?(?:T|S)?\\s*(\\d+(?:\\s*(?:AND|[,&])\\s*\\d+)*)[,\\s]+T(\\d+[NWSE]{0,2})[,\\s]+R(\\d+[NWSE]{0,2})(\\W|$)").matcher(legal);
		while(m.find()) {
			String[] sections = m.group(1).split("\\s*(AND|[,&])\\s*");
			twn = m.group(2);
			rng = m.group(3);
			for(String sec:sections) {
				if(!StringUtils.isEmpty(sec)) {
					boolean foundDuplicate = false;
					for(int i = 0; i < body.size(); i++){
						List<String> list = (List<String>) body.get(i);
						if(sec.equals(list.get(0)) && twn.equals(list.get(1)) && rng.equals(list.get(2))) {
							foundDuplicate = true;
							break;
						}
					}
					if(!foundDuplicate) {
						List<String> line = new ArrayList<String>();
						line.add(sec);
						line.add(twn);
						line.add(rng);
						body.add(line);
					}
				}
			}
			legal = m.replaceFirst("");
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
		m = Pattern.compile("\\bLO?T\\s+(\\d*[A-Z\\d]\\d*)\\b").matcher(legal);
		while(m.find()) {
			lot += m.group(1) + ",";
		}
		
		m = Pattern.compile("(?<=\\bLO?TS)\\s*(\\d+[A-Z]?)(\\s*(?:-|THR?U|AND)\\s*(\\d+[A-Z]?))?([&, ]+|$)").matcher(legal);
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
		m = Pattern.compile("\\bBL(?:OC)?K\\s+([A-Z\\d]+)\\b").matcher(legal);
		while(m.find()) {
			blk += m.group(1) + ",";
		}
		
		m = Pattern.compile("(?<=\\bBLKS)\\s*(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?([&, ]+|$)").matcher(legal);
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
		m = Pattern.compile("\\bUNIT\\s+(\\d*[A-Z]\\d*)\\b").matcher(legal);
		while(m.find()) {
			unit += m.group(1) + ",";
		}
		m = Pattern.compile("(?<=\\bUNITS?)\\s*(\\d+[A-Z]?)(\\s*(?:-|THR?U|AND)\\s*(\\d+[A-Z]?))?([&, ]+|$)").matcher(legal);
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
			if(m.group(1).matches("[IVX]{1,2}")) {
				try{
					phase += Roman.parseRoman(m.group(1)) + ",";
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else {
				phase += m.group(1);
			}
		}
		if(phase.length() > 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			resultMap.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}
		
		// extract cross refs from legal description
		try {
		   List<List> bodyCR = new ArrayList<List>();
		   m = Pattern.compile("\\b(?:BOOK|BK|B)\\s*(\\d+)\\s+(?:PAGE|PG|P)?\\s*(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&, ]+|$)").matcher(legal);      	   
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
			   legal = m.replaceFirst("B" + m.group(1) + " ");
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

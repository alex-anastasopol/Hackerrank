package ro.cst.tsearch.servers.functions;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class COSummitTR {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 3) {
			String[] col0 = cols[0].toPlainTextString().trim().split("\\s+");
			resultMap.put("PropertyIdentificationSet.ParcelID",col0[1]);
			resultMap.put("TaxHistorySet.Year", col0[2].replaceAll("[()]", ""));
			
			String ownerName = cols[1].toPlainTextString().trim();
			String address = cols[2].toPlainTextString().trim();
			
			try {
				parseNames(resultMap, ownerName, false);
				parseAddress(resultMap, address);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return resultMap;
	}

	public static void parseNames(ResultMap resultMap, String ownerName, boolean hasAddress) {
		
		if(StringUtils.isEmpty(ownerName)) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("SMITH AND HAWKEN");
		Vector<String> excludeWords = new Vector<String>();
		Vector<String> excludeLast = new Vector<String>();
		excludeLast.add("MARVIN");
		
		String[] nameLines = ownerName.split("\n");
		
		if(hasAddress) {
			nameLines = GenericFunctions.removeAddressFLTR(nameLines,
					excludeWords, new Vector<String>(), 2, Integer.MAX_VALUE);
		}
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = cleanName(nameLines[i]);
			nameLines[i] = nameLines[i].replaceAll("\\A\\s*&\\s*", "").replaceAll("\\s*&\\s*\\z", "").trim();
		}
		
		COFremontAO.genericParseNames(resultMap, nameLines, null, companyExpr, 
				excludeLast, new Vector<String>(), false, COFremontAO.ALL_NAMES_LF, -1);
		
		return;
	}
	
	private static String cleanName(String name) {
		
		String properName = name;
		properName = properName.replaceAll("\\bC/O\\b", "");
		properName = NameCleaner.cleanName(properName);
		
		properName = properName.replaceAll("\\s+,", ",");
		properName = properName.trim();
		
		return properName;
	}

	public static void parseLegalSummary(ResultMap resultMap, String legalDesc) {
		
		String legal = legalDesc;
		legal = legal.replaceAll("(?is)\\bLOT\\s+(\\d+)\\s+([A-Z])(?!\\w)", "LOT $1$2");
		legal = legal.replaceAll("\\s{2,}", " ");
		legal = legal.trim();
		
		String sec = "";
//		String twn = "";
//		String rng = "";
		String lot = "";
		String blk = "";
		String unit = "";
		String phase = "";
		String subName = "";
		
		// extract subdivision name
		Matcher m = Pattern.compile("(?is)(?:(?:UNIT|BLDG|BLOCK|LOT)\\s+[\\w-]+\\s+)+([A-Z-\\s]{2,}?)(CONDOS?\\b|CONDOMINIUM|SUB\\b|ESTATES|P[.]?U[.]?D[.]?|LODGE|$)")
			.matcher(legal);
		if(m.find()) {
			subName = m.group(1) + m.group(2);
			resultMap.put("PropertyIdentificationSet.SubdivisionName", subName.trim());
		}
		
		// extract section
		m = Pattern.compile("(?is)(?<=\\bSECS?|SECT|SECTIONS?)\\s+(\\d+)(\\s*(?:-|THRU)\\s*(\\d+))?(?:[&,+; ]+|$)").matcher(legal);
		while(m.find()) {
			sec += m.group(1);
			if(m.group(3) != null) {
				sec += "-" + m.group(3);
			}
			sec += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if(sec.length() > 0) {
			sec = LegalDescription.cleanValues(sec, false, true);
			resultMap.put("PropertyIdentificationSet.SubdivisionSection", sec);
		}
		
		// extract lots
		m = Pattern.compile("(?is)(?<=\\bLO?TS?)\\s+(\\d+|\\d*[A-Z]\\d*)(\\s*(?:-|THRU)\\s*(\\d+|\\d*[A-Z]\\d*))?(?:\\s*,\\s*ALL\\s*|[&,+;) ]+|$)+").matcher(legal);
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
		m = Pattern.compile("(?is)(?<=\\bBL(?:OC)?KS)\\s+(\\d+|\\d*[A-Z])(\\s*(?:-|THRU)\\s*(\\d+|\\d*[A-Z]))?(?:[&,+; ]+|$)").matcher(legal);
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
		m = Pattern.compile("(?is)\\bUNIT\\s+([\\w-]+)").matcher(legal);
		while(m.find()) {
			unit += m.group(1) + ",";
		}
		m = Pattern.compile("(?is)(?<=\\bUNITS?)\\s+(\\d+[A-Z]?|[A-Z])(\\s*(?:-|THRU)\\s*(\\d+[A-Z]?|[A-Z]))?(?:[&,+ ]+|$)").matcher(legal);
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
		m = Pattern.compile("(?is)\\bPHASE\\s*(\\w+)").matcher(legal);
		while(m.find()) {
			if(m.group(1).matches("\\d+")) {
				phase = m.group(1);
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
		
		
	}

	public static void parseAddress(ResultMap resultMap, String address) {
		
		String properAddress = address;
		// Matcher m = Pattern.compile("(?is)(.*)(\\w+)\\s+(\\w+)?\\s+(\\w+)?").matcher(address);
		
		//bug 6869 (975 TEN MILE DR F)
		properAddress = properAddress.replaceAll("(?is)\\b(ONE|TWO|THREE|FOUR|FIVE|SIX|SEVEN|EIGHT|NINE|TEN) MILE\\b", "\"$1 MILE\"");
		
		String[] tokens = properAddress.split("\\s+");
		String addr = "";
		String suff = "";
		boolean foundSuffix = false;
		
		for(int i = tokens.length - 1; i >= 0; i--) {
			if(foundSuffix) {
				addr = tokens[i] + " " + addr;
			} else {
				if(!Normalize.isSuffix(tokens[i]) && tokens[i].matches("[A-Z\\d]*\\d+[A-Z\\d]*|[A-Z]") && tokens.length - i <= 3) {
					suff = tokens[i] + " " + suff;
				} else {
					addr = tokens[i];
					foundSuffix = true;
				}
			}
		}
		addr = addr.trim();
		suff = suff.trim();
		String strName = "";
		if(addr.matches("\\d+\\s+.*")) {
			strName = addr.split("\\s+", 2)[1];
		} else {
			strName = addr;
		}
		String possibleNumber = suff.split("\\s+")[0];
		if(possibleNumber.matches("\\d+") && Normalize.isSuffix(strName)) {
			addr += " " + possibleNumber;
		}
		
		String parsedNo = StringFormats.StreetNo(addr.trim());
		String parsedName = StringFormats.StreetName(addr.trim());
		
		if(StringUtils.isEmpty(parsedNo)) {
			StandardAddress tokAddr = new StandardAddress(addr);
			parsedNo = tokAddr.getAddressElement(StandardAddress.STREET_NUMBER);
			if(StringUtils.isNotEmpty(parsedNo)) {
				parsedName = addr.replaceFirst(parsedNo, "").trim();
			}
		}
		if(parsedNo.startsWith("0")) {
			parsedNo = parsedNo.replaceFirst("^0+", "");
		}
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), parsedNo);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), parsedName);
	}
	
	
}

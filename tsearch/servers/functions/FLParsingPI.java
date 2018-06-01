package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
 * parsing functions for Florida PI data source
  */

public class FLParsingPI {
	
	public static void parsingLegalForNameSearch(ResultMap m, long searchId) throws Exception{
	
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		if (crtCounty.equalsIgnoreCase("Broward") || crtCounty.equalsIgnoreCase("Miami-Dade")){
			parsingLegalForNameSearchOnBroward(m, searchId, crtCounty);
		} else if (crtCounty.equalsIgnoreCase("Palm Beach")){
			parsingLegalForNameSearchOnPalmBeach(m, searchId, crtCounty);
		} else if (crtCounty.equalsIgnoreCase("Hillsborough")){
			parsingLegalForNameSearchOnHillsborough(m, searchId, crtCounty);
		} else if (crtCounty.equalsIgnoreCase("Orange")){
			parsingLegalForNameSearchOnOrange(m, searchId, crtCounty);
		} else if (crtCounty.equalsIgnoreCase("Pinellas")){
			parsingLegalForNameSearchOnPinellas(m, searchId, crtCounty);
		} else if (crtCounty.equalsIgnoreCase("Duval")){
			parsingLegalForNameSearchOnDuval(m, searchId, crtCounty);
		}
		
		m.removeTempDef();
		
	}
	
	public static void parsingLegalForApnAndAddressSearch(ResultMap m, long searchId) throws Exception{
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		if (crtCounty.equalsIgnoreCase("Broward") || crtCounty.equalsIgnoreCase("Miami-Dade") 
				|| crtCounty.equalsIgnoreCase("Pinellas") || crtCounty.equalsIgnoreCase("Duval")){
			parsingLegalForApnAndAddressSearchOnBroward(m, searchId, crtCounty);
		} else if (crtCounty.equalsIgnoreCase("Palm Beach") || crtCounty.equalsIgnoreCase("Hillsborough")){
			parsingLegalForApnAndAddressSearchOnPalmBeach(m, searchId, crtCounty);
		} else if (crtCounty.equalsIgnoreCase("Orange")){
			parsingLegalForApnAndAddressSearchOnOrange(m, searchId, crtCounty);
		}
		m.removeTempDef();
	}
	
	@SuppressWarnings("rawtypes")
	public static void parsingLegalForNameSearchOnPalmBeach(ResultMap m, long searchId, String crtCounty) throws Exception{
		
		String legal = (String) m.get("tmpPropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
		
		PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		legal = legal.replaceAll("(?is)\\b(A/K/A)\\b", "; $1");
		legal = legal.replaceAll("(?is)\\b(\\d)\\s*[THRU|TO]\\s*(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(PH)([IVX]+)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)(PB|RNG)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)([A-Z]+)\\b", "$1 $2");
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?T?S?)\\s*-?\\s*([A-Z]?[\\d-]+[A-Z]?|[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BL?K?)\\s*-?\\s*([A-Z]?[\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(U(?:NIT)?|UT:?)\\s+([A-Z]?\\d+-?[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*[#|-]?\\s*([\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

	
		// extract section from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR(?:A?CT)?)\\s*-?\\s*(\\d+-?[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
		
		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			bldg = bldg + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract STR
		String sec = "";
		String twp = "";
		String rng = "";
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s*-\\s*TWP\\s*(\\d+[A-Z]?)\\s*-\\s*RNG\\s*(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			sec = ma.group(2);
			twp = ma.group(3);
			rng = ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(TOWNSHIP)\\s*:\\s*(\\d+)\\s*RANGE\\s*:\\s*(\\d+)\\s*SECTION\\s*:\\s*(\\d+)\\b");
			ma = p.matcher(legal);
			if (ma.find()){
				sec = ma.group(2);
				twp = ma.group(3);
				rng = ma.group(4);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\b(RNG)\\s*:?\\s*(\\d+)\\s*TWN\\s*:?\\s*(\\d+)\\s*SEC\\s*:?\\s*(\\d+)\\b");
				ma = p.matcher(legal);
				if (ma.find()){
					sec = ma.group(2);
					twp = ma.group(3);
					rng = ma.group(4);
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
				}
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract Section
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find() && sec.length() == 0){
			sec = ma.group(2);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("(?is)\\b(M(?:BD?|D|_BK\\s*:?)|PB)\\s*(\\d+|[A-Z])\\s*(?:-|/|MP|PGS?|;?\\s*M_PG\\s*:?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = ma.group(2).replaceAll("\\A\\s*0+", "");
			pg = ma.group(3).replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d{1,4})\\s+([A-Z])\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pb = ma.group(1).replaceAll("\\A\\s*0+", "");
				pg = ma.group(2).replaceAll("\\A\\s*0+", "");
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(3));
				legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				legal = legalTemp;
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
		
		//extract ParcelId
		String pid = "";
		if (crtCounty.equalsIgnoreCase("Plat Beach")){
			p = Pattern.compile("(?is)\\b([\\d-]{17,23})\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pid = ma.group(1).trim();
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), pid);
		
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(O[R|Z|U]?B?)\\s*(\\d+)[\\s+|-](\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(2));
			line.add(ma.group(3));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		p = Pattern.compile("(?is)\\b(?:(?:EXT)\\s*:)?\\s*(\\d{3,})/(\\d{3,})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(1));
			line.add(ma.group(2));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
		}
		
		p = Pattern.compile("(?is)\\b(\\d{11})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
		}
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			ResultTable crFromTags = (ResultTable) m.get("CrossRefSet");
			try {
				if (crFromTags != null) {
					cr = ResultTable.joinHorizontalWithMap(cr, crFromTags);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;	
		
		//extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?i)\\b([\\dA-Z]{10,})\\s+(.+?)(\\s+LOT|,?\\s+UNIT|\\s+TRACT|WK|$)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			p = Pattern.compile("(?is).*?(?:SUB/DIV:)\\s+(.*)");
			ma = p.matcher(legal.trim());
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+\\s+PLAT).*");
				ma = p.matcher(legal.trim());
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceAll("(?is)(.*?)A PORTION OF\\b.*", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)(\\s+ADDITION).*", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)\\s+(\\d)(ST|ND|RD|TH)\\s*(SEC(TION)?)", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)\\s+(SEC(TION)?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.*)(\\d+)\\s*-\\s*(\\d+)\\s*[A-Z]{1,2}", "$1");
			subdiv = subdiv.replaceFirst("(.*)(ADD)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(UNREC.*)", "$1");
			//subdiv = subdiv.replaceFirst("(.+)\\s+(A\\s+)?CONDO(MINIUM)?.*", "$1");
			//subdiv = subdiv.replaceFirst("\\A(.+?) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\s+-)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\d+)", "$1");
			subdiv = subdiv.replaceFirst("COM\\s+AT", "");
			subdiv = subdiv.replaceFirst("COR\\s+OF", "");

		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdiv);
		m.put("tmpPis", tmpPis);
		
		updatePIS(m, searchId);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parsingLegalForNameSearchOnOrange(ResultMap m, long searchId, String crtCounty) throws Exception{
		
		String legal = (String) m.get("tmpPropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal += " ";
		PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		legal = legal.replaceAll("(?is)\\b(A/K/A)\\b", "; $1");
		legal = legal.replaceAll("(?is)\\b(\\d)\\s*[THRU|TO]\\s*(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(PH)([IVX]+)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)(PB|RNG)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)([A-Z]+)\\b", "$1 $2");
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*(?:-|:)?\\s*([A-Z]?[\\d-\\s]+[A-Z]?|[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLo?K)\\s*(?:-|:)?\\s*([A-Z]?[\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UN(?:IT)?|UT)\\s*(?:-|:)?\\s+([A-Z]?\\d+-?(?:[A-Z]{1,2})?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*[#|-]?\\s*([\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

	
		// extract section from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR(?:A?CT)?)\\s*(?::|-)?\\s*([A-Z]?\\s*-?\\d+-?[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
		
		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			bldg = bldg + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract STR
		String sec = "";
		String twp = "";
		String rng = "";
		p = Pattern.compile("(?is)\\b(SECTION)\\s*:?\\s*(\\d+)\\s*TOWNSHIP\\s*:?\\s*(\\d+[A-Z]?)\\s*RANGE\\s*:?\\s*(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			sec = ma.group(2);
			twp = ma.group(3);
			rng = ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s*-?\\s*(\\d+)\\s*-?\\s*(\\d+)\\b");
			ma = p.matcher(legal);
			if (ma.find()){
				sec = ma.group(2);
				twp = ma.group(3);
				rng = ma.group(4);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract Section
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find() && sec.length() == 0){
			sec = ma.group(2);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("(?is)\\b(MB|PB)\\s*(\\d+|[A-Z])\\s*(?:-|PGS?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = ma.group(2).replaceAll("\\A\\s*0+", "");
			pg = ma.group(3).replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(\\d{1,3})\\s*/\\s*(\\d{1,3})\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				try{
				pb = ma.group(1).replaceAll("\\A\\s*0+", "");
				pg = ma.group(2).replaceAll("\\A\\s*0+", "");
				legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				legal = legalTemp;
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
		
		//extract ParcelId
		String pid = "";
		if (crtCounty.equalsIgnoreCase("Orange")){
			p = Pattern.compile("(?is)\\b(Parcel\\s*:)\\s*([\\d\\s]{15,20})\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pid = ma.group(2).trim();
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), pid);
		
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(O[R|Z|U]?B?)\\s*(\\d+)[\\s+|-](\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(2));
			line.add(ma.group(3));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		p = Pattern.compile("(?is)\\b(?:(?:EXT)\\s*:)?\\s*(\\d{3,})/(\\d{3,})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(1));
			line.add(ma.group(2));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
		}
		
		p = Pattern.compile("(?is)\\b(\\d{11})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
		}
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			ResultTable crFromTags = (ResultTable) m.get("CrossRefSet");
			try {
				if (crFromTags != null) {
					cr = ResultTable.joinHorizontalWithMap(cr, crFromTags);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;	
		
		//extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?is)\\b(TS\\s*:|SUB)(.*?)(,|UT|ETC|LT|$)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} 
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceAll("(?is)(.*?)A PORTION OF\\b.*", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)(\\s+ADDITION).*", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)\\s+(\\d)(ST|ND|RD|TH)\\s*(SEC(TION)?)", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)\\s+(SEC(TION)?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.*)(\\d+)\\s*-\\s*(\\d+)\\s*[A-Z]{1,2}", "$1");
			subdiv = subdiv.replaceFirst("(.*)(ADD)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(UNREC.*)", "$1");
			//subdiv = subdiv.replaceFirst("(.+)\\s+(A\\s+)?CONDO(MINIUM)?.*", "$1");
			//subdiv = subdiv.replaceFirst("\\A(.+?) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\s+-)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\d+)", "$1");
			subdiv = subdiv.replaceFirst("COM\\s+AT", "");
			subdiv = subdiv.replaceFirst("COR\\s+OF", "");

		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdiv);
		m.put("tmpPis", tmpPis);
		
		updatePIS(m, searchId);	
	}
	
	@SuppressWarnings("rawtypes")
	public static void parsingLegalForNameSearchOnPinellas(ResultMap m, long searchId, String crtCounty) throws Exception{
		
		String legal = (String) m.get("tmpPropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal += " ";
		PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		//some legals doesn't have LT indicative, so we put them
		legal = legal.trim().replaceAll("(?is)\\A(\\d{1,3}|[A-Z])\\s+", "LT $1 ");
		//
		
		legal = legal.replaceAll("(?is)\\b(A/K/A)\\b", "; $1");
		legal = legal.replaceAll("(?is)\\b(\\d)\\s*[THRU|TO]\\s*(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(PH)([IVX]+)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)(PB|RNG)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)([A-Z]+)\\b", "$1 $2");
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*(?:-|:)?\\s*([A-Z]?[\\d-\\s]+|[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLo?K)\\s*(?:-|:)?\\s*([A-Z]?[\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UN(?:IT)?|UT)\\s*(?:-|:)?\\s+([A-Z]?\\d+-?(?:[A-Z]{1,2})?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*[#|-]?\\s*([\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

	
		// extract section from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR(?:A?CT)?)\\s*(?::|-)?\\s*([A-Z]?\\s*-?\\d+-?[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
		
		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]\\d?|\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			bldg = bldg + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract STR
		String sec = "";
		String twp = "";
		String rng = "";
		p = Pattern.compile("(?is)\\b(SECTION)\\s*:?\\s*(\\d+)\\s*TOWNSHIP\\s*:?\\s*(\\d+[A-Z]?)\\s*RANGE\\s*:?\\s*(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			sec = ma.group(2);
			twp = ma.group(3);
			rng = ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b\\s*(\\d{1,2}[A-Z]?)\\s*-\\s*(\\d{1,2}[A-Z]?)\\s*-\\s*(\\d{1,2}[A-Z]?)\\s+");
			ma = p.matcher(legal);
			if (ma.find()){
				sec = ma.group(2);
				twp = ma.group(3);
				rng = ma.group(4);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract Section
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find() && sec.length() == 0){
			sec = ma.group(2);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("(?is)\\b(PL)\\s*(\\d+)\\s*(?:-|/)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = ma.group(2).replaceAll("\\A\\s*0+", "");
			pg = ma.group(3).replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} 
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
		
		//extract ParcelId
		String pid = "";
		if (crtCounty.equalsIgnoreCase("Pinellas")){
			p = Pattern.compile("(?is)\\b(\\d{2}\\s*/\\s*\\d{2}\\s*/\\s*\\d{2}\\s*/\\s*\\d{5}\\s*/\\s*\\d{3}\\s*/\\s*\\d{4})\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pid = ma.group(1).trim();
				legalTemp = legalTemp.replaceFirst(ma.group(0), "");
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), pid);
		
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(?:OR)?\\s*(\\d{3,})\\s*/\\s*(\\d{2,})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(1));
			line.add(ma.group(2));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			ResultTable crFromTags = (ResultTable) m.get("CrossRefSet");
			try {
				if (crFromTags != null) {
					cr = ResultTable.joinHorizontalWithMap(cr, crFromTags);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;	
		
		//extract subdivision name
		String subdiv = "";
		legal = legal.replaceAll("(?is)\\bETC\\b", "");
		p = Pattern.compile("(?is)\\b(BLK?)(.*?)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			p = Pattern.compile("(?is)\\b(LTS?)(.*?)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(2);
			} else {
				p = Pattern.compile("(?is)\\b(UN)\\s+(.*?)");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(2);
				}
			}
		}
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceAll("(?is)(.*?)A PORTION OF\\b.*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.*)(\\d+)\\s*-\\s*(\\d+)\\s*[A-Z]{1,2}", "$1");
			subdiv = subdiv.replaceFirst("(.*)(ADD)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(UNREC.*)", "$1");
			//subdiv = subdiv.replaceFirst("(.+)\\s+(A\\s+)?CONDO(MINIUM)?.*", "$1");
			//subdiv = subdiv.replaceFirst("\\A(.+?) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\s+-)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\d+)", "$1");
			subdiv = subdiv.replaceFirst("COM\\s+AT", "");
			subdiv = subdiv.replaceFirst("COR\\s+OF", "");

		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdiv);
		m.put("tmpPis", tmpPis);
		
		updatePIS(m, searchId);	
	}
	
	@SuppressWarnings("rawtypes")
	public static void parsingLegalForNameSearchOnDuval(ResultMap m, long searchId, String crtCounty) throws Exception{
		
		String legal = (String) m.get("tmpPropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
		
		PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		legal = legal.replaceAll("(?is)\\b(A/K/A)\\b", "; $1");
		legal = legal.replaceAll("(?is)\\b(\\d)\\s*[THRU|TO]\\s*(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(PH)([IVX]+)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)(PB|RNG)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)([A-Z]+)\\b", "$1 $2");
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(L(?:OT)?S?)\\s*-?\\s*([\\d\\s]+|(?:[A-Z])?[\\d-,]+(?:[A-Z])?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), "LOT ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(B(?:LK)?)\\s+([A-Z]?[\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), "BLOCK ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UN(?:IT)?|UT:?)([A-Z]?\\d+-?[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), "UNIT ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*[#|-]?\\s*([\\d-]+[A-Z]?(?:[\\d-]+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), "PHASE ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

	
		// extract section from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR(?:A?CT)?)\\s*-?\\s*(\\d+-?[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), "TRACT ");
		}
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
		
		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			bldg = bldg + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract STR
		String sec = "";
		String twp = "";
		String rng = "";
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s+(\\d+[A-Z]?)\\s+(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			sec = ma.group(2);
			twp = ma.group(3);
			rng = ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b\\s(\\d{1,2})\\s+(\\d{1,2}[A-Z]?)\\s+(\\d{1,2}[A-Z]?)\\b");
			ma = p.matcher(legal);
			if (ma.find()){
				sec = ma.group(1);
				twp = ma.group(2);
				rng = ma.group(3);
				legalTemp = legalTemp.replaceFirst(ma.group(0), "");
			} 
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract Section
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find() && sec.length() == 0){
			sec = ma.group(2);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("(?is)\\b(M(?:BD?|D|_BK\\s*:?)|PB)\\s*(\\d+|[A-Z])\\s*(?:-|/|MP|PGS?|;?\\s*M_PG\\s*:?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = ma.group(2).replaceAll("\\A\\s*0+", "");
			pg = ma.group(3).replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d{1,4})\\s+([A-Z])\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pb = ma.group(1).replaceAll("\\A\\s*0+", "");
				pg = ma.group(2).replaceAll("\\A\\s*0+", "");
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(3));
				legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				legal = legalTemp;
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
		
		//extract ParcelId
		String pid = "";
		/*if (crtCounty.equalsIgnoreCase("Duval")){
			p = Pattern.compile("(?is)\\b\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pid = ma.group(1).trim();
			}
		}*/
		tmpPis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), pid);
		
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(OR(?:\\s+BK)?)\\s+(\\d+)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(2));
			line.add(ma.group(3));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		p = Pattern.compile("(?is)\\b(\\d{3,})/(\\d{1,})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(1));
			line.add(ma.group(2));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
		}
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			ResultTable crFromTags = (ResultTable) m.get("CrossRefSet");
			try {
				if (crFromTags != null) {
					cr = ResultTable.joinHorizontalWithMap(cr, crFromTags);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;	
		
		//extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?i)\\b(BLOCK)\\s+(.+?)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			p = Pattern.compile("(?is)\\b(LOT)\\s+(.*)");
			ma = p.matcher(legal.trim());
			if (ma.find()) {
				subdiv = ma.group(2);
			} else {
				p = Pattern.compile("(?is)\\b(UNIT)\\s+(.*)");
				ma = p.matcher(legal.trim());
				if (ma.find()) {
					subdiv = ma.group(2);
				} else {
					p = Pattern.compile("(?is)\\b(BLDG)\\s+(.*)");
					ma = p.matcher(legal.trim());
					if (ma.find()) {
						subdiv = ma.group(2);
					} 
				}
			}
		}
		subdiv = subdiv.replaceAll("(?is)\\bUNIT\\b.*", "");
		subdiv = subdiv.replaceAll("(?is)\\bOR\\b.*", "");
		subdiv = subdiv.replaceAll("(?is)\\bSEC\\b.*", "");
		
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdiv);
		m.put("tmpPis", tmpPis);
		
		updatePIS(m, searchId);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parsingLegalForNameSearchOnHillsborough(ResultMap m, long searchId, String crtCounty) throws Exception{
		
		String legal = (String) m.get("tmpPropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
		
		PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		legal = legal.replaceAll("(?is)\\b(A/K/A)\\b", "; $1");
		legal = legal.replaceAll("(?is)\\b(\\d)\\s*[THRU|TO]\\s*(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(PH)([IVX]+)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)(PB|RNG)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)([A-Z]+)\\b", "$1 $2");
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?T?S?)\\s*-?\\s*([A-Z]?[\\d-,]+[A-Z]?|[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), "LOT ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(B(?:LK)?)\\s+([A-Z]?[\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), "BLOCK ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(U(?:NIT)?|UT:?)\\s+([A-Z]?\\d+-?[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*[#|-]?\\s*([\\d-]+[A-Z]?(?:[\\d-]+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

	
		// extract section from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR(?:A?CT)?)\\s*-?\\s*(\\d+-?[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
		
		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			bldg = bldg + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract STR
		String sec = "";
		String twp = "";
		String rng = "";
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s*-\\s*TWP\\s*(\\d+[A-Z]?)\\s*-\\s*RNG\\s*(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			sec = ma.group(2);
			twp = ma.group(3);
			rng = ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(S)\\s*(\\d+)\\s*T\\s*(\\d+[A-Z]?)\\s*R\\s*(\\d+[A-Z]?)\\b");
			ma = p.matcher(legal);
			if (ma.find()){
				sec = ma.group(2);
				twp = ma.group(3);
				rng = ma.group(4);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			} 
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract Section
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find() && sec.length() == 0){
			sec = ma.group(2);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("(?is)\\b(M(?:BD?|D|_BK\\s*:?)|PB)\\s*(\\d+|[A-Z])\\s*(?:-|/|MP|PGS?|;?\\s*M_PG\\s*:?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = ma.group(2).replaceAll("\\A\\s*0+", "");
			pg = ma.group(3).replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d{1,4})\\s+([A-Z])\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pb = ma.group(1).replaceAll("\\A\\s*0+", "");
				pg = ma.group(2).replaceAll("\\A\\s*0+", "");
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(3));
				legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				legal = legalTemp;
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
		
		//extract ParcelId
		String pid = "";
		/*if (crtCounty.equalsIgnoreCase("Hillsborough")){
			p = Pattern.compile("(?is)\\b\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pid = ma.group(1).trim();
			}
		}*/
		tmpPis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), pid);
		
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(OR(?:\\s+BK)?)\\s*(\\d+)\\s*PG\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(2));
			line.add(ma.group(3));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		p = Pattern.compile("(?is)\\b(\\d{3,})/(\\d{1,})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(1));
			line.add(ma.group(2));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
		}
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			ResultTable crFromTags = (ResultTable) m.get("CrossRefSet");
			try {
				if (crFromTags != null) {
					cr = ResultTable.joinHorizontalWithMap(cr, crFromTags);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;	
		
		//extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?i)\\b(BLOCK)\\s+(.+?)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			p = Pattern.compile("(?is)(LOT)\\s+(.*)");
			ma = p.matcher(legal.trim());
			if (ma.find()) {
				subdiv = ma.group(2);
			} 
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdiv);
		m.put("tmpPis", tmpPis);
		
		updatePIS(m, searchId);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parsingLegalForNameSearchOnBroward(ResultMap m, long searchId, String crtCounty) throws Exception{
		String legal = (String) m.get("tmpPropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
		
		PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		legal = legal.replaceAll("(?is)\\b(A/K/A)\\b", "; $1");
		legal = legal.replaceAll("(?is)\\b(\\d)\\s*[THRU|TO]\\s*(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(PH)([IVX]+)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)(PB)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)([A-Z]+)\\b", "$1 $2");
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?T?S?)\\s*-?\\s*([A-Z]?[\\d-]+[A-Z]?|[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BL?K?)\\s*-?\\s*([A-Z]?[\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(U(?:NIT)?|UT:?)\\s+([A-Z]?\\d+-?[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

	
		// extract section from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR(?:A?CT)?)\\s*-?\\s*(\\d+-?[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
		
		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			bldg = bldg + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract STR
		String sec = "";
		String twp = "";
		String rng = "";
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s*-\\s*TWP\\s*(\\d+[A-Z]?)\\s*-\\s*RNG\\s*(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			sec = ma.group(2);
			twp = ma.group(3);
			rng = ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract Section
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find() && sec.length() == 0){
			sec = ma.group(2);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("(?is)\\b(M[BD?|D]|PB)\\s*(\\d+|[A-Z])\\s*[-|/]\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = ma.group(2).replaceAll("\\A\\s*0+", "");
			pg = ma.group(3).replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d{1,4})\\s+([A-Z])\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pb = ma.group(1).replaceAll("\\A\\s*0+", "");
				pg = ma.group(2).replaceAll("\\A\\s*0+", "");
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(3));
				legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				legal = legalTemp;
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
		
		//extract ParcelId
		String pid = "";
		if (crtCounty.equalsIgnoreCase("Broward")){
			p = Pattern.compile("(?is)\\b([\\dA-Z-]{12,16})\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pid = ma.group(1).trim();
			}
		} if (crtCounty.equalsIgnoreCase("Miami-Dade")){
			p = Pattern.compile("(?is)\\b([\\d-\\s]{13,16})\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pid = ma.group(1).trim();
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), pid);
		
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(O[R|Z|U]?B?)\\s*(\\d+)[\\s+|-](\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(2));
			line.add(ma.group(3));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		p = Pattern.compile("(?is)\\b(?:(?:EXT)\\s*:)?\\s*(\\d{3,})/(\\d{3,})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(1));
			line.add(ma.group(2));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
		}
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			ResultTable crFromTags = (ResultTable) m.get("CrossRefSet");
			try {
				if (crFromTags != null) {
					cr = ResultTable.joinHorizontalWithMap(cr, crFromTags);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;	
		
		//extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?i)\\b([\\dA-Z]{10,})\\s+(.+?)(\\s+LOT|,?\\s+UNIT|\\s+TRACT|WK|$)");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		} else {
			p = Pattern.compile("(?is).*?(?:SUB/DIV:)\\s+(.*)");
			ma = p.matcher(legal.trim());
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+\\s+PLAT).*");
				ma = p.matcher(legal.trim());
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceAll("(?is)(.*?)A PORTION OF\\b.*", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)(\\s+ADDITION).*", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)\\s+(\\d)(ST|ND|RD|TH)\\s*(SEC(TION)?)", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)\\s+(SEC(TION)?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.*)(\\d+)\\s*-\\s*(\\d+)\\s*[A-Z]{1,2}", "$1");
			subdiv = subdiv.replaceFirst("(.*)(ADD)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(UNREC.*)", "$1");
			//subdiv = subdiv.replaceFirst("(.+)\\s+(A\\s+)?CONDO(MINIUM)?.*", "$1");
			//subdiv = subdiv.replaceFirst("\\A(.+?) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\s+-)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\d+)", "$1");
			subdiv = subdiv.replaceFirst("COM\\s+AT", "");
			subdiv = subdiv.replaceFirst("COR\\s+OF", "");

		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdiv);
		m.put("tmpPis", tmpPis);
		
		updatePIS(m, searchId);
		
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static void parsingLegalForApnAndAddressSearchOnOrange(ResultMap m, long searchId, String crtCounty) throws Exception{
		
		String legal = (String) m.get("tmpPropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
		
		legal += " ";
		PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("(?is)\\b(A/K/A)\\b", "; $1");
		legal = legal.replaceAll("(?is)\\b(\\d)\\s*[THRU|TO]\\s*(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(UNIT)(\\d+)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)\\b[SWEN]{1,2}\\s+[\\d\\.]+\\s+FT\\s+OF\\b", "");
		legal = legal.replaceAll("(?is)\\+", "&");
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(Lo?tS?\\s*)([A-Z]?[\\d\\s&,-]+[A-Z]?|[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
		}

		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(Blo?c?k?\\s+)([A-Z]|[\\d&]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(Un(?:it)?\\s+|UT)(?:#|NO)?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PHA?S?E?\\s+)([\\d&,]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

	
		// extract section from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR(?:act(?:\\s+number)?)?)[\\s+|#]([A-Z]?[\\d\\s-&,]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(Bldg|Building\\s*)#?\\s*([\\d-]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			bldg = bldg + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
		}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		
		
		//extract STR
		String sec = "";
		String twp = "";
		String rng = "";
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s+TWN\\s*(\\d+[A-Z]?)\\s+RNG\\s*(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			sec = ma.group(2);
			twp = ma.group(3);
			rng = ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(S)\\s*(\\d+)\\s*T\\s*(\\d+[A-Z]?)\\s*R\\s*(\\d+[A-Z]?)\\b");
			ma = p.matcher(legal);
			if (ma.find()){
				sec = ma.group(2);
				twp = ma.group(3);
				rng = ma.group(4);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d{1,2})");
				ma = p.matcher(legal);
				if (ma.find()){
					sec = ma.group(1);
					twp = ma.group(2);
					rng = ma.group(3);
				}
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract Section
		p = Pattern.compile("(?is)\\b(SEC(?:TIONS?)?\\s*)([\\d\\s&]+|[A-Z]{1,2}[-|\\s]?\\d?)\\b");
		ma = p.matcher(legal);
		if (ma.find() && sec.length() == 0){
			sec = ma.group(2);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
				
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(O[R|Z|U]?B?)\\s*(\\d+)(?:\\s+|[-/]|(?:\\s*PG?\\s*))(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(2));
			line.add(ma.group(3));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		p = Pattern.compile("(?is)\\b(PRIOR\\s+REF)\\s*([\\d-]+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			if (ma.group(2).contains("-")){
				String[] bp = ma.group(2).split("-");
				if (bp.length == 2){
					line.add("");
					line.add(bp[0].replaceAll("\\A\\s*0+", ""));
					line.add(bp[1].replaceAll("\\A\\s*0+", ""));
				}
				
			} else {
				line.add(ma.group(2));
				line.add("");
				line.add("");
			}
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			ResultTable crFromTags = (ResultTable) m.get("CrossRefSet");
			try {
				if (crFromTags != null) {
					cr = ResultTable.joinHorizontalWithMap(cr, crFromTags);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;	
		
		//extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("(?is)\\b(MB|PB)\\s*(\\d+|[A-Z])\\s*(PG|[/-])\\s*([\\d]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = ma.group(2).replaceAll("\\A\\s*0+", "");
			pg = ma.group(4).replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} else {
			ma.reset();
			p = Pattern.compile("(?is)(?:\\s+|\\A\\s*)([A-Z]|\\d{2,})\\s*/\\s*(\\d+)\\s+");
			ma = p.matcher(legal);
			if (ma.find()) {
				pb = ma.group(1).replaceAll("\\A\\s*0+", "");
				pg = ma.group(2).replaceAll("\\A\\s*0+", "");
				legalTemp = legalTemp.replaceFirst(ma.group(0), " PB" + ma.group(0));
				legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				legal = legalTemp;
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
		
		//extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?is)\\bin\\s+(.*?)\\s+subdivision\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A\\s*(?:UNIT|BLK|LOT)\\s+(.*)");
			ma = p.matcher(legal.trim());
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)(?:\\s+UNIT|\\s+SUB|\\s+PB|$)");
				ma = p.matcher(legal.trim());
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}
		if (subdiv.matches("(?is).*\\b[NSWE]+\\s+[\\d',-/]+.*")){
			subdiv = "";
		}
		if (subdiv.length() != 0) {
	
			subdiv = subdiv.replaceAll("(?is)(.*?)REV\\b.*", "$1");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*(BLDG|BLK|LOT)\\s+(.*?)", "$2");
			//subdiv = subdiv.replaceAll("(?is)(.*)(\\s+ADDITION).*", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)\\s+(\\d)(ST|ND|RD|TH)\\s*(SEC(TION)?)", "$1");
			subdiv = subdiv.replaceAll("(?is)(.*)(\\d+)\\s*-\\s*(\\d+)\\s*[A-Z]{1,2}", "$1");
			subdiv = subdiv.replaceFirst("(.*)(ADD)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(UNREC.*)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\s+PB\\s+).*", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+(A\\s+)?CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceFirst("\\A(.+?) SUB(DIVISION)?.*", "$1");

		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdiv);
		m.put("tmpPis", tmpPis);
		
		updatePIS(m, searchId);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parsingLegalForApnAndAddressSearchOnPalmBeach(ResultMap m, long searchId, String crtCounty) throws Exception{
		
		String legal = (String) m.get("tmpPropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
		
		PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("(?is)\\b(A/K/A)\\b", "; $1");
		legal = legal.replaceAll("(?is)\\b(\\d)\\s*[THRU|TO]\\s*(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("(?is)\\b(UNIT)(\\d+)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)\\b[SWEN]{1,2}\\s+[\\d\\.]+\\s+FT\\s+OF\\b", "");
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(Lo?tS?)\\s*([A-Z]?[\\d\\s&,-]+[A-Z]?|[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
		}

		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(Blo?c?k?)\\s+([A-Z]|[\\d&]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(Un(?:it)?\\s+|UT:?)\\s*(?:#|NO)?\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PHA?S?E?)\\s+([\\d\\s&,]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

	
		// extract section from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR(?:act(?:\\s+number)?)?)[\\s+|#]([A-Z]?[\\d\\s-&,]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(Bldg|Building)\\s*#?\\s*([\\d-]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			bldg = bldg + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
		}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		
		
		//extract STR
		String sec = "";
		String twp = "";
		String rng = "";
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s+TWN\\s*(\\d+[A-Z]?)\\s+RNG\\s*(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			sec = ma.group(2);
			twp = ma.group(3);
			rng = ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(S)\\s*(\\d+)\\s*T\\s*(\\d+[A-Z]?)\\s*R\\s*(\\d+[A-Z]?)\\b");
			ma = p.matcher(legal);
			if (ma.find()){
				sec = ma.group(2);
				twp = ma.group(3);
				rng = ma.group(4);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d{1,2})");
				ma = p.matcher(legal);
				if (ma.find()){
					sec = ma.group(1);
					twp = ma.group(2);
					rng = ma.group(3);
				}
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract Section
		p = Pattern.compile("(?is)\\b(SEC(?:TIONS?)?)\\s*([\\d\\s&]+|[A-Z]{1,2}[-|\\s]?\\d?)\\b");
		ma = p.matcher(legal);
		if (ma.find() && sec.length() == 0){
			sec = ma.group(2);
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
			//legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("(?is)\\b(MB|PB)\\s*(\\d+|[A-Z])\\s*(PG|[/-])\\s*([\\d]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = ma.group(2).replaceAll("\\A\\s*0+", "");
			pg = ma.group(4).replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
		
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(O[R|Z|U]?B?)\\s*(\\d+)(?:\\s+|-|(?:\\s*PG?\\s*))(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(2));
			line.add(ma.group(3));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		p = Pattern.compile("(?is)\\b(PRIOR\\s+REF)\\s*(\\d+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add("");
			line.add("");
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			ResultTable crFromTags = (ResultTable) m.get("CrossRefSet");
			try {
				if (crFromTags != null) {
					cr = ResultTable.joinHorizontalWithMap(cr, crFromTags);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;	
		
		//extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?is)\\bin\\s+(.*?)\\s+subdivision\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A\\s*(?:UNIT|BLK)\\s+(.*)");
			ma = p.matcher(legal.trim());
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)(?:\\s+UNIT|\\s+SUB|\\s+PB|$)");
				ma = p.matcher(legal.trim());
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}
		if (subdiv.matches("(?is).*\\b[NSWE]+\\s+[\\d',-/]+.*")){
			subdiv = "";
		}
		if (subdiv.length() != 0) {
	
			subdiv = subdiv.replaceAll("(?is)(.*?)REV\\b.*", "$1");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BLDG\\s+(.*?)", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)(\\s+ADDITION).*", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)\\s+(\\d)(ST|ND|RD|TH)\\s*(SEC(TION)?)", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)\\s+(SEC(TION)?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.*)(\\d+)\\s*-\\s*(\\d+)\\s*[A-Z]{1,2}", "$1");
			subdiv = subdiv.replaceFirst("(.*)(ADD)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(UNREC.*)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+(A\\s+)?CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceFirst("\\A(.+?) SUB(DIVISION)?.*", "$1");

		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdiv);
		m.put("tmpPis", tmpPis);
		
		updatePIS(m, searchId);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parsingLegalForApnAndAddressSearchOnBroward(ResultMap m, long searchId, String crtCounty) throws Exception{
		
		String legal = (String) m.get("tmpPropertyIdentificationSet.PropertyDescription");
		
		if (StringUtils.isEmpty(legal))
			return;
				
		PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("(?is)\\b(A/K/A)\\b", "; $1");
		legal = legal.replaceAll("(?is)\\b(UNIT)(\\d+)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)\\b[SWEN]{1,2}\\s+[\\d\\./]+\\s+(FT\\s+)?OF\\b", "");
		legal = legal.replaceAll("(?is)\\b(O/?[R|Z|U]?B?(?:\\s+BKS?)?|RECD)\\s*(\\d+[\\s+|-]\\d+)\\s*,\\s*(\\d+[\\s+|-]\\d+)\\b", "$1 $2 $1 $3");
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(Lo?tS?)\\s*([A-Z]?[\\d\\s&,-]+[A-Z]?|[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);

		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(Blo?c?k?S?)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).replaceAll("-\\s*$", "").replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(Un(?:it)\\s+?|UT:?)#?\\s*([A-Z]?[\\d-]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

	
		// extract section from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR(?:act(?:\\s+number)?)?)[\\s+|#]([A-Z][&\\s]?[A-Z]?|[\\d\\s-&,]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(Bldg|Building)\\s*#?\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			bldg = bldg + " " + ma.group(2).replaceAll("\\A\\s*0+", "");
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
				
		//extract STR
		String sec = "";
		String twp = "";
		String rng = "";
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s+TWN\\s*(\\d+[A-Z]?)\\s+RNG\\s*(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			sec = ma.group(2);
			twp = ma.group(3);
			rng = ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(S)\\s*(\\d+)\\s*T\\s*(\\d+[A-Z]?)\\s*R\\s*(\\d+[A-Z]?)\\b");
			ma = p.matcher(legal);
			if (ma.find()){
				sec = ma.group(2);
				twp = ma.group(3);
				rng = ma.group(4);
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\b(\\d+)\\s*-s*(\\d+[A-Z]?)\\s*-\\s*(\\d+[A-Z]?)\\b");
				ma = p.matcher(legal);
				if (ma.find()){
					sec = ma.group(2);
					twp = ma.group(3);
					rng = ma.group(4);
					legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
				}
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
		
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract Section
		p = Pattern.compile("(?is)\\b(SEC(?:TION)?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find() && sec.length() == 0){
			sec = ma.group(2);
			//legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("(?is)\\b(MB|PB)\\s*(\\d+)\\s*(PG|[/-])\\s*([\\d]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = ma.group(2).replaceAll("\\A\\s*0+", "");
			pg = ma.group(4).replaceAll("\\A\\s*0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d+)\\s*[A-Z]{1,2}\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pb = ma.group(1).replaceAll("\\A\\s*0+", "");
				pg = ma.group(2).replaceAll("\\A\\s*0+", "");
				legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				legal = legalTemp;
			}
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
		tmpPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
		
		//extract ParcelId
		String pid = "";
		if (crtCounty.equalsIgnoreCase("Broward")){
			p = Pattern.compile("(?is)\\b([\\dA-Z-]{12,16})\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pid = ma.group(1).trim();
			}
		} else if (crtCounty.equalsIgnoreCase("Miami-Dade")){
			p = Pattern.compile("(?is)\\b([\\d-\\s]{13,16})\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pid = ma.group(1).trim();
			}
		} else if (crtCounty.equalsIgnoreCase("Pinellas")){
			p = Pattern.compile("(?is)\\b(\\d{2}\\s*/\\s*\\d{2}\\s*/\\s*\\d{2}\\s*/\\s*\\d{5}\\s*/\\s*\\d{3}\\s*/\\s*\\d{4})\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				pid = ma.group(1).trim();
				legalTemp = legalTemp.replaceFirst(ma.group(0), "");
			}
		}
		
		tmpPis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), pid);
		
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b(O/?[R|Z|U]?B?(?:\\s+BKS?)?|RECD)\\s*(\\d+)[\\s+|-](\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(2));
			line.add(ma.group(3));
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		p = Pattern.compile("(?is)\\b(PRIOR\\s+REF)\\s*([\\d-]+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			boolean isAlready = false;
			List<String> line = new ArrayList<String>();
			if (ma.group(2).contains("-")){
				String[] bp = ma.group(2).split("-");
				if (bp.length == 2){
					line.add("");
					line.add(bp[0].replaceAll("\\A\\s*0+", ""));
					line.add(bp[1].replaceAll("\\A\\s*0+", ""));
				}
				
			} else {
				line.add(ma.group(2));
				line.add("");
				line.add("");
			}
			if (bodyCR.isEmpty()){
				bodyCR.add(line);
			} else {
				for (List lst : bodyCR){
					if (lst.equals(line)){
						isAlready = true;
						break;
					}
				}
				if (!isAlready){
					bodyCR.add(line);
				}
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			ResultTable crFromTags = (ResultTable) m.get("CrossRefSet");
			try {
				if (crFromTags != null) {
					cr = ResultTable.joinHorizontalWithMap(cr, crFromTags);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;	
		
		//extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?is)\\bin\\s+(.*?)\\s+subdivision\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\A(?:UNIT)\\s+(.*)");
			ma = p.matcher(legal.trim());
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A(.+)\\s+(?:UNIT|SUB|PB)");
				ma = p.matcher(legal.trim());
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}
		if (subdiv.length() != 0) {
	
			subdiv = subdiv.replaceAll("(?is)(.*?)REV\\b.*", "$1");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*BLDG\\s+(.*?)", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)(\\s+ADDITION).*", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)\\s+(\\d)(ST|ND|RD|TH)\\s*(SEC(TION)?)", "$1");
			//subdiv = subdiv.replaceAll("(?is)(.*)\\s+(SEC(TION)?).*", "$1");
			subdiv = subdiv.replaceAll("(?is)(.*)(\\d+)\\s*-\\s*(\\d+)\\s*[A-Z]{1,2}", "$1");
			subdiv = subdiv.replaceFirst("(.*)(ADD)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\s+PINST:?).*", "$1");
			subdiv = subdiv.replaceFirst("(.*)(UNREC.*)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+(A\\s+)?CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceFirst("\\A(.+?) SUB(DIVISION)?.*", "$1");
		}
		tmpPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdiv);
		m.put("tmpPis", tmpPis);
		
		updatePIS(m, searchId);
				
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void updatePIS(ResultMap m, long searchId){
		
		boolean needNewPIS = false;
		Vector pisVector = (Vector) m.get("PropertyIdentificationSet");
		PropertyIdentificationSet tmpPis = (PropertyIdentificationSet) m.get("tmpPis");
		if (pisVector != null) {
			PropertyIdentificationSet pis = (PropertyIdentificationSet) pisVector.get(0);
			String subdiv = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName());
			String pb = tmpPis.getAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName());
			String pg = tmpPis.getAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName());
			String lot = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName());
			String block = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName());
			String unit = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName());
			String phase = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName());
			String tract = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName());
			String bldg = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName());
			String pid = tmpPis.getAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName());
			String sec = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName());
			String twp = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName());
			String rng = tmpPis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName());
			
			String pisSubdName = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName());
			String pisPB = pis.getAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName());
			String pisPNO = pis.getAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName());
			String pisBlock = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName());
			
			if (StringUtils.isNotEmpty(pisSubdName) && StringUtils.isNotEmpty(subdiv)){
				
				BigDecimal score = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_REGISTER_NAME_NA, pisSubdName, subdiv, searchId)).getScore();
		        if (score.compareTo(new BigDecimal(0.6)) >= 0){
		        	needNewPIS = false;
		        } else {
		        	needNewPIS = true;
		        }
			} else if (StringUtils.isNotEmpty(pisPB) && StringUtils.isNotEmpty(pisPNO)){	
				if (pisPB.equals(pb.trim()) && pisPNO.equals(pg.trim())){
					needNewPIS = false;
				} else {
		        	needNewPIS = true;
		        }
			} else if (StringUtils.isNotEmpty(pisBlock)){
				if (pisBlock.equals(block.trim())){
					needNewPIS = false;
				} else {
		        	needNewPIS = true;
		        }
			}

			if (!needNewPIS){
				lot = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName()) + " " + lot;
				lot = LegalDescription.cleanValues(lot, false, true);
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
				
				block = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName()) + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
				
				unit = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName()) + " " + unit;
				unit = LegalDescription.cleanValues(unit, false, true);
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
				
				phase = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName()) + " " + phase;
				phase = LegalDescription.cleanValues(phase, false, true);
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
				
				tract = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName()) + " " + tract;
				tract = LegalDescription.cleanValues(tract, false, true);
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
				
				bldg = pis.getAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName()) + " " + bldg;
				bldg = LegalDescription.cleanValues(bldg, false, true);
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
				
				pis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), pid);
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
				if (StringUtils.isEmpty(pisPB)){
					pis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
				}
				if (StringUtils.isEmpty(pisPNO)){
					pis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
				}
				
			} else {
				PropertyIdentificationSet newPis = new PropertyIdentificationSet();
				newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot);
				newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
				newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), unit);
				newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phase);
				newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
				newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), bldg);
				newPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pb);
				newPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), pg);
				newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdiv);
				if (subdiv.matches("(?is).*\\b(CO?NDO(MINIUM)?)\\b.*")) {
					newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_COND.getShortKeyName(), subdiv.trim());
				}
				newPis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), pid);
				newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), sec);
				newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), twp);
				newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), rng);
				pisVector.add(newPis);
				m.put("PropertyIdentificationSet", pisVector);
			}
        } else {
        	if (tmpPis != null){
        		pisVector = new Vector<PropertyIdentificationSet>();
        		pisVector.add(tmpPis);
				m.put("PropertyIdentificationSet", pisVector);
        	}
        }
	}

}

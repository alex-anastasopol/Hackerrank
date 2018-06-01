package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.RomanNumber;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.COGenericTylerTechTR;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class COPitkinTR {
	
	private static final Pattern LEGAL_CROSS_REF_BOOK_PAGE_PATTERN = 
		Pattern.compile("BK\\s*(\\d+)\\s+PG\\s*(\\d+)");
	private static final Pattern LEGAL_PHASE_PATTERN = 
		Pattern.compile("\\bPHASE\\s+([^\\s]+)");
	private static final Pattern LEGAL_TRACT_PATTERN = 
		Pattern.compile("(?is)\\bTRACT:?\\s+([^\\s]+)");
	private static final Pattern LEGAL_STR_PATTERN = 
		Pattern.compile("(?is)\\bSection:\\s+(\\d+[A-Z]?)\\s+Township:\\s+(\\d+[A-Z]?)\\s+Range:\\s+(\\d+[A-Z]?)");

	public static void parseIntermediaryRow(COGenericTylerTechTR server, ResultMap resultMap, TableColumn[] cols) {
		for(int i=0; i <cols.length ; i++) {
			NodeList tag = HtmlParser3.getTag(cols[i].getChildren(), TextNode.class, true);
			switch(i) {
				case 0:
					if (tag.size() == 2){
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), tag.elementAt(0).toHtml());
						String amount = RegExUtils.getFirstMatch("[\\d+\\.\\,]+", tag.elementAt(1).toHtml(), 0);
						amount = StringUtils.cleanAmount(amount);
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amount);
					}
					break;
				case 1:
					if (tag.size() == 1){
						resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), tag.elementAt(0).toHtml());
					}else if (tag.size() == 2) {
						resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), tag.elementAt(0).toHtml());
						resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), tag.elementAt(1).toHtml());
					}
					break;
				case 2:
					if (tag.size() == 2) {
						resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), tag.elementAt(0).toHtml());
					}
					break;
				case 3:
					break;
			}
		}
		
		try {
			server.parseName(null, resultMap);
			server.parseAddress("", resultMap);
			server.parseLegal("", resultMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	public static void parseLegal(ResultMap resultMap, long searchId) {
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDescription);
		
		legalDescription += " ";	//we need this to be sure all regex match :)
		legalDescription = legalDescription.replaceAll("(?is)\\bLOT SPLIT\\b\\s*", "");
		legalDescription = legalDescription.replaceFirst("(?is)\\bDESC BY M/B\\b\\s*", "");
		legalDescription = legalDescription.replaceAll("(?is) \\bA TRACT IN\\b\\s*", " ");
		String legalDescriptionFake = "FAKE " + legalDescription.toUpperCase().replaceAll(":", " ");
		
		legalDescriptionFake = GenericFunctions.replaceNumbers(legalDescriptionFake);
		//String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		//legalDescriptionFake = Roman.normalizeRomanNumbersExceptTokens("FAKE SUBDIVISION  ASPEN MESA ESTATES UNIT I LOT  22 BK-0218 PG-0891", exceptionTokens); // convert roman numbers
		
		
		List<List> body = new ArrayList<List>();
		List<String> line = null;
		
		Matcher matcher = LEGAL_CROSS_REF_BOOK_PAGE_PATTERN.matcher(legalDescription);
		while (matcher.find()) {
			line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add("");
			line.add(matcher.group(1).replaceAll("^0+", ""));
			line.add(matcher.group(2).replaceAll("^0+", ""));
			line.add("");
			body.add(line);
			legalDescription = legalDescription.replace(matcher.group(), " ");
			matcher.reset(legalDescription);
		}
		
		if (legalDescription.toLowerCase().contains("subdivision:") || legalDescription.toLowerCase().contains("sub:")){
			String[] tokens = {" UNIT:", " BLK:", " BLOCK:", " LOT:", " LOTS:", " SECTION:", " TOWNSHIP:", " RANGE:"};
			String subdName = LegalDescription.extractSubdivisionNameUntilToken(legalDescription.toUpperCase(), tokens);
			
			if(StringUtils.isNotEmpty(subdName)) {
				subdName = subdName.replaceAll("(?is).*?Subdivision\\s*:", "");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), /*Roman.normalizeRomanNumbers(*/subdName.trim()/*)*/);
			}
		}
		
		String lot = LegalDescription.extractLotFromText(legalDescriptionFake);
		if(StringUtils.isNotEmpty(lot)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), Roman.normalizeRomanNumbers(lot));
		}
		
		String block = LegalDescription.extractBlockFromText(legalDescriptionFake);
		if(StringUtils.isNotEmpty(block)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), Roman.normalizeRomanNumbers(block));
		}
		
		String unit = LegalDescription.extractUnitFromText(legalDescriptionFake);
		if(StringUtils.isNotEmpty(unit)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), Roman.normalizeRomanNumbers(unit));
		}
		
		String bldg = LegalDescription.extractBuildingFromText(legalDescriptionFake);
		if(StringUtils.isNotEmpty(bldg)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), Roman.normalizeRomanNumbers(bldg));
		}
		
		matcher = LEGAL_PHASE_PATTERN.matcher(legalDescription);
		if(matcher.find()) {
			try {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), Integer.toString(RomanNumber.parse(matcher.group(1))));
				legalDescription = legalDescription.replace(matcher.group(), " ");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		matcher = LEGAL_TRACT_PATTERN.matcher(legalDescription);
		if(matcher.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), matcher.group(1));
			legalDescription = legalDescription.replace(matcher.group(), " ");
		}
		
		matcher = LEGAL_STR_PATTERN.matcher(legalDescription);
		if(matcher.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), matcher.group(1));
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), matcher.group(2));
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), matcher.group(3));
			legalDescription = legalDescription.replace(matcher.group(), " ");
		}
		
		
		//adding all cross references - should contain transfer table and info parsed from legal description
		if(body != null && body.size() > 0) {
			ResultTable rt = new ResultTable();
			String[] header = {"SalesPrice", "InstrumentDate", "InstrumentNumber", "Book", "Page" , "DocumentType"};
			rt = GenericFunctions2.createResultTable(body, header);
			resultMap.put("SaleDataSet", rt);
		}
		
	}

	@SuppressWarnings("rawtypes")
	public static void parseName(Set<String> allNames, ResultMap m) throws Exception {
		List body = new ArrayList();
		for (String name : allNames) {
			ParseNameUtil.putNamesInResultMapFromNashvilleParse(m, name, body);
		}
	}
	
	
	
}

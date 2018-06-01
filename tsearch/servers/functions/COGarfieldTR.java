package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.TableColumn;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.reports.comparators.IntegerComparator;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.types.COGenericTylerTechTR;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;

public class COGarfieldTR {

	public static void parseLegal(String contents, ResultMap m) {
		String section = RegExUtils.getFirstMatch("Section:\\s(\\d+)", contents, 1);
		String township = RegExUtils.getFirstMatch("Township:\\s(\\d+)", contents, 1);
		String range = RegExUtils.getFirstMatch("Range:\\s(\\d+)", contents, 1);
		String tract = RegExUtils.getFirstMatch("Tract:\\s*(\\d+)", contents, 1);
		String phase = RegExUtils.getFirstMatch("PHASE\\s*(\\w+)", contents, 1);
		String subdivisionName = RegExUtils.getFirstMatch("Subdivision:(.*?)(?=(Unit|Block|Lot|PHASE|\\Z))", contents, 1);
		String block = RegExUtils.getFirstMatch("(Block:|BLK)\\s*(\\w+)", contents, 2);
		String unit = RegExUtils.getFirstMatch("Unit:\\s(\\w+)", contents, 1);
		
		String filling = "";
		Pattern p1 =  Pattern.compile("(?i)(.*?)\\s+FLG\\s+([0-9]+)\\s*");
		Matcher ma1 = p1.matcher(subdivisionName);
		if (ma1.find()){
			subdivisionName = ma1.group(1);
			filling = ma1.group(2);
		}
		
		if (subdivisionName.length() != 0){
			   if(filling.length()!=0){
				   subdivisionName = (subdivisionName.trim() +" "+ "Filing " + filling.replaceAll("^0+", "")).trim();
			   }
		}
		
		m.put("PropertyIdentificationSet.SubdivisionSection", section.trim());
		m.put("PropertyIdentificationSet.SubdivisionTownship", township.trim());
		m.put("PropertyIdentificationSet.SubdivisionRange", range.trim());
		m.put("PropertyIdentificationSet.SubdivisionTract", tract.trim());
		m.put("PropertyIdentificationSet.SubdivisionName", subdivisionName.trim());
		m.put("PropertyIdentificationSet.SubdivisionBlock", block.trim());
		m.put("PropertyIdentificationSet.SubdivisionPhase", phase.trim());
		
		String existingUnit = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
		if (!StringUtils.isEmpty(existingUnit)) {
			unit = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(unit);
			unit = existingUnit.contains(unit.trim()) ? unit : unit + " " + existingUnit;
		}
		m.put("PropertyIdentificationSet.SubdivisionUnit", unit);

		setLot(m, contents);
		setRefernces(m,contents);

	}

	public static void setRefernces(ResultMap m, String contents) {
		List<String> bookPageMatches = RegExUtils.getMatches("BK\\s*\\d+\\sPG\\s\\d+" , contents, 0);
		List<HashMap<String, String>> sourceSet = new LinkedList<HashMap<String, String>>();
		String[] header = new String[] { "Book", "Page", "InstrumentNumber" };
		for (String bookPage : bookPageMatches) {
			bookPage = bookPage.replaceAll("BK\\s*(\\d+)\\sPG\\s(\\d+)", "$1 $2");
			String[] split = bookPage.split("\\s");
			if ( split.length ==2 ){
				HashMap<String, String> map = new HashMap<String, String>();
				String book = split[0];
				String page = split[1];
				map.put("Book", ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(book));
				map.put("Page", ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(page));
				sourceSet.add(map);
			}
		} 
		
		List<String> instrNoMatches = RegExUtils.getMatches("\\bREC\\s?#(\\w+)" , contents, 1);
		
		for (String instr : instrNoMatches) {
			if (StringUtils.isNotEmpty(instr)){
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("InstrumentNumber", instr);
				sourceSet.add(map);
			}
			
		}

		ResultBodyUtils.buildInfSet(m, sourceSet, header, CrossRefSet.class);

		
	}

	public static void setLot(ResultMap m, String contents) {
		// Section: 10 Township: 6 Range: 92 Subdivision: ORIGINAL TWNSTE SILT
		// Block: 16 Lot: 5 THRU:- Lot: 7 & THE N 70' OF LOTS 8 - 10
		String thruIntervalRegEx = "(?is)Lot:\\s\\d+ THRU:- Lot:\\s\\d+";
		List<String> intervals = RegExUtils.getMatches(thruIntervalRegEx, contents, 0);
		contents = contents.replace(thruIntervalRegEx, "");

		// and OF LOTS 8 - 10 see ColumbiaTR
		String dashIntervalRegEx = "(?is)LO?TS\\s*(\\d+\\s*-\\s*\\d+)";
		List<String> intervals1 = RegExUtils.getMatches(dashIntervalRegEx, contents, 1);
		contents = contents.replaceAll(dashIntervalRegEx, "");

		List<String> cleanedIntervals = new ArrayList<String>();
		String result = "";

		// clean intervals
		for (String interval : intervals) {
			String replaceAll = interval.replaceAll("(?is)Lot:\\s(\\d+) (THRU):- Lot:\\s(\\d+)", "$1 $2 $3");
			cleanedIntervals.add(replaceAll);
			result = FLColumbiaTR.enumerationFromInterval(result, replaceAll);
		}

		for (String interval : intervals1) {
			String replaceAll = interval.replaceAll("(?is)\\s*(\\d+)\\s*-\\s*(\\d+)", "$1 THRU $2");
			result += " " + FLColumbiaTR.enumerationFromInterval(result, replaceAll);
			cleanedIntervals.add(replaceAll);
		}

		// LOTS 3 4 & 5 ; get enumeration
		String regExLots = "(?is)LO?TS ([\\d+(&|\\s)]+)";
		contents = contents.replaceAll("(?is)\\sLTS\\s", " LOTS ");
		String values = ro.cst.tsearch.utils.StringUtils.getSuccessionOFValues(contents, "LOTS", regExLots);
		result += " " + values;

		String individualRegEx = "(?is)Lot:?\\s+([A-Z\\-0-9]+)";
		List<String> individualValues = RegExUtils.getMatches(individualRegEx, contents, 1);

		result += " " + Arrays.toString(individualValues.toArray()).replaceAll("(?is)\\[|\\]", "").replaceAll(",", " ");

		String[] strings = result.split("\\s+");
		Arrays.sort(strings, new IntegerComparator());
		String lotValues = ro.cst.tsearch.utils.StringUtils.addCollectionValuesToString("", strings).trim();

		m.put("PropertyIdentificationSet.SubdivisionLotNumber", lotValues);
	}

	public static void parseName(Set<String> allNames, ResultMap resultMap) throws Exception {
		Set<String> names = new HashSet<String>();

		for (String string : allNames) {
			string =string.replaceAll("1/2", "");
			string = string.replaceAll("UNDER CALIFORNIA UTMA", "");
			if (string.contains("AS CUSTODIAN OF" )){
				String[] split = string.split("AS CUSTODIAN OF");
				//reverse the names if it's necessary
				String protegeeName = split[1].trim();
				String[] split2 = protegeeName.split("\\s");
				if (split2.length == 2){
					boolean isLastName = LastNameUtils.isLastName(split2[1]);
					if (isLastName){
						protegeeName = split2[1] + " " + split2[0];
					}
				}
				names.add(protegeeName);
				string = split[0];
			}
			string = string.replaceAll("AS CUSTODIAN OF", "&");
			string = string.replaceAll("(TTEE)\\sDATED\\s\\d+/\\d+/\\d+", " $1");
			if (string.endsWith("MANAGER")) {
				string = string.replaceAll("MANAGER", "");
			}
			names.add(string);
		}
		COWeldTR.parseName(names, resultMap);
	}

	public static void parseAddress(ResultMap m, String origAddress) throws Exception {
		origAddress = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(origAddress);

		String unitRegEx = "(?is)#(\\w+)";

		if (RegExUtils.matches(unitRegEx, origAddress)) {
			String firstMatch = RegExUtils.getFirstMatch(unitRegEx, origAddress, 1);
			firstMatch = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(firstMatch);
			m.put("PropertyIdentificationSet.SubdivisionUnit", firstMatch);
			origAddress = origAddress.replaceAll(unitRegEx, "");
		}

		origAddress = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(origAddress);
		if (origAddress.contains(",")) {
			String[] split = origAddress.split(",");
			origAddress = split[0];
		}
		COWeldTR.parseAddress(m, origAddress);
	}

	public static void parseIntermediaryRow(COGenericTylerTechTR server, ResultMap resultMap, TableColumn[] cols) throws Exception {
		for(int i=0; i <cols.length ; i++) {
			if (cols.length == 6) {
				String contents = "";
				Node colText = HtmlParser3.getFirstTag(cols[i].getChildren(), TextNode.class, true);
				if(colText!=null) {
					contents = COWeldTR.clean(colText.getText(),false,false);	
				}
			
				switch(i) {
				case 0:
					resultMap.put("PropertyIdentificationSet.ParcelID2",contents);
					break;
				case 3:
					resultMap.put("PropertyIdentificationSet.StreetName",contents);
					break;
				case 4:
					List<String> asList = Arrays.asList(contents.split("&"));
					List<String> newList = new ArrayList<String>();
				
					if (asList.size() == 2) {
						String newValue = asList.get(0) + " AND " + asList.get(1);
						newList.add(newValue);
						asList =  newList;
					}
					server.parseName(new HashSet<String>(asList),resultMap);
					break;
				case 5:
					server.parseLegal(contents,resultMap);
					break;
				}
			}
		}			
	}

}

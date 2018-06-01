package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;


import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;


/**
 * @author mihaib
*/

public class TXGenericSWData {
		
	
	@SuppressWarnings("unchecked")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", "").replaceAll("(?is)&amp;", "&")
								.replaceAll("(?is)<th\\b", "<td").replaceAll("(?is)</th\\b", "</td");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null); 
					
			String geoNo = "";
			try {
				geoNo = HtmlParser3.getNodeByID("ucidentification_webprop_geoid",	mainList, true).getChildren().toHtml();
			} catch (Exception e) {
				geoNo = HtmlParser3.getNodeByID("ucidentification_webprop_id", mainList, true).getChildren().toHtml();
			}
			
			if (StringUtils.isNotEmpty(geoNo)) {
				m.put("PropertyIdentificationSet.ParcelID2", geoNo.trim());
			}
			
			String pidn = HtmlParser3.getNodeByID("ucidentification_webprop_id", mainList, true).getChildren().toHtml();
			if (StringUtils.isNotEmpty(pidn)){
				m.put("PropertyIdentificationSet.ParcelID", pidn.trim());
			}
			
			String ownerName = HtmlParser3.getNodeByID("webprop_name", mainList, true).getChildren().toHtml();
			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName : "");
			
			String legal = HtmlParser3.getNodeByID("webprop_desc", mainList, true).getChildren().toHtml();
			if (StringUtils.isNotEmpty(legal)){
				legal = legal.replaceAll("(?is)\\blegal\\s*:?", "");
				m.put("tmpLegal", legal);
			}
			
			String siteAddress = HtmlParser3.getNodeByID("webprop_situs", mainList, true).getChildren().toHtml();
			if (StringUtils.isNotEmpty(siteAddress)){
				siteAddress = siteAddress.replaceAll("(?is)\\bsitus\\s*:?", "");
				m.put("tmpAddress", siteAddress);
			}
			
			String landAppraisal = HtmlParser3.getValueFromAbsoluteCell(1, 2, HtmlParser3.findNode(mainList, "Improvements"), "", true);
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.LandAppraisal", landAppraisal.trim());
			
			String improvementAppraisal = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(mainList, "Improvements"), "", true);
			improvementAppraisal = improvementAppraisal.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.ImprovementAppraisal", improvementAppraisal.trim());
			
			String assessed = HtmlParser3.getValueFromAbsoluteCell(8, 2, HtmlParser3.findNode(mainList, "Improvements"), "", true);
			assessed = assessed.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.TotalAssessment", assessed.trim());
			
			String taxYear = HtmlParser3.getValueFromAbsoluteCell(1, -2, HtmlParser3.findNode(mainList, "Base Tax"), "", true).trim();
			m.put("TaxHistorySet.Year", StringUtils.isNotEmpty(taxYear) ? taxYear : "");
			
			String baseAmount = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Base Tax"), "", true).trim();
			baseAmount = baseAmount.replaceAll("[\\$,]+", "");
			m.put("TaxHistorySet.BaseAmount", baseAmount);
			
			String amountPaid = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "Base Tax"), "", true).trim();
			amountPaid = amountPaid.replaceAll("[\\$,]+", "");
			m.put("TaxHistorySet.AmountPaid", amountPaid);
			
			String totalDue = HtmlParser3.getValueFromAbsoluteCell(1, 5, HtmlParser3.findNode(mainList, "Base Tax"), "", true).trim();
			totalDue = totalDue.replaceAll("[\\$,]+", "");
			m.put("TaxHistorySet.TotalDue", totalDue);

			String saleHist = "", taxHistory = "";
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			for (int k = 0; k < tables.size(); k++){
				if (tables.elementAt(k).toHtml().contains("Deed Date")){
					saleHist = tables.elementAt(k).toHtml();
				} else if (tables.elementAt(k).toHtml().contains("Base Tax")){
					taxHistory = tables.elementAt(k).toHtml();
				}
			}

			String delinqAmount = "";
			List<List<String>> taxHist = HtmlParser3.getTableAsList(taxHistory, false);
			for (int i = 1; i < taxHist.size() - 1; i++){
				delinqAmount += taxHist.get(i).get(7).replaceAll("(?is)[\\$,]+", "").trim() + "+";
			}
			
			m.put("TaxHistorySet.PriorDelinquent", GenericFunctions2.sum(delinqAmount, searchId));
			
			List<List<String>> transacHist = HtmlParser3.getTableAsList(saleHist, false);
			List<List> newBody = new ArrayList<List>(transacHist);
			if (newBody != null){
				for (List lst : newBody){
					lst.remove(0);
					
					lst.set(0, lst.get(0).toString().replaceAll("\\A0+", ""));
					lst.set(1, lst.get(1).toString().replaceAll("\\A0+", ""));
				}
				
				ResultTable rt = new ResultTable();
				String[] header = { "Book", "Page", "InstrumentDate", "InstrumentNumber" };
				rt = GenericFunctions2.createResultTable(newBody, header);
				m.put("SaleDataSet", rt);
			}
			
			try {
				parseAddressTXGenericSWData(m, searchId);
				parseLegalTXGenericSWData(m, searchId);
				partyNamesTXGenericSWData(m, searchId);

			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	

	public static ResultMap parseIntermediaryRowTXGenericSWData(TableRow row, long searchId, int miServerId) throws Exception {
		
		ResultMap resultMap = new ResultMap();
		//String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
		
		TableColumn[] cols = row.getColumns();
		int count = 0;
		String contents = "";
		NodeList nList = null;
		for(TableColumn col : cols) {
			//System.out.println(col.toHtml());
			if (count < 7){
				switch (count) {
				case 1:
					nList = col.getChildren();
					if (nList != null){
						contents = nList.toHtml();
						resultMap.put("PropertyIdentificationSet.ParcelID", contents.trim());
					}

					break;
				case 2:
					nList = col.getChildren();
					if (nList != null){
						contents = nList.toHtml();
						resultMap.put("PropertyIdentificationSet.ParcelID2", contents.trim());
					}
					break;
				case 3:
					nList = col.getChildren();
					if (nList != null){
						contents = nList.toHtml();
						resultMap.put("tmpOwner", contents);
					}
					break;
				case 4:
					nList = col.getChildren();
					if (nList != null){
						contents = nList.toHtml();
						resultMap.put("tmpAddress", contents.trim());
					}
					break;
				case 5:
					nList = col.getChildren();
					if (nList != null){
						contents = nList.toHtml();
						resultMap.put("PropertyIdentificationSet.PropertyDescription", contents.trim());
					}
					break;
				default:
					break;
				}
				count++;
					
			}
		}
		
		partyNamesTXGenericSWData(resultMap, searchId);
		parseAddressTXGenericSWData(resultMap, searchId);
		
		return resultMap;
	}
	
	@SuppressWarnings("unchecked")
	public static void partyNamesTXGenericSWData(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = GenericFunctions2.cleanOwnerNameFromPrefix(stringOwner);
		stringOwner = GenericFunctions2.resolveOtherTypes(stringOwner);
		
		//TO DO: ATTN case 				
		stringOwner = stringOwner.replaceAll("\\bATTN: C/O\\b", "");

		stringOwner = stringOwner.replaceAll("(?is)[^L]/[A-Z]\\b", "");
		stringOwner = stringOwner.replaceAll("\\s*&\\s*(ETAL)\\s*$", " $1");
		stringOwner = stringOwner.replaceAll("[\\(\\)]+", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bWI?FE?\\b", " ");
		stringOwner = stringOwner.replaceAll("(?is)\\b(FAMILY\\s+TR)\\b", "$1UST");
		//TO DO: ATTN case 				
		stringOwner = stringOwner.replaceAll("\\bATTN", "");		
		stringOwner = stringOwner.replaceAll("(?is)\\bDECD", "");
		stringOwner = stringOwner.replaceAll("(?is)/\\s*DMR\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s+-\\s*\\*\\s*SUSPENSE\\s*\\*\\s*", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bMRS", "");
		stringOwner = stringOwner.replaceAll("(?i)(\\w+\\s+\\w+(?:\\s+\\w+)?)\\s*,\\s*(\\w+\\s+\\w+)", "$1 AND $2");
		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*$", "");
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes, types, otherTypes;
		//String ln = "";
		//boolean coOwner = false;

		String[] owners = stringOwner.split(" & ");
		if (stringOwner.matches("(?is)\\A[A-Z]\\s*&.*") || 
				(stringOwner.matches("(?is)\\A[A-Z]+\\s+[A-Z]+(\\s+[A-Z]+)?(\\s+[A-Z]+)?\\s*&\\s*[A-Z]+(\\s+[A-Z]+)?") || NameUtils.isCompany(stringOwner))){
			owners = stringOwner.split("@@@@@@");
		}

		for (int i = 0; i < owners.length; i++) {
			names = StringFormats.parseNameNashville(owners[i], true);
			//21743.009.000.00
			if (names[1].trim().matches("[A-Z]\\s+[A-Z]") && names[0].trim().matches("I+")){
				String temp = names[0];
				names[0] = names[1].replaceAll("([A-Z])\\s+[A-Z]", "$1");
				names[1] = names[1].replaceAll("([A-Z])\\s+([A-Z])", "$2 ") + temp;
			}
			boolean twoPersonSameName = false;
			if (StringUtils.isNotEmpty(names[5]) && names[2].trim().equals(names[5].trim())){
				twoPersonSameName = true;
			}
			if (StringUtils.isNotEmpty(names[5]) && 
					LastNameUtils.isNotLastName(names[5]) && 
					NameUtils.isNotCompany(names[5]) && !twoPersonSameName){
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
			} 
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
											NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner);	
	}
	
	public static void parseLegalTXGenericSWData(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get("tmpLegal");
				
		if (StringUtils.isEmpty(legal))
			return;
		
		m.put("PropertyIdentificationSet.PropertyDescription", legal);
		
		legal = legal.replaceAll("(?is)&quot;", "\"");
		legal = legal.replaceAll("(?is)[\\)\\(]+", "");
		legal = legal.replaceAll("(?is)\\bPER SURVEY\\b", "");
		legal = legal.replaceAll("(?is)\\\"[^\\\"]+\\\"", "");
		legal = legal.replaceAll("(?is)[A-Z]\\s+[/\\d\\sX]+\\s*OF\\s+", "");
		legal = legal.replaceAll("(?is)\\s+(TR(ACT)?)\\b", " , $1");
		legal = legal.replaceAll("(?is)\\s+(PH(ASE)?)\\b", " , $1");
		//legal = legal.replaceAll("(?is)\\s+[NSEW]\\s*/\\s*\\d+\\s*OF\\s*(\\d+)", " L $1");
		//legal = legal.replaceAll("(?is)(\\w),(\\w)\\s+(\\w)", "$1,$2,$3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");
		legal = legal.replaceAll("(?is)\\bSALES CONTRACT\\b", "");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		String legalTemp = legal;
			
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LOTS?\\s*:)\\s*([^,]+)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2).trim().replaceAll("\\A0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").replaceAll("\\b([NSWE]{1,2}\\s+)?PT(\\s+LOT)?\\b", "").replaceAll("\\bLOTS?\\b", "")
					.replaceAll("\\bLEASEHOLD\\b", "").replaceAll("(?is)[\\.\\d]+\\s+OF\\b", "").replaceAll("(?is)[A-Z]\\s+[/\\d]+", "").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLKS?\\s*:)\\s*([^,]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2).trim().replaceAll("\\A0+", "");
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").replaceAll("\\bOF\\s+\\d+", "").trim();
		//19035.038.013.10
		block = block.replaceAll("(\\d/\\d)$", "");
		
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(A\\s*-|Abst:|AB\\s+)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.AbsNo", ma.group(2).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(2), "");
		}
		
		p = Pattern.compile("(?is)\\b(Acres)\\s*([^,]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.Acres", ma.group(2).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(2), "");
		}
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UNIT)\\s*#\\s*([\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH)\\s+([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		p = Pattern.compile("(?is)\\b(SEC?(?:TION)?)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2));
		}
		
		// extract section from legal description
		p = Pattern.compile("(?is)\\bTR(?:ACT)?\\s*:\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(1));
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
				
		// extract subdivision name from legal description
		String subdiv = "";

		p = Pattern.compile("(?is)\\b(Subd\\s*:)\\s*([^,]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(2);
		}
		
		if (subdiv.length() != 0) {
		
			subdiv = subdiv.replaceFirst(",", "");
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			
			if (legal.matches(".*\\bCOND.*"))
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		}

		
	}
	
	public static void parseAddressTXGenericSWData(ResultMap m, long searchId) throws Exception {
		
		String address = (String) m.get("tmpAddress");
		
		if (StringUtils.isEmpty(address))
			return;
				
		if (address.trim().matches("0+"))
			return;
		
		address = address.replaceAll("\\s+0+\\s*$", "");//CROSSHAIR CT 0  on Parker  11774.001.045.00
		
		address = address.replaceAll("(.*)\\s+(\\d+(?:\\s+[A-Z]+)?)\\s*$", "$2 $1");
		
		m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address.trim()));
		m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address.trim()));
		
	}
	
}

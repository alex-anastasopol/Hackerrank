package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class TXJohnsonAO {
	
	@SuppressWarnings("unchecked")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)<CAPTION[^>]*>", "<tr><td>").replaceAll("(?is)</CAPTION[^>]*>", "</td></tr>")
									.replaceAll("(?is)<th", "<td").replaceAll("(?is)</th>", "</td>").replaceAll("\n", "");
		m.put("OtherInformationSet.SrcType","AO");
		try {		
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			String pid = htmlParser3.getNodeByAttribute("class", "page_title", true).toHtml();
			pid = pid.replaceAll("</?div[^<]*>", "").replaceAll("(?is)\\s*Account\\s+Details\\s+for\\s*", "").trim();
			m.put("PropertyIdentificationSet.ParcelID", StringUtils.isNotEmpty(pid) ? pid : "");
			m.put("PropertyIdentificationSet.ParcelID2", StringUtils.isNotEmpty(pid.replaceAll("\\.", "")) ? pid : "");

			String ownerName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Owner Name:"), "", true).trim();
			ownerName = ownerName.replaceAll("(?is)</?span[^>]*>", "").replaceAll("(?is)\r\n", "").replaceAll("(?is)<br>", "@@@");
			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName : "");
			
			String siteAddress = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Property Location:"), "", true).trim();
			if (StringUtils.isNotEmpty(siteAddress)){
				siteAddress = siteAddress.replaceAll("(\\d)-([A-Z])", "$1$2");
				m.put("tmpAddress", siteAddress);
			}
			
			String legal = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Description:"), "", true).trim();
			m.put("PropertyIdentificationSet.PropertyDescription", StringUtils.isNotEmpty(legal) ? legal : "");
			
			String page = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Page #:"), "", true)
												.replaceAll("(?is)</?span[^>]*>", "").replaceAll("[\\$,]+", "").trim();
			
			String volume = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Volume #:"), "", true)
											.replaceAll("(?is)</?span[^>]*>", "").replaceAll("[\\$,]+", "").trim();

			List<List> body = new ArrayList<List>();
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(page.replaceAll("(?is)\\A\\s*[0]+", ""));
			line.add(volume.replaceAll("(?is)\\A\\s*[0]+", ""));
			body.add(line);
			
			if (body != null){
				ResultTable rt = new ResultTable();
				String[] header = { "InstrumentNumber", "Book", "Page" };
				rt = GenericFunctions2.createResultTable(body, header);
				m.put("SaleDataSet", rt);
			}
			
			String landAppraisal = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Land Market Value:"),"", true).trim();
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.LandAppraisal", landAppraisal);
			
			String improvementAppraisal = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Improvement Value"),"", true).trim();
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "");
			m.put("PropertyAppraisalSet.ImprovementAppraisal", improvementAppraisal);
			
			String assessed = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Appraised Value:"),"", true).trim();
			assessed = assessed.replaceAll("(?is)([\\d\\.]+).*", "$1");
			m.put("PropertyAppraisalSet.TotalAssessment", assessed);
			
			try {
				partyNamesTXJohnsonAO(m, searchId);
				parseLegalTXJohnsonAO(m, searchId);
				parseAddressTXJohnsonAO(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static void parseAddressTXJohnsonAO(ResultMap resultMap, long searchId) {

		String address = (String) resultMap.get("tmpAddress");
			
		if (StringUtils.isEmpty(address))
			return;
				
		resultMap.put("PropertyIdentificationSet.AddressOnServer", address);
		resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address.trim()));
		resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address.trim()).replaceAll("\\A0+", ""));
		
	}
	
	@SuppressWarnings("unchecked")
	public static void partyNamesTXJohnsonAO(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = stringOwner.toUpperCase();
		stringOwner = stringOwner.replaceAll("(?is)\\b[A-Z]/[A-Z]/[A-Z]\\b", "&");
		stringOwner = stringOwner.replaceAll("\\s*%\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\D/\\D", " & ");
		stringOwner = stringOwner.replaceAll("\\d+%", "");
		stringOwner = stringOwner.replaceAll("\\bINT\\b", "");
		stringOwner = stringOwner.replaceAll("\\bAS (TRUSTEE) OF THE\\b", " $1");
		stringOwner = stringOwner.replaceAll("(?is)\\b-\\s*(TR?U?STEES?)\\b", " $1");
		stringOwner = stringOwner.replaceAll("(?is)\\b-\\s*(TR)\\b", " $1");
		stringOwner = stringOwner.replaceAll("\\bTHE UND\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s+Heirs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bMrs\\b", "");
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] coNames = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		String[] owners = stringOwner.split("\\s*@@@\\s*");
		for (int i = 0; i < owners.length; i++) {
			names = StringFormats.parseNameNashville(owners[i], true);
			
			if (stringOwner.trim().matches("\\w+\\s+\\w+\\s+\\w+\\s+\\w+\\s+\\w+") && NameUtils.isNotCompany(stringOwner)){//126.3636.02610
				if (!names[1].matches(GenericFunctions1.nameOtherType.toString())){
					names[2] = names[2] + " " + names[0];
					names[0] = names[1].replaceAll("(\\w+)\\s+\\w+.*", "$1");
					names[1] = names[1].replaceAll("\\w+\\s+(\\w+)", "$1");
				}
			}
			names[2] = names[2].replaceAll("(?is),", "");
			if (NameUtils.isNotCompany(names[2])){
				if (stringOwner.matches("[^&]+&\\s+\\w+\\s+\\w{3,}(?:\\s+\\w+)?")){
					String coOwner = stringOwner.replaceAll("[^&]+&\\s+(\\w+\\s+\\w{3,}(?:\\s+\\w+)?)", "$1");
					coNames = StringFormats.parseNameDesotoRO(coOwner, true);
					names[3] = coNames[0];
					names[4] = coNames[1];
					names[5] = coNames[2];
				}
			}
			if (names[3].toLowerCase().equals("betty") && names[5].toLowerCase().equals("ann")){
				names[4] = names[5];
				names[5] = names[2];
			}
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
												NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		stringOwner = stringOwner.replaceAll("\\s*@@@\\s*", " AND ");
		
		String[] a = StringFormats.parseNameNashville(stringOwner, true);
		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
		
		m.put("PropertyIdentificationSet.NameOnServer", stringOwner);
		
	}
	
	public static void parseLegalTXJohnsonAO(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
				
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("\\d+\\.\\d+\\.\\d+", "");
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s*([\\d\\s]+[A-Z]?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLKS?)\\s*(\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(UNITS?\\s*:?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\bABST\\s+([\\d+\\s&]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.AbsNo", ma.group(1).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(1), "");
		}
		
		p = Pattern.compile("(?is)\\b(?:TR|TRACT)\\s+([\\dA-Z,]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legal = legal.replaceAll(ma.group(0), "");
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(1).replaceAll("\\s*,\\s*", " ").trim());
		}
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		String subdiv = "";
		legal = legal.replaceAll("<BR>", "<br>");
		String[] legalRows = legal.split("<br>");
		if (legalRows.length > 1){
			if (!legalRows[0].contains("GAS WELL")){
				if ((legalRows[1].contains("TR ") || legalRows[1].contains("BLK ") || legalRows[1].contains("ABST ") || legalRows[1].contains("LOT "))
							&& (!legalRows[1].trim().matches("[\\.\\d]+"))){
					if (legalRows.length > 2){
						subdiv = legalRows[2];
					}
				} else {
					if (!legalRows[0].contains("MOBILE HOME")){
						subdiv = legalRows[1];
					}
				}
			}
		}
		subdiv = subdiv.replaceAll("(.+)\\s+(SEC|PH).*", "$1");
		m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
		legal = legal.replaceAll("", "");
		
	}
	
}

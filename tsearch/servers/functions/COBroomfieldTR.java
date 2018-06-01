package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class COBroomfieldTR {
		
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		
		m.put("OtherInformationSet.SrcType","TR");
		m.put("PropertyIdentificationSet.City","Broomfield");
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		int commId = currentInstance.getCommunityId();
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			Date paidDate = HashCountyToIndex.getPayDate(commId, miServerID);
			if (paidDate != null){
				String year = paidDate.toString().replaceAll(".*?\\s+(\\d{4})", "$1").trim();
				if (StringUtils.isNotEmpty(year)){
				int taxYear = Integer.parseInt(year) - 1;
				m.put("TaxHistorySet.Year", Integer.toString(taxYear));
				}
			}
						
			String pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Tax Account Number"),"", true)
								.replaceAll("</?font[^>]*>", "").trim();
			m.put("PropertyIdentificationSet.ParcelID", StringUtils.isNotEmpty(pid) ? pid : "");
			
			String ownerName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Owner Name"),"", true)
								.replaceAll("</?font[^>]*>", "").trim();
			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName : "");
			
			String siteAddress = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Site Address"),"", true)
									.replaceAll("</?font[^>]*>", "").trim();
			if (StringUtils.isNotEmpty(siteAddress)){
				if (siteAddress.matches("\\d+\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\s+.*")){
					siteAddress = siteAddress.replaceAll("(\\d+)\\s+([A-Z]?\\d+[A-Z]?|[A-Z])\\s+(.*)", "$1 $3 $2");//Parcel No: 157320301046
				}
				m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(siteAddress));
				m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(siteAddress));
			}
			
			String legal = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Legal Information"),"", true)
									.replaceAll("</?font[^>]*>", "").trim();
			m.put("PropertyIdentificationSet.PropertyDescription", StringUtils.isNotEmpty(legal) ? legal : "");
			
			
			String baseAmount = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Current Year Tax Amount"),"", true)
									.replaceAll("</?font[^>]*>", "").replaceAll("[\\$,]+", "").trim();
			m.put("TaxHistorySet.BaseAmount", StringUtils.isNotEmpty(baseAmount) ? baseAmount : "");
			String amountPaid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Amount Paid"),"", true)
									.replaceAll("</?font[^>]*>", "").replaceAll("[\\$,]+", "").trim();
			m.put("TaxHistorySet.AmountPaid", StringUtils.isNotEmpty(amountPaid) ? amountPaid : "");
			String delinquenAmount = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Delinquent Amount"),"", true)
			.replaceAll("</?font[^>]*>", "").replaceAll("[\\$,]+", "").trim();
			
			m.put("TaxHistorySet.PriorDelinquent", StringUtils.isNotEmpty(delinquenAmount) ? delinquenAmount : "");
			
			double amtPaid = Double.parseDouble(org.apache.commons.lang.StringUtils.defaultIfEmpty(amountPaid, "0.0"));
			double basAmt = Double.parseDouble(org.apache.commons.lang.StringUtils.defaultIfEmpty(baseAmount,"0.0"));
			double deliq = Double.parseDouble(org.apache.commons.lang.StringUtils.defaultIfEmpty(delinquenAmount,"0.0"));
			m.put("TaxHistorySet.TotalDue", Double.toString(basAmt - amtPaid));
			
			try {
				partyNamesCOBroomfieldTR(m, searchId);
				parseLegalCOBroomfieldTR(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	

	@SuppressWarnings("unchecked")
	public static void partyNamesCOBroomfieldTR(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = stringOwner.replaceAll("\\s*%\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\s*\\(\\s*", " ");
		stringOwner = stringOwner.replaceAll("\\s*\\)\\s*", " ");
		stringOwner = stringOwner.replaceAll("&\\s*WF", "");
		stringOwner = stringOwner.replaceAll("\\s*&\\s*(\\w+)\\s+(\\w)", " AND $1 $2");
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		//String ln = "";
		//boolean coOwner = false;
		String[] owners = stringOwner.split(" & ");
		
		for (int i = 0; i < owners.length; i++) {
			if (i == 0) {
				names = StringFormats.parseNameNashville(owners[i], true);
				if (StringUtils.isNotEmpty(names[5]) && 
						LastNameUtils.isNotLastName(names[5]) && 
						NameUtils.isNotCompany(names[5])){
					names[4] = names[3];
					names[3] = names[5];
					names[5] = names[2];
				}
				//ln = names[2];
			} else {
				names = StringFormats.parseNameDesotoRO(owners[i], true);
				//coOwner = true;
			}
			// B 5003
			if (stringOwner.matches("\\A\\w+\\s+\\w\\s+.*")){
				String[] toks = names[1].split("\\s+");
				if (toks[0].length() == 1){
					String temp = names[0];
					names[0] = toks[0];
					names[1] = temp + " " + names[1].replaceAll(toks[0] + "\\s+", "");
				}
			}
			//B 5003
			if (stringOwner.matches(".*\\s+AND\\s+\\w\\s+\\w+") && names[4].length() == 1 && names[3].length() > 1){
				String temp = names[3];
				names[3] = names[4];
				names[4] = temp;
			}
			names[2] = names[2].replaceAll("(?is),", "");
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			   
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		String[] a = StringFormats.parseNameNashville(stringOwner, true);
		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
		
		m.put("PropertyIdentificationSet.NameOnServer", stringOwner);
		
	}
	
	@SuppressWarnings("unchecked")
	public static void parseLegalCOBroomfieldTR(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
				
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = legal.replaceAll("<br>", " ").replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		List<List> body = new ArrayList<List>();
		List<String> line = new ArrayList<String>();
		
		Pattern p = Pattern.compile("(?is)\\bBK\\s*:\\s*(\\d+)\\s*PG\\s*:\\s*([\\d-]+)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()){
			if (ma.group(2).contains("-")) {
				String[] intervalLimits = ma.group(2).split("-");
				if (intervalLimits.length == 2) {
					int leftLimit = Integer.valueOf(intervalLimits[0].trim());
					int rightLimit = Integer.valueOf(intervalLimits[1].trim());
					if ((rightLimit - leftLimit > 0) && (rightLimit - leftLimit <= 9)) {
						for (int i = leftLimit; i <= rightLimit; i++) {
							line = new ArrayList<String>();
							line.add(ma.group(1));
							line.add(Integer.toString(i));
							line.add("");
							body.add(line);	
						}
					}
				}
			} else {
				line = new ArrayList<String>();
				line.add(ma.group(1));
				line.add(ma.group(2));
				line.add("");
				body.add(line);
			}
		}
		
		p = Pattern.compile("(?is)\\bRE?CPT?\\s*:\\s*([A-Z]?[\\d-]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()){
			String instrNr = ma.group(1);
			line = new ArrayList<String>();
			line.add(instrNr.substring(0,4));
			line.add("");
			line.add("");
			line.add(instrNr);
			body.add(line);	
		}
		String srcType = (String)m.get("OtherInformationSet.SrcType");
	    if (srcType == null){
	        srcType = "";
	    }
	    
		if (!body.isEmpty()){
			String[] header = { "InstrumentDate", "Book", "Page", "InstrumentNumber" };
			ResultTable rt = GenericFunctions2.createResultTable(body, header);
			if (Bridge.isAssessorSite(srcType)){
				m.put("SaleDataSet", rt);//for COBroomfieldAO
			} else {
				m.put("SaleDataSet", rt);//for COBroomfieldTR
			}
		}
		
		/*body = new ArrayList<List>();
		line = new ArrayList<String>();
		
		p = Pattern.compile("(?is)\\bRE?CPT?\\s*:\\s*([A-Z]?[\\d-]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()){
			line.add(ma.group(1));
			body.add(line);	
		}
		if (!body.isEmpty()){
			String[] header = { "ReceiptNumber"};
			ResultTable rt = GenericFunctions2.createResultTable(body, header);
			m.put("TaxHistorySet", rt);
		}*/
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
		if (lot == null){
			lot = "";
		}
		p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
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
		String block = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
		if (block == null){
			block = "";
		}
		p = Pattern.compile("(?is)\\b(BLKS?\\s*:)\\s*(\\d+)\\b");
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
		
		p = Pattern.compile("(?is)\\b(UNIT\\s*:)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
				
		String section = "";
		String township = "";
		String range = "";
		p = Pattern.compile("(?is)\\bSECT\\s*,\\s*TWN\\s*,\\s*RNG\\s*:\\s*(\\d+)\\s*-\\s*(\\d+[A-Z])\\s*-\\s*(\\d+[A-Z]?)\\b");
		Matcher mp = p.matcher(legal);
		if (mp.find()) {
			section = mp.group(1);
			township = mp.group(2);
			range = mp.group(3);
			m.put("PropertyIdentificationSet.SubdivisionSection", section);
			m.put("PropertyIdentificationSet.SubdivisionTownship", township);
			m.put("PropertyIdentificationSet.SubdivisionRange", range);
		}

		legal = legal.replaceAll("", "");
		
		// extract subdivision name from legal description
		String subdiv = "";
		p = Pattern.compile("(?is)\\b(?:SUB|CONDO)\\s*:\\s*([^:]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1).trim();
			subdiv = subdiv.replaceAll("\\w+$", "");
			if (subdiv.contains(",")){
				subdiv = "";
			}
		}	
		subdiv = subdiv.replaceAll("&", " & ");
		
		if (subdiv.length() != 0) {
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			
			if (legal.matches(".*\\bCONDO.*"))
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		}
		
	}
	
}

package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
  */
public class ILWillTR {

	public static void parseAndFillResultMap(String rsResponse, ResultMap m, long searchId) {
		try {
			if (rsResponse != null) {

				HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
				NodeList nodeList = htmlParser3.getNodeList();
				String parcelId = "";

				// get parcel Id
				parcelId = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Permanent Index Number"), "", true);
				parcelId = parcelId.replaceAll("<[^>]*>", "").trim();
				if (StringUtils.isNotEmpty(parcelId)) {
					m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelId);
				}
				parcelId = parcelId.replaceAll("-", "");
				if (StringUtils.isNotEmpty(parcelId)) {
					m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), parcelId);
					m.put(PropertyIdentificationSetKey.PARCEL_ID3.getKeyName(), parcelId);
				}

				// get year
				NodeList yearNodes = nodeList.extractAllNodesThatMatch(new RegexFilter("(?is)Levy\\s+Real\\s+Estate"), true);
				if (yearNodes.size() > 0) {
					Node yearNode = yearNodes.elementAt(0);
					String year = StringUtils.extractParameter(yearNode.toPlainTextString(), "(?s)^\\s*(\\d+).*");
					if (StringUtils.isNotEmpty(year)) {
						m.put(TaxHistorySetKey.YEAR.getKeyName(), year);
					}

				}

				// get address
				String address = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Property Address"), "", true)
						.replaceAll("<[^>]*>", "").trim();
				if (StringUtils.isNotEmpty(address)) {
					m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				}
				String cityZip = HtmlParser3.getValueFromAbsoluteCell(2, 0, HtmlParser3.findNode(nodeList, "Property Address"), "", true)
						.replaceAll("<[^>]*>", "").trim();
				if (StringUtils.isNotEmpty(cityZip)) {
					String city = StringUtils.extractParameter(cityZip, "(.*)\\s+\\d+");
					if (StringUtils.isNotEmpty(cityZip)) {
						m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
					}
					String zip = StringUtils.extractParameter(cityZip, ".*\\s+(\\d+)");
					if (StringUtils.isNotEmpty(zip)) {
						m.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
					}
				}

				// get base amount
				String baseAmount = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Total Base Tax"), "", true)
						.replaceAll("[^\\d\\.]", "");

				if (StringUtils.isNotEmpty(baseAmount)) {
					m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
				}

				String ownerLine1 = HtmlParser3.getValueFromAbsoluteCell(2, 0, HtmlParser3.findNode(nodeList, "Owner Information"), "", true)
						.replaceAll("<[^>]*>", "").trim();
				if (StringUtils.isNotEmpty(ownerLine1)) {
					m.put("tmpOwnerName", ownerLine1);
				}
				String ownerLine2 = HtmlParser3.getValueFromAbsoluteCell(3, 0, HtmlParser3.findNode(nodeList, "Owner Information"), "", true)
						.replaceAll("<[^>]*>", "").trim();
				if (StringUtils.isNotEmpty(ownerLine2) && ownerLine2.matches("[^\\d]+")) {
					m.put("tmpOwnerNameExtra", ownerLine2);
				}

				// get intallments base amount
				String installmentBaseAmount = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Base Tax Amount"),
						"", true).replaceAll("<[^>]*>", "").replaceAll("[^\\d\\.]", "");
				if (StringUtils.isNotEmpty(installmentBaseAmount)) {
					m.put("tmpFirstInstalAmount", installmentBaseAmount);
				}
				installmentBaseAmount = HtmlParser3.getValueFromAbsoluteCell(2, 0, HtmlParser3.findNode(nodeList, "Base Tax Amount"),
						"", true).replaceAll("<[^>]*>", "").replaceAll("[^\\d\\.]", "");
				if (StringUtils.isNotEmpty(installmentBaseAmount)) {
					m.put("tmpSecInstalAmount", installmentBaseAmount);
				}

				// get intallments amount paid
				String installmentPaid = HtmlParser3.getValueFromAbsoluteCell(1, 2, HtmlParser3.findNode(nodeList, "Base Tax Amount"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("[^\\d\\.]", "");
				if (StringUtils.isNotEmpty(installmentPaid)) {
					m.put("tmpFirstInstalPaid", installmentPaid);
				}
				installmentPaid = HtmlParser3.getValueFromAbsoluteCell(2, 2, HtmlParser3.findNode(nodeList, "Base Tax Amount"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("[^\\d\\.]", "");
				if (StringUtils.isNotEmpty(installmentPaid)) {
					m.put("tmpSecInstalPaid", installmentPaid);

				}

				// get intallments amount due
				String installmentTotalDue = HtmlParser3.getValueFromAbsoluteCell(1, 4, HtmlParser3.findNode(nodeList, "Base Tax Amount"), "",
						true).replaceAll("[^\\d\\.]", "");
				if (StringUtils.isNotEmpty(installmentTotalDue)) {
					m.put("tmpFirstInstalDue", installmentTotalDue);
				}
				installmentTotalDue = HtmlParser3.getValueFromAbsoluteCell(2, 4, HtmlParser3.findNode(nodeList, "Base Tax Amount"), "",
						true).replaceAll("[^\\d\\.]", "");
				if (StringUtils.isNotEmpty(installmentTotalDue)) {
					m.put("tmpSecInstalDue", installmentTotalDue);

				}

				// get intallments date paid
				String installmentDatePaid = HtmlParser3.getValueFromAbsoluteCell(1, 3, HtmlParser3.findNode(nodeList, "Base Tax Amount"), "",
						true).replaceAll("<[^>]*>", "").trim();
				if (StringUtils.isNotEmpty(installmentDatePaid)) {
					m.put("tmpFirstInstalDatePaid", installmentDatePaid);
				}
				installmentDatePaid = HtmlParser3.getValueFromAbsoluteCell(2, 3, HtmlParser3.findNode(nodeList, "Base Tax Amount"), "",
						true).replaceAll("<[^>]*>", "").trim();
				if (StringUtils.isNotEmpty(installmentDatePaid)) {
					m.put("tmpSecInstalDatePaid", installmentDatePaid);
				}
				
				String taxAreaCode = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Tax Code"), "",
						true).replaceAll("<[^>]*>", "").trim();
				if (StringUtils.isNotEmpty(taxAreaCode)) {
					m.put(PropertyIdentificationSetKey.AREA_CODE.getKeyName(), taxAreaCode);
				}

				String totalAssessment = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Assessed Value"), "",
						true).replaceAll("<[^>]*>", "").replaceAll("[^\\d\\.]", "");
				if (StringUtils.isNotEmpty(totalAssessment)) {
					m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAssessment);
				}
				String exemption = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Exemptions"), "",
						true).replaceAll("<[^>]*>", "").replaceAll("[^\\d\\.]", "");
				if (StringUtils.isNotEmpty(totalAssessment)) {
					m.put(TaxHistorySetKey.TAX_EXEMPTION_AMOUNT.getKeyName(), exemption);
				}
				partyNamesILWillTR(m, searchId);
				ILWillAO.parseAddress(m);
				taxILWillTR(m, searchId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public static void partyNamesILWillTR(ResultMap m, long searchId) throws Exception {
		
		String owner = (String) m.get("tmpOwnerName");
		if (StringUtils.isEmpty(owner))
			return;

		owner = owner.trim();

		if(NameUtils.isNotCompany(owner)) {
			Matcher matcher1 = Pattern.compile("(?is)(^\\w+\\s+\\w+)\\s+([A-Z]{3,}(?:\\s+[A-Z])?)$").matcher(owner);
			if (matcher1.find()) {
				String p1 = matcher1.group(1);
				String p2 = matcher1.group(2);
				if (!p2.matches("(?i)TRUST")) {
					owner = p1 + " & " + p2;
				}
			}
			Matcher matcher2 = Pattern.compile("(?is)(^\\w+\\s+\\w+\\s+\\w)\\s+([A-Z]{3,}\\s?\\w?)$").matcher(owner);
			if (matcher2.find()) {
				String p1 = matcher2.group(1);
				String p2 = matcher2.group(2);
				if (!p2.matches("(?i)TRUST")) {
					owner = p1 + " & " + p2;
				}
			}
		}
		
		String ownerPlus = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpOwnerNameExtra"));
		ownerPlus = ownerPlus.replaceAll("(?is)JOINT DECLARATION OF TR(UST)?", "");
		ownerPlus = ownerPlus.replaceAll("(?is)\\bUNIT\\s+[A-Z0-9]+(-[A-Z0-9]+)?", "");
		if (StringUtils.isNotEmpty(ownerPlus)) {
			if (!ownerPlus.matches("\\d+.*")) {
				if (!ownerPlus.matches("PO\\s+BOX.*")) {
					ownerPlus = ownerPlus.replaceAll("(?is)C/O", "");
					ownerPlus = ownerPlus.replaceAll("(?is)\\bATTN\\b", "%").trim();
					ownerPlus = ownerPlus.trim();
					ownerPlus = ownerPlus.replaceAll("(?is)(^\\w+\\s+\\w+)\\s+([A-Z]{3,}(?:\\s+[A-Z])?)$", "$1 & $2");
					ownerPlus = ownerPlus.replaceAll("(?is)(^\\w+\\s+\\w+\\s+\\w)\\s+([A-Z]{3,}\\s+\\w)$", "$1 & $2");
					owner += "@@" + ownerPlus;
				}
			}
		}
		
		// THOMPSON RICHARD B JR JOAN T --> JOAN T is the wife
		Matcher matcher3 = Pattern.compile("(?is)\\b(JR|SR|II|III|IV)\\s+(\\w{2,})\\s+(\\w)\\b").matcher(owner);
		if(matcher3.find()) {
			String possibleWifeFirst = matcher3.group(2);
			if(FirstNameUtils.isFemaleName(possibleWifeFirst)) {
				owner = owner.replaceFirst(matcher3.group(), 
						matcher3.group(1) + "@@" + matcher3.group(2) + " " + matcher3.group(3));
			}
		}
  
		String[] owners;
		owners = owner.split("@@");

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes = {"", ""}, type = {"", ""}, otherType = {"", ""};
		String prevLast = "";
		
		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];
			
			if(ow.matches("(?s)\\w{2,}\\s+\\w")) {
				ow = prevLast + " " + ow;
			}
			
			if (ow.trim().startsWith("%")){
				ow = ow.replaceAll("%", "");
				names = StringFormats.parseNameDesotoRO(ow, true);
			} else {
				names = StringFormats.parseNameNashville(ow, true);
			}
				
			names[2] = names[2].replaceAll(",", "");
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			if (NameUtils.isNotCompany(names[2])){
				suffixes = GenericFunctions.extractNameSuffixes(names);
			}
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			
			prevLast = names[2];
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
	}
	
	public static void taxILWillTR(ResultMap m, long searchId) throws Exception {
		
		String firstInstalDue = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpFirstInstalDue"));
		String secondInstalDue = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpSecInstalDue"));
		String firstInstalPaid = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpFirstInstalPaid"));
		String secondInstalPaid = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpSecInstalPaid"));
		String firstInstalDatePaid = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpFirstInstalDatePaid"));
		String secondInstalDatePaid = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpSecInstalDatePaid"));
		
		String amountDue = firstInstalDue + "+" + secondInstalDue;
		
		m.put("TaxHistorySet.TotalDue", GenericFunctions2.sum(amountDue, searchId));
		
		if (StringUtils.isNotEmpty(firstInstalPaid)){
			String amountPaid = firstInstalPaid;
			if (StringUtils.isNotEmpty(secondInstalPaid)){
				amountPaid += "+" + secondInstalPaid;
			}
			m.put("TaxHistorySet.AmountPaid", GenericFunctions2.sum(amountPaid, searchId));
		}
		
		
		List<List> bodyRT = new ArrayList<List>();
		List<String> line = new ArrayList<String>();
		if (StringUtils.isNotEmpty(firstInstalDatePaid)){
			line.add(firstInstalDatePaid);
			line.add(firstInstalPaid);
			bodyRT.add(line);
		}
		
		if (StringUtils.isNotEmpty(secondInstalDatePaid)){
			line = new ArrayList<String>();
			line.add(secondInstalDatePaid);
			line.add(secondInstalPaid);
			bodyRT.add(line);
		}
		if (bodyRT.size() > 0){
			ResultTable rt = new ResultTable();
			String[] header = { "ReceiptDate", "ReceiptAmount"};
			rt = GenericFunctions1.createResultTableFromList(header, bodyRT);
			m.put("TaxHistorySet", rt);
		}
		
		String firstInstalAmount = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpFirstInstalAmount"));
		String secInstalAmount = org.apache.commons.lang.StringUtils.defaultString((String) m.get("tmpSecInstalAmount"));
		boolean paid = false;
		bodyRT = new ArrayList<List>();
		List<String> line1 = new ArrayList<String>();
		List<String> line2 = new ArrayList<String>();
		line1.add(firstInstalAmount);
		if (StringUtils.isNotEmpty(firstInstalPaid)) {
			line1.add(Double.toString(Double.parseDouble(firstInstalPaid) - Double.parseDouble(firstInstalAmount)));
			line1.add(firstInstalPaid);
			paid = true;
		} else {
			line1.add("0.00");
			line1.add("0.00");
			paid = false;
		}
		line1.add(firstInstalDue);
		if (paid){
			line1.add("PAID");
		} else {
			line1.add("UNPAID");
		}
		
		line2.add(secInstalAmount);
		if (StringUtils.isNotEmpty(secondInstalDatePaid)) {
			line2.add(Double.toString(Double.parseDouble(secondInstalPaid) - Double.parseDouble(secInstalAmount)));
			line2.add(secondInstalPaid);
			paid = true;
		} else {
			line2.add("0.00");
			line2.add("0.00");
			paid = false;
		}
		line2.add(secondInstalDue);
		if (paid){
			line2.add("PAID");
		} else {
			line2.add("UNPAID");
		}
		
		if (line1 != null){
			bodyRT.add(line1);
			if (line2 != null){
			   bodyRT.add(line2);
			}
		}
		if (bodyRT.size() > 0){
			ResultTable newRT = new ResultTable();
			String[] head = { "BaseAmount", "PenaltyAmount", "AmountPaid", "TotalDue", "Status"};
			newRT = GenericFunctions1.createResultTableFromList(head, bodyRT);
			m.put("TaxInstallmentSet", newRT);
		}
	}	
}

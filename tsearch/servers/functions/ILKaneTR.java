package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

/**
 * @author mihaib
  */
public class ILKaneTR {

	public static void parseAndFillResultMap(String rsResponse, ResultMap m, long searchId) {
		
		org.w3c.dom.Document finalDoc = Tidy.tidyParse(rsResponse);

		String instrumentNo = HtmlParserTidy.getValueFromTagById(finalDoc, "lblParcelNumber", "span");
		
		m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), instrumentNo.replaceAll("-", "").trim());
		m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), instrumentNo.replaceAll("-", "").trim());
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		try {
			parseAddressILKaneTR(finalDoc, m, searchId);
			parseNamesILKaneTR(finalDoc, m, searchId);
			taxInfoILKaneTR(finalDoc, m, searchId);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public static void parseNamesILKaneTR( org.w3c.dom.Document finalDoc, ResultMap m, long searchId) throws Exception {
		
		String namesAndAddress = HtmlParserTidy.getValueFromTagById(finalDoc, "lblMailingAddress1", "span").trim();
		if (StringUtils.isNotEmpty(HtmlParserTidy.getValueFromTagById(finalDoc, "lblMailingAddress2", "span").trim())){
			namesAndAddress += "\n" + HtmlParserTidy.getValueFromTagById(finalDoc, "lblMailingAddress2", "span").trim();
		}
		if (StringUtils.isNotEmpty(HtmlParserTidy.getValueFromTagById(finalDoc, "lblMailingAddress3", "span").trim())){
			namesAndAddress += "\n" + HtmlParserTidy.getValueFromTagById(finalDoc, "lblMailingAddress3", "span").trim();
		}
		if (StringUtils.isNotEmpty(HtmlParserTidy.getValueFromTagById(finalDoc, "lblMailingAddress4", "span").trim())){
			namesAndAddress += "\n" + HtmlParserTidy.getValueFromTagById(finalDoc, "lblMailingAddress4", "span").trim();
		}
		if (StringUtils.isNotEmpty(HtmlParserTidy.getValueFromTagById(finalDoc, "lblMailingAddress5", "span").trim())){
			namesAndAddress += "\n" + HtmlParserTidy.getValueFromTagById(finalDoc, "lblMailingAddress5", "span").trim();
		}
		namesAndAddress = namesAndAddress.replaceAll("&amp;", "&");
		
		String[] ownerRows = namesAndAddress.split("\n");
		StringBuffer stringOwnerBuff = new StringBuffer();
		for (String row : ownerRows){
			row = row.replaceFirst("^\\s*%\\s*", "");
			if (row.trim().matches("\\A\\s*\\d+.*")){
				break;
			} else if (row.toLowerCase().contains("box") || row.toLowerCase().contains("rfd")){
				break;
			} else if (LastNameUtils.isNoNameOwner(row)) {
			   break;
			} else {
				stringOwnerBuff.append(row.trim() + "\n");
			}
		}
		String stringOwner = stringOwnerBuff.toString().trim();
		stringOwner = stringOwner.replaceAll("&([^\\s])", "& $1");
		Matcher matcher = Pattern.compile("(?is)(.*?)\\sTRUST(.*?)TRUSTEE").matcher(stringOwner);
	   	if (matcher.find() && !NameUtils.isCompany(matcher.group(1))) 
	   		stringOwner = matcher.group(1) + matcher.group(2)+ "TRUST & " + matcher.group(1) + matcher.group(2) + "TRUSTEE";
	   	matcher = Pattern.compile("(?is)(.*?)\\s(REV(?:OCABLE)?\\sLIV(?:ING)?\\sTRUST)(.*?)\\z").matcher(stringOwner);
	   	if (matcher.find()){
	   		stringOwner = matcher.group(1) + matcher.group(3) + " " + matcher.group(2);
	   	}
	   	
	   	stringOwner = stringOwner.replaceAll("(?is)\\s*\\.\\s*$", "");
	   	
	   	stringOwner = stringOwner.replaceAll("(?is)&\\s*(\\w+\\s*,\\s*\\w\\s+\\w\\s*&\\s*\\w\\s+\\w)", "\n$1");
	   	
	   	//PIN 1511276032 year 2013: WILSON, DAVID E HEATHER K & HEATHER NICOLE
	   	stringOwner = stringOwner.replaceFirst("^([A-Z]+)\\s*,\\s*([A-Z]+\\s+[A-Z])\\s+([A-Z]+(?:\\s+[A-Z])?)\\s*&\\s*([A-Z]+(?:\\s+[A-Z]+)?)$", "$1, $2 & $3\n$1, $4");
	   	
	   	//PIN 1511276032 year 2009: DAVID E HEATHER K & HEATHER NICOLE WILSON
	   	stringOwner = stringOwner.replaceFirst("^([A-Z]+\\s+[A-Z])\\s+([A-Z]+(?:\\s+[A-Z])?)\\s*&\\s*([A-Z]+(?:\\s+[A-Z]+)?)\\s+([A-Z]+)$", "$4, $1 & $2 \n$4, $3");
	   	
		stringOwner = stringOwner.replaceAll("\n$", "");
		String[] nameLines = stringOwner.trim().split("\n");

		List<List> body = new ArrayList<List>();
		String[] suffixes, type, otherType;
		StringBuffer nameOnServerBuff = new StringBuffer();
		for (int i=0; i < nameLines.length; i++){
			String ow = nameLines[i];
			ow = ow.replaceAll("(?is)\\bMR & MRS\\b", "");
			if (NameUtils.isNotCompany(ow) && ow.contains("&")){
				ow = ow.replaceAll("(?is)\\A(\\w+(?:\\s+\\w+(?:\\s+[IV]{1,3}|\\s+[JRS]{2})?)?)\\s+&\\s+(.*)", "$2 & $1");
			}
			ow = ow.replaceAll("(?is),\\s*$", "");
			ow = ow.replaceAll("(?is),\\s*\\.\\s*$", "");
			String[] names = {"", "", "", "", "", ""};
			if(NameUtils.isCompany(ow)) {	//pin 0308426045
				names[2] = ow;
			} else {
				names = StringFormats.parseNameDesotoRO(ow, true);
				if (ow.trim().endsWith(" ATTY")){
					ow = ow.replaceAll("\\A\\s*%", "").replaceAll("\\bATTY\\b", "").trim();
					names = StringFormats.parseNameDesotoRO(ow, true);
				} else if (ow.trim().startsWith("%") || ow.trim().startsWith("C/O")){
					ow = ow.replaceAll("\\bAKA\\b", "").replaceAll("\\bC/O\\b", "").replaceAll("\\A\\s*%", "").trim();
					names = StringFormats.parseNameNashville(ow, true);
					if (LastNameUtils.isNotLastName(names[2]) && NameUtils.isNotCompany(names[2])){
						names = StringFormats.parseNameDesotoRO(ow, true);
					}
				} else if (ow.matches("(?is)\\A\\w+,.*")){
					names = StringFormats.parseNameNashville(ow, true);
				}
			}
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			nameOnServerBuff.append("/").append(ow);
		}
		String nameOnServer = nameOnServerBuff.toString();
		nameOnServer = nameOnServer.replaceFirst("/", "");
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer);
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
	}

	
	public static void parseAddressILKaneTR( org.w3c.dom.Document finalDoc, ResultMap m, long searchId) throws Exception {
		
		String address = HtmlParserTidy.getValueFromTagById(finalDoc, "lblPropertyAddress1", "span").trim();
		String cityZip = HtmlParserTidy.getValueFromTagById(finalDoc, "lblPropertyAddress2", "span").trim();
		if (StringUtils.isNotEmpty(address)){
			address = address.replaceAll("(?is)\\*\\*See\\s*notes\\*\\*", "");
			address = address.replaceAll("(?is),\\s*IL\\b", "");
			Matcher matcher = Pattern.compile("(?is)(\\d+)-(\\d+)(.*?)\\z").matcher(address);	//357-59 PRINCETON AVE -> 357 PRINCETON AVE 59
			if (matcher.find()){
				address = matcher.group(1) + matcher.group(3) + " "+ matcher.group(2); 
			}
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
		}
		if (StringUtils.isNotEmpty(cityZip)){
			if (!cityZip.startsWith(",")){
				String city = cityZip.replaceAll("(?is)([^,]+),.*", "$1");
				String zip = cityZip.replaceAll("(?is)[^,]+,\\s*[A-Z]+\\s*(.*)", "$1");
				m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				m.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip.trim().replaceAll("(?is)([^-]+)-$", "$1"));
			}
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	public static void taxInfoILKaneTR( org.w3c.dom.Document finalDoc, ResultMap m, long searchId) throws Exception {
		
		String taxYear = HtmlParserTidy.getValueFromTagById(finalDoc, "lblTaxHeading", "span");
		taxYear = taxYear.replaceAll("(?is).*County\\s*(.*)\\s*Payable.*", "$1").trim();
		
		String baseAmount = HtmlParserTidy.getValueFromTagById(finalDoc, "lblTotalTaxAmount", "span").replaceAll(",", "").trim();
		
		String firstInstalPaid = HtmlParserTidy.getValueFromTagById(finalDoc, "lblFirstPaidAmount", "span").replaceAll(",", "").trim();
		String secondInstalPaid = HtmlParserTidy.getValueFromTagById(finalDoc, "lblSecondPaidAmount", "span").replaceAll(",", "").trim();
		
		String firstInstalDue = HtmlParserTidy.getValueFromTagById(finalDoc, "lblFirstAmountDue", "span").replaceAll(",", "").trim();
		String secondInstalDue = HtmlParserTidy.getValueFromTagById(finalDoc, "lblSecondAmountDue", "span").replaceAll(",", "").trim();
		
		String firstPaidDate = HtmlParserTidy.getValueFromTagById(finalDoc, "lblFirstPaidDate", "span").trim();
		String secondPaidDate = HtmlParserTidy.getValueFromTagById(finalDoc, "lblSecondPaidDate", "span").trim();
		
		double totalDue = 0.00d;
		double amountPaid = 0.00d;
		if (firstPaidDate.contains("UNPAID")){
			totalDue += Double.parseDouble(StringUtils.isEmpty(firstInstalDue) ? "0.00" : firstInstalDue);
		} else {
			amountPaid += Double.parseDouble(StringUtils.isEmpty(firstInstalPaid) ? "0.00" : firstInstalPaid);
		}
		
		if (secondPaidDate.contains("UNPAID")){
			totalDue += Double.parseDouble(StringUtils.isEmpty(secondInstalDue) ? "0.00" : secondInstalDue);
		} else {
			amountPaid += Double.parseDouble(StringUtils.isEmpty(secondInstalPaid) ? "0.00" : secondInstalPaid);
		}

		m.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
		m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
		m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), Double.toString(totalDue));
		m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), Double.toString(amountPaid));

		String receiptsHtmlTable =  HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(finalDoc, "gvTaxHistoryDetails" , "table"));
		ResultTable rt = new ResultTable();
		Map<String, String[]> map = new HashMap<String, String[]>();
		String[] header = { "ReceiptDate", "ReceiptAmount", "AmountDue"};
		List<List<String>> bodyRT = HtmlParser3.getTableAsList(receiptsHtmlTable,false);
		
		map.put("ReceiptDate", new String[] {"ReceiptDate", "" });
		map.put("ReceiptAmount", new String[] {"ReceiptAmount", "" });
		rt.setHead(header);
		rt.setMap(map);
		rt.setBody(bodyRT);
		rt.setReadOnly();
		m.put("TaxHistorySet", rt);
		
		String priorDelinquent = "0.00";
		String body[][] = rt.getBodyRef();
		for(int i = 0; i < body.length; i++){
			priorDelinquent = GenericFunctions.sum(body[i][2], searchId);
		}
		
		m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
		
		String firstTaxAmount= HtmlParserTidy.getValueFromTagById(finalDoc, "lblFirstTaxAmount", "span").replaceAll(",", "").trim();
		String secondTaxAmount = HtmlParserTidy.getValueFromTagById(finalDoc, "lblSecondTaxAmount", "span").replaceAll(",", "").trim();
		String firstPenaltyAmount = HtmlParserTidy.getValueFromTagById(finalDoc, "lblFirstPenaltyAmount", "span").replaceAll(",", "").trim();
		String secondPenaltyAmount = HtmlParserTidy.getValueFromTagById(finalDoc, "lblSecondPenaltyAmount", "span").replaceAll(",", "").trim();
		
		ResultTable newRT = new ResultTable();
		List<String> line1 = new ArrayList<String>();
		List<String> line2 = new ArrayList<String>();
		List<List> bodySet = new ArrayList<List>();
		
		line1.add(firstTaxAmount);
		line1.add(firstPenaltyAmount);
		if (firstPaidDate.contains("UNPAID")){
			line1.add("0.00");
			line1.add(firstInstalDue);
			line1.add(firstPaidDate);
		} else {
			line1.add(firstInstalPaid);
			line1.add("0.00");
			line1.add("PAID");
		}
		
		line2.add(secondTaxAmount);
		line2.add(secondPenaltyAmount);
		if (secondPaidDate.contains("UNPAID")){
			line2.add("0.00");
			line2.add(secondInstalDue);
			line2.add(secondPaidDate);
		} else {
			line2.add(secondInstalPaid);
			line2.add("0.00");
			line2.add("PAID");
		}
		
		if (line1 != null){
			bodySet.add(line1);
			if (line2 != null){
			   bodySet.add(line2);
			}
		}
		if (bodySet.size() > 0){
			String[] head = { "BaseAmount", "PenaltyAmount", "AmountPaid", "TotalDue", "Status"};
			newRT = GenericFunctions1.createResultTableFromList(head, bodySet);
			m.put("TaxInstallmentSet", newRT);
		}
	}	
}

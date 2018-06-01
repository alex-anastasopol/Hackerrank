package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class COJeffersonTR {
	
	@SuppressWarnings("rawtypes")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)<CAPTION[^>]*>", "<tr><td>").replaceAll("(?is)</CAPTION[^>]*>", "</td></tr>")
									.replaceAll("(?is)<th", "<td").replaceAll("(?is)</th>", "</td>").replaceAll("(?is)</?div[^>]*>", "")
									.replaceAll("\n", "");
		m.put("OtherInformationSet.SrcType","TR");
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		int commId = currentInstance.getCommunityId();
		
		try {
			
			Date paidDate = HashCountyToIndex.getPayDate(commId, miServerID);
			if (paidDate != null){
				String year = paidDate.toString().replaceAll(".*?\\s+(\\d{4})", "$1").trim();
				if (StringUtils.isNotEmpty(year)){
				int taxYear = Integer.parseInt(year) - 1;
				m.put("TaxHistorySet.Year", Integer.toString(taxYear));
				}
			}
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);

			String pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Schedule Number"),"", true).trim();
			m.put("PropertyIdentificationSet.ParcelID", StringUtils.isNotEmpty(pid) ? pid : "");

			String ownerName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Owner Name:"), "", true).trim();
			ownerName = ownerName.replaceAll("(?is)</?span[^>]*>", "").replaceAll("(?is)\r\n", "").replaceAll("(?is)<br>", "@@@");
			m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName : "");
			
			String siteAddress = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Property Address:"), "", true).trim();
			if (StringUtils.isNotEmpty(siteAddress)){
				siteAddress = siteAddress.replaceAll("(\\d)-([A-Z])", "$1$2");
				m.put("tmpAddress", siteAddress);
			}
			
			String legal = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "PROPERTY DESCRIPTION"), "", true).trim();
			legal = legal.replaceAll("Subdivision Name:[^-]*-\\s*(.*)", "$1");
			m.put("PropertyIdentificationSet.SubdivisionName", StringUtils.isNotEmpty(legal) ? legal : "");
			
			if (StringUtils.isNotEmpty(legal)){
				if (legal.matches(".*\\bCONDO.*"))
					m.put("PropertyIdentificationSet.SubdivisionCond", legal);
				Pattern p = Pattern.compile("(?is)\\bUNIT\\s*(\\d+)\\b");
				Matcher ma = p.matcher(legal);
				if (ma.find()){
					m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1).trim().replaceAll("#", ""));
				}
			}
			
			
			String baseAmount = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Original Bill:"), "", true)
												.replaceAll("(?is)</?span[^>]*>", "").replaceAll("[\\$,]+", "").trim();
			m.put("TaxHistorySet.BaseAmount", StringUtils.isNotEmpty(baseAmount) ? baseAmount : "");
			
			String totalDue1 = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Balance Due:"), "", true)
											.replaceAll("(?is)</?span[^>]*>", "").replaceAll("[\\$,]+", "").trim();
			if (StringUtils.isEmpty(totalDue1)){
				totalDue1 = "0.00";
			}
			String totalDue2 = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(mainList, "Balance Due:"), "", true)
											.replaceAll("(?is)</?span[^>]*>", "").replaceAll("[\\$,]+", "").trim();
			if (StringUtils.isEmpty(totalDue2)){
				totalDue2 = "0.00";
			}
			//String totalDue = GenericFunctions2.sum(totalDue1 + "+" + totalDue2, searchId);
			m.put("TaxHistorySet.TotalDue", totalDue1);
			
			String amountPaid1 = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Amount Paid:"), "", true)
											.replaceAll("(?is)</?span[^>]*>", "").replaceAll("[\\$,\\(\\)]+", "").trim();

			String amountPaid2 = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(mainList, "Amount Paid:"), "", true)
						.replaceAll("(?is)</?span[^>]*>", "").replaceAll("[\\$,\\(\\)]+", "").trim();
			String amountPaid = "0.00";
			
			if (Double.parseDouble(totalDue2) == 0.00){
				amountPaid += "+" + amountPaid2;
			}
			if (Double.parseDouble(totalDue1) == 0.00){
				amountPaid += "+" + amountPaid1;
			}
			amountPaid = GenericFunctions2.sum(amountPaid, searchId);
			m.put("TaxHistorySet.AmountPaid", StringUtils.isNotEmpty(amountPaid) ? amountPaid : "");
			
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			List<List> bodyDatePaid = new ArrayList<List>();
			List<String> lineDatePaid = null;
			if (tables.size() > 10){
				for (int k = 0; k < tables.size(); k++){
					if (tables.elementAt(k).toHtml().contains("Deed Type")){
						List<List> body = new ArrayList<List>();
						List<String> line = null;
						NodeList rows = tables.elementAt(k).getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"));
						for (int i = 1; i < rows.size(); i++) {
							NodeList tdList = rows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
							if(tdList.size() > 3) {
								if (tdList.elementAt(3).getChildren() != null){
									if (!tdList.elementAt(3).getChildren().toHtml().trim().toLowerCase().startsWith("conver")){
										line = new ArrayList<String>();
										line.add(tdList.elementAt(3).getChildren().toHtml().trim());
										line.add("");
										line.add("");
										line.add(tdList.elementAt(0).getChildren().toHtml().trim());
										line.add(tdList.elementAt(2).getChildren().toHtml().trim());
										body.add(line);
									}
								}
							}
						}
						if (body != null){
							ResultTable rt = new ResultTable();
							String[] header = { "InstrumentNumber", "Book", "Page", "InstrumentDate", "DocumentType" };
							rt = GenericFunctions2.createResultTable(body, header);
							m.put("SaleDataSet", rt);
						}
					} else if (tables.elementAt(k).toHtml().contains("Section")){
						NodeList rows = tables.elementAt(k).getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"));
						String block = "@", lot = "@", section = "@", township = "@", range = "@";
						for (int i = 1; i < rows.size() - 1; i++) {
							NodeList tdList = rows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
							if(tdList.size() > 5) {
								 String blk = tdList.elementAt(0).getChildren().toHtml().trim().replaceAll("\\A0+", "");
								 if (!block.contains("@" + blk + "@")){
									 block += blk + "@";
								 }
								 String lt = tdList.elementAt(1).getChildren().toHtml().trim().replaceAll("\\A0+", "");
								 if (!lot.contains("@" + lt + "@")){
									 lot += lt + "@";
								 }
								 String sec = tdList.elementAt(3).getChildren().toHtml().trim().replaceAll("\\A0+", "");
								 if (!section.contains("@" + sec + "@")){
									 section += sec + "@";
								 }
								 String twp = tdList.elementAt(4).getChildren().toHtml().trim().replaceAll("\\A0+", "");
								 if (!township.contains("@" + twp + "@")){
									 township += twp + "@";
								 }
								 String rng = tdList.elementAt(5).getChildren().toHtml().trim().replaceAll("\\A0+", "");
								 if (!range.contains("@" + rng + "@")){
									 range += rng + "@";
								 }
								 
							}
						}
						m.put("PropertyIdentificationSet.SubdivisionBlock", block.replaceAll("@", "").trim());
						lot = lot.replaceAll("@", " ").trim();
						lot = LegalDescription.cleanValues(lot, false, true);
						m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
						m.put("PropertyIdentificationSet.SubdivisionSection", section.replaceAll("@", "").trim());
						m.put("PropertyIdentificationSet.SubdivisionTownship", township.replaceAll("@", "").trim());
						m.put("PropertyIdentificationSet.SubdivisionRange", range.replaceAll("@", "").trim());
					} else if (tables.elementAt(k).toHtml().contains("Receipt Number")){
						NodeList rows = tables.elementAt(k).getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"));
						for (int i = 1; i < rows.size(); i++) {
							NodeList tdList = rows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
							if(tdList.size() > 4) {
								lineDatePaid = new ArrayList<String>();
								 String amount = tdList.elementAt(1).getChildren().toHtml().trim().replaceAll("\\A0+", "")
								 						.replaceAll("(?is)</?span[^>]*>", "").replaceAll("[\\$,]+", "").trim();
								 lineDatePaid.add(amount);
								 String date = tdList.elementAt(2).getChildren().toHtml().trim().replaceAll("\\A0+", "").replaceAll("(?is)</?span[^>]*>", "");
								 lineDatePaid.add(date);
								 String receipt = tdList.elementAt(4).getChildren().toHtml().trim().replaceAll("\\A0+", "").replaceAll("(?is)</?span[^>]*>", "");
								 lineDatePaid.add(receipt);
								 bodyDatePaid.add(lineDatePaid);
								 								 
							}
						}
					} else if (tables.elementAt(k).toHtml().contains("Base Tax")){
						NodeList rows = tables.elementAt(k).getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"));
						for (int i = 1; i < rows.size(); i++) {
							NodeList tdList = rows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
							if(tdList.size() > 3) {
								lineDatePaid = new ArrayList<String>();
								 String amount = tdList.elementAt(3).getChildren().toHtml().trim().replaceAll("\\A0+", "")
								 						.replaceAll("(?is)</?span[^>]*>", "").replaceAll("[\\$,]+", "").trim();
								 lineDatePaid.add(amount);
								 String date = tdList.elementAt(2).getChildren().toHtml().trim().replaceAll("\\A0+", "").replaceAll("(?is)</?span[^>]*>", "");
								 lineDatePaid.add(date);
								 lineDatePaid.add("");
								 bodyDatePaid.add(lineDatePaid);								 
							}
						}
					} 
				}
			}
			
			if (!bodyDatePaid.isEmpty()){
				ResultTable rt = new ResultTable();
				String[] header = { "ReceiptAmount", "ReceiptDate", "ReceiptNumber" };
				rt = GenericFunctions2.createResultTable(bodyDatePaid, header);
				m.put("TaxHistorySet", rt);
			}

			try {
				partyNamesCOJeffersonTR(m, searchId);
				parseLegalCOJeffersonTR(m, searchId);
				parseAddressCOJeffersonTR(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			;
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static void parseAddressCOJeffersonTR(ResultMap resultMap, long searchId) {

		String address = (String) resultMap.get("tmpAddress");
			
		if (StringUtils.isEmpty(address))
			return;
				
		resultMap.put("PropertyIdentificationSet.AddressOnServer", address);
		resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address.trim()));
		resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address.trim()).replaceAll("\\A0+", ""));
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesCOJeffersonTR(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = stringOwner.replaceFirst("\\s*@@@\\s*$", "");
		stringOwner = stringOwner.replaceAll("\\s*%\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\b\\d+/\\d+\\b", "");
		stringOwner = stringOwner.replaceAll("\\s*/\\s*", " & ");
		stringOwner = stringOwner.replaceAll("[\\d%]+", "");
		stringOwner = stringOwner.replaceAll("\\bINT\\b", "");
		stringOwner = stringOwner.replaceAll("\\bAS( TRUSTEE) OF THE\\b", "$1");
		stringOwner = stringOwner.replaceAll("\\bTHE UND\\b", "");
			
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		String[] owners = stringOwner.split("\\s*@@@\\s*");
		for (int i = 0; i < owners.length; i++) {
			names = StringFormats.parseNameNashville(owners[i], true);
			
			names[2] = names[2].replaceAll("(?is),", "");
			
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractAllNamesSufixes(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		stringOwner = stringOwner.replaceAll("\\s*@@@\\s*", " AND ");
		
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner);
		
	}
	
	public static void parseLegalCOJeffersonTR(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
				
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = legal.replaceAll("<br>", " ").replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)(.*?AND)", "$1 $2 $1");
		/*List<List> body = new ArrayList<List>();
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
		
		if (!body.isEmpty()){
			String[] header = { "Book", "Page", "InstrumentNumber" };
			ResultTable rt = GenericFunctions2.createResultTable(body, header);
			m.put("CrossRefSet", rt);
		}*/
		
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
		p = Pattern.compile("(?is)\\b(BLKS?\\s*:)\\s*(\\d+[A-Z]?)\\b");
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
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(PHASE\\s*:?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
				
		String section = "";
		String township = "";
		String range = "";
		p = Pattern.compile("(?is)\\bSECT\\s*,\\s*TWN\\s*,\\s*RNG\\s*:\\s*(\\d+)\\s*-\\s*(\\d+[A-Z]?)\\s*-\\s*(\\d+)\\b");
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
		
	}
	
}

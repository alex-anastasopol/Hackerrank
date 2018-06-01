package ro.cst.tsearch.servers.functions;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class FLLeonAO {
	
    
	@SuppressWarnings("rawtypes")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&nbsp", " ").replaceAll("(?is)&amp;", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)</?strong[^>]*>", "").replaceAll("\n", "")
								.replaceAll("(</?t)h([^>]*>)", "$1d$2").replaceAll("(?is)</?span[^>]*>", "");
		m.put("OtherInformationSet.SrcType","AO");

		try {		
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Parcel ID"), "", true).replaceAll("(?is)</?strong[^>]*>", "").trim();
			pid = pid.replaceAll("(?is)\\s*Parcel Number:\\s*", "").trim();
			pid = pid.replaceFirst("(?is)<div[^>]*>([^<]+)</div>", "$1");
			if (StringUtils.isNotEmpty(pid)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid);
			}
			
			String accountNo = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Parent Parcel"), "", true).replaceAll("(?is)</?strong[^>]*>", "").trim();
			accountNo = accountNo.replaceAll("(?is)\\s*N/A\\s*", "").trim();
			m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), accountNo);

			NodeList tablesFromParcelInfoDiv = HtmlParser3.getNodeByID("parcel_Info", mainList, true).getChildren();
			if (tablesFromParcelInfoDiv != null){
				int numberofRows = 1;
				NodeList tables = tablesFromParcelInfoDiv.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				for (int k = 0 ; k < tables.size(); k++){
					if (tables.elementAt(k).toHtml().contains("Owner")){
						TableTag parcelTable = (TableTag) tables.elementAt(k);
						numberofRows = parcelTable.getRowCount();
						break;
					}
				}
				String ownerName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Owner"), "", true).replaceAll("(?is)</?strong[^>]*>", "").trim();
				ownerName = ownerName.replaceAll("(?is)([^<]+)<br.*", "$1");
				ownerName = ownerName.replaceFirst("(?is)<div[^>]*>([^<]+)</div>", "$1");
				int rowWithName = 1;
				//String coOwnerName = "";
				while (rowWithName <= numberofRows){
					String info = HtmlParser3.getValueFromAbsoluteCell(rowWithName, 1, HtmlParser3.findNode(mainList, "Owner"), "", true).trim();
					if (info.contains("<div")) {
						info = info.replaceFirst("(?is)<div[^>]*>([^<]+)</div>", "$1");
					}
					
					if (!info.matches("\\A\\d+.*")){
						if (!info.matches("\\APO\\s+.*")){
							if (StringUtils.isNotEmpty(info)){
								ownerName += "@@@" + info;
								ownerName = ownerName.replaceAll("(?is)</?span[^>]*>", "").replaceAll("(?is)\r\n", "");
							}
							rowWithName++;
						} else {
							break;
						}
					} else {
						break;
					}
				}
				m.put("tmpOwner", ownerName);
				
				String legal = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Legal :"), "", true);
				rowWithName = 0;
				while (rowWithName < numberofRows - 2){
					String legalRow = HtmlParser3.getValueFromAbsoluteCell(rowWithName, 3, HtmlParser3.findNode(mainList, "Owner"), "", true).trim();
					if (StringUtils.isNotEmpty(legalRow)){
						legal += " " + legalRow;
						legal = legal.replaceAll("(?is)</?span[^>]*>", "").replaceAll("(?is)\r\n", "");
						legal = legal.replaceFirst("(?is)<div[^>]*>([^<]+)</div>", "$1");
					}
					rowWithName++;

				}
				if (StringUtils.isNotEmpty(legal)) {
					legal = legal.replaceAll("\\s{3,}", " ");
					legal = legal.replaceFirst("(?is)<div id=\"Acreage\">[^<]+</div>", "");
					m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal.trim());
				}
				
				String subName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Legal :"), "", true);
				subName = subName.replaceFirst("(?is)<div[^>]*>([^<]+)</div>", "$1");
				if (!subName.trim().matches("\\A\\d+\\s+\\d+.*")){
					subName = subName.replaceAll("(?is)\\bUNREC.*", "");
					m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subName.trim());
				}
			}
				
			String address = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Location :"), "", true);
			address = address.replaceAll("(?is)</?strong[^>]*>", "");
			address = address.replaceFirst("(?is)<div[^>]*>([^<]+)</div>", "$1").trim();
			m.put("tmpAddress", address);
									
			String transacHistTable = "";
			NodeList tablesFromSalesDiv = HtmlParser3.getNodeByID("recentSales", mainList, true).getChildren();
			NodeList tables = tablesFromSalesDiv.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			for (int k = 0 ; k < tables.size(); k++){
				if (tables.elementAt(k).toHtml().contains("Instrument Type")){
					transacHistTable = tables.elementAt(k + 1).toHtml();
					break;
				}
			}
			
			
			if (StringUtils.isNotEmpty(transacHistTable)){
				transacHistTable = transacHistTable.replaceAll("\r", " ").replaceAll("\t", " ").replaceAll("\n", " ").replaceAll("[\\$,]+", "");
				transacHistTable = transacHistTable.replaceAll("(?is)<tr[^>]+>", "<tr>");
				List<List<String>> transacHist = HtmlParser3.getTableAsList(transacHistTable, true);
				for(List<String> lst : transacHist){
					String bookPage = lst.get(2).trim();
					lst.set(2, bookPage.replaceAll("(?is)(\\d+)\\s+(\\d+)", "$1").replaceAll("(?is)\\A0+", ""));
					lst.set(3, bookPage.replaceAll("(?is)(\\d+)\\s+(\\d+)", "$2").replaceAll("(?is)\\A0+", ""));
				}
				List<List> newBody = new ArrayList<List>(transacHist);
				
				String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
				if (StringUtils.isNotEmpty(legal)){
					Pattern p = Pattern.compile("\\b(\\d{2,})/(\\d{2,})\\b");
					Matcher ma = p.matcher(legal);	      	   
					while (ma.find()){
						boolean alreadyIn = false;
						for (List lst : newBody){
							if (lst.get(2).toString().equals(ma.group(1).trim()) && lst.get(3).toString().equals(ma.group(2).trim())){
							   alreadyIn = true;   
							}
						}
						if (!alreadyIn){
							List<String> line = new ArrayList<String>();
							line.add("");
							line.add("");
							line.add(ma.group(1).trim());
							line.add(ma.group(2).trim());
							line.add("");
							newBody.add(line);	
						}
					}	   
				}
	
				ResultTable rt = new ResultTable();
				String[] header = {"InstrumentDate", "SalesPrice", "Book", "Page", "DocumentType"};
							
				rt = GenericFunctions2.createResultTable(newBody, header);
				m.put("SaleDataSet", rt);
			}
			
			try {
				partyNamesFLLeonAO(m, searchId);
				parseAddressFLLeonAO(m, searchId);
				parseLegalFLLeonAO(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesFLLeonAO(ResultMap m, long searchId) throws Exception {
		String stringOwner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = stringOwner.replaceAll("\\s*%\\s*", "@@@");//143325 A0020
		stringOwner = stringOwner.replaceAll("\\b\\d+/\\d+\\b", "");
		stringOwner = stringOwner.replaceAll("\\s+/\\s+", " & ");
		stringOwner = stringOwner.replaceAll("[\\d%]+", "");
		stringOwner = stringOwner.replaceAll("\\bINT\\b", "");
		stringOwner = stringOwner.replaceAll("\\bAS( TRUSTEE) OF THE\\b", "$1");
		stringOwner = stringOwner.replaceAll("(?is)\\b(?:CO-?\\s*)(TRUSTEE)\\b", " $1");
		stringOwner = stringOwner.replaceAll("\\bTHE UND\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s+Heirs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bMrs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\b(?:AS\\s*)?(TR?(US)?TEEE?S?)\\b", "$1");
		stringOwner = stringOwner.replaceAll("(?is)@{4,}", "@@@");
			
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] coNames = {"", "", "", "", "", ""};
		String[] suffixes, types, otherTypes;
		String[] owners = stringOwner.split("@@@");
		for (int i = 0; i < owners.length; i++) {
			owners[i] = owners[i].replaceAll("(?is),", "");
			if (owners[i].contains("TRUSTEE") || owners[i].contains("C/O") || owners[i].trim().matches("(?is)\\w+\\s+\\w\\s+\\w+")){
				owners[i] = owners[i].replaceAll("\\bC/O\\b", "");
				owners[i] = owners[i].replaceAll("\\A(\\w+\\s+\\w+)\\s*&\\s*(\\w+\\s+(?:\\w+\\s+)?\\w+)", "$2 & $1");
				if (owners[i].matches("(?is)\\A(\\w+\\s+\\w+\\s+\\w+)\\s*&\\s*(\\w+\\s+\\w+\\s+\\w+)\\s*")){
					String[] coown = owners[i].split("&");
					names = StringFormats.parseNameDesotoRO(coown[0], true);
					coNames = StringFormats.parseNameDesotoRO(coown[1], true);
					names[3] = coNames[0];
					names[4] = coNames[1];
					names[5] = coNames[2];
				} else {
					names = StringFormats.parseNameDesotoRO(owners[i], true);
				}
			} else if (isCompany(owners[i])) {
				names = new String[]{"", "", "", "", "", ""};
				names[2] = owners[i];
			} else {
				names = StringFormats.parseNameNashville(owners[i], true);
			}
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
										isCompany(names[2]), isCompany(names[5]), body);
		}
		
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		stringOwner = stringOwner.replaceAll("\\s*@@@\\s*", " AND ");
		
		m.put("PropertyIdentificationSet.NameOnServer", stringOwner);
		
	}
	
	public static boolean isCompany(String s) {
		if (s.matches("(?i)TIMBERLANE\\s+COMMONS")) {
			return true;
		}
		return NameUtils.isCompany(s);
	}
	
	public static void parseAddressFLLeonAO(ResultMap resultMap, long searchId) {
		String address = (String) resultMap.get("tmpAddress");
			
		if (StringUtils.isEmpty(address))
			return;
		
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address).trim());
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address).trim());
		
	}
		
	public static void parseLegalFLLeonAO(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
				
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("(?is)[NSWE]{1,2}\\s*\\d+\\s*FT\\s+OF", "");
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = (String) m.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName());
		if (lot == null){
			lot = "";
		}
		Pattern p = Pattern.compile("(?is)\\b(LO?T'?S?\\s*)\\s*([\\d\\s&]+[A-Z]?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = (String) m.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName());
		if (block == null){
			block = "";
		}
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*(\\d+[A-Z]?|[A-Z]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+-?[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(UNITS?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\bABST\\s+([\\d+\\s&]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), ma.group(1).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(1), "");
		}
		
		p = Pattern.compile("(?is)\\b(?:TR|TRACT)\\s+([\\dA-Z,]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legal = legal.replaceAll(ma.group(0), "");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(1).replaceAll("\\s*,\\s*", " ").trim());
		}
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
				
		
		p = Pattern.compile("(?is)(\\d+)\\s+(\\d+[A-Z])\\s+(\\d+[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()){
		    m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(1));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(3));
			
		}
	}
		
}

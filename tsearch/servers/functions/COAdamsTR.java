package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author olivia
*/

public class COAdamsTR {
	
	private static String[] CITIES = {"Arvada", "Aurora", "Bennett", "Brighton", "Commerce City", "Federal Heights", "Northglenn", "Thornton", "Westminster"};
	
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
		
		try {
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList mainList = htmlParser.getNodeList();
			
			TableTag tbl = (TableTag) mainList.extractAllNodesThatMatch(new HasAttributeFilter("id", "paymentLinks"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			
//			if (tbl != null) {
//				if (tbl.getRowCount() > 0) {
//					if (tbl.getRow(0).getColumnCount() > 0) {
//						String taxYear = tbl.getRow(0).getColumns()[0].getChildrenHTML();
//						taxYear = taxYear
//								.replaceFirst("(?is)[\\w\\s]+\\bPayment\\b\\s*<\\s*/?\\s*br\\s*/?\\s*>Due\\s+(\\d+\\s*/\\s*\\d+\\s*/\\s*\\d+).*", "$1");
//						taxYear = taxYear.replaceFirst("\\d+\\s*/\\s*\\d+\\s*/\\s*(\\d+)", "20" + "$1");
//						m.put(TaxHistorySetKey.YEAR.getKeyName(), StringUtils.isNotEmpty(taxYear) ? taxYear : "");
//					}
//
//					if (tbl.getRowCount() > 1) {
//						if (tbl.getRow(1).getColumnCount() > 1) {
//							String baseAmt = tbl.getRow(1).getColumns()[1].getChildrenHTML().replaceAll("[\\$,]", "").trim();
//							m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), StringUtils.isNotEmpty(baseAmt) ? baseAmt : "");
//						}
//					}
//				}
//			}
			
			tbl = (TableTag) mainList.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxAccountSummary"), true)
						.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			if (tbl != null) {
				if (tbl.getRowCount() > 5) {
					if (tbl.getRows()[0].getColumnCount() > 1) {
						String accountId = tbl.getRows()[0].getColumns()[1].getChildrenHTML().trim();
						if (accountId.matches("R\\d{6,}")) {
							m.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");
						}
						m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), StringUtils.isNotEmpty(accountId) ? accountId : "");
					}

					if (tbl.getRows()[1].getColumnCount() > 1) {
						String pid = tbl.getRows()[1].getColumns()[1].getChildrenHTML().trim();
						m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), StringUtils.isNotEmpty(pid) ? pid : "");
					}

					if (tbl.getRows()[2].getColumnCount() > 1) {
						String ownerName = tbl.getRows()[2].getColumns()[1].getChildrenHTML().trim();
						m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName : "");
						partyNamesCOAdamsTR(m, searchId);
					}

					if (tbl.getRows()[4].getColumnCount() > 1) {
						String siteAddress = tbl.getRows()[4].getColumns()[1].getChildrenHTML().trim();
//						siteAddress = siteAddress.replaceFirst("(?is)(\\d+\\s+(?:[\\d\\s\\w]+)#\\w(?:\\s*\\d+)?)\\s+([\\w\\s]+(?:\\d+)?)", "$1<br>$2");
						siteAddress = siteAddress.replaceFirst("(?is)(\\d+\\s+[\\d\\w\\s]+)(?:\\s\\b[EWNS]\\b\\s*)#\\w(?:\\s*\\d+)?", "$1");
						siteAddress = siteAddress.replaceFirst("(?is)(\\d+\\s+[\\d\\w\\s]+)#\\w(?:\\s*\\d+)??", "$1");
						String[] siteAddressLines = siteAddress.split("<br>");
						if (StringUtils.isNotEmpty(siteAddressLines[0])) {
							siteAddressLines[0] = siteAddressLines[0].replaceAll("(\\d)-([A-Z])", "$1$2");
							String cityZip = siteAddressLines[0];

							Matcher mAdr = Pattern.compile("(?is)([\\w\\s\\d]+\\b(?:AVENUE|AVE|STREET|ST|CIRCLE|CIR|DRIVE|DR|ROAD|RD|HIGHWAY|HGWY|LANE|LN|PLACE|PL)\\b)(.*)").matcher(cityZip);
							if (mAdr.find()) {
								siteAddressLines[0] = mAdr.group(1).trim();
								m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(siteAddressLines[0]));
								m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(siteAddressLines[0]));

								cityZip = mAdr.group(2).trim();
								String city = "";
								if (cityZip.matches("(?is)([\\w\\s'-]+),\\s*\\bCO\\b\\s*([\\d\\s-]+)")) {
									city = cityZip.replaceFirst("(?is)([\\w\\s'-]+),\\s*\\bCO\\b\\s*([\\d\\s-]+)", "$1");
									m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
									String zip = cityZip.replaceFirst("(?is)([\\w\\s'-]+),\\s*\\bCO\\b\\s*([\\d\\s-]+)", "$2");
									m.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
								} else {
									cityZip = cityZip.replaceAll("\\d+", "");
									m.put(PropertyIdentificationSetKey.CITY.getKeyName(), cityZip);
								}
							}
						}
					}
					
					if (tbl.getRows()[5].getColumnCount() > 1) {
						String legal = tbl.getRows()[5].getColumns()[1].getChildrenHTML().trim();
						m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), StringUtils.isNotEmpty(legal) ? legal.trim() : "");
						parseLegalCOAdamsTR(m, searchId);
					}
				}

			}
			
			m.removeTempDef();
			
			String taxYear = "";
			tbl = (TableTag) mainList.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxSummary"), true).elementAt(0);
			if (tbl != null) {
				if (tbl.getRowCount() == 2) {
					if (tbl.getRow(1).getColumnCount() == 8) {
						taxYear = tbl.getRow(1).getColumns()[0].getChildrenHTML().trim();
						m.put(TaxHistorySetKey.YEAR.getKeyName(), StringUtils.isNotEmpty(taxYear) ? taxYear : "");
						
						String baseAmt = tbl.getRow(1).getColumns()[1].getChildrenHTML().replaceAll("[\\$,]", "").trim();
						m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), StringUtils.isNotEmpty(baseAmt) ? baseAmt : "");
						
						String amtDue = tbl.getRow(1).getColumns()[7].getChildrenHTML().replaceAll("[\\$,]", "").trim();
						m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), StringUtils.isNotEmpty(amtDue) ? amtDue : "");
					}
				}
			}
			
			String priorDelinq="";
			double priorDelinqDouble = 0.00d;
			
			tbl = (TableTag) mainList.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxHistTable"), true).elementAt(0);
			if (tbl != null) {
				List<List<String>> bodyRT = new ArrayList<List<String>>();
				
				for (int i = 0; i < tbl.getRowCount(); i++) {
					if (tbl.getRow(i).getColumnCount() == 5) {
						TableColumn c = tbl.getRow(i).getColumns()[0];
						String year = c.getChildrenHTML().trim();
						c = tbl.getRow(i).getColumns()[1];
						String type = c.getChildrenHTML().trim();
						c = tbl.getRow(i).getColumns()[2];
						String date = c.getChildrenHTML().trim();
						c = tbl.getRow(i).getColumns()[3];
						String amount = c.getChildrenHTML().replaceAll("[\\$,]", "").trim();
						c = tbl.getRow(i).getColumns()[4];
						String ad = c.getChildrenHTML().replaceAll("[\\$,]", "").trim();

						if ("tax payment".equalsIgnoreCase(type)) {
							if (year.equals(taxYear)) {
								m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amount);
								m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), ad);
								// String.valueOf(ap));
								// }

							} else {
								if (!"0.00".equals(ad)) {
									priorDelinq = ad;
									priorDelinqDouble += Double.parseDouble(priorDelinq);
								}
							}
							
							List<String> line = new ArrayList<String>();
							line.add(amount);
							line.add(date);
							bodyRT.add(line);
						}
					}
				}
				ResultTable receipts = new ResultTable();
				Map<String, String[]> map = new HashMap<String, String[]>();
				String[] header = { "ReceiptAmount", "ReceiptDate" };
				
				map.put("ReceiptAmount", new String[] {"ReceiptAmount", "" });
				map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
				receipts.setHead(header);
				receipts.setMap(map);
				receipts.setBody(bodyRT);
				receipts.setReadOnly();
				m.put("TaxHistorySet", receipts);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static void parseAddressCOAdamsTR(ResultMap resultMap, long searchId) {
		String address = (String) resultMap.get("tmpAddress");
			
		if (StringUtils.isEmpty(address))
			return;
			
		String city = "";
		for (int i = 0; i < CITIES.length; i++){
			if (address.toLowerCase().contains(CITIES[i].toLowerCase())){
				city = CITIES[i];
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				address = address.replaceAll("(?is)" + city + "\\s*(?:<\\s*/?\\s*br\\s*/?\\s*>)?", "");
				address = address.replaceAll("(\\d)-([A-Z])", "$1$2");
				//address = address.replaceFirst("(?is)#(\\d+(?:[A-Z])?)", "UNIT $1").trim();
				break;
			}
		}
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address.trim()));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address.trim()));
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesCOAdamsTR(ResultMap m, long searchId) throws Exception {
		String stringOwner = (String) m.get("tmpOwner");
		String stringCoOwner = (String) m.get("tmpCoOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		//prosteala
		stringOwner = stringOwner.replaceAll("\\b([A-Z])(POWELL)\\b", "$1 AND $2");
		stringOwner = stringOwner.replaceAll("(?is)\\s*\\b(MC)\\b\\s+(\\w)", " $1$2"); //MC GRADY RAYMOND D
		//end
		
		stringOwner = stringOwner.replaceAll("\\b\\d+/\\d+\\b(\\s+INT(\\s+JT)?)?", "");
		stringOwner = stringOwner.replaceAll("\\s*C\\s*/\\s*O\\b", " AND ");
		stringOwner = stringOwner.replaceAll("\\s*/\\s*", " & ");
		stringOwner = stringOwner.replaceAll("(?is)\\b[\\d\\.]+%", "");
		stringOwner = stringOwner.replaceAll("\\b(UND\\s+)?INT\\b", "");
		stringOwner = stringOwner.replaceAll("\\b(?:CO-|AS\\s+)(TRUSTEES?)(?:\\s+OF)?", " $1 AND ");
		stringOwner = stringOwner.replaceAll("\\s*%\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\b(?:AS\\s+|CO[-\\s]+)?(TRUSTEES?) (?:OF|FOR)(?:\\s+THE)?\\b", " $1");
		stringOwner = stringOwner.replaceAll("\\b(THE\\s+)?UND\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bJOINT\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bATTN\\s*:\\s*TAX\\s+DEPT\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\b(AND)([^E|A|I|O|U])", "$1 $2");
		stringOwner = stringOwner.replaceAll("(?is)\\s+(FBO)", " $1 ");
		stringOwner = stringOwner.replaceAll("(?is)\\s+\\bAND\\b\\s*", " @@@@@@@ ");
		
		
		String fmNamesforAKACase = "";// 0171916103021 SMITH PAULA JO AKA CURLEE
		if (stringOwner.matches("\\A\\w+\\s+\\w+(\\s+\\w+)?\\s+AKA\\s+\\w+\\s*$")){
			fmNamesforAKACase = stringOwner.replaceAll("\\A(\\w+\\s+)(\\w+(?:\\s+\\w+)?)\\s+AKA\\s+(\\w+)\\s*$", "$2");
			stringOwner = stringOwner.replaceAll("\\A(\\w+\\s+)(\\w+(?:\\s+\\w+)?)\\s+AKA\\s+(\\w+)\\s*$", "$1 $2 & $3 $2");
		}
		stringOwner = stringOwner.replaceAll("\\b(LLC)\\s+AND\\b", "$1 @@@@@@@");
			
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes, types, otherTypes;
		String[] namesCo = {"", "", "", "", "", ""};
		
		if (StringUtils.isNotEmpty(stringCoOwner)){
			stringCoOwner = stringCoOwner.replaceAll("\\bDATED\\s+\\w+\\s+[\\d\\s]+\\b", "");
			if (stringCoOwner.matches("\\ATRUST\\s+\\w+\\s*$")){
				stringOwner += " " + stringCoOwner;
				stringCoOwner = "";
			}
			if (stringCoOwner.matches("\\A\\w+\\s+TRUST(\\s+[A-Z]/[A-Z])?\\s*$")){
				stringOwner += " " + stringCoOwner;
				stringCoOwner = "";
			}
			stringOwner = stringOwner.replaceAll("\\b(TRUST)\\s+THE\\s*$", "$1");
			
		}
		stringOwner = stringOwner.replaceAll("\\b(TRUST)\\s+THE\\s*$", "$1");
		String[] moreComp = stringOwner.split("@@@@@@@");
		if (moreComp.length > 1){
			for (int i = 0; i < moreComp.length; i++) {
				names = StringFormats.parseNameNashville(moreComp[i], true);
				types = GenericFunctions.extractAllNamesType(names);
				otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
		} else {
			names = StringFormats.parseNameNashville(stringOwner, true);
			
			if (StringUtils.isNotEmpty(names[5]) && 
					LastNameUtils.isNotLastName(names[5]) && 
					NameUtils.isNotCompany(names[5])){
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
			}
			
			names[2] = names[2].replaceAll("(?is),", "");
			
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}

		if (StringUtils.isNotEmpty(stringCoOwner)){
			stringCoOwner = stringCoOwner.replaceAll("\\b[A-Z]/[A-Z]\\b", "");
			stringCoOwner = stringCoOwner.replaceAll("\\b\\d+/\\d+\\b", "");
			stringCoOwner = stringCoOwner.replaceAll("\\b(?:CO-|AS\\s+)(TRUSTEES)\\b", " $1");
			stringCoOwner = stringCoOwner.replaceAll("[\\d\\.]+%", "");
			stringCoOwner = stringCoOwner.replaceAll("\\b(UND\\s+)?INT\\b", "");
			stringCoOwner = stringCoOwner.replaceAll("\\s*/\\s*", " & ");
			stringCoOwner = stringCoOwner.replaceAll("\\b(THE\\s+)?UND\\b", "");
			stringCoOwner = stringCoOwner.replaceAll("\\b(TRUST)\\s+THE\\s*$", "$1");
			stringCoOwner = stringCoOwner.replaceAll("(?is)\\bATTN\\s*:\\s*TAX\\s+DEPT\\b", "");
			// 0171916103021 SMITH PAULA JO AKA CURLEE
			if (stringCoOwner.matches("\\AAKA\\s+\\w+\\s+AKA\\s+\\w+\\s*$") && StringUtils.isNotEmpty(fmNamesforAKACase)){
				stringCoOwner = stringCoOwner.replaceAll("\\AAKA\\s+(\\w+)\\s+AKA\\s+(\\w+)\\s*$", "$1 " + fmNamesforAKACase + " & $2 " + fmNamesforAKACase);
			}
			
			namesCo = StringFormats.parseNameNashville(stringCoOwner, true);
			//0172308303014 , 0171906207012   ,to be checked
			if (namesCo.length > 0 && namesCo[1].matches("\\w+\\s+\\w+") && namesCo[2].length() > 0){
				String temp = namesCo[1].replaceAll("\\A(\\w+)\\s+(\\w+)$", "$1");
				namesCo[1] = namesCo[1].replaceAll("\\A(\\w+)\\s+(\\w+)$", "$2 " + namesCo[2]);
				namesCo[2] = namesCo[0]; 
				namesCo[0] = temp;
			}
			
			namesCo[2] = namesCo[2].replaceAll("(?is),", "");
			
			types = GenericFunctions.extractAllNamesType(namesCo);
			otherTypes = GenericFunctions.extractAllNamesOtherType(namesCo);
			suffixes = GenericFunctions.extractNameSuffixes(namesCo);
			
			GenericFunctions.addOwnerNames(namesCo, suffixes[0], suffixes[1], types, otherTypes,
					NameUtils.isCompany(namesCo[2]), NameUtils.isCompany(namesCo[5]), body);
		}
			
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		if (StringUtils.isNotEmpty(stringCoOwner)){
			stringOwner += " AND " + stringCoOwner;
		}
		
		m.put("PropertyIdentificationSet.NameOnServer", stringOwner);
		
	}
	
	public static void parseLegalCOAdamsTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
				
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = legal.replaceAll("<br>", " ").replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)(.*?AND)", "$1 $2 $1");
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s*([\\d\\s]+[A-Z]?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
		}
		
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLKS?\\s*:)\\s*(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(UNITS?\\s*:?)\\s*([A-Z]?[\\d-]+\\s*[A-Z]?)\\b(?:\\s+\\b[A-Z]\\b)?");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), ma.group(2).trim().replaceAll("#", "").replaceAll("\\s", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(PHASE\\s*:?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0)," ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp.trim();
				
		String section = "";
		String township = "";
		String range = "";
		p = Pattern.compile("(?is)\\bSECT\\s*,\\s*TWN\\s*,\\s*RNG\\s*:\\s*(\\d+)\\s*-\\s*(\\d+[A-Z]?)\\s*-\\s*(\\d+)\\b");
		Matcher mp = p.matcher(legal);
		if (mp.find()) {
			section = mp.group(1);
			township = mp.group(2);
			range = mp.group(3);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), township);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), range);
		}

		p = Pattern.compile("(?is)\\b(SUB\\s*:?)\\s*([\\w\\s'-\\.\\d]+)");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
		
		} else {
			p = Pattern.compile("(?is)(?:CONDO\\s*:\\s*)?([\\w\\s\\d]+\\bCOND(?:OMINIUMS?)\\b)");
			ma = p.matcher(legal);
			if (ma.find()) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), ma.group(1).trim().replaceAll("#", ""));
			}
		}
		
	}
	
}

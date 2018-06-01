package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.GenericSKLD;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class COAdamsAO {
	
	private static String[] CITIES = {"Arvada", "Aurora", "Bennett", "Brighton", "Commerce City", "Federal Heights", "Northglenn", "Thornton", "Westminster"};
	
	@SuppressWarnings("rawtypes")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int countyId) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)<CAPTION[^>]*>", "<tr><td>").replaceAll("(?is)</CAPTION[^>]*>", "</td></tr>")
									.replaceAll("(?is)<th", "<td").replaceAll("(?is)</th>", "</td>").replaceAll("</?span[^>]*>", "");
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"AO");

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			m.put(TaxHistorySetKey.YEAR.getKeyName(), ((Integer)Calendar.getInstance().get(Calendar.YEAR)).toString());

						
			try {
				TableColumn tc = (TableColumn) HtmlParser3.findNode(mainList, "Parcel Number").getParent();
				String pid = HtmlParser3.getValueFromCell(tc, ":\\s*(\\d{6,})", true);
				pid = pid.replaceAll("</?font[^>]*>", "").replaceAll("</?b>", "").trim();
				m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), StringUtils.isNotEmpty(pid) ? pid : "");
				
				
				tc = (TableColumn) HtmlParser3.findNode(mainList, "Account Number").getParent();
				String accNo = HtmlParser3.getValueFromCell(tc, ":\\s*([RPM]\\d{6,})", true);
				accNo = accNo.replaceAll("</?font[^>]*>", "").replaceAll("</?b>", "").trim();
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), StringUtils.isNotEmpty(pid) ? accNo : "");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			String ownerName = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Owners Name and Address"), "", true)
											.replaceAll("(?is)</?font[^>]*>", "").trim();
			String[] ownerNameLines = ownerName.split("<br/>");
			m.put("tmpOwner", StringUtils.isNotEmpty(ownerNameLines[0]) ? ownerNameLines[0] : "");
			//String coOwnerName = HtmlParser3.getValueFromAbsoluteCell(2, 0, HtmlParser3.findNode(mainList, "Owners Name and Address"), "", true)
										//	.replaceAll("(?is)</?font[^>]*>", "").trim();
			m.put("tmpCoOwner", StringUtils.isNotEmpty(ownerNameLines[1]) ? ownerNameLines[1] : "");
			
			String siteAddress = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "Owners Name and Address"), "", true)
											.replaceAll("(?is)</?font[^>]*>", "").trim();
			String[] siteAddressLines = siteAddress.split("<br/>");
			if (StringUtils.isNotEmpty(siteAddressLines[0])){
				siteAddressLines[0] = siteAddressLines[0].replaceAll("(\\d)-([A-Z])", "$1$2");
				m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(siteAddressLines[0]));
				m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(siteAddressLines[0]));
				
				if (StringUtils.isNotEmpty(siteAddressLines[1])) {
					siteAddressLines[1] = siteAddressLines[1].replaceFirst("(?is)\\s*\\bCO\\b\\s*", "").trim();
					String city = "";
					for (int i = 0; i < CITIES.length; i++){
						if (siteAddressLines[1].toLowerCase().contains(CITIES[i].toLowerCase())){
							city = CITIES[i];
							m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
							break;
						}
					}
				}
			}
			
			String legal = "", subName = "";
			boolean legalReady = false, subNameReady = false;
			NodeList divs = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true);
			for (int k = 0; k < divs.size(); k++){
				if (divs.elementAt(k).toHtml().contains("Legal Description") && !legalReady){
					legal = divs.elementAt(k+1).toHtml().replaceAll("</?div[^>]*>", "");
					legalReady = true;
				} else if (divs.elementAt(k).toHtml().contains("Subdivision Plat") && !subNameReady){
					subName = divs.elementAt(k+1).toHtml().replaceAll("</?div[^>]*>", "");
					subNameReady = true;
				}
				if (legalReady && subNameReady)
					break;
			}
			//legal = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Legal Description"), "", true)
										//	.replaceAll("(?is)</?font[^>]*>", "").trim();
			m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), StringUtils.isNotEmpty(legal) ? legal.trim() : "");
			
			//subName = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Subdivision Plat"), "", true)
											//.replaceAll("(?is)</?font[^>]*>", "").trim();
			if (StringUtils.isNotEmpty(subName)){
				if (subName.matches("NO SUBDIVISION NAME") || subName.matches("N/A")){
					subName = "";
				}
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subName.trim());
				if (subName.matches(".*\\bCONDO.*"))
					m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subName.trim());
			}
			
			
//			String baseAmount = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "Tax Year"), "", true)
//												.replaceAll("(?is)</?font[^>]*>", "").replaceAll("[\\$,]+", "").trim();
//			m.put("TaxHistorySet.BaseAmount", StringUtils.isNotEmpty(baseAmount) ? baseAmount : "");
//			
//			String amountPaid = HtmlParser3.getValueFromAbsoluteCell(1, 4, HtmlParser3.findNode(mainList, "Tax Year"), "", true)
//												.replaceAll("(?is)</?font[^>]*>", "").replaceAll("[\\$,\\(\\)]+", "").trim();
//			m.put("TaxHistorySet.AmountPaid", StringUtils.isNotEmpty(amountPaid) ? amountPaid : "");
//			
//			String totalDue = HtmlParser3.getValueFromAbsoluteCell(1, 5, HtmlParser3.findNode(mainList, "Tax Year"), "", true)
//											.replaceAll("(?is)</?font[^>]*>", "").replaceAll("[\\$,]+", "").trim();
//			m.put("TaxHistorySet.TotalDue", StringUtils.isNotEmpty(totalDue) ? totalDue : "");
//			
//			String priorDue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Prior Taxes Due"),"", true)
//											.replaceAll("</?font[^>]*>", "").trim();
//			
//			Matcher mat = PRIOR_PAT.matcher(detailsHtml);
//			if (mat.find()){
//				String priorDelinq = mat.group(1).replaceAll("[\\$,]+", "").trim();
//				if (priorDue.toLowerCase().equals("yes")){
//					m.put("TaxHistorySet.PriorDelinquent", StringUtils.isNotEmpty(priorDelinq) ? priorDelinq : "0.00");
//				}
//			}
			
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tables.size() > 0){
				for (int k = 0; k < tables.size(); k++){
					if (tables.elementAt(k).toHtml().contains("Deed")){
						List<List> body = new ArrayList<List>();
						List<String> line = null;
						NodeList rows = tables.elementAt(k).getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"));
						for (int i = 1; i < rows.size(); i++) {
							NodeList tdList = rows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
							if(tdList.size() > 6) {
								
								line = new ArrayList<String>();
								String instrumentNumber = "";
								if (tdList.elementAt(0).getChildren() != null){
									line.add(tdList.elementAt(0).getChildren().toHtml().trim());
								} else {
									line.add("");
								}
								if (tdList.elementAt(3).getChildren() != null){
									instrumentNumber = tdList.elementAt(3).getChildren().toHtml().trim();
									if (tdList.size() > 9) {
										String date = tdList.elementAt(9).getChildren().toHtml().trim();
										if (!"".equals(date)) {
											Date d = Util.dateParser3(date);
											String year = FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(d);
											if (instrumentNumber.length()>year.length() && instrumentNumber.startsWith(year)) {
												instrumentNumber = instrumentNumber.substring(year.length());
											}
											instrumentNumber = GenericSKLD.generateSpecificInstrument(instrumentNumber.replaceFirst("(?i)^[A-Z]", "") + "-" + 
												FormatDate.getDateFormat(FormatDate.PATTERN_yyyy).format(d), d, countyId, searchId);
										}
									}
								}
								line.add(instrumentNumber);
								
								if (tdList.elementAt(4).getChildren() != null){
									line.add(tdList.elementAt(4).getChildren().toHtml().trim());
								} else {line.add("");}
								
								if (tdList.elementAt(5).getChildren() != null){
									line.add(tdList.elementAt(5).getChildren().toHtml().trim());
								} else {line.add("");}
								
								body.add(line);
							}
						}
						if (body != null){
							ResultTable rt = new ResultTable();
							String[] header = { "InstrumentDate", "InstrumentNumber", "Book", "Page" };
							rt = GenericFunctions2.createResultTable(body, header);
							m.put("SaleDataSet", rt);
						}
					}
				}
			}
			
			
			
			try {
				partyNamesCOAdamsAO(m, searchId);
				parseLegalCOAdamsAO(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static void parseAddressCOAdamsAO(ResultMap resultMap, long searchId) {

		String address = (String) resultMap.get("tmpAddress");
			
		if (StringUtils.isEmpty(address))
			return;
			
		String city = "";
		for (int i = 0; i < CITIES.length; i++){
			if (address.toLowerCase().contains(CITIES[i].toLowerCase())){
				city = CITIES[i];
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				address = address.replace(city + ".*", "");
				address = address.replaceAll("(\\d)-([A-Z])", "$1$2");
				break;
			}
		}
			
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address.trim()));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address.trim()));
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesCOAdamsAO(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		String stringCoOwner = (String) m.get("tmpCoOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		//prosteala
		stringOwner = stringOwner.replaceAll("\\b([A-Z])(POWELL)\\b", "$1 AND $2");
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
		stringOwner = stringOwner.replaceAll("(?is)\\s+AND\\s+", " @@@@@@@ ");
		
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
		
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner);
		
	}
	
	public static void parseLegalCOAdamsAO(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
				
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = legal.replaceAll("<br>", " ").replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("(?is)\\b(LOTS?\\s*:?)(.*?AND)", "$1 $2 $1");
		legal = legal.replaceAll("(?is)\\bN\\s*/\\s*A\\b\\s*", " ").trim();
		
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
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(UNITS?\\s*:?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(PHASE\\s*:?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
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
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), township);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), range);
		}

		legal = legal.replaceAll("", "");
		
	}
	
}

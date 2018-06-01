package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;


/**
 * @author mihaib
*/

public class COSummitAO {
		
	
	@SuppressWarnings("rawtypes")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap resultMap, long searchId) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", "").replaceAll("(?is)&amp;", "&")
								.replaceAll("(?is)</?font[^>]*>", "");
		resultMap.put("PropertyIdentificationSet.SrcType", "AO");
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null); 
					
			String pid = "";
			pid = StringUtils.extractParameter(detailsHtml, "(?i)Schedule\\s*#\\s*:\\s*([^<]*)");
			
			if (StringUtils.isNotEmpty(pid)) {
				resultMap.put("PropertyIdentificationSet.ParcelID", pid.trim());
			}
			
			String legal = HtmlParser3.getValueFromNextCell(mainList.extractAllNodesThatMatch(new HasAttributeFilter("id", "propertyDetails")), "Property Desc:", "", false).trim();
			if (StringUtils.isEmpty(legal)){
				legal = HtmlParser3.getValueFromNextCell(mainList, "Property Desc:", "", false).trim();
			}
			resultMap.put("PropertyIdentificationSet.PropertyDescription", legal.trim());
			
			String address = HtmlParser3.getValueFromNextCell(mainList, "Physical Address:", "", false).trim();
			if (StringUtils.isNotEmpty(address)){
				address = address.replaceAll("(?is)\\([^\\)]+\\)", "").trim();
				
				String streetNo = StringFormats.StreetNo(address.trim());
				String streetName = StringFormats.StreetName(address.trim());
				
				
				if(StringUtils.isEmpty(streetNo)) {
					StandardAddress tokAddr = new StandardAddress(address);
					streetNo = tokAddr.getAddressElement(StandardAddress.STREET_NUMBER);
					if(StringUtils.isNotEmpty(streetNo)) {
						streetName = address.replaceFirst(streetNo, "").trim();
					}
				}
				if(streetNo.startsWith("0")) {
					streetNo = streetNo.replaceFirst("^0+", "");
				}
				
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				
			}
			String owners = HtmlParser3.getValueFromNextCell(mainList, "Primary:", "", false).trim();
			owners += "\n" + HtmlParser3.getValueFromNextCell(mainList, "Secondary:", "", false).trim();
			if (StringUtils.isEmpty(owners.trim())){
				owners = HtmlParser3.getValueFromNextCell(mainList, "Owner Name:", "", false).trim();
			}
			resultMap.put("tmpOwner", owners);
			
			String subCode = HtmlParser3.getValueFromNextCell(mainList, "Subcode:", "", false).trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionCode", subCode);
			String phase = HtmlParser3.getValueFromNearbyCell(3, HtmlParser3.findNode(mainList, "Subcode"), "", true).trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionPhase", phase.replaceAll("(?is)\\A0+", ""));
			String block = HtmlParser3.getValueFromNearbyCell(4, HtmlParser3.findNode(mainList, "Subcode"), "", true).trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock", block.replaceAll("(?is)\\A0+", ""));
			String lot = HtmlParser3.getValueFromNearbyCell(5, HtmlParser3.findNode(mainList, "Subcode"), "", true).trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", lot.replaceAll("(?is)\\A0+", ""));
			
			
			String twp = HtmlParser3.getValueFromNextCell(mainList, "Tship:", "", false).trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionTownship", twp);
			String rng = HtmlParser3.getValueFromNextCell(mainList, "Range:", "", false).trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionRange", rng);
			
			String totalAssess = HtmlParser3.getValueFromNextCell(mainList, "AssdVal:", "", false).trim();
			resultMap.put("PropertyAppraisalSet.TotalAssessment", totalAssess);
			
			NodeList tableList = mainList.extractAllNodesThatMatch(new HasAttributeFilter("id", "propertyDetails"), true);
			if (tableList.size() > 0){
				TableTag mainTable = (TableTag)tableList.elementAt(0);
				String tabel = mainTable.toHtml();
				tabel = tabel.replaceAll("[\\$,]+", "");
				List<List<String>> tableAsList = HtmlParser3.getTableAsList(tabel, false);
				List<List<String>> saleList = new ArrayList<List<String>>();
				for (int i = 0; i < tableAsList.size(); i++){
					if (tableAsList.get(i).size() > 3 && tableAsList.get(i).get(3).contains("Reception")){
						i++;
						while (!tableAsList.get(i).get(0).toLowerCase().contains("actual value")){
							List<String> list = tableAsList.get(i);
							while (list.size() > 4){
								list.remove(0);
							}
							if (list.get(0) != ""){
								saleList.add(list);
							}
							i++;
						}
						break;
					}
				}
				if (!saleList.isEmpty()){										
					List<List> body = new ArrayList<List>();
					List<String> line = new ArrayList<String>();
					
					for (List<String> everySale : saleList){
						line = new ArrayList<String>();
						if (everySale.get(0).contains("/")){//Sch No 400134
							String[] bookPage = everySale.get(0).split("/");
							line.add(bookPage[0]);
							if (bookPage.length == 2){
								line.add(bookPage[1]);
							} else {
								line.add("");
							}
							line.add("");
						} else {
							line.add("");
							line.add("");
							line.add(everySale.get(0));
						}
						line.add(everySale.get(1).replaceAll("(?is)\\s+\\d+\\s*:\\s*\\d+\\s*:\\s*\\d+\\s*[A|P]M", ""));//9/20/2007 1:29:30 PM on 6508467
						line.add(everySale.get(2));
						line.add(everySale.get(3));
						body.add(line);
					}
					if (!body.isEmpty()){
						ResultTable rt = new ResultTable();
						String[] header = {"Book", "Page", "InstrumentNumber", "InstrumentDate", "DocumentType", "SalesPrice"};
						rt = GenericFunctions2.createResultTable(body, header);
						resultMap.put("SaleDataSet", rt);
					}
				}
			}			
			try {
				parseLegalCOSummitAO(resultMap, searchId);
				partyNamesCOSummitAO(resultMap, searchId);

			}catch(Exception e) {
				e.printStackTrace();
			}
			resultMap.removeTempDef();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesCOSummitAO(ResultMap m, long searchId) throws Exception {
		
		String owner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(owner))
			return;		
				
		owner = owner.replaceAll("(?is)\\b(?:CO-)?(TRUSTEE)\\b", " $1");
		owner = owner.replaceAll("(?is)\\bFOR THE BENIFIT OF\\b", "").replaceAll("(?is)[\\(\\)]+", "");
		owner = owner.trim();
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes, types, otherTypes;

		String[] owners = owner.split("\n");
		
		for (int i = 0; i < owners.length; i++) {
			if (owners[i].trim().matches("\\AA COLORADO LIMITED LIABILITY COMPANY\\s*,?$"))
				continue;
			owners[i] = owners[i].replaceAll("(?is)\\s*,\\s*", ", ").replaceAll("(?is),\\s*$", "");
			String[] multiOwnerOnSameLine = owners[i].split(";");
			for (int j = 0; j < multiOwnerOnSameLine.length; j++) {
				names = StringFormats.parseNameNashville(multiOwnerOnSameLine[j], true);
	
				types = GenericFunctions.extractAllNamesType(names);
				otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractAllNamesSufixes(names);
				
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		owner = owner.replaceAll("(?is)\n", " & ");
		m.put("PropertyIdentificationSet.NameOnServer", owner);
		String[] a = StringFormats.parseNameNashville(owner, true);
		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
		
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesIntermCOSummitAO(ResultMap resultMap, long searchId) throws Exception {
		
		String stringOwner = (String) resultMap.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		String ppiSearch = (String) resultMap.get("tmpPPI");
		
		stringOwner = stringOwner.replaceAll("(?is)\\b(?:CO-)?(TRUSTEE)\\b", " $1");
		stringOwner = stringOwner.trim();
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes, types, otherTypes;

		String[] owners = stringOwner.split("\n");

		for (int i = 0; i < owners.length; i++) {
			if (ppiSearch != null && ppiSearch.equals("PPISearch")){
				names = StringFormats.parseNameNashville(owners[i], true);
			} else {
				names = StringFormats.parseNameDesotoRO(owners[i], true);
			}

			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractAllNamesSufixes(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		
		resultMap.put("PropertyIdentificationSet.NameOnServer", stringOwner);
		String[] a = StringFormats.parseNameDesotoRO(stringOwner, true);
		resultMap.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		resultMap.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		resultMap.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		resultMap.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		resultMap.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		resultMap.put("PropertyIdentificationSet.SpouseLastName", a[5]);
		
	}
	
	
	public static void parseLegalCOSummitAO(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
				
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)\\s+THRU\\s+", "-");
		legal = legal.replaceAll("(?is)\\bALSO ROOM\\b", " UNIT "); 
		
		legal = legal.replaceAll("(?is)&quot;", "\"");
		legal = legal.replaceAll("(?is)\\\"[^\\\"]+\\\"", "");
		legal = legal.replaceAll("(?is)[\\d\\sX]+\\s*OF\\s+", "");
		legal = legal.replaceAll("(?is)\\s+(TR(ACT)?)\\b", " , $1");
		legal = legal.replaceAll("(?is)\\s+(PH(ASE)?)\\b", " , $1");
		//legal = legal.replaceAll("(?is)\\s+[NSEW]\\s*/\\s*\\d+\\s*OF\\s*(\\d+)", " L $1");
		//legal = legal.replaceAll("(?is)(\\w),(\\w)\\s+(\\w)", "$1,$2,$3");
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		String legalTemp = legal;
			
		// extract lot from legal description
		String lot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
		if (lot == null){
			lot = "";
		}
		Pattern p = Pattern.compile("(?is)\\b(LOTS?)\\s*((?:[\\d\\s,\\&-]+|[A-Z]?\\d+-?[A-Z]?)|[A-Z]{1,2})\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
				
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block =  (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
		if (block == null){
			block = "";
		}
		p = Pattern.compile("(?is)\\b(BLOC?KS?)\\s*((?:[A-Z]?\\d+-?[A-Z]?)|[A-Z]{1,2}|[\\d\\s,\\&-]+)\\b");
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
			
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UNIT)\\s*([A-Z]?-?(\\d+)?-?[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract phase from legal description
		String phase = (String) m.get("PropertyIdentificationSet.SubdivisionPhase");
		if (phase == null){
			phase = "";
		}
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			if (phase.length() != 0) {
				phase = LegalDescription.cleanValues(phase, false, true);
				m.put("PropertyIdentificationSet.SubdivisionPhase", phase);// ma.group(2));
			}
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

	
		// extract section from legal description
		p = Pattern.compile("(?is)\\b(Sec)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2).replaceAll("(?is)\\A\\s*0+", ""));
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		legal = legalTemp;
		
		// extract tract from legal description
		p = Pattern.compile("(?is)\\b(TractS?)\\s*([A-Z]-?\\d?|[\\d-\\s&]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(2));
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		legal = legalTemp;

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDGS?)\\s*((?:[A-Z]?\\d+-?[A-Z]?)|[A-Z]{1,2}|[\\d\\s,\\&-]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
				
		//extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?is)\\b(?:BLDG)\\s+(.*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\b(?:UNIT)\\s+(.*)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\b(?:BLOC?KS?)\\s+(.*)\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\b(?:LOTS?)\\s+(.*)\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					}
				}
			}
		}
		if (subdiv.toLowerCase().contains("minning claim")){
			subdiv = "";
		}
		if (subdiv.length() != 0) {

			subdiv = subdiv.replaceAll("(?is)#\\s*\\d+\\s*,?", "");
			subdiv = subdiv.replaceAll("\\A\\s*OF\\s+", "");
			subdiv = subdiv.replaceAll("\\A.*?LOT\\s+OF\\s+", "");
			subdiv = subdiv.replaceAll("(?is)(.*?)REV\\b.*", "$1");
			subdiv = subdiv.replaceAll("(.*)(\\d)(ST|ND|RD|TH)\\s*(ADDN)", "$1" + "$2");
			subdiv = subdiv.replaceAll("\\bBLK\\b", "");
			subdiv = subdiv.replaceAll("(?is)(.*),?\\s*phase.*", "$1");
			subdiv = subdiv.replaceAll("(.*)(UNREC.*)", "$1");
			subdiv = subdiv.replaceAll("(.+)\\s+(A\\s+)?CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceAll("\\A(.+?) SUB(DIVISION)?.*", "$1");
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv.trim());
			if (legal.matches(".*\\bCONDO.*"))
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv.trim());
		}
			
	}
		
}

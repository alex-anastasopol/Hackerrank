package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class COElPasoAO {
	
    
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&nbsp", " ").replaceAll("(?is)&amp;", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)</?font[^>]*>", "").replaceAll("(?is)</?strong[^>]*>", "")
									.replaceAll("(?is)</?h[\\d]*[^>]*>", "").replaceAll("(?is)</th>", "</td>").replaceAll("\n", "").replaceAll("</?div[^>]*>", "");
		m.put("OtherInformationSet.SrcType","AO");

		try {		
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Schedule No:"), "", true).trim();
			pid = pid.replaceAll("(?is)Inactive\\s+on.*", "").trim();
			m.put("PropertyIdentificationSet.ParcelID", StringUtils.isNotEmpty(pid) ? pid : "");
			m.put("PropertyIdentificationSet.ParcelID2", StringUtils.isNotEmpty(pid) ? pid : "");

			String ownerName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Owner Name:"), "", true).trim();
			ownerName = ownerName.replaceAll("(?is)</?select[^>]*>", "").replaceAll("(?is)<option[^>]*>", "").replaceAll("(?is)</option[^>]*>", "<br>");
			m.put("tmpOwner", ownerName);
				
			String siteAddress = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Location:"), "", true).trim();
			if (StringUtils.isNotEmpty(siteAddress)){
				siteAddress = siteAddress.replaceAll("\\A0+", "").replaceAll("(?is)</?select[^>]*>", "").replaceAll("(?is)<option[^>]*>", "")
												.replaceAll("(?is)</?option[^>]*>", "@@@@").replaceAll("(?is)@@@@\\s*$", "").trim();
				String[] addressRows = siteAddress.split("@@@@");
				siteAddress = addressRows[0].trim();
				if (org.apache.commons.lang.StringUtils.isAlpha(siteAddress.substring(0, 1)) && addressRows.length > 1 ){
					for (String addr : addressRows){
						if (org.apache.commons.lang.StringUtils.isNumeric(addr.trim().substring(0, 1))){
							siteAddress = addr.trim();
							break;
						}
					}
				}
				m.put("tmpAddress", siteAddress);
			}
			
			String legal = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Legal Description"), "", true).trim();
			legal = legal.replaceAll("(?is)</?select[^>]*>", "").replaceAll("(?is)<option[^>]*>", "").replaceAll("(?is)</option[^>]*>", " ");
			m.put("PropertyIdentificationSet.PropertyDescription", legal);
			
			try {
				String transacHistTable = HtmlParser3.getNodeByID("ctl00_ContentPlaceHolder1_saleTable", mainList, true).toHtml();
				if (StringUtils.isNotEmpty(transacHistTable)) {
					transacHistTable = transacHistTable.replaceAll("\r", " ").replaceAll("\t", " ").replaceAll("\n", " ");
					List<List<String>> transacHist = HtmlParser3.getTableAsList(transacHistTable, false);
					List<List> newBody = new ArrayList<List>(transacHist);
					for (List lst : newBody) {
						if (lst.get(0).toString().trim().matches("0+")) {
							lst.set(0, lst.get(0).toString().replaceAll("0+", ""));
						}
						if (lst.get(1).toString().trim().matches("0+")) {
							lst.set(1, lst.get(1).toString().replaceAll("0+", ""));
						}
						if (lst.get(2).toString().trim().matches("0+")) {
							lst.set(2, lst.get(2).toString().replaceAll("0+", ""));
						}
					}

					ResultTable rt = new ResultTable();
					String[] header = { "InstrumentNumber", "Book", "Page",	"SalesPrice", "InstrumentDate", "Grantee", "Grantor", "DocumentType" };

					rt = GenericFunctions2.createResultTable(newBody, header);
					m.put("SaleDataSet", rt);
				}
			} catch (Exception e) {
			}
			try {
				partyNamesCOElPasoAO(m, searchId);
				parseLegalCOElPasoAO(m, searchId);
				parseAddressCOElPasoAO(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static void parseAddressCOElPasoAO(ResultMap resultMap, long searchId) {

		String address = (String) resultMap.get("tmpAddress");
			
		if (StringUtils.isEmpty(address))
			return;
		
		
		resultMap.put("PropertyIdentificationSet.AddressOnServer", address);
		address = address.replaceAll("\\A0+", "");
	
		address = Normalize.trim(address);
		//ghertzo // 7402403020 from B 5078
		if (address.trim().matches("\\A\\d+(\\s+[NSWE]{1,2})?\\s+\\d+\\s+\\d+[A-Z]{2}\\s+.*")){
			String first = address.replaceAll("\\A\\d+(?:\\s+[NSWE]{1,2})?\\s+(\\d+)\\s+\\d+[A-Z]{2}\\s+.*", "$1");
			String sec = address.replaceAll("\\A\\d+(?:\\s+[NSWE]{1,2})?\\s+(\\d+)\\s+(\\d+)[A-Z]{2}\\s+.*", "$2");
			if (StringUtils.isNotEmpty(first) && StringUtils.isNotEmpty(sec)){
				int whole = Integer.parseInt(first) + Integer.parseInt(sec);
				address = address.replaceAll("\\A(\\d+(?:\\s+[NSWE]{1,2})?)\\s+\\d+\\s+\\d+([A-Z]{2}\\s+.*)", "$1 " + Integer.toString(whole) + "$2");
			}
		}
		resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address.trim()));
		resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address.trim()).replaceAll("\\A0+", ""));
		
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static void partyNamesCOElPasoAO(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = stringOwner.replaceAll("\r", " ").replaceAll("\t", " ").replaceAll("\n", " ");
		
		m.put("PropertyIdentificationSet.NameOnServer", stringOwner.trim().replaceAll("<br>", " & "));
		
		stringOwner = stringOwner.replaceAll("\\s*\r\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\s*%\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\b\\d+/\\d+\\b", "");
		//stringOwner = stringOwner.replaceAll("[\\d%]+", "");
		stringOwner = stringOwner.replaceAll("\\bINT\\b", "");
		stringOwner = stringOwner.replaceAll("\\bAS( TRUSTEE) OF THE\\b", "$1");
		stringOwner = stringOwner.replaceAll("\\bTHE UND\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s+Heirs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*&\\s*", " & ");
		stringOwner = stringOwner.replaceAll("(?is)\\bMrs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s+\\+\\s+", " & ");
		stringOwner = stringOwner.replaceAll("(?is)\\bF/K/A\\b", "");
		stringOwner = stringOwner.trim();
			
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		String[] owners = stringOwner.split("<br>");
		for (int i = 0; i < owners.length; i++) {
			boolean addVonDer = false;
			if(owners[i].contains("VON DER")){
				owners[i] = owners[i].replace("VON DER", "");
				addVonDer = true;
			}
			
			names = StringFormats.parseNameNashville(owners[i], true);
			
			if(addVonDer)
				names[2] = "VON DER " + names[2];				
			
			if (owners[i].trim().startsWith("C/O")){
				owners[i] = owners[i].replaceAll("(?is)\\bC/O\\b", " ");
				names = StringFormats.parseNameDesotoRO(owners[i], true);
			}
			
			names[2] = names[2].replaceAll("(?is),", "");
			if (StringUtils.isNotEmpty(names[5]) && 
					LastNameUtils.isNotLastName(names[5]) && 
					NameUtils.isNotCompany(names[5])){
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
			}
			if (names[1].matches("\\d+")){
				names[1] = "";
			}
			if (names[4].matches("\\d+")){
				names[4] = "";
			}
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractAllNamesSufixes(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		stringOwner = stringOwner.replaceAll("\\s*@@@\\s*", " AND ");
		stringOwner = stringOwner.replaceAll("<br>", " & ").replaceAll(" &\\s*$", "");
		
		/*String[] a = StringFormats.parseNameNashville(stringOwner.trim(), true);
		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);*/
			
	}
	
	public static void parseLegalCOElPasoAO(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
				
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)(\\d+)\\s*THRU\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(?is)(\\d+)\\s+(?:TO|ALL)\\s+(\\d+)", "$1 + $2");
		legal = legal.replaceAll("(?is)\\b+(OF\\s+)?[NSWE]+\\s*[\\d/]+\\s*(FT\\s+)?OF\\s+", "");
		
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s*([\\d\\s,\\+-]+[A-Z]?\\d?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		
		lot = lot.replaceAll("[\\+,]", " ").replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLKS?)\\s*([\\d-]+[A-Z]?|[A-Z])\\b");
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
		
		p = Pattern.compile("(?is)\\b(UNITS?)\\s*([A-Z]?[\\d-]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(ABST)\\s+([\\d+\\s&]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.AbsNo", ma.group(2).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(1), "");
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legal = legal.replaceAll(ma.group(0), "");
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(2).replaceAll("\\s*,\\s*", " ").trim());
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
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-Z]?)\\b");
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
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s*-\\s*(\\d+[A-Z]?)\\s*-\\s*(\\d+[A-Z]?)\\b");
		Matcher mp = p.matcher(legal);
		if (mp.find()) {
			section = mp.group(2);
			township = mp.group(3);
			range = mp.group(4);
			m.put("PropertyIdentificationSet.SubdivisionSection", section);
			m.put("PropertyIdentificationSet.SubdivisionTownship", township);
			m.put("PropertyIdentificationSet.SubdivisionRange", range);
		}
		
		p = Pattern.compile("(?is)\\b(SECT?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		String subdiv = "";

		p = Pattern.compile("(?is)\\bTR(?:ACT)?\\s+(.*?)($|S\\s*#.*|[\\d\\.]+.*)");
		ma = p.matcher(legal);
		if (ma.find()){
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\bBLKS?\\s+(.*?)($|S\\s*#.*|[\\d\\.]+.*)");
			ma.usePattern(p);
			ma = p.matcher(legal);
			if (ma.find()){
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\b(?:LOT|ABST|UNIT)\\s+(.*?)($|S\\s*#.*|[\\d\\.]{3,}\\s+.*)");
				ma.usePattern(p);
				ma = p.matcher(legal);
				if (ma.find()){
					subdiv = ma.group(1);
					subdiv = subdiv.replaceAll("(?:LOT|ABST|UNIT),?", "");
					subdiv = subdiv.replaceAll("[\\d\\.]{3,}", "");
				}
			}
		}
			
		subdiv = subdiv.replaceAll(",", "");
		subdiv = subdiv.replaceAll("(.+)\\s+(SEC|PH).*", "$1");
		subdiv = subdiv.replaceAll("(.+)\\s+\\*+\\s*NEW\\s+PARCEL.*", "$1");
		subdiv = subdiv.replaceAll("(.+)\\s+,?\\s*TOG\\s+WITH.*", "$1");
		subdiv = subdiv.replaceAll("(.+)\\s+\\s*FILL?\\s+NO.*", "$1");
		subdiv = subdiv.replaceAll("\\ABLKS?\\s+(.+)", "$1");
		subdiv = subdiv.replaceAll("(.+)\\s+SUB.*", "$1");
		m.put("PropertyIdentificationSet.SubdivisionName", subdiv.trim());
		legal = legal.replaceAll("", "");
	}
	
}

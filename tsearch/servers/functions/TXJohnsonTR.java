package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class TXJohnsonTR {
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		
		try {		
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String taxTable = "";
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			for (int k = tables.size() - 1; k < tables.size(); k--){
				if (tables.elementAt(k).toHtml().contains("Levy Amount")){
					taxTable= tables.elementAt(k).toHtml();
					break;
				}
			}
			
			String currentTaxYear = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(mainList, "Values"), "", true);
			currentTaxYear = currentTaxYear.replaceAll("(?is)\\A\\s*(\\d+)\\s*Values\\s*$", "$1");
			m.put("TaxHistorySet.Year", currentTaxYear);
			
			if (StringUtils.isNotEmpty(taxTable)){
				try{
				org.htmlparser.Parser parserO = org.htmlparser.Parser.createParser(taxTable, null);
				NodeList taxTableList = parserO.parse(null);
				TableTag mainTable = (TableTag) taxTableList.elementAt(0);
				TableRow[] rows = mainTable.getRows();
				//int lastYear = 0;
				List<List> body = new ArrayList<List>();
				List<String> line = null;
				String priorDelinq = "";
				
				//Date paidDate = HashCountyToIndex.getPayDate(commId, miServerID);
				//String year = paidDate.toString().replaceAll(".*?\\s+(\\d{4})", "$1").trim();
				//int taxYearPD = Integer.parseInt(year);
				
				for (int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					if (row.getColumnCount() == 9) {
						TableColumn[] cols = row.getColumns();
						if (cols[0].getChild(0).toHtml().trim().matches("(?is)\\d+\\s+totals")){
							
							String taxYear = cols[0].toPlainTextString().replaceAll("(?is)totals", "").trim();
							if (StringUtils.isEmpty(taxYear)){
								throw new RuntimeException("Exception when attempt to obtain the tax year!");
							}
							if (taxYear.equals(currentTaxYear)){
								line = new ArrayList<String>();
																
								String amountPaid = cols[3].toPlainTextString().trim();
								amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
								line.add(amountPaid);
								body.add(line);
								
								//if (Integer.parseInt(taxYear) == taxYearPD){
									
									m.put("TaxHistorySet.AmountPaid", StringUtils.isNotEmpty(amountPaid) ? amountPaid : "");
									
									String baseAmount = cols[2].toPlainTextString().trim();
									baseAmount = baseAmount.replaceAll("(?is)[\\$,]+", "");
									m.put("TaxHistorySet.BaseAmount", StringUtils.isNotEmpty(baseAmount) ? baseAmount : "");
		
									String totalDue = cols[8].toPlainTextString().trim();
									totalDue = totalDue.replaceAll("(?is)[\\$,]+", "");
									m.put("TaxHistorySet.TotalDue", StringUtils.isNotEmpty(totalDue) ? totalDue : "");
									//lastYear++;
								//} else {
								//	priorDelinq += "+" + cols[8].toPlainTextString().trim();
									//lastYear++;
								//}
							} else {
								line = new ArrayList<String>();
									
								String amountPaid = cols[3].toPlainTextString().trim();
								amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
								line.add(amountPaid);
								body.add(line);
									
								priorDelinq += "+" + cols[8].toPlainTextString().trim();
							}
						}
	
					}
				}
				
				if (StringUtils.isEmpty((String) m.get("TaxHistorySet.Year"))){
					TableColumn[] cols = rows[1].getColumns();
					m.put("TaxHistorySet.Year", cols[0].toPlainTextString().trim());
				}
				
				
				m.put("TaxHistorySet.PriorDelinquent", GenericFunctions2.sum(priorDelinq.replaceAll("(?is)[\\$,]+", ""), searchId));
				ResultTable rt = new ResultTable();
				String[] header = {"ReceiptAmount"};
				rt = GenericFunctions2.createResultTable(body, header);
				m.put("TaxHistorySet", rt);
				
				} catch (Exception e) {
						e.printStackTrace();
				}
			}
			
			try {
				partyNamesTXJohnsonTR(m, searchId);
				parseLegalTXJohnsonTR(m, searchId);
				parseAddressTXJohnsonTR(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static String scoateNumeDinStringCuAdresa(String myString, String regexForSplit){
		if (StringUtils.isEmpty(myString))
			return "";
		
		String name = "";
		String[] rows = myString.split(regexForSplit);
		for(String row : rows){
			row = row.replaceAll("\\(NO ADRS", "9999 ");
			row = row.replaceAll("MARRIED MAN SEPARATE PROPERTY", "99999 ");
			if (!row.trim().matches("\\*?\\d+.*")){
				if (!row.matches(".*(P\\s*O|RT\\s*\\d+)\\s+BOX.*")) {
					name += " " + row;
				} else 
					break;
				
			} else if (NameUtils.isCompany(row)){
				String[] tokens = row.split("[\\s]+");
				boolean isAddress = false;
				for (String token : tokens){
					if (Normalize.isSuffix(token)) {
						isAddress = true;
						break;
					}
				}
				if (!isAddress){
					name += " " + row;
				} else 
					break;
			} else 
				break;
		}
		
		return name.trim();
	}
	
	public static void parseAddressTXJohnsonTR(ResultMap resultMap, long searchId) {

		String address = (String) resultMap.get("tmpAddress");
			
		if (StringUtils.isEmpty(address))
			return;
		
		address = address.replaceAll("\\A0+", "");
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
		
		if (address.matches("(?is)\\A\\d+(ST|ND|RD|TH)\\s+.*")){//000081213TH ST this kind of address can't be parsed correctly so they not must be boostraped
			
		} else {
			address = address.replaceAll("(?is)\\A\\s*(\\d+)([A-Z]{3,})", "$1 $2");
			address = address.replaceAll("(?is)\\A\\s*(\\d+)([A-Z])\\s+", "$1 $2 ");//400E MAIN ST
			resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address.trim()));
			resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address.trim()).replaceAll("\\A0+", ""));
		}
		
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static void partyNamesTXJohnsonTR(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = scoateNumeDinStringCuAdresa(stringOwner, "<br>");
		
		stringOwner = stringOwner.toUpperCase();
		stringOwner = stringOwner.replaceAll("\\s*\r\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\s*%\\s*", "@@@");
		stringOwner = stringOwner.replaceAll("\\b\\d+/\\d+\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\b[A-Z]/[A-Z]/[A-Z]\\b", "&");
		stringOwner = stringOwner.replaceAll("\\s*/\\s*", " & ");
		//stringOwner = stringOwner.replaceAll("[\\d%]+", "");
		stringOwner = stringOwner.replaceAll("\\bINT\\b", "");
		stringOwner = stringOwner.replaceAll("\\bAS (TRUSTEE) OF THE\\b", " $1");
		stringOwner = stringOwner.replaceAll("\\bTHE UND\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s+Heirs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*&\\s*", " & ");
		stringOwner = stringOwner.replaceAll("(?is)\\bMrs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)([^&]+)&\\s*(\\w+)\\s*&\\s*(\\w+)\\s+(\\w+)\\s*$", "$1 @@@ $4 $2 & $3");
		stringOwner = stringOwner.replaceAll("(?is)\\b(REV LIV )TR\\b", "$1 TRUST");
		stringOwner = stringOwner.replaceAll("(?is)\\b(TRUST)\\s+([A-Z]+)\\s+", "$1 @@@ $2 ");
		stringOwner = stringOwner.replaceAll("\\b(TRUSTEES)\\s+(.+)", "$1@@@$2");
		
		while (stringOwner.matches("[^&]+&\\s*[^&]+&\\s*.*")){//B 5069
			stringOwner = stringOwner.replaceAll("(?is)([^&]+&\\s*[^&]+)&\\s*(.*)", "$1 @@@ $2");
		}
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] coNames = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		String[] owners = stringOwner.split("\\s*@@@\\s*");
		for (int i = 0; i < owners.length; i++) {
			names = StringFormats.parseNameNashville(owners[i], true);
			
			names[2] = names[2].replaceAll("(?is),", "");
			if (NameUtils.isNotCompany(names[5])){
				if (stringOwner.trim().matches("[^&]+&\\s+\\w+\\s+\\w{3,}(?:\\s+\\w+)?")){
					String coOwner = stringOwner.replaceAll("[^&]+&\\s+(\\w+\\s+\\w{3,}(?:\\s+\\w+)?)", "$1");
					coNames = StringFormats.parseNameDesotoRO(coOwner, true);
					names[3] = coNames[0];
					names[4] = coNames[1];
					names[5] = coNames[2];
				}
				boolean isEmpty = StringUtils.isNotEmpty(names[5]) &&StringUtils.isNotEmpty(names[3]); 
				if (isEmpty && LastNameUtils.isNotLastName(names[5]) && (!FirstNameUtils.isFirstName(names[3]))){
					String [] coOwner = new String [] { names[3], names[4], names[5] }; 
					names[3] = coOwner[2];
//					names[4] = coOwner[1];
					names[5] = coOwner[0];
				}
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
	
	public static void parseLegalTXJohnsonTR(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
				
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s*([\\d\\s,]+[A-Z]?\\d?)\\b");
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
		
		p = Pattern.compile("(?is)\\b(UNITS?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
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
		if (!legal.contains("GAS WELL")){
			if (!legal.contains("MOBILE HOME")){
				p = Pattern.compile("(?is)\\bTR\\s+(.*?)($|S\\s*#.*|[\\d\\.]+.*)");
				ma = p.matcher(legal);
				if (ma.find()){
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile("(?is)\\bBLK\\s+(.*?)($|S\\s*#.*|[\\d\\.]+.*)");
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
				
			}		
		}
		subdiv = subdiv.replaceAll(",", "");
		subdiv = subdiv.replaceAll("(.+)\\s+(SEC|PH).*", "$1");
		m.put("PropertyIdentificationSet.SubdivisionName", subdiv.trim());
		legal = legal.replaceAll("", "");
		
	}
	
}

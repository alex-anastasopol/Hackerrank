package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Date;
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
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class COPuebloTR {
	
	private static String[] CITIES = {"Avondale", "Beulah Valley", "Boone", "Colorado City", "Pueblo Springs Ranch", 
											"Pueblo West", "Pueblo", "Rye", "Salt Creek", "Vineland"};
    
	@SuppressWarnings("rawtypes")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&nbsp", " ").replaceAll("(?is)&amp;", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)<CAPTION[^>]*>", "<tr><td>").replaceAll("(?is)</CAPTION[^>]*>", "</td></tr>")
									.replaceAll("(?is)<th", "<td").replaceAll("(?is)</th>", "</td>").replaceAll("\n", "").replaceAll("</?div[^>]*>", "");
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		int commId = currentInstance.getCommunityId();
		try {		
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			Matcher mat = ro.cst.tsearch.servers.types.COPuebloTR.SCHEDULE_PIN_PAT.matcher(detailsHtml);
			if (mat.find()){
				String pid = mat.group(1).trim();
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid);
				m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), pid);
			}		

			mat.reset();
			Pattern pat = Pattern.compile("(?is)<dt>Name\\(s\\):\\s*</dt>(.*?)<dt>Location Address:\\s*</dt>");
			mat = pat.matcher(detailsHtml);
			if (mat.find()){
				String ownerName = mat.group(1);
				ownerName = ownerName.replaceAll("(?is)<dt>[^<]*</dt>", "").replaceAll("(?is)<dd>", "");
				m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName : "");
			} else {
				mat.reset();
				pat = Pattern.compile("(?is)<dt>Name\\(s\\):\\s*</dt>(.*?)<dt>Mailing Address:\\s*</dt>");
				mat = pat.matcher(detailsHtml);
				if (mat.find()){
					String ownerName = mat.group(1);
					ownerName = ownerName.replaceAll("(?is)<dt>[^<]*</dt>", "").replaceAll("(?is)<dd>", "");
					m.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName : "");
				}
			}
			
			mat.reset();
			pat = Pattern.compile("(?is)<dt>Legal Description:\\s*</dt>\\s*<dd>([^<]*)</dd>");
			mat = pat.matcher(detailsHtml);
			if (mat.find()){
				String legal = mat.group(1).trim();
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), StringUtils.isNotEmpty(legal) ? legal : "");
			}
			
			mat.reset();
			pat = Pattern.compile("(?is)<dt>Location Address:\\s*</dt>\\s*<dd>([^<]*)</dd>");
			mat = pat.matcher(detailsHtml);
			if (mat.find()){
				String address = mat.group(1).trim();
				m.put("tmpAddress", StringUtils.isNotEmpty(address) ? address : "");
			}
			
			String propertyType = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Property Type"), "", true).trim();
			if (propertyType.toLowerCase().contains("real")){
				m.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "REAL ESTATE");
			}
			
//			String baseAmount = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Total Tax Amount"), "", true).trim();
//			if (StringUtils.isNotEmpty(baseAmount)){
//				baseAmount = baseAmount.replaceAll("(?is)[\\$,]+", "");
//				m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
//			}
			
			Date paidDate = HashCountyToIndex.getPayDate(commId, miServerID);
			String year = paidDate.toString().replaceAll(".*?\\s+(\\d{4})", "$1").trim();
			int taxYearPD = Integer.parseInt(year) - 1;
			m.put(TaxHistorySetKey.YEAR.getKeyName(), Integer.toString(taxYearPD));
			
			String taxTable = "";
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			try {
				for (int k = tables.size() - 1; k < tables.size(); k--) {
					if (tables.elementAt(k).toHtml().contains("Gross Tax Amount")) {
						taxTable = tables.elementAt(k).toHtml();
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (StringUtils.isNotEmpty(taxTable)){
				try{
					taxTable = taxTable.replaceAll("[\\)\\(\\$,]+", "");
					org.htmlparser.Parser parserO = org.htmlparser.Parser.createParser(taxTable, null);
					NodeList taxTableList = parserO.parse(null);
					TableTag mainTable = (TableTag) taxTableList.elementAt(0);
					TableRow[] rows = mainTable.getRows();
						
					List<List<List<String>>> yearsList = new ArrayList<List<List<String>>>();
					List<List<String>> bodyList = new ArrayList<List<String>>();
					List<String> lineList = null;
					
					for (int i = 1; i < rows.length; i++) {
						TableRow row = rows[i];
						if (row.getColumnCount() > 0) {
							TableColumn[] cols = row.getColumns();
							TableColumn[] prevCols = rows[i-1].getColumns();
							String taxYear = cols[0].toPlainTextString().trim();
							if (!taxYear.equals(prevCols[0].toPlainTextString().trim())){
								if (!bodyList.isEmpty()){
									yearsList.add(bodyList);
								}
								bodyList = new ArrayList<List<String>>();
								lineList = new ArrayList<String>();
								lineList.add(cols[0].toPlainTextString().trim());
								lineList.add(cols[1].toPlainTextString().trim());
								lineList.add(cols[2].toPlainTextString().trim());
								lineList.add(cols[3].toPlainTextString().trim());
								lineList.add(cols[5].toPlainTextString().trim());
								lineList.add(cols[6].toPlainTextString().trim());
								lineList.add(cols[7].toPlainTextString().trim());
								bodyList.add(lineList);
							} else {
								lineList = new ArrayList<String>();
								lineList.add(cols[0].toPlainTextString().trim());
								lineList.add(cols[1].toPlainTextString().trim());
								lineList.add(cols[2].toPlainTextString().trim());
								lineList.add(cols[3].toPlainTextString().trim());
								lineList.add(cols[5].toPlainTextString().trim());
								lineList.add(cols[6].toPlainTextString().trim());
								lineList.add(cols[7].toPlainTextString().trim());
								bodyList.add(lineList);
							}
						}
					}
					//add last Year
					if (!bodyList.isEmpty()){
						yearsList.add(bodyList);
					}
					if (!yearsList.isEmpty()){
						List<List> body = new ArrayList<List>();
						List<String> line = null;
						String priorDue = "", priorPaid = "";
						String currentYearDue = "", currentYearPaid = "";
						String baseAmount = "";
						
						for (List<List<String>> listForEachYear : yearsList){
							for (List<String> listRow : listForEachYear){
								if (listRow.get(0).equals(Integer.toString(taxYearPD))) {
									if (listRow.get(3).trim().toLowerCase().equals("real")) {
										baseAmount += listRow.get(4).trim().replaceAll("[\\$,]", "");
									}
									
									if (listRow.get(1).toLowerCase().equals("due")) {
										currentYearDue += "+" + listRow.get(6);
										
									} else if (listRow.get(1).toLowerCase().equals("payment") ){
										currentYearPaid += "+" + listRow.get(6);
										line = new ArrayList<String>();
										line.add(listRow.get(2));
										line.add(listRow.get(6));
										body.add(line);
									}
								} else {
									if (listRow.get(1).toLowerCase().equals("due")){
										priorDue += "+" + listRow.get(6).replaceAll("\\*+", "");
										
									} else if (listRow.get(1).toLowerCase().equals("payment")) {
										priorPaid += "+" + listRow.get(6);
										line = new ArrayList<String>();
										line.add(listRow.get(2));
										line.add(listRow.get(6));
										body.add(line);
									}
								}
							}
						}
						
						priorDue = GenericFunctions2.sum(priorDue, searchId);
						priorPaid = GenericFunctions2.sum(priorPaid, searchId);
						double priorDelinq = Double.parseDouble(priorDue);
						double priorPaids = Double.parseDouble(priorPaid);
						m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), Double.toString(priorDelinq - priorPaids));

						baseAmount = GenericFunctions2.sum(baseAmount, searchId);
						currentYearDue = GenericFunctions2.sum(currentYearDue, searchId);
						currentYearPaid = GenericFunctions2.sum(currentYearPaid, searchId);
						double amountDue = Double.parseDouble(currentYearDue);
						double amountPaid = Double.parseDouble(currentYearPaid);
						
						if (StringUtils.isNotEmpty(baseAmount)){
							m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
						}
						m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), Double.toString(amountDue - amountPaid));
						m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), Double.toString(amountPaid));
						
						ResultTable rt = new ResultTable();
						String[] header = {"ReceiptDate", "ReceiptAmount"};
						rt = GenericFunctions2.createResultTable(body, header);
						m.put("TaxHistorySet", rt);	
					}
				} catch (Exception e) {
						e.printStackTrace();
				}
			}
			
			String transacHistTable = "";
			for (int k = tables.size() - 1; k > 0; k--){
				if (tables.elementAt(k).toHtml().contains("Sale Date")){
					transacHistTable= tables.elementAt(k).toHtml();
					break;
				}
			}
			if (StringUtils.isNotEmpty(transacHistTable)){
				transacHistTable = transacHistTable.replaceAll("(?is)>\\s*0+\\s*<", "><").replaceAll("\\bN/A\\b", "");
				transacHistTable = transacHistTable.replaceAll("(?is)<tr>\\s*<td>\\s*Transfer History\\s*</td>\\s*</tr>\\s*<tdead>", "");
				List<List<String>> transacHist = HtmlParser3.getTableAsList(transacHistTable, false);
				if (transacHist.size()>0) {
					List<String> firstRow = transacHist.get(0);
					if (firstRow.size()>0 && firstRow.get(0).contains("Sale Date")) {
						transacHist.remove(0);	//remove first row (header)
					}
				}
				for(List<String> list : transacHist){
					list.remove(4);
					list.remove(8);
				}
				List<List> newBody = new ArrayList<List>(transacHist);
	
				
				ResultTable rt = new ResultTable();
				String[] header = {"InstrumentDate", "SalesPrice", "InstrumentNumber", "DocumentType", "Grantor", "Grantee", "Book", "Page"};
							
				rt = GenericFunctions2.createResultTable(newBody, header);
				m.put("SaleDataSet", rt);
			}
	
			String landAppraisal = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Assessed Value"),"", true).trim();
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "").replaceAll("(?is)</?b>", "").replaceAll("(?is)[A-Z]/[A-Z]", "");
			m.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), landAppraisal);
			
			String improvementAppraisal = HtmlParser3.getValueFromAbsoluteCell(2, 0, HtmlParser3.findNode(mainList, "Assessed Value"),"", true).trim();
			landAppraisal = landAppraisal.replaceAll("[\\$,]", "").replaceAll("(?is)</?b>", "").replaceAll("(?is)[A-Z]/[A-Z]", "");
			m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvementAppraisal);
			
			String assessed = HtmlParser3.getValueFromAbsoluteCell(3, 0, HtmlParser3.findNode(mainList, "Assessed Value"),"", true).trim();
			assessed = assessed.replaceAll("(?is)[\\$,]", "$1").replaceAll("(?is)</?b>", "").replaceAll("(?is)[A-Z]/[A-Z]", "");
			m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessed);
			
			try {
				partyNamesCOPuebloTR(m, searchId);
				parseLegalCOPuebloTR(m, searchId);
				parseAddressCOPuebloTR(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static void parseAddressCOPuebloTR(ResultMap resultMap, long searchId) {

		String address = (String) resultMap.get("tmpAddress");
			
		if (StringUtils.isEmpty(address))
			return;
		
		String city = "";
		for (int i = 0; i < CITIES.length; i++){
			if (address.toLowerCase().contains(CITIES[i].toLowerCase())){
				city = CITIES[i];
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				address = address.toUpperCase().replaceFirst(city.toUpperCase() + ".*", "");
				address = address.replaceAll("(\\d)-([A-Z])", "$1$2");
				break;
			}
		}
		address = address.replaceAll("\\d{5}\\s*-?\\s*$", "");
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address.trim());
		address = address.replaceAll("\\A0+", "");
	
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address.trim()));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address.trim()).replaceAll("\\A0+", ""));
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesCOPuebloTR(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner.replaceAll("\\s*</dd>\\s*", " & ").trim());
		
		stringOwner = stringOwner.replaceAll("\\s*\r\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\s*%\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\b\\d+/\\d+\\b", "");
		stringOwner = stringOwner.replaceAll("\\b/\\b", " & ");
		//stringOwner = stringOwner.replaceAll("[\\d%]+", "");
		stringOwner = stringOwner.replaceAll("\\bINT\\b", "");
		stringOwner = stringOwner.replaceAll("\\bAS( TRUSTEE) OF THE\\b", "$1");
		stringOwner = stringOwner.replaceAll("\\bTHE UND\\b", "");
		//stringOwner = stringOwner.replaceAll("\\bTR\\b", "");  //4726103020 B5110
		stringOwner = stringOwner.replaceAll("(?is)\\s+Heirs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*&\\s*", " & ");
		stringOwner = stringOwner.replaceAll("(?is)\\bMrs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s+\\+\\s+", " & ");
		
		if (stringOwner.trim().matches("(?is)\\A(\\w+)\\s+([A-Z]+\\s+[A-Z]+\\s+([A-Z]))$") && NameUtils.isNotCompany(stringOwner)){
			stringOwner = stringOwner.replaceAll("(?is)\\A(\\w+)\\s+([A-Z]+\\s+[A-Z]+\\s+([A-Z]))$", "$1###$2");
		}
			
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		String[] owners = stringOwner.split("\\s*</dd>\\s*");
		for (int i = 0; i < owners.length; i++) {
			names = StringFormats.parseNameNashville(owners[i], true);
			
			names[2] = names[2].replaceAll("(?is),", "");
			names[2] = names[2].replaceAll("(?is)###", " ");
			if (StringUtils.isNotEmpty(names[5]) && 
					LastNameUtils.isNotLastName(names[5]) && 
					NameUtils.isNotCompany(names[5])){
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
			}
			
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractAllNamesSufixes(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		/*
		 * co owner special parsing removed
		 */
		
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
	}
	
	public static void parseLegalCOPuebloTR(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
				
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
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
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
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
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(UNITS?(?:\\s+NO)?)\\s*([A-Z]?[\\d-]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(ABST)\\s+([\\d+\\s&]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), ma.group(2).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(1), "");
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,-]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legal = legal.replaceAll(ma.group(0), "");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(2).replaceAll("\\s*,\\s*", " ").trim());
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
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		String subdiv = "";

		p = Pattern.compile("(?is)\\bTR\\s+(.*?)($|S\\s*#.*|[\\d\\.]+.*)");
		ma = p.matcher(legal);
		if (ma.find()){
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(?is)\\bBLK\\s+(.*?$)($|S\\s*#.*|[\\d\\.]+.*)");
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
					subdiv = subdiv.replaceAll("(?:LOT|ABST|UNIT(\\s+NO)?),?", "");
					subdiv = subdiv.replaceAll("[\\d\\.]{3,}", "");
				}
			}
		}
			
		subdiv = subdiv.replaceAll(",", "");
		subdiv = subdiv.replaceAll("(.+)\\s+(SEC|PH|LOTS?).*", "$1");
		subdiv = subdiv.replaceAll("(.+)\\s+SUB.*", "$1");
		subdiv = subdiv.replaceAll("(.+)\\s+SUB.*", "$1");
		m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
		legal = legal.replaceAll("", "");
		
	}
	
}

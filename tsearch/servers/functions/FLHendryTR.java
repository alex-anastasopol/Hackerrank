package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.tags.WhateverTag;
import ro.cst.tsearch.utils.tags.WhateverTag.FontTag;

	/**
	* @author mihaib
	*/

public class FLHendryTR {

	public static final String ACCOUNT_NUMBER_REG_EX = "(?is)[A-Z]?\\d+-[A-Z]?\\d+";

	public static void parseAndFillIntermediaryResultMap(ResultMap resultMap, String rowFromIntermediaryResponse, long searchId) {
		
		String rawAccountNumber = RegExUtils.getFirstMatch("(?is)<a.*>(.*)</A>", rowFromIntermediaryResponse, 1).trim();

		String accountNumber = RegExUtils.getFirstMatch(ACCOUNT_NUMBER_REG_EX, rawAccountNumber, 0).trim();
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), accountNumber);
		
		if (accountNumber.trim().matches("(?is)(\\d+)\\s*-.*")) {
			resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "REAL ESTATE");
		}
		
		try {
			rowFromIntermediaryResponse = rowFromIntermediaryResponse.replaceAll("(?is)</?i[^>]*>", "")
																	.replaceAll("(?is)&nbsp;", " ").trim();
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rowFromIntermediaryResponse, null);
			
			WhateverTag addTags = new WhateverTag();
			htmlParser.setNodeFactory(addTags.addNodeFactory());
			
			NodeList mainTableList = htmlParser.parse(null);
			String geoNumber = "";
			NodeList fontList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("FONT"), true);
			if (fontList != null){
				for (int j = 0; j < fontList.size(); j++){
					FontTag font = (FontTag) fontList.elementAt(j);
					String ere = font.getStringText();
					if (ere.toLowerCase().contains("geo number")){
						if (fontList.size() > j){
							font = (FontTag) fontList.elementAt(j + 1);
							if (font != null){
								geoNumber = font.getStringText();
							}
						}
					}
				}
			}
			if (geoNumber.length() > 0){
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), geoNumber.trim());
			}
			NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			
			if (tableList != null){
				if (tableList.size() == 3 && tableList.elementAt(0).toHtml().toLowerCase().contains("search results")){
					tableList.remove(0);
				}
				if (tableList.size() > 1){
					TableTag mainTable = (TableTag)tableList.elementAt(0);
					TableRow[] rows = mainTable.getRows();
					if (rows.length > 0){
						TableColumn[] cols = rows[0].getColumns();
						if (cols.length > 0){
							String ownerName = cols[0].toPlainTextString().trim();
							resultMap.put("tmpOwner", ownerName);
						}
					}
				}
				if (tableList.size() > 1){
					TableTag mainTable = (TableTag)tableList.elementAt(1);
					TableRow[] rows = mainTable.getRows();
					if (rows.length > 0){
						TableColumn[] cols = rows[0].getColumns();
						if (cols.length > 0){
							String legal = cols[0].toPlainTextString().trim();
							
							Pattern pat = Pattern.compile("(?i)\\b(DR|RUN|AVE|TER|BLVD|ST|PL|LN|CT|CIR|RD|[C|S]R\\s+\\d+|PLZ)\\b");
							Matcher mat = pat.matcher(legal);
							String siteAddress = "";
							if (mat.find()) {
								int idx = legal.indexOf(mat.group(1));
								if (idx != -1) {
									siteAddress = legal.substring(0, idx + 1 + (mat.group(1)).length()).trim();
									legal = legal.substring(idx + 1 + (mat.group(1)).length()).trim();
								}
							}

							String streetName = StringFormats.StreetName(siteAddress);
							String streetNo = StringFormats.StreetNo(siteAddress);
							
							resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
							resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
							
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			partyNamesFLHendryTR(resultMap, searchId);
			parseLegalFLHendryTR(resultMap, searchId);
		} catch (Exception e) {
			e.printStackTrace();;
		}
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap m, long searchId) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", "").replaceAll("(?is)&amp;", "&")
								.replaceAll("(?is)<th\\b", "<td").replaceAll("(?is)</th\\b", "</td")
								.replaceAll("(?is)</?b>", "").replaceAll("(?is)</?font[^>]*>", "");
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null); 

			String siteAddressAndGEO = "";
			siteAddressAndGEO = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(mainList, "Property Address"), "", true).trim();
			
			String address = "", city = "";
			String siteAddress = siteAddressAndGEO.replaceAll("(?is)\\bProperty Address(.*)GEO Number\\s+\\(PIN\\).*", "$1");
			siteAddress = siteAddress.replaceAll("(?is)<br>", " ").trim();
			Pattern pat = Pattern.compile("(?i)\\b(DR|RUN|AVE|TER|BLVD|ST|PL|LN|CT|CIR|RD|[C|S]R\\s+\\d+|PLZ)\\b");
			Matcher mat = pat.matcher(siteAddress);
			if (mat.find()) {
				int idx = siteAddress.indexOf(mat.group(1));
				if (idx != -1) {
					int lastIndex = idx + (mat.group(1)).length();
					address = siteAddress.substring(0, lastIndex).trim();
					if (siteAddress.length() > lastIndex){
						city = siteAddress.substring(lastIndex).trim();
					}
				}
			}
			
			if (StringUtils.isNotEmpty(address)) {
				m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
				m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
			}
			
			if (StringUtils.isNotEmpty(city)) {
				m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
			}
			
			String geoNumber = siteAddressAndGEO.replaceAll("(?is)\\bProperty Address.*GEO Number\\s+\\(PIN\\)(.*)", "$1");
			geoNumber = geoNumber.replaceAll("(?is)<br>", " ").trim();
			if (StringUtils.isNotEmpty(geoNumber)) {
				m.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), geoNumber);
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), geoNumber);
			}
			
			String acctNumber = "";
			acctNumber = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Account Number"), "", true).trim();
			
			if (StringUtils.isNotEmpty(acctNumber)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), acctNumber.trim());
			}
			
			String taxYear = "";
			taxYear = HtmlParser3.getValueFromAbsoluteCell(1, 2, HtmlParser3.findNode(mainList, "Account Number"), "", true).trim();
			if (StringUtils.isNotEmpty(taxYear)) {
				m.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear.trim());
			}
			
			String propType = "";
			propType = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "Account Number"), "", true).trim();
			
			if (StringUtils.isNotEmpty(propType)) {
				m.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), propType.trim());
			}
			
			String owners = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(mainList, "Mailing Address"), "", true).trim();
			owners = owners.replaceAll("(?is)Mailing Address", "").replaceAll("(?is)\\A\\s*<br>", " ").trim();
			m.put("tmpOwner", owners);
			
			
			String legal = "";
			legal = ro.cst.tsearch.utils.StringUtils.extractParameter(detailsHtml.replaceAll("(?is)<br>", " "), "(?s)Legal Description(.*?</TD>\\s*</TR>\\s*</TABLE>).*?</TD>");
			legal = legal.replaceAll("(?is)</?t[rd][^>]*>", "").replaceAll("(?is)</?table[^>]*>", "").replaceAll("(?is)[\n\t]+", " ").trim();
			if (StringUtils.isNotEmpty(legal)) {
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal.trim());
			}
			
			String baseAmount = "0.00";
			baseAmount = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Taxes & Assessments"), "", true).trim();
			if (StringUtils.isNotEmpty(baseAmount)) {
				baseAmount = baseAmount.replaceAll("[\\$,]", "").trim();
				m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount.trim());
			}
			
			String priorYears = "0.00";
			priorYears = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Prior Years Due"), "", true).trim();
			if (StringUtils.isNotEmpty(priorYears)) {
				priorYears = priorYears.replaceAll("[\\$,]", "").trim();
			}
			
			String totalDue = "";
			NodeList tables53percent = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
											   .extractAllNodesThatMatch(new HasAttributeFilter("width", "53%"));
			TableTag totalDueTable;
			if (tables53percent.size()>=2)
			{
				totalDueTable = (TableTag)tables53percent.elementAt(1);
				if (totalDueTable!=null)
				{
					TableRow[] rows = totalDueTable.getRows();
					for (int i=1; i<rows.length; i++)
						if (rows[i].getColumns()[1].toHtml().toLowerCase().contains("bgcolor=\"silver\""))
						{
							totalDue = rows[i].getColumns()[1].toPlainTextString().replaceAll("[\\$,]", "").trim();
							break;
						}
				}
			}
			
			String currentYearUnpaid = "0.0";
			boolean done = false;
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), false);
			for(int i = 0; i < tables.size() && !done; i++) {
				TableTag table = (TableTag) tables.elementAt(i);
				if (table.toPlainTextString().indexOf("Prior Year Taxes Due") > -1 &&
						table.toPlainTextString().indexOf("NO DELINQUENT TAXES") < 0){
					TableTag table2 = (TableTag) table.getChildren()
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true).elementAt(0);
					for(int j = 0; j < table2.getRowCount(); j++) {
						if(table2.getRows()[j].getColumns()[0].toPlainTextString().trim().equals(taxYear)) {
							currentYearUnpaid = table2.getRows()[j].getColumns()[5].toPlainTextString().replaceAll("[,$]|(?:&nbsp;)", "").trim();
							done = true;
							break;
						}
					}
				}
			}	
			
			m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), GenericFunctions1.sum(priorYears + "+-" + currentYearUnpaid, searchId));
			if (StringUtils.isEmpty(totalDue))
				totalDue = currentYearUnpaid;
			m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
			
			String amountPaid = "0.00";
			amountPaid = HtmlParser3.getValueFromAbsoluteCell(1, 3, HtmlParser3.findNode(mainList, "Transaction"), "", true).trim();
			if (StringUtils.isNotEmpty(amountPaid)) {
				amountPaid = amountPaid.replaceAll("[\\$,]", "").trim();
				m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
			}
			
			List<List> bodyTaxes = new ArrayList<List>();
			List<String> line = null;

			NodeList paymentTablesList = mainList.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "1"), true);
			if (paymentTablesList != null){
				for (int i = 0; i < paymentTablesList.size(); i++ ){
					if (paymentTablesList.elementAt(i).toHtml().contains("Date Paid")) {
						List<List<String>> taxTable = HtmlParser3.getTableAsList(paymentTablesList.elementAt(i).toHtml(), false);
						if (taxTable.size() > 0){
							List lst = taxTable.get(0);
							line = new ArrayList<String>();
							if (lst.size() > 4){
								line.add(lst.get(0).toString().trim());
								line.add(lst.get(2).toString().trim());
								line.add(lst.get(3).toString().trim());
								line.add(lst.get(4).toString().replaceAll("[\\$,]", "").trim());
								bodyTaxes.add(line);
							}
						}
					}
				}	
		
				if (bodyTaxes != null){
					if (!bodyTaxes.isEmpty()){
						ResultTable rt = new ResultTable();
						String[] header = {"ReceiptDate", "ReceiptNumber", "Year", "ReceiptAmount"};
						rt = GenericFunctions2.createResultTable(bodyTaxes, header);
						m.put("TaxHistorySet", rt);
					}
				}
			}
							
			try {
				partyNamesFLHendryTR(m, searchId);
				parseLegalFLHendryTR(m, searchId);

			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	@SuppressWarnings({ "rawtypes" })
	public static void partyNamesFLHendryTR(ResultMap m, long searchId) throws Exception {
		
		String owner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(owner))
			return;		
		
		owner = owner.replaceAll("(?is)\n\n\t\n", "<br>");
		owner = owner.replaceAll("(?is)\\+", "&");
		String[] ownerRows = owner.split("(?is)<br>");
		String stringOwner = "";
		for (String row : ownerRows){
			if (row.trim().matches("\\d+\\s+.*")){
				break;
			} else if (row.trim().toLowerCase().contains("box")){
				break;
			} else if (row.trim().toLowerCase().startsWith("pmb")){
				break;
			}else {
				stringOwner += row.trim() + "<br>";
			}
		}
		stringOwner = stringOwner.replaceAll("(?is)\\bWI?FE?\\b", " ");
		stringOwner = stringOwner.replaceAll("(?is)\\bMRS\\b", "&");
		stringOwner = stringOwner.replaceAll("(?is)[\\(\\)]+", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bOR\\b", "&");
		stringOwner = stringOwner.replaceAll("(?is)\\bET\\s*AL\\b", "ETAL");
		stringOwner = stringOwner.replaceAll("(?is)\\bET\\s*UX\\b", "ETUX");
		stringOwner = stringOwner.replaceAll("(?is)\\bET\\s*VIR\\b", "ETVIR");
		stringOwner = stringOwner.replaceAll("(?is)\\bSUITE\\s+\\d+", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bAS\\s+(TRUSTEES?)\\b", "$1");
		stringOwner = stringOwner.replaceAll("(?is)\\bL\\s*/\\s*E", "LE");
		stringOwner = stringOwner.replaceAll("&#39;", "'");	
		stringOwner = stringOwner.replaceAll("%", "<br>");
		stringOwner = stringOwner.replaceAll("(?is)<br>\\s*$", "");
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;

		//boolean coOwner = false;
		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*$", "");
		stringOwner = stringOwner.replaceAll("(?is)\\b(ET)\\s+(AL|UX|VIR)\\b", "$1$2");
		stringOwner = stringOwner.replaceAll("(?is)(&(?:\\s*[A-Z]+)?)\\s*<br>\\s*([A-Z]+(?:\\s+[A-Z])?)\\s*$", "$1 $2");
		stringOwner = stringOwner.replaceAll("(?is)(&\\s*[A-Z]+)\\s*<br>\\s*([A-Z]+)\\s*&\\s*", " $1 $2 <br>");
		stringOwner = stringOwner.replaceAll("(?is)&\\s*([A-Z]+)\\s*<br>\\s*([A-Z]+\\s+TRUSTEES?)\\s*$", "& $1 $2");
		String[] owners = stringOwner.split("<br>");
		boolean has2Owners = false;
		for (int i = 0; i < owners.length; i++) {
			owners[i] = owners[i].replaceAll("\\A\\s*&", "").replaceAll("\\A\\s*C/O", "").replaceAll("\\A\\s*ATT:", "")
									.replaceAll("\\A\\s*DBA/", "");
			names = StringFormats.parseNameNashville(owners[i], true);
			if (i == 0 && StringUtils.isNotEmpty(names[5].trim())){
				has2Owners = true;
			}
			
			if (i > 0){
				names = StringFormats.parseNameDesotoRO(owners[i], true);
				if (has2Owners && LastNameUtils.isNotLastName(names[2])){
					names = StringFormats.parseNameNashville(owners[i], true);
				}
			}

			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
											NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		stringOwner = stringOwner.replaceAll("(?is)\\s*<br>\\s*", " & ");
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner);
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseLegalFLHendryTR(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
				
		if (StringUtils.isEmpty(legal))
			return;
		
		if (legal.contains("click for full description")){
			legal = legal.replaceAll("(?is)\\(click for full description\\)", "");			
			Pattern pat = Pattern.compile("(?i)\\b(DR|RUN|AVE|TER|BLVD|ST|PL|LN|CT|CIR|RD|[C|S]R\\s+\\d+|PLZ)\\b");
			Matcher mat = pat.matcher(legal);
			if (mat.find()) {
				int idx = legal.indexOf(mat.group(1));
				if (idx != -1) {
					legal = legal.substring(idx + (mat.group(1)).length()).trim();
				}
			}
		}
		legal = legal.replaceAll("(?is)\\s+THRU\\s+", "-");
		legal = legal.replaceAll("&#39;", "'");	
		legal = legal.replaceAll("(?is)&quot;", "\"");
		legal = legal.replaceAll("(?is)\\\"[^\\\"]+\\\"", "");
		legal = legal.replaceAll("(?is)(\\d+)\\s*AND\\s*(\\d+)", "$1 & $2");
		legal = legal.replaceAll("(?is)\\+", "&");
		legal = legal.replaceAll("(?is)\\b[NSEW]{1,2}\\s*[\\d/]+\\s*(?:F\\s*T)?\\s+(OF|[\\d\\.]+\\s+AC(RES)?)\\b", " ");
		
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		String legalTemp = legal;
					
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\s*([\\d,\\s&]+[A-Z]?)\\b");
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
		p = Pattern.compile("(?is)\\b(BLKS?)\\s*([\\d]+[A-Z]?|[A-Z])\\b");
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
		
		
		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UN?I?T)\\s*([\\d]+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), "UNIT ");
		}
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);// ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		// extract unit from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s+([\\d]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);// ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

	
		// extract section from legal description
		p = Pattern.compile("(?is)\\bTR(?:ACT)?\\s+(\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(1));
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG)\\s+([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		//extract Section
		p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(2));
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		//extract plat book and page
		String pb = "";
		String pg = "";
		p = Pattern.compile("\\b(PB)\\s*(\\d+)\\s*[,|/]?\\s*(PG?S?)?\\s*([\\d-&\\s]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pb = pb + " " + ma.group(2);
			pg = pg + " " + ma.group(4);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pb.trim());
			m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pg.trim());
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\s+(\\d+)\\s*/\\s*(\\d+(?:\\s*&\\s*\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String book = ma.group(1).trim();
			String page = ma.group(2).trim();
			if (page.contains("&")){
				String[] pages = page.split("&");
				for (String eachPage : pages){
					List<String> line = new ArrayList<String>();
					line.add(book);
					line.add(eachPage.trim());
					line.add("");
					bodyCR.add(line);
				}
			} else {
				List<String> line = new ArrayList<String>();
				line.add(book);
				line.add(page);
				line.add("");
				bodyCR.add(line);
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), "");
		}
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\bOR\\s*(\\d+)\\s*PG\\s*(\\d+(?:\\s*[&|-]\\s*\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String book = ma.group(1).trim();
			String page = ma.group(2).trim();
			if (page.contains("&")){
				String[] pages = page.split("&");
				for (String eachPage : pages){
					List<String> line = new ArrayList<String>();
					line.add(book);
					line.add(eachPage.trim());
					line.add("");
					bodyCR.add(line);
				}
			} else {
				List<String> line = new ArrayList<String>();
				line.add(book);
				line.add(page);
				line.add("");
				bodyCR.add(line);
			}
			legalTemp = legalTemp.replaceFirst(ma.group(0), "");
		}
		legal = legalTemp;
		
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;	
		
		//extract subdivision name
		String subdiv = "";
		p = Pattern.compile("(?is)\\b(.*)(?:PHASE|UNIT|BLK).*");
		ma = p.matcher(legal.trim());
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		if (subdiv.length() != 0) {

			subdiv = subdiv.replaceAll("(?is)\\s+OR\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\bS/D\\b", "");
			subdiv = subdiv.replaceAll("(?is)\\s+DESC\\b.*", "");
			subdiv = subdiv.replaceAll("(?is)\\s+UNIT\\s*$", "");
			subdiv = subdiv.replaceAll("(?is)\\A\\s*[\\d-]+", "");

			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv.trim());
		}
		
		String parcelID = (String) m.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
		
		if (StringUtils.isNotEmpty(parcelID)){
			parcelID = parcelID.replaceAll("-", "").trim();
			if (parcelID.length() == 18){
				String sec = parcelID.substring(5, 7);
				m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec.trim().replaceAll("\\A0+", ""));
				String twn = parcelID.substring(3, 5);
				m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), twn.trim().replaceAll("\\A0+", ""));
				String rng = parcelID.substring(1, 3);
				m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), rng.trim().replaceAll("\\A0+", ""));
			}
		}
	}
}

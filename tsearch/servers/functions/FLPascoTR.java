package ro.cst.tsearch.servers.functions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.Text;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * parsing functions for FLPascoTR.
 * 
 * @author mihaib
 */

public class FLPascoTR {

	public static void parseAndFillResultMap(String detailsHtml, ResultMap resultMap, long searchId, int miServerID) {

		detailsHtml = StringEscapeUtils.unescapeHtml(detailsHtml);
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

		try {

			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			NodeList mainList = htmlParser3.getNodeList();

			NodeList parcelList = mainList.extractAllNodesThatMatch(new RegexFilter("Parcel Id"), true);
			String pid = "";
			if (parcelList.size() > 0) {
				pid = HtmlParser3.getFirstParentTag(parcelList.elementAt(0), Div.class).getLastChild().toPlainTextString().trim();
			}
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid);
			resultMap.put("tmpParcelID", pid);

			NodeList ownerList = mainList.extractAllNodesThatMatch(new RegexFilter("Owner of Record"), true);
			String ownerName = "";
			if (ownerList.size() > 0) {
				ownerName = HtmlParser3.getFirstParentTag(ownerList.elementAt(0), Div.class).getChildrenHTML()
						.replaceAll("(?is)\\s*<span[^>]*>.*?</span>\\s*", "");
				if (ownerName.replaceAll("(?is)(.*?)\\s*<br\\s*/>.*", "$1").endsWith("&")) {
					ownerName = ownerName.replaceAll("(?is)(.*?\\s*<br\\s*/>.*?)<br\\s*/>.*", "$1").replaceFirst("\\s*<br\\s*/>\\s*", " ");
				}
				else {
					ownerName = ownerName.replaceAll("(?is)(.*?)\\s*<br\\s*/>.*", "$1");
				}
			}

			NodeList siteAddressList = mainList.extractAllNodesThatMatch(new RegexFilter("Physical Address"), true);
			String siteAddress = "";
			if (siteAddressList.size() > 0) {
				siteAddress = HtmlParser3.getFirstParentTag(siteAddressList.elementAt(0), Div.class).getChildrenHTML()
						.replaceAll("(?is)\\s*<span[^>]*>.*?</span>\\s*", "");
			}

			resultMap.put("tmpOwnerName", ownerName);

			if (StringUtils.isNotEmpty(siteAddress)) {
				resultMap.put("tmpAddress", siteAddress);
				if (!siteAddress.contains("NOT ON FILE")) {
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(siteAddress));
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(siteAddress));
				}
			}

			NodeList legalList = mainList.extractAllNodesThatMatch(new RegexFilter("Legal Description"), true);
			String legal = "";
			if (legalList.size() > 0) {
				legal = HtmlParser3.getFirstParentTag(legalList.elementAt(0), Div.class).getChildrenHTML()
						.replaceAll("(?is)\\s*<span[^>]*>.*?</span>\\s*", "").replaceAll("\\s*<br\\s*/?>\\s*", " ");
			}

			resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);

			NodeList taxYearList = mainList.extractAllNodesThatMatch(new RegexFilter("Tax Year"), true);
			String year = "";
			if (taxYearList.size() > 0) {
				year = taxYearList.elementAt(0).toPlainTextString().trim().replaceFirst("(?s)(\\d+)\\b.*", "$1");
			}
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
			resultMap.put("tmpCrtTaxYear", year);

			String baseAmount = "";
			baseAmount = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "MILLAGE"), "", true).replaceAll("[\\$,]+", "").trim();
			resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);

			String totalAssess = "";
			totalAssess = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "ASSESSED VALUE"), "", true).replaceAll("[\\$,]+", "").trim();
			resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAssess);

			String rcptNos = "", rcptDates = "", amtPaids = "", rcptYears = "";

			Node payTable = HtmlParser3.getNodeByTypeAttributeDescription(mainList, "table", "class", "paytable",
					new String[] { "TAX YEAR", "RECEIPT", "AMOUNT PAID", "MARCH AMOUNT", "DATE" },
					true);
			Text textToFind = HtmlParser3.findNode(mainList, "RECEIPT");

			// amount paid
			String amountPaid = "";
			TableRow amountPaidRow = ((TableTag) payTable).getRow(1);
			if (amountPaidRow.getColumns()[0].toPlainTextString().trim().equals(resultMap.get(TaxHistorySetKey.YEAR.getKeyName().toString()))) {
				amountPaid = amountPaidRow.getColumns()[2].toPlainTextString().replaceAll("[\\$,]+", "");
			}
			resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);

			if (payTable != null && payTable instanceof TableTag && textToFind != null) {
				TableRow[] rows = ((TableTag) payTable).getRows();

				// receipts
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
					String rcptNo = row.getColumns()[1].toPlainTextString().trim();

					String rcptYear = row.getColumns()[0].toPlainTextString().trim();
					if (StringUtils.isNotEmpty(rcptYear) && rcptYear.trim().matches("\\d+")) {
						if (!(rcptYear.equals(year) && rcptNo.isEmpty())) {
							rcptYears += rcptYear + "@@";
						}

					}

					if (StringUtils.isNotEmpty(rcptNo)) {
						rcptNos += rcptNo + "@@";

						String rcptDate = row.getColumns()[4].toPlainTextString().trim();
						
						if (StringUtils.isNotEmpty(rcptDate) && rcptDate.matches("\\d+/\\d+/\\d+")) {
							rcptDates += rcptDate + "@@";
						}

						String amtPaid = row.getColumns()[2].toPlainTextString().trim();
						
						if (StringUtils.isNotEmpty(amtPaid)) {
							amtPaids += amtPaid.replaceAll("[\\$,]+", "") + "+";
						}

					}
				}

				resultMap.put("tmpRcptYears", rcptYears);
				resultMap.put("tmpRcptNos", rcptNos);
				resultMap.put("tmpRcptDates", rcptDates);
				resultMap.put("tmpAmountPaid", amtPaids);
			}
			else {
				Node taxNode = HtmlParser3.getNodeByID("paymentHistory", mainList, true);
				
				if (taxNode != null && taxNode instanceof TableRow) {
					
					String taxTable = taxNode.getFirstChild().getFirstChild().toHtml();
					
					if (StringUtils.isNotEmpty(taxTable)) {
						taxTable = taxTable.replaceAll("(?i)<tr[^>]*>\\s*<td[^>]*>[^<]*</td>\\s*</tr>", "").replaceAll("[\\$,]+", "");
						List<HashMap<String, String>> tableAsListMap = HtmlParser3.getTableAsListMap(taxTable);

						String[] header = { "Year", "ReceiptNumber", "ReceiptAmount", "ReceiptDate" };
						Map<String, String> headerToTableMap = new HashMap<String, String>();
						headerToTableMap.put("ReceiptDate", "DATE");
						headerToTableMap.put("ReceiptNumber", "RECEIPT");
						headerToTableMap.put("ReceiptAmount", "AMOUNT PAID");
						headerToTableMap.put("Year", "TAX YEAR");

						ResultBodyUtils.buildInfSet(resultMap, tableAsListMap, header, headerToTableMap, TaxHistorySet.class);
					}
				}
			}

			Date dateNow = new Date();
			DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);

			// amount due nodes
			NodeList amountDueNodes = mainList.extractAllNodesThatMatch(new RegexFilter("IF POSTMARKED BY"), true);
			
			int noOfAmountDueNodes = amountDueNodes.size();
			if (noOfAmountDueNodes > 0) {
				String amountDue = "";

				for (int i = 0; i < noOfAmountDueNodes; i++) {
					Node amountDueNode = amountDueNodes.elementAt(i);
					String amountDueDate = amountDueNode.toPlainTextString().replaceAll("(?s).*?(\\d{2}/\\d{2}/\\d{2})\\s*$", "$1");

					if (dateNow.before(df.parse(amountDueDate))) {
						amountDue = amountDueNodes.elementAt(i).getParent().getParent().getLastChild().toPlainTextString().replaceAll("[\\$,]+", "");
						break;
					}
				}
				resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
			}
			// delinquent

			Node delinquentTaxesNode = htmlParser3.getNodeById("delinquentTaxes");
			if (delinquentTaxesNode != null) {
				NodeList delinquentDatesNode = delinquentTaxesNode.getChildren().extractAllNodesThatMatch(new RegexFilter("IF RECEIVED BY"), true);
				
				String firstDate = "";
				String secondDate = "";
				int delinquentAmountColumn = -1;
				
				if (delinquentDatesNode.size() > 0) {

					Node datesNode = HtmlParser3.getFirstParentTag(delinquentDatesNode.elementAt(0), TableTag.class).getLastChild();

					if (datesNode instanceof TableRow) {
						TableColumn[] dateColumn = ((TableRow) datesNode).getColumns();

						if (dateColumn.length > 0) {
							firstDate = dateColumn[0].toPlainTextString().trim();
						}

						if (dateColumn.length > 1) {
							secondDate = dateColumn[1].toPlainTextString().trim();
						}
						
						Date date1 = df.parse(firstDate);
						Date date2 = df.parse(secondDate);
						
						if (dateNow.before(date1) || dateNow.equals(date1)) {
							delinquentAmountColumn = 4;
						}
						else if (dateNow.before(date2) || dateNow.equals(date2)) {
							delinquentAmountColumn = 5;
						}
					}
				}
				
				//delq table
				Node delinquentTable =HtmlParser3.getNodeByTypeAttributeDescription(delinquentTaxesNode.getChildren(), "table", "", "",
						new String[] { "TAX YEAR", "ORIGINAL GROSS TAX", "CERTIFICATE NUMBER", "BUYER NUMBER"},
						true);
				double delinquentAmount = 0.0;
				if (delinquentTable != null && delinquentTable instanceof TableTag) {
					TableRow[] rows = ((TableTag) delinquentTable).getRows();
					
					for (int i = 1; i < rows.length; i++) {
						int noOfColumns = rows[i].getColumnCount();
						
						if (delinquentAmountColumn > 0 && noOfColumns >= 6) {
							TableColumn delinquentColumn = rows[i].getColumns()[delinquentAmountColumn];
							delinquentAmount += Double.parseDouble(delinquentColumn.toPlainTextString().replaceAll("[\\$,]+", ""));
						}
					}
				}
				
				resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), String.valueOf(delinquentAmount));
			}
			
			partyNamesFLPascoTR(resultMap, searchId);
			taxFLPascoTR(resultMap, searchId);
			legalFLPascoTR(resultMap, searchId);

			resultMap.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	

	public static void partyNamesFLPascoTR(ResultMap m, long searchId) throws Exception {
		String ownerName = (String) m.get("tmpOwnerName");
		String address = (String) m.get("tmpAddress");
		 if(StringUtils.isNotEmpty(address)) {
			 //String[] split = address.split("\\b\\w+\\b");
			 List<Integer> stringContainsPositions = StringUtils.stringContainsComponentsFromOtherString(ownerName, address);				 
			 List<String> splitString = StringUtils.splitString(address);
			 if (stringContainsPositions.size()>=2){
				 boolean found = false;
				 for (String string : splitString) {
					 int indexOf = ownerName.indexOf(string);
					 if (indexOf >-1 && !found){
						 ownerName = ownerName.substring(0,indexOf);
						 found = true;
					 }
				}
			 }else{
				   Pattern p=Pattern.compile("((?=(INC|PO)).*|\\d.*)");
				   Matcher ma = p.matcher(ownerName);
				   if (ma.find()){
					   ownerName = ownerName.replaceAll("(?is)((INC|PO).*|\\d.*)", "");
				   }
			 }
		 }
		 if (StringUtils.isEmpty(ownerName))
			 return;
	      
		 partyNamesTokenizerTR(m, ownerName);  
}
	public static void legalFLPascoTR(ResultMap m, long searchId) throws Exception {

		String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)</?br\\s*/>", " ");
		// initial corrections and cleanup of legal description
		legal = legal.replaceAll("\\b([A-Z]+[-])(\\s*)([A-Z]+)\\b", "$1$3");
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		legal = legal.replaceAll("\\s+AND\\s+", "&");
		legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		legal = legal.replaceAll("\\bCO-OP\\b", "");
		legal = legal.replaceAll("\\s+\\d+\\s*(ST|ND|RD|TH)\\s*(ADD(N)?|WAY)", "");
		// legal = legal.replaceAll("\\b[NWSE][\\d\\./\\s]+(\\s*OF)?\\b", "");
		legal = legal.replaceAll("UNREC\\s*PLAT\\s*(OF)?", "");
		legal = legal.replaceAll("\\b[\\d]+\\s*OF\\b", "");
		// legal = legal.replaceAll("\\b(\\d+(ST|ND|RD|TH) )?REPLAT( OF)?\\b", "");
		// legal = legal.replaceAll("\\b(REVISED|AMENDED) PLAT( OF)?\\b", "");
		legal = legal.replaceAll("( N | S | W | E | NW | NE | SW | SE )\\d+([\\.|/]\\d+)?\\s+(FT\\s+)?(MOL\\s+)?(OF)", "");
		legal = legal.replaceAll("(\\b[NEWS]{1,2})\\s*\\d*\\b([\\.|/]\\d*)?\\s+(FT\\s+)?(MOL\\s+)?(OF)?", ""); //ex. 27-23-21-0000-00700-0023
		legal = legal.replaceAll(";", " ");
		legal = legal.replace(",", "");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		String legalTemp = legal;

		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("\\b(LOT|L)S?\\s*([\\d&\\|\\s,-]+|[\\d]+)\\b");
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
		p = Pattern.compile("\\b(BLOCK|BLK)S?\\s*([\\d]+|[A-Z]+|[\\dA-Z]+)\\b");
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
		p = Pattern.compile("\\b(UNIT)S?\\s*([\\d-&/A-Z]+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("\\b(BLDG)\\s*([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("\\b(PHASE)S?\\s*([\\dA-Z]+|[\\dA-Z&\\s]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		String parcel = "";
		p = Pattern.compile("\\b(PARCEL)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			parcel = parcel + " " + ma.group(2);
			parcel = parcel.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionParcel", parcel);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		String sec = "";
		p = Pattern.compile("\\b(SEC)T?I?O?N?\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			sec = sec + " " + ma.group(2);
			sec = sec.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionSection", sec);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		String tract = "";
		p = Pattern.compile("\\b((?:TR)A?C?T?S?\\s*)([\\d&\\s]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(2);
			tract = tract.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		String pb = "";
		String pg = "";
		p = Pattern.compile("\\b(PB|CB|RB|MB|B)\\s*0*(\\d+)\\s*(?:-)?\\s*(P)G?S?\\s*0*(\\d+)([-&\\s]+\\d+)?\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			pb = pb + " " + ma.group(2);
			pb = pb.trim();
			pg = pg + " " + ma.group(4);
			pg = pg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.PlatBook", pb);
			m.put("PropertyIdentificationSet.PlatNo", pg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b((?:O|P)R)\\s*0*(\\d+)\\s*(PG)S?\\s*0*([\\d-&]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(4));
			line.add("");
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}

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

		// legal = legal.replaceAll("\\b\\s+TR\\s+\\b","");
		legal = legal.replaceAll("BLK ", "");

		String subdiv = "";
		p = Pattern.compile("(?:\\s*)?(.*)(\\n)(UNIT|PHASE|BLOCK|PARCEL|TR|BLK|LOT|UNREC|TRACT|OR|L|PB|UNIT |MB|CB|RB)\\b*");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("\\b(?:\\s*)?(.+?)(?:SEC|PHASE|PARCEL|LOT|TR|BLOCK|TRACT|TR|PB|MB|CB|RB|LT|BLK|PH |\\s+OR\\s+|UNIT |BLDG)\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		if (subdiv.length() != 0) {

			subdiv = subdiv.replaceAll("\\bNO\\s+\\d+\\s*$", "");
			subdiv = subdiv.replaceFirst("(.*)(\\d)(ST|ND|RD|TH)\\s*(ADDN)\\b", "$1" + "$2");
			subdiv = subdiv.replaceFirst("(.*)(PARCEL|PHASE)\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*)(ADD)\\b", "$1");
			subdiv = subdiv.replaceFirst("(.*)(UNREC.*)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+(A)?\\s+CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceFirst("(?is)\\A\\s*TRS?\\b", "");
			subdiv = subdiv.replaceFirst("(.*)(\\s+-)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(\\d+)\\s*$", "$1");
			subdiv = subdiv.replaceFirst("\\bCOM\\s+AT\\b", "");
			subdiv = subdiv.replaceFirst("\\bCOR\\s+OF\\b", "");
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv.trim());
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv.trim());
		}

		String parcelId = (String) m.get("PropertyIdentificationSet.ParcelID");
		if (StringUtils.isEmpty(parcelId))
			return;

		String section = "";
		String township = "";
		String range = "";
		p = Pattern.compile("(?is)([A-Z\\d]+)(\\s+)([A-Z\\d]+)(\\s+)([A-Z\\d]+).*");
		Matcher mp = p.matcher(parcelId);
		if (mp.find()) {
			section = mp.group(1);
			township = mp.group(3);
			range = mp.group(5);
			m.put("PropertyIdentificationSet.SubdivisionTownship", township);
			m.put("PropertyIdentificationSet.SubdivisionRange", range);
			if (StringUtils.isEmpty(sec))
				m.put("PropertyIdentificationSet.SubdivisionSection", section);
		}
	}

	public static void taxFLPascoTR(ResultMap m, long searchId) throws Exception {

		String[] receiptYear = ((String) m.get("tmpRcptYears")).split("@@");
		String[] receiptNumber = ((String) m.get("tmpRcptNos")).split("@@");
		String[] receiptDate = ((String) m.get("tmpRcptDates")).split("@@");
		String[] receiptAmountPaid = ((String) m.get("tmpAmountPaid")).split("\\+");

		if (!StringUtils.isEmpty(receiptDate) && !StringUtils.isEmpty(receiptNumber)) {
			List<String> line = null;
			List<List> bodyRT = new ArrayList<List>();
			for (int i = 0; i < receiptNumber.length; i++) {
				line = new ArrayList<String>();
				line.add(receiptYear[i]);
				line.add(receiptNumber[i]);
				line.add(receiptAmountPaid[i]);
				line.add(receiptDate[i]);
				bodyRT.add(line);
			}

			if (!bodyRT.isEmpty()) {

				ResultTable newRT = new ResultTable();
				String[] header = { "Year", "ReceiptNumber", "ReceiptAmount", "ReceiptDate" };
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("ReceiptNumber", new String[] {"ReceiptNumber", "" });
				map.put("ReceiptAmount", new String[] {"ReceiptAmount", "" });
				map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
				newRT.setHead(header);
				newRT.setMap(map);
				newRT.setBody(bodyRT);
				newRT.setReadOnly();
				m.put("TaxHistorySet", newRT);
			}
		}
	}

	protected static String cleanOwnerFLPascoTR(String s) {
		s = s.toUpperCase();
		s = s.replaceFirst("\\b\\s*\\(H&W\\)", "");
		s = s.replaceFirst("\\b\\s*\\(F/D\\)", "");
		s = s.replace("%", "&");
		s = s.replaceAll("\\bMRS\\b", "");
		s = s.replaceAll("\\bDECEASED\\b", "");
		s = s.replaceAll("\\s{2,}", " ").trim();
		return s;
	}

	public static void partyNamesTokenizerTR(ResultMap m, String s) throws Exception {

		s = s.replaceAll("(.+)\\s+(\\d)(.+)", "$1");
		s = s.replaceAll("(.+)\\s+(PO\\s*BOX)(.+)", "$1");
		s = cleanOwnerFLPascoTR(s);
		s = s.replaceAll("(.+) MEADOW SWEET", "$1");
		s = s.replaceAll("(JOSEPHINE)\\s+(JONES MICHAEL WALTER)", "$1 & $2");
		s = s.replaceAll("& (ASSOCIATES)", "AND $1"); // pid = 072616037D000003820
		s = s.replaceAll("(FRANCES L)\\s*(DAY)", "$1 & $2");
		s = s.replaceAll("C/O", "&");
		s = s.replaceAll("(?ism)</?\\s*BR\\s*/?>", "");
		if ((s.contains(" TR ")) || (s.contains("TRUST"))) {
			s = s.replaceAll("&", "AND");
			s = s.replaceAll(" TR ", " TR & ");
			s = s.replaceAll("TRUST ", "TRUST & ");
			s = s.replaceAll("(TRUSTEES?)\\s*", "$1 & ");
			s = s.replaceAll("(TTEES)\\s*", "$1 & ");
		}
		s = s.replaceFirst("\\s*&\\s*$", "");
		String[] owners;
		owners = s.split("&");

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, types, otherTypes;
		String ln = "";

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];

			names = StringFormats.parseNameNashville(ow, true);
			if (!NameUtils.isCompany(names[2])) {
				if ((names[0].length() == 1 && names[1].length() == 0 && !LastNameUtils.isLastName(names[2]))
						|| (names[0].length() == 0 && names[1].length() == 0 && names[2].length() == 0)
						|| ((names[0].length() < 1) && !LastNameUtils.isLastName(names[2]))
						|| ((FirstNameUtils.isFirstName(names[0]) && names[1].length() == 0 && FirstNameUtils.isFirstName(names[2])) && owners.length > 1)) {
					names[1] = names[0];
					names[0] = names[2];
					names[2] = ln;
				}
				if ((names[2].length() == 1 && names[1].length() == 0)
						|| (names[2].length() == 0 && names[1].length() == 0)) {
					names[1] = names[2];
					// names[0] = names[2];
					names[2] = ln;
				}
				//HANKS JOHN W DECEASED
				//C/O JILL MADDEN
				if (names[1].length()==0 && LastNameUtils.isLastName(names[0]) && !FirstNameUtils.isFirstName(names[0]) &&
						FirstNameUtils.isFirstName(names[2]) && !LastNameUtils.isLastName(names[2])) {
					String aux = names[0];
					names[0] = names[2];
					names[2] = aux;
				}
			}
			ln = names[2];
			names[2] = names[2].replaceAll("AND (ASSOCIATES)", "& $1"); // pid = 072616037D000003820
			names[2] = names[2].replaceAll(" AND ", " & ");
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
											NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
	}
	
	public static void stdFLPascoTR(ResultMap m, String s) throws Exception {

	    s= s.replaceFirst("(?is)(.+)(&)(.+)(&)(.+)", "$1" + "$2" + "$3");
	    s = cleanOwnerFLPascoTR(s);
	    String[] lines = s.split("&");
	    
	    if (lines.length < 1)
			   return;

		   String[] a = StringFormats.parseNameNashville(cleanOwnerFLPascoTR(lines[0]), true);
		  
		   if ((a[5].length() == 0) && (lines.length >=2)){
			   String coowner = cleanOwnerFLPascoTR(lines[1]);
			   //String coowner; 
			   //coowner = a[2].concat(" ").concat(tempCoowner);
			   coowner = coowner.replaceAll("\\d+.*", "");
			   coowner = coowner.replaceFirst("^&\\s*", "");
			   String[] b = StringFormats.parseNameNashville(coowner, true); 
			   if (StringUtils.isEmpty(b[1])){
				   b[1] = b[0];
				   b[0] = b[2];
				   b[2] = a[2];
			   }
				   
			   if (b[0].contains(a[2]) || a[2].contains(b[0])) 
				   b = StringFormats.parseNameDesotoRO(coowner, true);
			   
		       b[1] = b[1].replaceFirst("^([A-Z]) [A-Z]{2,} [A-Z]{2,}( [A-Z])?.+", "$1"); 
			   a[3] = b[0];
			   a[4] = b[1];
			   a[5] = b[2];
		   }
		  
	    if (a[3].length() > a[5].length()) {
	    	String aux = a[3];
	    	a[3] = a[5];
	    	a[5] =aux;
	    }
		   
	    m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
	    m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
	    m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
	    m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
	    m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
	    m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	    
	    List<List> body = new ArrayList<List>();
	    String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
	    types = GenericFunctions.extractAllNamesType(a);
	    otherTypes = GenericFunctions.extractAllNamesOtherType(a);
	    suffixes = GenericFunctions.extractAllNamesSufixes(a);
	    
	    GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], types, otherTypes, 
	    		NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
		
	    GenericFunctions.storeOwnerInPartyNames(m, body, true);

	}
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId, TableColumn addressColumn) {
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		try {
			TableColumn[] cols = row.getColumns();
			if (cols.length == 2) {
				String parcelID = cols[0].toPlainTextString();

				String nameOnServer = "";
				String fullAddress = "";
				if (addressColumn != null) {// search by address intermediaries
					fullAddress = addressColumn.toPlainTextString();
					nameOnServer = cols[1].toPlainTextString();
				}
				else {
					fullAddress = cols[1].getChild(1).toPlainTextString();
					nameOnServer = cols[1].getFirstChild().toPlainTextString();
				}
				String[] address = StringFormats.parseAddress(fullAddress.trim());

				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID.trim());
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer.trim());
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), address[0]);
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), address[1]);
				stdFLPascoTR(resultMap, nameOnServer);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultMap;
	}

}

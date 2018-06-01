package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
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
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * parsing functions for ILMcHenryTR, ILKendallTR.
 * 
 * @author mihaib
 */

public class ILMcHenryTR {

	/**
	 * Parse base amount 
	 * @param parser
	 * @return
	 */
	private static String getBA(final HtmlParser3 parser) { 
		TableTag table = (TableTag) parser.getNodeById("ctl00_contentBody_gvTaxRates");
		if (table == null)
			return "";
		for (TableRow row : table.getRows()) {
			if (row.toPlainTextString().contains("Total")) {
				if (row.getColumnCount() < 3)
					break;
				return row.getColumns()[2].toPlainTextString().replaceAll("[$,]+", "");
			}
		}
		return "";
	}
	
	/**
	 * Parse legal description.
	 * @param legal
	 * @return
	 */
	private static String getLegal(String legal) {
		if (legal == null)
			return null;
		legal = legal.replaceAll("(?is)<b>\\s*Legal\\s*Description\\s*</b>", "");
		legal = legal.replaceAll("(?is)Legal\\s*Description\\s*(.*)", "$1");
		legal = legal.replaceAll("DOC\\.?\\s*\\w+(-\\w+)?", ""); // Bug 8145
		return legal.replace("<br>", "\n").replace('\n', ' ').trim();
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", "&");
		detailsHtml = detailsHtml.replaceAll("(?is)<span class\\s*=\\s*\\\"\\s*[^\\\"]+\\\"\\s*>([^<]+)</span>", "$1").replaceAll("(?is)</?legend>", "")
									.replaceAll("(?is)</?fieldset>", "").replaceAll("(?is)</?p>", "").replaceAll("(?is)(<br>){3,}", "")
									.replaceAll("(?is)<th", "<td").replaceAll("(?is)</th>", "</td>").replaceAll("\n", "");
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
		try {		
			HtmlParser3 parser = new HtmlParser3(detailsHtml);
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String taxYear = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Tax Year:"), "", true).trim();
			m.put(TaxHistorySetKey.YEAR.getKeyName(), StringUtils.isNotEmpty(taxYear) ? taxYear : "");
			
			String pid = parser.getNodePlainTextById("ctl00_lblParcelNumber").trim();
			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), StringUtils.isNotEmpty(pid) ? pid : "");
			m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), StringUtils.isNotEmpty(pid.replaceAll("-", "")) ? pid : "");

			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), getBA(parser));
			
			String ownerNameTable = parser.getNodeById("ctl00_contentBody_gvNames").toHtml();
			String ownerName = "";
			List<List<String>> ownerNameList = HtmlParser3.getTableAsList(ownerNameTable, false);
				for (List<String> name : ownerNameList){
					if (name.get(1).contains("OWNER")){
						ownerName += name.get(0) + "@@@";
					}
				}
			ownerName = ownerName.replaceAll("(?is)@@@$", "");
			m.put("tmpOwnerName", StringUtils.isNotEmpty(ownerName) ? ownerName : "");
			
			String siteAddress = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Sales"), "", true).trim();
			if (StringUtils.isNotEmpty(siteAddress)){
				siteAddress = siteAddress.replaceAll("(?is)<b>\\s*Site\\s*Address\\s*</b>", "");
				m.put("tmpAddress", siteAddress);
			}
			
			String legal = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "Sales"), "", true).trim();
			m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), getLegal(legal));
			
			String tmpFirstInstallmentTaxBilled = "";
			String tmpSecondInstallmentTaxBilled = "";
			boolean hasTaxBilled = false;
			String tmpFirstInstallmentTotalBilled = "";
			String tmpSecondInstallmentTotalBilled = "";
			boolean hasTotalBilled = false;
			
			String tmpFirstInstallmentAmountPaid = "";
			String tmpSecondInstallmentAmountPaid = "";
			String tmpFirstInstallmentAmountDue = "";
			String tmpSecondInstallmentAmountDue = "";
			String tmpFirstInstallmentReceipt = "";
			String tmpSecondInstallmentReceipt = "";
			String tmpFirstInstallmentDueDate = "";
			String tmpSecondInstallmentDueDate = "";
			String tmpFirstInstallmentDatePaid = "";
			String tmpSecondInstallmentDatePaid = "";
			NodeList tableList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_contentBody_gvPayments"));
			for (int i=0;i<tableList.size();i++) {
				TableTag table = (TableTag)tableList.elementAt(i);
				if (table.getRowCount()>1) {
					TableRow yearRow = table.getRow(1);
					if (yearRow.getColumnCount()>0) {
						String tableYear = yearRow.getColumns()[0].toPlainTextString();
						if (tableYear.equals(taxYear)) {
							for (int j=2;j<table.getRowCount();j++) {
								TableRow row = table.getRow(j);
								if (row.getColumnCount()==4) {
									String label = row.getColumns()[0].toPlainTextString().trim();
									if ("Tax Billed".equals(label)) {
										tmpFirstInstallmentTaxBilled = row.getColumns()[1].toPlainTextString().trim().replaceAll("[$,]+", "");
										tmpSecondInstallmentTaxBilled = row.getColumns()[2].toPlainTextString().trim().replaceAll("[$,]+", "");
										hasTaxBilled = true;
									} else if ("Total Billed".equals(label)) {
										tmpFirstInstallmentTotalBilled = row.getColumns()[1].toPlainTextString().trim().replaceAll("[$,]+", "");
										tmpSecondInstallmentTotalBilled = row.getColumns()[2].toPlainTextString().trim().replaceAll("[$,]+", "");
										hasTotalBilled = true;
									} else if ("Amount Paid".equals(label)) {
										tmpFirstInstallmentAmountPaid = row.getColumns()[1].toPlainTextString().trim().replaceAll("[$,]+", "");
										tmpSecondInstallmentAmountPaid = row.getColumns()[2].toPlainTextString().trim().replaceAll("[$,]+", "");
									} else if ("Total Unpaid".equals(label)) {
										tmpFirstInstallmentAmountDue = row.getColumns()[1].toPlainTextString().trim().replaceAll("[$,]+", "");
										tmpSecondInstallmentAmountDue = row.getColumns()[2].toPlainTextString().trim().replaceAll("[$,]+", "");
									} else if ("Receipt No.".equals(label)) {
										tmpFirstInstallmentReceipt = row.getColumns()[1].toPlainTextString().trim();
										tmpSecondInstallmentReceipt = row.getColumns()[2].toPlainTextString().trim();
									} else if ("Date Due".equals(label)) {
										tmpFirstInstallmentDueDate = row.getColumns()[1].toPlainTextString().trim();
										tmpSecondInstallmentDueDate = row.getColumns()[2].toPlainTextString().trim();
									} else if ("Date Paid".equals(label)) {
										tmpFirstInstallmentDatePaid = row.getColumns()[1].toPlainTextString().trim();
										tmpSecondInstallmentDatePaid = row.getColumns()[2].toPlainTextString().trim();
									}
								}
							}
						}
						break;
					}
				}		
			}
			
			String tmpFirstInstallmentBaseAmount = "";
			String tmpSecondInstallmentBaseAmount = "";
			if (hasTaxBilled) {
				tmpFirstInstallmentBaseAmount = tmpFirstInstallmentTaxBilled;
				tmpSecondInstallmentBaseAmount = tmpSecondInstallmentTaxBilled;
			} else if (hasTotalBilled) {
				tmpFirstInstallmentBaseAmount = tmpFirstInstallmentTotalBilled;
				tmpSecondInstallmentBaseAmount = tmpSecondInstallmentTotalBilled;
			}
			
			m.put("tmpFirstInstallmentBaseAmount", tmpFirstInstallmentBaseAmount);
			m.put("tmpSecondInstallmentBaseAmount", tmpSecondInstallmentBaseAmount);
			
			m.put("tmpFirstInstallmentAmountPaid", tmpFirstInstallmentAmountPaid);
			m.put("tmpSecondInstallmentAmountPaid", tmpSecondInstallmentAmountPaid);
			
			m.put("tmpFirstInstallmentAmountDue", tmpFirstInstallmentAmountDue);
			m.put("tmpSecondInstallmentAmountDue", tmpSecondInstallmentAmountDue);
			
			m.put("tmpFirstInstallmentReceipt", tmpFirstInstallmentReceipt);
			m.put("tmpSecondInstallmentReceipt", tmpSecondInstallmentReceipt);
			
			m.put("tmpFirstInstallmentDueDate", tmpFirstInstallmentDueDate);
			m.put("tmpSecondInstallmentDueDate", tmpSecondInstallmentDueDate);
			
			m.put("tmpFirstInstallmentDatePaid", tmpFirstInstallmentDatePaid);
			m.put("tmpSecondInstallmentDatePaid", tmpSecondInstallmentDatePaid);
			
			List<List> bodyHist1 = new ArrayList<List>();
			List<List> bodyHist2 = new ArrayList<List>();
			List<String> lineHist1 = new ArrayList<String>();
			List<String> lineHist2 = new ArrayList<String>();

			NodeList taxTablesList = HtmlParser3.getNodesByID("ctl00_contentBody_gvPayments", mainList, true);
			for (int i = 0; i < taxTablesList.size(); i++){
				String table = taxTablesList.elementAt(i).toHtml();
				List<List<String>> taxesPerYearBulk = HtmlParser3.getTableAsList(table, false);
				List<List<String>> taxesPerYear = new ArrayList<List<String>>();
				for (int j = 0; j < taxesPerYearBulk.size(); j++){
					if (!taxesPerYearBulk.get(j).get(0).contains("Amount Paid")){
						if (!taxesPerYearBulk.get(j).get(0).contains("Receipt")){
							if (!taxesPerYearBulk.get(j).get(0).contains("Date Paid")){
								// atunci fluiera
							} else {
								taxesPerYear.add(taxesPerYearBulk.get(j));
							}
						} else {
							taxesPerYear.add(taxesPerYearBulk.get(j));
						}
					} else { 
						taxesPerYear.add(taxesPerYearBulk.get(j));
					}
				}
				
				if (taxesPerYear.size() > 2){
					if (!"".equals(taxesPerYear.get(2).get(1).toString().trim())){
						lineHist1 = new ArrayList<String>();
						lineHist1.add(taxesPerYear.get(2).get(1).toString());
						lineHist1.add(taxesPerYear.get(1).get(1).toString());
						lineHist1.add(taxesPerYear.get(0).get(1).toString().replaceAll("(?is)[\\$,]+", ""));
						bodyHist1.add(lineHist1);
					}
					
					if (!"".equals(taxesPerYear.get(2).get(2).toString().trim())){
						lineHist2 = new ArrayList<String>();
						lineHist2.add(taxesPerYear.get(2).get(2).toString());
						lineHist2.add(taxesPerYear.get(1).get(2).toString());
						lineHist2.add(taxesPerYear.get(0).get(2).toString().replaceAll("(?is)[\\$,]+", ""));
						bodyHist2.add(lineHist2);
					}
				}
			}
			
			if (!bodyHist1.isEmpty()){
				ResultTable rt = new ResultTable();
				String[] header = {"ReceiptDate", "ReceiptNumber", "ReceiptAmount"};
				rt = GenericFunctions2.createResultTable(bodyHist1, header);
				m.put("tmpHist1stInstall", rt);
			}
			
			if (!bodyHist2.isEmpty()){
				ResultTable rt = new ResultTable();
				String[] header = {"ReceiptDate", "ReceiptNumber", "ReceiptAmount"};
				rt = GenericFunctions2.createResultTable(bodyHist2, header);
				m.put("tmpHist2ndInstall", rt);
			}
									
			try {
				partyNamesILMcHenryTR(m, searchId);
				legalILMcHenryTR(m, searchId);
				parseAddressILMcHenryTR(m, searchId);
				taxILMcHenryTR(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	

	@SuppressWarnings("rawtypes")
	public static void taxILMcHenryTR(ResultMap m, long searchId) throws Exception {
		
		String firstInstallmentBaseAmount = (String) m.get("tmpFirstInstallmentBaseAmount");
		String secondInstallmentBaseAmount = (String) m.get("tmpSecondInstallmentBaseAmount");
		String firstInstallmentPaid = (String) m.get("tmpFirstInstallmentAmountPaid");
		String secondInstallmentPaid = (String) m.get("tmpSecondInstallmentAmountPaid");
		String firstInstallmentDue = (String) m.get("tmpFirstInstallmentAmountDue");
		String secondInstallmentDue = (String) m.get("tmpSecondInstallmentAmountDue");
		String firstReceiptNo = (String) m.get("tmpFirstInstallmentReceipt");
		String secondReceiptNo = (String) m.get("tmpSecondInstallmentReceipt");
		
		String firstInstallmentPaidDate = (String) m.get("tmpFirstInstallmentDatePaid");
		String secondInstallmentPaidDate = (String) m.get("tmpSecondInstallmentDatePaid");
		String firstInstallmentDueDate = (String) m.get("tmpFirstInstallmentDueDate");
		//String secondInstallmentDueDate = (String) m.get("tmpSecondInstallmentDueDate");
		
		Date dateNow = new Date();
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
		
		if (StringUtils.isNotEmpty(firstInstallmentPaid) && StringUtils.isNotEmpty(secondInstallmentPaid)) {
			BigDecimal amountPaid = new BigDecimal(firstInstallmentPaid).add(new BigDecimal(secondInstallmentPaid));
			m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid.toString());
		}
		
		if (!StringUtils.isEmpty(firstInstallmentDueDate.trim())) {
			if (dateNow.before(df.parse(firstInstallmentDueDate))) {
				if(StringUtils.isEmpty(firstInstallmentPaidDate.trim())) {
					m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), firstInstallmentDue);
				}
			} else if (/*dateNow.before(df.parse(secondInstallmentDueDate)) &&*/ StringUtils.isEmpty(secondInstallmentPaidDate.trim())){
					String s = firstInstallmentDue + "+" + secondInstallmentDue;
					m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), GenericFunctions1.sum(s, searchId));
			}
		}
		
		List<List> bodyHist = new ArrayList<List>();
		List<String> lineHist = new ArrayList<String>();
		if (!StringUtils.isEmpty(secondInstallmentPaidDate.trim())) {
			lineHist.add(secondInstallmentPaidDate);
			lineHist.add(secondReceiptNo);
			lineHist.add(secondInstallmentPaid);
			bodyHist.add(lineHist);
		}
			   
		lineHist = new ArrayList<String>();
		if (!StringUtils.isEmpty(firstInstallmentPaidDate.trim())) {
			lineHist.add(firstInstallmentPaidDate);
			lineHist.add(firstReceiptNo);
			lineHist.add(firstInstallmentPaid);	
			bodyHist.add(lineHist);
		}
		
		ResultTable tmpHist1stInstall = (ResultTable) m.get("tmpHist1stInstall");
		ResultTable tmpHist2ndInstall = (ResultTable) m.get("tmpHist2ndInstall");
		
		if (tmpHist2ndInstall != null) {
			String body2[][] = tmpHist2ndInstall.getBodyRef();
			if (body2.length != 0) {
				for (int i = 0; i < body2.length; i++) {
					  lineHist = new ArrayList<String>();
					  for (int j = 0; j < tmpHist2ndInstall.getHead().length; j++) {
						  if (!StringUtils.isEmpty(body2[i][j])){
							  lineHist.add(body2[i][j]);
						  } else {
							  lineHist.add("");
						  }
					  }
					  bodyHist.add(lineHist);
					  if (tmpHist1stInstall != null) {
						  String body1[][] = tmpHist1stInstall.getBodyRef();
						  if (body1.length != 0) {
								  lineHist = new ArrayList<String>();
								  for (int k = 0; k < tmpHist1stInstall.getHead().length; k++){
									  if (!StringUtils.isEmpty(body1[i][k])){
										  lineHist.add(body1[i][k]);
									  } else {
										  lineHist.add("");
									  }
							  }
						  }
						  bodyHist.add(lineHist);
					  }
				  }
			}
		}
		if (!bodyHist.isEmpty()) {
			String [] header = {"ReceiptDate", "ReceiptNumber", "ReceiptAmount"};				   
		    Map<String,String[]> map = new HashMap<String,String[]>();
		    map.put("ReceiptDate", new String[]{"ReceiptDate", ""});
		    map.put("ReceiptNumber", new String[]{"ReceiptNumber", ""});
		    map.put("ReceiptAmount", new String[]{"ReceiptAmount", ""});
		    
		    ResultTable rtable = new ResultTable();	
		    rtable.setHead(header);
		    rtable.setBody(bodyHist);
		    rtable.setMap(map);
		    m.put("TaxHistorySet", rtable);
		}
		
		List<String> line = new ArrayList<String>();
		List<List> bodyInstallments = new ArrayList<List>();
		line = new ArrayList<String>();
		line.add("Installment1");
		line.add(firstInstallmentBaseAmount);
		line.add(firstInstallmentPaid);
		line.add(firstInstallmentDue);
		if (Double.parseDouble(firstInstallmentDue)>0.0) {
			line.add("UNPAID");
		} else {
			line.add("PAID");
		}
		bodyInstallments.add(line);
		line = new ArrayList<String>();
		line.add("Installment2");
		line.add(secondInstallmentBaseAmount);
		line.add(secondInstallmentPaid);
		line.add(secondInstallmentDue);
		if (Double.parseDouble(secondInstallmentDue)>0.0) {
			line.add("UNPAID");
		} else {
			line.add("PAID");
		}
		bodyInstallments.add(line);
		
		String [] header = {TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(), TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
				TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(), TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
				TaxInstallmentSetKey.STATUS.getShortKeyName()};				   
		Map<String,String[]> mapInstallments = new HashMap<String,String[]>();
		mapInstallments.put(TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(), new String[]{TaxInstallmentSetKey.INSTALLMENT_NAME.getShortKeyName(), ""});
		mapInstallments.put(TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(), new String[]{TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(), ""});
		mapInstallments.put(TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(), new String[]{TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(), ""});
		mapInstallments.put(TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(), new String[]{TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(), ""});
		mapInstallments.put(TaxInstallmentSetKey.STATUS.getShortKeyName(), new String[]{TaxInstallmentSetKey.STATUS.getShortKeyName(), ""});
		
		ResultTable installmentsRT = new ResultTable();	
		installmentsRT.setHead(header);
		installmentsRT.setBody(bodyInstallments);
		installmentsRT.setMap(mapInstallments);
		m.put("TaxInstallmentSet", installmentsRT);

	}
	
	private static String sanitizeLegal(String legal) {
		legal = legal.replaceAll("UNIT|PHASE|BLOCK|BLK|LOT", "");
		legal = legal.replace("&nbsp;", " ").replaceAll("\\s+", " ");
		legal = legal.replaceFirst("(?is)\\bCITY\\s+OF\\s+.+", "");
		legal=legal.trim();
		return legal;
	}
	
	private static boolean validSubdivison(String legal) {
		if (Pattern.compile("^[\\w\\s]+$").matcher(legal).find() == false)
			return false;
		return true;
	}
	
	public static void legalILMcHenryTR(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		if (StringUtils.isEmpty(legal))
			return;
		
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
		
		// initial corrections and cleanup of legal description
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		legal = legal.replaceAll("(?is)(;)amp;", "$1");//&amp;amp; 
		legal = legal.replaceAll("(?is)&amp;", "&");
		
		legal = legal.replaceAll("(\\d+)\\s+&\\s+(\\d+)", "$1&$2");
		legal = legal.replaceAll("([A-Z])\\s+&\\s+(\\d+)", "$1&$2");
		legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		legal = legal.replaceAll("\\bNO\\s*\\.?\\s*(\\d+)\\b", "$1");
		legal = legal.replaceAll("#", "");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		String legalTemp = legal;

		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LTS?|LOTS?)\\s*([\\d\\s&,-]+|[A-Z])\\b");
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
		p = Pattern.compile("(?is)\\b(BLK|BLOCK)S?\\s*(\\d+)\\b");
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
		p = Pattern.compile("(?is)\\b(UNIT)\\s*([\\dA-Z-&]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PHASE)S?\\s*(\\d+[A-Z]*|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		//String sec = "";
		p = Pattern.compile("(?is)\\bSECTION\\s*(\\d+)\\s*,?\\s*TOWNSHIP\\s*(\\d+)\\s*,?\\s*RANGE\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(1));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2));
			m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(3));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\bSEC\\.?\\s*(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
				m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(1));
				m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2));
				m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(3));
				legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				legal = legalTemp;		
			} else {
				ma.reset();
				p = Pattern.compile("(?is)\\b(SEC(?:TION)?)S?\\s*([\\d&]+)\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(2));				
				}
			} 
		}

		// extract cross refs from legal description
		List<List<String>> bodyCR = new ArrayList<List<String>>();
		p = Pattern.compile("(?is)\\b([\\dR?\\d]{6,})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1));
			if (!bodyCR.contains(line)) {
				bodyCR.add(line);
			}
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

		String subdiv = "";
		if ("kendall".equals(crtCounty.toLowerCase())){
			p = Pattern.compile("(?i)\\bLO?TS?\\s+(.+?)\\s+(SUB.*|UNIT|PHASE|ADDN)");
			ma = p.matcher(legal);
			Pattern p2 = Pattern.compile("(?is)\\bUNIT\\b(.+?)((\\bUNIT\\b)|(&\\s*%))");
			Matcher ma2 = p2.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
				ma.reset();
				p = Pattern.compile("(?is)\\bLO?TS?\\s+(.*)");
				ma = p.matcher(subdiv);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
				
				subdiv = subdiv.replaceFirst("(.*)(\\d)(ST|ND|RD|TH)\\s*$", "$1");
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
				if (legal.matches(".*\\b(CONDO(MINIUM)?S?)\\b.*")){
					m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
				}
			} else if (ma2.find()) {
				subdiv = ma2.group(1).trim();
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
				if (legal.matches(".*\\b(CONDO(MINIUM)?S?)\\b.*")){
					m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
				}
			} else {
				legal = sanitizeLegal(legal);
				if (validSubdivison(legal))
					m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), legal);
			}
		} else {
			p = Pattern.compile("(?:\\s*)?(.*)(\\n)(UNIT|PHASE|BLOCK|BLK|LOT)\\b*");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				//				\\b(?:\\s*)?(.+?)(?:SEC|PHASE|LOT|BLOCK|EST|ESTATES?|SUB|RESUB|UNIT)\\b
				p = Pattern.compile("(?:SEC|PHASE|LO?T|BLOCK|\\:wEST|ESTATES?|SUB\\.?|RESUB|UNIT)");
				ma.usePattern(p);
				ma.reset();
				if (ma.find()) {
					subdiv = legal.replace(ma.group(0), " ").trim();
				}
			}
			
			if(subdiv.contains("<br>")){
				String[] parts;
				if((parts=legal.split("(?ism)<br>")).length >= 3){
					subdiv = parts[2];
					if(parts.length > 3 && parts[3].contains("COND")){
						subdiv += " "+parts[3];
					} 
				} 
			}
			
			
			if (StringUtils.isNotEmpty(subdiv)) {
				subdiv = subdiv.replaceAll("\\s+", " ")
						.replaceAll("\\bPT\\b.*", "")
						//.replaceFirst("(.*)(\\d)(ST\\.?|ND\\.?|RD\\.?|TH\\.?)\\s*(ADDN)?", "$1" + "$2")
						.replaceFirst("(.*?)\\s*(PARCEL|PHASE|LOT|UNIT).*", "$1")
						.replaceFirst("(.*)(ADDN?)", "$1")
						.replaceFirst("(.*)(FT.*)", "$1")
						.replaceFirst("(.+)\\s+(A)?\\s+CONDO(MINIUM)?.*", "$1")
						.replaceFirst("(.+) SUB\\.?(DIVISION)?.*", "$1")
						.replaceFirst("(.*)\\s+(ALSO.*)", "$1")
						.replaceFirst("UNIT\\s*(?:IN)?\\s+(.*)", "$1")
						.replaceFirst("(.+)\\s+(E|W|N|A)\\b\\d+/\\d+.*", "$1")
						.trim();

				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), sanitizeLegal(subdiv));
				// if (legal.matches(".*\\b(CONDO(MINIUM)?S?)\\b.*"))
				// m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesILMcHenryTR(ResultMap m, long searchId) throws Exception {
		
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
		
		String ownerName = (String) m.get("tmpOwnerName");
		String legal = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		//sometimes, in legal we have the names typed more readable(e.g. MEMO: ), instead of the one from the owner field, or we have ET AL owner who isn't in the owner field
		String nameFromLegal = "";
		if (!StringUtils.isEmpty(legal)){
			legal = legal.replaceAll("(?is)Legal\\s*Description\\s*(.*)", "$1");
			nameFromLegal = legal.replaceAll("(?is).*?((?:memo|et\\s*al)\\s*:.*)\\s*\\)?", "$1");
		}
		
		if (!NameUtils.isCompany(ownerName)) {
			ownerName = ownerName.replaceAll("(?is)\\bDATED.*?\\d{4}\\b", "");
		}
		
		ownerName = ownerName.replaceAll("(?is)\\*", "");
		ownerName = ownerName.replaceAll("(?is)\\s*&amp;amp;\\s*", " & ");//&amp;amp; 
		ownerName = ownerName.replaceAll("(?is)&amp;", " & ");
				
		boolean twoOwners = false;
		if (ownerName.matches("([^&]+)\\s*&\\s*(\\w+)(.*)")){ //PLATTA, N R & MINKS K 2 owners
			if (org.apache.commons.lang.StringUtils.countMatches(ownerName, "&") == 2){
				//ALLEN, MICHAEL R & STEPHEN R & BEVERLY J
				ownerName = ownerName.replaceAll("(?is)\\A\\s*([A-Z]+\\s*,)\\s*([A-Z]+\\s+[A-Z])\\s*&\\s*([A-Z]+\\s+[A-Z])\\s*&\\s*([A-Z]+\\s+[A-Z])\\s*$"
												, "$1 $2 & $1 $3 & $1 $4");
			} else{
				if ("kendall".equalsIgnoreCase(crtCounty)){//ALLEN, STEPHEN R & BEVERLY J
					ownerName = ownerName.replaceAll("(?is)\\A\\s*([A-Z]+\\s*,)\\s*([A-Z]+\\s+[A-Z])\\s*&\\s*([A-Z]+\\s+[A-Z])\\s*$"
							, "$1 $2 & $1 $3");
				} else {
					ownerName = ownerName.replaceAll("([^&]+)\\s*&\\s*(\\w+)(.*)", "$1 & $2, $3");
					twoOwners = true;
				}
			}
		}
		ownerName = ownerName.replaceAll("(?is)\\bET\\s*AL\\b", "ETAL");
		ownerName = ownerName.replaceAll("(?is)(\\w+)\\s+,", "$1,");		
		ownerName = ownerName.replaceAll("(?is)(\\s+\\w),(\\w+)", "$1 & $2");

		Pattern pat = Pattern.compile("(?is).*\\s+(\\w+)");
		Matcher mat = pat.matcher(ownerName);

		if (NameUtils.isCompany(ownerName)){
			ownerName = ownerName.replaceAll(",", "");
		}
		if (!ownerName.contains("&")){
			if (mat.find() && !mat.group(1).matches("JR|SR|DR|[IV]{2,}") 
							&& (!mat.group(1).matches(GenericFunctions.nameTypeString) && !mat.group(1).matches(GenericFunctions.nameOtherTypeString))) {
				//2 owners JONES, SAMUEL G MELISSA M; or JONES, DAVID E II ANDREA; or BROWN, DANIEL E JR MEGHAN A
				ownerName = ownerName.replaceAll("(?is)(\\w+),\\s+(\\w+)\\s+(\\w(?:\\s+[IVXSJR]{1,3})?)\\s+(\\w+.*)", "$1, $2 $3 and $4");
			}
		}
		if (ownerName.matches("(\\w+\\s+\\w+(\\s+\\w)?)\\s*,(.*)")) { //HEINZEN BRUCE N, FOOTE DIANE M
			ownerName = ownerName.replaceAll("(\\w+\\s+\\w+(?:\\s+\\w)?)\\s*,(.*)", "$1 & $2");
		}
		if (ownerName.contains("/")){
			ownerName = ownerName.replaceAll("(?is)/", " & ");
		} else {
			//2 owners BROWNE, DANIEL WENDY; or BROWN, DAVID LAURA; or
			if (!ownerName.trim().endsWith("ETAL") && "mc henry".equals(crtCounty.toLowerCase())){
				ownerName = ownerName.replaceAll("(?is)\\A(\\w+),\\s+(\\w+(?:\\s+[A-Z]{1,3})?)\\s+([A-Z]{3,}.*)", "$1, $2 and $3");
			}
		}
		ownerName = ownerName.replaceAll("(?is)\\s+and\\s+and\\s+", " and ");
		if (nameFromLegal.matches("(?is).*ET\\s*AL.*")){
			if (nameFromLegal.contains(")")) {
				nameFromLegal = nameFromLegal.replaceAll("(?is)\\bET\\s*AL\\s*:\\s*([^\\)]+)", "$1");
			} else {
				nameFromLegal = nameFromLegal.replaceAll("(?is)\\bET\\s*AL\\s*:\\s*(.*)", "$1");
			}
			nameFromLegal = nameFromLegal.replaceAll("(?is)\\bINT/?", "");
			nameFromLegal = nameFromLegal.replaceAll("\\bEACH\\b", "");
			nameFromLegal = nameFromLegal.replaceAll("(?is)\\d+/\\d+", " & ");
			nameFromLegal = nameFromLegal.replaceAll("(?is)/", " & ");
			nameFromLegal = nameFromLegal.replaceAll("(?is)<\\s*/?\\s*br>", " ");
			nameFromLegal = nameFromLegal.replaceAll("(?is)\\s*\\)", "");
			nameFromLegal = nameFromLegal.replaceAll("(?is)\\s{2,}", " ");
			//if (nameFromLegal.contains(ownerName.substring(0, ownerName.indexOf(",")))) {
				//ownerName = nameFromLegal;
			//} else {
				ownerName += " & " +nameFromLegal;
			//}
		}
		boolean nameLegal = false;
		if (nameFromLegal.matches("(?is).*MEMO.*")) {
			if (nameFromLegal.contains(")")) {
				nameFromLegal = nameFromLegal.replaceAll("(?is).*?MEMO\\s*:\\s*([^\\)]+)\\).*", "$1");
			} else {
				nameFromLegal = nameFromLegal.replaceAll("(?is).*?MEMO\\s*:\\s*(.*)", "$1");
			}
			nameFromLegal = nameFromLegal.replaceAll("(?is)(\\bINT(EREST)?)?(\\s*(BY|PER)\\s*DOC\\s*[A-Z\\d,]*)?", "");
			nameFromLegal = nameFromLegal.replaceAll("(?is)\\bINT/(\\s*(BY|PER)\\s*DOC\\s*[A-Z\\d,]*)?", "/");
			nameFromLegal = nameFromLegal.replaceAll("\\bEACH\\b", "");
			nameFromLegal = nameFromLegal.replaceAll("\\d+%", "");
			nameFromLegal = nameFromLegal.replaceAll("&amp;", "");
			nameFromLegal = nameFromLegal.replaceAll("(?is)\\d+/\\d+", "");
			nameFromLegal = nameFromLegal.replaceAll("(?is)\\d+-\\d+", "");
			nameFromLegal = nameFromLegal.replaceAll("(?is)\\s+/\\s+", " ");
			nameFromLegal = nameFromLegal.replaceAll("(?is)/", " & ");
			nameFromLegal = nameFromLegal.replaceAll("(?is)-", "");
			nameFromLegal = nameFromLegal.replaceAll("(?is)&\\s+&", " & ");
			nameFromLegal = nameFromLegal.replaceAll("(?is)<\\s*/?\\s*br>", " ");
			nameFromLegal = nameFromLegal.replaceAll("(?is)\\s{2,}", " ");
			nameFromLegal = nameFromLegal.replaceAll("\\s*&\\s*$", "");
			if (nameFromLegal.contains(ownerName.substring(0, ownerName.indexOf(" ")).replaceAll("[,\\s]+", ""))) {
				ownerName = nameFromLegal;
				nameLegal = true;
			}
		}
		
		String[] owners;
		//if (NameUtils.isCompany(ownerName)) {
			//owners = ownerName.replaceAll("(?is)/", " & ").split("@@@");
		//} else {
			owners = ownerName.split("&");;
		//}

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;
		String ln = "";

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i].trim();
			ow = ow.replaceAll("(?is)\\A\\s*,\\s*", "");
			if (owners.length == 1 && NameUtils.isCompany(ow)) { //JOHNSON, HERBERT A & ANNETTE L TRUST 2006-1
				names[2] = ow;
			} else {
				if (!ow.contains(",") && nameLegal && !NameUtils.isCompany(ow)) { //DRIVER, WILLIAM RICHARD JR & JENNIFER MOORE
					ow = ln + " " +ow;
				}
				names = StringFormats.parseNameNashville(ow, true);
			}
			if (NameUtils.isCompany(names[2])) {
				names[2] = names[2].replaceAll("(?is),", "");
				names[2] = names[2].replaceAll("(?is)\\bTR\\b", "TRUST");
			}
			if (!NameUtils.isCompany(names[2]) && ln.length()>1) {
				if (i > 0 && names[0].length() == 1 && names[1].length() == 0 && !twoOwners) {
					names[1] = names[0];
					names[0] = names[2];
					names[2] = ln;
				}
				else if (i > 0 && names[0].length() == 1 && !twoOwners) {
					String names1 = names[1].replaceAll(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameSuffixString, "");
					names1 = names1.replaceAll(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameTypeString, "");
					names1 = names1.replaceAll(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameOtherTypeString, "");
					names1 = names1.trim();
					if (names1.length()==0) {	//JONES, EDWARD J & CORRINE C & EDWARD J JR
						names[1] = names[0] + " " + names[1];
						names[0] = names[2];
						names[2] = ln;
					}
				}
				if (names[0].length() == 0 && names[1].length() == 0) {
					names[0] = names[2];
					names[2] = ln;
				} else if (names[0].length() >= 1 && names[1].length() == 0 && !LastNameUtils.isLastName(names[2])){
					names[1] = names[0];
					names[0] = names[2];
					names[2] = ln;
				}
			}
			ln = names[2];

			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesInterILMcHenryTR(ResultMap m, String ownerName) throws Exception {
		
		String[] suffixes, type, otherType;
		List<List> body = new ArrayList<List>();
		ownerName = ownerName.replaceAll("(?is)\\s*\\*\\s*", " ");
		ownerName = ownerName.replaceAll("(?is)(;)amp;", "$1");//&amp;amp; 
		ownerName = ownerName.replaceAll("(?is)&amp;", "&");
		ownerName = ownerName.replaceAll("\\s*/\\s*", " ");
		//ownerName = ownerName.replaceAll("&", " ");
		ownerName = ownerName.replaceAll("\\s{2,}", "\n");
				
		String[] lines = ownerName.split("\n");
		for (String ow: lines) {
			//ow = ow.replaceAll("&", " ");
			ow = ow.replaceFirst("^\\s*&", "");
			ow = ow.replaceFirst("&\\s*$", "");
			ow = ow.replaceAll("(?ism)\\bET\\s+AL\\b","ETAL");
			ow = ow.trim();
			boolean isNashville = false;
			if (!NameUtils.isCompany(ow)) {
				if (ow.matches("\\w+\\s+\\w+\\s+\\w\\s+\\w+\\s+\\w+\\s+\\w")) {
					ow = ow.replaceAll("^(\\w+)\\s+(\\w+\\s+\\w)\\s+(\\w+)\\s+(\\w+\\s+\\w)$", "$1, $2 & $3, $4");
					isNashville = true;
				} else if (!ow.matches("\\w+\\s+\\w+\\s+\\w+")) {
					String stringType = "";
					Matcher matcher1 = Pattern.compile("(.*)\\s+(" + 
							ro.cst.tsearch.extractor.xml.GenericFunctions1.nameTypeString + ")$").matcher(ow);
					if (matcher1.find()) {
						ow = matcher1.group(1);
						stringType = matcher1.group(2);
					}
					Matcher matcher2 = Pattern.compile("(\\w+(?:\\s+\\w\\s+)?)(.*)\\s+(\\w+)").matcher(ow);
					if (matcher2.find()) {
						if (!matcher2.group(2).trim().matches(".*?\\bET\\s*AL$") && !ow.contains(" ETAL")) {
							String s1 = matcher2.group(1);
							String s2 = matcher2.group(2).replaceFirst("^\\s*&", "");;
							Matcher matcher3 = Pattern.compile("^\\s*" + 
									ro.cst.tsearch.extractor.xml.GenericFunctions1.nameSuffixString +
									"(.*)").matcher(s2);
							if (matcher3.find()) {
								String s2_1 = matcher3.group(1).trim();
								String s2_2 = matcher3.group(2).trim();
								s1 += " " + s2_1;
								s2 = s2_2;
							}
							ow = matcher2.group(3) + ", " + s1 + " & " +
							 	 matcher2.group(3) + ", " + s2;
							isNashville = true;
						} 
					}
					ow += " " + stringType;
				}

				ow = ow.replaceAll("\\s{2,}", " ").trim();
				
				if(ow.contains(" ETAL")){
					ow = ow.replace(" ETAL","");
					//isNashville = true;
				}
			}
			
			if(!isNashville){
				if(ow.contains("&") && ow.matches("\\w+ \\w & \\w+ (\\w+)? \\w+")){
					
				}
			}
			
			String[] a = new String[6];
			if (isNashville) {
				a = StringFormats.parseNameNashville(ow, true);
			} else {
				a = StringFormats.parseNameDesotoRO(ow, true);
				
				if(a[2].length()==1){
					String aux = a[1];
					a[1] = a[2];
					a[2] = aux;
				}
				
				if(a[5].length()==1){
					String aux = a[4];
					a[4] = a[5];
					a[5] = aux;
				}
				
				if(a[2].length()==0 && a[5].length()!=0){
					a[2] = a[5];
				}
			}
			
			suffixes = GenericFunctions.extractNameSuffixes(a);
			type = GenericFunctions.extractNameType(a);
			otherType = GenericFunctions.extractNameOtherType(a);
			GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
		}
		
		//remove repeating names
		List<List> newBody = new ArrayList<List>();
		for (List i: body) {
			boolean found = false;
			for (int j=0;j<newBody.size()&&!found;j++) {
				if (equalLists(i, newBody.get(j))) {
					found = true;
				}
			}
			if (!found) {
				newBody.add(i);
			}
		}
		
		GenericFunctions.storeOwnerInPartyNames(m, newBody, true);
	    
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean equalLists(List l1, List l2) {
		
		if (l1.size()!=l2.size()) {
			return false;
		}
		
		int size = l1.size();
		for (int i=0;i<size;i++) {
			if (!l1.get(i).equals(l2.get(i))) {
				return false;
			}
		}
		
		return true;
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesInterILKendallTR(ResultMap m, String ownerName) throws Exception {
		
		String[] suffixes, type, otherType;
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		ownerName = ownerName.replaceAll("(?is)\\*", "");
		ownerName = ownerName.replaceAll("(?is)(;)amp;", "$1");//&amp;amp; 
		ownerName = ownerName.replaceAll("(?is)&amp;", "&");
		ownerName = ownerName.replaceAll("(?is)\\b(ET)\\s+(AL|UX|VIR)\\b", "$1$2");
		ownerName = ownerName.replaceAll("/", " ");
		
		String[] owners = ownerName.split("(?is)\\s*<br>\\s*");
		for (String eachOwner : owners){
			if (NameUtils.isNotCompany(eachOwner) && eachOwner.contains("&")){
				eachOwner = eachOwner.replaceAll("(?is)(.*?)\\s+(\\w+)\\s*$", "$2 $1");
				eachOwner = eachOwner.replaceAll("(?is)\\b([A-Z])\\s+([A-Z]+,)\\s*", "$1 & $2 ");
				eachOwner = eachOwner.replaceAll("(?is)\\s*&\\s*$", "");
			}
			if (NameUtils.isNotCompany(eachOwner) && !eachOwner.contains("&")){
				names = StringFormats.parseNameDesotoRO(eachOwner, true);
			} else {
				if (NameUtils.isNotCompany(eachOwner) && org.apache.commons.lang.StringUtils.countMatches(eachOwner, "&") == 2){
					//ALLEN MICHAEL R & STEPHEN R & BEVERLY J
					String owner = eachOwner.replaceAll("(?is)\\A\\s*([A-Z]+)\\s+([A-Z]+\\s+[A-Z])\\s*&\\s*([A-Z]+\\s+[A-Z])\\b", "$1 $2 / $1 $3");
					String[] owneri = owner.split("\\s*/\\s*");
					for (String ow : owneri){
						names = StringFormats.parseNameNashville(ow, true);
						
						suffixes = GenericFunctions.extractNameSuffixes(names);
						type = GenericFunctions.extractNameType(names);
						otherType = GenericFunctions.extractNameOtherType(names);
						GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
															NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
					}
				} else{
					names = StringFormats.parseNameNashville(eachOwner, true);
				}
			}
			
		    suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractNameType(names);
			otherType = GenericFunctions.extractNameOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
												NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		
	    GenericFunctions.storeOwnerInPartyNames(m, body, true);	    
	}
	
	public static void parseAddressILMcHenryTR(ResultMap m, long searchId) throws Exception {
		
		String s = (String) m.get("tmpAddress");
		
		if (!StringUtils.isEmpty(s)) {
			s = s.replaceAll("(?is)site\\s*address\\s*(.*)", "$1").replaceAll("<br>", "   ").trim();
			String[] address = s.split("\\s{2,}");
			if (!StringUtils.isEmpty(address[0])) {
				m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address[0]));
				m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address[0]));
			}
			if (address.length > 1) {
				if (!StringUtils.isEmpty(address[1])) {
					address[1] = address[1].replaceAll("(?is)(.*),.*", "$1");
					m.put(PropertyIdentificationSetKey.CITY.getKeyName(), address[1].trim());
				}
			}
		}
	}
}

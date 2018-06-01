package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * 
 * @author Oprina George
 * 
 *         May 17, 2012
 */

public class ILCookTR {
	public static void stdMPisILCookTR(ResultMap m, long searchId) throws Exception {

		String name = (String) m.get("PropertyIdentificationSet.OwnerLastName");
		if (name == null || name.length() == 0)
			return;

		name = cleanOwnerNameILCookTR(name);
		if (!NameUtils.isCompany(name)) {
			name = name.replaceFirst("^([A-Z]+)\\s*&\\s*([A-Z]+)\\s+([A-Z]+)$", "$1 $3 & $2"); // fix for strings as OF & WF OL (PID 32-29-103-014-0000)

			String tokens[] = StringFormats.parseNameDesotoRO(name);

			m.put("PropertyIdentificationSet.OwnerFirstName", tokens[0]);
			m.put("PropertyIdentificationSet.OwnerMiddleName", tokens[1]);
			m.put("PropertyIdentificationSet.OwnerLastName", tokens[2]);
			m.put("PropertyIdentificationSet.SpouseFirstName", tokens[3]);
			m.put("PropertyIdentificationSet.SpouseMiddleName", tokens[4]);
			m.put("PropertyIdentificationSet.SpouseLastName", tokens[5]);
		}
	}

	public static void partyNamesILCookTR(ResultMap m, long searchId) throws Exception {

		String name = (String) m.get("tmpOwnerFullName");
		if (name == null || name.length() == 0)
			return;

		partyNamesTokenizerILCookTR(m, name);
	}

	public static String cleanOwnerNameILCookTR(String s) {
		s = s.replace('/', ' ');
		s = s.replace('.', ' ');
		if (!(s.contains("LLC") || s.contains("TR") || s.contains("PROPERTIE") || s.contains("INC") || s.contains("ETAL")
				|| s.contains("ASSC") || s.contains("CTLTC") || s.contains("TAXPAYER")))
			s = s.replaceAll("\\s*\\d+(?:\\s*[A-Z]|-\\d+)?", " ");

		s = s.replaceAll("PROP.", "PROPERTIE");
		s = s.replaceAll("DR &MRS ", "");
		s = s.replaceAll("MRS\\s*", "");
		s = s.replaceAll("(?is)&amp;", "&");
		s = s.replaceAll("\\s{2,}", " ").trim();
		return s;
	}

	public static void partyNamesTokenizerILCookTR(ResultMap m, String name) throws Exception {

		name = cleanOwnerNameILCookTR(name);
		List<List> body = new ArrayList<List>();
		String[] tokens = { "", "", "", "", "", "" };
		if (name.matches("\\b(\\w+)\\s+\\1\\b") || (name.contains("CTLTC"))) // cazul SMITH SMITH, cazul CTLTC 0001078611, cazul TAXPAYER OF 1846 S KILDARE AVE
		{
			tokens[2] = name;
			GenericFunctions1.addOwnerNames(tokens, "", "", true, false, body);
		}
		else if (!NameUtils.isCompany(name) && !name.matches("[A-Z'-]+ & [A-Z'-]+")) {
			if (name.matches("[A-Z'-]{2,} [A-Z]+( [A-Z])? & [A-Z]{2,}( [A-Z])?")) { // LFM
				tokens = StringFormats.parseNameNashville(name);
			} else { // FML
				name = name.replaceFirst("^([A-Z]{2,}(?: [A-Z])?)\\s*&\\s*([A-Z]{2,}(?: [A-Z])?)\\s+([A-Z]{2,})$", "$1 $3 & $2"); // fix for strings as OF & WF
																																	// OL (PID
																																	// 32-29-103-014-0000)
				// name = name.replaceFirst("([^&])\\s*&\\s*([A-Z])\\s([A-Z][A-Z]+)","$1 $3 & $2"); // fix for strings like OFist_initial & WFirst_initial OL
				// (PIN 17-09-306-032-1088)
				name = name.replaceFirst("([A-Z])\\s\\s*&\\s*([A-Z])\\s\\s*([A-Z]+)", "$1 $3 & $2"); // fix for strings like OFist_initial & WFirst_initial OL
																										// (PIN 17-09-306-032-1088)
				String coowner = "";
				Pattern p = Pattern.compile("([A-Z] [A-Z]{2,}) & ([A-Z] [A-Z]{2,})");
				Matcher ma = p.matcher(name);
				if (ma.matches()) {
					coowner = ma.group(2);
					name = ma.group(1);
				}
				tokens = StringFormats.parseNameDesotoRO(name);
				if (coowner.length() != 0) {
					String[] tokens2 = StringFormats.parseNameDesotoRO(coowner);
					tokens[3] = tokens2[0];
					tokens[4] = tokens2[1];
					tokens[5] = tokens2[2];
				}
			}
			String[] suffixes = GenericFunctions1.extractNameSuffixes(tokens);
			GenericFunctions1.addOwnerNames(tokens, suffixes[0], suffixes[1], false, false, body);
		} else {
			tokens[2] = name;
			GenericFunctions1.addOwnerNames(tokens, "", "", true, false, body);
		}
		GenericFunctions1.storeOwnerInPartyNames(m, body);
	}

	public static void taxILCookTR(ResultMap m, long searchId) throws Exception {

		boolean ignore2ndInstallment = false;
		String totalDue = (String) m.get("TaxHistorySet.TotalDue");
		if (totalDue == null || totalDue.length() == 0)
			totalDue = "0.00";

		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
		Date dateNow = new Date();

		// 1st installment
		List<String> line1 = null;
		//String tmp1stTaxBilled = (String) m.get("TaxHistorySet.BaseAmount");
		String tmp1stTaxBilled = (String) m.get("tmp1stTaxBillValue");
		if (tmp1stTaxBilled == null || tmp1stTaxBilled.length() == 0) {
			tmp1stTaxBilled = "0.00";
			line1 = new ArrayList<String>();
			line1.add(tmp1stTaxBilled);
		}
		else {
			line1 = new ArrayList<String>();
			line1.add(tmp1stTaxBilled);
		}

//		String tmp1stAmtPaid = (String) m.get("TaxHistorySet.AmountPaid");
		String tmp1stAmtPaid = (String) m.get("tmp1stAmtPaid");
		if (tmp1stAmtPaid == null || tmp1stAmtPaid.length() == 0)
			tmp1stAmtPaid = "0.00";
		if (line1 != null) {
			line1.add(tmp1stAmtPaid);
		}

		String tmp1stTaxDueDate = (String) m.get("tmp1stTaxDueDate");
		Date dueDate1 = null;
		if (tmp1stTaxDueDate != null && tmp1stTaxDueDate.length() > 0)
			dueDate1 = df.parse(tmp1stTaxDueDate);

		//String totalDueInst1 = (String) m.get(TaxHistorySetKey.TOTAL_DUE.getKeyName());
		//String basAmount1 = (String) m.get("TaxHistorySet.BaseAmount");
		String totalDueInst1 = (String) m.get("tmp1stTotalDue");
		if (StringUtils.isEmpty(totalDueInst1)) {
			totalDueInst1 = "0.00";
		}
		String basAmount1 = (String) m.get("tmp1stTaxBillValue");
		if (StringUtils.isEmpty(basAmount1)) {
			basAmount1 = "0.00";
		}
		String penalty1 = "0.00";

		if (!totalDueInst1.equals("0.00")) {
			penalty1 = new BigDecimal(totalDueInst1).subtract(new BigDecimal(basAmount1)).toString();
		}
		String status1 = "";
		if (line1 != null) {
			line1.add(totalDueInst1);
			line1.add(penalty1);
		}
		boolean totPaid1 = false;
		//String datPaid1 = (String) m.get("TaxHistorySet.DatePaid");
		String datPaid1 = (String) m.get("tmp1stTaxPaidDate");
		Date datePaid1 = null;
		if (datPaid1 != null && StringUtils.isNotEmpty(datPaid1)) {
			try {
				datePaid1 = df.parse(datPaid1);
			} catch (ParseException pe) {
				pe.printStackTrace();
				System.err.println(searchId + " taxILCookTR: cannot parse datePaid - 1st installment");
			}
		}
		if (totalDueInst1.equals("0.00")) {
			totPaid1 = true;
		} else if ((datePaid1 == null) && (totalDueInst1.equals("0.00")) && (basAmount1.equals("0.00"))) {
			totPaid1 = true;
		}
		if (!totalDueInst1.equals("0.00")) {
			if (dueDate1 != null) {
				if ((dateNow.before(dueDate1))) {
					status1 = "UNPAID";
				} else {
					status1 = "DELINQUENT";
				}
			} else {
				status1 = "UNPAID";
			}
		} else {
			if (datePaid1 != null) {
				if ((datePaid1.before(dueDate1)) || (datePaid1.equals(dueDate1))) {
					status1 = "PAID";
				} else if ((dateNow.before(dueDate1)) && (tmp1stAmtPaid.equals("0.00")))
				{
					status1 = "OPEN";
				}
			}
			if (totPaid1) {
				status1 = "PAID";
			}
		}
		if (line1 != null) {
			line1.add(status1);
		}

		// 2nd installment - compute base amount, amount paid, paid date when 2nd installment is present
		boolean has2ndInstallment = false;
		Date dueDate2 = null;
		String tmp2ndAmtPaid = (String) m.get("tmp2ndAmtPaid");
		String tmp2ndTaxPaidDate = (String) m.get("tmp2ndTaxPaidDate");
		String tmp2ndTaxBilled = (String) m.get("tmp2ndtTaxBillValue");

		List<String> line2 = null;
		List<String> lineHist2 = null;
		if (tmp2ndTaxBilled != null && tmp2ndTaxBilled.length() > 0) {
			has2ndInstallment = true;
			line2 = new ArrayList<String>();
			line2.add(tmp2ndTaxBilled);
			lineHist2 = new ArrayList<String>();

			if (tmp2ndAmtPaid == null || tmp2ndAmtPaid.length() == 0)
				tmp2ndAmtPaid = "0.00";
			line2.add(tmp2ndAmtPaid);
			lineHist2.add(tmp2ndAmtPaid);

			if (!ignore2ndInstallment) {
//				m.put("TaxHistorySet.BaseAmount", new BigDecimal(tmp1stTaxBilled).add(new BigDecimal(tmp2ndTaxBilled)).toString());
//				m.put("TaxHistorySet.AmountPaid", new BigDecimal(tmp1stAmtPaid).add(new BigDecimal(tmp2ndAmtPaid)).toString());
				// m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), GenericFunctions.sum(tmp1stTaxBilled + "+" + tmp2ndTaxBilled, searchId));
				if (StringUtils.isNotEmpty(tmp1stAmtPaid) && StringUtils.isNotEmpty(tmp2ndAmtPaid)) {
					m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), GenericFunctions.sum(tmp1stAmtPaid + "+" + tmp2ndAmtPaid, searchId));
				}
				
			} else {
				// m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), tmp1stTaxBilled);
				BigDecimal totalDueBD = new BigDecimal(totalDue);
				if (totalDueBD.compareTo(new BigDecimal(0)) != 0) {
					totalDueBD = totalDueBD.subtract(new BigDecimal(tmp2ndTaxBilled));
					m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDueBD.toString());
					//m.put("TaxHistorySet.CurrentYearDue", totalDueBD.toString());
				}
			}

			if (tmp2ndTaxPaidDate != null && tmp2ndTaxPaidDate.length() > 0)
				m.put(TaxHistorySetKey.DATE_PAID.getKeyName(), tmp2ndTaxPaidDate);

			String tmp2ndTaxDueDate = (String) m.get("tmp2ndTaxDueDate");
			if (tmp2ndTaxDueDate != null && tmp2ndTaxDueDate.length() > 0)
				dueDate2 = df.parse(tmp2ndTaxDueDate);
//			String totalDueInst2 = (String) m.get("TaxHistorySet.TotalDue");
//			String basAmount2 = (String) m.get("TaxHistorySet.BaseAmount");
			String totalDueInst2 = (String) m.get("tmp2ndTotalDue");
			String basAmount2 = (String) m.get("tmp2ndtTaxBillValue");
			String penalty2 = "0.00";
			if (!totalDueInst2.equals("0.00")) {
				penalty2 = new BigDecimal(totalDueInst2).subtract(new BigDecimal(basAmount2)).toString();
			}
			String status2 = "";
			line2.add(totalDueInst2);
			line2.add(penalty2);
			boolean totPaid2 = false;
			String datPaid2 = (String) m.get("TaxHistorySet.DatePaid");
			Date datePaid2 = null;
			if (datPaid2 != null && StringUtils.isNotEmpty(datPaid2)) {
				datePaid2 = df.parse(datPaid2);
			}
			if (totalDueInst2.equals("0.00")) {
				totPaid2 = true;
			} else if ((datePaid2 == null) && (totalDueInst2.equals("0.00")) && (basAmount2.equals("0.00"))) {
				totPaid2 = true;
			}
			if (!totalDueInst2.equals("0.00")) {
				if (dueDate2 != null) {
					if ((dateNow.before(dueDate2))) {
						status2 = "UNPAID";
					} else {
						status2 = "DELINQUENT";
					}
				} else {
					status2 = "UNPAID";
				}
			} else {
				if (datePaid2 != null) {
					if ((datePaid2.before(dueDate2)) || (datePaid2.equals(dueDate2))) {
						status2 = "PAID";
					} else if ((dateNow.before(dueDate2)) && (tmp2ndAmtPaid.equals("0.00"))) {
						status2 = "OPEN";
					}
				}
				if (totPaid2) {
					status2 = "PAID";
				}
			}
			line2.add(status2);
			if (datPaid2 != null) {
				lineHist2.add(datPaid2);
			} else
				lineHist2.add("");
		} else if (StringUtils.isNotEmpty(tmp1stAmtPaid)) {
			m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), tmp1stAmtPaid);
		}

		// m.put("TaxInstallmentSet.Status", status);
		List<List> bodyInstallments = new ArrayList<List>();
		String[] headerTIS = { "BaseAmount", "AmountPaid", "TotalDue", "PenaltyAmount", "Status" };
		if (line1 != null && line1.size() == headerTIS.length) {
			bodyInstallments.add(line1);
			if (line2 != null && line2.size() == headerTIS.length) {
				bodyInstallments.add(line2);
			}

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("BaseAmount", new String[] { "BaseAmount", "" });
			map.put("AmountPaid", new String[] { "AmountPaid", "" });
			map.put("TotalDue", new String[] { "TotalDue", "" });
			map.put("PenaltyAmount", new String[] { "PenaltyAmount", "" });
			map.put("Status", new String[] { "Status", "" });

			ResultTable installments = new ResultTable();
			installments.setHead(headerTIS);
			installments.setBody(bodyInstallments);
			installments.setMap(map);
			m.put("TaxInstallmentSet", installments);
		}

		List<String> lineHist1 = new ArrayList<String>();

		if (tmp1stAmtPaid != null) {
			lineHist1.add(tmp1stAmtPaid);
		} else
			lineHist1.add("");
		if (datPaid1 != null) {
			lineHist1.add(datPaid1);
		} else
			lineHist1.add("");

		List<List> bodyHist = new ArrayList<List>();
		if (lineHist1 != null) {
			bodyHist.add(lineHist1);
			if (lineHist2 != null) {
				bodyHist.add(lineHist2);
			}

			String[] header = { "ReceiptAmount", "ReceiptDate" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
			map.put("ReceiptDate", new String[] { "ReceiptDate", "" });

			ResultTable rtable = new ResultTable();
			rtable.setHead(header);
			rtable.setBody(bodyHist);
			rtable.setMap(map);
			m.put("TaxHistorySet", rtable);
		}

		// compute current year delinquent and total delinquent
		BigDecimal currentYearDelinq = new BigDecimal(0.00);
		if (dueDate1 != null) {
			if (dateNow.after(dueDate1)) {
				if (!has2ndInstallment) {
					currentYearDelinq = new BigDecimal(totalDue);
				} else if (dueDate2 != null) {
					if (dateNow.after(dueDate2)) {
						currentYearDelinq = new BigDecimal(totalDue);
					} else { // dueDate1 < dateNow <= dueDate2
						currentYearDelinq = new BigDecimal(totalDue).add(new BigDecimal(tmp2ndAmtPaid)).subtract(new BigDecimal(tmp2ndTaxBilled));
					}
				}
			}
		}

		BigDecimal totalDelinq = currentYearDelinq;
		String priorYearDelinq = (String) m.get(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName());
//		if (priorYearDelinq != null && priorYearDelinq.length() > 0)
//			totalDelinq = totalDelinq.add(new BigDecimal(priorYearDelinq));
//		m.put("TaxHistorySet.DelinquentAmount", totalDelinq.toString());

	}
}

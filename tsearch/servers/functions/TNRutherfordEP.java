package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.StringUtils;

public class TNRutherfordEP {

	protected static final Category	logger	= Logger.getLogger(TNRutherfordEP.class);

	public static void partyNamesTNRutherfordYA(ResultMap m, long searchId) throws Exception {

		String s = (String) m.get(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName());

		if (StringUtils.isEmpty(s))
			return;

		s = s.replaceAll("(?is)\\bETALS\\b", "ETAL");
		s = s.replaceAll("(?is)\\bET\\s+AL\\b", "ETAL");
		s = s.replaceAll("(?is)\\bET\\s+UX\\b", "ETUX");
		s = s.replaceAll("(?is)\\bETU\\b", "ETUX");
		s = s.replaceAll("%", "@@");
		s = s.replaceAll("C/O", "@@");
		s = s.replaceAll("(\\bCOMPAN\\b)$", "$1Y");
		s = s.replaceAll(",", " AND ");

		// s = s.replaceAll("(?is) TR(?:(?:UST)?EE)?S?", "");
		// s = s.replaceAll("&\\s*ETAL", "");
		// s = s.replaceFirst("PEDIGO", "ETUX PEDIGO"); // 00-103N-C-103N-01900--000 JONES EMILY C AND CALEB L C/O STEPHANIE LEE PEDIGO J
		s = s.replaceFirst("(WINTERS JR) E", "$1");
		s = s.replaceFirst("BROW$", "BROWN");
		s = s.replaceFirst("WIFE", ""); // 13-081I-A-081I-00100-C-098 PARIGIN CALVIN GLENN ANDWIFE JANIE PARGIN

		// if ((!s.contains("ETAL")) && s.contains(" AND ")) {
		// s = s.replaceAll(" AND ", " @@ ");
		// }
		// if (s.matches("(.+)\\s+ETALS?\\s+(\\w+)")) {
		// s = s.replaceAll("ETAL", "ETUX");
		// }
		String[] owners;
		owners = s.split("@@");

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		Pattern p3 = Pattern.compile("(.+)\\s+ETALS?\\s+(\\w+)");
		Matcher ma;

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];
			ma = p3.matcher(s);

			names = StringFormats.parseNameNashville(ow, true);
			ow = ow.replaceAll("(\\w+\\s+\\w)\\s+(\\w+)\\s+AND\\s+(\\w+)", "$1 $2 AND $3 $2");

			if ((ma.matches()) && (i == 1)) {
				if (ow.contains("AND")) {
					String[] temp = ow.split("(AND)");
					String aux = temp[0];
					temp[0] = temp[1];
					temp[1] = aux;
					ow = temp[0] + " AND " + temp[1];
				}
				names = StringFormats.parseNameDesotoRO(ow, true);
			}
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);

	}

	public static void parseDeedInformationYA(ResultMap m, long searchId) throws Exception {

		String deedInfo = (String) m.get("tmpDeed");

		if (org.apache.commons.lang.StringUtils.isEmpty(deedInfo)) {
			return;
		}

		List<List> body = new ArrayList<List>();
		List<String> list = new ArrayList<String>();

		String[] items = deedInfo.split("\\s+");

		if (items.length == 2) {
			String bookPage = items[0].trim();
			String instrumentDate = items[1].trim();

			if (StringUtils.isNotEmpty(bookPage)) {
				String[] bp = bookPage.split("\\s*-\\s*");
				if (bp.length == 2) {
					list.add(bp[0]);
					list.add(bp[1]);
					if (StringUtils.isNotEmpty(instrumentDate)) {
						list.add(instrumentDate.trim());
					} else {
						list.add("");
					}
					body.add(list);
				}
			}
			if (!body.isEmpty() && body.size() > 0) {
				String[] header = new String[] { "Book", "Page", "InstrumentDate" };
				ResultTable rt = new ResultTable();
				rt = GenericFunctions2.createResultTable(body, header);
				m.put(SaleDataSet.class.getSimpleName(), rt);
			}
		}
	}

	public static void taxRutherfordYA(ResultMap m, long searchId) throws Exception {
		String totalDue = (String) m.get(TaxHistorySetKey.TOTAL_DUE.getKeyName());
		if (totalDue == null)
			totalDue = "0";

		m.put(TaxHistorySetKey.CURRENT_YEAR_DUE.getKeyName(), totalDue);

		ResultTable totalsTbl = (ResultTable) m.get("TaxHistorySet");
		if (totalsTbl == null)
			return;

		String body[][] = totalsTbl.getBody();
		int len = body.length;
		if (len == 0)
			return;

//		BigDecimal priorDelinq = new BigDecimal(0);
//		for (int i = 1; i < len; i++) {
//			try {
//				if (body[i][4].matches("[\\d\\.,]+")) {
//					priorDelinq = priorDelinq.add(new BigDecimal(body[i][4]));
//				}
//			} catch (Exception e) {
//				logger.error("Parsing error", e);
//			}
//		}
//		m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinq.toString());

		String baseAmount = (String) m.get("tmpBaseAmount");
		if (baseAmount != null) {
			if (!"0.00".equals(baseAmount)) {
				m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
			} else {
				if (!"0.00".equals(m.get(TaxHistorySetKey.AMOUNT_PAID.getKeyName()))) {
					m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), m.get(TaxHistorySetKey.AMOUNT_PAID.getKeyName()));
				}
			}
		}

	}

	public static void parseTaxes(NodeList nodeList, ResultMap resultMap, long searchId) {
		NodeList tableList=nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView5"), true);
//		double priorDelinquent = 0;
		BigDecimal priorDelinquent = new BigDecimal(0);

		if (tableList != null) {
			TableTag tmpTable = (TableTag) tableList.elementAt(0);
			if (tmpTable.getRowCount() >= 2) {
				TableRow row = tmpTable.getRow(1);
				if (row.getColumnCount() >= 6) {
					
					String taxYear = row.getColumns()[0].getChildrenHTML().trim();
					String tmpbaseAmt = row.getColumns()[2].getChildrenHTML().trim().replaceAll("[$,]", "");
					String amtPaid = "";
					String amtDue = "";
					if (row.getColumnCount() == 7) {
						amtPaid = row.getColumns()[5].getChildrenHTML().trim().replaceAll("[$,]", "");
						amtDue = row.getColumns()[4].getChildrenHTML().trim().replaceAll("[$,]", "");

					} 
					
					if (StringUtils.isNotEmpty(taxYear)) {
						resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
					}
					if (StringUtils.isNotEmpty(amtPaid)) {
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amtPaid);
					}
					if (StringUtils.isNotEmpty(amtDue)) {
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amtDue);
					}
					if ("0.00".equals(amtDue) && !"0.00".equals(amtPaid)){
						tmpbaseAmt = tmpbaseAmt.replaceAll("[$,]","");
						if (StringUtils.isNotEmpty(tmpbaseAmt) && "0.00".equals(tmpbaseAmt)) {
							resultMap.put("tmpBaseAmount", amtPaid);
						}
					} else {
						resultMap.put("tmpBaseAmount", tmpbaseAmt);
					}
				}
			}
		}

		if (tableList != null) {
			TableTag tmpTable = (TableTag) tableList.elementAt(0);
			try {
				ResultTable receipts = new ResultTable();
				Map<String, String[]> map = new HashMap<String, String[]>();
				String[] header = { "ReceiptNumber","ReceiptDate", "ReceiptAmount" };
				List<List<String>> bodyRT = new ArrayList<List<String>>();
				NumberFormat formatter = new DecimalFormat("#.##");

				if (tmpTable.getRowCount() >=2) {
					for (int i = 1; i < tmpTable.getRowCount(); i++) {
						TableRow row = tmpTable.getRow(i);
						if (row.getColumnCount() == 7) {
							TableColumn col = row.getColumns()[1];
							String bill = col.getChildrenHTML().trim().toLowerCase();
							
							col = row.getColumns()[6];
							String date = col.getChildrenHTML().trim().replaceAll("[$,]", "");
							date = date.replaceAll("&nbsp;", "").trim();
							
							col = row.getColumns()[5];
							String amtPaid = col.getChildrenHTML().trim().replaceAll("[$,]", "");
							amtPaid = amtPaid.replaceAll("&nbsp;", "").trim();
							
							col = row.getColumns()[4];
							String amtUnpaid = col.getChildrenHTML().trim().replaceAll("[$,]", "");
							if (StringUtils.isEmpty(amtPaid) && StringUtils.isNotEmpty(amtUnpaid)) {
								if (!resultMap.get(TaxHistorySetKey.YEAR.getKeyName()).equals(row.getColumns()[0].getChildrenHTML().trim())) {
									try {
										//priorDelinquent += Double.parseDouble(amtUnpaid);
										priorDelinquent = priorDelinquent.add(new BigDecimal(amtUnpaid));	
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}
									
							
							List<String> paymentRow = new ArrayList<String>();
							paymentRow.add(bill);
							paymentRow.add(date);
							paymentRow.add(amtPaid);
							bodyRT.add(paymentRow);
						}
					}
					
					map.put("ReceiptNumber", new String[] { "ReceiptNumber", "" });
					map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
					map.put("ReceiptAmount", new String[] { "ReceiptAmount", "" });
					receipts.setHead(header);
					receipts.setMap(map);
					receipts.setBody(bodyRT);
					receipts.setReadOnly();
					resultMap.put("TaxHistorySet", receipts);
					
//					if (priorDelinquent != 0) {
					resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent.toString());
//					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

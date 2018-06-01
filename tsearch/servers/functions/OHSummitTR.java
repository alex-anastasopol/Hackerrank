package ro.cst.tsearch.servers.functions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.Roman;

/**
 * 
 * @author Oprina George
 * 
 *         Nov 17, 2011
 */

public class OHSummitTR {
	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo) {
		ResultMap m = new ResultMap();
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

		TableColumn[] cols = row.getColumns();

		if (cols.length == 6) {
			String parcel = cols[1].toPlainTextString();
			String route = cols[2].toPlainTextString();
			String addr = cols[3].toPlainTextString();
			String owner = cols[4].toPlainTextString();

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(parcel)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
			}
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(route)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), route);
			}

			parseAddress(m, addr);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(owner)) {
				List<String> names = new ArrayList<String>();
				if (NameUtils.isNotCompany(owner)) {
					names = Arrays.asList(owner.split("\\&"));
				} else {
					names.add(owner);
				}
				parseNames(m, names, "");
			}
		}
		return m;
	}

	public static void parseNames(ResultMap m, List<String> all_names, String auxString) {
		try {
			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();

			StringBuffer nameOnServer = new StringBuffer();

			for (String n : all_names) {
				n = n.toUpperCase().trim().replaceAll("\\s+", " ");
				n = n.replaceAll("\\sAND\\s", " & ");
				n = n.replaceAll("(?is)\\bCO\\s*(TRUSTEES?)\\b", "$1");
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(n)) {
					nameOnServer.append(n + " & ");
					String[] names = StringFormats.parseNameNashville(n, true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
			}
			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), StringUtils.strip(nameOnServer.toString()).replaceAll("\\&$", ""));
			if (body.size() > 0)
				GenericFunctions.storeOwnerInPartyNames(m, body, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAddress(ResultMap m, String addr) {
		addr = StringUtils.strip(StringUtils.defaultString(addr));

		if (StringUtils.isEmpty(addr))
			return;

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr.replaceAll("\\s+", " "));

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(addr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(addr)));
	}

	public static void parseLegal(ResultMap m, String legal) {
		if (StringUtils.isEmpty(legal))
			return;

		String legalDes = legal;

		m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDes);

		Pattern LEGAL_LOT = Pattern.compile("\\bLOTS? ?(\\w+(-|(\\s?&\\s?))?\\w*)");
		Pattern LEGAL_TRACT = Pattern.compile("\\bTR(ACT)? ?(\\d+\\s?&?\\s?\\d*)");
		Pattern LEGAL_BLK = Pattern.compile("\\bBLK ?((?:[A-Z]-?)?\\d?)");
		Pattern LEGAL_PHASE = Pattern.compile("\\bPHA?S?E? ?(\\w+((?:[\\s|-][A-Z]?\\d?)+))\\b");
		Pattern LEGAL_UNIT = Pattern.compile("\\bUNIT ?(\\d+)");
		Pattern LEGAL_PB = Pattern.compile("\\sPB\\s?(\\d+-?\\d*)");
		Pattern LEGAL_PP = Pattern.compile("\\sPG\\s?(\\d+-?\\d*)");
		Pattern LEGAL_SUB_NAME = Pattern.compile("\\b(.*?)\\s+(TR(?:ACTS?)?|BLK|PH(?:ASE)?|LOTS?)");

		Matcher matcher = LEGAL_SUB_NAME.matcher(legalDes);
		if (matcher.find()){
			String subdName = matcher.group(1);
			
			subdName = subdName.replaceFirst("(?is)\\bTR(?:ACTS?)?\\b\\s*\\d+", "");
			subdName = subdName.replaceAll("\\s*#?\\d+\\s*$", "");
			subdName = subdName.replaceAll("\\A\\s*SV-", "");
			subdName = subdName.replaceAll("\\bALL?\\s*LOT\\b", "");
			
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(subdName)){
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdName.trim());
			}
		}

		// get lot
		matcher = LEGAL_LOT.matcher(legalDes);
		StringBuffer lots = new StringBuffer();
		while (matcher.find()) {
			String lot = matcher.group(1);
			if (StringUtils.isNotEmpty(lot)) {
				lot = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(lot.trim()).replace("&", " ").replaceAll("\\s+", " ");
				
				lot = lot.replaceAll("ALL", "");
				lots.append(lot.trim()).append(" ");
			}
			legalDes = legalDes.replaceFirst(matcher.group(), " ");
		}

		m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots.toString().trim().replaceAll("(,$)|(^,)", ""));

		// get tract
		matcher = LEGAL_TRACT.matcher(legalDes);

		StringBuffer tracts = new StringBuffer();
		while (matcher.find()) {
			String tract = matcher.group(2);
			if (StringUtils.isNotEmpty(tract)) {
				tract = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(tract.trim()).replace("&", " ").replaceAll("\\s+", " ")
						.replace(" ", ", ");
				tracts.append(tract.trim() + ", ");
			}
			legalDes = legalDes.replaceFirst(matcher.group(), " ");
		}
		m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(),
				tracts.toString().trim().replaceAll(",\\s*,", " ").replaceAll(",+", " ").replaceAll("(,$)|(^,)", ""));

		// get blk
		matcher = LEGAL_BLK.matcher(legalDes);
		if (matcher.find()) {
			String blk = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), StringUtils.strip(blk));
		}

		// get phase
		matcher = LEGAL_PHASE.matcher(legalDes);
		if (matcher.find()) {
			String phase = matcher.group(1).trim();
			if (Roman.isRoman(phase)) {
				try {
					phase = Integer.toString(Roman.parseRoman(phase));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			legalDes = legalDes.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), StringUtils.strip(phase));
		}

		// get unit
		matcher = LEGAL_UNIT.matcher(legalDes);
		if (matcher.find()) {
			String unit = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), StringUtils.strip(unit));
		}

		// pb
		matcher = LEGAL_PB.matcher(legalDes);
		if (matcher.find()) {
			String pb = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(pb))
				pb = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(pb.trim());
			m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), StringUtils.strip(pb));
		}

		// pp
		matcher = LEGAL_PP.matcher(legalDes);
		if (matcher.find()) {
			String pp = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(pp))
				pp = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(pp.trim());
			m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), StringUtils.strip(pp));
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			NodeList nodes = new HtmlParser3(detailsHtml).getNodeList();

			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			String parcel = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "PARCEL", true), "", false).trim();
			if (StringUtils.isNotEmpty(parcel)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
			}
			String altId = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(tables, "ALT_ID", true), "", false).trim();
			if (StringUtils.isNotEmpty(altId)) {
				altId = altId.replaceAll("(?is)ALT_ID", "").trim();
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), altId);
			}

			String owner = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "OWNER", true), "", false).trim();
			if (StringUtils.isNotEmpty(owner)) {
				List<String> names = new ArrayList<String>();
				if (NameUtils.isNotCompany(owner)) {
					if (owner.matches("(?is)[^&]+&\\s*[A-Z]{1,2}\\s+[A-Z]+")//THOMPSON MICKEY LEROY &JO ANNE  6839159 
							|| owner.matches("(?is)[^&]+&\\s*[A-Z]+\\s+[A-Z]")){//THOMPSON THOMAS T & SALLY T   6700298
						names.add(owner);
					} else {
						names = Arrays.asList(owner.split("\\&"));
					}
				} else {
					names.add(owner);
				}
				parseNames(resultMap, names, "");
			}

			Node yNode = HtmlParser3.findNode(tables, "Summit County Auditor Division, OH - Tax Year", false);
			if (yNode != null) {
				String year = yNode.toPlainTextString().replace("Summit County Auditor Division, OH - Tax Year", "").trim();
				if (StringUtils.isNotEmpty(year)) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
				}
			}

			String addr = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "ADDR.", true), "", false).trim().replaceAll("\\s+", " ");
			if (StringUtils.isNotEmpty(owner)) {
				String[] parts = addr.split(",");
				parseAddress(resultMap, parts[0].trim());
				if (parts.length == 2) {
					String cityZip = parts[1].trim();
					String[] partsCity = cityZip.split(" ");
					if (partsCity.length == 2) {
						resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), partsCity[0]);
						resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), partsCity[1].replaceAll("-+$", ""));
					}
				}
			}

			String legal = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "DESC.", true), "", false).trim().replaceAll("\\s+", " ");
			parseLegal(resultMap, legal);

			NodeList n;
			TableTag summaryTable = null;
			if ((n = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "SummaryTable"), true)).size() > 0) {
				summaryTable = (TableTag) n.elementAt(0);
			}

			if (summaryTable != null) {
				Map<String, String> map = new HashMap<String, String>();
				map.put("bgcolor", "c0c0c0");

				String land = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "ASSESSED LAND:", true), "", false).trim()
						.replaceAll("\\s+", " ");
				String building = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "ASSESSED BLDG:", true), "", false).trim()
						.replaceAll("\\s+", " ");
				String totalAppr = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "TOTAL:", true), "", false).trim()
						.replaceAll("\\s+", " ");
				String totalAssesed = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "ASSESSED TOTAL:", true), "", false).trim()
						.replaceAll("\\s+", " ");

				if (!StringUtils.isEmpty(land)) {
					resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
				}
				
				if (!StringUtils.isEmpty(building)) {
					resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), building);
				}

				if (!StringUtils.isEmpty(totalAppr)) {
					resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), totalAppr);
				}

				if (!StringUtils.isEmpty(totalAssesed)) {
					resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAssesed);
				}
				
				NodeList trs = nodes.extractAllNodesThatMatch(new TagNameFilter("tr"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("bgcolor", "yellow"), true);

				Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
				List<List> installmentsBody = new ArrayList<List>();
				List installment1Row = new ArrayList();
				List installment2Row = new ArrayList();
				ResultTable resultTable = new ResultTable();
				String[] installmentsHeader =
				{ TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
						TaxInstallmentSetKey.PENALTY_AMOUNT.getShortKeyName(),
						TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName(),
						TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
						TaxInstallmentSetKey.STATUS.getShortKeyName()
				};

				for (String s : installmentsHeader)
				{
					installmentsMap.put(s, new String[] { s, "" });
				}

				if (trs.size() > 0) {
					for (int i = 0; i < trs.size(); i++) {
						TableRow r = (TableRow) trs.elementAt(i);
						if (r.getColumnCount() == 5) {
							if (r.toHtml().contains("TOTAL REAL ESTATE AND")) {
								Double amount1 = Double.parseDouble(r.getColumns()[2].toPlainTextString().trim());
								Double amount2 = Double.parseDouble(r.getColumns()[3].toPlainTextString().trim());
								installment1Row.add(amount1.toString());
								installment2Row.add(amount2.toString());

								double amount = amount1 + amount2;
								resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), Double.toString(amount));
							} else if (r.toHtml().contains("PAYMENTS")) {
								Double paid1 = Double.parseDouble(r.getColumns()[2].toPlainTextString().trim().replaceFirst("-", ""));
								Double paid2 = Double.parseDouble(r.getColumns()[2].toPlainTextString().trim().replaceFirst("-", ""));
								installment1Row.add(paid1.toString());
								installment2Row.add(paid2.toString());

								double paid = paid1 + paid2;
								resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), Double.toString(paid));
							} else if (r.toHtml().contains("AMOUNT DUE")) {
								Double due1 = Double.parseDouble(r.getColumns()[2].toPlainTextString().trim());
								Double due2 = Double.parseDouble(r.getColumns()[3].toPlainTextString().trim());
								installment1Row.add(due1.toString());
								installment2Row.add(due2.toString());
								String status1 = "PAID";
								String status2 = "PAID";
								if (due1 > 0.0) {
									status1 = "UNPAID";
								}
								if (due2 > 0.0) {
									status2 = "UNPAID";
								}
								installment1Row.add(status1);
								installment2Row.add(status2);

								double due = due1 + due2;
								resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), Double.toString(due));
								resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), r.getColumns()[1].toPlainTextString().trim());
							}
							else if (r.toHtml().contains("P & I & ADJ")) {
								Double penalty1 = Double.parseDouble(r.getColumns()[2].toPlainTextString().trim());
								Double penalty2 = Double.parseDouble(r.getColumns()[3].toPlainTextString().trim());
								installment1Row.add(penalty1.toString());
								installment2Row.add(penalty2.toString());
							}
						}
					}
					if (installment1Row.size() == 5) {
						installmentsBody.add(installment1Row);
					}
					if (installment2Row.size() == 5) {
						installmentsBody.add(installment2Row);
					}
				}
				if (!installmentsBody.isEmpty()) {
					try {
						resultTable.setHead(installmentsHeader);
						resultTable.setMap(installmentsMap);
						resultTable.setBody(installmentsBody);
					} catch (Exception e) {
						e.printStackTrace();
					}
					resultTable.setReadOnly();
					resultMap.put("TaxInstallmentSet", resultTable);
				}
			}

			// related docs
			NodeList relDocs = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocumentTable"));
			if (relDocs.size() > 0) {
				TableTag t = (TableTag) relDocs.elementAt(0);
				TableRow[] rows = t.getRows();

				NodeList aux = new NodeList();

				String[] transferHeader = { SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), SaleDataSetKey.RECORDED_DATE.getShortKeyName(),
						SaleDataSetKey.GRANTOR.getShortKeyName(), SaleDataSetKey.GRANTEE.getShortKeyName(), SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(),
						SaleDataSetKey.BOOK.getShortKeyName(), SaleDataSetKey.PAGE.getShortKeyName() };
				List<List<String>> transferBody = new ArrayList<List<String>>();
				List<String> transferRow = new ArrayList<String>();
				ResultTable resT = new ResultTable();
				Map<String, String[]> transferMap = new HashMap<String, String[]>();
				for (String s : transferHeader) {
					transferMap.put(s, new String[] { s, "" });
				}

				for (int i = 0; i < rows.length; i++) {
					TableRow r = rows[i];
					if (r.toHtml().split("(?ism)<[^>]*hr[^>]*>").length == 2) {
						// parse related docs
						transferRow = new ArrayList<String>();

						String instrNo = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(aux, "RECEPTION NO:"), "", false).trim();
						String recDate = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(aux, "RECORDED:"), "", false).trim()
								.replaceAll("\\s+", " ");
						String grantor = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(aux, "GRANTOR:"), "", false).trim()
								.replaceAll("\\s+", " ");
						String grantee = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(aux, "GRANTEE:"), "", false).trim()
								.replaceAll("\\s+", " ");
						String type = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(aux, "TYPE:"), "", false).trim()
								.replaceAll("\\s+", " ");
						String bp = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(aux, "BOOK/PAGE:"), "", false).trim()
								.replaceAll("\\s+", " ");

						String book = "";
						String page = "";

						if (StringUtils.isNotEmpty(recDate)) {
							recDate = recDate.replaceAll(".*(\\w+-\\w+-\\w+).*", "$1");
							SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy");
							Date d = sdf.parse(recDate);
							if (d != null) {
								Calendar c = Calendar.getInstance();
								c.setTime(d);
								recDate = (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.YEAR);
							}
						}

						if (StringUtils.isNotEmpty(bp)) {

						}

						transferRow.add(StringUtils.defaultString(instrNo));
						transferRow.add(StringUtils.defaultString(recDate));
						transferRow.add(StringUtils.defaultString(grantor));
						transferRow.add(StringUtils.defaultString(grantee));
						transferRow.add(StringUtils.defaultString(type));
						transferRow.add(StringUtils.defaultString(book));
						transferRow.add(StringUtils.defaultString(page));
						transferBody.add(transferRow);
						aux = new NodeList();
					} else {
						aux.add(r);
					}
				}

				if (transferBody.size() > 0) {
					resT.setHead(transferHeader);
					resT.setMap(transferMap);
					resT.setBody(transferBody);
					resT.setReadOnly();
					resultMap.put("SaleDataSet", resT);
				}
			}

			NodeList rcptList = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "PayHistTable"));
			if (rcptList.size() > 0) {
				TableTag t = (TableTag) rcptList.elementAt(0);
				TableRow[] rows = t.getRows();

				String[] rcptHeader = { TaxHistorySetKey.RECEIPT_DATE.getShortKeyName(), TaxHistorySetKey.RECEIPT_AMOUNT.getShortKeyName(),
						TaxHistorySetKey.RECEIPT_NUMBER.getShortKeyName() };
				List<List<String>> rcptBody = new ArrayList<List<String>>();
				List<String> rcptRow = new ArrayList<String>();
				ResultTable resT = new ResultTable();
				Map<String, String[]> rcptMap = new HashMap<String, String[]>();
				for (String s : rcptHeader) {
					rcptMap.put(s, new String[] { s, "" });
				}

				for (int i = 3; i < rows.length; i++) {
					TableRow r = rows[i];
					if (r.getColumnCount() == 5) {
						String date = r.getColumns()[0].toPlainTextString().trim();
						String amount = r.getColumns()[1].toPlainTextString().trim();
						String number = r.getColumns()[2].toPlainTextString().trim();

						rcptRow = new ArrayList<String>();
						rcptRow.add(date);
						rcptRow.add(amount);
						rcptRow.add("/".equals(number) ? "" : number);
						rcptBody.add(rcptRow);
					}
				}

				if (rcptBody.size() > 0) {
					resT.setHead(rcptHeader);
					resT.setMap(rcptMap);
					resT.setBody(rcptBody);
					resT.setReadOnly();
					resultMap.put("TaxHistorySet", resT);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Node getNodeByTypeAttributeDescription(NodeList nl, String type, String attributeName, String attributeValue, String description,
			boolean recursive) {
		NodeList returnList = null;

		if (!StringUtils.isEmpty(attributeName))
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
					new HasAttributeFilter(attributeName, attributeValue), recursive);
		else
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive);

		if (returnList != null)
			for (int i = 0; i < returnList.size(); i++)
				if (returnList.elementAt(i).toHtml().contains(description))
					return returnList.elementAt(i);

		return null;
	}

	public static Node getNodeByTypeAttributeDescription(NodeList nl, String type, String attributeName, String attributeValue, String[] description,
			boolean recursive) {
		NodeList returnList = null;

		if (!StringUtils.isEmpty(attributeName))
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
					new HasAttributeFilter(attributeName, attributeValue), recursive);
		else
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive);

		for (int i = returnList.size() - 1; i >= 0; i--) {
			boolean flag = true;
			for (String s : description) {
				if (!StringUtils.containsIgnoreCase(returnList.elementAt(i).toHtml(), s)) {
					flag = false;
					break;
				}
			}
			if (flag)
				return returnList.elementAt(i);
		}

		return null;
	}
}

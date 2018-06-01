package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

public class TNWilliamsonYB {

	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo) {
		ResultMap m = new ResultMap();

		TableColumn[] cols = row.getColumns();

		if (cols.length >= 4) {
			String parcel = "";
			String tax = "";
			String owner = "";
			String addr = "";

			switch (additionalInfo) {
			case 1:
				parcel = cols[0].toPlainTextString();
				tax = cols[1].toPlainTextString();
				addr = cols[2].toPlainTextString();
				owner = cols[3].toPlainTextString();
				break;

			case 2:
				if (cols.length > 5)
					parcel = cols[5].toPlainTextString();
				tax = cols[1].toPlainTextString();
				addr = cols[0].toPlainTextString();
				owner = cols[2].toPlainTextString() + "===" + cols[3].toPlainTextString();
				break;

			case 3:
				parcel = cols[0].toPlainTextString();
				tax = cols[2].toPlainTextString();
				addr = cols[3].toPlainTextString();
				owner = "";
				break;

			default:
				break;
			}

			if (StringUtils.isNotEmpty(parcel)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), tax.replaceAll("[ -]", ""));
			}
			if (StringUtils.isNotEmpty(tax)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), parcel);
			}

			parseAddress(m, addr);

			if (StringUtils.isNotEmpty(owner)) {
				parseNames(m, Arrays.asList(owner.replaceAll("&amp;", "").split("===")), "");
			}
		}
		return m;
	}

	public static void parseNames(ResultMap m, List<String> all_names, String auxString) {
		try {
			if (all_names == null)
				return;
			if (all_names.size() == 0)
				return;

			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();

			StringBuffer nameOnServer = new StringBuffer();

			for (String n : all_names) {
				n = n.toUpperCase().replaceAll("<[^>]*>", "")
						.replaceAll("\\s+", " ")
						.trim();

				boolean isCompany = false;

				if (StringUtils.isNotEmpty(n)) {
					nameOnServer.append(n + " & ");
					String[] names = new String[] { "", "", "", "", "", "" };
					if (n.contains("/")) {
						names[2] = n;
						isCompany = true;
					} else {
						names = StringFormats.parseNameNashville(n, true);
					}
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractNameSuffixes(names);

					if (!isCompany) {
						isCompany = NameUtils.isCompany(names[2]);
					}

					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, isCompany,
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

		String newAddr = addr;

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(newAddr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(newAddr)));
	}

	public static void parseLegal(ResultMap m, String legal) {
	}

	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			NodeList nodes = new HtmlParser3(detailsHtml.replaceAll("(?sim)&nbsp;", " ")).getNodeList();

			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			NodeList auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "propertyInfo"));

			if (auxNodes.size() > 0) {
				String id = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(auxNodes, "Parcel ID", true), "", false).trim();

				if (StringUtils.isNotEmpty(id)) {
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), id.replaceAll("[ -]", ""));
				}

				String address = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(auxNodes, "Property Location", true), "", false).trim();

				if (StringUtils.isNotEmpty(address)) {
					parseAddress(resultMap, address);
				}

				TableTag t = (TableTag) auxNodes.elementAt(0);

				TableRow[] rows = t.getRows();

				ArrayList<String> owners = new ArrayList<String>();

				for (int i = 4; i < rows.length - 2; i++) {
					owners.add(rows[i].toPlainTextString().trim());
				}

				if (!owners.isEmpty()) {
					parseNames(resultMap, owners, "");
				}
			}

			auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxInfo"));

			String year = "";

			if (auxNodes.size() > 0) {
				TableTag t = (TableTag) auxNodes.elementAt(0);

				TableRow[] rows = t.getRows();

				double pd = 0;

				for (int i = 1; i < rows.length; i++) {
					if (rows[i].getColumnCount() == 8) {
						if (i == 1) {
							year = rows[i].getColumns()[0].toPlainTextString().trim();

							String assessed = rows[i].getColumns()[2].toPlainTextString().replaceAll("[ -,$]", "");
							String ba = rows[i].getColumns()[4].toPlainTextString().replaceAll("[ -,$]", "");

							String ad = rows[i].getColumns()[7].toPlainTextString().replaceAll("[ -,$]", "");
							
							resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);

							if (StringUtils.isNotEmpty(assessed)) {
								resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessed);
							}

							if (StringUtils.isNotEmpty(ba)) {
								resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), ba);
							}
							
							if (StringUtils.isNotEmpty(ad)) {
								resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), ad);
							}
						} else {
							try {
								String balance = rows[i].getColumns()[7].toPlainTextString().replaceAll("[ -,$]", "");
								if (balance.matches("\\d+\\.?\\d+")) {
									pd += Double.parseDouble(balance);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}

				resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), Double.toString(pd));

			}

			auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "receiptInfo"));

			if (auxNodes.size() > 0) {
				TableTag t = (TableTag) auxNodes.elementAt(0);

				TableRow[] rows = t.getRows();

				String[] rcptHeader = { TaxHistorySetKey.YEAR.getShortKeyName(), TaxHistorySetKey.DATE_PAID.getShortKeyName(),
						TaxHistorySetKey.RECEIPT_NUMBER.getShortKeyName(), TaxHistorySetKey.RECEIPT_AMOUNT.getShortKeyName() };
				List<List<String>> rcptBody = new ArrayList<List<String>>();
				List<String> rcptRow = new ArrayList<String>();
				ResultTable resT = new ResultTable();
				Map<String, String[]> rcptMap = new HashMap<String, String[]>();

				for (String s : rcptHeader) {
					rcptMap.put("TaxHistorySet." + s, new String[] { s, "" });
				}

				for (int i = 1; i < rows.length; i++) {
					if (rows[i].getColumnCount() == 6) {
						String rcptYear = rows[i].getColumns()[0].toPlainTextString().trim();
						String rcptNo = rows[i].getColumns()[1].toPlainTextString().trim();
						String paimentDate = rows[i].getColumns()[2].toPlainTextString().trim();
						String amount = rows[i].getColumns()[5].toPlainTextString().replaceAll("[ -,$]", "");

						if (rcptYear.equals(year) && resultMap.get(TaxHistorySetKey.AMOUNT_PAID.getKeyName()) == null) {
							resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amount);
							resultMap.put(TaxHistorySetKey.DATE_PAID.getKeyName(), paimentDate);
						}

						rcptRow = new ArrayList<String>();
						rcptRow.add(rcptYear);
						rcptRow.add(rcptNo);
						rcptRow.add(paimentDate);
						rcptRow.add(amount);

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
				if (!StringUtils.containsIgnoreCase(returnList.elementAt(i).toHtml(), s))
					flag = false;
			}
			if (flag)
				return returnList.elementAt(i);
		}

		return null;
	}

}

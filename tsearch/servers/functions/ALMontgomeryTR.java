package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

/**
 * 
 * @author Oprina George
 * 
 *         Nov 1, 2012
 */

public class ALMontgomeryTR {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId, String parcel, TableTag t) {

		ResultMap m = new ResultMap();

		if (StringUtils.isNotEmpty(parcel)) {
			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel.replaceAll("[ -]", ""));
		}

		NodeList nodes = t.getChildren();

		String address = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "ADDRESS:"), "", true).replaceAll("\\s+", " ").trim();

		parseAddress(m, address);

		String owner = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "OWNER NAME:"), "", true).trim();

		if (StringUtils.isNotEmpty(owner)) {
			ArrayList<String> names = new ArrayList<String>();
			names.add(owner);
			parseNames(m, names, "");
		}

		String receipt = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "RECEIPT NO:"), "", true).trim().replaceAll("<[^>]*>", "");

		String land = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "LAND VALUE:"), "", true).trim().replaceAll("\\s+", "");
		String improvement = (land.contains("<br") ? land.split("(?ism)<br[^>]*>")[1] : "").replaceAll("<[^>]*>", "").replaceAll("\\s+", "");
		land = land.split("(?ism)<br[^>]*>")[0].replaceAll("<[^>]*>", "").replaceAll("\\s+", "");
		String total = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "TOTAL VALUE:"), "", true).trim().replaceAll("<[^>]*>", "")
				.replaceAll("\\s+", "");
		String ba = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "TOTAL TAX:"), "", true).trim().replaceAll("<[^>]*>", "")
				.replaceAll("\\s+", "");
		String ap = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "PAID:"), "", true).trim().replaceAll("<[^>]*>", "")
				.replaceAll("\\s+", "");
		String td = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "TOTAL DUE:"), "", true).trim().replaceAll("<[^>]*>", "")
				.replaceAll("\\s+", "");

		if (StringUtils.isNotEmpty(receipt)) {
			m.put(TaxHistorySetKey.RECEIPT_NUMBER.getKeyName(), receipt.trim());
		}

		if (StringUtils.isNotEmpty(land)) {
			m.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land.replaceAll("[ -,$]", ""));
		}

		if (StringUtils.isNotEmpty(improvement)) {
			m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvement.replaceAll("[ -,$]", ""));
		}

		if (StringUtils.isNotEmpty(total)) {
			m.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), total.replaceAll("[ -,$]", ""));
		}

		if (StringUtils.isNotEmpty(ba)) {
			m.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), ba.replaceAll("[ -,$]", ""));
		}

		if (StringUtils.isNotEmpty(ap)) {
			m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), ap.replaceAll("[ -,$]", ""));
		}

		if (StringUtils.isNotEmpty(td)) {
			m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), td.replaceAll("[ -,$]", ""));
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
						.replaceAll("\\s+", " ").trim()
						.replaceAll("\\&$", "");

				if (StringUtils.isNotEmpty(n)) {
					nameOnServer.append(n + " & ");
					String[] names = new String[] { "", "", "", "", "", "" };
					if(n.contains("/")){
						names = new String[] { "", "", n, "", "", "" };
					} else {
						names = StringFormats.parseNameNashville(n, true);
					}
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
			}
			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer.toString().trim().replaceAll("\\&$", ""));
			if (body.size() > 0)
				GenericFunctions.storeOwnerInPartyNames(m, body, true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAddress(ResultMap m, String addr) {
		if (StringUtils.isEmpty(addr))
			return;

		String newAddr = addr.split("<[^>]*>")[0];

		newAddr = newAddr.replace("..", "").replaceAll("C/O", "").replaceAll("\\s+", " ").trim();

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), newAddr);

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(newAddr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(newAddr)));
	}

	public static void parseLegal(ResultMap resultMap, String legal) {

		if (StringUtils.isEmpty(legal))
			return;

		String legalDes = legal;

		resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDes);

		Pattern LEGAL_LOT = Pattern.compile("LOT:?S? ?(\\d+)");
		Pattern LEGAL_BOOK = Pattern.compile("BO?O?K:? ?(\\d+)");
		Pattern LEGAL_PAGE = Pattern.compile("PAGE:? ?(\\d+)");
		Pattern LEGAL_BLOCK = Pattern.compile("BLOCK:? ?(\\d+)");
		Pattern LEGAL_SUBDIVISON = Pattern.compile("SUB DIVISON1:? ?([a-zA-Z0-9 ]+)");
		Pattern LEGAL_STR = Pattern.compile("SECT(\\w+) T(\\w+) R(\\w+)\\b");

		Matcher matcher = LEGAL_LOT.matcher(legalDes);

		// get lot
		StringBuffer lots = new StringBuffer();
		while (matcher.find()) {
			String lot = matcher.group(1);
			lot = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(lot).replace(" ", ", ").replaceAll("\\s+", " ");
			lots.append(StringUtils.strip(lot) + ", ");
			legalDes = legalDes.replaceFirst(matcher.group(), " ").replaceAll("\\s+", " ");
		}
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(),
				lots.toString().replaceAll("(,+)", ",").trim().replaceAll("(,$)|(^,)", "")
						.trim());

		// get book
		matcher = LEGAL_BOOK.matcher(legalDes);
		if (matcher.find()) {
			String book = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ").replaceAll("\\s+", " ");
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), book.trim());
		}

		// get page
		matcher = LEGAL_PAGE.matcher(legalDes);
		if (matcher.find()) {
			String page = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ").replaceAll("\\s+", " ");
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), page.trim());
		}

		// get block
		matcher = LEGAL_BLOCK.matcher(legalDes);
		if (matcher.find()) {
			String block = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ").replaceAll("\\s+", " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
		}

		boolean hasSubdName = false;

		// get subdivision
		matcher = LEGAL_SUBDIVISON.matcher(legalDes.split("\\bMAP\\b")[0]);
		if (matcher.find()) {
			String subdivision = matcher.group(1).trim();
			if (StringUtils.isNotEmpty(subdivision)) {
				legalDes = legalDes.replace(matcher.group(), " ").replaceAll("\\s+", " ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision.replaceAll("PLAT \\d+", "").trim());
				hasSubdName = true;
			}
		}

		// get str
		matcher = LEGAL_STR.matcher(legalDes.split("\\bMAP\\b")[0]);
		if (matcher.find()) {
			String sec = matcher.group(1);
			String twn = matcher.group(2);
			String rng = matcher.group(3);
			legalDes = legalDes.replace(matcher.group(), " ").replaceAll("\\s+", " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec.trim());
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), twn.trim());
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), rng.trim());
		}

		legalDes = legalDes.split("SUB DIVISON1:")[0].replaceAll("METES AND BOUNDS:", "").trim();

		if (StringUtils.isNotEmpty(legalDes) && !hasSubdName) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), legalDes);
		}
	}

	public static void parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap, HashMap<String, ResultMap> intermediaryData) {
		try {
			NodeList nodes = new HtmlParser3(detailsHtml.replaceAll("(?ism)&nbsp;", "")).getNodeList();

			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			String parcel = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "PARCEL #:"), "", false).replaceAll("\\s+", "")
					.replaceAll("[^\\d]", "");

			ResultMap intermediaryMap = intermediaryData.get(parcel);

			if (StringUtils.isNotEmpty(parcel) && intermediaryMap != null) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);

				// copy data from intermediary

				if (intermediaryMap.get(TaxHistorySetKey.BASE_AMOUNT.getKeyName()) != null) {
					resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), intermediaryMap.get(TaxHistorySetKey.BASE_AMOUNT.getKeyName()));
				}
				if (intermediaryMap.get(TaxHistorySetKey.AMOUNT_PAID.getKeyName()) != null) {
					resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), intermediaryMap.get(TaxHistorySetKey.AMOUNT_PAID.getKeyName()));
				}
				if (intermediaryMap.get(TaxHistorySetKey.TOTAL_DUE.getKeyName()) != null) {
					resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), intermediaryMap.get(TaxHistorySetKey.TOTAL_DUE.getKeyName()));
				}

				String year = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Tax Year:"), "", false).replaceAll("\\s+", "");

				if (StringUtils.isNotEmpty(year)) {
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
				}

				// get new data
				String owner = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(tables, "Current Owners"), "", false).trim()
						.replaceAll("\\s+", " ");

				ArrayList<String> names = new ArrayList<String>();
				
				if (StringUtils.isNotEmpty(owner)) {
					if (owner.contains(" CFD ")) {
						names.addAll(Arrays.asList(owner.split("\\bCFD\\b")));
					} else {
						names.add(owner);
					}

					parseNames(resultMap, names, "");
				} else {
					owner = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "OWNER:"), "", false).trim()
							.replaceAll("\\s+", " ");

					if (StringUtils.isNotEmpty(owner))
						names.add(owner);
				}

				String address = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "LOCATION:"), "", false).trim();

				if (StringUtils.isNotEmpty(address)) {
					address = address.split("(?ism) MONTGOMERY")[0].trim();
					
					if(address.contains("LLC")){
						names.add(address.split("LLC")[0] + " LLC");
						address = address.split("LLC")[1];
					}
					
					parseAddress(resultMap, address);
				}
				
				parseNames(resultMap, names, "");

				String land = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(tables, "Market Value"), "", false).trim();

				if (StringUtils.isNotEmpty(land)) {
					resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land.replaceAll("[ -,$]", ""));
				}

				String improvement = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "TOTAL IMP. VALUE"), "", false).trim();

				if (StringUtils.isNotEmpty(improvement)) {
					resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvement.replaceAll("[ -,$]", ""));
				}

				String marketValue = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "TOTAL MARKET VALUE:"), "", false).trim();

				if (StringUtils.isNotEmpty(marketValue)) {
					resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), marketValue.replaceAll("[ -,$]", ""));
				}

				String assessed = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(tables, "ASSD. VALUE:"), "", false).trim()
						.replaceAll("ASSD. VALUE:", "");

				if (StringUtils.isNotEmpty(assessed)) {
					resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), assessed.replaceAll("[ -,$]", ""));
				}

				String legal = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(tables, "METES AND BOUNDS:"), "", false).trim()
						.replaceAll("\\s+", " ").trim();

				String legalSubdivision = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(tables, "SUB DIVISON1:"), "", false).trim()
						.replaceAll("\\s+", " ").trim();
				String legalBookPage = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(tables, "MAP BOOK:"), "", false).trim()
						.replaceAll("\\s+", " ").trim();
				String legalLot = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(tables, "PRIMARY LOT:"), "", false).trim()
						.replaceAll("\\s+", " ").trim();
				String legalBlock = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(tables, "PRIMARY BLOCK:"), "", false).trim()
						.replaceAll("\\s+", " ").trim();

				legal += " " + legalSubdivision + " " + legalBookPage + " " + legalLot + " " + legalBlock;

				if (StringUtils.isNotEmpty(legal)) {
					parseLegal(resultMap, legal);
				}

				// references
				Node n = getNodeByTypeAttributeDescription(tables, "table", "", "", new String[] { "INSTRUMENT NUMBER", "DATE" }, false);

				if (n != null) {
					TableTag t = (TableTag) n;
					TableRow[] rows = t.getRows();

					@SuppressWarnings("rawtypes")
					List<List> body = new ArrayList<List>();
					List<String> line = new ArrayList<String>();

					for (int i = 1; i < rows.length; i++) {
						if (rows[i].getColumnCount() == 2) {
							String bp = rows[i].getColumns()[0].toPlainTextString();
							String date = rows[i].getColumns()[1].toPlainTextString();

							if (bp.contains("-")) {
								line = new ArrayList<String>();
								line.add(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(bp.split("-")[0]));
								line.add(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(bp.split("-")[1]));
								line.add(date);
								body.add(line);
							}
						}
					}

					if (body.size() > 0) {
						String[] header = { "Book", "Page", "Year" };
						ResultTable rt = GenericFunctions2.createResultTable(body, header);
						resultMap.put("CrossRefSet", rt);
					}
				}

				// sales
				NodeList auxNodes = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "TABLE3"));

				if (auxNodes.size() > 0) {
					n = getNodeByTypeAttributeDescription(auxNodes, "table", "", "", new String[] { "SALES HISTORY:" }, false);

					if (n != null) {

						TableTag t = (TableTag) n;
						TableRow[] rows = t.getRows();

						@SuppressWarnings("rawtypes")
						List<List> body = new ArrayList<List>();
						List<String> line = new ArrayList<String>();

						for (int i = 2; i < rows.length; i++) {
							if (rows[i].getColumnCount() == 5) {
								String date = rows[i].getColumns()[0].toPlainTextString().trim();
								String price = rows[i].getColumns()[1].toPlainTextString().trim();
								// String deed = rows[i].getColumns()[2].toPlainTextString().trim().replaceAll("^-$", "");
								String grantor = rows[i].getColumns()[3].toPlainTextString().trim();
								String grantee = rows[i].getColumns()[4].toPlainTextString().trim();

								line.add(date);
								line.add(price.replaceAll("[ -,$]", ""));
								// line.add(deed);
								line.add(grantor);
								line.add(grantee);
								body.add(line);
							}
						}

						if (body.size() > 0) {
							String[] header = { SaleDataSetKey.RECORDED_DATE.getShortKeyName(), SaleDataSetKey.SALES_PRICE.getShortKeyName(),
									SaleDataSetKey.GRANTOR.getShortKeyName(), SaleDataSetKey.GRANTEE.getShortKeyName() };
							ResultTable rt = GenericFunctions2.createResultTable(body, header);
							resultMap.put("SaleDataSet", rt);
						}
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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

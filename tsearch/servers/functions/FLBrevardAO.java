package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
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

/**
 * 
 * @author Oprina George
 * 
 *         Sep 25, 2012
 */

public class FLBrevardAO {

	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo) {
		ResultMap m = new ResultMap();
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");

		TableColumn[] cols = row.getColumns();

		if (cols.length == 4 || cols.length == 5) {
			String account = cols[0].toPlainTextString();
			String owner = cols[1].toPlainTextString();
			String addr = cols[2].toPlainTextString();
			String parcel = cols[3].toPlainTextString();

			if (StringUtils.isNotEmpty(parcel)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
			}
			if (StringUtils.isNotEmpty(account)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), account);
			}

			parseAddress(m, addr);

			if (StringUtils.isNotEmpty(owner)) {
				List<String> names = new ArrayList<String>();
				names.add(owner.replace("&amp;", "&"));// Arrays.asList(owner.split("&amp;")));
				parseNames(m, names, "");
			}

			if (cols.length == 5) {
				String pb_pp = cols[4].toPlainTextString();
				if (StringUtils.isNotBlank(pb_pp) && pb_pp.contains("/")) {
					m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(pb_pp.split("/")[0]));
					m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(pb_pp.split("/")[1]));
				}
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
				n = n.toUpperCase().trim().replaceAll("\\s+", " ")
						.replaceAll("H/W", "")
						.replaceAll("(C/O ?)+", "");

				if (StringUtils.isNotEmpty(n)) {
					nameOnServer.append(n + " & ");
					String[] names = new String[] { "", "", "", "", "", "" };
					if (n.contains("/") || n.contains("STATE")) {
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
			m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), StringUtils.strip(nameOnServer.toString()).replaceAll("\\&$", "").replace("_", "/"));
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

		String newAddr = addr.replaceAll("(?ism)UNIT ?CELLTW", "").replaceAll("(?ism)UNIT ?COMMON", "");

		if (addr.contains(",")) {
			newAddr = addr.split(",")[0];
			String cityZip = addr.split(",")[1];
			String city = cityZip.replaceAll("(.* )\\d+", "$1").trim();
			String zip = cityZip.replaceAll(city, "").trim();

			if (StringUtils.isNotEmpty(city))
				m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
			if (StringUtils.isNotEmpty(zip))
				m.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
		}

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(newAddr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(newAddr)));
	}

	public static void parseLegal(ResultMap m, String legal) {
		if (StringUtils.isEmpty(legal))
			return;

		String legalDes = legal;

		legalDes = legalDes.replace("Abbreviated Description", "")
				.replaceAll("[\r\t]", "")
				.replaceAll("\n", "%")
				.replaceAll(" *% *", "%")
				.replaceAll("%+", "%")
				.replaceAll("\\s+", " ").trim();

		m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDes.replaceAll("%", " ").replaceAll("\\s+", " "));

		Pattern LEGAL_LOT_INTERVAL = Pattern.compile("(?ism)\\bLOTS? (\\d+ TO \\d+),?&? ");
		Pattern LEGAL_LOT = Pattern.compile("\\b(LOTS?) (\\d+|\\w{1}),?&? ");
		Pattern LEGAL_BLK = Pattern.compile("\\bBLK ?((?:[A-Z]-?)?\\d*)");
		Pattern LEGAL_UNIT = Pattern.compile("\\bUNIT ?(\\d+)");
		Pattern LEGAL_PB_PP = Pattern.compile("(?ism)\\bPlat Book/Page:\\s?(\\d+)/(\\d+)");
		Pattern LEGAL_PB_PP_FROMLEGAL = Pattern.compile("(?ism)\\bDB ?(\\d+) ?PG ?(\\d+)");
		Pattern LEGAL_SUB_NAME = Pattern.compile("\\bSub Name:(.*?)(?:UNIT|%)");
		Pattern LEGAL_SECTION = Pattern.compile("(?i)\\bSEC\\b\\s*(\\d+)");
		
		// get section
		Matcher matcher = LEGAL_SECTION.matcher(legalDes);
		if (matcher.find()) {
			String section = matcher.group(1).trim();
			legalDes = legalDes.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
		}

		matcher = LEGAL_SUB_NAME.matcher(legalDes);
		if (matcher.find()) {
			String subdName = matcher.group(1);

			subdName = subdName.replaceAll("\\s+", " ").trim();
			
			if (subdName.indexOf("PH ") > -1 || subdName.indexOf("PHASE ") > -1) {
				String phase = subdName.replaceFirst("(?is).*\\bPH(?:ASE)?\\s*([A-Z\\d]+)", "$1");
				if (StringUtils.isNotBlank(phase)) {
					if (phase.matches("[A-Z]+")) {
						phase = GenericFunctions1.replaceOnlyNumbers(phase);
					}
					m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
					subdName = subdName.replaceFirst("(?is)(?:,?\\s*\\bTHE\\b)?\\s+\\bPH(?:ASE)?\\s*[\\dA-Z]+\\s*", "");
				}
			}
			
			if (subdName.contains(" ADDN") || subdName.contains(" ADDITION")) {
				subdName = subdName.replaceFirst("(?is)\\d+(?:ST|TH|ND|RD)(?:\\s*\\(?[A-Z\\s'-]+\\)?)?\\s+\\b(?:ADDN|ADDITION)", "");
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(subdName)) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdName.trim());
			}
		}

		// get lot
		matcher = LEGAL_LOT_INTERVAL.matcher(legalDes);
		StringBuffer lots = new StringBuffer();

		if (matcher.find()) {
			lots.append(matcher.group(1).replace("TO", "-").replaceAll(" ", ""));
			legalDes = legalDes.replaceFirst(matcher.group(), " ").replaceAll("\\s+", " ");
		}

		matcher = LEGAL_LOT.matcher(legalDes);

		for (int i = 0; matcher.find() && i < 5; i++) {
			String lot = matcher.group(2);
			if (StringUtils.isNotEmpty(lot)) {
				lot = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(lot.trim()).replace("&", " ").replaceAll("\\s+", " ");

				lots.append(" " + lot.trim()).append(" ");
			}
			legalDes = legalDes.replaceFirst(matcher.group(), matcher.group(1) + " ").replaceAll("\\s+", " ");
			matcher = LEGAL_LOT.matcher(legalDes);
		}

		legalDes = legalDes.replaceAll("(?ism)\\bLOTS?", "");

		m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots.toString().trim().replaceAll(",", ""));

		// get blk
		matcher = LEGAL_BLK.matcher(legalDes);
		if (matcher.find()) {
			String blk = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk.trim());
		}

		// get unit
		matcher = LEGAL_UNIT.matcher(legalDes);
		if (matcher.find()) {
			String unit = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit.trim());
		}

		// pb pp
		matcher = LEGAL_PB_PP.matcher(legalDes);

		String pb = "";
		String pp = "";

		if (matcher.find()) {
			pb = matcher.group(1);
			pp = matcher.group(2);

			legalDes = legalDes.replace(matcher.group(), " ");
		} else {
			matcher = LEGAL_PB_PP_FROMLEGAL.matcher(legalDes);
			if (matcher.find()) {
				pb = matcher.group(1);
				pp = matcher.group(2);

				legalDes = legalDes.replace(matcher.group(), " ");
			}
		}

		if (StringUtils.isNotEmpty(pb))
			m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(pb.trim()));
		if (StringUtils.isNotEmpty(pp))
			m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(pp.trim()));
	}

	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			detailsHtml = StringEscapeUtils.unescapeHtml(detailsHtml).replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			NodeList nodes = htmlParser3.getNodeList();

			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			String parcel = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Parcel ID:", true), "", false).trim();
			if (StringUtils.isNotEmpty(parcel)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
			}

			String taxAcct = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Tax ID:", true), "", false).trim();
			if (StringUtils.isNotEmpty(taxAcct)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), taxAcct);
			}

			String address = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Site Address:"), "", false).trim();

			parseAddress(resultMap, address);

			// extract table in center of page
			Node ownerInfoColumn = null;
			Node legalInfoColumn = null;
			Node valueSummaryRow = null;

			Node centerTable = htmlParser3.getNodeById("tOwnerInfo");
			if (centerTable != null && centerTable instanceof TableTag) {
				TableRow[] tableRows = ((TableTag) centerTable).getRows();
				if (tableRows.length > 0) {
					TableColumn[] firstRowCols = tableRows[0].getColumns();
					if (firstRowCols.length > 0) {
						ownerInfoColumn = (Node) firstRowCols[0];
						if (firstRowCols.length > 1) {
							legalInfoColumn = (Node) firstRowCols[1];
						}
					}
				}
				if (tableRows.length > 1) {
					valueSummaryRow = tableRows[1];
				}
			}
			if (ownerInfoColumn != null) {//get names
				NodeList ownerInfoTableChildren = ownerInfoColumn.getChildren();
				String owner1 = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(ownerInfoTableChildren, "Owner Name:", true), "", false)
						.trim();
				String owner2 = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(ownerInfoTableChildren, "Second Name:", true), "", false)
						.trim();
				String owner3 = "";
				if (StringUtils.isBlank(HtmlParser3
						.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(ownerInfoTableChildren, "Second Name:", true), "", false))) {
					owner3 = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(ownerInfoTableChildren, "Second Name:", true), "", false).trim();
				}

				List<String> names = new ArrayList<String>();

				if ("LLC".equals(owner2)) {
					if (StringUtils.isNotEmpty(owner1)) {
						names.add(owner1 + " & " + owner2);
					}
				} else {
					if (StringUtils.isNotEmpty(owner1)) {
						names.add(owner1);
					}
					if (StringUtils.isNotEmpty(owner2)) {
						names.add(owner2);
					}
				}

				if (StringUtils.isNotEmpty(owner3)) {
					names.add(owner3);
				}

				parseNames(resultMap, names, "");
			}

			// get legal
			if (legalInfoColumn != null) {
				String legal = ((TableColumn) legalInfoColumn).getChild(1).toPlainTextString();
				parseLegal(resultMap, legal);
			}

			// get values
			if (valueSummaryRow != null) {
				NodeList valueSummaryRowChildren = valueSummaryRow.getChildren();
				String marketValue = HtmlParser3
						.getValueFromAbsoluteCell(0, 3, HtmlParser3.findNode(valueSummaryRowChildren, "Market Value Total:"), "", false)
						.replaceAll("[^\\d.]", "");
				String landValue = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(valueSummaryRowChildren, "Land Value:"), "", false)
						.replaceAll("[^\\d.]", "");

				if (StringUtils.isNotEmpty(marketValue)) {
					resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), marketValue);
				}

				if (StringUtils.isNotEmpty(landValue)) {
					resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), landValue);
				}

			}
			
			//get related docs
			NodeList salesInfo = nodes.extractAllNodesThatMatch(new RegexFilter("Sale Information"), true);
			if (salesInfo.size() > 0) {
				Node saleTable = HtmlParser3.getFirstParentTag(salesInfo.elementAt(0), TableTag.class);
				if (saleTable != null) {
					TableRow[] rows = ((TableTag) saleTable).getRows();

					NodeList aux = new NodeList();

					String[] salesHeader = { SaleDataSetKey.RECORDED_DATE.getShortKeyName(),
							SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(),
							SaleDataSetKey.BOOK.getShortKeyName(),
							SaleDataSetKey.PAGE.getShortKeyName(),
							SaleDataSetKey.SALES_PRICE.getShortKeyName() };
					List<List<String>> salesBody = new ArrayList<List<String>>();
					List<String> salesRow = new ArrayList<String>();
					ResultTable resT = new ResultTable();
					Map<String, String[]> salesMap = new HashMap<String, String[]>();
					for (String s : salesHeader) {
						salesMap.put(s, new String[] { s, "" });
					}

					for (int i = 1; i < rows.length; i++) {
						TableRow r = rows[i];
						if (r.getColumnCount() == 8) {
							
							// parse related docs
							salesRow = new ArrayList<String>();

							String bp = r.getColumns()[0].toPlainTextString();
							String recDate = r.getColumns()[1].toPlainTextString();
							String saleAmount = r.getColumns()[2].toPlainTextString();
							String type = r.getColumns()[3].toPlainTextString();

							String book = "";
							String page = "";

							if (bp.contains("/")) {
								book = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(bp.split("/")[0]);
								page = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(bp.split("/")[1]);
							}

							salesRow.add(StringUtils.defaultString(recDate).trim());
							salesRow.add(StringUtils.defaultString(type).trim());
							salesRow.add(StringUtils.defaultString(book).trim());
							salesRow.add(StringUtils.defaultString(page).trim());
							salesRow.add(StringUtils.defaultString(saleAmount).replaceAll("[^\\d.]", ""));

							salesBody.add(salesRow);
							aux = new NodeList();
						} else {
							aux.add(r);
						}
					}

					if (salesBody.size() > 0) {
						resT.setHead(salesHeader);
						resT.setMap(salesMap);
						resT.setBody(salesBody);
						resT.setReadOnly();
						resultMap.put("SaleDataSet", resT);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}

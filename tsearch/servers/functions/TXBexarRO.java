package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.TableColumn;
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
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

/**
 * 
 * @author Oprina George
 * 
 *         Oct 14, 2011
 */
public class TXBexarRO {

	public static HashMap<String, String> getFormParams(String resp, String name) {
		HashMap<String, String> params = new HashMap<String, String>();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
			NodeList nodeList = htmlParser.parse(null);

			nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("name", name), true);

			if (nodeList.size() == 1) {
				NodeList children = nodeList.elementAt(0).getChildren();

				for (int i = 0; i < children.size(); i++) {
					if (children.elementAt(i) instanceof InputTag) {
						String key = ((InputTag) children.elementAt(i)).getAttribute("name");
						String val = ((InputTag) children.elementAt(i)).getAttribute("value");
						params.put(key, val);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return params;
	}

	public static ResultMap parseIntermediaryRow(TableRow row, int parseID) {

		ResultMap m = new ResultMap();

		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");

		TableColumn[] cols = row.getColumns();

		if (cols.length == 15 && parseID == 1) {
			String gtr_gte = StringUtils.strip(cols[3].toPlainTextString().replaceAll("\\s+", " "));

			String grantor = "";
			String grantee = "";
			// parse names
			if ("GTR".equalsIgnoreCase(gtr_gte)) {
				grantor = StringUtils.strip(cols[2].toPlainTextString().replaceAll("\\s+", " "));
				grantee = StringUtils.strip(cols[9].toPlainTextString().replaceAll("\\s+", " "));
			} else if ("GTE".equalsIgnoreCase(gtr_gte)) {
				grantor = StringUtils.strip(cols[9].toPlainTextString().replaceAll("\\s+", " "));
				grantee = StringUtils.strip(cols[2].toPlainTextString().replaceAll("\\s+", " "));
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantor)) {
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);

				m.put("tmpName", grantor);
				m.put("tmpSetName", "GrantorSet");

				parseNames(m);
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantee)) {
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);

				m.put("tmpName", grantee);
				m.put("tmpSetName", "GranteeSet");

				parseNames(m);
			}

			String doc_no = StringUtils.strip(cols[4].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(doc_no))
				m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), doc_no);

			String recorded_date = StringUtils.strip(cols[5].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(recorded_date))
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recorded_date);

			String[] book_page = cols[7].toHtml().split("(?ism)<br[^>]*>");

			if (book_page.length > 1) {
				String book = StringUtils.strip(book_page[0].replaceAll("<[^>]*>", ""));
				String page = StringUtils.strip(book_page[1].replaceAll("<[^>]*>", ""));

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book))
					m.put(SaleDataSetKey.BOOK.getKeyName(), book);

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(page))
					m.put(SaleDataSetKey.PAGE.getKeyName(), page);
			}

			String instrument_type = StringUtils.strip(cols[8].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_type))
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instrument_type);

			String lots = StringUtils.strip(cols[10].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(lots))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots);

			String block = StringUtils.strip(cols[11].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(block))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);

			String subdivision = StringUtils.strip(cols[14].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(subdivision))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);

		} else if (parseID == 5) {
			String grantor = StringUtils.strip(cols[7].toPlainTextString().replaceAll("\\s+", " "));
			String grantee = StringUtils.strip(cols[8].toPlainTextString().replaceAll("\\s+", " "));

			// parse names
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantor)) {
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);

				m.put("tmpName", grantor);
				m.put("tmpSetName", "GrantorSet");

				parseNames(m);
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantee)) {
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);

				m.put("tmpName", grantee);
				m.put("tmpSetName", "GranteeSet");

				parseNames(m);
			}

			String doc_no = StringUtils.strip(cols[2].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(doc_no))
				m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), doc_no);

			String recorded_date = StringUtils.strip(cols[3].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(recorded_date))
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recorded_date);

			String[] book_page = cols[6].toHtml().split("(?ism)<br[^>]*>");

			if (book_page.length > 1) {
				String book = StringUtils.strip(book_page[0].replaceAll("<[^>]*>", ""));
				String page = StringUtils.strip(book_page[1].replaceAll("<[^>]*>", ""));

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book))
					m.put(SaleDataSetKey.BOOK.getKeyName(), book);

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(page))
					m.put(SaleDataSetKey.PAGE.getKeyName(), page);
			}

			String instrument_type = StringUtils.strip(cols[4].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_type))
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instrument_type);

			String lots = StringUtils.strip(cols[9].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(lots))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots);

			String block = StringUtils.strip(cols[10].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(block))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);

			String subdivision = StringUtils.strip(cols[13].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(subdivision))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
		} else if (parseID == 8) {
			String grantor = StringUtils.strip(cols[7].toPlainTextString().replaceAll("\\s+", " "));
			String grantee = StringUtils.strip(cols[8].toPlainTextString().replaceAll("\\s+", " "));

			// parse names
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantor)) {
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);

				m.put("tmpName", grantor);
				m.put("tmpSetName", "GrantorSet");

				parseNames(m);
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantee)) {
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);

				m.put("tmpName", grantee);
				m.put("tmpSetName", "GranteeSet");

				parseNames(m);
			}

			String doc_no = StringUtils.strip(cols[6].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(doc_no))
				m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), doc_no);

			String recorded_date = StringUtils.strip(cols[4].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(recorded_date))
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recorded_date);

			String[] book_page = cols[2].toHtml().split("(?ism)<br[^>]*>");

			if (book_page.length > 1) {
				String book = StringUtils.strip(book_page[0].replaceAll("<[^>]*>", ""));
				String page = StringUtils.strip(book_page[1].replaceAll("<[^>]*>", ""));

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book))
					m.put(SaleDataSetKey.BOOK.getKeyName(), book);

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(page))
					m.put(SaleDataSetKey.PAGE.getKeyName(), page);
			}

			String instrument_type = StringUtils.strip(cols[5].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_type))
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instrument_type);

			String lots = StringUtils.strip(cols[9].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(lots))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots);

			String block = StringUtils.strip(cols[10].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(block))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);

			String subdivision = StringUtils.strip(cols[13].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(subdivision))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
		} else if (parseID == 9) {
			String grantor = StringUtils.strip(cols[12].toPlainTextString().replaceAll("\\s+", " "));
			String grantee = StringUtils.strip(cols[13].toPlainTextString().replaceAll("\\s+", " "));

			// parse names
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantor)) {
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);

				m.put("tmpName", grantor);
				m.put("tmpSetName", "GrantorSet");

				parseNames(m);
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantee)) {
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);

				m.put("tmpName", grantee);
				m.put("tmpSetName", "GranteeSet");

				parseNames(m);
			}

			String doc_no = StringUtils.strip(cols[9].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(doc_no))
				m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), doc_no);

			String[] book_page = cols[11].toHtml().split("(?ism)<br[^>]*>");

			if (book_page.length > 1) {
				String book = StringUtils.strip(book_page[0].replaceAll("<[^>]*>", ""));
				String page = StringUtils.strip(book_page[1].replaceAll("<[^>]*>", ""));

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book))
					m.put(SaleDataSetKey.BOOK.getKeyName(), book);

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(page))
					m.put(SaleDataSetKey.PAGE.getKeyName(), page);
			}

			String instrument_type = StringUtils.strip(cols[8].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_type))
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instrument_type);

			String lots = StringUtils.strip(cols[2].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(lots))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots);

			String block = StringUtils.strip(cols[3].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(block))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);

			String subdivision = StringUtils.strip(cols[6].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(subdivision))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
		} else if (parseID == 18) {
			String grantor = StringUtils.strip(cols[7].toPlainTextString().replaceAll("\\s+", " "));
			String grantee = StringUtils.strip(cols[8].toPlainTextString().replaceAll("\\s+", " "));

			// parse names
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantor)) {
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);

				m.put("tmpName", grantor);
				m.put("tmpSetName", "GrantorSet");

				parseNames(m);
			}

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantee)) {
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);

				m.put("tmpName", grantee);
				m.put("tmpSetName", "GranteeSet");

				parseNames(m);
			}

			String doc_no = StringUtils.strip(cols[4].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(doc_no))
				m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), doc_no);

			String recorded_date = StringUtils.strip(cols[3].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(recorded_date))
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recorded_date);

			String[] book_page = cols[6].toHtml().split("(?ism)<br[^>]*>");

			if (book_page.length > 1) {
				String book = StringUtils.strip(book_page[0].replaceAll("<[^>]*>", ""));
				String page = StringUtils.strip(book_page[1].replaceAll("<[^>]*>", ""));

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book))
					m.put(SaleDataSetKey.BOOK.getKeyName(), book);

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(page))
					m.put(SaleDataSetKey.PAGE.getKeyName(), page);
			}

			String instrument_type = StringUtils.strip(cols[2].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_type))
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instrument_type);

			String lots = StringUtils.strip(cols[9].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(lots))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots);

			String block = StringUtils.strip(cols[10].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(block))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);

			String subdivision = StringUtils.strip(cols[13].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(subdivision))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
		}

		m.removeTempDef();

		return m;

	}

	public static void parseNames(ResultMap m) {
		// all names are LCFM
		String name = (String) m.get("tmpName");

		String setName = (String) m.get("tmpSetName");

		name = name.toUpperCase();

		try {
			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();

			String[] all_names = name.split("&&&");

			for (String n : all_names) {
				n = StringUtils.strip(n);
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(n)) {
					String[] names = StringFormats.parseNameNashville(n, true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(n, names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
			}
			if (body.size() > 0)
				m.put(setName, GenericFunctions.storeOwnerInSet(body, true));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseLegal(ResultMap m) {

	}

	public static void parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map, long searchId) {
		try {
			NodeList nodes = new HtmlParser3(detailsHtml).getNodeList();

			// general
			String doc_no = StringUtils.defaultString(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodes, "Document Number:"), "", true));
			if (StringUtils.isNotEmpty(doc_no)){
				map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), doc_no);
			}

			String book = StringUtils.defaultString(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodes, "Book #:"), "", true));
			if (StringUtils.isNotEmpty(book))
				map.put(SaleDataSetKey.BOOK.getKeyName(), book);

			String page = StringUtils.defaultString(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodes, "Page #:"), "", true));
			if (StringUtils.isNotEmpty(page))
				map.put(SaleDataSetKey.PAGE.getKeyName(), page);

			String instr_type = StringUtils.defaultString(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodes, "Instrument Type:"), "", true));
			if (StringUtils.isNotEmpty(instr_type))
				map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instr_type.trim());

			String considetation_amt = StringUtils.defaultString(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodes, "Consideration Amt:"), "", true));
			if (StringUtils.isNotEmpty(considetation_amt))
				map.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), considetation_amt.replaceAll("[\\s$,-]", ""));

			String filled_date = StringUtils.defaultString(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodes, "Filed Date:"), "", true));
			if (StringUtils.isNotEmpty(filled_date))
				map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), filled_date);

			String instrument_date = StringUtils.defaultString(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodes, "Instrument Date:"), "", true));
			if (StringUtils.isNotEmpty(instrument_date))
				map.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrument_date);

			// legal
			String subdivision = StringUtils.defaultString(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodes, "Subdivision:"), "", true));
			if (StringUtils.isNotEmpty(subdivision))
				map.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);

			String plat_book = StringUtils.defaultString(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodes, "Plat Book:"), "", true));
			plat_book = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(plat_book);
			if (StringUtils.isNotEmpty(plat_book))
				map.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), plat_book);

			String plat_page = StringUtils.defaultString(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodes, "Plat Page:"), "", true));
			plat_page = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(plat_page);
			if (StringUtils.isNotEmpty(plat_page))
				map.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), plat_page);
			else {
				if(StringUtils.countMatches(detailsHtml,"Plat Book:") == 2 ){
					NodeList nodes1 = new HtmlParser3(detailsHtml.replaceFirst("Plat Book:", "")).getNodeList();
					String plat_page1 = StringUtils.defaultString(HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodes1, "Plat Book:"), "", true));
					plat_page1 = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(plat_page1);
					if (StringUtils.isNotEmpty(plat_page1))
						map.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), plat_page1);
				}
			}
			
			String lot = StringUtils.defaultString(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "Lot"), "", true));
			if (StringUtils.isNotEmpty(lot))
				map.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.replace("N/A", "").replaceAll("<[^>]*>", "").trim());

			String block = StringUtils.defaultString(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "Block"), "", true));
			if (StringUtils.isNotEmpty(block))
				map.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.replace("N/A", "").replaceAll("<[^>]*>", "").trim());

			NodeList tables = nodes.extractAllNodesThatMatch(new HasAttributeFilter("id", "resultsDetail"), true);

			if (tables.size() != 1)
				return;

			TableTag resultDetail = (TableTag) tables.elementAt(0);

			TableRow[] rows = resultDetail.getRows();

			NodeList allTables = null;

			if (rows.length == 1 && rows[0].getColumnCount() == 1) {
				allTables = rows[0].getColumns()[0].getChildren().extractAllNodesThatMatch(new HasAttributeFilter("table"), true);
				allTables = allTables.extractAllNodesThatMatch(new HasAttributeFilter("style", "background-color:White;"), true);

				if (allTables.size() > 0) {
					TableTag t = ((TableTag) allTables.elementAt(0));
					if (t.getRowCount() > 1) {
						allTables = t.getRow(1).getColumns()[0].getChildren().extractAllNodesThatMatch(new HasAttributeFilter("table"));
					}
				}
			}

			if (allTables == null)
				return;

			// get grantor table

			TableTag grantorTable = null;

			for (int i = 0; i < allTables.size(); i++) {
				if (allTables.elementAt(i).toHtml().contains("Grantor") && i + 1 < allTables.size()) {
					grantorTable = (TableTag) allTables.elementAt(i + 1);
					break;
				}
			}

			if (grantorTable != null) {
				// parse names
				StringBuffer grantors = new StringBuffer();
				StringBuffer grantees = new StringBuffer();

				TableTag grantorsT = null;
				TableTag granteesT = null;

				if (grantorTable.getRowCount() == 1 && grantorTable.getRow(0).getColumnCount() == 2) {
					try {
						grantorsT = (TableTag) grantorTable.getRow(0).getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("table"))
								.elementAt(0);
					} catch (Exception e) {
						e.printStackTrace();
					}

					try {
						granteesT = (TableTag) grantorTable.getRow(0).getColumns()[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("table"))
								.elementAt(0);
					} catch (Exception e) {
						e.printStackTrace();
					}

				}

				if (grantorsT != null) {
					for (TableRow r : grantorsT.getRows()) {
						grantors.append(StringUtils.strip(r.toPlainTextString().replace("&nbsp;", " ").replaceAll("\\s+", " ")) + " &&& ");
					}
				}

				if (granteesT != null) {
					for (TableRow r : granteesT.getRows()) {
						grantees.append(StringUtils.strip(r.toPlainTextString().replace("&nbsp;", " ").replaceAll("\\s+", " ")) + " &&& ");
					}
				}

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantors.toString())) {
					map.put(SaleDataSetKey.GRANTOR.getKeyName(),
							grantors.toString().replace("&&&", "&").substring(0, grantors.toString().replace("&&&", "&").lastIndexOf("&")));

					map.put("tmpName", grantors.toString());
					map.put("tmpSetName", "GrantorSet");

					parseNames(map);
				}

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantees.toString())) {
					map.put(SaleDataSetKey.GRANTEE.getKeyName(),
							grantees.toString().replace("&&&", "&").substring(0, grantees.toString().replace("&&&", "&").lastIndexOf("&")));

					map.put("tmpName", grantees.toString());
					map.put("tmpSetName", "GranteeSet");

					parseNames(map);
				}
			}

			// address
			TableTag addressTable = null;

			for (int i = 0; i < allTables.size(); i++) {
				if (allTables.elementAt(i).toHtml().contains("Property Address") && i + 1 < allTables.size()) {
					addressTable = (TableTag) allTables.elementAt(i + 1);
					if (!addressTable.toHtml().contains("Address 2:"))
						addressTable = null;
					break;
				}
			}

			if (addressTable != null) {
				// parse address
				String address = StringUtils.defaultString(HtmlParser3.getValueFromAbsoluteCell(0, 1,
						HtmlParser3.findNode(addressTable.getChildren(), "Address 2:"), "", true)).replace("N/A", "");

				if (StringUtils.isNotEmpty(address)) {
					parseAddress(map, address);
				}

				String city = StringUtils.defaultString(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(addressTable.getChildren(), "City:"),
						"", true));
				if (StringUtils.isNotEmpty(city))
					map.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.replace("N/A", "").replaceAll("<[^>]*>", "").trim());

				String zip = StringUtils.defaultString(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(addressTable.getChildren(), "Zip:"), "",
						true));
				if (StringUtils.isNotEmpty(zip))
					map.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip.replace("N/A", "").replaceAll("<[^>]*>", "").trim());
			}

			// related doc
			TableTag relatedDocTable = getTableFromNodeList(allTables, new String[] { "Related Doc No", "Related Instr Type", "Related Book No",
					"Related Page No" });

			if (relatedDocTable != null) {
				TableTag crossref_table = relatedDocTable;
				TableRow[] crossRows = crossref_table.getRows();

				@SuppressWarnings("rawtypes")
				List<List> bodyCR = new ArrayList<List>();
				List<String> line = new ArrayList<String>();

				for (int j = 1; j < crossRows.length; j++) {
					if (crossRows[j].getColumnCount() == 6) {
						line = new ArrayList<String>();
						line.add(crossRows[j].getColumns()[0].toPlainTextString());
						line.add(crossRows[j].getColumns()[1].toPlainTextString());
						line.add(crossRows[j].getColumns()[2].toPlainTextString());
						line.add(crossRows[j].getColumns()[3].toPlainTextString());
						bodyCR.add(line);
					}
				}

				if (bodyCR.size() > 0) {
					String[] header = { "InstrumentNumber", "Instrument_Ref_Type", "Book", "Page" };
					ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
					map.put("CrossRefSet", rt);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return;
	}

	public static void parseAddress(ResultMap m, String addr) {
		addr = StringUtils.strip(StringUtils.defaultString(addr));

		if (ro.cst.tsearch.utils.StringUtils.isEmpty(addr))
			return;

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr);

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(addr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(addr)));
	}

	public static TableTag getTableFromNodeList(NodeList nodes, String[] contains) {
		TableTag t = null;
		nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);
		for (int i = 0; i < nodes.size(); i++) {
			boolean is = true;
			for (String s : contains) {
				if (!nodes.elementAt(i).toHtml().contains(s))
					is = false;
			}
			if (is)
				t = (TableTag) nodes.elementAt(i);
		}
		return t;
	}

	public static Node getNodeByTypeAttributeDescription(NodeList nl, String type, String attributeName, String attributeValue, String description,
			boolean recursive) {
		NodeList returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
				new HasAttributeFilter(attributeName, attributeValue), recursive);

		for (int i = 0; i < returnList.size(); i++)
			if (returnList.elementAt(i).toHtml().contains(description))
				return returnList.elementAt(i);

		return null;
	}
}

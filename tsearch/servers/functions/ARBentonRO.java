package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
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
 *         Jul 25, 2011
 */

public class ARBentonRO {

	public static ResultMap parseIntermediaryRow(TableRow row, int parseID) {

		ResultMap m = new ResultMap();

		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");

		TableColumn[] cols = row.getColumns();

		if (cols.length == 13) {
			String gtr_gte = StringUtils.strip(cols[3].toPlainTextString()
					.replaceAll("\\s+", ""));

			String grantor = "";
			String grantee = "";

			if (gtr_gte.equalsIgnoreCase("GTR")) {
				grantor = StringUtils.strip(cols[2].toPlainTextString()
						.replaceAll("\\s+", " "));

				grantee = StringUtils.strip(cols[9].toPlainTextString()
						.replaceAll("\\s+", " "));
			} else {
				grantor = StringUtils.strip(cols[9].toPlainTextString()
						.replaceAll("\\s+", " "));

				grantee = StringUtils.strip(cols[2].toPlainTextString()
						.replaceAll("\\s+", " "));
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

			// String book_type =
			// StringUtils.strip(cols[4].toPlainTextString());
			// if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book_type))
			// m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), book_type);

			String[] book_page = cols[5].toHtml().split("(?ism)<br[^>]*>");

			if (book_page.length > 1) {
				String book = StringUtils.strip(book_page[0].replaceAll(
						"<[^>]*>", ""));
				String page = StringUtils.strip(book_page[1].replaceAll(
						"<[^>]*>", ""));

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book))
					m.put(SaleDataSetKey.BOOK.getKeyName(), book);

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(page))
					m.put(SaleDataSetKey.PAGE.getKeyName(), page);
			}

			String recorded_date = StringUtils.strip(cols[6]
					.toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(recorded_date))
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recorded_date);

			String instrument_type = StringUtils.strip(cols[7]
					.toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_type))
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(),
						instrument_type);

			String instrument_date = StringUtils.strip(cols[8]
					.toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_date))
				m.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(),
						instrument_date);

			String amount = StringUtils.strip(cols[10].toPlainTextString()
					.replaceAll("[$,-]", ""));

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(amount)) {
				m.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount);
				m.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
			}

			String legal = StringUtils.strip(cols[11].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(legal))
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION
						.getKeyName(), legal);

			// String case_no = StringUtils.strip(cols[12].toPlainTextString());
		}

		if (cols.length == 12 && parseID == 8) { // ID_SEARCH_BY_BOOK_AND_PAGE
			String grantor = StringUtils.strip(cols[7].toPlainTextString()
					.replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantor)) {
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);

				m.put("tmpName", grantor);
				m.put("tmpSetName", "GrantorSet");

				parseNames(m);
			}

			// String book_type =
			// StringUtils.strip(cols[2].toPlainTextString());
			// if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book_type))
			// m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), book_type);

			String[] book_page = cols[3].toHtml().split("(?ism)<br[^>]*>");

			if (book_page.length > 1) {
				String book = StringUtils.strip(book_page[0].replaceAll(
						"<[^>]*>", ""));
				String page = StringUtils.strip(book_page[1].replaceAll(
						"<[^>]*>", ""));

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book))
					m.put(SaleDataSetKey.BOOK.getKeyName(), book);

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(page))
					m.put(SaleDataSetKey.PAGE.getKeyName(), page);
			}

			String recorded_date = StringUtils.strip(cols[4]
					.toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(recorded_date))
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recorded_date);

			String instrument_type = StringUtils.strip(cols[6]
					.toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_type))
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(),
						instrument_type);

			String instrument_date = StringUtils.strip(cols[5]
					.toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_date))
				m.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(),
						instrument_date);

			String grantee = StringUtils.strip(cols[8].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantee)) {
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);

				m.put("tmpName", grantee);
				m.put("tmpSetName", "GranteeSet");

				parseNames(m);
			}

			String amount = StringUtils.strip(cols[9].toPlainTextString()
					.replaceAll("[$,-]", ""));

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(amount)) {
				m.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount);
				m.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
			}

			String legal = StringUtils.strip(cols[10].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(legal))
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION
						.getKeyName(), legal);

			// String case_no = StringUtils.strip(cols[11].toPlainTextString());
		}

		if (cols.length == 12 && parseID == 18) { // ID_SEARCH_BY_SALES
			String grantor = StringUtils.strip(cols[7].toPlainTextString()
					.replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantor)) {
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);

				m.put("tmpName", grantor);
				m.put("tmpSetName", "GrantorSet");

				parseNames(m);
			}

			// String book_type =
			// StringUtils.strip(cols[4].toPlainTextString());
			// if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book_type))
			// m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), book_type);

			String[] book_page = cols[5].toHtml().split("(?ism)<br[^>]*>");

			if (book_page.length > 1) {
				String book = StringUtils.strip(book_page[0].replaceAll(
						"<[^>]*>", ""));
				String page = StringUtils.strip(book_page[1].replaceAll(
						"<[^>]*>", ""));

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book))
					m.put(SaleDataSetKey.BOOK.getKeyName(), book);

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(page))
					m.put(SaleDataSetKey.PAGE.getKeyName(), page);
			}

			String recorded_date = StringUtils.strip(cols[2]
					.toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(recorded_date))
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recorded_date);

			String instrument_type = StringUtils.strip(cols[3]
					.toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_type))
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(),
						instrument_type);

			String instrument_date = StringUtils.strip(cols[6]
					.toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_date))
				m.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(),
						instrument_date);

			String grantee = StringUtils.strip(cols[8].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantee)) {
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);

				m.put("tmpName", grantee);
				m.put("tmpSetName", "GranteeSet");

				parseNames(m);
			}

			String amount = StringUtils.strip(cols[9].toPlainTextString()
					.replaceAll("[$,-]", ""));

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(amount)) {
				m.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount);
				m.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
			}

			String legal = StringUtils.strip(cols[10].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(legal))
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION
						.getKeyName(), legal);

			// String case_no = StringUtils.strip(cols[11].toPlainTextString());
		}

		if (cols.length == 15) {
			String lots = StringUtils.strip(cols[2].toPlainTextString()
					.replaceAll("\\s+", " "));

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(lots))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
						.getKeyName(), lots);

			String block = StringUtils.strip(cols[3].toPlainTextString()
					.replaceAll("\\s+", " "));

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(block))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK
						.getKeyName(), block);

			String subdivision = StringUtils.strip(cols[4].toPlainTextString()
					.replaceAll("\\s+", " "));

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(subdivision))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME
						.getKeyName(), subdivision);

			String city = StringUtils.strip(cols[5].toPlainTextString()
					.replaceAll("\\s+", " "));

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(city))
				m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);

			String section = StringUtils.strip(cols[6].toPlainTextString()
					.replaceAll("\\s+", " "));

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(section))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION
						.getKeyName(), section);

			String twn = StringUtils.strip(cols[7].toPlainTextString()
					.replaceAll("\\s+", " "));

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(twn))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP
						.getKeyName(), twn);

			String range = StringUtils.strip(cols[8].toPlainTextString()
					.replaceAll("\\s+", " "));

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(range))
				m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE
						.getKeyName(), range);

			// String book_type =
			// StringUtils.strip(cols[9].toPlainTextString());
			// if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book_type))
			// m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), book_type);

			String[] book_page = cols[10].toHtml().split("(?ism)<br[^>]*>");

			if (book_page.length > 1) {
				String book = StringUtils.strip(book_page[0].replaceAll(
						"<[^>]*>", ""));
				String page = StringUtils.strip(book_page[1].replaceAll(
						"<[^>]*>", ""));

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book))
					m.put(SaleDataSetKey.BOOK.getKeyName(), book);

				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(page))
					m.put(SaleDataSetKey.PAGE.getKeyName(), page);
			}

			String recorded_date = StringUtils.strip(cols[11]
					.toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(recorded_date))
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recorded_date);

			String grantor = StringUtils.strip(cols[12].toPlainTextString()
					.replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantor)) {
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);

				m.put("tmpName", grantor);
				m.put("tmpSetName", "GrantorSet");

				parseNames(m);
			}

			String grantee = StringUtils.strip(cols[13].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantee)) {
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), grantee);

				m.put("tmpName", grantee);
				m.put("tmpSetName", "GranteeSet");

				parseNames(m);
			}

			String instrument_type = StringUtils.strip(cols[14]
					.toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_type))
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(),
						instrument_type);
		}

		m.removeTempDef();

		String doc_type = (String) m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName());

		if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(doc_type))
			m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(),
					doc_type.replaceAll(" ", ""));

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
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(n)
						&& !n.contains("NOT GIVEN")
						&& !n.matches("S\\d+T\\d+R\\d+")) {
					String[] names = StringFormats.parseNameNashville(n, true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions
							.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions
							.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(n, names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
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
		String legal = (String) m.get("tmpLegal");

		if (ro.cst.tsearch.utils.StringUtils.isEmpty(legal))
			return;

		if (ro.cst.tsearch.utils.StringUtils.isEmpty((String) m
				.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
						.getKeyName()))) {
			Pattern lot_pattern = Pattern
					.compile("(?ism)LOTS?(\\s[\\d][\\w]*+)");

			Matcher mat = lot_pattern.matcher(legal);

			while (mat.find()) {
				String lot_aux = (String) m
						.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
								.getKeyName());

				if (ro.cst.tsearch.utils.StringUtils.isEmpty(lot_aux))
					lot_aux = "";

				String lot = mat.group(1);

				legal = legal.replaceFirst(lot, "");

				mat = lot_pattern.matcher(legal);

				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
						.getKeyName(), lot_aux + lot);
			}
		}
		if (ro.cst.tsearch.utils.StringUtils.isEmpty((String) m
				.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK
						.getKeyName()))) {
			Pattern block_pattern = Pattern.compile("(?ism)BLK\\s+([\\w]+)");

			Matcher mat = block_pattern.matcher(legal);

			if (mat.find()) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
						.getKeyName(), mat.group(1));
				legal = legal.replace(mat.group(), "");
			}
		}

		Pattern phase_pattern = Pattern.compile("(?ism)PHASE\\s+([\\w]+)");

		Matcher mat_phase = phase_pattern.matcher(legal);

		if (mat_phase.find()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(),
					mat_phase.group(1));
			legal = legal.replace(mat_phase.group(), "");
		}

		Pattern unit_pattern = Pattern.compile("(?ism)UNIT\\s+([\\w]+)");

		Matcher mat_unit = unit_pattern.matcher(legal);

		if (mat_unit.find()) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(),
					mat_unit.group(1));
			legal = legal.replace(mat_unit.group(), "");
		}

	}

	public static void parseSaleDataSet(ResultMap m) {

	}

	public static HashMap<String, String> getParams(String resp, String formName) {
		HashMap<String, String> params = new HashMap<String, String>();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(resp, null);
			NodeList nodeList = htmlParser.parse(null);

			nodeList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("form"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("name", formName), true);

			if (nodeList.size() == 1) {
				NodeList children = nodeList.elementAt(0).getChildren();

				for (int i = 0; i < children.size(); i++) {
					if (children.elementAt(i) instanceof InputTag) {
						String key = ((InputTag) children.elementAt(i))
								.getAttribute("name");
						String val = ((InputTag) children.elementAt(i))
								.getAttribute("value");
						params.put(key, val);
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return params;
	}

	public static void parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap map, long searchId) {
		try {
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml.replace("&nbsp;", " "), null);
			NodeList nodeList = htmlParser.parse(null);

			// general info
			// String instr_no = "";

			String instrument_type = HtmlParser3
					.getValueFromNextCell(
							HtmlParser3.findNode(nodeList, "Instrument Type"),
							"", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_type)) {
				map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(),
						instrument_type.replace(" ", ""));
				// instr_no += instrument_type;
			}

			String recorded_date = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Filed Date:"), "", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(recorded_date)) {
				map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(),
						recorded_date);
			}

			String book_no = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Book #:"), "", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book_no)) {
				map.put(SaleDataSetKey.BOOK.getKeyName(), book_no);
				// instr_no += book_no;
			}

			String book_page = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Page #:"), "", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(book_page)) {
				map.put(SaleDataSetKey.PAGE.getKeyName(), book_page);
				// instr_no += book_page;
			}

			// if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instr_no)) {
			// map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
			// instr_no);
			// }

			String instrument_date = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Instrument Date:"), "",
					true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_date)) {
				map.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(),
						instrument_date);
			}

			String amount = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Amount:"), "", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(amount)) {
				map.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(),
						amount.replaceAll("[$,-]", ""));
				map.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(),
						amount.replaceAll("[$,-]", ""));
			}

			// grantee grantor
			TableTag main_table = null;

			NodeList n = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("style", "background-color:White;"),
					true);

			if (n != null)
				main_table = (TableTag) n.elementAt(0);

			if (main_table != null && main_table.getRows().length == 3) {
				TableRow r = main_table.getRow(1);

				NodeList tables = r.getChild(0).getChildren()
						.extractAllNodesThatMatch(new TagNameFilter("table"));

				// grantee grantor

				for (int i = 0; i < tables.size(); i++) {
					if (tables.elementAt(i).toHtml().contains("Grantor")
							&& tables.elementAt(i).toHtml().contains("Grantee")
							&& i + 1 < tables.size()
							&& tables.elementAt(i + 1) instanceof TableTag) {
						TableTag grantor_grantee = (TableTag) tables
								.elementAt(i + 1);
						TableRow[] rows = grantor_grantee.getRows();

						String grantees = "";

						String grantors = "";

						if (rows.length == 1) {
							if (rows[0].getColumns()[0]
									.getChildren()
									.extractAllNodesThatMatch(
											new TagNameFilter("table")).size() == 1) {
								TableTag t = (TableTag) rows[0].getColumns()[0]
										.getChildren()
										.extractAllNodesThatMatch(
												new TagNameFilter("table"))
										.elementAt(0);
								TableRow[] tr = t.getRows();

								StringBuilder grantors_b = new StringBuilder();
								for (TableRow raux : tr)
									grantors_b.append(raux.toPlainTextString()
											.replaceAll("\\s+", " ") + "&&&");
								grantors = grantors_b.toString();
							}

							if (rows[0].getColumns()[1]
									.getChildren()
									.extractAllNodesThatMatch(
											new TagNameFilter("table")).size() == 1) {
								TableTag t = (TableTag) rows[0].getColumns()[1]
										.getChildren()
										.extractAllNodesThatMatch(
												new TagNameFilter("table"))
										.elementAt(0);
								TableRow[] tr = t.getRows();

								StringBuilder grantees_b = new StringBuilder();
								for (TableRow raux : tr)
									grantees_b.append(raux.toPlainTextString()
											.replaceAll("\\s+", " ") + "&&&");

								grantees = grantees_b.toString();
							}
						}

						map.put("tmpName", grantors);
						map.put("tmpSetName", "GrantorSet");

						parseNames(map);

						map.put("tmpName", grantees);
						map.put("tmpSetName", "GranteeSet");

						parseNames(map);

						break;
					}
				}

				// related doc
				// get references from comment
				String comment = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "Comment:"), "", true);

				ArrayList<String> comRefList = new ArrayList<String>();

				if (StringUtils.isNotEmpty(comment)) {
					Pattern p = Pattern.compile("\\d+-\\d+");
					Matcher ma = p.matcher(comment);
					while (ma.find()) {
						comRefList.add(ma.group());
					}
				}

				for (int i = 0; i < tables.size(); i++) {
					if (tables.elementAt(i).toHtml()
							.contains("Related Document Information")
							&& i + 1 < tables.size()
							&& tables.elementAt(i + 1) instanceof TableTag) {
						TableTag crossref_table = (TableTag) tables
								.elementAt(i + 1);
						TableRow[] rows = crossref_table.getRows();

						@SuppressWarnings("rawtypes")
						List<List> bodyCR = new ArrayList<List>();
						List<String> line = new ArrayList<String>();

						for (int j = 1; j < rows.length; j++) {
							if (rows[j].getColumnCount() == 5) {
								line = new ArrayList<String>();
								line.add(rows[j].getColumns()[0]
										.toPlainTextString());
								line.add(rows[j].getColumns()[1]
										.toPlainTextString());
								line.add(rows[j].getColumns()[2]
										.toPlainTextString());
								bodyCR.add(line);
							}
						}

						if (!comRefList.isEmpty()) {
							for (String ref : comRefList) {
								line = new ArrayList<String>();
								String book = ref.split("-")[0];
								String page = ref.split("-")[1];
								line.add(book.length() == 2 ? "19" + book : book);
								line.add(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(page));
								line.add("");
								bodyCR.add(line);
							}
						}

						if (bodyCR.size() > 0) {
							String[] header = { "Book", "Page", "DocumentType" };
							ResultTable rt = GenericFunctions2
									.createResultTable(bodyCR, header);
							map.put("CrossRefSet", rt);
						}

						break;
					}
				}
			}

			// legal des
			String lot_list = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Lot List:"), "", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(lot_list)) {
				map.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
						.getKeyName(), lot_list.trim());
			}

			String subdivision = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Subdivision:"), "", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(lot_list)) {
				map.put(PropertyIdentificationSetKey.SUBDIVISION_NAME
						.getKeyName(), subdivision.trim());
			}

			String section = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Section:"), "", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(section)) {
				map.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION
						.getKeyName(), section.trim());
			}

			String range = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Range:"), "", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(range)) {
				map.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE
						.getKeyName(), range.trim());
			}

			String block = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Block:"), "", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(block)) {
				map.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK
						.getKeyName(), block.trim());
			}

			String city = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "City:"), "", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(city)) {
				map.put(PropertyIdentificationSetKey.CITY.getKeyName(),
						city.trim());
			}

			String twn = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Township:"), "", true);

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(twn)) {
				map.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP
						.getKeyName(), twn.trim());
			}

			String legal = HtmlParser3.getValueFromNextCell(
					HtmlParser3.findNode(nodeList, "Comment:"), "", true);

			map.put("tmpLegal", legal);

			parseLegal(map);

			map.removeTempDef();
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String	FAKE_LINK_SEPARATOR	= "___000___";

	public static String makePartOfFakeLink(ResultMap m) {
		String fake = "&document_type=";
		if (StringUtils.isNotEmpty((String) m.get(SaleDataSetKey.DOCUMENT_TYPE
				.getKeyName())))
			fake += (String) m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName());
		if (StringUtils.isNotEmpty((String) m.get(SaleDataSetKey.BOOK
				.getKeyName())))
			fake += "_" + (String) m.get(SaleDataSetKey.BOOK.getKeyName());
		if (StringUtils.isNotEmpty((String) m.get(SaleDataSetKey.PAGE
				.getKeyName())))
			fake += "_" + (String) m.get(SaleDataSetKey.PAGE.getKeyName());
		return fake;
	}
}

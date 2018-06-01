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

public class KSSedgwickRO {

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

		if (cols.length == 7) {

			String grantor = StringUtils.strip(cols[3].toPlainTextString().replaceAll("\\s+", " "));

			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(grantor)) {
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor);

				m.put("tmpName", grantor);
				m.put("tmpSetName", "GrantorSet");

				parseNames(m);
			}

			String doc_no = StringUtils.strip(cols[0].toPlainTextString().replaceAll("\\s+", ""));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(doc_no))
				m.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), doc_no);

			String instr_no = StringUtils.strip(cols[4].toPlainTextString().replaceAll("\\s+", ""));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instr_no))
				m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr_no);

			String recorded_date = StringUtils.strip(cols[1].toPlainTextString());
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(recorded_date))
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recorded_date);

			String instrument_type = StringUtils.strip(cols[2].toPlainTextString().replaceAll("\\s+", " "));
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrument_type))
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instrument_type);

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
			String inst_no = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodes, "DOC.#/FLM-PG:"), "", true).replaceAll("(?ism)\\&NBSP;", "");
			if (StringUtils.isNotEmpty(inst_no))
				map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), inst_no);

			String doc_no = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodes, "FILM/PAGE:"), "", true).replaceAll("(?ism)\\&NBSP;", "");
			if (StringUtils.isNotEmpty(doc_no))
				map.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), doc_no);

			String instr_type = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodes, "Instrument Type"), "", true)
					.replaceAll("(?ism)\\&NBSP;", "");
			if (StringUtils.isNotEmpty(instr_type))
				map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), instr_type.trim());

			String rec_date = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodes, "Reception Date"), "", true)
					.replaceAll("(?ism)\\&NBSP;", "");
			if (StringUtils.isNotEmpty(rec_date))
				map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), rec_date);

			// legal
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"));

			StringBuffer allLots = new StringBuffer();
			StringBuffer allBlocks = new StringBuffer();

			if (tables.size() > 0) {
				NodeList platInfo = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "dgrdPlatInfo"), true);
				if (platInfo.size() > 0) {
					TableTag legalT = (TableTag) platInfo.elementAt(0);
					TableRow[] rows = legalT.getRows();

					for (int i = 1; i < rows.length; i++) {
						if (rows[i].getColumnCount() == 11) {
							String lot = rows[i].getColumns()[1].toPlainTextString().replaceAll("(?ism)\\&NBSP;", "");
							String thru_lot = rows[i].getColumns()[2].toPlainTextString().replaceAll("(?ism)\\&NBSP;", "");

							String lots = lot;

							if (StringUtils.isNotEmpty(thru_lot)) {
								lots = lot + "-" + thru_lot;
								lots = StringFormats.ReplaceIntervalWithEnumeration(lots);
							}

							if (StringUtils.isNotEmpty(lots))
								allLots.append(lots + " ");

							String block = rows[i].getColumns()[3].toPlainTextString().replaceAll("(?ism)\\&NBSP;", "");

							if (StringUtils.isNotEmpty(block))
								allBlocks.append(block + " ");
						}
					}
				}
			}

			if (StringUtils.isNotEmpty(allLots.toString().trim()))
				map.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), allLots.toString().trim());

			if (StringUtils.isNotEmpty(allBlocks.toString().trim()))
				map.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), allBlocks.toString().trim());

			NodeList partyTable = nodes
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "dgrdParty"), true);

			if (partyTable.size() == 0)
				return;

			StringBuffer grantors = new StringBuffer();
			StringBuffer grantees = new StringBuffer();

			TableRow[] rows = ((TableTag) partyTable.elementAt(0)).getRows();

			for (TableRow r : rows) {
				if (r.getColumnCount() == 2) {
					if (r.getColumns()[0].toPlainTextString().toUpperCase().contains("GRANTOR")) {
						grantors.append(StringUtils.strip(r.getColumns()[1].toPlainTextString().replace("&nbsp;", " ").replaceAll("\\s+", " ")) + " &&& ");
					} else if (r.getColumns()[0].toPlainTextString().toUpperCase().contains("GRANTEE")) {
						grantees.append(StringUtils.strip(r.getColumns()[1].toPlainTextString().replace("&nbsp;", " ").replaceAll("\\s+", " ")) + " &&& ");
					}
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

			// crossrefs
			if (tables.size() > 0) {
				NodeList crossRef = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "dgrdRefer"), true);
				if (crossRef.size() > 0) {
					TableTag relatedDocTable = (TableTag) crossRef.elementAt(0);

					TableRow[] crossRows = relatedDocTable.getRows();

					@SuppressWarnings("rawtypes")
					List<List> bodyCR = new ArrayList<List>();
					List<String> line = new ArrayList<String>();

					for (int j = 1; j < crossRows.length; j++) {
						if (crossRows[j].getColumnCount() == 5) {
							line = new ArrayList<String>();
							line.add(crossRows[j].getColumns()[0].toPlainTextString());
							line.add(crossRows[j].getColumns()[2].toPlainTextString());
							line.add(crossRows[j].getColumns()[3].toPlainTextString());
							// line.add(crossRows[j].getColumns()[4].toPlainTextString());
							bodyCR.add(line);
						}
					}

					if (bodyCR.size() > 0) {
						String[] header = { "DocumentNumber", "InstrumentNumber", "Instrument_Ref_Type" };
						ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
						map.put("CrossRefSet", rt);
					}
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

package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
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

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameNormalizer;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;

/**
 * 
 * @author Oprina George
 * 
 *         Oct 5, 2012
 */

public class FLHillsboroughAO {

	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo) {
		ResultMap m = new ResultMap();
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "AO");

		TableColumn[] cols = row.getColumns();

		if (cols.length == 6) {

			try {

				String owner = "";
				String parcel = "";
				String folio = "";
				String addr = "";
				String city = "";

				switch (additionalInfo) {
				case 1:
					owner = cols[1].toPlainTextString();
					parcel = cols[2].toPlainTextString();
					folio = cols[3].toPlainTextString();
					addr = cols[4].toPlainTextString();
					city = cols[5].toPlainTextString();
					break;
				case 2:
					addr = cols[1].toPlainTextString();
					parcel = cols[2].toPlainTextString();
					folio = cols[3].toPlainTextString();
					owner = cols[4].toPlainTextString();
					city = cols[5].toPlainTextString();
					break;
				case 12:
					folio = cols[1].toPlainTextString();
					parcel = cols[2].toPlainTextString();
					owner = cols[3].toPlainTextString();
					addr = cols[4].toPlainTextString();
					city = cols[5].toPlainTextString();
					break;
				case 3:
					parcel = cols[1].toPlainTextString();
					folio = cols[2].toPlainTextString();
					owner = cols[3].toPlainTextString();
					addr = cols[4].toPlainTextString();
					city = cols[5].toPlainTextString();
					break;
				}

				if (StringUtils.isNotEmpty(folio)) {
					m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), folio);
				}

				if (StringUtils.isNotEmpty(parcel)) {
					m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), parcel);
				}

				if (StringUtils.isNotEmpty(city)) {
					m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
				}

				parseAddress(m, addr);

				if (StringUtils.isNotEmpty(owner)) {
					List<String> names = new ArrayList<String>();
					names.add(owner);
					parseNames(m, names, "");
				}

			} catch (Exception e) {
				e.printStackTrace();
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
				n = n.toUpperCase().trim().replaceAll("\\s+", " ");

				if (StringUtils.isNotEmpty(n)) {
					nameOnServer.append(n + " & ");
					String[] names = new String[] { "", "", "", "", "", "" };
					if (n.matches("\\w+ \\w \\w+")) {
						names = StringFormats.parseNameDavidsonAO(n, true);
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

		if (StringUtils.isEmpty(addr) || "0".equals(addr))
			return;

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr.replaceAll("\\s+", " "));

		String newAddr = addr;
		String streetNo = StringUtils.defaultString(StringFormats.StreetNo(newAddr)).trim().replaceAll("^0+", "");
		if (!streetNo.isEmpty()) {
			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		}

		String streetName = StringUtils.defaultString(StringFormats.StreetName(newAddr)).trim();
		if (!RegExUtils.getFirstMatch("(?i)\\b(CONFIDENTIAL)\\b", streetName, 1).isEmpty()) {
			streetName = streetName.replaceAll("(?i)\\s*(\\bXX\\b|\\bSITE\\b|\\*+)\\s*","");
		}
		if (!streetName.isEmpty()) {
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		}
	}

	public static void parseLegal(ResultMap m, String legal) {
		if (StringUtils.isEmpty(legal))
			return;

		String legalDes = legal;

		legalDes = legalDes.replaceAll("[\r\t]", "").replaceAll("\\s+", " ").replaceAll("===$", "").trim();

		m.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDes.replaceAll("=", " ").replaceAll("\\s+", " "));

		Pattern LEGAL_LOT = Pattern.compile("\\b(LOTS?) (\\d+)");
		Pattern LEGAL_BLK = Pattern.compile("\\bBLO?C?K ?(\\w+(?:-\\w+)?)");
		Pattern LEGAL_UNIT = Pattern.compile("\\bUNIT ?N?O? ?(\\d+)");
		Pattern LEGAL_TRACT = Pattern.compile("\\bTRACT (\\d+)");
		Pattern LEGAL_PHASE = Pattern.compile("\\bPHASE (\\d+(?:-\\d+)?)");
		Pattern LEGAL_PLAT = Pattern.compile("\\bPLAT (\\d+-\\d+)");
		Pattern LEGAL_STR_SECTION = Pattern.compile("\\b(SECT?I?O?N?) (\\d+)");
		Pattern LEGAL_SUBDIV_SECTION = Pattern.compile("(?is)\\b(SECT?I?O?N?)\\s+((?:\\d+|[A-Z])(?:(?:\\s+AND)?\\s+(?:\\d+|[A-Z]))*)\\s+");

		legalDes = legalDes.replaceAll("\\bAND\\b", " ").replaceAll("\\s+", " ").trim();

		// get lot
		StringBuffer lots = new StringBuffer();

		Matcher matcher = LEGAL_LOT.matcher(legalDes);

		for (int i = 0; matcher.find() && i < 10; i++) {
			String lot = matcher.group(2);
			if (StringUtils.isNotEmpty(lot)) {
				lots.append(" " + lot.trim() + " ");
			}
			legalDes = legalDes.replaceFirst(matcher.group(), matcher.group(1) + " ").replaceAll("\\s+", " ");
			matcher = LEGAL_LOT.matcher(legalDes);
		}

		legalDes = legalDes.replaceAll("(?ism)\\bLOTS?", "");

		if (StringUtils.isNotEmpty(lots.toString().trim())) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots.toString().trim());
		}

		legalDes = legalDes.replaceAll("\\s+", " ").trim();
		matcher = LEGAL_STR_SECTION.matcher(legalDes.replaceFirst(".*?===", ""));

		StringBuffer sections = new StringBuffer();
		for (int i = 0; matcher.find() && i < 10; i++) {
			String section = matcher.group(2);
			if (StringUtils.isNotEmpty(section)) {
				sections.append(" " + section.trim() + " ");
			}
			legalDes = legalDes.replaceFirst(matcher.group(), matcher.group(1) + " ").replaceAll("\\s+", " ");
			matcher = LEGAL_STR_SECTION.matcher(legalDes);
		}

		if (StringUtils.isNotEmpty(sections.toString().trim())) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sections.toString().replaceAll("\\s+", " ").trim());
		}

		// get blk
		matcher = LEGAL_BLK.matcher(legalDes);
		if (matcher.find()) {
			String blk = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk.trim());
		}

		// clean subdiv
		String cleanSubdiv = (String) m.get(PropertyIdentificationSetKey.SUBDIVISION.getKeyName());
		cleanSubdiv = StringUtils.defaultString(cleanSubdiv);

		// get unit
		matcher = LEGAL_UNIT.matcher(cleanSubdiv);
		String unit = "";
		if (matcher.find()) {
			unit = matcher.group(1);
			cleanSubdiv = cleanSubdiv.replace(matcher.group(), " ");
		}

		// get tract
		matcher = LEGAL_TRACT.matcher(cleanSubdiv);
		if (matcher.find()) {
			String tract = matcher.group(1);
			cleanSubdiv = cleanSubdiv.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract.trim());
		}

		// get phase
		matcher = LEGAL_PHASE.matcher(cleanSubdiv);
		if (matcher.find()) {
			String phase = matcher.group(1).replaceAll("-", " ");
			phase = LegalDescription.cleanValues(phase, false, true);
			cleanSubdiv = cleanSubdiv.replace(matcher.group(), " ");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase.trim());
		}

		if (StringUtils.isNotEmpty(cleanSubdiv)) {
			
			cleanSubdiv = NameNormalizer.normalizeString(cleanSubdiv, NameNormalizer.numbers);
			matcher = LEGAL_UNIT.matcher(cleanSubdiv);
			if (matcher.find()) {
				unit += " " + matcher.group(1);
				cleanSubdiv = cleanSubdiv.replace(matcher.group(), " ");
			}
			
			String newCleanSubdiv = cleanSubdiv;
			//sometimes first line from Legal Lines is more complete than Subdivision
			//e.g. first line: TEMPLE TERRACE ESTATES SEC 22 23 26 AND 27 28 19
			//Subdivision: 55Z | TEMPLE TERRACE ESTATES SEC 22 23 26 AND 27 28
			String firstLine = legal.replaceFirst("===.*", "");
			if (firstLine.length()>newCleanSubdiv.length() && firstLine.contains(newCleanSubdiv) && firstLine.indexOf(newCleanSubdiv)==0) {
				newCleanSubdiv = firstLine;
			}
			matcher = LEGAL_SUBDIV_SECTION.matcher(newCleanSubdiv + " ");
			if (matcher.find()) {
				String subdSection = matcher.group(2).replaceAll("(?is)\\bAND\\b", "");
				subdSection = subdSection.replaceAll("\\s{2,}", " ");
				if (StringUtils.isNotEmpty(subdSection)) {
					m.put(PropertyIdentificationSetKey.SECTION.getKeyName(), subdSection);
				}
			}
			
			cleanSubdiv = cleanSubdiv.replaceAll("\\s+", " ").trim();

			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), cleanSubdiv);
		}
		
		if (!StringUtils.isEmpty(unit.trim())) {
			m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(unit.trim()));
		}
		
		// get plat 072511-0700
		String pb = (String) m.get(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName());
		String pp = (String) m.get(PropertyIdentificationSetKey.PLAT_NO.getKeyName());

		if (StringUtils.isEmpty(pb) && StringUtils.isEmpty(pp)) {
			matcher = LEGAL_PLAT.matcher(legalDes);
			if (matcher.find()) {
				String pb_pp = matcher.group(1);
				legalDes = legalDes.replace(matcher.group(), " ");

				if (pb_pp.contains("-")) {
					m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pb_pp.split("-")[0].trim());
					m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pb_pp.split("-")[1].trim());
				}
			}
		}
	}

	public static Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			NodeList nodes = new HtmlParser3(detailsHtml).getNodeList();

			NodeList divs = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true);

			String folio = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(divs, "Folio:", true), "", false).trim();
			if (StringUtils.isNotEmpty(folio)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), folio);
			}

			String pin = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(divs, "PIN:", true), "", false).trim();
			if (StringUtils.isNotEmpty(pin)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), pin);
			}

			String pb_pp = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(divs, "Plat Book / Page:", true), "", false)
					.replaceAll("(?ism)&nbsp;", "").trim();
			if (StringUtils.isNotEmpty(pb_pp)) {
				String pb = "";
				String pp = "";
				if (pb_pp.contains("/") && pb_pp.length() > 1) {
					pb = pb_pp.split("/")[0].trim();
					pp = pb_pp.split("/")[1].trim();
				}

				if (StringUtils.isNotEmpty(pb)) {
					resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pb);
				}

				if (StringUtils.isNotEmpty(pp)) {
					resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pp);
				}
			}

			String subdivision = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(divs, "Subdivision:", true), "", false)
					.replaceAll("(?ism)&nbsp;", "").trim();
			if (StringUtils.isNotEmpty(subdivision)) {
				if (subdivision.contains("|")) {
					subdivision = subdivision.split("[|]")[1];
				}

				subdivision = subdivision.replaceAll("(?ism)UNPLATTED", "");

				if (StringUtils.isNotEmpty(subdivision)) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION.getKeyName(), subdivision.trim());
				}
			}

			NodeList auxNList = divs.extractAllNodesThatMatch(new HasAttributeFilter("class", "siteAddress"), true);

			if (auxNList.size() > 0) {

				String address = auxNList.elementAt(0).toPlainTextString();

				if (address.contains(":")) {
					address = address.split(":")[1].trim();
				}

				parseAddress(resultMap, address);
			}

			auxNList = divs.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "ownerName"), true);

			if (auxNList.size() > 0) {
				TableTag nameT = (TableTag) auxNList.elementAt(0);

				List<String> names = new ArrayList<String>();

				for (TableRow r : nameT.getRows()) {
					names.add(r.toPlainTextString().trim());
				}

				parseNames(resultMap, names, "");
			}

			// appraisal
			auxNList = divs.extractAllNodesThatMatch(new HasAttributeFilter("id", "displayValueOther"), true);
			if (auxNList.size() > 0) {
				auxNList = auxNList.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(
						new HasAttributeFilter("class", "dataGrid"));

				if (auxNList.size() > 0) {
					String assesed = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(auxNList, "County", true), "", false).trim();
					String market = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(auxNList, "County", true), "", false).trim();

					if (StringUtils.isNotEmpty(assesed)) {
						resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assesed.replaceAll("[-,$]", ""));
					}

					if (StringUtils.isNotEmpty(market)) {
						resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), market.replaceAll("[-,$]", ""));
					}
				}
			}

			// references

			auxNList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "references"), true);

			if (auxNList.size() > 0) {
				TableTag t = (TableTag) auxNList.elementAt(0);
				TableRow[] rows = t.getRows();

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

				for (int i = 2; i < rows.length; i++) {
					TableRow r = rows[i];
					if (r.getColumnCount() == 8) {
						// parse related docs
						salesRow = new ArrayList<String>();

						String book = r.getColumns()[0].toPlainTextString();
						String page = r.getColumns()[1].toPlainTextString();
						String year = r.getColumns()[3].toPlainTextString();
						String type = r.getColumns()[4].toPlainTextString();
						String saleAmount = r.getColumns()[7].toPlainTextString();

						salesRow.add(StringUtils.defaultString(year).trim());
						salesRow.add(StringUtils.defaultString(type).trim());
						salesRow.add(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(StringUtils.defaultString(book)).trim());
						salesRow.add(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(StringUtils.defaultString(page)).trim());
						salesRow.add(StringUtils.defaultString(saleAmount).replaceAll("[ $,-]", ""));

						salesBody.add(salesRow);
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

			// getlegal
			auxNList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(new HasAttributeFilter("id", "legal"));

			if (auxNList.size() > 0) {
				TableTag t = (TableTag) auxNList.elementAt(0);

				StringBuffer buf = new StringBuffer();
				for (TableRow r : t.getRows()) {
					if (!r.toHtml().contains("Legal Description") && r.getColumnCount() == 2) {
						buf.append(r.getColumns()[1].toPlainTextString() + "===");
					}
				}
				parseLegal(resultMap, buf.toString());
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
			for (int i = returnList.size() - 1; i >= 0; i--)
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

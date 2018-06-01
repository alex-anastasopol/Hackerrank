package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
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
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

/**
 * 
 * @author Oprina George
 * 
 *         Nov 7, 2012
 */

public class MOJacksonYB {

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
					names = StringFormats.parseNameNashville(n, true);
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

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr.replaceAll("\\s+", " ").trim());

		String newAddr = addr;

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(newAddr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(newAddr)));
	}

	public static void parseLegal(ResultMap resultMap, String legal) {

		if (StringUtils.isEmpty(legal))
			return;

		String legalDes = legal;

		resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDes);

		Pattern LEGAL_LOT = Pattern.compile("LOTS? ?(\\d+)");
		Pattern LEGAL_BLK = Pattern.compile("BLK ?(\\d+)");

		Matcher matcher = LEGAL_LOT.matcher(legalDes);

		// get lot
		StringBuffer lots = new StringBuffer();
		while (matcher.find()) {
			String lot = matcher.group(1);
			lot = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(lot).replace(" ", ", ").replaceAll("\\s+", " ");
			lots.append(StringUtils.strip(lot) + ", ");
			legalDes = legalDes.replaceFirst(matcher.group(), " ").replaceAll("\\s+", " ");
		}
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), StringUtils.strip(lots.toString()).replaceAll("(,+)", ",").replaceAll(
				"(,$)|(^,)", ""));

		// get block
		matcher = LEGAL_BLK.matcher(legalDes);
		if (matcher.find()) {
			String block = matcher.group(1);
			legalDes = legalDes.replace(matcher.group(), " ").replaceAll("\\s+", " ");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), StringUtils.strip(block));
		}

		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), legalDes.trim());

	}

	public static void parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			NodeList nodes = new HtmlParser3(detailsHtml.replaceAll("(?ism)&nbsp;", "")).getNodeList();

			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			String parcel = getValueFromHtml(detailsHtml, "Parcel Number:");

			if (StringUtils.isNotEmpty(parcel)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel);
			}

			String address = getValueFromHtml(detailsHtml, "Address:");

			if (StringUtils.isNotEmpty(address)) {
				// split address & legal
				String legal = address;

				if (address.contains("(")) {
					String addr = address.replaceAll("(?ism)[^(]*\\(([^)]*)\\).*", "$1");
					legal = legal.replaceAll("(?ism)\\([^)]*\\)", "");
					parseAddress(resultMap, addr.trim());
				}

				parseLegal(resultMap, legal);
			}

			String amountDue = getValueFromHtml(detailsHtml, "Total Due To City:");

			if (StringUtils.isNotEmpty(amountDue)) {
//				resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue.replaceAll("[ -$,]", ""));
			}

			tables = tables.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "4"));

			String date = "";

			if (tables.size() > 0) {
				TableTag t = (TableTag) tables.elementAt(0);
				TableRow[] rows = t.getRows();

				for (int i = 1; i < rows.length; i++) {
					if (rows[i].getColumnCount() == 3 && rows[i].getColumns()[2].toPlainTextString().contains("Due to City")) {
						date = rows[i].getColumns()[1].toPlainTextString().replaceAll("\\s+", "");
					}
				}

				if (StringUtils.isNotEmpty(date)) {
//					resultMap.put("tmpDate", date);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getValueFromHtml(String html, String description) {
		try {
			if (StringUtils.isEmpty(html) || StringUtils.isEmpty(description)) {
				return "";
			}

			String[] htmlParts = html.replaceAll("\\s+", " ").split("(?ism)<br[^>]*>");

			for (String s : htmlParts) {
				if (s.contains(description)) {
					return s.replace(description, "").trim();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
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

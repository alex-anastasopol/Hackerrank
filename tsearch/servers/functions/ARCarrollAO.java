package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 15, 2011
 */

public class ARCarrollAO {
	public static void putSearchType(ResultMap resultMap, String type) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), type);
	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();

		putSearchType(resultMap, "AO");
		TableColumn[] cols = row.getColumns();
		if (cols.length == 8) {
			resultMap.put(
					PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getKeyName(),
					cols[0].toPlainTextString().trim());
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
					cols[1].toPlainTextString().trim());
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID3.getKeyName(),
					cols[1].toPlainTextString().replace("-", "").trim());

			parseNames(resultMap, cols[2].toPlainTextString().trim());
			parseAddress(resultMap, cols[3].toPlainTextString().trim());

			String s_t_r[] = cols[4].toPlainTextString()
					.replaceAll("[^\\d,-]", "").trim().split("-");
			if (s_t_r.length == 3) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION
						.getKeyName(), s_t_r[0]);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP
						.getKeyName(), s_t_r[1]);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE
						.getKeyName(), s_t_r[2]);
			}
		}
		resultMap.removeTempDef();
		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	public static List<List> parseName(ResultMap resultMap, String name) {

		name = name.replace("1/2", "");
		name = name.replace("%", "");

		name = name.replace("/", " & ").replace("\\", " & ");
		name = name.replace(" TST", " TRUST");
		name = name.replace(" TRST", " TRUST");
		name = name.replace(" REV ", " REVOCABLE ");
		name = name.replace(" LIV ", " LIVING ");
		name = StringUtils.strip(name.replace("\\s+", " "));

		HashSet<String> name_set = new HashSet<String>();

		name_set.add(name);

		if (name.contains("(") && name.contains(")")
				&& name.indexOf(")") != name.length() - 1) {
			name_set.remove(name);

			String first_name = name.replaceAll("\\([^\\)]*\\)", "");
			String second_name = name.substring(name.indexOf(" ")).replaceAll(
					"[()]", "");

			name_set.add(StringUtils.strip(first_name.replaceAll("\\s+", " ")));
			name_set.add(StringUtils.strip(second_name.replaceAll("\\s+", " ")));

			name = name.replaceAll("\\(.*\\)", " ").replaceAll("\\s+", " ");
			name = name.replace(first_name, "");
		}

		try {
			List<List> body = new ArrayList<List>();
			for (String s : name_set) {
				String[] names;
				if (LastNameUtils.isLastName(s.split(" ")[0]))
					names = StringFormats.parseNameNashville(s, true);
				else
					names = StringFormats.parseNameFML(s, new Vector<String>(),
							false, true);

				String[] type = GenericFunctions.extractAllNamesType(names);
				String[] otherType = GenericFunctions
						.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractNameSuffixes(names);
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
						type, otherType, NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), body);
			}
			return body;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<List>();
	}

	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, String name) {
		String parts[] = name.split("&&");
		List<List> body = new ArrayList<List>();

		String name_aux = "";
		// parse only names
		for (String s : parts) {
			if (!s.equals(""))
				if (isName(s)) {
					if (name_aux.equals(""))
						name_aux += s;
					else
						name_aux += " & " + s;
					List<List> ll = parseName(resultMap, s);
					for (List l : ll)
						body.add(l);
				} else
					break;
		}
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
				name_aux.replace("%", "").replaceAll("\\s+", " ").trim());
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean isName(String s) {
		if (s.matches(".*\\d+.*"))
			return false;
		String parts[] = s.split("[\\s/]");
		for (String ss : parts) {
			if (ss.matches("\\d+"))
				return false;
			if (LastNameUtils.isLastName(ss) || FirstNameUtils.isFirstName(ss)
					|| NameUtils.isCompany(ss))
				return true;
		}
		return false;
	}

	public static void parseAddress(ResultMap resultMap, String address) {

		address = address.toUpperCase();
		address = address.replaceAll("-", " ");
		address = address.replaceAll("\\([^\\)]*\\)", " ").trim();
		address = StringUtils.strip(address.replaceAll("\\s+", " ")) + " ";
		address = address.replaceAll("(\\d+)([A-Z]) ", "$1 $2").trim();

		resultMap.put(
				PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),
				address);

		// S HWY 221
		// HWY 221 SO
		String addr_parts[] = address.split(" ");
		if (addr_parts.length == 3) {
			if ((Normalize.isDirectional(addr_parts[0])
					&& Normalize.isSuffix(addr_parts[1]) && Normalize
					.isCompositeNumber(addr_parts[2]))
					|| (Normalize.isDirectional(addr_parts[2])
							&& Normalize.isSuffix(addr_parts[0]) && Normalize
							.isCompositeNumber(addr_parts[1]))) {
				resultMap.put(
						PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
						address);
				return;
			}
		}

		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
				StringFormats.StreetNo(address));
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
				StringFormats.StreetName(address));
	}

	public static void parseLegalDescription(ResultMap resultMap,
			String legal_des) {

		legal_des = legal_des.replaceAll("(?is)\\b(LOT)S?:?\\s*(\\d+)\\s*,?\\s*ALL\\s*(\\d+)", "$1 $2-$3");
		
		Pattern LEGAL_UNIT = Pattern.compile("\\bUNIT:*\\s+([^\\s]*)");
		Pattern LEGAL_LOT = Pattern.compile("\\bLOTS*:*\\s+([^A-Z]*)");
		Pattern LEGAL_BLOCK = Pattern.compile("\\bBLOCK:*\\s+([^\\s]*)");
		Pattern LEGAL_TRACT = Pattern.compile("\\bTRACT:*\\s+([^\\s,^/]*)");
		Pattern LEGAL_PHASE = Pattern.compile("\\bPH(ASE)*:*\\s+([^\\s]*)");

		String[] legal_part = StringUtils.strip(legal_des).split("&&");

		String subdiv = "";
		String legal = "";

		if (legal_part.length == 2) {
			subdiv = legal_part[0];
			legal = legal_part[1];
		}

		String s = (String) resultMap
				.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName());

		if (s != null)
			subdiv = subdiv.toUpperCase() + s;
		else
			subdiv = subdiv.toUpperCase();
		legal = legal.toUpperCase();

		resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER
				.getKeyName(), StringUtils.strip(legal));

		Matcher matcher = LEGAL_LOT.matcher("");

		String unit = "";
		String lot = "";
		String block = "";
		String tract = "";
		String phase = "";

		if (!subdiv.equals("")) {
			// get unit lot block tract phase
			matcher = LEGAL_UNIT.matcher(subdiv);
			if (matcher.find()) {
				unit = matcher.group(1);
				subdiv = subdiv.replace(matcher.group(), " ");
				unit = unit.replaceAll("\\s+", " ").trim();
			}

			matcher = LEGAL_LOT.matcher(subdiv);
			if (matcher.find()) {
				lot = matcher.group(1);
				subdiv = subdiv.replace(matcher.group(), " ");
				lot = lot.replace("&", ", ");
				lot = lot.replace(",", ", ");
				lot = lot.replace(".", ", ");
				lot = lot.replaceAll("\\s+", " ").trim();
				lot = ro.cst.tsearch.extractor.legal.LegalDescription
						.cleanValues(lot, true, true);
			}

			matcher = LEGAL_BLOCK.matcher(subdiv);
			if (matcher.find()) {
				block = matcher.group(1);
				subdiv = subdiv.replace(matcher.group(), " ");
				block = block.replaceAll("\\s+", " ").trim();
			}

			matcher = LEGAL_TRACT.matcher(subdiv);
			if (matcher.find()) {
				tract = matcher.group(1);
				subdiv = subdiv.replace(matcher.group(), " ");
				tract = tract.replaceAll("\\s+", " ").trim();
			}

			matcher = LEGAL_PHASE.matcher(subdiv);
			if (matcher.find()) {
				phase = matcher.group(2);
				subdiv = subdiv.replace(matcher.group(), " ");
				phase = phase.replaceAll("\\s+", " ").trim();
			}
		}

		if (!legal.equals("")) {
			if (unit.equals("")) {
				matcher = LEGAL_UNIT.matcher(legal);
				if (matcher.find()) {
					unit = matcher.group(1);
					legal = legal.replace(matcher.group(), " ");
					unit = unit.replaceAll("\\s+", " ").trim();
				}
			}
			if (lot.equals("")) {
				matcher = LEGAL_LOT.matcher(legal);
				if (matcher.find()) {
					lot = matcher.group(1);
					legal = legal.replace(matcher.group(), " ");
					lot = lot.replace("&", ", ");
					lot = lot.replace(",", ", ");
					lot = lot.replace(".", ", ");
					lot = lot.replaceAll("\\s+", " ").trim();
					lot = ro.cst.tsearch.extractor.legal.LegalDescription
							.cleanValues(lot, true, true);
				}
			}
			if (block.equals("")) {
				matcher = LEGAL_BLOCK.matcher(legal);
				if (matcher.find()) {
					block = matcher.group(1);
					legal = legal.replace(matcher.group(), " ");
					block = block.replaceAll("\\s+", " ").trim();
				}
			}
			if (tract.equals("")) {
				matcher = LEGAL_TRACT.matcher(legal);
				if (matcher.find()) {
					tract = matcher.group(1);
					legal = legal.replace(matcher.group(), " ");
					tract = tract.replaceAll("\\s+", " ").trim();
				}
			}
			if (phase.equals("")) {
				matcher = LEGAL_PHASE.matcher(legal);
				if (matcher.find()) {
					phase = matcher.group(2);
					legal = legal.replace(matcher.group(), " ");
					phase = phase.replaceAll("\\s+", " ").trim();
				}
			}
		}

		if (ro.cst.tsearch.utils.Roman.isRoman(phase)) {
			// maybe roman number
			try {
				phase = ro.cst.tsearch.utils.Roman.parseRoman(phase) + "";
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_NAME
				.getKeyName()) == null) {
			resultMap.put(
					PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),
					StringUtils.strip(subdiv));
		}

		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(),
				unit);

		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
				.getKeyName(),
				lot.replaceAll("['\\*]", " ").replaceAll("\\s+", " "));

		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(),
				block);

		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(),
				tract);

		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(),
				phase);
	}

	public static void parseTaxes(ResultMap resultMap, NodeList taxNodeTag) {
	}

	public static ArrayList<Node> getNodes(NodeList nodeList,
			StringBuilder details) {
		ArrayList<Node> result = new ArrayList<Node>();

		NodeList tableList = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("table"), true);

		NodeList countyList = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("span"), true).extractAllNodesThatMatch(
				new HasAttributeFilter("id", "ctl00_MainContent_CountyLabel"),
				true);

		NodeList infoTableList = tableList.extractAllNodesThatMatch(
				new HasAttributeFilter("id", "Table1"), false);

		// legal
		NodeList legalList = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("span"), true).extractAllNodesThatMatch(
				new HasAttributeFilter("id", "ctl00_MainContent_lbllegal"),
				true);

		// ctl00_MainContent_Label4
		NodeList transferLabelList = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("span"), true).extractAllNodesThatMatch(
				new HasAttributeFilter("id", "ctl00_MainContent_Label4"), true);

		// ctl00_MainContent_TransfersGrid
		NodeList transferTableList = tableList
				.extractAllNodesThatMatch(new HasAttributeFilter("id",
						"ctl00_MainContent_TransfersGrid"), false);

		// ctl00_MainContent_landLabel
		NodeList landLabelList = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("span"), true).extractAllNodesThatMatch(
				new HasAttributeFilter("id", "ctl00_MainContent_landLabel"),
				true);

		// ctl00_MainContent_landGrid
		NodeList landTableList = tableList.extractAllNodesThatMatch(
				new HasAttributeFilter("id", "ctl00_MainContent_landGrid"),
				false);

		// lot may be vacant
		NodeList buildingLabelList = nodeList.extractAllNodesThatMatch(
				new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(
						new HasAttributeFilter("id",
								"ctl00_MainContent_buildingLabel"), true);

		// ctl00_MainContent_BldgGrid
		NodeList buildingsTableList1 = tableList.extractAllNodesThatMatch(
				new HasAttributeFilter("id", "ctl00_MainContent_BldgGrid"),
				false);
		NodeList buildingsTableList2 = tableList.extractAllNodesThatMatch(
				new HasAttributeFilter("id", "ctl00_MainContent_BldgGrid2"),
				false);
		NodeList buildingsTableList3 = tableList.extractAllNodesThatMatch(
				new HasAttributeFilter("id",
						"ctl00_MainContent_bldgDetailsGrid"), false);

		// create tables & details

		details.append("<table align=\"center\" border=\"1\"><tr><td align=\"center\">");

		Span countyS = null;
		if (countyList.size() > 0) {
			countyS = (Span) countyList.elementAt(0);
			for (int i = 0; i < countyS.getChildren().size(); i++) {
				if (countyS.childAt(i).toPlainTextString()
						.contains("brought to you")) {
					countyS.removeChild(i);
				}
			}
			// countyS.getChildren().remove(2);

			details.append(countyS.toHtml() + "<br>");
		}
		result.add(countyS);

		TableTag infoT = null;
		if (infoTableList.size() > 0) {
			infoT = (TableTag) infoTableList.elementAt(0);

			details.append(infoT.toHtml() + "<br>");
		}
		result.add(infoT);

		Span legalS = null;
		if (legalList.size() > 0) {
			legalS = (Span) legalList.elementAt(0);
			NodeList children = legalS.getChildren();
			children.prepend(new org.htmlparser.nodes.TextNode(
					"<strong>Legal: </strong><br>"));
			children.add(new org.htmlparser.nodes.TextNode("<br><br>"));
			legalS.setChildren(children);
			legalS.removeAttribute("style");

			details.append(legalS.toHtml());
		}

		result.add(legalS);

		Span transferS = null;
		if (transferLabelList.size() > 0) {
			transferS = (Span) transferLabelList.elementAt(0);

			details.append(transferS.toHtml());
		}

		result.add(transferS);

		TableTag transferT = null;
		if (transferTableList.size() > 0) {
			transferT = (TableTag) transferTableList.elementAt(0);

			details.append(transferT.toHtml() + "<br>");
		}

		result.add(transferT);

		Span landS = null;
		if (landLabelList.size() > 0) {
			landS = (Span) landLabelList.elementAt(0);

			details.append(landS.toHtml());
		}

		result.add(landS);

		TableTag landT = null;
		if (landTableList.size() > 0) {
			landT = (TableTag) landTableList.elementAt(0);

			details.append(landT.toHtml() + "<br>");
		}

		result.add(landT);

		Span buildingS = null;
		if (buildingLabelList.size() > 0) {
			buildingS = (Span) buildingLabelList.elementAt(0);

			details.append(buildingS.toHtml());
		}

		result.add(buildingS);

		TableTag buildingT1 = null;
		if (buildingsTableList1.size() > 0) {
			buildingT1 = (TableTag) buildingsTableList1.elementAt(0);
			details.append(buildingT1.toHtml() + "<br>");
		}

		result.add(buildingT1);

		TableTag buildingT2 = null;
		if (buildingsTableList2.size() > 0) {
			buildingT2 = (TableTag) buildingsTableList2.elementAt(0);

			details.append(buildingT2.toHtml() + "<br>");
		}

		result.add(buildingT2);

		TableTag buildingT3 = null;
		if (buildingsTableList3.size() > 0) {
			buildingT3 = (TableTag) buildingsTableList3.elementAt(0);

			details.append(buildingT3.toHtml());
		}

		result.add(buildingT3);

		details.append("</table>");

		return result;
	}

	private static String[] SUB_DIVS = null;

	private static void makeSub_divs() {
		String sub_divs = ro.cst.tsearch.servers.types.ARCarrollAO
				.getSUB_SELECT();

		sub_divs = sub_divs.replaceAll("<option value=\"[^>]*>", "");

		sub_divs = sub_divs.replaceAll("<select[^>]*>", "");

		sub_divs = sub_divs.replaceAll("[\r,\n,\t]", "");

		sub_divs = sub_divs.replaceAll(" </option>", "</option>").replace(
				"</select>", "");

		String[] sub_divs_vect = sub_divs.split("</option>");

		Comparator<String> c = new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				if (o1.length() > o2.length()) {
					return -1;
				} else if (o1.length() < o2.length()) {
					return 1;
				} else {
					return o1.compareTo(o2);
				}
			}
		};

		Arrays.sort(sub_divs_vect, c);

		SUB_DIVS = sub_divs_vect;
	}

	public static String getSubdivision(String sub) {
		String subdivision = "";

		if (SUB_DIVS == null)
			makeSub_divs();

		// search in subdivision list from site (best practice)
		for (String s : SUB_DIVS) {
			if (sub.indexOf(s) != -1)
				return s;
		}

		return subdivision;
	}

	public static void main(String args[]) {
		// && WILSON M NELL (HALL) && % NELL HALL WILSON && 2 FIRST STREET &&
		// EUREKA SPRINGS, AR 72632 &&
		ResultMap resm = new ResultMap();
		// parseNames(resm,"&& WILSON M NELL (HALL) && % NELL HALL WILSON && 2 FIRST STREET && EUREKA SPRINGS, AR 72632 &&");
		// parseNames(resm,"&& JOHNSON BETTY L TST && JOHNSON BETTY L TSTEE && 11 HARVEY RD && EUREKA SPRINGS, AR 72632 &&");
		// parseNames(resm,"&& REEVE (WHEELER) EVA M && PO BOX 816 && BERRYVILLE, AR 72616 &&");
		parseAddress(resm, "hwy 543 s");
		// int x = 0;
	}
}

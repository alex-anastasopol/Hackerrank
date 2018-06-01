package ro.cst.tsearch.servers.functions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;

public class UTDavisTR extends ParseClass {

	private static UTDavisTR _instance = null;

	private UTDavisTR() {

	}

	public static UTDavisTR getInstance() {
		if (_instance == null) {
			_instance = new UTDavisTR();
		}
		return _instance;
	}

	public enum UTDavisCities {
		BOUNTIFUL("Bountiful"), CENTERVILLE("Centerville"), CLEARFIELD(
				"Clearfield"), CLINTON("Clinton"), FARMINGTON("West Farmington"), FRUIT_HEIGHTS(
				"Fruit Heights"), KAYSVILLE("Kaysville"), LAYTON("Layton"), SALT_LAKE(
				"N. Salt Lake"), WEBER("S. Weber"), SUNSET("Sunset"), SYRACUSE(
				"Syracuse"), WEST_BOUNTIFUL("West Bountiful"), WEST_POINT(
				"West Point"), WOODS_CROSS("Woods Cross");

		String name = "";

		UTDavisCities(String n) {
			name = n;
		}
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(
			ServerResponse serverResponse, String response, long searchId,
			MessageFormat format) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		String table = response
				.replaceAll("(?is)<th colspan=\"4\".*?</th>", "");
		HtmlParser3 parser = new HtmlParser3(response);
		NodeList tableTag = HtmlParser3.getTag(parser.getNodeList(),
				new TableTag(), false);
		TableTag resultTable = (TableTag) tableTag.elementAt(0);
		if (resultTable != null) {

			TableRow[] rows = resultTable.getRows();

			int parcelColumn = 1;
			int addressColumn = 3;
			int legalColumn = 5;
			int detailLinkColumn = 6;
			String startLink = createPartialLink(format, TSConnectionURL.idGET);

			for (int i = 2; i < rows.length; i++) {
				NodeList cells = rows[i].getChildren();

				String parcelId = StringUtils.defaultIfEmpty(
						cells.elementAt(parcelColumn).toPlainTextString(), "")
						.trim();
				if (StringUtils.isNotEmpty(parcelId)) {
					ResultMap resultMap = new ResultMap();
					resultMap
							.put(PropertyIdentificationSet.PropertyIdentificationSetKey.PARCEL_ID
									.getKeyName(), parcelId);

					String legal = cells.elementAt(legalColumn)
							.toPlainTextString();

					String regEx = "(?is)\\.\\.\\.\\s*(<br />)?.*?";
					String smallLegal = "";
					if (RegExUtils.matches(regEx, legal)) {
						String[] split = legal.split(regEx);
						legal = split[1];
						smallLegal = split[0];
					}

					resultMap
							.put(PropertyIdentificationSet.PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER
									.getKeyName(), legal);
					parseLegalDescription(legal, resultMap);

					String address = cells.elementAt(addressColumn)
							.toPlainTextString();
					resultMap
							.put(PropertyIdentificationSet.PropertyIdentificationSetKey.ADDRESS_ON_SERVER
									.getKeyName(), address);
					parseAddress(address, resultMap);

					ParsedResponse currentResponse = new ParsedResponse();

					LinkTag linkTag = HtmlParser3.getFirstTag(cells,
							LinkTag.class, true);
					if (linkTag != null) {
						String newLink = startLink + linkTag.extractLink();
						linkTag.setLink(newLink);
						currentResponse.setPageLink(new LinkInPage(newLink,
								newLink, TSServer.REQUEST_SAVE_TO_TSD));
					}

					String rowHtml = rows[i].toHtml();
					rowHtml = rowHtml.replace(smallLegal, "");
					rowHtml = rowHtml.replaceAll("\\.\\.\\.\\s*<br />", "");
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					createDocument(searchId, currentResponse, resultMap);
					intermediaryResponse.add(currentResponse);

				}
			}
			String tableHeader = rows[1].getChildren().toHtml();
			serverResponse.getParsedResponse().setHeader(
					"<table>" + tableHeader);
			serverResponse.getParsedResponse().setFooter("</table>");
		}
		return intermediaryResponse;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		int offSetColumn = 1;
		int offSetRow = 0;
		HtmlParser3 parser = new HtmlParser3(response);

		String nodeLabel;
		String nodeValue = getYear(offSetColumn, offSetRow, parser);
		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), nodeValue.trim());

		nodeValue = getParcelID(offSetColumn, offSetRow, parser);
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
				nodeValue.trim());

		nodeLabel = "Legal Description:";
		nodeValue = HtmlParser3.getNodeValue(parser, nodeLabel, offSetRow,
				offSetColumn);
		String legalDescriptionOnServer = nodeValue;
		resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER
				.getKeyName(), nodeValue.trim());

		nodeLabel = "SITUS Address:";
		nodeValue = HtmlParser3.getNodeValue(parser, nodeLabel, offSetRow,
				offSetColumn);
		String addressOnServer = nodeValue;
		resultMap.put(
				PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),
				nodeValue.trim());

		nodeLabel = "2010 Tax Statement Recipient:";
		nodeValue = HtmlParser3.getNodeValue(parser, nodeLabel, offSetRow,
				offSetColumn);
		String nameOnServer = nodeValue;
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
				nodeValue.trim());

		setTaxData(response, resultMap);
		parseAddress(addressOnServer, resultMap);
		parseLegalDescription(legalDescriptionOnServer, resultMap);
		parseName(nameOnServer, resultMap);
		// saveTestDataToFiles(resultMap);
	}

	public String getYear(int offSetColumn, int offSetRow, HtmlParser3 parser) {
		String nodeLabel = "Year:";

		String nodeValue = HtmlParser3.getNodeValue(parser, nodeLabel,
				offSetRow, offSetColumn);
		nodeValue = RegExUtils.getFirstMatch("(?is)\\d{4,4}", nodeValue, 0);
		return nodeValue;
	}

	public String getParcelID(int offSetColumn, int offSetRow,
			HtmlParser3 parser) {
		String nodeLabel;
		String nodeValue;
		nodeLabel = "Serial Number:";
		nodeValue = HtmlParser3.getNodeValue(parser, nodeLabel, offSetRow,
				offSetColumn);
		return nodeValue;
	}

	public void setTaxData(String response, ResultMap resultMap) {
		String taxHistorytableHtml = "<table>"
				+ RegExUtils
						.getFirstMatch(
								"(?is)<tr>\\s*<th class=\"border-light_bgcolor-light\">Year.*?</table>",
								response, 0);

		List<HashMap<String, String>> tableAsListMap = HtmlParser3
				.getTableAsListMap(taxHistorytableHtml);

		String[] header = { "Year", "BaseAmount", "AmountPaid", "TotalDue" };

		Map<String, String> resultBodyHeaderToSourceTableHeader = new HashMap<String, String>();
		resultBodyHeaderToSourceTableHeader.put("Year", "Year");
		resultBodyHeaderToSourceTableHeader.put("BaseAmount", "Gen Taxes");
		resultBodyHeaderToSourceTableHeader.put("AmountPaid", "Paid");
		resultBodyHeaderToSourceTableHeader.put("TotalDue", "Due");
		tableAsListMap = ro.cst.tsearch.utils.StringUtils
				.cleanAmount(tableAsListMap);
		ResultBodyUtils.buildInfSet(resultMap, tableAsListMap, header,
				resultBodyHeaderToSourceTableHeader, TaxHistorySet.class);

		String year = (String) resultMap.get("TaxHistorySet.Year");
		int currentTaxYear = Integer.parseInt(year.replace(" ", ""));
		ResultTable rt = (ResultTable) resultMap.get("TaxHistorySet");

		String[] head = rt.getHead();
		int amountPaid = -1;
		int totalDue = -1;

		for (int i=0;i<head.length;i++) {
			if (amountPaid==-1 && head[i].toUpperCase().equals("AMOUNTPAID")) {
				amountPaid = i;
			}
			if (totalDue==-1 && head[i].toUpperCase().equals("TOTALDUE")) {
				totalDue = i;
			}
		}
		
		StringBuilder priorDelinquent = new StringBuilder("0.00");
				
		if (rt != null && rt.getLength() > 0 && amountPaid != -1 && totalDue != -1) {
			for (String[] row : rt.getBody()) {
				int rowYear = Integer.parseInt(row[0].replace(" ", ""));
				if (currentTaxYear > rowYear && row[amountPaid].equals("0.00")) {
					priorDelinquent.append("+").append(
							row[totalDue].replace(" ",""));
				}
			}
			resultMap.put("TaxHistorySet.PriorDelinquent", GenericFunctions2.sum(
					priorDelinquent.toString(), 0L));
		}
		
	}

	@Override
	public void parseName(String nameOnServer, ResultMap resultMap) {
		String name = "";
		String[] content = nameOnServer.split("(?is)<br />");

		for (int i = 0; i < content.length - 2; i++) {
			name = name + content[i].trim() + "<br />";
		}

		parseNamePrivate(name, resultMap);

	}

	private void parseNamePrivate(String name, ResultMap resultMap) {
		String[] split = name.split("<br />|C/O|%");
		int length = split.length;
		List body = new ArrayList();
		for (String string : split) {
			string = string.replaceAll("(?is)-?-?\\s?(TRUSTEES?)", " $1");
			string = string.replaceAll("(?is)ETAL?-?-?\\s?(TRUSTEES?)", "  $1");
			string = string.replaceAll("\\bAKA\\b", " AND ");

			string = string.replaceAll("\\(|\\)", "");

			string = string.replaceAll("%", "");
			string = string.replaceAll("C/O", "");

			string = string.replaceAll("(AND \\w+)-(\\w+)", "$1 AND $2");

			String tempNames = string.replaceAll(" & ", " AND ");
			int countMatches = StringUtils.countMatches(tempNames, " AND ");

			if (countMatches >= 2) {
				String[] subNames = string.split(" AND|& ");
				for (int i = 0; i <= subNames.length - 2; i = i + 2) {
					ParseNameUtil.putNamesInResultMapFromNashvilleParse(
							resultMap, subNames[i] + " AND " + subNames[i + 1],
							body);
					if (i + 1 == subNames.length - 2) {
						ParseNameUtil.putNamesInResultMapFromNashvilleParse(
								resultMap, subNames[i + 2], body);
					}
				}
			} else {
				ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap,
						string, body);
			}
		}

	}

	@Override
	public void parseAddress(String addressOnServer, ResultMap resultMap) {
		addressOnServer = StringUtils.defaultIfEmpty(addressOnServer, "")
				.trim();
		if (RegExUtils.matches("(?is)\\d+$", addressOnServer)) {
			String zip = RegExUtils.getFirstMatch("(?is)\\d+$",
					addressOnServer, 0);
			resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
			addressOnServer = addressOnServer.replaceAll(zip, "").trim();
		}

		String address = "";
		String htmlBreak = "(?is)<br />";
		String city = "";
		if (RegExUtils.matches(htmlBreak, addressOnServer)) {
			String[] split = addressOnServer.split(htmlBreak);
			address = split[0];
			String[] zipState = RegExUtils.parseByRegEx(split[1],
					"(?is)(\\w+),( \\d+)?", new int[] { 1, 2 });
			if (zipState.length == 2) {
				String keyName = PropertyIdentificationSetKey.ZIP.getKeyName();
				String zip = (String) resultMap.get(keyName);
				if (StringUtils.isEmpty(zip)) {
					resultMap.put(keyName, zipState[1]);
				}
				city = split[1];
			}
		} else {
			String cities = "(Bountiful|Centerville|Clearfield|Clinton|Farmington|Fruit Heights|Kaysville|Layton|Salt Lake|"
					+ "Weber|Sunset|Syracuse|West Bountiful|West Point|Woods Cross)";
			// get the city
			String cityRegEx = "(?is)(NORTH|EAST|WEST)? " + cities
					+ "( CITY)?$";

			city = RegExUtils.getFirstMatch(cityRegEx, addressOnServer, 0);
			address = addressOnServer.replaceAll(city, "");
		}

		if (StringUtils.isNotEmpty(city)) {
			city = city.trim().replaceAll(",", "");
		}

		resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
		addressOnServer = addressOnServer.replaceAll(htmlBreak, "");

		// if (content.length >= 2) {
		String streetName = StringFormats.StreetName(address).trim();
		String streetNo = StringFormats.StreetNo(address);

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
				streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
				streetNo);

		// String cityZip = content[content.length - 1];
		// String[] split = cityZip.split(",");

		// if (split.length == 2) {
		// resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(),
		// split[0]);
		// String[] zipState = RegExUtils.parseByRegEx(split[1],
		// "(UT)\\s(\\d+)", new int[] { 1, 2 });
		// if (zipState.length == 2) {
		// resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(),
		// zipState[1]);
		// resultMap.put(PropertyIdentificationSetKey.STATE.getKeyName(),
		// zipState[0]);
		// }
		// }
		// }
	}

	@Override
	public void parseLegalDescription(String legalDescription,
			ResultMap resultMap) {
		String legal = (String) resultMap
				.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER
						.getKeyName());
		setLot(legal, resultMap);
		setPhase(legal, resultMap);
		setBlock(legal, resultMap);
		setUnit(legal, resultMap);
		setSecTwnRng(legal, resultMap);
		setSubdivision(legal, resultMap);
	}

	public void setSubdivision(String legal, ResultMap resultMap) {
		String regEx = ",.*?\\s(?=SUB?|PRUD|PUD|EST|SURVEY|TS)";
		String subdivision = RegExUtils.getFirstMatch(regEx, legal, 0);
		subdivision = subdivision.replaceAll("P R U D", "PRUD");
		String subdivisionToTest = subdivision;

		while (StringUtils.isNotEmpty(subdivisionToTest)
				&& subdivisionToTest.length() != subdivision.length()) {
			subdivisionToTest = RegExUtils.getFirstMatch(regEx, legal, 0);
			if (StringUtils.isNotEmpty(subdivisionToTest)) {
				subdivision = subdivisionToTest;
			}
		}

		String[] split = subdivision.split(",");
		if (split.length >= 2) {
			subdivision = split[split.length - 1];
		}
		subdivision = subdivision.replaceAll("PRUD|SUB", "");
		subdivision = subdivision.replaceAll("BLK \\d+ (AMD)?", "");
		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),
				subdivision);
	}

	@Override
	public void setLot(String legal, ResultMap resultMap) {
		if (legal.contains("LOT")) {
			int lotIndex = legal.indexOf("LOT");
			String regExLots = "(?is)LOTS? ([\\d+\\s]+)";
			legal = legal.replaceAll("(\\d)(,)", "$1 ");
			legal = legal.replaceAll("\\b(AND)\\b", "&");
			if (lotIndex >= 0) {
				String lots = ro.cst.tsearch.utils.StringUtils
						.getSuccessionOFValues(legal, lotIndex, regExLots);
				resultMap.put(
						PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
								.getKeyName(), lots.replaceAll("\\s+", " "));
			}
		}
	}

	@Override
	public void setSecTwnRng(String name, ResultMap resultMap) {
		// RegExUtils.parseByRegEx("SEC\\s(\\w+)-T(\\w+)-R(\\w+)", name, new
		// int[] {1,2,3});
		List<String> matches = RegExUtils.getFirstMatch(
				"SEC\\s(\\w+)-T(\\w+)-R(\\w+)", name, 1, 2, 3);
		if (matches.size() == 3) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION
					.getKeyName(), matches.get(0));
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP
					.getKeyName(), matches.get(1));
			resultMap
					.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE
							.getKeyName(), matches.get(2));
		}
	}

	@Override
	public void setPhase(String text, ResultMap resultMap) {
		String phase = RegExUtils.getFirstMatch("PHASE\\s#?(\\d+)", text, 1);
		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(),
				phase);
	}

	@Override
	public void setBlock(String text, ResultMap resultMap) {
		String blk = RegExUtils.getFirstMatch("BLK\\s#?(\\d+)", text, 1);
		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(),
				blk);
	}

	@Override
	public void setUnit(String text, ResultMap resultMap) {
		String unit = RegExUtils.getFirstMatch("UNIT\\s#?(\\d+)", text, 1);
		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(),
				unit);
	}
}

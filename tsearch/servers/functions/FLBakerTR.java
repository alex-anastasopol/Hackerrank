package ro.cst.tsearch.servers.functions;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.Roman;

public class FLBakerTR extends ParseClass {

	private static FLBakerTR _instance = null;

	private FLBakerTR() {

	}

	public static FLBakerTR getInstance() {
		if (_instance == null) {
			_instance = new FLBakerTR();
		}
		return _instance;
	}

	@Override
	public Vector<ParsedResponse> parseIntermediary(ServerResponse serverResponse, String response, long searchId, MessageFormat format) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		HtmlParser3 parser = new HtmlParser3(response);
		NodeList nl = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), false);
		TableTag resultTable = (TableTag) nl.elementAt(0);
		TableRow[] rows = resultTable.getRows();
		String startLink = createPartialLink(format, TSConnectionURL.idGET);

		String tableHeader = rows[0].getChildren().toHtml();

		for (int i = 1; i < rows.length; i++) {
			NodeList cells = rows[i].getChildren();
			ResultMap resultMap = new ResultMap();

			if (cells.size() == 2) {
				resultMap.getMap().put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
				
				String name = cells.elementAt(0).toPlainTextString();
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
				parseName(name, resultMap);
				String parcelID = cells.elementAt(1).toPlainTextString();
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			}
			LinkTag linkTag = HtmlParser3.getFirstTag(cells, LinkTag.class, true);
			String newLink = startLink + "taxcollector/" + linkTag.extractLink();
			linkTag.setLink(newLink);
			
			String rowHtml = rows[i].toHtml();
			ParsedResponse currentResponse = new ParsedResponse();
			
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
			currentResponse.setOnlyResponse(rowHtml);
			currentResponse.setPageLink(new LinkInPage(newLink, newLink, TSServer.REQUEST_SAVE_TO_TSD));
			
			createDocument(searchId, currentResponse, resultMap);
			intermediaryResponse.add(currentResponse);
		}

		serverResponse.getParsedResponse().setHeader("<table>" + tableHeader);
		serverResponse.getParsedResponse().setFooter("</table>");
		return intermediaryResponse;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		HtmlParser3 parser = new HtmlParser3(response);
		
		//String year = RegExUtils.getFirstMatch("(\\d{4,4}) Tax Roll Parcel Information:", response, 1);
			
		NodeList nodeList = parser.getNodeList();
		Node node = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "td", "class", "name", true);
		String name = node.toPlainTextString();
		String nodeValue = HtmlParser3.getNodeValue(parser, "Parcel/Account Number", 0, 0);
		String parcelID = RegExUtils.getFirstMatch("(?is)<br>(.*)", nodeValue, 1);
		String addressNode = HtmlParser3.getNodeValue(parser, name, 1, 0);
		String legalDescriptionOnserver = RegExUtils.getFirstMatch("(?is)Legal Information:.*?<i>(.*?)</i>", response, 1);

		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);

		String[] split = addressNode.split("<br>");
		// sometimes there are names in address cell
		if (addressNode.startsWith("%") || name.endsWith("&") || split.length >= 2) {
			int length = split.length;
			if (length >= 2) {
				addressNode = split[length - 2] + "<br>" + split[length - 1];
				if (length > 2) {
					for (int j = 0; j <= length - 3; j++) {
						name += "<br>" + split[j];
					}
				}
			}
		}

		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
		parseName(name, resultMap);
//		resultMap.put("PropertyIdentificationSet.AddressOnServer", addressNode.trim());
//		parseAddress(addressNode.trim(), resultMap);
		legalDescriptionOnserver = legalDescriptionOnserver.trim();
		resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDescriptionOnserver);
		parseLegalDescription(legalDescriptionOnserver, resultMap);
		
		parseTaxes(parser, resultMap);

//		saveTestDataToFiles(resultMap);
	}

	private void parseTaxes(HtmlParser3 parser, ResultMap resultMap) {
		String year = RegExUtils.getFirstMatch("(\\d{4,4}) Tax Roll Parcel Information:", parser.getHtml(), 1);
		String baseAmount = StringUtils.defaultIfEmpty(HtmlParser3.getNodeValue(parser, "Total Bill:", 0, 1), "");
		String amountPaid = StringUtils.defaultIfEmpty(HtmlParser3.getNodeValue(parser, "Total Amount Paid", 1, 0), "");
		String amountDue = "";
		//String amountDue = StringUtils.defaultIfEmpty(HtmlParser3.getNodeValue(parser, "Current Pay Off Amount", 1, 0), "");
		baseAmount = ro.cst.tsearch.utils.StringUtils.cleanAmount(baseAmount);
		amountPaid = ro.cst.tsearch.utils.StringUtils.cleanAmount(amountPaid);
		if (StringUtils.isNotEmpty(baseAmount) && StringUtils.isNotEmpty(amountPaid)) {
			Double valueDue = Double.valueOf(baseAmount) - Double.valueOf(amountPaid);
			NumberFormat formatter = new DecimalFormat("#.##");	
			if (valueDue > 0) {
				amountDue = formatter.format(valueDue);
			}
			resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
			resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
			resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
		}
		
		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), ro.cst.tsearch.utils.StringUtils.cleanAmount(year));
	}

	@Override
	public void parseAddress(String addressOnServer, ResultMap resultMap) {
		addressOnServer = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());

		String[] split = addressOnServer.split("<br>");
		if (split.length == 2) {
			String address = split[0];

			address = address.replaceAll("\\bAPT\\b", "# ");
			address = address.replaceFirst("(?is)\\bP\\.?\\s*O\\.?\\b\\s+\\bBOX\\b\\s*[\\d-\\s]+", "");
			
			if (StringUtils.isNotEmpty(address)) {
				String streetName = StringFormats.StreetName(address).trim();
				String streetNo = StringFormats.StreetNo(address);

				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
			}

			String[] split2 = split[1].split(",");
			if (split2.length > 1) {
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), split2[0]);
				RegExUtils.getFirstMatch("\\d+", split2[1], 0);

				resultMap.put(PropertyIdentificationSetKey.STATE.getKeyName(), RegExUtils.getFirstMatch("\\w{2,2}", split2[1], 0));
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), RegExUtils.getFirstMatch("\\d+", split2[1], 0));
			}
		
			resultMap.remove(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		}
	}

	@Override
	public void parseName(String name, ResultMap resultMap) {
		String jointTennantsRegEx1 = "\\(?JNT TNTS (N COMMON.*?WITH RIGHT|W/RGT) OF SURVIVRSHP\\)?";

		String jointTennantsRegEx2 = "\\(JOINT TENANTS\\)";

		String containsRegExBetweenParanthesis = "\\((.*)<br>(.*)\\)";
		if (RegExUtils.matches(containsRegExBetweenParanthesis, name)) {
			name = name.replaceAll(containsRegExBetweenParanthesis, "$1 $2");
		}

		name = name.replaceAll("\\(LIFE ESTATE\\)", "");
		name = name.replaceAll("\\(TRUSTEE\\)", "TRUSTEE");
		name = name.replaceAll("<br>\\(?TRUSTEES\\)?", " TRUSTEES");
		name = name.replaceAll("EDIS\\.", "");
		name = name.replaceAll("& ET AL", "ETAL");
		name = name.replaceAll("\\(WITH RIGHT OF SURVIVORSHIP\\)", "");
		name = name.replaceAll("<br>INC", " INC");
		name = name.replaceAll("(?is)\\s+<\\s*/?\\s*br>\\s*\\*{3,}\\s*[\\w\\s]+\\*{3,}\\s*", " ");
		
		String[] names = name.split("<br>");
		List body = new ArrayList();

		// special case if the name contains Joint tenants or C/O
		if (RegExUtils.matches(jointTennantsRegEx1, name) || RegExUtils.matches(jointTennantsRegEx2, name)) {
			// remove the joint tenants
			name = name.replaceAll(jointTennantsRegEx1, "");
			name = name.replaceAll(jointTennantsRegEx2, "");
			name = name.replace("<br>", " ");
			String[] split = name.split("&");
			// from observation if a subsequent name is not related to the first
			// one, then it has a different last name
			List<String> newNames = new LinkedList<String>();
			int noOfNewNames = 0;
			String keepLastLastName = "";
			int concatenatedNamesCounter = 0;
			for (String string : split) {
				string = string.trim();
				boolean singleWord = string.split("\\s").length == 1;
				boolean namesInARow = RegExUtils.matches("^\\w*( \\w{1,1})?$", string.trim());
				boolean isLastName = LastNameUtils.isLastName(string);
				if (isLastName && !(singleWord || namesInARow)) {
					newNames.add(string);
					keepLastLastName = string.trim().split("\\s")[0];
					noOfNewNames++;
				} else {
					String newName = newNames.get(noOfNewNames - 1);
					if (concatenatedNamesCounter >= 1) {
						newName = keepLastLastName + " " + string;
						concatenatedNamesCounter = 0;
						newNames.add(newName);
						noOfNewNames++;
					} else {
						newName += " & " + string;
						concatenatedNamesCounter++;
						newNames.set(noOfNewNames - 1, newName);
					}
				}
			}
			names = newNames.toArray(new String[] {});
		}

		// if it contains "TRUSTEES" it could have a company after it
		String separator = "TRUSTEES";
		if (names.length == 1 && name.contains(separator)) {
			names = name.split(separator);
			for (int i = 0; i < names.length - 1; i++) {
				names[i] += " " + separator;
			}
		}

		// simple case
		if (names.length == 1) {
			ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, clean(names[0]), body);
		} else {
			for (String n : names) {
				// long name & Last name MI
				if (RegExUtils.matches("^\\b\\w*\\b\\s\\w{1,1}\\b$", n)) {
					// get last name from previous name
					n = getLastNameFromPreviousName(body, n);
				}

				if (RegExUtils.matches("& \\b\\w*\\b\\s\\w*\\b$", n)) {
					String newName = n.replaceAll("&", "").trim();
					if (!LastNameUtils.isLastName(newName)) {
						n = getLastNameFromPreviousName(body, newName);
					}
				}
				boolean isDessotoParse = false;
				n = n.replaceAll("%", "").trim();
				String[] split = n.split("\\s");
				String string = split[0];
				
				if (!LastNameUtils.isLastName(string)) {  //|| startsWithPercent
					isDessotoParse = true;
				}
				
				if (NameUtils.isCompany(n) && RegExUtils.matches("\\b\\w\\s&\\s\\w", n)){
					ParseNameUtil.putCompanyInResultMap(resultMap, body, n);
				}else{
					if (n.startsWith("C/O:") || n.startsWith("ATTN:") || isDessotoParse) {
						ParseNameUtil.putNamesInResultMapFromDeSotoParse(resultMap, clean(n), body);
					} else {
						ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, clean(n), body);
					}
				}
			}
		}
	}

	private String getLastNameFromPreviousName(List body, String n) {
		if (body.size() > 0) {
			ArrayList<String> nameArray = (ArrayList<String>) body.get(body.size() - 1);
			n = nameArray.get(2) + " " + n;
		}
		return n;
	}

	private String clean(String a) {
		a = a.replaceAll("C/O:", "");
		a = a.replaceAll("%", "");
		a = a.replaceAll("&", "AND");
		a = a.replaceAll("ATTN:", "");
		return a;
	}

	@Override
	public void parseLegalDescription(String legalDescription, ResultMap resultMap) {
		legalDescription = legalDescription.replaceAll("<br>", " ");
		legalDescription = legalDescription.replaceAll("(?is)(\\bLOTS?\\b\\s*(?:[\\d&-]+\\s*)*)\\s*((?:\\bSEC\\b\\s*[A-Z\\d]\\s*)?(?:\\bBLO?C?K\\b\\s*[-\\dA-Z&])?\\s*&?\\s*)\\1", "$1 $2");
		legalDescription = legalDescription.replaceAll("(?is)(\\bSEC\\b\\s*[A-Z\\d+])\\s*(\\bBLO?C?K\\b\\s*[-\\d+A-Z])\\s*&\\s*\\bSEC\\b\\s*([-A-Z\\d+])\\s+\\bBLO?C?K\\b\\s*([-\\d+A-Z])", "$1 & $3 $2 & $4");
		legalDescription = legalDescription.replaceAll("(?is)\\b(SEC|BLO?C?K)\\b\\s*([A-Z\\d+])\\s+&\\s+\\2", "$1 $2");
		legalDescription = legalDescription.replaceAll("(?is)\\d+\\s*ACR(?:ES)?\\s+\\b[NSEW]\\b\\s+\\d+\\s*/\\d+\\s*", "");
		legalDescription = legalDescription.replaceAll("(?is)\\bAS\\b\\s+DESC(?:RIBED)?\\s+\\bIN\\b+", "");
		legalDescription = legalDescription.replaceAll("\\s+", " ");

		setSecTwnRng(legalDescription, resultMap);
		setPhase(legalDescription, resultMap);
		setUnit(legalDescription, resultMap);
		setBlock(legalDescription, resultMap);
		setLot(legalDescription, resultMap);
		setCrossRefSet( legalDescription, resultMap);
		
		legalDescription = legalDescription.replaceAll("\\s{2,}", " ");
		setSubdivName(legalDescription, resultMap);

	}

	private void setSubdivName(String legalDescription, ResultMap resultMap) {
		String subdiv = legalDescription;
		String regExp = "(?is)\\bLEG\\b(?:[\\w\\s]+)?\\s*\\bLOTS?\\b\\s*(?:[\\d&-]+\\s*)*\\s*(?:\\bSEC\\b\\s*(?:[A-Z\\d+](?:\\s*&\\s*[A-Z\\d+])?)\\s*)?"
				+ "(?:\\bBLO?C?K\\b\\s*[-\\d+A-Z&]+\\s*(?:SUB\\b(?: \\bOF\\b)? \\bBLO?C?K\\b\\s*[A-Z\\d-]+)?)?([\\s\\w/]+)\\s*\\bUN(?:IT)?";
		Matcher m = Pattern.compile(regExp).matcher(subdiv);
		
		if (subdiv.contains(" UNIT")) {
			if (subdiv.contains(" PH") || subdiv.contains(" PHASE") || subdiv.contains(" PB")) {
				if (m.find()) {
					subdiv = m.group(1).trim();
				}
			}
		} else {
			if (subdiv.contains(" PH") || subdiv.contains(" PHASE")) {
				if (subdiv.contains(" PB") && subdiv.contains(" PG")) {
					regExp = "(?is)\\bLEG\\b(?:[\\w\\s]+)?\\s*\\bLOTS?\\b\\s*(?:[\\d&-]+\\s*)*\\s*(?:\\bSEC\\b\\s*(?:[A-Z\\d+](?:\\s*&\\s*[A-Z\\d+])?)\\s*)?"
							+ "(?:\\bBLO?C?K\\b\\s*[-\\d+A-Z&]+\\s*(?:SUB\\b(?: \\bOF\\b)? \\bBLO?C?K\\b\\s*[A-Z\\d-]+)?)?([\\s\\w/]+)\\s*\\bPH(?:ASE)?";
					m.reset();
					m = Pattern.compile(regExp).matcher(subdiv);
					if (m.find()) {
						subdiv = m.group(1).trim();
					}
				}
			}  else if (subdiv.contains(" PB") && subdiv.contains(" PG")) {
				regExp = "(?is)\\bLEG\\b(?:[\\w\\s]+)?\\s*\\bLOTS?\\b\\s*(?:[\\d&-]+\\s*)*\\s*(?:\\bSEC\\b\\s*(?:[A-Z\\d+](?:\\s*&\\s*[A-Z\\d+])?)\\s*)?"
						+ "(?:\\bBLO?C?K\\b\\s*[-\\d+A-Z&]+\\s*(?:SUB\\b(?: \\bOF\\b)? \\bBLO?C?K\\b\\s*[A-Z\\d-]+)?)?([\\s\\w/]+)\\s*\\bPB\\b";
				m.reset();
				m = Pattern.compile(regExp).matcher(subdiv);
				if (m.find()) {
					subdiv = m.group(1).trim();
				}
			
			} else {
				regExp = "(?is)\\bLEG\\b(?:[\\w\\s]+)?\\s*\\bLOTS?\\b\\s*(?:[\\d&-]+\\s*)*\\s*(?:\\bSEC\\b\\s*(?:[A-Z\\d+](?:\\s*&\\s*[A-Z\\d+])?)\\s*)?"
						+ "(?:\\bBLO?C?K\\b\\s*[-\\d+A-Z&]+\\s*(?:SUB\\b(?: \\bOF\\b)? \\bBLO?C?K\\b\\s*[A-Z\\d-]+)?)?([\\s\\w/]+)\\s*\\b(?:NORTH|SOUTH|WEST|EAST)\\b OF\\b";
				m.reset();
				m = Pattern.compile(regExp).matcher(subdiv);
				if (m.find()) {
					subdiv = m.group(1).trim();
				
				} else {
					subdiv = "";
				}
			}
		}
		
		subdiv = subdiv.replaceAll("(?is)\\b(?:ADD(?:ITION)?|PER|(?:RE)?SUB(?:DIVISION)?|DESC IN|\\(?\\s*\\(?UNRECORDED\\s*\\)?\\s*)\\b", "");
		if (StringUtils.isNotEmpty(subdiv)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv.trim());
		}
	}

	@Override
	public void setSecTwnRng(String name, ResultMap resultMap) {

		String fullSecTwnRngRegEx = "SEC\\s(\\w+(-\\w+-\\w+)?)";

		// String onlySecRegEx = "SEC\\s(\\w+)";
		List<String> matches = null;
		if (RegExUtils.matches(fullSecTwnRngRegEx, name)) {
			matches = RegExUtils.getMatches(fullSecTwnRngRegEx, name, 1);
		}
		// else if (RegExUtils.matches(onlySecRegEx, name)) {
		// matches = RegExUtils.getMatches(onlySecRegEx, name, 1);
		// }

		String sec = StringUtils.defaultIfEmpty((String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName()), "");
		String twn = StringUtils.defaultIfEmpty((String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName()), "");
		String rng = StringUtils.defaultIfEmpty((String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName()), "");
		if (matches != null) {
			for (String s : matches) {
				s = StringUtils.defaultIfEmpty(s, "").trim();
				String[] split = s.split("-");
				if (split.length == 3) {
					if (StringUtils.isNotEmpty(sec)) {
						sec += " " + split[0];
					} else {
						sec = split[0];
					}

					if (StringUtils.isNotEmpty(twn)) {
						twn += " " + split[1];
					} else {
						twn = split[1];
					}
					if (StringUtils.isNotEmpty(rng)) {
						rng += " " + split[2];
					} else {
						rng = split[2];
					}
				} else if (split.length == 1) {
					if (StringUtils.isNotEmpty(sec)) {
						sec += " " + split[0];
					} else {
						sec = split[0];
					}
				}
			}
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), sec);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), twn);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), rng);
		}
	}

	@Override
	public void setLot(String legal, ResultMap resultMap) {
		if (legal.contains("LOT")) {
			int lotIndex = legal.indexOf("LOT");
			String regExLots = "(?is)LOTS? ([\\d+\\s]+)";
			legal = legal.replaceAll("(\\d)(,)", "$1 ");
			legal = legal.replaceAll("\\b(AND)\\b", "&");
			if (lotIndex >= 0) {
				String lots = ro.cst.tsearch.utils.StringUtils.getSuccessionOFValues(legal, lotIndex, regExLots);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lots.replaceAll("\\s+", " "));
			}
		}
	}

	@Override
	public void setBlock(String text, ResultMap resultMap) {
		List<String> matches = RegExUtils.getMatches("BLK\\s([A-Z\\-0-9]*)", text, 1);
		String blk = "";
		for (String s : matches) {
			blk += " " + s;
		}
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk);
	}

	@Override
	public void setPhase(String text, ResultMap resultMap) {
		String match = RegExUtils.getFirstMatch("PHASE\\s(\\w*)", text, 1);
		match = Roman.normalizeRomanNumbers(match);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), match);
	}

	@Override
	public void setUnit(String text, ResultMap resultMap) {
		String match = RegExUtils.getFirstMatch("UNIT\\s(\\w*)", text, 1);
		match = Roman.normalizeRomanNumbers(match);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), match);
	}

	@Override
	public void setCrossRefSet(String text, ResultMap resultMap) {
		// \\b(PB?|OR|BK|DB)\\s\\d+\\sPG?S?\\s\\d+

		String[] header = new String[] { "Book", "Page", "InstrumentNumber" };
		List<HashMap<String, String>> sourceSet = new ArrayList<HashMap<String, String>>();

		List<String> matches = RegExUtils.getMatches("\\b(PB?|OR|BK|DB)\\s(\\d+)\\sPG?S?\\s(\\d+)", text, 2, 3);
		List<String> yearInstrMatches = RegExUtils.getMatches("OR\\s(\\d+)-(\\d+)", text, 1, 2);
		
		for (String string : matches) {
			String[] split = string.split(" ");
			if (split.length == 2) {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("Book", split[0]);
				map.put("Page", split[1]);
				sourceSet.add(map);
			}
		}
		
		for (String string : yearInstrMatches) {
			String[] split = string.split(" ");
			if (split.length == 2) {
				HashMap<String, String> map = new HashMap<String, String>();
//				map.put("Year", split[0]);
				map.put("InstrumentNumber", split[0]+ "-" + split[1]);
				sourceSet.add(map);
			}
		}
		
		ResultBodyUtils.buildInfSet(resultMap, sourceSet, header, CrossRefSet.class);
	}
}

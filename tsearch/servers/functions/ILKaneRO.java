package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class ILKaneRO {
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void parseAndFillResultMap(String rsResponse, ResultMap m, long searchId) {

		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			NodeList nodeList = htmlParser3.getNodeList();

			String docType = "";
			String remarks = "";
			String grantor = "";
			String grantee = "";
			String address = "";
			String block = "";
			String lot = "";
			String amount = "";
			String subBlockLot = "";
			String secTwpRng = "";
			String secTwpRngAndQuadrands = "";
			String qtrSecNE = "";
			String qtrSecNW = "";
			String qtrSecSE = "";
			String qtrSecSW = "";
			String associatedDocuments = "";
			String recDoc = "";
			String origDoc = "";
			String uccDescription = "";
			String parcelIDs = "";
			String[] quadrants = new String[4];
			String[] parcels = null;

			String breakLineTagPattern = "\\s*<br\\s*/?>\\s*";

			NodeList list = nodeList.extractAllNodesThatMatch(new RegexFilter("Document\\s*(?:#|Number|No.?)"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					String documentNumber = node.toPlainTextString()
							.replaceFirst("(?is)Document\\s*(?:#|Number|No.?)?\\s*:?\\s*\\b(.*)\\s*$", "$1")
							.trim();
					m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
				}
			}

			// doctype: the first 11 modules bring only the doctype code; 'UCC search' brings the full name
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("(?:Document)?\\s*Type\\s*:"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					docType = node
							.toPlainTextString()
							.replaceFirst("(?is)(?:Document)?\\s*Type\\s*:?\\s*(.*)\\s*", "$1")
							.trim();
					m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
				}
			}

			// recorded date
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Date\\s+Filed"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					String recordedDate = node.toPlainTextString()
							.replaceFirst("(?is)Date\\s+Filed:?\\s*(\\d{1,2}/\\d{1,2}/\\d{4}).*", "$1")
							.trim();
					if (recordedDate.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
						m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
					}
				}
			}

			// document date
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Document\\s+Date"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					String documentDate = node.toPlainTextString()
							.replaceFirst("(?is)Document\\s+Date\\s?:?\\s*(\\d{1,2}/\\d{1,2}/\\d{4}).*", "$1")
							.trim();
					if (documentDate.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
						m.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), documentDate);
					}
				}
			}

			// consideration/mortgage amount
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Consideration"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					amount = node
							.toPlainTextString()
							.replaceFirst("(?is)Consideration\\s*:?\\s*([\\$\\d.,]+)", "$1")
							.replaceAll("[,$-]", "")
							.trim();

					if (amount.matches("[\\d+.]+")) {
						if (docType.equalsIgnoreCase("MTG") || docType.equalsIgnoreCase("MORTGAGE")) {
							m.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount);
						}
						else {
							m.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
						}
					}
				}
			}

			// grantor, grantee; debtor - UCC details ; should it be parsed as grantor ?
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Grantor"), true);
			if (list.size() > 0) {
				grantor = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class)).toHtml().trim();
			}
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Grantee"), true);
			if (list.size() > 0) {
				grantee = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class)).toHtml().trim();
			}

			// section/township/range
			List<List> body = new ArrayList<List>();
			List<String> line = null;

			list = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "lrsDetailsSectionsContainer"), true);
			if (list.size() > 0) {
				secTwpRngAndQuadrands = list.elementAt(0).toPlainTextString().replaceAll("(?s)\\s*/\\s*", "/").trim();
			}

			Pattern strPattern = Pattern.compile("(?is)([^/]+(?:/[^/]+)(?:/[^/\\n\\r]+))(.*)");
			Matcher strMatcher = strPattern.matcher(secTwpRngAndQuadrands);

			if (strMatcher.find()) {
				quadrants = strMatcher.group(2).trim().replaceAll("(?s)\\s+", " ").split(" ");
				secTwpRng = strMatcher.group(1) + "@";
			}

			qtrSecNE = quadrants[0] + "@";
			qtrSecNW = quadrants[1] + "@";
			qtrSecSE = quadrants[2] + "@";
			qtrSecSW = quadrants[3] + "@";

			// pot fi mai multi quadranti..
			if (StringUtils.isNotEmpty(secTwpRng)) {
				String[] str = secTwpRng.split("@");
				String[] ne = qtrSecNE.split("@");
				String[] nw = qtrSecNW.split("@");
				String[] se = qtrSecSE.split("@");
				String[] sw = qtrSecSW.split("@");
				for (int i = 0; i < str.length; i++) {
					if (StringUtils.isNotEmpty(str[i].trim())) {
						line = new ArrayList<String>();
						String[] sTR = str[i].split("/");
						String qValue = "";
						line.add(sTR[0]);
						line.add(sTR[1]);
						line.add(sTR[2]);
						if (StringUtils.isNotEmpty(ne[i])) {
							if (ne[i].matches("X")) {
								qValue += "NE, ";
							}
						}
						if (StringUtils.isNotEmpty(nw[i])) {
							if (nw[i].matches("X")) {
								qValue += "NW, ";
							}
						}
						if (StringUtils.isNotEmpty(se[i])) {
							if (se[i].matches("X")) {
								qValue += "SE, ";
							}
						}
						if (StringUtils.isNotEmpty(sw[i])) {
							if (sw[i].matches("X")) {
								qValue += "SW";
							}
						}
						line.add(qValue.replaceAll("(?is),\\s*$", ""));
						body.add(line);
					}
				}
				try {
					GenericFunctions2.saveSTRQInMap(m, body);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// parcelIDs
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Parcel\\s*Number"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));
				if (node != null) {
					parcelIDs = node.toHtml()
							.replaceAll("(?is)</?div[^>]*>", "")
							.replaceAll("(?is)<span[^>]*>.*?</span>" + breakLineTagPattern, "")
							.trim();
				}
			}

			if (StringUtils.isNotEmpty(parcelIDs)) {
				parcels = parcelIDs.split(breakLineTagPattern);
			}

			// subd block lot
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Sub\\s*/\\s*Block\\s*/\\s*Lot"), true);
			Vector pisVector = new Vector();
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class)).getNextSibling().getNextSibling();

				if (node != null) {
					subBlockLot = node.toHtml().replaceAll("(?s)\\s*/\\s*", "/");
				}

				subBlockLot = subBlockLot.replaceAll("(?is)</?(?:span|div)[^>]*>", "");
				subBlockLot = subBlockLot.replaceAll(breakLineTagPattern, "@");
				subBlockLot = subBlockLot.replaceAll("(?is)&amp;", "&").trim();
				String[] subBlockLotArray = subBlockLot.split("@");
				block = " ";
				lot = "";

				// for multiple subdivisions
				StringBuilder legals = new StringBuilder();
				for (int i = 0; i < subBlockLotArray.length; i++) {
					subBlockLotArray[i] = subBlockLotArray[i].trim();
					if (StringUtils.isNotEmpty(subBlockLotArray[i])) {
						String[] raw = subBlockLotArray[i].split("/");
						if (i == 0) {
							legals.append(subBlockLotArray[i]);
						} else {
							String[] rawAnterior = subBlockLotArray[i - 1].split("/");
							if (raw[0].trim().equals(rawAnterior[0])) {
								legals.append("@@").append(subBlockLotArray[i]);
							} else {
								legals.append("####").append(subBlockLotArray[i]);
							}
						}
					}
				}
				String[] sBL = legals.toString().split("####");
				if (sBL.length > 1) {// for multiple subdivisions
					for (int j = 0; j < sBL.length; j++) {
						String[] subdiv = sBL[j].split("@@");
						PropertyIdentificationSet pis = new PropertyIdentificationSet();
						for (int k = 0; k < subdiv.length; k++) {
							subdiv[k] = subdiv[k].replaceAll("(?is)/([\\w-\\s]+)/([\\w-\\s]+)$", "##$1##$2");// MOTE&RUN//NONE/1; ADMRS SUB/NONE/1; PEPVY 1//4/3
							String[] items = subdiv[k].split("##");

							if (k == 0) {
								pis.setAtribute("SubdivisionName", items[0]);
							}
							if (StringUtils.isNotEmpty(items[1])) {
								if (!items[1].contains("NONE")) {
									if (!block.contains(" " + items[1] + " ")) {
										block += items[1] + " ";
									}
								}
							}
							if (StringUtils.isNotEmpty(items[2])) {
								if (!items[2].contains("NONE")) {
									lot += items[2].replaceAll("\\s+", "#") + "  ";// we have lot = 26 C, or lot = PCL 1 and in cleanValues() it will be
																					// transformed in C 26 and the search by subdiv will fail
								}
							}
						}
						if (StringUtils.isNotEmpty(block.trim())) {
							// blk = LegalDescription.cleanValues(blk, false, true);//B4568
							pis.setAtribute("SubdivisionBlock", block.trim());
							block = "";
						}
						if (StringUtils.isNotEmpty(lot.trim())) {
							// lot = LegalDescription.cleanValues(lot, false, true);//B4568
							lot = lot.replaceAll("#", " ");
							pis.setAtribute("SubdivisionLotNumber", lot.trim());
							lot = "";
						}
						pisVector.add(pis);

					}
					// for multiple subdivisions
					ResultTable pisWithSTR = (ResultTable) m.get("PropertyIdentificationSet");
					if (pisWithSTR != null) {
						String[][] bodyRef = pisWithSTR.getBodyRef();
						for (int i = 0; i < bodyRef.length; i++) {
							PropertyIdentificationSet pis = new PropertyIdentificationSet();
							pis.setAtribute("SubdivisionSection", bodyRef[i][0]);
							pis.setAtribute("SubdivisionTownship", bodyRef[i][1]);
							pis.setAtribute("SubdivisionRange", bodyRef[i][2]);
							pisVector.add(pis);
						}
					}
					if (parcels != null) {
						for (int i = 0; i < parcels.length; i++) {
							PropertyIdentificationSet pis = new PropertyIdentificationSet();
							pis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcels[i]);
							pisVector.add(pis);
						}
					}
					m.put("PropertyIdentificationSet", pisVector);
				} else {
					String[] subdiv = sBL[0].split("@@");
					for (int k = 0; k < subdiv.length; k++) {
						subdiv[k] = subdiv[k].replaceAll("(?is)/([\\w-\\s]+)/([\\w-\\s]+)$", "##$1##$2");// MOTE&RUN//NONE/1; ADMRS SUB/NONE/1; PEPVY 1//4/3
						String[] items = subdiv[k].split("##");
						if (k == 0) {
							m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), items[0]);
						}
						if (StringUtils.isNotEmpty(items[1])) {// 92K15283
							if (!items[1].contains("NONE")) {
								if (!block.contains(" " + items[1] + " ")) {
									block += items[1] + " ";
								}
							}
						}
						if (StringUtils.isNotEmpty(items[2])) {
							if (!items[2].contains("NONE")) {
								lot += items[2].replaceAll("\\s+", "#") + " ";// we have lot = 26 C, or lot = PCL 1 and in cleanValues() it will be transformed
																				// in C 26 and the search by subdiv will fail
							}
						}
					}
					if (StringUtils.isNotEmpty(block.trim())) {
						// blk = LegalDescription.cleanValues(blk, false, true);//B4568
						m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block.trim());
						block = "";
					}
					if (StringUtils.isNotEmpty(lot.trim())) {
						// lot = LegalDescription.cleanValues(lot, false, true);//B4568
						lot = lot.replaceAll("#", " ");
						m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
						lot = "";
					}
				}
				
				if (parcels != null && StringUtils.isNotEmpty(parcels[0])) {
					m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcels[0].trim());

				}
			}

			// associated doc(s)
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Associated\\s+Doc"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					associatedDocuments = node.toHtml().replaceAll("(?is)</?div[^>]*>", "")
							.replaceAll("(?is)<span[^>]*>.*?</span>" + breakLineTagPattern, "")
							.replaceAll("(?is)<!?--[^>]*>", "")
							.trim();
				}
			}

			// address
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Address"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					address = node.toHtml().trim();
				}
			}

			// remarks
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Remarks"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					remarks = node.toPlainTextString().trim();
				}
			}

			// croffRefSet
			associatedDocuments = associatedDocuments.replaceAll("(?is)</?span[^>]*>", "").trim();
			String[] crossRefs = associatedDocuments.split(breakLineTagPattern);
			body = new ArrayList<List>();
			for (int i = 0; i < crossRefs.length; i++) {
				if (StringUtils.isNotEmpty(crossRefs[i])) {
					line = new ArrayList<String>();
					line.add("");
					line.add("");
					line.add(crossRefs[i].replaceAll("(?is)K((?:19|20)\\d{2})(.*)", "$1K$2"));// 2008K089929
					body.add(line);
				}
			}

			remarks = remarks.replaceAll("(?is)@(K\\d{4,11})\\s*&\\s*((\\d+)K\\d{4,11})\\b", "$3$1 & $2");// 95K067818
			remarks = remarks.replaceAll("(?is)\\b(\\d{2})\\d(K\\d+)\\b", "$1$2");// 94K028506

			remarks = remarks.replaceAll("(?is)\\b(20\\d{2})(\\d{5,6})\\b", "$1K$2");// 200018167 search with 2000K066441

			Pattern refFromRemaks = Pattern.compile("\\b([\\dK]{5,12})\\b");
			Matcher matcher = refFromRemaks.matcher(remarks);
			while (matcher.find()) {

				String instrNo = matcher.group(1).trim();
				instrNo = instrNo.replaceAll("(?is)K((?:19|20)\\d{2})(.*)", "$1K$2"); // //2008K089929

				if (instrNo.matches("(?is)\\A(\\d{2})K\\d+")) {
					String year = instrNo.replaceFirst("\\A(\\d{2})K\\d+", "$1");
					try {
						int yearInt = Integer.parseInt(year);
						if (yearInt < 94) {
							instrNo = instrNo.replaceFirst("\\A(\\d{2}K)0+(\\d+)", "$1$2");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (instrNo.matches("(?is)\\A(\\d{4})K\\d+")) {
					String year = instrNo.replaceFirst("\\A(\\d{4})K\\d+", "$1");
					try {
						int yearInt = Integer.parseInt(year);
						if (yearInt > 1999) {
							String partInstrNO = instrNo.replaceFirst("\\A(\\d{4}K)(\\d+)", "$2");
							instrNo = instrNo.replaceFirst("\\A(\\d{4}K)(\\d+)", "$1" + org.apache.commons.lang.StringUtils.leftPad(partInstrNO, 6, "0"));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				line = new ArrayList<String>();
				line.add("");
				line.add("");
				line.add(instrNo);
				body.add(line);
				remarks = remarks.replaceFirst(matcher.group(0), "");
			}

			if (!body.isEmpty()) {
				String[] header = { "Book", "Page", "InstrumentNumber" };
				ResultTable rt = GenericFunctions2.createResultTable(body, header);
				m.put("CrossRefSet", rt);
			}

			if (StringUtils.isNotEmpty(remarks)) {
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), remarks);
			}

			list = nodeList.extractAllNodesThatMatch(new RegexFilter("\\bFee\\(?s?\\b"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					amount = node
							.toPlainTextString()
							.replaceFirst("\\bFee\\(?s?\\b\\s*:?\\s*([\\$\\d.,]+).*", "$1")
							.replaceAll("[,$-]", "")
							.trim();

					if (amount.matches("[\\d+.]+")) {
						m.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
					}
				}
			}

			// to get from ucc
			// orig doc, R.E. doc
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Original\\s+Document"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					origDoc = node.toHtml()
							.replaceAll("(?is)</?div[^>]*>", "")
							.replaceAll("(?is)<span[^>]*>.*?</span>" + breakLineTagPattern, "")
							.trim();
				}
			}

			list = nodeList.extractAllNodesThatMatch(new RegexFilter("R.E.\\s+Document"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					recDoc = node.toHtml()
							.replaceAll("(?is)</?div[^>]*>", "")
							.replaceAll("(?is)<span[^>]*>.*?</span>" + breakLineTagPattern, "")
							.trim();
				}
			}

			// description for UCC documents
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("\\bDescription\\s*:?"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					uccDescription = node.toPlainTextString()
							.replaceFirst("Description\\s*:?(.*?)", "$1")
							.replaceAll("(?s)\\s+", " ")
							.trim();

					if (StringUtils.isNotEmpty(uccDescription)) {
						m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), uccDescription);
					}
				}
			}

			// crossRef set for UCC documents
			List<List> bodyCR = new ArrayList<List>();
			line = new ArrayList<String>();

			origDoc = origDoc.replaceAll("(?is)</?span[^>]*>", "").trim();
			if (StringUtils.isNotEmpty(origDoc)) {

				String[] crossDocs = origDoc.split(breakLineTagPattern);
				for (int i = 0; i < crossDocs.length; i++) {
					if (!crossDocs[i].contains("Document") && StringUtils.isNotEmpty(crossDocs[i])) {
						line = new ArrayList<String>();
						line.add("");
						line.add("");
						line.add(crossDocs[i]);
						bodyCR.add(line);
					}
				}
			}

			recDoc = recDoc.replaceAll("(?is)</?span[^>]*>", "").trim();
			if (StringUtils.isNotEmpty(recDoc)) {
				String[] crossDocs = recDoc.split(breakLineTagPattern);
				for (int i = 0; i < crossDocs.length; i++) {
					if (!crossDocs[i].contains("Document") && StringUtils.isNotEmpty(crossDocs[i])) {
						line = new ArrayList<String>();
						line.add("");
						line.add("");
						line.add(crossDocs[i]);
						bodyCR.add(line);
					}
				}
			}

			if (!bodyCR.isEmpty()) {
				String[] header = { "Book", "Page", "InstrumentNumber" };
				ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
				m.put("CrossRefSet", rt);
			}
			// names for ucc
			// debtor
			// secured party;
			// if parsing ucc, don't parse address !! - e ok, nu se parseaza..
			Pattern namePattern = Pattern.compile("(?is)<span[^>]*>(?:" + breakLineTagPattern + ")*(.*?)</span>");
			String debtors = "";
			String securedParties = "";
			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Debtor"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					debtors = node.toHtml()
							.replaceAll("(?is)</?div[^>]*>", "")
							.replaceAll("(?is)<span[^>]*>.*?</span>" + breakLineTagPattern, "")
							.replaceAll("(?s)\\s+", " ")
							.trim();
				}
				if (StringUtils.isNotEmpty(debtors)) {
					Matcher nameMatcher = namePattern.matcher(debtors);
					while (nameMatcher.find()) {
						grantor += nameMatcher.group(1).replaceFirst("(?is)^(.*?)" + breakLineTagPattern + ".*", "$1");
						if (nameMatcher.group(1).matches("(?is).*Et\\s+Alibi.*")) {
							grantor += " ET AL";
						}
						grantor += "<br />";
					}
				}
			}

			list = nodeList.extractAllNodesThatMatch(new RegexFilter("Secured\\s+Party"), true);
			if (list.size() > 0) {
				Node node = ((Node) HtmlParser3.getFirstParentTag(list.elementAt(0), Div.class));

				if (node != null) {
					securedParties = node.toHtml()
							.replaceAll("(?is)</?div[^>]*>", "")
							.replaceAll("(?is)<span[^>]*>.*?</span>" + breakLineTagPattern, "")
							.replaceAll("(?s)\\s+", " ")
							.trim();
				}
				if (StringUtils.isNotEmpty(securedParties)) {
					Matcher nameMatcher = namePattern.matcher(securedParties);
					while (nameMatcher.find()) {
						grantee += nameMatcher.group(1).replaceFirst("(?is)^(.*?)" + breakLineTagPattern + ".*", "$1");
						if (nameMatcher.group(1).matches("(?is).*Et\\s+Alibi.*")) {
							grantee += " ET AL";
						}
						grantee += "<br />";
					}
				}
			}

			parseDocTypeILKaneRO(m, remarks, searchId);
			parseAddressILKaneRO(address, m, searchId);
			parseNamesILKaneRO(grantor, grantee, rsResponse, m, searchId);

			m.removeTempDef();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseNamesILKaneRO(String grantorString, String granteeString, String rsResponse, ResultMap m, long searchId) throws Exception {

		String names[] = { "", "", "", "", "", "" };
		String[] suffixes, type = { "", "" }, otherType = { "", "" };
		ArrayList<List> grantor = new ArrayList<List>();
		ArrayList<List> grantee = new ArrayList<List>();
		String breakLineTagPattern = "\\s*<br\\s*/?>\\s*";
		String spanOrDiv = "(?is)</?(?:span|div)[^>]*>";
		grantorString = grantorString.replaceAll(spanOrDiv, "").replaceFirst("(?is)Grantor\\s*:?" + breakLineTagPattern, "").trim();
		if (StringUtils.isNotEmpty(grantorString)) {
			grantorString = grantorString.replaceAll("&amp;", "&");
			String[] gtors = grantorString.split(breakLineTagPattern);
			grantorString = grantorString.replaceAll(breakLineTagPattern, " & ").replaceFirst("&\\s*$", "");

			for (int i = 0; i < gtors.length; i++) {
				if (gtors[i].contains("TRUST# LT")) {
					gtors[i] = gtors[i].replaceAll("^\\s*TRUST#\\s+", "");
					names[2] = gtors[i];
					GenericFunctions.addOwnerNames(gtors[i], names, "",
							"", type, otherType, true, true, grantor);
				} else {
					if (gtors[i].trim().matches("(?is)(TRUST)\\s*#\\s+(\\d+)")) {
						gtors[i] = gtors[i].replaceFirst("#", "").trim();
					}
					if (gtors[i].trim().endsWith(" LIVING")) {
						gtors[i] = gtors[i].replaceFirst("(?is)(.*?)(\\s+TRUST\\b#?)(.*)", "$1 $3$2").trim();
					} else {
						gtors[i] = gtors[i].replaceFirst("(?is)(.*?)(\\s+TRUST\\b#?)(.*)", "$3 $1$2").replaceFirst("#", "").trim();
					}
					gtors[i] = gtors[i].replaceFirst("(?is)^(TRUST\\b#?)(.*)", "$2 $1").replaceFirst("#", "").trim();
					names = StringFormats.parseNameNashville(gtors[i], true);

					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(gtors[i], names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), grantor);
				}
			}

			m.put("SaleDataSet.Grantor", grantorString);
			m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
		}

		granteeString = granteeString.replaceAll(spanOrDiv, "").replaceFirst("(?is)Grantees?\\s*:?" + breakLineTagPattern, "").trim();
		if (StringUtils.isNotEmpty(granteeString)) {
			granteeString = granteeString.replaceAll("&amp;", "&");
			String[] gtee = granteeString.split(breakLineTagPattern);
			granteeString = granteeString.replaceAll(breakLineTagPattern, " & ").replaceFirst("&\\s*$", "");

			for (int i = 0; i < gtee.length; i++) {

				if (gtee[i].contains("TRUST# LT")) {
					gtee[i] = gtee[i].replaceAll("^\\s*TRUST#\\s+", "");
					names[2] = gtee[i];
					GenericFunctions.addOwnerNames(gtee[i], names, "",
							"", type, otherType, true, true, grantee);
				} else {
					if (gtee[i].trim().matches("(?is)(TRUST)\\s*#\\s+(\\d+)")) {
						gtee[i] = gtee[i].replaceFirst("#", "").trim();
					}
					if (gtee[i].trim().endsWith(" LIVING")) {
						gtee[i] = gtee[i].replaceFirst("(?is)(.*?)(\\s+TRUST\\b#?)(.*)", "$1 $3$2").trim();
					} else {
						gtee[i] = gtee[i].replaceFirst("(?is)(.*?)(\\s+TRUST\\b#?)(.*)", "$3 $1$2").replaceFirst("#", "").trim();
					}
					gtee[i] = gtee[i].replaceFirst("(?is)^(TRUST\\b#?)(.*)", "$2 $1").replaceFirst("#", "").trim();
					names = StringFormats.parseNameNashville(gtee[i], true);

					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(gtee[i], names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), grantee);
				}
			}

			m.put("SaleDataSet.Grantee", granteeString);
			m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
		}

		GenericFunctions1.setGranteeLanderTrustee2(m, searchId, true);
		GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);

	}
	
	public static void parseAddressILKaneRO(String addressString, ResultMap m, long searchId) throws Exception {

		if (StringUtils.isNotEmpty(addressString)) {
			String breakLineTagPattern = "\\s*<br\\s*/?>\\s*";

			addressString = addressString.replaceAll("(?is)</?(?:span|div)[^>]*>", "").replaceFirst("(?is)Address\\s*:?" + breakLineTagPattern, "").trim();
			addressString = addressString.replaceAll("(?is)UNIT\\s*", "#").trim();

			String[] addressTokens = addressString.split(breakLineTagPattern);
			if (addressTokens.length > 1) {
				if (addressTokens[0].matches("(?is)(\\d+)(?:\\s+|\\s*-?\\s*)([A-Z])\\s+([SNWE]\\s+\\w+.*)")) {// 1848593
					addressTokens[0] = addressTokens[0].replaceAll("(?is)(\\d+)(?:\\s+|\\s*-?\\s*)([A-Z])\\s+([SNWE]\\s+\\w+.*)", "$1 $3 $2");
				}
				m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(addressTokens[0]));
				m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(addressTokens[0]));

				if (addressTokens[1].indexOf(" IL") != -1) {
					String city = addressTokens[1].substring(0, addressTokens[1].indexOf(" IL"));
					m.put("PropertyIdentificationSet.City", city);
					String zip = addressTokens[1].substring(addressTokens[1].indexOf(" IL") + 3, addressTokens[1].length());
					if (StringUtils.isNotEmpty(zip)) {
						m.put("PropertyIdentificationSet.Zip", zip.trim().replaceAll("(?is)([^-]+)-$", "$1"));
					}
				} else {
					m.put("PropertyIdentificationSet.City", addressTokens[1]);
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public static void partyNamesInterILKaneRO(ResultMap m, long searchId) throws Exception {
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		ArrayList<List> grantor = new ArrayList<List>();
		ArrayList<List> grantee = new ArrayList<List>();
		
		String grantorName = (String)m.get("tmpGrantor");
		if (StringUtils.isNotEmpty(grantorName)){
			m.put("SaleDataSet.Grantor", grantorName);
			names = StringFormats.parseNameNashville(grantorName, true);
			
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			
			GenericFunctions.addOwnerNames(grantorName, names, suffixes[0],
											suffixes[1], type, otherType,
											NameUtils.isCompany(names[2]),
											NameUtils.isCompany(names[5]), grantor);
		}
		
		
		String granteeName = (String) m.get("tmpGrantee");
		
		if (StringUtils.isNotEmpty(granteeName)){
			m.put("SaleDataSet.Grantee", granteeName); 
			names = StringFormats.parseNameNashville(granteeName, true);
			
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			
			GenericFunctions.addOwnerNames(granteeName, names, suffixes[0],
					suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), grantee);
		}
		
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
		
		GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);
	}
	
	public static void parseDocTypeInterILKaneRO(ResultMap m, long searchId) throws Exception{
		parseDocTypeILKaneRO(m, null, searchId);
	}
	
	public static String parseDocTypeILKaneRO(ResultMap m, String serverDocType, String remarksDetails, long searchId) throws Exception{
		String doctype = "";
		m.put("SaleDataSet.DocumentType", serverDocType);
		parseDocTypeILKaneRO(m, remarksDetails, searchId);
		
		doctype = (String) m.get("SaleDataSet.DocumentType");
		m = null;
		
		return doctype;
	}
	
	public static void parseDocTypeILKaneRO(ResultMap m, String remarks, long searchId) throws Exception{
		String doctype = (String) m.get("SaleDataSet.DocumentType");

		//	JGMT ?
		if (doctype != null){
			if (StringUtils.isNotEmpty(remarks)){
				String newDocType = doctype.trim();
				if (doctype.trim().equals("AMDT")){
					if (remarks.contains("MTG") || remarks.contains("MORTGAGE") || remarks.contains("TRUST DEED") || remarks.contains("LOAN") || remarks.contains("MASTER AMDT")){
						newDocType = "AMDT + MTG";
					} else if (remarks.contains("AOR")){
							newDocType = "AMDT + AOR";
					} else if (remarks.contains("COVENANTS") || remarks.contains("COVNTS") || remarks.contains("COVEN") || remarks.contains("COVE") || remarks.contains("COV")){
							newDocType = "AMDT + COVENANTS";
					} else if ((remarks.contains("ASGT")) && (remarks.contains("LEASE") || remarks.contains("LEASES") || remarks.contains("RENT") || remarks.contains("RENTS"))){
							newDocType = "AMDT + ASGT LEASE or RENTS";
					} else if (remarks.contains("ASGT")){
							newDocType = "AMDT + ASGT";
					} else if (remarks.contains("DEED")){
							newDocType = "AMDT + DEED";
					} else if (remarks.contains("LEASE")){
							newDocType = "AMDT + LEASE";
					} else if (remarks.contains("BYLAWS") || remarks.contains("BY-LAWS")){
							newDocType = "AMDT + BY-LAWS";
					} else if (remarks.contains("EASEMENT") || remarks.contains("EASE") || remarks.contains("ESMT") || remarks.contains("ROW")){
							newDocType = "AMDT + ESMT";
					} else if (remarks.contains("TRANSCRIPT")){
							newDocType = "AMDT + TRANSCRIPT";
					} else if (remarks.contains("PLAT")){
							newDocType = "AMDT + PLAT";
					} else if (remarks.contains("REST")){
							newDocType = "AMDT + REST";
					} else if (remarks.contains("DCLR")){
							newDocType = "AMDT + DCLR";
					} else if (remarks.contains("DECLN") || remarks.contains("DECL") || remarks.contains("DCLN")){
							newDocType = "AMDT + DECLN";
					} else if (remarks.contains("PARTNERSHIP")){
							newDocType = "AMDT + PARTNERSHIP";
					} else if (remarks.contains("AMDT")){
							newDocType = "AMDT + AMDT";
					} else {
							newDocType = "MISCELLANEOUS";
					}
					m.put("SaleDataSet.DocumentType", newDocType);
				}  
				if (doctype.trim().equals("EXT")){
					if (remarks.contains("MODIFICATION") || remarks.contains("MODIF") || remarks.contains("MOD")){
							newDocType = "EXT + MOD";
					} else if (remarks.contains("DEFERRAL") || remarks.contains("OF AGMT") || remarks.contains("OPTION")){
							newDocType = "EXT + MISC";
					} else if (remarks.contains("DEED")){
							newDocType = "EXT + DEED";
					}
					m.put("SaleDataSet.DocumentType", newDocType);
				}
				if (remarks.contains("RRCRD") || remarks.contains("RERCD") || remarks.contains("RERECORD") || remarks.contains("RERECD") || remarks.contains("REREC") 
							|| remarks.contains("RE-REC") || remarks.contains("RRCD")){
					if (doctype.trim().equals("QCD")){
							newDocType = "QCD + REREC";
					} else if (doctype.trim().equals("WD")) {
							newDocType = "WD + REREC";
					} else if (doctype.trim().equals("MTG")) {
							newDocType = "MTG + REREC";
					} else if (doctype.trim().equals("ASGT")) {
							newDocType = "ASGT + REREC";
					} else if (doctype.trim().equals("AOR")) {
							newDocType = "AOR + REREC";
					}
					m.put("SaleDataSet.DocumentType", newDocType);
				}
				if (remarks.contains("RENTS") || remarks.contains("RENT") || remarks.contains("LEASES") || remarks.contains("LEASE") || remarks.contains("AOR")){
					if (doctype.trim().equals("ASGT")){
						newDocType = "ASGT + RENT LEASE";
					} 
					m.put("SaleDataSet.DocumentType", newDocType);
				}
				if (doctype.trim().equals("MOD")){
					if (remarks.contains("MORTGAGE") || remarks.contains("MTG") || remarks.contains("TRUST DEED") || remarks.contains("LOAN") || remarks.contains("LN")
							|| remarks.contains("TRUST")){
						newDocType = "MOD + MTG";
					} else if (remarks.contains("LIEN") || remarks.contains("MECH") || remarks.contains("FINANCE STATEMENT") || remarks.contains("LIS PENDENS") || remarks.contains("LP")){
						newDocType = "MOD + LIEN";
					} 
					m.put("SaleDataSet.DocumentType", newDocType);
				}

			}
		}
	}
	
	private static void parseintermediarySubdivILKaneRO(ResultMap m) {
		try {
			String subs = (String) m.get("tmpSub");
			if (StringUtils.isEmpty(subs)) {
				return;
			}

			String blk = "", lot = "";
			subs = subs.replaceAll("(?is)/([\\w-\\s]+)/([\\w-\\s]+)$", "##$1##$2");// MOTE&RUN//NONE/1; ADMRS SUB/NONE/1; PEPVY 1//4/3
			String[] items = subs.split("##");

			m.put("PropertyIdentificationSet.SubdivisionName", items[0]);

			if (items.length >= 2 && StringUtils.isNotEmpty(items[1])) {
				if (!items[1].contains("NONE")) {
					blk += items[1] + " ";
				}
			}
			if (items.length == 3 && StringUtils.isNotEmpty(items[2])) {
				if (!items[2].contains("NONE")) {
					lot += items[2].replaceAll("\\s+", "#") + " ";// we have lot = 26 C, or lot = PCL 1 and in cleanValues() it will be transformed in C 26 and
																	// the search by subdiv will fail
				}
			}

			if (StringUtils.isNotEmpty(blk.trim())) {
				m.put("PropertyIdentificationSet.SubdivisionBlock", blk);
				blk = "";
			}
			if (StringUtils.isNotEmpty(lot.trim())) {
				lot = lot.replaceAll("#", " ");
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
				lot = "";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "rawtypes" })
	private static void parseintermediarySectionTRILKaneRO(ResultMap m) {
		try {
			String secTR = (String) m.get("tmpSecTwpRng");

			if (StringUtils.isEmpty(secTR)) {
				return;
			}

			List<List> body = new ArrayList<List>();
			List<String> line = new ArrayList<String>();

			String[] sTR = secTR.split("/");
			line.add(sTR[0]);

			if (sTR.length > 1) {
				line.add(sTR[1]);
			}
			else {
				line.add("");
			}
			if (sTR.length == 3) {
				line.add(sTR[2]);
			}
			else {
				line.add("");
			}
			body.add(line);

			GenericFunctions2.saveSTRInMap(m, body);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static ResultMap parseintermediaryResultsILKaneRO(String htmlRow, long searchId) {

		ResultMap m = new ResultMap();

		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(htmlRow);
			Node row = htmlParser3.getNodeList().elementAt(0);
			TableColumn[] columns = ((TableRow) row).getColumns();
			String documentNumber = "";
			String dateFiled = "";
			String docType = "";// the first 11 modules bring only the doctype code; 'UCC search' brings the full name
			String grantor = "";
			String grantee = "";
			String subBlockLot = "";
			String secTwnRng = "";
			String crossRefInstr = "";
			String remarks = "";
			String address = "";
			if (columns.length == 9) {

				documentNumber = columns[0].toPlainTextString().trim();
				dateFiled = columns[1].toPlainTextString().trim();
				docType = columns[2].toPlainTextString().trim();
				grantor = columns[3].toPlainTextString().trim();
				grantee = columns[4].toPlainTextString().trim();
				subBlockLot = columns[5].toPlainTextString().trim();
				secTwnRng = columns[6].toPlainTextString().trim();
				crossRefInstr = columns[8].toPlainTextString().trim();
				remarks = columns[8].toPlainTextString().trim();

				if (StringUtils.isNotEmpty(dateFiled)) {
					m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), dateFiled);
				}

				if (StringUtils.isNotEmpty(grantor)) {
					m.put("tmpGrantor", grantor);
				}
				if (StringUtils.isNotEmpty(grantee)) {
					m.put("tmpGrantee", grantee);
				}
				if (StringUtils.isNotEmpty(subBlockLot)) {
					m.put("tmpSub", subBlockLot);
					parseintermediarySubdivILKaneRO(m);
				}
				if (StringUtils.isNotEmpty(secTwnRng)) {
					m.put("tmpSecTwpRng", secTwnRng);
					parseintermediarySectionTRILKaneRO(m);
				}
				if (StringUtils.isNotEmpty(crossRefInstr)) {
					m.put(SaleDataSetKey.CROSS_REF_INSTRUMENT.getKeyName(), crossRefInstr);
				}
				if (StringUtils.isNotEmpty(remarks)) {
					parseDocTypeILKaneRO(m, remarks, searchId);
				}
			} else if (columns.length == 4) { // UCC search by original or specific doc no
				documentNumber = columns[0].toPlainTextString().trim();
				docType = columns[1].toPlainTextString().trim();
				grantor = columns[2].toPlainTextString().trim();
				address = columns[3].toPlainTextString().trim();

			}
			else if (columns.length == 5) {// UCC search by name

				documentNumber = columns[0].toPlainTextString().trim();
				String amendment = columns[1].toPlainTextString().trim();
				docType = columns[2].toPlainTextString().trim();
				grantor = columns[3].toPlainTextString().trim();
				address = columns[4].toPlainTextString().trim();
				if (StringUtils.isNotEmpty(amendment)) {
					m.put(SaleDataSetKey.CROSS_REF_INSTRUMENT.getKeyName(), amendment);
				}
			}
			
			if (StringUtils.isNotEmpty(documentNumber)) {
				m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
			}

			if (StringUtils.isNotEmpty(docType)) {
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
			}

			if (StringUtils.isNotEmpty(grantor)) {
				m.put("tmpGrantor", grantor);
			}

			if (StringUtils.isNotEmpty(address)) {
				parseAddressILKaneRO(address, m, searchId);
			}
			partyNamesInterILKaneRO(m, searchId);

			m.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return m;
	}
	
}

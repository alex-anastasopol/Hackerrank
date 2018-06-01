package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.functions.COGunnisonAO;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.COGenericTylerTechTR;

/**
 * @author vladb
 * 
 */
public class COGrandTR {
	
	public static void parseIntermediaryRow(COGenericTylerTechTR server, ResultMap resultMap, TableColumn[] cols) {
		for(int i=0; i <cols.length ; i++) {
			NodeList tag = HtmlParser3.getTag(cols[i].getChildren(), TextNode.class, true);
			switch(i) {
				case 0:
					if (tag.size() == 2){
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), tag.elementAt(0).toHtml());
						String amount = RegExUtils.getFirstMatch("[\\d+\\.\\,]+", tag.elementAt(1).toHtml(), 0);
						amount = StringUtils.cleanAmount(amount);
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amount);
					}
					break;
				case 2:
					if (tag.size() == 1){
						resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), tag.elementAt(0).toHtml());
					}
					break;
				case 3:
					if (tag.size() == 2) {
						resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), tag.elementAt(0).toHtml());
					}
					break;
			}
		}
		
		try {
			server.parseName(null, resultMap);
			server.parseAddress("", resultMap);
			server.parseLegal("", resultMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseNames(ResultMap resultMap, long searchId) {

		String ownerName = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		
		if(StringUtils.isEmpty(ownerName)) {
			return;
		}
		
		String properInfo = fixLines(ownerName);

		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("ENTRUST");

		Vector<String> excludeLast = new Vector<String>();
		excludeLast.add("LARA");

		Vector<String> excludeFirst = new Vector<String>();
		excludeFirst.add("SPENCER");

		String[] lines = properInfo.split("\n");
		
		for (int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		lines = processName(lines, companyExpr);
		for (int i = 0; i < lines.length; i++) {
			lines[i] = lines[i].replaceAll("\\A\\s*&?\\s*", "").replaceAll("\\s*&?\\s*\\z", "");
			lines[i] = lines[i].replaceAll("\\A\\s*,?\\s*", "");
		}

		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, excludeLast, excludeFirst,
				false, COFremontAO.ALL_NAMES_LF, -1);

		return;
	}

	// merge people names split on 2 lines, split company names placed on the same line
	public static String[] processName(String[] lines, Vector<String> companyExpr) {

		ArrayList<String> newLines = new ArrayList<String>();
		String crtLine = "";

		for (String line : lines) {
			line = " " + line + " ";
			String[] parts = line.split("&");
			if (COFremontAO.isCompany(line, companyExpr)) {
				if (crtLine.length() > 0) {
					newLines.add(crtLine);
				}
				// boolean allAreCompanies = true;
				// for(String part:parts) {
				// if(part.trim().split("\\s+").length <= 1 || !COFremontAO.isCompany(part, companyExpr)) {
				// allAreCompanies = false;
				// break;
				// }
				// }
				// if(allAreCompanies) {
				// for(String part:parts) {
				// if(part.length() > 0) {
				// newLines.add(part);
				// }
				// }
				// } else {
				newLines.add(line);
				// }
				crtLine = "";
			} else {
				parts = line.split("(&)|(C/O)");
				String firstPart = parts[0].trim();
				String pat = "\\A\\s*([A-Z]+,\\s+[A-Z]+\\s+[A-Z]),\\s+([A-Z]+\\s+[A-Z]+),\\s*$";
				if (firstPart.matches("\\w{2,}(\\s+\\w)*") || crtLine.matches("\\w{2,}(\\s*,)?")) {
					//DAHLSTROM, MARSHALL C, JUDITH ANN,
					Matcher ma1 = Pattern.compile(pat).matcher(crtLine);
					//JIM & JEANIE KEMP
					Matcher ma2 = Pattern.compile("\\A\\s*([A-Z]+)\\s*&\\s*([A-Z]+)\\s+([A-Z]+)\\s*$").matcher(line);
					if (ma1.matches()) {
						newLines.add(ma1.group(1));
						newLines.add(ma1.group(2));
						newLines.add(firstPart);
					} else if (ma2.matches()) {
						newLines.add(ma2.group(3) + " " + ma2.group(1));
					} else {
						newLines.add(crtLine + " " + firstPart);
					}
					crtLine = "";
				} else {
					Matcher ma3 = Pattern.compile(pat).matcher(line);
					if (ma3.matches()) {
						newLines.add(ma3.group(1));
						newLines.add(ma3.group(2));
						crtLine = "";
					} else {
						if (crtLine.length() > 0) {
							newLines.add(crtLine);
						}
						crtLine = firstPart;
					}
				}
				for (int i = 1; i < parts.length; i++) {
					if (crtLine.length() > 0) {
						newLines.add(crtLine);
					}
					crtLine = parts[i].trim();
				}
			}
		}
		if (crtLine.length() > 0) {
			newLines.add(crtLine);
		}

		return newLines.toArray(new String[newLines.size()]);
	}

	@SuppressWarnings("rawtypes")
	public static void parseLegalSummary(ResultMap resultMap, long searchId) {

		String legal = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		
		if(StringUtils.isEmpty(legal)) {
			return;
		}
		
		String sec = "";
		String twn = "";
		String rng = "";
		String phase = "";

		// extract sec-twn-rng
		List<List> body = new ArrayList<List>();
		Matcher m = Pattern.compile("\\bS(?:EC)?\\s*(\\d+)[,\\s]+T(\\d+[NWSE]{0,2})[,\\s]+R(\\d+[NWSE]{0,2})(\\W|\\z)").matcher(legal);

		while (m.find()) {
			sec = m.group(1);
			twn = m.group(2);
			rng = m.group(3);

			List<String> line = new ArrayList<String>();
			line.add(sec);
			line.add(twn);
			line.add(rng);
			body.add(line);

		}
		if (body.size() > 0) {
			try {
				GenericFunctions2.saveSTRInMap(resultMap, body);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// extract subdivision name
		m = Pattern.compile("(?is)\\A\\s*(?:Subd:)?(.+?)\\b(BLOCK|LOT|BLK|BLDG|UNIT|PHASE|LOTS|DESC)").matcher(legal);
		if (m.find()) {
			String subd = m.group(1);
			subd = subd.replaceAll("(?is)\\b(ALL)?\\s*[\\d.]+\\s*AC(\\s+IN)?\\s*", "");
			subd = subd.replaceAll("\\s{2,}", " ").trim();
			resultMap.put("PropertyIdentificationSet.SubdivisionName", subd);
			if (subd.matches("(?is).*\\bCONDO.*")) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subd);
			}
		}

		// extract lots
		String lot = (String) resultMap.get("PropertyIdentificationSet.SubdivisionLotNumber");
		if (lot == null) {
			lot = "";
		} else {
			lot += ",";
		}
		m = Pattern.compile("(?is)\\bLOT:?\\s+(\\w+)").matcher(legal);
		while (m.find()) {
			lot += m.group(1) + ",";
		}
		m = Pattern.compile("(?is)(?<=\\bLO?TS?:?)\\s+(\\d+|\\d*[A-Z])(\\s*(?:-|THRU)\\s*(\\d+|\\d*[A-Z]))?(?:\\s*,\\s*ALL\\s*|[&,+;) ]+|$)+").matcher(legal);
		while (m.find()) {
			lot += m.group(1);
			if (m.group(3) != null) {
				lot += "-" + m.group(3);
			}
			lot += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if (lot.length() > 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract block
		String blk = (String) resultMap.get("PropertyIdentificationSet.SubdivisionBlock");
		if (blk == null) {
			blk = "";
		} else {
			blk += ",";
		}
		m = Pattern.compile("(?is)\\bBL(?:OC)?K:?\\s+([A-Z0-9]+)\\b").matcher(legal);
		while (m.find()) {
			blk += m.group(1) + ",";
		}

		m = Pattern.compile("(?is)(?<=\\bBL(?:OC)?KS?:?)\\s+(\\d+|\\d*[A-Z])(\\s*(?:-|THRU)\\s*(\\d+|\\d*[A-Z]))?(?:[&,+; ]+|$)").matcher(legal);
		while (m.find()) {
			blk += m.group(1);
			if (m.group(3) != null) {
				blk += "-" + m.group(3);
			}
			blk += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if (blk.length() > 0) {
			blk = LegalDescription.cleanValues(blk, false, true);
			resultMap.put("PropertyIdentificationSet.SubdivisionBlock", blk);
		}
		
		// Extract bldg
		m = Pattern.compile("(?is)BLDG:?\\s*(\\w)").matcher(legal);
		if (m.find()) {
			String bldg = m.group(1).trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
		}
		
		// Extract tract
		m = Pattern.compile("(?is)TRACT:?\\s*(\\w)").matcher(legal);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			sb.append(m.group(1).trim()).append(" ");
		}
		String tract = sb.toString().trim();
		if (tract.length()>0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
		}
		
		// extract unit
		String unit = (String) resultMap.get("PropertyIdentificationSet.SubdivisionUnit");
		if (unit == null) {
			unit = "";
		} else {
			unit += ",";
		}
		m = Pattern.compile("(?is)\\bUNIT:?\\s*(\\w+)").matcher(legal);
		while (m.find()) {
			unit += m.group(1) + ",";
		}
		m = Pattern.compile("(?is)(?<=\\bUNITS?:?)\\s*(\\d+[A-Z]?|[A-Z])(\\s*(?:-|THRU)\\s*(\\d+[A-Z]?|[A-Z]))?(?:[&,+ ]+|$)").matcher(legal);
		while (m.find()) {
			unit += m.group(1);
			if (m.group(3) != null) {
				unit += "-" + m.group(3);
			}
			unit += ",";
			legal = m.replaceFirst(" ");
			m.reset(legal);
		}
		if (unit.length() > 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			resultMap.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		// extract phase
		m = Pattern.compile("(?is)\\bPHASE:?\\s*(\\w+)").matcher(legal);
		while (m.find()) {
			try {
				if (!m.group(1).matches("\\d+")) {
					phase += Roman.parseRoman(m.group(1)) + ",";
				} else {
					phase += m.group(1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (phase.length() > 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			resultMap.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}

		// extract cross refs from legal description
		try {
			List<List> bodyCR = new ArrayList<List>();
			m = Pattern.compile("\\bB/?(\\d+)\\s*(?:,|\\s+)\\s*P/?(\\d+)(\\s*-\\s*(\\d+))?(?:[&, ]+|$)").matcher(legal);
			while (m.find()) {

				int fromPage = Integer.valueOf(m.group(2));
				int toPage = fromPage;

				if (m.group(4) != null) {
					toPage = Integer.valueOf(m.group(4));
				}
				if (toPage < fromPage) {
					int aux = toPage;
					toPage = fromPage;
					fromPage = aux;
				}
				for (int i = fromPage; i <= toPage; i++) {
					List<String> line = new ArrayList<String>();
					line.add(m.group(1));
					line.add(String.valueOf(i));
					line.add("");
					if (!COGunnisonAO.isDuplicateRef(resultMap, line)) {
						bodyCR.add(line);
					}
				}
				legal = m.replaceFirst("B" + m.group(1) + " ");
				m.reset(legal);
			}

			m = Pattern.compile("#(\\d{4}) ?(\\d{4})").matcher(legal);
			while (m.find()) {
				List<String> line = new ArrayList<String>();
				line.add("");
				line.add("");
				line.add(m.group(1) + m.group(2));
				if (!COGunnisonAO.isDuplicateRef(resultMap, line)) {
					bodyCR.add(line);
				}
			}

			if (!bodyCR.isEmpty()) {
				String[] header = { "Book", "Page", "InstrumentNumber" };
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("Book", new String[] { "Book", "" });
				map.put("Page", new String[] { "Page", "" });
				map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });

				ResultTable cr = new ResultTable();
				cr.setHead(header);
				cr.setBody(bodyCR);
				cr.setMap(map);
				resultMap.put("CrossRefSet", cr);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String fixLines(String names) {

		String properNames = names;
		properNames = properNames.replaceAll("(?:\n\\s*)?(DA?TE?D)(?:\\s*\n)?", " $1 ");
		properNames = properNames.replaceAll("NCI-\\d+-\\d+-\\d+", ""); // NCI-001-03-80 (P704067)
		properNames = properNames.replaceAll("\\bBANK\\s+OF\\s*\n", "BANK OF ");
		properNames = properNames.replaceAll("\n\\s*((LIVING\\s+)?TRUST)", " $1"); // P307599
		properNames = properNames.replaceAll("(THE[\\w\\s,]*?)\n([\\w\\s,]*?TRUST)", "$1 $2"); // R056530
		properNames = properNames.replaceAll("C/O", "&\n"); // R148350

		properNames = properNames.replaceAll("\\s+AS\\s+TSTE\\s+OF\\s+THE\\s+", " FAKE_TRUSTEE \n");
		properNames = properNames.replaceAll("\\s+UAD\\s+\\d+/\\d+/\\d+", "");

		properNames = properNames.replaceAll("(?:,\\s*)?SUCCESSOR\\s+TO\\b", "\n");
		properNames = properNames.replaceAll("\\b((,|AND)\\s*)?(AS\\s+)?(CO-?)?T(RU)?STEE?S?(\\s*((OF)|(FOR)|(,)))?\\b", "\n"); // R011210, R193921
		properNames = properNames.replaceAll("\\b(,\\s*)?INDIVIDUALLY\\b", "\n"); // R193921
		properNames = properNames.replaceAll("\\b(,\\s*)?AS\\s*PR\\s*OF\\s*ESTATE?\\b", "\n"); // R193921
		properNames = properNames.replaceAll("THE\\s+BENEFIT\\s+OF", "\n"); // R037210

		properNames = properNames.replaceAll("\\s*\n\\s*", "\n");
		properNames = properNames.replaceAll(" +", " ");

		properNames = properNames.replace("FAKE_TRUSTEE", "TRUSTEE");
		
		properNames = properNames.replaceAll("(?is)&amp;", "&");
		properNames = properNames.replaceAll("&,", "\n");
		
		properNames = properNames.replaceAll("(?is)\\b([A-Z]+,\\s+[A-Z]+\\s+[A-Z]),\\s+([A-Z]+,\\s+[A-Z]+\\s+[A-Z])\\b", "$1 & $2");

		return properNames;
	}

	private static String cleanName(String name) {

		String properName = name;

		properName = properName.replaceAll("\\bET\\s*AL\\b", "");
		properName = properName.replaceAll("\\bATTN:?", "");
		// properName = properName.replaceAll("\\bC/O\\b", "");
		properName = properName.replaceAll(" {2,}", " ");
		properName = properName.replaceAll("(UND)?\\s*1/2\\s*INT?\\b", "");
		properName = properName.replaceAll("\\bET\\s*AL\\b", "");

		properName = properName.replaceAll("([A-Z]+)-(\\W|\\z)", "$1"); // SMITH, MICHAEL JOHN & CATHY A WALTON-

//		properName = NameCleaner.cleanName(properName);
		properName = properName.trim();

		return properName;
	}
	
	public static void parseAddress(ResultMap resultMap, long searchId) {

		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		
		if (StringUtils.isEmpty(address)) {
			return;
		}
		
		address = address.trim();
		
		String no = StringFormats.StreetNo(address);
		no = no.replaceAll("^0+", "");
		String name = StringFormats.StreetName(address);
		name = parseStreetName(name);
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), no);
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), name);
		
	}
	
	public static String parseStreetName(String address) {
		// CO RD 702 / VILLAGE --> COUNTY ROAD 702
		Matcher m = Pattern.compile("(/.*)").matcher(address);
		if (m.find()) {
			address = m.replaceAll("").trim();
		}
		address = address.replaceFirst("(?is)\\bCO\\s*RD\\s*(\\d+)", "COUNTY ROAD $1");	
		m = Pattern.compile("(\\bCO\\s*RD)").matcher(address);
		if (m.find()) {
			address = m.replaceFirst("COUNTY ROAD");
		}
		address = address.replaceFirst("(?is)\\bUS\\s*HWY\\s*(\\d+)", "US HIGHWAY $1");
		return address;
	}

}

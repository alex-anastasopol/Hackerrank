package ro.cst.tsearch.servers.functions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.utils.Roman;

public class FLOkeechobeeRV {
	public static Pattern crossRefRemarks = Pattern
			.compile("ORB (\\d+)\\s*PA?GE? (\\d+)");
	//public static Pattern crossRefRemarks2 = Pattern.compile("(\\d{3,3})(?:\\s+|\\-)(\\d{2,3})");

	public static Pattern lotRemarks = Pattern.compile("\\bL\\-(\\d+)");
	public static Pattern blockRemarks = Pattern
			.compile("\\bB\\-(\\d+|\\w{1,2})\\b");
	public static Pattern unitRemarks = Pattern
			.compile("\\bUNI?T? (?:NO |# ?)?([\\d\\w]+)\\b");

	public static void legalRemarksFLOkeechobeeRV(ResultMap m, long searchId)
			throws Exception {

		String legal = (String) m.get("SaleDataSet.Remarks");
		if (legal == null || legal.length() == 0
				|| legal.trim().matches(".*SS #.*"))
			return;
		//legal = legal.replaceAll("App Date:", "");
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics
		legal = GenericFunctions.replaceNumbers(legal);
		legal = legal.replaceAll("\\bOR BK\\b", "ORB");
		legal = legal.replaceAll("\\bLO\\b", "LOT");
		legal = legal.replaceAll("\\bUNIT LOT\\b", "LOT");
		legal = legal.replaceAll("(WD|QCD)", "");
		legal = legal.replaceAll("\\s{2,}", " ");
		legal = legal.replaceAll("(\\d+)\\s*(?:THROUGH|TO)\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("\\s*(&|\\bAND\\b)\\s*", " ");
		legal = legal.trim();
		Pattern p;
		Matcher ma;
		// extract and replace tract from legal description
		String tract = (String) m
				.get("PropertyIdentificationSet.SubdivisionTract");
		if (tract == null) {
			tract = "";
		}
		String patt = "([A-Z0-9-]|\\d+ )";
		p = Pattern.compile("\\bTR(?:ACTS?)? (" + patt + "+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract += " " + ma.group(1);
		}
		tract = LegalDescription.cleanValues(tract, false, true);
		if (tract.length() > 0) {
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}
		ma.reset();
		legal = ma.replaceAll("");

		//section-township-range
		// extract section, township and range from legal description
		List<List> body = new ArrayList<List>();
		body = GenericFunctions.goToGetSTRFromMapFromPIS(m, body);
		ArrayList<Pattern> secs = new ArrayList<Pattern>();
		//secs.add(Pattern.compile("((?:\\d+ )+\\d+)/(\\d+)/(\\d+)"));
		secs.add(Pattern.compile("\\bSEC ((?:\\d+ )+\\d+)/(\\d+)/(\\d+)"));
		secs.add(Pattern.compile("\\bSEC ((?:\\d+ )+)(\\d+)/(\\d+)"));
		secs.add(Pattern
				.compile("\\b((?:\\d+-\\d+)? \\d+-\\d+)/(\\d+)/(\\d+)\\b"));
		secs
				.add(Pattern
						.compile("\\bSEC (\\d+[NWSE]?)(?:-)(\\d+[NWSE]?)(?:-)(\\d+[NWSE]?)\\b"));
		secs
				.add(Pattern
						.compile("\\b(\\d+[NWSE]*?)(?:/)(\\d+[NWSE]*?)(?:/)(\\d+[NWSE]*?)\\b"));
		secs.add(Pattern.compile("\\bSECT?(?:ION)? ([\\d\\w]+)()()"));

		for (int i = 0; i < secs.size(); i++) {
			ma = secs.get(i).matcher(legal);
			while (ma.find()) {
				List<String> line = new ArrayList<String>();
				if (ma.group(1).equals("BLK")) {
					continue;
				}
				line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
				line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
				line.add(ma.group(3).replaceFirst("^0+(\\d+)", "$1"));
				body.add(line);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			}
			ma.reset();
			legal = ma.replaceAll("");
		}
		GenericFunctions2.goToSaveSTRInMap(m, body);

		//book and page
		Matcher bpFinal = crossRefRemarks.matcher(legal);
		;
		if (bpFinal.find()) {
			bpFinal.reset();
			List<List> bodyCR = new ArrayList<List>();
			while (bpFinal.find()) {
				List<String> line = new ArrayList<String>();
				line.add(bpFinal.group(1));
				line.add(bpFinal.group(2));
				line.add("");
				bodyCR.add(line);
			}
			ResultTable cr = (ResultTable) m.get("CrossRefSet");
			if (cr == null) {
				String[] header = { "Book", "Page", "InstrumentNumber" };
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("Book", new String[] { "Book", "" });
				map.put("Page", new String[] { "Page", "" });
				map.put("InstrumentNumber", new String[] { "InstrumentNumber",
						"" });
				cr = new ResultTable();
				cr.setHead(header);
				cr.setBody(bodyCR);
				cr.setMap(map);

			} else {
				String[][] b = cr.getBodyRef();
				for (int i = 0; i < b.length; i++) {
					List<String> line = new ArrayList<String>();
					line.add(b[i][0]);
					line.add(b[i][1]);
					line.add(b[i][2]);
					bodyCR.add(line);
				}
				cr.setBody(bodyCR);
			}
			m.put("CrossRefSet", cr);
			bpFinal.reset();
			legal = bpFinal.replaceAll("");
		}
		// extract and remove building # from legal description
		p = Pattern.compile("\\bB(?:UI)?LD(?:IN)?G ([A-Z])\\b");
		String building = (String) m
				.get("PropertyIdentificationSet.SubdivisionBldg");
		if (building == null) {
			building = "";
		}
		ma = p.matcher(legal);
		if (ma.find()) {
			building += " " + ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", building);
		}
		ma.reset();
		legal = ma.replaceAll("");

		//unit
		ma = unitRemarks.matcher(legal);
		String unit = (String) m
				.get("PropertyIdentificationSet.SubdivisionUnit");
		if (unit == null) {
			unit = "";
		}
		while (ma.find()) {
			unit += " " + ma.group(1);
		}
		unit = unit.replaceAll("NO", "");
		unit = LegalDescription.cleanValues(unit, false, true);
		if (!"".equals(unit)) {
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}
		ma.reset();
		legal = ma.replaceAll("");

		//lot

		p = Pattern
				.compile("\\bL(?:O?TS?\\s*)?(?:NO )?([-\\d\\s,&]+([A-Z])?)\\b");
		ma = p.matcher(legal);
		String lot = (String) m
				.get("PropertyIdentificationSet.SubdivisionLotNumber");
		if (lot == null) {
			lot = "";
		}
		while (ma.find()) {
			lot += " " + ma.group(1);
		}
		lot = LegalDescription.cleanValues(lot, false, true);
		if (!"".equals(lot)) {
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		ma.reset();
		legal = ma.replaceAll("");

		//block
		p = Pattern
				.compile("\\bB(?:L?(?:OC)?KS?\\s*)?((\\d+,?|\\b[A-Z]{1,2}\\b|[A-Z]\\b|-|\\s)+)");
		ma = p.matcher(legal);
		String block = (String) m
				.get("PropertyIdentificationSet.SubdivisionBlock");
		if (block == null) {
			block = "";
		}
		while (ma.find()) {
			block += " " + ma.group(1);
		}
		block = block.replaceAll("\\b(OF|EL)\\b", "");
		block = LegalDescription.cleanValues(block, false, true);
		if (!"".equals(block)) {

			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		ma.reset();
		legal = ma.replaceAll("");

		//phase
		// extract phase from legal description
		String phase = (String) m
				.get("PropertyIdentificationSet.SubdivisionPhase");
		if (phase == null) {
			phase = "";
		}
		p = Pattern.compile("\\bPH(?:ASES?)?(( (\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase += " " + ma.group(1).trim();
			phase = LegalDescription.cleanValues(phase, false, true);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}

	}

	/**
	 * easy way to save results
	 */
	public static void legalFLOkeechobeeDASLRV(ResultMap m, long searchId, int a)
			throws Exception {

		if (m.get("tmpInter") != null) {
			return;
		}
		String remarks = (String) m.get("SaleDataSet.Remarks");
		String legal = (String) m
				.get("PropertyIdentificationSet.PropertyDescription");
		boolean saveLegal = false;
		boolean saveRem = false;

		if (legal != null && legal.length() > 0 && !legal.equals(remarks)) {
			saveLegal = true;
		}
		if (remarks != null && remarks.length() > 0) {
			saveRem = true;
		}

		boolean save = false;
		String pin = getPin(m);
		//String o = legal;
		String filename = "flokeechobeerv";
		String contain = ""; // if the same function is used for inter, then
		// what substring should be in o to save it
		if (m.get("saving") == null && save) {
			try {
				if (saveLegal) {
					FileWriter fw = new FileWriter(new File(
							"/home/danut/Desktop/work/parsing/" + filename
									+ "/" + filename + "_legal.html"), true);
					fw.write(legal + "\n");
					fw
							.write("\n=============================================================="
									+ pin + "\n");
					fw.close();
				}
				if (saveRem) {
					FileWriter fw = new FileWriter(new File(
							"/home/danut/Desktop/work/parsing/" + filename
									+ "/" + filename + "_rem.html"), true);
					fw.write(remarks + "\n");
					fw
							.write("\n=============================================================="
									+ pin + "\n");
					fw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			//}

		}
		//done saving
	}

	public static void legalFLOkeechobeeDASLRV(ResultMap m, long searchId)
			throws Exception {

		String legal = (String) m
				.get("PropertyIdentificationSet.PropertyDescription");
		String remarks = (String) m.get("SaleDataSet.Remarks");
		if (legal == null || legal.length() == 0)
			return;
		if (!legal.equals(remarks)) {
			legal = legal.replaceAll("&", " ");
			legal = legal.replaceAll("(\\d+) TO (\\d+)", "$1-$2");
			String origLegal = legal;
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			legal = Roman.normalizeRomanNumbersExceptTokens(legal,
					exceptionTokens); // convert roman numbers to arabics
			legal = GenericFunctions.replaceNumbers(legal);
			Matcher ma;
			Pattern p;

			// extract and replace tract from legal description
			String tract = "";
			String patt = "[A-Z0-9]";
			p = Pattern.compile("\\bTR(?:ACTS?)? (" + patt + "+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				tract = ma.group(1);
				m.put("PropertyIdentificationSet.SubdivisionTract", tract);
				ma.reset();
				legal = ma.replaceAll("");
			}

			// extract and replace phase from legal description
			String phase = "";
			patt = "(\\d+|[A-Z]\\d*)";
			p = Pattern.compile("\\bPH(?:ASE)? (" + patt + ")\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				phase = ma.group(1);
				String[] exceptionTokens1 = { "M", "C", "L", "D" };
				phase = Roman.normalizeRomanNumbersExceptTokens(phase,
						exceptionTokens1); // convert roman numbers to arabics
				m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
				if (ma.start() != 0) {
					legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b",
							"PHASE ");
				} else {
					legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
				}
				legal = legal.trim().replaceAll("\\s{2,}", " ");
			}
			// extract lot from legal description
			String lot = ""; // can have multiple occurrences
			p = Pattern.compile("\\bLOTS?\\s+([-\\d\\s,&]+([A-Z])?)\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				lot = lot + " " + ma.group(1);
			}
			ma.reset();
			legal = ma.replaceAll("");
			legal = legal.replaceAll(",\\s*", " ");
			p = Pattern.compile("\\bLOTS?\\s+([A-Z]( [A-Z])*)\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				lot = lot + " " + ma.group(1);
			}
			ma.reset();
			legal = ma.replaceAll("");
			lot = lot.trim();
			lot = lot.replaceFirst("\\-$", "");
			if (lot.length() != 0) {
				lot = LegalDescription.cleanValues(lot, false, true);
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			}

			// extract and remove section, township and range from legal description	   
			List<List> body = GenericFunctions2.goToGetSTRFromMap(m); //first add sec-twn-rng extracted from XML specific tags, if any (for DT use)
			Vector<Pattern> ps = new Vector<Pattern>();
			ps.add(Pattern
					.compile("SECTION (\\d+).*TOWNSHIP (\\d+).*RANGE (\\d+)?"));
			ps.add(Pattern.compile("SECTION (\\d+).*TOWNSHIP (\\d+).*()"));
			ps
					.add(Pattern
							.compile("\\bSEC(?:TIONS?)? ([0-9A-Z]+)(?:[\\s-]+(\\d+)[\\s-]+(\\d+))?\\b"));
			ps
					.add(Pattern
							.compile("\\b([0-9]+[EWSN]?) ([0-9]+[EWSN]) ([0-9]+[EWSN])\\b"));
			for (int i = 0; i < ps.size(); i++) {
				ma = ps.get(i).matcher(legal);
				while (ma.find()) {
					List<String> line = new ArrayList<String>();
					String sec = ma.group(1);
					if (sec.matches("LINE|COR")) {
						continue;
					}
					String twn = ma.group(2);
					String rng = ma.group(3);
					if (twn == null) {
						twn = "";
					}
					if (rng == null) {
						rng = "";
					}
					line.add(sec);
					line.add(twn);
					line.add(rng);
					body.add(line);
					//legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
					//legal = legal.trim().replaceAll("\\s{2,}", " ");
				}
				ma.reset();
				legal = ma.replaceAll("");
			}
			GenericFunctions2.goToSaveSTRInMap(m, body);

			// extract plat book & page from legal description
			p = Pattern.compile("\\bPB (\\d+) PG (\\d+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
				m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
				legal = ma.replaceAll("");
			} else {
				p = Pattern.compile("\\b(?:PLATS|PB|REC) (\\d+)-(\\d+)\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
					m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
					legal = ma.replaceAll("");
				}
			}

			//	 extract cross refs from legal description
			List<List> bodyCR = new ArrayList<List>();
			p = Pattern.compile("\\b(?:OR|DB|BOOK)\\s+(\\d+)(?:-(\\d+))?\\b");
			ma = p.matcher(legal);
			String docPage = (String) m.get("SaleDataSet.Page");
			if (docPage == null) {
				docPage = "";
			}
			String docBook = (String) m.get("SaleDataSet.Book");
			if (docBook == null) {
				docBook = "";
			}
			while (ma.find()) {
				String book = ma.group(1);
				String page = ma.group(2);
				if (page == null) {
					page = "";
				}
				//we might have OR 972 P 120,121
				page = page.replaceAll(",$", "");
				page = page.replaceAll("^,", "");
				page = page.replaceAll("&", ",");
				page = page.replaceAll("\\-.*", "");
				page = page.trim();
				page = page.replaceAll("\\s*,\\s*", ",");
				String[] pages = page.split(",|\\s");
				for (int jj = 0; jj < pages.length; jj++) {

					if (!docBook.equals(book)
							&& (pages[jj].equals("") || !docPage
									.equals(pages[jj]))) {
						List<String> line = new ArrayList<String>();
						line.add(book);
						line.add(pages[jj]);
						line.add("");
						bodyCR.add(line);
					}
				}
			}
			ma.reset();
			if (ma.find()) {
				legal = ma.replaceAll("");
			}

			if (!bodyCR.isEmpty()) {
				String[] header = { "Book", "Page", "InstrumentNumber" };
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("Book", new String[] { "Book", "" });
				map.put("Page", new String[] { "Page", "" });
				map.put("InstrumentNumber", new String[] { "InstrumentNumber",
						"" });

				ResultTable cr = new ResultTable();
				cr.setHead(header);
				cr.setBody(bodyCR);
				cr.setMap(map);
				m.put("CrossRefSet", cr);
			}

			// extract and remove building # from legal description
			p = Pattern.compile("\\bBUILDING ([A-Z])\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			}
			ma.reset();
			legal = ma.replaceAll("");

			// extract unit from legal description
			p = Pattern
					.compile("\\bUNITS?(?: NO)?\\s+([-\\d\\s]+|\\d+-?[A-Z]|[A-Z\\d-]+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1)
						.replaceFirst("^0+(\\w.*)", "$1"));
				legal = ma.replaceAll("");
			}

			// extract block from legal description
			String block = ""; // can have multiple occurrences
			p = Pattern
					.compile("\\bBL(?:OC)?KS?\\s+((\\d+,?|\\b[A-Z]{1,2}\\b|[A-Z]\\b|-|\\s)+)");
			ma = p.matcher(legal);
			String firstBlk = "";
			while (ma.find()) {

				String newBlk = ma.group(1).replaceAll("IN|OF.*", "");
				if (block.length() == 0) {
					firstBlk = newBlk;
					block = firstBlk;
				} else if (firstBlk.trim().matches("\\d+")
						&& newBlk.matches(".*[A-Z].*")) {

				} else {
					block = block + " " + newBlk;
				}
			}
			block = block.trim();
			block = block.replaceFirst("\\-$", "");
			if (block.length() != 0) {
				block = LegalDescription.cleanValues(block, false, true);
				m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			}
			ma.reset();
			legal = ma.replaceAll("");
			/*
			// extract subdvision name
			String subdiv = "";
			legal = origLegal
					.replaceAll(
							"\\b(BEING|BEG(IN)?|COMM?|ALL (THAT PART )?OF|A PARCEL|COMMENCING)\\b.*",
							"");
			{
				p = Pattern
						.compile("^([A-Z\\s&0-9\\.]+?)\\s*(PHASE|SECTION|CONDOMINIUM|UNIT|SUB(DIVISION)?|ADDN|ADDITION|LOTS?)(\\b)");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}

			subdiv = subdiv.trim();
			if (subdiv.length() != 0) {
				subdiv = subdiv.replaceAll(",", "");
				subdiv = subdiv.replaceAll("\\b(SEC|LOT).*", "");
				subdiv = subdiv.replaceAll(".*\\bIN\\s", "");
				subdiv = subdiv.replaceAll("\\.", "");
				subdiv = subdiv.trim();
				subdiv = subdiv.replaceAll("\\s{2,}", " ");
			}
			if (subdiv.length() != 0) {
				m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				if (origLegal.trim().matches(".*\\bCO?NDO(MINIUM)?\\b.*")) {
					m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
				}
			}
			*/
		}
		legalRemarksFLOkeechobeeRV(m, searchId);
	}

	public static void writeNames(ResultTable rt, String pin, String type) {
		String filename = "flokeechobeerv";

		try {
			FileWriter fw = new FileWriter(new File(
					"/home/danut/Desktop/work/parsing/" + filename + "/"
							+ filename + "_names.html"), true);
			String[][] b = rt.getBodyRef();
			for (int i = 0; i < b.length; i++) {
				fw.write(type + "@@" + b[i][0] + "@@" + b[i][1] + "@@"
						+ b[i][2] + "@@" + b[i][3] + "\n");
			}
			fw.write("\n===============================================" + pin
					+ "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static String getPin(ResultMap m) {
		String pin = (String) m.get("PropertyIdentificationSet.ParcelID");
		if (pin == null) {
			pin = (String) m.get("SaleDataSet.InstrumentNumber");
			if (pin == null) {
				pin = "";
			}
		}
		return pin;
	}

}

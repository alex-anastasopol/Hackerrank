package ro.cst.tsearch.servers.functions;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * parsing fuctions for ILLakeTR.
 * 
 * @author mihaib
 */

public class ILLakeTR {
	
	@SuppressWarnings("rawtypes")
	public static void taxILLakeTR(ResultMap m, long searchId) throws Exception {
		String firstInstallment = (String) m.get("tmpFirstInstallmentAmountDue");
		String firstInstallmentDueDate = (String) m.get("tmpFirstInstallmentDueDate");
		String secondInstallment = (String) m.get("tmpSecondInstallmentAmountDue");
//		String secondInstallmentDueDate = (String) m.get("tmpSecondInstallmentDueDate");
		String totalDue = "";
		Date dateNow = new Date();
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
		
		//tax year
		NodeList yNodes = new HtmlParser3((String) (m.get("tmpCupon")!=null ? m.get("tmpCupon") : "") ).getNodeList();
		
		if(yNodes.size()>0){
			yNodes = yNodes.extractAllNodesThatMatch(new TagNameFilter("TR"));
			if(yNodes.size()>0){
				TableRow r = (TableRow) yNodes.elementAt(0);
				if(r.getColumnCount()==4){
					String year = r.getColumns()[3].toPlainTextString().trim();
					year = year.replaceAll("(?ism).*Tax Year\\s(\\d+).*", "$1");
					if(year.length() == 4){
						m.put(TaxHistorySetKey.YEAR.getKeyName(), year);
					}
				}
			}
		}
		
		if (!StringUtils.isEmpty(firstInstallment)) {
			if (!StringUtils.isEmpty(firstInstallmentDueDate)) {
				if (dateNow.before(df.parse(firstInstallmentDueDate))) {
					totalDue = firstInstallment;
				} else {
					if (!StringUtils.isEmpty(secondInstallment)) {
						totalDue = secondInstallment;
					}
				}
			}
		}
		
		if (StringUtils.isEmpty(firstInstallment) && StringUtils.isEmpty(secondInstallment)) {
			totalDue = (String) m.get("tmpTotalDue");
		}
	
		if (StringUtils.isNotEmpty(totalDue))
			m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
		
		
		//amount paid
		NodeList apNodes = new HtmlParser3((String) (m.get("tmpAmtPaid")!=null ? m.get("tmpAmtPaid") : "") ).getNodeList();
		
		if(apNodes.size()>0){
			apNodes = apNodes.extractAllNodesThatMatch(new TagNameFilter("TR"));
			if(apNodes.size()>0){
				TableRow r = (TableRow) apNodes.elementAt(0);
				if(r.getColumnCount()==7){
					String totalBalance = r.getColumns()[6].toPlainTextString().trim();
					totalBalance = totalBalance.replaceAll("(?ism)[$,-]", "");
					if(StringUtils.isNotEmpty(totalBalance)){
						double valOfAP = Double.valueOf(totalBalance) - Double.valueOf(totalDue);
						m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), Double.toString(valOfAP));
					}
				}
			}
		}
		
		

		ResultTable newRT = new ResultTable();
		List<String> line1 = new ArrayList<String>();
		List<String> line2 = new ArrayList<String>();
		List<List> bodyRT = new ArrayList<List>();
		String amountPaid = (String) m.get("TaxHistorySet.AmountPaid");
		String amount = "0.00";
		
		try {
        	NumberFormat nf = NumberFormat.getInstance(Locale.US); 
        	nf.setGroupingUsed(true);
        	nf.setMinimumFractionDigits(2);
        	nf.setMaximumFractionDigits(2);
        	amount = nf.format(Double.parseDouble(amountPaid) / 2);
        }catch(Exception ignored) {}
        
         
		
		if ( StringUtils.isNotEmpty(firstInstallment) && !firstInstallment.equals("0.00")){
			line1.add(firstInstallment);
			line1.add("0.00");
			line1.add("0.00");
			line1.add(firstInstallment);
			line1.add("UNPAID");
		} else {
			if (StringUtils.isNotEmpty(secondInstallment) && !secondInstallment.equals("0.00")){
				line1.add(amountPaid);
				line1.add("0.00");
				line1.add(amountPaid);
				line1.add("0.00");
				line1.add("PAID");
			} else{
				line1.add(amount);
				line1.add("0.00");
				line1.add(amount);
				line1.add("0.00");
				line1.add("PAID");
			}
		}
		if (StringUtils.isNotEmpty(secondInstallment) && !secondInstallment.equals("0.00")){
			line2.add(secondInstallment);
			line2.add("0.00");
			line2.add("0.00");
			line2.add(secondInstallment);
			line2.add("UNPAID");
		} else{
			line2.add(amount);
			line2.add("0.00");
			line2.add(amount);
			line2.add("0.00");
			line2.add("PAID");
		}
		
		if (line1 != null){
			bodyRT.add(line1);
			if (line2 != null){
			   bodyRT.add(line2);
			}
		}
		if (bodyRT.size() > 0){
			String[] header = { "BaseAmount", "PenaltyAmount", "AmountPaid", "TotalDue", "Status"};
			newRT = GenericFunctions1.createResultTableFromList(header, bodyRT);
			m.put("TaxInstallmentSet", newRT);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void legalILLakeTR(ResultMap m, String legal) throws Exception {
		
		// initial corrections and cleanup of legal description
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers

		legal = legal.replaceAll("(?is)(?:\\s*&\\s*)[\\s\\d\\.%&]+\\s*\\bINT\\b", "");
		legal = legal.replaceAll("(?is)\\bIN\\b\\s+\\bCOM(?:MON)?\\b\\s+ELEM(?:ENTS)?\\b(?:\\s+\\bIN\\b(?:\\s+THE\\b)?[\\w\\s]+\\bCONDO(?:MINIUM)?)?", "");
		legal = legal.replaceAll("(\\d+)\\s+&\\s+(\\d+)", "$1&$2");
		legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		legal = legal.replaceAll("\\bNO\\s*\\.?\\s*(\\d+)\\b", "$1");
		legal = legal.replaceAll(";", " ");
		legal = legal.replaceAll("\\s{2,}", " ").trim();
		
		String legalTemp = legal;

		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LT|LOT)S?\\s*([\\d&-]+|[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLK|BLOCK)S?\\s*([\\d&]+|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UNIT)S?\\s*([\\d-A-Z]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(2);
			unit = unit.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);// ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("(?is)\\b(BLDG|BUILDING)S?\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(2);
			bldg = bldg.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PHASE)S?\\s*(\\d+[A-Z]*|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = phase + " " + ma.group(2);
			phase = phase.trim();
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
//		String sec = "";
		p = Pattern.compile("(?is)\\bSECTION\\s*(\\d+)\\s*,?\\s*TOWNSHIP\\s*(\\d+)\\s*,?\\s*RANGE\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionTownship", ma.group(2));
			m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(3));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		} else {
			ma.reset();
			p = Pattern.compile("(?is)\\b(SEC(?:TION|\\.)?)S?\\s*([\\d&]+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
				m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2));
				legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
				legal = legalTemp;
			}
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\b[\\s|,|&|-]+(\\d[\\dA-Z]{5,})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1));
			bodyCR.add(line);
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
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		String subdiv = "";
		p = Pattern.compile("(?:\\s*)?(.*)(\\n)(UNIT|PHASE|BLOCK|BLK|LOT)\\b*");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			//				\\b(?:\\s*)?(.+?)(?:SEC|PHASE|LOT|BLOCK|EST|ESTATES?|SUB|RESUB|UNIT)\\b
			p = Pattern.compile("(?:SEC\\.?|PHASE|LOT|BLOCK|\\:wEST|ESTATES?|SUB(?:DIVISION)?\\.?|RESUB|UNIT)");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				subdiv = legal.replace(ma.group(0), " ").trim();
			}
		}
		subdiv = subdiv.replaceAll("\\bPT\\b.*\\bIN\\b", "");
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceFirst("(.*)(\\d)(ST\\.?|ND\\.?|RD\\.?|TH\\.?)\\s*(ADDN)?", "$1" + "$2");
			subdiv = subdiv.replaceFirst("(.*?)\\s*(PARCEL|PHASE|LOT).*", "$1");
			subdiv = subdiv.replaceFirst("(.*)(ADD)", "$1");
			subdiv = subdiv.replaceFirst("(.*)(FT.*)", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\bPER\\b.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\bCONDO\\s+\\d+\\s*\\z", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+(A)?\\s+CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) SUB\\.?(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.*)\\s+(ALSO.*)", "$1");
			subdiv = subdiv.replaceFirst("UNIT\\s*(?:IN)?\\s+(.*)", "$1");
			subdiv = subdiv.replaceFirst("(.+)\\s+(E|W|N|A)\\b\\d+/\\d+.*", "$1");
			subdiv = subdiv.replaceFirst("(?is)(.*)-$", "$1");
			subdiv = subdiv.trim();
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			if (legal.matches(".*\\b(CONDO(MINIUM)?S?)\\b.*"))
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesILLakeTR(ResultMap m, String s) throws Exception {
		
		s = s.replaceAll("(?is)&nbsp", " ");
		s = s.replaceAll("\\b(,|(?:CO)?-?)\\s*TRUSTEES?\\b", "");
		s = s.replaceAll("TTEES?", "");
		s = s.replaceAll("\\bMR(\\s*&\\s*MRS)?\\b", "");
		s = s.replaceAll("\\bDTD\\s\\d+/\\d+/\\d+", "");
		s = s.replaceAll("\\s*-\\s*", "-");
		s = s.replaceAll("(?is)(SMITH)\\s+(\\w+\\s+\\w+)", "$1 & $2"); //G SMITH A SMITH-GRAVES
		s = s.replaceAll("(?is)(DEPASS)\\s+(\\w+\\s+\\w+)", "$1 & $2");//M DEPASS J SMITH
		s = s.replaceAll("(?is)(SMITHERMAN)\\s+(\\w+\\s+\\w+)", "$1 & $2");//B SMITHERMAN J SMITHERMAN
		s = s.replaceAll("(?is)(DONOHUE)\\s+(\\w+\\s+\\w+)", "$1 & $2");//L DONOHUE A SMITH
		s = s.replaceAll("(?is)(COLLINS)\\s+(\\w+\\s+\\w+)", "$1 & $2");//T COLLINS R SMITH
		s = s.replaceAll("(?is)(KRANDEL)\\s+(\\w+\\s+\\w+)", "$1 & $2");//J KRANDEL M SMITH
		s = s.replaceAll("(?is)(MALICKI)\\s+(\\w+\\s+\\w+)", "$1 & $2");//S MALICKI L SMITH&nbsp&nbsp
		s = s.trim();
		String[] owners;
		owners = s.split("&");

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] auxNames = { "", "", "", "", "", "" };
		String[] suffixes;
		boolean isFirst = true;
		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];
			if (ow.contains(",") && (!ow.matches(".*\\b(?:JR|SR|DR|[IV]{2,}).*"))) {
				names = StringFormats.parseNameDavidsonAO(s);
				isFirst = false;
			} else {
				ow = ow.replaceAll(",", "");
				ow = ow.replaceAll("\\bDR\\b", "");
				names = StringFormats.parseNameDesotoRO(ow);
				try {
					if (!NameUtils.isCompany(names[2])) {
						if (names[2].length() == 0 && isFirst) {
							auxNames = StringFormats.parseNameDesotoRO(owners[i + 1]);
							names[2] = auxNames[2];
						} else if (names[0].length() == 0 && names[1].length() == 0 && names[2].length() != 0 && isFirst) {
							auxNames = StringFormats.parseNameDesotoRO(owners[i + 1]);
							names[0] = names[2];
							names[2] = auxNames[2];
						} else if (names[0].length() != 0 && names[1].length() == 0 && names[2].length() == 1 && isFirst) {
							auxNames = StringFormats.parseNameDesotoRO(owners[i + 1]);
							names[1] = names[2];
							names[2] = auxNames[2];
						}
					} 
				}catch (ArrayIndexOutOfBoundsException e) {
					e.printStackTrace();
					System.out.println(s);
				}
			}
			names[2] = names[2].replaceAll(",", "");
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body);
	}
}

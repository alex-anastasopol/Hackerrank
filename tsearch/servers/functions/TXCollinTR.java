package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class TXCollinTR {
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID, String crtCounty) {
		
		try {		
			
			// swap pin (Account No.) with apd (PIDN)
			String pid = (String) m.get(PropertyIdentificationSetKey.PARCEL_ID.getKeyName());
			String apd = (String) m.get(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName());
			if (StringUtils.isNotEmpty(apd)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), apd);
			}
			if (StringUtils.isNotEmpty(pid)) {
				m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), pid);
			}
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String taxTable = "";
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			for (int k = 0; k < tables.size(); k++){
				if (tables.elementAt(k).toHtml().contains("Levy Amount")){
					taxTable= tables.elementAt(k).toHtml();
				}
			}
			String currentTaxYear = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(mainList, "Values"), "", true);
			currentTaxYear = currentTaxYear.replaceAll("(?is)\\A\\s*(\\d+)\\s*Values\\s*$", "$1");
			m.put("TaxHistorySet.Year", currentTaxYear);
			
			if (StringUtils.isNotEmpty(taxTable)){
				try{
				org.htmlparser.Parser parserO = org.htmlparser.Parser.createParser(taxTable, null);
				NodeList taxTableList = parserO.parse(null);
				TableTag mainTable = (TableTag) taxTableList.elementAt(0);
				TableRow[] rows = mainTable.getRows();
				//int lastYear = 0;
				List<List> body = new ArrayList<List>();
				List<String> line = null;
				String priorDelinq = "";
				
				for (int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					if (row.getColumnCount() == 10) {
						TableColumn[] cols = row.getColumns();
						if (cols[0].getChild(0).toHtml().trim().matches("(?is)\\d+\\s+totals")){
							String taxYear = cols[0].toPlainTextString().replaceAll("(?is)totals", "").trim();
							if (StringUtils.isEmpty(taxYear)){
								throw new RuntimeException("Exception when attempt to obtain the tax year!");
							}
							if (taxYear.equals(currentTaxYear)){
								line = new ArrayList<String>();
								
								String amountPaid = cols[3].toPlainTextString().trim();
								amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
								String receiptDate = rows[i-1].getColumns()[9].toPlainTextString().trim();
								if (StringUtils.isNotEmpty(receiptDate)){
									line.add(amountPaid);
									line.add(receiptDate);
									body.add(line);
								}
									
								m.put("TaxHistorySet.AmountPaid", StringUtils.isNotEmpty(amountPaid) ? amountPaid : "");
									
								String baseAmount = cols[2].toPlainTextString().trim();
								baseAmount = baseAmount.replaceAll("(?is)[\\$,]+", "");
								m.put("TaxHistorySet.BaseAmount", StringUtils.isNotEmpty(baseAmount) ? baseAmount : "");
		
								String totalDue = cols[8].toPlainTextString().trim();
								totalDue = totalDue.replaceAll("(?is)[\\$,]+", "");
								m.put("TaxHistorySet.TotalDue", StringUtils.isNotEmpty(totalDue) ? totalDue : "");
								//lastYear++;
							} else {
								line = new ArrayList<String>();
									
								String amountPaid = cols[3].toPlainTextString().trim();
								amountPaid = amountPaid.replaceAll("(?is)[\\$,]+", "");
								String receiptDate = rows[i-1].getColumns()[9].toPlainTextString().trim();
								if (StringUtils.isNotEmpty(receiptDate)){
									line.add(amountPaid);
									line.add(receiptDate);
									body.add(line);
								}
									
								priorDelinq += "+" + cols[8].toPlainTextString().trim();
							}
						}
					}
				}
				
				if (StringUtils.isEmpty((String) m.get("TaxHistorySet.Year"))){
					TableColumn[] cols = rows[1].getColumns();
					m.put("TaxHistorySet.Year", cols[0].toPlainTextString().trim());
				}
				
				m.put("TaxHistorySet.PriorDelinquent", GenericFunctions2.sum(priorDelinq.replaceAll("(?is)[\\$,]+", ""), searchId));
				ResultTable rt = new ResultTable();
				String[] header = {"ReceiptAmount", "ReceiptDate"};
				rt = GenericFunctions2.createResultTable(body, header);
				m.put("TaxHistorySet", rt);
				
				} catch (Exception e) {
						e.printStackTrace();
				}
			}
			
			try {
				if ("Collin".equals(crtCounty)){
					partyNamesTXCollinTR(m, searchId);
					parseLegalTXCollinTR(m, searchId);
				} else if ("Tarrant".equals(crtCounty) || "Denton".equals(crtCounty)){
					partyNamesTXCollinTR(m, searchId);
					parseLegalTXTarrantTR(m, searchId);
				}
				TXJohnsonTR.parseAddressTXJohnsonTR(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static void partyNamesTXCollinTR(ResultMap m, long searchId) throws Exception {
		
		String stringOwner = (String) m.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = scoateNumeDinStringCuAdresa(stringOwner, "<br>");
		
		stringOwner = stringOwner.toUpperCase();
		stringOwner = stringOwner.replaceAll("[\\\"]+", "");
		stringOwner = stringOwner.replaceAll("\\s*\r\\s*", " & ");
		stringOwner = stringOwner.replaceAll("\\s*%\\s*", "@@@%");
		stringOwner = stringOwner.replaceAll("\\b\\d+/\\d+\\b", "");
		stringOwner = stringOwner.replaceAll("\\s+&\\s+(C/O)\\b", "@@@$1");
		stringOwner = stringOwner.replaceAll("([A-Z]+)(C/O)\\b", "$1@@@$2");
		stringOwner = stringOwner.replaceAll("\\s+MR\\s+", " ");
		stringOwner = stringOwner.replaceAll("\\s+/\\s+", " & ");
		stringOwner = stringOwner.replaceAll("\\bINT\\b", "");
		stringOwner = stringOwner.replaceAll("\\bAS (TRUSTEE) OF THE\\b", " $1");
		stringOwner = stringOwner.replaceAll("\\b-\\s*(TRS?|T(?:(?:RU?)?S)?TEE?S?)\\b", " $1");
		stringOwner = stringOwner.replaceAll("\\s+(?:CO)\\s*(TRS?|T(?:(?:RU?)?S)?TEE?S?)\\b", " $1");
		stringOwner = stringOwner.replaceAll("\\b(TRUSTEES)\\s+(.+)", "$1@@@$2");
		stringOwner = stringOwner.replaceAll("\\bTHE UND\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s+Heirs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*&\\s*", " & ");
		stringOwner = stringOwner.replaceAll("(?is)\\bMrs\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)([^@]+)@@@\\s*(\\w+)\\s*&\\s*(\\w+)\\s+(\\w+)\\s*$", "$1 @@@ $4 $2 & $3");
		stringOwner = stringOwner.replaceAll("(?is)\\b(REV LIV(?:ING)?)\\s*TR\\b", "$1 TRUST");
		stringOwner = stringOwner.replaceAll("(?is)\\b(LIV(?:ING)? REV)\\s*TR\\b", "$1 TRUST");
		stringOwner = stringOwner.replaceAll("(?is)&\\s*(ATTN):?", "@@@$1 ");
		stringOwner = stringOwner.replaceAll("(?is)\\b(TRUST(?:\\s+THE)?)\\s*&\\s*", "$1@@@");
		stringOwner = stringOwner.replaceAll("(?is)\\b(ASSOCIATES?|TRUSTS?)([A-Z]+)\\s+", "$1@@@$2 ");
		stringOwner = stringOwner.replaceAll("(?is)\\b(TRUST)@@@(EES?)\\b", "$1$2");
		stringOwner = stringOwner.replaceAll("(?is)\\s+&\\s+\\(\\d+[A-Z]\\s+[A-Z]+\\)", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s+&\\s+MC:.*", "");
		stringOwner = stringOwner.replaceAll("(?is)\\s+(INC)\\s+&\\s+", " $1@@@%");
		stringOwner = stringOwner.replaceAll("(?is)(&\\s+HEAT(?:IN?G?)?)([A-Z]+)", "$1@@@$2");
		stringOwner = stringOwner.replaceAll("(?is)&\\s+([A-Z]+'[A-Z]+)\\b", "@@@$1");
		
		if (stringOwner.trim().matches("(?is)([^&]+)&\\s*(.*?\\bTRUST)$") && !stringOwner.contains("@@@")){
			stringOwner = stringOwner.replaceAll("(?is)([^&]+)&\\s*(.*?\\bTRUST)\\s*$", "$1@@@$2");
		}
		stringOwner = stringOwner.replaceAll("(?is)\\A\\s*@@@", "");
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		//String[] coNames = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		String[] owners = stringOwner.split("\\s*@@@\\s*");
		for (int i = 0; i < owners.length; i++) {
			owners[i] = owners[i].replaceAll("(?is)\\A\\s*&\\s*", "");
			names = StringFormats.parseNameNashville(owners[i], true);
			if (owners[i].trim().startsWith("ATTN ") || owners[i].trim().startsWith("%") || owners[i].trim().startsWith("C/O")){
				owners[i] = owners[i].replaceAll("(?is)\\A\\s*ATTN\\s*", "").replaceAll("(?is)\\A\\s*%\\s*", "").replaceAll("(?is)\\A\\s*C/O\\s*", "");
				names = StringFormats.parseNameDesotoRO(owners[i], true);
			}
			
			names[2] = names[2].replaceAll("(?is),", "");
			/*if (NameUtils.isNotCompany(names[5]) && LastNameUtils.isNotLastName(names[5])){
				if (stringOwner.trim().matches("(?is)[^&]+&\\s+\\w+\\s+\\w(?:\\s+\\w+)?\\b")){
					String coOwner = stringOwner.replaceAll("[^&]+&\\s+(\\w+\\s+\\w{3,}(?:\\s+\\w+)?)", "$1");
					coNames = StringFormats.parseNameDesotoRO(coOwner, true);
					names[3] = coNames[0];
					names[4] = coNames[1];
					names[5] = coNames[2];
				}
			}*/

			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			
			if (LastNameUtils.isLastName(names[1]) && LastNameUtils.isNotLastName(names[2]) && NameUtils.isNotCompany(names[2])){
				String aux = names[1];
				names[1] = names[0];
				names[0] = names[2];
				names[2] = aux;
			} else if (StringUtils.isEmpty(names[1]) && LastNameUtils.isNotLastName(names[2]) 
					&& NameUtils.isNotCompany(names[2]) && LastNameUtils.isLastName(names[0])){
				String aux = names[0];
				names[0] = names[2];
				names[2] = aux;
			} else if (StringUtils.isEmpty(names[4]) && LastNameUtils.isNotLastName(names[5]) 
					&& NameUtils.isNotCompany(names[5]) && LastNameUtils.isLastName(names[3])){
				String aux = names[3];
				names[3] = names[5];
				names[5] = aux;
			}
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
												NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		stringOwner = stringOwner.replaceAll("\\s*@@@\\s*", " AND ");
		
		String[] a = StringFormats.parseNameNashville(stringOwner, true);
		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
		
		m.put("PropertyIdentificationSet.NameOnServer", stringOwner);
		
	}
	
	public static String scoateNumeDinStringCuAdresa(String myString, String regexForSplit){
		if (StringUtils.isEmpty(myString))
			return "";
		
		String name = "";
		//SMITH BENJAMIN ALVIN<br>SMITH JENNIFER DIANE8116 BLACKTAIL TRL<br>MC KINNEY TX 75070-7912
		myString = myString.replaceAll("(?is)\\b([A-Z]+)(\\d+)\\s+", " $1" + regexForSplit + "$2 ");
		//JOHNSON A MBOX 43    Collin
		myString = myString.replaceAll("(?is)\\s+([A-Z]+)\\s*(BOX)\\b", " $1" + regexForSplit + "$2");
		myString = myString.replaceAll("(?is)\\b(BOX)\\s*<br>\\s*(\\d+)\\s*<", "$1 $2<");
		myString = myString.replaceAll("(?is)(<br>\\s*P\\s*O\\s*)<br>(\\s*BOX\\s*)", "$1 $2");
		myString = myString.replaceAll("(?is)(<br>(?:C/O)?\\s+TAX DEP(?:ARTMEN)?T)\\s+(\\d+)", "$1<br>$2");
		String[] rows = myString.split(regexForSplit);
		for(String row : rows){
			if (!row.trim().matches("\\*?\\d+.*")){
				if (!row.trim().matches(".*((P\\s*O|RT\\s*\\d+)\\s+BOX|BOX\\s+\\d+).*")) {
					if (!row.matches(".*(TX\\s*\\d+).*")) {
						name += " & " + row.trim();
					} else {
						break;
					}
				} else 
					break;
				
			} else if (NameUtils.isCompany(row)){
				String[] tokens = row.split("[\\s]+");
				boolean isAddress = false;
				for (String token : tokens){
					if (Normalize.isSuffix(token.replaceAll(",", ""))) {
						isAddress = true;
						break;
					}
				}
				if (!isAddress){
					if (!row.trim().matches("\\d+\\s+[A-Z]+\\s+#?\\d+")){
						name += " " + row;
					} else {
						break;
					}
				} else 
					break;
			} else 
				break;
		}
		name = name.replaceAll("(?is)\\s*&\\s*&\\s*", " & ").replaceAll("(?is)\\A\\s*&\\s*", "");
		return name.trim();
	}
	
	public static void parseLegalTXCollinTR(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
				
		if (StringUtils.isEmpty(legal))
			return;
			
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", "");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LOTS?\\s*:?)\\s*([\\d\\s,&-]+[A-Z]?\\d?)\\b");
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
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*(\\d+[A-Z]?|[A-Z])[ ,]");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(UNITS?)\\s*([A-Z]?[\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(ABS?T?)\\s+([A-Z]?[\\d+\\s&]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.AbsNo", ma.group(2).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(1), "");
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legal = legal.replaceAll(ma.group(0), "");
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(2).replaceAll("\\s*,\\s*", " ").trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		String subdiv = "";
		if (legal.contains("(")) {
			p = Pattern.compile("(?is)\\A([^\\(]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} 
		} else {
			p = Pattern.compile("(?is)\\bABST?\\s+([^,]+)");
			ma.usePattern(p);
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A([^,]+)");
				ma.usePattern(p);
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}

		subdiv = subdiv.replaceAll(",", "");
		subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?", "");
		m.put("PropertyIdentificationSet.SubdivisionName", subdiv.trim());
		legal = legal.replaceAll("", "");
	}
	
	public static void parseLegalTXTarrantTR(ResultMap m, long searchId) throws Exception {
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
				
		if (StringUtils.isEmpty(legal))
			return;
		
		legal = legal.replaceAll("(?is)(\\s[^\\s]+)<br>([^\\s]+)(BLO?C?KS?\\s)", "$1$2 $3");
		legal = legal.replaceAll("(?is)(B)(?:<br>)?(LO?C?KS?\\s)", " $1$2");
		legal = legal.replaceAll("(?is)(LO?TS?\\s)", " $1");
		
		String originalLegal = legal;
			
		legal = GenericFunctions.replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		
		legal = legal.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
		legal = legal.replaceAll("<br>", " ");
		legal = legal.replaceAll("(?is)\\s+\\d?\\.\\d+\\s+ACRES\\b", "");
		legal = legal.replaceAll("(?is)\\bLOTS\\s*(\\d+[A-Z])\\s*&\\s*(\\d+[A-Z])\\b", "LOT $1 & LOT $2");
		legal = legal.replaceAll("(?is)\\([^\\)]+\\)", " ");
		legal = legal.replaceAll("(?is)\\s+THRU\\s+", "-");
		legal = legal.replaceAll("(?is),", ", ");
		legal = legal.replaceAll("(?is)\\b(LOT\\s*[\\d-]+)([A-Z]\\d+)", "$1 $2");//95296DEN; O T RHOME, BLOCK 17, LOT1-3C41 G04
		legal = legal.replaceAll("(?is)\\b(LOT\\s*\\d{2}+)\\s*&\\s*(\\d)\\s+(\\d)([A-Z]\\d+)", "$1&$2$3 $4");//95289DEN; O T RHOME,  BLOCK 16,  LOT11 & 1 2C41 G04
		
		
		String legalTemp = legal;
		
		// extract lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s*([A-Z][\\d&-]+|[\\d\\\\s,&]+[A-Z]?\\d?)(\\b|INT\\d+|VOL\\d+)");
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
		p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*(\\d+[A-Z]?|[A-Z])\\b");
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
		
		p = Pattern.compile("(?is)\\b(UNITS?)\\s*([A-Z]?#?[\\d-]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(A(?:BST?)?)\\s*([A-Z]?[\\d+\\s&]+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.AbsNo", ma.group(2).replaceAll("\\s*&\\s*", " ").trim());
			legal = legal.replaceAll(ma.group(0), "ABS");
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s+([\\dA-Z,]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			legal = legal.replaceAll(ma.group(0), "");
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(2).replaceAll("\\s*,\\s*", " ").trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(BLDG\\s*:?)\\s*([A-Z]?\\d+[A-Z]?|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		p = Pattern.compile("(?is)\\b(PH(?:ASE)?)\\s*(\\d+[A-B]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
				
		p = Pattern.compile("(?is)\\b(SECT?)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2).trim().replaceAll("#", ""));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		
		if (!(originalLegal.toLowerCase().contains("mineral") || originalLegal.toLowerCase().contains("energy")
				|| originalLegal.toLowerCase().contains("gas well") || originalLegal.toLowerCase().contains("personal property"))){
			String[] lines = originalLegal.split("<br>");
			if (lines.length > 0){
				String subdiv = lines[0];
				subdiv = subdiv.replaceAll("(?is)([^,]+),.*", "$1");
				subdiv = subdiv.replaceAll("(?is)\\b(A(?:BST?)?)\\s*([A-Z]?[\\d+\\s&]+[A-Z]?)\\b", "");
				subdiv = subdiv.replaceAll(",", "");
				subdiv = subdiv.replaceAll("(?is)(.+)\\s+BLO?C?K.*", "$1");
				subdiv = subdiv.replaceAll("(?is)([^\\(]+)\\(.*", "$1");
				subdiv = subdiv.replaceAll("(?is)(.+)\\s+PH(?:ASE)?.*", "$1");
				subdiv = subdiv.replaceAll("(?is)(.+)\\s+MHP.*", "$1");
				subdiv = subdiv.replaceAll("(?is)((.+)\\s+)?LOT.*", "$1");
				subdiv = subdiv.trim();
				if (subdiv.length() != 0)
					m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			}
		}
		/*String subdiv = "";
		if (legal.contains("(")) {
			p = Pattern.compile("(?is)\\A([^\\(]+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} 
		} else {
			p = Pattern.compile("(?is)\\bABST?\\s+([^,]+)");
			ma.usePattern(p);
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile("(?is)\\A([^,]+)");
				ma.usePattern(p);
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
		}

		subdiv = subdiv.replaceAll(",", "");
		subdiv = subdiv.replaceAll("(?is)\\A\\s*ABST?", "");
		m.put("PropertyIdentificationSet.SubdivisionName", subdiv.trim());
		legal = legal.replaceAll("", "");*/
	}
	
}

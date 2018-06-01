package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class FLSeminoleTR {
	
	@SuppressWarnings("rawtypes")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId, int miServerID) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&nbsp", " ").replaceAll("(?is)&amp;", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)</?div[^>]*>", "").replaceAll("(?is)<!--[^-]*-->", "").replaceAll("</?font[^>]*>", "")
									.replaceAll("(?is)</?p[^>]*>", "").replaceAll("(?is)</?strong>", "").replaceAll("\n", "").replaceAll("<tr[^>]+>", "<tr>");
		m.put("OtherInformationSet.SrcType","TR");
		
		try {		
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Parcel:"), "", true).trim();
			m.put("PropertyIdentificationSet.ParcelID", StringUtils.isNotEmpty(pid) ? pid : "");
			m.put("PropertyIdentificationSet.ParcelID2", StringUtils.isNotEmpty(pid.replaceAll("-", "")) ? pid.replaceAll("-", "") : "");
			m.put("PropertyIdentificationSet.ParcelID3", StringUtils.isNotEmpty(pid.replaceAll("-", "")) ? pid.replaceAll("-", "") : "");
			
			String taxYr = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Tax Year:"), "", true).trim();
			m.put("TaxHistorySet.Year", StringUtils.isNotEmpty(taxYr) ? taxYr : "");
			
			String taxBill = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Tax Bill #:"), "", true).trim();
			m.put("TaxHistorySet.TaxBillNumber", StringUtils.isNotEmpty(taxBill) ? taxBill : "");
			
			String totalAssessedValue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Total Assessed Value"), "", true).trim();
			m.put("PropertyAppraisalSet.TotalAssessment", StringUtils.isNotEmpty(totalAssessedValue) ? totalAssessedValue.replaceAll("[\\$,]", "") : "0.00");
			
			String baseAmount = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Gross Tax Amount:"), "", true).trim();
			m.put("TaxHistorySet.BaseAmount", StringUtils.isNotEmpty(baseAmount) ? baseAmount.replaceAll("[\\$,]", "") : "0.00");

			Node ownerTableNode = HtmlParser3.getNodeByID("Owner", mainList, true);
			if (ownerTableNode != null){
				TableTag ownerTable = (TableTag) ownerTableNode;
				TableRow[] rows = ownerTable.getRows();
				if (rows.length > 0){
					TableColumn[] cols = rows[0].getColumns();
					if (cols.length > 0){
						String ownerAndHisAddr = cols[0].getChildrenHTML();
						ownerAndHisAddr = ownerAndHisAddr.replaceAll("(?is)Address/Ownership\\s+Changes\\s*\\(link\\s+to\\s+Property\\s+Appraiser\\)", "")
										.replaceAll("<br\\s*/?\\s*>", "<br>").replaceAll("</?span>", "").replaceAll("\r", "");
						String[] stuffs = ownerAndHisAddr.split("<br>");
						int ownerRowsNo = 2;
						int counter = 0;
						boolean isOwner = false;
						boolean isAddress = false;
						boolean isAlreadyFoundAddress = false;
						StringBuilder sb = new StringBuilder();
						for(String str : stuffs){
							if (str.contains("Owner")){
								isOwner = true;
								isAddress = false;
								counter = 0;
							} else if (str.contains("Property")){
								isAddress = true;
								isOwner = false;
							} else if (isOwner) {
								if (counter<ownerRowsNo) {
									str = str.trim();
									if (!"".equals(str)) {
										sb.append(str).append(" ");
									}
								}
								counter++;
							} else if (isAddress && !isAlreadyFoundAddress) {
								str = str.trim();
								if (!"".equals(str)) {
									m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(str));
									m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(str));
								}
							}
						}
						m.put("tmpOwnerName", sb.toString().trim());
					}
				}
			}
			String legal = "";
			for (int idx = 1; idx < 5; idx++) {
				String legalRow = "";
				try {
					legalRow = HtmlParser3.getValueFromAbsoluteCell(idx, 0, HtmlParser3.findNode(mainList, "Legal Description:"), "", true, false).trim();
				} catch (Exception e) {	}
					
				if (org.apache.commons.lang.StringUtils.isBlank(legalRow)) {
					break;
				}
				legal += legalRow + " ";
			}
				
			legal = legal.replaceAll("\\s{2,}", " ");
			m.put("PropertyIdentificationSet.PropertyDescription", legal.trim());
			
			Node taxDueTableNode = HtmlParser3.getNodeByID("Current", mainList, true);
			if (taxDueTableNode != null){
				TableTag dueTable = (TableTag) taxDueTableNode;
				TableRow[] rows = dueTable.getRows();
				//String amtDue = "0.00";
				for (TableRow row : rows){
					TableColumn[] cols = row.getColumns();
					if (cols[0].toHtml().contains("Entire Payoff of " + taxYr +" Installment Tax")){
						putAmountDue(m, cols);
					}else if (cols[0].toHtml().contains("Amount Due")){
						putAmountDue(m, cols);
					} else if (cols[0].toHtml().contains("Tax Payment")){
						putAmountDue(m, cols);
					}
				}
			}
			
			Node taxPaidTableNode = HtmlParser3.getNodeByID("paid", mainList, true);
			if (taxPaidTableNode != null){
				String taxTable = taxPaidTableNode.toHtml();
				taxTable = taxTable.replaceAll("[\\$,]", "");
				List<List<String>> taxPaid = HtmlParser3.getTableAsList(taxTable, false);
				List<List> newBody = new ArrayList<List>();
				List<String> line = null;
				for (List<String> lst : taxPaid){
					for (int i = 0; i < lst.size(); i=i+3) {
						line = new ArrayList<String>();
						line.add(lst.get(i).trim());
						line.add(lst.get(i+1).trim());
						line.add(lst.get(i+2).trim());
						if (!line.isEmpty()){
							newBody.add(line);
						}
					}
				}
				ResultTable rt = new ResultTable();
				String[] header = {"ReceiptDate", "ReceiptNumber", "ReceiptAmount"};
				rt = GenericFunctions2.createResultTable(newBody, header);
				m.put("TaxHistorySet", rt);	
			}
			
			Node priorDelinqTableNode = HtmlParser3.getNodeByID("DQ", mainList, true);
			if (priorDelinqTableNode != null){
				TableTag priorDelinqTable = (TableTag) priorDelinqTableNode;
				TableRow[] rows = priorDelinqTable.getRows();
				String amtDelinq = "0.00";
				for (int i = 1; i < rows.length; i++){
					TableColumn[] cols = rows[i].getColumns();
					if (cols.length > 2){
						if (cols[2].toHtml().contains("#FFFF99")){
							if(cols[0].toPlainTextString().trim().equals(taxYr)) {
								m.put("TaxHistorySet.TotalDue", cols[2].getChildrenHTML().replaceAll("[\\$,]+", "").trim());
								m.put("TaxHistorySet.CurrentYearDue", cols[2].getChildrenHTML().replaceAll("[\\$,]+", "").trim());
							} else {
								amtDelinq += "+" + cols[2].getChildrenHTML().replaceAll("[\\$,]+", "").trim();
							}
						}
					}
					if (cols.length > 4){
						if (cols[4].toHtml().contains("#FFFF99")){
							if(cols[0].toPlainTextString().trim().equals(taxYr)) {
								m.put("TaxHistorySet.TotalDue", cols[4].getChildrenHTML().replaceAll("[\\$,]+", "").trim());
								m.put("TaxHistorySet.CurrentYearDue", cols[4].getChildrenHTML().replaceAll("[\\$,]+", "").trim());
							} else {
								amtDelinq += "+" + cols[4].getChildrenHTML().replaceAll("[\\$,]+", "").trim();
							}
						}
					}
					if (cols.length > 6){
						if (cols[6].toHtml().contains("#FFFF99")){
							if(cols[0].toPlainTextString().trim().equals(taxYr)) {
								m.put("TaxHistorySet.TotalDue", cols[6].getChildrenHTML().replaceAll("[\\$,]+", "").trim());
								m.put("TaxHistorySet.CurrentYearDue", cols[6].getChildrenHTML().replaceAll("[\\$,]+", "").trim());
							} else {
								amtDelinq += "+" + cols[6].getChildrenHTML().replaceAll("[\\$,]+", "").trim();
							}
						}
					}
				}
				m.put("tmpPriorDelinq", amtDelinq);
			}		
						
			try {
				partyNamesFLSeminoleTR(m, searchId);
				taxFLSeminoleTR(m, searchId);
				legalFLSeminoleTR(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static void putAmountDue(ResultMap m, TableColumn[] cols) {
		String amtDue;
		amtDue = cols[0].getChildrenHTML();
		amtDue = amtDue.replaceAll("(?is).*?(Due|Installment Tax)\\s*:\\s*([^<]+).*", "$2").replaceAll("[\\$,]", "").replaceAll("<[^>]*>","").replaceAll("\\s+","");
		m.put("TaxHistorySet.TotalDue", amtDue.trim());
		m.put("TaxHistorySet.CurrentYearDue", amtDue.trim());
	}	
	
	@SuppressWarnings({ "rawtypes" })
	public static void partyNamesFLSeminoleTR(ResultMap m,long searchId) throws Exception {
		   
	    String ownName = (String) m.get("tmpOwnerName");
	    
	    if (StringUtils.isEmpty(ownName))
	 	   return;
	    
	    ownName = ownName.replaceAll("\\*C/O", "&");
	    String s = ownName;
	    
	    s = s.replaceAll("(?is)(#)\\s*\\d+", "$1");
	    s = s.replaceAll("(?is)\\b(UNIT)\\s+\\d+", "$1");
	    s = s.replaceAll("(?is)(.+)\\s+(PO\\s*BOX)(.+)", "$1");
	    s = s.replaceAll("(?is)(.+)\\s+(\\d+)\\s+(?:\\d)?([A-Z]+)(.+)", "$1");
	    s = s.replaceAll("FBO.*", "");
	    s = s.replaceAll("ELDER.*", "");
	    s = s.replaceAll("\\bCO-(TRUSTEES.*)", " $1");
	    s = s.replaceAll("\\bCO-(TRS.*)", " $1");
	    s = s.replaceAll("AND/OR", "&");
	    s = s.replaceAll("\\(", "& ");
	    s = s.replaceAll("\\)", "");
	    s = s.replaceAll("PER REP FOR EST OF", "&");
	    s = s.replaceAll("\\s*&\\s+(HENRY &\\s+DENNIS JR &)",  " $1");
	    s = cleanOwnerFLSeminoleTR(s);
	   
	    s = s.replaceAll("C/O", "&");
	    		    
	    String[] owners ;
	   	//String[] own = null;
	   	owners = s.split("&");
	   	if (s.matches("(?is)[^&]+&\\s\\w+\\s+\\w+(\\sTRU?S?T?E?ES)?")){
	   		owners = s.split("@@##@@");
	   	}

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };

		String ln = "";

		boolean isDesotoParseName = false;
		if ((ownName.contains("C/O"))
				|| (ownName.contains("PER REP FOR EST OF"))
				|| (ownName.contains("PER REP ESTATE OF"))) {
			isDesotoParseName = true;
		}

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];

			if ((i > 0) && isDesotoParseName) {
				names = StringFormats.parseNameDesotoRO(ow, true);
			} else {
				names = StringFormats.parseNameNashville(ow, true);
			}

			if (ow.contains("MARTIN ALTAMESE")) { // pid 33-19-31-507-0000-0700
				names = StringFormats.parseNameDesotoRO(ow, true);
			}

			if (!NameUtils.isCompany(names[2])) {
				if ((names[0].length() == 1 && names[1].length() == 0 && !LastNameUtils.isLastName(names[2]))
						|| (names[0].length() == 0 && names[1].length() == 0 && names[2].length() == 0)
						|| ((names[0].length() < 1) && !LastNameUtils.isLastName(names[2]))) {
					names[1] = names[0];
					names[0] = names[2];
					names[2] = ln;
				}
				if ((names[0].length() <= 1 && names[1].length() == 0 && names[2].length() != 0)) {
					names[1] = names[0];
					names[0] = names[2];
					names[2] = ln;
				}
				Matcher ma = GenericFunctions1.nameSuffix.matcher(names[0]);
				if (ma.find() && names[1].length() == 0 && names[2].length() != 0) {
					names[1] = names[0];
					names[0] = names[2];
					names[2] = ln;
				}

				if ((names[2].length() == 1 && names[1].length() == 0)
						|| (names[2].length() == 0 && names[1].length() == 0)) {
					names[1] = names[2];
					// names[0] = names[2];
					names[2] = ln;
				}

			}
			ln = names[2];
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions1.extractNameSuffixes(names);
			GenericFunctions1.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
					NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions1.storeOwnerInPartyNames(m, body, true);
	}
	
	@SuppressWarnings("rawtypes")
	public static void stdFLSeminoleTR(ResultMap m, long searchId) throws Exception {
		   
		   String owners = (String) m.get("tmpOwnerName");
		   if (StringUtils.isEmpty(owners))
			   return;
		   
		   String[] lines = owners.split(" {2,}");
		   // the owner and co-owner name are stored in the first 1 or 2 lines and the last 2 lines contains the mailing address
		   	   	   	   	   
		   String[] a = {"", "", "", "", "", ""};
		   String[] b = {"", "", "", "", "", ""};		   
		   
		   if (lines.length >1){
			   lines[1] = lines[1].replaceAll("\\s*\\bFBO\\s*\\b", "").trim();
		   }
		   if (lines.length == 1 || (lines.length > 1 && lines[1].length() == 0)){
			   lines[0] = cleanOwnerFLSeminoleTR(lines[0]);
		   } else {
				if (lines[0].endsWith("&") || lines[0].endsWith("+")){
					if (lines[1].matches("[A-Z-]{2,}( [A-Z])?")){  // PID 05-21-30-502-0G00-0160
						lines[0] = cleanOwnerFLSeminoleTR(lines[0] + " " + lines[1]);
						lines[1] = "";
					} else {
						lines[0] = cleanOwnerFLSeminoleTR(lines[0]);
						lines[1] = cleanOwnerFLSeminoleTR(lines[1]);
					}
				} else {
					if (!lines[1].startsWith("C/O")){
						lines[0] = lines[0] + " " + lines[1];
						lines[1] = "";
						Pattern p = Pattern.compile("(.+) \\(([^\\)]+)\\)$"); // PID 35-21-31-509-0000-0120, 14-20-29-5NE-0000-0810
						Matcher ma = p.matcher(lines[0]);
						if (ma.find()){
							lines[0] = ma.group(1);
							lines[1] = cleanOwnerFLSeminoleTR(ma.group(2));
						}
					}
					lines[0] = cleanOwnerFLSeminoleTR(lines[0]);				
				}
		   }

			// keep only the first 2 owners, if more than 2 owners
		   lines[0] = lines[0].replaceFirst("^([^&]+&[^&]+)&.*", "$1").trim();
		   a = StringFormats.parseNameNashville(lines[0], true);
		   
		   if (a[5].length() == 0 && lines.length > 1 && lines[1].length() != 0){
			   if (!lines[1].startsWith("C/O"))
				   b = StringFormats.parseNameNashville(lines[1], true);
			   else 
				   b = StringFormats.parseNameDesotoRO(lines[1].replaceFirst("^C/O\\b\\s*", ""), true);
			   a[3] = b[0];
			   a[4] = b[1];
			   a[5] = b[2];	
		   }
			   		   		   
		   m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
	       m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
	       m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
	       m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
	       m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
	       m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	       
	       List<List> body = new ArrayList<List>();
		   String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		   types = GenericFunctions.extractAllNamesType(a);
		   otherTypes = GenericFunctions.extractAllNamesOtherType(a);
		   suffixes = GenericFunctions.extractAllNamesSufixes(a);
		   
		   GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], types, otherTypes, 
					NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
		
		   GenericFunctions.storeOwnerInPartyNames(m, body, true);
		}
	
	protected static String cleanOwnerFLSeminoleTR(String s){
		   s = s.toUpperCase();
		   s = s.replaceFirst("\\s*[&\\+]\\s*$", "");	
		   s = s.replaceAll("\\b((SUCC|MARITAL) )?(CO-?\\s*)?\\b", "");
		   s = s.replaceAll("\\+", "&");
		   s = s.replaceAll("\\([^\\)]*$", "");		   
		   s = s.replaceAll("\\bHEIRS\\b", "");
		   s = s.replaceAll("\\bPER REPS?\\b", "");
		   s = s.replaceAll("\\bESTATE OF\\b", "&");		   		
		   s = s.replaceAll("\\s{2,}", " ").trim();
	       return s;
	}
	
	public static void legalFLSeminoleTR(ResultMap m, long searchId) throws Exception {
		   
		   String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");
		   if (StringUtils.isEmpty(legal))
			   return;
		   
		   //initial cleanup of legal description
		   legal = legal.replaceAll("^LEG\\b", "");
		   legal = legal.replaceAll("\\b[SWEN]{1,2}\\s*(\\d+ )?\\d*[\\./]?\\d+(\\s*FT)?(\\s*OF)?\\b", "");
		   legal = legal.replaceAll("\\b\\d*[\\./]\\d+\\b", "");
		   legal = legal.replaceAll("\\bP(AR)?T OF\\b", "");
		   legal = legal.replaceAll("\\b(SUB)?DIVISION OF\\b", "");	   	
		   legal = legal.replaceAll("\\b(OF )?(STS?|ALLEY) ADJ ON [SWEN]( & [SWEN])*\\b", "");
		   legal = legal.replaceAll("(& )?\\bALLEYS BET LOTS\\b", "");
		   legal = legal.replaceAll("\\bVACD\\b", "");
		   legal = GenericFunctions2.replaceNumbers(legal);	   
		   legal = legal.replaceAll("(\\d) TO (\\d)", "$1-$2");
		   legal = legal.replaceAll("\\sTHRU\\s", "-");	    
		   legal = legal.replaceAll("(\\d)\\s+-", "$1-");
		   legal = legal.replaceAll("-\\s+(\\d)", "-$1");
		   legal = legal.replaceAll("\\+", "&");
		   legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) SEC", "SEC $1");
		   legal = legal.replaceAll("\\s{2,}", " ").trim();
		   	   	   
		   // extract and remove lot from legal description	
		   String patt="\\d+[A-Z]?";
		   String lot = ""; // can have multiple occurrences	   
		   Pattern p = Pattern.compile("\\bLOTS? ("+patt+"(?:\\s*[&\\s]\\s*"+patt+")*)\\b");
		   Matcher ma = p.matcher(legal);
		   while (ma.find()){
			   lot = lot + " " + ma.group(1).replace('&', ' ');
			   legal = legal.replaceFirst(ma.group(0), "LOT ");
		   }
		   lot = lot.replaceAll("\\s{2,}", " ").trim();
		   if (lot.length() != 0){
			   lot = LegalDescription.cleanValues(lot, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   	   	   	
		   // extract and remove section from legal description
		   String sec = "";
		   p = Pattern.compile("\\bSEC(?:TION)? (\\d+(?:-?[A-Z]?))\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   sec = sec + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");
			   legal = legal.replaceFirst(ma.group(0), "SEC ");
		   }	   
		   if (sec.length() != 0){
			   sec = sec.replaceAll("\\s{2,}", " ").trim();
			   sec = LegalDescription.cleanValues(sec, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionSection", sec);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and remove township from legal description
		   p = Pattern.compile("\\bTWP (\\d+[SWEN]?)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionTownship", ma.group(1));
			   legal = legal.replaceFirst(ma.group(0), "TWP ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and remove range from legal description
		   p = Pattern.compile("\\bRGE (\\d+[SWEN]?)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(1));
			   legal = legal.replaceFirst(ma.group(0), "RGE ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and remove block from legal description
		   p = Pattern.compile("\\bBLK ([A-Z]|\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionBlock", ma.group(1));
			   legal = legal.replaceFirst(ma.group(0), "BLK ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and remove unit from legal description
		   String unit = ""; // can have multiple occurrences
		   p = Pattern.compile("\\bUNIT (\\d+[A-Z]?|[A-Z]\\d+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   unit = unit + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");
			   legal = legal.replaceFirst(ma.group(0), "UNIT ");
		   }
		   p = Pattern.compile("\\bU (\\d+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   unit = unit + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");
			   legal = legal.replaceFirst(ma.group(0), "UNIT ");
		   }	   
		   unit = unit.trim();
		   if (unit.length() != 0){
			   unit = LegalDescription.cleanValues(unit, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }

		   // extract and remove building # from legal description
		   p = Pattern.compile("\\bBLDG (\\d+(?:-?\\w+)?|[A-Z](?:-?\\d+)?)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			   legal = legal.replaceFirst(ma.group(0), "BLDG ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and remove phase from legal description
		   p = Pattern.compile("\\bPH(?:ASE)? (\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1));
			   legal = legal.replaceFirst(ma.group(0), "PHASE ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and remove tract from legal description
		   p = Pattern.compile("\\bTR(ACT)?\\s+(\\d+)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(1));
			   legal = legal.replaceFirst(ma.group(0), "TRACT ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   	   
		   // extract and remove plat book & page from legal description
		   p = Pattern.compile("\\bPB (\\d+) PGS? (\\d+(?:\\s*[-&]\\s*\\d+)*)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.PlatBook", ma.group(1).replaceFirst("^0+(.+)", "$1"));
			   m.put("PropertyIdentificationSet.PlatNo", ma.group(2).replace('&', ' ').replaceAll("\\s{2,}", " ").replaceAll("\\b0+(\\d+)", "$1"));
			   legal = legal.replace(ma.group(0), "PB ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   } 	   
		   	   	 	   
		   // extract cross refs from legal description
		   p = Pattern.compile("\\bORB (\\d+) PG (\\d+)\\b");
		   ma = p.matcher(legal);	      	   
		   if (ma.find()){
			   m.put("CrossRefSet.Book", ma.group(1).replaceFirst("^0+(.+)", "$1"));
			   m.put("CrossRefSet.Page", ma.group(2).replaceFirst("^0+(.+)", "$1"));
			   legal = legal.replace(ma.group(0), "ORB ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   } 

		   // extract subdivision name
		   String subdiv = "";
		   patt = "\\b(?:PB|UNIT|SEC(?:TION)?|SUBD?|PH(?:ASE)?|$|ORB|(A )?CONDO(?:MINIUM)?|BLK|TRACT|NO|VILLAGE \\d+|\\d+)\\b.*";
		   legal = legal.replaceAll("\\([^\\)]+\\)?", "").trim().replaceAll("\\s{2,}", " ");
		   if (legal.matches(".*\\bPLAT\\b.*")){
			   p = Pattern.compile(".*\\b(?:UNRECD|AMENDED) PLAT(?: OF)? (.*?)"+patt);		   
			   ma = p.matcher(legal);
			   if (ma.find()){
				   subdiv = ma.group(1);
			   }	   
		   }
		   if (subdiv.length() == 0 && (legal.matches(".*\\bLOTS?\\b.*") || legal.matches(".*\\bUNIT\\b.*") || legal.matches(".*\\bBLK\\b.*"))){
			   p = Pattern.compile("(?:(?:ALL )?(?:LOTS?|UNIT|BLK|SEC(?:TION)?|TWP|RGE|TRACT|BLDG)\\s*[&\\s]\\s*)+\\b(.*?)"+patt);		   
			   ma = p.matcher(legal);
			   if (ma.find()){
				   subdiv = ma.group(1);
			   }
		   }
		   subdiv = subdiv.trim();
		   if (subdiv.matches(".*\\bBEG\\b.*")){
			   subdiv = "";
		   }
		   if (subdiv.length() != 0){		   
			   subdiv = subdiv.replaceAll("\\b(\\d+(ST|ND|RD|TH) )?ADD(ITION)?\\b.*", "");		   
			   subdiv = subdiv.replaceFirst("^AT\\b", "");
			   subdiv = subdiv.replaceFirst("\\bAT$", "");
			   subdiv = subdiv.replaceFirst("^OF\\b", "");
			   subdiv = subdiv.replaceFirst("\\bTHE$", "");
			   subdiv = subdiv.replaceFirst("\\b\\d+$", "");
			   subdiv = subdiv.replaceFirst("\\b(UNRECD|AMENDED) PLAT\\b", "");
			   subdiv = subdiv.replaceFirst("\\bREPLAT (OF)?\\b", "");
			   subdiv = subdiv.replaceAll("\\s{2,}", " ").trim();
			   if (subdiv.length() != 0){
				   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				   if (legal.matches(".*\\bCONDO(MINIUM)?\\b.*"))
					   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }
		   }
		}	
	
	public static void taxFLSeminoleTR(ResultMap m, long searchId) throws Exception {
		   
		ResultTable receipts = (ResultTable) m.get("TaxHistorySet");
		String a = (String)m.get("tmp ReceiptValues");
		if (a != null && !StringUtils.isEmpty(a.replaceAll("@@", ""))){
			List<List<String>> body = new ArrayList<List<String>>();
			List<String> line = new ArrayList<String>();
			String[] tmpSplit = a.split("@@");
			if (tmpSplit.length != 0 && tmpSplit.length%3 == 0){
//				int j = -1;
				for(int i = 0; i<tmpSplit.length; i++){
					if (i%3 == 0){
//						j++;
						line = new ArrayList<String>();
					}
					line.add(tmpSplit[i]);
					if (i%3 == 2){
						body.add(line);
					}
				}
				receipts.setBody(body);
				receipts.setReadOnly();
				m.put("TaxHistorySet", receipts);
			}
		}
		
		
		//String receiptTR = (String)m.get("tmpReceiptTR");
		//if(!StringUtils.isEmpty(receiptTR)){
		//	Vector<TaxHistorySet> newTaxHist = new Vector<TaxHistorySet>();
		//	int a = 1;
		//}
		BigDecimal amtPaid = new BigDecimal("0.00");
		if (receipts != null){
			String body[][] = receipts.getBody();			
			for (int i=0; i<body.length; i++){
				if (body[i][2] != null && body[i][2].length() != 0)
					amtPaid = amtPaid.add(new BigDecimal(body[i][2]));
			}
		}
		m.put("TaxHistorySet.AmountPaid", amtPaid.toString());
	   		   
		String priorYearsDelinq = GenericFunctions2.sum((String) m.get("tmpPriorDelinq"), searchId); 		
		m.put("TaxHistorySet.PriorDelinquent", priorYearsDelinq);		   	    
	}
	
}

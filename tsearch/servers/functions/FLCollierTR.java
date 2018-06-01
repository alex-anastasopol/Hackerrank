package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
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
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLCollierTR {
	static Pattern suffixComa = Pattern.compile("(" + GenericFunctions.nameSuffixString + ")");
	
	@SuppressWarnings("rawtypes")
	public static void parseAndFillResultMap(String detailsHtml, ResultMap m, long searchId) {
		
		m.put("OtherInformationSet.SrcType","TR");
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", "").replaceAll("(?is)</?p[^>]*>", "").replaceAll("(?is)<!--.*?-->", "");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null); 
			
			String taxYear = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(mainList, "Collier County Tax Collector"), "", true).trim();
			if (StringUtils.isNotEmpty(taxYear)){
				taxYear = taxYear.replaceAll("(?is).*?(\\d+)\\s+Tax\\s+Roll\\s+Inquiry\\s+System.*", "$1");
				if (StringUtils.isNotEmpty(taxYear)){
					m.put("TaxHistorySet.Year", taxYear);
				}
			}
			
			String name = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Name:"),"", true).trim();
			if (StringUtils.isNotEmpty(name)){
				m.put("tmpOwnerName", name);
			}
			String address1 = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "Name:"),"", true).trim();
			if (StringUtils.isNotEmpty(address1)){
				m.put("tmpAddress1", address1);
			}
			String address2 = HtmlParser3.getValueFromAbsoluteCell(2, 1, HtmlParser3.findNode(mainList, "Name:"),"", true).trim();
			if (StringUtils.isNotEmpty(address2)){
				m.put("tmpAddress2", address2);
			}
			String address3 = HtmlParser3.getValueFromAbsoluteCell(3, 1, HtmlParser3.findNode(mainList, "Name:"),"", true).trim();
			if (StringUtils.isNotEmpty(address3)){
				m.put("tmpAddress3", address3);
			}
			String address4 = HtmlParser3.getValueFromAbsoluteCell(4, 1, HtmlParser3.findNode(mainList, "Name:"),"", true).trim();
			if (StringUtils.isNotEmpty(address4)){
				m.put("tmpAddress4", address4);
			}
			String address5 = HtmlParser3.getValueFromAbsoluteCell(5, 1, HtmlParser3.findNode(mainList, "Name:"),"", true).trim();
			if (StringUtils.isNotEmpty(address5)){
				m.put("tmpAddress5", address5);
			}
			
			String parcelID = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Parcel:"),"", true).trim();
			if (StringUtils.isNotEmpty(parcelID)){
				m.put("PropertyIdentificationSet.ParcelID", parcelID);
			}
			
			String propAddress = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Loc:"),"", true).trim();
			if (StringUtils.isNotEmpty(propAddress)){
				m.put("tmpLoc", propAddress);
			}
			
			String legal1 = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "Loc:"),"", true).trim();
			if (StringUtils.isNotEmpty(legal1)){
				m.put("tmpLegal1", legal1);
			}
			String legal2 = HtmlParser3.getValueFromAbsoluteCell(2, 1, HtmlParser3.findNode(mainList, "Loc:"),"", true).trim();
			if (StringUtils.isNotEmpty(legal2)){
				m.put("tmpLegal2", legal2);
			}
			String legal3= HtmlParser3.getValueFromAbsoluteCell(3, 1, HtmlParser3.findNode(mainList, "Loc:"),"", true).trim();
			if (StringUtils.isNotEmpty(legal3)){
				m.put("tmpLegal3", legal3);
			}
			String legal4 = HtmlParser3.getValueFromAbsoluteCell(4, 1, HtmlParser3.findNode(mainList, "Loc:"),"", true).trim();
			if (StringUtils.isNotEmpty(legal4)){
				m.put("tmpLegal4", legal4);
			}
			
			String totalAssess = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Market Value:"),"", true).trim();
			if (StringUtils.isNotEmpty(totalAssess)){
				m.put("PropertyAppraisalSet.TotalAssessment", totalAssess);
			}
			
			String baseAmount = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Gross Tax:"),"", true).trim();
			if (StringUtils.isNotEmpty(baseAmount)){
				baseAmount = baseAmount.replaceAll("[\\$,]+", "");
				m.put("TaxHistorySet.BaseAmount", baseAmount);
			}
			String totalDue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Now Due:"),"", true).trim();
			if (StringUtils.isNotEmpty(totalDue)){
				totalDue = totalDue.replaceAll("[\\$,]+", "");
				m.put("TaxHistorySet.TotalDue", totalDue);
			}
			
			NodeList tableList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			for (int i = 3; i < tableList.size(); i++){
				if (tableList.elementAt(i).toHtml().contains("PAYMENT INFO")){
					org.htmlparser.Parser htmlLittleParser = org.htmlparser.Parser.createParser(tableList.elementAt(i).toHtml(), null);
					NodeList mList = htmlLittleParser.parse(null); 
					String amountPaid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mList, "Paymt:"),"", true).trim();
					if (StringUtils.isNotEmpty(amountPaid)){
						amountPaid = amountPaid.replaceAll("[\\$,]+", "");
						m.put("TaxHistorySet.AmountPaid", amountPaid);
						m.put(TaxHistorySetKey.RECEIPT_AMOUNT.getKeyName(), amountPaid);
					}
					String recpt = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mList, "Recpt:"),"", true).trim();
					if (StringUtils.isNotEmpty(recpt)){
						m.put("TaxHistorySet.ReceiptNumber", recpt);
					}
					String paidDate = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mList, "Paid Dt"),"", true).trim();
					if (StringUtils.isNotEmpty(paidDate)){
						if (paidDate.trim().matches("\\d{1,2}/\\d{1,2}/\\d{4}")){
							m.put("TaxHistorySet.ReceiptDate", paidDate);
						}
					}
					break;
				}
			}
			
			for (int i = 3; i < tableList.size(); i++){
				if (tableList.elementAt(i).toHtml().contains("CERTIFICATE INFORMATION")){
					m.put("tmpTableCertificateInformation", tableList.elementAt(i).toHtml());
					break;
				}
			}
			
			NodeList taxHistoryTableList = HtmlParser3.getNodesByID("paymentInfo", mainList, true);
			if (taxHistoryTableList != null){
				List<List> body = new ArrayList<List>();
				List<String> line = null;
				for (int i = 0; i < taxHistoryTableList.size(); i++){
					TableTag table = (TableTag) taxHistoryTableList.elementAt(i);
					if (table.getRowCount() > 5){
						TableRow row = table.getRow(1);
						if (row.getColumnCount() > 1){
							if (!(row.getColumns()[1].getChild(0).toHtml().matches("\\s*00/00/0000\\s*"))){
								line = new ArrayList<String>();
								line.add(row.getColumns()[1].getChild(0).toHtml().trim());
								line.add(table.getRow(2).getColumns()[1].getChild(0).toHtml().trim());
								line.add(table.getRow(4).getColumns()[1].getChild(0).toHtml().trim().replaceAll("[\\$,]+", ""));
								body.add(line);
							}
						}
					}
				}
				if (!body.isEmpty()){
					ResultTable rt = new ResultTable();
					String[] header = { "ReceiptDate", "ReceiptNumber", "ReceiptAmount" };
					rt = GenericFunctions2.createResultTable(body, header);
					m.put("TaxHistorySet", rt);
				}
			}
			
			try {
				partyNamesFLCollierTR(m, searchId);
			} catch(Exception e) {
				e.printStackTrace();
			}
			try {
				legalFLCollierTR(m, searchId);
			} catch(Exception e) {
				e.printStackTrace();
			}
			try {
				taxFLCollierTR(m, searchId);

			} catch(Exception e) {
				e.printStackTrace();
			}
			
			m.removeTempDef();
			
		} catch(Exception e) {
			e.printStackTrace();;
		}
	}	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void taxFLCollierTR(ResultMap m, long searchID) throws Exception
	{
		String year  = (String) m.get("TaxHistorySet.Year");
		
		String tmpTableCertificateInformation = (String)m.get("tmpTableCertificateInformation");
		
		
		if(StringUtils.isNotEmpty(tmpTableCertificateInformation)) {
			Parser parser = Parser.createParser(tmpTableCertificateInformation, null);
			NodeList rows = parser.extractAllNodesThatMatch(new TagNameFilter("tr"));
			HashMap<String, String> yearsAndValues = new HashMap<String, String>();
			String totalDueParsed = (String) m.get("TaxHistorySet.TotalDue");
			BigDecimal priorDelinquent = new BigDecimal(0);
			Map<String,HashMap<String,String>> taxList = new HashMap<String,HashMap<String,String>>();
			
			//skip first two headers
			for (int i = 2; i < rows.size(); i++) {
				NodeList tdList = rows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
				if(tdList.size() > 0) {
					String firstColumn = tdList.elementAt(0).toPlainTextString().trim();
					if(firstColumn.matches("[0-9]+")) {
						for (int j = i; j < rows.size(); j++, i++) {
							NodeList tdListInternal = rows.elementAt(j).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
							if(tdListInternal.size() > 1) {
								String firstColumnInternal = tdListInternal.elementAt(0).toPlainTextString().trim();
								if(firstColumnInternal.matches("[0-9]+")) {
//									Rec # Amount Paid Paid Date
									HashMap<String,String> put = new HashMap<String,String>();
									if (tdListInternal.size() == 10){
										put.put("ReceiptDate", StringUtils.cleanHtml(tdListInternal.elementAt(4).toHtml().trim()));
										put.put("ReceiptNumber", StringUtils.cleanHtml(tdListInternal.elementAt(5).toHtml().trim()));
										put.put("ReceiptAmount", StringUtils.cleanHtml(tdListInternal.elementAt(9).toHtml().trim().replaceAll("[\\$,]+", "")));
									}
									taxList.put(firstColumnInternal, put);
								} else if (firstColumnInternal.contains("Amount due thru")) {
									yearsAndValues.put(firstColumn, tdListInternal.elementAt(1).toPlainTextString().trim());
									i++;
									break;
								}
							}
							
						}
					}
				}
			}
			String receiptDate = (String) m.get("TaxHistorySet.ReceiptDate");
			if (StringUtils.isNotEmpty(receiptDate)){
				HashMap<String,String> put = new HashMap<String,String>();
				put.put("ReceiptDate", receiptDate.trim());
				String receiptNumber = (String) m.get("TaxHistorySet.ReceiptNumber");
				if (StringUtils.isNotEmpty(receiptNumber)){
					put.put("ReceiptNumber", receiptNumber.trim());
					String receiptAmount = (String) m.get("TaxHistorySet.AmountPaid");
					if (StringUtils.isNotEmpty(receiptAmount)){
						put.put("ReceiptAmount", receiptAmount.trim());
						taxList.put(year, put);
					}
				}
			}
			ResultTable currentResultTable = (ResultTable) m.get("TaxHistorySet");
			String[] header= new String[] {"ReceiptDate", "ReceiptNumber", "ReceiptAmount"};
			ResultBodyUtils.buildInfSet(m, new ArrayList(taxList.values()), header, TaxHistorySet.class);
			ResultTable newResultTable = (ResultTable) m.get("TaxHistorySet");
			
			if (currentResultTable!= null && newResultTable!=null){
				newResultTable = ResultTable.joinVertical(currentResultTable, newResultTable, true);
				m.put("TaxHistorySet", newResultTable);
			}
			
			
			for (Entry<String, String> yearAndValue : yearsAndValues.entrySet()) {
				if(year.equals(yearAndValue.getKey())) {
					if(StringUtils.isEmpty(totalDueParsed)) {
						m.put("TaxHistorySet.TotalDue", yearAndValue.getValue());
					}
				} else {
					try {
						priorDelinquent = priorDelinquent.add(new BigDecimal(yearAndValue.getValue()));	
					} catch (Throwable t) {
						t.printStackTrace();
					}
					
				}
			}
			
			m.put("TaxHistorySet.PriorDelinquent", priorDelinquent.toString());
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void legalFLCollierTR(ResultMap m, long searchId) throws Exception {		   
		   //String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");
		String legal1 = (String)m.get("tmpLegal1");
		String legal2 = (String)m.get("tmpLegal2");
		String legal3 = (String)m.get("tmpLegal3");
		String legal4 = (String)m.get("tmpLegal4");
		String extraLegal = (String)m.get("tmpExtraLegal");
		String location = (String)m.get("tmpLoc");
		location = location.trim();
		
		Pattern locPat1 = Pattern.compile("(?is)(.+)( BLVD| CT | PL | TER | CIR | WAY | DR | LN | AV | AVE | ST | RD | TRL | PT)(.+)");
		Pattern locPat2 = Pattern.compile("(?is)(\\d\\d\\d+\\d\\d)\\s([A-Z].+)"); //exmpl: 35126  NAPLES  => Section:3, Township:51, Range:26 (asa e pe AO la unele)
		Matcher locMat1 = locPat1.matcher(location);
		Matcher locMat2 = locPat2.matcher(location);
		String city="";
		if (locMat1.find())
		{
			city = (String) locMat1.group(3);
			if (city.contains(" CITY")) {
				if (city.contains(" CITY OF")) {
					city = city.replaceFirst("(?is).*\\s*City of\\s+([^$]+)", "$1").trim();
				} else {
					city = city.substring(0,city.indexOf(" "));
				}
			}
				
			String address = locMat1.group(1) + locMat1.group(2);
			address = address.replaceAll("(?is)\\([^\\)]+\\)", "").replaceAll("\\s{2,}"," ").trim();
			city = city.replaceAll("(?is)\\([^\\)]+\\)", "").trim();
			m.put("PropertyIdentificationSet.City", city);
			m.put("PropertyIdentificationSet.StreetName",StringFormats.StreetName(address));
			m.put("PropertyIdentificationSet.StreetNo",StringFormats.StreetNo(address));
		}
		
		if (locMat2.find()) {
			if (StringUtils.isEmpty(city)) {
				city= (String)locMat2.group(2);
				if (city.contains(" CITY"))
					city = city.substring(0,city.indexOf(" "));					
				if (city.contains("TRS= ")) 
					city = city.substring(4,city.length());
				
				m.put("PropertyIdentificationSet.City",city);
			}
		}
		
		String legal = "";
		legal += (legal1 != null ? legal1 : "") +" "+ (legal2 != null ? legal2 : "") +" "+ (legal3 != null ? legal3 : "") +" "+ (legal4 != null ? legal4 : "");
		if (extraLegal!=null)
			legal += " " + extraLegal;
		if (StringUtils.isEmpty(legal))
			return;
		m.put("PropertyIdentificationSet.PropertyDescription", legal);
		
		//initial corrections and cleanup of legal description	   
		//legal = legal.replaceAll("\\b([A-Z]+[-])(\\s*)([A-Z]+)\\b", "$1$3");
		legal = legal.replaceAll("(?is)(\\bUN(?:IT)?\\b\\s+[A-Z])\\s*-\\s*(\\d+)", "$1$2");
		legal = GenericFunctions.replaceNumbers(legal);
		  
		   String[] exceptionTokens = {"M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		   
		   legal += " ";
		   legal = legal.replaceAll("\\s+AND\\s+", "&");
		   legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		   
		   legal = legal.replaceAll("\\bCO-OP\\b", "");
		   legal = legal.replaceAll("\\s+\\d+\\s*(ST|ND|RD|TH)\\s*(ADD(N)?|WAY)", "");
		   //legal = legal.replaceAll("\\b[NWSE][\\d\\./\\s]+(\\s*OF)?\\b", "");
		   legal = legal.replaceAll("UNREC\\s*PLAT\\s*(OF)?", "");
		   legal = legal.replaceAll("\\b[\\d]+\\s*OF\\b", "");
		   //legal = legal.replaceAll("\\b(\\d+(ST|ND|RD|TH) )?REPLAT( OF)?\\b", "");
		   //legal = legal.replaceAll("\\b(REVISED|AMENDED) PLAT( OF)?\\b", "");
		   //legal = legal.replaceAll("( N | S | W | E | NW | NE | SW | SE )[\\d\\./\\s]+(\\s*OF)?", "");
		   legal = legal.replaceAll("(?is)\\b[NSEW]{1,2}\\b\\s+[\\d\\.\\s]+FT\\b\\s*(?:\\bOF\\b)?", "");
		   legal = legal.replaceAll("\\(HO\\)","");
		   legal = legal.replaceAll("\\(CONDO\\)","");
		   legal = legal.replaceFirst("(?:\\bA\\b)?(?:\\s+\\bPH(?:ASE)?\\b)?\\s+CONDO(MINIUM)?","");
		   legal = legal.replaceAll("(\\d+)\\s*\\+\\s*(\\d+)","$1,$2" );
		   legal = legal.replaceAll("(\\d+)\\s*TO\\s*(\\d+)","$1-$2");
		   legal = legal.replaceAll(";", " ");
		   legal = legal.replace(",", " ");
		   legal = legal.replaceAll("\\s{2,}", " ");

		   String legalTemp = legal;
		   
		   // extract lot from legal description
		   String lot = "";
		   Pattern p = Pattern.compile("\\b(LOT(?:S)?)\\s*((?:[A-Z])?[\\d&\\s,-]+)[\\s|$]");
		   Matcher ma = p.matcher(legal);
		   while (ma.find()){
			   lot = lot + " " + ma.group(2);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1)+" ");
		   }
		   lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		   if (lot.length() != 0){
			   lot = LegalDescription.cleanValues(lot, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		   }
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		   	   	   	   
		   // extract block from legal description
		   String block = "";
		   p = Pattern.compile("\\b(BLOCK|BLK)S?\\s*([\\d,\\s-]+[A-Z]?)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   block = block + " " + ma.group(2);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1)+" ");
		   }
		   block = block.replaceAll("\\s*&\\s*", " ").trim();
		   if (block.length() != 0){
			   block = LegalDescription.cleanValues(block, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		   }	   
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		   	   
		   // extract unit from legal description
		   String unit = "";
		   p = Pattern.compile("\\b(UNIT)S?\\s*([\\d-&/A-Z]+)");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   unit = unit +" " + ma.group(2);
			   unit = unit.trim();
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);//ma.group(2));
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   
		   // extract building #
		   String bldg="";
		   p = Pattern.compile("\\b(BLDG)\\s*([A-Z]|\\d+(?:-(?:\\d+|[A-Z]))?)\\b"); 
		   ma = p.matcher(legal);
		   if (ma.find()){
			   bldg = bldg + " " + ma.group(2);
			   bldg = bldg.trim();
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   	   
		   // extract phase from legal description
		   String phase = "";
		   p = Pattern.compile("\\b(PH(?:ASE)?)S?\\s*([\\dA-Z-,]+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   phase = phase + " " + ma.group(2);
			   phase = phase.trim();
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			   m.put("PropertyIdentificationSet.SubdivisionPhase", phase); 
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   
		   // pt Township, Range, Section din legal
		   String section = "";
		   String township = "";
		   String range = "";
		   //(?is)SE(?:C|CT|CTION)\s(\d+)\sT(?:WP|OWN)\s(\d+)\sR(?:NG|ANGE)\s(\d+)
		   p = Pattern.compile("(?is)SEC(?:T|TION)?\\s(\\d+)\\sT(?:WP|OWN)\\s(\\d+)\\sR(?:NG|ANGE)\\s(\\d+)");
		   Matcher mp = p.matcher(legal);
		   if (mp.find())
		   {
			   section = mp.group(1);
			   township = mp.group(2);
			   range = mp.group(3);
			   //if (StringUtils.isEmpty(codeLocation))
			   //{	   
				   m.put("PropertyIdentificationSet.SubdivisionTownship", township);
			   	   m.put("PropertyIdentificationSet.SubdivisionRange", range);
			   	   m.put("PropertyIdentificationSet.SubdivisionSection", section);
			   //}
		   }
		   
		   // extract section from legal description
		   String sec = "";
		   p = Pattern.compile("\\b(SECT?(?:ION)?)\\s*(\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   sec = sec + " " + ma.group(2);
			   sec = sec.trim();
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			   if (StringUtils.isEmpty(section))
				   m.put("PropertyIdentificationSet.SubdivisionSection", sec);
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   
		// extract tract from legal description
		   String tract = "";
		   legal = legal.replaceAll("([A-Z])\\s&\\s([A-Z])","$1&$2");
		   p = Pattern.compile("\\b(TR(?:ACT)?)S?\\b\\s*([A-Z\\d&,]+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   tract = tract + " " + ma.group(2);
			   tract = tract.trim();
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }

		   // extract cross refs from legal description
		   List<List> bodyCR = new ArrayList<List>();
		   p = Pattern.compile("\\b(OR)\\s*(\\d+)\\s*(PG)S?\\s*(\\d+)\\b");
		   ma = p.matcher(legal);	      	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();		   
			   line.add(ma.group(2));
			   line.add(ma.group(4));
			   line.add("");
			   bodyCR.add(line);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));		   
		   } 
		   
		   if (!bodyCR.isEmpty()){		  		   		   
			   String [] header = {"Book", "Page", "InstrumentNumber"};		   
			   Map<String,String[]> map = new HashMap<String,String[]>();		   
			   map.put("Book", new String[]{"Book", ""});
			   map.put("Page", new String[]{"Page", ""});
			   map.put("InstrumentNumber", new String[]{"InstrumentNumber", ""});
			   
			   ResultTable cr = new ResultTable();	
			   cr.setHead(header);
			   cr.setBody(bodyCR);
			   cr.setMap(map);		   
			   m.put("CrossRefSet", cr);
		   }	  
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		   
		   //legal = legal.replaceAll("\\b\\s+TR\\s+\\b","");
		   legal = legal.replaceAll("BLK ", "");
		  
		   String subdiv = "";
		   p = Pattern.compile("(?is)\\s*(.*)(\\n)?\\b(UNIT|PHASE|BLOCK|PARCEL|TR|BLK|LOT|UNREC|TRACT|OR|L|PB|UNIT |MB|CB|RB)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   subdiv = ma.group(1);		   
		   } else {
			   p = Pattern.compile("\\b(?:\\s*)?(.+?)\\b(?:SEC|PHASE|PARCEL|LOT|TR|BLOCK|TRACT|TR|PB|MB|CB|RB|LT|BLK|PH |\\s+OR\\s+|UNIT |BLDG)");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   subdiv = ma.group(1);
			   }   
		   }
		  if (subdiv.length() != 0){
			   subdiv = subdiv.replaceAll("NO\\s+\\d+", "");
			   subdiv = subdiv.replaceFirst("(.*)(\\d)(ST|ND|RD|TH)\\s*(ADDN)", "$1" +"$2" );
			   subdiv = subdiv.replaceFirst("(.*)(PARCEL|PHASE)", "$1");
			   subdiv = subdiv.replaceFirst("(.*)(ADD)", "$1");
			   subdiv = subdiv.replaceFirst("(.*)(UNREC.*)", "$1");
			   subdiv = subdiv.replaceFirst("(.+)\\s+(?:\\bA\\b)?\\s+CONDO(MINIUM)?.*", "$1");
			   subdiv = subdiv.replaceFirst("(.+) SUB(DIVISION)?.*", "$1");
			   subdiv = subdiv.replaceFirst("\\bUNIT\\b(?:\\s+\\b(?:RE)?PLAT\\b)?", "");
			   subdiv = subdiv.replaceFirst("(.*)(\\s+-)", "$1");
			   subdiv = subdiv.replaceFirst("(.*)(\\d+)", "$1");
			   subdiv = subdiv.replaceFirst("COM\\s+AT", "");
			   subdiv = subdiv.replaceFirst("COR\\s+OF", "");
			   subdiv = subdiv.replaceFirst("(?is)\\bVERANDA\\b(?:\\s*\\d+)?\\s+\\bAT\\b\\s+", "");
			   subdiv = subdiv.replaceAll("\\d+\\s*FT\\s+OF","");
			   subdiv = subdiv.replaceAll("\\bOF\\b", "");
			   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			   if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		   }
		   
	}
	
	protected static String cleanOwnerFLCollierTR(String s){
		   s = s.toUpperCase();
	       //s = s.replaceFirst("\\b\\s*\\(H&W\\)", "");
	       //s = s.replaceFirst("\\b\\s*\\(F/D\\)", "");
	       //s = s.replaceFirst("\\bC/O\\b", "");
	       //s = s.replaceFirst("\\bL/E\\b","");
	       //s = s.replaceFirst("\\bTRUSTEES?|TRUST?| TR\\b", "");
	       //s = s.replaceAll("\\bEST?\\b", "");
	       //s = s.replaceAll("\\bCO-TR\\b", "");
	       //s = s.replace("%", "&");       
	       //s = s.replaceAll("\\bMRS\\b", "");
	       //s = s.replaceAll("\\bRLE\\b", "");	
		   //only for collier
		   //SMITH JR, CARL W

		   Matcher ma = suffixComa.matcher(s);
		   if (ma.find()){
			   String suff = ma.group(1);
			   s = ma.replaceFirst("");
			   if (s.contains("&")){
				   s = s.replaceFirst(" & ", " " + suff + " & ");
			   } else {
				   s += " " + suff;
			   }
		   }

		   s = s.replaceAll("\\s+GARDIAN$", "");
		   s = s.replaceAll("^AGENT FOR\\s+", "");
		   //s = s.replaceAll("^ITF\\s", "");
		   s = s.replaceAll("=", "");
		   s = s.replaceAll("\\s+UTD \\d+/\\d+/\\d+", "");
		   s = s.replaceAll("R/T", "REVOCABLE TRUST");
		   s = s.replaceAll("R/L/T", "REVOCABLE LIVING TRUST");
		   
	       s = s.replaceAll("\\*+", "");
		   
	       s = s.replaceAll(" AND ", " & ");
	       s = s.replaceFirst(" OR ", " & ");
	       s = s.replaceFirst("\\b LMT\\b", "");
	       s = s.replaceFirst("\\b MD PA\\b","");
	       s = s.replaceFirst("\\b PA\\b","");
	       s = s.replaceAll("\\s{2,}", " ").trim();
	       s = s.replaceAll(",,", ",");
	       //collier specific
	       s = s.replaceAll("\\d+(____|/)\\d+(____|/)\\d+", "TRUST");
	       s = s.replaceAll("^OF ", "");
	       s = s.replaceAll("SELF DECLARATION OF TRUST", "");
	       s = GenericFunctions2.resolveOtherTypes(s);
	       return s;
	   }
	
	@SuppressWarnings("rawtypes")
	public static void stdFinalFLCollierTR(ResultMap m,long searchId) throws Exception {	
		
		   String owner = (String) m.get("tmpOwnerName");
		   if (StringUtils.isEmpty(owner))
			   return;
	 
		   boolean val_de_adev = false;  // true cand avem WILLIAMS I sau II sau III, etc
		   try {
		   if (!owner.contains(",") && NameUtils.isCompany(owner))
			   //if (!owner.contains("&"))
		          m.put("PropertyIdentificationSet.OwnerLastName",owner);
		   else
		   {	   
		   if (owner.indexOf("&")<owner.indexOf(",") && owner.indexOf("&")!= -1)  // true cand avem situatia SMITH & ASSOC INC, PAUL L 			   
		   {
			   int cont = owner.indexOf("&");
			   String aux = "";
			   while (cont < owner.indexOf(","))
			   {
				   aux += owner.charAt(cont); 
				   cont++;
			   }
			   owner = owner.replaceAll(aux, "");
			   owner += " " + aux;
		   }
		   if (owner.contains(",") && owner.indexOf(",") < owner.length()-1)
		   for (int i=owner.indexOf(",")-2; i<owner.indexOf(",");i++)
		   {
			   if ((owner.charAt(i)==' ' && owner.charAt(i+1)=='I' && owner.charAt(i+2)==',') || 
				    (owner.charAt(i)=='I' && owner.charAt(i+1)=='I' && owner.charAt(i+2)==',')||
				    (owner.charAt(i)=='I' && owner.charAt(i+1)=='V' && owner.charAt(i+2)==',')||
				    (owner.charAt(i)=='V' && owner.charAt(i+1)=='I' && owner.charAt(i+2)==',')||
				    (owner.charAt(i)=='I' && owner.charAt(i+1)=='I' && owner.charAt(i+2)=='I' && owner.charAt(i+3)==','))  val_de_adev=true;
				   
		   }
		   owner = owner.replace(","," ");
		   String s1="";
		   if (val_de_adev)
		   { 
			   int poz = owner.indexOf(" ") ;
			   int j=poz+1;
			   while (owner.charAt(j)!=' ')
			   {
				   s1 += owner.charAt(j);
				   j++;
			   }
			   owner = owner.replaceAll(s1,"");
		   }
		 
		   String[] lines = owner.split("&");
		   String[] a = StringFormats.parseNameNashville(cleanOwnerFLCollierTR(lines[0]), true);
		   
		   String vtemp = a[1];
		   String aux="";
		   int pos=0;
		   if (vtemp.contains(" "))
		   {
			   for (int i=0; i<vtemp.length();i++)
				   if (vtemp.charAt(i)==' ') pos=i;
			   if (pos !=0) aux = vtemp.substring(pos+1,vtemp.length());
			   aux += " " + a[0];
			   a[0] = aux;
			   a[1] = a[1].substring(0,pos);
			   
			   aux = a[1];
			   a[1] = a[0];
			   a[0] = aux;					   
		   }
		   if (!s1.equals(""))
		   {
			   a[1] += " " + s1;
		   }
		   if ((a[5].length() == 0) && (lines.length >=2)){
			   String coowner = cleanOwnerFLCollierTR(lines[1]);
			   coowner = coowner.replaceAll("\\d+.*", "");
			   coowner = coowner.replaceFirst("^&\\s*", "");
			   String[] b = StringFormats.parseNameNashville(coowner, true); 
			  if (b[0].contains(a[2])) 
				   b = StringFormats.parseNameDesotoRO(coowner, true);
			  if (b[0].length()==1)
			  {
				  aux = b[0];
				  b[0] = b[1];
				  b[1] = aux;
			  }
			  if (b[2].contains("INC"))  //cand avem situatia SMITH & ASSOC INC, PAUL L  sa imi afiseze la coowner SMITH & ASSOC INC
			  {
				  b[2] = a[2] + " & " + b[2];
			  }
			  else
			  {	  
			  if (b[0].equals("")) 
			  {
				  aux = b[2];
				  b[2] = b[0];
				  b[0] = aux;					   
			  }
				  
			   if (b[2].equals(""))
				   b[2] = a[2];
			  }
		       b[1] = b[1].replaceFirst("^([A-Z]) [A-Z]{2,} [A-Z]{2,}( [A-Z])?.+", "$1"); 
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
		   }
		   catch(Exception e)
		   {
			   throw new Exception(e);
		   }
	}

	
    @SuppressWarnings("rawtypes")
	public static void partyNamesFLCollierTR(ResultMap m, long searchId) throws Exception {
		ArrayList<String> lines = new ArrayList<String>();

		int i;
		boolean isIntermPage = false;
		
		String owner = "";
		if (m.get("tmpOwnerInterm") != null) {
			isIntermPage = true;
			owner = (String) m.get("tmpOwnerInterm");
			
		} else {
			isIntermPage = false;
			owner = (String) m.get("tmpOwnerName");
		}
		
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		lines.add(owner);
		String tmpAddress;
		tmpAddress = (String) m.get("tmpAddress1");
		if (!StringUtils.isEmpty(tmpAddress)){
			lines.add(tmpAddress);
		}
		tmpAddress = (String) m.get("tmpAddress2");
		if (!StringUtils.isEmpty(tmpAddress)){
			lines.add(tmpAddress);
		}
		tmpAddress = (String) m.get("tmpAddress3");
		if (!StringUtils.isEmpty(tmpAddress)){
			lines.add(tmpAddress);
		}
		tmpAddress = (String) m.get("tmpAddress4");
		if (!StringUtils.isEmpty(tmpAddress)){
			lines.add(tmpAddress);
		}
		tmpAddress = (String) m.get("tmpAddress5");
		if (!StringUtils.isEmpty(tmpAddress)){
			lines.add(tmpAddress);
		}
		String prevLastName = "";
		Vector<String> excludeCompany = new Vector<String>();
		excludeCompany.add("PAT");
		excludeCompany.add("ST");
		
		Vector<String> extraCompany = new Vector<String>();
		extraCompany.add("BUZBY");
		
		List<List> body = new ArrayList<List>();
		if (lines.size() <= 2) {
			if (!isIntermPage) {
				return;
			}
		}
		// clean address out of mailing address
		ArrayList<String> lines2 = null;
		if (!isIntermPage) {
			lines2 = GenericFunctions.removeAddressFLTR2(lines,
					excludeCompany, extraCompany, new Vector<String>(), 2, 30);
		} else {
			lines2 = lines;
		}
		//lines2 = NameCleaner.splitName(lines2, excludeCompany);

		for (i = 0; i < lines2.size(); i++) {
			String[] names = { "", "", "", "", "", "" };
			boolean isCompany = false;
			// 1 = "L, FM & WFWM"
			// 2 = "F M L"
			// 3 = "F & WF L
			int nameFormat = 1;
			// C/O - I want it before clean because I don't clean company names
			String ln = lines2.get(i);
			if (ln.matches("(?i).*?c/o.*")
					||ln.matches("^\\s*%.*")) {
				ln = ln.replaceAll("(?i)c/o\\s*", "");
				ln = ln.replaceFirst("^\\s*%\\s*", "");
			}
			//ln = cleanOwnerFLCollierTR(ln);
			ln = NameCleaner.paranthesisFix(ln);
			
			String curLine = NameCleaner.cleanNameAndFix(ln, new Vector<String>(), true);
			if (curLine.matches("&\\s+\\w+\\s+\\w{1}") && !prevLastName.equals("")){
				curLine = curLine.replaceFirst("&\\s", prevLastName + ", ");
			}
			curLine = cleanOwnerFLCollierTR(curLine);
			Vector<Integer> ampIndexes = StringUtils.indexesOf(curLine, '&');
			Vector<Integer> commasIndexes = StringUtils.indexesOf(curLine, ',');
			
			if (NameUtils.isCompany(curLine, excludeCompany, true)) {
				// this is not a typo, we don't know what to clean in companies'
				// names
				ln = ln.replaceAll("^OF ", "");
				names[2] = ln.replaceAll("(DTD|UTD|TAD)?\\s+\\d+(-|/)\\d+(-|/)\\d+", "");
				isCompany = true;
			} else {
				if (commasIndexes.size() == 0){
					if (GenericFunctions.FWFL1.matcher(curLine).matches()
						 ||GenericFunctions.FWFL2.matcher(curLine).matches()){
							nameFormat = 3;
						} else if (GenericFunctions.FML.matcher(curLine).matches()
									|| GenericFunctions.FMML.matcher(curLine).matches()){
							//MELANIE J THOMPSON
							nameFormat = 2;
						}
				}else {
					if (commasIndexes.size() == 2){
						if (ampIndexes.size() == 0 && !curLine.matches(".*,\\s*\\w{1,2}$")){
							curLine = curLine.replaceFirst("\\s*,\\s*", "@@@").replaceFirst("\\s*,\\s*", " & ").replaceFirst("@@@", ", ");
							curLine = curLine.replaceFirst("(.*), (.*) &", "$2 $1 &");
							String[] n = curLine.split("\\s&\\s");
							curLine = n[0];
							lines2.add(n[1]);
							nameFormat = 3;
						}
					}
				}
				curLine = curLine.replaceAll("(?is)\\s+((?:TR(?:U?S)?(?:(?:TEE?)?S?)?\\s+)?ET(?:AL|UX|VIR)(?:\\s+TR(?:U?S)?(?:(?:TEE?)?S?)?)?)\\s*,\\s+([^&]+|[^$]+)", 
													", $2 $1");
				curLine = curLine.replaceAll("(?is)\\b(TR?(?:U?S)?(?:TEE?)?S?)\\s*,\\s+([^&]+|[^$]+)", ", $2 $1 ");
				switch (nameFormat) {
				case 1:
					names = StringFormats.parseNameLFMWFWM(curLine,
							excludeCompany, true, true);
					break;
				case 2:
					names = StringFormats.parseNameDesotoRO(curLine, true);
					break;
				case 3:
					names = StringFormats.parseNameFMWFWML(curLine, excludeCompany, true);
					break;
				}
			}
		
			String[] suffixes = { "", "" }, types = {"", ""}, otherTypes = {"", ""};
			String[] maiden = { "", ""};
			if (!isCompany) {
				names = NameCleaner.tokenNameAdjustment(names);
				if (nameFormat != 1 
					|| !names[2].equals(names[5]) && names[2].length() > 0 && names[5].length() > 0){
					names = NameCleaner.lastNameSwap(names);
					
				}
				types = GenericFunctions.extractAllNamesType(names);
				otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractAllNamesSufixes(names);
				
				maiden = NameFactory.getInstance().extractMaiden(names);
				names = NameCleaner.removeUnderscore(names);
				maiden = NameCleaner.removeUnderscore(maiden);
				names = fixFLCollierTR(names);
			}
			prevLastName = names[2];
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
					NameUtils.isCompany(names[2], excludeCompany, true), NameUtils
							.isCompany(names[5], excludeCompany, true), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		/*
		 * done multiowner parsing
		 */
	}	
    
    public static String[] fixFLCollierTR(String[] names){
    	String[] s = names[0].split(" ");
    	if (s.length == 2 ){
    		if (s[0].length() == 1){
    			names[0] = names[0].substring(2);
    			names[1] += " " + s[0];
    		} else if (s[1].length() == 1){
    			names[0] = names[0].substring(0, names[0].length()-2);
    			names[1] += " " + s[1];
    		}
    		
    	}

    	s = names[2].split(" ");
    	if (s.length == 2 ){
    		if (s[0].length() == 1){
    			names[2] = names[2].substring(2);
    			names[3] += " " + s[0];
    		} else if (s[1].length() == 1){
    			names[2] = names[2].substring(0, names[0].length()-2);
    			names[3] += " " + s[1];
    		}
    		
    	}
    	return names;
    	
    }
	public static void parseAddress(ResultMap m, long searchId) throws Exception {
			String address = (String) m.get("tmpAddress");
			if(address != null){
				if (address.contains("TRS="))
					return;
				
				m.put("PropertyIdentificationSet.StreetName",  StringFormats.StreetName(address.trim()));
				m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address.trim()));
			}
	   }
    
}

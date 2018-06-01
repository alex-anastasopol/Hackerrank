package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
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
import ro.cst.tsearch.search.address.Normalize;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLOrangeTR 
  {
	   public static String goToPartyNameTokenizerFLOrangeTR(String s)
	   {
		 return partyNameTokenizerFLOrangeTR(s);   
	   }
	
	   protected static String partyNameTokenizerFLOrangeTR(String s)
	   {
		   if(StringUtils.isEmpty(s))
			   return null;
		   s = s.replaceAll("(?:\\d+/\\d+|\\d+%)\\s(?:INT\\s(?:ETAL|N|S|E|W)?)?","");
		 //raman doar numele ownerilor dupa regula: tot ce e dupa un numar urmat de un cuvant sau tot ce e dupa PO BOX urmat de un numar dispare
		   s = s.replaceFirst("(?:PO BOX)?\\s*\\d+\\s*[A-Z\\d\\s-,]+","");   
		   if (s.contains("C/O") || (s.contains("REM:"))) 
		     {
			   String s1 = "";
			   if (s.contains("C/O")) s1= s.substring(s.indexOf('/')+3);
			      else	s1 = s.substring(s.indexOf(":")+2);
			   String lastName1 = "", lastName2 = "";
			   if (!NameUtils.isCompany(s1) || (s1.matches("(.*)\\s\\b(TR|TRUST|TRST|TRU|TRUS)\\b")))
			   {
				   if (s1.contains("&"))
				   {
					   /* cazul 	SMITH FREDDIA LEE ESTATE				(PIN: 26-22-29-6716-13-043)  
									C/O MARY CABBLE & LEIDA B LAWRENCE                                   */			   
					   if (s1.matches("[A-Z]+\\s[A-Z]\\s&\\s[A-Z\\s-]+"))			// cazul: s1 = MARY C & JOHN MATHEW SMITH
					   	{
						   String aux = s1;
						   if (s1.matches("[A-Z]+\\s[A-Z]\\s&\\s[A-Z\\s-]+\\s\\b(TR|TRUST|TRST|TRU|TRUS)\\b"))		// cazul: s1 = JAMES C & MARY LEE SMITH TRUST
						     {
							   aux = aux.replaceFirst("[A-Z]+\\s[A-Z]\\s&\\s[A-Z\\s-]+\\s\\b(TRUST|TRST|TRU|TRUS)\\b","$1");
							   s1 = s1.replaceFirst("([A-Z]+\\s[A-Z]\\s&\\s[A-Z\\s-]+)\\s\\b(TRUST|TRST|TRU|TRUS)\\b","$1");
						     }
						   lastName2 = s1.substring(s1.lastIndexOf(' ')+1);
						   s1 = s1.replaceFirst(lastName2,"");
						   s1 = s1.replaceFirst("([^&]+)&\\s(.*)",lastName2 + " $1" + "& " + lastName2 + " $2");
						   if (aux.indexOf(' ') == -1) //am TR sau TRUST sau ...
							   s1 += aux;
					   	}
					   else
						   if (s1.matches("[A-Z]+\\s([A-Z]\\s)?[A-Z][A-Z]+\\s&\\s[A-Z\\s-]+"))		// cazul: C/O MARY CABBLE & LEIDA B LAWRENCE
						   {
							   lastName1 = s1.substring(s1.indexOf(' ')+1, s1.indexOf('&'));	//last name-ul primului owner
							   lastName1 = lastName1.replaceAll(" ","");
							   lastName2 = s1.substring(s1.lastIndexOf(' ')+1);		// last name-ul celui de-al doilea owner
							   s1 = s1.replaceFirst("([A-Z]+\\s(?:[A-Z]\\s)?)[A-Z][A-Z]+\\s&\\s([A-Z]+\\s(?:[A-Z]\\s)?)[A-Z]+",lastName1 + " $1" + " & " + lastName2 + " $2"); 							   
						   }
					   if (s.contains("C/O")) s = s.replaceFirst("C/O [A-Z\\s-&]+",s1);
					   else
						   if (s.contains("REM:")) s =s.replaceFirst("REM:\\s*[A-Z\\s-&]+",s1);
				   }
				   else
				   {  
					if (s1.matches("[A-Z\\s-]+\\b(TRUST|TRST|TRU|TRUS)\\b"))  // C/O BETTY JOYCE THWEATT TR (PIN: 19-22-30-6752-00050)	-> daca era TRUST ramanea, asa TR dispare
						{
							String aux = s1;
							s1 = s1.replaceFirst("([A-Z\\s-]+)\\s\\b(TRUST|TRST|TRU|TRUS)\\b","$1");
							aux = aux.replaceFirst("([A-Z\\s-]+)\\b(TRUST|TRST|TRU|TRUS)\\b","$2");
							lastName2 = s1.substring(s1.lastIndexOf(' ')+1);
							s1 = s1.replaceFirst("\\s"+lastName2,"");
							s1 = lastName2 + " " + s1 + " " + aux;
						}
					else  // C/O PATTIE SMITH JACOBUS (PIN: 18-22-33-6217-00-730)
					  {
						   lastName2 = s1.substring(s1.lastIndexOf(' ')+1);
						   s1 = s1.replaceFirst(lastName2,"");
						   s1 = lastName2 + " " + s1;
					  }
					if (s.contains("C/O")) 
						s = s.replaceFirst("C/O [A-Z\\s-]+",s1);
					else
						if (s.contains("REM:")) 
							s = s.replaceFirst("REM: [A-Z\\s-]+",s1);
				   }
				   
			   }
			   else
			   {
				   // cazuri de tipul: C/O MARGES SPECIALTIES (PIN: 04-22-29-0000-00012)
				   s = s.replaceFirst("C/O ","");
				   s = s.replaceFirst("REM: ","");
			   }
		     }
		   s = s.replaceAll("\\s{2,}"," & ");		// separ ownerii prin "&"					
		   s = s.replaceFirst("\\s&\\s\\Z","");
		   s = s.replaceAll("&\\s*&","&");
		   s = s.replaceAll("ETAL","");
		   s = s.replaceAll("\\bTR\\b","");
		  /*  pentru versiune viitoare se aplica regula de mai jos
		   if ((s.contains("TRUST")))		// noua regula: ownerul ce are TRUST e parsat ca: I owner: xxx yyy z TRUST si al-II-lea: xxx yyy z 	
		   {
			  //s = s.replaceAll("([A-Z\\s-]+)(\\s\\bTR(?:UST)?\\b)","$1 & $1$2");   -> in cazul in care se va hotari sa fie si TR in regula de mai sus
			  s = s.replaceAll("([A-Z\\s-]+)(\\s\\bTRUST\\b)","$1 & $1$2");  
		   }
		   */
		   return s;
	   }
	   
	
	   @SuppressWarnings("unchecked")
	public static void partyNamesFinalFLOrangeTR(ResultMap m, String s) throws Exception
	   {
		   String[] owners = s.split("&"); 
		   List<List> body = new ArrayList<List>();
		   String[] names = {"", "", "", "", "", ""};
		   String[] suffixes;
		   
		  	for (int i=0; i<owners.length; i++)
		  	  {
		  		boolean lifeEst = false;
		  		String ow = owners[i], aux="";
		  		if (ow.matches(".*\\b((?:LIFE\\s*)?ESTATE)\\b\\s*"))
		  		{
		  			aux = ow;
		  			aux = aux.replaceAll(".*\\b((?:LIFE\\s*)?ESTATE)\\b","$1");
		  			lifeEst = true;
		  		}
		  		if (lifeEst) 				// in parseNameNashville se stergea ESTATE sau LIFE ESTATE
		  			names[2] = owners[i];
		  		else
		  		{
		  			names = StringFormats.parseNameNashville(ow);
		  			if (ow.contains("LA JAUNESSE"))
		  			{
		  				names[0] += " " + names[1].substring(0, names[1].lastIndexOf(' '));
		  				names[1] = names[1].substring(names[1].lastIndexOf(' ')+1);
		  			}
		  		}
		  			
		  		suffixes = GenericFunctions.extractNameSuffixes(names);        
		  		GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		  	  }
		  	GenericFunctions.storeOwnerInPartyNames(m, body);
	   }
	   
	   
	   public static void taxFLOrangeTR(ResultMap m, long searchId) throws Exception {
		   String htmlInfo = (String) m.get("tmpTaxInfo");
		   String year = "";
		   BigDecimal priorDelinq = new BigDecimal("0.00");
		   BigDecimal amountPaid = new BigDecimal("0.00");
		   BigDecimal amountDue = new BigDecimal("0.00");
		   //amtDue = (String) m.get(TaxHistorySetKey.TOTAL_DUE.getKeyName());
		   
		   if (htmlInfo != null) {
			   HtmlParser3 htmlParser = new HtmlParser3(htmlInfo);	
				NodeList nodeList = htmlParser.getNodeList();
				if (nodeList != null) {
					Node node =  nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_cellTaxYearInfo"), true)
							.elementAt(0);
					if (node != null) {
						year = node.getFirstChild().toHtml();
						year = year.replaceFirst("(?is)\\s*Tax Year\\s*:\\s*(\\d{4})\\s*", "$1");
						if (StringUtils.isNotEmpty(year)) {
							m.put(TaxHistorySetKey.YEAR.getKeyName(), year);
						}
					}
						
					node = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_mainContent_tableUnpaidTaxes"), true)
							.elementAt(0);
					if (node != null) {
						List<List> bodyRT = new ArrayList<List>();
						List<String> line = null;
							
						TableTag taxHistTable = (TableTag) node;
						TableRow[] rows = taxHistTable.getRows();
						if (rows.length > 1) {
							for (int i=1; i<rows.length; i++) {
								line = new ArrayList<String>();
								if (rows[i].getColumnCount() == 5) {
									String yearInfo = rows[i].getColumns()[0].getChildrenHTML().trim();
									String amtPaymentInfo = rows[i].getColumns()[3].getChildrenHTML().trim();
									String amtDueInfo = rows[i].getColumns()[2].getChildrenHTML();
									String receiptNo = "";
									String amtPaid = "";
									String receiptDate = "";
									
									amtDueInfo = amtDueInfo.replaceAll("[\\$,\\s\\*]", "").trim();
									amtDueInfo = amtDueInfo.replaceAll("(?is)</?SPAN>", "");
									
									Matcher infoPmtMatcher = Pattern.compile("(?is)[\\sA-Z]*([\\d-]+)\\s*#\\s*([\\.\\$\\d,]+)\\s*#\\s*(\\d{1,2}\\s*/\\s*\\d{1,2}\\s*/\\s*\\d{4})\\s*")
											.matcher(amtPaymentInfo);
									
									if (infoPmtMatcher.find() && "PAID".equals(amtDueInfo)) {
										receiptNo = infoPmtMatcher.group(1).trim();
										amtPaid = infoPmtMatcher.group(2).replaceAll("[\\$,]", "").trim();
										receiptDate = infoPmtMatcher.group(3).trim();
										line.add(receiptNo);
										line.add(amtPaid);
										line.add(receiptDate);
										bodyRT.add(line);
										
										try {
											if (yearInfo.equals(year)) { // current tax year - single payment
												amountPaid = new BigDecimal(amtPaid);
											} else if (yearInfo.contains("Installment") && yearInfo.contains(year)) { // current tax year - instalments
												amountPaid = amountPaid.add(new BigDecimal(amtPaid));
											}
										} catch (NumberFormatException e) {
											amountPaid = amountPaid.add(new BigDecimal("0.00"));
										}
										
										
									} else {
										if (yearInfo.equals(year) || yearInfo.contains(year)) {
											//unpaid value - for current tax year
											try {
												if (yearInfo.contains("Installment") && yearInfo.contains(year)) { //installments due
													amountDue = amountDue.add(new BigDecimal(amtDueInfo));
												} else {
													amountDue = new BigDecimal(amtDueInfo);
												}
											} catch (NumberFormatException e) {
												amountDue = amountDue.add(new BigDecimal("0.00"));
											}
											
										} else {
											//unpaid value - for previous years
											try {
												priorDelinq = priorDelinq.add(new BigDecimal(amtDueInfo));
											 } catch (NumberFormatException e){
												priorDelinq = priorDelinq.add(new BigDecimal("0.00"));
											 }
										}
									}
									
								}
							}
							
							ResultTable rt = new ResultTable();
							String[] header = {"ReceiptNumber", "ReceiptAmount", "ReceiptDate"};
							rt = GenericFunctions2.createResultTable(bodyRT, header);
							m.put("TaxHistorySet", rt);
						}
						
						m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid.toString());
						m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue.toString());
						m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinq.toString());		   	 
					}
				}
		   }
	   }
	   
	   @SuppressWarnings("unchecked")
	   public static void taxFLOrangeTR_old(ResultMap m, long searchId) throws Exception {
		   ResultTable receipts = (ResultTable) m.get("tmpTaxHistorySet");
		   String year = (String) m.get("TaxHistorySet.Year");
		   
		   if (receipts != null){
			   //String body[][] = receipts.body;
			   String body[][] = receipts.getBodyRef();
			   BigDecimal amtPaid = new BigDecimal("0.00");
			   BigDecimal amountDueInstallments = new BigDecimal("0.00");
			   boolean hasInstallments = false;
			   for (int i = 0; i< body.length; i++){
				   if (body[i][0] != null){
					   if (year.equals(body[i][0].trim())){
						   if (StringUtils.isNotEmpty(body[i][2])){
							   String[] items = body[i][2].split(" # ");
							   if (items.length > 2){
								   amtPaid = amtPaid.add(new BigDecimal(items[1]));
								   break;
							   }
						   } else 
							   break;
					   }
					else if (body[i][0].matches("^\\s*" + year + ".*")) {
						hasInstallments = true;
						if (StringUtils.isNotEmpty(body[i][2])) {
							String[] items = body[i][2].split(" # ");
							if (items.length > 2) {
								amtPaid = amtPaid.add(new BigDecimal(items[1]));
							}
						} else
							break;
					}
				}
			   }
			//total due if tax year has installments
			if (body.length >= 4 && hasInstallments) {
				for (int i = 0; i < 4; i++) {
					if (body[i][0].matches("^\\s*" + year + ".*") && body[i][1].matches("\\s*$?\\d+[\\d.,\\s]*")) {
						amountDueInstallments = amountDueInstallments.add(new BigDecimal(body[i][1].replaceAll("[ $,-]", "")));
					}
				}
				m.put("TaxHistorySet.TotalDue", amountDueInstallments.toString());
			}
			   m.put("TaxHistorySet.AmountPaid", amtPaid.toString());
			   
			   List<List> bodyRT = new ArrayList<List>();
			   List<String> line = null;;
			   
			   for (int i = 0; i < body.length; i++){
				   if (StringUtils.isNotEmpty(body[i][2])){
					   String[] items = body[i][2].split(" # ");
					   line = new ArrayList<String>();
					   if (items.length > 2){
						   line.add(items[0].replaceAll("(?is)\\s*PAID\\s*", "").trim());
						   line.add(items[1].trim());
						   line.add(items[2].trim());
						   bodyRT.add(line);
					   }
				   }
			   }
			   
			   ResultTable rt = new ResultTable();
				
			   String[] header = {"ReceiptNumber", "ReceiptAmount", "ReceiptDate"};
			   rt = GenericFunctions2.createResultTable(bodyRT, header);
			   m.put("TaxHistorySet", rt);
		   }
		   
		   String amtDue = (String) m.get("TaxHistorySet.TotalDue");
		   if (StringUtils.isEmpty(amtDue)){
			   amtDue = "0.00";
		   }
		   // adjust the total Due as the some of all installments due for installments payments (e.g. PID 05-23-31-2000-00-651)
		   try {
			   //BigDecimal amtDueTotal = new BigDecimal(GenericFunctions1.sum((String) m.get("tmpAmtDue"), searchId));
			   BigDecimal amtDueTotal = new BigDecimal(GenericFunctions1.sum(amtDue, searchId)); //B3677
			   BigDecimal baseAmt;
			   if (StringUtils.isEmpty((String)m.get("TaxHistorySet.BaseAmount"))){
				   baseAmt = new BigDecimal("0.00");
			   } else {
				   baseAmt = new BigDecimal((String) m.get("TaxHistorySet.BaseAmount"));
			   }
			   if (amtDueTotal.compareTo(baseAmt) <= 0){
				   amtDue = amtDueTotal.toString();
				   m.put("TaxHistorySet.TotalDue", amtDue);
			   }
			   
		   } catch (Exception e) {
			   e.printStackTrace();
		   }
		   m.put("TaxHistorySet.CurrentYearDue", amtDue);
		   
		   Date dateNow = new Date();	   	   
		   String crtYearDelinq = "0.00";
		   BigDecimal priorDelinq = new BigDecimal("0.00");	
		   
//		   Date dateNow = new Date();	   	   
//		   String crtYearDelinq = "0.00";
//		   BigDecimal priorDelinq = new BigDecimal("0.00");	
//		   
//		   Date dueDate = DBManager.getDueDateForCountyAndCommunity(
//	               InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity()
//	                       .getID().longValue(), InstanceManager.getManager()
//	                       .getCurrentInstance(searchId).getCurrentCounty().getCountyId()
//	                       .longValue());
//	       
//	       if (dateNow.after(dueDate)){
//	    	   crtYearDelinq = amtDue;
//	       }
	       
		   ResultTable delinq = (ResultTable) m.get("tmpDelinqTable"); 
		   if (delinq != null){
			   //String body[][] = delinq.body;
			   String body[][] = delinq.getBodyRef();
			   if (body.length != 0){
				   int colIndex = 1; // delinquent amounts are recorded in "Current Payoff" column
				   
				   boolean foundCrtYear = false;			   		   
				   for (int i=0; i<body.length; i++){
					   if (!foundCrtYear && year != null && body[i][0].contains(year)){
						   foundCrtYear = true;
						   crtYearDelinq = body[i][colIndex];
						   if (crtYearDelinq != null && crtYearDelinq.length() != 0){					  
							   if (amtDue == null || amtDue.length() == 0 || Double.parseDouble(amtDue)==0.00){
								   m.put("TaxHistorySet.TotalDue", crtYearDelinq);
								   m.put("TaxHistorySet.CurrentYearDue", crtYearDelinq);
							   }
						   } else {
							   crtYearDelinq = "0.00";
						   }					   
					   } else {
						   try {
							   priorDelinq = priorDelinq.add(new BigDecimal(body[i][colIndex]));
						   } catch (NumberFormatException e){
							   priorDelinq = priorDelinq.add(new BigDecimal("0.00"));
						   }
					   }
				   }			   			   
			   }
		   }
		   
		   m.put("TaxHistorySet.PriorDelinquent", priorDelinq.toString());		   	 
	       m.put( "TaxHistorySet.DelinquentAmount", priorDelinq.add(new BigDecimal(crtYearDelinq)).toString());   
	   }
	
	   @SuppressWarnings("unchecked")
	public static void legalTokenizerRemaksFLOrangeRV(ResultMap m, String legal) throws Exception{
		   
		   //initial corrections and cleanup of legal description
		   legal = legal.replaceAll("(\\d)ETC\\b\\.?", "$1");
		   legal = legal.replaceAll("\\bCONDOMINIIUM\\b", "CONDOMINIUM");
		   legal = legal.replaceAll("\\bETC\\b\\.?", "");
		   legal = legal.replaceAll("\\b(NUMBER|NO|NIMBER)\\b", "");
		   legal = legal.replaceAll("\\bDESC\\b\\.?", "");	   	   
		   legal = legal.replaceAll("\\bREP(LAT)?\\b", "");
		   legal = legal.replaceAll("\\bPL\\b", "");
		   legal = legal.replaceAll("\\bWK\\s+\\d+([\\s,-]+\\d+)*\\b", "");
		   
		   legal = GenericFunctions.replaceNumbers(legal);
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics
		   	   
		   legal = legal.trim();
		   legal = legal.replaceAll("\\s{2,}", " ");	   
		   
		   // extract and replace unit from legal description
		   String unit = "";
		   String bldg = "";
		   String patt = "(?:\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+[A-Z]?)?)";
		   Pattern p = Pattern.compile("\\b(?:UN(?:IT)?|APT) ("+patt+"(?:-"+patt+")?)(?: (\\d+))?\\b");
		   Matcher ma = p.matcher(legal);
		   while (ma.find()){
			   unit = unit + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");
			   bldg = ma.group(2);
			   if (!StringUtils.isEmpty(bldg)){
				   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			   } else {
				   bldg = "";
			   }
			   if (ma.start() != 0){
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "UNIT ");
			   } else {
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }
		   }
		   unit = unit.trim();
		   if (unit.length() != 0){
			   String unit2 = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			   if (!StringUtils.isEmpty(unit2)){
				   unit2 = unit2 + " " + unit;
			   } else {
				   unit2 = unit;
			   }		   
			   unit2 = LegalDescription.cleanValues(unit2, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionUnit", unit2);		  
			   legal = legal.trim().replaceAll("\\s{2,}", " ");		   
		   }
		   	   
		   // extract and replace building # from legal description	   
		   p = Pattern.compile("\\b(?:BLDGE?|BUILDING) (\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   bldg = ma.group(1);
			   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			   if (ma.start() != 0){
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "BLDG ");
			   } else {
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and replace block from legal description
		   String block = "";
		   p = Pattern.compile("\\bBLKL? (\\d+|[A-Z])\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   block = ma.group(1);
			   String block2 = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			   if (!StringUtils.isEmpty(block2)){
				   block2 = block2 + " " + block;
				   block2 = LegalDescription.cleanValues(block2, false, true);
			   } else {
				   block2 = block;
			   }
			   m.put("PropertyIdentificationSet.SubdivisionBlock", block2);
			   if (ma.start() != 0){
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "BLK ");
			   } else {
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");			   
			   }
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and replace tract from legal description
		   String tract = "";
		   patt = "[A-Z]";
		   p = Pattern.compile("\\bTR(?:ACTS?)? ("+patt+"(?:[\\s&,-]+"+patt+")*)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   tract = ma.group(1);
			   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			   if (ma.start() != 0){
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "TRACT ");
			   } else {
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and replace phase from legal description
		   String phase = "";
		   patt = "(\\d+|[A-Z]\\d*)";
		   p = Pattern.compile("\\bPH(?:ASE)? ("+patt+")\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   phase = ma.group(1);
			   String[] exceptionTokens1 = {"M", "C", "L", "D"};
			   phase = Roman.normalizeRomanNumbersExceptTokens(phase, exceptionTokens1); // convert roman numbers to arabics
			   m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			   if (ma.start() != 0){
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "PHASE ");
			   } else {
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and remove section, township and range from legal description	   
		   List<List> body = GenericFunctions2.goToGetSTRFromMap(m); //first add sec-twn-rng extracted from XML specific tags, if any (for DT use)	   
		   p = Pattern.compile("\\bSEC(?:TION)? (\\d+)(?:[\\s-]+(\\d+)[\\s-]+(\\d+))?\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){	
			   List<String> line = new ArrayList<String>();
			   String sec = ma.group(1);
			   String twn = ma.group(2);
			   String rng = ma.group(3);		   
			   if (twn == null){
				   twn = "";
			   }
			   if (rng == null){
				   rng = "";	
			   }
			   line.add(sec);
			   line.add(twn);
			   line.add(rng);
			   body.add(line);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   GenericFunctions2.goToSaveSTRInMap(m, body);
		  
		   // extract and replace cross refs from legal description
		   List<List> bodyCR = new ArrayList<List>();
		   p = Pattern.compile("\\bBK (\\d+) PG (\\d+)\\b");
		   ma = p.matcher(legal);   	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();		   
			   line.add(ma.group(1));
			   line.add(ma.group(2));
			   line.add("");
			   line.add("");
			   bodyCR.add(line);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "BK ");		   
		   }
		   
		   p = Pattern.compile("\\b(?:(?:OR|BK) )?(\\d+)/(\\d+)");
		   ma = p.matcher(legal);   	   
		   if (ma.find()){
			   List<String> line = new ArrayList<String>();		   
			   line.add(ma.group(1));
			   line.add(ma.group(2));
			   line.add("");
			   line.add("");
			   bodyCR.add(line);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "BK ");		   
		   }
		   GenericFunctions2.goToSaveCRInMap(m, bodyCR); 
		   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   
		   // extract and replace lot from legal description
		   String lot = ""; // can have multiple occurrences
		   patt = "(?:\\d+-?[A-Z]\\b|\\d{1,4})";
		   p = Pattern.compile("^(?:L )?("+patt+"(?:[\\s-]+"+patt+")*)(?= [A-Z])");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   lot = ma.group(1);
			   String lot2 = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			   if (!StringUtils.isEmpty(lot2)){
				   lot2 = lot2 + " " + lot;
			   } else {
				   lot2 = lot;
			   }
			   lot2 = LegalDescription.cleanValues(lot2, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot2);
			   if (ma.start() != 0){
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "LOT ");
			   } else {
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract subdivision name - only if lot or block or unit or tract or phase was extracted 
		   // or legal contains CONDO(MINIIUM)?or SUB(DIVISION)? or legal starts with UN(IT)?; otherwise the text might be a doc type
		   // additional cleaning before extracting subdivision name	   
		   legal = legal.replaceFirst("^PAR(CEL)?\\b( \\d+\\b)?", "");
		   legal = legal.trim();
		   String subdiv = "";
		   p = Pattern.compile("(.*?)(?:\\b(?:BK|PHASE|UNIT|PAR(?:CEL)?)\\b\\s*)*\\s*\\b(?:SUB(?:DIVISION)?|CONDO(?:MINIUM)?)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   subdiv = ma.group(1);	   
			   subdiv = subdiv.replaceFirst(".*(?:\\b(?:TRACT|UNIT|BLDG|LOT|BLK|PHASE|PAR(?:CEL)?)\\b\\s*)+", "").trim();
		   } else if (lot.length() != 0 || block.length() != 0 || unit.length() != 0 || phase.length() != 0 || tract.length() != 0 || legal.matches("UN(IT)?\\s+.+")){
			   p = Pattern.compile("^(?:\\b(?:TRACT|UNIT|BLDG|LOT|BLK|PHASE|PAR(?:CEL)?)\\b\\s*)*(.*?);?\\s*(?:\\b(?:BK|PHASE|UNIT|PAR(CEL)?)\\b|$)");		   
			   ma = p.matcher(legal);
			   if (ma.find()){
				   subdiv = ma.group(1).trim();
			   }
		   }	   
		   if (subdiv.length() != 0){
			   subdiv = subdiv.replaceFirst("\\s+[A-Z]{2,3}\\s*-?\\s*\\d+(\\s*-?\\s*\\d+(\\s*-?\\s*\\d+)?)?(/[A-Z])?$", "");
			   subdiv = subdiv.replaceFirst("\\s+[A-Z]{2}\\s*-?\\s*[A-Z]\\s*-?\\s*\\d+(\\s*-?\\s*\\d+)?(/[A-Z])?$", "");
			   subdiv = subdiv.replaceFirst("\\s+\\d+\\s*[A-Z]?$", "");
			   subdiv = subdiv.replaceFirst("\\s+\\d+[A-Z]+$", "");
			   subdiv = subdiv.replaceFirst("\\d+(ST|ND|RD|TH)? ADD(ITION)?\\b.*", "");
			   subdiv = subdiv.replaceFirst("\\bADD \\d+\\b.*", "");
			   subdiv = subdiv.replaceFirst("^UN(?:IT)?\\s+(.+)", "$1");
		   }
		   if (subdiv.length() != 0){
			   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			   if (legal.matches(".*\\bCONDO(MINIUM)?\\b.*")){
				   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }
		   }
	   }	
	   
	   @SuppressWarnings("unchecked")
	public static void legalTokenizerFLOrangeTR(ResultMap m, String legal) throws Exception {
		   legal = legal.replaceAll("\\b([NWSE]{1,2})\\s+(\\d)", "$1$2");
		   //B3578
		   /*
		   // legal is composed of several lines, some of the words were split in different lines and need to be concatenated
		   legal = legal.replaceAll("/@@", "/");
		   legal = legal.replaceAll("@@/", "/");
		   legal = legal.replaceAll("\\(@@", "(");
		   legal = legal.replaceAll("@@\\)", ")");
		   legal = legal.replaceAll("\\b([NWES]{1,2}\\d*)@@(\\d)", "$1$2");
		   
		   legal = legal.replaceAll("\\b(LOT \\d+)@@(\\d)", "$1$2");
		   legal = legal.replaceAll("\\b(UNIT \\d+)@@(\\d)", "$1$2");
		   legal = legal.replaceAll("\\b(\\d+)@@(\\d+ (?:FT|TO))\\b", "$1$2");	   
		   legal = legal.replaceAll("(\\d[-\\.])@@(\\d)", "$1$2");
		   legal = legal.replaceAll("(\\d)@@([-\\.]\\d)", "$1$2");	   
		   legal = legal.replaceAll("\\b(\\d+[\\./]\\d+)@@(\\d)", "$1$2");
		   legal = legal.replaceAll("( \\d{2})@@(\\d{2}/\\d)", "$1$2"); // PID 02-21-28-0000-00039
		   legal = legal.replaceAll("(-\\d+)@@(\\d+-)", "$1$2");
		   */
		   String tokens = " LOT LOTS BEG SEC BLK BLOCK BLKS BLOCKS UNIT PHASE OR CONDO CONDOMINIUM LESS COMM MISC ALLEY RD FT TO ";
		   Pattern p = Pattern.compile("\\b([A-Z]+@@[A-Z]+)\\b");
		   Matcher ma = p.matcher(legal);
		   StringBuffer sb = new StringBuffer();
		   while (ma.find()){
			  //String word = ma.group(1).replace("@@", "");
			   String word = ma.group(1);
			  if (tokens.contains(" " + word + " ")){
				  ma.appendReplacement(sb, word);
			  }
		   }
		   ma.appendTail(sb);
		   legal = sb.toString();
		   	   
		   //legal = legal.replaceAll("@@", " ").replaceAll("\\s{2,}", " ");
		   legal = legal.replaceAll("\\s{2,}", " ");
		   m.put("PropertyIdentificationSet.PropertyDescription", legal);	   
		   
		   // clean legal description in preparation for extracting the legal tokens
		   legal = legal.replaceAll("\\s*\\.\\s*$", "");
		   legal = legal.replaceFirst("\\s*\\bERROR IN LEGAL DESCRIPTION\\b\\s*", " ");
		   legal = legal.replaceAll("\\s*\\bREPLAT\\b\\s*", " ").trim();
		   legal = GenericFunctions.replaceNumbers(legal);
		   
		   // extract subdivision name
		   String subdiv = "";
		   String legalTemp = legal;
		   legalTemp = legalTemp.replaceFirst("^\\d+/\\d+\\b\\s*", "");
		   legalTemp = legalTemp.replaceFirst("^UNIT \\d+\\s", "").trim();
		   legalTemp = legalTemp.replaceAll("^\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*", ""); // fix for bug #2299
		   
		   p = Pattern.compile("(.*?)(?:\\s+|\\s*-\\s*|\\b)(?:[A-Z]/\\d+|\\d+/\\d+|TRACT|PH(ASES?)?|(\\d+(ST|ND|RD|TH) )?ADD(ITION)?|SECTION|U(NI)?TS?|BLDG|CONDO(MINIUM)?|SUB|PARCEL)\\b.*");
		   ma = p.matcher(legalTemp);
		   if (ma.find()){
			   subdiv = ma.group(1).trim();
			   if (subdiv.matches(".*?\\bSEC \\d+-\\d+-\\d+\\b.*"))
				   subdiv = "";
			   subdiv = subdiv.replaceAll("( NO\\.?)? \\d+\\b\\s*", "").trim();		   
			   
			   if (subdiv.length() != 0){		   
				   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				   if (legal.matches(".*?\\bCONDO(MINIUM)?\\b.*"))
					   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }
		   } 
		   
		   // additional legal description cleanup before extracting the rest of the legal elements
		   legalTemp = legal;	     
		   legalTemp = legalTemp.replaceAll("\\bNO\\b\\.?\\s*", "");
		   legalTemp = legalTemp.replaceAll("\\s*\\bAND\\b\\s*", " ");
		   legalTemp = legalTemp.replaceAll("\\s*[&,]\\s*", " ");	  
		   legalTemp = legalTemp.replaceAll("\\b\\d+[\\./]\\d+\\b\\s*", "");
		   legalTemp = legalTemp.replaceAll("\\b[A-Z]/\\d+\\b\\s*", "");
		   legalTemp = legalTemp.replaceAll("\\.", "");
		   
		   // extract section from legal description
		   p = Pattern.compile("\\bSECTION (\\d+)");
		   ma = p.matcher(legalTemp);
		   if (ma.find()){
			   String sec = ma.group(1);		   
			   m.put("PropertyIdentificationSet.SubdivisionSection", sec);
		   }
		   	  	   	   	  
		   // extract building # from legal description
		   p = Pattern.compile("\\bBLDG (\\d+|[A-Z])");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		   }
		   	
		   // extract unit from legal description
		   StringBuilder unit = new StringBuilder(); // can have multiple occurrences
		   p = Pattern.compile("\\bU(?:NI)?TS?((\\s+(\\d+(-?[A-Z])?|[A-Z]-?\\d+|[A-Z]))+)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   unit.append(" ").append(ma.group(1));
		   }	   
		   if (unit.length() != 0){
			   m.put("PropertyIdentificationSet.SubdivisionUnit", LegalDescription.cleanValues(unit.toString().trim(), false, true));
		   }
		   
		   // extract phase from legal description
		   StringBuilder phase = new StringBuilder(); // can have multiple occurrences
		   p = Pattern.compile("\\bPH(?:ASES?)?((\\s+\\d+(-?[A-Z])?)+)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   phase.append(" ").append(ma.group(1));
		   }
		   if (phase.length() != 0){
			   m.put("PropertyIdentificationSet.SubdivisionPhase", LegalDescription.cleanValues(phase.toString().trim(), false, true));
		   }
		   
		   // extract tract from legal description	   
		   p = Pattern.compile("\\bTRACT (([A-Z]-?)?\\d+( [A-Z])?)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(1));
		   }
		   	   
		   String lotFromPid = "";
		   String blockFromPid = "";
		   String secFromPid = "";
		   String twnFromPid = "";
		   String rngFromPid = "";
		   String pid = (String) m.get("PropertyIdentificationSet.ParcelID");
		   if (pid != null && pid.length() != 0){
			   blockFromPid = pid.replaceFirst("\\w{2}-\\w{2}-\\w{2}-\\w{4}-(\\w{2})\\w{3}", "$1").replaceFirst("^0*(\\d+)", "$1");
			   lotFromPid = pid.replaceFirst("\\w{2}-\\w{2}-\\w{2}-\\w{4}-\\w{2}(\\w{3})", "$1").replaceFirst("^0*(\\d+)", "$1");
			   secFromPid = pid.replaceFirst("(\\w{2})-\\w{2}-\\w{2}-\\w{4}-\\w{5}", "$1").replaceFirst("^0*(\\d+)", "$1"); 
			   twnFromPid = pid.replaceFirst("\\w{2}-(\\w{2})-\\w{2}-\\w{4}-\\w{5}", "$1").replaceFirst("^0*(\\d+)", "$1");
			   rngFromPid = pid.replaceFirst("\\w{2}-\\w{2}-(\\w{2})-\\w{4}-\\w{5}", "$1").replaceFirst("^0*(\\d+)", "$1");
			   m.put("PropertyIdentificationSet.SubdivisionCode", pid.replaceFirst("\\w{2}-\\w{2}-\\w{2}-(\\w{4})-\\w{5}", "$1"));
		   }
		   
		   // extract lot from legal description
		   StringBuilder lot = new StringBuilder(); // can have multiple occurrences
		   p = Pattern.compile("\\bLOTS?(( \\d+(?:-?[A-Z])?)+)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   while (ma.find()){
			   lot.append(" ").append(ma.group(1));
		   }
		   
		   boolean blockInPid = true; 
		   if (lot.length() != 0){
			   String lotStr = lot.toString().trim();
//			   // verify if the lot extracted from PID, having the last zero removed, is already present in the lots list extracted 
//			   // from the legal description; if it's not, then add the lot extracted from PID to the list of lots
//			   if (lotFromPid.length() != 0){
//				   String lotTrimmed = lotFromPid.replaceFirst("(\\d+)0$", "$1");
//				   if (!lotStr.matches(".*\\b" + lotTrimmed + "\\b.*"))
//					   if (!lotStr.matches(".*\\b" + blockFromPid + "0?" + lotTrimmed + "\\b.*"))
//						   lotStr = lotStr + " " + lotFromPid;
//					   else
//						   blockInPid = false;  // e.g. PID 21-24-29-2014-01250, lot=125, block=null
//			   }
			   if (lotFromPid.length() != 0){
				   String lotTrimmed = lotFromPid.replaceFirst("(\\d+)0$", "$1");
				   if (!lotStr.matches(".*\\b" + lotTrimmed + "\\b.*"))
					   if (lotStr.matches(".*\\b" + blockFromPid + "0?" + lotTrimmed + "\\b.*"))
						   blockInPid = false;  // e.g. PID 21-24-29-2014-01250, lot=125, block=null
			   }
			   lotStr = LegalDescription.cleanValues(lotStr, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lotStr);
		   } else {
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lotFromPid);
		   }
		   	   	   	   	   	   	 
		   // extract block from legal description 
		   String block = "";
		   p = Pattern.compile("\\bBLK ([A-Z]|\\d+)\\b");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   block = ma.group(1);
//			   if (blockFromPid.length() != 0 && blockInPid){
//				   block = block + " " + blockFromPid;
//			   }
//			   block = LegalDescription.cleanValues(block, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		   } else if (!blockFromPid.equals("0") && blockInPid){
			   m.put("PropertyIdentificationSet.SubdivisionBlock", blockFromPid);
		   }
		   	   
		   // extract sec-twn-rng from legal description
		   // but first add sec-twn-rng extracted from PID
		   List<List> body = new ArrayList<List>();
		   boolean strFromPid = false; 
		   if (!StringUtils.isEmpty(secFromPid) || !StringUtils.isEmpty(twnFromPid) || !StringUtils.isEmpty(rngFromPid)){
			   List<String> line = new ArrayList<String>();
			   line.add(secFromPid);
			   line.add(twnFromPid);
			   line.add(rngFromPid);
			   body.add(line);
			   strFromPid = true;
		   }

		   p = Pattern.compile("\\bSEC (\\d+)-(\\d+)-(\\d+)\\b");
		   ma.usePattern(p);
		   ma.reset();	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();
			   String sec = ma.group(1).replaceFirst("^0*(\\d+)", "$1");
			   String twn = ma.group(2).replaceFirst("^0*(\\d+)", "$1");
			   String rng = ma.group(3).replaceFirst("^0*(\\d+)", "$1");
			   if (!strFromPid || !sec.equals(secFromPid) || !twn.equals(twnFromPid) || !rng.equals(rngFromPid)){
				   line.add(sec);
				   line.add(twn);
				   line.add(rng);
				   body.add(line);
			   }
		   } 
		   if (!body.isEmpty()){
			   String [] header = {"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"};
			   
			   Map<String,String[]> map = new HashMap<String,String[]>();
			   map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
			   map.put("SubdivisionTownship", new String[]{"SubdivisionTownship", ""});
			   map.put("SubdivisionRange", new String[]{"SubdivisionRange", ""});
			   
			   ResultTable pis = new ResultTable();	
			   pis.setHead(header);
			   pis.setBody(body);
			   pis.setMap(map);
			   m.put("PropertyIdentificationSet", pis);
		   }
		   	   	   	  
		   // extract plat B&P and cross references B&P 
		   ResultTable cr = new ResultTable();    
		   List<List> bodyCR = new ArrayList<List>();
	       
		   legal = legal.replaceAll("\\bB&P\\b\\s*", "");
		   p = Pattern.compile("(\\b[A-Z]+\\b)?\\s*\\b(\\d+)/(\\d+)\\b");
		   ma = p.matcher(legal);	   
		   while (ma.find()){
			   if ("CB".equals(ma.group(1))){
				  m.put("PropertyIdentificationSet.CondominiumPlatBook", ma.group(2));
				  m.put("PropertyIdentificationSet.CondominiumPlatPage", ma.group(3));			  
			   } else if ("OR".equals(ma.group(1)) || (((ma.group(1) == null) || (ma.group(1).length() == 0) || !(ma.group(1).equals("CB"))) && (ma.group(2) != null) && Integer.parseInt(ma.group(2)) > 99)){
				   List<String> line = new ArrayList<String>();
				   line.add("O");
				   line.add(ma.group(2));
				   line.add(ma.group(3));
				   bodyCR.add(line);
			   } else {
				   m.put("PropertyIdentificationSet.PlatBook", ma.group(2));
				   m.put("PropertyIdentificationSet.PlatNo", ma.group(3));
			   }
		   }
		   
		   if (!bodyCR.isEmpty()){	   
			   String [] header = {"Book_Page_Type", "Book", "Page"};
		       Map<String,String[]> map = new HashMap<String,String[]>();
		       map.put("Book_Page_Type", new String[]{"Book_Page_Type", ""});
		       map.put("Book", new String[]{"Book", ""});
		       map.put("Page", new String[]{"Page", ""});
		       	
		       cr.setHead(header);
		       cr.setBody(bodyCR);
		       cr.setMap(map);
		       m.put("CrossRefSet", cr);
		   }
	   }
	   
	   public static void parseAddressFLOrangeTR(ResultMap m, String address) throws Exception {
		   	//B 4066 
			String onlyAddress = "";
			String cityAndZip = "";
			int counter = 0;
			String suffix = "";

			//13212 ST COLE CT 32828; 3518 VICTORIA PINES DR 32829; 2659 BALFORN TOWER WAY WINTER GARDEN 34787; 740 W NEW ENGLAND AVE WINTER PARK 32789
			//810 E CHURCH ST ORLANDO 32801; 524 W WINTER PARK ST ORLANDO 32804
		   	StringTokenizer st = new StringTokenizer(address);
			while (st.hasMoreTokens()) {
		    	 String token = st.nextToken();
		    	  if (!Normalize.isSuffix(token)){
		    				  onlyAddress = onlyAddress + " " + token;
		    				  counter++;
		    	  } else { 
		    		  suffix = token;
		    		  counter++;
		    	  }
				  if (!"".equals(suffix)) {
					  onlyAddress = onlyAddress + " " + suffix;
					  suffix = "";
				  }
				  if (counter > 2 && Normalize.isSuffix(token)){ //13212 ST COLE CT 32828
					  String nextToken = st.nextToken();
					  if (Normalize.isSuffix(nextToken)) {
						  onlyAddress = onlyAddress + " " + nextToken;
						  break;
					  } else {
						  break;
					  }
				  }
			}
			
			m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(onlyAddress.trim()));
			m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(onlyAddress.trim()));
			
			cityAndZip = address.replaceAll("(?is)" + onlyAddress.trim() +"(.*)", "$1");
			cityAndZip = cityAndZip.replaceAll("(?is)\\s*UNINCORPORATED(.*)", "$1");
			cityAndZip = cityAndZip.trim();
			
			m.put("PropertyIdentificationSet.City", cityAndZip.replaceAll("(?is)(.*?)(\\d+)", "$1").trim());
			m.put("PropertyIdentificationSet.Zip", cityAndZip.replaceAll("(?is)(.*?)(\\d+)", "$2").trim());
	   }
	   	   
  }
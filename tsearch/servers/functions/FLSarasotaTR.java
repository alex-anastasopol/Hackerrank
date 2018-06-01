package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
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
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLSarasotaTR {
	   @SuppressWarnings("rawtypes")
	public static void taxFLSarasotaTR(ResultMap m, long searchId) throws Exception {
		   
		   String amtPaid = GenericFunctions.sum((String) m.get("tmpAmtPaid"), searchId);
		   m.put("TaxHistorySet.AmountPaid", amtPaid);
		   	   
		   String crtYearDelinq = "0.00";
		   ResultTable delinq = (ResultTable) m.get("tmpDelinqTable");
		   String priorYearDue = (String) m.get("tmpPriorYearsDue");
		   String priorDelinq = "0.00";
		   String certYear = (String) m.get("tmpDelinqCertYear");
		   if (delinq != null && !StringUtils.isEmpty(priorYearDue)){
			   String body[][] = delinq.getBodyRef();
			   
			   if (body.length != 0){
				   String year = (String) m.get("TaxHistorySet.Year");			   
				   int len = body[0].length;
				   boolean foundCrtYear = false;			   		   
				   for (int i=0; i<body.length; i++){
					   if (!foundCrtYear && year != null && len != 0 && body[i][0].contains(year)){
						   if (certYear == null) {
							   foundCrtYear = true;
							   for(int j=len-1; j>0; j--){
								   if (body[i][j].contains("$")){
									   crtYearDelinq = body[i][j].replaceAll("[\\$,]", "");
									   break;
								   }
							   }
							   break;
						   }
					   }
				   }
				   if (foundCrtYear){
					   priorDelinq = (new BigDecimal(priorYearDue).subtract(new BigDecimal(crtYearDelinq))).toString();	
					   String totalDue = (String) m.get("TaxHistorySet.TotalDue"); 
					   if (StringUtils.isEmpty(totalDue)){
						   m.put("TaxHistorySet.TotalDue", crtYearDelinq);
					   } 
				   } else {
					   priorDelinq = priorYearDue;
				   }
			   }
		   }
		   
		   m.put("TaxHistorySet.PriorDelinquent", priorDelinq);	
		   if (certYear != null) {
			   m.put("TaxHistorySet.PriorDelinquent", priorYearDue);	
		   }
		   String totalDue = (String) m.get("TaxHistorySet.TotalDue"); 
		   if (StringUtils.isEmpty(totalDue)){
			   m.put("TaxHistorySet.TotalDue", "0.00");
		   } 
		   
		   String receiptDate = (String) m.get("tmpReceiptDate");
		   String receiptNumber = (String) m.get("tmpReceiptNumber");
		   ResultTable tmpHistory = (ResultTable) m.get("TaxHistorySet");
		   
		   if (tmpHistory == null) {
			   	ResultTable newRT = new ResultTable();
				List<String> line = new ArrayList<String>();
				if (receiptDate != null && receiptNumber != null && amtPaid != null) {
					line.add(amtPaid);
					line.add(receiptNumber);
					line.add(receiptDate);
				}
				if (!line.isEmpty()) {
					List<List> bodyRT = new ArrayList<List>();
					bodyRT.add(line);
					String[] header = {"ReceiptAmount", "ReceiptNumber", "ReceiptDate"};
					Map<String,String[]> map = new HashMap<String,String[]>();
					map.put("ReceiptAmount", new String[]{"ReceiptAmount", ""});
					map.put("ReceiptNumber", new String[]{"ReceiptNumber", ""});
					map.put("ReceiptDate", new String[]{"ReceiptDate", ""});
					newRT.setHead(header);
					newRT.setMap(map);
					newRT.setBody(bodyRT);
					newRT.setReadOnly();
					m.put("TaxHistorySet", newRT);
				}
		   }
		   
		  	   	          
	   }
	   
	   @SuppressWarnings("rawtypes")
	public static void legalFLSarasotaTR(ResultMap m, String legal) throws Exception {
		   
		   //initial corrections and cleanup of legal description	   
		   legal = GenericFunctions.replaceNumbers(legal);
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		   
		   legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		  // legal = legal.replaceAll("\\bESTATES?\\b", "");
		   legal = legal.replaceAll("\\bCO-OP\\b", "");
		   legal = legal.replaceAll("\\b\\d+(ST|ND|RD|TH) ADD( TO)?\\b", "");
		   legal = legal.replaceAll("\\b(THE )?[NWSE]{1,2}(LY)? [\\d\\./\\s]+(\\s*\\bOF)?\\b", "");
		   legal = legal.replaceAll("\\b(\\d+\\s+)?FT(\\s+OF)?\\b", "");
		   legal = legal.replaceAll("\\bRESUB( OF)?\\b", "");
		   legal = legal.replaceAll("\\b(\\d+(ST|ND|RD|TH) )?REPLAT( OF)?\\b", "");
		   legal = legal.replaceAll("\\b(REVISED|AMENDED) PLAT( OF)?\\b", "");
		   legal = legal.replaceAll(",\\s*SUBJ TO [^,]+,?", "");
		   legal = legal.replace(",", "");
		   legal = legal.replaceAll("\\s{2,}", " ").trim();

		   String legalTemp = legal;
		   
		   // extract lot from legal description
		   String lot = ""; // can have multiple occurrences
		   Pattern p = Pattern.compile("\\b(LOT)S? ([\\d\\s&-]+|\\d+[A-Z])\\b");
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
		   p = Pattern.compile("\\b(BLK)S? ((?:\\b[A-Z]\\b|\\d+|&|\\s)+)\\b");
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
		   p = Pattern.compile("\\b(UNIT) ((?:[A-Z]-?)?\\d+[A-Z]?(?:-\\d+)?)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			   m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(2));
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   
		   // extract building #
		   p = Pattern.compile("\\b(BLDG) ([A-Z]|\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2));
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   	   
		   // extract phase from legal description
		   p = Pattern.compile("\\b(PH)(?:ASES?)? ([\\d\\s&-]+|\\d+[A-Z]?)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			   m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(2).replaceAll("\\s*&\\s*", " "));
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   	   
		   //extract Section, Township, Range
		   List<List> body = new ArrayList<List>();
		   p = Pattern.compile("\\s+(\\b(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)\\b)");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();
			   String sec = ma.group(2).replaceAll("\\s*,\\s*", " ");
			   String twn = ma.group(3);
			   String rng = ma.group(4);
			   line.add(sec);
			   line.add(twn);
			   line.add(rng);
			   body.add(line);
			   legalTemp = legalTemp.replace(ma.group(1), "");		   
		   }
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		   
		   // extract section from legal description
		   p = Pattern.compile("\\b(SEC) (\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   List<String> line = new ArrayList<String>();
			   line.add(ma.group(2));
			   line.add("");
			   line.add("");
			   body.add(line);
			   legalTemp = legalTemp.replace(ma.group(1), "");
		   }
		   GenericFunctions2.saveSTRInMap(m, body);
		   
		   // extract cross refs from legal description
		   List<List> bodyCR = new ArrayList<List>();
		   p = Pattern.compile("\\b(OR) (\\d+)/(\\d+)\\b");
		   ma = p.matcher(legal);	      	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();		   
			   line.add(ma.group(2));
			   line.add(ma.group(3));
			   line.add("");
			   bodyCR.add(line);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));		   
		   } 
		   p = Pattern.compile("\\b(ORI) ([A-Z\\d]+)\\b");
		   ma.usePattern(p);
		   ma.reset();	      	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();		   
			   line.add("");
			   line.add("");
			   line.add(ma.group(2));
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
		   	  	
		   String subdiv = "";
		   p = Pattern.compile(".*\\b(?:LOT|BLK|UNIT|PH|BLDG) (.+?) (?:UNIT|PH|SEC(?! OF\\b)|ORI?)\\b.*");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   subdiv = ma.group(1);		   
		   } else {
			   p = Pattern.compile(".*\\b(?:LOT|BLK|UNIT|PH|BLDG) (.+)");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   subdiv = ma.group(1);
			   }   
		   }		   
		   if (subdiv.length() != 0){
			   if (subdiv.matches(".*\\b[SEWN]-\\d+-\\d+-\\d+.*")){
				   subdiv = "";		// 0072-14-0027 for intermediate results parser
			   }
			   subdiv = subdiv.replaceFirst("^\\s*OF\\b\\s*", "");
			   subdiv = subdiv.replaceFirst("(.+) CONDO(MINIUM)?.*", "$1");
			   subdiv = subdiv.replaceFirst("(.+) SUB(DIVISION)?.*", "$1");
			   subdiv = subdiv.replaceFirst("^\\d+\\s", "");
			   subdiv = subdiv.replaceFirst("(.+)\\s*LESS\\s*COM.*", "$1");
			   subdiv = subdiv.replaceFirst("\\bLESS\\s+(BOTH\\s+)?LOTS?(\\s*BEING\\s+A\\s+PARCEL)?\\s+\\d*(\\s*BY\\s*\\d*)?", "");
			   //subdiv = subdiv.replaceFirst("\\s\\d+$", "");
			   subdiv = subdiv.trim();
			   if (subdiv.length() != 0){
				   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				   if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
					   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }
		   }	
		   
		   
			
			
	   }
	   
	   protected static String cleanOwnerFLSarasotaTR(String s){
		   s = s.toUpperCase();
	       s = s.replaceFirst("\\bH/W\\b", "");			 
	       //s = s.replaceAll("\\bEST( OF)?\\b", ""); 			//WILSON JEANNE E EST OF % DAVID P WILSON
	       s = s.replaceAll("\\bCO-TTEE\\b", " TRUSTEE");
	       s = s.replaceAll("INTE?R?", "");
	       s = s.replaceAll("\\d+%", "");
	       s = s.replaceAll("\\d+/\\d+", "");
	       s = s.replaceAll("\\bT(TEES?)\\b", " TRUS$1");
	       s = s.replaceAll("\\bCUSTODIAN\\b", "");
	       s = s.replace("%", "&");       
	       s = s.replaceAll("\\([^\\)]+\\)", "");				//WILSON JR (TOD) GEORGE R WILSON (TOD) LILIANE 
	       s = s.replaceAll("\\bC/O\\b", "&").trim();
	       return s;
	   }
	   	   
	   @SuppressWarnings("rawtypes")
	public static void partyNamesFLSarasotaTR(ResultMap m, String owner) throws Exception {
		String ownerCopy = owner;
		owner = cleanOwnerFLSarasotaTR(owner);
		owner = owner.replaceAll("\\b(\\w+)\\s+([A-Z]+(?:\\s+[A-Z])?)\\s(\\1)\\b", "$1 $2@@$3");
		owner = owner.replaceAll("@@", "\n");
		owner = owner.replaceAll("\\n&\\s+(\\w+\\s*\\w+)$", " & $1");

		owner = owner.replaceAll("[A-Z]-(\\d)", "$1");
		owner = owner.replaceAll("(?is)(.*)\\n(R[A-Z] )(?:\\d+)?\\s*(BOX.*)", "$1");
		owner = owner.replaceAll("(?is)(.+)\\s*(P\\s*O\\s+BOX)(.+)", "$1");
		owner = owner.replaceAll("(?is)(.+)\\s*(PMB|.+)", "$1");
		owner = owner.replaceAll("(?is)(.+)\\s*(BOX.+)", "$1");
		owner = owner.replaceAll("(?is)([^0-9]*).*", "$1");
		owner = owner.replaceAll("(JO) (ANN)", "$1#$2");
		owner = owner.replaceAll("\\bPOA\\b", "");
		owner = owner
				.replaceAll(
						"(BROWN MAMIE L LIFE EST\n)(ALLEN C)(E)(\nALLEN CURTIS) (MORRISON)\n(P)(D) (WILLIAMS D)(L) & (MOULTRY J)(S)\n",
						"$1$2 $3$4\n$5 $6 $7\n$8 $9\n$10 $11"); // 2024-13-0085
		owner = owner
				.replaceAll(
						"(SMITH LILLIAN C) (BOOTHBY L M\n)(SILVER L)(J) (SMITH L)(E) & (BREGEL S)(P)",
						"$1\n$2$3 $4\n$5 $6\n$7 $8"); // 0957-11-7718
		owner = owner.replaceAll("(KELLY L) (SMITH LORI)", "$1\n$2"); // 0495-03-0023
		owner = owner.replaceAll("(\\n *)+", "\n");
		String[] owners = owner.split("\n");
		for (int j = 0; j < owners.length; j++) {
			owners[j] = owners[j].replaceAll("\\s{2,}", " ");
		}
		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] lines = { "", "", "", "", "" };
		String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };

		try {
			if ((ownerCopy.contains("ENGLAND")) || (ownerCopy.contains("UK"))) {// for this kind of examples 0129-16-4005
				for (int j = 0; j < owners.length; j++) {
					if (!LastNameUtils.isNoNameOwner(owners[j])) {
						lines[j] = owners[j];
					} else
						break;
				}
				for (int k = 0; k < owners.length; k++) {
					owners[k] = "";
				}
				for (int i = 0; i < lines.length; i++) {
					if (!"".equals(lines[i])) {
						owners[i] = lines[i];
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];
			ow = ow.replaceAll("\\A(\\w+)\\s+([IJSRVX]{1,4})\\s+(.+)", "$1 $3 $2");
			Matcher ma = GenericFunctions.nameType.matcher(ow);
			if (ma.matches()){//put TRUSTEE at the end of the name
				ow = ow.replaceAll("\\b" + ma.group(2) + "\\b", " ").replaceAll("\\s{2,}", " ").trim() + " " + ma.group(2);
			}
			ma.reset();
			ma = GenericFunctions.nameOtherType.matcher(ow);
			if (ma.matches()){///put ETAL at the end of the name
				ow = ow.replaceAll("\\b" + ma.group(2) + "\\b", " ").replaceAll("\\s{2,}", " ").trim() + " " + ma.group(2);
			}
			ow = ow.replaceAll("\\A& (.+)", "$1");
			if (ow.endsWith("III   ")) {
				ow = ow.replace("III", "");
				names = StringFormats.parseNameNashville(ow, true);
				names[1] = (names[1] + " III");
			} else {
				names = StringFormats.parseNameNashville(ow, true);
			}

			if (names[2].contains("JR")) { // 1133-10-7219 SMITH CATHERINE G\nGRILL JR C JAMES where here GRILL is not a company name
				names[2] = names[2].replaceAll("(?is)([A-Z])([A-Z]+\\s+(.+))", "$1@$2");
				names = StringFormats.parseNameNashville(names[2], true);
				names[2] = names[2].replaceAll("@", "");
			}
			if (ow.contains("C/O") || ow.startsWith("ATTN")) {
				ow = ow.replaceAll("C/O\\s*", "");
				ow = ow.replaceFirst("(?is)^ATTN:?\\s*", "");
				if (NameUtils.isCompany(ow)) {
					names[2] = ow;
				} else {
					names = StringFormats.parseNameDesotoRO(ow, true);
				}
			}

			names[0] = names[0].replaceAll("#", " ");
			names[1] = names[1].replaceAll("#", " ");
			suffixes = GenericFunctions2.extractAllNamesSufixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			
			GenericFunctions2.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
		}

		GenericFunctions2.storeOwnerInPartyNames(m, body, true);
	}
	   
	@SuppressWarnings("rawtypes")
	public static void stdFLSarasotaTR(ResultMap m, String s) throws Exception {
		// cleanup
		s = cleanOwnerFLSarasotaTR(s);

		// if more than an owner
		if (!s.matches("[A-Z]{2,}(-[A-Z]{2,})?( (JR|SR|II|III))? [A-Z]+( [A-Z]+)?( [A-Z])?")) {
			// if owner and spouse name are not separated with &, then separate
			// the names
			if (!s.contains("&")) {
				s = s.replaceFirst("^(([A-Z]{2,})\\b.*)\\b([A-Z]{2,}\\-\\2)\\b", "$1& $3"); // SMITH JOHNNY W LLOVIO-SMITH ROSA
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("^(([A-Z]{2,})\\b.*)\\b(\\2)\\b", "$1& $3"); // SMITH JOHN A SMITH MARY ANNE
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("([A-Z]+(?: (?:JR|SR|II|III))? [A-Z]+(?: [A-Z])?(?: [A-Z])?) ([A-Z]{2,}(?: (?:JR|SR|II|III))? [A-Z]+.*)",
						"$1 & $2");// SMITH JOHN E JORDAN JUDITH M
			}
		}

		s = s.replaceFirst("(?is)(\\bEST)\\s+&", "$1 and ");
		String[] a = StringFormats.parseNameNashville(s, true);
		// remove the 3 owner, if present (parsed in the 2nd owner middle name)
		// - e.g. 0495-03-0023 - WILSON JACQUELINE J DAVIS KELLY L SMITH LORI K
		//a[4] = a[4].replaceFirst("^([A-Z]) [A-Z]{2,} [A-Z]{2,}( [A-Z])?.+",	"$1");

		String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
		List<List> body = new ArrayList<List>();
		type = GenericFunctions.extractAllNamesType(a);
		otherType = GenericFunctions.extractAllNamesOtherType(a);
		suffixes = GenericFunctions.extractAllNamesSufixes(a);

		GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], type,
				otherType, NameUtils.isCompany(a[2]),
				NameUtils.isCompany(a[5]), body);

		GenericFunctions.storeOwnerInPartyNames(m, body, true);

		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	}
}

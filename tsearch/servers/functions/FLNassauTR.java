package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;


public class FLNassauTR {
	
	public static final String[] CITIES = {"Amelia City", "Amelia Island MH Park", "American Beach", "Andrews", "Becker", "Boulogne", 
											"Braddocks Trailer Park", "Bryceville", "Callahan", "Chester", "Crandall", "Crawford", "Dahoma", "Dyal", 
											"Evergreen", "Fernandina Beac", "Fernandina Beach", "Franklintown", "Glenwood", "Goodbread MH Park", 
											"Gross", "Hedges", "Hero", "Hilliard", "Ingle", "Italia", "Keene", "Kent", "Kings Ferry", "Lessie", 
											"Mattox", "Nassau Acres MH Park", "Nassauville", "Nassau Village", "Old Amelia MH Park", "Old Fernandina", 
											"O'Neil", "Ratliff", "Ruby's MH Park", "Sandpiper MH Park", "Verdie", "Yulee", "Yulee Heights"};
	
	
	public static String cleanOwnerFLNassauTR(String s){
		s = s.toUpperCase();
		s = s.replaceFirst("\\b\\s*\\(H&W\\)", "");
		s = s.replaceFirst("\\b\\s*\\(F/D\\)", "");
		s = s.replaceFirst("C/O", "&");
		s = s.replace("%", "&");
		s = s.replaceAll("(\\S)&(\\S)", "$1 & $2");
		s = s.replaceAll(" OR ", " & ");
		s = s.replaceAll("&\\s*&", " & ");
		s = s.replaceAll("\\bMRS\\b", "");
		s = s.replaceAll("(?is)\\b(ET)\\s*(AL|UX|VIR)\\b", "$1$2");
		s = s.replaceAll("JTROS", "");
		s = s.replaceAll("\\s{2,}", " ").trim();
		return s;
	}
	
	public static void parseAddressFLNassauTR(ResultMap m,long searchId) throws Exception {
	     
		String address = (String) m.get("tmpAddress");
		
		if (StringUtils.isEmpty(address))
			return;
		
		if (CITIES != null) {
			for (int i = 0; i < CITIES.length; i++){
				if (address.toLowerCase().contains(CITIES[i].toLowerCase())){
					if ("fernandina beac".equals(CITIES[i].toLowerCase())){
						m.put(PropertyIdentificationSetKey.CITY.getKeyName(), "Fernandina Beach");
					} else {
						m.put(PropertyIdentificationSetKey.CITY.getKeyName(), CITIES[i]);
					}
					address = address.toLowerCase().replaceFirst(CITIES[i].toLowerCase() + "\\s*$", "");
					break;
				}
			}
		}
		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
		   
		   
	}
	
	@SuppressWarnings("rawtypes")
	public static void stdFLNassauTR(ResultMap m,long searchId) throws Exception {

		String s = (String) m.get("tmpOwnerName");

		if (s == null || s.length() == 0)
			return;

		s = cleanOwnerFLNassauTR(s);

		if (!s.matches("[A-Z]{2,}(-[A-Z]{2,})?( (JR|SR|II|III))? [A-Z]+( [A-Z]+)?( [A-Z])?")) {

			if (!s.contains("&")) {
				s = s.replaceFirst("^(([A-Z]{2,})\\b.*)\\b([A-Z]{2,}\\-\\2)\\b", "$1& $3");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("^(([A-Z]{2,})\\b.*)\\b(\\2)\\b", "$1& $3");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("\b([A-Z]) ([A-Z]) ([A-Z]+)", "$3" + "$1" + "$2");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("([A-Z]+(?: (?:JR|SR|II|III))? [A-Z]+(?: [A-Z])?(?: [A-Z])?) ([A-Z]{2,}(?: (?:JR|SR|II|III))? [A-Z]+.*)",
						"$1 & $2");
			}
		}

		String[] a = StringFormats.parseNameNashville(s, true);

		a[4] = a[4].replaceFirst("^([A-Z]) [A-Z]{2,} [A-Z]{2,}( [A-Z])?.+", "$1");

		List<List> body = new ArrayList<List>();
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		types = GenericFunctions.extractAllNamesType(a);
		otherTypes = GenericFunctions.extractAllNamesOtherType(a);
		suffixes = GenericFunctions.extractAllNamesSufixes(a);

		GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], types,
				otherTypes, NameUtils.isCompany(a[2]),
				NameUtils.isCompany(a[5]), body);

		GenericFunctions.storeOwnerInPartyNames(m, body, true);

	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNamesFLNassauTR(ResultMap m,long searchId) throws Exception {
	     
		String owner = (String) m.get("tmpOwnerAddress");
		
		owner = owner.replaceFirst("(?is)\\bMailing\\s+Address\\b", "").trim();
		owner = owner.replaceAll("\n{2,}", "\n");
		
		if (StringUtils.isEmpty(owner))
			return;

		String s = owner;
		owner = owner.replaceAll("(?is)(.+)\n(.+)", "$1");
		owner = owner.replaceAll("\\(JT/ROS\\)", "&");
		owner = owner.replaceAll("(.+)\\s+(PINE GROVE.*)", "$1"); // 000000-00004298-4191
		owner = owner.replaceAll("(.+)\\s+(RT.*)", "$1");
		owner = owner.replaceAll("(.+)\\s+(R\\s*R.*)", "$1");
		owner = owner.replaceAll("(.+)\\s+(P\\s*O.*)", "$1");
		owner = owner.replaceAll("(?is)(.*)\\s+(\\d+)([A-Z])?\\s+([A-Z]+).*", "$1");
		owner = owner.replaceAll("(NANCY)\\(21%\\)", "$1 & ");
		owner = owner.replaceAll("\\([^\\)]+\\)", "");
		owner = owner.replaceAll("(ANGELA J)\\s+(BROYKINS MELVIN)", "$1 & $2");
		owner = owner.replaceAll("(LONA A &) (SUE A) (PARKINSON)", "$1 $3 $2");
		owner = owner.replaceAll("(E D) (MCCULLOUGH) & (CHARLES) (PRATT)", "$2 $1 & $4 $3");
		owner = owner.replaceAll("(EST) (BRANCH)", "$1 & $2");
		owner = owner.replaceAll("OF 2000", "&");
		owner = owner.replaceAll("\\(JT/RS", "");
		owner = owner.replaceAll("\\(JTRO", "");
		owner = owner.replaceAll("L/E", "&");
		owner = owner.replaceAll("(?im)living trust\\s*.*", "");
		// owner =
		// ((String)m.get("tmpOwnerAddress")).replaceAll("(?m)living trust.*",
		// "")
		owner = cleanOwnerFLNassauTR(owner);

		String[] owners;
		if (owner.matches("\\w+\\s+\\w+\\s+(\\w+\\s+)?&\\s+\\w+\\s*\\w?")) {
			owners = owner.split("\n");
		} else {
			owners = owner.split("&");
		}

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		String ln = "";

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];

			names = StringFormats.parseNameNashville(ow, true);
			if ((s.contains("C/O") && i == owners.length - 1)
					|| (((s.contains("L/E")) || (s.contains("(JT/ROS)"))) && !LastNameUtils.isLastName(names[2]))
					|| (s.contains("M&D") && i == owners.length - 1)) {
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
				if ((names[2].length() == 1 && names[1].length() == 0)
						|| (names[2].length() == 0 && names[1].length() == 0)) {
					names[1] = names[2];
					// names[0] = names[2];
					names[2] = ln;
				}
				if (names[2].length() != 0 && names[1].length() == 0
						&& names[0].length() == 0) {
					names[0] = names[2];
					names[2] = ln;
				}

			}
			ln = names[2];
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
					type, otherType, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
	}
	
	@SuppressWarnings("rawtypes")
	public static void legalFLNassauTR(ResultMap m, long searchId) throws Exception {		   
		   String legal = (String)m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		   if (StringUtils.isEmpty(legal))
			   return;
		   
		   //initial corrections and cleanup of legal description	   
		   legal = GenericFunctions.replaceNumbers(legal);
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		   
		   legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		   legal = legal.replaceAll("\\bCO-OP\\b", "");
		   legal = legal.replaceAll("\\b\\d+(ST|ND|RD|TH) ADD( TO)?\\b", "");
		   legal = legal.replaceAll("\\bSUB( OF)?\\b", "");
		   legal = legal.replaceAll("\\b(\\d+(ST|ND|RD|TH) )?REPLAT( OF)?\\b", "");

		   legal = legal.replaceAll("\\b [SWNE]+[\\d/\\s]+(\\s*\\bOF)?\\b", " ");
		   legal = legal.replaceFirst("\\bPT\\b", "");
		   
		   //legal = legal.replaceAll("\\b[A-Z][\\d\\s&]+\\b", " ");
		                    /* --- */
		  // legal = legal.replaceAll("([A-Z]+')", "$1"+"S");
		                    /* --- */
		   //legal = legal.replaceAll("\\b(TRACT)\\s*([\\d]+)\\b", "$1");
		   //legal = legal.replaceAll("\\bMH/ID\\s*[\\d]+\\b", "");
		   legal = legal.replaceAll("\\b(OF)(\\sLOT(S)?)\\b", "$2");
		   legal = legal.replaceAll("\\b([A-Z]+[/][A-Z]+[\\s\\d]*)\\b", " ");
		   //legal = legal.replaceAll("\\b(DB)\\s*([&\\s\\d/]*)\\b", " ");
		   legal = legal.replaceAll("\\b(PB)\\s*(0[/])([\\d]+[/])([\\d-]+)\\b", "$1" + "$3" + "$4");
		   legal = legal.replaceAll("\\b(?is)(IN)\\s*([OR\\s&0-9/-]*)\\b", "$2"); 
		   legal = legal.replaceAll("\\b([PT|FT]\\s*[OF]+[\\s\\d/]*)\\b", " ");
		   legal = legal.replaceAll("\\b(?is)(\\(*(EX)\\s*[a-z&\\s0-9-/]*\\)*)(OR)\\b", "$2" + " " + "$3");
		   legal = legal.replaceAll("(IN)(\\s+OR)", "$2");
		   legal = legal.replace(",", "");
		   legal = legal.replaceAll("\\s{2,}", " ").trim();

		   String legalTemp = legal;
		   
		   // extract lot from legal description
		   String lot = ""; // can have multiple occurrences
		   Pattern p = Pattern.compile("\\b(LOT(?:S)?)\\s*((?:\\d+|[A-Z]|[A-Z]-?\\d+)\\b(?:\\s*&?\\s*(?:\\d+|\\b[A-Z]|\\b[-A-Z]-?\\d+)\\b)*)");
		   Matcher ma = p.matcher(legal);
		   while (ma.find()){
			   lot = lot + " " + ma.group(2);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1)+" ");
		   }
		   lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		   if (lot.length() != 0){
			   lot = LegalDescription.cleanValues(lot, false, true);
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		   }
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		   	   	   	   
		   // extract block from legal description
		    
		   String block = "";
		   p = Pattern.compile("\\b(BLK|BLOCK)S? ((?:\\b[A-Z]\\b|\\d+|&|\\s)+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   block = block + " " + ma.group(2);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1)+" ");
		   }
		   block = block.replaceAll("\\s*&\\s*", " ").trim();
		   if (block.length() != 0){
			   block = LegalDescription.cleanValues(block, false, true);
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		   }	   
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		   	   
		   // extract unit from legal description
		   String unit = "";
		   p = Pattern.compile("\\b(UNIT) ((?:[A-Z]-?)?\\d+[A-Z]?(?:-\\d+)?)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   unit = unit +" " + ma.group(2);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);//ma.group(2));
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   
		   // extract building #
		   p = Pattern.compile("\\b(BLDG) ([A-Z]|\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), ma.group(2));
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   	   
		   // extract phase from legal description
		   String phase = "";
		   p = Pattern.compile("\\b(PHASE) (\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   phase = phase + " " + ma.group(2);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase); 
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   	   	 
		   // extract section from legal description
		   p = Pattern.compile("\\b(SEC) (\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), ma.group(2));
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   
		   String tract = "";
		   p = Pattern.compile("\\b(TR)A?C?T?\\s*([\\dA-Z]+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   tract = tract + " " + ma.group(2);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), tract);
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   String pb = "";
		   String pn = "";
		   p = Pattern.compile("\\b(PB)\\s*([\\d]+)[/]([\\d-]+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   pb = pb + " " + ma.group(2);
			   pn = pn + " " + ma.group(3);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			   pb = org.apache.commons.lang.StringUtils.strip(pb.trim(), "0");
			   pn = org.apache.commons.lang.StringUtils.strip(pn.trim(), "0");
			   if (!pb.isEmpty() && !pn.isEmpty()) {
					m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), pb);
					m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), pn);
			   }
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   	   
		   // extract cross refs from legal description
		   List<List> bodyCR = new ArrayList<List>();
		   p = Pattern.compile("\\b(OR)\\s*(\\d+)/(\\d+)\\b");
		   ma = p.matcher(legal);	      	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();		   
			   line.add(ma.group(2));
			   line.add(ma.group(3));
			   line.add("");
			   bodyCR.add(line);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));		   
		   } 
		   p = Pattern.compile("\\b(OR)\\s*(\\d+)\\s*(PG)\\s*(\\d+)\\b");
		   ma.usePattern(p);
		   ma.reset();	      	   
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
		   	  	
		   String subdiv = "";
		   p = Pattern.compile(".*\\b(?:LOT|BLK|BLOCK|UNIT|PHASE|BLDG|\\)|TRACT|OR) (.+) (EST|PB)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   subdiv = ma.group(1);					   
		   } 
		   
		   // aici am schimbat eu ca sa functioneze corect parsarea cf BUG 2903
		   else
		   {
			   p = Pattern.compile(".*\\b(?:LOT|BLK|BLOCK|UNIT|PHASE|BLDG|OR)(.*)");
		       ma = p.matcher(legal);
		       if (ma.find()) {
		    	   subdiv = ma.group(1);
		       } else {
				   p = Pattern.compile(".*\\b(?:LOT|BLK|BLOCK|UNIT|PHASE|BLDG|OR) (.+?) (?:UNIT|PB|PHASE|#|-|PER|SEC(?! OF\\b)|ORI?)\\b.*");
				   ma.usePattern(p);
				   ma.reset();
				   if (ma.find()){
					   subdiv = ma.group(1);
				   }   
			   }
		   }
		   if (subdiv.equalsIgnoreCase("sec")) {
			   subdiv = "";
		   }
		   if (subdiv.length() != 0){
			 
			   subdiv = subdiv.replaceFirst("(.+) CONDO(MINIUM)?.*", "$1");
			   subdiv = subdiv.replaceFirst("(.+) SUB(DIVISION)?.*", "$1");
			   subdiv = subdiv.replaceFirst("^\\d+\\s", "");
			   subdiv = subdiv.replaceFirst("(PG)(.*)", "$2");
			   subdiv = subdiv.replaceFirst("(.*)(PB)", "$1");
			   subdiv = subdiv.replaceFirst("(?is)([^#]+)([#\\d]+)", "$1");
			   subdiv = subdiv.replaceFirst("\\b(.*)(PER.*)\\b", "$1");
			   subdiv = subdiv.replaceFirst("\\b(.*)(CASE.*)\\b", "$1");
			   //subdiv = subdiv.replaceFirst("\\b(.*)(SURVEY\\s)\\b", "$1");   
			   subdiv = subdiv.replaceFirst("\\b(.*)(OR)\\b", "$1");
			   subdiv = subdiv.replaceAll("[\\(\\)]", "");
			   subdiv = subdiv.replaceAll("@\\s*", "");
			   subdiv = subdiv.replaceAll("\\bPARCELS?\\s+\\d[\\d&.\\s-]+", "");
			   
			   m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdiv);
			   if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				   m.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subdiv);
		   }	 
	   }
		
		public static void taxFLNassauTR(ResultMap m, long searchId) throws Exception {
			   
			String amtPaid = GenericFunctions.sum((String) m.get("tmpAmtPaid"), searchId);
			m.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amtPaid);
			   	   
			String crtYearDelinq = "0.00";
			ResultTable delinq = (ResultTable) m.get("tmpDelinqTable");
			String priorYearDue = (String) m.get("tmpPriorYearsDue");
			String priorDelinq = "0.00";
			String totalDelinq = "0.00";
			boolean totalDelinqSet = false;
			if (delinq != null && !StringUtils.isEmpty(priorYearDue)){
				String body[][] = delinq.getBodyRef();
				if (body.length != 0){
					String year = (String) m.get(TaxHistorySetKey.YEAR.getKeyName());			   
					int len = body[0].length;
					boolean foundCrtYear = false;			   		   
					for (int i=0; i<body.length; i++){
						if (!foundCrtYear && year != null && len != 0 && body[i][0].contains(year)){
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
					if (foundCrtYear){
						priorDelinq = (new BigDecimal(priorYearDue).subtract(new BigDecimal(crtYearDelinq))).toString();				   
						totalDelinqSet = true;
						String totalDue = (String) m.get(TaxHistorySetKey.TOTAL_DUE.getKeyName()); 
						if (StringUtils.isEmpty(totalDue)){
							m.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), crtYearDelinq);
						}
					} else {
						priorDelinq = priorYearDue;
					}
					totalDelinq = priorYearDue;
				}
			}
			   
			if (!totalDelinqSet){
				String amtDue = (String) m.get(TaxHistorySetKey.TOTAL_DUE.getKeyName());
				if (!StringUtils.isEmpty(amtDue)){
					Date dateNow = new Date();	   	   	   
					Date dueDate = DBManager.getDueDateForCountyAndCommunity(
							InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity()
							.getID().longValue(), InstanceManager.getManager()
							.getCurrentInstance(searchId).getCurrentCounty().getCountyId()
							.longValue());
				       			   
					if (dateNow.after(dueDate)){
						totalDelinq = (new BigDecimal(totalDelinq).add(new BigDecimal(amtDue))).toString();
					}
				}
			}
		       	   	   
			m.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinq);    
		} 
			  
}

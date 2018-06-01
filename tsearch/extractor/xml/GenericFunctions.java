package ro.cst.tsearch.extractor.xml;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.ParseRule;
import ro.cst.tsearch.extractor.ParseRules;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.search.address.Normalize;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.functions.CODenverTR;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.test.Testutils;

/* This file should contain the GenericFunctions methods related only to California counties. 
 * The methods for new states should be included in different GenericFunctions files.
 * The methods for Florida are stored in GenericFunctions2.
 * The methods for Tennessee, Missouri, Kansas, Kentucky, Ohio, Illinois, Michigan are stored in GenericFunctions1.
*/ 

public class GenericFunctions extends GenericFunctions2{
		
	public static void taxesDASLDT(ResultMap m, long searchId) throws Exception {
		// compute the tax parameters only if the current processed document is a tax statement 
		String year = (String) m.get("TaxHistorySet.Year");
		if (year != null){ 	    		    	
	    	String totalDue = (String) m.get("tmpTotalDue");
	    	String totalDueSum = "0.00";
	    	if (!"SEE REDEMPTION".equals(totalDue)){
	    		totalDueSum = sum(totalDue, searchId);
	    	} else {
	    		ResultTable redemption = (ResultTable) m.get("TaxRedemptionSet");
	    		if (redemption != null){
		    		String[][] body = redemption.body;
		    		if (body != null && body.length != 0){
		    			totalDueSum = body[0][2];
		    		}
	    		}
	    	}
	    	m.put("TaxHistorySet.TotalDue", totalDueSum);
	    	m.put("TaxHistorySet.CurrentYearDue", totalDueSum);
	    	
	    	ResultTable rt = (ResultTable) m.get("tmpTaxInstallments");
	    	if (rt != null){
	    		String[][] body = rt.body;
	    		if (body != null && body.length != 0){
	    			BigDecimal amtPaid = new BigDecimal(0.00);
	    			BigDecimal baseAmt = new BigDecimal(0.00);
	    			List<List> bodyInstallments = new ArrayList<List>(); 
	    	        for (int i=0; i<body.length; i++){
	    	        	if (body[i].length == 6){
	    	        		String status = body[i][3];
	    	        		String yearDesc = body[i][4].replaceFirst("(\\d{4})-\\d+", "$1");
	    	        		if (!status.matches(".*CANCEL.*")){
		    	        		String ba = body[i][0].replaceAll(",", "");
		    	        		String penalty = body[i][1].replaceAll(",", "");
		    	        		BigDecimal paid = new BigDecimal(0.00);
		    	        		if (yearDesc.contains(year)) {
		    	        			baseAmt = baseAmt.add(new BigDecimal(ba));
			    	        		if (status.equals("PAID") || status.equals("PAID_PREVIOUS")){
			    	        			paid = paid.add(new BigDecimal(ba));
			    	        		} else if (status.equals("PAID_W_PENALTY")){
			    	        			paid = paid.add(new BigDecimal(ba)).add(new BigDecimal(penalty));		//add also the penalties
			    	        		}
		    	        		}
		    	        		amtPaid = amtPaid.add(paid);
		    	        			
		    	        		List<String> line = new ArrayList<String>();
		    	        		line.add(ba.replaceFirst("^\\.", "0."));
		    	        		line.add(paid.toString().replaceFirst("^0$", "0.00"));
		    	        		line.add(body[i][2].replaceAll(",", "").replaceFirst("^\\.", "0."));
		    	        		line.add(penalty.replaceFirst("^\\.", "0."));
		    	        		line.add(status);
		    	        		line.add(body[i][4]);
		    	        		line.add(body[i][5]);
		    	        		bodyInstallments.add(line);
	    	        		}
	    	        	} 
	    	        }    	        
	    	        m.put("TaxHistorySet.AmountPaid", amtPaid.toString().replaceFirst("^0$", "0.00"));
	    	        m.put("TaxHistorySet.BaseAmount", baseAmt.toString().replaceFirst("^0$", "0.00"));
	    	         
	    	        if (!bodyInstallments.isEmpty()){
		    	        String [] header = {"BaseAmount", "AmountPaid", "TotalDue", "PenaltyAmount", "Status", "TaxYearDescription", "TaxBillType"};				   
						Map<String,String[]> map = new HashMap<String,String[]>();
						map.put("BaseAmount", new String[]{"BaseAmount", ""});
						map.put("AmountPaid", new String[]{"AmountPaid", ""});
						map.put("TotalDue", new String[]{"TotalDue", ""});
						map.put("PenaltyAmount", new String[]{"PenaltyAmount", ""});
						map.put("Status", new String[]{"Status", ""});
						map.put("TaxYearDescription", new String[]{"TaxYearDescription", ""});
						map.put("TaxBillType", new String[]{"TaxBillType", ""});
						
						ResultTable installments = new ResultTable();	
						installments.setHead(header);
						installments.setBody(bodyInstallments);
						installments.setMap(map);
						m.put("TaxInstallmentSet", installments);
	    	        }
	    		}
	    	}
	    	
	    	String priorYearsDelinq = (String) m.get("tmpPriorYearsDelinquentDue");
	    	m.put("TaxHistorySet.PriorDelinquent", sum(priorYearsDelinq, searchId));
		}
	}
	
	public static void legalTaxDASLDT(ResultMap m, long searchId) throws Exception {
	   
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
	   
		if ("CA".equals(crtState)){
			if ("Alameda".equals(crtCounty)){
				legalCAAlamedaTaxDT(m, searchId);
			}else if ("Orange".equals(crtCounty)){
				legalCAOrangeTaxDT(m, searchId);
			}else if ("Los Angeles".equals(crtCounty)){
				legalCALosAngelesTaxDT(m, searchId);				
			} else {
				legalCAContraCostaTaxDT(m, searchId);
			}
		}
	}
	
	public static void legalCALosAngelesTaxDT(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;
	   
		legalTokenizerCALosAngelesTaxDT(m, legal); 
	}	
	
    public static void SocialSecurityNumberDT(ResultMap m, long searchId) throws Exception {
    	String comment = (String) m.get("tmpComment");
    	if (StringUtils.isEmpty(comment))
    		return;
    	
    	String ssn4 = "";
    	Pattern p = Pattern.compile("\\b(?:SS)?XXX-?XX-?(\\d{4})\\b");
		Matcher ma = p.matcher(comment);
    	while (ma.find()) {
    		ssn4 = ssn4 + " " + ma.group(1);
    	}
		if (ssn4.length() != 0) {
    		m.put("PropertyIdentificationSet.SSN4", ssn4.trim()); 
    	} 
    }

	public static void legalTokenizerCALosAngelesTaxDT(ResultMap m, String legal) throws Exception{
	   				
		// initial cleanup and corrections
		legal = legal.replaceAll("\\bNO(?= \\d)", "");
		legal = legal.replaceAll("\\s*\\bAND$", "");
		legal = legal.replaceAll("[#\\.]", "");
		legal = legal.replaceAll("[=\\*]", " ");
		legal = legal.replaceAll("\\bCOLO NY\\b", "COLONY");	
		legal = legal.replaceAll("(?<=\\d )F T\\b", "FT");
		legal = legal.replaceAll("\\b(FT OF)([SWEN]{1,2})\\b", "$1 $2");
		legal = legal.replaceAll("\\b([SWEN]{1,2} )?\\d+(\\.\\d+)? FT( OF)?\\b", "");
		legal = legal.replaceAll("\\b(LO) (TS?)\\b", "$1$2");	
		legal = legal.replaceAll("\\b(L) (OTS?)\\b", "$1$2");
		legal = legal.replaceAll("\\b(LOT \\d) (\\d{1,2})(?=\\s|$)", "$1$2");
		legal = legal.replaceAll("\\b(BL) (K)\\b", "$1$2");
		legal = legal.replaceAll("\\b(S) (EC \\d+)\\b", "$1$2");
		legal = legal.replaceAll("\\bL\\s*S \\d+(-\\d+)?\\b", "");
		legal = legal.replaceAll("(?<=\\d) (TO|THRU) (?=\\d)", "-");
		legal = legal.replaceAll("(?<=\\d) AND (?=\\d)", ", ");
		legal = legal.replaceFirst("(,\\s*\\d) (\\d{1,2})$", "$1$2");
		legal = legal.replaceFirst("(\\s*\\b\\d{1,2}) (\\d)$", "$1$2");
		legal = legal.replaceAll("\\b(\\w+)(OR\\s*\\d+)", "$1 $2");		
		legal = legal.replaceAll("\\b[SWEN]{1,2}\\s*\\d+/\\d+( OF)?\\b", "");
		legal = legal.replaceAll("\\bPOR( OF)?\\b", "");
		legal = legal.replaceAll("\\s{2,}", " ");
		
		// extract section, township and range from legal description
 	   	List<List> bodySTR = new ArrayList<List>();
 	   	List<String> line;
 	   	Pattern p = Pattern.compile("\\bLOT/SECT (\\d+) BLK/DIV/TWN (\\d+[SWEN]?) REG/RNG (\\d+[SWEN]?)\\b");
	    Matcher ma = p.matcher(legal);
	    while (ma.find()){
	    	line = new ArrayList<String>();
	    	line.add(ma.group(1));
	    	line.add(ma.group(2));
	    	line.add(ma.group(3));
	    	bodySTR.add(line);		   
	    	legal = legal.replace(ma.group(0), "");
	    }
	    p = Pattern.compile("\\bSEC (\\d+) T\\s*(\\d+[SWEN]?) R\\s*(\\d+[SWEN]?)\\b");
	    ma = p.matcher(legal);
	    while (ma.find()){
	    	line = new ArrayList<String>();
	    	line.add(ma.group(1));
	    	line.add(ma.group(2));
	    	line.add(ma.group(3));
	    	bodySTR.add(line);		   
	    	legal = legal.replace(ma.group(0), "");
	    }
		if (!bodySTR.isEmpty()){
		   String [] header = {"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"};
		   
		   Map<String,String[]> map = new HashMap<String,String[]>();
		   map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
		   map.put("SubdivisionTownship", new String[]{"SubdivisionTownship", ""});
		   map.put("SubdivisionRange", new String[]{"SubdivisionRange", ""});
		   
		   ResultTable pis = new ResultTable();	
		   pis.setHead(header);
		   pis.setBody(bodySTR);	   
		   pis.setMap(map);		   
		   m.put("PropertyIdentificationSet", pis);
		   legal = legal.replaceAll("\\s{2,}", " ").trim();
	    }
		
		//extract lot from legal description
	 	String lot = ""; // can have multiple occurrences
	 	p = Pattern.compile("\\bLOT/SECT 0*(\\d+|[A-Z])\\b");
 		ma = p.matcher(legal);	   
 		while (ma.find()){
 			lot = lot + " " + ma.group(1);
 			legal = legal.replace(ma.group(0), "");
 		}
 		p = Pattern.compile("\\bLOTS?\\s*0*(\\d+(?:-[A-Z](?: AND [A-Z])?)?(?:\\s*[,\\s-]\\s*\\d+)*|\\b[A-Z])\\b");
 		ma = p.matcher(legal);	   
 		while (ma.find()){
 			String lotTemp = ma.group(1);
 			lotTemp = lotTemp.replaceAll("(\\d+-)([A-Z]) AND ([A-Z])", "$1$2 $1$3").replaceAll("\\sAND\\s", " ");
 			lotTemp = lotTemp.replaceAll("\\s*,\\s*", " ");
 			lot = lot + " " + lotTemp; 
 			legal = legal.replace(ma.group(0), " LOT ");
 		} 		
	 	lot =lot.trim();
	 	if (lot.length() != 0){
	 		lot = LegalDescription.cleanValues(lot, false, true);
	 		m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
	 		legal = legal.trim().replaceAll("\\s{2,}", " ");
	 	}
		
	 	// extract block from legal description
	 	String block = "";
	 	p = Pattern.compile("\\bBLK/DIV/TWN (\\d+|[A-Z])\\b");
	 	ma = p.matcher(legal);
	 	while (ma.find()){
	 		block = block + " " + ma.group(1);
	 		legal = legal.replace(ma.group(0), "");
	 	}
	 	p = Pattern.compile("\\bBL(?:OC)?KS? (\\d+|[A-Z](?:(?:\\s| AND )[A-Z]\\b)*)\\b");
	 	ma = p.matcher(legal);
	 	while (ma.find()){
	 		block = block + " " + ma.group(1).replace(" AND ", " ");
	 		legal = legal.replace(ma.group(0), " BLK ");
	 	}
	 	block = block.trim();
	 	if (block.length() != 0){
	 		block = LegalDescription.cleanValues(block, false, true);
	 		m.put("PropertyIdentificationSet.SubdivisionBlock", block);
	 		legal = legal.trim().replaceAll("\\s{2,}", " ");
	 	}
		   
	 	//extract tract from legal description
	 	String tract = ""; 
	 	p = Pattern.compile("\\bTR(?:ACT)? (\\d[\\dA-Z]*(?:-(?:\\d+|[A-Z]\\b))?)\\b");
	 	ma = p.matcher(legal);
	 	while (ma.find()){
	 		tract = tract + " " + ma.group(1);
	 		legal = legal.replace(ma.group(0), " TRACT ");
	 	}
	 	tract = tract.trim();
	 	if (tract.length() != 0){
	 		tract = LegalDescription.cleanValues(tract, false, true);
	 		m.put("PropertyIdentificationSet.SubdivisionTract", tract);
	 		legal = legal.trim().replaceAll("\\s{2,}", " ");
	 	}
	 	
	 	String unit = "";
		p = Pattern.compile("\\bUNITS? (\\d+(?:\\s*[-,]\\s*\\d+)*)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			unit = ma.group(1);
			unit = unit.replaceAll("\\s*,\\s*", " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);	
			legal = legal.replace(ma.group(0), " UNIT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}	
		
		// extract building from legal description
		String bldg = "";
		p = Pattern.compile("\\bBLDG (\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			bldg = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replace(ma.group(0), " BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		   
		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASE)? (\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()){
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).trim());
			legal = legal.replace(ma.group(0), " PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		   
		//extract plat book & page from legal description		
		List<List> bodyPIS = new ArrayList<List>();		
		p = Pattern.compile("\\bBK (\\d+) PG? (\\d+(?:\\s*[-,\\s]\\s*\\d+)?)\\b");
	 	ma = p.matcher(legal);
	 	while (ma.find()){
	 		line = new ArrayList<String>();
	 		line.add(ma.group(1));
	 		line.add(ma.group(2).replaceAll("\\s*,\\s*", " "));
	 		addIfNotAlreadyPresent(bodyPIS, line);
	 		legal = legal.replace(ma.group(0), "");
	 	}
	 	p = Pattern.compile("\\b(?:P\\s*M|M\\s*R) (\\d+)-(\\d+(?:-\\d+)?)\\b");
	 	ma = p.matcher(legal);
	 	while (ma.find()){
	 		line = new ArrayList<String>();
	 		line.add(ma.group(1));
	 		line.add(ma.group(2));
	 		addIfNotAlreadyPresent(bodyPIS, line);
	 		legal = legal.replace(ma.group(0), "");
	 	}
	 	if (!bodyPIS.isEmpty()){	   		   
	 		String [] header = {"PlatBook", "PlatNo"};		   
			Map<String,String[]> map = new HashMap<String,String[]>();		   
			map.put("PlatBook", new String[]{"PlatBook", ""});
			map.put("PlatNo", new String[]{"PlatNo", ""});
			   
			ResultTable pis = new ResultTable();	
			pis.setHead(header);
			pis.setBody(bodyPIS);
			
			ResultTable pisSTR = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisSTR != null){
				pis = ResultTable.joinHorizontal(pis, pisSTR);
				map.putAll(pisSTR.map);
			}
			pis.setMap(map);		   
			m.put("PropertyIdentificationSet", pis);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
	 	}
	 	
	 	// extract cross refs from legal description
	 	List<List> bodyCR = new ArrayList<List>();
	 	p = Pattern.compile("\\bDOC (\\d+(?:,\\s*\\d+)*)\\b(?!-)");
	 	ma = p.matcher(legal);	      	   
	 	while (ma.find()){	 			
	 		String docNo = ma.group(1);
	 		docNo = docNo.replaceAll("\\b0+(\\d+)", "$1");
	 		String docs[] = docNo.split(",\\s*"); 
	 		for (int i=0; i<docs.length; i++){
	 			line = new ArrayList<String>();
	 			line.add("");
	 			line.add("");
	 			line.add(docs[i]);
		 		bodyCR.add(line);
	 		}	 		
	 		legal = legal.replace(ma.group(0), "");
	 	} 	
	 	p = Pattern.compile("\\bOR\\s*(\\d+)-(\\d+)\\b");
	 	ma = p.matcher(legal);	      	   
	 	while (ma.find()){	 			
 			line = new ArrayList<String>();
 			line.add(ma.group(1));
 			line.add(ma.group(2));
 			line.add("");
	 		bodyCR.add(line);	 		
	 		legal = legal.replace(ma.group(0), "");
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
	 		legal = legal.trim().replaceAll("\\s{2,}", " ");
	 	}
	    
	 	if (lot.length() != 0 || unit.length() != 0 || block.length() != 0){
		    String subdiv = "";
		    legal = legal.replaceAll("\\([^\\)]*\\)?", "");
		    legal = legal.replaceAll("\\s{2,}", " ").trim();
	 	   	p = Pattern.compile("(.*?)\\s*(?:\\b(?:LOT|TR(?:ACT)?|CONDO(?:MINIUM)?|ADD)\\b|$)");
	 	   	ma = p.matcher(legal);
	 	   	if (ma.find()){
	 	   		subdiv = ma.group(1);
	 	   	}
	 	   	if (subdiv.length() == 0){
	 	   		p = Pattern.compile("\\bSUBDIVISIONS? OF (.*?) IN\\b");
	 	   		ma = p.matcher(legal);
	 	   		if (ma.find()){
	 	   			subdiv = ma.group(1);
	 	   		}
	 	   	}
	 	   	if (subdiv.length() != 0){
	 	   		subdiv = subdiv.replaceFirst("\\s*\\bOF\\s*$", "");
	 	   		subdiv = subdiv.replaceFirst("\\s*\\b(LICENSED SURVEYOR'?S|PARCEL) MAP\\b.*", "");
	 	   		subdiv = subdiv.replaceFirst("\\s*\\bEX OF\\b.*", "");
	 	   		subdiv = subdiv.replaceFirst("^\\s*SUBDIVISIONS? OF\\b", "");
	 	   		subdiv = subdiv.replaceFirst("\\bLAND DESC\\b.*", "");
	 	   		subdiv = subdiv.replaceFirst("\\bFOR DESC SEE\\b.*", "");
	 	   		subdiv = subdiv.replaceFirst("\\bRECORD OF SURVEY\\b.*", "");
	 	   		subdiv = subdiv.replaceFirst("\\bBLK\\b\\s*", "");
	 	   		subdiv = subdiv.replaceFirst("\\bTHAT PART( OF)?\\b", "");
	 	   		subdiv = subdiv.replaceFirst("^\\s*OF\\b\\s*", "");
	 	   		subdiv = subdiv.replaceFirst("\\s*\\b\\d+/\\d+ VAC\\b.*", "");
	 	   	}	 		 	   	
	 	   	subdiv = subdiv.trim();
	 	   	if (subdiv.length() != 0){ 	   		
	 	   		m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
	 	   		if (legal.matches(".*\\bCONDO(?:MINIUM)?\\b.*")){
	 	   			m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
	 	   		}
	 	   	}
	 	}
	}
	
	public static void legalCAOrangeTaxDT(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;
	   
		legalTokenizerCAOrangeTaxDT(m, legal); 
	}	
	
	public static void legalTokenizerCAOrangeTaxDT(ResultMap m, String legal) throws Exception{
	   				
		// initial cleanup and corrections
		legal = legal.replaceAll("\\.", "");
		legal = legal.replaceAll("\\b\\d+(\\.\\d+)? X \\d+(\\.\\d+)?\\s*FT\\b", "");
		legal = legal.replaceAll("^RESUB OF\\b", "");
		legal = legal.replaceAll("\\bNO(?= \\d)", "");
		legal = legal.replaceAll("\\b(\\w+)(TOGETHER)\\b", "$1 $2"); 
		legal = legal.replaceAll("\\b(AND)(BL(?:OC)?K|LOTS?)\\b", "$1 $2");
		legal = legal.replaceAll("\\b(\\d+)([A-Z]{3,})\\b", "$1 $2");
		legal = legal.replaceFirst("^A\\s*(\\d+)\\b\\s*", "");
		legal = legal.replaceAll("\\s{2,}", " ");
		
	 	//extract tract from legal description
	 	String tract = ""; 
	 	Pattern p = Pattern.compile("\\bTR(?:ACT)?\\s*(\\d+)\\b");
	 	Matcher ma = p.matcher(legal);
	 	while (ma.find()){
	 		tract = tract + " " + ma.group(1);
	 		legal = legal.replace(ma.group(0), " TRACT ");
	 	}
	 	tract = tract.trim();
	 	if (tract.length() != 0){
	 		tract = LegalDescription.cleanValues(tract, false, true);
	 		m.put("PropertyIdentificationSet.SubdivisionTract", tract);
	 		legal = legal.trim().replaceAll("\\s{2,}", " ");
	 	}
		
		//extract plat book & page from legal description		
		List<List> bodyPIS = new ArrayList<List>();
		List<String> line;
		p = Pattern.compile("\\b[PR] BK (\\d+) PG (\\d+)\\b");
	 	ma = p.matcher(legal);
	 	while (ma.find()){
	 		line = new ArrayList<String>();
	 		line.add(ma.group(1));
	 		line.add(ma.group(2));
	 		addIfNotAlreadyPresent(bodyPIS, line);
	 		legal = legal.replace(ma.group(0), " PLAT ");
	 	}
	 	p = Pattern.compile("\\bPM (\\d+)-(\\d+)\\b");
	 	ma = p.matcher(legal);
	 	while (ma.find()){
	 		line = new ArrayList<String>();
	 		line.add(ma.group(1));
	 		line.add(ma.group(2));
	 		addIfNotAlreadyPresent(bodyPIS, line);
	 		legal = legal.replace(ma.group(0), " PLAT ");
	 	}
	 	if (!bodyPIS.isEmpty()){	   		   
	 		String [] header = {"PlatBook", "PlatNo"};		   
			Map<String,String[]> map = new HashMap<String,String[]>();		   
			map.put("PlatBook", new String[]{"PlatBook", ""});
			map.put("PlatNo", new String[]{"PlatNo", ""});
			   
			ResultTable pis = new ResultTable();	
			pis.setHead(header);
			pis.setBody(bodyPIS);
			pis.setMap(map);		   
			m.put("PropertyIdentificationSet", pis);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
	 	}

	 	//extract lot from legal description
	 	String lot = ""; // can have multiple occurrences
	 	p = Pattern.compile("\\bLOTS\\s*(\\d+/\\d+)\\b");
 		ma = p.matcher(legal);	   
 		while (ma.find()){
 			lot = lot + " " + ma.group(1).replaceAll("/", " ");
 			legal = legal.replace(ma.group(0), " LOT ");
 		}
 		p = Pattern.compile("\\bLOTS?\\s*(\\d+[A-Z]?(?:\\s*(?:[,&]|\\bAND\\b)\\s*\\d+[A-Z]?)*|[A-Z])\\b");
 		ma = p.matcher(legal);	   
 		while (ma.find()){
 			lot = lot + " " + ma.group(1).replaceAll("\\s*([,&]|\\bAND\\b)\\s*", " ");
 			legal = legal.replace(ma.group(0), " LOT ");
 		} 		
	 	lot =lot.trim();
	 	if (lot.length() != 0){
	 		lot = LegalDescription.cleanValues(lot, false, true);
	 		m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
	 		legal = legal.trim().replaceAll("\\s{2,}", " ");
	 	}	  
	   	 	
	   // extract block from legal description
	   String block = "";
	   p = Pattern.compile("\\bBL(?:OC)?K\\s*([A-Z]|\\d+(?:-?[A-Z])?)\\b");
	   ma = p.matcher(legal);
	   while (ma.find()){
		   block = block + " " + ma.group(1);
		   legal = legal.replace(ma.group(0), " BLK ");
	   }
	   block = block.trim();
	   if (block.length() != 0){
		   block = LegalDescription.cleanValues(block, false, true);
		   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		   legal = legal.trim().replaceAll("\\s{2,}", " ");
	   }	   
	   
	   // extract section, township and range from legal description
 	   	List<List> bodySTR = new ArrayList<List>();
 	   	p = Pattern.compile("\\bSEC (\\d+) T (\\d+)(?: R (\\d+))?\\b");
	    ma = p.matcher(legal);
	    while (ma.find()){
	    	line = new ArrayList<String>();
	    	line.add(ma.group(1));
	    	line.add(ma.group(2));
	    	String rng = ma.group(3);
	    	if (rng == null)
	    		rng = "";
	    	line.add(rng);
	    	bodySTR.add(line);		   
	    	legal = legal.replace(ma.group(0), " SEC ");
	    }
	    p = Pattern.compile("\\bT (\\d+) R (\\d+) SEC (\\d+)\\b");
	    ma = p.matcher(legal);
	    while (ma.find()){
	    	line = new ArrayList<String>();
	    	line.add(ma.group(3));
	    	line.add(ma.group(1));
	    	line.add(ma.group(2));
	    	bodySTR.add(line);		   
	    	legal = legal.replace(ma.group(0), " SEC ");
	    }
		if (!bodySTR.isEmpty()){
		   String [] header = {"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"};
		   
		   Map<String,String[]> map = new HashMap<String,String[]>();
		   map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
		   map.put("SubdivisionTownship", new String[]{"SubdivisionTownship", ""});
		   map.put("SubdivisionRange", new String[]{"SubdivisionRange", ""});
		   
		   ResultTable pis = new ResultTable();	
		   pis.setHead(header);
		   pis.setBody(bodySTR);
		   
		   ResultTable pisPLAT = (ResultTable) m.get("PropertyIdentificationSet");
		   if (pisPLAT != null){
			   pis = ResultTable.joinHorizontal(pis, pisPLAT);
			   map.putAll(pisPLAT.map);
		   }
		   pis.setMap(map);		   
		   m.put("PropertyIdentificationSet", pis);
		   legal = legal.replaceAll("\\s{2,}", " ").trim();
	    }
 	   	String sec = "";
 	   	p = Pattern.compile("\\bSEC (\\d+)\\b");  	   	
 	   	ma = p.matcher(legal);	   
	    while (ma.find()){
	    	sec = sec + " " + ma.group(1);
	    	legal = legal.replace(ma.group(0), " SEC ");
	    }
	    sec = sec.trim();
	    if (sec.length() != 0){
	    	sec = LegalDescription.cleanValues(sec, true, true);
	    	m.put("PropertyIdentificationSet.SubdivisionSection", sec); 	    	 
	    	legal = legal.trim().replaceAll("\\s{2,}", " ");
	    }
	    
	    String subdiv = "";
	    legal = legal.replaceAll("\\([^\\)]*\\)?", "");
	    legal = legal.replaceAll("\\s{2,}", " ").trim();
 	   	p = Pattern.compile("^(.*?)\\s*\\b(?:LOTS?|BL(?:OC)?K|TR(?:ACT)?|(?:RE)?SUB|SEC(?:TION)?|ADD)\\b");
 	   	ma = p.matcher(legal);
 	   	if (ma.find()){
 	   		subdiv = ma.group(1);		   
 	   	}	 	   
 	   	subdiv = subdiv.trim();
 	   	if (subdiv.length() != 0){ 	   		
 	   		m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
 	   	}
	}
	
	public static void legalCAAlamedaTaxDT(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;
	   
		legalTokenizerCAAlamedaTaxDT(m, legal); 
	}	
	
	public static void legalTokenizerCAAlamedaTaxDT(ResultMap m, String legal) throws Exception{
	   				
	 	//extract tract from legal description
	 	String tract = ""; 
	 	Pattern p = Pattern.compile("\\bTR: (\\w+)\\b(?!:)");
	 	Matcher ma = p.matcher(legal);
	 	while (ma.find()){
	 		tract = tract + " " + ma.group(1).replaceFirst("^0+(\\w+)", "$1");
	 	}
	 	tract = tract.trim();
	 	if (tract.length() != 0){
	 		tract = LegalDescription.cleanValues(tract, false, true);
	 		m.put("PropertyIdentificationSet.SubdivisionTract", tract);
	 	}
		
		//extract plat book & page from legal description		
		List<List> bodyPIS = new ArrayList<List>();
		List<String> line;
		p = Pattern.compile("\\bBK: ([A-Z\\d]+) PAGE: ([A-Z\\d]+)\\b(?!:)");
	 	ma = p.matcher(legal);
	 	while (ma.find()){
	 		line = new ArrayList<String>();
	 		line.add(ma.group(1).replaceFirst("^([A-Z]?)0+(.+)", "$2"));
	 		line.add(ma.group(2).replaceFirst("^0+(.+)", "$1"));
	 		addIfNotAlreadyPresent(bodyPIS, line);
	 	}
	 	p = Pattern.compile("\\b(?:MP|PM): (\\w+) PAGE: (\\d+)\\b(?!:)");
	 	ma.usePattern(p);
	 	ma.reset();
	 	while (ma.find()){
	 		line = new ArrayList<String>();
	 		line.add(ma.group(1).replaceFirst("^0+(.+)", "$1"));
	 		line.add(ma.group(2).replaceFirst("^0+(.+)", "$1"));
	 		addIfNotAlreadyPresent(bodyPIS, line);
	 	}
	 	if (!bodyPIS.isEmpty()){	   		   
	 		String [] header = {"PlatBook", "PlatNo"};		   
			Map<String,String[]> map = new HashMap<String,String[]>();		   
			map.put("PlatBook", new String[]{"PlatBook", ""});
			map.put("PlatNo", new String[]{"PlatNo", ""});
			   
			ResultTable pis = new ResultTable();	
			pis.setHead(header);
			pis.setBody(bodyPIS);
			pis.setMap(map);		   
			m.put("PropertyIdentificationSet", pis);
	 	}

	 	//extract lot from legal description
	 	String lot = ""; // can have multiple occurrences
 		p = Pattern.compile("\\bLOT: (\\w+)\\b(?!:)");
 		ma = p.matcher(legal);	   
 		while (ma.find()){
 			lot = lot + " " + ma.group(1).replaceFirst("^0+(\\w+)", "$1");
 		}
	 	lot =lot.trim();
	 	if (lot.length() != 0){
	 		lot = LegalDescription.cleanValues(lot, false, true);
	 		m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
	 	}	  
	   
	   // extract building from legal description
	   String bldg = "";
	   p = Pattern.compile("\\bBLDG: (\\w+)\\b(?!:)");
	   ma = p.matcher(legal);
	   while (ma.find()){
		   bldg = bldg + "  "+ ma.group(1).replaceFirst("^0+(\\w+)", "$1");
	   }
	   bldg = bldg.trim();
	   if (bldg.length() != 0){
		   bldg = LegalDescription.cleanValues(bldg, false, true);
		   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
	   }
	 	
	   // extract block from legal description
	   String block = "";
	   p = Pattern.compile("\\bBLK: (\\w+)\\b(?!:)");
	   ma = p.matcher(legal);
	   while (ma.find()){
		   block = block + " " + ma.group(1).replaceFirst("^0+(\\w+)", "$1");
	   }
	   block = block.trim();
	   if (block.length() != 0){
		   block = LegalDescription.cleanValues(block, false, true);
		   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
	   }	   
	   
	   // extract unit from legal description
	   String unit = "";
	   p = Pattern.compile("\\bUNIT: (\\w+)\\b(?!:)");
	   ma = p.matcher(legal);
	   while (ma.find()){
		   unit = unit + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");		  
	   }
	   unit = unit.trim();
	   if (unit.length() != 0){
		   unit = LegalDescription.cleanValues(unit, false, true);
		   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);		  		   		   
	   }	   	   
	}
   
	public static void legalCAContraCostaTaxDT(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;
		String address = (String) m.get("tmpAddress");
		if (!StringUtils.isEmpty(address)){
			if (GenericNameFilter.computeScoreForStrings(legal, address) >= 0.93){
				return;
			}
		}
		legalTokenizerCAContraCostaTaxDT(m, legal); 
	}	
	
	public static void legalTokenizerCAContraCostaTaxDT(ResultMap m, String legal) throws Exception{
	   
		String subdivNo = null;
		Pattern p = Pattern.compile("^T0*(\\d+[A-Z]?)\\b\\s*(.*)"); 
		Matcher ma = p.matcher(legal);
		if (ma.find()){
			subdivNo = ma.group(1);	
			legal = ma.group(2);
		}
		
		//B 4036
		String address = (String) m.get("tmpAddress");
		legal = legal.replaceAll("(.*)" + address + "(.*)", "$1 $2");

		//initial cleanup of legal description
		legal = legal.replaceAll("\\bNO(?= \\d)", "");
		legal = legal.replaceAll("\\bPORS?\\b", "");
		legal = legal.replaceAll("(?<=\\d)\\s*TO\\s*(?=\\d)", "-");
		legal = legal.replaceFirst("[\\d\\.]+ AC(RES?)?\\b", "");
		legal = legal.replaceFirst("\\bEXT(CEPT)?\\b", "");
		legal = legal.replaceAll("\\s{2,}", " ");
		legal = legal.replaceAll("W75 OF E180FT OF S2.5 OF N10AC OF W H OF SEQ OF NEQ", "");
		//B3727
		legal = legal.replaceFirst("\\d+[^/]+/ VOL CONV TO LPT \\d+ ", "");
		legal = legal.replaceFirst("FOR TOTAL DESCRIPTION SEE ASSESSORS MAPS","");
		
		String form1, form2 = null;
		p = Pattern.compile("(.*?)\\s?\\b((LT|L|BLK|BK|B|U|BLD):.*)"); 
		ma = p.matcher(legal);
		if (ma.matches()){
			form1 = ma.group(1);
			form2 = ma.group(2);
		} else {
			form1 = legal;
		}
		
	 	//extract tract from legal description
	 	String tract = ""; 
	 	p = Pattern.compile("\\bTR(?:ACT)? (\\d+)\\b");
	 	ma = p.matcher(form1);
	 	if (ma.find()){
	 		tract = tract + " " + ma.group(1).replaceFirst("^0+(\\w+)", "$1");
	 	}
	 	p = Pattern.compile("\\bTR(\\d+)\\b");
	 	ma.usePattern(p);
	 	ma.reset();
	 	if (ma.find()){
	 		tract = tract + " " + ma.group(1).replaceFirst("^0+(\\w+)", "$1");
	 		form1 = form1.replaceFirst("\\b"+ ma.group(0) +"\\b", "TRACT ");
	 	}
	 	tract = tract.trim();
	 	if (tract.length() != 0){
	 		tract = LegalDescription.cleanValues(tract, false, true);
	 		m.put("PropertyIdentificationSet.SubdivisionTract", tract);
	 	}
		
		//extract plat book & page from legal description		
		List<List> bodyPIS = new ArrayList<List>();
		List<String> line;
		if (form2 != null){
			p = Pattern.compile("\\bBK: ([A-Z\\d]+) PG:\\s*([A-Z\\d]+)\\b");
		 	ma = p.matcher(form2);
		 	while (ma.find()){
		 		line = new ArrayList<String>();
		 		line.add(ma.group(1).replaceFirst("^([A-Z]?)0+(.+)", "$1$2"));
		 		line.add(ma.group(2).replaceFirst("^0+(.+)", "$1"));
		 		addIfNotAlreadyPresent(bodyPIS, line);
		 	}
		 	p = Pattern.compile("\\bB:([A-Z\\d]+) P:([A-Z\\d]+)\\b");
		 	ma.usePattern(p);
		 	ma.reset();
		 	while (ma.find()){
		 		line = new ArrayList<String>();
		 		line.add(ma.group(1).replaceFirst("^([A-Z]?)0+(.+)", "$1$2"));
		 		line.add(ma.group(2).replaceFirst("^0+(.+)", "$1"));
		 		addIfNotAlreadyPresent(bodyPIS, line);
		 	}
		}
		p = Pattern.compile("\\bMAP (\\d+) PG (\\d+)\\b");
	 	ma = p.matcher(form1);
	 	while (ma.find()){
	 		line = new ArrayList<String>();
	 		line.add(ma.group(1).replaceFirst("^0+(.+)", "$1"));
	 		line.add(ma.group(2).replaceFirst("^0+(.+)", "$1"));
	 		addIfNotAlreadyPresent(bodyPIS, line);
	 	}		
	 	if (!bodyPIS.isEmpty()){	   		   
	 		String [] header = {"PlatBook", "PlatNo"};		   
			Map<String,String[]> map = new HashMap<String,String[]>();		   
			map.put("PlatBook", new String[]{"PlatBook", ""});
			map.put("PlatNo", new String[]{"PlatNo", ""});
			   
			ResultTable pis = new ResultTable();	
			pis.setHead(header);
			pis.setBody(bodyPIS);
			pis.setMap(map);		   
			m.put("PropertyIdentificationSet", pis);
	 	}

	 	//extract lot from legal description
	 	String lot = ""; // can have multiple occurrences
	 	if (form2 != null){
	 		p = Pattern.compile("\\bLT: (\\w+)\\b(?!:)");
	 		ma = p.matcher(form2);	   
	 		while (ma.find()){
	 			lot = lot + " " + ma.group(1).replaceFirst("^0+(\\w+)", "$1");
	 		}
	 		p = Pattern.compile("\\bL:(\\w+)\\b");
	 		ma.usePattern(p);
	 		ma.reset();
	 		while (ma.find()){
	 			lot = lot + " " + ma.group(1).replaceFirst("^0+(\\w+)", "$1");
	 		}
	 	}
	 	p = Pattern.compile("\\bLO?TS? (\\d+[A-Z]?(?:[&,\\s-]+\\d+[A-Z]?)*)\\b");
	 	ma = p.matcher(form1);	   
	 	if (ma.find()){
	 		lot = lot + " " + ma.group(1).replaceAll("\\s*[&,]\\s*", " ").replaceAll("\\b0+(\\w+)", "$1");
	 	}
	 	p = Pattern.compile("\\bL\\s?(\\d+[A-Z]?(?:[&,\\s-]+\\d+[A-Z]?)*)\\b");
	 	ma.usePattern(p);
	 	ma.reset();
	 	if (ma.find()){
	 		lot = lot + " " + ma.group(1).replaceAll("\\s*[&,]\\s*", " ").replaceFirst("^0+(\\w+)", "$1");
	 		form1 = form1.replaceFirst("\\b"+ ma.group(0) +"\\b", "LOT ");
	 	}
	 	lot =lot.trim();
	 	if (lot.length() != 0){
	 		lot = LegalDescription.cleanValues(lot, false, true);
	 		m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
	 	}	  
	   
	   // extract building from legal description
	   String bldg = "";
	   if (form2 != null){
		   p = Pattern.compile("\\bBLD:\\s?(\\w+)\\b(?!:)");
		   ma = p.matcher(form2);
		   while (ma.find()){
			   bldg = bldg + "  "+ ma.group(1).replaceFirst("^0+(\\w+)", "$1");
		   }
	   }
	   p = Pattern.compile("\\bBG\\s*(\\d+)\\b");
	   ma = p.matcher(form1);
	   if (ma.find()){
		   bldg = bldg + "  "+ ma.group(1).replaceFirst("^0+(\\w+)", "$1");
		   form1 = form1.replaceFirst("\\b"+ ma.group(0) +"\\b", "BLDG ");
	   }
	   p = Pattern.compile("\\bBLDG? (\\w+)\\b");
	   ma = p.matcher(form1);
	   if (ma.find()){
		   bldg = bldg + "  "+ ma.group(1).replaceFirst("^0+(\\w+)", "$1");
	   }
	   bldg = bldg.trim();
	   if (bldg.length() != 0){
		   bldg = LegalDescription.cleanValues(bldg, false, true);
		   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
	   }
	 	
	   // extract block from legal description
	   String block = "";
	   if (form2 != null){
		   p = Pattern.compile("\\bBLK: (\\w+)\\b(?!:)");
		   ma = p.matcher(form2);
		   while (ma.find()){
			   block = block + " " + ma.group(1).replaceFirst("^0+(\\w+)", "$1");
		   }
	   }
	   p = Pattern.compile("\\bB(\\d+[A-Z]?|[A-Z]\\d*)\\b");
	   ma = p.matcher(form1);
	   if (ma.find()){
		   block = block + " " + ma.group(1).replaceFirst("^0+(\\w+)", "$1");
		   form1 = form1.replaceFirst("\\b"+ ma.group(0) +"\\b", "BLK ");
	   }
	   p = Pattern.compile("\\bBLK (\\d+|[A-Z])\\b");
	   ma = p.matcher(form1);
	   if (ma.find()){
		   block = block + " " + ma.group(1).replaceFirst("^0+(\\w+)", "$1");
	   }
	   block = block.trim();
	   if (block.length() != 0){
		   block = LegalDescription.cleanValues(block, false, true);
		   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
	   }	   
	   
	   // extract unit from legal description
	   String unit = "";
	   if (form2 != null){
		   p = Pattern.compile("\\bU:(\\w+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   unit = unit + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");		  
		   }
	   }
	   p = Pattern.compile("\\bU(\\d+)\\b");
	   ma = p.matcher(form1);
	   if (ma.find()){
		   unit = unit + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");
		   form1 = form1.replaceFirst("\\b"+ ma.group(0) +"\\b", "UNIT ");
	   }
	   p = Pattern.compile("\\bUNIT (\\d+|[A-Z])\\b");
	   ma = p.matcher(form1);
	   if (ma.find()){
		   unit = unit + " " + ma.group(1).replaceFirst("^0+(.+)", "$1");
	   }
	   unit = unit.trim();
	   if (unit.length() != 0){
		   unit = LegalDescription.cleanValues(unit, false, true);
		   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);		  		   		   
	   }
	   	   
	   // extract and remove section, township and range from legal description	   	   
	   p = Pattern.compile("\\bSEC (\\d+) T(\\d+[SWEN]?) R(\\d+[SWEN]?)\\b");
	   ma.usePattern(p);
	   ma.reset();
	   if (ma.find()){	
		   m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));
		   m.put("PropertyIdentificationSet.SubdivisionTownship", ma.group(2));
		   m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(3));
	   } else {
		   //W75 OF E180FT OF S2.5 OF N10AC OF W H OF SEQ OF NEQ SEC 25-9-4W
		   p = Pattern.compile("\\bSEC (\\d+)\\-(\\d+)\\-(\\d+([SWEN]+)?)");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){	
			   m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));
			   m.put("PropertyIdentificationSet.SubdivisionTownship", ma.group(2));
			   m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(3));
			   //String newLegal = ma.replaceFirst("");
			   //ma = p.matcher(newLegal);
		   }
	   }
	  
	   legal = legal.trim().replaceAll("\\s{2,}", " ");
	   
	   // extract subdivision name
	   String subdiv = "";	   
	   p = Pattern.compile("(?is)\\d+/\\d+ (.*?)(ADDN?)"); // B 4000
	   ma.usePattern(p);
	   ma.reset();
	   if (ma.find()){
		   subdiv = ma.group(1) + ma.group(2);
	   } else {
		   p = Pattern.compile("(.*?)\\s*\\b(LO?TS?|TR(ACT)?|BLK|L B|PARCEL|PCL|SEC|SUBN?|(\\d+(ST|ND|RD|TH) )?ADDN|MAP|BLDG?|UNIT|CONDO(INIUMS?)?)\\b.*");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   subdiv = ma.group(1);
		   } else {
			   subdiv = form1;
		   }
	   }
	   if (subdiv.length() != 0){
		   subdiv = subdiv.replaceFirst("\\s*\\b[A-Z]\\s*$", "");
		   subdiv = subdiv.replaceFirst("\\s*#?[\\d\\s&]+$", "");
		   subdiv = subdiv.replaceFirst("\\b(REC(ORD)? OF|ORIGINAL) SURVEY\\b.*", "");
		   subdiv = subdiv.replaceFirst("^[\\d-]+$", "");
		   subdiv = subdiv.replaceFirst("^(TOWN|CITY) OF\\b", "");
		   subdiv = subdiv.replaceFirst("^\\d+T\\b", ""); // PIN 189-190-009-2		   
	   }
	   subdiv = subdiv.trim();
	   if (subdiv.length() != 0){		   
		   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
		   if (legal.contains("CONDO"))
			   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
	   }
	   
	   if (subdivNo != null){
		   m.put("PropertyIdentificationSet.SubdivisionNo", subdivNo);
	   }
	}
   
	public static void pisTaxDASLDT(ResultMap m, long searchId) throws Exception {
		String name = (String) m.get("PropertyIdentificationSet.OwnerLastName");
		if (name == null || name.length() == 0)
			return; 
		
		// cleanup
		name = name.replaceAll("\\([^\\)]*\\)?", "");
		name = StringFormats.unifyNameDelim(name);
		name = name.replaceFirst("\\s*\\bTRE\\s*$", "");
		name = name.replaceFirst("\\s*\\bEST(ATE)?( OF)?\\b", "");
		name = name.replaceAll("\\bTRU?S?\\b", "");
		name = name.replaceAll("\\bHEIRS?( OF)?\\b", "");
		name = name.replaceAll("\\s{2,}", " ").trim();
		
		if (NameUtils.isCompany(name)){
			m.put("PropertyIdentificationSet.OwnerLastName", name);
		}
		
		String[] a = StringFormats.parseNameNashville(name);
		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	}
	
	public static void partyNameTaxDASLDT(ResultMap m, long searchId) throws Exception {
	   
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
	   
		if ("CA".equals(crtState)){
			if ("Alameda".equals(crtCounty)){
				partyNamesCAAlamedaTaxDT(m, searchId);
			} else if ("Orange".equals(crtCounty)){
				partyNamesCAOrangeTaxDT(m, searchId);				
			} else if ("Los Angeles".equals(crtCounty)){
				partyNamesCALosAngelesTaxDT(m, searchId);				
			} else {
				partyNamesCAContraCostaTaxDT(m, searchId);
			}
		}
	}
	
	public static void partyNamesCAOrangeTaxDT(ResultMap m, long searchId) throws Exception {
		String name = (String) m.get("tmpOwnerName");
		if (name == null || name.length() == 0)
			return;
		
		partyNamesTokenizerCAOrangeTaxDT(m, name); 
	}
	
	public static void partyNamesTokenizerCAOrangeTaxDT(ResultMap m, String s) throws Exception {
		
		boolean isFML = s.matches("EST(AT)?E?S? OF .+");
		// cleanup
	    s = s.replaceAll("\\s*\\b(?<!(IR)?REVOCABLE )TR\\s*$", "");
	    s = s.replaceAll("\\s*\\b(CNSV|EST(AT)?E?S?|TR) OF [A-Z]\\s*$", "");
	    s = s.replaceAll("\\s*\\b(CNSV|EST(AT)?E?S?)( OF)?\\b", "");
	    s = s.replaceAll("\\.", "");
	    s = s.replaceFirst("^%", "");	    
	    s = s.replaceAll("\\s{2,}", " ").trim();
	    		    
	    // parse first entity as L F M
	    String[] a = {"", "", "", "", "", ""};	    	   
        String[] suffixes;
        List<List> body = new ArrayList<List>();
        
        if (NameUtils.isCompany(s)){        		 
    		a[2] = s;
    		addOwnerNames(a, "", "", true, false, body);
        } else {
        	if (s.matches("[A-Z]( [A-Z]+)? [A-Z'-]{2,}") || isFML){
        		a = StringFormats.parseNameDesotoRO(s);
        	} else {
        		a = StringFormats.parseNameNashville(s);
        	}
        	suffixes = extractNameSuffixes(a);
        	addOwnerNames(a, suffixes[0], suffixes[1], false, false, body);
        }        	    
	    storeOwnerInPartyNames(m, body);  
	}
	
	public static void partyNamesCAAlamedaTaxDT(ResultMap m, long searchId) throws Exception {
		String name = (String) m.get("tmpOwnerName");
		if (name == null || name.length() == 0)
			return;
		
		partyNamesTokenizerCAAlamedaTaxDT(m, name); 
	}
	
	public static void partyNamesTokenizerCAAlamedaTaxDT(ResultMap m, String s) throws Exception {
		// cleanup
	    s = s.replaceAll("\\([^\\)]*\\)?", "");
	    s = StringFormats.unifyNameDelim(s);
	    s = s.replaceAll("\\bTRU?S?\\b", "");
	    s = s.replaceAll("\\bHEIRS?( OF)?\\b", "");
	    s = s.replaceAll("\\bEST(ATE)?( OF)?\\b", "");
	    s = s.replaceAll("\\s{2,}", " ").trim();
	    		    
	    // parse first entity as L F M
	    String[] a = {"", "", "", "", "", ""}; 
        String[] addOwners;
        String[] suffixes;
        List<List> body = new ArrayList<List>();
        Pattern p = Pattern.compile("(.+?)\\s*&\\s*([^&]+)&?(.*)");
	    String owner, spouse = "", others = "";
    	Matcher ma1 = p.matcher(s);
    	if (ma1.matches()){
    		owner = ma1.group(1);
    		spouse = ma1.group(2).trim();
    		others = ma1.group(3).trim();
    	} else {
    		owner = s;
    	}
    	boolean ownerIsCompany = false;
    	if (NameUtils.isCompany(owner)){        		 
        	if (NameUtils.isCompany(spouse)){
        		a[2] = s;
        		addOwnerNames(a, "", "", true, false, body);
        		ownerIsCompany = true;
        		spouse = "";
        	} else {
        		a[2] = owner;
            	addOwnerNames(a, "", "", true, false, body);  
            	ownerIsCompany = true;
        	}
    	} else if (NameUtils.isCompany(s)){
    		a[2] = s; 
        	addOwnerNames(a, "", "", true, false, body);
        	ownerIsCompany = true;
        	spouse = "";
    	}
    	String spouseSuffix = "";
		if (spouse.length() != 0){
			Matcher ma2 = nameSuffix.matcher(spouse);
			if (ma2.matches()){		// spouse has suffix => remove it 
    			spouse = ma2.group(1).trim();
    			spouseSuffix = ma2.group(2);
    		} 
		}
    	if (!ownerIsCompany){
    		if (spouse.matches("[A-Z]+( [A-Z])?")){
    			a = StringFormats.parseNameNashville(owner + " & " + spouse);
    			spouse = "";
    		}
    		else {
    			a = StringFormats.parseNameNashville(owner);
    		}
    		suffixes = extractNameSuffixes(a);
    		if (spouseSuffix.length() != 0 && spouse.length() == 0){
    			suffixes[1] = spouseSuffix;
    		}
			addOwnerNames(a, suffixes[0], suffixes[1], false, false, body);
    	}
		if (spouse.length() != 0){		    		       		        	
			a = StringFormats.parseNameNashville(spouse);				
			addOwnerNames(a, spouseSuffix, "", NameUtils.isCompany(spouse), false, body);
		}
		String prevLast = a[2];	       
    	if (others.length() != 0){
    		addOwners = others.trim().split("\\s?&\\s?");
    		for (int j=0; j<addOwners.length; j++){        			
    			if (!addOwners[j].matches("[A-Z'-]+ [A-Z]{2,}.*") && !addOwners[j].contains(prevLast)){
    				addOwners[j] = prevLast + " " + addOwners[j];
    			}
    			a = StringFormats.parseNameNashville(addOwners[j]);
    			prevLast = a[2];
    			suffixes = extractNameSuffixes(a);
    			addOwnerNames(a, suffixes[0], suffixes[1], NameUtils.isCompany(addOwners[j]), false, body);
    		}
    	}
	    
	    storeOwnerInPartyNames(m, body);  
	}
	
	public static void partyNamesCALosAngelesTaxDT(ResultMap m, long searchId) throws Exception {
		String name = (String) m.get("tmpOwnerName");
		if (name == null || name.length() == 0)
			return;
		
		partyNamesTokenizerCALosAngelesTaxDT(m, name); 
	}
	
	public static Pattern pattLFM = Pattern.compile("([A-Z']+ [A-Z']+),\\s*([A-Z]+)( [A-Z])?");
	public static void fixLFMparser(String a[]){
		
		if (a != null && a.length >= 3 && a[0].length() == 0){
			Matcher ma2 = pattLFM.matcher(a[2]);					
			if (ma2.matches()){
				a[0] = ma2.group(2);
				String middle = ma2.group(3);
				if (middle == null)
					middle = "";
				else 
					middle = middle.trim();
				a[1] = middle;
				a[2] = ma2.group(1);
			}
		}
	}
	
	public static void partyNamesTokenizerCALosAngelesTaxDT(ResultMap m, String s) throws Exception {
		// cleanup and corrections	
	    s = s.replaceAll("\\s+(?=,)", "");
	    s = s.replaceAll("\\bTR(US?)?$", "TRUST");
	    s = s.replaceAll("\\bFAMILY TR?$", "FAMILY TRUST");  // the owner name is truncated at 36 character, we need to fix it where we can
	    s = s.replaceAll("\\bFAMI?(LY?)?$", "FAMILY TRUST");
	    s = s.replaceAll("((.+?),.+) (\\2 )F\\w*$", "$1 $3 FAMILY TRUST");
	    s = s.replaceAll("((.+?),.+) (\\2 )T\\w*$", "$1 $3 TRUST");
	    s = s.replaceFirst("\\s*,\\s*$", "");
	    s = s.replaceAll("\\s{2,}", " ").trim();
	    
	    String owner, spouse = "", others = "", last = "";
	    // separate the family trust from the end of the name string
	    Pattern p = Pattern.compile("(.+)\\bCO(?:-TR)?\\b(.+)");	  // RIZZOTTI,CHRISTOPHER J CO RIZZOTTI
    	Matcher ma1 = p.matcher(s);
    	if (ma1.matches()){
    		last = ma1.group(2).trim();
    		s = ma1.group(1);
    	} else {
		    p = Pattern.compile("^(.+,([A-Z]{2,}(?: [A-Z])?)\\b.*) (\\2.+)");		// CAPPS,JODI M JODI M CAPPS TRUST
	    	ma1 = p.matcher(s);													// or BROWN,JUNE M JUNE MARIE BROWN TRUST
	    	if (ma1.matches()){													// or SMITH CANGE,MILDRED MILDRED CANGE S
	    		last = ma1.group(3).trim();
	    		s = ma1.group(1);
	    	} else {
		    	p = Pattern.compile("(([A-Z'-]+),([A-Z]+)) (\\3 \\2.+)");	// MILLER,JAN JAN MILLER TRUST
		    	ma1 = p.matcher(s);
		    	if (ma1.matches()){
		    		last = ma1.group(4).trim();
		    		s = ma1.group(1);
		    	} else {
		    		p = Pattern.compile("(([A-Z'-]+)\\b,?\\s?[A-Z]+ [A-Z](?: "+nameSuffixString+")?) ([A-Z]+ [A-Z] \\2\\b.+)");	// BROWN,JEANETTE S DONALD L BROWN TRU  
			    	ma1 = p.matcher(s);
			    	if (ma1.matches()){
			    		last = ma1.group(4).trim();
			    		s = ma1.group(1);
			    	} else {
				    	p = Pattern.compile("(.+,([A-Z]+&[A-Z]+)(?: [A-Z])?) (\\2 .+)");	// GOLD,MEL&ELLEN MEL&ELLEN GOLD TRUST    
				    	ma1 = p.matcher(s);
				    	if (ma1.matches()){
				    		last = ma1.group(3).trim();
				    		s = ma1.group(1);
				    	} else {
					    	p = Pattern.compile("(([A-Z'-]+)\\b,?.+&.+) (\\2 FAMILY\\b.+)"); // BROWN,JOHN&JUNE R BROWN FAMILY TRUS  
					    	ma1 = p.matcher(s);
					    	if (ma1.matches()){
					    		last = ma1.group(3).trim();
					    		s = ma1.group(1);
					    	} else {
						    	p = Pattern.compile("(([A-Z'-]+),[A-Z]+(?: [A-Z]+)?) (\\2 TRUST)"); // BROWN,LOYCE W BROWN TRUST 
						    	ma1 = p.matcher(s);
						    	if (ma1.matches()){
						    		last = ma1.group(3).trim();
						    		s = ma1.group(1);
						    	} else {
						    		p = Pattern.compile("([A-Z'-]+,[A-Z]+ [A-Z]+) ([A-Z]+ [A-Z] [A-Z'-]{2,} TRUST)"); 	// MILLER,JAMES F JAMES B JESSUP TRUST 
							    	ma1 = p.matcher(s);
							    	if (ma1.matches()){
							    		last = ma1.group(2);
							    		s = ma1.group(1);
							    	} else {
							        	// separate co-owner
							        	p = Pattern.compile("(.+?,[A-Z]+ [A-Z]+) (.+,[A-Z]+ [A-Z]+)");		//LAWRENCE,SEAN M COOKSON,A CHRISTIAN  
							        	ma1 = p.matcher(s);
							        	if (ma1.matches()){
							        		last = ma1.group(2);
							        		s = ma1.group(1);
							        	}
							    	}
						    	}
					    	}
				    	}
			    	}
		    	}
	    	}
    	}
    	if (last.length() == 0){
    		p = Pattern.compile("(.+) (\\w+ FAMILY TRUST)");		   //SMITH,C THOMAS&MARTHA N TMS FAMILY  
        	ma1 = p.matcher(s);
        	if (ma1.matches()){
        		last = ma1.group(2);
        		s = ma1.group(1);
        	} else {
        		p = Pattern.compile("(.+,.+&.+) ([A-Z]&[A-Z] .+ TRUST)");		 //SMITH,BRIT O&MARYLOU B&M SMITH TRUS  
            	ma1 = p.matcher(s);
            	if (ma1.matches()){
            		last = ma1.group(2);
            		s = ma1.group(1);
            	} else {
            		p = Pattern.compile("((.+),.+&.+) ([A-Z]+&[A-Z]+(?: [A-Z])? \\2)");		//SMITH,CHARLES&PHYLLIS C K&P J SMITH  
                	ma1 = p.matcher(s);
                	if (ma1.matches()){
                		last = ma1.group(3);
                		s = ma1.group(1);
                	} else {
                		p = Pattern.compile("(.+,([A-Z]+)\\b.*&([A-Z]+)\\b.*) (\\2&\\3.*)");		//SMITH,JOHN J&JO ANN M JOHN&JO ANN S 
                    	ma1 = p.matcher(s);
                    	if (ma1.matches()){
                    		last = ma1.group(3);
                    		s = ma1.group(1);
                    	} else {
                    		p = Pattern.compile("(.+,.+&([A-Z]+)\\b.*) (\\2&.*)");					//MILLER,JOHN R&BARBARA A BARBARA&JOH 
                        	ma1 = p.matcher(s);
                        	if (ma1.matches()){
                        		last = ma1.group(3);
                        		s = ma1.group(1);
                        	}
                    	}
                	}                	
            	}
        	}
    	}
	    // parse first entity as L F M
	    String[] a = {"", "", "", "", "", ""}; 
        String[] suffixes;
        List<List> body = new ArrayList<List>();
        p = Pattern.compile("(.+?)\\s*&\\s*([^&]+)&?(.*)");
	    
    	ma1 = p.matcher(s);
    	if (ma1.matches()){
    		owner = ma1.group(1);
    		spouse = ma1.group(2).trim();
    		others = ma1.group(3).trim();
    	} else {
    		owner = s;
    	}
    	boolean ownerIsCompany = false;
    	if (NameUtils.isCompany(owner)){        		 
        	if (NameUtils.isCompany(spouse)){
        		a[2] = s;
        		addOwnerNames(a, "", "", true, false, body);
        		ownerIsCompany = true;
        		spouse = "";
        	} else {
        		a[2] = owner;
            	addOwnerNames(a, "", "", true, false, body);  
            	ownerIsCompany = true;
        	}
    	} else if (NameUtils.isCompany(s)){
    		a[2] = s; 
        	addOwnerNames(a, "", "", true, false, body);
        	ownerIsCompany = true;
        	spouse = "";
    	}
    	
    	Matcher ma2;
    	String spouseSuffix = "";
		if (spouse.length() != 0){
			ma2 = nameSuffix.matcher(spouse);
			if (ma2.matches()){		// spouse has suffix => remove it 
    			spouse = ma2.group(1).trim();
    			spouseSuffix = ma2.group(2);
    		} 
		}
    	if (!ownerIsCompany){
    		if (spouse.matches("[A-Z]+( [A-Z])?") || spouse.matches("[A-Z] [A-Z]+")){
    			a = StringFormats.parseNameNashville(owner + " & " + spouse);
    			spouse = "";
    		}
    		else {
    			a = StringFormats.parseNameNashville(owner);
    		}
    		fixLFMparser(a);				// EDLAM SMITH,LORNA&SMITH,ALEX
    		
    		suffixes = extractNameSuffixes(a);
    		if (spouseSuffix.length() != 0 && spouse.length() == 0){
    			suffixes[1] = spouseSuffix;
    		}
			addOwnerNames(a, suffixes[0], suffixes[1], false, false, body);
    	}    	
		if (spouse.length() != 0){
			if (spouse.matches("[A-Z]+ [A-Z] [A-Z]")){				
				spouse = a[2] + ", " + spouse;
			} else {
				ma1 = Pattern.compile("([A-Z]{2,}) ([A-Z'-]{2,}) ([A-Z])").matcher(spouse);
				if (ma1.matches()){
					spouse = ma1.group(2) + ", " + ma1.group(1) + " " + ma1.group(3);
				}
			}
			if (spouse.contains(",")){	
				a = StringFormats.parseNameNashville(spouse);
				fixLFMparser(a);								//JONES,GEROGE R&MILLER JONES,LESLIE
			} else {
				a = StringFormats.parseNameDesotoRO(spouse);
			}
			addOwnerNames(a, spouseSuffix, "", NameUtils.isCompany(spouse), false, body);
		}
		String prevLast = a[2];	       
    	if (others.length() != 0){
    		others = others.replaceAll("\\b([A-Z]+&[A-Z]+) ([A-Z'-]+)\\b(?=&|$)", "$2,$1");    		        		
			if (!others.matches("[A-Z]+( [A-Z])? [A-Z'-]{2,}.*") && !others.contains(prevLast)){
				others = prevLast + " " + others;
				a = StringFormats.parseNameNashville(others);
			} else {
				a = StringFormats.parseNameDesotoRO(others);
			}
			prevLast = a[2];
			suffixes = extractNameSuffixes(a);
			addOwnerNames(a, suffixes[0], suffixes[1], NameUtils.isCompany(others), false, body);
    	}
	    if (last.length() != 0){
	    	last = last.replaceAll("\\b([A-Z]+&[A-Z]+(?: [A-Z])?) ([A-Z'-]+)(?=&|$)", "$2,$1"); // SMITH,ALBERT&JOAN M ALBERT&JOAN SMI
	    	if (last.matches("[A-Z]+&[A-Z]+")){
	    		a[0] = ""; a[1] = ""; a[2] = last;
	    		a[3] = ""; a[4] = ""; a[5] = "";
	    	} else {
		    	if (last.matches("[A-Z]+( [A-Z])? [A-Z'-]{2,}")){
		    		a = StringFormats.parseNameDesotoRO(last);
		    	} else {
		    		a = StringFormats.parseNameNashville(last);
		    		fixLFMparser(a);  // MILLER,JACK M&MILLER ROJAS,LETICIA
		    	}
	    	}
	    	suffixes = extractNameSuffixes(a);
			addOwnerNames(a, suffixes[0], suffixes[1], NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
	    }
    	
	    storeOwnerInPartyNames(m, body);  
	}
   
	public static void emptyRecordedDateDASLDT(ResultMap m, long searchId) throws Exception {

		String recDate = (String) m.get("SaleDataSet.RecordedDate");
		String recDate1 = (String) m.get("SaleDataSet.RecordedDate1");
		String instrDate = (String) m.get("SaleDataSet.InstrumentDate");

		if (StringUtils.isEmpty(recDate)) {
			recDate = recDate1;
		}

		if (StringUtils.isEmpty(recDate) && StringUtils.isEmpty(instrDate)) {
			m.put("SaleDataSet.RecordedDate", "01/01/1960");
			m.put("SaleDataSet.InstrumentDate", "01/01/1960");
		} else if (StringUtils.isEmpty(recDate)) {
			m.put("SaleDataSet.RecordedDate", instrDate);
		} else if (StringUtils.isEmpty(instrDate)) {
			m.put("SaleDataSet.InstrumentDate", "*");
		}
	}
	
	
	public static void partyNamesCAContraCostaTaxDT(ResultMap m, long searchId) throws Exception {
		String name = (String) m.get("tmpOwnerName");
		if (name == null || name.length() == 0)
			return;
		
		partyNamesTokenizerCAContraCostaTaxDT(m, name); 
	}
	
	public static void partyNamesTokenizerCAContraCostaTaxDT(ResultMap m, String s) throws Exception{
		
		// cleanup
		s = s.replaceFirst("\\s*\\bTRE\\s*$", "");
		s = s.replaceFirst("\\s*\\bEST(ATE)? OF\\s*$", "");
		s = s.replaceAll("\\bEST(ATE)?( OF)?\\b", "");		
		s = s.replaceAll("/", "&");
	    s = s.replaceAll("\\s{2,}", " ").trim();
	    		    
	    // parse first entity as L F M
	    String[] a = {"", "", "", "", "", ""}; 
        String[] addOwners;
        String[] suffixes;
        List<List> body = new ArrayList<List>();
        Pattern p = Pattern.compile("(.+?)\\s*&\\s*([^&]+)&?(.*)");
	    String owner, spouse = "", others = "";
    	Matcher ma1 = p.matcher(s);
    	if (ma1.matches()){
    		owner = ma1.group(1);
    		spouse = ma1.group(2).trim();
    		others = ma1.group(3).trim();
    	} else {
    		owner = s;
    	}
    	boolean ownerIsCompany = false;
    	if (NameUtils.isCompany(owner)){        		 
        	if (NameUtils.isCompany(spouse)){
        		a[2] = s;
        		addOwnerNames(a, "", "", true, false, body);
        		ownerIsCompany = true;
        		spouse = "";
        	} else {
        		a[2] = owner;
            	addOwnerNames(a, "", "", true, false, body);  
            	ownerIsCompany = true;
        	}
    	} else if (NameUtils.isCompany(s)){
    		a[2] = s; 
        	addOwnerNames(a, "", "", true, false, body);
        	ownerIsCompany = true;
        	spouse = "";
    	}
    	String spouseSuffix = "";
		if (spouse.length() != 0){
			Matcher ma2 = nameSuffix.matcher(spouse);
			if (ma2.matches()){		// spouse has suffix => remove it 
    			spouse = ma2.group(1).trim();
    			spouseSuffix = ma2.group(2);
    		} 
		}		
    	if (!ownerIsCompany){
    		if (spouse.matches("[A-Z]+( [A-Z])?") || spouse.matches("[A-Z]+ [A-Z]+ [A-Z]")){
    			a = StringFormats.parseNameNashville(owner + " & " + spouse);
    			spouse = "";
    		} else {
    			a = StringFormats.parseNameNashville(owner);
        		if (spouse.matches("[A-Z]+ [A-Z]+")){
        			spouse =  spouse + " " + a[2];
        		}    		
    		}
		
    		suffixes = extractNameSuffixes(a);
    		if (spouseSuffix.length() != 0 && spouse.length() == 0){
    			suffixes[1] = spouseSuffix;
    		}
			addOwnerNames(a, suffixes[0], suffixes[1], false, false, body);
    	}
		if (spouse.length() != 0){		    		       		        	
			a = StringFormats.parseNameDesotoRO(spouse);				
			addOwnerNames(a, spouseSuffix, "", NameUtils.isCompany(spouse), false, body);
		}
		String prevLast = a[2];	       
    	if (others.length() != 0){
    		addOwners = others.trim().split("\\s?&\\s?");
    		for (int j=0; j<addOwners.length; j++){        			
    			if (!addOwners[j].matches("[A-Z'-]+ [A-Z]+ [A-Z'-]+.*") && !addOwners[j].contains(prevLast)){
    				addOwners[j] = prevLast + " " + addOwners[j];
    			}
    			a = StringFormats.parseNameNashville(addOwners[j]);
    			prevLast = a[2];
    			suffixes = extractNameSuffixes(a);
    			addOwnerNames(a, suffixes[0], suffixes[1], NameUtils.isCompany(addOwners[j]), false, body);
    		}
    	}
	    
	    storeOwnerInPartyNames(m, body);  
	}
	
	private static final Pattern PAT_BOOK_PAGES_TS = Pattern.compile("\\b(\\d{1,6})/(\\d{1,6}\\b)");
	public static void setInfoFromCommentDT(ResultMap m, long searchId) throws Exception {  // CR #2906
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();	
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		
		if("TX".equals(crtState)||"AR".equals(crtState)){
			String remarks = (String)m.get("tmpInstrumentRemarks");
			if(remarks!=null){
				remarks +=" ";
				remarks = remarks.replaceAll("(?is)UNI?DI?V?(?:IDED)? \\d+/\\d+ INT(?:EREST)?", "");
				Matcher mat = PAT_BOOK_PAGES_TS.matcher(remarks);
				while(mat.find()){
					m.put("CrossRefSet.Book", mat.group(1));
					m.put("CrossRefSet.Page", mat.group(2));
					break;
				}
			}
		}
		
		if ("CA".equals(crtState)){
			
			if ("San Francisco".equals(crtCounty)) { // B 3527
				ResultTable crossRefs = (ResultTable) m.get("CrossRefSet");
				String crossRefBook = "";
				if (crossRefs != null){
					String[][] body = crossRefs.body;
					for (int i = 0; i < body.length; i++){
						crossRefBook = body[i][0];
						crossRefBook = crossRefBook.replaceAll("([A-Z])0+(\\d+)", "$1$2");
						body[i][0] = crossRefBook;
					}
				}
				String saleDataSetBook = (String) m.get("SaleDataSet.Book");
				if (saleDataSetBook != null) {
					saleDataSetBook = saleDataSetBook.replaceAll("([A-Z])0+(\\d+)", "$1$2");
					m.put("SaleDataSet.Book", saleDataSetBook);
				}
				String coment = (String)m.get("tmpComment");
				if (StringUtils.isEmpty(coment))
					return;	
				if (coment.matches("[A-Z]\\d+\\s+\\d+")) {
					if (crossRefs == null ) {
						m.put("CrossRefSet.Book", coment.replaceFirst("(.+)\\s+(.+)", "$1"));
						m.put("CrossRefSet.Page", coment.replaceFirst("(.+)\\s+(.+)", "$2"));
					}
				}
			} else if ("ContraCosta".equals(crtCounty)) {//B 3562
				String comment = (String)m.get("tmpComment");
				if (StringUtils.isEmpty(comment))
					return;
				
				if (comment.matches("\\d+\\s+\\d+")) { 
					m.put("CrossRefSet.Book", comment.replaceFirst("(.+)\\s+(.+)", "$1"));
					m.put("CrossRefSet.Page", comment.replaceFirst("(.+)\\s+(.+)", "$2"));
				}
			}
				// cleanup
				String comment = (String)m.get("tmpComment");
				if (StringUtils.isEmpty(comment))
					return;
				
				comment = comment.replaceFirst("^(COR|XXX|DOC ER|NO LEGAL|REREC|OOC).*", "");
				if (comment.length() == 0)
					return;
				
				String docType = (String)m.get("SaleDataSet.DocumentType");
				if (StringUtils.isEmpty(docType))
					return;
		   
				if (DocumentTypes.getDocumentSubcategory(docType, searchId).equals(DocumentTypes.COURT) || 
						docType.equals("AM") || docType.equals("RL") || docType.equals("PL")){
					String instrNo = (String) m.get("SaleDataSet.InstrumentNumber");
					if (StringUtils.isEmpty(instrNo)){		// fix for bug #3185
						m.put("CourtDocumentIdentificationSet.CaseNumber", comment);
						m.put("SaleDataSet.InstrumentNumber", comment);
					}
				} else if (DocumentTypes.checkDocumentType(docType, DocumentTypes.MORTGAGE_INT, null, searchId)){
					if (comment.matches("[$\\d\\.,]+")){
						//m.put("SaleDataSet.MortgageAmount", comment.replaceFirst("^[A-Z]+", ""));
						m.put("SaleDataSet.MortgageAmount", "0.00");
					}
				}
		}
		/**this part has been replaced by GenericFunctions.legalFromCommentFLDT. There was a request to parse only
		 * cross references mortgage amount and apn from comment. see bug <a>http://ldap.cst.ro/bugzilla/show_bug.cgi?id=4200</a>  
		if ("FL".equals(crtState)) {
				if ("Miami-Dade".equals(crtCounty)){
					parseLegalFromCommentMiamiDadeDT(m, searchId);
				} else if ("Orange".equals(crtCounty)){
					parseLegalFromCommentOrangeDT(m, searchId);
				} else if ("Brevard".equals(crtCounty)){
					parseLegalFromCommentBrevardDT(m, searchId);
				} 
		}
		*/
	}	
		private static void parseLegalFromCommentBrevardDT(ResultMap m, long searchId) throws Exception {
		
			String coment = (String) m.get("tmpComment");
			if (StringUtils.isEmpty(coment))
				return;
			
			String lot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			   Pattern p = Pattern.compile("(?is)\\b(LTS?|LOTS?)\\s+([\\d,-]+)\\b");
			   Matcher ma = p.matcher(coment);
			   if (StringUtils.isEmpty(lot)) {
				   lot = "";
				   while (ma.find()){
					   lot = lot + " " + ma.group(2);
					   }
				   if (!StringUtils.isEmpty(lot)){
					   lot = lot.replaceAll("\\s*&\\s*", " ").trim();
					   lot = LegalDescription.cleanValues(lot, false, true);
					   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
				   }
			   }
			   
			   // extract cross refs from comment
			   List<List> bodyCR = new ArrayList<List>();

			   p = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*[A-Z]+\\s*-\\s*(\\d+)\\b"); // 2006-CP-008624, 2006-DR-045852
			   ma = p.matcher(coment);
			   while (ma.find()) {
				   List<String> line = new ArrayList<String>();
					   line.add("");
					   line.add("");
				   if(ma.group(1)==null && ma.group(2)==null)
					   line.add("");
				   else 
					   line.add("(" + ma.group(1) + ")" + ma.group(2));
				   line.add("O");
				   bodyCR.add(line);
			   }
			   ma.reset();
			   p = Pattern.compile("(?is)\\b(\\d{8})\\b"); // 19991216, 20051109
			   ma = p.matcher(coment);
			   while (ma.find()) {
				   List<String> line = new ArrayList<String>();
				   	   line.add("");
					   line.add("");
				   if(ma.group(1)==null)
					   line.add("");
				   else
					   line.add(ma.group(1));
				   line.add("O");
				   bodyCR.add(line);
			   }
			   saveCRInMap(m, bodyCR);
	}

		private static void parseLegalFromCommentOrangeDT(ResultMap m, long searchId) throws Exception {
			String coment = (String) m.get("tmpComment");
			if (StringUtils.isEmpty(coment))
				return;
				
			coment = coment.replaceAll("\\b(REF)\\b;", "$1");
			coment = coment.replaceAll("\\s*;\\s*", ",");
			coment = coment.replaceAll("\\s*,,\\s*", ",");
			coment = coment.replaceAll("(TRACTS);\\s*", "$1 ");
			coment = coment.replaceAll("(?is)(\\d{4,})\\s*-\\s*(\\d+)", " $1-$2");
			   
			String lot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			   Pattern p = Pattern.compile("(?is)\\b(LTS?|LOTS?)\\s*([\\d,-]+)\\b");
			   Matcher ma = p.matcher(coment);
			   if (StringUtils.isEmpty(lot)) {
				   lot = "";
				   while (ma.find()){
					   lot = lot + " " + ma.group(2);
					   }
				   if (!StringUtils.isEmpty(lot)){
					   lot = lot.replaceAll("\\s*&\\s*", " ").trim();
					   lot = LegalDescription.cleanValues(lot, false, true);
					   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
				   }
			   }
			   
			   String sec = (String) m.get("PropertyIdentificationSet.SubdivisionSection");
			   p = Pattern.compile("\\b(SECTION)\\s+([\\d,]+)\\b");
			   ma = p.matcher(coment);
			   if (StringUtils.isEmpty(sec)) {
				   sec = "";
				   while (ma.find()){
					   sec = sec + " " + ma.group(2);
				   }
				   if (!StringUtils.isEmpty(sec)){
					   sec = sec.replaceAll("\\s*&\\s*", " ").trim();
					   sec = LegalDescription.cleanValues(sec, false, true);
					   m.put("PropertyIdentificationSet.SubdivisionSection", sec);
				   }
			   }
			   
			   String unit = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			   p = Pattern.compile("\\b(UNIT)\\s+(\\d+)\\b");
			   ma = p.matcher(coment);
			   if (StringUtils.isEmpty(unit)) {
				   unit = "";
				   while (ma.find()){
					   unit = unit + " " + ma.group(2);
				   }
				   if (!StringUtils.isEmpty(unit)){
					   unit = unit.replaceAll("\\s*&\\s*", " ").trim();
					   unit = LegalDescription.cleanValues(unit, false, true);
					   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
				   }
			   }
			   
			   String tract = (String) m.get("PropertyIdentificationSet.SubdivisionTract");
			   p = Pattern.compile("(?is)\\b(TRA?CTS?)\\s+([\\dA-Z,]+)\\b");
			   ma = p.matcher(coment);
			   if (StringUtils.isEmpty(tract)) {
				   tract = "";
				   while (ma.find()){
					   tract = tract + " " + ma.group(2);
				   }
				   if (!StringUtils.isEmpty(tract)){
					   tract = tract.replaceAll("\\s*&\\s*", " ").trim();
					   tract = LegalDescription.cleanValues(tract, false, true);
					   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
				   }
			   }
			   
			   String bldg = (String) m.get("PropertyIdentificationSet.SubdivisionBldg");
			   p = Pattern.compile("(?is)\\b(BLDGS?)\\s+([\\d-\\s]+)\\b");
			   ma = p.matcher(coment);
			   if (StringUtils.isEmpty(bldg)) {
				   bldg = "";
				   while (ma.find()){
					   bldg = bldg + " " + ma.group(2);
				   }
				   if (!StringUtils.isEmpty(bldg)){
					   bldg = bldg.replaceAll("\\s*&\\s*", " ").trim();
					   bldg = LegalDescription.cleanValues(bldg, false, true);
					   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
				   }
			   }
			  
			   String phase = (String) m.get("PropertyIdentificationSet.SubdivisionPhase");
			   p = Pattern.compile("(?is)\\b(PHASE)\\s+(\\d+)\\b");
			   ma = p.matcher(coment);
			   if (StringUtils.isEmpty(phase)) {
				   phase = "";
				   while (ma.find()){
					   phase = phase + " " + ma.group(2);
				   }
				   if (!StringUtils.isEmpty(phase)){
					   phase = phase.replaceAll("\\s*&\\s*", " ").trim();
					   phase = LegalDescription.cleanValues(phase, false, true);
					   m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
				   }
			   }
			   List<List> bodyCR = new ArrayList<List>();
			   coment = coment.replaceAll("\\b0+(\\d+)", "$1");
			   //p = Pattern.compile("(?is)(?:REF|AMENDS)\\s+(\\d+)\\s*-\\s*(\\d+),?(?:(\\d+)\\s*-\\s*(\\d+))?");
			   p = Pattern.compile("(?is)(\\d{4,})\\s*-\\s*([\\d,]+)");
			   ma = p.matcher(coment);	 
			   
			   while (ma.find()){
				   String book = ma.group(1);
				   String page = ma.group(2);
				   if (page == null){
					   page = "";
				   }
				   //we might have 3319-2298,2318,2312,;  3441-2343,2342,2344, 3507-2751,;
				   page = page.trim();
				   page = page.replaceAll("\\s*,\\s*", ",");
				   String[] pages = page.split(",|\\s");
				   ResultTable crossRefs = (ResultTable) m.get("CrossRefSet");
				   String[][] body = null;
					if (crossRefs != null){
						body = crossRefs.body;
					
					   for (int jj = 0; jj< pages.length; jj++){
						   for (int i = 0; i < body.length; i++){
							   if (!body[i][0].equals(book) && (pages[jj].equals("") || !body[i][1].equals(pages[jj]))){
								   List<String> line = new ArrayList<String>();
								   line.add(book);
							   	   line.add(pages[jj]);
							   	   line.add("");
							   	   bodyCR.add(line);
							   }		
						   }
					   }
					} else {
						 for (int jj = 0; jj< pages.length; jj++){
							      List<String> line = new ArrayList<String>();
									   line.add(book);
								   	   line.add(pages[jj]);
								   	   line.add("");
								   	   bodyCR.add(line); 
						  }
					}
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
			 		
			 		String block = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
				   p = Pattern.compile("(?is)\\b(BLKS?|BLOCKS?)\\s+([\\dA-Z]+)\\b");
				   ma = p.matcher(coment);
				   if (StringUtils.isEmpty(block)) {
					   block = "";
					   while (ma.find()){
						   block = block + " " + ma.group(2);
					   }
					   if (!StringUtils.isEmpty(block)){
						   block = block.replaceAll("\\s*&\\s*", " ").trim();
						   block = LegalDescription.cleanValues(block, false, true);
						   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
					   }
				   }
		
	}

		public static void parseLegalFromCommentMiamiDadeDT(ResultMap m, long searchId) throws Exception {
			
			String coment = (String) m.get("tmpComment");
			if (StringUtils.isEmpty(coment))
				return;
			
			String lot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
				coment = coment.replaceAll("PTLT", "");
				coment = coment.replaceAll(" THRU ", "-");
				coment = coment.replaceAll("(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
				coment = coment.replaceAll("(?is)(LT\\d+,)\\s*(\\d{4,})", "$1 ; $2");

				coment = coment.replaceAll("(\\d+)THRU(\\d+)", "$1-$2");
				coment = coment.replaceAll("(LTS\\d+-\\d+)", "$1 ");
				coment = coment.replaceAll("\\b(LT[\\d-]+)(BL?K\\d+)", "$1 $2");
			   Pattern p = Pattern.compile("\\b((?:OF)?(?:LT)S?\\s*|(?:\\A|\\s)L\\s+|LOTS?\\s+)([\\d-&,\\s]+)\\b");

			   Matcher ma = p.matcher(coment);
			   if (StringUtils.isEmpty(lot)) {
				   lot = "";
				   while (ma.find()){
					   lot = lot + " " + ma.group(2);
					   }
				   if (!StringUtils.isEmpty(lot)){
					   lot = lot.replaceAll("\\s*&\\s*", " ").trim();
					   lot = LegalDescription.cleanValues(lot, false, true);
					   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
				   }
			   }
			   ma.reset();
			   coment = ma.replaceAll("");
			   String sec = (String) m.get("PropertyIdentificationSet.SubdivisionSection");
			   p = Pattern.compile("\\b(SEC)\\s+([\\d,]+)\\b");
			   ma = p.matcher(coment);
			   if (StringUtils.isEmpty(sec)) {
				   sec = "";
				   while (ma.find()){
					   sec = sec + " " + ma.group(2);
				   }
				   if (!StringUtils.isEmpty(sec)){
					   sec = sec.replaceAll("\\s*&\\s*", " ").trim();
					   sec = LegalDescription.cleanValues(sec, false, true);
					   m.put("PropertyIdentificationSet.SubdivisionSection", sec);
				   }
			   }
			   
			   String unit = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			   p = Pattern.compile("\\b(UNITS?\\s+|U\\s*)((?:\\d+)?(?:[A-Z])?)\\b");
			   ma = p.matcher(coment);
			   if (StringUtils.isEmpty(unit)) {
				   unit = "";
				   while (ma.find()){
					   unit = unit + " " + ma.group(2);
				   }
				   if (!StringUtils.isEmpty(unit)){
					   unit = unit.replaceAll("\\s*&\\s*", " ").trim();
					   unit = LegalDescription.cleanValues(unit, false, true);
					   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
				   }
			   }
			   
			   String tract = (String) m.get("PropertyIdentificationSet.SubdivisionTract");
			   p = Pattern.compile("\\b(TR(?:ACT)?)\\s+([A-Z])\\b");
			   ma = p.matcher(coment);
			   if (StringUtils.isEmpty(tract)) {
				   tract = "";
				   while (ma.find()){
					   tract = tract + " " + ma.group(2);
				   }
				   if (!StringUtils.isEmpty(tract)){
					   tract = tract.replaceAll("\\s*&\\s*", " ").trim();
					   tract = LegalDescription.cleanValues(tract, false, true);
					   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
				   }
			   }
				// extract building # from legal description
				p = Pattern.compile("\\bB(?:UI)?LD(?:IN)?G ?([A-Z0-9]+)\\b");
				String building = (String) m
						.get("PropertyIdentificationSet.SubdivisionBldg");
				if (building == null) {
					building = "";
				}
				ma = p.matcher(coment);
				if (ma.find()) {
					building += " " + ma.group(1);
					m.put("PropertyIdentificationSet.SubdivisionBldg", building);
				}
				ma.reset();
			   coment = ma.replaceAll("");
				//phase
				// extract phase from legal description
				String phase = (String) m
						.get("PropertyIdentificationSet.SubdivisionPhase");
				if (phase == null) {
					phase = "";
				}
				p = Pattern.compile("\\bPH(?:ASES?)?(( ?(\\d+\\w?|\\b[A-Z]\\b))+)\\b");
				ma = p.matcher(coment);
				if (ma.find()) {
					phase += " " + ma.group(1).trim();
					phase = LegalDescription.cleanValues(phase, false, true);
					m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
				}
			   ma.reset();
			   coment = ma.replaceAll("");
				
			   //pin COMMENT:3011340011000
			   p = Pattern.compile("(\\d{12,})");
			   ma = p.matcher(coment);
			   if (ma.find()){
				   m.put("PropertyIdentificationSet.ParcelID", ma.group(1));
			   }
			   
			   //cross refs
			   List<List> bodyCR = new ArrayList<List>();
			   coment = coment.replaceAll("\\bLNORB\\b", "LN ORB");
			   coment = coment.replaceAll("\\bRR|ORNB\\b", "ORB");
			   //coment = coment.replaceAll("\\b\\d+ \\d+ \\d+\\b", "");
			   p = Pattern.compile("\\bO(?:RB?)?\\s*(\\d+)\\s+(\\d+)\\s*,\\s*(\\d+) (\\d+)\\b");
			 	ma = p.matcher(coment);	      	   
			 	while (ma.find()){	 			
			 		List<String> line = new ArrayList<String>();
		 			line.add(ma.group(1));
		 			line.add(ma.group(2));
		 			line.add("");
			 		bodyCR.add(line);	
			 		line = new ArrayList<String>();
		 			line.add(ma.group(3));
		 			line.add(ma.group(4));
		 			line.add("");
			 		bodyCR.add(line);
			 	} 
			 	ma.reset();
			 	coment = ma.replaceAll("");
			   p = Pattern.compile("\\bO(?:RB?,?)?\\s*(\\d+)\\s+(\\d+)\\b");
			 	ma = p.matcher(coment);	      	   
			 	while (ma.find()){	 			
			 		List<String> line = new ArrayList<String>();
		 			line.add(ma.group(1));
		 			line.add(ma.group(2));
		 			line.add("");
			 		bodyCR.add(line);	 		
			 	} 	
			 	String tmpComment = ma.replaceAll("_!_!_");
			 	p = Pattern.compile("^(\\d+) (\\d+)$");
			 	ma = p.matcher(tmpComment);
			 	while (ma.find()){
			 		List<String> line = new ArrayList<String>();
		 			line.add(ma.group(1));
		 			line.add(ma.group(2));
		 			line.add("");
			 		bodyCR.add(line);			 		
			 	}
			 	p = Pattern.compile("\\bRERE(\\d+)/(\\d+)\\b");
			 	tmpComment = ma.replaceAll("_!_!_");
			 	ma = p.matcher(tmpComment);
			 	while (ma.find()){
			 		List<String> line = new ArrayList<String>();
		 			line.add(ma.group(1));
		 			line.add(ma.group(2));
		 			line.add("");
			 		bodyCR.add(line);			 		
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
			 		coment = coment.replaceAll("\\bORB|RB\\b", "");
			 		String block = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
				   p = Pattern.compile("(BLK\\s*|\\bB\\s+|\\bBL\\s*|\\bBK\\s*)(\\d+(?:[A-Z])?)\\b");
				   ma = p.matcher(coment);
				   if (StringUtils.isEmpty(block)) {
					   block = "";
					   while (ma.find()){
						   block = block + " " + ma.group(2);
					   }
					   if (!StringUtils.isEmpty(block)){
						   block = block.replaceAll("\\s*&\\s*", " ").trim();
						   block = LegalDescription.cleanValues(block, false, true);
						   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
					   }
				   }
		}
	
	public static void setInstrumentDT(ResultMap m, long searchId) throws Exception {
		
		String instNo = (String)m.get("SaleDataSet.InstrumentNumber");
		String book = (String)m.get("SaleDataSet.Book");
		String page = (String)m.get("SaleDataSet.Page");
		
		if(!StringUtils.isEmpty(instNo)&&instNo.contains("_")){
			String a[] = instNo.split("_");
			if(a.length>=2){
				if(a[0].equalsIgnoreCase(book) && a[1].equalsIgnoreCase(page)){
					m.put("SaleDataSet.InstrumentNumber", "");
				}
			}
		} else {
			if(StringUtils.isNotEmpty(instNo) && StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page))	{
				instNo = org.apache.commons.lang.StringUtils.leftPad(instNo, 8, "0");
				book = org.apache.commons.lang.StringUtils.leftPad(book, 4, "0");
				page = org.apache.commons.lang.StringUtils.leftPad(page, 4, "0");
				if(instNo.length() == 8 && book.length() == 4 && instNo.equals(book + page) ){
					m.put("SaleDataSet.InstrumentNumber", "");
				}
			}
		}
		
		if (StringUtils.isNotEmpty(instNo)) {
			// Bug 8018: ignore book-page 1-1 when instrument nr is valid
			try {
				if (Integer.parseInt(book) == 1 && Integer.parseInt(page) == 1) {
					m.remove("SaleDataSet.Book");
					m.remove("SaleDataSet.Page");
				}
			} catch(Exception e) {}
		}
		
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String docType = (String) m.get("SaleDataSet.DocumentType");
		if ("AK".equals(crtState)){
			if (StringUtils.isNotEmpty(docType)){
				if (DocumentTypes.checkDocumentType(docType, DocumentTypes.PLAT_INT, null, searchId)){
					if (StringUtils.isEmpty(book) && StringUtils.isEmpty(page) && StringUtils.isNotEmpty(instNo)){
						if (instNo.contains("-")){
							String[] bp = instNo.split("-");
							if (bp.length > 1){
								m.put("SaleDataSet.Book", bp[0]);
								m.put("SaleDataSet.Page", bp[1]);
								m.put("SaleDataSet.InstrumentNumber", "");
							}
						}
					}
				}
			}
		} else if ("CA".equals(crtState)){
			
			if ("Los Angeles".equals(crtCounty)){
				Object starter = m.get("tmpStarterDoc");
				if (starter == null){
					String instr = (String)m.get("SaleDataSet.InstrumentNumber");
					if (StringUtils.isEmpty(instr))
						return;
					
					instr = instr.replaceAll("\\s", "");
					instr = instr.replaceFirst("^[^\\d]+", "");
					m.put("SaleDataSet.InstrumentNumber", instr);
				}	
				
				ResultTable crossRefs = (ResultTable) m.get("CrossRefSet");
				String crossRef = "";
				if (crossRefs != null) {
					String[][] body = crossRefs.body;
					for (int i = 0; i < body.length; i++){
						crossRef = body[i][2];
						crossRef = crossRef.replaceAll("[A-Z]\\s*(.+)", "$1");
						body[i][2] = crossRef;
					}
				}
			}
		}
	}
	
	public static void correctSubdivisionNameForTS(ResultMap m, long searchId) throws Exception{
		String addition = (String)m.get("PropertyIdentificationSet.Addition");
		String subdivName = (String)m.get("PropertyIdentificationSet.SubdivisionName");
		String subdivParcel = (String)m.get("PropertyIdentificationSet.SubdivisionParcel");
		
		if(StringUtils.isEmpty(addition)&&StringUtils.isEmpty(subdivName)){
			m.put("PropertyIdentificationSet.SubdivisionName", subdivParcel);
		}
		String subdivisionName = (String) m.get("PropertyIdentificationSet.SubdivisionName");
		if(StringUtils.isNotEmpty(subdivisionName)) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", LegalDescription.extractPhaseFromText(subdivisionName));
		}
		
	}
	
	public static void extractDataFromAdditionDT(ResultMap m, long searchId) throws Exception{
		String addition = (String)m.get("PropertyIdentificationSet.Addition");
    	try{
    		if(addition!=null && !StringUtils.isEmpty(addition)){
    			Matcher mat = Pattern.compile("([0-9][0-9a-zA-Z]?)-([0-9][0-9a-zA-Z]?)-([0-9][0-9a-zA-Z]?)").matcher(addition);
    			if(mat.find()){
    				m.put("PropertyIdentificationSet.SubdivisionSection", mat.group(1));
    				m.put("PropertyIdentificationSet.SubdivisionTownship", mat.group(2));
    				m.put("PropertyIdentificationSet.SubdivisionRange", mat.group(3));
    			}
    		}
    	}
    	catch(Exception e){}
	}
	
	public static void extractAddressCA_DT(ResultMap m, long searchId) throws Exception{
		String address = (String) m.get("tmpAddress");
		if (StringUtils.isEmpty(address))
			return;
		
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
		
		if ("CA".equals(crtState)){
				String unit = "";
				address = address.replaceAll("(?is)\\b(BEACH)\\s+(CA)", "$1$2");//B 3360
				unit = address.replaceAll("(?is).*\\b(NO|APT)\\s+(\\d+).*", "$2"); // B 3435
				//address = address.replaceAll("\\bLOS ANGELES[^\\d]+(\\d+)", "$1"); //6420 W 85TH PL LOS ANGELES CA 90045
		
				StringTokenizer st = new StringTokenizer(address); // B 3583
				String onlyAddress = "";
				boolean hasSuffix = false;

				while (st.hasMoreTokens()) {
			    	 String token = st.nextToken();
			    	  if (!Normalize.isSuffix(token)){
			    		  if (hasSuffix == false){
			    			  onlyAddress = onlyAddress + " " + token;
			    		  } else 
			    			  break; //745 W CENTER ST POMONA CA 91768 Los Angeles

			    	  } else { 
			    		  onlyAddress = onlyAddress + " " + token;
			    		  hasSuffix = true;
			    	  }
				}
				if (hasSuffix && unit.matches("\\d+")){
					onlyAddress = onlyAddress + " " + unit;
				}
				onlyAddress = onlyAddress.trim();
				onlyAddress = onlyAddress.replaceAll("(?is)\\b(BEACH)(CA)\\b", "$1 $2");//B 3360
				address = address.replaceAll("(?is)\\b(BEACH)(CA)\\b", "$1 $2");//B 3360
				String cityStateZip = address;
				if (address.length() != onlyAddress.length()){
					 cityStateZip = address.substring(onlyAddress.length()).trim();
				}
				
				 if (cityStateZip.matches(".*\\bCA\\b.*")) {
					 String city = cityStateZip.replaceAll("(?is)(?:[\\d\\s]*)?(.+?)\\s+(CA).*", "$1");
					city = city.replaceAll("(?is).*(?:[\\d]+)(.+)", "$1");
					m.put("PropertyIdentificationSet.City", city.trim());
					String zip = cityStateZip.replaceAll("(?is).*(CA)\\s+(.*)", "$2");
					m.put("PropertyIdentificationSet.Zip", zip.trim());
				 }
				
				onlyAddress = onlyAddress.replaceAll("(?is)\\b(.+)\\s+APT\\s+(\\d+).*", "$1 $2");//B 3360

				m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(onlyAddress));
				m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(onlyAddress));
		} else {
			m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address));
			m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address));
		}
	}

	@SuppressWarnings("rawtypes")
	public static void legalFromCommentFLDT(ResultMap m, long searchId) throws Exception {
			String comment = (String) m.get("tmpComment");
			comment = comment == null ? "" : comment;
			comment=comment.replace(";", ",");
			comment=comment.replace("&", ",");
			comment=comment.replaceAll("(?is),;\\s+", ", ");
			comment=comment.replaceAll("(?is)(\\d+)\\s+AND\\s+(\\d+)", "$1, $2");
			comment=comment.replaceAll("(?is)(BLKS\\s+[A-Z])\\s+AND\\s+([A-Z])", "$1, $2");
			comment=comment.replaceAll("-(\\s*[A-Z]\\d+)", "@$1");
			comment=comment.replaceAll("\\s*PA?GE?\\s*", "-");
			//these needs for 5571
			String lot = extractLotFromComment(comment);	
			String block = extractFromComment(comment,"\\b(?i)BLO?C?K?S? ([\\d\\s,-]+[A-Z]?(\\d+)*(?!-)|[A-Z](?:\\s*,\\s+[A-Z])?)\\b");
			String unit = extractFromComment(comment,"(?i)\\bUNIT\\s+([\\d+[A-Z]?,]+)\\b");
			//
			String instrNoYear = extractFromComment(comment,"(?i)(CASE NO)\\s*(\\d{2,4}[/|\\s|_|-]\\d{2,5})\\b",2);
			comment = comment.replaceAll("(?i)(CASE NO)\\s*(\\d{2,4}[/|\\s|_|-]\\d{2,5})\\b", "");
			
//			String tract= extractFromComment(comment,"(?i)\\bTRACTS\\s+([\\d+[A-Z]?,]+)\\b"); 
			String mortgageAmount = extractFromComment(comment,"(?<=\\$)[\\d\\.,]+",0);
			
			if (StringUtils.isEmpty((String) m.get("SaleDataSet.MortgageAmount"))){
				m.put("SaleDataSet.MortgageAmount", mortgageAmount);
			}
			
			if(StringUtils.isNotEmpty(lot)) {
				lot = lot.replaceAll("\\s\\d{4,}-\\d+(-\\s)?", " ");
				lot = LegalDescription.cleanValues(lot, false, true);
				if(StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionLotNumber"))){
					if (StringUtils.isNotEmpty(lot)){
						m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
					}
				}
			}
			if(StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionBlock"))){
				if (StringUtils.isNotEmpty(block)){
					m.put("PropertyIdentificationSet.SubdivisionBlock", block);
				}
			}
			if(StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionUnit"))){
				if (StringUtils.isNotEmpty(unit)){
					m.put("PropertyIdentificationSet.SubdivisionUnit", unit.replace(",", " "));
				}
			}
		
			String refset = "";
			
			String docType = (String)m.get("SaleDataSet.DocumentType");
			if (StringUtils.isNotEmpty(docType) 
					&& (DocumentTypes.isJudgementDocType(docType, searchId, false) 
							|| "LIS PENDENS".equalsIgnoreCase(DocumentTypes.getDocumentSubcategory(docType, null,searchId, false)))){
				//if modify this regex, please verify testcases from TSEARCHALPHA/unit_tests/ro/cst/tsearch/servers/functions/FLCommentDT_legals.txt
				refset = extractFromComment(comment, "(?i)\\b([A-Z]{0,2}(?:\\s*[A-Z]{0,2})?(?:[\\d/]+)?[\\d\\s/-]{3,}(?:\\s*[A-Z]{1,5}\\s*(?:[\\d\\s/-]+)?(?:[A-Z]\\d+)?)?|\\d+[\\dA-Z]+(?:\\s+[A-Z]+)?)\\b", 1);
				
				refset = refset.replaceAll("(?is)FCL\\s*$", "");
				refset = refset.replaceAll("(?is)P\\d+\\s*$", "");
				m.put(OtherInformationSetKey.REMARKS.getKeyName(), refset);
			} else {
				refset = extractFromComment(comment,"(?i)(ORB?(?:\\s+BK)?|REF|PR|\\s|,|\\b)(\\d{3,5}[/|\\s|_|-]\\d{2,5})\\b",2);// (?i)\\bREF\\s+(\\d+-\\d+)\\b//(?i)\\bREF\\s+([\\d+-[A-Z]?,]+)\\b  //(?i)(ORB|REF|PR)?\\s?(\\d{3,5}[\\s|_|-]\\d{3,5})
				
				List<List> bodyCR = new ArrayList<List>();
				ResultTable currentCr = (ResultTable) m.get("CrossRefSet");
				
				List<String> line = null;
				if (StringUtils.isNotEmpty(refset)) {
					String[] split = refset.split("\\s|_|/|-");
					if (split.length%2==0){
						for (int i=0;i<split.length;i++) {
							line = new ArrayList<String>();
							line.add(split[i]);
							i++;
							line.add(split[i]);
							line.add("");
							bodyCR.add(line);
						}
					}
				}
				
				if (StringUtils.isNotEmpty(instrNoYear)) {
					line =new ArrayList<String>();
					line.add("");
					line.add("");
					line.add(instrNoYear);
					bodyCR.add(line);
				}
				ResultTable.appendRefSetToResultMap(m, bodyCR, currentCr);
			}
			
			if(StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionTract"))){
//				m.put("PropertyIdentificationSet.SubdivisionTract", tract.replace(",", " "));
			}
			
			Pattern p = Pattern.compile("P-?(\\d+)-(\\d+)");
			Matcher ma = p.matcher(comment);
			String platBook="";
			String platNo="";
	/*		while(ma.find()){
				platBook =  ma.group(1) + " " + platBook  ;
				platNo =    ma.group(2) + " " + platNo;
			}
			if(StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.PlatBook"))){
				m.put("PropertyIdentificationSet.PlatBook", platBook);
			}
			if(StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.PlatNo"))){
				m.put("PropertyIdentificationSet.PlatNo", platNo);
			}
			*/
			
			List<String> line = new ArrayList<String>();
			List<List> bodySTR = new ArrayList<List>();
			p = Pattern.compile("(?is)\\bSEC\\s*(\\d+)\\s*T\\s*(\\d+[A-Z]?)\\s*R\\s*(\\d+[A-Z]?\\b)");
			ma = p.matcher(comment);
			while (ma.find()){
		    	line = new ArrayList<String>();
		    	line.add(ma.group(1).replaceAll("\\A0+", ""));
		    	line.add(ma.group(2).replaceAll("\\A0+", ""));
		    	line.add(ma.group(3).replaceAll("\\A0+", ""));
		    	bodySTR.add(line);		   
	    	}
			if (!bodySTR.isEmpty()){
				   String [] header = {"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"};
				   
				   Map<String,String[]> map = new HashMap<String,String[]>();
				   map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
				   map.put("SubdivisionTownship", new String[]{"SubdivisionTownship", ""});
				   map.put("SubdivisionRange", new String[]{"SubdivisionRange", ""});
				   
				   ResultTable pis = new ResultTable();	
				   pis.setHead(header);
				   pis.setBody(bodySTR);	
				   pis.setMap(map);		   
				   m.put("PropertyIdentificationSet", pis);
			    }
			
	}
	
	private static String extractFromComment(String comment,String pattern){
		return extractFromComment(comment, pattern, 1);
	}
	/***
	 * Extracts a string from the given comment according to the pattern given. 
	 * The group that should be extracted if there are multiple occurrences. 
	 * @param comment
	 * @param pattern
	 * @param group
	 * @return
	 */
	public static String extractFromComment(String comment,String pattern, int group){		
		String returnValue = "";
		Set<String> values = new HashSet<String>();
		
		String result = ""; // can have multiple occurrences
		Pattern p = Pattern.compile(pattern);
		Matcher ma = p.matcher(comment);
		while (ma.find()){
			values.add(ma.group(group).trim());
		}
		
		returnValue = values.toString();
		returnValue = returnValue.replaceAll("(?is),", "").replaceAll("(?is)\\A\\[", "").replaceAll("(?is)\\]$", "");
		
		return returnValue;
	}
	
	private static String extractLotFromComment(String comment){		
		
			String returnValue = "";
			// extract lot from legal description
			StringBuffer lot = new StringBuffer(); // can have multiple occurrences
			Pattern p = Pattern.compile("\\b(?i)LO?TS? ([\\d\\s,-]+[A-Z]?(\\d+)*(?!-)|[A-Z])\\b");
			Matcher ma = p.matcher(comment);
			while (ma.find()) {
				String lts = ma.group(1).replaceAll("\\s*-\\s*$", "");
				lts = lts.replaceAll("\\s*-\\s*[NSWE]{1,2}[\\d\\.]+\\s*$", "");// LOT 76 - E220, means East 220 feet
				lot.append(" ").append(lts);
			}
			returnValue = lot.toString().trim();
			
		return returnValue;
	}
	
	public static void extractAPNFromComment(ResultMap m, long searchId) throws Exception {
		try{
			String county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			State state = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState();
	
			String comment = (String) m.get("tmpComment");
			comment = comment == null ? "" : comment;
	
			// gather test data
			Testutils.generateTestCase("extractAPNFromCommentTestCase_", county, state.getStateId().toPlainString(), comment);
			
			ParseRules ruleByCountyAndState = ParseRules.getParseRuleByCountyAndState(county, state.getStateAbv());
	
			List<ParseRule> rules = ruleByCountyAndState.getRules();
			Map<String, String> propertyFromPatterns = ParseRules.getPropertyFromPatterns(rules, comment);
			// SearchManager.getSearch(searchId)
			// getPropertyFromPatterns(propertyPatterns, comment);
			String key = "";
			if (propertyFromPatterns.size() >= 1) {
				// key = (String) propertyFromPatterns.entrySet()
				Set<Entry<String, String>> entrySet = propertyFromPatterns.entrySet();
				for (Entry<String, String> entry : entrySet) {
					key = entry.getKey();
				}
			}
	
			String val = (String) propertyFromPatterns.get(key);
			String instrumentNumber = (String) m.get("SaleDataSet.InstrumentNumber");
			val = (val == null ? "" : val);
			instrumentNumber = (instrumentNumber == null ? "" : instrumentNumber);
	
			if (instrumentNumber.length() > 2
					&& (val.contains(instrumentNumber) || comment.contains(instrumentNumber.substring(2)))) {
				val = "";
			}
			if (!key.equals("")) {
				m.put(key, val);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	public static Map<String,String> getPropertyFromPatterns(Map<String,List<String>> propertyPatterns, String comment){
		Map<String,String> results= new HashMap<String,String>();
		Set<String> propertSet = propertyPatterns.keySet();
		boolean alreadyFound = false;
		for (String key : propertSet) {
			List<String> patterns = propertyPatterns.get(key);
			
			for(Iterator<String> iterator = patterns.iterator(); iterator.hasNext()&&!alreadyFound; ){
				String pattern = iterator.next();
				comment = comment.replaceAll("-", "");
				Pattern p = Pattern.compile(pattern);
				Matcher m = p.matcher(comment);
				if (m.find()){
					results.put(key, m.group());
					alreadyFound = true;
				}
			}
		}
		return results;
	}
	
	@SuppressWarnings("rawtypes")
	public static void legalRemarksTXGenericLA(ResultMap m, long searchId) throws Exception{
		String remarks = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if(remarks == null) {
			
			String subdivParcel = (String)m.get("PropertyIdentificationSet.SubdivisionParcel");
			if(StringUtils.isNotEmpty(subdivParcel)) {
				m.put("PropertyIdentificationSet.SubdivisionTract", LegalDescription.extractTractFromText(subdivParcel));
			}
			
			
			return;
		}
		remarks = remarks.replaceAll("(?is)\\bnonhs\\b", "");
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
		if (StringUtils.isNotEmpty(remarks)){
			String secondRemarks = (String) m.get("tmpInstrumentRemarks");
			if (StringUtils.isNotEmpty(secondRemarks) && !remarks.contains(secondRemarks)){
				remarks += " " + secondRemarks;
			}
		} else {
			String secondRemarks = (String) m.get("tmpInstrumentRemarks");
			if (StringUtils.isNotEmpty(secondRemarks) ){
				remarks = secondRemarks;
			}
		}
		m.put("PropertyIdentificationSet.PropertyDescription", remarks);
		if("CO".equals(crtState)) {
			remarks = remarks.replaceAll("\\bC(ASE)?[#\\s]+\\d+\\w*\\s+\\d+\\b", "");
			remarks = remarks.replaceAll("\\bC(ASE)?[#\\s]+\\d+\\w*(-\\w+)?\\b", "");
		} 
		if (StringUtils.isNotEmpty(remarks)){
			String secondRemarks = (String) m.get("tmpInstrumentRemarks");
			if (StringUtils.isNotEmpty(secondRemarks) && !remarks.contains(secondRemarks)){
				remarks += " " + secondRemarks;
			}
			
			
			
			List<List> bodyCR = new ArrayList<List>();
			ResultTable currentCr = (ResultTable) m.get("CrossRefSet");
			boolean isRefSet = currentCr != null ? true : false;
			if (isRefSet) {
				cleanCrossRefsBookTS(m, searchId);
			}
			List<String> line = new ArrayList<String>();
			
			if ("TX".equals(crtState)) {
				if("Brazoria".equals(crtCounty)) {
					
					Pattern specialBrazoriaTXPattern = Pattern.compile("\\b(\\d{2})-(\\d{6})\\b");
					Matcher matcher = specialBrazoriaTXPattern.matcher(remarks);
					
					while(matcher.find()) {
						int year = Integer.parseInt(matcher.group(1));
						if(year < 30) {
							year += 2000;
						} else if(year <= 99) {
							year += 1900;
						}
						line = new ArrayList<String>();
						line.add("");
						line.add("");
						line.add(year + matcher.group(2));
						bodyCR.add(line);
						
						remarks = remarks.replaceFirst(matcher.group(), "");
					}
					
					if(StringUtils.isEmpty(remarks) && !bodyCR.isEmpty()) { 
						ResultTable.appendRefSetToResultMap(m, bodyCR, currentCr);
						return;
					}
				}
			}
			
			
			
			
			//clean remarks from dates 
			remarks = remarks.replaceAll("(?is)\\b(DTD)?\\s*(\\d{1,2})[/|-](\\d{1,2})[/|-](\\d{2,4})", "");
//			remarks = remarks.replaceAll("(?is)D \\d{1,2}-\\d{1,2}-\\d{2,4}", "");
			remarks = remarks.replaceAll("\\w{3} \\d{1,2} \\d{4,4}\\s+#", "");//EX:FEB 15 1978 # 
			remarks = remarks.replaceAll(";HAYS \\d{4,4}", "");//EMMA DIED 11-23-77;HAYS 1949  Guadalupe inst 8800001265
			//for what is this ????     remarks = remarks.replaceAll("(DR)(\\s*)\\d{2,3}/\\d{2,3}", "");
			remarks = remarks.replaceAll("(?is)\\bSEE\\b\\s+DOCS?", "");
			
			//REL'S DT(87/597)/LT 7 O/O SUBD 3			
//			Pattern lotSubd = Pattern.compile("(?is)(?<=LT\\s)(\\d+).*SUBD\\s(\\d+)");
//			Matcher matcher = lotSubd.matcher(remarks); 
//			while (matcher.find()){
//				if (StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionLotNumber"))){
//					m.put("PropertyIdentificationSet.SubdivisionLotNumber", matcher.group(1));	
//				}
//			}
			
			if (StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionLotNumber"))){
				String extractLotFromText = LegalDescription.extractLotFromText(remarks);
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", StringUtils.isEmpty(extractLotFromText)?null:extractLotFromText);	
			}
			if (StringUtils.isEmpty((String)m.get("PropertyIdentificationSet.SubdivisionUnit"))){
				m.put("PropertyIdentificationSet.SubdivisionUnit", LegalDescription.extractUnitFromText(remarks));	
			}
			//REL DT  N PT LT 3 RESUB OF TRACT 3			
//			m.put("PropertyIdentificationSet.SubdivisionTract",
//					getPropertyValueForPattern("(?is)(?<=TRACT\\s)(\\d+)",(String) m.get("PropertyIdentificationSet.SubdivisionTract"),remarks));
			
			m.put("PropertyIdentificationSet.SubdivisionNo",
					RegExUtils.getPropertyValueForPattern("(?is)((?<=SUBDI?V?I?S?I?O?N?\\s\\(?)\\d+)",(String) m.get("PropertyIdentificationSet.SubdivisionNo"),remarks));
			
			//PORTION OF LOT 2 BLOCK NO. 37          *
			
			m.put("PropertyIdentificationSet.SubdivisionBlock",
					RegExUtils.getPropertyValueForPattern("(?is)(?<=BLOCK\\sN?O?.?\\s?)\\d+",(String) m.get("PropertyIdentificationSet.SubdivisionBlock"),remarks));
			//%13.874AC
//			m.put("PropertyIdentificationSet.Acreage",
//					RegExUtils.getPropertyValueForPattern("(?<=%|:)\\d+.?\\d+(?=AC)",(String) m.get("PropertyIdentificationSet.Acreage"),remarks));
		
			m.put("PropertyIdentificationSet.SubdivisionTract", LegalDescription.extractTractFromText(remarks));
			
//			m.put("PropertyIdentificationSet.SubdivisionTract",
//					RegExUtils.getPropertyValueForPattern("(?<=TR)\\d+",(String) m.get("PropertyIdentificationSet.SubdivisionTract"),remarks));
			
			m.put("PropertyIdentificationSet.SubdivisionSection",
					RegExUtils.getPropertyValueForPattern("\\d+(?=SQ)",(String) m.get("PropertyIdentificationSet.SubdivisionSection"),remarks));

			

			Matcher matcher = SEC_TOW_RANGE_PATTERN.matcher(remarks);
			if(matcher.find()) {
				remarks = remarks.replace(matcher.group(0), "");
				m.put("PropertyIdentificationSet.SubdivisionSection",matcher.group(1));
				m.put("PropertyIdentificationSet.SubdivisionTownship",matcher.group(2));
				m.put("PropertyIdentificationSet.SubdivisionRange",matcher.group(3));
			} else {
				String foundSection = (String) m.get("PropertyIdentificationSet.SubdivisionSection");
				if(StringUtils.isEmpty(foundSection) && m.get("tmpSubdivisionSection") != null) {
					m.put("PropertyIdentificationSet.SubdivisionSection", (String)m.get("tmpSubdivisionSection"));
				}
				String foundTownship = (String) m.get("PropertyIdentificationSet.SubdivisionTownship");
				if(StringUtils.isEmpty(foundTownship) && m.get("tmpSubdivisionTownship") != null) {
					m.put("PropertyIdentificationSet.SubdivisionTownship", (String)m.get("tmpSubdivisionTownship"));
				}
				String foundRange = (String) m.get("PropertyIdentificationSet.SubdivisionRange");
				if(StringUtils.isEmpty(foundRange) && m.get("tmpSubdivisionRange") != null) {
					m.put("PropertyIdentificationSet.SubdivisionRange", (String)m.get("tmpSubdivisionRange"));
				}
			}
			
			if(remarks.matches("[^\\d]*")) {
				
				if(!bodyCR.isEmpty()) {
					ResultTable.appendRefSetToResultMap(m, bodyCR, currentCr);
				}
				return;
			}
			
			
			
			matcher = BK_PG_PATTERN.matcher(remarks);
			if(matcher.find()) {
				remarks = remarks.replace(matcher.group(0), "");
				line = new ArrayList<String>();
				line.add(matcher.group(1));
				line.add(matcher.group(2));
				line.add("");
				bodyCR.add(line);
				
			}
			
			//remove acreages
			remarks = remarks.replaceAll("%?\\d+?\\.?\\d+(?= ?AC|%)", "");
			//PT %22-3/4AC DED 247/578			
			remarks = remarks.replaceAll("%\\d{2}-\\d/\\dAC", "");
			//remove SUR  SURV 600 LSE #90633 # 
			remarks = remarks.replaceAll("SURV? \\d+", "");
			
			//WEST 1/2 LOT B
			remarks = remarks.replaceAll("(?is)(EAST|NORTH|SOUTH|WEST|[NSWE]{1,2})\\s*\\d\\s*/\\s*\\d", "");
			//RES NO 24 2007
			remarks = remarks.replaceAll("(?is)\\b(RES\\s+NO\\s*\\d+)\\s+(\\d+)", "$1-$2");
			
			//remove the undivided UND 1/2 INT      UNIDV 3/4 INT DR V186/288
			remarks = remarks.replaceAll("(?is)UNI?DI?V?(?:IDED)? \\d+/\\d+ INT(?:EREST)?", "");
			
			//DT F#66374 V#274/130 F#67049 V#277/623  or D/T 176/763 F#45245  or  D/T 192/340;DR 502/166
			Pattern crossRefPat = Pattern.compile("(?is)[A-Z/]\\s*#\\s*(\\d+)\\s+[A-Z]?\\s*#?\\s*(\\d+)\\s*/\\s*(\\d+)");
			Pattern crossRefPat1 = Pattern.compile("(?is)(\\d+)\\s*/\\s*(\\d+)\\s*[A-Z]\\s*#\\s*(\\d+)^/");
			Matcher mat = crossRefPat.matcher(remarks);
			Matcher mat1 = crossRefPat1.matcher(remarks);
			if (mat.find()){
				mat.reset();
				mat = crossRefPat.matcher(remarks);
				while (mat.find()) {
					line = new ArrayList<String>();
					line.add(mat.group(2).replaceAll("\\A0+", ""));
					line.add(mat.group(3).replaceAll("\\A0+", ""));
					line.add(mat.group(1).replaceAll("\\A0+", ""));
					bodyCR.add(line);
				}
				remarks = remarks.replaceAll(crossRefPat.pattern(), "");
			} else if (mat1.find()){
					mat1.reset();
					mat1 = crossRefPat1.matcher(remarks);
						while (mat1.find()) {
							line = new ArrayList<String>();
							line.add(mat1.group(1).replaceAll("\\A0+", ""));
							line.add(mat1.group(2).replaceAll("\\A0+", ""));
							line.add(mat1.group(3).replaceAll("\\A0+", ""));
							bodyCR.add(line);
						}
					remarks = remarks.replaceAll(crossRefPat1.pattern(), "");
			} 
//			else
			{
				String bookPagePattern = "(?m)(?is)\\b([\\d+]{2,}|--)\\s*/\\s*P?([\\d+]{2,})(/[\\d+]{2,})?|(?is)((?<=(?:REL )?VOL(?:UME)?\\s)[\\d+]{2,})\\sPA?GE?\\s([\\d+]{2,})";
				crossRefPat = Pattern.compile(bookPagePattern);
				mat.reset();
				mat = crossRefPat.matcher(remarks);
				if (mat.find()){
					mat.reset();
					while (mat.find()) {
						line = new ArrayList<String>();
						String g1 = mat.group(1);
						String g2 = mat.group(2);
						String g3 = mat.group(3);
						String g4 = mat.group(4);
						
						if(g1 != null && g2 != null && g3 != null) {
							line.add(g2);
							line.add(g3.replaceAll("/", ""));
							line.add(g1);
						} else {
							line.add((g1==null?(g3==null?"":g3):g1).replaceAll("\\A0+", ""));
							line.add((g2==null?(g4==null?"":g4):g2).replaceAll("\\A0+", ""));
							line.add("");
						}
						bodyCR.add(line);
					}
					remarks = remarks.replaceAll(bookPagePattern, "");
//				}else{
				}
				crossRefPat = Pattern.compile("\\b(\\d{6,})\\s*AND\\s*(\\d{6,})\\b");
				mat.reset();
				mat = crossRefPat.matcher(remarks);
				while (mat.find()) {
					line = new ArrayList<String>();
					line.add("");
					line.add("");
					line.add(mat.group(1).replaceAll("\\A0+", ""));
					bodyCR.add(line);
					line = new ArrayList<String>();
					line.add("");
					line.add("");
					line.add(mat.group(2).replaceAll("\\A0+", ""));
					bodyCR.add(line);
					remarks = remarks.replaceAll(mat.group(1), "").replaceAll(mat.group(2), "");
				}
				
				crossRefPat = Pattern.compile("\\b(\\d{6,})\\s+[A-Z]{3,}\\b");
				mat.reset();
				mat = crossRefPat.matcher(remarks);
				while (mat.find()) {
					line = new ArrayList<String>();
					line.add("");
					line.add("");
					line.add(mat.group(1).replaceAll("\\A0+", ""));
					bodyCR.add(line);
				}
				
				Pattern crossRefInstrPat = Pattern.compile("\\bORD#:\\s*(\\d{2,})\\b");
				mat.reset();
				mat = crossRefInstrPat.matcher(remarks);
				while (mat.find()) {
					line = new ArrayList<String>();
					line.add("");
					line.add("");
					line.add(mat.group(1).replaceAll("\\A0+", ""));
					bodyCR.add(line);
				}
				
				Pattern crossRefBPPat = Pattern.compile("(?is)[A-Z/]+\\s+[A-Z]\\s*(\\d+)\\s*/\\s*(\\d+)\\b");//D/T V197/135
				mat.reset();
				mat = crossRefBPPat.matcher(remarks);
				while (mat.find()) {
					line = new ArrayList<String>();
					line.add(mat.group(1).replaceAll("\\A0+", ""));
					line.add(mat.group(2).replaceAll("\\A0+", ""));
					line.add("");
					bodyCR.add(line);
					remarks = remarks.replaceAll(mat.group(0), "");
				}
				
				//remove these constructions
				if (remarks.contains("REN/EXT")){
					remarks = remarks.replace("REN/EXT","");
				}
				if (remarks.contains("REFILE"))	{
					remarks = remarks.replace("REFILE","");	
				}
				
					remarks = remarks.replace("OFFICIAL RECORDS", "");
					remarks = remarks.replace("RATIFICATION", "");
					remarks = remarks.replace("FAC", "");
					remarks = remarks.replace("FAV", "");
					remarks = remarks.replace("FED", "");
					remarks = remarks.replace("fed", "");
					remarks = remarks.replace("FIC", "");
					remarks = remarks.replace("FIL", "");
					remarks = remarks.replace("FIN", "");
					remarks = remarks.replace("FIR", "");

					remarks = remarks.replace("FLO", "");
					remarks = remarks.replace("FOR", "");
					remarks = remarks.replace("FOS", "");
					remarks = remarks.replace("FRE", "");
					remarks = remarks.replace("PFE", "");
					
					//too specific REMARKS:M/L DTD-- EX:--  
					remarks = remarks.replace("M/L DTD", "");

					remarks = remarks.replaceAll("(?is)\\bOF\\b", "");
					
					//to specific REMARKS:W/TRF 111 FREDONIA #
					//W/TRF == with power of sale and transfer
					remarks = remarks.replace("W/TRF 111", "");
					
					remarks = remarks.replace("23 STREET", "");
					
					//remove ex
					remarks = remarks.replaceAll("EX:", " EX:");
				 
					//for case  FNS 194971 TERM 90703                  #
					remarks = remarks.replaceAll("TERM(INATION)?( |#)?", "F#");
					//remove address					
					remarks = remarks.replaceAll("(\\d{2,}?&?\\d{1,}? (\\w+)? ? (\\w+)? ?(\\w+)? ?(CT|CIR|CR|AVE|LN|WAY|HGWY|BLVD|#|RD|DRIVE|DR|ROAD|ST|STREET))", "");
					//remove address that has no suffix or prefix from the remarks that contain only address
					String temp = remarks.replaceAll("\\d{3,} (?!DT)\\w+", "");
					if(StringUtils.isEmpty(temp)){
						remarks = temp;
					}
					
					//remove amount figures
					remarks = remarks.replaceAll("\\$ ?[0-9.,]+", "");
					//remove SHARE 5 N/HMSTD 
					remarks = remarks.replace("N/HMSTD", "");
					remarks = remarks.replaceAll("\\bRERECORDED\\b", "");
					remarks = remarks.replaceAll("\\bDOC\\s*#\\b", "");
					
					String wordsToCleanPattern = "(?i)(-\\s*[A-Z]{4,}(?:\\s+[A-Z]{4,})?\\s*)$";
					if (StringUtils.isNotEmpty(RegExUtils.getFirstMatch(wordsToCleanPattern, remarks, 1))) {
						remarks = remarks.replaceFirst(wordsToCleanPattern, "");
					}
					
//					(?<=#)(\d{3})[\s|-](\d{2})[\s|-](\d{2})[\s|-]
//					remarks.replaceAll("(?<=#)(\\d{3})[\\s|-](\\d{2})[\\s|-](\\d{2})[\\s|-]","$1$2$3" );                                        
//					((?is)(?<=(([C|F]#)|(M/L\\s)|(D/T\\s)|(DED\\s)|(SWD\\s)" +
//					"|(F)|(CB)|(N/)|(#)|(NCB\\s?)))\\w{3,})		
//					((?is)(?<=(([C|F]#)|(M/L\\s)|(D/T\\s)|(DED\\s)|(SWD\\s)|(F)|(CB)|(N/)|(#)|(NCB\\s?)))[A-Za-z0-9- ]{3,})
//					((?is)(?<=(([C|F]#)|(M/L\\s)|(D/T\\s)|(DED\\s)|(SWD\\s)|(F)|(CB)|(N/)|(#)|(NCB\\s?)))(\\w{3,})|(\\d+ ?-?\\w+ ?-?\\d+))
					
					remarks = remarks.replaceAll(";", " ");
					if(remarks.matches("[\\d\\s]+")) {
						String[] remarksParts = remarks.split("\\s+");
						for (String remarkPart : remarksParts) {
							line = new ArrayList<String>();
							line.add("");
							line.add("");
							line.add(remarkPart);
							bodyCR.add(line);
						}
					} else {
						crossRefPat = Pattern.compile("((?is)\\b(?<=(([C|F]#)|(M/L\\s)|(D/T\\s)|(DED\\s)|(SWD\\s)|(F)|(CB)|(N/)|(#)|(NCB\\s?)))(\\w{3,})?|(\\d+ ?-?\\w+ ?-?\\d+-?\\w+))-?\\w+-?\\w+");
						mat.reset();
						mat = crossRefPat.matcher(remarks);
	 					while (mat.find()){
							line = new ArrayList<String>();
							line.add("");
							line.add("");
							String group = mat.group();
							// rule for this case D/T F308865 #
							if (group.startsWith("F")){
								group = group.substring(1);
							}
							if (group.endsWith("DT")){
								group = group.replaceAll("DT", "");
							}
							group = group.trim().replaceAll("\\A.*\\s", "");			//E30'L16 19387 -> 19387
							line.add(group.replaceAll("\\A0+", ""));
							
							Matcher mat2 = Pattern.compile("00-([0-9]+)[ ]+([0-9]+)-([0-9]+)").matcher(group);
							Matcher mat3 = Pattern.compile("([0-9]+)[ ]+([0-9]+)-([0-9]+)").matcher(group);
							if(mat3.find()){
								ArrayList<String> line1 = new ArrayList<String>();
								line1.add(mat3.group(2));
								line1.add(mat3.group(3));
								line1.add(mat3.group(1));
								bodyCR.add(line1);
								
							}else if(mat2.find()){
								ArrayList<String> line1 = new ArrayList<String>();
								line1.add(mat2.group(2));
								line1.add(mat2.group(3));
								line1.add(mat2.group(1));
								bodyCR.add(line1);
								
							}else{
								String inst = line.get(2);
								if(inst.matches("[0-9]+-[0-9]+")){
									
									String []bookPage = inst.split("-");
									if(StringUtils.isEmpty(line.get(1))){
										
										line = new ArrayList<String>();
										line.add(bookPage[0]);
										line.add(bookPage[1]);
										line.add("");
									}
								}
								if (!checkIfExists(currentCr, group)){
									bodyCR.add(line);
								}
							}
						}
					}
			}
			
			ResultTable.appendRefSetToResultMap(m, bodyCR, currentCr);
		}
	}

	public static boolean checkIfExists(ResultTable crSet, String group){
		
		boolean isRefSet = crSet != null ? true : false;
		
		if (isRefSet){
			String[][] body = crSet.getBody();
			for (String[] row : body){
				if (row[2].trim().equals(group)){
					return true;
				}
			}
		} else {
			return false;
		}
		return false;
	}
	
	public static void partyNamesCODenverTR(ResultMap m, long searchId) throws Exception{
		CODenverTR.partyNamesCODenverTR(m, searchId);
	}
	
	public static void taxCODenverTR(ResultMap m, long searchId) throws Exception{
		CODenverTR.taxCODenverTR(m, searchId);
	}
	
	public static void parseAddressCODenverTR(ResultMap m, long searchId) throws Exception{
		CODenverTR.parseAddressCODenverTR(m, searchId);
	}
	
	public static void  copyThroughValuesSanAntonio(ResultMap m, long searchId) throws Exception{
		String blockThrough = (String) m.get("PropertyIdentificationSet.SubdivisionBlockThrough");
		String lotThrough = (String) m.get("PropertyIdentificationSet.SubdivisionLotThrough");
		String lot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
		String block = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
		
		if ( (!StringUtils.isEmpty(lot)) && (!StringUtils.isEmpty(lotThrough))){
			if (!lot.equals(lotThrough)){
				lot += "-"+lotThrough ;
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			}
		}
		
		if ( (!StringUtils.isEmpty(block)) && (!StringUtils.isEmpty(blockThrough)) ){
			if (!block.equals(blockThrough)){
				block += "-"+blockThrough;
				m.put("PropertyIdentificationSet.SubdivisionBlock",block);
			}
		}
	}
	/**
	 * Rule that can be applied for all DASL(data trace) sites.If we have both signed(instrument) and posted(recorded) date then
	 * posted date goes to recorded date and signed date goes to instrument date and if there is only one then it goes to recorded date.
	 * It is supposed that we have posted(recorded) date parsed into  "SaleDataSet.RecordedDate" and instrument(signed) date is parsed into 
	 * "SaleDataSet.InstrumentDate".
	 * @param m
	 * @param searchId
	 * @throws Exception
	 */
	public static void inverseInstrumentDates(ResultMap m, long searchId) throws Exception{
		//posted date
		String recordedDate = (String) m.get("SaleDataSet.RecordedDate");
		//signed date
		String instrumentDate = (String) m.get("SaleDataSet.InstrumentDate");
		
		if (StringUtils.isEmpty(recordedDate)||StringUtils.isEmpty(instrumentDate)){
			if (StringUtils.isEmpty(recordedDate)){
				m.put("SaleDataSet.RecordedDate", instrumentDate);
			}
		}
	}
	
	/**
	 * Parsed the value found on <em>PropertyIdentificationSet.AddressOnServer</em> <br>
	 * as a street number and street name and stores it in 
	 * <em>PropertyIdentificationSet.StreetNo</em> and
	 * <em>PropertyIdentificationSet.StreetName</em>
	 * @param resultMap	the map that should contains the key PropertyIdentificationSet.AddressOnServer
	 * @return true if the parsing was successful
	 */
	public static boolean parseAddressOnServer(ResultMap resultMap) {
		if(resultMap == null) {
			return false;
		}
		String addressOnServer = (String) resultMap.get("PropertyIdentificationSet.AddressOnServer");
		if(StringUtils.isEmpty(addressOnServer)) {
			return false;
		}
		String[] addressParts = StringFormats.parseAddress(addressOnServer);
		if(addressParts == null || addressParts.length != 2) {
			return false;
		}
		resultMap.put("PropertyIdentificationSet.StreetNo", addressParts[0]);
		resultMap.put("PropertyIdentificationSet.StreetName", addressParts[1]);
		return true;
	}
	
	/*if book has more values split by semicolons (e.g. REL BOOK: 30'L16 19387; 934	REL PAGE: 80)
	keep only the value after the last semicolon*/  
	@SuppressWarnings("rawtypes")
	public static void cleanCrossRefsBookTS(ResultMap m, long searchId) throws Exception {
	
		ResultTable crossRefs = (ResultTable) m.get("CrossRefSet");
		if(crossRefs != null) {
			String[] header = crossRefs.getHead();
			if (header!=null) {
				int indexBook = getColumnIndexCrossRef(crossRefs, "book", true);
				if (indexBook!=-1) {
					String[][] body = crossRefs.getBodyRef();
					List<List> newBody = new ArrayList<List>();
					List<String> list;
					for (String[] row : body) {
						list = new ArrayList<String>();
						for (int i=0;i<row.length;i++) {
							if (i==indexBook) {
								if (row[i].indexOf(";")!=-1) {
									list.add(row[i].replaceFirst(".*;", "").trim());					
								}
								else {
									list.add(row[i].trim());											
								}
							}
							else 
								list.add(row[i].trim());
						} 												
						newBody.add(list);
					}
					
					boolean isReadOnly = crossRefs.readOnly;
					if(isReadOnly) {
						crossRefs.setReadWrite();
					}
					crossRefs.setBody(newBody);
					if(isReadOnly) {
						crossRefs.setReadOnly();
					}
				}
			}	
		}
	}
		
	public static void compactCrossRefsTS(ResultMap m, long searchId) throws Exception{
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
	   
		if ("AK".equals(crtState)){
			if ("Anchorage Borough".equals(crtCounty)){
				ResultTable crossRefs = (ResultTable) m.get("CrossRefSet");
				if(crossRefs != null) {
					String[][] body = crossRefs.getBodyRef();
					LinkedHashMap<String, String[]> alreadyFoundRows = new LinkedHashMap<String, String[]>();
					List<String[]> untouched = new ArrayList<String[]>();
					for (String[] row : body) {
						String book = row[0];
						String page = row[1];
						String instrument = row[2];
						if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
							String key = org.apache.commons.lang.StringUtils.leftPad(book, 4, "0") + 
								org.apache.commons.lang.StringUtils.leftPad(page, 4, "0");
							String[] old = alreadyFoundRows.get(key);
							if(old == null) {
								alreadyFoundRows.put(key, row);
							} else {
								old[0] = book;
								old[1] = page;
							}
						} else if(StringUtils.isNotEmpty(instrument)) {
							String key = org.apache.commons.lang.StringUtils.leftPad(instrument, 8, "0");
							String[] old = alreadyFoundRows.get(key);
							if(old == null) {
								alreadyFoundRows.put(key, row);
							} else {
								old[2] = instrument;
							}
						} else {
							untouched.add(row);
						}
						
					}
					body = new String[alreadyFoundRows.size() + untouched.size()][];
					int i = 0;
					for (String key : alreadyFoundRows.keySet()) {
						String[] temp = alreadyFoundRows.get(key);
						
						if(StringUtils.isNotEmpty(temp[0]) && StringUtils.isNotEmpty(temp[1]) && StringUtils.isNotEmpty(temp[2]) &&
								org.apache.commons.lang.StringUtils.leftPad(temp[2], 8, "0").equals(org.apache.commons.lang.StringUtils.leftPad(temp[0], 4, "0") + 
								org.apache.commons.lang.StringUtils.leftPad(temp[1], 4, "0"))) {
							temp[2] = "";
						}
						
						body[i++] = temp;
					}
					for (String[] strings : untouched) {
						body[i++] = strings;
					}
					boolean isReadOnly = crossRefs.readOnly;
					if(isReadOnly) {
						crossRefs.setReadWrite();
					}
					crossRefs.setBody(body);
					if(isReadOnly) {
						crossRefs.setReadOnly();
					}
					
				}
			}
		} else if("CO".equals(crtState)){
			if ("Grand".equals(crtCounty)){
				ResultTable crossRefs = (ResultTable) m.get("CrossRefSet");
				if(crossRefs != null) {
					String[][] body = crossRefs.getBodyRef();
					for (String[] row : body) {
						String instrument = row[2];
						if(instrument.matches("\\d{10}")) {
							instrument = instrument.substring(0, 4) + "-" + instrument.substring(4);
							row[2] = instrument;
						} else if(instrument.matches("\\d{4}-{2,}\\d{6}")) {
							row[2] = instrument.substring(0, 4) + "-" + instrument.substring(instrument.length() - 6);
						}
					}
				}
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void cleanCrossRefsTS(ResultMap m, long searchId) throws Exception {
	
		ResultTable crossRefs = (ResultTable) m.get("CrossRefSet");
		CurrentInstance c = InstanceManager.getManager().getCurrentInstance(searchId);
		if(crossRefs != null) {
			String[] header = crossRefs.getHead();
			if (header!=null) {
				int indexBook = getColumnIndexCrossRef(crossRefs, "book", true);
				int indexInstrumentNumber = getColumnIndexCrossRef(crossRefs, "instrumentnumber", true);
				int indexPage = getColumnIndexCrossRef(crossRefs, "page", true);
				if (indexBook!=-1 || indexInstrumentNumber!=-1) {
					String[][] body = crossRefs.getBodyRef();
					List<List> newBody = new ArrayList<List>();
					List<String> list;
					for (String[] row : body) {
						list = new ArrayList<String>();
						if (indexPage!=-1){
							//T7262
							if(c!=null && (c.getCurrentState().getStateAbv()+c.getCurrentCounty().getName()).equals("ARBenton") ){
								row[indexPage] = row[indexPage].replaceAll("[a-zA-Z]+$","");
							}
						}
						if (indexBook!=-1)
							row[indexBook] = row[indexBook].replaceFirst("\\A.*[A-Za-z]", "").trim();		//17GF66469 -> 66469, 30'L1619387 -> 1619387
						if (indexInstrumentNumber!=-1)
							row[indexInstrumentNumber] = row[indexInstrumentNumber].replaceFirst("\\A.*[A-Za-z]", "").trim();
						if (indexBook!=-1 && indexPage!=-1 && 
								toBeRemovedFromCrossRef(crossRefs, row[indexBook], indexBook, row[indexPage], indexPage)) {
							for (int i=0;i<row.length;i++)
								if (i==indexBook || i==indexPage)
									list.add("");
								else
									list.add(row[i]);
						} else {
							for (int i=0;i<row.length;i++)
								list.add(row[i]);
						}
						if (!emptyList(list))
							newBody.add(list);
					}
					
					boolean isReadOnly = crossRefs.readOnly;
					if(isReadOnly) {
						crossRefs.setReadWrite();
					}
					try {
						crossRefs.setBody(newBody);
					} catch(Exception e) {
						e.printStackTrace();
					}
					if(isReadOnly) {
						crossRefs.setReadOnly();
					}
				}
			}
		}
	}
	
	//if current book contains another book and current page
	//is identical to the corresponding page
	//e.g. 166461-193 if we have also 6461-193 in CrossRefSet
	public static boolean toBeRemovedFromCrossRef(ResultTable crossRefs, String book, int indexBook, String page, int indexPage) {
		
		boolean isRefSet = crossRefs != null ? true : false;
		if (isRefSet){
			String[][] body = crossRefs.getBodyRef();
			for (String[] row : body) {
				String rowBook = row[indexBook];
				String rowPage = row[indexPage];
				if (book.matches("[A-Za-z0-9']+"+rowBook) && page.equals(rowPage))
					return true;
			}
		} else
				return false;
		return false;
	}
	
	public static int getColumnIndexCrossRef(ResultTable crossRefs, String column, boolean ignoreCase) {
		
		boolean isRefSet = crossRefs != null ? true : false;
		if (isRefSet){
			String[] header = crossRefs.getHead();
			if (header!=null) {
				if (ignoreCase)
					column = column.toLowerCase();
				for (int i=0;i<header.length;i++) {
					String currentColumn = header[i];
					if (ignoreCase)
						currentColumn = currentColumn.toLowerCase();
					if (column.equals(currentColumn))
						return i;
					
				}
			}
		}
		return -1;
	}
	
	public static boolean emptyList(List<String> list) {
		for (int i=0;i<list.size();i++)
			if (list.get(i).trim().length()!=0)
				return false;
		return true;
	}
	
	public static void newParsingGrantorGranteeTNShelbyRO(ResultMap m, long searchId) throws Exception {
		
		ro.cst.tsearch.servers.functions.TNShelbyRO.parseName(m, searchId);
	}
	
    public static void main(String[] args) {
    	String comment = "EASE PT LTS IN S-85; EASE ACROSS LTS 58-60 F-48; EASE ACROSS LTS 54-56 F-48; EASE ACROSS LTS 40-42 F-48; EASE N30 0013 22329; EASE 0021 W OF RR 362329; EASE PT BLKS 149 150 B-144; EASE PT BLKS 136-142 B-144; ";
    	String lot = extractLotFromComment(comment);
    	if(StringUtils.isNotEmpty(lot)) {
			lot = lot.replaceAll("\\s\\d{4,}-\\d{4,}(-\\s)?", " ");
			lot = LegalDescription.cleanValues(lot, false, true);
    	}
	}

}

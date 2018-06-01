package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;

public class TNShelbyTR {
    @SuppressWarnings("unchecked")
	public static void partyNamesTokenizerTNShelbyTR(ResultMap m, String s) throws Exception {
    	
    	// corrections and cleanup
    	s = s.replaceFirst("(?is)(?:&|\\bAND\\b)\\s*([A-Z]+\\s+[A-Z]+)\\s+\\(?\\s*TRS\\s*\\)?\\s*$", "TR & $1 TR");
    	s = s.replaceFirst("\\s+AND\\s+(ETAL)\\s*$", " $1");
    	s = s.replaceAll("\\(+", "");
    	s = s.replaceAll("\\)", " AND ");
    	s = s.replaceAll("\\bSUB\\b", " ");
    	s = s.replaceAll("\\b\\d+/\\d+\\s*INT\\b", "");
    	s = s.replaceAll("(?<=[A-Z])'(?=\\s)", "");
    	s = s.replaceAll("\\bAND(\\s+AND\\b)+", "AND");
    	s = s.replaceAll("(,\\s?)?\\bMD\\.?", "");
    	s = s.replaceAll("(,\\s?)?\\bPC\\.?", "");
    	s = s.replaceAll("(,\\s?)?\\bCDP\\.?", "");
    	s = s.replaceAll("(,\\s?)?\\bATT(ORNE)?Y\\b", "");
    	s = s.replaceAll("[\\)\\(\\.]", "");    	
    	s = s.replaceAll("\\bEST\\b", "");
    	s = s.replaceAll("\\s*\\bAND\\s*$", "");
    	s = s.replaceAll("#?\\b\\d+\\b", "");
    	s = s.replaceAll("\\b([A-QS-Z])(REVOCABLE)\\b", "$1 $2");
    	s = s.replaceAll("(REVOCABLE)(TRUST)\\b", "$1 $2");
    	s = s.replaceAll("\\bAND(?!E?R)(?=[A-Z]+\\s+[A-Z]\\s+[A-Z'-]+)", "AND ");
    	s = s.replaceAll("\\bAND(?!E?R)([A-Z]+(\\s+[A-Z])?)$", "AND $1");
    	s = s.replaceAll("\\bND(?=\\s+[A-Z]+\\s+[A-Z]+\\b)", "AND");
    	s = s.replaceAll("\\bAND(\\s+AND\\b)+", "AND");
    	s = s.replaceAll("\\bAND\\s+(?=[A-Z-]+(\\s+[A-Z](\\s+[A-Z])?)?(\\s+AND\\b|\\s+&|\\s*$))", "& ");
    	s = s.replaceAll("-\\s*$", "");
        s = s.replaceAll("\\s{2,}", " ").trim();
        //B 3446 the last names is C and on AO and RO is CARTWRIGHT
        s = s.replaceAll("(TRAFT CHARLWES F & LOIS L &) (JOSEPH E) C", "$1 CARTWRIGHT $2"); 
        //B 3905 it's the only name in F L format
        s = s.replaceAll("(BARRY) (ANDERSON)", "$2 $1");
        
        List<List> body = new ArrayList<List>();
        
        String entities[] = s.split("\\s?\\bAND\\b\\s?");
        
        String[] a = {"", "", "", "", "", ""};
        String[] b = {"", "", "", "", "", ""};
        String[] suffixes, type, otherType;
        
        int lastPos = 0;
        // check if first owner is company
        if (!entities[0].matches("(?i).*\\bD/?B/?A\\b/?.*")){
	        if (entities.length == 2){
	        	if (NameUtils.isCompany(entities[0])){        		             	
	            	if (NameUtils.isCompany(entities[1])){	            		
            			a[2] = s;
            			type = GenericFunctions.extractAllNamesType(a);
		    			otherType = GenericFunctions.extractAllNamesOtherType(a);
		        		GenericFunctions.addOwnerNames(a, "", "", type, otherType, true, false, body);
            			lastPos = 2;  
	            	} else {
	            		a[2] = entities[0];
	            		type = GenericFunctions.extractAllNamesType(a);
		    			otherType = GenericFunctions.extractAllNamesOtherType(a);
		        		GenericFunctions.addOwnerNames(a, "", "", type, otherType, true, false, body);
	                	lastPos = 1;
	            	}
	        	} else if (NameUtils.isCompany(entities[0]) && NameUtils.isCompany(entities[1])){
		        	if (!entities[1].contains("FAMILY")){	// SMITH J SHIRLEY AND SMITH FAMILY TRUST
			        	a[2] = s; 
			        	type = GenericFunctions.extractAllNamesType(a);
			    		otherType = GenericFunctions.extractAllNamesOtherType(a);
			        	GenericFunctions.addOwnerNames(a, "", "", type, otherType, true, false, body);
			            lastPos = 2;
	        		}
	        	}
	        } 
        }
        
        // parse first entity as L F M
        Matcher ma1, ma2;
        String owner, spouse = "", owner3 = "", ownerSuffix = "", spouseSuffix = ""; 
        String[] addOwners;
        if (lastPos == 0){        	
        	owner = entities[0];
        	ma1 = Pattern.compile("(?i)(.*?)\\s*\\bD/?B/?A\\b/?\\s*(.+)").matcher(owner);
        	if (ma1.matches()){
        		owner = ma1.group(1);
        		a[2] = ma1.group(2); 
        		GenericFunctions.addOwnerNames(a, "", "", true, false, body); 
            	 entities[0] = owner;
        	} 
        	ma1 = Pattern.compile("(.+?)\\s*&\\s*([^&]+)&?(.*)").matcher(owner);
        	if (ma1.matches()){
        		owner = ma1.group(1);
        		spouse = ma1.group(2);
        		owner3 = ma1.group(3);
        	}
        	a[0] = ""; a[1] = ""; a[2] = ""; a[3] = ""; a[4] = ""; a[5] = "";
        	boolean ownerIsCompany = false;
        	if (NameUtils.isCompany(owner)){        		             	
            	if (NameUtils.isCompany(spouse) || spouse.matches("\\w+")){	            		
        			a[2] = entities[0];
        			spouse = "";
        			owner3 = "";
        			ownerIsCompany = true;
            	} else {
            		a[2] = owner;  
                	ownerIsCompany = true;
            	}
            	lastPos = 1;
        	} else if (NameUtils.isCompany(entities[0])){	
	        		a[2] = entities[0]; 
	            	spouse = "";
        			owner3 = "";
        			ownerIsCompany = true;
	            	lastPos = 1;
        	} else {
        		ma2 = GenericFunctions.nameSuffix.matcher(owner);
        		if (ma2.matches()){		// owner has suffix => remove it 
        			owner = ma2.group(1).trim();
        			ownerSuffix = ma2.group(2);
        		}
        		a = StringFormats.parseNameNashville(owner, true);
        	}
        	spouse = StringFormats.unifyNameDelim(spouse, true);
    		if (spouse.length() != 0){
    			ma2 = GenericFunctions.nameSuffix.matcher(spouse);
        		if (ma2.matches()){		// spouse has suffix => remove it 
        			spouse = ma2.group(1).trim();
        			spouseSuffix = ma2.group(2);
        			entities[0] = ma1.group(1) + " & " + spouse;
        		}        		        		        		
        		// if spouse is F MI? L then parse it separately   
        		if ((spouse.matches("[A-Z]{1,} [A-Z]+( [A-Z]+)? [A-Z'-]{2,}( ETAL| ETUX| ETVIR])?") && !spouse.matches("[A-Z]{2,} [A-Z]+( [A-Z])? TR(USTEE?)?")) 
        								|| spouse.matches("[A-Z]{2,} [A-Z]{2,}-[A-Z]+")){
        			b = StringFormats.parseNameDesotoRO(spouse, true);
        			suffixes = GenericFunctions.extractNameSuffixes(b);//JONES F M & N D SMITH JR ETAL
        			if (ro.cst.tsearch.utils.StringUtils.isEmpty(spouseSuffix)){
        				spouseSuffix = suffixes[0];
        			}
        		// else add the owner last name to spouse and parse the spouse as L F M	
        		} else {
        			if (!spouse.contains(a[2]) && a[0].length() != 0){
        				spouse = a[2] + " " + spouse;
        			}
        			b = StringFormats.parseNameNashville(spouse, true);
        		}
        		a[3] = b[0];
    			a[4] = b[1];
    			a[5] = b[2]; 
    			
    			type = GenericFunctions.extractAllNamesType(a);
    			otherType = GenericFunctions.extractAllNamesOtherType(a);
    			GenericFunctions.addOwnerNames(a, ownerSuffix, spouseSuffix, type, otherType, ownerIsCompany, NameUtils.isCompany(spouse), body);
        	
				String prevLast = a[5];
				owner3 = owner3.trim();
	        	if (owner3.length() != 0){
	        		addOwners = owner3.split("\\s?&\\s?");
	        		for (int j=0; j<addOwners.length; j++){
	        			ma2 = Pattern.compile("(.*\\b[A-Z]+)("+prevLast+")$").matcher(addOwners[j]);
	        			if (ma2.matches()){
	        				addOwners[j] = ma2.group(1) + " " + ma2.group(2);
	        				a = StringFormats.parseNameDesotoRO(addOwners[j], true);
	        			} else {
		        			if (!addOwners[j].matches("[A-Z'-]{2,} [A-Z]+ [A-Z]+") && !addOwners[j].contains(prevLast)){
		        				addOwners[j] = prevLast + " " + addOwners[j];
		        			}
		        			a = StringFormats.parseNameNashville(addOwners[j]);
	        			}
	        			prevLast = a[2];
	        			suffixes = GenericFunctions.extractNameSuffixes(a);
	        			type = GenericFunctions.extractAllNamesType(a);
	        			otherType = GenericFunctions.extractAllNamesOtherType(a);
	        			GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], type, otherType, false, false, body);
	        		}
	        	}
    		} else {
    			type = GenericFunctions.extractAllNamesType(a);
    			otherType = GenericFunctions.extractAllNamesOtherType(a);
    			GenericFunctions.addOwnerNames(a, ownerSuffix, "", type, otherType, ownerIsCompany, false, body);
    		}
    		lastPos = 1;
        }
        	
        // parse the rest of the entities as F M L                
        for (int i=lastPos; i<entities.length; i++){        	
        	if (NameUtils.isCompany(entities[i])){
        		a[0] = ""; a[1] = ""; a[3] = ""; a[4] = ""; a[5] = "";
        		a[2] = entities[i]; 
        		type = GenericFunctions.extractAllNamesType(a);
    			otherType = GenericFunctions.extractAllNamesOtherType(a);
    			GenericFunctions.addOwnerNames(a, "", "", type, otherType, true, false, body); 
        	} else {
        		a = StringFormats.parseNameDesotoRO(entities[i]);
        		suffixes = GenericFunctions.extractNameSuffixes(a);
        		type = GenericFunctions.extractAllNamesType(a);
    			otherType = GenericFunctions.extractAllNamesOtherType(a);
    			GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], type, otherType, false, false, body);
        	}
        }
        GenericFunctions.storeOwnerInPartyNames(m, body, true);                
    }
    
    public static void taxShelbyTR(ResultMap m,long searchId) throws Exception {//county
    	
        BigDecimal sumAmountPaidTR = new BigDecimal("0.00");
        String year = (String) m.get("TaxHistorySet.Year");
        ResultTable transacTable = (ResultTable) m.get("TaxHistorySet");
        if (transacTable != null) {
        	String [][] bodyT = transacTable.getBody();
        	if (bodyT.length != 0) {
        		for (int i = 0; i < bodyT.length-1; i++) {
        			if (bodyT[i][0].contains(year)){
        				sumAmountPaidTR = sumAmountPaidTR.add(new BigDecimal(bodyT[i][3]));
        			}
        		}
        		
        	}
        }
        m.put("TaxHistorySet.AmountPaid", sumAmountPaidTR.toString());
        
        String allDue = (String) m.get( "tmpDueTotal" ); //all the due value, including the current year
        if( allDue == null ) allDue = "0.00";
        
        //B 3285
        String feesEP = "0.00";
        String penaltyEP = "0.00";
        m.put("TaxHistorySet.TotalDueEP", "0.00");
        
        ResultTable taxTable = (ResultTable) m.get("tmpTaxesAllYearsTable");
        BigDecimal sumDelinqTR = new BigDecimal("0.00");
        BigDecimal sumDelinqEP = new BigDecimal("0.00");
        if (taxTable != null) {
        	String[][] body = taxTable.getBodyRef();
        	if (body.length != 0) {
        		for (int i = 0; i < body.length-1; i++) {
	        			if ((body[i][0].contains(year)) && (body[i][1].contains("Shelby County"))) {
	        				m.put("TaxHistorySet.TotalDue", body[i][6]);
	        			} else if ((body[i][0].contains(year)) && !(body[i][1].contains("Shelby County"))) {
	        				m.put("TaxHistorySet.TotalDueEP", body[i][6]);
	        				m.put("TaxHistorySet.TaxYearEPfromTR", body[i][0]);
	        				feesEP = body[i][4];
	        				penaltyEP = body[i][5];
	        			} else	if (!body[i][0].contains(year) && !body[i][1].contains("Fees") && body[i][6] != null){
	        				try {
        						if (body[i][1].contains("Shelby County")){
        							sumDelinqTR = sumDelinqTR.add(new BigDecimal(body[i][6]));
        						} else {
        							sumDelinqEP = sumDelinqEP.add(new BigDecimal(body[i][6]));
        						}
	        				} catch (Exception e) {
								e.printStackTrace();
							}
	        			}
        			}
        	}
        }
    	m.put("TaxHistorySet.PriorDelinquent", sumDelinqTR.toString());
    	m.put("TaxHistorySet.PriorDelinquentEP", sumDelinqEP.toString());
    	
        String totalDue = (String) m.get("TaxHistorySet.TotalDue"); // TSD Total
        if( totalDue == null ) totalDue = "0.00";
        
        String fees = (String) m.get( "tmpOtherCharges" );
        if( fees == null ) fees = "0.00";
        
        String penalty = (String) m.get( "tmpPenalty" );
        if( penalty == null ) penalty = "0.00";
        
        BigDecimal baseAmount = new BigDecimal(totalDue);
        
        if (!"0.00".equals(totalDue)) {
        	baseAmount = baseAmount.subtract( new BigDecimal( fees ) );
        	baseAmount = baseAmount.subtract( new BigDecimal( penalty ) );
        	m.put("TaxHistorySet.BaseAmount", baseAmount.toString());
        }
        
        String totalDueEP = (String) m.get("TaxHistorySet.TotalDueEP");
        if( totalDueEP == null ) totalDueEP = "0.00";
        if( feesEP == null ) feesEP = "0.00";
        if( penaltyEP == null ) penaltyEP = "0.00";
        
        BigDecimal baseAmountEP = new BigDecimal(totalDueEP);
        baseAmountEP = baseAmountEP.subtract( new BigDecimal( feesEP ) );
        baseAmountEP = baseAmountEP.subtract( new BigDecimal( penaltyEP ) );
        m.put("TaxHistorySet.BaseAmountEP", baseAmountEP.toString());           
        
        //BigDecimal delinquentValue = new BigDecimal( allDue );
        //delinquentValue = delinquentValue.subtract( new BigDecimal( totalDue ) );               
        //m.put("TaxHistorySet.PriorDelinquent", delinquentValue.toString() );
        
    }    
}

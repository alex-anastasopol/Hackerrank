package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;

public class TNKnoxEP {
    @SuppressWarnings("unchecked")
	public static void stdPisTNKnoxEP(ResultMap m,long searchId) throws Exception {
        
    	
    	String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
    	
    	if (s == null || s.length() == 0)
        	return;
    	
    	boolean ampe = false;
    	s = s.replaceAll("(?is)\\s+MRS?\\s+", " ");
    	s = s.replaceAll("(?is)\\s+&\\s*CO\\s*$", "");
    	s = s.replaceAll("[\\)\\(]+", "");
    	s = s.replaceAll("Sterling", "& Sterling");
    	s = s.replaceAll("(?is)(LIFE)\\s+(EST(?:ATE)?)", "");
    	s = s.replaceAll("\\s+UNION", " UNION"); //pid 082PK002
    	s = s.replaceAll("\\s+TAYLOR", " TAYLOR");
    	s = s.replaceAll("%\\s*", "% ");
    	s = s.replaceAll("(?is)co\\s*-\\s*(TR(?:(?:UST)?EE)?S?)\\b", " $1");
    	s = s.replaceAll("(?is)\\s*ET\\s*UX\\s*", " ETUX ");
    	s = s.replaceAll("C/O", "&");
    	s = s.replaceAll("(?is)\\s*&\\s*([A-Z]+(?:\\s+[A-Z])?)(\\s+TRU(?:STEE)?S)", " and $1 $2");
    	s = s.replaceAll("(?is)\\b(trustees?)\\s*of\\s*the", "$1 & ");//Smith Bobbye Ruth Trusteeof The Bobbye Ruth Smithrevocable Living Trust
    	//s = s.replaceAll("(?is)\\b(trustees?)\\s+(revocable)", "$1 & $2");//Smith Bobbye Ruth Trustee Revocable Living Trust
    	if (!s.toLowerCase().contains("etal") && !s.toLowerCase().contains("etux")){
    		s = s.replaceAll("(?is)\\A\\s*([A-Z]+\\s+[A-Z]+\\s+[A-Z])\\s+([A-Z]{4,}\\s+[A-Z]+(?:\\s+[A-Z])?)\\b", "$1 & $2");//PAPPAS SUE A SMITH DAVIDA TRUSTEES
    	}
    	s = s.replaceAll("(?is)\\s+(ETAL)\\s+(TRUSTEES)", " $2 $1");
    	String[] owners ;
    	if (!s.matches("(.+)\\s+&\\s+(.+)\\s+&\\s+(.+)"))
    		s = s.replaceAll("\\b\\s{2,}\\b", " & ");
    	if (s.matches("(.+)\\s+&\\s+(.+)\\s+&\\s+(.+)"))
    			ampe = true;
    	else s = s.replaceAll("\\s{2,}", " ");
    	s = s.toUpperCase();
    	if (!ampe && !s.contains("%") && !s.contains("&")){
    		owners = s.split("\\s{2,}");
    	} else {
    		owners = s.split("&|%");
    	}
		List<List> body = new ArrayList<List>();
		String[] names;
		String[] suffixes, type, otherType;
		
		//Pattern p3 = Pattern.compile("(.+) [%|&] (.+)");
		//Matcher ma;
		boolean parserCoownerFML = false;
		String ln="";
		boolean bothAreTrustees = false;
		if (owners.length > 1){
			for (int i=0; i<owners.length; i++){
				if (owners[i].matches(".*\\b(TRS|TRUSTEES)\\b.*")){//PAPPAS SUE A & SMITH DAVIDA TRUSTEES
					bothAreTrustees = true;
					owners[i] = owners[i].replaceAll("(?is)\\b(TRS|TRUSTEES)\\b", "").trim();
					break;
				}
			}
		}
		if (bothAreTrustees){
			for (int i=0; i<owners.length; i++){
				owners[i] = owners[i].trim() + " TR";

			}
		}
		for (int i=0; i<owners.length; i++){
			String ow = owners[i];
			boolean fml = false;
			if (i == owners.length -1 
				&& s.contains("%")){
				fml = true;
			}
				parserCoownerFML = false;
				String coowner = "";
			
			/*ma = p3.matcher(ow);			
			if (ma.matches()){
				parserCoownerFML = true;
				coowner = ma.group(2);
					names = StringFormats.parseNameDesotoRO(ma.group(1), true);
				
			} else */ 				
					names = StringFormats.parseNameNashville(ow, true);	
				
				if (!NameUtils.isCompany(names[2])) {
					if ((names[0].length() == 1 && names[1].length() == 0) || (names[0].length() == 0 && names[1].length() == 0)) {
						names[1] = names[0];
						names[0] = names[2];
						names[2] = ln;
					}
					if ((names[2].length() == 1 && names[1].length() == 0) || (names[2].length() == 0 && names[1].length() == 0)) {
						names[1] = names[2];
						//names[0] = names[2];
						names[2] = ln;
					}
					if (i!=0 && names[1].length()==0 												//MERIDIETH KYLE EDWARD & SARAH JEAN 
							&& !LastNameUtils.isLastName(names[0])	&& FirstNameUtils.isFirstName(names[0])
							&& !LastNameUtils.isLastName(names[2])	&& FirstNameUtils.isFirstName(names[2])) {
						names[1] = names[0];
						names[0] = names[2];
						names[2] = ln;
					}
				}
			if (parserCoownerFML ){//&& !ampe && !s.contains("%")){										
				//String[] names2 = StringFormats.parseNameNashville(coowner, true); 				//SMITH MARY ELLA TR (REM) % YVONNE MAXINE RAHAMING
				//names[3] = names2[0];											
				//names[4] = names2[1];
				//names[5] = names2[2];
			//} else {
				String[] names2 = StringFormats.parseNameDesotoRO(coowner, true); 	//TAYLOR ROBERT A III TAYLOR ANTHONY J & TAYLOR ALLAN TODD
				names[3] = names2[0];											
				names[4] = names2[1];
				names[5] = names2[2];
			}
			if ((LastNameUtils.isLastName(names[0]) && !LastNameUtils.isLastName(names[2])) || fml) {
				String aux = names[0];
				names[0] = names[2];
				names[2] = aux;
			}
			if (LastNameUtils.isLastName(names[1]) && LastNameUtils.isNotLastName(names[2])) {
				String aux = names[2];
				names[2] = names[1];
				names[1] = aux;
				
			}
			//% first last
			if (ow.contains("%") && names[4].equals("")
				&& NameFactory.getInstance().isLast(names[3])
				&& NameFactory.getInstance().isFirstMiddle(names[5])){
					String tmpName = names[3];
					names[3] = names[5];
					names[5] = tmpName;
			}
			ln = names[2];
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);        
	        GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
	        								NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		String[] a = StringFormats.parseNameNashville(s, true);
        m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
        m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
        m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
        m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
        
        /*

        // owner name may contains several names
        // first try to tokenize the first line, if there are already 2 names in the first line, then ignore the other lines 
        //- ATS supports max 2 owners for now
        s = StringFormats.unifyNameDelim(s);
        String[] lines = s.split("\\s{2,}");               
        String[] a = StringFormats.parseNameNashville(lines[0]);
        if (a[5].length() == 0 && lines.length >= 2){        
        	if (lines[0].trim().endsWith("&") || lines[1].trim().startsWith("&")){
        		a = StringFormats.parseNameNashville(lines[0] + " " + lines[1]);
        	} else {
        		String[] b = StringFormats.parseNameNashville(lines[1]);
        		a[3] = b[0];
        		a[4] = b[1];
        		a[5] = b[2];
        	}        		
        	a[4] =  a[4].replaceFirst("(.+?)\\s*&.*", "$1");		// PID 068NE033, Owner=SMITH VERNON  & GLADYS R% JOHN D DYESS 
        }
                        
        m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
        m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
        m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
        m.put("PropertyIdentificationSet.SpouseLastName", a[5]);*/        
    }
    
    public static void taxKnoxvilleEP(ResultMap m,long searchId) throws Exception {
        
        String relief = (String) m.get("tmpRelief");
        if (relief == null || relief.length() == 0) 
        	relief = "0.00";
        
        String discount = (String) m.get("tmpDiscount");
        if (discount == null || discount.length() == 0) 
        	discount = "0.00";
        
        String baseAmt = (String) m.get("TaxHistorySet.BaseAmount");
        if (baseAmt == null || baseAmt.length() == 0) 
        	baseAmt = "0.00";
        
        String priorYears = (String) m.get("TaxHistorySet.PriorDelinquent");
        if (priorYears == null || priorYears.length() == 0) 
        	priorYears = "0.00";
        
        String currentYearUnpaid = (String) m.get("tmpTaxBalance");
        if(currentYearUnpaid == null || currentYearUnpaid.length() == 0) 
        	currentYearUnpaid = "0.00";
        
        String totalDue = (String) m.get("TaxHistorySet.TotalDue");
        if (totalDue == null || totalDue.length() == 0)
        	totalDue = "0.00";
                
        BigDecimal amountPaid = new BigDecimal(baseAmt).
        							subtract(new BigDecimal(relief)).
        							subtract(new BigDecimal(discount)).
        							subtract(new BigDecimal(currentYearUnpaid)); // fix for bug #1297
        m.put("TaxHistorySet.AmountPaid", amountPaid.toString() );
        m.put("TaxHistorySet.CurrentYearDue", totalDue);
    }
}

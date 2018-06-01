package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class TNGenericTR {
	
	@SuppressWarnings("rawtypes")
	public static void ownerTNGenericTR(ResultMap m, long searchId) throws Exception {
		   
		   String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
	   	
		   	if(StringUtils.isEmpty(s))
		       	return;
		   	
		   	String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		   	
		   	s = s.replaceAll("(?is)(\\s)ETU($|\\s)","$1ETUX$2"); //B7234
		   	
		   	s = s.replaceAll("MRS", "");
		   	if (!s.contains("(LE)")){
		   		s = s.replaceAll("[\\(\\)]+", "");
		   	}
		   	s = s.replaceAll("\\bATTN\\s*:?\\b", "");
//		   	if (s.matches("(?is).*ETUX\\s*[A-Z]{3,}\\s*"))  //B3430
//		   	{
//		   		s = s.replaceFirst("ETUX\\s*(.*)", "& $1 ETUX ");	
//		   	}		
		   	s = s.replaceAll("(ETUX)\\s+%\\s*", "$1 ");
		   	s = s.replaceAll("\\s+(ET)\\s*(UX|AL|VIR)\\s+", " $1$2 ");
		   	s = s.replaceAll("%", " @@ ");
		   	s = s.replaceAll("&\\s+", "& ");
		   	//s = s.replaceFirst("&\\s+([A-Z]+\\s+[A-Z]{1,3})?\\s+&", "ETUX $1 &"); //TURNER FRANCES JANETT &  JOHN III & VINCENT MALONE
		   	if (s.contains("REVOCABLE")){
		   		s = s.replaceAll("(?is)\\A\\s*(\\w+\\s+\\w+(?:\\s+[A-Z])?\\s+ETUX(?:\\s+\\w+)?)\\s+(REVOCABLE\\s+LIVING\\s+TR(?:UST)?)\\b", "$1 @@ $1 $2");//SMITH ARLEY C ETUX  REVOCABLE LIVING TRUST
		   		s = s.replaceAll("(?is)\\s+ETUX\\s+(REV)", " $1");
		   	} else if (s.matches("(?is).*?LIVING\\s+TR(UST)?\\s+TRUSTEES.*")){
		   		s = s.replaceAll("\\b(.+)\\s+(LIVING\\s+TR)\\s+(TRUSTEES)\\b", "$1 $3 @@ $1 $2");//SMITH HAROLD D ETUX DESSIE LIVING TR TRUSTEES
		   		s = s.replaceAll("\\s+", " ");
		   	} else {
		   		s = s.replaceAll("(?is)\\A\\s*(\\w+\\s+\\w+(?:\\s+[A-Z])?\\s+ETUX(?:\\s+\\w+)?)\\s+(LIVING\\s+TR(?:UST)?)\\b", "$1 @@ $1 $2");//SMITH ARLEY C ETUX  REVOCABLE LIVING TRUST
		   	}
			s = s.replaceAll("\\bETUX\\s+(\\w+\\s+LIVING\\s+TR(?:UST)?)\\b", "#@# $1");//SMITH HAROLD D ETUX DESSIE LIVING TR TRUSTEES
		   	
		   	if (s.matches(".*ETUX\\s+[A-Z]+\\s+[A-Z].*")) {
		   		s = s.replaceFirst("\\s{2,}", " ");
		   	}
		   	if (s.matches(".*&\\s+[A-Z]+\\s+[A-Z]+\\s+&.*")) {
		   		s = s.replaceFirst("\\s{2,}", " ");
		   	}
		   	if ((!s.contains(" ETAL ")) && (!s.matches(".*ETUX\\s{2,}.*"))) {
		   		s = s.replaceAll("\\s{2,}", " @@ ");
		   	} else {
		   		s = s.replaceAll("\\s{3,}", " ");
		   	}
		   	
		   	s = s.replaceAll("C/O", "@@");

		   	if (!(s.contains("ETAL")) && s.contains(" AND ")) {
		   		s = s.replaceAll(" AND ", " @@ ");
		   	}
		   	
		   	if (s.matches(".*ETUX\\s+([A-Z]+)?\\s?([A-Z]+)\\s+&\\s+([A-Z]+)\\s+([A-Z]+)\\s+([A-Z]+)?")){
				s = s.replaceFirst("&", "@@");
		   	}
		   	if (s.matches(".*ETUX\\s+[A-Z]+(\\s+[A-Z]+)?\\s+ETAL\\s+[A-Z]+.*")) {//tn williamson ep 	SMITH JOHN W ETUX MARY ETAL CLAYBROOKS ROBBIE 
		   		s = s.replaceFirst("(?is)(.*ETUX\\s+[A-Z]+(?:\\s+[A-Z]+)?\\s+ETAL)\\s+([A-Z]+.*)", "$1 @@ $2");
		   	}
		   	if (s.matches("(?is)(\\w+)\\s\\w+(?:\\sJR|SR)?\\s+\\bETUX\\b\\s(\\w+\\s[A-Z])\\Z")) {//TN Polk TR 	WILSON PARK ETUX KATHERINE M
		   		s = s.replaceFirst("(?is)((\\w+)\\s\\w+(?:\\sJR|SR)?)\\s+\\bETUX\\b\\s(\\w+\\s[A-Z])\\Z", "$1 @@ $3 $2");
		   	}
		   	s = s.replaceFirst("(?is)(ETAL)\\s+(\\w+)\\s*&\\s*(\\w+\\s+\\w+)", "$1@@$3 & $2");
			
		   	//if (s.matches("(.+)\\s+ETAL\\s+(\\w+)\\s+ETAL.+")){
			//	s = s.replaceFirst("ETAL", "ETUX"); //WILLIAMSON J B ETAL  EDDIE ETAL LARRY WILLIAMSON
			//}
		   	//s = s.replaceAll("(JOHN)\\s+ETAL\\s+(LORI)", "$1 & $2");
		   	String[] owners ;
		   	String[] own = null;
		   	owners = s.split("\\s*@@\\s*");
		  
		   	List<List> body = new ArrayList<List>();
		   	String[] names = {"", "", "", "", "", ""};
		   	String[] suffixes, type, otherType;
		   	String ln="";
				
		   	for (int i=0; i<owners.length; i++){
		   		String ow = owners[i];
		   		ow = ow.replaceAll("U/D/T", "");
		   		if ("williamson".equals(crtCounty.toLowerCase())){
		   			names = StringFormats.parseNameNashville(ow, true);
				} else {
					if (i == 0){
						if (NameUtils.isCompany(ow)){
							names[2] = ow;
							type = GenericFunctions.extractAllNamesType(names);
							otherType = GenericFunctions.extractAllNamesOtherType(names);
							GenericFunctions.addOwnerNames(names, "", "", type, otherType,
									true, false, body);
							continue;
						} else {
							names = StringFormats.parseNameNashville(ow, true);
							ln = names[2];
							if (names[5].length() > 0){
								ln = names[5];
							}
						}
					}  	
					if (i > 0) {
						String firstWord = ow.replaceAll("(?is)\\A\\s*(\\w+)\\s+.*", "$1");
						if (firstWord.trim().matches(ln)){
							names = StringFormats.parseNameNashville(ow, true);
						} else {
							if (ow.contains(" AND ")) {
								own = ow.split(" AND "); 
							} else if (NameUtils.isCompany(ow)) {
								names[5] = ow.trim();
							} else {
								ow = ow.replaceAll("SAVELLA & TERRANCE", "SAVELLA @ TERRANCE"); // EDWARDS GWENDOLYN ETAL LUTHER,SAVELLA & TERRANCE i think
																										// the second owner is a company name
								ow = ow.trim();
								if (ow.matches("ETUX \\w+")) {
									ow = ow.replaceFirst("(ETUX .*)", "$1 " + ln);
								}
								names = StringFormats.parseNameFML(ow, new Vector<String>(), true, true);
								names[2] = names[2].replaceFirst("SAVELLA @ TERRANCE", "SAVELLA & TERRANCE");
							}
							if (own != null){
								for (int j = 0; j < own.length; j++) {
									ow = own[j];
									if (j == 0) {
										names = StringFormats.parseNameDesotoRO(ow, true);
									} else {
										String[] name = StringFormats.parseNameDesotoRO(ow, true);
										names[3] = name[0];
										names[4] = name[1];
										names[5] = name[2];
									}
								}
							}
						}
					}
				}
		   		names[2] = names[2].replaceAll("#@#", "AND");
		   		if (names[1].toLowerCase().endsWith(" broach") && names[2].equalsIgnoreCase("anderson")) {	//T7370
		   			names[1] = names[1].replaceFirst("(?is)\\sBROACH\\z", "");
		   			names[2] = "BROACH " + names[2];
		   		}
		   		type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions1.extractNameSuffixes(names);
				
		   		GenericFunctions1.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
		   												NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
		   	GenericFunctions1.storeOwnerInPartyNames(m, body, true);
		
	   }
	
	@SuppressWarnings("deprecation")
	public static void taxTNGenericTR(ResultMap m, long searchId) throws Exception {
		   
		   String baseAmt = (String) m.get("TaxHistorySet.BaseAmount");
		   String amtPaid = (String) m.get("TaxHistorySet.AmountPaid");
		   
		   if (baseAmt != null && baseAmt.length() != 0 && amtPaid != null && amtPaid.length() != 0){
			   String totalDueTotal = new BigDecimal(baseAmt).subtract(new BigDecimal(amtPaid)).toString();
			   m.put( "TaxHistorySet.TotalDue", totalDueTotal);
			   m.put( "TaxHistorySet.CurrentYearDue", totalDueTotal);
			   
			   String totalPriorBaseAmt = GenericFunctions1.sum((String) m.get("tmpPriorBaseAmt"), searchId);
			   String totalPriorAmtPaid = GenericFunctions1.sum((String) m.get("tmpPriorAmtPaid"), searchId);
			   
			   BigDecimal priorDelinq = new BigDecimal(totalPriorBaseAmt).subtract(new BigDecimal(totalPriorAmtPaid));
			   if (priorDelinq.signum() == -1){// B 4461
				   priorDelinq = new BigDecimal("0.00");
			   }
			   String tmpPriorDue = (String) m.get("tmpPriorDue");
			   if (StringUtils.isNotEmpty(tmpPriorDue)){
				   priorDelinq = priorDelinq.add(new BigDecimal(tmpPriorDue));
			   }
			   
			   m.put("TaxHistorySet.PriorDelinquent", priorDelinq.toString());
			   
			   String currentYearDelinq = "0.00";
		       String dueDat = DBManager.getDueOrPayDateBySiteName("TN" + InstanceManager.getManager()
		                       .getCurrentInstance(searchId).getCurrentCounty().getName() + "TR", "dueDate");
		       
		       if (StringUtils.isNotEmpty(dueDat)){
			       Date dueDate = new Date(dueDat);
			       Date dateNow = new Date();
			       if (dateNow.after(dueDate)){ 
			    	   currentYearDelinq = totalDueTotal;
			       }       
			       m.put( "TaxHistorySet.DelinquentAmount", new BigDecimal(currentYearDelinq).add(priorDelinq).toString());
		       }
		   }
		   
		   
	   }     
}

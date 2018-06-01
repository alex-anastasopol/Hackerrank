package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class TNShelbyAO {
    public static void partyNamesTokenizerTNShelbyAO(ResultMap m, String s) throws Exception {
    	
    	// first owner (+ spouse) is L F M and additional co-owners are F M L or L F M
    	// owners (+ spouse) are separated with 'AND'    	
    	
        // cleanup
    	s = s.replace("(THE)",""); //bug 7196
    	s = s.replaceAll("( JAC KSON )|( JAC KSON$)", " JACKSON ");	//bug 7196
    	
    	s = s.replaceAll("\\(\\s*\\d+/\\d+%\\s*\\)", "");	//KILLET JERI & TIM (1/2%) AND AMY MCQUEEN & VAN (1/2%), Parcel ID:	00101600004C
    	s = s.replaceAll("[\\(\\)]+", "");
        s = s.replaceAll("\\bAN D\\b", "AND");
        s = s.replaceFirst("\\s+AND\\s*$", "");
        s = s.replaceFirst("\\s+AND\\s+(ETAL)\\s*$", " $1");
        s = s.replaceFirst("\\s+EST(ATE( OF)?)?\\s*$", "");
        s = s.replaceFirst("(?is)(?:&|\\bAND\\b)\\s*([A-Z]+\\s+[A-Z]+)\\s+TRS\\s*$", "TR & $1 TR");
        s = s.replaceFirst("^\\s*(\\w+) AND (\\w+(?: \\w)?)\\s*$", "$1 &&& $2");	// SMITH AND ALBRIGHT (DRS) (PSO), PID 056042A00001
        s = s.replaceFirst("^\\s*(\\w+(?: \\w)?) AND (\\w+)\\s*$", "$1 &&& $2");
        s = s.replaceAll("\\s*&\\s*", " AND ");
        s = s.replaceAll("\\s{2,}", " ").trim();
        String entities[], type[], otherType[];
        if (s.matches("\\w+ \\w+ \\w (?:TR(USTEE?)?\\s+)?AND \\w+ \\w(?:\\s+TR(USTEE?)?)?")){
        	entities = new String[]{s};
        }
        else{
        	entities = s.split(" AND ");
        }
        
        List<List> body = new ArrayList<List>();
        String ln = "";
        
        // parse first entity as L F M
        String[] a = {"", "", "", "", "", ""};
        if (entities[0].contains("&&&")){
        	a[2] = entities[0].replace("&&&", "AND");
        } else {
	        a = StringFormats.parseNameNashville(entities[0], true);	        
	        // correct middle name which might be tokenized as the last name
	        if (a[5].length() != 0 && !a[5].equals(a[2]) && !a[5].contains(a[2]) && !a[2].contains(a[5]) && !a[5].contains("'")){
	        	if (GenericFunctions.nameSuffix.matcher(a[5]).matches()){	// SMITH ALISON B & ROBERT F JR, PID=G0231QG00016
	        		a[4] = (a[4] + " " + a[5]).trim();
	        	} else {									// SMITH DAVID W & JO ANNE H, PID=096517E00189
	        		a[4] = a[3] + " " + a[4];
	        		a[3] = a[5];
	        	}
        		a[5] = a[2];
	        }
	    }
        ln = a[2];
        String[] suffixes = GenericFunctions.extractNameSuffixes(a);
        type = GenericFunctions.extractAllNamesType(a);
		otherType = GenericFunctions.extractAllNamesOtherType(a);
		GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
        
        String crtName = "";
        for (int i=1; i<entities.length; i++){   
        	crtName = entities[i];        	
        	if(crtName.contains("&&&") || NameUtils.isCompany(crtName)){
        		 a[0] = ""; 
        		 a[1] = ""; 
        		 a[2] = crtName.replace("&&&", "AND");
        		 a[3] = "";
        		 a[4] = "";
        		 a[5] = "";
        		 type = GenericFunctions.extractAllNamesType(a);
        		 otherType = GenericFunctions.extractAllNamesOtherType(a);
        		 GenericFunctions.addOwnerNames(a, "", "", type, otherType, true, false, body);
        		 continue;
        	}
        	if (crtName.matches(".+ [A-Z]") && !crtName.contains("&")){	// L F M (e.g. SMITH ETHEL L AND ERVIN EDWINA P AND POWELL A G, PID 03403100020)
        		a = StringFormats.parseNameNashville(crtName, true);
    	        // correct middle name which might be tokenized as the last name
    	        if (a[5].length() != 0 && !a[5].equals(a[2]) && !a[5].contains(a[2]) && !a[2].contains(a[5]) && !a[5].contains("'")){
    	        	if (GenericFunctions.nameSuffix.matcher(a[5]).matches()){	
    	        		a[4] = (a[4] + " " + a[5]).trim();
    	        	} else {									
    	        		a[4] = a[3] + " " + a[4];
    	        		a[3] = a[5];
    	        	}
            		a[5] = a[2];
    	        }
        		suffixes = GenericFunctions.extractNameSuffixes(a);
        	} else {
            	String[] n = GenericFunctions.extractSuffix(entities[i]);
            	crtName = n[0];
            	String suffix = n[1]; 
            	crtName = crtName.replaceFirst("^([A-Z]+(?: [A-Z])*) & ([A-Z]+(?: [A-Z])*) ([^\\s]+)", "$1 $3 & $2"); 	// fix for strings as OF & WF OL
        		a = StringFormats.parseNameDesotoRO(crtName, true);	// F M L
        		suffixes = GenericFunctions.extractNameSuffixes(a);
        		type = GenericFunctions.extractAllNamesType(a);
       		 	otherType = GenericFunctions.extractAllNamesOtherType(a);
        		if (a[5].length() == 0){	// there is just owner, no spouse
        			suffixes[0] = suffixes[0] + " " + suffix;
        			suffixes[0] = suffixes[0].trim();
        		} else { 
        			suffixes[1] = suffixes[1] + " " + suffix;
        			suffixes[1] = suffixes[1].trim();
        		}
        	}    
        	if (a[0].length() <= 1 && a[1].length() == 0 && !NameUtils.isCompany(a[2])) {//B 4175
        		a[1] = a[0];
    			a[0] = a[2];
    			a[2] = ln;
    		}
        	
        	if (a[0].length() > 1 && a[1].length() == 0 && !LastNameUtils.isLastName(a[2])){//B 4489
        		a[1] = a[2];
        		a[2] = ln;
        	}
        	GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);
        }
        
        GenericFunctions.storeOwnerInPartyNames(m, body, true);                
    }
    
    public static void cleanLotShelby(ResultMap m,long searchId) throws Exception{
        String tmpLot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
        if (tmpLot==null) {
            return;
        }
        
    	tmpLot = tmpLot.trim();        
    	// if lot ends with "&", then remove it        
        if (tmpLot.endsWith("&")) {
            tmpLot = tmpLot.substring(0, tmpLot.length()-2);
        	tmpLot = tmpLot.trim();             
        }
           
        //cazul 38&PT37 & 1383 SILVER
        if (tmpLot.matches("\\d+\\s*&PT\\s*\\d+")) {
            tmpLot=tmpLot.replaceAll("(\\d+)\\s*&PT\\s*(\\d+)","$1"+" "+"$2");
        }
        
        // 1389 Woodbine
        tmpLot = tmpLot.replaceAll("(?i)\\b(\\d+)THRU\\b", "$1");
        
        // 2784  AMSDEN  
        tmpLot = tmpLot.replaceAll("(?i)(\\d+)\\s*&\\s*[EWSN]\\b", "$1");
        
        // 1246  FAXON & 1521 BROOKINS
        tmpLot = tmpLot.replaceAll("(?i)\\b[EWSN]\\s*PT\\s+(\\d+)\\b", "$1");
        
        tmpLot = tmpLot.replaceAll("\\s+", " ");
        
        m.put("PropertyIdentificationSet.SubdivisionLotNumber",tmpLot);        
    }
    
    public static void lotNumberTNShelby(ResultMap m,long searchId) throws Exception {
        String tmp = (String) m.get("tmpAddress");
        String tmp2 = (String) m.get("tmpSubdivision");

        String tmpL = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
        if (!StringUtils.isEmpty(tmpL)){
        	tmpL = cleanLotTNShelbyAO(tmpL.trim());
        	m.put("PropertyIdentificationSet.SubdivisionLotNumber", tmpL);
        }
        //lot                          
        if ((tmp != null)) {

	        String lotFromUnit = "";
	               
	            int t;
	            
	            t=tmp.indexOf("#");
	            if (t!=-1) {
		            t++;
		            
		            while ((t<tmp.length())&& tmp.charAt(t)!=' ') {
		                lotFromUnit += tmp.charAt(t);
		                t++;
		            }
		            
		            String tmpLot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");		            
		            if (tmpLot != null) {
		            	tmpLot = tmpLot + " " + lotFromUnit;
		            } else {
		            	tmpLot = lotFromUnit;
		            }

			        try {
			            m.put("PropertyIdentificationSet.SubdivisionLotNumber",tmpLot);
			        } catch (Exception e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
			        }    		            
	            }            
        }
                          
        //block
        if ((tmp2 != null)) {
	        String block = "";
	        String blkSeparators[] = {"BLK"};
			int blkIndex;
			for (int i=0;i<blkSeparators.length;i++) {
			    if (tmp2.indexOf(blkSeparators[i])!=-1) {
			        blkIndex = blkSeparators[i].length() + tmp2.indexOf(blkSeparators[i]);
					if (blkIndex == tmp2.length()) {
						String subdivName = (String) m.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName());
						if (StringUtils.isNotEmpty(subdivName)) {
							subdivName = subdivName.substring(0, subdivName.indexOf(blkSeparators[i]));
							m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivName);
						}
					} else if (blkIndex < tmp2.length()) {
						while (tmp2.charAt(blkIndex)==' ') blkIndex++;
					}
			        int end = tmp2.indexOf(" ",blkIndex);
			        if (end ==-1) end = tmp2.length();
			        block = tmp2.substring(blkIndex, end);
			        block = block.trim();
			    }
			}
			m.put("PropertyIdentificationSet.SubdivisionBlock",block);			
        }             
    }
    
    
    public static String cleanLotTNShelbyAO(String s){
    	s = s.replaceAll("\\bPT\\b", "");
    	s = s.replaceAll("\\s{2,}", " ");
    	s = s.replaceAll("(?is)\\s+0", "");
    	s = s.replaceAll("(?is)\\A0+", "");
    	return s.trim();
    }
    public static void bookAndPageShelbyAO(ResultMap m,long searchId) throws Exception{
        ResultTable tmp = (ResultTable) m.get("SaleDataSet");
        if (tmp==null) {
            return;
        }
        int k=0;
        boolean isInstrNo = false;
        boolean has2Pages = false;
        String platBook = "";
        String platPage1 = "";
        String platPage2 = "";
        
        String tmpBP = (String) m.get("tmpBP");
        if (tmpBP!=null) {      
        	
        	tmpBP = tmpBP.trim();
        	
        	// check if the book&page string is actually an instrument number
            if (tmpBP.matches("^\\w+$")) {
            	k=1;
            	isInstrNo = true;
            } else {

            	// check if book&page string contains 2 pages, i.e. BP = book-page1&page2
            	Matcher matcher = Pattern.compile("(\\w+)\\s*-\\s*(\\w+)\\s*&\\s*(\\w+)").matcher(tmpBP); 
            	if (matcher.find()) {
	                k=2;
	                has2Pages = true; 
	                platBook = matcher.group(1);
	                platPage1 = matcher.group(2);
	                platPage2 = matcher.group(3);
	            }
            }
        }
        
        if (isInstrNo || has2Pages) {
            m.put("PropertyIdentificationSet.PlatBook","");
            m.put("PropertyIdentificationSet.PlatNo","");        
        }
               
        String head[] = {"Date","InstrNo","InstrType","Book","Page"};
        String body[][] = tmp.getBodyRef();
        String newBody[][]=new String[tmp.getBodyRef().length+k][5];
       
        
        for (int i = 0;i<tmp.getBodyRef().length;i++) {
            for (int j=0;j<3;j++) {
                newBody[i][j]=body[i][j];
            }
            
            String book="";
            String page="";
            String instrNo=newBody[i][1];
            int indx = instrNo.indexOf("-");
            if (indx!=-1) {
                book = instrNo.substring(0,indx);
                page = instrNo.substring(indx+1,instrNo.length());
            }
            
            if (book.matches("[0-9]+")&& page.matches("[0-9]+")) {
                newBody[i][3]=book;
                newBody[i][4]=page;
                newBody[i][1]="";
            }else {
                newBody[i][3]="";
                newBody[i][4]="";

            }
        }

        // lastLine=index of the last processed line in table
        int lastLine = tmp.getBodyRef().length - 1; 
        
    	//if book&page is actually an instrument number, the instr# should be added to SaleDataSet        
        if (isInstrNo) {            
        	lastLine ++;        	
            for (int j=0;j<5;j++) {
                newBody[lastLine][j]="";
            }            
            newBody[lastLine][1]=tmpBP;
            
        } else {                
	        //if book&page actually contains 2 pages, the pages should be added to SaleDataSet
	        if (has2Pages) {
	        		lastLine ++;
	                for (int j=0;j<3;j++) {
	                    newBody[lastLine][j]="";
	                }
	                newBody[lastLine][3] = platBook;
	                newBody[lastLine][4] = platPage1;
	                
	        		lastLine ++;                
	                for (int j=0;j<3;j++) {
	                    newBody[lastLine][j]="";
	                }
	                newBody[lastLine][3] = platBook;
	                newBody[lastLine][4] = platPage2;
	        }
        }      
        
        ResultTable rt = new ResultTable();
        rt.setHead(head);
        HashMap hm = (HashMap) tmp.getMapRefference();
        hm.put("Book", new String[] {"Book", "iw"} );
        hm.put("Page", new String[] {"Page", "iw"} );
        rt.setMap(hm);
        
        rt.setBody(newBody);
        m.put("SaleDataSet",rt);

        
    }    
    
}

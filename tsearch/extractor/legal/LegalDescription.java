/*
 * Created on Nov 10, 2005
 *
 */
package ro.cst.tsearch.extractor.legal;

import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.utils.RegExUtils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

/**
 * @author vladp
 *
 */
public class LegalDescription {
   
	/**
	 * Extracts tracts and multiple tracts from remarks separated by space.
	 * For now just for TP sites. (e.g DT <ol>   
	 * 											<li> 61/104 TRACTS 25 AND 26 </li>     
	 * 											<li> REL DT 69/144 SEE DOC PT TRACT 3 </li>
	 * 											<li>MEMO; TR1:1527AC%SEVERAL ABSTRACTS TR2:0.04089AC%A227 TR3:0.7162AC%A227</li>
	 * 											<li>! % 214.84AC TR 1 %432AC DR</li>
	 * 									  </ol>	) 
	 * @param tmp
	 * @return
	 */
	public static String extractTractFromText(String tmp){
		tmp = tmp.replaceAll("(\\d) AND (\\d)", "$1 $2");
		
		String regex = "(?<=TRACTS?\\s)(\\d*\\s?)+";
		String tract = RegExUtils.parseValuesForRegEx(tmp, regex);
		regex = "(?<=TR) ?\\d+";
		String newTract = RegExUtils.parseValuesForRegEx(tmp, regex);
		
		if(ro.cst.tsearch.utils.StringUtils.isEmpty(tract)) {
			if(ro.cst.tsearch.utils.StringUtils.isEmpty(newTract)) {
				return "";
			} else {
				return newTract.trim();
			}
		} else {
			if(ro.cst.tsearch.utils.StringUtils.isEmpty(newTract)) {
				return tract.trim();
			} else {
				return tract.trim() + " " + newTract.trim();
			}
		}
	}

	
    public static String extractLotFromText(String tmp) {
    	if(tmp == null) {
    		return "";
    	}
    	tmp = tmp.toUpperCase();
    	
		//cleanup
    	tmp = tmp.replaceAll("\\d+-\\d+-\\d+-\\d+", "");
    	tmp = tmp.replaceAll("(\\d)-(\\d)", "$1 - $2"); // to allow lots as <number>-<letter> to be treated as a single lot, not lot interval - Jacskon CnT PID 70-940-08-23
    	tmp = tmp.replaceAll("(?i)\\bAND\\b", "&"); // Jackson CnT PID 47-340-05-10-00-0-00-000, bug 420    	
    	tmp = tmp.replaceAll("\\s*&\\s*", " & ");    	
    	tmp = tmp.replaceAll(";", " "); // Jackson CnT PID 25-810-02-28-02-0-00-000
    	tmp = tmp.replaceAll("-{3,}"," ");    // Jackson CNT 2321 MADISON AVENUE     
    	tmp = tmp.replaceAll(",(?=[^\\s])", " ");
    	tmp = tmp.replaceAll("\\s{2,}", " ").trim();
    	tmp = tmp.replaceAll("\\n", " ");
    	tmp = tmp.replaceAll("\\s*\\bLO?TS? (THRU|TH|TO)\\b\\s*", " "); // Clay CiT PID CL1331900150090001, bug #993
    	tmp = tmp.replaceAll("\\b\\w+LT\\b", ""); // 103/619 REINSTMNT AGRE AFTER DEFLT;1AC/2 
    	tmp = tmp.replaceAll("LT #", "");//TXSan Patricio instr  495482
    	tmp = tmp.replaceAll("LT V/L", "");//TXSan Patricio instr 825018
    	tmp = tmp.replaceAll("%", " ");//TXNueces 1996019891
    	tmp = tmp.replaceAll("/", " ");//TXNueces  866811
    	tmp = tmp.replaceAll("-LOTS", " LOTS ");//TX Comal “Subdivided Search” with: Lt: 1898, Plat Bk:2 , Plat Pg: 37
    	tmp = tmp.replaceAll("(?i)\\bLOT:", "LOT");//TX Comal “Subdivided Search” with: Lt: 1898, Plat Bk:2 , Plat Pg: 37 
    	
    	// don't consider lot interval if first token is letter and second is digit or viceversa // Jackson CnT PID 31-740-05-34-00-0-00-000, bug #296 
    	Matcher m = Pattern.compile("(.)\\s+(THRU|TH|TO)\\s+(.)").matcher(tmp);
    	while(m.find()){
    		String c1 = m.group(1);
    		String c2 = m.group(3);
    		if (!((c1.matches(".*\\d") && c2.matches("[A-Z].*")) || (c2.matches("\\d.*") && c1.matches(".*[A-Z]")))){
    			try {
    				tmp = tmp.replaceFirst(Pattern.quote(c1+"\\s+(THRU|TH|TO)\\s+"+c2), Matcher.quoteReplacement(c1+" - "+c2));
    			} catch (Exception e) {
					e.printStackTrace();
				}
    		}
    	}    	
        tmp = tmp.replaceAll("\\bRES(URVEY)?\\b(\\s*OF\\s+(THE\\s+)?)?", "");
        tmp = tmp.replaceAll("\\bREPLAT\\b(\\s*OF\\s+(THE\\s+)?)?", "");    	
		tmp = tmp.replaceFirst("\\bCORR? (?:PLAT\\s)?(?:OF\\s)?LO?TS?.*(\\bLO?TS?\\b.+)", "$1");  // Jackson CNT PID 26-720-09-08-00-0-00-000, 29-220-06-08-00-0-00-000; JA CiT 3031 BELL ST 
		tmp = tmp.replaceAll("\\bPR?TS? (?:OF )?(?=LO?TS?)", ""); // Jackson CNT PID 29-220-06-08-00-0-00-000
		tmp = tmp.replaceAll("(?<=LO?TS? )PR?TS? (?:OF )?", "");
		tmp = tmp.replaceAll("LO?TS?\\s(?=LO?TS?)", ""); //Jackson CNT PID 29-220-06-08-00-0-00-000
		
		// when LOT or LT is used with multiple lot values, use LOTS/LTS instead, to allow extracting all lots value, not just the first 
		tmp = tmp.replaceAll("(\\bLO?T)(?=\\s+\\d+ (-|&) \\d+)", "$1S"); //Clay RO Instr# R81561
        tmp = tmp.replaceAll("(\\bLO?T)(?=\\s+\\d+\\s+[A-Z] OF\\b)", "$1S");  // Clay RO Instr# R81524
        
        //replace AND and "&" when they are used as separators(e.g LOTS 2 & 3 FPA1; LOTS 14 AND 15)
        tmp = tmp.replaceAll("LO?TS?\\s(\\d*,?(\\s?(&|AND)\\s?)\\d*)\\2", ",");
        tmp = tmp.replaceAll("(\\b?LO?T)(\\s+\\d+(-|&|,)\\d+)", "$1S");
        if (Pattern.compile("(\\bLO?T)(\\s+\\d+(\\s)\\d+\\b)").matcher(tmp).find()){
        	tmp = tmp.replace("LT", "LTS");
        	tmp = tmp.replace("LOT", "LOTS");
        }
        tmp = tmp.replaceAll("((\\d)(?=-REL))","$1 ");
        
		tmp = tmp.replaceAll("\"", ""); //Clay RO Instr# G85080 
		if (!tmp.matches(".*\\bLO?TS?\\b.*") && tmp.matches("^O?TS? (\\d|[A-Z]).*")) { //Clay RO Instr# 2005007519
        	tmp = "L"+tmp;
        }
		
		tmp = tmp.replaceAll("\\bLO?T\\b.+\\bEX\\b", ""); //Jackson CnT PID 32-830-10-27-00-0-00-000
		tmp = tmp.replaceAll("\\bLO?TS (\\d+)$", "LOT $1"); //Jackson CnT PID 62-420-06-12-00-0-00-000, bug #981
				    	
        int lotIndex=-1;
		//if we have LT5
        tmp = tmp.replaceAll("LT(\\d+)","LT $1");
        
		String [] lotSeparators = {" LOT ", "LT "};
		int []indexDelims = new int[lotSeparators.length];
		String lot = "";
		String multipleLots = "";
			
		int minIndex = 0;
		
		for (int i=0; i<lotSeparators.length;i++) {
		    indexDelims[i] =  tmp.indexOf(lotSeparators[i]);
		    if (indexDelims[i]==-1) indexDelims[i]= tmp.length() +1;
		}
		for(int i=0;i<lotSeparators.length;i++) {
		    if (indexDelims[i] < indexDelims[minIndex]) minIndex= i;
		}

		lotIndex = tmp.indexOf(lotSeparators[minIndex]);

		if (lotIndex !=-1) {
			    lotIndex = lotIndex + lotSeparators[minIndex].length();
			    while (tmp.charAt(lotIndex)==' ') lotIndex++;	
				if (tmp.indexOf(" ",lotIndex)!=-1) {
				    lot = tmp.substring(lotIndex,tmp.indexOf(" ",lotIndex));
				}else {
				    lot = tmp.substring(lotIndex,tmp.length());
				}
				lot = lot.replaceAll(","," ");
				lot=lot.trim();
				
				//check for multiple lot
				if (lotSeparators[minIndex].equals(" LOT ")) {
				    
				    int lotIndex2;
				    int lotEndIndex;
				    String lotStr = " LOT ";				    
				    lotIndex2 = tmp.indexOf(lotStr,lotIndex+1);
				    if (lotIndex2 == -1){
				    	lotStr = " LT ";				    	
					    lotIndex2 = tmp.indexOf(lotStr,lotIndex+1);
				    }
				    while (lotIndex2!=-1) {
				        lotIndex2 = lotIndex2 + lotStr.length();
				        lotEndIndex = tmp.indexOf(" ",lotIndex2+1);
				        if (lotEndIndex!=-1) {
				        	String newLot = tmp.substring(lotIndex2,lotEndIndex);
				        	if(!newLot.matches("[A-Z]{3,}")) {
				        		lot = lot + " "+newLot;
				        	}
				            lotIndex2 = tmp.indexOf(lotStr,lotEndIndex+1);
				        }else {
				            lot = lot + " "+tmp.substring(lotIndex2,tmp.length());
				            lotIndex2 =-1;
				        }
				    }
				}else {
				    if (lotSeparators[minIndex].equals("LT ")) {
					    
					    int lotIndex2;
					    int lotEndIndex;
					    String lotStr = "LT ";
					    lotIndex2 = tmp.indexOf(lotStr,lotIndex+1);
					    if (lotIndex2 == -1) {
					    	lotStr = "LOT ";
					    	lotIndex2 = tmp.indexOf(lotStr,lotIndex+1);
					    }
					    while (lotIndex2!=-1) {
					        lotIndex2 = lotIndex2 + lotStr.length();
					        lotEndIndex = tmp.indexOf(" ",lotIndex2+1);
					        if (lotEndIndex!=-1) {
					            lot = lot + " "+tmp.substring(lotIndex2,lotEndIndex);
					            lotIndex2 = tmp.indexOf(lotStr,lotEndIndex+1);
					        }else {
					            lot = lot + " "+tmp.substring(lotIndex2,tmp.length());
					            lotIndex2 =-1;
					        }
					    }
					}
				}
				lot = lot.trim();
				//END check for multiple lot							
		}
			    
	    //LOTS multiple sau interval
		int startIdx = 0;
		// TODO  when "LOTS" appear they should be in this form " LOTS " and thus remove (tmp.indexOf("LOTS ", startIdx); 
		//find examples where "LOTS " is used  
	    while ((startIdx<tmp.length()) && ((tmp.indexOf(" LOTS ", startIdx)!=-1)||(tmp.indexOf("LOTS ", startIdx)!=-1))) {
	    	int lotStartIndex=tmp.indexOf(" LOTS ", startIdx);
	    	if (lotStartIndex==-1){
	    		lotStartIndex= tmp.indexOf("LOTS ", startIdx);
	    		tmp=tmp.replaceFirst("LOTS ", " LOTS ");
	    	}
	        lotIndex = lotStartIndex + " LOTS ".length();
		    
		    while (lotIndex < tmp.length() && tmp.charAt(lotIndex)==' ') lotIndex++;
	        boolean b1=true,b2=true;
	        String s1="";		    
			if (tmp.indexOf(" ",lotIndex)!=-1) {
			    int lotEndIndex;
			    lotEndIndex = tmp.indexOf(" ",lotIndex);
			    if (tmp.indexOf(" ",lotEndIndex+1)!=-1) {
			        Boolean found = false;
		        	do{
		        		int idx = tmp.indexOf(" ",lotEndIndex+1);
		        		if(idx == -1){
		        			idx = tmp.length();
		        		}
		            	s1= tmp.substring(lotEndIndex+1, idx);
		            	s1 = s1.replace(",", "");
		            	
		            	//if a letter follows a lot, consider it as being a lot value 
		            	//but only if the letter is not a cardinal point followed by a number or by OF
		            	b1 = s1.matches("[A-Z&-]{1}");
		            	if (b1 && s1.matches("[ESWN]")){
		            		if(tmp.length()-1 >= idx+1)
		            			b1 = !String.valueOf(tmp.charAt(idx+1)).matches("\\d");
		            		if(tmp.length()-1 >= idx+2)
		            			b1 = b1 && !"OF".equalsIgnoreCase(tmp.substring(idx+1, idx+3));
		            	}
		            	
		            	//b1 = s1.matches("[A-Z&-]{1}") && !(s1.matches("[ESWN]") && String.valueOf(tmp.charAt(idx+1)).matches("\\d"));
		            	b2 = s1.matches("\\d+") && !(String.valueOf(tmp.charAt(idx-1)).matches("[ESWN]"));
		            	if (b1||b2) {
		            		lotEndIndex = idx;
		            		found = true;
		            	}
				    }while((b1||b2) && (lotEndIndex<tmp.length()));
		        	
				    if (found) {
				        multipleLots = multipleLots + " " + tmp.substring(lotIndex,lotEndIndex);
				    }
				    startIdx = lotEndIndex;				        				        				    	
			    }else {
			        multipleLots = multipleLots + " " + tmp.substring(lotIndex,tmp.length());
			        startIdx = tmp.length();
			    }
			    			    
			}else {
			    multipleLots = multipleLots + " " + tmp.substring(lotIndex,tmp.length());
			    startIdx = tmp.length();
			}
			multipleLots=multipleLots.trim();			
			
		//END LOTS multiple sau interval
			
	    } 
	    // 	LTS multiple sau interval
		startIdx = 0;
	    while ((startIdx<tmp.length()) && ( (tmp.indexOf("LTS ", startIdx)!=-1) )) { 
	        if ( (tmp.indexOf("LTS ")!=-1) ) {	        	
	        	lotIndex = tmp.indexOf("LTS ", startIdx) + "LTS ".length();
			    
			    while ( lotIndex < tmp.length() && tmp.charAt(lotIndex)==' ' ) lotIndex++;
		        boolean b1=true,b2=true;
		        String s1="";			    
				if (tmp.indexOf(" ",lotIndex)!=-1) {
				    int lotEndIndex;
				    lotEndIndex = tmp.indexOf(" ",lotIndex);
				    if (tmp.indexOf(" ",lotEndIndex+1)!=-1) {
				        Boolean found = false;				        
			        	do {
			        		int idx = tmp.indexOf(" ",lotEndIndex+1);
			        		if(idx == -1){
			        			idx = tmp.length();
			        		}			        		
			            	s1= tmp.substring(lotEndIndex+1, idx);
			            	
			            	//if a letter follows a lot, consider it as being a lot value 
			            	//but only if the letter is not a cardinal point followed by a number or by OF
			            	b1 = s1.matches("[A-Z&-]{1}");
			            	if (b1 && s1.matches("[ESWN]")){
			            		if(tmp.length()-1 >= idx+1)
			            			b1 = !String.valueOf(tmp.charAt(idx+1)).matches("\\d");
			            		if(tmp.length()-1 >= idx+2)
			            			b1 = b1 && !"OF".equalsIgnoreCase(tmp.substring(idx+1, idx+3));
			            	}
			            	
//			            	b1 = s1.matches("[A-Z&-]{1}") && !(s1.matches("[ESWN]") && String.valueOf(tmp.charAt(idx+1)).matches("\\d"));				            	
			            	b2 = s1.matches("\\d+") && !(String.valueOf(tmp.charAt(idx-1)).matches("[ESWN]"));
			            	if (b1||b2) {
			            		lotEndIndex = idx;
			            		found = true;
			            	}
					    }while((b1||b2) && (lotEndIndex<tmp.length()));
			        	
					    if (found) {
					        multipleLots = multipleLots + " " + tmp.substring(lotIndex,lotEndIndex);
					    }
					    startIdx = lotEndIndex;
				    }else {
				        multipleLots = multipleLots + " " + tmp.substring(lotIndex,tmp.length());
				        startIdx = tmp.length();
				    }
				    				    
				}else {
				    multipleLots = multipleLots + " " + tmp.substring(lotIndex,tmp.length());
				    startIdx = tmp.length();
				}
		    }
	    }
	    // END LTS multiple sau interval
			    		
		lot = lot.replaceAll(",", ""); //PID 07913001900200 Clay
		lot = lot.replaceAll("\\s*&\\s*$","");
		lot = lot.replaceAll(" - ", "-").trim();
		
		multipleLots = multipleLots.replaceAll("\\)","");
		multipleLots = multipleLots.replaceAll(",", "");
		multipleLots = multipleLots.replaceAll("&","");
		multipleLots = multipleLots.replaceAll("\\s*-\\s*", "-").trim();
		multipleLots = multipleLots.replaceAll("-$", "").trim(); // fix for bug #1855
		//odd remark for lot (PT%LTS V/L #). have to gather more examples to make rule
		multipleLots = multipleLots.replaceAll("V/L #", "").trim();
		// if lot value is not included in multipleLots interval, then return lot + multipleLots
		// else returns only lot
		String resultLot = lot+" "+multipleLots;
		/*
		if (lot.length() != 0 && lot.matches("\\d+")) {
			if (multipleLots.length() != 0) {
				if (multipleLots.matches(".*\\b"+lot+"\\b.*")) {
					resultLot = lot;				
				} else {
		            // TODO: aici trebuie inlocuita aceasta verificare cu functiile de parsare/validare 
		            // de loturi din ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval (.contains(), .related())
		            lot = lot.replaceAll("(.*?)(\\d+)(.*)", "$2");
		            
		            int iLot = Integer.parseInt(lot);
		            
			        Pattern range=Pattern.compile("(?i)\\b(\\d+)\\s*-\\s*(\\d+)\\b");
			        Matcher ma=range.matcher(multipleLots);
			        while(ma.find())
			        {
			            int start=Integer.parseInt(ma.group(1));
			            int end=Integer.parseInt(ma.group(2));
			            
				        if(start<=iLot && end>=iLot) {
				        	return lot;
						} 
//				        	else {
//							if (iLot < start) {
//								return lot+" "+multipleLots;
//							} else {
//								return multipleLots+" "+lot;
//							}
//						}
					} 
			        resultLot = lot+" "+multipleLots;					
				}
			} else {
				resultLot = lot;
			}
		} else if(lot.length() == 0){
			resultLot = multipleLots;
		} 
		*/
		resultLot = StringFormats.RemoveDuplicateValues(resultLot);

		return resultLot;
    }
    
    /*
     * Process a string that contains lot or block or other values: there can be enumeration or intervals or duplicate  values, that will be processed
     * into an enumeration or interval, if possible.
     */
    public static String cleanValues(String val, Boolean onlyNumbers, Boolean removeDuplicates) {
    	    	
    	if(val != null && val.length() > 0) {
    		
    		val = val.replace(",", " ");
    		    	
	    	//  remove literal values, when values are supposed to be only numerical
	    	if(onlyNumbers){
	    		val = val.replaceAll("[A-Z]+", "");
	    		val = val.replaceAll("\\s{2,}", " ");
	    	}
	    	if (removeDuplicates){
	    		// if lot string contains more values than one numbers interval, the interval(s) must be expanded; 
	    		// this is needed also when the lot string contains more intervals which may intersect  
	    		int iterations = 0;
	    		if(!val.matches("\\d+(\\s*-\\s*\\d+)?")) {
	    			val = val.replaceAll("(\\d)-([A-Z])", "$1__$2");
	    			val = val.replaceAll("([A-Z])-(\\d)", "$1__$2");
	    			while(iterations <= 100 && val.matches(".*\\b\\d+\\s*-\\s*\\d+\\b(?!-?[A-Z]).*")){
	    				val = StringFormats.ReplaceIntervalWithEnumeration(val); 
	    				iterations ++;
		    		}
	    			val = StringFormats.RemoveDuplicateValues(val);
	    			val = StringFormats.ReplaceEnumerationWithInterval(val);
	    			val = val.replaceAll("__", "-");
	    		}
	    	}
    	}
    	return val;
    }
    
    public static String extractLotFromText(String tmp, Boolean onlyNumbers, Boolean removeDuplicates) {
    	
    	String lot = extractLotFromText(tmp);
    	
    	return cleanValues(lot, onlyNumbers, removeDuplicates);
    }
    
    public static String prepare(String s, String before[] , String after[]) {
        if (before.length!=after.length) {
            return s;
        }
        for (int i=0;i<before.length;i++) {
            s = s.replaceAll(before[i],after[i]);
        }
        return s;
    }
    
    public static String cleanGarbage(String g[], String s) {
        
        for (int i=0;i<g.length;i++) {
            s = s.replaceAll(g[i],"");
        }
        s = s.replaceAll("\\s{2,}", " ");
        return s;
    }
    
    public static String extractSubdivisionNameUntilToken(String tmp, String tokens[]) {
        
        int []indexes = new int[tokens.length];
		int first = 0;
		String r = "";
		int len = tmp.length();
		for (int i=0; i<tokens.length;i++) {
		    indexes[i] =  tmp.indexOf(tokens[i]);		    
		    if (tokens[i].matches(".+\\w$")) 
		    	if (indexes[i]+tokens[i].length() <=len-1 && !String.valueOf(tmp.charAt(indexes[i]+tokens[i].length())).matches("\\b")) 
		    		indexes[i] = len + 1;		    
		    if (indexes[i]==-1) indexes[i]= len +1;
		}
		first = 0;
		
		for (int i=0; i<tokens.length;i++) {
		    if (indexes[i]<indexes[first]) first = i;
		}
		
		int index = tmp.indexOf(tokens[first]);
        
		if (index > -1) {
		    r = tmp.substring(0, index);
		    r = r.replaceFirst("\\s*&\\s*$", "");
		    r = r.trim();
		    return r;
		}
		else {
		    return tmp;
		}
    }
    
    public static String extractSubdivisionNameByCleaning(String tmp, String tokens[]) {
        String s = tmp;
        for (int i=0;i<tokens.length;i++) {
            s = s.replaceAll(tokens[i],"");
        }
        return s;
    }
    
    public static String extractBlockFromText(String tmp) {
    	if(tmp == null) {
    		return "";
    	}
    	tmp = tmp.toUpperCase();
        StringBuilder block = new StringBuilder();
        String blkSeparators[] = {"BLKS ", "BLK ", "BLOCK ", "BLOCK: "};
		int blkIndex;
		
		tmp = tmp.replaceAll("\\bBLK II\\b", "BLK 2"); // Clay RO Instr# R63089
		tmp = tmp.replaceAll(" - ", "-");
		tmp = tmp.replaceAll("\\s{2,}", " ");
		tmp = tmp.replaceAll("\\s*&\\s(?=\\d+|[A-Z]\\b)", "&");
		for (int i=0;i<blkSeparators.length;i++) {
			int startIdx = 0;
		    while (tmp.indexOf(blkSeparators[i], startIdx)!=-1) {
		        blkIndex = blkSeparators[i].length() + tmp.indexOf(blkSeparators[i], startIdx);
		        
		        while (tmp.length() < blkIndex && tmp.charAt(blkIndex)==' ') { 
		        	blkIndex++;
		        }
		        int end = tmp.indexOf(" ",blkIndex);
		        if (end ==-1) end = tmp.length();
		        block.append(' ').append(tmp.substring(blkIndex, end));
		        startIdx = end;
		    }
		}
		
		String strBlock = block.toString();		
		strBlock = strBlock.replaceAll("&", " ").trim();
		strBlock = StringFormats.RemoveDuplicateValues(strBlock);
		return strBlock;
    }
    
    public static String extractBuildingFromText(String tmp) {
        StringBuilder result = new StringBuilder();
        String separators[] = {"BLDG "};
		int blkIndex;
		
		tmp = tmp.replaceAll(" - ", "-");
		tmp = tmp.replaceAll("\\s*&\\s(?=\\d+|[A-Z]\\b)", "&");
		for (int i=0;i<separators.length;i++) {
			int startIdx = 0;
		    while (tmp.indexOf(separators[i], startIdx)!=-1) {
		        blkIndex = separators[i].length() + tmp.indexOf(separators[i], startIdx);
		        
		        while (tmp.length() < blkIndex && tmp.charAt(blkIndex)==' ') { 
		        	blkIndex++;
		        }
		        int end = tmp.indexOf(" ",blkIndex);
		        if (end ==-1) end = tmp.length();
		        result.append(' ').append(tmp.substring(blkIndex, end));
		        startIdx = end;
		    }
		}
		
		return result.toString().replaceAll("&", " ").trim();
    }
    
    public static String extractUnitFromText(String tmp) {
    	if(tmp == null) {
    		return "";
    	}
    	tmp = tmp.toUpperCase();
    	tmp = tmp.replaceAll("\\s{2,}", " ");
        StringBuilder result = new StringBuilder();
        String separators[] = {"UNIT ", "UNIT: "};
		int index;
		
		tmp = tmp.replaceAll(" - ", "-");
		tmp = tmp.replaceAll("\\s*&\\s(?=\\d+|[A-Z]\\b)", "&");
		for (int i=0;i<separators.length;i++) {
			int startIdx = 0;
		    while (tmp.indexOf(separators[i], startIdx)!=-1) {
		        index = separators[i].length() + tmp.indexOf(separators[i], startIdx);
		        
		        while (tmp.length() < index && tmp.charAt(index)==' ') { 
		        	index++;
		        }
		        int end = tmp.indexOf(" ",index);
		        if (end ==-1) end = tmp.length();
		        result.append(' ').append(StringUtils.remove(tmp.substring(index, end),","));
		        startIdx = end;
		    }
		}
		
		return result.toString().replaceAll("&", " ").trim();
    }
    
    public static String extractPhaseFromText(String tmp) {
        StringBuilder result = new StringBuilder();
        String separators[] = {"PHASE ", "PHS ", "PH "};
		int index;
		
		tmp = tmp.replaceAll(" - ", "-");
		tmp = tmp.replaceAll("\\s*&\\s(?=\\d+|[A-Z]\\b)", "&");
		for (int i=0;i<separators.length;i++) {
			int startIdx = 0;
		    while (tmp.indexOf(separators[i], startIdx)!=-1) {
		        index = separators[i].length() + tmp.indexOf(separators[i], startIdx);
		        
		        while (tmp.length() < index && tmp.charAt(index)==' ') { 
		        	index++;
		        }
		        int end = tmp.indexOf(" ",index);
		        if (end ==-1) end = tmp.length();
		        result.append(' ').append(tmp.substring(index, end));
		        startIdx = end;
		    }
		}
		
		return result.toString().replaceAll("&", " ").trim();
    }
    
    public static String extractSectionFromText(String tmp) {
        StringBuilder result = new StringBuilder();
        String separators[] = {"SECTION ", "SEC ", "SEC."};
		int index;
		
		tmp = tmp.replaceAll(" - ", "-");
		tmp = tmp.replaceAll("\\s*&\\s(?=\\d+|[A-Z]\\b)", "&");
		for (int i=0;i<separators.length;i++) {
			int startIdx = 0;
		    while (tmp.indexOf(separators[i], startIdx)!=-1) {
		        index = separators[i].length() + tmp.indexOf(separators[i], startIdx);
		        
		        while (tmp.length() < index && tmp.charAt(index)==' ') { 
		        	index++;
		        }
		        int end = tmp.indexOf(" ",index);
		        if (end ==-1) end = tmp.length();
		        result.append(' ').append(tmp.substring(index, end));
		        startIdx = end;
		    }
		}
		
		return result.toString().replaceAll("&", " ").trim();
    }
    
    public static String extractBlockFromText(String tmp, Boolean onlyNumbers, Boolean removeDuplicates) {
    	
    	String block = extractBlockFromText(tmp);
    	
    	if(block.length() > 0) {
    	
			// remove literal blocks, when blocks are supposed to have only numerical values    	
	    	if(onlyNumbers){
	    		block = block.replaceAll("[A-Z]+", "");
	    		block = block.replaceAll("\\s{2,}", " ");
	    	}
	    	if (removeDuplicates){
	    		// if block string contains more values than one numbers interval, the interval(s) must be expanded; 
	    		// this is needed also when the lot string contains more intervals which may intersect  
	    		if(!block.matches("\\d+\\s+-\\s+\\d+")) {
	    			int iterations = 0;
	    			String lastBlock = "";
		    		while(block.indexOf("-") != -1 && iterations < 20 && !lastBlock.equals(block)) {
		    			iterations++;
		    			lastBlock = block;
		    			block = StringFormats.ReplaceIntervalWithEnumeration(block);
		    		}
		    		block = StringFormats.RemoveDuplicateValues(block);
		    		block = StringFormats.ReplaceEnumerationWithInterval(block);
	    		}
	    	}
    	}
    	return block;
    }    
    
    public static void main( String[] args) {
        String[] legals = new String[] {
        		/*"UNIT 302",
        		"475 STRAIGHT CREEK DR. UNIT 302A, DILLON",
        		"61/104 TRACTS 25 AND 26",
        		"REL DT 69/144 SEE DOC PT TRACT 3", 
        		"MEMO; TR1:1527AC%SEVERAL ABSTRACTS TR2:0.04089AC%A227 TR3:0.7162AC%A227",
        		"! % 214.84AC TR 1 %432AC DR",
        		"! % 214.84AC TRACTS 25 AND 26 TR 1 %432AC DR",
        		"BEAVER PARK PLAT 3 RPLT TR 44",
        		"CRYSTAL VLG FLG 6,PH 2 AMEND Block: 1 Lot: 10",
        		"CRYSTAL VLG FLG 6,PH 2 AMEND Block 1 Lot 10",
        		"Section: 36 Township: 5 Range: 91 Subdivision: MOUNTAIN SHADOWS SUBDIVISION Block: 5 Lot: 41",
        		*/"Section: 21 Township: 6 Range: 89 Subdivision: VALLEY VIEW FARMS SUB Block: 3 Lot: 15 LOT BOUNDARY ADJ PLAT OF LOTS 13,14 & 15, BLK 3",
        		"Section: 3 Township: 7 Range: 89 Subdivision: SUNLIGHT VIEW SUB. Block: 6 Lot: 3 2ND AMENDED",
        		"Section: 33 Township: 7 Range: 88 Subdivision: LINES PLAZA II CONDOMINIUMS Unit: 105",
        		"MBL HOME TITLE: 24E447933 SERIAL: 12325089 YEAR: 1997 MAKE: REDMAN SIZE: 16 X 76 SPACE: 0 " +
        			"Subdivision: SADDLEBACK/TAMARISK PARK Block: 21 Lot: 26 Unit: 26 2407-192-22-021 " +
        			"SADDLEBACK VILLAGE FLG. #1 BLK 26 LOT 21 2407-192-22-021"
        };
        legals = new String[0];
        for (String legal : legals) {
        	legal = legal.replaceAll("(.*)PROPERTY ADDRESS:.*", "$1");
        	System.out.println("LEGAL: " + legal);
        	System.out.print("TRACT: " + extractTractFromText(legal) + "___");
        	System.out.print("SECTION: " + extractSectionFromText(legal) + "___");
        	System.out.print("LOT: " + extractLotFromText(legal) + "___");
            System.out.print("BLOCK: " + extractBlockFromText(legal) + "___");
            System.out.print("BUILDING: " + extractBuildingFromText(legal) + "___");
            System.out.print("UNIT: " + extractUnitFromText(legal) + "___");
            System.out.println("PHASE: " + extractPhaseFromText(legal));
		}
        
        
        
        String[] legalsToClean = new String[] {
        		"21 S 11,  7,8 Q 5 Q 6  26, S 40, 1142-252228- 24  26  41,42 E 5-14 12-27, S5 9, 0013-122228- 4  64, 0344-242329- 39  3  1,N 5, 0032-152328- 9, J 5, E 3,8  110,, 115, 0013-352228 5, A 3, 2-99 4,N68 5,S37 4, 5, 2-99 4, 2-99 5, 2-99 4, 0022-282431- 17, N 17, 2211-82230 3, H 2,3, U 7, 6, Q 12, S 24, 7 1-129, 3-70- 1, S 9, S 9, S 9, D A 2, J 11, R 3, V"
        };
        
        for (String string : legalsToClean) {
			long time = System.currentTimeMillis();
			System.out.println("Cleaning " + string);
			String res = cleanValues(string, false, true);
			System.out.println("It took " + ((System.currentTimeMillis() - time)/1000)  + " second to get " + res);
		}
        
        
        
         
    }
}

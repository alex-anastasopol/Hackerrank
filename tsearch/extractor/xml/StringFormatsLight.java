package ro.cst.tsearch.extractor.xml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.NameUtils;

public class StringFormatsLight {
	
	public static String SPLIT_NAME_H_W_LFM = "&|/ ?";
	
	public static String[] lastNames={"Smith", "VAN_HOOK"};
	
	public static String[] parseNameNashville(String s) {
    	return parseNameNashville(s, false);
    }
    public static String[] parseNameNashville(String s, boolean newParsing) { // L,FM
        String[] ret={"", "", "", "", "", ""}; // of, om, ol, wf, wm, wl
        if (s==null)
            return ret;

        s = s.replaceAll("(JENKINS ROBERT S) (SYLVIA L)", "$1 & $2"); //B 3492
        s=s.replaceFirst("([A-Z]+)\\s*-\\s*([A-Z]+)","$1-$2");  // pt FLSantaRosaTR, PIN: 242S280000007220000
        s=s.replaceAll("(?s)\\((.*?)\\)","");
        s = s.replaceFirst("\\s*\\bA/?K/?A/?\\s*$", "");
        s = s.replaceFirst("(?<!\\b[A-Z]\\s?)&\\s*H(USBAND)?\\s*$", "").trim();
        s = s.replaceFirst("\\s*&\\s*(WF|WIFE)\\s*$", "");
        //s = s.replaceFirst("\\s*\\b(LIFE )?ESTATE\\s*$", "");	// fix for bug #3151
        s = s.replaceFirst("(COPPEDGE ANNE) ESTATE", "$1"); // re fix for bug 3151 because now on florida LIFE ESTATE must appear in the name
        //s = s.replaceAll("(.+LOAN TRUST).*", "$1");//B 3097
        s = s.replaceAll("(LONG BCH MTG LOAN TRUST).*", "LONG BEACH MTG LOAN TRUST");
        s = s.replaceAll(">HW(JT)?\\b", ""); //CALos Angeles APN 4108-019-021
        s = s.replaceAll(">SM\\b", "");
	    s = s.replaceAll(">MMSP\\b", "");
	    s = s.replaceAll("\\bHUSBAND AND WIFE\\b", "");
	    s = s.replaceAll("\\bAS JOINT TENANTS\\b", "");
	    s = s.replaceAll("\\bMULTIPLE GRANTEES\\b", "");
	    s = s.replaceAll("\\b(MARRIED|SINGLE) MAN\\b", "");
	    s = s.replaceAll("\\bAS SEPARATE PROPERTY\\b", "");
	    s = s.replaceAll("(BARRY) (ANDERSON)", "$2 $1");
	    s = s.replaceAll("\\+", "&");// fix for B 4156
	    s = s.replaceAll(",\\s+SUB\\s+TR\\b", "");
	    s = s.replaceAll("\\bADMINISTRATOR\\b", "");		// bug #5214
	    s = s.replaceAll("\\b(ET)\\s+(UX|AL|VIR)\\b", "$1$2");
	    
	    s = s.replaceAll("\\s{2,}", " ");
	    s = s.trim();

        if (s.indexOf(',')==-1)
            return parseNameWilliamson(s, newParsing);
        
        boolean hasSpouse = s.matches(".+ ETUX \\w+ \\w+") || // bug #2744, GREEN DANIEL R ETUX MARY ANN
							s.matches(".+ ETVIR \\w+ \\w+") ;  // bug #5137, GREEN DANIEL R ETVIR MARY ANN
        
        s=s.replaceAll("\\s+", " ");
        Matcher ma=Pattern.compile("^[\\w'-]+( \\w+)?,").matcher(s);
        if (!ma.find()) {
        	ma=Pattern.compile("(?i)^(MAC|VAN(?:\\s+DE[N|R]?)?|O|D|ST|MC|DE(?: LA)?|DI|EL|DEL) (\\w|-)+,").matcher(s); // MC SMITH, JULIOUS; VAN DUSSELDORP, COURTNEY; D ALESSANDRO
        	if (!ma.find()) {
	            ret[2]=s;
	            return ret;
        	}
        }
        s=unifyNameDelim(s, newParsing);
        ma=Pattern.compile("&|/ ?").matcher(s);
        String husband, wife="";
        if (ma.find()) {
            husband=s.substring(0, ma.start()).trim();
            wife=s.substring(ma.end()).trim();
        } else {
            husband=s.trim();
        }

        if (!husband.matches("(\\w|-)+, \\w+( \\w+)?") && NameUtils.isCompany(husband)) { //fix for bug #2292
            ret[2]=husband;
        } else {
            int i=husband.indexOf(",");
            if (i != -1){
	            ret[2]=husband.substring(0, i).trim();
	            husband=husband.substring(i+1).replaceAll("\\.", " ").replaceAll(",", " ").trim();
	            int j=husband.indexOf(" ");
	            if (j!=-1) {
	                ret[0]=husband.substring(0, j).trim();
	                ret[1]=husband.substring(j+1).trim();
	            } else {
	                ret[0]=husband;
	            }
            } else {
            	ret[0]=husband;
            }
        }

        if (!wife.matches("\\w+( \\w)?") && NameUtils.isCompany(wife)) {
            ret[5]=wife;
        } else if (wife.length() != 0){
        	
        	String patt = ret[2].replaceAll("([\\+\\{\\}])", "\\\\$1");	 //fix for a parser bug reported by ATS on 11/14/2006
        	boolean pattIsMod = !patt.equals(ret[2]);
        	String wl = ret[2];
        	Pattern pt = Pattern.compile("\\b("+patt+"-\\w+)\\b"); //fix for bug #964
            Matcher m = pt.matcher(wife);	            
            if(m.matches())
            {
                wl = m.group(1); 
                patt = wl.replaceAll("([\\+\\{\\}])", "\\\\$1");
            } else {
		        if (patt !=null && !"".equals(patt))
		        {
			        if (!pattIsMod && patt.contains("\\") && !patt.contains("\\\\"))
			        {
			        	patt = patt.replaceAll("\\\\", "\\\\\\\\");
			        }
		        }

            	Pattern pt1 = Pattern.compile("\\b(\\w+-"+patt+")\\b");
            	Pattern pt2 = Pattern.compile("\\b("+patt+"-\\w+)\\b");
            	m = pt1.matcher(wife);
            	if (m.find()){
            		wl = m.group(1);
            		patt = wl.replaceAll("([\\+\\{\\}])", "\\\\$1");
            	}
            	else {
            		m = pt2.matcher(wife);
            		if (m.find()){
            			wl = m.group(1);
            			patt = wl.replaceAll("([\\+\\{\\}])", "\\\\$1");
            		}
            	}	
            }
            
            
    		String tempWife=wife.replaceAll("\\b"+patt+"\\b,?", "").trim(); //remove wife LN, if identical to husband LN
    		boolean wifeDifferentLN = false;;
    		
    		// SMITH CHERYL & ELMER H JR (JR must not be considered as a different Last Name for wife)
    		boolean hasSuffix  = tempWife.matches(".+ (JR|SR|II|III|IV)\\s*")?true:false;
    		// SMITH CHARLES B TR & BETTY D TR (TR(USTEE)? must not be considered as a different Last Name for wife)
    		boolean hasType  = tempWife.matches(".+ (T(?:(?:RU?)?S)?(?:TE)?E?S?)\\s*") ? true : false;
    		// SMITH DAILY A & ARVAZINE ETAL (ETAL|ETUX|ETVIR must not be considered as a different Last Name for wife)
    		boolean hasOtherType  = tempWife.matches(".+ (ET[\\s,;]*UX|ET[\\s,;]*AL|ET[\\s,;]*VIR)\\s*") ? true : false;
    			
    		// added for cases like that : BENNETT JEREMY T & MARY ANN , when the wife name was perceived as having a 
    		// last name 
    		boolean wifeNameIsFirstName = false;
    		if (tempWife.trim().contains(" ")){
    			String[] split = tempWife.split("\\s+");
    			wifeNameIsFirstName = true; 
    			for (String string : split) {
					boolean firstName = FirstNameUtils.isFirstName(string);
					wifeNameIsFirstName =wifeNameIsFirstName && firstName; 
				}
    		}
    		if(tempWife.equals(wife) && !hasSpouse && !hasSuffix && !hasType && !hasOtherType && !wifeNameIsFirstName){
    			//nothing changed it means that the wife has another last name so we must treat this case separately
    			wifeDifferentLN = true;
    		}
    		if (tempWife.matches("(\\w+\\s){3,}\\w+\\b") && !(hasSuffix && hasType)){ //LACLAIR CATHY A & MONTAGUE IRA D JR
    			wifeDifferentLN = true;
    		}
    		
    		wife = tempWife;
    		
    		pt = Pattern.compile("((?:(?:VAN(?:\\s+DE[N|R]?)?|O|D|ST|MC|DE(?: LA)?|DI|EL|DEL)\\s)?[\\w'-]{2,})\\s?,\\s?\\w+ \\w"); //wife name can be as LN FN MI, where wife LN != husband LN (fix for bug #1186)
    		m = pt.matcher(wife);
    		if (m.matches()){
    			wl = m.group(1);
    			wife = wife.replace(wl, "").trim();
    		} else if(wifeDifferentLN){
    			pt = Pattern.compile("\\A((?:(?:VAN(?:\\s+DE[N|R]?)?|O|D|ST|MC|DE(?: LA)?|DI|EL|DEL)\\s)?[\\w'-]{2,}),\\s?\\w{2,}"); // JOHNSON ABDUL & EL ZEFRI FATIMA (MOJacksonTR)
    			m = pt.matcher(wife);
    			if (m.find()){
        			wl = m.group(1);
        			wife = wife.replace(wl, "").trim();
    			} else {
    				pt = Pattern.compile("\\w{2,} \\w{1,2} ((?:(?:VAN(?:\\s+DE[N|R]?)?|O|D|ST|MC|DE(?: LA)?|DI|EL|DEL)\\s)?[\\w'-]{2,})");
        			m = pt.matcher(wife);
        			if (m.find()){
            			wl = m.group(1);
            			wife = wife.replace(wl, "").trim();
        			}
    			}
    		}

            if (wife.length()>0)
                ret[5]=wl;

            wife=wife.replaceAll("\\.", " ").replaceAll(",", " ").replaceAll(" &", "").trim();

            int j=wife.indexOf(" ");
            if (j!=-1) {
                ret[3]=wife.substring(0, j).trim();
                ret[4]=wife.substring(j+1).trim();
            } else {
                ret[3]=wife;
            }
            if (ret[3].length() == 1 && ret[4].length() > 1){ // interchange spouse middle with spouse first if first is just one letter (part of the bug #3042 fix)
            	String temp = ret[3];
            	ret[3] = ret[4];
            	ret[4] = temp;
            }
        }
        ret[1] = ret[1].replaceAll("\\s{2,}", " ");        
        ret[4] = ret[4].replaceAll("\\s{2,}", " ");
        return ret;
       
    }
    
    public static String[] parseNameWilliamson(String s) {
    	return parseNameWilliamson(s, false);
    }
    public static String[] parseNameWilliamson(String s, boolean newParsing) { // LFM
        String[] ret={"", "", "", "", "", ""}; // of, om, ol, wf, wm, wl
        if (s==null)
            return ret;
        s = s.replaceAll("\\[","");
        s = s.replaceAll("\\]","");
        s = s.replaceAll("[*.?]", "");
        s = s.replace("FORCL & BANKRUPTCY", ""); // bug #577
        s = s.replace("C/O FID/NATL", "");		// bug #577
        if (s.contains("WILLIAMSON& VICKIE"))   // bug 2899
        	s = s.replace("&", "");
        if (s.length() == 0) {
        	return ret;
        }        
        s = s.replaceAll("\\bD/?B/?A\\b", "&"); // wife is a company - bug #566, 2746 (CLINARD PAUL L ETUX DBA)
		if (s
			.trim()
			.matches(".*\\w+\\s*(\\&|AND)\\s*\\w\\s*\\w+.*") 
				&& NameUtils
					.isCompany(s.trim())) 
			ret[2]=s;
		else if ((s.split(" ")).length == 3 && (s.split(" ")[1].equals("&") || s.split(" ")[1].equals("AND"))) {
		    // daca este companie Ex: "SMITH & SONS" sau "GONZALEZ AND AGUILAR" (B 4656)
		    ret[2]=s;
		}		
		else {			
	        if (s.contains("/") && s.matches("\\w+,(?:\\s*\\w+)*\\s*/.*")) 
	            return parseNameWilliamson2(s, newParsing);
	        s=s.replaceAll("\\s+", " ");
	        boolean hasSpouse = s.matches(".+ ETUX \\w+ \\w+") || // bug #2744, GREEN DANIEL R ETUX MARY ANN
	        					s.matches(".+ ETVIR \\w+ \\w+") ;  // bug #5137, GREEN DANIEL R ETVIR MARY ANN
	        s=unifyNameDelim(s, newParsing);
	        Matcher ma=Pattern.compile(SPLIT_NAME_H_W_LFM).matcher(s);
	        String husband, wife="";
	        if (ma.find()) {
	            husband=s.substring(0, ma.start()).trim();
	            wife=s.substring(ma.end()).trim();
	        } else {
	            husband=s.trim();
	        }
	
	        if (NameUtils.isCompany(husband)) {
	            ret[2]=husband;
	        } else {
	            for (int i=0; i<lastNames.length; i++) {
	                //husband=husband.replaceFirst("^(?i)(\\w+) (\\b"+lastNames[i]+"\\b)(.*)", "$2 $1$3");
	            }
	            husband=husband.replaceFirst("^(?i)(\\w+) ((?:\\w|[iv]{2,})\\s?(?:\\s+(?:\\w|[iv]{2,}))*) ([A-Z]{3,}\\b)(.+)", "$1 $3 $2$4");
	            int i=husband.indexOf(" "), j;	            
	            ma = Pattern.compile(StringFormats.NAME_WITH_PREPOSITION_PATTERN).matcher(husband);
	            if (ma.find()){
	            	i = husband.indexOf(" ", ma.end(1) + 1);
	            }
	            if (i==-1) {
	                ret[2]=husband;
	            } else {
	                ret[2]=husband.substring(0, i).trim();
	                husband=husband.substring(i+1).replaceAll("\\.", " ").replaceAll(",", " ").trim();
	                j=husband.indexOf(" ");
	                if (j!=-1) {
	                    ret[0]=husband.substring(0, j).trim();
	                    ret[1]=husband.substring(j+1).trim();
	                } else {
	                    ret[0]=husband;
	                }
	            }
	        }

	        if (wife.matches("\\d+")){
	        	wife = "";
	        }
	        if (NameUtils.isCompany(wife)) {
	            ret[5]=wife;
	        } else if (wife.length() != 0){
	        	
	        	String patt = ret[2].replaceAll("([\\+\\{\\}])", "\\\\$1");	 //fix for a parser bug reported by ATS on 11/14/2006
	        	boolean pattIsMod = !patt.equals(ret[2]);
	        	String wl = ret[2];
	        	Pattern pt = Pattern.compile("\\b("+patt+"-\\w+)\\b"); //fix for bug #964
	            Matcher m = pt.matcher(wife);	            
	            if(m.matches())
	            {
	                wl = m.group(1); 
	                patt = wl.replaceAll("([\\+\\{\\}])", "\\\\$1");
	            } else {
			        if (patt !=null && !"".equals(patt))
			        {
				        if (!pattIsMod && patt.contains("\\") && !patt.contains("\\\\"))
				        {
				        	patt = patt.replaceAll("\\\\", "\\\\\\\\");
				        }
			        }

	            	Pattern pt1 = Pattern.compile("\\b(\\w+-"+patt+")\\b");
	            	Pattern pt2 = Pattern.compile("\\b("+patt+"-\\w+)\\b");
	            	m = pt1.matcher(wife);
	            	if (m.find()){
	            		wl = m.group(1);
	            		patt = wl.replaceAll("([\\+\\{\\}])", "\\\\$1");
	            	}
	            	else {
	            		m = pt2.matcher(wife);
	            		if (m.find()){
	            			wl = m.group(1);
	            			patt = wl.replaceAll("([\\+\\{\\}])", "\\\\$1");
	            		}
	            	}	
	            }
	            
	            
        		String tempWife=wife.replaceAll("\\b"+patt+"\\b,?", "").trim(); //remove wife LN, if identical to husband LN
        		boolean wifeDifferentLN = false;;
        		
        		// SMITH CHERYL & ELMER H JR (JR must not be considered as a different Last Name for wife)
        		boolean hasSuffix  = tempWife.matches(".+ (JR|SR|II|III|IV)\\s*")?true:false;
        		// SMITH CHARLES B TR & BETTY D TR (TR(USTEE)? must not be considered as a different Last Name for wife)
        		boolean hasType  = tempWife.matches(".+ (T(?:(?:RU?)?S)?(?:TE)?E?S?)\\s*") ? true : false;
        		// SMITH DAILY A & ARVAZINE ETAL (ETAL|ETUX|ETVIR must not be considered as a different Last Name for wife)
        		boolean hasOtherType  = tempWife.matches(".+ (ET[\\s,;]*UX|ET[\\s,;]*AL|ET[\\s,;]*VIR)\\s*") ? true : false;
        			
        		// added for cases like that : BENNETT JEREMY T & MARY ANN , when the wife name was perceived as having a 
        		// last name 
        		boolean wifeNameIsFirstName = false;
        		if (tempWife.trim().contains(" ")){
        			String[] split = tempWife.split("\\s+");
        			wifeNameIsFirstName = true; 
        			for (String string : split) {
						boolean firstName = FirstNameUtils.isFirstName(string);
						wifeNameIsFirstName =wifeNameIsFirstName && firstName; 
					}
        		}
        		if(tempWife.equals(wife) && !hasSpouse && !hasSuffix && !hasType && !hasOtherType && !wifeNameIsFirstName){
        			//nothing changed it means that the wife has another last name so we must treat this case separately
        			wifeDifferentLN = true;
        		}
        		if (tempWife.matches("(\\w+\\s){3,}\\w+\\b") && !(hasSuffix && hasType)){ //LACLAIR CATHY A & MONTAGUE IRA D JR
        			wifeDifferentLN = true;
        		}
        		
        		wife = tempWife;
        		
        		pt = Pattern.compile("((?:(?:VAN(?:\\s+DE[N|R]?)?|O|D|ST|MC|DE(?: LA)?|DI|EL|DEL)\\s)?[\\w'-]{2,}) \\w{2,} \\w"); //wife name can be as LN FN MI, where wife LN != husband LN (fix for bug #1186)
        		m = pt.matcher(wife);
        		if (m.matches()){
        			wl = m.group(1);
        			wife = wife.replace(wl, "").trim();
        		} else if(wifeDifferentLN){
        			pt = Pattern.compile("\\A((?:(?:VAN(?:\\s+DE[N|R]?)?|O|D|ST|MC|DE(?: LA)?|DI|EL|DEL)\\s)?[\\w'-]{2,}) \\w{3,}"); // JOHNSON ABDUL & EL ZEFRI FATIMA (MOJacksonTR)
        			m = pt.matcher(wife);
        			if (m.find()){
            			wl = m.group(1);
            			wife = wife.replace(wl, "").trim();
        			} else {
        				pt = Pattern.compile("\\w{2,} \\w{1,2} ((?:(?:VAN(?:\\s+DE[N|R]?)?|O|D|ST|MC|DE(?: LA)?|DI|EL|DEL)\\s)?[\\w'-]{2,})");
            			m = pt.matcher(wife);
            			if (m.find()){
                			wl = m.group(1);
                			wife = wife.replace(wl, "").trim();
            			}
        			}
        		}

	            if (wife.length()>0)
	                ret[5]=wl;
	
	            wife=wife.replaceAll("\\.", " ").replaceAll(",", " ").replaceAll(" &", "").trim();
	
	            int j=wife.indexOf(" ");
	            if (j!=-1) {
	                ret[3]=wife.substring(0, j).trim();
	                ret[4]=wife.substring(j+1).trim();
	            } else {
	                ret[3]=wife;
	            }
	            if (ret[3].length() == 1 && ret[4].length() > 1){ // interchange spouse middle with spouse first if first is just one letter (part of the bug #3042 fix)
	            	if (!("W".equalsIgnoreCase(ret[3]) && "KENT".equalsIgnoreCase(ret[4]))) { //B 6871
	            		String temp = ret[3];
	            		ret[3] = ret[4];
	            		ret[4] = temp;
	            	}	
	            }
	        }
		}
		
        return ret;
    }
    
    public static String unifyNameDelim(String s, Boolean newParsing) {
    	s = unifyNameDelimWithoutRemove(s, newParsing);
        s=s.replaceAll("& *$", "");
        return s.trim();
    }
    
    public static String[] parseNameWilliamson2(String s, boolean newParsing) { // LFM
        String[] ret={"", "", "", "", "", ""}; // of, om, ol, wf, wm, wl
        if (s==null)
            return ret;
        s=s.replaceAll("\\s+", " ");
        s=unifyNameDelim(s, newParsing);
        String[] st=s.split("/");
        String husband, wife="";
        if (st.length>1) {
            husband=st[0].trim();
            for (int i=1; i<st.length-1; i++) 
                wife+=st[i].trim()+" & ";
            wife+=st[st.length-1].trim();
        } else {
            husband=st[0].trim();
        }
        String[] st2=parseNameNashville(husband, newParsing);
        System.arraycopy(st2, 0, ret, 0, 3);
        if (wife.length()>0) {
            wife=wife.replaceAll("\\&", "@");
            st2=parseNameNashville(wife, newParsing);
            st2[1]=st2[1].replaceAll("@", "\\&");
            System.arraycopy(st2, 0, ret, 3, 3);
        }

        return ret;
    }
    
    public static String unifyNameDelimWithoutRemove(String s, boolean newParsing) {
        s=special_needs(s);
        s=s.replaceAll("\\(([^\\&]*)$", "&$1");
        s=s.replaceAll("\\(", "&");
        s=s.replaceAll("\\)", "");
        if (newParsing){
	        s=s.replaceAll("(?i) ?\\b(ET ?UXX?)\\b", " $1 &").replaceAll("(?i) & (TRU?S?(?:TEE?)?)", " $1");
	        s=s.replaceAll("(?i) ?\\b(ETU)\\b", " $1 &").replaceAll("(?i) & (TRU?S?(?:TEE?)?)", " $1");
	        s=s.replaceAll("(?i) ?\\b(ET ?A(LS?)?)\\b", " $1 &").replaceAll("(?i) & (TRU?S?(?:TEE?)?)", " $1");
	        s=s.replaceAll("(?i) ?\\b(ET ?VIR)\\b", " $1 &").replaceAll("(?i) & (TRU?S?(?:TEE?)?)", " $1");
        } else {
        	s=s.replaceAll("(?i) ?\\bET ?UXX?\\b", " &");
	        s=s.replaceAll("(?i) ?\\bETU\\b", " &");
	        s=s.replaceAll("(?i) ?\\bET ?A(LS?)?\\b", " &");
	        s=s.replaceAll("(?i) ?\\bET ?VIR\\b", " &");
        }
        s=s.replaceAll("(?i) ?\\bET\\b", " &");
        s=s.replaceAll("(?i) ?\\bAND\\b", " &");
        s=s.replaceAll("&( &)+", "&");
        s=s.replaceAll("%", "&");
        return s.trim();
    }
    
  //to be used while the specs for name matcher are developed
    private static String special_needs(String s) {
        s=s.replaceFirst("\\bHOWARD D ETUX\\b", "DAVID H ETUX");
        return s;
    }

}

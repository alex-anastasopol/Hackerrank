package ro.cst.tsearch.extractor.xml;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.utils.CountyCities;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class StringFormats {

	protected static final Category logger= Logger.getLogger(StringFormats.class);
	
	/**
	 * The string used for the pattern that splits name in Last First Middle name parsing
	 */
	public static String SPLIT_NAME_H_W_LFM = "&|/ ?";
	
	public final static String NAME_WITH_PREPOSITION_PATTERN = "^(VAN(?:\\s+DE[N|R]?)?|O|D|ST|MC|DE(?: LA)?|DI|EL|DELA?|AL|LA|LO)\\s\\S+\\s\\S+.*$";//D ALESSANDRO, Al Jafari
	
    protected static String[] middleParticles={"JR", "SR", "DR", "MD"};
    protected static String[] middleOtherParticles={"TR", "TRUSTEE", "ETAL", "ETUX", "ETVIR"};
    protected static boolean isInMiddleName(String s) {
        s=s.toUpperCase().trim();
        if (Roman.isRoman(s))
            return true;
        for (int i=0; i<middleParticles.length; i++)
            if (s.equals(middleParticles[i]))
                return true;
        
        return false;
    }
    protected static boolean isTypeAndMustBeInMiddleName(String s) {
        s=s.toUpperCase().trim();
        if (Roman.isRoman(s))
            return true;
        for (int i=0; i<middleOtherParticles.length; i++)
            if (s.equals(middleOtherParticles[i]))
                return true;
        
        return false;
    }

    protected static Set<String> nashvilleRODocumentType;
    static {
        nashvilleRODocumentType=new TreeSet<String>(new Comparator<String>() {
                                                public int compare(String o1, String o2) {
                                                    return -(((String)o1).compareTo((String)o2));
                                                }
                                            });
        Set set = null;
        try {
            set = ro.cst.tsearch.titledocument.abstracts.DocumentTypes.getAllTypes(County.getCounty("Davidson", "TN").getCountyId().intValue());
        } catch (Exception e) {
            set = ro.cst.tsearch.titledocument.abstracts.DocumentTypes.getAllTypes(0);
        }
        
        Iterator it=set.iterator();
        while (it.hasNext()) {
            String s=(String)it.next();
            int i=s.indexOf('+');
            if (i!=-1)
                nashvilleRODocumentType.add(s.substring(i+1));
        }
    }

    public static String JohnsonROPage(String s) {

        if (s == null || s.length() == 6)
            return s;
        else
            return s.replaceAll("^[0]*(\\d{1,})$", "$1");
    }
    
    public static String NashvilleRODocumentType(String s) {
        
        int idxa = s.indexOf('@');
        String dt = null;
        if(idxa == -1) {
        	dt = s.trim();
        } else {
        	dt = s.substring(0, idxa).trim();
        }
        if(StringUtils.isEmpty(dt)) {
        	return "";
        }
        s = s.substring(idxa + 1);
        s = s.replaceAll(".*:\\s*(.*)", "$1"); //B 4049
        Iterator<String> it = nashvilleRODocumentType.iterator();
        String result = dt;
        String temp = "";
        while (it.hasNext()) {

            String s1 = (String) it.next();

            // checks if "s" contains a Nashville RO document type (the substring that matches a document type must be delimited with word boundaries)   
            int  idxdt = s.indexOf(s1);
            if (idxdt != -1) {
            	if ((idxdt == 0 || String.valueOf(s.charAt(idxdt-1)).matches("\\W"))  
            			&& (idxdt+s1.length() == s.length() || String.valueOf(s.charAt(idxdt+s1.length())).matches("\\W"))) {
            		temp = dt.length() > 0 ? dt + "+" + s1 : s1;
            		if(result.length()<temp.length()) {
            			result = temp;
            		}
            	}
            }
        }
        return result;
    }

    public static String instrnonewdavidsonAO(String s)
    {
        Pattern pt=Pattern.compile("([1-9]\\d*)-(\\d*)");
        Matcher m=pt.matcher(s);
        
        String rez="";
        if(m.matches())
        {
            rez=m.group(1)+m.group(2); 
        }
        return rez;
    }
    
    public static String BooknewdavidsonAO(String s)
    {
        Pattern pt=Pattern.compile("(0\\d*)-(\\d*)");
        Matcher m=pt.matcher(s);
        
        String rez="";
        if(m.matches())
        {
            rez=m.group(1).replaceAll("^(0*)(\\d*)","$2"); 
        }
        return rez;
    }
    
    public static String PagenewdavidsonAO(String s)
    {
        Pattern pt=Pattern.compile("(0\\d*)-(\\d*)");
        Matcher m=pt.matcher(s);
        
        String rez="";
        if(m.matches())
        {
            rez = m.group(2).replaceAll("^(0*)(\\d*)","$2"); 
        }
        return rez;
    }
    public static String yearbuiltNewDavidsonAO(String s)
    {
        String rez=(new SimpleDateFormat("yyyy").format(new Date())).toString();
        Pattern pt=Pattern.compile("(?s).*built about\\s*(\\d+).*");
        Matcher m=pt.matcher(s);
        
        if(m.matches())
        {
            rez=m.group(1); 
        }
        
        return rez;
    }
    protected static Pattern paddr1=Pattern.compile("(?i)^(?:NO )?(\\d+)\\s*(-\\d+)? (.*)$");
    protected static Pattern paddr2=Pattern.compile("(?i)^(.*?)(?:/([NWSE]))? (?:NO )?(\\d+(?:-\\d+)?)( #.*| [-\\w]+)?$"); //fix for bugs #698, #940, #1233
    protected static Pattern paddr3=Pattern.compile("(?i)^(HWY\\s+\\d+(?:\\s+[NWSE])?)(?:\\s+(\\d+))?$"); //fix for HWY 431 3930
    protected static Pattern paddrPO = Pattern.compile("(?is)(p\\s*o\\s*box\\s*)(\\d+)");
    protected static Pattern paddr4 = Pattern.compile("(?is)([\\d]+)([A-Z\\s\\d]+)");
    protected static Pattern paddr5 = Pattern.compile("(?is)^(\\d+) ([A-Z].+) -(\\d+)$");
    protected static Pattern paddr6 = Pattern.compile("(?i)(\\d+[A-Z]?\\d+)\\s* (.*)$"); //B4450
    protected static Pattern paddrCRFM = Pattern.compile("(?is)(\\d+ )?((?:[NSWE]{0,2} )?(?:FM|CR) \\d+(?:\\s*[A-Z])?)"); //For Texas    13725 E FM 2790,
    		// FM 1099,   3165  FM 1099,    3030  CR 415,    S CR 345,     1460  CR 422 A
    		// FLSumterTR: 4418 CR 124A
    
    public static String[] parseAddressShelbyAO(String s) {
        s=s.replaceAll("\\s+", " ").toUpperCase().trim();
        String[] ret=new String[]{"", ""};
        if (s.toUpperCase().startsWith("PO BOX")) {
            ret[0]="";
            ret[1]=s;
        } else {
            Matcher ma=paddr1.matcher(s);
            if (ma.find()) {
                ret[0]=ma.group(1);
                ret[1]=ma.group(3);
            } else {
                ret[1]=s;
            }
        }
        return ret;
    }
    protected static String[] parseAddressNassauTR(String s) {
    s = s.replaceAll("(?is)(p)?\\s*(o\\s*box\\s*)(\\d+)", "$1"+"$2"+"$3");
   	 s=s.replaceAll("\\s+", " ").toUpperCase().trim();
        String[] ret=new String[]{"", ""};
       
        Matcher mp = paddrPO.matcher(s);
        if (mp.find()) {
            ret[0]=mp.group(2);
            ret[1]=mp.group(1);
        } else {
            Matcher ma=paddr1.matcher(s);
            if (ma.find()) {
                ret[0]=ma.group(1);
                ret[1]=ma.group(3);
            } else {
                ret[1]=s;
            }
        }
        return ret;
   }
  
    protected static String[] parseAddressPascoTR(String s) {
        s = s.replaceAll("(?is)(p)?\\s*(o\\s*box\\s*)(\\d+)", "$1"+"$2"+"$3");
       	 s=s.replaceAll("\\s+", " ").toUpperCase().trim();
            String[] ret=new String[]{"", ""};
           
            Matcher mp = paddrPO.matcher(s);
            if (mp.find()) {
                ret[0]=mp.group(2);
                ret[1]=mp.group(1);
            } else {
                Matcher ma=paddr4.matcher(s);
                if (ma.find()) {
                    ret[0]=ma.group(1);
                    ret[1]=ma.group(2);
                } else {
                    ret[1]=s;
                }
            }
            return ret;
       }
    
    /**
     * Returns the given address split into city string for position 0 and the address without the city info.
     * @param addr
     * @param cities @see {@link CountyCities}
     * @return
     */
   	public static String[] parseCityFromAddress( String addr, String[] cities) {
   		String[] ret=new String[]{"", ""};
   		boolean containsCity = false;
		if (cities != null) {
			for (int i = 0; i < cities.length && !containsCity; i++) {
				if (addr.toUpperCase().contains(cities[i].toUpperCase())) {
					ret[0] = cities[i];
					ret[1] = addr.toUpperCase().trim().replaceFirst(cities[i].toUpperCase() + "$", "");
					containsCity = true;
				}
			}
		}
		if (!containsCity){
			ret[1] = addr;
		}
		return ret;
	}
   	
   	/**
     * Returns the given address split into city string for position 0 and the address without the city info.
     * The city must be at the end of the address
     * @param addr
     * @param cities @see {@link CountyCities}
     * @return
     */
   	public static String[] parseCityFromAddress2( String addr, String[] cities) {
   		addr = addr.trim();
   		String[] ret=new String[]{"", ""};
   		boolean containsCity = false;
		if (cities != null) {
			for (int i = 0; i < cities.length && !containsCity; i++) {
				if (addr.toUpperCase().endsWith(cities[i].toUpperCase())) {
					ret[0] = cities[i];
					ret[1] = addr.toUpperCase().trim().replaceFirst(cities[i].toUpperCase() + "$", "");
					containsCity = true;
				}
			}
		}
		if (!containsCity){
			ret[1] = addr;
		}
		return ret;
	}
    
    /**
     * Splits a string that should contain an address (without City or Zip)
     * @param s unparsed address
     * @return a String[] with two elements. First element is the StreetNo and the second one is the rest of the Address
     */
    public static String[] parseAddress(String s) {
        s=s.replaceAll("\\s+", " ").toUpperCase().trim();
        
        // remove leading zeros from numbers contained in the address string 
		s = s.replaceAll("\\b0+(\\d+)\\b", "$1");

        
        String unit = s.replaceFirst(".*\\b(UNIT \\d+(?:\\s*-\\s*\\d+)?).*", "$1");  // fix for bug #1178 
        if (unit.equals(s)){
        	unit = "";
        } else {
            s = s.replaceFirst("(.*)\\bUNIT \\d+(?:\\s*-\\s*\\d+)?(.*)", "$1$2").trim();        	
        }                
        
        String[] ret=new String[]{"", ""};
        if (s.toUpperCase().startsWith("PO BOX") 
        		|| s.matches("(?i)\\b(CR|COUNTR?Y ROAD|CO RD|FM) \\d+")) { // fix for bug #2341
            ret[0]="";
            ret[1]=s;
        }	else {
	        	Matcher mCRFM = paddrCRFM.matcher(s);// Texas AtascosaAO  
		    	if(mCRFM.find()){
		    		if (mCRFM.group(1) == null){
		    			ret[0]= "";
		    		} else {
		    			ret[0]= mCRFM.group(1);
		    		}
		            ret[1]=mCRFM.group(2);
		    	} else{
		        	Matcher m = paddr3.matcher(s);
		        	if(m.find()){
		        		ret[0]=m.group(2);
		                ret[1]=m.group(1);
		        	}else{
		        		Matcher ma=paddr5.matcher(s);	// OH Franklin AO PID 010-148347-00, Adr: 1832 BAIRSFORD DR -834 (str# is 1832-1834)
			            if (ma.find()) {
			            	String n1 = ma.group(1);
			            	String n2 = ma.group(3);
			            	int len = n1.length()-n2.length();
			            	switch (len){
			            		case 1: n2 = n1.substring(0,1) + n2; break;
			            		case 2: n2 = n1.substring(0,2) + n2; break;
			            	}
			                ret[0]=n1+"-"+n2;
			                ret[1]=ma.group(2);
			            } else {
				            ma=paddr1.matcher(s);
				            if (ma.find()) {
				                ret[0]=ma.group(1);
				                if(StringUtils.isNotEmpty(ma.group(2))) {
				                	ret[0] += ma.group(2);
				                }
				                ret[1]=ma.group(3);
				            } else {
				                ma=paddr2.matcher(s);
				                if (ma.find()) {
				                    ret[0]=ma.group(3);
				                    if (ma.group(2) != null && ma.group(2).length()>0){
				                    	ret[1]=ma.group(2) + " " + ma.group(1)+(ma.group(4)==null?"":ma.group(4));
				                    } else {
				                    	ret[1]=ma.group(1)+(ma.group(4)==null?"":ma.group(4));
				                    }                    	
				                } else{
				                	ma = paddr6.matcher(s);
				                	if (ma.find()){
				                		ret[0] = ma.group(1);
				                		ret[1] = ma.group(2);
				                	} else ret[1]=s;
				                }
				                    
				            }
			            }
		        	}
		        }
        	}
        if (unit.length()>0){
        	ret[1] = ret[1] + " " + unit;
        }       
       
        if (ret[0] == null){
			ret[0]= "";
        }
			
        return ret;
    }
    

    public static String StreetNo(String s) {
        return parseAddress(s)[0];
    }

    public static String StreetName(String s) {
        return parseAddress(s)[1];
    }

    public static String StreetNoShelbyAO(String s) {
        return parseAddressShelbyAO(s)[0];
    }

    public static String StreetNameShelbyAO(String s) {
        return parseAddressShelbyAO(s)[1];
    }

    private static String big="(?:\\b?\\d+(?:\\s*\\w)?\\b|\\b\\w(?:\\d\\w*)?\\b|\\b\\w+\\b)(?:(?:\\s*-\\s*|\\s*&\\s*|\\s*AND\\s*|\\s*,\\s*|\\s+)(?:\\b\\d+\\w?\\b|\\b\\w(?:\\d\\w*)?(?!')\\b))*"; 
    private static String[] headers={"(?i)\\bLO?TS? ", null, "(?i)\\bSEC(?:TION)? ", "(?i)\\bPH(?: ?ASES?)?[ ]*", "(?i)\\b(?:UNIT|NO) ", "(?i)\\bBL(?:OC)?KS? ", "(?i)\\bCOND(?:OMINIUMS?)? ", "(?i)\\b(?:BLDG|BUILDING) ", "(?i)\\bTRACTS? "};
    private static String[] headersRuthRO={"(?i)\\bLO?TS? ", null, "(?i)\\bSEC(?:TION)? ", "(?i)\\bPH(?:ASES?)? ", "(?i)\\b(?:UNIT|NO) ", "(?i)\\bBL(?:OC)?K ", "(?i)\\bCOND(?:OMINIUMS?)? ","(?i)DISTRICT"};
    private static String bigRutherford="(\\s+?)(\\d+)*";
    private static Pattern town=Pattern.compile("(?<!^)\\bTOWN(?!(HOME|HOUSE)S?)");
    /*
    private static String[] SubdivNashvilleAOnew(String s) {
        String[] r=new String[headers.length];
        ///manareala pt noul Davidson AO
        if(!ro.cst.tsearch.servers.response.Parser.useoldavidsonAO.equals("true"))
             s=s.replaceFirst("(.*)\\s+[A-Z0-9]+$","$1");
        s=s.replaceAll("\\s+", " ").trim();
        s=s.replaceAll("\\b\\.", "");
        s=s.replaceAll("[()]", "");
        s=s.replaceAll("(?i)\\d+'?\\s*X\\s*\\d+'?", "");
        s=s.replaceAll("(?i)\\b\\d+[nsew]\\d+[nsew]\\b", "");
        s=s.replaceAll("(?i)\\b((OF )?RE-?)?SUBD?(REV)?( OF)?\\b", "");
        s=s.replaceAll("(?i)\\bS[/\\-]?D\\b", "");
        s=s.replaceAll("(?i) TO ", "-");
        s=s.replaceAll("(?i)\\b(& )?(PT|PART)S?( OF)?\\b", "");       

        if (s.startsWith("THE ")) {
            s=s.substring(4);
        }

        // "just to see how it's working..."  >>
        Matcher m=town.matcher(s);
        if (m.find()) {
            s=s.substring(0, m.start());
        }
        // <<


        for (int i=0; i<headers.length; i++)
            if (headers[i]!=null) {
                // init
                r[i]="";
                Matcher ma=Pattern.compile(headers[i]+"("+big+")").matcher(s);
                while (ma.find()) {
                    r[i]+=ma.group(1).replaceAll("[,&]|AND", " ")+" ";
                    r[i]=r[i].replaceAll("\\s+", " ");
                }
                r[i]=r[i].trim(); //sau, daca se schimba separatorul substr(0, len-1)
                s=s.replaceAll(headers[i]+"("+big+")", "");
                r[i]=Roman.transformToArabic(r[i]);
            }

        r[1]=s.replaceAll("\\s+|,", " ").trim();

        return r;
    }
    */
    /////////////////////parser for davidsonAOnew
//  0- name
    //1- lot
    //2- apt
    //3- unit
    //4- sec
    //5- phase
    private static final String lotDavidson = "(\\d+|\\b[A-Z](?!')\\b|\\d+\\-[A-Z]\\b)"; 
    private static final String hdrsDavidsonAOnew[]={null,"\\bL(?:(?:O?)T(?:S?))?","\\bAPT","\\bUNIT","\\bSEC","\\b(?:PHASE|PH|P)"};
    private static final String dataformatDavidsonAOnew[]={null,"("+lotDavidson+"(\\s*(&|AND|,|-|\\s+TH\\s+|\\s+THRU\\s+|\\s)\\s*"+lotDavidson+")*)","([A-Z]?)(-?)\\d+[A-Z]?","\\w+(?:\\-\\w+)?","\\d+","\\d+(?:\\-[A-Z])?"};   											  
    
    private static String[] SubdivparseDavidsonAOnew(String s)
    {
        String rez[]=null;
        rez=new String[hdrsDavidsonAOnew.length+1];
        
        for(int i=0;i<rez.length;rez[i++]=null);
        
        if(s==null || s.trim().length()==0) return rez;
        
        String tmp=s.toUpperCase().trim(); 
        boolean isAddress = false;
        boolean isCondo = s.matches(".*\\bCOND\\.?O?(MINIUM)?$");
        
        ///cleaning         
        tmp=tmp.replaceAll("\\s+"," ");  
        // remove letters followed by ".", in most of the cases these are just name initials, but don't remove them when they 
        // are a block, unit, section or phase value  (ex. 1900 RICHARD JONES, HOWERTON JEFFREY H)
        String pattern = "(?<=(?:BLO?C?K|APT|UNIT|SEC|PH?(?:ASE)?)(?:\\.\\s|\\s|\\.))(\\b[A-Z]\\.)+";
        Pattern p = Pattern.compile(pattern);
        Matcher match = p.matcher(tmp);
        while (match.find()){
        	tmp = tmp.replaceFirst(pattern, match.group().replaceAll("\\.", ""));
        }
        tmp=tmp.replaceAll("\\b[A-Z]\\.", "");
        
        //remove all "." characters
        tmp=tmp.replaceAll("[\\.]"," ");          
        tmp=tmp.trim();
                		        	
        //always remove last token; it's garbage
        if (!tmp.matches(".*\\b(PK|PARK|GREEN|MEADOWS|EST|ESTATES|PROP|HILLS|ST|LN|ROAD|CO|CITY|TOWN|GORDON|COND)$")) {        			
        	tmp=tmp.replaceAll("(.*)\\s+([^0-9]*)$","$1 "); 
        }
        
        tmp=tmp.replaceAll("\\s+P-"," P ");
        //tmp=tmp.replaceAll("RE-SUB\\s+LOT(S?)"," "); 
        tmp=tmp.replaceAll("#"," ");
        // set de rezlvari punctuale pe care eu (VladP) le-am pus in lipsa de solutii globale
        tmp=tmp.replaceAll("ZONE LOTS? (DIV)?","");
        tmp=tmp.replaceAll("\\bLOTS? DIV\\b","");
        tmp=tmp.replaceAll("LOT NUMBER","");        
        tmp=tmp.replaceAll("(\\bTR?A?C?T\\b)(.*)","$2");        
        tmp=tmp.replaceAll("(\\s|&)*\\bRESERVE\\s+PARCEL\\s+[A-Z]\\b", " ");  //832 FONNIC DR   
        
        // general & particular lots cleaning
        
        // remove PT/PTS when followed by lot numbers
        //tmp=tmp.replaceAll("(\\s+&){0,1}\\s+PT\\s+\\d*(\\s*&\\s*\\d+)*","");        
        tmp=tmp.replaceAll("(?:\\s+&|\\s+AND)?\\s*\\bPTS?(\\s+\\d+(\\s*(&|,)\\s*\\d+)*)", " $1");
        
        // replace "L<number>" with "LOT <number>"  (1456 OCOEE)
        tmp=tmp.replaceAll("\\bL(\\d+)\\b", "LOT $1"); 
        
        // remove irellevant lots following 1st or 2nd rev and/or re-sub; lots are defined as a list of lots (number or letter) separated by space, comma, &, AND
        // but there are exceptions to this rule (Anita C. Lowrance); for the exceptions the strig prefix should be removed before applying the rule
        tmp = tmp.replaceAll("\\b\\d+(ST|ND|RD|TH)\\s+(?=REV\\b)", "");
        String regexp1 = "\\bREV";
        String regexp2 = "(\\bRE(-|\\s+)?)?SUB\\b,?(\\s+OF.*?)?"; 
        if(s.equalsIgnoreCase("LOT 5  RE-SUB LOTS 1&3 TH 7 HIGHLANDS OF BRENTWOOD SEC 4")) { //Anita C. Lowrance
        	tmp=tmp.replaceFirst(regexp2, "");
        }
        //tmp=tmp.replaceFirst("\\s("+regexp1+"|"+regexp2+"|"+regexp1+"\\s+"+regexp2+")\\s+(LOTS\\s+|L\\s+)?\\d+\\s*(\\s*(&|AND|,)\\s*(\\d+|[A-Z]))*", "");
        tmp=tmp.replaceFirst("\\s("+regexp1+"\\s+"+regexp2+"|"+regexp1+"|"+regexp2+")\\s+((LOTS|LTS|LOT|L)\\s+)?"+dataformatDavidsonAOnew[1], " ");
        
        tmp=tmp.replaceAll("\\b(RE(-?))?(?<!CO\\s)SUB\\b"," "); //don't remove "SUB" when "CO SUB" (bug# 128)
                        
        //if LOT <lot1> .. LOT/LT <lot2>  then consider lot as being "<lot1> <lot2>"
        //tmp=tmp.replaceFirst("\\s*(?:L|LT|LOT)\\s*"+lotDavidson+"\\s+(.*?)(?:,|&|\\s)+(?:L|LT|LOT|LOTS|LTS)\\s*"+lotDavidson+"\\s", " LOTS $1 $3 $2 ");
        tmp=tmp.replaceFirst("\\s*\\b(?:LOT|LT|L)\\s+"+lotDavidson+"\\s+(.*?)(?:,|&|\\s)+(?:LOTS|LTS|LT|LOT|L)\\b\\s*"+dataformatDavidsonAOnew[1], " LOTS $1 $3 $2");
                        
        // phase cleaning 
        // add a space between phase header and phase token when header and token are not separated (3104 HIDDEN CREEK)
        tmp=tmp.replaceAll("("+hdrsDavidsonAOnew[5]+")("+dataformatDavidsonAOnew[5]+")", "$1 $2");
        // replace "ONE" with "1" when next to phase (301 HERITAGE)
        tmp=tmp.replaceAll("("+hdrsDavidsonAOnew[5]+")\\s+ONE\\b", "$1 1");
        // replace "-Phase <number>-" with "Phase <number>" (301 HERITAGE)
        tmp=tmp.replaceAll("\\-+("+hdrsDavidsonAOnew[5]+")\\s+("+dataformatDavidsonAOnew[5]+")\\-+", " $1 $2 ");
        
        tmp=tmp.replaceAll("\\s+"," ");        
                        
        for(int i=0;i<dataformatDavidsonAOnew.length;i++)
        {
           if((hdrsDavidsonAOnew[i]!=null && dataformatDavidsonAOnew[i]!=null && rez[i]==null)) //no data parsed yet
           {
               Pattern pt=Pattern.compile(hdrsDavidsonAOnew[i]+"\\s("+dataformatDavidsonAOnew[i]+")");
               Matcher m=pt.matcher(tmp);               
               if(m.find())
               {
                  String t=m.group(1);
                  if(t!=null && t.trim().length()>0)
                  {
                      int ist,iend;
                      ist=m.start();iend=m.end();
                      tmp=tmp.substring(0,ist)+tmp.substring(iend);
                      switch (i) { 
                      	case 1: { //LOT                        		
							// particular lot cleaning
                      		
							//replace "LOT <number> <letter> <letter> <subdivisiom>" with "LOT <number> <subdivision> <letter> <letter>"
                      		//Ex: Tax ID: 19-119.01-0-426.00 legal: LOT 22 W G THUSS ADDN; Tax ID: 19-071.14-0-079.00 legal: LOT 9 W G BUSH SUB (bug #313)                         		
                      		Matcher ma = Pattern.compile("(\\d+)(\\s+[A-Z]\\s+[A-Z])").matcher(t);
                      		if (ma.find() && tmp.matches("\\W+(\\w+\\b).*")) {
                      			tmp = tmp.replaceFirst("(\\w+\\b).*", "$1") + ma.group(2) + tmp.replaceFirst("\\w+\\b(.*)", "$1");
                      		}
							t=t.replaceAll("(\\d+)\\s+[A-Z]\\s+[A-Z]", "$1"); 
							
							//in some cases no lot info should be parsed from legal, else the seach on RO doesn't bring correct document :(
							if(s.equalsIgnoreCase("UNIT 27 LOT 17 PATIO VILLA SEC 3 PH 2")) { // (1000 PATIO)
								tmp=tmp.replaceFirst("LOT 17 ", "");
							}        
							    
							// tmp = tmp.replaceAll("(&\\s+[0-9A-Z]+\\s+)*",""); // this will remove also tokens from subdivision name (ex. 3032 lakeshore)
							tmp = tmp.replaceAll("(&\\s+([0-9]+[A-Z]+|[A-Z]+[0-9]+)+\\s+)*","");
							tmp = tmp.trim();
							
							t=t.replaceAll("(\\d+)\\-([A-Z])", "$1$2");
							t=t.replaceFirst("^0+(\\d+)", "$1");
							break;
                      	}
                      	case 3: { //UNIT
                      		// if unit = <number>-<letter>, letter should be removed in all cases?
                      		if (s.equals("UNIT 45-C TIMBER LAKE CONDOMINIUM")) { //148 NORTH TIMBER DR
                      			t=t.replaceFirst("(\\d+)\\-[A-Z]", "$1");
                      		}
                      		break;
                      	}
                      	case 4: { //SEC
                    		//SEC - if multiple sections, remove the additional sections from subdiv (only first sec is parsed for further searching)
                      		tmp=tmp.replaceAll(hdrsDavidsonAOnew[i]+"\\s+("+dataformatDavidsonAOnew[i]+")", "");
                      		break;
                      	} 
                      	case 5: { // PHASE                     	  
                    		//PHASE - if multiple phases, remove the additional phases from subdiv (only first phase is parsed for further searching) 
                      		tmp=tmp.replaceAll(hdrsDavidsonAOnew[i]+"\\s+("+dataformatDavidsonAOnew[i]+")", "");
                      		
                      		// remove "-" from phase, if present before a letter
                    		t=t.replaceAll("(\\d+)\\-([A-Z])", "$1$2"); // 1416 MARKET SQ
                    		break;
                    	} 
                     } 
                      
                      rez[i]=t.trim();
                  }  
                  else
                  {
                      tmp=tmp.replaceAll(hdrsDavidsonAOnew[i],"");                 
                  }                  
                  
               }
               if(rez[i]==null) rez[i]="";
           }    
           if (i == 1 && rez[1].length() == 0) { // when not lot can be parsed and legal contains a street suffix, don't parse anything because it's an address -bug #909
        	   if (s.matches(".*\\b(CT|CIR|CR|AVE|DR|ST|RD|ROAD|TRACE|TR|LN|WAY|HGWY|BLVD|PK)\\b.*")){
        		   for (int j=0; j<dataformatDavidsonAOnew.length; j++){
        			   rez[j] = "";
        		   }
        		   isAddress = true;
        		   break;
        	   }
           }
        }             
        
        if (!isAddress) {
        	// additional cleaning        
	        tmp=tmp.replaceAll("BLO?C?K\\s+([0-9A-Z]+)","");
	        tmp=tmp.replaceAll("BLDG\\s+([0-9A-Z]+)","");
	        tmp=tmp.replaceAll("\\s*(&\\s*)?P/O\\s"," ");
	        tmp=tmp.replaceAll("(\\s|&)+PT(S?)\\s+(OF\\s+)?CL(OSED)?(\\s+(ALLEY|ST))?"," ");        
	                              
	        tmp=tmp.replaceAll("(?<!A)PT\\s+","");
	        tmp=tmp.replaceAll("\\b(FIRST|1ST|SECOND|2ND)\\s+REVISED\\b", ""); // 301 HERITAGE
	        tmp=tmp.replaceAll("\\-*\\b(1ST||2ND|3RD)\\s+ADDN?\\b", ""); // 105 VISTA / 125 TWO MILE
	        tmp=tmp.replaceAll("\\sREV\\b", ""); // 1416 MARKET
	        tmp=tmp.replaceAll("\\bHOME\\b", ""); // 4616 GRANNY WHITE PK 
	        tmp=tmp.replaceAll("\\bPARTS\\s+OF\\b", "");
	        //remove "COND" from subdivision name if the original Legal doesn't end with "COND" (as in 6680 CHARLOTTE, owner Louise D BAKER) 
	        if(!s.endsWith("COND")) {
	        	tmp=tmp.replaceAll("\\bCOND\\b", ""); //5510 COUNTRY, owner VIVIENNE R GRAHAM
	        }
	        tmp=tmp.replaceAll("\\b(LOTS|LOT|LTS|LT)\\b", "");
	        tmp=tmp.replaceAll("\\bRLTY\\b", "");
	        
	        tmp=tmp.replaceAll(",", "");
	        tmp=tmp.trim();
	        // if subdivision name ends with a number, remove it
	        tmp=tmp.replaceFirst("\\d+$", "");
	        // if subdivision name begins with a number, remove it
	        tmp=tmp.replaceFirst("^\\d+", "");
	        // if subdivision name begins with a word formed by a letter followed by number, then remove them
	        tmp=tmp.replaceFirst("^[A-Z]-?\\d+\\b", "");
	        
	        // remove last token, if garbage 
	        tmp = tmp.replaceFirst("\\bAPT\\s*$", ""); // fix for bug #1731
	        
	        rez[0]=tmp.trim(); 
        }
        if (isCondo){
        	rez[6] = rez[0];
        }
        return rez;  
    }

    public static String cleanLegalDavidsonCnT(String s){      	
    	s = s.replaceAll("\\bSECTIO N\\b", "SECTION");	// fix for bug #2193
    	s = s.replaceAll("\\bR EV\\b", "REV");   		// fix for bug #2198
    	s = s.replaceAll("\\bHIGHLAND S OF\\b", "HIGHLANDS OF"); // 5656 OAKES 
    	s = s.replaceAll("\\bWH ITLAND\\b", "WHITLAND"); // PID 10413009800
    	s = s.replaceAll("\\bS UB\\b", "SUB");
    	s = s.replaceAll("\\bSU B\\b", "SUB");
    	s = s.replaceAll("\\bSE C\\b", "SEC"); // PID 087050A05300CO
    	s = s.replaceAll("\\bLO TS\\b", "LOTS"); // PID 11905030300
    	s = s.replaceAll("\\bP/O\\b", ""); // PID 10502044300
    	s = s.replaceAll("\\bRE\\s*-\\s*SUB\\b", "RE-SUB"); // PID 05414010100
    	s = s.replaceAll("\\bSEC OND\\b", "2ND"); // PID 042070A03500CO
    	s = s.replaceAll("\\bB LVD\\b", "BLVD"); // PID 06402009600
    	s = s.replaceAll("\\bL AKE\\b", "LAKE"); // PID 09801007100
    	s = s.replaceAll("\\bEDENWOL D\\b", "EDENWOLD"); // PID 03409011000   
    	s = s.replaceAll("\\bPHAS E\\b", "PHASE"); // PID 142090B82000CO
    	s = GenericFunctions.replaceNumbers(s);
    	return s;
    }
    
    ////////////////////////////////////////////////////////
    private static String[] SubdivNashvilleAO(String s) {
        String[] r=new String[headers.length];
        s=s.replaceAll("\\s+", " ").trim();
        s=s.replaceAll("\\b\\.", "");
        s=s.replaceAll("[()]", "");
        s=s.replaceAll("(?i)\\d+'?\\s*X\\s*\\d+'?", "");
        s=s.replaceAll("(?i)\\b\\d+[nsew]\\d+[nsew]\\b", "");
        s=s.replaceAll("(?i)\\b((OF )?RE-?)?SUBD?(REV)?( OF)?\\b", "");
        s=s.replaceAll("(?i)\\bS[/\\-]?D\\b", "");
        s=s.replaceAll("(?i) TO ", "-");
        s=s.replaceAll("(?i)\\b(& )?(PT|PART)S?( OF)?\\b", "");                	 
        
        // particular cleaning 
        s=s.replaceAll("(?i)\\bU(?:NIT)?(\\d+)\\b", "UNIT $1"); //Knox: 420 KENDALL; 9849 SAINT GERMAINE DR, CONDRY MARY ANN
        s=s.replaceAll("(?i)\\bU(\\d+)ANX", "UNIT $1 ANX"); //Knox: 420 KENDALL;        
        s=s.replaceAll("(?i)\\bANX\\s(\\d{4}|\\d{1,2}/\\d{1,2}/\\d{1,2})((\\sORD)?\\s\\d+(-|\\d+)*)?", ""); //Knox: 420 KENDALL; 7612 LUSCOMBE
        s=s.replaceAll("(?i)\\bESTATESUNIT\\b", "ESTATES UNIT"); //Knox: 7912 BISHOP RD
        s=s.replaceAll("\\bLOTS? DIV\\b", ""); //Davidson: 3118 WINDEMERE  (bug #315), 3921 Westmont (b2593)
        s=s.replaceAll("\\bRESTATED VACATION PLAN.*", ""); //Davidson RO 199805120928959 (bug #3170): 
        s = s.replaceAll("(.+)\\s+ADD\\b", "$1"); // KNOX EP 107GD011 B 
		//replace "LOT <number> <letter> <letter>" with "LOT <number>"; letters will be added to subdivision name  
  		//Ex: PID 11901042600 legal: LOT 22 W G THUSS ADDN; PID 07114007900 legal: LOT 9 W G BUSH SUB (bug #313)
        s = s.replaceAll("("+headers[0]+"\\s*\\d+)\\s+[A-Z]\\s+[A-Z](\\W\\w+\\b.*)", "$1$2");
        
        s=s.replaceAll("%","");
        s=s.trim();

        if (s.startsWith("THE ")) {
            s=s.substring(4);
        }

        // "just to see how it's working..."  >>
        Matcher m=town.matcher(s);
        if (m.find()) {
            s=s.substring(0, m.start());
        }
        // <<
        
        s = ReplaceIntervalWithEnumeration(s);  

        for (int i=0; i<headers.length; i++)
            if (headers[i]!=null) {
                // init
                r[i]="";
                Matcher ma=Pattern.compile(headers[i]+"("+big+")").matcher(s);
                while (ma.find()) {
                    r[i]+=ma.group(1).replaceAll("[,&]|AND", " ")+" ";
                    r[i]=r[i].replaceAll("\\s+", " ");
                }
                r[i]=r[i].trim(); //sau, daca se schimba separatorul substr(0, len-1)
                s=s.replaceAll(headers[i]+"("+big+")", "");
                r[i]=Roman.transformToArabic(r[i]);
            }

        r[1]=s.replaceAll("\\s+|,", " ").trim();
        if (r[1]!=null) {
            r[1] = r[1].replaceAll("LOTS","");
            // can't generalize this for the sake of bug #2588, because it causes bug #3170
            // r[1] = r[1].replaceAll("(?is)\\bAT\\s+.*", "");
            r[1] = r[1].replaceFirst("^HEATH PL AT FRANKLIN$", "HEATH PLACE");
        }
        r[0] = r[0].replaceAll("-", "");
        r[0] = RemoveDuplicateValues(r[0]);				// Davidson TR:  203 Brittany Park Cr
        r[0] = ReplaceEnumerationWithInterval(r[0]);
        
        r[3] = r[3].replaceAll("-", "");				// Davidson RO: B2194
        
        return r;
    }
   
    private static String[] SubdivNashvilleTR(String s) {    	    	
        String[] r=new String[headers.length];                
        
        s=s.replaceAll("\\s+", " ").trim();
        
        if (s.contains("NO LEGAL DESCRIPTION")){ // fix for bug #909, second issue
        	return r;
        }
        
        s=s.replaceAll("\\bSE C\\b", "SEC"); 	// Davidson TR Pid=087050A05300CO       
        s=s.replaceAll("(?<!\\bBLK\\.? )\\b[A-Z]\\.",""); //bug #140
        s=s.replaceAll("\\b\\.", "");
        s=s.replaceAll("[()]", "");
        s=s.replaceAll("(?i)\\d+'?\\s*X\\s*\\d+'?", "");
        s=s.replaceAll("(?i)\\b\\d+[nsew]\\d+[nsew]\\b", "");
        s=s.replaceAll("(?i)\\bS[/\\-]?D\\b", "@");
        s=s.replaceAll("\\bSU B\\b", "SUB"); // Davidson: 203 Brittany Park Cr (bug #1766)
        s=s.replaceAll("(?i)\\b((OF )?RE-?)?SUBD?(REV)?( OF)?\\b", "@");                
        s=s.replaceAll("(?i) (TO|TH) ", "-");        
        s=s.replaceAll("(?i)(?:\\s+&|\\s+AND)?\\s*\\b(PT|PART)S?( OF)?\\b", "");                
        s=s.replaceAll("BLDG\\s+([0-9A-Z\\-]+)","");
        s=s.replaceAll("APT\\s+#?\\s*([0-9A-Z\\-]+)","");  
        s=s.replaceAll("\\bZONE\\b", ""); //Davidson: 3118 WINDEMERE  (bug #315)             
        s=s.replaceAll("(\\s|&)*\\bRESERVE\\s+PARCEL\\s+[A-Z]\\b", " ");  //Davidson TR: 832 FONNIC DR  
        s=s.replaceAll("\\bL(\\d+)\\b", "LOT $1"); // Davidson TR: 1456 OCOEE 
        s=s.replaceAll("\\bCL(OSED)? (ST|ALLEY)\\b", "");        
		//replace "LOT <number> <letter> <letter>" with "LOT <number>"; letters will be added to subdivision name  
  		//Ex: PID 11901042600 legal: LOT 22 W G THUSS ADDN; PID 07114007900 legal: LOT 9 W G BUSH SUB (bug #313)
        s = s.replaceAll("("+headers[0]+"\\s*\\d+)(\\s+[A-Z]\\s+[A-Z])(\\W\\w+\\b)(.*)", "$1$3$2$4");        
        s=s.trim();
        
        if (s.startsWith("THE ")) {
            s=s.substring(4);
        }

        // "just to see how it's working..."  >>
        Matcher m=town.matcher(s);
        if (m.find()) {
            s=s.substring(0, m.start());
        }
        // <<

        s = ReplaceIntervalWithEnumeration(s);       

        for (int i=0; i<headers.length; i++)
            if (headers[i]!=null) {
                // init
                r[i]="";
                Matcher ma=Pattern.compile(headers[i]+"("+big+")").matcher(s);
                while (ma.find()) {
                	String value = ma.group(1);
                	String[] values = value.split("\\s+");
                	boolean removeAll = true, skip = false;
                	if (values.length > 1){
                		if ((values[0].matches("\\d+") && values[1].matches("[A-Z]+")) || (values[0].matches("[A-Z]+") && values[1].matches("\\d+"))){
                			String nextToken = s.replace(ma.group(0), "").replaceFirst("([A-Z]+)\\b.*", "$1").trim() + " ";
                			for (int j=0; j<headers.length; j++){
                				if (headers[j] != null){
                					if (nextToken.matches(headers[j])){
                						skip = true;
                						break;
                					}
                				}
                			}
                			if (!skip){
                				value = values[0];
                				removeAll = false;
                				s = s.replace(ma.group(0).replace(ma.group(1), value), "").trim();
                			}
                		}	
                	}
                    if (removeAll)
                    	s = s.replace(ma.group(0), "");
                    r[i]+=value.replaceAll("[,&]|AND", " ")+" ";
                    r[i]=r[i].replaceAll("\\s{2,}", " ");
                }
                r[i]=r[i].trim(); //sau, daca se schimba separatorul substr(0, len-1)                
                r[i]=Roman.transformToArabic(r[i]);
            }

        s=s.replaceAll("\\s+|,", " ").trim();
        //if subdiv name begins with a number, remove it //bug #140
        s=s.replaceFirst("^\\d+\\b", "");        
        //if subdiv name ends with a number, remove it
        s=s.replaceFirst("\\b\\d+$", "");
        s=s.replaceAll("\\bLOTS\\b", ""); // bug #140
        s=s.replaceAll("\\bRE-?SUB\\b", ""); // bug #1766
        s=s.replaceAll("@", "");
        s=s.replaceAll("\\-*\\b\\d+(ST|ND|RD|TH)\\s+ADDN?\\b", ""); // 105 VISTA / 125 TWO MILE
        s=s.replaceAll("\\b\\d+(ST|ND|RD|TH)\\s+REVISED( PLAN)?\\b", ""); // 301 HERITAGE
        s = s.replaceFirst("\\s*[-&]+\\s*$", "");
        s = s.replaceFirst("^\\s*[-&]+\\s*", "");
        r[1]=s.trim().replaceFirst("(.+)\\s*@.*", "$1");        
        
        // when the legal description contains an address (i.e. a street suffix and no lot)
        if (s.matches(".*\\b(CT|CIR|CR|AVE|DR|ST|RD|ROAD|TRACE|TR|LN|WAY|HGWY|BLVD|PK)\\b.*") // Davidson TR: addr 545 MONCRIEF
        	&& StringUtils.isEmpty(r[0])){ //  Davidson TR: addr 214 CRAIGHEAD  
        	 for (int i=0; i<headers.length; i++){
        		 r[i]="";
             }
        }
        
        //remove duplicates lots
//        r[0] = StringFormats.RemoveDuplicateValues(r[0]);
//        r[0] = StringFormats.ReplaceEnumerationWithInterval(r[0]);

        return r;
    }
    private static String[] SubdivRutherfordRO(String s) {
        String[] r=new String[headersRuthRO.length];
        ///manareala pt noul Davidson AO
       // s=s.replaceFirst("(.*)\\s+[A-Z0-9]+$","$1");
        s=s.replaceAll("\\s+", " ").trim();
        s=s.replaceAll("\\b\\.", "");
        s=s.replaceAll("[()]", "");
        s=s.replaceAll("(?i)\\d+'?\\s*X\\s*\\d+'?", "");
        s=s.replaceAll("(?i)\\b\\d+[nsew]\\d+[nsew]\\b", "");
        s=s.replaceAll("(?i)\\b((OF )?RE-?)?SUBD?(REV)?( OF)?\\b", "");
        s=s.replaceAll("(?i)\\bS[/\\-]?D\\b", "");
        s=s.replaceAll("(?i) TO ", "-");
        s=s.replaceAll("(?i)\\b(& )?(PT|PART)S?( OF)?\\b", "");        
        s=s.replaceAll(",", "");
        if (s.startsWith("THE ")) {
            s=s.substring(4);
        }

        // "just to see how it's working..."  >>
        Matcher m=town.matcher(s);
        if (m.find()) {
            s=s.substring(0, m.start());
        }
        // <<


        for (int i=0; i<headersRuthRO.length; i++)
            if (headersRuthRO[i]!=null) {
                // init
                r[i]="";
                Matcher ma=Pattern.compile(headersRuthRO[i]+"("+bigRutherford+")").matcher(s);
                while (ma.find()) {
                    r[i]+=ma.group(2).replaceAll("[,&]|AND", " ")+" ";
                    r[i]=r[i].replaceAll("\\s+", " ");
                }
                r[i]=r[i].trim(); //sau, daca se schimba separatorul substr(0, len-1)
                s=s.replaceAll(headersRuthRO[i]+"("+big+")", "");
                r[i]=Roman.transformToArabic(r[i]);
            }

        r[1] = s.replaceAll("\\s+|,", " ").trim();
        
        return r;
    }
    private static String[] SubdivWilliamsonAO(String s) {
        String[] r=new String[headers.length];        
        s=s.replaceAll("\\s+", " ").trim();
        s=s.replaceAll("\\b\\.", "");
        s=s.replaceAll("[()]", "");
        s=s.replaceAll("(?i)\\d+'?\\s*X\\s*\\d+'?", "");
        s=s.replaceAll("(?i)\\b\\d+[nsew]\\d+[nsew]\\b", "");
        s=s.replaceAll("(?i)\\b((OF )?RE-?)?SUBD?(REV)?( OF)?\\b", "");
        s=s.replaceAll("(?i)\\bS[/\\-]?D\\b", "");
        s=s.replaceAll("(?i) TO ", "-");
        s=s.replaceAll("(?i)\\b(& )?(PT|PART)S?( OF)?\\b", "");
        s=s.replaceAll("(?i)(.+)\\s+CONDOS\\s+(\\1)", "$1"); // 3326 ASPEN GROVE #140

        if (s.startsWith("THE ")) {
            s=s.substring(4);
        }

        // "just to see how it's working..."  >>
        Matcher m=town.matcher(s);
        if (m.find()) {
            s=s.substring(0, m.start());
        }
        // <<


        for (int i=0; i<headers.length; i++)
            if (headers[i]!=null) {
                // init
                r[i]="";
                Matcher ma=Pattern.compile(headers[i]+"("+big+")").matcher(s);
                while (ma.find()) {
                    r[i]+=ma.group(1).replaceAll("[,&]|AND", " ")+" ";
                    r[i]=r[i].replaceAll("\\s+", " ");
                }
                r[i]=r[i].trim(); //sau, daca se schimba separatorul substr(0, len-1)
                s=s.replaceAll(headers[i]+"("+big+")", "");
                r[i]=Roman.transformToArabic(r[i]);
            }

        r[1] = s.replaceAll("\\s+|,", " ").trim();
        
        return r;
    }

    /* Replaces a first number interval from within a string with an enumeration of all the
     * numbers between and including the interval limits. A number interval is specified as
     * <number1> <separator> <number2>, where separator is one of "THRU", "TH", "TO", "-"     
     * */
    public static String ReplaceIntervalWithEnumeration(String s) {
        Pattern range=Pattern.compile("(?i)(?<!-)\\b(\\d+)\\s*(THRU|TH|TO|-)\\s*(\\d+)\\b(?!-)");
        Matcher ma=range.matcher(s);
        if(ma.find()) {
            long start=Long.parseLong(ma.group(1));
            long end=Long.parseLong(ma.group(3));
            StringBuilder rg= new StringBuilder();
            if(end - start <= 150) {
	            for(long i=start;i<=end;i++) {
	                rg.append(i).append(" ");
	            }
            }
	        s= s.substring(0,ma.start())+" "+rg.toString()+s.substring(ma.end());
        }
        return s.replaceAll("\\s+"," ").trim();    	
    	
    } 
    
    /* Replaces a first number_letter interval from within a string with an enumeration of all the
     * numbers-letters between and including the interval limits. A number interval is specified as
     * <number1letter1> <separator> <number2letter2>, where separator is one of "THRU", "TH", "TO", "-"     
     * */
    public static String ReplaceNumberLetterIntervalWithEnumeration(String s) {
        Pattern range=Pattern.compile("(?i)(?<!-)\\b(\\d+\\w*)\\s*(THRU|TH|TO|-)\\s*(\\d+\\w*)\\b(?!-)");
        Matcher ma=range.matcher(s);
        if(ma.find())
        {
            int start = -1;
            int end = -1;
            int startInt = -1;
			String startLetter = null;
			int endInt = -1;
			String endLetter = null;
			Matcher matcher = null;
            try {
            	start = Integer.parseInt(ma.group(1));
            } catch (Exception e) {
            	try {
					matcher = StringUtils.NUMBERS_LETTERS_PATTERN.matcher(ma.group(1));
					if(matcher.find()){
						if(matcher.group(2).length()==1){	//just one letter
							startInt = Integer.parseInt(matcher.group(1));
							startLetter = matcher.group(2).toUpperCase();
						}
					}
				} catch (Exception ex) {
				}
			}
            try {
            	end = Integer.parseInt(ma.group(3));
            } catch (Exception e) {
            	try {
					matcher = StringUtils.NUMBERS_LETTERS_PATTERN.matcher(ma.group(3));
					if(matcher.find()){
						if(matcher.group(2).length()==1){	//just one letter
							endInt = Integer.parseInt(matcher.group(1));
							endLetter = matcher.group(2).toUpperCase();
						}
					}
				} catch (Exception ex) {
				}
			}
            
            StringBuilder rg = new StringBuilder();
            
            if(start!=-1 && end != -1) {
            	if(start + 100 < end)
            		end = start + 100;	//maximum 100 lots
	            for(int i = start; i <= end; i++)
	            {
	                rg.append(i + " ");
	            }
            } else {
            	if ( start == -1 && end == -1 ) {
					//both contains more than numbers
					if( startLetter!=null && endLetter!=null && (startInt == endInt) ){
						char startChar = startLetter.charAt(0);
						char endChar = endLetter.charAt(0);
						if(startChar > endChar){
							char temp = startChar;
							startChar = endChar;
							endChar = temp;
						}
						for (char i = startChar; i <= endChar; i++){
							rg.append(startInt + String.valueOf(i) + " ");
						}
					} 
				}
            }
            s = s.substring(0,ma.start()) + " " + rg + s.substring(ma.end());
        }
        s = s.replaceAll("\\s+"," ").trim();    	
    	
    	return s;
    }
    
    /** 
     * Returns a string that contains numbers intervals instead of numbers enumerations.<br> 
     * The tokens in the input string are separated with space. Non-numbers are copied in the destination string as they are. 
     */
    public static String ReplaceEnumerationWithInterval(String s) {
    	String array[] = s.split(" ");
    	StringBuilder sb = new StringBuilder();
    	if (s.matches(".*\\b0\\b.*")){
    		sb.append("0 "); 
    	}
    	int iArray[] = new int[array.length];
    	int k = 0;
		for (int i=0; i<array.length; i++){
			if(array[i].matches("\\d{1,10}") && Long.parseLong(array[i])<=(new Long (Integer.MAX_VALUE)).longValue()){
				try {
					iArray[k] = Integer.parseInt(array[i]);
					k++;
				} catch (NumberFormatException e){
					// if the string cannot be parsed as an integer, just ignore it 
				}
			} else {
				sb.append(array[i]).append(" ");
			}
		}
    	Arrays.sort(iArray);
    	int j=0;
    	while ((j<iArray.length) && (iArray[j] == 0)) j++;
    	//if we have only 2 numbers, return them, without trying to compute an interval
    	if(k > 2){
	    	boolean startInterval = false;
	    	int start=0, end=0;
	    	for (int i=j; i<iArray.length-1; i++){
	    		if(!startInterval){
	    			if(iArray[i]+1 == iArray[i+1]){
		    			startInterval = true;
		    			start = iArray[i];
	    			} else {
	    				sb.append(iArray[i]).append(" ");
	    			}
	    		} else if(iArray[i]+1 != iArray[i+1]){
	    			end = iArray[i];
	    			startInterval = false;
	    			if(end-start > 1) {
	    				sb.append(start).append("-").append(end).append(" ");
	    			} else {
	    				sb.append(start).append(" ").append(end).append(" ");
	    			}
	    		} 
	    	}
	    	end = iArray[iArray.length-1];
	    	if(startInterval){
    			if(end-start > 1) {
    				sb.append(start).append("-").append(end);
    			} else {
    				sb.append(start).append(" ").append(end);
    			}	    		
	    	} else {
	    		sb.append(end);
	    	}
    	} else {
    		for(int i=j; i<iArray.length; i++) {
    			sb.append(iArray[i]).append(" ");
    		}
    	}    	
    	return sb.toString().trim();
    }
    
    /* Removes duplicate values from a string, if exist. Values are separated with space. 
     * */
    public static String RemoveDuplicateValues(String s) {
    	
    	
    	Set<String> values = new HashSet<String>();
    	values.addAll(Arrays.asList(s.split("[\\s]+")));
    	
    	/*
    	String[] tmp = s.split("\\s");
        int len = tmp.length;
        if (len > 1) {
        	for (int i=0; i<len-1; i++) {
        		for (int j=i+1; j<len; j++) {
        			if (tmp[i].length() >0 && tmp[i].equals(tmp[j])) {
        				s=s.replaceAll("\\b"+tmp[j]+"\\b\\s?", "");
        				tmp[j] = "";
        			}
        		}
        	}
        }        
        s = "";
        for (int i=0; i<len; i++)
        	s += tmp[i] + " "; 
        s=s.replaceAll("\\s+"," ").trim();
        
        */
    	
    	StringBuilder sb = new StringBuilder();
    	for (String string : values) {
    		sb.append(string).append(" ");
		}
    	if(sb.length() > 0) {
    		sb.deleteCharAt(sb.length() - 1);
    	}
        
    	return sb.toString();
    }
    
    public static String LotNashvilleAO(String s) {
        return SubdivNashvilleAO(s)[0];
    }
    
    public static String LotNashvilleAOnew(String s) {
        //return SubdivNashvilleAOnew(s)[0];
        String r=SubdivparseDavidsonAOnew(s)[1];
        r=r.replaceAll("[\\-/&,]"," ");
        
        r = ReplaceIntervalWithEnumeration(r);
        
        // if LOT info contains more lots, then check for duplicates and remove them
        r = RemoveDuplicateValues(r);       
        return r; 
    }
    
    public static String LotRutherfordRO(String s) {
        return SubdivRutherfordRO(s)[0];
    }

    public static String SubdivisionNashvilleAO(String s) {
        return SubdivNashvilleAO(s)[1];
    }
    
    public static String SubdivisionNashvilleAOnew(String s) {
        //return SubdivNashvilleAOnew(s)[1];
        return SubdivparseDavidsonAOnew(s)[0];
    }
    
   
    public static String SubdivisionNashvilleTR(String s) {
        return SubdivNashvilleTR(s)[1];
    }
    
    public static String BlockNashvilleTR(String s) {
        return SubdivNashvilleTR(s)[5];
    }
    
    public static String SubdivisionRutherfordRO(String s) {
        return SubdivRutherfordRO(s)[1];
    }
    public static String SubdivisionWilliamsonAO(String s) {
        //System.out.println("s: " + s + " lista: "+ SubdivWilliamsonAO(s) +" |Subdname: " + SubdivWilliamsonAO(s)[1]);
        return SubdivWilliamsonAO(s)[1];
    }

    public static String SectionWilliamsonAO(String s) {
        return SubdivWilliamsonAO(s)[2];
    }
    
    public static String SectionNashvilleAO(String s) {
    	String sec = SubdivNashvilleAO(s)[2];
    	sec = RemoveDuplicateValues(sec);
        return sec;
    }
    
    public static String SectionNashvilleAOnew(String s) {
        //return SubdivNashvilleAOnew(s)[2];
        return SubdivparseDavidsonAOnew(s)[4];
    }
    
    public static String PhaseWilliamsonAO(String s) {
        return SubdivWilliamsonAO(s)[3];
    }
    
    
    public static String PhaseNashvilleAOnew(String s) {
        //return SubdivNashvilleAOnew(s)[3];
        return SubdivparseDavidsonAOnew(s)[5];
    }
    
    public static String PhaseNashvilleAO(String s) {
        return SubdivNashvilleAO(s)[3];
    }
    
    
    public static String UnitNashvilleAO(String s) {
        return SubdivNashvilleAO(s)[4].replaceAll("-", "");
    }
    
    public static String UnitNashvilleAOnew(String s) {
        //return SubdivNashvilleAOnew(s)[4].replaceAll("-", "");        
        return SubdivparseDavidsonAOnew(s)[3].replaceAll("-", "");
    }

    public static String BlockNashvilleAO(String s) {
        return SubdivNashvilleAO(s)[5];
    }

    public static String CondNashvilleAO(String s) {
        return SubdivNashvilleAO(s)[6];
    }
    
    public static String CondNashvilleAOnew(String s) {
        return SubdivparseDavidsonAOnew(s)[6];
    }
    
    public static String BuildingNashvilleAO(String s) {
        return SubdivNashvilleAO(s)[7];
    }

    public static String TractNashvilleAO(String s) {
        return SubdivNashvilleAO(s)[8];
    }
    
    private static String bigMobile="(?:\\b\\d+\\w?\\b|\\b\\w(?:\\d\\w*)?\\b|\\b\\w+\\b)(?:(?:-|&|AND|,)(?:\\b\\d+\\w?\\b|\\b\\w(?:\\d\\w*)?\\b))*,?"; 
    private static String[] headersMobile={"(?i)(?:^| )L ", null, "(?i)(?:^| )S ", "(?i)(?:^| )PH(?:A?SES?)? ", "(?i)(?:^| )U ", "(?i)(?:^| )B "};
    private static String[] SubdivMobile(String s) {
        String[] r=new String[headersMobile.length];
        s=s.replaceAll("\\s+", " ").trim();
        s=s.replaceAll("\\b\\.", " ");
        s=parseBookAndPageMobile(s)[1];
        s=s.replaceAll("[()]", "");
        s=s.replaceAll("(?i)\\d+'?\\s*X\\s*\\d+'?", "");
        s=s.replaceAll("(?i)\\b\\d+[nsew]\\d+[nsew]\\b", "");
        s=s.replaceAll("(?i)\\b((OF )?RE-?)?SUBD?(REV)?( OF)?\\b", "");
        s=s.replaceAll("(?i)\\bS[/\\-]?D\\b", "");
        s=s.replaceAll("(?i) TO ", "-");
        s=s.replaceAll("(?i) THRU ", "-");
        s=s.replaceAll("(?i)\\b(& )?(PT|PART)S?( OF)?\\b", "");

        for (int i=0; i<headersMobile.length; i++)
            if (headersMobile[i]!=null) {
                // init
                r[i]="";
                Matcher ma=Pattern.compile(headersMobile[i]+"("+bigMobile+")").matcher(s);
                while (ma.find()) {
                    r[i]+=ma.group(1).replaceAll("[,&]|AND", " ")+" ";
                    r[i]=r[i].replaceAll("\\s+", " ");
                }
                r[i]=r[i].trim(); //sau, daca se schimba separatorul substr(0, len-1)
                s=s.replaceAll(headersMobile[i]+"("+bigMobile+")", "");
                r[i]=Roman.transformToArabic(r[i]);
            }

        r[1]=s.replaceAll("\\s+", " ").trim();

        return r;
    }

    public static String LotMobile(String s) {
        return SubdivMobile(s)[0];
    }

    public static String SubdivisionMobile(String s) {
        return SubdivMobile(s)[1];
    }

    public static String SectionMobile(String s) {
        return SubdivMobile(s)[2];
    }

    public static String PhaseMobile(String s) {
        return SubdivMobile(s)[3];
    }

    public static String UnitMobile(String s) {
        return SubdivMobile(s)[4];
    }

    public static String BlockMobile(String s) {
        return SubdivMobile(s)[5];
    }

    private static Pattern bkpgMobile=Pattern.compile("\\b(?:(?:(?:RE )?\\w[A-Z/]+) |(?:\\w\\s\\w) )?(?:RP|BK|VOL)[ /](\\d+/\\d+(?:,\\s*\\d+/\\d+)*),?");
    private static String[] parseBookAndPageMobile(String s) {
        String[] ret=new String[2]; // BkPg string and rest
        s=s.replaceAll("\\b\\.", " ");
        Matcher m=bkpgMobile.matcher(s);
        ret[0]="";
        while (m.find()) {
            if (ret[0].length()==0)
                ret[0]=m.group(1);
            else
                ret[0]+=","+m.group(1);
            s=s.substring(0, m.start())+s.substring(m.end()).replaceAll("\\s+", " ");
            m=bkpgMobile.matcher(s);
        }
        ret[1]=s;

        return ret;
    }

    public static String BookAndPageMobile(String s) {
        return parseBookAndPageMobile(s)[0];
    }

    private static Pattern isLot=Pattern.compile("(?i)\\bLO?TS? "),
                                 isSec=Pattern.compile("(?i)\\bSEC(?:TION) "),
                                       isPh=Pattern.compile("(?i)\\bPH(?:ASE)? ");
    public static boolean isSubdivision(String s) {
        return isLot.matcher(s).find() ||
        isSec.matcher(s).find() ||
        isPh.matcher(s).find();

    }

    private static String[] ignoreSingleMarginalKnoxvilleRO={"MAP", "PCL"};
    private static String SingleMarginalKnoxvilleRO(String s) {
        for (int i=0; i<ignoreSingleMarginalKnoxvilleRO.length; i++)
            if (s.indexOf(ignoreSingleMarginalKnoxvilleRO[i])!=-1)
                return "";
        if (s.startsWith("Bkwd "))
            s=s.substring(5);
        else if (s.startsWith("Fwd "))
            s=s.substring(4);
        s=s.replaceFirst("^[a-zA-Z]+ (.*)", "$1");
        s=s.replaceFirst("(\\d\\w+(?: \\d\\w+)?)\\D?.*", "$1");
        return s;
    }

    public static String MarginalKnoxvilleRO(String s) {
        StringTokenizer st=new StringTokenizer(s, ",");
        while (st.hasMoreTokens()) {
            String t=st.nextToken().trim();
            t=SingleMarginalKnoxvilleRO(t);
            if (t.length()>0)
                return t;
        }
        return "";
    }

    //to be used while the specs for name matcher are developed
    private static String special_needs(String s) {
        s=s.replaceFirst("\\bHOWARD D ETUX\\b", "DAVID H ETUX");
        return s;
    }

    /**
     * 
     * @param s
     * @return
     * @deprecated
     */
    public static String unifyNameDelim(String s) {
    	return unifyNameDelim(s, false);
    }
    public static String unifyNameDelim(String s, Boolean newParsing) {
    	s = unifyNameDelimWithoutRemove(s, newParsing);
        s=s.replaceAll("& *$", "");
        return s.trim();
    }
    
    /**
     * 
     * @param s
     * @return
     * @deprecated
     */
    public static String unifyNameDelimWithoutRemove(String s) {
    	return unifyNameDelimWithoutRemove(s, false);
    }
    public static String unifyNameDelimWithoutRemove(String s, boolean newParsing) {
        s=special_needs(s);
        s=s.replaceAll("\\(([^\\&]*)$", "&$1");
        s=s.replaceAll("\\(", "&");
        s=s.replaceAll("\\)", "");
        if (newParsing){
	        s=s.replaceAll("(?i) ?\\b(ET ?UXX?)\\b", " $1 &").replaceAll("(?i) & (TRU?S?(?:TEE?)?)\\b", " $1");
	        s=s.replaceAll("(?i) ?\\b(ETU)\\b", " $1 &").replaceAll("(?i) & (TRU?S?(?:TEE?)?)\\b", " $1");
	        s=s.replaceAll("(?i) ?\\b(ET ?A(LS?)?)\\b", " $1 &").replaceAll("(?i) & (TRU?S?(?:TEE?)?)\\b", " $1");
	        s=s.replaceAll("(?i) ?\\b(ET ?VIR)\\b", " $1 &").replaceAll("(?i) & (TRU?S?(?:TEE?)?)\\b", " $1");
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
    /**
     * For fomst like LAST, FIRSTMIDLE or LASTFIRSTMIDLE
     * @param s
     * @return
     */
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
        	ma=Pattern.compile("(?i)^(MAC|VAN(?:\\s+DE[N|R]?)?|D|ST|MC|DE(?: LA)?|DI|EL|DEL|AL) (\\w|-)+,").matcher(s); // MC SMITH, JULIOUS; VAN DUSSELDORP, COURTNEY; D ALESSANDRO
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
    		boolean hasType  = tempWife.matches(".+ (T(?:(?:RU?)?S?)?(?:TE)?E?S?)\\s*") ? true : false;
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
    		
    		pt = Pattern.compile("((?:(?:VAN(?:\\s+DE[N|R]?)?|D|ST|MC|DE(?: LA)?|DI|EL|DEL|AL)\\s)?[\\w'-]{2,})\\s?,\\s?\\w+ \\w"); //wife name can be as LN FN MI, where wife LN != husband LN (fix for bug #1186)
    		m = pt.matcher(wife);
    		if (m.matches()){
    			wl = m.group(1);
    			wife = wife.replace(wl, "").trim();
    		} else if(wifeDifferentLN){
    			pt = Pattern.compile("\\A((?:(?:VAN(?:\\s+DE[N|R]?)?|D|ST|MC|DE(?: LA)?|DI|EL|DEL|AL)\\s)?[\\w'-]{2,}),\\s?\\w{2,}"); // JOHNSON ABDUL & EL ZEFRI FATIMA (MOJacksonTR)
    			m = pt.matcher(wife);
    			if (m.find()){
        			wl = m.group(1);
        			wife = wife.replace(wl, "").trim();
    			} else {
    				pt = Pattern.compile("\\w{2,} \\w{1,2} ((?:(?:VAN(?:\\s+DE[N|R]?)?|D|ST|MC|DE(?: LA)?|DI|EL|DEL|AL)\\s)?[\\w'-]{2,})");
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

    //copy of parseNameDavidsonAO
    //small changes are done.
    /**
     * what to use for lastLastCompany
     */
    
    public static String[] parseNameLFMWFWM(String s, Vector<String> excludeComp, boolean lastLastCompany) {
    	return parseNameLFMWFWM(s, excludeComp, lastLastCompany, false);
    }
    public static String[] parseNameLFMWFWM(String s, Vector<String> excludeComp, boolean lastLastCompany, boolean newParsing) {
    	//BLACK,CHARLES E & DEANNA J
    	s = s.replaceFirst("(\\w),(\\w)", "$1, $2");
    	String[] names = {"", "", "", "", "", ""};
    	Pattern ll = Pattern.compile("(\\w+)\\s*&\\s*(\\w+)");
    	Matcher m = ll.matcher(s);
    	if (m.matches()){
    		 names[2] = m.group(1);
    		 names[5] = m.group(2);
    		
    	} else {
    		    String[] ret={"", "", "", "", "", ""}; // of, om, ol, wf, wm, wl
    	        if (s==null)
    	            return ret;
    	        s=s.replaceAll("\\(.+\\)", "");
    	        
    			if (s
    				.trim()
    				.matches(".*\\w\\s*\\&\\s*\\w\\s*\\w+.*") 
    					&& NameUtils
    						.isCompany(s.trim(), excludeComp, lastLastCompany)) 
    				ret[2]=s;
    			else {
    		        if (s.matches("\\w+,(?:\\s*\\w+)*\\s*/.*")) 
    		            return parseNameWilliamson2(s, newParsing);
    		        s=s.replaceAll("\\s+", " ");
    		        s=unifyNameDelim(s, newParsing);
    		        Matcher ma=Pattern.compile("&|/ ?").matcher(s);
    		        String husband, wife="";
    		        if (ma.find()) {
    		            husband=s.substring(0, ma.start()).trim();
    		            wife=s.substring(ma.end()).trim();
    		        } else {
    		            husband=s.trim();
    		        }
    		
    		        if (NameUtils.isCompany(husband, excludeComp, lastLastCompany)) {
    		            ret[2]=husband;
    		        } else {
    		            for (int i=0; i<lastNames.length; i++) {
    		                husband=husband.replaceFirst("^(?i)(\\w+) (\\b"+lastNames[i]+"\\b)(.*)", "$2 $1$3");
    		            }
    		            //husband = husband.replaceFirst(" JR\\.*", "");
    		            husband=husband.replaceFirst("^(?i)(\\w+) ((?:\\w\\w?|iii)(?:\\s+(?:\\w\\w?|iii))*) (\\w{3,}\\b)(.*)", "$1 $3 $2$4");
    		            int i=husband.indexOf(" "), j;
    		            if (i==-1) {
    		                ret[2]=husband;
    		            } else {
    		                ret[2]=husband.substring(0, i).replaceFirst(",\\s*$", "").trim(); // fix for bug #2944
    		                husband=husband.substring(i+1).replaceAll("\\.", " ").replaceAll(",", " ").trim();
    		                j=husband.indexOf(" ");
    		                if (j!=-1) {
    		                    if (j==1 && husband.matches(".*\\w{3,}.*")) {
    		                        ret[1]=husband.substring(0, j).trim();
    			                    ret[0]=husband.substring(j+1).trim();
    		                    }else {
    		                        ret[0]=husband.substring(0, j).trim();
    			                    ret[1]=husband.substring(j+1).trim();
    		                    }
    		                    	                
    		                } else {
    		                    ret[0]=husband;
    		                }
    		            }
    		        }
    		
    		        if (NameUtils.isCompany(wife, excludeComp, lastLastCompany)) {
    		            ret[5]=wife;
    		        } else {
    		        	int l = wife.length();
    		        	boolean lastRemoved = false;
    		            if (wife.length()>0){
    		            	String patt = ret[2].replaceAll("\\+", "\\\\+");
    		                wife=wife.replaceAll("\\b"+patt+"(\\b|,)", "").trim();
    		            }
    		            if (l != wife.length()){
    		            	lastRemoved = true;
    		            }
    		            wife=wife.replaceAll("\\.", " ").replaceAll(",", " ").replaceAll("\\s{2,}", " ").trim();
    		            
    		            /* ma=Pattern.compile("^\\w+ ").matcher(wife);
    		            if (ma.find()) { // another last name
    		                int i=wife.indexOf(" ");
    		                ret[5]=wife.substring(0, i).trim();
    		                wife=wife.substring(i+1).trim();
    		            } else {
    		                ret[5]=ret[2];
    		            }	            

    		            int j=wife.indexOf(" ");
    		            if (j!=-1) {
    		                ret[3]=wife.substring(0, j).trim();
    		                ret[4]=wife.substring(j+1).trim();
    		            } else {
    		                ret[3]=wife;
    		            }*/
    		            if (wife.indexOf('&') != -1) {
    		            	wife = wife.replaceFirst("\\s*&.*", "");
    		            }
    		            String wifeFull[] = wife.split(" |,"); 
    	                if (wife.length() > 0)
    	                {
    	                	String[] wifeFull2 = wifeFull;
    	                	//SMITH ANITA T & ANTHONY S F
    	                	Vector<Integer> namesLen1 = new Vector<Integer>();
    	                	Vector<Integer> namesLenmore1 = new Vector<Integer>();
    	                	if (!wife.matches("\\w{1} \\w{1} \\w{2,}")){
	    	                	for(int ij=0; ij<wifeFull.length; ij++){
	    	                		if (wifeFull[ij].length() == 1 
	    	                				|| GenericFunctions.nameSuffix.matcher(wifeFull[ij]).matches()
	    	                				|| GenericFunctions.nameType.matcher(wifeFull[ij]).matches()
	    	                				|| GenericFunctions.nameOtherType.matcher(wifeFull[ij]).matches()){
	    	                			namesLen1.add(ij);
	    	                		} else {
	    	                			namesLenmore1.add(ij);
	    	                		}
	    	                	}
	    	                	if (namesLen1.size() == 2 && namesLenmore1.size() == 1){
	    	                		wifeFull = new String[2];
	    	                		wifeFull[0] = wifeFull2[namesLenmore1.get(0)];
	    	                		wifeFull[1] = wifeFull2[namesLen1.get(0)] + " " + wifeFull2[namesLen1.get(1)];
	    	                	}
    	                	}
    	    	        int len = wifeFull.length;                	
    	                for(int i=1; i<len; i++) {
    	                	Matcher ma1 = GenericFunctions.nameSuffix3.matcher(wifeFull[i]);
    		                if(ma1.matches()){
    		                	wifeFull[i-1] = wifeFull[i-1]+ " " +ma1.group(0);
    		                	for(int j=i; j<len-1; j++) 
    		                		wifeFull[j] = wifeFull[j+1]; 
    		                	len--;
    		                	break;
    		                }	
    	                }
    	                for(int i=1; i<len; i++) {
	    	                Matcher ma1 = GenericFunctions.nameType.matcher(wifeFull[i]);
	    		            if(ma1.matches()){
	    		            	if (wifeFull[i].length() > 1 && wifeFull[i].indexOf(" ") > 0 && wifeFull[i].substring(0, wifeFull[i].indexOf(" ")).length() > 1){
	    		                	wifeFull[i-1] = wifeFull[i-1]+ " " +ma1.group(0);
	    		                	for(int j=i; j<len-1; j++) 
	    		                		wifeFull[j] = wifeFull[j+1]; 
	    		                	len--;
	    		                	break;
	    		                }	
    	                	}
    	                }
    		            switch(len){
    		            	case 0: 
    		            	    break;
    		            	case 1:
    		            	    ret[3] = wife;
    		            	    ret[5] = ret[2];                      
    		            	    break;
    		            	case 2:
    		            	    ret[3] = wifeFull[0];
    		            	    ret[4] = wifeFull[1];
    		            	    ret[5] = ret[2];
    		            	    break;
    		            	case 3:
    		            	    ret[3] = wifeFull[0];
    		            	    ret[4] = wifeFull[1];
    		            	    ret[5] = wifeFull[2];
    		            	    if (lastRemoved && !ret[2].equals(ret[5])){
    		            	    	ret[4] += " " + ret[5];
    		            	    	ret[5] = ret[2];
    		            	    }
    		            	    break;
    		            	default:
    		            	    ret[5] = wife;
    		            	    System.out.println("Invalid spouse name: " + wife);
    		            		break;
    		            }
    	                }
    		        }
    			}
    		names = ret;
    	}
    	return names;
    }

    public static String[] parseNameFML(String s, Vector<String> excludeCompany, boolean lastLastCompany ) {
    	return parseNameFML(s, excludeCompany, lastLastCompany, false);
    }
    public static String[] parseNameFML(String s, Vector<String> excludeCompany, boolean lastLastCompany, boolean newParsing ) { // FML
        String[] ret={"", "", "", "", "", ""}; // of, om, ol, wf, wm, wl
        if (s==null)
            return ret;
        s=s.replaceAll("\\s+", " ");
        s=unifyNameDelim(s, newParsing);
        s = s.replaceAll("\\bD/?B/?A\\b", "&"); // fix for bug #2746
        Matcher ma=Pattern.compile("& ?").matcher(s);
        String husband, wife="";
        if (ma.find()) {
            husband=s.substring(0, ma.start()).trim();
            wife=s.substring(ma.end()).trim();
        } else {
            husband=s.trim();
        }

        if (husband.length() == 0 && wife.length() != 0){
        	husband = wife;
        	wife = "";
        }
        if (NameUtils.isCompany(husband, excludeCompany, lastLastCompany)) {
            ret[2]=husband;
        } else {
            int i=husband.lastIndexOf(" "), j;
            if (i==-1) {
                ret[2]=husband;
            } else {
                String lastItem=husband.substring(i+1).trim();
                if (isInMiddleName(lastItem)) {
                    husband=husband.replaceFirst("^(.+?) ([^ ]+) ([^ ]+)$", "$1 $3 $2");
                    i=husband.lastIndexOf(" ");
                	lastItem=husband.substring(i+1).trim();
                	if (isTypeAndMustBeInMiddleName(lastItem)) {//N D SMITH ETAL JR
                		husband=husband.replaceFirst("^(.+?) ([^ ]+) ([^ ]+) ([^ ]+)$", "$1 $4 $3 $2");
                        i=husband.lastIndexOf(" ");
                	}
                } else if (isTypeAndMustBeInMiddleName(lastItem)){
                	husband=husband.replaceFirst("^(.+?) ([^ ]+) ([^ ]+)$", "$1 $3 $2");
                    i=husband.lastIndexOf(" ");
                	lastItem=husband.substring(i+1).trim();
                	if (isInMiddleName(lastItem)) {//N D SMITH JR ETAL
                		husband=husband.replaceFirst("^(.+?) ([^ ]+) ([^ ]+) ([^ ]+)$", "$1 $3 $4 $2");
                        i=husband.lastIndexOf(" ");
                	}
                }
                Pattern compoundLNPat = Pattern.compile(".+ (MC|DI) .+$"); 	// JONATHAN E MC NEIL
                Matcher compoundLNMat = compoundLNPat.matcher(husband);
                if (compoundLNMat.find()){
                	i = compoundLNMat.start(1)-1;
                }

                ret[2]=husband.substring(i+1).trim();
                husband=husband.substring(0, i).replaceAll("\\.", " ").replaceAll(",", " ").trim();
                j=husband.indexOf(" ");
                if (j!=-1) {
                    ret[0]=husband.substring(0, j).trim();
                    ret[1]=husband.substring(j+1).trim();
                } else {
                    ret[0]=husband;
                }
            }
        }

        if (NameUtils.isCompany(wife, excludeCompany, lastLastCompany)) {
            ret[5]=wife;
        } else {
            if (!NameUtils.isCompany(husband, excludeCompany, lastLastCompany)){ 
            	String patt = ret[2].replaceAll("\\+", "\\\\+");
                wife=wife.replaceAll("\\b"+patt+",?", "").trim();
            }
            if (wife.length()>0)
                ret[5]=ret[2];

            wife=wife.replaceAll("\\.", " ").replaceAll(",", " ").replaceAll(" &", "").trim();

            int j=wife.indexOf(" ");
            if (j!=-1) {
                ret[3]=wife.substring(0, j).trim();
                ret[4]=wife.substring(j+1).trim();
            } else {
                ret[3]=wife;
            }
        }
      
        return ret;
    }    
    public static String[] parseNameFMWFWML(String name, Vector<String> excludeCompany){
    	return parseNameFMWFWML(name, excludeCompany, false);
    }
    public static String[] parseNameFMWFWML(String name, Vector<String> excludeCompany, boolean newParsing){
    	String[] ret = {"", "", "", "", "", ""};
    	boolean isTrust = false;
    	boolean isLFM = name.contains(";");
    	
    	name = NameILCookLA(name);

    	String s = name.replaceAll("(?i)\\bTR(\\.?\\s?#)?\\s*\\d+", "");
    	isTrust = !name.equals(s);
    	s = s.replaceAll("\\b\\d+\\b", " ");
    	s = s.replaceAll("\\b\\d+\\w+\\s*$", " ");
    	s = s.replaceAll("\\s{2,}", " ").trim();
    	
    	String coowner = "";
    	Pattern p;
//		Matcher ma = p.matcher(s);
//    	if (ma.matches()){
//    		isLFM = false;
//    		s =  ma.group(1);
//    		coowner = ma.group(2); 
//    	} else {   	    	
	    	// change F & WF L names (as JOSE & DIANA BAEZ) to L F & WF, because FML parser does not parse it correctly   
	    	Matcher ma = GenericFunctions.FWFL.matcher(s);
	    	if (ma.find()){
	    		isLFM = true;
	    		s =  ma.group(7) + " " + ma.group(1);
	    		s = s.trim();
	    	} else {    	
		    	p = Pattern.compile("[A-Z'-]{2,} [A-Z]{2,} [A-Z]");
		    	ma = p.matcher(s);
		    	if (ma.matches()){
		    		isLFM = true; 
		    	} 
	    	}
//    	}
    	
    	if (isTrust){    		
    		ret[2] = s;
    	} else {
	    	p = Pattern.compile("([A-Z'-]{2,}) (JR)");
	    	ma = p.matcher(s);
	    	if (ma.matches()){
	    		ret[2] = ma.group(1);
	    		ret[0] = ma.group(2);
	    	} else if (isLFM || s.contains(",") || s.contains("&")){	// apply L F M tokenizer
	    		ret = StringFormats.parseNameLFMWFWM(s, excludeCompany, true, newParsing);
	    	} else {									// apply F M L tokenzier    	
	    		ret = StringFormats.parseNameDesotoRO(s, newParsing);
	    		if (coowner.length() != 0){
	    			String[] names = StringFormats.parseNameDesotoRO(coowner, newParsing);
	    			ret[3] = names[0];
	    			ret[4] = names[1];
	    			ret[5] = names[2];
	    		}
	    	}
    	}
    	return ret;
    }    
    
    public static String[] parseNameDavidsonAO(String s){
    	return parseNameDavidsonAO(s, false);
    }
    
    public static String[] parseNameDavidsonAO(String s, boolean newParsing) { // L,FM
    	String[] ret={"", "", "", "", "", ""}; // of, om, ol, wf, wm, wl
        if (s==null)
            return ret;
        s=s.replaceAll("\\(.+\\)", "");
        
		if (s
			.trim()
			.matches(".*\\w\\s*\\&\\s*\\w\\s*\\w+.*") 
				&& NameUtils
					.isCompany(s.trim())) 
			ret[2]=s;
		else {
	        if (s.matches("\\w+,(?:\\s*\\w+)*\\s*/.*")) 
	            return parseNameWilliamson2(s, newParsing);
	        s=s.replaceAll("\\s+", " ");
	        s=unifyNameDelim(s, newParsing);
	        Matcher ma=Pattern.compile("&|/ ?").matcher(s);
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
	                husband=husband.replaceFirst("^(?i)(\\w+) (\\b"+lastNames[i]+"\\b)(.*)", "$2 $1$3");
	            }
	            husband = husband.replaceFirst(" JR\\.*", "");
	            husband=husband.replaceFirst("^(?i)(\\w+) ((?:\\w\\w?|iii)(?:\\s+(?:\\w\\w?|iii))*) (\\w{3,}\\b)(.*)", "$1 $3 $2$4");
	            int i=husband.indexOf(" "), j;
	            if (i==-1) {
	                ret[2]=husband;
	            } else {
	                ret[2]=husband.substring(0, i).replaceFirst(",\\s*$", "").trim(); // fix for bug #2944
	                husband=husband.substring(i+1).replaceAll("\\.", " ").replaceAll(",", " ").trim();
	                j=husband.indexOf(" ");
	                if (j!=-1) {
	                    if (j==1) {
	                        ret[1]=husband.substring(0, j).trim();
		                    ret[0]=husband.substring(j+1).trim();
	                    }else {
	                        ret[0]=husband.substring(0, j).trim();
		                    ret[1]=husband.substring(j+1).trim();
	                    }
	                    	                
	                } else {
	                    ret[0]=husband;
	                }
	            }
	        }
	
	        if (NameUtils.isCompany(wife)) {
	            ret[5]=wife;
	        } else {
	            if (wife.length()>0){
	            	String patt = ret[2].replaceAll("\\+", "\\\\+");
	                wife=wife.replaceAll("\\b"+patt+",?", "").trim();
	            }
	            wife=wife.replaceAll("\\.", " ").replaceAll(",", " ").trim();
	            
	            /* ma=Pattern.compile("^\\w+ ").matcher(wife);
	            if (ma.find()) { // another last name
	                int i=wife.indexOf(" ");
	                ret[5]=wife.substring(0, i).trim();
	                wife=wife.substring(i+1).trim();
	            } else {
	                ret[5]=ret[2];
	            }	            

	            int j=wife.indexOf(" ");
	            if (j!=-1) {
	                ret[3]=wife.substring(0, j).trim();
	                ret[4]=wife.substring(j+1).trim();
	            } else {
	                ret[3]=wife;
	            }*/
	            if (wife.indexOf('&') != -1) {
	            	wife = wife.replaceFirst("\\s*&.*", "");
	            }
	            String wifeFull[] = wife.split(" |,"); 
                if (wife.length() > 0)
                {
    	        int len = wifeFull.length;                	
                for(int i=1; i<len; i++) {
	                if("JR".equals(wifeFull[i])){
	                	wifeFull[i-1] = wifeFull[i-1]+" JR";
	                	for(int j=i; j<len-1; j++) 
	                		wifeFull[j] = wifeFull[j+1]; 
	                	len--;
	                	break;
	                }	
                }
	            switch(len){
	            	case 0: 
	            	    break;
	            	case 1:
	            	    ret[3] = wife;
	            	    ret[5] = ret[2];                      
	            	    break;
	            	case 2:
	            	    ret[3] = wifeFull[0];
	            	    ret[4] = wifeFull[1];
	            	    ret[5] = ret[2];
	            	    break;
	            	case 3:
	            	    ret[3] = wifeFull[1];
	            	    ret[4] = wifeFull[2];
	            	    ret[5] = wifeFull[0];
	            	    break;
	            	default:
	            	    ret[5] = wife;
	            	    System.out.println("Invalid spouse name: Davidson");
	            		break;
	            }
                }
	        }
		}
		
        return ret;
    }
    
    /**
     * This is better because it keeps the JR suffix ;)
     * 
     **/
    public static String[] parseNameLikeDavidsonAOOnlyBetter(String s) {
    	return parseNameLikeDavidsonAOOnlyBetter(s, false);
    }
    public static String[] parseNameLikeDavidsonAOOnlyBetter(String s, boolean newParsing) { // L,FM
        String[] ret={"", "", "", "", "", ""}; // of, om, ol, wf, wm, wl
        if (s==null)
            return ret;
        s=s.replaceAll("\\(.+\\)", "");
        
		if (s
			.trim()
			.matches(".*\\w\\s*\\&\\s*\\w\\s*\\w+.*") 
				&& NameUtils
					.isCompany(s.trim())) 
			ret[2]=s;
		else {
	        if (s.matches("\\w+,(?:\\s*\\w+)*\\s*/.*")) 
	            return parseNameWilliamson2(s, newParsing);
	        s=s.replaceAll("\\s+", " ");
	        s=unifyNameDelim(s, newParsing);
	        Matcher ma=Pattern.compile("&|/ ?").matcher(s);
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
	                husband=husband.replaceFirst("^(?i)(\\w+) (\\b"+lastNames[i]+"\\b)(.*)", "$2 $1$3");
	            }
	            //husband = husband.replaceFirst(" JR\\.*", "");
	            husband=husband.replaceFirst("^(?i)(\\w+) ((?:\\w\\w?|iii)(?:\\s+(?:\\w\\w?|iii))*) (\\w{3,}\\b)(.*)", "$1 $3 $2$4");
	            int i=husband.indexOf(" "), j;
	            if (i==-1) {
	                ret[2]=husband;
	            } else {
	                ret[2]=husband.substring(0, i).replaceFirst(",\\s*$", "").trim(); // fix for bug #2944
	                husband=husband.substring(i+1).replaceAll("\\.", " ").replaceAll(",", " ").trim();
	                j=husband.indexOf(" ");
	                if (j!=-1) {
	                    if (j==1) {
	                        ret[1]=husband.substring(0, j).trim();
		                    ret[0]=husband.substring(j+1).trim();
	                    }else {
	                        ret[0]=husband.substring(0, j).trim();
		                    ret[1]=husband.substring(j+1).trim();
	                    }
	                    	                
	                } else {
	                    ret[0]=husband;
	                }
	            }
	        }
	
	        if (NameUtils.isCompany(wife)) {
	            ret[5]=wife;
	        } else {
	            if (wife.length()>0){
	            	String patt = ret[2].replaceAll("\\+", "\\\\+");
	                wife=wife.replaceAll("\\b"+patt+",?", "").trim();
	            }
	            wife=wife.replaceAll("\\.", " ").replaceAll(",", " ").trim();
	            
	            /* ma=Pattern.compile("^\\w+ ").matcher(wife);
	            if (ma.find()) { // another last name
	                int i=wife.indexOf(" ");
	                ret[5]=wife.substring(0, i).trim();
	                wife=wife.substring(i+1).trim();
	            } else {
	                ret[5]=ret[2];
	            }	            

	            int j=wife.indexOf(" ");
	            if (j!=-1) {
	                ret[3]=wife.substring(0, j).trim();
	                ret[4]=wife.substring(j+1).trim();
	            } else {
	                ret[3]=wife;
	            }*/
	            if (wife.indexOf('&') != -1) {
	            	wife = wife.replaceFirst("\\s*&.*", "");
	            }
	            String wifeFull[] = wife.split(" |,"); 
                if (wife.length() > 0)
                {
    	        int len = wifeFull.length;                	
                for(int i=1; i<len; i++) {
	                if("JR".equals(wifeFull[i])){
	                	wifeFull[i-1] = wifeFull[i-1]+" JR";
	                	for(int j=i; j<len-1; j++) 
	                		wifeFull[j] = wifeFull[j+1]; 
	                	len--;
	                	break;
	                }	
                }
	            switch(len){
	            	case 0: 
	            	    break;
	            	case 1:
	            	    ret[3] = wife;
	            	    ret[5] = ret[2];                      
	            	    break;
	            	case 2:
	            	    ret[3] = wifeFull[0];
	            	    ret[4] = wifeFull[1];
	            	    ret[5] = ret[2];
	            	    break;
	            	case 3:
	            	    ret[3] = wifeFull[1];
	            	    ret[4] = wifeFull[2];
	            	    ret[5] = wifeFull[0];
	            	    break;
	            	default:
	            	    ret[5] = wife;
	            	    System.out.println("Invalid spouse name: Davidson");
	            		break;
	            }
                }
	        }
		}
		
        return ret;
    }

    public static String OwnerFirstNameNashville(String s) {
        return parseNameNashville(s)[0];
    }

    public static String OwnerMiddleNameNashville(String s) {
        return parseNameNashville(s)[1];
    }

    public static String OwnerLastNameNashville(String s) {
        return parseNameNashville(s)[2];
    }
    
    public static String OwnerSuffix(String s) {
    	String suffixes[] = GenericFunctions1.extractSuffix(s);
    	return  suffixes[1];
    }
    
    public static String OwnerType(String s) {
    	String type[] = GenericFunctions1.extractType(s);
    	return  type[1];
    }
    
    public static String OwnerOtherType(String s) {
    	String otherType[] = GenericFunctions1.extractOtherType(s);
    	return  otherType[1];
    }
    
    public static String SpouseFirstNameNashville(String s) {
        return parseNameNashville(s)[3];
    }

    public static String SpouseMiddleNameNashville(String s) {
        return parseNameNashville(s)[4];
    }

    public static String SpouseLastNameNashville(String s) {
        return parseNameNashville(s)[5];
    }

    public static ResultTable reParseGrantorGranteeSet(ResultTable rt) {
    	if(rt != null) {
	    	for (int i = 0; i < rt.getLength(); i++){
	    		for (int j = 0; j < rt.getHead().length; j++){
	    			if ("".equals(rt.body[i][1])&& "".equals(rt.body[i][2]) && NameUtils.isCompany(rt.body[i][3])) {
	    				rt.body[i][0] = rt.body[i][0].replaceAll("\\bTR(U?(STEE))?\\b", "");
	    				rt.body[i][3] = rt.body[i][3].replaceAll("\\bTR(U?(STEE))?\\b", "");
	    				if(rt.body[i].length>1){
	    					rt.body[i][1] = OwnerFirstNameNashville(rt.body[i][3]);
	    				}
	    				if(rt.body[i].length>2){
	    					rt.body[i][2] = OwnerMiddleNameNashville(rt.body[i][3]);
	    				}
	    				if(rt.body[i].length>3){
	    					rt.body[i][3] = OwnerLastNameNashville(rt.body[i][3]);
	    				}
	    				if(rt.body[i].length>4){
	    					rt.body[i][4] = SpouseFirstNameNashville(rt.body[i][3]);
	    				}
	    				if(rt.body[i].length>5){
	    					rt.body[i][5] = SpouseMiddleNameNashville(rt.body[i][3]);
	    				}
	    				if(rt.body[i].length>6){
	    					rt.body[i][6] = SpouseLastNameNashville(rt.body[i][3]);
	    				}
	    			}
	    		}
	    	}
    	}
    	return rt;
    }
    
    public static ResultTable parseGrantorGranteeSetFromString(String names, String splitString) throws Exception {
    	
    	ResultTable rt = new ResultTable();
    	
    	String[] header = {"all", "OwnerFirstName", "OwnerMiddleName", "OwnerLastName", "SpouseFirstName", "SpouseMiddleName", "SpouseLastName"};
    	Map<String,String[]> map = new HashMap<String,String[]>();
    	map.put("all", new String[]{"all", ""});
		map.put("OwnerFirstName", new String[]{"OwnerFirstName", ""});
		map.put("OwnerMiddleName", new String[]{"OwnerMiddleName", ""});
		map.put("OwnerLastName", new String[]{"OwnerLastName", ""});
 		map.put("SpouseFirstName", new String[]{"SpouseFirstName", ""});
 		map.put("SpouseMiddleName", new String[]{"SpouseMiddleName", ""});
 		map.put("SpouseLastName", new String[]{"SpouseLastName", ""});
    	
 		String[] name = names.split(splitString);// White Dana / Brigdon D Christopher / Nai Ohio Equities Realtors
    	String[][] body = new String[name.length][header.length];
    	
    	for (int i = 0; i < name.length; i++){
    				body[i][0] = name[i];
    				body[i][1] = OwnerFirstNameNashville(name[i]);
    				body[i][2] = OwnerMiddleNameNashville(name[i]);
    				body[i][3] = OwnerLastNameNashville(name[i]);
    				body[i][4] = SpouseFirstNameNashville(name[i]);
    				body[i][5] = SpouseMiddleNameNashville(name[i]);
    				body[i][6] = SpouseLastNameNashville(name[i]);

    	}
    	rt.setReadWrite();
    	rt.setHead(header);
    	rt.setMap(map);
    	rt.setBody(body);
    	rt.setReadOnly();
    	return rt;
    }
    
    public static String[] lastNames={"Smith", "VAN_HOOK"};
    /**
     *	Pay attention to ETVIR, ETUX and ETAL constructions. 
     *  EtUx" is a Latin phrase meaning "and wife." 
     *  The phrase "EtVir" means "and husband", "EtAl" means "and others."
     *  This function deals only with the wife case (checks only for ETUX).
     * */ 
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
	            ma = Pattern.compile(NAME_WITH_PREPOSITION_PATTERN).matcher(husband);//D ALESSANDRO, Al Jafari
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
        		boolean hasType  = tempWife.matches(".+ (T(?:(?:RU?)?S?)?(?:TE)?E?S?)\\s*") ? true : false;
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
        		
        		pt = Pattern.compile("((?:(?:VAN(?:\\s+DE[N|R]?)?|D|ST|MC|DE(?: LA)?|DI|EL|DEL|AL)\\s)?[\\w'-]{2,}) \\w{2,} \\w"); //wife name can be as LN FN MI, where wife LN != husband LN (fix for bug #1186)
        		m = pt.matcher(wife);
        		if (m.matches()){
        			wl = m.group(1);
        			wife = wife.replace(wl, "").trim();
        		} else if(wifeDifferentLN){
        			pt = Pattern.compile("\\A((?:(?:VAN(?:\\s+DE[N|R]?)?|D|ST|MC|DE(?: LA)?|DI|EL|DEL|AL)\\s)?[\\w'-]{2,}) \\w{3,}"); // JOHNSON ABDUL & EL ZEFRI FATIMA (MOJacksonTR)
        			m = pt.matcher(wife);
        			if (m.find()){
            			wl = m.group(1);
            			wife = wife.replace(wl, "").trim();
        			} else {
        				pt = Pattern.compile("\\w{2,} \\w{1,2} ((?:(?:VAN(?:\\s+DE[N|R]?)?|D|ST|MC|DE(?: LA)?|DI|EL|DEL|AL)\\s)?[\\w'-]{2,})");
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
	            String cleanMiddle = ret[4];
        		cleanMiddle = cleanMiddle.replaceAll("(JR|SR|II|III|IV)\\s*", "");
        		cleanMiddle = cleanMiddle.replaceAll("(T(?:(?:RU?)?S?)?(?:TE)?E?S?)\\s*", "");
        		cleanMiddle = cleanMiddle.replaceAll("(ET[\\s,;]*UX|ET[\\s,;]*AL|ET[\\s,;]*VIR)\\s*", "");
        		cleanMiddle = cleanMiddle.trim();
	            if (ret[3].length() == 1 && cleanMiddle.length() > 1){ // interchange spouse middle with spouse first if first is just one letter (part of the bug #3042 fix)
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

    public static String[] parseNameWilliamson2(String s) {
    	return parseNameWilliamson2(s, false);
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

    public static String cleanNameMERS(String s){
    	if (s != null){
    		s = s.replaceAll("(?is)\\b(Mortgage Ele?\\s?c?r?(t?r?(on)?ic)? Re\\s?g(is)?(tra)?(ti)?(on)?s?\\s?(Sys)?(tem)?s?|M\\s*\\.?,?\\s*E\\s*\\.?\\s*R\\s*\\.?\\s*S\\s*\\.?-?(\\s+MORTGAGE)?)(\\s*INC)?\\b", "");
    	}
    	return s;
    }
    
    public static String OwnerFirstNameWilliamson(String s) {
        return parseNameNashville(s)[0];
    }

    public static String OwnerMiddleNameWilliamson(String s) {
        return parseNameNashville(s)[1];
    }

    public static String OwnerLastNameWilliamson(String s) {
        return parseNameNashville(s)[2];
    }

    public static String SpouseFirstNameWilliamson(String s) {
        return parseNameNashville(s)[3];
    }

    public static String SpouseMiddleNameWilliamson(String s) {
        return parseNameNashville(s)[4];
    }

    public static String SpouseLastNameWilliamson(String s) {
        return parseNameNashville(s)[5];
    }

    /*The following functions are used for tokenizing names parsed from Trustee Appointments docs from DailyNews.
     These documents have names in format FML, but also in format LFM, in the last case the name has "S Tr" or "K Tr" suffix */
    public static String OwnerFirstNameTrAppShelbyDN(String s) {
    	if (s.matches("(?i).*\\b(S|K)? Tr\\b.*")){
    		return OwnerFirstNameWilliamson(s.replaceFirst("\\b(S|K)? Tr\\b", ""));
    	} else {
    		return OwnerFirstNameDesotoRO(s);
    	}
    }

    public static String OwnerMiddleNameTrAppShelbyDN(String s) {
    	if (s.matches("(?i).*\\b(S|K)? Tr\\b.*")){
            return OwnerMiddleNameWilliamson(s.replaceFirst("\\b(S|K)? Tr\\b", ""));
    	} else {
            return OwnerMiddleNameDesotoRO(s);
    	}
    }

    public static String OwnerLastNameTrAppShelbyDN(String s) {
    	if (s.matches("(?i).*\\b(S|K)? Tr\\b.*")){
            return OwnerLastNameWilliamson(s.replaceFirst("\\b(S|K)? Tr\\b", ""));
    	} else {
            return OwnerLastNameDesotoRO(s);
    	}        
    }

    public static String SpouseFirstNameTrAppShelbyDN(String s) {
    	if (s.matches("(?i).*\\b(S|K)? Tr\\b.*")){
            return SpouseFirstNameWilliamson(s.replaceFirst("\\b(S|K)? Tr\\b", ""));
    	} else {
            return SpouseFirstNameDesotoRO(s);
    	}
    }

    public static String SpouseMiddleNameTrAppShelbyDN(String s) {
    	if (s.matches("(?i).*\\b(S|K)? Tr\\b.*")){
            return SpouseMiddleNameWilliamson(s.replaceFirst("\\b(S|K)? Tr\\b", ""));
    	} else {
            return SpouseMiddleNameDesotoRO(s);
    	}
    }

    public static String SpouseLastNameTrAppShelbyDN(String s) {
    	if (s.matches("(?i).*\\b(S|K)? Tr\\b.*")){
            return SpouseLastNameWilliamson(s.replaceFirst("\\b(S|K)? Tr\\b", ""));
    	} else {
            return SpouseLastNameDesotoRO(s);
    	}
    }
    
    public static String[] parseNameDesotoRO(String s) {
    	return parseNameDesotoRO(s, false);
    }
    public static String[] parseNameDesotoRO(String s, boolean newParsing) { // FML
        String[] ret={"", "", "", "", "", ""}; // of, om, ol, wf, wm, wl
        if (s==null)
            return ret;
        s=s.replaceAll("\\s+", " ");
        s = s.replaceAll("\\b(ET)\\s+(UX|AL|VIR)\\b", "$1$2");
        
        s=unifyNameDelim(s, newParsing);
        s = s.replaceAll("\\bD/?B/?A\\b", "&"); // fix for bug #2746
        Matcher ma=Pattern.compile("& ?").matcher(s);
        String husband, wife="";
        if (ma.find()) {
            husband=s.substring(0, ma.start()).trim();
            wife=s.substring(ma.end()).trim();
        } else {
            husband=s.trim();
        }

        if (husband.length() == 0 && wife.length() != 0){
        	husband = wife;
        	wife = "";
        }
        if (NameUtils.isCompany(husband)) {
            ret[2]=husband;
        } else {
            int i=husband.lastIndexOf(" "), j;
            if (i==-1) {
                ret[2]=husband;
            } else {
                String lastItem=husband.substring(i+1).trim();
                if (isInMiddleName(lastItem)) {
                    husband=husband.replaceFirst("^(.+?) ([^ ]+) ([^ ]+)$", "$1 $3 $2");
                    i=husband.lastIndexOf(" ");
                	lastItem=husband.substring(i+1).trim();
                	if (isTypeAndMustBeInMiddleName(lastItem)) {//N D SMITH ETAL JR
                		husband=husband.replaceFirst("^(.+?) ([^ ]+) ([^ ]+) ([^ ]+)$", "$1 $4 $3 $2");
                        i=husband.lastIndexOf(" ");
                	}
                } else if (isTypeAndMustBeInMiddleName(lastItem)){
                	husband=husband.replaceFirst("^(.+?) ([^ ]+) ([^ ]+)$", "$1 $3 $2");
                    i=husband.lastIndexOf(" ");
                	lastItem=husband.substring(i+1).trim();
                	if (isInMiddleName(lastItem)) {//N D SMITH JR ETAL
                		husband=husband.replaceFirst("^(.+?) ([^ ]+) ([^ ]+) ([^ ]+)$", "$1 $3 $4 $2");
                        i=husband.lastIndexOf(" ");
                	}
                }
                Pattern compoundLNPat = Pattern.compile(".+ (MAC|VAN(?:\\s+DE[N|R]?)?|ST|MC|DE(?: LA)?|DI|EL|DELA?|AL) .+$"); 	// JONATHAN E MC NEIL, GEORGE VAN DORN; D ALESSANDRO
                Matcher compoundLNMat = compoundLNPat.matcher(husband);
                if (compoundLNMat.find()){
                	i = compoundLNMat.start(1)-1;
                } else {
                	compoundLNPat = Pattern.compile(".+ (MAC|VAN(?:\\s+DE[N|R]?)?|ST|MC|D|DE(?: LA)?|DI|EL|DELA?|AL) ALESSANDRO$"); 	// JONATHAN E MC NEIL, GEORGE VAN DORN; D ALESSANDRO
                	compoundLNMat = compoundLNPat.matcher(husband);
                    if (compoundLNMat.find()){
                    	i = compoundLNMat.start(1)-1;
                    }
                }

                ret[2]=husband.substring(i+1).trim();
                husband=husband.substring(0, i).replaceAll("\\.", " ").replaceAll(",", " ").trim();
                j=husband.indexOf(" ");
                if (j!=-1) {
                    ret[0]=husband.substring(0, j).trim();
                    ret[1]=husband.substring(j+1).trim();
                } else {
                    ret[0]=husband;
                }
            }
        }

        if (NameUtils.isCompany(wife)) {
            ret[5]=wife;
        } else {
            if (!NameUtils.isCompany(husband)){ 
            	String patt = ret[2].replaceAll("\\+", "\\\\+");
                wife=wife.replaceAll("\\b"+patt+",?", "").trim();
            }
            if (wife.length()>0)
                ret[5]=ret[2];

            wife=wife.replaceAll("\\.", " ").replaceAll(",", " ").replaceAll(" &", "").trim();

            int j=wife.indexOf(" ");
            if (j!=-1) {
                ret[3]=wife.substring(0, j).trim();
                ret[4]=wife.substring(j+1).trim();
            } else {
                ret[3]=wife;
            }
        }
      
        return ret;
    }

        
        /*if (NameUtils.isCompany(s)) {
            ret[2]=s;
        }else {
            Matcher ma=Pattern.compile("& ?").matcher(s);
            String husband, wife="";
            if (ma.find()) {
                husband=s.substring(0, ma.start()).trim();
                wife=s.substring(ma.end()).trim();
            } else {
                husband=s.trim();
            }
            int i=husband.lastIndexOf(" "), j;
            if (i==-1) {
                ret[2]=husband;
            } else {
                String lastItem=husband.substring(i+1).trim();
                //if (isInMiddleName(lastItem)) {
                    //husband=husband.replaceFirst("^(.+?) ([^ ]+) ([^ ]+)$", "$1 $3 $2");
                    int istart,iend;
                    istart = husband.indexOf(" ");
                    iend  = husband.indexOf(" ",istart+1);
                    String tmp = husband.substring(istart,iend);
                    husband = husband.replaceAll(tmp,"");
                    husband = husband+ " "+tmp;
                    i=husband.lastIndexOf(" ");
                //}

               
                ret[0]=husband.substring(i+1).trim();
                husband=husband.substring(0, i).replaceAll("\\.", " ").replaceAll(",", " ").trim();
                j=husband.indexOf(" ");
                if (j!=-1) {
                    ret[2]=husband.substring(0, j).trim();
                    ret[1]=husband.substring(j+1).trim();
                } else {
                    ret[2]=husband;
                }
            }

            wife=wife.replaceAll("\\b"+ret[2]+",?", "").trim();
            if (wife.length()>0)
                ret[5]=ret[2];

            wife=wife.replaceAll("\\.", " ").replaceAll(",", " ").replaceAll(" &", "").trim();

            int k=wife.indexOf(" ");
            if (k!=-1) {
                ret[3]=wife.substring(0, k).trim();
                ret[4]=wife.substring(k+1).trim();
            } else {
                ret[3]=wife;
            }
        }*/

    
    

    public static String OwnerFirstNameDesotoRO(String s) {
        return parseNameDesotoRO(s)[0];
    }

    public static String OwnerMiddleNameDesotoRO(String s) {
        return parseNameDesotoRO(s)[1];
    }

    public static String OwnerLastNameDesotoRO(String s) {
        return parseNameDesotoRO(s)[2];
    }

    public static String SpouseFirstNameDesotoRO(String s) {
        return parseNameDesotoRO(s)[3];
    }

    public static String SpouseMiddleNameDesotoRO(String s) {
        return parseNameDesotoRO(s)[4];
    }

    public static String SpouseLastNameDesotoRO(String s) {
        return parseNameDesotoRO(s)[5];
    }
       
    public static String[] parseOwnerNameAndAddressDesotoAO(String s) {
        String[] ret=new String[2];
        String[] v=s.split("@");
        logger.info("size = "+v.length);
        switch (v.length) {
            case 0:
                ret[0]="";
                ret[1]="";
                break;
            case 1:
                ret[0]=v[0];
                ret[1]="";
                break;
            case 2:
                if (v[1].matches("^\\d\\d.*") || v[1].length()==0) {
                    ret[0]=v[0];
                    ret[1]=v[1];
                } else {
                    ret[0]=v[0]+" & "+v[1];
                    ret[1]="";
                }
                break;
            default:
                if (v[1].matches("^\\d\\d.*") || v[1].length()==0) {
                    ret[0]=v[0];
                    ret[1]=v[1]+" "+v[2];
                } else {
                    ret[0]=v[0]+" & "+v[1];
                    ret[1]=v[2];
                }
        }
        if (ret[0].toUpperCase().startsWith("DESOTO COUNTY (")) {
            ret[0]=ret[0].substring("DESOTO COUNTY (".length());
        }

        return ret;
    }

    public static String OwnerNameDesotoAO(String s) {
        return parseOwnerNameAndAddressDesotoAO(s)[0];
    }

    public static String OwnerAddressDesotoAO(String s) {
        return parseOwnerNameAndAddressDesotoAO(s)[1];
    }

    private static Pattern unitWilly=Pattern.compile("(?is)UNIT(?:S)?(?: NO\\.?)?\\s+((?:\\b\\d+(?:-?[A-Z])?\\b|\\b\\w\\w?\\b)(?:(?:\\s*-\\s*|\\s*&\\s*|\\s*AND\\s*|\\s*,\\s*|\\s+)(?:\\b\\d+\\b|\\b\\w\\w?\\b))*)");
    public static String unitTNWilliamsonRO(String s) {
        Matcher m=unitWilly.matcher(s);
        if (m.find()) {
            return m.group(1);
        } else
            return "";
    }
    
    private static String garbageMOClayRO[] = {"&LT.*$","INCL RES LTS IN"};
    
    public static String SubdivisionMOClayRO(String s){
        String garbageSubiv[] = {"\\n|\\r", "\\d\\.", "\"", "(\\bPT\\s*)?TR\\s*[\\d,&-]+", "(\\bPT\\s*)?\\bSECS?\\s*[\\d,&\\s-]+",
        		"(&\\s*)?(\\bPT\\s*)?\\bLO?TS?\\s*\\d*[A-Z]?\\b(\\s*[&,\\s-]\\s*\\d*[A-Z]?\\b)*", 
        		"(&\\s*)?\\bLO?TS?\\s*(PT\\s*)?\\d*[A-Z]?\\b(\\s*[&,\\s-]\\s*\\d*[A-Z]?\\b)*",
                "PB\\s*[\\d,-]+", "\\bBK\\s*[\\dA-Z,-]+", "\\bPG\\s*[\\d,]+", 
                "\\bBLK II\\b", "(\\bPT\\s)?\\bBLK?S?\\s*\\d*[A-Z]?\\b(\\s*[&,-]\\s*\\d*[A-Z]?\\b)*", 
                "\\bUNIT\\s*[\\d,]+",
                "[A-Z]+\\sST\\s", "ER\\d+", "TOWN OF SODDY", "SUB\\s+\\d+-\\d+", "\\bRESUB\\b", ",.*$", "\\s*\\bPL(AT)?S?\\s*$", " OF ", "\\bREPL\\b", 
                "\\bSEE RECORD.*", "\\bRESE?RVY\\s*", "\\bRES\\b", "CERT/IMPRV", "\\(.*\\)", "(&?\\s*\\b\\d+(ST|ND|RD|TH)\\s*)+$", "\\bAKA\\s*"};
        String tokens[] = {" TRACT"," BEG "," ALL"," PT " ," RES "," BLK ", " BLKS "," BLOCKS ", " LOT "," LOTS "," E "," W "," N "," S "," SUB ",
                " SUBDIVISION", " COMMON "," ---LOT ","(" , " LTS" , "---Units", " UNITS","---Unit" ,"BLK"," AM ", " LT "," LOTS" , "REREC"," REC "};
        
        //cleanup
        if (!s.matches(".*\\bLO?TS?\\b.*") && s.matches("^O?TS? (\\d|[A-Z]).*")) { //Clay RO Instr# 2005007519
        	s = "L"+s;
        }
        s = s.replaceAll("(\\bLO?T \\d+)\\s+([A-Z] OF\\b)", "$1$2");  // Clay RO Instr# R81524
    	s = LegalDescription.cleanGarbage(garbageMOClayRO, s);
    	s = GenericFunctions.replaceNumbers(s);
    	
    	String subdiv = LegalDescription.cleanGarbage(garbageSubiv, s);
        subdiv = LegalDescription.extractSubdivisionNameUntilToken(subdiv, tokens);    	
    	return subdiv.trim();
    }
    
	public static String d10 = "\\b(FIRST|SECOND|THIRD|(FOUR|FIF|SIX|SEVEN|EIGH|NIN)TH)\\b";
	public static String d20 = "\\b(TEN|ELEVEN|TWELF|(THIR|FOURT|FIF|SIX|SEVEN|EIGHT|NINE)TEEN)TH\\b";
	public static String d100 = "\\b(TWEN|THIR|FOR|FIF|SIX|SEVEN|EIGH|NINE)T";
	public static String number = "("+d10+"|"+d20+"|"+d100+"IETH\\b|"+d100+"Y-"+d10+")";
	
    public static String SubdivisionMOClayOR(String subdiv){
    	subdiv = subdiv.replaceAll("(.*?)\\s+(\\d+(ST|ND|RD|TH)-)?\\d+(ST|ND|RD|TH)\\s+(RE)?PLATS?", "$1");
    	subdiv = subdiv.replaceAll("(.*?)\\s+\\(.*$", "$1");
    	subdiv = subdiv.replaceAll("(.*?)-?\\s+"+number+"\\s+(RE)?PLATS?", "$1");
    	subdiv = subdiv.replaceAll("(.*?),?\\s+((A\\s+)?REPLAT( OF)?|((REVISED|AMENDED)\\s)?PLAT|(RE)?SURVEY( OF)?|PHASE|SUBDIVISION|ADDITION|LO?TS?|BL(OC)?KS?|TRACT)\\b.*$", "$1");
    	subdiv = subdiv.replaceAll("(.*?)(,(\\sTHE)?|(\\d+(ST|ND|RD|TH),\\s)?\\d+(ST|ND|RD|TH)\\s?&?)$", "$1");
    	subdiv = subdiv.replaceAll("\\s+(UNIT|UNIT #|#)\\s*\\d+$", "");
    	subdiv = subdiv.replaceAll("\\s+ANNEX$", "");
    	subdiv = subdiv.replaceAll("(.*?)-\\s*TRACT\\s+\\d+.*", "$1");
    	
    	subdiv = GenericFunctions.replaceNumbers(subdiv);
    	subdiv = subdiv.replaceFirst("\\s*\\b\\d+(ST|ND|RD|TH)\\s*$", "");
    	
    	return subdiv;
    }
    
    public static String SubdivisionEquivMOClayRO(String subdiv){
    	subdiv = subdiv.replaceAll("(?is)\\bAME?ND(ED)?\\s+PL(AT)?", "");
    	
    	return subdiv.trim();
    }
    
    public static String LotMOClayRO(String s){
    	String[] lotAndBlock = ro.cst.tsearch.servers.functions.MOClayRO.getLotAndBlock(s);
    	return lotAndBlock[0];    	
    }
    
    public static String BlockMOClayRO(String s){
    	s = LegalDescription.cleanGarbage(garbageMOClayRO, s);
    	
        String block =LegalDescription.extractBlockFromText(s, false, true);    	
    	return block;    	
    }    
    
    public static String SubdivisionKSJohnsonOR(String subdiv){
    	subdiv = subdiv.replaceAll("(.*?)\\s+(\\d+(ST|ND|RD|TH)-)?\\d+(ST|ND|RD|TH)\\s+(RE)?PLATS?", "$1"); 
    	subdiv = subdiv.replaceAll("(.*?)-?\\s+"+number+"\\s+(RE)?PLATS?", "$1"); // fix for bug #1221
    	subdiv = subdiv.replaceAll("(.*?)(\\s+\\b(?:RE)?SURVEY OF.*)", "$1"); // fix for bug #620
    	subdiv = GenericFunctions1.convertFromRomanToArab(subdiv);
    	return subdiv;
    }
    
    public static String replXXX(String s) {
        s=s.replaceAll("(?:\\b\\d+\\w?\\b|\\b\\w\\d*\\b)(?:(?:\\s*-\\s*|\\s*&\\s*|\\s*,\\s*|\\s+)(?:\\b\\d+\\w?\\b|\\b\\w\\d*\\b))*", "@");
//        s=s.replaceAll("(\\b\\d+\\w?\\b|\\b\\w\\d*\\b)((\\s*-\\s*|\\s*&\\s*|\\s*,\\s*|\\s+)(\\b\\d+\\w?\\b|\\b\\w\\d*\\b))*", "@");
        return s;
    }
    
    public static String MonthNo(String s) {
    	s = s.toUpperCase();
    	s = s.replace("JAN", "01");
    	s = s.replace("FEB", "02");
    	s = s.replace("MAR", "03");
    	s = s.replace("APR", "04");
    	s = s.replace("MAY", "05");
    	s = s.replace("JUN", "06");
    	s = s.replace("JUL", "07");
    	s = s.replace("AUG", "08");
    	s = s.replace("SEP", "09");
    	s = s.replace("OCT", "10");
    	s = s.replace("NOV", "11");
    	s = s.replace("DEC", "12");
    	
    	s = s.replaceFirst("(\\d{1,2})-(\\d{1,2})-(\\d{4})", "$2/$1/$3");
        return s;
    }

    /**
     * Converts a date formated as yyyy-mm-dd or yyyy/mm/dd to mm/dd/yyyy format.
     * @param s	the date to be converted, stored as String
     * @return
     */
    public static String DateYearLast(String s) {
    	s = s.toUpperCase();
    	
    	s = s.replaceFirst("(\\d{4}).(\\d{1,2}).(\\d{1,2})", "$2/$3/$1");
    	return s;
    }
        	
    public static String NameILCookLA(String name){
    	name = name.replace(";", " ");
    	name = name.replace(".", " ");
    	name = name.replaceAll("\\bA/G\\b", "");
    	name = name.replaceAll("\\bR\\s?E AGENT\\b", "");
    	name = name.replaceAll("\\bCURRENT OWNER", "");    	
    	name = name.replace("/", " ");
    	name = name.replaceAll("\\bMR\\s*&\\s*MRS\\b", "");    	
    	name = name.replaceAll("\\s{2,}", " ").trim();
    	name = name.replaceFirst("(.+)\\s*,$", "$1");
    	return name;
    }
    
    public static String[] parseNameILCookLA(String name){
    	String[] ret = {"", "", "", "", "", ""};
    	boolean isTrust = false;
    	boolean isLFM = name.contains(";");
    	
    	name = NameILCookLA(name);

    	String s = name.replaceAll("(?i)\\bTR(\\.?\\s?#)?\\s*\\d+", "");
    	isTrust = !name.equals(s);
    	s = s.replaceAll("\\b\\d+\\b", " ");
    	s = s.replaceAll("\\b\\d+\\w+\\s*$", " ");
    	s = s.replaceAll("\\s{2,}", " ").trim();
    	
    	String coowner = "";
    	Pattern p = Pattern.compile("([A-Z]+ [A-Z]+) & ([A-Z]+ [A-Z]+)"); // D WILSON & J ECKHOLM, PID 16-07-312-026-1014
    	Matcher ma = p.matcher(s);
    	if (ma.matches()){
    		isLFM = false;
    		s =  ma.group(1);
    		coowner = ma.group(2); 
    	} else {   	    	
	    	// change F & WF L names (as JOSE & DIANA BAEZ) to L F & WF, because FML parser does not parse it correctly   
	    	p = Pattern.compile("\\A(\\w+\\s*&\\s*\\w+)\\s(\\w+)");  // BOONE EDWARD L&NANCY P; YANCKOWITZ ANNA H&JOHN R; CATHEY ROBERT G&MARTHA C
	    	ma = p.matcher(s);
	    	if (ma.find()){
	    		isLFM = true;
	    		s =  ma.group(2) + " " + ma.group(1); 
	    	} else {    	
		    	p = Pattern.compile("[A-Z'-]{2,} [A-Z]{2,} [A-Z]");
		    	ma = p.matcher(s);
		    	if (ma.matches()){
		    		isLFM = true; 
		    	} 
	    	}
    	}
    	
    	if (isTrust){    		
    		ret[2] = s;
    	} else {
	    	p = Pattern.compile("([A-Z'-]{2,}) (JR)");
	    	ma = p.matcher(s);
	    	if (ma.matches()){
	    		ret[2] = ma.group(1);
	    		ret[0] = ma.group(2);
	    	} else if (isLFM || s.contains(",") || s.contains("&")){	// apply L F M tokenizer
	    		ret = StringFormats.parseNameNashville(s);
	    	} else {									// apply F M L tokenzier    	
	    		ret = StringFormats.parseNameDesotoRO(s);
	    		if (coowner.length() != 0){
	    			String[] names = StringFormats.parseNameDesotoRO(coowner);
	    			ret[3] = names[0];
	    			ret[4] = names[1];
	    			ret[5] = names[2];
	    		}
	    	}
    	}
    	return ret;
    }
    
    public static String OwnerFirstNameILCookLA(String s) {
        return parseNameILCookLA(s)[0];
    }

    public static String OwnerMiddleNameILCookLA(String s) {
        return parseNameILCookLA(s)[1];
    }

    public static String OwnerLastNameILCookLA(String s) {
        return parseNameILCookLA(s)[2];
    }

    public static String SpouseFirstNameILCookLA(String s) {
        return parseNameILCookLA(s)[3];
    }

    public static String SpouseMiddleNameILCookLA(String s) {
        return parseNameILCookLA(s)[4];
    }

    public static String SpouseLastNameILCookLA(String s) {
        return parseNameILCookLA(s)[5];
    }
    
    public static String DTSNameCleanup(String s) {
    	
    	s = s.replaceAll("/(JR|SR|[IV]+)\\b.*", " $1");    	
    	s = s.replaceFirst("/([^\\s]|$).*", "");
    	return s;    	
    }
    
    public static String NameASK(String name){    	
    	name = name.replace(".", " ");
    	name = name.replace("a k a", "aka");
    	name = name.replaceAll("\\band\\b", "&");
    	boolean isCompany = NameUtils.isCompany(name);
    	if (name.contains(",") && !isCompany){
    		name = name.replaceAll("(.+?)\\s*,[^&]*", "$1 ");
    		
    	// for cases as "Maude I. Cobb, survivor of herself and her deceased husband L. Duane Cobb" or "Michael Woods, Attorney for Aegis Mortgage Corporation"    		
    	} else if(name.contains(",") &&  (name.contains("survivor") || name.contains("ttorney"))){ 
    		name = name.replaceAll("(.+)\\s*,.*", "$1");
    	}	
    	   
    	if (name.contains(" aka ")){
        	String akaNames = "";    		
    		akaNames = name.replaceFirst(".+? aka (.+?)($|\\s*&\\s*.+)", "$1");
    		name = name.replaceFirst("(.+?) aka .+?($|\\s*&\\s*.+)", "$1 $2");
    		if (name.contains(" aka ")){
        		akaNames = akaNames + " & " + name.replaceFirst(".+? aka (.+)$", "$1");
        		name = name.replaceFirst("(.+?) aka .+", "$1");        		
        	}
    		name = name + "/" + akaNames;
    	}    	
    	    	    	
    	name = name.replaceAll("\\s{2,}", " ").trim();
    	
    	return name;
    }
    
    public static String[] parseNameASK(String name){
    	String[] ret = {"", "", "", "", "", ""};
    	boolean isCompany = NameUtils.isCompany(name);
    	
    	// change F MI? & WF L names (as Patrick M & Tina Ott) to L F & WF
    	if (!isCompany){
	    	Pattern p = Pattern.compile("^(\\w+(?:\\s\\w)?)(\\s*&\\s*.+)(\\s\\w+)");
	    	Matcher ma = p.matcher(name);
	    	if (ma.find()){
	    		name =  ma.group(1) + ma.group(3) + ma.group(2) + ma.group(3); 
	    	}
	    	ret = StringFormats.parseNameDesotoRO(name);
    	} else {
    		ret[2] = name;
    	}    	
    	
    	return ret;
    }
    
    public static String OwnerFirstNameASK(String s) {
        return parseNameASK(s)[0];
    }

    public static String OwnerMiddleNameASK(String s) {
        return parseNameASK(s)[1];
    }

    public static String OwnerLastNameASK(String s) {
        return parseNameASK(s)[2];
    }

    public static String SpouseFirstNameASK(String s) {
        return parseNameASK(s)[3];
    }

    public static String SpouseMiddleNameASK(String s) {
        return parseNameASK(s)[4];
    }

    public static String SpouseLastNameASK(String s) {
        return parseNameASK(s)[5];
    }
    
    private static Pattern rangePatMichiganRO = Pattern.compile( "(\\d+)\\s+-\\s+(\\d+)");
    public static String RangeMichiganRO(String lot){
 		lot = lot.replaceAll("\\b0+(\\d)", "$1"); // remove leading zeros from lots value
 		Matcher match = rangePatMichiganRO.matcher(lot);  // if the interval limits are equal, remove the duplicate lot value
 		if (match.find()){
 			if (match.group(1).equals(match.group(2)))
 				lot = match.group(1);
 		}
 		return lot;
    }
    
    public static String SubdivCondoMIMacombRO(String subdiv){
	   subdiv = subdiv.replaceFirst("(.+?)\\b(SUB(DIVISION)?|ADD(ITION)?|CONDO|PLAN)\\b.*", "$1");
	   subdiv = subdiv.replaceAll("\\s*\\(.*\\)", "");
	   subdiv = subdiv.replaceFirst("(.+) & (RE)?SURVEY", "$1");
	   return subdiv;
   }
    
    public static String cleanRuralCityName (String city){
    	city = city.replaceFirst("(?i).*\\b(UNINCORPORATED|UNKNOWN|RURAL)\\b.*", "");
    	return city;
    }
    
    public static void main (String args[]) {
/*        logger.info(unitTNWilliamsonRO("UNIT 2 B asd"));
        if (true)         return;
*/
        /*String[] v={"LOTS UNIT 1"
                    };
        for (int i=0; i<v.length; i++) {
            logger.info("Input = \""+v[i]+"\"");
            logger.info("Name  = \""+SubdivisionNashvilleAOnew(v[i])+"\"");
            logger.info("Lot   = \""+LotNashvilleAO(v[i])+"\"");
            logger.info("Sec   = \""+SectionNashvilleAO(v[i])+"\"");
            logger.info("Phase = \""+PhaseNashvilleAO(v[i])+"\"");
            logger.info("Unit  = \""+UnitNashvilleAO(v[i])+"\"");
        }*/
    	
//    	System.out.println(ReplaceEnumerationWithInterval("1 2 D 3 6 7"));
//    	
//    	System.err.println(PhaseNashvilleAO("UNIT 40 NASHBORO VILLAGE TRACT 18 PH ASE I"));
//    	System.err.println(PhaseNashvilleAO("UNIT 40 NASHBORO VILLAGE TRACT 18 PHASE I"));
//    	System.err.println(PhaseNashvilleAO("UNIT 40 NASHBORO VILLAGE TRACT 18 PH I"));
//    	System.err.println(PhaseNashvilleAO("UNIT 40 NASHBORO VILLAGE TRACT 18 PH ASES I"));
//    	System.err.println(PhaseNashvilleAO("UNIT 40 NASHBORO VILLAGE TRACT 18 PHASES I"));
//    	
//        if (true)         return;
    	parseNameDesotoRO("TRACY D ALESSANDRO", true);
        
/*
        String[] v={
            "MTG.RP.4782/1674 ",
            "MTG RP 5227/890 ",
            "MTG.RP.4423/1029 ",
            "L 12 PINE RUN U 1 ",
            "L 2 NORTH PARK ESTS U 1 ",
            "L 20 FIELDVIEW ESTS (MTG RP 5838/150) ",
            "MTG RP 4099/169 (WEIGEL DEBRA K) ",
            "PT L 5 CREOLA FARMS SUB ",
            "JUD RP 4186/32 ",
            "RE RP 5453/525 STILL IN FULL FORCE & EFFECT ",
            "L 13 B 242 RESUB PT SOUTHERN DIV BERNOUDY TRACT ",
            "L 1,2 B 38 NORTH MOBILE SUB",
            "MTG.RP.5303/197 (EDWARDS SYLVESTER SR)",
            "S 29 1N1W ",
            "S 16 6S3W ",
            "L J-2 SPRING LAKE U 2 ",
            "MTG RP 5246/287, 5246/309 ",
            "RE REST ON SEW SYS PROP LOCATED 6111 DEERFIELD CT S ",
            "S 6 7S2W",
            "RE MTG RP 5053/605 ",
            "L 2 B 4S-D U 4 WATER ST URBAN RENEWAL PROJECT ",
            "CLAIM RP.5189/460",
            "L 3,4,5,6 CORNELIUS C HAMMAC EST",
            "VL RP 1328/46",
            "H.L.RP.5478/1877",
            "H/LIEN RP 5485/399 ",
            "S 2 3S2W ",
            "RP 4892/19",
            "L P RP 4632/14 ",
            "L B,C NORA HARRIS PROPERTY DIV ",
            "L 11,PT L 12 B 5 TCI&RR CO'S 1ST ADD CK'SAW BOGUE ",
            "COMG SW COR L 24-A B C 1ST ADD DOG RIVER PARK ETC.",
            "L 10,PT L 21 FOWL RIVER PARK",
            "LIEN.RP.5450/1485 ",
            "L 12 BAY RANCH(RP.4888/731 ",
            "L 10 THE OAKS FOWL RIVER PHASE 1",
            "L 60 BRIGHTON ESTS PHSE 2 ",
            "L 2 THRU 10 L 50,51,52,54, 56,57,58,59,60,61 WOODBERRY ",
            "FRAC S 45 2N1E ",
            "MTG.RP.5276/1454,RP.5497/1546 ",
            "L 7-B DEAD LAKE",
            "L 23E WILLOW POINTE U 1 ",
            "L 26 PARK COURT 1ST ADDJUD.RP.3404/791,RP.2637/992,RP.3481/16JUD.RP.3346/547 "

        };
        for (int i=0; i<v.length; i++) {
            logger.info("Input = \""+v[i]+"\"");
            logger.info("Name  = \""+SubdivisionMobile(v[i])+"\"");
            logger.info("Lot   = \""+LotMobile(v[i])+"\"");
            logger.info("Sec   = \""+SectionMobile(v[i])+"\"");
            logger.info("Phase = \""+PhaseMobile(v[i])+"\"");
            logger.info("Unit  = \""+UnitMobile(v[i])+"\"");
            logger.info("Block  = \""+BlockMobile(v[i])+"\"");
            logger.info("BookAndPageMobile = "+parseBookAndPageMobile(v[i])[0]);
            logger.info("-------------------------------------------------");
        }
*/        
/*        for (int i=0; i<v.length; i++) {
            logger.info("Input = \""+v[i]+"\" StreetNo = \""+StreetNo(v[i])+"\" , StreetName =  \""+StreetName(v[i])+"\"");
        }
        String sub="LOT 20 & PT. LOT 19, SEC. 2 GLENDALE PIKE";
        logger.info("Name  = "+SubdivisionNashvilleAO(sub));
        logger.info("Lot   = "+LotNashvilleAO(sub));
        logger.info("Sec   = "+SectionNashvilleAO(sub));
        logger.info("Phase = "+PhaseNashvilleAO(sub));*/
    }
}


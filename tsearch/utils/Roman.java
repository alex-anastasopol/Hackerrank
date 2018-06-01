package ro.cst.tsearch.utils;

import java.util.regex.*;
import java.util.*;
import org.apache.log4j.Category;

import ro.cst.tsearch.search.name.RomanNumber;

public class Roman {
	
	protected static final Category logger= Category.getInstance(Roman.class.getName());
	
    protected static Map letters=new HashMap();
    static {
        letters.put(new Character('M'), new Integer(1000));
        letters.put(new Character('D'), new Integer(500));
        letters.put(new Character('C'), new Integer(100));
        letters.put(new Character('L'), new Integer(50));
        letters.put(new Character('X'), new Integer(10));
        letters.put(new Character('V'), new Integer(5));
        letters.put(new Character('I'), new Integer(1));
    }

    protected static int parseLetter(char c) throws Exception {
        Integer i=(Integer)letters.get(new Character(c));
        if (i==null)
            throw new Exception("Invalid letter : "+c);
        return i.intValue();
    }

    protected static int parseGroup(String s) throws Exception {
        char[] a=s.toCharArray();
        int ret=0;
        for (int i=0; i<a.length; i++) {
            int ai=parseLetter(a[i]);
            int j=i+1;
            if (j<a.length) {
                int aj=parseLetter(a[j]);
                if (aj>ai) {
                    ret+=aj-ai;
                    i=j+1;
                } else
                    ret+=ai;
            } else
                ret+=ai;
        }
        return ret;
    }

    public static int parseRoman(String s) throws Exception {
        s=s.toUpperCase().trim();
        String s1=s;
        if (s.length()==0)
            throw new Exception("Invalid roman number : \"\"");
        if (!s.matches("^[MDCLXVI]+$"))
            throw new Exception("Invalid roman number : \""+s+"\"");
        int ret=0;
        Matcher m=Pattern.compile("^M+").matcher(s);
        if (m.find()) {
            ret=m.group().length()*1000;
            s=s.substring(m.end());
        }
        m=Pattern.compile("^(CM|CD|D?C{1,3}|D)").matcher(s);
        if (m.find()) {
            ret+=parseGroup(m.group());
            s=s.substring(m.end());
        }
        m=Pattern.compile("^(XC|XL|L?X{1,3}|L)").matcher(s);
        if (m.find()) {
            ret+=parseGroup(m.group());
            s=s.substring(m.end());
        }
        m=Pattern.compile("^(IX|IV|V?I{1,3}|V)").matcher(s);
        if (m.find()) {
            ret+=parseGroup(m.group());
            s=s.substring(m.end());
        }
        if (s.length()!=0)
            throw new Exception("Invalid roman number : \""+s1+"\"");

        return ret;
    }
    
    public static boolean isRoman(String s) {
    	try {
    		parseRoman(s);
    		return true;
    	} catch (Exception e) {
    		return false;
    	}
    }
    
    public static Pattern tok=Pattern.compile("(?i)^([MDCLXVI]+)(\\s.*)?$");
    public static String transformToArabic(String s) { // except M, D, C, L
        s=s.trim();
        Matcher m=tok.matcher(s);
        if (m.find()) {
            String t=m.group(1);
            if (t.matches("^[MDCLmdcl]$"))
                return s;
            int i;
            try {
                i=parseRoman(t);
            } catch (Exception e) {
                return s;
            }
            t=String.valueOf(i);
            if (m.group(2)!=null) 
                t+=m.group(2);
            return t;
        } else
            return s;
    }
    
    public static String normalizeRomanNumbers(String s) {
		String[] t = s.split("\\s+");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < t.length; i++) {
			sb.append(RomanNumber.convertToRomanNumber(t[i]));
			sb.append(" ");
		}
		return sb.toString().trim();
	}
    
    public static String normalizeRomanNumbersIfLast(String s) {
		String[] t = s.split("\\s+");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < t.length; i++) {
			if( i==t.length-1 )
				sb.append(RomanNumber.convertToRomanNumber(t[i]));
			else 
				sb.append(t[i]);
			sb.append(" ");
		}
		return sb.toString().trim();
	}
    
    public static String normalizeRomanNumbersIfLastExceptTokens(String s, String[] tokens) {
		String[] t = s.split("\\s+");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < t.length; i++) {
			if( i==t.length-1 )
				sb.append(Roman.normalizeRomanNumbersExceptTokens(t[i], tokens));
			else 
				sb.append(t[i]);
			sb.append(" ");
		}
		return sb.toString().trim();
	}
    
    public static String normalizeRomanNumbersExceptTokens(String s, String[] tokens) {
		String[] t1 = s.split("\\s+");
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < t1.length; j++) {
			String[] t = t1[j].split("\\b");
			for (int i=0; i<t.length; i++){
				boolean isToken = false;
				for (int k=0; k<tokens.length; k++){
					if (t[i].equals(tokens[k])){
						isToken = true;
						break;
					}
					if (!tokens[k].equals("I") && t[i].contains(tokens[k])){
						isToken = true;
						break;
					}
				}
				if (!isToken){
					sb.append(RomanNumber.convertToRomanNumber(t[i]));				
				} else {
					sb.append(t[i]);
				}
			}
			sb.append(" ");			
		}
		return sb.toString().trim();
	}

    public static String arabicToRoman(String s){
		int nr = 0;
		try {
			nr = Integer.parseInt(s);
		} catch (Exception e){
			return s;
		}
		return arab2r(nr);
    }
	
    public static String arab2r(int nr){

    	String roman = "";
    	try {      
    		if (nr >= 889){
    			roman = "M" + arab2r((nr-1000));
    		} else if(nr >= 389){
    			roman = "D" + arab2r((nr-500));
    		} else if(nr >= 89){
    			roman = "C" + arab2r((nr-100));
    		} else if(nr >= 39){
    			roman = "L" + arab2r((nr-50));
    		} else if(nr >= 9){
    			roman = "X" + arab2r((nr-10));
    		} else if(nr >= 4){
    			roman = "V" + arab2r((nr-5));
    		} else if(nr >= 1){
    			roman = "I" + arab2r((nr-1));
    		} else if(nr <= -889){
    			roman = "M" + arab2r(nr+1000);
    		} else if(nr <= -389){
    			roman = "D" + arab2r(nr+500);
    		} else if(nr <= -89){
    			roman = "C" + arab2r(nr+10);
    		} else if(nr <= -39){
    			roman = "L" + arab2r(nr+50);
    		} else if(nr <= -9){ 
    			roman = "X" + arab2r(nr+10);
    		} else if(nr <= -4){
    			roman = "V" + arab2r(nr+5);
    		} else if(nr <= -1){
    			roman = "I" + arab2r(nr+1);
    		}
    	} catch(Exception e) {}
      
    	return roman;
    }

    public static void main (String args[]) throws Exception {
        if (args.length==0) {
            logger.error("Usage : java Roman <roman_number>");
            System.exit(1);
        }
        logger.info("" + parseRoman(args[0]));
    }
}

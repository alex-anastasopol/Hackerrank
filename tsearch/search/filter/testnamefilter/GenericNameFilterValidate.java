package ro.cst.tsearch.search.filter.testnamefilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.BigDecimalValidator;


public class GenericNameFilterValidate {
	//the non alpha characters accepted in names
	final static private Pattern nameExtraPattern = Pattern.compile("[0-9 -.'*]");
	final static private Pattern onPattern = Pattern.compile("^on$");
	//negative value for unlimited wrong candidates
	
		
	public static boolean validateCandidatesFile(String[][] c){
		if (GenericNameFilterTestConf.nAcceptedWrongCandidates < 0) return true; 
		int nWrongCandidates = 0;
		for (int i = 0; i< c.length; i++){
			if (! validateCandidate(c[i])){
				c[i] = null;
				nWrongCandidates++;
			}
		}
	
		return GenericNameFilterTestConf.nAcceptedWrongCandidates >= nWrongCandidates;
	}
	
	public static Vector<String[]> validateCandidatesFile(Vector<String[]> c){
		Iterator i = c.iterator();
		//if (GenericNameFilterTestConf.nAcceptedWrongCandidates < 0) return c; 
		int nWrongCandidates = 0;
		while (i.hasNext()){
			String[] tmpS = (String[])i.next();
			if (tmpS.length != 3 || ! validateCandidate(tmpS)){
				i.remove();
				nWrongCandidates++;
			}
		}
	
		return (GenericNameFilterTestConf.nAcceptedWrongCandidates < 0 || GenericNameFilterTestConf.nAcceptedWrongCandidates >= nWrongCandidates)?c:null;
	}	
	
	public static boolean validateCandidate(String[] c){
		for (int i =0; i<c.length; i++)
			if (! validateName(c[i])) return false;
		return true;
	}
	
	public static boolean validateName(String n){
		Matcher m;
		String t;
		m = nameExtraPattern.matcher(n);
		t = m.replaceAll("");
		return StringUtils.isAlpha(t) && n.length() <= GenericNameFilterTestConf.maxCharacters;
	}
	
	public static boolean validateBigDecimal(String b){
		return (new BigDecimalValidator().validate(b)) != null;
	}
	
	public static boolean validateOnCheck(String b){
		return onPattern.matcher(b).matches();
	}
}

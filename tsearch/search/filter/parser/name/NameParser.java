package ro.cst.tsearch.search.filter.parser.name;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.search.tokenlist.TokenList;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.extractor.xml.GenericFunctions;

public class NameParser implements Serializable {
    
    static final long serialVersionUID = 10000000;
    
	protected static final Category logger = Category.getInstance(NameParser.class.getName());
	
	protected  String[] delimitsSeveralNames ;
	protected  String[] delimitsOwnerSpouseNames ;
	protected String[] abbreviations ;

	public NameParser (){
		delimitsSeveralNames = new String[]{};
		delimitsOwnerSpouseNames = new String[]{};
		abbreviations = new String[]{ };
	}
										
										
	public  List parseNames(String input){
		//logger.debug("input parse names = " + input);
		List names = StringUtils.splitAfterDelimitersList(input, delimitsSeveralNames);
		List rez = new ArrayList();
		
		for (int i=0; i<names.size(); i++) {
			String candName = (String) names.get(i);
			candName = candName.replaceAll(TSServer.AND_REPLACER, "&");
			
			hookPosition(i);
			
			NameTokenList[] ntl = parseName(candName);
			rez.add(ntl);
		}
		return rez;
	}

	
	protected void hookPosition(int i) {
	}


	public NameTokenList[] parseName(String input){
		input = input.toUpperCase();
		//logger.debug("input parse name = " + input);
		
		List l = StringUtils.splitAfterDelimitersList(input, delimitsOwnerSpouseNames);
		l = removeAbbreviations(l);
		
		NameTokenList owner = parseOwnerName((String) l.get(0));
		NameTokenList spouse = new NameTokenList();
		if (l.size() > 1){
			spouse = parseSpouseName((String) l.get(1), owner);
		}
		// if l.size()>2...the remaining tokens are ignored
		//logger.debug("owner = " + owner);
		//logger.debug("spouse = " + spouse);
		return new NameTokenList[]{owner, spouse};
	}

	public NameTokenList parseOwnerName(String input){
		return parseNameLFM(input);
	}

	public static NameTokenList parseSpouseName(String string, NameTokenList owner) {
		String lastName =TokenList.getString(owner.getLastName());
		return parseNameFML(string, lastName);
	}
	
	
	protected String removeAbbreviations(String s) {
		//logger.debug("remove abbrev" + Arrays.asList(abbreviations));
		s=s.toUpperCase();
		List l = new ArrayList(StringUtils.splitString(s));
		l.removeAll(Arrays.asList(abbreviations)) ;
		return StringUtils.join(l," ");
	}

	protected  List removeAbbreviations(List l) {
		List rez = new ArrayList();
		//logger.debug("enter remove abbrev");
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			rez.add(removeAbbreviations((String) iter.next()));
		}
	
		return rez;
	}

	public static NameTokenList parseNameLFM(String string) {
		string = string.replaceAll(",", " ");
		List l = StringUtils.splitString(string);
		String lastName ="";
		String firstName ="";
		String middleName ="";
		if (l.size() >0 ){
			lastName = (String) l.get(0);
			lastName = StringUtils.removeTrailingSubstring(lastName, ",");
		}
		if (l.size()>1){
			firstName = (String) l.get(1);
			firstName  = StringUtils.removeTrailingSubstring(firstName , ".");
		}
		if (l.size()>2){
			l  = StringUtils.removeTrailingSubstring(l.subList(2, l.size()) , ".");
			middleName = StringUtils.join( l, " ");
		}
		return new NameTokenList(lastName, firstName, middleName);
	}

	public static NameTokenList parseNameFML(String string, String lastName) {
		boolean sameLastName =false;
		String firstName ="";
		String middleName ="";
	
		if ((!lastName.equals(""))&&(string.indexOf(lastName)>-1)){
			string = string.replaceAll(lastName,"");
			sameLastName = true;
		}
	
		string = string.replaceAll(",", " ");
		
		String suffix = "";
		Matcher ma = GenericFunctions.nameSuffix.matcher(string);
		if (ma.find()){
			string = ma.group(1);
			suffix = ma.group(2);
		}
		
		List l = StringUtils.splitString(string);
		
		int middleFromIdx = 0;
		int middleToIdx = l.size();
		if (l.size() > 0 ){
			firstName = (String) l.get(0);
			firstName  = StringUtils.removeTrailingSubstring(firstName , ".");
			middleFromIdx = 1;
		}
		if ((((String)l.get(l.size()-1)).length()>2 )&&(!sameLastName)&&(l.size()>1)){//the last token is not an abreviation
			lastName = (String) l.get(l.size()-1);
			middleToIdx = l.size()-1;
		}
		l  = StringUtils.removeTrailingSubstring(l.subList(middleFromIdx, middleToIdx) , ".");
		middleName = StringUtils.join( l, " ");
		
		if (suffix.length() > 0){
			middleName = middleName + " " + suffix;
			middleName = middleName.trim();
		}
		
		return new NameTokenList(lastName, firstName, middleName);
	}

	public static NameTokenList parseNameFML(String string) {
		if (NameUtils.isCompany(string)){				// fix for bug #2976
			return new NameTokenList(string, "", "");
		}
		return parseNameFML(string,"");
	}


}

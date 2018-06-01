/*
 * Created on Mar 30, 2004
 *
 */
package ro.cst.tsearch.search.filter.parser.name;

import java.util.List;

import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.search.tokenlist.TokenList;
import ro.cst.tsearch.utils.StringUtils;

/**
 * Name parsing for Baldwin AO & TR.
 * 
 * @author catalinc
 *
 */
public class BaldwinAssessorNameParser extends NameParser
{
	public BaldwinAssessorNameParser() 
	{
		delimitsSeveralNames = new String[]{"\\bETAL\\b"};	
		delimitsOwnerSpouseNames = new String[]{"\\bAND\\b", "&","\\bETUX\\b"};
	}
	
	public NameTokenList[] parseName(String input){
		input = input.toUpperCase();
		input = input.replaceAll("\\(","&");
		input = input.replaceAll("\\)","");

		List names = StringUtils.splitAfterDelimitersList(input, delimitsSeveralNames);
				
		List l = StringUtils.splitAfterDelimitersList((String)names.get(0), delimitsOwnerSpouseNames);
		l = removeAbbreviations(l);
		
		NameTokenList owner = parseOwnerName((String) l.get(0));
		NameTokenList spouse = new NameTokenList();
		if (l.size() > 1){
			spouse = parseSpouseName((String) l.get(1), owner);
		}

		return new NameTokenList[]{owner, spouse};
	}		
}

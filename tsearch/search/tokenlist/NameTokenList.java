/*
 * Created on May 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.tokenlist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.token.Token;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class NameTokenList extends TokenList {

	protected static final Category logger= Category.getInstance(NameTokenList.class.getName());
	
	private List lastName = new ArrayList();
	private List firstName = new ArrayList();
	private List middleName = new ArrayList();

	public void clearMiddleName(){
		middleName = new ArrayList();
	}
	
	public NameTokenList() {
	}

	public NameTokenList(List last, List first) {
		init(last, first, new ArrayList());
	}

	public NameTokenList(List last, List first, List middle) {
		init(last, first, middle);
	}

	public NameTokenList(String last, String firstmiddle) {
		List l = StringUtils.splitString(firstmiddle);
		String first = firstmiddle;
		String middle = "";
		if (l.size() > 0) {
			first = (String) l.get(0);
			middle = firstmiddle.substring(first.length());
		}
        
        last = StringUtils.addDelimiterToStr( last, "&", " " );
        
		init(
			StringUtils.splitString(last),
			StringUtils.splitString(first),
			StringUtils.splitString(middle));
	}

	public NameTokenList(String last, String first, String middle) {
        
        last = StringUtils.addDelimiterToStr( last, "&", " " );
        
		init(
			StringUtils.splitString(last),
			StringUtils.splitString(first),
			StringUtils.splitString(middle));
	}

	public NameTokenList(NameTokenList tl) {
		if (tl != null) {
			lastName.addAll(tl.getLastName());
			firstName.addAll(tl.getFirstName());
			middleName.addAll(tl.getMiddleName());
		}
	}

	private void init(List last, List first, List middle) {
		lastName.addAll(buildTokenList(last, Token.TYPE_LAST_NAME));
		firstName.addAll(buildTokenList(first, Token.TYPE_FIRST_NAME));
		middleName.addAll(buildTokenList(middle, Token.TYPE_MIDDLE_NAME));
	}

	private List buildTokenList(List l, int type) {
		List lt = new ArrayList();
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			String s = (String) iter.next();
			if ((type == Token.TYPE_FIRST_NAME)
				|| (type == Token.TYPE_MIDDLE_NAME)) {
				s = s.replaceAll("\\.\\z", "");
			}
			lt.add(new Token(s, type));
		}

		return lt;
	}

	public List getList() {
		List l = new ArrayList();
		l.addAll(lastName);
		l.addAll(firstName);
		l.addAll(middleName);
		return l;
	}

	/**
	 * @return
	 */
	public List getFirstName() {
		return firstName;
	}

	public String getFirstNameAsString() {
		return TokenList.getString(firstName);
	}

	public List getLastName() {
		return lastName;
	}

	public String getLastNameAsString() {
		return TokenList.getString(lastName);
	}

	public List getMiddleName() {
		return middleName;
	}

	public String getMiddleNameAsString() {
		return TokenList.getString(middleName);
	}
	public static List getInitials(List tokenList) {
		List l = new ArrayList();
		for (Iterator iter = tokenList.iterator(); iter.hasNext();) {
			Token token = (Token) iter.next();
			l.add(new Token(getInitial(token.getString())));
		}
		return l;
	}

	public static String getInitial(String s) {
		if (s.endsWith(".")) {
			s = s.substring(0, s.length() - 1);
		}

		if (s.length() < 3) {
			return s;
		}

		if (s.matches(".*\\d+.*")) {
			return s;
		}

		for (int i = 0; i < specialCasesForInitial.length; i++) {
			String special = specialCasesForInitial[i];
			if (special.equalsIgnoreCase(s)) {
				return s;
			}
		}
		//logger.debug("s4 = " + s);

		/*String[] str = s.split("-");
		List l = new ArrayList();
		for (int i = 0; i < str.length; i++) {
			l.add(getInitial(str[i]));
		}
		return StringUtils.join(l, " ");*/
		s = s.substring(0, 1);
		return s;
	}

	private static String[] specialCasesForInitial = new String[] { "III" };
	public List getFirstNameInitials() {
		return getInitials(getFirstName());
	}

	public List getMiddleNameInitials() {
		return getInitials(getMiddleName());
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof NameTokenList))
			return false;
		NameTokenList cand = (NameTokenList) o;

		return cand.getFirstName().equals(getFirstName())
			&& cand.getMiddleName().equals(getMiddleName())
			&& cand.getLastName().equals(getLastName());
	}

	public boolean startsWith(NameTokenList ntl) {
		return getString(getFirstName()).startsWith(
			getString(ntl.getFirstName()))
			&& !StringUtils.isStringBlank(getString(ntl.getFirstName()))
			&& getString(getMiddleName()).startsWith(
				getString(ntl.getMiddleName()))
			&& !StringUtils.isStringBlank(getString(ntl.getMiddleName()))
			&& getString(getLastName()).startsWith(getString(ntl.getLastName()))
			&& !!StringUtils.isStringBlank(getString(ntl.getLastName()));
	}

	public String toString() {
		String last = getLastNameAsString();
		String first = getFirstNameAsString();
		String middle = getMiddleNameAsString();
		if (StringUtils.isStringBlank(last)
			&& StringUtils.isStringBlank(first)
			&& StringUtils.isStringBlank(middle)) {
			return "";
		}

		String s = last + ", " + first;
		if (!StringUtils.isStringBlank(middle)) {
			s += " _ " + middle;
		}
		return s;
	}
	
	/**
	 * Collapse first, middle, last in a single word
	 */
	public NameTokenList collapse(){
		
		String ln = getString(lastName).toUpperCase().replaceAll("\\s+", " ").trim();
		String fn = getString(firstName).toUpperCase().replaceAll("\\s+", " ").trim();  
		String mn = getString(middleName).toUpperCase().replaceAll("\\s+", " ").trim();
		
		return new NameTokenList(ln, fn, mn);
		
	}
	
	/**
	 * Check if empty
	 * @return
	 */
	public boolean isEmpty(){		
		String criteria = getString(lastName) + getString(firstName) + getString(middleName);
		return StringUtils.isEmpty(criteria);
	
	}
	
	@Override
	public int hashCode() {
		return getString(lastName).hashCode() +  
		       getString(firstName).hashCode() * 17 + 
		       getString(middleName).hashCode() * 23;
	}
	
	public String display(){
		return "Last='" + getString(lastName) + 
				"' First='" + getString(firstName) + 
				"' Middle='" + getString(middleName) + "'";
	}
}

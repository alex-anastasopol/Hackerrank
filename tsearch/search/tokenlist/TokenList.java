package ro.cst.tsearch.search.tokenlist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.token.Token;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 */
public class TokenList implements Serializable {

    static final long serialVersionUID = 10000000;
    
	protected static final Category logger = Category.getInstance(TokenList.class.getName());

	protected List list= new ArrayList();
	
	public TokenList(){
	}

	public TokenList(String str){
		buildList(StringUtils.splitString(str));
	}

	public TokenList(List l){
		if ((l != null)&&(l.size() != 0)){
			buildList(l);
		}else{
			buildList(Arrays.asList(new String[]{""}));
		}
	}

	public TokenList(TokenList tl){
		if ((tl != null)&&(tl.getList()!=null)&&(tl.getList().size()!=0)){
			for (Iterator iter = tl.getList().iterator(); iter.hasNext();) {
				Token t = (Token) iter.next();
				add(new Token(t));
			}
		}else{
			buildList(Arrays.asList(new String[]{""}));
		}
	}

	protected void buildList(List tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			Object o = tokens.get(i);
			Token token;
			if (o instanceof Token){
				token = new Token ((Token) o);
			}else{
				token = new Token ((String) o);
			}
			add(token);
		}
	}


	public void add (Token t) {
		getList().add(t); 
	}

	public void addAll(TokenList tl){
		if ((tl != null)&&(tl.getList()!=null)){
			for (Iterator iter = tl.getList().iterator(); iter.hasNext();) {
				add((Token) iter.next());
			}
		}
	}


	public List getList(){
		return list; 
	}

	public String getString(){
		return getString(getList());
	}
	
	public static String getString(List l){
		String s = "";
		List ls = new ArrayList();
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			String element = ((Token) iter.next()).getString();
			ls.add(element);
		}		
		return StringUtils.join(ls, " ");
	}

	public static String getStringToken(List l, int index) {
		
		if (index < l.size())
			return ((Token)l.get(index)).getString();
		
		return "";
	}
	
	public static List getStringList(List l){
		List ls = new ArrayList();
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			ls.add(((Token) iter.next()).getString());
		}		
		return ls;
	}

	public String toString(){
		int beginIndex = this.getClass().getPackage().getName().length()+1;
		String className = this.getClass().getName().substring(beginIndex);
		String s = className + " [" + getList() + "]; "; 
		return s;
	}

	public boolean containsTokenIgnoreCase(Token token){
		if (token == null){
			return false;
		}

		List l = this.getList(); 
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			if (token.flexibleEqualsIgnoreCase( (Token) iter.next())){
				return true;
			}
		}
	
		return false;
	}

	public boolean containsAllTokensIgnoreCase(TokenList second){
		if (second == null) {
			return false;
		}
		List l = second.getList(); 
	
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			Token t = (Token) iter.next();
			if (!containsTokenIgnoreCase(t)){
				return false;
			}
			if (logger.isDebugEnabled())
				logger.debug ( this + " contains " + t);
		}
	
		return true;
	}
	



	

}

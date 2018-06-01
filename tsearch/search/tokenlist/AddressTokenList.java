/*
 * Created on May 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.tokenlist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.token.AddressAbrev;
import ro.cst.tsearch.search.token.Token;
import ro.cst.tsearch.utils.StringUtils;
import org.apache.log4j.Category;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AddressTokenList extends TokenList{

	/**
	 * Version ID - in order to avoid the compiler warning 
	 */
	private static final long serialVersionUID = 1000000000L;
	
	/**
	 * Main logging class.
	 */	
	protected static final Category logger = Category.getInstance(AddressTokenList.class.getName());
	
	protected List listNo = new ArrayList();        // street number
	protected List listDir = new ArrayList();       // predirectional
	protected List listStName = new ArrayList();    // street name
	protected List listSuffix = new ArrayList();    // street suffix
	protected List listPostDir = new ArrayList();	// postdirectional
	protected List listSecInfo = new ArrayList();	// secondary address designator
	protected List listSecRange = new ArrayList();	// secondary address range 
	protected StandardAddress standardForm;			// standard form for this address
	
	/**
	 * Used only inside function boostrapAddress in order to isolate the 
	 * street name from an address string
	 * @param str
	 */
	public AddressTokenList(String str){
		str = str.replaceAll("(?i)&nbsp;?"," ").trim(); 
		buildList(StringUtils.splitString(str));
		//buildListNew(str);
	}
	
	/*
	private void buildListNew(String str) {
		//listNo       // only set by the constructor that gets the 4 strings, also set by  
		//listDir      // set by buildLists
		//listStName   // set by buildLists
		//listSuffix   // set by buildLists
		//listPostDir  // always empty in old implementation
		//listSecInfo  // always empty in old implementation
		//listSecRange // always empty in old implementation
		StandardAddress standardForm = new StandardAddress(str);
		addStringsToList(standardForm.getAddressElement(StandardAddress.STREET_NUMBER),         listNo,      Token.TYPE_STREET_NO);
		addStringsToList(standardForm.getAddressElement(StandardAddress.STREET_PREDIRECTIONAL), listDir,     Token.TYPE_STREET_DIRECTION);
		addStringsToList(standardForm.getAddressElement(StandardAddress.STREET_NAME),           listStName,  Token.TYPE_STREET_NAME);
		addStringsToList(standardForm.getAddressElement(StandardAddress.STREET_SUFFIX),         listSuffix,  Token.TYPE_STREET_SUFIX);
		addStringsToList(standardForm.getAddressElement(StandardAddress.STREET_POSTDIRECTIONAL),listPostDir, Token.TYPE_STREET_POST_DIRECTIONAL);
		addStringsToList(standardForm.getAddressElement(StandardAddress.STREET_SEC_ADDR_IDENT), listSecInfo, Token.TYPE_STREET_SECONDARY_INFO);
		addStringsToList(standardForm.getAddressElement(StandardAddress.STREET_SEC_ADDR_RANGE), listSecRange,Token.TYPE_STREET_SECONDARY_RANGE);		
	}
	*/
	

	/**
	 * Used by the iterators to create addresses
	 * @param strName
	 * @param directions
	 * @param suffixes
	 * @param strNumber
	 */
	public AddressTokenList (String strName, String directions, String suffixes, String strNumber){
		/*this();*/
		addStreetName(strName);
		addStreetDirections(directions);
		addStreetSuffixes(suffixes);
		addStreetNo(strNumber);
	}

	// used only by constructor that receives several strings
	private void addStreetName(String name){
		List l = StringUtils.splitString(name);
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			Token token = new Token ((String) iter.next(), Token.TYPE_STREET_NAME);
			listStName.add(token);
		}
	}
	
	// used only by constructor that receives several strings
	private void addStreetDirections(String directions){
		List l = StringUtils.splitString(directions);
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			String tokenS = (String) iter.next();
			if (AddressAbrev.isDirection(tokenS)){
				Token token = new Token (tokenS, Token.TYPE_STREET_DIRECTION);
				listDir.add( token);
			}
		}
	}
	
    // used only by constructor that receives several strings
	private void addStreetSuffixes(String suffixes){
		List l = StringUtils.splitString(suffixes);
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			String tokenS = (String) iter.next();
			if (AddressAbrev.isStreetSufix(tokenS)){
				Token token = new Token (tokenS, Token.TYPE_STREET_SUFIX);
				listSuffix.add( token);
			}
		}
	}
	
    //	used only by constructor that receives several strings
	private void addStreetNo(String strNumber){
		Token token = new Token (strNumber, Token.TYPE_STREET_NO);
		listNo.add(token);
	}
	
	private void addStringsToList(String str, List list, int type){
		String[] elements = str.split(" ");
		for(int i=0; i<elements.length; i++){
			list.add(new Token(elements[i],type));
		}
	}
		
	protected void buildList(List tokens) {
		if (logger.isDebugEnabled())
			logger.debug("tokens = " + tokens);
		if ((tokens == null)||((tokens.size() == 0))){
			tokens = Arrays.asList(new String[]{""});
		}
	
		if(tokens.size() == 3) {		// special case
			if(	AddressAbrev.isDirection((String)tokens.get(0))
				&& AddressAbrev.isStreetSufix((String)tokens.get(1))
				&& AddressAbrev.isDirection((String)tokens.get(2)) ) {
					listDir.add(new Token((String)tokens.get(0),Token.TYPE_STREET_DIRECTION));
					listStName.add(new Token((String)tokens.get(1),Token.TYPE_STREET_NAME));
					listSuffix.add(new Token((String)tokens.get(2),Token.TYPE_STREET_SUFIX));
					return;
				}
		}
		
		if(tokens.size() == 2) {
			if(AddressAbrev.isDirection((String)tokens.get(0))
				&& AddressAbrev.isStreetSufix((String)tokens.get(1))) {
					listStName.add(new Token((String)tokens.get(0),Token.TYPE_STREET_NAME));
					listSuffix.add(new Token((String)tokens.get(1),Token.TYPE_STREET_SUFIX));
					return;					
			}
		}
		
		int firstIndex = 0; 
		int lastIndex = tokens.size() -1; 
		
		if (tokens.size()>1){
			if (AddressAbrev.isDirection((String)tokens.get(lastIndex ))){
				if (lastIndex>firstIndex){
					lastIndex --;
				}
			}else if (AddressAbrev.isDirection((String)tokens.get(firstIndex ))){
				if (firstIndex<lastIndex){
					firstIndex++;
				}
			} 
		
			if (AddressAbrev.isStreetSufix((String)tokens.get(lastIndex ))){
				if (lastIndex>firstIndex){
					lastIndex --;
				}
			}
		}
		
		if (firstIndex > lastIndex){ // nu am street name simplu
			firstIndex -- ;
		}

		for (int i = 0; i < firstIndex; i++) {
			Token token = new Token ((String)tokens.get(i), Token.TYPE_STREET_DIRECTION);
			listDir.add( token);
		}
		for (int i = firstIndex; i <= lastIndex; i++) {
			String stname = (String)tokens.get(i);
			stname = stname.replaceAll("#\\d+","").trim();
			if(!StringUtils.isStringBlank(stname)) {
				Token token = new Token (stname, Token.TYPE_STREET_NAME);
				listStName.add( token);				
			}
		}
		for (int i = lastIndex+1; i < tokens.size(); i++) {
			Token token;
			if (AddressAbrev.isDirection((String)tokens.get(i))){ 
				token = new Token ((String)tokens.get(i), Token.TYPE_STREET_DIRECTION);
				listDir.add( token);
			}else{//must be street sufix
				token = new Token ((String)tokens.get(i), Token.TYPE_STREET_SUFIX);
				listSuffix.add( token);
			}
		}
	}
	
	public List getList(){
		List l = new ArrayList();
		
		l.addAll(listNo);
		l.addAll(listDir);
		l.addAll(listStName);
		l.addAll(listSuffix);		
		l.addAll(listPostDir);
		l.addAll(listSecInfo);
		l.addAll(listSecRange);
		
		return l;
	}

	public List getStreetName(){
		return listStName;
	}
	
	public String getStreetNameAsString(){
		return getString(listStName);
	}

	public String getStreetNoAsString(){
		return getString(listNo);
	}

	public List getDirections() {
		return listDir;
	}

	public List getNormalizedDirections() {
		return getNormalizedAbrev(getDirections());
	}

	public List getNormalizedStreetSufixes() {
		return getNormalizedAbrev(getStreetSufixes());
	}

	private List getNormalizedAbrev(List l) {
		List<Token> rez = new ArrayList<Token>();
		for (Iterator iter= l.iterator(); iter.hasNext();)
		{
			Token t = new Token((Token) iter.next());
			t.setString(AddressAbrev.getNormalForm(t.getString()));
			rez.add(t);
		}
		return rez;
	}

	public List getStreetSufixes() {
		return listSuffix;
	}

	public List getStreetNo(){
		return listNo;
	}

	public String toString(){
		String s = "(" + getString(getStreetNo()) + ") " 
				+ "(" + getString(getDirections()) + ") " 
				+ "(" + getString(getStreetName()) + ") " 
				+ "(" + getString(getStreetSufixes()) + ") "
				//+ " StandardAddr =[" +  standardForm + "]"
				; 
		return s;
	}	
}

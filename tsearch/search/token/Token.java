package ro.cst.tsearch.search.token;

import java.io.Serializable;

import org.apache.log4j.Category;

public class Token implements Serializable {
    
    static final long serialVersionUID = 10000000;
	
	public static final int TYPE_DEFAULT = 0 ;
	
	public static final int TYPE_STREET_NAME = 1;
	public static final int TYPE_STREET_NO = 2;
	public static final int TYPE_STREET_SUFIX = 3;
	public static final int TYPE_STREET_DIRECTION = 4;
	
	public static final int TYPE_LAST_NAME = 5;
	public static final int TYPE_FIRST_NAME = 6;
	public static final int TYPE_MIDDLE_NAME = 7;

	public static final int TYPE_STREET_POST_DIRECTIONAL = 8;	// postdirectional
	public static final int TYPE_STREET_SECONDARY_INFO	= 9;	// secondary address info
	public static final int TYPE_STREET_SECONDARY_RANGE = 10;	// secondary range (UNITs)

	protected static final Category logger = Category.getInstance(Token.class.getName());

	
	private String str="";
	private int type=TYPE_DEFAULT;
	

	/**
	 * 
	 */
	public Token(String str , int type) {
		this.str = str;
		this.type = type;
	}

	public Token(String str ) {
		this.str = str;
	}

	public Token(Token t) {
		if (t == null){
			return ;
		}
		this.str = t.getString();
		this.type = t.getType();
	}

	
	/**
	 * @return
	 */
	public String getString() {
		return str;
	}

	/**
	 * @return
	 */
	public int getType() {
		return type;
	}

	/**
	 * @param string
	 */
	public void setString(String string) {
		str = string;
	}

	/**
	 * @param i
	 */
	public void setType(int i) {
		type = i;
	}
	
	public String toString(){
		return "Token("+str+","+ type +")";
	}

	public boolean equals(Object o){
		if (o == this)
			return true;
		if (!(o instanceof Token)){
			return false;
		}
		Token token = (Token) o;
		boolean rez =  (str == null ? token.str == null : str.equals( token.str))  &&
			token.type == type; 
		//logger.debug(this + "=" + o + ":" +  rez);
		return rez;
	}
	
	public  boolean flexibleEqualsIgnoreCase(Token secondT){
		//logger.debug("containsIgnoreCase(" +  main + ",  " +	substring + ")");
		String main = getString();
		if (secondT == null){
			return false;
		}
		String second = secondT.getString();
		if ((main == null) || (second == null)){
			return false;
		}
		main.trim();
		second.trim();

		boolean rez = (main.equalsIgnoreCase(second)); 
		//logger.debug("flexibleEqualsIgnoreCase(" +  main + ",  " +	second + ") : " + rez);
		return rez;
	}
}

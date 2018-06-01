package ro.cst.tsearch.servers;

import java.io.Serializable;
import java.util.Calendar;

import org.apache.log4j.Category;

import ro.cst.tsearch.utils.Log;

/**
 * @author elmarie
 */
public class Cookie implements Serializable, Cloneable {
    
    static final long serialVersionUID = 10000000;

	private static final Category logger = Category.getInstance(Cookie.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + Cookie.class.getName());

	private static final String EMPTY_VALUE = " ";
	private String value = EMPTY_VALUE;
	private Calendar lastAccessTime = Calendar.getInstance();
	private static int timeout = 20; //minutes
	
	
	public String getValue() {
		return value;
	}

	public void setValue(String string) {
		value = string;
		resetTimeoutCounter();
	}
	
	
	public void appendValue(String s){
		String oldValue = getValue();
		if (oldValue.indexOf(s) == -1)
		{
			if (oldValue.length()  >2)
				oldValue += " " + s;
			else 
				oldValue = s;
		}
		setValue(oldValue); 	
	}

	public void resetTimeoutCounter(){
		lastAccessTime = Calendar.getInstance();
		loggerDetails.debug("update lastAccessTime=" + lastAccessTime.getTime());
 	}
	
	public void resetValue(){
		value = EMPTY_VALUE;
	}
	
	public boolean isEmpty(){
		Calendar crtTime = Calendar.getInstance();
		crtTime.add(Calendar.MINUTE, -timeout);
		loggerDetails.debug("lastAccessTime=" + lastAccessTime.getTime());
		loggerDetails.debug("crtTime=" + crtTime.getTime());
		if (crtTime.after(lastAccessTime) ){
			resetValue();
		}
		return isReallyEmpty();
	}
	
	private boolean isReallyEmpty(){
		return (EMPTY_VALUE.equals(value));
	}
	
	public String toString(){
		return  super.toString() + " value =" + value;
	}
}

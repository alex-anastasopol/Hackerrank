/*
 * Created on Apr 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.connection;

import java.util.Calendar;
import java.util.Date;

/**
 * @author 
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ATSCookie {
	private String cookie="";
	private Calendar ctime;
	public ATSCookie(String t)
	{
	    cookie=t;
	    ctime=Calendar.getInstance();
	}
	
	/**
	 * @return
	 */
	public String getCookie() {
		return cookie;
	}

	/**
	 * @param string
	 */
	public void setCookie(String string) {
		cookie = string;
		ctime=Calendar.getInstance();
	}

	/**
	 * @return
	 */
	public Calendar getCtime() {
		return ctime;
	}

	/**
	 * @param calendar
	 */
	public void setCtime(Calendar calendar) {
		ctime = calendar;
	}

}

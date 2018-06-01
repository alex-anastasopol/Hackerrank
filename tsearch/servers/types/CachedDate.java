/*
 * Created on Aug 23, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.servers.types;

import java.util.Calendar;

/**
 * @author cozmin
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CachedDate {
	
	static final long serialVersionUID = 10000000;
	
			private String value;
			private Calendar tstamp;
			public CachedDate(String v,Calendar c)
			{
				value=v;
				tstamp=c;
			}
		
			/**
			 * @return
			 */
			public Calendar getTstamp() {
				return tstamp;
			}

			/**
			 * @return
			 */
			public String getValue() {
				return value;
			}

			/**
			 * @param calendar
			 */
			public void setTstamp(Calendar calendar) {
				tstamp = calendar;
			}

			/**
			 * @param string
			 */
			public void setValue(String string) {
				value = string;
			}

		}



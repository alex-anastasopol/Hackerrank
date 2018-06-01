/*
 * Created on Nov 10, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ro.cst.tsearch.utils;

import java.io.File;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author george
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class BookPageUtil {
	public static String instrumentToPage( Vector fileStringVector ) {
		
		String pageForm = new String();
		Vector vTokens = new Vector();
		int i = 0;
			
		if( fileStringVector.size() != 0){
			
			String fileString = fileStringVector.elementAt(0).toString();
			StringTokenizer st = new StringTokenizer(fileString, "-");
			while(st.hasMoreElements()){
				vTokens.add( i, st.nextToken().toString() );
				i++;  
			}
			
			pageForm = vTokens.elementAt(vTokens.size()-2).toString();
			
		}
		 
		return pageForm;
	}
	public static String pageToInstrum( Vector fileStringVector){
		String instrum = new String();
		
		File fileTotal = (File) fileStringVector.elementAt(0);
		String fileName = fileTotal.getName();
		instrum = fileName.substring(0, fileName.indexOf("-"));
		
		return instrum;
	}

}

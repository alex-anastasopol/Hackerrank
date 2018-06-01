/*
 * Created on Nov 7, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ro.cst.tsearch.fsearch;

import java.io.*;
import java.util.*;

/**
 * @author george
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FileFindRec{

	public static boolean accept(File dir, String fileName, String instrumNumber) {
		return fileName != null && fileName.startsWith(instrumNumber);
	}
	
	public static Vector search(File dir, String intrumNumber) {

		Vector v = new Vector();

		File files[] = dir.listFiles();
		if( files == null ) return v;
		for( int i = 0; i < files.length; i++ ) {
			if( files[i].isFile() ) 
				if( accept(dir, files[i].getName(), intrumNumber ))
					v.add( files[i] );
		}
		
		return v;
	}


}

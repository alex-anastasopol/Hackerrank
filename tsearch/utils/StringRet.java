/*
 * Created on Nov 4, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ro.cst.tsearch.utils;

import java.io.*;
import java.util.*;

/**
 * @author george
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class StringRet {
	public static String getString(ResourceBundle rb, String key) {
			return rb.getString(key).trim();
	}

	static public byte[] getStreamBytes( InputStream in )
									throws IOException	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copyStream ( in, out );
		return out.toByteArray();
	}	
	
	static public int copyStream( InputStream in, OutputStream out )
									throws IOException  {
		int result = 0;
		byte[] buf = new byte[ 8 * 1024 ];
	
		for ( ; ; ) {
			int numRead = in.read( buf );
		
			if ( numRead == -1 )
				break;
		
			out.write( buf, 0, numRead );
			result += numRead;
		}
		
		out.flush();
		return result;
	}			
}
/*
 * Created on Nov 7, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ro.cst.tsearch.fsearch;

import java.io.File;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.utils.StringIntUtil;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author george
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FileFindBookPage {
	
	private static final Category logger = Category.getInstance(FileFindBookPage.class.getName());
	
	private static boolean accept(File dir, String fileName, String page) throws NumberFormatException{
		
		if(StringUtils.isStringBlank(page)) {
			return false;
		}
		
		String pageSubstracted = new String();
		boolean foundFile = false;

		StringTokenizer st1 = new StringTokenizer(fileName,"-");
		Vector partialStr = new Vector();
		while( st1.hasMoreTokens() ){
			partialStr.addElement(st1.nextToken());
		}
		if( partialStr.size() < 4) {
			//=============   nu am instrument number   =================
			//cazul cu doua cratime (pageul este incadrat de doua cratime)
			if( partialStr.size() == 3)  	pageSubstracted = partialStr.elementAt(1).toString();
			if( partialStr.size() == 2) {

				pageSubstracted = partialStr.elementAt(1).toString();
				pageSubstracted = pageSubstracted.substring(0,pageSubstracted.toLowerCase().indexOf(".tif"));
				
				 

			}
		} else {

			//==============   am instrumentNumber   ====================
			pageSubstracted = partialStr.elementAt(2).toString();; 	
		}
		
		String s = StringIntUtil.stringLetStrip(pageSubstracted);
		if(StringUtils.isStringBlank(s)) {
			return false;
		}
		
		return Integer.parseInt(s) == Integer.parseInt(page);
	}
	
	public static Vector search(File dir, String page) throws NumberFormatException{

		Vector v = new Vector();

		File files[] = dir.listFiles();
		try {
			if( files == null ) return v;
			for( int i = 0; i < files.length; i++ ) {
				if( files[i].isFile() ) 
					try{
						if( accept(dir, files[i].getName(), page ))
							v.add( files[i] );
					} catch (Exception e){
						e.printStackTrace();
						logger.error("Error to add file to vector", e);
					}
			}
		} catch (NumberFormatException e){
			logger.error("Error to page string!", e);
		}
		return v;
	}

}

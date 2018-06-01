/*
 * Created on Apr 18, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.titledocument.abstracts;

import java.io.File;
import java.io.FilenameFilter;

import ro.cst.tsearch.titledocument.FakeDocumentsCreator;

/**
 * @author adrian
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class HTMLSourcesFilter implements FilenameFilter {

	/* (non-Javadoc)
	 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
	 */
	public boolean accept(File dir, String FileName) 
	{
	    FileName = FileName.toUpperCase();
	    //get html files, but only those that have not been uploaded
	    //(we will not parse uploaded html files)
		return
		    (
		        ( FileName.endsWith(".HTML") || FileName.endsWith(".HTM") )
		        &&
		        FileName.indexOf( FakeDocumentsCreator.NoParseHTML ) < 0
		    );
	}

}

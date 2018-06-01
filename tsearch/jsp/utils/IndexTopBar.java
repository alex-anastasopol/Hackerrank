/*
 * Created on Oct 28, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.jsp.utils;

import javax.servlet.http.HttpServletRequest;

import ro.cst.tsearch.Search;

/**
 * @author nae
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class IndexTopBar extends TopBar {

	public IndexTopBar(
		Search global,
		String firstTitle,
		String secondTitle,
		String helpChapter,
		String helpFile,HttpServletRequest request) {
		super(global, firstTitle, secondTitle, helpChapter, helpFile,request);
	}
	
	public IndexTopBar(
			Search global,
			String firstTitle,
			String secondTitle,
			String helpChapter,
			String helpFile,HttpServletRequest request,
			int [] whatToShow
			) {
			super(global, firstTitle, secondTitle, helpChapter, helpFile,request,whatToShow);	
			showFileId = true;
		}	
	protected String getHomeHref(long searchId){
		return super.getHomeHref(searchId) + "\" target='_top' ";
	}

}

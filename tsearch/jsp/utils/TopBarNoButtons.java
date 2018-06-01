/*
 * Created on Oct 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.jsp.utils;

import javax.servlet.http.HttpServletRequest;

import ro.cst.tsearch.Search;

/**
 * @author elmarie
 * 
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TopBarNoButtons extends TopBar {

	public TopBarNoButtons(String firstTitle, String secondTitle,
			String helpChapter, String helpFile, HttpServletRequest request) {
		super(firstTitle, secondTitle, helpChapter, helpFile, request);
		setNoButtonsFlag(true);
	}

	public TopBarNoButtons(Search global, String firstTitle,
			String secondTitle, String helpChapter, String helpFile,
			HttpServletRequest request) {
		super(global, firstTitle, secondTitle, helpChapter, helpFile, request);
		setNoButtonsFlag(true);
	}
	
	public TopBarNoButtons(Search global, String firstTitle,
			String secondTitle, String helpChapter, String helpFile,
			HttpServletRequest request,int [] whatToShow) {
		super(global, firstTitle, secondTitle, helpChapter, helpFile, request,whatToShow);
		setNoButtonsFlag(true);
	}

	public TopBarNoButtons(Search global, String firstTitle,
			String secondTitle, HttpServletRequest request) {
		this(global, firstTitle, secondTitle, "", "", request);
	}
	
	public TopBarNoButtons(Search global, String firstTitle,
			String secondTitle, HttpServletRequest request, int [] whatToShow) {
		this(global, firstTitle, secondTitle, "", "", request,whatToShow);
	}

}

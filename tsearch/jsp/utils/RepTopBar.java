/*
 * Created on Oct 28, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.jsp.utils;

import javax.servlet.http.HttpServletRequest;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.AppLinks;

/**
 * @author nae
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class RepTopBar extends TopBar {

	public RepTopBar(
		String firstTitle,
		String secondTitle,
		String helpChapter,
		String helpFile,HttpServletRequest reques) {
		super(firstTitle, secondTitle, helpChapter, helpFile,reques);
	}

	public  RepTopBar(Search global,
			String firstTitle,
			String secondTitle,
			String helpChapter,
			String helpFile,
			boolean noBack,
			boolean noHome,HttpServletRequest request) {
			super(global,firstTitle, secondTitle, helpChapter, helpFile,request);
			setNoBackFlag(noBack);
			//setNoHomeFlag(noHome);
		}
	
	public  RepTopBar(Search global,
			String firstTitle,
			String secondTitle,
			String helpChapter,
			String helpFile,
			boolean noBack,
			boolean noHome,
			String docLink,
			HttpServletRequest request) {
			super(global,firstTitle, secondTitle, helpChapter, helpFile,request);
			setNoBackFlag(noBack);
			//setNoHomeFlag(noHome);
			setDocLink(docLink);
	}
	
	public  RepTopBar(
			String firstTitle,
			String secondTitle,
			String helpChapter,
			String helpFile,
			boolean noBack,
			boolean noHome,HttpServletRequest request) {
			super(firstTitle, secondTitle, helpChapter, helpFile,request);
			setNoBackFlag(noBack);
			//setNoHomeFlag(noHome);
		}
	public  RepTopBar(
			String firstTitle,
			String secondTitle,
			String helpChapter,
			String helpFile,
			boolean noBack,
			boolean noHome,
			boolean noPrint,
			HttpServletRequest request) {
			super(firstTitle, secondTitle, helpChapter, helpFile,request);
			setNoBackFlag(noBack);
			//setNoHomeFlag(noHome);
			setPrinterFlag(noPrint);
		}
	
	public  RepTopBar(
			Search global,
			String firstTitle,
			String secondTitle,
			String helpChapter,
			String helpFile,
			boolean noBack,
			boolean noHome,
			boolean noPrint,
			HttpServletRequest request) {
			super(global,firstTitle, secondTitle, helpChapter, helpFile,request);
			setNoBackFlag(noBack);
			if(noHome) setNoHomeFlag(noHome);
			setPrinterFlag(noPrint);
		}
	
	public RepTopBar(
		Search global,
		String firstTitle,
		String secondTitle,
		String helpChapter,
		String helpFile,HttpServletRequest request) {
		super(global, firstTitle, secondTitle, helpChapter, helpFile,request);
	}
	
	
	protected String getHomeHref(long searchId){
		return AppLinks.getRepHomeHref(searchId);
	}
	
	public String toString(long searchId){
		return super.toString(searchId);
	}

}

package ro.cst.tsearch.jsp.utils;

import javax.servlet.http.HttpServletRequest;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.AppLinks;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.utils.URLMaping;

public class OrderTSTopBar extends TopBar {

    public OrderTSTopBar(String firstTitle, String secondTitle, String helpChapter, String helpFile,HttpServletRequest request) {
        super(firstTitle, secondTitle, helpChapter, helpFile,request);
        
        setNoBackFlag(true);        
        setNoHomeFlag(true);
    }

    public OrderTSTopBar(Search global, String firstTitle, String secondTitle, String helpChapter, String helpFile,HttpServletRequest request) {
        super(global, firstTitle, secondTitle, helpChapter, helpFile,request);
        
        setNoBackFlag(true);        
        setNoHomeFlag(true);
    }

    protected String getHomeHref(long searchId) {
        return AppLinks.getBackToOrderPageHref(searchId);
    }
    
    public static String getHeader(Search global, String pageTitle){
		String rez = getBroserWindowTitle(global, pageTitle) + "\n";
		rez += getHeaderMetaNoCache();
		rez +="<LINK media=screen href='"+URLMaping.STYLESHEETS_DIR+"/default.css' type=text/css rel=stylesheet>\n";
		return rez;
	}
    
    public static String getBroserWindowTitle(Search global, String pageTitle){
		String fileno ="";
		if (global != null) {
			SearchAttributes sa = global.getSa();
			fileno = sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
		}
		return getBrowserWindowTitle(fileno, pageTitle);
	}
    
    public static String getBrowserWindowTitle(String fileno, String pageTitle) {
		String rez =  "<title>Advanced Title Search: " + pageTitle;
		rez += " [" + fileno +"]";
		return rez + "</title>";
	}
    
    public static String getTitle(Search global, String pageTitle){
		String fileno ="";
		if (global != null) {
			SearchAttributes sa = global.getSa();
			fileno = sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
		}
		String rez =  "<font class=frameTitleFont>  " + "Advanced Title Search&trade;: " + pageTitle;
		if (fileno.length() > 0){
			rez += " [" + fileno +"]";
		}
		return rez + "</font>  ";
	}
    
    public  String toString(){
		
        long searchId = Search.SEARCH_NONE;
		if (global != null){
			searchId = global.getSearchID();
		}

		String jsHelp = "<SCRIPT language=JavaScript>\n"
			+ "var chapter = '" + helpChapter +"';\n"
			+ "var helpfile = '" + helpFile + "';\n"
			+ "</SCRIPT>\n";
		 
		 String title = firstTitle;
		 if (!secondTitle.equals("")){
		 	title += "-"+secondTitle;  
		 }

		String 	s = "<TABLE height=19 cellSpacing=0 cellPadding=0 width='100%' border=0>" 
              			+ "<TBODY><TR>"
						+ "<TD vAlign=center noWrap align=left>" 
						+ getTitle(global, title)
						+ "  " + extraText
						+ "</TD>"
						+ "<TD vAlign=center noWrap align=right> ";
		if (!noButtons){
			if (!noHome)			
			s +=  getHomeLink( searchId);
			if (prn) {
				s +=  getPrintLink();
				s +=  getEmailLink(firstTitle, secondTitle, searchId);
			}
			s +=  getHelpLink();
			if (!noBack)
				s+= getCloseLink();
		}
		s +=   "    </TD>"
			+ "  </TR></TBODY></TABLE>";
		return jsHelp + s;
	}

}
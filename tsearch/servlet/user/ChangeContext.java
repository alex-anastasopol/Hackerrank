/*
 * Text here
 */

package ro.cst.tsearch.servlet.user; 

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.community.CategoryAttributes;
import ro.cst.tsearch.community.CategoryUtils;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityFilter;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;


public final class ChangeContext extends  BaseServlet {

  /**
     * Build the newitem page for discussions 
     * 
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are producing
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
	protected static final Category logger= Category.getInstance(ChangeContext.class.getName());
	
    /*public void doGet(HttpServletRequest request,
	HttpServletResponse response)
	throws IOException, ServletException{
		doRequest(request, response);
	}*/
	
    public void doRequest(HttpServletRequest request,
			  HttpServletResponse response)
	throws IOException, ServletException {
	ParameterParser pp = new ParameterParser(request);
	//String user_id = pp.getStringParameter(UserAttributes.USER_ID, "");
	int op_code = pp.getIntParameter(TSOpCode.OPCODE, TSOpCode.SELECT_COMM);
	
	//String retPage = pp.getStringParameter(VPOParameters.RET_PAGE, "");

	response.setContentType("text/html");	
	//UserAttributes ua = InstanceManager.getCurrentInstance().getCurrentUser();

	/*if(retPage.indexOf(VPOPages.getContextPath() + JspConstants.USERS_BASEPATH) != -1){
	    retPage = VPOPages.getAbsoluteURL(VPOPages.SETTINGS_WNEW);
	}else if(retPage.indexOf(JspConstants.DISCUSSIONS_BASEPATH) != -1){
	    retPage = VPOPages.getContextPath() + JspConstants.DISCUSSIONS_PAGE_MAIN;
	}else if(retPage.indexOf(JspConstants.DOCUMENTS_BASEPATH) != -1){
	    retPage = VPOPages.getContextPath() + JspConstants.DOCUMENTS_PAGE_MAIN;
	}*/
	HttpSession session=request.getSession(true);
	User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
	Search global= (Search) currentUser.getSearch( request);		
	
	long searchId = global.getSearchID();
	
	String htmlTemplate="<html>"
	+"<title>Title Search - Available Groups With Included Communities</title>"
	+ "<head>" + "<LINK media=screen href=\""+ URLMaping.STYLESHEETS_DIR + "/default.css\" type=text/css rel=stylesheet>" +
			"<link rel='shortcut icon' href='/title-search/favicon.ico' type='image/x-icon'>"+
	        "<link rel='icon' href='/title-search/favicon.ico' type='image/x-icon'></head>"
	+ "<body bgcolor=\"#FFFFFF\">"
	+ "<form name=\"chgCtx\" method=\"POST\"  >"
	+ "<input type=\"hidden\" name=\""+ TSOpCode.OPCODE +"\" value=\""+ op_code +"\" >"
	+ "<input type=\"hidden\" name=\""+ UserAttributes.USER_ID +"\" value=\""+ /*user_id +*/"\" >"
	+ "<input type=\"hidden\" name=\""+ CommunityAttributes.COMMUNITY_ID +"\" value=\""+ "" +"\" >"
	+ "<input type=\"hidden\" name=\""+ RequestParams.SEARCH_ID +"\" value=\""+ "" +"\" >"	
	+ "<table border=\"1\" cellpadding=\"1\" cellspacing=\"0\" width=\"100%\">"
	+ "<tr>"
	+ 		"<td align=\"center\" class=\"headerSubDetailsRow\">"
	+			"&nbsp;&nbsp;Your communities are:&nbsp;&nbsp;"			 
	+ 		"</td>"	
	+		"currentContext"			
	+ "</tr>"
	+ "</table>"
	+ "</form>"
	+ "</body>"
	+"</html>";
    PrintWriter out = response.getWriter();
    out.println(htmlTemplate.replaceAll("currentContext", getContext(searchId)));
    
	}    
  
  
   public static String getContext(long searchId){
	//CurrentInstance ci = InstanceManager.getCurrentInstance();
	//UserAttributes crtUser = ci.getCurrentUser();
	String context = "";
	//String userId = ((BigDecimal)crtUser.getAttribute (UserAttributes.ID)).toString();
		try{
	
			CategoryAttributes[] categVisible = CategoryUtils.getCategories(CategoryAttributes.CATEGORY_NAME);
			
			for(int i=0; i< categVisible.length ; i++){
				CommunityFilter cf = new CommunityFilter();
				CommunityAttributes[] comm = CommunityUtils.getCommunitiesInCategory(categVisible[i].getID(),cf);
				context = context + "<tr><td class=\"headerRowLeft\">&nbsp;" + categVisible[i].getNAME() + "(" + "<font color=\" \">" + comm.length + "</font> " + " communit" + ( ((new Integer(comm.length)).intValue()==1)?"y":"ies" ) +" )&nbsp;"+"</td></tr>\n";
				for(int j=0;j<comm.length;j++){
					String action ="javascript:window.document.forms.chgCtx."
					+ CommunityAttributes.COMMUNITY_ID + ".value="
					+ comm[j].getID()
					+ ";window.document.forms.chgCtx."
					+ RequestParams.SEARCH_ID + ".value=" 
					+ searchId
					+ ";window.document.forms.chgCtx.action='"+ URLMaping.path + URLMaping.RedirectContext+ "';window.document.forms.chgCtx.target=window.opener.name;window.document.forms.chgCtx.submit();window.close();";		
					context = context+ "<tr><td class=\"row1\">" + "<a href=\"" + action + "\">"+ comm[j].getNAME() +"</a></td></tr>";
				}
			}
		}catch(Exception e){
			logger.error("Error to get communities context!" + e.getMessage());	
		}
	return context;  	
   }
   
}


package ro.cst.tsearch.servlet.user;

import java.io.IOException;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.AppLinks;
import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;



public class LoginDispacher extends BaseServlet {

	private static final long serialVersionUID = 1L;
	
	public void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {

  		String pdfFile = request.getParameter("pdfFile");
  		if( pdfFile == null ){
  			pdfFile = "";
  		}
  		
  		String displayPdfFile = request.getParameter("displayPdfFile");
  		if( displayPdfFile == null ){
  			displayPdfFile = "";
  		}
  		
  		String agentId = request.getParameter("agentId");
  		if( agentId == null ){
  			agentId = "";
  		}
  		
  		String findExternal = request.getParameter(RequestParams.REPORTS_FIND_EXTERNAL);
  		String tsrSearchString = request.getParameter(RequestParams.REPORTS_SEARCH_TSR);
		
		ParameterParser pp = new ParameterParser(request);
        int commID = pp.getIntParameter(RequestParams.USER_COMMUNITYID, -1);
        HttpSession session = request.getSession(true);
        
        boolean isAgent = false;        
        try {
	        User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
	        isAgent = UserUtils.isAgent(currentUser.getUserAttributes());
        } catch (Exception e) {
        	
        }
        
        if (commID != -1 && isAgent) {
        	CommunityProducts.fillComunityProducts();
      		if ((!"".equals( pdfFile ) || !"".equals( displayPdfFile )))
      			sendRedirect(request, response, URLMaping.path + URLMaping.PDF_SHOW, "?pdfFile=" + pdfFile + "&displayPdfFile=" + displayPdfFile + "&agentId=" + agentId);
      		else
      			forward(request, response, URLMaping.OrderTSPage);
            
            session.setAttribute("OrderSearch", Boolean.TRUE);
        } else{
        	HttpSession currentSession = request.getSession( true );
        	
        	currentSession.setAttribute( "loggedIn" , new Boolean(true));
        	
        	
        	User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
      		Search global 	= currentUser.addNewFakeSearch();
      		request.setAttribute(RequestParams.SEARCH_ID,global.getID()+"");
        	long searchId  	= global.getID();
      		try{
      			InstanceManager.getManager().getCurrentInstance(searchId).setup(currentUser, request, response, session);
      			InstanceManager.getManager().getCurrentInstance(searchId).setCurrentCommunity(CommunityUtils.getCommunityFromId(currentUser.getUserAttributes().getCOMMID().longValue()));
      			InstanceManager.getManager().getCurrentInstance(searchId).setup(currentUser, request, response, session);
      		}
      		catch(Exception e){
      			e.printStackTrace();
      		}
      		
      		
      		InstanceManager.getManager().getCurrentInstance(searchId).setCrtSearchContext(global);  
      		CommunityProducts.fillComunityProducts();
      		
      		/*
      		 * if we have a SSL connection, perform a redirect to normal HTTP
      		 */
      		
      		if( request.isSecure() ){
      			/* here we have a secure connection (HTTPS) */
      			
      			/* get app URL */
      			String appUrl = ServerConfig.getAppUrl();
      			if(appUrl.isEmpty()) {
      				appUrl = "http://localhost:8080";
      			}
      			
      			
          		if ((!"".equals( pdfFile ) || !"".equals( displayPdfFile ))) {
          			sendRedirect(request, response, URLMaping.path + URLMaping.PDF_SHOW, "?pdfFile=" + pdfFile + "&displayPdfFile=" + displayPdfFile + "&agentId=" + agentId);
          		} else if(StringUtils.isNotEmpty(tsrSearchString) && StringUtils.isNotEmpty(findExternal)){
          			session.setAttribute("searchIdHome",searchId);
          			response.sendRedirect( appUrl + URLMaping.path +  encodeUrl(
            				AppLinks.getHomepage(searchId, isAgent, currentUser.getUserAttributes().getID().longValue())));
          		} else {
            		session.setAttribute("searchIdHome",searchId);
            		response.sendRedirect( appUrl + URLMaping.path +  encodeUrl(
            				AppLinks.getHomepage(searchId, isAgent, currentUser.getUserAttributes().getID().longValue())));
            	}
      		}
      		else{
      			/* normal connection */
      			
         		if ((!"".equals( pdfFile ) || !"".equals( displayPdfFile ))) {
          			sendRedirect(request, response, URLMaping.path + URLMaping.PDF_SHOW, "?pdfFile=" + pdfFile + "&displayPdfFile=" + displayPdfFile + "&agentId=" + agentId);
         		} else if(StringUtils.isNotEmpty(tsrSearchString) && "1".equals(findExternal)) {
         			session.setAttribute("searchIdHome",searchId);
//         			String pageToGo = URLMaping.REPORT_SEARCH 
//         					+ "?" + RequestParams.SEARCH_ID + "=" + searchId
//         					+ "&" + RequestParams.REPORTS_SEARCH_TSR + "=" + tsrSearchString
//         					+ "&" + RequestParams.REPORTS_SEARCH_ALL + "=on";
         			//forward(request, response, AppLinks.getRepHomeHref(searchId, tsrSearchString));
         			forward(request, response, 
            				AppLinks.getHomepage(searchId, isAgent, currentUser.getUserAttributes().getID().longValue()));
         		} else {
            		session.setAttribute("searchIdHome",searchId);
            		forward(request, response, 
            				AppLinks.getHomepage(searchId, isAgent, currentUser.getUserAttributes().getID().longValue()));
            	}
      		}
        	
        }
    }
}
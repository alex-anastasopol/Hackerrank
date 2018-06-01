package ro.cst.tsearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.LoadConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.loadBalServ.ServerInfoSingleton;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.URLMaping;

/**
 * @author nae
 */
public class Logout extends BaseServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    public void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {

        try {

            int commID = -1;
            try {
                commID = Integer.parseInt(request.getParameter(RequestParams.USER_COMMUNITYID));
            } catch (Exception ignored) {}
            
            HttpSession session = request.getSession(true);
            session.invalidate();
            
            
            response.sendRedirect(ServerConfig.getAppMainUrl() + URLMaping.path + URLMaping.LOGIN_PAGE + 
                    (commID != -1 ? 
                            "?" + RequestParams.USER_COMMUNITYID + "=" + commID
                            : "")
                    );
            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
package ro.cst.tsearch.servlet.user;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.bean.AppLinks;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

import org.apache.log4j.Category;

public class RedirectContext extends BaseServlet {

	protected static final Category logger= Category.getInstance(RedirectContext.class.getName());
    /**
     *  Only TSAdmin users can change the community
     */
    public void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {

        ParameterParser pp = new ParameterParser(request);
        int op_code = pp.getIntParameter(TSOpCode.OPCODE, 0);
        String commId = pp.getStringParameter(CommunityAttributes.COMMUNITY_ID);
        long searchId = pp.getLongParameter(RequestParams.SEARCH_ID);

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute(SessionParams.CURRENT_USER);
        request.getParameter("searchId");
        if (user != null) {

            UserAttributes userAttributes = user.getUserAttributes();
            
            try {

                if ((op_code == TSOpCode.SELECT_COMM || op_code == TSOpCode.CHANGE_CONTEXT) 
                        &&(UserUtils.isTSAdmin(userAttributes)||UserUtils.isTSCAdmin(userAttributes))) {
                    CommunityAttributes ca = CommunityUtils.getCommunityFromId(Long.parseLong(commId));
                    session.setAttribute(CommunityAttributes.COMMUNITY_ID, new BigDecimal(commId));
                    InstanceManager.getManager().getCurrentInstance(searchId).setCurrentCommunity(ca);
                    InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().setAgent(null);
                    user.setUserAgentClient(null);
                }

            } catch (Exception e) {
                logger.error("Change Community:" + e.getMessage());
            }
        }

        response.sendRedirect(URLMaping.path + encodeUrl(AppLinks.getHomepage(searchId,false,-1)));
    }
}
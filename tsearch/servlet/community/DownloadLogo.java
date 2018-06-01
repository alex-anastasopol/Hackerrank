package ro.cst.tsearch.servlet.community;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.ParameterParser;

public class DownloadLogo extends HttpServlet {

    public void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        
        ParameterParser pp = new ParameterParser(request);
        String commId = pp.getStringParameter(CommunityAttributes.COMMUNITY_ID, "-1");
        request.getParameterMap();
        byte[] logo = null;

        try{
        if ("-1".equals(commId))
            logo = CommunityUtils.getCommLogo( CommunityUtils.getCommunityFromId(((BigDecimal)request.getSession().getAttribute(CommunityAttributes.COMMUNITY_ID)).longValue()));
        else {
            try {
                logo = CommunityUtils.getCommLogo(new BigDecimal(commId));
            } catch (Exception e) {
                logo = CommunityUtils.getCommLogo(CommunityUtils.getCommunityFromId(((BigDecimal)request.getSession().getAttribute(CommunityAttributes.COMMUNITY_ID)).longValue()));
            }
        }
        }
        catch(Exception e){
        	e.printStackTrace();
        }

        if (logo != null) {

            response.setContentLength(logo.length);
            ServletOutputStream sout = response.getOutputStream();
            sout.write(logo, 0, logo.length);
            sout.flush();
            sout.close();
        }

    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doRequest(request, response);
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doRequest(request, response);
    }

}
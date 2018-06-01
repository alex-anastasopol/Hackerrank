package ro.cst.tsearch.servlet.user;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Random;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.math.NumberUtils;

import ro.cst.tsearch.data.User;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MultipartParameterParser;
import ro.cst.tsearch.utils.ParameterNotFoundException;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.TSParameters;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.user.UserManagerI;

/**
 * @author nae
 */
public class UserDispacher extends BaseServlet {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
		
        MultipartParameterParser mpp = null;
        ParameterParser pp = null;
        int opCode = 0;
        Hashtable<String, String> hashd = null;
        String typePage = "0";
        long searchId = -2;

        String contentType = request.getContentType();
        if (contentType != null && contentType.indexOf("multipart/form-data") > -1) {
            mpp = new MultipartParameterParser(request);
            opCode = mpp.getMultipartIntParameter(TSOpCode.OPCODE);
            typePage = encode(mpp.getMultipartStringParameter("typePage", "0"));
            searchId = mpp.getMultipartLongParameter(RequestParams.SEARCH_ID);
        } else {
            pp = new ParameterParser(request);
            opCode = pp.getIntParameter(TSOpCode.OPCODE);
            searchId = pp.getLongParameter(RequestParams.SEARCH_ID);
            typePage = encode(pp.getStringParameter("typePage", "0"));
        }
        String ret = "OK";
        String showHidden = "no";
        UserManagerI userManager = com.stewart.ats.user.UserManager.getInstance();
        if(pp != null){
            showHidden = encode(pp.getStringParameter(RequestParams.SHOW_HIDDEN, "no"));
        } else if(mpp != null){
        	showHidden = encode(mpp.getMultipartStringParameter(RequestParams.SHOW_HIDDEN, "no"));
        }
        RequestDispatcher view;
        switch (opCode) {
        case TSOpCode.USER_ADD_APPLY:
            ret = UserManager.manipulateUser(pp, mpp, TSOpCode.USER_ADD_APPLY,searchId);
            response.sendRedirect(URLMaping.path + URLMaping.USER_ADD 
                    + "?" + RequestParams.SEARCH_ID + "=" + searchId
                    + "&" + TSOpCode.OPCODE + "=" + TSOpCode.USER_ADD_VIEW
                    + "&ret=" + ret);
            break;
        case TSOpCode.USER_ADD_SUBMIT:
            ret = UserManager.manipulateUser(pp, mpp, TSOpCode.USER_ADD_SUBMIT,searchId);
            response.sendRedirect(URLMaping.path + URLMaping.USER_LIST 
                    + "?" + RequestParams.SEARCH_ID + "=" + searchId
                    + "&" + TSOpCode.OPCODE + "=" + TSOpCode.USER_LIST_VIEW
                    + "&" + RequestParams.SHOW_HIDDEN + "=" + showHidden
                    + "&ret=" + ret);
            break;
        case TSOpCode.USER_DEL:
            hashd = pp.getSetParameter(TSParameters.CBX_LIST);
            ret = UserManager.deleteUser(hashd);            
            response.sendRedirect(URLMaping.path + URLMaping.USER_LIST 
                    + "?" + RequestParams.SEARCH_ID + "=" + searchId
                    + "&" + TSOpCode.OPCODE + "=" + TSOpCode.USER_LIST_VIEW
                    + "&" + RequestParams.SHOW_HIDDEN + "=" + showHidden
                    + "&ret=" + ret);            
            break;
        case TSOpCode.USER_HIDE:
            hashd = pp.getSetParameter(TSParameters.CBX_LIST);
            
            try {
				userManager.getAccess();
				if(userManager.hideShowUsers(true, hashd.values())) {
					ret = "OK";
				}
			} catch (Throwable t) {
				logger.error("Error while hiding users!", t);
			} finally {
				userManager.releaseAccess();
			}
            //ret = UserManager.hideUser(hashd);
            response.sendRedirect(URLMaping.path + URLMaping.USER_LIST 
                    + "?" + RequestParams.SEARCH_ID + "=" + searchId
                    + "&" + TSOpCode.OPCODE + "=" + TSOpCode.USER_LIST_VIEW
                    + "&" + RequestParams.SHOW_HIDDEN + "=" + showHidden
                    + "&ret=" + ret);            
            break;  
        case TSOpCode.USER_UNHIDE:
            hashd = pp.getSetParameter(TSParameters.CBX_LIST);
           
            try {
				userManager.getAccess();
				if(userManager.hideShowUsers(false, hashd.values())) {
					ret = "OK";
				}
			} catch (Throwable t) {
				logger.error("Error while Unhiding users!", t);
			} finally {
				userManager.releaseAccess();
			}
            
            //ret = UserManager.unHideUser(hashd);
            response.sendRedirect(URLMaping.path + URLMaping.USER_LIST 
                    + "?" + RequestParams.SEARCH_ID + "=" + searchId
                    + "&" + TSOpCode.OPCODE + "=" + TSOpCode.USER_LIST_VIEW 
                    + "&" + RequestParams.SHOW_HIDDEN + "=" + showHidden
                    + "&ret=" + ret);        	
        	break;
        case TSOpCode.USER_EDIT_APPLY :
        case TSOpCode.MY_PROFILE_EDIT_APPLY :
            ret = UserManager.manipulateUser(pp, mpp, TSOpCode.USER_EDIT_APPLY,searchId);
           if(opCode == TSOpCode.MY_PROFILE_EDIT_APPLY && ret.equals("OK") ) {
                HttpSession session = request.getSession(true);
                        UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
                        User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
                        currentUser.setUserAttributes(ua);
            }
            response.sendRedirect(URLMaping.path + URLMaping.USER_EDIT 
                    + "?typePage=" + typePage 
                    + "&" + UserAttributes.USER_ID + "=" + encode(mpp.getMultipartStringParameter(UserAttributes.USER_LOGIN)) 
                    + "&" + RequestParams.SEARCH_ID + "=" + searchId 
                    + "&update=" + new Random().nextInt()                     
                    + "&" + TSOpCode.OPCODE + "=" 
                    + (opCode == TSOpCode.USER_EDIT_APPLY ? TSOpCode.USER_EDIT : TSOpCode.MY_PROFILE_EDIT)
                    + "&ret=" + ret);
            break;
/*            
        case TSOpCode.USER_EDIT_SUBMIT :
        case TSOpCode.MY_PROFILE_EDIT_SUBMIT :
            ret = UserManager.manipulateUser(pp, mpp, TSOpCode.USER_EDIT_SUBMIT,searchId);
            if(opCode == TSOpCode.MY_PROFILE_EDIT_SUBMIT && ret.equals("OK") ) {
                HttpSession session = request.getSession(true);
                        UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
                        User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
                        currentUser.setUserAttributes(ua);
            }
            if (typePage.equals("1")) {
                UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
                response.sendRedirect(URLMaping.path + URLMaping.USER_VIEW 
                        + "?" + RequestParams.SEARCH_ID + "=" + searchId                        
                        + "&typePage=" + typePage 
                        + "&update=" + new Random().nextInt()
                        + "&" + UserAttributes.USER_ID + "=" + encode(ua.getLOGIN()) 
                        + "&" + RequestParams.SEARCH_ID + "=" + searchId 
                        + "&" + TSOpCode.OPCODE + "=" 
                        + (opCode == TSOpCode.USER_EDIT_SUBMIT ? TSOpCode.USER_VIEW : TSOpCode.MY_PROFILE_VIEW)
                        + "&ret=" + ret );
            } else
                response.sendRedirect(URLMaping.path + URLMaping.USER_LIST 
                        + "?" + RequestParams.SEARCH_ID + "=" + searchId                        
                        + "&" + TSOpCode.OPCODE + "=" + TSOpCode.USER_LIST_VIEW
                        + "&ret=" + ret );

            break;
 */           
        case TSOpCode.MY_ATS_EDIT_APPLY :
            ret = UserManager.setMyATSAttributes(pp, mpp,searchId);
           if(opCode == TSOpCode.MY_ATS_EDIT_APPLY && ret.equals("OK") ) {
                HttpSession session = request.getSession(true);
                        UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
                        User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
                        currentUser.setUserAttributes(ua);
            }
            response.sendRedirect(URLMaping.path + URLMaping.MY_ATS_EDIT 
                    + "?typePage=" + typePage 
                    + "&" + UserAttributes.USER_ID + "=" + encode(pp.getStringParameter(UserAttributes.USER_ID)) 
                    + "&" + RequestParams.SEARCH_ID + "=" + searchId 
                    + "&update=" + new Random().nextInt()                     
                    + "&" + TSOpCode.OPCODE + "=" 
                    + (opCode == TSOpCode.MY_ATS_EDIT_APPLY ? TSOpCode.MY_ATS_EDIT : TSOpCode.MY_ATS_EDIT)
                    + "&ret=" + ret);
            break;
        case TSOpCode.MY_ATS_EDIT_SUBMIT :
        	ret = UserManager.setMyATSAttributes(pp, mpp,searchId);
            if(opCode == TSOpCode.MY_ATS_EDIT_SUBMIT && ret.equals("OK") ) {
                		HttpSession session = request.getSession(true);                			
                        UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
                        User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
                        if(pp.getBigDecimalParameter(UserAttributes.USER_ID).equals(currentUser.getUserAttributes().getID()))
                        currentUser.setUserAttributes(ua);
            }
            
            UserAttributes ua = null;
            try {
            	ua = UserUtils.getUserFromId(pp.getBigDecimalParameter(UserAttributes.USER_ID));
            }catch(BaseException e) {
            	ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
            }
                response.sendRedirect(URLMaping.path + URLMaping.MY_ATS_VIEW 
                        + "?" + RequestParams.SEARCH_ID + "=" + searchId                        
                        + "&typePage=" + typePage 
                        + "&update=" + new Random().nextInt()
                        + "&" + UserAttributes.USER_ID + "=" + encode(ua.getLOGIN()) 
                        + "&" + RequestParams.SEARCH_ID + "=" + searchId 
                        + "&" + TSOpCode.OPCODE + "=" 
                        + (opCode == TSOpCode.MY_ATS_EDIT_SUBMIT ? TSOpCode.MY_ATS_VIEW : TSOpCode.MY_ATS_VIEW)
                        + "&ret=" + ret );
            break;
        case TSOpCode.USER_EDIT_DELPHOTO :
        case TSOpCode.USER_EDIT_DELPHOTO_MYPROFILE :
        	ret = UserManager.manipulateUser(pp, mpp, TSOpCode.USER_EDIT_DELPHOTO,searchId);
            response.sendRedirect(URLMaping.path + URLMaping.USER_EDIT 
                    + "?typePage=" + typePage 
                    + "&" + UserAttributes.USER_ID + "=" + encode(mpp.getMultipartStringParameter(UserAttributes.USER_LOGIN)) 
                    + "&" + RequestParams.SEARCH_ID + "=" + searchId 
                    + "&update=" + new Random().nextInt()                     
                    + "&" + TSOpCode.OPCODE + "=" 
                    + (opCode == TSOpCode.USER_EDIT_DELPHOTO ? TSOpCode.USER_EDIT : TSOpCode.MY_PROFILE_EDIT)
                    + "&ret=" + ret);
        	break;
        case TSOpCode.USER_EDIT_DELRESUME :
        case TSOpCode.USER_EDIT_DELRESUME_MYPROFILE :
        	ret = UserManager.manipulateUser(pp, mpp, TSOpCode.USER_EDIT_DELRESUME,searchId);
            response.sendRedirect(URLMaping.path + URLMaping.USER_EDIT 
                    + "?typePage=" + typePage 
                    + "&" + UserAttributes.USER_ID + "=" + encode(mpp.getMultipartStringParameter(UserAttributes.USER_LOGIN)) 
                    + "&" + RequestParams.SEARCH_ID + "=" + searchId 
                    + "&update=" + new Random().nextInt()                     
                    + "&" + TSOpCode.OPCODE + "=" 
                    + (opCode == TSOpCode.USER_EDIT_DELPHOTO ? TSOpCode.USER_EDIT : TSOpCode.MY_PROFILE_EDIT)
                    + "&ret=" + ret);
        	break;
        case TSOpCode.CHANGE_PASSWORD_VIEW :
        	setPageAttributes(request, mpp, pp, typePage, searchId);
        	view = request.getRequestDispatcher("/jsp/Users/changePassword.jsp");
        	view.forward(request, response);
        	break;
        case TSOpCode.CHANGE_PASSWORD:
        	setPageAttributes(request, mpp, pp, typePage, searchId);
        	ret = UserManager.manipulateUser(pp, mpp, TSOpCode.CHANGE_PASSWORD,searchId);
        	view = request.getRequestDispatcher("/jsp/Users/changePassword.jsp");
        	request.setAttribute("passwordJustChanged", ret);
//        	response.setHeader("Pragma", "no-cache");
//        	response.setIntHeader("Expires", -1);
//        	response.setHeader("Cache-control", "no-cache");
        	view.forward(request, response);
        }
        
    }

	private void setPageAttributes(HttpServletRequest request,MultipartParameterParser mpp, ParameterParser pp,String typePage, long searchId)
			throws ParameterNotFoundException, IOException {
        String contentType = request.getContentType();
        String userId = null;
        if (contentType != null && contentType.indexOf("multipart/form-data") > -1) {
        	userId = mpp.getMultipartStringParameter(UserAttributes.USER_ID, "");
        }else{
        	userId = pp.getStringParameter(UserAttributes.USER_ID);	
        }	
		request.setAttribute("userID", userId);
		request.setAttribute("opCode", TSOpCode.CHANGE_PASSWORD);
		request.setAttribute("searchId", searchId);
		request.setAttribute("typePage", typePage);
		User currentUser = (User) request.getSession().getAttribute(SessionParams.CURRENT_USER);
//		request.setAttribute("currentUser", currentUser);
		UserAttributes crtUser = currentUser.getUserAttributes();
		boolean isTSAdmin = false;
			try {
				isTSAdmin = UserUtils.isTSAdmin (crtUser);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		boolean isCommAdminJsp = (crtUser.getGROUP().intValue()==GroupAttributes.CA_ID)||(crtUser.getGROUP().intValue()==GroupAttributes.CCA_ID);
		
		if (!(isTSAdmin || isCommAdminJsp) && !userId.equals(crtUser.getLOGIN())) {
		   	userId = crtUser.getLOGIN();
		} else if (isCommAdminJsp&(!(crtUser.getGROUP().intValue()==GroupAttributes.CCA_ID))) {
			UserAttributes someUser = null;
			if(NumberUtils.isNumber(userId)) {
				someUser = UserManager.getUser(new BigDecimal(userId));
			}
			if(!NumberUtils.isNumber(userId) || someUser == null) {
		   	 	someUser = UserManager.getUser(userId, false);
			}
		  	if (crtUser.getCOMMID().longValue() != someUser.getCOMMID().longValue())
		 		userId = crtUser.getLOGIN();
		}
	}
}
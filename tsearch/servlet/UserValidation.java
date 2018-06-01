package ro.cst.tsearch.servlet;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.performanceMonitor.LoggedUsersMonitor;
import ro.cst.tsearch.servers.parentsite.Company;
import ro.cst.tsearch.titledocument.TSDManager;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.IndividualUserLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SecurityUtils;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.reuse.ClipboardAts;

/**
 * @author nae
 */

public class UserValidation {
	
	private static final Category logger = Logger.getLogger(UserValidation.class);
	private static final long BLOCK_USER_INTERVAL = 1000 * 60 * ServerConfig.getBlockUserInterval(10); //in miliseconds
	public static final String LAST_ACCESS_PARAM = "LastAccessTime";
//	the parameter name which identify the server in jsp or the user name
	public static final String PRM_USER_NAME = "usr";
//	the parameter name which identify the server in jsp or the password}
	private static final String PRM_USER_PASS = "pswd";
	public static final String PRM_CODED = "coded";
	
	public static boolean validate(HttpServletRequest request, HttpServletResponse response, String sRealPath)
	throws IOException,BaseException {
		StringBuilder tempBufferReason = new StringBuilder();
		return validate(request, response, sRealPath, tempBufferReason, false);
	}
	
	static final Map<String, Integer> usersTable = new HashMap<String, Integer>();
	
	public static boolean validate(HttpServletRequest request, HttpServletResponse response, String sRealPath, StringBuilder tempBufferReason, boolean leaveCurrentInstanceAlone)
		throws IOException,BaseException {

		HttpSession session = request.getSession(true);
		
		User currentUser = null;
		boolean bSuccess = false;
		
		try {
			currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
			//lastAcces = (Long) session.getAttribute(LAST_ACCESS_PARAM);
			if(currentUser != null) {
				if(!User.getActiveUsers().containsKey(currentUser.toString())) {
					User.getLogger().error("Session had something in current user " + 
								currentUser.getUserAttributes().getLOGIN() + 
								" but ATS does not contain the user " + currentUser.toString());
					
					Collection users = User.getActiveUsers().values();
					for (Object savedObject : users) {
						if(savedObject instanceof User) {
							User savedUser = (User)savedObject;
							if(savedUser.getUserAttributes().getLOGIN().equals(currentUser.getUserAttributes().getLOGIN())) {
								User.getLogger().error("Found in Active User same login as  " + 
										currentUser.getUserAttributes().getLOGIN() + " but with code " + savedUser.toString());
							}
							if(savedUser.getMySeqNumber() == currentUser.getMySeqNumber()) {
								User.getLogger().error("Found in Active User same myseqnumber  " + savedUser.getMySeqNumber() + 
										" with savedUser login " + currentUser.getUserAttributes().getLOGIN() + 
										" with currentUser login " + currentUser.getUserAttributes().getLOGIN() + " but with code " + savedUser.toString());
							}
						}
					}
					
					
					session.setAttribute(LAST_ACCESS_PARAM, new Long(System.currentTimeMillis()));
					String userAgent = request.getHeader("User-Agent");
    				currentUser.setUserAgent(userAgent);
					currentUser.forceUser(session);
					
					IndividualUserLogger.infoDebug("Session had something in current user " + currentUser.getUserAttributes().getLOGIN() + 
							" but ATS does not contain the user " + currentUser.toString(), currentUser);
					
				} else if(currentUser.getHttpSession() == null) {
					User.getLogger().error("Current user " + 
							currentUser.getUserAttributes().getLOGIN() + "(" + currentUser.toString() + ")" +
							" appears logged and active but has no session ");
					
					IndividualUserLogger.infoDebug("Current user " + 
							currentUser.getUserAttributes().getLOGIN() + "(" + currentUser.toString() + ")" +
							" appears logged and active but has no session ", currentUser);
				}
				
			}
		} catch (Exception e) {
			logger.error(" Session expired!", e);
		}
		
        try
        {
        	long searchId =-400;
        	String searchIdStr = request.getParameter("searchId");
        	try{
        	searchId = Long.parseLong(searchIdStr);
        	}
        	catch(Exception e){
        		//e.printStackTrace();
        	}
        	if(searchId!=-400){
	    		if (currentUser == null) { // nu era logat
	    			final String login = request.getParameter(PRM_USER_NAME);
	    			boolean accountBlocked = false;
	    			if(!StringUtils.isEmpty(login)){
	    				synchronized (usersTable) {
	    					try{ accountBlocked = (usersTable.get(login) == 3);}catch(Exception e){}
						}
	    			}
	    			
	    			if (  !accountBlocked && ((currentUser = authenticate(request, sRealPath, tempBufferReason)) != null) )    {
	    				if(!StringUtils.isEmpty(login)){
	    					synchronized (usersTable) {
	    						usersTable.remove(login);
	    					}
	    				}
	    				session.setAttribute(LAST_ACCESS_PARAM, new Long(System.currentTimeMillis()));
	    				String userAgent = request.getHeader("User-Agent");
	    				currentUser.setUserAgent(userAgent);
	    				session.setAttribute(SessionParams.CURRENT_USER, currentUser);
	    				
	    				if(!User.getActiveUsers().containsKey(currentUser.toString())) {
	    					
	    					User.getLogger().error("Session just added current user " + 
	    								currentUser.getUserAttributes().getLOGIN() + 
	    								" but ATS does not contain the user in ActiveList() " + currentUser.toString());
	    					IndividualUserLogger.infoDebug("Session just added  current user " + currentUser.getUserAttributes().getLOGIN() + 
	    							" but ATS does not contain the user in ActiveList() " + currentUser.toString(), currentUser);
	    					
	    				}
	    				
	    				session.setAttribute(CommunityAttributes.COMMUNITY_ID, currentUser.getUserAttributes().getCOMMID());
	    				synchronized(session){
	    					session.setAttribute(SessionParams.CLIPBOARD_ATS, new ClipboardAts());
	    				}
	    				LoggedUsersMonitor.getInstance().setMaxInactiveInterval( session.getMaxInactiveInterval() );
	    				
	    				if(searchId!=-1){
		    				InstanceManager.getManager().getCurrentInstance(searchId).setup(currentUser, request, response, session);
		    				Search search = (Search) currentUser.getSearch(request);
		    				if(search!=null){
		    					InstanceManager.getManager().getCurrentInstance(search.getSearchID()).setup(currentUser, request, response, session);
		    					InstanceManager.getManager().getCurrentInstance(search.getID()).setup(currentUser, request, response, session);
		    				}
	    				}
	    				UserUtils.updateLastLogin(currentUser.getUserAttributes(), System.currentTimeMillis());
	    				currentUser.updateLastAccess(request);
	    				bSuccess = true;
	    			} else {
	    				if(accountBlocked){
	    					tempBufferReason.delete(0, tempBufferReason.length());
	    					tempBufferReason.append(RequestParamsValues.ERR_NO_ACCOUNT_BLOCKED);
	    				}
	    				else{
		    				if(!StringUtils.isEmpty(login)){
		    					synchronized ( usersTable ) {
		    						int count = 0;
		    						try{count = usersTable.get(login);}catch(Exception e){};
		    						if( count < 3){
		    							count ++;
		    						}
		    						if(count == 3){
		    							Timer t = new Timer();
		    							t.schedule(new TimerTask(){
											public void run() {
												synchronized ( usersTable ) {
													usersTable.remove(login);
												}
											}
		    							}, BLOCK_USER_INTERVAL);
		    						}
		    						usersTable.put(login, count);
								}
		    				}
	    				}
	    				bSuccess = false;
	    			}
	    			
	    		} else { //era deja logat
	    			session.setAttribute(LAST_ACCESS_PARAM, new Long(System.currentTimeMillis()));
	    			LoggedUsersMonitor.getInstance().setMaxInactiveInterval( session.getMaxInactiveInterval() );
	    			
	    			synchronized(session){
	    				if(session.getAttribute(SessionParams.CLIPBOARD_ATS)==null){
	    					session.setAttribute(SessionParams.CLIPBOARD_ATS, new ClipboardAts());
	    				}    					
    				}
	    			
	    			bSuccess = true;
	    			if(searchId!=-1 && !leaveCurrentInstanceAlone){
		    			InstanceManager.getManager().getCurrentInstance(searchId).setup(currentUser, request, response, session);
		    			Search search = (Search) currentUser.getSearch(request);
	    				if(search!=null){
	    					if(search.getID() != searchId && search.getID() != Search.SEARCH_NONE) {
	    						InstanceManager.getManager().getCurrentInstance(search.getSearchID()).setup(currentUser, request, response, session);
	    					}
	    				}
	    			}
	    			currentUser.updateLastAccess(request);
	    		}
        	}
        	else{
        		if (currentUser == null) { // nu era logat
    	    		
	    			if ((currentUser = authenticate(request, sRealPath, tempBufferReason)) != null) {
	    				
	    				session.setAttribute(LAST_ACCESS_PARAM, new Long(System.currentTimeMillis()));
	    				
	    				
	    				String userAgent = request.getHeader("User-Agent");
	    				currentUser.setUserAgent(userAgent);
	    				session.setAttribute(SessionParams.CURRENT_USER, currentUser);
	    				
	    				if(!User.getActiveUsers().containsKey(currentUser.toString())) {
	    					
	    					User.getLogger().error("Session just added current user " + 
	    								currentUser.getUserAttributes().getLOGIN() + 
	    								" but ATS does not contain the user in ActiveList() " + currentUser.toString());
	    					IndividualUserLogger.infoDebug("Session just added  current user " + currentUser.getUserAttributes().getLOGIN() + 
	    							" but ATS does not contain the user in ActiveList() " + currentUser.toString(), currentUser);
	    					
	    				}
	    				
	    				session.setAttribute(CommunityAttributes.COMMUNITY_ID, currentUser.getUserAttributes().getCOMMID());
	    				synchronized(session){
	    					session.setAttribute(SessionParams.CLIPBOARD_ATS, new ClipboardAts());
	    				}
	    				LoggedUsersMonitor.getInstance().setMaxInactiveInterval( session.getMaxInactiveInterval() );
	    				UserUtils.updateLastLogin(currentUser.getUserAttributes(), System.currentTimeMillis());
	    				
		    			currentUser.updateLastAccess(request);
	    				
	    				bSuccess = true;
	    			} else {
	    				bSuccess = false;
	    			}
	    			
	    		} else { //era deja logat
	    			session.setAttribute(LAST_ACCESS_PARAM, new Long(System.currentTimeMillis()));
	    			LoggedUsersMonitor.getInstance().setMaxInactiveInterval( session.getMaxInactiveInterval() );
	    			
	    			currentUser.updateLastAccess(request);
	    			bSuccess = true;
	    		}
        	}
        }
        catch( IllegalStateException ise )
        {
            ise.printStackTrace();
        }


		return bSuccess;
	}

	private static User authenticate(HttpServletRequest request, String sSitePath, StringBuilder tempBufferReason) {
		
		try{
			String coded = request.getParameter(PRM_CODED);
			String login = request.getParameter(PRM_USER_NAME);
			String passwd = request.getParameter(PRM_USER_PASS);
			
			if(coded!=null && coded.equals("1")){
				login = SecurityUtils.getInstance().decrypt(login);
				passwd = SecurityUtils.getInstance().decrypt(passwd);
			} 
			 
			if((StringUtils.isEmpty(login) || login.equals("null")) && (StringUtils.isEmpty(passwd) || passwd.equals("null"))){
				logger.info("Invalid Login! Session Expired!");
				tempBufferReason.delete(0, tempBufferReason.length());
				tempBufferReason.append(RequestParamsValues.ERR_NO_INVALID_SESSION);
				String searchIdStr = request.getParameter("searchId");
				if (searchIdStr==null) {
					searchIdStr = request.getParameter("viewDescrSearchId");
				}
				if (searchIdStr!=null) {
					long searchId =-1;
					try {
						searchId = Long.parseLong(searchIdStr);
					} catch (NumberFormatException nfe) {
						searchId =-1;
					}
					if (searchId!=-1) {
						SearchLogger.info("<br>Your session expired! " + SearchLogger.getTimeStampAndLocation(searchId) + "<br>", searchId);
		        	}
				}
	        	return null;
			}
			
			UserAttributes userAttributes = UserManager.getUser(login, passwd);
			
			if (userAttributes != null) {
				if(userAttributes.isINTERACTIVE()){
					UserAttributes agentAttributes = UserManager.getUser(userAttributes.getAGENTID());
					Company company = UserManager.getCompany(userAttributes.getCOMPANYID());
					
					User currentUser = new User(getDirPath(sSitePath));
					userAttributes.setUserLoginIp(request.getRemoteHost());
					//aici se face initializarea userului...			
					currentUser.setUserAttributes(userAttributes);
					if ( !UserUtils.isTSAdmin(currentUser.getUserAttributes()) ) {
						
						/* Agent log */
						/*
						try {
							String logContent = "";
				    		StringWriter sw = new StringWriter();
					        PrintWriter pw = new PrintWriter(sw);
					        new Throwable().fillInStackTrace().printStackTrace(pw);
							logContent += "========= DEBUG INFO (ValidateInputs.java) ========"+ "\n";
							logContent += " Setting user agent client for " + currentUser.getUserAttributes().getID() + " (" + currentUser.getUserAttributes().getLOGIN() + ") \n";
							logContent += " UserAgentClient = " + agentAttributes.getLOGIN() + " (" + currentUser.getUserAttributes().getAGENTID() + ") " + "\n";;
							logContent += " Request = " + request.getRequestURI() + "\n";
							logContent += " RequestQueryString = " + request.getQueryString() + "\n";
							logContent += " Stack trace = " + sw.toString() + "\n";
							logContent += "========= END DEBUG INFO ========";
							System.out.println(logContent);
						}catch(Exception e) {}
						*/
						/* Agent log */
						
						currentUser.setUserAgentClient(agentAttributes);					
					}
					currentUser.setCompany(company);
					currentUser.setHostName(request.getRemoteHost());
					IndividualUserLogger.initLog(currentUser);
					
					return currentUser;
				}
			}
			tempBufferReason.delete(0, tempBufferReason.length());
			tempBufferReason.append(RequestParamsValues.ERR_NO_INVALID_LOGIN);
			
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public static String getDirPath(String sSitePath) {
		
	    //deleteOldSearches(sSitePath);
		
	    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	    
		String sToday = String.valueOf(calendar.get(Calendar.YEAR));
		sToday += "_" + String.valueOf(calendar.get(Calendar.MONTH) + 1);
		sToday += "_" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));		
		
		return sSitePath + TSDManager.TSDDir + File.separator + sToday + File.separator;		
	}
	
	@SuppressWarnings("unused")
	private static void deleteOldSearches(String sSitePath) {
	    
	    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		calendar.add(Calendar.DATE, -1);
		
		String sYesterday = String.valueOf(calendar.get(Calendar.YEAR));
		sYesterday  += "_" + String.valueOf(calendar.get(Calendar.MONTH) + 1);
		sYesterday  += "_" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
				
		String filesFolderPath = sSitePath + TSDManager.TSDDir + File.separator + sYesterday + File.separator;
		File filesFolder = new File(filesFolderPath);
		
		String[] fileList = filesFolder.list();
		if (fileList != null && fileList.length > 1) { 
		    
			for (int i = 0; i < fileList.length; i++) {

				try {
				    
					if( !fileList[i].endsWith(".zip") ){
					
					    long searchId = Long.parseLong(fileList[i]);
					    if (!DBManager.haveSearchContext(searchId) && 
					            SearchManager.getSearch(searchId, false) == null) {
					        
					        FileUtils.deleteDir(new File(filesFolderPath + fileList[i]));
					        File zipFile = new File( filesFolderPath + fileList[i] + ".zip" );
					        if( zipFile.exists() ){
					        	zipFile.delete();
					        }
					        logger.debug("Folder [" + filesFolderPath + fileList[i] + "] was deleted.");
					        
					    } else {
					        logger.debug("The context [" + filesFolderPath + fileList[i] + "] will not be deleted .");    
					    }
					    
					}
				    
				} catch (Exception ignored) {
					ignored.printStackTrace();
				    FileUtils.deleteDir(new File(filesFolderPath + fileList[i]));
				    logger.debug("Folder [" + filesFolderPath + fileList[i] + "] was deleted.");
				}
			}
		}
	}
	
}

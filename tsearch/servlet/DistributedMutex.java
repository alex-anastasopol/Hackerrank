package ro.cst.tsearch.servlet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Category;
import org.mortbay.util.Credential.MD5;

import ro.cst.tsearch.connection.ATSConn;
import ro.cst.tsearch.connection.ATSConnConstants;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DBManager.SearchAvailabitily;
import ro.cst.tsearch.loadBalServ.ServerInfoEntry;
import ro.cst.tsearch.templates.UpdateTemplates;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.SearchReserved;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

/*
 * Extends the search availability routine to multiple servers
 */
public class DistributedMutex extends HttpServlet{
	private static final long serialVersionUID = -5586345077335164523L;

	private final static int ONE_REQUEST_TIME_OUT = 15000;
	
	private final static int SLEEP_TIME = 5000;

	private final static int MAX_RETRIES = 100;
	
	//remote user that requests a search status
	public static final String remoteUserParam = "userId";
	
	//remote user credentials ( md5(password))
	public static final String credentialsParam = "credentials";
	
	//search id for the search that we need status info
	public static final String searchIdParam = "searchId";
	
	//operation
	public static final String operation = "operation";
	
	//policy name
	public static final String POLICY_NAME = "policyName";
	
	//policy date
	public static final String POLICY_DATE = "policyDate";
	
	//community id
	public static final String COMM_ID = "commId";
	
	//search available
	public static final String availableAndLocked = "true";
	
	//search not available
	public static final String notAvailable = "false";
	
	//check if search is available
	public static final int OP_CHECK_AVAILABLE = 1;
	
	//unlock previously locked search
	public static final int OP_UNLOCK = 2;
	
	//propagate template to remote server
	public static final int OP_PROPAGATE_TEMPLATE = 3;

	private static final Category logger = Category.getInstance(DistributedMutex.class.getName());
	
	//config
    private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	
    public static Object mutexLock = new Object();
    
	public void service( HttpServletRequest req, HttpServletResponse res ){
		
		long searchId = -1;
		long userId = -1;
		String credentials = "";
		int currentOperation = 0;
		
		PrintWriter output = null;
		
		try{
			output = res.getWriter();
		}
		catch( Exception e ){
			//no writer, return
			e.printStackTrace();
			return;
		}
		
		//get current operation
		try{
			currentOperation = Integer.parseInt( req.getParameter( operation ) );
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
		if(currentOperation == OP_PROPAGATE_TEMPLATE) {
			//get policy name and date
			final String polName = req.getParameter(POLICY_NAME);
			final String polDate = req.getParameter(POLICY_DATE);
			final String commId = req.getParameter(COMM_ID);

			Thread t = new Thread() {
				@Override
				public void run() {
					updateTemplate(polName, polDate, Integer.parseInt(commId));
				}
			};
			t.start();
			
			return;
		}
		
		//get the searchId from request
		try{
			searchId = Long.parseLong( req.getParameter( searchIdParam ) );
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
		logger.error( "Remote Check for search ID:" + searchId );
		
		//get user id from request
		try{
			userId = Long.parseLong( req.getParameter( remoteUserParam ) );
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
		//get user credentials from request
		credentials = req.getParameter( credentialsParam );
		
		//authenticate the user
		if( !authenticate( userId, credentials ) ){
			
			//not authenticated , print false
			output.println( notAvailable );
			
			return;
		}
		
		//user authenticated, we can continue
		
		switch( currentOperation ){
		case OP_CHECK_AVAILABLE:
			
			/*
			 * checks availability for a given search
			 * uses test and set operation, if search is available it will be locked for the requesting user
			 */
			SearchAvailabitily searchAvailable = DBManager.checkAvailability(searchId,userId, DBManager.CHECK_AVAILABLE, true, false);
			
			if( searchAvailable.status != DBManager.SEARCH_AVAILABLE ){
				//search not available
				output.println( notAvailable );
			}
			else{
				//search available, lock it for the requesting user
				//DBManager.setSearchOwner(searchId, userId);
				
				//return available
				output.println( availableAndLocked );
			}
			break;
		case OP_UNLOCK:
			
			/*
			 * unlocks the given search
			 * if the search was already locked on at least one server, we must unlock it on all others 
			 */

			synchronized( DistributedMutex.mutexLock ){
				SearchReserved.getInstance().clearReserved( searchId );
			}
			break;
		}
	}
	
	/**
	 * authenticates an user
	 * @param userId - user id for the user we want to authenticate
	 * @param md5Credentials - md5(password) for the user we want to authenticate
	 * @return true if and only if this user and password are valid
	 * 			false otherwise
	 */
	private boolean authenticate( long userId, String md5Credentials ){
		boolean validUser = false;

		try{
			String originalDigest = MD5.digest( UserManager.getEncodedPassword(userId) );
			
			validUser = originalDigest.equals( md5Credentials );
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
		return validUser;
	}

	/**
	 * checks if a search is available on the other servers
	 * @param userId - current user id
	 * @param searchId - search
	 * @return true if search is available on all other servers, false otherwise
	 */
	public static boolean remoteCheckAvailability( long userId, long searchId ){
		
		boolean isAvailableOnAll = true;
		
		try{
		
			int port = 80; //default port
			
			//get application port from config file
			try{
				String applicationUrl = rbc.getString( "app.url" );
				
				applicationUrl = applicationUrl.replace( "http://" , "");
				
				int idx = applicationUrl.lastIndexOf( ":" );
				if( idx >= 0 ){
					port = Integer.parseInt( applicationUrl.substring( idx + 1 ) );
				}
			}
			catch( Exception e ){
				e.printStackTrace();
			}
			
			//get the servers list
			ServerInfoEntry[] serversList = getServersList(false);
			
			if( serversList == null ){
				return true;
			}
			
			//list of server where search was locked(necessary if at least one server has the search not available)
			Vector<String> availableAndLockedServers = new Vector<String>();
			//for each server in list
			for( int j = 0 ; j < serversList.length ; j ++ ){
				
				if(isAvailableOnServer( serversList[j].getIp() + ( port != 80 ? ( ":" + port ) : "" ), userId, searchId )){
					//search available and locked on this server, add it to the list
					availableAndLockedServers.add( serversList[j].getIp() );
				}
				else{
					//not available on this server, break
					isAvailableOnAll = false;
					break;
				}
			}
			
			//if not available on all, unlock it on the servers where we locked it
			if( !isAvailableOnAll ){
				Iterator<String> serverIterator = availableAndLockedServers.iterator();
				while( serverIterator.hasNext() ){
					String server = serverIterator.next();
					
					unlockSearchOnServer( server, userId, searchId );
				}
			}
		}
		catch( Exception e ){
			isAvailableOnAll = false;
			e.printStackTrace();
		}
		return isAvailableOnAll;
	}
	
	/**
	 * gets the list from the config file
	 * @return
	 */
	private static ServerInfoEntry[] getServersList(boolean includeDisabledServers){
		ServerInfoEntry[] allServers = null;
				
		//get servers from database
		try{
			allServers = DBManager.getServersNoLoad(includeDisabledServers);
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
		if( allServers == null || allServers.length == 0 ){
			return null;
		}
		
		return allServers;
	}
	
	private static void unlockSearchOnServer( String server, long userId, long searchId ){
		try{
	        HashMap<String,String> reqprops = new HashMap<String,String>();
	        reqprops.put("Accept","image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, */*");
	        reqprops.put("Accept-Language","en-us");
	        reqprops.put("Connection","Keep-Alive");
	        reqprops.put("Host", server);
	        reqprops.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
	        
	        String userCredentials = "";
	        
	        try{
	        	userCredentials = MD5.digest( UserManager.getEncodedPassword(userId) );
	        }
	        catch( Exception e ){
	        	e.printStackTrace();
	        }
	        
	        String getPage = "/title-search/DistributedMutex";

	        int dumyServerID =-1;  //just for old connection
	        ATSConn c = new ATSConn(666, getPage, ATSConnConstants.GET, null, reqprops,searchId,dumyServerID);
	        c.setUsefastlink(true);
			c.setFollowRedirects(true);
			c.setEncQuerry( remoteUserParam		+ "=" + userId + "&" +
	        					searchIdParam		+ "=" + searchId + "&" +
	        					credentialsParam	+ "=" + userCredentials + "&" +
	        					operation			+ "=" + OP_UNLOCK );
			c.doConnection();
			String result = c.getResult().toString();
		}
		catch( Exception e2 ){
			e2.printStackTrace();
		}
	}
	
	private static boolean isAvailableOnServer( String server, long userId, long searchId ){
		
		boolean isAvailableOnServer = true;
		
		try{
	        HashMap<String,String> reqprops = new HashMap<String,String>();
	        reqprops.put("Accept","image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, */*");
	        reqprops.put("Accept-Language","en-us");
	        reqprops.put("Connection","Keep-Alive");
	        reqprops.put("Host", server);
	        reqprops.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
	        
	        String userCredentials = "";
	        
	        try{
	        	userCredentials = MD5.digest( UserManager.getEncodedPassword(userId) );
	        }
	        catch( Exception e ){
	        	e.printStackTrace();
	        }
	        
	        String getPage = "/title-search/DistributedMutex";
	        int dumyServerID =-1;  //just for old connection
	        ATSConn c = new ATSConn(666, "http://" + server + getPage, ATSConnConstants.GET, null, reqprops,searchId,dumyServerID);
	        logger.error( "Remote check http://" + server + getPage );
	        c.setUsefastlink(true);
			c.setFollowRedirects(true);
			c.setTimeout( ONE_REQUEST_TIME_OUT );
			c.setEncQuerry( remoteUserParam		+ "=" + userId + "&" + 
						searchIdParam		+ "=" + searchId + "&" +
						credentialsParam	+ "=" + userCredentials + "&" +
						operation			+ "=" + OP_CHECK_AVAILABLE );
			logger.error( "Remote check params: " + remoteUserParam		+ "=" + userId + "&" + 
					searchIdParam		+ "=" + searchId + "&" +
					credentialsParam	+ "=" + userCredentials + "&" +
					operation			+ "=" + OP_CHECK_AVAILABLE);
			c.doConnection();
			String result = c.getResult().toString();
			if( c.getReturnCode() == 200 ){
				if( !result.contains( availableAndLocked ) ){
					isAvailableOnServer = false;
				}
			}
		}
		catch( Exception e2 ){
			e2.printStackTrace();
		}
		
		return isAvailableOnServer;
	}
	
	public static void propagateTemplate(String policyName, String policyDate, int commId) {
		try {
			int port = 80; //default port
			//get application port from config file
			String applicationUrl = "";
			try{
				applicationUrl = rbc.getString( "app.url" );
				applicationUrl = applicationUrl.replace( "http://" , "");
				
				int idx = applicationUrl.lastIndexOf( ":" );
				if( idx >= 0 ){
					port = Integer.parseInt( applicationUrl.substring( idx + 1 ) );
				}
			} catch( Exception e ){
				e.printStackTrace();
			}
			
			//get the servers list
			ServerInfoEntry[] serversList = getServersList(true);
			if( serversList == null ){
				return;
			}
			
			for(int j = 0; j < serversList.length; j++) {
				if(!serversList[j].getIp().equals(applicationUrl.replaceFirst(":.*", ""))) {
					propagateTemplateToServer(serversList[j].getIp() + (port != 80 ? (":" + port) : ""), policyName, policyDate, commId);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void propagateTemplateToServer(String server, String policyName, String policyDate, int commId) {
		try{
	        HashMap<String,String> reqprops = new HashMap<String,String>();
	        reqprops.put("Accept","image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, */*");
	        reqprops.put("Accept-Language","en-us");
	        reqprops.put("Connection","Keep-Alive");
	        reqprops.put("Host", server);
	        reqprops.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
	        
	        String getPage = "/title-search/DistributedMutex";

	        int dumyServerID = -1;  //just for old connection
	        ATSConn c = new ATSConn(1, "http://" + server + getPage, ATSConnConstants.GET, null, reqprops, -1, dumyServerID);
	        c.setUsefastlink(true);
			c.setFollowRedirects(true);
			c.setEncQuerry(operation + "=" + OP_PROPAGATE_TEMPLATE + "&"
					+ DistributedMutex.POLICY_NAME + "=" + StringUtils.urlEncode(policyName) + "&"
					+ DistributedMutex.POLICY_DATE + "=" + StringUtils.urlEncode(policyDate) + "&"
					+ DistributedMutex.COMM_ID + "=" + commId);
			c.doConnection();
			logger.debug("Request for updating template " + policyName + " was sent");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private static void updateTemplate(String policyName, String policyDate, int commId) {
		int retryCount = 0;
		String sDate = "str_to_date( '" + policyDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
		String sql = "select count(*) as total from " + DBConstants.TABLE_COMMUNITY_TEMPLATES
			+ " where " + DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME + " = ? and "
			+ DBConstants.FIELD_COMMUNITY_TEMPLATES_LAST_UPDATE + " = " + sDate + " and "
			+ DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID + " = ?";

		try {
			// repeat until the template arrives in DB
			while(retryCount < MAX_RETRIES) {
				int checkForFile = DBManager.getSimpleTemplate().queryForInt(sql, policyName, commId);
	
				if(checkForFile > 0) {
					break;
				}
	
				Thread.sleep(SLEEP_TIME);
				retryCount++;
			}
		} catch (Exception e) {
			logger.error("Error updating the template " + policyName, e);
			return;
		}
		if(retryCount == MAX_RETRIES) {
			logger.error("Template not found in database: " + policyName);
			return;
		}

		final ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
		String templatePath = rbc.getString("templates.path");

		ArrayList<String> templatesToBeCompiled = new ArrayList<String>();
		templatesToBeCompiled.add(policyName);

		UpdateTemplates.updateTemplates(templatePath, new Long(commId), templatesToBeCompiled);
		logger.debug("Template updated successfully: " + policyName);
	}
	
	public static long random( long m, long n){
		long a=Math.max(m, n);
		long b=Math.min(m, n);
				
		return Math.round(Math.random()*(a-b))+b;		
	}
	
	public static void backOff( int attempt ){
		
		try{
			 long waittime = attempt * random( 1, 5 ) * 1000;
			logger.info("Backing OFF  "+waittime+   "attempt = "+attempt);
			Thread.sleep( waittime );
		}
		catch( InterruptedException ie ){
			ie.printStackTrace();
		}
		
	}
}
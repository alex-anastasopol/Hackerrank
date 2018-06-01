package ro.cst.tsearch.data; 
import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.performanceMonitor.LoggedUsersMonitor;
import ro.cst.tsearch.servers.parentsite.Company;
import ro.cst.tsearch.servlet.UserValidation;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.IndividualUserLogger;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;

public class User implements HttpSessionBindingListener, Serializable {
    
	private static final Logger logger = Logger.getLogger(User.class);
	
    static final long serialVersionUID = 10000000;

	public static final int NEW_SEARCH = -1;

	private String msStoreZonePath;	
	private UserAttributes userAttributes = null; 
	private UserAttributes userAgentClient = null; // default agent that is stored in database
	private Company company = null;
	private String hostName = "N/A";

	private transient HttpSession userSession = null;
	/**
	 * This field should be set on user login and should contain the UserAgent from that moment
	 */
	private transient String userAgent = null;
	private transient String browserVersion = null;
	
	public static User fakeUser;
	
	private long lastAccess = 0;
	
	private static int seq = 0;	
	protected synchronized static int getSeq(){
		return seq++;
	}
	
	
	private long mySeqNumber;
	
	static {
	   fakeUser = new User("");
	   fakeUser.setUserAttributes(new UserAttributes());
	}

	public User(String sStoreZonePath) {
		msStoreZonePath = sStoreZonePath;
		
		File userPath = new File(getUserPath());
		if (!userPath.exists())
		    userPath.mkdirs();
		
		mySeqNumber = getSeq();
		
	}
	
	public long getMySeqNumber() {
		return mySeqNumber;
	}

//	public boolean equals(Object o){
//		if(o==null  ){
//			return false;
//		}
//		if (o instanceof User) {
//			User user= (User) o;
//			return user.msStoreZonePath.equals(this.msStoreZonePath);
//		}
//		return false;
//	}
	
	public String getUserPath() {
		return msStoreZonePath;
	}
	
	/**
	 * Updates last access time param to current timp
	 *
	 */
	public void updateLastAccess(HttpServletRequest request){
		lastAccess = System.currentTimeMillis();
		
		if (request != null){
			IndividualUserLogger.infoDebug("The user is going to: " + request.getRequestURL() + " searchId: " + request.getParameter("searchId"), this);
		}
	}
	
	public void setLastAccess(long newtime ) {
		lastAccess = newtime;
	}
	
	
	/**
	 * 
	 * @return lastAccess param
	 */
	public long getLastAcces(){
		return lastAccess;
	}
	
	/**
	 * 
	 * @return time in milliseconds that this user has been idle
	 */
	public long getIdleTime(){
		return System.currentTimeMillis() - lastAccess;
	}
	
	/**
	 * 
	 * @return time in seconds that this user has been idle
	 */	
	public long getIdleSeconds(){
		return (System.currentTimeMillis() - lastAccess) / 1000;
	}
	
	///////////////////////////////////////////////////////	
	public Search getSearch(HttpServletRequest request) {
	    return SearchManager.getSearch(request);
	}

	public void setSearch(Search newSearch) {
	    SearchManager.setSearch(newSearch, this);  
	}
	
	public static Search getSearchFromSearchId( long searchId ){
		return SearchManager.getSearch(searchId);
	}
	
	public Search getSearch(long searchId) {
	    return SearchManager.getSearch(searchId);
	}

	public Search addNewSearch(boolean interactive) {
		return SearchManager.addNewSearch(this, interactive);
	}

	public Search addNewFakeSearch(){
		
		return SearchManager.addNewFakeSearch(this);
		
	}
	///////////////////////////////////////////////////////
	public void setUserAttributes(UserAttributes userAttributes) {
		this.userAttributes = userAttributes;
	}

	public UserAttributes getUserAttributes() {
		return this.userAttributes;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public Company getCompany() {
		return this.company;
	}
		
	public UserAttributes getUserAgentClient() {
		return userAgentClient;
	}

	public void setUserAgentClient(UserAttributes attributes) {
		try {
			logger.debug("SetUserAgentClient for: " + this.getUserAttributes().getID() + " agent: " + (attributes!=null?attributes.getID():"null agent") + "\n");
		}catch(Exception e) {
			logger.error("setUserAgentClient problem", e);
		}
		userAgentClient = attributes;
	}

	public String getHostName() {
	    return hostName;
	}
	
	public void setHostName(String hostName) {
	    this.hostName = hostName;
	}

	////////////////////////////////////////
	// SessionListener
	////////////////////////////////////////
	private static final Hashtable activeUserList = new Hashtable();
	private static void addActiveUser(User user) {
	    activeUserList.put(user.toString(), user);
	    activeUserList.put(user.toString() + "_time", new Long(System.currentTimeMillis()));
	    
	    IndividualUserLogger.infoDebug("addActiveUser ; activeUserList: " + prettyPrintActiveUsers(), user);
	    
	    LoggedUsersMonitor.getInstance().addActiveUser( user );
	}
	
	/**
	 * 
	 * @param user user to kick out
	 * 
	 * doest not call LoggedUsersMonitor
	 * called only from IdleUsersKickerThread to increment kicked user count
	 */
	public static void kickActiveUser( User user ){
	    activeUserList.remove(user.toString());
	    activeUserList.remove(user.toString() + "_time");
	    
	    IndividualUserLogger.infoDebug("kickActiveUser  ; activeUserList: " + prettyPrintActiveUsers(), user);
	    IndividualUserLogger.finishLog(user.toString().replaceAll("(?i)[^@]+@", "@"));
	}
	
	private static void removeActiveUser(User user) {
	    activeUserList.remove(user.toString());
	    activeUserList.remove(user.toString() + "_time");
	    
	    IndividualUserLogger.infoDebug("removeActiveUser  ; activeUserList: " + prettyPrintActiveUsers(), user);
	    IndividualUserLogger.finishLog(user.toString().replaceAll("(?i)[^@]+@", "@"));
	    
	    LoggedUsersMonitor.getInstance().removeActiveUser( user );
	}
	public static Hashtable getActiveUsers() {
	    return activeUserList;
	}
	private static void listActiveUsers() {
	    
	    logger.info("*****************************************************");
	    Collection users = activeUserList.values();
	    for (Iterator iterator = users.iterator(); iterator.hasNext();) {
			
            User activeUser = (User) iterator.next();
            logger.info(activeUser.getUserAttributes().getUserFullName() 
                    + "[" + activeUser + "]");
	    }
	    logger.info("*****************************************************");
	}
	
	private static String prettyPrintActiveUsers() {
	    StringBuffer list = new StringBuffer();
	    
	    Collection users = activeUserList.values();
	    for (Iterator iterator = users.iterator(); iterator.hasNext();) {
	    	
	    	Object object = iterator.next();
			
	    	if (object instanceof User){
	            User activeUser = (User) object;
	            list.append(activeUser.getUserAttributes().getUserFullName() + "[" + activeUser + "]").append(";");
	    	}
	    }

	    return list.toString();
	}
	

	//protected HttpSession currentSession;
	protected boolean isOnSession = false;
	public boolean isOnSession() {
		return isOnSession;
	}

	public void removeOnSession(){
		isOnSession = false;
	}
	
	public HttpSession getHttpSession(){
		return userSession;
	}
	
	/*public HttpSession getSession() {
		return currentSession;
	}*/

	public void valueBound(HttpSessionBindingEvent event) {
		//currentSession = event.getSession();		
		isOnSession = true;
		logger.debug("enter valueBound user " + this.getUserAttributes().getLOGIN() + " (" + this.toString() + ")");
		try {
			
			IndividualUserLogger.infoDebug("valueBound; sessionid =  " + event.getSession().getId(), this);
			// add user to activeUserList
		    User.addActiveUser(this);
		    
		    userSession = event.getSession();
		} catch (Exception e) {
			logger.error("Error in valueBound, this is " + this.getUserAttributes().getLOGIN() + " (" + this.toString() + ")", e);
		}
		logger.debug("exit valueBound user " + this.getUserAttributes().getLOGIN() + " (" + this.toString() + ") session - " + (userSession !=null?userSession.toString():""));
		
	}
	
	public void forceUser(HttpSession session) {
		isOnSession = true;
		logger.debug("enter forceUser user " + this.getUserAttributes().getLOGIN() + " (" + this.toString() + ") session - " + (userSession !=null?userSession.toString():""));
		try {
			IndividualUserLogger.infoDebug("forceUser; sessionid =  " + session.getId(), this);
			
			// add user to activeUserList
		    User.addActiveUser(this);
		    
		    userSession = session;
		} catch (Exception e) {
			logger.error("Error in forceUser, this is " + this.getUserAttributes().getLOGIN() + " (" + this.toString() + ")", e);
		}
		logger.debug("exit forceUser user " + this.getUserAttributes().getLOGIN() + " (" + this.toString() + ") session - " + (userSession !=null?userSession.toString():""));
	}

	public void valueUnbound(HttpSessionBindingEvent event) {
		
		logger.debug("Enter valueUnbound " + this.getUserAttributes().getLOGIN() + " (" + this.toString()  + ") session - " + (userSession !=null?userSession.toString():"") + " event.session - " + (event.getSession() !=null?event.getSession().toString():""));
		
	    //currentSession = null;
		if( !isOnSession ){
			//already invalidated in IdleUserKickerThread
			logger.debug("isOnSession = false");
			IndividualUserLogger.infoDebug("valueUnbound; sessionid =  " + event.getSession().getId(), this);
			return;
		}
		logger.debug("isOnSession = true");
		isOnSession = false;
				
		try {
			IndividualUserLogger.infoDebug("valueUnbound; sessionid =  " + event.getSession().getId(), this);
			// remove user from activeUserList
		    User.removeActiveUser(this);
		    logger.debug("removed Active user");
		    SearchManager.removeSearches(this);
		    logger.debug("removed searches by user");
		    event.getSession().setAttribute(SessionParams.CURRENT_USER, null);
		    event.getSession().setAttribute(UserValidation.LAST_ACCESS_PARAM, null);
		    
		    event.getSession().invalidate();
		    logger.debug("session invalidated" + " (" + this.toString() + ")");
		} catch (Exception e) {
			logger.error("Error in valueUnbound, this is " + this.getUserAttributes().getLOGIN() + " (" + this.toString() + ")", e);
		}
		
		logger.debug("Exit valueUnbound " + this.getUserAttributes().getLOGIN() + " (" + this.toString() + ")");
	}

	public static Logger getLogger() {
		return logger;
	}
	/**
	 * Get the UserAgent from the moment the user logged in
	 * @return UserAgent or null if not found
	 */
	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
		this.browserVersion = StringUtils.getPrettyBrowserString(userAgent);
	}
	/**
	 * Get the browser version from which the user logged in
	 * @return pretty browser version or null
	 */
	public String getBrowserVersion() {
		return browserVersion;
	}
	public static void main(String[] args) {
		
	}

}

package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;

public interface HTTPSiteInterface {
	
	public static final int MAX_CHARACTERS_TO_LOG_ON_INFO	= 100;

	public abstract HTTPResponse process(HTTPRequest request);

	public abstract void destroySession();

	public abstract void destroyAllSessions();

	public abstract void onRedirect(HTTPRequest req);

	public abstract void onAfterRequest(HTTPRequest req, HTTPResponse res);

	public abstract void setSession(HTTPSession session);

	public abstract boolean hasSession();

	public abstract HTTPSession getSession();

	public abstract Object getUserData();

	public abstract void setUserData(Object userData);
   
	public  abstract LoginResponse onLogin();
	
	public  abstract boolean onLogout();
	
	public  abstract void lock();
	
	public  abstract void unlock();
	
	public void performSpecificActionsAfterCreation();

}
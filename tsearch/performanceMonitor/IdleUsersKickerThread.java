package ro.cst.tsearch.performanceMonitor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.time.DateUtils;

import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.data.User;

/**
 * Forcibly terminates idle sessions that didn't receive the valueUnbound(...) notification
 * when session terminated
 *
 * Thread has lowest priority
 * Runs every five minutes, configurable 
 */
public class IdleUsersKickerThread extends Thread{
	public static final long WAKE_UP_INTERVAL = 5 * 60 * 1000; //milliseconds
	
	private long kickedUserCount = 0;
	
	private Vector<User> activeUsersList = new Vector<User>();
	
	public IdleUsersKickerThread( Vector<User> activeUsersList ){
		this.activeUsersList = activeUsersList;
		
		this.setName( "IdleUsersKickerThread" );
	}
	
	public void run(){
		while( true ){
			
			//sleep WAKE_UP_INTERVAL millis
//			synchronized(this){
				try{
					Thread.sleep( WAKE_UP_INTERVAL );
				}
				catch( Exception e ){
					e.printStackTrace();
				}
				
//			}
			
			try {

			//boolean runGC = false;
			
				Set<String> foundUsersToString = new HashSet<>();
				
			//go through logged users list and erase the ones that have their session expired
			synchronized( activeUsersList ){
				Iterator<User> activeUserListIterator = activeUsersList.iterator();
				while( activeUserListIterator.hasNext() ){
					User user = activeUserListIterator.next();
					
					foundUsersToString.add(user.toString());
					
					//user has to be kicked
					if( user.getIdleSeconds() > LoggedUsersMonitor.getInstance().getMaxInactiveInterval() ){
								
						HttpSession userSession = user.getHttpSession();
						
						try {
							if (userSession != null) {
								
								Date creationDate = new Date(userSession.getCreationTime());
								Date lastAccessDate = new Date(userSession.getLastAccessedTime());
								Date now = new Date();
								
								User.getLogger().info("User " + user.toString()
										+ " needs to be kicked and session is " + userSession
										+ " with getCreationTime: " + creationDate
										+ " and getLastAccessedTime: " + lastAccessDate 
										+ " and now is: " + now);
								
								if(DateUtils.addSeconds(lastAccessDate, LoggedUsersMonitor.getInstance().getMaxInactiveInterval()).before(now)) {
									User.getLogger().info("invalidating....");
									
									kickedUserCount++;
									activeUserListIterator.remove();
									
									userSession.invalidate();
									
									break;		//avoid concurrent modification
								}
							} else {
								User.getLogger().info("User " + user.toString()  + " needs to be kicked and has no session ");
								
								User.kickActiveUser( user );
								// increment kicked users count
							    kickedUserCount ++;
							    // erase searches for this user
							    SearchManager.removeSearches(user);
							    
							    //erase from iterating collection
							    activeUserListIterator.remove();
							    
								User.getLogger().error("User " + user.getUserAttributes().getLOGIN() + " (" + user.toString() + ") has no session and will remain in memory like a stupid user");
								
							}
						} catch (IllegalStateException ils) {
							
							User.getLogger().error("User " + user.toString() + " needs to be kicked and an exception has occurred, hope the session is not valid ", ils);
							
							user.removeOnSession();

							// remove user from User object
						    User.kickActiveUser( user );
						    
						    //increment kicked users count
						    kickedUserCount ++;
						    
						    //erase searches for this user
						    SearchManager.removeSearches(user);
						    
						    //erase from iterating collection
						    activeUserListIterator.remove();
							
						    User.getLogger().error("User " + user.getUserAttributes().getLOGIN() + " (" + user.toString() + ") had session invalidated and was removed from memory");
							
						} catch (Throwable t) {
							User.getLogger().error("User " + user.toString() + " needs to be kicked but something happened BAAAAAAAAAAD ", t);
						}
						
					} else {
						User.getLogger().error("User " + user.getUserAttributes().getLOGIN() + " (" + user.toString() + ") has idle seconds " + 
								user.getIdleSeconds() + " (less than " + LoggedUsersMonitor.getInstance().getMaxInactiveInterval() + ")");
						
//						if(ServerConfig.getBoolean("remove.idle.users.with.active.sessions", true)) {
//						
//						user.removeOnSession();
//
//						// remove user from User object
//					    User.kickActiveUser( user );
//					    
//					    //increment kicked users count
//					    kickedUserCount ++;
//					    
//					    //erase searches for this user
//					    SearchManager.removeSearches(user);
//					    
//					    //erase from iterating collection
//					    activeUserListIterator.remove();
//						}
						
					}
				}
			}
			List<User> extraUsersToInvalidate = new ArrayList<>();
			Hashtable activeUsersFromUser = User.getActiveUsers();
			for(Object key : activeUsersFromUser.keySet()) {
				Object object = activeUsersFromUser.get(key);
				if(object instanceof User) {
					if(!foundUsersToString.contains(key)) {
						extraUsersToInvalidate.add((User)object);
					}
				}
			}
			
			if(extraUsersToInvalidate.size() > 0) {
				User.getLogger().error("Found extra " + extraUsersToInvalidate.size() + " to invalidate ");
			} else {
				User.getLogger().error("DidnotFind extra users to invalidate ");
			}
			
			for (User user : extraUsersToInvalidate) {
				//user has to be kicked
				if( user.getIdleSeconds() > LoggedUsersMonitor.getInstance().getMaxInactiveInterval() ){
							
					HttpSession userSession = user.getHttpSession();
					
					try {
						if (userSession != null) {
							
							Date creationDate = new Date(userSession.getCreationTime());
							Date lastAccessDate = new Date(userSession.getLastAccessedTime());
							Date now = new Date();
							
							User.getLogger().info("User extraUsersToInvalidate " + user.toString()
									+ " needs to be kicked and session is " + userSession
									+ " with getCreationTime: " + creationDate
									+ " and getLastAccessedTime: " + lastAccessDate 
									+ " and now is: " + now);
							
							if(DateUtils.addSeconds(lastAccessDate, LoggedUsersMonitor.getInstance().getMaxInactiveInterval()).before(now)) {
								User.getLogger().info("invalidating extraUsersToInvalidate....");
								
//								kickedUserCount++;
								
								userSession.invalidate();
								
							}
						} else {
							User.getLogger().info("User extraUsersToInvalidate " + user.toString()  + " needs to be kicked and has no session ");
							
							User.kickActiveUser( user );
							// increment kicked users count
//						    kickedUserCount ++;
						    // erase searches for this user
						    SearchManager.removeSearches(user);
						    
						    
							User.getLogger().error("User extraUsersToInvalidate " + user.getUserAttributes().getLOGIN() + " (" + user.toString() + ") has no session and will remain in memory like a stupid user");
							
						}
					} catch (IllegalStateException ils) {
						
						User.getLogger().error("User extraUsersToInvalidate " + user.toString() + " needs to be kicked and an exception has occurred, hope the session is not valid ", ils);
						
						user.removeOnSession();

						// remove user from User object
					    User.kickActiveUser( user );
					    
					    //increment kicked users count
//					    kickedUserCount ++;
					    
					    //erase searches for this user
					    SearchManager.removeSearches(user);
					    
					    User.getLogger().error("User extraUsersToInvalidate " + user.getUserAttributes().getLOGIN() + " (" + user.toString() + ") had session invalidated and was removed from memory");
						
					} catch (Throwable t) {
						User.getLogger().error("User extraUsersToInvalidate " + user.toString() + " needs to be kicked but something happened BAAAAAAAAAAD ", t);
					}
					
				} else {
					User.getLogger().error("User extraUsersToInvalidate " + user.getUserAttributes().getLOGIN() + " (" + user.toString() + ") has idle seconds " + 
							user.getIdleSeconds() + " (less than " + LoggedUsersMonitor.getInstance().getMaxInactiveInterval() + ")");
				}
			}
			
			} catch (Exception e) {
				User.getLogger().error("Big error, largest of all in IdleUsersKickerThread", e );
			}
		}
	}
	
	public long getKickedUsersCount(){
		return kickedUserCount;
	}
}
package ro.cst.tsearch.performanceMonitor;

import java.util.Vector;

import ro.cst.tsearch.data.User;

/**
 * Singleton will be used to hold and display stats about logged in users and related searches
 *
 */

public class LoggedUsersMonitor{
	
	private Vector<User> activeUsers;
	
	private int maxSessionInactiveInterval = 0; //int seconds
	
	private long correctlyInvalidatedSessions = 0;
	
	private long totalLogins = 0;
	
	private IdleUsersKickerThread userKickerThread = null;
	
	private static class LoggedUsersMonitorHolder{
		private static LoggedUsersMonitor loggedUsersMonitorInstance = new LoggedUsersMonitor();
	}
	
	public static LoggedUsersMonitor getInstance(){
		return LoggedUsersMonitorHolder.loggedUsersMonitorInstance;
	}
	
	private LoggedUsersMonitor(){
		activeUsers = new Vector<User>();
		
		userKickerThread = new IdleUsersKickerThread( activeUsers );
		userKickerThread.start();
	}

	public void setMaxInactiveInterval( int timeout ){
		maxSessionInactiveInterval = timeout;
	}
	
	public int getMaxInactiveInterval(){
		return maxSessionInactiveInterval;
	}
	
	public Vector<User> getActiveUsers(){
		return activeUsers;
	}
	
	public void addActiveUser( User user ){
		synchronized( activeUsers ){
			if( !activeUsers.contains( user ) ){
				activeUsers.add( user );
				
				totalLogins ++;
			}
		}
	}
	
	public void removeActiveUser( User user ){
		synchronized( activeUsers ){
			if( activeUsers.contains( user ) ){
				correctlyInvalidatedSessions ++;
			}
			
			activeUsers.remove( user );
		}		
	}
	
	//current statistics
	public long getUsersCount(){
		synchronized( activeUsers ){
			return activeUsers.size();
		}
	}
	
	public long getCorrectInvalidatedSessions(){
		return correctlyInvalidatedSessions;
	}
	
	public long getTotalLogins(){
		return totalLogins;
	}
	
	public long getKickedUserCount(){
		return userKickerThread.getKickedUsersCount();
	}
	
	public String getSessionCountHealthCheck(){
		StringBuffer message = new StringBuffer();
			
		synchronized( activeUsers ){

			if( correctlyInvalidatedSessions + activeUsers.size() + userKickerThread.getKickedUsersCount() == totalLogins ){
				//system healthy
				message.append("User count check OK");
			}
			else{
				//error
				message.append( "User count check FAILED<br>Some user sessions were not invalidated!" );
			}
		}
		return message.toString();
	}
}
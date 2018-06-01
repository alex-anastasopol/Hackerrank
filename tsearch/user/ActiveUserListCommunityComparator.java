package ro.cst.tsearch.user;

/*
 * Implements the Comparator interface
 * compares two users using the community they belong to
*/

import java.util.*;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.community.*;

public class ActiveUserListCommunityComparator implements Comparator
{
	private boolean asc = true;
	private int sort_opcode;
	
	public static final int BYFULLNAME = 0;
	public static final int BYLOGINNAME = 1;
	public static final int BYCOMMUNITY = 2;
	public static final int BYIDLE_TIME = 3;
	public static final int BYLOGIN_TIME = 4;
	
	public ActiveUserListCommunityComparator( boolean direction, int sort_opcode )
	{
		//direction = true for ascending
		//direction = false for descending
		super();
		asc = direction;
		this.sort_opcode = sort_opcode;
	}
	
	public int compare(Object o1, Object o2)
	{
		User u1, u2;
		int retval;
		
		if( o1 instanceof User )
		{
			u1 = (User) o1;
		}
		else
		{
			return 0;
		}
		
		if( o2 instanceof User )
		{
			u2 = (User) o2;
		}
		else
		{
			return 0;
		}

		try
		{
			switch( sort_opcode )
			{
				case BYFULLNAME:
					//sorting by full name
					retval = u1.getUserAttributes().getUserFullName().compareToIgnoreCase( u2.getUserAttributes().getUserFullName() );
					break;
				case BYLOGINNAME:
					//sorting by login name
					retval = u1.getUserAttributes().getLOGIN().compareToIgnoreCase( u2.getUserAttributes().getLOGIN() );
					break;
				case BYIDLE_TIME:
					//sorting by idle time
					retval = new Long(u1.getIdleSeconds()).compareTo( u2.getIdleSeconds() );
					break;
				case BYLOGIN_TIME:
					//sorting by login time
					
					Long loggedIn1 = (Long) User.getActiveUsers().get(u1.toString() + "_time");
					Long loggedIn2 = (Long) User.getActiveUsers().get(u2.toString() + "_time");
					
					retval = loggedIn1.compareTo( loggedIn2 );
					break;
				default:
					//sorting by community name, the default action
					CommunityAttributes userCommunity1 = CommunityManager.getCommunity( u1.getUserAttributes().getCOMMID().longValue() );
					CommunityAttributes userCommunity2 = CommunityManager.getCommunity( u2.getUserAttributes().getCOMMID().longValue() );
					retval = userCommunity1.getNAME().compareToIgnoreCase( userCommunity2.getNAME() );				
					break;
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
			retval = 0;
		}
		
		//compare the community id of the two users
		return asc ? - retval : retval ;
	}
}
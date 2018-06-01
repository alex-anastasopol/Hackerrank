package ro.cst.tsearch.utils;

import ro.cst.tsearch.*;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.user.*;
import ro.cst.tsearch.threads.*;
import ro.cst.tsearch.database.*;
import ro.cst.tsearch.bean.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class SearchComparator implements Comparator
{
    public static final int ORDERBY_COUNTY = 0;
    public static final int ORDERBY_STATE = 1;
    public static final int ORDERBY_ID = 2;
    
    public static final int ORDER_ASC = 2;
    public static final int ORDER_DESC = 3;
    
    private int sortOrder;
    private int orderBy;
    
    public SearchComparator( int sortOrder, int orderBy )
    {
        this.sortOrder = sortOrder;
        this.orderBy = orderBy;
    }
    
    public int compare( Object o1, Object o2 )
    {
        SearchAttributes s1 = null;
        SearchAttributes s2 = null;
        
        int compareValue = 0;
        
        if( o1 instanceof Search )
        {
            s1 = ((Search)o1).getSa();
        }
        else
        {
            return 0;
        }
        
        if( o2 instanceof Search )
        {
            s2 = ((Search)o2).getSa();
        }
        else
        {
            return 0;
        }
        
        switch( orderBy )
        {
        	case SearchComparator.ORDERBY_COUNTY:
        	    String county1 = "";
        		String county2 = "";
        		try
        		{
        		    county1 = DBManager.getCountyForId( Long.parseLong( s1.getAtribute( SearchAttributes.P_COUNTY ) ) ).getName();
        		}
        		catch( Exception e1 )
        		{}
        		
        		try
        		{
        		    county2 = DBManager.getCountyForId( Long.parseLong( s2.getAtribute( SearchAttributes.P_COUNTY ) ) ).getName();
        		}
        		catch( Exception e2 )
        		{}
        		
        	    compareValue = county1.compareToIgnoreCase( county2 );
        	    break;
        	case SearchComparator.ORDERBY_STATE:
        	    String state1 = "";
        		String state2 = "";
        		try
        	    {
        		    state1 = DBManager.getStateForId( Long.parseLong( s1.getAtribute( SearchAttributes.P_STATE ) ) ).getName();
        	    }
        		catch( Exception e1 )
        		{}
        		
        		try
        		{
        		    state2 = DBManager.getStateForId( Long.parseLong( s2.getAtribute( SearchAttributes.P_STATE ) ) ).getName();
        		}
        		catch( Exception e2 )
        		{}
        		
	    	    compareValue = state1.compareToIgnoreCase( state2 );
        	    break;
        	case SearchComparator.ORDERBY_ID:
        	    Long id1 = new Long( ((Search)o1).getSearchID() );
    	    	Long id2 = new Long( ((Search)o2).getSearchID() );
    	    	compareValue = id1.compareTo( id2 );
    	    	break;
        	default:
        	    return 0;
        }
        
        if( sortOrder == SearchComparator.ORDER_DESC )
        {
            compareValue = -compareValue;
        }
        
        return compareValue;
    }
}
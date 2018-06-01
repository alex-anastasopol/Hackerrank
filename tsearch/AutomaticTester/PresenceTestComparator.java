package ro.cst.tsearch.AutomaticTester;

import java.util.*;
import java.io.File;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.bean.*;

public class PresenceTestComparator implements Comparator
{
    private int order;
    private int sortBy;
    
    public PresenceTestComparator( int order )
    {
        this.order = order;
        this.sortBy = AutomaticTesterManager.SORT_PRESENCETESTS_BY_SITENAME;
    }
    
    public PresenceTestComparator( int order, int sortBy )
    {
        this.order = order;
        this.sortBy = sortBy;
    }
    
    public int compare( Object job1, Object job2 )
    {
        int result = 0;
        if( !( job1 instanceof PresenceTesterJob ) || !( job2 instanceof PresenceTesterJob ) )
        {
            return 0;
        }
        
        PresenceTesterJob ptj1 = (PresenceTesterJob) job1;
        PresenceTesterJob ptj2 = (PresenceTesterJob) job2;
        
        switch( sortBy )
        {
        	case AutomaticTesterManager.SORT_PRESENCETESTS_BY_SITENAME:
        	    result = ptj1.getServerName().compareToIgnoreCase( ptj2.getServerName() );
        	    break;
        	case AutomaticTesterManager.SORT_PRESENCETESTS_BY_LAST_TEST_STATUS:
        	    result = ptj1.getLastStatus().compareToIgnoreCase( ptj2.getLastStatus() );
        	    break;
        	case AutomaticTesterManager.SORT_PRESENCETESTS_BY_EXEC_STATUS:
        	    result = ptj1.getLastExecutionStatus().compareToIgnoreCase( ptj2.getLastExecutionStatus() );
        	    break;
        }
        
        if( order == AutomaticTesterManager.SORT_PRESENCETEST_DESC )
        {
            result = - result;
        }
        
        return result;
    }
}
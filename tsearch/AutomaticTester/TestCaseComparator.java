package ro.cst.tsearch.AutomaticTester;

import java.util.*;
import java.io.File;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.bean.*;

public class TestCaseComparator implements Comparator
{
    private int order;
    private int sortBy;
    
    public TestCaseComparator( int order )
    {
        this.order = order;
        sortBy = AutomaticTesterManager.SORT_TESTCASES_BY_COUNTY;
    }
    
    public TestCaseComparator( int order, int sortBy )
    {
        this.order = order;
        this.sortBy = sortBy;
    }
    
    public int compare( Object job1, Object job2 )
    {
        int result = 0;
        if( !( job1 instanceof AutomaticSearchJob ) || !( job2 instanceof AutomaticSearchJob ) )
        {
            return 0;
        }
        
        AutomaticSearchJob j1 = (AutomaticSearchJob) job1;
        AutomaticSearchJob j2 = (AutomaticSearchJob) job2;
        
        SearchAttributes sa1 = j1.getSearch().getSa() ;
        SearchAttributes sa2 = j2.getSearch().getSa() ;
        
        switch( sortBy )
        {
        	case AutomaticTesterManager.SORT_TESTCASES_BY_COUNTY:
		        String ownerState_County1 =DBManager.getStateForId( Long.parseLong( sa1.getAtribute( SearchAttributes.P_STATE ) ) ).getStateAbv() + " " + DBManager.getCountyForId( Long.parseLong( sa1.getAtribute( SearchAttributes.P_COUNTY ) ) ).getName();
		        String ownerState_County2 =DBManager.getStateForId( Long.parseLong( sa2.getAtribute( SearchAttributes.P_STATE ) ) ).getStateAbv() + " " + DBManager.getCountyForId( Long.parseLong( sa2.getAtribute( SearchAttributes.P_COUNTY ) ) ).getName();
		        
		        result = ownerState_County1.compareToIgnoreCase( ownerState_County2 );
		        break;
		    case AutomaticTesterManager.SORT_TESTCASES_BY_EXEC_STATUS:
		        
		        result = j1.getLastExecutionStatusString().compareToIgnoreCase( j2.getLastExecutionStatusString() );
		        break;
		    case AutomaticTesterManager.SORT_TESTCASES_BY_LAST_TEST_STATUS:
		        
		    	 	
		        result = j1.getLastStatusString().compareToIgnoreCase( j2.getLastStatusString() );
		        break;
		   
		    case AutomaticTesterManager.SORT_TESTCASES_BY_TEST_SUITE:
		    			      
		       AutomaticTesterManager ATM = AutomaticTesterManager.getInstance();
		       result = (ATM.getTestSuite(j1).getName()).compareToIgnoreCase(ATM.getTestSuite(j2).getName());
		       
		       break;	
		        
        }
        if( order == AutomaticTesterManager.SORT_TESTCASES_DESC )
        {
            result = - result;
        }
        
        return result;
    }
}
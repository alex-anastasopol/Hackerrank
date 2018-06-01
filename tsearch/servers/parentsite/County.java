/*
 * Created on Jun 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.servers.parentsite;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.Hashtable;

import ro.cst.tsearch.data.DataAttribute;
import ro.cst.tsearch.data.GenericCounty;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.DBConstants;

/**
 * @author nae
 * 
 * To change the template for this generated type comment go to Window>Preferences>Java>Code Generation>Code
 * and Comments
 */
public class County extends DataAttribute {
    public static final String COUNTY_NAME = "NAME";

    public static final String COUNTY_ID = "ID";

    private static int STATE = 0;

    private static int NAME = 1;

    private static int ID = 2;

    private static Hashtable countyList = null;

    static
    {
        getCountyList();
    }

	private int countyFips;
	private String countyFipsString = "000";
    
    public County() {
        super();
    }

    //use to construct your own DocumentOraAttributes
    public County(DatabaseData data, int row, State state) {
        setState(state);
        setCountyId(data, row);
        setName(data, row);
    }

    protected int getAttrCount() {
        return ID + 1;
    }

    public void setState(State state) {
        setAttribute(STATE, state);
    }

    public State getState() {
        return (State) getAttribute(STATE);
    }

    // COUNTY NAME
    public void setName(DatabaseData data, int row) {
        setAttribute(NAME, data, COUNTY_NAME, row);
    }

    public void setName(String stateName) {
        setAttribute(NAME, stateName);
    }

    public String getName() {
        return (String) getAttribute(NAME);
    }

    // COUNTY ID

    public void setCountyId(DatabaseData data, int row) {
        setAttribute(ID, data, COUNTY_ID, row);
    }

    public void setCountyId(BigDecimal countyId) {
        setAttribute(ID, countyId);
    }

    public BigDecimal getCountyId() {
        return (BigDecimal) getAttribute(ID);
    }
    
    private static Hashtable cachedCounties = new Hashtable();
    public static County getCounty(String name, String abrvState) throws BaseException {

        
        County county = (County) cachedCounties.get(abrvState + "_" + name);
        
        if (county == null) {
        
        	county = new County();
        	
        	if("ALL".equals(abrvState)) {
        		county.setCountyId(new BigDecimal(0));
                county.setName("");
                State state = new State();
                state.setStateId(new BigDecimal(0));
                state.setName("");
                state.setStateAbv("");
                county.setState(state);
                
                cachedCounties.put(abrvState + "_" + name, county);
                return county;
                
        	}
        	
        	GenericState genericState = DBManager.getStateForAbv(abrvState);
        	if(genericState != null) {
        		GenericCounty genericCounty = DBManager.getCountyForNameAndStateId(name, genericState.getId());
        		if(genericCounty != null) {
        			county.setCountyId(new BigDecimal(genericCounty.getId()));
        			county.setName(genericCounty.getName());
        			State state = new State();
        			
	                state.setStateId(new BigDecimal(genericState.getId()));
	                state.setName(genericState.getName());
	                state.setStateAbv(genericState.getStateAbv());
	                
	                county.setState(state);
	                cachedCounties.put(abrvState + "_" + name, county);
	                return county;
        		}
        	}
        	
        	
        	
            DBConnection conn = null;
            

	        try {
	
	            conn = ConnectionPool.getInstance().requestConnection();
	
	            String stm = " select a.ID,a.NAME, b.ID, b.NAME, b.STATEABV from " + DBConstants.TABLE_COUNTY + " a,"
	                    + " ts_state b where a.STATE_ID = b.ID and UPPER(a.NAME)=UPPER( ? ) and "
	                    + " b.STATEABV= ?" ;
	
	    		PreparedStatement pstmt = conn.prepareStatement( stm );
	    		pstmt.setString( 1, name.trim());
	    		pstmt.setString( 2, abrvState);
	    		DatabaseData data = conn.executePrepQuery(pstmt);	
	    		pstmt.close();	

	
	            if (data.getRowNumber() > 0) {
	                county.setCountyId(new BigDecimal(data.getValue(1, 0).toString()));
	                county.setName((String) data.getValue(2, 0));
	                State state = new State();
	                state.setStateId(new BigDecimal(data.getValue(3, 0).toString()));
	                state.setName((String) data.getValue(4, 0));
	                state.setStateAbv((String) data.getValue(5, 0));
	                county.setState(state);
	            } else {
	                county.setCountyId(new BigDecimal(0));
	                county.setName("");
	                State state = new State();
	                state.setStateId(new BigDecimal(0));
	                state.setName("");
	                state.setStateAbv("");
	                county.setState(state);
	            }
	
	        } catch (Exception e) {
	            throw new BaseException(e);
	        } finally {
	            try {ConnectionPool.getInstance().releaseConnection(conn);} catch (Exception e) {e.printStackTrace();}
	        }
	        
	        cachedCounties.put(abrvState + "_" + name, county);
        }
        
        return county;

    }

    public static County getCounty(BigDecimal countyId) throws BaseException {
    	return getCounty(countyId.intValue());
    }
    
    public static County getCounty(int countyId) throws BaseException
    {
        
        County county = null;
        
        if( countyList != null )
        {
            county = (County) countyList.get( new BigDecimal( countyId ) );
            
            if( county != null )
            {
                return county;
            }
        }
        
        county = new County();
        county.setCountyId(new BigDecimal(countyId));
        county.setName("");
        county.setState(null);
        
        return county;

    }

    public static synchronized Hashtable getCountyList()
    {
        DBConnection conn = null;
        if( countyList != null )
        {
            return countyList;
        }
        
        countyList = new Hashtable();
        
        try
        {
            conn = ConnectionPool.getInstance().requestConnection();

            String stm = "select ID, NAME, STATE_ID, countyFIPS from " + DBConstants.TABLE_COUNTY + " ";

            DatabaseData data = conn.executeSQL(stm);

            if (data.getRowNumber() > 0)
            {
                for( int i = 0 ; i < data.getRowNumber() ; i++ )
                {
                    County county = new County();
                    county.setCountyId(new BigDecimal(data.getValue(1, i).toString()));
                    county.setName((String) data.getValue(2, i));
                    try{
                    	county.setState( State.getState(new BigDecimal(data.getValue(3, i).toString())) );
                    }
                    catch(Exception e){
                    	e.printStackTrace();
                    }
                    county.setCountyFipsString(String.valueOf(data.getValue(4, i)));
                    try{
                    	county.countyFips = Integer.parseInt(county.getCountyFipsString());
                    }
                    catch(Exception e){
                    }
                    
                    countyList.put( county.getCountyId(), county );
                }
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try {ConnectionPool.getInstance().releaseConnection(conn);} catch (Exception e) {e.printStackTrace();}
        }
        
        return countyList;
    }

	public int getCountyFips() {
		return countyFips;
	}

	public String getCountyFipsString() {
		return countyFipsString;
	}

	public void setCountyFipsString(String countyFipsString) {
		this.countyFipsString = countyFipsString;
	}
	
}
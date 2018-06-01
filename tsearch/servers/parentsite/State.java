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
import java.util.Vector;

import ro.cst.tsearch.data.DataAttribute;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
/**
 * @author nae
 */
public class State extends DataAttribute {
	public static final String STATE_NAME	="NAME";
	public static final String STATE_ID		="ID";
	public static final String STATE_ABV	="STATEABV";
	//attributes section
	public static final int NAME	= 0;
	public static final int ID	= 1;
	public static final int ABV	= 2;
    
    private static Hashtable stateList = null;
    private static Vector states = null;
    private static final Vector<String> stateAbrev = new Vector<String>();
	
    public final static int  NR_OF_STATES_SUA = 51;
    
    static
    {
        getStateList();
    }

	private int stateFips;
	private String stateFipsString = "00";
    
	protected int getAttrCount() {
		return ABV + 1;
	}
	
	// STATE ID 
	public void setStateId(DatabaseData data, int row) {
		setAttribute(ID, data, STATE_ID, row);
	}
	public void setStateId(BigDecimal stateId) {
		setAttribute(ID, stateId);
	}
	public BigDecimal getStateId() {
		return (BigDecimal) getAttribute(ID);
	}
	
	
	// STATE NAME
	public void setName(DatabaseData data, int row) {
		setAttribute(NAME, data, STATE_NAME, row);
	}
	public void setName(String stateName) {
		setAttribute(NAME, stateName);
	}
	public String getName() {
		return (String) getAttribute(NAME);
	}

	// STATE ABV
	public void setStateAbv(DatabaseData data, int row) {
		setAttribute(ABV, data, STATE_ABV, row);
	}
	public void setStateAbv(String stateName) {
		setAttribute(ABV, stateName);
	}
	public String getStateAbv() {
		return (String) getAttribute(ABV);
	}	
	public State() {}
	// TO CONSTRUNCT YOUR OWN DATAATTRIBUTES

	public State(DatabaseData data, int row) {
		setName(data, row);
		setStateId(data, row);
		setStateAbv(data, row);
	}
	
	public static State getStateFromAbv(String abv) 
	throws BaseException {
	    
		State state = new State();
		
		if("ALL".equals(abv)) {
			state.setStateAbv(abv);
			state.setName("");
			state.setStateId(new BigDecimal(0));
			return state;
		}
		
		
		GenericState genericState = DBManager.getStateForAbv(abv);
		
		if(genericState != null) {
			state.setStateId(new BigDecimal(genericState.getId()));
			state.setName(genericState.getName());
			state.setStateAbv(abv);
			state.setStateFips(genericState.getStateFips());
			return state;
		}
		
		DBConnection conn = null;		
		String stm = " select id, name from ts_state where stateabv= ?";
		
		
		try {
		
		    conn = ConnectionPool.getInstance().requestConnection();
		    
			PreparedStatement pstmt = conn.prepareStatement( stm );
			pstmt.setString( 1, abv);
			DatabaseData data = conn.executePrepQuery(pstmt);	
			pstmt.close();	
		
			if (data.getRowNumber()>0){
				state.setStateId(new BigDecimal(data.getValue(1,0).toString()));
				state.setName((String)data.getValue(2,0));
				state.setStateAbv(abv);			
			}else{
				state.setStateAbv(abv);
				state.setName("");
				state.setStateId(new BigDecimal(0));
			}
			
		}catch(Exception e){
            throw new BaseException("SQLException:" + e.getMessage());
        }finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    throw new BaseException("SQLException:" + e.getMessage());
			}			
		}
		
		return state;
	}
	
	public static State getState(BigDecimal stateId) throws BaseException {
		return getState(stateId.intValue());
	}
	
	public static State getState(int stateId) throws BaseException {

		State state = null;
        
        if( stateList != null )
        {
            state = (State) stateList.get( new BigDecimal( stateId ) );
            
            if( state != null )
            {
                return state;
            }
        }
        
        state = new State();
        state.setStateAbv("");
        state.setName("");
        state.setStateId( new BigDecimal(stateId) );
        
        return state;
	}
	
	public static Vector getStates() throws BaseException
    {
		return states;
	}
    
    public static synchronized void getStateList()
    {

        if( stateList != null )
        {
            return ;
        }
        
        DBConnection conn = null;
        String stm = " select id, name, stateabv, stateFIPS from ts_state order by name ";
        
        stateList = new Hashtable();
        states = new Vector();
            
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(stm);
            
            int rownum = data.getRowNumber();
            for(int i=0;i<rownum;i++){
                State state = new State();
                state.setStateId(new BigDecimal(data.getValue(1,i).toString()));
                state.setName((String)data.getValue(2,i));
                String stateabv = (String)data.getValue(3,i);
                state.setStateFipsString(String.valueOf(data.getValue(4, i)));
                state.stateFips = Integer.parseInt(state.getStateFipsString());
                state.setStateAbv(stateabv);
                stateList.put( state.getStateId(), state );
                states.addElement(state);
                stateAbrev.add(stateabv);
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
    }

	public static Vector<String> getStateAbrevVectorreadOnly() {
		return ( Vector<String>)stateAbrev.clone();
	}

	public int getStateFips() {
		return stateFips;
	}

	public  void setStateFips(int stateFips) {
		this.stateFips = stateFips ;
	}

	public String getStateFipsString() {
		return stateFipsString;
	}

	public void setStateFipsString(String stateFipsString) {
		this.stateFipsString = stateFipsString;
	}
	
}

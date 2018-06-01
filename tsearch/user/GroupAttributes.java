/*
 * Text here
 */

package ro.cst.tsearch.user;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.database.DatabaseData;

/**
 *
 */
public class GroupAttributes implements Comparable<GroupAttributes>{

	
	public static final int TA_ID  =1; 	// TITLE SEARCH ADMINISTRATOR
	public static final int CA_ID  =2;	// COMMUNITY ADMINISTRATOR
	public static final int AM_ID  =4;	// NOT USED YET - FIRST TIME WAS COUNTY MAN
	public static final int CCA_ID =7; //community admin with "change community" rights
	public static final int ABS_ID=32;	// ABSTRACTOR
	public static final int AG_ID=64; 	// AGENT
	
	public static final  Map<Integer,Integer> SORT_ORDER = new HashMap<Integer, Integer>();
	static{
		SORT_ORDER.put(AG_ID, 0);
		SORT_ORDER.put(ABS_ID, 1);
		SORT_ORDER.put(AM_ID, 2);
		SORT_ORDER.put(CA_ID, 3);
		SORT_ORDER.put(CCA_ID, 4);
		SORT_ORDER.put(TA_ID, 5);
	}
	
	public static final String TA_NICK="TA";
	public static final String CA_NICK="CA";
	public static final String CCA_NICK="CCA";
	public static final String AM_NICK="EX";
	public static final String ABS_NICK="ABS";
	public static final String AG_NICK="AG";
	
	public static final String TA_NAME="Title Search Administrator";
	public static final String CA_NAME="Community Administrator";
	public static final String CCA_NAME="All Communities Administrator";
	public static final String AM_NAME="Executive";
	public static final String ABS_NAME="Abstractor";
	public static final String AG_NAME="Agent";
	
	//end alinap
	public static final String COMM_FIELDS_PREFIX = "COMM";
	/****************************************************/
	//end for manipulated data related to communities

	public static final String GROUP_ID = "GID";
	public static final String GROUP_PAYABLE="PAYABLE";
	public static final String GROUP_NAME="GROUP_NAME";
	public static final String GROUP_ORDER="GROUP_ORDER";
	public static final String GROUP_JOB_TITLE="JOB_TITLE";
	public static final String GROUP_SHORT_TITLE="SHORT_TITLE";
	public static final String GROUP_FIELDS_PREFIX = "GROUP";
    // attributes section
    public static final int ID = 0;
    public static final int NAME  = 1;
    public static final int ORDER  = 2;
    public static final int SHORT_TITLE  = 3;
    public static final int JOB_TITLE  = 4;
    public static final int PAYABLE  = 5;
    // end attributes section

    public static final int ATTR_COUNT = PAYABLE + 1;

    private Object[] attrValues = new Object[ATTR_COUNT];

    public GroupAttributes() {
    }

    public GroupAttributes(GroupAttributes ga) {
        for (int i=0; i<ATTR_COUNT;i++) {
            this.attrValues[i] = ga.getAttrValue(i);
        }
    }
    
    public GroupAttributes(String name){
        attrValues[NAME]      = new String(name);
    }

    public GroupAttributes(String name, BigDecimal id, BigDecimal order, String shortTitle, String jobTitle, String payable){
        attrValues[NAME]      = name;
        attrValues[ID]      = id;
        attrValues[ORDER] = order;
        attrValues[SHORT_TITLE] = shortTitle;
        attrValues[JOB_TITLE] = jobTitle;
        attrValues[PAYABLE] = payable;
    }

    private void init (String prefix, DatabaseData data, int row){
        attrValues[ID] = data.getValue(prefix + GROUP_ID, row);
        attrValues[NAME] = data.getValue(prefix + GROUP_NAME, row);
        attrValues[ORDER] = data.getValue(prefix + GROUP_ORDER, row);
        attrValues[SHORT_TITLE] = data.getValue(prefix + GROUP_SHORT_TITLE, row);
        attrValues[JOB_TITLE] = data.getValue(prefix + GROUP_JOB_TITLE, row);
        attrValues[PAYABLE] = data.getValue(prefix + GROUP_PAYABLE, row);
    }

    public GroupAttributes(DatabaseData data, int row){
        init ("",data, row);
    }
    public GroupAttributes(boolean prefix, DatabaseData data, int row){
        init (GROUP_FIELDS_PREFIX + "_",data, row);
    }
    public Object getAttrValue(int attrId) { 
        return attrValues[attrId];
    }

    public Object getAttribute(int attrId){
        return getAttrValue(attrId);
    }

    public void setAttribute(int attrId, Object value) { 
        attrValues[attrId] = value;
    }
    
    public boolean equals(GroupAttributes ga)  {
        if (ga == null) {
            return false;
        }else {
            long gid1 = new BigDecimal( attrValues[ID].toString()).longValue();
            long gid2 = new BigDecimal(ga.getAttribute(GroupAttributes.ID).toString()).longValue();
            return (gid1 == gid2);
        }
    }
        
    public String toString(){
        return " group = " + attrValues[NAME];
    }

	@Override
	public int compareTo(GroupAttributes o) {
		int oId = Integer.parseInt(getAttribute(ID).toString());
		int id = Integer.parseInt(o.getAttribute(ID).toString());
		
		return SORT_ORDER.get(oId).compareTo(SORT_ORDER.get(id));
		
	}
	
	public int compareTo(UserAttributes ua) {
		int id = Integer.parseInt(getAttribute(ID).toString());
		
		return GroupAttributes.SORT_ORDER.get(id).compareTo(SORT_ORDER.get(ua.getGROUP().intValue()));
		
	}

}
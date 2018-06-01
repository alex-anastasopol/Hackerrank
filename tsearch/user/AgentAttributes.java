/*
 * Created on Sep 11, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.user;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.data.*;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.utils.DBConstants;

/**
 * @author nae
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AgentAttributes extends DataAttribute implements ParameterizedRowMapper<AgentAttributes>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String AGENT_ID = "AGENT_ID";
	public static String AGENT_FIRST_NAME  = "FIRST_NAME"; 
	public static String AGENT_MIDDLE_NAME = "MIDDLE_NAME"; 
	public static String AGENT_LAST_NAME   = "LAST_NAME";  
	public static String AGENT_COMPANY_ID  = "COMPANY_ID";
	public static String AGENT_COMPANY	   = "COMPANY"; 
	public static String AGENT_COMP_TYPE   = "COMP_TYPE";  
	public static String AGENT_PHONE 	   = "PHONE";	
	public static String AGENT_HPHONE 	   = "HPHONE";	
	public static String AGENT_EMAIL 		 = "EMAIL";
	public static String AGENT_WADDRESS 	 = "WADDRESS";
	public static String AGENT_WCITY 		 = "WCITY";
	public static String AGENT_STATE_ID 	 = "STATE_ID";
	public static String AGENT_WZCODE 	 = "WZCODE";
	public static String AGENT_USER_ID	 = "USER_ID";
	public static String AGENT_STREETNO  		="STREETNO";
	public static String AGENT_STREETDIRECTION	="STREETDIRECTION";
	public static String AGENT_STREETNAME		="STREETNAME";
	public static String AGENT_STREETSUFFIX	="STREETSUFFIX";
	public static String AGENT_STREETUNIT		="STREETUNIT";
	public static String AGENT_TYPE = "TYPE";
	public static final String AGENT_COUNTY_ID	 		= "COUNTY_ID";
	public static final String AGENT_SPOUSE_FIRST_NAME 	= "SFIRST_NAME";
	public static final String AGENT_SPOUSE_MIDDLE_NAME 	= "SMIDDLE_NAME";
	public static final String AGENT_SPOUSE_LAST_NAME 	= "SLAST_NAME";
	public static final String AGENT_SPOUSE_PHONE		 	= "SPHONE";
	
	
	public static int OWNR_TYPE = 1;
	public static int BUYR_TYPE = 2;
	public static int AGNT_TYPE = 3;
	
	public static int ID    = 1;
	public static int FIRST_NAME  = 2; 
	public static int MIDDLE_NAME = 3; 
	public static int LAST_NAME   = 4;  
	public static int COMPANY_ID  = 5; 
	public static int COMP_TYPE   = 6;  
	public static int PHONE 		 = 7;	
	public static int HPHONE 	 = 8;	
	public static int EMAIL 		 = 9;
	public static int WADDRESS 	 = 10;
	public static int WCITY 		 = 11;
	public static int STATE_ID 	 = 12;
	public static int WZCODE 	 = 13;
	public static int USER_ID	 = 14;
	
	public static int STREETNO  			=15;
	public static int STREETDIRECTION	=16;
	public static int STREETNAME		=17;
	public static int STREETSUFFIX		=18;
	public static int STREETUNIT		=19;
	public static int TYPE				=20;

	public static final int COUNTY_ID	 		= 21;
	public static final int SPOUSE_FIRST_NAME 	= 22;
	public static final int SPOUSE_MIDDLE_NAME 	= 23;
	public static final int SPOUSE_LAST_NAME 	= 24;
	public static final int SPOUSE_PHONE		= 25;
	public static final int COMPANY		= 26;
	
	/* (non-Javadoc)
	 * @see ro.cst.tsearch.data.DataAttribute#getAttrCount()
	 */
	protected int getAttrCount() {
		return COMPANY+1;
	}

	public AgentAttributes() {
	}
	
	public AgentAttributes(DatabaseData data, int row) {
		setID(data,row);
		setCOMP_TYPE(data,row);
		setCOMPANY_ID(data,row);
		setCOMPANY(data,row);
		setFIRST_NAME(data,row);
		setMIDDLE_NAME(data,row);
		setLASTNAME(data,row);		
		setHPHONE(data,row);
		setPHONE(data,row);
		setSTATE_ID(data,row);
		setUSER_ID(data,row);
		setEMAIL(data,row);
		setWADDRESS(data,row);
		setWCITY(data,row);
		setWZCODE(data,row);
		setSTREETNO(data,row);
		setSTREETDIRECTION(data, row);
		setSTREETNAME(data,row);
		setSTREETSUFFIX(data,row);
		setSTREETUNIT(data,row);
		setTYPE(data,row); 	
		setSPOUSE_FIRST_NAME(data,row); 	
		setSPOUSE_LAST_NAME(data,row); 	
		setSPOUSE_MIDDLE_NAME(data,row); 	
		setSPOUSE_PHONE(data,row); 	
		setCOUNTY_ID(data,row); 	
	}
	/**
	 * @return
	 */
	public BigDecimal getID() {
		if(getAttribute(ID) instanceof BigDecimal)
			return (BigDecimal)getAttribute(ID);
		return new BigDecimal(getAttribute(ID).toString());
	}
	/**
	 * @param i
	 */
	public  void setID(BigDecimal id) {
		setAttribute(ID, id);
	}
	public void setID(DatabaseData data, int row) {
		setAttribute(ID, data, AGENT_ID, row);
	}
	
	/**
	 * @return
	 */
	public  BigDecimal getCOMP_TYPE() {
		return new BigDecimal(getAttribute(COMP_TYPE).toString());
	}
	/**
	 * @param i
	 */
	public void setCOMP_TYPE(BigDecimal compType) {
		setAttribute(COMP_TYPE,compType);
	}
	public void setCOMP_TYPE(DatabaseData data, int row) {
		setAttribute(COMP_TYPE, data, AGENT_COMP_TYPE, row);
	}

	/**
	 * @return
	 */
	public BigDecimal getCOMPANY_ID() {
		return new BigDecimal(getAttribute(COMPANY_ID).toString());
	}
	
	/**
	 * 
	 * @return
	 */
	public String getCOMPANY(){
		return getAttribute(COMPANY).toString();
	}
	/**
	 * @param i
	 */
	public void setCOMPANY_ID(BigDecimal compId) {
		setAttribute(COMPANY_ID, compId);
	}
	public void setCOMPANY_ID(DatabaseData data, int row) {
		setAttribute(COMPANY_ID, data, AGENT_COMPANY_ID, row);
	}
	
	/**
	 * @param company
	 */
	public void setCOMPANY (String  company) {
		setAttribute(COMPANY, company);
	}	
	public void setCOMPANY(DatabaseData data, int row){
		setAttribute(COMPANY, data, AGENT_COMPANY, row);
	}

	/**
	 * @return
	 */
	public String getEMAIL() {
		return (String)getAttribute(EMAIL);
	}
	/**
	 * @param i
	 */
	public void setEMAIL(String email) {
		setAttribute(EMAIL, email);
	}
	public void setEMAIL(DatabaseData data, int row) {
		setAttribute(EMAIL, data, AGENT_EMAIL, row);
	}
	
	/**
	 * @param i
	 */
	public String getFIRST_NAME() {
		return transformNull((String) getAttribute(FIRST_NAME));
	}
	public void setFIRST_NAME(DatabaseData data, int row) {
		setAttribute(FIRST_NAME, data, AGENT_FIRST_NAME, row);
	}
	public void setFIRST_NAME(String value) {
		setAttribute(FIRST_NAME, value);
	}
	
	/**
	 * @return
	 */
	public String getHPHONE() {
		return (String)getAttribute(HPHONE);
	}
	/**
	 * @param i
	 */
	public void setHPHONE(String hphone) {
		setAttribute(HPHONE, hphone);
	}
	public void setHPHONE(DatabaseData data, int row) {
		setAttribute(HPHONE, data, AGENT_HPHONE, row);
	}
	
	// MIDDLENAME = 5;
	public String getMIDDLE_NAME() {
		return transformNull((String) getAttribute(MIDDLE_NAME));
	}
	public void setMIDDLE_NAME(DatabaseData data, int row) {
		setAttribute(MIDDLE_NAME, data, AGENT_MIDDLE_NAME, row);
	}
	public void setMIDDLE_NAME(String value) {
		setAttribute(MIDDLE_NAME, value);
	}
	
	//LASTNAME = 3;
	public String getLAST_NAME() {
		return transformNull((String) getAttribute(LAST_NAME));
	}
	public void setLASTNAME(DatabaseData data, int row) {
		setAttribute(LAST_NAME, data, AGENT_LAST_NAME, row);
	}
	public void setLAST_NAME(String value) {
		setAttribute(LAST_NAME, value);
	}		

	// PHONE = 9;
	public String getPHONE() {
		return transformNull((String) getAttribute(PHONE));
	}
	public void setPHONE(DatabaseData data, int row) {
		setAttribute(PHONE, data, AGENT_PHONE, row);
	}
	public void setPHONE(String value) {
		setAttribute(PHONE, value);
	}
	
	/**
	 * @return
	 */
	public String getSTATE_ID() {
		return (String)getAttribute(STATE_ID);
	}
	/**
	 * @param i
	 */
	/*public void setSTATE_ID(BigDecimal stateId) {
		setAttribute(STATE_ID, stateId);
	}*/
	public void setSTATE_ID(String stateId) {
		setAttribute(STATE_ID, stateId);
	}
	public void setSTATE_ID(DatabaseData data, int row) {
		setAttribute(STATE_ID, data, AGENT_STATE_ID, row);
	}

	/**
	 * @return
	 */
	public BigDecimal getUSER_ID() {
		if(getAttribute(USER_ID)==null)
			return null;
		return new BigDecimal(getAttribute(USER_ID).toString());
	}
	public void setUSER_ID(BigDecimal userId) {
		setAttribute(USER_ID, userId);
	}
	public void setUSER_ID(DatabaseData data, int row) {
		setAttribute(USER_ID, data, AGENT_USER_ID, row);
	}
	
	// WADDRESS = 14;
	public String getWADDRESS() {
		return transformNull((String) getAttribute(WADDRESS));
	}
	public void setWADDRESS(DatabaseData data, int row) {
		setAttribute(WADDRESS, data, AGENT_WADDRESS, row);
	}
	public void setWADDRESS(String value) {
		setAttribute(WADDRESS, value);
	}

	// WCITY = 15;
	public String getWCITY() {
		return transformNull((String) getAttribute(WCITY));
	}
	public void setWCITY(DatabaseData data, int row) {
		setAttribute(WCITY, data, AGENT_WCITY, row);
	}
	public void setWCITY(String value) {
		setAttribute(WCITY, value);
	}

	// WZCODE = 17;
	public String getWZCODE() {
		return transformNull((String) getAttribute(WZCODE));
	}
	public void setWZCODE(DatabaseData data, int row) {
		setAttribute(WZCODE, data, AGENT_WZCODE, row);
	}
	public void setWZCODE(String value) {
		setAttribute(WZCODE, value);
	}

	/**
	 * @return
	 */
	public String getSTREETNO() {
		return (String) getAttribute(STREETNO);
	}
	public void setSTREETNO(DatabaseData data, int row) {
		setAttribute(STREETNO, data, AGENT_STREETNO, row);
	}
	public void setSTREETNO(String value) {
		setAttribute(STREETNO, value);
	}	

	/**
	 * @return
	 */
	public String getSTREETDIRECTION() {
		return transformNull((String) getAttribute(STREETDIRECTION));
	}
	public void setSTREETDIRECTION(DatabaseData data, int row) {
		setAttribute(STREETDIRECTION, data, AGENT_STREETDIRECTION, row);
	}
	public void setSTREETDIRECTION(String value) {
		setAttribute(STREETDIRECTION, value);
	}	
	
	/**
	 * @return
	 */
	public String getSTREETNAME() {
		return transformNull((String) getAttribute(STREETNAME));
	}
	public void setSTREETNAME(DatabaseData data, int row) {
		setAttribute(STREETNAME, data, AGENT_STREETNAME, row);
	}
	public void setSTREETNAME(String value) {
		setAttribute(STREETNAME, value);
	}	
	
	/**
	 * @return
	 */
	public String getSTREETSUFFIX() {
		return transformNull((String) getAttribute(STREETSUFFIX));
	}
	public void setSTREETSUFFIX(DatabaseData data, int row) {
		setAttribute(STREETSUFFIX, data, AGENT_STREETSUFFIX, row);
	}
	public void setSTREETSUFFIX(String value) {
		setAttribute(STREETSUFFIX, value);
	}		

	/**
	 * @return
	 */
	public String getSTREETUNIT() {
		return transformNull((String) getAttribute(STREETUNIT));
	}
	public void setSTREETUNIT(DatabaseData data, int row) {
		setAttribute(STREETUNIT, data, AGENT_STREETUNIT, row);
	}
	public void setSTREETUNIT(String value) {
		setAttribute(STREETUNIT, value);
	}

	/**
	 * @return
	 */
	public int getTYPE() {
		return (new BigDecimal(getAttribute(TYPE).toString())).intValue();
	}
	public void setTYPE(String value) {
		setAttribute(TYPE, value);
	}
	public void setTYPE(DatabaseData data, int row) {
		setAttribute(TYPE, data, AGENT_TYPE, row);
	}

	/**
	 * @return
	 */
	public int getCOUNTY_ID() {
		return new BigDecimal(getAttribute(COUNTY_ID).toString()).intValue();
	}
	public void setCOUNTY_ID(BigDecimal value) {
		setAttribute(COUNTY_ID, value);
	}
	public void setCOUNTY_ID(DatabaseData data, int row) {
		setAttribute(COUNTY_ID, data, AGENT_COUNTY_ID, row);
	}

	/**
	 * @return
	 */
	public String getSPOUSE_FIRST_NAME() {
		return transformNull((String) getAttribute(SPOUSE_FIRST_NAME));
	}
	public void setSPOUSE_FIRST_NAME(String value) {
		setAttribute(SPOUSE_FIRST_NAME, value);
	}
	public void setSPOUSE_FIRST_NAME(DatabaseData data, int row) {
		setAttribute(SPOUSE_FIRST_NAME, data, AGENT_SPOUSE_FIRST_NAME, row);
	}

	/**
	 * @return
	 */
	public String getSPOUSE_MIDDLE_NAME() {
		return transformNull((String) getAttribute(SPOUSE_MIDDLE_NAME));
	}
	public void setSPOUSE_MIDDLE_NAME(String value) {
		setAttribute(SPOUSE_MIDDLE_NAME, value);
	}
	public void setSPOUSE_MIDDLE_NAME(DatabaseData data, int row) {
		setAttribute(SPOUSE_MIDDLE_NAME, data, AGENT_SPOUSE_MIDDLE_NAME, row);
	}

	/**
	 * @return
	 */
	public String getSPOUSE_LAST_NAME() {
		return transformNull((String) getAttribute(SPOUSE_LAST_NAME));
	}
	public void setSPOUSE_LAST_NAME(String value) {
		setAttribute(SPOUSE_LAST_NAME, value);
	}
	public void setSPOUSE_LAST_NAME(DatabaseData data, int row) {
		setAttribute(SPOUSE_LAST_NAME, data, AGENT_SPOUSE_LAST_NAME, row);
	}

	/**
	 * @return
	 */
	public String getSPOUSE_PHONE() {
		return transformNull((String) getAttribute(SPOUSE_PHONE));
	}
	public void setSPOUSE_PHONE(String value) {
		setAttribute(SPOUSE_PHONE, value);
	}
	public void setSPOUSE_PHONE(DatabaseData data, int row) {
		setAttribute(SPOUSE_PHONE, data, AGENT_SPOUSE_PHONE, row);
	}

	@Override
	public AgentAttributes mapRow(ResultSet rs, int rowNum) throws SQLException {
		AgentAttributes agent = new AgentAttributes();
    	agent.setID(new BigDecimal(rs.getLong(DBConstants.FIELD_USER_ID)));
    	agent.setFIRST_NAME(rs.getString(DBConstants.FIELD_USER_FIRST_NAME));
    	agent.setLAST_NAME(rs.getString(DBConstants.FIELD_USER_LAST_NAME));
    	agent.setCOMPANY(rs.getString(DBConstants.FIELD_USER_COMPANY));
        return agent;
	}

}

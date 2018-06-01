/*
 * @(#)CommunityAttributes.java 1.30 2000/08/15
 * Copyright (c) 1998-2000 CornerSoft Technologies, SRL
 * Bucharest, Romania
 * All Rights Reserved.
 *
 * This software is he confidential and proprietary information of
 * CornerSoft Technologies, SRL. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with CST.
 */

package ro.cst.tsearch.community;

import java.math.BigDecimal;
import java.math.BigInteger;

import ro.cst.tsearch.data.DataAttribute;
import ro.cst.tsearch.data.DataException;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;

/**
 * Class used in order to define attributes for community entities
 */

public class CommunityAttributes extends DataAttribute {

	/* TODO's
	 * search for appropriate ctime and lastaccess attribute in LDAP
	 */

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 461417161840134153L;


	private static String crtTime =
		Long.toString(FormatDate.currentTimeMillis());
	

	// default values for a default community	
	public static final String DEFAULT_COMMUNITY = "Default";
	public static final String DEFAULT_DESCRIPTION = "";
	public static final String DEFAULT_CATEGORY = "Main";
	public static final String DEFAULT_SEE_ALSO = "";
	public static final String DEFAULT_CTIME = crtTime;
	public static final String DEFAULT_LAST_ACCESS = crtTime;
	public static final String DEFAULT_ADRESS = "";
	public static final String DEFAULT_PHONE = "";
	public static final String DEFAULT_EMAIL = "";
	public static final String DEFAULT_CONTACT = "";
	
	public static final String COMMUNITY_ID = "COMM_ID";
	public static final String COMMUNITY_NAME = "COMM_NAME";
	public static final String COMMUNITY_CATEGORY = "CATEG_ID";
	public static final String COMMUNITY_DESCRIPTION = "DESCRIPTION";
	public static final String COMMUNITY_ADMIN = "COMM_ADMIN";
	public static final String COMMUNITY_CTIME = "CTIME";
	public static final String COMMUNITY_LAST_ACCESS = "LAST_ACCESS";
	public static final String COMMUNITY_ADDRESS = "ADDRESS";
	public static final String COMMUNITY_PHONE = "PHONE";
	public static final String COMMUNITY_EMAIL = "EMAIL";
	public static final String COMMUNITY_CONTACT = "CONTACT_PERSON";
	public static final String COMMUNITY_CODE = "COMM_CODE";
	public static final String COMMUNITY_SEE_ALSO = "SEE_ALSO";
	public static final String COMMUNITY_CRNCY_ID = "CRNCY_ID";
	public static final String COMMUNITY_TEMPLATE = "TEMPLATE";
	public static final String COMMUNITY_LOGO		= "LOGO";
	public static final String COMMUNITY_LOGO_ID		= "ID";
	public static final String COMMUNITY_INV_DUE_OFFSET = "INV_DUE_OFFSET";
	public static final String COMMUNITY_TSD_INDEX = "TSD_INDEX";
	public static final String COMMUNITY_COMMITMENT= "COMMITMENT";
	public static final String COMMUNITY_TEMPLATES_PATH= "TEMPLATES_PATH";
	public static final String COMMUNITY_OFFSET = "OFFSET";
	public static final String COMMUNITY_AUTOFILEID = "AUTOFILEID";
	public static final String COMMUNITY_TERMS_OF_USE = "TERMS_FILE";
	public static final String COMMUNITY_PRIVACY_STATEMENT = "PRIVACY_FILE";
	public static final String COMMUNITY_IGNORE_PRIVACY_STATEMENT = "IGNORE_PRIVACY_STATEMENT";
	public static final String COMMUNITY_DEFAULT_SLA ="DEFAULT_SLA";
	public static final String COMMUNITY_SUPPORT_EMAIL ="SUPPORT_EMAIL";
	
	public static final String COMMUNITY_NUMBER_OF_UPDATES ="NUMBER_OF_UPDATES";
	public static final String COMMUNITY_NUMBER_OF_DAYS ="NUMBER_OF_DAYS";
    
	//default start date offset for this community ( years )
	public static final String COMMUNITY_TERMS_ID = "TERMS_ID";
	
	//specific information 
    public static final String COMMUNITY_PLACEHOLDER = "COMMUNITY_PLACEHOLDER";
	
	public static final String COMMUNITY_VIEWED ="comm_viewed";
	// attributes indexes section
	public static final int ID = 0;
	public static final int NAME = 1;
	public static final int CATEGORY = 2;
	public static final int DESCRIPTION = 3;
	public static final int COMM_ADMIN = 4;
	public static final int CTIME = 5;
	public static final int LAST_ACCESS = 6;
	public static final int ADDRESS = 7;
	public static final int PHONE = 8;
	public static final int EMAIL = 9;
	public static final int CONTACT = 10;
	public static final int CODE = 11;
	public static final int SEE_ALSO = 12;
	public static final int CRNCY_ID = 13;
	public static final int TEMPLATE = 14;
	public static final int INV_DUE_OFFSET = 15;
	public static final int TSD_INDEX = 16;
	public static final int COMMITMENT= 17;
	public static final int LOGO= 18;
	public static final int TEMPLATES_PATH = 19;
	public static final int OFFSET = 20;
	public static final int AUTOFILEID = 21;
    //public static final int DEFAULT_STARTDATE = 22;
    public static final int DEFAULT_SLA =22;
    public static final int SUPPORT_EMAIL =23;
    public static final int IGNORE_PRIVACY_STATEMENT =24;
    
    public static final int PLACEHOLDER =25;
    
    public static final int NUMBER_OF_UPDATES = 26;
    public static final int NUMBER_OF_DAYS = 27;

	protected int getAttrCount() {

		return NUMBER_OF_DAYS + 1;

	}	
	/** a basic constructor used for special circumstances */
	public CommunityAttributes() {
	}
	
//	public CommunityAttributes(
//			String name,
//			String description,
//			long admin,
//			long category,
//			String seealso,
//			String address,
//			String email,
//			String phone,
//			String contact,
//			BigInteger ctime,
//			String code,
//			Integer offset,
//			Integer autofileid,
//            BigInteger defaultSLA,
//            String supportEmail, 
//            Boolean ignorePrivacyStatement) {
//	    
//		setNAME(name);
//		setDESCRIPTION(description);
//		setCOMM_ADMIN(new BigDecimal(admin));
//		setCATEGORY(new BigDecimal(category));
//		setSEE_ALSO(seealso);
//		setADDRESS(address);
//		setEMAIL( email);
//		setPHONE(phone);
//		setCONTACT(contact);
//		setCTIME(ctime);
//		setCODE(code);
//		setOFFSET(offset);
//		setAUTOFILEID(autofileid);
//        //setDEFAULTSTARTDATE( defaultOffset );
//        setDEFAULTSLA(defaultSLA);
//        setSUPPORTEMAIL(supportEmail);
//        setIgnorePrivacyStatement(ignorePrivacyStatement);
//        
//	}
		
	public void setIgnorePrivacyStatement(Boolean ignorePrivacyStatement) {
		setAttribute(IGNORE_PRIVACY_STATEMENT, ignorePrivacyStatement);
	}
	
	public CommunityAttributes(DatabaseData data, int row) {
		setADDRESS(data,row);		
		setCOMM_ADMIN(data,row);
		setCATEGORY(data,row);
		setCODE(data,row);
		setDESCRIPTION(data,row);
		setEMAIL(data,row);
		setPHONE(data,row);
		setCONTACT(data,row);
		setID(data,row);
		setLAST_ACCESS(data,row);
		setNAME(data,row);
		setSEE_ALSO(data,row);
		setTSD_INDEX(data,row);
		setCOMMITMENT(data,row);
		setTEMPLATES_PATH(data,row);
		setOFFSET(data, row);
		setAUTOFILEID(data, row);
        //setDEFAULTSTARTDATE(data,row);
        setDEFAULTSLA(data,row);
        setSUPPORTEMAIL(data,row);
        setIgnorePrivacyStatement(data,row);
        setPLACEHOLDER(data,row);
        setNUMBEROFUPDATES(data, row);
        setNUMBEROFDAYS(data, row);
	}
	
	public CommunityAttributes(String name,
			String description,
			long admin,
			long category,
			String seealso,
			String address,
			String email,
			String phone,
			String contact,
			BigInteger ctime,
			String code,
			Integer offset,
			Integer autofileid,
            BigInteger defaultSLA,
            String supportEmail, 
            Boolean ignorePrivacyStatement,
            String placeHolder,
            Integer numberOfUpdates,
            Integer numberOfDays) {
		
		setNAME(name);
		setDESCRIPTION(description);
		setCOMM_ADMIN(new BigDecimal(admin));
		setCATEGORY(new BigDecimal(category));
		setSEE_ALSO(seealso);
		setADDRESS(address);
		setEMAIL( email);
		setPHONE(phone);
		setCONTACT(contact);
		setCTIME(ctime);
		setCODE(code);
		setOFFSET(offset);
		setAUTOFILEID(autofileid);
        //setDEFAULTSTARTDATE( defaultOffset );
        setDEFAULTSLA(defaultSLA);
        setSUPPORTEMAIL(supportEmail);
        setIgnorePrivacyStatement(ignorePrivacyStatement);
        setPLACEHOLDER(placeHolder);
        setNUMBEROFUPDATES(numberOfUpdates);
        setNUMBEROFDAYS(numberOfDays);
	}
	public void setIgnorePrivacyStatement(DatabaseData data, int row) {
		setAttribute( IGNORE_PRIVACY_STATEMENT, data, COMMUNITY_IGNORE_PRIVACY_STATEMENT, row );
	}
	
	public  Boolean getIgnorePrivacyStatement() {
		return new Boolean(getAttribute(IGNORE_PRIVACY_STATEMENT).toString());
	}
	
	/**
	 * @return
	 */
	public  String getADDRESS() {
		return (String)getAttribute(ADDRESS);
	}

	public  void setADDRESS(String address) {
		setAttribute(ADDRESS,address);
	}
	
	public  void setADDRESS(DatabaseData data, int row) {
		setAttribute(ADDRESS,data,COMMUNITY_ADDRESS,row);
	}
	
	/**
	 * @return
	 */
	public  BigDecimal getCATEGORY() {
		return new BigDecimal(getAttribute(CATEGORY).toString());
	}

	public  void setCATEGORY(BigDecimal category) {
		setAttribute(CATEGORY,category);
	}
	public  void setCATEGORY(DatabaseData data, int row) {
		setAttribute(CATEGORY, data, COMMUNITY_CATEGORY, row);
	}
	/**
	 * @return
	 */
	public BigDecimal getCOMM_ADMIN() {
		return new BigDecimal(getAttribute(COMM_ADMIN).toString());
	}
	public  void setCOMM_ADMIN(BigDecimal comm_admin) {
		setAttribute(COMM_ADMIN,comm_admin);
	}
	public  void setCOMM_ADMIN(DatabaseData data, int row) {
		setAttribute(COMM_ADMIN, data, COMMUNITY_ADMIN, row);
	}

	/**
	 * @return
	 */
	public String getCODE() {
		return (String) getAttribute(CODE);
	}
	public  void setCODE(String code) {
		setAttribute(CODE, code);
	}
	public  void setCODE(DatabaseData data, int row) {
		setAttribute(CODE, data, COMMUNITY_CODE, row);
	}
	
	/**
	 * @return
	 */
	public Integer getOFFSET() {
		return new Integer(getAttribute(OFFSET).toString());
	}
	public  void setOFFSET(Integer offset) {
		setAttribute(OFFSET, offset);
	}
	public  void setOFFSET(DatabaseData data, int row) {
		setAttribute(OFFSET, data, COMMUNITY_OFFSET, row);
	}
	
	/**
	 * @return
	 *///COMMUNITY_AUTOFILEID
	public Integer getAUTOFILEID() {
		return new Integer(getAttribute(AUTOFILEID).toString());
	}
	public  void setAUTOFILEID(Integer autofileid) {
		setAttribute(AUTOFILEID, autofileid);
	}
	public  void setAUTOFILEID(DatabaseData data, int row) {
		setAttribute(AUTOFILEID, data, COMMUNITY_AUTOFILEID, row);
	}
	
	/**
	 * @return
	 */
	public String getCONTACT() {
		return (String)getAttribute(CONTACT);
	}
	public  void setCONTACT(String contact) {
		setAttribute(CONTACT,contact);
	}
	public  void setCONTACT(DatabaseData data, int row) {
		setAttribute(CONTACT, data, COMMUNITY_CONTACT, row);
	}
	/**
	 * @return
	 */
	public BigDecimal getCRNCY_ID() {
		return new BigDecimal(getAttribute(CRNCY_ID).toString());
	}
	public  void setCRNCY_ID(BigDecimal value) {
		setAttribute(CRNCY_ID,value);
	}
	public  void setCRNCY_ID(DatabaseData data, int row) {
		setAttribute(CRNCY_ID, data, COMMUNITY_CRNCY_ID, row);
	}
	/**
	 * @return
	 */
	public  String getDESCRIPTION() {
		return (String)getAttribute(DESCRIPTION);
	}
	public  void setDESCRIPTION(String value) {
		setAttribute(DESCRIPTION,value);
	}
	public  void setDESCRIPTION(DatabaseData data, int row) {
		setAttribute(DESCRIPTION, data, COMMUNITY_DESCRIPTION, row);
	}
	/**
	 * @return
	 */
	public String getEMAIL() {
		return (String)getAttribute(EMAIL);
	}
	public  void setEMAIL(String value) {
		setAttribute(EMAIL,value);
	}
	public  void setEMAIL(DatabaseData data, int row) {
		setAttribute(EMAIL, data, COMMUNITY_EMAIL, row);
	}
	/**
	 * @return
	 */
	public BigDecimal getID() {
		Object ob = getAttribute(ID);
		if(ob instanceof BigInteger)
			return new BigDecimal((BigInteger)ob);
		return (BigDecimal)ob;
	}
	public  void setID(BigDecimal value) {
		setAttribute(ID,value);
	}
	public  void setID(DatabaseData data, int row) {
		setAttribute(ID, data, COMMUNITY_ID, row);
	}
	/**
	 * @return
	 */
	public BigDecimal getINV_DUE_OFFSET() {
		return new BigDecimal(getAttribute(INV_DUE_OFFSET).toString());
	}
	public  void setINV_DUE_OFFSET(String value) {
		setAttribute(INV_DUE_OFFSET,value);
	}
	public  void setINV_DUE_OFFSET(DatabaseData data, int row) {
		setAttribute(INV_DUE_OFFSET, data, COMMUNITY_INV_DUE_OFFSET, row);
	}
	/**
	 * @return
	 */
	public BigDecimal getLAST_ACCESS() {
		return new BigDecimal(getAttribute(LAST_ACCESS).toString());
	}
	public  void setLAST_ACCESS(BigDecimal value) {
		setAttribute(LAST_ACCESS,value);
	}
	public  void setLAST_ACCESS(DatabaseData data, int row) {
		setAttribute(LAST_ACCESS, data, COMMUNITY_LAST_ACCESS, row);
	}
	/**
	 * @return
	 */
	public String getNAME() {
		return (String)getAttribute(NAME);
	}
	public  void setNAME(String value) {
		setAttribute(NAME,value);
	}
	public  void setNAME(DatabaseData data, int row) {
		setAttribute(NAME, data, COMMUNITY_NAME, row);
	}
	/**
	 * @return
	 */
	public String getPHONE() {
		return (String)getAttribute(PHONE);
	}
	public  void setPHONE(String value) {
		setAttribute(PHONE,value);
	}
	public  void setPHONE(DatabaseData data, int row) {
		setAttribute(PHONE, data, COMMUNITY_PHONE, row);
	}
	/**
	 * @return
	 */
	public String getSEE_ALSO() {
		return (String)getAttribute(SEE_ALSO);
	}
	public  void setSEE_ALSO(String value) {
		setAttribute(SEE_ALSO,value);
	}
	public  void setSEE_ALSO(DatabaseData data, int row) {
		setAttribute(SEE_ALSO, data, COMMUNITY_SEE_ALSO, row);
	}
	/**
	 * @return
	 */
	public BigDecimal getTEMPLATE() {
		return new BigDecimal(getAttribute(TEMPLATE).toString());
	}
	public  void setTEMPLATE(BigDecimal value) {
		setAttribute(LAST_ACCESS,value);
	}
	public  void setTEMPLATE(DatabaseData data, int row) {
		setAttribute(TEMPLATE, data, COMMUNITY_TEMPLATE, row);
	}
	
	public  String getTEMPLATES_PATH() {
		return (String)getAttribute(TEMPLATES_PATH);
	}
	public  void setTEMPLATES_PATH(String value) {
		setAttribute(TEMPLATES_PATH,value);
	}
	public  void setTEMPLATES_PATH(DatabaseData data, int row) {
		setAttribute(TEMPLATES_PATH, data, COMMUNITY_TEMPLATES_PATH, row);
	}
	
	public BigInteger getCTIME() {
		return new BigInteger(getAttribute(CTIME).toString());
	}
	public  void setCTIME(BigInteger value) {
		setAttribute(CTIME,value);
	}
	public  void setCTIME(DatabaseData data, int row) {
		setAttribute(CTIME, data, COMMUNITY_CTIME, row);
	}

	public BigDecimal getTSD_INDEX() {
		if(getAttribute(TSD_INDEX)==null)
			return null;
		return new BigDecimal(getAttribute(TSD_INDEX).toString());
	}
	public  void setTSD_INDEX(BigDecimal value) {
		setAttribute(TSD_INDEX,value);
	}
	public  void setTSD_INDEX(DatabaseData data, int row) {
		setAttribute(TSD_INDEX, data, COMMUNITY_TSD_INDEX, row);
	}
	
	/**
	 * @return
	 */
	public BigDecimal getCOMMITMENT() {
		return new BigDecimal(getAttribute(COMMITMENT).toString());
	}

	/**
	 * @param i
	 */
	public void setCOMMITMENT(BigDecimal i) {
		setAttribute(COMMITMENT,i);
	}
	
	public  void setCOMMITMENT(DatabaseData data, int row) {
		setAttribute(COMMITMENT, data, COMMUNITY_COMMITMENT, row);
	}
	
	public static void setCOMMITMENT(BigDecimal commId, int value)
	throws BaseException, DataException{
		String condition = CommunityAttributes.COMMUNITY_ID + "=" + commId;
		updateAttributes(DBConstants.TABLE_COMMUNITY,
		CommunityAttributes.COMMUNITY_COMMITMENT,
		new Integer(value).toString(),condition);
	}
	public byte[] getLOGO() {
		return (byte[])getAttribute(LOGO);
	}
	public  void setLOGO(byte[] value) {
		setAttribute(LOGO,value);
	}
		
    public void setDEFAULTSLA(BigInteger defaultSLA){
    	setAttribute(DEFAULT_SLA, defaultSLA);
    }
	
    public BigInteger getDEFAULTSLA(){
    	return new BigInteger(getAttribute(DEFAULT_SLA).toString());
    }
    
    public void setDEFAULTSLA(DatabaseData data, int row)
    {
        setAttribute( DEFAULT_SLA, data, COMMUNITY_DEFAULT_SLA, row );
    }
    
    public void setSUPPORTEMAIL(String supportEmail){
    	setAttribute(SUPPORT_EMAIL, supportEmail);
    }
	
    public String getSUPPORTEMAIL(){
    	return getAttribute(SUPPORT_EMAIL).toString();
    }
    
    public void setSUPPORTEMAIL(DatabaseData data, int row)
    {
        setAttribute( SUPPORT_EMAIL, data, COMMUNITY_SUPPORT_EMAIL, row );
    }
    
    public void setPLACEHOLDER(String placeHolder){
    	setAttribute(PLACEHOLDER, placeHolder);
    }
    
    public String getPLACEHOLDER(){
    	try{
    		if(getAttribute(PLACEHOLDER)!=null)
    			return getAttribute(PLACEHOLDER).toString();
    	}catch (Exception e) {
    		System.err.println("ERROR: 			PLACEHOLDER exception!!!");
    		e.printStackTrace();
    	}
    	return "";
    }
    
    public void setPLACEHOLDER(DatabaseData data, int row){
        setAttribute( PLACEHOLDER, data, COMMUNITY_PLACEHOLDER, row );
    }
    
    /**
	 * @return
	 */
	public Integer getNUMBEROFUPDATES() {
		return new Integer(getAttribute(NUMBER_OF_UPDATES).toString());
	}
	public  void setNUMBEROFUPDATES(Integer numberOfUpdates) {
		setAttribute(NUMBER_OF_UPDATES, numberOfUpdates);
	}
	public  void setNUMBEROFUPDATES(DatabaseData data, int row) {
		setAttribute(NUMBER_OF_UPDATES, data, COMMUNITY_NUMBER_OF_UPDATES, row);
	}
	
	/**
	 * @return
	 */
	public Integer getNUMBEROFDAYS() {
		return new Integer(getAttribute(NUMBER_OF_DAYS).toString());
	}
	public  void setNUMBEROFDAYS(Integer numberOfDays) {
		setAttribute(NUMBER_OF_DAYS, numberOfDays);
	}
	public  void setNUMBEROFDAYS(DatabaseData data, int row) {
		setAttribute(NUMBER_OF_DAYS, data, COMMUNITY_NUMBER_OF_DAYS, row);
	}

}

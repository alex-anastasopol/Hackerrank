package ro.cst.tsearch.user;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.data.DataAttribute;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.utils.DBConstants;

import com.stewart.ats.base.document.ImageI;

public class UserAttributes extends DataAttribute 
		implements Serializable, Cloneable, ParameterizedRowMapper<UserAttributes> {

    static final long serialVersionUID = 10000000;
    
	public static final int GRP_REGULAR   = 0x00000001;
	public static final int GRP_PM        = 0x00000002;
	public static final int GRP_ADMIN     = 0x00000006;
	
	public static final String OS_DISABLED = "DISABLED";
	public static final String OS_ASK = "ASK";
	public static final String OS_SO = "SO";
	public static final String OS_SUS = "SUS";
	
	public static final int GROUP_AGENT = 64;
	
	public static final int TSR_MODE    = 0;
	public static final int ATTACH_MODE = 1; 
	public static final int COMMIT_MODE = 2;	
	public static final int EOL_POLICY_MODE = 3;
	public static final int OWNERS_POLICY_MODE = 4;
	public static final int SHORT_POLICY_MODE = 5;
	public static final int FCOT_MODE = 6;
	public static final int LOAN_POLICY_MODE = 7;
	public static final int COM_SCH_B2_MODE = 8;
	public static final int MTG_SCH_A_MODE = 9;
	public static final int MTG_SCH_B1_MODE = 10;
	public static final int MTG_SCH_B2_MODE = 11;
	public static final int ATS_FIELDS_MODE = 12;
	
	public static final String USER_PAGES = "USRPAGES";
	public static final String USER_ID = "USER_ID";
	public static final String USER_LOGIN = "LOGIN";
	/**
	 * To be removed. This was the key for the password's plain text format 
	 */
	@Deprecated
	public static final String USER_PASSWD = "PASSWD";
	public static final String USER_PASSWORD = "PASSWORD";
	public static final String USER_RANDOM_TOKEN = "randomToken";
	// FOR JSP PAGE
	public static final String USER_NEW_PASSWORD = "NEW_PASSWORD";
	public static final String USER_CONFIRMED_PASSWORD = "CONFIRMED_PASSWORD";
	

	// END FOR PAGE
	
	public static final String USER_LASTNAME = "LAST_NAME";
	public static final String USER_FIRSTNAME = "FIRST_NAME";
	public static final String USER_MIDDLENAME = "MIDDLE_NAME";

	public static final String USER_COMPANY = "COMPANY";
	public static final String USER_EMAIL = "EMAIL";
	public static final String USER_ALTEMAIL = "A_EMAIL";
	public static final String USER_PHONE = "PHONE";
	public static final String USER_ALTPHONE = "A_PHONE";
	public static final String USER_ICQ = "ICQ_NUMBER";
	public static final String USER_AOL = "AOL_SCREEN_NAME";
	public static final String USER_YAHOO = "YAHOO_MESSAGER";

	public static final String USER_WADDRESS = "WADDRESS";
	public static final String USER_WCITY = "WCITY";
	public static final String USER_WSTATE = "WSTATE";
	public static final String USER_WZCODE = "WZCODE";
	public static final String USER_WCOUNTRY = "WZCOUNTRY";
	public static final String USER_WCOMPANY = "WCOMPANY";
	public static final String USER_EDITEDBY = "EDITHIMSELF";
	public static final String USER_GROUP = "GID";
	public static final String USER_LASTLOGIN = "LAST_LOGIN";
	public static final String USER_DELETED = "DELETED_FLAG";
	public static final String USER_HIDDEN = "HIDDEN_FLAG";
	public static final String USER_UMESSAGES = "UMESSAGE";
	public static final String USER_LASTCOMM = "LAST_COMMUNITY";
	// ????
	public static final String USER_RESUME = "RESUME";
	public static final String USER_PHOTO = "PHOTO";
	// new attributes
	public static final String USER_PCARD_ID = "PCARD_ID"; // personal card id
	public static final String USER_WCARD_ID = "WCARD_ID"; // work     card id
	public static final String USER_DATEOFBIRTH = "DATEOFBIRTH";
	public static final String USER_PLACE = "PLACE";
	public static final String USER_PADDRESS = "PADDRESS";
	public static final String USER_PLOCATION = "PLOCATION";
	public static final String USER_HPHONE = "HPHONE";
	public static final String USER_MPHONE = "MPHONE";
	public static final String USER_PAGER = "PAGER";
	public static final String USER_INSTANT_MESSENGER = "INSTANT_MESSENGER";
	public static final String USER_MESSENGER_NUMBER = "MESSENGER_NUMBER";
	public static final String USER_HCITY = "HCITY";
	public static final String USER_HSTATE = "HSTATE";
	public static final String USER_HZIPCODE = "HZIPCODE";
	public static final String USER_HCOUNTRY = "HCOUNTRY";
	public static final String USER_COMMID = "COMM_ID";
	public static final String USER_COMPANYID = "COMPANY_ID";
	public static final String USER_AGENTID = "AGENT_ID";
	//public static final String USER_STATE_ID="STATE_ABV";
	public static final String USER_STATE_ID="STATE_ID";
	public static final String USER_STREETNO="STREETNO";
	public static final String USER_STREETDIRECTION="STREETDIRECTION";
	public static final String USER_STREETNAME="STREETNAME";
	public static final String USER_STREETSUFFIX="STREETSUFFIX";
	public static final String USER_STREETUNIT="STREETUNIT";
	public static final String USER_ADDRESS="ADDRESS";
	public static final String USER_C2ARATEINDEX="C2ARATEINDEX";
	public static final String USER_ATS2CRATEINDEX="ATS2CRATEINDEX";
	public static final String USER_RATINGFROMDATE="START_DATE";
	public static final String USER_RATE_ID = "ID";
   
	//other settings
	public static final String  USER_PROFILE_READ_ONLY = "PROFILE_READ_ONLY";
	public static final String  SEND_IMAGES_SURECLOSE = "SEND_IMAGES_SURECLOSE";
	public static final String  SEND_REPORT_SURECLOSE = "SEND_REPORT_SURECLOSE";
	
	public static final String  USER_MODIFIED_BY = "MODIFIED_BY";
	public static final String  USER_DATE_MODIFIED = "DATE_MODIFIED";
	
	
	public static final String TSADMIN = "TSAdmin";
	
	public static final String USER_DISTRIBUTION_TYPE = "DISTRIBUTION_TYPE";
	public static final String USER_DISTRIBUTION_MODE = "DISTRIBUTION_MODE";
	public static final String USER_DISTRIB_ATS = "DISTRIB_ATS";
	public static final String USER_DISTRIB_LINK = "DISTRIB_LINK";
	public static final String USER_INTERACTIVE = "INTERACTIVE";
	public static final String USER_AUTO_ASSIGN_SEARCH_LOCKED = "AAS_LOCKED";
	
	public static final String USER_OUTSOURCE = "OUTSOURCE";
	
	public static final String USER_TEMPLATES = "USER_TEMPLATES";
	public static final String USER_TEMPLATES_DELIM = ",";
	
	public static final String USER_ASSIGN_MODE = "ASSIGN_MODE";
	public static final String USER_SINGLE_SEAT = "SINGLE_SEAT";
	public static final String USER_AUTO_UPDATE = "AUTO_UPDATE";
	public static final String USER_OTHER_FILES_ON_SSF = "OTHER_FILES_ON_SSF";
	
	/* My ATS settings*/
    public static final String  USER_SEARCH_PAGE_STATE  = "SEARCH_PAGE_STATE";
    public static final String  USER_SEARCH_PAGE_COUNTY = "SEARCH_PAGE_COUNTY";
    public static final String  USER_SEARCH_PAGE_AGENT = "SEARCH_PAGE_AGENT";	
    
    public static final String  USER_TSR_SORTBY = "TSR_SORTBY";
    public static final String  USER_TSR_UPPER_LOWER = "TSR_UPPER_LOWER";
    public static final String  USER_TSR_NAME_FORMAT= "TSR_NAME_FORMAT";
    public static final String  USER_TSR_COLORING= "TSR_COLORING";
    public static final String  USER_TSR_PAGINATE= "paginate_tsrindex";
    public static final String  USER_LEGAL_CASE= "legalCase";
    public static final String  USER_VESTING_CASE= "vestingCase";
    public static final String  USER_ADDRESS_CASE= "addressCase";
    public static final String  USER_START_VIEW_DATE_VALUE = "startViewDateValue";
    
    public static final String  USER_DASHBOARD_STATE = "reportState";
    public static final String  USER_DASHBOARD_COUNTY = "reportCounty";
    public static final String  USER_DASHBOARD_ABSTRACTOR = "reportAbstractor";
    public static final String  USER_DASHBOARD_AGENCY = "reportCompanyAgent";
    public static final String  USER_DASHBOARD_AGENT = "reportAgent";
    public static final String  USER_DASHBOARD_STATUS = "reportStatus";
    public static final String  USER_DASHBOARD_AGENT_SELECT_WIDTH = "agentsSelectWidth";
    
    public static final String  USER_DASHBOARD_VIEW = "reportDefaultView";
    public static final String  USER_DASHBOARD_SORTBY= "reportSortBy";
    public static final String  USER_DASHBOARD_SORTDIR= "reportSortDir";
	public static final String  USER_DASHBOARD_START_INTERVAL = "DASHBOARD_START_INTERVAL";
	public static final String  USER_DASHBOARD_END_INTERVAL = "DASHBOARD_END_INTERVAL";
	public static final String  USER_DASHBOARD_ROWS_PER_PAGE	 = "DASHBOARD_ROWS_PER_PAGE";

	
    public static final String  USER_DEFAULT_HOMEPAGE = "DEFAULT_HOMEPAGE";
    public static final String  USER_DEFAULT_CASE = "DEFAULT_CASE";
    public static final String  USER_MY_ATS_READ_ONLY = "MY_ATS_READ_ONLY";
    
    public static final String  USER_RECEIVE_NOTIFICATION  = "receive_notification";
    public static final String  USER_SEARCH_LOG_LINK = "search_log_link";
    public static final String  USER_INVOICE_EDIT_EMAIL = "invoiceEditEmail";
    
    	 /* End My ATS settings */
    
	public static final int EMAIL_TYPE   = 0;
	public static final int ASK_TYPE     = 1;
	public static final int WEBSERV_TYPE = 2;
	
	public static final String EMAIL_TYPE_STRING   = "EMAIL";
	public static final String ASK_TYPE_STRING     = "HTTP";
	public static final String WEBSERV_TYPE_STRING = "WEB-SERVICES";
	
	public static final String PDF_TYPE = "0";
	public static final String TIFF_TYPE = "1";
	
	//	attributes section
	public static final int ID = 0;
	public static final int LOGIN = 1;
	public static final int PASSWD = 2;
	public static final int LASTNAME = 3;
	public static final int FIRSTNAME = 4;
	public static final int MIDDLENAME = 5;

	public static final int COMPANY = 6;
	public static final int EMAIL = 7;
	public static final int ALTEMAIL = 8;
	public static final int PHONE = 9;
	public static final int ALTPHONE = 10;
	public static final int ICQ = 11;
	public static final int AOL = 12;
	public static final int YAHOO = 13;

	public static final int WADDRESS = 14;
	public static final int WCITY = 15;
	public static final int WSTATE = 16;
	public static final int WZCODE = 17;
	public static final int WCONTRY = 18;
	public static final int WCOMPANY = 19;
	public static final int EDITEDBY = 20;
	public static final int GROUP = 21;
	public static final int LASTLOGIN = 22;
	public static final int DELETED = 23;
	public static final int UMESSAGES = 24;
	public static final int LASTCOMM = 25;
	public static final int RESUME = 26;
	public static final int PHOTO = 27;
	// new attributes
	public static final int PCARD_ID = 28; // personal card id
	public static final int WCARD_ID = 29; // work     card id
	public static final int DATEOFBIRTH = 30;
	public static final int PLACE = 31;
	public static final int PADDRESS = 32;
	public static final int PLOCATION = 33;
	public static final int HPHONE = 34;
	public static final int MPHONE = 35;
	public static final int PAGER = 36;
	public static final int INSTANT_MESSENGER = 37;
	public static final int MESSENGER_NUMBER = 38;
	public static final int HCITY = 39;
	public static final int HSTATE = 40;
	public static final int HZIPCODE = 41;
	public static final int HCOUNTRY = 42;
	public static final int COMMID = 43;
	public static final int COMPANYID = 44;
	public static final int AGENTID =45;
	public static final int STATE_ID = 47;
	public static final int STREETNO = 48;
	public static final int STREETDIRECTION =49;
	public static final int STREETNAME = 50;
	public static final int STREETSUFFIX = 51;
	public static final int STREETUNIT	= 52;
	public static final int DISTRIBUTION_TYPE = 53;
	public static final int DISTRIBUTION_MODE = 54;
	public static final int ADDRESS = 55;
	public static final int C2ARATEINDEX = 56;	
	public static final int ATS2CRATEINDEX= 57;
	public static final int RATINGFROMDATE= 58;
	public static final int TEMPLATES = 59;
	public static final int ASSIGN_MODE = 60;
	
    public static final int C2ARATE = 60;
    public static final int A2CRATE = 61;
		
    /* My ATS settings*/
    /* Not here anymore - look for them in MyAtsAttributes.java */
    /* End My ATS settings */
    public static final int  PROFILE_READ_ONLY = 86;
    
    public static final int SINGLE_SEAT = 87;
    public static final int DISTRIB_ATS = 88;
    public static final int DISTRIB_LINK = 89;   
    public static final int INTERACTIVE = 90;
    public static final int OUTSOURCE = 91;
    public static final int AUTO_ASSIGN_SEARCH_LOCKED = 92;
    
    public static final int AUTO_UPDATE = 93;
    
    public static final int IMAGES_SURECLOSE = 94;

    public static final int OTHER_FILES_ON_SSF = 95;
    
    public static final int REPORT_SURECLOSE = 96;
    
    public static final int MODIFIED_BY = 97;
    public static final int DATE_MODIFIED = 98;
    
    private Vector<County> allowedCountyList = new Vector<County>();
    private MyAtsAttributes myAtsAttributes = null;
    private Hashtable<BigDecimal,BigDecimal> ats2CommRates = new Hashtable<BigDecimal,BigDecimal>();
    private Hashtable<BigDecimal,BigDecimal> comm2AgentRates = new Hashtable<BigDecimal,BigDecimal>();
    
    private String userLoginIp = ""; 
    
	protected int getAttrCount() {
		return DATE_MODIFIED + 1;
	}

	public UserAttributes() {
		
	}
	
	public UserAttributes(DatabaseData data, int row){
		this(data,row,true);
	}
	
	public UserAttributes(DatabaseData data, int row, boolean loadAllowedCounties) {
		setID(data, row);
		setLOGIN(data, row);
		//setPASSWD(data, row);
		setFIRSTNAME(data, row);
		setLASTNAME(data, row);
		setMIDDLENAME(data, row);
		setPCARD_ID(data,row);
		setDATEOFBIRTH(data,row );
		setPLACE(data,row );
		setPADDRESS( data, row);
		setPLOCATION(data, row);
		setHCITY(data, row);
		setHSTATE( data, row);
		setHCOUNTRY(data, row);
		setPHONE(data,row);
		setHPHONE(data, row);
		setHZIPCODE( data, row);
		setMPHONE(data, row);
		setPAGER( data, row);		
		setEMAIL(data, row);
		setINSTANT_MESSENGER(data, row);
		setMESSENGER_NUMBER(data, row );
		setWCARD_ID( data, row);
		setWADDRESS( data, row);
		setWCITY( data, row);
		setWSTATE(data, row);
		setWZCODE(data, row);
		setWCOMPANY(data, row);
		setWCONTRY( data, row);
		setCOMPANY(data, row);		
		setCOMMID(data, row);
		setCOMPANYID(data, row);
		setGROUP(data, row);
		setLASTLOGIN(data,row);
		setALTEMAIL(data,row);
		setALTPHONE(data,row);
		setAOL(data,row);
		//setDATEOFBIRTH( data,row);
		setHCITY( data,row);
		setHCOUNTRY(data,row);
		setAGENTID(data,row);
		setSTREETNO(data,row);
		setSTREETDIRECTION(data, row);
		setSTREETNAME(data,row);
		setSTREETSUFFIX(data,row);
		setSTREETUNIT(data,row);
		setSTATE_ID(data,row);
		setDISTRIBUTION_TYPE(data,row);
		setDISTRIBUTION_MODE(data,row);
		setDELIV_TEMPLATES(data,row);
		setADDRESS(data, row);
		//setC2ARATEINDEX(data,row);
		//setATS2CRATEINDEX(data,row);
        
		setASSIGN_MODE(data, row);
		setSINGLE_SEAT(data, row);
		
		setPROFILE_READ_ONLY(data, row);			
		setDISTRIB_ATS(data, row);
		setDISTRIB_LINK(data, row);
		setINTERACTIVE(data, row);
		setOUTSOURCE(data, row);
		setAUTO_ASSIGN_SEARCH_LOCKED(data, row);
		setAUTO_UPDATE(data, row);
		setOTHER_FILES_ON_SSF(data, row);
		
		setSEND_IMAGES_FORCLOSURE(data, row);
		setSEND_REPORT_FORCLOSURE(data, row);
		setMODIFIED_BY(data, row);
		setDATE_MODIFIED(data, row);
		
		if(loadAllowedCounties){
			loadAllowedCounties();
		}
        //this.myAtsAttributes = UserManager.loadMyAtsAttributesForUser(getID());
        //this.myAtsAttributes.setUa(this);
	}

	public void addRate( int rateType, BigDecimal rateValue, BigDecimal countyId )
    {
	    switch( rateType )
        {
        case A2CRATE:
            ats2CommRates.put( countyId, rateValue );
            break;
        case C2ARATE:
            comm2AgentRates.put( countyId, rateValue );
            break;
        }
    }

	public BigDecimal getRate( int rateType, int countyId ){
		return getRate(rateType, new BigDecimal(countyId));
    }
	
    public BigDecimal getRate( int rateType, BigDecimal countyId )
    {
        BigDecimal returnValue = null;
        
        switch( rateType )
        {
        case A2CRATE:
            returnValue = ats2CommRates.get( countyId );
            break;
        case C2ARATE:
            returnValue = comm2AgentRates.get( countyId );
            break;
        }
        
        if( returnValue == null )
        {
            returnValue = new BigDecimal(1);
        }
        
        return returnValue;
    }
    
    public boolean isAllowedCounty( int countyId )
    {
    	if(countyId < 0) {
    		return true;
    	}
        try
        {
            if( UserUtils.isTSAdmin( this ) || UserUtils.isCommAdmin( this ) ||  allowedCountyList.size() == 0 )
            {
                return true;
            }
        }catch(Exception e) {}
                
        for( int i = 0 ; i < allowedCountyList.size() ; i ++ )
        {
            County tmp = allowedCountyList.elementAt( i );
            if( tmp.getCountyId().intValue() == countyId )
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * The same as the <i>public boolean isAllowedCounty( County c )</i>
     * It used the countyId directly so no more sql is needed to get all data for that county
     * @param countyId
     * @return
     */
    public boolean isAllowedCounty(BigInteger countyId )
    {
        if( countyId == null )
        {
            return true;
        }
        
        try
        {
            if( UserUtils.isTSAdmin( this ) || UserUtils.isCommAdmin( this ) ||  allowedCountyList.size() == 0 )
            {
                return true;
            }
        }catch(Exception e) {}
                
        for( int i = 0 ; i < allowedCountyList.size() ; i ++ )
        {
            County tmp = allowedCountyList.elementAt( i );
            if( tmp.getCountyId().longValue() == countyId.longValue()  )
            {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isAllowedState(BigDecimal stateId )
    {
        if( stateId == null )
        {
            return true;
        }
        
        try
        {
            if( UserUtils.isTSAdmin( this ) || UserUtils.isCommAdmin( this ) ||  allowedCountyList.size() == 0 )
            {
                return true;
            }
        }catch(Exception e) {}
                
        for( int i = 0 ; i < allowedCountyList.size() ; i ++ )
        {
            County tmp = allowedCountyList.elementAt( i );
            if( tmp.getState().getStateId().longValue() == stateId.longValue()  )
            {
                return true;
            }
        }
        
        return false;
    }
    
    public void loadAllowedCounties()
    {
        allowedCountyList = DBManager.loadAllowedCountiesForUser( getID() );
    }
    
    public Vector<County> getAllowedCountyList(  )
    {
        return allowedCountyList;
    }
    
    public Vector<County> getAllowedCountyList( String[] countyIdsFilter )
    {
        Vector<County> tempList = new Vector<County>();
        
        int countyId;
        
        for( int i = 0 ; i < allowedCountyList.size() ; i ++ )
        {
            County tmpCounty = allowedCountyList.elementAt( i );
            
            for( int j = 0 ; j < countyIdsFilter.length ; j ++ )
            {
                countyId = -1;
                
                try{
                    countyId = Integer.parseInt( countyIdsFilter[j] );
                }catch( NumberFormatException nfe ){
                    nfe.printStackTrace();
                }
                
                if( countyId == -1 || countyId == tmpCounty.getCountyId().intValue() )
                {
                    tempList.add( tmpCounty );
                    break;
                }
                
            }
        }
        
        return tempList;
    }
    
	//ID = 0;
	public BigDecimal getID() {
		Object id = getAttribute(ID);
		if(id==null)
			return null;
		return new BigDecimal (id.toString());
	}

	public void setID(DatabaseData data, int row) {
		setAttribute(ID, data, USER_ID, row);
	}

	public void setID(BigDecimal id) {
		setAttribute(ID, id);
	}

	//LOGIN = 1;
	public String getLOGIN() {
		return transformNull((String) getAttribute(LOGIN));
	}

	public void setLOGIN(DatabaseData data, int row) {
		setAttribute(LOGIN, data, USER_LOGIN, row);
	}

	public void setLOGIN(String value) {
		setAttribute(LOGIN, value);
	}

	//PASSWD = 2;
	

	public void setPASSWD(DatabaseData data, int row) {
		setAttribute(PASSWD, data, USER_PASSWD, row);
	}

	public void setPASSWD(String value) {
		setAttribute(PASSWD, value);
	}

	//LASTNAME = 3;
	public String getLASTNAME() {
		return transformNull((String) getAttribute(LASTNAME));
	}

	public void setLASTNAME(DatabaseData data, int row) {
		setAttribute(LASTNAME, data, USER_LASTNAME, row);
	}

	public void setLASTNAME(String value) {
		setAttribute(LASTNAME, value);
	}

	//FIRSTNAME = 4;
	public String getFIRSTNAME() {
		return transformNull((String) getAttribute(FIRSTNAME));
	}

	public void setFIRSTNAME(DatabaseData data, int row) {
		setAttribute(FIRSTNAME, data, USER_FIRSTNAME, row);
	}

	public void setFIRSTNAME(String value) {
		setAttribute(FIRSTNAME, value);
	}

	// MIDDLENAME = 5;
	public String getMIDDLENAME() {
		return transformNull((String) getAttribute(MIDDLENAME));
	}

	public void setMIDDLENAME(DatabaseData data, int row) {
		setAttribute(MIDDLENAME, data, USER_MIDDLENAME, row);
	}

	public void setMIDDLENAME(String value) {
		setAttribute(MIDDLENAME, value);
	}

	// COMPANY = 6;
	public String getCOMPANY() {
		return transformNull((String) getAttribute(COMPANY));
	}

	public void setCOMPANY(DatabaseData data, int row) {
		setAttribute(COMPANY, data, USER_COMPANY, row);
	}

	public void setCOMPANY(String value) {
		setAttribute(COMPANY, value);
	}

	// EMAIL = 7;
	public String getEMAIL() {
		return transformNull((String) getAttribute(EMAIL));
	}

	public void setEMAIL(DatabaseData data, int row) {
		setAttribute(EMAIL, data, USER_EMAIL, row);
	}

	public void setEMAIL(String value) {
		setAttribute(EMAIL, value);
	}

	// ALTEMAIL = 8;
	public String getALTEMAIL() {
		return transformNull((String) getAttribute(ALTEMAIL));
	}

	public void setALTEMAIL(DatabaseData data, int row) {
		setAttribute(ALTEMAIL, data, USER_ALTEMAIL, row);
	}

	public void setALTEMAIL(String value) {
		setAttribute(ALTEMAIL, value);
	}

	// PHONE = 9;
	public String getPHONE() {
		return transformNull((String) getAttribute(PHONE));
	}

	public void setPHONE(DatabaseData data, int row) {
		setAttribute(PHONE, data, USER_PHONE, row);
	}

	public void setPHONE(String value) {
		setAttribute(PHONE, value);
	}
	
	
	// ALTPHONE = 10;
	public String getALTPHONE() {
		return transformNull((String) getAttribute(ALTPHONE));
	}

	public void setALTPHONE(DatabaseData data, int row) {
		setAttribute(ALTPHONE, data, USER_ALTPHONE, row);
	}

	public void setALTPHONE(String value) {
		setAttribute(ALTPHONE, value);
	}

	// ICQ = 11;
	public String getICQ() {
		return transformNull((String) getAttribute(ICQ));
	}

	public void setICQ(DatabaseData data, int row) {
		setAttribute(ICQ, data, USER_ICQ, row);
	}

	public void setICQ(String value) {
		setAttribute(ICQ, value);
	}

	// AOL = 12;
	public String getAOL() {
		return transformNull((String) getAttribute(AOL));
	}

	public void setAOL(DatabaseData data, int row) {
		setAttribute(AOL, data, USER_AOL, row);
	}

	public void setAOL(String value) {
		setAttribute(AOL, value);
	}

	// YAHOO = 13;
	public String getYAHOO() {
		return transformNull((String) getAttribute(YAHOO));
	}

	public void setYAHOO(DatabaseData data, int row) {
		setAttribute(YAHOO, data, USER_YAHOO, row);
	}

	public void setYAHOO(String value) {
		setAttribute(YAHOO, value);
	}

	// WADDRESS = 14;
	public String getWADDRESS() {
		return transformNull((String) getAttribute(WADDRESS));
	}

	public void setWADDRESS(DatabaseData data, int row) {
		setAttribute(WADDRESS, data, USER_WADDRESS, row);
	}

	public void setWADDRESS(String value) {
		setAttribute(WADDRESS, value);
	}

	// WCITY = 15;
	public String getWCITY() {
		return transformNull((String) getAttribute(WCITY));
	}

	public void setWCITY(DatabaseData data, int row) {
		setAttribute(WCITY, data, USER_WCITY, row);
	}

	public void setWCITY(String value) {
		setAttribute(WCITY, value);
	}

	// WSTATE = 16;
	public String getWSTATE() {
		return transformNull((String) getAttribute(WSTATE));
	}

	public void setWSTATE(DatabaseData data, int row) {
		setAttribute(WSTATE, data, USER_WSTATE, row);
	}

	public void setWSTATE(String value) {
		setAttribute(WSTATE, value);
	}

	// WZCODE = 17;
	public String getWZCODE() {
		return transformNull((String) getAttribute(WZCODE));
	}

	public void setWZCODE(DatabaseData data, int row) {
		setAttribute(WZCODE, data, USER_WZCODE, row);
	}

	public void setWZCODE(String value) {
		setAttribute(WZCODE, value);
	}

	//  WCONTRY = 18;
	public String getWCONTRY() {
		return transformNull((String) getAttribute(WCONTRY));
	}

	public void setWCONTRY(DatabaseData data, int row) {
		setAttribute(WCONTRY, data, USER_WCOUNTRY, row);
	}

	public void setWCONTRY(String value) {
		setAttribute(WCONTRY, value);
	}

	// WCOMPANY = 19;
	public String getWCOMPANY() {
		return transformNull((String) getAttribute(WCOMPANY));
	}

	public void setWCOMPANY(DatabaseData data, int row) {
		setAttribute(WCOMPANY, data, USER_WCOMPANY, row);
	}

	public void setWCOMPANY(String value) {
		setAttribute(WCOMPANY, value);
	}

	// EDITEDBY = 20;
	public String getEDITEDBY() {
		return transformNull((String) getAttribute(EDITEDBY));
	}

	public void setEDITEDBY(DatabaseData data, int row) {
		setAttribute(EDITEDBY, data, USER_EDITEDBY, row);
	}

	public void setEDITEDBY(String value) {
		setAttribute(EDITEDBY, value);
	}

	// GROUP = 21;
	public BigDecimal getGROUP() {
		return new BigDecimal(getAttribute(GROUP).toString());
	}

	public void setGROUP(DatabaseData data, int row) {
		setAttribute(GROUP, data, USER_GROUP, row);
	}

	public void setGROUP(BigDecimal value) {
		setAttribute(GROUP, value);
	}

	// LASTLOGIN = 22;
	public BigDecimal getLASTLOGIN() {
		try{
			return new BigDecimal(getAttribute(LASTLOGIN).toString());
		}
		catch( Exception e ){
			e.printStackTrace();
			return null;
		}
	}

	public void setLASTLOGIN(DatabaseData data, int row) {
		setAttribute(LASTLOGIN, data, USER_LASTLOGIN, row);
	}

	public void setLASTLOGIN(BigDecimal value) {
		setAttribute(LASTLOGIN, value);
	}

	// DELETED = 23;
	public BigDecimal getDELETED() {
		return new BigDecimal(getAttribute(DELETED).toString());
	}

	public void setDELETED(DatabaseData data, int row) {
		setAttribute(DELETED, data, USER_DELETED, row);
	}

	public void setDELETED(BigDecimal value) {
		setAttribute(DELETED, value);
	}

	// UMESSAGES = 24;
	public BigDecimal getUMESSAGES() {
		return new BigDecimal(getAttribute(UMESSAGES).toString());
	}

	public void setUMESSAGES(DatabaseData data, int row) {
		setAttribute(UMESSAGES, data, USER_UMESSAGES, row);
	}

	public void setUMESSAGES(BigDecimal value) {
		setAttribute(UMESSAGES, value);
	}

	// LASTCOMM = 25;
	public BigDecimal getLASTCOMM() {
		return new BigDecimal(getAttribute(LASTCOMM).toString());
	}

	public void setLASTCOMM(DatabaseData data, int row) {
		setAttribute(LASTCOMM, data, USER_LASTCOMM, row);
	}

	public void setLASTCOMM(BigDecimal value) {
		setAttribute(LASTCOMM, value);
	}

	/*
	// RESUME = 26;
	public String getRESUME() {
		return transformNull((String) getAttribute(RESUME);
	}
	
	public void setRESUME(DatabaseData data, int row) {
		setAttribute(RESUME, data, USER_RESUME, row);
	}
	
	// PHOTO = 27;
	public String getPHOTO() {
		return transformNull((String) getAttribute(PHOTO);
	}
	
	public void setPHOTO(DatabaseData data, int row) {
		setAttribute(PHOTO, data, USER_PHOTO, row);
	}
	*/

	// PCARD_ID = 28;
	public String getPCARD_ID() {
		return transformNull((String) getAttribute(PCARD_ID));
	}

	public void setPCARD_ID(DatabaseData data, int row) {
		setAttribute(PCARD_ID, data, USER_PCARD_ID, row);
	}

	public void setPCARD_ID(String value) {
		setAttribute(PCARD_ID, value);
	}

	// WCARD_ID = 29;
	public String getWCARD_ID() {
		return transformNull((String) getAttribute(WCARD_ID));
	}

	public void setWCARD_ID(DatabaseData data, int row) {
		setAttribute(WCARD_ID, data, USER_WCARD_ID, row);
	}

	public void setWCARD_ID(String value) {
		setAttribute(WCARD_ID, value);
	}

	// DATEOFBIRTH = 30;
	public BigDecimal getDATEOFBIRTH() {
		return new BigDecimal(getAttribute(DATEOFBIRTH).toString());
	}

	public void setDATEOFBIRTH(DatabaseData data, int row) {
		setAttribute(DATEOFBIRTH, data, USER_DATEOFBIRTH, row);
	}

	public void setDATEOFBIRTH(Long value) {
		setAttribute(DATEOFBIRTH, value);
	}

	// PLACE = 31;
	public String getPLACE() {
		return transformNull((String) getAttribute(PLACE));
	}

	public void setPLACE(DatabaseData data, int row) {
		setAttribute(PLACE, data, USER_PLACE, row);
	}

	public void setPLACE(String value) {
		setAttribute(PLACE, value);
	}

	// PADDRESS = 32;
	public String getPADDRESS() {
		return transformNull((String) getAttribute(PADDRESS));
	}

	public void setPADDRESS(DatabaseData data, int row) {
		setAttribute(PADDRESS, data, USER_PADDRESS, row);
	}

	public void setPADDRESS(String value) {
		setAttribute(PADDRESS, value);
	}

	// PLOCATION = 33;
	public String getPLOCATION() {
		return transformNull((String) getAttribute(PLOCATION));
	}

	public void setPLOCATION(DatabaseData data, int row) {
		setAttribute(PLOCATION, data, USER_PLOCATION, row);
	}

	public void setPLOCATION(String value) {
		setAttribute(PLOCATION, value);
	}

	// HPHONE = 34;
	public String getHPHONE() {
		return transformNull((String) getAttribute(HPHONE));
	}

	public void setHPHONE(DatabaseData data, int row) {
		setAttribute(HPHONE, data, USER_HPHONE, row);
	}

	public void setHPHONE(String value) {
		setAttribute(HPHONE, value);
	}

	// MPHONE = 35;
	public String getMPHONE() {
		return transformNull((String) getAttribute(MPHONE));
	}

	public void setMPHONE(DatabaseData data, int row) {
		setAttribute(MPHONE, data, USER_MPHONE, row);
	}

	public void setMPHONE(String value) {
		setAttribute(MPHONE, value);
	}

	// PAGER = 36;
	public String getPAGER() {
		return transformNull((String) getAttribute(PAGER));
	}

	public void setPAGER(DatabaseData data, int row) {
		setAttribute(PAGER, data, USER_PAGER, row);
	}

	public void setPAGER(String value) {
		setAttribute(PAGER, value);
	}

	// INSTANT_MESSENGER = 37;
	public String getINSTANT_MESSENGER() {
		return transformNull((String) getAttribute(INSTANT_MESSENGER));
	}

	public void setINSTANT_MESSENGER(DatabaseData data, int row) {
		setAttribute(INSTANT_MESSENGER, data, USER_INSTANT_MESSENGER, row);
	}

	public void setINSTANT_MESSENGER(String value) {
		setAttribute(INSTANT_MESSENGER, value);
	}

	// MESSENGER_NUMBER = 38;
	public String getMESSENGER_NUMBER() {
		return transformNull((String) getAttribute(MESSENGER_NUMBER));
	}

	public void setMESSENGER_NUMBER(DatabaseData data, int row) {
		setAttribute(MESSENGER_NUMBER, data, USER_MESSENGER_NUMBER, row);
	}

	public void setMESSENGER_NUMBER(String value) {
		setAttribute(MESSENGER_NUMBER, value);
	}

	// HCITY = 39;
	public String getHCITY() {
		return transformNull((String) getAttribute(HCITY));
	}

	public void setHCITY(DatabaseData data, int row) {
		setAttribute(HCITY, data, USER_HCITY, row);
	}

	public void setHCITY(String value) {
		setAttribute(HCITY, value);
	}

	// HSTATE = 40;
	public String getHSTATE() {
		return transformNull((String) getAttribute(HSTATE));
	}

	public void setHSTATE(DatabaseData data, int row) {
		setAttribute(HSTATE, data, USER_HSTATE, row);
	}

	public void setHSTATE(String value) {
		setAttribute(HSTATE, value);
	}

	// HZIPCODE = 41;
	public String getHZIPCODE() {
		return transformNull((String) getAttribute(HZIPCODE));
	}

	public void setHZIPCODE(DatabaseData data, int row) {
		setAttribute(HZIPCODE, data, USER_HZIPCODE, row);
	}

	public void setHZIPCODE(String value) {
		setAttribute(HZIPCODE, value);
	}

	// HCOUNTRY = 42;
	public String getHCOUNTRY() {
		return transformNull((String) getAttribute(HCOUNTRY));
	}
	

	public void setHCOUNTRY(DatabaseData data, int row) {
		setAttribute(HCOUNTRY, data, USER_HCOUNTRY, row);
	}

	public void setHCOUNTRY(String value) {
		setAttribute(HCOUNTRY, value);
	}

	// COMMID = 43
	public BigDecimal getCOMMID() {
		return new BigDecimal(getAttribute(COMMID).toString());
	}

	public void setCOMMID(DatabaseData data, int row) {
		setAttribute(COMMID, data, USER_COMMID, row);
	}

	public void setCOMMID(BigDecimal value) {
		setAttribute(COMMID, value);
	}

	//COMPANYID = 44
	public BigDecimal getCOMPANYID() {
		return new BigDecimal(getAttribute(COMPANYID).toString());
	}

	public void setCOMPANYID(DatabaseData data, int row) {
		setAttribute(COMPANYID, data, USER_COMPANYID, row);
	}

	public void setCOMPANYID(BigDecimal value) {
		setAttribute(COMPANYID, value);
	}

	//AGENT_ID = 44
	public BigDecimal getAGENTID() {
		if(getAttribute(AGENTID)==null)
			return null;
		return new BigDecimal(getAttribute(AGENTID).toString());
	}

	public void setAGENTID(DatabaseData data, int row) {
		setAttribute(AGENTID, data, USER_AGENTID, row);
	}

	public void setAGENTID(BigDecimal value) {
		setAttribute(AGENTID, value);
	}

	public BigDecimal getDASHBOARD_ROWS_PER_PAGE() {
		return getMyAtsAttributes().getDASHBOARD_ROWS_PER_PAGE();
	}

	public String getDEFAULT_HOMEPAGE() {
		return getMyAtsAttributes().getDEFAULT_HOMEPAGE();
	}

	//PROFILE_READ_ONLY = 86
	public Integer getPROFILE_READ_ONLY() {
		if(getAttribute(PROFILE_READ_ONLY)==null)
			return 0;
		return new Integer(getAttribute(PROFILE_READ_ONLY).toString());
	}

	public void setPROFILE_READ_ONLY(DatabaseData data, int row) {
		setAttribute(PROFILE_READ_ONLY, data, USER_PROFILE_READ_ONLY, row);
	}

	public void setPROFILE_READ_ONLY(Integer value) {
		setAttribute(PROFILE_READ_ONLY, value);
	}
	
	/**
	 * Returns this SEND_IMAGES_FORCLOSURE from ts_user, or 0 in case this flag is not set 
	 * 
	 * @return 0
	 */
	public Integer getSEND_IMAGES_FORCLOSURE() {
		try{
			if(getAttribute(IMAGES_SURECLOSE)!=null)
				return new Integer(getAttribute(IMAGES_SURECLOSE).toString());
		}catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public void setSEND_IMAGES_FORCLOSURE(DatabaseData data, int row) {
		setAttribute(IMAGES_SURECLOSE, data, SEND_IMAGES_SURECLOSE, row);
	}

	public void setSEND_IMAGES_FORCLOSURE(Integer value) {
		setAttribute(IMAGES_SURECLOSE, value);
	}
	
	public Integer getSEND_REPORT_FORCLOSURE() {
		try{
			if(getAttribute(REPORT_SURECLOSE)!=null)
				return new Integer(getAttribute(REPORT_SURECLOSE).toString());
		}catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public void setSEND_REPORT_FORCLOSURE(DatabaseData data, int row) {
		setAttribute(REPORT_SURECLOSE, data, SEND_REPORT_SURECLOSE, row);
	}

	public void setSEND_REPORT_FORCLOSURE(Integer value) {
		setAttribute(REPORT_SURECLOSE, value);
	}
	
	public String getLastModified(){
		if(getMODIFIED_BY() == null || (Integer) getMODIFIED_BY() == -1){
			return "";
		}
		
		UserAttributes ua = UserManager.getUser(new BigDecimal(getMODIFIED_BY()));
		
		String modBy = ua.getUserFullName();
		Date dateMod = (Date) attrValues[DATE_MODIFIED];
		
		if(StringUtils.isNotBlank(modBy) && dateMod!=null){
			modBy +="("+ua.getLOGIN()+")";			
			return modBy + " on " + dateMod;
		}
		
		return "";
	}
	
	public void setLastModified(int userID){
		setAttribute(MODIFIED_BY, new BigInteger(Integer.toString(userID)));
		setAttribute(DATE_MODIFIED, new Date());
	}
	
	public Integer getMODIFIED_BY(){
		if(getAttribute(MODIFIED_BY) == null){
			return null;			
		}

		return ((BigInteger) getAttribute(MODIFIED_BY)).intValue();
	}
	
	public void setMODIFIED_BY(DatabaseData data, int row) {
		setAttribute(MODIFIED_BY, data, USER_MODIFIED_BY, row);
	}
	
	public Date getDATE_MODIFIED(){
		if(getAttribute(DATE_MODIFIED) == null){
			return new Date();
		}
		
		return (Date) getAttribute(DATE_MODIFIED);
	}
	
	public void setDATE_MODIFIED(DatabaseData data, int row) {
		setAttribute(DATE_MODIFIED, data, USER_DATE_MODIFIED, row);
	}
	
	public String getUserFullName() {
		return (String)attrValues[FIRSTNAME] + " " + (String)attrValues[LASTNAME];
	}
	
	
	public String getSTREETNO() {
		return (String) getAttribute(STREETNO);
	}
	public void setSTREETNO(DatabaseData data, int row) {
		setAttribute(STREETNO, data, USER_STREETNO, row);
	}
	public void setSTREETNO(String value) {
		setAttribute(STREETNO, value);
	}	

	
	public String getSTREETDIRECTION() {
		return transformNull((String) getAttribute(STREETDIRECTION));
	}
	public void setSTREETDIRECTION(DatabaseData data, int row) {
		setAttribute(STREETDIRECTION, data, USER_STREETDIRECTION, row);
	}
	public void setSTREETDIRECTION(String value) {
		setAttribute(STREETDIRECTION, value);
	}	
	
	
	public String getSTREETNAME() {
		return transformNull((String) getAttribute(STREETNAME));
	}
	public void setSTREETNAME(DatabaseData data, int row) {
		setAttribute(STREETNAME, data, USER_STREETNAME, row);
	}
	public void setSTREETNAME(String value) {
		setAttribute(STREETNAME, value);
	}	
	
	
	public String getSTREETSUFFIX() {
		return transformNull((String) getAttribute(STREETSUFFIX));
	}
	public void setSTREETSUFFIX(DatabaseData data, int row) {
		setAttribute(STREETSUFFIX, data, USER_STREETSUFFIX, row);
	}
	public void setSTREETSUFFIX(String value) {
		setAttribute(STREETSUFFIX, value);
	}		

	
	public String getSTREETUNIT() {
		return transformNull((String) getAttribute(STREETUNIT));
	}
	public void setSTREETUNIT(DatabaseData data, int row) {
		setAttribute(STREETUNIT, data, USER_STREETUNIT, row);
	}
	public void setSTREETUNIT(String value) {
		setAttribute(STREETUNIT, value);
	}
	
	
	public String getDISTRIBUTION_TYPE() {
		return transformNull((String) getAttribute(DISTRIBUTION_TYPE));
	}
	public void setDISTRIBUTION_TYPE(DatabaseData data, int row) {
		setAttribute(DISTRIBUTION_TYPE, data, USER_DISTRIBUTION_TYPE, row);
	}
	public void setDISTRIBUTION_TYPE(String value) {
		setAttribute(DISTRIBUTION_TYPE, value);
	}
	
	
	public String getADDRESS() {
		return transformNull((String) getAttribute(ADDRESS));
	}
	public void setADDRESS(DatabaseData data, int row) {
		setAttribute(ADDRESS, data, USER_ADDRESS, row);
	}
	public void setADDRESS(String value) {
		setAttribute(ADDRESS, value);
	}
	
	public void setDISTRIBUTION_MODE(DatabaseData data, int row) {
		setAttribute(DISTRIBUTION_MODE, data, USER_DISTRIBUTION_MODE, row);
	}
	public void setDISTRIBUTION_MODE(String value) {
		setAttribute(DISTRIBUTION_MODE, value);
	}
	public String getDISTRIBUTION_MODE() {
		return transformNull((String) getAttribute(DISTRIBUTION_MODE));
	}
	
	public void setDELIV_TEMPLATES(DatabaseData data, int row) {
		setAttribute(TEMPLATES, data, USER_TEMPLATES, row);
	}
	public void setTEMPLATES(String value) {
		setAttribute(TEMPLATES, value);
	}
	public String getTEMPLATES() {
		return transformNull((String) getAttribute(TEMPLATES));
	}
	
	public BigDecimal getSTATE_ID() {
		if(getAttribute(STATE_ID)==null)
			return new BigDecimal(0);
		else
			return new BigDecimal(getAttribute(STATE_ID).toString());
	}
	
	public void setSTATE_ID(String stateId) {
		setAttribute(STATE_ID, stateId);
	}
	public void setSTATE_ID(DatabaseData data, int row) {
		setAttribute(STATE_ID, data, USER_STATE_ID, row);
	}
	
	
	public BigDecimal getC2ARATEINDEX() {
		return new BigDecimal(getAttribute(C2ARATEINDEX).toString());
	}
	
	public void setC2ARATEINDEX(BigDecimal index) {
		setAttribute(C2ARATEINDEX, index);
	}
	
	public void setC2ARATEINDEX(DatabaseData data, int row) {
		setAttribute(C2ARATEINDEX, data, USER_C2ARATEINDEX, row);
	}

  
	public BigDecimal getATS2CRATEINDEX() {
		return new BigDecimal(getAttribute(ATS2CRATEINDEX).toString());
	}
	
	public void setATS2CRATEINDEX(BigDecimal index) {
		setAttribute(ATS2CRATEINDEX, index);
	}
	public void setATS2CRATEINDEX(DatabaseData data, int row) {
		setAttribute(ATS2CRATEINDEX, data, USER_ATS2CRATEINDEX, row);
	}  
	
  
	public ImageI.IType getDistributionType(){
		String distribType = getDISTRIBUTION_TYPE();
		if(distribType.equals(UserAttributes.PDF_TYPE))
			return ImageI.IType.PDF;
		else
			return ImageI.IType.TIFF;		
	}
	
	public boolean hasDistributionMode(int lookingForMode){
		String distributionMode = getDISTRIBUTION_MODE();
		if(( distributionMode.length() > lookingForMode ) 
						&& ( distributionMode.charAt(lookingForMode)=='1' ))
			return true;
		return false;
	}	

	public Date getRATINGFROMDATE() {
		return (Date)getAttribute(RATINGFROMDATE);
	}

	public void setRATINGFROMDATE(Date date) {
		setAttribute(RATINGFROMDATE,date);
	}
	
	public synchronized Object clone() {
	    
	    UserAttributes userAttributes = (UserAttributes) super.clone();
		return userAttributes;
	}

	public String getUserLoginIp() {
		return userLoginIp;
	}

	public void setUserLoginIp(String userLoginIp) {
		this.userLoginIp = userLoginIp;
	}
	public boolean isTSAdmin()
	{
		return (getGROUP().intValue() == GroupAttributes.TA_ID);
	}
	
	public boolean isTSCAdmin(){
		return (getGROUP().intValue() == GroupAttributes.CCA_ID) ;
	}

	public boolean isCommAdmin()
	{			
		return (getGROUP().intValue() == GroupAttributes.CA_ID);
	}

	public boolean isAgent()
	{			
		return (getGROUP().intValue() == GroupAttributes.AG_ID);
	
	}
	
	public boolean isAbstractor()
	{			
		return (getGROUP().intValue() == GroupAttributes.ABS_ID);

	}
	
	public boolean isAdmin(){
		return (isTSAdmin() || isTSCAdmin() || isCommAdmin());
	}
	
	public void setASSIGN_MODE(String value) {
		setAttribute(ASSIGN_MODE, value);
	}
	
	public void setASSIGN_MODE(DatabaseData data, int row) {        
		setAttribute(ASSIGN_MODE, data, USER_ASSIGN_MODE, row);
	}
	
	public void setSINGLE_SEAT(String value){
		setAttribute(SINGLE_SEAT, value);
	}
	
	public void setSINGLE_SEAT(DatabaseData data, int row){
		setAttribute(SINGLE_SEAT, data, USER_SINGLE_SEAT, row);
	}
	
	public boolean isSINGLE_SEAT(){
		return getSINGLE_SEAT() != 0;
	}
	
	public int getSINGLE_SEAT(){
		Object obj = getAttribute(SINGLE_SEAT);
		if(obj instanceof Integer){
			return (Integer)obj;
		} else if(obj instanceof String) {
			return Integer.parseInt((String)obj);
		} else {
			return 0;
		}
	}
	
	public void setAUTO_UPDATE(String value){
		setAttribute(AUTO_UPDATE, value);
	}
	
	public void setAUTO_UPDATE(DatabaseData data, int row){
		setAttribute(AUTO_UPDATE, data, USER_AUTO_UPDATE, row);
	}
	
	public boolean isAUTO_UPDATE(){
		return getAUTO_UPDATE() != 0;
	}
	
	public int getAUTO_UPDATE(){
		Object obj = getAttribute(AUTO_UPDATE);
		if(obj instanceof Integer){
			return (Integer)obj;
		} else if(obj instanceof String) {
			return Integer.parseInt((String)obj);
		} else {
			return 0;
		}
	}
	
	public void setOTHER_FILES_ON_SSF(String value){
		setAttribute(OTHER_FILES_ON_SSF, value);
	}
	
	public void setOTHER_FILES_ON_SSF(DatabaseData data, int row){
		setAttribute(OTHER_FILES_ON_SSF, data, USER_OTHER_FILES_ON_SSF, row);
	}
	
	public boolean isOTHER_FILES_ON_SSF(){
		return getOTHER_FILES_ON_SSF() != 0;
	}
	
	public int getOTHER_FILES_ON_SSF(){
		Object obj = getAttribute(OTHER_FILES_ON_SSF);
		if(obj instanceof Integer){
			return (Integer)obj;
		} else if(obj instanceof String) {
			return Integer.parseInt((String)obj);
		} else {
			return 0;
		}
	}
	
	public int getASSIGN_MODE(){
		Object obj = getAttribute(ASSIGN_MODE);
		if(obj instanceof Integer){
			return (Integer)obj;
		} else if(obj instanceof String) {
			return Integer.parseInt((String)obj);
		} else {
			return 0;
		}
	}
	
	public String getASSIGN_MODE_STRING(){

		switch(getASSIGN_MODE()){
			case EMAIL_TYPE: return EMAIL_TYPE_STRING;
			case ASK_TYPE: return ASK_TYPE_STRING;
			case WEBSERV_TYPE: return WEBSERV_TYPE_STRING;
		}
		return "n/a";
		
	}
	
	public String getAssignModeSelect(){
		
		StringBuilder sb = new StringBuilder();
		sb.append("<SELECT name=\"" + USER_ASSIGN_MODE + "\">");
		
		int mode = getASSIGN_MODE();			
		
		sb.append("<OPTION value=\"" + EMAIL_TYPE + "\"");
		if(mode == EMAIL_TYPE){sb.append(" SELECTED");}
		sb.append(">" + EMAIL_TYPE_STRING + "</OPTION>");
		
		sb.append("<OPTION value=\"" + ASK_TYPE + "\"");
		if(mode == ASK_TYPE){ sb.append(" SELECTED");}
		sb.append(">" + ASK_TYPE_STRING + "</OPTION>");

		sb.append("<OPTION value=\"" + WEBSERV_TYPE + "\"");
		if(mode == WEBSERV_TYPE){ sb.append(" SELECTED");}
		sb.append(">" + WEBSERV_TYPE_STRING + "</OPTION>");
		
		sb.append("</SELECT>");
		
		return sb.toString();
	}
	
	public void setDISTRIB_ATS(String value){
		setAttribute(DISTRIB_ATS, value);
	}
	
	public void setDISTRIB_ATS(DatabaseData data, int row){
		setAttribute(DISTRIB_ATS, data, USER_DISTRIB_ATS, row);
	}
	
	public void setDISTRIB_LINK(String value){
		setAttribute(DISTRIB_LINK, value);
	}
	
	public void setDISTRIB_LINK(DatabaseData data, int row){
		setAttribute(DISTRIB_LINK, data, USER_DISTRIB_LINK, row);
	}
	
	public void setINTERACTIVE(boolean interactive){
		setAttribute(INTERACTIVE, interactive);
	}
	
	public void setINTERACTIVE(DatabaseData data, int row){
		setAttribute(INTERACTIVE, data, USER_INTERACTIVE, row);
	}
	public void setAUTO_ASSIGN_SEARCH_LOCKED(DatabaseData data, int row){
		setAttribute(AUTO_ASSIGN_SEARCH_LOCKED, data, USER_AUTO_ASSIGN_SEARCH_LOCKED, row);
	}
	public void setAUTO_ASSIGN_SEARCH_LOCKED(boolean locked){
		setAttribute(AUTO_ASSIGN_SEARCH_LOCKED, locked);
	}
	
	public void setOUTSOURCE(String outsource){
		setAttribute(OUTSOURCE, outsource);
	}
	
	public void setOUTSOURCE(DatabaseData data, int row){
		setAttribute(OUTSOURCE, data, USER_OUTSOURCE, row);
	}
	
	public int getDISTRIB_ATS(){
		Object obj = getAttribute(DISTRIB_ATS);
		if(obj instanceof Integer){
			return (Integer)obj;
		} else if(obj instanceof String) {
			return Integer.parseInt((String)obj);
		} else {
			return 0;
		}
	}
	
	public boolean isAutoExportTsr(){
		if(getDISTRIB_ATS()!=0)
			return true;
		
		return false;
	}

	public int getDISTRIB_LINK(){
		Object obj = getAttribute(DISTRIB_LINK);
		if(obj instanceof Integer){
			return (Integer)obj;
		} else if(obj instanceof String) {
			return Integer.parseInt((String)obj);
		} else {
			return 0;
		}
	}
	
	public boolean isINTERACTIVE(){
		Object obj = getAttribute(INTERACTIVE);
		if(obj ==null){
			return false;
		}
		if(obj instanceof Boolean){
			return (Boolean)obj;
		} else if(obj instanceof String) {
			return Boolean.parseBoolean((String)obj);
		} else {
			return false;
		}
	}
	public boolean isAUTO_ASSIGN_SEARCH_LOCKED(){
		Object obj = getAttribute(AUTO_ASSIGN_SEARCH_LOCKED);
		if(obj ==null){
			return true;
		}
		if(obj instanceof Boolean){
			return (Boolean)obj;
		} else if(obj instanceof String) {
			return Boolean.parseBoolean((String)obj);
		} else {
			return false;
		}
	}
	
	public String getOUTSOURCE(){
		Object obj = getAttribute(OUTSOURCE);
		if(obj==null){
			return UserAttributes.OS_DISABLED;
		}
		return obj.toString();
	}
	
	public MyAtsAttributes getMyAtsAttributes() {
		synchronized (this) {
			if (myAtsAttributes == null) {
				this.myAtsAttributes = UserManager
						.loadMyAtsAttributesForUser(getID());
				this.myAtsAttributes.setUa(this);
			}
		}
		return myAtsAttributes;
	}

	public void setMyAtsAttributes(MyAtsAttributes myAtsAttributes) {
		this.myAtsAttributes = myAtsAttributes;
	}

	@Override
	public UserAttributes mapRow(ResultSet rs, int rowNum) throws SQLException {
		UserAttributes abs = new UserAttributes();
    	abs.setID(new BigDecimal(rs.getLong(DBConstants.FIELD_USER_ID)));
    	abs.setFIRSTNAME(rs.getString(DBConstants.FIELD_USER_FIRST_NAME));
    	abs.setLASTNAME(rs.getString(DBConstants.FIELD_USER_LAST_NAME));
    	abs.setLOGIN(rs.getString(DBConstants.FIELD_USER_LOGIN));
        return abs;
	}

	/**
	 * Returns a nicer name
	 */
	public String getNiceName(){
		return getFIRSTNAME() + " " + getMIDDLENAME() + " " + getLASTNAME() + "(" + getEMAIL() + ")";
	}

	public boolean isAdministratorToACommunity() {
		return DBManager.getSimpleTemplate().queryForInt(
				"select count(*) from ts_community where comm_admin = ?", getID().longValue()) > 0;
	}
	
}

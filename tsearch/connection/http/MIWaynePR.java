 package ro.cst.tsearch.connection.http;

import java.util.Arrays;
import java.util.Vector;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;


public class MIWaynePR extends HTTPSite {

	private boolean loggingIn = false;
	
	public LoginResponse onLogin() {
		
		loggingIn = true;
		
		setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIWaynePR" );
		
		//go to disclaimer page
		HTTPRequest req = new HTTPRequest( "http://public.wcpc.us/pa/" );
		HTTPResponse res = process( req );
		
		String response = res.getResponseAsString();
		
		//accept disclaimer
		req = new HTTPRequest( "http://public.wcpc.us/pa/pa.urd/pamw6500.display" );
		res = process( req );
		
		response = res.getResponseAsString();
		
		loggingIn = false;
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
    public void onBeforeRequest(HTTPRequest req)
    {
    	if( loggingIn ){
    		return;
    	}
    	String[] paramPos = null;
    	
    	String getUrl = req.getURL();
    	
    	if( getUrl.indexOf( "caseNum=" ) >= 0 ){
    		getUrl = getUrl.replace( "caseNum=" , "");
    		req.URL = getUrl;
    	}
    	
    	paramPos =new String[] {"CASE_NBR.PAPROFILE.PAM","SEARCH_BUTTON.PAPROFILE.PAM","CASE_CD.PAPROFILE.PAM"
    			,"BEGIN_DT_LBL.PAPROFILE.PAM","ACTN_CD_PRF.SRCHPR02.PAM","MIDDLE_NAME.PAPROFILE.PAM","CURRENT_YEAR.PROFILE.CRTV","DOB_LBL.PAPROFILE.PAM","PTY_CD_LBL.PAPROFILE.PAM",
    			"END_DT_LBL.PAPROFILE.PAM" ,"FIRST_NAME.PAPROFILE.PAM","%25.PROFILE.CRTV.1.","CASE_CD_LBL.PAPROFILE.PAM","TICKET_NBR.PAPROFILE.PAM","ACTN_CD.PAPROFILE.PAM","ACTN_CD_LBL.PAPROFILE.PAM",
    			"COMPANY_NAME_LBL.PAPROFILE.PAM","SSN.PAPROFILE.PAM","CASE_NBR_LBL.PAPROFILE.PAM","LAST_NAME.PAPROFILE.PAM","DOB.PAPROFILE.PAM","STAT_CD_LBL.PAPROFILE.PAM","TICKET_NBR_LBL.PAPROFILE.PAM","DOD_LBL.PAPROFILE.PAM"
    			,"DOD.PAPROFILE.PAM","%23CRC.SRCHPR02.PAM.1-1-1.","END_DT.PAPROFILE.PAM","LAST_NAME_LBL.PAPROFILE.PAM","BEGIN_DT.PAPROFILE.PAM",
    			"FIRST_NAME_LBL.PAPROFILE.PAM","DRIVERS_LIC_NO_LBL.PAPROFILE.PAM","%25.PAPROFILE.PAM.1.","DRIVERS_LIC_NO.PAPROFILE.PAM",
    			"%23.SRCHPR02.PAM.1-1-1.","PTY_CD.PAPROFILE.PAM","STAT_CD.PAPROFILE.PAM","RESULT_MSG.PROFILE.CRTV","SSN_LBL.PAPROFILE.PAM","COMPANY_NAME.PAPROFILE.PAM"};
    	
    	if( paramPos != null ){
			Vector<String> paramPosVector = new Vector<String>( Arrays.asList( paramPos ) );
			req.setPostParameterOrder( paramPosVector );
		}
		
    	
        setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIWaynePR" );
    }
}

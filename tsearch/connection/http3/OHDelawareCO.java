package ro.cst.tsearch.connection.http3;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class OHDelawareCO extends HttpSite3 {

	@Override
	public LoginResponse onLogin() {
		HTTPResponse response;
		HTTPRequest request = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		try {
			response = exec(request);
		} catch (IOException e) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login Page not found");
		}

		String responseString = response.getResponseAsString();
		if ((responseString.contains("Public Access") && responseString.contains("Attorney Inquiry"))
				|| responseString.contains("We are pleased to announce that our")) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}

		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login Page not found");
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		try {
			if (status == STATUS_LOGGED_IN && req.getMethod() == HTTPRequest.POST && StringUtils.containsIgnoreCase(req.getURL(), "/pa.urd/PAMW6500")) {
				req.setPostParameter("%.PAPROFILE.PAM.1.", "");
				req.setPostParameter("LAST_NAME_LBL.PAPROFILE.PAM", "Last Name");
				req.setPostParameter("LAST_NAME_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("COMPANY_NAME_LBL.PAPROFILE.PAM", "Company Name");
				req.setPostParameter("COMPANY_NM_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("FIRST_NAME_LBL.PAPROFILE.PAM", "First Name");
				req.setPostParameter("FIRST_NAME_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("ACTN_CD_LBL.PAPROFILE.PAM", "Action Code");
				req.setPostParameter("ACTN_CD_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("BEGIN_DT_LBL.PAPROFILE.PAM", "Begin Date");
				req.setPostParameter("BEGIN_DT_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("PTY_CD_LBL.PAPROFILE.PAM", "Party Type");
				req.setPostParameter("PARTY_CD_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("END_DT_LBL.PAPROFILE.PAM", "End Date");
				req.setPostParameter("END_DT_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("SSN_LBL.PAPROFILE.PAM", "SSN");
				req.setPostParameter("SSN.PAPROFILE.PAM", "");
				req.setPostParameter("SSN_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("DOB_LBL.PAPROFILE.PAM", "D.O.B.");
				req.setPostParameter("DOB_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("CASE_CD_LBL.PAPROFILE.PAM", "Case Type");
				req.setPostParameter("CASE_CD_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("DOD_LBL.PAPROFILE.PAM", "D.O.D");
				req.setPostParameter("DOD.PAPROFILE.PAM", "");
				req.setPostParameter("DOD_REQ.PAPROFILE.PAM", " *");
				req.setPostParameter("TICKET_NBR_LBL.PAPROFILE.PAM", "Ticket Nbr");
				req.setPostParameter("TICKET_NBR.PAPROFILE.PAM", "");
				req.setPostParameter("TICKET_NBR_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("CASE_NBR_LBL.PAPROFILE.PAM", "Case Nbr");
				req.setPostParameter("CASE_NBR_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("STAT_CD_LBL.PAPROFILE.PAM", "Status");
				req.setPostParameter("STAT_CD_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("DRIVERS_LIC_NO_LBL.PAPROFILE.PAM", "Drivers License No.");
				req.setPostParameter("DRIVERS_LIC_NO.PAPROFILE.PAM", "");
				req.setPostParameter("DL_NBR_REQ.PAPROFILE.PAM", "*");
				req.setPostParameter("%.PROFILE.CRTV.1.", "");
				req.setPostParameter("CURRENT_YEAR.PROFILE.CRTV", "");
				req.setPostParameter("RESULT_MSG.PROFILE.CRTV", "");
				req.setPostParameter("REQUIRED.PAPROFILE.PAM", "* Required For Search");
				req.setPostParameter("SEARCH_BUTTON.PAPROFILE.PAM", "Searching");
				req.setPostParameter("#CRC.SRCHPR02.PAM.1-1-1.", "00000021");
				req.setPostParameter("ACTN_CD_PRF.SRCHPR02.PAM", "1");

				req.setPostParameter(URLDecoder.decode("%23.SRCHPR02.PAM.1-1-1.", "UTF-8"), "fS9JQzY2OTc2Mjg4");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}

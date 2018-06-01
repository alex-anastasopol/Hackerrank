/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class TXDriverData extends HttpSite {

	@Override
	public LoginResponse onLogin() {
		// get username and password from database
		String userName = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TXDriverData", "user");
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TXDriverData", "password");
		
		if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't find user / password in database");
		}
		
		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
        Protocol.registerProtocol("https", easyhttps);
		
		HTTPRequest request = new HTTPRequest(getSiteLink() + "/Password/PWDproc.asp", HTTPRequest.POST);
		request.setPostParameter("LoginID", userName);
		request.setPostParameter("Password", password);
		request.setPostParameter("PasswordMode", "Login");
		
		String page = execute(request);		
		if(page.contains("Error Detected")){
			logger.error("Invalid username/password");
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}
		
		return LoginResponse.getDefaultSuccessResponse();
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		String moduleId = req.getPostFirstParameter("moduleId");
		
		if(moduleId == null) {
			return;
		}
		
		if(moduleId.equals("0")) { // Driver's Licence Search
			req.removePostParameters("moduleId");
			
			// go to Driver's License Search Page
			HTTPRequest request = new HTTPRequest(getSiteLink() + "/DriversLicense/DLProc.asp?Button1=DisplayForm", HTTPRequest.GET);
			execute(request);
		} else if(moduleId.equals("1")) { // Vehicle Search
			req.removePostParameters("moduleId");
			
			// go to Vehicle Search Page
			HTTPRequest request = new HTTPRequest(getSiteLink() + "/AdvDmv/DMVProc.asp?Button1=DisplayForm", HTTPRequest.GET);
			execute(request);
		} else if(moduleId.equals("2")) { // Voter Registration Search
			req.removePostParameters("moduleId");
			
			// go to Vehicle Search Page
			HTTPRequest request = new HTTPRequest(getSiteLink() + "/VoterReg/VRProc.asp?Button1=DisplayForm", HTTPRequest.GET);
			String page = execute(request);
			
			// check the current county
			String county = getCounty().replaceAll("\\s+", "");
			Matcher m = Pattern.compile("(?is)<td>\\s*<input\\s+id=(CoID\\d+)\\s+type=checkbox\\s+name=CoID\\d+>(.*?)</td>").matcher(page);
			while(m.find()) {
				String countyName = m.group(2).replaceAll("\\s+", "");
				if(county.equals(countyName)) {
					req.setPostParameter(m.group(1), "on");
					break;
				}
			}
		}
	}
}

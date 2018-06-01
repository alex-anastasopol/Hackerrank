package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.utils.StringUtils;


public class COSummitAO extends HttpSite {
	
	@Override
	public LoginResponse onLogin() {
		
		// get the search page
		HTTPRequest req1 = new HTTPRequest(getSiteLink(), GET);
		execute(req1);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {

		if (req.getURL().indexOf("ppi.asp") != -1){
			String resp = execute(req);
			if (StringUtils.isNotEmpty(resp)){
				if (resp.indexOf("LegalList.asp") != -1){
					String subCode = StringUtils.extractParameter(resp, "(?is)subdiv\\s*=\\s*([^\\s]+)").replaceAll("\\\"", "");
					String filing = StringUtils.extractParameter(resp, "(?is)filing\\s*=\\s*([^\\s]+)").replaceAll("\\\"", "");
					String phase = StringUtils.extractParameter(resp, "(?is)phase\\s*=\\s*([^\\s]+)").replaceAll("\\\"", "");
					String page = StringUtils.extractParameter(resp, "(?is)document\\.location\\.href\\s*=\\s*\\\"\\s*([^\\?]+)").replaceAll("\\\"", "");
					req.setURL(getSiteLink() + page + "?SubCode=" + subCode + "&Filing=" + filing + "&Phase=" + phase);
					execute(req);
				} else if (resp.indexOf("Somedata.asp") != -1){
					String searchvalue = StringUtils.extractParameter(resp, "(?is)\\bsearchvalue\\s*=\\s*([^\\s]+)\\s*intSchno").replaceAll("\\\"", "");
					String intSchno = StringUtils.extractParameter(resp, "(?is)\\bintSchno\\s*=\\s*([^\\s]+)").replaceAll("\\\"", "");
					String page = StringUtils.extractParameter(resp, "(?is)document\\.location\\.href\\s*=\\s*\\\"\\s*([^\\?]+)");
					req.setURL(getSiteLink() + page + "?PPI=PPI%20=%20%22" + searchvalue + "%22&Schno=" + intSchno + "&Tool=1");
					execute(req);
				}
			}
		}
	}
}

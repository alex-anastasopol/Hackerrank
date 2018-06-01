package ro.cst.tsearch.connection.http2;

import java.util.Map;
import java.util.Map.Entry;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;

/* used for TNShelbyYB(Bartlett) and TNMontgomeryYA(Clarksville)*/

public class TNMontgomeryEP extends AdvancedTemplateSite {
	
	public TNMontgomeryEP() {
		mainParameters = new String[2];
		mainParameters[0] = "__VIEWSTATE";
		mainParameters[1] = "__EVENTVALIDATION" ;
		mainParametersKey = "search.params";
		formName =  "aspnetForm";
		
		targetArgumentMiddleKey = ":params:";
		targetArgumentParameters = new String[5];
		targetArgumentParameters[0] = "__VIEWSTATE";
		targetArgumentParameters[1] = "__EVENTVALIDATION" ;
		targetArgumentParameters[2] = "__EVENTTARGET";
		targetArgumentParameters[3] = "__EVENTARGUMENT" ;
		targetArgumentParameters[4] = "__VIEWSTATEENCRYPTED";
		singleLineCookies = true;
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
	}
	
	@Override
	public void performSpecificActionsAfterCreation() {
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
	}
	
	public LoginResponse onLogin() {
		return super.onLogin();
	}

    
    /**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		return getSiteLink();
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if(req.getURL().contains("mode=new")) {
			onLogin();
		}
		super.onBeforeRequestExcl(req);
	}
	
	public String getPage(String page) {
		String link = getCrtServerLink().replace("Default.aspx?mode=new", page);
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
	}
	
	public String getPostPage(String page, Map<String,String> params) {
		String link = getCrtServerLink().replace("Default.aspx?mode=new", page);
		HTTPRequest req = new HTTPRequest(link,HTTPRequest.POST);
		for (Entry<String,String> param : params.entrySet()) {
			req.removePostParameters(param.getKey());
			req.setPostParameter(param.getKey(),param.getValue());
		}
		return execute(req);
	}
}
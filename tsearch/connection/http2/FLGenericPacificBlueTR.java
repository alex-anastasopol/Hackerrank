package ro.cst.tsearch.connection.http2;


import java.util.Map;

import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class FLGenericPacificBlueTR extends HttpSite {
		
	@SuppressWarnings("unused")
	@Override
	public LoginResponse onLogin() {
		
		String link = getPageLoginLink();
		String resp = "";
		@SuppressWarnings("deprecation")
		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
        Protocol.registerProtocol("https", easyhttps);
        
        if (link.contains("lctax.org")) {				//FL Levy TR
        	HTTPRequest req1 = new HTTPRequest(link);
        	req1.setMethod(HTTPRequest.GET);
        	execute(req1);
        	HTTPRequest req2 = new HTTPRequest(link);
        	req2.setMethod(HTTPRequest.POST);
        	req2.setPostParameter("action", "list");
        	req2.setPostParameter("agreeButton", "I Agree");
        	execute(req2);
        }
        
		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse res = process(req);
		//resp = execute(req);
		resp = res.getResponseAsString();
				
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	protected String getPageLoginLink() {
		String loginLink = getCrtServerLink();
		if (loginLink.contains("lctax"))
			return loginLink + ".org/ptaxweb/";
		else 
			return loginLink + ".com/ptaxweb/";
	}
		
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = -1;
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		if ("levy".equalsIgnoreCase(crtCounty)) {
			idx = link.indexOf(".org");
		} else {
			idx = link.indexOf(".com");
		}
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if (req.getURL().contains("editPropertySearch.do") && req.getMethod() == HTTPRequest.GET) {
			req.setMethod(HTTPRequest.POST);
			req.setPostParameter("action", "list");
			req.setPostParameter("agreeButton", "I Agree");
		}
	}
	
	@SuppressWarnings("unchecked")
	public String getAssessorPage(String link, int method){
		String param = StringUtils.extractParameterFromUrl(link, "assessorLink");;
		if (StringUtils.isNotEmpty(param)){
			Map<String,String> addParams = (Map<String, String>)getTransientSearchAttribute("paramsDetails:");
			if (addParams != null){
				String assessorLink = addParams.get(param);
				if (assessorLink!=null) {
					HTTPRequest req = new HTTPRequest(assessorLink, HTTPRequest.GET);
					return execute(req);
				}
			}
		}
		
		
		return "";
	}
	
}

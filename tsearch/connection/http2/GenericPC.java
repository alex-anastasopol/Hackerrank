package ro.cst.tsearch.connection.http2;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.protocol.Protocol;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.StringUtils;

public class GenericPC extends HttpSite {

    private static String FORM_NAME = "f_login";
    private static final String USER_AGENT_VALUE = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.16) Gecko/20110319 Firefox/3.6.16 ( .NET CLR 3.5.30729; .NET4.0C)";
    private static final String USER_AGENT_KEY = "User-Agent";
		
	@Override
	public LoginResponse onLogin(){
		
		Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
		
		DataSite dataSite = getDataSite();
		
		// obtain needed parameters
		String siteName = dataSite.getName();
		if(siteName.startsWith("CO")) {
			siteName = "COGenericPC";	//B5492
		}else if(siteName.startsWith("FL")){
			siteName = "FLGenericPC";
		}
		
		String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), 
				siteName.replaceAll("\\s", ""), "user");
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), 
				siteName.replaceAll("\\s", ""), "password");	
		
		/*
		if(siteName.startsWith("FL")) {
			user = "st4792";
			password = "maggie24";
		}
		
		if(siteName.startsWith("FL")) {
			user = "st0202";
			password = "PIC2013!";
		}
		*/
		if(StringUtils.isEmpty(user) || StringUtils.isEmpty(password)) {
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}
		
		String fileId = "";
		try {
			fileId = getSearch().getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
		} catch (Exception e){}
		
		// get login page
		HTTPRequest req = new HTTPRequest(dataSite.getLink(), HTTPRequest.GET);
		req.setHeader(USER_AGENT_KEY, getUserAgentValue());
		String page = process(req).getResponseAsString();
		
		HtmlParser3 parser = new HtmlParser3(page);
		
		FormTag formNode = (FormTag) parser.getNodeByTypeAndAttribute("form", "name", FORM_NAME, true);
		
		if(formNode == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, 
        			"Invalid page. Could not find expected form = [" + FORM_NAME + "]. Last request to " + 
        			dataSite.getLink());
		}

		// login
		req = new HTTPRequest(dataSite.getServerRelativeLink() + formNode.extractFormLocn(), HTTPRequest.POST);
		req.setHeader(USER_AGENT_KEY, getUserAgentValue());
        req.setPostParameter("loginid", user);
        req.setPostParameter("passwd", password);
        req.setPostParameter("client", fileId);
        req.setPostParameter("faction", "Login");
        for (InputTag hiddenTag : parser.getInputsOfType(formNode, "hidden")) {
        	req.setPostParameter(hiddenTag.getAttribute("name"), hiddenTag.getAttribute("value"));
		}
        setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
        page = process(req).getResponseAsString();
    	
        if(!page.contains("case_type_select")){
        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, 
        			"Invalid page (expected div with id = case_type_select). Last link followed: " + 
        			dataSite.getServerRelativeLink() + formNode.extractFormLocn());
        }

        return LoginResponse.getDefaultSuccessResponse();
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req){
		
		req.setHeader(USER_AGENT_KEY, getUserAgentValue());
		String url = req.getURL();
		int indexOfSecondHttps = url.lastIndexOf("https://");
		if(indexOfSecondHttps > 0) {
			url = url.substring(indexOfSecondHttps);
			if(url.contains("dummyCaseNum=")) {
				if(!url.endsWith("&")) {
					url += "&";
				}
				url = url.replaceFirst("dummyCaseNum=[^&]+&", "");
			}
			if(url.contains("dummyDisposition=")) {
				if(!url.endsWith("&")) {
					url += "&";
				}
				url = url.replaceFirst("dummyDisposition=[^&]+&", "");
			}
			if(url.endsWith("&")) {
				url = url.substring(0, url.length() - 1);
			}
			
			req.modifyURL(url);
			
		}
		req.removePostParameters("dummyCaseNum");
		req.removePostParameters("dummyDisposition");
		req.removePostParameters("help");
		req.removePostParameters("moreInfo");
		req.removePostParameters("status");
		req.removePostParameters("assetbased");
		
		String courtType = req.getPostFirstParameter("court_type");
		if("cv".equals(courtType)) {
			String value = req.getPostFirstParameter("exact_party");
			if(StringUtils.isEmpty(value)) {
				req.removePostParameters("exact_party");
			}
			value = req.getPostFirstParameter("show_title");
			if(StringUtils.isEmpty(value)){
				req.removePostParameters("show_title");
			}
			value = req.getPostFirstParameter("nos");
			if(StringUtils.isEmpty(value)){
				req.removePostParameters("nos");
			}
			value = req.getPostFirstParameter("dc_region");
			if(StringUtils.isEmpty(value)){
				req.removePostParameters("dc_region");
			}
			
			req.removePostParameters("chapter");
			req.removePostParameters("all_region");
			req.removePostParameters("ap_region");
			req.removePostParameters("bk_region");
			
		} else if("bk".equals(courtType)){
			String value = req.getPostFirstParameter("exact_party");
			if(StringUtils.isEmpty(value)) {
				req.removePostParameters("exact_party");
			}
			value = req.getPostFirstParameter("show_title");
			if(StringUtils.isEmpty(value)){
				req.removePostParameters("show_title");
			}
			value = req.getPostFirstParameter("chapter");
			if(StringUtils.isEmpty(value)){
				req.removePostParameters("chapter");
			}
			value = req.getPostFirstParameter("bk_region");
			if(StringUtils.isEmpty(value)){
				req.removePostParameters("bk_region");
			}
			req.removePostParameters("all_region");
			req.removePostParameters("ap_region");
			req.removePostParameters("dc_region");
			req.removePostParameters("nos");
		} else if("all".equals(courtType)){
			String value = req.getPostFirstParameter("exact_party");
			if(StringUtils.isEmpty(value)) {
				req.removePostParameters("exact_party");
			}
			value = req.getPostFirstParameter("show_title");
			if(StringUtils.isEmpty(value)){
				req.removePostParameters("show_title");
			}
			value = req.getPostFirstParameter("all_region");
			if(StringUtils.isEmpty(value)){
				req.removePostParameters("all_region");
			}
			req.removePostParameters("bk_region");
			req.removePostParameters("ap_region");
			req.removePostParameters("dc_region");
			req.removePostParameters("nos");
			req.removePostParameters("chapter");
		}
		
		Cookie[] cookies = getHttpClient().getState().getCookies();
		String cookieLine = "";
		for (int i = 0; i < cookies.length; i++) {
			if(i == 0) {
				cookieLine = cookies[i].getName() + "=\"" + cookies[i].getValue() + "\"";
			} else {
				cookieLine += "; " + cookies[i].getName() + "=\"" + cookies[i].getValue() + "\"";
			}
		}
		req.setHeader("Cookie", cookieLine);
		
		String savedReferer = (String) getSearch().getAdditionalInfo("Referer_" + url);
		if(savedReferer != null) {
			req.setHeader("Referer", savedReferer);
		}
		
		
		if(req.getMethod() == HTTPRequest.POST && url.contains("TransportRoom")) {
			if(!req.getPostParameter("servlet").isEmpty()) {
				String servlet = req.getPostFirstParameter("servlet");
				if(!url.contains("?")) {
					url += "?servlet=" + servlet;
				} else if(url.endsWith("?") || url.endsWith("&")) {
					url += "servlet=" + servlet;
				} else {
					url += "&servlet=" + servlet;
				}
				req.modifyURL(url);
			}
		}
		
		
		
	}
	
	public void onRedirect(HTTPRequest req) {
		if(req != null) {
			req.setHeader(USER_AGENT_KEY, getUserAgentValue());
		}
	}
	
	@Override
	public String getUserAgentValue() {
		return USER_AGENT_VALUE;
	}

}

package ro.cst.tsearch.connection.http3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;

public class FLMiamiDadeAO extends HttpSite3 {
	
	@Override
	public LoginResponse onLogin() {
		HTTPRequest request = new HTTPRequest(getSiteLink());
		request.setHeader("User-Agent", getUserAgentValue());	
		request.setHeader("Accept", getAccept());
		request.setHeader("Accept-Language", getAcceptLanguage());
		//request.setHeader("Accept-Encoding", "gzip, deflate");
		request.setHeader("Host", "www.miamidade.gov");
		request.setReferer("www.miamidade.gov");
		
		String responseAsString = execute(request);
		
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for " + getSiteLink());
		}
		
		if(responseAsString.contains("ng-app=\"propertySearchApp\"")) {
			return LoginResponse.getDefaultSuccessResponse();
			
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not get login page!");
		}
	}
	 
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		req.setHeader("User-Agent", getUserAgentValue());	
		req.setHeader("Accept", getAccept());
		req.setHeader("Accept-Language", getAcceptLanguage());
		req.setHeader("Accept-Encoding", "gzip, deflate");
		req.setHeader("Host", "www.miamidade.gov");
		
		if (req.getURL().contains("-") && req.getURL().contains("folioNumber=")) {
			req.setURL(req.getURL().replaceAll("-", ""));
		}
		
		if (req.getURL().contains("gisweb")) {
			String url = req.getURL();
			url = url.replaceFirst("(?is)[/.//:A-z]+gov(www.*)", "http://$1");
			if (!url.contains("www.miamidade")) { //det pg link to be corrected
				url = url.replaceFirst("gisweb", "www");
			}
			req.setURL(url);
		}
	}
	

	@Override
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0";
	}
	
	@Override
	public String getAcceptLanguage() {
		return "en-US,en;q=0.5";
	}
	
	@Override
	public String getAccept() {
		return "application/json, text/plain, */*";
	}
}

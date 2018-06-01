package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;

public class FLHillsboroughTR extends SimplestSite {

	@Override
	public void onBeforeRequest(HTTPRequest req){
		String url = req.getURL();
		url = url.replaceFirst("(?is)pin=.*?(&|$)", "");
		url = url.replaceFirst("(?is)folio=.*?(&|$)", "");
		req.setURL(url);
		
		req.setHeader("User-Agent", getUserAgentValue());
		req.setHeader("Accept", getAccept());
		req.setHeader("Accept-Language", getAcceptLanguage());
		req.setHeader("Host", "http://www.hillstax.org/");
		super.onBeforeRequest(req);
	}
	
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:28.0) Gecko/20100101 Firefox/28.0";
	}
	
	public String getAcceptLanguage() {
		return "en-US,en;q=0.5";
	}
	
	public String getAccept() {
		return "ext/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	}
	
	public LoginResponse onLogin() {
		return super.onLogin();
	}
}

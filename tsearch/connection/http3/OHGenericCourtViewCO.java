package ro.cst.tsearch.connection.http3;

import java.io.ByteArrayInputStream;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class OHGenericCourtViewCO extends HttpSite3 {
	
	public static final String SELECT_SEARCH_CRITERIA = "Select your search criteria below";
	
	private String lastResponse = "";
	private String xValue = "";
	private String xValueNumberOfResults = "";
	
	public static String generateRandom() {
		StringBuilder res = new StringBuilder();
		res.append("0.");
		Random random = new Random();
		for (int i=0;i<16;i++) {
			res.append(Integer.toString(random.nextInt(10)));
		}
		return res.toString();
	}
	
	public static String getXValueFromForm(String s) {
		String res = "";
		Matcher ma = Pattern.compile("(?is)<form[^>]+action=\"\\?x=([^\"]+)\"[^>]*>").matcher(s);
		if (ma.find()) {
			res = ma.group(1);
		}
		return res;
	}
	
	public static String getXValueNumberOfResultsFromForm(String s) {
		String res = "";
		Matcher ma = Pattern.compile("(?is)<select[^>]+name=\"bodyLayout:topSearchPanel:pageSize\"[^>]+onchange=\"[^\"]+'\\?x=([^']+)'[^\"]*\"[^>]*>").matcher(s);
		if (ma.find()) {
			res = ma.group(1);
		}
		return res;
	}
	
	@Override
	public LoginResponse onLogin() {
		
		String hostName = getSiteLink();
		hostName = hostName.replaceFirst("(?is)^https?://", "");
		int index = hostName.indexOf("/");
		if (index!=-1) {
			hostName = hostName.substring(0, index);
		}
		
		//go to first page
		HTTPRequest req1 = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		String resp1 = execute(req1);
		String x = getXValueFromForm(resp1);
		
		//send browser properties
		HTTPRequest req2 = new HTTPRequest(getLastRequest() + "?x=" + x, HTTPRequest.POST);
		req2.setPostParameter("id1_hf_0", "");
		req2.setPostParameter("navigatorAppName", "Netscape");
		req2.setPostParameter("navigatorAppVersion", "5.0 (Windows)");
		req2.setPostParameter("navigatorAppCodeName", "Mozilla");
		req2.setPostParameter("navigatorCookieEnabled", "true");
		req2.setPostParameter("navigatorJavaEnabled", "true");
		req2.setPostParameter("navigatorLanguage", "en-US");
		req2.setPostParameter("navigatorPlatform", "Win32");
		req2.setPostParameter("navigatorUserAgent", "Mozilla/5.0 (Windows+NT 6.1; WOW64; rv:24.0) Gecko/20100101 Firefox/24.0");
		req2.setPostParameter("screenWidth", "1920");
		req2.setPostParameter("screenHeight", "1080");
		req2.setPostParameter("screenColorDepth", "24");
		req2.setPostParameter("utcOffset", "2");
		req2.setPostParameter("utcDSTOffset", "3");
		req2.setPostParameter("browserWidth", "1920");
		req2.setPostParameter("browserHeight", "956");
		req2.setPostParameter("hostname", hostName);
		String resp2 = execute(req2);
		Matcher ma = Pattern.compile("(?is)<a[^>]+onclick=[^>]+'\\?x=([^']+)'[^>]*>").matcher(resp2);
		if (ma.find()) {
			x = ma.group(1);
		}
		
		//click on "Click Here To search records."
		HTTPRequest req3 = new HTTPRequest(getLastRequest() + "?x=" + x + "&random=" + generateRandom(), HTTPRequest.POST);
		req3.setPostParameter("id1_hf_0", "");
		req3.setPostParameter("linkFrag:beginButton", "1");
		String resp3 = execute(req3);
		
		xValue = getXValueFromForm(resp3);
		xValueNumberOfResults = getXValueNumberOfResultsFromForm(resp3);
		
		lastResponse = resp3;
		
		if (resp3.contains(SELECT_SEARCH_CRITERIA)) {
			return LoginResponse.getDefaultSuccessResponse();
		} else {
			return LoginResponse.getDefaultFailureResponse();
		}
		
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {

		String url = req.getURL();
		
		if (req.getMethod()==HTTPRequest.POST) {
			String submitLink = req.getPostFirstParameter("submitLink");
			if (submitLink!=null) {													//intermediary page
				
				if (!lastResponse.contains(SELECT_SEARCH_CRITERIA)) {				//click on "New Search"
					Matcher ma = Pattern.compile("(?is)<a[^>]+href=\"\\?x=([^\"]+)\"[^>]*>New Search</a>").matcher(lastResponse);
					if (ma.find()) {
						String x = ma.group(1);
						HTTPRequest req1 = new HTTPRequest(getSiteLink() + "?x=" + x, HTTPRequest.GET);
						String resp1 = execute(req1);
						xValue = getXValueFromForm(resp1);
						lastResponse = resp1;
					}
				}
				
				String caseDscr = req.getPostFirstParameter("caseDscr");
				if (caseDscr!=null) {												//Case Number Search
					Matcher ma = Pattern.compile("(?is)<a[^>]+href=\"\\?x=([^\"]+)\"[^>]*>\\s*<span>Case Number</span>\\s*</a>").matcher(lastResponse);
					if (ma.find()) {
						String x = ma.group(1);
						HTTPRequest req1 = new HTTPRequest(getSiteLink() + "?x=" + x, HTTPRequest.GET);
						String resp1 = execute(req1);
						xValue = getXValueFromForm(resp1);
						xValueNumberOfResults = getXValueNumberOfResultsFromForm(resp1);
						lastResponse = resp1;
					}
				} else {															//Name Search
					String numberOfResults = req.getPostFirstParameter("numberOfResults");
					if (!"0".equals(numberOfResults)) {
						HTTPRequest req2 = new HTTPRequest(getSiteLink() + "?x=" + xValueNumberOfResults + "&random=" + generateRandom(), HTTPRequest.POST);
						req2.setPostParameter("bodyLayout:topSearchPanel:pageSize", numberOfResults);
						String resp2 = execute(req2);
						xValue = getXValueFromForm(resp2);
						xValueNumberOfResults = getXValueNumberOfResultsFromForm(resp2);
						lastResponse = resp2;
					}
					req.removePostParameters("numberOfResults");
				}
				
				req.modifyURL(url + "/?x=" + xValue);
				
			}
		}
		
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		
		if (req.getMethod()==HTTPRequest.POST) {
			String lastName = req.getPostFirstParameter("lastName");
			String caseDscr = req.getPostFirstParameter("caseDscr");
			if (lastName!=null || caseDscr!=null) {					//Name Search or Case Number Search
				String content = res.getResponseAsString();
				if (content.contains("Search Results")) {
					lastResponse = content;
				}
				res.is = new ByteArrayInputStream(content.getBytes());
			}
		}
	}
	
}

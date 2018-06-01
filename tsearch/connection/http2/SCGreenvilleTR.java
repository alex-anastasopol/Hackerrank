package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpStatus;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class SCGreenvilleTR extends HttpSite {

	@Override
	public LoginResponse onLogin() {
		HTTPRequest httpRequest = new HTTPRequest(getSiteLink());
		HTTPResponse response = process(httpRequest);
		LoginResponse defaultSuccessResponse = LoginResponse.getDefaultSuccessResponse();
		if (response.getReturnCode() != HttpStatus.SC_OK) {
			defaultSuccessResponse = LoginResponse.getDefaultFailureResponse();
		}
		return defaultSuccessResponse;
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		String url = req.getURL();
		boolean querryTax = url.contains("voTaxQry");
		if (req.getMethod() == HTTPRequest.POST && !querryTax) {// this brings
																// the
																// intermediary
																// result
			HashMap<String, String> hashMap = new HashMap<String, String>();
			hashMap.put("WCE", "Form1");
			hashMap.put("WCI", "tplRealSearch");
			String addParametersToUrl = StringUtils.addParametersToUrl(url + "?", hashMap);
			req.setURL(addParametersToUrl);
		} else if (querryTax) {
			HashMap<String, String> requestParameters = new HashMap<String, String>();
			requestParameters.put("SearchType", "Real");
			requestParameters.put("Choice", "Real");
			requestParameters.put("Account", "");
			requestParameters.put("Name", "");
			requestParameters.put("VinNum", "");
			String mapNum = RegExUtils.getFirstMatch("MapNum=(\\d+)", url, 1);
			requestParameters.put("MapNum", mapNum);
			requestParameters.put("Send", "Send");
			String link = "http://www.greenvillecounty.org/voTaxQry/wcMain.ASP?WCI=Process&WCE=submit";
			req.setURL(link);
			Set<Entry<String, String>> entrySet = requestParameters.entrySet();
			for (Entry<String, String> entry : entrySet) {
				req.setPostParameter(entry.getKey(), entry.getValue());
			}
			// link = StringUtils.addParametersToUrl(link, requestParameters) ;

		}
		super.onBeforeRequest(req);
	}

	public String getAppraiserPage(String link, int type) {
		HTTPRequest req = new HTTPRequest(link, type);
		return execute(req);
	}

}

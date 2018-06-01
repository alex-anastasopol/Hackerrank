package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpStatus;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.StringUtils;

public class XXGenericFDIC extends SimplestSite {
	@Override
	public LoginResponse onLogin() {
		String siteLink = getSiteLink();
		HTTPRequest httpRequest = new HTTPRequest(siteLink);
		process(httpRequest);
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "");
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
//		/idasp/BHClist_cert.asp?inSortIndex=4&inCert1=2513250
		String paramCert1 = "inCert1";
		String parameter = req.getPostFirstParameter(paramCert1);
		if (StringUtils.isNotEmpty((String) parameter)){
			String siteLink = getSiteLink();
			int lastIndexOf = siteLink.lastIndexOf("/main.asp");
			String baseLink = siteLink.substring(0,lastIndexOf);
			String newUrl = baseLink + "/BHClist_cert.asp?";
			if ( req.getURL().contains("FindAnInstitution.asp") ){
				newUrl = baseLink + "/rpt_offices.asp?";
				List<String> paramKeySet = new ArrayList<String>(req.getPostParameters().keySet());
				
				for (String param : paramKeySet) {
					req.removePostParameters(param);
				}
				req.setPostParameter("cert", parameter);
			}
			reconstructRequestForIdSearch(req, newUrl);
		}else {
			parameter = req.getPostFirstParameter("inCert");
//			String idParameterOffices = req.getPostFirstParameter("cert");
			if (StringUtils.isNotEmpty((String) parameter) ){//|| StringUtils.isNotEmpty((String) idParameterOffices)
				String siteLink = getSiteLink();
				int lastIndexOf = siteLink.lastIndexOf("/main.asp");
				String baseLink = siteLink.substring(0,lastIndexOf);
				if ( req.getURL().contains("FindOffices.asp") ){
					req.removePostParameters("inCert");
					req.setPostParameter("cert", parameter);
				}
				String newUrl = baseLink + "/rpt_offices.asp?";
				reconstructRequestForIdSearch(req, newUrl);
			}
		}
		
		super.onBeforeRequest(req);
	}

	/**
	 * @param req
	 * @param newUrl
	 */
	public void reconstructRequestForIdSearch(HTTPRequest req, String newUrl) {
		HashMap<String,ParametersVector> postParameters = req.getPostParameters();
		Set<Entry<String, ParametersVector>> entrySet = postParameters.entrySet();
		HashMap<String, String> getParameters = new HashMap<String,String>();
		for (Entry<String, ParametersVector> entry : entrySet) {
			String firstParameter = entry.getValue().getFirstParameter();
			if (StringUtils.isNotEmpty(firstParameter)){
				getParameters.put(entry.getKey(), firstParameter);
			}
		}
		req.setURL(StringUtils.addParametersToUrl(newUrl, getParameters).replaceAll(" ", "+"));
		req.setMethod(HTTPRequest.GET);
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String content = "";
		if (res != null) {
			if (res.getContentType().contains("text/html")) {
				content = res.getResponseAsString();
				res.is = new ByteArrayInputStream(content.getBytes());
				if (content.contains("Session Expired")|| res.getReturnCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
					destroySession();
				}
			}
		}
	}
}

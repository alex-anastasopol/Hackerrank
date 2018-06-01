package ro.cst.tsearch.connection.http3;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.servers.bean.DataSite;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

public class TNShelbyPR extends HttpSite3 {
	private Vector<String>			POST_PARAMETERS_ORDERED	= new Vector<String>();
	private HashMap<String, String>	POST_PARAMETERS			= null;
	private String					siteHomePage			= "/cgi/cgicgi.pl?TimeStamp=";

	@Override
	public LoginResponse onLogin() {
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	protected void goToSearchPage(HTTPRequest originalRequest) {
		boolean isCaseNameSearch = false;
		if (originalRequest.hasPostParameter("XPBCHO_000101")
				&& originalRequest.getPostParameter("XPBCHO_000101").toString().equals("A")) {
			isCaseNameSearch = true;
		}

		DataSite dataSite = getDataSite();
		HTTPRequest request = new HTTPRequest(dataSite.getLink());
		HTTPResponse res = process(request);
		if (res.getReturnCode() == 200) {
			String redirectLink = dataSite.getLink() + siteHomePage;
			request.setURL(redirectLink);
			res = process(request);
			String responseAsString = res.getResponseAsString();
			Pattern p = Pattern.compile("(?is)Probate\\s+Case\\s+Information");
			Matcher m = p.matcher(responseAsString);
			if (m.find()) {
				if (isCaseNameSearch) {// go to name search page and get its params
					request.setMethod(HTTPRequest.POST);

					request.removePostParameters("XPBCHO_000101");
					request.setPostParameter("XPBCHO_000101", "A");
					setRequestPostParams(request);
					res = process(request);
					responseAsString = res.getResponseAsString();
				}
			}
		}
	}

	public void getFormParamsOrdered(String responseAsString) {
		POST_PARAMETERS_ORDERED = ro.cst.tsearch.utils.HttpUtils.getFormParamsOrdered(responseAsString);
		POST_PARAMETERS = getFormParams(responseAsString);
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		String link = dataSite.getLink() + siteHomePage;
		String reqURL = req.getURL();
		if (req.getMethod() == HTTPRequest.POST) {
			if (req.hasPostParameter("TimeStamp")) {
				// if doing a search remove timestamp as POST param and put it as a GET param
				req.removePostParameters("TimeStamp");
				req.setURL(link);
				goToSearchPage(req);// get case number or case name search page
				setRequestPostParams(req);
			} else if (ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(reqURL, "isGetMoreDetailsLink").equals("true")) {
				req.setURL(link);// get more details from the links in main details page
				setRequestPostParams(req);
			} else if (req.hasPostParameter("isGetDetailsLink")) {// details link from intermediaries
				String key = "";
				String number = "";

				if (req.hasPostParameter("key")) {
					key = req.getPostParameter("key").toString();
				}
				if (req.hasPostParameter("number")) {
					number = req.getPostParameter("number").toString();
				}
				req.clearPostParameters();

				// set the 3 params!!
				req.setPostParameter("XPBCHO_000101", "B");
				req.setPostParameter("XCACPFC000101", key);
				req.setPostParameter("XCAMDKC000101", number);
				req.setURL(link);

				goToSearchPage(req);
				setRequestPostParams(req);
			} else if (req.hasPostParameter("isNextLink")) {// next link in intermediaries
				req.setURL(link);
				setRequestPostParams(req);
			}
		}
	}

	protected void setRequestPostParams(HTTPRequest req) {
		HashMap<String, ParametersVector> reqParams = new HashMap<String, HTTPRequest.ParametersVector>(req.getPostParameters());
		req.clearPostParameters();

		for (String param : POST_PARAMETERS_ORDERED) {
			String paramValue;
			if (reqParams.get(param) != null) {
				ParametersVector paramValuePV = reqParams.get(param);
				if (paramValuePV != null) {
					req.removePostParameters(param);
					req.setPostParameter(param, paramValuePV.toString());
				}
			} else {
				paramValue = StringUtils.defaultString(POST_PARAMETERS.get(param));
				req.setPostParameter(param, paramValue);
			}
		}

		if (req.hasPostParameter("MY_PFKEY")) {
			String myPFKey = req.getPostParameter("MY_PFKEY").toString();
			if (StringUtils.isEmpty(myPFKey)) {
				req.removePostParameters("MY_PFKEY");
				req.setPostParameter("MY_PFKEY", "32");
			}
		}

		// remove unneeded params
		req.removePostParameters("PFKEYEN");
		POST_PARAMETERS_ORDERED.remove("PFKEYEN");

		req.removePostParameters("PFKEYLEN");
		POST_PARAMETERS_ORDERED.remove("PFKEYLEN");

		req.setPostParameterOrder(POST_PARAMETERS_ORDERED);
	}

	private HashMap<String, String> getFormParams(String html) {
		HashMap<String, String> params = new HashMap<String, String>();

		Pattern pat = Pattern.compile("(?is)<input[^>]*\\bname\\s*=\\s*\"([^\"]+)\"(?:[^>]*\\bvalue\\s*=\\s*\"([^\"]+)\")?[^>]*>");
		Matcher matcher = pat.matcher(html);
		while (matcher.find()) {
			String value = StringUtils.defaultString(matcher.group(2));
			params.put(matcher.group(1), value);
		}
		return params;
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		// always get the new params from the response we just got !!
		String responseAsString = res.getResponseAsString();
		getFormParamsOrdered(responseAsString);
		res.is = new ByteArrayInputStream(responseAsString.getBytes());
	}
}
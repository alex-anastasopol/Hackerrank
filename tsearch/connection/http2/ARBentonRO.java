package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.protocol.Protocol;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.InputTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.utils.StringUtils;

/**
 * 
 * @author Oprina George
 * 
 *         Jul 25, 2011
 */

public class ARBentonRO extends HttpSite {
	private String cert_date_resp = "";
	public static String SITE_LINK_PREFIX = "";
	
	public String getCert_date_resp() {
		return cert_date_resp;
	}

	@Override
	public LoginResponse onLogin() {

		@SuppressWarnings("deprecation")
		Protocol easyhttps = new Protocol("https",
				new EasySSLProtocolSocketFactory(), 443);
		Protocol.registerProtocol("https", easyhttps);

		HTTPRequest req1 = new HTTPRequest(
						"https://gov.propertyinfo.com/AR-Benton/", GET);
		
		String resp1 = execute(req1);
		
		int indexOfLoginFrom = resp1.indexOf("showLoginForm('");
		
		if(indexOfLoginFrom!=-1){
			resp1 = resp1.substring(indexOfLoginFrom);
			
			int indexOfLogin = resp1.indexOf("Login / Begin Search");
		
			if(indexOfLogin!=-1){
				resp1 = resp1.substring(0,indexOfLogin);
				
				if(resp1.split(",").length==2){
					SITE_LINK_PREFIX = resp1.split(",")[0].replaceAll("(?ism).*'([^']*)'.*","$1");
				}
			}
		}
		
		if(StringUtils.isEmpty(SITE_LINK_PREFIX))
			return LoginResponse.getDefaultFailureResponse();
		
		setTransientSearchAttribute("siteLinkPrefix", SITE_LINK_PREFIX);

		HTTPRequest req4 = new HTTPRequest(
				SITE_LINK_PREFIX + "loginForm.asp?iWAMid=26&a=true", GET);
		
		process(req4);
		
		HTTPRequest req5 = new HTTPRequest(
				SITE_LINK_PREFIX + "loginVerify.asp", POST);

		String user = SitesPasswords.getInstance().getPasswordValue(
				getCurrentCommunityId(), "ARBentonRO", "user");
		String pass = SitesPasswords.getInstance().getPasswordValue(
				getCurrentCommunityId(), "ARBentonRO", "password");

		if (StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS,
					"Couldn't get credentials from database");
		}

		req5.setPostParameter("iWAMid", "26");
		req5.setPostParameter("validationCode", "");
		req5.setPostParameter("txtUserName", user);
		req5.setPostParameter("txtPassword", pass);
		req5.setPostParameter("x", "16");
		req5.setPostParameter("y", "8");

		String resp = execute(req5);
		this.cert_date_resp = resp;

		if (resp.contains("Error"))
			return LoginResponse.getDefaultInvalidCredentialsResponse();

		return LoginResponse.getDefaultSuccessResponse();
	}

	public void onBeforeRequest(HTTPRequest req) {
		String url = req.getURL();

		if (req.getMethod() == POST) {
			if (url.equals(SITE_LINK_PREFIX+"SearchResults.asp")) {

				HTTPRequest req0 = new HTTPRequest(SITE_LINK_PREFIX+"SearchSummary.asp",POST);
				HashMap<String, ParametersVector> params = new HashMap<String, ParametersVector>(
						req.getPostParameters());

				req0.setPostParameters(params);

				String sDocType = params.get("SearchDocType") == null ? ""
						: params.get("SearchDocType").toString();

				String[] sDocTypes = sDocType.split(",");

				String availSearchDocType = sDocTypes.length > 1 ? sDocTypes[sDocTypes.length - 1]
						: sDocTypes[0];

				if (availSearchDocType.equals("ABSTRACT"))
					sDocType = "ALL";

				req0.removePostParameters("SearchDocType");
				req0.setPostParameter("SearchDocType", sDocType);
				req0.setPostParameter("availSearchDocType", availSearchDocType);

				HTTPResponse resp0 = process(req0);

				String resp = resp0.getResponseAsString();

				HashMap<String, String> new_params = getNewParams(resp);

				for (String param : params.keySet())
					req.removePostParameters(param);

				for (String param : new_params.keySet())
					req.setPostParameter(
							param,
							new_params.get(param) == null ? "" : new_params
									.get(param));
			} else if (url.contains(SITE_LINK_PREFIX+"viewIndex.asp")) {

				req.setURL(SITE_LINK_PREFIX+"viewIndex.asp");

			} else if (url.contains(SITE_LINK_PREFIX+"viewImage.asp")
					&& !url.contains(SITE_LINK_PREFIX+"viewImage.asp_fake")) {
				req.setURL(SITE_LINK_PREFIX+"viewImage.asp");
				
				HTTPResponse resp0 = null;

				HTTPRequest req0 = new HTTPRequest(SITE_LINK_PREFIX+"viewImage.asp_fake",POST);

				if (req.getPostParameters().size() > 0)
					req0.setPostParameters(req.getPostParameters());
				
				resp0 = process(req0);
				
				if("image/tiff".equals(resp0.getContentType())){
					req.setBypassResponse(resp0);
					return;
				}
				
				String r0 = resp0.getResponseAsString();
				Form form = new SimpleHtmlParser(r0).getForm("frmViewImage");
				HashMap<String, String> params_aux = new HashMap<String, String>();

				if (form != null) {
					params_aux = (HashMap<String, String>) form.getParams();

					HTTPRequest req1 = new HTTPRequest( SITE_LINK_PREFIX + form.action, POST);

					for (String s : params_aux.keySet())
						req1.setPostParameter(s, params_aux.get(s) == null ? ""
								: params_aux.get(s));

					String resp1 = execute(req1);

					HashMap<String, String> image_params = getImageParams(resp1);

					if (StringUtils.isNotEmpty(image_params.get("docname"))
							&& StringUtils.isNotEmpty(image_params
									.get("BufferVol"))) {
						HTTPRequest req2 = new HTTPRequest(
								image_params.get("BufferVol") + "/"
										+ image_params.get("docname"), GET);
					
						HTTPResponse resp2 = process(req2);

						resp2.is = new ByteArrayInputStream(
								resp2.getResponseAsByte());

						req.setBypassResponse(resp2);

					}
				}
			} else if (url.equals(SITE_LINK_PREFIX + "viewImage.asp_fake"))
				req.setURL(SITE_LINK_PREFIX + "viewImage.asp");
			else if (url.equals(SITE_LINK_PREFIX + "SearchSummary.asp")) {
				String st = req.getPostFirstParameter("sSearchType");
				if ("N".equals(st)) {
					// search by name
					req.setPostParameter("iWAMid", "26");
					req.setPostParameter("sSearchTitle", "");
					req.setPostParameter("iUserSearchID", "0");
					req.setPostParameter("optLNWildCard", "1");
					req.setPostParameter("optFNWildCard", "2");
					req.setPostParameter("SearchPartyDataSetNum", "");
					req.setPostParameter("SearchbyGtrGte", "0");
					req.setPostParameter("iDataSetNum", "8001");
					req.setPostParameter("iRecordsPerPage", "100");
					req.setPostParameter("tiBatch", "0");
					req.setPostParameter("tiPhoneBk", "0");
					req.setPostParameter("x", "44");
					req.setPostParameter("y", "9");
					req.setPostParameter("iApplNum", "8001");
					req.setPostParameter("iSearchNum", "8001");
					req.setPostParameter("iCntyNum", "8");
					req.setPostParameter("sCntyName", "Benton County");
					req.setPostParameter("sApplName", "Benton County");
					req.setPostParameter("iRowID", "");
					req.setPostParameter("sSearchName", "Name");
					req.setPostParameter("iDirField", "");
					req.setPostParameter("iRevField", "");
					req.setPostParameter("iDateField", "8016");
					req.setPostParameter("iTypeField", "8017");
					req.setPostParameter("iDocField", "0");
					req.setPostParameter("iBookField", "0");
					req.setPostParameter("iPageField", "0");
					req.setPostParameter("iBookTypeField", "8002");
					req.setPostParameter("iLandList", "0");
					req.setPostParameter("iPistolList", "0");
					req.setPostParameter("iCaseField", "0");
					req.setPostParameter("iSupplementList", "0");
					req.setPostParameter("iUser1Field", "0");
					req.setPostParameter("iUser2Field", "0");
					req.setPostParameter("dtbeginDate", "");
					req.setPostParameter("dtlastDate", "");
					req.setPostParameter("updateStats", "yes");
				} else if ("BP".equals(st)) {
					// search by book & page
					req.setPostParameter("iWAMid", "26");
					req.setPostParameter("sSearchTitle", "");
					req.setPostParameter("iUserSearchID", "0");
					req.setPostParameter("iDataSetNum", "8001");
					req.setPostParameter("iRecordsPerPage", "100");
					req.setPostParameter("tiBatch", "0");
					req.setPostParameter("tiPhoneBk", "0");
					req.setPostParameter("x", "59");
					req.setPostParameter("y", "11");
					req.setPostParameter("iApplNum", "8001");
					req.setPostParameter("iSearchNum", "8002");
					req.setPostParameter("iCntyNum", "8");
					req.setPostParameter("sCntyName", "Benton County");
					req.setPostParameter("sApplName", "Benton County");
					req.setPostParameter("iRowID", "");
					req.setPostParameter("sSearchName", "Book / Page");
					req.setPostParameter("iDirField", "");
					req.setPostParameter("iRevField", "");
					req.setPostParameter("iDateField", "8016");
					req.setPostParameter("iTypeField", "8017");
					req.setPostParameter("iDocField", "0");
					req.setPostParameter("iBookField", "8003");
					req.setPostParameter("iPageField", "8004");
					req.setPostParameter("iBookTypeField", "8001");
					req.setPostParameter("iLandList", "0");
					req.setPostParameter("iPistolList", "0");
					req.setPostParameter("iCaseField", "0");
					req.setPostParameter("iSupplementList", "0");
					req.setPostParameter("iUser1Field", "0");
					req.setPostParameter("iUser2Field", "0");
					req.setPostParameter("dtbeginDate", "");
					req.setPostParameter("dtlastDate", "");
					req.setPostParameter("updateStats", "yes");
				} else if ("DT".equals(st)) {
					// search by doc type
					req.setPostParameter("iWAMid", "26");
					req.setPostParameter("sSearchTitle", "");
					req.setPostParameter("iUserSearchID", "0");
					req.setPostParameter("iDataSetNum", "8001");
					req.setPostParameter("iRecordsPerPage", "100");
					req.setPostParameter("tiBatch", "0");
					req.setPostParameter("tiPhoneBk", "0");
					req.setPostParameter("x", "62");
					req.setPostParameter("y", "10");
					req.setPostParameter("iApplNum", "8001");
					req.setPostParameter("iSearchNum", "8003");
					req.setPostParameter("iCntyNum", "8");
					req.setPostParameter("sCntyName", "Benton County");
					req.setPostParameter("sApplName", "Benton County");
					req.setPostParameter("iRowID", "");
					req.setPostParameter("sSearchName", "Date / Doc Type");
					req.setPostParameter("iDirField", "");
					req.setPostParameter("iRevField", "");
					req.setPostParameter("iDateField", "8009");
					req.setPostParameter("iTypeField", "8011");
					req.setPostParameter("iDocField", "0");
					req.setPostParameter("iBookField", "0");
					req.setPostParameter("iPageField", "0");
					req.setPostParameter("iBookTypeField", "8002");
					req.setPostParameter("iLandList", "0");
					req.setPostParameter("iPistolList", "0");
					req.setPostParameter("iCaseField", "0");
					req.setPostParameter("iSupplementList", "0");
					req.setPostParameter("iUser1Field", "0");
					req.setPostParameter("iUser2Field", "0");
					req.setPostParameter("dtbeginDate", "");
					req.setPostParameter("dtlastDate", "");
					req.setPostParameter("updateStats", "yes");
				} else if ("L".equals(st)) {
					// search by plat
					req.setPostParameter("iWAMid", "26");
					req.setPostParameter("sSearchTitle", "");
					req.setPostParameter("iUserSearchID", "0");
					req.setPostParameter("iDataSetNum", "8001");
					req.setPostParameter("iRecordsPerPage", "100");
					req.setPostParameter("tiBatch", "2");
					req.setPostParameter("tiPhoneBk", "0");
					req.setPostParameter("x", "52");
					req.setPostParameter("y", "11");
					req.setPostParameter("iApplNum", "8001");
					req.setPostParameter("iSearchNum", "8004");
					req.setPostParameter("iCntyNum", "8");
					req.setPostParameter("sCntyName", "Benton County");
					req.setPostParameter("sApplName", "Benton County");
					req.setPostParameter("iRowID", "");
					req.setPostParameter("sSearchName", "Plat Legal");
					req.setPostParameter("iDirField", "");
					req.setPostParameter("iRevField", "");
					req.setPostParameter("iDateField", "8009");
					req.setPostParameter("iTypeField", "8011");
					req.setPostParameter("iDocField", "0");
					req.setPostParameter("iBookField", "0");
					req.setPostParameter("iPageField", "0");
					req.setPostParameter("iBookTypeField", "8001");
					req.setPostParameter("iLandList", "8009");
					req.setPostParameter("iPistolList", "0");
					req.setPostParameter("iCaseField", "0");
					req.setPostParameter("iSupplementList", "0");
					req.setPostParameter("iUser1Field", "0");
					req.setPostParameter("iUser2Field", "0");
					req.setPostParameter("dtbeginDate", "");
					req.setPostParameter("dtlastDate", "");
					req.setPostParameter("updateStats", "yes");
				}
			} else if (url.contains("PREV_NEXT")){
				req.setURL(url.replace("PREV_NEXT","").replaceFirst("\\&","?"));
			}
		}
		super.onBeforeRequest(req);
	}

	private int retries = 0;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res != null) {
			if (res.getReturnCode() == 500) {
				res.returnCode = 200;
				try {
					res.setLastURI(new URI("Error"));
				} catch (URIException e) {
					e.printStackTrace();
				}
			} else {
				if(res.getLastURI().getEscapedURI().contains("Error") || res.getLastURI().getEscapedURI().contains("error")){
					LoginResponse lr = onLogin();
					if(retries == 10){
						retries = 0;
						return;
					}
					if (lr.getStatus().equals(LoginResponse.getDefaultSuccessResponse().getStatus())){
						retries = 0;
						HTTPResponse resp = process(req);
						res.is = new ByteArrayInputStream(resp.getResponseAsByte());
						res.contentLenght= resp.contentLenght;
						res.contentType = resp.contentType;
						res.headers = resp.headers;
						res.returnCode = resp.returnCode;
						res.body = resp.body;
						res.setLastURI(resp.getLastURI());
					} else {
						retries++;
					}
				}
			}
		}
	}

	public byte[] getImage(String lnk) {
		if (lnk.split("\\?").length == 2) {
			HTTPRequest req = new HTTPRequest(lnk.split("\\?")[0], POST);
			String params[] = lnk.split("\\?")[1].split("&");
			for (String s : params) {
				req.setPostParameter(s.split("=")[0],
						s.split("=").length == 2 ? s.split("=")[1] : "");
			}

			HTTPResponse res = process(req);

			if (res.returnCode == 500)
				return null;
			
			return res.getResponseAsByte();
		} else
			return null;
	}

	private HashMap<String, String> getNewParams(String resp) {
		HashMap<String, String> params = new HashMap<String, String>();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(resp, null);
			NodeList nodeList = htmlParser.parse(null);

			nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter(
					"form"), true);

			if (nodeList.size() == 1) {
				NodeList children = nodeList.elementAt(0).getChildren();

				for (int i = 0; i < children.size(); i++) {
					if (children.elementAt(i) instanceof InputTag) {
						String key = ((InputTag) children.elementAt(i))
								.getAttribute("name");
						String val = ((InputTag) children.elementAt(i))
								.getAttribute("value");
						params.put(key, val);
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return params;
	}

	private HashMap<String, String> getImageParams(String resp) {
		HashMap<String, String> params = new HashMap<String, String>();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(resp, null);
			NodeList nodeList = htmlParser.parse(null);

			nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter(
					"param"), true);

			if (nodeList.size() > 0) {
				for (int i = 0; i < nodeList.size(); i++) {
					if (nodeList.elementAt(i) instanceof TagNode) {
						TagNode node = (TagNode) nodeList.elementAt(i);
						params.put(
								node.getAttribute("name") != null ? node
										.getAttribute("name") : "",
								node.getAttribute("value") != null ? node
										.getAttribute("value") : "");
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return params;
	}

	public String getCertDate() {
		onLogin();
		return getCert_date_resp();
	}
}

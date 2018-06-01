package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.servers.types.XXGenericPublicDataParentSiteConfiguration;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

/**
 * 
 * @author Oprina George
 * 
 *         Aug 12, 2011
 */

public class XXGenericPublicData extends HttpSite {
	private static final String PASSWORD2 = "password";
	private static final String STATE_ID = "state_id";
	private static final String LOGIN_ID = "login_id";
	private static final String sitePasswordKey = "XXGenericPublicData";

	@Override
	public LoginResponse onLogin() {
		HTTPRequest request = new HTTPRequest(
				"http://www.publicdata.com/pdmain.php/Logon/checkAccess",
				HTTPRequest.POST);

		String loginId = SitesPasswords.getInstance().getPasswordValue(
				getCurrentCommunityId(searchId), sitePasswordKey, LOGIN_ID);
		String stateId = SitesPasswords.getInstance().getPasswordValue(
				getCurrentCommunityId(searchId), sitePasswordKey, STATE_ID);
		String password = SitesPasswords.getInstance().getPasswordValue(
				getCurrentCommunityId(searchId), sitePasswordKey, PASSWORD2);
		
		if(StringUtils.isEmpty(loginId) || 
				StringUtils.isEmpty(stateId) ||
				StringUtils.isEmpty(password)) {
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}

		request.setPostParameter(LOGIN_ID, loginId);
		request.setPostParameter(STATE_ID, stateId);
		request.setPostParameter(PASSWORD2, password);
		request.setPostParameter("login", "Login");
		request.setPostParameter("Process", "Logon");
		HTTPResponse response = process(request);
		String page = response.getResponseAsString();
		String url = RegExUtils.getFirstMatch(
				"document.location.href\\s*=\\s*'(.*?)'", page, 1);
		if(StringUtils.isEmpty(url)) {
			return LoginResponse.getDefaultFailureResponse();
		}
		
		String id = url.substring(url.lastIndexOf("/") + 1);
		if(StringUtils.isEmpty(id)) {
			return LoginResponse.getDefaultFailureResponse();
		}
		getAttributes().put("" + getSearchId(), id);
		request = new HTTPRequest(url, HTTPRequest.GET);
		response = process(request);

		//quick fix, hope it worked
		return LoginResponse.getDefaultSuccessResponse();
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.POST) {

			if (req.hasPostParameter("login")
					|| req.hasPostParameter("pageid_string"))
				return;

			String sessionID = (String) getAttribute("" + getSearchId());
			String loginId = SitesPasswords.getInstance().getPasswordValue(
					getCurrentCommunityId(searchId), sitePasswordKey, LOGIN_ID);
			String stateId = SitesPasswords.getInstance().getPasswordValue(
					getCurrentCommunityId(searchId), sitePasswordKey, STATE_ID);
			Map<String, String> params = new HashMap<String, String>();
			// =smith&p2=&p3=&input=GRP_CIV_NAME&type=name&=LANDAT001&dlstate=CORP&id=0A54422974D5C7D30B6415947C3C2DC1&o=GRP_CIV_NAME

			String type = org.apache.commons.lang.StringUtils.defaultIfEmpty(
					req.getPostFirstParameter("type"), "");

			params.put("url", req.getURL());
			params.put("dlnumber", loginId);
			params.put("dlstate", stateId);
			params.put("id", sessionID);
			params.put("type", type);

			if (type.equalsIgnoreCase("name")) {

				String o = req.getPostFirstParameter("o");
				String o2 = req.getPostFirstParameter("o2");
				String input = req.getPostFirstParameter("input");

				if (o.equals("GRP_DL_NAME")) {
					params.put("tacDMV", input);
					input = o2;
				} else if (XXGenericPublicDataParentSiteConfiguration.SEARCH_ALL_ITEMS_BELOW_LIST
						.contains(o2.toUpperCase())
						&& input.equalsIgnoreCase("FAKE_ALL")) {
					input = o;
				} else {
					o = o2;
					if(StringUtils.isEmpty(input))
						input = o2;
				}

				params.put("p1", req.getPostFirstParameter("p1") == null ? ""
						: req.getPostFirstParameter("p1"));
				params.put("p2", req.getPostFirstParameter("p2") == null ? ""
						: req.getPostFirstParameter("p2"));
				params.put("p3", req.getPostFirstParameter("p3") == null ? ""
						: req.getPostFirstParameter("p3"));
				params.put("o", o);
				params.put("input", input);

				req.clearPostParameters();
				req.setMethod(HTTPRequest.GET);
				req.setURL(makeUrl(params));

			} else if (req.hasPostParameter("o")
					&& (req.getPostFirstParameter("o").equalsIgnoreCase(
							"GRP_DMV_VIN") || req.getPostFirstParameter("o")
							.equalsIgnoreCase("GRP_DMV_PLATE"))) {
				String o = req.getPostFirstParameter("o");
				String input = req.getPostFirstParameter("input");
				String tacDMV = req.getPostFirstParameter("tacDMV");

				params.put("p1", req.getPostFirstParameter("p1") == null ? ""
						: req.getPostFirstParameter("p1"));

				params.put("o", o);
				params.put("input", input);
				params.put("tacDMV", tacDMV);

				if (o.contains("VIN"))
					params.put("type", "vin");
				else if (o.contains("PLATE"))
					params.put("type", "plate");

				req.clearPostParameters();
				req.setMethod(HTTPRequest.GET);
				req.setURL(makeUrl(params));
			} else if (!req.hasPostParameter("login")) {
				params = new HashMap<String, String>();

				params.put("url", req.getURL());

				HashMap<String, ParametersVector> param = req
						.getPostParameters();

				for (String s : param.keySet()) {
					params.put(s, param.get(s).getFirstParameter());
				}

				req.clearPostParameters();
				req.setMethod(HTTPRequest.GET);
				req.setURL(makeUrl(params));
			}
		}
	}

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		super.onAfterRequest(req, res);
	}

	private String makeUrl(Map<String, String> params) {
		StringBuffer url = new StringBuffer();

		url.append(params.get("url"));

		params.remove("url");

		for (String s : params.keySet())
			url.append("&"
					+ s
					+ "="
					+ (params.get(s) == null || params.get(s).equals("") ? ""
							: params.get(s)));

		return url.toString().replaceFirst("&", "?");
	}
}

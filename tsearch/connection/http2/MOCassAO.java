package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

public class MOCassAO extends HttpSite {

	private static final String[] URL_PARAM_NAMES = {"AppID", "LayerID", "PageTypeID", "PageID", "Q"};
	private static final String[] FORM_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION"};
	private static final String[] GT_PARAM_NAMES = { "ctlBodyPane$ctl05$btnSearch", "ctlBodyPane$ctl08$btnSearch"};
	
	private Map<String,String> formParams = new HashMap<String,String>();
	
	@Override
	public LoginResponse onLogin() {
		
		HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
		String responseAsString  = execute(req);
		
		formParams = isolateParams(responseAsString, "Form1");
		if (!checkParams(formParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
			
		return LoginResponse.getDefaultSuccessResponse();
	}
	
	public static Map<String, String> isolateParams(String page, String form){
		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : FORM_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		
		return addParams;
	}
	
	public static boolean checkParams(Map<String, String> addParams){
		for(String key: FORM_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return false;
			}
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		if (req.getMethod()==HTTPRequest.POST) {			
			String q = null;
			StringBuilder url_sb = new StringBuilder(req.getURL() + "?");
			for (int i=0;i<URL_PARAM_NAMES.length;i++) {	//move parameters to url
				String value = req.getPostFirstParameter(URL_PARAM_NAMES[i]);
				if ("Q".equals(URL_PARAM_NAMES[i])) {
					q = value;
				}
				if (value!=null) {
					url_sb.append(URL_PARAM_NAMES[i]).append("=").append(value).append("&");
					req.removePostParameters(URL_PARAM_NAMES[i]);
				}
			}
			String url = url_sb.toString();
			url = url.replaceFirst("&$", "");
			url = url.replaceFirst("\\?$", "");
			req.modifyURL(url);
			if (q==null) {	//intermediary page
				for (String key: FORM_PARAM_NAMES) {
					req.setPostParameter(key, formParams.get(key));
				}
				for (int i=0;i<GT_PARAM_NAMES.length;i++) {
					String value = req.getPostFirstParameter(GT_PARAM_NAMES[i]);
					if (value!=null) {
						value = value.replaceAll("(?i)&gt;", ">");
						req.removePostParameters(GT_PARAM_NAMES[i]);
						req.setPostParameter(GT_PARAM_NAMES[i], value);
					}
				}
			} else {		//details page
				String keyParameter = req.getPostFirstParameter("seq");
				Map<String, String>  params = null;
				if (keyParameter!=null)
				{
					params = (Map<String, String>) getTransientSearchAttribute("params:" + keyParameter);
					req.removePostParameters("seq");
				}	
				if (params!=null)
				{
					for (String k: params.keySet())
						req.setPostParameter(k, params.get(k));
				}
			} 
		}
	}
}

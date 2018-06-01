/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 */
public class COGenericValuewestAO extends HttpSite {

/*	private static final String[] PARAM_NAMES = { 
		"scrMgrAjax",
		"scrMgrAjax_HiddenField",
		"__EVENTTARGET", 
		"__EVENTARGUMENT", 
		"__VIEWSTATE",
		"cboSearch",
		"txtSearch",
		"AccountListCollapsiblePanel_ClientState",
		"AccountDetailCollapsiblePanel_ClientState",
		"SalesCollapsiblePanel_ClientState",
		"SalesCompCollapsiblePanel_ClientState",
		"ValuesCollapsiblePanel_ClientState",
		"ModelsCollapsiblePanel_ClientState",
		"txtUserID",
		"txtPassword",	
		"AccountListRowCount",
		"AccountDetailRowCount",
		"ValuesListRowCount",
		"SalesListRowCount",
		"SalesListCompRowCount",
		"ModelOccurrenceListRowCount",
		"pnlAccountListScrollPos",
		"pnlLeftScrollPos",
		"__SCROLLPOSITIONX",
		"__SCROLLPOSITIONY",
		"__EVENTVALIDATION"
	};*/
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	private static final String FORM_NAME = "frmMain";
	
	@Override
	public LoginResponse onLogin() {

		// go to taxes search 
		HTTPRequest req = new HTTPRequest(getSiteLink(), GET);
		
		if (req.headers.get("User-Agent").toString().contains("Mozilla/4.0")) {
			req.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:18.0) Gecko/20100101 Firefox/18.0");
		}	
		String p = execute(req);
		
		Map<String, String> params = new SimpleHtmlParser(p).getForm(FORM_NAME).getParams();
		Map<String, String> addParams = new HashMap<String, String>();
		
		// check parameters
		for(String key : REQ_PARAM_NAMES){
			if(!params.containsKey(key)){
				String errorMsg = "Did not find required parameter " + key;
				logger.error(errorMsg);
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, errorMsg);
			}
			addParams.put(key, params.get(key));
			params.remove(key);
		}
		if (params != null && !params.isEmpty()){
			params.remove("txtSearch");
			params.remove("hidSearchTypeSelectedIndex");
			addParams.putAll(params);
		}
		try {

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(p, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node button = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "cmdSearch"), true)
				.elementAt(0);
			if (button != null) {
				String buttonHtml = button.toHtml();
				if (buttonHtml.contains("&#39;")){
					buttonHtml = buttonHtml.replaceAll("&#39;", "'");
				}
				Matcher matcher = Pattern.compile("(?is)onclick=\"pnlPopupClient.Show[(][)];\\s*__doPostBack\\('([^']+)','([^']*)'\\);?\"").matcher(buttonHtml);
				if (matcher.find()){
					addParams.remove("__EVENTTARGET");
					addParams.remove("__EVENTARGUMENT");
					addParams.put("__EVENTTARGET", matcher.group(1));
					addParams.put("__EVENTARGUMENT", matcher.group(2));
				}
			}
			
		} catch (Exception  e) {
			e.printStackTrace();
		}
			
		// store parameters
		setAttribute("params", addParams);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		 
		if (req.headers.get("User-Agent").toString().contains("Mozilla/4.0")) {
			req.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:18.0) Gecko/20100101 Firefox/18.0");
		}	
		
		if(req.hasPostParameter("seq")) { // details link
			// remove the seq parameter
			String seq = req.getPostFirstParameter("seq");
			req.removePostParameters("seq");		
	
			Map<String, String> params = (Map<String, String>) getTransientSearchAttribute("params:" + seq);			
			if(params != null){
				String url = req.getURL();
				url = StringUtils.urlDecode(req.getURL());
				url = url.replaceAll("&amp;", "&");
				req.modifyURL(url);
				for(String key : REQ_PARAM_NAMES){
					if(params.containsKey(key)) {
						req.setPostParameter(key, params.get(key));
					} else {
						logger.error("Did not find required parameter " + key);
					}
				}
			}
		} else { // search from Parent Site
			Map<String,String> params = (Map<String,String>)getAttribute("params");
			if(params != null) {
				for(Map.Entry<String, String> entry : params.entrySet()){
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			}
		}
		
		if (req.getPostFirstParameter("scrMgrAjax") == null && req.getPostFirstParameter("__EVENTTARGET").contains("grdAccountList")) {
			String val = req.getPostFirstParameter("__EVENTTARGET"); 
			req.setPostParameter("scrMgrAjax", "pnlUpdate|" + val);
		}
//		Vector<String> order = new Vector<String>(Arrays.asList(PARAM_NAMES));
//		req.setPostParameterOrder(order);
	}

}

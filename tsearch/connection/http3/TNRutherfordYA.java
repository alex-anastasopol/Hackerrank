package ro.cst.tsearch.connection.http3;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.FrameTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.StringUtils;


public class TNRutherfordYA extends HttpSite3 {
	
	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE" };
	private String correctLink = null;
	@Override
	public LoginResponse onLogin() {
	
		DataSite dataSite = getDataSite(); 
		HTTPRequest request = new HTTPRequest(dataSite.getLink());
		String responseAsString = execute(request);
		Map<String,String> addParams = isolateParams(responseAsString, "aspnetForm");
		
		String nextLink = getCorrectLink(new HtmlParser3(responseAsString));

		if(org.apache.commons.lang.StringUtils.isBlank(nextLink)) 
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid next link");
			
		//   |http://www4.murfreesborotn.gov| /dasdas
		correctLink = nextLink.substring(0, nextLink.indexOf("/", nextLink.indexOf("//") + 2));
		
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("params", addParams);
		
		setAttribute(HttpSite3.SINGLE_LINE_COOKIES_IN_REDIRECT, Boolean.TRUE);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	public String getCorrectLink(HtmlParser3 parser) {
		NodeList someNodes = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "Section1"))
				.extractAllNodesThatMatch(new TagNameFilter("iframe"), true);
		
		if(someNodes.size() > 0) {
			return((TagNode)someNodes.elementAt(0)).getAttribute("src");
		}
		return null;
	}
	
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".gov");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}
	
	public static Map<String, String> isolateParams(String page, String form){
		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		
		return addParams;
	}
	
	public static boolean checkParams(Map<String, String> addParams){
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
						
		Map<String, String>  addParams = new HashMap<String, String>();
		String keyParameter = req.getPostFirstParameter("seq");
		
		String initialURL=req.getURL();
		String initialURLBase=initialURL.substring(0, initialURL.indexOf("/", initialURL.indexOf("//") + 2));
		String correctURL=initialURL.replace(initialURLBase, correctLink);
		req.modifyURL(correctURL);
		
		if (keyParameter!=null)
		{
			addParams = (Map<String, String>) getTransientSearchAttribute("params:" + keyParameter);
			req.removePostParameters("seq");
		}	
		if (addParams!=null)
		{
			for (String k: addParams.keySet())
				req.setPostParameter(k, addParams.get(k));
		}
	}

}

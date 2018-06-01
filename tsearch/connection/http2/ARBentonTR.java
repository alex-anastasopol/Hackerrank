
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import java.util.Map;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author vladb
 *
 */
public class ARBentonTR extends HttpSite {

	private static final String FORM_NAME = "aspnetForm";
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	private String sid;
	@Override
	public LoginResponse onLogin() {
		
		try {
			// go to taxes search 
			HTTPRequest req = new HTTPRequest(getSiteLink(), GET);   
			//get session id for post request
			req.noRedirects = true;
			String response = execute(req); 
			
			String host=getSiteLink();
			HtmlParser3 parser= new HtmlParser3(response);
			NodeList sidNode = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("body"), true)
					.extractAllNodesThatMatch(new TagNameFilter("h2"), true)
					.extractAllNodesThatMatch(new TagNameFilter("a"), true);
			sid=((TagNode)sidNode.elementAt(0)).getAttribute("href");
			sid=sid.replaceAll("%..", "/");
			
			req=new HTTPRequest(host+sid, GET);
			response = execute(req);
			
			Map<String, String> params = new SimpleHtmlParser(response).getForm(FORM_NAME).getParams();
			
			// check parameters
			for(String key : REQ_PARAM_NAMES){
				if(!params.containsKey(key)){
					String errorMsg = "Did not find required parameter " + key;
					logger.error(errorMsg);
					return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, errorMsg);
				}
			}
				
			// store parameters
			setAttribute("params", params);
		
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) { 
		 
		Map<String, String> params = null;
	       
		if(req.hasPostParameter("seq")) { // details link
			// remove the seq parameter
			String seq = req.getPostFirstParameter("seq");
			req.removePostParameters("seq");
	
			params = (Map<String, String>) getTransientSearchAttribute("params:" + seq);
			params.remove("ctl00$ContentPlaceHolder1$Button1");
			
		} else { // search from Parent Site
			params = (Map<String,String>)getAttribute("params");
		}
		
		// check parameters
		if(params != null) {
			for(String key : REQ_PARAM_NAMES){
				if(!params.containsKey(key)){
					logger.error("Did not find required parameter " + key);
				}
			}
			for(Map.Entry<String, String> entry : params.entrySet()){
				if(!req.hasPostParameter(entry.getKey())) {
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			}
		}
		String host=getSiteLink();
		String currentRequest = host+sid;
		req.setURL(currentRequest);	
	}
}

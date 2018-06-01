package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.parser.SimpleHtmlParser.Input;


/**
 * @author mihaib
*/

public class MOCassRO extends HttpSite{

	/** The Content-Type for multipart/form-data. */
	public static final String MULTIPART_FORM_CONTENT_TYPE = "multipart/form-data";
	
	public static final Pattern SORT_LINK_PAT = Pattern.compile("(?is)<a\\s+href\\s*=\\s*\\\"([^\\\"]+)");

	@Override
	public LoginResponse onLogin() {
		
		String link = getSiteLink();

		HTTPRequest req = new HTTPRequest(link + "login.asp", HTTPRequest.GET);
		String resp = execute(req);
		
		req = new HTTPRequest(link + "login.asp", HTTPRequest.POST);
		req.setPostParameter("Logid", getPassword("user"));
		req.setPostParameter("Password", getPassword("password"));
		req.setPostParameter("Login", "Login");
		req.noRedirects = true;
		resp = execute(req);
		if(resp.contains("was invalid")){
			logger.error("Log-In / Password combination was invalid. ");
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Invalid credentials");
		}
		//Pattern newSearch = Pattern.compile("(?is)<a.*?href\\s*=\\s*\\\"([^\\\"]*)\\\"\\s*>New Search");
		//Matcher mat = newSearch.matcher(resp);
		//if (mat.find()){
			req = new HTTPRequest(link + "or_sch_1.asp", HTTPRequest.GET);
			req.setHeader("Referer", link + "login.asp");
			req.noRedirects = false;
			resp = execute(req);
		//}
		
		
		// indicate success
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {

		if (req.getPostParameter("img") != null){
			Map<String,String> params = (Map<String, String>)getTransientSearchAttribute("image:");
	         if (params != null) {
	        	 List<Part> parts = new ArrayList<Part>();
	        	 for(Map.Entry<String, String> entry: params.entrySet()){
	        		 StringPart sp = new StringPart(entry.getKey(), entry.getValue()); 
	        		 sp.setTransferEncoding(null);
	        		 parts.add(sp);
	        	 }
	        	 req.setPartPostData(parts.toArray(new Part[0]));
	         }
		} 
    	
    	return;
	}	
	
	public String getPage(String src) {
		
		String link = getSiteLink() + src;
		
		HTTPRequest req = new HTTPRequest(link);
		
		return execute(req);
	}

	public byte[] getImage(String lnk){
		String link = getSiteLink() + lnk;
		
		HTTPRequest req = new HTTPRequest(link);
		String res = execute(req);
		try {
			Form form = new SimpleHtmlParser(res).getForm("courtform");
			HashMap<String, String> params = new HashMap<String, String>();
			String url = form.action;
			List<Input> inputs = form.inputs;
			for (Input inp : inputs) {
				//if (inp.name.contains("page") || inp.name.contains("WaterMarkText")	|| inp.name.contains("id")) {
					params.put(inp.name, inp.value);
				//}
			}

			req = new HTTPRequest(url, HTTPRequest.POST);
			if (params != null) {
				List<Part> parts = new ArrayList<Part>();
				for (Map.Entry<String, String> entry : params.entrySet()) {
					StringPart sp = new StringPart(entry.getKey(), entry
							.getValue());
					sp.setTransferEncoding(null);
					parts.add(sp);
				}
				req.setPartPostData(parts.toArray(new Part[0]));
			}
			//String dfd = execute(req);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return process(req).getResponseAsByte();
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		String content = "";
		if (!req.getURL().toLowerCase().contains("pdf")){
			if (res.getContentType().contains("text/html")){
				content = res.getResponseAsString();
				res.is = new ByteArrayInputStream(content.getBytes());
				if(content.contains("Registered Log In")) {
					destroySession();
				}
			}
		}
		
	}
	
}
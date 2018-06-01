package ro.cst.tsearch.connection.http2;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;

public class TNSullivanTR extends HttpSite {

	private String SESSION;
	private static Pattern URL_PAT = Pattern.compile("(?is)\\burl=\\\"([^\\\"]+)");
	private static Pattern SESSION_PAT = Pattern.compile("(?is)\\bsession=\\\"([^\\\"]+)");
	
	public LoginResponse onLogin() {
		
		// get the search page
		String siteLink = getSiteLink();
		String host = siteLink.substring(0, siteLink.indexOf(".com") + 4);
		HTTPRequest req = new HTTPRequest(siteLink);

		String response = execute(req);

		if (response.indexOf("User Access") < 0){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page not found");
		} else{
			String user = getPassword("user");
			String password = getPassword("password");
			
			if(StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password)){
				req = new HTTPRequest(host + "/dx/cgi-bin/dxserversul.cgi", HTTPRequest.POST);
				req.setPostParameter("ID", user);
				req.setPostParameter("Password", password);
				req.setPostParameter("x", "29");
				req.setPostParameter("y", "7");
				req.setPostParameter("Option", "Login");
				
				response = execute(req);
				
				Matcher mat = URL_PAT.matcher(response);
				if (mat.find()){
					req = new HTTPRequest(host + mat.group(1), HTTPRequest.GET);
					response = execute(req);
					
					if (response.contains("loaddata()")){
						mat = SESSION_PAT.matcher(response);
						if (mat.find()){
							SESSION = mat.group(1);
							req = new HTTPRequest(host + "/dx/cgi-bin/dxserversul.cgi?Option=PTLkUp&Action=Input&S_Unique=1349962716728&Session=" + SESSION, HTTPRequest.GET);
							response = execute(req);
							
							if (response.contains("PROPERTY TAX")){
								SESSION = ro.cst.tsearch.utils.StringUtils.getTextBetweenDelimiters("NAME=\"SESSION\" VALUE=\"" , "\"", response);
								
								// indicate success
								return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
							}
						} 
					}
				}
			} else{
				return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Invalid credentials");
			}
		}

		return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page not found");
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req){
		
		String url = req.getURL();	
		url = url.replaceAll("&amp;", "&");
		
		if (req.getMethod() == HTTPRequest.POST){
			//get the module
			if (req.getPostFirstParameter("U_LookUpBy") != null){
				String lookupBY = req.getPostFirstParameter("U_LookUpBy");
				if (StringUtils.isNotEmpty(lookupBY)){
					HTTPRequest newRequest = new HTTPRequest(url, HTTPRequest.POST);
					newRequest.setPostParameter("SESSION", SESSION);
					newRequest.setPostParameter("U_LookUpBy", req.getPostFirstParameter("U_LookUpBy"));
					newRequest.setPostParameter("U_PTFocus", req.getPostFirstParameter("U_PTFocus"));
					newRequest.setPostParameter("PTRec", "");
					newRequest.setPostParameter("U_PType", req.getPostFirstParameter("U_PType"));
					newRequest.setPostParameter("PTKeyValue", req.getPostFirstParameter("PTKeyValue"));
					newRequest.setPostParameter("OPTION", req.getPostFirstParameter("OPTION"));
					newRequest.setPostParameter("Action", req.getPostFirstParameter("Action"));
					String getModuleResponse = execute(newRequest);
					if (getModuleResponse.contains("PROPERTY TAX LookUp")){
						Form form = new SimpleHtmlParser(getModuleResponse).getForm("form1");
						if (form != null){
							//Map<String, String> params = new HashMap<String, String>();
							String session = form.getParams().get("SESSION");
							if (StringUtils.isNotEmpty(session)){
								SESSION = session;
							}
						}
					}
				}
			}
			
			if (req.getPostFirstParameter("SESSION") != null){
				req.removePostParameters("SESSION");
				req.setPostParameter("SESSION", SESSION);
			} else{
				String seq = req.getPostFirstParameter("seq");
				if (seq != null){
					req.removePostParameters("seq");
					
					// additional parameters are stored inside the search since they are too long
					Map<String, String> addParams = (Map<String, String>)getTransientSearchAttribute("params:" + seq);
					if (addParams != null){
						req.setPostParameter("SESSION", addParams.get("SESSION"));
					}
				}
			}
		}
		
		HTTPResponse response = process(req);
		String resp = response.getResponseAsString();
		int i = 0;
		while (resp.contains("KeyMore=\"") && i < 5){
			Form form = new SimpleHtmlParser(resp).getForm("form1");
			if (form != null){
				//Map<String, String> params = new HashMap<String, String>();
				String session = form.getParams().get("SESSION");
				if (StringUtils.isNotEmpty(session)){
					SESSION = session;
					
					if (req.getPostFirstParameter("SESSION") != null){
						req.removePostParameters("SESSION");
						req.setPostParameter("SESSION", SESSION);
					}
					response = process(req);
					resp = response.getResponseAsString();
					i++;
				}
			}
		}
		
		if (resp.contains("Revalidate Password")){
			onLogin();
			
			url = req.getURL();
			if (req.getMethod() == HTTPRequest.GET){
				url = url.replaceAll("(?is)\\b(Session=)[^&]+", "$1" + SESSION).replaceAll("(?is)\\bdummy=true&", "");
				req.setURL(url);
			} else{
				req.removePostParameters("SESSION");
				req.setPostParameter("SESSION", SESSION);
			}
			response = process(req);
			resp = response.getResponseAsString();
			
			response.contentType = "text/html; charset=utf-8";
			// bypass response
			response.is = IOUtils.toInputStream(resp);
			req.setBypassResponse(response);

		} else{
			response.contentType = "text/html; charset=utf-8";
			// bypass response
			response.is = IOUtils.toInputStream(resp);
			req.setBypassResponse(response);
		}
		
		
	}

}

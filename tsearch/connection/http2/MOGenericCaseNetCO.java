package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.parser.SimpleHtmlParser;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;
import static ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import static ro.cst.tsearch.parser.SimpleHtmlParser.Select;
import static ro.cst.tsearch.parser.SimpleHtmlParser.Option;

public class MOGenericCaseNetCO extends HttpSite {

	private static String host = "https://www.courts.mo.gov";
	private static Map<String,Map<String,String>> cachedParams = new HashMap<String,Map<String,String>>();
	
	protected Map<String,String> getParams(String county, String sType){			
		synchronized(cachedParams){
			
			String key = county + "_" + sType;	
			Map<String,String> params = cachedParams.get(key);
			if(params != null){
				return params;
			}
			
			// get the home page
			HTTPRequest req = new HTTPRequest(host + "/casenet/cases/searchCases.do", GET);
			String response = execute(req);

			// get name search page
			req = new HTTPRequest(host + "/casenet/cases/searchCases.do?searchType=name", GET);
			response = execute(req);
			
			// obtain the change court parameters
			SimpleHtmlParser parser = new SimpleHtmlParser(response);
			Form form = parser.getForm("nameSearchForm");			
			Select select = form.getSelect("courtId");
			Option option = select.getOption(county);
			params = form.getParams();
			params.put("inputVO.subAction", "court");
			params.put("inputVO.type", option.attributes.get("type"));
			params.put("inputVO.courtId", option.attributes.get("value"));
			params.put("courtId", option.attributes.get("value"));
			params.put("inputVO.selectedIndexCourt", String.valueOf(option.index));
			params.remove("findButton");
			
			// change the court
			req = new HTTPRequest(host + form.action, POST);
			req.addPostParameters(params);
			response = execute(req);
			
			// obtain the change county parameters
			parser = new SimpleHtmlParser(response);
			form = parser.getForm("nameSearchForm");
			select = form.getSelect("inputVO.countyCode");
			option = select.getOption(county);
			params = form.getParams();
			params.remove("findButton");
			params.remove("selectedAlias");
			params.put("inputVO.countyCode", option.attributes.get("value"));
			params.put("inputVO.subAction", "county");
			params.put("courtId", params.get("inputVO.courtId"));
			
			// change county
			req = new HTTPRequest(host + form.action, POST);
			req.addPostParameters(params);
			response = execute(req);			
			
			// obtain the search parameters
			parser = new SimpleHtmlParser(response);
			form = parser.getForm("nameSearchForm");
			params = form.getParams();
			params.remove("findButton");
			params.put("inputVO.subAction", "search");
			params.put("courtId", params.get("inputVO.courtId"));

			cachedParams.put(key,params);
			return params;
		}		
	}
	
	public LoginResponse onLogin(){
		
		// make sure we have the parameters
		try {
			
			Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
	        Protocol.registerProtocol("https", easyhttps);
			
			getParams(getCounty(), "name");
		} catch (Exception e){
			logger.error("Cannot obtain the parameters!");
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		// get the home page
		HTTPRequest req = new HTTPRequest(host + "/casenet/cases/searchCases.do", GET);
		execute(req);
				
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {	
		
		// don't do anything during logging in
		if(status != STATUS_LOGGED_IN){
			return;
		}
		
		// don't do anything while we're already inside a onBeforeRequest call
		if(getAttribute("onBeforeRequest") == Boolean.TRUE){
			return;
		}
		
		// mark that we're treating onBeforeRequest
		setAttribute("onBeforeRequest", Boolean.TRUE);
		
		String url = req.getURL();
		if (url.contains("http:")) {
			url = url.replaceAll("http:", "https:");
			req.modifyURL(url);
		}
		
		try{
			// if not initial search, then add parameters
			if(!req.hasPostParameter("inputVO.courtId")){
				Map<String,String> params = new HashMap<String,String>(getParams(getCounty(), "name"));
				for(Map.Entry<String, ParametersVector> entry: (Set<Map.Entry<String, ParametersVector>>)req.getPostParameters().entrySet()){
					String key = entry.getKey();
					String value = (String)entry.getValue().get(0);
					params.put(key, value);
					if("inputVO.aliasFlag".equals(key) && "Y".equals(value)){
						params.put("selectedAlias", "on");
					}
				}
				req.clearPostParameters();
				req.addPostParameters(params);			}
		} finally {
			setAttribute("onBeforeRequest", Boolean.FALSE);
		}
	}
}

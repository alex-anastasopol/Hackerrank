package ro.cst.tsearch.connection.http2;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Option;
import ro.cst.tsearch.parser.SimpleHtmlParser.Select;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.utils.StringUtils;

public class TXGenericSWData extends HttpSite {
	
	private static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION",};
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	public String cookie="";
	private static final Pattern txtHiddenNewsPAT = Pattern.compile("(?i)<inpu.*?name\\s*=\\s*\\\"txtHiddenNews.*?value\\s*=\\s*\\\"([^\\\"]*)\\\"");
	private String dbKey = "";
		
	@Override
	public LoginResponse onLogin() {

		String link = getCrtServerLink() + ".com";
		String resp = "";
		HTTPRequest req = new HTTPRequest(link + "/corp/", HTTPRequest.GET);
		HTTPResponse res = process(req);
		this.cookie=res.getHeader("Set-Cookie");
		resp = res.getResponseAsString();
		
		if (getDataSite().getSiteTypeInt() == GWTDataSite.TR_TYPE && !getDataSite().getCountyName().toLowerCase().contains("parker")){
			List<Select> select = new SimpleHtmlParser(resp).getForm("frmTax").selects;
			if (select != null){
				List<Option> opts = select.get(0).options;
				for (Option opt : opts){
					if (getDataSite().getCountyName().toLowerCase().contains(opt.text.toLowerCase())){
						link += "/client/webindex.aspx?dbkey=" + opt.value;
						dbKey = opt.value;
						break;
					}
				}
			}
		} else if (getDataSite().getSiteTypeInt() == GWTDataSite.AO_TYPE || getDataSite().getCountyName().toLowerCase().contains("parker")){
			List<Select> select = new SimpleHtmlParser(resp).getForm("frmCad").selects;
			if (select != null){
				List<Option> opts = select.get(0).options;
				for (Option opt : opts){
					if (getDataSite().getCountyName().toLowerCase().contains(opt.text.toLowerCase())){
						link += "/client/webindex.aspx?dbkey=" + opt.value;
						dbKey = opt.value;
						break;
					}
				}
			}
		}
		
		req = new HTTPRequest(link, HTTPRequest.GET);
		res = process(req);
		resp = res.getResponseAsString();
		
		//isolate parameters
		Map<String,String> addParams = isolateParams(resp, "searchForm");
		String action = new SimpleHtmlParser(resp).getForm("searchForm").action;
		if (StringUtils.isNotEmpty(action)){
			action = action.replaceAll("&amp;", "&");
			//addParams.put("dbkey", StringUtils.extractParameterFromUrl(action, "dbkey"));
			addParams.put("time", StringUtils.extractParameterFromUrl(action, "time"));
		}
		
		Matcher txtHiddenNewsMat = txtHiddenNewsPAT.matcher(resp);
		if (txtHiddenNewsMat.find()){
			addParams.put("txtHiddenNews", txtHiddenNewsMat.group(1));
		}
		
		// check parameters
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		setAttribute("params", addParams);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
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
		
	public static HTTPRequest addParams(Map<String, String> addParams, HTTPRequest request){
		Iterator<String> i = addParams.keySet().iterator();
		while(i.hasNext()){
			String k = i.next();
			request.setPostParameter(k, addParams.get(k));
		}		
		return request;
	}
		
	public static boolean checkParams(Map<String, String> addParams){
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return false;
			}
		}
		return true;
	}
		
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".com");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		Map<String,String> addParams = (Map<String, String>) getAttribute("params");
		
		if (req.getMethod() == HTTPRequest.POST){
			String url = req.getURL();
			if (!url.contains("webProperty.aspx")){
				if (StringUtils.isNotEmpty(dbKey)){
					url += "?dbkey=" + dbKey;
				}
				if (addParams.containsKey("time") && req.getURL().contains("webindex.aspx")){
					if (url.contains("dbkey")){
						url += "&time=" + addParams.get("time");
					} else {
						url += "?time=" + addParams.get("time");
					}
					addParams.remove("time");
				}
				req.setURL(url);
				
				//go to search form
				if (!req.getURL().contains("webindex.aspx")){
					HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.GET);
					HTTPResponse res = process(req1);
					String resp = res.getResponseAsString();
					addParams = isolateParams(resp, "searchForm");
				}
			}
			if (addParams != null){
				for(Map.Entry<String, String> entry: addParams.entrySet()){
					if (!req.getURL().contains("webindex.aspx") && entry.getKey().contains("txtHiddenNews")){
						continue;
					}
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			}
		} else if (req.getMethod() == HTTPRequest.GET){
			String link = req.getURL();
			if (link.contains("mapdefault.aspx")){
				HTTPResponse res = process(req);
				String resp = res.getResponseAsString();
				String newUrl = StringUtils.extractParameter(resp, "(?is)name=\\\"frMapMain\\\"\\s+src\\s*=\\s*\\\"([^\\\"]*)");
				req = new HTTPRequest(link.substring(0, link.indexOf("client/") + 7) + newUrl, HTTPRequest.GET);
			} else {
				link = link.replaceAll("\\|", "%7C");
				req.setURL(link);
			}
		}
	}
	
	public String getPage(String link) {
		HTTPRequest req = new HTTPRequest(getCrtServerLink() + ".com" + link, HTTPRequest.GET);
		
		return execute(req);
	}
}

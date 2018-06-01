package ro.cst.tsearch.connection.http2;


import java.net.URLDecoder;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class CODenverTR extends HttpSite{
	
	/**
		*Used also for CODenverAO
	*/
	
	@Override
	public LoginResponse onLogin() {
		
		@SuppressWarnings("unused")
		String resp = "";
		
		String link = getCrtServerLink() + (getDataSite().getName().contains("CODenverTR") ? ".org/apps/treasurypt/PropertyTax.asp?b=1" : ".org/apps/realpropertyapplication/realproperty.asp");
		HTTPRequest req = new HTTPRequest(link);
		resp = execute(req);
		
		// indicate success
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".org");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}
	
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		Map<String,String> addParams = null;
		
		if (req.getMethod() == HTTPRequest.POST && req.hasPostParameter("direction") && req.hasPostParameter("streettype")) {
			if (req.getPostFirstParameter("direction").equals("Select Direction")){
				req.removePostParameters("direction");
				req.setPostParameter("direction", "");
			}
			if (req.getPostFirstParameter("streettype").equals("Select Type")){
				req.removePostParameters("streettype");
				req.setPostParameter("streettype", "");
			}
		}
		if (req.getURL().contains("tmp")){//for CODenverAO
			String url = req.getURL();
			String tmp = StringUtils.extractParameterFromUrl(url, "tmp");
			String searchname = StringUtils.extractParameterFromUrl(url, "searchname");
			req.setPostParameter("searchname", searchname);
			String parcelid = StringUtils.extractParameterFromUrl(url, "parcelid");
			req.setPostParameter("parcelid", parcelid);
			url = url.replaceAll("(?is)\\?.*", "");
			req.setURL(url);
			req.setMethod(HTTPRequest.POST);
			addParams = (Map<String, String>)getTransientSearchAttribute("params:" + tmp);
			for(Map.Entry<String, String> entry: addParams.entrySet()){
				req.setPostParameter(entry.getKey(), entry.getValue());
			}
		}
		if (req.getMethod() == HTTPRequest.POST && req.hasPostParameter("searchname")) {	//for CODenverAO, Name Search
			String searchname = req.getPostFirstParameter("searchname");
			searchname = URLDecoder.decode(searchname);
			req.removePostParameters("searchname");
			req.setPostParameter("searchname", searchname);
		}
		
		@SuppressWarnings("unused")
		String resp = "";
		resp = execute(req);
    	
    	return;
	}	

}

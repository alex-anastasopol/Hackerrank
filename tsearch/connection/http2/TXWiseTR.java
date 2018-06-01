package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parentsitedescribe.ServerInfoDSMMap;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.RegExUtils;

public class TXWiseTR extends SimplestSite {
	@Override
	public LoginResponse onLogin() {
		String siteLink = getSiteLink();
		HTTPRequest firstPageReq = new HTTPRequest(siteLink);
		process(firstPageReq);
		return new  LoginResponse(LoginStatus.STATUS_SUCCESS, "");
	}
	
	public static String getIntermediaryForAccount(String accountId, TXWiseTR site){
		String response = "";
		String link = site.getSiteLink();
		TSServerInfoModule module = site.getDefaultServerInfo().getModule(TSServerInfo.GENERIC_MODULE_IDX);
		List<TSServerInfoFunction> functionList = module.getFunctionList();
		HashMap<String, String> parametersMap = new HashMap<String, String>();
		for (TSServerInfoFunction tsServerInfoFunction : functionList) {
			String paramName = tsServerInfoFunction.getParamName();
			parametersMap.put(paramName, tsServerInfoFunction.getDefaultValue());
			if (tsServerInfoFunction.getName().contains("Account Number")){
				parametersMap.put(paramName, accountId.trim());
			}else{
				if(StringUtils.isNotEmpty(paramName)){
					parametersMap.put(paramName.trim(), tsServerInfoFunction.getDefaultValue().trim());
				}
			}
		}
		
		parametersMap.put("postParams", "true");
//		link = ro.cst.tsearch.utils.StringUtils.addParametersToUrl(link + "?", parametersMap);
		try {
			if (StringUtils.isNotEmpty(accountId)) {
				response = ((ro.cst.tsearch.connection.http2.TXWiseTR) site).getTXWiseTR(link, accountId, parametersMap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		return response;
	}
	
	public String getTXWiseTR(String link, String accountId, Map<String,String> parametersMap) {
		LoginResponse onLogin = onLogin();
		HTTPRequest req = new HTTPRequest(link);
		Set<Entry<String,String>> entrySet = parametersMap.entrySet();
		for (Entry<String, String> entry : entrySet) {
			req.setPostParameter(entry.getKey(), entry.getValue());
		}
		req.setMethod(HTTPRequest.POST);
		HTTPResponse response = process(req);
		return response.getResponseAsString();
	}
	
	public static String getDetailsForTaxHistory(String link, String siteServerName, long searchId){
		HttpSite site = HttpManager.getSite(siteServerName, searchId);
		String response = "";
		try {
			if (StringUtils.isNotEmpty(link)) {
				response = ((ro.cst.tsearch.connection.http2.TXWiseTR) site).getTXWiseTRDetail(link);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		return "";
	}
	
	private String getTXWiseTRDetail(String link) {
		link = getSiteLink() + link;
		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse response = process(req);
		return response.getResponseAsString();
	}

	public TSServerInfo getDefaultServerInfo(){
		TSServerInfo msiServerInfoDefault= null;
		ServerInfoDSMMap DSM= new ServerInfoDSMMap();
    	DSM.setElseParam(false);
		msiServerInfoDefault=DSM.getServerInfo(this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".")+1)+".xml", searchId);		
		return msiServerInfoDefault;
	}
	
	
}

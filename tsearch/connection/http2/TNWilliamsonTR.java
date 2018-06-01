package ro.cst.tsearch.connection.http2;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author oliviav
 */

	public class TNWilliamsonTR extends AdvancedTemplateSite	{
		
	public TNWilliamsonTR() {
		mainParameters = new String[2];
		mainParameters[0] = "__VIEWSTATE";
		mainParameters[1] = "__EVENTVALIDATION" ;
	
		
		mainParametersKey = "search.params";
		formName =  "aspnetForm";
		
		targetArgumentMiddleKey = ":params:";
		targetArgumentParameters = new String[7];
		targetArgumentParameters[0] = "__VIEWSTATE";
		targetArgumentParameters[1] = "__EVENTVALIDATION" ;
		targetArgumentParameters[2] = "__EVENTTARGET";
		targetArgumentParameters[3] = "__EVENTARGUMENT" ;
		targetArgumentParameters[4] = "__LASTFOCUS";
		targetArgumentParameters[5] = "__VIEWSTATEENCRYPTED";
		targetArgumentParameters[6] = "__ncforminfo";
	}
	
	private String county = "";
	private String btnAccept =	 "ctl00$MainContent$SkeletonCtrl_4$btnAccept";
	private String btnSearch =	 "ctl00$MainContent$SkeletonCtrl_4$btnSearch";
	private String drpSearchParam = "ctl00$MainContent$SkeletonCtrl_4$drpSearchParam";

	private String getTaxYearInfo = "";
	
	public String getNcFormInfo (String response) {
		String ncFormInfo = "";
		//<input type="hidden" name="__ncforminfo" value="sMLs_kqwlN8ZfJrgO5WSYMDHBURfcCZAioPJlPYZO5RUOMm5G4sD2Xl4SLuZf8EJ9iXUIINFy2IyYTDJPsgklRrJzdmGp02PX3DAoa4Y85R9li7ziztZxfK7rpb3nNIQ">
		HtmlParser3 parser = new HtmlParser3(response);
		HashMap<String,String> paramsOfReq = parser.getListOfPostParams(formName);
		ncFormInfo = paramsOfReq.get("__ncforminfo");
		
		return ncFormInfo;
	}
	
	public void onBeforeRequestExcl(HTTPRequest req) {
		String seq = req.getPostFirstParameter("seq");
		String searchParam = req.getPostFirstParameter("ctl00$MainContent$SkeletonCtrl_4$drpSearchParam");
		Map<String,String> addParams = null;
		String[] keysToUse = null;
		
		if (req.getURL().contains("default.aspx")) {
			
			if (req.getPostFirstParameter("TaxYear") != null) {
				req.removePostParameters("TaxYear");
			}
			
			if(StringUtils.isNotEmpty(seq)) {
				
				req.removePostParameters("seq");
				
				addParams = (Map<String,String>)InstanceManager.getManager().getCurrentInstance(getSearchId()).getCrtSearchContext()
					.getAdditionalInfo(getDataSite().getName() + targetArgumentMiddleKey + seq);
				
				if ("ctl00$MainContent$SkeletonCtrl_4$btnSearch".equals(req.getPostFirstParameter("__EVENTTARGET"))) {
					keysToUse = mainParameters;
				} else{
					keysToUse = addParams.keySet().toArray(new String[addParams.size()]);
				}
				
			} else {
				addParams = (Map<String,String>)getAttribute(mainParametersKey);
				if (searchParam != null) {
					//sa adaug si pe acel __ncforminfo
					keysToUse = addParams.keySet().toArray(new String[addParams.size()]);
					
					if ("Property ID".equals(searchParam)) {
						HTTPRequest req1 = new HTTPRequest(getSiteLink() + "/default.aspx", HTTPRequest.POST);
						req1.addPostParameters(addParams);
						req1.removePostParameters("__EVENTTARGET");
						req1.setPostParameter("__EVENTTARGET", drpSearchParam);
						req1.setPostParameter("__VIEWSTATEENCRYPTED", "");
						req1.setPostParameter("ctl00$MainContent$SkeletonCtrl_4$drpSearchParam", "Property ID");
						req1.setPostParameter("ctl00$MainContent$SkeletonCtrl_4$drpTaxEntity", req.getPostFirstParameter("ctl00$MainContent$SkeletonCtrl_4$drpTaxEntity"));
						req1.setPostParameter("ctl00$MainContent$SkeletonCtrl_4$drpStatus", req.getPostFirstParameter("ctl00$MainContent$SkeletonCtrl_4$drpStatus"));
						req1.setPostParameter("ctl00$MainContent$SkeletonCtrl_4$drpTaxYear", req.getPostFirstParameter("ctl00$MainContent$SkeletonCtrl_4$drpTaxYear"));
						String respTmp = execute(req1);
						addParams = fillAndValidateConnectionParams(respTmp, mainParameters, formName);
						String ncFormInfo = "";
						
						if (respTmp.contains("__ncforminfo"))  { 
							//must be added this param in order to proceed with search
							ncFormInfo = getNcFormInfo(respTmp);
							if (StringUtils.isNotEmpty(ncFormInfo)) {
								addParams.put("__ncforminfo", ncFormInfo);
							}
						}
					}
					req.setPostParameter("__EVENTTARGET", btnSearch);
					req.setPostParameter("__VIEWSTATEENCRYPTED", "");
					
				} else {
					keysToUse = mainParameters;
				}
			}
			
			for (int i = 0; i < keysToUse.length; i++) {
				req.removePostParameters(keysToUse[i]);
				req.setPostParameter(keysToUse[i], addParams.get(keysToUse[i]));
			}
			
		}
	}

	
	public String getPage(String link, Map<String, String> params) {
		HTTPRequest req = null;
		if (params == null){
			req = new HTTPRequest(link, HTTPRequest.GET);
		} else {
			req = new HTTPRequest(link, HTTPRequest.POST);
			Iterator<String> i = params.keySet().iterator();
			while(i.hasNext()){
				String k = i.next();
				req.setPostParameter(k, params.get(k));
			}
		}
		
		return super.execute(req);
	}
	
	
	public String getMainPageForTaxYear() {
		HTTPRequest req = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		process(req);
		
		return getTaxYearInfo;
	}
	
	public LoginResponse onLogin() {
		HTTPRequest req = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
		String resp = execute(req);
		Map<String,String> addParams = fillAndValidateConnectionParams(resp, mainParameters, formName);
		
		if(addParams == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		req = new HTTPRequest(getSiteLink() + "/default.aspx", HTTPRequest.POST);
		req.addPostParameters(addParams);
		req.removePostParameters("__EVENTTARGET");
		req.setPostParameter("__EVENTTARGET", btnAccept);
		req.setPostParameter("__VIEWSTATEENCRYPTED", "");
		resp = execute(req);
		
		addParams = fillAndValidateConnectionParams(resp, mainParameters, formName);
		String ncFormInfo = "";
		
		if (resp.contains("__ncforminfo"))  { 
			//must be added this param in order to proceed with search
			ncFormInfo = getNcFormInfo(resp);
			if (StringUtils.isNotEmpty(ncFormInfo)) {
				addParams.put("__ncforminfo", ncFormInfo);
			}
		}
		
		if(addParams == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}	
		
		setAttribute(mainParametersKey, addParams);
		req.setPostParameter("__ncforminfo", ncFormInfo);
		
		getTaxYearInfo = extractSelectListTaxYears(resp);
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		
	}
	
	public String extractSelectListTaxYears(String resp) {
		String selectList = "";
		HtmlParser3 parser = new HtmlParser3(resp);
		NodeList nodeList = parser.getNodeList();
		
		nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("select"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "ctl00$MainContent$SkeletonCtrl_4$drpTaxYear"));
		
		if(nodeList != null && nodeList.size() > 0) {
			selectList = nodeList.elementAt(0).toHtml();
		}
		
		return selectList;
	}

}

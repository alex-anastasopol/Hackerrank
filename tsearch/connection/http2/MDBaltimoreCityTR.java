package ro.cst.tsearch.connection.http2;

import java.util.Iterator;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class MDBaltimoreCityTR extends AdvancedTemplateSite {

	public MDBaltimoreCityTR() {
		mainParameters = new String[1];
		mainParameters[0] = "__VIEWSTATE";
		mainParametersKey = "search.params";
		formName =  "aspnetForm";
		
		targetArgumentMiddleKey = ":params:";
		targetArgumentParameters = new String[3];
		targetArgumentParameters[0] = "__VIEWSTATE";
		targetArgumentParameters[1] = "__EVENTTARGET";
		targetArgumentParameters[2] = "__EVENTARGUMENT" ;

	}
	
	
	public LoginResponse onLogin() {
		return super.onLogin();
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
		
		return execute(req);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		String seq = req.getPostFirstParameter("seq");
		Map<String,String> addParams = null;
		String[] keysToUse = null;
		if(StringUtils.isNotEmpty(seq)) {
			req.removePostParameters("seq");
			addParams = (Map<String,String>)InstanceManager.getManager().getCurrentInstance(getSearchId()).getCrtSearchContext()
				.getAdditionalInfo(getDataSite().getName() + targetArgumentMiddleKey + seq);
			keysToUse = addParams.keySet().toArray(new String[addParams.size()]);
			
		} else {
			addParams = (Map<String,String>)getAttribute(mainParametersKey);
			keysToUse = mainParameters;
		}
		if (StringUtils.isEmpty(req.getPostFirstParameter("ctl00$ctl00$rootMasterContent$LocalContentPlaceHolder$btnPrevYear"))) {
			if (keysToUse != null) {
				for (int i = 0; i < keysToUse.length; i++) {
					req.removePostParameters(keysToUse[i]);
					req.setPostParameter(keysToUse[i], addParams.get(keysToUse[i]));
				}
			}
		}	
	}
	
}

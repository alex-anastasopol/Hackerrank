/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import java.util.Iterator;
import java.util.Map;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;


/**
 * @author oliviav
 *
 */
public class COSummitTR extends AdvancedTemplateSite {
	
	public COSummitTR() {
		mainParameters = new String[2];
		mainParameters[0] = "__VIEWSTATE";
		mainParameters[1] = "__EVENTVALIDATION" ;
		
		mainParametersKey = "search.params";
		//formName =  "frmDefault";
		formName =  "aspnetForm";
		
		targetArgumentMiddleKey = ":params:";
		targetArgumentParameters = new String[4];
		targetArgumentParameters[0] = "__VIEWSTATE";
		targetArgumentParameters[1] = "__EVENTVALIDATION" ;
		targetArgumentParameters[2] = "__EVENTTARGET";
		targetArgumentParameters[3] = "__EVENTARGUMENT" ;
		
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		String seq = req.getPostFirstParameter("seq");
		Map<String,String> addParams = null;
		String[] keysToUse = null;
		if (req.getURL().contains("default.aspx")) {
			if(StringUtils.isNotEmpty(seq)) {
				
				req.removePostParameters("seq");
				
				addParams = (Map<String,String>)InstanceManager.getManager().getCurrentInstance(getSearchId()).getCrtSearchContext()
					.getAdditionalInfo(getDataSite().getName() + targetArgumentMiddleKey + seq);
				
				if ("ctl00$contentBody$TabContainer1$pnlResults$gvSearchResults".equals(req.getPostFirstParameter("__EVENTTARGET"))) {
					keysToUse = mainParameters;
				} else{
					keysToUse = addParams.keySet().toArray(new String[addParams.size()]);
				}
				
			} else {
				addParams = (Map<String,String>)getAttribute(mainParametersKey);
				keysToUse = mainParameters;
			}
			
			for (int i = 0; i < keysToUse.length; i++) {
				req.removePostParameters(keysToUse[i]);
				req.setPostParameter(keysToUse[i], addParams.get(keysToUse[i]));
			}
			
			String val = req.getPostFirstParameter("contentBody_TabContainer1_ClientState");
			if (val != null) {
				val = val.replaceAll("&quot;", "\"");
				req.removePostParameters("contentBody_TabContainer1_ClientState");
				req.setPostParameter("contentBody_TabContainer1_ClientState", val);
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
}

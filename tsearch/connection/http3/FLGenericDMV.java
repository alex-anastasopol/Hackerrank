package ro.cst.tsearch.connection.http3;

import java.util.Map;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.RegExUtils;

public class FLGenericDMV extends HttpSite3 {
	
	public static final String VIEWSTATE = "__VIEWSTATE";
	public static final String EVENTVALIDATION = "__EVENTVALIDATION";
	
	private String lastSeq = "";	//used to prevent going to next page when refreshing the current page

	public static String getParamValue(String id, String text) {
		return RegExUtils.getFirstMatch("(?is)<input[^>]+id=\"" + id + "\"[^>]+=\"([^\"]*)\"[^>]/>", text, 1);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		if (req.getMethod()==HTTPRequest.POST) {
			
			String keyParameter = req.getPostFirstParameter("seq");
			if (keyParameter!=null && !lastSeq.equals(keyParameter))	{
				Map<String, String> params = (Map<String, String>) getTransientSearchAttribute("params:" + keyParameter);
				req.removePostParameters("seq");
				for (Map.Entry<String, String> entry : params.entrySet()) {
					String key = entry.getKey();
					String value = entry.getValue();
					if (value!=null) {
						req.setPostParameter(key, value);
					}
				}
				lastSeq = keyParameter;
			} else {
				HTTPRequest reqBefore = new HTTPRequest(req.getURL(), HTTPRequest.GET);
				HTTPResponse respBefore = process(reqBefore);
				String respString = respBefore.getResponseAsString();
				String viewState = getParamValue(VIEWSTATE, respString);
				String eventValidation = getParamValue(EVENTVALIDATION, respString);
				req.setPostParameter(VIEWSTATE, viewState);
				req.setPostParameter(EVENTVALIDATION, eventValidation);
			}
			
		}
	}	
	
}

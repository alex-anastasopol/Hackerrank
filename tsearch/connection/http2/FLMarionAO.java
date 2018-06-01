package ro.cst.tsearch.connection.http2;

import java.net.URLDecoder;
import ro.cst.tsearch.connection.http.HTTPRequest;

public class FLMarionAO extends HttpSite {

	private static final String[] PARAMS = {"__EVENTTARGET", "__EVENTARGUMENT", "__VIEWSTATE", "ctl03$btnMore",
		"ctl03$txtName", "ctl03$txtSitus", "ctl07$txtDesc", "ctl09$ctl00$txtTraverse" ,	
		"ctl11$txtNotes", "__EVENTVALIDATION" };
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		String url = req.getURL();
		if (url.toLowerCase().contains("/default.aspx"))					//change host for details page 
		{
			url = url.replace("www.propappr.marion.fl.us", "216.255.243.135");
			req.setURL(url);
		}
	}
	
	@SuppressWarnings("deprecation")
	public String getPage(String link, String[] values) {
		HTTPRequest req = new HTTPRequest(link);
		if (link.toLowerCase().contains("/default.aspx"))					//details page if there is
		{																	//a "More Owners" button
			req.setMethod(HTTPRequest.POST);
			for (int i=0;i<PARAMS.length; i++)
				if (values[i].length() != 0)
					req.setPostParameter(URLDecoder.decode(PARAMS[i]), values[i]);
		}	
		return execute(req);
	}
	
}

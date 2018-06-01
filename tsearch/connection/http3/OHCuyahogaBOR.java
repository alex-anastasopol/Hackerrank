package ro.cst.tsearch.connection.http3;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class OHCuyahogaBOR extends HttpSite3 {
	
	void shortenParameter(HTTPRequest req, String param, int len) {
		String value = req.getPostFirstParameter(param);
		if (value!=null && value.length()>len) {
			value = value.substring(0, len);
			req.removePostParameters(param);
			req.setPostParameter(param, value);
		}
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req){
		if (HTTPRequest.POST==req.getMethod()) {
			String url = req.getURL();
			if (url.contains("/ComplaintsReports/complaint_list.asp")) {
				shortenParameter(req, "startcomplaint", 15);
				shortenParameter(req, "startparcel", 11);
				shortenParameter(req, "ownername", 50);
			}
		}
	}
	
	public String getPageWithPost(String link) {
		HTTPRequest req = new HTTPRequest(link, HTTPRequest.POST);
		String url = req.getURL();
		int pos = url.indexOf("?");
		if (pos!=-1) {
			String param = url.substring(pos+1);
			String[] split = param.split("&");
			for (String s: split) {
				String[] spl = s.split("=");
				if (spl.length==2) {
					req.setPostParameter(spl[0], spl[1]);
				} else if (spl.length==1) {
					req.setPostParameter(spl[0], "");
				} 
			}
			url = url.substring(0, pos);
			req.modifyURL(url);
		}
		return execute(req);
	}
		
}

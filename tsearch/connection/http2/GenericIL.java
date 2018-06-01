package ro.cst.tsearch.connection.http2;

import java.net.URLEncoder;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class GenericIL extends SimplestSite {
	
	@SuppressWarnings("deprecation")
	@Override
	public void onBeforeRequest(HTTPRequest req){

		String url = req.getURL();
		int pos1 = url.indexOf("?");
		if (pos1!=-1) {
			String address = url.substring(0, pos1+1);
			String params = url.substring(pos1+1);
			StringBuilder newParams = new StringBuilder();
			String[] split = params.split("&");
			for (int i=0;i<split.length;i++) {
				String par = split[i];
				int pos2 = par.indexOf("=");
				if (pos2!=-1) {
					newParams.append(par.substring(0, pos2));
					newParams.append("=");
					newParams.append(URLEncoder.encode(par.substring(pos2+1)));
				} else {
					newParams.append(par);
				}
				if (i!=split.length-1) {
					newParams.append("&");
				}
			}
			url = address + newParams.toString();
		}
		
		req.setURL(url);
		
		super.onBeforeRequest(req);
	}

}

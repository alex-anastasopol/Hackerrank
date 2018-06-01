package ro.cst.tsearch.connection.http2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;

public class FLStLucieAO extends HttpSite {
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		if (req.getMethod()==HTTPRequest.POST) {
			StringBuilder sb = new StringBuilder("?");
			String url = req.getURL();
			if (url.toLowerCase().endsWith("/propertyquery.aspx")) {
				HashMap<String, ParametersVector> params = req.getPostParameters();
				try {
					for(Map.Entry<String, ParametersVector> entry: params.entrySet()) {
			    		sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue().toString(), "UTF-8")).append("&");
			    	}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				url += sb.toString();
				url = url.replaceFirst("&$", "");
				url = url.replaceFirst("\\?$", "");
				req.modifyURL(url);
			}
		}
	}
	
	public String getLegalDescription(String link) {
		HTTPRequest req = new HTTPRequest(link);
		return execute(req);
	}
	
}

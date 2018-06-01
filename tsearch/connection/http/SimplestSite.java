package ro.cst.tsearch.connection.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class SimplestSite extends HTTPSite {
	
	public void onBeforeRequest(HTTPRequest req){

		// check if it's actually a post 
		String url = req.getURL();		
		int idx1 = url.indexOf("?postParams=true&");
		if(idx1 == -1){ return; }
		
		// modify link
		req.modifyURL(url.substring(0, idx1));
		req.setMethod(HTTPRequest.POST);
				
		// extract params
		String params [] = url.substring(idx1 + "?postParams=true&".length()).split("&");
		for(String param: params){
			int idx2 = param.indexOf("=");
			try{
				String name = URLDecoder.decode(param.substring(0, idx2), "UTF-8");
				String value = URLDecoder.decode(param.substring(idx2+1), "UTF-8");
				req.setPostParameter(name, value);
			}catch(UnsupportedEncodingException e){
				throw new RuntimeException(e);
			}			
		}
		
	}
}

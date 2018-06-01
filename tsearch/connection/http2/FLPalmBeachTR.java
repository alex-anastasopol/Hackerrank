package ro.cst.tsearch.connection.http2;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ro.cst.tsearch.connection.http.HTTPRequest;
public class FLPalmBeachTR extends SimplestSite {

	public static String getSearchLink() {
		return "/DesktopModules/ListX/xListing.IM.aspx&";
	}
	
	public void onBeforeRequest(HTTPRequest req){

		// check if it's actually a post 
		String url = req.getURL();		
		Matcher mat = Pattern.compile("(?is)&owner2=[\\w+\\s]*").matcher(url);
		if (mat.find())
			url = url.replaceAll(mat.group(0), "");
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

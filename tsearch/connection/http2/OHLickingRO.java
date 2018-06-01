package ro.cst.tsearch.connection.http2;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class OHLickingRO extends HttpSite {
	
	@Override
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 5.1; rv:25.0) Gecko/20100101 Firefox/25.0";
	}
	
	@Override
	public String getAcceptLanguage() {
		return "en-US,en;q=0.5";
	}
	
	@Override
	public String getAccept() {
		return "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	}
	
	public byte[] getImage(String instrNo, String fileName) {
		String url = "http://" + getSiteLink() + "/LoadImage.asp?InstrID=" + instrNo;
		HTTPRequest req = new HTTPRequest(url, HTTPRequest.GET);
//		String resp = execute(req);
//		
//		return resp.getBytes();
		
		return process(req).getResponseAsByte();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		String url = req.getURL();	
//		url = url.replaceAll("&amp;(amp;)*","&");
		try {
			url = URLDecoder.decode(url, "UTF-8");
			req.setURL(url);
		} catch (UnsupportedEncodingException e) {							
			e.printStackTrace();
			logger.trace(e);
		}
		
		if (url.contains("LoadImage.asp")) {
			//view image
			//req.setReferer("http://www.lcounty.com/recordings/simplequery.asp?instrs=" + url.substring(url.indexOf("=")+1));
			req.setReferer("http://www.lcounty.com/recordings/SimpleQuery.asp");
			req.setHeader("User-Agent", getUserAgentValue());
			req.setHeader("Accept", getAccept());
			req.setHeader("Accept-Language", getAcceptLanguage());
			req.setHeader("Host", "www.lcounty.com");
		}
		
		if (req.getMethod() == HTTPRequest.POST) {
			if (req.getPostFirstParameter("fromDate") != null) {
				String fromDate = req.getPostFirstParameter("fromDate");
				if (!"".equals(fromDate)){
					String[] dateParts = fromDate.split("\\s+");
					if (dateParts.length == 3){
						req.setPostParameter("StartMonth", dateParts[0]);
						req.setPostParameter("StartDay", dateParts[1].replace(",", ""));
						req.setPostParameter("StartYear", dateParts[2]);
						req.removePostParameters("fromDate");
					}
				}
			}
			if (req.getPostFirstParameter("toDate") != null) {
				String toDate = req.getPostFirstParameter("toDate");
				if (!"".equals(toDate)){
					String[] dateParts = toDate.split("\\s+");
					if (dateParts.length == 3){
						req.setPostParameter("EndMonth", dateParts[0]);
						req.setPostParameter("EndDay", dateParts[1].replace(",", ""));
						req.setPostParameter("EndYear", dateParts[2]);
						req.removePostParameters("toDate");
					}
				}
			}
			
			if (req.getPostFirstParameter("navig") != null) {
				Map<String,String> navParams = (Map<String, String>)getTransientSearchAttribute("paramsNav:");
				req.removePostParameters("navig");
				if (navParams != null){
					req.addPostParameters(navParams);
				}
			} else { 
				if (req.getPostFirstParameter("DocTypeCats") != null){
					if ("".equals(req.getPostFirstParameter("DocTypeCats"))){
						req.removePostParameters("DocTypeCats");
					}
				}
				if (req.getPostFirstParameter("DocTypes") != null){
					if ("".equals(req.getPostFirstParameter("DocTypes"))){
						req.removePostParameters("DocTypes");
					}
				}
			}
//			if (req.getPostFirstParameter("DocTypes") != null) {
//				if (!"".equals(req.getPostFirstParameter("DocTypes")) && req.getPostFirstParameter("DocTypes").contains(",")) {
//					String[] doctypes = req.getPostFirstParameter("DocTypes").split("\\s*,\\s*");
//					req.removePostParameters("DocTypes");
//					for (String doctype : doctypes) {
//						req.setPostParameter("DocTypes", doctype);
//					}
//				}
//			}
		}
		
	}
}

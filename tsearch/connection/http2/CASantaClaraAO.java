package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class CASantaClaraAO extends HttpSite {

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		if (req.getMethod()==HTTPRequest.GET) {		//delete dashes from APN
			String url = req.getURL() + "&";
			Matcher matcher = Pattern.compile("(.+?)(apnValue=)([\\d-]+&)(.*)").matcher(url);
			if (matcher.find()) {
				url = matcher.group(1) + matcher.group(2) + matcher.group(3).replaceAll("-", "") +  matcher.group(4);
				url = url.replaceFirst("&$", "");
				req.modifyURL(url);
			}			
		}
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		
		if (res != null) {

			String respString = res.getResponseAsString();

			if (respString.indexOf("Situs Address(es) :") == -1) { // intermediary page

				String address = "https://www.sccassessor.org/apps/jqGridHandler.ashx?orderBy=0&direction=asc"; // get additional page of details
				HTTPRequest req1 = new HTTPRequest(address, HTTPRequest.GET);
				try {
					HTTPResponse resp = exec(req1);
					res.is = new ByteArrayInputStream(resp.getResponseAsByte());
					res.contentLenght = resp.contentLenght;
					res.contentType = resp.contentType;
					res.headers = resp.headers;
					res.returnCode = resp.returnCode;
					res.body = resp.body;
					res.setLastURI(resp.getLastURI());
				} catch (IOException e) {
					logger.error(e);
					throw new RuntimeException(e);
				}
			} else {
				res.is = new ByteArrayInputStream(respString.getBytes());
			}

		}
	}
	
}

package ro.cst.tsearch.connection.http2;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.Tidy;



/**
 * @author mihaib
*/

public class OHCuyahogaConnTR extends HttpSite{
	

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		if (req.getURL().contains("ShowTaxBill")){
			String firstResponse = execute(req);
			if (firstResponse.contains("Cuyahoga County Treasurer") && !firstResponse.contains("routine maintenance")){
				firstResponse = Tidy.tidyParse(firstResponse, null);
				Map<String, String> params = new SimpleHtmlParser(firstResponse).getForm("form1").getParams();
				if (params != null){
					HTTPRequest newReq = new HTTPRequest("http://treasurer.cuyahogacounty.us/payments/real_prop/taxbill.asp", HTTPRequest.POST);
					newReq.addPostParameters(params);
					HTTPResponse secondResponse = process(newReq);
					String response = secondResponse.getResponseAsString();
					if (response.contains("Parcel Number")){						
						secondResponse.contentType = "text/html; charset=utf-8";
						// bypass response
						secondResponse.is = IOUtils.toInputStream(response);
						req.setBypassResponse(secondResponse);
					}
				}
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
		
		return execute(req);
	}
	
	
}
package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;


public class MDHarfordTR extends HttpSite {
	private String county = "";
	
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 5.1; rv:18.0) Gecko/20100101 Firefox/18.0";
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
        String currentRequest = req.getURL();  
        String url = "";

        if ((req.getPostFirstParameter("Option") == null)  || 
           (currentRequest.contains("TaxBill.aspx") || currentRequest.contains("TaxBill.aspx") || currentRequest.contains("AssessmentsTaxDetails.aspx") || currentRequest.contains("WaterSewer.aspx"))) {
        		req.setMethod(HTTPRequest.GET);
        		url = currentRequest;
        		if (req.getURL().contains("TaxBill.aspx") || req.getURL().contains("TaxBillDetails.aspx") 
        				|| req.getURL().contains("WaterSewer.aspx") || req.getURL().contains("AssessmentsTaxDetails.aspx")) {
        			//Host: http://bills.harfordcountymd.gov
        			url = currentRequest.replaceFirst("www", "bills");
        		}
       	} else {
        		 url = currentRequest;
            	 req.setMethod(HTTPRequest.POST);
       	}
        
        req.setHeader("User-Agent", getUserAgentValue());
        req.setURL(url);
	}

	
	public LoginResponse onLogin() {
		county = getDataSite().getCountyName();
		String resp = "";
		String link = getSiteLink();
		//site link: http://www.harfordcountymd.gov/Treasury/Bills/index.cfm
		HTTPRequest req = new HTTPRequest(link, HTTPRequest.GET);
		req.setHeader("User-Agent", getUserAgentValue());
		resp = execute(req);
		req = new HTTPRequest(link, HTTPRequest.POST);
		req.setHeader("User-Agent", getUserAgentValue());
		req.setPostParameter("AgreeForm", "1");
		req.setPostParameter("AgreeAction", "I Agree");
		resp = execute(req);
		
		//search page: /Treasury/Bills/Search.cfm [GET]
		link = link.replaceFirst("(?is)([\\w/\\.:]+/).*", "$1");
		link += "Search.cfm";
		req = new HTTPRequest(link, HTTPRequest.GET);
		req.setHeader("User-Agent", getUserAgentValue());
		resp = execute(req);
				
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
}

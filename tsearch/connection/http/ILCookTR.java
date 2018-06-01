package ro.cst.tsearch.connection.http;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.utils.InstanceManager;

public class ILCookTR extends HTTPSite {
	
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		Search crtSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		if (crtSearch.getSearchType() == Search.AUTOMATIC_SEARCH &&
			req.getPostParameter("ctl00$ContentPlaceHolder1$PaymentSearchBox1$GeneratedSecurityCode") != null)
		{
			HTTPRequest reqCode = new HTTPRequest("http://www.cookcountytreasurer.com/payment.aspx?ntopicid=3");
			reqCode.setMethod(HTTPRequest.GET);
			reqCode.setHeader("Host", "www.cookcountytreasurer.com");
			
			HTTPResponse resCode = process(reqCode);
			String resCodeStr = resCode.getResponseAsString();
			if (resCodeStr.matches("(?is).*<\\s*input[^>]+name[^>]+GeneratedSecurityCode[^>]+id[^>]+GeneratedSecurityCode[^>]+value=[\\\"]([^\\\"]+)[\\\"][^>]+>.*"))
			{
				String secCodeStr = resCodeStr.replaceFirst("(?is).*<\\s*input[^>]+name[^>]+GeneratedSecurityCode[^>]+id[^>]+GeneratedSecurityCode[^>]+value=[\\\"]([^\\\"]+)[\\\"][^>]+>.*", "$1");
				req.removePostParameters("ctl00$ContentPlaceHolder1$PaymentSearchBox1$GeneratedSecurityCode");
				req.removePostParameters("ctl00$ContentPlaceHolder1$PaymentSearchBox1$EnteredSecurityCode");
				req.setPostParameter( "ctl00$ContentPlaceHolder1$PaymentSearchBox1$GeneratedSecurityCode" , secCodeStr);
				req.setPostParameter( "ctl00$ContentPlaceHolder1$PaymentSearchBox1$EnteredSecurityCode" , secCodeStr);
			}
		}
		
		if(req.hasPostParameter("ctl00$ContentPlaceHolder1$PaymentSearchBox1$PIN1")){
			req.modifyURL("http://www.cookcountytreasurer.com/pinsummary.aspx");
			req.removePostParameters("ntopicid");
		} 
		
		return;
	}

}

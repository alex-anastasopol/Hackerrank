package ro.cst.tsearch.connection.http2;

import org.apache.commons.httpclient.protocol.Protocol;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.utils.InstanceManager;

public class TNGenericCountyTR extends HttpSite{
	
	private String TNcountyNameTR = "TNFranklinTR";
    
	protected void setCountyName (String name)
	{
		TNcountyNameTR = name;
	}
	
	protected String getSpecificCntySrvName()
	{
		if (TNcountyNameTR.equalsIgnoreCase("TNTrousdaleTR"))
			return "TNWilsonTR";
		else if (TNcountyNameTR.equalsIgnoreCase("TNMooreTR"))
			return "TNCoffeeTR";
		else if (TNcountyNameTR.equalsIgnoreCase("TNHamblenTR"))
			return "TNGreeneTR";

		return TNcountyNameTR;
	}

	public LoginResponse onLogin(){        
     
		String cntyName = InstanceManager.getManager().getCurrentInstance(searchId)
			.getCrtSearchContext().getSa().getCountyName().replaceAll("[\\s\\p{Punct}]+", "");
		String link = getCrtServerLink() + ".org";
		HTTPRequest req = new HTTPRequest(link);
		req.setMethod(HTTPRequest.GET);
		req.setHeader("Host", cntyName + ".tennesseetrustee.org");
		
		Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
		
		HTTPResponse res = process( req );
		res.getResponseAsString();
	    
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".org");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link.substring(0, idx);
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {

        String url = req.getURL();

        req.removePostParameters("search[86:&gt;:0]");
        req.setPostParameter("search[86:>:0]", "use");
        url = url.replaceFirst("(?is)[/][/]search.php", "/search.php");
       
        String cntyName = InstanceManager.getManager().getCurrentInstance(searchId)
			.getCrtSearchContext().getSa().getCountyName().replaceAll("[\\s\\p{Punct}]+", "");
        
        req.setHeader("Host", cntyName + ".tennesseetrustee.org");
	}

	public String getPage(String link) {
		HTTPRequest req = new HTTPRequest(link);
		req.setMethod(HTTPRequest.GET);
		
		return execute(req);
	}
	
	public void onRedirect(HTTPRequest req) {
		String location = req.getRedirectLocation();
		if (location.contains("maintenance.php")) {
			destroySession();
			throw new RuntimeException("Redirected to " + location
					+ ". Session needs to be destroyed");
		}
	}
	
}

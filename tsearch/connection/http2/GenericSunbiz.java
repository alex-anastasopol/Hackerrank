package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;

public class GenericSunbiz extends SimplestSite{

	@Override
	public LoginResponse onLogin() {
		try {
			HTTPRequest req = new HTTPRequest("http://search.sunbiz.org/Inquiry/CorporationSearch/ByName");

			String resp = execute(req);

			if (resp.contains("Search By Entity Name"))
				return LoginResponse.getDefaultSuccessResponse();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return LoginResponse.getDefaultFailureResponse();
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
//		Set<Entry<String, ParametersVector>> postParameters = req
//				.getPostParameters().entrySet();
//		
//		for (Entry<String, ParametersVector> entry : postParameters) {
//			try {
//				entry.getValue().set(0,
//						URLDecoder.decode(entry.getValue().getFirstParameter(),"UTF-8"));
//			} catch (UnsupportedEncodingException e) {
//				e.printStackTrace();
//			}
//		}
//		String url = req.getURL();
//		url = url.replace("%20", "+");
//		
//		req.setURL(url);
		
		super.onBeforeRequest(req);
	}
}

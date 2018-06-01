package ro.cst.tsearch.connection.http2;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.http.HTTPRequest;

public abstract class FLGenericQPublicAO extends SimplestSite {

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		super.onBeforeRequest(req);
		String countyParameterName = "county";
		String countyFirstParameter = req.getPostFirstParameter(countyParameterName);

		if (StringUtils.isEmpty(countyFirstParameter)) {
			String url = req.getURL();
			String regExCounty = "county=(\\w*)?(&)?";
			url = url.replaceAll(regExCounty, "");
			url += "&" + countyParameterName + "=" + getCounty();
			req.setURL(url);
		} else {
			req.removePostParameters(countyParameterName);
			req.setPostParameter(countyParameterName, getCounty());
		}
	}

	public abstract String getCounty();

	public abstract String getLegalLink();

	public String getCompleteLegalPage(String accountId) {
		String result = "";
		try {
			String link = "http://www.qpublic.net" + getLegalLink() + accountId;
			HTTPRequest req = new HTTPRequest(link);
			result = execute(req);
		}catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}
}

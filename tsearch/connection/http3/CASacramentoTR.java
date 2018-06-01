package ro.cst.tsearch.connection.http3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.utils.StringUtils;

public class CASacramentoTR extends HttpSite3 {
	private String	fullPIDParamName	= "fullParcelNumber";

	@Override
	public LoginResponse onLogin() {
		try {
			HTTPRequest req = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
			String responseAsString = execute(req);
			if (!StringUtils.extractParameter(responseAsString,
					"(?is)(Please\\s+be\\s+sure\\s+you\\s+enter\\s+the\\s+correct\\s+14\\s+digit\\s+parcel\\s+number)").isEmpty()) {
				return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
			}

			return LoginResponse.getDefaultFailureResponse();
		} catch (Exception e) {
			e.printStackTrace();
			return LoginResponse.getDefaultFailureResponse();
		}
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.POST) {// if doing a search
			ParametersVector fullParcelNumberPV = req.getPostParameter(fullPIDParamName);
			if (fullParcelNumberPV != null) {
				String fullParcelNumber = fullParcelNumberPV.toString().replaceAll("[^\\d]", "");

				req.removePostParameters(fullPIDParamName);

				Pattern parcelPattern = Pattern.compile("(\\d{3})(\\d{4})(\\d{3})(\\d{4})");
				Matcher parcelMatcher = parcelPattern.matcher(fullParcelNumber);
				if (parcelMatcher.find()) {
					req.setPostParameter("Parcel_Number1", parcelMatcher.group(1));
					req.setPostParameter("Parcel_Number2", parcelMatcher.group(2));
					req.setPostParameter("Parcel_Number3", parcelMatcher.group(3));
					req.setPostParameter("Parcel_Number4", parcelMatcher.group(4));
				}
			}
		}
	}
}

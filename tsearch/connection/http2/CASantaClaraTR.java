package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 26, 2012
 */

public class CASantaClaraTR extends HttpSite {

	public static String	SERVER_LINK	= "http://payments.scctax.org";

	@Override
	public LoginResponse onLogin() {
		try {
			HTTPRequest req = new HTTPRequest(SERVER_LINK + "/payment/jsp/startup.jsp");

			String resp = execute(req);

			if (resp.contains("On-line Bill Presentment and Payment Site") && !resp.contains("Session has timed out"))
				return LoginResponse.getDefaultSuccessResponse();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return LoginResponse.getDefaultFailureResponse();
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.GET) {
			if (!req.getURL().contains("/payment/jsp/currentSecured.jsp"))
				if (req.getURL().contains("apn=&") || (req.getURL() + "&").contains("apn=&")) {
					HTTPRequest reqCurrentSecured = new HTTPRequest(SERVER_LINK + "/payment/jsp/currentSecured.jsp", HTTPRequest.POST);
					process(reqCurrentSecured);
				}
		}
	}

	private static int	tries	= 0;

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res != null) {
			String resString = res.getResponseAsString();

			if (res.returnCode == 500) {
				res.is = IOUtils.toInputStream("<html>error</html>");
				res.body = "<html>error</html>";
				res.contentLenght = res.body.length();
				res.returnCode = 200;
				return;
			}

			if (resString.contains("Session has timed out")) {
				if (tries < 3) {
					onLogin();
					tries++;

					HTTPResponse resp = process(req);
					res.body = resp.body;
					res.contentLenght = resp.contentLenght;
					res.returnCode = resp.returnCode;
					res.is = new ByteArrayInputStream(resp.getResponseAsByte());
				} else {
					res.is = IOUtils.toInputStream("<html>error</html>");
					res.body = "<html>error</html>";
					res.contentLenght = res.body.length();
					res.returnCode = 200;
					return;
				}
			} else {
				res.body = resString;
				res.is = new ByteArrayInputStream(resString.getBytes());
			}
		}
	}
}

package ro.cst.tsearch.connection.http2;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

/**
 * 
 * @author Oprina George
 * 
 *         Oct 5, 2012
 */
public class FLHillsboroughAO extends HttpSite {

	public LoginResponse onLogin() {
		try {
			HTTPRequest req = new HTTPRequest(getDataSite().getServerHomeLink() + getDataSite().getServerDestinationPage());

			String resp = execute(req);

			if (resp.contains("OWNER NAME SEARCH"))
				return LoginResponse.getDefaultSuccessResponse();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return LoginResponse.getDefaultFailureResponse();
	}

	public void onBeforeRequestExcl(HTTPRequest req) {

		if (req.getMethod() == HTTPRequest.POST) {
			// fake the pins
			if (req.hasPostParameter("pin_fake")) {
				String pin = req.getPostFirstParameter("pin_fake");

				try {
					pin = pin.replaceAll("[\\s,.-]", "");
					if (pin.replaceAll("[\\s,.-]", "").length() == 22) {
						String mun = pin.substring(0, 1);
						String sec = pin.substring(1, 3);
						String twp = pin.substring(3, 5);
						String rng = pin.substring(5, 7);
						String sub = pin.substring(7, 10);
						String blk = pin.substring(10, 16);
						String lot = pin.substring(16);

						req.removePostParameters("pin_fake");
						req.setPostParameter("city", mun);
						req.setPostParameter("section", sec);
						req.setPostParameter("township", twp);
						req.setPostParameter("range", rng);
						req.setPostParameter("area", sub);
						req.setPostParameter("block", blk);
						req.setPostParameter("lot", lot);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else if (req.hasPostParameter("FolioId1")) {
				String folio = req.getPostFirstParameter("FolioId1").trim();

				try {
					if (folio.contains("-")) {
						String f1 = folio.split("-")[0].trim();
						String f2 = folio.split("-")[1].trim();

						req.removePostParameters("FolioId1");
						req.setPostParameter("FolioId1", f1);
						req.setPostParameter("FolioId2", f2);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void onBeforeRequest(HTTPRequest req) {
		super.onBeforeRequest(req);
	}

	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
	}

}

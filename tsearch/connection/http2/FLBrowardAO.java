package ro.cst.tsearch.connection.http2;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class FLBrowardAO extends HttpSite {

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		if (req.getMethod()==HTTPRequest.POST) {
			
			String name = req.getPostFirstParameter("Name");
			if (name!=null) {
				if (name.length()<3) {
					req.setBypassResponse(getErrorResponse("Name must have at least 3 characters."));
					return;
				}
			}
			
			//parameter "Folio" obtained from parameter "FOLIO_NUMBER" like on the official site
			String folio = req.getPostFirstParameter("FOLIO_NUMBER");
			if (folio!=null) {
				
				StringBuilder workFolio = new StringBuilder();
				String buildFolio = "";
				
				folio = folio.replaceAll("\\s", "");
				for (int i=0;i<folio.length();i++) {
					String wkSh = folio.substring(i, i+1);
					if (wkSh.equals(" ") || wkSh.equals("-")) {
						wkSh = "";
					} else {
						workFolio.append(wkSh);
					}
				}
				folio = workFolio.toString();
				
				if (workFolio.length()==9) {
					workFolio.append("0");
				}
				
				if (workFolio.length()==10) {
					if (workFolio.substring(0, 1).equals("7") ||
						workFolio.substring(0, 1).equals("8") ||
						workFolio.substring(0, 1).equals("9")) {
							buildFolio = "4";
					}
					if (workFolio.substring(0, 1).equals("0") ||
						workFolio.substring(0, 1).equals("1")) {
							buildFolio = "5";
					}
					buildFolio += workFolio.substring(0, 1);
					if (workFolio.substring(1, 2).equals("8") ||
						workFolio.substring(1, 2).equals("9")) {
							buildFolio += "3";
					} else {
						if (workFolio.substring(1, 2).equals("0") ||
							workFolio.substring(1, 2).equals("1") ||
							workFolio.substring(1, 2).equals("2") ||
							workFolio.substring(1, 2).equals("3")) {
								buildFolio += "4";
							}
					}
					buildFolio += workFolio.substring(1,10);
					folio = buildFolio;
				}
				
				workFolio = new StringBuilder(folio);
				
				if (workFolio.substring(0, 1) == "1") {
					workFolio = new StringBuilder(workFolio.substring(1, 11));
					if ((workFolio.substring(0, 1) == "7") ||
						(workFolio.substring(0, 1) == "8") ||
						(workFolio.substring(0, 1) == "9")) {
							buildFolio = "4";
			        }
					if ((workFolio.substring(0, 1) == "0") ||
						(workFolio.substring(0, 1) == "1")) {
							buildFolio = "5";
			        }
					buildFolio += workFolio.substring(0, 1);
					if ((workFolio.substring(1, 2) == "8") ||
						(workFolio.substring(1, 2) == "9")) {
							buildFolio += "3";
			        }
					if ((workFolio.substring(1, 2) == "0") ||
						(workFolio.substring(1, 2) == "1") ||
						(workFolio.substring(1, 2) == "2") ||
						(workFolio.substring(1, 2) == "3")) {
							buildFolio += "4";
			        }
					buildFolio += workFolio.substring(1, 12);
					folio = buildFolio.toString();
				}

				if (folio.length() == 11) {
			       folio += "0";
			    }

			    if (folio.length() == 12) {
			    	req.setPostParameter("Folio", folio);
			    } else {
			    	req.setBypassResponse(getErrorResponse("Incorrect Folio length."));
					return;
			    }
			}
		}
	}
	
	private HTTPResponse getErrorResponse(String error) {
		HTTPResponse resp = new HTTPResponse();

		resp.body = "<html><head></head><body><div>" + error + "<div></body></html>";
		resp.contentLenght = resp.body.length();
		resp.contentType = "text/html;";
		resp.is = IOUtils.toInputStream(resp.body);
		resp.returnCode = 200;

		return resp;
	}
	
}

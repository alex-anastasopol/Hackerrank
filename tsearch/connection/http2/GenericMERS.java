package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.POST;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.StringUtils;

public class GenericMERS extends HttpSite {

	public final String[] EMPTY_PARAMS = {"error_message", "kCode", "min", "number_po", "street_po", 
			"unit_po", "city_po", "zip_po", "firstname_in", "lastname_in", 
			"number_in", "street_in", "unit_in", "city_in", "zip_in", 
			"corpname_cn", "number_cn", "street_cn", "unit_cn", "city_cn", 
			"zip_cn", "firstname_iz", "lastname_iz", "ssn1", "ssn2", 			//parameters with value ""
			"ssn3", "zip_iz", "corpname_cz", "tin", "zip_cz", "certificate"};	//parameters with value "00"
	public final String[] ZERO_PARAMS = {"state_po", "state_in", "state_cn"};
	public final String[] ZIP_SUFFIXES = {"po", "in", "cn", "iz", "cz"};
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		if (req.getMethod()==HTTPRequest.POST) {
			
			String min = req.getPostFirstParameter("min");
			if (min!=null) {
				if (!min.matches("\\d{18}") && !min.matches("\\d{7}-\\d{10}-\\d")) {
					req.setBypassResponse(getErrorResponse("Wrong MIN format!"));
				}
			}
			
			for (String suffix: ZIP_SUFFIXES) {
				String zip = req.getPostFirstParameter("zip_" + suffix);
				if (zip!=null) {
					if (!zip.matches("\\d{5}")) {
						req.setBypassResponse(getErrorResponse("Wrong Zip Code format!"));
					}
				}
			}
			
			String ssn = req.getPostFirstParameter("ssn");
			if (ssn!=null) {
				Matcher matcher = Pattern.compile("(\\d{3})-(\\d{2})-(\\d{4})").matcher(ssn);
				if (matcher.matches()) {
					String ssn1 = matcher.group(1);
					String ssn2 = matcher.group(2);
					String ssn3 = matcher.group(3);
					req.removePostParameters("ssn");
					req.setPostParameter("ssn1", ssn1);
					req.setPostParameter("ssn2", ssn2);
					req.setPostParameter("ssn3", ssn3);
				} else {
					req.setBypassResponse(getErrorResponse("Wrong SSN format!"));
				}
			}
			
			String tin = req.getPostFirstParameter("tin");
			if (tin!=null) {
				if (!tin.matches("\\d{9}")) {
					req.setBypassResponse(getErrorResponse("Wrong Taxpayer Identification Number format!"));
				}
			}
			
			String captcha = req.getPostFirstParameter("kaptchafield");
			
			if (StringUtils.isEmpty(captcha)) {
				req.setBypassResponse(getErrorResponse("No captcha entered!"));
				return;
			}
			
			//enter captcha
			HTTPRequest reqLogin = new HTTPRequest(getSiteLink() + "index.jsp", POST);
			reqLogin.setPostParameter("refreshed", "N");
			reqLogin.setPostParameter("formsubmitted", "N");
			reqLogin.setPostParameter("kaptchafield", captcha);
			reqLogin.setPostParameter("submit", "Logon");
			
			String respLoginString = "";
			try {
				respLoginString = exec(reqLogin).getResponseAsString();
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
			
			if (!respLoginString.contains("Search for servicer information")) {
				req.setBypassResponse(getErrorResponse("Login Error!"));
				return;
			}
			
			req.removePostParameters("image");
			req.removePostParameters("kaptchafield");
			for (String s: EMPTY_PARAMS) {
				String value = req.getPostFirstParameter(s);
				if (value==null) {
					req.setPostParameter(s, "");
				}
			}
			for (String s: ZERO_PARAMS) {
				String value = req.getPostFirstParameter(s);
				if (value==null) {
					req.setPostParameter(s, "00");
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

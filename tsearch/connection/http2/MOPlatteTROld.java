package ro.cst.tsearch.connection.http2;

import java.io.InputStream;
import java.io.StringBufferInputStream;

import org.apache.commons.io.input.AutoCloseInputStream;

import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class MOPlatteTROld extends SimplestSite {
	public static final String THE_SITE_DOESN_T_CONTAIN_THE_REQUIRED_PDF_FILE = "There is no image associated with this tax document on Missouri Platte Treasurer Office site!";
	public static String PDF_URL = "http://plattecountycollector.com/onlinerec/platter_rec_state.php?fullparcel=";

	public byte[] getPDFDocument(String accountId) {
		String link = PDF_URL + accountId;
		HTTPRequest req = new HTTPRequest(link);
		HTTPResponse response = process(req);
		return response.getResponseAsByte();
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res.getReturnCode() == 200 && res.getContentLenght() == 0 && req.getURL().replace("www.", "").contains(PDF_URL) ){
			res.body = THE_SITE_DOESN_T_CONTAIN_THE_REQUIRED_PDF_FILE;
			InputStream in = new StringBufferInputStream(THE_SITE_DOESN_T_CONTAIN_THE_REQUIRED_PDF_FILE);
			res.is = new AutoCloseInputStream(in);
		}
	}

}

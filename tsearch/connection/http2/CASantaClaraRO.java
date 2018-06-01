package ro.cst.tsearch.connection.http2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class CASantaClaraRO extends HttpSite {
	
	private static String[] getDate(final String input) {
		if (input == null)
			return null;
		// Parse date (eg. 07/27/1982)
		Matcher m = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{4})").matcher(input);
		if (m.find())
			return new String[] {m.group(1), m.group(2), m.group(3)};
	    return null;
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		// Parse start date and end date
		String[] from = getDate(req.getPostFirstParameter("start_date"));
		if (from != null) {
			req.removePostParameters("start_date");
			req.setPostParameter("F_Month", from[0]);
			req.setPostParameter("F_Day", from[1]);
			req.setPostParameter("F_Year", from[2]);
		}
		String[] to = getDate(req.getPostFirstParameter("end_date"));
		if (to != null) {
			req.removePostParameters("end_date");
			req.setPostParameter("T_Month", to[0]);
			req.setPostParameter("T_Day", to[1]);
			req.setPostParameter("T_Year", to[2]);
		}
		req.setHeader("cookie", "ScreenHeight=1026; ScreenWidth=1696; Session=YXXXXXXXXXXXXYNNNNNN; DISPLAYWITHOUTCLICK=; PUBLICVIEWER=; PUBLICAPI=M");
		super.onBeforeRequest(req);
	}
}

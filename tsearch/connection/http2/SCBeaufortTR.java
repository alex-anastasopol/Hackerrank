package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;

public class SCBeaufortTR extends HttpSite {
	
	private String sid = "";
	
	@Override
	public LoginResponse onLogin() {

		String link1 = getSiteLink();
		
		HTTPRequest req1 = new HTTPRequest(link1, GET);
		String responseAsString1  = execute(req1);
		
		if(responseAsString1 != null)
		{
			org.htmlparser.Parser htmlParser1 = org.htmlparser.Parser.createParser(responseAsString1, null);
			try {
				NodeList nodeList1 = htmlParser1.parse(null);
				NodeList linkList1 = nodeList1.extractAllNodesThatMatch(new TagNameFilter("FRAME"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("NAME", "body"));
				if (linkList1.size()>0)
				{
					String link2 = linkList1.elementAt(0).toHtml();
					Matcher matcher1 = Pattern.compile("(?i)SRC=\"(.*?)\"").matcher(link2);
					if (matcher1.find())
						link2 = matcher1.group(1);
					link2 = link1 + link2;
					
					//get sid parameter
					Matcher matchersid = Pattern.compile("(?i)sid=(.*?)(&|\")").matcher(link2);
					if (matchersid.find())
						sid = matchersid.group(1);
										
					HTTPRequest req2 = new HTTPRequest(link2, GET);
					String responseAsString2 = execute(req2);
					if(responseAsString2 != null)
					{
						org.htmlparser.Parser htmlParser2 = org.htmlparser.Parser.createParser(responseAsString2, null);
						NodeList nodeList2 = htmlParser2.parse(null);
						NodeList linkList2 = nodeList2.extractAllNodesThatMatch(new TagNameFilter("a"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("class", "listlink"));
						if (linkList2.size()>=2)
						{
							String link3 = linkList2.elementAt(1).toHtml();
							Matcher matcher2 = Pattern.compile("(?i)href=\"(.*?)\"").matcher(link3);
							if (matcher2.find())
								link3 = matcher2.group(1);
							link3 = link1 + link3.substring(6);
							
							HTTPRequest req3 = new HTTPRequest(link3, GET);
							execute(req3);
						}
					}
				}
				
			} catch (ParserException e) {
				logger.error("Problem Parsing Form on SCBeaufortTR", e);
				new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Problem Parsing Form on SCBeaufortTR");
			}
		}
			
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {

		String url = req.getURL();
		url = url.replaceAll("\\|", "%7C");
		req.setURL(url);
		
		if (req.getMethod() == HTTPRequest.POST)
		{
			req.setPostParameter("sid", sid);
			
			//replace parameters
			String saledate = req.getPostFirstParameter("s.saledate2");
			req.removePostParameters("s.saledate2");
			req.setPostParameter("s.saledate", saledate);
			
			String totsale = req.getPostFirstParameter("s.totsale2");
			req.removePostParameters("s.totsale2");
			req.setPostParameter("s.totsale", totsale);
			
			String yearconst = req.getPostFirstParameter("i.yearconst2");
			req.removePostParameters("i.yearconst2");
			req.setPostParameter("i.yearconst", yearconst);
						
			String totbed = req.getPostFirstParameter("d.totbed2");
			req.removePostParameters("d.totbed2");
			req.setPostParameter("d.totbed", totbed);
						
			String totfbath = req.getPostFirstParameter("d.totfbath2");
			req.removePostParameters("d.totfbath2");
			req.setPostParameter("d.totfbath", totfbath);
						
			String tothbath = req.getPostFirstParameter("d.tothbath2");
			req.removePostParameters("d.tothbath2");
			req.setPostParameter("d.tothbath", tothbath);
						
			String floorarea = req.getPostFirstParameter("d.floorarea2");
			req.removePostParameters("d.floorarea2");
			req.setPostParameter("d.floorarea", floorarea);
						
			String storyht = req.getPostFirstParameter("d.storyht2");
			req.removePostParameters("d.storyht2");
			req.setPostParameter("d.storyht", storyht);
						
		}
		
		if (req.getURL().contains("grmtax"))			//taxes page from details
			req.setURL(req.getURL() + "&wait=done");
	}
	
}

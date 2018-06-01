package ro.cst.tsearch.connection.http2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.parser.HtmlParser;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.utils.PDFUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;

public class OHFranklinCO extends HttpSite{
	
	private static List<String> recoveryPostProcessingReports = new ArrayList<String>();
	
	@Override
	public LoginResponse onLogin() {
		
		if (recoveryPostProcessingReports.size()==0) {
			//get Attorney General Recovery Post Processing Reports
			String baseLink = "http://www.franklincountyohio.gov/clerk/";
			String resp0 = "";
			HTTPRequest req0 = new HTTPRequest(baseLink + "cio.cfm", HTTPRequest.GET);
			try {
				resp0 = exec(req0).getResponseAsString();
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
			if (!StringUtils.isEmpty(resp0)) {
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp0, null);
					NodeList nodeList = htmlParser.parse(null);
					NodeList list = HtmlParser3.getNodeListByType(nodeList, "p", true);
					String par = "";
					for (int i=0;i<list.size();i++) {
						if (list.elementAt(i).toHtml().contains("Attorney General Recovery Post Processing Reports")) {
							par = list.elementAt(i).toHtml();
							break;
						}
					}
					if (!StringUtils.isEmpty(par)) {
						Matcher ma1 = Pattern.compile("(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>").matcher(par);
						while (ma1.find()) {
							String link1 = ma1.group(1);
							String resp1 = "";
							HTTPRequest req1 = new HTTPRequest(baseLink + link1, HTTPRequest.GET);
							try {
								resp1 = exec(req1).getResponseAsString();
							} catch(IOException e){
								logger.error(e);
								throw new RuntimeException(e);
							}
							if (!StringUtils.isEmpty(resp1)) {
								Matcher ma2 = Pattern.compile("(?is)<link[^>]+id=\"shLink\"[^>]+href=\"([^\"]+)\"[^>]*>").matcher(resp1);
								if (ma2.find()) {
									String link2 = ma2.group(1);
									String resp2 = "";
									HTTPRequest req2 = new HTTPRequest(baseLink + "AG Files/" + link2, HTTPRequest.GET);
									try {
										resp2 = exec(req2).getResponseAsString();
									} catch(IOException e){
										logger.error(e);
										throw new RuntimeException(e);
									}
									if (!StringUtils.isEmpty(resp2)) {
										recoveryPostProcessingReports.add(resp2);
									}
								}
							}
						}
					}
				} catch (ParserException e) {
					e.printStackTrace();
				}
			}
		}
		
		HTTPRequest req = new HTTPRequest( "http://fcdcfcjs.co.franklin.oh.us/CaseInformationOnline/" );
	    req.setMethod( HTTPRequest.GET );
	    req.setHeader("Host", "fcdcfcjs.co.franklin.oh.us");
	    req.noRedirects = false;        
	    HTTPResponse res = process( req );
	    	    
		if(res .getReturnCode() == 200) 
		{
			String responseString = res.getResponseAsString();
		    HtmlParser htmlParams = new HtmlParser(responseString);
		    
			req = new HTTPRequest( "http://fcdcfcjs.co.franklin.oh.us/CaseInformationOnline/acceptDisclaimer");
		    req.setMethod( HTTPRequest.POST );
		    req.setHeader("Host", "fcdcfcjs.co.franklin.oh.us");
		    req.setHeader("Referer", "http://fcdcfcjs.co.franklin.oh.us/CaseInformationOnline/");
		    try {
			    req.setPostParameter("Accept", htmlParams.getElementByName("Accept").getValue());
			    req.setPostParameter("fromPage", htmlParams.getElementByName("FromPage").getValue());
		    } catch (Exception e) {
				e.printStackTrace();
			}
		    req.noRedirects = false;
		    
		    res = process( req );
		    res.getResponseAsString();
			
		    if(res.getReturnCode() == 200) {
		    	return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		    }
		}
		
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) 
	{
		// don't do anything during logging in
		if(status != STATUS_LOGGED_IN){
			return;
		}
		// don't do anything while we're already inside a onBeforeRequest call
		if(getAttribute("onBeforeRequest") == Boolean.TRUE){
			return;
		}
		// mark that we're treating onBeforeRequest
		setAttribute("onBeforeRequest", Boolean.TRUE);
		
	    req.setHeader("Host", "fcdcfcjs.co.franklin.oh.us");
	    
	    String url =req.getURL();
	    
	    if(url.contains("nameSearch"))
	    	req.setHeader("Referer", "http://fcdcfcjs.co.franklin.oh.us/CaseInformationOnline/");
	    else if (url.contains("caseSearch"))
	    	req.setHeader("Referer", "http://fcdcfcjs.co.franklin.oh.us/CaseInformationOnline/nameSearch");
		
		setAttribute("onBeforeRequest", Boolean.FALSE);
	}
	
//	/**
//	 * makes the last search for intermediary page one more time,
//	 * in order to refresh the connection
//	 * @param 
//	 */
//	private void lastSearchForIntermPage()
//	{
//		Search crtIntermSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
//		HTTPRequest req = (HTTPRequest) crtIntermSearch.getAdditionalInfo((new Long(searchId)).toString());
//		HTTPResponse res = null;
//		
//		if (req != null)
//		{
//        	process (req);//res = // the 'process' method inherited from http2.HttpSite 
////            String htmlRes = res.getResponseAsString();
//		}
//	}
//	
//	/**
//	 * log a message, together with instance id and session id
//	 * @param message
//	 */
//	private void info(String message) {
//		logger.info("search=" + searchId + " :" + message);
//	}

	@Override
	public void onRedirect(HTTPRequest req){
		String location = req.getRedirectLocation();
		if(location.contains("?e=newSession") || location.contains("?e=sessionTerminated")){
			setDestroy(true);
			throw new RuntimeException("Redirected to " + location + ". Session needs to be destroyed");
		}
	}
	
	public List<String> getRecoveryPostProcessingReports() {
		if (this.status != STATUS_LOGGED_IN) {
			onLogin();
		}
		return recoveryPostProcessingReports;
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		
		if (res!=null && res.returnCode>=500) {
			res.is = IOUtils.toInputStream("<html>Server error</html>");
			res.body = "<html>Server error</html>";
			res.contentLenght = res.body.length();
			res.returnCode = 200;
			return;
		}
	}
	
	public byte[] getImage(String link) {
		String response = getImageResponse(link);
		return getImageFromResponse(response);
	}
	
	private String getImageResponse(String link) {
		
		String response = "";
		
		link = link.replaceAll(".*?Link=", "");
		String caseno = StringUtils.extractParameter(link, "caseno=([^&?]*)");
		String[] parts = caseno.split("-");
		if (parts.length==3) {
			HTTPRequest req = new HTTPRequest(getDataSite().getLink() + "/caseSearch", HTTPRequest.POST);
			req.setPostParameter("caseYear", parts[0]);
			req.setPostParameter("caseType", parts[1]);
			req.setPostParameter("caseSeq", parts[2]);
			req.setPostParameter("lname", "");
			req.setPostParameter("fname", "");
			req.setPostParameter("mint", "");
			req.setPostParameter("selType", "All");
			req.setPostParameter("txtCalendar2", "");
			req.setPostParameter("txtCalendar1", "");
			req.setPostParameter("recs", "25");
			response = process(req).getResponseAsString();		//details
		}
		
		return response;
	}
	
	private byte[] getImageFromResponse(String response) {
		
		if ("".equals(response)) {
			return null;
		}
		
		List<String> coords = new ArrayList<String>();
		
		Matcher maLinks = Pattern.compile(ro.cst.tsearch.servers.types.OHFranklinCO.IMAGE_LINK_PATTERN).matcher(response);
		while (maLinks.find()) {
			String description = maLinks.group(2);
			if (description.matches(ro.cst.tsearch.servers.types.OHFranklinCO.PRAECIPE_PATTERN)) {
				String coord = "";
				Matcher maCoords = Pattern.compile(ro.cst.tsearch.servers.types.OHFranklinCO.IMAGE_PART1_PATTERN + maLinks.group(3) + 
						ro.cst.tsearch.servers.types.OHFranklinCO.IMAGE_PART2_PATTERN).matcher(response);
				if (maCoords.find()) {
					coord = maCoords.group(1);
					try {
						coord = URLEncoder.encode(coord, "UTF-8");
					} catch (UnsupportedEncodingException uee) {}
				}
				if (!"".equals(coord)) {
					coords.add(coord);
				}
			}
		}
		
		List<byte[]> pages = new ArrayList<byte[]>();
		for (String s: coords) {
			HTTPRequest req2 = new HTTPRequest(getDataSite().getLink() + ro.cst.tsearch.servers.types.OHFranklinCO.IMAGE_LINK + s, HTTPRequest.GET);
			pages.add(process(req2).getResponseAsByte());
		}
		
		int pagesNumber = pages.size();
		
		if (pagesNumber==0) {
			return null;
		}
		
		if (pagesNumber==1) {
			return pages.get(0);
		} else {
			byte[] imageBytes = pages.get(0);
			for (int i=1;i<pagesNumber;i++) {
				imageBytes = PDFUtils.mergePDFs(imageBytes, pages.get(i), true);
			}
			return imageBytes;
		}
		
	}

}
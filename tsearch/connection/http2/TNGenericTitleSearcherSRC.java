package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FrameTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.utils.PDFUtils;

import com.lowagie.text.DocumentException;

/**
 * 
 * @author Oprina George
 * 
 *         Oct 26, 2012
 */

public class TNGenericTitleSearcherSRC extends HttpSite {
	static Map<Integer, String> countyCNum = new LinkedHashMap<Integer, String>();
	
	static{
		
		countyCNum.put(CountyConstants.TN_Anderson, 	"2");
		
		countyCNum.put(CountyConstants.TN_Bedford, 		"34");
		countyCNum.put(CountyConstants.TN_Bledsoe, 		"T92");
		countyCNum.put(CountyConstants.TN_Bradley, 		"13");
		
		countyCNum.put(CountyConstants.TN_Campbell, 	"T12");
		countyCNum.put(CountyConstants.TN_Carter, 		"T7");
		countyCNum.put(CountyConstants.TN_Claiborne, 	"33");
		countyCNum.put(CountyConstants.TN_Clay, 		"T91");
		countyCNum.put(CountyConstants.TN_Cocke, 		"T19");
		countyCNum.put(CountyConstants.TN_Coffee, 		"40");
		countyCNum.put(CountyConstants.TN_Cumberland, 	"5");
		
		countyCNum.put(CountyConstants.TN_Decatur, 		"52");
		
		countyCNum.put(CountyConstants.TN_Fayette, 		"57");
		countyCNum.put(CountyConstants.TN_Fentress, 	"T25");
		countyCNum.put(CountyConstants.TN_Franklin, 	"T42");
	
		countyCNum.put(CountyConstants.TN_Giles, 		"28");
		countyCNum.put(CountyConstants.TN_Grainger, 	"T90");
		countyCNum.put(CountyConstants.TN_Greene, 		"T65");
		
		countyCNum.put(CountyConstants.TN_Hamblen, 		"29");
		countyCNum.put(CountyConstants.TN_Hawkins, 		"T46");
		countyCNum.put(CountyConstants.TN_Hickman, 		"59");
		countyCNum.put(CountyConstants.TN_Humphreys, 	"23");
		
		countyCNum.put(CountyConstants.TN_Jackson, 		"T44");
		countyCNum.put(CountyConstants.TN_Jefferson, 	"20");
		countyCNum.put(CountyConstants.TN_Johnson, 		"T39");
		
		countyCNum.put(CountyConstants.TN_Lawrence, 	"65");
		countyCNum.put(CountyConstants.TN_Lincoln, 		"T48");
		countyCNum.put(CountyConstants.TN_Loudon, 		"T95");
		
		countyCNum.put(CountyConstants.TN_Macon, 		"T99");
		countyCNum.put(CountyConstants.TN_Madison, 		"18");
		countyCNum.put(CountyConstants.TN_Marion, 		"21");
		countyCNum.put(CountyConstants.TN_Maury, 		"53");
		countyCNum.put(CountyConstants.TN_Monroe, 		"62");
		countyCNum.put(CountyConstants.TN_Moore, 		"M1");
		
		countyCNum.put(CountyConstants.TN_Perry, 		"T68");
		countyCNum.put(CountyConstants.TN_Pickett, 		"14");
		countyCNum.put(CountyConstants.TN_Polk, 		"T70");
		
		countyCNum.put(CountyConstants.TN_Rhea, 		"T43");
		countyCNum.put(CountyConstants.TN_Roane, 		"T69");
		
		countyCNum.put(CountyConstants.TN_Sequatchie, 	"T77");
		countyCNum.put(CountyConstants.TN_Sevier, 		"T16");
		countyCNum.put(CountyConstants.TN_Shelby, 		"T79");
		countyCNum.put(CountyConstants.TN_Smith, 		"63");
		countyCNum.put(CountyConstants.TN_Sullivan, 	"T94");
		
		countyCNum.put(CountyConstants.TN_Unicoi, 		"56");
		countyCNum.put(CountyConstants.TN_Union, 		"T89");
		
		countyCNum.put(CountyConstants.TN_Van_Buren, 	"T88");
		
		countyCNum.put(CountyConstants.TN_Washington, 	"3");
		countyCNum.put(CountyConstants.TN_Weakley, 		"32");
		countyCNum.put(CountyConstants.TN_White, 		"36");
		countyCNum.put(CountyConstants.TN_Williamson, 	"T4");
		countyCNum.put(CountyConstants.TN_Wilson, 		"24");
	}

	public LoginResponse onLogin() {
		try {
			String serverHomeLink = getDataSite().getServerHomeLink();

			HTTPRequest req = new HTTPRequest(serverHomeLink);

			String resp = execute(req);

			if (resp.contains("Forgot username")) {
				HTTPRequest loginReq = new HTTPRequest(serverHomeLink + "countySelect.php", HTTPRequest.POST);

				String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TNGenericTitleSearcherSRC", "user");
				String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TNGenericTitleSearcherSRC", "password");

				if (StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password)) {
					loginReq.setPostParameter("userName", user);
					loginReq.setPostParameter("password", password);
					loginReq.setPostParameter("loginb", "Go");

					resp = execute(loginReq);

					if (resp.contains("Subscribed Counties")) {
						HTTPRequest countyRequest = new HTTPRequest(serverHomeLink + getCnumForCounty());

						resp = execute(countyRequest);
						String countyName = getDataSite().getCountyName().toUpperCase();
						if (resp.contains("SEARCH " + countyName) || resp.contains("SEARCH " + countyName.replaceAll("\\s+", ""))) {
							HTTPRequest searchReq = new HTTPRequest(serverHomeLink + "search.php");

							resp = execute(searchReq);

							if (resp.contains("SEARCH OPTIONS")) {
								return LoginResponse.getDefaultSuccessResponse();
							} else{
								return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Search options not found");
							}
						} else{
							return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page with counties not found");
						}
					} else{
						return LoginResponse.getDefaultFailureResponse();
					}
				} else{
					return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed. No credentials");
				}
			} else{
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Login page not found");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return LoginResponse.getDefaultFailureResponse();
	}

	private String getCnumForCounty() {
		
		return "countySearchPage.php?cnum=" + countyCNum.get(getDataSite().getCountyId());
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.GET) {
			String serverHomeLink = getDataSite().getServerHomeLink();

			if(req.getURL().contains("imgview.php") && !req.getURL().contains("PHPSESSID")){
				for(Cookie c : getHttpClient().getState().getCookies()){
					if(c.getName().equals("PHPSESSID")){
						req.setURL(req.getURL()+"&PHPSESSID="+c.getValue());
						break;
					}
				}
				
				String instr = req.getURL().replaceAll("(?sim).*instNum=([^\\&$]*).*", "$1");
				
				HTTPRequest instrSearchReq = new HTTPRequest(serverHomeLink + "instNumSearch.php?executeSearch=execute+search&instNum="+instr);
				execute(instrSearchReq);
			} else if (req.getURL().contains("deedholdImage.php")){
				req.setURL(req.getURL().replace(".pdf", ""));
				String response = execute(req);
				if (response.contains("deedholdViewer.php")){
					HtmlParser3 parser = new HtmlParser3(response);
					if (parser != null){
						NodeList frameList = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("frame"), true);
						if (frameList != null && frameList.size() > 0){
							String checkForImageLink = "", getImageLink = "";
							for (int i = 0; i < frameList.size(); i++){
								FrameTag frame = (FrameTag) frameList.elementAt(i);
								if (frame != null){
									String location = frame.getFrameLocation();
									if (location.contains("imgview.php")){
										getImageLink = location;
									} else if (location.contains("deedholdViewer.php")){
										checkForImageLink = location;
									}
								}
							}
							if (StringUtils.isNotEmpty(checkForImageLink)){
								HTTPRequest instrSearchReq = new HTTPRequest(serverHomeLink + checkForImageLink);
								execute(instrSearchReq);
							}
							if (StringUtils.isNotEmpty(getImageLink)){
								req.setURL(serverHomeLink + getImageLink);
							}
						}
					}
				}
			} else if (req.getURL().contains("platholdImage.php")){
				req.setURL(req.getURL().replace(".pdf", ""));
				String response = execute(req);
				if (response.contains("platholdViewer.php")){
					HtmlParser3 parser = new HtmlParser3(response);
					if (parser != null){
						NodeList frameList = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("frame"), true);
						if (frameList != null && frameList.size() > 0){
							String checkForImageLink = "", getImageLink = "";
							for (int i = 0; i < frameList.size(); i++){
								FrameTag frame = (FrameTag) frameList.elementAt(i);
								if (frame != null){
									String location = frame.getFrameLocation();
									if (location.contains("imgview.php")){
										getImageLink = location;
									} else if (location.contains("platholdViewer.php")){
										checkForImageLink = location;
									}
								}
							}
							if (StringUtils.isNotEmpty(checkForImageLink)){
								HTTPRequest instrSearchReq = new HTTPRequest(serverHomeLink + checkForImageLink);
								execute(instrSearchReq);
							}
							if (StringUtils.isNotEmpty(getImageLink)){
								req.setURL(serverHomeLink + getImageLink);
							}
						}
					}
				}
			} else if (req.getURL().contains("indexedbooks.php")){
				req.setURL(req.getURL().replace(".pdf", ""));
				String response = execute(req);
				if (response.contains("imgview.php")){
					Matcher newLocationMatcher = Pattern.compile("(?is)window.location\\s*=\\s*\\\"([^\\\"]+)").matcher(response);
					if (newLocationMatcher.find()){
						String getImageLink = newLocationMatcher.group(1);
						if (StringUtils.isNotEmpty(getImageLink)){
							req.setURL(serverHomeLink + getImageLink);
							req.setMethod(HTTPRequest.GET);
						}
					}
				}
			} else if (req.getURL().contains("view_oldIndexes.php")){
				String url = req.getURL().replace(".pdf", "");
				req.setURL(url);
				String allPages = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(url, "allPages");
				url = url.replaceFirst("(?is)allPages[^&]*&", "");
				if (StringUtils.isNotEmpty(allPages)){
					allPages = allPages.replaceFirst("_$", "");
					
					String[] pages = allPages.split("_");
					if (pages.length > 0){
						List<InputStream> sourcesList = new LinkedList<InputStream>();
						HTTPResponse imageResp = null;
						for (String page : pages) {
							String newUrl = url.replaceFirst("(?is)indexNum=&", "indexNum=" + page + "&");
							HTTPRequest newReq = new HTTPRequest(newUrl, HTTPRequest.GET);
							try {
								imageResp = exec(newReq);
								sourcesList.add(imageResp.getResponseAsStream());
							} catch (Exception e) {}
						}
						
						if (sourcesList.size() > 0){
							OutputStream outputStream = new ByteArrayOutputStream();
							try {
								PDFUtils.concatenatePDFs(sourcesList, outputStream);
							} catch (IOException e) {
								e.printStackTrace();
							} catch (DocumentException e) {
								e.printStackTrace();
							}
							imageResp.is = new ByteArrayInputStream(((ByteArrayOutputStream) outputStream).toByteArray());
							// bypass response
							req.setBypassResponse(imageResp);
						}
					}
				}
			} else if (!req.getURL().contains("viewDetails.php") &&
					!req.getURL().contains("imgview.php") &&
					!req.getURL().contains("bookPageSearch.php") &&
					!req.getURL().contains("instNumSearch.php") &&
					!req.getURL().contains("legalDescSearch.php")) {
				if (req.getURL().contains("nameType")) {
					HTTPRequest nameSearchReq = new HTTPRequest(serverHomeLink + "nameSearch.php");
					execute(nameSearchReq);
				} else if (req.getURL().contains("book")) {
					HTTPRequest nameSearchReq = new HTTPRequest(serverHomeLink + "bookPageSearch.php");
					execute(nameSearchReq);
				} else if (req.getURL().contains("District")) {
					HTTPRequest nameSearchReq = new HTTPRequest(serverHomeLink + "subdivisionSearch.php");
					execute(nameSearchReq);
				} else if (req.getURL().contains("instNum")) {
					HTTPRequest nameSearchReq = new HTTPRequest(serverHomeLink + "instNumSearch.php");
					execute(nameSearchReq);
				} else if (req.getURL().contains("legalDesc")) {
					HTTPRequest nameSearchReq = new HTTPRequest(serverHomeLink + "legalDescSearch.php");
					execute(nameSearchReq);
				} else if (req.getURL().contains("instType1")) {
					HTTPRequest nameSearchReq = new HTTPRequest(serverHomeLink + "instTypeSearch.php");
					execute(nameSearchReq);
				}
			}
		}
	}

	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
	}

	public String getCertDate() {
		String serverHomeLink = getDataSite().getServerHomeLink();
		
		HTTPRequest countyRequest = new HTTPRequest(serverHomeLink + getCnumForCounty());

		String resp = execute(countyRequest);

		if (resp.contains("SEARCH " + getDataSite().getCountyName().toUpperCase())) {
			HTTPRequest searchReq = new HTTPRequest(serverHomeLink + "search.php");

			resp = execute(searchReq);

			if (resp.contains("SEARCH OPTIONS")) {
				return resp;
			}
		}
		
		return "";
	}

}

package ro.cst.tsearch.connection.http3;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.cookie.Cookie;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

public class COGenericSOS extends HttpSite3 {

	private String	docWorkThruDt	= "";

	private String	urlSrv1			= "biz/BusinessEntityCriteriaExt.do";
	private String	urlSrv2			= "biz/AdvancedSearchCriteria.do";
	private String	urlSrv3			= "biz/AdvancedTrademarkSearchCriteria.do";
	private String  urlDetails      = "biz/BusinessEntityDetail.do"; 
	private String	urlPdf			= "biz/ViewImage.do";
	
	private String cookie = "";
	private boolean isFirstRequest = true;
	private int numberOfRetry = 0;
	private static final int MAX_NUMBER_OF_RETRIES = 3;

	// private String imageReferer =
	// "biz/BusinessEntityHistory.do?quitButtonDestination=BusinessEntityDetail&pi1=1&srchTyp=ENTITY&nameTyp=ENT&entityId2=&masterFileId=";

	public HTTPResponse getResponse(String url) {
		
		try {
			
			HTTPRequest req = new HTTPRequest(getDataSite().getServerHomeLink() + url);
			HTTPResponse res = process(req);
			isFirstRequest = false;
			String resp = res.getResponseAsString();
			
			//make another request (a POST)
			Matcher ma1 = Pattern.compile("(?is)<body[^>]*>\\s*<form\\s+method=\"POST\"\\s+action=\"([^\"]+)\"/>(.*?)</form>\\s*</body>").matcher(resp);
			if (ma1.find()) {
				HTTPRequest reqP = new HTTPRequest(getDataSite().getServerHomeLink() + URLDecoder.decode(ma1.group(1), "UTF-8").replaceFirst("^/", ""), HTTPRequest.POST);
				Matcher ma2 = Pattern.compile("(?is)<input[^>]+name=\"([^\"]+)\"[^>]+value=\"([^\"]+)\"[^>]*>").matcher(ma1.group(2));
				Vector<String> order = new Vector<String>();
				while (ma2.find()) {
					reqP.setPostParameter(ma2.group(1), ma2.group(2));
					order.add(ma2.group(1));
				}
				reqP.setPostParameterOrder(order);
				String cookie = computeCookie(resp);
				this.cookie = cookie;
				reqP.setHeader("Cookie", cookie);
				HTTPResponse resP = process(reqP);
				return resP;
			}	
			
		}  catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
			
	}
	
	public String buildCookie() {
		List<Cookie> cookies = getCookies();
		StringBuilder sb = new StringBuilder();
		if (cookies != null && cookies.size() > 0){
			for (Cookie ck : cookies) {
				sb.append(ck.getName()).append("=").append(ck.getValue()).append("; ");
			}
		}
		String s = sb.toString();
		if (s.contains(cookie)) {
			s = s.replaceFirst("; $", "");
		} else {
			s += cookie;
		}
		return s;
	}
	
	public LoginResponse onLogin() {
		try {
			
			HTTPResponse response = getResponse(urlSrv1);
			String resp = "";
			if (response!=null) {
				resp = response.getResponseAsString();
			}
			
			// you might need to call computeCookie(resp) here and make an additional
			// request with the cookie set to get the main search page
			if (resp.contains("Business paper documents processed through:")) {
				// extract docWorkThruDt
				NodeList nodes = new HtmlParser3(Tidy.tidyParse(resp, null)).getNodeList()
						.extractAllNodesThatMatch(new HasAttributeFilter("action", "/biz/BusinessEntityCriteriaExt.do"), true)
						.extractAllNodesThatMatch(new TagNameFilter("table"), true);

				if (nodes.size() > 0) {
					TableTag t = (TableTag) nodes.elementAt(0);
					if (t.toHtml().contains("Business paper documents processed through")) {
						String docWorkThruDt = t.getRow(0).getColumns()[1].toPlainTextString();
						if (StringUtils.isNotEmpty(docWorkThruDt)) {
							this.docWorkThruDt = docWorkThruDt;
							return LoginResponse.getDefaultSuccessResponse();
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return LoginResponse.getDefaultFailureResponse();
	}

	public void onBeforeRequestExcl(HTTPRequest req) {
		String url = req.getURL();
		if (req.getMethod() == HTTPRequest.POST) {
			if (url.contains(urlSrv1)) {
				try {
					HTTPResponse response = getResponse(urlSrv1);
					String resp = "";
					if (response!=null) {
						resp = response.getResponseAsString();
					}

					if (resp.contains("Business paper documents processed through:")) {
						req.setPostParameter("docWorkThruDt", docWorkThruDt);
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (url.contains(urlSrv2)) {
				getResponse(urlSrv2);
			} else if (url.contains(urlSrv3)) {
				try {
					getResponse(urlSrv3);
					
					// remove fake params
					for (int i = 1; i <= 3; i++) {
						req.removePostParameters("fake" + i);
					}

					// merge request params

					// tmStatus
					for (int i = 0; i < 5; i++) {
						String value = req.getPostFirstParameter("tmStatus" + i);
						req.removePostParameters("tmStatus" + i);
						if (StringUtils.isNotEmpty(value)) {
							req.setPostParameter("tmStatus", value);
						}
					}
					
					for (int i = 0; i < 45; i++) {
						String value = req.getPostFirstParameter("tmClasses" + i);
						req.removePostParameters("tmClasses" + i);
						if (StringUtils.isNotEmpty(value)) {
							req.setPostParameter("tmClasses", value);
						}
					}

					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			req.setHeader("Cookie", buildCookie());
		} else if (req.getMethod() == HTTPRequest.GET && !isFirstRequest) {
			req.setHeader("Cookie", buildCookie());
		}
	}

	int	numberOfTries		= 0;
	int	maxNumberOfTries	= 5;

	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
//		if (res.getLastURI().getEscapedURI().contains("Timeout")) {
//
//		}
		if (!isFirstRequest && numberOfRetry<MAX_NUMBER_OF_RETRIES) {
			if ( (req.getMethod() == HTTPRequest.GET && !req.getURL().contains(urlPdf)) || (req.getMethod() == HTTPRequest.POST && res.getLastURI().toString().contains(urlDetails)) ) {
				String resp = res.getResponseAsString();
				if (resp.contains("<body onload=\"test()\">")) {	//bad page, need to retry
					if (!StringUtils.isEmpty(cookie)) {
						numberOfRetry++;
						HTTPRequest req2 = new HTTPRequest(req.getURL());
						req.setHeader("Cookie", buildCookie());
						HTTPResponse res2 = process(req2);
						res.is = new ByteArrayInputStream(res2.getResponseAsByte());
						res.contentLenght= res2.contentLenght;
						res.contentType = res2.contentType;
						res.headers = res2.headers;
						res.returnCode = res2.returnCode;
						res.body = res2.body;
						res.setLastURI(res2.getLastURI());
					}
				} else {
					numberOfRetry = 0;
					res.is = new ByteArrayInputStream(resp.getBytes());
				}
			}
		}
		
	}

	/**
	 * @return the urlSrv1
	 */
	public String getUrlSrv1() {
		return urlSrv1;
	}

	/**
	 * @param urlSrv1
	 *            the urlSrv1 to set
	 */
	public void setUrlSrv1(String urlSrv1) {
		this.urlSrv1 = urlSrv1;
	}

	/**
	 * @return the urlSrv2
	 */
	public String getUrlSrv2() {
		return urlSrv2;
	}

	/**
	 * @param urlSrv2
	 *            the urlSrv2 to set
	 */
	public void setUrlSrv2(String urlSrv2) {
		this.urlSrv2 = urlSrv2;
	}

	/**
	 * @return the urlSrv3
	 */
	public String getUrlSrv3() {
		return urlSrv3;
	}

	/**
	 * @param urlSrv3
	 *            the urlSrv3 to set
	 */
	public void setUrlSrv3(String urlSrv3) {
		this.urlSrv3 = urlSrv3;
	}
	
	// you might need this function to compute the TSd9747d_75 cookie
	private static String computeCookie(String resp) {
		String cookie = "";

		String table = "";
		Matcher mat = Pattern.compile("var table = \"([^\"]*)\"").matcher(resp);
		if(mat.find()) {
			table = mat.group(1);
		}

		long c = 0;
		mat = Pattern.compile("var c = (\\d+)").matcher(resp);
		if(mat.find()) {
			c = Long.parseLong(mat.group(1));
		}

		String slt = "";
		mat = Pattern.compile("var slt = \"([^\"]*)\"").matcher(resp);
		if(mat.find()) {
			slt = mat.group(1);
		}

		String s1 = "";
		mat = Pattern.compile("var s1 = '([a-z])'").matcher(resp);
		if(mat.find()) {
			s1 = mat.group(1);
		}

		String s2 = "";
		mat = Pattern.compile("var s2 = '([a-z])'").matcher(resp);
		if(mat.find()) {
			s2 = mat.group(1);
		}

		int n = 0;
		mat = Pattern.compile("var n = (\\d+)").matcher(resp);
		if(mat.find()) {
			n = Integer.parseInt(mat.group(1));
		}

		int start = s1.charAt(0);
		int end = s2.charAt(0);
		String[] arr = new String[n];
		double m = Math.pow((double)(end - start + 1), (double)n);
		String chlg = "";
		int crc = 0;

		for(int i = 0; i < n; i++) {
			arr[i] = s1;
		}
		for(int i = 0; i < m - 1; i++) {
			for(int j = n - 1; j >= 0; --j) {
				int t = arr[j].charAt(0);
				t++;
				arr[j] = Character.toChars(t)[0] + "";

				if(arr[j].charAt(0) <= end) {
					break;
				} else {
					arr[j] = s1;
				}
			}
			chlg = joinArray(arr);
			String str = chlg + slt;
			crc = 0;
			crc = crc ^ (-1);

			for(int k = 0; k < str.length(); k++) {
				long beginIdx = ((crc ^ (0 + str.charAt(k))) & 0x000000FF) * 9;
				crc = ((int)crc >> 8) ^ (int)Long.parseLong(table.substring((int)beginIdx, (int)beginIdx + 8), 16);
			}
			crc =  crc ^ (-1);
			crc = Math.abs(crc);
			if (crc == c){
				break;
			}
		}

		String prefix = "";
		mat = Pattern.compile("document.cookie = \"([^\"]*)\" \\+ \"([^\"]*)\"").matcher(resp);
		if(mat.find()) {
			prefix = mat.group(1) + mat.group(2);
		}

		cookie = prefix + chlg + ":" + slt + ":" + crc;

		return cookie;
	}

	private static String joinArray(String[] array) {
		StringBuilder sb = new StringBuilder();
		for(String str : array) {
			sb.append(str);
		}
		return sb.toString();
	}
}

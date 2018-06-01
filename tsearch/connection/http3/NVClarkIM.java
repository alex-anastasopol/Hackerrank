package ro.cst.tsearch.connection.http3;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.ATSConnConstants;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.CustomSSLSocketFactory;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.StringUtils;

public class NVClarkIM extends HttpSite3 {

	
	public static final Pattern FORM_PATTERN = Pattern.compile("<form(.*?)name=\"formQuery\"\\s+action=\"([^\"]+)\"\\s*>");
	
	private String lastFormAction = null; 
	private String logoutLink = null;
	
	@Override
	public LoginResponse onLogin() {
		
		DataSite dataSite = getDataSite();
		
		((DefaultHttpClient)getHttpClient()).getCookieStore().clear();
		
		String vpnUser = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), dataSite.getName(), "vpnUser");
		String vpnPassword = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), dataSite.getName(), "vpnPassword");
		
		String libertyUser = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), dataSite.getName(), "libertyUser");
		String libertyPassword = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), dataSite.getName(), "libertyPassword");
		
		if(StringUtils.isEmpty(vpnUser) 
				|| StringUtils.isEmpty(vpnPassword) 
				|| StringUtils.isEmpty(libertyUser)
				|| StringUtils.isEmpty(libertyPassword)) {
			return LoginResponse.getDefaultInvalidCredentialsResponse();
		}
		
//		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
//        Protocol.registerProtocol("https", easyhttps);
		
		/**
		 * Get first page and check if we are on login page
		 * 01. ---------------------------------------------------------------------------------------------------------
		 */
		HTTPRequest request = new HTTPRequest(getSiteLink());
		request.setHeader("User-Agent", getUserAgentValue());
		request.setHeader("Accept", getAccept());
		request.setHeader("Accept-Language", getAcceptLanguage());
		request.setHeader("Accept-Encoding", "gzip, deflate");
		request.setHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		
		String responseAsString = execute(request);
		Pattern titlePattern = Pattern.compile("(?is)<title>(.*?)</title>");
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for " + getSiteLink());
		}
		Matcher matcher = titlePattern.matcher(responseAsString);
		
		if(matcher.find()) {
			String title = matcher.group(1);
			if(!"Installation".equalsIgnoreCase(title)) {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Unexpected page received with title " + title);
			} 
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Unexpected page received with no title ");
		}
		
		/**
		 * Get main.js page in order to read the vkey
		 * 02. ---------------------------------------------------------------------------------------------------------
		 */
		request.modifyURL("https://vpn3030.insnoc.com/CACHE/sdesktop/install/binaries/main.js");
		request.setHeader("Referer", "https://vpn3030.insnoc.com/CACHE/sdesktop/install/start.htm");
		responseAsString = execute(request);
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for main.js");
		}
		
		Pattern vkeyPattern = Pattern.compile("var vkey = \"([^\"]+)\"");
		matcher = vkeyPattern.matcher(responseAsString);
		String vkey = null;
		if(matcher.find()) {
			vkey = matcher.group(1);
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find vkey in main.js");
		}
		
		/**
		 * Get empty.htm.... just for fun
		 * 03. ---------------------------------------------------------------------------------------------------------
		 */
		
		request.modifyURL("https://vpn3030.insnoc.com/CACHE/sdesktop/install/empty.htm");
		request.setHeader("Referer", "https://vpn3030.insnoc.com/CACHE/sdesktop/install/start.htm");
		execute(request);
		
		
		/**
		 * Get secret.xml to read the secret number
		 * 04. ---------------------------------------------------------------------------------------------------------
		 */
		request.modifyURL("https://vpn3030.insnoc.com/CACHE/sdesktop/install/secret.xml");
		request.removeHeader("Referer");
		setLastRequest(null);
		
		responseAsString = execute(request);
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for secret.xml");
		}
		
		Pattern secretPattern = Pattern.compile("(?is)<secret>(.*?)</secret>");
		matcher = secretPattern.matcher(responseAsString);
		String secret = null;
		if(matcher.find()) {
			secret = matcher.group(1);
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find secret in secret.xml");
		}
		
		String timestamp = vigenere(secret, vkey, false);
		
		/**
		 * Get data.xml to read the data :)
		 * 05. ---------------------------------------------------------------------------------------------------------
		 */
		request.modifyURL("https://vpn3030.insnoc.com/CACHE/sdesktop/data.xml");
		request.removeHeader("Referer");
		setLastRequest(null);
		responseAsString = execute(request);
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for data.xml");
		}
		
		Pattern fieldPattern = Pattern.compile("(?i)<field\\s+type=\"([^\"]+)\"\\s+name=\"([^\"]+)\"\\s+value=\"([^\"]+)\"/>");
		Pattern multiLocationPattern  = Pattern.compile("(?is)<multilocation>(.*?)</multilocation>");
		matcher = multiLocationPattern.matcher(responseAsString);
		
		String wFailureWebBrowsing = null;
		String wFailureFileAccess = null;
		String wFailurePortForwarding = null;
		String wFailureFullTunneling = null;
		
		if(matcher.find()) {
			String multiLocation = matcher.group(1);
			
			matcher = fieldPattern.matcher(multiLocation);
			
			while(matcher.find()) {
				String currentName = matcher.group(2);
				String currentValue = matcher.group(3);
				
				if(currentName.equals("dVPNWindowsFailureWebBrowsing")) {
					wFailureWebBrowsing = currentValue;
				}
				if(currentName.equals("dVPNWindowsFailureFileAccess")) {
					wFailureFileAccess = currentValue;
				}
				if(currentName.equals("dVPNWindowsFailurePortForwarding")) {
					wFailurePortForwarding = currentValue;
				}
				if(currentName.equals("dVPNWindowsFailureFullTunneling")) {
					wFailureFullTunneling = currentValue;
				}
			}			
		}
		
		if(wFailureWebBrowsing == null || wFailureFileAccess == null || wFailurePortForwarding == null || wFailureFullTunneling == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find parse data.xml and extract multilocation fields");
		}
		
		
		String version = null;
		
		Pattern versionPattern = Pattern.compile("(?i)<data\\s+version=\"([^\"]+)\">");
		matcher = versionPattern.matcher(responseAsString);
		if(matcher.find()) {
			version = matcher.group(1);
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find parse data.xml and extract version");
		}
		
		String paramForId = "ts" + timestamp + ";wb" + wFailureWebBrowsing
				+ ";fa" + wFailureFileAccess + ";pf" + wFailurePortForwarding
				+ ";ft" + wFailureFullTunneling + ";fewifa;ve" + version;
		
		String data = encodeString(vigenere(paramForId, vkey, true));
		
		
		
		/**
		 * Run result.htm page with data parameter
		 * 06. ---------------------------------------------------------------------------------------------------------
		 */
		request.modifyURL("https://vpn3030.insnoc.com/CACHE/sdesktop/install/result.htm?group=&data=" + data);		
		request.setHeader("Referer", "https://vpn3030.insnoc.com/CACHE/sdesktop/install/start.htm");
		
		responseAsString = execute(request);
		
		/**
		 * Get global.js - don't care about response
		 */
		request.setReferer(request.getURL());
		request.modifyURL("https://vpn3030.insnoc.com/CACHE/sdesktop/globals.js");
		execute(request);
		
		
		/**
		 * Get login page again
		 * 07. ---------------------------------------------------------------------------------------------------------
		 */
		
		BasicClientCookie cookie = new BasicClientCookie("sdesktop", data);
		cookie.setDomain("vpn3030.insnoc.com");
		cookie.setPath("/");
		((DefaultHttpClient)getHttpClient()).getCookieStore().addCookie(cookie);
//		getHttpClient().getState().addCookie(cookie);
		request.modifyURL(getSiteLink());
		
		responseAsString = execute(request);
		
		if(!responseAsString.contains("Please enter your username and password")) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not get login page!");
		}
		
		
		
		
		/**
		 * Enter vpn user and pass
		 * 08. ---------------------------------------------------------------------------------------------------------
		 */
		request.setMethod(HTTPRequest.POST);
		request.setReferer(request.getURL());
		
		request.setPostParameter("username", vpnUser);
		request.setPostParameter("password", vpnPassword);
		request.setPostParameter("Login", "Login");
		request.setPostParameter("next", "");
		
		Vector<String> postParameterOrderVector = new Vector<String>();
		postParameterOrderVector.add("username");
		postParameterOrderVector.add("password");
		postParameterOrderVector.add("Login");
		postParameterOrderVector.add("next");
		request.setPostParameterOrder(postParameterOrderVector);
		
		responseAsString = execute(request);
		request.setPostParameterOrder(null);	//clean to be sure
		if(!responseAsString.contains("var next = \"/webvpn/index.html\"") || responseAsString.contains("Login failed")) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not pass first login page");
		}
		
		/**
		 * Go get page with the link to liberty
		 * 09. ---------------------------------------------------------------------------------------------------------
		 */
		request.modifyURL("https://vpn3030.insnoc.com/webvpn/index.html");
		request.setMethod(HTTPRequest.GET);
		request.clearPostParameters();
		
		responseAsString = execute(request);
		
		if(responseAsString == null 
				|| (!responseAsString.contains("If the Floating Toolbar does not open, click here to open it") && !responseAsString.contains("LibertyNet"))) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find LibertyNet link");
		}
		
		/**
		 * Go get page with the link to liberty
		 */
		request.modifyURL("https://vpn3030.insnoc.com/http/0/10.10.50.10/");
		responseAsString = execute(request);
		if(responseAsString == null 
				|| !responseAsString.contains("<title>NETCommunicate Login</title>")) {
			
			request.modifyURL("https://vpn3030.insnoc.com/webvpn_logout.html");
			process(request);
			
			
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not load LibertyNet login page");
		}
		
		/**
		 * Enter login information
		 */
		request.setMethod(HTTPRequest.POST);
		request.setHeader("Referer", request.getURL());
		request.setPostParameter("name", libertyUser);
		request.setPostParameter("password", libertyPassword);
		request.setPostParameter("Login", "Login");
		request.setPostParameter("system", "Clark County Image L.L.C.");
		
		responseAsString = execute(request);
		if(responseAsString == null 
				|| !responseAsString.contains("<font class=\"LibFieldName\">Book-Page</font>")) {
			
			process(new HTTPRequest("https://vpn3030.insnoc.com/webvpn_logout.html"));
			
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not load LibertyNet search page, maybe invalid liberty login");
		}
		
		
		matcher = FORM_PATTERN.matcher(responseAsString);
		if(matcher.find()) {
			lastFormAction = matcher.group(2);
			logoutLink = getLogoutLink(new HtmlParser3(responseAsString));
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find form formQuery");
		}
		
		if(lastFormAction != null) {
			return LoginResponse.getDefaultSuccessResponse();
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find action for for formQuery, found null");
		}
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		req.modifyURL(lastFormAction);
		
		String text = req.getPostFirstParameter("date");
		req.removePostParameters("date");
		String secondPart = req.getPostFirstParameter("docnumber");
		req.removePostParameters("docnumber");

		if(StringUtils.isNotEmpty(text)) {
			text += "-" + secondPart;
		} else {
			text = secondPart;
		}
		
		req.setPostParameter("text", text);
		
		String responseAsString = execute(req);
		HTTPResponse imageResponse = null;
		HTTPResponse response = new HTTPResponse();
		response.returnCode = ATSConnConstants.HTTP_OK;
		response.contentType = "text/html";
		response.headers = new HashMap<String, String>();
		
		if(responseAsString == null) {
			response.is = IOUtils.toInputStream("Message: Could not get a response from official site");
		} else {
			
			try {
				
				HtmlParser3 parser = new HtmlParser3(responseAsString);
				
				NodeList allLinks = parser.getNodeList()
						.extractAllNodesThatMatch(new TagNameFilter("a"), true);
				
				List<LinkTag> links = new ArrayList<LinkTag>();
				
				for (int i = 0; i < allLinks.size(); i++) {
					LinkTag tempLink = (LinkTag) allLinks.elementAt(i);
					String altAttribute = tempLink.getAttribute("alt");
					if(altAttribute != null && altAttribute.startsWith("Click to open this document")) {
						links.add(tempLink);
					}
				}
				
				String newLink = getFormLink(parser);
				
				if(links.size() == 0) {
					response.is = IOUtils.toInputStream("Message: No Documents Found");
				} else {
					LinkTag link = null;
					for (LinkTag tempLink : links) {
						if(text.equals(tempLink.getLinkText().trim())) {
							link = tempLink;
							break;
						}
					}
					if(link == null) {
						response.is = IOUtils.toInputStream("Message: No Records Found");
					} else {
						/**
						 * I have in link the link to image, let's get it
						 */
						req.modifyURL(link.getLink());
						req.setMethod(HTTPRequest.GET);
						req.clearPostParameters();
						
						imageResponse = process(req);
						if(imageResponse == null) {
							//TODO:
						}
					}
				}
				
				if(newLink != null) {
					req.modifyURL(newLink);
					req.setReferer(lastFormAction);
					
					responseAsString = execute(req);
					
					if(responseAsString == null 
							|| !responseAsString.contains("<font class=\"LibFieldName\">Book-Page</font>")) {
						response.is = IOUtils.toInputStream("Message: Error while getting image!");
						destroySession();	
					} else {
					
						Matcher matcher = FORM_PATTERN.matcher(responseAsString);
						if(matcher.find()) {
							lastFormAction = matcher.group(2);
							String tempLink = getLogoutLink(new HtmlParser3(responseAsString));
							if(tempLink != null) {
								logoutLink = tempLink; 
							} else {
								logger.error("Could not get logout link. Using old link: " + logoutLink);
							}
							
							
						} else {
							response.is = IOUtils.toInputStream("Message: Error while getting image!");
							destroySession();
						}
					}
					
				}
				
				//TODO: manage errors
				
			} catch (Exception e) {
				logger.error("Error while parsing response");
				response.is = IOUtils.toInputStream("Message: Error while parsing response");
				return;	
			}
			
		}
		
		
		if(imageResponse != null) {
			req.setBypassResponse(imageResponse);
		} else {
			
			if(response.is == null) {
				response.is = IOUtils.toInputStream("Message: Error processing request!");
			}
			req.setBypassResponse(response);
		}
		
	}
	
	@Override
	public boolean onLogout() {
		
		if(logoutLink != null) {
			
			setAttribute("onBeforeRequest", Boolean.TRUE);
			
			try {
			
				HTTPRequest req = new HTTPRequest(logoutLink);
				process(req);
				
				req.modifyURL("https://vpn3030.insnoc.com/webvpn_logout.html");
				process(req);
				
				return true;
			} finally {
				setAttribute("onBeforeRequest", Boolean.FALSE);
			}
		}
		
		
		return false;
	}

	public String getFormLink(HtmlParser3 parser) {
		NodeList allLinks = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("a"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("href", "#"))
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "LibTabLink"));
		
		LinkTag formSearchLink = new LinkTag();
		
		for (int i = 0; i < allLinks.size(); i++) {
			LinkTag tempLink = (LinkTag) allLinks.elementAt(i);
			if("Search".equals(tempLink.getLinkText())) {
				formSearchLink = tempLink;
				break;
			}
		}
		
		String onClick = formSearchLink.getAttribute("onClick");
		
		Pattern pattern = Pattern.compile("TabButtonHandler\\((\\d+),'http://10.10.50.10/LibertyIMS::([^']+)'\\)");
		Matcher matcher = pattern.matcher(onClick);
		
		String newLink = null;
		
		if(matcher.find()) {
			newLink = "https://vpn3030.insnoc.com/http/0/10.10.50.10/LibertyIMS::" + matcher.group(2) + "cmd=XMLExecTBButton;ID=" + matcher.group(1);
		}
		return newLink;
	}
	
	public String getLogoutLink(HtmlParser3 parser) {
		NodeList someNodes = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("value", "Logout"));
		
		
		if(someNodes.size() > 0) {
			InputTag node = (InputTag)someNodes.elementAt(0);
			String onClick = node.getAttribute("onclick");
			Pattern pattern = Pattern.compile("SiteButtonHandler\\((\\d+),'http://10.10.50.10/LibertyIMS::([^']+)'\\)");
			Matcher matcher = pattern.matcher(onClick);
			
			String newLink = null;
			
			if(matcher.find()) {
				newLink = "https://vpn3030.insnoc.com/http/0/10.10.50.10/LibertyIMS::" + matcher.group(2) + "cmd=XMLExecTBButton;ID=" + matcher.group(1);
			}
			
			return newLink;
		}
		
				
		
		return null;
	}
	
	
	/**
	 * Equivalent with javascript vigenere function found in main.js
	 * @param input
	 * @param key
	 * @param forward
	 * @return
	 */
	protected String vigenere(String input, String key, boolean forward) {
		String alphabet = "iPkjKwpEA1IcgxV0TZ7qrbXOeRG8M4vf2hdUtzBNFYoaS3u6mLJQD9HnyWC5sl";
		int keyLength = key.length();
		// Transform input:
		int inputLength = input.length();
		String output = "";
		int key_index = 0;
		for (int i = 0; i < inputLength; i++) {
			char input_char = input.charAt(i);
			int input_char_value = alphabet.indexOf(input_char);
			if (input_char_value < 0) {
				output += input_char;
				continue;
			}
			if (forward) {
				input_char_value += alphabet.indexOf(key.charAt(key_index));
			} else {
				input_char_value -= alphabet.indexOf(key.charAt(key_index));
			}
			input_char_value = (input_char_value + alphabet.length()) % alphabet.length();
			output += alphabet.charAt(input_char_value);
			key_index = (key_index + 1) % keyLength;
		}
		return output;
	}
	
	protected String  encodeString( String st )
	{
		String st2 = "";
		for ( int j = 0; j < st.length(); j++ )
		{
			String temp2 = Integer.toHexString((int)st.charAt(j)).toUpperCase();
			//String temp2 = ( '' + parseInt( st.charCodeAt( j ) ).toString( 16 ) ).toUpperCase();
			if ( temp2.length() == 1 )
				temp2 = "0" + temp2;
			st2 += temp2;
		}
		return st2;
	}
	
	@Override
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:5.0) Gecko/20100101 Firefox/5.0";
	}
	
	@Override
	public String getAcceptLanguage() {
		return "en-us,en;q=0.5";
	}
	
	public static void main(String[] args) {
		
		NVClarkIM site = new NVClarkIM();
		
		String vkey = "Sw9C7";
		String secret = "1DSw1iJJO";
		String timestamp = site.vigenere(secret, vkey, false);
		
		
		String wFailureWebBrowsing = "1";
		String wFailureFileAccess = "1";
		String wFailurePortForwarding = "1";
		String wFailureFullTunneling = "0";
		
		String version = "3.0";
		
		String paramForId = "ts" + timestamp + ";wb" + wFailureWebBrowsing
				+ ";fa" + wFailureFileAccess + ";pf" + wFailurePortForwarding
				+ ";ft" + wFailureFullTunneling + ";fewifa;ve" + version;
		
		String data = site.encodeString(site.vigenere(paramForId, vkey, true));
		
		System.out.println("d1 = " + data );
		System.out.println("d2 = " + "376A3761313943315930313B4967773B4C52563B3538383B7859703B386F4C77584E3B6D704A2E70");
		
		System.out.println(data.equals("376A3761313943315930313B4967773B4C52563B3538383B7859703B386F4C77584E3B6D704A2E70"));
		
		
	}
	
	@Override
	protected SSLSocketFactory buildSSLSocketFactory() {
		TrustStrategy ts = new TrustStrategy() {
			@Override
			public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
				return true;
			}
		};

		SSLSocketFactory sf = null;

		try {
			sf = new CustomSSLSocketFactory(ts, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		} catch (NoSuchAlgorithmException e) {
			logger.error("Failed to initialize SSL handling.", e);
		} catch (KeyManagementException e) {
			logger.error("Failed to initialize SSL handling.", e);
		} catch (KeyStoreException e) {
			logger.error("Failed to initialize SSL handling.", e);
		} catch (UnrecoverableKeyException e) {
			logger.error("Failed to initialize SSL handling.", e);
		}

		return sf;
	}
}

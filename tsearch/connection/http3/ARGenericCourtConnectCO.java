package ro.cst.tsearch.connection.http3;



import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.FrameTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.auth.ssl.CustomSSLSocketFactory;
import ro.cst.tsearch.utils.StringUtils;

public class ARGenericCourtConnectCO extends HttpSite3{
	
	public String cookie="";
	
	public LoginResponse onLogin(){
		
//		Protocol authhttps = new Protocol("https", (ProtocolSocketFactory) (new TLSSocketFactory()), 443);
//		getHttpClient().getHostConfiguration().setHost("arep2.aoc.arkansas.gov", 443, authhttps);
			
		// get the home page
		String link = getSiteLink();
		HTTPRequest req = new HTTPRequest(link);
		
		logger.info("Trying ARGenericCourtConnectCO: " + link);
		String resp = execute(req);
		logger.info("SUCCESS ARGenericCourtConnectCO: " + link);
		if (StringUtils.isNotEmpty(resp)){
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
				NodeList nl = htmlParser.parse(new TagNameFilter("frameset"));
				if (nl != null){
					if (nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "Big"), true).elementAt(0) != null){
						FrameTag frameSRC = (FrameTag) nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "Big"), true).elementAt(0);
						String nextLinkToFollow = frameSRC.getAttribute("src").toString();
						String newLink = link.substring(0, link.indexOf("ck_public"));
						
						logger.info("Trying ARGenericCourtConnectCO: " + newLink + nextLinkToFollow);
						
						req = new HTTPRequest(newLink + nextLinkToFollow);
						resp = execute(req);
						
						logger.info("SUCCESS ARGenericCourtConnectCO: " + newLink + nextLinkToFollow);
						
						if (StringUtils.isNotEmpty(resp)){
						//person name search
							htmlParser = org.htmlparser.Parser.createParser(resp, null);
							nl = htmlParser.parse(new TagNameFilter("table"));
							if (nl != null){
								TableTag tableTag = (TableTag) nl.elementAt(0);
								
								if (tableTag != null){
									TableRow[] rows = tableTag.getRows();
									for (TableRow row:rows){
										if (row.getColumnCount() > 1){
											if (row.getColumns()[1].toHtml().contains("party")){
												LinkTag linkTag = (LinkTag)row.getColumns()[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
												nextLinkToFollow = linkTag.extractLink().trim();
												
												req = new HTTPRequest(newLink + nextLinkToFollow);
												resp = execute(req);
												
												if (StringUtils.isNotEmpty(resp)){
													htmlParser = org.htmlparser.Parser.createParser(resp, null);
													nl = htmlParser.parse(new TagNameFilter("frameset"));
													
													if (nl != null){
														if (nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "Action"), true).elementAt(0) != null){
															frameSRC = (FrameTag) nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "Action"), true).elementAt(0);
															nextLinkToFollow = frameSRC.getAttribute("src").toString();
															
															req = new HTTPRequest(newLink + nextLinkToFollow);
															resp = execute(req);
															
															if (StringUtils.isNotEmpty(resp)){
																htmlParser = org.htmlparser.Parser.createParser(resp, null);
																nl = htmlParser.parse(new TagNameFilter("form"));
																if (nl != null){
																	for (int i = 0; i < nl.size(); i++){
																		FormTag form = (FormTag) nl.extractAllNodesThatMatch(new TagNameFilter("form"), true).elementAt(0);
																		if (form != null){
																			if (form.toHtml().contains("Accept")){//ck_public_qry_cpty.cp_personcase_setup_idx
																				nextLinkToFollow = form.getAttribute("action");
																				req = new HTTPRequest(newLink + nextLinkToFollow, HTTPRequest.POST);
																				req.setHeader("Cookie", "disclaimer=Y");
																				this.cookie = "disclaimer=Y";
																				resp = execute(req);
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Error while logging in", e);
				return new LoginResponse(LoginStatus.STATUS_UNKNOWN, e.getMessage());
			}
		}
		
				
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		req.setHeader("Cookie", this.cookie);
		req.setURL(req.getURL().replaceAll("(?is)&parentSite=true", ""));
		
		String resp = execute(req);
		if (StringUtils.isNotEmpty(resp)){
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
				NodeList nl = htmlParser.parse(new TagNameFilter("frameset"));
				if (nl != null){
					if (nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "Big"), true).elementAt(0) != null){
						FrameTag frameSRC = (FrameTag) nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "Big"), true).elementAt(0);
						String nextLinkToFollow = frameSRC.getAttribute("src").toString();
						nextLinkToFollow = nextLinkToFollow.replaceAll("(?is)\n", "");
						String newLink = req.getURL().substring(0, req.getURL().indexOf("ck_public"));
						req = new HTTPRequest(newLink + nextLinkToFollow);
						req.setHeader("Cookie", this.cookie);
						//resp = execute(req);
					} /*else if (nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "main"), true).elementAt(0) != null){
						FrameTag frameSRC = (FrameTag) nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "main"), true).elementAt(0);
						String nextLinkToFollow = frameSRC.getAttribute("src").toString();
						nextLinkToFollow = nextLinkToFollow.replaceAll("(?is)\n", "");
						String newLink = req.getURL().substring(0, req.getURL().indexOf("ck_public"));
						req = new HTTPRequest(newLink + nextLinkToFollow);
						req.setHeader("Cookie", this.cookie);
						resp = execute(req);
					}*/
				}
			} catch (Exception e) {
				
			}
		}
		
	}
	
	public byte[] getImage(String link){
				
		HTTPRequest req = new HTTPRequest(link);
		//String res = execute(req);
		
		return process(req).getResponseAsByte();
	}
	
	@Override
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.16) Gecko/20110319 Firefox/3.6.16 ( .NET CLR 3.5.30729; .NET4.0C)";
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

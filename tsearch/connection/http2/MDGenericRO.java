package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.io.IOUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class MDGenericRO extends HttpSite {
	
	private static Map<String, String> COUNTY_CODES = new HashMap<String, String>();
	static {
		COUNTY_CODES.put("ALLEGANY", "AL");
		COUNTY_CODES.put("ANNE ARUNDEL", "AA");
		COUNTY_CODES.put("BALTIMORE", "BA");
		COUNTY_CODES.put("BALTIMORE CITY", "BC");
		COUNTY_CODES.put("CALVERT", "CV");
		COUNTY_CODES.put("CAROLINE", "CA");
		COUNTY_CODES.put("CARROLL", "CR");
		COUNTY_CODES.put("CECIL", "CE");
		COUNTY_CODES.put("CHARLES", "CH");
		COUNTY_CODES.put("DORCHESTER", "DO");
		COUNTY_CODES.put("FREDERICK", "FR");
		COUNTY_CODES.put("GARRETT", "GA");
		COUNTY_CODES.put("HARFORD", "HA");
		COUNTY_CODES.put("HOWARD", "HO");
		COUNTY_CODES.put("KENT", "KE");
		COUNTY_CODES.put("MONTGOMERY", "MO");
		COUNTY_CODES.put("PRINCE GEORGE'S", "PG");
		COUNTY_CODES.put("QUEEN ANNE'S", "QA");
		COUNTY_CODES.put("SOMERSET", "SO");
		COUNTY_CODES.put("ST. MARY'S", "SM");
		COUNTY_CODES.put("TALBOT", "TA");
		COUNTY_CODES.put("WASHINGTON", "WA");
		COUNTY_CODES.put("WICOMICO", "WI");
		COUNTY_CODES.put("WORCESTER", "WO");
	}
	
	private static final String CFID_PARAM = "CFID";
	private static final String CFTOKEN_PARAM = "CFTOKEN";
	private static final String COUNTY_PARAM = "county";
	private static final String DO_NOT_ADD_IN_SESSIONS_PARAM = "doNotAddInSessions";
	
	private final static String BOOK_PARAM = "bk";
	private final static String PAGE_PARAM = "sp";
	private final static String IND_PARTYPE_PARAM = "Ind_PARTYPE";
	private final static String SORT_PARAM = "sort";
	private final static String SEARCH_RETURN_TYPE_PARAM = "Search_Return_Type";
	private final static String FORMNO_PARAM = "FormNo";
	private final static String A1_PARAM = "A1";
	
	private final static String IND_PARTYPE_VALUE = "B";
	private final static String SORT_VALUE = "book/page";
	private final static String SEARCH_RETURN_TYPE_VALUE = "Instrument";
	private final static String FORMNO_VALUE = "2";
	private final static String A1_VALUE = "Search";
	
	private static final String CONTROL_TEXT = "Begin your search of county land records by selecting a county from list at left";
	
	private String cfid = "";
	private String cftoken = "";
	
	private static Map<Long, Map<String, String>> sessions;
	private static List<Long> doNotAddinSessions;
	
	private static final Object LOCK = new Object();
	
	private String certificationDateHtml = "";
	
	public static final String PDF_LINK_PATT = "(?is)<iframe[^>]+src=\"([^\"]+\\.pdf)\"[^>]*>";
	
	@Override
	public void performSpecificActionsAfterCreation() {
		if(sessions == null) {
			sessions = new HashMap<Long, Map<String, String>>();
		}
		if(doNotAddinSessions == null) {
			doNotAddinSessions = new ArrayList<Long>();
		}
	}
	
	@Override
	public LoginResponse onLogin() {
		
		//get user name and password from database
		String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "MDGenericLandRecRO", "user");
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "MDGenericLandRecRO", "password");
		
		if(StringUtils.isEmpty(user) || StringUtils.isEmpty(password)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't find user / password in database");
		}
		
		setAttribute(HttpSite.SINGLE_LINE_COOKIES_IN_REDIRECT, Boolean.TRUE);
		
		String url = "";
		String resp1 = "";
		HTTPRequest req1 = new HTTPRequest(getSiteLink(), HTTPRequest.GET);		//go to first page
		try {
			HTTPResponse resp = exec(req1);
			URI uri = resp.getLastURI();
			if (uri!=null) {
				url = uri.getURI();
				url = url.replaceFirst("\\?.*", "");
			}
			resp1 = resp.getResponseAsString();
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		cfid = getParamValue(CFID_PARAM, resp1);
		cftoken = getParamValue(CFTOKEN_PARAM, resp1);
		
		if(StringUtils.isEmpty(cfid) || StringUtils.isEmpty(cftoken)) {
			return LoginResponse.getDefaultFailureResponse();
		}
		
		url += "?" + CFID_PARAM + "=" + cfid + "&" + CFTOKEN_PARAM + "=" + cftoken;
		
		//if not already logged in
		if (!resp1.contains(CONTROL_TEXT)) {
			String resp2 = "";
			HTTPRequest req2 = new HTTPRequest(url, HTTPRequest.POST);		//enter user and password
			req2.setPostParameter("UserLogin", user);
			req2.setPostParameter("UserPassword", password);
			req2.setPostParameter("IPUser", "False");
			req2.setPostParameter("login", "Login");
			req2.setPostParameter("MultipleSessions", "True");
			try {
				resp2 = exec(req2).getResponseAsString();
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
			if (!resp2.contains(CONTROL_TEXT)) {
				return LoginResponse.getDefaultFailureResponse();
			}
		}
		
		String url3 = url.replaceFirst("index.cfm", "dsp_county.cfm") + "&" + COUNTY_PARAM + "=" + getCounty();
		HTTPRequest req3 = new HTTPRequest(url3);		//select county
		try {
			certificationDateHtml = exec(req3).getResponseAsString();
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		this.status = STATUS_LOGGED_IN;
		
		return LoginResponse.getDefaultSuccessResponse();
	}
	
	public static String getParamValue(String name, String page) {
		String result = "";
		Matcher ma = Pattern.compile("(?is)\\b" + name + "=(.+?)[&\"']").matcher(page);
		if (ma.find()) {
			result = ma.group(1);
		}
		return result;
	}
	
	protected String getCounty() {
		DataSite dataSite = getDataSite();
		String county =  dataSite.getCountyName().toUpperCase();
		String res = "";
		res = COUNTY_CODES.get(county);
		if (res!=null) {
			return res;
		}
		return "";
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		if (req.getMethod()==HTTPRequest.POST) {
			String url = req.getURL();
			if (url.contains("act_setform.cfm") || url.contains("act_bookjump.cfm")) {
				String doNotAddinSessionsValue = req.getPostFirstParameter(DO_NOT_ADD_IN_SESSIONS_PARAM);
				if ("true".equals(doNotAddinSessionsValue)) {
					doNotAddinSessions.add(new Long(getSearchId()));
					req.removePostParameters(DO_NOT_ADD_IN_SESSIONS_PARAM);
				}
				
				url = url.replaceFirst("\\?.*", "");
				url += "?" + CFID_PARAM + "=" + cfid + "&" + CFTOKEN_PARAM + "=" + cftoken;
				req.modifyURL(url);
			}
		} else if (req.getMethod()==HTTPRequest.GET) {
			String url = req.getURL();
			url = url.replaceFirst("(?is)(www\\.mdlandrec\\.net)(/dsp_(?:fullcitation|book)\\.cfm)", "$1" + "/msa/stagser/s1700/s1741/cfm" + "$2");
			url = url.replaceFirst("(?is)(www\\.mdlandrec\\.net)(/act_captureimage.cfm)", "$1" + "/msa/stagser/s1700/s1741/cfm" + "$2");
			url = url + "&";
			url = url.replaceFirst("(?is)\\b(CFID=)[^&]+", "$1" + cfid);
			url = url.replaceFirst("(?is)\\b(CFTOKEN=)[^&]+", "$1" + cftoken);
			url = url.replaceFirst("&$", "");
			req.modifyURL(url);
		}
	}
	
	public String getCertificationDateHtml() {
		if (this.status != STATUS_LOGGED_IN) {
			onLogin();
		}
		return certificationDateHtml;
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		
		String stringResponse = res.getResponseAsString();
				
		if (req.getURL().contains("act_setform.cfm") && stringResponse.contains("The web site you are accessing has experienced an unexpected error")) {
			res.is = IOUtils.toInputStream("<html>The web site you are accessing has experienced an unexpected error</html>");
			res.body = "<html>The web site you are accessing has experienced an unexpected error</html>";
			res.contentLenght = res.body.length();
			res.returnCode = 200;
			return;
		}
				
		boolean sessionExpired = false;
		if (stringResponse.contains("Forget your password?") || stringResponse.contains("The web site you are accessing has experienced an unexpected error")) {
			sessionExpired = true;
		} else {
				
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(stringResponse, null);
				NodeList nodeList = htmlParser.parse(null);
				Node node = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "class", "searchresults", true);
				if (node!=null && node instanceof TableTag) {
					TableTag table = (TableTag)node;
					if (table.getRowCount()<5) {
						sessionExpired = true;
					}
				}
			} catch (ParserException e) {
				e.printStackTrace();
			}
			
			if (!sessionExpired) {
				stringResponse = stringResponse.replaceAll("(?s)<!--.*?-->", "");
				stringResponse = stringResponse.replaceAll("(?is)<script[^>]*>.*?</script>", "");
				if (stringResponse.trim().length()==0) {
					sessionExpired = true;
				}
			}
			
		}
		if (sessionExpired) {
			synchronized(LOCK) {
				destroySession();
				onLogin();
				setDestroy(false);
				Map<String, String> par = sessions.get(new Long(getSearchId()));
				if (par!=null) {
					String reqString = req.toString();
					int nr = 0;
					String nrString = par.get(reqString);
					if (nrString!=null && nrString.matches("\\d+")) {
						nr = Integer.parseInt(nrString);
					}
					if (nr<5) {
						par.put(reqString, Integer.toString(nr+1));
					} else {
						res.is = IOUtils.toInputStream("<html>The web site you are accessing has experienced an unexpected error</html>");
						res.body = "<html>The web site you are accessing has experienced an unexpected error</html>";
						res.contentLenght = res.body.length();
						res.returnCode = 200;
						return;
					}
							
					String url = par.get("url");
					if (!StringUtils.isEmpty(url)) {
						url += "&";
						url = url.replaceFirst("(?is)\\b(CFID=).+?(&)", "$1" + cfid + "$2");
						url = url.replaceFirst("(?is)\\b(CFTOKEN=).+?(&)", "$1" + cftoken + "$2");
						url = url.replaceFirst("&*$", "");
						HTTPRequest req1 = new HTTPRequest(url, HTTPRequest.POST);
						for (String p: par.keySet()) {
							if (!"url".equals(p)) {
								req1.setPostParameter(p, par.get(p));
							}
						}
						try {
							exec(req1).getResponseAsString();
						} catch(IOException e){
							logger.error(e);
							throw new RuntimeException(e);
						}
					}
				}
				HTTPResponse resp = process(req);
				res.is = new ByteArrayInputStream(resp.getResponseAsByte());
				res.contentLenght= resp.contentLenght;
				res.contentType = resp.contentType;
				res.headers = resp.headers;
				res.returnCode = resp.returnCode;
				res.body = resp.body;
				res.setLastURI(resp.getLastURI());
			}
		} else {
			if (req.getMethod()==HTTPRequest.POST) {
				String url = req.getURL();
				if (url.contains("act_setform.cfm")) {
					Long longSeachId = new Long(getSearchId());
					if (!doNotAddinSessions.contains(longSeachId)) {
						HashMap<String,HTTPRequest.ParametersVector> params = req.getPostParameters();
						 Map<String, String> par = new HashMap<String, String>();
						 par.put("url", req.getURL());
						 for (String p: params.keySet()) {
							 ParametersVector pv = params.get(p);
							 par.put(p, pv.getFirstParameter());
						 }
						 sessions.put(longSeachId, par);
					} else {
						doNotAddinSessions.remove(longSeachId);
					}
				}
			}
			res.is = new ByteArrayInputStream(stringResponse.getBytes());
		}
	}
	
	public String searchByBookPage(String book, String page) {
		if (!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)) {
			String resp = "";
			HTTPRequest req = new HTTPRequest(getSiteLink() + "/msa/stagser/s1700/s1741/cfm/act_setform.cfm", HTTPRequest.POST);
			req.setPostParameter(BOOK_PARAM, book);
			req.setPostParameter(PAGE_PARAM, page);
			req.setPostParameter(IND_PARTYPE_PARAM, IND_PARTYPE_VALUE);
			req.setPostParameter(SORT_PARAM, SORT_VALUE);
			req.setPostParameter(SEARCH_RETURN_TYPE_PARAM, SEARCH_RETURN_TYPE_VALUE);
			req.setPostParameter(FORMNO_PARAM, FORMNO_VALUE);
			req.setPostParameter(A1_PARAM, A1_VALUE);
			req.setPostParameter(DO_NOT_ADD_IN_SESSIONS_PARAM, "true");
			resp = execute(req);
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
			try {
				NodeList nodeList = htmlParser.parse(null);
				Node node = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "class", "searchresults", true);
				if (node!=null) {
					HashSet<String> setImageLink = new HashSet<String>();
					HashSet<String> setDetailsLink = new HashSet<String>();
					setImageLink.addAll(RegExUtils.getMatches(ro.cst.tsearch.servers.types.MDGenericRO.BOOK_PAGE_PATT1, node.toHtml(), 1));
					setDetailsLink.addAll(RegExUtils.getMatches(ro.cst.tsearch.servers.types.MDGenericRO.DETAILS_PAGE_PATT, node.toHtml(), 1));
					if (setImageLink.size()==1) {
						if (setDetailsLink.size()>0) {
							Iterator<String> it = setDetailsLink.iterator();
							if (it.hasNext()) {
								return it.next();
							}
						}
					}
				}
			} catch (ParserException e) {
				e.printStackTrace();
			}
			
		}
		return "";
	}
	
	public byte[] getImage(String link, String fileName, boolean isDirectJump) {
		
		String pdfLink = "";
		String resp1 = "";
		
		if (isDirectJump) {
			String s = link;
			link = link.replaceFirst("\\?.*", "");
			link += "?" + CFID_PARAM + "=" +  cfid + "&" + CFTOKEN_PARAM + "=" + cftoken;
			HTTPRequest req1 = new HTTPRequest(getSiteLink() + link, HTTPRequest.POST);
			s = s.replaceFirst(".*?\\?", "");
			String[] split = s.split("&");
			for (int i=0;i<split.length;i++) {
				String[] spl = split[i].split("=");
				if (spl.length==2) {
					if (!CFID_PARAM.equals(spl[0]) && !CFTOKEN_PARAM.equals(spl[0])) {
						req1.setPostParameter(spl[0], spl[1]);
					}
				} else if (spl.length==1) {
					if (!CFID_PARAM.equals(spl[0]) && !CFTOKEN_PARAM.equals(spl[0])) {
						req1.setPostParameter(spl[0], "");
					}
				}
			}
			resp1 = execute(req1);
		} else {
			link = link.replaceFirst("(?is).*?&Link=", "");
			HTTPRequest req1 = new HTTPRequest(getSiteLink() + "/" + link, HTTPRequest.GET);
			resp1 = execute(req1);
		}
		
		//table with images, select the one in the interval corresponding to the recorded year                    
		if (resp1.contains("Please select the book you wish to view from the listing below")) {
			String yr = StringUtils.extractParameter(link, "yr=([^&?]*)");
			if (yr.matches("\\d{4}")) {
				int year = Integer.parseInt(yr);
				try {
					resp1 = resp1.replaceAll("(?is)(</?)th>", "$1td>");
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp1, null);
					NodeList nodeList = htmlParser.parse(null);
					Node node = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "width", "75%", true);
					if (node!=null && node instanceof TableTag) {
						TableTag table = (TableTag)node;
						if (table.getRowCount()>1) {
							TableRow header = table.getRow(0);
							int datesIndex = -1;
							int linkIndex = -1;
							for (int i=0;i<header.getColumnCount();i++) {
								if ("Dates".equals(header.getColumns()[i].toPlainTextString().trim())) {
									datesIndex = i;
								} else if ("Volume".equals(header.getColumns()[i].toPlainTextString().trim())) {
									linkIndex = i;
								}
							}
							if (datesIndex>-1 && linkIndex>-1) {
								for (int i=1;i<table.getRowCount();i++) {
									TableRow row = table.getRow(i);
									if (row.getColumnCount()>datesIndex && row.getColumnCount()>linkIndex) {
										String dates = row.getColumns()[datesIndex].toPlainTextString().trim();
										int year1 = -1;
										int year2 = -1;
										if (dates.matches("\\d{4}")) {
											year1 = year2 = Integer.parseInt(dates);
										} else {
											Matcher ma = Pattern.compile("(\\d{4})-(\\d{4})").matcher(dates);
											if (ma.matches()) {
												year1 = Integer.parseInt(ma.group(1));
												year2 = Integer.parseInt(ma.group(1));
											}
										}
										if (year1>-1 && year2>-1) {
											if (year1<=year && year<=year2) {
												String volume = row.getColumns()[linkIndex].toHtml().trim();
												Matcher ma = Pattern.compile("(?is)\\bhref=\"([^\"]+)\"").matcher(volume);
												if (ma.find()) {
													HTTPRequest req1 = new HTTPRequest(getSiteLink() + "/" + ma.group(1), HTTPRequest.GET);
													resp1 = execute(req1);
												}
												break;
											}
										}
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
		
		Matcher matcher = Pattern.compile(PDF_LINK_PATT).matcher(resp1);
		if (matcher.find()) {
			pdfLink = matcher.group(1);
		}
		
		if (!StringUtils.isEmpty(pdfLink)) {
			HTTPRequest req2 = new HTTPRequest(getSiteLink() + pdfLink, HTTPRequest.GET);
			try {
				HTTPResponse resp2 = exec(req2);
				return resp2.getResponseAsByte();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp1, null);
				NodeList nodeList = htmlParser.parse(null);
				Node node = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "width", "70%", true);
				if (node!=null && node instanceof TableTag) {
					return new byte[]{-1};
				}
			} catch (ParserException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}

}

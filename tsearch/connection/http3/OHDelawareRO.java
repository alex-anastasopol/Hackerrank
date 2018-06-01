package ro.cst.tsearch.connection.http3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.AppletTag;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TiffConcatenator;


public class OHDelawareRO extends HttpSite3 {

	public static final String[] ADD_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__VIEWSTATEENCRYPTED"};
	public static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "ctl00_smScriptMan_HiddenField" };
	
	public static final Pattern FORM_PATTERN = Pattern.compile("<form(.*?)name=\"formQuery\"\\s+action=\"([^\"]+)\"\\s*>");
	
	private String formID = "aspnetForm";
	
	private static String searchByParcelNo		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchParcel$btnSearch";
	private static String searchByInstrumentNo	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchInstNo$btnSearch";
	private static String searchByQuickName 	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchNames$btnInstruments";
	private static String searchByAdvName	 	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$btnInstruments";
	private static String searchByBookPage	 	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchBkPg$btnSearch";
	private static String searchByProperty 	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$btnInstruments";
	private static String nextActionLink		= "ctl00$cphMain$upMain|ctl00$cphMain$tcMain$tpInstruments$ucInstrumentsGridV2$cpInstruments_Top$ibResultsNextPage";
	private static String prevActionLink		= "ctl00$cphMain$upMain|ctl00$cphMain$tcMain$tpInstruments$ucInstrumentsGridV2$cpInstruments_Top$ibResultsPreviousPage";
	private static String ajax_HiddenField		= "";
	
	//InstrNo search params
	private static String propTypeParamInstrSrch	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchInstNo$ddlIndexType";
	//Book and Pag search params
	private static String propTypeParamBookPageSrch	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchBkPg$ddlIndexType";
	//Quick Name search params
//	private static String propTypeParamQuickNameSrch	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchInstNo$ddlIndexType";
	
	
	//Property search params
	private static String propTypeParamPropSrch	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$ddlPropertyType";
	private static String sortingParamPropSrch		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$ddlSortDir";
//	private static String indexTypeOffPropSrch 	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$repIndexTypes$ctl00$cbIndexType";
//	private static String indexTypeUccPropSrch 	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$repIndexTypes$ctl02$cbIndexType";
	private static String unitParamPropSrch 		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtUnit";
	private static String blkParamPropSrch 		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtBlock";
	private static String lotParamPropSrch 		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtLot";
	private static String sectionParamPropSrch 	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtGMDSect";
	private static String districtParamPropSrch 	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtDistrict";
	private static String LLotParamPropSrch 		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtLLot";
	private static String secPropSrch 				= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtSection";
	private static String twnPropSrch 				= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtTownship";
	private static String rngPropSrch 				= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtRange";
	private static String unplatedLotParamPropSrch	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtUnplattedLot";
	private static String sortPropSrch 			= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$Sort";
	private static String sortDirPropSrch 			= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$ddlSortDir";
	private static String propSearchTypePropSrch 	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$PropSearchType";
	private static String subClientStTypePropSrch 	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$lseSubdivisions_ClientState";
	private static String lastNamePropSrch			= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtFirmSurName";
	private static String firstNamePropSrch		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtGivenName";
	private static String middleNamePropSrch		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtGivenName2";
	private static String lastWildcardPropSrch		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$ddlWildcardLast";
	private static String firstWildcardPropSrch	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$ddlWildcardFirst";
	private static String fromDatePropSrch			= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtFiledFrom";
	private static String thruDatePropSrch			= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchProperty$txtFiledThru";
	//Adv search params
	private static String propTypeParamAdvSrch		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$ddlPropertyType";
	private static String indexTypeOffAdvSrch 		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$repIndexTypes$ctl00$cbIndexType";
	private static String indexTypeUccAdvSrch 		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$repIndexTypes$ctl02$cbIndexType";
	private static String unitParamAdvSrch 		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$txtUnit";
	private static String blkParamAdvSrch 			= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$txtBlock";
	private static String lotParamAdvSrch 			= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$txtLot";
	private static String sectionParamAdvSrch 		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$txtGMDSect";
	private static String districtParamAdvSrch 	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$txtDistrict";
	private static String LLotParamAdvSrch 		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$txtLLot";
	private static String secParamAdvSrch 			= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$txtSection";
	private static String twnParamAdvSrch 			= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$txtTownship";
	private static String rngParamAdvSrch 			= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$txtRange";
	private static String unplatedLotParamAdvSrch	= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$txtUnplattedLot";
	
	
	public static String[] getTargetArgumentParameters() {
		return REQ_PARAM_NAMES;
	}
	
	public String getPage(String link, Map<String, String> params) {
		HTTPRequest req = null;
		if (params == null){
			req = new HTTPRequest(link, HTTPRequest.GET);
		} else {
			req = new HTTPRequest(link, HTTPRequest.POST);
			Iterator<String> i = params.keySet().iterator();
			while(i.hasNext()){
				String k = i.next();
				req.setPostParameter(k, params.get(k));
			}
		}
		
		return super.execute(req);
	}
	
	public String getNextDetailsPage(String link, Map<String, String> params) {
		HTTPRequest req = null;
		req = new HTTPRequest(link, HTTPRequest.POST);
		Iterator<String> i = params.keySet().iterator();
		while(i.hasNext()){
			String k = i.next();
			req.setPostParameter(k, params.get(k));
		}
		
		return super.execute(req);
	}
	
	public static Map<String, String> isolateParams(String page, String form){
		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		
		return addParams;
	}
	
	public static Map<String, String> isolateParams(String page, String form, String[] paramsSet){
		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : paramsSet) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		
		return addParams;
	}
	

	public String getHiddenParam(NodeList nodeList)  {
		String hiddenVal = "";
		String hiddenKey = "";
		
		if (nodeList!= null && nodeList.size() > 0) {
			for (int i=0; i< nodeList.size(); i++) {
				ScriptTag script = (ScriptTag) nodeList.elementAt(i);
				if (script.getAttribute("src") != null) {
					Pattern p = Pattern.compile("(?is)[^?]+\\?_TSM_HiddenField_=([^&]+)[^_]+_TSM_CombinedScripts_=([^\"]+)");
					Matcher m = p.matcher(script.getAttribute("src"));
				
					if (script != null && m.find()) {
						hiddenKey = m.group(1);
						if ("ctl00_smScriptMan_HiddenField".equals(hiddenKey)) {
							hiddenVal = m.group(2);
							try {
								hiddenVal = URLDecoder.decode(hiddenVal, "UTF-8");
							} catch (UnsupportedEncodingException e) {							
								e.printStackTrace();
								logger.trace(e);
							}
							break;
						}
					}
				}
			}
		}
		
		return hiddenVal;
	}

	
	public NodeList getScriptNodes (String response, String formNameOrId) {
		NodeList scripts = new HtmlParser3(response).getNodeList();
		scripts = scripts.extractAllNodesThatMatch(new TagNameFilter("form"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", formNameOrId))
				.extractAllNodesThatMatch(new TagNameFilter("script"), true);
		
		return scripts;
	}
	
	
	private String viewImageFromRO(String link, String instrNo) {
		String response = "";
		
		response = getParamsFromLoginPageAgain(link, instrNo);
		return response;
	}
	
	public byte[] getImageFromRO(String link) {
		if (StringUtils.isNotEmpty(link)) {
			//link in this format: "http://cotthosting.com/ohdelaware/LandRecords/protected/DocumentDetails.aspx?instrNo=" + instrument
			int idx = link.lastIndexOf("?");
			String response = viewImageFromRO(link.substring(0,idx), link.substring(idx+9));
			try {
				return downloadImage(response);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		return null;
	}
	
	
	
	public byte[] getImage(String link) {
		String response = viewImageFromTSRI(link);
		try {
			return downloadImage(response);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private String viewImageFromTSRI(String link) {
		String response = "";
		String instrNo = "";
		link = link.replaceAll(".*?Link=", "");
		link = link.replaceAll("(?is)&amp;", "&");
		
		HTTPRequest req = null;
		
		String[] split1 = link.split("\\?");
		if (split1.length==2) {
			link = split1[0];
			String split2[] = split1[1].split("&");
			link = link.replaceFirst("/ohdelaware", "");
			link = getSiteLink() + link;
			
			req = new HTTPRequest(link, HTTPRequest.POST);
			
			for (int i=0; i<split2.length; i++) {
				String param = split2[i];
				param = param.replace("\\$", "$");
				int index = param.indexOf("=");
				if (index>-1) {
					String name = param.substring(0, index);
					if (!"seq".equals(name)) {
						String value = param.substring(index+1);
						if ("InstrumentNo".equals(name)) {
							instrNo = value;
							if (instrNo.length() == 12) {
								instrNo = instrNo.substring(0, 4) + "-" + instrNo.substring(4);
							}
						} else {
							req.setPostParameter(name, value);
						}
					}
				}
			}
		}
		
		//we must be sure that session didn't expired
		//Map<String,String> paramsForReq = getParamsFromLoginPageAgain(link, instrNo);
		response = getParamsFromLoginPageAgain(link, instrNo);
		
		
		return response;
	}
	
//	private byte[] getImageFromResponse(String response) {
//		try {
//			byte[] completeImage = null;
//			HtmlParser3 htmlParser = new HtmlParser3(response);
//			
//			if (htmlParser != null) {
//				LinkedList<String> pageIndex = new LinkedList<String>();
//				NodeList divList = htmlParser.getNodeList()
//						.extractAllNodesThatMatch(new TagNameFilter("div"), true)
//						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_divDaeja"), true);
//				
//				if (divList != null && divList.size() > 0) {
//					NodeList appletList = divList.extractAllNodesThatMatch(new TagNameFilter("applet"), true);
//					
//					if (appletList != null && appletList.size() > 0) {
//						AppletTag applet = (AppletTag) appletList.elementAt(0);
//
//						Hashtable<String, String> params = applet.getAppletParams();
//						if (params != null) {
//							Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
//							while (it.hasNext()) {
//								 Map.Entry<String, String> entry = it.next();
//								 String key = entry.getKey();
//								 if (key.matches("(?is)Page\\d+")){
//									pageIndex.add(key.replaceFirst("(?is)Page(\\d+)", "$1")) ;
//								 }
//							}
//						}
//						
//						Collections.sort(pageIndex);
//						List<byte[]> images = new LinkedList<byte[]>();
//						HTTPResponse imageResp = null;
//						for (String index : pageIndex) {
//							try {
//								HTTPRequest newReq = new HTTPRequest(params.get("Page" + index), HTTPRequest.GET);
//								newReq.setHeader("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
//								newReq.setHeader("User-Agent", "Mozilla/4.0 (Windows 7 6.1) Java/1.6.0_33");
//								imageResp = exec(newReq);
//								images.add(imageResp.getResponseAsByte());
//								
//							} catch (IOException e) {
//								e.printStackTrace();
//							}
//						}
//						if (images.size() > 0){
//							completeImage = TiffConcatenator.concatePngInTiff(images);
//						}
//					}
//				}					
//			}
//			
////			byte[] completeImage = TiffConcatenator.concateTiff(pages);
//			return completeImage;
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		return null;
//	}
	
	public String getParamsFromLoginPageAgain(String url, String instrNo) {
//		  01. login
		HTTPRequest request = new HTTPRequest(getSiteLink());
		request.setHeader("User-Agent", getUserAgentValue());
		request.setHeader("Accept", getAccept());
		request.setHeader("Accept-Language", getAcceptLanguage());
		request.setHeader("Accept-Encoding", "gzip, deflate");
		
		String responseAsString = execute(request);
		String redirectURL = request.getRedirectLocation();
		//this is for when DTG is trying to get the image from RO and the redirectLocation link is incomplete
		//this is for avoid exception like: java.lang.IllegalStateException: Target host must not be null, or set in parameters.
		if (redirectURL.startsWith("/ohdelaware/")){
			redirectURL = redirectURL.replaceFirst("/ohdelaware/", getSiteLink() + "/");
		}
		Pattern titlePattern = Pattern.compile("(?is)<title>(.*)</title>");
		
		if(responseAsString == null) {
			return null;
		}
		
		Matcher matcher = titlePattern.matcher(responseAsString);
		Map<String,String> addParams = null;
		Map<String,String> cookie = new HashMap<String, String>();
		
		if(matcher.find()) {
			String title = matcher.group(1);
			if(title.contains("eSearch") && title.contains("Account Sign In")) {
				//isolate parameters
				addParams = isolateParams(responseAsString, "aspnetForm", ADD_PARAM_NAMES);
			} else {
				return null;
			}
			
		} else {
			return null;
		}
		
//		  02. get page with default search module: /LandRecords/protected/v4/SrchName.aspx
		request.modifyURL(redirectURL);
		request.setMethod(HTTPRequest.POST);
		
		NodeList scripts = getScriptNodes(responseAsString, formID);
		if (scripts != null) {
			ajax_HiddenField = getHiddenParam(scripts);
		}
		request.setPostParameter("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
		request.addPostParameters(addParams);
		request.setPostParameter("ctl00$cphMain$blkLogin$txtUsername", "");
		request.setPostParameter("ctl00$cphMain$blkLogin$txtPassword", "");
		request.setPostParameter("ctl00$cphMain$blkLogin$btnGuestLogin", "Sign in as a Guest");
		
		responseAsString = execute(request);
		
		if(responseAsString == null) {
			return null;
		}
		
		if(responseAsString.contains("Indexed Records")  && responseAsString.contains("Name Search")) {
			addParams = isolateParams(responseAsString, formID, ADD_PARAM_NAMES);
			
			scripts = getScriptNodes(responseAsString, formID);
			if (scripts != null) {
				ajax_HiddenField = getHiddenParam(scripts);
			}
			addParams.put("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
			cookie.put(getCookies().get(0).getName(), getCookies().get(0).getValue());
			cookie.put(getCookies().get(1).getName(), getCookies().get(1).getValue());
			setAttribute("params", addParams);
			setAttribute("cookie", cookie);
		}
		
		
		String linkSearchByInstr = url;
		linkSearchByInstr = linkSearchByInstr.replaceFirst("(?is)(.*[^/]+/).*", "$1");
		linkSearchByInstr += "v4/SrchInstNo.aspx";
		
		HTTPRequest firstReq = new HTTPRequest(linkSearchByInstr, HTTPRequest.POST);
		firstReq.addPostParameters(addParams);
		firstReq.setPostParameter("ctl00$cphMain$tcMain$tpNewSearch$ucSrchInstNo$txtFileNumber", instrNo);
		firstReq.setPostParameter("ctl00$cphMain$tcMain$tpNewSearch$ucSrchInstNo$ddlIndexType", "ALL");
		firstReq.setPostParameter("ctl00$cphMain$tcMain$tpNewSearch$ucSrchInstNo$meeFileNumber_ClientState", "");
		firstReq.setPostParameter("ctl00$cphMain$tcMain$tpInstruments$ucInstrumentsGridV2$cpInstruments_Top$ddlResultsPerPage", "50");
		firstReq.setPostParameter(searchByInstrumentNo, "Search");
		
		responseAsString = execute(firstReq);
		
		HtmlParser3 parser = new HtmlParser3(responseAsString);
		NodeList results = parser.getNodeList();
		
		scripts = getScriptNodes(responseAsString, formID);
		if (scripts != null) {
			ajax_HiddenField = getHiddenParam(scripts);
		}
		
		if (results.size() > 0) {
			TableTag table = (TableTag) results.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_tcMain_tpInstruments_ucInstrumentsGridV2_cpgvInstruments"), true)
					.elementAt(0);
			if (table != null) {
				TableRow[] rows = table.getRows();
				if (rows.length > 1) {
					for (int i=1; i < rows.length; i++) {
						TableColumn col = rows[i].getColumns()[7];
						LinkTag info = (LinkTag) col.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
						
						if (info != null) {
							if (info.getLinkText().trim().equals(instrNo) || info.getLinkText().trim().equals(instrNo.substring(0, 4) + "-" + instrNo.substring(4))) {
								String detPgPattern = "(?is)javascript:WebForm_DoPostBackWithOptions\\([^\\(]+\\(&quot;([^&]+)&quot;[^\\)]+\\)\\)";
								String link = info.extractLink();
								String imgId = RegExUtils.getFirstMatch(detPgPattern, link, 1);
								if (StringUtils.isNotEmpty(imgId)) {
									addParams.put("ctl00$smScriptMan", "ctl00$cphMain$upMain|" + imgId);
									addParams.remove("__EVENTTARGET");
									addParams.put("__EVENTTARGET", imgId);
									addParams.remove("ctl00_smScriptMan_HiddenField");
									addParams.put("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
									
									request = new HTTPRequest(linkSearchByInstr, HTTPRequest.POST);
									request.addPostParameters(addParams);
									responseAsString = execute(request);
								}
							}
						}
					}
				}
				
			}
		}
		
		parser =  new HtmlParser3(responseAsString);
		results = parser.getNodeList();
		scripts = getScriptNodes(responseAsString, formID);
		if (scripts != null) {
			ajax_HiddenField = getHiddenParam(scripts);
		}
		
		if (results.size() > 0) {
			String actionLink = "";
			NodeList formList = results.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", formID));
			
			if (formList != null && formList.size() > 0){
				FormTag form = (FormTag) formList.elementAt(0);
				if (form != null){
					actionLink = form.getAttribute("action");
				}
			}
			addParams.remove("ctl00$smScriptMan");
			addParams.remove("ctl00_smScriptMan_HiddenField");
			addParams.put("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
			addParams.remove("__EVENTTARGET");
			addParams.put("__EVENTTARGET", "ctl00$cphMain$gvDetails1");
			addParams.remove("__EVENTARGUMENT");
			addParams.put("__EVENTARGUMENT", "Image$0");
			
			String link = url.substring(0, url.lastIndexOf("/")+1) + actionLink; 
			request = new HTTPRequest(link, HTTPRequest.POST);
			request.addPostParameters(addParams);
			
//			getImageFromReq(request);
			responseAsString = execute(request);
		}
		
		
		return responseAsString;
	}
	
	
	@Override
	public LoginResponse onLogin() {
//		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
//      Protocol.registerProtocol("https", easyhttps);
		/**
		 * 01. Get first page and check if we are on login page
		 * ---------------------------------------------------------------------------------------------------------
		 **/
		HTTPRequest request = new HTTPRequest(getSiteLink());
		request.setHeader("User-Agent", getUserAgentValue());
		request.setHeader("Accept", getAccept());
		request.setHeader("Accept-Language", getAcceptLanguage());
		request.setHeader("Accept-Encoding", "gzip, deflate");
		
		String responseAsString = execute(request);
		String redirectURL = request.getRedirectLocation();
		
		//this is for when DTG is trying to get the image from RO and the redirectLocation link is incomplete
		//this is for avoid exception like: java.lang.IllegalStateException: Target host must not be null, or set in parameters.
		if (redirectURL.startsWith("/ohdelaware/")){
			redirectURL = redirectURL.replaceFirst("/ohdelaware/", getSiteLink() + "/");
		}
		Pattern titlePattern = Pattern.compile("(?is)<title>(.*)</title>");
		
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for " + getSiteLink());
		}
		
		Matcher matcher = titlePattern.matcher(responseAsString);
		Map<String,String> addParams = null;
		Map<String,String> cookie = new HashMap<String, String>();
		
		if(matcher.find()) {
			String title = matcher.group(1);
			if(title.contains("eSearch") && title.contains("Account Sign In")) {
				//isolate parameters
				addParams = isolateParams(responseAsString, "aspnetForm", ADD_PARAM_NAMES);
			} else {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Unexpected page received with title " + title);
			}
			
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Unexpected page received with no title ");
		}
		
		/**
		 * 02. GET page with default search module: /LandRecords/protected/v4/SrchName.aspx
		 * ---------------------------------------------------------------------------------------------------------
		 **/
		request.modifyURL(redirectURL);
		request.setMethod(HTTPRequest.POST);
		
		NodeList scripts = getScriptNodes(responseAsString, formID);
		if (scripts != null) {
			ajax_HiddenField = getHiddenParam(scripts);
		}
		request.setPostParameter("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
		request.addPostParameters(addParams);
		request.setPostParameter("ctl00$cphMain$blkLogin$txtUsername", "");
		request.setPostParameter("ctl00$cphMain$blkLogin$txtPassword", "");
		request.setPostParameter("ctl00$cphMain$blkLogin$btnGuestLogin", "Sign in as a Guest");
		
		responseAsString = execute(request);
		
		if(responseAsString == null) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "No respose for SrchName.aspx");
		}
		
		if(responseAsString.contains("Indexed Records")  && responseAsString.contains("Name Search")) {
			addParams = isolateParams(responseAsString, formID, ADD_PARAM_NAMES);
//			// check parameters
//			if (!checkParams(addParams)){
//				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
//			}
			
			scripts = getScriptNodes(responseAsString, formID);
			if (scripts != null) {
				ajax_HiddenField = getHiddenParam(scripts);
			}
			addParams.put("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
			cookie.put(getCookies().get(0).getName(), getCookies().get(0).getValue());
			cookie.put(getCookies().get(1).getName(), getCookies().get(1).getValue());
			setAttribute("params", addParams);
			setAttribute("cookie", cookie);
			
			return LoginResponse.getDefaultSuccessResponse();
			
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not get login page!");
		}
	}
	 
	
	public Map<String,String> extractParamsStartingWith(HTTPRequest req, String prefixParamName) {
		Map<String,String> params = new HashMap<String,String>();
		HashMap<String,HTTPRequest.ParametersVector> paramsOfReq =  req.getPostParameters();
		
		if (req != null) {
			for(Map.Entry<String,HTTPRequest.ParametersVector> entry: paramsOfReq.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue().toString();
				if (key.startsWith(prefixParamName)) {
					params.put(key, val);
				} 
			}
			
		}
		
		return params;
	}
	
	public void removeParamsDependingOnPropertyType(HTTPRequest req, String propTypeVal) {
		boolean isPropSearch = false;
		boolean isAdvSearch = false;
		
		if (req.getPostFirstParameter(sortingParamPropSrch) != null) {
			isPropSearch = true;
		} else {
			isAdvSearch = true;
		}
		
		if ("S".equals(propTypeVal)) {
			if (isAdvSearch) {
				req.removePostParameters(secParamAdvSrch);
				req.removePostParameters(twnParamAdvSrch);
				req.removePostParameters(rngParamAdvSrch);
				req.removePostParameters(unplatedLotParamAdvSrch);
			} else if (isPropSearch) {
				req.removePostParameters(secPropSrch);
				req.removePostParameters(twnPropSrch);
				req.removePostParameters(rngPropSrch);
				req.removePostParameters(unplatedLotParamPropSrch);
			}
			
			
		} else if ("T".equals(propTypeVal)) {
			if (isAdvSearch) {
				req.removePostParameters(unitParamAdvSrch);
				req.removePostParameters(blkParamAdvSrch);
				req.removePostParameters(lotParamAdvSrch);
				req.removePostParameters(sectionParamAdvSrch);
				req.removePostParameters(districtParamAdvSrch);
				req.removePostParameters(LLotParamAdvSrch);
			} else if (isPropSearch) {
				req.removePostParameters(unitParamPropSrch);
				req.removePostParameters(blkParamPropSrch);
				req.removePostParameters(lotParamPropSrch);
				req.removePostParameters(sectionParamPropSrch);
				req.removePostParameters(districtParamPropSrch);
				req.removePostParameters(LLotParamPropSrch);
			}
			
			
		} else if ("-1".equals(propTypeVal) || "V".equals(propTypeVal)) { 
			//no extra params
			if (isAdvSearch) {
				req.removePostParameters(unitParamAdvSrch);
				req.removePostParameters(blkParamAdvSrch);
				req.removePostParameters(lotParamAdvSrch);
				req.removePostParameters(sectionParamAdvSrch);
				req.removePostParameters(districtParamAdvSrch);
				req.removePostParameters(LLotParamAdvSrch);
				req.removePostParameters(secParamAdvSrch);
				req.removePostParameters(twnParamAdvSrch);
				req.removePostParameters(rngParamAdvSrch);
				req.removePostParameters(unplatedLotParamAdvSrch);
			} else if (isPropSearch) {
				req.removePostParameters(unitParamPropSrch);
				req.removePostParameters(blkParamPropSrch);
				req.removePostParameters(lotParamPropSrch);
				req.removePostParameters(sectionParamPropSrch);
				req.removePostParameters(districtParamPropSrch);
				req.removePostParameters(LLotParamPropSrch);
				req.removePostParameters(secPropSrch);
				req.removePostParameters(twnPropSrch);
				req.removePostParameters(rngPropSrch);
				req.removePostParameters(unplatedLotParamPropSrch);
			}
			
		}
		
		return;
	}
	
	public void perfomParcelNoSearch(HTTPRequest req, Map<String,String> addParams, Map<String,String> cookies) {
		String resp = "";
		HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.GET);
		
		for(Map.Entry<String, String> entry: cookies.entrySet()) {
			req.setHeader(entry.getKey(), entry.getValue());
			req1.setHeader(entry.getKey(), entry.getValue());
		}
		resp = execute(req1);
		addParams = isolateParams(resp, formID, REQ_PARAM_NAMES);
		NodeList scripts = getScriptNodes(resp, formID);
		if (scripts != null) {
			ajax_HiddenField = getHiddenParam(scripts);
		}
		addParams.remove("ctl00_smScriptMan_HiddenField");
		addParams.put("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
		req.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,false,true,true]}");
		req.addPostParameters(addParams);
		return;
	}
	
	public void performInstrNoSearch(HTTPRequest req, Map<String,String> addParams, Map<String,String> cookies) {
		String resp = "";
		HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.GET);
		
		for(Map.Entry<String, String> entry: cookies.entrySet()) {
			req.setHeader(entry.getKey(), entry.getValue());
			req1.setHeader(entry.getKey(), entry.getValue());
		}
		resp = execute(req1);
		addParams = isolateParams(resp, formID, REQ_PARAM_NAMES);
		NodeList scripts = getScriptNodes(resp, formID);
		if (scripts != null) {
			ajax_HiddenField = getHiddenParam(scripts);
		}
		
		req1 = new HTTPRequest(req.getURL(), HTTPRequest.POST);
		for(Map.Entry<String, String> entry: cookies.entrySet()) {
			req.setHeader(entry.getKey(), entry.getValue());
			req1.setHeader(entry.getKey(), entry.getValue());
		}
		req1.setPostParameter("ctl00$cphMain$tcMain$tpNewSearch$ucSrchInstNo$meeFileNumber_ClientState", "");
		req1.setPostParameter("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
		req1.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,false,false,true]}");
		req1.setPostParameter(propTypeParamInstrSrch, req.getPostFirstParameter(propTypeParamInstrSrch));
		req1.setPostParameter("__EVENTTARGET", propTypeParamInstrSrch);
		req1.setPostParameter("__EVENTARGUMENT", "");
		req1.setPostParameter("__VIEWSTATE", "");
		req1.setPostParameter("__VIEWSTATEENCRYPTED", "");
		resp = execute(req1);
		
		if (resp != null) {
			ajax_HiddenField += ";";
		}
		
		addParams.remove("ctl00_smScriptMan_HiddenField");
		addParams.put("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
		req.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,false,true,true]}");
		req.addPostParameters(addParams);
		if ("ALL".equals(req.getPostFirstParameter("ctl00$cphMain$tcMain$tpNewSearch$ucSrchInstNo$ddlIndexType"))) {
			req.removePostParameters("__EVENTTARGET");
			req.setPostParameter("__EVENTTARGET", propTypeParamInstrSrch);
		}
		req.setPostParameter("__VIEWSTATEENCRYPTED", "");
		return;
	}
	
	public void performBookPageSearch(HTTPRequest req, Map<String,String> addParams, Map<String,String> cookies) {
		String resp = "";
		HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.GET);
		
		for(Map.Entry<String, String> entry: cookies.entrySet()) {
			req.setHeader(entry.getKey(), entry.getValue());
			req1.setHeader(entry.getKey(), entry.getValue());
		}
		resp = execute(req1);
		addParams = isolateParams(resp, formID, REQ_PARAM_NAMES);
		NodeList scripts = getScriptNodes(resp, formID);
		if (scripts != null) {
			ajax_HiddenField = getHiddenParam(scripts);
		}
		
		req1 = new HTTPRequest(req.getURL(), HTTPRequest.POST);
		for(Map.Entry<String, String> entry: cookies.entrySet()) {
			req.setHeader(entry.getKey(), entry.getValue());
			req1.setHeader(entry.getKey(), entry.getValue());
		}
		req1.setPostParameter("ctl00$cphMain$tcMain$tpNewSearch$ucSrchInstNo$meeFileNumber_ClientState", "");
		req1.setPostParameter("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
		req1.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,false,false,true]}");
		req1.setPostParameter(propTypeParamBookPageSrch, req.getPostFirstParameter(propTypeParamBookPageSrch));
		req1.setPostParameter("__EVENTTARGET", propTypeParamBookPageSrch);
		req1.setPostParameter("__EVENTARGUMENT", "");
		req1.setPostParameter("__VIEWSTATE", "");
		req1.setPostParameter("__VIEWSTATEENCRYPTED", "");
		resp = execute(req1);
		
		if (resp != null) {
			ajax_HiddenField += ";";
		}
		
		//propTypeParamBookPageSrch
		addParams.remove("ctl00_smScriptMan_HiddenField");
		addParams.put("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
		req.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,false,true,true]}");
		req.addPostParameters(addParams);
		return;
	}
	
	public void performQuickNameSearch(HTTPRequest req, Map<String,String> addParams, Map<String,String> cookies) {
		String resp = "";
		HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.GET);
		for(Map.Entry<String, String> entry: cookies.entrySet()) {
			req.setHeader(entry.getKey(), entry.getValue());
			req1.setHeader(entry.getKey(), entry.getValue());
		}
		resp = execute(req1);
		addParams = isolateParams(resp, formID, REQ_PARAM_NAMES);
		NodeList scripts = getScriptNodes(resp, formID);
		if (scripts != null) {
			ajax_HiddenField = getHiddenParam(scripts);
		}
		addParams.remove("ctl00_smScriptMan_HiddenField");
		addParams.put("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
		req.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,false,true,true]}");
		req.addPostParameters(addParams);
		return;
	}
	
	public void performPropertySearch(HTTPRequest req, Map<String,String> addParams, Map<String,String> cookies) {
		String resp = "";
		String propTypeValue = req.getPostFirstParameter(propTypeParamPropSrch);
		removeParamsDependingOnPropertyType(req, propTypeValue);
		
		HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.GET);
		for(Map.Entry<String, String> entry: cookies.entrySet()) {
			req.setHeader(entry.getKey(), entry.getValue());
			req1.setHeader(entry.getKey(), entry.getValue());
		}
		resp = execute(req1);
		if  (resp != null && resp.length()> 0) {
			addParams = isolateParams(resp, formID, REQ_PARAM_NAMES);
		}
		NodeList scripts = getScriptNodes(resp, formID);
		if (scripts != null) {
			ajax_HiddenField = getHiddenParam(scripts);
		}
		
		req1 = new HTTPRequest(req.getURL(), HTTPRequest.POST);
		for(Map.Entry<String, String> entry: cookies.entrySet()) {
			req.setHeader(entry.getKey(), entry.getValue());
			req1.setHeader(entry.getKey(), entry.getValue());
		}
		req1.setPostParameter(propTypeParamPropSrch, propTypeValue);
		req1.setPostParameter(sortPropSrch, "rbSortByFileDate");
		req1.setPostParameter(sortDirPropSrch, "Ascending");
		req1.setPostParameter(propSearchTypePropSrch, "rbPropSearchType_Platted");
		req1.setPostParameter(subClientStTypePropSrch, "");
		req1.setPostParameter(lastNamePropSrch, "");
		req1.setPostParameter(firstNamePropSrch, "");
		req1.setPostParameter(middleNamePropSrch, "");
		req1.setPostParameter(lastWildcardPropSrch, "0");
		req1.setPostParameter(firstWildcardPropSrch, "0");
		req1.setPostParameter(fromDatePropSrch, "");
		req1.setPostParameter(thruDatePropSrch, "");
		req1.setPostParameter("ctl00_smScriptMan_HiddenField", ajax_HiddenField);
		req1.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,false,false,false,false,false,true]}");
		req1.setPostParameter("__EVENTTARGET", propTypeParamPropSrch);
		req1.setPostParameter("__EVENTARGUMENT", "");
		req1.setPostParameter("__VIEWSTATE", "");
		req1.setPostParameter("__VIEWSTATEENCRYPTED", "");
		resp = execute(req1);
		
		addParams.remove("ctl00_smScriptMan_HiddenField");
		addParams.put("ctl00_smScriptMan_HiddenField", ajax_HiddenField + ";");
		req.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,false,false,false,false,false,true]}");
		req.setPostParameter("__VIEWSTATEENCRYPTED", "");
		req.addPostParameters(addParams);
		req.removePostParameters("__EVENTTARGET");
		req.setPostParameter("__EVENTTARGET", "");
		req.removePostParameters("ctl00$smScriptMan");
		return;
	}
	
	public void performAdvanceNameSearch(HTTPRequest req, Map<String,String> addParams, Map<String,String> cookies) {
		String resp = "";
		boolean hasIndexTypeParams 			= false;
		Map<String,String> indexTypeParams 	= new HashMap<String, String>();
		
		boolean hasPropertyTypeParam 	= false;
		String subdivParamPrefix 		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$lbSubdivisions";
		Map<String,String> subdivParams = new HashMap<String, String>();
		
		boolean hasKindGroupsParams 	= false;
		String groupParamPrefix 		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$repGroups";
		Map<String,String> groupParams 	= new HashMap<String, String>();
//		String kindParamPrefix  		= "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$repKinds";
//		Map<String,String> kindsParams 	= new HashMap<String, String>();
		
		String paramVal = "";
		
		String doctypeGroup = req.getPostFirstParameter(groupParamPrefix);
		if (doctypeGroup != null) {
			hasKindGroupsParams = true;
			doctypeGroup = "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$repGroups" + doctypeGroup;
			req.removePostParameters("ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$repGroups");
			req.removePostParameters("ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$repKinds");
			groupParams = extractParamsStartingWith(req, groupParamPrefix);
//			kindsParams = extractParamsStartingWith(req, kindParamPrefix);
			if (groupParams != null && groupParams.isEmpty()) {
				hasKindGroupsParams = false;
			}
		}
		
		paramVal = req.getPostFirstParameter(indexTypeOffAdvSrch);
		if (paramVal != null && StringUtils.isNotEmpty(paramVal)) {
			hasIndexTypeParams = true;
			indexTypeParams.put(indexTypeOffAdvSrch, paramVal);
		} 
		paramVal = req.getPostFirstParameter(indexTypeUccAdvSrch);
		if (paramVal != null && StringUtils.isNotEmpty(paramVal)) {
			hasIndexTypeParams = true;
			indexTypeParams.put(indexTypeUccAdvSrch, paramVal);
		}
		
		paramVal = req.getPostFirstParameter(propTypeParamAdvSrch);
		if (paramVal != null && StringUtils.isNotEmpty(paramVal)) {
			hasPropertyTypeParam = true;
			removeParamsDependingOnPropertyType(req, paramVal);
			subdivParams = extractParamsStartingWith(req, subdivParamPrefix);
		}
		
		HTTPRequest req1 = new HTTPRequest(req.getURL(), HTTPRequest.GET);
		for(Map.Entry<String, String> entry: cookies.entrySet()) {
			req.setHeader(entry.getKey(), entry.getValue());
			req1.setHeader(entry.getKey(), entry.getValue());
		}
		resp = execute(req1);
		if  (resp != null && resp.length()> 0) {
			addParams = isolateParams(resp, formID, REQ_PARAM_NAMES);
		}
		NodeList scripts = getScriptNodes(resp, formID);
		if (scripts != null) {
			ajax_HiddenField = getHiddenParam(scripts);
		}
		
		req1 = new HTTPRequest(req.getURL(), HTTPRequest.POST);
//		for(Map.Entry<String, String> entry: cookies.entrySet()) {
//			req1.setHeader(entry.getKey(), entry.getValue());
//		}
		req1.setPostParameter("ctl00_smScriptMan_HiddenField", ajax_HiddenField + ";");
//		req1.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,true,false,true,true]}");
		req1.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,false,false,false,true]}");
		req1.setPostParameter("__EVENTARGUMENT", "");
		req1.setPostParameter("__VIEWSTATE", "");
		req1.setPostParameter("__VIEWSTATEENCRYPTED", "");
		
		if (hasIndexTypeParams) {
			String tmpParamKey = "";
			for(Map.Entry<String,String> entry: indexTypeParams.entrySet()) {
				tmpParamKey = entry.getKey();
				req1.setPostParameter(entry.getKey(), entry.getValue());
				req1.removePostParameters("__EVENTTARGET");
				req1.setPostParameter("__EVENTTARGET", tmpParamKey);
			}
			req1.removePostParameters("ctl00$smScriptMan");
			req1.setPostParameter("ctl00$smScriptMan", "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$upSearchControlContainer|" + tmpParamKey);
			resp = execute(req1);
		}
		
		if (hasKindGroupsParams) {
			String tmpParamKey = "";
			for(Map.Entry<String,String> entry: groupParams.entrySet()) {
				tmpParamKey = entry.getKey();
				req1.setPostParameter(entry.getKey(), entry.getValue());
				req1.removePostParameters("__EVENTTARGET");
				req1.setPostParameter("__EVENTTARGET", tmpParamKey);
			}
			req1.removePostParameters("ctl00$smScriptMan");
			req1.setPostParameter("ctl00$smScriptMan", "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$upSearchControlContainer|" + tmpParamKey);
			
			resp = execute(req1);
			
//			addParams.putAll(kindsParams);
//			for (Map.Entry<String,HTTPRequest.ParametersVector> entry: req.getPostParameters().entrySet()) {
//				if (entry.getKey().startsWith(kindParamPrefix)) {
//					req.removePostParameters(entry.getKey());
//				}
//			}
		}
		
		if (hasPropertyTypeParam && !"-1".equals(req.getPostFirstParameter(propTypeParamAdvSrch))) {
			String tmpParamKey = "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$ddlPropertyType";
			req1.removePostParameters("ctl00$smScriptMan");
			req1.setPostParameter("ctl00$smScriptMan", "ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$upSearchControlContainer|" + tmpParamKey);
			req1.setPostParameter("ctl00$cphMain$tcMain$tpNewSearch$ucSrchAdvName$PropSearchType", "rbPropSearchType_Platted");
			req1.removePostParameters("__EVENTTARGET");
			req1.setPostParameter("__EVENTTARGET", tmpParamKey);
			req1.setPostParameter(tmpParamKey, req.getPostFirstParameter(tmpParamKey));
			for(Map.Entry<String,String> entry: subdivParams.entrySet()) {
				req1.setPostParameter(entry.getKey(), entry.getValue());
			}
			resp = execute(req1);
		}
		
		
		addParams.remove("ctl00_smScriptMan_HiddenField");
		addParams.put("ctl00_smScriptMan_HiddenField", ajax_HiddenField + ";");
		//req.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,true,false,true,true]}");
		req.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":0,\"TabState\":[true,false,false,false,true]}");
		req.setPostParameter("__EVENTARGUMENT", "");
		req.setPostParameter("__VIEWSTATE", "");
		req.setPostParameter("__VIEWSTATEENCRYPTED", "");
		req.addPostParameters(addParams);
		req.removePostParameters("__EVENTTARGET");
		req.setPostParameter("__EVENTTARGET", "");
//		req.setPostParameter("__EVENTTARGET", req1.getPostFirstParameter("__EVENTTARGET"));
		req.removePostParameters("ctl00$smScriptMan");
		return;
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		Map<String,String> addParams 	= (HashMap<String,String>)getAttribute("params");
		Map<String,String> cookies 		= (HashMap<String,String>)getAttribute("cookie");
		
		if (req.getPostFirstParameter(searchByParcelNo) != null) {
			perfomParcelNoSearch(req, addParams, cookies);
		
		} else if (req.getPostFirstParameter(searchByInstrumentNo) != null) {
			performInstrNoSearch(req, addParams, cookies);
		
		}  else if (req.getPostFirstParameter(searchByBookPage) != null) {
			performBookPageSearch(req, addParams, cookies);
				
		} else if (req.getPostFirstParameter(searchByQuickName) != null) {
			performQuickNameSearch(req, addParams, cookies);
			
		} else if (req.getPostFirstParameter(searchByProperty) != null) {
			performPropertySearch(req, addParams, cookies);
			
		} else if (req.getPostFirstParameter(searchByAdvName) != null) {
			performAdvanceNameSearch(req, addParams, cookies);
			
			
		} else if (req.getPostFirstParameter("ctl00$smScriptMan") != null) { 
			if (req.getPostFirstParameter("seq") != null) {
				String seq = req.getPostFirstParameter("seq");
				addParams = (HashMap<String,String>)getTransientSearchAttribute("params:" + seq);
									
				if (addParams != null) {
					req.removePostParameters("seq");
					for(Map.Entry<String, String> entry: addParams.entrySet()) {
						req.setPostParameter(entry.getKey(), entry.getValue());
					}
					
					if (!nextActionLink.equals(req.getPostFirstParameter("ctl00$smScriptMan")) && 
							!prevActionLink.equals(req.getPostFirstParameter("ctl00$smScriptMan"))) {
							//details page
							//req.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":2,\"TabState\":[true,true,true,true]}");
					} else {
						Random generator = new Random(); 
						int randomX = generator.nextInt(20);
						int randomY = generator.nextInt(20);
						String paramBaseName = req.getPostFirstParameter("ctl00$smScriptMan");
						paramBaseName = paramBaseName.replaceFirst("(?is)[^\\|]+\\|", "");
						req.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":2,\"TabState\":[true,true,true,true]}");
						req.setPostParameter(paramBaseName + ".x", Integer.toString(randomX));
						req.setPostParameter(paramBaseName + ".y", Integer.toString(randomY));
						//req.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":3,\"TabState\":[true,true,false,true,true]}");
					}
				}
				
//				if (!nextActionLink.equals(req.getPostFirstParameter("ctl00$smScriptMan")) && 
//						!prevActionLink.equals(req.getPostFirstParameter("ctl00$smScriptMan"))) {
//						//details page
//						req.setPostParameter("ctl00_cphMain_tcMain_ClientState","{\"ActiveTabIndex\":2,\"TabState\":[true,true,true,true]}");
//				}
			}	
			
		} else if (req.getPostParameter("ImageID") != null) {
			String seq = req.getPostFirstParameter("seq");
			String typeOfParams = "paramsImage:";
			
			if (req.getPostFirstParameter("isRefInstr") != null) {
				req.removePostParameters("isRefInstr");
				typeOfParams = "paramsRefInstr:";
			}
			if (req.getPostFirstParameter("isRefImg") != null) {
				req.removePostParameters("isRefImg");
				typeOfParams = "paramsRefImg:";
			}
			
			HashMap<String, String> postParams = (HashMap<String,String>)getTransientSearchAttribute(typeOfParams + seq);
				
			if (postParams != null) {
				req.removePostParameters("seq");
				for(Map.Entry<String, String> entry: cookies.entrySet()) {
					req.setHeader(entry.getKey(), entry.getValue());
				}
				for(Map.Entry<String, String> entry: postParams.entrySet()) {
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
				req.setPostParameter("__VIEWSTATE", "");
				req.setPostParameter("__VIEWSTATEENCRYPTED", "");
				
				if ("paramsRefInstr:".equals(typeOfParams)) {
					//details page for reference from detail page of current doc
					
				} else {
					getImageFromReq(req);
				}
			}
		
		} else if (req.getPostFirstParameter("ImageFromInterm") != null) {
			//click to see image from interm results
			String seq = req.getPostFirstParameter("seq");
			addParams = (HashMap<String,String>)getTransientSearchAttribute("paramsImage:" + seq);
			
			if (addParams != null) {
				req.removePostParameters("seq");
				req.removePostParameters("ImageFromInterm");
				
				for(Map.Entry<String, String> entry: cookies.entrySet()) {
					req.setHeader(entry.getKey(), entry.getValue());
				}
				
				for(Map.Entry<String, String> entry: addParams.entrySet()) {
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
				
				req.removePostParameters("__EVENTTARGET");
				req.setPostParameter("__EVENTTARGET","");
				
				String imgId = req.getPostFirstParameter("imgID");
				imgId = imgId.replaceFirst("(?is)[^\\|]+\\|", "").trim();
				
				req.removePostParameters("imgID");
				req.removePostParameters("ctl00$smScriptMan");
				req.setPostParameter("ctl00$smScriptMan", "ctl00$cphMain$upMain|" + imgId);
				Random generator = new Random(); 
				int randomX = generator.nextInt(20);
				int randomY = generator.nextInt(20);

				req.setPostParameter("__VIEWSTATEENCRYPTED","");
				req.setPostParameter("ctl00_cphMain_tcMain_ClientState", "{\"ActiveTabIndex\":2,\"TabState\":[true,true,true,true]}");
				req.setPostParameter(imgId + ".x", Integer.toString(randomX));
				req.setPostParameter(imgId + ".y", Integer.toString(randomY));
				
				getImageFromReq(req);
			}
		}
	}
	

	public byte[] downloadImage(String response) {
		byte[] finalImage = null;
		HtmlParser3 htmlParser = new HtmlParser3(response);
		
		if (htmlParser != null) {
			LinkedList<String> pageIndex = new LinkedList<String>();
			NodeList divList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_divDaeja"), true);
			
			if (divList != null && divList.size() > 0) {
				NodeList appletList = divList.extractAllNodesThatMatch(new TagNameFilter("applet"), true);
				
				if (appletList != null && appletList.size() > 0) {
					AppletTag applet = (AppletTag) appletList.elementAt(0);
	
					Hashtable<String, String> params = applet.getAppletParams();
					if (params != null) {
						Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
						while (it.hasNext()) {
							 Map.Entry<String, String> entry = it.next();
							 String key = entry.getKey();
							 if (key.matches("(?is)Page\\d+")){
								pageIndex.add(key.replaceFirst("(?is)Page(\\d+)", "$1")) ;
							 }
						}
					}
					
					Collections.sort(pageIndex);
					List<byte[]> images = new LinkedList<byte[]>();
					HTTPResponse imageResp = null;
					for (String index : pageIndex) {
						try {
							HTTPRequest newReq = new HTTPRequest(params.get("Page" + index), HTTPRequest.GET);
							newReq.setHeader("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
							newReq.setHeader("User-Agent", "Mozilla/4.0 (Windows 7 6.1) Java/1.6.0_33");
							imageResp = exec(newReq);
							images.add(imageResp.getResponseAsByte());
							
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (images.size() > 0){
						finalImage = TiffConcatenator.concatePngInTiff(images);
					}
				}
			}					
		}
		
		return finalImage;
	}
	
	public void getImageFromReq(HTTPRequest req) {
		String response = execute(req);
		HtmlParser3 htmlParser = new HtmlParser3(response);
		
		if (htmlParser != null) {
			LinkedList<String> pageIndex = new LinkedList<String>();
			NodeList divList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_cphMain_divDaeja"), true);
			
			if (divList != null && divList.size() > 0) {
				NodeList appletList = divList.extractAllNodesThatMatch(new TagNameFilter("applet"), true);
				
				if (appletList != null && appletList.size() > 0) {
					AppletTag applet = (AppletTag) appletList.elementAt(0);

					Hashtable<String, String> params = applet.getAppletParams();
					if (params != null) {
						Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
						while (it.hasNext()) {
							 Map.Entry<String, String> entry = it.next();
							 String key = entry.getKey();
							 if (key.matches("(?is)Page\\d+")){
								pageIndex.add(key.replaceFirst("(?is)Page(\\d+)", "$1")) ;
							 }
						}
					}
					
					Collections.sort(pageIndex);
					List<byte[]> images = new LinkedList<byte[]>();
					HTTPResponse imageResp = null;
					for (String index : pageIndex) {
						try {
							HTTPRequest newReq = new HTTPRequest(params.get("Page" + index), HTTPRequest.GET);
							newReq.setHeader("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
							newReq.setHeader("User-Agent", "Mozilla/4.0 (Windows 7 6.1) Java/1.6.0_33");
							imageResp = exec(newReq);
							images.add(imageResp.getResponseAsByte());
							
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (images.size() > 0){
						byte[] image = TiffConcatenator.concatePngInTiff(images);
						
//						HTTPResponse lastResponse = new HTTPResponse();
						imageResp.is = new ByteArrayInputStream(image);
						// bypass response
						req.setBypassResponse(imageResp);
					}
				}
			}					
		}
	}
	

	@Override
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 5.1; rv:23.0) Gecko/20100101 Firefox/23.0";
	}
	
	@Override
	public String getAcceptLanguage() {
		return "en-US,en;q=0.5";
	}
	
}


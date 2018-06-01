package ro.cst.tsearch.connection.http2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.ParserException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.webservice.OCRService;
import com.stewart.ats.webservice.ocr.xsd.Image;
import com.stewart.ats.webservice.ocr.xsd.Output;

public class ILDuPageRO extends HttpSite {
	private static final String[] ADD_PARAM_NAMES = { "__LASTFOCUS", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION" };
	private static final String[] REQ_PARAM_NAMES = { "__VIEWSTATE", "__EVENTVALIDATION" };
	
	private static final String FORM_NAME = "frmCommon";
	private static final HashMap<String, String[]> PARAM_PER_PAGE = new HashMap<String, String[]>();
	static {
		PARAM_PER_PAGE.put("SEARCH", new String[]{"__LASTFOCUS", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION"});
		//we don't add "__EVENTTARGET" for RESULTS page because is a special argument, treated in onBeforeRequest
		PARAM_PER_PAGE.put("RESULTS", new String[]{"__VIEWSTATE", "__EVENTARGUMENT", "__EVENTVALIDATION"});
		PARAM_PER_PAGE.put("ACCEPTCOSTCONFIRMATION", new String[]{"__LASTFOCUS", "__VIEWSTATE", "__EVENTTARGET", "__EVENTARGUMENT", "__EVENTVALIDATION"});
		PARAM_PER_PAGE.put("DETAIL",  new String[]{"__VIEWSTATE", "__EVENTVALIDATION"});
	}
	private static final Pattern ID_URL = Pattern.compile("ID=([0-9A-Z]+)");
	private static final Pattern PT_URL = Pattern.compile("PT=([A-Z]+)");
	public static final Pattern IMAGE_LINK = Pattern.compile("winPDF = window.open\\('([^']+)"); 
	private static final int MAX_RETRY_CAPTCHA = 5;
	String PARAM_PREFIX = "_ctl0:mainPlaceHolder:_ctl0:";
	
	boolean doLastSearch = false; //from time to time we are forced to relogin by the official site
								//we need to repeat the last search before we access the rest of the results from intermediary page
	int currentPage = 1; //we need to know what intermediary page we process, because the intermediary links are the same on both intermediary pages
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		Map<String, String> addParams = null;
		String paramsKey = "";
		boolean ignoreEventTarget = false;
		if (req.getPostParameter("PT") != null || req.getURL().contains("PT=")){
			String PT;
			if (req.getPostParameter("PT") != null ){
				PT = req.getPostParameter("PT").toString();
				req.setURL(req.getURL() + "?PT=" + PT);
			} else {
				PT = ro.cst.tsearch.servers.types.ILDuPageRO.getInformationFromResponse(req.getURL(), PT_URL);
			}
			if (PT.equals("RESULTS")){
				paramsKey = "RESULTS.params";
				ignoreEventTarget = true;
			} else if (PT.equals("SEARCH")){
				setTransientSearchAttribute("lastrequest", req);
				paramsKey = "SEARCH.params";
			} else if (PT.equals("ACCEPTCOSTCONFIRMATION") || req.getURL().contains("ACCEPTCOSTCONFIRMATION")){
				paramsKey = "ACCEPTCOSTCONFIRMATION.params";
			} else if (PT.equals("DETAIL")){
				paramsKey = "DETAIL.params";
			}
			req.removePostParameters("PT");
			if (req.getPostParameter("ID") != null){
				req.setURL(req.getURL() + "&ID=" + req.getPostParameter("ID"));
				req.removePostParameters("ID");
			}
			if (req.getPostParameter("IMAGETYPE") != null){
				req.setURL(req.getURL() + "&IMAGETYPE=" + req.getPostParameter("IMAGETYPE"));
				req.removePostParameters("IMAGETYPE");
			}
		}
		if (req.getURL().startsWith("http://recor")){
			req.setURL(req.getURL().replaceFirst("http:", "https:"));
		}

		String instrNString = "";
		if (req.getPostParameter("instrNum") != null){
			instrNString = req.getPostParameter("instrNum").toString();
			Map<String, String>instrEventTarget = (Map<String, String>)getTransientSearchAttribute("INSTR_EVENTTARGET:0");
			if (instrEventTarget.get(instrNString) != null){
				req.removePostParameters("__EVENTTARGET");
				req.setPostParameter("__EVENTTARGET", instrEventTarget.get(instrNString));
			}
			req.removePostParameters("instrNum");
			if (req.getPostParameter("page") != null){
				String page = req.getPostParameter("page").toString();
				String p = "";
				if (page.equals("1") && currentPage == 2){
					p = "_ctl0$mainPlaceHolder$_ctl0$CountySpecificResults1$dgResults$_ctl104$_ctl0";
				} else if (page.equals("2") && currentPage == 1){
					p = "_ctl0$mainPlaceHolder$_ctl0$CountySpecificResults1$dgResults$_ctl104$_ctl1";
				}
				if (!StringUtils.isEmpty(p)){
					HTTPRequest r = new HTTPRequest("https://recorder.dupageco.org/common.aspx");
					r.setPostParameter("PT", "RESULTS");
					r.setPostParameter("__EVENTTARGET", p);
					r.setMethod(HTTPRequest.POST);
					process(r);
				}
				req.removePostParameters("page");

			}
		}

		if (!paramsKey.equals("")){
			addParams = (Map<String, String>)getAttribute(paramsKey);
		}
		if (addParams == null){
			addParams = new HashMap<String, String>();
		}
		if (ignoreEventTarget){
			addParams.remove("__EVENTTARGET");
		}
		if( req.getPostFirstParameter("getFakeImage")!=null ){
			addParams = (Map<String, String>)getAttribute("login.params");
		}
		addParams(addParams, req);
		
		//break pin
		String pin = "";
		if (req.getPostParameter(PARAM_PREFIX+"CountySpecificSearch1:txtPIN1") != null && 
				StringUtils.isEmpty(req.getPostParameter(PARAM_PREFIX+"CountySpecificSearch1:txtPIN2").toString())){
			pin = req.getPostParameter(PARAM_PREFIX+"CountySpecificSearch1:txtPIN1").toString();
		}
		if (!StringUtils.isEmpty(pin)){
			pin = pin.replaceAll("-", "");
			String pin1 = "";
			String pin2 = "";
			String pin3 = "";
			String pin4 = "";
			if (pin.matches("\\d+")){
				//complete with zeroes
				for (int i=0; i< 10-pin.length(); i++){
					pin = "0" + pin;
				}
				pin1 = pin.substring(0, 2);
				pin2 = pin.substring(2, 4);
				pin3 = pin.substring(4, 7);
				pin4 = pin.substring(7);
			} 
			req.removePostParameters(PARAM_PREFIX+"CountySpecificSearch1:txtPIN1");
			req.setPostParameter(PARAM_PREFIX+"CountySpecificSearch1:txtPIN1", pin1);
			req.removePostParameters(PARAM_PREFIX+"CountySpecificSearch1:txtPIN2");
			req.setPostParameter(PARAM_PREFIX+"CountySpecificSearch1:txtPIN2", pin2);
			req.removePostParameters(PARAM_PREFIX+"CountySpecificSearch1:txtPIN3");
			req.setPostParameter(PARAM_PREFIX+"CountySpecificSearch1:txtPIN3", pin3);
			req.removePostParameters(PARAM_PREFIX+"CountySpecificSearch1:txtPIN4");
			req.setPostParameter(PARAM_PREFIX+"CountySpecificSearch1:txtPIN4", pin4);

		}
		
		String page = "";
		String getFakeImage = req.getPostFirstParameter("getFakeImage");
		if( getFakeImage != null ){
			try {
				req.removePostParameters("getFakeImage");
				HTTPResponse response  = process(req);
				page = response.getResponseAsString();
				HTTPRequest myRequest = null;
				if(page.contains("Enter Access Code")) {
					this.destroySession();
					return;
				} else {
					req.modifyURL("https://recorder.dupageco.org/common.aspx?PT=RESULTS");
					req.setMethod(HTTPRequest.POST);
					req.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=RESULTS");
					
					addParams = isolateParams(page, FORM_NAME);
					for (int i = 0; i < REQ_PARAM_NAMES.length; i++) {
						req.removePostParameters(REQ_PARAM_NAMES[i]);
						req.setPostParameter(REQ_PARAM_NAMES[i], addParams.get(REQ_PARAM_NAMES[i]));
					}
					Vector<String> toRemoveParams = new Vector<String>();
					for(Object paramObject : req.getPostParameters().keySet()){
						if (paramObject instanceof String) {
							String paramName = (String) paramObject;
							if(paramName.startsWith("_ctl2")){
								toRemoveParams.add(paramName);
							}
						}
					}
					for (String paramName : toRemoveParams) {
						req.removePostParameters(paramName);
					}
					req.removePostParameters("__LASTFOCUS");
					String id = null;
					Parser parser = Parser.createParser(page, null);
					org.htmlparser.util.NodeList trs;
					try {
						trs = parser.extractAllNodesThatMatch(new TagNameFilter("table"))
							.extractAllNodesThatMatch(new HasAttributeFilter("class","AdminListContent"))
							.extractAllNodesThatMatch(new TagNameFilter("tr"), true);
						
						if(trs.size()>1) {
							org.htmlparser.util.NodeList aList = trs.elementAt(1).getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
							if(aList.size() > 0) {
								id = ((LinkTag)aList.elementAt(0)).getLinkText();
								req.removePostParameters("__EVENTTARGET");
								req.setPostParameter("__EVENTTARGET", "_ctl0$mainPlaceHolder$_ctl0$CountySpecificResults1$dgResults$_ctl3$_ctl0");
								
							}
						}
					} catch (ParserException e) {
						e.printStackTrace();
					}
					
					if(id == null) {
						this.destroySession();
						return;
					}
					
					page = execute(req);
					
					String capcha = getCapchaAsString(3);
					if("justDetails".equals(getFakeImage)) {
						myRequest = getRequestForImagePage(page, id, capcha);
						
						req.clearPostParameters();
						req.modifyURL(myRequest.getURL());
						req.setMethod(myRequest.getMethod());
						req.setHeader("Referer", myRequest.getHeader("Referer"));
						req.setPostParameters(myRequest.getPostParameters());
						
					} else {
						myRequest = getRequestForImagePage(page, id, capcha);
						page = execute(myRequest);
						String doctype = "";
						if (page.contains("Doc Desc:")){
							try {
								parser = org.htmlparser.Parser.createParser(page, null);
								org.htmlparser.util.NodeList nodeList = parser.parse(null);
								org.htmlparser.util.NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id","tblInsideDiv"));
								if(mainTableList.size() == 0) {
									return;
								}
								
								org.htmlparser.util.NodeList tempList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
											.extractAllNodesThatMatch(new HasAttributeFilter("id","Table3"));
								if (tempList.size() == 1){
									TableRow[] rows = ((TableTag)tempList.elementAt(0)).getRows();
									if(rows.length > 1) {
										TableColumn[] columns = rows[3].getColumns();
										if (columns.length == 2){
											doctype = columns[1].toPlainTextString().trim();
										}
									}
								}
							} catch (Exception e) {
								logger.error("Error while parsing the doctype");
							}
						}
						boolean isPlat = false; //DocumentTypes.isPlatDocType(doctype, searchId);
						myRequest = getRequestForImageContent(page,isPlat);
						page = execute(myRequest);
						myRequest = new HTTPRequest("https://recorder.dupageco.org/ImageWaitPopup.aspx", HTTPRequest.GET);
						myRequest.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=IMAGE&IMAGETYPE="+(isPlat?"PLAT":"IMAGE"));
						page = execute(myRequest);
						myRequest = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=IMAGE&IMAGETYPE="+(isPlat?"PLAT":"IMAGE"), HTTPRequest.GET);
						myRequest.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=DETAIL");
						page = execute(myRequest);
						String link = ro.cst.tsearch.servers.types.ILDuPageRO.getInformationFromResponse(page, IMAGE_LINK);
						
						myRequest = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=DETAIL", HTTPRequest.GET);
						myRequest.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=IMAGE&IMAGETYPE="+(isPlat?"PLAT":"IMAGE"));
						
						String resp = execute(myRequest);
						
						if(!resp.contains("Image...")){
							req.setPostParameter("noimage", "true");
							return;
						}
						
						if(StringUtils.isEmpty(link)) {
							
							req.modifyURL("https://recorder.dupageco.org/DocImage/DN" + id.substring(0,5) + "-" + id.substring(5) + "FULL.pdf");
						} else {
							req.modifyURL("https://recorder.dupageco.org/" + link);
						}
						req.setMethod(HTTPRequest.GET);
						req.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=IMAGE&IMAGETYPE="+(isPlat?"PLAT":"IMAGE"));
					}
				}
				
				return;
			
			} finally {
				
			}
		}
		
		
		String url = req.getURL();
		if(req.getPostParameter("fileId") == null
			|| req.getPostParameter("fileId").toString().equals("")
			|| req.getPostParameter("strCAPTCHA") == null
			|| req.getPostParameter("strCAPTCHA").toString().equals("")
		){
			return;
		}
			
		// extract information
		String strCAPTCHA = req.getPostParameter("strCAPTCHA").toString().toUpperCase();
		String fileId = req.getPostParameter("fileId").toString();
		req.removePostParameters("strCAPTCHA");
		req.removePostParameters("fileId");
		if (!url.contains("ACCEPTCOSTCONFIRMATION")){
			return;
		}
		// modfify url
		req.setMethod(HTTPRequest.POST);
		req.setPostParameter(PARAM_PREFIX+"txtCode", strCAPTCHA);
		req.setPostParameter(PARAM_PREFIX+"cmdFreeSubmit", "Submit");
		req.setHeader("Referer", req.getURL());
		req.modifyURL(url.replaceFirst("&fileId=\\d+&strCAPTCHA=[\\d\\w]+",""));
		
		// try to delete image file				
		if(!"".equals(fileId)){
			String fileName = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchDir() + 
				"temp" + File.separator + fileId;
			new File(fileName + ".jpg").delete();
			new File(fileName + ".jpg.tiff").delete();
			
		}
	}

	public HTTPRequest getRequestForImageContent(String page,boolean isPlat) {
		try {
			HTTPRequest r = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=DETAIL", HTTPRequest.POST);
			if(isPlat) {
				r.setPostParameter(PARAM_PREFIX+"cmdViewPlat", "Plat...");
			} else {
				r.setPostParameter(PARAM_PREFIX+"cmdViewImage", "Image...");	
			}
			r.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=DETAIL");
			
			Map<String, String> addParams = isolateParams(page, FORM_NAME);
			
			r.setPostParameter("__VIEWSTATE", addParams.get("__VIEWSTATE"));
			r.setPostParameter("__EVENTVALIDATION", addParams.get("__EVENTVALIDATION"));
			
			return r;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public HTTPRequest getRequest(String page) {
		
		try {
			HTTPRequest r = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=SEARCH", HTTPRequest.POST);
			
			r.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=SEARCH");
			
			Map<String, String> addParams = isolateParams(page, FORM_NAME);
			
			r.setPostParameter("__VIEWSTATE", addParams.get("__VIEWSTATE"));
			r.setPostParameter("__EVENTVALIDATION", addParams.get("__EVENTVALIDATION"));
			
			return r;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private HTTPRequest getRequestForImagePage(String page, String id, String capcha) {
		try {
			
			HTTPRequest r = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=ACCEPTCOSTCONFIRMATION&ID=" + id);
			r.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=ACCEPTCOSTCONFIRMATION&ID=" + id);
			r.setPostParameter(PARAM_PREFIX+"txtCode", capcha);
			r.setPostParameter(PARAM_PREFIX+"cmdFreeSubmit", "Submit");
			r.setMethod(HTTPRequest.POST);
			
			Map<String, String> addParams = isolateParams(page, FORM_NAME);
			
			r.setPostParameter("__VIEWSTATE", addParams.get("__VIEWSTATE"));
			r.setPostParameter("__EVENTVALIDATION", addParams.get("__EVENTVALIDATION"));
			
			return r;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Map<String, String> isolateParams(String page, String form){
		Map<String,String> addParams = new HashMap<String,String>();
		if (StringUtils.isEmpty(page) || StringUtils.isEmpty(form)){
			return addParams;
		}
		Map<String, String> params = new HashMap<String,String>();
		try {
			params = new SimpleHtmlParser(page).getForm(form).getParams();
		}catch(Exception e) {
			try{
				params = new SimpleHtmlParser(page).getForm("aspnetForm").getParams();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		
		if(page.contains("ChallengeScript")){
			Pattern p = Pattern.compile("ChallengeScript\":\"~(\\d+)\"");
			Matcher m = p.matcher(page);
			
			if(m.find()){
				int group = 0 ;
				try{
					group = Integer.parseInt(m.group(1));
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
				addParams.put("NoBot:NoBot_NoBotExtender_ClientState", "-" + (group+1));
			}
		}
		
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		return addParams;
	}
	
	
	public String[] getImage(HttpSite site){
		String[] ret = new String[]{null, null};
		HTTPRequest request = new HTTPRequest("https://recorder.dupageco.org/controls/CodeGenerater.aspx");
		HTTPResponse response = null;
		for (int i = 0; i < 3 && (response == null || response.getReturnCode() != 200); i++){
			try {
				response = site.process(request);
			} finally {
				// always release the HttpSite
				HttpManager.releaseSite(site);
			}
		}
		// 	check that we'we got an image
		if(response!= null && !response.getContentType().contains("image/jpeg")){
			logger.error("Image was not downloaded!");
			return null;
		}
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				
		long fileId = System.nanoTime();
		String fileName = search.getSearchDir() + "temp" + File.separator + fileId + ".jpg";
		ret[0] = String.valueOf(fileId);
		ret[1] = fileName;
		InputStream inputStream = response.getResponseAsStream();
    	FileUtils.writeStreamToFile(inputStream, fileName);
    	return ret;
	}
	
	private String getCapchaAsString(int retryNumber) {
		String capcha = null;
		logger.debug("Trying to get capcha as string...");
		for (int i = 0; i < retryNumber; i++) {
			capcha = getCapchaAsString();
			if (capcha != null && (capcha.length() == 5 || capcha.length() == 7)) {
				break;
			}
		}
		logger.debug("Capcha retrieved as: [" + capcha + "]");
		return capcha;
	}

	String	ocrString	= null;

	private String getCapchaAsString() {

		String[] fileName = getImage(this);
		if (fileName != null) {
			logger.debug("Downloaded image [" + fileName[1] + "] ... Trying to ocr");
			// we downloaded the image
			try {
				BufferedImage img = ImageIO.read(new File(fileName[1]));
				img = removeGrid(img);
				ImageIO.write(img, "tiff", new File(fileName[1] + ".tiff"));

				for (int i = 0; i < MAX_RETRY_CAPTCHA; i++) {
					if (ServerConfig.getTesseractEnabledForCaptcha()) {
						ocrString = ocrImage(fileName[1] + ".tiff");
					} else {
						ocrString = ocrImage(FileUtils.readBinaryFile(fileName[1] + ".tiff"));
					}
					
					if (StringUtils.isNotEmpty(ocrString) && (ocrString.length() == 5 || ocrString.length() == 7)) {
						ocrString = ocrString.replaceAll("O", "0");
						break;
					}
				}
				
				// do requests
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.debug("Downloaded image [" + fileName[1] + "] ... Ocr complete");
		}
		return ocrString;
	}
	public boolean isParentSite(){
		return InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchType() == Search.PARENT_SITE_SEARCH;
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		
		if(req.hasPostParameter("noimage")){
			res.body = "<html>image_error</html>";
			res.is = IOUtils.toInputStream(res.body);
			res.contentLenght = res.body.length();
			res.returnCode = 200;
			return;
		}
		
		if (res == null || req.getPostFirstParameter("getFakeImage")!=null) {
			return;
		}
		String content = "";
		if (res.getContentType().contains("text/html")){
			content = res.getResponseAsString();
			res.is = new ByteArrayInputStream(content.getBytes());
		}
		if (content.contains("Create New Account") && !req.getURL().contains("LOGIN") && !req.getURL().contains("EntryCode.aspx")){
			setForceRelogin(res);
		} else {
			String key = "";
			String form = "frmCommon";
			boolean doCapcha = false;
			if (req.getURL().contains("RESULTS") && StringUtils.isEmpty(req.getRedirectLocation())){
				if (content.matches("(?is).*_ctl0\\$mainPlaceHolder\\$_ctl0\\$CountySpecificResults1\\$dgResults\\$_ctl104\\$_ctl1.*")){
					currentPage = 1;
				} else if (content.matches("(?is).*_ctl0\\$mainPlaceHolder\\$_ctl0\\$CountySpecificResults1\\$dgResults\\$_ctl104\\$_ctl0.*")){
					currentPage = 2;
				}
			}
			if (req.getRedirectLocation().contains("RESULTS")){
				currentPage = 1;
			}
			if (req.getRedirectLocation().contains("RESULTS")
				|| (req.getURL().contains("RESULTS") && StringUtils.isEmpty(req.getRedirectLocation()))){
				key = "RESULTS.params";
				
			} else if (req.getRedirectLocation().contains("DETAIL")){
				key = "DETAIL.params";
			} else if (req.getRedirectLocation().contains("ACCEPTCOSTCONFIRMATION")){
				key = "ACCEPTCOSTCONFIRMATION.params";
				if (!isParentSite() && !content.matches("(?si).*document\\s*Detail\\s*\\-\\s*DuPage\\s*County\\s*Recorder.*")
						&& !StringUtils.isEmpty(req.getPostParameter("__VIEWSTATE").toString())){
					//doCapcha = true;
				} 
			} else if (req.getRedirectLocation().contains("SEARCH")){
				key = "SEARCH.params";
			}
			if (!key.equals("")){
				Map<String, String> addParams = isolateParams(content, form);
				setAttribute(key, addParams);
			}
			if (doCapcha){
				boolean successCapcha = false; 
				for (int j = 0;j<MAX_RETRY_CAPTCHA && !successCapcha; j++){
					String[] fileName = getImage(this);
					if (fileName != null){
						//we downloaded the image
						try{
							BufferedImage img = ImageIO.read(new File(fileName[1]));
							img = removeGrid(img);
							ImageIO.write(img, "tiff", new File(fileName[1] +  ".tiff"));
							String ocrString = "";
							
							for (int i=0;i<MAX_RETRY_CAPTCHA && StringUtils.isEmpty(ocrString);i++){
								if(ServerConfig.getTesseractEnabledForCaptcha()) {
									ocrString = ocrImage(fileName[1]+".tiff");
								} else {
									ocrString = ocrImage(FileUtils.readBinaryFile(fileName[1]+ ".tiff"));
								}
							}
							if (!StringUtils.isEmpty(ocrString) && ocrString.length() == 5){
								ocrString = ocrString.replaceAll("O", "0");
								
							}
							
							if (!StringUtils.isEmpty(ocrString) && ocrString.length() == 5){
								ocrString = ocrString.replaceAll("O", "0");
								//get the detail page
								HTTPRequest request;
								if (req.getRedirectLocation().contains("ACCEPTCOSTCONFIRMATION")){
									request = new HTTPRequest(req.getRedirectLocation());
								} else if (req.getURL().contains("ACCEPTCOSTCONFIRMATION")) {
									request = new HTTPRequest(req.getURL());
								} else {
									return;
								}
								request.setPostParameter("fileId", fileName[0]);
								request.setPostParameter("strCAPTCHA", ocrString);
	
								HTTPResponse response = null;
								for (int i = 0; i < 3 && (response == null || response.getReturnCode() != 200); i++){
									response = process(request);
								}
								if (response != null && response.getReturnCode() == 200){
									content = response.getResponseAsString();
									response.is = new ByteArrayInputStream(content.getBytes());
									if (!content.matches("(?is).*Verify\\s+and\\s+enter\\s+the" +
					"\\s+access\\s+code\\s+to\\s+obtain\\s+the\\s+detail\\s+information\\s+at\\s+no\\s+charge.*")){
										successCapcha = true;
										res.is = response.is;
										res.returnCode = response.returnCode;
										res.body = response.body;
										res.contentLenght = response.contentLenght;
										res.contentType = response.contentType;
										res.headers = response.headers;
									}
								}
							}
							//do requests
						} catch (IOException e){
							e.printStackTrace();
						}
						
					}
				}
				if (!successCapcha){
					SearchLogger.logWithServerName("We had troubles getting the CAPTCHA image from server or OCR-ing it."
										+ " Instrument number " + getParameterID(req.getRedirectLocation()) + " was not processed", searchId, 1, getDataSite());
				}
			}
			//get the image link and press the button that returns us to intermediate page.
			if (false && req.getRedirectLocation().contains("DETAIL")){
				//get the image link
				HTTPRequest r;
				if (false && content.contains("value=\"Image...\"")){
					r = new HTTPRequest("https://recorder.dupageco.org/common.aspx", HTTPRequest.POST);
					r.setPostParameter(PARAM_PREFIX+"cmdViewImage", "Image...");
					r.setPostParameter("PT", "DETAIL");
					r.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=DETAIL");
					HTTPResponse rs = process(r);
					if (rs.getReturnCode() == 500){
						setForceRelogin(res);
					} else {
						r = new HTTPRequest("https://recorder.dupageco.org/ImageWaitPopup.aspx", HTTPRequest.GET);
						//r.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=DETAIL");
						process(r);
						r = new HTTPRequest("https://recorder.dupageco.org/common.aspx", HTTPRequest.GET);
						r.setPostParameter("PT", "IMAGE");
						r.setPostParameter("IMAGETYPE", "IMAGE");
						r.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=DETAIL");
						rs = process(r);
						if (rs.getReturnCode() == 500){
							setForceRelogin(res);
						} else {
							String c = rs.getResponseAsString();
							String link = ro.cst.tsearch.servers.types.ILDuPageRO.getInformationFromResponse(c, IMAGE_LINK);
							if (!StringUtils.isEmpty(link)){
								content = res.getResponseAsString();
								content += "<!-- linktoimage='" + link + "'-->";
								res.is = new ByteArrayInputStream(content.getBytes());
								res.body = content;
							}
							r = new HTTPRequest("https://recorder.dupageco.org/common.aspx", HTTPRequest.GET);
							r.setPostParameter("PT", "DETAIL");
							rs = process(r);
							if (rs.getReturnCode() == 500){
								setForceRelogin(res);
							}
						}
					}
					
				}
				//return to results
				if (res.getReturnCode() != 500){
					r = new HTTPRequest("https://recorder.dupageco.org/common.aspx", HTTPRequest.POST);
					r.setPostParameter(PARAM_PREFIX+"cmdBack", "Search Result");
					r.setPostParameter("PT", "DETAIL");
					r.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=DETAIL");
					process(r);
				}
			}
		}
	}

	public void setForceRelogin(HTTPResponse res){
		res.returnCode = 500; //I want this request to be done again
		status = STATUS_NOT_KNOWN;
		if (!isParentSite()){
			doLastSearch = true;
		} else {
			res.is = new ByteArrayInputStream("Please repeat search, official site forced us to relogin.".getBytes());
			res.body = "Please repeat search, official site forced us to relogin.";
		}
	}
	
	public static String getParameterID(String url){
		return ro.cst.tsearch.servers.types.ILDuPageRO.getInformationFromResponse(url, ID_URL);
	}
	
	public static boolean checkParams(Map<String, String> addParams){
		for(String key: REQ_PARAM_NAMES){
			if(StringUtils.isEmpty(addParams.get(key))){
				return false;
			}
		}
		return true;
	}
	
	public static HTTPRequest addParams(Map<String, String> addP, HTTPRequest request){
		String url = request.getURL();
		Matcher ptm = PT_URL.matcher(url);
		Map<String, String> addParams = new HashMap<String, String>();
		if (ptm.find()){
			String pt = ptm.group(1);
			if (PARAM_PER_PAGE.containsKey(pt)){
				for (String s:PARAM_PER_PAGE.get(pt)){
					if (!addP.containsKey(s)){
						addParams.put(s, "");
					} else {
						addParams.put(s, addP.get(s));
					}
				}
			}
		}
		if (addParams.size() == 0 && addP.size() != 0){
			addParams = addP;
		}
		Iterator<String> i = addParams.keySet().iterator();
		while(i.hasNext()){
			String k = i.next();
			request.removePostParameters(k);
			request.setPostParameter(k, addParams.get(k));
		}		
		return request;
	}
	
	@Override
	public LoginResponse onLogin() {
		
		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
        Protocol.registerProtocol("https", easyhttps);
		
		String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "ILDuPageRO", "user");
		String pass = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "ILDuPageRO", "password");
		
		HTTPRequest request;
		String page;
		
		//referer site
		//request = new HTTPRequest("http://www.dupageco.org/recorder/generic.cfm?doc_id=332", HTTPRequest.GET);
		//page = execute(request);
		
		// login page
		request = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=LOGIN", HTTPRequest.GET);
		page = execute(request);
		
//		request = new HTTPRequest("https://recorder.dupageco.org/EntryCode.aspx", HTTPRequest.GET);
//		page = execute(request);
		if (page.contains("The system is currently undergoing") || page.contains("There is not enough space on the disk")){
			//it does not work during night
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "The site is in maintenance!");
		}
		
		Map<String,String> addParams = new HashMap<String, String>();
		
		for(int i=0; i<3; i++){
			String capcha = getCapchaAsString(3);
			
			if(capcha == null || !(capcha.length() == 5 || capcha.length() == 7) ) {
				return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
			}
			
			addParams = isolateParams(page, "frmEntryCode");
			if (!checkParams(addParams)){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
			
			request = new HTTPRequest("https://recorder.dupageco.org/EntryCode.aspx", HTTPRequest.POST);
			request.setPostParameter("cmdSubmit", "Submit");
			request.setPostParameter("txtCode", capcha);
			request.setHeader("Referer", "https://recorder.dupageco.org/EntryCode.aspx");
			for (Entry<String, String> e : addParams.entrySet()) {
				request.setPostParameter(e.getKey(), e.getValue());
			}
	
			HTTPResponse response = process(request);
			
			page = response.getResponseAsString();
			
			if(page.contains("Start Searching"))
				break;
		}
		
		if(!page.contains("Start Searching"))
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Wrong captcha!");
		
        //isolate parameters
		addParams = isolateParams(page, "frmEntryCode");
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		addParams.remove("NoBot:NoBot_NoBotExtender_ClientState");
		
		// store parameters
		setAttribute("login.params", addParams);
		
		request = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=LOGIN");
		request.setMethod(HTTPRequest.POST);
		request.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=LOGIN");
		request.setPostParameter(PARAM_PREFIX+"txtUserID", user);
		request.setPostParameter(PARAM_PREFIX+"txtPassword" , pass);
		request.setPostParameter(PARAM_PREFIX+"cmdSubmit", "Submit");
		request = addParams(addParams, request);
		
		page = execute(request);

		if (doLastSearch  && getTransientSearchAttribute("lastrequest") != null){
			doLastSearch = false;
			request = (HTTPRequest)getTransientSearchAttribute("lastrequest");
			int i;
			page = "";
			for (i = 1; i<=3 && !page.matches("(?s).*To\\s*view\\s*the\\s*document\\s*details,\\s*please\\s*click\\s*on\\s*the\\s*document.*"); i++){
				page = execute(request);
			}
			if (i>3){
				return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
			}
		}
        
		if (!page.matches("(?s).*Use\\s*calendar\\s*to\\s*pick\\s*a\\s*date.*")
				&& !page.matches("(?s).*To\\s*view\\s*the\\s*document\\s*details,\\s*please\\s*click\\s*on\\s*the\\s*document.*")){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
		}
		
		//isolate parameters
		addParams = isolateParams(page, "frmCommon");
		if (!checkParams(addParams)){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
		}
		
		// store parameters
		setAttribute("login.params", addParams);

		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onRedirect(HTTPRequest req) {
		if (status != STATUS_LOGGED_IN) {
			return;
		}
		super.onRedirect(req);
	}

	public HTTPRequest getLastSearch(){
		HTTPRequest req = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=SEARCH"); 
		return req;
	}
	
	public static void main(String args[]){
		String htmlFile = "<table border=1 align=center>";
		htmlFile +="<tr><td>Original</td><td>Prelucrat</td><td>Rez OCR</td><td>Timp executie</td></tr>";
		BufferedImage img = null;
		long total = 0;
		int i;
		File dir = new File("D:\\work\\sites\\ILDuPageRO\\capcha1");
		try {
			String[] fileNames = dir.list();
			
			for (i =0; i<fileNames.length; i++){
				long start = (new Date()).getTime();
				htmlFile += "<tr><td><img src=D:\\work\\sites\\ILDuPageRO\\capcha\\" + fileNames[i] + "></td>";
				img = ImageIO.read(new File("D:\\work\\sites\\ILDuPageRO\\capcha\\" + fileNames[i]));
				img = removeGrid(img);
				ImageIO.write(img, "jpg", new File("D:\\work\\sites\\ILDuPageRO\\capcha\\" + fileNames[i] + "done.jpg"));
				ImageIO.write(img, "tiff", new File("D:\\work\\sites\\ILDuPageRO\\capcha\\" + fileNames[i] + ".tiff"));
				htmlFile += "<td><img src=D:\\work\\sites\\ILDuPageRO\\capcha" + fileNames[i] + "done.jpg></td>";
				htmlFile += "<td>" + ocrImage(FileUtils.readBinaryFile("D:\\work\\sites\\ILDuPageRO\\capcha\\" + fileNames[i] + ".tiff")) +"</td>";

				long end = (new Date()).getTime();
				start = (end -start)/1000;
				total += start;
				htmlFile += "<td>" + start + "s</td>";
				htmlFile += "</tr>";
				System.out.println(i + " - " + start);
			}
			long average = total/i;
			htmlFile += "<tr><td>Total: " + total + "s</td><td>Average: " + average + "s</td></tr>"; 
			htmlFile += "</table>";
			FileUtils.writeTextFile("D:\\work\\sites\\ILDuPageRO\\result.html", htmlFile);
			
		} catch(IOException e){
			e.printStackTrace();
		}
		System.out.println(img.getTileWidth() + " x " +img.getTileHeight());
	}
	
	public static BufferedImage removeGrid(BufferedImage img){

		ArrayList<Integer> x = new ArrayList<Integer>();
		ArrayList<Integer> y = new ArrayList<Integer>();
		Graphics2D graphic = img.createGraphics();
		graphic.setColor(new Color(255, 255, 255));
		
		//record the horizontal lines
		for(int i=1; i<img.getHeight()-1; i++){
			if (isBlack(img.getRGB(1, i))){
				y.add(i);
			}
		}
		//record the vertical lines
		for(int j =1; j < img.getWidth()-1; j++){
			if (isBlack(img.getRGB(j, 1))){
				x.add(j);
			}
		}
		//delete horizontal lines
		for (int i:y){
			for(int j =1; j < img.getWidth()-1; j++){
				if (!isBlack(img.getRGB(j, i-1) ) && !isBlack(img.getRGB(j, i+1))){
						img.setRGB(j, i, 0x00ffffff);
				}
			}
		}
		
		//delete vertical lines
		for (int j:x){
			for (int i = 1; i < img.getHeight()-1; i++){
				if (!isBlack(img.getRGB(j-1, i) ) && !isBlack(img.getRGB(j+1, i))){
						img.setRGB(j, i, 0x00ffffff);
				}			
			}
		}
		//delete margins
		graphic.drawLine(0, 0, img.getWidth(), 0);
		graphic.drawLine(0, 0, 0, img.getHeight()-1);
		graphic.drawLine(0, img.getHeight()-1, img.getWidth()-1, img.getHeight()-1);
		graphic.drawLine(img.getWidth()-1, 0, img.getWidth()-1, img.getWidth()-1);
		
		int blackInt = Color.BLACK.getRGB();
		int whiteInt = Color.WHITE.getRGB();
		for (int i = 1; i < img.getHeight()-1; i++){
			for(int j =1; j < img.getWidth()-1; j++){
				if (isAlmostBlack(img.getRGB(j, i))){
					img.setRGB(j, i, blackInt);
				} else {
					img.setRGB(j, i, whiteInt);
				}
			}
		}
		
		return img;
	}
	
	private static boolean isAlmostBlack(int rgb) {
		Color middle = new Color(128,128,128);
		if(middle.getRGB() < rgb) {
			return false;
		} 
		return true;
	}

	public static boolean isBlack(int c){
		int u = 50; //the max values of a byte
		int  r = (c & 0x00ff0000) >> 16;
		int  g = (c & 0x0000ff00) >> 8;
		int  b = c & 0x000000ff;
		return r < u && g < u && b <u;
	}

	public static String ocrImage(byte[] img){
		Image image = Image.Factory.newInstance();
		image.setDescription("Captcha");
		image.setData(img);
		image.setType("tif");
		try {
			Output out = OCRService.process(image, false);;
			if(out.getSuccess()){							
				return processResult(out);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static String ocrImage(String filename){
		  try {
		   File initialFile = new File(filename);
		   String newFilename = filename.substring(0,filename.indexOf(".")) + ".tif";
		   File tifFile = new File(newFilename);
		   org.apache.commons.io.FileUtils.copyFile(initialFile, tifFile);
		   String textFile = newFilename.replaceAll(".tif", "");
		   String[] exec = new String[3];
		   exec[0] = ServerConfig.getTesseractPath();
		   exec[1] = tifFile.getAbsolutePath();
		   exec[2] = textFile;
		   ClientProcessExecutor cpe = new ClientProcessExecutor( exec, true, true );
		            cpe.start();
		            String ocrdText = FileUtils.readTextFile(textFile+".txt");
		            return ocrdText;
		  } catch (/*Remote*/Exception e) {
		   e.printStackTrace();
		  }
		  return "";
	 }
	
	public static String processResult(Output out){
		try {
			byte[] res = out.getResult();
			String outS = new String(res, 2, res.length -2, "UTF-8").replace(new String(new char[]{(char)0}), "");
			Document result = XmlUtils.parseXml(outS);
			String ocrRes = "";
			NodeList wd = result.getElementsByTagName("wd");
			for(int i =0; i< wd.getLength(); i++){
				if (wd.item(i).getFirstChild() != null){
					String nodeVal = wd.item(i).getFirstChild().getNodeValue();
					ocrRes += nodeVal;
				}
					
			}
			return ocrRes.replaceAll("\\s", "");
		} catch(Exception e){
			return "";
		}
	}
	
	
	public RawResponseWrapper getResponseForLink(String page, String id) {
		
		String doctype = "";
		if (page.contains("Doc Desc:")){
			try {
				org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(page, null);
				org.htmlparser.util.NodeList nodeList = parser.parse(null);
				org.htmlparser.util.NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","tblInsideDiv"));
				if(mainTableList.size() != 0) {
					org.htmlparser.util.NodeList tempList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"),true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id","Table3"));
					if (tempList.size() == 1){
						TableRow[] rows = ((TableTag)tempList.elementAt(0)).getRows();
						if(rows.length > 1) {
							TableColumn[] columns = rows[3].getColumns();
							if (columns.length == 2){
								doctype = columns[1].toPlainTextString().trim();
							}
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error while parsing the doctype");
			}
		}
		boolean isPlat = false; //DocumentTypes.isPlatDocType(doctype, searchId);
		

		// mark that we're treating onBeforeRequest
		setAttribute("onBeforeRequest", Boolean.TRUE);
		try {
			HTTPRequest myRequest = getRequestForImageContent(page,isPlat);
			page = execute(myRequest);
			myRequest = new HTTPRequest("https://recorder.dupageco.org/ImageWaitPopup.aspx", HTTPRequest.GET);
			myRequest.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=IMAGE&IMAGETYPE="+(isPlat?"PLAT":"IMAGE"));
			page = execute(myRequest);
			myRequest = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=IMAGE&IMAGETYPE="+(isPlat?"PLAT":"IMAGE"), HTTPRequest.GET);
			myRequest.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=DETAIL");
			page = execute(myRequest);
			String link = ro.cst.tsearch.servers.types.ILDuPageRO.getInformationFromResponse(page, 
					ro.cst.tsearch.connection.http2.ILDuPageRO.IMAGE_LINK);
			
			myRequest = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=DETAIL", HTTPRequest.GET);
			myRequest.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=IMAGE&IMAGETYPE="+(isPlat?"PLAT":"IMAGE"));
			
			process(myRequest);
			
			if(StringUtils.isEmpty(link)) {
				myRequest.modifyURL("https://recorder.dupageco.org/DocImage/DN" + id.substring(0,5) + "-" + id.substring(5) + "FULL.pdf");
			} else {
				myRequest.modifyURL("https://recorder.dupageco.org/" + link);
			}
			myRequest.setMethod(HTTPRequest.GET);
			myRequest.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=IMAGE&IMAGETYPE="+(isPlat?"PLAT":"IMAGE"));
			
			HTTPResponse response = process(myRequest);
			RawResponseWrapper rawResponseWrapper = new RawResponseWrapper(
					response.getContentType(),
					(int)response.getContentLenght(), 
					new BufferedInputStream(response.getResponseAsStream()));
			return rawResponseWrapper;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			setAttribute("onBeforeRequest", Boolean.FALSE);
		}
		return null;
	}
	
	public String getResponse(String page, String id) {
		

		// mark that we're treating onBeforeRequest
		setAttribute("onBeforeRequest", Boolean.TRUE);
		try {
			HTTPRequest myRequest = getRequest(page);
			
			HTTPResponse response = process(myRequest);
			
			myRequest = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=SEARCH", HTTPRequest.POST);
			myRequest.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=SEARCH");
			Map<String, String> addParams = isolateParams(response.getResponseAsString(), FORM_NAME);
			
			myRequest.setPostParameter("__VIEWSTATE", addParams.get("__VIEWSTATE"));
			myRequest.setPostParameter("__EVENTVALIDATION", addParams.get("__EVENTVALIDATION"));
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtDocNumber", id);
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtName", "");
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtStreetNumber", "");
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtStreetName", "");
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtCity", "");
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtPIN1", "");
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtPIN2", "");
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtPIN3", "");
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtPIN4", "");
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtTrustNumber", "");
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtStart", "");
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:CountySpecificSearch1:txtEnd", "");
			myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:cmdSearch", "Search");
			
			response = process(myRequest);
			String resp = response.getResponseAsString();
			if (resp.contains("_ctl0$mainPlaceHolder$_ctl0$CountySpecificResults1$dgResults$_ctl3$_ctl0")){
				myRequest = new HTTPRequest("https://recorder.dupageco.org/common.aspx?PT=RESULTS", HTTPRequest.POST);
				myRequest.setHeader("Referer", "https://recorder.dupageco.org/common.aspx?PT=RESULTS");
				addParams = isolateParams(resp, FORM_NAME);
				myRequest.setPostParameter("__VIEWSTATE", addParams.get("__VIEWSTATE"));
				myRequest.setPostParameter("__EVENTVALIDATION", addParams.get("__EVENTVALIDATION"));
				myRequest.setPostParameter("__EVENTTARGET", "_ctl0$mainPlaceHolder$_ctl0$CountySpecificResults1$dgResults$_ctl3$_ctl0");
				response = process(myRequest);
				resp = response.getResponseAsString();
				String capcha = getCapchaAsString(3);
				if(capcha == null || capcha.length() != 5) {
					return "";
				}
				Form form = new SimpleHtmlParser(resp).getForm("aspnetForm");
				String url = "https://recorder.dupageco.org/" + form.action;
				myRequest = new HTTPRequest(url, HTTPRequest.POST);
				myRequest.setHeader("Referer", url);
				addParams = isolateParams(resp, "aspnetForm");
				myRequest.setPostParameter("__VIEWSTATE", addParams.get("__VIEWSTATE"));
				myRequest.setPostParameter("__EVENTVALIDATION", addParams.get("__EVENTVALIDATION"));
				myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:txtCode", capcha);
				myRequest.setPostParameter("_ctl0:mainPlaceHolder:_ctl0:cmdFreeSubmit", "Submit");
				response = process(myRequest);
				resp = response.getResponseAsString();
			}
				
			
			return response.getResponseAsString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			setAttribute("onBeforeRequest", Boolean.FALSE);
		}
		return null;
	}
	
	public String getFormPageForCertDate(String link){

		HTTPRequest request;
		String page;
		
		request = new HTTPRequest(link, HTTPRequest.GET);
		page = execute(request);
		
		
		return page;
		
	}
}

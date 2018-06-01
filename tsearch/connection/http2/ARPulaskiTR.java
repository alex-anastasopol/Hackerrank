/**
 * 
 */
package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.utils.StringUtils.urlDecode;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.utils.FileUtils;

/**
 * @author Mihai Dediu
  */
public class ARPulaskiTR extends HttpSite {
	
	private static final Pattern	TAX_CC_PAYMENTS_PATTERN	= Pattern.compile("TAX.CC.PAYMENTS_(\\d+).html");
	private String taxCcPaymentsNumber = "85"; 
	
	@Override
	public LoginResponse onLogin() {
		
		String link = getCrtServerLink() + "/cgi-bin/webshell.asp";
		Calendar c = Calendar.getInstance();
		String webiohandle = c.getTimeInMillis()+"";
		
		Map<String,String> parameters = new HashMap<String,String>();

		parameters.put("GATEWAY","GATEWAY");
		parameters.put("CGISCRIPT","webshell.asp");
		parameters.put("FINDDEFKEY","TAX.MAIN");
		parameters.put("XEVENT","VERIFY");
		parameters.put("WEBIOHANDLE",webiohandle);
		parameters.put("BROWSER","IE");
		parameters.put("MYPARENT","px");
		parameters.put("APPID","tax");
		parameters.put("WEBWORDSKEY","SAMPLE");
		parameters.put("DEVPATH","/GSASYS/PULASKI/TAX");
		parameters.put("OPERCODE","PUBLIC");
		parameters.put("PASSWD","PASS");
		
		HTTPRequest loginRequest = new HTTPRequest(link,HTTPRequest.POST);
		loginRequest.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)");
		
		for(Entry<String,String> param : parameters.entrySet()) {
			loginRequest.setPostParameter(param.getKey(), param.getValue());
		}
			
		process(loginRequest);
		
		setAttribute("WEBIOHANDLE", webiohandle);
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
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
		String link = getCrtServerLink() + "/cgi-bin/webshell.asp";
		
		String xevent = req.getPostFirstParameter("XEVENT");
		if("STDLIST".equals(xevent)) {
			/* Intermediary results query */
			Map<String,String> parameters = new HashMap<String,String>();
			
			parameters.put("SELVERB_1","=");
			parameters.put("SELVERB_2","=");
			parameters.put("SELVERB_3","BEGINS");
			parameters.put("SELVERB_4","BEGINS");
			parameters.put("SELVERB_5","BEGINS");
			parameters.put("SELVERB_6","BEGINS");
			parameters.put("CODEITEMNM","");
			parameters.put("CURRPROCESS","TAX.CC.PAYMENTS");
			parameters.put("CURRVAL","");
			parameters.put("FINDDEFKEY","TAX.CC.PAYMENTS");
			parameters.put("LINENBR","");
			parameters.put("NEEDRECORDS","3");
			parameters.put("NPROMPTS","6");
			parameters.put("PARENT","STDFIND");
			parameters.put("FINDTIMES","6");
			parameters.put("RTNVAL","SUBMIT");
			parameters.put("STDID","");
			parameters.put("XEVENT","STDLIST");
			parameters.put("WEBIOHANDLE",(String)getAttribute("WEBIOHANDLE"));
			parameters.put("GATEWAY","GATEWAY");
			parameters.put("APPID","tax");
			parameters.put("DEVAPPID","");
			parameters.put("DEVPATH","/GSASYS/PULASKI/TAX");
			parameters.put("OPERCODE","PUBLIC");
			parameters.put("STDURL","/tax_pro_green.taxlogo.html");
			parameters.put("WINDOWNAME","update");
			
			req.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)");
			
			for(Entry<String,String> param : parameters.entrySet()) {
				req.setPostParameter(param.getKey(), param.getValue());
			}
		}
		
		if("READREC".equals(xevent)) {
			/* Detailed results query */
			Map<String,String> parameters = new HashMap<String,String>();
			
			parameters.put("WEBIOHANDLE",(String)getAttribute("WEBIOHANDLE"));
			parameters.put("GATEWAY","");
			parameters.put("APPID","tax");
			parameters.put("DEVAPPID","");
			parameters.put("DEVPATH","/GSASYS/PULASKI/TAX");
			parameters.put("OPERCODE","PUBLIC");
			parameters.put("STDURL","/tax_pro_green.taxlogo.html");
			parameters.put("WINDOWNAME","update");
			parameters.put("XEVENT","STDHUB");
			parameters.put("FINDDEFKEY","TAX.CC.PAYMENTS");
			parameters.put("PARENT","STDLIST");
			parameters.put("RTNVAL","");
			parameters.put("NAPPID","");
			parameters.put("NDEVPATH","");
			parameters.put("NBTNNM","");
			parameters.put("NEXT.EVENT","STDHUB");
			parameters.put("NEXT.EVENT.ID","TAX.CC.PAYMENTS");
			parameters.put("ORIGPARENT","STDFIND");
			parameters.put("HUBTOPOST","");
			parameters.put("CODEITEMNM","");
			
			HTTPRequest request = new HTTPRequest(link,HTTPRequest.POST);
			
			request.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)");
			for(Entry<String,String> param : parameters.entrySet()) {
				request.setPostParameter(param.getKey(), param.getValue());
			}
			String page = execute(request);
			if(!page.contains("LOADING TAX.CC.PAYMENTS FORM")) {
				System.out.println("[ARPulaskiTR] Error getting detailed data");
			}
			
			Matcher taxCcPaymentsMatcher = TAX_CC_PAYMENTS_PATTERN.matcher(page);
			if(taxCcPaymentsMatcher.find()) {
				taxCcPaymentsNumber = taxCcPaymentsMatcher.group(1);
			}
						
			req.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)");
			parameters = new LinkedHashMap<String,String>();
			parameters.put("WEBIOHANDLE",(String)getAttribute("WEBIOHANDLE"));
			parameters.put("GATEWAY","FL");
			parameters.put("APPID","tax");
			parameters.put("DEVAPPID","");
			parameters.put("DEVPATH","/GSASYS/PULASKI/TAX");
			parameters.put("OPERCODE","PUBLIC");
			parameters.put("STDURL","/tax_pro_green.taxlogo.html");
			parameters.put("WINDOWNAME","update");
			parameters.put("CHANGED","0");
			parameters.put("CODEITEMNM","");
			parameters.put("CURRPANEL","1");
			parameters.put("CURRPROCESS","TAX.CC.PAYMENTS");
			parameters.put("CURRVAL","1");
			parameters.put("FINDDEFKEY","TAX.CC.PAYMENTS");
			parameters.put("HUBFILE","TAX.FILE");
			parameters.put("LINENBR","-1");
			parameters.put("NEEDRECORDS","-1");
			parameters.put("NPKEYS","0");
			parameters.put("PARENT","STDHUB");
			parameters.put("PREVVAL","0");
			parameters.put("STDID", req.getPostFirstParameter("STDID"));
			parameters.put("SUBMITCOUNT","2");
			parameters.put("WEBEVENTPATH","/GSASYS/TKT/TKT.ADMIN/WEB_EVENT");
			parameters.put("XEVENT", req.getPostFirstParameter("XEVENT"));
			parameters.put("WCVARS","");
			parameters.put("WCVALS","");
			
			req.removePostParameters("STDID");
			req.removePostParameters("XEVENT");
			
			
			Vector<String> postParamOrder = new Vector<String>();
			
			for(Entry<String,String> param : parameters.entrySet()) {
				postParamOrder.add(param.getKey());
				req.setPostParameter(param.getKey(), param.getValue());
			}
			req.setPostParameterOrder(postParamOrder);
			req.setHeader("Referer", "http://public.pulaskicountytreasurer.net/TAX_web_html/tax_pro_green.css_TAX.CC.PAYMENTS_" + taxCcPaymentsNumber + ".html");
			
		}
		
		try{
			HTTPResponse httpResponse = process(req);				
			String htmlResponse = httpResponse.getResponseAsString();
			
			Pattern pattern = Pattern.compile("self.location=\"([^\"]+)\"");
			Matcher matcher = pattern.matcher(htmlResponse);
			
			if(matcher.find()) {
				String linkFinalPage = matcher.group(1);
				req.modifyURL(getCrtServerLink() + linkFinalPage);
				req.setMethod(HTTPRequest.GET);
				
				httpResponse = process(req);				
				htmlResponse = httpResponse.getResponseAsString();
				
			}
			
			httpResponse.is = IOUtils.toInputStream(htmlResponse);
			req.setBypassResponse(httpResponse);
			

		} finally {
			
			// mark that we are out of treating onBeforeRequest 
			setAttribute("onBeforeRequest", Boolean.FALSE);
			
		}
	}
	
	public String getDetailsHtmlTemplate() {
		String link = getCrtServerLink() + "/TAX_web_html/tax_pro_green.css_TAX.CC.PAYMENTS_" + taxCcPaymentsNumber + ".html";
		HTTPRequest req = new HTTPRequest(link);
		req.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)");
		return execute(req);
	}
	
	public String getReceiptsInfo(String instrumentNo, String receiptIndex) {
		
		String link = getCrtServerLink() + "/cgi-bin/webshell.asp";
		HTTPRequest req = new HTTPRequest(link,HTTPRequest.POST);
		
		Map<String,String> parameters = new HashMap<String,String>();
		
		parameters.put("WEBIOHANDLE",(String)getAttribute("WEBIOHANDLE"));
		parameters.put("GATEWAY","PB,NOLOCK,1,1");
		parameters.put("APPID","tax");
		parameters.put("DEVAPPID","");
		parameters.put("DEVPATH","/GSASYS/PULASKI/TAX");
		parameters.put("OPERCODE","PUBLIC");
		parameters.put("STDURL","/tax_pro_green.taxlogo.html");
		parameters.put("WINDOWNAME","update");
		parameters.put("CHANGED","1");
		parameters.put("CODEITEMNM",receiptIndex);
		parameters.put("CURRPANEL","1");
		parameters.put("CURRPROCESS","TAX.CC.PAYMENTS");
		parameters.put("CURRVAL","Receipts");
		parameters.put("FINDDEFKEY","TAX.CC.PAYMENTS");
		parameters.put("HUBFILE","TAX.FILE");
		parameters.put("LINENBR","2");
		parameters.put("NEEDRECORDS","1");
		parameters.put("NPKEYS","0");
		parameters.put("PARENT","STDHUB");
		parameters.put("PREVVAL","");
		parameters.put("STDID",instrumentNo);
		parameters.put("SUBMITCOUNT","1");
		parameters.put("WEBEVENTPATH","/GSASYS/TKT/TKT.ADMIN/WEB_EVENT");
		parameters.put("XEVENT","POSTBACK");
		
		for(Entry<String,String> param : parameters.entrySet()) {
			req.setPostParameter(param.getKey(), param.getValue());
		}
		
		req.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)");
		return execute(req);
	}
	
	public String getChargeForLastYear(String instrumentNo, String origOrCurrent) {
		
		boolean wantOriginal = "original".equals(origOrCurrent);
		
		String link = getCrtServerLink() + "/cgi-bin/webshell.asp"; 
		HTTPRequest req = new HTTPRequest(link,HTTPRequest.POST);
		
		Map<String,String> parameters = new HashMap<String,String>();
		
		parameters.put("WEBIOHANDLE",(String)getAttribute("WEBIOHANDLE"));
		parameters.put("GATEWAY","PB,NOLOCK,1,1");
		parameters.put("APPID","tax");
		parameters.put("DEVAPPID","");
		parameters.put("DEVPATH","/GSASYS/PULASKI/TAX");
		parameters.put("OPERCODE","PUBLIC");
		parameters.put("STDURL","/tax_pro_green.taxlogo.html");
		parameters.put("WINDOWNAME","update");
		parameters.put("CHANGED","1");
		parameters.put("CODEITEMNM",wantOriginal?"WTKCB_20_1":"WTKCB_21_1");
		parameters.put("CURRPANEL","1");
		parameters.put("CURRPROCESS","TAX.CC.PAYMENTS");
		parameters.put("CURRVAL","&nbsp;&#1758;&nbsp;");
		parameters.put("FINDDEFKEY","TAX.CC.PAYMENTS");
		parameters.put("HUBFILE","TAX.FILE");
		parameters.put("LINENBR","0");
		parameters.put("NEEDRECORDS","1");
		parameters.put("NPKEYS","0");
		parameters.put("PARENT","STDHUB");
		parameters.put("PREVVAL","");
		parameters.put("STDID",instrumentNo);
		parameters.put("SUBMITCOUNT","4");
		parameters.put("WEBEVENTPATH","/GSASYS/TKT/TKT.ADMIN/WEB_EVENT");
		parameters.put("XEVENT","POSTBACK");

		for(Entry<String,String> param : parameters.entrySet()) {
			req.setPostParameter(param.getKey(), param.getValue());
		}
		
		req.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)");
		return execute(req);
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (res == null) {
			return;
		}		
		if(status != STATUS_LOGGING_IN && (res == null || res.returnCode == 0)){
				destroySession();    		
		}
	
		String content = "";
		if (res.getContentType().contains("text/html")){
			content = res.getResponseAsString();
		}
		
		if (content.contains("You are not Logged in or have been Logged out")){
			setForceRelogin(res);
		} 
	}
	
	public void setForceRelogin(HTTPResponse res){
		res.returnCode = 500; //I want this request to be done again
		status = STATUS_NOT_KNOWN;
		res.is = new ByteArrayInputStream("Please repeat search, official site forced us to relogin.".getBytes());
		res.body = "Please repeat search, official site forced us to relogin.";
	}
	
	public static void main(String... args) {
		String temp ="GATEWAY=GATEWAY&CGISCRIPT=webshell.asp&FINDDEFKEY=TAX.MAIN&XEVENT=VERIFY&WEBIOHANDLE=1285321488158&BROWSER=N&MYPARENT=px&APPID=tax&WEBWORDSKEY=SAMPLE&DEVPATH=%2FGSASYS%2FPULASKI%2FTAX&OPERCODE=PUBLIC&PASSWD=PASS";
		temp  = "CURRPANEL=1&DEVAPPID=&SUBMITCOUNT=2&STDURL=%2Ftax_pro_green.taxlogo.html&XEVENT=READREC&APPID=tax&WEBIOHANDLE=1307711097016&CODEITEMNM=&WEBEVENTPATH=%2FGSASYS%2FTKT%2FTKT.ADMIN%2FWEB_EVENT&PREVVAL=0&DEVPATH=%2FGSASYS%2FPULASKI%2FTAX&PARENT=STDHUB&OPERCODE=PUBLIC&LINENBR=-1&GATEWAY=FL&HUBFILE=TAX.FILE&CURRPROCESS=TAX.CC.PAYMENTS&CHANGED=0&WCVARS=&NPKEYS=0&CURRVAL=1&WINDOWNAME=update&STDID=552898P&NEEDRECORDS=-1&FINDDEFKEY=TAX.CC.PAYMENTS&WCVALS=";
		temp = "WEBIOHANDLE=1307704397679&GATEWAY=FL&APPID=tax&DEVAPPID=&DEVPATH=%2FGSASYS%2FPULASKI%2FTAX&OPERCODE=PUBLIC&STDURL=%2Ftax_pro_green.taxlogo.html&WINDOWNAME=update&CHANGED=0&CODEITEMNM=&CURRPANEL=1&CURRPROCESS=TAX.CC.PAYMENTS&CURRVAL=1&FINDDEFKEY=TAX.CC.PAYMENTS&HUBFILE=TAX.FILE&LINENBR=-1&NEEDRECORDS=-1&NPKEYS=0&PARENT=STDHUB&PREVVAL=0&STDID=552898P&SUBMITCOUNT=2&WEBEVENTPATH=%2FGSASYS%2FTKT%2FTKT.ADMIN%2FWEB_EVENT&XEVENT=READREC&WCVARS=&WCVALS";
		for(String str : temp.split("&")) {
			String[] x = str.split("=");
			if(x.length==1) {
				System.out.println("parameters.put(\""+x[0]+"\",\""+""+"\");");
			}else {
				System.out.println("parameters.put(\""+x[0]+"\",\""+urlDecode(x[1])+"\");");
			}
		}
		
		//String apns = getApnTestCases();
		//FileUtils.writeTextFile("C:\\Documents and Settings\\Mihai\\Desktop\\ARPulaskiTR-apns-1.txt",apns);
		//System.out.println(apns);
		
		//String legals = getLegalTestCases();
		//FileUtils.writeTextFile("C:\\Documents and Settings\\Mihai\\Desktop\\ARPulaskiTR-legals-1.txt",legals);
		//System.out.println(legals);
		
		 // The list of files can also be retrieved as File objects
		/*String apnFile = FileUtils.readFilePreserveNewLines("C:\\Documents and Settings\\Mihai\\Desktop\\ARPulaskiTR-apns-1.txt");
		String[] apns = apnFile.split("\\n");
		
		for(String apn : apns) {
			apn = apn.trim();
			ResultMap m = new ResultMap();
			try {
				String contents = FileUtils.readFilePreserveNewLines("C:\\Documents and Settings\\Mihai\\Desktop\\ARPulaskiTR\\"+apn+".txt");
				String name ="";
				Pattern addressP = Pattern.compile("parent.MainForm.TXF_1.value=\"([^\"]*?)\";");
				Matcher addressM = addressP.matcher(contents);
				if(addressM.find()) {
					name = addressM.group(1);
				}
				//System.out.println("++++++++++++++++++++++++++++++++++++++++++");
				//System.out.println(apn);
				System.out.println(name);
				//ro.cst.tsearch.servers.functions.ARPulaskiTR.parseName(m,name);
				//System.out.println(m.get("PartyNameSet").toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}*/
	}
	
	/**
	 * Get legal description from detail pages for testcases
	 * @return
	 */
	@SuppressWarnings("unused")
	private static String getLegalTestCases() {
		String legalInfo = "";
		
		String apnFile = FileUtils.readFilePreserveNewLines("C:\\Documents and Settings\\Mihai\\Desktop\\ARPulaskiTR-apns-1.txt");
		String[] apns = apnFile.split("\\n");
		
		int i = 1;
		for(String apn : apns) {
			apn = apn.trim();
			System.out.println("Processing record "+ i + " of " + apns.length);
			i++;
			HTTPRequest req = new HTTPRequest("http://public.pulaskicountytreasurer.net/cgi-bin/webshell.asp",HTTPRequest.POST);
			req.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)");
			Map<String,String> parameters = new HashMap<String,String>();
			parameters = new HashMap<String,String>();
			parameters.put("WEBIOHANDLE","1242818086426");
			parameters.put("GATEWAY","FL");
			parameters.put("APPID","tax");
			parameters.put("DEVAPPID","");
			parameters.put("DEVPATH","/GSASYS/PULASKI/TAX");
			parameters.put("OPERCODE","PUBLIC");
			parameters.put("STDURL","/tax_pro_green.taxlogo.html");
			parameters.put("WINDOWNAME","update");
			parameters.put("CHANGED","0");
			parameters.put("CODEITEMNM","");
			parameters.put("CURRPANEL","1");
			parameters.put("CURRPROCESS","TAX.CC.PAYMENTS");
			parameters.put("CURRVAL","1");
			parameters.put("FINDDEFKEY","TAX.CC.PAYMENTS");
			parameters.put("HUBFILE","TAX.FILE");
			parameters.put("LINENBR","0");
			parameters.put("NEEDRECORDS","-1");
			parameters.put("NPKEYS","0");
			parameters.put("PARENT","STDHUB");
			parameters.put("PREVVAL","0");
			parameters.put("STDID",apn);
			parameters.put("SUBMITCOUNT","2");
			parameters.put("WEBEVENTPATH","/GSASYS/TKT/TKT.ADMIN/WEB_EVENT");
			parameters.put("XEVENT","READREC");
			parameters.put("WCVARS","");
			parameters.put("WCVALS","");
			
			for(Entry<String,String> param : parameters.entrySet()) {
				req.setPostParameter(param.getKey(), param.getValue());
			}
			try {	
				HTTPResponse httpResponse = HttpSite.executeSimplePostRequest(req);				
				String htmlResponse = httpResponse.getResponseAsString();
				FileUtils.writeTextFile("C:\\Documents and Settings\\Mihai\\Desktop\\ARPulaskiTR\\"+apn+".txt",htmlResponse);
			}catch(Exception e){ 
				e.printStackTrace();
			}
			// Matcher legalMatcher = legalPattern.matcher(htmlResponse);
			//legalInfo += "APN => " + apn + "\n";
			//while(legalMatcher.find()) {
			//	legalInfo += legalMatcher.group(1) + "=>" + " " + legalMatcher.group(2) + "\n";
			//}
		}
		return legalInfo;
	}
	
	/**
	 * Get 1000 APN's from the intermediary results for testcases
	 * @return
	 */
	@SuppressWarnings("unused")
	private static String getApnTestCases() {
		String apns = "";
		
		Pattern apnPattern = Pattern.compile("RecSel\\('(\\w+)','\\w+'\\)");
		for(int page = 1; page < 54; page++) {
			HTTPRequest req = new HTTPRequest("http://public.pulaskicountytreasurer.net/cgi-bin/webshell.asp",HTTPRequest.POST);
			req.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)");
			Map<String,String> parameters = new HashMap<String,String>();
			parameters.put("SELVERB_1","=");
			parameters.put("ANS_1","");
			parameters.put("SELVERB_2","=");
			parameters.put("ANS_2","");
			parameters.put("SELVERB_3","BEGINS");
			parameters.put("ANS_3","Smith J");
			parameters.put("SELVERB_4","BEGINS");
			parameters.put("ANS_4","");
			parameters.put("SELVERB_5","BEGINS");
			parameters.put("ANS_5","");
			parameters.put("SELVERB_6","BEGINS");
			parameters.put("ANS_6","");
			parameters.put("CODEITEMNM","");
			parameters.put("CURRPROCESS","TAX.CC.PAYMENTS");
			parameters.put("CURRVAL","Smith J");
			parameters.put("FINDDEFKEY","TAX.CC.PAYMENTS");
			parameters.put("LINENBR","");
			parameters.put("NEEDRECORDS","3");
			parameters.put("NPROMPTS","6");
			parameters.put("PARENT","STDFIND");
			parameters.put("FINDTIMES","6");
			parameters.put("RTNVAL","SUBMIT");
			parameters.put("STDID","");
			parameters.put("XEVENT","STDLIST");
			parameters.put("WEBIOHANDLE","1242818086426");
			parameters.put("GATEWAY","GATEWAY");
			parameters.put("APPID","tax");
			parameters.put("DEVAPPID","");
			parameters.put("DEVPATH","/GSASYS/PULASKI/TAX");
			parameters.put("OPERCODE","PUBLIC");
			parameters.put("STDURL","/tax_pro_green.taxlogo.html");
			parameters.put("WINDOWNAME","update");
			parameters.put("XNGRPS","54");
			parameters.put("NEXTGRP",page+"");
			
			for(Entry<String,String> param : parameters.entrySet()) {
				req.setPostParameter(param.getKey(), param.getValue());
			}
			try {
				HTTPResponse httpResponse = HttpSite.executeSimplePostRequest(req);				
				String htmlResponse = httpResponse.getResponseAsString();
				Matcher apnMatcher = apnPattern.matcher(htmlResponse);
				while(apnMatcher.find()) {
					apns+=apnMatcher.group(1)+"\n";
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		return apns;
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getSiteLink();
		int idx = link.indexOf(".net");
		if(idx == -1){
			throw new RuntimeException("County " + getDataSite().getName() + " not supported by this class!");
		}
		return link;
	}
	
}

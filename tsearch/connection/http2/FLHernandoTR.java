package ro.cst.tsearch.connection.http2;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.htmlparser.Node;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;

public class FLHernandoTR extends HttpSite {

	public FLHernandoTR() {
		super();
		singleLineCookies = true;
	}

	public String					ASPNET_SESSION_ID		= "";

	public String					PARENT_SUBSESSION_ID	= "";
	public String					TRANSACTION_NR			= "";
	public String					SUBSESSION_ID			= "";
	public String					FORM					= "";
	public String					SID						= "";
	public String					ACTION_STRING			= "";

	final Pattern					FORM_NAME_PAT			= Pattern.compile("(?is)\\\"FormName\\\":\\\"([^\\\"]+)");
	final Pattern					LISTBOX_NAME_PAT		= Pattern.compile("(?is)\\\"ActiveControlName\\\":\\\"([^\\\"]+)");
	final Pattern					TRANSACTION_NR_PAT		= Pattern.compile("(?is)\\\"TransactionNr\\\":\\\"([^\\\"]+)");
	final String[]					LABELS_MAIN				= { "FormName", "TransactionNr", "ParentSubSessionId", "SessionDataSubSessionId", "SubSessionId",
															"Items", "BehaviourState", "SessionId", "FormAction", "FormStyle" };
	final String[]					LABELS_ITEMS			= { "Type", "Name", "Value" };
	final String[]					LABELS_BEHAVIOR_STATE	= { "Events", "ActiveControlName", "PrevActiveControlName", "FormSessions" };
	final String[]					LABELS_EVENTS			= { "Source", "Routine", "EventType", "Args" };

	final String[]					JSON_STRING				= { "listboxDataDateTimeUtc", "formName", "listboxName", "ValueSend", "ItemsDisplayed", "Sorted" };

	final String[]					ROUTINE					= { "", "DISP-CRITERIA", "CHOICE-MADE" };
	final String[]					EVENT_TYPE				= { "", "OnClickEvent", "OnSelectEvent", "OnXMitEvent" };
	
	public static final int OWNER_NAME_TYPE = 0;
	public static final int ADDRESS_TYPE = 1;
	public static final int PARCEL_TYPE = 2;
	public static final int KEY_TYPE = 3;
	public static final int LEGAL_TYPE = 4;
	
	int lastSearchType = -1;

	public LoginResponse onLogin() {
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		HTTPRequest req = new HTTPRequest(getSiteLink());
		HTTPResponse res = process(req);
		String response = res.getResponseAsString();
		
		HtmlParser3 htmlParser3 = new HtmlParser3(response);
		Node linkNode = htmlParser3.getNodeById("AppFrameNoFramework");
		String link = req.getURL();
		link = link.substring(0, link.lastIndexOf("/")+1);
		if (linkNode != null) {
			link += linkNode.toHtml().replaceAll("(?is).*?\\bsrc=\"([^\"]+)\".*", "$1");

		}
		req = new HTTPRequest(link);
		req.setMethod(HTTPRequest.GET);
		res = process(req);
		response = res.getResponseAsString();

		if (response.indexOf("Application error") != -1) {
			return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
		}

		Form shpForm = new SimpleHtmlParser(response).getForm("theform");
		if (shpForm != null) {
			setIDs(response, "theform");
			Pattern sidPat = Pattern.compile("(?is)\\?sessionid=(\\d+)$");
			Matcher sidMat = sidPat.matcher(res.getLastURI().toString());
			if (sidMat.find()) {
				this.SID = sidMat.group(1);
			}
			String url = req.getURL();
			String sendValues = "";
			try {
				sendValues = getSendValueJSONString(req, true, ROUTINE[0], EVENT_TYPE[3], null);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			String redirectLocation = req.getRedirectLocation();
			if (!org.apache.commons.lang.StringUtils.defaultString(redirectLocation).isEmpty()) {
				int sessionIdIndex = redirectLocation.lastIndexOf("?");
				if (sessionIdIndex > 0) {
					String sessionId = redirectLocation.substring(sessionIdIndex);
					url = url.replaceAll(url.substring(url.lastIndexOf("/")), "/AmtAjaxPage.aspx") + sessionId;
					url = url.replaceFirst("(?is)^http\\b", "https");
					req = new HTTPRequest(url, HTTPRequest.POST);
					req.setPostParameter("sendValues", sendValues);
					// press 'Continue'
					res = process(req);
					response = res.getResponseAsString();
					Pattern goToPagePat = Pattern.compile("(?is)\\{\\\"GotoPage\\\":\\\"([^\\\"]+)");// to extract PV000.aspx
																										// {"GotoPage":"../Default/PV000.aspx","DeltaPacket":{...
					Matcher goToPageMat = goToPagePat.matcher(response);
					if (goToPageMat.find()) {
						String goToPage = goToPageMat.group(1);
						goToPage = goToPage.replaceAll("(?is)\\A\\.\\.", "").toUpperCase();
						url = url.replaceAll("(?is)AmtAjaxPage.aspx.*", "Forms" + goToPage) + sessionId;
						req = new HTTPRequest(url, HTTPRequest.GET);
						response = execute(req);
					}

					shpForm = new SimpleHtmlParser(response).getForm("theform");
					if (shpForm != null) {
						setIDs(response, "theform");
					}
				}
			}
		
		}
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		// don't do anything during logging in
		if (status != STATUS_LOGGED_IN) {
			return;
		}

		// don't do anything while we're already inside a onBeforeRequest call
		if (getAttribute("onBeforeRequest") == Boolean.TRUE) {
			return;
		}

		// mark that we're treating onBeforeRequest
		setAttribute("onBeforeRequest", Boolean.TRUE);
		
		boolean isInDetails = false;
		
		int currentSearchType = -1;
		
		String val = req.getPostFirstParameter("Value");
		if ("N".equals(val)||"B".equals(val)) {		//next or back
			currentSearchType = lastSearchType;
		} else {
			if ("OWNER_AMT".equals(req.getPostFirstParameter(("ActiveControlName")))) {
				currentSearchType = OWNER_NAME_TYPE;
			} else if ("STR_NAME_AMT".equals(req.getPostFirstParameter("ActiveControlName"))) {
				currentSearchType = ADDRESS_TYPE;
			} else if ("IN_PAR_NUM_AMT".equals(req.getPostFirstParameter("ActiveControlName"))) {
				currentSearchType = PARCEL_TYPE;
			} else if ("ALT_KEY_AMT".equals(req.getPostFirstParameter("ActiveControlName"))) {
				currentSearchType = KEY_TYPE;
			} else if ("R".equals(req.getPostFirstParameter("AmtRadioButtonGroup"))) {
				currentSearchType = LEGAL_TYPE;
			}
		}
		
		if ((lastSearchType!=-1 && currentSearchType!=lastSearchType)) {
			onLogin();
		}
		lastSearchType = currentSearchType;
		
		req.setURL(req.getURL() + "?sessionid=" + this.SID);

		try {
			if (req.getPostFirstParameter("AmtRadioButtonGroup") != null) {// go to Location search, Real Property

				ArrayList<String[]> typeNameValueList = new ArrayList<String[]>();
				String[] typeNameValue = new String[] { "AmtRadioButtonGroup", "SEARCH_TYPE_AMT", req.getPostFirstParameter("AmtRadioButtonGroup") };
				typeNameValueList.add(typeNameValue);

				String sendValues = getSendValueJSONString(req, true, ROUTINE[1], EVENT_TYPE[1], typeNameValueList);

				String url = req.getURL();
				url = url.replaceAll("(?is)(/AmtAjaxPage.aspx)", this.ACTION_STRING + "$1");
				url = url.replaceFirst("(?is)^http\\b", "https");
				HTTPRequest newReq = new HTTPRequest(url, HTTPRequest.POST);
				newReq.setPostParameter("sendValues", sendValues);
				HTTPResponse httpResp = process(newReq);
				String resp = httpResp.getResponseAsString();
				
				String listBox = getListBox(resp);
				String formNm = getFormName(resp);
				
				setTransactionNr(resp);
				
				String jsonString = getJSONString(newReq, formNm, listBox);
											
				url = url.replaceAll("(?is)AmtAjaxPage\\.aspx", "AmtAjaxListBoxPage.aspx");
				newReq = new HTTPRequest(url, HTTPRequest.POST);
				newReq.setPostParameter("jsonString", jsonString);
				httpResp = process(newReq);
				resp = httpResp.getResponseAsString();

			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {

			if (req.getPostFirstParameter("PA_SEC_AMT") != null) { // real Property search
				ArrayList<String[]> typeNameValueList = getPropertySearchListFromReq(req);
				String sendValues = getSendValueJSONString(req, false, ROUTINE[0], EVENT_TYPE[3], typeNameValueList);

				String url = req.getURL();
				url = url.replaceAll("(?is)(/AmtAjaxPage.aspx)", this.ACTION_STRING + "$1");
				HTTPRequest newReq = new HTTPRequest(url, HTTPRequest.POST);
				newReq.setPostParameter("sendValues", sendValues);
				HTTPResponse httpResp = process(newReq);
				String resp = httpResp.getResponseAsString();
				
				String listBox = getListBox(resp);
				String formNm = getFormName(resp);
				
				setTransactionNr(resp);
				
				String jsonString = getJSONString(newReq, formNm, listBox);
											
				url = url.replaceAll("(?is)AmtAjaxPage\\.aspx", "AmtAjaxListBoxPage.aspx");
				newReq = new HTTPRequest(url, HTTPRequest.POST);
				newReq.setPostParameter("jsonString", jsonString);
				httpResp = process(newReq);
				resp = httpResp.getResponseAsString();
				
				httpResp.contentType = "text/html; charset=utf-8";
				// bypass response
				httpResp.is = IOUtils.toInputStream(resp);
				req.setBypassResponse(httpResp);
				
			
			} else if (req.getPostFirstParameter("STR_NAME_AMT") != null) { // address search
				ArrayList<String[]> typeNameValueList = getAddressSearchListFromReq(req);

				String sendValues = getSendValueJSONString(req, false, ROUTINE[0], EVENT_TYPE[3], typeNameValueList);

				String url = req.getURL();
				url = url.replaceAll("(?is)(/AmtAjaxPage.aspx)", this.ACTION_STRING + "$1");
				HTTPRequest newReq = new HTTPRequest(url, HTTPRequest.POST);
				newReq.setPostParameter("sendValues", sendValues);
				HTTPResponse httpResp = process(newReq);
				String resp = httpResp.getResponseAsString();
				
				String listBox = getListBox(resp);
				String formNm = getFormName(resp);
				
				setTransactionNr(resp);
				
				String jsonString = getJSONString(newReq, formNm, listBox);
											
				url = url.replaceAll("(?is)AmtAjaxPage\\.aspx", "AmtAjaxListBoxPage.aspx");
				newReq = new HTTPRequest(url, HTTPRequest.POST);
				newReq.setPostParameter("jsonString", jsonString);
				httpResp = process(newReq);
				resp = httpResp.getResponseAsString();
				
				httpResp.contentType = "text/html; charset=utf-8";
				// bypass response
				httpResp.is = IOUtils.toInputStream(resp);
				req.setBypassResponse(httpResp);
				
				
			} else if (req.getPostFirstParameter("Value") != null && req.getPostFirstParameter("listboxFileName") != null) {// details

				ArrayList<String[]> typeNameValueList = new ArrayList<String[]>();

				String[] typeNameValue = new String[] { "AmtListBox", req.getPostFirstParameter("listboxFileName").toUpperCase(), req.getPostFirstParameter("Value").toUpperCase() };
				typeNameValueList.add(typeNameValue);

				String sendValues = getSendValueJSONString(req, false, ROUTINE[2], EVENT_TYPE[2], typeNameValueList);

				String url = req.getURL();
				url = url.replaceAll("(?is)(/AmtAjaxPage.aspx)", this.ACTION_STRING + "$1");
				HTTPRequest newReq = new HTTPRequest(url, HTTPRequest.POST);
				newReq.setPostParameter("sendValues", sendValues);
				HTTPResponse httpResp = process(newReq);
				String resp = httpResp.getResponseAsString();
				
				setTransactionNr(resp);
				
				Pattern goToPagePat = Pattern.compile("(?is)\\{\\\"GotoPage\\\":\\\"[\\.]*([^\\\"]+)");//to extract PV001.aspx {"GotoPage":"../Default/PV001.aspx","DeltaPacket":{...
				Matcher goToPageMat = goToPagePat.matcher(resp);
				if (goToPageMat.find()){
					String goToPage = goToPageMat.group(1);
					goToPage = goToPage.replaceAll("(?is)\\A\\.\\.", "");
					url = url.replaceAll("(?is)/AmtAjaxPage.aspx", "/Forms" + goToPage);
					url = url.replaceFirst("(?is)^http\\b", "https");
					newReq = new HTTPRequest(url, HTTPRequest.GET);
					httpResp = process(newReq);
					resp = httpResp.getResponseAsString();
					
					setTransactionNrFromForm(resp, "aspnetForm");
					// bypass response
					httpResp.is = IOUtils.toInputStream(resp);
					req.setBypassResponse(httpResp);
				}
				
				isInDetails = true;
				
			} else if(req.getPostFirstParameter("Type") != null && "AmtButtonGroup".equals(req.getPostFirstParameter("Type"))){ //prev-next link
				
				String type = "", name = "", value = "";
				HashMap<String, ParametersVector> params = req.getPostParameters();
				if (params != null){
					for(Map.Entry<String, ParametersVector> entry: params.entrySet()){
						if (entry.getKey().matches("Type")){
							type = entry.getValue().toString();
						} else if (entry.getKey().matches("Name")){
							name = entry.getValue().toString();
						}  else if (entry.getKey().matches("Value")){
							value = entry.getValue().toString();
						}
					}
				}
								
				ArrayList<String[]> typeNameValueList = new ArrayList<String[]>();
				
				String[] typeNameValue = new String[]{type, name, value};
				typeNameValueList.add(typeNameValue);

				String sendValues = getSendValueJSONString(req, false, ROUTINE[0], EVENT_TYPE[3], typeNameValueList);

				String url = req.getURL();
				url = url.replaceAll("(?is)(-fl\\.us)", "$1" + this.ACTION_STRING);
				url = url.replaceFirst("(?is)^http\\b", "https");
				HTTPRequest newReq = new HTTPRequest(url, HTTPRequest.POST);
				newReq.setPostParameter("sendValues", sendValues);
				HTTPResponse httpResp = process(newReq);
				String resp = httpResp.getResponseAsString();

				if (name.contains("IN_PAR_NUM_AMT") || name.contains("ALT_KEY_AMT")) {
					Pattern goToPagePat = Pattern.compile("(?is)\\{\\\"GotoPage\\\":\\\"[\\.]*([^\\\"]+)");// to extract PV001.aspx
																											// {"GotoPage":"../Default/PV001.aspx","DeltaPacket":{...
					Matcher goToPageMat = goToPagePat.matcher(resp);
					if (goToPageMat.find()) {
						String goToPage = goToPageMat.group(1);
						goToPage = goToPage.replaceAll("(?is)\\A\\.\\.", "");
						url = url.replaceAll("(?is)AmtAjaxPage.aspx", goToPage);
						newReq = new HTTPRequest(url, HTTPRequest.GET);
						httpResp = process(newReq);
						resp = httpResp.getResponseAsString();
					}
				} else {
					
					String listBox = getListBox(resp);
					String formNm = getFormName(resp);
					
					setTransactionNr(resp);
					
					String jsonString = "";
					try {
						jsonString = getJSONString(newReq, formNm, listBox);
					} catch (JSONException e) {
						e.printStackTrace();
					}
								
					url = url.replaceAll("(?is)AmtAjaxPage\\.aspx", "AmtAjaxListBoxPage.aspx");
					newReq = new HTTPRequest(url, HTTPRequest.POST);
					newReq.setPostParameter("jsonString", jsonString);
					httpResp = process(newReq);
					resp = httpResp.getResponseAsString();
										
					httpResp.contentType = "text/html; charset=utf-8";
				}
				
				setTransactionNr(resp);
				
				// bypass response
				httpResp.is = IOUtils.toInputStream(resp);
				req.setBypassResponse(httpResp);
			} else { //name search, parcel search, alt key search
				
				String name = "", value = "";//, activeControlName = "", prevActiveControlName = "";
				HashMap<String, ParametersVector> params = req.getPostParameters();
				if (params != null){
					for(Map.Entry<String, ParametersVector> entry: params.entrySet()){
						if (entry.getKey().matches("Name")){
							name = entry.getValue().toString();
						}  else if (entry.getKey().matches("Value")){
							value = entry.getValue().toString();
						}
					}
				}
								
				ArrayList<String[]> typeNameValueList = new ArrayList<String[]>();
				
				String[] typeNameValue = new String[]{"AmtTextBox", name, value};
				typeNameValueList.add(typeNameValue);

				String sendValues = getSendValueJSONString(req, false, ROUTINE[0], EVENT_TYPE[3], typeNameValueList);

				String url = req.getURL().replaceFirst("(?i)(/AmtAjaxPage\\.aspx)", this.ACTION_STRING + "$1");
				HTTPRequest newReq = new HTTPRequest(url, HTTPRequest.POST);
				newReq.setPostParameter("sendValues", sendValues);
				HTTPResponse httpResp = process(newReq);
				String resp = httpResp.getResponseAsString();

				if (name.contains("IN_PAR_NUM_AMT") || name.contains("ALT_KEY_AMT")) {
					Pattern goToPagePat = Pattern.compile("(?is)\\{\\\"GotoPage\\\":\\\"[\\.]*([^\\\"]+)");// to extract PV001.aspx
																											// {"GotoPage":"../Default/PV001.aspx","DeltaPacket":{...
					Matcher goToPageMat = goToPagePat.matcher(resp);
					if (goToPageMat.find()) {
						String goToPage = goToPageMat.group(1);
						goToPage = goToPage.replaceAll("(?is)\\A\\.\\.", "");
						url = url.replaceAll("(?is)/AmtAjaxPage.aspx", "/Forms" + goToPage);
						newReq = new HTTPRequest(url, HTTPRequest.GET);
						httpResp = process(newReq);
						resp = httpResp.getResponseAsString();
						isInDetails = true;
					}
				} else {
					
					String listBox = getListBox(resp);
					String formNm = getFormName(resp);
					
					setTransactionNr(resp);
					
					String jsonString = "";
					try {
						jsonString = getJSONString(newReq, formNm, listBox);
					} catch (JSONException e) {
						e.printStackTrace();
					}
								
					url = url.replaceAll("(?is)AmtAjaxPage\\.aspx", "AmtAjaxListBoxPage.aspx");
					newReq = new HTTPRequest(url, HTTPRequest.POST);
					newReq.setPostParameter("jsonString", jsonString);
					httpResp = process(newReq);
					resp = httpResp.getResponseAsString();
										
					httpResp.contentType = "text/html; charset=utf-8";
				}
				
				setTransactionNr(resp);
				
				if (resp.indexOf("Request processing has faulted") == -1){
					// bypass response
					httpResp.is = IOUtils.toInputStream(resp);
					req.setBypassResponse(httpResp);
				}
			}
		} catch (Exception e) {
			
		}
		finally {
				
			// mark that we are out of treating onBeforeRequest 
			setAttribute("onBeforeRequest", Boolean.FALSE);
			if (isInDetails){//if passed to details, destroy session
				status = STATUS_NOT_KNOWN;
				destroySession();
			}
		}

	}	
	
	public void setForceRelogin(HTTPResponse res){
		res.returnCode = 500; //I want this request to be done again
		status = STATUS_NOT_KNOWN;
		res.is = new ByteArrayInputStream("Please repeat search, official site forced us to relogin.".getBytes());
		res.body = "Please repeat search, official site forced us to relogin.";
	}

	private String getSendValueJSONString(HTTPRequest req, boolean logging, String routine, String eventType, ArrayList<String[]> typeNameValues) throws JSONException{
				
		String activeControlName = "", prevActiveControlName = "";
		JSONObject jsonObject = new JSONObject();

		jsonObject.put(LABELS_MAIN[0], this.FORM);
		jsonObject.put(LABELS_MAIN[1], this.TRANSACTION_NR);
		jsonObject.put(LABELS_MAIN[2], this.PARENT_SUBSESSION_ID);
		jsonObject.put(LABELS_MAIN[3], this.SUBSESSION_ID);

		JSONArray items = new JSONArray();
		
		if (typeNameValues == null){//only in onlogin!!!
			
		} else {
			for (String[] typeNameValue : typeNameValues) {// when searching by address, real property the size of list typeNameValues is greater than 1
				JSONObject jsonItemsObject = new JSONObject();
				jsonItemsObject.put(LABELS_ITEMS[0], typeNameValue[0]);
				jsonItemsObject.put(LABELS_ITEMS[1], typeNameValue[1]);
				jsonItemsObject.put(LABELS_ITEMS[2], typeNameValue[2]);
				items.put(jsonItemsObject);

				if ("STR_NAME_AMT".equals(typeNameValue[1])) {
					activeControlName = "STR_NAME_AMT";
				}
				prevActiveControlName += "#" + typeNameValue[1];
			}
		}

		jsonObject.put(LABELS_MAIN[4], "1");

		JSONArray events = new JSONArray();
		
		JSONObject jsonEventsObject = new JSONObject();
		if (logging) {
			if (typeNameValues == null) {
				jsonEventsObject.put(LABELS_EVENTS[0], "CONTINUE_AMT");
			} else {
				jsonEventsObject.put(LABELS_EVENTS[0], "SEARCH_TYPE_AMT");
			}
		} else {
			if (prevActiveControlName.contains("SRCH_DIR_AMT")) {
				jsonEventsObject.put(LABELS_EVENTS[0], "SRCH_DIR_AMT");
			} else if (prevActiveControlName.contains("SRCH_LIST_AMT")) {// search name
				jsonEventsObject.put(LABELS_EVENTS[0], "SRCH_LIST_AMT");
				activeControlName = "SRCH_LIST_AMT";
			} else if (prevActiveControlName.contains("IN_PAR_NUM_AMT")) {// search parcel no
				jsonEventsObject.put(LABELS_EVENTS[0], "SUBMIT_AMT");
				activeControlName = "IN_PAR_NUM_AMT";
			} else if (prevActiveControlName.contains("ALT_KEY_AMT")) {// search alt key
				jsonEventsObject.put(LABELS_EVENTS[0], "");
				activeControlName = "ALT_KEY_AMT";
			} else if (prevActiveControlName.contains("STR_NAME_AMT") || prevActiveControlName.contains("STR_NUM_AMT")
					|| prevActiveControlName.contains("PA_")) {// search address, search Real property
				jsonEventsObject.put(LABELS_EVENTS[0], "SUBMIT_AMT");
				activeControlName = "SUBMIT_AMT";
			} else {
				jsonEventsObject.put(LABELS_EVENTS[0], "");
			}
		}
		jsonEventsObject.put(LABELS_EVENTS[1], routine);
		jsonEventsObject.put(LABELS_EVENTS[2], eventType);
		jsonEventsObject.put(LABELS_EVENTS[3], new JSONArray());

		events.put(jsonEventsObject);
		
		JSONObject jsonBehaviourStateObject = new JSONObject();
		jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[0], events);
		if (logging) {
			if (typeNameValues == null) {
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[1], "CONTINUE_AMT");
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[2], "");
			} else {
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[1], "SEARCH_TYPE_AMT");
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[2], "SUBMIT_AMT");
			}
		} else {
			if ("STR_NAME_AMT".equals(activeControlName)) {
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[1], "STR_NAME_AMT");
			} else if ("IN_PAR_NUM_AMT".equals(activeControlName)) {
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[1], "SUBMIT_AMT");
			} else {
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[1], activeControlName);
			}
			if (prevActiveControlName.contains("STR_APT_AMT")) {
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[2], "STR_APT_AMT");
			} else if (prevActiveControlName.contains("STR_DIR_AMT")) {
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[2], "STR_DIR_AMT");
			} else if (prevActiveControlName.contains("STR_SUFFDIR_AMT")) {
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[2], "STR_SUFFDIR_AMT");
			} else if (prevActiveControlName.contains("STR_NUM_AMT")) {
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[2], "STR_NUM_AMT");
			} else if (prevActiveControlName.contains("IN_PAR_NUM_AMT")) {// search parcel no
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[2], "IN_PAR_NUM_AMT");
			} else if (prevActiveControlName.contains("ALT_KEY_AMT")) {// search alt key
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[2], "SUBMIT_AMT");
			} else if (prevActiveControlName.contains("PA_")) {// search Real property
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[2], (typeNameValues.get(0))[1]);
			} else if (prevActiveControlName.contains("SRCH_DIR_AMT")) {// next/prev
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[1], "SRCH_DIR_AMT");
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[2], "SRCH_LIST_AMT");
			} else {
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[1], "OWNER_AMT");
				jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[2], "SUBMIT_AMT");
			}
		}

		jsonBehaviourStateObject.put(LABELS_BEHAVIOR_STATE[3], new JSONArray());
		jsonObject.put(LABELS_MAIN[5], items);
		jsonObject.put(LABELS_MAIN[6], jsonBehaviourStateObject);
		jsonObject.put(LABELS_MAIN[7], this.SID);
		jsonObject.put(LABELS_MAIN[8], 0);
		jsonObject.put(LABELS_MAIN[9], "0");

		return jsonObject.toString();
	}

	private String getJSONString(HTTPRequest req, String form, String listBoxName) throws JSONException{
			
		JSONObject jsonObject = new JSONObject();

		jsonObject.put(JSON_STRING[0], "1/1/0001 12:00:00 AM");
		jsonObject.put(JSON_STRING[1], form);
		jsonObject.put(JSON_STRING[2], listBoxName);
		jsonObject.put(JSON_STRING[3], 1);
		jsonObject.put(JSON_STRING[4], 2);
		jsonObject.put(JSON_STRING[5], "false");

		return jsonObject.toString();
	}
	
	private ArrayList<String[]> getAddressSearchListFromReq(HTTPRequest req){
		
		ArrayList<String[]> typeNameValueList = new ArrayList<String[]>();

		if (req.getPostFirstParameter("STR_NAME_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("STR_NAME_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "STR_NAME_AMT", req.getPostFirstParameter("STR_NAME_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}

		if (req.getPostFirstParameter("STR_NUM_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("STR_NUM_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "STR_NUM_AMT", req.getPostFirstParameter("STR_NUM_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}
		if (req.getPostFirstParameter("STR_DIR_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("STR_DIR_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "STR_DIR_AMT", req.getPostFirstParameter("STR_DIR_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}
		
		if (req.getPostFirstParameter("STR_SUFFDIR_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("STR_SUFFDIR_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "STR_SUFFDIR_AMT", req.getPostFirstParameter("STR_SUFFDIR_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}
		
		if (req.getPostFirstParameter("STR_APT_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("STR_APT_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "STR_APT_AMT", req.getPostFirstParameter("STR_APT_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}
		
		return typeNameValueList;
	}
	
	private ArrayList<String[]> getPropertySearchListFromReq(HTTPRequest req){
		
		ArrayList<String[]> typeNameValueList = new ArrayList<String[]>();

		if (req.getPostFirstParameter("PA_SEC_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("PA_SEC_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "PA_SEC_AMT", req.getPostFirstParameter("PA_SEC_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}

		if (req.getPostFirstParameter("PA_BOOK_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("PA_BOOK_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "PA_BOOK_AMT", req.getPostFirstParameter("PA_BOOK_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}
		if (req.getPostFirstParameter("PA_TOWN_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("PA_TOWN_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "PA_TOWN_AMT", req.getPostFirstParameter("PA_TOWN_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}
		if (req.getPostFirstParameter("PA_RANGE_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("PA_RANGE_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "PA_RANGE_AMT", req.getPostFirstParameter("PA_RANGE_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}
		if (req.getPostFirstParameter("PA_SUBDIV_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("PA_SUBDIV_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "PA_SUBDIV_AMT", req.getPostFirstParameter("PA_SUBDIV_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}
		if (req.getPostFirstParameter("PA_BLOCK_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("PA_BLOCK_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "PA_BLOCK_AMT", req.getPostFirstParameter("PA_BLOCK_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}
		if (req.getPostFirstParameter("PA_LOT_AMT") != null) {
			if (!"".equals(req.getPostFirstParameter("PA_LOT_AMT").trim())) {
				String[] typeNameValue = new String[] { "AmtTextBox", "PA_LOT_AMT", req.getPostFirstParameter("PA_LOT_AMT") };
				typeNameValueList.add(typeNameValue);
			}
		}
		
		return typeNameValueList;
	}

	private void setTransactionNr(String resp) {

		Matcher transactionNrMat = TRANSACTION_NR_PAT.matcher(resp);
		if (transactionNrMat.find()) {
			this.TRANSACTION_NR = transactionNrMat.group(1);
		}
	}
	
	private void setIDs(String response, String form){
		Form shpForm = new SimpleHtmlParser(response).getForm(form);
		if (shpForm != null){
			boolean hasSubSess = false, hasCallerSess = false, hasFormName = false;
			
			setTransactionNrFromForm(response, form);
			
			List<ro.cst.tsearch.parser.SimpleHtmlParser.Input> shpList = shpForm.inputs;
			for (ro.cst.tsearch.parser.SimpleHtmlParser.Input inp : shpList) {
				if ("_SUBSESSION".equals(inp.name)) {
					this.SUBSESSION_ID = inp.value;
					hasSubSess = true;
				} else if ("_CALLERSESSION".equals(inp.name)) {
					this.PARENT_SUBSESSION_ID = inp.value;
					hasCallerSess = true;
				} else if ("_FORMNAME".equals(inp.name)) {
					this.FORM = inp.value;
					hasFormName = true;
				}
				
				if (hasSubSess && hasCallerSess && hasFormName)
					break;
			}
		}
	}
	
	private void setTransactionNrFromForm(String response, String form){
		Form shpForm = new SimpleHtmlParser(response).getForm(form);
		if (shpForm != null){
			List<ro.cst.tsearch.parser.SimpleHtmlParser.Input> shpList = shpForm.inputs;
			for (ro.cst.tsearch.parser.SimpleHtmlParser.Input inp : shpList) {
				if ("_TRANSACTIONNUMBER".equals(inp.name)) {
					this.TRANSACTION_NR = inp.value;
					break;
				}
			}
		}
		
	}

	private String getFormName(String resp) {

		String formNm = "";

		Matcher formNameMat = FORM_NAME_PAT.matcher(resp);
		if (formNameMat.find()) {
			formNm = formNameMat.group(1);
		}
		
		return formNm;
	}
	
	private String getListBox(String resp){
		
		String listBox = "";

		Matcher listboxNameMat = LISTBOX_NAME_PAT.matcher(resp);
		if (listboxNameMat.find()) {
			listBox = listboxNameMat.group(1);
		}
		
		return listBox;
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
		if (res.getContentType().contains("text/html")) {
			content = res.getResponseAsString();
		}

		if (content.indexOf("Received data expired, transaction numbers do not match") != -1||content.indexOf("Session transmit error")!=-1) {
			setForceRelogin(res);
		}
	}
	
	@Override
	public void onRedirect(HTTPRequest req) {
		String location = req.getRedirectLocation();
		if (location.contains("AmtUnhandledExceptionPage")) {
			Pattern sidPat = Pattern.compile("(?is)\\?sessionid=(\\d+)$");
			Matcher sidMat = sidPat.matcher(location);
			if (sidMat.find()) {
				this.SID = sidMat.group(1);
			}
		} else {
			HttpState httpState = getHttpClient().getState();
			Cookie[] cookies = httpState.getCookies();
			for (Cookie cookie : cookies) {
				if ("ASP.NET_SessionId".equals(cookie.getName())) {
					ASPNET_SESSION_ID = cookie.getName() + "=" + cookie.getValue();
				}
			}
		}
	}
	
}

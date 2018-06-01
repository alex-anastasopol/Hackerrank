package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.parser.SimpleHtmlParser;

public class NVClarkRO extends HttpSite {
	private static String DOC_TYPES_PARAM = "ctl00_ContentPlaceHolder1_DocTypeDropParcel_RadComboBox1_ClientState";
	private static String DOC_TYPES_USER_PARAM = "ctl00$ContentPlaceHolder1$DocTypeDropParcel$RadComboBox1";
	private static String START_DATE_PARAM = "ctl00$ContentPlaceHolder1$DatePickersParcel$StartDatePicker$dateInput";
	private static String END_DATE_PARAM = "ctl00$ContentPlaceHolder1$DatePickersParcel$EndDatePicker$dateInput";

	private String viewstate = null;
	private String eventvalidation = null;
	
	private static final String __VIEWSTATE = "__VIEWSTATE";
	private static final String __EVENTVALIDATION = "__EVENTVALIDATION";
	
	public static final String FORM_NAME = "aspnetForm";
	
	@Override
	public LoginResponse onLogin() {
		
		HTTPRequest request = new HTTPRequest(getSiteLink());
		String response = process( request ).getResponseAsString();		
		
		if(StringUtils.isBlank(response)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Cannot get response for " + getSiteLink());
		}
		try {
			Map<String,String> addParams = new SimpleHtmlParser(response).getForm(FORM_NAME).getParams();
			
			if(addParams == null || addParams.isEmpty()) {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Cannot get parameters for " + getSiteLink() + " from form " + FORM_NAME);
			}
			
			request.clearPostParameters();
			
			request.setMethod(HTTPRequest.POST);
			
			addParams.put("__EVENTARGUMENT", "5");
			addParams.put("__EVENTTARGET", "ctl00$ContentPlaceHolder1$RadTabStrip1");
			addParams.put("ctl00_TKSM_HiddenField", ";;AjaxControlToolkit,+Version=3.5.40412.0,+Culture=neutral,+PublicKeyToken=28f01b0e84b6d53e:en-US:47d532b1-93b1-4f26-a107-54e5292e1525:f2c8e708:de1feab2:720a52bf:f9cec9bc:1a2a8638");
			addParams.put("ctl00_ContentPlaceHolder1_RadTabStrip1_ClientState", "{\"selectedIndexes\":[\"5\"],\"logEntries\":[],\"scrollState\":{}}");
			addParams.put("ctl00$ContentPlaceHolder1$SearchRD", "AMMC,MARR,MC,MCCR");
			addParams.put("ctl00_ContentPlaceHolder1_DatePickers1_StartDatePicker_dateInput_ClientState", "{\"enabled\":true,\"emptyMessage\":\"\",\"minDateStr\":\"1/1/1900 0:0:0\",\"maxDateStr\":\"12/31/2099 0:0:0\"}");
			addParams.put("ctl00_ContentPlaceHolder1_TabPages_ClientState", "{\"selectedIndex\":5,\"changeLog\":[]}");
			
			
			addParams.remove("ctl00$ContentPlaceHolder1$Button1");
			addParams.remove("ctl00$ContentPlaceHolder1$ResetButton");
					
			request.addPostParameters(addParams);
			
			response = process( request ).getResponseAsString();		
			if(StringUtils.isBlank(response)) {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Cannot get response for second page on (POST) " + getSiteLink());
			}
			
			addParams = new SimpleHtmlParser(response).getForm(FORM_NAME).getParams();
			if(addParams == null || addParams.isEmpty()) {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Cannot get parameters second page on (POST) " + getSiteLink() + " from form " + FORM_NAME);
			}
			
			viewstate = addParams.get(__VIEWSTATE);
			eventvalidation = addParams.get(__EVENTVALIDATION);
			
		} catch (Exception e) {
			logger.error("Problem Parsing Form on NVClarkRO", e);
		}
		
		if(StringUtils.isBlank(viewstate) || StringUtils.isBlank(eventvalidation)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Cannot get __VIEWSTATE and/or __EVENTVALIDATION");
		}
		
		return LoginResponse.getDefaultSuccessResponse();
	}
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		HashMap<String, ParametersVector> params = req.getPostParameters();
		
		// Set doc types param.
		ParametersVector param = req.getPostParameter(DOC_TYPES_USER_PARAM);
		String value = "";
		if (param != null && param.size() > 0
				&& param.get(0).equals("--- ALL DOCUMENT TYPES ---") == false) {
			value = (String) param.get(0);
		}
		String docs = "{\"logEntries\":[],\"value\":\"\",\"text\":\"" + value
				+ "\",\"enabled\":true}";

		req.removePostParameters(DOC_TYPES_PARAM);
		req.setPostParameter(DOC_TYPES_PARAM, docs);

		// Set start date
		param = params.get(START_DATE_PARAM);
		if (param != null && param.size() > 0) {
			value = (String) param.get(0) + "-00-00-00";
			req.removePostParameters(START_DATE_PARAM);
			req.setPostParameter(START_DATE_PARAM, value);
		}
		// Set end date
		param = params.get(END_DATE_PARAM);
		if (param != null && param.size() > 0) {
			value = (String) param.get(0) + "-00-00-00";
			req.removePostParameters(END_DATE_PARAM);
			req.setPostParameter(END_DATE_PARAM, value);
		}
		
		req.removePostParameters(__VIEWSTATE);
		req.removePostParameters(__EVENTVALIDATION);
		
		req.setPostParameter(__VIEWSTATE, viewstate);
		req.setPostParameter(__EVENTVALIDATION, eventvalidation);
		
	}
}

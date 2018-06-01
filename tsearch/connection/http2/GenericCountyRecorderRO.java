package ro.cst.tsearch.connection.http2;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedImageAdapter;
import javax.media.jai.RenderedOp;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.types.GenericCountyRecorderRO.SelectLists;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TiffConcatenator;

import com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;

public class GenericCountyRecorderRO extends HttpSite {
	
	private static List<String> STATES = new ArrayList<String>();
	static {
		STATES.add("AZ");
		STATES.add("CO");
	}
	private static Map<String, String> AZ_COUNTY_CODES = new HashMap<String, String>();
	static {
		AZ_COUNTY_CODES.put("APACHE", "5");
		AZ_COUNTY_CODES.put("COCHISE", "18");
		AZ_COUNTY_CODES.put("GRAHAM", "1");
		AZ_COUNTY_CODES.put("GREENLEE", "2");
		AZ_COUNTY_CODES.put("LA PAZ", "3");
		AZ_COUNTY_CODES.put("NAVAJO", "4");
		AZ_COUNTY_CODES.put("SANTA CRUZ", "8");
	}
	private static Map<String, String> CO_COUNTY_CODES = new HashMap<String, String>();
	static {
		CO_COUNTY_CODES.put("BACA", "13");
		CO_COUNTY_CODES.put("CHEYENNE", "15");
		CO_COUNTY_CODES.put("DOLORES", "20");
		CO_COUNTY_CODES.put("HUERFANO", "17");
		CO_COUNTY_CODES.put("KIOWA", "12");
		CO_COUNTY_CODES.put("LINCOLN", "14");
		CO_COUNTY_CODES.put("SAGUACHE", "6");
		CO_COUNTY_CODES.put("SAN JUAN", "9");
		CO_COUNTY_CODES.put("SAN MIGUEL", "10");
		CO_COUNTY_CODES.put("SEDGWICK", "21");
		CO_COUNTY_CODES.put("TELLER", "19");
		CO_COUNTY_CODES.put("WASHINGTON", "22");
	}
	
	private final static String DISCLAIMER_LINK = "http://www.thecountyrecorder.com/Disclaimer.aspx";
	
	private final static String EVENTTARGET_PARAM = "__EVENTTARGET";
	private final static String EVENTARGUMENT_PARAM = "__EVENTARGUMENT";
	private final static String VIEWSTATE_PARAM = "__VIEWSTATE";
	private final static String EVENTVALIDATION_PARAM = "__EVENTVALIDATION";
	private final static String LASTFOCUS_PARAM = "__LASTFOCUS";
	private final static String POPULATE_LOG_PARAM = "TreeView1_PopulateLog";
	private final static String ASYNCPOST_PARAM = "__ASYNCPOST";
	
	private final static String ACCEPT_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$btnAccept";
	private final static String ACCEPT_VALUE = "Yes,+I+Accept";
	
	private final static String TRUE_VALUE = "true";
	private final static String OPEN_DOCUMENT_VALUE = "Open Document";
	private final static String LOAD_VALUE = "Load";
	
	private final static String SELECTED_NODE_PARAM = "TreeView1_SelectedNode";
	private final static String SELECTED_NODE_VALUE2 = "TreeView1t2";
	private final static String SELECTED_NODE_VALUE3 = "TreeView1t3";
	private final static String SELECTED_NODE_VALUE4 = "TreeView1t4";
	
	private final static String SELECT_STATES_PARAM = "ctl00$ctl00$ContentPlaceHolder_SelectCounty$ctl00$cboStatesV";
	private final static String SELECT_COUNTIES_PARAM = "ctl00$ctl00$ContentPlaceHolder_SelectCounty$ctl00$cboCountiesV";

	private final static String RECEPTION_NUMBER_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbReceptionNumber";
	public final static String BOOK_TYPE_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$cboBookType";
	public final static String BOOK_NUMBER_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$cboBookNumber";
	public final static String PAGE_NUMBER_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$cboPageNumber";
	private final static String TRACKING_ID_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbTrackingID";
	private final static String DOCTYPES_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$btnLoadDocumentTypes";
	private final static String SUBS_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$btnLoadSubdivsions";
	private final static String EXPAND_STATE_PARAM = "TreeView1_ExpandState";
	
	private final static String SCRIPT_MANAGER_PARAM = "ctl00$ctl00$MainContent$ScriptManager1";
	private final static String SCRIPT_MANAGER_STATES = "ctl00$ctl00$ContentPlaceHolder_SelectCounty$ctl00$UpdatePanelPublicSearchSearch2|" + SELECT_STATES_PARAM;
	private final static String SCRIPT_MANAGER_COUNTIES = "ctl00$ctl00$ContentPlaceHolder_SelectCounty$ctl00$UpdatePanelPublicSearchSearch2|" + SELECT_COUNTIES_PARAM;
	private final static String SCRIPT_MANAGER_BOOKTYPE = "ctl00$ctl00$MainContent$searchMainContent$ctl00$UpdatePanel1|" + BOOK_TYPE_PARAM;
	private final static String SCRIPT_MANAGER_BOOKNUMBER = "ctl00$ctl00$MainContent$searchMainContent$ctl00$UpdatePanel1|" + BOOK_NUMBER_PARAM;
	private final static String SCRIPT_MANAGER_DOCTYPES = "ctl00$ctl00$MainContent$searchMainContent$ctl00$UpdatePanelSearch2|" + DOCTYPES_PARAM;
	private final static String SCRIPT_MANAGER_SUBS1 = "ctl00$ctl00$MainContent$searchMainContent$ctl00$UpdatePanelSearch1|" + SUBS_PARAM;
	private final static String SCRIPT_MANAGER_SUBS2 = "ctl00$ctl00$MainContent$searchMainContent$ctl00$UpdatePanelSearch2|" + SUBS_PARAM;
		
	private final static String BUTTON1_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$Button1";
	private final static String BUTTON2_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$Button2";
	private final static String BUTTON3_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$Button3";
	
	private final static String RADIODOC_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$radioDocName";
	private final static String BUSINESS_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbBusinessName";
	private final static String LASTNAME_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbDocNameLast";
	private final static String FIRSTNAME_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbDocNameFirst";
	private final static String MIDDLENAME_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbDocNameMiddle";
	
	private final static String SEARCH_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$btnSearch1";
	private final static String SEARCH_VALUE = "Execute Search";
	
	private final static String RADIODOC_VALUE1 = "radioDocName1";
	private final static String RADIODOC_VALUE2 = "radioDocName2";
	
	public final static String GROUP_OR_TYPE_PARAM = "groupOrType";
	public final static String DOCUMENT_GROUP_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$cboDocumentGroup";
	public final static String DOCUMENT_TYPE_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$cboDocumentType";
	public final static String SUBDIVISION_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$cboSubdivision";
	private final static String RESULTS_PER_PAGE_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$cboResultsPerPage";
	
	private final static String DOCUMENT_GROUP_DEFAULT_VALUE = "-1";
	private final static String DOCUMENT_TYPE_DEFAULT_VALUE1 = "-1";
	private final static String SUBDIVISION_DEFAULT_VALUE1 = "-1";
	private final static String DOCUMENT_TYPE_DEFAULT_VALUE2 = "";
	private final static String SUBDIVISION_DEFAULT_VALUE2 = "";
	private final static String BOOK_TYPE_DEFAULT_VALUE = "";
	private final static String RESULTS_PER_PAGE_DEFAULT_VALUE = "10";
	private final static String DATE_END_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbDateEnd";
	
	private final static String IMAGE_ID = "MainContent_searchMainContent_ctl00_Image2";
	private final static String NEXT_BUTTON_ID = "MainContent_searchMainContent_ctl00_btnNext";
	private final static String GO_TO_PAGE_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbGoToPage";
	private final static String NEXT_PAGE_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$btnNext";
	private final static String TB_RECEPTION_NUMBER_PARAM = "tbReceptionNumber";
	
	private static String[] EMPTY_SEARCH_PARAMS = {"ctl00$ctl00$MainContent$searchMainContent$ctl00$tbDateStart", 
												   "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbDocNameLast",
		                                           "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbDocNameFirst",
		                                           "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbDocNameMiddle",
		                                           "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbLot",
		                                           "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbBlock",
		                                           "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbSection",
		                                           "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbTownship",
		                                           "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbRange"};
	private static String[] NAME_SEARCH_PARAMS = {DOCUMENT_TYPE_PARAM,
												  SUBDIVISION_PARAM,
												  RESULTS_PER_PAGE_PARAM};
	private static String[] VALUE_SEARCH_PARAMS = {DOCUMENT_TYPE_DEFAULT_VALUE1, SUBDIVISION_DEFAULT_VALUE1, RESULTS_PER_PAGE_DEFAULT_VALUE};
		
	private String certificationDateHtml = "";
	
	private String ultimateDocumentType = DOCUMENT_TYPE_DEFAULT_VALUE1;
	private String penultimateDocumentType = DOCUMENT_TYPE_DEFAULT_VALUE1;
	private String ultimateSubdivision = SUBDIVISION_DEFAULT_VALUE1;
	private String penultimateSubdivision = SUBDIVISION_DEFAULT_VALUE1;
	
	private final static String RECEPTION_NO_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbReceptionNo";
	private final static String BOOK_PAGE_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbBookPage";
	private final static String RECEPTION_DATE_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbReceptionDate";
	private final static String DOCUMENT_TYPE_PARAM_TB = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbDocumentType";
	private final static String PAGE_COUNT_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbPageCount";
	private final static String VIEW_IMAGE_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$btnViewImage";
	private final static String DESCRIPTION_PARAM = "ctl00$ctl00$MainContent$searchMainContent$ctl00$tbDescription";
	
	private final static String OPTION_VALUE_PATT = "(?is)<option[^>]+value=\"([^\"]*)\"[^>]*>([^<]*)</option>";
	
	private final static String BOOK_TYPE_SELECT = "MainContent_searchMainContent_ctl00_cboBookType";
	private final static String BOOK_NUMBER_SELECT = "MainContent_searchMainContent_ctl00_cboBookNumber";
	private final static String PAGE_NUMBER_SELECT = "MainContent_searchMainContent_ctl00_cboPageNumber";
	private final static String DOCUMENT_GROUP_SELECT = "MainContent_searchMainContent_ctl00_cboDocumentGroup";
	private final static String RESULTS_PER_PAGE_SELECT = "MainContent_searchMainContent_ctl00_cboResultsPerPage";
	private final static String DOCUMENT_TYPE_SELECT = "MainContent_searchMainContent_ctl00_cboDocumentType";
	private final static String SUBDIVISION_SELECT = "MainContent_searchMainContent_ctl00_cboSubdivision";
	
	@Override
	public LoginResponse onLogin() {
		
		setAttribute(HttpSite.SINGLE_LINE_COOKIES_IN_REDIRECT, Boolean.TRUE);
		
		String resp1 = "";
		HTTPRequest req1 = new HTTPRequest(getSiteLink(), HTTPRequest.GET);		//go to Introduction page
		try {
			resp1 = exec(req1).getResponseAsString();
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		if (resp1.contains("Use the links at left to open or search recorded documents.")) {
			return LoginResponse.getDefaultSuccessResponse();
		}
		
		if (resp1.contains("Disclaimer")) {
			
			String resp2 = "";
			HTTPRequest req2 = new HTTPRequest(DISCLAIMER_LINK + "?jsEnabled=1", HTTPRequest.POST);	//click on "Yes, I Accept"
			String viewState_resp1 = getParameterValueFromHtml(VIEWSTATE_PARAM, resp1);
			String eventValidation_resp1 = getParameterValueFromHtml(EVENTVALIDATION_PARAM, resp1);
			req2.setPostParameter(EVENTTARGET_PARAM, "");
			req2.setPostParameter(EVENTARGUMENT_PARAM, "");
			req2.setPostParameter(VIEWSTATE_PARAM, viewState_resp1);
			req2.setPostParameter(EVENTVALIDATION_PARAM, eventValidation_resp1);
			req2.setPostParameter(ACCEPT_PARAM, ACCEPT_VALUE);
			try {
				resp2 = exec(req2).getResponseAsString();
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
			
			String resp3 = "";
			HTTPRequest req3 = new HTTPRequest(getSiteLink(), HTTPRequest.POST);	//select the state
			String viewState_resp2 = getParameterValueFromHtml(VIEWSTATE_PARAM, resp2);
			String eventValidation_resp2 = getParameterValueFromHtml(EVENTVALIDATION_PARAM, resp2);
			String expandState_resp2 = getParameterValueFromHtml(EXPAND_STATE_PARAM, resp2);
			req3.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_STATES);
			req3.setPostParameter(EXPAND_STATE_PARAM, expandState_resp2);
			req3.setPostParameter(SELECTED_NODE_PARAM, SELECTED_NODE_VALUE2);
			req3.setPostParameter(EVENTTARGET_PARAM, SELECT_STATES_PARAM);
			req3.setPostParameter(EVENTARGUMENT_PARAM, "");
			req3.setPostParameter(POPULATE_LOG_PARAM, "");
			req3.setPostParameter(LASTFOCUS_PARAM, "");
			req3.setPostParameter(VIEWSTATE_PARAM, viewState_resp2);
			req3.setPostParameter(EVENTVALIDATION_PARAM, eventValidation_resp2);
			req3.setPostParameter(SELECT_STATES_PARAM, getState());
			req3.setPostParameter(SELECT_COUNTIES_PARAM, "");
			req3.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
			try {
				resp3 = exec(req3).getResponseAsString();
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
			
			HTTPRequest req4 = new HTTPRequest(getSiteLink(), HTTPRequest.POST);	//select the county
			String viewState_resp3 = getParameterValueFromText(VIEWSTATE_PARAM, resp3);
			String eventValidation_resp3 = getParameterValueFromText(EVENTVALIDATION_PARAM, resp3);
			String expandState_resp3 = getParameterValueFromHtml(EXPAND_STATE_PARAM, resp3);
			req4.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_COUNTIES);
			req4.setPostParameter(EXPAND_STATE_PARAM, expandState_resp3);
			req4.setPostParameter(SELECTED_NODE_PARAM, SELECTED_NODE_VALUE2);
			req4.setPostParameter(EVENTTARGET_PARAM, SELECT_COUNTIES_PARAM);
			req4.setPostParameter(EVENTARGUMENT_PARAM, "");
			req4.setPostParameter(POPULATE_LOG_PARAM, "");
			req4.setPostParameter(LASTFOCUS_PARAM, "");
			req4.setPostParameter(VIEWSTATE_PARAM, viewState_resp3);
			req4.setPostParameter(EVENTVALIDATION_PARAM, eventValidation_resp3);
			req4.setPostParameter(SELECT_STATES_PARAM, getState());
			req4.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
			req4.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
			try {
				exec(req4).getResponseAsString();
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
			
			String resp5 = "";												//go to Introduction page
			HTTPRequest req5 = new HTTPRequest(getSiteLink(), HTTPRequest.GET);
			try {
				resp5 = exec(req5).getResponseAsString();
				certificationDateHtml = resp5;
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
			
			if (resp5.contains("Use the links at left to open or search recorded documents.")) {
				return LoginResponse.getDefaultSuccessResponse();
			}
		}
		
		ultimateDocumentType = DOCUMENT_TYPE_DEFAULT_VALUE1;
		penultimateDocumentType = DOCUMENT_TYPE_DEFAULT_VALUE1;
		ultimateSubdivision = SUBDIVISION_DEFAULT_VALUE1;
		penultimateSubdivision = SUBDIVISION_DEFAULT_VALUE1;
		
		return LoginResponse.getDefaultFailureResponse();
	}
	
	public static String getSelect(String name, String text) {
		String result = "";
		Matcher ma = Pattern.compile("(?is)<select[^>]+id=\"" + name + "\"[^>]*>.*?</select>").matcher(text);
		if (ma.find()) {
			result = ma.group(0);
		}
		return result;
	}
	
	protected String getState() {
		DataSite dataSite = getDataSite();
		String state =  dataSite.getStateAbbreviation();
		if (STATES.contains(state)) {
			return state;
		}
		return "";
	}
	
	protected String getState(String state) {
		if (STATES.contains(state)) {
			return state;
		}
		return "";
	}
	
	protected String getCounty() {
		DataSite dataSite = getDataSite();
		String state =  dataSite.getStateAbbreviation();
		String county =  dataSite.getCountyName().toUpperCase();
		String res = "";
		if ("AZ".equals(state)) {
			res = AZ_COUNTY_CODES.get(county);
		} else if ("CO".equals(state)) {
			res = CO_COUNTY_CODES.get(county);
		} 
		if (res!=null) {
			return res;
		}
		return "";
	}
	
	protected String getCounty(String state, String county) {
		county = county.toUpperCase();
		String res = "";
		if ("AZ".equals(state)) {
			res = AZ_COUNTY_CODES.get(county);
		} else if ("CO".equals(state)) {
			res = CO_COUNTY_CODES.get(county);
		} 
		if (res!=null) {
			return res;
		}
		return "";
	}
	
	//for first book number value from first book type return 1
	//for first book number value from second book type return 2
	//... else return 0 (normal situation)
	protected int getSituation(String bookType, String bookNumber) {
		
		DataSite dataSite = getDataSite();
		String state =  dataSite.getStateAbbreviation();
		String county =  dataSite.getCountyName();
		
		HashMap<String, SelectLists> cachedSelectLists = ro.cst.tsearch.servers.types.GenericCountyRecorderRO.getCachedSelectLists();
		if (cachedSelectLists!=null) {
			SelectLists selectLists = cachedSelectLists.get(state + county);
			if (selectLists!=null) {
				LinkedHashMap<String,LinkedHashMap<String,ArrayList<String>>> typeBookPage = selectLists.getTypeBookPageMap();
				if (typeBookPage!=null) {
					int i = 1;
					Iterator<Entry<String, LinkedHashMap<String, ArrayList<String>>>> it1 = typeBookPage.entrySet().iterator();
					while (it1.hasNext()) {
						Entry<String, LinkedHashMap<String, ArrayList<String>>> entry1 = it1.next();
						LinkedHashMap<String, ArrayList<String>> bookPage = entry1.getValue();
						Iterator<Entry<String, ArrayList<String>>> it2 = bookPage.entrySet().iterator();
						if (it2.hasNext()) {
							Entry<String, ArrayList<String>> entry2 = it2.next();
							if (bookNumber.equals(entry2.getKey())) {
								return i;
							}
						}
						i++;
					}
				}
			}
		}
		
		return 0;
	}
	
	//return second book number value for each book type (each situation)
	protected String getAdditionalBookNumber(int situation) {
		
		if (situation==0) {
			return "";
		}	
		
		ArrayList<String> secondBookNumberList = new ArrayList<String>();
		
		DataSite dataSite = getDataSite();
		String state =  dataSite.getStateAbbreviation();
		String county =  dataSite.getCountyName();
		
		HashMap<String, SelectLists> cachedSelectLists = ro.cst.tsearch.servers.types.GenericCountyRecorderRO.getCachedSelectLists();
		if (cachedSelectLists!=null) {
			SelectLists selectLists = cachedSelectLists.get(state + county);
			if (selectLists!=null) {
				LinkedHashMap<String,LinkedHashMap<String,ArrayList<String>>> typeBookPage = selectLists.getTypeBookPageMap();
				if (typeBookPage!=null) {
					Iterator<Entry<String, LinkedHashMap<String, ArrayList<String>>>> it1 = typeBookPage.entrySet().iterator();
					while (it1.hasNext()) {
						Entry<String, LinkedHashMap<String, ArrayList<String>>> entry1 = it1.next();
						LinkedHashMap<String, ArrayList<String>> bookPage = entry1.getValue();
						Iterator<Entry<String, ArrayList<String>>> it2 = bookPage.entrySet().iterator();
						String secondBookNumber = "";
						if (it2.hasNext()) {
							Entry<String, ArrayList<String>> entry2 = it2.next();
							if (it2.hasNext()) {
								entry2 = it2.next();
								secondBookNumber = entry2.getKey();
							}
						}
						secondBookNumberList.add(secondBookNumber);
					}
				}
			}
		}
		
		if (situation<=secondBookNumberList.size()) {
			return secondBookNumberList.get(situation-1); 
		}
		
		return "";
	}
	
	//return first page number value for each additional book number (each situation)
	protected String getAdditionalPageNumber(int situation) {
		
		if (situation==0) {
			return "";
		}
		
		ArrayList<String> pageNumberList = new ArrayList<String>();
		
		DataSite dataSite = getDataSite();
		String state =  dataSite.getStateAbbreviation();
		String county =  dataSite.getCountyName();
		
		HashMap<String, SelectLists> cachedSelectLists = ro.cst.tsearch.servers.types.GenericCountyRecorderRO.getCachedSelectLists();
		if (cachedSelectLists!=null) {
			SelectLists selectLists = cachedSelectLists.get(state + county);
			if (selectLists!=null) {
				LinkedHashMap<String,LinkedHashMap<String,ArrayList<String>>> typeBookPage = selectLists.getTypeBookPageMap();
				if (typeBookPage!=null) {
					Iterator<Entry<String, LinkedHashMap<String, ArrayList<String>>>> it1 = typeBookPage.entrySet().iterator();
					while (it1.hasNext()) {
						Entry<String, LinkedHashMap<String, ArrayList<String>>> entry1 = it1.next();
						LinkedHashMap<String, ArrayList<String>> bookPage = entry1.getValue();
						Iterator<Entry<String, ArrayList<String>>> it2 = bookPage.entrySet().iterator();
						String pageNumber = "";
						if (it2.hasNext()) {
							Entry<String, ArrayList<String>> entry2 = it2.next();
							if (it2.hasNext()) {
								entry2 = it2.next();
								ArrayList<String> list = entry2.getValue();
								if (list.size()>0) {
									pageNumber = list.get(0);
								}
							}
						}
						pageNumberList.add(pageNumber);
					}
				}
			}
		}
		
		if (situation<=pageNumberList.size()) {
			return pageNumberList.get(situation-1); 
		}
		
		return "";
	}
	
	//return the value for "TreeView1_SelectedNode" parameter for each situation
	protected String getAdditionalTreeVieParam(int situation) {
		
		if (situation==0) {
			return "";
		} else if (situation==1) {
			return "SELECTED_NODE_VALUE2";
		} else if (situation==2) {
			return "SELECTED_NODE_VALUE3";
		}
		
		return "";
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		
		if (req.getMethod()==HTTPRequest.POST) {
			
			String url = req.getURL();
			String viewState = "";
			String eventValidation = "";
			String expandState = "";
			
			String resp1 = "";
			HTTPRequest req1 = new HTTPRequest(url, HTTPRequest.GET);
			try {
				resp1 = exec(req1).getResponseAsString();
				viewState = getParameterValueFromHtml(VIEWSTATE_PARAM, resp1);
				eventValidation = getParameterValueFromHtml(EVENTVALIDATION_PARAM, resp1);
				expandState = getParameterValueFromHtml(EXPAND_STATE_PARAM, resp1);
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
			
			if (url.contains("Open.aspx")) {				//search by Reception Number, Book and Page or Tracking ID
				
				if (req.getPostFirstParameter(RECEPTION_NUMBER_PARAM)!=null) {		//search by Reception Number
					
					req.setPostParameter(EXPAND_STATE_PARAM, expandState);
					req.setPostParameter(SELECTED_NODE_PARAM, SELECTED_NODE_VALUE3);
					req.setPostParameter(EVENTTARGET_PARAM, "");
					req.setPostParameter(EVENTARGUMENT_PARAM, "");
					req.setPostParameter(POPULATE_LOG_PARAM, "");
					req.setPostParameter(LASTFOCUS_PARAM, "");
					req.setPostParameter(VIEWSTATE_PARAM, viewState);
					req.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
					req.setPostParameter(SELECT_STATES_PARAM, getState());
					req.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
					req.setPostParameter(BUTTON1_PARAM, OPEN_DOCUMENT_VALUE);
					req.setPostParameter(BOOK_TYPE_PARAM, BOOK_TYPE_DEFAULT_VALUE);
					req.setPostParameter(TRACKING_ID_PARAM, "");
					
				} else if (req.getPostFirstParameter(BOOK_TYPE_PARAM)!=null &&		//search by Book and Page
						   req.getPostFirstParameter(BOOK_NUMBER_PARAM)!=null &&
						   req.getPostFirstParameter(PAGE_NUMBER_PARAM)!=null) {
					
					String bookType = req.getPostFirstParameter(BOOK_TYPE_PARAM);
					if (bookType==null) {
						bookType = "";
					}
					String bookNumber = req.getPostFirstParameter(BOOK_NUMBER_PARAM);
					if (bookNumber==null) {
						bookNumber = "";
					}
					String pageNumber = req.getPostFirstParameter(PAGE_NUMBER_PARAM);
					if (pageNumber==null) {
						pageNumber = "";
					}
					
					int situation = getSituation(bookType, bookNumber);
					
					String additionalBookNumber = getAdditionalBookNumber(situation);
					String additionalPageNumber = getAdditionalPageNumber(situation);
					String treeViewParam = getAdditionalTreeVieParam(situation);
					
					String resp2 = "";
					HTTPRequest req2 = new HTTPRequest(url, HTTPRequest.POST);	//select book number
					req2.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_BOOKTYPE);
					req2.setPostParameter(EXPAND_STATE_PARAM, expandState);
					req2.setPostParameter(SELECTED_NODE_PARAM, treeViewParam);
					req2.setPostParameter(EVENTTARGET_PARAM, BOOK_TYPE_PARAM);
					req2.setPostParameter(EVENTARGUMENT_PARAM, "");
					req2.setPostParameter(POPULATE_LOG_PARAM, "");
					req2.setPostParameter(LASTFOCUS_PARAM, "");
					req2.setPostParameter(VIEWSTATE_PARAM, viewState);
					req2.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
					req2.setPostParameter(SELECT_STATES_PARAM, getState());
					req2.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
					req2.setPostParameter(RECEPTION_NUMBER_PARAM, "");
					req2.setPostParameter(BOOK_TYPE_PARAM, bookType);
					req2.setPostParameter(TRACKING_ID_PARAM, "");
					req2.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
					try {
						resp2 = exec(req2).getResponseAsString();
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
					
					if (situation>0) {	//make additional requests
						
						String addResp1 = "";
						HTTPRequest addReq1 = new HTTPRequest(url, HTTPRequest.POST);	//select the second option from Book Number
						String viewState_resp2 = getParameterValueFromText(VIEWSTATE_PARAM, resp2);
						String eventValidation_resp2 = getParameterValueFromText(EVENTVALIDATION_PARAM, resp2);
						String expandState_resp2 = getParameterValueFromText(EXPAND_STATE_PARAM, resp2);
						addReq1.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_BOOKNUMBER);
						addReq1.setPostParameter(EXPAND_STATE_PARAM, expandState_resp2);
						addReq1.setPostParameter(SELECTED_NODE_PARAM, treeViewParam);
						addReq1.setPostParameter(EVENTTARGET_PARAM, BOOK_NUMBER_PARAM);
						addReq1.setPostParameter(EVENTARGUMENT_PARAM, "");
						addReq1.setPostParameter(POPULATE_LOG_PARAM, "");
						addReq1.setPostParameter(LASTFOCUS_PARAM, "");
						addReq1.setPostParameter(VIEWSTATE_PARAM, viewState_resp2);
						addReq1.setPostParameter(EVENTVALIDATION_PARAM, eventValidation_resp2);
						addReq1.setPostParameter(SELECT_STATES_PARAM, getState());
						addReq1.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
						addReq1.setPostParameter(RECEPTION_NUMBER_PARAM, "");
						addReq1.setPostParameter(BOOK_TYPE_PARAM, bookType);
						addReq1.setPostParameter(BOOK_NUMBER_PARAM, additionalBookNumber);
						addReq1.setPostParameter(TRACKING_ID_PARAM, "");
						addReq1.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
						try {
							addResp1 = exec(addReq1).getResponseAsString();
						} catch(IOException e){
							logger.error(e);
							throw new RuntimeException(e);
						}
						
						String addResp2 = "";
						HTTPRequest addReq2 = new HTTPRequest(url, HTTPRequest.POST);	//select the first option from Book Number
						String viewState_resp3 = getParameterValueFromText(VIEWSTATE_PARAM, addResp1);
						String eventValidation_resp3 = getParameterValueFromText(EVENTVALIDATION_PARAM, addResp1);
						String expandState_resp3 = getParameterValueFromText(EXPAND_STATE_PARAM, addResp1);
						addReq2.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_BOOKNUMBER);
						addReq2.setPostParameter(EXPAND_STATE_PARAM, expandState_resp3);
						addReq2.setPostParameter(SELECTED_NODE_PARAM, treeViewParam);
						addReq2.setPostParameter(EVENTTARGET_PARAM, BOOK_NUMBER_PARAM);
						addReq2.setPostParameter(EVENTARGUMENT_PARAM, "");
						addReq2.setPostParameter(POPULATE_LOG_PARAM, "");
						addReq2.setPostParameter(LASTFOCUS_PARAM, "");
						addReq2.setPostParameter(VIEWSTATE_PARAM, viewState_resp3);
						addReq2.setPostParameter(EVENTVALIDATION_PARAM, eventValidation_resp3);
						addReq2.setPostParameter(SELECT_STATES_PARAM, getState());
						addReq2.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
						addReq2.setPostParameter(RECEPTION_NUMBER_PARAM, "");
						addReq2.setPostParameter(BOOK_TYPE_PARAM, bookType);
						addReq2.setPostParameter(BOOK_NUMBER_PARAM, bookNumber);
						addReq2.setPostParameter(PAGE_NUMBER_PARAM, additionalPageNumber);
						addReq2.setPostParameter(TRACKING_ID_PARAM, "");
						addReq2.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
						try {
							addResp2 = exec(addReq2).getResponseAsString();
						} catch(IOException e){
							logger.error(e);
							throw new RuntimeException(e);
						}
						
						viewState = getParameterValueFromText(VIEWSTATE_PARAM, addResp2);
						eventValidation = getParameterValueFromText(EVENTVALIDATION_PARAM, addResp2);
						
					} else {
						
						String resp3 = "";
						HTTPRequest req3 = new HTTPRequest(url, HTTPRequest.POST);	//select page number
						String viewState_resp2 = getParameterValueFromText(VIEWSTATE_PARAM, resp2);
						String eventValidation_resp2 = getParameterValueFromText(EVENTVALIDATION_PARAM, resp2);
						String expandState_resp2 = getParameterValueFromText(EXPAND_STATE_PARAM, resp2);
						req3.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_BOOKNUMBER);
						req3.setPostParameter(EXPAND_STATE_PARAM, expandState_resp2);
						req3.setPostParameter(SELECTED_NODE_PARAM, SELECTED_NODE_VALUE2);
						req3.setPostParameter(EVENTTARGET_PARAM, BOOK_NUMBER_PARAM);
						req3.setPostParameter(EVENTARGUMENT_PARAM, "");
						req3.setPostParameter(POPULATE_LOG_PARAM, "");
						req3.setPostParameter(LASTFOCUS_PARAM, "");
						req3.setPostParameter(VIEWSTATE_PARAM, viewState_resp2);
						req3.setPostParameter(EVENTVALIDATION_PARAM, eventValidation_resp2);
						req3.setPostParameter(SELECT_STATES_PARAM, getState());
						req3.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
						req3.setPostParameter(RECEPTION_NUMBER_PARAM, "");
						req3.setPostParameter(BOOK_TYPE_PARAM, bookType);
						req3.setPostParameter(BOOK_NUMBER_PARAM, bookNumber);
						req3.setPostParameter(TRACKING_ID_PARAM, "");
						req3.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
						try {
							resp3 = exec(req3).getResponseAsString();
						} catch(IOException e){
							logger.error(e);
							throw new RuntimeException(e);
						}
							
						viewState = getParameterValueFromText(VIEWSTATE_PARAM, resp3);
						eventValidation = getParameterValueFromText(EVENTVALIDATION_PARAM, resp3);
					}
										
					req.setPostParameter(EXPAND_STATE_PARAM, expandState);
					req.setPostParameter(SELECTED_NODE_PARAM, SELECTED_NODE_VALUE3);
					req.setPostParameter(EVENTTARGET_PARAM, "");
					req.setPostParameter(EVENTARGUMENT_PARAM, "");
					req.setPostParameter(POPULATE_LOG_PARAM, "");
					req.setPostParameter(LASTFOCUS_PARAM, "");
					req.setPostParameter(VIEWSTATE_PARAM, viewState);
					req.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
					req.setPostParameter(SELECT_STATES_PARAM, getState());
					req.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
					req.setPostParameter(BUTTON2_PARAM, OPEN_DOCUMENT_VALUE);
					req.setPostParameter(RECEPTION_NUMBER_PARAM, "");
					req.setPostParameter(TRACKING_ID_PARAM, "");
					
				} else if (req.getPostFirstParameter(TRACKING_ID_PARAM)!=null) {		//search by Tracking ID
					
					req.setPostParameter(EXPAND_STATE_PARAM, expandState);
					req.setPostParameter(SELECTED_NODE_PARAM, SELECTED_NODE_VALUE4);
					req.setPostParameter(EVENTTARGET_PARAM, "");
					req.setPostParameter(EVENTARGUMENT_PARAM, "");
					req.setPostParameter(POPULATE_LOG_PARAM, "");
					req.setPostParameter(LASTFOCUS_PARAM, "");
					req.setPostParameter(VIEWSTATE_PARAM, viewState);
					req.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
					req.setPostParameter(SELECT_STATES_PARAM, getState());
					req.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
					req.setPostParameter(BUTTON3_PARAM, OPEN_DOCUMENT_VALUE);
					
				}
			} else if (url.contains("Search.aspx")) {				//combined search
				
				String groupOrType = req.getPostFirstParameter(GROUP_OR_TYPE_PARAM);
				String nameType = req.getPostFirstParameter(RADIODOC_PARAM);
				String documentType = req.getPostFirstParameter(DOCUMENT_TYPE_PARAM);
				if (DOCUMENT_TYPE_DEFAULT_VALUE2.equals(documentType)) {
					documentType = DOCUMENT_TYPE_DEFAULT_VALUE1;
					req.removePostParameters(DOCUMENT_TYPE_PARAM);
					req.setPostParameter(DOCUMENT_TYPE_PARAM, documentType);
				}
				String subdivision = req.getPostFirstParameter(SUBDIVISION_PARAM);
				if (SUBDIVISION_DEFAULT_VALUE2.equals(subdivision)) {
					subdivision = SUBDIVISION_DEFAULT_VALUE1;
					req.removePostParameters(SUBDIVISION_PARAM);
					req.setPostParameter(SUBDIVISION_PARAM, subdivision);
				}
				String dateEnd = req.getPostFirstParameter(DATE_END_PARAM);
				
				if (!DOCUMENT_TYPE_DEFAULT_VALUE1.equals(documentType) && DOCUMENT_TYPE_DEFAULT_VALUE1.equals(ultimateDocumentType)) {
					
					String resp3 = "";
					HTTPRequest req3 = new HTTPRequest(url, HTTPRequest.POST);	//load document type list
					req3.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_DOCTYPES);
					req3.setPostParameter(EXPAND_STATE_PARAM, expandState);
					req3.setPostParameter(SELECTED_NODE_PARAM, SELECTED_NODE_VALUE4);
					req3.setPostParameter(EVENTTARGET_PARAM, "");
					req3.setPostParameter(EVENTARGUMENT_PARAM, "");
					req3.setPostParameter(POPULATE_LOG_PARAM, "");
					req3.setPostParameter(LASTFOCUS_PARAM, "");
					req3.setPostParameter(VIEWSTATE_PARAM, viewState);
					req3.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
					req3.setPostParameter(SELECT_STATES_PARAM, getState());
					req3.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
					req3.setPostParameter(RADIODOC_PARAM, nameType);
					req3.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
					req3.setPostParameter(DOCTYPES_PARAM, LOAD_VALUE);
					req3.setPostParameter(BUSINESS_PARAM, "");
					req3.setPostParameter(DOCUMENT_GROUP_PARAM, DOCUMENT_GROUP_DEFAULT_VALUE);
					req3.setPostParameter(DATE_END_PARAM, dateEnd);
					for (int i=0;i<EMPTY_SEARCH_PARAMS.length;i++) {
						req3.setPostParameter(EMPTY_SEARCH_PARAMS[i], "");
					}
					if (!SUBDIVISION_DEFAULT_VALUE1.equals(ultimateSubdivision)) {
						if (SUBDIVISION_DEFAULT_VALUE1.equals(subdivision)) {
							req3.setPostParameter(SUBDIVISION_PARAM, SUBDIVISION_DEFAULT_VALUE2);
						}
					} else {
						if (SUBDIVISION_DEFAULT_VALUE1.equals(ultimateSubdivision) && SUBDIVISION_DEFAULT_VALUE1.equals(penultimateSubdivision)) {
							req3.setPostParameter(SUBDIVISION_PARAM, SUBDIVISION_DEFAULT_VALUE1);
						} else {
							req3.setPostParameter(SUBDIVISION_PARAM, subdivision);
						}
					}
					req3.setPostParameter(NAME_SEARCH_PARAMS[0], VALUE_SEARCH_PARAMS[0]);
					req3.setPostParameter(NAME_SEARCH_PARAMS[2], VALUE_SEARCH_PARAMS[2]);
					try {
						resp3 = exec(req3).getResponseAsString();
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
					
					viewState = getParameterValueFromText(VIEWSTATE_PARAM, resp3);
					eventValidation = getParameterValueFromText(EVENTVALIDATION_PARAM, resp3);
					expandState = getParameterValueFromText(EXPAND_STATE_PARAM, resp3);
				}
				penultimateDocumentType = ultimateDocumentType;
				ultimateDocumentType = documentType;
				if (DOCUMENT_TYPE_DEFAULT_VALUE2.equals(ultimateDocumentType)) {
					ultimateDocumentType = DOCUMENT_TYPE_DEFAULT_VALUE1;
				}
				
				if (!SUBDIVISION_DEFAULT_VALUE1.equals(subdivision) && SUBDIVISION_DEFAULT_VALUE1.equals(ultimateSubdivision)) {
					
					String resp4 = "";
					HTTPRequest req4 = new HTTPRequest(url, HTTPRequest.POST);	//load subdivision list
					req4.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_SUBS2);
					req4.setPostParameter(EXPAND_STATE_PARAM, expandState);
					req4.setPostParameter(SELECTED_NODE_PARAM, SELECTED_NODE_VALUE4);
					req4.setPostParameter(EVENTTARGET_PARAM, "");
					req4.setPostParameter(EVENTARGUMENT_PARAM, "");
					req4.setPostParameter(POPULATE_LOG_PARAM, "");
					req4.setPostParameter(LASTFOCUS_PARAM, "");
					req4.setPostParameter(VIEWSTATE_PARAM, viewState);
					req4.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
					req4.setPostParameter(SELECT_STATES_PARAM, getState());
					req4.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
					req4.setPostParameter(RADIODOC_PARAM, nameType);
					req4.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
					req4.setPostParameter(SUBS_PARAM, LOAD_VALUE);
					req4.setPostParameter(BUSINESS_PARAM, "");
					req4.setPostParameter(DATE_END_PARAM, dateEnd);
					for (int i=0;i<EMPTY_SEARCH_PARAMS.length;i++) {
						req4.setPostParameter(EMPTY_SEARCH_PARAMS[i], "");
					}
					if (!DOCUMENT_TYPE_DEFAULT_VALUE1.equals(penultimateDocumentType)) {
						if (DOCUMENT_TYPE_DEFAULT_VALUE1.equals(documentType)) {
							req4.setPostParameter(DOCUMENT_TYPE_PARAM, DOCUMENT_TYPE_DEFAULT_VALUE2);
							req4.setPostParameter(DOCUMENT_GROUP_PARAM, DOCUMENT_GROUP_DEFAULT_VALUE);
						}
					} else {
						req4.setPostParameter(DOCUMENT_TYPE_PARAM, documentType);
					}
					for (int i=1;i<NAME_SEARCH_PARAMS.length;i++) {
						req4.setPostParameter(NAME_SEARCH_PARAMS[i], VALUE_SEARCH_PARAMS[i]);
					}
					try {
						resp4 = exec(req4).getResponseAsString();
					} catch(IOException e){
						logger.error(e);
						throw new RuntimeException(e);
					}
					
					viewState = getParameterValueFromText(VIEWSTATE_PARAM, resp4);
					eventValidation = getParameterValueFromText(EVENTVALIDATION_PARAM, resp4);
				}
				penultimateSubdivision = ultimateSubdivision;
				ultimateSubdivision = subdivision;
				if (SUBDIVISION_DEFAULT_VALUE2.equals(ultimateSubdivision)) {
					ultimateSubdivision = SUBDIVISION_DEFAULT_VALUE1;
				}
				
				req.setPostParameter(EXPAND_STATE_PARAM, expandState);
				req.setPostParameter(SELECTED_NODE_PARAM, SELECTED_NODE_VALUE4);
				req.setPostParameter(EVENTTARGET_PARAM, "");
				req.setPostParameter(EVENTARGUMENT_PARAM, "");
				req.setPostParameter(POPULATE_LOG_PARAM, "");
				req.setPostParameter(LASTFOCUS_PARAM, "");
				req.setPostParameter(VIEWSTATE_PARAM, viewState);
				req.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
				req.setPostParameter(SELECT_STATES_PARAM, getState());
				req.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
				req.setPostParameter(SEARCH_PARAM, SEARCH_VALUE);
				if (!DOCUMENT_TYPE_DEFAULT_VALUE1.equals(penultimateDocumentType)) {
					if (DOCUMENT_TYPE_DEFAULT_VALUE1.equals(documentType)) {
						req.removePostParameters(DOCUMENT_TYPE_PARAM);
						req.setPostParameter(DOCUMENT_TYPE_PARAM, DOCUMENT_TYPE_DEFAULT_VALUE2);
					}
				}
				if (!SUBDIVISION_DEFAULT_VALUE1.equals(penultimateSubdivision)) {
					if (SUBDIVISION_DEFAULT_VALUE1.equals(subdivision)) {
						req.removePostParameters(SUBDIVISION_PARAM);
						req.setPostParameter(SUBDIVISION_PARAM, SUBDIVISION_DEFAULT_VALUE2);
					}
				}
				
				req.removePostParameters(GROUP_OR_TYPE_PARAM);
				if ("group".equals(groupOrType)) {
					req.removePostParameters(DOCUMENT_TYPE_PARAM);
				} else if ("type".equals(groupOrType)) {
					req.removePostParameters(DOCUMENT_GROUP_PARAM);
				}
				if (RADIODOC_VALUE1.equals(nameType)) {			//person name
					req.removePostParameters(BUSINESS_PARAM);
				} else if (RADIODOC_VALUE2.equals(nameType)) {	//business name
					req.removePostParameters(LASTNAME_PARAM);
					req.removePostParameters(FIRSTNAME_PARAM);
					req.removePostParameters(MIDDLENAME_PARAM);
				}
			}
		}
	}
	
	public SelectLists getSelectLists(String state, String county) {
		if (this.status != STATUS_LOGGED_IN) {
			onLogin();
		}
		
		SelectLists selectLists = null;
		
		selectLists = new SelectLists();
//		selectLists.setDateModified(Calendar.getInstance());
			
		String siteLink = getSiteLink().replaceFirst("(?is)/Introduction\\.aspx.*", "");
		String expandState = "";
		String selectedNode = "";
		String viewState = "";
		String eventValidation = "";
		
		String resp6 = "";
		HTTPRequest req6 = new HTTPRequest(siteLink + "/Open.aspx", HTTPRequest.GET);
		try {
			resp6 = exec(req6).getResponseAsString();
			expandState = getParameterValueFromHtml(EXPAND_STATE_PARAM, resp6);
			selectedNode = getParameterValueFromHtml(SELECTED_NODE_PARAM, resp6);
			viewState = getParameterValueFromHtml(VIEWSTATE_PARAM, resp6);
			eventValidation = getParameterValueFromHtml(EVENTVALIDATION_PARAM, resp6);
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		if (!StringUtils.isEmpty(resp6)) {
			String bookTypeSelect = getSelect(BOOK_TYPE_SELECT, resp6);
			if (!StringUtils.isEmpty(bookTypeSelect)) {
				ArrayList<String> bookTypeList = new ArrayList<String>();
				LinkedHashMap<String, String> bookTypeMap = new LinkedHashMap<String, String>();
				Matcher ma1 = Pattern.compile(OPTION_VALUE_PATT).matcher(bookTypeSelect);
				while (ma1.find()) {
					String g1 = ma1.group(1).trim();
					String g2 = StringEscapeUtils.unescapeXml(ma1.group(2).trim());
					if (!StringUtils.isEmpty(g1) && !StringUtils.isEmpty(g2)) {
						bookTypeMap.put(g1, g2);
						bookTypeList.add(g1);
					}
				}
				if (bookTypeList.size()!=0) {
					selectLists.setBookTypeMap(bookTypeMap);
					
					LinkedHashMap<String,LinkedHashMap<String,ArrayList<String>>> typeBookPage = new LinkedHashMap<String,LinkedHashMap<String,ArrayList<String>>>();
					for (String bookType: bookTypeList) {
						LinkedHashMap<String,ArrayList<String>> bookPageMap = new LinkedHashMap<String,ArrayList<String>>();
						if (!StringUtils.isEmpty(bookType)) {
							String resp7 = "";
							HTTPRequest req7 = new HTTPRequest(siteLink + "/Open.aspx", HTTPRequest.POST);
							req7.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_BOOKTYPE);
							req7.setPostParameter(EXPAND_STATE_PARAM, expandState);
							req7.setPostParameter(SELECTED_NODE_PARAM, selectedNode);
							req7.setPostParameter(EVENTTARGET_PARAM, BOOK_TYPE_PARAM);
							req7.setPostParameter(EVENTARGUMENT_PARAM, "");
							req7.setPostParameter(POPULATE_LOG_PARAM, "");
							req7.setPostParameter(LASTFOCUS_PARAM, "");
							req7.setPostParameter(VIEWSTATE_PARAM, viewState);
							req7.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
							req7.setPostParameter(SELECT_STATES_PARAM, getState(state));
							req7.setPostParameter(SELECT_COUNTIES_PARAM, getCounty(state, county));
							req7.setPostParameter(RECEPTION_NUMBER_PARAM, "");
							req7.setPostParameter(BOOK_TYPE_PARAM, bookType);
							req7.setPostParameter(TRACKING_ID_PARAM, "");
							req7.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
							try {
								resp7 = exec(req7).getResponseAsString();
								viewState = getParameterValueFromText(VIEWSTATE_PARAM, resp7);
								eventValidation = getParameterValueFromText(EVENTVALIDATION_PARAM, resp7);
							} catch(IOException e){
								logger.error(e);
								throw new RuntimeException(e);
							}
							
							if (!StringUtils.isEmpty(resp7)) {
								String bookNumberSelect = getSelect(BOOK_NUMBER_SELECT, resp7);
								if (!StringUtils.isEmpty(bookNumberSelect)) {
									ArrayList<String> bookNumberList = new ArrayList<String>();
									Matcher ma2 = Pattern.compile(OPTION_VALUE_PATT).matcher(bookNumberSelect);
									while (ma2.find()) {
										String g1 = ma2.group(1).trim();
										if (!StringUtils.isEmpty(g1)) {
											bookNumberList.add(g1);
										}
									}
									
									if (bookNumberList.size()>1) {	//make an additional request
										
										String addResp1 = "";
										HTTPRequest addReq1 = new HTTPRequest(siteLink + "/Open.aspx", HTTPRequest.POST);	//select the second option from Book Number
										addReq1.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_BOOKNUMBER);
										addReq1.setPostParameter(EXPAND_STATE_PARAM, expandState);
										addReq1.setPostParameter(SELECTED_NODE_PARAM, selectedNode);
										addReq1.setPostParameter(EVENTTARGET_PARAM, BOOK_NUMBER_PARAM);
										addReq1.setPostParameter(EVENTARGUMENT_PARAM, "");
										addReq1.setPostParameter(POPULATE_LOG_PARAM, "");
										addReq1.setPostParameter(LASTFOCUS_PARAM, "");
										addReq1.setPostParameter(VIEWSTATE_PARAM, viewState);
										addReq1.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
										addReq1.setPostParameter(SELECT_STATES_PARAM, getState(state));
										addReq1.setPostParameter(SELECT_COUNTIES_PARAM, getCounty(state, county));
										addReq1.setPostParameter(RECEPTION_NUMBER_PARAM, "");
										addReq1.setPostParameter(BOOK_TYPE_PARAM, bookType);
										addReq1.setPostParameter(BOOK_NUMBER_PARAM, bookNumberList.get(1));
										addReq1.setPostParameter(TRACKING_ID_PARAM, "");
										addReq1.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
										try {
											addResp1 = exec(addReq1).getResponseAsString();
											viewState = getParameterValueFromText(VIEWSTATE_PARAM, addResp1);
											eventValidation = getParameterValueFromText(EVENTVALIDATION_PARAM, addResp1);
										} catch(IOException e){
											logger.error(e);
											throw new RuntimeException(e);
										}
									}
									
									for (int i=0;i<bookNumberList.size();i++) {
										String bookNumber = bookNumberList.get(i);
										if (!StringUtils.isEmpty(bookNumber)) {
											if (i<2) {
												String resp8 = "";
												HTTPRequest req8 = new HTTPRequest(siteLink + "/Open.aspx", HTTPRequest.POST);
												req8.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_BOOKTYPE);
												req8.setPostParameter(EXPAND_STATE_PARAM, expandState);
												req8.setPostParameter(SELECTED_NODE_PARAM, selectedNode);
												req8.setPostParameter(EVENTTARGET_PARAM, BOOK_NUMBER_PARAM);
												req8.setPostParameter(EVENTARGUMENT_PARAM, "");
												req8.setPostParameter(POPULATE_LOG_PARAM, "");
												req8.setPostParameter(LASTFOCUS_PARAM, "");
												req8.setPostParameter(VIEWSTATE_PARAM, viewState);
												req8.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
												req8.setPostParameter(SELECT_STATES_PARAM, getState(state));
												req8.setPostParameter(SELECT_COUNTIES_PARAM, getCounty(state, county));
												req8.setPostParameter(RECEPTION_NUMBER_PARAM, "");
												req8.setPostParameter(BOOK_TYPE_PARAM, bookType);
												req8.setPostParameter(BOOK_NUMBER_PARAM, bookNumber);
												req8.setPostParameter(TRACKING_ID_PARAM, "");
												req8.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
												try {
													resp8 = exec(req8).getResponseAsString();
												} catch(IOException e){
													logger.error(e);
													throw new RuntimeException(e);
												}
												
												if (!StringUtils.isEmpty(resp8)) {
													String pageNumberSelect = getSelect(PAGE_NUMBER_SELECT, resp8);
													if (!StringUtils.isEmpty(pageNumberSelect)) {
														ArrayList<String> pageNumberList = new ArrayList<String>();
														Matcher ma3 = Pattern.compile(OPTION_VALUE_PATT).matcher(pageNumberSelect);
														if (ma3.find()) {
															String g1 = ma3.group(1).trim();
															if (!StringUtils.isEmpty(g1)) {
																pageNumberList.add(g1);
															}
														}
														bookPageMap.put(bookNumber, pageNumberList);
													}
												}
											} else {
												bookPageMap.put(bookNumber, new ArrayList<String>());
											}
										} else {
											bookPageMap.put(bookNumber, new ArrayList<String>());
										}
									}
								}
							}
						}
						typeBookPage.put(bookType, bookPageMap);
					}
					selectLists.setTypeBookPageMap(typeBookPage);
				}
			}
		}
		
		String resp9 = "";
		HTTPRequest req9 = new HTTPRequest(siteLink + "/Search.aspx", HTTPRequest.GET);
		try {
			resp9 = exec(req9).getResponseAsString();
			expandState = getParameterValueFromHtml(EXPAND_STATE_PARAM, resp9);
			selectedNode = getParameterValueFromHtml(SELECTED_NODE_PARAM, resp9);
			viewState = getParameterValueFromHtml(VIEWSTATE_PARAM, resp9);
			eventValidation = getParameterValueFromHtml(EVENTVALIDATION_PARAM, resp9);
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		if (!StringUtils.isEmpty(resp9)) {
			String documentGroupSelect = getSelect(DOCUMENT_GROUP_SELECT, resp9);
			if (!StringUtils.isEmpty(documentGroupSelect)) {
				StringBuilder sb = new StringBuilder();
				sb.append("<select id=\"").append(DOCUMENT_GROUP_PARAM).append("\" name=\"").append(DOCUMENT_GROUP_PARAM).append("\">");
				LinkedHashMap<String, String> documentGroupMap = new LinkedHashMap<String, String>();
				Matcher ma4 = Pattern.compile(OPTION_VALUE_PATT).matcher(documentGroupSelect);
				while (ma4.find()) {
					String g1 = ma4.group(1).trim();
					String g2 = StringEscapeUtils.unescapeXml(ma4.group(2).trim());
					sb.append("<option value=\"" + g1 + "\">").append(g2).append("</option>");
					if (!"".equals(g1) && !"-1".equals(g1)) {
						documentGroupMap.put(g1, g2);
					}
				}
				sb.append("</select>");
				//selectLists.setDocumentGroupSelect(sb.toString());
				if (documentGroupSelect.contains("class=\"aspNetDisabled\"")) {
					selectLists.setHasDocumentGroup(false);
				} else {
					selectLists.setHasDocumentGroup(true);
				}
				selectLists.setDocumentGroupMap(documentGroupMap);
			} else {
				selectLists.setHasDocumentGroup(false);
			}
			
			String resultsPerPageSelect = getSelect(RESULTS_PER_PAGE_SELECT, resp9);
			if (!StringUtils.isEmpty(resultsPerPageSelect)) {
				StringBuilder sb = new StringBuilder();
				sb.append("<select id=\"").append(RESULTS_PER_PAGE_PARAM).append("\" name=\"").append(RESULTS_PER_PAGE_PARAM).append("\">");
				Matcher ma5 = Pattern.compile(OPTION_VALUE_PATT).matcher(resultsPerPageSelect);
				while (ma5.find()) {
					String g1 = ma5.group(1).trim();
					String g2 = ma5.group(2).trim();
					sb.append("<option value=\"" + g1 + "\">").append(g2).append("</option>");
				}
				sb.append("</select>");
				selectLists.setResultsPerPageSelect(sb.toString());
			}
		}
		
		String resp10 = "";
		HTTPRequest req10 = new HTTPRequest(siteLink + "/Search.aspx", HTTPRequest.POST);	//load document type list
		req10.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_DOCTYPES);
		req10.setPostParameter(EXPAND_STATE_PARAM, expandState);
		req10.setPostParameter(SELECTED_NODE_PARAM, selectedNode);
		req10.setPostParameter(EVENTTARGET_PARAM, "");
		req10.setPostParameter(EVENTARGUMENT_PARAM, "");
		req10.setPostParameter(POPULATE_LOG_PARAM, "");
		req10.setPostParameter(LASTFOCUS_PARAM, "");
		req10.setPostParameter(VIEWSTATE_PARAM, viewState);
		req10.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
		req10.setPostParameter(SELECT_STATES_PARAM, getState(state));
		req10.setPostParameter(SELECT_COUNTIES_PARAM, getCounty(state, county));
		req10.setPostParameter(RADIODOC_PARAM, RADIODOC_VALUE1);
		req10.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
		req10.setPostParameter(DOCTYPES_PARAM, LOAD_VALUE);
		req10.setPostParameter(BUSINESS_PARAM, "");
		req10.setPostParameter(DOCUMENT_GROUP_PARAM, DOCUMENT_GROUP_DEFAULT_VALUE);
		req10.setPostParameter(DATE_END_PARAM, "");
		for (int i=0;i<EMPTY_SEARCH_PARAMS.length;i++) {
			req10.setPostParameter(EMPTY_SEARCH_PARAMS[i], "");
		}
		req10.setPostParameter(SUBDIVISION_PARAM, SUBDIVISION_DEFAULT_VALUE1);
		req10.setPostParameter(NAME_SEARCH_PARAMS[0], VALUE_SEARCH_PARAMS[0]);
		req10.setPostParameter(NAME_SEARCH_PARAMS[2], VALUE_SEARCH_PARAMS[2]);
		try {
			resp10 = exec(req10).getResponseAsString();
			viewState = getParameterValueFromText(VIEWSTATE_PARAM, resp10);
			eventValidation = getParameterValueFromText(EVENTVALIDATION_PARAM, resp10);
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		if (!StringUtils.isEmpty(resp10)) {
			String documentTypeSelect = getSelect(DOCUMENT_TYPE_SELECT, resp10);
			if (!StringUtils.isEmpty(documentTypeSelect)) {
				StringBuilder sb = new StringBuilder();
				sb.append("<select id=\"").append(DOCUMENT_TYPE_SELECT).append("\" name=\"").append(DOCUMENT_TYPE_SELECT).append("\">");
				LinkedHashMap<String, String> documentTypeMap = new LinkedHashMap<String, String>();
				Matcher ma6 = Pattern.compile(OPTION_VALUE_PATT).matcher(documentTypeSelect);
				while (ma6.find()) {
					String g1 = ma6.group(1).trim();
					String g2 = StringEscapeUtils.unescapeXml(ma6.group(2).trim());
					sb.append("<option value=\"" + g1 + "\">").append(g2).append("</option>");
					if (!"".equals(g1) && !"-1".equals(g1)) {
						documentTypeMap.put(g1, g2);
					}
				}
				sb.append("</select>");
				selectLists.setDocumentTypeMap(documentTypeMap);
				//selectLists.setDocumentTypeSelect(sb.toString());
			}
		}
		
		String resp11= "";
		HTTPRequest req11 = new HTTPRequest(siteLink + "/Search.aspx", HTTPRequest.POST);	//load subdivision list
		req11.setPostParameter(SCRIPT_MANAGER_PARAM, SCRIPT_MANAGER_SUBS1);
		req11.setPostParameter(EXPAND_STATE_PARAM, expandState);
		req11.setPostParameter(SELECTED_NODE_PARAM, selectedNode);
		req11.setPostParameter(EVENTTARGET_PARAM, "");
		req11.setPostParameter(EVENTARGUMENT_PARAM, "");
		req11.setPostParameter(POPULATE_LOG_PARAM, "");
		req11.setPostParameter(LASTFOCUS_PARAM, "");
		req11.setPostParameter(VIEWSTATE_PARAM, viewState);
		req11.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
		req11.setPostParameter(SELECT_STATES_PARAM, getState(state));
		req11.setPostParameter(SELECT_COUNTIES_PARAM, getCounty(state, county));
		req11.setPostParameter(RADIODOC_PARAM, RADIODOC_VALUE1);
		req11.setPostParameter(ASYNCPOST_PARAM, TRUE_VALUE);
		req11.setPostParameter(SUBS_PARAM, LOAD_VALUE);
		req11.setPostParameter(BUSINESS_PARAM, "");
		req11.setPostParameter(DATE_END_PARAM, "");
		for (int i=0;i<EMPTY_SEARCH_PARAMS.length;i++) {
			req11.setPostParameter(EMPTY_SEARCH_PARAMS[i], "");
		}
		req11.setPostParameter(DOCUMENT_TYPE_PARAM, DOCUMENT_TYPE_DEFAULT_VALUE2);
		req11.setPostParameter(DOCUMENT_GROUP_PARAM, DOCUMENT_GROUP_DEFAULT_VALUE);
		for (int i=1;i<NAME_SEARCH_PARAMS.length;i++) {
			req11.setPostParameter(NAME_SEARCH_PARAMS[i], VALUE_SEARCH_PARAMS[i]);
		}
		try {
			resp11 = exec(req11).getResponseAsString();
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		if (!StringUtils.isEmpty(resp11)) {
			String subdivisionSelect = getSelect(SUBDIVISION_SELECT, resp11);
			if (!StringUtils.isEmpty(subdivisionSelect)) {
				StringBuilder sb = new StringBuilder();
				sb.append("<select id=\"").append(DOCUMENT_TYPE_SELECT).append("\" name=\"").append(DOCUMENT_TYPE_SELECT).append("\">");
				LinkedHashMap<String, String> subdivisionMap = new LinkedHashMap<String, String>();
				Matcher ma7 = Pattern.compile(OPTION_VALUE_PATT).matcher(subdivisionSelect);
				while (ma7.find()) {
					String g1 = ma7.group(1).trim();
					String g2 = StringEscapeUtils.unescapeXml(ma7.group(2).trim());
					sb.append("<option value=\"" + g1 + "\">").append(g2).append("</option>");
					if (!"".equals(g1) && !"-1".equals(g1)) {
						subdivisionMap.put(g1, g2);
					}
				}
				sb.append("</select>");
				selectLists.setSubdivisionMap(subdivisionMap);
				//selectLists.setSubdivisionSelect(sb.toString());
			}
		}
		
		return selectLists;
	}
	
	public String getCertificationDateHtml() {
		if (this.status != STATUS_LOGGED_IN) {
			onLogin();
		}
		return certificationDateHtml;
	}
	
	public static String getParameterValueFromHtml(String parameter, String value) {
		String result = "";
		
		Matcher matcher = Pattern.compile("<input.+?id=\"" + parameter + "\".+value=\"(.+?)\"").matcher(value);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		
		return result;
	}
	
	public static String getParameterValueFromText(String parameter, String value) {
		String result = "";
		
		Matcher matcher = Pattern.compile("\\|" + parameter + "\\|([^|]+)\\|").matcher(value);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		
		return result;
	}
	
	public static String getImageSrc(String id, String value) {
		String result = "";
		
		Matcher matcher = Pattern.compile("<img.+?id=\"" + id + "\".+src=\"(.+?)\"").matcher(value);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		
		return result;
	}
	
	public static boolean hasNextButton(String parameter, String value) {
		boolean result = false;
		
		Matcher matcher = Pattern.compile("(?is)<input[^>]+id=\"" + parameter + "\"[^>]*>").matcher(value);
		if (matcher.find()) {
			if (matcher.group(0).toLowerCase().contains("disabled=\"disabled\"")) {
				result = false;
			} else {
				result = true;
			}
		}
		
		return result;
	}
	
	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		
		if (req.getMethod()==HTTPRequest.POST) {
			
			if (req.getURL().contains("Open.aspx")) {
				String bookType = req.getPostFirstParameter(BOOK_TYPE_PARAM);
				String bookNumber = req.getPostFirstParameter(BOOK_NUMBER_PARAM);
				String pageNumber = req.getPostFirstParameter(PAGE_NUMBER_PARAM);
				if (bookType!=null && bookNumber!=null && pageNumber !=null) {		//search by Book and Page
					
					if (bookType.trim().length()==0) {
						res.is = IOUtils.toInputStream("<html>Book Type must be selected.</html>");
						res.body = "<html>Book Type must be selected.</html>";
						res.contentLenght = res.body.length();
						res.returnCode = 200;
						return;
					}
					if (bookNumber.trim().length()==0) {
						res.is = IOUtils.toInputStream("<html>Book Number must be selected.</html>");
						res.body = "<html>Book Number must be selected.</html>";
						res.contentLenght = res.body.length();
						res.returnCode = 200;
						return;
					}
					if (pageNumber.trim().length()==0) {
						res.is = IOUtils.toInputStream("<html>Page Number must be selected.</html>");
						res.body = "<html>Page Number must be selected.</html>";
						res.contentLenght = res.body.length();
						res.returnCode = 200;
						return;
					}
				}
			}
			
			String stringResponse = res.getResponseAsString();
			if (stringResponse.contains("Session Timeout")) {
				destroySession();
				HTTPResponse resp = process(req);
				res.is = new ByteArrayInputStream(resp.getResponseAsByte());
				res.contentLenght= resp.contentLenght;
				res.contentType = resp.contentType;
				res.headers = resp.headers;
				res.returnCode = resp.returnCode;
				res.body = resp.body;
				res.setLastURI(resp.getLastURI());
			} else {
				res.is = new ByteArrayInputStream(stringResponse.getBytes());
			}
		}
	}
	
	/**
	* Read one page out of the TIFF file and return it.
	*
	* @param imageFile
	* @param pageNumber
	* @return
	* @throws IOException
	*/
	private static IIOImage readDocumentPage(File imageFile) throws IOException
	{
	ImageReader tiffReader = null;

	try
	{
		// locate a TIFF reader
		Iterator<ImageReader> tiffReaders = ImageIO.getImageReadersByFormatName("tiff");
		if (!tiffReaders.hasNext()) throw new IllegalStateException("No TIFF reader found");
			tiffReader = tiffReaders.next();
	
		// point it to our image file
		ImageInputStream tiffStream = ImageIO.createImageInputStream(imageFile);
		tiffReader.setInput(tiffStream);
	
		// read one page from the TIFF image
		return tiffReader.readAll(0, null);
	}
	finally
	{
		if (tiffReader != null) tiffReader.dispose();
	}
	}
	
	/**
	* Rescale the input image to fit the maximum dimensions indicated.
	* Only large images are shrunk; small images are not expanded.
	*
	* @param source
	* @param maxWidth
	* @param maxHeight
	* @return
	*/
	private static RenderedImage scaleImage(IIOImage source, float maxWidth, float maxHeight)
	{
	// shrink but respect the original aspect ratio
	float scaleFactor;
	if (source.getRenderedImage().getHeight() > source.getRenderedImage().getWidth()){
		scaleFactor = maxHeight / source.getRenderedImage().getHeight();
	} else {
		scaleFactor = maxWidth / source.getRenderedImage().getWidth();
	}

	if (scaleFactor >= 1){
		// don't expand small images, only shrink large ones
		return source.getRenderedImage();
	}

	// prepare parameters for JAI function call
	ParameterBlockJAI params = new ParameterBlockJAI("scale");
	params.addSource(source.getRenderedImage());
	params.setParameter("xScale", scaleFactor);
	params.setParameter("yScale", scaleFactor);

	RenderedOp resizedImage = JAI.create("scale", params);

	return resizedImage;
	}
	
	private static void convertJpgToTiff(InputStream inputStream, String fileNameTemp, int index, List<String> pages) {
		try{
			
			BufferedImage img = ImageIO.read(inputStream);
	        TIFFImageWriterSpi tiffspi = new TIFFImageWriterSpi();
	        ImageWriter writer = tiffspi.createWriterInstance();
	        
	        ImageWriteParam param = writer.getDefaultWriteParam();
	        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	        param.setCompressionType("LZW");
	        param.setCompressionQuality(0.9f);
	       
	        File fOutputFile = null;
	        fOutputFile = new File(fileNameTemp + "Page" + index + ".tiff");
	        pages.add(fOutputFile.toString());
	        
	        ImageOutputStream ios = ImageIO.createImageOutputStream(fOutputFile);
	        writer.setOutput(ios);
	        writer.write(null, new IIOImage(img, null, null), param);
	        
	        // read one page as image
	        IIOImage pageImage = readDocumentPage(fOutputFile);

	        // rescale image to fit
	        RenderedImage scaledImage = scaleImage(pageImage, 1800, 1400);
	        PlanarImage image= new RenderedImageAdapter (scaledImage) ; 

	        BufferedImage bufImg = image.getAsBufferedImage(); 
	        ios = ImageIO.createImageOutputStream(fOutputFile);
	        writer.setOutput(ios);
	        writer.write(null, new IIOImage(bufImg, null, null), param);

		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public boolean hasViewImageButton(String response) {
		boolean res = false;
		
		Matcher ma = Pattern.compile("(?is)(<input[^>]+id=\"MainContent_searchMainContent_ctl00_btnViewImage\"[^>]*>)").matcher(response);
		if (ma.find()) {
			if (!ma.group(1).toLowerCase().contains("disabled=\"disabled\"")) {
				res = true;
			}
		}
		
		return res;
		
	}
	
	public byte[] getImage(String link, String fileName) {
		
		List<String> pages = new ArrayList<String>();
		
		String instrno = StringUtils.extractParameter(link, "instrno=([^&?]*)");
		String bookType = StringUtils.extractParameter(link, "bookType=([^&?]*)");
		String book = StringUtils.extractParameter(link, "book=([^&?]*)");
		String page = StringUtils.extractParameter(link, "page=([^&?]*)");
		String trackingID = StringUtils.extractParameter(link, "DK=([^&?]*)");
		
		String resp0 = "";
		
		HTTPRequest req0 = new HTTPRequest(getSiteLink().replaceFirst("(?is)/Introduction\\.aspx.*", "") + "/Open.aspx", HTTPRequest.POST);
		
		//try with book and page
		if (!StringUtils.isEmpty(bookType) && !StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)) {
			req0.setPostParameter(BOOK_TYPE_PARAM, bookType);
			req0.setPostParameter(BOOK_NUMBER_PARAM, book);
			req0.setPostParameter(PAGE_NUMBER_PARAM, page);
			resp0 = execute(req0);
		}
		
		//try with instrument number
		if (!hasViewImageButton(resp0)) {
			req0 = new HTTPRequest(getSiteLink().replaceFirst("(?is)/Introduction\\.aspx.*", "") + "/Open.aspx", HTTPRequest.POST);
			req0.setPostParameter(RECEPTION_NUMBER_PARAM, instrno);
			resp0 = execute(req0);
		}
		
		//try with tracking ID
		if (!hasViewImageButton(resp0)) {
			req0 = new HTTPRequest(getSiteLink().replaceFirst("(?is)/Introduction\\.aspx.*", "") + "/Open.aspx", HTTPRequest.POST);
			req0.setPostParameter(TRACKING_ID_PARAM, trackingID);
			resp0 = execute(req0);
		}
		
		if (!hasViewImageButton(resp0)) {
			return null;
		}
		
		link = req0.getRedirectLocation();
		HTTPRequest req = new HTTPRequest(link, HTTPRequest.POST);
		req.setPostParameter(SELECT_STATES_PARAM, getState());
		req.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
		
		String expandState0 = getParameterValueFromHtml(EXPAND_STATE_PARAM, resp0);
		req.setPostParameter(EXPAND_STATE_PARAM, expandState0);
		
		req.setPostParameter(SELECTED_NODE_PARAM, "");
		req.setPostParameter(EVENTTARGET_PARAM, "");
		req.setPostParameter(EVENTARGUMENT_PARAM, "");
		req.setPostParameter(POPULATE_LOG_PARAM, "");
		req.setPostParameter(LASTFOCUS_PARAM, "");
		
		String viewState0 = getParameterValueFromHtml(VIEWSTATE_PARAM, resp0);
		req.setPostParameter(VIEWSTATE_PARAM, viewState0);
		
		String eventValidation0 = getParameterValueFromHtml(EVENTVALIDATION_PARAM, resp0);
		req.setPostParameter(EVENTVALIDATION_PARAM, eventValidation0);
		
		String receptionNo = "";
		String bookPage = "";
		String receptionDate = "";
		String documentType = "";
		String pageCount = "";
		String description = "";
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp0, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList receptionNoList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbReceptionNo"));
			if (receptionNoList.size()>0) {
				String value = receptionNoList.elementAt(0).toHtml();
				Matcher ma = Pattern.compile("(?is)value=\"([^\"]+)\"").matcher(value);
				if (ma.find()) {
					receptionNo = ma.group(1);
				}	
			}
			
			NodeList bookPageList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbBookPage"));
			if (bookPageList.size()>0) {
				String value = bookPageList.elementAt(0).toHtml();
				Matcher ma = Pattern.compile("(?is)value=\"([^-]+-[^-]+-[^\"]+)\"").matcher(value);
				if (ma.find()) {
					bookPage = ma.group(1);
				}
			}
			
			NodeList receptionDateList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbReceptionDate"));
			if (receptionDateList.size()>0) {
				String value = receptionDateList.elementAt(0).toHtml();
				Matcher ma = Pattern.compile("(?is)value=\"(\\d{1,2}-\\d{1,2}-\\d{4}[^\"]+)\"").matcher(value);
				if (ma.find()) {
					receptionDate = ma.group(1);
				}
			}
			
			NodeList documentTypeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbDocumentType"));
			if (documentTypeList.size()>0) {
				String value = documentTypeList.elementAt(0).toHtml();
				Matcher ma = Pattern.compile("value=\"([^\"]+)\"").matcher(value);
				if (ma.find()) {
					documentType = ma.group(1).trim();
					documentType = ro.cst.tsearch.servers.types.GenericCountyRecorderRO.cleanType(documentType);
				} 
			}
			
			NodeList pageCountList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbPageCount"));
			if (pageCountList.size()>0) {
				String value = pageCountList.elementAt(0).toHtml();
				Matcher ma = Pattern.compile("value=\"([^\"]+)\"").matcher(value);
				if (ma.find()) {
					pageCount = ma.group(1).trim();
				} 
			}
			
			NodeList descriptionList = nodeList.extractAllNodesThatMatch(new TagNameFilter("textarea"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbDescription"));
			if (descriptionList.size()>0) {
				description = descriptionList.elementAt(0).toPlainTextString().trim();
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		req.setPostParameter(RECEPTION_NO_PARAM, receptionNo);
		req.setPostParameter(BOOK_PAGE_PARAM, bookPage);
		req.setPostParameter(RECEPTION_DATE_PARAM, receptionDate);
		req.setPostParameter(DOCUMENT_TYPE_PARAM_TB, documentType);
		req.setPostParameter(PAGE_COUNT_PARAM, pageCount);
		req.setPostParameter(VIEW_IMAGE_PARAM, "View Image");
		req.setPostParameter(DESCRIPTION_PARAM, description);
		
		String resp = "";
		try {
			resp = exec(req).getResponseAsString();
		} catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		
		if (resp.contains("No image is available for this document.")) {
			return null;
		}
		
		link = req.getRedirectLocation();
		
		String imageLink = getImageSrc(IMAGE_ID, resp);
		
		HTTPRequest req1 = new HTTPRequest(req.getURL().replaceFirst("(?is)/Document\\.aspx.*", "/"+imageLink), HTTPRequest.GET);
		HTTPResponse resp1 = process(req1);
		
		if (StringUtils.isEmpty(instrno)) {
			instrno = bookType + "-" + book + "-" + page;
		}
		String fileNameTemp = getSearch().getImageDirectory() + 
				File.separator + instrno;
		
		int index = 1;
		convertJpgToTiff(resp1.getResponseAsStream(), fileNameTemp, index, pages);
		
		boolean hasNext = hasNextButton(NEXT_BUTTON_ID, resp);
		while (hasNext) {
			
			HTTPRequest req2 = new HTTPRequest(link, HTTPRequest.POST);
			String expandState = getParameterValueFromHtml(EXPAND_STATE_PARAM, resp);
			req2.setPostParameter(EXPAND_STATE_PARAM, expandState);
			req2.setPostParameter(SELECTED_NODE_PARAM, "");
			req2.setPostParameter(EVENTTARGET_PARAM, "");
			req2.setPostParameter(EVENTARGUMENT_PARAM, "");
			req2.setPostParameter(POPULATE_LOG_PARAM, "");
			req2.setPostParameter(LASTFOCUS_PARAM, "");
			String viewState = getParameterValueFromHtml(VIEWSTATE_PARAM, resp);
			req2.setPostParameter(VIEWSTATE_PARAM, viewState);
			String eventValidation = getParameterValueFromHtml(EVENTVALIDATION_PARAM, resp);
			req2.setPostParameter(EVENTVALIDATION_PARAM, eventValidation);
			req2.setPostParameter(SELECT_STATES_PARAM, getState());
			req2.setPostParameter(SELECT_COUNTIES_PARAM, getCounty());
			req2.setPostParameter(GO_TO_PAGE_PARAM, Integer.toString(index));
			req2.setPostParameter(NEXT_PAGE_PARAM, "Next Page");
			String tbReceptionNumber = getParameterValueFromHtml(TB_RECEPTION_NUMBER_PARAM, resp);
			req2.setPostParameter(TB_RECEPTION_NUMBER_PARAM, tbReceptionNumber);
			String resp2 = "";
			try {
				resp2 = exec(req2).getResponseAsString();
				resp = resp2;
			} catch(IOException e){
				logger.error(e);
				throw new RuntimeException(e);
			}
			
			String imageLink2 = getImageSrc(IMAGE_ID, resp2);
			
			HTTPRequest req3 = new HTTPRequest(req1.getURL().replaceFirst("(?is)/Document.aspx.*", "/"+imageLink2), HTTPRequest.GET);
			HTTPResponse resp3 = process(req3);
			
			index++;
			convertJpgToTiff(resp3.getResponseAsStream(), fileNameTemp, index, pages);
			
			hasNext = hasNextButton(NEXT_BUTTON_ID, resp2);
		}
		
		TiffConcatenator.concatenate(pages.toArray(new String[pages.size()]), fileName);
		
		for(int i = 0; i < pages.size(); i++){
		   	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		   	FileUtils.deleteFile(pages.get(i));

		   }
		
		return FileUtils.readBinaryFile(fileName);
	}

}

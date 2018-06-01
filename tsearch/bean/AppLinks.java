package ro.cst.tsearch.bean;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.reports.throughputs.ThroughputOpCode;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.tsrindex.client.TsdIndexPage;
/**
 * @author nae
 */
public class AppLinks
{
	protected static final Category logger= Logger.getLogger(AppLinks.class);
	
	public static final String REP_MENU  = "REPORTS_MENU";
	public static final String COMM_MENU = "COMM_MENU";
	public static final String USER_MENU = "USER_MENU";  
	public static final String INV_MENU  = "INVOICE_MENU";
	public static final String SETTINGS_MENU = "SETTINGS_MENU";
	public static final String ADD_INFO_PAGE_NAME = TsdIndexPage.ADD_INFO_PAGE_NAME;
	
	public static String getBackToAutoSearchPageLink(long searchId){
		return getBackToAutoSearchPageLink(searchId, "_self");
	}
	
	public static String getBackToAutoSearchPageLink(long searchId, String target){
		String href = getBackToSearchPageHref(searchId);
		String img = getIcon("00ico_back.gif",20,15);
		String name = "Search Page"; 
		return getGenericLink (href, target, img, name, 16);
	}

	public static String getDateTypeDropDown(String selected){
		if (selected == null){
			selected = "";
		}
		String html = "";
		html += "Date Type:&nbsp;";
		html += "<select name='" + RequestParams.REPORTS_DATE_TYPE + "'>\n";
		//combined
		html += "<option value='" + RequestParamsValues.REPORTS_DATE_TYPE_COMBINED + "'";  
		if (selected.equals(Integer.toString(RequestParamsValues.REPORTS_DATE_TYPE_COMBINED))){
			html += " selected ";
		}
		html += ">Order&TSR</option>\n";
		//order date
		html += "<option value='" + RequestParamsValues.REPORTS_DATE_TYPE_ORDER + "'";
		if (selected.equals(Integer.toString(RequestParamsValues.REPORTS_DATE_TYPE_ORDER))){
			html += " selected ";
		}
		html += ">Order Date</option>\n";
		//tsr date
		html += "<option value='" + RequestParamsValues.REPORTS_DATE_TYPE_TSR + "'";
		if (selected.equals(Integer.toString(RequestParamsValues.REPORTS_DATE_TYPE_TSR))){
			html += " selected ";
		}
		html += ">TSR Date</option>\n";
		html += "</select>\n";
		return html;
	}
	
	public static String getAssignEditEmailWindow(long searchId, String availableIdList, String assignSubject, String userEmail ) 
	{
	return 
			 "emailTS"
			+ "=window.open('"
			+ URLMaping.path
			+ URLMaping.SendEmailAssignToPage
			+ "?"
			+ RequestParams.SEARCH_ID
			+ "="
			+ searchId
			+ "&"
			+ "assignSearchIds"
			+ "="
			+ availableIdList
			+ "&"
			+ "assignSubject"
			+ "="
			+ assignSubject					
			+ "&"
			+ "assignUser"
			+ "="
			+ userEmail
			+ "','"
			+ "emailTS"
			+ "','width=850,height=480,top=185,left=20,resizable=yes,toolbar=no,status=yes,scrollbars=auto');"
			+ "emailTS" + ".focus();"
		;
	}

	public static String getLogoutLink(){
	    return getLogoutLink("", true);
	}
	
	
	
	public static String getLogoutLink(String params, boolean confirm){
		//String href = lbURL +":"+ lbPort +URLMaping.path + URLMaping.Logout + params;
		String href = URLMaping.path + URLMaping.Logout + params;
		String img = getIcon("logoff.gif");
		String name = "Log Out"; 
		return getGenericLinkOnClick(confirm ? "javascript:if (confirm('All current unsaved searches will be lost!\\nClick \\'OK\\' to continue with logout or click \\'Cancel\\' and continue this search.')) {window.location='" + href + "'};" 
		        : "javascript:window.location='" + href + "'", img, name, 14);
	}


	public static String getWindowCloseLink(){
	    return getWindowCloseLink("Close");
	}
	
	public static String getWindowCloseLink(String title) {
		String href = "javascript:window.close();";
		String img = "";
		if ("Close".equals(title))
		    img = getIcon("00ico_close.gif",19,17);
		return "<div id='windowCloseLink'>"+getGenericLink (href, img, title, 18)+"</div>";
	}
	
	public static String getDSMAWindowCloseLink(){
		String href = "javascript:window.top.close();";
		String img = getIcon("00ico_close.gif",19,17);
		String name = "Close"; 
		return "<div id='windowCloseLink'>"+getGenericLink (href, img, name, 18)+"</div>";
	}
			

			
	public static String getParentSiteNoSaveLink(long searchId, String target){
		String href = getParentSiteNoSaveHref(searchId);
		String img = getIcon("/00ico_parent.gif",19,17);
		String name = "Parent Sites"; 
		return getGenericLink (href, target, img, name, 20);
	}

	public static String getParentSiteNoSaveHref(long searchId) {
		return URLMaping.path + URLMaping.ParentSearch
						+ "?" + RequestParams.SEARCH_ID + "=" + searchId 
						+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.PARENT_SITE;
	}
			
	public static String getParentSiteNoSaveHref(long searchId,boolean isHome) {
		return URLMaping.path + URLMaping.ParentSearch
						+ "?" + RequestParams.SEARCH_ID + "=" + searchId 
						+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.PARENT_SITE
						+ "&isHome="+(isHome?"1":"0")
						+ "&";
	}	
	
	public static String getParentSiteNoSaveLink(long searchId){
		return getParentSiteNoSaveLink(searchId, "_self");
	}
			
	public static String getMyProfileLink(long searchId, String loginName){
		return getMyProfileLink(searchId, loginName, TSOpCode.MY_PROFILE_VIEW);
	}
	
	public static String getMyProfileLink(long searchId, String loginName, int opCode){
		String href = URLMaping.path+URLMaping.USER_VIEW +
				"?" + RequestParams.SEARCH_ID + "=" + searchId +
				"&" + UserAttributes.USER_ID + "=" + loginName + 
				"&opCode=" +
				+ opCode
				+ "&typePage=1";
		String img = getIcon("my_profile.gif");
		String name = "My Profile"; 
		return getGenericLinkNewPage (href, img, name, 13);
	}
	
	public static String getMyATSLink(long searchId, String userId){
		return getMyATSLink(searchId, userId, TSOpCode.MY_ATS_VIEW);
	}
	
	public static String getMyATSLink(long searchId, String userId, int opCode){
		String href = URLMaping.path+URLMaping.MY_ATS_VIEW +
				"?" + RequestParams.SEARCH_ID + "=" + searchId +
				"&" + UserAttributes.USER_ID + "=" + userId + 
				"&opCode=" +
				+ opCode
				+ "&typePage=1";
		String img = getIcon("my_profile.gif");
		String name = "My ATS"; 
		return getGenericLinkNewPage (href, img, name, 191);
	}
	
	public static String getCloseLink() {
	    return getCloseLink("Close");
	}
	
	public static String getCloseLink(String title)
	{
		String href = "<b><a class='menu' href=\"javascript:window.close()\">" + title + "</a></b>";
		href = getWindowCloseLink(title);
		return href;
	}

	public static String getChangeCommLink(){

		String href = "javascript:;";
		String img = getIcon("globe.gif");
		String name = "Change Community"; 
		return getGenericLink (href, img, name, 12);
		
	}	
	public static String getChangeCommLink(long searchId){
		String href = "javascript:commChange=window.open('"
                    +URLMaping.path + URLMaping.ChangeContext
					+ "?" + RequestParams.SEARCH_ID + "="+ searchId
					+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.CHANGE_CONTEXT
                    //+ "&"+ VPOParameters.COMM_ID + "="+ cidStr
					//+ "&"+ VPOParameters.BW_TYPE + "="+ bwtype
                    /*+ "&"+ VPOParameters.RET_PAGE */ 
                    //+ "'+window.location"
					+ "'"
					+ ",'" + URLMaping.INSTANCE_DIR + searchId + "',"
                    + "'width=400,height=450,left=100,top=150,scrollbars=yes');" 
				    + "commChange.focus();";
		String img = getIcon("globe.gif");
		String name = "Change Community"; 
		return getGenericLinkOnClick(href, img, name, 12);
	}

	public static String getAlternateSearchLink(long searchId) {
		String winHref = getNewSearchHref(User.NEW_SEARCH);
		String href = "javascript: var AlternateSearch= window.open('" + winHref		 	 
					+ "','ATSF" + searchId +"_'+random_1000000()," +
					"'width=1024,height=768,left=0,top=0,resizable=yes,menubar=yes,toolbar=yes,status=yes,scrollbars=yes,location=yes');" +
					"AlternateSearch.focus();";
		String img = getIcon("ParalelSearch.gif");
		String name = "New Search Page"; 
		return getGenericLinkOnClick (href, img, name, 11);
	}
	
	
	public static String getTsdIndexLink(String formName){
		String href = getTSDIndexHref(formName);
		String img = getIcon("ico_table.gif");
		String name = "TSR Index"; 
		return getGenericLink (href, img, name, 15);
	}

	public static String getTsdIndexNoSaveLink(long searchId){
		String href = getTSDIndexNoSaveHref(searchId);
		String img = getIcon("ico_table.gif");
		String name = "TSR Index"; 
		return getGenericLink (href, img, name, 15);
	}


	private static String getGenericLinkOnClick (String href, String img, String name, int tooltipNo){
		return getGenericLink ("#\" onClick=\"" + href, img, name, tooltipNo);
	}

	private static String getGenericLink (String href, String img, String name, int tooltipNo){
		return "<a class='menu' href=\"" + href +  "\" onMouseOver=\"stm(" + tooltipNo + ",Style[9])\" onmouseout=\"htm()\">"	+ img + " <b>" + name + "</b></a>";
	}
	private static String getGenericLinkNewPage(String href, String img, String name, int tooltipNo){
			return "<a class='menu' href=\"javascript:openMyPage('" + href +  "')\" onMouseOver=\"stm(" + tooltipNo + ",Style[9])\" onmouseout=\"htm()\">"	+ img + " <b>" + name + "</b></a>";
		}
	private static String getGenericLink (String href, String target, String img, String name, int tooltipNo){
		return getGenericLink (href + "\" target=\"" + target, img, name, tooltipNo);
	}

	private static String getIcon(String name){
		return getIcon(name, 18, 17);
	}
	
	private static String getIcon(String name, int width, int height){
		String img = "<img src='" +	URLMaping.IMAGES_DIR + "/"+ name + "' width='" +	width +	"' height='" + height + "' border='0' align='absmiddle'>";
		return img;
	}

	public static String getNewSearchHref(long searchId)
	{
	    return URLMaping.path + URLMaping.StartTSPage 
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.NEW_SEARCH;
	}
	
	public static String getRemoveOldAndAddNewSearch(long searchId)
	{
	    return URLMaping.path + URLMaping.StartTSPage 
		+ "?" + RequestParams.SEARCH_ID + "=" + searchId
		+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.REMOVE_OLD_ADD_AND_NEW_SEARCH;
	}
	
	public static String getBackToSearchPageHref(long searchId)
	{
		return URLMaping.path + URLMaping.StartTSPage 
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.NEW_TS;
	}
	
	public static String getBackToSearchPageHref(long searchId, boolean isHome)
	{
		return URLMaping.path + URLMaping.StartTSPage 
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.NEW_TS
			+ "&isHome="+(isHome?"1":"0");
	}
	
	public static String getBackToOrderPageHref(long searchId)
	{
		return URLMaping.path + URLMaping.OrderTSPage 
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.NEW_TS
			+ "&" + RequestParams.ORDER_TS + "=" + 1;
	}

	public static String getSaveTSHref(String formName, long searchId)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document." + formName	+ "." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.UpdateTSRIndex
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId 
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.SAVE_SEARCH
			+ "&searchtype=" + Search.PARENT_SITE_SEARCH + "&fromPage=newTS" +"'"
			+ ";"
			+ "window.document." + formName	+ ".searchtype.value="
			+ Search.SAVE_SEARCH
			+ ";"
			+ "confirmInputs();";
	}
	
	public static String getValidateInputsHref(String formName, long searchId)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document." + formName	+ ".searchtype.value="
			+ Search.SAVE_SEARCH
			+ ";"
			+ "confirmInputs();";
	}
	
	public static String getSaveUnlockedHref(String formName, long searchId)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document." + formName	+ "." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.UpdateTSRIndex
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId 
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.SAVE_UNLOCKED_SEARCH
			+ "&searchtype=" + Search.PARENT_SITE_SEARCH + "&fromPage=newTS" +"'"
			+ ";"
			+ "window.document." + formName	+ ".searchtype.value="
			+ Search.SAVE_SEARCH
			+ ";"
			+ "confirmInputs();";
	}
	
	public static String getSaveForReviewHref(String formName, long searchId)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document." + formName	+ "." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.UpdateTSRIndex
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId 
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.SAVE_SEARCH_FOR_REVIEW
			+ "&searchtype=" + Search.PARENT_SITE_SEARCH + "&fromPage=newTS" +"'"
			+ ";"
			+ "window.document." + formName	+ ".searchtype.value="
			+ Search.SAVE_SEARCH
			+ ";"
			+ "confirmInputs();";
	}
	
	public static String getSaveStarterTSHref(String formName, long searchId)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document." + formName	+ "." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.UpdateTSRIndex
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId 
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.SAVE_SEARCH_STARTER
			+ "&searchtype=" + Search.PARENT_SITE_SEARCH + "&fromPage=newTS" +"'"
			+ ";"
			+ "window.document." + formName	+ ".searchtype.value="
			+ Search.SAVE_SEARCH
			+ ";"
			+ "confirmInputs();";
	}
	
	public static String getSaveTestCaseTSHref(String formName, long searchId, int opCode)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document." + formName	+ "." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.path + URLMaping.SaveTestCase
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId 
			+ "&" + TSOpCode.OPCODE + "=" + opCode
			+ "&newID=" + searchId
			+ "&searchtype=" + Search.PARENT_SITE_SEARCH + "&fromPage=newTS" +"'"
			+ ";"
			+ "window.document." + formName	+ ".searchtype.value="
			+ Search.SAVE_SEARCH
			+ ";"
			+ "confirmInputs();";
	}
	
	public static String getAutomaticTSHref(String formName)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document.forms[0]." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.TSD +"'"
			+ ";"
			+ "window.document.forms[0].searchtype.value="
			+ Search.AUTOMATIC_SEARCH
			+ ";"
			+ "window.document.forms[0]." +	RequestParams.SEARCH_STARTED + ".value="
			+ "'true'"
			+ ";"			
			+ "confirmInputs();";
	}

	public static String getDataSourceTSHref(String formName)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document.forms[0]." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.TSD +"'"
			+ ";"
			+ "window.document.forms[0].searchtype.value="
			+ Search.AUTOMATIC_SEARCH 
			+ ";"
			+ "window.document.forms[0]."+SearchAttributes.ADDITIONAL_SEARCH_TYPE+".value='"
			+ SearchAttributes.DATA_SOURCE
			+ "';"
			+ "window.document.forms[0]." +	RequestParams.SEARCH_STARTED + ".value="
			+ "'true'"
			+ ";"			
			+ "confirmInputs();";
	}
	
	public static String getDateDownTSHref(String formName)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document.forms[0]." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.TSD +"'"
			+ ";"
			+ "window.document.forms[0].searchtype.value="
			+ Search.AUTOMATIC_SEARCH 
			+ ";"
			+ "window.document.forms[0]."+SearchAttributes.ADDITIONAL_SEARCH_TYPE+".value='"
			+ SearchAttributes.DATE_DOWN
			+ "';"
			+ "window.document.forms[0]." +	RequestParams.SEARCH_STARTED + ".value="
			+ "'true'"
			+ ";"			
			+ "confirmInputs();";
	}
	
	public static String getFVSUpdateTSHref(String formName)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document.forms[0]." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.TSD +"'"
			+ ";"
			+ "window.document.forms[0].searchtype.value="
			+ Search.AUTOMATIC_SEARCH 
			+ ";"
			+ "window.document.forms[0]."+SearchAttributes.ADDITIONAL_SEARCH_TYPE+".value='"
			+ SearchAttributes.FVS_UPDATE
			+ "';"
			+ "window.document.forms[0]." +	RequestParams.SEARCH_STARTED + ".value="
			+ "'true'"
			+ ";"			
			+ "confirmInputs();";
	}
	
	public static String getSubmitAnyTypeTSHref(String formName)
	{
		return "javascript:document." + formName + ".action='" + URLMaping.path + URLMaping.validateInputs + "';"
			+ "window.document." + formName	+ "." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.StartTS +"'"
			+ ";"
			 + "window.document." + formName	+ ".validate.value=0;"
			 + "document." + formName	+ ".submit();"
		/*"confirmInputs();"*/;
	}
	public static String getSearchParentTSHref(String formName)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document." + formName	+ "." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.StartTS +"'"
			+ ";"
			+ "window.document." + formName	+ ".searchtype.value="
			+ Search.PARENT_SITE_SEARCH
			+ ";"
			+ "confirmInputs();";
	}
	
	public static String getTSDIndexHref(String formName)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document." + formName	+ "." +	RequestParams.FORWARD_LINK + ".value="
			+ "'" + URLMaping.TSD +"'"
			+ ";"
			+ "window.document." + formName	+ ".searchtype.value="
			+ Search.PARENT_SITE_SEARCH
			+ ";" +
			"confirmInputs();";
	}
	
	public static String getNewTSDIndexHref(String formName, long searchId, long userId)
	{
		return "javascript:document."
			+ formName
			+ ".action='"
			+ URLMaping.path
			+ URLMaping.validateInputs
			+ "';"
			+ "window.document." + formName	+ "." +	RequestParams.FORWARD_LINK + ".value="
			+ "'/jsp/newtsdi/tsdindexpage.jsp?searchId=" + searchId + "&userId=" +  userId + "'"
			+ ";"
			+ "window.document." + formName	+ ".searchtype.value="
			+ Search.PARENT_SITE_SEARCH
			+ ";"
			+ "confirmInputs();";
	}
	
	
	



	public static String getTSDIndexNoSaveHref(long searchId)
	{
		return 	URLMaping.path + URLMaping.TSD 
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.TSD;
	}

	public static String getRepHomeHref(long searchId) {
		
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		int yearR = c.get(Calendar.YEAR);
		int monthR = c.get(Calendar.MONTH) + 1;
		int dayR = c.get(Calendar.DAY_OF_MONTH);
		
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		
		String reportState= ua.getMyAtsAttributes().getReportState(); 
		String reportCounty = ua.getMyAtsAttributes().getReportCounty();
  		String reportAbstractor = ua.getMyAtsAttributes().getReportAbstractor();
  		String reportCompanyAgent = ua.getMyAtsAttributes().getReportCompanyAgent();
  		String reportAgent = ua.getMyAtsAttributes().getReportAgent();
  		String reportStatus = ua.getMyAtsAttributes().getReportStatus();
  		
  		String reportSortBy = ua.getMyAtsAttributes().getReportSortBy();
  		String reportSortDir = ua.getMyAtsAttributes().getReportSortDir();
  		
		return URLMaping.path+URLMaping.REPORT_MONTH 
			+ "?dayReport=" + dayR 
			+ "&monthReport=" + monthR 
			+ "&yearReport=" + yearR
			+ "&searchId=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.REPORT_MODULE
			+ "&reportState="+reportState+"&reportCounty="+reportCounty
			+ "&reportCompanyAgent="+reportCompanyAgent
			+ "&reportAbstractor="+reportAbstractor
			+ "&orderBy="+reportSortBy
			+ "&orderType="+reportSortDir
			+ "&reportAgent="+reportAgent
			+ "&reportStatus="+reportStatus;
	}
	
	public static String getRepHomeHref(long searchId, String token ) {
		
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		int yearR = c.get(Calendar.YEAR);
		int monthR = c.get(Calendar.MONTH) + 1;
		int dayR = c.get(Calendar.DAY_OF_MONTH);
		
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		
		String reportState= ua.getMyAtsAttributes().getReportState(); 
		String reportCounty = ua.getMyAtsAttributes().getReportCounty();
  		String reportAbstractor = ua.getMyAtsAttributes().getReportAbstractor();
  		String reportCompanyAgent = ua.getMyAtsAttributes().getReportCompanyAgent();
  		String reportAgent = ua.getMyAtsAttributes().getReportAgent();
  		String reportStatus = ua.getMyAtsAttributes().getReportStatus();
  		
  		String reportSortBy = ua.getMyAtsAttributes().getReportSortBy();
  		String reportSortDir = ua.getMyAtsAttributes().getReportSortDir();
  		
		return URLMaping.REPORT_SEARCH 
			+ "?dayReport=" + dayR 
			+ "&monthReport=" + monthR 
			+ "&yearReport=" + yearR
			+ "&" + RequestParams.SEARCH_ID + "=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.REPORT_MODULE
			+ "&reportState="+reportState
			+ "&reportCounty="+reportCounty
			+ "&reportCompanyAgent="+reportCompanyAgent
			+ "&reportAbstractor="+reportAbstractor
			+ "&orderBy="+reportSortBy
			+ "&orderType="+reportSortDir
			+ "&reportAgent="+reportAgent
			+ "&reportStatus="+reportStatus
			+ "&" + RequestParams.REPORTS_SEARCH_ALL + "=on"
			+ "&" + RequestParams.REPORTS_SEARCH_TSR + "=" + token
			+ "&" + RequestParams.REPORTS_DATE_TYPE + "=" + 1
			+ "&" + RequestParams.SEARCH_TYPE + "=searchInterval"
			+ "&" + RequestParams.REPORTS_SEARCH_FIELD + "=TSR+File+ID"
			+ "&" + RequestParams.REPORTS_DASHBOARD + "=" + 1
			;
	}
	
	public static String getThroughputHref(long searchId) {
	
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		
		String reportState= ua.getMyAtsAttributes().getReportState(); 
		String reportCounty = ua.getMyAtsAttributes().getReportCounty();
  		String reportAbstractor = ua.getMyAtsAttributes().getReportAbstractor();
  		String reportCompanyAgent = ua.getMyAtsAttributes().getReportCompanyAgent();
  		String reportAgent = ua.getMyAtsAttributes().getReportAgent();
  		String reportStatus = ua.getMyAtsAttributes().getReportStatus();
  		
  		String reportSortBy = ua.getMyAtsAttributes().getReportSortBy();
  		String reportSortDir = ua.getMyAtsAttributes().getReportSortDir();

  		return URLMaping.path+URLMaping.REPORT_THROUGHPUT
			+ "?searchId=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + ThroughputOpCode.LOAD_PARAMETERS
			+ "&reportState="+reportState+"&reportCounty="+reportCounty
			+ "&reportCompanyAgent="+reportCompanyAgent
			+ "&reportAbstractor="+reportAbstractor
			+ "&orderBy="+reportSortBy
			+ "&orderType="+reportSortDir
			+ "&reportAgent="+reportAgent
			+ "&reportStatus="+reportStatus;
	}
	
	public static String getIncomeHref(long searchId) {
		
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		
		String reportState= ua.getMyAtsAttributes().getReportState(); 
		String reportCounty = ua.getMyAtsAttributes().getReportCounty();
  		String reportAbstractor = ua.getMyAtsAttributes().getReportAbstractor();
  		String reportCompanyAgent = ua.getMyAtsAttributes().getReportCompanyAgent();
  		String reportAgent = ua.getMyAtsAttributes().getReportAgent();
  		String reportStatus = ua.getMyAtsAttributes().getReportStatus();
  		
  		String reportSortBy = ua.getMyAtsAttributes().getReportSortBy();
  		String reportSortDir = ua.getMyAtsAttributes().getReportSortDir();
  		
		return URLMaping.path+URLMaping.REPORT_INCOME
			+ "?searchId=" + searchId
			+ "&type="+ThroughputOpCode.INCOME_BEAN
			+ "&" + TSOpCode.OPCODE + "=" + ThroughputOpCode.LOAD_PARAMETERS
			+ "&reportState="+reportState+"&reportCounty="+reportCounty
			+ "&reportCompanyAgent="+reportCompanyAgent
			+ "&reportAbstractor="+reportAbstractor
			+ "&orderBy="+reportSortBy
			+ "&orderType="+reportSortDir
			+ "&reportAgent="+reportAgent
			+ "&reportStatus="+reportStatus;
	}
	
	public static String getImageCountHref(long searchId) {
		return URLMaping.path+URLMaping.REPORT_IMAGE_COUNT + "?searchId=" + searchId;
	}
	
	public static String getDashboardHomeHref(long searchId) {
		
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		int yearR = c.get(Calendar.YEAR);
		int monthR = c.get(Calendar.MONTH) + 1;
		int dayR = c.get(Calendar.DAY_OF_MONTH);
		
		return URLMaping.path+URLMaping.REPORTS_MONTH_DETAILED 
			+ "?dayReport=" + dayR 
			+ "&monthReport=" + monthR 
			+ "&yearReport=" + yearR
			+ "&searchId=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.REPORT_MODULE
			+ "&" + RequestParams.REPORTS_DASHBOARD + "=" + 1
			+ "&reportStatus=20";
	}


	public static String getRepHomeDetailed(long searchId) {
		
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		int yearR = c.get(Calendar.YEAR);
		int monthR = c.get(Calendar.MONTH) + 1;
		int dayR = c.get(Calendar.DAY_OF_MONTH);
		
		return URLMaping.REPORTS_MONTH_DETAILED 
			+ "?dayReport=" + dayR 
			+ "&monthReport=" + monthR 
			+ "&yearReport=" + yearR
			+ "&searchId=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.REPORT_MODULE;
	}
	
	public static String getDashboardHomeDetailed(long searchId) {
		
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		int yearR = c.get(Calendar.YEAR);
		int monthR = c.get(Calendar.MONTH) + 1;
		int dayR = c.get(Calendar.DAY_OF_MONTH);
		
		return URLMaping.REPORTS_MONTH_DETAILED
			+ "?dayReport=" + dayR 
			+ "&monthReport=" + monthR 
			+ "&yearReport=" + yearR
			+ "&searchId=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.REPORT_MODULE
			+ "&" + RequestParams.REPORTS_DASHBOARD + "=" + 1
			+ "&reportStatus=20";
	}
	
	public static String getHomepage(long searchId, boolean isAgent, long agentId) {
		try {
			UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
			String homepage = ua.getDEFAULT_HOMEPAGE();
		
		if(homepage.equals(URLMaping.REPORTS_INTERVAL))
			return getDashboardHome(searchId, isAgent, agentId);
		if(homepage.equals(URLMaping.INVOICE_MONTH))
			return getInvoiceHome(searchId);
		if(homepage.equals(URLMaping.REPORTS_TABLE_MONTH))
			return getReportsTableHome(searchId);
		if(homepage.equals(URLMaping.StartTSPage))
			return getNewTSPage(searchId,true);
		
		}catch(Exception e) {
			e.printStackTrace();
		}
		return getDashboardHome(searchId, isAgent, agentId);
	}
	
	public static String getNewTSPage(long searchId, boolean isHome) {
		return URLMaping.StartTSPage 
		+ "?" + RequestParams.SEARCH_ID + "=" + searchId
		+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.NEW_TS
		+ "&isHome="+(isHome?"1":"0");
	}
	
	public static String getDashboardHome(long searchId, boolean isAgent, long agentId){
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		
		int minusDays = -30; 
		String start_date = ua.getMyAtsAttributes().getDASHBOARD_START_INTERVAL();
		String end_date = ua.getMyAtsAttributes().getDASHBOARD_END_INTERVAL();
			 
		int yearR = c.get(Calendar.YEAR);
		int monthR = c.get(Calendar.MONTH) + 1;
		int dayR = c.get(Calendar.DAY_OF_MONTH);
		
		int yearTo = c.get(Calendar.YEAR);
		int monthTo = c.get(Calendar.MONTH) + 1;
		int dayTo = c.get(Calendar.DAY_OF_MONTH);
		
		int yearFrom = c.get(Calendar.YEAR);
		int monthFrom = c.get(Calendar.MONTH) + 1;
		int dayFrom = c.get(Calendar.DAY_OF_MONTH);
			
		try {
			Pattern now_minus_number = Pattern.compile("^now(\\-[0-9]+)?$",Pattern.CASE_INSENSITIVE);
			Matcher matcher1 = now_minus_number.matcher(start_date);
			if(matcher1.matches()) { 
				 if(matcher1.group(1)!=null) {
					 minusDays= Integer.parseInt(matcher1.group(1));
				 }else{
					 minusDays = 0;
				 }
			}	
			c.add(Calendar.DATE, minusDays);
			yearFrom = c.get(Calendar.YEAR);
			monthFrom = c.get(Calendar.MONTH) + 1;
			dayFrom = c.get(Calendar.DAY_OF_MONTH);
			
			Pattern month_day_year = Pattern.compile("^((0|1)?[0-9])[/]([0-9]{1,2})[/]([0-9]{4})$");
			Matcher matcher2 = month_day_year.matcher(start_date);
			
			if(matcher2.matches()) { 
				yearFrom= Integer.parseInt(matcher2.group(4));
				 monthFrom= Integer.parseInt(matcher2.group(1));
				 dayFrom= Integer.parseInt(matcher2.group(3));
			}
			
			Matcher matcher3 = month_day_year.matcher(end_date);
			
			if(matcher3.matches()) { 
				yearTo= Integer.parseInt(matcher3.group(4));
				monthTo= Integer.parseInt(matcher3.group(1));
				dayTo= Integer.parseInt(matcher3.group(3));
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		String reportState= ua.getMyAtsAttributes().getReportState(); 
		String reportCounty = ua.getMyAtsAttributes().getReportCounty();
  		String reportAbstractor = ua.getMyAtsAttributes().getReportAbstractor();
  		String reportCompanyAgent = ua.getMyAtsAttributes().getReportCompanyAgent();
  		String reportAgent = ua.getMyAtsAttributes().getReportAgent();
  		String reportStatus = ua.getMyAtsAttributes().getReportStatus();
  		
  		String reportSortBy = ua.getMyAtsAttributes().getReportSortBy();
  		String reportSortDir = ua.getMyAtsAttributes().getReportSortDir();
  		String reportDefaultView = ua.getMyAtsAttributes().getReportDefaultView();
  		
  		  		
		return reportDefaultView
			+ "?dayReport=" + dayR 
			+ "&monthReport=" + monthR 
			+ "&yearReport=" + yearR
			+ "&" + RequestParams.REPORTS_TO_DAY + "=" + dayTo 
			+ "&" + RequestParams.REPORTS_TO_MONTH + "=" + monthTo 
			+ "&" + RequestParams.REPORTS_TO_YEAR + "=" + yearTo
			+ "&" + RequestParams.REPORTS_FROM_DAY + "=" + dayFrom
			+ "&" + RequestParams.REPORTS_FROM_MONTH + "=" + monthFrom
			+ "&" + RequestParams.REPORTS_FROM_YEAR + "=" + yearFrom
			+ "&searchId=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.REPORT_MODULE
			+ "&" + RequestParams.REPORTS_DASHBOARD + "=" + 1
			+ "&reportState="+reportState+"&reportCounty="+reportCounty
			+ "&reportCompanyAgent="+reportCompanyAgent
			+ (isAgent?"":"&reportAbstractor="+reportAbstractor)
			+ "&orderBy="+reportSortBy
			+ "&orderType="+reportSortDir
			+ (isAgent?"&reportAgent=" + agentId + "&reportAbstractor="+agentId:"&reportAgent="+reportAgent)
			+ "&reportStatus="+reportStatus;
	}
	
	public static String getDashboardHomeIntervalHref(long searchId, boolean isAgent, long agentId){
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		
		int minusDays = -30; 
		String start_date = ua.getMyAtsAttributes().getDASHBOARD_START_INTERVAL();
		String end_date = ua.getMyAtsAttributes().getDASHBOARD_END_INTERVAL();
				 
		int yearR = c.get(Calendar.YEAR);
		int monthR = c.get(Calendar.MONTH) + 1;
		int dayR = c.get(Calendar.DAY_OF_MONTH);
		
		int yearTo = c.get(Calendar.YEAR);
		int monthTo = c.get(Calendar.MONTH) + 1;
		int dayTo = c.get(Calendar.DAY_OF_MONTH);
		
		int yearFrom = c.get(Calendar.YEAR);
		int monthFrom = c.get(Calendar.MONTH) + 1;
		int dayFrom = c.get(Calendar.DAY_OF_MONTH);
		
		try {
			Pattern now_minus_number = Pattern.compile("^now(\\-[0-9]+)?$",Pattern.CASE_INSENSITIVE);
			Matcher matcher1 = now_minus_number.matcher(start_date);
			if(matcher1.matches()) { 
				 if(matcher1.group(1)!=null) {
					 minusDays= Integer.parseInt(matcher1.group(1));
				 }else{
					 minusDays = 0;
				 }
			}	
			c.add(Calendar.DATE, minusDays);
			yearFrom = c.get(Calendar.YEAR);
			monthFrom = c.get(Calendar.MONTH) + 1;
			dayFrom = c.get(Calendar.DAY_OF_MONTH);
			
			Pattern month_day_year = Pattern.compile("^((0|1)?[0-9])[/]([0-9]{1,2})[/]([0-9]{4})$");
			Matcher matcher2 = month_day_year.matcher(start_date);
			
			if(matcher2.matches()) { 
				yearFrom= Integer.parseInt(matcher2.group(4));
				 monthFrom= Integer.parseInt(matcher2.group(1));
				 dayFrom= Integer.parseInt(matcher2.group(3));
			}
			
			Matcher matcher3 = month_day_year.matcher(end_date);
			
			if(matcher3.matches()) { 
				yearTo= Integer.parseInt(matcher3.group(4));
				monthTo= Integer.parseInt(matcher3.group(1));
				dayTo= Integer.parseInt(matcher3.group(3));
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		String reportState= ua.getMyAtsAttributes().getReportState(); 
		String reportCounty = ua.getMyAtsAttributes().getReportCounty();
  		String reportAbstractor = ua.getMyAtsAttributes().getReportAbstractor();
  		String reportCompanyAgent = ua.getMyAtsAttributes().getReportCompanyAgent();
  		String reportAgent = ua.getMyAtsAttributes().getReportAgent();
  		String reportStatus = ua.getMyAtsAttributes().getReportStatus();
  		
  		String reportSortBy = ua.getMyAtsAttributes().getReportSortBy();
  		String reportSortDir = ua.getMyAtsAttributes().getReportSortDir();
  		String reportDefaultView = ua.getMyAtsAttributes().getReportDefaultView();

  		
		return URLMaping.path+reportDefaultView
			+ "?dayReport=" + dayR 
			+ "&monthReport=" + monthR 
			+ "&yearReport=" + yearR
			+ "&" + RequestParams.REPORTS_TO_DAY + "=" + dayTo 
			+ "&" + RequestParams.REPORTS_TO_MONTH + "=" + monthTo 
			+ "&" + RequestParams.REPORTS_TO_YEAR + "=" + yearTo
			+ "&" + RequestParams.REPORTS_FROM_DAY + "=" + dayFrom
			+ "&" + RequestParams.REPORTS_FROM_MONTH + "=" + monthFrom
			+ "&" + RequestParams.REPORTS_FROM_YEAR + "=" + yearFrom
			+ "&searchId=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.REPORT_MODULE
			+ "&" + RequestParams.REPORTS_DASHBOARD + "=" + 1
			+ "&reportState="+reportState+"&reportCounty="+reportCounty
			+ "&reportCompanyAgent="+reportCompanyAgent
			+ (isAgent?"":"&reportAbstractor="+reportAbstractor)
			+ "&orderBy="+reportSortBy
			+ "&orderType="+reportSortDir
			+ (isAgent?"&reportAgent=" + agentId + "&reportAbstractor="+agentId:"&reportAgent="+reportAgent)
			+ "&reportStatus="+reportStatus;
	}

	
	public static String getInvHomeHref(long searchId) {
		
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		int yearR = c.get(Calendar.YEAR);
		int monthR = c.get(Calendar.MONTH) + 1;
		int dayR = c.get(Calendar.DAY_OF_MONTH);
		
		return URLMaping.path+URLMaping.INVOICE_MONTH_DETAILED 
			+ "?dayReport=" +	dayR 
			+ "&monthReport=" + monthR 
			+ "&yearReport=" + yearR
			+ "&searchId=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.INVOICE_MODULE;
	}
	
	public static String getInvoiceHome(long searchId) {
		
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		int yearR = c.get(Calendar.YEAR);
		int monthR = c.get(Calendar.MONTH) + 1;
		int dayR = c.get(Calendar.DAY_OF_MONTH);
		
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		
		String reportState= ua.getMyAtsAttributes().getReportState(); 
		String reportCounty = ua.getMyAtsAttributes().getReportCounty();
  		String reportAbstractor = ua.getMyAtsAttributes().getReportAbstractor();
  		String reportCompanyAgent = ua.getMyAtsAttributes().getReportCompanyAgent();
  		String reportAgent = ua.getMyAtsAttributes().getReportAgent();
  		String reportStatus = ua.getMyAtsAttributes().getReportStatus();
  		
  		String reportSortBy = ua.getMyAtsAttributes().getReportSortBy();
  		String reportSortDir = ua.getMyAtsAttributes().getReportSortDir();
  		
		return URLMaping.INVOICE_MONTH 
			+ "?dayReport=" +	dayR 
			+ "&monthReport=" + monthR 
			+ "&yearReport=" + yearR
			+ "&searchId=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.INVOICE_MODULE
			+ "&reportState="+reportState+"&reportCounty="+reportCounty
			+ "&reportCompanyAgent="+reportCompanyAgent
			+ "&reportAbstractor="+reportAbstractor
			+ "&orderBy="+reportSortBy
			+ "&orderType="+reportSortDir
			+ "&reportAgent="+reportAgent
			+ "&reportStatus="+reportStatus;
	}
	
	public static String getReportsTableHome(long searchId) {
		
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		int yearR = c.get(Calendar.YEAR);
		int monthR = c.get(Calendar.MONTH) + 1;
		int dayR = c.get(Calendar.DAY_OF_MONTH);
		
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		
		String reportState= ua.getMyAtsAttributes().getReportState(); 
		String reportCounty = ua.getMyAtsAttributes().getReportCounty();
  		String reportAbstractor = ua.getMyAtsAttributes().getReportAbstractor();
  		String reportCompanyAgent = ua.getMyAtsAttributes().getReportCompanyAgent();
  		String reportAgent = ua.getMyAtsAttributes().getReportAgent();
  		String reportStatus = ua.getMyAtsAttributes().getReportStatus();
  		
  		String reportSortBy = ua.getMyAtsAttributes().getReportSortBy();
  		String reportSortDir = ua.getMyAtsAttributes().getReportSortDir();
  		
		return URLMaping.REPORTS_TABLE_MONTH 
			+ "?dayReport=" +	dayR 
			+ "&monthReport=" + monthR 
			+ "&yearReport=" + yearR
			+ "&searchId=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.REPORT_MODULE
			+ "&reportState="+reportState+"&reportCounty="+reportCounty
			+ "&reportCompanyAgent="+reportCompanyAgent
			+ "&reportAbstractor="+reportAbstractor
			+ "&orderBy="+reportSortBy
			+ "&orderType="+reportSortDir
			+ "&reportAgent="+reportAgent
			+ "&reportStatus="+reportStatus;
	}
	// communities
	
	public static String getCommAddLink(long searchId) {
		
		return URLMaping.path+URLMaping.COMMUNITY_PAGE_ADD 
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId 
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMMUNITY_PAGE_ADD_VIEW;
	}
	
	public static String getCategAddLink(long searchId) {
		return URLMaping.path+URLMaping.CATEGORY_PAGE_ADD 
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId 
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.CATEGORY_PAGE_ADD_VIEW;
	}
	
	public static String getCategAdminLink(long searchId) {
		return URLMaping.path+URLMaping.CATEGORY_PAGE_ADMIN 
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId 
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.CATEGORY_PAGE_ADMIN_VIEW;
	}
	
	public static String getCommAdminLink(long searchId) {
		return URLMaping.path+URLMaping.COMMUNITY_PAGE_ADMIN
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMMUNITY_PAGE_ADMIN_VIEW;
	}
	
	public static String getHideCommLink(long searchId) {
		return URLMaping.path+URLMaping.COMMUNITY_PAGE_HIDE
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMMUNITY_PAGE_ADMIN_HIDE;
	}
	
	public static String getCommViewLink(long searchId, long commID) {
		return URLMaping.path+URLMaping.COMMUNITY_PAGE_VIEW
			+ "?" + RequestParams.SEARCH_ID + "=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMMUNITY_PAGE_ADMIN_VIEW
			+ "&" + CommunityAttributes.COMMUNITY_ID + "=" + commID;
	}
	
	public static String getPolicyAdmin(long searchId) {
		long commId = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().longValue();
		return URLMaping.path+URLMaping.COMMUNITY_POLICY_MANAGE
		+ "?" + CommunityAttributes.COMMUNITY_ID + "=" + commId
		+ "&" + RequestParams.SEARCH_ID + "=" + searchId;
	}
	
	public static String getLogoHref(String commId, long searchId) {

		String[] params =
			{
				CommunityAttributes.COMMUNITY_ID,
				commId,				
				"update",
				new Integer(new Random().nextInt()).toString(),
				RequestParams.SEARCH_ID,
				String.valueOf(searchId),
				TSOpCode.OPCODE,
				String.valueOf(TSOpCode.DOWNLOAD_LOGO)
			};

		return URLMaping.getAbsoluteURL(URLMaping.DownloadLogo, params);
	}	
	public static String getSettingsHomeHref(long searchId){

		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		int yearR = c.get(Calendar.YEAR);
		int monthR = c.get(Calendar.MONTH) + 1;
		int dayR = c.get(Calendar.DAY_OF_MONTH);
		
		return URLMaping.path + URLMaping.SETTINGS_PAGE 
			+ "?dayReport=" +	dayR 
			+ "&monthReport=" + monthR 
			+ "&yearReport=" + yearR 
			+ "&" + RequestParams.SEARCH_ID + "=" + searchId
			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.SETTINGS_VIEW;
	}
	public static String getAssignHomeHref(long searchId){	
		return URLMaping.path+"/jsp/assign/assign.jsp?searchId="+searchId;
	}
	
	
	public static String getTestAgentHomeHref(long searchId){

			return URLMaping.path+"/jsp/filters.jsp?searchId="+searchId;//URLMaping.TESTAGENT_STATUS;
		}
	public static String getTestAgentStartHref(long searchId){
				return URLMaping.path+"/jsp/statusstarter.jsp";
			}
	
	public static String getTestToolsHomeHref(long searchId){
		return URLMaping.path+"/jsp/testTools.jsp?searchId="+searchId;
	}
	
	public static String getResetFormLink(){
		String href = "javascript:window.document.forms[0].reset()";
		String name = "Clear form"; 
		return getGenericLinkOnClick(href, "", name, 25) ;
	}
	
	public static String getSubmitOrderLink(){
		String href = "javascript:if (checkData()) {window.document.forms[0].action='" 
		    + URLMaping.path + URLMaping.UpdateTSRIndex
		    + "';window.document.forms[0]." + TSOpCode.OPCODE + ".value=" 
		    + TSOpCode.SUBMIT_ORDER + ";window.document.forms[0].submit() }";
		String name = "Submit order"; 
		return getGenericLinkOnClick(href, "", name, 25) ;
	}
	public static String getSendEmailLink(String from){
		String select = "";
		select = "<TABLE cellspacing=\"0\" cellpadding=\"0\" border=0> "+
			"<TR> " +           
			"<TD noWrap align=\"right\"> " +	
			"Send&nbsp;&nbsp;PDF:<input type=\"checkbox\" name=\"" + RequestParams.INVOICE_SEND_PDF_CHECKBOX + "\" id=\"" + RequestParams.INVOICE_SEND_PDF_CHECKBOX + "\" value=\"pdf\" onclick=\"javascript:checkUncheckEmail(this);\"/>&nbsp;" + 
			"XML:<input type=\"checkbox\" name=\"" + RequestParams.INVOICE_SEND_XML_CHECKBOX + "\" id=\"" + RequestParams.INVOICE_SEND_XML_CHECKBOX + "\" value=\"xml\" onclick=\"javascript:checkUncheckEmail(this);\"/>&nbsp;" +
			"XLS:<input type=\"checkbox\" name=\"" + RequestParams.INVOICE_SEND_XLS_CHECKBOX + "\" id=\"" + RequestParams.INVOICE_SEND_XLS_CHECKBOX + "\" value=\"xls\" onclick=\"javascript:checkUncheckEmail(this);\"/>&nbsp;" +
			"CSV:<input type=\"checkbox\" name=\"" + RequestParams.INVOICE_SEND_CSV_CHECKBOX + "\" id=\"" + RequestParams.INVOICE_SEND_CSV_CHECKBOX + "\" value=\"csv\" onclick=\"javascript:checkUncheckEmail(this);\"/>" +
			"</TD> "+
			"<TD valign=\"center\" noWrap>" + 
			"<input " +
				//"style=\"margin-left:5px;margin-right:5px;\" " +
				"type=\"button\" " +
				"value=\"Send...\" " +
				"class=\"submitLinkBlue\" " +
				"onClick=\"javascript:if(pageLoaded==1) { if(updateFormEmailFields()) sendEmail(); else alert('Please select an invoice type'); } else {alert('Please wait for the page to be loaded');}\" " +
				"onMouseOver=\"stm(" + 95 + ",Style[9])\" " +
				"onmouseout=\"htm()\" />" +
			"<input " +
				//"style=\"margin-left:5px;margin-right:5px;\" " +
				"type=\"button\" " +
				"value=\"Send to ALL\" " +
				"class=\"submitLinkBlue\" " +
				"onClick=\"javascript:if(pageLoaded==1) { if(updateFormEmailFields()) sendEmailToAll(); else alert('Please select an invoice type'); } else {alert('Please wait for the page to be loaded');}\" " +
				"onMouseOver=\"stm(" + 190 + ",Style[9])\" " +
				"onmouseout=\"htm()\" />" + 
			"</TD> "+
			"</TR> " + 
			"</TABLE>";
		
		
		return select;
	}
	
	public static String getFindOption(long searchId, String searchType){
		String findOption = "All hist:" + 
			"<INPUT TYPE='checkbox' name='" + RequestParams.REPORTS_SEARCH_ALL + "'"+ "  onclick='checkAllHist(document.getElementsByName(\""+RequestParams.REPORTS_SEARCH_ALL+"\")[0])' >\n" + 
			"&nbsp;" +
			"<INPUT id='TSRsearchString' type='text' name='" + RequestParams.REPORTS_SEARCH_TSR + "' size='20' onMouseOver=\"stm(210,Style[9])\" onmouseout=\"htm()\">" +
			"<script language='Javascript'>" +
			" var browser=navigator.appName; var b_version=navigator.appVersion; var version6=(b_version.indexOf('MSIE 6')>=0);"+
			" if (browser=='Microsoft Internet Explorer' && version6 == true) { \n" +
			" document.getElementById('TSRsearchString').title='Allowed formats: \\n\\n Property Address: \\n\\t street_number street_name OR any part \\n Property Owner: \\n\\t first_name last_name OR any part \\n TSR Name: \\n\\tany part of TSR name \\n Legal: \\n\\tSubdivision name Lot <no> Block <no> Sec <no> Phase <no> (valid search: Meadow Lot 5) \\n APN: \\n\\tany part of APN(Parcel ID) field';" +
			" document.getElementById('TSRsearchString').onmouseover  = function() {};"+
			" document.getElementById('TSRsearchString').onmouseout = function() {};"+
			" }"+
			" </script>"+
			"<INPUT type='hidden' name='" + RequestParams.TSR_SEARCH_TYPE + "' value='" + searchType + "'>\n" +
			"<INPUT type='hidden' name='" + RequestParams.REPORTS_SEARCH_FIELD + "' id='" + RequestParams.REPORTS_SEARCH_FIELD + "' >\n";
		
		return findOption;
	}
	public static String getFindOption(long searchId, String searchType, String allHist, 
			String searchFor, String searchField, String searchFieldFrom){
		if (searchType == ""){
			searchType = RequestParamsValues.TSR_SEARCH_INTERVAL;
		}
		String searchHtml = getFindOption(searchId, searchType);
		if("starter".equalsIgnoreCase(searchFieldFrom)){
			searchHtml = searchHtml.replaceFirst("<INPUT TYPE='checkbox' ", "<INPUT TYPE='checkbox' checked ");
			
		} else {
			if (allHist != null && allHist.equals("on")){
				searchHtml = searchHtml.replaceFirst("<INPUT TYPE='checkbox' ", "<INPUT TYPE='checkbox' checked ");
			}
		}
		if (searchFor == null){
			searchFor = "";
		}
		searchHtml = searchHtml.replaceFirst("<INPUT id='TSRsearchString' type='text' ", 
									"<INPUT id='TSRsearchString' type='text' value='" + searchFor + "' ");
		if (searchField == null){
			searchField = "";
		}
		searchHtml = searchHtml.replaceFirst("<INPUT type='hidden' name='" + RequestParams.REPORTS_SEARCH_FIELD + "'",
									"<INPUT type='hidden' name='" + RequestParams.REPORTS_SEARCH_FIELD + "'" + " value='" + searchField + "'");
		return searchHtml;
	}
}
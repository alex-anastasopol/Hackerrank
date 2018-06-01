/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.utils;

import java.io.File;
import java.net.URLEncoder;

/**
 * @author nae
 *
 */
public class URLMaping {
	
	public final static String path = "/title-search";

	//servlets	
	public final static String RE_ORDER_TSR		= "/ReOrderTSR";
	public final static String TesterServlet		= "/TesterServlet";
	public final static String ATTACH_IMAGE_SERVLET		= "/AttachImage";
	public final static String CONVERT_TO_PDF_SERVLET		= "/ConvertToPdf";
	public final static String REPORT_IMAGE_COUNT = "/jsp/reports/imagecount/reports_image_count.jsp";
	 
	public final static String INIT_CONTEXT_SERVLET		= "/InitContext";
	
	public final static String REMOVE_CHAPTERS_SERVLET 	= "/RemoveChapters";
	public final static String SEND_EMAIL_SERVLET 		= "/SendEmailServlet";
	public final static String SEND_XML_PARAM 		= "/SendXMLParam";
	public final static String DOCTYPE_SERVLET 		= "/DocTypeServlet";
	public final static String MAINTENANCEMESSAGE_SERVLET = "/MaintenanceMessageServlet";
	public final static String MESSAGE_RECEIVER_SERVLET = "/MessageReceiverServlet";
	public final static String SEND_NAME_COMPANY 		= "/CompanyException.xml";
	public final static String URL_CONN_READER_SERVLET 	= "/URLConnectionReader";
	public final static String USER_VALIDATION_SERVLET 	= "/UserValidation";
	public final static String validateInputs 				= "/ValidateInputs";
	public final static String Logout						= "/Logout";
	public final static String StartTS						= "/StartTS";
	public final static String AutomaticSearch				= "/AutomaticSearch";
	public final static String TSD							= "/TSD";
	public final static String SendPageViaEmail			= "/SendScreenViaEmail";
	public final static String loginDispacher 				= "/loginDispacher";
	public final static String UserDispacher  				= "/UserDispacher";
	public final static String ChangeContext				= "/ChangeContext";
	public final static String RedirectContext 			= "/RedirectContext";
	public final static String USER_ADMIN_SERVLET			= "/UserAdmin";
	public final static String DownloadLogo				= "/DownloadLogo";
	public final static String COMMUNITY_DISPCH			= "/CommunityDispacher";
	public final static String GET_PHOTO 					= "/DownloadFile";
	public final static String DownloadFileAs				= "/DownloadFileAs";
	public final static String TSDFileServlet				= "/TSDFileServlet";	
	public final static String ACTIONS_SERVLET 			= "/Actions";
	public final static String PARAMETERS_SERVLET 		= "/Parameters";
	public final static String BarChart						= "/BarChart";
	public final static String Invoice						= "/Invoice";
	public final static String ARCHIVE_DOWNLOAD				= "/ArchiveDownload";
	//public final static String SEARCH_TSR					= "/SearchTSR";
	public final static String UpdateInvoice				= "/UpdateInvoice";
	public final static String UpdateReports				= "/UpdateReports";
	public final static String AdminSettings 				= "/AdminSettings";
	public final static String CHECKXMLDOCTYPE 		    = "/CheckDocTypeXML";
	public final static String UpdateTSRIndex			="/UpdateTSRIndex";
	public final static String GetImagefromDisk 		    = "/GetImagefromDisk";
	public final static String COMM_UPLOAD_POL				= "/CommUploadPolicy";
	public final static String ContactUs					= "/ContactUs";
	public final static String UPDATE_BEAN_INDEX_SERVLET	= "/UpdateBeanIndex";
	public final static String SaveTestCase					= "/SaveTestCase";
	public final static String THROUGHPUT_SERVLET			= "/ThroughputServlet"; 		
    public final static String UpdateArea								= "/UpdateArea";
    public final static String FixTemplates								= "/FixTemplates";
    public final static String LoadBalancingServlet			= "/LoadBalancingServlet";
	public final static String GenericNameFilterTest		= "/GenericNameFilterTestCalculate";
	public final static String ManageCountyList				= "/ManageCountyList";
	public final static String FileServlet					= "/fs";
	public final static String GLOBAL_TEMPLATES				="/GlobalTemplates";
	public final static String GLOBAL_TEMPLATES_VIEW				="/GlobalTemplatesView";
	public final static String PARENT_SITE_ACTIONS				="/ParentSiteActions";

	
	// jsps
	public final static String JSP_DIR = path + "/jsp";

	
	public final static String LOGIN_PAGE  = "/jsp/login.jsp"; // nu are topframe
	public final static String StartTSPage = "/jsp/newTS.jsp";
	
	public final static String OrderTSPage = "/jsp/newTSOrder.jsp";
	public final static String TSDIndexedPage    = "/jsp/TSDIndexPage/TSDIndexPageFrameset.jsp";  //nu are top frame
	public final static String TSDIndexedPageJSP = "/jsp/TSDIndexPage/TSDIndexPage.jsp";
	public final static String AttachPage = "/jsp/TSDIndexPage/attach.jsp";
	public final static String UpdateHTMLIndexPage = "/jsp/TSDIndexPage/updateHTMLIndex.jsp";
	public final static String ERROR_PAGE = "/jsp/errorPage.jsp";
	public final static String NOT_IMPLEMENTED_PAGE = "/jsp/notImplementedPage.jsp";
	public final static String ParentSearch		= "/jsp/property1.jsp";
	public final static String PARENT_SITE_RESPONSE	= 	"/jsp/parentSiteResponse.jsp";
	public final static String SendPageViaEmailPage	= 	"/jsp/emaill.jsp";
	public final static String SendEmailAssignToPage	= 	"/jsp/emailAssignTo.jsp";
	public final static String UPLOAD_POLICY			= "/jsp/Community/UploadFile.jsp";
	
	public final static String USER_LIST		=	"/jsp/Users/UserList.jsp";
	public final static String USER_ADD			=	"/jsp/Users/useradd.jsp";
	public final static String USER_EDIT		=	"/jsp/Users/UserEdit.jsp";
	public final static String USER_VIEW		=	"/jsp/Users/UserView.jsp";
	public final static String USER_RATES_HISTORY	=	"/jsp/Users/UserRatesHistory.jsp";
	public final static String USER_ADMIN			= 	"/jsp/Users/useradmin.jsp";
	public final static String USER_SADMIN		 	= 	"/jsp/Users/usersadmin.jsp";
	public final static String USER_MANAGE_COUNTY	=	"/jsp/Users/manageCountyList.jsp";
	public final static String USER_RESTRICTIONS_JSP	=	"/jsp/Users/userRestrictions.jsp";
	public final static String USER_ASSIGN_JSP	=	"/jsp/Users/userOrders.jsp";
	public final static String USER_RATES_JSP	=	"/jsp/Users/userRates.jsp";
	
	public final static String MY_ATS_VIEW = "/jsp/Users/myATSView.jsp";
	public final static String MY_ATS_EDIT = "/jsp/Users/myATSEdit.jsp";

	public static final String AUTO_LOGON			= "/jsp/AutoLogon.jsp";
	public final static String MISSING_PS			= "/jsp/MissingPSFiles.jsp";
	public final static String PDF_SHOW		 		= "/jsp/PDFShow.jsp";
	public final static String SETTINGS_PAGE 		= "/jsp/Settings.jsp";
	public final static String SETTINGS_RATES_PAGE	= "/jsp/SettingsRates.jsp";
	public static final String EMAIL				= "/jsp/emaill.jsp";
	public static final String MULTIPLE_EMAIL		= "/jsp/multiple_email.jsp";
	public static final String SETTINGS				= "/jsp/Settings.jsp";
	public static final String PAYRATE_HISTORY		= "/jsp/PayrateHistory.jsp";
	public static final String loadBalancingPage	= "/jsp/loadBalancing.jsp";
	public static final String ATSInstances			= "/jsp/lbsServers.jsp";
	public static final String GenericNameFilter	= "/jsp/GenericNameFilterTest.jsp";
	public static final String USER_NEW_OR_RECOVER 	= "/jsp/newusers.jsp";
	
	public static final String GOOGLE_MAPS 	= "/jsp/googleMaps.jsp";

	private final static String instanceDir = System.getProperty("tsearch_inst", "local");

	public final static String INSTANCE_DIR	    = 	instanceDir;
	public final static String ORA_CONFIG		= 	"conf." + instanceDir + ".Ora";
	public final static String SERVER_CONFIG	= 	"conf." + instanceDir + ".ServerConfig";
	public final static String PARSER_CONFIG	= 	"conf.Parser";
	public final static String COMPANY_CONFIG	= 	"conf.Company";
	public final static String LOG4J_CONFIG		= 	"conf/" + instanceDir + "/Log4J.properties";

	public final static String REPORT_SEARCH	=	"/jsp/reports/reports_search.jsp";
	public final static String REPORT_MONTH		=	"/jsp/reports/reports_month.jsp";
	public final static String REPORT_YEAR		=	"/jsp/reports/reports_year.jsp";
	public final static String REPORT_DAY		=	"/jsp/reports/reports_day.jsp";
	public final static String REPORTS_MONTH_DETAILED	=	"/jsp/reports/reports_month_detailed.jsp";
	public final static String REPORTS_INTERVAL			=	"/jsp/reports/reports_interval.jsp";
	public final static String REPORT_THROUGHPUT	=	"/jsp/reports/reports_throughput.jsp";
	public final static String REPORT_THROUGHPUT_Y	=	"/jsp/reports/reports_throughput_year.jsp";
	public final static String REPORT_THROUGHPUT_M	=	"/jsp/reports/reports_throughput_month.jsp";
	public final static String REPORT_INCOME	=	"/jsp/reports/reports_income.jsp";
	public final static String REPORT_INCOME_Y	=	"/jsp/reports/reports_income_year.jsp";
	public final static String REPORT_INCOME_M	=	"/jsp/reports/reports_income_month.jsp";
	public final static String REPORTS_TABLE_MONTH = "/jsp/reports/reports_table_month.jsp";
	
	public static final String REPORTS_NOTE				= "/jsp/reports/note.jsp";
	public static final String LEGAL_DESCRIPTION		= "/jsp/legalDescription.jsp";

	public final static String INVOICE_MONTH			=	"/jsp/invoice/invoice_month.jsp";
	public final static String INVOICE_DAY				=	"/jsp/invoice/invoice_day.jsp";
	public final static String INVOICE_MONTH_DETAILED	=	"/jsp/invoice/invoice_month_detailed.jsp";
	public final static String INVOICE_INTERVAL			=	"/jsp/invoice/invoice_interval.jsp";
	public final static String INVOICE_SEARCH			=	"/jsp/invoice/invoice_search.jsp";
	
	public final static String TOP_FRAME_PAGE  = JSP_DIR + "/topframe.jsp";
	public final static String CONVERT_TO_PDF_REDIR = 	"/jsp/TSR/ConvertToPDFRedirect.jsp";
	public final static String CONVERT_TO_PDF_SHOW  = 	"/jsp/TSR/ConvertToPDFShow.jsp";
	public final static String CONVERT_TO_PDF_STOP 	= 	"/jsp/TSR/ConvertToPDFStop.jsp";

	public final static String UPDATE_BEAN_INDEX	= 	"/jsp/TSDIndexPage/updateBeanIndexInSession.jsp";
	public final static String CREATE_TSD		 	= 	"/jsp/TSR/CreateTSD.jsp";
	public final static String REDIRECT_TO_OPENER = "/jsp/RedirectToOpener.jsp";

	// community related
	public final static String GLOBAL_TEMPLATES_JSP="/jsp/templates/globalTemplatesEdit.jsp";
	public final static String DoCompileTemplates="/jsp/Community/doCompileTemplates.jsp";
	public final static String COMMUNITY_PAGE_VIEW = "/jsp/Community/CommunityView.jsp";
	public final static String COMMUNITY_PAGE_EDIT = "/jsp/Community/CommunityEdit.jsp";
	public final static String COMMUNITY_PAGE_ADD  = "/jsp/Community/CommunityAdd.jsp";
	public final static String COMMUNITY_PAGE_ADMIN  = "/jsp/Community/CommunityAdmin.jsp";
	public final static String COMMUNITY_PAGE_HIDE  = "/jsp/Community/HideCommunity.jsp";
	public final static String CATEGORY_PAGE_ADMIN  = "/jsp/Community/CategoryAdmin.jsp";
	public final static String CATEGORY_PAGE_ADD  = "/jsp/Community/CategoryAdd.jsp";
	public final static String COMMUNITY_POLICY_MANAGE = "/jsp/Community/CommManagePolicy.jsp";
	public final static String COMMUNITY_VIEW_FILE			= "/jsp/Community/ViewFile.jsp";		
	
	public final static String TOP_FRAME_JSP	   = "/jsp/topframe.jsp";

	public final static String JAVASCRIPTS_DIR = path + "/web-resources/javascripts";
	public final static String JAVASCRIPTS_INCLUDES_DIR = JAVASCRIPTS_DIR +"/includes";
	
	public final static String IMAGES_DIR = path + "/web-resources/images";
	public final static String STYLESHEETS_DIR = path + "/web-resources/images";
	public final static String HELP_DIR = path + "/web-resources/help";
	public final static String CSS_DIR = path + "/web-resources/css";
	
	
	//update
	public final static String  UPDATE_AREA_UPDATE		= "/jsp/UpdateArea/update.jsp";
	//	js
	
	public final static String PS_VALIDATE_JS							= JAVASCRIPTS_DIR + "/PSinclude.js";
	
	// js includes	
	public final static String TEMPLATES_TEXT_EDITOR_JS		=   JAVASCRIPTS_INCLUDES_DIR +"/templatesTextEditor.js";
	// public final static String TEMPLATES_HTML_EDITOR_JS = JAVASCRIPTS_INCLUDES_DIR +"/templatesTextEditor.js";
	
	
	// css
	
	public final static String PS_CSS_LAYOUT				= CSS_DIR + "/cssDef.css";
	
	public final static String HTML_RES_DIR = "/web-resources/htmlResources";
	public static final String PRIVACY_STATEMENT = "/jsp/PrivacyStatement.htm";
	public static final String TESTAGENT_STATUS="/jsp/Showstatus.html";

	public static final String EMAIL_OK_PAGE	= File.separator + "WEB-INF" + 
				File.separator + "classes" + 
				File.separator + "resource" + 
				File.separator + "utils" + 
				File.separator + "emailOk.html";
	
	public static final String PARENT_XML="src/rules/parentsitedescribe/";
 
	public static final String REUSE_SEARCH		=	"/ReuseSearch";
	
	public static final String SET_DEFAULT_LD   = 	"/jsp/setLegalDescription.jsp";
	
	// end community related	
	@SuppressWarnings("deprecation")
	public static String getAbsoluteURL(String url, String[] params) {

	if(params.length < 2) {
		return url;
	}

		String ps = new String("?");

		ps += params[0] + "=" + params[1];
		for(int i=2; i<params.length; i+=2) {
				ps += "&" + params[i] + "=" + URLEncoder.encode(params[i+1]);
		}
        
		return  URLMaping.path + url + ps;
	}
}

package ro.cst.tsearch.jsp.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Enumeration;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.AppLinks;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBReports;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.FileServlet;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.SharedDriveUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;
/**
 * @author nae
 */

/**
 * Button class holds the functionality related to a HTML representation of a
 * button
 */
public class TopBar {

	public static final String encondingType = "UTF-8";
	public static final int SHOW_STATE_COUNTY = 1;
	
	static final int DEFAULT_WIN = 0;

	static final int REP_WIN = 1;

	protected Search global = null;

	protected String firstTitle = "";

	protected String secondTitle = "";

	protected String helpChapter = "";

	protected String helpFile = "";

	protected boolean prn = true;
	
	protected boolean email  = true;
	

	protected String extraText = "";

	protected boolean noButtons = false;

	protected boolean noBack = false;

	protected boolean noHome = false;

	protected boolean showFileId = false;

	protected boolean showStateCounty = false;
	
	protected int opCode = -1;

	protected HttpServletRequest request = null;

	public TopBar(Search global, String firstTitle, String secondTitle,
			String helpChapter, String helpFile, int opCode,
			HttpServletRequest request) {
		this(firstTitle, secondTitle, helpChapter, helpFile, request);
		this.global = global;
		if (!global.getSa().isSet()) {
			this.opCode = opCode;
		}
		this.request = request;
	}

	protected TopBar(String firstTitle, String secondTitle, String helpChapter,
			String helpFile, HttpServletRequest request) {
		this.firstTitle = firstTitle;
		this.secondTitle = secondTitle;
		this.helpChapter = helpChapter;
		this.helpFile = helpFile;
		this.request = request;
	}
	
	protected TopBar(String firstTitle, String secondTitle, String helpChapter,
			String helpFile, HttpServletRequest request, int[] whatToShow) {
		this.firstTitle = firstTitle;
		this.secondTitle = secondTitle;
		this.helpChapter = helpChapter;
		this.helpFile = helpFile;
		this.request = request;
		
		for(int i=0;i<whatToShow.length; i++) {
			switch(whatToShow[i]) {
				case SHOW_STATE_COUNTY:
					this.showStateCounty = true; 
					break;
			}
		}
	}
	
    //in asta intra
	public TopBar(Search global, String firstTitle, String secondTitle,
			String helpChapter, String helpFile, HttpServletRequest request) {
		this(firstTitle, secondTitle, helpChapter, helpFile, request);
		this.global = global;
		this.request = request;
	}

	public TopBar(Search global, String firstTitle, String secondTitle,
			String helpChapter, String helpFile, HttpServletRequest request,boolean viewemail) {
		this(firstTitle, secondTitle, helpChapter, helpFile, request);
		this.global = global;
		this.request = request;
		this.email = viewemail;
	}
	
	public TopBar(Search global, String firstTitle, String secondTitle,
			String helpChapter, String helpFile, boolean showFileId,
			HttpServletRequest request) {
		this(firstTitle, secondTitle, helpChapter, helpFile, request);
		this.global = global;
		this.showFileId = showFileId;
		this.request = request;
	}

	public TopBar(Search global, String firstTitle, String secondTitle,
			String helpChapter, String helpFile, HttpServletRequest request, int[] whatToShow) {
		this(firstTitle, secondTitle, helpChapter, helpFile, request, whatToShow);
		this.global = global;		
		this.request = request;
	}	
	
	public TopBar(Search global, String firstTitle, String secondTitle,
			String helpChapter, String helpFile, int opCode,
			HttpServletRequest request, int[] whatToShow) {
		this(firstTitle, secondTitle, helpChapter, helpFile, request,whatToShow);
		this.global = global;
		if (!global.getSa().isSet()) {
			this.opCode = opCode;
		}		
		this.request = request;
	}
	
	public void setPrinterFlag(boolean prn) {
		this.prn = prn;
	}

	public void setNoButtonsFlag(boolean noButtons) {
		this.noButtons = noButtons;
	}

	public void setNoBackFlag(boolean noBack) {
		this.noBack = noBack;
	}

	public void setNoHomeFlag(boolean noHome) {
		this.noHome = noHome;
	}

	public void setExtraText(String extraText) {
		this.extraText = extraText;
	}

	public static String getBroserWindowTitle(Search global, String pageTitle) {
		return getBroserWindowTitle(global, pageTitle, false);
	}

	public static String getBroserWindowTitle(Search global, String pageTitle,
			boolean showFileId) {
		String fileno = "";
		long searchId = 0;
		if (global != null) {
			SearchAttributes sa = global.getSa();
			fileno = sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
			searchId = global.getID();
		}
		if (showFileId)
			return getBrowserWindowTitle(showFileId, fileno, pageTitle, searchId);
		return getBrowserWindowTitle(showFileId, "", pageTitle, searchId);
	}

	public static String getBrowserWindowTitle(boolean showFileId, String fileno, String pageTitle, long searchId) {
//		String rez = "<title>Advanced Title Search: " + 
//		StringUtils.HTMLEntityEncode(pageTitle);
		
		String rez = "<title>" + StringUtils.HTMLEntityEncode(pageTitle);
		
		if(showFileId) {
			if (fileno.length() > 0) {
				rez += " [" + 
				StringUtils.HTMLEntityEncode(fileno) + 
				"]";
			} else {
				rez += " []";
			}
			rez += "[" + Long.toString(searchId) + "]";
			try {
				rez+= "[" + DBReports.getReportStatus(searchId, searchId) + "]";
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return rez + "</title>";
	}

	public static String getHeader(String pageTitle) {
		return getHeader(null, pageTitle);
	}

	/**
	 * This is a static method that is very annoying 
	 * Use getTopBarHeaded instead
	 * @param global
	 * @param pageTitle
	 * @return
	 */
	@Deprecated
	public static String getHeader(Search global, String pageTitle) {
		return getHeader(global, pageTitle, false);
	}
	
	public String getTopBarHeader(Search global, String pageTitle) {
		return getHeader(global, pageTitle, showFileId);
	}

	public static String getHeader(Search global, String pageTitle,
			boolean showFileId) {
		String rez = TopBar.getBroserWindowTitle(global, pageTitle, showFileId)
				+ "\n";
		rez += getHeaderMetaNoCache();
		rez += "<LINK media=screen href='" + URLMaping.STYLESHEETS_DIR
				+ "/default.css' type=text/css rel=stylesheet>\n";
		rez += "<link rel=\"SHORTCUT ICON\" href=\"" + URLMaping.path + "/favicon.ico\">\n";

		return rez;
	}

	public static String getReportsHeader(Search global, String pageTitle) {
		String rez = TopBar.getBroserWindowTitle(global, pageTitle) + "\n";
		rez += getHeaderMetaNoCache();
		rez += "<LINK media=screen href='" + URLMaping.STYLESHEETS_DIR
				+ "/default_reports.css' type=text/css rel=stylesheet>\n";
		return rez;
	}

	public static String getSettingRatesHeader(Search global, String pageTitle) {
		String rez = TopBar.getBroserWindowTitle(global, pageTitle) + "\n";
		rez += getHeaderMetaNoCache();
		rez += "<LINK media=screen href='" + URLMaping.STYLESHEETS_DIR
				+ "/default_reports.css' type=text/css rel=stylesheet>\n";
		return rez;
	}

	public static String getHeaderMetaNoCache() {
		String rez = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">\n";
		rez += "<meta http-equiv=\"expires\" content=\"-1\">\n";
		rez += "<meta http-equiv=\"Cache-Control\" content=\"no-cache\">\n";
		rez += "<meta http-equiv=\"Pragma\" content=\"no-cache\">\n";
		rez += "<link rel=\"SHORTCUT ICON\" href=\"" + URLMaping.path + "/favicon.ico\">\n";
		return rez;
	}

	public static String getTitle(Search global, String pageTitle) {
		return getTitle(global, pageTitle, true);
	}

	public static String getTitle(Search global, String pageTitle,
			boolean showFileId) {
		String fileno = "";
		String so_order_id = "";
		long searchId = 0;
		if (global != null) {
			SearchAttributes sa = global.getSa();
			fileno = sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
			so_order_id = sa.getAtribute(SearchAttributes.STEWARTORDERS_ORDER_ID);
			searchId = global.getID();
		}
		String rez = "<font class=frameTitleFont>  "
				+ "Advanced Title Search&trade;: " + pageTitle;
		if (showFileId) {
			if (fileno.length() > 0 ) {
				rez += " [" + 
				StringUtils.HTMLEntityEncode(fileno) 
				+ "]";
			} else {
				rez += " []";
			}
			rez += "[" + Long.toString(searchId) + "]";
			if (!StringUtils.isEmpty(so_order_id)) {
				rez += "[" + so_order_id + "]";
			}
			try {
				rez+= "[" + DBReports.getReportStatus(searchId, searchId) + "]";
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		return rez + "</font>  ";
	}

	public static String getCopyright() {
		return "<font style=\"FONT-SIZE: 11px\">  &copy; ATS </font>";
	}

	protected String getEmailHref(String firstTitle, String secondTitle,
			long searchId) {
		if (searchId > 0) {
			global = InstanceManager.getManager().getCurrentInstance(searchId)
					.getCrtSearchContext();
		}
		// HttpServletRequest request = getRequest(searchId);
		String strA1 = "A";
		String strZ1 = "Z";
		String strA2 = "a";
		String strZ2 = "z";

		String queryStr = "";
		StringBuffer rebuildURL = new StringBuffer("");
		
		if(request!=null){
			Enumeration en = request.getParameterNames();
	
			
			String nameX;
			String[] paramX;
			while (en.hasMoreElements()) {
				nameX = en.nextElement().toString();
				if (((nameX.charAt(0) >= strA1.charAt(0)) && (nameX.charAt(0) <= strZ1
						.charAt(0)))
						|| ((nameX.charAt(0) >= strA2.charAt(0)) && (nameX
								.charAt(0) <= strZ2.charAt(0)))
						&& (!nameX.startsWith("chapter")) // depaseste 1024
				) {
	
					try {
						paramX = request.getParameterValues(nameX);
						if (paramX == null)
							paramX = new String[0];
						for (int i = 0; i < paramX.length; i++) {
							queryStr = queryStr + nameX + "="
								+ URLEncoder.encode(paramX[i], encondingType) + "&";	
						}
						
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
			
			rebuildURL = request.getRequestURL();
		}
		queryStr = queryStr + RequestParams.EMAIL_REQUEST + "="
				+ RequestParamsValues.TRUE;

		
		String title = firstTitle;
		if (!secondTitle.equals("")) {
			title += " - " + secondTitle;
		}
		
		if(showStateCounty) {
			try {
			title += " in " +
						State.getState(new BigDecimal(((global.getSa()).getAtribute(SearchAttributes.P_STATE)))).getStateAbv() + 
						"_" + 
						County.getCounty(new BigDecimal(((global.getSa()).getAtribute(SearchAttributes.P_COUNTY)))).getName();
			}
			catch (Exception e) {
				// logger.error(e.printStackTrace());
			}
		}
		
		String fileno = "";
		if (global != null) {
			SearchAttributes sa = global.getSa();
			fileno = sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
		}
		if (fileno.length() > 0) {
			title += " - [" + fileno + "]";
		}

		CurrentInstance ci = InstanceManager.getManager().getCurrentInstance(
				global.getID());
		UserAttributes ua = ci.getCurrentUser();
		String userEmail = ua.getAttribute(UserAttributes.EMAIL).toString();

		try {
			InternetAddress[] from = InternetAddress.parse(userEmail);
			if (from != null && from.length > 1 && from[0] != null) {

				userEmail = from[0].getAddress();

			}
		} catch (AddressException e) {
			e.printStackTrace();
		}

		String jsAdditional = "";
		if (opCode == TSOpCode.VALIDATE_INPUTS_ONLY_TO_SEND_EMAIL) {
			jsAdditional = "javascript:document.forms[0]" + ".action='"
					+ URLMaping.path + URLMaping.validateInputs + "';"
					+ "window.document.forms[0]." + RequestParams.FORWARD_LINK
					+ ".value=" + "'" + URLMaping.StartTSPage + "'" + ";"
					+ "window.document.forms[0]." + TSOpCode.OPCODE
					+ ".value='" + TSOpCode.VALIDATE_INPUTS_ONLY_TO_SEND_EMAIL
					+ "';" + "window.document.forms[0].submit();";

			return jsAdditional;
		}
		String sur = "1";
		String enc = StringUtils.HTMLEntityEncode("'");
		title = title.replace(enc, "\\" + enc);
		title = title.replace("'", "\\'");
		String s =""; 
		
		String method = request!=null ? request.getMethod() : "GET";
		
		try {
		s = "javascript:"
				+ "emailTS"
				+ "=window.open('"
				+ URLMaping.path
				+ URLMaping.SendPageViaEmailPage
				+ "?"
				+ RequestParams.SEARCH_ID
				+ "="
				+ searchId
				+ "&"
				+ "Method"
				+ "="
				+ method
				+ "&"
				+ "CurPageURL"
				+ "="
				+ rebuildURL.toString()
				+ "&"
				+ "ProposeSubject"
				+ "="
				+ title
				+ "&"
				+ "ProposeFrom"
				+ "="
				+ URLEncoder.encode(userEmail,encondingType)
				+ "&"
				+ "sursa"
				+ "="
				+ sur
				+ "&"
				+ "QueryStr"
				+ "="
				+ queryStr.replaceAll("&", "__").replaceAll("=", "||")
				+ "','"
				+ "emailTS"
				+ "','width=850,height=480,top=185,left=20,resizable=yes,toolbar=no,status=yes,scrollbars=auto');"
				+ "emailTS" + ".focus();";
		
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return s;
	}

	protected String getEmailLink(String firstTitle, String secondTitle) {
		return getEmailLink(firstTitle, secondTitle, Search.SEARCH_NONE);

	}

	protected String getEmailLink(String firstTitle, String secondTitle,
			long searchId) {
		return getGenericLinkOnClick(getEmailHref(firstTitle, secondTitle,
				searchId), "emailSwapImage", "ico_square_email.gif",
				"ico_square_email2.gif", 7);
	}

	protected String getGenericLink(String href, String imgName, String icon1,
			String icon2, int tooltipNo) {
		return "<a href=\""
				+ href
				+ "\"  onMouseOut=\"swapImgRestore();htm()\""
				+ "        onMouseOver=\"swapImage('"
				+ imgName
				+ "','','"
				+ URLMaping.IMAGES_DIR
				+ "/"
				+ icon2
				+ "',1);"
				+ "						stm("
				+ tooltipNo
				+ ",Style[9])\""
				+ "><img src='"
				+ URLMaping.IMAGES_DIR
				+ "/"
				+ icon1
				+ "' width='17' height='17' border='0' "
				+ "name='"
				+ imgName
				+ "' align='absmiddle'></a>"
				+ "<img src='"
				+ URLMaping.IMAGES_DIR
				+ "/spacer.gif' width='2' height='17' border='0' align='absmiddle'>";
	}

	protected String getGenericLinkOnClick(String href, String imgName,
			String icon1, String icon2, int tooltipNo) {
		return getGenericLink("javascript:;\" onClick=\"" + href, imgName, icon1, icon2,
				tooltipNo);
	}

	protected String getHomeHref(long searchId) {
		return AppLinks.getBackToSearchPageHref(searchId);
	}

	protected String getHomeLink(long searchId) {

		return getGenericLink(getHomeHref(searchId), "homeSwapImage",
				"ico_square_home.gif", "ico_square_home2.gif", 5);

	}

	protected String getPrintLink() {
		if("".equals(docLink)){
			return getGenericLinkOnClick("javascript:window.print();",
					"prnSwapImage", "ico_square_prn.gif", "ico_square_prn2.gif", 6);
		}else{
			return getGenericLinkOnClick("javascript: var newWin = window.open('" + docLink + "'); newWin.setTimeout('window.print()', 1000);",
					"prnSwapImage", "ico_square_prn.gif", "ico_square_prn2.gif", 174);
		}
	}

	protected String getHelpLink() {
		String help = "";
		if(!StringUtils.isEmpty(helpChapter) && !StringUtils.isEmpty(helpFile)) {
			help = "chapter='"+helpChapter+"';helpfile='"+helpFile+"';";
		}
		String href = "javascript:" + help + "mywindow=window.open('"
				+ URLMaping.HELP_DIR
				+ "/helpset_logo.htm',"
				+ "'mywindow','toolbar=yes,location=yes,directories=no,status=yes,"
				+ "menubar=yes,scrollbars=yes,resizable=Yes,width=800,height=415'); mywindow.focus();";

		return getGenericLinkOnClick(href, "helpSwapImage", "ico_square_q.gif",
				"ico_square_q2.gif", 8);
	}

	protected String getOrderLink(Search search, long userId, boolean forceShowOrderLink) {
		if (!forceShowOrderLink && request != null)
			if (search.isFakeSearch() || "true".equals(request.getParameter("original"))) {
				return "";
			}
		
		boolean hasOrder = false;
    	
    	String fullPath = SharedDriveUtils.getSharedLogFolderForSearch(global.getID());
		fullPath += "orderFile.html";
		try {
			hasOrder = new File(fullPath).exists();
		} catch (Exception e) {
			System.err.println("Check samba failed for path " + fullPath);
			e.printStackTrace();
			Log.sendExceptionViaEmail(
					MailConfig.getMailLoggerToEmailAddress(), 
					"Order File Check on Samba failed", 
					e, 
					"SearchId used: " + global.getID() + ", path used: " + fullPath);
		}
    	if(hasOrder) {
    		String href = "javascript:mywindow=window.open('"
					+ URLMaping.path
					+ "/jsp/TSDIndexPage/viewDescription.jsp?original=true&view=" +
					FileServlet.VIEW_ORDER + "&viewOrder=1&userId=" +
					"&viewDescrSearchId=" +
					search.getID() + "&" + RequestParams.SHOW_FILE_ID + "=true',"
					+ "'mywindow','toolbar=no,location=no,directories=no,status=no,"
					+ "menubar=no,scrollbars=no,resizable=Yes,width=750,height=520'); mywindow.focus();";

			href = href.replaceAll("\\\\", "/");

			return getGenericLinkOnClick(href, "orderSwapImage", "ico_order_1.gif",
					"ico_order_2.gif", 78);
    	} else {
		
			//first go ask DBManager
			if (DBReports.hasOrderInDatabase(search.getID())) {
	
				String href = "javascript:mywindow=window.open('"
						+ URLMaping.path
						+ "/jsp/TSDIndexPage/viewDescription.jsp?original=true&view=" +
						FileServlet.VIEW_ORDER + "&viewOrder=1&userId=" +
						"&viewDescrSearchId=" +
						search.getID() + "&" + RequestParams.SHOW_FILE_ID + "=true',"
						+ "'mywindow','toolbar=no,location=no,directories=no,status=no,"
						+ "menubar=no,scrollbars=no,resizable=Yes,width=750,height=520'); mywindow.focus();";
	
				href = href.replaceAll("\\\\", "/");
	
				return getGenericLinkOnClick(href, "orderSwapImage", "ico_order_1.gif",
						"ico_order_2.gif", 78);
			}
			
			
			File orderFile = new File("");
			if (!search.isFakeSearch())
				orderFile = new File(BaseServlet.FILES_PATH + File.separator + search.getRelativePath() + "orderFile.html");
	
			if (orderFile.exists()) {
	
				String href = "javascript:mywindow=window.open('"
						+ URLMaping.path
						+ "/fs?f="
						+ search.getRelativePath()
						+ "orderFile.html&searchId=" + search.getID() + "',"
						+ "'mywindow','toolbar=no,location=no,directories=no,status=no,"
						+ "menubar=no,scrollbars=no,resizable=Yes,width=750,height=520'); mywindow.focus();";
	
				href = href.replaceAll("\\\\", "/");
	
				return getGenericLinkOnClick(href, "orderSwapImage", "ico_order_1.gif",
						"ico_order_2.gif", 78);
	
			}
    	}

		return "";
	}

	protected String getCloseLink() {
		String href = "javascript:history.go(-1);";

		return getGenericLink(href, "closeSwapImage", "ico_square_close.gif",
				"ico_square_close2.gif", 9);
	}

	/**
	 * 
	 * foloseste numai pentru a executa linkul de email in pagina de search
	 * 
	 * @return
	 */
	public String getEmailLinkStandAlone() {
		long searchId = Search.SEARCH_NONE;
		if (global != null) {
			searchId = global.getSearchID();
		}
		return getEmailLink(firstTitle, secondTitle, searchId).replaceAll(
				"javascript:", "");
	}

	public String toString() {

		long searchId = Search.SEARCH_NONE;
		if (global != null) {
			searchId = global.getSearchID();
		}

		return toString(searchId);
	}

	public String toString(long searchId) {

		String jsHelp = "<SCRIPT language=JavaScript>\n" + "var chapter = '"
				+ helpChapter + "';\n" + "var helpfile = '" + helpFile + "';\n"
				+ "</SCRIPT>\n";

		String title = firstTitle;
		if (!secondTitle.equals("")) {
			title += "-" + secondTitle;
		}

		if(showStateCounty) {
			title += "<span id='topBarStateCounty'> ";
			try {
			title += " in " +
						State.getState(new BigDecimal(((global.getSa()).getAtribute(SearchAttributes.P_STATE)))).getStateAbv() + 
						"_" + 
						County.getCounty(new BigDecimal(((global.getSa()).getAtribute(SearchAttributes.P_COUNTY)))).getName();
			}
			catch (Exception e) {
				// logger.error(e.printStackTrace());
			}
			title += "</span>";
		}
		
		String s = "<TABLE height=19 cellSpacing=0 cellPadding=0 width='100%' border=0>"
				+ "<TBODY><TR>"
				+ "<TD vAlign=center noWrap align=left>"
				+ getTitle(global, title, showFileId)
				+ "  "
				+extraText
				+ "</TD>" + "<TD vAlign=center noWrap align=right> ";
		
		
		long userId; 
		User currentUser = null;
		UserAttributes ua = null;
		
		if(request==null){
			if(this.global.getAdditionalInfo("USER_ATTRIBUTES")!=null) {
				ua = (UserAttributes) this.global.getAdditionalInfo("USER_ATTRIBUTES");
			}
			if (ua !=null){
				userId = ua.getID().longValue();
			} else {
				userId = 0;
			}
		} else {
			userId = ((User) request.getSession().getAttribute(SessionParams.CURRENT_USER)).getUserAttributes().getID().longValue();	
			currentUser	= (User) request.getSession().getAttribute(SessionParams.CURRENT_USER);
		}
		
		
		if (!noButtons) {
			if (!noHome){
				//s += getHomeLink(searchId);
				
					
				boolean isAgent = false;
				try {
					isAgent = ua==null ? UserUtils.isAgent(currentUser.getUserAttributes()) : UserUtils.isAgent(ua);
				}catch(BaseException e) {}
				
				String link = 
					"<a href=\"#\" " 
					+ " onMouseOut=\"swapImgRestore();htm();\""
					+ " onMouseOver=\"swapImage('homeSwapImage','','" + URLMaping.IMAGES_DIR + "/ico_square_home2.gif',1); stm(134,Style[9]);\""
					+ " onClick=\"javascript: "
					  + " var wn=window.open('" + URLMaping.path + AppLinks.getHomepage(searchId,isAgent,userId) + "','Dashboard" + userId + "','width=1024,height=725,top=0,left=0,resizable=yes,toolbar=yes,status=yes,scrollbars=yes'); "
					  + " if(window.name != 'Dashboard' + " + userId + " ) window.close(); wn.focus(); \""
					+ ">"
					+ "<img src='"+ URLMaping.IMAGES_DIR + "/ico_square_home.gif' width='17' height='17' border='0' name='homeSwapImage' align='absmiddle'>"
					+ "</a>"
					+ "<img src='" + URLMaping.IMAGES_DIR + "/spacer.gif' width='2' height='17' border='0' align='absmiddle'>";			
				s += link;									
			}
			if (prn) {
				s += getPrintLink();
				if(email){
					s += getEmailLink(firstTitle, secondTitle, searchId);
				}
			}
			
			boolean forceShowOrderLink = false;
			
			if (request != null && "2".equals(request.getParameter("view")))
					forceShowOrderLink = true;
			
			if (global != null && (!global.isFakeSearch() || forceShowOrderLink == true)) {
				s += getOrderLink(global, userId, forceShowOrderLink);
			}

			s += getHelpLink();
			if (!noBack)
				s += getCloseLink();
		}
		s += "    </TD>" + "  </TR></TBODY></TABLE>";
		return jsHelp + s;
	}
	
	public String getLinks(boolean home, boolean print, boolean email, boolean order, boolean help, boolean close){
		String s="";
		
		//s += getHomeLink(searchId);
		long userId; 
		User currentUser = null;
		UserAttributes ua = null;
		
		if(request==null){
			if(this.global.getAdditionalInfo("USER_ATTRIBUTES")!=null)
				ua = (UserAttributes) this.global.getAdditionalInfo("USER_ATTRIBUTES");
			if (ua !=null){
				userId = ua.getID().longValue();
			} else 
				userId = 0;
		} else {
			userId = ((User) request.getSession().getAttribute(SessionParams.CURRENT_USER)).getUserAttributes().getID().longValue();	
			currentUser	= (User) request.getSession().getAttribute(SessionParams.CURRENT_USER);
		}
			
		boolean isAgent = false;
		try {
			isAgent = ua==null ? UserUtils.isAgent(currentUser.getUserAttributes()) : UserUtils.isAgent(ua);
		}catch(BaseException e) {}
		
		String link = 
			"<a href=\"#\" " 
			+ " onMouseOut=\"swapImgRestore();htm();\""
			+ " onMouseOver=\"swapImage('homeSwapImage','','" + URLMaping.IMAGES_DIR + "/ico_square_home2.gif',1); stm(134,Style[9]);\""
			+ " onClick=\"javascript: "
			  + " var wn=window.open('" + URLMaping.path + AppLinks.getHomepage(this.global.getID(),isAgent,userId) + "','Dashboard" + userId + "','width=1024,height=725,top=0,left=0,resizable=yes,toolbar=yes,status=yes,scrollbars=yes'); "
			  + " if(window.name != 'Dashboard' + " + userId + " ) window.close(); wn.focus(); \""
			+ ">"
			+ "<img src='"+ URLMaping.IMAGES_DIR + "/ico_square_home.gif' width='17' height='17' border='0' name='homeSwapImage' align='absmiddle'>"
			+ "</a>"
			+ "<img src='" + URLMaping.IMAGES_DIR + "/spacer.gif' width='2' height='17' border='0' align='absmiddle'>";			
		if(home)
			s += link;									

		if(print)
			s += getPrintLink();
		
		if(email)
			s += getEmailLink(firstTitle, secondTitle, this.global.getID());

		if (order && global != null /*&& !global.isFakeSearch()*/) {
			s += getOrderLink(global, userId, true);
		}

		if(help)
			s += getHelpLink();
		
		if (close)
			s += getCloseLink();
		
//		s += "</TD>";
		
		return s;
	}
	
	private String docLink = "";
	
	public String getDocLink(){
		return docLink;
	}
	
	public void setDocLink(String docLink){
		this.docLink = docLink;
	}

	/**
	 * @return the showFileId
	 */
	public boolean isShowFileId() {
		return showFileId;
	}

	/**
	 * @param showFileId the showFileId to set
	 */
	public void setShowFileId(boolean showFileId) {
		this.showFileId = showFileId;
	}

}
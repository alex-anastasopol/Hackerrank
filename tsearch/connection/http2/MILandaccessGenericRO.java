package ro.cst.tsearch.connection.http2;

import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.tools.codec.Base64Encoder;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.InstanceManager;

public abstract class MILandaccessGenericRO extends HttpSite {

	protected String countyName = "";
	protected String searchPath = "";
	protected String countyCode = "";
	protected String instName = "";

	public static final Pattern idPattern = Pattern.compile("(?is)idcode=([^\']*)");
	public static final Pattern btPattern = Pattern.compile("(?i)<input type=hidden name=btransact value='([^']+)'>");

	// search parameters order for first page after login
	private static final String [] PARAM_POS_IGGSNS             = new String[] { "County", "sealcode", "countyname", "btransact", "township", "govunit", "cityr", "idcode" };
	
	// search parameters order for each search module
	private static final String [] PARAM_POS_DOC_NO_SEARCH      = new String[] {"County", "countyname", "btransact", "searchtype", "township", "govunit", "cityr", "idcode", "instnum", "year", "docsplt", "matchtype", "appliccode", "instrumenttype", "instrumenttypecode", "dayhist", "list", "sealcode", "Contents", "Title", "Submit222" };
	private static final String [] PARAM_POS_NAME_SEARCH        = new String[] {"County", "countyname", "btransact", "searchtype", "township", "govunit", "cityr", "idcode", "lastname", "firstname", "suffix", "corpname", "series", "appliccode", "instrumenttype", "instrumenttypecode", "DateFrom", "DateTo", "sorttype", "dayhist", "list", "sealcode", "Contents", "Title", "Submit222" };
	private static final String [] PARAM_POS_ASSOC_SEARCH       = new String[] {"County", "countyname", "btransact", "searchtype", "township", "govunit", "cityr", "idcode", "book", "page", "matchtype", "appliccode", "instrumenttype", "instrumenttypecode", "DateFrom", "DateTo", "dayhist", "list", "sealcode", "Contents", "Title", "Submit222" };	
	private static final String [] PARAM_POS_BOOK_PAGE_SEARCH   = new String[] {"County", "countyname", "btransact", "searchtype", "township", "govunit", "cityr", "idcode", "book", "page", "matchtype", "appliccode", "instrumenttype", "instrumenttypecode", "DateFrom", "DateTo", "dayhist", "list", "sealcode", "Contents", "Title", "Submit222" };
	private static final String [] PARAM_POS_PIN_SEARCH         = new String[] {"County", "countyname", "btransact", "searchtype", "township", "govunit", "cityr", "idcode", "pinnum", "matchtype", "appliccode", "instrumenttype", "instrumenttypecode", "DateFrom", "DateTo", "dayhist", "list", "sealcode", "Contents", "Title", "Submit222" };
	private static final String [] PARAM_POS_CONDO_SEARCH       = new String[] {"Contents", "Title", "County", "countyname", "btransact", "searchtype", "township", "govunit", "cityr", "idcode", "Descript1", "book", "page", "code", "phase", "block", "lowlot", "matchtype", "appliccode", "instrumenttype", "instrumenttypecode", "DateFrom", "DateTo", "dayhist", "list", "Submit222" };
	private static final String [] PARAM_POS_SUBDIV_SEARCH      = new String[] {"County", "countyname", "btransact", "searchtype", "township", "govunit", "cityr", "idcode", "Descript1", "book", "page", "code", "phase", "block", "lowlot", "matchtype", "appliccode", "instrumenttype", "instrumenttypecode", "DateFrom", "DateTo", "dayhist", "list", "Contents", "Title", "Submit222" };
	private static final String [] PARAM_POS_ADDR_SEARCH        = new String[] {"County",  "countyname",  "btransact",  "searchtype",  "township",  "govunit",  "cityr",  "idcode",  "addnum",  "adddir",  "addline1",  "addline2",  "city",  "state",  "zipcode",  "matchtype",  "appliccode",  "instrumenttype",  "instrumenttypecode",  "DateFrom",  "DateTo",  "dayhist",  "list",  "sealcode",  "Contents",  "Title",  "Submit222" };
	
	// search parameters order for each type of next link
	private static final String [] PARAM_POS_DOC_NO_SEARCH_NEXT = new String[] {"County", "countyname", "btransact", "searchtype", "township",  "govunit",  "cityr",  "idcode",  "instnum",  "year",  "docsplt",  "matchtype",  "appliccode",  "instrumenttype",  "dayhist",  "list",  "sealcode",  "searchcode",  "passback",  "noofrecs",  "crtstamp",  "pgmname",  "rwf",  "oparm1",  "oparm2",  "oparm3",  "next",  "Contents",  "Title"};
	private static final String [] PARAM_POS_NAME_SEARCH_NEXT   = new String[] {"County", "countyname", "btransact", "searchtype", "township", "govunit", "cityr", "idcode", "lastname", "firstname", "suffix", "corpname", "series", "appliccode", "instrumenttype", "DateFrom", "DateTo", "sorttype", "dayhist", "list", "sealcode", "instnum", "searchcode", "lastdate", "noofrecs", "recordcnt", "pgmname", "rwf", "oparm1", "oparm2", "oparm3", "next", "Contents", "Title" };
	private static final String [] PARAM_POS_ASSOC_SEARCH_NEXT  = new String[] {"County", "countyname", "btransact", "searchtype", "township",  "govunit",  "cityr",  "idcode",  "book",  "page",  "matchtype",  "instrumenttype",  "DateFrom",  "DateTo",  "dayhist",  "list",  "sealcode",  "instnum",  "docsplt",  "searchcode",  "lastdate",  "passback",  "noofrecs",  "pgmname",  "rwf",  "oparm1",  "oparm2",  "oparm3",  "next",  "Contents",  "Title" };
	private static final String [] PARAM_POS_BOOK_PAGE_NEXT     = new String[] {"County", "countyname", "btransact", "searchtype", "township",  "govunit",  "cityr",  "idcode",  "book",  "page",  "matchtype",  "appliccode",  "instrumenttype",  "DateFrom",  "DateTo",  "dayhist",  "list",  "sealcode",  "instnum",  "docsplt",  "searchcode",  "lastdate",  "passback",  "noofrecs",  "pgmname",  "rwf",  "oparm1",  "oparm2",  "oparm3",  "next",  "Contents",  "Title"};
	private static final String [] PARAM_POS_PIN_SEARCH_NEXT    = new String[] {"County", "countyname", "btransact", "searchtype", "township", "govunit", "cityr", "idcode", "pinnum", "matchtype", "instrumenttype", "DateFrom", "DateTo", "dayhist", "list", "sealcode", "instnum", "docsplt", "searchcode", "recordnum", "lastdate", "noofrecs", "pgmname", "rwf", "oparm1", "oparm2", "oparm3", "next", "Contents", "Title"};
	private static final String [] PARAM_POS_CONDO_SEARCH_NEXT  = new String[] {"County", "countyname", "btransact", "searchtype", "township",  "govunit",  "cityr",  "idcode",  "Descript1",  "volume",  "book",  "page",  "code",  "phase",  "block",  "lowlot",  "matchtype",  "instrumenttype",  "DateFrom",  "DateTo",  "dayhist",  "list",  "sealcode",  "instnum",  "docsplt",  "searchcode",  "lastdate",  "passback",  "year",  "returntype",  "recordnum",  "noofrecs",  "pgmname",  "rwf",  "oparm1",  "oparm2",  "oparm3",  "next",  "Contents",  "Title" };
	private static final String [] PARAM_POS_SUBDIV_SEARCH_NEXT = new String[] {"County", "countyname", "btransact", "searchtype", "township",  "govunit",  "cityr",  "idcode",  "Descript1",  "book",  "page",  "code",  "phase",  "block",  "lowlot",  "matchtype",  "instrumenttype",  "DateFrom",  "DateTo",  "dayhist",  "list",  "sealcode",  "instnum",  "docsplt",  "searchcode",  "lastdate",  "passback",  "year",  "returntype",  "recordnum",  "noofrecs",  "pgmname",  "rwf",  "oparm1",  "oparm2",  "oparm3",  "next",  "Contents",  "Title" };
	private static final String [] PARAM_POS_ADDR_SEARCH_NEXT   = new String[] {"County", "countyname", "btransact", "searchtype", "township",  "govunit",  "cityr",  "idcode",  "addnum",  "adddir",  "addline1",  "addline2",  "city",  "state",  "zip",  "matchtype",  "instrumenttype",  "DateFrom",  "DateTo",  "dayhist",  "list",  "sealcode",  "instnum",  "docsplt",  "searchcode",  "lastdate",  "noofrecs",  "pgmname",  "rwf",  "oparm1",  "oparm2",  "oparm3",  "next",  "Contents",  "Title" };
	
	@Override
	public LoginResponse onLogin() {

		setAttribute("loggingIn", Boolean.TRUE);

		// get the homepage
		HTTPRequest req = new HTTPRequest("http://www2.landaccess.com/cgibin/homepage?County=" + countyCode);
		HTTPResponse res = process(req);
		String response = res.getResponseAsString();

		// obtain idcode
		Matcher idMatcher = idPattern.matcher(response);
		if (idMatcher.find()) {
			setAttribute("idcode", idMatcher.group(1));
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Couldn't find required attribute");
		}

		// login
		req = new HTTPRequest("http://www2.landaccess.com/" + searchPath + "/CGIBIN/publogin?County=" + countyCode + "&countyname=" + countyName + "&menu=&userid=&password=&pin=&idcode=" + getAttribute("idcode"));
		res = process(req);
		response = res.getResponseAsString();

		// get btransact
		Matcher btMatcher = btPattern.matcher(response);
		if (btMatcher.find()) {
			setAttribute("btransact", btMatcher.group(1));
		} else {
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Couldn't find required attribute");
		}

		// get the name search page
		req = new HTTPRequest("http://www2.landaccess.com/" + searchPath + "/CGIBIN/IGGSNS");
		req.setMethod(HTTPRequest.POST);
		req.setPostParameter("County", countyCode);
		req.setPostParameter("sealcode", "Y");
		req.setPostParameter("countyname", countyName);
		req.setPostParameter("btransact", (String)getAttribute("btransact"));
		req.setPostParameter("township", "");
		req.setPostParameter("govunit", "");
		req.setPostParameter("cityr", "");
		req.setPostParameter("idcode", (String)getAttribute("idcode"));
		res = process(req);
		response = res.getResponseAsString();

		setAttribute("loggingIn", Boolean.FALSE);
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {

		// set authorization string
		String auth = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), instName, "user") + ":" + SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), instName, "password");
		req.setHeader("Authorization", "Basic " + (new Base64Encoder(auth)).processString());

		// modify the idcode from the (images) links
		String url = req.getURL();
		url = url.replaceFirst("&isSubResult=true", "");
		url = url.replaceFirst("&crossRefSource=[^&]+", "");
		url = url.replaceFirst("dummy=[^&]*&", "");
		url = url.replaceFirst("&idcode=([A-Za-z0-9]+)", "&idcode=" + (String)getAttribute("idcode"));
		req.modifyURL(url);

		// set idcode and btransact
		if (getAttribute("loggingIn") != Boolean.TRUE) {
			// set idcode
			if (req.getPostFirstParameter("idcode") != null) {
				req.removePostParameters("idcode");
				req.setPostParameter("idcode", (String)getAttribute("idcode"));
			}
			// set btransact
			if (req.getPostFirstParameter("btransact") != null) {
				req.removePostParameters("btransact");
				req.setPostParameter("btransact", (String)getAttribute("btransact"));
			}
		}

		// determine if we have to reorder the parameters
		String[] paramPos = null;
		String searchType = req.getPostFirstParameter("searchtype");
		if (searchType != null) {
			searchType = searchType.replaceAll("\\+", " ");
		}

		// figure out the correct parrameter order
		if (req.getURL().contains("IGGSNS")) {
			// first page after login
			paramPos = PARAM_POS_IGGSNS;
		} else if(searchType != null){
			if(req.getPostFirstParameter("initSearch") != null){
				// search query
				req.removePostParameters("initSearch");
				if (searchType.equals("Document Number")) {paramPos = PARAM_POS_DOC_NO_SEARCH;} 
				else if (searchType.equals("Name")) { paramPos = PARAM_POS_NAME_SEARCH; }
				else if (searchType.indexOf("Associated Book/Page") >= 0) {paramPos = PARAM_POS_ASSOC_SEARCH;} 
				else if (searchType.indexOf("Book/Page") >= 0) { paramPos = PARAM_POS_BOOK_PAGE_SEARCH; }
				else if (searchType.indexOf("Pin Number") >= 0) { paramPos = PARAM_POS_PIN_SEARCH; }				
				else if (searchType.indexOf("Condominium") >= 0) {paramPos = PARAM_POS_CONDO_SEARCH;} 
				else if (searchType.indexOf("Subdivision") >= 0) { paramPos = PARAM_POS_SUBDIV_SEARCH; }
				else if (searchType.equals("Address")){ paramPos = PARAM_POS_ADDR_SEARCH;}
			} else {
				// next 
				if (searchType.equals("Document Number")) {paramPos = PARAM_POS_DOC_NO_SEARCH_NEXT;} 
				else if (searchType.equals("Name")) { paramPos = PARAM_POS_NAME_SEARCH_NEXT; }
				else if (searchType.indexOf("Associated Book/Page") >= 0) {paramPos = PARAM_POS_ASSOC_SEARCH_NEXT;} 
				else if (searchType.indexOf("Book/Page") >= 0) { paramPos = PARAM_POS_BOOK_PAGE_NEXT; }
				else if (searchType.indexOf("Pin Number") >= 0) { paramPos = PARAM_POS_PIN_SEARCH_NEXT; }				
				else if (searchType.indexOf("Condominium") >= 0) {paramPos = PARAM_POS_CONDO_SEARCH_NEXT;} 
				else if (searchType.indexOf("Subdivision") >= 0) { paramPos = PARAM_POS_SUBDIV_SEARCH_NEXT; }
				else if (searchType.equals("Address")){paramPos = PARAM_POS_ADDR_SEARCH_NEXT;}
			}
		}

		// reorder parameters
		if (paramPos != null) {
			Vector<String> paramPosVector = new Vector<String>(Arrays.asList(paramPos));
			req.setPostParameterOrder(paramPosVector);
		}

		// set referer
		if (searchType != null) {

			if (searchType.equals("Document Number")) { req.setHeader("Referer", "http://www2.landaccess.com/" + searchPath + "/CGIBIN/IGGSDS");} 
			else if (searchType.equals("Name")) { req.setHeader("Referer", "http://www2.landaccess.com/" + searchPath + "/CGIBIN/IGGSNS");} 
			else if (searchType.equals("Book/Page")) {req.setHeader("Referer", "http://www2.landaccess.com/" + searchPath + "/CGIBIN/IGGSBPS");} 
			else if (searchType.equals("Pin Number")) {req.setHeader("Referer", "http://www2.landaccess.com/" + searchPath + "/CGIBIN/IGGSPS");}
			else if (searchType.equals("Associated Book/Page")) {req.setHeader("Referer", "http://www2.landaccess.com/" + searchPath + "/CGIBIN/IGGSABPS");} 
			else if (searchType.equals("Condominium")) {req.setHeader("Referer", "http://www2.landaccess.com/" + searchPath + "/CGIBIN/IGGSCNS");} 
			else if (searchType.equals("Subdivision")) {req.setHeader("Referer", "http://www2.landaccess.com/" + searchPath + "/CGIBIN/IGGSSS");}
			else if (searchType.equals("Address")){req.setHeader("Referer", "http://www2.landaccess.com/" + searchPath + "/CGIBIN/IGGSAS");}
		}
				
	}

}
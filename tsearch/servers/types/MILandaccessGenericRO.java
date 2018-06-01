package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_ACTION_TEXT;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.HTMLObject;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.ExpenseTracking;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

public abstract class MILandaccessGenericRO extends TSServer implements TSServerROLikeI{

	private static final long serialVersionUID = -2048546969390386112L;

	protected static final Category logger = Logger.getLogger(MILandaccessGenericRO.class);

	
	private static final int ID_BOOK_PAGE2 = 102;
	private static final int ID_CONDOMINIUM = 103;

	private static final Pattern imageLinkPattern = Pattern.compile("(?i)<a .*?Popupr1\\('IGGSIMAGE\\?(.*?)'.*?>");
	private static final Pattern searchTypePattern = Pattern.compile("(?i)<i>(.*?)</i>");
	private static final Pattern patImageLink = Pattern.compile("Link=(/[^/]*/CGIBIN/IGGSIMAGE\\?.*)");
	private static final Pattern patInstNo = Pattern.compile("&instno=([^&]+)&");

	protected String countyName = "";
	protected String searchPath = "";
	protected String countyCode = "";
	protected boolean addressSupported = false;
	
	protected String docTypeSelect = "";
	protected String seriesSelect = "";

	protected String appcodeSelectName = "";
	protected String appcodeSelectDoc = "";
	protected String appcodeSelectLiber = "";
	protected String appcodeSelectPin = "";
	protected String appcodeSelectSubd = "";
	protected String appcodeSelectCond = "";
	protected String appcodeSelectAssoc = "";
	protected String appcodeSelectAddr = "";

	protected static final String matchTypeSelect = "<select name=\"matchtype\">" + "<option value='M' selected>Match</option>" + "<option value='S'>Start From</option>" + "</select>";

	public int subdivKey = 0;
	public int condoKey = 0;

	private boolean downloadingForSave;

	protected abstract void setupConstants();
	protected abstract String getSubdivCondoCode(String subdivCondoFullName, int type);
	
	public MILandaccessGenericRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
		setupConstants();
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo(){
		
		setupConstants();
		
        TSServerInfo msiServerInfoDefault = null;
        TSServerInfoModule simTmp = null;                
        msiServerInfoDefault = new TSServerInfo( addressSupported?8:7 );
        
        msiServerInfoDefault.setServerAddress( "www2.landaccess.com" );
        msiServerInfoDefault.setServerLink( "http://www2.landaccess.com/cgibin/homepage?County=" + countyCode );
        msiServerInfoDefault.setServerIP( "www2.landaccess.com" );
        
        // build start date and end date formatted strings 
        String startDateStr = "01/01/1986", endDateStr = "";
        try{
        	SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
            SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
            Date start = sdf.parse(sa.getAtribute(SearchAttributes.FROMDATE));	            
            Date end = sdf.parse(sa.getAtribute(SearchAttributes.TODATE));	            
            sdf.applyPattern("MM/dd/yyyy");	            
            startDateStr = sdf.format(start);
            endDateStr = sdf.format(end);	            
        }catch (ParseException e){}


        // name search
        {
            simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.NAME_MODULE_IDX, 26);
        	simTmp.setName("Name");
        	simTmp.setDestinationPage("/" + searchPath + "/CGIBIN/IGGSRESP");
        	simTmp.setRequestMethod(TSConnectionURL.idPOST);
        	simTmp.setParserID(ID_SEARCH_BY_NAME);
        	
            try
            {
                PageZone searchByName = new PageZone("searchByName", "Name Search", HTMLObject.ORIENTATION_HORIZONTAL, null, 820, new Integer(250),HTMLObject.PIXELS , true);
                searchByName.setBorder(true);

                HTMLControl 
                lastName      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  25, simTmp.getFunction(0), "lastname",       "Last",           null, searchId),
                firstName     = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  1, 1,  25, simTmp.getFunction(1), "firstname",      "First",          null, searchId),
                suffix        = new HTMLControl(HTML_TEXT_FIELD, 3, 3,  1, 1,  10, simTmp.getFunction(2), "suffix",         "Suffix",         null, searchId),
                
                corporateName = new HTMLControl(HTML_TEXT_FIELD, 1, 3,  2, 2,  43, simTmp.getFunction(3), "corpname",       "Corporate Name", null, searchId),
                
                seriesCode    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  3, 3,  35, simTmp.getFunction(4), "series",         "Series Code",    " ",  searchId),                        
                appCode       = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  3, 3,  35, simTmp.getFunction(5), "appliccode",     "Appl. Code",     "O",  searchId),
                
                instrumentType= new HTMLControl(HTML_TEXT_FIELD, 1, 3,  4, 4,   1, simTmp.getFunction(6), "instrumenttype", "Document Type",  " ",  searchId),
                itypecode     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(7), "instrumenttypecode","Type Code",   null, searchId),
                
                dateFrom      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  5, 5,  10, simTmp.getFunction(8), "DateFrom",   	"Date From",      startDateStr, searchId),
                dateTo        = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  5, 5,  10, simTmp.getFunction(9), "DateTo",         "DateTo",         endDateStr, 	searchId),
                
                county        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(10), "County",         "County",         countyCode,   searchId),
                cntyNameCtrl  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(11), "countyname",     "countyname",     countyName, 	searchId),
                btransact     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(12), "btransact",      "btransact",      null, 	searchId),
                searchType    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(13), "searchtype",     "searchtype",     "Name", 	searchId),
                townShip      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(14), "township",       "township",       null, 	searchId),
                govUnit       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(15), "govunit",        "govunit",        null, 	searchId),
                cityT         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(16), "cityr",          "cityr",          null, 	searchId),
                sortType      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(17), "sorttype",       "sorttype",       "D", 		searchId),
                dayHist       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(18), "dayhist",        "dayhist",        "H", 		searchId),
                list          = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(19), "list",           "list",           "25", 	searchId),
                sealCode      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(20), "sealcode",       "sealcode",       "Y", 		searchId),
                contents      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(21), "Contents",       "Contents",       "", 		searchId),
                title         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(22), "Title",          "Title",          "Online Dates", searchId),
                submit222     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(23), "Submit222",      "Submit222",      "Searching", 	  searchId),
                initsearch    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(24), "initSearch",     "initSearch",     "initSearch",   searchId),
                idcode        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(25), "idcode",         "idcode",         null,     searchId);
                
                HTMLControl.setHiddenParamMulti(true, btransact, cityT,
						contents, cntyNameCtrl, govUnit, list, sortType,
						submit222, title, townShip, county, dayHist,
						initsearch, sealCode, searchType, itypecode, idcode);
                
                HTMLControl.setRequiredExclMulti(true, firstName, lastName, corporateName);
         
                HTMLControl.setJustifyFieldMulti(false, firstName, suffix, appCode, dateTo);
                
                searchByName.addHTMLObjectMulti(appCode, btransact, cityT,
						contents, corporateName, county, cntyNameCtrl,
						dateFrom, dateTo, dayHist, firstName, govUnit,
						initsearch, instrumentType, itypecode, lastName, list,
						sealCode, searchType, seriesCode, sortType, submit222,
						suffix, title, townShip, idcode);
                                
                simTmp.setModuleParentSiteLayout(searchByName);
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            
            simTmp.getFunction(0).setSaKey(SearchAttributes.OWNER_LNAME);
            simTmp.getFunction(1).setSaKey(SearchAttributes.OWNER_FNAME);
            simTmp.setSaObjKey(SearchAttributes.OWNER_OBJECT);
            
            simTmp.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
            simTmp.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
            simTmp.setSaObjKey(SearchAttributes.OWNER_OBJECT);
            
            simTmp.getFunction(4).setHtmlformat(seriesSelect);
            simTmp.getFunction(5).setHtmlformat(appcodeSelectName);                
            simTmp.getFunction(6).setHtmlformat(docTypeSelect);
        }
        
        // document number search
        {
        	simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.INSTR_NO_MODULE_IDX, 22);
        	simTmp.setName("Instrument Number");
        	simTmp.setDestinationPage("/" + searchPath + "/CGIBIN/IGGSRESP");
        	simTmp.setRequestMethod(TSConnectionURL.idPOST);
        	simTmp.setParserID(ID_SEARCH_BY_INSTRUMENT_NO);
			
            try
            {
                PageZone searchByDocument = new PageZone("searchByDocument", "Document Search", HTMLObject.ORIENTATION_HORIZONTAL, null, 820, new Integer(250),HTMLObject.PIXELS , true);
                searchByDocument.setBorder(true);
                
                HTMLControl
                instrument    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  15, simTmp.getFunction(0), "instnum",        "Doc No",         null, searchId),
                year          = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  1, 1,   4, simTmp.getFunction(1), "year",           "Year",           null, searchId),
                docSplit      = new HTMLControl(HTML_TEXT_FIELD, 3, 3,  1, 1,   4, simTmp.getFunction(2), "docsplt",        "Split",          null, searchId),
                
                matchType     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2, 2,   1, simTmp.getFunction(3), "matchtype",      "Match Type",     "M",  searchId),                
                appCode       = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  2, 2,  35, simTmp.getFunction(4), "appliccode",     "Appl. Code",     "O",  searchId),
                
                instrumentType= new HTMLControl(HTML_TEXT_FIELD, 1, 3,  3, 3,   1, simTmp.getFunction(5), "instrumenttype", "Document Type",  " ",  searchId),
                itypecode     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(6), "instrumenttypecode","Type Code",   null, searchId),
                                        
                county        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(7), "County",         "County",         countyCode,        searchId),
                cntyNameCtrl  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(8), "countyname",     "countyname",     countyName, 	     searchId),
                btransact     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(9), "btransact",      "btransact",      null, 	         searchId),
                searchType    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(10),"searchtype",     "searchtype",     "Document Number", searchId),
                townShip      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(11),"township",       "township",       null, 	searchId),
                govUnit       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(12),"govunit",        "govunit",        null, 	searchId),
                cityT         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(13),"cityr",          "cityr",          null, 	searchId),
                dayHist       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(14),"dayhist",        "dayhist",        "H", 		searchId),
                list          = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(15),"list",           "list",           "100", 	searchId),
                sealCode      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(16),"sealcode",       "sealcode",       "Y", 		searchId),
                contents      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(17),"Contents",       "Contents",       "", 		searchId),
                title         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(18),"Title",          "Title",          "Online Dates", searchId),
                submit222     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(19),"Submit222",      "Submit222",      "Searching", 	  searchId),
                initsearch    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(20),"initSearch",     "initSearch",     "initSearch",   searchId),
                idcode        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,   1, simTmp.getFunction(21),"idcode",         "idcode",         null,     searchId);
                
                HTMLControl.setHiddenParamMulti(true, btransact, cityT, contents, cntyNameCtrl, govUnit, list,
					submit222, title, townShip, county, dayHist, initsearch, sealCode, searchType, itypecode,
					idcode);

                HTMLControl.setRequiredExclMulti(true, instrument, year);
                
                HTMLControl.setJustifyFieldMulti(false, year, docSplit, appCode);
                
                searchByDocument.addHTMLObjectMulti(instrument, year, docSplit, matchType, appCode, instrumentType,
					itypecode, county, cntyNameCtrl, btransact, searchType, townShip, govUnit, cityT, dayHist,
					list, sealCode, contents, title, submit222, initsearch, idcode);                        

                simTmp.setModuleParentSiteLayout( searchByDocument );
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }            
            
            simTmp.getFunction(0).setSaKey(SearchAttributes.LD_INSTRNO);
            
            simTmp.getFunction(3).setHtmlformat(matchTypeSelect);                    
            simTmp.getFunction(4).setHtmlformat(appcodeSelectDoc);                    
            simTmp.getFunction(5).setHtmlformat(docTypeSelect);

        }
            
        // liber/page search
        {   
            simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, 23);
        	simTmp.setName("Book And Page");
        	simTmp.setDestinationPage("/" + searchPath + "/CGIBIN/IGGSRESP");
        	simTmp.setRequestMethod(TSConnectionURL.idPOST);
        	simTmp.setParserID(ID_SEARCH_BY_BOOK_AND_PAGE);

            try
            {
                PageZone searchByLiberPage = new PageZone("searchByLiberPage", "Liber/Page Search", HTMLObject.ORIENTATION_HORIZONTAL, null, 820, new Integer(250),HTMLObject.PIXELS , true);
                searchByLiberPage.setBorder(true);
                
                HTMLControl 
                
                book         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 12, simTmp.getFunction(0),   "book",          "Liber",     null, searchId),
                page         = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  1, 1, 13, simTmp.getFunction(1),   "page",          "Page",      null, searchId),
                
                matchType     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2, 2,  1, simTmp.getFunction(2),  "matchtype",     "Match Type",     "M",  searchId), 
                appCode       = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  2, 2, 35, simTmp.getFunction(3),  "appliccode",    "Appl. Code",     "O",  searchId),
                
                instrumentType= new HTMLControl(HTML_TEXT_FIELD, 1, 2,  3, 3,  1, simTmp.getFunction(4),  "instrumenttype",    "Document Type",  " ",  searchId),
                itypecode     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(5),  "instrumenttypecode","Type Code",      null, searchId),
                
                dateFrom      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  4, 4, 10, simTmp.getFunction(6),  "DateFrom",   	"Date From",      startDateStr, searchId),
                dateTo        = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  4, 4, 10, simTmp.getFunction(7),  "DateTo",         "DateTo",         endDateStr, 	searchId),
                
                county        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(8),  "County",         "County",         countyCode,   searchId),
                cntyNameCtrl  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(9),  "countyname",     "countyname",     countyName, 	searchId),
                btransact     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(10),  "btransact",      "btransact",      null, 	    searchId),
                searchType    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(11), "searchtype",     "searchtype",     "Book/Page", 	searchId),
                townShip      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(12), "township",       "township",       null, 	searchId),
                govUnit       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(13), "govunit",        "govunit",        null, 	searchId),
                cityT         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(14), "cityr",          "cityr",          null, 	searchId),
                dayHist       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(15), "dayhist",        "dayhist",        "H", 		searchId),
                list          = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(16), "list",           "list",           "100", 	searchId),
                sealCode      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(17), "sealcode",       "sealcode",       "Y", 		searchId),
                contents      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(18), "Contents",       "Contents",       "", 		searchId),
                title         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(19), "Title",          "Title",          "Online Dates", searchId),
                submit222     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(20), "Submit222",      "Submit222",      "Searching", 	  searchId),
                initsearch    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(21), "initSearch",     "initSearch",     "initSearch",   searchId),
                idcode        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(22), "idcode",         "idcode",         null,     searchId);
                
                HTMLControl.setHiddenParamMulti(true, btransact, cityT,
						contents, cntyNameCtrl, govUnit, list, submit222,
						title, townShip, county, dayHist, initsearch, sealCode,
						searchType, itypecode, idcode);
                
                HTMLControl.setRequiredCriticalMulti(true, book, page);
         
                HTMLControl.setJustifyFieldMulti(false, page, appCode, dateTo);
                
                searchByLiberPage.addHTMLObjectMulti(appCode, btransact, cityT,
						contents, county, cntyNameCtrl, dateFrom, dateTo,
						dayHist, matchType, govUnit, initsearch,
						instrumentType, itypecode, book, page, list, sealCode,
						searchType, submit222, title, townShip, idcode);
                                           
                simTmp.setModuleParentSiteLayout(searchByLiberPage);
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            
            simTmp.getFunction(0).setSaKey(SearchAttributes.LD_BOOKNO);
            simTmp.getFunction(1).setSaKey(SearchAttributes.LD_PAGENO);
            
            simTmp.getFunction(2).setHtmlformat(matchTypeSelect);
			simTmp.getFunction(3).setHtmlformat(appcodeSelectLiber);					
			simTmp.getFunction(4).setHtmlformat(docTypeSelect);
			
        }
            
        // pin number search
        {            
            simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.PARCEL_ID_MODULE_IDX, 22);
        	simTmp.setName("Parcel Number");
        	simTmp.setDestinationPage("/" + searchPath + "/CGIBIN/IGGSRESP");
        	simTmp.setRequestMethod(TSConnectionURL.idPOST);
        	simTmp.setParserID(ID_SEARCH_BY_PARCEL);
        	            
            try
            {
                PageZone searchByPID = new PageZone("searchByPID", "Pin Number Search", HTMLObject.ORIENTATION_HORIZONTAL, null, 820, new Integer(250),HTMLObject.PIXELS , true);
                searchByPID.setBorder(true);
     
                HTMLControl 
                
                pinNo         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 25, simTmp.getFunction(0),  "pinnum",         "Pin Number",     null, searchId),
                
                matchType     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2, 2,  1, simTmp.getFunction(1),  "matchtype",      "Match Type",     "M",  searchId), 
                appCode       = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  2, 2, 35, simTmp.getFunction(2),  "appliccode",     "Appl. Code",     "O",  searchId),
                
                instrumentType= new HTMLControl(HTML_TEXT_FIELD, 1, 2,  3, 3,  1, simTmp.getFunction(3),  "instrumenttype", "Document Type",  " ",  searchId),
                itypecode     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(4),  "instrumenttypecode","Type Code",   null, searchId),
                
                dateFrom      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  4, 4, 10, simTmp.getFunction(5),  "DateFrom",   	"Date From",      startDateStr, searchId),
                dateTo        = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  4, 4, 10, simTmp.getFunction(6),  "DateTo",         "DateTo",         endDateStr, 	searchId),
                
                county        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(7),  "County",         "County",         countyCode,   searchId),
                cntyNameCtrl  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(8),  "countyname",     "countyname",     countyName, 	searchId),
                btransact     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(9),  "btransact",      "btransact",      null, 	    searchId),
                searchType    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(10), "searchtype",     "searchtype",     "Pin Number", 	searchId),
                townShip      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(11), "township",       "township",       null, 	searchId),
                govUnit       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(12), "govunit",        "govunit",        null, 	searchId),
                cityT         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(13), "cityr",          "cityr",          null, 	searchId),
                dayHist       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(14), "dayhist",        "dayhist",        "H", 		searchId),
                list          = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(15), "list",           "list",           "100", 	searchId),
                sealCode      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(16), "sealcode",       "sealcode",       "Y", 		searchId),
                contents      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(17), "Contents",       "Contents",       "", 		searchId),
                title         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(18), "Title",          "Title",          "Online Dates", searchId),
                submit222     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(19), "Submit222",      "Submit222",      "Searching", 	  searchId),
                initsearch    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(20), "initSearch",     "initSearch",     "initSearch",   searchId),
                idcode        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(21), "idcode",         "idcode",         null,     searchId);
                
                HTMLControl.setHiddenParamMulti(true, btransact, cityT,
						contents, cntyNameCtrl, govUnit, list, submit222,
						title, townShip, county, dayHist, initsearch, sealCode,
						searchType, itypecode, idcode);
                
                HTMLControl.setRequiredCriticalMulti(true, pinNo);
         
                HTMLControl.setJustifyFieldMulti(false, appCode, dateTo);
                
                searchByPID.addHTMLObjectMulti(appCode, btransact, cityT,
						contents, county, cntyNameCtrl, dateFrom, dateTo,
						dayHist, matchType, govUnit, initsearch,
						instrumentType, itypecode, pinNo, list, sealCode,
						searchType, submit222, title, townShip, idcode);
                
                
                simTmp.setModuleParentSiteLayout(searchByPID);
                
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            
            simTmp.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
            
            simTmp.getFunction(1).setHtmlformat(matchTypeSelect);
			simTmp.getFunction(2).setHtmlformat(appcodeSelectPin);				
			simTmp.getFunction(3).setHtmlformat(docTypeSelect);
            
        }
            
        // assoc liber/page search
        {
        	
            simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.GENERIC_MODULE_IDX, 23);
        	simTmp.setName("Book And Page");
        	simTmp.setDestinationPage("/" + searchPath + "/CGIBIN/IGGSRESP");
        	simTmp.setRequestMethod(TSConnectionURL.idPOST);
        	simTmp.setParserID(ID_BOOK_PAGE2);
            
            try
            {
                PageZone searchByAssocLiberPage = new PageZone("searchByAssocLiberPage", "Associated Liber/Page Search", HTMLObject.ORIENTATION_HORIZONTAL, null, 820, new Integer(250),HTMLObject.PIXELS , true);
                searchByAssocLiberPage.setBorder(true);
                
                HTMLControl 
                
                book         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 12, simTmp.getFunction(0),   "book",          "Liber",     null, searchId),
                page         = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  1, 1, 13, simTmp.getFunction(1),   "page",          "Page",      null, searchId),
                
                matchType     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2, 2,  1, simTmp.getFunction(2),  "matchtype",     "Match Type",     "M",  searchId), 
                appCode       = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  2, 2, 35, simTmp.getFunction(3),  "appliccode",    "Appl. Code",     "O",  searchId),
                
                instrumentType= new HTMLControl(HTML_TEXT_FIELD, 1, 2,  3, 3,  1, simTmp.getFunction(4),  "instrumenttype",    "Document Type",  " ",  searchId),
                itypecode     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(5),  "instrumenttypecode","Type Code",      null, searchId),
                
                dateFrom      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  4, 4, 10, simTmp.getFunction(6),  "DateFrom",   	"Date From",      startDateStr, searchId),
                dateTo        = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  4, 4, 10, simTmp.getFunction(7),  "DateTo",         "DateTo",         endDateStr, 	searchId),
                
                county        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(8),  "County",         "County",         countyCode,   searchId),
                cntyNameCtrl  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(9),  "countyname",     "countyname",     countyName, 	searchId),
                btransact     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(10), "btransact",      "btransact",      null, 	    searchId),
                searchType    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(11), "searchtype",     "searchtype",     "Associated Book/Page", 	searchId),
                townShip      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(12), "township",       "township",       null, 	searchId),
                govUnit       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(13), "govunit",        "govunit",        null, 	searchId),
                cityT         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(14), "cityr",          "cityr",          null, 	searchId),
                dayHist       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(15), "dayhist",        "dayhist",        "H", 		searchId),
                list          = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(16), "list",           "list",           "100", 	searchId),
                sealCode      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(17), "sealcode",       "sealcode",       "Y", 		searchId),
                contents      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(18), "Contents",       "Contents",       "", 		searchId),
                title         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(19), "Title",          "Title",          "Online Dates", searchId),
                submit222     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(20), "Submit222",      "Submit222",      "Searching", 	  searchId),
                initsearch    = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(21), "initSearch",     "initSearch",     "initSearch",   searchId),
                idcode        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(22), "idcode",         "idcode",         null,     searchId);
                
                HTMLControl.setHiddenParamMulti(true, btransact, cityT,
						contents, cntyNameCtrl, govUnit, list, submit222,
						title, townShip, county, dayHist, initsearch, sealCode,
						searchType, itypecode, idcode);
                
                HTMLControl.setRequiredCriticalMulti(true, book, page);
         
                HTMLControl.setJustifyFieldMulti(false, page, appCode, dateTo);
                
                searchByAssocLiberPage.addHTMLObjectMulti(appCode, btransact,
						cityT, contents, county, cntyNameCtrl, dateFrom,
						dateTo, dayHist, matchType, govUnit, initsearch,
						instrumentType, itypecode, book, page, list, sealCode,
						searchType, submit222, title, townShip, idcode);
                                           
                simTmp.setModuleParentSiteLayout(searchByAssocLiberPage);
                
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            
            simTmp.getFunction(2).setHtmlformat(matchTypeSelect);
			simTmp.getFunction(3).setHtmlformat(appcodeSelectAssoc);			
			simTmp.getFunction(4).setHtmlformat(docTypeSelect);
        }
            
        {// subdivision search        	
            simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.SUBDIVISION_MODULE_IDX, 27);
        	simTmp.setName("Subdivision Name");
        	simTmp.setDestinationPage("/" + searchPath + "/CGIBIN/IGGSRESP");
        	simTmp.setRequestMethod(TSConnectionURL.idPOST);
        	simTmp.setParserID(ID_SEARCH_BY_SUBDIVISION_NAME);
        	
        	PageZone searchBySubdivision = new PageZone("searchBySubdivision", "Subdivision Search", HTMLObject.ORIENTATION_HORIZONTAL, null, 820, new Integer(250),HTMLObject.PIXELS , true);
        	searchBySubdivision.setBorder(true);
            simTmp.setModuleParentSiteLayout( searchBySubdivision );
            try{
            	
            	HTMLControl
            	subdName       = new HTMLControl(HTML_ACTION_TEXT,1, 1,  1, 1, 25, simTmp.getFunction(25), "Descript1",       "Subdivision Name", null,        searchId),
            	
            	code           = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2, 2, 25, simTmp.getFunction(0),  "code",            "Code",             null,        searchId),
            	phase          = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  2, 2, 13, simTmp.getFunction(1),  "phase",           "Phase",            null,        searchId),
            	
            	block          = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  3, 3, 12, simTmp.getFunction(2),  "block",           "Block",            null,        searchId),
            	lowlot         = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  3, 3, 13, simTmp.getFunction(3),  "lowlot",          "Low Lot",          null,        searchId),
            	
            	matchType      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  4, 4,  1, simTmp.getFunction(4),  "matchtype",         "Match Type",     "M",         searchId),
            	itypecode      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(5),  "instrumenttypecode","Type Code",      null,        searchId),            	
            	appCode        = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  4, 4,  1, simTmp.getFunction(6),  "appliccode",        "Appl. Code",     "O",         searchId),
            	
            	instrumentType = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  5, 5,  1, simTmp.getFunction(7),  "instrumenttype",  "Document Type",    " ",          searchId),
            	
            	dateFrom       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  6, 6, 10, simTmp.getFunction(8),  "DateFrom",        "Date From",       startDateStr, searchId),
            	dateTo         = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  6, 6, 10, simTmp.getFunction(9),  "DateTo",          "Date To",         endDateStr,   searchId),
            	
            	dayHist        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(10),  "dayhist",        "dayhist",         "H",          searchId),
            	
            	county         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(11),  "County",         "County",         countyCode,    searchId),
            	cntyNameCtrl   = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(12),  "countyname",     "countyname",     countyName,    searchId),
            	btransact      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(13),  "btransact",      "btransact",      "",            searchId),
            	searchType     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(14),  "searchtype",     "searchtype",     "Subdivision", searchId),
            	townShip       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(15),  "township",       "township",       null,          searchId),
            	govUnit        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(16),  "govunit",        "govunit",        null,          searchId),
            	cityT          = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(17),  "cityr",          "cityr",          null,          searchId),
            	list           = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(18),  "list",           "list",           "100",         searchId),
            	contents       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(19),  "Contents",       "Contents",       "",            searchId),
            	title          = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(20),  "Title",          "Title",          "Online Dates",searchId),
            	submit222      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(21),  "Submit222",      "Submit222",      "Searching",   searchId),
            	initsearch     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(22),  "initSearch",     "initSearch",     "initSearch",  searchId),
            	book           = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(23),  "book",           "Book",           null,          searchId),
            	page           = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(24),  "page",           "Page",           null,          searchId),
            	idcode         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(26),  "idcode",         "idcode",         null,          searchId); 
            	
            	String subdJSFunction =  "subdivisionJSEvent(\"param_7_0,param_7_25\",document.forms[0].elements[\"param_7_25\"].value,\""+subdivKey+"\")";
            	subdName.setJSFunction(subdJSFunction);
            	
            	HTMLControl.setHiddenParamMulti(true, btransact, cityT,
						contents, cntyNameCtrl, govUnit, list, submit222,
						title, townShip, county, dayHist, initsearch,
						searchType, itypecode, idcode, book, page);
            
                HTMLControl.setRequiredCriticalMulti(true);
                
                HTMLControl.setRequiredExclMulti(true, subdName, code, phase);
                
                HTMLControl.setJustifyFieldMulti(false, phase, lowlot, appCode, dateTo);

            	searchBySubdivision.addHTMLObjectMulti(subdName, code, phase,
						block, lowlot, matchType, itypecode, appCode,
						instrumentType, dateFrom, dateTo, dayHist, county,
						cntyNameCtrl, btransact, searchType, townShip, govUnit,
						cityT, list, contents, title, submit222, initsearch,
						book, page, idcode);
            	
            	 simTmp.setModuleParentSiteLayout(searchBySubdivision);
            }
            catch(Exception e){
            	e.printStackTrace();
            }
            
            simTmp.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
            
            simTmp.getFunction(0).setSaKey("");            
            simTmp.getFunction(1).setSaKey("");                                
            simTmp.getFunction(3).setSaKey(SearchAttributes.LD_LOTNO);
            simTmp.getFunction(25).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
            
            simTmp.getFunction(7).setHtmlformat(docTypeSelect);            
            simTmp.getFunction(6).setHtmlformat(appcodeSelectSubd);            
            simTmp.getFunction(4).setHtmlformat(matchTypeSelect);

        }
            
        {// condominium search        	
            simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.CONDOMIN_MODULE_IDX, 27);
        	simTmp.setName("Condominium Name");
        	simTmp.setDestinationPage("/" + searchPath + "/CGIBIN/IGGSRESP");
        	simTmp.setRequestMethod(TSConnectionURL.idPOST);
        	simTmp.setParserID(ID_SEARCH_BY_CONDO_NAME);
        	
        	PageZone searchByCondominium = new PageZone("searchByCondominium", "Condominium Search", HTMLObject.ORIENTATION_HORIZONTAL, null, 820, new Integer(250),HTMLObject.PIXELS , true);
        	searchByCondominium.setBorder(true);
            simTmp.setModuleParentSiteLayout( searchByCondominium );
            try{
            	HTMLControl
            	subdName       = new HTMLControl(HTML_ACTION_TEXT,1, 1,  1, 1, 25, simTmp.getFunction(25), "Descript1",       "Condominium Name", null,        searchId),
            	
            	code           = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2, 2, 25, simTmp.getFunction(0),  "code",            "Code",             null,        searchId),
            	phase          = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  2, 2, 13, simTmp.getFunction(1),  "phase",           "Phase",            null,        searchId),
            	
            	block          = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  3, 3, 12, simTmp.getFunction(2),  "block",           "Building",         null,        searchId),
            	lowlot         = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  3, 3, 13, simTmp.getFunction(3),  "lowlot",          "Low Lot",          null,        searchId),
            	
            	matchType      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  4, 4,  1, simTmp.getFunction(4),  "matchtype",         "Match Type",     "M",         searchId),
            	itypecode      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(5),  "instrumenttypecode","Type Code",      null,        searchId),            	
            	appCode        = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  4, 4,  1, simTmp.getFunction(6),  "appliccode",        "Appl. Code",     "O",         searchId),
            	
            	instrumentType = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  5, 5,  1, simTmp.getFunction(7),  "instrumenttype",  "Document Type",    " ",         searchId),
            	
            	dateFrom       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  6, 6, 10, simTmp.getFunction(8),  "DateFrom",        "Date From",       startDateStr, searchId),
            	dateTo         = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  6, 6, 10, simTmp.getFunction(9),  "DateTo",          "Date To",         endDateStr,   searchId),
            	
            	dayHist        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(10),  "dayhist",        "dayhist",         "H",          searchId),
            	
            	county         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(11),  "County",         "County",         countyCode,    searchId),
            	cntyNameCtrl   = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(12),  "countyname",     "countyname",     countyName,    searchId),
            	btransact      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(13),  "btransact",      "btransact",      "",            searchId),
            	searchType     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(14),  "searchtype",     "searchtype",     "Condominium", searchId),
            	townShip       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(15),  "township",       "township",       null,          searchId),
            	govUnit        = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(16),  "govunit",        "govunit",        null,          searchId),
            	cityT          = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(17),  "cityr",          "cityr",          null,          searchId),
            	list           = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(18),  "list",           "list",           "100",         searchId),
            	contents       = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(19),  "Contents",       "Contents",       "",            searchId),
            	title          = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(20),  "Title",          "Title",          "Online Dates",searchId),
            	submit222      = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(21),  "Submit222",      "Submit222",      "Searching",   searchId),
            	initsearch     = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(22),  "initSearch",     "initSearch",     "initSearch",  searchId),
            	book           = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(23),  "book",           "Book",           null,          searchId),
            	page           = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(24),  "page",           "Page",           null,          searchId),
            	idcode         = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1,  1, simTmp.getFunction(26),  "idcode",         "idcode",         null,          searchId); 
            	
            	String condoJSFunction =  "subdivisionJSEvent(\"param_19_0,param_19_25\",document.forms[0].elements[\"param_19_25\"].value,\""+condoKey+"\")";
            	subdName.setJSFunction(condoJSFunction);
            	
            	HTMLControl.setHiddenParamMulti(true, btransact, cityT,
						contents, cntyNameCtrl, govUnit, list, submit222,
						title, townShip, county, dayHist, initsearch,
						searchType, itypecode, idcode, book, page);
            
                HTMLControl.setRequiredCriticalMulti(true);
                
                HTMLControl.setRequiredExclMulti(true, subdName, code, phase);
                
                HTMLControl.setJustifyFieldMulti(false, phase, lowlot, appCode, dateTo);

                searchByCondominium.addHTMLObjectMulti(subdName, code, phase,
						block, lowlot, matchType, itypecode, appCode,
						instrumentType, dateFrom, dateTo, dayHist, county,
						cntyNameCtrl, btransact, searchType, townShip, govUnit,
						cityT, list, contents, title, submit222, initsearch,
						book, page, idcode);
                
                simTmp.setModuleParentSiteLayout(searchByCondominium);

            }
            catch(Exception e){
            	e.printStackTrace();
            }
            simTmp.getFunction(0).setSaKey("");            
            simTmp.getFunction(1).setSaKey("" );
            simTmp.getFunction(3).setSaKey(SearchAttributes.LD_SUBDIV_UNIT);
            simTmp.getFunction(25).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
                       
            simTmp.getFunction(4).setHtmlformat(matchTypeSelect);
            simTmp.getFunction(6).setHtmlformat(appcodeSelectCond);
            simTmp.getFunction(7).setHtmlformat(docTypeSelect);            
        }
        
        if(addressSupported){
            simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.ADDRESS_MODULE_IDX, 28);
        	simTmp.setName("Address");
        	simTmp.setDestinationPage("/" + searchPath + "/CGIBIN/IGGSRESP");
        	simTmp.setRequestMethod(TSConnectionURL.idPOST);
        	simTmp.setParserID(ID_SEARCH_BY_ADDRESS);
        	
        	PageZone searchByAddress = new PageZone("searchByAddress", "Address Search", HTMLObject.ORIENTATION_HORIZONTAL, null, 820, new Integer(250),HTMLObject.PIXELS , true);
        	searchByAddress.setBorder(true);
            simTmp.setModuleParentSiteLayout(searchByAddress);
            try{
            	HTMLControl
	        	addrnum  = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 10, simTmp.getFunction(0),  "addnum","Number",null,searchId),
	        	adddir   = new HTMLControl(HTML_TEXT_FIELD,  2, 2,  1,  1,  6, simTmp.getFunction(1),  "adddir","Direction",null,searchId),
	        	addline1 = new HTMLControl(HTML_TEXT_FIELD,  1, 3,  2,  2, 32, simTmp.getFunction(2),  "addline1","Address 1", null,searchId),
	        	addline2 = new HTMLControl(HTML_TEXT_FIELD,  1, 3,  3,  3, 32, simTmp.getFunction(3),  "addline2","Address 2","",searchId),
	        	city     = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  4,  4, 12, simTmp.getFunction(4),  "city","City",null,searchId),
	        	state    = new HTMLControl(HTML_TEXT_FIELD,  2, 2,  4,  4,  3, simTmp.getFunction(5),  "state","State","MI",searchId),
	        	zipcode  = new HTMLControl(HTML_TEXT_FIELD,  3, 3,  4,  4, 10, simTmp.getFunction(6),  "zipcode","Zip",null,searchId),
	        	mtype    = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  5,  5, 25, simTmp.getFunction(7),  "matchtype","Match Type","M",searchId),
	        	appcode  = new HTMLControl(HTML_TEXT_FIELD,  2, 2,  5,  5, 25, simTmp.getFunction(8),  "appliccode","Appl. Code","",searchId),
	        	itype    = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  6,  6, 25, simTmp.getFunction(9),  "instrumenttype","Document Type","",searchId),
	        	DateFrom = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  7,  7, 10, simTmp.getFunction(10), "DateFrom","Date From",startDateStr,searchId),
	        	DateTo   = new HTMLControl(HTML_TEXT_FIELD,  2, 2,  7,  7, 10, simTmp.getFunction(11), "DateTo","Date To",endDateStr,searchId),
	        	p12      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(12), "County","County",countyCode,searchId),
	        	p13      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(13), "countyname","countyname",countyName,searchId),
	        	p14      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(14), "btransact","btransact","",searchId),
	        	p15      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(15), "searchtype","searchtype","Address",searchId),
	        	p16      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(16), "township","township","",searchId),
	        	p17      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(17), "govunit","govunit","",searchId),
	        	p18      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(18), "cityr","cityr","",searchId),
	        	p19      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(19), "idcode","idcode","",searchId),
	        	p20      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(20), "instrumenttypecode","instrumenttypecode","",searchId),
	        	p21      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(21), "dayhist","dayhist","H",searchId),
	        	p22      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(22), "list","list","25",searchId),
	        	p23      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(23), "sealcode","sealcode","Y",searchId),
	        	p24      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(24), "Contents","Contents","",searchId),
	        	p25      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(25), "Title","Title","Online Dates",searchId),
	        	p26      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(26), "Submit222","Submit222","Searching",searchId),
	        	p27      = new HTMLControl(HTML_TEXT_FIELD,  1, 1,  1,  1, 25, simTmp.getFunction(27), "initSearch","initSearch","initSearch",searchId);
                
            	HTMLControl.setHiddenParamMulti(true, p12, p13, p14, p15, p16,
						p17, p18, p19, p20, p21, p22, p23, p24, p25, p26, p27);
            	HTMLControl.setJustifyFieldMulti(false, adddir, state, zipcode,
						appcode, DateTo);
            	searchByAddress.addHTMLObjectMulti(addrnum, adddir, addline1,
						addline2, city, state, zipcode, mtype, appcode, itype,
						DateFrom, DateTo, p12, p13, p14, p15, p16, p17, p18,
						p19, p20, p21, p22, p23, p24, p25, p26, p27);

            }catch(Exception e){
            	e.printStackTrace();
            }
            simTmp.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);            
            simTmp.getFunction(1).setSaKey(SearchAttributes.P_STREETDIRECTION_ABBREV);
            simTmp.getFunction(2).setSaKey(SearchAttributes.P_STREETNAME);
            simTmp.getFunction(4).setSaKey(SearchAttributes.P_CITY);
            simTmp.getFunction(6).setSaKey(SearchAttributes.P_ZIP);
                       
            simTmp.getFunction(7).setHtmlformat(matchTypeSelect);
            simTmp.getFunction(8).setHtmlformat(appcodeSelectAddr);
            simTmp.getFunction(9).setHtmlformat(docTypeSelect);          

        }
        
        msiServerInfoDefault.setupParameterAliases();
        setModulesForAutoSearch( msiServerInfoDefault );
        setModulesForGoBackOneLevelSearch(msiServerInfoDefault);

                    
        return msiServerInfoDefault;
    }

	/**
	 * Create Already Present filter
	 * @return
	 */
	protected FilterResponse getAlreadyPresentFilter(){
		RejectAlreadyPresentFilterResponse filter = new RejectAlreadyPresentFilterResponse(searchId);
		filter.setUseBookPage(true);
		filter.setUseInstr(false);
		filter.setUseYearInstr(true);
		filter.setThreshold(new BigDecimal("0.90"));
		return filter;
	}
	
	/**
	 * Create interval filter
	 * @return
	 */
	protected FilterResponse getIntervalFilter(){
		return BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId);
	}
	
	/**
	 * Create legal filter
	 * @return
	 */
	protected FilterResponse getLegalFilter(){
		GenericLegal filter = new GenericLegal(searchId);
		filter.disableAll();
		filter.setEnableLot(true);
		filter.setEnableBlock(true);
		filter.setEnableSection(true);
		filter.setThreshold(new BigDecimal("0.70"));
		return filter;
	}
		
	/**
	 * Create PIN filter
	 * @return
	 */
	protected FilterResponse getPinFilter(){
		PinFilterResponse filter = new PinFilterResponse(SearchAttributes.LD_PARCELNO, searchId);
		return filter;
	}	
    
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String sTmp;
		int istart, iend;
		String keyNumber = "";

		String rsResponse = Response.getResult();
		String initialResponse = rsResponse;
		String next = null, prev = null;
		String imgLink = null;

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_BOOK_AND_PAGE:
		case ID_SEARCH_BY_PARCEL:
		case ID_BOOK_PAGE2:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_CONDOMINIUM:
		case ID_SEARCH_BY_CONDO_NAME:
		case ID_SEARCH_BY_ADDRESS:
			
			String searchTypeString = "";
			Matcher searchTypeMatcher = searchTypePattern.matcher(rsResponse);
			if (searchTypeMatcher.find()) {
				searchTypeString = searchTypeMatcher.group(1);
			}

			sTmp = CreatePartialLink(TSConnectionURL.idGET);

			String nextPrevFormString = StringUtils.getTextBetweenDelimiters("<form name=form1", "</form", rsResponse);
			Vector<String> nextPrevParams = HttpUtils.getFormParamsOrdered(nextPrevFormString);
			HashMap<String, String> nextPrevParamsHash = HttpUtils.getFormParams(nextPrevFormString);

			String number = nextPrevParamsHash.get("list");
			if (number == null) {
				number = "100";
			}

			nextPrevParamsHash.put("next", "Next+" + number + "++%3E%3E");
			nextPrevParamsHash.put("oparm1", "");
			nextPrevParamsHash.put("oparm2", "");
			nextPrevParamsHash.put("oparm3", "");
			nextPrevParamsHash.put("Contents", "");

			int currentPage = -1;

			try {
				currentPage = Integer.parseInt(nextPrevParamsHash.get("noofrecs"));
			} catch (Exception e) {
				// e.printStackTrace();
			}

			if (currentPage != -1) {
				next = CreatePartialLink(TSConnectionURL.idPOST) + "/" + searchPath + "/CGIBIN/IGGSRESP";

				for (int i = 0; i < nextPrevParams.size(); i++) {
					String param = nextPrevParams.elementAt(i);

					String paramValue = nextPrevParamsHash.get(param);

					next += "&" + param + "=" + paramValue;
				}

			}

			if (currentPage - 1 >= 1) {
				prev = "javascript: history.go(-1);";
			}

			istart = rsResponse.indexOf("Results_Tbl");
			istart = rsResponse.lastIndexOf("<table", istart);
			iend = rsResponse.indexOf("</table", istart + 1);

			if (istart < 0 || iend < 0) {
				// error
				return;
			}

			// strip the information table from the result
			rsResponse = rsResponse.substring(istart, iend) + "</table>";

			// when parsing each TR from intermendiate results table, also
			// the table header is needed => save intermediate results table
			// header in parser.header
			int headerStart = rsResponse.indexOf("<td");
			int headerStop = rsResponse.indexOf("</tr>");
			if (headerStart != -1 && headerStop != -1) {
				parser.setHeader("<tr>" + rsResponse.substring(headerStart, headerStop) + "</tr>");
			}

			rsResponse = rsResponse.replaceAll("(?is)<tr[^>]*>", "<tr>");

			rsResponse = rsResponse.replaceAll("(?i)<a .*?Popupl\\('(.*?)\\?(.*?)'\\).*?>(.*?)</a>", "<input type=\"checkbox\" name=\"docLink\" value=\"" + sTmp + "/" + searchPath + "/CGIBIN/$1&$2\"><a href=\"" + sTmp + "/" + searchPath + "/CGIBIN/$1&$2\">$3</a>");

			// take out image links
			rsResponse = rsResponse.replaceAll("(?i)<a .*?IMAGESTAGE.*?>(.*?)</a>", "$1");

			if (!"".equals(searchTypeString)) {
				rsResponse = "<table><tr><td>" + searchTypeString + "</td></tr></table>" + rsResponse;
			}

			if (prev != null) {
				rsResponse += "<a onClick=\"" + prev + "\">Previous</a>&nbsp;&nbsp;&nbsp;";
			}

			if (next != null) {
				rsResponse += "<a href=\"" + next + "\">Next</a>";
			}
			
			istart = rsResponse.indexOf("</table>");
			iend = rsResponse.indexOf("</tr>", istart);
			String header = "";
			if (istart != -1 && iend != -1) {
				iend += "</tr>".length();
				header = rsResponse.substring(0, iend);
				rsResponse = rsResponse.substring(iend);
			}
			
			rsResponse = rsResponse.replaceFirst("<tr>[\n\r]+</td></tr></table>", "</td></tr></table>");
			rsResponse = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + rsResponse + "<BR>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);

			parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);

			if (istart != -1 && iend != -1 && Response.getParsedResponse().getResultsCount() != 0) {
				Response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + header);
				Response.getParsedResponse().setFooter("</table><BR>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1) + (prev != null ? "<a onClick=\"" + prev + "\">Previous</a>&nbsp;&nbsp;&nbsp;" : "") + (next != null ? "<a href=\"" + next + "\">Next</a>" : ""));
			}

			if (next != null) {
				Response.getParsedResponse().setNextLink("<a href=\"" + next + "\">Next</a>");
			}

			parser.setHeader(null);

			break;
		case ID_DETAILS:
			istart = rsResponse.indexOf("<table align=center border=1 bordercolor=\"black\" cellspacing=0 cellpadding=0>");
			iend = rsResponse.indexOf("Public Search");
			iend = rsResponse.lastIndexOf("</table>", iend);

			if (istart < 0 || iend < 0) {
				// error
				return;
			}

			// strip the information table from the result
			rsResponse = rsResponse.substring(istart, iend) + "</table>";

			rsResponse = rsResponse.replaceAll("<font.*?>", "");
			rsResponse = rsResponse.replaceAll("</font>", "");
			rsResponse = rsResponse.replaceAll("bgcolor=[^\\s>]*", "");

			// get key number from html result
			try {
				istart = rsResponse.indexOf("Instrument");
				istart = rsResponse.indexOf("<tr>", istart + 1);
				istart = rsResponse.indexOf("<td>", istart + 1);

				iend = rsResponse.indexOf("</TD>", istart + 1);

				keyNumber = rsResponse.substring(istart + 4, iend);
			} catch (Exception e) {
				e.printStackTrace();
			}

			keyNumber = StringUtils.removeLeadingZeroes(keyNumber);

			Matcher imageLinkMatcher = imageLinkPattern.matcher(rsResponse);
			if (imageLinkMatcher.find()) {
				imgLink = CreatePartialLink(TSConnectionURL.idGET) + "/" + searchPath + "/CGIBIN/IGGSIMAGE?" + imageLinkMatcher.group(1);
			}

			try {

				// isolate first part for which we remove all links and second
				// part, for which we rewrite the
				// cross-reference links to work fine
				int idx = rsResponse.indexOf("Associated-Type");
				String firstPart, secondPart;
				if (idx != -1) {
					firstPart = rsResponse.substring(0, idx);
					secondPart = rsResponse.substring(idx);
				} else {
					firstPart = rsResponse;
					secondPart = "";
				}
				firstPart = firstPart.replaceAll("(?i)<a .*?>", "");
				firstPart = firstPart.replaceAll("(?i)</a>", "");

				// rewrite cross-reference links
				String prefix = downloadingForSave ? "/cross-ref" : ""; 
				secondPart = secondPart.replaceAll("<a href=\"javascript:void\\(0\\);\" onClick=\"Popupld\\('IGGSDETAIL\\?([^']+)'[^>]+>", "<a HREF='" + prefix + CreatePartialLink(TSConnectionURL.idGET) + "/" + searchPath + "/CGIBIN/IGGSDETAIL&" + "$1'>");

				rsResponse = firstPart + secondPart;
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("AddMissingLinksError:", e);
			}

			if (imgLink != null) {
				Response.getParsedResponse().addImageLink(new ImageLinkInPage(imgLink, keyNumber + ".tiff"));
			}

			if (!downloadingForSave) {
				// not saving to TSR

				if (imgLink != null) {
					rsResponse += "<a href=\"" + imgLink + "\">View Image</a>";
				}

				String qry = Response.getQuerry();
				qry = "dummy=" + keyNumber + "&" + qry;
				Response.setQuerry(qry);
				String originalLink = sAction + "&" + qry;
				String sSave2TSDLink = CreatePartialLink(TSConnectionURL.idGET) + originalLink;

				if (FileAlreadyExist(keyNumber + ".html") ) {
					rsResponse += CreateFileAlreadyInTSD();
				} else {
					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink,viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
			} else {
				// saving
				msSaveToTSDFileName = keyNumber + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);

				// HTML correction for docs with condominium
				rsResponse = rsResponse.replaceAll("(?s)(<table width=100% cellspacing=0 cellpadding=0 border=1>.?<tr><td  colspan=6><b>Condominium Description Sequence #([2-9]|1\\d))", "</table>$1");
				rsResponse = Tidy.tidyParse(rsResponse, null);
				
				// fix back the cross-reference links
				rsResponse = rsResponse.replaceAll("<a href='/cross-ref([^']+)'>","<a HREF='$1'>");
				if (imgLink != null) {
					rsResponse += "<a href=\"" + imgLink + "\">View Image</a>";
				}
				msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();

				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
			}

			break;
		case ID_GET_LINK:
			if (sAction.indexOf("IGGSDETAIL") >= 0) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			}
			break;
		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			break;
		default:
			break;
		}
	}
	 
	@SuppressWarnings("deprecation")
	@Override
	protected String getFileNameFromLink(String link) {
		String parcelId = org.apache.commons.lang.StringUtils.getNestedString(link, "dummy=", "&");
		return parcelId + ".html";
	}
	
    public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
		p.splitResultRows(pr, htmlString, pageId, "<tr", "</table>", linkStart, action);
		
	}    
    
    /**
	 * retrieve an image using the link
	 * http://www2.landaccess.com/oakland_mi/CGIBIN/IGGSIMAGE?...
	 * 
	 * @param instNo
	 * @param link1
	 */
	private void retrieveImage(String fileName, String link1, String instNo, HTTPSiteInterface site) {

		// check if file not already downloaded
		if (FileUtils.existPath(fileName)) {
			return;
		}

		// get link 1
		link1 = link1.replace(" ", "%20");
		HTTPRequest imageRequest = new HTTPRequest(link1);
		HTTPResponse imageResponse = site.process(imageRequest);
		String htmlResponse = imageResponse.getResponseAsString();
		String link3 = "";
		if (htmlResponse.matches("(?is).*<\\s*input[^>]+value\\s*=\\s*[\\\"]YES[\\\"][^>]+window.location\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"].*"))
		{// get 2nd link
			link3 = htmlResponse.replaceFirst("(?is).*<\\s*input[^>]+value\\s*=\\s*[\\\"]YES[\\\"][^>]+window.location\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"].*", "$1");
			if(!link3.contains("www2"))
			{
				if (!link3.startsWith("/"))
				{
					link3 = "http://www2.landaccess.com/" + searchPath + "/CGIBIN/" + link3;
				}
				else
				{
					link3 = "http://www2.landaccess.com/" + searchPath + "/CGIBIN"+ link3;
				}
			}
			link3 = link3.replace(" ", "%20");
			
			HTTPRequest request = new HTTPRequest(link3);
			request.setMethod(HTTPRequest.GET);
			request.setHeader("Host", "www2.landaccess.com");
			request.setHeader("Referer", link3);
			HTTPResponse res = site.process(request);
			String resStr = res.getResponseAsString();
			// get image location
			link3 = resStr.replaceFirst("(?is).*window.location\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"].*", "$1");
		}
		else
		{// get image location
			link3 = htmlResponse.replaceFirst("(?is).*window.location\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"].*", "$1");
		}

		// create the output folder if it does not exist
		FileUtils.CreateOutputDir(fileName);

		// download the image
		link3 = "http://www2.landaccess.com" + link3;
		HTTPRequest request = new HTTPRequest(link3);
		request.setMethod(HTTPRequest.GET);
		request.setHeader("Host", "www2.landaccess.com");
		request.setHeader("Referer", imageRequest.getURL());
		HTTPResponse res = site.process(request);
		
		InputStream inputStream = res.getResponseAsStream();
		FileUtils.writeStreamToFile(inputStream, fileName);

	}    
    
    @Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {

		// check if we have a special image link and isolate the actual link
		Matcher m = patImageLink.matcher(image.getLink());
		if (!m.find()) {
			return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );
		}

		String link1 = "http://www2.landaccess.com" + m.group(1);

		// isolate instrument number
		m = patInstNo.matcher(image.getLink());
		if (!m.find()) {
			return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );
		}
		String instNo = m.group(1);
		instNo = StringUtils.removeLeadingZeroes(instNo);

		// find image file name
		// String fileName = getCrtSearchDir() + "Register" + File.separator + instNo + ".tiff";
		String fileName = image.getPath();

		// download the image if necessary
		HttpSite site = HttpManager.getSite("MI" + countyName + "RO", searchId);
		try{
			retrieveImage(fileName, link1, instNo, site);
		} finally {
			HttpManager.releaseSite(site);
		}
		if(FileUtils.existPath(fileName)){
			byte b[] = FileUtils.readBinaryFile(fileName);
			afterDownloadImage(true);
    		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
		}
		return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );
	}
     
    /**
	 * treat the case in which the user clicked on an image link, and download
	 * it only once
	 */
	@Override
	public ServerResponse GetLink(String vsRequest, boolean vbEncoded) throws ServerResponseException {

		// check if we have a special image link and isolate the actual link
		String link = vsRequest;
		Matcher m = patImageLink.matcher(link);
		if (!m.find()) {
			return super.GetLink(vsRequest, vbEncoded);
		}
		String link1 = "http://www2.landaccess.com" + m.group(1);

		// isolate instrument number
		m = patInstNo.matcher(link);
		if (!m.find()) {
			return new ServerResponse();
		}
		String instNo = m.group(1);
		instNo = StringUtils.removeLeadingZeroes(instNo);

		// find image file name
		String fileName = getCrtSearchDir() + "Register" + File.separator + instNo + ".tiff";

		// download the image if if does not exist
		HttpSite site = HttpManager.getSite("MI" + countyName + "RO", searchId);
		try{
			retrieveImage(fileName, link1, instNo, site);
		} finally {
			HttpManager.releaseSite(site);
		}		

		// write the image to the client web-browser
		boolean imageOK = writeImageToClient(fileName, "image/tiff");

		// image not retrieved
		if (!imageOK) {
			// return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);
		}
		// return solved response
		return ServerResponse.createSolvedResponse();
	}    

	/**
	 * Obtain the certification date
	 * @param stateName eg MI
	 * @param countyName eg Macomb
	 * @param countyCode eg 8025
	 * @return certification date in format MM/dd/yyyy or empty string if no certification date found
	 */
	synchronized protected static String getCertificationDate(String stateName, String countyName, String path, String countyCode) {

		// create cert link
		String link = "http://www2.landaccess.com/" + path + "/CGIBIN/ONLNDATES?County=" + countyCode + "&sealcode=Y&countyname=" + countyName;

		// obtain site name
		String siteName = stateName + countyName + "RO";

		// <Font color=red> 10/19/1995<Font color=black> - <Font color=red>02/15/2008</font>			
		String DATE = "\\s*(\\d\\d/\\d\\d/\\d\\d\\d\\d)\\s*";
		String FONT = "(?:<font[^>]*>)?";
		Pattern pat = Pattern.compile("(?i)([^><]*)" + FONT + DATE + FONT + "\\s*-\\s*" + FONT + DATE);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

		// try 3 times
		for (int i = 0; i < 3; i++) {
			
			// wait 10 seconds before trying again
			if (i != 0) {
				try {
					TimeUnit.SECONDS.sleep(10);
				} catch (InterruptedException ie) {
					throw new RuntimeException(ie);
				}
			}
			
			// get site
			HttpSite site = HttpManager.getSite(siteName, -1);			
			try {				
				String response = site.process(new HTTPRequest(link)).getResponseAsString();
				Matcher mat = pat.matcher(response);
				Date date = new Date(0);
				while (mat.find()) {
					String text = mat.group(1);
					String end = mat.group(3);
					// discard entries containing UNVERIFIED or IMAGE
					if (text.contains("UNVERIFIED") || text.contains("IMAGE")) {
						continue;
					}
					// also discard entries not containing INDEX
					if (!text.contains("INDEX")) {
						continue;
					}
					Date crtDate = sdf.parse(end);
					if (date.getTime() == 0) {
						date = crtDate;
					} else if (crtDate.getTime() < date.getTime()) {
						date = crtDate;
					}
				}
				if (date.getTime() == 0) {
					return "";
				} else {
					return sdf.format(date);
				}
			} catch (Exception e) {
				logger.error(e);
			} finally {
				// always release site
				HttpManager.releaseSite(site);
			}			
		}

		// cert date not found
		return "";
	}

	@Override
    public ServerResponse performLinkInPage(LinkInPage link) throws ServerResponseException {
    	
		// do not explore link for doc that was already saved in TSR index
    	if(link != null && link.getLink() != null && link.getLink().contains("isSubResult=true")){
    		    		
    		String year = StringUtils.extractParameter(link.getLink(), "recyear=([^&]+)");
    		String instNo = StringUtils.extractParameter(link.getLink(), "instno=([^&]+)");
    		
    		if(!StringUtils.isEmpty(year) && !StringUtils.isEmpty(instNo)){
    			String key1 = "year=" + year + ";inst=" + instNo;
    			String key2 = "year=" + year + ";inst=" + instNo.replaceAll("^0+", "");
    			if(getSearch().hasSavedInst(key1) || getSearch().hasSavedInst(key2)){
    				if(getLogAlreadyFollowed()){
    					SearchLogger.info("<div>Document with " + key1 + " already saved.</div><br/>", searchId);
    				}
    				return new ServerResponse();
    			}
    		}    		
    	}   
    	
    	return super.performLinkInPage(link);
    }
	
	@Override
    protected boolean getLogAlreadyFollowed(){
    	return false;
    }
	
}

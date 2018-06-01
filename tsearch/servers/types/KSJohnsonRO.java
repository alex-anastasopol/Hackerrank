/*
 * Created on May 20, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ro.cst.tsearch.servers.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.CookieManager;
import ro.cst.tsearch.connection.CookieMgr;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.GranteeDoctypeFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.HTMLObject;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.DavidsonSplit;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.CSTCalendar;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringEquivalents;
import ro.cst.tsearch.utils.SubdivisionMatcher;

import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;


public class KSJohnsonRO extends TSServer implements TSServerROLikeI {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;


    private final static String SEARCH_PATH = "/SimpleQuery.asp"; //path
      
    public final static String FILE_REFERER = "http://recorder.jocogov.org/Navigate.asp?AdvancedSearch.x=121&AdvancedSearch.y=5";
    public static final String LOAD_IMAGE_REFERER = "http://recorder.jocogov.org/SimpleQuery.asp";
	
	private static final Category logger = Logger.getLogger(KSJohnsonRO.class);
	
	private static final Pattern bpPattern = Pattern.compile("(?is)\\s*(<i>(?:Bkwd|Fwd)</i>\\s*[A-Z]*\\s+)([0-9]+[A-Z]?)\\s+([0-9]+[A-Z]?)\\s*");
	private static final Pattern iPattern = Pattern.compile("(?is)\\s*(<i>(?:Bkwd|Fwd)</i>\\s+[A-Z]*\\s+)([0-9A-Z]{6,})\\s*");
	private static final Pattern hrefPattern = Pattern.compile("(?is)\\s*<\\s*a\\s+href\\s*=\\s*");
	private static final Pattern certDatePattern = Pattern.compile("(?ism)The Data is Current Thru:</SPAN>(.*?)</font>");
	
    /**
     * 
     */
    public KSJohnsonRO(long searchId) {
        super(searchId);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param rsRequestSolverName
     * @param rsSitePath
     * @param rsServerID
     * @param rsPrmNameLink
     */
    public KSJohnsonRO(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
    }

    public void resetServerSession()
    {
        CookieManager.resetCookie( Integer.toString(miServerID), true );
        CookieManager.resetCookie( Integer.toString(miServerID), true );
        CookieMgr.removeCookie( Integer.toString(miServerID) );
    }
    
    public ServerResponse SearchBy(TSServerInfoModule module,
            Object sd) throws ServerResponseException {
        boolean bResetQueryParam = true;
        
        String sStartDate = null;
        String sEndDate = null;
        
        if (module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX)
        {
        	sStartDate = module.getFunction(6).getParamValue();
        	sEndDate  = module.getFunction(7).getParamValue();
        }
        else if (module.getModuleIdx() == TSServerInfo.ADV_SEARCH_MODULE_IDX)
        {
        	sStartDate = module.getFunction(22).getParamValue();
        	sEndDate  = module.getFunction(23).getParamValue();
        }

        if ((sStartDate != null) && (sStartDate.length() != 0)) {
            getTSConnection().BuildQuery("StartMonth",
                    CSTCalendar.getMonthNameFromInput(sStartDate),
                    bResetQueryParam);
            getTSConnection().BuildQuery("StartDay",
                    CSTCalendar.getDayFromInput(sStartDate), false);
            getTSConnection().BuildQuery("StartYear",
                    CSTCalendar.getYearFromInput(sStartDate), false);
            bResetQueryParam = false;
        }
        if ((sEndDate != null) && (sEndDate.length() != 0)) {
            getTSConnection().BuildQuery("EndMonth",
                    CSTCalendar.getMonthNameFromInput(sEndDate),
                    bResetQueryParam);
            getTSConnection().BuildQuery("EndDay",
                    CSTCalendar.getDayFromInput(sEndDate), false);
            getTSConnection().BuildQuery("EndYear",
                    CSTCalendar.getYearFromInput(sEndDate), false);
            bResetQueryParam = false;
        }
        
        return super.SearchBy(bResetQueryParam, module, sd);
    }
    
/* Pretty prints a link that was already followed when creating TSRIndex
 * (non-Javadoc)
 * @see ro.cst.tsearch.servers.types.TSServer#getPrettyFollowedLink(java.lang.String)
 */	
    public String getPrettyFollowedLink (String initialFollowedLnk)
    {
    	if (initialFollowedLnk.matches("(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)[&=a-z]+crossRefSource[=]([a-z]*)[^a-z]*.*"))
    	{
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)[&=a-z]+crossRefSource[=]([a-z]*)[^a-z]*.*", 
    				"Instrument " + "$2" + " " + "$3" + 
    				" has already been processed from a previous search in the log file. ");
    		retStr =  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    		return retStr;
    	}
    	
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
    
    public TSServerInfo getDefaultServerInfo() 
    {
        TSServerInfo msiServerInfoDefault = null;
        TSServerInfoModule simTmp = null;
        //
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        String startDate="", endDate="";
        try{
            
        	Calendar cal = Calendar.getInstance();
            cal.set(1960, 0, 1);
            Date start = cal.getTime();
            
            Date end = sdf.parse(InstanceManager.getManager().getCurrentInstance(searchId).
                    getCrtSearchContext().
                    getSa().
                getAtribute(SearchAttributes.TODATE));
            
          //  sdf.applyPattern("MM/dd/yyyy");
            startDate = sdf.format(start);
            endDate = sdf.format(end);
            
        }catch (ParseException e)
        {        
        }
        if (msiServerInfoDefault == null) {
            //SET SERVER
            //number of search modules
            msiServerInfoDefault = new TSServerInfo(7);
            //set Address
            msiServerInfoDefault
                    .setServerAddress("recorder.jocogov.org");
            //set link
            msiServerInfoDefault
                    .setServerLink("http://recorder.jocogov.org/logon.asp");
            //msiServerInfo.setServerLink("http://www.registerofdeeds.nashville.org/recording/logon.asp?DTSUser=dLowrance&DTSPassword=d3ce564");
            //set IP
            
			///SimpleDateFormat sdf = new SimpleDateFormat( "MMM dd, yyyy" );
			//Date today = new Date();
            
			HashMap<String, String> radioOrderValues = new HashMap<String, String>();
			radioOrderValues.put( "Ascending", "ASC" );
			radioOrderValues.put( "Descending", "DESC" );
			
			HashMap<String, String> radioByValues = new HashMap<String, String>();
			radioByValues.put( "Recorded Date/Time", "DateTime" );
			radioByValues.put( "Document Category", "DocCat" );
            
            msiServerInfoDefault.setServerIP("recorder.jocogov.org");
            { //SET EACH SEARCH
                { //Search by name
                    simTmp = SetModuleSearchByName(3 /* 10 */
                    , msiServerInfoDefault, TSServerInfo.NAME_MODULE_IDX,
                            SEARCH_PATH, TSConnectionURL.idPOST, "LastName",
                            "FirstName");
                    //
                    simTmp.getFunction(2).setParamName("SUBMIT");
                    simTmp.getFunction(2).setDefaultValue("Detail Data");
                    simTmp.getFunction(2).setHiden(true);

                    simTmp.setVisible(false);
                    //
                    //simTmp.getFunction(3).setParamName("DocTypeCats");
                    //simTmp.getFunction(3).setDefaultValue(" "); //Plat ->
                    // val="8"
                    //simTmp.getFunction(3).setHiden(true);
                }
                { //Search by parcel NO
                    simTmp = SetModuleSearchByParcelNo(3, msiServerInfoDefault,
                            TSServerInfo.PARCEL_ID_MODULE_IDX, SEARCH_PATH,
                            TSConnectionURL.idPOST, "Legal1");
                    //
                    simTmp.getFunction(1).setParamName("FREEFORM1");
                    simTmp.getFunction(1).setDefaultValue("ParcelNum");
                    simTmp.getFunction(1).setHiden(true);
                    //
                    simTmp.getFunction(2).setParamName("SUBMIT");
                    simTmp.getFunction(2).setDefaultValue("Detail Data");
                    simTmp.getFunction(2).setHiden(true);

                    simTmp.setVisible(false);
                }
                { //Search by Instrument NO
                    simTmp = SetModuleSearchByInstrumentNo(9,
                            msiServerInfoDefault,
                            TSServerInfo.INSTR_NO_MODULE_IDX, SEARCH_PATH,
                            TSConnectionURL.idPOST, "Instrs");
                    simTmp.setSearchType("IN");
                    
                    try
                    {
    					PageZone searchByInstrument = new PageZone("searchByInstrument", "Search by Instrument Number", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(800), new Integer(50),HTMLObject.PIXELS , true);
    					searchByInstrument.setBorder(true);
    					
    					HTMLControl instrumentNo = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Instrs", "Instrument", 1, 1, 1, 1, 50, null, simTmp.getFunction( 0 ), searchId );
    					instrumentNo.setJustifyField( true );
    					instrumentNo.setRequiredExcl( true );
    					searchByInstrument.addHTMLObject( instrumentNo );
    					
    					HTMLControl book = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Book", "Book", 1, 1, 2, 2, 50, null, simTmp.getFunction( 1 ), searchId );
    					book.setJustifyField( true );
    					book.setRequiredExcl( true );
    					searchByInstrument.addHTMLObject( book );
    					
    					HTMLControl page = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Page", "Page", 1, 1, 3, 3, 50, null, simTmp.getFunction( 2 ), searchId );
    					page.setJustifyField( true );
    					page.setRequiredExcl( true );
    					searchByInstrument.addHTMLObject( page );
    					
    					HTMLControl bookType = new HTMLControl( HTMLControl.HTML_SELECT_BOX, "BookType", "BookType", 1, 1, 4, 4, 50, "", simTmp.getFunction( 8 ), searchId );
						bookType.setJustifyField( true );
						bookType.setRequiredExcl( true );
						searchByInstrument.addHTMLObject( bookType );
    					
    					HTMLControl docId = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "FileNumber", "Document ID", 1, 1, 5, 5, 50, null, simTmp.getFunction( 3 ), searchId );
    					docId.setJustifyField( true );
    					docId.setRequiredExcl( true );
    					searchByInstrument.addHTMLObject( docId );
    					
    					HTMLControl submit = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "SUBMIT", "", 1, 1, 1, 1, 50, "Detail Data", simTmp.getFunction( 4 ), searchId );
    					submit.setHiddenParam( true );
    					searchByInstrument.addHTMLObject( submit );
    					
    					HTMLControl sortOrder = new HTMLControl( HTMLControl.HTML_RADIO_BUTTON, "SortDir", "Order Results", 1, 1, 6, 6, 50, radioOrderValues, simTmp.getFunction( 5 ), searchId);
    					sortOrder.setJustifyField( true );
    					sortOrder.setRequiredExcl( true );
    					sortOrder.setDefaultRadio( "Descending" );
    					searchByInstrument.addHTMLObject( sortOrder );
    					
    					HTMLControl dateFrom = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StartDate", "Recorded From", 1, 1, 7, 7, 50, startDate, simTmp.getFunction( 6 ), searchId);
    					dateFrom.setJustifyField( true );
    					dateFrom.setRequiredExcl( true );
    					searchByInstrument.addHTMLObject( dateFrom );
    					
    					HTMLControl dateTo = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "EndDate", "Recorded To", 1, 1, 8, 8, 50, endDate, simTmp.getFunction( 7 ), searchId);
    					dateTo.setJustifyField( true );
    					dateTo.setRequiredExcl( true );
    					searchByInstrument.addHTMLObject( dateTo );
    					
    					simTmp.setModuleParentSiteLayout( searchByInstrument );
                    }
                    catch( FormatException e )
                    {
                        e.printStackTrace();
                    }
                    
                    //
                    simTmp.getFunction(1).setName("Book No:");
                    simTmp.getFunction(1).setParamName("Book");
                    simTmp.getFunction(1).setSaKey(SearchAttributes.LD_BOOKNO);
                    //
                    simTmp.getFunction(2).setName("Page No:");
                    simTmp.getFunction(2).setParamName("Page");
                    simTmp.getFunction(2).setSaKey(SearchAttributes.LD_PAGENO);
                    
                    simTmp.getFunction(3).setName("File ID:");
                    simTmp.getFunction(3).setParamName("FileNumber");
                    simTmp.getFunction(3).setDefaultValue("");
                  //  simTmp.getFunction(3).setHiden(true);
                   // simTmp.getFunction(2).setSaKey(SearchAttributes.LD_PAGENO);
                    //
                    simTmp.getFunction(4).setParamName("SUBMIT");
                    simTmp.getFunction(4).setDefaultValue("Detail Data");
                    simTmp.getFunction(4).setHiden(true);
                    
                    simTmp.getFunction(5).setName("Sort Order:");
					simTmp.getFunction(5).setParamName("SortDir");
					simTmp.getFunction(5).setValue("ASC");
					
		            simTmp.getFunction(6).setName("Recorded From:");
					simTmp.getFunction(6).setParamName( "StartDate" );
					simTmp.getFunction(6).forceValue(startDate);
					simTmp.getFunction(6).setParamType(TSServerInfoFunction.idDate);
					
					simTmp.getFunction(7).setName("Recorded To:");
					simTmp.getFunction(7).setParamName("EndDate");
					simTmp.getFunction(7).setDefaultValue(endDate);
					simTmp.getFunction(7).setParamType(TSServerInfoFunction.idDate);
					simTmp.getFunction(7).setSaKey(SearchAttributes.TODATE);
					
					
					simTmp.getFunction(8).setName("BookType:");
                    simTmp.getFunction(8).setParamName("BookType");
                    simTmp.getFunction(8).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
                    simTmp.getFunction(8).setDefaultValue("");
                    simTmp.getFunction(8).setHtmlformat(DOC_TYPE_SELECT);
                }
                { //Search by Subdivision
                    simTmp = SetModuleSearchBySubdivisionName(7,
                            msiServerInfoDefault,
                            TSServerInfo.SUBDIVISION_MODULE_IDX, SEARCH_PATH,
                            TSConnectionURL.idPOST, "Legal1");
                    //					
                    simTmp.getFunction(1).setParamName("Legal2");
                    simTmp.getFunction(1).setName("Lot number :");
                    simTmp.getFunction(1).setDefaultValue("");
                    //					
                    simTmp.getFunction(2).setParamName("Legal4");
                    simTmp.getFunction(2).setName("Unit :");
                    simTmp.getFunction(2).setDefaultValue("");
                    //
                    simTmp.getFunction(3).setParamName("FREEFORM1");
                    simTmp.getFunction(3).setDefaultValue("Subdivision");
                    simTmp.getFunction(3).setHiden(true);
                    //
                    simTmp.getFunction(4).setParamName("FREEFORM2");
                    simTmp.getFunction(4).setDefaultValue("LotNumBegin");
                    simTmp.getFunction(4).setHiden(true);
                    //
                    simTmp.getFunction(5).setParamName("FREEFORM4");
                    simTmp.getFunction(5).setDefaultValue("UnitNum");
                    simTmp.getFunction(5).setHiden(true);

                    //
                    simTmp.getFunction(6).setParamName("SUBMIT");
                    simTmp.getFunction(6).setDefaultValue("Detail Data");
                    simTmp.getFunction(6).setHiden(true);

                    simTmp.setVisible(false);
                }
                { //Advanced Parent Site Search
                    simTmp = SetModuleSearchByName(25, msiServerInfoDefault,
                            TSServerInfo.ADV_SEARCH_MODULE_IDX, SEARCH_PATH,
                            TSConnectionURL.idPOST, "LastName", "FirstName");
                    simTmp.setSearchType("CS");
                      
                    try
                    {
						PageZone searchByName = new PageZone("searchByName", "Search By Name", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(800), new Integer(50),HTMLObject.PIXELS , true);
						searchByName.setBorder(true);
						
						HTMLControl lastName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "LastName", "Last Name/Organization", 1, 1, 1, 1, 50, null, simTmp.getFunction( 0 ), searchId );
						lastName.setJustifyField( true );
						lastName.setRequiredExcl( true );
						searchByName.addHTMLObject( lastName );
						
						HTMLControl firstName = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "FirstName", "First Name", 1, 1, 2, 2, 50, null, simTmp.getFunction( 1 ), searchId );
						firstName.setJustifyField( true );
						firstName.setRequiredExcl( true );
						searchByName.addHTMLObject( firstName );
						
						HTMLControl lastNameComparator = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "CmpLastName", "Last Name comparator", 1, 1, 3, 3, 50, "IS", simTmp.getFunction( 2 ), searchId );
						lastNameComparator.setJustifyField( true );
						lastNameComparator.setRequiredExcl( true );
						searchByName.addHTMLObject( lastNameComparator );
						
						HTMLControl firstNameComparator = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "CmpFirstName", "First Name comparator", 1, 1, 4, 4, 50, "LIKE", simTmp.getFunction( 3 ), searchId );
						firstNameComparator.setJustifyField( true );
						firstNameComparator.setRequiredExcl( true );
						searchByName.addHTMLObject( firstNameComparator );
						
    					HTMLControl submit = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "SUBMIT", "", 1, 1, 1, 1, 50, "Detail Data", simTmp.getFunction( 4 ), searchId );
    					submit.setHiddenParam( true );
    					searchByName.addHTMLObject( submit );
    					
						HTMLControl freeForm1 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "FREEFORM1", "Legal description I", 1, 1, 5, 5, 50, "Subdivision", simTmp.getFunction( 5 ), searchId );
						freeForm1.setJustifyField( true );
						freeForm1.setRequiredExcl( true );
						searchByName.addHTMLObject( freeForm1 );
						
						HTMLControl cmpLegal1 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "CmpLegal1", "Legal 1 comparator", 1, 1, 6, 6, 50, "LIKE", simTmp.getFunction( 6 ), searchId );
						cmpLegal1.setJustifyField( true );
						cmpLegal1.setRequiredExcl( true );
						searchByName.addHTMLObject( cmpLegal1 );
						
						HTMLControl legal1 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Legal1", "Description I", 1, 1, 7, 7, 50, null, simTmp.getFunction( 7 ), searchId );
						legal1.setJustifyField( true );
						legal1.setRequiredExcl( true );
						searchByName.addHTMLObject( legal1 );
						
						HTMLControl freeForm2 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "FREEFORM2", "Legal description II", 1, 1, 8, 8, 50, "Block", simTmp.getFunction( 8 ), searchId );
						freeForm2.setJustifyField( true );
						freeForm2.setRequiredExcl( true );
						searchByName.addHTMLObject( freeForm2 );
						
						HTMLControl cmpLegal2 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "CmpLegal2", "Legal 2 comparator", 1, 1, 9, 9, 50, "IS", simTmp.getFunction( 9 ), searchId );
						cmpLegal2.setJustifyField( true );
						cmpLegal2.setRequiredExcl( true );
						searchByName.addHTMLObject( cmpLegal2 );
						
						HTMLControl legal2 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Legal2", "Description II", 1, 1, 10, 10, 50, null, simTmp.getFunction( 10 ), searchId );
						legal2.setJustifyField( true );
						legal2.setRequiredExcl( true );
						searchByName.addHTMLObject( legal2 );
						
						HTMLControl freeForm3 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "FREEFORM3", "Legal description III", 1, 1, 11, 11, 50, "LotNumBegin", simTmp.getFunction( 11 ), searchId );
						freeForm3.setJustifyField( true );
						freeForm3.setRequiredExcl( true );
						searchByName.addHTMLObject( freeForm3 );
						
						HTMLControl cmpLegal3 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "CmpLegal3", "Legal 3 comparator", 1, 1, 12, 12, 50, "IS", simTmp.getFunction( 12 ), searchId );
						cmpLegal3.setJustifyField( true );
						cmpLegal3.setRequiredExcl( true );
						searchByName.addHTMLObject( cmpLegal3 );
						
						HTMLControl legal3 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Legal3", "Description III", 1, 1, 13, 13, 50, null, simTmp.getFunction( 13 ), searchId );
						legal3.setJustifyField( true );
						legal3.setRequiredExcl( true );
						searchByName.addHTMLObject( legal3 );
						
						HTMLControl freeForm4 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "FREEFORM4", "Legal description IV", 1, 1, 14, 14, 50, "ParcelNum", simTmp.getFunction( 14 ), searchId );
						freeForm4.setJustifyField( true );
						freeForm4.setRequiredExcl( true );
						searchByName.addHTMLObject( freeForm4 );
						
						HTMLControl cmpLegal4 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "CmpLegal4", "Legal 4 comparator", 1, 1, 15, 15, 50, "IS", simTmp.getFunction( 15 ), searchId );
						cmpLegal4.setJustifyField( true );
						cmpLegal4.setRequiredExcl( true );
						searchByName.addHTMLObject( cmpLegal4 );
						
						HTMLControl legal4 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Legal4", "Description IV", 1, 1, 16, 16, 50, "", simTmp.getFunction( 16 ), searchId );
						legal4.setJustifyField( true );
						legal4.setRequiredExcl( true );
						searchByName.addHTMLObject( legal4 );
						
						HTMLControl freeForm5 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "FREEFORM5", "Legal description V", 1, 1, 17, 17, 50, "MedLegal1", simTmp.getFunction( 17 ), searchId );
						freeForm5.setJustifyField( true );
						freeForm5.setRequiredExcl( true );
						searchByName.addHTMLObject( freeForm5 );
						
						HTMLControl cmpLegal5 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "CmpLegal5", "Legal 5 comparator", 1, 1, 18, 18, 50, "IS", simTmp.getFunction( 18 ), searchId );
						cmpLegal5.setJustifyField( true );
						cmpLegal5.setRequiredExcl( true );
						searchByName.addHTMLObject( cmpLegal5 );
						
						HTMLControl legal5 = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "Legal5", "Description V", 1, 1, 19, 19, 50, null, simTmp.getFunction( 19 ), searchId );
						legal5.setJustifyField( true );
						legal5.setRequiredExcl( true );
						searchByName.addHTMLObject( legal5 );
						
						HTMLControl documentCat = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "DocTypes", "Document Category", 1, 1, 20, 20, 50, "", simTmp.getFunction( 20 ), searchId );
						documentCat.setJustifyField( true );
						documentCat.setRequiredExcl( true );
						searchByName.addHTMLObject( documentCat );
						
    					HTMLControl sortOrder = new HTMLControl( HTMLControl.HTML_RADIO_BUTTON, "SortDir", "Order Results", 1, 1, 21, 21, 50, radioOrderValues, simTmp.getFunction( 21 ), searchId);
    					sortOrder.setJustifyField( true );
    					sortOrder.setRequiredExcl( true );
    					sortOrder.setDefaultRadio( "Descending" );
    					searchByName.addHTMLObject( sortOrder );
    					
    					HTMLControl sortBy = new HTMLControl( HTMLControl.HTML_RADIO_BUTTON, "SortBy", "Order By", 1, 1, 22, 22, 50, radioByValues, simTmp.getFunction( 24 ), searchId);
    					sortBy.setJustifyField( true );
    					sortBy.setRequiredExcl( true );
    					sortBy.setDefaultRadio( "Recorded Date/Time" );
    					searchByName.addHTMLObject( sortBy );
						
    					HTMLControl dateFrom = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StartDate", "Recorded From", 1, 1, 23, 23, 50, startDate, simTmp.getFunction( 22 ), searchId);
    					dateFrom.setJustifyField( true );
    					dateFrom.setRequiredExcl( true );
    					searchByName.addHTMLObject( dateFrom );
    					
    					HTMLControl dateTo = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "EndDate", "Recorded To", 1, 1, 24, 24, 50, endDate, simTmp.getFunction( 23 ), searchId);
    					dateTo.setJustifyField( true );
    					dateTo.setRequiredExcl( true );
    					searchByName.addHTMLObject( dateTo );
    					
						simTmp.setModuleParentSiteLayout( searchByName );
                    }
                    catch( FormatException e )
                    {
                        e.printStackTrace();
                    }
                    
                    simTmp.getFunction(2).setHtmlformat("<select NAME=\"CmpLastName\"><OPTION VALUE='LIKE'>LIKE<OPTION VALUE='IS' selected>IS</select>");
                    simTmp.getFunction(2).setParamName("CmpLastName");
                    simTmp.getFunction(2).setName("Last Name comparator:");
                    simTmp.getFunction(2).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
                    simTmp.getFunction(2).setDefaultValue("IS");
                    //simTmp.getFunction(14).setHiden(true);
                    
                    
                    simTmp.getFunction(3).setParamName("CmpFirstName");
                    simTmp.getFunction(3).setName("First Name comparator:");
                    simTmp.getFunction(3).setHtmlformat("<select NAME=\"CmpFirstName\"><OPTION VALUE='LIKE' selected>LIKE<OPTION VALUE='IS'>IS</select>");
                    simTmp.getFunction(3).setDefaultValue("LIKE");
                    simTmp.getFunction(3).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
                  //  simTmp.getFunction(15).setHiden(true);   
                    
                    simTmp.getFunction(4).setHiddenParam("SUBMIT",
                            "Detail Data");
                    simTmp.getFunction(4).setDefaultValue("Detail Data");
                    
                    simTmp
                            .getFunction(5)
                            .setHtmlformat(
                                    "<SELECT NAME=\"FREEFORM1\">"
                                            + "<OPTION VALUE='Subdivision' SELECTED>Subdivision"
                                            +"<OPTION VALUE='Block'>Block"
                                            +"<OPTION VALUE='LotNumBegin'>Lot Number"
                                            +"<OPTION VALUE='ParcelNum'>PIN"
                                            +"<OPTION VALUE='MedLegal1'>Tract"
                                            +"<OPTION VALUE='UnitNum'>Unit"
                                            +"<OPTION VALUE='MedLegal4'>Building"
                                            +"<OPTION VALUE='MedLegal5'>Garage"
                                            +"<OPTION VALUE='MedLegal2'>QuarterQuarter"
                                            +"<OPTION VALUE='MedLegal3'>Quarter"
                                            +"<OPTION VALUE='Section'>Section"
                                            +"<OPTION VALUE='Townshipname'>Township"
                                            +"<OPTION VALUE='Range'>Range"
                                            +"<OPTION VALUE='ExtendedDesc'>Extended Description"
                                            + "</SELECT>");
                    simTmp.getFunction(5).setName("Legal description I");
                    simTmp.getFunction(5).setParamName("FREEFORM1");
                    simTmp.getFunction(5).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
                    simTmp.getFunction(5).setDefaultValue("Subdivision");
                    
                    simTmp.getFunction(6).setParamName("CmpLegal1");
                    simTmp.getFunction(6).setName("Legal 1 comparator:");
                    simTmp.getFunction(6).setDefaultValue("LIKE");
                    simTmp.getFunction(6).setHtmlformat("<SELECT NAME=\"CmpLegal1\"><OPTION VALUE='LIKE' SELECTED>LIKE<OPTION VALUE='IS'>IS<OPTION VALUE='IN'>IN</select>");
                    simTmp.getFunction(6).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
                    //simTmp.getFunction(16).setHiden(true);
                    
                    simTmp.getFunction(7).setName("Description I:");
                    simTmp.getFunction(7).setParamName("Legal1");
                    simTmp.getFunction( 7 ).setSaKey( SearchAttributes.LD_SUBDIV_NAME );
                    
                    simTmp
                            .getFunction(8)
                            .setHtmlformat(
                                    "<SELECT NAME=\"FREEFORM2\">"
                                    + "<OPTION VALUE='Subdivision'>Subdivision "
                                    +"<OPTION VALUE='Block' SELECTED>Block"
                                    +"<OPTION VALUE='LotNumBegin'>Lot Number"
                                    +"<OPTION VALUE='ParcelNum'>PIN"
                                    +"<OPTION VALUE='MedLegal1'>Tract"
                                    +"<OPTION VALUE='UnitNum'>Unit"
                                    +"<OPTION VALUE='MedLegal4'>Building"
                                    +"<OPTION VALUE='MedLegal5'>Garage"
                                    +"<OPTION VALUE='MedLegal2'>QuarterQuarter"
                                    +"<OPTION VALUE='MedLegal3'>Quarter"
                                    +"<OPTION VALUE='Section'>Section"
                                    +"<OPTION VALUE='Townshipname'>Township"
                                    +"<OPTION VALUE='Range'>Range"
                                    +"<OPTION VALUE='ExtendedDesc'>Extended Description"
                                    + "</SELECT>");
                    simTmp.getFunction(8).setName("Legal description II");
                    simTmp.getFunction(8).setParamName("FREEFORM2");
                    simTmp.getFunction(8).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
                    simTmp.getFunction(8).setDefaultValue("Block");
                    
                    simTmp.getFunction(9).setParamName("CmpLegal2");
                    simTmp.getFunction(9).setName("Legal 2 comparator:");
                    simTmp.getFunction(9).setDefaultValue("IS");
                    simTmp.getFunction(9).setHtmlformat("<SELECT NAME=\"CmpLegal2\"><OPTION VALUE='LIKE'>LIKE<OPTION VALUE='IS' SELECTED>IS</select>");
                    simTmp.getFunction(9).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
                    //simTmp.getFunction(17).setHiden(true);
                    
                    
                    simTmp.getFunction(10).setName("Description II:");
                    simTmp.getFunction(10).setParamName("Legal2");
                    simTmp.getFunction( 10 ).setSaKey( SearchAttributes.LD_SUBDIV_BLOCK );
                    
                    simTmp
                            .getFunction(11)
                            .setHtmlformat(
                                    "<SELECT NAME=\"FREEFORM3\">"
                                    + "<OPTION VALUE='Subdivision'>Subdivision"
                                    +"<OPTION VALUE='Block'>Block"
                                    +"<OPTION VALUE='LotNumBegin' SELECTED>Lot Number"
                                    +"<OPTION VALUE='ParcelNum'>PIN"
                                    +"<OPTION VALUE='MedLegal1'>Tract"
                                    +"<OPTION VALUE='UnitNum'>Unit"
                                    +"<OPTION VALUE='MedLegal4'>Building"
                                    +"<OPTION VALUE='MedLegal5'>Garage"
                                    +"<OPTION VALUE='MedLegal2'>QuarterQuarter"
                                    +"<OPTION VALUE='MedLegal3'>Quarter"
                                    +"<OPTION VALUE='Section'>Section"
                                    +"<OPTION VALUE='Townshipname'>Township"
                                    +"<OPTION VALUE='Range'>Range"
                                    +"<OPTION VALUE='ExtendedDesc'>Extended Description"
                                    + "</SELECT>");
                    simTmp.getFunction(11).setName("Legal description III");
                    simTmp.getFunction(11).setParamName("FREEFORM3");
                    simTmp.getFunction(11).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
                    simTmp.getFunction(11).setDefaultValue("LotNumBegin");
                    
                    simTmp.getFunction(12).setParamName("CmpLegal3");
                    simTmp.getFunction(12).setName("Legal 3 comparator:");
                    simTmp.getFunction(12).setDefaultValue("IS");
                    simTmp.getFunction(12).setHtmlformat("<SELECT NAME=\"CmpLegal3\"><OPTION VALUE='LIKE'>LIKE<OPTION VALUE='IS' SELECTED>IS</select>");
                    simTmp.getFunction(12).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
                    //simTmp.getFunction(18).setHiden(true);
                    
                    
                    simTmp.getFunction(13).setName("Description III:");
                    simTmp.getFunction(13).setParamName("Legal3");
                    simTmp.getFunction( 13 ).setSaKey( SearchAttributes.LD_LOTNO );
                    
                    simTmp
                            .getFunction(14)
                            .setHtmlformat(
                                    "<SELECT NAME=\"FREEFORM4\">"
                                    + "<OPTION VALUE='Subdivision'>Subdivision"
                                    +"<OPTION VALUE='Block'>Block"
                                    +"<OPTION VALUE='LotNumBegin'>Lot Number"
                                    +"<OPTION VALUE='ParcelNum' SELECTED>PIN"
                                    +"<OPTION VALUE='MedLegal1'>Tract"
                                    +"<OPTION VALUE='UnitNum'>Unit"
                                    +"<OPTION VALUE='MedLegal4'>Building"
                                    +"<OPTION VALUE='MedLegal5'>Garage"
                                    +"<OPTION VALUE='MedLegal2'>QuarterQuarter"
                                    +"<OPTION VALUE='MedLegal3'>Quarter"
                                    +"<OPTION VALUE='Section'>Section"
                                    +"<OPTION VALUE='Townshipname'>Township"
                                    +"<OPTION VALUE='Range'>Range"
                                    +"<OPTION VALUE='ExtendedDesc'>Extended Description"
                                    + "</SELECT>");
                    simTmp.getFunction(14).setName("Legal description IV");
                    simTmp.getFunction(14).setParamName("FREEFORM4");
                    simTmp.getFunction(14).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
                    simTmp.getFunction(14).setDefaultValue("ParcelNum");
                    

                    simTmp.getFunction(15).setParamName("CmpLegal4");
                    simTmp.getFunction(15).setName("Legal 4 comparator:");
                    simTmp.getFunction(15).setDefaultValue("IS");
                    simTmp.getFunction(15).setHtmlformat("<SELECT NAME=\"CmpLegal4\"><OPTION VALUE='LIKE'>LIKE<OPTION VALUE='IS' SELECTED>IS</select>");
                    simTmp.getFunction(15).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
//                    simTmp.getFunction(19).setHiden(true);
                    
                    
                    
                    simTmp.getFunction(16).setName("Description IV:");
                    simTmp.getFunction(16).setParamName("Legal4");
                    simTmp.getFunction(16).setDefaultValue("");
                    
                    
                    simTmp
                    .getFunction(17)
                    .setHtmlformat(
                            "<SELECT NAME=\"FREEFORM5\">"
                            + "<OPTION VALUE='Subdivision'>Subdivision"
                            +"<OPTION VALUE='Block'>Block"
                            +"<OPTION VALUE='LotNumBegin'>Lot Number"
                            +"<OPTION VALUE='ParcelNum'>PIN"
                            +"<OPTION VALUE='MedLegal1' SELECTED>Tract"
                            +"<OPTION VALUE='UnitNum'>Unit"
                            +"<OPTION VALUE='MedLegal4'>Building"
                            +"<OPTION VALUE='MedLegal5'>Garage"
                            +"<OPTION VALUE='MedLegal2'>QuarterQuarter"
                            +"<OPTION VALUE='MedLegal3'>Quarter"
                            +"<OPTION VALUE='Section'>Section"
                            +"<OPTION VALUE='Townshipname'>Township"
                            +"<OPTION VALUE='Range'>Range"
                            +"<OPTION VALUE='ExtendedDesc'>Extended Description"
                            + "</SELECT>");
            simTmp.getFunction(17).setName("Legal description V");
            simTmp.getFunction(17).setParamName("FREEFORM5");
            simTmp.getFunction(17).setParamType(
                    TSServerInfoFunction.idSingleselectcombo);            
            simTmp.getFunction(17).setDefaultValue("MedLegal1");
            
            simTmp.getFunction(18).setParamName("CmpLegal5");
            simTmp.getFunction(18).setName("Legal 5 comparator:");
            simTmp.getFunction(18).setDefaultValue("IS");
            simTmp.getFunction(18).setHtmlformat("<SELECT NAME=\"CmpLegal5\"><OPTION VALUE='LIKE'>LIKE<OPTION VALUE='IS' SELECTED>IS</select>");
            simTmp.getFunction(18).setParamType(
                    TSServerInfoFunction.idSingleselectcombo);
//                  simTmp.getFunction(20).setHiden(true);
            
            simTmp.getFunction(19).setName("Description V:");
            simTmp.getFunction(19).setParamName("Legal5");
            simTmp.getFunction( 19 ).setSaKey( "" );
           
                    simTmp
                            .getFunction(20)
                            .setHtmlformat(
                                    "<select NAME=\"DocTypes\" SIZE='6' MULTIPLE>"
                                    +"<option VALUE='ABST JUDGE'>ABST JUDGE - Abstract of Judgement"
                                    +"<option VALUE='AFF'>AFF - Affidavit"

                                    +"<option VALUE='AFF DOC REFILE'>AFF DOC REFILE - Affidavit of Document Refile"
                                    +"<option VALUE='AFF EQU INT'>AFF EQU INT - Affidavit of Equitable Interest"
                                    +"<option VALUE='AFF MOBILE'>AFF MOBILE - Aff Permanently Affixed Mobile Home" 
                                    +"<option VALUE='AGIST LIEN'>AGIST LIEN - Agister's Lien"
                                    +"<option VALUE='ARTIS LIEN'>ARTIS LIEN - Artisian's Lien"
                                    +"<option VALUE='ASG LSE'>ASG LSE - Assignment of Leases"
                                    +"<option VALUE='ASG RENT'>ASG RENT - Assignment of Rents" 
                                    +"<option VALUE='ASGN MTG'>ASGN MTG - Assignment of Mortgage"
                                    +"<option VALUE='ASGN MTG &'>ASGN MTG & - Assignment of Mortgage & Other Documents"
                                    +"<option VALUE='ASGN O&G LSE'>ASGN O&G LSE - Assignment of Oil and Gas Lease"
                                    +"<option VALUE='ASGN PROCEEDS'>ASGN PROCEEDS - Assignment of Proceeds"
                                    +"<option VALUE='ASGN RENT & LSE'>ASGN RENT & LSE - Assignment of Rents and Leases"

                                    +"<option VALUE='CEM DEED'>CEM DEED - Cemetery Deed"
                                    +"<option VALUE='CERT DISCHG PROP'>CERT DISCHG PROP - Cert. Discharge of Prop. / Partial Relea"
                                    +"<option VALUE='CERT NONATTACH'>CERT NONATTACH - Cert. Nonattachment"
                                    +"<option VALUE='CHATTEL MTG'>CHATTEL MTG - Chattel Mortgage"
                                    +"<option VALUE='CONDEMN'>CONDEMN - Condemnations"
                                    +"<option VALUE='COURT REC'>COURT REC - Court Records"
                                    +"<option VALUE='DEATH CERT'>DEATH CERT - Death Certificate"
                                    +"<option VALUE='DEC HOME ASSN'>DEC HOME ASSN - Declaration of Home Owners Association"
                                    +"<option VALUE='DECL REST'>DECL REST - Declaration of Restrictions"
                                    +"<option VALUE='DEED'>DEED"
                                    +"<option VALUE='DISC ESMT'>DISC ESMT - Disclaimer of Easement"
                                    +"<option VALUE='DOC OMITTED'>DOC OMITTED - Doc Intentionally Omitted"
                                    +"<option VALUE='DPTJUST LI'>DPTJUST LI - Dept. of Justice Notice of Lien" 
                                    +"<option VALUE='EM DOMAIN'>EM DOMAIN - Eminent Domain"
                                    +"<option VALUE='ESMT'>ESMT - Easement"
                                    +"<option VALUE='FED TX LIEN'>FED TX LIEN - Federal Tax Lien"
                                    +"<option VALUE='FTX'>FTX - Fed Tax Lien Cert Sub"

                                    +"<option VALUE='FUEL TX LIEN'>FUEL TX LIEN - Fuel Tax Lien"
                                    +"<option VALUE='GOV RES ORD'>GOV RES ORD - Government Resolution / Ordinance / Boun"
                                    +"<option VALUE='INC PAPERS'>INC PAPERS - Incorporation Papers"
                                    +"<option VALUE='KS EMP TX LIEN'>KS EMP TX LIEN - Kansas State Employer's Tax Lien"
                                    +"<option VALUE='KS ST TX ORD'>KS ST TX ORD - Kansas State Inheritance Tax Order"
                                    +"<option VALUE='LIS PENDENS'>LIS PENDENS - Lis Pendes"
                                    +"<option VALUE='MECH LIEN'>MECH LIEN - Mechanics Lien" 
                                    +"<option VALUE='MEM AGREE'>MEM AGREE - Memorandum of Agreement"
                                    +"<option VALUE='MEMO LSE'>MEMO LSE - Memorandum of Lease"
                                    +"<option VALUE='MTG'>MTG - Mortgage" 
                                    +"<option VALUE='Mtg Reg'>Mtg Reg - Mortgage Registration Fee Addidavit" 
                                    +"<option VALUE='MULTI MTG'>MULTI MTG - Multi-County Mortgage" 
                                    +"<option VALUE='NOTARY'>NOTARY"
                                    +"<option VALUE='NOTICE ENV USE CTRL'>NOTICE ENV USE CTRL - Notice of Environmental Use Control" 
                                    +"<option VALUE='NOTICE LIE'>NOTICE LIE - Notice of Lien US" 
                                    +"<option VALUE='O&G LEASE'>O&G LEASE - Oil and Gas Lease"

                                    +"<option VALUE='OASIS PRINT'>OASIS PRINT - OASIS PRINT SCREEN"
                                    +"<option VALUE='OTHER'>OTHER"
                                    +"<option VALUE='OTHER LIEN'>OTHER LIEN"
                                    +"<option VALUE='OTHER ST TX LIEN'>OTHER ST TX LIEN - Other State Employer's Tax Lien"
                                    +"<option VALUE='PAR REL MTG'>PAR REL MTG - Partial Release of Mortgage" 
                                    +"<option VALUE='PAR REL MTG & OTH'>PAR REL MTG & OTH - Partial Release of Mortgage & Other Docu"
                                    +"<option VALUE='PLAT'>PLAT"
                                    +"<option VALUE='POA'>POA - Power of Attorney"
                                    +"<option VALUE='POR'>POR - Picture of Record" 
                                    +"<option VALUE='PT DISC ESMT'>PT DISC ESMT - Partial Disclaimer of Easement"
                                    +"<option VALUE='REL ART LIEN'>REL ART LIEN - Release of Artisans Lien"
                                    +"<option VALUE='REL ASGNRL'>REL ASGNRL - Release of Assignment and Rnts & Lease" 
                                    +"<option VALUE='REL FED TX LIEN'>REL FED TX LIEN - Release Federal Tax Lien"
                                    +"<option VALUE='REL JUDGE'>REL JUDGE - Release of Judgment"

                                    +"<option VALUE='REL LIS PENDENS'>REL LIS PENDENS - Lis Pendes Release"
                                    +"<option VALUE='REL MTG'>REL MTG - Release of Mortgage"
                                    +"<option VALUE='REL MTG &'>REL MTG & - Release of Mortgage & Other Documents" 
                                    +"<option VALUE='REL NOTICE OTH LIEN'>REL NOTICE OTH LIEN - Release of Notice of Lien US" 
                                    +"<option VALUE='REL OTHER LIEN'>REL OTHER LIEN - Release of Other Lien"
                                    +"<option VALUE='REL ST TX'>REL ST TX - Rel of Kansas State Employer's Tax Lien" 
                                    +"<option VALUE='REL WSTE WTRLIEN'>REL WSTE WTRLIEN - Release of Wastewater Lien"
                                    +"<option VALUE='RELDEPTJST'>RELDEPTJST - Release of Department of Justice/Notice "
                                    +"<option VALUE='RELMOR&ASG'>RELMOR&ASG - Release of Mortgage and Assignment" 
                                    +"<option VALUE='RELMTG/AGN'>RELMTG/AGN - Rel Mtg & Asgn Rents & Asgn Int in Lse" 
                                    +"<option VALUE='REV POA'>REV POA - Revocation of Power of Attorney"

                                    +"<option VALUE='REV REL FED TX LIEN'>REV REL FED TX LIEN - Revocation of Release of Federal Tax Lie"
                                    +"<option VALUE='REV REL MTGL'>REV REL MTGL - Revocation of Release of Mortgage"
                                    +"<option VALUE='REV TOD'>REV TOD - Revocation of Transfer on Death Deed"
                                    +"<option VALUE='RW'>RW - Right of Way"
                                    +"<option VALUE='SEED&BAIL LIEN'>SEED&BAIL LIEN - Seeding and Bailing of Broomcorn and Hay"
                                    +"<option VALUE='STATETXORDER'>STATETXORDER - Other State Inheritance Tax Order"
                                    +"<option VALUE='SUB AGR'>SUB AGR - Subordination Agreement" 
                                    +"<option VALUE='THRESH LIEN'>THRESH LIEN - Threshing and Husking Lien"
                                    +"<option VALUE='TOD'>TOD - Transfer on Death Deed" 
                                    +"<option VALUE='TRUCK LIEN'>TRUCK LIEN - Quarterly Truck Registration Lien"
                                    +"<option VALUE='UCC AMEND'>UCC AMEND - UCC Amendment" 
                                    +"<option VALUE='UCC ASGN'>UCC ASGN - UCC Assignment" 
                                    +"<option VALUE='UCC CONT'>UCC CONT - UCC Continuation" 
                                    +"<option VALUE='UCC COR'>UCC COR - UCC CORRECTION" 
                                    +"<option VALUE='UCC FIX'>UCC FIX - UCC Fixture Filing" 

                                    +"<option VALUE='UCC FS'>UCC FS - UCC Financing Statement" 
                                    +"<option VALUE='UCC REL'>UCC REL - UCC Release" 
                                    +"<option VALUE='UCC TERM'>UCC TERM - UCC Termination" 
                                    +"<option VALUE='VET LIEN'>VET LIEN - Veterinary Lien"
                                    +"<option VALUE='WAIV & CONS'>WAIV & CONS - Waiver and Consent to Disposition of Rea"
                                    +"<option VALUE='WFEDTAXLIE'>WFEDTAXLIE - Withdrawal of Federal Tax Lien"
                                    +"<option VALUE='WSTE WTR LIEN'>WSTE WTR LIEN - Wastewater Lien"
                                            + "</select>");
                    simTmp.getFunction(20).setName("Document Category:");                   
                    simTmp.getFunction(20).setParamName("DocTypes");
                    simTmp.getFunction(20).setParamType(
                            TSServerInfoFunction.idSingleselectcombo);
                    simTmp.getFunction(20).setDefaultValue("");
                    
                    simTmp.getFunction(21).setName("Sort Order:");
					simTmp.getFunction(21).setParamName("SortDir");
					simTmp.getFunction(21).setValue("ASC");
					simTmp.getFunction(21).setDefaultValue("ASC");
                    
		            simTmp.getFunction(22).setName("Recorded From:");
					simTmp.getFunction(22).setParamName( "StartDate" );
					simTmp.getFunction(22).forceValue(startDate );
					simTmp.getFunction(22).setParamType(TSServerInfoFunction.idDate);
					
					simTmp.getFunction(23).setName("Recorded To:");
					simTmp.getFunction(23).setParamName("EndDate");
					simTmp.getFunction(23).setDefaultValue(endDate);
					simTmp.getFunction(23).setParamType(TSServerInfoFunction.idDate);
					simTmp.getFunction(23).setSaKey(SearchAttributes.TODATE);
                    
					simTmp.getFunction(24).setName("Sort By:");
					simTmp.getFunction(24).setParamName("SortBy");
					simTmp.getFunction(24).setValue("DateTime");
					simTmp.getFunction(24).setDefaultValue("DateTime");
                  
                
                }
                {
                	simTmp = SetModuleArchiveBrowsing(0, msiServerInfoDefault,
                        	TSServerInfo.ARCHIVE_DOCS_MODULE_IDX, "/SPVIndexSearch.asp",
                			TSConnectionURL.idGET);
                	simTmp.setSearchType("IN");
                	//simTmp = SetModuleBackScannedDeeds(2, msiServerInfoDefault,TSServerInfo.PROP_NO_IDX, "/SPVIndexSearch.asp",TSConnectionURL.idGET);
	                try
	                {
	        		    PageZone instrBPS = new PageZone("browseScannedPages", "Index Book Page Search", HTMLObject.ORIENTATION_HORIZONTAL, 
	        		    		null, new Integer(800), new Integer(50),HTMLObject.PIXELS , true);
	        		    instrBPS.setBorder(true);
	        		    
	        		    /*HTMLControl cNum = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "cnum", "", 1, 1, 1, 1, 30, "24", simTmp.getFunction( 0 ), searchId );
	        		    cNum.setHiddenParam( true );
	        		    instrBPS.addHTMLObject( cNum );
	        		    
	        		    HTMLControl op = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "op", "", 1, 1, 1, 1, 30, "deedlookup", simTmp.getFunction( 1 ), searchId );
	        		    op.setHiddenParam( true );
	        		    instrBPS.addHTMLObject( op );*/ 
	        		    
	        		    simTmp.setModuleParentSiteLayout( instrBPS );
	                }
	                catch( Exception e )
	                {
	                    e.printStackTrace();
	                }
                }
                //simTmp.getFunction(0).setParamName("cnum");
                //simTmp.getFunction(0).setDefaultValue("24");
                //simTmp.getFunction(0).setHiden(true); 
            {       	
                simTmp = SetModuleArchiveBrowsing(0, msiServerInfoDefault,
                    	TSServerInfo.SECOND_ARCHIVE_DOCS_MODULE_IDX, "/SPVBookSearch.asp",
            			TSConnectionURL.idGET);
                simTmp.setSearchType("IN");
                
	            try
	            {
	    		    PageZone instrBPSp = new PageZone("browseScannedBookPages", "Book Page Search", HTMLObject.ORIENTATION_HORIZONTAL, 
	    		    		null, new Integer(800), new Integer(50),HTMLObject.PIXELS , true);
	    		    instrBPSp.setBorder(true);
	    		    
	    		    //HTMLControl bookType = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "BookType", "", 1, 1, 1, 1, 30, "Index to Land,", simTmp.getFunction( 0 ), searchId );
					//bookType.setHiddenParam( true );
					//nstrBPSp.addHTMLObject( bookType );
	    		    
					/*HTMLControl sortOrder = new HTMLControl( HTMLControl.HTML_RADIO_BUTTON, "SortDir", "Order Results", 1, 1, 2, 2, 50, radioOrderValues, simTmp.getFunction( 1 ), searchId);
					sortOrder.setJustifyField( true );
					sortOrder.setRequiredExcl( true );
					sortOrder.setDefaultRadio( "Descending" );
					instrBPSp.addHTMLObject( sortOrder );
					
					HTMLControl dateFrom = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "StartDate", "Recorded From", 1, 1, 3, 3, 50, startDate, simTmp.getFunction( 2 ), searchId);
					dateFrom.setJustifyField( true );
					dateFrom.setRequiredExcl( true );
					instrBPSp.addHTMLObject( dateFrom );
					
					HTMLControl dateTo = new HTMLControl( HTMLControl.HTML_TEXT_FIELD, "EndDate", "Recorded To", 1, 1, 4, 4, 50, endDate, simTmp.getFunction( 3 ), searchId);
					dateTo.setJustifyField( true );
					dateTo.setRequiredExcl( true );
					instrBPSp.addHTMLObject( dateTo );*/
	    		    
	    		    simTmp.setModuleParentSiteLayout( instrBPSp );
	            }
	            catch( Exception e )
	            {
	                e.printStackTrace();
	            }
			
                }
                
                { //DateFilter
                    /*
                    miDateModule = TSServerInfo.BGN_END_DATE_MODULE_IDX;
                    simTmp = SetModuleDateFilter(msiServerInfoDefault,
                            TSServerInfo.BGN_END_DATE_MODULE_IDX, "StartDate",
                            "EndDate", "Jan 1, 1845", "");
                    */

                }
                { //SortOrderFilter
                    /*
                    simTmp = SetModuleSortFilter(msiServerInfoDefault,
                            TSServerInfo.SORT_TYPE_MODULE_IDX, "SortDir",
                            "SortDir", "ASC", "DESC");
                     */
                }
               
            }
            msiServerInfoDefault.setupParameterAliases();
            setModulesForAutoSearch(msiServerInfoDefault);
            setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
        }
        return msiServerInfoDefault;
    }
    
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded)
    	throws ServerResponseException {
    	ServerResponse rtnResponse;
		if (vsRequest.indexOf("LoadImage") != -1) {
		    //try to get the file
		    //request file
		    getTSConnection().setHostName("recorder.jocogov.org");
		    
		
		    getTSConnection().SetReferer(FILE_REFERER);
		    rtnResponse = super.GetLink(vsRequest, vbEncoded);
		    getTSConnection().SetReferer("");
		    getTSConnection().setHostName(msiServerInfo.getServerAddress());
		    getTSConnection().setHostIP(msiServerInfo.getServerIP());
			} else {
		    getTSConnection().SetReferer(FILE_REFERER);
		    
		    rtnResponse = super.GetLink(vsRequest, vbEncoded);
		    getTSConnection().SetReferer("");

		}
		
	return rtnResponse;
	}
    
    /*
     * Given an instrument number, it forms a link that starts a search 
     * for the respective value.
     */
    private String GetLinkInstrument (String instr)
    {
    	String link_str=new String();
        String default_link=CreatePartialLink(TSConnectionURL.idGET);
        default_link.substring(0,default_link.indexOf("&Link="));
        link_str = " <a HREF='" + default_link + SEARCH_PATH +
        "&Instrs=" + instr + "'>" + instr + "</a> ";
        
        return link_str;
    }
    
    /*
     * Given a book and a page number, it forms a link that starts a search 
     * for the respective values.
     */
    private String GetLinkBookPage (String book, String page, String bk_pg)
    {
    	String link_str=new String();
        String default_link=CreatePartialLink(TSConnectionURL.idGET);
        default_link.substring(0,default_link.indexOf("&Link="));
        link_str = " <a HREF='" + default_link + SEARCH_PATH +
        "&Instrs=&Book=" + book + "&Page=" + page +
        "&SUBMIT=Detail Data&SortDir=ASC&StartDate=&EndDate="+
        "'>" + bk_pg + "</a> ";
        
        return link_str;
    }
    
    
    /**
     * Add the document missing links
     * @param record
     * @return
     */
    private String addMissingLinks (String record){
    	
    	// determine start of interesting sequence
    	int istart = record.indexOf("Marginal:");
    	if(istart == -1){return record;}
    	istart = record.indexOf("<TD", istart);
    	if(istart == -1){return record;}
    	istart = record.indexOf(">", istart);
    	if(istart == -1){return record;}
    	istart +=1;
    	
    	// determine end of interesting sequence
    	int iend = record.indexOf("</TD></TR></Table>", istart);
    	if(iend == -1){return record;}
    	
    	// split record into prefix, toProcess, suffix
    	String prefix = record.substring(0, istart);
    	String toProcess = record.substring(istart, iend).trim();
    	String suffix = record.substring(iend);
    	
    	// build the result
    	StringBuilder sb = new StringBuilder();
    	// add prefix
    	sb.append(prefix);
    	
    	// nothing to process
    	if("&nbsp".equals(toProcess) || "&nbsp;".equals(toProcess)){
    		return record;
    	} 	
   
    	//<i>Bkwd</i> BK 8695 8
    	//$1= REF TYPE DOCTYPE $2=BOOK $3=PAGE
    	//<i>Bkwd</i> RERECORD 200609010108470
    	//<i>Bkwd</i> UCCREF E102456
    	//$1= REF TYPE DOCTYPE $2=INSTRUM
    	//Search for book and page
    	//Search for instrument
    	
    	//process and add each record to output 
    	toProcess = toProcess.replaceAll("</?nobr>","");
    	String [] parts = toProcess.split(",");
    	boolean first = true;
    	for(String part : parts){
    		if(!first){ sb.append(","); } first = false;    
    		
			// re-write part according to rules, using only RE, no indices
			// for 'part' there can be only one type match: (book, page) or instrument
    		Matcher bpMatcher = bpPattern.matcher(part);
    		Matcher hrefMatcher = hrefPattern.matcher(part);
			int ahref = -1;
			if(hrefMatcher.find())
			{
				String ahref_str = hrefMatcher.group(0);
				ahref = part.indexOf(ahref_str);
			}
			
			if(bpMatcher.find())
			{
				String raw_str = bpMatcher.group(0);
				String ltype = bpMatcher.group(1);
				String book = bpMatcher.group(2);
				String page = bpMatcher.group(3);
				
				if (ahref==-1 || (ahref>part.indexOf(raw_str)) )
				{
					int end_idx =  part.indexOf(raw_str)+raw_str.length();
					String end = part.substring(end_idx);
					part = ltype+GetLinkBookPage(book, page, book + " " + page)+end;
				}
			}

			Matcher iMatcher = iPattern.matcher(part);
    		if(iMatcher.find())
    		{
				String raw_str = iMatcher.group(0);
				String ltype = iMatcher.group(1);
				String instrum = iMatcher.group(2);

				if (ahref==-1 || (ahref>part.indexOf(raw_str)) )
				{
					int end_idx = part.indexOf(raw_str)+raw_str.length();
					String end = part.substring(end_idx);
					part = ltype+GetLinkInstrument(instrum)+end;
				}
			}
    		sb.append("<nobr>" + part + "</nobr>");
    	}//    	sb.append(toProcess);
    	
    	// add suffix
    	sb.append(suffix);
    	
    	return sb.toString();
    }

    
    protected void ParseResponse(String sAction, ServerResponse Response,
            int viParseID) throws ServerResponseException {
        
        int iTmp;
        String rsResponce = Response.getResult();
        String sFileLink = null;

        switch (viParseID) {
        case ID_SEARCH_BY_NAME:
        case ID_SEARCH_BY_PARCEL:
        case ID_SAVE_TO_TSD:
        case ID_SEARCH_BY_INSTRUMENT_NO:
        case ID_SEARCH_BY_SUBDIVISION_NAME:
            
            if (rsResponce.indexOf("THE MINIMUM SEARCH CRITERIA WAS NOT PROVIDED") >= 0) {
                return;
            }
            if (rsResponce.indexOf("NO RECORDS RETRIEVED") >= 0) {
                return;
            }
            if (rsResponce.indexOf("YOU CAN NOW VIEW PAGES FROM BOOK") >= 0) {
                return;
            }
            if (rsResponce.indexOf("Error Retrieving Detail Data") >= 0) {
                return;
            }
            if (rsResponce.indexOf("Search Criteria") >= 0) { //response not yet processed by us

                rsResponce = rsResponce.replaceAll("\\r\\n", "");
                //Apartments Condominiums
                rsResponce = rsResponce.replaceAll("(?i)Apartments|Condominiums", "");
                //cut all until the first hr
                int i = rsResponce.indexOf("<hr>");
                if (i > -1)
                	rsResponce = rsResponce.substring(i);
                
                //kip all the information until the last table and kut all from here
                rsResponce = rsResponce.substring( 0, rsResponce.indexOf("<table border=1 width='100%'><tr><td><b>Search Criteria:</b>"));
                
                //replace file links with my one file links
                rsResponce.replaceAll("asp\\?", "asp&");
                rsResponce = rsResponce.replaceAll("<a href='", "<A HREF='");
                rsResponce = rsResponce.replaceAll("<A HREF='", "<A HREF='" + CreatePartialLink(TSConnectionURL.idGET) + "/");
                rsResponce = rsResponce.replaceAll("<A HREF='", "<a href='");
               
                //s-a introdus intermediate link pentru dispecerizare in bridgeConn
                rsResponce = rsResponce.replaceAll("simplequery.asp\\?", "simplequery.asp&intermediateLink=true&");
                rsResponce = rsResponce.replaceAll("<a href=(.*?)'(.*?)'>", "<a href='$1$2'>");

                if( Response.getRawQuerry().indexOf( "Names+Summary" ) >= 0 )
                {
                    //automatic name search --> first the name summary list is retrieved
                    String querry = Response.getQuerry();
                    
                    querry = querry.replaceAll( "Names Summary", "Detail Data" );
                    if ((iTmp = rsResponce.indexOf("<form")) != -1)
                    {
                        //logger.debug(" am gasit form!!!");
                        int endIdx = rsResponce.indexOf("/form>", iTmp) + 6;
                        rsResponce = rsResponce.substring(iTmp, endIdx);
                        
                        rsResponce = rsResponce.replaceAll( "<input TYPE=\"checkbox\" NAME=\"Names\" Value=\"(.*?)\">", "<a href='" + CreatePartialLink(TSConnectionURL.idPOST) + sAction + "&" + querry + "&Names=$1&automaticNameSearch=true'>View</a>" );                        
                    }
                }
                else
                {
                    if ((iTmp = rsResponce.indexOf("<form")) != -1) {
    
                        int endIdx = rsResponce.indexOf("/form>", iTmp) + 6;
                        String sForm = rsResponce.substring(iTmp, endIdx);
    
                        List links = GetRequestSettings(sForm, true);
    
                        String replacer = "";
                        int iActionType = TSConnectionURL.idPOST;
                        if (links.size() == 0) {
                            ;
                        } else if (links.size() == 1) {
                            String name = "";
                            String link = (String) links.get(0);
    
                            //logger.debug("link =" + link);
    
                            Matcher m = Pattern.compile("Detail Data \\d*-\\?")
                                    .matcher(link);
                            if (m.find()) {
                                name = "Next";
                            } else {
                                name = "Previous";
                            }
    
                            //logger.debug("name =" + name);
                            replacer += CreateLink(name, link, iActionType);
                            
                        } else {
                            replacer += CreateLink("Previous", ((String) links.get(0)), iActionType);
                            replacer += CreateLink("Next", ((String) links.get(1)), iActionType);
                        }
    
                        //logger.debug("replacer =" + replacer);
    
                        rsResponce = rsResponce.substring(0, iTmp) + replacer + rsResponce.substring(endIdx);
                    }
                }
            }

            if (viParseID == ID_SAVE_TO_TSD) {

                //String sInstrumentNo = getInstrNoFromResponse(rsResponce);
                String sRealInstrumentNo = getRealInstrNoFromResponse(rsResponce);
                
                logger.info("Instrument NO:" + sRealInstrumentNo);

                msSaveToTSDFileName = sRealInstrumentNo + ".html";
                
                // ca sa nu mai intre in bucla la crossreferinte
                mSearch.addRODoc(sRealInstrumentNo + ".html");
                
                Response.getParsedResponse().setFileName(
                        getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD(true);

                //download view file if any;
                if (rsResponce.indexOf("No Image") == -1) {
                    //logger.debug(" sFileLink = " + sFileLink );
                    sFileLink = sRealInstrumentNo + ".tif";

                    int iTmp1 = rsResponce.indexOf("/title-search");
                    String imglink = rsResponce.substring(iTmp1, rsResponce.indexOf("'", iTmp1));
                    imglink = imglink.replaceAll("asp\\?", "asp\\&");

                    ImageLinkInPage imageLinkInPage = new ImageLinkInPage(imglink, sFileLink);
                    Response.getParsedResponse().addImageLink(imageLinkInPage);
                    imageLinkInPage.setFakeLink(true); // in cazul ca am pe server link de imagine, dar la click pe el am mesaj de eroare
                }
                ParsedResponse pr = Response.getParsedResponse();
                //save any coss ref link before removing it
                rsResponce = rsResponce.replaceAll("asp\\?", "asp\\&");
                parser.Parse(pr, rsResponce, Parser.PAGE_DETAILS,
                        getLinkPrefix(TSConnectionURL.idGET),
                        TSServer.REQUEST_SAVE_TO_TSD);

                //	removing "Marginal" link
                if (rsResponce.indexOf("Display Doc") != -1)
                    rsResponce = rsResponce.replaceFirst("<a href", "<A HREF"); // pt 
                rsResponce = rsResponce.replaceAll("<a.*?>(.*?)</a>", "$1");
                pr.setOnlyResponse(rsResponce);

            } else { 
                // not saving to TSD

                if( Response.getRawQuerry().indexOf( "Names+Summary" ) < 0 )
                {
                    List items = new ArrayList();
                    boolean specialCase = false;
                    String prevLink = "";
                    String nextLink = "";
                    try {
                        DavidsonSplit ds = new DavidsonSplit();
                        ds.setDoc(rsResponce);
    
                        int count = ds.getSplitNo();
                        logger.debug("found count =" + count + " results");
                        prevLink = ds.getPrevLink();
                        nextLink = ds.getNextLink();
                        logger.debug("prev Link =" + prevLink);
                        logger.debug("next Link =" + nextLink);
                        if (count == 0) { // special case: when there is only one document per page
                            //the splitter returns 0 items, this case should be treated differently
                            //logger.debug("zero results found, special treatment");
                            items.add(nextLink);
                            specialCase = true;
                        } else {
                        	String record = "";
                        	String procRecord = "";

                        	for (int i = 0; i < count; i++) 
                        	{
                            	record = ds.getSplitDoc(i);
                            	try
                            	{
                            		procRecord = addMissingLinks(record);
                            	}
                            	catch (Exception e) {
                                    e.printStackTrace();
                                    logger.error("AddMissingLinksError:", e);
                                }

                                items.add(procRecord);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("JohnsonSplitterError:", e);
                    }
                    
                    String result = new String();
                    Vector parsedRows = new Vector();
    
                    for (Iterator iter = items.iterator(); iter.hasNext();) {
                        
                    	String item = (String) iter.next();
                        item = item.replaceAll("asp\\?", "asp\\&");
                        String initialResponse = item;
    
                        //String sInstrumentNo = getInstrNoFromResponse(item);
                        String sRealInstrumentNo = getRealInstrNoFromResponse(item);
    
                        String originalLink = getOriginalLink(sRealInstrumentNo);
                        String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
    
                        HashMap<String, String> data = new HashMap<String, String>();
						data.put("type", getDoctypeFromResponse(item));
						
						if (isInstrumentSaved(sRealInstrumentNo, null, data)  &&
                                !isFromOR(mSearch.getSearchDir()+ getServerTypeDirectory()+ sRealInstrumentNo + ".html")){
                            
							item += CreateFileAlreadyInTSD(true) + "<br>";
                        } else {
                            //special case
                            if (specialCase) {
                                if (item.toLowerCase().indexOf("</table") == -1) {
                                    int i2 = item.indexOf("Display Doc");
                                    int i3 = item.indexOf("</a>", i2);
                                    if (i3 == -1)
                                        i3 = item.indexOf("</A>", i2);
                                    item = new StringBuffer(item).insert(i3 + 4,
                                            "</td></tr></table>").toString();
                                }
                            }
    
                            //item = addSaveToTsdButton(item, sSave2TSDLink, true);
                            item = item.replaceFirst("</TR></Table>", 
                            		"</TR><TR><TD COLSPAN='100'><input type='checkbox' name='docLink' value='" 
                            		+ sSave2TSDLink + "'>Select for saving to TS Report</TD></TR><TR><TD COLSPAN='100'><hr></TD></TR></Table>");
    
                            mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
                        }
                        
                        ParsedResponse pr = new ParsedResponse();
                        pr.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
                        //logger.debug("item2="+item);
                        parser.Parse(pr, item, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
                        parsedRows.add(pr);
                        result += item;
                    }
                    rsResponce = result;
    
                    if (mSearch.getSearchType() == Search.PARENT_SITE_SEARCH) {
                    	
        	            // add form to result
                    	Response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + 
                    			SELECT_ALL_CHECKBOXES + "<font color='#08088A'><b>Select all documents</b></font>");
                        
                        if (!prevLink.equals("")) {
                            ParsedResponse pr = new ParsedResponse();
                            parser.Parse(pr, prevLink + "&nbsp;&nbsp;&nbsp;&nbsp;", Parser.NO_PARSE);
                            parsedRows.add(pr);
                        }
    
                        if (!nextLink.equals("")) {
                            ParsedResponse pr1 = new ParsedResponse();
                            parser.Parse(pr1, nextLink + "&nbsp;&nbsp;&nbsp;", Parser.NO_PARSE);
                            parsedRows.add(pr1);
                        }
                        
                        // add form end to result
                        Response.getParsedResponse().setFooter(CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1));
                    }
    
                    parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);
                    logger.debug("am gasint rezultate = " + parsedRows.size());
                    Response.getParsedResponse().setResultRows(parsedRows);
                    Response.getParsedResponse().setNextLink(nextLink);
                }
                else
                {
                    rsResponce = rsResponce.replaceAll( "(?i)</?form.*?>", "" );
                    rsResponce = rsResponce.replaceAll( "(?i)<input.*?>", "" );
                    rsResponce = rsResponce.replaceAll( "(?i)<br>", "" );
                    iTmp = rsResponce.indexOf( "<p>" );
                    if(iTmp >= 0)
                    {
                        rsResponce = rsResponce.substring( iTmp + 3 );
                    }
                    
                    rsResponce = rsResponce.replaceAll( "(<a href=.*?>.*?</a>)([^<]*)", "<tr><td>$1</td><td>$2</td></tr>" );
                    rsResponce = "<table>" + rsResponce + "</table>";
                    
                    parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_ROWS_NAME, getLinkPrefix(TSConnectionURL.idPOST), TSServer.REQUEST_GO_TO_LINK_REC);
                    /*
                    for( int i = 0 ; i < Response.getParsedResponse().getResultRows().size() ; i ++ )
                    {
                        ParsedResponse pr = (ParsedResponse) Response.getParsedResponse().getResultRows().get(i);
                        LinkInPage linkObj = pr.getPageLink();
                    }*/
                }
            }
            break;
        case ID_BROWSE_SCANNED_INDEX_PAGES:
        	if (rsResponce.contains("IBT")) {
        		
	        	rsResponce = rsResponce.replaceAll("(?is)<script.*?</script>", "");
	        	rsResponce = rsResponce.replaceAll("onclick='IPRCLK\\(\\)'", "");
	        	//rsResponce = rsResponce.replaceAll("onclick='PGCLK\\(\\)'", "");
	        	rsResponce = rsResponce.replaceAll("<html>\\s*<body>", "<body>\n"
	                    + "<script language='javascript'>\n"
	                    + "function IBT() {\n" 
	        			+ " 	var frmNdx = document.frmIndexes;\n"
	        			+ " 	var i = frmNdx.IndexBookType.selectedIndex;\n"
	        			+ " 	var stri = \"&IndexBookType=\" + frmNdx.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	        			+ " 	var k = frmNdx.searchId.value;\n"
	        			+ " 	var strk = \"&searchId=\" + k;\n"
	                    + " 	var strURL = \"URLConnectionReader?p1=070&p2=1\";\n"
	                    //+ " 	alert(\"Opening \" + strURL + strk + \"&ActionType=2&Link=/SPVIndexSearch.asp\" + stri)\n"
	        			+ " 	window.location.href = strURL + strk + \"&ActionType=2&Link=/SPVIndexSearch.asp\" + stri;\n" 
	        			+ "} \n" 
	        	
	                    +"function IDR() {\n" 	
	    				+ " 	var frmNdx = document.frmIndexes;\n"
	        			+ " 	var i = frmNdx.IndexBookType.selectedIndex;\n"
	        			+"  	var j = frmNdx.IndexDateRange.selectedIndex;\n"
	        			+ " 	var stri = \"&IndexBookType=\" + frmNdx.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	        			+"  	var strj = \"&IndexDateRange=\" + frmNdx.IndexDateRange.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	        			+ " 	var k = frmNdx.searchId.value;\n"
	        			+ " 	var strk = \"&searchId=\" + k;\n"
	                    + " 	var strURL = \"URLConnectionReader?p1=070&p2=1\";\n"
	                    //+ " 	alert(\"Opening \" + strURL + strk + \"&ActionType=2&Link=/SPVIndexSearch.asp\" + stri + strj)\n"
	        			+ " 	window.location.href = strURL + strk + \"&ActionType=2&Link=/SPVIndexSearch.asp\" + stri + strj ;\n" 
	                    +"}\n"
	                   
	                   +"function IAR() {\n" 
	                    + "	var frmNdx = document.frmIndexes;\n"
	                    + " 	var i = frmNdx.IndexBookType.selectedIndex;\n"
	        			+"  	var j = frmNdx.IndexDateRange.selectedIndex;\n"
	        			+" 	var m = frmNdx.IndexAlphaRange.selectedIndex;\n"
	        			+ " 	var k = frmNdx.searchId.value;\n"
	        			+ " 	var stri = \"&IndexBookType=\" + frmNdx.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	        			+"  	var strj = \"&IndexDateRange=\" + frmNdx.IndexDateRange.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	        			+" 	var strm = \"&IndexAlphaRange=\" + frmNdx.IndexAlphaRange.options[m].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	                    + "	var strk = \"&searchId=\" + k;\n"
	                    + "	var strURL = \"URLConnectionReader?p1=070&p2=1\";\n"
	                    // + "alert(window.document.forms[0].Page.options[window.document.forms[0].Page.selectedIndex].text);\n"
	                    + " window.location.href = strURL + strk + \"&ActionType=2&Link=/SPVIndexSearch.asp\" + stri + strj + strm ;\n"			
	                    +"}\n" 
	                    
	                    +"function IPR() {\n" 
	                    + "	var frmNdx = document.frmIndexes;\n"
	                    + " 	var i = frmNdx.IndexBookType.selectedIndex;\n"
	        			+"  	var j = frmNdx.IndexDateRange.selectedIndex;\n"
	        			+" 	var m = frmNdx.IndexAlphaRange.selectedIndex;\n"
	        			+" 	var l = frmNdx.IndexPageRange.selectedIndex;\n"
	        			+ " 	var k = frmNdx.searchId.value;\n"
	        			+ " 	var stri = \"&IndexBookType=\" + frmNdx.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	        			+"  	var strj = \"&DateRange=\" + frmNdx.IndexDateRange.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	        			+" 	var strm = \"&AlphaRange=\" + frmNdx.IndexAlphaRange.options[m].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	        			+ " 	var strl = \"&IndexPage=\" + frmNdx.IndexPageRange.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	                    + "	var strk = \"&searchId=\" + k;\n"
	                    + "	var strURL = \"URLConnectionReader?p1=070&p2=1\";\n"
	                    // + "alert(window.document.forms[0].Page.options[window.document.forms[0].Page.selectedIndex].text);\n"
	                    //+ "	open(strURL + strk + \"&ActionType=2&Link=/LoadImage.asp\" + stri + strj + strm + strl);\n"		
	                    +"}\n" 
	                    
	                    +"function ViewDoc() {\n" 
	                    + "	var rPageSelect = document.frmIndexes.IndexPageRange.selectedIndex;\n"
	                    + "if (rPageSelect == -1) {\n"
	                    + "    alert(\"Cannot view! No page selected!\");\n"
	                    + "    }\n"
	                    + "	else {\n"
	                    + "		var frmNdx = document.frmIndexes;\n"
	                    + " 		var i = frmNdx.IndexBookType.selectedIndex;\n"
	        			+"  		var j = frmNdx.IndexDateRange.selectedIndex;\n"
	        			+" 		var m = frmNdx.IndexAlphaRange.selectedIndex;\n"
	        			+" 		var l = frmNdx.IndexPageRange.selectedIndex;\n"
	        			+ " 		var k = frmNdx.searchId.value;\n"
	        			+ " 		var stri = \"&IndexBookType=\" + frmNdx.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	        			+"  		var strj = \"&DateRange=\" + frmNdx.IndexDateRange.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	        			+" 		var strm = \"&AlphaRange=\" + frmNdx.IndexAlphaRange.options[m].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	        			+ " 		var strl = \"&IndexPage=\" + frmNdx.IndexPageRange.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
	                    + "		var strk = \"&searchId=\" + k;\n"
	                    + "		var strURL = \"URLConnectionReader?p1=070&p2=1\";\n"
	                    // + "alert(window.document.forms[0].Page.options[window.document.forms[0].Page.selectedIndex].text);\n"
	                    + "	open(strURL + strk + \"&ActionType=2&Link=/LoadImage.asp\" + stri + strj + strm + strl);\n"		
	                    +"}\n" 
	                    +"}\n"
	                    
	                   +"function iSaveTSD() {\n" 
	                    + "	var rPageSelect = document.frmIndexes.IndexPageRange.selectedIndex;\n"
	                    + "	if (rPageSelect == -1) {\n"
	                    + "    	alert(\"Cannot save! No page selected!\");\n"
	                    + "    }\n"
	                    + "	else {\n"
	                    + "    	document.frmIndexes.realPage.value=document.frmIndexes.IndexPageRange.options[rPageSelect].text; document.frmIndexes.submit();"
	                    + "    }\n"
	                    + "}\n"                         
	                    + "</script>");
	        	
	        	rsResponce = rsResponce.replaceAll("action='SPVIndexSearch\\.asp'", "action='MultiDocSave'");
	        	rsResponce = rsResponce.replaceAll("</form></body></html>", "") + "<br></br><input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL + "\" onClick=\"iSaveTSD();\">" 
	        		+ "&nbsp;&nbsp;<input name=\"Button\" type=\"button\" class=\"button\" value=\"View\" onClick=\"ViewDoc();\">"
	        		+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId +"\">"
		 			+ "<input type=\"hidden\" name=\"realPage\" />" 
		 			+ "<input type=\"hidden\" name=\"serverId\" value=\"70\"/> \n</form>";
    	} else {
		        	rsResponce = rsResponce.replaceAll("(?is)<script.*?</script>", "");
		        	rsResponce = rsResponce.replaceAll("onclick='PGCLK\\(\\)'", "");
		        	rsResponce = rsResponce.replaceAll("<html>\\s*<body>", "<body>\n"
		                    + "<script language='javascript'>\n"
		                    + "function BT() {\n" 
		        			+ " 	var frmNdx = document.frmBooks;\n"
		        			+ " 	var i = frmNdx.BookType.selectedIndex;\n"
		        			+ " 	var stri = \"&BookType=\" + frmNdx.BookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
		        			+ " 	var k = frmNdx.searchId.value;\n"
		        			+ " 	var strk = \"&searchId=\" + k;\n"
		                    + " 	var strURL = \"URLConnectionReader?p1=070&p2=1\";\n"
		                    //+ " 	alert(\"Opening \" + strURL + strk + \"&ActionType=2&Link=/SPVBookSearch.asp\" + stri)\n"
		        			+ " 	window.location.href = strURL + strk + \"&ActionType=2&Link=/SPVBookSearch.asp\" + stri;\n" 
		        			+ "} \n" 
		        	
		                    +"function BK() {\n" 
		    				+ " 	var frmNdx = document.frmBooks;\n"
		        			+ " 	var i = frmNdx.BookType.selectedIndex;\n"
		        			+"  	var j = frmNdx.Book.selectedIndex;\n"
		        			+ " 	var stri = \"&BookType=\" + frmNdx.BookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
		        			+"  	var strj = \"&Book=\" + frmNdx.Book.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
		        			+ " 	var k = frmNdx.searchId.value;\n"
		        			+ " 	var strk = \"&searchId=\" + k;\n"
		                    + " 	var strURL = \"URLConnectionReader?p1=070&p2=1\";\n"
		                    //+ " 	alert(\"Opening \" + strURL + strk + \"&ActionType=2&Link=/SPVBookSearch.asp\" + stri + strj)\n"
		        			+ " 	window.location.href = strURL + strk + \"&ActionType=2&Link=/SPVBookSearch.asp\" + stri + strj ;\n" 
		                    +"}\n"
		                   +"function PG() {\n" 
		                    + "	var frmNdx = document.frmBooks;\n"
		                    + " 	var i = frmNdx.BookType.selectedIndex;\n"
		        			+"  	var j = frmNdx.Book.selectedIndex;\n"
		        			+" 	var l = frmNdx.Page.selectedIndex;\n"
		        			+ " 	var k = frmNdx.searchId.value;\n"
		        			+ " 	var stri = \"&BookType=\" + frmNdx.BookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
		        			+"  	var strj = \"&Book=\" + frmNdx.Book.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
		        			+" 	var strl = \"&Page=\" + frmNdx.Page.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
		                    + "	var strk = \"&searchId=\" + k;\n"
		                    + "	var strURL = \"URLConnectionReader?p1=070&p2=1\";\n"
		                    //+ " 	alert(\"Opening \" + strURL + strk + \"&ActionType=2&Link=/SPVBookSearch.asp\" + stri + strj + strl)\n"
		                    //+ " open(strURL + strk + \"&ActionType=2&Link=/LoadImage.asp\" + stri + strj + strl) ;\n"
		                    +"}\n" 
		                    
		                    +"function ViewDoc() {\n" 
		                    + "	var rPageSelect = document.frmBooks.Page.selectedIndex;\n"
		                    + "if (rPageSelect == -1) {\n"
		                    + "    alert(\"Cannot view! No page selected!\");\n"
		                    + "    }\n"
		                    + "	else {\n"
		                    + "		var frmNdx = document.frmBooks;\n"
		                    + " 		var i = frmNdx.BookType.selectedIndex;\n"
		        			+"  		var j = frmNdx.Book.selectedIndex;\n"
		        			+" 		var l = frmNdx.Page.selectedIndex;\n"
		        			+ " 		var k = frmNdx.searchId.value;\n"
		        			+ " 		var stri = \"&BookType=\" + frmNdx.BookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
		        			+"  		var strj = \"&Book=\" + frmNdx.Book.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
		        			+" 		var strl = \"&Page=\" + frmNdx.Page.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
		                    + "		var strk = \"&searchId=\" + k;\n"
		                    + "		var strURL = \"URLConnectionReader?p1=070&p2=1\";\n"
		                    //+ " 	alert(\"Opening \" + strURL + strk + \"&ActionType=2&Link=/SPVBookSearch.asp\" + stri + strj + strl)\n"
		                    + " 	open(strURL + strk + \"&ActionType=2&Link=/LoadImage.asp\" + stri + strj + strl) ;\n"
		                    + "}\n"           
		                    + "}\n"
		
		                    +"function SaveTSD() {\n" 
		                    + "var rPageSelect = document.frmBooks.Page.selectedIndex;\n"
		                    
		                    + "if (rPageSelect == -1) {\n"
		                    + "    alert(\"Cannot save! No page selected!\");\n"
		                    + "    }\n"
		                    + "else {\n"
		                    + "    document.frmBooks.realPage.value=document.frmBooks.Page.options[rPageSelect].text; document.frmBooks.submit();"
		                    + "    }\n"
		                    + "}\n"                         
		                    + "</script>");
		        	//if (rsResponce.contains("SPVBookSearch")) {
		        		
			        	 rsResponce = rsResponce.replaceAll("action='SPVBookSearch\\.asp'", "action='MultiDocSave'");
			             rsResponce = rsResponce.replaceAll("</form></body></html>", "") + "<br>" /* +</br><input name=\"Button\" type=\"button\" class=\"button\" value=\"Save To TSRI\" onClick=\"SaveTSD();\">"*/ 
			             + "&nbsp;&nbsp;<input name=\"Button\" type=\"button\" class=\"button\" value=\"View\" onClick=\"ViewDoc();\">"
			             + "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId +"\">"
			 			+ "<input type=\"hidden\" name=\"realPage\" />" 
			 			+ "<input type=\"hidden\" name=\"serverId\" value=\"70\"/> \n</form>"; 
    	/*} else {
    		rsResponce = rsResponce.replaceAll("action='SPVIndexSearch\\.asp'", "action='MultiDocSave'");
        	rsResponce = rsResponce.replaceAll("</form></body></html>", "") + "<br></br><input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL + "\" onClick=\"iSaveTSD();\">" 
	 			+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId +"\">"
	 			+ "<input type=\"hidden\" name=\"realPage\" />" 
	 			+ "<input type=\"hidden\" name=\"serverId\" value=\"70\"/> \n</form>";*/
    	}
        		
             parser.Parse(Response.getParsedResponse(), rsResponce,
                     Parser.NO_PARSE);
        	break;
        	
        case ID_GET_LINK:
        	 if(sAction.indexOf("SPVIndexSearch.asp") != -1){
                 ParseResponse(sAction, Response, ID_BROWSE_SCANNED_INDEX_PAGES);
             }
        	 else if(sAction.indexOf("SPVBookSearch.asp") != -1){
                 ParseResponse(sAction, Response, ID_BROWSE_SCANNED_INDEX_PAGES);
             }
             else if( Response.getQuerry().indexOf( "automaticNameSearch" ) >= 0 ){
                ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
            }
            else{
                ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);
            }
            break;
        default:
            break;
        }

    }
    private String getInstrNoFromResponse(String sTmp) {
        
        String sInstrumentNo = "";
        
        
        Matcher ma = Pattern.compile("DocId:(.*?)</TD>").matcher(sTmp);
        if (ma.find()) 
        {
            sInstrumentNo = ma.group(1).trim();
        }
        else
        {
            int iTmp = sTmp.indexOf("Instrument");
            iTmp = sTmp.indexOf("/B>", iTmp);
            iTmp = sTmp.indexOf(">", iTmp) + 1;
            sInstrumentNo = sTmp.substring(iTmp, sTmp.indexOf("<", iTmp))
                    .trim();
        }
        return sInstrumentNo;
    }
    private String getRealInstrNoFromResponse(String sTmp) {
        
        String sInstrumentNo = "";
       
            int iTmp = sTmp.indexOf("Instrument");
            iTmp = sTmp.indexOf("/B>", iTmp);
            iTmp = sTmp.indexOf(">", iTmp) + 1;
            sInstrumentNo = sTmp.substring(iTmp, sTmp.indexOf("<", iTmp))
                    .trim();
       
        return sInstrumentNo;
    }
    
    private String getDoctypeFromResponse(String sTmp){
        
    	String doctype = "";
       
        int iTmp = sTmp.indexOf("Document Type");
        iTmp = sTmp.indexOf("/B>", iTmp);
        iTmp = sTmp.indexOf(">", iTmp) + 1;
        doctype = sTmp.substring(iTmp, sTmp.indexOf("<", iTmp)).trim();
       
        return doctype;
    }
    
    private String getOriginalLink(String InstrumentNo) {

        return "/SimpleQuery.asp&Instrs=" + InstrumentNo;
    }
    
    protected DownloadImageResult saveImage(ImageLinkInPage image)
    throws ServerResponseException {
		getTSConnection().SetReferer(FILE_REFERER);
		DownloadImageResult res = super.saveImage(image);
		getTSConnection().SetReferer("");
		return res;
	}
    
    
    public static void main(String[] args) {
        
        /*HashMap conparams = null, reqprops = null;
        ATSConn c;

        conparams = new HashMap();
        reqprops = new HashMap();
        
        reqprops.put("Accept","image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, /*");
        reqprops.put("Accept-Encoding", "gzip, deflate");
        reqprops.put("Accept-Language","en-us");
        reqprops.put("Connection","Keep-Alive");
        reqprops.put("Host", "recorder.jocogov.org");
        reqprops.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");

        
        // first page
        String link = "http://recorder.jocogov.org/";        
		c = new ATSConn(666, link, ATSConnConstants.GET, null, reqprops,searchId);
		c.setFollowRedirects(false);
		c.doConnection();
		CookieMgr.addCookie("[KSJohnsonRO] ", c.getResultHeaders().get("Set-Cookie"));
        /////////////////////////////////////////////////////////////////////////////////////////////////
        
		
        /// login
        conparams.put("DTSUser", "STEWART");
        conparams.put("DTSPassword", "STEWART1");

        link = "http://recorder.jocogov.org/Logon.asp";
        
        reqprops.put("Referer", "http://recorder.jocogov.org/");
        reqprops.put("Cookie", CookieMgr.getCookie("[KSJohnsonRO] "));
        
        c = new ATSConn(666, link, ATSConnConstants.POST, conparams, reqprops);
        c.setFollowRedirects(false);
        c.doConnection();
        CookieMgr.addCookie("[KSJohnsonRO] ", c.getResultHeaders().get("Set-Cookie"));
        /////////////////////////////////////////////////////////////////////////////////////////////////
        
        
        // redirectarea dupa login
        int cc = c.getReturnCode();
        if (cc == 302) {
            
            link = "http://recorder.jocogov.org/default.asp";
            
            reqprops.put("Referer", "http://recorder.jocogov.org/");
            reqprops.put("Cookie", CookieMgr.getCookie("[KSJohnsonRO] "));
            
            c = new ATSConn(666, link, ATSConnConstants.GET, null, reqprops);
            c.setFollowRedirects(false);
            c.doConnection();
            CookieMgr.addCookie("[KSJohnsonRO] ", c.getResultHeaders().get("Set-Cookie"));
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////

        //pagina de cautare
        link = "http://recorder.jocogov.org/Navigate.asp?AdvancedSearch.x=71&AdvancedSearch.y=8";
        
        reqprops.put("Cookie", CookieMgr.getCookie("[KSJohnsonRO] "));
        reqprops.put("Referer", "http://recorder.jocogov.org/default.asp");
        
        c = new ATSConn(666, link, ATSConnConstants.GET, null, reqprops);
        c.setUsefastlink(false);
        c.setFollowRedirects(false);
        c.doConnection();
        CookieMgr.addCookie("[KSJohnsonRO] ", c.getResultHeaders().get("Set-Cookie"));
        /////////////////////////////////////////////////////////////////////////////////////////////////
     
        for (int i = 0; i < 5; i++) {
	        // facem o cautare
	        link = "http://recorder.jocogov.org/SimpleQuery.asp";
	        
	        reqprops.put("Cookie", CookieMgr.getCookie("[KSJohnsonRO] "));
	        reqprops.put("Referer", "http://recorder.jocogov.org/Navigate.asp?AdvancedSearch.x=71&AdvancedSearch.y=8");
	
	        conparams = new HashMap();
			conparams.put("CmpLastName",i%2==0?"LIKE":"IS");
			conparams.put("LastName","123" + i);
			conparams.put("CmpFirstName","LIKE");
			conparams.put("FirstName","");
			conparams.put("FREEFORM1","Subdivision");
			conparams.put("CmpLegal1","LIKE");
			conparams.put("Legal1","");
			conparams.put("FREEFORM2","Block");
			conparams.put("CmpLegal2","IS");
			conparams.put("Legal2","");
			conparams.put("FREEFORM3","LotNumBegin");
			conparams.put("CmpLegal3","IS");
			conparams.put("Legal3","");
			conparams.put("FREEFORM4","ParcelNum");
			conparams.put("CmpLegal4","IS");
			conparams.put("Legal4","");
			conparams.put("FREEFORM5","MedLegal1");
			conparams.put("CmpLegal5","IS");
			conparams.put("Legal5","");
			conparams.put("StartMonth","Oct");
			conparams.put("StartDay","10");
			conparams.put("StartYear","1980");
			conparams.put("EndMonth","May");
			conparams.put("EndDay","25");
			conparams.put("EndYear","2005");
			conparams.put("SortDir","DESC");
			conparams.put("SUBMIT","Detail Data");
			
			conparams.put("EndDate","May 6 2005");
			conparams.put("StartDate","Jan 1 1810");
	        
	        c = new ATSConn(666, link, ATSConnConstants.POST, conparams, reqprops);
	        c.setUsefastlink(false);
	        c.setFollowRedirects(false);
	        c.doConnection();
	        CookieMgr.addCookie("[KSJohnsonRO] ", c.getResultHeaders().get("Set-Cookie"));
	        
	        c.getResult(); 
	        System.err.println("Intoarce : " + c.getReturnCode());
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////
        
        // download imagine
        link = "http://recorder.jocogov.org/LoadImage.asp?200301210746987";

        reqprops.put("Cookie", CookieMgr.getCookie("[KSJohnsonRO] "));
        reqprops.put("Referer", "http://recorder.jocogov.org/SimpleQuery.asp");
        
        c = new ATSConn(666, link, ATSConnConstants.GET, null, reqprops);
        c.setFollowRedirects(false);
        c.doConnection();
        CookieMgr.addCookie("[KSJohnsonRO] ", c.getResultHeaders().get("Set-Cookie"));
        
        
        // redirectarea pentru download-ul imaginii
        cc = c.getReturnCode();
        if (cc == 302) {
            
            for (int i = 0; i < 3; i++) {
            
	            link = "http://recorder.jocogov.org/LoadImage2.tif?200504220008694";
	            
	            reqprops.put("Cookie", CookieMgr.getCookie("[KSJohnsonRO] "));
	            reqprops.put("Referal", "http://recorder.jocogov.org/SimpleQuery.asp");
	            
	            c = new ATSConn(666, link, ATSConnConstants.GET, null, reqprops);
	            c.setFollowRedirects(false);
	            c.doConnection();
	            CookieMgr.addCookie("[KSJohnsonRO] ", c.getResultHeaders().get("Set-Cookie"));
	            
	            if (c.getResultMimeType().equals("image/tiff")) {
	                
	                try {
	                    ((ByteArrayOutputStream)c.getResult()).writeTo(new FileOutputStream("c:\\image" + i + ".tiff"));
	                } catch (FileNotFoundException e) {
	                    e.printStackTrace();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	                
	                System.err.println("Am ajuns la imagine");
	            }
            }
        }
        
        System.err.println(CookieMgr.getCookie("[KSJohnsonRO] "));*/
    }
    
    private static String [] liensAndJudgementsDocTypes = {
		"ABST JUDGE",
		"AGIST LIEN",
		"ARTIS LIEN",
		"DPTJUST LI",
		"FED TX LIEN",
		"FTX",
		"FUEL TX LIEN",
		"KS EMP TX LIEN",
		"Lis",
		"LIS PENDENS",
		"MECH LIEN",
		"NOTICE LIE",
		"OTHER LIEN",
		"OTHER ST TX LIEN",
		"REL ART LIEN",
		"REL FED TX LIEN",
		"REL JUDGE",
		"REL LIS PENDENS",
		"REL NOTICE OTH LIEN",
		"REL OTHER LIEN",
		"REL ST TX",
		"REL WSTE WTRLIEN",
		"RELDEPTJST",
		"REV REL FED TX LIEN",
		"SEED%26BAIL LIEN",
		"THRESH LIEN",
		"TRUCK LIEN",
		"VET LIEN",
		"WFEDTAXLIE",
		"WSTE WTR LIEN"
    };
    
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
        
    	List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
        
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
		DocsValidator lastTransferDateValidator = (new LastTransferDateFilter(searchId)).getValidator();
		DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator(); 
        boolean validateWithDates = applyDateFilter();
		
        FilterResponse nameFilterCO = new MOGenericCaseNetCO.NameFilter(SearchAttributes.OWNER_OBJECT, searchId);
        
    	TSServerInfoModule m;
    	
    	boolean searchWithSubdivision = searchWithSubdivision();
    	String[] subdivisions = null;
    	if(searchWithSubdivision) {
    		subdivisions = getSubdivisions(getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME), searchId);
    	} else {
    		subdivisions = new String[0];
    		printSubdivisionException();
    	}
    	        
        // search by book and page list from Search Page - needed to be executed before bootstrapping any instrument # from RO (related to incident #36330)
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_SEARCH_PAGE_BP);
        m.setSaObjKey(SearchAttributes.LD_BOOKPAGE);
        m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_SEARCH);
        m.clearSaKey(0);
        m.clearSaKey(1);
        m.clearSaKey(2);
        m.setParamValue(5, "DESC");        
        m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
        m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
        m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( addressHighPassValidator );
		m.addCrossRefValidator( pinValidator );
		if (validateWithDates) {
			m.addValidator(recordedDateValidator);
			m.addCrossRefValidator(recordedDateValidator);
		}
        l.add(m);
        
        // search by instrument number list from Search Page - needed to be executed before bootstrapping any book&page # from RO (related to incident # 36330)
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_SEARCH_PAGE_INSTR);
		m.setSaObjKey(SearchAttributes.LD_INSTRNO);
		m.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_SEARCH);
		m.clearSaKey(0);
        m.clearSaKey(1);
        m.clearSaKey(2);
        m.setParamValue(5, "DESC");
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);

		m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( addressHighPassValidator );
		m.addCrossRefValidator( pinValidator );
		if (validateWithDates) {
			m.addValidator(recordedDateValidator);
			m.addCrossRefValidator(recordedDateValidator);
		}
		l.add(m);
       
        // search by subdivision

        for (String subdivision : subdivisions)
        {
        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
        	m.addValidator(addressHighPassValidator);
        	m.addValidator(defaultLegalValidator);
            m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
            		TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK);
            m.clearSaKey(0);
            m.clearSaKey(1);
            m.clearSaKey(7);
            m.clearSaKey(19);
            m.setParamValue(21, "DESC");                
            m.setData(7, subdivision);
            m.setSaKey(10, SearchAttributes.LD_SUBDIV_BLOCK);
            m.setSaKey(13, SearchAttributes.LD_LOTNO);
            m.addCrossRefValidator( defaultLegalValidator );
    		m.addCrossRefValidator( addressHighPassValidator );
    		m.addCrossRefValidator( pinValidator );
            if (validateWithDates) {
            	m.addValidator(recordedDateValidator);
    			m.addCrossRefValidator(recordedDateValidator);
    		}
            l.add(m);
            
            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
            m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
            		TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_PLAT);
            m.clearSaKeys();
            m.addValidator(addressHighPassValidator);
        	m.addValidator(defaultLegalValidator);
            m.setParamValue(0, subdivision);
            m.setParamValue(2, "LIKE");	//Last Name comparator
            m.setParamValue(20, "PLAT");
            m.clearSaKey(19);
            if (validateWithDates) {
            	m.forceValue(22, "Jan 1, 1960");
            } else {
            	m.setSaKey(22, SearchAttributes.START_HISTORY_DATE);
            }
            m.addCrossRefValidator( defaultLegalValidator );
    		m.addCrossRefValidator( addressHighPassValidator );
    		m.addCrossRefValidator( pinValidator );
            l.add(m);


            String[] searchDocTypes = { "ESTM", "RW", "DECL REST",
					"DISC ESTM" };
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			m.clearSaKeys();
			m.addValidator(addressHighPassValidator);
        	m.addValidator(defaultLegalValidator);
			m.setParamValue(0, subdivision);
			m.setParamValue(2, "LIKE"); // Last Name comparator
			m.forceValue(22, "Jan 1, 1960");
			m.addCrossRefValidator( defaultLegalValidator );
    		m.addCrossRefValidator( addressHighPassValidator );
    		m.addCrossRefValidator( pinValidator );

			for (String s : searchDocTypes) {
				int newFctId = m.addFunction();
				TSServerInfoFunction newFct = m.getFunction(newFctId);
				newFct.setParamName("DocTypes");
				newFct.setDefaultValue(s);
				newFct.setHiden(true);
			}
			l.add(m);
        }           
		
		ConfigurableNameIterator ownerNameIterator = null;

        if( hasOwner() ){
	        m = new TSServerInfoModule(serverInfo .getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	        
	        DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator(); 
	        
	        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	        		TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
	        m.clearSaKeys();
	        m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
	        m.addFilter( NameFilterFactory.getDefaultNameFilter(	SearchAttributes.OWNER_OBJECT, searchId, m) );
			m.addValidator( defaultLegalValidator );
			m.addValidator( addressHighPassValidator );
	        m.addValidator( pinValidator );
	        m.addValidator( new GranteeDoctypeFilterResponse(searchId).getValidator() );
	        m.addValidator( lastTransferDateValidator );
	        m.addValidator( recordedDateNameValidator );
	        addFilterForUpdate(m, false);
			m.addCrossRefValidator( defaultLegalValidator );
			m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( recordedDateNameValidator );
	        
	    	ownerNameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;F;"} );
			m.addIterator( ownerNameIterator ); 
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,	
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);

	        m.setParamValue(4, "Names Summary");
	        m.setDefaultValue(4, "Names Summary");
	        m.forceValue(4, "Names Summary");
	        m.setParamValue(21, "DESC");
            l.add(m);

            // bug #826: search for liens and judgements and use IGNORE_MI filter
            for (String subdivision : subdivisions)
            {
            	m = new TSServerInfoModule(serverInfo .getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
    	        recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator(); 
    	        //m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "dasodas11");
    	        m.addValidator(defaultLegalValidator);
    	        m.addFilter(nameFilterCO);
    	        //LF, LM
    	        m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
    	        
//    	        m.getFunction( 4 ).setParamValue("Names Summary");
//    	        m.getFunction( 4 ).setDefaultValue("Names Summary");
    	        m.getFunction( 7 ).setSaKey("");
    	        m.getFunction( 7 ).setParamValue(subdivision);
    	        m.getFunction( 10 ).setSaKey("");
    	        m.getFunction( 21 ).setParamValue("DESC");
    	        m.getFunction( 13 ).setSaKey("");
    	        m.getFunction( 19 ).setSaKey("");
    	        
    	        ConfigurableNameIterator it2 =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;F;", "L;M;"} );
    			m.addIterator( it2 );

    	        // request only liens and judgements
    	        for(String docType : liensAndJudgementsDocTypes){
    	        	int newFctId = m.addFunction();
    	        	TSServerInfoFunction newFct = m.getFunction(newFctId);
    	        	newFct.setParamName("DocTypes");
    	        	newFct.setDefaultValue(docType);
    	        	newFct.setHiden(true);
    	        }
                if (validateWithDates) {
        			m.addValidator(recordedDateNameValidator);
        			m.addCrossRefValidator(recordedDateNameValidator);
        		}
                m.addCrossRefValidator( pinValidator );
                l.add(m);
            }
	    }
		
        
        //search by crossRef book and page list from RO docs
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		m.clearSaKeys();
		m.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);		    		    
	    m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_NOT_AGAIN);
	    m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);		    
	    m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
	    m.getFunction(5).setParamValue("DESC");
	    m.addValidator(addressHighPassValidator);
    	m.addValidator(defaultLegalValidator);
    	m.addValidator(pinValidator);
    	m.addValidator(recordedDateValidator);
	    m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( addressHighPassValidator );
		m.addCrossRefValidator( pinValidator );
	    l.add(m);

		
		//search by crossRef instr# list from RO docs
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		m.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);		
		m.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_NOT_AGAIN);
		m.getFunction(0).setSaKey("");
        m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
        m.getFunction(1).setSaKey("");
        m.getFunction(2).setSaKey("");
        m.getFunction(5).setParamValue("DESC");
        m.addValidator( defaultLegalValidator );
		m.addValidator( addressHighPassValidator );
		m.addValidator( pinValidator );
        m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( addressHighPassValidator );
		m.addCrossRefValidator( pinValidator );
		l.add(m);
		
		// OCR last transfer
        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        		TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP_INST);
        m.setIteratorType(ModuleStatesIterator.TYPE_OCR);
        m.getFunction(0).setSaKey("");
        m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
        m.getFunction(1).setSaKey("");
        m.getFunction( 5 ).setParamValue("DESC");
        m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
        m.getFunction(2).setSaKey("");
        m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
        m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( addressHighPassValidator );
		m.addCrossRefValidator( pinValidator );
        l.add(m);
        
        //owner search if we bootstrap any from OCR
        m = new TSServerInfoModule(serverInfo .getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
        m.clearSaKeys();
        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,	
				TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
        m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
        m.addFilter( NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m, true));
        			
		m.addValidator( defaultLegalValidator );
		m.addValidator( addressHighPassValidator );
        m.addValidator( pinValidator );
        m.addValidator( new GranteeDoctypeFilterResponse(searchId).getValidator() );
        m.addValidator( lastTransferDateValidator );
        m.addValidator( recordedDateValidator );
		m.setStopAfterModule(true);
		m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( addressHighPassValidator );
        m.addCrossRefValidator( pinValidator );
        m.addCrossRefValidator( recordedDateValidator );
        
    	ArrayList<NameI> searchedNames = null;
		if(ownerNameIterator!=null)
			ownerNameIterator.getSearchedNames();
		else
			searchedNames = new ArrayList<NameI>();
		ownerNameIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator( m, searchId, false, new String[]{"L;F;"} );
		ownerNameIterator.setInitAgain( true );
		ownerNameIterator.setSearchedNames( searchedNames );
		
    	//ownerNameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;F;"} );
		m.addIterator( ownerNameIterator ); 
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,	
				TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
		/*
        m.setParamValue(4, "Names Summary");
        m.setDefaultValue(4, "Names Summary");
        m.forceValue(4, "Names Summary");
        */
        m.setParamValue(21, "DESC");
        
        l.add(m);        
        
        serverInfo.setModulesForAutoSearch(l);
    }

	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory
				.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,
				true, true).getValidator();
		DocsValidator addressHighPassValidator = AddressFilterFactory
				.getAddressHighPassFilter(searchId, 0.8d).getValidator();

		SearchAttributes sa = InstanceManager.getManager()
				.getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa
				.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			module.getFunction(4).setParamValue("Names Summary");
			module.getFunction(4).setDefaultValue("Names Summary");
			module.getFunction(21).setParamValue("DESC");
			module.addFilter(NameFilterFactory.getDefaultNameFilter(
					SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			String date = gbm.getDateForSearch(id, "MMM dd, yyyy", searchId);
			if (date != null)
				module.getFunction(22).forceValue(date);
			module.addValidator(defaultLegalValidator);
			module.addValidator(addressHighPassValidator);
			module.addValidator(pinValidator);
			module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(
					searchId, 0.90d, module).getValidator());
			module.addValidator(DateFilterFactory.getDateFilterForGoBack(
					SearchAttributes.GB_MANAGER_OBJECT, searchId, module)
					.getValidator());

			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;" });
			module.addIterator(nameIterator);
			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(
						serverInfo
								.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				module.getFunction(4).setParamValue("Names Summary");
				module.getFunction(4).setDefaultValue("Names Summary");
				module.getFunction(21).setParamValue("DESC");
				module.addFilter(NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				date = gbm.getDateForSearchBrokenChain(id, "MMM dd, yyyy",
						searchId);
				if (date != null)
					module.getFunction(22).forceValue(date);
				module.addValidator(defaultLegalValidator);
				module.addValidator(addressHighPassValidator);
				module.addValidator(pinValidator);
				module.addValidator(NameFilterFactory
						.getDefaultTransferNameFilter(searchId, 0.90d, module)
						.getValidator());
				module.addValidator(DateFilterFactory.getDateFilterForGoBack(
						SearchAttributes.GB_MANAGER_OBJECT, searchId, module)
						.getValidator());
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId,
								new String[] { "L;F;" });
				module.addIterator(nameIterator);
				modules.add(module);

			}

		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);

	}
    
    protected String getFileNameFromLink(String url) {
        
    	String rez = url.replaceAll(".*instrs=(.*?)(?=&|$)", "$1");
    	
    	if (rez.trim().length() > 10) {
    		rez = rez.replaceAll("&parentSite=true", "");
    		rez = rez.replaceAll("&isSubResult=true", "");
    		rez = rez.replaceFirst("&crossRefSource=[^&]+", "");
    	}
    	
        return rez.trim() + ".html";
    }
    
    public TSServerInfoModule getSearchByBookPageModule(TSServerInfo serverInfo, String book, String page, String instrumentNo)
    {
        if( "".equals( book ) && "".equals( page ) && "".equals( instrumentNo ) )
        {
            return null;
        }
        
        TSServerInfoModule retVal = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));

        retVal.getFunction( 0 ).setSaKey( "" );
        retVal.getFunction( 1 ).setSaKey( "" );
        retVal.getFunction( 2 ).setSaKey( "" );
        retVal.getFunction( 3 ).setSaKey( "" );
        
        if( !"".equals( book ) && !"".equals( page ) )
        {
            retVal.getFunction( 1 ).setParamValue( book );
            retVal.getFunction( 2 ).setParamValue( page );
        }
        else if( !"".equals( instrumentNo ) )
        {     
            retVal.getFunction( 0 ).setParamValue( instrumentNo );
        }
        
        retVal.getFunction( 4 ).setParamValue("Detail Data");
        
        return retVal;
    }
    
    public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
        p.splitResultRows(pr, htmlString, pageId, "<tr","</table", linkStart,  action);
    }
    
    public static final String DOC_TYPE_SELECT	=	
    	"<SELECT NAME='BookType'>" + 
	    "<OPTION VALUE='' SELECTED>" + 
	    "<OPTION VALUE='OR'>OFFICIAL RECORDS" + 
	    "<OPTION VALUE='DEED'>DEED" + 
	    "<OPTION VALUE='MTG'>MORTGAGE" + 
	    "<OPTION VALUE='AFDV'>AFFIDAVIT" + 
	    "<OPTION VALUE='MISC'>MISCELLANEOUS" + 
	    "<OPTION VALUE='OGL'>OIL & GAS" + 
	    "<OPTION VALUE='OR'>OFFICIAL RECORDS" + 
	    "<OPTION VALUE='ORD'>ORDINANCE" + 
	    "</SELECT>";
    

	public static String retrieveImageFromLink(String docNo, String fileName, long searchId)
	{

		// check whether the file exists
		fileName = fileName.replaceFirst("(?i)\\.pdf$", ".tiff");
		if(FileUtils.existPath(fileName)){
			return fileName;
		}

		HttpSite site = HttpManager.getSite("KSJohnsonRO", searchId);
		try 
		{
			
			//URL=http://recorder.jocogov.org/LoadImage.asp?199403291624385---> exemplu de request
			HTTPRequest req = new HTTPRequest( "http://recorder.jocogov.org/LoadImage.asp?"+docNo );
			req.setMethod(HTTPRequest.GET);
			req.setHeader( "Referer","http://recorder.jocogov.org/SimpleQuery.asp" );
			HTTPResponse res = site.process(req);
			if ("image/tiff".equals(res.getContentType())) {
				FileUtils.CreateOutputDir(fileName);
				FileUtils.writeStreamToFile(res.getResponseAsStream(), fileName);
			}
		} 
			catch(Exception e)
		{
		    logger.error(e);
		}
		finally 
		{
			HttpManager.releaseSite(site);
		}
		// return result
		if (FileUtils.existPath(fileName)) {
			return fileName;
		}
		return null;
	 
	}
	
	/**
	 * Create list of subdivisions that match
	 * @return
	 */
	
	public static String [] getSubdivisions(String subdivision, long searchId){
		
		SubdivisionMatcher matcher = SubdivisionMatcher.getInstance(SubdivisionMatcher.KS_JOHNSON, searchId);
		
        Vector<String> initialEquivalents = StringEquivalents.getInstance().getEquivalents(subdivision);
        
        HashSet<String> allSubdivisions = new HashSet<String>();
        allSubdivisions.addAll(initialEquivalents);
        
        for (String equivalent : initialEquivalents) {
			for(String subdivisionMatcherEquivalent: matcher.match(equivalent)){
				allSubdivisions.add(subdivisionMatcherEquivalent);
			}
		}
        if(allSubdivisions.isEmpty())
        	allSubdivisions.add(subdivision);
        
        return allSubdivisions.toArray(new String[allSubdivisions.size()]);
                
	}
	
	@Override
	protected void setCertificationDate() {
		if(false) {
	        try {
	        	if (CertificationDateManager.isCertificationDateInCache(dataSite)){
					String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
					getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
				} else{
			
		        	String html = HttpUtils.downloadPage("http://recorder.jocogov.org/logon.asp");
		            Matcher certDateMatcher = certDatePattern.matcher(html);
		            if(certDateMatcher.find()) {
		            	String date = certDateMatcher.group(1).trim();
		            	
		            	CertificationDateManager.cacheCertificationDate(dataSite, date);
						getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
		            }
				}
	        } catch (Exception e) {
	            logger.error(e.getMessage());
	        }
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap m,long searchId) {
		ArrayList<List> grantor = new ArrayList<List>();
		String tmpPartyGtor = (String)m.get(SaleDataSetKey.GRANTOR.getKeyName());
		
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			
			parseName(tmpPartyGtor, grantor, m);
			try {
				m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
				
		ArrayList<List> grantee = new ArrayList<List>();
		String tmpPartyGtee = (String)m.get(SaleDataSetKey.GRANTEE.getKeyName());
		
		if (StringUtils.isNotEmpty(tmpPartyGtee)){
			
			parseName(tmpPartyGtee, grantee, m);
			try {
				m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseName(String rawNames, ArrayList<List> list, ResultMap resultMap) {
		
		rawNames = rawNames.replaceAll("(?)\\bCO-TRUSTEE\\b", "TR");
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		String split[] = rawNames.split(" / ");
		for (int i=0;i<split.length;i++) {
			names = StringFormats.parseNameNashville( split[i], true);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(split[i], names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), list);
		}
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
		
		TSServerInfoModule module = null;
		if(StringUtils.isNotEmpty(instrumentNumber)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		    module.forceValue(0, instrumentNumber);
		    module.forceValue(4, "Detail Data");
		    module.forceValue(5, "DESC");
		    if(restoreDocumentDataI.getRecordedDate() != null) {
				module.forceValue(6, sdf.format(restoreDocumentDataI.getRecordedDate()));
				module.forceValue(7, sdf.format(restoreDocumentDataI.getRecordedDate()));
		    }
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		    module.forceValue(1, book);
			module.forceValue(2, page);
			module.forceValue(4, "Detail Data");
			module.forceValue(5, "DESC");
			if(restoreDocumentDataI.getRecordedDate() != null) {
				module.forceValue(6, sdf.format(restoreDocumentDataI.getRecordedDate()));
				module.forceValue(7, sdf.format(restoreDocumentDataI.getRecordedDate()));
		    }
		} 
		
		return module;
	}
     
    
    
}
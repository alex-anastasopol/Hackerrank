package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_LABEL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.createHiddenHTMLControl;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setJustifyFieldMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterResponse2;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.MunicipalityFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.SubdivisionMatcher;

import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.NameI;

public class MIWayneRO extends TSServer implements TSServerROLikeI {
    
	private static final long serialVersionUID = 4760453061875693625L;

	
	private static final int ID_CROSSREFERENCE = 102;
	private static final int ID_UNPLATTED = 103;
	
	private boolean downloadingForSave;

	public static final Pattern nextLinkPattern = Pattern.compile( "(?is)<img[^>]*src=[\"']/images/ToolbarIcons/next.gif[\"'] [^>]*>" );
	public static final Pattern prevLinkPattern = Pattern.compile( "(?is)<img[^>]*src=[\"']/images/ToolbarIcons/previous.gif[\"'] [^>]*>" );
	public static final Pattern pageNoPattern = Pattern.compile( "(?is)through</font></span>\\s*<span[^>]*><font[^>]*>(\\d+)" );
	public static final Pattern pageNoPattern2 = Pattern.compile( "(?is)through</span>\\s*<span[^>]*>(\\d+)" );
	
	public static final Pattern imageLinkPattern = Pattern.compile( "(?is)<a href=\"([^\"]*)\">View Image" );
	
	public MIWayneRO(long searchId) {super(searchId); }

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 */
	public MIWayneRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public static final int REC_DATE_IDX 	= 0;
	public static final int DOC_TYPE_IDX 	= 1;
	public static final int DOC_DATE_IDX 	= 2;
	public static final int INSTR_IDX 		= 3;
	public static final int BOOK_IDX 		= 4;
	public static final int PAGE_IDX 		= 5;
	public static final int LAST_IDX 		= 6;
	public static final int FIRST_IDX 		= 7;
	public static final int MIDDLE_IDX 		= 8;
	public static final int PTYPE_IDX 		= 9;
	public static final int CR_INSTR_IDX 	= 10;
	public static final int CR_BOOK_IDX 	= 11;
	public static final int CR_PAGE_IDX 	= 12;
	public static final int TAX_ID_IDX 		= 13;
	public static final int MUNICIP_IDX 	= 14;
	public static final int STR_NO_IDX		= 15;
	public static final int STR_NAME_IDX 	= 16;
	public static final int SUBD_IDX 		= 17;
	public static final int PL_BOOK_IDX 	= 18;
	public static final int PL_PAGE_IDX 	= 19;
	public static final int LOT_FROM_IDX 	= 20;
	public static final int LOT_TO_IDX 		= 21;
	public static final int BLK_IDX 		= 22;
	public static final int BLK_ALPHA_IDX 	= 23;
	public static final int CONDO_IDX 		= 24;
	public static final int CONDO_PLAN_IDX 	= 25;
	public static final int UNIT_FROM_IDX 	= 26;
	public static final int UNIT_TO_IDX 	= 27;
	public static final int BLDG_IDX 		= 28;
	public static final int BLDG_ALPHA_IDX 	= 29;
	public static final int TWN_IDX 		= 30;
	public static final int RNG_IDX 		= 31;
	public static final int SCT_IDX 		= 32;
	public static final int QTR_IDX 		= 33;
	public static final int SURVEY_IDX 		= 34;
	public static final int SURVEY_NO_IDX 	= 35;
	
	@Override
	public TSServerInfo getDefaultServerInfo()
    {
        TSServerInfo msiServerInfoDefault = null;
        
        msiServerInfoDefault = new TSServerInfo(1);        
        msiServerInfoDefault.setServerAddress("www.waynecountylandrecords.com");
        msiServerInfoDefault.setServerLink("http://www.waynecountylandrecords.com/Login.aspx");
        msiServerInfoDefault.setServerIP("www.waynecountylandrecords.com");
        
        // Combined search
        {
        	TSServerInfo si = msiServerInfoDefault;
			TSServerInfoModule 		
			sim = si.ActivateModule(TSServerInfo.ADV_SEARCH_MODULE_IDX, 54);
			sim.setName("Combined");
			sim.setDestinationPage("/RealEstate/SearchEntry.aspx");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_SEARCH_BY_ADDRESS);

			PageZone pz = new PageZone("SearchCombined", "Combined Search", ORIENTATION_HORIZONTAL, null, 770, 50, PIXELS , true);
			
			try{

				// add the hidden parameters
				int idx = SURVEY_NO_IDX + 1;
				pz.addHTMLObjectMulti( 
					new HTMLControl[] {
			        	createHiddenHTMLControl(sim.getFunction(idx++),  "Mainmenu1:MT", 			"", "",      searchId),
			        	createHiddenHTMLControl(sim.getFunction(idx++),  "Mainmenu1:timerData", 	"", "-1",    searchId),
			        	createHiddenHTMLControl(sim.getFunction(idx++),  "Mainmenu1:timerUrl", 	    "", "0",     searchId),
			        	createHiddenHTMLControl(sim.getFunction(idx++),  "f:d1_hidden", 			"", "",      searchId),
			        	createHiddenHTMLControl(sim.getFunction(idx++),  "f:d2_hidden", 			"", "",      searchId),
			        	createHiddenHTMLControl(sim.getFunction(idx++),  "f_d1_DrpPnl_Calendar1",   "", "",      searchId),
			        	createHiddenHTMLControl(sim.getFunction(idx++),  "f_d2_DrpPnl_Calendar1",   "", "",      searchId),
			        	createHiddenHTMLControl(sim.getFunction(idx++),  "Search__10", 			    "", ":0",    searchId),
			        	createHiddenHTMLControl(sim.getFunction(idx++),  "__EVENTTARGET", 		    "", "Search",searchId),
			        	createHiddenHTMLControl(sim.getFunction(idx++),  "__EVENTARGUMENT", 		"", "0",     searchId)
		        	}
				);
				
				// add the visible parameters
				HTMLControl
	            lbl1   = new HTMLControl(HTML_LABEL,      1, 1, 1, 1, 15, sim.getFunction(idx++), "LabelGeneral",  "<font color=\"brown\"><left><b>General</b></left></font>",  null, searchId),	            
				book   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 2, 2, 10, sim.getFunction(BOOK_IDX), "f:t4", "Book", null, searchId),
				page   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 2, 2, 10, sim.getFunction(PAGE_IDX), "f:t5", "Page", null, searchId),
				inst   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 3, 3, 15, sim.getFunction(INSTR_IDX), "f:t3", "Instr", null, searchId),				
				doct   = new HTMLControl(HTML_TEXT_FIELD, 1, 2, 4, 4, 10, sim.getFunction(DOC_TYPE_IDX), "f:d3", "Document Type", null, searchId),						
				recd   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 5, 5, 10, sim.getFunction(REC_DATE_IDX), "fxd1_input", "Reception Date", null, searchId),
				datd   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 5, 5, 10, sim.getFunction(DOC_DATE_IDX), "fxd2_input", "Dated Date", null, searchId),
				
				lbl2   = new HTMLControl(HTML_LABEL,      1, 1, 6, 6, 15, sim.getFunction(idx++), "LabelParty",  "<font color=\"brown\"><left><b>Party Name</b></left></font>",   null, searchId),				
				last   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 7, 7, 20, sim.getFunction(LAST_IDX), "f:t1", "Last/Company Name", null, searchId),
				first  = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 7, 7, 20, sim.getFunction(FIRST_IDX), "f:t2", "First", null, searchId),
				mid    = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 8, 8, 15, sim.getFunction(MIDDLE_IDX), "f:txtMName", "Middle", null, searchId),						
				ptype  = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 8, 8, 10, sim.getFunction(PTYPE_IDX), "f:r1", "Party Type", "", searchId),

				lbl3   = new HTMLControl(HTML_LABEL,      1, 1, 9, 9, 15, sim.getFunction(idx++), "LabelCross",  "<font color=\"brown\"><left><b>Cross Reference</b></left></font>",   null, searchId),				
				crbook = new HTMLControl(HTML_TEXT_FIELD, 1, 1,10,10, 15, sim.getFunction(CR_BOOK_IDX), "f:t22", "Cr. Book", null, searchId),
				crpage = new HTMLControl(HTML_TEXT_FIELD, 2, 2,10,10, 10, sim.getFunction(CR_PAGE_IDX), "f:t23", "Cr. Page", null, searchId),
				crinst = new HTMLControl(HTML_TEXT_FIELD, 1, 1,11,11, 15, sim.getFunction(CR_INSTR_IDX), "f:t21", "Cr. Instr", null, searchId),
				
				lbl4   = new HTMLControl(HTML_LABEL,      1, 1, 12, 12, 15, sim.getFunction(idx++), "LabelLegal",  "<font color=\"brown\"><left><b>Common Legal</b></left></font>",   null, searchId),				
				taxid  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 13, 13, 20, sim.getFunction(TAX_ID_IDX), "f:t31", "Tax ID", null, searchId),
				munic  = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 13, 13, 10, sim.getFunction(MUNICIP_IDX), "f:d31", "Municipality", null, searchId),
				strno  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 14, 14, 10, sim.getFunction(STR_NO_IDX), "f:t32", "Str. No", null, searchId),
				strna  = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 14, 14, 27, sim.getFunction(STR_NAME_IDX), "f:t33", "Str. Name", null, searchId),

				lbl5   = new HTMLControl(HTML_LABEL,      1, 1, 15, 15, 15, sim.getFunction(idx++), "LabelPlatted",  "<font color=\"brown\"><left><b>Platted</b></left></font>",   null, searchId),				
				subd   = new HTMLControl(HTML_TEXT_FIELD, 1, 2, 16, 16, 10, sim.getFunction(SUBD_IDX), "f:txtSubdivision", "Subdivision", null, searchId),
				pbook  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 17, 17, 10, sim.getFunction(PL_BOOK_IDX), "f:txtSDBook", "Plat Liber", null, searchId),
				ppage  = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 17, 17, 10, sim.getFunction(PL_PAGE_IDX), "f:txtSDPage", "Plat Page", null, searchId),
				lotfr  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 18, 18, 10, sim.getFunction(LOT_FROM_IDX), "f:t41", "Lot From", null, searchId),
				lotto  = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 18, 18, 10, sim.getFunction(LOT_TO_IDX), "f:t42", "Lot To", null, searchId),
				blk    = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 19, 19, 10, sim.getFunction(BLK_IDX), "f:t44", "Block", null, searchId),
				lotalp = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 19, 19, 10, sim.getFunction(BLK_ALPHA_IDX), "f:t43", "Lot Alpha", null, searchId),

				lbl6   = new HTMLControl(HTML_LABEL,      1, 1, 20, 20, 15, sim.getFunction(idx++), "LabelCondo",  "<font color=\"brown\"><left><b>Condominium</b></left></font>",   null, searchId),				
				condo  = new HTMLControl(HTML_TEXT_FIELD, 1, 2, 21, 21, 10, sim.getFunction(CONDO_IDX), "f:txtCondo", "Condominium", null, searchId),
				cdplan = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 22, 22, 10, sim.getFunction(CONDO_PLAN_IDX), "f:txtCPlanNo", "Plan #", null, searchId),
				unitfr = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 23, 23, 10, sim.getFunction(UNIT_FROM_IDX), "f:t52", "Unit # From", null, searchId),
				unitto = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 23, 23, 10, sim.getFunction(UNIT_TO_IDX), "f:t53", "Unit # To", null, searchId),
				bldg   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 24, 24, 10, sim.getFunction(BLDG_IDX), "f:t56", "Building", null, searchId),
				bldgal = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 24, 24, 10, sim.getFunction(BLDG_ALPHA_IDX), "f:t57", "Bldg Alpha", null, searchId),

				lbl7   = new HTMLControl(HTML_LABEL,      1, 1, 25, 25, 15, sim.getFunction(idx++), "LabelUnplatted",  "<font color=\"brown\"><left><b>Unplatted</b></left></font>",   null, searchId),
				twn    = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 26, 26, 10, sim.getFunction(TWN_IDX), "f:d61", "Township", null, searchId),
				rng    = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 26, 26, 10, sim.getFunction(RNG_IDX), "f:d62", "Range", null, searchId),
				sect   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 27, 27, 10, sim.getFunction(SCT_IDX), "f:d63", "Section", null, searchId),
				qtr    = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 27, 27, 10, sim.getFunction(QTR_IDX), "f:d64", "Quarter", null, searchId),
				
				lbl8   = new HTMLControl(HTML_LABEL,      1, 1, 28, 28, 15, sim.getFunction(idx++), "LabelSurvey",  "<font color=\"brown\"><left><b>Survey</b></left></font>",   null, searchId),
				svu    = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 29, 29, 10, sim.getFunction(SURVEY_IDX), "f:d7", "Survey Unit", null, searchId),
				svno   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 29, 29, 10, sim.getFunction(SURVEY_NO_IDX), "f:t71", "Survey No", null, searchId);
				
				pz.addHTMLObjectMulti(book, page, inst, doct, recd, datd, last,
						first, mid, ptype, crinst, crbook, crpage, taxid,
						munic, strno, strna, subd, pbook, ppage, lotfr, lotto,
						blk, lotalp, condo, cdplan, unitfr, unitto, bldg,
						bldgal, twn, rng, sect, qtr, svu, svno);			
				
				pz.addHTMLObjectMulti(lbl1, lbl2, lbl3, lbl4, lbl5, lbl6, lbl7, lbl8);
				                
                sim.getFunction(PTYPE_IDX).setHtmlformat(PARTY_TYPE_SELECT);
				sim.getFunction(DOC_TYPE_IDX).setHtmlformat(DOCTYPE_SELECT);
				sim.getFunction(MUNICIP_IDX).setHtmlformat(MUNICIP_SELECT);
	            sim.getFunction(SCT_IDX).setHtmlformat(SECTION_SELECT);
	            sim.getFunction(TWN_IDX).setHtmlformat(TOWNSHIP_SELECT);
	            sim.getFunction(RNG_IDX).setHtmlformat(RANGE_SELECT);                    
	            sim.getFunction(QTR_IDX).setHtmlformat(QUARTER_SELECT);
	            sim.getFunction(SURVEY_IDX).setHtmlformat(SURVEY_UNIT_SELECT);
	            
				sim.getFunction(SUBD_IDX).setHtmlformat(SUBDIV_SELECT);
				sim.getFunction(CONDO_IDX).setHtmlformat(CONDO_SELECT);
				
				setJustifyFieldMulti(false, page, datd, first, crpage, munic, strna, ppage, lotto, ptype, lotalp, unitto, bldgal, rng, qtr, svno);
				setJustifyFieldMulti(true, lbl1, lbl2, lbl3, lbl4, lbl5, lbl6, lbl7, lbl8);
				
			} catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);
        }

        msiServerInfoDefault.setupParameterAliases();
        setModulesForAutoSearch( msiServerInfoDefault );
        setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
                    
        return msiServerInfoDefault;
    }
	
	 protected String getFileNameFromLink(String link) {
		 String parcelId = StringUtils.getTextBetweenDelimiters( "dummy=" , "&", link);
		 if("".equals(parcelId)){
			 parcelId = StringUtils.extractParameter(link, "inst=([a-zA-Z0-9]+)");
		 }
		 return parcelId + ".html";
	}
	
	private static String getWarnings(String html){
		String WARN_START = "<table id=\"tblValidations\" class=\"Warning\" border=\"0\">";
		int istart = html.indexOf(WARN_START);
		if(istart == -1){
			return "";
		}
		istart += WARN_START.length();
		int iend = html.indexOf("</table>", istart);
		if(iend == -1){
			return "";
		}
		String warnings = html.substring(istart, iend);
		warnings = warnings.replaceAll("(?i)</td>", "<br/>");
		warnings = warnings.replaceAll("(?i)</?t[rd]>", " ");
		warnings = warnings.replaceAll("(?s)[\\n\\r]+", " ");
		warnings = warnings.replaceAll("\\s{2,}", " ");
		warnings = warnings.trim();
		if(warnings.endsWith("<br/>")){
			warnings = warnings.substring(0, warnings.lastIndexOf("<br/>"));
		}
		return warnings;		
	}
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		
		// check if we have received an error
		
		String initialResponse = rsResponse;
		String keyNumber = "";	
		String sTmp;
		int istart, iend;
		
		switch( viParseID ){
		
		    case ID_SEARCH_BY_BOOK_AND_PAGE:
			case ID_SEARCH_BY_NAME:
			case ID_CROSSREFERENCE:
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_SUBDIVISION_NAME:
			case ID_SEARCH_BY_CONDO_NAME:
			case ID_UNPLATTED:
				
				// get warnings
				String warnings = getWarnings(rsResponse);
				if(!StringUtils.isEmpty(warnings)){
					Response.getParsedResponse().setWarning(warnings);
					return;
				}
				
				//get table header
				istart = initialResponse.indexOf( "<thead" );
				iend = initialResponse.indexOf( "</thead>" , istart);
				
				if( istart < 0 || iend < 0 ){
					return;
				}
				
				String header = initialResponse.substring( istart , iend);
				
				header = header.replaceAll( "(?is)<thead[^>]*>" , "");
				header = header.replaceAll( "(?is)<th ([^>]*)>" , "<td $1>");
				header = header.replaceAll( "(?is)</th>" , "</td>");
				
				istart = rsResponse.indexOf( "<tbody>" );
				
				iend = rsResponse.indexOf( "DataFieldLabel" );
				iend = rsResponse.lastIndexOf( "</tbody>" , iend );
				
				if( istart < 0 || iend < 0 ){
					return;
				}
				
				
				String warning = StringUtils.extractParameter(rsResponse, "(Only first \\d+ matches are displayed. Refine your search criteria.)");
				if(!"".equals(warning)){
					Response.setWarning(warning);
				}				
				sTmp = CreatePartialLink(TSConnectionURL.idGET);						
				rsResponse = rsResponse.substring( istart  , iend + 8);
			
				// remove hidden rows
				rsResponse = removeHiddenRows(rsResponse);
				
				// rewrite all links to include the inst and doct parameters
				rsResponse = rewriteIntermLinks(rsResponse);	
				rsResponse = rsResponse.replaceFirst( "<tbody>" , "<tbody>" + header);
				rsResponse = rsResponse.replaceAll( "(?is)<tbody" , "<table" );
				rsResponse = rsResponse.replaceAll( "(?is)</tbody>" , "</table>" );
				rsResponse = rsResponse.replaceAll( "(?is)<th " , "<td " );
				rsResponse = rsResponse.replaceAll( "(?is)</th>" , "</td>" );
				rsResponse = rsResponse.replaceAll( "(?is)<colgroup>.*?</colgroup>" , "" );
				rsResponse = rsResponse.replaceAll( "(?is)<thead[^>]*>" , "" );
				rsResponse = rsResponse.replaceAll( "(?is)</thead>" , "" );				
				rsResponse = rsResponse.replaceAll( "(?is)</tr>\\s*<table>" , "</tr>" );
				rsResponse = rsResponse.replaceAll( "(?is)</table>\\s*</table>" , "</table>" );
				rsResponse = rsResponse.replaceAll( "(?is)<span[^>]*>" , "" );
				rsResponse = rsResponse.replaceAll( "(?is)</span>" , "" );
				rsResponse = rsResponse.replaceAll( "(?is)href=\"\\./SearchResults.aspx\\?([^\"]*)\"" , "href=\"" + sTmp + "/RealEstate/SearchResults.aspx&$1\"" );
				rsResponse = rsResponse.replaceAll( "(?is)<a\\s+href=\"javascript:_openImgWindow\\('\\./SearchResults.aspx\\?([^']*)'\\);\"><img[^>]*></a>" , "<A href=\"" + sTmp + "/RealEstate/SearchResults.aspx&$1\">View Image</a>" );				
				rsResponse = rsResponse.replaceAll( "(?is)<a href=\"javascript:igtbl_doPostBack\\([^\\)]*\\);\">\\s*<img[^>]*>\\s*</a>" , "");
				
				// remove images
				rsResponse = rsResponse.replaceAll("(?i)<img[^>]*>", "");
				
				rsResponse = rsResponse.replaceAll( "(?is)<input[^>]*>" , "");
				rsResponse = rsResponse.replaceAll( "(?is)<a href=\"([^\"]*&type=dtl[^\"]*)\">([^<]*)</a>" , "<a href=\"$1&dummy=$2&inst=$2\">$2</a>");
				rsResponse = rsResponse.replaceAll( "id='gxG1r_\\d+_\\d+'" , "");
				
				rsResponse = rsResponse.replaceAll( "(?is)<a href=\"([^\"]*/RealEstate/SearchResults.aspx[^\"]*type=dtl[^\"]*)\">" , "<input type=\"checkbox\" name=\"docLink\" value=\"$1\">$0");
				
				if(!"".equals(warning)){
					rsResponse = "<b><font color=\"red\">" + warning + "</font></b><br/>" + rsResponse; 
				}
				rsResponse =  rsResponse + "<div>" + SELECT_ALL_CHECKBOXES + "</div>";
				
				rsResponse = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + rsResponse +
						"<BR>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
 
				parser.Parse( Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);

				break;
				
			case ID_DETAILS:
				
				sTmp = CreatePartialLink(TSConnectionURL.idGET);			
				
				keyNumber = StringUtils.getTextBetweenDelimiters( "class=\"SDFld\">" , "<", rsResponse);
				
				//we have summary from the detailed page
				istart = rsResponse.indexOf( "General" );
				istart = rsResponse.lastIndexOf( "<table" , istart );
				
				iend = rsResponse.indexOf( "class=\"TDImage\"" );
				iend = rsResponse.lastIndexOf( "</table>" , iend );
				
				if( istart < 0 || iend < 0 ){
					return;
				}
				
				rsResponse = rsResponse.substring( istart, iend + 8 );
				
				rsResponse = rewriteDetailsLink(rsResponse);
				
				rsResponse = rsResponse.replaceAll( "(?is)<span[^>]*>" , "" );
				rsResponse = rsResponse.replaceAll( "(?is)</span>" , "" );
				
				rsResponse = rsResponse.replaceAll( "(?is)href=/RealEstate/(SearchResults|SearchDetail)\\.aspx\\?([^>]*)" , "href=\"" + sTmp + "/RealEstate/$1\\.aspx&$2\"" );
				rsResponse = rsResponse.replaceAll( "(?is)<a onclick=\"ShowPopup[^>]*>([^<]*)</a>" , "$1");
				rsResponse = rsResponse.replaceAll( "(?is)<a.*?href=\"javascript:_openImgWindow\\('\\./SearchImage.aspx\\?([^']*)'\\);\"><img[^>]*></a>" , "<A href=\"" + sTmp + "/RealEstate/SearchResults.aspx&$1\">View Image</a>" );
				
				Matcher imageLinkMatcher = imageLinkPattern.matcher( rsResponse );
				if( imageLinkMatcher.find() ){
					Response.getParsedResponse().addImageLink(new ImageLinkInPage (imageLinkMatcher.group(1).replace("&amp;", "&"), keyNumber + ".tiff" ));
				}
				
				if( !downloadingForSave ){
            		//not saving to TSR
            		
    				String qry = Response.getQuerry();
    				Response.setQuerry(qry);
    				String originalLink = sAction + "&" + qry + "&dummy=" + keyNumber;
                    String sSave2TSDLink = CreatePartialLink(TSConnectionURL.idGET) + originalLink;

    				if (FileAlreadyExist(keyNumber + ".html") ) {
    					rsResponse += CreateFileAlreadyInTSD();
    				} else {
    					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink,viParseID);
    					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
    				}

    				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
    				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
            	}
            	else{
            		//saving
                    msSaveToTSDFileName = keyNumber + ".html" ;
                    Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                    msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
            		
                    parser.Parse(
                        Response.getParsedResponse(),
                        rsResponse,
                        Parser.PAGE_DETAILS,
                        getLinkPrefix(TSConnectionURL.idGET),
                        TSServer.REQUEST_SAVE_TO_TSD);
                    
                    Matcher allLinks = Pattern.compile( "<a.*?>(.*?)</a>" ).matcher( rsResponse );
                    while( allLinks.find() ){
                    	String linkMatched = allLinks.group( 0 );
                    	if( !"View Image".equals( allLinks.group( 1 ) ) ){
                    		rsResponse = rsResponse.replace( linkMatched , allLinks.group( 1 ));
                    	}
                    }
                    
                    Response.getParsedResponse().setOnlyResponse(rsResponse);
            	}
				
				break;
				
			case ID_SAVE_TO_TSD:
				
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
				
			case ID_GET_LINK:
				if( Response.getQuerry().contains( "type=dtl" ) ){
					ParseResponse(sAction, Response, ID_DETAILS);	
				}
				else{
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}				
				break;
				
			default:
				break;
		}
	}

	private static final String HIDDEN_ROW = 
		"<tr><th class='SRHdr gxG1-hc' height=''><img src='/ig_common/Images/ig_tblBlank.gif' border='0' imgType='blank' style='visibility:hidden;'";
	
	/**
	 * Remove hidden rows from response
	 * @param response
	 * @return
	 */
	protected String removeHiddenRows(String response){
		
		int pos = 0, istart = -1, iend = -1;
		StringBuilder sb = new StringBuilder();		
		do{			
			istart = response.indexOf(HIDDEN_ROW, pos);
			if(istart != -1){				
				iend = response.indexOf("</tr>", istart);
				if(iend != -1){ 
					iend += "</tr>".length();
					sb.append(response.substring(pos, istart));
					pos = iend;
				}
			}
		}while(istart != -1 && iend != -1);		
		sb.append(response.substring(pos));		
		return sb.toString();
	}
	/**
	 * 
	 * @param response
	 * @return
	 */
	protected String rewriteIntermLinks(String response){
		
		int crt = 0;
		int istart = response.indexOf("<tr id='gxG1r_" + crt + "'", 0);
		if(istart == -1){ return response; }
		
		// append prefix
		StringBuilder sb = new StringBuilder();
		sb.append(response.substring(0, istart));
		
		boolean done = false;
		while(!done){
			crt ++;
			int iend = response.indexOf("<tr id='gxG1r_" + crt + "'", istart);
			String row;
			if(iend == -1){
				row = response.substring(istart);
				done = true;
			} else {
				row = response.substring(istart, iend);
			}
			String inst = StringUtils.extractParameter(row, "<a href=\"\\./SearchResults.aspx\\?global_id=[A-Z0-9]+&type=dtl\">([A-Z0-9]+)</a>");
			String doct = StringUtils.extractParameter(row, "igDataValue=\'([^']+)\'");
			
			row = row.replaceAll("SearchResults\\.aspx\\?global_id=([^&]+)&type=([a-zA-Z]+)","SearchResults.aspx?global_id=$1&type=$2" + "&inst=" + inst + "&doct=" + doct);
			sb.append(row);
			istart = iend;
		}
		
		String newResponse = sb.toString();
		
		return newResponse;
		
	}
	
	/**
	 * 
	 * @param origResponse
	 * @return
	 */
	protected String rewriteDetailsLink(String origResponse){
		String response = origResponse;
		
		// fix the image link
		//<span id="f_lst0__ctl0_txtInstrumentNo" class="SDFld">92007805</span>
		//<span id="f_lst0__ctl0_l2" class="SDFld">ASSIGNMENT</span></td>		
		String inst = StringUtils.extractParameter(response, "<span id=\"f_lst0__ctl0_txtInstrumentNo\" class=\"SDFld\">([A-Z0-9]+)</span>");		
		String doct = StringUtils.extractParameter(response, "<span id=\"f_lst0__ctl0_l2\" class=\"SDFld\">([^<]+)</span></td>");		
		doct = getDocTypeAbbrev(doct);						
		response = response.replaceFirst("SearchImage\\.aspx\\?global_id=([^&]+)&amp;type=img","SearchImage.aspx?global_id=$1&amp;type=img&amp;inst=" + inst + "&amp;doct=" + doct);
		
		// fix the related doc links
		// <TD><span id="f_l231" class="Heading">Related Document(s)</span></TD>
		// <TD><p><br></P><span id="f_l111" class="Heading">Grantor Name(s)</span></TD>
		int istart = response.indexOf("<table id=\"f_lst01\"");
		if(istart == -1){ return response;}
		int iend = response.indexOf("</table>", istart);
		if(iend == -1){ return response;}
		// go to table header
		istart = response.indexOf("<tr>", istart);
		if(istart == -1 || istart >= iend){ return response; }
		istart += "<tr>".length();
		if(istart >=  iend){ return response; }
		// go to first table row
		istart = response.indexOf("<tr>", istart);
		if(istart == -1 || istart >= iend){ return response; }
		istart += "<tr>".length();
		if(istart >=  iend){ return response; }
				
		String prefix = response.substring(0, istart);
		String table = response.substring(istart, iend);
		String suffix = response.substring(iend);
		
		StringBuilder sb = new StringBuilder();
		sb.append(prefix);
		
		istart = 0;
		while(true){
			
			iend = table.indexOf("</tr>", istart);
			if(iend == -1){
				sb.append(table.substring(istart));
				break;
			}
			iend += "</tr>".length();
			
			String row = table.substring(istart, iend);		
			inst = StringUtils.extractParameter(row, "<a href=/RealEstate/SearchDetail\\.aspx\\?GLOBAL_ID=[^>]+>([A-Z0-9]+)</a>");
			
			//>MTG</span></td>
			int iend1 = row.lastIndexOf("</span></td>");
			if(iend1 != -1){
				int istart1 = row.lastIndexOf(">", iend1);
				if(istart1 != -1 && istart < iend){
					doct = row.substring(istart1 + 1, iend1);
					// <a href=/RealEstate/SearchDetail.aspx?GLOBAL_ID=OPR162355320>89038025</a>
					//row = row.replaceFirst("SearchDetail\\.aspx\\?GLOBAL_ID=([^>]+)", "SearchDetail.aspx?GLOBAL_ID=$1&type=dtl&inst=" + inst + "&doct=" + doct);
					row = row.replaceFirst("SearchDetail\\.aspx\\?GLOBAL_ID=([^>]+)", "SearchResults.aspx?global_id=$1&type=dtl&inst=" + inst + "&doct=" + doct + "&dummy=" + inst);
					//row = row.replaceFirst("SearchDetail\\.aspx\\?GLOBAL_ID=([^>]+)", "SearchDetail.aspx?GLOBAL_ID=$1&type=dtl&inst=" + inst + "&doct=" + doct + "&dummy=" + inst);
				}
			}
			istart = iend;
			sb.append(row);						
		}
		
		sb.append(suffix);		

		return sb.toString();
	}

	protected FilterResponse getRejectAlreadyPresentFilter(){
		RejectAlreadyPresentFilterResponse filter = new RejectAlreadyPresentFilterResponse(searchId);
		filter.setThreshold(new BigDecimal("0.95"));
		return filter;
	}
	
	protected FilterResponse getIntervalFilter(){
		BetweenDatesFilterResponse filter = new BetweenDatesFilterResponse(searchId);
		filter.setThreshold(new BigDecimal("0.90"));
		return filter;
	}
	
	protected FilterResponse getLegalFilter(){
		GenericLegal filter = new GenericLegal(searchId);
		filter.disableAll();
		filter.setEnablePlatBook(true);
		filter.setEnablePlatPage(true);
		filter.setEnableLot(true);
		filter.setEnableBlock(true);
		filter.setEnableSection(true);
		filter.setThreshold(new BigDecimal("0.70"));
		return filter;
	}
	
	protected FilterResponse getAddressFilter(){
		return new AddressFilterResponse2(searchId);
	}
		
	protected FilterResponse getNameFilter(TSServerInfoModule module){
		GenericNameFilter filter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(module.getSaObjKey(), searchId, module);
		filter.setIgnoreMiddleOnEmpty(true);
		filter.setInitAgain(true);
		return filter;
	}
	
	protected FilterResponse getSubdivisionNameFilter(TSServerInfoModule module){
		return NameFilterFactory.getNameFilterForSubdivisionWithScore( searchId, 0.6);		
	}
	
	protected FilterResponse getSubdivisionFilterResponse(TSServerInfoModule module){
		return NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
	}
	
	protected boolean hasAddress(){

		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		return !StringUtils.isEmpty(strName) && !StringUtils.isEmpty(strNo);
	}
	
	protected boolean hasSubdivision(){
		String subdivision = getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME);
		return !StringUtils.isEmpty(subdivision);
	}
	
	protected boolean hasLegal(){
		
		String lot = getSearchAttribute(SearchAttributes.LD_LOTNO);
		String block = getSearchAttribute(SearchAttributes.LD_SUBDIV_BLOCK);
		String section = getSearchAttribute(SearchAttributes.LD_SUBDIV_SEC);
		
		return !StringUtils.isEmpty(lot) ||
		       !StringUtils.isEmpty(block) || 
		       !StringUtils.isEmpty(section);
	}
	
	@SuppressWarnings("unchecked")
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		
		// list of subdivisions  - max 3
		String subdivisionName = getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME).toUpperCase();
		String[] subdivisions = SubdivisionMatcher.getInstance(SubdivisionMatcher.MI_WAYNE_SUBDIV, searchId).match(subdivisionName);			
		// limit to 3
		if(subdivisions.length > 3){
			String [] newSubdivisions = new String[3];
			System.arraycopy(subdivisions, 0, newSubdivisions, 0, 3);
			subdivisions = newSubdivisions;
		}
		
		// list of condos - max 3
		String[] condos = SubdivisionMatcher.getInstance( SubdivisionMatcher.MI_WAYNE_CONDO, searchId).match( subdivisionName );
		if(condos.length > 3){
			String [] newCondos = new String[3];
			System.arraycopy(condos, 0, newCondos, 0, 3);
			condos = newCondos;
			
		}
		
		// List of Pins to be searched
		Collection<String> pins = new LinkedHashSet<String>();
		String pn = getSearchAttribute(SearchAttributes.LD_PARCELNO);			
		if(!StringUtils.isEmpty(pn)){
			pn = pn.replaceFirst("\\.", "");
			pins.add(pn);
		}
		if(pn.matches("\\d{8}")){				
			pins.add(pn.substring(0,2) + "/" + pn.substring(2));
		}
		
		// Create filters
		FilterResponse legalFilter = getLegalFilter();
		FilterResponse rejectAlreadyPresentFilter = getRejectAlreadyPresentFilter();
		FilterResponse intervalFilter = getIntervalFilter();
		FilterResponse addressFilter = getAddressFilter();
		FilterResponse lastTransferDateFilter = new LastTransferDateFilter(searchId);
		FilterResponse subdivisionNameFilter = getSubdivisionNameFilter(null);
		FilterResponse municipalityFilter = new MunicipalityFilterResponse(searchId);
		
		// Create validators
		DocsValidator legalValidator = legalFilter.getValidator();
		DocsValidator rejectAlreadyPresentValidator = rejectAlreadyPresentFilter.getValidator();
		DocsValidator intervalValidator = intervalFilter.getValidator();
		DocsValidator addressValidator = addressFilter.getValidator();
		DocsValidator lastTransferDateValidator = lastTransferDateFilter.getValidator();
		DocsValidator subdivisionNameValidator = subdivisionNameFilter.getValidator();
		DocsValidator municipalityValidator = municipalityFilter.getValidator();
		
		DocsValidator crossRefValidators [] =  new DocsValidator[]{addressValidator, subdivisionNameValidator, rejectAlreadyPresentValidator, intervalValidator};
		// Pin Search
		for(String pin: pins) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(TAX_ID_IDX).forceValue(pin);
			module.addFilters(rejectAlreadyPresentFilter, intervalFilter);
			module.addCrossRefValidators(crossRefValidators);
			modules.add(module);
		}
		
		// Address Search
		if(hasAddress()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(STR_NO_IDX).setSaKey(SearchAttributes.P_STREETNO);
			module.getFunction(STR_NAME_IDX).setSaKey(SearchAttributes.P_STREETNAME);
			module.addFilters(rejectAlreadyPresentFilter, intervalFilter);
			module.addValidator(addressValidator);
			module.addCrossRefValidators(crossRefValidators);											
			modules.add(module);
		}

		// Condo Search
		if(hasAddress()){
			String unit = getSearchAttribute(SearchAttributes.P_STREETUNIT_CLEANED);
			if(!StringUtils.isEmpty(unit)){
				for(String condo: condos){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
					module.clearSaKeys();
					module.getFunction(CONDO_IDX).forceValue(condo);
					module.getFunction(UNIT_FROM_IDX).forceValue(unit);
					module.addFilters(rejectAlreadyPresentFilter, intervalFilter);
					module.addValidators(addressValidator, subdivisionNameValidator);
					module.addCrossRefValidators(crossRefValidators);
					modules.add(module);
				}
			}
		}
		
		// owner search
		
		// Buyer Search	- search as grantor and then as grantee
		   ConfigurableNameIterator nameIterator = null;
		for(String gg : new String[]{"R", "E"}){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			FilterResponse intervalNameFilter = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module);
			module.clearSaKeys();
	//		module.setIteratorType(ModuleStatesIterator.TYPE_REGISTER_NAME_LAST_FIRST);
			module.setSaObjKey(SearchAttributes.BUYER_OBJECT);
            module.getFunction(LAST_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
            module.getFunction(FIRST_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);            
			module.getFunction(PTYPE_IDX).forceValue(gg);			
			String[] docTypeList = new String[] { "FAJ", "AL", "ATL", "BNF", "BL", "CNA", "COL", "CLN", "CL", "DEC", "SSL", "DJ", "FJL", 
					"FSF", "FSM", "FSC", "FSS", "FXF", "FXM", "FXC", "FCL", "JL", "JDG", "JD", "LAW", "LN", "LP", "LP1", "ML", "MDM", 
					"MEC", "MSL", "MTL", "NOB", "NL", "ROF", "ORD", "PCO", "PRB", "PRL", "RLR", "URR", "SFL", "SL", "SLT", "TLM", "USS", 
					"USL", "CDT", "STL", "WIL", "WFL", "WSL", "WTL", "AFL", "BLR", "JLT", "JGL", "JOF", "LR", "LPR", "MLC", "MCR", "MSR", 
					"MTR", "FET", "PTR", "SLR", "TLR", "USR", "WCA" };			
			for (String docType : docTypeList) {
				TSServerInfoFunction function = module.getFunction(module.addFunction());
				function.setParamName("f:d3");				
				function.setParamValue(docType);
				function.setName("doc type");
			}
			module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
			module.addFilters(getNameFilter(module), rejectAlreadyPresentFilter, intervalNameFilter);
			addFilterForUpdate(module, true);
			module.addCrossRefValidators(rejectAlreadyPresentValidator, intervalNameFilter.getValidator());			
		  	nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"});
			module.addIterator( nameIterator);
			modules.add(module);
		}
		
		// Search by book page list from AO
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
		module.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_NOT_AGAIN);
		module.setSaObjKey(SearchAttributes.INSTR_LIST);
		module.clearSaKeys();		
		module.getFunction(BOOK_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);		
		module.getFunction(PAGE_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		if(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().isUpdate() ||
				InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().isDateDown() ) {
			module.addValidator( intervalValidator );
			module.addCrossRefValidator( intervalValidator );
		}
		module.addCrossRefValidators(crossRefValidators);
		modules.add(module);
				
		// Search by book and page list from search page
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));		
		module.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_SEARCH_NOT_AGAIN);		
		module.setSaObjKey(SearchAttributes.LD_BOOKPAGE);
		module.clearSaKeys();
		module.getFunction(BOOK_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);		
		module.getFunction(PAGE_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
	    module.addValidators(crossRefValidators);
	    module.addCrossRefValidators(crossRefValidators);
		modules.add(module);
		
		// search by instrument list from search page
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
		module.setSaObjKey(SearchAttributes.LD_INSTRNO);
		module.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_SEARCH_NOT_AGAIN);
		module.clearSaKeys();
		module.getFunction(INSTR_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	    module.addValidators(crossRefValidators);
	    module.addCrossRefValidators(crossRefValidators);
		modules.add(module);		

	    // OCR last transfer - book / page search
	    module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    module.clearSaKeys();
	    module.getFunction(BOOK_IDX).setIteratorType( FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH );
	    module.getFunction(PAGE_IDX).setIteratorType( FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH );
	    module.addValidators(crossRefValidators);
	    module.addCrossRefValidators(crossRefValidators);
		modules.add(module);
		
	    // OCR last transfer - instrument search
	    module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    module.clearSaKeys();
	    module.getFunction(INSTR_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	    module.addValidators(crossRefValidators);
	    module.addCrossRefValidators(crossRefValidators);
		modules.add(module);

		for(String gg : new String[]{"R"}){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.clearSaKeys();
			FilterResponse intervalNameFilter = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module);
	//		module.setIteratorType(ModuleStatesIterator.TYPE_REGISTER_NAME_LAST_FIRST);
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
            module.getFunction(LAST_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
            module.getFunction(FIRST_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);            
			module.getFunction(PTYPE_IDX).forceValue(gg);			
			String[] docTypeList = new String[] { "FAJ", "AL", "ATL", "BNF", "BL", "CNA", "COL", "CLN", "CL", "DEC", "SSL", "DJ", "FJL", 
					"FSF", "FSM", "FSC", "FSS", "FXF", "FXM", "FXC", "FCL", "JL", "JDG", "JD", "LAW", "LN", "LP", "LP1", "ML", "MDM", 
					"MEC", "MSL", "MTL", "NOB", "NL", "ROF", "ORD", "PCO", "PRB", "PRL", "RLR", "URR", "SFL", "SL", "SLT", "TLM", "USS", 
					"USL", "CDT", "STL", "WIL", "WFL", "WSL", "WTL", "AFL", "BLR", "JLT", "JGL", "JOF", "LR", "LPR", "MLC", "MCR", "MSR", 
					"MTR", "FET", "PTR", "SLR", "TLR", "USR", "WCA" };			
			for (String docType : docTypeList) {
				TSServerInfoFunction function = module.getFunction(module.addFunction());
				function.setParamName("f:d3");				
				function.setParamValue(docType);
				function.setName("doc type");
			}
			module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
			module.addFilters(getNameFilter(module), rejectAlreadyPresentFilter, intervalNameFilter);	
			module.addCrossRefValidators(rejectAlreadyPresentValidator, intervalNameFilter.getValidator());			
		     ArrayList< NameI> searchedNames = null;
				if( nameIterator!=null ){
					searchedNames = nameIterator.getSearchedNames();
				}
				else
				{
					searchedNames = new ArrayList<NameI>();
				}
					
				nameIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator( module, searchId, false, new String[]{"L;F;"} );
				//get your values at runtime
				nameIterator.setInitAgain( true );
				//
				nameIterator.setSearchedNames( searchedNames );
				module.addIterator( nameIterator ); 
				
				modules.add(module);
		}
		
		/* From this point on, the legal is assumed to be OK since it is taken from RO */
		
		// Legal Description Search - book, page, lot
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
		module.setSaKey(PL_BOOK_IDX, SearchAttributes.LD_BOOKNO);
		module.setSaKey(PL_PAGE_IDX, SearchAttributes.LD_PAGENO);
		module.addIterator(new LegalDescriptionIterator(searchId));
		module.addFilters(rejectAlreadyPresentFilter, intervalFilter);
		module.addValidator(addressValidator);
		module.addCrossRefValidators(legalValidator, rejectAlreadyPresentValidator, intervalValidator);
		modules.add(module);
		
		// Plat Book & Page Search - Plats, Restrictions, Easements 
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
		module.addIterator(new HasPlatIterator(searchId));
		module.clearSaKeys();
		module.setSaKey(PL_BOOK_IDX, SearchAttributes.LD_BOOKNO);
		module.setSaKey(PL_PAGE_IDX, SearchAttributes.LD_PAGENO);		
		for (String docType : new String[] { "PLT", "PLA", "PRC", "EAS", "RST", "ABE", "VAC" }) {
			TSServerInfoFunction function = module.getFunction(module.addFunction());
			function.setParamName("f:d3");				
			function.setParamValue(docType);
			function.setName("doc type");
		}			
		module.addFilters(rejectAlreadyPresentFilter, intervalFilter);
		module.addValidators(legalValidator);
		module.addCrossRefValidators(addressValidator, legalValidator, rejectAlreadyPresentValidator, intervalValidator);
		modules.add(module);
		
		// Owner Search	- search as grantor and then as grantee
	 
		if(hasAddress()){
			for(String gg : new String[]{"R", "E"}){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
				module.clearSaKeys();
	      		//module.setIteratorType(ModuleStatesIterator.TYPE_REGISTER_NAME_LAST_FIRST);
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
	            module.getFunction(LAST_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
	            module.getFunction(FIRST_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.getFunction(PTYPE_IDX).forceValue(gg);
				module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
				module.addFilters(getNameFilter(module), rejectAlreadyPresentFilter, intervalFilter);				
				module.addValidators(addressValidator, subdivisionNameValidator, municipalityValidator, lastTransferDateValidator);				
				module.addCrossRefValidators(crossRefValidators);
				nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"});
				module.addIterator( nameIterator);
				
				modules.add(module);
			}
		}
		
		// Search by cross ref book and page list from ro docs
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		module.clearSaKeys();
		module.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);
		module.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_NOT_AGAIN);
		module.getFunction(BOOK_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);		
		module.getFunction(PAGE_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
	    module.addValidators(crossRefValidators);
	    module.addCrossRefValidators(crossRefValidators);
		modules.add(module);
		
		// Search by cross ref instrument list from ro docs
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		module.clearSaKeys();
		module.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);
		module.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_NOT_AGAIN);
		module.getFunction(INSTR_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);		
	    module.addValidators(crossRefValidators);
	    module.addCrossRefValidators(crossRefValidators);
		modules.add(module);		
		
		// modules.clear();
		
		// search for releases
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
		module.clearSaKeys();
		module.addIterator(new ReleasableDocsIterator(searchId));
		modules.add(module);

		// set list of automatic search modules 		
		serverInfo.setModulesForAutoSearch(modules);
	}	


	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		
		ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	  		 module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     module.getFunction(PTYPE_IDX).setData("B");
		     module.getFunction(LAST_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
	         module.getFunction(FIRST_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);      
		     module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);		    
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;", "L;m;"} );
		 	 module.addIterator(nameIterator);
		 	 module.addValidator( defaultLegalValidator );
			 module.addValidator( addressHighPassValidator );
	         module.addValidator( pinValidator );
             module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
		  	 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
		
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
		    	 module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 module.getFunction(PTYPE_IDX).setData("B");
				 module.getFunction(LAST_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		         module.getFunction(FIRST_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);      
				 module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;", "L;m;"} );
				 module.addIterator(nameIterator);
				 module.addValidator( defaultLegalValidator );
				 module.addValidator( addressHighPassValidator );
				 module.addValidator( pinValidator );
			     module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
				 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
			
	
			 modules.add(module);
			 
		     }

	    }	 
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);	     
		     		     
	
	
	}
	
		
		
	

    /**
     * get the image using the add to cart, checkout, etc
     */
    @Override
    protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException{
    	
    	String imageLink = image.getLink();
    	String link = StringUtils.extractParameter(imageLink, "Link=(.*)");
    	link = "http://www.waynecountylandrecords.com" + link;
    	
    	//String fileName = folderName + instNo + ".tiff";
		String fileName = image.getPath();
    	
		// retrieve the image
    	HttpSite site = HttpManager.getSite("MIWayneRO", searchId);
    	try{
			for(int i=0; i<2; i++){
	    		if(retrieveImage(link, fileName, site)){   		
	    			byte b[] = FileUtils.readBinaryFile(fileName);
	    			afterDownloadImage(true);
	        		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
	    		}
	    	}
    	} finally {
    		HttpManager.releaseSite(site);
    	}
    	return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );
    }
	
    @Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded)throws ServerResponseException { 		
    	
    	if(vsRequest.contains("type=img")){
    		
    		String instNo = StringUtils.extractParameter(vsRequest, "inst=([^&]+)");
    		
         	// construct fileName
        	String folderName = getCrtSearchDir() + "Register" + File.separator;
    		new File(folderName).mkdirs();
        	String fileName = folderName + instNo + ".tiff";
        	
        	// construct the link
        	String link = StringUtils.extractParameter(vsRequest, "Link=(.*)");
        	link = "http://www.waynecountylandrecords.com" + link;
        	
    		// retrieve the image
        	HttpSite site = HttpManager.getSite("MIWayneRO", searchId);
        	try{
    			for(int i=0; i<2; i++){
    	    		if(retrieveImage(link, fileName, site)){   		
    	    			break;
    	    		}
    	    	}
        	} finally {
        		HttpManager.releaseSite(site);
        	}
        	
        	// write the image to the client web-browser
    		boolean imageOK = writeImageToClient(fileName, "image/tiff");
    		
    		// image not retrieved
    		if(!imageOK){ 
    	        // return error message
    			ParsedResponse pr = new ParsedResponse();
    			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
    			throw new ServerResponseException(pr);			
    		}
    		// return solved response
    		return ServerResponse.createSolvedResponse();    
    		
    	}
    	
    	return super.GetLink(vsRequest, vbEncoded);    	
	}
	
    /**
     * Retrieve image from server
     * @param link
     * @param fileName
     * @param site
     * @return
     */
    private boolean retrieveImage(String link, String fileName, HTTPSiteInterface site){
    	
    	// do not retrieve the image twice
    	if(FileUtils.existPath(fileName)){
    		return true;
    	}    	
    	
		// create the output folder if it does not exist
		FileUtils.CreateOutputDir(fileName);
    	    		
		
		if(link.contains("?") || link.contains("&")){
			link = link.replace('?', '&');
			int idx = link.indexOf('&');
			link = link.substring(0, idx) + "?" + link.substring(idx + 1);
		}
		
		// download the image    	
		HTTPRequest httpRequest = new HTTPRequest(link);
		HTTPResponse httpResponse = site.process(httpRequest);
    	FileUtils.writeStreamToFile(httpResponse.getResponseAsStream(), fileName);
    	
    	// return status
    	return FileUtils.existPath(fileName);
    }
    
    public static void splitResultRows( Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
    throws ro.cst.tsearch.exceptions.ServerResponseException
    {
    	if(htmlString.contains("<tr id='gxG1r_")){
    		p.splitResultRows( pr, htmlString, pageId, "<tr id='gxG1r_", "</table>", linkStart, action);
    	} else {
    		htmlString = htmlString.replaceFirst("<tr>", "<tr bgcolor='E0E0FF'>");
    		htmlString = htmlString.replaceFirst("<table>", "<table border='0' cellpadding='1' cellspacing='1'>");
    		htmlString = htmlString.replaceAll("<tr id='gxG1_r_\\d+_0' level='\\d+_0'>", "<tr>");
    		htmlString = htmlString.replaceAll("(<tr id='gxG1_r_\\d+'(?: alt='true')?(?: selected='true')? level='\\d+')", "$1 bgcolor='#F0F0F0'");	    	
	    	p.splitResultRows( pr, htmlString, pageId, "<tr id='gxG1_r_", "</table>", linkStart, action);
    	}
    	// add the docType in the intermediate links, so that their context can be reconstructed when needed from the connection class
    	for(ParsedResponse pres: (Vector<ParsedResponse>)pr.getResultRows()){
    		String html = pres.getResponse();
    		String docType = StringUtils.extractParameter(html, "<td iDV=\"([^\"]+)\" title=\"Row #\\d+, Document Type\"");
    		html = html.replaceAll("&dummy=", "&doct=" + docType + "&dummy=");
    		pres.setResponse(html);
    		LinkInPage lip = pres.getPageLink();
    		lip.setOnlyOriginalLink(lip.getOriginalLink().replaceAll("&dummy=", "&doct=" + docType + "&dummy="));
    		lip.setOnlyLink(lip.getLink().replaceAll("&dummy=", "&doct=" + docType + "&dummy="));    		
    	}
    }
    
    public static String getSubdivisionsSelect( String selectName, long searchId, int type ){
    	StringBuffer subdivSelect = new StringBuffer();
    	
    	subdivSelect.append( "<select name=\"" + selectName + "\">" );
    	subdivSelect.append( "<option value=\"\" SELECTED></option>" );
    	String[] allWayneSubdivisions = DBManager.getSubdivisions( type );
    	for( int i = 0 ; i < allWayneSubdivisions.length ; i ++ ){
    		subdivSelect.append( "<option value=\"" + allWayneSubdivisions[i] + "\">" + allWayneSubdivisions[i] + "</option>" );
    	}
    	
    	subdivSelect.append("</select>");
    	
    	return subdivSelect.toString();
    }
    
	/** 
	 * document type abbreviations to be used from details page where only the
	 * expanded version is available
	**/
	private static final String [][] DOCTYPE_ABBREV = 
	{
	    {"APP","ABANDONED PROPERTY PROJECT"},
	    {"ABE","ABANDONMENT OF EASEMENT"},
	    {"FAJ","ABSTRACT OF JUDGEMENT"},
	    {"FAR","ABSTRACT OF JUDGEMENT RELEASE"},
	    {"AAA","ACKNOWLEDGMENT OF ADDITIONAL ADVANCE"},
	    {"ADD","ADDENDUM"},
	    {"AD","ADMINISTRATOR DEED"},
	    {"AFF","AFFADAVIT"},
	    {"AFA","AFFIDAVIT OF ABANDONMENT"},
	    {"AFL","AFFIDAVIT OF LIEN FOR NON PAYMENT ASSESSMENT"},
	    {"ALD","AFFIDAVIT OF LOST DEED"},
	    {"AFO","AFFIDAVIT OF OCCUPANCY"},
	    {"AGR","AGREEMENT"},
	    {"AMD","AMENDMENT"},
	    {"AME","AMENDMENT TO RELEASE AN AGREEMENT"},
	    {"ART","ARTICLES OF ASSOCIATION"},
	    {"AL","ASSESSMENT LIEN"},
	    {"ASG","ASSIGNMENT"},
	    {"ALC","ASSIGNMENT OF LAND CONTRACT"},
	    {"ATL","ATTORNEY LIEN"},
	    {"BNF","BANKRUPTCY NO FEE"},
	    {"BS","BILL OF SALE"},
	    {"BST","BILL OF SALE TERMINATION"},
	    {"MDI","BLANKET ASG OR DIS"},
	    {"BL","BOND LIEN"},
	    {"BLR","BOND LIEN RELEASE"},
	    {"CTF","CERTIFICATE"},
	    {"CAF","CERTIFICATE OF ACCELERATED FORFEITURE OF REAL PROPERTY"},
	    {"CA","CERTIFICATE OF ATTACHMENT"},
	    {"CC","CERTIFICATE OF COMPLETION"},
	    {"COF","CERTIFICATE OF FORFEITURE OF REAL PROPERTY"},
	    {"COR","CERTIFICATE OF REDEMPTION"},
	    {"CTA","CERTIFICATE OF TRUST AGREEMENT"},
	    {"CNA","CERTIFICATE OF U.S. / STATE NON-ATTACHMENT FOR TAX LIEN"},
	    {"CS","CERTIFIED SURVEY"},
	    {"CHY","CHANCERY FILE"},
	    {"COL","CLAIM OF LIEN"},
	    {"CMD","COMMISSIONERS DEED"},
	    {"CND","CONDEMNATION"},
	    {"CNS","CONSENT"},
	    {"CLN","CONSENT LIEN"},
	    {"CL","CONSTRUCTION LIEN"},
	    {"CON","CONTRACT"},
	    {"CD","COVENANT DEED"},
	    {"COE","COVENANT OF EXEMPTIONS"},
	    {"DC","DEATH CERTIFICATE"},
	    {"DIA","DECLARATION OF CLAIM"},
	    {"DOT","DECLARATION OF TAKING"},
	    {"DT","DECLARATION OF TRUST"},
	    {"DTA","DECLARATION OF TRUST (DTA)"},
	    {"DEC","DECREE"},
	    {"DD","DEED"},
	    {"DPR","DEED OF PERSONAL REPRESENTATIVE"},
	    {"DEF","DEFAULT"},
	    {"DJL","DEPARTMENT OF JUSTICE LIEN"},
	    {"DJR","DEPARTMENT OF JUSTICE LIEN RELEASE"},
	    {"SSL","DEPT OF SOCIAL SERVICES LIEN"},
	    {"DIS","DISCHARGE"},
	    {"DIC","DISCLAIMER"},
	    {"DJ","DIVORCE JUDGMENT OF DECREE"},
	    {"EAS","EASEMENT"},
	    {"ED","EXECUTORS DEED"},
	    {"FJL","FEDERAL JUDGEMENT LIEN"},
	    {"FD","FIDUCIARY DEED"},
	    {"FSF","FINANCING STATEMENT"},
	    {"FSM","FINANCING STATEMENT AMENDMENT"},
	    {"FSA","FINANCING STATEMENT ASSIGNMENT"},
	    {"FSC","FINANCING STATEMENT CONTINUATION"},
	    {"FSR","FINANCING STATEMENT RELEASE"},
	    {"FSS","FINANCING STATEMENT SUBORDINATION"},
	    {"FST","FINANCING STATEMENT TERMINATION"},
	    {"FXF","FIXTURE FILING"},
	    {"FXM","FIXTURE FILING AMENDMENT"},
	    {"FXA","FIXTURE FILING ASSIGNMENT"},
	    {"FXC","FIXTURE FILING CONTINUATION"},
	    {"FXR","FIXTURE FILING RELEASE"},
	    {"FXT","FIXTURE FILING TERMINATION"},
	    {"FCL","FRIEND OF COURT LIEN"},
	    {"GRN","GRANT"},
	    {"XXX","GRAPHIC SCIENCE CONVERSION"},
	    {"GDN","GUARDIAN DEED"},
	    {"IC","INSTALLMENT CONTRACT"},
	    {"ICT","INSTALLMENT CONTRACT TERMINATION"},
	    {"INV","INVENTORY"},
	    {"JL","JEOPARDY LIEN"},
	    {"JLT","JEOPARDY LIEN TERMINATION"},
	    {"JDG","JUDGMENT"},
	    {"JGL","JUDGMENT LIEN"},
	    {"JOF","JUDGMENT OF FORECLOSURE"},
	    {"JD","JUDICIAL DEED"},
	    {"LC","LAND CONTRACT"},
	    {"LCR","LAND CORNER RECORDATION"},
	    {"PAT","LAND PATENT"},
	    {"LAW","LAW FILE"},
	    {"LEA","LEASE"},
	    {"LA","LETTERS OF AUTHORITY"},
	    {"LN","LIEN"},
	    {"LR","LIEN RELEASE"},
	    {"LP","LIS PENDENS"},
	    {"LP1","LIS PENDENS (LIEN)"},
	    {"LPR","LIS PENDENS RELEASE"},
	    {"LOC","LOCATION DEED"},
	    {"MDA","MASTER DEED AMENDMENT"},
	    {"MDC","MASTER DEED FOR CONDOMINIUM"},
	    {"MDT","MASTER DEED TERMINATION"},
	    {"ML","MECHANICS LIEN"},
	    {"MLC","MECHANICS LIEN CERTIFICATE"},
	    {"MEM","MEMO"},
	    {"LCM","MEMO OF LAND CONTRACT"},
	    {"MDM","MEMORANDUM OF MORTGAGE"},
	    {"MEC","MESC LIEN"},
	    {"MCR","MESC LIEN RELEASE"},
	    {"MSL","MICHIGAN STATE TAX LIEN"},
	    {"MSR","MICHIGAN STATE TAX LIEN RELEASE"},
	    {"MIS","MISCELLANEOUS"},
	    {"MTG","MORTGAGE"},
	    {"MTL","MUNICIPAL TAX LIEN"},
	    {"MTR","MUNICIPAL TAX LIEN RELEASE"},
	    {"NOT","NOTICE"},
	    {"CTD","NOTICE BY PERSONS CLAIMING TITLE UNDER TAX DEED"},
	    {"EVA","NOTICE OF APPROVED ENVIRONMENTAL REMEDIATION"},
	    {"NOB","NOTICE OF BANKRUPTCY"},
	    {"NCI","NOTICE OF CLAIM"},
	    {"NCL","NOTICE OF CLAIM OF INTEREST"},
	    {"NCR","NOTICE OF CLAIM OF INTEREST RELEASE"},
	    {"NC","NOTICE OF COMMENCEMENT"},
	    {"EVN","NOTICE OF CORRECTIVE ACTION (DEPT. OF ENVIRON)"},
	    {"FET","NOTICE OF FEDERAL ESTATE TAX LIEN"},
	    {"NF","NOTICE OF FURNISHING"},
	    {"NL","NOTICE OF LEVY"},
	    {"NLR","NOTICE OF LEVY RELEASE"},
	    {"ROF","NOTICE OF RELEASE OF JUDGMENT OF FORECLOSURE"},
	    {"NTS","NOTICE OF TAX SALE"},
	    {"OPA","OPTION AGREEMENT"},
	    {"ORD","ORDER"},
	    {"PTD","PART DISCHARGE"},
	    {"PTR","PART RELEASE OF LIEN"},
	    {"PLT","PLAT"},
	    {"PLA","PLAT MAP"},
	    {"PRC","PLAT RECORDING CERTIFICATE"},
	    {"PA","POWER OF ATTORNEY"},
	    {"PCO","PROBATE COURT ORDER"},
	    {"PRB","PROBATE FILE"},
	    {"PD","PROOF OF DEATH"},
	    {"PON","PROOF OF NOTICE/PROOF OF RETURNED NOTICE"},
	    {"PS","PROOF OF SERVICE"},
	    {"POS","PROOF OF SERVICE NOTICE CLAIM"},
	    {"PNF","PROOF OF SERVICE OF NOTICE OF FURNISHING"},
	    {"PRL","PROPERTY REHABILITATION LIEN"},
	    {"QCD","QUIT CLAIM DEED"},
	    {"REC","REAL ESTATE CONSENT"},
	    {"RC","RECORDATION CERTIFICATE"},
	    {"RR","REDEMPTION RECEIPT"},
	    {"REL","RELEASE"},
	    {"RES","RESOLUTION"},
	    {"RST","RESTRICTIONS"},
	    {"RP","REVOCABLE PERMIT"},
	    {"RPA","REVOCATION OF POWER OF ATTORNEY"},
	    {"RLR","REVOCATION OF RELEASE FEDERAL / STATE LIEN"},
	    {"URR","REVOCATION OF RELEASE FEDERAL LIEN"},
	    {"ROW","RIGHT OF WAY"},
	    {"FSG","SECURITY AGREEMENT"},
	    {"FGT","SECURITY AGREEMENT TERMINATION"},
	    {"SCD","SHERIFF CERTIFICATE DEED"},
	    {"SHC","SHERIFFS CERTIFICATE"},
	    {"SHD","SHERIFFS DEED"},
	    {"SHA","SHERIFFS DEED AFFIDAVIT"},
	    {"SD","STATE DEED"},
	    {"SUB","SUBORDINATION AGREEMENT"},
	    {"SFL","SUBORDINATION OF FEDERAL TAX LIEN"},
	    {"SL","SUPER LIEN"},
	    {"SLR","SUPER LIEN RELEASE"},
	    {"SLT","SUPER LIEN TERMINATION"},
	    {"SI","SUPPLEMENTAL INDENTURE"},
	    {"SUR","SURVEY"},
	    {"SAF","SURVEYORS AFFIDAVIT"},
	    {"SA","SURVEYORS CERTIFICATE"},
	    {"TXD","TAX DEED"},
	    {"TLM","TAX LIEN MISCELLANEOUS"},
	    {"TLR","TAX LIEN MISCELLANEOUS RELEASE"},
	    {"TRM","TERMINATION"},
	    {"TA","TRUST AGREEMENT"},
	    {"TRD","TRUST DEED"},
	    {"TI","TRUST INDENTURE"},
	    {"TD","TRUST_DEED"},
	    {"USS","U S SUBORDINATION OF TAX LIEN"},
	    {"USL","U S TAX LIEN"},
	    {"USR","U S TAX LIEN RELEASE"},
	    {"CDT","U.S./STATE CERTIFICATE OF DISCHARGE FROM PROPERTY TAX LIEN"},
	    {"STL","U.S./STATE SUBORDINATION OF TAX LIEN"},
	    {"USD","US DEED"},
	    {"VAC","VACATING OF EASEMENT"},
	    {"VA","VETERANS ADMINISTRATION"},
	    {"V","VOID DOCUMENT"},
	    {"WVR","WAIVER"},
	    {"WAI","WAIVER 1"},
	    {"WDR","WAIVER OF DOWER RIGHTS"},
	    {"WD","WARRANTY DEED"},
	    {"WCA","WAYNE CIVIL ACTION"},
	    {"WIL","WILL"},
	    {"WAF","WITHDRAWAL OF AFFIDAVIT"},
	    {"WFL","WITHDRAWAL OF FEDERAL TAX LIEN"},
	    {"WSL","WITHDRAWAL OF STATE LIEN"},
	    {"WTL","WITHDRAWAL OF U.S./STATE TAX LIEN"},
	    {"WOE","WRIT OF EXECUTION"},
	    {"WOR","WRIT OF RESTITUTION"},
	    {"ZA","ZONING APPEAL"}
	};

	/** 
	 * document type abbreviations to be used from details page where only the
	 * expanded version is available
	 **/
	private static final Map<String,String> DOCTYPE_ABBREV_MAP = new HashMap<String,String>();
	static{
		for(String [] pair: DOCTYPE_ABBREV){
			DOCTYPE_ABBREV_MAP.put(pair[1], pair[0]);
		}
	}
	
	/**
	 * Convert expanded doctype to abbreviated doctype
	 * @param expanded
	 * @return
	 */
	private String getDocTypeAbbrev(String expanded){		
		String abbrev =  DOCTYPE_ABBREV_MAP.get(expanded);
		if(abbrev == null){ abbrev = ""; }
		return abbrev;		
	}
	
	private static final String DOCTYPE_SELECT = 
		"<select name=\"f:d3\" size=\"8\" multiple=\"multiple\">" +
		"<option value=\"\"></option>" +
		"<option value=\"APP\">ABANDONED PROPERTY PROJECT</option>" +
		"<option value=\"ABE\">ABANDONMENT OF EASEMENT</option>" +
		"<option value=\"FAJ\">ABSTRACT OF JUDGEMENT</option>" +
		"<option value=\"FAR\">ABSTRACT OF JUDGEMENT RELEASE</option>" +
		"<option value=\"AAA\">ACKNOWLEDGMENT OF ADDITIONAL ADVANCE</option>" +
		"<option value=\"ADD\">ADDENDUM</option>" +
		"<option value=\"AD\">ADMINISTRATOR DEED</option>" +
		"<option value=\"AFF\">AFFADAVIT</option>" +
		"<option value=\"AFA\">AFFIDAVIT OF ABANDONMENT</option>" +
		"<option value=\"AFL\">AFFIDAVIT OF LIEN FOR NON PAYMENT ASSESSMENT</option>" +
		"<option value=\"ALD\">AFFIDAVIT OF LOST DEED</option>" +
		"<option value=\"AFO\">AFFIDAVIT OF OCCUPANCY</option>" +
		"<option value=\"AGR\">AGREEMENT</option>" +
		"<option value=\"AMD\">AMENDMENT</option>" +
		"<option value=\"AME\">AMENDMENT TO RELEASE AN AGREEMENT</option>" +
		"<option value=\"ART\">ARTICLES OF ASSOCIATION</option>" +
		"<option value=\"AL\">ASSESSMENT LIEN</option>" +
		"<option value=\"ASG\">ASSIGNMENT</option>" +
		"<option value=\"ALC\">ASSIGNMENT OF LAND CONTRACT</option>" +
		"<option value=\"ATL\">ATTORNEY LIEN</option>" +
		"<option value=\"BNF\">BANKRUPTCY NO FEE</option>" +
		"<option value=\"BS\">BILL OF SALE</option>" +
		"<option value=\"BST\">BILL OF SALE TERMINATION</option>" +
		"<option value=\"MDI\">BLANKET ASG OR DIS</option>" +
		"<option value=\"BL\">BOND LIEN</option>" +
		"<option value=\"BLR\">BOND LIEN RELEASE</option>" +
		"<option value=\"CTF\">CERTIFICATE</option>" +
		"<option value=\"CAF\">CERTIFICATE OF ACCELERATED FORFEITURE OF REAL PROPERTY</option>" +
		"<option value=\"CA\">CERTIFICATE OF ATTACHMENT</option>" +
		"<option value=\"CC\">CERTIFICATE OF COMPLETION</option>" +
		"<option value=\"COF\">CERTIFICATE OF FORFEITURE OF REAL PROPERTY</option>" +
		"<option value=\"COR\">CERTIFICATE OF REDEMPTION</option>" +
		"<option value=\"CTA\">CERTIFICATE OF TRUST AGREEMENT</option>" +
		"<option value=\"CNA\">CERTIFICATE OF U.S. / STATE NON-ATTACHMENT FOR TAX LIEN</option>" +
		"<option value=\"CS\">CERTIFIED SURVEY</option>" +
		"<option value=\"CHY\">CHANCERY FILE</option>" +
		"<option value=\"COL\">CLAIM OF LIEN</option>" +
		"<option value=\"CMD\">COMMISSIONERS DEED</option>" +
		"<option value=\"CND\">CONDEMNATION</option>" +
		"<option value=\"CNS\">CONSENT</option>" +
		"<option value=\"CLN\">CONSENT LIEN</option>" +
		"<option value=\"CL\">CONSTRUCTION LIEN</option>" +
		"<option value=\"CON\">CONTRACT</option>" +
		"<option value=\"CD\">COVENANT DEED</option>" +
		"<option value=\"COE\">COVENANT OF EXEMPTIONS</option>" +
		"<option value=\"DC\">DEATH CERTIFICATE</option>" +
		"<option value=\"DIA\">DECLARATION OF CLAIM</option>" +
		"<option value=\"DOT\">DECLARATION OF TAKING</option>" +
		"<option value=\"DT\">DECLARATION OF TRUST</option>" +
		"<option value=\"DTA\">DECLARATION OF TRUST (DTA)</option>" +
		"<option value=\"DEC\">DECREE</option>" +
		"<option value=\"DD\">DEED</option>" +
		"<option value=\"DPR\">DEED OF PERSONAL REPRESENTATIVE</option>" +
		"<option value=\"DEF\">DEFAULT</option>" +
		"<option value=\"SSL\">DEPT OF SOCIAL SERVICES LIEN</option>" +
		"<option value=\"DIS\">DISCHARGE</option>" +
		"<option value=\"DIC\">DISCLAIMER</option>" +
		"<option value=\"DJ\">DIVORCE JUDGMENT OF DECREE</option>" +
		"<option value=\"EAS\">EASEMENT</option>" +
		"<option value=\"ED\">EXECUTORS DEED</option>" +
		"<option value=\"FJL\">FEDERAL JUDGEMENT LIEN</option>" +
		"<option value=\"FD\">FIDUCIARY DEED</option>" +
		"<option value=\"FSF\">FINANCING STATEMENT</option>" +
		"<option value=\"FSM\">FINANCING STATEMENT AMENDMENT</option>" +
		"<option value=\"FSA\">FINANCING STATEMENT ASSIGNMENT</option>" +
		"<option value=\"FSC\">FINANCING STATEMENT CONTINUATION</option>" +
		"<option value=\"FSR\">FINANCING STATEMENT RELEASE</option>" +
		"<option value=\"FSS\">FINANCING STATEMENT SUBORDINATION</option>" +
		"<option value=\"FST\">FINANCING STATEMENT TERMINATION</option>" +
		"<option value=\"FXF\">FIXTURE FILING</option>" +
		"<option value=\"FXM\">FIXTURE FILING AMENDMENT</option>" +
		"<option value=\"FXA\">FIXTURE FILING ASSIGNMENT</option>" +
		"<option value=\"FXC\">FIXTURE FILING CONTINUATION</option>" +
		"<option value=\"FXR\">FIXTURE FILING RELEASE</option>" +
		"<option value=\"FXT\">FIXTURE FILING TERMINATION</option>" +
		"<option value=\"FCL\">FRIEND OF COURT LIEN</option>" +
		"<option value=\"GRN\">GRANT</option>" +
		"<option value=\"XXX\">GRAPHIC SCIENCE CONVERSION</option>" +
		"<option value=\"GDN\">GUARDIAN DEED</option>" +
		"<option value=\"IC\">INSTALLMENT CONTRACT</option>" +
		"<option value=\"ICT\">INSTALLMENT CONTRACT TERMINATION</option>" +
		"<option value=\"INV\">INVENTORY</option>" +
		"<option value=\"JL\">JEOPARDY LIEN</option>" +
		"<option value=\"JLT\">JEOPARDY LIEN TERMINATION</option>" +
		"<option value=\"JDG\">JUDGMENT</option>" +
		"<option value=\"JGL\">JUDGMENT LIEN</option>" +
		"<option value=\"JOF\">JUDGMENT OF FORECLOSURE</option>" +
		"<option value=\"JD\">JUDICIAL DEED</option>" +
		"<option value=\"LC\">LAND CONTRACT</option>" +
		"<option value=\"LCR\">LAND CORNER RECORDATION</option>" +
		"<option value=\"PAT\">LAND PATENT</option>" +
		"<option value=\"LAW\">LAW FILE</option>" +
		"<option value=\"LEA\">LEASE</option>" +
		"<option value=\"LA\">LETTERS OF AUTHORITY</option>" +
		"<option value=\"LN\">LIEN</option>" +
		"<option value=\"LR\">LIEN RELEASE</option>" +
		"<option value=\"LP\">LIS PENDENS</option>" +
		"<option value=\"LP1\">LIS PENDENS (LIEN)</option>" +
		"<option value=\"LPR\">LIS PENDENS RELEASE</option>" +
		"<option value=\"LOC\">LOCATION DEED</option>" +
		"<option value=\"MDA\">MASTER DEED AMENDMENT</option>" +
		"<option value=\"MDC\">MASTER DEED FOR CONDOMINIUM</option>" +
		"<option value=\"MDT\">MASTER DEED TERMINATION</option>" +
		"<option value=\"ML\">MECHANICS LIEN</option>" +
		"<option value=\"MLC\">MECHANICS LIEN CERTIFICATE</option>" +
		"<option value=\"MEM\">MEMO</option>" +
		"<option value=\"LCM\">MEMO OF LAND CONTRACT</option>" +
		"<option value=\"MDM\">MEMORANDUM OF MORTGAGE</option>" +
		"<option value=\"MEC\">MESC LIEN</option>" +
		"<option value=\"MCR\">MESC LIEN RELEASE</option>" +
		"<option value=\"MSL\">MICHIGAN STATE TAX LIEN</option>" +
		"<option value=\"MSR\">MICHIGAN STATE TAX LIEN RELEASE</option>" +
		"<option value=\"MIS\">MISCELLANEOUS</option>" +
		"<option value=\"MTG\">MORTGAGE</option>" +
		"<option value=\"MTL\">MUNICIPAL TAX LIEN</option>" +
		"<option value=\"MTR\">MUNICIPAL TAX LIEN RELEASE</option>" +
		"<option value=\"NOT\">NOTICE</option>" +
		"<option value=\"CTD\">NOTICE BY PERSONS CLAIMING TITLE UNDER TAX DEED</option>" +
		"<option value=\"EVA\">NOTICE OF APPROVED ENVIRONMENTAL REMEDIATION</option>" +
		"<option value=\"NOB\">NOTICE OF BANKRUPTCY</option>" +
		"<option value=\"NCI\">NOTICE OF CLAIM</option>" +
		"<option value=\"NCL\">NOTICE OF CLAIM OF INTEREST</option>" +
		"<option value=\"NCR\">NOTICE OF CLAIM OF INTEREST RELEASE</option>" +
		"<option value=\"NC\">NOTICE OF COMMENCEMENT</option>" +
		"<option value=\"EVN\">NOTICE OF CORRECTIVE ACTION (DEPT. OF ENVIRON)</option>" +
		"<option value=\"FET\">NOTICE OF FEDERAL ESTATE TAX LIEN</option>" +
		"<option value=\"NF\">NOTICE OF FURNISHING</option>" +
		"<option value=\"NL\">NOTICE OF LEVY</option>" +
		"<option value=\"NLR\">NOTICE OF LEVY RELEASE</option>" +
		"<option value=\"ROF\">NOTICE OF RELEASE OF JUDGMENT OF FORECLOSURE</option>" +
		"<option value=\"NTS\">NOTICE OF TAX SALE</option>" +
		"<option value=\"OPA\">OPTION AGREEMENT</option>" +
		"<option value=\"ORD\">ORDER</option>" +
		"<option value=\"PTD\">PART DISCHARGE</option>" +
		"<option value=\"PTR\">PART RELEASE OF LIEN</option>" +
		"<option value=\"PLT\">PLAT</option>" +
		"<option value=\"PLA\">PLAT MAP</option>" +
		"<option value=\"PRC\">PLAT RECORDING CERTIFICATE</option>" +
		"<option value=\"PA\">POWER OF ATTORNEY</option>" +
		"<option value=\"PCO\">PROBATE COURT ORDER</option>" +
		"<option value=\"PRB\">PROBATE FILE</option>" +
		"<option value=\"PD\">PROOF OF DEATH</option>" +
		"<option value=\"PON\">PROOF OF NOTICE/PROOF OF RETURNED NOTICE</option>" +
		"<option value=\"PS\">PROOF OF SERVICE</option>" +
		"<option value=\"POS\">PROOF OF SERVICE NOTICE CLAIM</option>" +
		"<option value=\"PNF\">PROOF OF SERVICE OF NOTICE OF FURNISHING</option>" +
		"<option value=\"PRL\">PROPERTY REHABILITATION LIEN</option>" +
		"<option value=\"QCD\">QUIT CLAIM DEED</option>" +
		"<option value=\"REC\">REAL ESTATE CONSENT</option>" +
		"<option value=\"RC\">RECORDATION CERTIFICATE</option>" +
		"<option value=\"RR\">REDEMPTION RECEIPT</option>" +
		"<option value=\"REL\">RELEASE</option>" +
		"<option value=\"RES\">RESOLUTION</option>" +
		"<option value=\"RST\">RESTRICTIONS</option>" +
		"<option value=\"RP\">REVOCABLE PERMIT</option>" +
		"<option value=\"RPA\">REVOCATION OF POWER OF ATTORNEY</option>" +
		"<option value=\"RLR\">REVOCATION OF RELEASE FEDERAL / STATE LIEN</option>" +
		"<option value=\"URR\">REVOCATION OF RELEASE FEDERAL LIEN</option>" +
		"<option value=\"ROW\">RIGHT OF WAY</option>" +
		"<option value=\"FSG\">SECURITY AGREEMENT</option>" +
		"<option value=\"FGT\">SECURITY AGREEMENT TERMINATION</option>" +
		"<option value=\"SCD\">SHERIFF CERTIFICATE DEED</option>" +
		"<option value=\"SHC\">SHERIFFS CERTIFICATE</option>" +
		"<option value=\"SHD\">SHERIFFS DEED</option>" +
		"<option value=\"SHA\">SHERIFFS DEED AFFIDAVIT</option>" +
		"<option value=\"SD\">STATE DEED</option>" +
		"<option value=\"SUB\">SUBORDINATION AGREEMENT</option>" +
		"<option value=\"SFL\">SUBORDINATION OF FEDERAL TAX LIEN</option>" +
		"<option value=\"SL\">SUPER LIEN</option>" +
		"<option value=\"SLR\">SUPER LIEN RELEASE</option>" +
		"<option value=\"SLT\">SUPER LIEN TERMINATION</option>" +
		"<option value=\"SI\">SUPPLEMENTAL INDENTURE</option>" +
		"<option value=\"SUR\">SURVEY</option>" +
		"<option value=\"SAF\">SURVEYORS AFFIDAVIT</option>" +
		"<option value=\"SA\">SURVEYORS CERTIFICATE</option>" +
		"<option value=\"TXD\">TAX DEED</option>" +
		"<option value=\"TLM\">TAX LIEN MISCELLANEOUS</option>" +
		"<option value=\"TLR\">TAX LIEN MISCELLANEOUS RELEASE</option>" +
		"<option value=\"TRM\">TERMINATION</option>" +
		"<option value=\"TA\">TRUST AGREEMENT</option>" +
		"<option value=\"TRD\">TRUST DEED</option>" +
		"<option value=\"TI\">TRUST INDENTURE</option>" +
		"<option value=\"TD\">TRUST_DEED</option>" +
		"<option value=\"USS\">U S SUBORDINATION OF TAX LIEN</option>" +
		"<option value=\"USL\">U S TAX LIEN</option>" +
		"<option value=\"USR\">U S TAX LIEN RELEASE</option>" +
		"<option value=\"CDT\">U.S./STATE CERTIFICATE OF DISCHARGE FROM PROPERTY TAX LIEN</option>" +
		"<option value=\"STL\">U.S./STATE SUBORDINATION OF TAX LIEN</option>" +
		"<option value=\"USD\">US DEED</option>" +
		"<option value=\"VAC\">VACATING OF EASEMENT</option>" +
		"<option value=\"VA\">VETERANS ADMINISTRATION</option>" +
		"<option value=\"V\">VOID DOCUMENT</option>" +
		"<option value=\"WVR\">WAIVER</option>" +
		"<option value=\"WAI\">WAIVER 1</option>" +
		"<option value=\"WDR\">WAIVER OF DOWER RIGHTS</option>" +
		"<option value=\"WD\">WARRANTY DEED</option>" +
		"<option value=\"WCA\">WAYNE CIVIL ACTION</option>" +
		"<option value=\"WIL\">WILL</option>" +
		"<option value=\"WAF\">WITHDRAWAL OF AFFIDAVIT</option>" +
		"<option value=\"WFL\">WITHDRAWAL OF FEDERAL TAX LIEN</option>" +
		"<option value=\"WSL\">WITHDRAWAL OF STATE LIEN</option>" +
		"<option value=\"WTL\">WITHDRAWAL OF U.S./STATE TAX LIEN</option>" +
		"<option value=\"WOE\">WRIT OF EXECUTION</option>" +
		"<option value=\"WOR\">WRIT OF RESTITUTION</option>" +
		"<option value=\"ZA\">ZONING APPEAL</option>" +
		"</select>";

	private static final String MUNICIP_SELECT = 
		"<select name=\"f:d31\">" +
		"<option value=\"\"></option>" +
		"<option value=\"ALLEN PARK\">ALLEN PARK</option>" +
		"<option value=\"BELLEVILLE\">BELLEVILLE</option>" +
		"<option value=\"BROWNSTOWN TOWNSHIP\">BROWNSTOWN TOWNSHIP</option>" +
		"<option value=\"CANTON TOWNSHIP\">CANTON TOWNSHIP</option>" +
		"<option value=\"DEARBORN\">DEARBORN</option>" +
		"<option value=\"DEARBORN HEIGHTS\">DEARBORN HEIGHTS</option>" +
		"<option value=\"DETROIT\">DETROIT</option>" +
		"<option value=\"ECORSE\">ECORSE</option>" +
		"<option value=\"FLAT ROCK\">FLAT ROCK</option>" +
		"<option value=\"GARDEN CITY\">GARDEN CITY</option>" +
		"<option value=\"GIBRALTAR\">GIBRALTAR</option>" +
		"<option value=\"GROSSE ILE TOWNSHIP\">GROSSE ILE TOWNSHIP</option>" +
		"<option value=\"GROSSE POINTE\">GROSSE POINTE</option>" +
		"<option value=\"GROSSE POINTE FARMS\">GROSSE POINTE FARMS</option>" +
		"<option value=\"GROSSE POINTE PARK\">GROSSE POINTE PARK</option>" +
		"<option value=\"GROSSE POINTE SHORES\">GROSSE POINTE SHORES</option>" +
		"<option value=\"GROSSE POINTE WOODS\">GROSSE POINTE WOODS</option>" +
		"<option value=\"HAMTRAMCK\">HAMTRAMCK</option>" +
		"<option value=\"HARPER WOODS\">HARPER WOODS</option>" +
		"<option value=\"HIGHLAND PARK\">HIGHLAND PARK</option>" +
		"<option value=\"HURON TOWNSHIP\">HURON TOWNSHIP</option>" +
		"<option value=\"INKSTER\">INKSTER</option>" +
		"<option value=\"LINCOLN PARK\">LINCOLN PARK</option>" +
		"<option value=\"LIVONIA\">LIVONIA</option>" +
		"<option value=\"MELVINDALE\">MELVINDALE</option>" +
		"<option value=\"NORTHVILLE\">NORTHVILLE</option>" +
		"<option value=\"NORTHVILLE TOWNSHIP\">NORTHVILLE TOWNSHIP</option>" +
		"<option value=\"PLYMOUTH\">PLYMOUTH</option>" +
		"<option value=\"PLYMOUTH TOWNSHIP\">PLYMOUTH TOWNSHIP</option>" +
		"<option value=\"REDFORD TOWNSHIP\">REDFORD TOWNSHIP</option>" +
		"<option value=\"RIVER ROUGE\">RIVER ROUGE</option>" +
		"<option value=\"RIVERVIEW\">RIVERVIEW</option>" +
		"<option value=\"ROCKWOOD\">ROCKWOOD</option>" +
		"<option value=\"ROMULUS\">ROMULUS</option>" +
		"<option value=\"SOUTHGATE\">SOUTHGATE</option>" +
		"<option value=\"SUMPTER TOWNSHIP\">SUMPTER TOWNSHIP</option>" +
		"<option value=\"TAYLOR\">TAYLOR</option>" +
		"<option value=\"TRENTON\">TRENTON</option>" +
		"<option value=\"UNKNOWN\">UNKNOWN</option>" +
		"<option value=\"VAN BUREN TOWNSHIP\">VAN BUREN TOWNSHIP</option>" +
		"<option value=\"WAYNE\">WAYNE</option>" +
		"<option value=\"WESTLAND\">WESTLAND</option>" +
		"<option value=\"WOODHAVEN\">WOODHAVEN</option>" +
		"<option value=\"WYANDOTTE\">WYANDOTTE</option>" +
		"</select>";
	
	private static final String SECTION_SELECT = 
		"<select name=\"f:d61\">" +
		"<option value=\"\"></option>" +
		"<option value=\"1\">1</option>" +
		"<option value=\"2\">2</option>" +
		"<option value=\"3\">3</option>" +
		"<option value=\"4\">4</option>" +
		"<option value=\"5\">5</option>" +
		"</select>" ;		
	
	private static final String TOWNSHIP_SELECT = 
		"<select name=\"f:d62\">" +
		"<option value=\"\"></option>" +
		"<option value=\"8\">8</option>" +
		"<option value=\"9\">9</option>" +
		"<option value=\"10\">10</option>" +
		"<option value=\"11\">11</option>" +
		"<option value=\"12\">12</option>" +
		"<option value=\"13\">13</option>" +
		"</select>";
	
	private static final String RANGE_SELECT = 
		"<select name=\"f:d63\">" +
		"<option value=\"\"></option>" +
		"<option value=\"1\">1</option>" +
		"<option value=\"2\">2</option>" +
		"<option value=\"3\">3</option>" +
		"<option value=\"4\">4</option>" +
		"<option value=\"5\">5</option>" +
		"<option value=\"6\">6</option>" +
		"<option value=\"7\">7</option>" +
		"<option value=\"8\">8</option>" +
		"<option value=\"9\">9</option>" +
		"<option value=\"10\">10</option>" +
		"<option value=\"11\">11</option>" +
		"<option value=\"12\">12</option>" +
		"<option value=\"13\">13</option>" +
		"<option value=\"14\">14</option>" +
		"<option value=\"15\">15</option>" +
		"<option value=\"16\">16</option>" +
		"<option value=\"17\">17</option>" +
		"<option value=\"18\">18</option>" +
		"<option value=\"19\">19</option>" +
		"<option value=\"20\">20</option>" +
		"<option value=\"21\">21</option>" +
		"<option value=\"22\">22</option>" +
		"<option value=\"23\">23</option>" +
		"<option value=\"24\">24</option>" +
		"<option value=\"25\">25</option>" +
		"<option value=\"26\">26</option>" +
		"<option value=\"27\">27</option>" +
		"<option value=\"28\">28</option>" +
		"<option value=\"29\">29</option>" +
		"<option value=\"30\">30</option>" +
		"<option value=\"31\">31</option>" +
		"<option value=\"32\">32</option>" +
		"<option value=\"33\">33</option>" +
		"<option value=\"34\">34</option>" +
		"<option value=\"35\">35</option>" +
		"<option value=\"36\">36</option>" +
		"</select>";
	
	private static final String QUARTER_SELECT = 
		"<select name=\"f:d64\">" +
		"<option value=\"\"></option>" +
		"<option value=\"FR\">FR</option>" +
		"<option value=\"NE\">NE</option>" +
		"<option value=\"NW\">NW</option>" +
		"<option value=\"SE\">SE</option>" +
		"<option value=\"SW\">SW</option>" +
		"</select>";

	private static final String PARTY_TYPE_SELECT = 
		"<SELECT name=\"f:r1\" size=\"1\">" +
		"<OPTION value=\"\" SELECTED>Both</option>" +
		"<OPTION value=\"R\">Grantor</option>" +
		"<OPTION value=\"E\">Grantee</option>" +
		"</SELECT>";
	
	private static final String SURVEY_UNIT_SELECT = 
		"<select name=\"f:d7\">"+
		"<option selected=\"selected\" value=\"\"></option>"+
		"<option value=\"FRACTIONAL SECTIONS\">FRACTIONAL SECTIONS</option>"+
		"<option value=\"GOVERNOR AND JUDGES\">GOVERNOR AND JUDGES</option>"+
		"<option value=\"MILITARY RESERVE\">MILITARY RESERVE</option>"+
		"<option value=\"PARK LOTS\">PARK LOTS</option>"+
		"<option value=\"PRIVATE CLAIM\">PRIVATE CLAIM</option>"+
		"<option value=\"SHIPYARDS\">SHIPYARDS</option>"+
		"<option value=\"TEN THOUSAND ACRES\">TEN THOUSAND ACRES</option>"+
		"</select>";	
	
	private static final String SUBDIV_SELECT = MIWayneRO.getSubdivisionsSelect("f:txtSubdivision", -1, SubdivisionMatcher.MI_WAYNE_SUBDIV);
	private static final String CONDO_SELECT = MIWayneRO.getSubdivisionsSelect("f:txtCondo", -1, SubdivisionMatcher.MI_WAYNE_CONDO);

	/**
	 * Get certification date 
	 * @param searchId 
	 * @return certification date in MM/dd/yyyy or empty string if no certification date could be found
	 */
	synchronized public static String getCertificationDate(long searchId) {		
		try { 
			// try 3 times
			for (int i = 0; i < 3; i++) {
				// wait 10 secs before trying again
				if (i != 0) {					
					try {
						TimeUnit.SECONDS.sleep(10);
					} catch (InterruptedException ie) {
						throw new RuntimeException(ie);
					}
				}
				// get site
				HttpSite site = HttpManager.getSite("MIWayneRO", searchId);
				try {
					String link = "http://www.waynecountylandrecords.com/RealEstate/SearchEntry.aspx";
					String page = site.process(new HTTPRequest(link)).getResponseAsString();
					String date = StringUtils.extractParameter(page, "(?i)Permanent Index From \\d{2}/\\d{2}/\\d{4} to (\\d{2}/\\d{2}/\\d{4})");
					return date;
				} catch (RuntimeException e) {
					logger.error(e);
				} finally {
					// always release site
					HttpManager.releaseSite(site);
				}				
			}
		} catch (RuntimeException e) {
			logger.error(e);
		}
		
		// cert date could be found
		return "";
	}
	
    protected boolean getLogAlreadyFollowed(){
    	return false;
    }

    @Override
    protected ServerResponse SearchBy(boolean bResetQuery, TSServerInfoModule module, Object sd) throws ServerResponseException {

    	// clear party type parameter if name search is not needed, so that it does not appear in the search log
    	String last = module.getParamValue(LAST_IDX);
    	String first = module.getParamValue(FIRST_IDX);
    	String middle = module.getParamValue(MIDDLE_IDX);
    	if(StringUtils.isEmpty(last) && StringUtils.isEmpty(first) && StringUtils.isEmpty(middle)){
    		module.setValue(PTYPE_IDX, "");
    		module.getFunction(PTYPE_IDX).setLoggable(false);
    	}
    	
    	return super.SearchBy(bResetQuery, module, sd);
    }

//    @Override
//    public void processLastRealTransfer(TransferI transfer, OCRParsedDataStruct ocrdata){
//    	MIWayneRO.processLastRealTransfer(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext(), miServerID);
//    }
//    
//    /**
//     * Try to bootstrap the subdivision name, plat book and page, lot number 
//     */
//    private static void processLastRealTransfer(Search search, int miServerID){
//    	
//    	boolean legalBootstrapEnabled = HashCountyToIndex.isLegalBootstrapEnabled(search.getCommId(), miServerID);
//        boolean addressBootstrapEnabled = HashCountyToIndex.isAddressBootstrapEnabled(search.getCommId(), miServerID);
//        
//    	// obtain last transfer
//    	ParsedResponse pr = search.getLastTransferAsParsedResponse(true, false);
//    	if(pr == null){
//    		return;
//    	}
//    	
//    	// no use if it does not have parsed properties
//    	if(pr.getPropertyIdentificationSetCount() == 0){
//    		return;
//    	}
//    	
//    	// isolate subdivision name, plat book-page, lot
//    	String subd = "";
//    	String plbk = "";
//    	String plpg = "";
//    	String mun = "";
//    	Set<String> lots = new LinkedHashSet<String>();
//    	
//    	boolean found = false;
//    	for(int i=0; i<pr.getPropertyIdentificationSetCount(); i++){
//    		PropertyIdentificationSet pis = pr.getPropertyIdentificationSet(i);
//    		
//    		String crtMun = pis.getAtribute("MunicipalJurisdiction");
//    		if(StringUtils.isEmpty(mun)){
//    			mun = crtMun;
//    		}
//    		String crtSubd = pis.getAtribute("SubdivisionName");     		
//    		String crtPlbk = pis.getAtribute("PlatBook");
//    		String crtPlpg = pis.getAtribute("PlatNo");
//    		String crtLot = pis.getAtribute("SubdivisionLotNumber");
//    		
//    		// do't do anything if not sure
//    		if(StringUtils.isEmpty(crtSubd) || StringUtils.isEmpty(crtPlbk) || 
//    		   StringUtils.isEmpty(crtPlpg) || StringUtils.isEmpty(crtLot)){ 
//    				return; 
//    		}    		
//    		if(!crtLot.matches("\\d+") && !crtLot.matches("\\d+-\\d+")){
//    			return;
//    		}
//    		
//    		if(!found){
//    			// found first pis 
//    			subd = crtSubd;
//    			plbk = crtPlbk;
//    			plpg = crtPlpg;    			
//    			found = true;    			
//    		} else {
//    			// found subsequent pis
//    			// do't do anything if not sure
//    			if(!crtSubd.equals(subd) || !crtPlbk.equals(plbk) || !crtPlpg.equals(plpg)){
//    				return;
//    			}
//    		}
//    		lots.add(crtLot);
//    	}
//
//    	// take care of the lot
//    	String [] lotz = lots.toArray(new String[lots.size()]);
//    	String lot = lotz[0];
//    	for(int i=1; i<lotz.length; i++){
//    		lot += "," + lotz[i];
//    	}
//    	
//    	SearchAttributes sa = search.getSa();
//    	
//    	// save old values
//    	String ldBp1 = sa.getAtribute(SearchAttributes.LD_BOOKPAGE);
//    	String subd1 = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
//    	String lot1 = sa.getAtribute(SearchAttributes.LD_LOTNO);
//    	String mun1 = sa.getAtribute(SearchAttributes.P_MUNICIPALITY);
//    	
//    	// perform bootstrap
//    	String ldBp = ldBp1;
//    	if (legalBootstrapEnabled) {
//    		ldBp = plbk + "-" + plpg;
//        	sa.setAtribute(SearchAttributes.LD_BOOKPAGE, ldBp);
//        	sa.setAtribute(SearchAttributes.LD_BOOKNO, plbk);
//        	sa.setAtribute(SearchAttributes.LD_PAGENO, plpg);
//        	sa.setAtribute(SearchAttributes.LD_SUBDIV_NAME, subd);
//        	sa.setAtribute(SearchAttributes.LD_LOTNO, lot);
//    	}
//    	if (addressBootstrapEnabled) {
//    		sa.setAtribute(SearchAttributes.P_MUNICIPALITY, mun);
//    	}
//    	
//    	StringBuilder sbBefore = new StringBuilder();
//    	StringBuilder sbAfter = new StringBuilder();
//    	if (legalBootstrapEnabled || addressBootstrapEnabled) {
//    		sbBefore.append("<b>Legal before MIWayneRO bootstrap</b>:");
//    		sbAfter.append("<b>Legal after MIWayneRO bootstrap</b>:");
//    		if (legalBootstrapEnabled) {
//    			sbBefore.append("subdivision name=<b>" + subd1 + "</b>, plat=<b>" + ldBp1 + "</b>, lot=<b>" + lot1 + "</b>");
//        		sbAfter.append("subdivision name=<b>" + subd + "</b>, plat=<b>" + ldBp + "</b>, lot=<b>" + lot + "</b>");
//    		}
//    		if (addressBootstrapEnabled) {
//    			if (legalBootstrapEnabled) {
//    				sbBefore.append(", ");
//    	    		sbAfter.append(", ");
//    			}
//    			sbBefore.append("mun=<b>" + mun1 + "</b>");
//	    		sbAfter.append("mun=<b>" + mun + "</b>");
//    		}
//    		sbBefore.append(" " + SearchLogger.getTimeStamp(search.getSearchID()) + "<br/>");
//    		sbAfter.append(" " + SearchLogger.getTimeStamp(search.getSearchID()) + "<br/>");
//    	}
//    	
//    	// log the bootstrap 
//    	if (sbBefore.length()>0 && sbAfter.length()>0) {
//    		SearchLogger.info(sbBefore.toString(), search.getID());
//        	SearchLogger.info(sbAfter.toString(), search.getID());
//    	}
//    			
//	}

    private class LegalDescriptionIterator extends GenericRuntimeIterator<LotInterval> {
		
		private static final long serialVersionUID = 8989586891817117069L;
		public LegalDescriptionIterator(long searchId) {
			super(searchId);
		}
		@SuppressWarnings("unchecked")
		protected List<LotInterval> createDerrivations(){
			String plBk = getSearchAttribute(SearchAttributes.LD_BOOKNO);
			String plPg = getSearchAttribute(SearchAttributes.LD_PAGENO);
			String lot = getSearchAttribute(SearchAttributes.LD_LOTNO);	
			List<LotInterval>list = new LinkedList<LotInterval>();
			if(!StringUtils.isEmpty(plBk) && !StringUtils.isEmpty(plPg) && !StringUtils.isEmpty(lot)){
				Vector<LotInterval> lots = (Vector<LotInterval>)LotMatchAlgorithm.prepareLotInterval(lot);
				list.addAll(lots);
			}
			return list;
		}
		protected void loadDerrivation(TSServerInfoModule module, LotInterval lot){
			module.setData(LOT_FROM_IDX, String.valueOf(lot.getLow()));
			module.setData(LOT_TO_IDX, String.valueOf(lot.getHigh()));
		}
	}
    
	private class HasPlatIterator extends GenericRuntimeIterator<String> {
		private static final long serialVersionUID = 8989586891817117069L;
		public HasPlatIterator(long searchId) {
			super(searchId);
		}
		@SuppressWarnings("unchecked")
		protected List<String> createDerrivations(){
			String plBk = getSearchAttribute(SearchAttributes.LD_BOOKNO);
			String plPg = getSearchAttribute(SearchAttributes.LD_PAGENO);
			String plBkPg = getSearchAttribute(SearchAttributes.LD_BOOKPAGE);
			List<String> list = new LinkedList<String>();
			if(!StringUtils.isEmpty(plBk) && !StringUtils.isEmpty(plPg) && plBkPg.equals(plBk + "-" + plPg)){
				list.add(""); // does not matter what is in the list
			}
			return list;
		}
		protected void loadDerrivation(TSServerInfoModule module, String lot){}    	
    }
    
	private class ReleasableDocsIterator extends GenericRuntimeIterator<Map<String,String>>{
		
		private static final long serialVersionUID = 8989586891817117069L;
		
		public ReleasableDocsIterator(long searchId) {
			super(searchId);
		}
		@SuppressWarnings("unchecked")
		
		protected List<Map<String,String>> createDerrivations(){
			
			List<Map<String,String>> list = new LinkedList<Map<String,String>>();			
//			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			
//	    	// identify last transfer
//	    	for(ParsedResponse pr: (Collection<ParsedResponse>)search.getRegisterMap().values()){
//	    		// make sure it's a RO site
//	    		String src = pr.getOtherInformationSet().getAtribute("SrcType");
//	    		if(!src.equals("RO") && !src.equals("LA") && !src.equals("AD") && !src.equals("RV") && !src.equals("DT") && !src.equals("GI") && !src.equals("PI")){
//	    			continue;
//	    		}
//	    		// make sure it is a transfer
//	    		if(pr.getSaleDataSetsCount() == 0){
//	    			continue;
//	    		}
//	    		SaleDataSet sds = pr.getSaleDataSet(0);
//	    		String docType = sds.getAtribute("DocumentType");
//	    		if(!DocumentTypes.isLienDocType(docType, searchId) && !DocumentTypes.isMortgageDocType(docType, searchId)){
//	    			continue;
//	    		}
//	    		String inst = sds.getAtribute("InstrumentNumber");
//	    		if(StringUtils.isEmpty(inst)){
//	    			inst = sds.getAtribute("DocumentNumber");
//	    		}	    		
//	    		if(!StringUtils.isEmpty(inst)){
//	    			Map<String,String> map = new HashMap<String,String>();
//	    			map.put("inst", inst);
//	    			// also put book, page
//	    			String book = sds.getAtribute("Book");
//		    		String page = sds.getAtribute("Page");	    			
//		    		if(!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)){
//		    			map.put("book", book);
//			    		map.put("page", page);
//			    	}
//	    			list.add(map);
//	    		}
//
//	    	}
	    	System.out.println("Releasable docs: " + list);	    	
			return list;
			
		}
		
		protected void loadDerrivation(TSServerInfoModule module, Map<String,String> instr){
			module.setData(CR_INSTR_IDX, instr.get("inst"));
		}
	}
	
    @Override
	protected boolean isCrossRefToBeValidated(String type) {
		return true;
	}
}

package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_LABEL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setJustifyFieldMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.SubdivisionNameFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.search.filter.matchers.subdiv.SubdivMatcher;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MatchEquivalents;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringCleaner;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.SubdivisionMatcher;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

@SuppressWarnings("deprecation")
public class MOJacksonRO extends TSServer {

	private static final long serialVersionUID = -657153484566353413L;

	public MOJacksonRO(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public MOJacksonRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	// General
	private static final int GTOR_IDX 		= 0;
	private static final int GTEE_IDX 		= 1;
	private static final int DTYP_IDX 		= 2;
	private static final int FROMD_IDX 		= 3;
	private static final int TOD_IDX 		= 4;
	private static final int FROMI_IDX 		= 5;
	private static final int TOI_IDX 		= 6;
	private static final int BOOK_IDX 		= 7;
	private static final int PAGE_IDX 		= 8;
	private static final int LOC_IDX 		= 9;
	
	// Subdivisions
	private static final int SUB_IDX 		= 10;
	private static final int CITY_IDX 		= 11;
	private static final int BLOCK_IDX 		= 12;
	private static final int LOT_IDX 		= 13;
	
	// Condominiums
	private static final int CND_NAME_IDX 	= 14;
	private static final int CND_CITY_IDX 	= 15;
	private static final int CND_BLK_IDX 	= 16;
	private static final int CND_LOT_IDX 	= 17;
	private static final int CND_PH_IDX 	= 18;
	private static final int CND_BLDG_IDX 	= 19;
	private static final int CND_UNIT_IDX 	= 20;
	
	// Section Land
	private static final int SEC_IDX	 	= 21;
	private static final int TWN_IDX 		= 22;
	private static final int RNG_IDX 		= 23;
	
	// Surveys
	private static final int SRVY_IDX 		= 24;
	private static final Pattern certDatePattern = Pattern.compile("(?ism)Permanent Index From.*?to(.*?)<");
	
	public static final HashMap<String, String> ROTREquivalences = new HashMap<String, String>();
	static {
		ROTREquivalences.put("PATE'S ADD TO TOWN OF WESTPORT", "PATES ADD");
		ROTREquivalences.put("WERNER WHITE & WILSON'S SUB", "WERNER WHITE & WILSON SUB");
		ROTREquivalences.put("BUTCHER'S ADD ADD TO GRANDVIEW", "BUTCHERS ADD / BUTCHERS ADD TO GRANDVIEW");
		ROTREquivalences.put("THOMPSON'S JO O", "THOMPSON'S ADD J O");
		ROTREquivalences.put("CAMPBELL'S JOHN ADD TO WESTPORT", "CAMPBELL'S ADD TO WESTPORT (JOHN CAMPBELL'S)");
		ROTREquivalences.put("CASE'S THEO S SUB", "CASES T S");
		ROTREquivalences.put("WHITING & COOPER'S ADD TO LEE'S SUMMIT", "WHITING & COOPER'S ADD");
		ROTREquivalences.put("CANNON'S ADD TO GRAIN VALLEY", "CANNON'S ... ADD GRAIN VALLEY / CANNONS ");
		ROTREquivalences.put("HUTCHING'S", "HUTCHINS PARK");
		ROTREquivalences.put("LEE'S SUMMIT CITY FORMERLY TOWN STROTHER", "LEE'S SUMMIT");
		ROTREquivalences.put("STANLEY & JONES'", "JONES ADD OF STANLEY & JONES");
		ROTREquivalences.put("BAKER AND DE TRAY'S", "BAKER & DE FRAY");
		ROTREquivalences.put("CHICK'S J S PLACE SUB", "CHICK'S J S SUB OF� CH ICK'SPLACE");
		ROTREquivalences.put("PLEASANT VIEW CHARLES E FINLAY CO'S", "PLEASANT VIEW CHARLES FINLAY CO 6TH OF");
	}
	
	private static String DTYP_SELECT = "";
	private static String LOC_SELECT = "";
	
	static {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			DTYP_SELECT = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath + File.separator + "MOJacksonRODocType.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			LOC_SELECT = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath + File.separator + "MOJacksonROLocation.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static SimpleDateFormat writeDateFormat = new SimpleDateFormat("MM/dd/yyyy");
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
        TSServerInfo msiServerInfoDefault = null;
        
        msiServerInfoDefault = new TSServerInfo(1);        
        msiServerInfoDefault.setServerAddress("records.jacksongov.org");
        msiServerInfoDefault.setServerLink("http://records.jacksongov.org");
        msiServerInfoDefault.setServerIP("http://records.jacksongov.org");
        
        // Combined Search
    	TSServerInfo si = msiServerInfoDefault;
		TSServerInfoModule 		
		sim = si.ActivateModule(TSServerInfo.ADV_SEARCH_MODULE_IDX, 30);
		sim.setName("Combined");
		sim.setDestinationPage("/results.asp");
		sim.setRequestMethod(TSConnectionURL.idPOST);
		sim.setParserID(ID_SEARCH_BY_NAME);
		sim.setSearchType("CS");
		
		PageZone pz = new PageZone("SearchCombined", "Combined Search", ORIENTATION_HORIZONTAL, null, 550, 50, PIXELS , true);
		
        // build start date and end date formatted strings 
        String startDate="01/01/1960", endDate="";
        try{
        	
            SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
            Date end = SearchAttributes.DEFAULT_DATE_PARSER.parse(sa.getAtribute(SearchAttributes.TODATE));	            	            
            endDate = writeDateFormat.format(end);	            
        }catch (ParseException e){}
        
		
		
		try {
			
			int idx = SRVY_IDX + 1;
			
			HTMLControl			
			lbl1   = new HTMLControl(HTML_LABEL,      1, 1, 1, 1, 15, sim.getFunction(idx++), "LabelGeneral", "<font color=\"brown\"><b>General</b></font>", null, searchId),
			
			gtor   = new HTMLControl(HTML_TEXT_FIELD, 1, 3, 2, 2, 35, sim.getFunction(GTOR_IDX),  "txtTor",     	"Grantor", 	null, searchId),
			gtee   = new HTMLControl(HTML_TEXT_FIELD, 1, 3, 3, 3, 35, sim.getFunction(GTEE_IDX),  "txtTee",     	"Grantee", 	null, searchId),			
			dtyp   = new HTMLControl(HTML_TEXT_FIELD, 1, 3, 4, 4, 25, sim.getFunction(DTYP_IDX),  "selDocType",	 	"Doc Type",	null, searchId),
			fromd  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 5, 5, 10, sim.getFunction(FROMD_IDX), "txtDateFiledFr", "From Date",startDate, searchId),
			tod    = new HTMLControl(HTML_TEXT_FIELD, 2, 3, 5, 5, 10, sim.getFunction(TOD_IDX),   "txtDateFiledTo", "To Date", 	endDate,   searchId),
			fromi  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 6, 6, 10, sim.getFunction(FROMI_IDX), "txtInstNumFr",   "From Inst",null, searchId),
			toi    = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 6, 6, 10, sim.getFunction(TOI_IDX),   "txtInstNumTo",   "To Inst",	null, searchId),
			book   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 7, 7, 10, sim.getFunction(BOOK_IDX),  "txtBook",   		"Book",		null, searchId),
			page   = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 7, 7, 10, sim.getFunction(PAGE_IDX),  "txtPage",   		"Page",		null, searchId),
			loc    = new HTMLControl(HTML_TEXT_FIELD, 1, 3, 8, 8, 25, sim.getFunction(LOC_IDX),   "selLocation",   	"Location",	null, searchId),
			
			lbl2   = new HTMLControl(HTML_LABEL,      1, 1, 9, 9, 15, sim.getFunction(idx++), "LabelSubdivisions",  "<font color=\"brown\"><left><b>Subdiv</b></left></font>",  null, searchId),

			subd   = new HTMLControl(HTML_TEXT_FIELD, 1, 2, 10,10,35, sim.getFunction(SUB_IDX),   "txtSubDiv",   	"Subdiv",	null, searchId),
			city   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 11,11,10, sim.getFunction(CITY_IDX),  "txtSubDivTown",  "City",		null, searchId),
			block  = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 11,11,10, sim.getFunction(BLOCK_IDX), "txtBlock",   	"Block",	null, searchId),
			lot    = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 11,11,10, sim.getFunction(LOT_IDX),   "txtLot2",   	    "Lot",		null, searchId),
			
			lbl3   = new HTMLControl(HTML_LABEL,      1, 1, 12,12,15, sim.getFunction(idx++), "LabelCondominiums",  "<font color=\"brown\"><left><b>Condo</b></left></font>",  null, searchId),
			cndn   = new HTMLControl(HTML_TEXT_FIELD, 1, 2, 13,13,25, sim.getFunction(CND_NAME_IDX), "txtCondo",   	 "Condo",	null, searchId),
			cndcit = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 14,14,10, sim.getFunction(CND_CITY_IDX), "txtCondoTown", "City",	null, searchId),
			cndblk = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 14,14,10, sim.getFunction(CND_BLK_IDX),  "txtCondoBlock","Block",	null, searchId),
			cndlot = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 14,14,10, sim.getFunction(CND_LOT_IDX),  "txtCondoLot2", "Lot",		null, searchId),
			cndph  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 15,15,10, sim.getFunction(CND_PH_IDX),   "txtPhase",   	 "Phase",	null, searchId),
			cndbld = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 15,15,10, sim.getFunction(CND_BLDG_IDX), "txtBldg",   	 "Building",null, searchId),
			cndunt = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 15,15,10, sim.getFunction(CND_UNIT_IDX), "txtUnit",   	 "Unit",	null, searchId),
						
			lbl4   = new HTMLControl(HTML_LABEL,      1, 1, 16,16,15, sim.getFunction(idx++), "LabelSectionLand",  	"<font color=\"brown\"><left><b>Section</b></left></font>",  null, searchId),
			sec    = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 17,17,10, sim.getFunction(SEC_IDX), 	"txtSect",   	 "Section",	null, searchId),
			twn    = new HTMLControl(HTML_TEXT_FIELD, 2, 2, 17,17,10, sim.getFunction(TWN_IDX), 	"txtTown",   	 "Township",null, searchId),
			rng    = new HTMLControl(HTML_TEXT_FIELD, 3, 3, 17,17,10, sim.getFunction(RNG_IDX), 	"txtRange",   	 "Range",	null, searchId),
			
			lbl5   = new HTMLControl(HTML_LABEL,      1, 1, 18,18,15, sim.getFunction(idx++), "LabelSurveys",  		"<font color=\"brown\"><left><b>Survey</b></left></font>", 	  null, searchId),
			srvy   = new HTMLControl(HTML_TEXT_FIELD, 1, 2, 19,19,35, sim.getFunction(SRVY_IDX), 	"txtSurvey",   	 "Survey",	null, searchId);
			
			pz.addHTMLObjectMulti(lbl1, gtor, gtee, dtyp, fromd, tod, fromi, toi, book, page, loc);
			pz.addHTMLObjectMulti(lbl2, subd, city, block, lot);
			pz.addHTMLObjectMulti(lbl3, cndn, cndcit, cndblk, cndlot, cndph, cndbld, cndunt);
			pz.addHTMLObjectMulti(lbl4, sec, twn, rng);
			pz.addHTMLObjectMulti(lbl5, srvy);
			
			setJustifyFieldMulti(false, lbl1, gtor, gtee, dtyp, fromd, tod, fromi, toi, book, page, loc);
			setJustifyFieldMulti(false, lbl2, subd, city, block, lot);
			setJustifyFieldMulti(false, lbl3, cndn, cndcit, cndblk, cndlot, cndph, cndbld, cndunt);
			setJustifyFieldMulti(false, lbl4, sec, twn, rng);
			setJustifyFieldMulti(false, lbl5, srvy);
			
			gtor.setFieldNote("Last, First");
			gtee.setFieldNote("Last, First");
			fromd.setFieldNote("MM/dd/yyyy");
			tod.setFieldNote("MM/dd/yyyy");
			
		} catch (FormatException e) {			
			e.printStackTrace();
		}
		
		sim.getFunction(DTYP_IDX).setHtmlformat(DTYP_SELECT);
		sim.getFunction(LOC_IDX).setHtmlformat(LOC_SELECT);		
		
		sim.setModuleParentSiteLayout(pz);
		
        msiServerInfoDefault.setupParameterAliases();
        setModulesForAutoSearch( msiServerInfoDefault );
        setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
                    
        return msiServerInfoDefault;
	}

/* Pretty prints a link that was already followed when creating TSRIndex
 * (non-Javadoc)
 * @see ro.cst.tsearch.servers.types.TSServer#getPrettyFollowedLink(java.lang.String)
 */	
    public String getPrettyFollowedLink (String initialFollowedLnk)
    {
    	if (initialFollowedLnk.matches("(?i).*[^a-z]+insno[=]([0-9a-z]+)[^0-9a-z][&=a-z]+crossRefSource[=]([a-z]*)[^a-z]*.*"))
    	{
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*[^a-z]+insno[=]([0-9a-z]+)[^0-9a-z][&=a-z]+crossRefSource[=]([a-z]*)[^a-z]*.*", 
    				"Instrument " + "$1" + " " + "$2" + 
    				" has already been processed from a previous search in the log file. ");
    		retStr =  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    		return retStr;
    	}
    	
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
	    
	private boolean downloadingForSave = false;

	@Override
	protected void ParseResponse(String action, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String result = Response.getResult();
		String query = Response.getQuerry();
		ParsedResponse pr = Response.getParsedResponse();
		
		switch(viParseID){
		
		case ID_SEARCH_BY_NAME:
			
			String interm = getIntermTable(result);
			if(StringUtils.isEmpty(interm)){
				pr.setOnlyResponse("No results found!");
				return;
			}
			String [] navInfo = getNavInfo(result);
			String prevLink = navInfo[0];
			String nextLink = navInfo[1];
			String posInfo = navInfo[2];
			
			if(!StringUtils.isEmpty(prevLink)){
				interm += prevLink + "&nbsp;&nbsp;&nbsp;";
			}
			if(!StringUtils.isEmpty(nextLink)){
				interm += nextLink + "&nbsp;&nbsp;&nbsp;";
			}
			if(!StringUtils.isEmpty(posInfo)){
				interm += posInfo;
			}
			interm += "<br/><br/>";
			
	        interm =  CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + 
	        		  interm + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);	        
			
			parser.Parse(pr, interm, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), REQUEST_SAVE_TO_TSD);
			
			pr.setNextLink(nextLink);
			
			break;
			
		case ID_DETAILS:
			
			String details = getDetails(result);
			if(StringUtils.isEmpty(details)){
				pr.setError("Could not find details for the selected document!");
				return;
			}			
			
			pr.setResponse(details);
			String keyNumber = StringUtils.extractParameter(details, "<b>Instrument Number:</b></td>[\n\r\t ]*<td>([^>]*)</td>");			
			if(StringUtils.isEmpty(keyNumber)){
				pr.setError("Could not find instrument number for the selected document!");
				return;
			}
			details = details.replaceAll("(?is)<tr>\\s*<td [^>]+><img\\s+src[^>]+></td>\\s*</tr>", "");//B3434
			String imageLink = StringUtils.extractParameter(details, "<a href='([^']*)'>View Image</a>");
			if(!StringUtils.isEmpty(imageLink)){
				String sFileLink = keyNumber + ".tiff";
				ImageLinkInPage ilip = new ImageLinkInPage(imageLink, sFileLink);
				pr.addImageLink(ilip);
			}

			if( !downloadingForSave ){
        		
				String qry = Response.getQuerry();
				Response.setQuerry(qry);
				String originalLink = action + "&" + qry + "&dummy=" + keyNumber;
                String sSave2TSDLink = CreatePartialLink(TSConnectionURL.idGET) + originalLink;

				if (FileAlreadyExist(keyNumber + ".html")) {
					details += CreateFileAlreadyInTSD();
				} else {
					details = addSaveToTsdButton(details, sSave2TSDLink,viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, result);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), details, Parser.NO_PARSE);
				
        	} else {
        		String resultForCross = details;
        		Pattern crossRefLinkPattern = Pattern.compile("(?is)<a href\\s*=\\s*'([^']+)'[^>]*>([^<]+)</a>");
                Matcher crossRefLinkMatcher = crossRefLinkPattern.matcher(resultForCross);
                
                while(crossRefLinkMatcher.find()) {
	                ParsedResponse prChild = new ParsedResponse();
	                String link = crossRefLinkMatcher.group(1) + "&isSubResult=true";
	                LinkInPage pl = new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD);
	                prChild.setPageLink(pl);
	                Response.getParsedResponse().addOneResultRowOnly(prChild);
                }
                
                msSaveToTSDFileName = keyNumber + ".html" ;
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                // details = details.replaceAll("(?is)<a href\\s*=\\s*'([^']+)'[^>]*>([^<]+)</a>", "$2");	
                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
                parser.Parse( Response.getParsedResponse(), details, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
                Response.getParsedResponse().setOnlyResponse(details.replaceAll("(?is)<a href\\s*=\\s*'([^']+)'[^>]*>([^<]+)</a>", "$2"));
        	}
			
			break;
			
		case ID_GET_LINK:

			if(query.contains( "id=" ) ){
				ParseResponse(action, Response, ID_DETAILS);	
			} else{
				ParseResponse(action, Response, ID_SEARCH_BY_NAME);
			}	
			
			break;			
			
		case ID_SAVE_TO_TSD:
			
			downloadingForSave = true;
			ParseResponse(action, Response, ID_DETAILS);
			downloadingForSave = false;
			
			break;
		}
	}
    @SuppressWarnings("unchecked")
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) 
   		throws ro.cst.tsearch.exceptions.ServerResponseException { 

	    	// split results
	        p.splitResultRows(pr, htmlString, pageId, "<tr", "</tr>", linkStart, action);
	
	        // remove table header
	        Vector rows = pr.getResultRows();
	        if (rows.size()>0) { 
	            ParsedResponse firstRow = (ParsedResponse)rows.remove(0); 
	            pr.setResultRows(rows);
	            pr.setHeader(pr.getHeader()+firstRow.getResponse()); 
	        }
	        rows = pr.getResultRows();
	        
	        for(ParsedResponse row: (Vector<ParsedResponse>)rows){
	        	String html = row.getResponse();	        	
	        	// <td><a href='/title-search/URLConnectionReader?p1=071&p2=1&searchId=703026&ActionType=2&Link=docdetail.asp&id=Tk2NDkwFb%03%13MJXItV0pCTYwN%2EzKYkIwN&ms=0&cabinet=opr&pg=&id2=S8xLE6BRH%03%0FM7ATDVH9OjY%3D%21%21%21eszE5N'>1966B0560490</a>
	        	String docNo = StringUtils.extractParameter(html, "<td><a href='[^']*'>([^>]+)</a>");
	        	// <a href='/title-search/URLConnectionReader?p1=071&p2=1&searchId=703026&ActionType=2&Link=docimage.asp&id=Tk2NDkw4%21%03%13M7HjCOP0%21TYwNmArpkIwN&ms=0&cabinet=opr&pg=&id2=S8xLVI9a%21%03%0FMq%21CTMN9DjY%3DjcpaVzE5N'>View Image</a>
	        	html = html.replaceFirst("<a href='([^']*)'>View Image</a>", "<a href='$1&docNo=" + docNo + "'>View Image</a>"); 
	        	row.setResponse(html);
	        }

    }
    
    /**
     * Extract intermediate table of results
     * @param result
     * @return
     */
	private String getIntermTable(String result) {
		
		// isolate table
		int istart = result.indexOf("<tr valign=top><td scope=\"col\"><b>Instrument Number</b></td>");
		int iend = result.indexOf("</table>", istart);
		if(istart == -1 || iend == -1){
			return "";
		}
		String table = "<table width=100% border=\"1\" cellpadding=\"0\" cellspacing=\"0\">" + 			
			result.substring(istart, iend + "</table>".length());
		        
		table = table.replaceAll("<!--[^-]*-->", "");
        table = table.replaceAll("<td>\\s*</td>", "<td>&nbsp;</td>");
		table = table.replace("<tr><td colspan=99><img src=\"/graphics/line.jpg\" width=100% height=1 alt=\"divider\"></td></tr>", "");
		table = table.replaceFirst("<tr>[\n\r\t ]+<td colspan[^>]*><img[^>]*></td>[\n\r\t ]+</tr>","");		
		table = table.replaceAll("<(td|tr)[^>]*>","<$1>");
        table = table.replaceFirst("(?i)<tr[^>]*>", "<tr bgcolor=#DEDEDE><td><b><div>" + SELECT_ALL_CHECKBOXES + "</div></b></td>");
				

		// rewrite details links
		//<a href=docdetail.asp?id=Tk2NDkwfL%03%13Mt9%21Tm%214STYwN%2FhN%21kIwN&ms=0&cabinet=opr&pg=1&id2=S8xLPufw%21%03%0FM%21%21%2131U%21OjY%3D%21XqdgzE5N>
		table = table.replaceAll(
				"<td><a href=docdetail\\.asp\\?id=([^>]*)>",
				"<td><input type=\"checkbox\" name=\"docLink\" value=\"" +  CreatePartialLink(TSConnectionURL.idGET) + "docdetail.asp&id=$1\"></td><td><a href='" + CreatePartialLink(TSConnectionURL.idGET) + "docdetail.asp&id=$1'>");

		// <a href=docimage.asp?id=Tg0MDkxph%03%13MHVaIHoSBDA4M%21%21EGUkwM&ms=0&cabinet=opr&pg=&id2=S8xLOh%21C%21%03%0FMN%21X%21%21%210KDE%3D%21Ti4yzE4N><img border=0 src="/graphics/paper.gif" alt="Click here to retrieve document image" width=12 height=15 ><BR></a>
		table = table.replaceAll(
				"<a href=docimage\\.asp\\?(id=[^>]*)><img[^>]*><BR></a>",
				"<a href='" + CreatePartialLink(TSConnectionURL.idGET) + "docimage.asp&$1'>View Image</a>");
		
		return table;

	}

	/**
	 * Extract navigation info: prev link, next link, location
	 * @param result
	 * @return
	 */
	private String[] getNavInfo(String result) {
		
		String [] retVal = new String[]{"", "", ""};
		String crtPageStr = StringUtils.extractParameter(result, "<font color=red><b>(\\d+)</b></font>");
		if(StringUtils.isEmpty(crtPageStr)){
			return retVal;
		}
		int crtPage = Integer.parseInt(crtPageStr);

		if(result.contains("<a href=\"results.asp?pg=" + (crtPage-1) + "\"")){
			retVal[0] = "<a href='" + CreatePartialLink(TSConnectionURL.idGET) + "/results.asp&pg=" + (crtPage-1) + "'>Prev</a>";
		}
		if(result.contains("<a href=\"results.asp?pg=" + (crtPage+1) + "\"")){
			retVal[1] = "<a href='" + CreatePartialLink(TSConnectionURL.idGET) + "/results.asp&pg=" + (crtPage+1) + "'>Next</a>";
		}

		String no = StringUtils.extractParameter(result, "More than (\\d+) matches found");
		if(!StringUtils.isEmpty(no)){
			retVal[2] = "more than " + no + " matches found";
		} else if(result.contains("1 match found")){
			retVal[2] = "1 match found";
		} else {
			no = StringUtils.extractParameter(result, "(\\d+) matches");
			if(!StringUtils.isEmpty(no)){
				retVal[2] = no + " matches";
			}
		}
		retVal[2] = "Page " + crtPage + " - " + retVal[2];
		
		return retVal;
	}

	/**
	 * Extract details from html
	 * @param result
	 * @return
	 */
	private String getDetails(String result) {
		
		int istart = result.indexOf("<table width=95% border=\"0\" cellpadding=\"2\" cellspacing=\"2\">");
		if(istart == -1){
			return "";
		}
		int iend = result.indexOf("<img src=\"/graphics/fade3_hr.gif", istart);
		if(iend == -1){
			return "";
		}
		iend = result.lastIndexOf("</table", iend);
		if(iend == -1){
			return "";
		}
		iend += "</table>".length();
		
		String details = result.substring(istart, iend);
		
		details = details.replaceAll("<!--[^-]*-->", "");
		
		String keyNumber = StringUtils.extractParameter(details, "<b>Instrument Number:</b></td>[\n\r\t ]*<td>([^>]*)</td>");
		
		// rewrite image link
		// <a href=docimage.asp?id=Tk3MjA3MD%03%13M2%2Fw%217%211%21DgwMo03bUkwM&ms=0&cabinet=opr&pg=4&id2=y8xMjg%21%21J%03%0FM2b0eV%21ZdTcxvNy2Ui8xO><img border=0 src="/graphics/paper.gif" alt="Click here to retrieve document image" width=12 height=15 ><BR></a>
		details = details.replaceFirst(
				"(?i)<a href=docimage\\.asp\\?([^>]*)><img[^>]*><BR></a>", 
				"<a href='" + CreatePartialLink(TSConnectionURL.idGET) + "docimage.asp&$1&docNo=" + keyNumber + "'>View Image</a>");		
		
		// rewrite crossreferences links
		// <a href=docdetail.asp?id=Tk3OTI4aq%03%13MJ6%21%21nkrNzIxOye%21qEkwM&ms=0&cabinet=opr&pg=4&id2=y82Luew%21w%03%0FMm%21SW1B1Yzg%3D2S%21%21xzE5N>1978I0321928</a>
		details = details.replaceAll(
				"(?i)<a href=docdetail\\.asp\\?id=([^>]*)>",
				"<a href='" + CreatePartialLink(TSConnectionURL.idGET) + "docdetail.asp&id=$1'>");

		return details;
	}
	
	/**
	 * Create list of subdivisions that match
	 * @return
	 */
	
	private String [] getSubdivisions(){
		
		String subdivision = getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME);		
		String[] subdivisions = SubdivisionMatcher.getInstance(SubdivisionMatcher.MO_JACKSON,searchId).match(subdivision);
		
        //try to match the equivalent too
        if ((subdivisions == null||subdivisions.length==0) && subdivision.length() > 0) {
        	subdivisions = SubdivisionMatcher.getInstance(SubdivisionMatcher.MO_JACKSON,searchId).match(MatchEquivalents.getInstance(searchId).getEquivalent( subdivision ) );
        }

        if (subdivisions == null && subdivision.length() > 0) {
        	subdivisions = SubdivisionMatcher.getInstance(SubdivisionMatcher.MO_JACKSON,searchId).match(subdivision.replace(" AND ", " & "));
        }
        
        if (subdivisions == null && subdivision.length() > 0) {
        	subdivisions = new String[1];
        	subdivisions[0] = subdivision;
        }
        
        return subdivisions;
	}
	public static FilterResponse subdivName(long searchId){
	      SubdivisionNameFilterResponse sfr = new SubdivisionNameFilterResponse(searchId);
        SubdivMatcher subdivMatcher = sfr.getSubdivMatcher();
        subdivMatcher.setLotMatchAlgorithm(MatchAlgorithm.TYPE_LOT_ZERO_IGNORE);
        subdivMatcher.setSubdivMatchAlgorithm(MatchAlgorithm.TYPE_SUBDIV_NA_DESOTO);
	    return sfr;
	}
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;
		Search global = InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext(); 
		SearchAttributes sa = global.getSa();
		String subdivision = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		String lot = sa.getAtribute(SearchAttributes.LD_LOTNO);
		String block = sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
	     
		subdivision = subdivision.replaceFirst("MCGRAIL & FELTONS", "MC GRAIL & FELTON'S");
		String[] subdivisions = getSubdivisions();
		
		DocsValidator lastTransferDateValidator = (new LastTransferDateFilter(
				searchId)).getValidator();
        DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
		DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
		DocsValidator subdivisionNameValidator = NameFilterFactory.getNameFilterForSubdivisionWithCleaner(searchId, StringCleaner.JACKSON_SUBDIV).getValidator();
		FilterResponse subdivName = NameFilterFactory.getNameFilterForSubdivisionWithScore( searchId, 0.65);

		boolean validateWithDates = sa.isUpdate() || sa.isDateDown();
		
		// search for plats
		// subdiv + city + block
		// subdiv + city + lot
		// grantor/grantee
		// buyer search - removed as requested by customer
		// restrictions
		// easements
		// transfers
		// grantee - removed for B367
		// grantee = subdiv name && plat
		// instr number list from search page
		// book-page list from search page
		// OCR last stransfer
		// grantor grantee search with names brought by OCR or Transfer Index
        // search by grantor/grantee
		
        boolean searchWithSubdivision = searchWithSubdivision() && subdivisions != null;
        if ( !searchWithSubdivision ){
        	printSubdivisionException();
        }
        
        
        
	     
	    {
       	   	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
       	   	m.clearSaKeys();
       	   	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
       	   			TSServerInfoConstants.VALUE_PARAM_LIST_AO_INSTR);   
       	   	m.addIterator(getInstrumentIterator());
       	   	m.getFunction(FROMI_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
    	   	m.getFunction(TOI_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
    	   
	        m.addValidator(subdivisionNameValidator);
			m.addValidator(addressHighPassValidator);
			m.addValidator(pinValidator);
	        m.addValidator(defaultLegalValidator);
	        m.addCrossRefValidator(subdivisionNameValidator);
	        m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
	        m.addCrossRefValidator(defaultLegalValidator); 
	       	if(validateWithDates) {
	       		m.addValidator( recordedDateValidator );
	       		m.addCrossRefValidator( recordedDateValidator );
	       	}
		   	l.add(m);
	    }
        
	    
	    {
	    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
       	   	m.clearSaKeys();
       	   	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
       			   TSServerInfoConstants.VALUE_PARAM_LIST_AO_BP);
       	   	m.addIterator(new InstrumentGenericIterator(searchId).enableBookPage());
       	   	m.getFunction(BOOK_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
       	   	m.getFunction(PAGE_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
    	   
	        m.addValidator(subdivisionNameValidator);
			m.addValidator(addressHighPassValidator);
			m.addValidator(pinValidator);
	        m.addValidator(defaultLegalValidator);
	        m.addCrossRefValidator(subdivisionNameValidator);
	        m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
	        m.addCrossRefValidator(defaultLegalValidator);   
	       	if(validateWithDates) {
	       		m.addValidator( recordedDateValidator );
	       		m.addCrossRefValidator( recordedDateValidator );
	       	}
       	   	l.add(m);
	    }
        
        
        
        
        
        if (searchWithSubdivision) {
        	for (int i = 0; i < subdivisions.length; i++) {
        		
        		//search with subdivision at grantee and doctype plat
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	            m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	            		TSServerInfoConstants.VALUE_PARAM_GRANTEE_NAME_SUBDIVISION_PLAT);
	            m.clearSaKeys();
	            m.setData(GTEE_IDX, subdivisions[i]);
		        m.forceValue(DTYP_IDX, "PLAT");
		        try {
		        	String startDate = (validateWithDates) ? "Jan 1, 1960"
		        									 : sa.getAtribute(SearchAttributes.START_HISTORY_DATE);
		        	m.setData(FROMD_IDX, writeDateFormat.format(SearchAttributes.DEFAULT_DATE_PARSER.parse(startDate)));
		        } catch (Exception e) {
					e.printStackTrace();
				}
		        m.addFilter(subdivName);
		        m.addFilter(subdivName(searchId));
		        
		        //m.addValidator(registerValidatorJackson); // removed lot, block comparison from it
		        m.addValidator(subdivisionNameValidator);
		        m.addValidator(defaultLegalValidator);
		        m.addCrossRefValidator(subdivisionNameValidator);
		        m.addCrossRefValidator(defaultLegalValidator);
		        
		        l.add(m);
		        
		        //search with subdivision at grantor and doctype plat
		        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	            m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	            		TSServerInfoConstants.VALUE_PARAM_GRANTOR_NAME_SUBDIVISION_PLAT);
	            m.clearSaKeys();
	            m.setData(GTOR_IDX, subdivisions[i]);
		        m.forceValue(DTYP_IDX, "PLAT");
		        try {
		        	String startDate = (validateWithDates) ? "Jan 1, 1960"
							 						 : sa.getAtribute(SearchAttributes.START_HISTORY_DATE);
		        	m.setData(FROMD_IDX, writeDateFormat.format(SearchAttributes.DEFAULT_DATE_PARSER.parse(startDate)));
		        } catch (Exception e) {
					e.printStackTrace();
				}
		        m.addFilter(subdivName);
		        m.addFilter(subdivName(searchId));
		        
		        m.addValidator(subdivisionNameValidator);
		        m.addValidator(defaultLegalValidator);
		        m.addCrossRefValidator(subdivisionNameValidator);
		        m.addCrossRefValidator(defaultLegalValidator);
		        
		        l.add(m);
		        
		        SearchAttributes ref = (SearchAttributes) sa.clone();
   	            ref.setAtribute( SearchAttributes.LD_SUBDIV_NAME, subdivisions[i] );
   	            
   	            //search with subdivision name at subdivision and doctype plat
		        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
   	            m.clearSaKeys();  
   	            m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
   	            m.setSaForComparison( ref );
   	            m.setStringCleaner( StringCleaner.JACKSON_SUBDIV );                
   		        m.setSaObjKey(SearchAttributes.NO_KEY);
            
   		        m.setData(SUB_IDX,subdivisions[i]);
   		        m.forceValue(DTYP_IDX, "PLAT");
                
   		        try {
		        	String startDate = (validateWithDates) ? "Jan 1, 1960"
							 						 : sa.getAtribute(SearchAttributes.START_HISTORY_DATE);
		        	m.setData(FROMD_IDX, writeDateFormat.format(SearchAttributes.DEFAULT_DATE_PARSER.parse(startDate)));
		        } catch (Exception e) {
					e.printStackTrace();
				}
		        
   		        m.addFilter(subdivName);
		        m.addFilter(subdivName(searchId));
   		        
		        m.addValidator(subdivisionNameValidator);
		        m.addValidator(defaultLegalValidator);
		        m.addCrossRefValidator(subdivisionNameValidator);
		        m.addCrossRefValidator(defaultLegalValidator);              
   		        
		        l.add(m);
		        
		        if(!StringUtils.isEmpty(block) && StringUtils.isEmpty(lot)){
		        	//subdivision + block  (if we do not have lot)
		        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));			        
			        m.clearSaKeys();
	                m.setSaForComparison( ref );
	                m.setStringCleaner( StringCleaner.JACKSON_SUBDIV );
	                m.setData(SUB_IDX, subdivisions[i]);
	                m.setSaKey(BLOCK_IDX,SearchAttributes.LD_SUBDIV_BLOCK);
	                
	                m.addFilter(subdivName);
			        m.addFilter(subdivName(searchId));
			        
			        if(validateWithDates) {
			        	m.addValidator( recordedDateValidator );
			        	m.addCrossRefValidator( recordedDateValidator );
			        }
			        m.addValidator(subdivisionNameValidator);
					m.addValidator(addressHighPassValidator);
					m.addValidator(pinValidator);
			        m.addValidator(defaultLegalValidator);
			        m.addCrossRefValidator(subdivisionNameValidator);
			        m.addCrossRefValidator(addressHighPassValidator);
					m.addCrossRefValidator(pinValidator);
			        m.addCrossRefValidator(defaultLegalValidator);
			        
			        l.add(m);
		        } else {
		        	//subdivision + lot  (if we have lot) or just subdivision
		        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			        m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
			        		TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT);
			        m.clearSaKeys();
	                m.setSaForComparison( ref );
	                m.setStringCleaner( StringCleaner.JACKSON_SUBDIV );
	               
			        m.setIteratorType(ModuleStatesIterator.TYPE_LOT_SEARCH);
			        m.setData(SUB_IDX, subdivisions[i]);
	                m.setSaKey(LOT_IDX,SearchAttributes.LD_LOTNO);
	                m.setSaKey(BLOCK_IDX,SearchAttributes.LD_SUBDIV_BLOCK);
	                m.setDefaultValue(FROMD_IDX, "");
	                m.setDefaultValue(TOD_IDX, "");
			        
			        m.addFilter(subdivName);
			        m.addFilter(subdivName(searchId));
			        m.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
			        
			        if(validateWithDates) {
			        	m.addValidator( recordedDateValidator );
			        	m.addCrossRefValidator( recordedDateValidator );
			        }			        
			        m.addValidator(subdivisionNameValidator);
					m.addValidator(addressHighPassValidator);
					m.addValidator(pinValidator);
			        m.addValidator(defaultLegalValidator);
			        m.addCrossRefValidator(subdivisionNameValidator);
			        m.addCrossRefValidator(addressHighPassValidator);
					m.addCrossRefValidator(pinValidator);
			        m.addCrossRefValidator(defaultLegalValidator);
	       
			        l.add(m);
		        }
		        
		        //search with subdivision and doctype Restriction
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	            m.clearSaKeys();
	            m.setData(GTEE_IDX, subdivisions[i]);
                m.forceValue(DTYP_IDX, "REST");
		        
                m.addFilter(subdivName);
		        m.addFilter(subdivName(searchId));  
		        
		        m.addValidator(subdivisionNameValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
		        m.addValidator(defaultLegalValidator);
		        m.addCrossRefValidator(subdivisionNameValidator);
		        m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
		        m.addCrossRefValidator(defaultLegalValidator);
		        if(validateWithDates) {
		        	m.addValidator( recordedDateValidator );
		        	m.addCrossRefValidator( recordedDateValidator );
		        }
		        l.add(m);
		        
		        //search with subdivision and doctype easement
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	            m.clearSaKeys();
	            m.setData(GTEE_IDX, subdivisions[i]);
                m.forceValue(DTYP_IDX, "EASE");
		        
                m.addFilter(subdivName);
		        m.addFilter(subdivName(searchId));  
		        
		        m.addValidator(subdivisionNameValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
		        m.addValidator(defaultLegalValidator);
		        m.addCrossRefValidator(subdivisionNameValidator);
		        m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
		        m.addCrossRefValidator(defaultLegalValidator);
		        if(validateWithDates) {
		        	m.addValidator( recordedDateValidator );
		        	m.addCrossRefValidator( recordedDateValidator );
		        }
		        l.add(m);		        
		        
        	}
        }
        
           	
        ConfigurableNameIterator nameIterator = null;
	    if (hasOwner()) {
        	for(int i=0; i<2; i++){
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	            
	            DocsValidator dateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
	            
	            m.clearSaKeys();
	            m.clearIteratorTypes();
	            m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
	            m.setIteratorType(i,  FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE );
	            m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT_IGNORE_MIDDLE);
	            
	            m.forceValue(FROMD_IDX, "01/01/1960");
	            m.setIteratorType(FROMD_IDX, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
                m.setSaKey(TOD_IDX, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
	            
	            m.addValidator( defaultLegalValidator );
				m.addValidator( addressHighPassValidator );
		        m.addValidator( pinValidator );
		        m.addValidator( dateValidator );
		        m.addValidator(lastTransferDateValidator);
		        addFilterForUpdate(m, false);
				m.addCrossRefValidator( defaultLegalValidator );
				m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( dateValidator );
		        m.addCrossRefValidator(lastTransferDateValidator);
		        
		        nameIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L F;;"} );
		        nameIterator.setAllowMcnCompanies(false);
		        m.addIterator( nameIterator );
	            l.add(m);
        	}
        }    
	    
          	
	    //OCR last transfer
	    m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	    m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    		TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP_INST);
	    m.clearSaKeys();
	    m.setIteratorType(ModuleStatesIterator.TYPE_OCR);
	    m.getFunction(FROMI_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	    m.getFunction(TOI_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	    m.getFunction(BOOK_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
	    m.getFunction(PAGE_IDX).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
  
        m.addValidator(subdivisionNameValidator);
		m.addValidator(addressHighPassValidator);
		m.addValidator(pinValidator);
        m.addValidator(defaultLegalValidator);
        m.addCrossRefValidator(subdivisionNameValidator);
        m.addCrossRefValidator(addressHighPassValidator);
		m.addCrossRefValidator(pinValidator);
        m.addCrossRefValidator(defaultLegalValidator);      
	    l.add(m);
      		  
	    //grantor grantee search with names brought by OCR or Transfer Index
	    ArrayList< NameI> searchedNames =((nameIterator==null)?new ArrayList< NameI>():nameIterator.getSearchedNames());
	    for( int i=0; i<2; i++ ){
	    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	    	m.clearSaKeys();
  	        m.clearIteratorTypes();
  	        m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
  	        m.setIteratorType(i,  FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE );
  	        m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
  	        m.addValidator( defaultLegalValidator );
  			m.addValidator( addressHighPassValidator );
  	        m.addValidator( pinValidator );
  	        m.addValidator( recordedDateValidator );
  	        m.addValidator(lastTransferDateValidator);
  			m.addCrossRefValidator( defaultLegalValidator );
  			m.addCrossRefValidator( addressHighPassValidator );
  	        m.addCrossRefValidator( pinValidator );
  	        m.addCrossRefValidator( recordedDateValidator );
  	        m.addCrossRefValidator(lastTransferDateValidator);
  	        
  	        nameIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L F;;"} );
  	        nameIterator.setInitAgain( true );
  			nameIterator.setSearchedNames( searchedNames );
  	        m.addIterator( nameIterator  );
  	        l.add(m);
  		}
             
        serverInfo.setModulesForAutoSearch(l);
	}

	@Override
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
			  
		   
	    	for(int i=0; i<2; i++){ 
	    	 module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		   
		     module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
		     module.setIteratorType(i,  FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
		     String date=gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
		     if (date!=null)
		    	 module.getFunction(FROMD_IDX).forceValue(date);
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
		 	 module.addIterator(nameIterator);
		 	 module.addValidator( defaultLegalValidator );
			 module.addValidator( addressHighPassValidator );
	         module.addValidator( pinValidator );
	         module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
			 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
		     modules.add(module);
	    	}
	    	
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
		    	for(int i=0; i<2; i++){
		    	 module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				// module.addFilter(NameFilterFactory.getNameFilterForNext(SearchAttributes.GB_MANAGER_OBJECT,searchId,module,true));
				 module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
				 module.setIteratorType(i,  FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				 String date=gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
			     if (date!=null)
			    	 module.getFunction(FROMD_IDX).forceValue(date);
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
				 module.addIterator(nameIterator);
				 module.addValidator( defaultLegalValidator );
				 module.addValidator( addressHighPassValidator );
			     module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
				 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
				 modules.add(module);
			 
		      }
		     }
	   
	    }	 
		     
      serverInfo.setModulesForGoBackOneLevelSearch(modules);
	    
	    }
	
	
	/**
	 * Retrieve an image starting from the link
	 * @param link
	 * @param fileName
	 * @return
	 */
	private boolean retrieveImage(String link, String fileName){
		
		HttpSite site = HttpManager.getSite("MOJacksonRO", searchId);
		try {
			boolean result = retrieveImageFromLink(link, fileName, site);
			afterDownloadImage(result);
		} finally {
			HttpManager.releaseSite(site);
		}

		return FileUtils.existPath(fileName);
	}
	
	/**
	 * Function to be used both by internal retrieveImage() and external retrieveImageFromDocNo()
	 * @param link
	 * @param fileName
	 * @param site
	 * @return
	 */
	private static boolean retrieveImageFromLink(String link, String fileName, HttpSite site){

		// check whether the file exists
		if(FileUtils.existPath(fileName)){
			return true;
		}
		
		// go to 1st link
		String link1 = "http://records.jacksongov.org/" + link;
		link1 = link1.replace("?", "&").replaceFirst("&", "?");
		link1 = link1.replaceFirst("&docNo=[^&]*", ""); 
		HTTPRequest req1 = new HTTPRequest(link1, HTTPRequest.GET);
		String page1 = site.process(req1).getResponseAsString();		
		String link2 = StringUtils.extractParameter(page1, "action=\"/docimage\\.asp\\?([^\"]*)\"");
		if(StringUtils.isEmpty(link2)){
			return false;
		}
		// <INPUT type="text" name=startPage size=10 value=1></td>
		String startPage = StringUtils.extractParameter(page1, "(?i)<INPUT\\s+type=\"text\"\\s+name=startPage\\s+size=\\d+\\s+value=(\\d+)>");
		// <INPUT type="text" name=endPage size=10  value=2>
		String endPage = StringUtils.extractParameter(page1, "(?i)<INPUT\\s+type=\"text\"\\s+name=endPage\\s+size=\\d+\\s+value=(\\d+)>");
		if(StringUtils.isEmpty(startPage) || StringUtils.isEmpty(endPage)){
			return false;
		}
		
		// go to 2nd link
		link2 = "http://records.jacksongov.org/docimage.asp?" + link2;
		HTTPRequest req2 = new HTTPRequest(link2, HTTPRequest.POST);
		req2.setPostParameter("startPage", startPage);
		req2.setPostParameter("endPage", endPage);
		req2.setPostParameter("imgRetrieve", "Retrieve as TIFF");
		String page2 = site.process(req2).getResponseAsString();
		String link3 = StringUtils.extractParameter(page2, "<a target=_new href=\"(/imgcache/[^.]*\\.tif)\">");
		if(StringUtils.isEmpty(link3)){
			return false;
		}
		
		// get the image
		link3 = "http://records.jacksongov.org" + link3;
		HTTPRequest req3 = new HTTPRequest(link3, HTTPRequest.GET);
		HTTPResponse res3 = site.process(req3);
		if(!"image/tiff".equals(res3.getContentType())){
			return false;
		}
		FileUtils.CreateOutputDir(fileName);
		FileUtils.writeStreamToFile(res3.getResponseAsStream(), fileName);
		
		// return result
		return FileUtils.existPath(fileName);
	}
	
	/**
	 * Used by MOJacksonOR
	 * @param docNo
	 * @param fileName
	 * @return
	 */
	public static String retrieveImageFromDocNo(String docNo, String fileName, long searchId){
		fileName = fileName.replaceFirst("(?i)\\.pdf$", ".tiff");
		if(FileUtils.existPath(fileName)){
			return fileName;
		}
		
		HttpSite site = HttpManager.getSite("MOJacksonRO", searchId);
		try {
	
			HTTPRequest req = new HTTPRequest("http://records.jacksongov.org/results.asp", HTTPRequest.POST);
			for(String param: new String[]{"txtTor", "txtTee", "selDocType", "txtDateFiledFr", "txtDateFiledTo", "txtInstNumTo", "txtBook", "txtPage", "selLocation", "txtSubDiv", "txtSubDivTown", "txtBlock", "txtLot2", "txtCondo", "txtCondoTown", "txtCondoBlock", "txtCondoLot2", "txtPhase", "txtBldg", "txtUnit", "txtSect", "txtTown", "txtRange", "txtSurvey"} ){
				req.setPostParameter(param, "");
			}
			req.setPostParameter("txtInstNumFr", docNo);
			String page = site.process(req).getResponseAsString();
			
			String id = StringUtils.extractParameter(page, "<a href=docimage\\.asp\\?id=([^>]*)><img");
			if(!StringUtils.isEmpty(id)){
				retrieveImageFromLink("docimage.asp?id=" + id + "&docNo=" + docNo, fileName, site);
			}
			
		} catch(Exception e){
			logger.error(e);
		} finally {
			HttpManager.releaseSite(site);
		}
		
		if (FileUtils.existPath(fileName)) {
			return fileName;
		}
		return null;

	}


	@Override
	public ServerResponse GetLink(String vsRequest, boolean vbEncoded) throws ServerResponseException {
		 
		if(!vsRequest.contains("docimage.asp")){
			return super.GetLink(vsRequest, vbEncoded);
		}
			
		// get docNo
		String docNo = StringUtils.extractParameter(vsRequest, "&docNo=([^&]*)");
		
		// create output folder and determine file name
		String folderName = getCrtSearchDir() + "Register" + File.separator;
		new File(folderName).mkdirs();
    	String fileName = folderName + docNo + ".tiff";
    	
    	// retrieve image
    	String link = StringUtils.extractParameter(vsRequest, "&Link=(.*)");
    	retrieveImage(link, fileName);
    	
		// write the image to the client web-browser
		boolean imageOK = writeImageToClient(fileName, "image/tiff");
		
		// image not retrieved
		if(!imageOK){ 
	        
			// return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);
			
		} else {
			
			// return solved response
			return ServerResponse.createSolvedResponse();    	
    	}
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
		
    	String fileName = image.getPath();
    	String link = StringUtils.extractParameter(image.getLink(), "&Link=(.*)");
    	
    	// retrieve image
    	if(retrieveImage(link, fileName)){
    		byte b[] = FileUtils.readBinaryFile(fileName);
    		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
    	}
    	return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );

	}
	
    protected String preProcessLink(String link){
    	String proc = super.preProcessLink(link);
    	String insNo = StringUtils.extractParameter(link, "insno=([^&]*)");
    	if(!StringUtils.isEmpty(insNo)){
    		proc = "DETECTED-INSTO-" + insNo;
    	}
    	return proc;
    }
    
	@Override
	protected void setCertificationDate() {
        try {
			if(false) {
				if (CertificationDateManager.isCertificationDateInCache(dataSite)){
					String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
					getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
				} else{
		        	String html = HttpUtils.downloadPage("http://records.jacksongov.org/search.asp?cabinet=opr");
		            Matcher certDateMatcher = certDatePattern.matcher(html);
		            if(certDateMatcher.find()) {
		            	String date = certDateMatcher.group(1).trim();
		            	
		            	CertificationDateManager.cacheCertificationDate(dataSite, date);
		            	getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
		            }
				}
			}
        } catch (Exception e) {
            logger.error(e.getMessage());
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
		
		TSServerInfoModule module = null;
		if(StringUtils.isNotEmpty(instrumentNumber)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX);
		    module.forceValue(FROMI_IDX, instrumentNumber);
		    if(restoreDocumentDataI.getRecordedDate() != null) {
				module.forceValue(FROMD_IDX, writeDateFormat.format(restoreDocumentDataI.getRecordedDate()));
				module.forceValue(TOD_IDX, writeDateFormat.format(restoreDocumentDataI.getRecordedDate()));
		    }
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX);
			module.forceValue(BOOK_IDX, book);
			module.forceValue(PAGE_IDX, page);
		    if(restoreDocumentDataI.getRecordedDate() != null) {
				module.forceValue(FROMD_IDX, writeDateFormat.format(restoreDocumentDataI.getRecordedDate()));
				module.forceValue(TOD_IDX, writeDateFormat.format(restoreDocumentDataI.getRecordedDate()));
		    }
		} 
		
		return module;
	}
	
	private InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator iterator = new InstrumentGenericIterator(searchId) {
			
			private Set<String> notSearchable = new HashSet<String>();
			private Set<String> searchable = new HashSet<String>();
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected String cleanInstrumentNo(String inst, int year) {
				
				String instNo = inst.replaceFirst("^0+", "");
				if(instNo.isEmpty()) {
					return instNo;
				}
				if(year == SimpleChapterUtils.UNDEFINED_YEAR || !instNo.matches("\\d+[a-zA-Z]")){
					notSearchable.add(inst);
					return "";
				} else {
					searchable.add(inst);
				}
				instNo = org.apache.commons.lang.StringUtils.leftPad(instNo, 8, "0");
				
				instNo = year + instNo.substring(7) + instNo.substring(0, 7);
				
				return instNo;
			}
			
			@Override
			public List<InstrumentI> createDerrivations() {
				List<InstrumentI> derivations = super.createDerrivations();
				
				for (String intrument : notSearchable) {
					if(!searchable.contains(intrument)) {
						SearchLogger.info("Will not search with instrument [" + intrument+ "] because of not enough data.<br>", searchId);
					}
				}
				
				return derivations;
			}
			
		};
		return iterator;
	}
}

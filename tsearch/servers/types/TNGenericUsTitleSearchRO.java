/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static org.apache.commons.lang.StringUtils.substringBetween;
import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_SELECT_BOX;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.createHiddenHTMLControl;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setRequiredCriticalMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setRequiredExclMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;

import java.io.File;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.corrector.HTMLCorrector;
import ro.cst.tsearch.data.CountyState;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.PassManager;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author radu bacrau
 *
 */
@SuppressWarnings("serial")
abstract public class TNGenericUsTitleSearchRO extends TSServer implements TSServerROLikeI {
	
	protected static final int NAME_MODULE_IDX = 2;
	protected static final int SUBD_MODULE_IDX = 3;
	protected static final int BP_MODULE_IDX   = 4;
	protected static final int INST_MODULE_IDX = 5;
	
	protected static final int ID_NAME = 100 + NAME_MODULE_IDX;
	protected static final int ID_SUBD = 100 + SUBD_MODULE_IDX;
	protected static final int ID_BP   = 100 + BP_MODULE_IDX;
	protected static final int ID_INST = 100 + INST_MODULE_IDX;
	protected String qury = "";
	protected TSServerInfoModule ini;
	private static final Pattern certDatePattern = Pattern.compile("(?ism)Indexes are current as of (.+?)\\s");
	
	protected static final String PARTY_TYPE_SELECT = 
		"<select NAME=\"PartyType\" size=\"1\">" + 
		"<option value=\"Direct\">Grantor (Seller)</option>" +                            
		"<option value=\"Reverse\">Grantee (Buyer)</option>" +                            
		" <option value=\"Both\" SELECTED>Both</option>" +                            
		"</select>";
	
	protected String class_select = "";
	protected String instr_type_select = "";
	protected String party_type_select = "";
	
	protected String[] platTypeIndexes = new String[0];
	protected String[] easementTypeIndexes = new String[0];
	protected String[] restrictionTypeIndexes = new String[0];
	protected String[] bookPageTypeIndexes = new String[0];
	protected String[] platClassIndexes = new String[0];
		
	
	protected static Map<String,Map<CountySpecificInfo,String>> parentSiteInfo = new Hashtable<String,Map<CountySpecificInfo,String>>();
	protected static enum CountySpecificInfo {
		CLASS_SELECT,
		INSTR_TYPE_SELECT,
		PLAT_TYPES,
		EASEMENT_TYPES,
		RESTRICTION_TYPES,
		BOOK_PAGE_TYPES,
		/**
		 * Should refer the Plat entry from Class Select in Book/Page module
		 */
		PLAT_CLASS_TYPE
	}
	
	static {
		loadParentSiteData();
	}
	
	/** used to initialize the select fields */
	abstract protected void initFields();
	
	/** used to extract hidden parameters */
	private static final Pattern hidParamPattern = Pattern.compile("<input name=\"([^\"]+)\" type=\"hidden\" value=\"([^\"]*)\">");
	
	/** mark that we want to save the crt doc */
	private boolean downloadingForSave = false;
	
	/** for compatibility with old searches. not really used */
	@SuppressWarnings("unused")
	private transient PassManager pm = null;
	
	/**
	 * @param searchId
	 */
	public TNGenericUsTitleSearchRO(long searchId) {
		super(searchId);
		initFields();
		/*
		if(!(this instanceof TNGenericUsTitleSearchDefaultRO)) {
			docsValidatorType = DocsValidator.TYPE_REGISTER_SHELBY;	
		}
		*/
		resultType = MULTIPLE_RESULT_TYPE;			
	}

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param miServerID
	 */
	public TNGenericUsTitleSearchRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {		
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		initFields();
		/*
		if(!(this instanceof TNGenericUsTitleSearchDefaultRO)) {
			docsValidatorType = DocsValidator.TYPE_REGISTER_SHELBY;	
		}
		*/
		resultType = MULTIPLE_RESULT_TYPE;		
	}

	@Override
	public TSServerInfo getDefaultServerInfo(){
		
		initFields();
		
		TSServerInfo si = new TSServerInfo(4);
		si.setServerAddress("wwwX.ustitlesearch.net");
		si.setServerIP("wwwX.ustitlesearch.net");
		si.setServerLink("http://wwwX.ustitlesearch.net" );
		
        // build start date and end date formatted strings 
        String startDate="", endDate="";
        try{
        	SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
            SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
            Date start = sdf.parse(sa.getAtribute(SearchAttributes.FROMDATE));	            
            Date end = sdf.parse(sa.getAtribute(SearchAttributes.TODATE));	            
            sdf.applyPattern("MM/dd/yyyy");	            
            startDate = sdf.format(start);
            endDate = sdf.format(end);	            
        }catch (ParseException e){}
        
		// Party Name Search
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(NAME_MODULE_IDX, 12);
			sim.setName("Name");
			sim.setDestinationPage("/PartyNameSearchResults.asp");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_NAME);
			sim.setSearchType("GI");

			PageZone pz = new PageZone("PartyNameSearch", "Party Name Search", ORIENTATION_HORIZONTAL, null, 550, 50, PIXELS , true);

			try{				
	            HTMLControl 
	            name  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1,35, sim.getFunction(0), "PartyName",             "Party Name",      null,      searchId),
	            type  = new HTMLControl(HTML_SELECT_BOX, 1, 1,  2,  2, 1, sim.getFunction(10),"PartyType",             "Party Type",      "-1",      searchId),
	            sdate = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  3,  3,10, sim.getFunction(3), "BeginningDate",         "Beginning Date",  startDate, searchId),
	            edate = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  4,  4,10, sim.getFunction(4), "EndingDate",            "Ending Date",     endDate,   searchId),
	            instr = new HTMLControl(HTML_SELECT_BOX, 1, 1,  5,  5, 1, sim.getFunction(6), "InstrumentType",        "Instrument Type", "0",       searchId),
	            size  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  6,  6, 4, sim.getFunction(9), "PageSize",              "Page Size",       "25",      searchId),
	            
	            subd  = createHiddenHTMLControl(sim.getFunction(5),  "IncludeSubdivisions", "IncludeSubdivisions", "ON",     searchId),
	            act   = createHiddenHTMLControl(sim.getFunction(2),  "Action", 				"Action",              "SEARCH", searchId),
	            page  = createHiddenHTMLControl(sim.getFunction(7),  "Page", 				"Page",                "1",      searchId),
	            pbas  = createHiddenHTMLControl(sim.getFunction(8),  "PageBase", 			"PageBase",            "1",      searchId),
	            lix   = createHiddenHTMLControl(sim.getFunction(11), "I2.x", 				"I2.x",                "47",     searchId),
	            liy   = createHiddenHTMLControl(sim.getFunction(1),  "I2.y", 				"I2.y",                "6",      searchId);
	            
	            name.setFieldNote("(e.g. SMITH JOHN W)");
	            sdate.setFieldNote("(mm/dd/yyyy)");
	            edate.setFieldNote("(mm/dd/yyyy)");
	            setupSelectBox(sim.getFunction(10), party_type_select);
	            setupSelectBox(sim.getFunction(6), instr_type_select);
	           
	            setRequiredCriticalMulti(true, name);
	            setRequiredCriticalMulti(true, size);
	            
	            pz.addHTMLObjectMulti(name, type, sdate, edate, instr, size, subd, act,page, pbas, lix, liy);

	            sim.getFunction(0).setSaKey(SearchAttributes.OWNER_LFM_NAME);
	            	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);
		}
		
		// Subdivision Search
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(SUBD_MODULE_IDX, 16);
			sim.setName("Subdivision");
			sim.setDestinationPage("/SubdivisionSearchResults.asp");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_SUBD);
			sim.setSearchType("LS");

			PageZone pz = new PageZone("SubdivisionSearch", "Subdivision Search", ORIENTATION_HORIZONTAL, null, 550, 50, PIXELS , true);

			try{				
	            HTMLControl 
	            
	            subd  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1,47, sim.getFunction(0), "Subdivision",     "Subdivision",     null,      searchId),
	            sdate = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2,  2,10, sim.getFunction(8), "BeginningDate",   "Beginning Date",  startDate, searchId),
	            edate = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  3,  3,10, sim.getFunction(9), "EndingDate",      "Ending Date",     endDate,   searchId),
	            instr = new HTMLControl(HTML_SELECT_BOX, 1, 1,  4,  4, 1, sim.getFunction(13),"InstrumentType",  "Instrument Type", "0",       searchId),
	            sec   = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  5,  5, 4, sim.getFunction(1), "Section",         "Section",         null,      searchId),
	            pha   = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  6,  6, 4, sim.getFunction(2), "Phase",           "Phase",           null,      searchId),
	            lot   = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  7,  7, 4, sim.getFunction(3), "Lot",             "Lot",             null,      searchId),
	            bldg  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  8,  8, 4, sim.getFunction(5), "Building",        "Building",        null,      searchId),
	            unit  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  9,  9, 4, sim.getFunction(4), "Unit",            "Unit",            null,      searchId),
	            distr = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 10, 10, 4, sim.getFunction(6), "District",        "District",        null,      searchId),
	            size  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 11, 11, 4, sim.getFunction(12),"PageSize",        "Page Size",       "25",      searchId),	            

	            act   = createHiddenHTMLControl(sim.getFunction(7), "Action", 	 "Action",   "SEARCH", searchId),
	            page  = createHiddenHTMLControl(sim.getFunction(10), "Page", 	 "Page",     "1",      searchId),
	            pbas  = createHiddenHTMLControl(sim.getFunction(11), "PageBase", "PageBase", "1",      searchId),
	            lix   = createHiddenHTMLControl(sim.getFunction(14), "I3.x", 	 "I3.x",     "74",     searchId),
	            liy   = createHiddenHTMLControl(sim.getFunction(15), "I3.y", 	 "I3.y",     "5",      searchId);
	            	            
	            setupSelectBox(sim.getFunction(13), instr_type_select);
	           
	            setRequiredCriticalMulti(true, subd);
	            setRequiredCriticalMulti(true, size);

	            pz.addHTMLObjectMulti(subd, sdate, edate, instr, sec, pha, lot, bldg, unit, distr, size, act, page, pbas, lix, liy);

	            sim.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
	            sim.getFunction(1).setSaKey(SearchAttributes.LD_SUBDIV_SEC);
	            sim.getFunction(2).setSaKey(SearchAttributes.LD_SUBDIV_PHASE);
	            sim.getFunction(3).setSaKey(SearchAttributes.LD_LOTNO);
	            sim.getFunction(4).setSaKey(SearchAttributes.LD_SUBDIV_UNIT);
	            	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);
		}

		// Book And Page Search
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(BP_MODULE_IDX, 6);
			sim.setName("BookAndPage");
			sim.setDestinationPage("/bookandpagesearchresults.asp");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_BP);
			sim.setSearchType("IN");

			PageZone pz = new PageZone("BookAndPageSearch", "Book And Page Search", ORIENTATION_HORIZONTAL, null, 550, 50, PIXELS , true);

			try{				
	            HTMLControl 
	            
	            clas  = new HTMLControl(HTML_SELECT_BOX, 1, 1,  1,  1, 1, sim.getFunction(5), "Class",        "Class",         "-1", searchId),
	            book  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2,  2,10, sim.getFunction(0), "Book",         "Book",          null, searchId),
	            page  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  3,  3,10, sim.getFunction(1), "Page",         "Page",          null, searchId),
	            file  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  4,  4,20, sim.getFunction(4), "FilingNumber", "Filing Number", null, searchId),

	            lix   = createHiddenHTMLControl(sim.getFunction(2), "I1.x", "I1.x", "45", searchId),
	            liy   = createHiddenHTMLControl(sim.getFunction(3), "I1.y", "I1.y", "8",  searchId);
	            
	            setupSelectBox(sim.getFunction(5), class_select);
	           
	            setRequiredExclMulti(true, book, page, file);

	            pz.addHTMLObjectMulti(clas, book, page, file, lix, liy);

	            sim.getFunction(0).setSaKey(SearchAttributes.LD_BOOKNO);
	            sim.getFunction(1).setSaKey(SearchAttributes.LD_PAGENO);
	            
	            
	            	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);
		}
		
		// Instrument Search
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(INST_MODULE_IDX, 3);
			sim.setName("Instrument");
			sim.setDestinationPage("/InstrumentNumberSearchResults.asp");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_INST);
			sim.setSearchType("IN");

			PageZone pz = new PageZone("InstrumentSearch", "Instrument Search", ORIENTATION_HORIZONTAL, null, 550, 50, PIXELS , true);

			try{				
	            HTMLControl 
	            
	            instr = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 20, sim.getFunction(0), "InstrumentNumber", "Instrument Number", null, searchId),

	            lix   = createHiddenHTMLControl(sim.getFunction(1), "I1.x", "I1.x", "71", searchId),
	            liy   = createHiddenHTMLControl(sim.getFunction(2), "I1.y", "I1.y", "11", searchId);
	            
	            setupSelectBox(sim.getFunction(1), class_select);
	           
	            setRequiredCriticalMulti(true, instr);

	            pz.addHTMLObjectMulti(instr, lix, liy);

	            sim.getFunction(0).setSaKey(SearchAttributes.LD_INSTRNO);
	            	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);
		}

		si.setupParameterAliases();
		setModulesForAutoSearch(si);
		
        return si;
	}
	
/* Pretty prints a link that was already followed when creating TSRIndex
 * (non-Javadoc)
 * @see ro.cst.tsearch.servers.types.TSServer#getPrettyFollowedLink(java.lang.String)
 */	
    public String getPrettyFollowedLink (String initialFollowedLnk)
    {
    	if (initialFollowedLnk.matches("(?i).*DL[_]{2,}([a-zA-Z0-9]+)[_]{1,}([a-zA-Z0-9]+)[^a-z]*([a-z]+).*"))
    	{
/*"Book 13676 Page 1504 which is a Court doc type has already been saved from a
previous search in the log file."*/
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*DL[_]{2,}([a-zA-Z0-9]+)[_]{1,}([a-zA-Z0-9]+)[^a-z]*([a-z]*)[^a-z]*.*", 
    				"Book " + "$1" + " Page " + "$2" + " " + "$3" + " has already been processed from a previous search in the log file. ");
    		retStr =  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    		return retStr;
    	}
    	
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
	
    protected boolean fakeDocumentAlreadyExists(ServerResponse response, String htmlContent, boolean forceOverritten, DocumentsManagerI manager, DocumentI doc){
    	if((doc.isFake()||"MISCELLANEOUS".equals(doc.getDocType())) && manager.flexibleContains(doc)){    	
	    	if(response.getParsedResponse().getImageLinksCount() > 0){
				List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, doc);
				if(!almostLike.isEmpty()){
					String refDummy = response.getParsedResponse().getImageLink(0).getLink().replaceAll("(?ism).*dummy=([^&]*).*","$1");
					
					for (DocumentI documentI : almostLike) {
						if(documentI.hasImage()){
							//get dummy from link
							String dummy = documentI.getImage().getLink(0).replaceAll("(?ism).*dummy=([^&]*).*","$1");
							
							if(dummy.equals(refDummy)){
								return true;
							}
						}
					}
				}
			}
    	}
    	
		return false;
	}
    
	@Override
	protected void ParseResponse(String action, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String response = Response.getResult();
		String contents = "";
		
		// check not found messages
		if(response.contains("Instrument number was not found!") ||
		   response.contains("Book and Page was not found!") ||
		   response.contains("No records were found!")){
			return;
		}
		
		switch (viParseID){
		case ID_NAME:
		case ID_SUBD:
			
			// extract intermediate table and check it
			contents = extractIntermTable(response);
			if(StringUtils.isEmpty(contents)){
				return;
			}
			
			// make "Item" to be link
			contents = contents.replace("&nbsp;Item&nbsp;", "<div>" + SELECT_ALL_CHECKBOXES + " All Items </div>");
			
			// make sub-tables stand out
			contents = contents.replaceAll("<th bgcolor=\"#000099\"", "<th bgcolor=\"#222222\"");
			contents = contents.replaceAll("<th width=\"100%\" bgcolor=\"#000099\"", "<th width=\"100%\" bgcolor=\"#222222\"");
			
			String [] navLinks = getNavLinks(response);
			String prev = navLinks[0];
			String next = navLinks[1];
			contents += "<br/>";
			
			if(!StringUtils.isEmpty(prev)){
				contents += " " + prev + " ";
			}
			if(!StringUtils.isEmpty(next)){
				contents += " " + next + " ";
			}
			
			String page_x_of_y = " " + getLocation(response) + "<br/><br/>";
			
			contents +=  page_x_of_y;
			
			contents = contents.replaceAll("(?i)(?s)(<td[^>]*>)(<a href=\"([^\"]*)\">.*?</a>)",
			"<td><input type=\"checkbox\" name=\"docLink\" value=\"$3\"></td><td>$2");

			contents = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + 
					   contents +
					   CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);

			int parseRowsId = Parser.PAGE_ROWS;

			if (viParseID == ID_SUBD) {
				parseRowsId = Parser.PAGE_ROWS_SUBDIV_CODE;
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, response, outputTable);
											
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
						parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
		            }
	
				}catch(Exception e) {
					e.printStackTrace();
				}
			}else {
				parser.Parse(Response.getParsedResponse(), contents, parseRowsId, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);	
			}
			Response.getParsedResponse().setNextLink(next);

			Vector parsedRows = Response.getParsedResponse().getResultRows();
			
			
			for (Object s : parsedRows) {
				ParsedResponse pr= (ParsedResponse) s;
				
				ServerResponse serverResponse = GetLink(pr.getPageLink().getLink(), true);
				String result = serverResponse.getResult();
				if (result==null) {		//T7596 (when skipping RO result is null)
					result = "";
				}
				DocumentI registerDocument =  pr.getDocument();
				String book = registerDocument.getBook();
				String page = registerDocument.getPage();
				String instno = registerDocument.getInstrument().getInstno();
				String extractedInstrumentNumber = extractInstrumentNumber(result);
				instno=(extractedInstrumentNumber!=null&&!extractedInstrumentNumber.equals(""))? extractedInstrumentNumber:instno;
				if(org.apache.commons.lang.StringUtils.isBlank(registerDocument.getInstrument().getInstno()) 
						&& org.apache.commons.lang.StringUtils.isNotBlank(instno)) {
					registerDocument.setInstno(instno);
				}
				
				String docType = extractDocumentType(result);
				registerDocument.getType();
 				HashMap<String, String> data = new HashMap<String,String>();
				data.put("book", book);
				data.put("page", page);
				data.put("instno", instno);
				data.put("type",docType);
				
				if(org.apache.commons.lang.StringUtils.isNotBlank(docType)) {
					docType = docType.replaceAll("\\s+", " ").toUpperCase();
			    	
			    	String docCateg = DocumentTypes.getDocumentCategory(docType, searchId); 
			    	registerDocument.getInstrument().setDocType(docCateg);
		        	String stype = DocumentTypes.getDocumentSubcategory(docType, searchId);
		        	if("MISCELLANEOUS".equals(stype)&&!"MISCELLANEOUS".equals(docCateg)){
		        		stype = docCateg;
		        	}
		        	registerDocument.getInstrument().setDocSubType(stype);
		        	registerDocument.getInstrument().setServerDocType(docType);
				}
				
				if(isInstrumentSaved(instno, registerDocument, data, false)){
					pr.setResponse(pr.getResponse().replaceAll("(?m)<input type=\"checkbox\".*?(?=</td>)", "saved"));
				}
			}
			
			break;
			
		case ID_GET_LINK:
			if(action.contains("/PartyNameSearchResults.asp")){
				ParseResponse(action, Response, ID_NAME);
			} else if(action.contains("/SubdivisionSearchResults.asp")){
				ParseResponse(action, Response, ID_SUBD);
			} else if(action.contains("/InstrumentDisplay.asp")){
				ParseResponse(action, Response, ID_DETAILS);
			} else {
				Response.getParsedResponse().setError("Did not know hot to handle: " + action);
			}
			break;
			
		case ID_DETAILS:
		case ID_BP:
		case ID_INST:		
			viParseID = ID_DETAILS;
			qury = Response.getQuerry();
			HashMap<String,String> data = new HashMap<String, String>();
			Pattern pattern = Pattern.compile("(?is)(.*)(Book=)([a-zA-Z0-9]+)(.*)");
			Matcher matcher = pattern.matcher(qury);
			String book="";
			if ( matcher.find()){
				book = qury.replaceAll("(?is)(.*)(Book=)([a-zA-Z0-9]+)(.*)", "$3");
			}
			Pattern pattern2 = Pattern.compile("(?is)(.*)(Page=)([a-zA-Z0-9]+)(.*)");
			Matcher matcher2 = pattern2.matcher(qury);
			
			String pageBook = "";
			if (matcher2.find()){
				pageBook = qury.replaceAll("(?is)(.*)(Page=)([a-zA-Z0-9]+)(.*)", "$3");
			}
			
			String instrumentNumber = org.apache.commons.lang.StringUtils.defaultString(extractInstrumentNumber(response));
			
			data.put("docno", instrumentNumber);
			data.put("book", book);
			data.put("page", pageBook);
			contents = extractDetails(response);
			if (contents == null){
				contents = extractDetailsNonIndexed(response);
			}
			if(StringUtils.isEmpty(contents)){
				return;
			}
			String id = extractId(response) + "_" + instrumentNumber;
			if (response.contains("Non-Indexed image was found!"))
				id = book + "_" + pageBook + "_" + instrumentNumber;
			
			if(StringUtils.isEmpty(id) || id.equals("_") ){				
				Response.getParsedResponse().setError("Book & Page / Filling # not found for: " + contents);
			}
			String imgLink = extractImageLink(response, id);
			if(!StringUtils.isEmpty(imgLink)){
				contents = contents + "<br/><center><A target='_blank' href='" + imgLink + "'> View image </a></center>";
				ImageLinkInPage imip = new ImageLinkInPage(imgLink, id + ".tif");
				Response.getParsedResponse().addImageLink(imip);
			}
			
			if(!downloadingForSave){
				
				String originalLink = action + "&dummy=" + id + "&" + Response.getQuerry();
                String sSave2TSDLink = "";
                
                if(action.contains("/bookandpagesearchresults.asp")){
                    sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
                } else {
                    sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
                }
                String sDoctype = extractDocumentType(response);
                if(!StringUtils.isEmpty(sDoctype)){
                	data.put("type", sDoctype);
                }

                boolean isInstrumentSaved=false;
                if(instrumentNumber!=null){
                	isInstrumentSaved = isInstrumentSaved(instrumentNumber, null, data); 
                }else{
                	isInstrumentSaved = isInstrumentSaved(id, null, data);
                }
                
				//if (FileAlreadyExist(id + ".html")) {               
                if(isInstrumentSaved){
					contents += CreateFileAlreadyInTSD();
                	
				} else {
					contents = addSaveToTsdButton(contents, sSave2TSDLink, viParseID);
//					contents = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + contents
//					+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
					mSearch.addInMemoryDoc(sSave2TSDLink, response);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(contents);
				
			} else {

				msSaveToTSDFileName = id + ".html";				
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);

				//prepare for parsing. must make a simple cleanup of the html so the xmlparsing to not 
				contents = contents.replaceAll("(?is)\\bTHE INSTRUMENT NUMBER IS\\b", "");
				
				parser.Parse(Response.getParsedResponse(), contents, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
				if(this instanceof TNGenericUsTitleSearchDefaultRO) {
					try {
						Pattern crossRefLinkPattern = Pattern.compile("(?ism)<a[^>]*?href=\"(/title-search/.*?)\">(.*?)</a>");
						Matcher crossRefLinkMatcher = crossRefLinkPattern.matcher(Response.getParsedResponse().getResponse());
						while(crossRefLinkMatcher.find()) {
							ParsedResponse prChild = new ParsedResponse();					
							String link = crossRefLinkMatcher.group(1) + "&isSubResult=true";
							LinkInPage pl = new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD);
							prChild.setPageLink(pl);
							Response.getParsedResponse().addOneResultRowOnly(prChild);
						}
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
				
				// remove cross ref links
				contents = contents.replaceAll("<a href=\".[^\"]*\">(.[^<]*)</a>", "$1");
				Response.getParsedResponse().setOnlyResponse(contents);
			}

			break;
			
		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(action, Response, ID_DETAILS);
			downloadingForSave = false;			
			break;
			
		}
		
	}
	
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
    		return false;
    	}
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null) {
    				if (GWTDataSite.isRealRoLike(dataSite.getSiteTypeInt())){
	    				RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
	    				RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
	    				
	    				docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
    				}
    				return true;
    			} else if(!checkMiServerId) {
    				List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
    				if(almostLike != null && !almostLike.isEmpty()) {
    					return true;
    				}
    			}
    		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		if(!StringUtils.isEmpty(data.get("type"))) {
		        		String serverDocType = data.get("type");
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType(DocumentTypes.getDocumentSubcategory(serverDocType, searchId));
		    		}
		    		
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
	    		}
	    		
	    		try {
	    			instr.setYear(Integer.parseInt(data.get("year")));
	    		} catch (Exception e) {}
	    		
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
	    			if(checkMiServerId) {
		    			boolean foundMssServerId = false;
	    				for (DocumentI documentI : almostLike) {
	    					if(miServerID==documentI.getSiteId() && documentI.getInstno().equals(instr.getInstno())){
	    						foundMssServerId  = true;
	    						break;
	    					}
	    				}
		    			
	    				if(!foundMssServerId){
	    					return false;
	    				}
	    			}
	    			
    				if(data!=null) {
    					if(StringUtils.isEmpty(data.get("type"))){
    						data.put("type","MISC");
    					}
    					
		        		String serverDocType = data.get("type"); 
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
		    	    	String dataSource = data.get("dataSource");
		    	    	for (DocumentI documentI : almostLike) {
		    	    		if( (!checkMiServerId || miServerID==documentI.getSiteId()) && 
		    	    				documentI.getDocType().equals(docCateg) && 
		    	    				documentI.getInstno().equals(instr.getInstno())){
								return true;
		    	    		}
						}	
		    		} else {
		    			EmailClient email = new EmailClient();
		    			email.addTo(MailConfig.getExceptionEmail());
		    			email.setSubject("isInstrumentNoSaved problem on " + URLMaping.INSTANCE_DIR + this.getClass().getName());
		    			email.addContent("We should at least have type!!!!\nSearchId=" + searchId);
		    			email.sendAsynchronous();
		    		}
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }

    @Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded) throws ServerResponseException {
    	
    	if(!vsRequest.contains("Link=preparingimage0812.asp")){
    		return super.GetLink(vsRequest, vbEncoded); 
    	}
    	
		String link = vsRequest.substring(vsRequest.indexOf("preparingimage0812.asp"));
		String id = StringUtils.extractParameter(link, "&dummy=([^&]+)");
		String fileName = getCrtSearchDir() + "Register" + File.separator + id + ".tiff";
		
		for(int i=0; i<2; i++){
			if(retrieveImage(link, fileName)){
				break;
			}
		}
		
		boolean imageOK = writeImageToClient(fileName, "image/tiff");    	
		if(imageOK){
			return ServerResponse.createSolvedResponse();
		} else {
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);			
		}    	
    }

    /**
     * get the image using the add to cart, checkout, etc
     */
    @Override
    protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException{
    	String link = image.getLink().replace("preparingimage.asp", "preparingimage0812.asp");
    	int startPos = link.indexOf("preparingimage0812.asp");
    	if(startPos >= 0) {
    		link = link.substring(startPos);
	    	String fileName = image.getPath();
	    	for(int i=0; i<2; i++){
	    		if(retrieveImage(link, fileName)){
	    			byte b[] = FileUtils.readBinaryFile(fileName);
	        		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
	    		}
	    	}
    	}
    	return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );
    }
    
	@Override
	protected String getFileNameFromLink(String link) {
		String parcelId = substringBetween(link, "dummy=", "&");
		if (StringUtils.isEmpty(parcelId)){
			parcelId = substringBetween(link, "Id=", "&");
		}
		return parcelId + ".html";
	}	
	
    @Override
	protected boolean isFakeDocAlreadyProcessed(String fNameTSD, Instrument instr) {
		if (instr.getInstrumentType() == Instrument.TYPE_BOOK_PAGE
				&& (ro.cst.tsearch.utils.StringUtils.isStringBlank(instr.getBookNo()) || ro.cst.tsearch.utils.StringUtils.isStringBlank(instr.getPageNo()))) {
			return true;
		}
        String prefix = null, suffix = null;
        boolean checkPrefix = false;
        if(fNameTSD.indexOf(".") > 0) {
        	checkPrefix = true;
        	prefix = fNameTSD.substring(0,fNameTSD.indexOf("."));
        	suffix = fNameTSD.substring(fNameTSD.indexOf("."));
        }
        
		if (! (FileAlreadyExist(fNameTSD)
				|| checkPrefix?samePrefixFileAlreadyExist(mSearch.getSearchDir() + getServerTypeDirectory(), prefix, suffix):false)) {
			return false;
		}
		return super.isFakeDocAlreadyProcessed(fNameTSD, instr);
	}

	/**
	 * Called by reflection
	 * @param p
	 * @param pr
	 * @param htmlString
	 * @param pageId
	 * @param linkStart
	 * @param action
	 * @throws ro.cst.tsearch.exceptions.ServerResponseException
	 */
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException {
		p.splitResultRows(pr, htmlString, pageId,
				"<TR>         <td width=\"100%\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">          <tr>",
				"        </table>       </td>", linkStart, action);
	}
	
	/**
	 * Extract the table with the intermediate results
	 * @param page
	 * @return
	 */
	private String extractIntermTable(String page){
		
		// isolate results table
		int istart, iend;
		String table = "";
		istart = page.indexOf("Item"); if(istart == -1){ return null; }
		istart = page.lastIndexOf("<table", istart); if(istart == -1) { return null; }
		istart = page.lastIndexOf("<table", istart); if(istart == -1) { return null; }
		iend = page.lastIndexOf("</table>"); if(iend == -1){ return null; }
		iend = page.lastIndexOf("</table>", iend - 1);  if(iend == -1){ return null; }
		table = page.substring(istart, iend) + "</TABLE>";
		
		// rewrite the details links
		String prefix = CreatePartialLink(TSConnectionURL.idGET);
		table = table.replaceAll("InstrumentDisplay.asp\\?", prefix + "InstrumentDisplay.asp&");

		// help the splitting process
		table = table.replaceAll(
				"<tr>        <td width=\"100%\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">",
				"<TR>         <td width=\"100%\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">");
		
		// add "dummy" parameter to all intermediate results

		return table;
	}
	
	/**
	 * Extract the details page
	 * @param page
	 * @return
	 */
	private String extractDetails(String page){
		
		// check the page
		if(!page.contains("<title>Instrument Display</title>")){ return null;}
		
		// extract details table
		int istart = page.indexOf("Type</strong>"); if(istart == -1){ return null; }
		istart = page.lastIndexOf("<p></p>", istart); if(istart == -1){ return null; }
		istart += "<p></p>".length();
		int iend = page.indexOf("</form>", istart);  if(iend == -1){ return null; }
		iend = page.lastIndexOf("</div>", iend); if(iend == -1){ return null; }
		iend += "</div>".length();		
		String details = page.substring(istart, iend);
		
		// replace any crossref links
		String prefix = CreatePartialLink(TSConnectionURL.idGET);
		details = details.replaceAll("InstrumentDisplay.asp\\?", prefix + "InstrumentDisplay.asp&");
		
		return details;			
		
	}

	/**
	 * Extract the details page for Non-Indexed image
	 * @param page
	 * @return
	 */
	private String extractDetailsNonIndexed(String page){
		
		// check the page
		if(!page.contains("Non-Indexed image was found!")){ return null;}
		String book = qury.replaceAll("(?is)(.*)(Book=)([a-zA-Z0-9]+)(.*)", "$3");
		String pageBook = qury.replaceAll("(?is)(.*)(Page=)([a-zA-Z0-9]+)(.*)", "$3");
		String cls = qury.replaceAll("(?is)(.*)(Class=)([a-zA-Z0-9]+)(.*)", "$3");
		
		page = page.replaceAll("(?is)(<\\s*html\\s*>.*)</body>\\s*</html>", "<!--$1--></body></html>");
		page = page.replaceAll("(?is)(.*-->)(</body>\\s*</html>)", "$1 <b>Non-Indexed image</b> $2");
		page = page.replaceAll("(</body>\\s*</html>)", "");
		page = page  +  "<br><br><br><div align=\"left\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td valign=\"top\"><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" valign=\"top\" width=\"100%\" bgcolor=\"#000099\" nowrap>"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Instrument Type</strong></font></td></tr>"
								+ "<tr><td align=\"center\" width=\"100%\" nowrap>"
								+ "<font face=\"Tahoma\" size=\"2\">"
//								+StringUtils.getTextBetweenDelimiters(cls+"\">", "</option>", class_select)+
								
								+ "&nbsp;</td></tr></table></div></td>"
								+ "<td valign=\"top\" width=\"20%\" nowrap><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" width=\"100%\" bgcolor=\"#000099\" nowrap>"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Book &amp; Page/Filing #</strong></font></td></tr>"
								+ "<tr><td align=\"center\" width=\"100%\" nowrap><font face=\"Tahoma\" size=\"2\">" + book + "-" + pageBook + "</font> &nbsp;</td></tr></table></div></td>"
								+ "<td valign=\"top\" width=\"25%\"><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\" nowrap>"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Recording Date &amp; Time</strong></font></td></tr>"
								+ "<tr><td align=\"center\" nowrap><font face=\"Tahoma\" size=\"2\"></font> 1/1/1960</td></tr></table></div></td>"
								+ "<td valign=\"top\" width=\"15%\"><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\" nowrap>"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Instrument #</strong></font></td></tr>"
								+ "<tr><td align=\"center\" nowrap><font face=\"Tahoma\" size=\"2\"></font> &nbsp;</td></tr></table></div></td></tr>"
								+ "<tr><td valign=\"top\" nowrap>"
								+ "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse: collapse\" bordercolor=\"#111111\" width=\"100%\" >"
								+ "<tr><td width=\"50%\">&nbsp;</td><td width=\"50%\"><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\" nowrap>"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Document Date</strong></font></td></tr>"
								+ "<tr><td align=\"center\" nowrap><font face=\"Tahoma\" size=\"2\"></font>&nbsp;</td></tr></table></div></td></tr></table></td>"
								+ "<td valign=\"top\" width=\"20%\" nowrap><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\" nowrap>"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Consideration Amount</strong></font></td></tr>"
								+ "<tr><td align=\"center\" nowrap><font face=\"Tahoma\" size=\"2\"></font>&nbsp;</td></tr></table></div></td>"
								+ "<td valign=\"top\" width=\"25%\" nowrap><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\">"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Index Class</strong></font></td></tr>"
								+ "<tr><td align=\"center\"><font face=\"Tahoma\" size=\"2\"></font> &nbsp;</td></tr></table></div></td>"
								+ "<td valign=\"top\" width=\"15%\" nowrap><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\">"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Recording Class</strong></font></td></tr>"
								+ "<tr><td align=\"center\"><font face=\"Tahoma\" size=\"2\"></font> &nbsp;</td></tr></table></div></td></tr></table></div>"
								+ "<div align=\"left\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td valign=\"top\" width=\"50%\"><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\" nowrap>	"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Direct Parties</strong></font></td></tr>"
								+ "<tr><td nowrap><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><th align=\"left\" valign=\"top\" width=\"90%\"><font face=\"Tahoma\" size=\"2\">Name</font></th>"
								+ "<td align=\"center\" valign=\"top\" width=\"10%\"><strong><font face=\"Tahoma\" size=\"2\">WHO</font></strong><font face=\"Tahoma\" size=\"2\">"
								+ "<tr><td><font face=\"Tahoma\" size=\"2\"></font></td><td><font face=\"Tahoma\" size=\"2\"><center>&nbsp;&nbsp;</center></font></td></tr></font></td></tr></table></td></tr></table></div></td>"
								+ "<td valign=\"top\" width=\"50%\"><div align=\"left\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td valign=\"top\"><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\" nowrap>"
								+ "<font color=\"#FFFF00\" style=\"font-size: t\" face=\"Tahoma\" size=\"2\"><strong>Reverse Parties</strong></font></td></tr>"
								+ "<tr><td nowrap><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><th align=\"left\" valign=\"top\" width=\"90%\"><font face=\"Tahoma\" size=\"2\">Name</font></th>"
								+ "<td align=\"center\" valign=\"top\" width=\"10%\"><strong>"
								+ "<font face=\"Tahoma\" size=\"2\">WHO</font><tr><td><font face=\"Tahoma\" size=\"2\"></font></td>"
								+ "<td><font face=\"Tahoma\" size=\"2\"><center>&nbsp;&nbsp;</center></font></td></tr>"
								+ "<tr><td><font face=\"Tahoma\" size=\"2\"></font></td><td><font face=\"Tahoma\" size=\"2\"><center>&nbsp;&nbsp;</center></font>"
								+ "</td></tr></td></tr></table></td></tr></table></div></td></tr></table></div></td></tr></table></div>"
								+ "<div align=\"left\"><table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\" nowrap>"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Property Information</strong></font></td></tr>"
								+ "<tr><td nowrap><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><th align=\"left\" valign=\"top\"><font face=\"Tahoma\" size=\"2\">Subdivision / Property Address</font></th>"
								+ "<th valign=\"top\"><font face=\"Tahoma\" size=\"2\">Section</font></th><th valign=\"top\"><font face=\"Tahoma\" size=\"2\">Phase</font></th>"
								+ "<th valign=\"top\"><font face=\"Tahoma\" size=\"2\">Lot</font></th><th valign=\"top\"><font face=\"Tahoma\" size=\"2\">Building</font></th>"
								+ "<th valign=\"top\"><font face=\"Tahoma\" size=\"2\">Unit<th valign=\"top\"><font face=\"Tahoma\" size=\"2\">Acres"
								+ "<th valign=\"top\"><font face=\"Tahoma\" size=\"2\">District</font></th></tr></table> </td></tr></table></div>"
								+ "<div align=\"left\"><table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td valign=\"top\" width=\"50%\" nowrap><div align=\"left\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td valign=\"top\" width=\"50%\"><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\">"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Comments</strong></font></td>"
								+ "</tr></table></div></td></tr></table></div></td>"
								+ "<td valign=\"top\" width=\"50%\" nowrap><div align=\"left\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td valign=\"top\" width=\"50%\"><div align=\"left\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td valign=\"top\"><div align=\"left\"><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\">"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Legal Description</strong></font></td>"
								+ "</tr></table></div></td></tr></table></div></td></tr></table></div></td></tr>"
								+ "<tr><td valign=\"top\" width=\"50%\" nowrap><font face=\"Tahoma\" size=\"2\">&nbsp;</font> &nbsp;</td>"
								+ "<td valign=\"top\" width=\"50%\" nowrap><font face=\"Tahoma\" size=\"2\">&nbsp;</font> &nbsp;</td></tr></table></div>"
								+ "<div align=\"left\"><table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><td align=\"center\" bgcolor=\"#000099\" nowrap>"
								+ "<font color=\"#FFFF00\" face=\"Tahoma\" size=\"2\"><strong>Linked Documents</strong></font></td></tr>"
								+ "<tr><td nowrap><table border=\"1\" cellspacing=\"0\" width=\"100%\">"
								+ "<tr><th align=\"left\" valign=\"top\"><font face=\"Tahoma\" size=\"2\">Recording Class</font></th>"
								+ "<th valign=\"top\"><font face=\"Tahoma\" size=\"2\">Book &amp; Page/Filing #</font></th>"
								+ "<th valign=\"top\"><font face=\"Tahoma\" size=\"2\">Description</font></th>"
								+ "<th valign=\"top\"><font face=\"Tahoma\" size=\"2\">Recording Date &amp; Time</font></th>"
								+ "<th valign=\"top\"><font face=\"Tahoma\" size=\"2\">Instrument Number</font></th></tr></table></td></tr></table></div><br/>";
		String details = page;
		return details;			
		
	}
	/**
	 * Return page I of N type of message
	 * @param page
	 * @return
	 */
	private String getLocation(String page){ 
		String crtPage = StringUtils.extractParameter(page, "<input name=\"Goto(\\d+)\" type=image src=\"images/nav_sel.gif\"");
		if(StringUtils.isEmpty(crtPage)){
			return "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; page <b>1</b> of <b>1<b> ";
		}
		int istart = page.lastIndexOf("<input name=\"Goto");
		if(istart == -1){ return ""; }
		istart += "<input name=\"Goto".length();
		int iend = page.indexOf("\"", istart);
		if(iend == -1){ return ""; }
		String noPages = page.substring(istart, iend);
		return "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; page <b>" + crtPage + "</b> of <b>" + noPages + "</b>";
	}
	/**
	 * Extract next and previous links
	 * @param page
	 * @return array with exactly two strings
	 */
	private String[] getNavLinks(String page){

		// create array with links
		String [] links = new String[2];
		
		// extract the form target
		String link = StringUtils.extractParameter(page, "<form method=\"POST\" action=\"([^\"]+)\">");
		if(StringUtils.isEmpty(link)){
			return links;
		} 
		// extract the form hidden params
		Matcher hidMatcher =  hidParamPattern.matcher(page);
		while(hidMatcher.find()){
			link += "&" + StringUtils.urlEncode(hidMatcher.group(1)) + "=" + StringUtils.urlEncode(hidMatcher.group(2));
		}

		// determine the current page no
		String pNoStr = StringUtils.extractParameter(page, "<input name=\"Goto(\\d+)\" type=image src=\"images/nav_sel.gif\"");
		if(StringUtils.isEmpty(pNoStr)){
			return links;
		}
		int pNo = Integer.valueOf(pNoStr);
		
		// construct the prev & next links
		String prefix = CreatePartialLink(TSConnectionURL.idPOST);
				
		if(page.contains("><input name=\"Previous\"")){
			String p = "Goto" + (pNo-1);
			links[0] = "<a href=\"" + prefix + link + "&" + p + ".x=6&" + p +".y=6" + "\">Previous</a>";
		}		
		if(page.contains("><input name=\"Next\"")){
			String p = "Goto" + (pNo+1);
			links[1] = "<a href=\"" + prefix + link + "&" + p + ".x=6&" + p +".y=6" + "\">Next</a>";
		}				
		return links;
	}
	
	/**
	 * Extract unique id for a page
	 * @param page
	 * @return Book & Page/Filling #, or null if nothing found
	 */
	private String extractId(String page){
		int istart, iend;
		istart = page.indexOf("Page/Filing #</strong>"); if(istart == -1){ return null; }
		istart = page.indexOf("<font", istart); if(istart == -1){ return null; }
		istart = page.indexOf(">", istart); if(istart == -1){ return null; }
		istart += 1;
		iend = page.indexOf("<", istart); if(iend == -1){ return null; }
		return page.substring(istart, iend).replace("-", "_"); 
	}
	
	/**
	 * Extract instrument number from page
	 * @param page
	 * @return Instrument #, or null if nothing found
	 */
	private String extractInstrumentNumber(String page){
		int istart, iend;
		istart = page.indexOf("Instrument #</strong>");
		if(istart == -1){ return null; }
		istart = page.indexOf("<font", istart); 
		if(istart == -1){ return null; }
		istart = page.indexOf(">", istart);
		if(istart == -1){ return null; }
		istart += 1;
		iend = page.indexOf("<", istart);
		if(iend == -1){ return null; }
		return page.substring(istart, iend); 
	}
	
//	/**
//	 * Extract instrument number from page
//	 * @param page
//	 * @return Instrument #, or null if nothing found
//	 */
//	private String extractRecordedDate(String page){
//		int istart, iend;
//		istart = page.indexOf("Instrument #</strong>");
//		if(istart == -1){ return null; }
//		istart = page.indexOf("<font", istart); 
//		if(istart == -1){ return null; }
//		istart = page.indexOf(">", istart);
//		if(istart == -1){ return null; }
//		istart += 1;
//		iend = page.indexOf("<", istart);
//		if(iend == -1){ return null; }
//		return page.substring(istart, iend); 
//	}
	
	
	private String extractDocumentType(String page){
		int istart, iend;
		try {
			page = HTMLCorrector.correct(page.replaceAll("\r?\n", " "));
			istart = page.indexOf("Instrument Type"); 
			if(istart == -1){ 
				return null; 
			}
			istart = page.indexOf("<TD", istart); 
			if(istart == -1){ 
				return null; 
			}
			istart = page.indexOf(">", istart); 
			if(istart == -1){ 
				return null; 
			}
			istart += 1;
			iend = page.indexOf("<", istart); if(iend == -1){ return null; }
			return page.substring(istart, iend).trim();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * Extract image link
	 * @param response
	 * @return
	 */
	private String extractImageLink(String page, String id){
		
		// check that we have the image
		if(!page.contains("<input width=\"100%\" type=\"button\" value=\"             View Image             \"")){
			return null;
		}
		if(page.contains("Image is not available!")){
			return null;
		}
		if(page.contains("This document can't be viewed on the internet")){
			return null;
		}
		
		String link = StringUtils.extractParameter(page, "parent\\.page\\.location\\.href = 'preparingimage0812\\.asp\\?([^']+)");
		if(StringUtils.isEmpty(link)){
			return null;
		}
		link = "preparingimage0812.asp&" + link + "200&viewer=external&printnotice=false";
		String prefix = CreatePartialLink(TSConnectionURL.idGET);
		link = prefix + link + "&dummy=" + id;
		
		return link;
				
	}

    /**
     * Retrieve image
     * @param link
     * @param fileName
     * @return true if image succesfully retrieved
     */
    private boolean retrieveImage(String origLink, String fileName){
    	    	
    	// check if the file was already downloaded
    	if(FileUtils.existPath(fileName)){
    		return true;
    	}
    	
		// create output folder
		FileUtils.CreateOutputDir(fileName);

		// clean the link
		String link = origLink;
		link = link.replaceFirst("&dummy=[^&]*", "");
		link = link.replaceFirst("&", "?");
		
		// obtain http site
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try{
			link = "http://wwwX.ustitlesearch.net/" + link;
			String page = site.process(new HTTPRequest(link)).getResponseAsString();
			link = StringUtils.extractParameter(page, "\"(downloadimage\\.asp\\?[^\"]+)");
			if(StringUtils.isEmpty(link)){
				return false;
			}
			link = "http://wwwX.ustitlesearch.net/" + link;
	    	HTTPResponse response = site.process(new HTTPRequest(link));
	    	if(!"image/tiff".equals(response.getContentType())){
	    		return false;
	    	}    	
	    	InputStream inputStream = response.getResponseAsStream();
	    	FileUtils.writeStreamToFile(inputStream, fileName);
	    	afterDownloadImage();
			return FileUtils.existPath(fileName);
		} catch(RuntimeException e){
			logger.error("origLink: " + origLink + " and fileName: " + fileName, e);
			return false;
		} finally {
			// ALWAYS release http site
			HttpManager.releaseSite(site);
		}    	
    }
    
	@Override
	protected void setCertificationDate() {

		try {
			 
		String countyName = dataSite.getCountyName();
		
        logger.debug("Intru pe get Certification Date - " + countyName);
        if (CertificationDateManager.isCertificationDateInCache(dataSite)){
			String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
			getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
		} else{
	        String html = "";
	    		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
	    		try{
	    			String link = "http://wwwX.ustitlesearch.net/tn/"+countyName+".asp?page=tn/"+countyName+".asp";
	    			html = site.process(new HTTPRequest(link)).getResponseAsString();
	    		} catch(RuntimeException e){
	    			e.printStackTrace();
	    		} finally {
	    			HttpManager.releaseSite(site);
	    		}   
	        	
	            Matcher certDateMatcher = certDatePattern.matcher(html);
	            if(certDateMatcher.find()) {
	            	String date = certDateMatcher.group(1).trim();
	            	
	            	date = CertificationDateManager.sdfOut.format(CertificationDateManager.sdfOut.parse(date));
	            	CertificationDateManager.cacheCertificationDate(dataSite, date);
	            	getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
	            }
		}
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		
		 String page_x_of_y = " " + getLocation(table) + "<br/><br/>";
		 
		 table = table.replaceAll("(?ism)<td width=\"2\" align=\"center\" nowrap></td> ", "");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(null);
			TableTag mainTable = (TableTag) ((mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"),true).toNodeArray())[1]);
			NodeList nl = mainTable.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("border", "1"),true);
			
			for(int i=0; i<nl.size(); i++ ) {
					try {
						String row = ((TableTag)nl.elementAt(i)).toHtml();
						NodeList links = ((TableTag)nl.elementAt(i)).getChildren().extractAllNodesThatMatch(new TagNameFilter("a"),true);
						LinkTag linkTag = (LinkTag) links.elementAt(0);
						String url = linkTag.extractLink();
						String link = CreatePartialLink(TSConnectionURL.idGET) + url;
						
						
						String rowHtml =  row.replaceFirst("<a href=\"(InstrumentDisplay\\.asp.*?\")>",
								"<a href=\""+link+"\">" );
						
						
						
						ParsedResponse currentResponse = new ParsedResponse();
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row);
						
						currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
						
						ResultMap m = parseIntermediaryRow(((TableTag)nl.elementAt(i)));
						
						Bridge bridge = new Bridge(currentResponse,m,searchId);
						
						DocumentI document = (RegisterDocumentI)bridge.importData();				
						currentResponse.setDocument(document);
						
						if(isInstrumentSaved("blabla", document, null)) {
							rowHtml = rowHtml.replaceAll("(?i)(?s)(<td[^>]*>)(<a href=\"([^\"]*)\">.*?</a>)",
									"<td>Saved</td><td>$2");
						} else {
							rowHtml = rowHtml.replaceAll("(?i)(?s)(<td[^>]*>)(<a href=\"([^\"]*)\">.*?</a>)",
									"<td><input type=\"checkbox\" name=\"docLink\" value=\"$3\"></td><td>$2");
						}
						currentResponse.setOnlyResponse(rowHtml);
						intermediaryResponse.add(currentResponse);
					}catch(Exception e) {
						e.printStackTrace();
					}
			}
			
		NodeList nlHeader = mainTable.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("border", "0"),true);
		
		String header1 = nlHeader.elementAt(0).toHtml();
		header1 = header1.replaceFirst("&nbsp;Item&nbsp;", "<div>" + SELECT_ALL_CHECKBOXES + " All Items </div>");
		String footer = "";
		String [] navLinks = getNavLinks(table);
		String prev = navLinks[0];
		String next = navLinks[1];
		footer += "<br/>";
		
		if(!StringUtils.isEmpty(prev)){
			footer += " " + prev + " ";
		}
		if(!StringUtils.isEmpty(next)){
			footer += " " + next + " ";
		}
		
		footer+=page_x_of_y;		
		
		//if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
			
			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + 
					"<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n"+ header1);
			response.getParsedResponse().setFooter(footer + "</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SUBD, -1));
	    //}
		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	protected ResultMap parseIntermediaryRow(TableTag row) {
		return ro.cst.tsearch.servers.functions.TNGenericUsTitleSearchRO.parseIntermediaryRow( row, this);
	}
	
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		
		if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module = getDefaultServerInfo().getModule(INST_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
			module.forceValue(1, "1");
			module.forceValue(2, "1");
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(BP_MODULE_IDX);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(2, "1");
			module.forceValue(3, "1");
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module = getDefaultServerInfo().getModule(INST_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
			module.forceValue(1, "1");
			module.forceValue(2, "1");
		} else {
			module = null;
		}
		return module;
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		return getRecoverModuleFrom(document);
	}
	
	public static void loadParentSiteData() {
		String folderPath = ServerConfig.getModuleDescriptionFolder(ServerConfig.getRealPath() + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if(!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			String xml = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath + File.separator + TNGenericUsTitleSearchRO.class.getSimpleName() + ".xml"));
			Pattern countyPattern = Pattern.compile("(?ism)<county id=\"(.*?)\">(.*?)</county>");
			Matcher countyM = countyPattern.matcher(xml);
			while(countyM.find()) {
				String countyName = countyM.group(1);
				String controls = countyM.group(2);
				String instrumentTypeSelect = ro.cst.tsearch.utils.StringUtils.extractParameter(controls, "(?ism)(<select name=\"InstrumentType\".*?</select>)");
				String classSelect = ro.cst.tsearch.utils.StringUtils.extractParameter(controls, "(?ism)(<select name=\"Class\".*?</select>)");
				
				String platTypes = getTypeIndexes(controls, instrumentTypeSelect, "plat", true);
				String easementTypes = getTypeIndexes(controls, instrumentTypeSelect, "ease", true);
				String restrictionTypes = getTypeIndexes(controls, instrumentTypeSelect, "rest", true);
				
				String bookPageTypes = getTypeIndexes(controls, classSelect, "bookPage", false);
				String platClass = getTypeIndexes(controls, classSelect, "platClass", false);
				
				Map<CountySpecificInfo,String> info = new HashMap<CountySpecificInfo, String>();
				info.put(CountySpecificInfo.INSTR_TYPE_SELECT,instrumentTypeSelect);
	    		info.put(CountySpecificInfo.CLASS_SELECT,classSelect);
	    		info.put(CountySpecificInfo.PLAT_TYPES, platTypes);
	    		info.put(CountySpecificInfo.EASEMENT_TYPES, easementTypes);
	    		info.put(CountySpecificInfo.RESTRICTION_TYPES, restrictionTypes);
	    		info.put(CountySpecificInfo.BOOK_PAGE_TYPES, bookPageTypes);
	    		info.put(CountySpecificInfo.PLAT_CLASS_TYPE, platClass);
	    		parentSiteInfo.put(countyName, info);
			}
		} catch (Exception e) {
			e.printStackTrace();	
		}
	}
	
	protected static String getTypeIndexes(String controls, String selectBox, String type, boolean tryAbbreviationAlso) {
		
		String values  = ro.cst.tsearch.utils.StringUtils.extractParameter(controls, "(?ism)<"+type+"Types>(.*?)</"+type+"Types>");
		String indexes = "";
		if(values.isEmpty()) {
			return ro.cst.tsearch.utils.StringUtils.extractParameter(selectBox, "(?im)<option.*?value=\"(.*?)\".*?>[^<]*?\\["+type.toUpperCase()+"\\]");
		}
		
		for(String value :  values.split(";")) {
			String index = "";
			value = value.trim();
			if(tryAbbreviationAlso) {
				index = ro.cst.tsearch.utils.StringUtils.extractParameter(selectBox, "(?im)<option.*?value=\"(.*?)\".*?>[^<]*?\\["+value+"\\]");
			}
			if(index.isEmpty()) {
				index = ro.cst.tsearch.utils.StringUtils.extractParameter(selectBox, "(?im)<option.*?value=\"(.*?)\".*?>\\s*[^<]*?"+value+"");
			}
			if(!index.isEmpty()) {
				indexes += index + ";";
			}
		}
		
		return indexes;
	}
	protected void downloadParentSiteData() {
		String html = "";
		ro.cst.tsearch.connection.http2.TNGenericUsTitleSearchRO site = (ro.cst.tsearch.connection.http2.TNGenericUsTitleSearchRO)HttpManager.getSite(getCurrentServerName(), searchId);
    	String server = site.getServer();
    	
    	String folderPath = ServerConfig.getModuleDescriptionFolder(ServerConfig.getRealPath() + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if(!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		File f = new File(folderPath + File.separator + TNGenericUsTitleSearchRO.class.getSimpleName() + ".xml");
    	
    	String allParentSiteData = "<state id=\"TN\">";
    	try{
    		Vector<CountyState> allTNCounties = DBManager.getCounties(StateContants.TN);
    		for(CountyState c : allTNCounties) {
    			System.out.println(c.getCountyName());
    			String link = ""; 
    			
    			link = "http://" + server + ".ustitlesearch.net/page.asp?page=subscription.asp";
    	        html = site.process(new HTTPRequest(link)).getResponseAsString();        
    	        link = "http://" + server + ".ustitlesearch.net/subscription.asp?page=subscription.asp";
    	        html = site.process(new HTTPRequest(link)).getResponseAsString(); 
    	        link = ro.cst.tsearch.utils.StringUtils.extractParameter(html, "<a href=\"(changesubscription[^\"]*)\" target=\"page\">" + "TN" + ", " + c.getCountyName() + "</a>");
    	        if(link.isEmpty() ) continue;
    	        link = "http://" + server + ".ustitlesearch.net/" + link;
    	        html = site.process(new HTTPRequest(link)).getResponseAsString();
    	        
	    		link = "http://wwwX.ustitlesearch.net/searchbypartyname.asp?page=searchbypartyname.asp";
	    		html = site.process(new HTTPRequest(link)).getResponseAsString();
	 
	    		String instrumentTypeSelect = ro.cst.tsearch.utils.StringUtils.extractParameter(html, "(?ism)(<select name=\"InstrumentType\".*?</select>)");
	    		
	    		link = "http://wwwX.ustitlesearch.net/SearchByBookAndPage.asp";
	    		html = site.process(new HTTPRequest(link)).getResponseAsString();
	    		
	    		
	    		String classSelect = ro.cst.tsearch.utils.StringUtils.extractParameter(html, "(?ism)(<select name=\"Class\".*?</select>)");
	    		
	    		System.out.println(classSelect);
	    		System.out.println(instrumentTypeSelect);
	    		
	    		allParentSiteData += "<county id=\""+c.getCountyName()+"\">\n";
	    		allParentSiteData += classSelect;
	    		allParentSiteData += instrumentTypeSelect;
	    		allParentSiteData += "</county>\n";
    		}
    		allParentSiteData += "</state>\n";
    		allParentSiteData = allParentSiteData.replaceAll("</option>", "</option>\n");
    		org.apache.commons.io.FileUtils.writeStringToFile(f, allParentSiteData);
    	} catch(Exception e){
    		e.printStackTrace();
    	} finally {
    		HttpManager.releaseSite(site);
    	}  
	}

}

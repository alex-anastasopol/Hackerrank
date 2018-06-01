package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsForUpdateFilter;
import ro.cst.tsearch.search.filter.newfilters.address.GenericMultipleAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterWithDoctype;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentSKLDIterator;
import ro.cst.tsearch.search.iterator.legal.LegalSKLDIterator;
import ro.cst.tsearch.search.iterator.legal.UnplattedSKLDIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.functions.GenericSKLDFunctions;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.servlet.parentsite.ParentSiteActions;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.address.Address;
import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.PriorFileDocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.SKLDInstrument;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.warning.Warning;
import com.stewart.ats.base.warning.WarningInfo;
import com.stewart.ats.connection.skld.images.SkldImages;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class GenericSKLD extends TSServerROLike {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected static final Category logger = Logger.getLogger(GenericSKLD.class);
	
	public static final String DELIMITER_INTERESTING_DATA = 
		"{00001------------------------------------------------------------------------";
	private static final String DELIMITER_END_OF_DATA = 
		"{-----------------------------------------------------------------------------";
	private static final Pattern SEARCH_SUMMARY_PATTERN = Pattern.compile("(?is).*?(\\{[\\w\\s]+\\s*COUNTY\\s*\\{\\*\\* SEARCH SUMMARY \\*\\*.*?\\*\\* END OF SUMMARY \\*\\*)");
	public static final String FIELD_SEPARATOR = "/____@@";
	
	public static final Pattern SUBDIVISION_LOOKUP_PATTERN = 
		Pattern.compile("(.*?)\\s+(\\w+)/(\\w+)");
	public static final Pattern SUBDIVISION_LOOKUP_STATUS_PATTERN = 
		Pattern.compile("[ ]{4}([ NV])");
	private static final Pattern CERTIFICATION_DATE_PATTERN = Pattern.compile("Plant Certified From:\\s*([\\d/]+)Thru:\\s*([\\d/]+)Order Act Cmt");
	
	public static SimpleDateFormat CERTIFICATION_DATE_FORMAT = new SimpleDateFormat("MM/ddyyyy");
	public static final String SUBDIVISION_NAME_KEY = "name=";
	public static final String MAPID_BOOK_KEY = "mapbook=";
	public static final String MAPID_PAGE_KEY = "mappage=";
	public static final String LOT_LOW = "lotlow=";
	public static final String BLOCK_LOW = "blocklow=";
	public static final String LOT_HIGH = "lothigh=";
	public static final String BLOCK_HIGH = "blockhigh=";
	
	public static final Pattern FILED_DATE = Pattern.compile("Filed Date:\\s+([\\d/]+)\\s+(\\d+)");
	public static final Pattern VACATE_DATE = Pattern.compile("Vacate Date:\\s+([\\d/]+)\\s+(\\d+)");
	public static final Pattern DOC_NUMBER = Pattern.compile("Doc #:\\s+([^\\s]+)");
	public static final Pattern FORCE_ERROR = Pattern.compile("Force Error:\\s+([^\\s]+)");
	
	public static final String RESULTSHEET_PATH = BaseServlet.REAL_PATH + "WEB-INF/classes/resource/DocIndexTemplates/SKLDResultSheet.html";
	private boolean templateVersionVerified = false;
	
	public static final HashSet<String> mapIdModuleSaKeys = new HashSet<String>(){/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{
		add(SearchAttributes.LD_SK_SUBDIVISION_NAME);
		add(SearchAttributes.LD_SK_MAPID_BOOK);
		add(SearchAttributes.LD_SK_MAPID_PAGE);
		add(SearchAttributes.LD_SK_LOT_LOW);
		add(SearchAttributes.LD_SK_LOT_HIGH);
		add(SearchAttributes.LD_SK_BLOCK_LOW);
		add(SearchAttributes.LD_SK_BLOCK_HIGH);
	}};
	
	
	public GenericSKLD(long searchId) {
    	super(searchId);
        resultType = MULTIPLE_RESULT_TYPE;
    }
	
	private static HashMap<Integer, String> siteOptionMap = new HashMap<Integer, String>(){
		private static final long serialVersionUID = 1L;
	{
		put(340724, "1");		//Adams
		put(340924, "2");		//Arapahoe
		put(341324, "3");		//Boulder
		put(341424, "4");		//Broomfield
		put(341724, "5");		//Clear Creek
		put(342324, "6");		//Denver
		put(342524, "7");		//Douglas
		put(342624, "8");		//Eagle
		put(342724, "9");		//El Passo
		put(343724, "11");		//Jefferson
		
		
	}};
	
    public GenericSKLD(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
    }
    
    @Override
    public TSServerInfo getDefaultServerInfo() {
    	TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
    	String countyNo = siteOptionMap.get(miServerID);
    	TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.forceValue(10, countyNo);
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.forceValue(2, countyNo);
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.forceValue(8, countyNo);
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.forceValue(8, countyNo);
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.forceValue(10, countyNo);
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.TYPE_NAME_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.forceValue(1, countyNo);
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SECTION_LAND_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.forceValue(9, countyNo);
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.CONDOMIN_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.forceValue(8, countyNo);
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SURVEYS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.forceValue(10, countyNo);
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ARCHIVE_DOCS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.forceValue(1, countyNo);
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.GENERIC_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.forceValue(2, countyNo);
		}
		
		
    	return msiServerInfoDefault;
    }
    
    @SuppressWarnings("rawtypes")
	@Override
    protected void ParseResponse(String sAction, ServerResponse Response,
    		int viParseID) throws ServerResponseException {

    	ParsedResponse parsedResponse = Response.getParsedResponse();
    	String rsResponce = Response.getResult();
    	
    	checkErrorMessages(parsedResponse, rsResponce);
    	
    	if(StringUtils.isNotEmpty(parsedResponse.getError())) {
    		Response.setError(parsedResponse.getError());
    		parsedResponse.setResponse("");
    		Response.setResult("");
    		return;
    	}
    	rsResponce = rsResponce.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");
    	
    	String resultSheet = rsResponce;
    	resultSheet = resultSheet.replaceAll("(?ms)", "");
    	resultSheet = resultSheet.replaceFirst("(?is)[^\\{]+", "");
    	resultSheet = resultSheet.replaceAll("(?is)\\b(COUNTY)\\s*\\{", "$1              ");
    	resultSheet = resultSheet.replaceAll("(?is)\\{\\s*(PAGE:)", "              $1");
    	resultSheet = resultSheet.replaceAll("(?is)\\b(REQUESTOR:)\\s*\\{\\n", "$1              ");
    	
    	resultSheet = resultSheet.replaceAll("(\\{[\\d]+[-]+)(\\{[\\d]+[-]+)", "$1");
    	resultSheet = resultSheet.replaceAll("(?is)(\\{.*?)\\1", "$1");
		resultSheet = resultSheet.replaceAll("(?is)\\{", " ");
    	
    	int tempIndex = 0;
    	StringBuilder outputTable = new StringBuilder();
    	Collection<ParsedResponse> smartParsedResponses = null;
    	
    	try {
	    	Matcher certDateMatcher = Pattern.compile("Plant Certified From:\\s*([\\d/\\s]+)Through:\\s*([\\d/\\s]+)\\{").matcher(rsResponce);
	    	String countyId = getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY);
	        if(certDateMatcher.find()) {
	        	String date = certDateMatcher.group(2).trim();

	        	date = CertificationDateManager.sdfOut.format(new SimpleDateFormat("MM/dd yyyy").parse(date));
            	
            	CertificationDateManager.cacheCertificationDate(dataSite, date);
	        	getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
	        	
	        	logger.debug("Set Certification Date for SearchId: "  + searchId + " to " + date + " for county " + countyId);
	        } else {
	        	logger.error("Could NOT set Certification Date for SearchId: "  + searchId + " for county " + countyId);
	        } 
        } catch (Exception e) {
			logger.error("Error while setting Certification Date from received response", e);
		}
    	
    	
    	switch(viParseID){
    		case ID_SEARCH_BY_NAME:
    			tempIndex = rsResponce.indexOf(DELIMITER_END_OF_DATA);
    			if(tempIndex > 0) {
    				rsResponce = rsResponce.substring(0, tempIndex);
    			}
    			
    			tempIndex = rsResponce.indexOf(DELIMITER_INTERESTING_DATA);
    			if(tempIndex > 0) {
    				rsResponce = rsResponce.substring(tempIndex);

    			}
    			
    			rsResponce = rsResponce.replaceAll("(\\{[\\d]+[-]+)(\\{[\\d]+[-]+)", "$1");
    			rsResponce = rsResponce.replaceAll("(?m)^(\\{[^\\{\\r\\n]+)(\\{[^\\{\\r\\n]+)", "$1");
    			rsResponce = rsResponce.replaceAll("(?ms).*?Through: [\\d/\\s]+$", "");
    			rsResponce = rsResponce.replaceAll("\\{Order Nbr[^\\r\\n]+", "");
    			//rsResponce = rsResponce.replaceAll("(?m)^\\{", "");
    			
    			createAndAddInfoInResultSheet(resultSheet);
    			
    			if(rsResponce.trim().isEmpty() || (!GenericSKLDFunctions.FIRST_LINE_PATTERN.matcher(rsResponce).find() && !GenericSKLDFunctions.FIRST_LINE_STR_PATTERN.matcher(rsResponce).find())) {
    				parsedResponse.setResultRows(new Vector());
    				logger.error(Response.getResult().replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", ""));
					return;
    			}
    			
    			/**
    			 * Save module with no error ;)
    			 */
    			Search search = getSearch();
    			@SuppressWarnings("unchecked")
				Set<Integer> additionalInfo = (Set<Integer>) search.getAdditionalInfo(AdditionalInfoKeys.PERFORMED_WITH_NO_ERROR_MODULE_ID_SET);
    			if(additionalInfo == null) {
    				additionalInfo = new HashSet<Integer>();
    				search.setAdditionalInfo(AdditionalInfoKeys.PERFORMED_WITH_NO_ERROR_MODULE_ID_SET, additionalInfo);
    			}
    			Object possibleModule = parsedResponse.getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
//    			StringBuilder keyModuleForRS = new StringBuilder("moduleId_");
//    			StringBuilder sb = new StringBuilder();
    	        
    			if(possibleModule instanceof TSServerInfoModule) {
    				TSServerInfoModule module = (TSServerInfoModule) possibleModule;
    				additionalInfo.add(module.getModuleIdx());
    				
//    				keyModuleForRS.append(module.getModuleIdx());
//    				if (module.getModuleIdx() == 0){
//    					if (module.getFunctionCount() > 13){
//    						String propType = module.getParamValue(13);
//    						if (org.apache.commons.lang.StringUtils.isBlank(propType)){
//    							propType = "N";
//    						}
//    						keyModuleForRS.append("_").append(propType);
//    					}
//    					if (SearchAttributes.OWNER_OBJECT.equals(module.getSaObjKey())){
//    						keyModuleForRS.append("_O");
//    					} else if (SearchAttributes.BUYER_OBJECT.equals(module.getSaObjKey())){
//    						keyModuleForRS.append("_B");
//    					} else{
//    						keyModuleForRS.append("_O");
//    					}
//    				}
//    				createSearchByInfo(sb, module);
    			}
    			
    			

    			SearchLogger.info("<br>Receiving SKLD Response:<br><br>" + 
    					rsResponce.replaceAll(" ", "&nbsp;").replaceAll("[\\r\\n]+", "<br>") + "<br><br>",searchId); 
    			smartParsedResponses = smartParseIntermediary(Response, 
						rsResponce, outputTable);

				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					
		            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();                           	
		            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
		            	        "<tr><th rowspan=1>"+ SELECT_ALL_CHECKBOXES + "</th>" +
		            			"<td>Document Content</td></tr>";

		            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
		            				viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}

		            	
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
					
					
				} else {
					parsedResponse.setResultRows(new Vector());
					logger.error(rsResponce);
					return;
				}
    			break;
    			
    		case ID_SEARCH_BY_SECTION_LAND:
    			
    			
    			rsResponce = Response.getResult();
    			tempIndex = rsResponce.indexOf("F3=Exit");
    			if(tempIndex >= 0) {
    				rsResponce = rsResponce.substring(tempIndex + "F3=Exit".length() + 17);
    				rsResponce = rsResponce.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]+", " ");
    				rsResponce = rsResponce.replaceAll("", " ");
    				
    				
    				smartParsedResponses = smartParseLookup(Response, 
    						rsResponce, outputTable);
    				
    				if(smartParsedResponses.size() > 0) {
    					String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();     
		               	
		               	header += "<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"/title-search" + URLMaping.PARENT_SITE_ACTIONS + "\"" + " method=\"POST\" > "
		                + "<input type=\"hidden\" name=\""+ TSOpCode.OPCODE + "\" value=\""+ ParentSiteActions.SUBDIVISION_NAME_LOOKUP + "\">"
		                + "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\">" 
		                + "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + mSearch.getSearchID() + "\"> "; 
		               		
		               		
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
		            	        "<tr><th rowspan=1>Select</th>" +
		            			"<td >Subdivision Name</td><td >S/P</td><td >MapID Book</td><td >MapID Page</td></tr>";
		               	
    					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
    					
    					footer = outputTable.toString() + "\n</table>" + "<input  type=\"submit\" class=\"button\" name=\"Button\" value=\"Select Subdivision\">\r\n</form>\n" ;;
    					parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
    				}
    			}
    			break;
    		case ID_SEARCH_BY_MODULE26:
    			rsResponce = Response.getResult();
    			
    			/*
    			//just debugging
				try {
					FileUtils.writeStringToFile(new File("D:\\" + Response.getQuerry() +".txt"), rsResponce);
				} catch (IOException e) {
					e.printStackTrace();
				}
    			*/
    			
    			tempIndex = rsResponce.indexOf("Pri Low:");
    			if(tempIndex >= 0) {
    				//rsResponce = rsResponce.substring(tempIndex);
    				//rsResponce = rsResponce.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", " ");
    				//rsResponce = rsResponce.replaceAll("", " ");
    				
    				
    				smartParsedResponses = smartParseSecondaryLookup(Response, 
    						rsResponce, outputTable);
    				
    				if(smartParsedResponses.size() > 0) {
    					String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();     
		               	
		               	header += "<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"/title-search" + URLMaping.PARENT_SITE_ACTIONS + "\"" + " method=\"POST\" > "
		                + "<input type=\"hidden\" name=\""+ TSOpCode.OPCODE + "\" value=\""+ ParentSiteActions.SUBDIVISION_SECONDARY_NAME_LOOKUP + "\">"
		                + "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\">" 
		                + "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + mSearch.getSearchID() + "\"> "; 
		               		
		               		
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
		            		"<tr><th rowspan=1>Select</th><td>Secondary Low</td><td>Secondary High</td><td>F/E</td><td>E/O</td><td>Filed</td><td>Vacanted</td><td>Rmk</td></tr>\n";
		               	
    					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
    					
    					footer = outputTable.toString() + "\n</table>" + "<input  type=\"submit\" class=\"button\" name=\"Button\" value=\"Select Secondary Subdivision\">\r\n</form>\n" ;
    					parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
    				} else if(outputTable.toString().contains("No secondary legal found")){
    					String header = parsedResponse.getHeader();
    					String footer = parsedResponse.getFooter(); 
    					header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
	            		"<tr><th rowspan=1>Select</th><td>Secondary Low</td><td>Secondary High</td><td>F/E</td><td>E/O</td><td>Filed</td><td>Vacanted</td><td>Rmk</td></tr>\n";
	               	
    					ParsedResponse currentResponse = new ParsedResponse();
    					currentResponse.setOnlyResponse("<tr><td colspan=\"8\">No secondary legal found</td></tr>");
						currentResponse
								.setAttribute(
										ParsedResponse.SERVER_NAVIGATION_LINK,
										true);
						smartParsedResponses.add(currentResponse);
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						
						footer = "\n</table>";
						parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
    				}
    			}
    			
    		case ID_DETAILS :
    		case ID_SAVE_TO_TSD :
				DocumentI document = parsedResponse.getDocument();
				
				if(document!= null) {
					msSaveToTSDFileName = document.getId() + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				}
				break;
    		default:
    			System.out.println("Default VIPARCELID: " + viParseID);
    			;
    	}
    }

    public void createAndAddInfoInResultSheet(String resultSheet){
    	DocumentsManagerI manager = mSearch.getDocManager();
        
        try{
        	manager.getAccess();
        	Instrument instr = new Instrument("ResultSheet");
        	instr.setDocType(DocumentTypes.OTHERFILES);
        	instr.setDocSubType(DocumentTypes.OTHER_FILE_RESULTSHEET);
        	
        	DocumentI document = manager.createResultSheetEmptyDocument(DType.ROLIKE, DocumentTypes.OTHERFILES, DocumentTypes.OTHER_FILE_RESULTSHEET, 
        											instr, "SK", false, getSearch(), RESULTSHEET_PATH, templateVersionVerified);
        	String resultSheetContent = DBManager.getDocumentIndex(document.getIndexId());

        	org.htmlparser.Parser resultSheetParser = org.htmlparser.Parser.createParser(resultSheetContent, null);
			NodeList nodes = resultSheetParser.parse(null);

			if (nodes != null){
				NodeList tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				
				if (tableList.extractAllNodesThatMatch(new HasAttributeFilter("id", "results")).size() > 0){
					Node node = tableList.extractAllNodesThatMatch(new HasAttributeFilter("id", "results")).elementAt(0);
	        		if (node instanceof TableTag){
	        			TableTag tableNode = (TableTag) node;
	        			
	        			TableRow tr = new TableRow();
	        			TableColumn tcResp = new TableColumn();
	        			tcResp.setChildren(new NodeList(new TextNode(resultSheet.replaceAll(" ", "&nbsp;").replaceAll("[\\r\\n]+", "<br>") + "<br><br>")));
	        			
	        			if (tr.getChildren() == null){
	        				tr.setChildren(new NodeList(tcResp));
	        			} else{
	        				tr.getChildren().add(tcResp);
	        			}
	        			if (tableNode.getChildren() == null){
	        				tableNode.setChildren(new NodeList(tcResp));
	        			} else{
	        				tableNode.getChildren().add(tr);
	        			}
	        		}
	        	}
				if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH){
    				if (tableList.extractAllNodesThatMatch(new HasAttributeFilter("id", "searchSummary")).size() > 0){
    					String searchSummary = addSearchSummaryToResultSheet(true);
    					if (org.apache.commons.lang.StringUtils.isNotBlank(searchSummary)){
	    					Node node = tableList.extractAllNodesThatMatch(new HasAttributeFilter("id", "searchSummary")).elementAt(0);
	    	        		if (node instanceof TableTag){
	    	        			TableTag tableNode = (TableTag) node;
	    	        			if (tableNode.getRowCount() > 0){
	    	        				while (tableNode.getRowCount() > 0){
	    	        					tableNode.removeChild(0);
	    	        				}
//	    	        				tableNode = new TableTag();
//	    	        				tableNode.setAttribute("id", "searchSummary");
	    	        			}
	    	        			
	    	        			TableRow tr = new TableRow();
	    	        			TableColumn tcResp = new TableColumn();
	    	        			tcResp.setChildren(new NodeList(new TextNode(searchSummary.replaceAll(" ", "&nbsp;").replaceAll("[\\r\\n]+", "<br>") + "<br><br>")));
	    	        			
	    	        			if (tr.getChildren() == null){
	    	        				tr.setChildren(new NodeList(tcResp));
	    	        			} else{
	    	        				tr.getChildren().add(tcResp);
	    	        			}
	    	        			if (tableNode.getChildren() == null){
	    	        				tableNode.setChildren(new NodeList(tcResp));
	    	        			} else{
	    	        				tableNode.getChildren().add(tr);
	    	        			}
	    	        		}
    					}
    	        	}
    			}
				DBManager.updateDocumentIndex(document.getIndexId(), nodes.toHtml(), getSearch());
			}
			
//			if (tableList.extractAllNodesThatMatch(new HasAttributeFilter("id", keyModuleForRS.toString())).size() > 0){
//				Node node = tableList.extractAllNodesThatMatch(new HasAttributeFilter("id", keyModuleForRS.toString())).elementAt(0);
//        		if (node instanceof TableTag){
//        			TableTag tableNode = (TableTag) node;
//        			TableRow tr1 = new TableRow();
//        			
//        			TableColumn tcHead = new TableColumn();
//        			tcHead.setChildren(new NodeList(new TextNode(sb.toString())));
//        			if (tr1.getChildren() == null){
//        				tr1.setChildren(new NodeList(tcHead));
//        			} else{
//        				tr1.getChildren().add(tcHead);
//        			}
//        			tableNode.getChildren().add(tr1);
//        			
//        			TableRow tr2 = new TableRow();
//        			TableColumn tcResp = new TableColumn();
//        			tcResp.setChildren(new NodeList(new TextNode(resultSheet.replaceAll(" ", "&nbsp;").replaceAll("[\\r\\n]+", "<br>") + "<br><br>")));
//        			
//        			if (tr2.getChildren() == null){
//        				tr2.setChildren(new NodeList(tcResp));
//        			} else{
//        				tr2.getChildren().add(tcResp);
//        			}
//        			
//        			tableNode.getChildren().add(tr2);
//        		}
//        		DBManager.updateDocumentIndex(document.getIndexId(), nodes.toHtml(), getSearch());
//        	}
        	
        } catch(Exception e){  
        	e.printStackTrace(); 
        } finally{
        	manager.releaseAccess();
        }
    }
    
    
    
    public void createSearchByInfo(StringBuilder sb, TSServerInfoModule module){
    	//searchLogPage.
		boolean automatic = (getSearch().getSearchType() != Search.PARENT_SITE_SEARCH) || (GPMaster.getThread(searchId) != null);
        sb.append("<span class='serverName'>");
        String serverName = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName();
		sb.append(serverName);
        sb.append("</span> ");

       	sb.append(automatic? "automatic" : "manual");
       	Object info = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
       	if (StringUtils.isNotEmpty(module.getLabel())) {
	        
	        if(info!=null){
	        	sb.append(" - " + info + "<br>");
	        }
	        sb.append(" <span class='searchName'>");
	        sb.append(module.getLabel());
       	} else {
       		sb.append(" <span class='searchName'>");
	        if(info!=null){
	        	sb.append(" - " + info + "<br>");
	        }
       	}
        sb.append("</span> by ");
        
        boolean firstTime = true;
        for (Entry<String,String> entry : module.getParamsForLog().entrySet() ){
        	String value = entry.getValue();
        	value = value.replaceAll("(, )+$", ""); 
        	if(!firstTime){
        		sb.append(", ");
        	} else {
        		firstTime = false;
        	}
        	sb.append(entry.getKey().replaceAll("&lt;br&gt;", "") + " = <b>" + value + "</b>");
        } 
        //log time when manual is starting        
        if (!automatic){
        	sb.append(" ");
        	sb.append(SearchLogger.getTimeStamp(searchId));
        }
        sb.append(":<br/>");
    }
    
	@SuppressWarnings("rawtypes")
	private void checkErrorMessages(ParsedResponse parsedResponse,
			String rsResponce) {
		
		/*if(rsResponce.contains("NO MATCHES FOUND FOR SEARCH CRITERIA ENTERED")) {
			parsedResponse.setError("<font color=\"red\">No results were found.</font> Please change search criteria and try again.");
			return;
		}*/
		if(rsResponce.contains("Site error. Please try again")) {
			parsedResponse.setError("<font color=\"red\">Site error. Please try again.</font>");
			return;
		}
		
		if(rsResponce.contains("INVALID LEGAL ENTERED")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font><br><br>INVALID LEGAL ENTERED<br><br> Please change search criteria and try again.");
			return;
    	}
    	if(rsResponce.contains("field cannot be equal")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: Field cannot be equal to High Field<br>Please change search criteria and try again.");
			return;
    	}
    	if(rsResponce.contains("Primary legal must be entered")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: Primary legal must be entered<br>Please change search criteria and try again.");
			return;
    	}
    	if(rsResponce.contains("required in Low field")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: rick required in Low field, in the same position as High field.<br>Please change search criteria and try again.");
			return;
    	}
    	if(rsResponce.contains("required in High field")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: rick required in High field, in the same position as Low field.<br>Please change search criteria and try again.");
			return;
    	}
    	if(rsResponce.contains("HIGH OR LOW SUBDIVISION SECONDARY")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: HIGH OR LOW SUBDIVISION SECONDARY LEGAL INVALID AS KEYED<br>Please change search criteria and try again.");
			return;
    	}
    	if(rsResponce.contains("HIGH OR LOW ACREAGE SECONDARY")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: HIGH OR LOW ACREAGE SECONDARY LEGAL INVALID AS KEYED<br>Please change search criteria and try again.");
			return;
    	}
    	if(rsResponce.contains("MAP EDIT NOT FOUND FOR THE NAME GIVEN")) {
    		parsedResponse.setResultRows(new Vector());
			return;
    	}
    	if(rsResponce.contains("SUBDIVISION LEGAL KEYED NOT FOUND IN MAP EDIT")) {
    		parsedResponse.setResultRows(new Vector());
			return;
    	}
    	if(rsResponce.contains("SURVEY LEGAL KEYED DOES NOT EXIST IN MAP EDIT")) {
    		parsedResponse.setResultRows(new Vector());
			return;
    	}
    	if(rsResponce.contains("Invalid Doctype Entered")) {
    		parsedResponse.setError("No results found <br><font color=\"black\">ServerResponse: Invalid Doctype Entered</font><br>Please change search criteria and try again.");
			return;
    	}
    	
    	if(rsResponce.contains("Incomplete results found - No End Of Search Received")) {
    		parsedResponse.setError("Incomplete results found - No End Of Search Received - please try your search again or restrict search parameters");
			return;
    	}
    	if(rsResponce.contains("You must key in a secondary legal")) {
    		parsedResponse.setError("No results found <br><font color=\"black\">ServerResponse: You must key in a secondary legal</font><br>Please change search criteria and try again.");
			return;
    	}
    	
    	if(rsResponce.contains("Exception received: ")) {
    		parsedResponse.setError("There was an error with your request. Please try your search again or change search parameters");
			return;
    	}
	}
    
    private Collection<ParsedResponse> smartParseLookup(
			ServerResponse response, String rsResponce,
			StringBuilder outputTable) {
    	Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
    	
    	byte[] primaryLegalAsBytes = new byte[] {
    			(byte) 0x50, (byte) 0x72, (byte) 0x69, (byte) 0x6D, (byte) 0x61, 
				(byte) 0x72, (byte) 0x79, (byte) 0x20, (byte) 0x4C, (byte) 0x65, 
				(byte) 0x67, (byte) 0x61, (byte) 0x6C
			};
		byte[] sSelectAsBytes = new byte[] {
				(byte) 0x53, (byte) 0x3D, (byte) 0x53, 
				(byte) 0x65, (byte) 0x6C, (byte) 0x65, (byte) 0x63, (byte) 0x74
				};
		String dirtyInput = response.getResult();
		int indexOfPrimaryLegalAsBytes = dirtyInput.indexOf(new String(primaryLegalAsBytes));
		int indexOfSSelectAsBytes = dirtyInput.indexOf(new String(sSelectAsBytes));
		List<String> statusList = new ArrayList<String>();
		if(indexOfPrimaryLegalAsBytes > 0 && indexOfSSelectAsBytes > indexOfPrimaryLegalAsBytes	) {
			dirtyInput = dirtyInput.substring(indexOfPrimaryLegalAsBytes + (new String(primaryLegalAsBytes)).length(), indexOfSSelectAsBytes);
			Matcher matcher = SUBDIVISION_LOOKUP_STATUS_PATTERN.matcher(dirtyInput);
			while(matcher.find()) {
				statusList.add(matcher.group(1));
			}
		}
    	
    	
    	
    	Matcher matcher = SUBDIVISION_LOOKUP_PATTERN.matcher(rsResponce);
		String radioStart = "<input type=\"radio\" name=\"doclink\" value=\"";

		String firstNameFound = null;
		int indexOfRow = 0;
		while (matcher.find()) {
			try {
				
				if(firstNameFound == null) {
					firstNameFound = URLEncoder.encode(matcher.group(1), "UTF-8");
				}
				
				String value = SUBDIVISION_NAME_KEY + matcher.group(1).trim() + 
					FIELD_SEPARATOR + MAPID_BOOK_KEY + matcher.group(2).trim() + 
					FIELD_SEPARATOR + MAPID_PAGE_KEY + matcher.group(3).trim();
				
				ParsedResponse currentResponse = new ParsedResponse();
				
				responses.add(currentResponse);
				
				String secondaryLegalLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.ARCHIVE_DOCS_MODULE_IDX) + 
						"&subdivisionName=" + URLEncoder.encode(matcher.group(1).trim(), "UTF-8") + 
						"&mapidbook=" + URLEncoder.encode(matcher.group(2).trim(), "UTF-8")+ 
						"&mapidpage=" +  URLEncoder.encode(matcher.group(3).trim(), "UTF-8");
				
				String status = "&nbsp;";
				if(statusList.size() > indexOfRow && StringUtils.isNotEmpty(statusList.get(indexOfRow))) {
					status = statusList.get(indexOfRow);
				}
				
				currentResponse.setOnlyResponse("<tr><td>" + radioStart + URLEncoder.encode(value, "UTF-8") + "\"> " + 
						"</td><td><a href=\"" +  
						secondaryLegalLink + "\">" + matcher.group(1) + 
						"</a></td><td>" + status + "</td><td>" +  matcher.group(2) + "</td><td>" +  matcher.group(3) + "</td></tr>");

				
			} catch (Exception e) {
				logger.error("Some error while parsing " + matcher.group());
			} finally {
				indexOfRow++;
			}
		}
		
		if(firstNameFound != null) {
			
			String previousLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.TYPE_NAME_MODULE_IDX) + "&subdivisionName=" + firstNameFound + "&prevLink=1";;
			String nextLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.TYPE_NAME_MODULE_IDX) + "&subdivisionName=" + firstNameFound + "&nextLink=1";
			outputTable.append("<tr><td colspan=\"5\">");
			outputTable.append("<a href=\"").append(previousLink).append("\">Previous</a>");
			outputTable.append("&nbsp");
			outputTable.append("<a href=\"").append(nextLink).append("\">Next</a>");
			outputTable.append("</td></tr>");
			
		}
		
		return responses;
	}
    
    private Collection<ParsedResponse> smartParseSecondaryLookup(
			ServerResponse response, String rsResponce,
			StringBuilder outputTable) {
    	Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
    	
    	String[] possiblePages = rsResponce.split(Pattern.quote(DELIMITER_INTERESTING_DATA));
    	String rawQuerry = response.getRawQuerry() + "&";
    	String encodedSubdivisionName = org.apache.commons.lang.StringUtils.substringBetween(rawQuerry, "subdivisionName=", "&");
    	String encodedMapidBook = org.apache.commons.lang.StringUtils.substringBetween(rawQuerry, "mapidbook=", "&");
    	String encodedMapidPage = org.apache.commons.lang.StringUtils.substringBetween(rawQuerry, "mapidpage=", "&");
    	
    	
    	String remarks = null;
		String sub = null;
		String mapId = null;
		String filedDate = null;
		String vacateDate = null;
		String docNumber = null;
		String forceError = null;
		
		
		byte[] beforeRemarksBytes = new byte[] {0x02, 0x09, 0x14, 0x00, 0x11, 
				(byte) 0xEF, (byte) 0xBF, (byte) 0xBD, (byte) 0xEF, (byte) 0xBF, (byte) 0xBD, (byte) 0xEF, (byte) 0xBF, (byte) 0xBD
			};
		byte[] afterRemarksBytes = new byte[] {0x02, (byte) 0xEF, (byte) 0xBF};
		
		byte[] beforeSubBytes = new byte[] {0x11, 0x09,
				(byte) 0xEF, (byte) 0xBF, (byte) 0xBD, (byte) 0xEF, (byte) 0xBF, (byte) 0xBD
			};
			
		byte[] beforePriLow = new byte[] {0x11, 0x03,
				(byte) 0xEF, (byte) 0xBF, (byte) 0xBD, (byte) 0xEF, (byte) 0xBF, (byte) 0xBD
			};
    	
    	for (int pageIndex = 0; pageIndex < possiblePages.length; pageIndex++) {
    		String dirtyInput = possiblePages[pageIndex];
        	String cleanedInput = dirtyInput.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", " ").replaceAll("", " ");
        	
        	if(pageIndex == 0) {
	    		int indexOfBeforeRemarksBytes = dirtyInput.indexOf(new String(beforeRemarksBytes));
	    		if(indexOfBeforeRemarksBytes > 0) {
	    			int indexOfAfterRemarksBytes = dirtyInput.indexOf(new String(afterRemarksBytes), indexOfBeforeRemarksBytes + 1);
	    			if(indexOfAfterRemarksBytes > 0) {
	    				remarks = dirtyInput.substring(indexOfBeforeRemarksBytes + (new String(beforeRemarksBytes)).length(), indexOfAfterRemarksBytes);
	    				if(!remarks.matches("[\\p{Alnum}\\p{Punct}\\s]+")) {
	    					remarks = null;
	    				}
	    			}
	    			
	    			int indexOfBeforeSubBytes = dirtyInput.lastIndexOf(new String(beforeSubBytes), indexOfBeforeRemarksBytes);
	    			if(indexOfBeforeSubBytes > 0) {
	    				sub = dirtyInput.substring(indexOfBeforeSubBytes + new String(beforeSubBytes).length(), indexOfBeforeRemarksBytes);
	    				if(!sub.matches("[\\p{Alnum}\\p{Punct}\\s]+")) {
	    					sub = null;
	    				}
	    				
	    				
	    				int indexOfBeforeMapid = dirtyInput.lastIndexOf(new String(beforePriLow), indexOfBeforeSubBytes);
	    				if(indexOfBeforeMapid > 0) {
	    					mapId = new String(dirtyInput.substring(indexOfBeforeMapid + new String(beforePriLow).length())).replaceFirst("(?is)\\A([A-Z\\d\\s]+).*", "$1");
	    				}
	    			}
	    		}
	        	
	    		Matcher matcher = FILED_DATE.matcher(cleanedInput);
	    		if(matcher.find()) {
	    			filedDate = matcher.group(1) + "/" + matcher.group(2);
	    		}
	    		matcher = VACATE_DATE.matcher(cleanedInput);
	    		if(matcher.find()) {
	    			vacateDate = matcher.group(1) + "/" + matcher.group(2);
	    		}
	    		matcher = DOC_NUMBER.matcher(cleanedInput);
	    		if(matcher.find()) {
	    			docNumber = matcher.group(1);
	    		}
	    		
	    		forceError = org.apache.commons.lang.StringUtils.defaultString(
	    					org.apache.commons.lang.StringUtils.substringBetween(cleanedInput, "Force Error:", "Sub:").trim());
	    		
	    		StringBuilder header = new StringBuilder("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\"><tr><td><b>Pri Low:</b></td><td>");
	    		header
	    			.append(org.apache.commons.lang.StringUtils.defaultString(mapId))
	    			.append("</td><td>Filed date:</td><td>")
	    			.append(org.apache.commons.lang.StringUtils.defaultString(filedDate))
	    			.append("</td><td><b>Doc #:</b></td><td>")
	    			.append(org.apache.commons.lang.StringUtils.defaultString(docNumber))
	    			.append("</td></tr><tr><td>Hi:</td><td></td><td>Vacate Date:</td><td>")
	    			.append(org.apache.commons.lang.StringUtils.defaultString(vacateDate))
	    			.append("</td><td>Force Error:</td><td>")
	    			.append(org.apache.commons.lang.StringUtils.defaultString(forceError)).append("</td></tr><tr><td><b>Sub:</b></td><td colspan=\"3\">")
	    			.append(org.apache.commons.lang.StringUtils.defaultString(sub))
	    			.append("</td><td>S/P:</td><td></td></tr><tr><td><b>Remarks:</b></td><td colspan=\"3\">")
	    			.append(org.apache.commons.lang.StringUtils.defaultString(remarks))
	    			.append("</td><td colspan=\"2\">MR</td></tr></table><br><br>")
	    			;
	    		response.getParsedResponse().setHeader(header.toString());        	
        	}
    		
    		byte[] beforeFirstRow1 = new byte[] { 0x02, (byte) 0x1D,
    				(byte) 0x20, (byte) 0xEF, (byte) 0xBF, (byte) 0xBD,
    				(byte) 0xEF, (byte) 0xBF, (byte) 0xBD, 0x00, 0x01, 0x00,
    				(byte) 0xEF, (byte) 0xBF, (byte) 0xBD };
    		String[] rows = dirtyInput.split(new String(beforeFirstRow1));
    		if (rows.length == 11) {
    			rows[10] = rows[10].replace(new String(new byte[] { 0x11,
    					(byte) 0xEF, (byte) 0xBF, (byte) 0xBD }), new String(
    					new byte[] { 0x11, 0x14 }));
    		}
    		
    		
    		String splitter = new String(new byte[] { 0x11 });
    		if(rows.length == 1) {
    			outputTable.append("No secondary legal found");
    		}
    		String radioStart = "<input type=\"radio\" name=\"doclink\" value=\"";
    		for (int i = 1; i < rows.length; i++) {
    			try {
    				splitter = new String(new byte[] { 0x11, (byte) (i + 10) });
    				String[] possibleColumns = rows[i].split(splitter);

    				StringBuilder rowToShow = new StringBuilder("<tr>");
    				
    				String lotLow = "";
    				String blockLow = "";
    				String lotHigh = "";
    				String blockHigh = "";
    				
    				for (int indexColumn = 1; indexColumn < possibleColumns.length; indexColumn++) {
    					String readableColumn = possibleColumns[indexColumn]
    							.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]+", "")
    							.replaceAll(" ", " ").trim();
    					
    					if (indexColumn == 1) {
    						
    						Matcher simpleMatcher = GenericSKLDFunctions.SIMPLE_BLOCK_LOT_PATTERN.matcher(readableColumn);
    						if(simpleMatcher.find()) {
    							if(StringUtils.isNotEmpty(simpleMatcher.group(1))){
    								blockLow = simpleMatcher.group(1);
    							}
    							if(StringUtils.isNotEmpty(simpleMatcher.group(2))){
    								lotLow = simpleMatcher.group(2);
    							}
    						}
    						
    						simpleMatcher = GenericSKLDFunctions.SIMPLE_BLOCK_LOT_PATTERN.matcher(
    								possibleColumns[indexColumn + 1]
    								                .replaceAll("[^\\p{Alnum}\\p{Punct}\\s]+", "")
    						                        .replaceAll(" ", " ").trim());
    						if(simpleMatcher.find()) {
    							if(StringUtils.isNotEmpty(simpleMatcher.group(1))){
    								blockHigh = simpleMatcher.group(1);
    							}
    							if(StringUtils.isNotEmpty(simpleMatcher.group(2))){
    								lotHigh = simpleMatcher.group(2);
    							}
    						}
    						
    						String value = GenericSKLD.SUBDIVISION_NAME_KEY + URLDecoder.decode(encodedSubdivisionName, "UTF-8") + 
    							GenericSKLD.FIELD_SEPARATOR + GenericSKLD.MAPID_BOOK_KEY + URLDecoder.decode(encodedMapidBook, "UTF-8") + 
    							GenericSKLD.FIELD_SEPARATOR + GenericSKLD.MAPID_PAGE_KEY + URLDecoder.decode(encodedMapidPage, "UTF-8") + 
    							GenericSKLD.FIELD_SEPARATOR + GenericSKLD.BLOCK_LOW + blockLow + 
    							GenericSKLD.FIELD_SEPARATOR + GenericSKLD.BLOCK_HIGH + blockHigh + 
    							GenericSKLD.FIELD_SEPARATOR + GenericSKLD.LOT_LOW + lotLow + 
    							GenericSKLD.FIELD_SEPARATOR + GenericSKLD.LOT_HIGH + lotHigh;
    						
    						rowToShow.append("<tr><td>").append(radioStart).append(URLEncoder.encode(value, "UTF-8"))
    							.append("\"></td><td>").append(readableColumn.trim()).append("</td>");
    					} else if (indexColumn == 2	|| indexColumn == 4 || indexColumn == 5
    							) {
    						rowToShow.append("<td>").append(readableColumn.trim())
    								.append("</td>");
    					} else if(indexColumn == 6 || indexColumn == 8) {
    						rowToShow.append("<td>").append(readableColumn.trim());
    					} else if(indexColumn == 7 || indexColumn == 9) {
    						if(readableColumn.isEmpty()) {
    							rowToShow.append("</td>");
    						} else {
    							rowToShow.append("/").append(readableColumn).append("</td>");
    						}
    					} else if(indexColumn == 10) {
    						rowToShow.append("<td>").append(readableColumn.trim().replace("(", ""))
    						.append("</td>");
    					}
    				}
    				rowToShow.append("</tr>\n");
    				
    				ParsedResponse currentResponse = new ParsedResponse();
    				
    				responses.add(currentResponse);
    				currentResponse.setOnlyResponse(rowToShow.toString());
    			
    			} catch (Exception e) {
    				logger.error("Some error while parsing line " + i);
    			}
    			
    		}	
		}
    	
    	
		
		return responses;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder newTable) {
    	Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
    	Map<RegisterDocumentI, ParsedResponse> docToParsedRes = new LinkedHashMap<RegisterDocumentI, ParsedResponse>(); 
		newTable.append("<table BORDER='1' CELLPADDING='2'>");
		int numberOfUncheckedElements = 0;
		String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
		
		int lastPartyFound = 0;
		String[] intermediaryResults = table.split("\\{[\\d*]+[-]+");
		for (String intermediaryResult : intermediaryResults) {
			if(intermediaryResult.trim().isEmpty()) {
				continue;
			}
			List<ParsedResponse> prChilds = new ArrayList<ParsedResponse>();
			String[] lines = intermediaryResult.split("[\\r\\n]+");
			Vector<String> validLines = new Vector<String>();
			for (String line : lines) {
				if(StringUtils.isNotEmpty(line)) {
					validLines.add(line);
				}
			}
			
			newTable.append("<tr><td>");
			
			ParsedResponse currentResponse = new ParsedResponse();
			ResultMap resultMap = new ResultMap();
			RegisterDocumentI document = null;
			int validPartyIndex = 0;
			StringBuilder tableColumn = new StringBuilder();
			String party1 = "";
			String party2 = "";
			int noOfPages = 0;
			
			int[] lineTypes = new int[validLines.size()];
			
			for (int i = 0; i < validLines.size(); i++) {
				lineTypes[i] = GenericSKLDFunctions.LINE_TYPE_UNKNOWN;
				String line = validLines.elementAt(i);
				
				if(!line.startsWith("{")) {	//B5704 - too hard to explain
					
					logger.info("line_without: " + line);
					
					if( (i >= 2 && !line.contains(":") &&
							(lineTypes[i-1] ==  GenericSKLDFunctions.LINE_TYPE_PARTY_1 || 
									lineTypes[i-1] ==  GenericSKLDFunctions.LINE_TYPE_PARTY_2 || 
									lineTypes[i-1] ==  GenericSKLDFunctions.LINE_TYPE_PARTY_NEXT))  ) {
						//now it might be a next address line
						
						String modifiedLine = line.replaceAll("^\\s+", "");
						if(line.length() - modifiedLine.length() < 11) {
							line = "           " + modifiedLine;
						}
						
					}
					line = "{" + line;
					validLines.set(i, line);
					
				}
				
				
				
				Matcher tempCrossRefMatcher = GenericSKLDFunctions.CROSS_REF_BY_PATTERN.matcher(line);
				if( tempCrossRefMatcher.find() ) {
					lineTypes[i] = GenericSKLDFunctions.LINE_TYPE_CROSS_REF_BY;
					tempCrossRefMatcher.reset();
					ro.cst.tsearch.servers.functions.GenericSKLDFunctions.
						parseCrossRef(tempCrossRefMatcher, resultMap, this);
					Object tempObject = resultMap.get("tmpModule");
					String tempString = (String) resultMap.get("tmpBookPageLink");
					if(tempObject != null) {
						try {
							resultMap.remove("tmpModule");
						} catch (Exception e) {	logger.error("Exception while removing temporary module", e); }
						
						if (tempObject instanceof TSServerInfoModule) {
							TSServerInfoModule module = (TSServerInfoModule) tempObject;
							module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Save with crossreferences");
							ParsedResponse prChild = new ParsedResponse();
							LinkInPage linkInPage = new LinkInPage(module,
									TSServer.REQUEST_SEARCH_BY_REC);
							if(tempString != null) {
								linkInPage.setOnlyLink("Book/Page " + tempCrossRefMatcher.group(1).trim() + 
										//" with doctype " + tempCrossRefMatcher.group(3).trim() +
										" was already searched for.");
							} else {
								linkInPage.setOnlyLink("Instrument " + tempCrossRefMatcher.group(2).trim() + 
										//" with doctype " + tempCrossRefMatcher.group(3).trim() +
										" was already searched for.");
							}
							prChild.setPageLink(linkInPage);
							prChilds.add(prChild);
						}
					}
					
					line = line.substring(1).replaceAll("\\+([\\s\\r\\n$]+)", "$1").replaceAll(" ", "&nbsp;");
					if(tempString != null) {
						try {
							resultMap.remove("tmpBookPageLink");
						} catch (Exception e) {logger.error("Exception while removing temporary module", e);}
						
						line = line.replace(tempCrossRefMatcher.group(1).trim(), tempString);
					}
					tempString = (String) resultMap.get("tmpInstrumentLink");
					if(tempString != null) {
						try {
							resultMap.remove("tmpInstrumentLink");
						} catch (Exception e) { logger.error("Exception while removing temporary module", e); }
						
						line = line.replace(tempCrossRefMatcher.group(2).trim(), tempString);
					} 
					tableColumn.append(line).append("<br>");
					
					continue;	//jump to next line
				} else {
					tempCrossRefMatcher = GenericSKLDFunctions.CROSS_REF_TO_PATTERN.matcher(line);
					if(tempCrossRefMatcher.find()) {
						lineTypes[i] = GenericSKLDFunctions.LINE_TYPE_CROSS_REF_TO;
						tempCrossRefMatcher.reset();
						ro.cst.tsearch.servers.functions.GenericSKLDFunctions.
							parseCrossRef(tempCrossRefMatcher, resultMap, this);
						Object tempObject = resultMap.get("tmpModule");
						String tempString = (String) resultMap.get("tmpBookPageLink");
						if(tempObject != null) {
							try {
								resultMap.remove("tmpModule");
							} catch (Exception e) {
								logger.error("Exception while removing temporary module", e);
							}
							if (tempObject instanceof TSServerInfoModule) {
								TSServerInfoModule module = (TSServerInfoModule) tempObject;
								module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Save with crossreferences");
								ParsedResponse prChild = new ParsedResponse();
								LinkInPage linkInPage = new LinkInPage(module,
										TSServer.REQUEST_SEARCH_BY_REC);
								if(tempString != null) {
									linkInPage.setOnlyLink("Book/Page " + tempCrossRefMatcher.group(1).trim() + 
											//" with doctype " + tempCrossRefMatcher.group(3).trim() +
											" was already searched for.");
								} else {
									linkInPage.setOnlyLink("Instrument " + tempCrossRefMatcher.group(2).trim() + 
											//" with doctype " + tempCrossRefMatcher.group(3).trim() +
											" was already searched for.");
								}
								
								prChild.setPageLink(linkInPage);
								
								prChilds.add(prChild);
							}
						}
						
						line = line.substring(1)
							.replaceAll("\\+([\\s\\r\\n$]+)", "$1")
							.replaceAll(" ", "&nbsp;");
						if(tempString != null) {
							try {
								resultMap.remove("tmpBookPageLink");
							} catch (Exception e) {
								logger.error("Exception while removing temporary module", e);
							}
							line = line.replace(tempCrossRefMatcher.group(1).trim(), tempString);
						}
						tempString = (String) resultMap.get("tmpInstrumentLink");
						if(tempString != null) {
							try {
								resultMap.remove("tmpInstrumentLink");
							} catch (Exception e) {
								logger.error("Exception while removing temporary module", e);
							}
							line = line.replace(tempCrossRefMatcher.group(2).trim(), tempString);
						}
						tableColumn.append(line).append("<br>");
						continue;	//jump to next line
					} else {
						//if this line is not crossRefLine treat it as usual
						tableColumn.append(line.substring(1).replaceAll(" ", "&nbsp;").replaceAll("\\+([\\s\\r\\n$]+)", "$1")).append("<br>");		
					}
				}
				
				Matcher matcher = null;
				if(i == 0) {
					matcher = GenericSKLDFunctions.FIRST_LINE_PATTERN.matcher(line);
					if(matcher.find()) {
						ro.cst.tsearch.servers.functions.GenericSKLDFunctions.
							parseBookPageInstrumentLine(matcher, resultMap);
						responses.add(currentResponse);
						if (matcher.group(7) != null){
							String pages = matcher.group(7);
							if (StringUtils.isNotEmpty(pages)){
								try {
									noOfPages = Integer.parseInt(pages.trim());
								} catch (NumberFormatException nfe) {
									logger.error("SKLD can't parse the number of pages for image: " + line, nfe);
								}
							}
						}
					} else {
						matcher = GenericSKLDFunctions.FIRST_LINE_STR_PATTERN.matcher(line);
						if(matcher.find()) {
							ro.cst.tsearch.servers.functions.GenericSKLDFunctions.
								parseSTRInstrumentLine(matcher, resultMap);
							responses.add(currentResponse);
						} else {
							logger.error("SKLD first line is illegal for line: " + line);
							break;
						}
					}
				} else {
					matcher = GenericSKLDFunctions.PARTY_LINE_PATTERN.matcher(line);
					if(matcher.find()) {
						lastPartyFound = Integer.parseInt(matcher.group(1));
						if(validPartyIndex == 0) {	//if we found the first party 
							
							validPartyIndex = i;	//mark the position so we know
							//let's go back and check for address
							//we have an address if the line before does not match first line or remarks line
							//change it if you do not like it
							String previousLine = validLines.elementAt( i - 1);
							if( i > 1 && lineTypes[i - 1] != GenericSKLDFunctions.LINE_TYPE_REMARKS /*!GenericSKLDFunctions.REMARKS_LINE_PATTERN.matcher(previousLine).find()*/) {
								//we are lucky, we found an address, let's parse it using position
								//i'm sorry if you need to fix this
								Matcher addressMatcher = GenericSKLDFunctions.FULL_ADDRESS_PATTERN.matcher(previousLine);
								if(addressMatcher.find()) {
									String[] addressTokens = StringFormats.parseAddress(addressMatcher.group(1));
									resultMap.put("PropertyIdentificationSet.StreetNo", addressTokens[0]);
									resultMap.put("PropertyIdentificationSet.StreetName", addressTokens[1]);
									resultMap.put("PropertyIdentificationSet.City", addressMatcher.group(2));
									resultMap.put("PropertyIdentificationSet.Zip", addressMatcher.group(3));
								}
							}
							String fullName = matcher.group(2);
							if(fullName.endsWith(" Adr")) {
								fullName = fullName.substring(0, fullName.length() - 3).trim();
							}
							if(!fullName.toLowerCase().contains("xxx-xx") && !fullName.toUpperCase().startsWith("ID ") && !fullName.toUpperCase().startsWith("MERS ") ) {
								
								if(StringUtils.isEmpty((String)resultMap.get("PropertyIdentificationSet.StreetName")) &&  
										fullName.matches(".*\\b\\d+\\b.*")) {
									lineTypes[i] = GenericSKLDFunctions.LINE_TYPE_FULL_ADDRESS;
									Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) 
										resultMap.get("PropertyIdentificationSet");
									if(pisVector == null) {
										pisVector = new Vector<PropertyIdentificationSet>();
										resultMap.put("PropertyIdentificationSet", pisVector);
									}
									PropertyIdentificationSet pis = new PropertyIdentificationSet();
									String[] addressTokens = StringFormats.parseAddress(fullName);
									pis.setAtribute("StreetNo", addressTokens[0]);
									pis.setAtribute("StreetName", addressTokens[1]);
									pisVector.add(pis);
								} else {
									lineTypes[i] = GenericSKLDFunctions.LINE_TYPE_PARTY_1;
									party1 = NameCleaner.cleanName(fullName, true);
									if(party1.equalsIgnoreCase("RECORD OWNER") || 
											party1.equalsIgnoreCase("PARTY UNKNOWN")) {
										party1 = "";
									}
								}
							}
							//now let's parse the owner
							
						} else {
							lineTypes[i] = GenericSKLDFunctions.LINE_TYPE_PARTY_2;
							String fullName = matcher.group(2);
							if(fullName.endsWith(" Adr")) {
								fullName = fullName.substring(0, fullName.length() - 3).trim();
							}
							if(!fullName.toLowerCase().contains("xxx-xx") && !fullName.toUpperCase().startsWith("ID ") && !fullName.toUpperCase().startsWith("MERS ") ) {
								if(!NameUtils.isCompany(fullName) && 
										StringUtils.isEmpty((String)resultMap.get("PropertyIdentificationSet.StreetName")) &&  
										fullName.matches(".*\\b\\d+\\b.*")) {
									Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) 
										resultMap.get("PropertyIdentificationSet");
									if(pisVector == null) {
										pisVector = new Vector<PropertyIdentificationSet>();
										resultMap.put("PropertyIdentificationSet", pisVector);
									}
									PropertyIdentificationSet pis = new PropertyIdentificationSet();
									String[] addressTokens = StringFormats.parseAddress(fullName);
									pis.setAtribute("StreetNo", addressTokens[0]);
									pis.setAtribute("StreetName", addressTokens[1]);
									pisVector.add(pis);
								} else {
									party2 = NameCleaner.cleanName(fullName, true);
									if(party2.equalsIgnoreCase("RECORD OWNER") || 
											party2.equalsIgnoreCase("PARTY UNKNOWN")) {
										party2 = "";
									}
								}
							}
						}
					} else if (GenericSKLDFunctions.PARTY_NEXT_LINE_PATTERN.matcher(line).find()) {
						
						lineTypes[i] = GenericSKLDFunctions.LINE_TYPE_PARTY_NEXT;
						
						matcher = GenericSKLDFunctions.PARTY_NEXT_LINE_PATTERN.matcher(line);
						matcher.find();
						if(lastPartyFound == 1) {
							String fullName = matcher.group(1);
							if(fullName.endsWith(" Adr")) {
								fullName = fullName.substring(0, fullName.length() - 3).trim();
							}
							if(!fullName.toLowerCase().contains("xxx-xx") && !fullName.toUpperCase().startsWith("ID ") && !fullName.toUpperCase().startsWith("MERS ") ) {
								if(StringUtils.isEmpty((String)resultMap.get("PropertyIdentificationSet.StreetName")) &&  
										fullName.matches(".*\\b\\d+\\b.*")) {
									
									Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) 
										resultMap.get("PropertyIdentificationSet");
									if(pisVector == null) {
										pisVector = new Vector<PropertyIdentificationSet>();
										resultMap.put("PropertyIdentificationSet", pisVector);
									}
									PropertyIdentificationSet pis = new PropertyIdentificationSet();
									String[] addressTokens = StringFormats.parseAddress(fullName);
									pis.setAtribute("StreetNo", addressTokens[0]);
									pis.setAtribute("StreetName", addressTokens[1]);
									pisVector.add(pis);
								} else {
									fullName = NameCleaner.cleanName(fullName, true);
									if(fullName.equalsIgnoreCase("RECORD OWNER") || 
											fullName.equalsIgnoreCase("PARTY UNKNOWN")) {
										//nothing
									} else {
										party1 += "/" + fullName;
									}
								}
								
							}
						} else if(lastPartyFound == 2) {
							String fullName = matcher.group(1);
							if(fullName.endsWith(" Adr")) {
								fullName = fullName.substring(0, fullName.length() - 3).trim();
							}
							if(!fullName.toLowerCase().contains("xxx-xx") && !fullName.toUpperCase().startsWith("ID ") && !fullName.toUpperCase().startsWith("MERS ") ) {
								if(StringUtils.isEmpty((String)resultMap.get("PropertyIdentificationSet.StreetName")) &&  
										fullName.matches(".*\\b\\d+\\b.*")) {
									
									Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) 
										resultMap.get("PropertyIdentificationSet");
									if(pisVector == null) {
										pisVector = new Vector<PropertyIdentificationSet>();
										resultMap.put("PropertyIdentificationSet", pisVector);
									}
									PropertyIdentificationSet pis = new PropertyIdentificationSet();
									String[] addressTokens = StringFormats.parseAddress(fullName);
									pis.setAtribute("StreetNo", addressTokens[0]);
									pis.setAtribute("StreetName", addressTokens[1]);
									pisVector.add(pis);
								} else {
									
									fullName = NameCleaner.cleanName(fullName, true);
									if(fullName.equalsIgnoreCase("RECORD OWNER") || 
											fullName.equalsIgnoreCase("PARTY UNKNOWN")) {
										//nothing
									} else {
										party2 += "/" + fullName;
									}
								}
							}
						}
					} else if(GenericSKLDFunctions.SECOND_ADDRESS_PATTERN.matcher(line).find()) {
						//we are lucky, we found an address, let's parse it using position
						//i'm sorry if you need to fix this
						Matcher addressMatcher = GenericSKLDFunctions.SECOND_ADDRESS_PATTERN.matcher(line);
						
						Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) 
							resultMap.get("PropertyIdentificationSet");
						if(pisVector == null) {
							pisVector = new Vector<PropertyIdentificationSet>();
							resultMap.put("PropertyIdentificationSet", pisVector);
						}
						PropertyIdentificationSet pis = new PropertyIdentificationSet();
						
						if(addressMatcher.find()) {
							String[] addressTokens = StringFormats.parseAddress(addressMatcher.group(1));
							pis.setAtribute("StreetNo", addressTokens[0]);
							pis.setAtribute("StreetName", addressTokens[1]);
							pis.setAtribute("City", addressMatcher.group(2));
							pis.setAtribute("Zip", addressMatcher.group(3));
							pisVector.add(pis);
						}
					} else if(GenericSKLDFunctions.LEGAL_SUBDIVISION_NAME_PATTERN.matcher(line).find()) {
						//TODO: if already set, then create a new pis
						matcher = GenericSKLDFunctions.LEGAL_SUBDIVISION_NAME_PATTERN.matcher(line);
						matcher.find();
						resultMap.put("PropertyIdentificationSet.SubdivisionName", 
								matcher.group(1).trim());
					} else if(GenericSKLDFunctions.LEGAL_LOT_BLOCK_PATTERN.matcher(line).find()) {
						Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) 
							resultMap.get("PropertyIdentificationSet");
						if(pisVector == null) {
							pisVector = new Vector<PropertyIdentificationSet>();
							resultMap.put("PropertyIdentificationSet", pisVector);
						}
						PropertyIdentificationSet pis = new PropertyIdentificationSet();
						ro.cst.tsearch.servers.functions.GenericSKLDFunctions.
							parseLegalLotBlock(GenericSKLDFunctions.LEGAL_LOT_BLOCK_PATTERN.matcher(line), pis);
						pisVector.add(pis);
						
						if(i + 1 < validLines.size()) {
							matcher = GenericSKLDFunctions.LEGAL_SUBDIVISION_NAME_PATTERN.matcher(validLines.elementAt(i+1));
							if(matcher.find()) {
								pis.setAtribute("SubdivisionName", matcher.group(1).trim());
								tableColumn.append(validLines.elementAt(i+1).substring(1).replaceAll(" ", "&nbsp;").replaceAll("\\+([\\s\\r\\n]*)$", "$1")).append("<br>");
								i++;	//go to next Line
							}
						}
						
					} else if(GenericSKLDFunctions.LEGAL_ARB_PATTERN.matcher(line).find()) {
						Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) 
							resultMap.get("PropertyIdentificationSet");
						if(pisVector == null) {
							pisVector = new Vector<PropertyIdentificationSet>();
							resultMap.put("PropertyIdentificationSet", pisVector);
						}
						PropertyIdentificationSet pis = new PropertyIdentificationSet();
						GenericSKLDFunctions.parseArbLine(GenericSKLDFunctions.LEGAL_ARB_PATTERN.matcher(line), pis);
						pisVector.add(pis);
						
					
					} else {
						matcher = GenericSKLDFunctions.REMARKS_LINE_PATTERN.matcher(line);
						if(matcher.find()) {
							lineTypes[i] = GenericSKLDFunctions.LINE_TYPE_REMARKS;
							try {
								GenericSKLDFunctions.parseRemarksLine(matcher, resultMap, this);
							} catch (Exception e) {
								logger.error("Error while parsing SKLD LINE_TYPE_REMARKS: " + line, e);
							}
						} else if(line.contains("* CLOSED STARTER *")) {
							resultMap.put("OtherInformationSet.Remarks", "CLOSED STARTER");
						}
					}
				}
				
				
				
			}
			
			String names[] = null;
			ArrayList<List> grantor = new ArrayList<List>();
			ArrayList<List> grantee = new ArrayList<List>();
			
			String[] gtors = party1.replaceAll(";","&").split("/");
			
			for (int i = 0; i < gtors.length; i++){
				names = StringFormats.parseNameNashville(gtors[i]);
				GenericFunctions.addOwnerNames(party1, names, grantor);
			}
			
			resultMap.put("SaleDataSet.Grantor", party1);
			try {
				resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			
			String[] gtee = party2.replaceAll(";","&").split("/");
			for (int i = 0; i < gtee.length; i++){
				names = StringFormats.parseNameNashville(gtee[i]);
				GenericFunctions.addOwnerNames(party2, names, grantee);
			}
			
			resultMap.put("SaleDataSet.Grantee", party2);
			try {
				resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			try {
				GenericFunctions1.setGranteeLanderTrustee2(resultMap, searchId,true);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			String allSSN4 = (String) resultMap.get("tmpSSN4");
			String tmpAgencyNo  = (String) resultMap.get("tmpAgencyNo");
			resultMap.removeTempDef();
			
			
			String serverDocType = (String) resultMap.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName());
			if("REMARK".equals(serverDocType)) {
				String instrumentNumber = (String) resultMap.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName());
				if(StringUtils.isEmpty(instrumentNumber)) {
					Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) 
							resultMap.get("PropertyIdentificationSet");
					if(pisVector != null) {
						for (PropertyIdentificationSet propertyIdentificationSet : pisVector) {
							String pb = propertyIdentificationSet.getAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName());
							String pp = propertyIdentificationSet.getAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName());
							
							if(StringUtils.isNotEmpty(pb) && StringUtils.isNotEmpty(pp)) {
								resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), "remark_" + pb + "/" + pp);
							}
						}
					}
				}
			}
			
			currentResponse.setParentSite(isParentSite());
			
			Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
			try {
				DocumentI rawDocumentI = bridge.importData();
				if (rawDocumentI instanceof RegisterDocumentI) {
					document = (RegisterDocumentI) rawDocumentI;

					
					RegisterDocumentI toSearchClone = document.clone();
					SKLDInstrument skldInstrumentToSearch = (SKLDInstrument)toSearchClone.getInstrument();
					skldInstrumentToSearch.setOnlyInstno(generateSpecificInstrument(toSearchClone));
					
					ParsedResponse alreadyParsed = docToParsedRes.get(toSearchClone);
					if(alreadyParsed != null) {
						//need to remove this response if it was already added
						responses.remove(currentResponse);
						
						DocumentI document2 = alreadyParsed.getDocument();
						if(document2 instanceof RegisterDocumentI) {
							RegisterDocumentI alreadySavedDoc = (RegisterDocumentI)document2;
							document.mergeDocumentsInformation(alreadySavedDoc, searchId, false, false);
							
							String alreadyResponse = alreadyParsed.getResponse();
							if(alreadyResponse.contains("View Image")) {
								alreadyParsed.setOnlyResponse(alreadyResponse.replaceFirst("(?is)(<a href=[^>]+>\\s*View\\s+Image)\\b", Matcher.quoteReplacement(tableColumn.toString()) + "$1"));
							} else {
								alreadyParsed.setOnlyResponse(alreadyResponse.replace("</td></tr>", tableColumn + "</td></tr>"));
							}
							
							String alreadyRowResponse = (String)alreadyParsed.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE);
							alreadyRowResponse = alreadyRowResponse.replace("</td></tr>", tableColumn + "</td></tr>");
							alreadyParsed.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, alreadyRowResponse);
//							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + 
//									tableColumn.toString()
//										.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1")
//										.replaceAll("View Image", "") + "</td></tr>");
							
							
						}
						
					} else {
						
						SKLDInstrument skldInstrument = (SKLDInstrument)document.getInstrument();
						skldInstrument.setOnlyInstno(generateSpecificInstrument(document));
						String instrumentNo = skldInstrument.getOriginalInstrumentNo();
						
						docToParsedRes.put(document, currentResponse);
						currentResponse.setDocument(document);
						document.setNote((String)resultMap.get("OtherInformationSet.Remarks"));
						if(allSSN4 != null) {
							document.setAllSSN(allSSN4);
						}
						if(tmpAgencyNo != null && document instanceof PriorFileDocumentI) {
							((PriorFileDocumentI)document).setAgency(tmpAgencyNo);
						}
						
						String checkBox = "checked";
						if (isInstrumentSaved("gogo", document, null) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
			    			checkBox = "saved";
			    		} else {
			    			numberOfUncheckedElements++;
			    			LinkInPage linkInPage = new LinkInPage(
			    					linkPrefix + "FK____" + document.getId(), 
			    					linkPrefix + "FK____" + document.getId(), 
			    					TSServer.REQUEST_SAVE_TO_TSD);
			    			checkBox = "<input type='checkbox' name='docLink' value='" + 
			    			linkPrefix + "FK____" + document.getId() + "'>";
			    			
			    			Object possibleModule = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
			    			
		        			if(getSearch().getInMemoryDoc(linkPrefix + "FK____" + document.getId())==null){
		        				getSearch().addInMemoryDoc(linkPrefix + "FK____" + document.getId(), currentResponse);
		        				
		        				/**
		            			 * Save module in key in additional info. The key is instrument number that should be always available. 
		            			 */
		            			String keyForSavingModules = getKeyForSavingInIntermediary(document.getInstno());
		            			getSearch().setAdditionalInfo(keyForSavingModules, possibleModule);
		        			}
		        			
		        			if(possibleModule instanceof TSServerInfoModule) {
		        				TSServerInfoModule module = (TSServerInfoModule) possibleModule;
		        				if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX){
		        					if (module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION) != null 
		        							&& module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION).toString().contains("Names for General Name Index")) {
		        	    				document.setSearchType(SearchType.GI);
		        	    			} else {
		        	    				document.setSearchType(SearchType.PI);
		        	    			}
		        				}		
		        			}
		        			for (ParsedResponse prChild : prChilds) {
		        				prChild.getPageLink().getModule().setSearchType(document.getSearchType().toString());
		        				currentResponse.addOneResultRowOnly(prChild);
							}
		        			currentResponse.setPageLink(linkInPage);
			    		}
						try {
							String realInstrument = "";
							String realYear = "";
							
							if (instrumentNo.matches("\\d+-\\d{4}")){
								realInstrument = instrumentNo.replaceAll("-\\d+", "");
								realYear = instrumentNo.replaceAll("\\d+-", "");
							} else if (instrumentNo.matches("\\w+/\\w+")){
								realInstrument = instrumentNo;
								realYear = "";
								if ("map".equalsIgnoreCase(skldInstrument.getDocSubType())){
									realInstrument = "M-" + instrumentNo;
								}
							} else if (org.apache.commons.lang.StringUtils.isNotBlank(skldInstrument.getBook())
									&& org.apache.commons.lang.StringUtils.isNotBlank(skldInstrument.getPage())){
								instrumentNo = skldInstrument.getBook() + "/" + skldInstrument.getPage();
								realYear = "";
							} else if ("arb-map".equalsIgnoreCase(skldInstrument.getDocSubType())){
								//testcase: CODouglas: PI - Section Search by Arb = 4002, Section = 35, Township = 6S, Range = 69W
								realInstrument = "m-" + org.apache.commons.lang.StringUtils.stripEnd(instrumentNo, "0");
								realYear = "";
							}
								
							if (noOfPages < 1){
								noOfPages = SkldImages.getInstance().checkForImage(
											SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericSKLD","SKLDMainUserNameImage"), 
											SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericSKLD","SKLDMainPasswordImage"), 
											getSearchAttributes().getCountyName(), 
											realInstrument, 
											realYear);
									
							}
								
								if (noOfPages > 0) {
									getSearch().addImagesToDocument(document, "&realInstrument=" + realInstrument + "&realYear=" + realYear + "&fakeName=" + instrumentNo + ".tiff");
									
									String imageLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.IMG_MODULE_IDX) + 
										"&realInstrument=" + realInstrument + "&realYear=" + realYear  + "&fakeName=" + instrumentNo + ".tiff";
									tableColumn.append("<a href=\"" + imageLink + "\" title=\"View Image\" target=\"_blank\">View Image</a>");
									
								}
						} catch (Exception e) {
							logger.error("Error while checking image for " + instrumentNo, e);
						}
						
						
						currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + tableColumn + "</td></tr>");
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + 
								tableColumn.toString()
									.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1")
									.replaceAll("View Image", "") + "</td></tr>");
						
						newTable.append(checkBox).append("</td><td>").append(tableColumn);
					}
				} else {
					logger.error("Error while importing data - Document is not RegisterDoc");
					newTable.append("Error").append("</td><td>").append(tableColumn);
				}
				
			} catch (Exception e) {
				logger.error("Error while importing data", e);
				newTable.append("Error").append("</td><td>").append(tableColumn);
			}
			newTable.append("</td></tr>");

		}
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		newTable.append("</table>");
		return responses;
    }
    
    public static void main(String[] args)  {
    	try {
			/*ServerResponse serverResponse = new ServerResponse();
			serverResponse.setResult(FileUtils.readFileToString(new File("D:\\work\\SKLD\\parsare\\001.txt")));
			GenericSKLD skld = new GenericSKLD(-2);
			skld.ParseResponse("ID_SEARCH_BY_NAME", serverResponse, ID_SEARCH_BY_NAME);
			*/

    		String[][] data = new String[][]{
    				
    				new String[]{"Boulder", "R22/24","2007"},
    				new String[]{"Boulder", "R2224","2007"},
    				new String[]{"Boulder", "R22/24",""},
    				new String[]{"Boulder", "R2224",""},
    				new String[]{"Boulder", "2224",""},
    				new String[]{"Boulder", "22/24",""},
    				new String[]{"Boulder", "2224","2007"},
    				new String[]{"Boulder", "R22/24","*"},
    				new String[]{"Boulder", "R2224","*"},
    				new String[]{"Boulder", "2224","*"},
    				new String[]{"Boulder", "22/24","*"},
    				
    				
    				new String[]{"Douglas", "H/445","2007"},
    				new String[]{"Eagle", "H/188","2008"},
    				
    				new String[]{"Jefferson", "83/146","2008"},
    				new String[]{"Jefferson", "120183","1995"},
    				
    				//new String[]{"Boulder", "2450280","2003"},
    				//new String[]{"Douglas", "149473","2003"},
    				//new String[]{"Douglas", "9706117","1997"},
    				//new String[]{"Douglas", "9615597","1996"},
    				
    				
    				
    		};
    		
    		
    		for (String[] array : data) {
    			if(array == null) {
    				continue;
    			}
				try {
					byte[] imageBytes = SkldImages.getInstance().downloadImage(
							"pinfo", "pr0p3rty", 
							array[0], array[1], array[2]);
						
					System.out.println(imageBytes.length);
				} catch (Exception e) {
					System.err.println("error on " + array[1]);
				}
				
			}
    		
			
			
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	System.out.println("Gata");
	}
    
    @Override
    protected ServerResponse SearchBy(boolean bResetQuery,
    		TSServerInfoModule module, Object sd)
    		throws ServerResponseException {
    	  	
    	if(!isParentSite() 
    			&& "true".equalsIgnoreCase(getSearchAttribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND))
    			&& (module.getModuleIdx() != TSServerInfo.NAME_MODULE_IDX
    				|| (module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION) != null 
    						&& !module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION).toString().contains("Names for General Name Index"))
    					)){
			return new ServerResponse();
		}
    	
    	if(module.getModuleIdx()==TSServerInfo.IMG_MODULE_IDX) {
    		ServerResponse response = new ServerResponse();
    		ImageI image = new Image();
    		Set<String> list = new HashSet<String>();
    		list.add("&realInstrument=" + module.getFunction(0).getParamValue() + 
    				"&realYear=" + module.getFunction(1).getParamValue() + 
    				"&fakeName=" + module.getFunction(2).getParamValue());
    		image.setLinks(list);
    		image.setContentType("image/tiff");
    		response.setImageResult(saveImage(image));
    		
    		return response;
    	}
    	
    	if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
    		if(getSearch().getSearchType() == Search.AUTOMATIC_SEARCH) {
    			if(module.getFunction(5).getSaKey()
    					.equals(SearchAttributes.LAST_REAL_TRANSFER_DATE_MMDD)) {
    				module.forceValue(5, getSearchAttribute(SearchAttributes.LAST_REAL_TRANSFER_DATE_MMDD));
    			}
    			if(module.getFunction(6).getSaKey()
    					.equals(SearchAttributes.LAST_REAL_TRANSFER_DATE_YYYY)) {
    				module.forceValue(6, getSearchAttribute(SearchAttributes.LAST_REAL_TRANSFER_DATE_YYYY));
    			}
    		}
    	} 
    	
    	if (module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX || 
    				module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX || 
    				module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
    		List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if(!modules.isEmpty()) {
				return searchByMultipleInstrument(bResetQuery, modules, sd, null);
			}
    	}
    	
    	ServerResponse serverResponseError = validateDatesForModule(module);
    	if(serverResponseError != null) {
    		return serverResponseError;
    	}
    	return super.SearchBy(bResetQuery, module, sd);
    }

	protected ServerResponse validateDatesForModule(TSServerInfoModule module) {
		//--- let's perform some data validation and ignore problems
    	HashMap<String, Integer> namePositionMap = new HashMap<String, Integer>();
    	for (int i = 0; i < module.getFunctionCount(); i++) {
			namePositionMap.put(module.getFunction(i).getParamName(), i);
		}
    	if(namePositionMap.get("startMonthDay") != null) {
	    	String field1 = module.getFunction(namePositionMap.get("startMonthDay")).getParamValue().trim();
	    	String field2 = module.getFunction(namePositionMap.get("startYear")).getParamValue().trim();
	    	
	    	ServerResponse serverResponse = new ServerResponse();
	    	ParsedResponse parsedResponse = serverResponse.getParsedResponse();
	    	
	    	SimpleDateFormat monthDayFormat = new SimpleDateFormat("MM/dd");
	    	monthDayFormat.setLenient(false);
	    	
	
	    	if(field2.isEmpty()) {
	    		module.getFunction(namePositionMap.get("startMonthDay")).forceValue("");
	    	} else {
	    		if(field2.matches("\\d\\d\\d\\d")) {
	    			if(!field1.isEmpty()) {
	    				try {
	    					monthDayFormat.parse(field1);
						} catch (ParseException e) {
							logger.error("Error while parsing start month/day", e);
							parsedResponse.setError("<font color=\"red\">Invalid start month/day entered. " +
									"It must be a valid \"Month/Day\" (ex 02/13)</font><br>Please change search criteria and try again.");
						}
	    			}
	        	} else {
	        		parsedResponse.setError("<font color=\"red\">Invalid start year entered. " +
	        				"It must be a valid year.(ex 2010)</font><br>Please change search criteria and try again.");
	        	}
	    	}
	    	
	    	field1 = module.getFunction(namePositionMap.get("endMonthDay")).getParamValue().trim();
	    	field2 = module.getFunction(namePositionMap.get("endYear")).getParamValue().trim();
	    	
	    	if(field2.isEmpty()) {
	    		module.getFunction(namePositionMap.get("endMonthDay")).forceValue("");
	    	} else {
	    		if(field2.matches("\\d\\d\\d\\d")) {
	    			if(!field1.isEmpty()) {
	    				try {
	    					monthDayFormat.parse(field1);
						} catch (ParseException e) {
							logger.error("Error while parsing start month/day", e);
							parsedResponse.setError("<font color=\"red\">Invalid start month/day entered. " +
									"It must be a valid \"Month/Day\" (ex 02/13)</font><br>Please change search criteria and try again.");
						}
	    			}
	        	} else {
	        		parsedResponse.setError("<font color=\"red\">Invalid start year entered. " +
	        				"It must be a valid year.(ex 2010)</font><br>Please change search criteria and try again.");
	        	}
	    	}
	    	if(StringUtils.isNotEmpty(parsedResponse.getError())) {
	    		return serverResponse;
	    	}
    	}
    	return null;
	}
    
    private ServerResponse searchByMultipleInstrument(
			boolean bResetQuery, List<TSServerInfoModule> modules, Object sd, Object object) {
    	
    	List<ServerResponse> allValidResponses = new ArrayList<ServerResponse>();
    	boolean firstSearchBy = true;
		for (TSServerInfoModule module : modules) {
			if (module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
				if(getSearch().getSearchType() == Search.AUTOMATIC_SEARCH) {
	    			String instrumentNo = module.getFunction(2).getParamValue().trim();
	    	    	String year = module.getFunction(10).getParamValue().trim();
	    	    	if(StringUtils.isEmpty(year)) {
	    	    		module.forceValue(10, "*");
	    	    	}
	    	    	if(instrumentNo.matches("\\d+[/\\\\]")) {
	    	    		module.forceValue(2, instrumentNo.substring(0, instrumentNo.length() - 1));
	    	    	} else if(instrumentNo.matches("\\d+-\\d{4}")) {
	    	    		module.forceValue(2, instrumentNo.substring(0, instrumentNo.indexOf("-")));
	    	    		module.forceValue(10, instrumentNo.substring(instrumentNo.indexOf("-") + 1));
	    	    	}
	    		}
			}
			
			ServerResponse serverResponseError = validateDatesForModule(module);
	    	if(serverResponseError == null && verifyModule(module)) {
	    		try {
	    			if(firstSearchBy) {
	    				firstSearchBy = false;
	    			} else {
	    				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
	    			}
					ServerResponse validResponse = super.SearchBy(bResetQuery, module, sd);
					allValidResponses.add(validResponse);
				} catch (Exception e) {
					logger.error("Error while searching for a module", e);
				}
	    	}
		}
		
		return mergeMultipleResponses(allValidResponses);
	}

	protected boolean verifyModule(TSServerInfoModule mod) {
		if (mod == null)
    		return false;
    	
    	if (mod.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
			if (mod.getFunctionCount()>0 && StringUtils.isNotEmpty(mod.getFunction(0).getParamValue())) {
				return true;
			} 
			return false;
		}
    	
    	if (mod.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) {
			if (mod.getFunctionCount()>1 && StringUtils.isNotEmpty(mod.getFunction(0).getParamValue()) && StringUtils.isNotEmpty(mod.getFunction(1).getParamValue())) {
				return true;
			} 
			return false;
		}
    	
    	if (mod.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
    		if (mod.getFunctionCount()>2 && StringUtils.isNotEmpty(mod.getFunction(2).getParamValue())) {
				return true;
			} 
			return false;
		}
    	
    	System.err.println(this.getClass()+ "I shouldn't be here!!!");
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ServerResponse mergeMultipleResponses(
			List<ServerResponse> allValidResponses) {
		
		boolean hasSomethingToSave = false;
		
		ServerResponse serverResponseFinal = null;
		for (ServerResponse serverResponse : allValidResponses) {
			ParsedResponse parsedResponse = serverResponse.getParsedResponse();
	    	String rsResponce = serverResponse.getResult();
	    	
	    	checkErrorMessages(parsedResponse, rsResponce);
	    	
	    	if(StringUtils.isEmpty(parsedResponse.getError())) {
	    		//Do Something
	    		if(serverResponseFinal == null) {
	    			serverResponseFinal = serverResponse;
	    		} else {
	    			Vector allParsedResponse = serverResponseFinal.getParsedResponse().getResultRows();
	    			allParsedResponse.addAll(parsedResponse.getResultRows());
	    			serverResponseFinal.getParsedResponse().setResultRows(allParsedResponse);
	    			if (StringUtils.isEmpty(serverResponseFinal.getParsedResponse().getHeader())) {
	    				serverResponseFinal.getParsedResponse().setHeader(parsedResponse.getHeader());
	    			}
	    		}
	    		if (!hasSomethingToSave && parsedResponse.getFooter().contains(RequestParams.PARENT_SITE_SAVE_TYPE)) {
    				hasSomethingToSave = true;
    			}
	    	}
		}
		
		if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
			String footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 0, hasSomethingToSave?1:0);
			serverResponseFinal.getParsedResponse().setFooter(footer);
		}
		
		//ServerResponse serverResponse = new ServerResponse();
    	//serverResponse.getParsedResponse().setResultRows(allParsedResponse);
    	
		return serverResponseFinal;
	}

	@Override
    public String getContinueForm(String p1, String p2, long searchId) {
    	
    	long userId = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getID().longValue();
    	
    	String form = "<br>\n<form name=\"formContinue\" "        	
        	+ "action=\"/title-search/jsp/newtsdi/tsdindexpage.jsp?searchId="+searchId+"&userId=" +  userId + "\""
        	+ " method=\"POST\">\n"
        	+ "<input  type=\"submit\" class=\"button\" name=\"Button\" value=\"Continue\">\n"
            + "</form>";
		return form;
    }
    
    @Override
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
    	
    	Search global = getSearch();
    	List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
    	int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
			
			boolean disableNameGI = ServerConfig.getBoolean("disable.co.name.gi.fix", false);
			boolean disableStarterGI = ServerConfig.getBoolean("disable.co.starter.fix", false);
			
   			String countyNo = siteOptionMap.get(miServerID);
	    	
	    	TSServerInfoModule module = null;
	    	
	    	RejectAlreadySavedDocumentsForUpdateFilter rejectAlreadySavedDocumentsForUpdateFilter = null;
	    	if(isUpdate()) {
	    		rejectAlreadySavedDocumentsForUpdateFilter = new RejectAlreadySavedDocumentsForUpdateFilter(searchId);
	    	}
	    	
	    	GenericMultipleLegalFilter legalFilter = new GenericMultipleLegalFilter(searchId);
	    	legalFilter.setUseLegalFromSearchPage(true);
	    	legalFilter.setThreshold(new BigDecimal(0.7));
	    	legalFilter.disableAll();
	    	legalFilter.setEnableLot(true);
	    	legalFilter.setEnableBlock(true);
	    	legalFilter.setEnablePlatBook(true);
	    	legalFilter.setEnablePlatPage(true);
	    	legalFilter.setEnableSection(true);
	    	legalFilter.setEnableTownship(true);
	    	legalFilter.setEnableRange(true);
	    	
	    	GenericMultipleAddressFilter addressFilter 	= new GenericMultipleAddressFilter(searchId);
			//if(isUpdate) {
				for (AddressI address : getSearchAttributes().getForUpdateSearchAddressesNotNull(getServerID())) {
					if(StringUtils.isNotEmpty(address.getStreetName())) {
						addressFilter.addNewFilterFromAddress(address);
					}
				}
			//}
			
			FilterResponse nameFilterOwner 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, global.getID(), null );
			((GenericNameFilter)nameFilterOwner).setUseSynonymsForCandidates(true);
			((GenericNameFilter)nameFilterOwner).setUseSynonymsBothWays(true);
			((GenericNameFilter)nameFilterOwner).setIgnoreMiddleOnEmpty(true);
			nameFilterOwner.setInitAgain(true);
			FilterResponse nameFilterBuyer 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, global.getID(), null );
			((GenericNameFilter)nameFilterBuyer).setUseSynonymsForCandidates(true);
			((GenericNameFilter)nameFilterBuyer).setUseSynonymsBothWays(true);
			((GenericNameFilter)nameFilterBuyer).setIgnoreMiddleOnEmpty(true);
			nameFilterBuyer.setInitAgain(true);
			
			
			FilterResponse lastTransferDateFilter = new LastTransferDateFilter(searchId);
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
			InstrumentSKLDIterator instrumentSKLDIterator = new InstrumentSKLDIterator(searchId);
			module.addIterator(instrumentSKLDIterator);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
			module.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
			module.forceValue(8, countyNo);
			addBetweenDateTest(module, true, false, true);
			if(rejectAlreadySavedDocumentsForUpdateFilter != null) {
				module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
			}
			moduleList.add(module);
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
			InstrumentSKLDIterator bookPageSKLDIterator = new InstrumentSKLDIterator(searchId, true);
			module.addIterator(bookPageSKLDIterator);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			module.forceValue(8, countyNo);
			addBetweenDateTest(module, true, false, true);
			if(rejectAlreadySavedDocumentsForUpdateFilter != null) {
				module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
			}
			moduleList.add(module);
			
	
	    	module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
	    	module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_LEGAL_MAP_ID);
			module.clearSaKeys();
			module.setSaKey(5, SearchAttributes.FROMDATE_MMDD);
			module.setSaKey(6, SearchAttributes.FROMDATE_YEAR);
			module.setSaKey(7, SearchAttributes.TODATE_MMDD);
			module.setSaKey(8, SearchAttributes.TODATE_YEAR);
			LegalSKLDIterator it = new LegalSKLDIterator(searchId, getDataSite());
			module.addIterator(it);
			module.addFilter(legalFilter);
			addBetweenDateTest(module, true, false, true);
			if(rejectAlreadySavedDocumentsForUpdateFilter != null) {
				module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
			}
			module.addCrossRefValidator(legalFilter.getValidator());
			module.forceValue(10, countyNo);
			if(disableStarterGI) {
				module.forceValue(14, "N");
			}
			moduleList.add(module);
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SECTION_LAND_MODULE_IDX));
	    	module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_LEGAL_MAP_ID);
			module.clearSaKeys();
			module.setSaKey(4, SearchAttributes.FROMDATE_MMDD);
			module.setSaKey(5, SearchAttributes.FROMDATE_YEAR);
			module.setSaKey(6, SearchAttributes.TODATE_MMDD);
			module.setSaKey(7, SearchAttributes.TODATE_YEAR);
			UnplattedSKLDIterator unplattedSKLDIterator = new UnplattedSKLDIterator(searchId);
			unplattedSKLDIterator.setPlattedIterator(it);
			module.addIterator(unplattedSKLDIterator);
			module.addFilter(legalFilter);
			if(rejectAlreadySavedDocumentsForUpdateFilter != null) {
				module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
			}
			module.addCrossRefValidator(legalFilter.getValidator());
			module.forceValue(9, countyNo);
			if(disableStarterGI) {
				module.forceValue(12, "N");
			}
			moduleList.add(module);
	    	
			if(hasStreet()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_ADDRESS_NO_NAME);
				module.addFilter(addressFilter);
				module.addFilter(legalFilter);
				addBetweenDateTest(module, true, false, true);
				module.addCrossRefValidator(addressFilter.getValidator());
				module.addCrossRefValidator(legalFilter.getValidator());
				if(rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
					module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
				}
				module.forceValue(2, countyNo);
				moduleList.add(module);
			}
				//if(isUpdate) {
			for (AddressI address : getSearchAttributes().getForUpdateSearchAddressesNotNull(getServerID())) {
				if(StringUtils.isNotEmpty(address.getStreetName())) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.clearSaKeys();
					module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
							TSServerInfoConstants.VALUE_PARAM_ADDRESS_NO_NAME);
					
					module.addFilter(addressFilter);
					module.addFilter(legalFilter);
					addBetweenDateTest(module, true, false, true);
					module.addCrossRefValidator(addressFilter.getValidator());
					module.addCrossRefValidator(legalFilter.getValidator());
					
					module.addFilter(new BetweenDatesFilterResponse(searchId));
					if(rejectAlreadySavedDocumentsForUpdateFilter != null) {
						module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
						module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
					}
					module.forceValue(0, address.getNumber());
					module.forceValue(1, address.getStreetName());
					module.forceValue(2, countyNo);
					moduleList.add(module);
				}
				
			}
		//}
					
			
			FilterResponse[] filtersO = { 
					nameFilterOwner, 
					legalFilter, 
					rejectAlreadySavedDocumentsForUpdateFilter,
					//addressFilter,  
					lastTransferDateFilter, 
					//new PropertyTypeFilter(searchId)
					};
			
			
			ArrayList<NameI> searchedNamesOwnersN = null;
			if(!disableNameGI) {
				searchedNamesOwnersN = addNameSearch( moduleList, serverInfo, countyNo, SearchAttributes.OWNER_OBJECT, "N", 
						null, 
						new FilterResponse[] {
							nameFilterOwner, 
							rejectAlreadySavedDocumentsForUpdateFilter,
							DoctypeFilterFactory.getDoctypeFilterForGeneralIndexOwnerNameSearch( searchId )
								.setForcePassIfNoReferences(true)
								.setIsUpdate(isUpdate())/*,
							lastTransferDateFilter*/}, 
						new FilterResponse[] {rejectAlreadySavedDocumentsForUpdateFilter}  
				);
			}
			
			
			ArrayList<NameI> searchedNames = addNameSearch(  moduleList, serverInfo, countyNo, SearchAttributes.OWNER_OBJECT, "P", null, 
					filtersO,
					new FilterResponse[]{legalFilter, rejectAlreadySavedDocumentsForUpdateFilter});
			
			addOCRSearch( moduleList, serverInfo, countyNo, legalFilter);
			
			
			if(!disableNameGI) {
				addNameSearch( moduleList, serverInfo, countyNo, SearchAttributes.OWNER_OBJECT, "N", 
						searchedNamesOwnersN==null?new ArrayList<NameI>():searchedNamesOwnersN, 
						new FilterResponse[] {
							nameFilterOwner, 
							rejectAlreadySavedDocumentsForUpdateFilter,
							DoctypeFilterFactory.getDoctypeFilterForGeneralIndexOwnerNameSearch( searchId )
								.setForcePassIfNoReferences(true)
								.setIsUpdate(isUpdate())/*,
							lastTransferDateFilter*/}, 
						new FilterResponse[] {rejectAlreadySavedDocumentsForUpdateFilter}  
				);
			}
			
			addNameSearch( moduleList, serverInfo, countyNo, SearchAttributes.OWNER_OBJECT, "P", 
					searchedNames==null?new ArrayList<NameI>():searchedNames, 
					filtersO,
					new FilterResponse[]{legalFilter, rejectAlreadySavedDocumentsForUpdateFilter});
			
			if(disableNameGI) {
			
				addNameSearch( moduleList, serverInfo, countyNo, SearchAttributes.OWNER_OBJECT, "N", 
						null, 
						new FilterResponse[] {
							nameFilterOwner, 
							rejectAlreadySavedDocumentsForUpdateFilter,
							DoctypeFilterFactory.getDoctypeFilterForGeneralIndexOwnerNameSearch( searchId )
								.setForcePassIfNoReferences(true)
								.setIsUpdate(isUpdate())/*,
							lastTransferDateFilter*/}, 
						new FilterResponse[] {rejectAlreadySavedDocumentsForUpdateFilter}  
				);
			
			}
			
			addNameSearch( moduleList, serverInfo, countyNo, SearchAttributes.BUYER_OBJECT, "N", 
					null, 
					new FilterResponse[] {
						nameFilterBuyer,
						rejectAlreadySavedDocumentsForUpdateFilter,
						DoctypeFilterFactory.getDoctypeFilterForGeneralIndexBuyerNameSearch( searchId )
						.setForcePassIfNoReferences(true)
						.setDocTypesForGoodDocuments(new String[]{DocumentTypes.RELEASE})
						.setIsUpdate(isUpdate())},
					new FilterResponse[] {rejectAlreadySavedDocumentsForUpdateFilter}
			);
			
			{	//adding cross-references searching from SK documents
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR.replaceAll(" RO ", " SK "));
				InstrumentSKLDIterator instrumentSKLDROIterator = new InstrumentSKLDIterator(searchId);
				instrumentSKLDROIterator.setLoadFromRoLike(true);
				module.addIterator(instrumentSKLDROIterator);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
				module.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				module.forceValue(8, countyNo);

				module.addFilter(addressFilter);
				module.addFilter(legalFilter);
				module.addFilter(new DateFilterWithDoctype(searchId));
				addBetweenDateTest(module, true, false, true);
				module.addCrossRefValidator(addressFilter.getValidator());
				module.addCrossRefValidator(legalFilter.getValidator());
				
				if(rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
					module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
				}
				
				moduleList.add(module);
				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP.replaceAll(" RO ", " SK "));
				InstrumentSKLDIterator bookPageSKLDROIterator = new InstrumentSKLDIterator(searchId, true);
				bookPageSKLDROIterator.setLoadFromRoLike(true);
				module.addIterator(bookPageSKLDROIterator);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				module.forceValue(8, countyNo);

				
				module.addFilter(addressFilter);
				module.addFilter(legalFilter);
				module.addFilter(new DateFilterWithDoctype(searchId));
				addBetweenDateTest(module, true, false, true);
				module.addCrossRefValidator(addressFilter.getValidator());
				module.addCrossRefValidator(legalFilter.getValidator());
				
				if(rejectAlreadySavedDocumentsForUpdateFilter != null) {
					module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
					module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
				}
				
				moduleList.add(module);
				
			}
			
			if(disableStarterGI) {
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
	    	module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_LEGAL_MAP_ID);
			module.clearSaKeys();
			module.setSaKey(5, SearchAttributes.FROMDATE_MMDD);
			module.setSaKey(6, SearchAttributes.FROMDATE_YEAR);
			module.setSaKey(7, SearchAttributes.TODATE_MMDD);
			module.setSaKey(8, SearchAttributes.TODATE_YEAR);
			LegalSKLDIterator secondLegalIterator = new LegalSKLDIterator(searchId, getDataSite());
			module.addIterator(secondLegalIterator);
			module.addFilter(legalFilter);
			addBetweenDateTest(module, true, false, true);
			module.addCrossRefValidator(legalFilter.getValidator());
			if(rejectAlreadySavedDocumentsForUpdateFilter != null) {
				module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
			}
			module.forceValue(10, countyNo);
			module.forceValue(14, "O");
			moduleList.add(module);
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SECTION_LAND_MODULE_IDX));
	    	module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_LEGAL_MAP_ID);
			module.clearSaKeys();
			module.setSaKey(4, SearchAttributes.FROMDATE_MMDD);
			module.setSaKey(5, SearchAttributes.FROMDATE_YEAR);
			module.setSaKey(6, SearchAttributes.TODATE_MMDD);
			module.setSaKey(7, SearchAttributes.TODATE_YEAR);
			UnplattedSKLDIterator secondUnplattedSKLDIterator = new UnplattedSKLDIterator(searchId);
			secondUnplattedSKLDIterator.setPlattedIterator(it);
			module.addIterator(secondUnplattedSKLDIterator);
			module.addFilter(legalFilter);
			addBetweenDateTest(module, true, false, true);
			module.addCrossRefValidator(legalFilter.getValidator());
			if(rejectAlreadySavedDocumentsForUpdateFilter != null) {
				module.addFilter(rejectAlreadySavedDocumentsForUpdateFilter);
				module.addCrossRefValidator(rejectAlreadySavedDocumentsForUpdateFilter.getValidator());
			}
			module.forceValue(9, countyNo);
			module.forceValue(12, "O");
			moduleList.add(module);
			
			}
		}
		serverInfo.setModulesForAutoSearch(moduleList);
    }
    
    @Override
    protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
    	ConfigurableNameIterator nameIterator = null;
    	
    	Search global = getSearch();
		SearchAttributes sa = global.getSa();
		String countyNo = siteOptionMap.get(miServerID);
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
	    
	    FilterResponse doctypeFilter = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);

	    for (String id : gbm.getGbTransfers()) {
	    	
			module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
			module.clearSaKeys();
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Grantor Name GoBack Search" );
			module.forceValue(10, countyNo);
			module.forceValue(13, "N");	
			module.setIndexInGB(id);
	        module.setTypeSearchGB("grantor");
		    module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		    
			String dateMMdd = gbm.getDateForSearch(id, "MM/dd", searchId);
			String dateYYYY = gbm.getDateForSearch(id, "yyyy", searchId);
			if (dateMMdd != null) {
				module.getFunction(5).forceValue(dateMMdd);
			}
			if (dateYYYY != null) {
				module.getFunction(6).forceValue(dateYYYY);
			}
			module.setSaKey(7, SearchAttributes.CURRENTDATE_MMDD);
			module.setSaKey(8, SearchAttributes.CURRENTDATE_YYYY);
			
			FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
			((GenericNameFilter)nameFilter).setUseSynonymsForCandidates(true);
			((GenericNameFilter)nameFilter).setUseSynonymsBothWays(true);
			((GenericNameFilter)nameFilter).setIgnoreMiddleOnEmpty(true);
			nameFilter.setInitAgain(true);
			
			FilterResponse transferNameFilter = NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module);
			((GenericNameFilter)transferNameFilter).setUseSynonymsForCandidates(true);
			((GenericNameFilter)transferNameFilter).setUseSynonymsBothWays(true);
			((GenericNameFilter)transferNameFilter).setIgnoreMiddleOnEmpty(true);
			transferNameFilter.setInitAgain(true);
			
		    module.addFilter( nameFilter);
		    module.addFilter( transferNameFilter );
		    module.addFilter( doctypeFilter );
		    module.addFilter( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module) );

		    nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			nameIterator.setAllowMcnPersons( true );
		    module.addIterator( nameIterator );
		    
		    moduleList.add(module);
		     
		    if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
		    	module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		    	module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Grantee Name (Broken Chain) GoBack Search" );
		    	module.forceValue(10, countyNo);
		    	module.forceValue(13, "N");	
			    module.setIndexInGB(id);
			    module.setTypeSearchGB("grantee");
			    module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				dateMMdd = gbm.getDateForSearchBrokenChain(id, "MM/dd", searchId);
				dateYYYY = gbm.getDateForSearchBrokenChain(id, "yyyy", searchId);
				if (dateMMdd != null) {
					module.getFunction(5).forceValue(dateMMdd);
				}
				if (dateYYYY != null) {
					module.getFunction(6).forceValue(dateYYYY);
				}
				module.setSaKey(7, SearchAttributes.CURRENTDATE_MMDD);
				module.setSaKey(8, SearchAttributes.CURRENTDATE_YYYY);
				
				nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
				((GenericNameFilter)nameFilter).setUseSynonymsForCandidates(true);
				((GenericNameFilter)nameFilter).setUseSynonymsBothWays(true);
				((GenericNameFilter)nameFilter).setIgnoreMiddleOnEmpty(true);
				nameFilter.setInitAgain(true);
				
				transferNameFilter = NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module);
				((GenericNameFilter)transferNameFilter).setUseSynonymsForCandidates(true);
				((GenericNameFilter)transferNameFilter).setUseSynonymsBothWays(true);
				((GenericNameFilter)transferNameFilter).setIgnoreMiddleOnEmpty(true);
				transferNameFilter.setInitAgain(true);
				
				module.addFilter( nameFilter );
				module.addFilter( transferNameFilter );
				module.addFilter( doctypeFilter );
			    module.addFilter( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module) );
			    
			    nameIterator =  (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			    nameIterator.setAllowMcnPersons( true );
			    module.addIterator(nameIterator);
				moduleList.add(module);
		    }
	    }	 
	    serverInfo.setModulesForGoBackOneLevelSearch(moduleList);
	}
    
    
    
    protected static Set<InstrumentI> getAllAoAndTaxReferences(DocumentsManagerI manager){
		Set<InstrumentI> allAoRef = new HashSet<InstrumentI>();
		try{
			manager.getAccess();
			List<DocumentI> list = manager.getDocumentsWithType( true, DType.ASSESOR, DType.TAX );
			for(DocumentI assessor:list){
				for(RegisterDocumentI reg : assessor.getReferences()){
					allAoRef.add(reg.getInstrument());
				}
				allAoRef.addAll(assessor.getParsedReferences());
			}
		}
		finally {
			manager.releaseAccess();
		}
		return allAoRef;
	}
    
    protected ArrayList<NameI>  addNameSearch( List<TSServerInfoModule> modules, TSServerInfo serverInfo,
    		String countyNo, String key, String propertySearchType, ArrayList<NameI> searchedNames, FilterResponse[] filters, FilterResponse []filtersCrossRef ) {
		ConfigurableNameIterator nameIterator = null;
		
		TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		
		String moduleDescription = null;
		
		if(key.equals(SearchAttributes.OWNER_OBJECT)) {
			if("P".equals(propertySearchType)) {
				module.setSaKey(5, SearchAttributes.FROMDATE_MMDD);
				module.setSaKey(6, SearchAttributes.FROMDATE_YEAR);
				if(searchedNames == null) {
					moduleDescription = "Name Search - Searching by Owner Names from Search page for Real Property Records";
				} else {
					moduleDescription = "Name Search - Searching by Owner Names from OCR for Real Property Records";
				}
				module.setSearchType("PI");
			} else if("N".equals(propertySearchType)) {
				moduleDescription = "Name Search - Searching by Owner Names for General Name Index";
				module.setSaKey(5, SearchAttributes.LIEN_DATE_MMDD);
				module.setSaKey(6, SearchAttributes.LIEN_DATE_YYYY);
				module.setSearchType("GI");
			}
		} else if(key.equals(SearchAttributes.BUYER_OBJECT)) {
			if("N".equals(propertySearchType)) {
				moduleDescription = "Name Search - Searching by Buyer Names for General Name Index";
				module.setSaKey(5, SearchAttributes.LIEN_DATE_MMDD);
				module.setSaKey(6, SearchAttributes.LIEN_DATE_YYYY);
				module.setSearchType("GI");
			}
		}
		module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		if(moduleDescription != null) {
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, moduleDescription);
		}
		
		module.forceValue(10, countyNo);
		module.setSaObjKey(key);
		
		module.forceValue(13, propertySearchType);	
		
		for (int i = 0; i < filters.length; i++) {
			if(filters[i]!=null){
				module.addFilter(filters[i]);
				module.addCrossRefValidator(filters[i].getValidator());
			}
		}
		addBetweenDateTest(module, true, true, true);
		//addFilterForUpdate(module, true);
		for (FilterResponse filterResponse : filtersCrossRef) {
			if(filterResponse != null) {
				module.addCrossRefValidator(filterResponse.getValidator());
			}
		}
		
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }, 40);
		nameIterator.setAllowMcnPersons( true );
		
		if ( searchedNames!=null ) {
			nameIterator.setInitAgain( true );
			nameIterator.setSearchedNames( searchedNames );
		}
		
		searchedNames = nameIterator.getSearchedNames() ;
		module.addIterator( nameIterator );
		modules.add( module );
		return searchedNames;
	}
    
    protected void addOCRSearch(List<TSServerInfoModule> modules,TSServerInfo serverInfo, String countyNo, FilterResponse ...filters){
		// OCR last transfer - book / page search
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
	    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    
        //module.addIterator(new OcrOrBootStraperWithGoodNamesIterator( searchId)); 
        
	    module.forceValue(8, countyNo);
	    module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH );
	    module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH );
	    for(int i=0;i<filters.length;i++){
	    	module.addFilter(filters[i]);
	    }
	    addBetweenDateTest(module, false, false, false);
		modules.add(module);
		
	    // OCR last transfer - instrument search
	    module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
	    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    
	    //module.addIterator(new OcrOrBootStraperWithGoodNamesIterator( searchId)); 
	    
	    module.forceValue(8, countyNo);
	    module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	    module.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
	    for(int i=0;i<filters.length;i++){
	    	module.addFilter(filters[i]);
	    }
	    addBetweenDateTest(module, false, false, false);
		modules.add(module);
	}
    public String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}
    
    public String getPrettyFollowedLink (String initialFollowedLnk)
    {	
    	return "<br/><span class='followed'>" + preProcessLink(initialFollowedLnk) + "</span><br/>";
    }

    @Override
	protected void setCertificationDate() {
		
		try {
			String countyId = getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY);

			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
			
		        String html = "";
	    		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
	    		try{
	    			HTTPRequest request = new HTTPRequest("");
	    			request.setPostParameter("countyNo", siteOptionMap.get(miServerID));
	    			request.setPostParameter("destinationPage", "30");
	    			
	    			html = site.process(request).getResponseAsString();
	    			
	    		} catch(RuntimeException e){
	    			e.printStackTrace();
	    		} finally {
	    			HttpManager.releaseSite(site);
	    		}   
	    		html = html.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");
	            Matcher certDateMatcher = CERTIFICATION_DATE_PATTERN.matcher(html);
	            if(certDateMatcher.find()) {
	            	String date = certDateMatcher.group(2).trim();
	            	
	            	date = CertificationDateManager.sdfOut.format(CERTIFICATION_DATE_FORMAT.parse(date));

	            	CertificationDateManager.cacheCertificationDate(dataSite, date);
		        	getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
		        	
	            	logger.debug("Set Certification Date for SearchId: "  + searchId + " to " + date + " for county " + countyId);
	            } else {
	            	logger.error("Could NOT set Certification Date for SearchId: "  + searchId + " for county " + countyId);
	            }
            }
            
        } catch (Exception e) {
            logger.error("Exception while setting certification date", e);        }
	}
    
    @Override
    protected DownloadImageResult saveImage(ImageI image)
    		throws ServerResponseException {
    	
    	if(image != null) {
    		String allParams = image.getLink(0);
    		String realInstrument = allParams.replaceAll(".*&realInstrument=([^&]+).*", "$1");
    		String realYear = allParams.replaceAll(".*&realYear=([^&]*)&.*", "$1");
    		
    		try {
				byte[] imageBytes = SkldImages.getInstance().downloadImage(
						SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericSKLD","SKLDMainUserNameImage"), 
						SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericSKLD","SKLDMainPasswordImage"), 
						getSearchAttributes().getCountyName(), 
						realInstrument, 
						realYear);
				
				DownloadImageResult downloadImageResult = new DownloadImageResult(
						DownloadImageResult.Status.OK, imageBytes, image.getContentType());
				
				afterDownloadImage(true);
				
				return downloadImageResult;
				
			} catch (Exception e) {
				logger.error(searchId + " error for " + allParams, e);
			} 
    		
    	}
    	
    	return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
    }
    
    @Override
    protected void addSpecificValidatedLink(ServerResponse response) {
    	if(response != null && response.getParsedResponse() != null ) {
    		DocumentI rawDocumentI = response.getParsedResponse().getDocument();
    		if(rawDocumentI != null && rawDocumentI instanceof RegisterDocumentI) {
    			if(StringUtils.isNotEmpty(rawDocumentI.getInstno())) {
    				addValidatedLink("Instrument " + rawDocumentI.getInstno() + 
						" with doctype " + rawDocumentI.getServerDocType() +
						" was already saved.");
    			} else if(StringUtils.isNotEmpty(rawDocumentI.getBook()) || 
    					StringUtils.isNotEmpty(rawDocumentI.getPage())) {
    				addValidatedLink("Book/Page " + rawDocumentI.getBook() + "/" + rawDocumentI.getPage() +
    						" with doctype " + rawDocumentI.getServerDocType() +
    						" was already saved.");
    			}
    		}
    	}
    }
    
    @Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(9, "5");
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			String instrument = restoreDocumentDataI.getInstrumentNumber();
			if(instrument.contains("-")) {
				instrument = instrument.replaceAll("([^-]*).*", "$1");
			}
			module.forceValue(2, instrument);
			module.forceValue(9, "5");
			
			if(restoreDocumentDataI.getYear() > 0) {
				module.forceValue(10, Integer.toString(restoreDocumentDataI.getYear()));
			}
		} 
		return module;
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		
		String instrumentNo = document.getInstrumentNumber();
		try {
			if(instrumentNo.matches("\\d+-\\d{4}")) {
				String realInstrument = instrumentNo.replaceAll("-\\d+", "");
				String realYear = instrumentNo.replaceAll("\\d+-", "");
				int noOfPages = SkldImages.getInstance().checkForImage(
						SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericSKLD","SKLDMainUserNameImage"), 
						SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericSKLD","SKLDMainPasswordImage"), 
						getSearchAttributes().getCountyName(), 
						realInstrument, 
						realYear);
				if(noOfPages > 0) {
					
					TSServerInfoModule imgModule = getDefaultServerInfo().getModule(TSServerInfo.IMG_MODULE_IDX);
					
					
					
					
					imgModule.forceValue(0, realInstrument);
					imgModule.forceValue(1, realYear);
					imgModule.forceValue(2, instrumentNo + ".tiff");
					
					return imgModule;
					
				}
			}
		} catch (Exception e) {
			logger.error("Error while checking image for " + instrumentNo, e);
		}
		
		return null;
		
	}
	
	@Override
	protected ServerResponse performRequest(String page, int methodType, String action, 
			int parserId, String imagePath, String vbRequest, Map<String, Object> extraParams)
			throws ServerResponseException {
		if(page.matches("/FK____\\d+")) {
			ServerResponse response = new ServerResponse();
			response.setError(ServerResponse.NOT_VALID_DOC_ERROR);
			return response;
		} else {
			return super.performRequest(page, methodType, action, parserId, imagePath,
					vbRequest, extraParams);
		}
	}
		
	public String generateSpecificInstrument(RegisterDocumentI documentI) {
		return generateSpecificInstrument(documentI, getDataSite().getCountyId(), searchId);
	}
	
	public String generateSpecificInstrument(RegisterDocumentI documentI, int countyId, long searchId) {
		
		InstrumentI instrumentI = documentI.getInstrument();
		if(!(instrumentI instanceof SKLDInstrument)) {
			return instrumentI.getInstno();
		}
		
		SKLDInstrument skldInstrument = (SKLDInstrument)instrumentI;
		
		return generateSpecificInstrument(skldInstrument.getOriginalInstrumentNo(), documentI.getRecordedDate(), countyId, searchId);
		
	}
	
	public static String generateSpecificInstrument(String instno, Date recordedDate, int countyId, long searchId) {
		
		String firstPart = null;
		String firstYear = null;
		
		Calendar recordedCalendar = Calendar.getInstance();
		
		if(recordedDate == null) {
			return instno;
		}
		recordedCalendar.setTime(recordedDate);
		int year = recordedCalendar.get(Calendar.YEAR);
		
		if(StringUtils.isEmpty(instno)) {
			return instno;
		} else if(instno.matches("\\d+-\\d{4}")) {
			firstPart = instno.substring(0, instno.indexOf("-"));
			firstYear = instno.substring(instno.length() - 4);
			instno =  firstYear + firstPart;
		} else if(instno != null && instno.matches("\\d+-\\d{2}")) {
			firstPart = instno.substring(0, instno.indexOf("-"));
			firstYear = instno.substring(instno.length() - 2);
			instno =  firstYear + firstPart;
		} else {
			logger.info("Found strange file id: " + instno + " on countyId = " + countyId + " and searchId = " + searchId);
			/*
			firstPart = instno;
			firstYear = Integer.toString(year);
			instno =  firstYear + firstPart;
			*/
			return instno;
		}
		
		
		
		//int month = recordedCalendar.get(Calendar.MONTH);
		//int day = recordedCalendar.get(Calendar.DATE);
		
		if(countyId == CountyConstants.CO_Adams) {	//Adams
			if(year <= 1974) {
				instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");;
			} else if(year <= 1976) {
				instno = "A" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
			} else if(year <= 1990) {
				instno = "B" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
			} else if(year < 1994) {
				instno = "B" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
			} else {
				Calendar tempCalendar = new GregorianCalendar(2004, Calendar.MARCH, 23, 0, 0, 0);
				if(recordedCalendar.before(tempCalendar)) {
					instno = "C" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
				} else {
					tempCalendar.set(Calendar.YEAR, 2006);
					tempCalendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
					tempCalendar.set(Calendar.DAY_OF_MONTH, 30);
					if(recordedCalendar.before(tempCalendar)) {
						instno = FormatDate.getDateFormat(FormatDate.PATTERN_yyyyMMdd).format(recordedDate) + 
							org.apache.commons.lang.StringUtils.leftPad(firstPart, 9, "0");
					} else {
						instno = year + org.apache.commons.lang.StringUtils.leftPad(firstPart, 9, "0");
					}
				}
			}
		} else if(countyId == CountyConstants.CO_Arapahoe) {	//Arapahoe
			if(year < 1991) {
				instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
			} else if(year == 1991) {
				instno = FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(recordedDate) + 
					org.apache.commons.lang.StringUtils.leftPad(firstPart, 8, "0");
			} else if(year < 1994) {
				instno = FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(recordedDate) + 
					org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
			} else if(year == 1994) {
				instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
			} else { 
				Calendar tempCalendar = new GregorianCalendar(1995, Calendar.NOVEMBER, 2, 0, 0, 0);
				if(recordedCalendar.before(tempCalendar)) {
					instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 8, "0");
				} else if(year < 1996){
					instno = "A5" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
				} else if(year == 1996) {
					instno = "A6" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
				} else if(year <= 1999) {
					instno = "A" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
				} else if(year <= 2009){
					instno = "B" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
				} else {
					instno = "D" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
				}
			}
		} else if(countyId == CountyConstants.CO_Boulder) {	//Boulder
			if(year < 1997) {
				instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 8, "0");
			} else {
				Calendar tempCalendar = new GregorianCalendar(2009, Calendar.MAY, 1, 0, 0, 0);
				if(recordedCalendar.before(tempCalendar)) {
					instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
				} else {
					instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 8, "0");
				}
			}
			
		} else if(countyId == CountyConstants.CO_Broomfield) {	//Broomfield
			Calendar tempCalendar = new GregorianCalendar(1982, Calendar.DECEMBER, 16, 0, 0, 0);
			if(recordedCalendar.before(tempCalendar)) {
				instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
			} else {
				tempCalendar.set(Calendar.YEAR, 1994);
				tempCalendar.set(Calendar.MONTH, Calendar.APRIL);
				tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
				if(recordedCalendar.before(tempCalendar)) {
					instno = FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(recordedDate) + 
						org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
				} else if (year < 1995){
					instno = "B" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
				} else {
					tempCalendar.set(Calendar.YEAR, 2001);
					tempCalendar.set(Calendar.MONTH, Calendar.NOVEMBER);
					tempCalendar.set(Calendar.DAY_OF_MONTH, 15);
					if(recordedCalendar.before(tempCalendar)) {
						instno = "C" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
					} else {
						instno = year + org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
					}
				}					
			}		
		} else if(countyId == CountyConstants.CO_Clear_Creek) {	//Clear Creek
			instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
		} else if(countyId == CountyConstants.CO_Denver) {	//Denver
			Calendar tempCalendar = new GregorianCalendar(1988, Calendar.MAY, 1, 0, 0, 0);
			if(recordedCalendar.before(tempCalendar)) {
				instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 8, "0");
			} else {
				tempCalendar.set(Calendar.YEAR, 1993);
				tempCalendar.set(Calendar.MONTH, Calendar.JANUARY);
				tempCalendar.set(Calendar.DAY_OF_MONTH, 9);
				if(recordedCalendar.before(tempCalendar)) {
					instno = "R-" + FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(recordedDate) + "-" +
						org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
				} else {
					tempCalendar.set(Calendar.YEAR, 1994);
					if(recordedCalendar.before(tempCalendar)) {
						instno = FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(recordedDate) + org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
					} else {
						tempCalendar.set(Calendar.YEAR, 2000);
						if(recordedCalendar.before(tempCalendar)) {
							instno = FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(recordedDate) + org.apache.commons.lang.StringUtils.leftPad(firstPart, 8, "0");
						} else {
							instno = year + org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
						}
					}
				}					
			}
		} else if(countyId == CountyConstants.CO_Douglas) {	//Douglas
			Calendar tempCalendar = new GregorianCalendar(1986, Calendar.MARCH, 1, 0, 0, 0);
			if(recordedCalendar.before(tempCalendar)) {
				instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
			} else if(year <= 2001){
				String yearAsString = FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(recordedDate);
				if(firstPart.length() > 5 && firstPart.startsWith(yearAsString)) {
					instno = firstPart;
				} else {
					instno = FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(recordedDate) + org.apache.commons.lang.StringUtils.leftPad(firstPart, 5, "0");
				}
			} else {
				instno = year + org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
			}
		} else if(countyId == CountyConstants.CO_Eagle) {	//Eagle
			if(year < 2006) {
				instno = firstPart;
			} else {
				instno = firstYear + org.apache.commons.lang.StringUtils.leftPad(firstPart, 5, "0");;
			}
		} else if(countyId == CountyConstants.CO_El_Paso) {	//El Paso
			if(year <= 1991) {
				instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 8, "0");;
			} else if(year <= 1993) {
				instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 9, "0");;
			}
			
			Calendar tempCalendar = new GregorianCalendar(1996, Calendar.JULY, 1, 0, 0, 0);
			if(recordedCalendar.before(tempCalendar)) {
				instno = org.apache.commons.lang.StringUtils.leftPad(firstPart, 5, "0");
			} else if(year <= 1999){
				instno = 0 + FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(recordedDate) + org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
			} else {
				instno = 2 + FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(recordedDate) + org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
			}
		} else if(countyId == CountyConstants.CO_Jefferson) {	//Jefferson
			if(year < 1978) {
				instno = firstPart;
			} else if(year < 1995) {
				instno = FormatDate.getDateFormat(FormatDate.PATTERN_yy).format(recordedDate) + 
					org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");
			} else {
				Calendar tempCalendar = new GregorianCalendar(2006, Calendar.APRIL, 1, 0, 0, 0);
				if(recordedCalendar.before(tempCalendar)) {
					instno = "F" + org.apache.commons.lang.StringUtils.leftPad(firstPart, 7, "0");
				} else {
					instno = FormatDate.getDateFormat(FormatDate.PATTERN_yyyy).format(recordedDate) + 
						org.apache.commons.lang.StringUtils.leftPad(firstPart, 6, "0");				
				}
			}
		}
		
		return instno;
		
		
	}
	
	public List<TSServerInfoModule> getMultipleModules(TSServerInfoModule module,Object sd) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		final HttpServletRequest originalRequest = ((SearchDataWrapper) sd).getRequest();
		
		if(sd instanceof SearchDataWrapper) {
			
			int cnt = 0;
			
			try {
				cnt = Integer.parseInt(originalRequest.getParameter(RequestParams.PARENT_SITE_ADDITIONAL_CNT+module.getMsName()));
			}catch(Exception e) {}
			
			if(cnt == 0 ) {
				return modules;
			}
			
			for(int i = 0; i<= cnt; i++ ) {
			
				final TSServerInfoModule mod = (TSServerInfoModule)module.clone();
				final int index = i;	
				HttpServletRequest req = new HttpServletRequestWrapper(originalRequest) {
					@Override
					public String getParameter(String name) {
						if(originalRequest.getParameter(name+"_"+index)==null) {
							if (index != 0)
								return "";
							return originalRequest.getParameter(name);
						}else {
							return originalRequest.getParameter(name+"_"+index);
						}
					}
				};
	
				mod.setData(new SearchDataWrapper(req));
				modules.add(mod);
			}
		}
		
		return modules;
	}
	
	protected int[] getModuleIdsForSavingLegal() {
		return new int[]{TSServerInfo.SUBDIVISION_MODULE_IDX, TSServerInfo.SECTION_LAND_MODULE_IDX};
	}
	
	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 1) {
			String usedFirstName = module.getFunction(1).getParamValue();
			String usedMiddleName = module.getFunction(2).getParamValue();
			String usedLastName = module.getFunction(0).getParamValue();
			
			if(StringUtils.isEmpty(usedLastName)) {
				return null;
			}
			
			/**
			 * There are cases where we have to enter last + first in the last field
			 */
			if(usedLastName.contains(" ") && StringUtils.isEmpty(usedFirstName)) {
				String[] names = null;
				if(NameUtils.isCompany(usedLastName)) {
					names = new String[]{"", "", usedLastName, "", "", ""};
				} else {
					names = StringFormats.parseNameNashville(usedLastName, true);
				}
				
				names = StringFormats.parseNameNashville(usedLastName, true);
				name.setLastName(names[2]);
				name.setFirstName(names[0]);
				name.setMiddleName(names[1]);
			} else {
				name.setLastName(usedLastName);
				name.setFirstName(usedFirstName);
				name.setMiddleName(usedMiddleName);
			}
			return name;
		}
		return null;
	}
	
	@Override
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		
		LegalI legal = null;
		
		if(module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX && module.getFunctionCount() > 12) {
			SubdivisionI subdivision = new Subdivision();
			
			subdivision.setName(module.getFunction(0).getParamValue().trim());
			
			subdivision.setLot(module.getFunction(1).getParamValue().trim());
			subdivision.setLotThrough(module.getFunction(2).getParamValue().trim());
			
			subdivision.setBlock(module.getFunction(3).getParamValue().trim());
			subdivision.setBlockThrough(module.getFunction(4).getParamValue().trim());
			
			subdivision.setPlatBook(module.getFunction(12).getParamValue().trim());
			subdivision.setPlatPage(module.getFunction(13).getParamValue().trim());
			
			legal = new Legal();
			legal.setSubdivision(subdivision);
			
		} else if(module.getModuleIdx() == TSServerInfo.SECTION_LAND_MODULE_IDX && module.getFunctionCount() > 1) {
			TownShipI townShip = new TownShip();
			
			townShip.setArb(module.getFunction(0).getParamValue().trim());
			
			townShip.setSection(module.getFunction(1).getParamValue().trim());
			townShip.setTownship(module.getFunction(2).getParamValue().trim());
			
			townShip.setRange(module.getFunction(3).getParamValue().trim());
			townShip.setAddition(module.getFunction(13).getParamValue().trim());
			
			legal = new Legal();
			legal.setTownShip(townShip);
		}
		
		return legal;
	}
	
	@Override
	public AddressI getAddressFromModule(TSServerInfoModule module) {
		AddressI addressI = null;
		
		if(module.getModuleIdx() == TSServerInfo.ADDRESS_MODULE_IDX && module.getFunctionCount() > 3) {
			addressI = new Address();
			addressI.setNumber(module.getFunction(0).getParamValue().trim());
			addressI.setStreetName(module.getFunction(1).getParamValue().trim());
		}
		
		return addressI;
	}
	
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo,
			DocumentI documentToCheck, HashMap<String, String> data) {
		if(documentToCheck == null) {
			return false;
		}
		DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		InstrumentI instToCheck = documentToCheck.getInstrument();
    		for(DocumentI e: documentManager.getDocumentsWithDataSource(false, "SK")){
    			InstrumentI savedInst = e.getInstrument();
    			if( savedInst.getInstno().equals(instToCheck.getInstno())  
    					&& (savedInst.getBook().equals(instToCheck.getBook()) && savedInst.getPage().equals(instToCheck.getPage()))  
    					&& savedInst.getDocno().equals(instToCheck.getDocno())
    					&& e.getServerDocType().equals(documentToCheck.getServerDocType())
    					&& savedInst.getYear() == instToCheck.getYear()
    			){
    				return true;
    			}
    		}
    	} finally {
    		documentManager.releaseAccess();
    	}
		return false;
	}
    
	@Override
	public String getSaveSearchParametersButton(ServerResponse response) {
		if(response == null 
				|| response.getParsedResponse() == null) {
			return null;
		}
		
		Object possibleModule = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		
		if(!(possibleModule instanceof TSServerInfoModule)) {
			return null;
		}
		
		Search search = getSearch();
		int moduleIdx = ((TSServerInfoModule)possibleModule).getModuleIdx();
		
		if(moduleIdx == TSServerInfo.SUBDIVISION_MODULE_IDX || 
				moduleIdx == TSServerInfo.ADDRESS_MODULE_IDX || 
				moduleIdx == TSServerInfo.NAME_MODULE_IDX) {
		
			String key = "SSP_" + System.currentTimeMillis();
			
			/**
			 * Store this for future use (do not worry, it will not be saved)
			 */
			search.setAdditionalInfo(key, possibleModule);
			return "<input type=\"button\" name=\"ButtonSSP\" value=\"Save Search Parameters\" onClick=\"saveSearchedParametersAJAX('" + 
					key + "','" + getServerID() + "')\" class=\"button\" title=\"Save Last Searched Parameters\">";
		} else {
			return null;
		}
	}
	
	@Override
	public void performAdditionalProcessingAfterRunningAutomatic() {
		super.performAdditionalProcessingAfterRunningAutomatic();
		
		Search search = getSearch();
		@SuppressWarnings("unchecked")
		Set<Integer> additionalInfo = (Set<Integer>) search.getAdditionalInfo(AdditionalInfoKeys.PERFORMED_WITH_NO_ERROR_MODULE_ID_SET);
		if(additionalInfo != null) {
			
			boolean legalPerformed = false;
			int[] moduleIds = new int[]{TSServerInfo.SUBDIVISION_MODULE_IDX, TSServerInfo.SECTION_LAND_MODULE_IDX};
			for (int moduleId : moduleIds) {
				if(additionalInfo.contains(moduleId)) {
					legalPerformed = true;
					break;
				}
			}
			if(!legalPerformed) {
				WarningInfo warning = new WarningInfo(Warning.WARNING_NO_LEGAL_SEARCH_WAS_PERFORMED_ID); 
				getSearch().getSearchFlags().addWarning(warning);
			}
			
		} else {
			//no flags, nothing happened
			WarningInfo warning = new WarningInfo(Warning.WARNING_NO_LEGAL_SEARCH_WAS_PERFORMED_ID); 
			getSearch().getSearchFlags().addWarning(warning);
		}
		
		addSearchSummaryToResultSheet(false);
		
	}
	
	public String addSearchSummaryToResultSheet(boolean isParentSite){
		
		try {
			ServerResponse serverResponse = new ServerResponse();
			TSServerInfoModule module = this.getDefaultServerInfo().getModuleForSearch(TSServerInfo.GENERIC_MODULE_IDX, new SearchDataWrapper());
			module.forceValue(0, getSearchAttribute(SearchAttributes.ABSTRACTOR_FILENO));
			module.setVisible(false);
			
			TSInterface genericSK = TSServersFactory.GetServerInstance(miServerID, searchId);
			genericSK.setDoNotLogSearch(true);
			serverResponse = genericSK.SearchBy(module, new SearchDataWrapper());
			if (serverResponse != null && org.apache.commons.lang.StringUtils.isNotBlank(serverResponse.getResult())) {
				String result = serverResponse.getResult();
				
				result = result.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");
				result = result.replaceAll("(?ms)", "");
//				System.out.println(result);
				Matcher mat = SEARCH_SUMMARY_PATTERN.matcher(result);
				if (mat.find()){
					result = mat.group(1);
//			    	result = result.replaceFirst("(?is)[^\\{]+", "");
			    	result = result.replaceAll("(?is)\\{+", "     ");
				} else{
					result = "";
				}
				
				if (isParentSite){
					return result;
				} else{
					DocumentsManagerI manager = mSearch.getDocManager();
			        
			        try{
			        	manager.getAccess();
			        	Instrument instr = new Instrument("ResultSheet");
			        	instr.setDocType(DocumentTypes.OTHERFILES);
			        	instr.setDocSubType(DocumentTypes.OTHER_FILE_RESULTSHEET);
			        	
			        	DocumentI document = manager.createResultSheetEmptyDocument(DType.ROLIKE, DocumentTypes.OTHERFILES, DocumentTypes.OTHER_FILE_RESULTSHEET, 
			        											instr, "SK", false, getSearch(), RESULTSHEET_PATH, templateVersionVerified);
			        	String resultSheetContent = DBManager.getDocumentIndex(document.getIndexId());

			        	org.htmlparser.Parser resultSheetParser = org.htmlparser.Parser.createParser(resultSheetContent, null);
						NodeList nodes = resultSheetParser.parse(null);

						if (nodes != null){
							NodeList tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);
							
							if (getSearch().getSearchType() != Search.PARENT_SITE_SEARCH){
			    				if (tableList.extractAllNodesThatMatch(new HasAttributeFilter("id", "searchSummary")).size() > 0){
			    					String searchSummary = addSearchSummaryToResultSheet(true);
			    					if (org.apache.commons.lang.StringUtils.isNotBlank(searchSummary)){
				    					Node node = tableList.extractAllNodesThatMatch(new HasAttributeFilter("id", "searchSummary")).elementAt(0);
				    	        		if (node instanceof TableTag){
				    	        			TableTag tableNode = (TableTag) node;
				    	        			if (tableNode.getRowCount() > 0){
				    	        				while (tableNode.getRowCount() > 0){
				    	        					tableNode.removeChild(0);
				    	        				}
//				    	        				tableNode = new TableTag();
//				    	        				tableNode.setAttribute("id", "searchSummary");
				    	        			}
				    	        			
				    	        			TableRow tr = new TableRow();
				    	        			TableColumn tcResp = new TableColumn();
				    	        			tcResp.setChildren(new NodeList(new TextNode(searchSummary.replaceAll(" ", "&nbsp;").replaceAll("[\\r\\n]+", "<br>") + "<br><br>")));
				    	        			
				    	        			if (tr.getChildren() == null){
				    	        				tr.setChildren(new NodeList(tcResp));
				    	        			} else{
				    	        				tr.getChildren().add(tcResp);
				    	        			}
				    	        			if (tableNode.getChildren() == null){
				    	        				tableNode.setChildren(new NodeList(tcResp));
				    	        			} else{
				    	        				tableNode.getChildren().add(tr);
				    	        			}
				    	        		}
			    					}
			    	        	}
			    			}
							DBManager.updateDocumentIndex(document.getIndexId(), nodes.toHtml(), getSearch());
						}
			        } catch(Exception e){  
			        	e.printStackTrace(); 
			        } finally{
			        	manager.releaseAccess();
			        }
				}
			}
			
		} catch (ServerResponseException e){
			e.printStackTrace();
			return "";
		}
		return "";
	}
	
	
	@Override
    public void countOrder() {
		getSearch().countOrder(getSearch().getSa().getSearchIdSKLD(), 
				getDataSite().getCityCheckedInt());
	}
}

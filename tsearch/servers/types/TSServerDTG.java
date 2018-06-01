package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.getAllNodes;
import static ro.cst.tsearch.datatrace.Utils.parseTitleRec;
import static ro.cst.tsearch.datatrace.Utils.parseXMLDocument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.database.rowmapper.RequestCount;
import ro.cst.tsearch.datatrace.DTRecord;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.ExactDateFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.data.LegalStructDTG;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.ServletServerComm;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.CertificationDateDS.CDType;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.PersonalDataStruct;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult.Status;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.ImageI.IType;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.warning.MissingLandSearchWarning;
import com.stewart.ats.base.warning.Warning;
import com.stewart.ats.connection.clarkimages.NvClarkAOMConn.MapType;
import com.stewart.ats.connection.datatrace.ArbRequestParameters;
import com.stewart.ats.connection.datatrace.DataTraceConn;
import com.stewart.ats.connection.datatrace.DataTraceConnI.NameCategoriesTypes;
import com.stewart.ats.connection.datatrace.GeneralNameParameters;
import com.stewart.ats.connection.datatrace.SectionalRequestParameters;
import com.stewart.ats.connection.datatrace.SubdividedRequestParameters;
import com.stewart.ats.connection.datatrace.xsd.arb.ARBIDENTIFIERType;
import com.stewart.ats.connection.datatrace.xsd.arb.ARBSEARCHPARAMETERSType;
import com.stewart.ats.connection.datatrace.xsd.sectional.SECTIONALSEARCHPARAMETERSType;
import com.stewart.ats.connection.datatrace.xsd.subdivided.LOTIDENTIFIERType;
import com.stewart.ats.connection.datatrace.xsd.subdivided.PLATIDENTIFIERType;
import com.stewart.ats.connection.datatrace.xsd.subdivided.SUBDIVIDEDIDENTIFIERType;
import com.stewart.ats.connection.datatrace.xsd.subdivided.SUBDIVIDEDSEARCHPARAMETERSType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.datatree.DataTreeAccount;
import com.stewart.datatree.DataTreeConn;
import com.stewart.datatree.DataTreeImageException;
import com.stewart.datatree.DataTreeManager;
import com.stewart.datatree.DataTreeStruct;
import com.stewart.datatree.SimpleImage;

/**
 * @author cristian stochina
 */
public  class TSServerDTG extends TSServerROLike implements DTLikeAutomatic{
	
	transient protected List<DataTreeStruct> datTreeList;
	protected static final String DTG_FAKE_RESPONSE = ro.cst.tsearch.utils.StringUtils.fileReadToString(TSServerDASL.FAKE_FOLDER + "FL_DTG_FakeResponse.xml");
	protected static final Pattern FAILURE_PAT = Pattern.compile("(?is)<commentary>\\s*<comment>\\s*.*?<text>([^>]+)</text>");
	protected static final Pattern replaceCutTagsWithLabelValue = Pattern.compile("(?is)<(cut\\d+) ([^>]*label=\\\"([^\\\"]+)\\\"[^>]*>[^<]*)</\\1>");
	
	public static final String PLAT_DOCTYPE 			= "PLAT";
	public static final String PLAT_MAP_SUBDOCTYPE 		= "PLAT_MAP";
	public static final String ASSESSOR_MAP_SUBDOCTYPE 	= "ASSESSOR_MAP";
	
	public static final String ARB_TYPE 		= "arb";
	public static final String SECTIONAL_TYPE 	= "sectional";
	public static final String SUBDIVIDED_TYPE 	= "subdivided";
	
	public static final String BASE_SEARCH_DONE 			= "BASE_SEARCH_DONE";
	public static final String BASE_ARB_SEARCH_DONE 		= "BASE_ARB_SEARCH_DONE";
	public static final String BASE_SUBDIVIDED_SEARCH_DONE 	= "BASE_SUBDIVIDED_SEARCH_DONE";
	
	private static final long serialVersionUID = -324752662773381320L;
	
	transient private DataTraceConn conn;
	
	private boolean usePlatInstrumentInsteadOfPlat = false;
	
	protected boolean lookupWithInstrument = false;
	
	public TSServerDTG(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) throws FileNotFoundException, IOException {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
    	
        int commId = InstanceManager.getManager().getCurrentInstance(searchId).getCommunityId();
		DataSite site = HashCountyToIndex.getDateSiteForMIServerID(commId, mid);
		
        try {
			conn = new DataTraceConn(site,searchId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		resultType = MULTIPLE_RESULT_TYPE;
    }
    
    public TSServerDTG(long searchId){
    	super(searchId);
    }
    
    public class DTGDateObject {
    	private String date = "";
    	
    	private boolean isDateValid = false;
    	
    	public DTGDateObject(){}

		/**
		 * @return the date
		 */
		public String getDate() {
			return date;
		}

		/**
		 * @param date the date to set
		 */
		public void setDate(String date) {
			this.date = date;
		}

		/**
		 * @return the isDateValid
		 */
		public boolean isDateValid() {
			return isDateValid;
		}

		/**
		 * @param isDateValid the isDateValid to set
		 */
		public void setDateValid(boolean isDateValid) {
			this.isDateValid = isDateValid;
		}
    }
    
    @Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();

		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();

		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.PARCEL_ID_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if(tsServerInfoModule != null){
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.INSTR_NO_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);
		if (tsServerInfoModule != null) {
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.SUBDIVISION_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SECTION_LAND_MODULE_IDX);
		if (tsServerInfoModule != null) {
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.SECTION_LAND_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX41);
		if(tsServerInfoModule != null){
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.MODULE_IDX41, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX42);
		if (tsServerInfoModule != null){
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.MODULE_IDX42, nameToIndex.get(functionName));
					if (StringUtils.isNotEmpty(comment)){
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ARB_MODULE_IDX);
		if (tsServerInfoModule != null) {
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.ARB_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		return msiServerInfoDefault;
	}
    
    public boolean alreadySavedDocs(String book, String page, String instrNo){
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	
    	boolean checkByBookPage = false;
    	boolean checkByInstrNo = false;
    	if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)){
    		checkByBookPage = true;
    	} else if (StringUtils.isNotEmpty(instrNo)){
    		checkByInstrNo = true;
    	}
    	try{
    		documentManager.getAccess();
			List<DocumentI> allRODocuments = documentManager .getDocumentsWithDataSource(false, getDataSite().getSiteTypeAbrev());
			
			for (DocumentI documentI : allRODocuments) {
				if (checkByBookPage){
					if (org.apache.commons.lang.StringUtils.isNotEmpty(documentI.getBook()) 
								&& documentI.getBook().equals(book)
						   && org.apache.commons.lang.StringUtils.isNotEmpty(documentI.getPage()) 
						   		&& documentI.getPage().equals(page)) {
							
						return true;
					} 
				} else if (checkByInstrNo){
							if (org.apache.commons.lang.StringUtils.isNotEmpty(documentI.getInstno()) 
									&& documentI.getInstno().equals(instrNo)){
								return true;
							}
				}
			}
    	}
		finally {
			documentManager.releaseAccess();
		}
    	
    	return false;
    }
    
    @Override
	protected ServerResponse performRequest(String page, int methodType, String action, 
			int parserId, String imagePath, String vbRequest, Map<String, Object> extraParams) throws ServerResponseException {
		if (page.contains("/DT___")) {
			ServerResponse response = new ServerResponse();
			response.setError(ServerResponse.NOT_VALID_DOC_ERROR);
			return response;
		} else {
			return super.performRequest(page, methodType, action, parserId, imagePath, vbRequest, extraParams);
		}
	}
    
    @Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
    	Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    		
		if(isParentSite()){
			mSearch.setAdditionalInfo("RESAVE_DOCUMENT",Boolean.FALSE);
		}
		
		if (Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF))
				&& Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CHECK_ALREADY_SAVED))){
			
			if (module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX){
				String book = StringUtils.defaultString(module.getParamValue(0)).replaceAll("^0+", "");
				String page = StringUtils.defaultString(module.getParamValue(1)).replaceAll("^0+", "");
				
				if (alreadySavedDocs(book, page, null)){
					return new ServerResponse();
				}
			} 
			if (module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX){
				String instrNo = StringUtils.defaultString(module.getParamValue(0)).replaceAll("^0+", "");

				if (alreadySavedDocs(null, null, instrNo)){
					return new ServerResponse();
				}
			}
		}
		switch (module.getModuleIdx()) {
			case TSServerInfo.IMG_MODULE_IDX:
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
				return imageSearch(module);
				
			case TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX:					
				mSearch.setAdditionalInfo("RESAVE_DOCUMENT",Boolean.FALSE);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
				return findImageAndCreateFakeDocument(module, sd, global);
			
			case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX: 
			{
				String book = StringUtils.defaultString(module.getParamValue(0)).replaceAll("^0+", "");
				String page = StringUtils.defaultString(module.getParamValue(1)).replaceAll("^0+", "");
				module.forceValue(0, book);
				module.forceValue(1, page);
				break;
			}
			
			case TSServerInfo.INSTR_NO_MODULE_IDX:{
				String instr = StringUtils.defaultString(module.getParamValue(0));
				module.forceValue(0, instr);
				break;
			}
			
			case TSServerInfo.FAKE_MODULE_IDX:
			{
				ServerResponse sr = new ServerResponse();
	    		RestoreDocumentDataI restoreDocumentDataI = (RestoreDocumentDataI)module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE);
				if(restoreDocumentDataI != null) {
					Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
					
					RegisterDocumentI docR = restoreDocumentDataI.toRegisterDocument(getSearch(), getDataSite());
					
					LinkInPage linkInPage = new LinkInPage(
							getLinkPrefix(TSConnectionURL.idPOST) + "DT___" + docR.getId(), 
							getLinkPrefix(TSConnectionURL.idPOST) + "DT___" + docR.getId(), 
	    					TSServer.REQUEST_SAVE_TO_TSD);
					
					ParsedResponse pr = new ParsedResponse();
					pr.setDocument(docR);
					
					
					String asHtml = docR.asHtml(); 
					pr.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + asHtml + "</td></tr>");
					pr.setOnlyResponse((String)pr.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE));
					pr.setSearchId(searchId);
					pr.setUseDocumentForSearchLogRow(true);
					pr.setPageLink(linkInPage);
					getSearch().addInMemoryDoc(linkInPage.getLink(), pr);
					parsedRows.add(pr);
					sr.getParsedResponse().setResultRows(parsedRows );
			        sr.setResult("");
				}
	    		return sr;
			}
		}
		
		if( !isParentSite() 
				&& (dontMakeTheSearch(module, searchId, true) 
						|| "true".equalsIgnoreCase(global.getSa().getAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND)))
			){
			return new ServerResponse();
		}
		
		ServerResponse serverResponse = searchBy(module, sd, null);
		
		if (serverResponse != null && serverResponse.getParsedResponse() != null) {
			serverResponse.getParsedResponse().setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, module);
		}
		
		return serverResponse;
	}
    
    public boolean dontMakeTheSearch(TSServerInfoModule module,	long searchId, boolean ignoreYear) {
		
    	Search search 		= getSearch();
		
		boolean isFloridaDTGSearch = getDataSite().getStateFIPS().equals(StateContants.FL_STRING_FIPS);
		boolean isCO = getDataSite().getStateFIPS().equals(StateContants.CO_STRING_FIPS);
		
		switch (module.getModuleIdx()){
			
			case TSServerInfo.SUBDIVISION_MODULE_IDX:					
					boolean test = doNotMakeSearch(module, SUBDIVIDED_TYPE, false);
					if (!test){
						search.setAdditionalInfo(BASE_SEARCH_DONE, "true");
						search.setAdditionalInfo(BASE_SUBDIVIDED_SEARCH_DONE, "true");
					}
					return test;
				case TSServerInfo.SECTION_LAND_MODULE_IDX:
					if (isFloridaDTGSearch || isCO){
						test = doNotMakeSearch(module, SECTIONAL_TYPE, true);
					} else{
						test = doNotMakeSearch(module, SECTIONAL_TYPE, false);
					}
					if (!test){
						if (search.getAdditionalInfo(BASE_ARB_SEARCH_DONE) != null 
								|| (search.getAdditionalInfo(BASE_SUBDIVIDED_SEARCH_DONE) != null && aoOrTrIsPlated(search))){
							test = true;
						} else{
							search.setAdditionalInfo(BASE_SEARCH_DONE, "true");
						}
					}
					return test;
				case TSServerInfo.ARB_MODULE_IDX:
					test = doNotMakeSearch(module, ARB_TYPE, false);
					if (!test){
						if (search.getAdditionalInfo(BASE_SUBDIVIDED_SEARCH_DONE) != null && aoOrTrIsPlated(search)){
							test = true;
						} else{
							search.setAdditionalInfo(BASE_ARB_SEARCH_DONE, "true");
							search.setAdditionalInfo(BASE_SEARCH_DONE, "true");
						}
					}
					return test;
				case TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX:
					return StringUtils.isEmpty(module.getParamValue(0)) || StringUtils.isEmpty(module.getParamValue(1));
				case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
					return StringUtils.isEmpty(module.getParamValue(0)) || StringUtils.isEmpty(module.getParamValue(1));
				case TSServerInfo.PARCEL_ID_MODULE_IDX:					
					test =  StringUtils.isEmpty(module.getParamValue(0)) || search.getAdditionalInfo(BASE_SEARCH_DONE) != null ;
					if (!test){
						search.setAdditionalInfo(BASE_SEARCH_DONE, "true");
					}
					return test;
				case TSServerInfo.INSTR_NO_MODULE_IDX:
					return StringUtils.isEmpty(module.getParamValue(0)) 
						|| (!ignoreYear && (StringUtils.isEmpty(module.getParamValue(1)) || "-1".equalsIgnoreCase(module.getParamValue(2)))); 
		}		
		return false;
	}
    
    private boolean aoOrTrIsPlated(Search search) {
		DocumentsManagerI m = search.getDocManager();
		try{
			m.getAccess();
			for (DocumentI doc : m.getDocumentsWithType(DType.ASSESOR, DType.TAX)){
				for (PropertyI prop : doc.getProperties()){
					if (prop.hasSubdividedLegal()){
						return true;
					}
				}
			}
		}finally{
			m.releaseAccess();
		}
		return false;
	}
    
    private boolean doNotMakeSearch(TSServerInfoModule module, String what, boolean ignoreQOandQV) {
		
    	boolean isFloridaDTGSearch = getDataSite().getStateFIPS().equals(StateContants.FL_STRING_FIPS);
		boolean isCADTGSearch = getDataSite().getStateFIPS().equals(StateContants.CA_STRING_FIPS);
		boolean isCADTGWithoutPlatPage = false;
		try {
			isCADTGWithoutPlatPage = (isCADTGSearch && getDataSite().getCountyId() == CountyConstants.CA_San_Diego);
		} catch (Exception e) {	}
		
		String lot 				= "";
		String block 			= "";
		String platBook 		= "";
		String platPage 		= "";
		String platInst 		= "";
		String platInstYear 	= "";

		String ncbNumber 	= "";
		String subdivision 	= "";
		String tract 		= "";

		String sec 			= "";
		String tw 			= "";
		String rg 			= "";
		String qo 			= "";
		String qv 			= "";
		String arb 			= "";
		
		if (SUBDIVIDED_TYPE.equals(what)){
			lot  = module.getParamValue(2);
			block  = module.getParamValue(3);
			platBook  = module.getParamValue(4);
			platPage = module.getParamValue(5);
			platInst = module.getParamValue(6);
			if (module.getFunctionCount() > 11){
				platInstYear = module.getParamValue(11);
			}
			
			if (module.getFunctionCount() > 21){
				ncbNumber = module.getParamValue(21);
			}
			
			//for CA
			if (isCADTGSearch && module.getFunctionCount() > 21){
				tract = module.getParamValue(21);
			}
			if (module.getFunctionCount() > 12){
				subdivision = module.getParamValue(12);
			}
		}
		else if(SECTIONAL_TYPE.equals(what)){
			sec  = module.getParamValue(0);
			tw  = module.getParamValue(1);
			rg = module.getParamValue(2);
			qo = module.getParamValue(3);
			qv = module.getParamValue(4);
		}
		else if(ARB_TYPE.equals(what)){
			sec  = module.getParamValue(0);
			tw  = module.getParamValue(1);
			rg = module.getParamValue(2);
			qo = module.getParamValue(3);
			qv = module.getParamValue(4);
			arb = module.getParamValue(5);
		}
		
		boolean emptyLot = StringUtils.isEmpty(lot);
		boolean emptyBlock = StringUtils.isEmpty(block);
		boolean emptyPlatBook = StringUtils.isEmpty(platBook);
		boolean emptyPlatPage = StringUtils.isEmpty(platPage);
		boolean emptyPlatInst = StringUtils.isEmpty(platInst);
		boolean emptyPlatInstYear = StringUtils.isEmpty(platInstYear);
		
		boolean emptySection = StringUtils.isEmpty(sec);
		boolean emptyRange = StringUtils.isEmpty(rg);
		boolean emptyTownship = StringUtils.isEmpty(tw);
		boolean emptyQO = StringUtils.isEmpty(qo);
		boolean emptyQV = StringUtils.isEmpty(qv);
		
		boolean emptyArb = StringUtils.isEmpty(arb);
		
		boolean emptyNcb = StringUtils.isEmpty(ncbNumber);
		boolean emptySubdivision = StringUtils.isEmpty(subdivision);
		boolean emptyTract = StringUtils.isEmpty(tract);
		
		boolean isPlated = !( (emptyPlatBook || emptyPlatPage) && emptyPlatInst) && !(emptyLot && emptyBlock) ;
		if (isCADTGSearch && !isPlated){
			isPlated = !(emptyPlatInst && emptyPlatInstYear);
		}
		
		if (isCADTGWithoutPlatPage){
			isPlated = !(emptyPlatBook && emptyPlatInst) && !(emptyLot && emptyBlock);
		}
		
		boolean subdivided = !isPlated;
		
		if (!emptyNcb && !emptySubdivision){
			subdivided = (emptyNcb && emptySubdivision);
		}
		
		if (isCADTGSearch && !emptyTract){
			subdivided = emptyTract;
		}
		boolean sectional = (emptySection || emptyRange || emptyTownship || emptyQO || emptyQV) || !emptyArb /*|| isPlated*/;
		if (ignoreQOandQV){
			sectional = (emptySection || emptyRange || emptyTownship) || !emptyArb /*|| isPlated*/;
		}
		boolean arb1 = emptyArb /*|| isPlated*/;
		
		if(SUBDIVIDED_TYPE.equals(what)){
			return subdivided;
		}
		else if(SECTIONAL_TYPE.equals(what)){
			return sectional;
		}
		else if(ARB_TYPE.equals(what)){
			if (isFloridaDTGSearch){
				if (emptyArb){
					return emptyArb;
				} else{
					return arb1 && emptySection;
				}
			} else{
				return arb1&&emptySection;
			}
		}
		
		return subdivided && sectional && arb1;
	}
    
    protected TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent, boolean forceOverritten){
    	ParsedResponse pr = response.getParsedResponse();
    	DocumentsManagerI manager = mSearch.getDocManager();
    	DocumentI doc = pr.getDocument() ;
    	if(doc instanceof RegisterDocumentI){
	    	RegisterDocumentI regDoc = (RegisterDocumentI)doc;
    		try{
	         	manager.getAccess();
	         	if ( regDoc!=null ){
	         		DocumentI origDoc = manager.getDocument(regDoc);
	         		if(origDoc instanceof RegisterDocumentI){
		         		RegisterDocumentI origRegDoc = (RegisterDocumentI)origDoc;
		         		if(origRegDoc!=null){
		         			if(origRegDoc.getParsedReferences().size() < regDoc.getParsedReferences().size() 
		         					|| origRegDoc.getGrantee().size() < regDoc.getGrantee().size() 
		         					|| origRegDoc.getGrantor().size() < regDoc.getGrantor().size() ){
		         				forceOverritten = true;
		         			}
		         		}
	         		}
	         	}
	    	 }finally{
	    		 manager.releaseAccess();
	    	 }
    	}
    	return super.addDocumentInATS(response, htmlContent,forceOverritten);
    }
    
    protected ServerResponse searchBy(TSServerInfoModule module, Object sd, String fakeResult) throws ServerResponseException {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		if (!Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS))) {
			global.removeAllInMemoryDocs();
        }
		global.clearClickedDocuments();
		
		int moduleIDX = module.getModuleIdx();
		int parserID = module.getParserID();
		
		Map<String, String[]> multiParams = new HashMap<String, String[]>();
		Map<String, String> params = getNonEmptyParams( module, multiParams );
		
		if( moduleIDX==TSServerInfo.IMG_MODULE_IDX ){
			logSearchBy(module);

			/*byte[] image = null;
			try {
				image = imagesConn.getDocumentByKey(StringUtils.defaultString(params.get("imageId")));
				//writeImageToClientAndCloseInputStream(new ByteArrayInputStream(image),"image/tiff" , image.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(image!=null && image.length>0){
				ServerResponse sr = new ServerResponse();
				sr.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK,image,
						"image/tiff"));
				return sr;
			}else{
				ServerResponse sr = new ServerResponse();
				sr.setImageResult(new DownloadImageResult(DownloadImageResult.Status.ERROR,null,
						"image/tiff"));
				return sr;
			}*/
			return null;
		}else{
			
			if(moduleIDX==TSServerInfo.NAME_MODULE_IDX){
				if(StringUtils.isBlank(module.getParamValue(3))){
					//return ServerResponse.createWarningResponse("Could not run Name Search ! OE Result ID is Empty !");
				}
			}
			Map<InstrumentI, DocumentI> temporaryDocuments = new HashMap<InstrumentI, DocumentI>();
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			
			if(modules.size()>1){
				
				TSServerInfoModule firstModule = modules.get(0);
				
				Vector<ParsedResponse> prs = new Vector<ParsedResponse>();
				
				for (TSServerInfoModule mod : modules) {
					if (verifyModule(mod))
						try {
							logSearchBy(mod);

							String response = performSearch( getNonEmptyParams( mod, null ), multiParams, mod.getModuleIdx(), fakeResult);
							if (StringUtils.isNotEmpty(response)){
								ServerResponse sr = processDTGResponse(response, mod.getModuleIdx(), mod.getParserID(),
										moduleIDX == TSServerInfo.MODULE_IDX41, true);
								
								if(isRelatedModule(mod)) {
									Map<InstrumentI, DocumentI> processRelated = processRelated(mod, sr);
									if(processRelated != null) {
										temporaryDocuments.putAll(processRelated);
									}
								}
								
								if(sr.getParsedResponse().getResultRows().size()>0)
									prs.addAll(sr.getParsedResponse().getResultRows());
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

				}
				
				ServerResponse serverResponse = new ServerResponse();  
				
				
				if(prs.size()>0){
					serverResponse.getParsedResponse().setResultRows(prs);
					serverResponse.setResult("");
					solveHtmlResponse("", parserID, "SearchBy", serverResponse, serverResponse.getResult());
					
					if(isRelatedModule(firstModule)) {
						
						Vector resultRows = serverResponse.getParsedResponse().getResultRows();
						if(resultRows != null && !resultRows.isEmpty()) {
							for (Object object : resultRows) {
								if (object instanceof ParsedResponse) {
									ParsedResponse parsedResponse = (ParsedResponse) object;
									DocumentI document = parsedResponse.getDocument();
									
									DocumentI alreadyProcessedDocument = temporaryDocuments.get(document.getInstrument());
									if(alreadyProcessedDocument != null 
											&& document != null
											&& alreadyProcessedDocument.getParsedReferences().size() > document.getParsedReferences().size()) {
										document.getParsedReferences().addAll(alreadyProcessedDocument.getParsedReferences());
									}
								}
							}
						}
					}
					
					serverResponse.getParsedResponse().setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, module);
					
					return serverResponse;
				} else {
					logInitialResponse(serverResponse);
					return ServerResponse.createEmptyResponse();
				}
					 
				
			}
			//don't log again if is a fake search
			if (StringUtils.isEmpty(fakeResult)){
				logSearchBy(module);
			}
			
			String response = null;
			try {
				response = performSearch( params, multiParams, module.getModuleIdx(), fakeResult );
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (JAXBException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
			ServerResponse sr = new ServerResponse();
			sr.setResult(response);
			
			if(response==null || StringUtils.isBlank(response)){
				logInitialResponse(sr);
				return ServerResponse.createEmptyResponse(); 
			}
			
		/*try {
			FileUtils.writeStringToFile(new File("E:/dtgresponse.xml"), response);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
			
			if (StringUtils.isEmpty(response)){
				logInSearchLogger("<font color=\"red\">PI returned empty response ! </font>", searchId, true);
				logInitialResponse(sr);
				return ServerResponse.createWarningResponse("PI returned empty response !");
			} else if (response.indexOf("outcome=\"FAILURE\"") > -1){
				Matcher mat = FAILURE_PAT.matcher(response);
				String failureMessage = "";
				if (mat.find()){
					failureMessage = "DTG response: " + mat.group(1);
				}
				logInSearchLogger("<font color=\"red\">" + failureMessage + "</font><br>", searchId, true);
				logInitialResponse(sr);
				return ServerResponse.createWarningResponse(failureMessage);
			} else if (response.indexOf("INVALID_INPUT") > -1){

				Pattern pat = Pattern.compile("(?is)<field name=\\\"[^\\\"]*\\\"\\s+value=\\\"([^\\\"]+)\\\"[^>]*>");
				Matcher mat = pat.matcher(response);
				if (mat.find()){
					String datasourceMessage = "DTG response: " + mat.group(1);
					logInSearchLogger("<font color=\"red\">" + datasourceMessage + " </font><br>", searchId, true);
					logInitialResponse(sr);
					return ServerResponse.createWarningResponse(datasourceMessage);
				}
			} else if (response.indexOf("SYSTEM_ERROR") > -1){

				Pattern pat = Pattern.compile("(?is)<field name=\\\"[^\\\"]*\\\"\\s+value=\\\"([^\\\"]+)\\\"[^>]*>");
				Matcher mat = pat.matcher(response);
				if (mat.find()){
					String datasourceMessage = "DTG response: " + mat.group(1);
					logInSearchLogger("<font color=\"red\">DTG response: " + datasourceMessage + " </font><br>", searchId, true);
					logInitialResponse(sr);
					return ServerResponse.createWarningResponse(datasourceMessage);
				}
			}
						
			ServerResponse serverResponse =  processDTGResponse(response,moduleIDX,parserID,moduleIDX==TSServerInfo.MODULE_IDX41,true);
			
			serverResponse.getParsedResponse().setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, module);
			
			if (StringUtils.isNotEmpty(fakeResult)){
				serverResponse.setFakeResponse(true);
			}
			solveHtmlResponse("", parserID, "SearchBy", serverResponse, serverResponse.getResult());
			processRelated(module, serverResponse);

			return serverResponse;
		}
		//return null;
	}
    
    protected Map<InstrumentI, DocumentI> processRelated(TSServerInfoModule module, ServerResponse serverResponse) {
		
		Map<InstrumentI, DocumentI> temporaryDocuments = new HashMap<InstrumentI, DocumentI>();
		
		if (isRelatedModule(module)){
			
			String modBook = null;
			String modPage = null;
			String modInstNo = null;
			
			boolean bookPage = org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(0).getParamValue())
					&& org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(1).getParamValue());
			boolean instrNo = org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(3).getParamValue());
			
			if (bookPage){
				modBook = module.getFunction(0).getParamValue().trim();
				modPage = module.getFunction(1).getParamValue().trim();
			}
			if (instrNo){
				modInstNo = module.getFunction(3).getParamValue().trim();
			}
			
			if (bookPage || instrNo){
			
				@SuppressWarnings("rawtypes")
				Vector resultRows = serverResponse.getParsedResponse().getResultRows();
				if (resultRows != null && !resultRows.isEmpty()){
					for (Object object : resultRows){
						if (object instanceof ParsedResponse){
							ParsedResponse parsedResponse = (ParsedResponse) object;
							DocumentI document = parsedResponse.getDocument();
							if (document != null){
								Set<InstrumentI> parsedReferences = document.getParsedReferences();
								boolean foundReference = false;
								if (parsedReferences != null && !parsedReferences.isEmpty()){
									for (InstrumentI instrumentI : parsedReferences){
										if (bookPage){
											if (modBook.equals(instrumentI.getBook()) && modPage.equals(instrumentI.getPage())){
												foundReference = true;
												break;
											}
										}
										if (instrNo){
											if (modInstNo.equals(instrumentI.getInstno())){
												foundReference = true;
												break;
											}
										}
									}
								}
								if (!foundReference){
									InstrumentI newReference = new Instrument();
									if (bookPage){
										newReference.setBook(modBook);
										newReference.setPage(modPage);
									}
									if (instrNo){
										newReference.setInstno(modInstNo);
									}
									if (org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(4).getParamValue())){
										try {
											newReference.setYear(Integer.parseInt(module.getFunction(4).getParamValue().trim()));
										} catch (Exception e) {
										}
									}
									document.addParsedReference(newReference);
									temporaryDocuments.put(document.getInstrument(), document);
								}
							}
						}
					}
				}
			}
		}
		
		return temporaryDocuments;
	}
	
	protected boolean isRelatedModule(TSServerInfoModule firstModule) {
		return firstModule.getModuleIdx() == TSServerInfo.MODULE_IDX41 && firstModule.getFunctionCount() == 6;
	}
	
    private boolean verifyModule(TSServerInfoModule mod) {
    	
    	if (mod == null)
    		return false;
    	
    	if (mod.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
			if (mod.getFunctionCount()>4 && StringUtils.isNotEmpty(mod.getFunction(4).getParamValue())) {
				return true;
			} 
			return false;
		}
    	
    	if (mod.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) {
			if (mod.getFunctionCount()>2 && StringUtils.isNotEmpty(mod.getFunction(0).getParamValue()) && StringUtils.isNotEmpty(mod.getFunction(1).getParamValue())) {
				return true;
			} 
			return false;
		}
    	
    	if (mod.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
    		if (mod.getFunctionCount()>2 && StringUtils.isNotEmpty(mod.getFunction(0).getParamValue()) && StringUtils.isNotEmpty(mod.getFunction(1).getParamValue())) {
				return true;
			} 
			return false;
		}
    	
    	if (mod.getModuleIdx() == TSServerInfo.TYPE_NAME_MODULE_IDX) {
    		if (mod.getFunctionCount()>4 && StringUtils.isNotEmpty(mod.getFunction(4).getParamValue())) {
				return true;
			} 
			return false;
		}
    	
    	if (mod.getModuleIdx() == TSServerInfo.MODULE_IDX41) {
    		if (mod.getFunctionCount()>4 && 
    				(StringUtils.isNotEmpty(mod.getFunction(0).getParamValue()) || StringUtils.isNotEmpty(mod.getFunction(1).getParamValue()) ||
    				StringUtils.isNotEmpty(mod.getFunction(3).getParamValue()) || StringUtils.isNotEmpty(mod.getFunction(4).getParamValue()))) {
				return true;
			} 
			return false;
		}
    	
    	System.err.println(this.getClass()+ "I shouldn't be here!!!");
		return false;
	}

	protected ServerResponse processDTGResponse(String response, int moduleIDX , int parserID, boolean referenceSearch, boolean log) throws ServerResponseException {
				
		// create & populate server response
		try {
			if (StringUtils.isNotBlank(response)){
				//CASatna Clara: 1984-33046
				Matcher mat = replaceCutTagsWithLabelValue.matcher(response);
				StringBuffer sb = new StringBuffer();
			    while (mat.find()) {
			    	mat.appendReplacement(sb, "<" + mat.group(3).toLowerCase() + " " + mat.group(2).toLowerCase() + "</" + mat.group(3).toLowerCase() + ">");
			    }
			    mat.appendTail(sb);
				response = sb.toString();
			}
			return processXMLResponse(response.getBytes(), false, null, referenceSearch, moduleIDX);
		} catch (Exception e) {
			e.printStackTrace();
			logInSearchLogger("<font color=\"red\">Data Source Exception ! </font>", searchId,log);
			return ServerResponse.createWarningResponse("Data Source Exception !");
		}
	}
    
    
    /**
     * create a 'virtual' link for searching the images: look_for_image.ats?book=<book>&page=<page>" 
     * @param record
     * @return virtual link or null if doc has no book and page
     */
    protected String getImageLink(DTRecord record){
    	
    	if(record == null){ return null; }
    	String [] dtImgInfo = record.getDTImageInfo();
    	if(dtImgInfo == null) { 
    		dtImgInfo = new String[2]; 
    		dtImgInfo[0] = "";
    		dtImgInfo[1] = "";
    	}
    	
    	String instr = record.getInstrumentNo();
    	if(StringUtils.isBlank(instr) || "unknown".equals(instr)){  
    		instr = "";
    	}
    	
    	String book = record.getBook();
    	String page = record.getPage();
    	String year = record.getInstYear();
    	String day  = record.getInstDay();
		String month = record.getInstMonth();
    	
    	book = StringUtils.isBlank(book)? "" :book; 
    	page = StringUtils.isBlank(page)? "" :page;
    	year = StringUtils.isBlank(year)? "" :year;
    	
    	String extraParameters = "";
    	
    	
    	String dataTreeIndex = "";
    	try{dataTreeIndex = StringUtils.defaultString(record.getInstrumentInfo().get("image.image_params.document_index_id"));}
    	catch(Exception e){};
    	
    	String dataTreeDesc = "";
    	try{dataTreeDesc = StringUtils.defaultString(record.getInstrumentInfo().get("image.image_params.description"));}
    	catch(Exception e){};
    	
    	
		if(getDataSite().getCountyId() == CountyConstants.NV_Clark && record.getRemarks() != null) {
			for (String remark : record.getRemarks()) {
				if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(remark)) {
					Pattern pattern = Pattern.compile("\\b([A-Z]{2})\\s*(\\d+)\\s*-\\s*(\\d+)\\b");
					Matcher matcher = pattern.matcher(remark.trim());
					if(matcher.find()) {
						MapType mapType = null;
						try {
							mapType = MapType.valueOf(matcher.group(1));
						} catch (IllegalArgumentException illegalArgumentException) {
						}
						
						if(mapType != null) {
							
							String aomBook = matcher.group(2);
							String aomPage = matcher.group(3);
							
							if(StringUtils.isBlank(book) && StringUtils.isBlank(page)) {
								record.getInstrumentInfo().put("book", aomBook);
								record.getInstrumentInfo().put("page", aomPage);
							}
							
							extraParameters = "&aomBook=" + aomBook + "&aomPage=" + aomPage + "&aomType=" + matcher.group(1);
							break;
						}
					}
				}
			}
			
    	}
    	
		if(StringUtils.isNotEmpty(extraParameters)) {
			return  CreatePartialLink(TSConnectionURL.idGET) + "look_for_dt_image" + extraParameters;
		}
    	
    	if(StringUtils.isEmpty(dataTreeIndex)||StringUtils.isEmpty(dataTreeDesc)){
			return null;
    	}else{
    		return  CreatePartialLink(TSConnectionURL.idGET) + "look_for_dt_image&id=" + dtImgInfo[0] +"&description=" + dtImgInfo[1]+ "&instr=" + instr 
					+ "&book=" + book+"&page=" + page  +"&year=" + year+"&month=" + month+ "&day=" + day
					+"&dataTreeIndex="+dataTreeIndex+"&dataTreeDesc="+dataTreeDesc;    	
    	}
    }
    
    protected String createPartialLink(int iActionType, int dispatcher) {
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
		build.append("&");
		build.append(getLinkPrefix(iActionType));
		return build.toString();
	}
        
	@Override
    protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
    	
    	String rsResponse = Response.getResult();
    	
    	if (StringUtils.isNotEmpty(Response.getError())){
    		return;
    	}
    	
        switch (viParseID) {
        case ID_DETAILS:
        case ID_SEARCH_BY_BOOK_AND_PAGE:
        case ID_SEARCH_BY_INSTRUMENT_NO: 
        	
        	// not saving to TSD
            List<ParsedResponse> items = (List<ParsedResponse>)Response.getParsedResponse().getResultRows();
            if (items.size()==0) {
            	Response.getParsedResponse().setResultRows(new Vector());	
				return;
			}
            for (ParsedResponse item:items) {
            	
            	DTRecord record = (DTRecord) item.getAttribute(ParsedResponse.DT_RECORD);
            	
            	// parse data
                //record.setParsedData(item,searchId, getDataSite());
            	setParsedData(record, item);
                
                // get instrument number
                String sInstrumentNo = record.getInstrumentNo();
                String instYear = record.getInstYear();
                if (StringUtils.isNotEmpty(instYear)){
                	sInstrumentNo += "_" + instYear;
                }
                                
                // to string
                String itemText = record.toString();
                itemText = itemText.replaceAll("(?is)null", "");
                
                String link = null;
            	if(Boolean.FALSE != getSearch().getAdditionalInfo("img_" + sInstrumentNo)){
            		link = getImageLink(record);
            	}                                
                if(link != null && link.length()>0){
                	if (Response.isFakeResponse() && "35".equals(dataSite.getSiteType())){
                		link += "&isFake=true";
                	}
                	itemText += "<a href=\"" + link + "\" target=\"_blank\">View Image</a>";
                	if(item.getImageLinksCount() == 0){
                		item.addImageLink(new ImageLinkInPage (link, sInstrumentNo + ".tiff" ));
                	}                	
                }
                
                item.setResponse(itemText);

                String originalLink = "DT___" + sInstrumentNo;
                String type = record.getServerDocType();
                if (StringUtils.isNotEmpty(type)){
                	originalLink += "_" + type.replaceAll("\\s+", "");
                }
                String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;   
                
                DocumentI doc = item.getDocument();
                
                if (Response.isFakeResponse()){
                	doc.setFake(true);
                }
                
                String instrumentNo = record.getInstrumentNo();
                
                boolean alreadySaved = isAlreadySaved(instrumentNo, doc, record);
                
            	String msg = alreadySaved ? "saved" : "<input type='checkbox' name='docLink' value='" + sSave2TSDLink + "'>";
            	itemText = "<tr><td valign=\"center\" align=\"center\">" + msg + "</td><td>" +  itemText + "</td></tr>";
                
				//if (!alreadySaved) {  this is not good, because some documents need to be resaved, and without link...can't be done
            	item.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
            	mSearch.addInMemoryDoc(sSave2TSDLink, item);
				//}
					
				try{
					getSearch().setAdditionalInfo(AdditionalInfoKeys.MODULE_PREFIX_KEY + "_" + miServerID + "_" + doc.getInstno(), 
																	Response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE));
        		} catch (Exception e) {
        			logger.error("Error tring to set module source for: " + instrumentNo, e);
				}
                
                parser.Parse(item, itemText,
                        Parser.NO_PARSE,
                        getLinkPrefix(TSConnectionURL.idGET),
                        TSServer.REQUEST_SAVE_TO_TSD);
                
            }
            
            if (mSearch.getSearchType() == Search.PARENT_SITE_SEARCH) {
                // add form header to result
                Response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + "<table width=\"100%\" border=\"1\" cellpadding=\"0\" cellspacing=\"0\">" +
                		"<tr bgcolor=\"#cccccc\">" +
            			"<th width=\"0\" align=\"center\">" + SELECT_ALL_CHECKBOXES + "All</th>" +
            			"<th align=\"left\">Document</th>" +
        			"</tr>");                
                //add form footer to result
                Response.getParsedResponse().setFooter("</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1));
            }
            
        	break;
        	
        case ID_SAVE_TO_TSD:
        	
        	ParsedResponse pr = (ParsedResponse) Response.getParsedResponse();
        	
        	DTRecord record = (DTRecord) pr.getAttribute(ParsedResponse.DT_RECORD);
        	DocumentI document = pr.getDocument();
        	
        	if(record == null && document != null) {
        		msSaveToTSDFileName = document.getInstno() + ".html";
	            
	            pr.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
        	} else {
        	
	    		// fill infsets
	    		//record.setParsedData(pr,searchId, getDataSite());
//	    		setParsedData(record, pr);
	    		
	    		if (document != null){
	    			if (document.getParsedReferences() != null){
	    				Set<InstrumentI> parsedReferences = document.getParsedReferences();
	    				for (InstrumentI instrumentI : parsedReferences) {
	    					if (!pr.getDocument().flexibleContainsParsedReference(instrumentI, true)){
	    						pr.getDocument().addParsedReference(instrumentI);
	    					}
						}
	    			}
	    			if (document.isFake()){
	    				pr.getDocument().setFake(true);
	                }
	    		}
	    		
	    		// create string representation
	    		String itemText = record.toString();
		        		
	        	String sInstrumentNo = record.getInstrumentNo();
	            
	            logger.info("Instrument NO:" + sInstrumentNo);
	           
	            String link = null;
	        	if(Boolean.FALSE != getSearch().getAdditionalInfo("img_" + sInstrumentNo)){
	        		link = getImageLink(record);
	        	} else {
	        		pr.resetImages();
	        	}
	        	
	            if(link != null && link.length()>0){
	            	if (document.isFake() && "35".equals(dataSite.getSiteType())){
                		link += "&isFake=true";
                	}
	            	itemText += "<a href=\"" + link + "\">View Image</a>";
	            	if(pr.getImageLinksCount() == 0){
	            		pr.addImageLink(new ImageLinkInPage (link, sInstrumentNo + ".tiff" ));
	            	}
	            }           
	            
	            pr.setResponse(itemText);
	            
	            msSaveToTSDFileName = sInstrumentNo + ".html";
	            
	            pr.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	            
	            rsResponse = pr.getResponse();
	            rsResponse = rsResponse.replaceAll("(?s)<input.*?>", "");
	            rsResponse = rsResponse.replaceAll("Select for saving to TS Report", "");
	            rsResponse = rsResponse.replaceAll("<hr>","");
	            
	            msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD(true);
	            
	            parser.Parse(pr, rsResponse, Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
	
	            String savedResponse = rsResponse;
	            savedResponse = savedResponse.replace("<table cellspacing=\"0\">", "<table border=\"1\" cellspacing=\"0\">");
	            savedResponse = savedResponse.replaceAll(":\\s*</b>", "");            
	            pr.setOnlyResponse(savedResponse);
	        	
	            // add the crossreferenced doc 
	            if(record.getReferencedDocs().size() != 0){
	            	
	            	Vector<ParsedResponse> parsedRows2 = new Vector<ParsedResponse>();
	            	
	            	for(DTRecord record2 : record.getReferencedDocs()){
	            		
	            		// check if doc already saved
	                    boolean alreadySaved = 
	                    	mSearch.hasSavedBookPage(record2.getBook(), record2.getPage()) || 
	                    	mSearch.hasSavedYearInst(record2.getInstYear(), record2.getInstNo()) || 
	                    	mSearch.hasSavedInst(record2.getCaseNo());                    
	                    if(alreadySaved){
	                    	continue;
	                    }
	                    
	                	ParsedResponse item2 = new ParsedResponse();
	                	
	                	// set DT record field
	                	item2.setAttribute(ParsedResponse.DT_RECORD, record2);
	                	
	                	// parse data
		                //record2.setParsedData(item2,searchId, getDataSite());
		                setParsedData(record2, item2);
		                
		                // to string
		                String itemText2 = record2.toString();
		                item2.setResponse(itemText2);
		            	
		                // get instrument number
		                String sInstrumentNo2 = record2.getInstrumentNo();
		                
		                String originalLink2 = "DT___" + sInstrumentNo2;
		                String type = record.getServerDocType();
		                if (StringUtils.isNotEmpty(type)){
		                	originalLink2 += "_" + type.replaceAll("\\s+", "");
		                }
		                String sSave2TSDLink2 = getLinkPrefix(TSConnectionURL.idGET) + originalLink2;   
		                
		                mSearch.addInMemoryDoc(sSave2TSDLink2, item2);	                
		                item2.setPageLink(new LinkInPage(sSave2TSDLink2+"&isSubResult=true", originalLink2+"&isSubResult=true", TSServer.REQUEST_SAVE_TO_TSD));   
		                
		                parsedRows2.add(item2);
	            	}
	            	
	            	Response.getParsedResponse().setOnlyResultRows(parsedRows2);
	            }
//	            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH){
		            Vector<CrossRefSet> crVector = (Vector) pr.infVectorSets.get("CrossRefSet");
		            if (crVector != null && crVector.size() > 0) {
		            	for (CrossRefSet crs : crVector){
		            		saveWithCrossReferences(crs, pr);
		            	 }
		            }
//	            }
        	}
            
        	break;
        }
    }
	
	/**
	 * implement it in the derived class (either in the class specific for a county if exists, either in the generic class for a state)
	 *       where you want to do the shit for equivalence
	 * @param instrumentNo
	 * @param doc
	 * @param record
	 * @return
	 */
	public boolean isAlreadySaved(String instrumentNo, DocumentI doc, DTRecord record){
		return isInstrumentSaved(instrumentNo, doc, null);
	}
	
	public void saveWithCrossReferences(CrossRefSet crs, ParsedResponse currentResponse){

		String book 	= crs.getAtribute(CrossRefSetKey.BOOK.getShortKeyName());
		String page 	= crs.getAtribute(CrossRefSetKey.PAGE.getShortKeyName());   
		String instNo 	= crs.getAtribute(CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName()); 
			
		if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {

			TSServerInfoModule module = this.getDefaultServerInfo().getModuleForSearch(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, new SearchDataWrapper());
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CHECK_ALREADY_SAVED, Boolean.TRUE);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Save with crossreferences");

			book = formatBook(book);
			module.forceValue(0, book);
			module.forceValue(1, page);

			String tempBookPageLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) + "&book=" + book + "&page=" + page;

			ParsedResponse prChild = new ParsedResponse();

			LinkInPage linkInPage = new LinkInPage(module, TSServer.REQUEST_SEARCH_BY_REC);
			if (tempBookPageLink != null) {
				linkInPage.setOnlyLink(tempBookPageLink);
			}
			prChild.setPageLink(linkInPage);
			currentResponse.addOneResultRowOnly(prChild);
		}
		if (StringUtils.isNotEmpty(instNo)){
			DTGDateObject dtgDateObject = checkForYear(crs);
			
			if (dtgDateObject.isDateValid()) {
				TSServerInfoModule module = this.getDefaultServerInfo().getModuleForSearch(TSServerInfo.INSTR_NO_MODULE_IDX, new SearchDataWrapper());
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CHECK_ALREADY_SAVED, Boolean.TRUE);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Save with crossreferences");
				instNo = formatInstrumentNumber(instNo);
				module.forceValue(0, instNo);
				if (dtgDateObject.getDate() != null){
					module.forceValue(1, dtgDateObject.getDate());
				}
	
				String tempInstrumentLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.INSTR_NO_MODULE_IDX) + "&instrument=" + instNo;
				ParsedResponse prChild = new ParsedResponse();
	
				LinkInPage linkInPage = new LinkInPage(module, TSServer.REQUEST_SEARCH_BY_REC);
				if (tempInstrumentLink != null) {
					linkInPage.setOnlyLink(tempInstrumentLink);
				}
	
				prChild.setPageLink(linkInPage);
	
				currentResponse.addOneResultRowOnly(prChild);
			}
		}
	}
	
	protected String formatInstrumentNumber(String instNo){
		return instNo;
	}
	
	protected String formatBook(String book){
		return book;
	}
	
	protected DTGDateObject checkForYear(CrossRefSet crs){
		DTGDateObject dtgDateObject = new DTGDateObject();
		
		String year 	= crs.getAtribute(CrossRefSetKey.YEAR.getShortKeyName());
		String month 	= crs.getAtribute(CrossRefSetKey.MONTH.getShortKeyName());
		String day 		= crs.getAtribute(CrossRefSetKey.DAY.getShortKeyName());
		
		if (StateContants.CA_STRING_FIPS.equals(dataSite.getStateFIPS())){
			if (CountyConstants.CA_Santa_Clara == dataSite.getCountyId()){
				dtgDateObject.setDate(null);
				dtgDateObject.setDateValid(true);
			} else{
				if (StringUtils.isNotEmpty(year)){
					dtgDateObject.setDate(year);
					dtgDateObject.setDateValid(true);
				}
			}
		} else if (StateContants.FL_STRING_FIPS.equals(dataSite.getStateFIPS())){
			if (StringUtils.isNotEmpty(year)){
				dtgDateObject.setDate(year);
				dtgDateObject.setDateValid(true);
			}
		} else if (StateContants.IL_STRING_FIPS.equals(dataSite.getStateFIPS())){
			if (StringUtils.isNotEmpty(year)){
				dtgDateObject.setDate(year);
				dtgDateObject.setDateValid(true);
			}
		} else if (StateContants.MO_STRING_FIPS.equals(dataSite.getStateFIPS())){
			if (StringUtils.isNotEmpty(year)){
				dtgDateObject.setDate(year);
				dtgDateObject.setDateValid(true);
			}
		} else if (StateContants.NV_STRING_FIPS.equals(dataSite.getStateFIPS())){
					if (CountyConstants.NV_Carson_City == dataSite.getCountyId()
							|| CountyConstants.NV_Lyon == dataSite.getCountyId()
							|| CountyConstants.NV_Washoe == dataSite.getCountyId()
							|| CountyConstants.NV_Douglas == dataSite.getCountyId()){
						if (StringUtils.isNotEmpty(year)){
							dtgDateObject.setDate(year);
							dtgDateObject.setDateValid(true);
						}
					} else if (CountyConstants.NV_Clark == dataSite.getCountyId()){
						if (StringUtils.isNotEmpty(year) && StringUtils.isNotEmpty(month) && StringUtils.isNotEmpty(day)){
							dtgDateObject.setDate(year + StringUtils.leftPad(month, 2, "0") + StringUtils.leftPad(day, 2, "0"));
							dtgDateObject.setDateValid(true);
						}
					}
		} else if (StateContants.OH_STRING_FIPS.equals(dataSite.getStateFIPS())){
			if (CountyConstants.OH_Summit == dataSite.getCountyId()){
				dtgDateObject.setDate(null);
				dtgDateObject.setDateValid(true);
			} else{
				if (StringUtils.isNotEmpty(year)){
					dtgDateObject.setDate(year);
					dtgDateObject.setDateValid(true);
				}
			}
		}
		
		return dtgDateObject;
	}
	
	void setParsedData(DTRecord record2, ParsedResponse item2){
		 record2.setParsedData(item2, searchId, getDataSite());
	}
	
	@Override
	protected boolean isEnabledAlreadyFollowed() {
		if (StateContants.NV_STRING_FIPS.equalsIgnoreCase(dataSite.getStateFIPS())){
			return false;
		} else{
			return true;
		}
	}
    
	@Override
	public String getPrettyFollowedLink (String initialFollowedLnk){	
		if (initialFollowedLnk.contains("instrument=") || initialFollowedLnk.contains("book=")){
			String instrument = StringUtils.substringBetween(initialFollowedLnk, "instrument=", "&");
    		String retStr =  "Document with ";
    		
    		if (StringUtils.isEmpty(instrument)){
    			instrument = StringUtils.substringAfter(initialFollowedLnk, "instrument=");
    			if (StringUtils.isNotEmpty(instrument)){
    				retStr += " Instrument " + instrument;
    			}
    		}
    		
    		String book = StringUtils.substringBetween(initialFollowedLnk, "book=", "&");
    		if (StringUtils.isEmpty(book)){
    			book = StringUtils.substringAfter(initialFollowedLnk, "book=");
    		}
    		String page = StringUtils.substringBetween(initialFollowedLnk, "page=", "&");
    		if (StringUtils.isEmpty(page)){
    			page = StringUtils.substringAfter(initialFollowedLnk, "page=");
    		}
    		if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)){
    			retStr += " Book-Page " + book + "-" + page;
    		}
    		
    		if (initialFollowedLnk.contains("year=")){
    			retStr += ":" + StringUtils.substringBetween(initialFollowedLnk, "year=", "&");
    		}
    		
    		retStr += " has already been processed from a previous search in the log file.";
    		
    		return  "<br/><span class='followed'>" + retStr + "</span><br/>";
    	} else if (initialFollowedLnk.toLowerCase().contains("dt___")){
    		String instrument = StringUtils.substringAfter(initialFollowedLnk.toLowerCase(), "dt___");
    		String retStr =  "Document with ";
    		
    		if (StringUtils.isNotEmpty(instrument)){
    			String[] instr = instrument.split("_");
    			if (instr.length > 0){
    				retStr += " Instrument " + instr[0];
    			}
    			if (instr.length > 1){
        			retStr += ":" + instr[1];
        		}
    			
    			retStr += " has already been processed from a previous search in the log file.";
        		
        		return  "<br/><span class='followed'>" + retStr + "</span><br/>";
    		} else{
    			return "<br/><span class='followed'>Link already followed: </span>" + preProcessLink(initialFollowedLnk) + "<br/>";
    		}
    	}
    	return "<br/><span class='followed'>Link already followed: </span>" + preProcessLink(initialFollowedLnk) + "<br/>";
    }
	
    private void calculateAndSetCertificationDate(Node doc){
    	NodeList certificationDates;
		try {
			certificationDates = getAllNodes(doc, "//plant_currency/thru_date");
			calculateAndSetCertificationDate(certificationDates, CDType.NA, false);
		} catch (XPathExpressionException e1) {
			
		}  
		try {
			certificationDates = getAllNodes(doc, "//plant_currency/court_date");
			calculateAndSetCertificationDate(certificationDates, CDType.CT, true);
		} catch (XPathExpressionException e1) {
			
		}
		try {
			certificationDates = getAllNodes(doc, "//plant_currency/geo_end_date");
			calculateAndSetCertificationDate(certificationDates, CDType.PI, true);
		} catch (XPathExpressionException e1) {
			
		}
		try {
			certificationDates = getAllNodes(doc, "//plant_currency/tte_end_date");
			calculateAndSetCertificationDate(certificationDates, CDType.GI, true);
		} catch (XPathExpressionException e1) {
			
		}
		try {
			certificationDates = getAllNodes(doc, "//plant_currency/inst_thru_date");
			calculateAndSetCertificationDate(certificationDates, CDType.IN, true);
		} catch (XPathExpressionException e1) {
			
		}
		
    	
    }
    
    private void calculateAndSetCertificationDate(NodeList certificationDates, CertificationDateDS.CDType type, boolean skipInCalculation){
    	if(certificationDates!=null){
   		 for(int i=0; i<certificationDates.getLength();) {

   	        	Node node = certificationDates.item(i);
   	        	NodeList children = node.getChildNodes();
   	        	
   	        	if(children!=null){
   	        		Calendar cal = Calendar.getInstance();
   	        		int testAllPressent = 0;
   	        		
	    	        	for(int j=0;j<children.getLength();j++){
	    	        		Node child = children.item(j);
	    	        		String nodeName = child.getNodeName();
	    	        		if("year".equalsIgnoreCase(nodeName)){
	    	        			String year = StringUtils.defaultString(child.getTextContent()).trim();
	    	        			if(StringUtils.isNotBlank(year)){
	    	        				try{
	    	        					int yearI = Integer.parseInt(year);
	    	        					testAllPressent+=1;
	    	        					cal.set(Calendar.YEAR, yearI);
	    	        				}catch(NumberFormatException e){};
	    	        			}
	    	        			
	    	        		}else if("month".equalsIgnoreCase(nodeName)){
	    	        			String month = StringUtils.defaultString(child.getTextContent()).trim();
	    	        			if(StringUtils.isNotBlank(month)){
	    	        				try{
	    	        					int monthI = Integer.parseInt(month);
	    	        					testAllPressent+=10;
	    	        					cal.set(Calendar.MONTH, monthI-1);
	    	        				}catch(NumberFormatException e){};
	    	        			}
	    	        		}else if("day".equalsIgnoreCase(nodeName)){
	    	        			String day = StringUtils.defaultString(child.getTextContent()).trim();
	    	        			try{
   	        					int dayI = Integer.parseInt(day);
   	        					testAllPressent+=100;
   	        					cal.set(Calendar.DAY_OF_MONTH, dayI);
   	        				}catch(NumberFormatException e){};
	    	        		}
	    	        	}
	    	        	
	    	        	//success parsing certification dates
	    	        	if( testAllPressent==111 ){
	    	        		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	    					String d2 = sdf.format(cal.getTime());

	    					CertificationDateDS certificationDateDS = new CertificationDateDS(Util.dateParser3(d2), dataSite.getSiteTypeInt());
	    					certificationDateDS.setType(type);
	    					certificationDateDS.setSkipInCalculation(skipInCalculation);
	    					
	    					getSearch().getSa().updateCertificationDateObject(certificationDateDS, dataSite);
	    				
	    	        	}
   	        	}
   	        	break;
   		 }
   	}
    }
    
    
    protected ServerResponse processXMLResponse(byte [] xmlResponse, boolean getDetails, Map<String,String> params, boolean referenceSearch, int moduleIDX) 
    		throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
    	    		
        // process the response                
    	Node doc = parseXMLDocument(xmlResponse);
    	calculateAndSetCertificationDate(doc);
    	
    	NodeList titleRecs = null;
    	if(referenceSearch){
    		titleRecs = getAllNodes(doc, "//title_report/title_rec/title_doc");
    		if (getDataSite().getCountyId() == CountyConstants.OH_Franklin
    				|| getDataSite().getStateFIPS().equals(StateContants.MO_STRING_FIPS)
    				
    				){
    			titleRecs = getAllNodes(doc, "//title_report/title_rec/title_doc/ref");
    		} else{
	    		if (titleRecs.getLength() == 1){
	    			Node singleNodeRef = titleRecs.item(0);
	    			if (singleNodeRef.getNodeName().equals("title_doc")){
	    				//CAOrange Related Search:1980-22395
	    				if (singleNodeRef.getChildNodes().getLength() == 2){
	    					if (singleNodeRef.getChildNodes().item(0).getNodeName().equals("inst")
	    							&& singleNodeRef.getChildNodes().item(1).getNodeName().equals("referring_docs")){
	    						try {
	    							titleRecs = getAllNodes(singleNodeRef, "referring_docs/title_rec/title_doc");
	    						} catch (XPathExpressionException e) {
	    							e.printStackTrace();
	    						}
	    					}
	    				}
	    			}
	    		}
	    		if (titleRecs.getLength() == 0){
	    			titleRecs = getAllNodes(doc, "//referring_docs/title_rec/title_doc/ref");
	    		}
    		}
    	} else {
    		if (dataSite.getStateFIPS().equals(StateContants.CA_STRING_FIPS)){
    			titleRecs = getAllNodes(doc, "//title_report/title_rec");
    		} else{
    			titleRecs = getAllNodes(doc, "//title_report/title_rec/title_doc");
    		}
    	}
    	
    	if (moduleIDX==TSServerInfo.SUBDIVISION_MODULE_IDX || moduleIDX==TSServerInfo.SECTION_LAND_MODULE_IDX ||
    			moduleIDX==TSServerInfo.ARB_MODULE_IDX) {
    		if (titleRecs.getLength()>0) {
    			Search search = getSearch();
    			@SuppressWarnings("unchecked")
				Set<Integer> additionalInfo = (Set<Integer>)search.getAdditionalInfo(AdditionalInfoKeys.MISSING_LAND_SEARCH_ID_SET);
    			if(additionalInfo == null) {
    				additionalInfo = new HashSet<Integer>();
    			}
    			additionalInfo.add(moduleIDX);
    			search.setAdditionalInfo(AdditionalInfoKeys.MISSING_LAND_SEARCH_ID_SET, additionalInfo);
    		} 
    	}
    		        
        // create the set of parsed records
        Map<String,DTRecord> parsedTitleRecs = new LinkedHashMap<String,DTRecord>();
        for(int i=0; i<titleRecs.getLength(); i++) {
 
        	Node node = titleRecs.item(i);
        	DTRecord record = parseTitleRec(node, dataSite);
        	if(record == null){ continue; }
        	String id = record.getId();
        	if(id == null) { continue; }
        	DTRecord same =  parsedTitleRecs.get(id);
        	if(same != null){
        		same.addInfo(record);
        	} else {
        		Set<String> remarks = record.getRemarks();
        		boolean isNotADoc = false;
        		if (remarks != null){
        			for (String remark : remarks) {
						if ("REFERRING:NOT_FOUND".equals(remark)){
							isNotADoc = true;
							break;
						}
					}
        		}
        		if (!isNotADoc){
        			parsedTitleRecs.put(id, record);
        		}
        	}
        }
        
        // create lists of book-page and case # to retrieve
        Set<String> bookPages = new LinkedHashSet<String>();               

        // add the party-ref cross-references to search list
        for(DTRecord record : parsedTitleRecs.values()){
    		for(Map<String,String> instr: record.getPartyRefList()){
    			String book = instr.get("book");
    			String page = instr.get("page");
    			String serverDoctype = instr.get("type");
    			if(book != null && page != null){
    				String key = book + "_" + page;
    				if (StringUtils.isNotEmpty(serverDoctype)){
    					key += "_" + serverDoctype.replaceAll("\\s+", "");
    				}
    				// add to the search list only if not already present in main list
    				if(!parsedTitleRecs.containsKey(key)){
    					bookPages.add(key);
    				}
    			}
    		} 
        }
        
        // in case we did not perform aditional searches: at least fix the party-ref book-page references
    	for(DTRecord record : parsedTitleRecs.values()){
    		if(record.getPartyRefList().size() != 0){
    			record.fixPartyRefInfo(parsedTitleRecs);
    		}
    	}        	
             
        ServerResponse serverResponse = new ServerResponse();        
    	StringBuffer sb = new StringBuffer();
        Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
       
        for(DTRecord record: parsedTitleRecs.values()){                    
            ParsedResponse parsedResponse = new ParsedResponse();
            parsedResponse.setAttribute(ParsedResponse.DT_RECORD, record);
            parsedRows.add(parsedResponse );
        }
        serverResponse.getParsedResponse().setResponse("DTG intermediary");
        serverResponse.getParsedResponse().setResultRows(parsedRows);
        serverResponse.setResult(sb.toString());
        logInitialResponse(serverResponse);
   
        return serverResponse;
        
    }
    protected boolean isTeleTitleCounty(int countyId){
    	return false;
    }
    
    protected void logInSearchLogger(String message, long searchId, boolean doLog){
		if (doLog){
			SearchLogger.info(message, searchId);
		}
	}
    
    private static ArbRequestParameters fillArbParameters(Map<String, String> params,Map<String, String[]>  multiParams){
    	//{platPage=pp33, sublot=sb11, fromDate=11/02/1965, chain=DISPLAY, 
    		//platBook=pb23, includeTax=NO, block=b20, lot=l10, toDate=11/02/2010, platDocNo=pinst34}
    	
    	String fromDateStr = StringUtils.defaultString(params.get("fromDate"));
		String toDateSTr = StringUtils.defaultString(params.get("toDate"));
		Date fromDate = Util.dateParser3(fromDateStr);
		Date toDate = Util.dateParser3(toDateSTr);
    	
    	String last = StringUtils.defaultString(params.get("lastName1"));
		String firstName = StringUtils.defaultString(params.get("firstName1"));
		String middleName = StringUtils.defaultString(params.get("middleName1"));
		String role = StringUtils.defaultString(params.get("role"));
		//String ssn = StringUtils.defaultString(params.get("ssn"));
		
		String investigativeStr = StringUtils.defaultString(params.get("investigative"));
		boolean investigative = "investigative".equalsIgnoreCase(investigativeStr);
		
		String []doctypes = multiParams.get("doctype");
		
		//String nickName = StringUtils.defaultString(params.get("nickName"));
		//String nameSearchType = StringUtils.defaultString(params.get("nameSearchType"));
		//String soundexBoolean = StringUtils.defaultString(params.get("soundexBoolean"));
		//String soundex = StringUtils.defaultString(params.get("sounddex"));
		//String nameMix = StringUtils.defaultString(params.get("nameMix"));
		//String sort = StringUtils.defaultString(params.get("sort"));
		
		NameI name = new Name();
		name.setLastName(last);
		name.setFirstName(firstName);
		name.setMiddleName(middleName);
		
		ArbRequestParameters arbRequestParams = new ArbRequestParameters();
		arbRequestParams.setInvestigative(investigative);
		arbRequestParams.setName(name);
		if(!"BOTH".equalsIgnoreCase(role)){
			arbRequestParams.setRole(role);
		}
		arbRequestParams.setInvestigative(investigative);
		arbRequestParams.setName(name);
		arbRequestParams.setRole(role);
		arbRequestParams.setFromDate(fromDate);
		arbRequestParams.setToDate(toDate);
		
		if(doctypes!=null){
			for(String str:doctypes){
				arbRequestParams.addNameCategory(NameCategoriesTypes.valueOf(str));
			}
		}
		
		arbRequestParams.setSearchParameters(fillArbSearchParameters(params, multiParams));
		
		return arbRequestParams;
    }
    
    private static SectionalRequestParameters fillSTRParameters(Map<String, String> params,Map<String, String[]>  multiParams){
    	   	
		
		String investigativeStr = StringUtils.defaultString(params.get("investigative"));
		boolean investigative = "investigative".equalsIgnoreCase(investigativeStr);
		
		SectionalRequestParameters sectionalRequestParams = new SectionalRequestParameters();
		sectionalRequestParams.setInvestigative(investigative);		
		
		String fromDateStr = StringUtils.defaultString(params.get("fromDate"));
		String toDateSTr = StringUtils.defaultString(params.get("toDate"));
		Date fromDate = Util.dateParser3(fromDateStr);
		Date toDate = Util.dateParser3(toDateSTr);
		
		sectionalRequestParams.setFromDate(fromDate);
		sectionalRequestParams.setToDate(toDate);
		
		sectionalRequestParams.setSearchParameters(fillSTRSearchParameters(params, multiParams));
		
		return sectionalRequestParams;
    }
    
    private  SubdividedRequestParameters fillSubdividedParameters(Map<String, String> params,Map<String, String[]>  multiParams){
    	//{platPage=pp33, sublot=sb11, fromDate=11/02/1965, chain=DISPLAY, 
    		//platBook=pb23, includeTax=NO, block=b20, lot=l10, toDate=11/02/2010, platDocNo=pinst34}
    	
    	String fromDateStr = StringUtils.defaultString(params.get("fromDate"));
		String toDateSTr = StringUtils.defaultString(params.get("toDate"));
		Date fromDate = Util.dateParser3(fromDateStr);
		Date toDate = Util.dateParser3(toDateSTr);
    	
    	String last = StringUtils.defaultString(params.get("lastName1"));
		String firstName = StringUtils.defaultString(params.get("firstName1"));
		String middleName = StringUtils.defaultString(params.get("middleName1"));
		String role = StringUtils.defaultString(params.get("role"));
		//String ssn = StringUtils.defaultString(params.get("ssn"));
		
		String investigativeStr = StringUtils.defaultString(params.get("investigative"));
		boolean investigative = "investigative".equalsIgnoreCase(investigativeStr);
		
		String []doctypes = multiParams.get("doctype");
		
		//String nickName = StringUtils.defaultString(params.get("nickName"));
		//String nameSearchType = StringUtils.defaultString(params.get("nameSearchType"));
		//String soundexBoolean = StringUtils.defaultString(params.get("soundexBoolean"));
		//String soundex = StringUtils.defaultString(params.get("sounddex"));
		//String nameMix = StringUtils.defaultString(params.get("nameMix"));
		//String sort = StringUtils.defaultString(params.get("sort"));
		
		NameI name = new Name();
		name.setLastName(last);
		name.setFirstName(firstName);
		name.setMiddleName(middleName);
		
		SubdividedRequestParameters subdividedParam = new SubdividedRequestParameters();
		subdividedParam.setInvestigative(investigative);
		subdividedParam.setName(name);
		if(!"BOTH".equalsIgnoreCase(role)){
			subdividedParam.setRole(role);
		}
		subdividedParam.setInvestigative(investigative);
		subdividedParam.setName(name);
		subdividedParam.setRole(role);
		subdividedParam.setFromDate(fromDate);
		subdividedParam.setToDate(toDate);
		
		if(doctypes!=null){
			for(String str:doctypes){
				subdividedParam.addNameCategory(NameCategoriesTypes.valueOf(str));
			}
		}
		
		subdividedParam.setSearchParameters(fillSubdividedSearchParameters(params, multiParams));
		
		return subdividedParam;
    }
    
    /**
	 * @return the usePlatInstrumentInsteadOfPlat
	 */
	protected boolean isUsePlatInstrumentInsteadOfPlat() {
		return usePlatInstrumentInsteadOfPlat;
	}

	/**
	 * @param usePlatInstrumentInsteadOfPlat the usePlatInstrumentInsteadOfPlat to set
	 */
	protected void setUsePlatInstrumentInsteadOfPlat(boolean usePlatInstrumentInsteadOfPlat) {
		this.usePlatInstrumentInsteadOfPlat = usePlatInstrumentInsteadOfPlat;
	}

	private static GeneralNameParameters fillNameParameters(Map<String, String> params,Map<String, String[]>  multiParams){
    	String last = StringUtils.defaultString(params.get("lastName1"));
		String firstName = StringUtils.defaultString(params.get("firstName1"));
		String middleName = StringUtils.defaultString(params.get("middleName1"));
		String role = StringUtils.defaultString(params.get("role"));
		String ssn = StringUtils.defaultString(params.get("ssn"));
		
		String investigativeStr = StringUtils.defaultString(params.get("investigative"));
		boolean investigative = "investigative".equalsIgnoreCase(investigativeStr);
		
		String spouseFirst = StringUtils.defaultString(params.get("firstNameSpouse"));
		String spouseMi = StringUtils.defaultString(params.get("miSpouse"));
		String spouseSSN = StringUtils.defaultString(params.get("spouseSSN"));
		String []doctypes = multiParams.get("doctype");
		
		String nickName = StringUtils.defaultString(params.get("nickName"));
		String nameSearchType = StringUtils.defaultString(params.get("nameSearchType"));
		String soundexBoolean = StringUtils.defaultString(params.get("soundexBoolean"));
		String soundex = StringUtils.defaultString(params.get("sounddex"));
		String nameMix = StringUtils.defaultString(params.get("nameMix"));
		String sort = StringUtils.defaultString(params.get("sort"));
		String fromDateStr = StringUtils.defaultString(params.get("fromDate"));
		String toDateSTr = StringUtils.defaultString(params.get("toDate"));
		//String torTee = StringUtils.defaultString(params.get("TorTee"));
		
		Date fromDate = Util.dateParser3(fromDateStr);
		Date toDate = Util.dateParser3(toDateSTr);
		
		NameI name = new Name();
		name.setLastName(last);
		name.setFirstName(firstName);
		name.setMiddleName(middleName);
		
		NameI spouse = null;
		if(StringUtils.isNotBlank(spouseFirst+spouseSSN+spouseMi)){
			spouse = new Name();
			spouse.setSsn4Decoded(ssn);
			spouse.setFirstName(spouseFirst);
			spouse.setMiddleName(spouseMi);
		}
		
		GeneralNameParameters nameParam = new GeneralNameParameters();
		nameParam.setInvestigative(investigative);
		nameParam.setName(name);
		if(!"BOTH".equalsIgnoreCase(role)){
			nameParam.setRole(role);
		}
		nameParam.setSpouse(spouse);
		nameParam.setNickName(nickName);
		nameParam.setNameSearchType(nameSearchType);
		nameParam.setSoundexBoolean(soundexBoolean);
		nameParam.setSoundexPercent(soundex);
		nameParam.setNameMix(nameMix);
		nameParam.setSort(sort);
		nameParam.setFromDate(fromDate);
		nameParam.setToDate(toDate);
		//if(!"BOTH".equalsIgnoreCase(torTee)){
			//nameParam.setTorTee(torTee);
		//}
		
		if(doctypes!=null){
			for(String str:doctypes){
				nameParam.addNameCategory(NameCategoriesTypes.valueOf(str));
			}
		}
		
		return nameParam;
    }
    
    protected String prepareDocumentNoForImageSearch(String docNo, String year, String month, String day){
    	return docNo;
    }
    
    
	@Override
	protected DownloadImageResult saveImage(ImageI image)throws ServerResponseException {
		HashMap<String, String> map = HttpUtils.getParamsFromLink( image.getLink(0) );
		
		String book 	 = map.get( "book" ) ;
		String page 	 = map.get( "page" ) ;
		String docNumber = map.get( "instr");
		String year 	 = map.get( "year" ) ;
		String type 	 = map.get( "type" ) ;	
    	String month 	 = map.get( "month" );
    	String day 		 = map.get( "day" );
		String dataTreeIndexStr = StringUtils.defaultString(map.get( "dataTreeIndex" ));
    	String dataTreeDesc =  StringUtils.defaultString(map.get( "dataTreeDesc" ));
		
    	int dataTreeIndex = -1;
    	try{dataTreeIndex = Integer.parseInt(dataTreeIndexStr);}catch(Exception e){};
    	
    	boolean savedImage = false;
    	
    	docNumber = prepareDocumentNoForImageSearch(docNumber, year, month, day);
    	
    	int yearInt = -1;
    	
		if(datTreeList==null){
			datTreeList = initDataTreeStruct();
		}
		
		InstrumentI i = new Instrument();
		i.setBook(book);
		i.setPage(page);
		i.setInstno(docNumber);
		i.setDocType(type);
		
		if (yearInt != SimpleChapterUtils.UNDEFINED_YEAR){
			i.setYear(yearInt);
		}
		
		if(dataTreeIndex>0){
			DataTreeAccount acc = DataTreeManager.getDatatTreeAccount(String.valueOf(getCommunityId()));
			try {
				SimpleImage im = null;
				try {
					im = DataTreeConn.retrieveImage(acc, dataTreeIndex , dataTreeDesc, 999, 0);
				} catch (DataTreeImageException e) {
					e.printStackTrace();
					SearchLogger.info(
							"<br/>FAILED to take Image(searchId="+searchId+" ) book="+i.getBook()+" page="+i.getPage()+" inst="+i.getInstno()+" from DataTree. "+
							"Official Server Message: [" + e.getLocalizedMessage() + " (" + e.getStatus() + ") ]<br/>", searchId);
				}
				if(im!=null){
					FileUtils.writeByteArrayToFile(new File(image.getPath()),im.getContent());
					logger.info("------------------------ downloadImageFromDataTree return true for instr="+i+" savePath="+image.getPath());
					savedImage =  true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			try {
				savedImage = FLGenericDASLDT.downloadImageFromDataTree(i, datTreeList, image.getPath(), String.valueOf(getCommunityId()), month, day);
			} catch (DataTreeImageException e) {
				logger.error("Error while getting image ", e);
				SearchLogger.info(
						"<br/>FAILED to take Image(searchId="+searchId+" ) book=" +
						i.getBook()+" page="+i.getPage()+" inst="+
						i.getInstno()+" from DataTree. "+
						"Official Server Message: [" + e.getLocalizedMessage() + " (" + e.getStatus() + ") ]<br/>", searchId);
			}
		}
		
		if (savedImage) {
			SearchLogger.info("<br/>Image(searchId=" + searchId + " )book=" + i.getBook() + "page=" + i.getPage() + "inst=" + i.getInstno() + " was taken from DataTree<br/>", searchId);
		}
		afterDownloadImage(savedImage, GWTDataSite.DG_TYPE);
		
		byte[] b = new byte[0];
		if (new File(image.getPath()).exists()){
			try {
				b = FileUtils.readFileToByteArray(new File(image.getPath()));
			} catch (IOException e) {e.printStackTrace();}
		}
		if(b.length>0){
			return new DownloadImageResult(Status.OK, b , image.getContentType());
		}
		
		return new DownloadImageResult(Status.ERROR, b , image.getContentType());
    }
   
	protected List<DataTreeStruct> initDataTreeStruct(){
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				InstanceManager.getManager().getCommunityId(searchId), 
				miServerID);
		return DataTreeManager.getProfileDataUsingStateAndCountyFips(dat.getCountyFIPS(), dat.getStateFIPS());
	}
    

    
    private String performSearch(Map<String, String> params,Map<String, String[]>  multiParams, int moduleIDX, String fakeResult) throws SecurityException, IllegalArgumentException, JAXBException, IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException{

		//CurrentInstance ci = InstanceManager.getManager().getCurrentInstance(mSearch.getID());
		//County county = ci.getCurrentCounty();
		//State state = ci.getCurrentState();
		//int stateFips = state.getStateFips();
		//int countyFips = county.getCountyFips();
		//String countyName = county.getName(); 
		//String stateName = state.getName();
		//String stateAbrev = state.getStateAbv().toUpperCase();
    	
    	String stateAbbrev = dataSite.getStateAbbreviation();
		if (StringUtils.isNotEmpty(stateAbbrev)){
			params.put("stateAbbrev", stateAbbrev);
		}
		
		if(getRequestCountType(moduleIDX)!=null)
			mSearch.countRequest(getDataSite().getSiteTypeInt(), (Integer) getRequestCountType(moduleIDX));
		
		if (StringUtils.isNotEmpty(fakeResult)){
			return fakeResult;
		}
    	
		switch (moduleIDX) {
			case TSServerInfo.NAME_MODULE_IDX:
				return conn.searchByName(fillNameParameters(params, multiParams));
			case TSServerInfo.TYPE_NAME_MODULE_IDX:
				if("ALL".equalsIgnoreCase(StringUtils.defaultString(params.get("searchType")))){
					return conn.searchByTrueGrantorGrantee(fillNameParameters(params, multiParams));
				}else{
					return conn.searchByGrantorGrantee(fillNameParameters(params, multiParams));
				}
			case TSServerInfo.SUBDIVISION_MODULE_IDX:{
				if(!isParentSite()){
					//params.put("investigative","INVESTIGATIVE");
				}
				return conn.searchBySubdividedLegal(fillSubdividedParameters(params, multiParams));
			}
			case TSServerInfo.ARB_MODULE_IDX:{
				if(!isParentSite()){
					//params.put("investigative","INVESTIGATIVE");
				}
				return conn.searchByArb(fillArbParameters(params, multiParams));
			}
			case TSServerInfo.SECTION_LAND_MODULE_IDX:{
				return conn.searchBySTR(fillSTRParameters(params, multiParams));
			}	
			case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
			case TSServerInfo.INSTR_NO_MODULE_IDX:
			case TSServerInfo.MODULE_IDX41:
				
				String book = StringUtils.defaultString(params.get("book"));
				String page = StringUtils.defaultString(params.get("page"));
				String instrNo = StringUtils.defaultString(params.get("docno"));
				String yearStr = StringUtils.defaultString(params.get("year"));
				String investigative = params.get("investigative");
				int year = -1;
				try{year = Integer.parseInt(yearStr);}catch(NumberFormatException e){}
				
				InstrumentI inst = new Instrument();
				inst.setBook(book);
				inst.setPage(page);
				inst.setYear(year);
				inst.setInstno(instrNo);
				
				if(moduleIDX == TSServerInfo.MODULE_IDX41){
					return conn.searchByReference(inst);
				}
				
				return conn.searchByInstrumentNo(inst);
			
			case TSServerInfo.PARCEL_ID_MODULE_IDX:
				String apn = StringUtils.defaultString(params.get("apn"));
				String portion = StringUtils.defaultString(params.get("portion"));
				String fromDateStr = StringUtils.defaultString(params.get("fromDate"));
				String toDateStr = StringUtils.defaultString(params.get("toDate"));
				String exName = StringUtils.defaultString(params.get("extendedName"));
				investigative = params.get("investigative");
				
				Date fromDate = Util.dateParser3(fromDateStr);
				Date toDate = Util.dateParser3(toDateStr);
				
				return conn.searchByAPN(apn, portion, fromDate, toDate, "investigative".equalsIgnoreCase(investigative), exName);
			
			case TSServerInfo.MODULE_IDX42:
				
				return conn.searchByAPNTaxSearch((HashMap<String, String>) params);
			
			/*case TSServerInfo.ADDRESS_MODULE_IDX:
				AddressI address = new Address(StringUtils.defaultString(params.get("number")), 
						StringUtils.defaultString(params.get("name")), StringUtils.defaultString(params.get("suffix")));
				address.setCity(StringUtils.defaultString(params.get("city")));
				address.setThruNumber(StringUtils.defaultString(params.get("toNumber")));
				address.setZip(StringUtils.defaultString(params.get("zipp")));
				address.setIdentifierType(StringUtils.defaultString(params.get("unitPrefix")));
				address.setIdentifierNumber( StringUtils.defaultString(params.get("unit")));
				address.setPreDiretion(StringUtils.defaultString(params.get("preDiretion")));
				address.setPostDirection(StringUtils.defaultString(params.get("postDirection")));
			break;*/
			default:
			break;
		}
		
		return null;
	}
    
    private static ARBSEARCHPARAMETERSType fillArbSearchParameters(Map<String, String> params, Map<String, String[]> multiParams) {
		com.stewart.ats.connection.datatrace.xsd.arb.ObjectFactory factory = new com.stewart.ats.connection.datatrace.xsd.arb.ObjectFactory();
		String lot = StringUtils.defaultString(params.get("lot"));
		String block = StringUtils.defaultString(params.get("block"));
		String page =  StringUtils.defaultString(params.get("page"));
		String book =  StringUtils.defaultString(params.get("book"));
    	
		String district = StringUtils.defaultString(params.get("district"));
		String parcel = StringUtils.defaultString(params.get("parcel"));
		String parcelSplit = StringUtils.defaultString(params.get("parcelSplit"));
		
		String arb = StringUtils.defaultString(params.get("arb"));
		String arbAPN = StringUtils.defaultString(params.get("arbAPN"));
		
		boolean useApnAsArb = StringUtils.isNotEmpty(arbAPN);
		
    	boolean usearbVersion2 = book.length()>3;
    	
		/* ***** -- start search parameters --*/	
		ARBSEARCHPARAMETERSType searchParameters = factory.createARBSEARCHPARAMETERSType();
		com.stewart.ats.connection.datatrace.xsd.arb.LOTIDENTIFIERType lotIdentifier = factory.createLOTIDENTIFIERType();
		
		ARBIDENTIFIERType arbIndetifier = factory.createARBIDENTIFIERType();
		ARBSEARCHPARAMETERSType.PARCELIDENTIFIER parcelIdentifier = factory.createARBSEARCHPARAMETERSTypePARCELIDENTIFIER();
		
		ARBSEARCHPARAMETERSType.TOWNSHIPIDENTIFIER townshipIdentifier = factory.createARBSEARCHPARAMETERSTypeTOWNSHIPIDENTIFIER();
		
		ARBSEARCHPARAMETERSType.APNIDENTIFIER apnIdentifier = factory.createARBSEARCHPARAMETERSTypeAPNIDENTIFIER();
		
		
		if(usearbVersion2){
			if(StringUtils.isNotBlank(book))
				arbIndetifier.setBook2(book);
			if(StringUtils.isNotBlank(page))
				arbIndetifier.setPage2(page);
			if(StringUtils.isNotBlank(block))
				arbIndetifier.setBlock2(block);
			if(StringUtils.isNotBlank(lot))
				lotIdentifier.setLot2(lot);
		} else if (useApnAsArb){
			apnIdentifier.setApn(arbAPN);
		} else{
			//platIdentifier.setAcPage(value);
			//platIdentifier.setBlock2(value);
			//platIdentifier.setBlockId(value);
			//platIdentifier.setBlockId(block);
			if(StringUtils.isNotBlank(book))
				arbIndetifier.setBook(book);
			if(StringUtils.isNotBlank(page))
				arbIndetifier.setPage(page);
			if(StringUtils.isNotBlank(block))
				arbIndetifier.setBlock(block);
			if(StringUtils.isNotBlank(district)&&StringUtils.isNotBlank(parcel)&&StringUtils.isNotBlank(parcelSplit)){
				arbIndetifier.setDistrict(district);
				parcelIdentifier.setParcel(parcel);
				parcelIdentifier.setParcelSplit(parcelSplit);
				searchParameters.setPARCELIDENTIFIER(parcelIdentifier);
			} else if (StringUtils.isNotEmpty(parcel)){
				parcelIdentifier.setParcel(parcel);
			}
			if(StringUtils.isNotBlank(lot))
				lotIdentifier.setLot(lot);
		}
		if (StringUtils.isNotEmpty(arb)){
			String[] areas = arb.trim().split("\\s*-\\s*");
			if (areas.length == 3){
				arbIndetifier.setBook(StringUtils.stripStart(areas[0], "0"));
				arbIndetifier.setPage(StringUtils.stripStart(areas[1], "0"));
				lotIdentifier.setLot(StringUtils.stripStart(areas[2], "0"));
			} else if (areas.length == 2){
				arbIndetifier.setBook(StringUtils.stripStart(areas[0], "0"));
				arbIndetifier.setPage(StringUtils.stripStart(areas[1], "0"));
			} else{
				String section = StringUtils.defaultString(params.get("section"));
				String township = StringUtils.defaultString(params.get("township"));
				String range =  StringUtils.defaultString(params.get("range"));
				String quarterOrder =  StringUtils.defaultString(params.get("quarterOrder"));
				String quarterValue = StringUtils.defaultString(params.get("quarterValue"));
				
				arbIndetifier.setSection(section);
				arbIndetifier.setRange(range);
				arbIndetifier.setQuarter(quarterValue);
				
				parcelIdentifier.setParcel(arb);
				townshipIdentifier.setTownship(township);
				searchParameters.setPARCELIDENTIFIER(parcelIdentifier);
				searchParameters.setTOWNSHIPIDENTIFIER(townshipIdentifier);
			}
		}
		if (useApnAsArb){
			searchParameters.setAPNIDENTIFIER(apnIdentifier);
		} else{
			searchParameters.setARBIDENTIFIER(arbIndetifier);
			searchParameters.setLOTIDENTIFIER(lotIdentifier);
		}
		
		return searchParameters;
	}

    private static SECTIONALSEARCHPARAMETERSType fillSTRSearchParameters(Map<String, String> params, Map<String, String[]> multiParams) {
		com.stewart.ats.connection.datatrace.xsd.sectional.ObjectFactory factory = new com.stewart.ats.connection.datatrace.xsd.sectional.ObjectFactory();
		
		String section = StringUtils.defaultString(params.get("section"));
		String township = StringUtils.defaultString(params.get("township"));
		String range =  StringUtils.defaultString(params.get("range"));
		String quarterOrder =  StringUtils.defaultString(params.get("quarterOrder"));
		String quarterValue = StringUtils.defaultString(params.get("quarterOrder"));
    	
		/* ***** -- start search parameters --*/	
		SECTIONALSEARCHPARAMETERSType searchParameters = factory.createSECTIONALSEARCHPARAMETERSType();
		com.stewart.ats.connection.datatrace.xsd.sectional.SECTIONALIDENTIFIERType sectionalIdentifier = factory.createSECTIONALIDENTIFIERType();
		
		if(StringUtils.isNotBlank(section)){
			sectionalIdentifier.setSection(section);
		}
		if(StringUtils.isNotBlank(township)){
			sectionalIdentifier.setTownship(township);
		}
		if(StringUtils.isNotBlank(range)){
			sectionalIdentifier.setRange(range);
		}

		searchParameters.setSECTIONALIDENTIFIER(sectionalIdentifier);
		
		return searchParameters;
	}
    
	private  SUBDIVIDEDSEARCHPARAMETERSType fillSubdividedSearchParameters(Map<String, String> params, Map<String, String[]> multiParams) {
		com.stewart.ats.connection.datatrace.xsd.subdivided.ObjectFactory factory = new com.stewart.ats.connection.datatrace.xsd.subdivided.ObjectFactory();
		String lot = StringUtils.defaultString(params.get("lot"));
		String lotThru = StringUtils.defaultString(params.get("lotThru"));
		
		String subLot = StringUtils.defaultString(params.get("sublot"));
		String subLotThru = StringUtils.defaultString(params.get("sublotthru"));
		
		String stateAbbrev = StringUtils.defaultString(params.get("stateAbbrev"));
		
		String block = StringUtils.defaultString(params.get("block"));
		String page =  StringUtils.defaultString(params.get("platPage"));
		String book =  StringUtils.defaultString(params.get("platBook"));
    	String pInstrument = StringUtils.defaultString(params.get("platDocNo"));
    	String pYear = StringUtils.defaultString(params.get("platYear"));
    	String subdivision = StringUtils.defaultString(params.get("subdivision"));
    	String unit = StringUtils.defaultString(params.get("unit"));
    	
    	String cityBlock =  StringUtils.defaultString(params.get("cityBlock"));
    	
    	String tract =  StringUtils.defaultString(params.get("tract"));
    	
    	String building = StringUtils.defaultString(params.get("building"));
    	String garage = StringUtils.defaultString(params.get("garage"));
    	String storage = StringUtils.defaultString(params.get("storage"));
    	
    	String apn = StringUtils.defaultString(params.get("apn"));
    	String portion = StringUtils.defaultString(params.get("portion"));
    	String subdivMap = StringUtils.defaultString(params.get("subdivMap"));
    	
		/* ***** -- start search parameters --*/	
		SUBDIVIDEDSEARCHPARAMETERSType searchParameters = factory.createSUBDIVIDEDSEARCHPARAMETERSType();
		PLATIDENTIFIERType platIdentifier = factory.createPLATIDENTIFIERType();
		//platIdentifier.setAcPage(value);
		//platIdentifier.setBlock2(value);
		//platIdentifier.setBlockId(value);
		//platIdentifier.setBlockId(block);
		if(StringUtils.isNotBlank(book))
			platIdentifier.setBook(book);
		if(StringUtils.isNotBlank(page))
			platIdentifier.setPage(page);
		if(StringUtils.isNotBlank(pInstrument))
			if (isUsePlatInstrumentInsteadOfPlat()){
				platIdentifier.setInstrument(pInstrument);
			}else{
				platIdentifier.setPlat(pInstrument);
			}
		//platIdentifier.setInstrument(pInstrument);
		//platIdentifier.setCase(value);
		//platIdentifier.setMap(pInstrument);
		//platIdentifier.setPlan(value);
		//platIdentifier.setPlat(value);
		//platIdentifier.setReception(value);
		//platIdentifier.setSubdivision(value);
		
		//MOStLouisCityDTG
		if (dataSite.getCountyId() == CountyConstants.MO_St_Louis_City){
			if (StringUtils.isNotBlank(cityBlock) && StringUtils.isNotBlank(subdivision)){
				platIdentifier.setBlock2(cityBlock);
				platIdentifier.setSubdivision(subdivision);
			} 
		} else if ("06".equals(dataSite.getStateFIPS())){//CA
			if (StringUtils.isNotBlank(tract)){
				platIdentifier.setTract(tract);
			} 
		} else{
			if(StringUtils.isNotBlank(subdivision)){
				platIdentifier.setSubdivisionName(subdivision);
			}
		}
		
		//platIdentifier.setTract(value);
		//platIdentifier.setTractId(value);
		if(StringUtils.isNotBlank(pYear))
			platIdentifier.setYear(pYear);
		//platIdentifier.setYYMMDD(value);
		searchParameters.setPLATIDENTIFIER(platIdentifier);
		
		SUBDIVIDEDIDENTIFIERType subdividedIdentifier = factory.createSUBDIVIDEDIDENTIFIERType();
		if(StringUtils.isNotBlank(apn))
			subdividedIdentifier.setAPN(apn);
		if(StringUtils.isNotBlank(block))
			subdividedIdentifier.setBlock(block);
		LOTIDENTIFIERType lotIdentifier = factory.createLOTIDENTIFIERType();
		if(StringUtils.isNotBlank(building))
			lotIdentifier.setBuilding(building);
		if(StringUtils.isNotBlank(garage))
			lotIdentifier.setGarage(garage);
		if(StringUtils.isNotBlank(lot))
			lotIdentifier.setLot(lot);
		if(StringUtils.isNotBlank(lotThru))
			lotIdentifier.setLotThru(lotThru);
		//lotIdentifier.setLotThru(value);
		if (StringUtils.isNotEmpty(stateAbbrev) && "FL".equals(stateAbbrev)){
			if(StringUtils.isNotBlank(subLot)){
				lotIdentifier.setSublot(subLot);
			}
		}
		if (StringUtils.isNotEmpty(subLotThru)){
			lotIdentifier.setSublotThru(subLotThru);
		}

		//lotIdentifier.setSublotThru(value);
		if(StringUtils.isNotBlank(storage))
			lotIdentifier.setStorage(storage);
		if(StringUtils.isNotBlank(unit))
			lotIdentifier.setUnit(unit);
		//lotIdentifier.setUnitThru(value);
		
		subdividedIdentifier.setLOTIDENTIFIER(lotIdentifier);
		if(StringUtils.isNotBlank(portion))
			subdividedIdentifier.setPortion(portion);
		//subdividedIdentifier.setSingleSearch(value);
		if(StringUtils.isNotBlank(subdivMap))
			subdividedIdentifier.setSubdividedMap(subdivMap);
		/* ***** -- end search parameters --*/	
		
		searchParameters.setSUBDIVIDEDIDENTIFIER(subdividedIdentifier);
		return searchParameters;
	}

	private static boolean addDocNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId,Set<String> searched, boolean isUpdate){
		if ( inst.hasDocNo() ){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			String instr = inst.getDocno().replaceFirst("^0+", "");
			String year = String.valueOf(inst.getYear());
			if(!searched.contains(instr+year)){
				searched.add(instr+year);
			}else{
				return false;
			}
			module.setData(0, instr);
			//module.setData(2, year);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
		if ( inst.hasInstrNo() ){
			String instr = inst.getInstno().replaceFirst("^0+", "");
			
			String year1 = String.valueOf(inst.getYear());
			if(!searched.contains(instr+year1)){
				searched.add(instr+year1);
			}else{
				return false;
			}
			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.setData(0, instr);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean addAoLookUpSearches(TSServerInfo serverInfo,List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef, long searchId, boolean isUpdate, boolean isTimeShare) {
		boolean atLeastOne = false;
		final Set<String> searched = new HashSet<String>();
		int stop = isTimeShare?5:Integer.MAX_VALUE;
		int i=0;
		
		for(InstrumentI inst:allAoRef){
			i++;
			boolean t = addBookPageSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
			atLeastOne = atLeastOne || t;
			
			if( inst.hasInstrNo() ){
				boolean temp = addInstNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
				atLeastOne = atLeastOne || temp;
			}
			
			if ( inst.hasDocNo()  ){
				boolean temp = addDocNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
				atLeastOne = atLeastOne || temp;
			}
			if(i>=stop){
				break;
			}
		}
		
		if (atLeastOne){
			lookupWithInstrument = true;
		}
		return atLeastOne;
	}

	protected boolean addBookPageSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched,boolean isUpdate){
		if(inst.hasBookPage()){
			String book = inst.getBook().replaceFirst("^0+", "");
			String page = inst.getPage().replaceFirst("^0+", "");
			if(!searched.contains(book+"_"+page)){
				searched.add(book+"_"+page);
			}else{
				return false;
			}
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			module.setData(0, book);
			module.setData(1, page);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	@Override
	public void addIteratorModule(TSServerInfo serverInfo,	List<TSServerInfoModule> modules, int subdivisionModuleIdx, long searchId, boolean isUpdate, boolean isTimeShare) {
		FLSubdividedBasedDASLDT.addIteratorModuleGlobal(serverInfo, modules, subdivisionModuleIdx, searchId, isUpdate, isTimeShare);
	}

	@Override
	public ArrayList<NameI> addNameSearch(List<TSServerInfoModule> modules,TSServerInfo serverInfo, String key,
			ArrayList<NameI> searchedNames, List<FilterResponse> filters) {
		ConfigurableNameIterator nameIterator = null;
		
		TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		if (StringUtils.isNotEmpty(key)){
			if (SearchAttributes.OWNER_OBJECT.equals(key)){
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			} else if (SearchAttributes.BUYER_OBJECT.equals(key)){
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
			}
		}
		module.clearSaKeys();
		module.setSaObjKey(key);

		for (int i = 0; i < filters.size(); i++) {
			if(filters.get(i)!=null){
				module.addFilter(filters.get(i));
			}
		}
		addBetweenDateTest(module, false, true, true);
		module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }, 25);
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

	@Override
	public void addOCRSearch(List<TSServerInfoModule> modules,	TSServerInfo serverInfo, FilterResponse... filters) {
	    // ONLY OCR last transfer 
		TSServerInfoModule  module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
	    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    module.clearSaKeys();
		modules.add(module);
	}

	@Override
	public void addAssesorMapSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules, boolean isUpdate) {	}

	@Override
	public void addPlatMapSearch(TSServerInfoModule module,	PersonalDataStruct str) { }

	@Override
	public void addSubdivisionMapSearch(TSServerInfoModule module, PersonalDataStruct str) { }

	@Override
	public void addParcelMapSearch(TSServerInfoModule module, PersonalDataStruct str) { }

	@Override
	public void addCondoMapSearch(TSServerInfoModule module, PersonalDataStruct str) {	}

	@Override
	public void addAssesorMapSearch(TSServerInfoModule module, PersonalDataStruct str, String extraStringForsection) {	}
	
	
	protected static String[] getSubdivisionVector(DocumentsManagerI m){
		final String[] ret = new String[5];
		
		RegisterDocumentI tr = m.getLastRealTransfer();
		if (tr != null){
			for (PropertyI prop : tr.getProperties()){
				if (prop.hasSubdividedLegal()){
					SubdivisionI sub = prop.getLegal().getSubdivision();
					
					ret[0] = sub.getPlatBook();
					ret[1] = sub.getPlatPage();
				}
				if (prop.hasTownshipLegal()){
				TownShipI town = prop.getLegal().getTownShip();
					ret[2] = town.getTownship();
					ret[3] = town.getRange();
					ret[4] = town.getSection();
				}
			}
		} else{
			tr = m.getLastMortgageForOwner();
			if (tr != null){
				for (PropertyI prop : tr.getProperties()){
					if (prop.hasSubdividedLegal()){
						SubdivisionI sub = prop.getLegal().getSubdivision();
						ret[0] = sub.getPlatBook();
						ret[1] = sub.getPlatPage();
					}
					if (prop.hasTownshipLegal()){
						TownShipI town = prop.getLegal().getTownShip();
						ret[2] = town.getTownship();
						ret[3] = town.getRange();
						ret[4] = town.getSection();
					}
				}
			}
		}
		return ret;
	}
	
	/*
	 TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		module.clearSaKeys();
		module.setSaObjKey(key);

		for (int i = 0; i < filters.size(); i++) {
			if(filters.get(i)!=null){
				module.addFilter(filters.get(i));
			}
		}
		addBetweenDateTest(module, false, true, true);
		module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }, 25);
		nameIterator.setAllowMcnPersons( true );
		
		if ( searchedNames!=null ) {
			nameIterator.setInitAgain( true );
			nameIterator.setSearchedNames( searchedNames );
		}
		
		searchedNames = nameIterator.getSearchedNames() ;
		module.addIterator( nameIterator );
		
		modules.add( module );
		return searchedNames;
	 * */
	
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		ConfigurableNameIterator nameIterator = null;
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
	    
	    DocTypeSimpleFilter doctypeFilter = (DocTypeSimpleFilter)DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
	    String doctypes[] = {DocumentTypes.MISCELLANEOUS,DocumentTypes.LIEN,DocumentTypes.COURT,DocumentTypes.MORTGAGE,DocumentTypes.AFFIDAVIT,DocumentTypes.RELEASE};
	    doctypeFilter.setDocTypes(doctypes);
	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	    	module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
	    	module.clearSaKeys();
	    	module.setIndexInGB(id);
	    	module.setTypeSearchGB("grantor");
	    	module.clearSaKeys();
	    	module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
	    	String date=gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
	    	if (date!=null) { 
		    	module.getFunction(0).forceValue(date);
	    	}
	    	FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
			((GenericNameFilter)nameFilter).setUseSynonymsBothWays(true);
			nameFilter.setInitAgain(true);
			
			FilterResponse transferNameFilter = NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module);
			((GenericNameFilter)transferNameFilter).setUseSynonymsBothWays(true);
			transferNameFilter.setInitAgain(true);
		     
		    module.addFilter( nameFilter );
			module.addFilter( transferNameFilter );
			module.addFilter( doctypeFilter );
	    	module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
	    	
	    	module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			//module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
			module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
	    	nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;" } );
		 	module.addIterator(nameIterator);
		 	modules.add(module);
		     
		    if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
		    	module =new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		    	module.clearSaKeys();
			    module.setIndexInGB(id);
			    module.setTypeSearchGB("grantee");
			    module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				date=gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				if (date!=null) {
					module.getFunction(0).forceValue(date);
				}
				
				nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
				((GenericNameFilter)nameFilter).setUseSynonymsBothWays(true);
				nameFilter.setInitAgain(true);
				
				transferNameFilter = NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module);
				((GenericNameFilter)transferNameFilter).setUseSynonymsBothWays(true);
				transferNameFilter.setInitAgain(true);
				
				module.addFilter( nameFilter );
				module.addFilter( transferNameFilter );
				module.addFilter( doctypeFilter );
			    module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
			    
			    module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				//module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
				module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
				module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
			    nameIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;" } );
				module.addIterator(nameIterator);			
				modules.add(module);
		    }
	    } 
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		try{
			FLSubdividedBasedDASLDT.setModulesForAutoSearchGlobal(serverInfo, searchId, this);
		}catch (Exception e) {
			serverInfo.setModulesForAutoSearch(new ArrayList<TSServerInfoModule>());
			e.printStackTrace(); 
		}
	}
	
	@Override
	public String prepareApnPerCounty(long searchId) {
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
		
		String apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNONDB ).replaceAll("[.-]", "");
		if(StringUtils.isEmpty(apn)){
			apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNO ).replaceAll("[.-]", "");
		}
		
		search.getSa().setAtribute(SearchAttributes.LD_PARCELNO3,apn);
		return apn;
	}
	
	protected boolean testIfExist(Set<LegalStructDTG> legalStruct2, LegalStructDTG l,long searchId) {
		preparePersonalStructForCounty(l,searchId);
		
		if (ARB_TYPE.equalsIgnoreCase(l.getType())){
			for (LegalStructDTG p : legalStruct2){
				preparePersonalStructForCounty(p,searchId);
				if (p.isArb()){
					if (l.equalsArb(p)){
						return true;
					}
				}
			}
		}else if (SECTIONAL_TYPE.equalsIgnoreCase(l.getType())){
			for (LegalStructDTG p : legalStruct2){
				if (p.isSectional() || p.isArb()){
					preparePersonalStructForCounty(p,searchId);
					if (l.equalsSectional(p)){
						return true;
					}
				}
			}
		}else if(SUBDIVIDED_TYPE.equalsIgnoreCase(l.getType())){
			for (LegalStructDTG p:legalStruct2){
				if (p.isPlated()){
					preparePersonalStructForCounty(p,searchId);
					if (l.equalsSubdivided(p)){
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public  GenericRuntimeIterator<InstrumentI> getInstrumentIterator(boolean reference) {
		return new DTReferencesIterator();
	}


	protected static boolean validMonth(String monthStr) {
		return validInt(monthStr,1,12);
	}

	protected static boolean validYear(String yearStr) {
		if(yearStr==null){
			return false;
		}
		if(yearStr.length()==4){
			return validInt(yearStr,1960,Calendar.getInstance().get(Calendar.YEAR)) ;
		}else if(yearStr.length()==2){
			return validInt(yearStr,0,get2digitsYear()) || validInt(yearStr, 60, 99);
		}
		return false;
	}

	protected static int get2digitsYear(){
		return Calendar.getInstance().get(Calendar.YEAR) - 1900 ;
	}
	
	protected static boolean validDay(String day) {
		return validInt(day, 1, 31);
	}

	private static boolean validInt(String str, int min, int max){
		try{
			int month = Integer.parseInt(str);
			return month>=min && month<=max;
		}catch(Exception e){}
		return false;
	}
	
	class DTReferencesIterator extends GenericRuntimeIterator<InstrumentI> {
		 
		private static final long serialVersionUID = -8452953295747003765L;
		
		private int county ;
		
		
		DTReferencesIterator() {
			super(TSServerDTG.this.searchId);
			setDataSite(TSServerDTG.this.getDataSite());
			county = dataSite.getCountyId();
			
		}
		
		private  boolean disableThisSearchForCounty(int county){
			if (CountyConstants.NV_Clark == county || CountyConstants.NV_Washoe == county){
				return true;
			}
			return false;
		}
		
		private  boolean isEnabledForCounty(int county){
			return true;
		}
		
		protected List<InstrumentI> createDerrivations(){
			
			DocumentsManagerI docM = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
			List<InstrumentI> instruments = new ArrayList<InstrumentI>();
			
			if(isEnabledForCounty(county)){
				try{
					docM.getAccess();
					List<RegisterDocumentI> docs = docM.getRoLikeDocumentList();
					for(DocumentI doc: docs){
						RegisterDocumentI regDoc = (RegisterDocumentI)doc;
						if(regDoc.isNotOneOf(DocumentTypes.RELEASE)){ 
							Set<InstrumentI> references = regDoc.getParsedReferences();
							if (references.size() < ServletServerComm.MAX_CROSS_REFS_SEARCH){
								for(InstrumentI instr:references){
									InstrumentI ref = instr.clone();
									ref.setDocSubType(DocumentTypes.getDocumentSubcategory(ref.getDocType(), searchId));
									ref.setDocType(DocumentTypes.getDocumentCategory(ref.getDocType(), searchId));
									if(!docM.flexibleContains(ref)){
										if (ref.hasDocNo() || hasBookPage(ref) || (ref.hasInstrNo() && hasYear(ref))){
											instruments.add(ref);
										}
									}
								}
							}
						}
					}
				}finally{
					docM.releaseAccess();
				}
			}
			
			return instruments;
		}
		
		protected boolean hasBookPage(InstrumentI ref){
			if (ref.hasBookPage()){
				ref.setBook(prepareBookForReferenceSearch(ref));
				ref.setPage(preparePageForReferenceSearch(ref));
				
				hasYear(ref);
				return true;
			}
			
			return false;
		}
		
		protected boolean hasYear(InstrumentI ref){
			
			if (!yearIsMandatory()){
				return true;
			}
			if (ref.getYear() > SimpleChapterUtils.UNDEFINED_YEAR){
				ref.setInstno(prepareInstNoForReferenceSearch(ref));
				
				return true;
			} else{
				ref.setInstno(prepareInstNoForReferenceSearch(ref));
				String year = prepareInstrumentYearForReferenceSearch(ref);
				
				if (StringUtils.isEmpty(year)){
					return false;
				} else{
					int yearInt = -1;
					try {
						yearInt = Integer.parseInt(year);
					} catch (Exception e) {
					}
					
					if (yearInt > 0){
						ref.setYear(yearInt);
					} else{
						return false;
					}
				}
				
			}
			
			return false;
		}
		
		protected boolean yearIsMandatory(){
			
			int countyId = dataSite.getCountyId();
			// these counties don't need year for search
			if (
					countyId == CountyConstants.CA_San_Mateo
					|| countyId == CountyConstants.CA_Santa_Clara
					|| countyId == CountyConstants.FL_Collier
					|| countyId == CountyConstants.FL_Manatee
					|| countyId == CountyConstants.FL_Monroe
					|| countyId == CountyConstants.OH_Summit
					){
				
				return false;
			}
			
			return true;
		}
		protected void loadDerrivation(TSServerInfoModule module, InstrumentI doc){
			switch(module.getModuleIdx()){
				case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
					if (disableThisSearchForCounty(county)){
						if(!doc.hasYear() || !doc.hasInstrNo()){
							module.setData(0, doc.getBook());
							module.setData(1, doc.getPage());
						}
					} else {
						module.setData(0, doc.getBook());
						module.setData(1, doc.getPage());
					}
				break;
				case TSServerInfo.INSTR_NO_MODULE_IDX:
					module.setData(0, doc.getInstno());
					if (yearIsMandatory()){
						module.setData(1, String.valueOf(doc.getYear()));
					}
				break;
			}

			List<FilterResponse> allFilters = module.getFilterList();
			ExactDateFilterResponse dateFilter = null;
			if (allFilters != null) {
				for (FilterResponse filterResponse : allFilters) {
					if (filterResponse instanceof ExactDateFilterResponse) {
						dateFilter = (ExactDateFilterResponse) filterResponse;
						dateFilter.getFilterDates().clear();
						if (doc instanceof RegisterDocument) {
							dateFilter.addFilterDate(((RegisterDocumentI)doc).getRecordedDate());
							dateFilter.addFilterDate(((RegisterDocumentI)doc).getInstrumentDate());
						} else {
							dateFilter.addFilterDate(doc.getDate());
						}
					}
				}
			}
		}

	}
	
	protected String prepareInstNoForReferenceSearch(InstrumentI inst) {
		return inst.getInstno();
	}

	protected String prepareBookForReferenceSearch(InstrumentI inst) {
		return inst.getBook();
	}

	protected String preparePageForReferenceSearch(InstrumentI inst) {
		return inst.getPage();
	}

	protected String prepareInstrumentYearForReferenceSearch(InstrumentI inst) {
		if(inst.getYear()<0){
			return "";
		}
		return String.valueOf(inst.getYear());
	}

	@Override
	public void addGrantorGranteeSearch(List<TSServerInfoModule> modules, TSServerInfo serverInfo, String key, List<FilterResponse> filters) {
		
		if (lookupWithInstrument){
			return;
		}
		
		ConfigurableNameIterator nameIterator = null;
		
		TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.TYPE_NAME_MODULE_IDX) );
		module.clearSaKeys();
		module.setSaObjKey(key);

		for (int i = 0; i < filters.size(); i++) {
			if(filters.get(i)!=null){
				module.addFilter(filters.get(i));
			}
		}
		addBetweenDateTest(module, false, true, true);
		module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }, 25);
		nameIterator.setAllowMcnPersons( true );
		
		//nameIterator.setInitAgain( true );
		module.addIterator( nameIterator );
		
		modules.add( module );
	}

	@Override
	public void addRelatedSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		// no need for related search here
	}
	
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		List<TSServerInfoModule> list = new ArrayList<TSServerInfoModule>();
		
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(book) && ro.cst.tsearch.utils.StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(2,"INVESTIGATIVE");
			list.add(module);
		}
		
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber()) 
				&& restoreDocumentDataI.getRecordedDate() != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(restoreDocumentDataI.getRecordedDate());
			
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
			module.forceValue(1, Integer.toString(cal.get(Calendar.YEAR)));
			module.forceValue(2,"INVESTIGATIVE");
			list.add(module);
		}
		
		module = getDefaultServerInfo().getModule(TSServerInfo.FAKE_MODULE_IDX);
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE, 
				restoreDocumentDataI);
		list.add(module);
		
		return list;
	}
	
	@Override
	public void performAdditionalProcessingAfterRunningAutomatic() {
		super.performAdditionalProcessingAfterRunningAutomatic();
		
		Search search = getSearch();
		
		if (isUpdate()){
			SearchAttributes sa = search.getSa();
			
			if (!sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
				Set<InstrumentI> allAoRef = FLSubdividedBasedDASLDT.getAllAoAndTaxReferences(search);
				
				if (allAoRef.size() > 0){
					SearchLogger.info("\n</div><hr/><div><BR>Run additional searches to get Certification Date. <BR></div>\n", searchId);
					TSServerInfo serverInfo = getCurrentClassServerInfo();
					
					for(InstrumentI inst : allAoRef){
						try {
							if (inst.hasBookPage()){
								String book = inst.getBook();
								String page = inst.getPage();
								
								TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
								module.setData(0, book);
								module.setData(1, page);
								module.setData(2, "B_P");
									
								ServerResponse response = SearchBy(module, null);
								
								if (sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
									SearchLogger.info("\n</div><div><BR>Certification Date found!<BR><hr/></div>\n", searchId);
									break;
								}
								
							} else if (inst.hasDocNo()){
								
								String docNo = inst.getDocno();
								if(docNo.length() == 10 && !docNo.startsWith(FLSubdividedBasedDASLDT.getStartingZerosForCounty(searchId))){ //Book Page
									String book = docNo.substring(0,6);
									String page = docNo.substring(6,10);
											
									TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
									module.setData(0, book);
									module.setData(1, page);
									module.setData(2, "B_P");
									
									ServerResponse response = SearchBy(module, null);
									
								} else{
									String year = String.valueOf(inst.getYear());
	
									TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
									module.setData(0, inst.getInstno());
									module.setData(1, "INST");
									module.setData(2, year);
									
									ServerResponse response = SearchBy(module, null);
								}
								
								if (sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
									SearchLogger.info("\n</div><div><BR>Certification Date found!<BR><hr/></div>\n", searchId);
									break;
								}
							} else if (inst.hasInstrNo()){
							
								String  instNo = inst.getInstno();
								
								if(instNo.length() == 10 && !instNo.startsWith(FLSubdividedBasedDASLDT.getStartingZerosForCounty(searchId))){ //Book Page
										
									String book = instNo.substring(0,6);
									String page = instNo.substring(6,10);
											
									TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
									module.setData(0, book);
									module.setData(1, page);
									module.setData(2, "B_P");
									
									ServerResponse response = SearchBy(module, null);
									
								} else{
									String year = String.valueOf(inst.getYear());
	
									TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
									module.setData(0, inst.getInstno());
									module.setData(1, "INST");
									module.setData(2, year);
									
									ServerResponse response = SearchBy(module, null);
								}
								
								if (sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
									SearchLogger.info("\n</div><div><BR>Certification Date found!<BR><hr/></div>\n", searchId);
									break;
								}
							}
						}catch(Exception e) {
							e.printStackTrace();
						}
					}
					
					if (!sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
						SearchLogger.info("\n</div><div><BR>Certification Date still not found!<BR><hr/></div>\n", searchId);
					}
				}
			}
		}
		
		if (dataSite.isRoLikeSite()){
			Set<Integer> additionalInfo = (Set<Integer>) search.getAdditionalInfo(AdditionalInfoKeys.MISSING_LAND_SEARCH_ID_SET);
			if(additionalInfo != null) {
				
				boolean landSearchPerformed = false;
				int[] moduleIds = new int[]{TSServerInfo.SUBDIVISION_MODULE_IDX, TSServerInfo.SECTION_LAND_MODULE_IDX, TSServerInfo.ARB_MODULE_IDX};
				for (int moduleId : moduleIds) {
					if(additionalInfo.contains(moduleId)) {
						landSearchPerformed = true;
						break;
					}
				}
				MissingLandSearchWarning warning = new MissingLandSearchWarning(Warning.MISSING_LAND_SEARCH_ID, getDataSite().getSiteTypeAbrev());
				if(landSearchPerformed) {
					getSearch().getSearchFlags().getWarningList().remove(warning);
				} else {
					getSearch().getSearchFlags().addWarning(warning);
				}
				
				search.removeAdditionalInfo(AdditionalInfoKeys.MISSING_LAND_SEARCH_ID_SET);
				
			} else {
				//no flags, nothing happened
				MissingLandSearchWarning warning = new MissingLandSearchWarning(Warning.MISSING_LAND_SEARCH_ID, getDataSite().getSiteTypeAbrev()); 
				getSearch().getSearchFlags().addWarning(warning);
			}
		}
	}
	
	@Override
	protected Object getRequestCountType(int moduleIDX) {
		switch (moduleIDX) {
		case TSServerInfo.NAME_MODULE_IDX:
		case TSServerInfo.TYPE_NAME_MODULE_IDX:
			return RequestCount.TYPE_NAME_COUNT;
		case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
		case TSServerInfo.INSTR_NO_MODULE_IDX:
			return RequestCount.TYPE_INSTRUMENT_COUNT;
		case TSServerInfo.SUBDIVISION_MODULE_IDX:
		case TSServerInfo.SECTION_LAND_MODULE_IDX:
			return RequestCount.TYPE_LEGAL_COUNT;
		case TSServerInfo.PARCEL_ID_MODULE_IDX:
		case TSServerInfo.MODULE_IDX42:
			return RequestCount.TYPE_PIN_COUNT;
		case TSServerInfo.ARB_MODULE_IDX:
		case TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX:
		case TSServerInfo.MODULE_IDX41:
			return RequestCount.TYPE_MISC_COUNT;
		case TSServerInfo.IMG_MODULE_IDX:
			return RequestCount.TYPE_IMAGE_COUNT;
		
		}
		
		try{
			throw new Exception("Bad module Id for counting request on " + getDataSite().getSTCounty());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	 public ServerResponse GetLink(String vsRequest, boolean vbEncoded)throws ServerResponseException {
		 ServerResponse sr = null;
		 if (vsRequest.contains("look_for_dt_image")) {
			 ImageI i = new Image();
			 i.setContentType("image/tiff");
			 String filename = "" + (new Random()).nextInt(900000000) + ".tiff";
			 i.setFileName(filename);
			 i.setPath(getSearch().getImagesTempDir() + filename);
			 i.setType(IType.TIFF);
			 i.setExtension("tiff");
			 Set<String> links =  new HashSet<String>();
			 links.add(vsRequest.substring(vsRequest.indexOf("look_for_dt_image")));
			 i.setLinks(links);
			 
			 DownloadImageResult result = new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], "");
			 HashMap<String, String> params = HttpUtils.getParamsFromLink(i.getLink(0));
			 String book 	 = StringUtils.defaultString(params.get("book"));
			 String page 	 = StringUtils.defaultString(params.get("page"));
			 String docNumber = StringUtils.defaultString(params.get("instr"));
			 String year 	 = StringUtils.defaultString(params.get("year"));
			 String fake =  StringUtils.defaultString(params.get("isFake"));
			 String dataTreeDocType =  StringUtils.defaultString(params.get("dataTreeDocType"));
			 if ("@@DataTreeDocType@@".equalsIgnoreCase(dataTreeDocType) && "true".equalsIgnoreCase(fake)){
				 Instrument instrument = new Instrument();
				 instrument.setInstno(docNumber);
				 instrument.setBook(book);
				 instrument.setPage(page);
				 try {
					if (StringUtils.isNotEmpty(year)) {
						instrument.setYear(Integer.parseInt(year));
					}
				 } catch (Exception e) {}
				 RegisterDocument doc = null;
				 
				 DocumentsManagerI docma = getSearch().getDocManager();
				 try{
					 docma.getAccess();
					 doc = (RegisterDocument) docma.getDocument(instrument);
				 }finally{
					 docma.releaseAccess();
				 }
				 if (doc != null){	
					 result = lookupForImage(doc);
				 } else{
					 result = saveImage(i);
				 }
			 } else{
				 result = saveImage(i);
			 }
		    	
			
			 
			 if(result != null && Status.OK.equals(result.getStatus())){
				 writeImageToClient(i.getPath(), i.getContentType());
				 return null;
			 }
		 } else {
			 return super.GetLink(vsRequest, vbEncoded);
		 }
		 return sr;
	    }
	 
	 public static String createTempSaveFileName(InstrumentI i, final String SEARCH_DIR, String extension){
			String folderName = SEARCH_DIR + "Register" + File.separator;
			new File(folderName).mkdirs();
			String key = "";
			if (i.hasBookPage()){
				key = i.getBook() + "_" + i.getPage() + "_" + i.getDocType();
			} else if (i.hasInstrNo()){
				key = i.getInstno() + "_" + i.getDocType() + "_" + i.getYear();
			} else if (i.hasDocNo()){
				key = i.getDocno() + "_" + i.getDocType() + "_" + i.getYear();
			} else{
				throw new RuntimeException("-createTempSaveFileName- Please pase a valid Instrument");
			}
	    	return  folderName + key + "." + extension;
	 }
	 
	 @Override
	 protected String CreateSaveToTSDFormHeader(int action, String method) {
		 String s = "<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"" + msRequestSolverName + "\"" + " method=\"" + method + "\" > "
				 + "<input type=\"hidden\" name=\"dispatcher\" value=\""+ action + "\">"
				 + "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\">" 
				 + "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + mSearch.getSearchID() + "\"> "
				 + "<input type=\"hidden\" name=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" id=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" " +
				 			"value=\"" + RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF + "\"> "
				 + "<input type=\"hidden\" name=\"" + RequestParams.PARENT_SITE_SAVE_TYPE_COMBINED + "\" id=\"" + RequestParams.PARENT_SITE_SAVE_TYPE_COMBINED + "\" " +
				 			"value=\"" + RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF + "\">";
	    return s;
	 }
		
	 @Override
	 protected String CreateSaveToTSDFormEnd(String name, int parserId, int numberOfUnsavedRows){
		 if (name == null){
			 name = SAVE_DOCUMENT_BUTTON_LABEL;
		 }
	    	        
		 String s = "";
	        
		 if (numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0){
			 s = "<input  type=\"checkbox\" title=\"Save selected document(s) with cross-references\" " +
					 " onclick=\"javascript: if(document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "'))\r\n " +
					 " if(this.checked) { " +
					 " document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "').value='" + 
					 			RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITH_CROSSREF +
					 "' } else { " +
	 	        	 " document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "').value='" + 
	 	        	 			RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF +
		        	 "' } \"> Save with cross-references<br>\r\n" +
		        	 "<input type=\"checkbox\" name=\"" + RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS + 
		        	 			"\" id=\"" + RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS + 
		        			    "\" title=\"Save search parameters from selected document(s) for further use\" > Save with search parameters<br>\r\n" + 
	        		 "<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" " +"onclick=\"javascript:submitForm();\" >\r\n";
		 }
		 return s + "</form>\n";
	 }
	 
	 @Override
	 public String getSaveSearchParametersButton(ServerResponse response){
		 if (response == null || response.getParsedResponse() == null){
			 return null;
		 }
			
		 Object possibleModule = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
			
		 if (!(possibleModule instanceof TSServerInfoModule)){
			 return null;
		 }
			
		 Search search = getSearch();
		 int moduleIdx = ((TSServerInfoModule)possibleModule).getModuleIdx();
			
		 if(moduleIdx == TSServerInfo.SUBDIVISION_MODULE_IDX || moduleIdx == TSServerInfo.NAME_MODULE_IDX) {
			
			 String key = "SSP_" + System.currentTimeMillis();
				
			 /**
			  * Store this for future use (do not worry, it will not be saved)
			  */
			 search.setAdditionalInfo(key, possibleModule);
			 return "<input type=\"button\" name=\"ButtonSSP\" value=\"Save Search Parameters\" onClick=\"saveSearchedParametersAJAX('" + 
			 key + "','" + getServerID() + "')\" class=\"button\" title=\"Save Last Searched Parameters\">";
		 } else{
			return null;
		}
	}
	 
	 @Override
	 protected NameI getNameFromModule(TSServerInfoModule module){
		 NameI name = new Name();
		 if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 1){
			 String first = module.getFunction(2).getParamValue();
			 String middle = module.getFunction(3).getParamValue();
			 String last = module.getFunction(4).getParamValue();
			 
			 if (StringUtils.isEmpty(last)){
				 return null;
			 }

			 name.setLastName(last);
			 name.setFirstName(first);
			 name.setMiddleName(middle);
			 return name;
		 }
		 
		 return null;
	}
		
	 @Override
	 protected LegalI getLegalFromModule(TSServerInfoModule module){
		 LegalI legal = null;
		 SubdivisionI subdivision = null;
		 TownShipI townShip = null;
			
		 if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX && module.getFunctionCount() > 12){
			 subdivision = new Subdivision();
				
			 String subdivisionName = module.getFunction(12).getParamValue().trim();
			 subdivision.setName(subdivisionName);
			 subdivision.setLot(module.getFunction(2).getParamValue().trim());
			 subdivision.setBlock(module.getFunction(3).getParamValue().trim());
			 subdivision.setPlatBook(module.getFunction(4).getParamValue().trim());
			 subdivision.setPlatPage(module.getFunction(5).getParamValue().trim());
		 }
		 if (module.getModuleIdx() == TSServerInfo.SECTION_LAND_MODULE_IDX && module.getFunctionCount() > 3){
			 townShip = new TownShip();
				
			 townShip.setSection(module.getFunction(0).getParamValue().trim());
			 townShip.setTownship(module.getFunction(1).getParamValue().trim());
			 townShip.setRange(module.getFunction(2).getParamValue().trim());
			
		 }
		 if (subdivision != null){
			 legal = new Legal();
			 legal.setSubdivision(subdivision);
		 }
		 
		 if (townShip != null){
			 if (legal == null){
				 legal = new Legal();
			 }
			 legal.setTownShip(townShip);
		 }
		 
		 return legal;
	}
	 
	 /**
	  * @param module
	  * @param sd
	  * @param global
	  * @return
	  * @throws ServerResponseException
	  */
	 public ServerResponse findImageAndCreateFakeDocument(TSServerInfoModule module, Object sd, Search global) throws ServerResponseException {
		 DataTreeAccount acc = DataTreeManager.getDatatTreeAccount(String.valueOf(getCommunityId()));
		 String bookF = module.getParamValue(0);
		 String pageF = module.getParamValue(1);
		 String docNo = module.getParamValue(2);
		 String year = module.getParamValue(3);
		 String docType = StringUtils.defaultIfEmpty(module.getParamValue(4), "");
		 String dataTreeType = module.getParamValue(5);
		 String month = "";
		 String day = "";
		 
		 if (StringUtils.isEmpty(bookF) && StringUtils.isEmpty(pageF) && StringUtils.isEmpty(docNo)){
			 return new ServerResponse();
		 }	
		
		 String recDate = "";
		 if (year.matches("\\d{2}/\\d{2}/\\d{4}")){
			 recDate = year;
			 month = recDate.substring(0, recDate.indexOf("/"));
			 day = recDate.substring(recDate.indexOf("/") + 1, recDate.lastIndexOf("/"));
			 year = year.substring(year.lastIndexOf("/") + 1);
		 } else{
			 if (year.length() > 4 && year.matches("\\d{1,2}\\d{2}\\d{4}")){
				 if (year.length() == 7){
					 if (StringUtils.isEmpty(month)){
						 month = year.substring(0, 1);
					 }
					 if (StringUtils.isEmpty(day)){
						 day = year.substring(1, 3);
					 }
				 } else if (year.length() == 8){
					 if (CountyConstants.NV_Clark == dataSite.getCountyId()){
						 if (StringUtils.isEmpty(month)){
							 month = year.substring(4, 6);
						 }
						 if (StringUtils.isEmpty(day)){
							 day = year.substring(6, 8);
						 }
					 } else{
						 if (StringUtils.isEmpty(month)){
							 month = year.substring(0, 2);
						 }
						 if (StringUtils.isEmpty(day)){
							 day = year.substring(2, 4);
						 }
					 }
				 }
				 if (CountyConstants.NV_Clark == dataSite.getCountyId()){
					 year = year.substring(0, 4);
				 } else{
					 year = year.substring(year.length() - 4);
				 }
				 recDate = StringUtils.leftPad(month, 2, "0") + "/" + StringUtils.leftPad(day, 2, "0") + "/" + year;
			 
			 } else if (year.length() == 6) { //task 9403
				 if (CountyConstants.NV_Clark == dataSite.getCountyId()) { 
					 if (StringUtils.isEmpty(day) && StringUtils.isEmpty(month)) {
						 month = year.substring(2, 4);
						 day = year.substring(4, 6);
						 year = year.substring(0, 2);
						 if (year.matches("(?is)\\A0\\d+") || year.matches("(?is)\\A1\\d+") || year.matches("(?is)\\A2\\d+")){
							 year = "20" + year;
						 } else{
							 year = "19" + year;
						 }
					 }
				 }
			 }
		 }
		 int yearI = SimpleChapterUtils.UNDEFINED_YEAR; 
		 try{yearI = Integer.parseInt(year);}catch(Exception e){}
		 
		 if (yearI == SimpleChapterUtils.UNDEFINED_YEAR){
			 if (CountyConstants.CA_San_Mateo == dataSite.getCountyId()){
				 if (docNo.length() == 7){
					 year = docNo.substring(0, 1);
					 year = "200" + year;
					 try{yearI = Integer.parseInt(year);}catch(Exception e){}
				 }
				 if (docNo.length() == 8){
					 year = docNo.substring(0, 2);
					 if (year.matches("(?is)\\A0\\d+") || year.matches("(?is)\\A1\\d+") || year.matches("(?is)\\A2\\d+")){
						 year = "20" + year;
					 } else{
						 year = "19" + year;
					 }
					 try{yearI = Integer.parseInt(year);}catch(Exception e){}
				 }
			 }
		 }
				
		 InstrumentI i = new Instrument(bookF, pageF, docType, "", yearI);
		 i.setInstno(docNo);
		 i.setDocType(docType);
		 if (StringUtils.isNotEmpty(recDate)){
			 i.setDate(FormatDate.getDateFromFormatedString(recDate, FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY));
		 }
		 if (yearI != SimpleChapterUtils.UNDEFINED_YEAR){
				i.setYear(yearI);
		 }
		
		 HashMap<String, String> docItems = new HashMap<String, String>();
		 
		 String countyFips = "";
		 String indexType = "";
		 String dataTreeDocType = "";
		 int dataTreeId = 0;
		 
		 boolean isAssesorMap = "ASSESSOR_MAP".equalsIgnoreCase(dataTreeType);
		 boolean isFL = StateContants.FL_STRING_FIPS.equals(getDataSite().getStateFIPS());
		 String key = (bookF == null ? "" : bookF) + "-" + (pageF == null ? "" : pageF) + "-" + (docNo == null ? "" : docNo);
		 
		 HashSet<String> searchedImages = (HashSet<String>)global.getAdditionalInfo("SEARCHED_IMAGES");
				
		 if (searchedImages == null){
			 searchedImages = new HashSet<String>();
			 global.setAdditionalInfo("SEARCHED_IMAGES", searchedImages);
		 }
	
		 boolean imageDownloaded = false;
		 if (!isParentSite() && searchedImages.contains(key)){
			 return new ServerResponse();
		 }
	
		 List<DataTreeStruct> searched = new ArrayList<DataTreeStruct>();
				
		 if (DocumentTypes.PLAT.equals(docType)){	
			 String fileName = FLSubdividedBasedDASLDT.createTempSaveFileName(i, getCrtSearchDir());
			 
			 logSearchBy(module);
					
			 global.setAdditionalInfo("IMAGE_FAKE_DG", fileName);
					
			 if (datTreeList == null){
				 datTreeList = initDataTreeStruct();
			 }
	
			 for (DataTreeStruct  temp : datTreeList){
				 if (dataTreeType.equalsIgnoreCase(temp.getDataTreeDocType())){
					 searched.add(temp);
				 }
			 }
			 boolean imageDownloadedFromDataTree = false;
				 
			 List<DataTreeImageException> exceptions = new ArrayList<DataTreeImageException>();
				    	
			 for (DataTreeStruct struct : searched){
				 try {
					 //count request on datatree
					 mSearch.countRequest(getDataSite().getSiteTypeInt(), RequestCount.TYPE_MISC_COUNT);
								
					 if (DataTreeManager.downloadImageFromDataTree(acc, struct, i, fileName, null, null)){
						 imageDownloadedFromDataTree = true;
						 imageDownloaded = true;
						 int type = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID).getCityCheckedInt();
						 afterDownloadImage(imageDownloadedFromDataTree,type);
								
						 dataTreeId = struct.getDataTreeId();
						 indexType = struct.getIndexType();
						 countyFips = struct.getCountyFips();
						 dataTreeDocType = struct.getDataTreeDocType();
						 break;
					 }
				 } catch (DataTreeImageException e) {
					 exceptions.add(e);
				 }
			 }
			 if (!imageDownloadedFromDataTree && searched.size() == exceptions.size() && !exceptions.isEmpty()) {
				 DataTreeConn.logDataTreeImageException(i, searchId, exceptions, true);
			 }
		 } else{
			 logSearchBy(module);
			 Instrument instr = (Instrument) i.clone();
			 if (StringUtils.isNotEmpty(docType)){
				 instr.setDocType(DocumentTypes.getDocumentCategory(docType, searchId));
				 instr.setDocSubType(DocumentTypes.getDocumentSubcategory(docType, searchId));
			 }
			 RegisterDocument document = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr));
			 document.setInstrument(instr);
			 document.setDataSource(dataSite.getSiteTypeAbrev());
			 DownloadImageResult result = lookupForImage(document);
			 if (DownloadImageResult.Status.OK == result.getStatus()){
				 imageDownloaded = true;
			 }
			 if (imageDownloaded){
				 docItems = makeInstrumentSearchOnOtherServer(i);
			 }
		 }
			 
		 if (imageDownloaded){
			 if (DocumentTypes.PLAT.equals(docType)){
				 SearchLogger.info("<b><font color=\"red\">Image " + i.prettyPrint() + " Downloaded from Data Tree</font></b></br>", searchId);
			 }
					
			 if (!isParentSite()){
				 searchedImages.add(key);
			 }
				 
			 String grantor  = "County of " + InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			 grantor = StringUtils.defaultString(grantor);
					 
			 String grantee = mSearch.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME);
			 grantee = StringUtils.defaultString(grantee);
					 
			 grantee = ro.cst.tsearch.utils.StringUtils.HTMLEntityEncode(grantee);
			 grantor = ro.cst.tsearch.utils.StringUtils.HTMLEntityEncode(grantor);
					 
			 if (!DocumentTypes.PLAT.equalsIgnoreCase(docType)){
				 grantee = "";
				 grantor = "";
			 }
			 String remarks = "";
			 if (docItems.size() > 0){
				 if (!DocumentTypes.PLAT.equalsIgnoreCase(docType)){
					 grantee = StringEscapeUtils.escapeHtml(docItems.get(SaleDataSetKey.GRANTEE.getShortKeyName()));
					 grantor = StringEscapeUtils.escapeHtml(docItems.get(SaleDataSetKey.GRANTOR.getShortKeyName()));
				 }
				 if (StringUtils.isNotEmpty(docItems.get(SaleDataSetKey.RECORDED_DATE.getShortKeyName()))){
					 recDate = docItems.get(SaleDataSetKey.RECORDED_DATE.getShortKeyName());
					 if (recDate.matches("\\d{1,2}/\\d{2}/\\d{4}")){
						 String tempRecDate = recDate.replaceAll("/", "");
						 if (tempRecDate.length() == 7){
							 if (StringUtils.isEmpty(month)){
								 month = tempRecDate.substring(0, 1);
							 }
							 if (StringUtils.isEmpty(day)){
								 day = tempRecDate.substring(1, 3);
							 }	
							 if (StringUtils.isEmpty(year)){
								 year = tempRecDate.substring(4);
							 }
						 } else if (tempRecDate.length() == 8){
							 if (StringUtils.isEmpty(month)){
								 month = tempRecDate.substring(0, 2);
							 }	
							 if (StringUtils.isEmpty(day)){
								 day = tempRecDate.substring(2, 4);
							 }
							 if (StringUtils.isEmpty(year)){
								 year = tempRecDate.substring(4);
							 }
						 }
					 }
				 }
				 docType = docItems.get(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName());
				 bookF = docItems.get(SaleDataSetKey.BOOK.getShortKeyName());
				 pageF = docItems.get(SaleDataSetKey.PAGE.getShortKeyName());
//				 docNo = docItems.get(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName());
				 remarks = docItems.get(SaleDataSetKey.REMARKS.getShortKeyName());
				 remarks = StringEscapeUtils.escapeHtml(remarks);
			 }
				
			 String doc = DTG_FAKE_RESPONSE.replace("@@Grantee@@", grantee);
			 doc = doc.replace("@@Grantor@@", grantor);
			
			 String fromDateDD = StringUtils.defaultIfBlank(getSearch().getSa().getAtribute(SearchAttributes.FROMDATE_DD), "01");
			 String fromDateMM = StringUtils.defaultIfBlank(getSearch().getSa().getAtribute(SearchAttributes.FROMDATE_MM), "01");
			 String fromDateYYYY = StringUtils.defaultIfBlank(getSearch().getSa().getAtribute(SearchAttributes.FROMDATE_YEAR), "1960");
			 String fromDate = StringUtils.defaultIfBlank(getSearch().getSa().getAtribute(SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY), "01/01/1960");
			 
			 if (StringUtils.isNotEmpty(recDate)){
				 doc = doc.replace("@@Date@@", StringUtils.defaultString(recDate));
			 } else if (StringUtils.isNotEmpty(year) && year.matches("\\d{4}")){
				 doc = doc.replace("@@Date@@", fromDateMM + "/" + fromDateDD + "/" + year);
			 } else{
				 doc = doc.replace("@@Date@@", fromDate);
			 }
			 if (StringUtils.isNotEmpty(year)){
				 doc = doc.replace("@@Year@@", year);
			 } else{
				 doc = doc.replace("@@Year@@", fromDateYYYY);
			 }
			 if (StringUtils.isNotEmpty(month)){
				 doc = doc.replace("@@Month@@", month);
			 } else{
				 doc = doc.replace("@@Month@@", fromDateMM);
			 }
			 if (StringUtils.isNotEmpty(day)){
				 doc = doc.replace("@@Day@@", day);
			 } else{
				 doc = doc.replace("@@Day@@", fromDateDD);
			 }
			 
			 if (StringUtils.isNotEmpty(docType)){
				 if (DocumentTypes.PLAT.equals(docType) && (isFL || isAssesorMap) && StringUtils.isNotEmpty(dataTreeDocType)){
					 doc = doc.replace("@@Type@@-@@Type@@", dataTreeDocType);
				 } else{
					 doc = doc.replace("@@Type@@-@@Type@@", docType);
				 }
			 } else{
				 doc = doc.replace("@@Type@@-@@Type@@", dataTreeDocType);
			 }
				 
			 if (StringUtils.isNotEmpty(indexType)){
				 if ("Book.Page.Alpha".equalsIgnoreCase(indexType) || "Book.Page".equalsIgnoreCase(indexType)
							|| "Book.Alphanum".equalsIgnoreCase(indexType) || "Alphanum.Page".equalsIgnoreCase(indexType)
							|| "BookAlpha.Page".equalsIgnoreCase(indexType)){
					 if (dataTreeId > 0){
						 String dataTreeIdS = Integer.toString(dataTreeId);
						 doc = doc.replace("@@DataTreeIdBook@@", dataTreeIdS);
					 }
				 } else if ("Year.DocID".equals(indexType)){
					 if (dataTreeId > 0){
						 String dataTreeIdSd = Integer.toString(dataTreeId);
						 doc = doc.replace("@@DataTreeIdDoc@@", dataTreeIdSd);
					 }
				 }
			 }
			 if (StringUtils.isNotEmpty(dataTreeDocType)){
				 doc = doc.replace("@@DataTreeDocType@@", dataTreeDocType);
			 }
			 doc = doc.replace("@@CountyFips@@", countyFips);
			 if (StringUtils.isNotEmpty(remarks)){
				 doc = doc.replaceFirst("(?is)\\bSEE DOCUMENT\\b", remarks);
			 }
						
			 if (isAssesorMap && isFL){
				 doc = doc.replace("@@Book@@", "");
				 doc = doc.replace("@@Page@@", "");
				 doc = doc.replace("@@DocNo@@", key);
//			     doc = doc.replace("@@Year@@", StringUtils.defaultString(year));
			 } else{
				 doc = doc.replace("@@Book@@", StringUtils.defaultString(bookF));
				 doc = doc.replace("@@Page@@", StringUtils.defaultString(pageF));
				 doc = doc.replace("@@DocNo@@", StringUtils.defaultString(docNo));
//				 doc = doc.replace("@@Year@@", StringUtils.defaultString(year));
			 }
				 
			 module.setParserID(ID_SEARCH_BY_BOOK_AND_PAGE);
			 return searchBy(module, sd, doc);
			 } else{
				 SearchLogger.info("<b><font color=\"red\">Could not download image " + i.prettyPrint() + " from Data Tree</font></b>", searchId);
				 ServerResponse sr = new ServerResponse();
				 ParsedResponse pr = new ParsedResponse();
				 sr.setParsedResponse(pr);
				 sr.setResult("<b>Could not download image</b>");
				 solveHtmlResponse(module.getModuleIdx()+"", module.getParserID(), "SearchBy", sr, sr.getResult());
				 return sr;
			 }
		 }
		
	 	protected ServerResponse imageSearch(TSServerInfoModule module) throws ServerResponseException{
	    	
			Map<String, String> params = getNonEmptyParams(module, null);
			return imageSearch(params);
		}
			
		public ServerResponse imageSearch(Map<String, String> params) throws ServerResponseException{
			
			ServerResponse sr = new ServerResponse();
			
			String book = StringUtils.defaultString(params.get("book"));
			String page = StringUtils.defaultString(params.get("page"));
			String instr = StringUtils.defaultString(params.get("docno"));
			if (StringUtils.isEmpty(instr)){
				instr = StringUtils.defaultString(params.get("instr"));
			}
			String type = StringUtils.defaultString(params.get("type"));
			String year = StringUtils.defaultString(params.get("year"));
			
			InstrumentI i = new Instrument();

			int yearInt = -1;
	    	if (StringUtils.isNotEmpty(year)){
	    		try {
					yearInt = Integer.parseInt(year);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
	    	}
			i.setBook(book);
			i.setPage(page);
			i.setInstno(instr);
			i.setYear(yearInt);
			i.setDocType(type);
			SearchLogger.info("<br/>Image Look Up for book=" + i.getBook() + " page=" + i.getPage() + "; inst=" + i.getInstno() 
									+ " year=" + year + " using Image Look Up Search module",searchId);
			RegisterDocument doc = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, i));
			doc.setInstrument(i);
			
			DownloadImageResult imageResult = lookupForImage(doc, doc.getId());
			sr.setImageResult(imageResult);
			
			return sr;
			
	    }
		
	 @Override
	 public DownloadImageResult lookupForImage(DocumentI doc, String documentId) throws ServerResponseException{	
		 
		 if (doc != null){
			 InstrumentI i = doc.getInstrument();
			 getSearch();
			 ImageI image = doc.getImage();
			 boolean docWithoutImage = false;
			 if (image == null){
				 docWithoutImage = true;
				 image = new Image();
					
				 String imageExtension = "tiff";
				 if (StringUtils.isNotEmpty(image.getExtension())){
					 imageExtension = image.getExtension();
				 }
					
				 String imageDirectory = getSearch().getImageDirectory();
				 ro.cst.tsearch.utils.FileUtils.CreateOutputDir(imageDirectory);
				 String fileName = doc.getId() + "." + imageExtension;
				 String path = imageDirectory + File.separator + fileName;
				 if (StringUtils.isEmpty(image.getPath())){
					 image.setPath(path);
				 }
				 image.setFileName(fileName);
				 image.setContentType("IMAGE/TIFF");
				 image.setExtension("tiff");
				 image.setType(IType.TIFF);
				 doc.setImage(image);
				 doc.setIncludeImage(true);
			 }
			 String imageLink = CreatePartialLink(TSConnectionURL.idGET) + "look_for_dt_image&id=&description=&instr=" + i.getInstno() 
					 + "&book=" + i.getBook() + "&page=" + i.getPage()  + "&year=" + i.getYear();
			 if (i.getDate() != null){
				 String date = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(i.getDate());
				 if (StringUtils.isNotEmpty(date)){
					 String month = date.substring(0, 2);
					 if (StringUtils.isNotEmpty(month)){
						 imageLink += "&month=" + month; 
					 }
					 String day = date.substring(date.indexOf("/") + 1, date.indexOf("/") + 3);
					 if (StringUtils.isNotEmpty(day)){
						 imageLink += "&day=" + day; 
					 }
				 }
			 }
			 Set<String> links = image.getLinks();
			 if (links.size() == 0){
				 links.add(imageLink);
				 image.setLinks(links);
			 }
				
			 DownloadImageResult dldImageResult = downloadImage(image, doc.getId(), doc);
			 if (dldImageResult.getStatus().equals(DownloadImageResult.Status.ERROR) && docWithoutImage){
				 doc.setImage(null);
			 }
			  
			 return dldImageResult;
		 }
			
		 return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], "");
	 }
	 
	 @Override
		public void addDocumentAdditionalPostProcessing(DocumentI doc, ServerResponse response){
			super.addDocumentAdditionalPostProcessing(doc, response);

			DocumentsManagerI manager = getSearch().getDocManager();
			try {
				manager.getAccess();
				if (manager.contains(doc)) {
					if (doc instanceof RegisterDocumentI){
						RegisterDocumentI regDoc = (RegisterDocumentI)doc;
						if (regDoc.isOneOf(DocumentTypes.TRANSFER) && SearchType.GI == regDoc.getSearchType()){
							regDoc.setChecked(false);
							regDoc.setIncludeImage(false);
						}
							if (!regDoc.hasImage() && regDoc.isNotOneOf(DocumentTypes.COURT)){
								StringBuilder toLog = new StringBuilder();
								try {
									TSInterface tsi = TSServersFactory.GetServerInstance((int)doc.getSiteId(), searchId);
									DownloadImageResult res = tsi.lookupForImage(doc);
									if (res.getStatus() != DownloadImageResult.Status.OK){
										if (doc.getImage() != null){
											doc.getImage().setSaved(false);
										}
										toLog.append("<br>Image of document with following instrument number was not successfully retrieved: ")
												.append(doc.prettyPrint());
									} else{
										doc.getImage().setSaved(true);
										toLog.append("<br>Image of document with following instrument number was successfully retrieved: <a href='")
											.append(doc.getImage().getSsfLink())
											.append("'>")
											.append(doc.prettyPrint())
											.append("</a>");
									}
								} catch (Exception e){
									doc.getImage().setSaved(false);
									toLog.append("<br>Image of document with following instrument number was not successfully retrieved:")
											.append(doc.prettyPrint());
									doc.setImage(null);
									logger.error("performAdditionalProcessingAfterRunningAutomatic", e);
								}
								if (toLog.length() > 0){
									toLog.append("<br>");
									SearchLogger.info(toLog.toString(), searchId);
								}
							}
					}
				}
			} catch (Throwable t) {
				logger.error("Error while post processing document", t);
			} finally {
				manager.releaseAccess();
			}
		}
	 
	 public HashMap<String, String> makeInstrumentSearchOnOtherServer(InstrumentI instrument){
		 return new HashMap<String, String>();
	 }
	 
	 
	 ///upgrade of DG implementation. started with OH
	 public void preparePersonalStructForCounty(LegalStructDTG str, long searchId){
		 if (StateContants.FL_STRING_FIPS.equals(dataSite.getStateFIPS())){
			 boolean testSpecialLotAndBlock = (dataSite.getCountyId() == CountyConstants.FL_Orange) 
					 							|| (dataSite.getCountyId() == CountyConstants.FL_Lake) 
					 							|| (dataSite.getCountyId() == CountyConstants.FL_Osceola)
					 							|| (dataSite.getCountyId() == CountyConstants.FL_Seminole);
			 if (testSpecialLotAndBlock){
				 if (!StringUtils.isEmpty(str.getLot()) && !str.getLot().startsWith("L") && !str.getLot().startsWith("U")){
					 if (!(str.getLot().length() >= 2 && org.apache.commons.lang.StringUtils.isAlpha(str.getLot().substring(0,1)))){
						 str.setLot("L" + str.getLot());			
					 }
				 }
				 if (!StringUtils.isEmpty(str.getBlock())){
					 if (!(str.getBlock().length() >= 2 && org.apache.commons.lang.StringUtils.isAlpha(str.getBlock().substring(0,1)))){
						 str.setBlock("B" + str.getBlock());			
					}
				}
			}
		}
	 }
	 
	 protected Set<LegalStructDTG> keepOnlyGoodLegal(Set<LegalStructDTG> legals){
		 Set<LegalStructDTG> good = new HashSet<LegalStructDTG>();
		 for (LegalStructDTG str : legals){
			 if (!incompleteData(str)){
				 good.add(str);
			 }
		 }
		 return good;
	 }
		
	 protected boolean incompleteData(LegalStructDTG str) {
		 if (str == null){
			 return true;
		 }
			
		 boolean emptyLot = StringUtils.isEmpty(str.getLot());
		 boolean emptyBlock = StringUtils.isEmpty(str.getBlock());
		 boolean emptyPlatInst = StringUtils.isEmpty(str.getPlatInst());
		 boolean emptyPlatBook = StringUtils.isEmpty(str.getPlatBook());
		 boolean emptyPlatPage = StringUtils.isEmpty(str.getPlatPage());
			
		 boolean emptySection = StringUtils.isEmpty(str.getSection());
		 boolean emptyRange = StringUtils.isEmpty(str.getRange());
		 boolean emptyTownship = StringUtils.isEmpty(str.getTownship());
			
		 boolean emptyArblot = StringUtils.isEmpty(str.getArbLot());
		 boolean emptyArbBlock = StringUtils.isEmpty(str.getArbBlock());
		 boolean emptyArbBook = StringUtils.isEmpty(str.getArbBook());
		 boolean emptyArbPage = StringUtils.isEmpty(str.getArbPage());
			
		 boolean emptyNcbNumber = org.apache.commons.lang.StringUtils.isEmpty(str.getNcbNumber());
		 boolean emptySubdivisionName = org.apache.commons.lang.StringUtils.isEmpty(str.getSubdivisionName());
		 boolean emptyTract = StringUtils.isEmpty(str.getTract());
			
		 if (SECTIONAL_TYPE.equalsIgnoreCase(str.getType()) || ARB_TYPE.equalsIgnoreCase(str.getType())){
			 if (!emptySection){
				 return (emptySection || emptyTownship || emptyRange);
			}else{
				return (  (emptyArbBook || emptyArbPage) || (emptyArblot && emptyArbBlock) );
			}
		} else if(SUBDIVIDED_TYPE.equalsIgnoreCase(str.getType())){
			if (!emptyNcbNumber){
				return (emptyNcbNumber && emptySubdivisionName);
			} else if (!emptyTract){
				return emptyTract;
			} else{
				return ((emptyPlatBook || emptyPlatPage) && emptyPlatInst) || (emptyBlock && emptyLot);
			}
		}
			
		 return true;
	 }
		
	 protected Set<LegalStructDTG> expandLegalStructItemsSublot(Set<LegalStructDTG> legalStructList, long searchId) {
		 Set<LegalStructDTG> res = new HashSet<LegalStructDTG>();
		 for (LegalStructDTG pds : legalStructList){
			 if (StringUtils.isNotEmpty(pds.getSubLot())){
				 LegalStructDTG p;
				 try {
					 p = (LegalStructDTG) pds.clone();
					 p.setSubLot("");
					 res.add(p);
				 } catch (Exception e){
					 e.printStackTrace();
				 }
			 }
			 res.add(pds);
		 }
		 res = keepOnlyGoodLegal(res);
			
		 return res;
	 }
	 
	 protected void addLegalStructItemsUsingAoAndTrLots(Set<LegalStructDTG> legalStructList, String[] allAoAndTrlots,long searchId, boolean isDTG) {
		 LegalStructDTG first = getFirstPlatedStruct(legalStructList);
		 if (first != null){
			 StringBuilder sb = new StringBuilder();
			 HashSet<String> newAllAoAndTrlots = new HashSet<String>();
			 for (String lot : allAoAndTrlots) {
				 if (lot.matches("\\d+")) {
					 sb.append(lot).append(" ");
					}	 else {
						newAllAoAndTrlots.add(lot);
					}
				}
				String lots = LegalDescription.cleanValues(sb.toString(), false, true);
				if (org.apache.commons.lang.StringUtils.isNotEmpty(lots)){
					for (LotInterval interval: LotMatchAlgorithm.prepareLotInterval(lots)) {
						int lot = interval.getLow();
						int lotThru = interval.getHigh();
						LegalStructDTG n = null;
						try {
							n = (LegalStructDTG) first.clone();
						} catch (CloneNotSupportedException e) {
							e.printStackTrace();
						}
						if (n != null){
							n.setLot(Integer.toString(lot));
							if (lot != lotThru) {
								n.setLotThru(Integer.toString(lotThru));
							}	
							addLegalStructAtInterval(legalStructList, n);
						}
					}
				}
				allAoAndTrlots = newAllAoAndTrlots.toArray(new String[newAllAoAndTrlots.size()]);
				for (String lot : allAoAndTrlots){
					LegalStructDTG n = null;
					try {
						n = (LegalStructDTG) first.clone();
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
					if (n != null){
						n.setLot(lot);
						if (!testIfExist(legalStructList, n, searchId)){
							legalStructList.add(n);
						}
					}
				}
		 	}
		}
		
		/** 
		 * add an item extending lot interval of another item, if possible
		 * if not possible, add the new item separately
		 **/
		protected void addLegalStructAtInterval(Set<LegalStructDTG> list, LegalStructDTG newItem) {
			
			if (list.size()==0) {
				list.add(newItem);
				return;
			}
			
			String newItemLot = newItem.getLot();
			String newItemLotThrough = newItem.getLotThru();
			if (newItemLotThrough.equals("")) {
				newItemLotThrough = newItemLot;
			}
			
			if (!newItemLot.matches("\\d+") || !newItemLotThrough.matches("\\d+")) {
				list.add(newItem);
				return;
			}
			
			boolean found = false;
			for (LegalStructDTG current : list) {
				String currentItemLot = current.getLot();
				String curentItemLotThrough = current.getLotThru();
				if (curentItemLotThrough.equals("")) {
					curentItemLotThrough = currentItemLot;
				}
				if (equalExceptLot(current, newItem) && currentItemLot.matches("\\d+") && curentItemLotThrough.matches("\\d+")) {
					String concatenated =  newItemLot + "-" + newItemLotThrough + " " +
										   currentItemLot + "-" + curentItemLotThrough;
					Vector<LotInterval> intervals = LotMatchAlgorithm.prepareLotInterval(concatenated);
					if (intervals.size() == 1) {	//there is only an interval
						found = true;
						LotInterval lotInterval = intervals.elementAt(0);
						int low = lotInterval.getLow();
						int high = lotInterval.getHigh();
						current.setLot(Integer.toString(low));
						if (low != high) {
							current.setLotThru(Integer.toString(high));
						}
						break;	
					} 
				}
			}
			if (!found) {
				list.add(newItem);
			}
			
		}
		
		public boolean equalExceptLot(LegalStructDTG p1, LegalStructDTG p2) {
			return ((p1.getType()).equals(p2.getType()) &&
					 (p1.getBlock()).equals(p2.getBlock()) &&
					 (p1.getUnit()).equals(p2.getUnit()) &&
					 (p1.getPlatBook()).equals(p2.getPlatBook()) &&
					 (p1.getPlatPage()).equals(p2.getPlatPage()) &&
					 (p1.getPlatInst()).equals(p2.getPlatInst()) &&
					 (p1.getSection()).equals(p2.getSection()) &&
					 (p1.getTownship()).equals(p2.getTownship()) &&
					 (p1.getRange()).equals(p2.getRange()) &&
					 (p1.getArb()).equals(p2.getArb()) &&
					 (p1.getArbLot()).equals(p2.getArbLot()) &&
					 (p1.getArbBlock()).equals(p2.getArbBlock()) &&
					 (p1.getArbBook()).equals(p2.getArbBook()) &&
					 (p1.getArbPage()).equals(p2.getArbPage()) &&
					 (p1.getQuarterOrder()).equals(p2.getQuarterOrder()) &&
					 (p1.getQuarterValue()).equals(p2.getQuarterValue()) &&
					 (p1.getSubLot()).equals(p2.getSubLot()) &&
					 (p1.getPlatInstrYear()).equals(p2.getPlatInstrYear())); 
		}
		
		private LegalStructDTG getFirstPlatedStruct(Set<LegalStructDTG> list){
			for (LegalStructDTG struct : list){
				if (struct.isPlated()){
					return struct;
				}
			}
			return null;
		}
		
		private LegalStructDTG getFirstArbExtendedStruct(Set<LegalStructDTG> list){
			for (LegalStructDTG struct : list){
				if (struct.isArbExtended()){
					return struct;
				}
			}
			return null;
		}
		
		private LegalStructDTG getFirstSectionalStruct(Set<LegalStructDTG> list){
			for (LegalStructDTG struct : list){
				if (!struct.isPlated()){
					return struct;
				}
			}
			return null;
		}
		
		protected boolean isPlatedMultyLotAndIsSectionalMultyQV(Set<LegalStructDTG> legalStructList){
			Set<LegalStructDTG>  list1 = new HashSet<LegalStructDTG>();
			Set<LegalStructDTG> list2 = new HashSet<LegalStructDTG>();
			
			for (LegalStructDTG p : legalStructList){
				if (p.isPlated()){
					list1.add(p);
				} else if(p.isSectional() || p.isArb()){
					list2.add(p);
				}
			}
			
			return isPlatedMultyLot(list1) && isSectionalMultyQv(list2);
		}
		
		protected boolean isSectionalMultyQv(Set<LegalStructDTG> legalStructList){
			boolean isSectionalMultyQv = true;
			
			if (legalStructList == null || legalStructList.size() == 0){
				isSectionalMultyQv = false;
			}
			
			for (LegalStructDTG p:legalStructList){
				if (p.isPlated() && !p.isSectional()){
					isSectionalMultyQv = false;
					break;
				}
			}
			
			if (isSectionalMultyQv){
				LegalStructDTG first =  getFirstSectionalStruct(legalStructList);
				
				if (first == null){
					isSectionalMultyQv =  false;
				} else{
					for (LegalStructDTG p : legalStructList){
						if (!p.getSection().equalsIgnoreCase(first.getSection()) 
								|| !p.getTownship().equalsIgnoreCase(first.getTownship())
								|| !p.getRange().equalsIgnoreCase(first.getRange())
								|| !p.getQuarterOrder().equalsIgnoreCase(first.getQuarterOrder())){
							isSectionalMultyQv =  false;
							break;
						}
					}
				}
			}
			
			return isSectionalMultyQv;
		}
		
		protected boolean isArbExtended(Set<LegalStructDTG> legalStructList){
			boolean isArbExtended = true;
			
			if (legalStructList == null || legalStructList.size() == 0){
				isArbExtended = false;
			}
			
			for (LegalStructDTG p : legalStructList){
				if (!p.isArbExtended() && p.isPlated()){
					isArbExtended =  false;
					break;
				}
			}
			
			if (isArbExtended){
				LegalStructDTG first = getFirstArbExtendedStruct(legalStructList);
				
				if (first == null){
					isArbExtended =  false;
				} else{
					for (LegalStructDTG p : legalStructList){
						if ((!p.getArbBlock().equalsIgnoreCase(first.getArbBlock())
									&& StringUtils.isEmpty(first.getArbBlock())&&StringUtils.isEmpty(p.getArbBlock()))
								|| !p.getArbBook().equalsIgnoreCase(first.getArbBook()) 
								|| !p.getArbPage().equalsIgnoreCase(first.getArbPage())){
							isArbExtended =  false;
							break;
						}
					}
				}
			}
			return isArbExtended ;
		}
		
		protected boolean isPlatedMultyLot(Set<LegalStructDTG> legalStructList) {
			boolean isPlatedMultyLot = true;
			
			if (legalStructList == null || legalStructList.size() == 0){
				isPlatedMultyLot = false;
			}
			
			for (LegalStructDTG p : legalStructList){
				if (!p.isPlated() && p.isSectional()){
					isPlatedMultyLot =  false;
					break;
				}
			}
			
			if (isPlatedMultyLot){
				LegalStructDTG first =  getFirstPlatedStruct(legalStructList);
				
				if (first == null){
					isPlatedMultyLot =  false;
				} else{
					for (LegalStructDTG p : legalStructList){
						if ((!p.getBlock().equalsIgnoreCase(first.getBlock()) 
										&& StringUtils.isEmpty(first.getBlock()) && StringUtils.isEmpty(p.getBlock()))
								|| !platBookFlexibleCheck(p.getPlatBook(), first.getPlatBook())
								|| !platPageFlexibleCheck(p.getPlatPage(), first.getPlatPage())){
							isPlatedMultyLot =  false;
							break;
						}
					}
				}
			}
			return isPlatedMultyLot ;
		}
		
		private boolean platBookFlexibleCheck(String book1, String book2) {
			if (org.apache.commons.lang.StringUtils.isBlank(book1)) {
				if (org.apache.commons.lang.StringUtils.isBlank(book2)) {
					return true;
				} else {
					return false;
				}
			} else {
				if (org.apache.commons.lang.StringUtils.isBlank(book2)) {
					return false;
				} else {
					book1 = book1.toUpperCase();
					book2 = book2.toUpperCase();
					if (book1.equals(book2)) {
						return true;
					} else if (org.apache.commons.lang.StringUtils.endsWith(book1, book2)) {
						book1 = book1.substring(0, book1.length() - book2.length());
						if (book1.length() == 1 && Character.isLetter(book1.charAt(0))) {
							return true;
						}
					} else if (org.apache.commons.lang.StringUtils.endsWith(book2, book1)) {	
						book2 = book2.substring(0, book2.length() - book1.length());
						if (book2.length() == 1 && Character.isLetter(book2.charAt(0))) {
							return true;
						}
					}
					return false;
				}
			}
		}
		
		private boolean platPageFlexibleCheck(String page1, String page2) {
			if (org.apache.commons.lang.StringUtils.isBlank(page1)) {
				if (org.apache.commons.lang.StringUtils.isBlank(page2)) {
					return true;
				} else {
					return false;
				}
			} else {
				if (org.apache.commons.lang.StringUtils.isBlank(page2)) {
					return false;
				} else {
					page1 = page1.toUpperCase();
					page2 = page2.toUpperCase();
					if (page1.equals(page2)) {
						return true;
					} else if (org.apache.commons.lang.StringUtils.startsWith(page1, page2)) {
						page1 = page1.substring(page2.length(), page1.length());
						if (page1.length() == 1 && Character.isLetter(page1.charAt(0))) {
							return true;
						}
					} else if (org.apache.commons.lang.StringUtils.startsWith(page2, page1)) {	
						page2 = page2.substring(page1.length(), page2.length());
						if (page2.length() == 1 && Character.isLetter(page2.charAt(0))) {
							return true;
						}
					}
					return false;
				}
			}
		}
		
		protected void boostrapSectionalData(Set<LegalStructDTG> legalStruct1, Search search) {
			SearchAttributes sa = search.getSa();
			if (legalStruct1 != null){
				for (LegalStructDTG legalStructDTG : legalStruct1) {
					LegalStructDTG legalStruct = legalStructDTG;
					if (StringUtils.isNotEmpty(legalStruct.getSection())){
						sa.setAtribute(SearchAttributes.LD_SUBDIV_SEC, legalStruct.getSection());
					}
					
					if (StringUtils.isNotEmpty(legalStruct.getTownship())){
						sa.setAtribute(SearchAttributes.LD_SUBDIV_TWN, legalStruct.getTownship());
					}
					
					if (StringUtils.isNotEmpty(legalStruct.getRange())){
						sa.setAtribute(SearchAttributes.LD_SUBDIV_RNG, legalStruct.getRange());
					}
					if (StringUtils.isNotEmpty(legalStruct.getQuarterOrder())){
						sa.setAtribute(SearchAttributes.QUARTER_ORDER, legalStruct.getQuarterOrder());
					}
					
					if (StringUtils.isNotEmpty(legalStruct.getQuarterValue())){
						sa.setAtribute(SearchAttributes.QUARTER_VALUE, legalStruct.getQuarterValue());
					}
					
					if (StringUtils.isNotEmpty(legalStruct.getArb())){
						sa.setAtribute(SearchAttributes.ARB, legalStruct.getArb());
					}
					
					if (StringUtils.isNotEmpty(legalStruct.getArbLot())){
						sa.setAtribute(SearchAttributes.ARB_LOT, legalStruct.getArbLot());
					}
					
					if (StringUtils.isNotEmpty(legalStruct.getArbBlock())){
						sa.setAtribute(SearchAttributes.ARB_BLOCK, legalStruct.getArbBlock());
					}
					
					if (StringUtils.isNotEmpty(legalStruct.getArbBook())){
						sa.setAtribute(SearchAttributes.ARB_BOOK, legalStruct.getArbBook());
					}
					
					if (StringUtils.isNotEmpty(legalStruct.getArbPage())){
						sa.setAtribute(SearchAttributes.ARB_PAGE, legalStruct.getArbPage());
					}
					break;
				}
			}
		}

		protected static void boostrapSubdividedData(Set<LegalStructDTG> legalStruct1, Search search, boolean boostrapPlatsAndBlock) {
			
			String aoAndTrLots = search.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			String []allAoAndTrlots = new String[0];
			
			if (StringUtils.isNotEmpty(aoAndTrLots)){
				LotInterval li = new LotInterval(aoAndTrLots);
				aoAndTrLots = li.getLotList().toString();
				aoAndTrLots = aoAndTrLots.replaceAll("(?is)[\\[\\]]+", "");
				allAoAndTrlots = aoAndTrLots.split("[ /t/r/n,-]+");
			}
			
			Set<String> allLots = new TreeSet<String>();
			allLots.addAll(Arrays.asList(allAoAndTrlots));
			
			
			SearchAttributes sa = search.getSa();
			if (boostrapPlatsAndBlock){
				for (LegalStructDTG legalStruct : legalStruct1){
					if (StringUtils.isNotEmpty(legalStruct.getPlatBook())){
						sa.setAtribute(SearchAttributes.LD_BOOKNO, legalStruct.getPlatBook());
					}
					if (StringUtils.isNotEmpty(legalStruct.getPlatPage())){
						sa.setAtribute(SearchAttributes.LD_PAGENO, legalStruct.getPlatPage());
					}
					if (StringUtils.isNotEmpty(legalStruct.getBlock())){
						sa.setAtribute(SearchAttributes.LD_SUBDIV_BLOCK, legalStruct.getBlock());
					}
				}
			}
			
			for (LegalStructDTG legalStruct : legalStruct1){
				if (!StringUtils.isEmpty(legalStruct.getLot())){
					allLots.add(legalStruct.getLot());
				}
			}
			
			String finalLot = "";
			for (String lot : allLots){
				finalLot = finalLot + lot + ",";
			}
			
			if (finalLot.length() > 1){
				finalLot = finalLot.substring(0,finalLot.length() - 1);
				search.getSa().setAtribute(SearchAttributes.LD_LOTNO, finalLot);
			}
			
		}
		
		protected boolean isCompleteLegal(LegalStructDTG ret1, int qo, String qv, int siteType, String stateAbbrev){
			return ((StringUtils.isNotEmpty(qv) || qo > 0 || StringUtils.isNotEmpty(ret1.getArb())))
						|| (StringUtils.isNotEmpty(ret1.getSection()) && StringUtils.isNotEmpty(ret1.getTownship()) && StringUtils.isNotEmpty(ret1.getRange()))
					|| (StringUtils.isNotEmpty(ret1.getArbBook()) && StringUtils.isNotEmpty(ret1.getArbPage()) 
							&& !(StringUtils.isEmpty(ret1.getArbLot()) && StringUtils.isEmpty(ret1.getArbBlock())));
		}
		
		protected boolean testIfExistsForCounty(Set<LegalStructDTG> tempPersonal, LegalStructDTG struct, long searchId){
			if (dataSite.getCountyId() == CountyConstants.FL_Broward){
				if (SUBDIVIDED_TYPE.equalsIgnoreCase(struct.getType())){
					for (LegalStructDTG p : tempPersonal){
						if (p.isPlated()){
							if (struct.getLot().replaceAll("[^\\d]","").equals(p.getLot().replaceAll("[^\\d]","")) &&
									struct.getBlock().replaceAll("[^\\d]","").equals(p.getBlock().replaceAll("[^\\d]","")) &&
									struct.getPlatBook().replaceAll("[^\\d]","").equals(p.getPlatBook().replaceAll("[^\\d]","")) &&
									struct.getPlatPage().replaceAll("[^\\d]","").equals(p.getPlatPage().replaceAll("[^\\d]",""))
									){
								return true;
							}
						}
					}
				}
			}
			
			return false;
		}
		
		
}

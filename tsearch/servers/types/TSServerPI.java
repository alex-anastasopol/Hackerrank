package ro.cst.tsearch.servers.types;
import static ro.cst.tsearch.utils.XmlUtils.getChildren;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.database.rowmapper.RequestCount;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.functions.CAParsingPI;
import ro.cst.tsearch.servers.functions.FLParsingPI;
import ro.cst.tsearch.servers.functions.ILParsingPI;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.base.address.Address;
import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.connection.titlepoint.PropertyInsightConn;
import com.stewart.ats.connection.titlepoint.PropertyInsightConn.PropertyInsightResponse;
import com.stewart.ats.connection.titlepoint.PropertyInsightImageConn;
import com.stewart.ats.connection.titlepoint.PropertyInsightImageConn.ImageDownloadResponse;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.titlepoint.www.Result;

/**
 * @author cristi stochina
 */
public abstract class TSServerPI extends TSServerROLike implements TSServerROLikeI {
	
	protected static final String CLASSES_FOLDER = BaseServlet.REAL_PATH
		+ File.separator + "WEB-INF" + File.separator + "classes" + File.separator;

	protected static final String RESOURCE_FOLDER = (CLASSES_FOLDER
			+ "resource" + File.separator + "PI" + File.separator ).replaceAll("//", "/");
	
	protected static final String FAKE_FOLDER = RESOURCE_FOLDER +"fake"+ File.separator;
	
	private static final String DT_FAKE_RESPONSE = ro.cst.tsearch.utils.StringUtils.fileReadToString(FAKE_FOLDER+"PIFakeResponse.xml");
	
	public static final long serialVersionUID = -3423435433L;
	
	transient private PropertyInsightConn conn;

	transient private PropertyInsightImageConn imagesConn;
	
	protected static String PREFIX_FINAL_LINK = "PI___";
	
	protected static Pattern finalDocLinkPattern = Pattern.compile(PREFIX_FINAL_LINK + "([^&]+)&?");
	
	protected static final String O_E_PI_TYPE 				= "TitlePoint.OwnershipAndEncumbranceResult";
	protected static final String FULL_PI_TYPE 				= "TitlePoint.DocumentListResult";
	protected static final String MRTG_PI_TYPE 				= "TitlePoint.MortgageReportResult";
	protected static final String LINE_ITEMS				= "TitlePoint.ResultWithLineItems";
	protected static final String SUBDIVISION_PICK_LIST		= "TitlePoint.SubdivisionPickListResult";
	protected static final String SUBDIVISION_DETAIL		= "TitlePoint.SubdivisionDetailResult";


	/* --------- start abstracts methods ----------------- */
	protected abstract void ParseResponse(String moduleIdx,ServerResponse Response, int viParseID)throws ServerResponseException;
		/* ------------ end abstracts methods ------------------- */
	
	@Override
	protected ServerResponse performRequest(String page, int methodType, String action, int parserId, String imageLink, 
													String vbRequest, Map<String, Object> extraParams) throws ServerResponseException {
		
		if (page.contains("/PI___")) {
			ServerResponse response = new ServerResponse();
			response.setError(ServerResponse.NOT_VALID_DOC_ERROR);
			return response;
		} else {
			return super.performRequest(page, methodType, action, parserId, imageLink, vbRequest, extraParams);
		}
	}
	
	public TSServerPI(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId, mid);
		
		try {
			conn = new PropertyInsightConn(getDataSite(),searchId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			String link = getDataSite().getLink();
			link += ( link.endsWith("/")? "TpsImage.asmx" : "/TpsImage.asmx" );
			imagesConn = new PropertyInsightImageConn(link, searchId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		resultType = MULTIPLE_RESULT_TYPE; 
	}

	
	public TSServerPI(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	/**
	 * get file name from link
	 */
	protected String getFileNameFromLink(String link) {
		// try the normal link pattern
		Matcher ssfLinkMatcher = finalDocLinkPattern.matcher(link);
		serverTypeDirectoryOverride = null;
		
		if (ssfLinkMatcher.find()) {
			return ssfLinkMatcher.group(1) + ".html";
		}
		
		throw new RuntimeException("Unknown Link Type: " + link);
	}

	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		
		if (Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF))
				&& Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CHECK_ALREADY_SAVED))){
			
			if (module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX){
				String book = StringUtils.defaultString(module.getParamValue(0)).replaceAll("^0+", "");
				String page = StringUtils.defaultString(module.getParamValue(1)).replaceAll("^0+", "");
				
				if (alreadySavedDocs(book, page, null, null)){
					return new ServerResponse();
				}
			} 
			if (module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX){
				String instrNo = StringUtils.defaultString(module.getParamValue(0)).replaceAll("^0+", "");
				String year = StringUtils.defaultString(module.getParamValue(1)).replaceAll("^0+", "");

				if (alreadySavedDocs(null, null, instrNo, year)){
					return new ServerResponse();
				}
			}
		}
		
		return searchBy(module, sd, null);
	}

	public boolean alreadySavedDocs(String book, String page, String instrNo, String year){
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	
    	boolean checkByBookPage = false;
    	boolean checkByInstrNo = false;
    	if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)){
    		checkByBookPage = true;
    	} else if (StringUtils.isNotEmpty(instrNo)){
    		if (StringUtils.isNotBlank(year)){
    			instrNo = year + "-" + instrNo;
    		}
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
								if (documentI.getYear() != SimpleChapterUtils.UNDEFINED_YEAR && StringUtils.isNotBlank(year)){
									int yearInt = SimpleChapterUtils.UNDEFINED_YEAR;
									try {
										yearInt = Integer.parseInt(year);
									} catch (Exception e) {
									}
									if (yearInt != SimpleChapterUtils.UNDEFINED_YEAR && yearInt == documentI.getYear()){
										return true;
									}
								}
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
	
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	private PropertyInsightConn.PropertyInsightResponse performSearch(Map<String, String> params, int moduleIDX, String fakeResult){

		if(fakeResult!=null){
			Result res1 =null;
			try {
				res1 = Result.Factory.parse((fakeResult));
				ArrayList<Result> results = new ArrayList<Result>();
				results.add(res1);
				PropertyInsightConn.PropertyInsightResponse res = new PropertyInsightConn.PropertyInsightResponse("", results, "");
				return res;
			} catch (XmlException e1) {
				e1.printStackTrace();
			} 
		}
		
		/*SearchCriteriaType criteria = new SearchCriteriaType();
		
		String matchMode = params.get("matchMode");
		
		if(!StringUtils.isBlank(matchMode)){
			criteria.setMatchMode(MatchModeType.Factory.fromValue(new Token(matchMode)));
		}*/
		
		mSearch.setAdditionalInfo(RequestCount.REQUEST_COUNT_TYPE, getRequestCountType(moduleIDX));
		
		//unit=234, direction=dir, unitPrefix=apt, name=MIRABELLE, state=tx, number=2806, suffix=suffix, zipp=77494, city=katy
		if(moduleIDX==TSServerInfo.ADDRESS_MODULE_IDX){
			
			if (dataSite.getStateFIPS().equals(StateContants.CA_STRING_FIPS)){
				String serviceType = StringUtils.defaultString(params.get("serviceType"));
				return conn.searchByAddress(serviceType, params);
			} else{
			
				AddressI address = new Address(StringUtils.defaultString(params.get("number")), 
							StringUtils.defaultString(params.get("name")), StringUtils.defaultString(params.get("suffix")));
				address.setCity(StringUtils.defaultString(params.get("city")));
				address.setThruNumber(StringUtils.defaultString(params.get("toNumber")));
				address.setZip(StringUtils.defaultString(params.get("zipp")));
				address.setIdentifierType(StringUtils.defaultString(params.get("unitPrefix")));
				address.setIdentifierNumber( StringUtils.defaultString(params.get("unit")));
				address.setPreDiretion(StringUtils.defaultString(params.get("preDiretion")));
				address.setPostDirection(StringUtils.defaultString(params.get("postDirection")));
				String serviceType = "TitlePoint."+params.get("serviceType");
				return conn.searchByAddress(address, serviceType);
			}
		}else if( moduleIDX==TSServerInfo.PARCEL_ID_MODULE_IDX ){
			String searchType = StringUtils.defaultString(params.get("searchType"));
			if("TitlePoint.Geo.Property".equalsIgnoreCase(searchType)){
				String area = StringUtils.defaultString(params.get("area"));
				String section = StringUtils.defaultString(params.get("section"));
				String block = StringUtils.defaultString(params.get("block"));
				String parcel = StringUtils.defaultString(params.get("parcel"));
				
				String unit = StringUtils.defaultString(params.get("unit"));
				String arb = StringUtils.defaultString(params.get("arb"));
				
				return conn.searchByPin(searchType, area, section, block, parcel, unit, arb);
			}else{
				return conn.searchByPin(params.get("pin"), searchType);
			}
			
		} else if (moduleIDX == TSServerInfo.SUBDIVISION_MODULE_IDX){
			String serviceType = StringUtils.defaultString(params.get("serviceType"));
						
			return conn.searchByProperty(serviceType, params);
		} else if (moduleIDX == TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX){
			String serviceType = StringUtils.defaultString(params.get("serviceType"));
						
			return conn.searchBySubdivisionLookup(serviceType, params);
		} else if (moduleIDX == TSServerInfo.ARCHIVE_DOCS_MODULE_IDX){
			String serviceType = StringUtils.defaultString(params.get("serviceType"));
						
			return conn.searchBySubdivisionDetailLookup(serviceType, params);
		} else if( moduleIDX==TSServerInfo.BOOK_AND_PAGE_MODULE_IDX ){
			return conn.searchByBookPage(params);
		} else if( moduleIDX==TSServerInfo.INSTR_NO_MODULE_IDX ){
			
			String searchType = StringUtils.defaultString(params.get("searchType"));
			if("TitlePoint.Geo.Document".equalsIgnoreCase(searchType)){
				return conn.searchByInstrument(params);
			} else{
				return conn.searchByInstrument(params.get("instrno"));
			}
		}else if( moduleIDX==TSServerInfo.NAME_MODULE_IDX ){
			
			String last = StringUtils.defaultString(params.get("lastName1"));
			String firstName = StringUtils.defaultString(params.get("firstName1"));
			String middleName = StringUtils.defaultString(params.get("middleName1"));
			
			String searchType = StringUtils.defaultString(params.get("searchType"));

			String fullName = last;
			if(StringUtils.isNotBlank(firstName)){
				fullName += ", "+firstName;
			}
			if(StringUtils.isNotBlank(middleName)){
				fullName += " "+middleName;
			}
			
			if("TitlePoint.Geo.Name".equalsIgnoreCase(searchType)){
				return conn.searchByGIName(fullName, params);
			}else{
				return conn.searchByNameOE(fullName, StringUtils.defaultString(params.get("oeResultId")), StringUtils.defaultString(params.get("match"))
						,StringUtils.defaultString(params.get("includeProperties")), StringUtils.defaultString(params.get("fromValueDeed"))
						,StringUtils.defaultString(params.get("fromQualifiedSale")), StringUtils.defaultString(params.get("oeDeed")));
			}
		}
		
		return null;
	}
	
	@Override
	protected Object getRequestCountType(int moduleIDX) {
		switch (moduleIDX) {
		case TSServerInfo.NAME_MODULE_IDX:
			return RequestCount.TYPE_NAME_COUNT;
		case TSServerInfo.ADDRESS_MODULE_IDX:
			return RequestCount.TYPE_ADDR_COUNT;
		case TSServerInfo.PARCEL_ID_MODULE_IDX:
			return RequestCount.TYPE_PIN_COUNT;
		case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
		case TSServerInfo.INSTR_NO_MODULE_IDX:
			return RequestCount.TYPE_INSTRUMENT_COUNT;
		}
		
		return null;
	}

	public String getPiQuery(InstrumentI i, String type, String year, long searchId){		
		
		if (StateContants.IL_STRING_FIPS.equals(dataSite.getStateFIPS())){
			return getBasePiQuery(dataSite.getStateFIPS(), dataSite.getCountyFIPS(), searchId) + ",Type=Rec,SubType=All,Year=" + year + ",Inst=" + i.getInstno();
		}
		
		return getPiQuery(i, searchId);
	}
	
	public static String getBasePiQuery(String stateFIPS, String countyFips, long searchId){
    	
    	countyFips = "000".substring(countyFips.length()) + countyFips;
    	stateFIPS = "00".substring(stateFIPS.length()) + stateFIPS;
    	
    	return "FIPS=" + stateFIPS + countyFips;
	}
	
	public String getPiQuery(InstrumentI i, long searchId){
		
		String query = getBasePiQuery(dataSite.getStateFIPS(), dataSite.getCountyFIPS(), searchId);
    	
    	if (StringUtils.isNotEmpty(i.getBook()) && StringUtils.isNotEmpty(i.getPage())){
    		if (DocumentTypes.PLAT.equalsIgnoreCase(i.getDocType())){
    			query += ",Type=Map,SubType=All,Book=" + i.getBook() + ","+"Page=" + i.getPage();
    		} else{
    			query += ",Type=Rec,SubType=All,Book=" + i.getBook() + "," + "Page=" + i.getPage();
    		}
    	}else if( StringUtils.isNotEmpty(i.getInstno())){
    		if (DocumentTypes.PLAT.equalsIgnoreCase(i.getDocType())){
    			query += ",Type=Map,SubType=Assessor,Inst=" + i.getInstno() + ",Year=" + i.getYear();
    		} else{
    			query += ",Type=Rec,SubType=All,Inst=" + i.getInstno() + ",Year=" + i.getYear();
    		}
    	}
		return query;
	}
	
	protected ServerResponse searchBy(TSServerInfoModule module, Object sd, String fakeResult) throws ServerResponseException {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		int moduleIDX = module.getModuleIdx();
		int parserID = module.getParserID();
		
//		if (!Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS))) {
//			global.removeAllInMemoryDocs();
//        }
		global.clearClickedDocuments();
		
		//don't log again if is a fake search
		if (StringUtils.isEmpty(fakeResult)){
			logSearchBy(module);
		}
		
		Map<String, String> params = getNonEmptyParams( module, null );
		
		if( moduleIDX==TSServerInfo.IMG_MODULE_IDX ){
		
			byte[] image = null;
			ImageDownloadResponse imageresponse = downloadImageResponse(params, imagesConn);
			
			try{
				mSearch.countRequest(dataSite.getSiteTypeInt(), RequestCount.TYPE_IMAGE_COUNT);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (imageresponse != null){
				image = imageresponse.imageContent;
			} else{
				ServerResponse sr = new ServerResponse();
				sr.setImageResult(new DownloadImageResult(DownloadImageResult.Status.ERROR, null, "image/tiff"));
				return sr;
			}
			if(image!=null && image.length>0){
				ServerResponse sr = new ServerResponse();
				sr.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK,image,
						"image/tiff"));
				afterDownloadImage();
				return sr;
			}else{
				ServerResponse sr = new ServerResponse();
				sr.setImageResult(new DownloadImageResult(DownloadImageResult.Status.ERROR,null,
						"image/tiff"));
				return sr;
			}
			
		}else if( moduleIDX == TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX && module.getParserID() != ID_SEARCH_BY_BOOK_AND_PAGE){
			
				mSearch.setAdditionalInfo("RESAVE_DOCUMENT",Boolean.FALSE);
				
				String book = module.getParamValue( 0 );
				String page = module.getParamValue( 1 );
				String docNo = module.getParamValue( 2 );
				String year = module.getParamValue(3);
				String docType = module.getParamValue(4);
				
				int yearI =-1; 
				if (StringUtils.isNotBlank(year)){
					try{yearI=Integer.parseInt(year);}catch(Exception e){}
				} else{
					Calendar cal = Calendar.getInstance();
					yearI = cal.get(Calendar.YEAR);
					year = Integer.toString(yearI);
				}
				InstrumentI i = new Instrument(book,page,docType,"",yearI);
				i.setInstno(docNo);
				i.setYear(yearI);
				i.setDocType(docType);
				
		    	String key = (book==null?"":book)+"-"+(page==null?"":page)+"-"+(docNo==null?"":docNo);
		    	
		    	HashSet<String> searchedImages = (HashSet<String>)global.getAdditionalInfo("SEARCHED_IMAGES");
		    	if(searchedImages ==null){
		    		searchedImages  = new HashSet<String>();
		    		global.setAdditionalInfo("SEARCHED_IMAGES",searchedImages);
		    	}
		    	
		    	if( !isParentSite() && searchedImages.contains(key)){
		    		return new ServerResponse();
		    	}
		    	
		    	String fileName = createTempSaveFileName( i, getCrtSearchDir() );
		    	
		    	boolean imageDownloaded = false;
		    	ImageDownloadResponse imageDownloadResponse =null;
				try {
					imageDownloadResponse = downloadImageFromPropertyInsight(fileName, getPiQuery(i, docType, year, searchId), searchId);
					imageDownloaded = imageDownloadResponse.success;
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if(imageDownloaded ){
					SearchLogger.info( "<font>Image " +i.prettyPrint()+ " Downloaded from Property Insight</font><br>", searchId );  
					
					if( !isParentSite() ){
						searchedImages.add(key);
			    	}
					
					String grantor  = "County of "+InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
					grantor=grantor==null?"":grantor;
					String grantee = mSearch.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME);
					grantee=grantee==null?"":grantee;
					
					grantee = ro.cst.tsearch.utils.StringUtils.HTMLEntityEncode(grantee);
					grantor = ro.cst.tsearch.utils.StringUtils.HTMLEntityEncode(grantor);
					
					if(!"PLAT".equals(docType)){
						grantee="";
						grantor="";
					}
					
					String doc = DT_FAKE_RESPONSE.replace("@@Grantee@@", grantee);
					doc = doc.replace("@@Grantor@@", grantor);
					doc = doc.replace("@@Year@@", year);
					doc = doc.replace("@@Type@@", docType);
					doc = doc.replace("@@ImageId@@", imageDownloadResponse.imageKey);
					
					doc = doc.replace("@@Book@@", book==null?"":book);
					doc = doc.replace("@@Page@@", page==null?"":page);
					doc = doc.replace("@@DocNo@@", docNo==null?"":docNo);
					
					module.setParserID(ID_SEARCH_BY_BOOK_AND_PAGE);
					return searchBy(module, sd, doc);
				} else{
					SearchLogger.info( "<font>Could not download image " +i.prettyPrint()+ " from Property Insight</font><br>", searchId );
					ServerResponse sr = new ServerResponse();
					ParsedResponse pr = new ParsedResponse();
					sr.setParsedResponse(pr);
					sr.setResult("<b>Could not download image</b>");
					solveHtmlResponse(module.getModuleIdx()+"", module.getParserID(), "SearchBy", sr, sr.getResult());
					return sr;
				}
			
		}else{
			
			if(moduleIDX==TSServerInfo.NAME_MODULE_IDX){
				if(StringUtils.isBlank(module.getParamValue(3))){
					return ServerResponse.createWarningResponse("Could not run Name Search ! OE Result ID is Empty !");
				}
			}
			
			PropertyInsightResponse response = null;
			
			//get modules
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if(sd instanceof SearchDataWrapper){
				SearchDataWrapper searchDataWrapper = (SearchDataWrapper)sd;
				prepareQuery(true, module, searchDataWrapper);
			}
			
			if(modules.size()>1 && verifyModule(modules.get(1))){
				
				List<Result> results = new ArrayList<Result>();
				 
				for (int i = 0; i < modules.size(); i++) {
					TSServerInfoModule mod = modules.get(i);
				
					if (verifyModule(mod)) {
						logSearchBy(mod);
						results.addAll(performSearch( getNonEmptyParams(mod, null), mod.getModuleIdx(), fakeResult).getListResults());
					}						
				}	
				
				if (results.size() > 0)
					response = new PropertyInsightResponse("", results, "");
				else
					response = new PropertyInsightResponse("Error for multiple field search!", results, "");
				
			} else {
				response = performSearch( params, module.getModuleIdx(), fakeResult);				
			}
			
 			if (response == null){
				return ServerResponse.createErrorResponse("Empty response received form PI"); 
			} else if(response.hasErrors()){
				String errors = response.getErrorMessage();
				if (errors.toLowerCase().contains("servicetype") && dataSite.getStateFIPS().equals(StateContants.FL_STRING_FIPS)){
					errors  += ".<br> Take in consideration that PI is implemented only for O&E product Type";
				}
				logInSearchLogger("<font color=\"red\">" + errors + " </font>", searchId, true);
				return ServerResponse.createErrorResponse("PI Error: " + errors); 
			} else{
				String warning = "";
				if (response.hasResults() && response.getListResults().size() == 1){
					List<Result> result = response.getListResults();
					Node node = result.get(0).getDomNode();
					if (node != null){
						Node docListNode = (Node) getTransactions(node, "DocumentList");
						if (docListNode != null) {
							if (docListNode.getChildNodes().getLength() > 0){
								for (Node childP : getChildren(docListNode)) {
									if ("WarningMessage".equalsIgnoreCase(childP.getNodeName()) && childP.hasChildNodes()){
										warning = childP.getFirstChild().getNodeValue();
									}
								}
							}
						} else{
							docListNode = (Node) getTransactions(node, "PickList");
							if (docListNode != null) {
								if (docListNode.getChildNodes().getLength() > 0){
									for (Node childP : getChildren(docListNode)) {
										if ("OutputMessage".equalsIgnoreCase(childP.getNodeName()) && childP.hasChildNodes()){
											warning = childP.getFirstChild().getNodeValue();
										}
									}
								}
							}
						}
					}
				}
				if (StringUtils.isNotEmpty(warning) && !warning.toLowerCase().contains("the plant returned no records for this search")){
					logInSearchLogger("<font color=\"red\">" + warning + " </font>", searchId, true);
					return ServerResponse.createWarningResponse(warning);
				}
			}
			
			return processPIResponse(response, module,parserID, fakeResult);
		}
	}
	
	public static String createTempSaveFileName(InstrumentI i , final String SEARCH_DIR){
		String folderName = SEARCH_DIR + "Register" + File.separator;
		new File(folderName).mkdirs();
		String key ="";
		if (i.hasBookPage()){
			key = i.getBook() + "_" + i.getPage() + "_" + i.getDocType();
		} else if(i.hasInstrNo()){
			key = i.getInstno() + "_" + i.getDocType() + "_" + i.getYear();
		} else if(i.hasDocNo()){
			key = i.getDocno() + "_" + i.getDocType() + "_" + i.getYear();
		} else{
			throw new RuntimeException("-createTempSaveFileName- Please pase a valid Instrument");
		}
    	return  folderName + key + ".tif";
	}
	
	public static ImageDownloadResponse downloadImageFromPropertyInsight(String imageFilePath,  String query, long searchId) throws IOException{
		
    	PropertyInsightImageConn conn = new PropertyInsightImageConn("http://titlepoint.com/TitlePointServices/TpsImage.asmx", searchId);
		byte imageBytes[] = null;
		
    	
		ImageDownloadResponse resp = conn.getDocumentsByParameters2(query);
		
		if (resp == null){
			return new ImageDownloadResponse();
		}
		
		imageBytes = resp.imageContent;
			
    	if (imageBytes != null && imageBytes.length > 0){
    		
    		//image was downloaded -> mark this as soon as possible
    		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    		search.countNewImage(GWTDataSite.PI_TYPE);
    		
			org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(imageFilePath), imageBytes);
		}
    	
    	File file = new File(imageFilePath);
    	resp.success = (file.exists() && !file.isDirectory());
    	
    	return resp;
	}

	protected boolean verifyModule(TSServerInfoModule mod) {
		
		if(mod == null)
			return false;
		
		if (mod.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
			if (mod.getFunctionCount()>3 && StringUtils.isNotEmpty(mod.getFunction(2).getParamValue())
					&& StringUtils.isNotEmpty(mod.getFunction(3)
							.getParamValue())) {
				return true;
			} 
			return false;
		}
		
		if (mod.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) {
			if (mod.getFunctionCount() > 2
					&& (StringUtils.isNotEmpty(mod.getFunction(0)
							.getParamValue()) || StringUtils.isNotEmpty(mod
							.getFunction(1).getParamValue()))) {
				return true;
			}
			return false;
		}
		
		if (mod.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
			if (mod.getFunctionCount() > 1
					&& StringUtils.isNotEmpty(mod.getFunction(0)
							.getParamValue())) {
				return true;
			}
			return false;
		}

		System.err.println(this.getClass()+ "I shouldn't be here!!! Line 442");
		return false;
	}
	
	protected ServerResponse processPIResponse(PropertyInsightResponse response, TSServerInfoModule module, int parserID, String fakeDoc) throws ServerResponseException {
		return processPIResponse(response, module, parserID, true, fakeDoc);
	}
	
	protected ServerResponse processPIResponse(PropertyInsightResponse response, TSServerInfoModule module, int parserID, boolean log, String fakeDoc) throws ServerResponseException {
		
		if (response == null || !response.hasResults()){
			logInSearchLogger("<font color=\"red\">PI returned empty response ! </font>", searchId,log);
			return ServerResponse.createWarningResponse("PI returned empty response !");
		}
				
		// create & populate server response
		ServerResponse sr = new ServerResponse();
		ArrayList<ParsedResponse> parsedRowsList = new ArrayList<ParsedResponse>();
		String html = parseAndBuildHTML(response, parsedRowsList, module, fakeDoc);
		
		Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>(parsedRowsList);
		sr.getParsedResponse().setResultRows(parsedRows);
		sr.setResult(html);
		
		String saction = module.getModuleIdx() + "";
		
		String query = response.getQuery();
		if (StringUtils.isNotEmpty(query)){
			saction += "&" + query;
		}

		sr.setQuerry(getTSConnection().getQuery());
//        System.err.println(getTSConnection().getQuery());
		
		// sAction(link-ul) for SSF does not make any sense, but we can use it for transporting the module number
		solveHtmlResponse(saction, parserID, "SearchBy", sr, sr.getResult());
		
		// log number of results found
		logInSearchLogger("Found <span class='number'>"+ sr.getParsedResponse().getResultsCount()+ "</span> <span class='rtype'>intermediate</span> results.<br/>",searchId,log);
		
		return sr;
	}
	
	private void logInSearchLogger(String message, long searchId, boolean doLog) {
		if(doLog) {
			SearchLogger.info(message, searchId);
		}
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<Object> l = new ArrayList<Object>();
		
		serverInfo.setModulesForAutoSearch(l);
	}

	public String parseAndBuildHTML(PropertyInsightResponse response, List<ParsedResponse> list, TSServerInfoModule module, String fakeDoc){
		if (response == null || !response.hasResults()){
			return "";
		}
 
		StringBuilder fullHtml = new StringBuilder();
		for(Result result:response.getListResults()){
			try{
				//FileUtils.writeStringToFile(new File("e:/resultsnameOe.xml"), result.toString());
				int id = result.getID();
				//int product = mSearch.getSearchProduct();
				
				String resultType = result.getResultType().trim();
				//TitlePoint.ResultWithLineItems
				Node node = result.getDomNode();
				
				NodeList allTrans = null;
				Node docListNode = null, docIdentificationNode = null, imagesNode = null;
				Node parcelsNode = null, partiesNode = null, addressesNode = null, apnsNode = null;
				HashMap<String, Node> nodes = new LinkedHashMap<String, Node>();
				
				if (LINE_ITEMS.equalsIgnoreCase(resultType)){
					allTrans  = getTransactions(node, "Items");
				}else if (O_E_PI_TYPE.equalsIgnoreCase(resultType.trim())){
					mSearch.getSa().setAtribute(SearchAttributes.LD_PARCELNO_MAP, id+"");
					allTrans = getTransactions(node,"Transactions");
				}else if (MRTG_PI_TYPE.equalsIgnoreCase(resultType.trim())){
					allTrans = getTransactions(node,"MortgageHistory");
				} else if (FULL_PI_TYPE.equalsIgnoreCase(resultType.trim())){
					docListNode = (Node) getTransactions(node, "DocumentList");
					nodes.put("DocumentList", docListNode);
					allTrans = getTransactions(docListNode, "Items");
					
					docIdentificationNode = (Node) getTransactions(docListNode, "DocumentIdentifications");
					nodes.put("DocumentIdentifications", docIdentificationNode);
					
					imagesNode = (Node) getTransactions(docListNode, "Images");
					nodes.put("Images", imagesNode);
					
					parcelsNode = (Node) getTransactions(docListNode, "Parcels");
					nodes.put("Parcels", parcelsNode);
					
					partiesNode = (Node) getTransactions(docListNode, "Parties");
					nodes.put("Parties", partiesNode);
					
					addressesNode = (Node) getTransactions(docListNode, "Addresses");
					nodes.put("Addresses", addressesNode);
					
					apnsNode = (Node) getTransactions(docListNode, "APNs");
					nodes.put("APNs", apnsNode);
					
					Node plantDatesNode = (Node) getTransactions(docListNode, "PlantDates");
					if (plantDatesNode != null){
						String content = XmlUtils.createXMLString(plantDatesNode, true);
						Pattern CERT_DATE_PAT = Pattern.compile("(?is)<Date>([^<]*)</Date>\\s*<FipsCode>[^<]*</FipsCode>\\s*<PlantDateType>PROPERTY</PlantDateType>\\s*<PlantDateSubType>COVER</PlantDateSubType>");
						Matcher mat = CERT_DATE_PAT.matcher(content);
						if (mat.find()){
							String certificationDate = mat.group(1);
							certificationDate = certificationDate.replaceAll("(?is)\\A\\s*(\\d{4})\\s*-\\s*(\\d{1,2})\\s*-\\s*(\\d{1,2})\\s*$", "$2/$3/$1");
							
							try {
								setCertificationDate(certificationDate);
							} catch (Exception e) {
								logger.error("Could not parse Certification Date on " + this.toString() + ", searchid = " + searchId);
							}
						}
					}
				} else if (SUBDIVISION_PICK_LIST.equalsIgnoreCase(resultType)){
					Node nodeS = (Node) getTransactions(node, "SubdivisionPickList");
					return XmlUtils.createXMLString((Node)getTransactions(nodeS, "Items"), true);
				} else if (SUBDIVISION_DETAIL.equalsIgnoreCase(resultType)){
					Node nodeS = (Node) getTransactions(node, "SubdivisionDetail");
					return XmlUtils.createXMLString((Node)getTransactions(nodeS, "PropertyDetails"), true);
				} 
//				else if ("TitlePoint.PickListResult".equalsIgnoreCase(resultType)){
//					docListNode = (Node) getTransactions(node, "PickList");
//					allTrans = getTransactions(docListNode, "PickListItems");
//				}
				else throw new RuntimeException("Unknown response type received:" + resultType);
				
				Map<String,List<RegisterDocumentI>> docs = new HashMap<String,List<RegisterDocumentI>>();
				
				Set<String> docIds = new LinkedHashSet<String>();
				
				if (allTrans != null){
					for (int i = 0; i < allTrans.getLength(); i++){
						ParsedResponse item = new ParsedResponse();
						
						Node itemAsNode = allTrans.item(i);
						
						if (FULL_PI_TYPE.equalsIgnoreCase(resultType.trim())){
							boolean prepared = prepareItemNodeFromAllNodesNeeded(itemAsNode, docListNode, nodes, docIds);
							if (!prepared){
								continue;
							}
						}
						
						String html = buildHtml(itemAsNode,  module.getModuleIdx(), resultType);
						html = html.replaceAll("&thorn;", " ");
						
						fullHtml.append( html  );
						item.setAttribute(ParsedResponse.REAL_PI, "true");
						item.setResponse( html );
						
						ResultMap resultMap = new ResultMap();
						parseAndFillResultMap(itemAsNode, resultMap, searchId, module.getModuleIdx(), resultType);
						
						Bridge bridge = new Bridge(item, resultMap, searchId);
						DocumentI doc = bridge.importData();
						if (StringUtils.isNotBlank(module.getSearchType())){
							doc.setSearchType(SearchType.valueOf(module.getSearchType()));
						}
						addPropertiesToRegisterDoc(doc, itemAsNode, item);
						
						if (StringUtils.isNotBlank(fakeDoc)){
							doc.setFake(true);
						}
						
						String remarks = StringUtils.defaultString(findFastNodeValue(itemAsNode, "PropertyRemark"));
						if (StringUtils.isNotEmpty(remarks)){
							remarks = remarks.replace(((char) 254), ' ');
							remarks = remarks.replaceAll("&#254;", " ");
							remarks = remarks.replaceAll("&thorn;", " ");
							if (module.getModuleIdx() == 0){
								doc.setInfoForSearchLog(remarks);
							}
						}

						item.setDocument(doc);
						
						list.add(item);
						
						String srchExt = ((SaleDataSet)item.getSaleDataSet().get(0)).getAtribute("SearcherExtensions");
						
						if(StringUtils.isNotBlank(srchExt)){
							List<RegisterDocumentI> crtList = docs.get(srchExt);
							if (crtList == null){
								crtList = new ArrayList<RegisterDocumentI>();
							}
							
							DocumentI currDoc = item.getDocument();
							
							if (currDoc  instanceof RegisterDocumentI ){
								RegisterDocumentI currRegDoc = (RegisterDocumentI)currDoc;
								crtList.add(currRegDoc);
							}
							docs.put(srchExt, crtList);							
						}
					}
				}
				
				for (String key : docs.keySet()){
					List<RegisterDocumentI> crtList = docs.get(key);
					for (RegisterDocumentI currDoc:crtList){
						for (RegisterDocumentI currDoc1:crtList){
							if (!currDoc.equals(currDoc1)){
								Set<InstrumentI> parsedRef = currDoc.getParsedReferences();
								for (InstrumentI eachParsedRef : parsedRef){
									if (eachParsedRef.flexibleEquals(currDoc1.getInstrument())){
										parsedRef.remove(eachParsedRef);
										currDoc.addParsedReference(currDoc1.getInstrument());
									}
								}
							}
						}
					}
				}
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return fullHtml.toString();
	}
	
	protected void addPropertiesToRegisterDoc(DocumentI doc, Node itemAsNode, ParsedResponse parsedResponse){}
	
	public void saveWithCrossReferences(InstrumentI instrument, ParsedResponse currentResponse){}

	protected List<DocumentI> checkFlexibleInclusion(InstrumentI documentToCheck, DocumentsManagerI documentManager, boolean checkDocType) {
		
		InstrumentI documentToCheckCopy = documentToCheck.clone();
		String instno = documentToCheckCopy.getInstno();
		if (!instno.contains("-") && documentToCheckCopy.getYear() > SimpleChapterUtils.UNDEFINED_YEAR){
			String year = "";
			try {
				year = Integer.toString(documentToCheckCopy.getYear());
			} catch (Exception e) {
			}
			if (StringUtils.isNotEmpty(year)){
				documentToCheckCopy.setInstno(year + "-" + instno);
			}
		}
		String doctype = documentToCheckCopy.getDocType();
		if (StringUtils.isNotEmpty(doctype)){
			documentToCheckCopy.setDocType(DocumentTypes.getDocumentCategory(doctype, searchId));
			documentToCheckCopy.setDocSubType(DocumentTypes.getDocumentSubcategory(doctype, searchId));
		}
		List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheckCopy);
		
		List<DocumentI> alike = new ArrayList<DocumentI>();
		
		if (almostLike != null && !almostLike.isEmpty()) {
			
			return almostLike;
			
		} else {
			if (checkDocType) {
				return almostLike;
			}
			String bookToCheck = documentToCheck.getBook();
				
			List<DocumentI> allRODocuments = documentManager.getDocumentsWithDataSource(false, getDataSite().getSiteTypeAbrev());
			for (DocumentI documentI : allRODocuments) {
				if (org.apache.commons.lang.StringUtils.isNotEmpty(documentI.getBook()) && org.apache.commons.lang.StringUtils.isNotEmpty(bookToCheck) 
							&& documentI.getBook().equals(bookToCheck)
					   && org.apache.commons.lang.StringUtils.isNotEmpty(documentI.getPage()) && org.apache.commons.lang.StringUtils.isNotEmpty(documentToCheck.getPage()) 
					   		&& documentI.getPage().equals(documentToCheck.getPage())) {
						
					alike.add(documentI);
				} else if (documentI.getInstno().equals(documentToCheck.getInstno())){
					alike.add(documentI);
				}
			}
		}
		return alike;
	}


	protected static NodeList  getTransactions(Node node, String childName){
		NodeList allChilds = node.getChildNodes();
		for(int i=0;i<allChilds.getLength();i++){
			Node n = allChilds.item(i);
			if(n.getNodeName().trim().toUpperCase().endsWith(childName.toUpperCase())){
				return n.getChildNodes();
			}
		}
		return null;
	}
	
	private boolean prepareItemNodeFromAllNodesNeeded(Node currentItemNode, Node docListNode, HashMap<String, Node> nodes, Set<String> docIds){
		
		Node partiesNode = nodes.get("Parties");
		Node addressesNode = nodes.get("Addresses");
		Node docIdentificationNode = nodes.get("DocumentIdentifications");
		Node imagesNode = nodes.get("Images");
		Node parcelsNodeBulk = nodes.get("Parcels");
		Node apnsNode = nodes.get("APNs");
		
		/*try {
			String content = XmlUtils.createXMLString(currentItemNode, true);
			String contentLN = XmlUtils.createXMLString(docListNode, true);
			String contentIN = XmlUtils.createXMLString(imagesNode, true);
			String contentDIN = XmlUtils.createXMLString(docIdentificationNode, true);
			String contentPN = XmlUtils.createXMLString(parcelsNode, true);
			String contentPRN = XmlUtils.createXMLString(partiesNode, true);
			String contentADN = XmlUtils.createXMLString(addressesNode, true);
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		try {
			if (partiesNode != null) {
				if (currentItemNode.getChildNodes().getLength() > 0){
					if (currentItemNode.getChildNodes().item(0).getChildNodes().getLength() > 0){

						Node nodeParties = currentItemNode.getChildNodes().item(0);
						for (Node childP : getChildren(nodeParties)) {
							
							if (childP.hasAttributes()){
								String partyId = childP.getAttributes().item(0).getNodeValue();
							
								for (Node child : getChildren(partiesNode)) {
									String childItem = child.getNodeName();
									if ("DocumentParty".equals(childItem)) {
										if (child.hasChildNodes()){
											String childName = child.getFirstChild().getNodeName();
											if ("Id".equals(childName)) {
												if (child.hasChildNodes()){
													String childIdValue = child.getFirstChild().getFirstChild().getNodeValue();
													if (StringUtils.isNotEmpty(partyId) && partyId.equals(childIdValue)) {
														nodeParties.replaceChild(child.cloneNode(true), childP);
														break;
													}
												}
											}
										}
									}
								}
							}
						}
						currentItemNode.replaceChild(nodeParties, currentItemNode.getChildNodes().item(0));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//XmlUtils.createXMLString(currentItemNode, true);
		try {
			if (addressesNode != null) {
				if (currentItemNode.getChildNodes().getLength() > 40){
					for (Node child : getChildren(currentItemNode)){
						if ("DocumentAddresses".equals(child.getNodeName())){
							if (child.hasChildNodes()){
								Node nodeAddresses = child;
								
								for (Node childP : getChildren(nodeAddresses)) {
									
									if (childP.hasAttributes()){
										String addressId = childP.getAttributes().item(0).getNodeValue();
										
										for (Node childAddress : getChildren(addressesNode)) {
											String childItem = childAddress.getNodeName();
											if ("Address".equals(childItem)) {
												if (childAddress.hasChildNodes()){
													String childName = childAddress.getFirstChild().getNodeName();
													if ("Id".equals(childName)) {
														if (childAddress.hasChildNodes()){
															String childIdValue = childAddress.getFirstChild().getFirstChild().getNodeValue();
															if (StringUtils.isNotEmpty(addressId) && addressId.equals(childIdValue)) {
																nodeAddresses.replaceChild(childAddress.cloneNode(true), childP);
																break;
															}
														}
													}
												}
											}
										}
									}
								}
								currentItemNode.replaceChild(nodeAddresses, child);
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			if (apnsNode != null) {
				if (currentItemNode.getChildNodes().getLength() > 0){
					for (Node child : getChildren(currentItemNode)){
						if ("DocumentAPNs".equals(child.getNodeName())){
							if (child.hasChildNodes()){
								Node nodeAPN = child;
								
								for (Node childP : getChildren(nodeAPN)) {
									String childItem = childP.getNodeName();
									if ("DocumentAPN".equals(childItem)) {
										if (childP.hasChildNodes()){
											String childName = childP.getFirstChild().getNodeName();
											if ("Id".equals(childName)) {
												if (childP.hasChildNodes()){
													String childIdValue = childP.getFirstChild().getFirstChild().getNodeValue();
													for (Node childAPN : getChildren(apnsNode)) {
														if (childAPN.hasAttributes()){
															String apnKey = childAPN.getAttributes().item(0).getNodeValue();
															if (StringUtils.isNotEmpty(apnKey) && apnKey.equals(childIdValue)) {
																childP.appendChild(childAPN);
																break;
															}
														}
													}	
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			if (docIdentificationNode != null) {
				
				if (currentItemNode.getChildNodes().item(6).hasAttributes()){
					String docIdentificationId = currentItemNode.getChildNodes().item(6).getAttributes().item(0).getNodeValue();
					if (docIds.contains(docIdentificationId)){
						return false;
					} else {
						docIds.add(docIdentificationId);
					}
					for (Node child : getChildren(docIdentificationNode)) {
						String childItem = child.getNodeName();
						if ("DocumentIdentification".equals(childItem)) {
							if (child.hasChildNodes()){
								String childName = child.getFirstChild().getNodeName();
								if ("Id".equals(childName)) {
									if (child.hasChildNodes()){
										String childIdValue = child.getFirstChild().getFirstChild().getNodeValue();
										if (StringUtils.isNotEmpty(docIdentificationId) && docIdentificationId.equals(childIdValue)) {
											currentItemNode.replaceChild(child.cloneNode(true), currentItemNode.getChildNodes().item(6));
											break;
										}
									}
								}
							}
						}
					}
					
				}
				
				if (currentItemNode.getChildNodes().item(7).hasChildNodes()){
					
					Node nodeRefDocs = currentItemNode.getChildNodes().item(7);
					for (Node childP : getChildren(nodeRefDocs)) {
						
						if (childP.hasAttributes()){
							String refDocId = childP.getAttributes().item(0).getNodeValue();
							
							for (Node child : getChildren(docIdentificationNode)) {
								String childItem = child.getNodeName();
								if ("DocumentIdentification".equals(childItem)) {
									if (child.hasChildNodes()){
										String childName = child.getFirstChild().getNodeName();
										if ("Id".equals(childName)) {
											if (child.hasChildNodes()){
												String childIdValue = child.getFirstChild().getFirstChild().getNodeValue();
												if (StringUtils.isNotEmpty(refDocId) && refDocId.equals(childIdValue)) {
													nodeRefDocs.replaceChild(child.cloneNode(true), childP);
													break;
												}
											}
										}
									}
								}
							}
						}
					}
					currentItemNode.replaceChild(nodeRefDocs, currentItemNode.getChildNodes().item(7));
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (imagesNode != null) {
				if (currentItemNode.getChildNodes().item(8).hasAttributes()){
					String imageId = currentItemNode.getChildNodes().item(8).getAttributes().item(0).getNodeValue();
					for (Node child : getChildren(imagesNode)) {
						String childItem = child.getNodeName();
						if ("Item".equals(childItem)) {
							if (child.hasChildNodes()){
								String childName = child.getFirstChild().getNodeName();
								if ("Id".equals(childName)) {
									if (child.hasChildNodes()){
										String childIdValue = child.getFirstChild().getFirstChild().getNodeValue();
										if (StringUtils.isNotEmpty(imageId) && imageId.equals(childIdValue)) {
											currentItemNode.getChildNodes().item(8).appendChild(child.cloneNode(true));
											
											//currentItemNode.replaceChild(child.cloneNode(true), currentItemNode.getChildNodes().item(8));
											break;
										}
									}
								}
							}
						}
					}
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			if (parcelsNodeBulk != null){

				if (currentItemNode.getChildNodes().item(9).hasChildNodes()){
					Node nodeParcels = currentItemNode.getChildNodes().item(9);
					for (Node childP : getChildren(nodeParcels)) {
						
						if (childP.hasAttributes()){
							String parcelId = childP.getAttributes().item(0).getNodeValue();
							
							for (Node child : getChildren(parcelsNodeBulk)) {
								String childItem = child.getNodeName();
								if ("Parcel".equals(childItem)) {
									if (child.hasChildNodes()){
										for (Node eachChild : getChildren(child)){
											String childName = eachChild.getNodeName();
											if (eachChild.hasChildNodes() && "ParcelId".equals(childName)){
												String childIdValue = eachChild.getFirstChild().getNodeValue();
												
												if (StringUtils.isNotEmpty(childIdValue)) {												
													if (StringUtils.isNotEmpty(parcelId) && parcelId.equals(childIdValue)) {
														nodeParcels.replaceChild(child.cloneNode(true), childP);
														break;
													}
												}
											}
										}

									}
								}
							}
						}
					}
					currentItemNode.replaceChild(nodeParcels, currentItemNode.getChildNodes().item(9));
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	private static final Pattern PAT_MAP_REF = Pattern.compile("(?:MB|OR)\\s*([0-9]+)\\s*PG\\s*([0-9]+)");
	private static final Pattern PAT_MORTDOC_BP = Pattern.compile("([0-9]+)\\s*/\\s*([0-9]+)");
	

	@SuppressWarnings("unchecked")
	private static void parseReferences(ResultMap resultMap, List<InstrumentI> list) {
		String[] header = { "InstrumentNumber" ,"Book", "Page" };
		List<List> body = new ArrayList<List>();
		
		for(InstrumentI inst:list){
			if(inst.hasInstrNo()||inst.hasBookPage()){
				List<String> line = new ArrayList<String>();
				line.add(inst.getInstno());
				line.add(inst.getBook());
				line.add(inst.getPage());
				body.add(line);
			}
		}
		
		if(!body.isEmpty()) {
			resultMap.put("CrossRefSet", GenericFunctions2.createResultTable(body, header));
		}
	}
	
	protected void  setCertificationDate(String certif) {
		if(StringUtils.isNotBlank(certif)){
			getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(certif));
		}
	}
	
	protected static String findFastNodeValue(Node node, String key){
		String ret = XmlUtils.findFastNodeValue(node, key);
		if(StringUtils.isBlank(ret)){
			key = key.replace("/", "/tit:");
			ret  = XmlUtils.findFastNodeValue(node, "tit:"+key);
		}
		return ret;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void parseAndFillResultMap(Node node, ResultMap m, long searchId, int moduleIDX, String resultType) {
		
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());
		int countyId = dataSite.getCountyId();
		
		if (FULL_PI_TYPE.equals(resultType)){
			
			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
			
			String instrNo = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/InstrumentNumber"));
			if (StringUtils.isEmpty(instrNo)){
				instrNo = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/CaseNumber"));
			}
			String bookPage = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/BookPage"));
			if (StringUtils.isNotEmpty(bookPage)){
				String[] bp = bookPage.split("\\s*/\\s*");
				if (bp.length == 2){
					m.put(SaleDataSetKey.BOOK.getKeyName(), bp[0]);
					m.put(SaleDataSetKey.PAGE.getKeyName(), bp[1]);
				}
			}
			if (StringUtils.isEmpty(instrNo)){
				instrNo = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/Note"));
			}
			String serverDocType = StringUtils.defaultString(findFastNodeValue(node, "DocumentFullName"));

			String recordingDate = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/RecordingDate"));
			String instrumentDate = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/DatedDate"));

//			try {
//				String serverDocTypeCode = StringUtils.defaultString(findFastNodeValue(node, "DocumentType"));
//				FileWriter fwi = new FileWriter(new File("D:\\PI\\" + dataSite.getStateAbbreviation() + "\\" + dataSite.getCountyName() + "\\doctypes.txt"),true);
//				fwi.write(instrNo + "#####" + crtCounty + "#####" + serverDocType + "#####" + serverDocTypeCode + "#####" + recordingDate);
//				fwi.write("\n");
//				fwi.close();
//			} catch (IOException e2) {
//				e2.printStackTrace();
//			}
			
			if (StringUtils.isEmpty(instrumentDate)){
				instrumentDate = StringUtils.defaultString(findFastNodeValue(node, "DocumentIdentification/JudgementDate"));
			}
			
			if (dataSite.getStateFIPS().equals(StateContants.IL_STRING_FIPS)){
				instrNo = ILParsingPI.correctInstrumentNumber(crtState, crtCounty, serverDocType, instrNo, recordingDate);
			} else if (dataSite.getStateFIPS().equals(StateContants.CA_STRING_FIPS)){
				if (StringUtils.isNotEmpty(instrNo)){
					if (StringUtils.isNotEmpty(instrumentDate) && instrumentDate.length() > 4){
						instrNo = instrumentDate.substring(0, 4) + "-" + instrNo;
					} else if (StringUtils.isNotEmpty(recordingDate) && recordingDate.length() > 4){
						instrNo = recordingDate.substring(0, 4) + "-" + instrNo;
					}
				}
			}
			
			m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
			m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
			m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordingDate);
			m.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrumentDate);
			
//			String docket = StringUtils.defaultString(findFastNodeValue(node, "Id"));
//			if (StringUtils.isNotEmpty(docket)){
//				m.put(SaleDataSetKey.DOCKET.getKeyName(), docket);
//			}
			
			String content = XmlUtils.createXMLString(node, true);
			String parcel = "", apnSection = "", block = "", area = "", unit = "";
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(content)){
				Pattern p = Pattern.compile("(?is)<APNSection>([^<]*)</APNSection>");
				Matcher mat = p.matcher(content);
				if (mat.find()){
					apnSection = mat.group(1).trim();
					m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), apnSection);
				}
				
				if (countyId == CountyConstants.IL_Lake){
					p = Pattern.compile("(?is)</Block>\\s*<Parcel>([^<]*)</Parcel>");
					mat = p.matcher(content);
					if (mat.find()){
						parcel = mat.group(1).trim();
						m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), parcel);
					}
				}
				p = Pattern.compile("(?is)</Parcel>\\s*<Unit>([^<]*)</Unit>");
				mat = p.matcher(content);
				if (mat.find()){
					unit = mat.group(1).trim();
					m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
				}
				
				p = Pattern.compile("(?is)</ExtraPropertyInfo>\\s*<Area>([^<]*)</Area>");
				mat = p.matcher(content);
				if (mat.find()){
					area = mat.group(1).trim();
				}
				
				p = Pattern.compile("(?is)<Key>Property\\.Block</Key>\\s*<Value>([^<]*)</Value>");
				mat = p.matcher(content);
				if (mat.find()){
					block = mat.group(1).trim();
				}
				p = Pattern.compile("(?is)<Key>Property\\.Lot</Key>\\s*<Value>([^<]*)</Value>");
				mat = p.matcher(content);
				String lot = "";
				while (mat.find()){
					lot += " " + mat.group(1).trim();
				}
				if (StringUtils.isNotEmpty(lot)){
					lot = LegalDescription.cleanValues(lot, false, true);
					m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
				}
			}
			
			if (countyId != CountyConstants.IL_Will && StringUtils.isNotEmpty(apnSection) && StringUtils.isNotEmpty(block)
					&& StringUtils.isNotEmpty(area) && StringUtils.isNotEmpty(parcel)){
				StringBuffer pin = new StringBuffer(StringUtils.leftPad(area, 2, "0"));
				pin.append(StringUtils.leftPad(apnSection, 2, "0"));
				pin.append(StringUtils.leftPad(block, 3, "0"));
				pin.append(StringUtils.leftPad(parcel, 3, "0"));
				if (countyId == CountyConstants.IL_Cook || countyId == CountyConstants.IL_Lake){
					pin.append(StringUtils.leftPad(unit, 4, "0"));
				}
				m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pin.toString());
			}
			
			String amount = StringUtils.defaultString(findFastNodeValue(node, "LoanAmount"));
			m.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount);
			if (StringUtils.isEmpty(amount)){
				amount = StringUtils.defaultString(findFastNodeValue(node, "CourtAmount"));
			}
			if (StringUtils.isEmpty(amount)){
				amount = StringUtils.defaultString(findFastNodeValue(node, "EstimatedPurchaseAmount"));
			}
			
			amount = amount.replaceAll("[\\$,]+", "");
			m.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
			
			String docInfo = StringUtils.defaultString(findFastNodeValue(node, "Image/Item/DocInfo"));
			docInfo = docInfo.replaceAll(";", "&");
			
			StringBuffer imageId = new StringBuffer("");
			
//			imageId.append(",");imageId.append("Type=").append("REC");//ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(docInfo, "Doc"));
//			imageId.append(",");
//			imageId.append("SubType=").append("ALL");//StringUtils.defaultString(findFastNodeValue(node, "Image/Item/SubType")));
//			imageId.append(",");
			
			imageId.append("Year@").append(ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(docInfo, "Year"));
			imageId.append(",");
			if (docInfo.contains("Inst")){
				imageId.append("Inst@").append(ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(docInfo, "Inst"));
				m.put(SaleDataSetKey.INSTR_CODE.getKeyName(), imageId.toString());
			}
			
			String remarks = StringUtils.defaultString(findFastNodeValue(node, "PropertyRemark"));
			
			remarks = remarks.replace(((char) 254), ' ');
			remarks = remarks.replaceAll("&#254;", " ");
			remarks = remarks.replaceAll("&thorn;", " ");
			
			m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), remarks);
			
			String situsAddress = StringUtils.defaultString(findFastNodeValue(node, "DocumentAddresses/Address/SitusAddress"));
			if (StringUtils.isNotEmpty(situsAddress)){
				situsAddress = situsAddress.replaceAll("(?is)\\bCREEK CR\\b", "CREEK CIR");//2012R0013218 McHenry
				m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(situsAddress));
				m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(situsAddress));
			}
			
			
//			try {
//				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(remarks.trim())){
//			       FileWriter fw = new FileWriter(new File("D:\\IL PI remarks.txt"),true);
//			       fw.write(remarks);
//				   fw.write("\n");
//			       fw.close();
//				}
//			   }
//			   catch (IOException e) {
//			       e.printStackTrace();
//			   }
			
			try {
				if (dataSite.getStateFIPS().equals(StateContants.IL_STRING_FIPS)){
					ILParsingPI.parsingRemarks(m, searchId);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			try {
				parseNames(m, node, resultType, searchId);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			String refInstrNo = StringUtils.defaultString(findFastNodeValue(node, "ReferencedDocuments/DocumentIdentification/InstrumentNumber"));
			String refBookPage = StringUtils.defaultString(findFastNodeValue(node, "ReferencedDocuments/DocumentIdentification/BookPage"));
			if (StringUtils.isNotEmpty(refInstrNo) || StringUtils.isNotEmpty(refBookPage)){
				String refRecordingDate = StringUtils.defaultString(findFastNodeValue(node, "ReferencedDocuments/DocumentIdentification/RecordingDate"));
				
				String year = "";
				if (StringUtils.isNotEmpty(refRecordingDate)){
					year = refRecordingDate.substring(0, 4);
				}
				
				if (dataSite.getStateFIPS().equals(StateContants.IL_STRING_FIPS)){
					refInstrNo = ILParsingPI.correctInstrumentNumber(crtState, crtCounty, serverDocType, refInstrNo, refRecordingDate);
					
					Set<List> allLines = new LinkedHashSet<List>();
					
					List<List> body = new ArrayList<List>();
					List line = new ArrayList<String>();
					line.add(refInstrNo);
					line.add("");
					line.add("");
					line.add(year);
					
					ResultTable crs = (ResultTable) m.get("CrossRefSet");
					if (crs != null){
						String[][] bodyCRS = crs.getBodyRef();
						for (String[] bodyLine : bodyCRS) {
							List newLine = new ArrayList<String>();
							for (String string : bodyLine) {
								newLine.add(string);
							}
							
							allLines.add(newLine);
						}
						allLines.add(line);
					} else {
						body.add(line);
					}
					
					if (!allLines.isEmpty()){
						allLines.add(line);
						for (List list : allLines) {
							body.add(list);
						}
					}
					if(!body.isEmpty()) {
						String[] header = { "InstrumentNumber", "Book", "Page", "Year" };
						m.put("CrossRefSet", GenericFunctions2.createResultTable(body, header));
					}
				}
			}
		} else {
			String book = StringUtils.defaultString(findFastNodeValue(node, "Book"));
			String page = StringUtils.defaultString(findFastNodeValue(node, "Page"));
			String instrNo = StringUtils.defaultString(findFastNodeValue(node, "DocumentNumber"));
			String serverDocType = StringUtils.defaultString(findFastNodeValue(node, "DocumentType"));
			String recordingDate = StringUtils.defaultString(findFastNodeValue(node,"RecordingDate"));
			String instrumentDate = StringUtils.defaultString(findFastNodeValue(node,"ContractDate"));
			String certificationDate = StringUtils.defaultString(findFastNodeValue(node,"ThroughDate"));
			String typeCD = StringUtils.defaultString(findFastNodeValue(node,"TypeCD"));
			String fullLegal = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/LegalDescription"));
			String amount = StringUtils.defaultString(findFastNodeValue(node, "Loan1Amount"));
			if(StringUtils.isBlank(amount)){
				amount = StringUtils.defaultString(findFastNodeValue(node, "LoanAmount"));
			}
			
			String oeRating = StringUtils.defaultString(findFastNodeValue(node, "OeRating"));		
			
			m.put(SaleDataSetKey.OE_RATING.getKeyName(), oeRating);
			
			String searcherExtensions = "";
			
			try {
				setCertificationDate(certificationDate);
			} catch (Exception e) {
				logger.error("Could not parse Certification Date on " + this.toString() + ", searchid = " + searchId);
			}
			
			if (O_E_PI_TYPE.equalsIgnoreCase(resultType)){
				String legalBrief = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/LegalBriefDescription"));
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(legalBrief)){
					fullLegal += " " + legalBrief; //" # " + legalBrief;
				}
				String legalSectional = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/SecTwnshipRange"));
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(legalSectional)){
					fullLegal += " " + legalSectional; //" # " + legalSectional;
				}
				m.put("tmpPropertyIdentificationSet.PropertyDescription", fullLegal);
				if(StringUtils.isBlank(serverDocType)){
					if("1".equals(typeCD)){
						serverDocType = "TRANSFER";
					}else if("0".equals(typeCD)){
						serverDocType = "MORTGAGE";
					}
				}
				/*
				try {
					if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(fullLegal.trim())){
				       FileWriter fw = new FileWriter(new File("D:\\Duval PI legal APN.txt"),true);
				       fw.write(fullLegal);
					   fw.write("\n");
				       fw.close();
					}
				   }
				   catch (IOException e) {
				       e.printStackTrace();
				   } 
					
					*/
			}
			
			if(MRTG_PI_TYPE.equalsIgnoreCase(resultType)){
				int pos = serverDocType.indexOf('/');
				if(pos>0 && pos+1<serverDocType.length()){
					serverDocType = serverDocType.substring(pos+1);
				}
				instrNo = StringUtils.defaultString(findFastNodeValue(node, "DocID"));
				pos = instrNo.indexOf(';');
				if(pos>0){
					if(instrNo.length()>pos+1){
						String bookPage = instrNo.substring(pos+1);
						pos = bookPage.indexOf('-');
						if(pos>0 && pos+1<bookPage.length()){
							book = bookPage.substring(0,pos);
							page = bookPage.substring(pos+1);
						}
					}
					instrNo = instrNo.substring(0,pos);
				}
				
				if( instrNo.equalsIgnoreCase(book+"-"+page) ){
					instrNo = "";
				}			
			}
			
			if (LINE_ITEMS.equalsIgnoreCase(resultType)){
				instrNo = StringUtils.defaultString(findFastNodeValue(node, "DocID"));
				if( instrNo.equalsIgnoreCase(book+"-"+page) ){
					instrNo = "";
				}
				serverDocType = StringUtils.defaultString(findFastNodeValue(node, "DocTypeName"));
				recordingDate = StringUtils.defaultString(findFastNodeValue(node,"Date"));
				fullLegal += " " + StringUtils.defaultString(findFastNodeValue(node, "SearcherAssociatedLegals"));
				fullLegal += " # " + StringUtils.defaultString(findFastNodeValue(node, "Remarks"));
				searcherExtensions = StringUtils.defaultString(findFastNodeValue(node, "SearcherExtensions"));
				m.put("SaleDataSet.SearcherExtensions", searcherExtensions.trim());
				m.put("tmpPropertyIdentificationSet.PropertyDescription", fullLegal);
	
				/*
				try {
					if (fullLegal.trim().matches("\\A#$")){
						fullLegal = "";
					}
					if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(fullLegal.trim())){
				       FileWriter fw = new FileWriter(new File("D:\\Duval PI legal remarks.txt"),true);
				       fw.write(fullLegal);
					   fw.write("\n");
				       fw.close();
					}
				   }
				   catch (IOException e) {
				       e.printStackTrace();
				   }*/
			}
			
			String docType = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			String docSubType = DocumentTypes.getDocumentSubcategory(serverDocType, searchId);
			String imageId = StringUtils.defaultString(findFastNodeValue(node,"Image"));
			
			String block = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/Block"));
			String lot = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/LotNumber"));
			String platDocNumber = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/MapRef"));
			
			String cityMuniTwp = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/CityMuniTwp"));
			String district = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/District"));
			String landLotField = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/LandLot"));
			String lotCodeField = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/LotCode"));
			
			String sectionField = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/Section"));
			String phaseField = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/PhaseNumber"));
			String strField = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/SecTwnshipRange"));
			String tractField = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/TractNumber"));
			
			String platBook = "";
			String platPage = "";
			String subdivUnit = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/Unit"));
			String subdivName = StringUtils.defaultString(findFastNodeValue(node, "LegalDescriptionInfo/Subdivision"));
			
			List<InstrumentI> allReferences = new ArrayList<InstrumentI>();
			
			String mortgInstrument = StringUtils.defaultString(findFastNodeValue(node, "MortDoc"));
			if(StringUtils.isNotBlank(mortgInstrument)){
				InstrumentI inst = new Instrument();
				Matcher mat = PAT_MORTDOC_BP.matcher(mortgInstrument);
				inst.setDocSubType("MORTGAGE");
				inst.setDocType("MORTGAGE");
				if(mat.find()){
					String book1 = mat.group(1);
					String page1 = mat.group(2);
					inst.setBook(book1);
					inst.setPage(page1);
					allReferences.add(inst);
				}else{
					inst.setInstno(mortgInstrument);
					allReferences.add(inst);
				}
			}
			
			//extract the PB and PP from MapRef
			Matcher matMapRef =  PAT_MAP_REF.matcher(platDocNumber);
			if(matMapRef.find()){
				platBook = matMapRef.group(1);
				platPage = matMapRef.group(2);
				platDocNumber = "";
			}
			
			try {			 
				m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
				m.put(SaleDataSetKey.BOOK.getKeyName(), book);
				m.put(SaleDataSetKey.PAGE.getKeyName(), page);
				m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
				m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordingDate);
				m.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrumentDate);
				m.put(SaleDataSetKey.INSTR_CODE.getKeyName(), imageId);
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(amount)){
					amount = amount.replaceAll("(?is)[\\$,]+", "");
					m.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount);
					m.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
				}
				Pattern p = Pattern.compile("(?is)\\b(SEC)\\s*(\\d+)\\s+TWN\\s*(\\d+[A-Z]?)\\s+RNG\\s*(\\d+[A-Z]?)\\b");
				
				if(StringUtils.isNotBlank(subdivName)||StringUtils.isNotBlank(platBook)||StringUtils.isNotBlank(platDocNumber)
						||StringUtils.isNotBlank(block)||StringUtils.isNotBlank(lot)){
					Vector pisVector = new Vector();
					PropertyIdentificationSet pis = new PropertyIdentificationSet();
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(), subdivName);
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), StringUtils.stripStart(lot, "0"));
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), StringUtils.stripStart(block, "0"));
					pis.setAtribute(PropertyIdentificationSetKey.PLAT_INSTR.getShortKeyName(), platDocNumber);
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), subdivUnit);
					pis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), platBook);
					pis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), platPage);
					
					Matcher ma = p.matcher(strField);
					if (ma.find()){
						pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), 
										StringUtils.stripStart(ma.group(2), "0"));
						pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), 
										StringUtils.stripStart(ma.group(3), "0"));
						pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), 
										StringUtils.stripStart(ma.group(4), "0"));
					}
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tractField);
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getShortKeyName(), phaseField);
					
					pisVector.add(pis);
					m.put("PropertyIdentificationSet", pisVector);
				}
				if (O_E_PI_TYPE.equalsIgnoreCase(resultType)){
					try {
						FLParsingPI.parsingLegalForApnAndAddressSearch(m, searchId);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} else if(FULL_PI_TYPE.equalsIgnoreCase(resultType)){
					try {
						FLParsingPI.parsingLegalForNameSearch(m, searchId);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				parseReferences( m, allReferences);
		
			}catch(Exception e) {
				e.printStackTrace();
			}
			parseNames(m, node, resultType, searchId);
		}
		
	}
	
	public void parseNames(ResultMap m, Node node, String resultType, long searchId){
		
		ArrayList<String> grantor = new ArrayList<String>();
		ArrayList<String> grantee = new ArrayList<String>();
		String amount = StringUtils.defaultString(findFastNodeValue(node, "LoanAmount"));
		if (StringUtils.isEmpty(amount)){
			amount = StringUtils.defaultString(findFastNodeValue(node, "CourtAmount"));
			amount = amount.replaceAll("[\\$,]+", "");
		}
		try{		
			if (FULL_PI_TYPE.equals(resultType)){
					
				NodeList allChilds = node.getChildNodes();
				for(int i = 0; i < allChilds.getLength(); i++){
					Node n = allChilds.item(i);
					if(n.getNodeName().trim().toUpperCase().endsWith("PARTIES")){
						allChilds = n.getChildNodes();
						break;
					}
				}
				if (allChilds != null){
					for(int i = 0; i < allChilds.getLength(); i++){
						Node n = allChilds.item(i);
						if(n.getNodeName().trim().toUpperCase().endsWith("DOCUMENTPARTY")){
							boolean isGrantor = false;
							for (Node child : getChildren(n)){
								String childName = child.getNodeName();
								String childValue = child.getFirstChild().getNodeValue();
								if ("PartyType".equals(childName) && "1".equals(childValue)){
									isGrantor = true;
								} 
								if ("Name".equals(childName) && isGrantor){
									if (StringUtils.isNotBlank(childValue)){
										childValue = childValue.replaceFirst("\\b" + Matcher.quoteReplacement(amount) + "\\b", "").trim();
										if (StringUtils.isNotBlank(childValue)){
											grantor.add(childValue);
										}
									}
								} else if ("Name".equals(childName) && !isGrantor){
									if (StringUtils.isNotBlank(childValue)){
										childValue = childValue.replaceFirst("\\b" + Matcher.quoteReplacement(amount) + "\\b", "").trim();
										if (StringUtils.isNotBlank(childValue)){
											grantee.add(childValue);
										}
									}
								}
							}
						}
					}
				}
				if (dataSite.getStateFIPS().equals(StateContants.IL_STRING_FIPS)){
					ILParsingPI.tokenizeNames(m, grantor, grantee, searchId);
				} else if (dataSite.getStateFIPS().equals(StateContants.CA_STRING_FIPS)){
					CAParsingPI.tokenizeNames(m, grantor, grantee, searchId);
				}
				
			} else {
				String sellerName = StringUtils.defaultString(findFastNodeValue(node, "SellerName"));
				String buyerName = StringUtils.defaultString(findFastNodeValue(node, "BuyerName"));
				String landerName = StringUtils.defaultString(findFastNodeValue(node, "LenderName"));
				String grantorNameSearch = StringUtils.defaultString(findFastNodeValue(node, "Grantor"));
				String granteeNameSearch = StringUtils.defaultString(findFastNodeValue(node, "Grantee"));
				if (StringUtils.isBlank(buyerName)) {
					buyerName = StringUtils.defaultString(findFastNodeValue(node, "Borrower"));
				}
				if (StringUtils.isBlank(landerName)) {
					landerName = StringUtils.defaultString(findFastNodeValue(node, "Lender"));
				}
				if (LINE_ITEMS.equalsIgnoreCase(resultType)) {
					if (StringUtils.isNotBlank(grantorNameSearch)) {
						grantor.addAll(Arrays.asList(grantorNameSearch.replaceAll("(?is)\\bN/A\\b", "").split(";")));
					}
					if (StringUtils.isNotBlank(granteeNameSearch)) {
						grantee.addAll(Arrays.asList(granteeNameSearch.replaceAll("(?is)\\bN/A\\b", "").split(";")));
					}
				} else {
					if (StringUtils.isNotBlank(landerName) && StringUtils.isBlank(sellerName)) {
						grantor.addAll(Arrays.asList(buyerName.replaceAll("(?is)\\bN/A\\b", "").split(";")));
						grantee.addAll(Arrays.asList(landerName.replaceAll("(?is)\\bN/A\\b", "").split(";")));
					} else {
						grantor.addAll(Arrays.asList(sellerName.replaceAll("(?is)\\bN/A\\b", "").split(";")));
						grantee.addAll(Arrays.asList(buyerName.replaceAll("(?is)\\bN/A\\b", "").split(";")));
					}
				}
				
				tokenizeNames(m, grantor, grantee);
			}
			
		} catch (Exception e) {
			logger.error("Exception in name parsing on PI - searchId = " + searchId);
		}
	}
		
	public static void tokenizeNames(ResultMap m, ArrayList<String> grantor, ArrayList<String> grantee) throws Exception{

		String grantorString = "";
		String[] suffixes, type, otherType;
		ArrayList<List> allGrantors = new ArrayList<List>();
		for (String gtor : grantor) {

			String first = "";
			String middle = "";
			String last = "";
			gtor = repairString(gtor);

			if (NameUtils.isCompany(gtor)){
				gtor = gtor.replaceAll(",\\s*$", "");
				gtor = gtor.replaceAll("^,\\s*", "");
				last = gtor.trim();
			} else {
				gtor = gtor.replaceAll("(?is)\\s+([IV]{1,3}),(.*)", ", $2 $1");
				int pos = 0;
				if ((pos = gtor.indexOf(',')) > 0){
					last = gtor.substring(0, pos);
					first = gtor.substring(pos + 1).trim();
					String words[] = first.split(" ");
					if (words.length > 1) {
						first = words[0];
						for (int i = 1; i < words.length; i++){
							middle += words[i] + " ";
						}
						middle = middle.trim();
					}
				} else{
					last = gtor;
				}
			}

			String names[] = { first, middle, last, "", "", "" };
			if (NameUtils.isNotCompany(gtor) && (gtor.contains("&") || !gtor.contains(","))) {//LINDSAY, STANLEY & CYNTHIA ||  WYNN MAURICE
					
				names = StringFormats.parseNameNashville(gtor, true);
					
			}
			grantorString += first + " " + middle + " " + last + " / ";

			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			
			GenericFunctions.addOwnerNames(gtor, names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), allGrantors);

		}
		m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantorString);
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(allGrantors, true));
			
		String granteeString = "";
		ArrayList<List> allGrantees = new ArrayList<List>();
		for (String gtee : grantee) {
			String first = "";
			String middle = "";
			String last = "";
			gtee = repairString(gtee);
			if (NameUtils.isCompany(gtee)){
				gtee = gtee.replaceAll(",\\s*$", "");
				gtee = gtee.replaceAll("^,\\s*", "");
				last = gtee.trim();
			} else {
				int pos = 0;
				if ((pos = gtee.indexOf(',')) > 0){
					last = gtee.substring(0, pos);
					first = gtee.substring(pos + 1).trim();
					String words[] = first.split(" ");
					if (words.length > 1){
						first = words[0];
						for (int i = 1; i < words.length; i++){
							middle += words[i] + " ";
						}
						middle = middle.trim();
					}
				} else{
					last = gtee;
				}
			}

			String names[] = { first, middle, last, "", "", "" };
			if (NameUtils.isNotCompany(gtee) && (gtee.contains("&") || !gtee.contains(","))) {//LINDSAY, STANLEY & CYNTHIA ||  WYNN MAURICE

				names = StringFormats.parseNameNashville(gtee, true);
			}
			granteeString += first + " " + middle + " " + last + " / ";

			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);

			GenericFunctions.addOwnerNames(gtee, names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), allGrantees);

		}
		m.put(SaleDataSetKey.GRANTEE.getKeyName(), granteeString);
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(allGrantees, true));
		
	}
	
	public static String repairString(String name){
		
		if (ro.cst.tsearch.utils.StringUtils.isEmpty(name))
			return "";
		
		name = name.replaceAll("(?is)\\bD\\s*/?\\s*B\\s*/?\\s*A\\b", "");
		name = name.replaceAll("(?is)\\bAKA\\b", "");
		name = name.replaceAll("(?is)\\s+ET\\s+AL\\b", " ETAL ");
		name = name.replaceAll("(?is)\\s+ET\\s+UX\\b", " ETUX ");
		name = name.replaceAll("(?is)\\s+ET\\s+VIR\\b", " ETVIR ");

		return name.trim();
	}
	
	public String buildHtml(Node node, int moduleIDX, String resultType) throws XMLStreamException, FactoryConfigurationError, IOException {
		StringBuilder html = new StringBuilder();
		try{
			
			String content = XmlUtils.createXML(node);
			content = content.replaceAll("tit:", "");
			content = content.replaceAll("(?i)[ ]+xmlns:ns[0-9]?=\"[^\"]+\"[ ]*", "");
			content = content.replaceAll("(?i)[ ]+xmlns=\"[^\"]+\"[ ]*", "");
			content = content.replaceAll("(?i)[ ]*ns[0-9]?:[ ]*", "");
			for(int i=0;i<5;i++){
				content = content.replaceAll("(?i)<[^>/]+>[ \n\r\t]*<[/][^>]+>", "");
			}
			content = content.replaceAll("(?i)[ \n\r\t]*<[^>]+[/]>[ \n\r\t]*", "");
			
			//þ'on IL PI
			content = content.replace(((char) 254), ' ');
			content = content.replaceAll("&#254;", " ");
			content = content.replaceAll("&thorn;", " ");
			
			content =  htmlRepresentation(XmlUtils.parseXml(content,"UTF-8"), "");
			
			content = content.replaceAll("(?i)<br><br>", "<br>");
			content = content.replaceAll("(?i)&gt;(&nbsp;)+&lt;","&gt;&lt;");
			content  = content.replace("![CDATA[", "");
			content  = content.replace("]]", "");
			content = content.replace("&lt;text&gt;", "&lt;text&gt;<table border=\"1\" width=\"98%\"><tr><td><font color=\"blue\">");
			content = content.replace("&lt;/text&gt;", "</font></td></tr></table>&lt;/text&gt;");
			
			StringBuilder newContent = new StringBuilder();
			
			int start = 0;
			int stop = 0;
			int oldStart = 0;
			while( (start=content.indexOf("&lt;text&gt;",start))>0  
					&& 
					(stop=content.indexOf("&lt;/text&gt;",stop))>0 ){
				String beforeStrat = content.substring(oldStart,start+12);
				String between = content.substring(start+12,stop+13);
				newContent.append(beforeStrat);
				newContent.append(between.replace("\n", "<br>").replace("&nbsp;", ""));
				start = stop;
				oldStart = stop;
				stop++;
			}
			if(stop<content.length()){
				newContent.append(content.substring(stop));
			}
			
			content = newContent.toString();
			html.append("<table border=\"1\">");
			html.append("<tr>");
			html.append("<td>");
			html.append(content);
			
			html.append("</td>");
			html.append("</tr>");
			html.append("</table>");
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		String parserName = dataSite.getParserFilenameSufix();
		
		if (LINE_ITEMS.equalsIgnoreCase(resultType)){
			parserName+="_name";
		}
		
		if (MRTG_PI_TYPE.equalsIgnoreCase(resultType)){
			parserName+="_mortgage";
		}
		if (FULL_PI_TYPE.equalsIgnoreCase(resultType.trim())){
			parserName+="_full";
		}
		
		if (O_E_PI_TYPE.equalsIgnoreCase(resultType.trim()) 
				&& ("FullPI".equalsIgnoreCase(parserName) || "CAGenericPI".equalsIgnoreCase(parserName))){
			parserName = "GenericPI";
		}
		
		return /*html   +*/ 	XmlUtils.applyTransformation(createDocument(node), FileUtils.readFileToString(
				new File(RESOURCE_FOLDER + parserName + ".xsl")));
	}
	
	public static Document createDocument(Node node){
		return XmlUtils.parseXml("<?xml version=\"1.0\"?>" + XmlUtils.createXML(node).replace("tit:", ""));
	}
	
	private static String htmlRepresentation(Node doc, String prefix){
		
		StringBuilder sb = new StringBuilder();
		String name = doc.getNodeName();
		String value = XmlUtils.getNodeValue(doc);
		
		if(!"".equals(value)){
			sb.append("<br>"); 
			sb.append(prefix); 
			sb.append("&lt;" + name + "&gt;");
			sb.append("<b>&nbsp;"+value+"&nbsp;</b>"); 
			sb.append("&lt;/" + name + "&gt;");
			sb.append("<br>");
		} else{
			sb.append(prefix); 
			sb.append("&lt;" + name + "&gt;");
			for(Node child: XmlUtils.getChildren(doc)){							
				sb.append(htmlRepresentation(child, prefix + "&nbsp;&nbsp;&nbsp;&nbsp;"));
			}
			sb.append(prefix); 
			sb.append("&lt;/" + name + "&gt;");
		}
		return sb.toString();
	}
 
 	public ImageDownloadResponse downloadImageResponse(Map<String, String> params, PropertyInsightImageConn imagesConn){
	 	
 		ImageDownloadResponse imageresponse = null;
		try {
			imageresponse = imagesConn.getDocumentByKey(StringUtils.defaultString(params.get("imageId")));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return imageresponse;
	}
 	
 	protected boolean isTPPCounty(DataSite dataSite){
 		
 		return false;
 	}
}



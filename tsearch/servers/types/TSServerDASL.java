package ro.cst.tsearch.servers.types;
import static ro.cst.tsearch.utils.XmlUtils.applyTransformation;
import static ro.cst.tsearch.utils.XmlUtils.parseXml;
import static ro.cst.tsearch.utils.XmlUtils.xpathQuery;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface;
import ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface.DaslResponse;
import ro.cst.tsearch.database.rowmapper.RequestCount;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.XMLExtractor;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.functions.GenericDASLNDBFunctions;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSServerDASLAdapter.ServerPersonalData;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.ImageUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;

import com.lowagie.text.pdf.PdfReader;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.warning.ResultsLimitPerQueryWarning;

/**
 * @author cristi stochina
 * @author radu bacrau
 */
@SuppressWarnings("deprecation")
public abstract class TSServerDASL extends TSServer {
	
	protected boolean disableImageFromTheOriginalSite = false;
	
	private static Pattern patMultipleImagesNames = Pattern.compile("[<]Document\\s+FileName=\"([^\"]*)\"");
	
	private static Pattern patMultipleProviderImageId = Pattern.compile("(?i)<providerImageId>([^>]+)</providerImageId>");
	
	private static Pattern patProviderId = Pattern.compile("(?i)<ProviderId>([^>]+)</ProviderId>");
	
	public static final long serialVersionUID = 10000003427837240L;
	
	public static final String ILIP_ATTRIBUTE = "ILIP_ATTRIBUTE";

	protected static final String CLASSES_FOLDER = BaseServlet.REAL_PATH
			+ File.separator + "WEB-INF" + File.separator + "classes"
			+ File.separator;
	
	protected static final String RESOURCE_FOLDER = (CLASSES_FOLDER
			+ "resource" + File.separator + "DASL" + File.separator + "generic" + File.separator)
			.replaceAll("//", "/");
	
	protected static final String FAKE_FOLDER = RESOURCE_FOLDER +"fake"+ File.separator;
	
	protected static final String RULES_FOLDER = CLASSES_FOLDER + "rules"
			+ File.separator;

	static private Pattern finalDocLinkPattern = Pattern.compile("DL___([^&]+)&?");

	protected static final String LINK_TO_IMAGE = "LINK_TO_IMAGE";
	protected static final String LINK_TO_IMAGE_REGEX = "(?i)[<][ \t\r\n]*a[ \t\r\n]+href[ \t\r\n]*[=][ \t\r\n]*\"#LINK_TO_IMAGE\"[ \t\r\n]*>[ \t\r\n]*View[ \t\r\n]*Image([^<]*)</a>";
	protected static final String LINK_TO_FINAL_DOCUMENT = "LINK_TO_FINAL_DOCUMENT";
	protected static final String LINK_TO_FINAL_DOCUMENT_REGEX = "(?i)<a[ /t/r/n]*href[ /t/r/n]*=[ /t/r/n]*\"#"
			+ LINK_TO_FINAL_DOCUMENT + "[^>]*>([^<]*)</a>";

	protected static final Pattern patFinalDocumentLink = Pattern
			.compile(LINK_TO_FINAL_DOCUMENT_REGEX);
	protected static final Pattern patImageLink = Pattern
			.compile(LINK_TO_IMAGE_REGEX);

	/**
	 * Create a DASL site instance max once per TSServer
	 */
	private transient DaslConnectionSiteInterface cachedSite = null;

	/**
	 * Immutable class holding the rules for a certain record
	 * 
	 * @author radu bacrau
	 */
	static protected class RecordRules {
		public final int id;
		public final String style;
		public final String shortType;
		public final String longType;
		public final String xpath;
		public final Document rules;
		public final String overrideType;

		public RecordRules(int id, String style, String shortType,
				String longType, String xpath, Document rules,
				String overrideType) {
			this.id = id;
			this.style = style;
			this.shortType = shortType;
			this.longType = longType;
			this.xpath = xpath;
			this.rules = rules;
			this.overrideType = overrideType;
		}
	}

	/* --------- start abstracts methods ----------------- */
	protected abstract ServerPersonalData getServerPersonalData();

	protected abstract void ParseResponse(String moduleIdx,
			ServerResponse Response, int viParseID)
			throws ServerResponseException;

	protected abstract HashMap<String, Object> fillTemplatesParameters(
			Map<String, String> params);

	protected abstract DownloadImageResult saveImage(ImageLinkInPage image)
			throws ServerResponseException;

	protected abstract ServerResponse performRequest(String page,
			int methodType, String action, int parserId, String imageLink, String vbRequest,
			Map<String, Object> extraParams)
			throws ServerResponseException;

	/**
	 * Create the search query by using parameters supplied in parent site or
	 * automatic
	 * 
	 * @param params
	 * @return query, empty if not enough parameters
	 */
	protected abstract String buildSearchQuery(Map<String, String> params,
			int moduleIdx);

	/**
	 * Get the record rules. Load from hdd at first call
	 * 
	 * @return
	 */
	abstract Map<Integer, RecordRules> getRecordRules(int resultType);

	
	protected abstract void updateSearchDataAfterSearch(
			Vector<ParsedResponse> newResultRows, int viParseID);

	protected abstract String modifyXMLResponse(String xml, int moduleIDX);

	protected abstract int changeParserIdBasedOnXMLResponse(String xml,
			int moduleIDX, int oldParserId);

	protected   String createLinkForImage(HashMap<String, String> values){
			return null;
	}
	/* ------------ end abstracts methods ------------------- */

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 */
	public TSServerDASL(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	/**
	 * @param searchId
	 */
	public TSServerDASL(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	/**
	 * Get connection site
	 * 
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected DaslConnectionSiteInterface getDaslSite() {
		synchronized (this) { 
			// for best efficience wee need to modify this in a way that the synchronized(mutex){ }block to be executed only once
			if (cachedSite == null) {
				Class serverClass = null;
				Class parameterTypes[] = { int.class };
				Object parameters[] = { Integer.valueOf(miServerID) };
				DaslConnectionSiteInterface daslInterface = null;

				try {
					DataSite data = HashCountyToIndex.getDateSiteForMIServerID(
							InstanceManager.getManager().getCommunityId(searchId),
							miServerID);
					String classFullName = data.getClassConnFilename();
					classFullName = "ro.cst.tsearch.connection.dasl."+ classFullName;
					logger.debug(classFullName);
					serverClass = Class.forName(classFullName);
					daslInterface = (DaslConnectionSiteInterface) serverClass.getConstructor(parameterTypes).newInstance(parameters);

				} catch (Exception e) {
					if (e instanceof InvocationTargetException) {
						InvocationTargetException te = (InvocationTargetException) e;
						te.getTargetException().printStackTrace();
					}
					e.printStackTrace();
				}

				cachedSite = daslInterface;
			}
			return cachedSite;
		}
	}

	/**
	 * Remove formatting from html row displayed in intermediate results
	 * 
	 * @param html
	 * @return
	 */
	protected static String removeFormatting(String html) {
		int istart = html.indexOf("<td");
		if (istart == -1) {
			return html;
		}
		istart = html.indexOf("<td", istart + 1);
		if (istart == -1) {
			return html;
		}
		istart = html.indexOf("<td", istart + 1);
		if (istart == -1) {
			return html;
		}
		istart = html.indexOf(">", istart);
		if (istart == -1) {
			return html;
		}
		istart += 1;
		int iend = html.lastIndexOf("</td");
		if (iend == -1) {
			return html;
		}
		if (istart > iend) {
			return html;
		}
		return html.substring(istart, iend);
	}

	/**
	 * Fill a parsed response's infsets, create HTML representation
	 * 
	 * @param item
	 * @return retVal[0] - html format, retVal[1] - short type description
	 */
	protected String[] parse(ParsedResponse item, int parserID) {

		// load record rules
		RecordRules rr = getRecordRules(item, parserID);

		// get parsed XML document
		Node doc = (Node) item.getAttribute(ParsedResponse.DASL_RECORD);

		// create html response & fill infsets
		String itemHtml = applyTransformation( doc, rr.style);
		
		try{
			Document docum = XmlUtils.createDocument(doc);
	    	XMLExtractor xmle = new XMLExtractor(rr.rules, docum, searchId);
	    	xmle.process(); 
	    	ResultMap resultMap = xmle.getDefinitions();
	    	if (itemHtml.contains("<b> Tax&nbsp;Document</b>") && StringUtils.isEmpty((String) resultMap.get("SaleDataSet.DocumentType"))){
	    		resultMap.put("SaleDataSet.DocumentType", DocumentTypes.COUNTYTAX);
	    	}
	    	
	    	//5391
	    	String src = (String) resultMap.get("OtherInformationSet.SrcType");
	    	if (StringUtils.isNotEmpty(src) && "NB".equals(src)){
	    		resultMap = GenericDASLNDBFunctions.improveCrossRefsParsing(resultMap, getSearch());
	    	}
	    	
	    	parseLegalDescriptions(docum, item, resultMap);
	    	
	    	parseGrantorGrantee(docum, item, resultMap);
	    	
	    	parseAddress(docum, item, resultMap);
	    	
	    	Bridge bridge = new Bridge(item, resultMap, searchId);
	    	item.setDocument(bridge.importData());
    	}catch(Exception e){
    		e.printStackTrace();
    		throw new RuntimeException(e);
    	}
		
		// return
		return new String[] { itemHtml, rr.shortType };
	}
 
	/**
	 * get file name from link
	 */

	protected String getFileNameFromLink(String link) {

		boolean isTax = link.contains("DASLTAXDOCUMENT");
		link = link.replace("DASLTAXDOCUMENT", "");
		
		// try the normal link pattern
		Matcher daslLinkMatcher = finalDocLinkPattern.matcher(link);
		if(isTax){
			serverTypeDirectoryOverride ="County Tay";
		}
		else{
			serverTypeDirectoryOverride = null;
		}
		if (daslLinkMatcher.find()) {
			return daslLinkMatcher.group(1) + ".html";
		}
		if (link.contains("DASLFINAL")) {
			return link;
		}
		throw new RuntimeException("Unknown Link Type: " + link);
	}
 

	public ServerResponse SearchBy(TSServerInfoModule module,
			Object sd) throws ServerResponseException {
		return searchBy(module, sd, null);
	}

	private String extractErrorMessage(String daslResponse) {
		if(daslResponse == null) {
			return "Data Source internal error";
		}
		int startpoz = daslResponse.indexOf("<Message>")+"<Message>".length();
		int endpoz = daslResponse.indexOf("</Message>");
		if (startpoz > 0 && endpoz > 0 && startpoz < endpoz) {
			return daslResponse.substring(startpoz, endpoz);
		}
		
		return "";
	}
	

	protected boolean downloadImageFromOtherSite(String instrument,String book, String page, String year, String type,  ImageLinkInPage image) {
		return false;
	}

	protected DaslResponse  performSearch(Map<String, String> params, int moduleIDX, String fakeResponse){
		String xmlQuery = "";
		DaslResponse daslResponse = null;
		if( fakeResponse == null ){
			// create XML query
			xmlQuery = buildSearchQuery(params, moduleIDX);
			if ( StringUtils.isEmpty(xmlQuery ) ) {
				return null;
			}
			
			mSearch.setAdditionalInfo(RequestCount.REQUEST_COUNT_TYPE, getRequestCountType(moduleIDX));
			daslResponse = getDaslSite().performSearch(xmlQuery, searchId);
			if(daslResponse!=null && !(this instanceof ro.cst.tsearch.servers.types.GenericDASLNDB)){
				if(!StringUtils.isEmpty(daslResponse.certificationDate)){
					String d2 = daslResponse.certificationDate;

					getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(d2));
				}
			}
		}
		else{
			daslResponse = new ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface.DaslResponse();
			daslResponse.status = DaslConnectionSiteInterface.ORDER_COMPLETED;
			daslResponse.xmlResponse = fakeResponse;
			daslResponse.xmlQuery = xmlQuery ;
			daslResponse.isFake = true;
		}
		
		return daslResponse;
	}
	
	protected static boolean isMultipleImagesResultNotNull(DaslResponse daslResponse){
		return ( 
				   daslResponse!=null 
				&& daslResponse.xmlResponse!=null
				&& daslResponse.xmlResponse.indexOf("<Content>")<0 
				&& (daslResponse.xmlResponse.indexOf("<Document FileName=")>0 ||daslResponse.xmlResponse.indexOf("<ImageId>")>0)
				&& daslResponse.xmlResponse.indexOf("<DocumentImage>")>0 
		);
	}
	
	protected static String identifyGoodImageName(String book, String page, String year, String type, List<String> nameList ){
		String imageIdentifier = "" ;
		if( "PLAT".equals(type) || "ASSESORMAP".equals(type) ){
			imageIdentifier = "-ABP-";
		}
		else if("TRACTMAP".equals(type)){
			imageIdentifier = "-TBP-";
		}
		else if("PARCELMAP".equals(type)){
			imageIdentifier = "-PBP-";
		}
		else if("RECORDMAP".equals(type)){
			imageIdentifier = "-RBP-";
		}
		else{
			imageIdentifier = "other";
		}
		for( String fileName: nameList ){
			if(	fileName.contains(imageIdentifier)	){
				return fileName;
			}
		}
		if("other".equals(imageIdentifier)){
			for( String fileName: nameList ){
				if( !( fileName.contains("-ABP-") || fileName.contains("-TBP-") || fileName.contains("-PBP-") || fileName.contains("-RBP-") ) ){
					return fileName;
				}
			}
		}
		return "";
	}
	
	private static String identifyGoodImageName(String docno, String year, String type,List<String> nameList ){
		for( String fileName: nameList ){
			if(	fileName.contains(year+"."+docno+".")	){
				return fileName;
			}
		}
		return "";
	}
	
	protected boolean useIds(String xml){
		if(xml==null){
			xml="";
		}
		Matcher mat = patProviderId.matcher(xml);
		boolean useId = false;
		
		if(mat.find()){
			if("8".equalsIgnoreCase(mat.group(1).trim())){
				useId = true;
			}
		}
		return useId;
	}
	
	protected  DaslResponse downloadImagesUsingImageID(HashMap<String, String> params, DaslResponse daslResponse, int moduleIDX,  boolean isDocNo){
		
		boolean useId = useIds(daslResponse.xmlResponse);
		List<String> nameList = getFileNameList(daslResponse.xmlResponse,useId);
		
		String goodFileName = ( (isDocNo)?identifyGoodImageName( params.get("docno"), params.get("year"), params.get("type"), nameList )
										 :identifyGoodImageName( params.get("book"), params.get("page"),   params.get("year"),params.get("type"), nameList ));
		
		if (!"".equals(goodFileName)) {					//found match
			daslResponse = downloadImageUsingImageID(goodFileName, params, moduleIDX);
			if (daslResponse!=null) {
				return daslResponse;
			} else {
				return new DaslResponse();
			}
		} else {
			for (String eachName: nameList) {			//no match found, try with each image and return the first good image
				DaslResponse newDaslResponse = downloadImageUsingImageID(eachName, params, moduleIDX);
				if (isGoodImage(newDaslResponse)) {
					return newDaslResponse;
				}
			}
			return new DaslResponse();
		}
		
	}
	
	protected List<String> getFileNameList(String xmlResponse,boolean extractIds){
		
		
		ArrayList<String> nameList= new ArrayList<String>();
		
		if(extractIds){
			Matcher mat = patMultipleProviderImageId.matcher(xmlResponse);
			while( mat.find() ){
				nameList.add( mat.group(1) );
			}
		}else{
			Matcher mat = patMultipleImagesNames.matcher( xmlResponse );
			while( mat.find() ){
				nameList.add( mat.group(1) );
			}
		}
		
		return nameList ;
	}
	
	protected DaslResponse downloadImageUsingImageID(String goodFileName, HashMap<String, String> params, int moduleIDX){
		DaslResponse daslResponse = null;
		String DASLImageSearchType = params.get("DASLImageSearchType");
		params = new HashMap<String, String>();
		
		int poz = -1;
		if((poz=goodFileName.lastIndexOf('.'))>0){
			goodFileName = goodFileName.substring(0,poz);
		}
		
		if(!StringUtils.isEmpty(goodFileName)){
			params.put("DASLimageId", goodFileName);
			params.put("DASLImageSearchType", DASLImageSearchType );
			params.put("DASLSearchType", "IMG");
			
			// create XML query
			String xmlQuery = buildSearchQuery(params, moduleIDX);
			if ( StringUtils.isEmpty(xmlQuery ) ) {
				return null;
			}
			daslResponse = getDaslSite().performSearch( xmlQuery, searchId );
		}
		
		return daslResponse;
	}
	
	boolean isGoodImage(DaslResponse daslResponse) {
		if (daslResponse!=null) {
			Node doc = XmlUtils.parseXml(daslResponse.xmlResponse);
			ImageLinkInPage ilip = new ImageLinkInPage(false);
			if (decodeAndSaveImage(doc, ilip, "")) {
				return true;
	        }
		}
		return false;
	}
	
	protected static boolean couldNotDownloadImage(DaslResponse daslResponse){
		return (daslResponse==null 
			|| daslResponse.status == DaslConnectionSiteInterface.ORDER_ERROR 
			|| daslResponse.status == DaslConnectionSiteInterface.ORDER_PLACED
			|| daslResponse.status == DaslConnectionSiteInterface.ORDER_REJECTED
			|| daslResponse.xmlResponse.indexOf("<Content>")<0 );
	}
	
	protected DaslResponse  performImageSearch(String book, String page, String docno, String year, String month,
			String day, String type, String isPlat, String DASLImageSearchType, int moduleIDX){
		String xmlQuery = "";
		HashMap<String, String> params = new HashMap<String, String>();
		DaslResponse daslResponse = null;
		
		this.mSearch.setAdditionalInfo(RequestCount.REQUEST_COUNT_TYPE, RequestCount.TYPE_IMAGE_COUNT);
		
		//search with instrument no and type and year
		if(!StringUtils.isEmpty(docno)){
			params.put("docno", docno);
			if( !StringUtils.isEmpty(type) ){
				params.put("type", type);
			}
			params.put("DASLImageSearchType", DASLImageSearchType);
			if( !StringUtils.isEmpty(year) ){
				params.put("year", year);
			}
			if( !StringUtils.isEmpty(month) ){
				params.put("month", month);
			}
			if( !StringUtils.isEmpty(day) ){
				params.put("day", day);
			}
			params.put("isPlat", String.valueOf(isPlat));
			
			// create XML query
			xmlQuery = buildSearchQuery(params, moduleIDX);
			if ( StringUtils.isEmpty( xmlQuery ) ) {
				return null;
			}
			
			this.mSearch.setAdditionalInfo(RequestCount.REQUEST_COUNT_TYPE, RequestCount.TYPE_INSTRUMENT_COUNT);
			daslResponse = getDaslSite().performSearch(xmlQuery, searchId);
			
			//search without year
			if( couldNotDownloadImage(daslResponse) ){
				params.remove("year");
				params.remove("month");
				params.remove("day");
				
				// create XML query
				xmlQuery = buildSearchQuery(params, moduleIDX);
				if ( StringUtils.isEmpty( xmlQuery ) ) {
					return null;
				}
				
				this.mSearch.setAdditionalInfo(RequestCount.REQUEST_COUNT_TYPE, RequestCount.TYPE_INSTRUMENT_COUNT);
				daslResponse = getDaslSite().performSearch(xmlQuery, searchId);
			}
			
			if( isMultipleImagesResultNotNull( daslResponse ) ){
				daslResponse = downloadImagesUsingImageID(params, daslResponse, moduleIDX, true);
			}
		}
		
		if( couldNotDownloadImage(daslResponse) ){
			//search with B P and type and year  
			if(!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page)){
				params = new HashMap<String, String>();
				params.put("book", book);
				params.put("page", page);
				params.put("type", type);
				params.put("DASLImageSearchType", DASLImageSearchType);
				params.put("DASLSearchType", "IMG");
				if(!StringUtils.isEmpty(year)){
					params.put("year", year);
				}
				params.put("isPlat", String.valueOf(isPlat));
				
				// create XML query
				xmlQuery = buildSearchQuery(params, moduleIDX);
				if ( StringUtils.isEmpty(xmlQuery ) ) {
					return null;
				}
				daslResponse = getDaslSite().performSearch(xmlQuery, searchId );
				
				if(couldNotDownloadImage(daslResponse)){//TRY again without year
					params = new HashMap<String, String>();
					params = new HashMap<String, String>();
					params.put("book", book);
					params.put("page", page);
					params.put("type", type);
					params.put("DASLImageSearchType", DASLImageSearchType);
					params.put("DASLSearchType", "IMG");
					params.put("year", "");
					params.put("isPlat", String.valueOf(isPlat));
					
					// create XML query
					xmlQuery = buildSearchQuery(params, moduleIDX);
					if ( StringUtils.isEmpty( xmlQuery ) ) {
						return null;
					}
					daslResponse = getDaslSite().performSearch(xmlQuery, searchId);
				}
				
				if( isMultipleImagesResultNotNull( daslResponse ) ){
					daslResponse = downloadImagesUsingImageID(params, daslResponse, moduleIDX, false);
				}
				
				if(daslResponse==null){
					throw new RuntimeException("Could not find the Image");
				}
				boolean useId = useIds(daslResponse.xmlResponse);
				List<String> nameList = getFileNameList( daslResponse.xmlResponse ,useId);
				if( nameList.size()>0 && StringUtils.isEmpty( identifyGoodImageName(book, page, year, type, nameList ) ) ){
					throw new RuntimeException("Could not find the Image");
				}
				
			}
		}
		
		return daslResponse;
		
	}
	
	protected  boolean  decodeAndSaveImage(Node doc,ImageLinkInPage ilip, String imageName){
		
		String fileName = ilip.getPath();
		
		if(mSearch == null){
			mSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		}
		
		NodeList nl1 = xpathQuery(doc,"//Document");
		if(nl1.getLength() == 0){
			return false;
		}
		Node node = nl1.item(nl1.getLength() - 1);
		Node content = node.getFirstChild();
		if(content==null){
			return false;
		}
		
		if( StringUtils.isEmpty(fileName) ){
			
			String ext = "";
			
			String itemFileName = node.getAttributes().getNamedItem("FileName").getNodeValue();
			Node n= node.getAttributes().getNamedItem("FileType");
        	
        	String type ="";
        	if(n!=null){
        		type = n.getNodeValue();
        	}
        	
        	if("tff".equalsIgnoreCase(type) || "tiff".equalsIgnoreCase(type) || itemFileName.toLowerCase().endsWith("tif") || itemFileName.toLowerCase().endsWith("tiff")){
        		ilip.setContentType("image/tiff");
        		ext = "tif";
        	}
        	else if("pdf".equalsIgnoreCase(type) || itemFileName.toLowerCase().endsWith("pdf")){
        		ilip.setContentType("application/pdf");
        		ext = "pdf";
        	}
			
			String imagedirs = mSearch.getImagesTempDir();
        	FileUtils.CreateOutputDir(imagedirs);
        	if (!"".equals(imageName)) {
        		itemFileName = imageName + "." + ext;
        	}
        	fileName = imagedirs + itemFileName;
        	ilip.setPath(fileName);
        	
		}
		
		String coded = XmlUtils.getNodeCdataOrText(content);
		XmlUtils.decodeBase64(coded, fileName);
		
		if(FileUtils.existPath(fileName)){
			String contentType = ilip.getContentType();
			if (contentType.contains("pdf")) {
        		PdfReader reader = null;
        		try {
        			reader = new PdfReader(fileName);
        		} catch (Exception e) {}
        		if (reader!=null) {
        			return true;
        		} else {
        			FileUtils.deleteFile(fileName);
        		}
        	} else if (contentType.contains("tif")) {
        		byte[] data = null;
        		try {
        			data =  FileUtils.readBinaryFile(fileName);
        		} catch (Exception e) {}
        		if (data!=null) {
        			if (ImageUtils.checkTIFFImage(data)) {
        				return true;
	        		} else {
	        			FileUtils.deleteFile(fileName);
	        		}
        		}		
        	}
		}
		
		return false;
	}
	

	protected ServerResponse searchBy(Object sd, List<String> fakeResponses,TSServerInfoModule... modules) throws ServerResponseException {
		return searchByMultipleInstrument(Arrays.asList(modules), sd, fakeResponses);
	}
	
	/**
	 * Search by multiple instrument numbers
	 * @param modules
	 * @param sd
	 * @param fakeResponse
	 * @return
	 * @throws ServerResponseException
	 */
	@SuppressWarnings("rawtypes")
	protected ServerResponse searchByMultipleInstrument(List<TSServerInfoModule> modules, Object sd, List<String> fakeResponses) throws ServerResponseException {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		if(modules.isEmpty()) {
			return ServerResponse.createErrorResponse("No modules to search by");
		}
		List<DaslResponse> daslResponses = new ArrayList<DaslResponse>();

		TSServerInfoModule firstModule = modules.get(0);
		int moduleIDX = firstModule.getModuleIdx();
		int parserID = firstModule.getParserID();

		
		Map<InstrumentI, DocumentI> temporaryDocuments = new HashMap<InstrumentI, DocumentI>();
		
		for(int i = 0 ; i < modules.size() ; i++ ) {
			
			try {
				TSServerInfoModule mod = modules.get(i); 
				
				String fakeResponse = null;
				
				if(fakeResponses != null && fakeResponses.size() > i) {
					fakeResponse = fakeResponses.get(i);
				}
				
				if(fakeResponse != null || verifyModule(mod)){
					
					if( mod.getModuleIdx() != TSServerInfo.IMG_MODULE_IDX 
							&& (!Boolean.TRUE.equals(mod.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS)))){
						global.removeAllInMemoryDocs();
					}
					
					global.clearClickedDocuments();
					if(fakeResponse==null){
						// log the search in the SearchLogger
						logSearchBy(mod);
					}
					// get search parameters
					Map<String, String> params = getNonEmptyParams(mod, null);
			
					DaslResponse res = performSearch(params, moduleIDX, fakeResponse);
					
					if("IM".equals(getDataSite().getSiteTypeAbrev())){
						res.setAttribute("TSServerInfoModule", mod);
					}
					
					daslResponses.add(res);
					
					if(fakeResponse==null && !"IM".equals(getDataSite().getSiteTypeAbrev())) {
						ServerResponse serverResponseSingleSearch = processDaslResponse(res,moduleIDX,parserID);
						
						if(isRelatedModule(firstModule)) {
							Map<InstrumentI, DocumentI> processRelated = processRelated(mod, serverResponseSingleSearch);
							if(processRelated != null) {
								temporaryDocuments.putAll(processRelated);
							}
						}
						
					}
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
					
		}
		
		DaslResponse daslResponse = mergeResults(daslResponses);
		//this is not cool and will not work in case of multiple names searched in the same module
		if(daslResponse == null) {
			return ServerResponse.createErrorResponse("No response available to parse!");
		}
		daslResponse.setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, firstModule);
		ServerResponse serverResponseFinal = processDaslResponse(daslResponse,moduleIDX,parserID,false);
		
		
		
		if(isRelatedModule(firstModule)) {
			
			Vector resultRows = serverResponseFinal.getParsedResponse().getResultRows();
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
		
		
		return serverResponseFinal;
	}

	protected boolean isRelatedModule(TSServerInfoModule module) {
		return false;
	}

	protected Map<InstrumentI, DocumentI> processRelated(TSServerInfoModule module, ServerResponse serverResponse) {
		return null;
	}
	
	protected boolean verifyModule(TSServerInfoModule mod) {

		try {
			if (mod.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
				TSServerInfoFunction f = null;
				TSServerInfoFunction m = null;
				TSServerInfoFunction l = null;

				for (TSServerInfoFunction func : mod.getFunctionList()) {
					if (func.getParamName().equals("firstName1"))
						f = func;
					if (func.getParamName().equals("middleName1"))
						m = func;
					if (func.getParamName().equals("lastName1"))
						l = func;
					if (f != null && m != null && l != null)
						break;
				}

				if (StringUtils.isNotEmpty(l.getParamValue())) {
					return true;
				}
				return false;
			}

			if (mod.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) {
				TSServerInfoFunction b = null;
				TSServerInfoFunction p = null;

				for (TSServerInfoFunction func : mod.getFunctionList()) {
					if (func.getParamName().equals("book"))
						b = func;
					if (func.getParamName().equals("page"))
						p = func;
					if (b != null && p != null)
						break;
				}
							
				if (StringUtils.isNotEmpty(b.getParamValue()) && StringUtils.isNotEmpty(p.getParamValue())) {
					return true;
				}
				return false;
			}

			if (mod.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
				TSServerInfoFunction i = null;
				TSServerInfoFunction y = null;

				for (TSServerInfoFunction func : mod.getFunctionList()) {
					if (func.getParamName().equals("docno"))
						i = func;
					if (func.getParamName().equals("year"))
						y = func;
					if (i != null && y != null)
						break;
				}
				
				if (StringUtils.isNotEmpty(i.getParamValue())) {
					if (y == null)
						return true;
					else if (StringUtils.isNotEmpty(y.getParamValue()))
						return true;
				}
				return false;
			}

			if (mod.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX) {
				TSServerInfoFunction b = null;
				TSServerInfoFunction p = null;
				TSServerInfoFunction i = null;

				for (TSServerInfoFunction func : mod.getFunctionList()) {
					if (func.getParamName().equals("book"))
						b = func;
					if (func.getParamName().equals("page"))
						p = func;
					if (func.getParamName().equals("docno"))
						i = func;
					if (b != null && p != null && i != null)
						break;
				}
				if (StringUtils.isNotEmpty(b.getParamValue()) || StringUtils.isNotEmpty(p.getParamValue())
						|| StringUtils.isNotEmpty(i.getParamValue())) {
					return true;
				}
				return false;
			}
			
			if (mod.getModuleIdx() == TSServerInfo.RELATED_MODULE_IDX) {
				TSServerInfoFunction b = null;
				TSServerInfoFunction p = null;
				TSServerInfoFunction i = null;
				
				for (TSServerInfoFunction func : mod.getFunctionList()) {
					if (func.getParamName().equals("book"))
						b = func;
					if (func.getParamName().equals("page"))
						p = func;
					if (func.getParamName().equals("docno"))
						i = func;
				}
				if (StringUtils.isNotEmpty(b.getParamValue()) || 
						StringUtils.isNotEmpty(p.getParamValue()) ||
						StringUtils.isNotEmpty(i.getParamValue())) {
					return true;
				}
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.err.println(this.getClass() + "I shouldn't be here!!!");
		return false;
	}

	protected ServerResponse searchBy(TSServerInfoModule module, Object sd, String fakeResponse) throws ServerResponseException {
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		DaslResponse daslResponse = null;
		
		boolean isMultiplePINSearch = false;
		if (global.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) != null){
			if (Boolean.TRUE.equals(global.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN))){
				isMultiplePINSearch = true;
			}
		}
		if( module.getModuleIdx() != TSServerInfo.IMG_MODULE_IDX 
				&& (!Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS)))
				&& !isMultiplePINSearch){
			global.removeAllInMemoryDocs();
		}
		
		global.clearClickedDocuments();
		if(fakeResponse==null){
			// log the search in the SearchLogger
			logSearchBy(module);
		}
		// get search parameters
		Map<String, String> params = getNonEmptyParams(module, null);

		int moduleIDX = module.getModuleIdx();
		int parserID = module.getParserID();

		
		if (moduleIDX == TSServerInfo.IMG_MODULE_IDX) {
			return imageSearch(module, sd, "");
		}
		else{
			if ( ( daslResponse = performSearch(params, moduleIDX, fakeResponse))==null ){
				String mess = "</br><font color=\"red\">Not enough data entered for a search to be performed!</font></br>";
				SearchLogger.info(mess, searchId);
				return ServerResponse.createErrorResponse("Not enough data entered for a search to be performed!");
			}
		}
		if(daslResponse != null) {
			daslResponse.setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, module);
		}
		ServerResponse serverResponse = processDaslResponse(daslResponse,moduleIDX,parserID);
		
		processRelated(module, serverResponse);
		
		return serverResponse;

	}
	
	protected ServerResponse imageSearch(TSServerInfoModule module, Object sd, String imageName){

		
		if(!(sd instanceof ImageLinkInPage) ){
			throw new RuntimeException("Please call this function with good parameters");
		}
		
		DaslResponse daslResponse = null;
		ImageLinkInPage ilip = (ImageLinkInPage) sd;
		
		// get search parameters
		Map<String, String> params = getNonEmptyParams(module, null);
		
		String book = params.get("book");
		String page = params.get("page");
		String docno = params.get("docno");
		String type = params.get("type");
		String year = params.get("year");
		String isPlatStr = params.get("isPlat");
		String DASLImageSearchType = params.get("DASLImageSearchType");
		String month = params.get("month");
		String day = params.get("day");
		
		if (StringUtils.isEmpty(type)) {
			type = "";
		}
		
		int moduleIDX = module.getModuleIdx();
		
		boolean imageDownloaded = downloadImageFromOtherSite(docno, book, page, year, type, (ImageLinkInPage)sd);
		if (imageDownloaded) {
			//image downloaded from other site 
			mSearch.countRequest(getDataSite().getSiteTypeInt(), RequestCount.TYPE_IMAGE_COUNT);
			
			ServerResponse sr = new ServerResponse();
			sr.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK,FileUtils.readBinaryFile(((ImageLinkInPage)sd).getPath()),
					((ImageLinkInPage)sd).getContentType()));
			return sr;
		}
		
		if(disableImageFromTheOriginalSite){
			//throw new RuntimeException("Could not find the Image");
			String mess = "</br><font color=\"red\">An error was returned by our image provider. Please try your request again later!</font></br>";
			SearchLogger.info(mess, searchId);
			return ServerResponse.createErrorResponse("An error was returned by our image provider. Please try your request again later!");
		}
		
		daslResponse = performImageSearch(book, page, docno, year, month, day, type, isPlatStr, DASLImageSearchType, moduleIDX);
		ServerResponse sr = new ServerResponse();
		try {
			if(daslResponse.xmlResponse!=null){
				Node doc = XmlUtils.parseXml(daslResponse.xmlResponse);
				if( ilip.isJustImageLookUp() ){
					NodeList nl = xpathQuery(doc, "//Content");
					if(nl.getLength() != 0){
						SearchLogger.info("</br><font color=\"black\">Image downloaded with success.</font></br>", searchId);
						sr.setImageResult( new DownloadImageResult( DownloadImageResult.Status.OK, new byte[0], "" ));
						return sr;
					}
				}
				else{
					if( decodeAndSaveImage(doc, ilip, imageName)){
						byte b[] = FileUtils.readBinaryFile( ilip.getPath() );
						SearchLogger.info("</br><font color=\"black\">Image downloaded with success.</font></br>", searchId);
						sr.setImageResult( new DownloadImageResult( DownloadImageResult.Status.OK, b, ilip.getContentType() ));
						return sr;
					}
				}
								
			}
			
			SearchLogger.info("</br><font color=\"red\">Failed to download the image.</font></br>", searchId);
			sr.setImageResult( new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], "" ));
		}finally {
			if (sr.getImageResult() != null){
				afterDownloadImage(sr.getImageResult().getStatus()==DownloadImageResult.Status.OK);
			}
		}
		return sr;
	}
	
	protected ServerResponse processDaslResponse(DaslResponse daslResponse, int moduleIDX , int parserID) throws ServerResponseException {
		return processDaslResponse(daslResponse,moduleIDX,parserID,true);
	}
	
	protected ServerResponse processDaslResponse(DaslResponse daslResponse, int moduleIDX , int parserID, boolean log) throws ServerResponseException {
		if(daslResponse==null ) {
			logInSearchLogger("<font color=\"green\">Order Placed by no response received. Please try again later !</font>", searchId,log);
			ServerResponse res = ServerResponse.createWarningResponse("Order Placed by no response received. Please try again later ! ");
			return res;
		}
		// order not received in time
		if (daslResponse.status == DaslConnectionSiteInterface.ORDER_PLACED ) {
			logInSearchLogger("<font color=\"green\">Order "+ daslResponse.id +"Placed. Please try again later !</font>", searchId,log);
			ServerResponse res = ServerResponse.createWarningResponse("Order "+daslResponse.id+" placed. Please try again later ! ");
			res.getParsedResponse().setAttributes(daslResponse.getAttributes());
			return res;
		}

		// error appeared with order
		if (daslResponse.status == DaslConnectionSiteInterface.ORDER_ERROR) {
			String errorMessage = extractErrorMessage(daslResponse.xmlResponse);
			if(errorMessage.contains("No data found")){
				logInSearchLogger("Found <span class='number'> 0 </span> <span class='rtype'>intermediate</span> results.<br/>",searchId,log);
				return ServerResponse.createEmptyResponse();
			} else { 
				logInSearchLogger("<font color=\"red\">DASL error! Gave up.</font>", searchId,log);
				if (daslResponse.xmlResponse == null) {
					daslResponse.xmlResponse = "Data Source internal error";
				}
				String mess = "</br><font color=\"red\">" + parseErrorMessage(errorMessage) + "</font></br>";
	
				logInSearchLogger(mess, searchId,log);
				return getErrorResponse(mess);
			}
		}
		 
		daslResponse.xmlResponse = modifyXMLResponse(daslResponse.xmlResponse, moduleIDX);
		parserID = changeParserIdBasedOnXMLResponse(daslResponse.xmlResponse, moduleIDX, parserID);
		
		// extract useful information from the received XML
		Map<Integer, RecordRules> rrs = getRecordRules(parserID);
		NodeList[] nlists = new NodeList[rrs.size()];
		try {
			Node xmlDoc = parseXml(daslResponse.xmlResponse);
			for (int i = 0; i < nlists.length; i++) {
				RecordRules rr = rrs.get(i);
				nlists[i] = xpathQuery(xmlDoc, rr.xpath);
			}
		} catch (RuntimeException e) {
			logger.error("DASL parsing exception", e);
			logInSearchLogger("DASL response parse exception", searchId,log);
			return ServerResponse.createErrorResponse("Error parsing the response received from DASL");
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
		additionalInfo.add(moduleIDX);

		
		
		final int MAX = getMaxNoOfDocumentsFromDaslToAnalyze();
		
		// create & populate server response
		ServerResponse sr = new ServerResponse();
		Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
		int counter = 0;
		for (int i = 0; i < nlists.length; i++) {
			Integer type = i;
			NodeList nl = nlists[i];
			for (int j = 0; j < nl.getLength() && counter <MAX; j++, counter++) {
				Node node = nl.item(j);
				ParsedResponse parsedResponse = new ParsedResponse();
				parsedResponse.setAttribute(ParsedResponse.DASL_RECORD, node);
				parsedResponse.setAttribute(ParsedResponse.DASL_TYPE, type);
				parsedResponse.setAttribute("FAKE", daslResponse.isFake+"");
				parsedRows.add(parsedResponse);
			}
		}
		
		if(counter == MAX) {
			SearchLogger.info("<br><font color=\"red\">WARNING: The site returned more than the ATS limit of " +MAX + " results for a single query</font><br><br>", searchId);
			
			ModuleShortDescription moduleShortDescription = (ModuleShortDescription) getSearch()
					.getAdditionalInfo(TSServerInfoConstants.TS_SERVER_INFO_MODULE_DESCRIPTION);
			if (moduleShortDescription == null) {
				moduleShortDescription = new ModuleShortDescription();
				moduleShortDescription.setSearchModuleId(moduleIDX);
				
				TSServerInfoModule module = getDefaultServerInfo().getModule(moduleIDX);
				if(module != null) {
					moduleShortDescription.setDescription(module.getName());
				} else {
					moduleShortDescription.setDescription("Unknown search module");
				}
			}
			
			ResultsLimitPerQueryWarning warning = new ResultsLimitPerQueryWarning(MAX, moduleShortDescription.getDescription()); 
			getSearch().getSearchFlags().addWarning(warning);
			ServerResponse res = ServerResponse.createErrorResponse("WARNING: The site returned more than the ATS limit of " +MAX + " results for a single query!");
			res.getParsedResponse().setAttributes(daslResponse.getAttributes());
			return res;
		}
		
		sr.getParsedResponse().setResultRows(parsedRows);
		sr.setResult(daslResponse.xmlResponse);
		sr.getParsedResponse().setAttributes(daslResponse.getAttributes());
		
		String saction = moduleIDX + "";

		// sAction(link-ul) for DASL does not make any sense, but we can use it
		// for transporting the module number
		solveHtmlResponse(saction, parserID, "SearchBy", sr, sr.getResult());
		sr.getParsedResponse().setAttribute("FAKE", daslResponse.isFake+"");
		// log number of results found
		if (getDefaultServerInfo().getModule(moduleIDX).isVisible())
			logInSearchLogger("Found <span class='number'>"+ sr.getParsedResponse().getResultsCount()+ "</span> <span class='rtype'>intermediate</span> results.<br/>",searchId,log);

		return sr;
	}
	
	protected ServerResponse getErrorResponse(String mess) {
		return ServerResponse.createEmptyResponse();
	}

	protected int getMaxNoOfDocumentsFromDaslToAnalyze() {
		return Search.MAX_NO_OF_DOCUMENTS_FROM_DASL_TO_ANALYZE;
	}
	
	private void logInSearchLogger(String message, long searchId, boolean doLog) {
		if(doLog) {
			SearchLogger.info(message, searchId);
		}
	}

	/**
	 * Get the record rules for a parsed response
	 * 
	 * @param pr
	 * @return
	 */
	protected RecordRules getRecordRules(ParsedResponse pr, int typeSearch) {
		Integer type = (Integer) pr.getAttribute(ParsedResponse.DASL_TYPE);
		if (type == null) {
			throw new RuntimeException(
					"ParsedResponse.DASL_TYPE attribute NOT set!");
		}
		RecordRules retVal = getRecordRules(typeSearch).get(type);
		if (retVal == null) {
			throw new RuntimeException("RecordRules of type " + type
					+ " were not found!");
		}
		serverTypeDirectoryOverride = retVal.overrideType;
		return retVal;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<Object> l = new ArrayList<Object>();
		serverInfo.setModulesForAutoSearch(l);
	}

	/**
	 * Parses the xml and fills the resultmap.
	 * @param doc
	 * @param resultMap TODO
	 * @param map
	 * @throws Exception 
	 */
	protected void parseLegalDescriptions(Document doc, ParsedResponse item, ResultMap resultMap)
	{
		/* Implement me in the derived class */
	}
	
	protected void parseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap)
	{
		/* Implement me in the derived class */
	}
	
	protected void parseAddress(Document doc, ParsedResponse item, ResultMap resultMap)
	{
		/* Implement me in the derived class */
	}
	
	protected void mergeNodes(Node sourceNode, Node destinationNode, DataSite dataSite) {
		/* Implement me in the derived class */
	}
	
	/**
	 * Merge the <TitleRecord>...</TitleRecord> sections of multiple responses
	 * @param responses
	 * @return
	 */
	public DaslResponse mergeResults(List<DaslResponse> responses) {
				
		if(responses.isEmpty()) {
			return null;
		}
		
		DaslResponse finalResponse = null;
		Pattern titleRecordPattern = Pattern.compile("(?ism)<TitleRecord>.*?</TitleRecord>");
		
		for(DaslResponse daslResponse : responses) {
			if(daslResponse != null && daslResponse.status == DaslConnectionSiteInterface.ORDER_COMPLETED) {
				try {
					Matcher titleRecordMatcher = titleRecordPattern.matcher(daslResponse.xmlResponse);
					
					if(finalResponse == null) {
						finalResponse = daslResponse.clone();
					}else {
						if(!finalResponse.xmlResponse.contains("<TitleRecord>")) {
							finalResponse = daslResponse.clone();
							continue;
						}else {
							while(titleRecordMatcher.find()) {
								String newTitleRecord = titleRecordMatcher.group();
								finalResponse.xmlResponse = finalResponse.xmlResponse.replaceFirst("</TitleRecord>", "</TitleRecord>" + Matcher.quoteReplacement(newTitleRecord));
							}
						}
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		if(finalResponse == null ) {
			return responses.get(0);
		}
		
		return finalResponse;
	}
	
	// override this if you want a more user-friendly error message
	protected String parseErrorMessage(String errorMessage) {
		return errorMessage;
	}
	
	@Override
	public void addDocumentAdditionalPostProcessing(DocumentI doc,ServerResponse response){
		super.addDocumentAdditionalPostProcessing(doc,response);

		DocumentsManagerI manager = getSearch().getDocManager();
		try {
			manager.getAccess();
			if(manager.contains(doc)) {
				if(doc instanceof RegisterDocumentI){
					RegisterDocumentI regDoc = (RegisterDocumentI)doc;
					if(regDoc.isOneOf("TRANSFER") && SearchType.GI == regDoc.getSearchType()){
						regDoc.setChecked(false);
						regDoc.setIncludeImage(false);
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Error while post processing document", t);
		} finally {
			manager.releaseAccess();
		}
		
	}
	
	public static boolean isSiteSafeOnGi(int siteId) {
		int serverType = TSServersFactory.getServerTypeFromSiteId(siteId);
		return serverType == GWTDataSite.AC_TYPE 
				|| serverType == GWTDataSite.DT_TYPE
				|| serverType == GWTDataSite.RV_TYPE
				|| serverType == GWTDataSite.TS_TYPE;
	}
}



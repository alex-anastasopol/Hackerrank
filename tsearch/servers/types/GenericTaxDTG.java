package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.getAllNodes;
import static ro.cst.tsearch.datatrace.Utils.parseIntermediary;
import static ro.cst.tsearch.datatrace.Utils.parseTaxDelinqReport;
import static ro.cst.tsearch.datatrace.Utils.parseTaxReport;
import static ro.cst.tsearch.datatrace.Utils.parseXMLDocument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.datatrace.DTRecord;
import ro.cst.tsearch.datatrace.DTTaxRecord;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.connection.datatrace.DataTraceConn;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.datatree.DataTreeStruct;

/**
 * @author MihaiB
 */
@SuppressWarnings("deprecation")
public  class GenericTaxDTG extends TSServerDTG{
	
	transient protected List<DataTreeStruct> datTreeList;
	
	protected static final String CLASSES_FOLDER = BaseServlet.REAL_PATH
			+ File.separator + "WEB-INF" + File.separator + "classes"
			+ File.separator;
	
	protected static final String RESOURCE_FOLDER = (CLASSES_FOLDER
			+ "resource" + File.separator + "DTG" + File.separator).replaceAll("//", "/");
	
	protected static final String TAX_CERTIFICATE = "GenericDTG-Tax.xsl";
	
	private static final String TEMPLATE_TAX_CERTIFICATE_PATH = RESOURCE_FOLDER + TAX_CERTIFICATE;
	
//	private static final String TEMPLATE_TAX_CERTIFICATE_PATH = ServerConfig.getConnectionTemplatesPath() + File.separator + TAX_CERTIFICATE;
	
	protected static final Pattern FAILURE_PAT = Pattern.compile("(?is)<commentary>\\s*<comment>\\s*.*?<text>([^>]+)</text>");
	
	private static final long serialVersionUID = -324752662773381320L;
	
	transient private DataTraceConn conn;
	
	
	public GenericTaxDTG(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) throws FileNotFoundException, IOException {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
    	
        int commId = InstanceManager.getManager().getCurrentInstance(searchId).getCommunityId();
		DataSite site = HashCountyToIndex.getDateSiteForMIServerID(commId, mid);
		
        try {
			conn = new DataTraceConn(site,searchId);
		} catch (Exception e) {
			e.printStackTrace();
		}
        resultType = UNIQUE_RESULT_TYPE;
    }
    
    public GenericTaxDTG(long searchId){
    	super(searchId);
    	resultType = UNIQUE_RESULT_TYPE;
    }
    
    private String performSearch(Map<String, String> params, Map<String, String[]> multiParams, int moduleIDX, String fakeResult) throws SecurityException, IllegalArgumentException, JAXBException, IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException{
    	
    	String stateAbbrev = dataSite.getStateAbbreviation();
		if (StringUtils.isNotEmpty(stateAbbrev)){
			params.put("stateAbbrev", stateAbbrev);
		}
		
		if (getRequestCountType(moduleIDX) != null){
			mSearch.countRequest(getDataSite().getSiteTypeInt(), (Integer) getRequestCountType(moduleIDX));
		}
    	
		switch (moduleIDX) {
			case TSServerInfo.PARCEL_ID_MODULE_IDX:

				return conn.searchByAPNTaxSearch((HashMap<String, String>) params);
			
			case TSServerInfo.NAME_MODULE_IDX:

				return conn.searchByNameOrAddressTaxSearch((HashMap<String, String>) params);
			
			default:
				
			break;
		}
		
		return null;
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
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.NAME_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		return msiServerInfoDefault;
	}
    
    @Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
    	
    	ServerResponse serverResponse = searchBy(module, sd, null);
		
		if (serverResponse != null && serverResponse.getParsedResponse() != null) {
			serverResponse.getParsedResponse().setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, module);
		}
		
		return serverResponse;
	}
    
    protected ServerResponse searchBy(TSServerInfoModule module, Object sd, String fakeResult) throws ServerResponseException {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
//		global.removeAllInMemoryDocs();
		global.clearClickedDocuments();
		
		int moduleIDX = module.getModuleIdx();
		int parserID = module.getParserID();
		
		Map<String, String[]> multiParams = new HashMap<String, String[]>();
		Map<String, String> params = getNonEmptyParams( module, multiParams );
					
		logSearchBy(module);
			
		String response = null;
		try {
			response = performSearch(params, multiParams, module.getModuleIdx(), fakeResult);
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
		
		if (response == null || StringUtils.isBlank(response)){
			logInitialResponse(sr);
			return ServerResponse.createEmptyResponse(); 
		}			
		if (response.indexOf("outcome=\"FAILURE\"") > -1){
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
						
		ServerResponse serverResponse =  processDTGResponse(response, moduleIDX, parserID, false, true);
			
		serverResponse.getParsedResponse().setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, module);
			
		if (StringUtils.isNotEmpty(fakeResult)){
			serverResponse.setFakeResponse(true);
		}
		solveHtmlResponse("", parserID, "SearchBy", serverResponse, serverResponse.getResult());

		return serverResponse;
	}
    
    /**
     * create a 'virtual' link for searching the images: look_for_image.ats?book=<book>&page=<page>" 
     * @param record
     * @return virtual link or null if doc has no book and page
     */
    protected String getImageLink(DTTaxRecord record){
    	
    	if (record == null){ return null; }
    	String [] dtImgInfo = record.getDTImageInfo();
    	if (dtImgInfo == null) { 
    		dtImgInfo = new String[2]; 
    		dtImgInfo[0] = "";
    		dtImgInfo[1] = "";
    	}
    	
    	String apn = record.getAPN();
    	if (StringUtils.isBlank(apn) || "unknown".equals(apn)){  
    		apn = "";
    	}
    	
    	String book = "";
    	String page = "";
    	String year = "";
    	String day  = "";
		String month = "";
    	
    	book = StringUtils.isBlank(book)? "" :book; 
    	page = StringUtils.isBlank(page)? "" :page;
    	year = StringUtils.isBlank(year)? "" :year;
    	
    	String extraParameters = "";
    	
    	
    	String dataTreeIndex = "";
    	try{dataTreeIndex = StringUtils.defaultString(record.getParcelInfo().get("image.image_params.document_index_id"));}
    	catch(Exception e){};
    	
    	String dataTreeDesc = "";
    	try{dataTreeDesc = StringUtils.defaultString(record.getParcelInfo().get("image.image_params.description"));}
    	catch(Exception e){};
    	
    	if (StringUtils.isNotEmpty(dataTreeDesc) && dataTreeDesc.matches("(?is)\\w+\\.\\w+")
				&& StringUtils.isEmpty(book) && StringUtils.isEmpty(page)){
			String[] bp = dataTreeDesc.split("\\s*\\.\\s*");
			if (bp.length == 2){
				book = StringUtils.strip(bp[0], "0");
				page = StringUtils.strip(bp[1], "0");
			}
		}
    	
		if (StringUtils.isNotEmpty(extraParameters)){
			return  CreatePartialLink(TSConnectionURL.idGET) + "look_for_dt_image" + extraParameters;
		}
    	
    	if (StringUtils.isEmpty(dataTreeIndex) || StringUtils.isEmpty(dataTreeDesc)){
			return null;
    	} else{
    		return  CreatePartialLink(TSConnectionURL.idGET) + "look_for_dt_image&id=" + dtImgInfo[0] +"&description=" + dtImgInfo[1]+ "&instr=" + apn 
					+ "&book=" + book + "&page=" + page  + "&year=" + year + "&month=" + month + "&day=" + day
					+ "&dataTreeIndex=" + dataTreeIndex + "&dataTreeDesc=" + dataTreeDesc;    	
    	}
    }
        
	@Override
    protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
    	
    	String rsResponse = Response.getResult();
    	
    	if (StringUtils.isNotEmpty(Response.getError())){
    		return;
    	}
    	if (rsResponse.indexOf("<text>NO REFERENCES FOUND</text>") > -1){
    		Response.getParsedResponse().setError(TSServer.NO_DATA_FOUND);
			return;
    	}

    	if (viParseID == ID_SEARCH_BY_NAME && !(rsResponse.indexOf("<tax_roll ") > 0 && rsResponse.indexOf("record_count=") > 0)){
    		ParseResponse(sAction, Response, ID_DETAILS);
    		return;
    	}
        switch (viParseID) {
        
        case ID_SEARCH_BY_NAME:
        	
        	ParsedResponse parsedResponse = Response.getParsedResponse();
        	StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				logInitialResponse(Response);
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
//				parsedResponse.setResponse("DTG TAX Intermediary");
			}
			break;
			
        case ID_DETAILS:
        case ID_SEARCH_BY_PARCEL:
        	
        	// not saving to TSD
            @SuppressWarnings("unchecked")
			List<ParsedResponse> items = (List<ParsedResponse>)Response.getParsedResponse().getResultRows();
            if (items.size() == 0) {
            	Response.getParsedResponse().setResultRows(new Vector<ParsedResponse>());	
				return;
			}
            for (ParsedResponse item:items) {
            	
            	DTTaxRecord record = (DTTaxRecord) item.getAttribute(ParsedResponse.DT_RECORD);
            	
            	// parse data
            	setParsedData(record, item);
                
                // get APN
                String apn = record.getAPN();
                
                // to string
                String itemText = createHtmlViaXSLT(rsResponse);
                itemText = itemText.replaceAll("(?is)null", "");
                
                String link = null;
                String [] dtImgInfo = record.getDTImageInfo();
                if (dtImgInfo != null){
	                String bookPage = dtImgInfo[0]+ "_" + dtImgInfo[1];
	            	if (Boolean.FALSE != getSearch().getAdditionalInfo("img_" + bookPage)){
	            		link = getImageLink(record);
	            	}                                
	                if (link != null && link.length() > 0){
	                	itemText += "<a href=\"" + link + "\" target=\"_blank\">View Image</a>";
	                }
                }
                item.setResponse(itemText);

                String originalLink = "DT___" + apn;
                String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;   
                mSearch.addInMemoryDoc(sSave2TSDLink, item);
                
                HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");

				if (isInstrumentSaved(apn, null, data)){
					itemText += CreateFileAlreadyInTSD();
				} else{
					itemText = addSaveToTsdButton(itemText, sSave2TSDLink, ID_DETAILS);
				}
				item.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
            	
            	parser.Parse(item, itemText, Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
            }
        	break;
        	
        case ID_SAVE_TO_TSD:
        	
        	ParsedResponse pr = (ParsedResponse) Response.getParsedResponse();
        	
        	DTTaxRecord record = (DTTaxRecord) pr.getAttribute(ParsedResponse.DT_RECORD);
        	DocumentI document = pr.getDocument();
        	
        	if (record == null && document != null) {
        		msSaveToTSDFileName = document.getInstno() + ".html";
	            
	            pr.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
        	} else {		
	    		// create string representation
	    		String itemText = "";
				try {
					itemText = new String (rsResponse.getBytes(),"UTF-8");
				} catch (UnsupportedEncodingException e) {
				}
		        		
	        	String apn = record.getAPN();
	            
	            logger.info("Instrument NO:" + apn);
	           
	            String link = null;
	            String [] dtImgInfo = record.getDTImageInfo();
	            if (dtImgInfo != null){
	                String bookPage = dtImgInfo[0]+ "_" + dtImgInfo[1];
		        	if (Boolean.FALSE != getSearch().getAdditionalInfo("img_" + bookPage)){
		        		link = getImageLink(record);
		        	} else {
		        		pr.resetImages();
		        	}
	            
		            if (link != null && link.length() > 0){
		            	if (pr.getImageLinksCount() == 0){
		            		pr.addImageLink(new ImageLinkInPage (link, bookPage + ".tiff" ));
		            	}
//		            	Image image = new Image();
//		            	Set<String> links = new HashSet<String>();
//		            	links.add(link);
//		            	image.setLinks(links);
//		            	try {
//		            		addDocPlat(Response, image);
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
		            }
	            }
	            pr.setResponse(itemText);
	            
	            msSaveToTSDFileName = apn + ".html";
	            
	            pr.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	            
	            rsResponse = pr.getResponse();
	            rsResponse = pr.getResponse();
	            rsResponse = rsResponse.replaceAll("(?s)<input.*?>", "");
	            rsResponse = rsResponse.replaceAll("Select for saving to TS Report", "");
	            rsResponse = rsResponse.replaceAll("<hr>","");
	            
	            parser.Parse(pr, rsResponse, Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);        
	            pr.setOnlyResponse(rsResponse);
        	}
        	break;
        }
    }
	
	@SuppressWarnings("rawtypes")
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			Node doc = parseXMLDocument(table.getBytes());
			NodeList titleRecs = getAllNodes(doc, "//tax_parcel");
			for (int i = 0; i < titleRecs.getLength(); i++) {
	        	Node node = titleRecs.item(i);
	        	Map<String, String> row = parseIntermediary(node);
	        	if (row != null && row.size() > 0){
	        		String owner = StringUtils.defaultString(row.get("owner"));
	        		String address = StringUtils.defaultString(row.get("situs"));
	        		String apn = StringUtils.defaultString(row.get("apn"));
	        		apn = apn.replaceAll("\\s+", "");
	        		String link = CreatePartialLink(TSConnectionURL.idGET) + "GoForAPN=" + apn;
	        		
	        		String key = apn;
						
					ParsedResponse currentResponse = responses.get(key);							 
					if (currentResponse == null) {
						currentResponse = new ParsedResponse();
						responses.put(key, currentResponse);
					}
	        		
	        		ResultMap resultMap = new ResultMap();
	        							
					TaxDocumentI document = (TaxDocumentI)currentResponse.getDocument();
					if (document == null){
						StringBuffer htmlRow = new StringBuffer();
						htmlRow.append("<tr><td><a href=\"" + link + "\">" + apn + "</a></td><td>" + address + "</td><td>" + owner + "</td></tr>");
						
		        		Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
	
						resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());
		        		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), apn.trim());
		        		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
		        		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
		        		if (owner.length() > 0){
		        			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner.toString());
		        		}
		        		owner = owner.replaceFirst("(?is)\\bAND\\b\\s*$", "");
		        		owner = owner.replaceAll("(?is)\\b(DECD|DECEASED)\\b", "");
		        		owner = owner.replaceAll("(?is)\\bEST(ATE)?\\s+OF\\b", "");
		        		owner = owner.replaceFirst("(?is)\\bFAMI\\b\\s*$", "");
        				
        				String[] names = StringFormats.parseNameNashville(owner, true);

        				String[] type = GenericFunctions.extractAllNamesType(names);
        				String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
        				String[] suffixes = GenericFunctions.extractNameSuffixes(names);

        				ArrayList<List> body = new ArrayList<List>();
        				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
						        						NameUtils.isCompany(names[5]), body);
	
						if (body.size() > 0){
					    	try {
								GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
		        		
		        		document = (TaxDocumentI) bridge.importData();
		        		
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow.toString());
	
						currentResponse.setOnlyResponse(htmlRow.toString());
//						currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SEARCH_BY));
						
						TSServerInfoModule module = this.getDefaultServerInfo().getModuleForSearch(TSServerInfo.PARCEL_ID_MODULE_IDX, new SearchDataWrapper());
						module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
						module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CHECK_ALREADY_SAVED, Boolean.TRUE);
							 
						module.forceValue(0, "ORDER");
						module.forceValue(2, apn);

						String apnLink = createPartialLink(TSConnectionURL.idGET, TSServerInfo.PARCEL_ID_MODULE_IDX) + "&GoForAPN=" + apn;

						LinkInPage linkInPage = new LinkInPage(module, TSServer.REQUEST_SEARCH_BY_REC);
						if (apnLink != null) {
							linkInPage.setOnlyLink(apnLink);
						}
						currentResponse.setPageLink(linkInPage);
						
						currentResponse.setDocument(document);
						intermediaryResponse.add(currentResponse);
	        		} else{
	        			PartyI party = new Party(PType.GRANTOR);
	        			if (owner.length() > 0){
	        				String htmlRow = currentResponse.getResponse();
	        				htmlRow = htmlRow.replaceFirst("(?is)(</td></tr>)", " / " + owner + "$1");
	        				currentResponse.setOnlyResponse(htmlRow);
	        				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
	        				
		        			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner.toString());
		        			party.setFreeParsedForm(owner);
		        			
		        			owner = owner.replaceFirst("(?is)\\bAND\\b\\s*$", "");
			        		owner = owner.replaceAll("(?is)\\b(DECD|DECEASED)\\b", "");
			        		owner = owner.replaceAll("(?is)\\bEST(ATE)?\\s+OF\\b", "");
			        		owner = owner.replaceFirst("(?is)\\bFAMI\\b\\s*$", "");
	        				
	        				String[] names = StringFormats.parseNameNashville(owner, true);

	        				String[] type = GenericFunctions.extractAllNamesType(names);
	        				String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
	        				String[] suffixes = GenericFunctions.extractNameSuffixes(names);
	        				
	        				if (StringUtils.isNotEmpty(names[2])){
		        				addNamesToParty(party, new String[]{names[2], names[0], names[1]}, type[0], otherType[0], suffixes[0]);
	        				}
	        				if (StringUtils.isNotEmpty(names[5])){
		        				addNamesToParty(party, new String[]{names[5], names[3], names[4]}, type[1], otherType[1], suffixes[1]);
	        				}
	        				for (PropertyI prop : document.getProperties()) {
								if (prop.getOwner() != null){
									prop.getOwner().add(party);
								}
							}
		        		}
	        		}
	        	}
			}
			response.getParsedResponse().setHeader("<table border=\"1\" width=\"50%\"><tr><th>APN</th><th>SITUS ADDRESS</th><th>OWNER</th></tr>");
			response.getParsedResponse().setFooter("</table>");

			outputTable.append(table);
		} catch (Exception e) {
		}
		
		return intermediaryResponse;
	}

	/**
	 * @param party
	 * @param names
	 * @param type
	 * @param otherType
	 * @param suffixes
	 */
	public void addNamesToParty(PartyI party, String[] names, String type, String otherType, String suffix) {
		Name name = new Name();
		name.setLastName(names[2]);
		name.setFirstName(names[0]);
		name.setMiddleName(names[1]);
		name.setSufix(suffix);
		name.setNameType(type);
		name.setNameOtherType(otherType);
		boolean isCompany = NameUtils.isCompany(names[2]); 
		name.setCompany(isCompany);
		party.add(name);
	}
	
	protected void addDocPlat(ServerResponse response, ImageI image){
		
		if (image.getLinks().size() > 0){
			String link = image.getLink(0);
		
			String book = "", page = "";
			if (StringUtils.isNotEmpty(link)){
				HashMap<String, String> map = HttpUtils.getParamsFromLink(link);
				
				book 	 = map.get("book");
				page 	 = map.get("page");
				String dataTreeDesc = map.get("dataTreeDesc");
				if (StringUtils.isNotEmpty(dataTreeDesc) && dataTreeDesc.contains(".")){
					String[] bp = dataTreeDesc.split("\\s*\\.\\s*");
					if (StringUtils.isEmpty(book)){
						book = bp[0];
					}
					if (StringUtils.isEmpty(page)){
						page = bp[1];
					}
				}
			}
			
			if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)){
				int siteId = (int) TSServersFactory.getSiteId(dataSite.getStateAbbreviation(), dataSite.getCountyName(), "DG");
				DataSite data = null;
				
				try {
					data = HashCountyToIndex.getDateSiteForMIServerID(InstanceManager.getManager().getCommunityId(searchId), siteId);
				} catch (Exception e) {
					logger.error("Cannot obtain datasite for RO2 is not available for county:" + dataSite.getCountyName() + "; searchid=" + searchId);
				}
				
				/**
				 * If DG is not enabled for a certain county, then the AssessorMap will not be saved.
				 */
				if (data != null){
							
					String docIndex = DTG_FAKE_RESPONSE;
					docIndex = docIndex.replace("@@Grantor@@", "County of " + dataSite.getCountyName());
					docIndex = docIndex.replace("@@Grantee@@", "");
					
					String fromDateDD = StringUtils.defaultIfBlank(getSearch().getSa().getAtribute(SearchAttributes.FROMDATE_DD), "01");
					String fromDateMM = StringUtils.defaultIfBlank(getSearch().getSa().getAtribute(SearchAttributes.FROMDATE_MM), "01");
					String fromDateYYYY = StringUtils.defaultIfBlank(getSearch().getSa().getAtribute(SearchAttributes.FROMDATE_YEAR), "1960");
					String fromDate = StringUtils.defaultIfBlank(getSearch().getSa().getAtribute(SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY), "01/01/1960");
					 
					docIndex = docIndex.replace("@@Date@@", fromDateMM + "/" + fromDateDD + "/" + fromDateYYYY);
					docIndex = docIndex.replace("@@Date@@", fromDate);
					docIndex = docIndex.replace("@@Year@@", fromDateYYYY);
					docIndex = docIndex.replace("@@Month@@", fromDateMM);
					docIndex = docIndex.replace("@@Day@@", fromDateDD);
					docIndex = docIndex.replace("@@Type@@-@@Type@@", "ASSESSOR_MAP");
					docIndex = docIndex.replace("@@DataTreeDocType@@", "ASSESSOR_MAP");
					docIndex = docIndex.replace("@@CountyFips@@", dataSite.getCountyFIPS());
					docIndex = docIndex.replace("@@Book@@", StringUtils.defaultString(book));
					docIndex = docIndex.replace("@@Page@@", StringUtils.defaultString(page));
					docIndex = docIndex.replace("@@DocNo@@", "");
					
					TSInterface server = TSServersFactory.GetServerInstance(siteId, "06053", GWTDataSite.DG_TYPE + "", searchId);
					TSServerDTG serverDTG = null;
					if (server instanceof TSServerDTG){
						serverDTG = (TSServerDTG) server;
						serverDTG.setServerForTsd(mSearch, msSiteRealPath);
					}
					
					if (serverDTG != null){
						TSServerInfoModule module = server.getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX, new SearchDataWrapper());
						module.setParserID(ID_SEARCH_BY_BOOK_AND_PAGE);
						module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
						module.setParamValue(0, book);
						module.setParamValue(1, page);
						module.setParamValue(4, "PLAT");
						module.setParamValue(5, "ASSESSOR_MAP");
						module.setSearchType(SearchType.IM.toString());
						
						ServerResponse res = new ServerResponse();
						try {
							serverDTG.logSearchBy(module);
							res = serverDTG.searchBy(module,  new SearchDataWrapper(getSearchAttributes()), docIndex);
						} catch (ServerResponseException e) {
							e.printStackTrace();
						}
						
						if (res.getParsedResponse().getResultRows().size() > 0){
							Object obj = res.getParsedResponse().getResultRows().elementAt(0);
							if (obj != null && obj instanceof ParsedResponse){
								ParsedResponse pr = (ParsedResponse) obj;
								pr.setSearchId(searchId);
								
								Object ob = pr.getAttribute(ParsedResponse.DT_RECORD);
								if (ob != null && ob instanceof DTRecord){
									DTRecord record = (DTRecord) ob;
									String itemText = record.toString();
					                itemText = itemText.replaceAll("(?is)null", "");
					                pr.setResponse(itemText);
					                pr.setOnlyResponse(itemText);
					                res.setResult(itemText);
					                res.setParsedResponse(pr);
					                res.getParsedResponse().resetImages();
					                res.getParsedResponse().addImageLink(new ImageLinkInPage(link, book + "_" + page + ".tiff"));
									SearchLogger.info("</div><br><div><table border='1' cellspacing='0' width='99%'>" +
						            		"<tr><th>No</th>" +
						            		"<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th>" + 
						            		"</tr>", searchId);
						            StringBuilder sb = new StringBuilder();
						            DocumentI doc = pr.getDocument();
						            if (doc != null){
						            	doc.setSearchType(DocumentI.SearchType.IM);
						            }
						            
						            String logRepr = pr.getTsrIndexRepresentation();    	
						            String id = String.valueOf(System.nanoTime());
						            sb.append("<tr class='row' id='" + id + "'>");
						            sb.append("<td>1</td>");
						            sb.append(logRepr);
						            sb.append("</tr>");     	
						            SearchLogger.info(sb.toString(), searchId);
						            SearchLogger.info("</table></div><br/>", searchId);
						            
						            addDocumentInATS(res, res.getResult());
								}
							}
						}
					}
				}
			}
		}
	}

	public String createHtmlViaXSLT(String xmlResponse){
		StringBuffer details = new StringBuffer();
		if (StringUtils.isNotEmpty(xmlResponse)){
//			xmlResponse = xmlResponse.replaceAll("(?is)<\\?xml[^>]*>", "");
			Document xmlSourceDoc = XmlUtils.parseXml(xmlResponse, "UTF-8");
			
			try {
				File style = new File(TEMPLATE_TAX_CERTIFICATE_PATH);
	    	    TransformerFactory tFactory = TransformerFactory.newInstance();
	    	    Transformer transformer = tFactory.newTransformer(new StreamSource(style));
	    	    StringWriter outputWriter = new StringWriter();
	    	    transformer.transform(new DOMSource(xmlSourceDoc), new StreamResult (outputWriter));
	    	    
	    	    //transformer.transform(new DOMSource(xmlSourceDoc), 
	    	    		//new StreamResult (new FileOutputStream("D:\\DTG\\raspuns din ats.html")));
	    	    
	    	    String taxCertificate = outputWriter.toString();
//	    	    taxCertificate = taxCertificate.replaceAll("(?is)</?HTML[^>]*>", "")
//	    	    		.replaceAll("(?is)</?HEAD[^>]*>", "").replaceAll("(?is)</?META[^>]*>", "")
//	    	    		.replaceAll("(?is)</?title[^>]*>", "").replaceAll("(?is)</?body[^>]*>", "");
	    	    taxCertificate = taxCertificate.replaceAll("(?is)<\\?xml[^>]*>", "");
	    	    details.append("<br><center><h2><b>Tax Certificate</b></h2></center><br><br>").append(taxCertificate);
			} catch (Exception e) {
	    	    e.printStackTrace( );
    	    }
		}
		
		return details.toString();
	}
	
	void setParsedData(DTTaxRecord record2, ParsedResponse item2){
		 record2.setParsedData(item2, searchId, getDataSite());
	}
	
	void setParsedData(DTRecord record2, ParsedResponse item2){
		 record2.setParsedData(item2, searchId, getDTGDataSite());
	}
	
	public DataSite getDTGDataSite(){
		DataSite datasite = null;
		try {
			long siteId = TSServersFactory.getSiteId(dataSite.getStateAbbreviation(), dataSite.getCountyName(), "DG");
			try {
				datasite = HashCountyToIndex.getDateSiteForMIServerID(InstanceManager.getManager().getCommunityId(searchId), siteId);
			} catch (Exception e) {
				logger.error("Cannot obtain datasite for DTG is not available for county:" + dataSite.getCountyName() + "; searchid=" + searchId);
			}
			
		} catch (Exception e) {
			logger.error("DTG is not available for county:" + dataSite.getCountyName() + "; searchid=" + searchId);
		}
		
		return datasite;
	}
	
	protected ServerResponse processDTGResponse(String response, int moduleIDX , int parserID, boolean referenceSearch, boolean log) throws ServerResponseException {
		// create & populate server response
		try {
			return processXMLResponse(response, false, null, referenceSearch, moduleIDX);
		} catch (Exception e) {
			e.printStackTrace();
			super.logInSearchLogger("<font color=\"red\">Data Source Exception ! </font>", searchId,log);
			return ServerResponse.createWarningResponse("Data Source Exception !");
		}
	}
	
    private ServerResponse processXMLResponse(String xmlResponse, boolean getDetails, Map<String,String> params, boolean referenceSearch, int moduleIDX) 
    		throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
    	
        // process the response                
    	Node doc = parseXMLDocument(xmlResponse.getBytes());
    	
    	NodeList titleRecs = getAllNodes(doc, "//tax_report//tax_bill");
        
        // create the set of parsed records
        Map<String, DTTaxRecord> parsedTitleRecs = new LinkedHashMap<String, DTTaxRecord>();
        for (int i = 0; i < titleRecs.getLength(); i++) {
        	Node node = titleRecs.item(i);
        	DTTaxRecord record = parseTaxReport(node);
        	if (record == null){ continue; }
        	if (record.getCurrentAssessmentInfo() != null){
        		String billType = record.getCurrentAssessmentInfo().get("current_tax.tax_due.tax_bill_type");
        		if (StringUtils.isNotEmpty(billType) && billType.toUpperCase().contains("SUPPLEMENTAL BILL") && parsedTitleRecs.size() > 0){
        			Iterator<Entry<String, DTTaxRecord>> it = parsedTitleRecs.entrySet().iterator();
        		    while (it.hasNext()) {
        		        Map.Entry<String, DTTaxRecord> pairs = (Map.Entry<String, DTTaxRecord>)it.next();
        		        DTTaxRecord rec = (DTTaxRecord) pairs.getValue();
        		        if (record.getPartyList().size() > 0){
        		        	rec.getPartyList().addAll(record.getPartyList());
        		        }
        		        rec.setTaxInstallmentSupplementalList(record.getTaxInstallmentList());
        		    }
        			continue;
        		}
        	}
        	String id = record.getId();
        	if (id == null) { continue; }
        	parsedTitleRecs.put(id, record);
        }
        if (parsedTitleRecs.size() == 1){
        	NodeList titlePriorDelinqRecs = getAllNodes(doc, "//tax_report//tax_remittance");
        	for (int i = 0; i < titlePriorDelinqRecs.getLength(); i++) {
        		Node node = titlePriorDelinqRecs.item(i);
            	DTTaxRecord record = parseTaxDelinqReport(node);
            	if (record == null){ continue; }
            	Iterator<Entry<String, DTTaxRecord>> it = parsedTitleRecs.entrySet().iterator();
    		    while (it.hasNext()) {
    		        Map.Entry<String, DTTaxRecord> pairs = (Map.Entry<String, DTTaxRecord>)it.next();
    		        DTTaxRecord rec = (DTTaxRecord) pairs.getValue();
    		        rec.setPriorYearsDelinqList(record.getPriorYearsDelinqList());
    		        rec.setPriorYearsRedemptionList(record.getPriorYearsRedemptionList());
    		    }
        	}
        }
             
        ServerResponse serverResponse = new ServerResponse();        
        Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
       
        for(DTTaxRecord record : parsedTitleRecs.values()){                    
            ParsedResponse parsedResponse = new ParsedResponse();
            parsedResponse.setAttribute(ParsedResponse.DT_RECORD, record);
            parsedRows.add(parsedResponse );
        }
        
        serverResponse.getParsedResponse().setResponse("TAX DTG intermediary");
        serverResponse.getParsedResponse().setResultRows(parsedRows);
        serverResponse.setResult(xmlResponse);
        if (parsedTitleRecs.size() > 0){
        	logInitialResponse(serverResponse);
        }
   
        return serverResponse;
        
    }

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		
		if (hasPin()){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setData(0, "ORDER");
			module.setData(1, "NO");
			module.setData(2, prepareApnPerCounty(searchId));
			
			modules.add(module);
		}
		
		if (hasStreet() && hasStreetNo()){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setData(0, "INVESTIGATIVE");
			module.setData(1, "NO");
			module.setSaKey(2, SearchAttributes.P_STREETNO);
			module.setSaKey(3, SearchAttributes.P_STREETNAME);
			
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			
			modules.add(module);
		}
		
		if (hasOwner()){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setData(0, "INVESTIGATIVE");
			module.setData(1, "NO");
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			
			module.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			module.addIterator(nameIterator);
			
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	public String prepareApnPerCounty(long searchId) {
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
		int countyId = getDataSite().getCountyId();
		
		String apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNONDB );
		if(StringUtils.isEmpty(apn)){
			apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNO_GENERIC_TR );
		}
		if (countyId!=CountyConstants.CA_San_Bernardino) {
			apn = apn.replaceAll("[.-]", "");
		}
		
		search.getSa().setAtribute(SearchAttributes.LD_PARCELNO3,apn);
		return apn;
	}
	 
	@Override
	protected String CreateSaveToTSDFormEnd(String name, int parserId, int numberOfUnsavedRows) {
	    	
		if (name == null) {
			name = SAVE_DOCUMENT_BUTTON_LABEL;
		}
	    	
		if (isPropertyOrientedSite()){
			if (numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0) {
				return "<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" " 
	    				+ "onclick=\"javascript:submitFormByGet();\">\r\n</form>\n" ;
			} else {
				return "</form>\n";
			}
		}
	        
		String s = "";
	        
		if (!isRoLike(miServerID, true) && (parserId == ID_DETAILS || parserId == ID_DETAILS1 || parserId == ID_DETAILS2 )){
			if (numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0) {
				s = "<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" + onclick=\"javascript:submitFormByGet();\">\r\n" +
						 "\r\n";
			}       
		}
		return s + "</form>\n";
	}
	 
	public ServerResponse GetLink(String vsRequest, boolean vbEncoded)throws ServerResponseException {
		ServerResponse sr = null;
		int indexOfGoForAPN = vsRequest.indexOf("GoForAPN=");
		if (indexOfGoForAPN > 0) {
			String apn = vsRequest.substring(indexOfGoForAPN + 9);
			TSServerInfoModule module = this.getDefaultServerInfo().getModuleForSearch(TSServerInfo.PARCEL_ID_MODULE_IDX, new SearchDataWrapper());
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CHECK_ALREADY_SAVED, Boolean.TRUE);
			 
			module.forceValue(0, "ORDER");
			module.forceValue(2, apn);
			 
			sr = SearchBy(module, new SearchDataWrapper(getSearchAttributes()));
		} else {
			return super.GetLink(vsRequest, vbEncoded);
		}
		 
		return sr;
	}
	 
	@Override
	public void addAdditionalDocuments(DocumentI doc, ServerResponse response){
		if (doc != null && doc.hasImage()){
			try {
				addDocPlat(response, doc.getImage());
				doc.setImage(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	 public String getSaveSearchParametersButton(ServerResponse response){
		 return "";
	}
}

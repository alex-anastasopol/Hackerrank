package ro.cst.tsearch.servers.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DateFormatUtils;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.SubdivisionFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.instrument.InstrumentAKROIterator;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.functions.GenericOncoreFunctionsRO;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
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
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

public class GenericOncoreServerRO extends TSServerROLike{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// e.g. Release Through Date is:Thursday, Feb 21, 2013 
	private static final Pattern certDatePattern = Pattern.compile("(?is)Release Through Date is:\\s*[a-zA-Z]+,\\s*([a-zA-Z\\.]+\\s*\\d{1,2},\\s*\\d{4})");

	public GenericOncoreServerRO(long searchId) {
		super(searchId);
	}

	public GenericOncoreServerRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
    	ParsedResponse parsedResponse = Response.getParsedResponse();
    	
    	if(rsResponse.contains("document.getElementById(\"trExceedMessage\").style.display = \"\"")) {
    		parsedResponse.setError("<font color=\"red\">Official Site Says: </font>" +
    				"Maximum limit of 5000 records has been exceeded, please change the above criteria.");
    		Response.setError(parsedResponse.getError());
    		Response.setResult("");
			return;
    	}
    	    	
    	switch(viParseID){
			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_INSTRUMENT_NO:
			case ID_SEARCH_BY_SUBDIVISION_NAME:
			case ID_SEARCH_BY_MODULE19:		//Document Type Search
			{
				
				StringBuilder outputTable = new StringBuilder();
				
				Collection<ParsedResponse> smartParsedResponses = GenericOncoreFunctionsRO.
						smartParseIntermediary(Response, rsResponse, outputTable, this);
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(rsResponse);
					
		            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"5\">\n";
		            	header += parsedResponse.getHeader();
		            	
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(parsedResponse.getFooter() + "\n</table>");
		            	
		            	
		            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		parsedResponse.setFooter(parsedResponse.getFooter() + 
		            				CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer)numberOfUnsavedDocument));
		            	} else {
		            		parsedResponse.setFooter(parsedResponse.getFooter() + 
		            				CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1));
		            	}
		            	
		            }
		            if(viParseID == ID_SEARCH_BY_INSTRUMENT_NO) {
	            		Response.getParsedResponse().setNextLink("");
	            	}
					
				} else {
					parsedResponse.setError("<font color=\"red\">No results found.</font> Please try again.");
					return;
				}	
			}
				break;
				
			case ID_DETAILS:
    		case ID_SAVE_TO_TSD:
    		{
    			String[] contentsArray = GenericOncoreFunctionsRO.getDetailedContent(rsResponse, sAction, this);
    			if(contentsArray == null || contentsArray.length == 0 || StringUtils.isEmpty(contentsArray[0])) {
		    		parsedResponse.setError("<font color=\"red\">Could not parse document.</font>  Please search again.");
					return;
    			}
			    String contents = contentsArray[0];
    			parsedResponse.setResponse(contents);
    			String keyCode = contentsArray[1];
    			
    			if(viParseID == ID_SAVE_TO_TSD) {
    				msSaveToTSDFileName = keyCode + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = "<form>" + contents + CreateFileAlreadyInTSD();                
	                smartParseDetails(Response,contents, false);
    			} else {
    				String originalLink = sAction + "&shortened=true"; //Response.getQuerry();
                    String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
                    
                    HashMap<String, String> data = new HashMap<String, String>();
    				data.put("type",contentsArray[2]);
    				boolean hasImage = false;
    				if(contentsArray.length == 6) {
	    				data.put("book", contentsArray[3]);
	    				data.put("page", contentsArray[4]);
	    				hasImage = contentsArray[5] != null;
    				} else {
    					keyCode = contentsArray[3];
    					hasImage = contentsArray[4] != null;
    				}
    				
    				if(isInstrumentSaved(keyCode, null, data)){
    					contents += CreateFileAlreadyInTSD();
    				} else {
    					mSearch.addInMemoryDoc(sSave2TSDLink, contents/*.replaceAll("<a\\s+href[^>]+>([^<]+)</a>", "$1")*/);
    					contents = addSaveToTsdButton(contents, sSave2TSDLink, viParseID);
    				}
                    if(hasImage) {
	    				String imageLink = CreatePartialLink(TSConnectionURL.idPOST) + "/oncoreweb/ImageBrowser/default.aspx?id=" + contentsArray[1] + "&dtk=" + contentsArray[2];
	    				
	    				contents = contents.replace("</table>", "<tr><td>Image</td><td><a href=\"" + imageLink + "\" target=\"_blank\">View Image</a></td></tr></table>");
                    }
    				
                    Response.getParsedResponse().setResponse(contents);
                    Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
    			}
    		}
    			break;
    		case ID_GET_LINK:
    			System.out.println("sAction: " + sAction);
    			if(sAction.contains("details.aspx")) {
    				ParseResponse(sAction, Response, ID_DETAILS);
    			} else if(sAction.contains("search.aspx?q=detail")) {
    				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
    			}
    			break;
    	}
    	
	}

	
	

	/**
	 * Returns the official server id of a document or null if it cannot be found
	 * @param link
	 * @return
	 */
	public static String getDocumentServerIfFromLink(String link) {
		if(link != null) {
			return org.apache.commons.lang.StringUtils.substringBetween(link, "id=", "&");
		}
		return null;
	}

	
	
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		DocumentI document = null;
		try {
			ResultMap map = new ResultMap();
			String imageLink = (String)parseAndFillResultMap(response,detailsHtml, map);
			map.removeTempDef();//this is for removing tmp items. we remove them here to not remove them in every place when we parse something.
			Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
			document = bridge.importData();
			
			if(imageLink != null) {
				getSearch().addImagesToDocument(document, imageLink);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		response.getParsedResponse().setOnlyResponse(detailsHtml.replaceAll("<a\\s+href[^>]+>([^<]+)</a>", "$1").replace("View Image", ""));
		if(document!=null) {
        	response.getParsedResponse().setDocument(document);
		}
		return document;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
		return GenericOncoreFunctionsRO.parseAndFillResultsMap(
				response, 
				detailsHtml, 
				resultMap, 
				getSearch());
	}
	
	@Override
	protected ServerResponse SearchBy(boolean bResetQuery,
            TSServerInfoModule module, Object sd)
            throws ServerResponseException {
		
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
    	
    	if(namePositionMap.get("txtBeginDate") != null) {
    		String field1 = module.getFunction(namePositionMap.get("txtBeginDate")).getParamValue().trim();
	    	String field2 = module.getFunction(namePositionMap.get("txtEndDate")).getParamValue().trim();
	    	
	    	
	    	ServerResponse serverResponse = new ServerResponse();
	    	ParsedResponse parsedResponse = serverResponse.getParsedResponse();
	    	
	    	SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	    	sdf.setLenient(false);
	    	
	
	    	Date startDate = null;
	    	Date endDate = null;
	    	
	    	if(field1.isEmpty()) {
	    		parsedResponse.setError("<font color=\"red\">Invalid start date entered.</font><br>Please change search criteria and try again.");
	    		return serverResponse;
	    	} else {
	    		try {
	    			startDate = sdf.parse(field1);
				} catch (ParseException e) {
					logger.error("Error while parsing start month/day/yyyy", e);
					parsedResponse.setError("<font color=\"red\">Invalid start date entered.</font><br>Please change search criteria and try again.");
					return serverResponse;
				}
	    	}
	    		
	    	if(field2.isEmpty()) {
	    		parsedResponse.setError("<font color=\"red\">Invalid end date entered.</font><br>Please change search criteria and try again.");
	    		return serverResponse;
	    	} else {
	    		try {
	    			endDate = sdf.parse(field2);
				} catch (ParseException e) {
					logger.error("Error while parsing end month/day/yyyy", e);
					parsedResponse.setError("<font color=\"red\">Invalid end date entered.</font><br>Please change search criteria and try again.");
					return serverResponse;
				}
	    	}
	    	
	    	if(startDate.after(endDate)) {
	    		parsedResponse.setError("<font color=\"red\">Invalid dates entered. Begin date is after End Date.</font><br>Please change search criteria and try again.");
	    		return serverResponse;
	    	}
	    	
	    	if( (endDate.getTime() - startDate.getTime() ) / FormatDate.MILLIS_IN_DAYS > 36500) {
	    		parsedResponse.setError("<font color=\"red\">Exceeded date range limit of 36500 days.</font><br>Please change search criteria and try again.");
	    		return serverResponse;
	    	}
	    	
    	} else if(namePositionMap.get("txtRecordDate") != null) {
	    	String field1 = module.getFunction(namePositionMap.get("txtRecordDate")).getParamValue().trim();
	    	
	    	ServerResponse serverResponse = new ServerResponse();
	    	ParsedResponse parsedResponse = serverResponse.getParsedResponse();
	    	
	    	SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	    	sdf.setLenient(false);
	    	
	
	    	if(field1.isEmpty()) {
	    		parsedResponse.setError("<font color=\"red\">Invalid record date entered.</font><br>Please change search criteria and try again.");
	    		return serverResponse;
	    	} else {
	    		try {
	    			sdf.parse(field1);
				} catch (ParseException e) {
					logger.error("Error while parsing record month/day/yyyy", e);
					parsedResponse.setError("<font color=\"red\">Invalid record date entered.</font><br>Please change search criteria and try again.");
					return serverResponse;
				}
	    	}
    	}
    	return null;
	}
	
	@Override
    protected String getFileNameFromLink(String link) {
    	String retVal = StringUtils.extractParameter(link,"&dummy=([^&]+)");
    	if(StringUtils.isEmpty(retVal)){
    		retVal = StringUtils.extractParameter(link,"&bp=([^&]+)");
    	}
    	if(StringUtils.isEmpty(retVal)){
    		retVal = StringUtils.extractParameter(link, "&__EVENTARGUMENT=([^&]+)");
    	}
    	if(StringUtils.isEmpty(retVal)){
    		retVal = link;
    	}
    	return retVal + ".html";    	
    }
	 
	@Override
	protected boolean isRecursiveAnaliseInProgress(String link){
		if(super.isRecursiveAnaliseInProgress(link)){
			return true;
		}
		/**
 		 * Link is always changing due to SEQ number that is incremented.
 		 * Need to check the instrument number also
 		 */
		String link2 = StringUtils.extractParameter(link, "(&__EVENTARGUMENT=[^&]+)");
		if(!StringUtils.isEmpty(link2)){
			return super.isRecursiveAnaliseInProgress(link2);
		}
		return false;
	}
	
	@Override
    protected void addRecursiveAnalisedLink(String link){
    	 super.addRecursiveAnalisedLink(link);
    	 /**
 		 * Link is always changing due to SEQ number that is incremented.
 		 * Need to store the instrument number not to analyze it again
 		 */
    	 String link2 = StringUtils.extractParameter(link, "(&__EVENTARGUMENT=[^&]+)");
    	 if(!StringUtils.isEmpty(link2)){
    		 super.addRecursiveAnalisedLink(link2);
    	 }
    }
	
	public String getPrettyFollowedLink (String initialFollowedLnk)
    {	
		String instrument = getFileNameFromLink(initialFollowedLnk).replace(".html", "");
		if (StringUtils.isNotEmpty(instrument))
    	{
    		String retStr =  "Instrument "+ instrument+
    				" has already been processed from a previous search in the log file.";
    		return  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    	}
    	return "<br/><span class='followed'>" + preProcessLink(initialFollowedLnk) + "</span><br/>";
    }

	private static int seq = 0;	
	public synchronized static int getSeq(){
		return seq++;
	}
	
	@Override
	public DownloadImageResult downloadImage(ImageI image, String documentIdStr)
			throws ServerResponseException {
		/**
		 * Force deletion of parameters from last query
		 */
		getTSConnection().setQuery("");
		return super.downloadImage(image, documentIdStr);
	}
	
	private InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator iterator = new InstrumentGenericIterator(searchId) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected String cleanInstrumentNo(String inst, int year) {
				String instNo = inst.replaceFirst("^0+", "");
				if(instNo.isEmpty()) {
					return instNo;
				}
				instNo = org.apache.commons.lang.StringUtils.leftPad(instNo, 6, "0");
				if(year != SimpleChapterUtils.UNDEFINED_YEAR){
					instNo = year + instNo;
				}
				return instNo;
			}
			
			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				if(StringUtils.isNotEmpty(state.getInstno())) {
					if(filterCriteria != null) {
						filterCriteria.put("InstrumentNumber", state.getInstno());
					}
				}
				return state.getInstno();
			}
		};
		return iterator;
	}
	
	/**
     * Looks for the a document having the same instrumentNo
     * @param instrumentNo
     * @param data
     * @return
     */
    public boolean isInstrumentSavedInIntermediary(String instrumentNo, HashMap<String, String> data){
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	boolean firstTry = super.isInstrumentSaved(instrumentNo, null, data);
		
		if(firstTry) {
			return true;
		}
			
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		boolean validServerDoctype = false;
    		String docCateg = null;
    		
    		if (mSearch.getCountyId().equals(CountyConstants.AR_Pulaski_STRING) && instrumentNo.startsWith("19")){
    			instrumentNo = instrumentNo.substring(2, instrumentNo.length());
    		}
			InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
			if(data != null) {
				
	    		if(!StringUtils.isEmpty(data.get("type"))) {
	        		String serverDocType = data.get("type");
	        		if(serverDocType.length() == 3) {
	        			validServerDoctype = true;
	        			docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType(DocumentTypes.getDocumentSubcategory(serverDocType, searchId));
	        		} else {
	        			// in some intermediary we do not have the full document type so we need to force ATS to ignore category
	        			docCateg = DocumentTypes.MISCELLANEOUS; 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType(DocumentTypes.MISCELLANEOUS);
	        		}
	    	    	
	    		}
	    		
	    		instr.setBook(data.get("book"));
	    		instr.setPage(data.get("page"));
	    		instr.setDocno(data.get("docno"));
			}
			
			try {
				instr.setYear(Integer.parseInt(data.get("year")));
			} catch (Exception e) {}
			
			if(documentManager.getDocument(instr) != null) {
				return true;	//we are very lucky
			} else {
				List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
				if(almostLike.size() == 0) {
					return false;
				}
				if(data!=null) {
					if(StringUtils.isNotEmpty(docCateg)){
		    	    	for (DocumentI documentI : almostLike) {
		    	    		if( (!validServerDoctype || documentI.getDocType().equals(docCateg))){
								return true;
		    	    		}
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
    		
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }
    
    
    public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data){
    	/**
    	 * We must ignore the source of the document because we might have it from other DS like TS 
    	 */
    	boolean firstTry = super.isInstrumentSaved(instrumentNo, documentToCheck, data, false);
		
		if(firstTry) {
			return true;
		}
    	
		if (mSearch.getCountyId().equals(CountyConstants.AR_Pulaski_STRING)){
			
			DocumentsManagerI documentManager = getSearch().getDocManager();
	    	try {
	    		documentManager.getAccess();
	    		if (instrumentNo.startsWith("19")){
	    			instrumentNo = instrumentNo.substring(2, instrumentNo.length());
    			}
	    		InstrumentI instToCheck = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		instToCheck.setDocType(DocumentTypes.getDocumentCategory(data.get("type"), searchId));
	    		instToCheck.setBook(data.get("book"));
	    		instToCheck.setPage(data.get("page"));
	    		
	    		for(DocumentI e: documentManager.getDocumentsWithDataSource(false, "TS")){
	    			InstrumentI savedInst = e.getInstrument();
	    			
	    			if( savedInst.getInstno().equals(instToCheck.getInstno())  
	    					&& (savedInst.getBook().equals(instToCheck.getBook()) && savedInst.getPage().equals(instToCheck.getPage()))  
	    					&& savedInst.getDocno().equals(instToCheck.getDocno())
	    					&& e.getServerDocType().equals(instToCheck.getDocType())
	    			){
	    				return true;
	    			}
	    		}
	    	} finally {
	    		documentManager.releaseAccess();
	    	}
		}
		return false;
	}
	
	private LegalDescriptionIterator getLegalDescriptionIterator(boolean lookUpWasDoneWithNames) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookUpWasDoneWithNames, false, getDataSite()) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void loadDerrivation(TSServerInfoModule module,
					LegalStruct str) {
				for (Object functionObject : module.getFunctionList()) {
					if (functionObject instanceof TSServerInfoFunction) {
						TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
						switch (function.getIteratorType()) {
						case FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE:
							function.setParamValue(str.getAddition());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_LOT:
							function.setParamValue(str.getLot());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_BLOCK:
							function.setParamValue(str.getBlock());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_SECTION:
							function.setParamValue(str.getSection());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_TOWNSHIP:
							function.setParamValue(str.getTownship());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_RANGE:
							function.setParamValue(str.getRange());
							break;
						}
					}
				}
			}
			
			@Override
			protected void treatOnlySubdivision(String sourceKey, LegalI legal,
					boolean useAlsoSubdivisionName, String subdivisionName) {
				if(isEnableSubdivision() && legal.hasSubdividedLegal()) {
					processSubdivisionLotBlock(
							legal.getSubdivision().getName(), 
							legal.getSubdivision().getLot(), 
							legal.getSubdivision().getBlock());
				}
			}
			
			@Override
			protected void treatOnlyTownshipLegal(String sourceKey, LegalI legal,
					boolean useAlsoSubdivisionName, String subdivisionName) {
				if(isEnableTownshipLegal() && legal.hasTownshipLegal()){
					TownShipI township = legal.getTownShip();
					
					String sec = township.getSection();
					String tw = township.getTownship();
					String rg = township.getRange();
					
					LegalStruct legalStruct1 = new LegalStruct(true);
					legalStruct1.setSection(StringUtils.isEmpty(sec)?"":sec);
					legalStruct1.setTownship(StringUtils.isEmpty(tw)?"":tw);
					legalStruct1.setRange(StringUtils.isEmpty(rg)?"":rg);
					legalStruct.add(legalStruct1);
					
				}
			}
			
			@Override
			protected void treatOnlySubdivisionLegalForCurrentOwner(
					final Set<LegalStruct> ret, PartyI owner,
					RegisterDocumentI doc) {
				if(isEnableSubdivision()) {
					for(PropertyI prop:doc.getProperties()){
						if(doc.isOneOf("MORTGAGE","TRANSFER","RELEASE")){
							if(prop.hasSubdividedLegal()){
								SubdivisionI sub = prop.getLegal().getSubdivision();
								LegalStruct struct = new LegalStruct(false);
								struct.setLot(sub.getLot());
								struct.setBlock(sub.getBlock());
								struct.setAddition(sub.getName());
								
								boolean nameMatched = false;
								
								if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
										GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
										GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
										GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
										nameMatched = true;
								}
								
								if( nameMatched && StringUtils.isNotEmpty(struct.getAddition()) &&
										(StringUtils.isNotEmpty(struct.getLot()) || StringUtils.isNotEmpty(struct.getBlock()))){
									ret.add(struct);
								}
							}
						}
					}
				}
			}
			
			@Override
			protected void treatOnlyTownshipLegalForCurrentOwner(
					final Set<LegalStruct> ret,
					List<RegisterDocumentI> listRodocs, PartyI owner) {
				if(isEnableTownshipLegal()) {
					for(RegisterDocumentI doc:listRodocs){
						if(doc.isOneOf("TRANSFER","MORTGAGE","RELEASE")){
							for(PropertyI prop:doc.getProperties()){
								if( prop.hasTownshipLegal()){
									TownShipI sub = prop.getLegal().getTownShip();
									LegalStruct ret1 = new LegalStruct(true);
									ret1.setSection(sub.getSection());
									ret1.setTownship(sub.getTownship());
									ret1.setRange(sub.getRange());
									
									boolean nameMatched = false;
									
									if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
											GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
											GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
											GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
											nameMatched = true;
									}
									
									if( nameMatched && !StringUtils.isEmpty(ret1.getSection())&&!StringUtils.isEmpty(ret1.getTownship())&&!StringUtils.isEmpty(ret1.getRange())){
										ret.add(ret1);
									}
								}
							}
						}
					}
				}
			}
			
			@Override
			protected void performValidationOnList() {
				Set<LegalStruct> tempSet = new HashSet<LegalStruct>();
				if(isEnableSubdivision()) {
					for (LegalStruct personalDataStruct : legalStruct) {
						if(personalDataStruct.isSubdivision()) {
							tempSet.add(personalDataStruct);
						}
					}
					
					if(tempSet.size() > 0) {
						legalStruct.clear();
						legalStruct.addAll(tempSet);
						return;
					}
				}
				if(isEnableTownshipLegal()) {
					for (LegalStruct personalDataStruct : legalStruct) {
						if(StringUtils.isNotEmpty(personalDataStruct.getSection()) && 
								StringUtils.isNotEmpty(personalDataStruct.getTownship())&&
								StringUtils.isNotEmpty(personalDataStruct.getRange())) {
							tempSet.add(personalDataStruct);
						}
					}
					
					if(tempSet.size() > 0) {
						legalStruct.clear();
						legalStruct.addAll(tempSet);
						return;
					}
				}
				
			}
			
		};
		
		it.setCheckAlreadyFilledKeyWithDocuments(AdditionalInfoKeys.AR_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR);
		it.setAdditionalInfoKey(AdditionalInfoKeys.AR_RO_LOOK_UP_DATA);
		it.setEnableTownshipLegal(true);
		it.setEnableSubdividedLegal(false);
		it.setEnableSubdivision(true);
		it.setRoDoctypesToLoad(new String[]{"MORTGAGE", "TRANSFER", "RELEASE"});
		
		return it;
	}
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		Search global = getSearch();
		int searchType = global.getSearchType();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
			TSServerInfoModule m = null;
			
			FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , m );
			DocsValidator rejectSavedDocuments = (new RejectAlreadySavedDocumentsFilterResponse(searchId)).getValidator();
			DocsValidator betweenDatesValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
			GenericMultipleLegalFilter genericMultipleLegalFilter = new GenericMultipleLegalFilter(searchId);
			genericMultipleLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.AR_RO_LOOK_UP_DATA);
			genericMultipleLegalFilter.setUseLegalFromSearchPage(true);
			DocsValidator genericMultipleLegalValidator = genericMultipleLegalFilter.getValidator();
			SubdivisionFilter subdivisionFilter = new SubdivisionFilter(searchId);
			DocsValidator subdivisionValidator = subdivisionFilter.getValidator();
			DocsValidator defaultSingleLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
			DocsValidator lastTransferDateFilter = new LastTransferDateFilter(searchId).getValidator();
			
			boolean lookUpWasDoneWithNames = true;
			{
				/**
				 * Searching with instruments extracted from AO-like, TR-like
				 */
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
				m.clearSaKeys();
				InstrumentGenericIterator instrumentNoInterator = getInstrumentIterator();
				m.addIterator(instrumentNoInterator);
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				
				//add module only if we have something to search with
				if(!instrumentNoInterator.createDerrivations().isEmpty()) {
					m.addFilter(new GenericInstrumentFilter(searchId));
					m.addValidator(lastTransferDateFilter);
					m.addValidator( betweenDatesValidator );
					m.addCrossRefValidator(defaultSingleLegalValidator);
					m.addCrossRefValidator(lastTransferDateFilter);
					m.addCrossRefValidator(betweenDatesValidator);
					modules.add(m);
					lookUpWasDoneWithNames = false;
				} else {
					addNameSearch(
							modules, 
							serverInfo, 
							SearchAttributes.OWNER_OBJECT, 
							TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS,
							null, 
							new FilterResponse[]{ defaultNameFilter }, 
							new DocsValidator[]{
									defaultSingleLegalValidator,
									lastTransferDateFilter,
									subdivisionValidator, 
									rejectSavedDocuments}, 
							new DocsValidator[]{
									defaultSingleLegalValidator, 
									subdivisionValidator,
									lastTransferDateFilter,
									rejectSavedDocuments});
					SearchLogger.info( "<font color='red'><b> No valid transaction History Detected and Not enough info for Subdivision Search. We must perform Name Look Up.</b></font></br>", searchId );
				}
			}
			
			{
				/**
				 * Searching with plated legal. We must have subdivision name and at least lot or block
				 */
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
				m.clearSaKeys();
				
				LegalDescriptionIterator it = getLegalDescriptionIterator(lookUpWasDoneWithNames);
				m.addIterator(it);
				
				m.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				m.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				m.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_LOT);
				m.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_BLOCK);
				m.setIteratorType(11, FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE);
				
				
				GenericMultipleLegalFilter multipleLegalFilter = new GenericMultipleLegalFilter(searchId);
				multipleLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.AR_RO_LOOK_UP_DATA);
				m.addFilterForNext(multipleLegalFilter);
				m.addFilter(genericMultipleLegalFilter);
	
				m.addValidator(genericMultipleLegalValidator);
				m.addValidator(lastTransferDateFilter);
				m.addValidator(rejectSavedDocuments);
				m.addCrossRefValidator(genericMultipleLegalValidator);
				m.addCrossRefValidator(lastTransferDateFilter);
				m.addCrossRefValidator(betweenDatesValidator);
				modules.add(m);
			}

			ArrayList<NameI> searchedNames = null;
			{
				/**
				 * Owner search by name
				 */
				if(hasOwner()) {
					searchedNames = addNameSearch(
							modules, 
							serverInfo, 
							SearchAttributes.OWNER_OBJECT, 
							TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS,
							null, 
							new FilterResponse[]{defaultNameFilter}, 
							new DocsValidator[]{
									genericMultipleLegalValidator, 
									lastTransferDateFilter, 
									subdivisionValidator,
									rejectSavedDocuments}, 
							new DocsValidator[]{
									genericMultipleLegalValidator,
									subdivisionValidator,
									lastTransferDateFilter,
									rejectSavedDocuments});
				}
			}

			{
				/**
				 * OCR last transfer - instrument number search (no Book-Page yet) 
				 */
			    m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			    m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
			    m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
			    m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			    m.addValidator(genericMultipleLegalValidator);
			    addBetweenDateTest(m, true, false, false);
				m.addValidator(lastTransferDateFilter);
			    m.addCrossRefValidator(genericMultipleLegalValidator);
			    m.addCrossRefValidator(lastTransferDateFilter);
			    m.addCrossRefValidator(betweenDatesValidator);
			    m.addCrossRefValidator(rejectSavedDocuments);
			    modules.add(m);
			}
	    
		    {
		    	/**
		    	 * Owner name module with extra names from search page (for example added by OCR)
		    	 */
		    	
		    	searchedNames = addNameSearch(
						modules, 
						serverInfo, 
						SearchAttributes.OWNER_OBJECT, 
						TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS,
						searchedNames, 
						new FilterResponse[]{defaultNameFilter}, 
						new DocsValidator[]{
								genericMultipleLegalValidator, 
								lastTransferDateFilter, 
								subdivisionValidator,
								rejectSavedDocuments}, 
						new DocsValidator[]{
								genericMultipleLegalValidator, 
								subdivisionValidator,
								lastTransferDateFilter,
								rejectSavedDocuments});
		    	
			}
				
			/**
			 * Buyer Search
			 */
			if(hasBuyer()) {
				
				FilterResponse nameFilterBuyer 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, getSearch().getID(), null );
				
				addNameSearch(
						modules, 
						serverInfo, 
						SearchAttributes.BUYER_OBJECT, 
						TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS,
						null, 
						new FilterResponse[]{nameFilterBuyer}, 
						new DocsValidator[]{
								DoctypeFilterFactory.getDoctypeBuyerFilter( searchId ).getValidator(),
								lastTransferDateFilter,
								nameFilterBuyer.getValidator(),
								rejectSavedDocuments}, 
						new DocsValidator[]{genericMultipleLegalValidator, 
								lastTransferDateFilter, 
								rejectSavedDocuments});
			}
		
		
		}
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		Search search = getSearch();
		int searchType = search.getSearchType();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		if(searchType == Search.GO_BACK_ONE_LEVEL_SEARCH) {
			
			GenericMultipleLegalFilter genericMultipleLegalFilter = new GenericMultipleLegalFilter(searchId);
			genericMultipleLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.AR_RO_LOOK_UP_DATA);
			genericMultipleLegalFilter.setUseLegalFromSearchPage(true);
			DocsValidator genericMultipleLegalValidator = genericMultipleLegalFilter.getValidator();
			SubdivisionFilter subdivisionFilter = new SubdivisionFilter(searchId);
			DocsValidator subdivisionValidator = subdivisionFilter.getValidator();
			DocsValidator rejectSavedDocuments = (new RejectAlreadySavedDocumentsFilterResponse(searchId)).getValidator();
			
		    InstrumentAKROIterator instrumentNoInterator = new InstrumentAKROIterator(searchId);
		    boolean useNameForLookUp = false;
			if(instrumentNoInterator.createDerrivations().isEmpty() ){
				useNameForLookUp = true;
			}
			LegalDescriptionIterator it =  getLegalDescriptionIterator(useNameForLookUp);
			it.createDerrivations();	
			
			TSServerInfoModule module = null;
			GBManager gbm = (GBManager)search.getSa().getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
		    for (String id : gbm.getGbTransfers()) {
			
		    	
		    	module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		    	module.setIndexInGB(id);
		    	module.setTypeSearchGB("grantor");
			 	
			 	addNameSearch(
						modules, 
						serverInfo, 
						SearchAttributes.GB_MANAGER_OBJECT, 
						TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS,
						null, 
						new FilterResponse[]{
								NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module),
								NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module)}, 
						new DocsValidator[]{
								genericMultipleLegalValidator, 
								subdivisionValidator,
								rejectSavedDocuments,
								DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator()}, 
						new DocsValidator[]{
								genericMultipleLegalValidator, 
								subdivisionValidator,
								rejectSavedDocuments, 
								DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator()},
						module);
			 	module.clearSaKey(6);
			 	String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
				if (date != null)
					module.forceValue(6, date);
			     
			 	if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			 		module =new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
			 		module.setIndexInGB(id);
			 		module.setTypeSearchGB("grantee");
			 	
				 	addNameSearch(
							modules, 
							serverInfo, 
							SearchAttributes.GB_MANAGER_OBJECT, 
							TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS,
							null, 
							new FilterResponse[]{
									NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module),
									NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module)}, 
							new DocsValidator[]{
									genericMultipleLegalValidator, 
									subdivisionValidator,
									rejectSavedDocuments,
									DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator()}, 
							new DocsValidator[]{
									genericMultipleLegalValidator, 
									subdivisionValidator,
									rejectSavedDocuments, 
									DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator()},
							module);
				 	module.clearSaKey(6);
				 	date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
					if (date != null)
						module.forceValue(6, date);
			 		
			 	}
		    }	 
		}
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}
	
	/**
	 * Easy way to add name modules
	 * @param modules
	 * @param serverInfo
	 * @param key
	 * @param typeForGoBack
	 * @param extraInformation
	 * @param searchedNames
	 * @param filters
	 * @param docsValidators
	 * @param docsValidatorsCrossref
	 * @return the names that will be searched
	 */
	protected ArrayList<NameI>  addNameSearch( 
			List<TSServerInfoModule> modules, 
			TSServerInfo serverInfo,
			String key, 
			String extraInformation,
			ArrayList<NameI> searchedNames, 
			FilterResponse[] filters,
			DocsValidator[] docsValidators,
			DocsValidator[] docsValidatorsCrossref) {
		return addNameSearch(modules, serverInfo, key, extraInformation, 
				searchedNames, filters, docsValidators, docsValidatorsCrossref, null);
	}
	
	protected ArrayList<NameI>  addNameSearch( 
			List<TSServerInfoModule> modules, 
			TSServerInfo serverInfo,
			String key, 
			String extraInformation,
			ArrayList<NameI> searchedNames, 
			FilterResponse[] filters,
			DocsValidator[] docsValidators,
			DocsValidator[] docsValidatorsCrossref, TSServerInfoModule m) {
		
		if(m == null) {
    		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		}
    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			extraInformation);
		m.setSaObjKey(key);
		m.clearSaKeys();
		m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
		m.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		m.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
		
		for (FilterResponse filterResponse : filters) {
			m.addFilter(filterResponse);
		}
		addFilterForUpdate(m, true);
		for (DocsValidator docsValidator : docsValidators) {
			m.addValidator(docsValidator);
		}
		for (DocsValidator docsValidator : docsValidatorsCrossref) {
			m.addCrossRefValidator(docsValidator);
		}
		m.addCrossRefValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator());
		
		ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(m, false, searchId, new String[] {"L;F;", "L;F;M" });
		nameIterator.setInitAgain(true);		//initialize again after all parameters are set
		if ( searchedNames!=null ) {
			nameIterator.setSearchedNames( searchedNames );
		}
		searchedNames = nameIterator.getSearchedNames() ;
		
		m.addIterator(nameIterator);
		modules.add(m);
		
		return searchedNames;
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();

		TSServerInfoModule module = null;
		if(StringUtils.isNotEmpty(instrumentNumber)) {
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			filterCriteria.put("InstrumentNumber", instrumentNumber);
			module.forceValue(0, instrumentNumber);
			module.forceValue(1, "Search By Instrument #");;
			module.forceValue(2, "instrument");
			module.forceValue(6, "Search Records");
			GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
			module.getFilterList().clear();
			module.addFilter(filter);
		}
		return module;
	}
	
	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 1) {
			String usedName = module.getFunction(1).getParamValue();
			if(StringUtils.isEmpty(usedName)) {
				return null;
			}
			String[] names = null;
			if(NameUtils.isCompany(usedName)) {
				names = new String[]{"", "", usedName, "", "", ""};
			} else {
				names = StringFormats.parseNameNashville(usedName, true);
			}
			name.setLastName(names[2]);
			name.setFirstName(names[0]);
			name.setMiddleName(names[1]);
			return name;
		}
		return null;
	}
	
	@Override
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		LegalI legal = null;
		
		if(module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX && module.getFunctionCount() > 15) {
			SubdivisionI subdivision = new Subdivision();
			
			String subdivisionName = module.getFunction(11).getParamValue().trim();
			subdivision.setName(subdivisionName);
			subdivision.setLot(module.getFunction(9).getParamValue().trim());
			subdivision.setBlock(module.getFunction(10).getParamValue().trim());
			
			TownShipI townShip = new TownShip();
			
			townShip.setSection(module.getFunction(13).getParamValue().trim());
			townShip.setTownship(module.getFunction(14).getParamValue().trim());
			townShip.setRange(module.getFunction(15).getParamValue().trim());
			
			legal = new Legal();
			legal.setSubdivision(subdivision);
			legal.setTownShip(townShip);
		}
		
		return legal;
	}
    
	@Override
	protected void setCertificationDate() {
		try {
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
				String page = getLinkContents(dataSite.getLink());
					
				if (StringUtils.isNotEmpty(page)){
					Matcher certDateMatcher = certDatePattern.matcher(page);
					
					if(certDateMatcher.find()) {
						String date = certDateMatcher.group(1).trim();
						date = DateFormatUtils.format(Util.dateParser3(date), "MM/dd/yyyy");
						
						CertificationDateManager.cacheCertificationDate(dataSite, date);
						getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
					} else {
						CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because pattern not found");
					}
				} else {
					CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because html response is empty");
				}
			}
        } catch (Exception e) {
        	CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
        }
	}
}

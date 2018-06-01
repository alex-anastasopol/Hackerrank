package ro.cst.tsearch.servers.types;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axis2.AxisFault;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.LinkParser;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.ExactDateFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrFakeIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DocumentParsedResponse;
import ro.cst.tsearch.servers.functions.GenericATIDSFunctions;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;

import com.atids.services.AtidsS2SServiceStub.ArrayOfCountyCertificationDetail;
import com.atids.services.AtidsS2SServiceStub.ArrayOfSearchMatch;
import com.atids.services.AtidsS2SServiceStub.ArrayOfSearchWarning;
import com.atids.services.AtidsS2SServiceStub.CertificationRange;
import com.atids.services.AtidsS2SServiceStub.CountyCertificationDetail;
import com.atids.services.AtidsS2SServiceStub.GetAtidsCertificationDetailsResponse;
import com.atids.services.AtidsS2SServiceStub.GetConveyanceDocumentInformationResponse;
import com.atids.services.AtidsS2SServiceStub.GetDocumentImageAvailabilityResponse;
import com.atids.services.AtidsS2SServiceStub.GetDocumentInformationResponse;
import com.atids.services.AtidsS2SServiceStub.GetPlatInformationByNameResponse;
import com.atids.services.AtidsS2SServiceStub.OfficialRecordDocument;
import com.atids.services.AtidsS2SServiceStub.PlatInformation;
import com.atids.services.AtidsS2SServiceStub.SearchDetails;
import com.atids.services.AtidsS2SServiceStub.SearchMatch;
import com.atids.services.AtidsS2SServiceStub.SearchWarning;
import com.atids.services.AtidsS2SServiceStub.SearchWarningType;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionDetailed;
import com.stewart.ats.base.legal.SubdivisionDetailedI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.warning.MissingLandSearchWarning;
import com.stewart.ats.base.warning.Warning;
import com.stewart.ats.base.warning.WarningCustomInfo;
import com.stewart.ats.connection.atids.AtidsConn;
import com.stewart.ats.connection.atids.AtidsConnWrapper;
import com.stewart.ats.connection.atids.AtidsException;
import com.stewart.ats.connection.atids.AtidsUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;

public class GenericATIDS extends TSServerROLike {

	private static final long serialVersionUID = 1L;
	
	private static GetAtidsCertificationDetailsResponse certDate = null;
	
	public static final String MESSAGE_ANOTHER_FILE_FOUND = "Another file was found for this reference.";
	public static final String MESSAGE_FILE_REF_INVALID = "File id invalid according to ATI";
	
	
	public static final String WARNING_ANOTHER_FILE_FOUND = "Another file was found for this reference[ATI]";
	
	
	private transient AtidsConnWrapper connection = null;

	public GenericATIDS(long searchId) {
		super(searchId);
		setRangeNotExpanded(true);
	}

	public GenericATIDS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		setRangeNotExpanded(true);
	}
	
	@Override
	protected ServerResponse SearchBy(boolean bResetQuery, TSServerInfoModule module, Object sd)
			throws ServerResponseException {
		
		if (module.getModuleIdx()==TSServerInfo.FAKE_MODULE_IDX) {
			ServerResponse sr = new ServerResponse();
    		RestoreDocumentDataI restoreDocumentDataI = (RestoreDocumentDataI)module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE);
			if(restoreDocumentDataI != null) {
				Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
				
				RegisterDocumentI docR = restoreDocumentDataI.toRegisterDocument(getSearch(), getDataSite());
				
				LinkInPage linkInPage = new LinkInPage(
						getLinkPrefix(TSConnectionURL.idPOST) + "FK____" + docR.getId(), 
						getLinkPrefix(TSConnectionURL.idPOST) + "FK____" + docR.getId(), 
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
		
		if (!isParentSite() && "true".equalsIgnoreCase(getSearch().getSa().getAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND))) {
			return new ServerResponse();
		}
		
		List<TSServerInfoModule> modules = getMultipleModules(module, sd);
		
		ServerResponse response = null;
		
		if(modules.size() > 1){
			response = searchByMultipleModules(bResetQuery, modules, sd);
		} else {
			response = searchBy(bResetQuery, module, sd);
		}
		
		if(response != null && response.getParsedResponse() != null) {
			response.getParsedResponse().setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, module);
		}
		
		return response;
	}
	
	protected boolean verifyModule(TSServerInfoModule mod) {

		try {
			if (mod.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX || mod.getModuleIdx() == TSServerInfo.MODULE_IDX41) {
				TSServerInfoFunction f = null;
				TSServerInfoFunction l = null;

				for (TSServerInfoFunction func : mod.getFunctionList()) {
					if (func.getParamName().equals("LastName"))
						f = func;
					if (func.getParamName().equals("FirstName"))
						l = func;
					if (f != null && l != null)
						break;
				}

				if (StringUtils.isNotBlank(l.getParamValue()) && StringUtils.isNotBlank(f.getParamValue())) {
					return true;
				}
				return false;
			}
			
			if (mod.getModuleIdx() == TSServerInfo.BUSINESS_NAME_MODULE_IDX || mod.getModuleIdx() == TSServerInfo.MODULE_IDX42) {
				TSServerInfoFunction l = null;

				for (TSServerInfoFunction func : mod.getFunctionList()) {
					if (func.getParamName().equals("Name"))
						l = func;
					if (l != null)
						break;
				}

				if (StringUtils.isNotBlank(l.getParamValue())) {
					return true;
				}
				return false;
			}

			if (mod.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX || mod.getModuleIdx() == TSServerInfo.MODULE_IDX39
					 														|| mod.getModuleIdx() == TSServerInfo.MODULE_IDX44) {
				TSServerInfoFunction b = null;
				TSServerInfoFunction p = null;
				TSServerInfoFunction s = null;

				for (TSServerInfoFunction func : mod.getFunctionList()) {
					if (func.getParamName().equals("Book"))
						b = func;
					if (func.getParamName().equals("Page"))
						p = func;
					if (func.getParamName().equals("Source"))
						s = func;
					if (b != null && p != null && s != null)
						break;
				}
							
				if (StringUtils.isNotBlank(b.getParamValue()) && StringUtils.isNotBlank(p.getParamValue()) && StringUtils.isNotBlank(s.getParamValue())) {
					return true;
				}
				return false;
			}

			if (mod.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX || mod.getModuleIdx() == TSServerInfo.MODULE_IDX38
																		|| mod.getModuleIdx() == TSServerInfo.MODULE_IDX43) {
				TSServerInfoFunction i = null;
				TSServerInfoFunction y = null;
				TSServerInfoFunction s = null;

				for (TSServerInfoFunction func : mod.getFunctionList()) {
					if (func.getParamName().equals("Number"))
						i = func;
					if (func.getParamName().equals("Source"))
						s = func;
					if (func.getParamName().equals("Year"))
						y = func;
					if (i != null && y != null && s != null)
						break;
				}
				
				if (StringUtils.isNotBlank(i.getParamValue()) && StringUtils.isNotBlank(y.getParamValue()) && StringUtils.isNotBlank(s.getParamValue())) {
					return true;
				}
				return false;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.err.println(this.getClass() + " I shouldn't be here!!!");
		return false;
	}
	
	private ServerResponse searchByMultipleModules(boolean bResetQuery, List<TSServerInfoModule> modules, Object sd) {
		try {
			if (modules.size() <= 1)
				return null;

			List<ServerResponse> serverResponses = new ArrayList<ServerResponse>();
			boolean firstSearchBy = true;
			
			for (TSServerInfoModule mod : modules) {
				if (verifyModule(mod)) {
					
					if(firstSearchBy) {
	    				firstSearchBy = false;
	    			} else {
	    				mod.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
	    			}
					ServerResponse res = searchBy(bResetQuery, mod, sd);
					if (res != null)
						serverResponses.add(res);
				}
			}
			
			if(!serverResponses.isEmpty()){
				return mergeResults(serverResponses);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	protected ServerResponse mergeResults(List<ServerResponse> serverResponses) {

		ServerResponse response = new ServerResponse();
		Vector<ParsedResponse> rows = new Vector<ParsedResponse>();
		
		for (ServerResponse res : serverResponses) {
			try {
				if(res.getParsedResponse().getResultRows().size() != 0){
					rows.addAll(res.getParsedResponse().getResultRows());
				} 
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(rows.size() == 0){
			return ServerResponse.createWarningResponse(NO_DATA_FOUND);
		}
		
		response.getParsedResponse().setResultRows(rows);

		String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		header += "\n<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
				"<tr><th rowspan=1>" + SELECT_ALL_CHECKBOXES + "</th>" +
				"<td align=\"center\">Document Content</td></tr>";

		int nrUnsavedDoc = rows.size();

		String footer = "\n</table><br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, nrUnsavedDoc);

		response.getParsedResponse().setHeader(header);
		response.getParsedResponse().setFooter(footer);
		
		response.getParsedResponse().setOnlyResponse("");
		response.setResult("");

		return response;

	}
	
	protected ServerResponse searchBy (boolean bResetQuery, TSServerInfoModule module, Object sd) throws ServerResponseException {
		
		setCertificationDate();
		
		getSearch().clearClickedDocuments();
        if(!Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS))
        		&& module.getModuleIdx() != TSServerInfo.IMG_MODULE_IDX) {
        	//this is needed because, sometimes, doing a search for image will 
        	//destroy the document viewed but not yet saved in parent site.
        	
        	if(module.getModuleIdx() == TSServerInfo.MODULE_IDX38 
        			|| module.getModuleIdx() == TSServerInfo.MODULE_IDX39) {
        		String fakeName = module.getParamValue(5);
				if(StringUtils.isBlank(fakeName)) {
					getSearch().removeAllInMemoryDocs();	
				}
        	} else {
        		getSearch().removeAllInMemoryDocs();
        	}
        }
        
        logSearchBy(module);
		
        ServerResponse serverResponse = null;
        
		try {
			switch (module.getModuleIdx()) {
				case TSServerInfo.NAME_MODULE_IDX:
					serverResponse = processResponse(module, getConnection().searchByPersonName(module));
					break;
				case TSServerInfo.BUSINESS_NAME_MODULE_IDX:
					serverResponse = processResponse(module, getConnection().searchByCompanyName(module));
					break;
				case TSServerInfo.SUBDIVISION_MODULE_IDX:
				case TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX:
					serverResponse = processResponse(module, getConnection().searchByPlattedLegal(module, sd));
					break;
				case TSServerInfo.SECTION_LAND_MODULE_IDX:
				case TSServerInfo.SURVEYS_MODULE_IDX:
					serverResponse = processResponse(module, getConnection().searchByUnplattedLegal(module, sd));
					break;
				case TSServerInfo.MODULE_IDX41:
					serverResponse = processResponse(module, getConnection().searchByGrantorGranteePersonalName(module));
					break;
				case TSServerInfo.MODULE_IDX42:
					serverResponse = processResponse(module, getConnection().searchByGrantorGranteeCompanyName(module));
					break;
				case TSServerInfo.INSTR_NO_MODULE_IDX:
				case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
					serverResponse = processResponse(module, getConnection().searchByConveyanceDocumentInformation(module, sd)); 
					break;
				case TSServerInfo.MODULE_IDX38:	//IN
				case TSServerInfo.MODULE_IDX39:	//BP
					serverResponse = processResponse(module, getConnection().searchByDocumentImageInformation(module, sd)); 
					
					String fakeName = module.getParamValue(5);
					
					if(StringUtils.isNotBlank(fakeName)) {
						//we need to go and get the image
						ImageI image = null;
						
						if(serverResponse != null && serverResponse.getParsedResponse() != null && serverResponse.getParsedResponse().getDocument() != null) {
							image = serverResponse.getParsedResponse().getDocument().getImage();
						} else {
							image = null;
						}
						serverResponse = new ServerResponse();
						serverResponse.setImageResult(saveImage(image));
						serverResponse.getParsedResponse().setSolved(true);
					}
					
					break;
				case TSServerInfo.MODULE_IDX43://IN
				case TSServerInfo.MODULE_IDX44://BP
					serverResponse = processResponse(module, getConnection().searchByDocumentInformation(module, sd)); 
					
					String fkeName = module.getParamValue(5);
					
					if(StringUtils.isNotBlank(fkeName)) {
						//we need to go and get the image
						ImageI image = null;
						
						if(serverResponse != null && serverResponse.getParsedResponse() != null && serverResponse.getParsedResponse().getDocument() != null) {
							image = serverResponse.getParsedResponse().getDocument().getImage();
						} else {
							image = null;
						}
						serverResponse = new ServerResponse();
						serverResponse.setImageResult(saveImage(image));
						serverResponse.getParsedResponse().setSolved(true);
					}
					
					break;
				case TSServerInfo.MODULE_IDX40:
					GetPlatInformationByNameResponse searchByPlatInformation = getConnection().searchByPlatInformation(module);
					serverResponse = processResponse(module, searchByPlatInformation);
					break;
				case TSServerInfo.IMG_MODULE_IDX:
					
					serverResponse = new ServerResponse();
		    		ImageI image = new Image();
		    		Set<String> list = new HashSet<String>();
		    		list.add("&imageToken=" + module.getFunction(0).getParamValue() + 
		    				"&fakeName=" + module.getFunction(1).getParamValue());
		    		image.setLinks(list);
		    		image.setContentType("image/tiff");
		    		serverResponse.setImageResult(saveImage(image));
		    		break;
					
			}
		} catch (AxisFault axisFault) {
			logger.error("AxisFault while performing search with moduleId " + module.getModuleIdx(), axisFault);
			serverResponse = getServerResponseExpectedException(axisFault);
			if(serverResponse != null) {
				logInitialResponse(serverResponse);
				return serverResponse;
			}
		} catch (AtidsException atidsException){
			if(org.apache.commons.lang.StringUtils.isBlank(atidsException.getMessage())) {
				logger.error("AtidsException while performing search with moduleId " + module.getModuleIdx(), atidsException);
			}
			serverResponse = getServerResponseExpectedException(atidsException);
			if(serverResponse != null) {
				logInitialResponse(serverResponse);
				return serverResponse;
			}
		} catch (IllegalArgumentException illegalArgumentException) {
			serverResponse = getServerResponseExpectedException(illegalArgumentException);
			if(serverResponse != null) {
				logInitialResponse(serverResponse);
				return serverResponse;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (serverResponse == null){
			serverResponse = ServerResponse.createErrorResponse("Error when processing response.");
        	logInitialResponse(serverResponse);
		}
		return serverResponse;
	}
	
	private ServerResponse getServerResponseExpectedException(Exception exception) {
		if(exception == null) {
			return null;
		}
		
		ServerResponse serverResponse = new ServerResponse();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		
		String message = exception.getMessage();
		if(message == null) {
			
		} else if (message.contains("element is invalid")) {
			String errorMessage = "At least one element is invalid.";
    		
    		
    		Pattern invalidElementPattern = Pattern.compile("(The value '.*?' is invalid according to its datatype )'.*?:([^:']+)'");
    		Matcher matcher = invalidElementPattern.matcher(message);
    		if(matcher.find()) {
    			errorMessage = matcher.group(1) + "'" + matcher.group(2) + "'";
    		}
    		
    		serverResponse.setError(errorMessage);
    		parsedResponse.setError(errorMessage);
    		
    		return serverResponse;
        	
		} else if (message.contains("Illegal Argument")) {
			
			serverResponse.setError(message);
    		parsedResponse.setError(message);
			return serverResponse;
			
		} else if (message.contains("invalid according to its datatype 'FileReference'")) {
			String errorMessage = MESSAGE_FILE_REF_INVALID;
			serverResponse.setError(errorMessage);
    		parsedResponse.setError(errorMessage);
    		
    		return serverResponse;
    		
		} else if(message.equals("Connection timed out: connect")) {
			String errorMessage = "Site Error: Connection timed out.";
			serverResponse.setError(errorMessage);
    		parsedResponse.setError(errorMessage);
    		
    		return serverResponse;
		} else if(message.equals(AtidsConn.MESSAGE_NO_FILE_FOR_REF)) {
			String errorMessage = MESSAGE_ANOTHER_FILE_FOUND;
			serverResponse.setError(errorMessage);
    		parsedResponse.setError(errorMessage);
    		
    		return serverResponse;
		} else if(message.equals(AtidsConn.MESSAGE_LIMIT_NAME_REACHED) || message.contains(AtidsConn.MESSAGE_PART_NO_LONGER_USED)) {
			String errorMessage = message;
			serverResponse.setError(errorMessage);
    		parsedResponse.setError(errorMessage);
    		
    		return serverResponse;
		}
		
		return null;
	}
	
	protected void logInitialResponseSpecific(ServerResponse response) {
		
		String parsedResponseError = response.getParsedResponse().getError();
		
		if(StringUtils.isBlank(parsedResponseError)) {
			return;
		}
		
		if(GenericATIDS.MESSAGE_ANOTHER_FILE_FOUND.equals(parsedResponseError)) {
			WarningCustomInfo warningCustomInfo = new WarningCustomInfo(GenericATIDS.WARNING_ANOTHER_FILE_FOUND);
			getSearch().getSearchFlags().addWarning(warningCustomInfo);
		} else if(GenericATIDS.MESSAGE_FILE_REF_INVALID.equals(parsedResponseError)){
			WarningCustomInfo warningCustomInfo = new WarningCustomInfo("File id [" + getSearch().getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO) + "] invalid according to datasource[ATI]");
			getSearch().getSearchFlags().addWarning(warningCustomInfo);
		} else if(AtidsConn.MESSAGE_LIMIT_NAME_REACHED.equals(parsedResponseError) || parsedResponseError.contains(AtidsConn.MESSAGE_PART_NO_LONGER_USED)) {
			WarningCustomInfo warningCustomInfo = new WarningCustomInfo(response.getParsedResponse().getError() + "[ATI]");
			getSearch().getSearchFlags().addWarning(warningCustomInfo);
		}
	}

	private ServerResponse processResponse(TSServerInfoModule module, GetDocumentImageAvailabilityResponse response) {
		ServerResponse serverResponse = new ServerResponse();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		
		try{
			
			if(response == null) {
				serverResponse = ServerResponse.createErrorResponse("Site returned error for this request.");
	        	logInitialResponse(serverResponse);
	        	return serverResponse;
	    	}
			
			if(!response.getIsAvailable()){
				serverResponse = ServerResponse.createEmptyResponse();
				logInitialResponse(serverResponse);
	        	return serverResponse;
			}
			
			Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
	    	StringBuilder htmlContent = new StringBuilder();
	    	String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
	    	
	    	ParsedResponse currentResponse = new ParsedResponse();
	    	RegisterDocumentI registerDocumentI = GenericATIDSFunctions.getDocument(response, getSearch(), getDataSite(), module);
	    	if(isParentSite()) {
				registerDocumentI.setSavedFrom(SavedFromType.PARENT_SITE);
			} else {
				registerDocumentI.setSavedFrom(SavedFromType.AUTOMATIC);
			}
			currentResponse.setDocument(registerDocumentI);
			parsedResponse.setDocument(registerDocumentI);
			
			String checkBox = "checked";
			if (isInstrumentSaved("gogo", registerDocumentI, null, false) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
    			checkBox = "saved";
    		} else {
    			checkBox = "<input type='checkbox' name='docLink' value='" + linkPrefix + "FK____" + registerDocumentI.getId() + "'>";
    			
    			LinkInPage linkInPage = new LinkInPage(
    					linkPrefix + "FK____" + registerDocumentI.getId(), 
    					linkPrefix + "FK____" + registerDocumentI.getId(), 
    					TSServer.REQUEST_SAVE_TO_TSD);
    			
    			if(getSearch().getInMemoryDoc(linkPrefix + "FK____" + registerDocumentI.getId())==null){
    				getSearch().addInMemoryDoc(linkPrefix + "FK____" + registerDocumentI.getId(), currentResponse);
    				
    				/**
        			 * Save module in key in additional info. The key is instrument number that should be always available. 
        			 */
        			String keyForSavingModules = getKeyForSavingInIntermediary(registerDocumentI.getInstno());
        			getSearch().setAdditionalInfo(keyForSavingModules, module);
    			}
    			currentResponse.setPageLink(linkInPage);
    		}
			
			String imageLink = null;
			
			if(registerDocumentI.hasImage()) {
				imageLink = "<br><a href=\"" + createPartialLink(TSConnectionURL.idPOST, TSServerInfo.IMG_MODULE_IDX) + 
						GenericATIDSFunctions.getShortImageLink(response.getImageToken(), true) + 
						"\" title=\"View Image\" target=\"_blank\">View Image</a>";
			}
			
			String asHtml = registerDocumentI.asHtml(); 
			currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + asHtml + (imageLink!=null?imageLink:"")  + "</td></tr>");
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + asHtml + "</td></tr>");
			currentResponse.setSearchId(searchId);
			currentResponse.setUseDocumentForSearchLogRow(true);
			responses.add(currentResponse);
			
			parsedResponse.setResultRows(new Vector<ParsedResponse>(responses));
			parsedResponse.setOnlyResponse(htmlContent.toString());
			serverResponse.setResult(htmlContent.toString());
			
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
            	String header = parsedResponse.getHeader();
               	String footer = parsedResponse.getFooter();                           	
            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
            	header += "\n<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
            	        "<tr><th rowspan=1>"+ SELECT_ALL_CHECKBOXES + "</th>" +
            			"<td align=\"center\">Document Content</td></tr>";

            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, (Integer)numberOfUnsavedDocument);
            	} else {
            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, -1);
            	}
            	
            	parsedResponse.setHeader(header);
            	parsedResponse.setFooter(footer);
            	
            	StringBuilder sb = new StringBuilder();
        		
        		sb.append(ro.cst.tsearch.utils.StringUtils.createCollapsibleHeader())
        			.append("<table border='1' cellspacing='0' width='99%'>")
        			.append("<tr><th>No</th>")
                	.append("<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th></tr>");
        		int index = 0;
        		for (ParsedResponse row : responses) {
        			sb.append("<tr class='row")
	        			.append(((index%2)+1))
	        			.append("' id='")
	        			.append(String.valueOf(System.nanoTime()))
	        			.append("_passed'><td>")
	        			.append(index + 1)
	        			.append(row.getTsrIndexRepresentation())
	        			.append("</tr>");
        	    	index ++;
				}
        		sb.append("</table></div>");
        		
        		SearchLogger.info(sb.toString(), searchId);
            }
			
			return serverResponse;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return null;
	}

	private ServerResponse processResponse(TSServerInfoModule module, GetConveyanceDocumentInformationResponse response) {
		ServerResponse serverResponse = new ServerResponse();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		
		try{
			if(response == null) {
	        	serverResponse = ServerResponse.createErrorResponse("Site returned error for this request.");
	        	logInitialResponse(serverResponse);
	        	return serverResponse;
	    	}
			
			if(response.getOfficialRecordDocument() == null){
				serverResponse = ServerResponse.createEmptyResponse();
	        	logInitialResponse(serverResponse);
	        	return serverResponse;
			}
			
//          if(!response.getIsNotEligibleConveyanceDocument()){
//				String errorMessage = "This is not eligible conveyance document! Please change your criteria and try again!";
//				serverResponse.setError(errorMessage);
//	    		parsedResponse.setError(errorMessage);
//	        	return serverResponse;
//			}
			
			Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
	    	StringBuilder htmlContent = new StringBuilder();
	    	String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
	    	
	    	ParsedResponse currentResponse = new ParsedResponse();
			RegisterDocumentI registerDocumentI = GenericATIDSFunctions.getDocument(response.getOfficialRecordDocument(), getSearch(), getDataSite(), module);
			if(isParentSite()) {
				registerDocumentI.setSavedFrom(SavedFromType.PARENT_SITE);
			} else {
				registerDocumentI.setSavedFrom(SavedFromType.AUTOMATIC);
			}
			GenericATIDSFunctions.addPropertiesToRegisterDoc(registerDocumentI, response);
			
			currentResponse.setDocument(registerDocumentI);
			
			String checkBox = "checked";
			if (isInstrumentSaved("gogo", registerDocumentI, null, false) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
    			checkBox = "saved";
    		} else {
    			checkBox = "<input type='checkbox' name='docLink' value='" + linkPrefix + "FK____" + registerDocumentI.getId() + "'>";
    			
    			LinkInPage linkInPage = new LinkInPage(
    					linkPrefix + "FK____" + registerDocumentI.getId(), 
    					linkPrefix + "FK____" + registerDocumentI.getId(), 
    					TSServer.REQUEST_SAVE_TO_TSD);
    			
    			if(getSearch().getInMemoryDoc(linkPrefix + "FK____" + registerDocumentI.getId())==null){
    				getSearch().addInMemoryDoc(linkPrefix + "FK____" + registerDocumentI.getId(), currentResponse);
    				
    				/**
        			 * Save module in key in additional info. The key is instrument number that should be always available. 
        			 */
        			String keyForSavingModules = getKeyForSavingInIntermediary(registerDocumentI.getInstno());
        			getSearch().setAdditionalInfo(keyForSavingModules, module);
    			}
    			currentResponse.setPageLink(linkInPage);
    		}
			
			String imageLink = null;
			
			if(registerDocumentI.hasImage()) {
				imageLink = "<br><a href=\"" + createPartialLink(TSConnectionURL.idPOST, TSServerInfo.IMG_MODULE_IDX) + 
						GenericATIDSFunctions.getShortImageLink(response.getOfficialRecordDocument().getImageToken(), true) + 
						"\" title=\"View Image\" target=\"_blank\">View Image</a>";
			}
			
			String asHtml = registerDocumentI.asHtml(); 
			currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + asHtml + (imageLink!=null?imageLink:"")  + "</td></tr>");
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + asHtml + "</td></tr>");
			currentResponse.setSearchId(searchId);
			currentResponse.setUseDocumentForSearchLogRow(true);
			responses.add(currentResponse);
			
			parsedResponse.setResultRows(new Vector<ParsedResponse>(responses));
			parsedResponse.setOnlyResponse(htmlContent.toString());
			serverResponse.setResult(htmlContent.toString());
			
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
            	String header = parsedResponse.getHeader();
               	String footer = parsedResponse.getFooter();                           	
            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
            	header += "\n<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
            	        "<tr><th rowspan=1>"+ SELECT_ALL_CHECKBOXES + "</th>" +
            			"<td align=\"center\">Document Content</td></tr>";

            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, (Integer)numberOfUnsavedDocument);
            	} else {
            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, -1);
            	}
            	
            	parsedResponse.setHeader(header);
            	parsedResponse.setFooter(footer);
            	
            	StringBuilder sb = new StringBuilder();
        		
        		sb.append(ro.cst.tsearch.utils.StringUtils.createCollapsibleHeader())
        			.append("<table border='1' cellspacing='0' width='99%'>")
        			.append("<tr><th>No</th>")
                	.append("<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th></tr>");
        		int index = 0;
        		for (ParsedResponse row : responses) {
        			sb.append("<tr class='row")
	        			.append(((index%2)+1))
	        			.append("' id='")
	        			.append(String.valueOf(System.nanoTime()))
	        			.append("_passed'><td>")
	        			.append(index + 1)
	        			.append(row.getTsrIndexRepresentation())
	        			.append("</tr>");
        	    	index ++;
				}
        		sb.append("</table></div>");
        		
        		SearchLogger.info(sb.toString(), searchId);
            }
			
			return serverResponse;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return null;
	}
	
	private ServerResponse processResponse(TSServerInfoModule module, GetDocumentInformationResponse response) {
		ServerResponse serverResponse = new ServerResponse();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		
		try{
//			if(response == null) {
//	        	serverResponse = ServerResponse.createErrorResponse("Site returned error for this request.");
//	        	logInitialResponse(serverResponse);
//	        	return serverResponse;
//	    	}
			
			if(response == null || response.getOfficialRecordDocuments() == null){
				serverResponse = ServerResponse.createEmptyResponse();
	        	logInitialResponse(serverResponse);
	        	return serverResponse;
			}
			if (response.getOfficialRecordDocuments().getOfficialRecordDocument().length == 0){
				serverResponse = ServerResponse.createEmptyResponse();
	        	logInitialResponse(serverResponse);
	        	return serverResponse;
			}
			
			Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
	    	StringBuilder htmlContent = new StringBuilder();
	    	String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
	    	
	    	for (OfficialRecordDocument officialRecordDocument : response.getOfficialRecordDocuments().getOfficialRecordDocument()) {
				
		    	ParsedResponse currentResponse = new ParsedResponse();
				RegisterDocumentI registerDocumentI = GenericATIDSFunctions.getDocument(officialRecordDocument, getSearch(), getDataSite(), module);
				if(isParentSite()) {
					registerDocumentI.setSavedFrom(SavedFromType.PARENT_SITE);
				} else {
					registerDocumentI.setSavedFrom(SavedFromType.AUTOMATIC);
				}
				currentResponse.setDocument(registerDocumentI);
				
				String checkBox = "checked";
				if (isInstrumentSaved("gogo", registerDocumentI, null, false) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
	    			checkBox = "saved";
	    		} else {
	    			checkBox = "<input type='checkbox' name='docLink' value='" + linkPrefix + "FK____" + registerDocumentI.getId() + "'>";
	    			
	    			LinkInPage linkInPage = new LinkInPage(
	    					linkPrefix + "FK____" + registerDocumentI.getId(), 
	    					linkPrefix + "FK____" + registerDocumentI.getId(), 
	    					TSServer.REQUEST_SAVE_TO_TSD);
	    			
	    			if(getSearch().getInMemoryDoc(linkPrefix + "FK____" + registerDocumentI.getId())==null){
	    				getSearch().addInMemoryDoc(linkPrefix + "FK____" + registerDocumentI.getId(), currentResponse);
	    				
	    				/**
	        			 * Save module in key in additional info. The key is instrument number that should be always available. 
	        			 */
	        			String keyForSavingModules = getKeyForSavingInIntermediary(registerDocumentI.getInstno());
	        			getSearch().setAdditionalInfo(keyForSavingModules, module);
	    			}
	    			currentResponse.setPageLink(linkInPage);
	    		}
				
				String imageLink = null;
				
				if(registerDocumentI.hasImage()) {
					imageLink = "<br><a href=\"" + createPartialLink(TSConnectionURL.idPOST, TSServerInfo.IMG_MODULE_IDX) + 
							GenericATIDSFunctions.getShortImageLink(officialRecordDocument.getImageToken(), true) + 
							"\" title=\"View Image\" target=\"_blank\">View Image</a>";
				}
				
				String asHtml = registerDocumentI.asHtml();;
				currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + asHtml + (imageLink != null ? imageLink : "")  + "</td></tr>");
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + asHtml + "</td></tr>");
				currentResponse.setSearchId(searchId);
				currentResponse.setUseDocumentForSearchLogRow(true);
				responses.add(currentResponse);
	    	}
			
			parsedResponse.setResultRows(new Vector<ParsedResponse>(responses));
			parsedResponse.setOnlyResponse(htmlContent.toString());
			serverResponse.setResult(htmlContent.toString());
			
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
            	String header = parsedResponse.getHeader();
               	String footer = parsedResponse.getFooter();                           	
            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
            	header += "\n<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
            	        "<tr><th rowspan=1>" + SELECT_ALL_CHECKBOXES + "</th>" +
            			"<td align=\"center\">Document Content</td></tr>";

            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, (Integer)numberOfUnsavedDocument);
            	} else {
            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, -1);
            	}
            	
            	parsedResponse.setHeader(header);
            	parsedResponse.setFooter(footer);
            	
            	StringBuilder sb = new StringBuilder();
        		
        		sb.append(ro.cst.tsearch.utils.StringUtils.createCollapsibleHeader())
        			.append("<table border='1' cellspacing='0' width='99%'>")
        			.append("<tr><th>No</th>")
                	.append("<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th></tr>");
        		int index = 0;
        		for (ParsedResponse row : responses) {
        			sb.append("<tr class='row")
	        			.append(((index%2)+1))
	        			.append("' id='")
	        			.append(String.valueOf(System.nanoTime()))
	        			.append("_passed'><td>")
	        			.append(index + 1)
	        			.append(row.getTsrIndexRepresentation())
	        			.append("</tr>");
        	    	index ++;
				}
        		sb.append("</table></div>");
        		
        		SearchLogger.info(sb.toString(), searchId);
            }
			
			return serverResponse;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return null;
	}
	
	private ServerResponse processResponse(TSServerInfoModule module,
			GetPlatInformationByNameResponse response) {
		ServerResponse serverResponse = new ServerResponse();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
		try{
			
			String searchedParam = module.getParamValue(0).trim();
			
			if(response == null) {
	        	serverResponse = ServerResponse.createErrorResponse("Site returned error for this plat information request (" + searchedParam + ")");
	        	logInitialResponse(serverResponse);
	        	return serverResponse;
	    	}
			
			if(!response.getIsAvailable() 
					|| response.getPlats() == null 
					|| response.getPlats().getPlatInformation() == null 
					|| response.getPlats().getPlatInformation().length == 0){
				serverResponse = ServerResponse.createEmptyResponse();
	        	logInitialResponse(serverResponse);
	        	return serverResponse;
			}
			
			String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
			
			PlatInformation[] platInformations = response.getPlats().getPlatInformation();
			for (PlatInformation platInformation : platInformations) {
				
				ParsedResponse currentResponse = new ParsedResponse();
				
				DocumentParsedResponse documentParsedResponse = GenericATIDSFunctions.getDocument(platInformation, this, module);
				
				RegisterDocumentI registerDocumentI = documentParsedResponse.getDocument();
				
				
				String checkBox = "checked";
				if (isInstrumentSaved("gogo", registerDocumentI, null, false) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
	    			checkBox = "saved";
	    		} else {
	    			checkBox = "<input type='checkbox' name='docLink' value='" + linkPrefix + "FK____" + registerDocumentI.getId() + "'>";
	    			
	    			LinkInPage linkInPage = new LinkInPage(
	    					linkPrefix + "FK____" + registerDocumentI.getId(), 
	    					linkPrefix + "FK____" + registerDocumentI.getId(), 
	    					TSServer.REQUEST_SAVE_TO_TSD);
	    			
	    			if(getSearch().getInMemoryDoc(linkPrefix + "FK____" + registerDocumentI.getId())==null){
	    				getSearch().addInMemoryDoc(linkPrefix + "FK____" + registerDocumentI.getId(), currentResponse);
	    				
	    				/**
	        			 * Save module in key in additional info. The key is instrument number that should be always available. 
	        			 */
	        			String keyForSavingModules = getKeyForSavingInIntermediary(registerDocumentI.getInstno());
	        			getSearch().setAdditionalInfo(keyForSavingModules, module);
	    			}
	    			currentResponse.setPageLink(linkInPage);
	    		}
				
				String imageLink = null;
				
				if(StringUtils.isNotBlank(documentParsedResponse.getParentSiteImageLink())) {
					imageLink = "<br><a href=\"" + documentParsedResponse.getParentSiteImageLink() + 
							"\" title=\"View Image\" target=\"_blank\">View Image</a>";
				}
				
					
				/**
				 * Save module in key in additional info. The key is instrument number that should be always available.
				 */
				String keyForSavingModules = this
						.getKeyForSavingInIntermediary(getInstrumentNumberForSavingInFinalResults(registerDocumentI));
				getSearch().setAdditionalInfo(keyForSavingModules, module);
				
				String asHtml = registerDocumentI.asHtml(); 
				currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + asHtml + (imageLink!=null?imageLink:"")  + "</td></tr>");
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + asHtml + "</td></tr>");
				currentResponse.setSearchId(searchId);
				currentResponse.setUseDocumentForSearchLogRow(true);
				currentResponse.setDocument(registerDocumentI);
				responses.add(currentResponse);
				
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		logIntermediaryResponse(serverResponse, parsedResponse, responses, "");
		
		return serverResponse;
	}

	
	
	private ServerResponse processResponse(TSServerInfoModule module, SearchDetails result) {
		ServerResponse serverResponse = new ServerResponse();
		
		if(result == null) {
    		serverResponse = ServerResponse.createErrorResponse("No result for this request.");
        	logInitialResponse(serverResponse);
        	return serverResponse;
    	}
		
    	setCertificationDate(result);
    	
    	StringBuilder warningBuilder = new StringBuilder();
    	
    	ArrayOfSearchWarning searchWarnings = result.getSearchWarnings();
    	if(searchWarnings != null && searchWarnings.getSearchWarning() != null) {
    		List<SearchWarning> toShowWarnings = new ArrayList<SearchWarning>();
    		for (SearchWarning searchWarning : searchWarnings.getSearchWarning()) {
				if(!searchWarning.getSearchWarningType().equals(SearchWarningType.SearchThruGreaterThanCert)) {
					toShowWarnings.add(searchWarning);
				}
				
				if(SearchWarningType.SearchEarlierThanPlat.equals(searchWarning.getSearchWarningType())
						|| SearchWarningType.PlattedLegalReplat.equals(searchWarning.getSearchWarningType())
						|| SearchWarningType.UnplattedLegalReplat.equals(searchWarning.getSearchWarningType())
						|| SearchWarningType.RetroCertified.equals(searchWarning.getSearchWarningType())
						|| SearchWarningType.NewerEntries.equals(searchWarning.getSearchWarningType())
						|| SearchWarningType.MoreInstrumentsAvailable.equals(searchWarning.getSearchWarningType())
						) {
					WarningCustomInfo warningCustomInfo = new WarningCustomInfo(searchWarning.getDescription());
					getSearch().getSearchFlags().addWarning(warningCustomInfo);
				}
				
			}
    		if(toShowWarnings.size() == 1) {
    			if(result.getSearchMatches() != null && result.getSearchMatches().getSearchMatch() != null) {
    				SearchLogger.info("<br><font color=\"red\">WARNING returned from site<br>" + toShowWarnings.get(0).getDescription() + "</font><br><br>", searchId);
    			}
    			warningBuilder.append("<br>").append(toShowWarnings.get(0).getDescription());
    		} else if(toShowWarnings.size() > 1) {
    			StringBuilder sb = new StringBuilder("<br><font color=\"red\">WARNINGs returned from site");
    			for (SearchWarning searchWarning : toShowWarnings) {
					sb.append("<br>").append(searchWarning.getDescription());
					warningBuilder.append("<br>").append(toShowWarnings.get(0).getDescription());
				}
    			sb.append("</font><br><br>");
    			if(result.getSearchMatches() != null && result.getSearchMatches().getSearchMatch() != null) {
    				SearchLogger.info(sb.toString(), searchId);
    			}
    		}
    		
    	}
    	
    	if(result.getSearchMatches() == null) {
    		String errorMessage = "Datasource error!";
    		if(warningBuilder.length() > 0) {
    			errorMessage += warningBuilder.toString();
    		}
    		serverResponse = ServerResponse.createWarningResponse(errorMessage);
        	logInitialResponse(serverResponse);
        	return serverResponse;
    	}
    	
    	ArrayOfSearchMatch searchMatches = result.getSearchMatches();
    	if(searchMatches.getSearchMatch() == null) {
    		
    		if(warningBuilder.length() > 0) {
    			serverResponse = ServerResponse.createWarningResponse(NO_DATA_FOUND + warningBuilder.toString());
    		} else {
    			serverResponse = ServerResponse.createEmptyResponse();
    		}
    		logInitialResponse(serverResponse);
        	return serverResponse;
    	}
    	
    	int moduleIdx = module.getModuleIdx();
    	if (moduleIdx==TSServerInfo.SUBDIVISION_MODULE_IDX || moduleIdx==TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX ||
    			moduleIdx==TSServerInfo.SECTION_LAND_MODULE_IDX || moduleIdx==TSServerInfo.SURVEYS_MODULE_IDX) {
    		if (searchMatches.getSearchMatch().length>0) {
    			Search search = getSearch();
    			@SuppressWarnings("unchecked")
				Set<Integer> additionalInfo = (Set<Integer>)search.getAdditionalInfo(AdditionalInfoKeys.MISSING_LAND_SEARCH_ID_SET);
    			if(additionalInfo == null) {
    				additionalInfo = new HashSet<Integer>();
    			}
    			additionalInfo.add(moduleIdx);
    			search.setAdditionalInfo(AdditionalInfoKeys.MISSING_LAND_SEARCH_ID_SET, additionalInfo);
    		} 
    	}
    	
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
    	
    	Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
    	StringBuilder htmlContent = new StringBuilder();
    	String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
    	
    	Map<String, ParsedResponse> allPreviosResponses = new HashMap<String, ParsedResponse>();
    	
    	for (SearchMatch searchMatch : searchMatches.getSearchMatch()) {
    		ParsedResponse currentResponse = new ParsedResponse();
			//OfficialRecordDocument officialRecordDocument = searchMatch.getOfficialRecordDocument();
			RegisterDocumentI registerDocumentI = GenericATIDSFunctions.getDocument(searchMatch, getSearch(), getDataSite(), module);
			if(isParentSite()) {
				registerDocumentI.setSavedFrom(SavedFromType.PARENT_SITE);
			} else {
				registerDocumentI.setSavedFrom(SavedFromType.AUTOMATIC);
			}
			//currentResponse.setDocument(registerDocumentI);
			
			String key = registerDocumentI.shortPrint();
			
			boolean update = false;
			ParsedResponse alreadyProcessedResponse = null;
			RegisterDocumentI alreadyProcessedDocument = null;
			
			if(allPreviosResponses.containsKey(key)) {
				alreadyProcessedResponse = allPreviosResponses.get(key);
				alreadyProcessedDocument = (RegisterDocumentI) alreadyProcessedResponse.getDocument();
				
				for(NameI nameI : registerDocumentI.getGrantee().getNames()){
					if(!alreadyProcessedDocument.getGrantee().contains(nameI)) {
						alreadyProcessedDocument.getGrantee().add(nameI);
						update = true;
					}
				}
				for(NameI nameI : registerDocumentI.getGrantor().getNames()) {
					if(!alreadyProcessedDocument.getGrantor().contains(nameI)) {
						alreadyProcessedDocument.getGrantor().add(nameI);
						update = true;
					}
				}
								
			} else {
				allPreviosResponses.put(key, currentResponse);
				currentResponse.setDocument(registerDocumentI);
			}
			
			if(!update && alreadyProcessedDocument != null) {
				continue;
			}
			
			String checkBox = "checked";
			if (isInstrumentSaved("gogo", registerDocumentI, null, false) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
    			checkBox = "saved";
    		} else {
    			checkBox = "<input type='checkbox' name='docLink' value='" + linkPrefix + "FK____" + registerDocumentI.getId() + "'>";
    			
    			LinkInPage linkInPage = new LinkInPage(
    					linkPrefix + "FK____" + registerDocumentI.getId(), 
    					linkPrefix + "FK____" + registerDocumentI.getId(), 
    					TSServer.REQUEST_SAVE_TO_TSD);
    			
    			if(!update) {
    			
	    			if(getSearch().getInMemoryDoc(linkPrefix + "FK____" + registerDocumentI.getId())==null){
	    				getSearch().addInMemoryDoc(linkPrefix + "FK____" + registerDocumentI.getId(), currentResponse);
	    				
	    				/**
	        			 * Save module in key in additional info. The key is instrument number that should be always available. 
	        			 */
	        			String keyForSavingModules = getKeyForSavingInIntermediary(registerDocumentI.getInstno());
	        			getSearch().setAdditionalInfo(keyForSavingModules, module);
	    			}
	    			currentResponse.setPageLink(linkInPage);
	    			
    			}
    			
    		}
			
			String imageLink = null;
			
			if(registerDocumentI.hasImage()) {
				imageLink = "<br><a href=\"" + createPartialLink(TSConnectionURL.idPOST, TSServerInfo.IMG_MODULE_IDX) + 
						GenericATIDSFunctions.getShortImageLink(searchMatch.getOfficialRecordDocument().getImageToken(), true) + 
						"\" title=\"View Image\" target=\"_blank\">View Image</a>";
			}
			
			
			
			
			if(update) {
				String asHtml = alreadyProcessedDocument.asHtml(); 
				
				int indexOf = alreadyProcessedResponse.getResponse().indexOf("</td><td>");
				
				alreadyProcessedResponse.setOnlyResponse(
						alreadyProcessedResponse.getResponse().substring(0, indexOf) + "</td><td>" + asHtml + (imageLink!=null?imageLink:"")  + "</td></tr>");
				alreadyProcessedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + asHtml + "</td></tr>");
			} else {
				
				/**
				 * Save module in key in additional info. The key is instrument number that should be always available.
				 */
				String keyForSavingModules = this
						.getKeyForSavingInIntermediary(getInstrumentNumberForSavingInFinalResults(registerDocumentI));
				getSearch().setAdditionalInfo(keyForSavingModules, module);
				
				String asHtml = registerDocumentI.asHtml(); 
				currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + asHtml + (imageLink!=null?imageLink:"")  + "</td></tr>");
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + asHtml + "</td></tr>");
				currentResponse.setSearchId(searchId);
				currentResponse.setUseDocumentForSearchLogRow(true);
				responses.add(currentResponse);
			}
		}
    	
    	logIntermediaryResponse(serverResponse, parsedResponse, responses, htmlContent.toString());
		return serverResponse;
	}

	protected void logIntermediaryResponse(ServerResponse serverResponse, ParsedResponse parsedResponse,
			Collection<ParsedResponse> responses, String htmlContent) {
		long noResults = responses.size();
    	SearchLogger.info("Found <span class='number'>" + noResults + "</span> <span class='rtype'>intermediate</span> results.<br/>",searchId);
    	
    	if(noResults > 0) {
    		
    		//sort responses
        	List<ParsedResponse> responsesList = new Vector<ParsedResponse>();
        	responsesList.addAll(responses);
        	 
        	Collections.sort(responsesList, new ParsedResponseDateComparator());
        	
        	responses.clear();
        	responses.addAll(responsesList);
    		
			parsedResponse.setResultRows(new Vector<ParsedResponse>(responses));
			parsedResponse.setOnlyResponse(htmlContent.toString());
			serverResponse.setResult(htmlContent.toString());
			
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
            	String header = parsedResponse.getHeader();
               	String footer = parsedResponse.getFooter();                           	
            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
            	header += "\n<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
            	        "<tr><th rowspan=1>"+ SELECT_ALL_CHECKBOXES + "</th>" +
            			"<td align=\"center\">Document Content</td></tr>";

            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
            				ID_SEARCH_BY_NAME, (Integer)numberOfUnsavedDocument);
            	} else {
            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, -1);
            	}

            	
            	parsedResponse.setHeader(header);
            	parsedResponse.setFooter(footer);
            	
            	StringBuilder sb = new StringBuilder();
        		
        		sb.append(ro.cst.tsearch.utils.StringUtils.createCollapsibleHeader())
        			.append("<table border='1' cellspacing='0' width='99%'>")
        			.append("<tr><th>No</th>")
                	.append("<th>DS</th><th align='left'>Desc</th><th>Date</th><th>Grantor</th><th>Grantee</th><th>Instr Type</th><th>Instr</th><th>Remarks</th></tr>");
        		int index = 0;
        		for (ParsedResponse row : responses) {
        			sb.append("<tr class='row")
	        			.append(((index%2)+1))
	        			.append("' id='")
	        			.append(String.valueOf(System.nanoTime()))
	        			.append("_passed'><td>")
	        			.append(index + 1)
	        			.append(row.getTsrIndexRepresentation())
	        			.append("</tr>");
        	    	index ++;
				}
        		sb.append("</table></div>");
        		
        		SearchLogger.info(sb.toString(), searchId);
            }
			
			
    	}
	}

	private class ParsedResponseDateComparator implements Comparator<ParsedResponse> {

		@Override
		public int compare(ParsedResponse o1, ParsedResponse o2) {
			try {
				Date d1 = o1.getDocument().getDate();
				Date d2 = o2.getDocument().getDate();

				if (d1.after(d2)) {
					return -1;
				} else if (d1.before(d2)) {
					return 1;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

	}

	private void setCertificationDate(SearchDetails result) {
		if(result.getCertificationRange()!= null && result.getCertificationRange().getThroughDate() != null){
			getSearch().getSa().updateCertificationDateObject(dataSite, result.getCertificationRange().getThroughDate().getTime());
		}
	}
	
	@Override
	protected void setCertificationDate() {
		try {
			
			if (!CertificationDateManager.isCertificationDateInCache(dataSite)){
				
				if(certDate == null) {
					certDate = getConnection().getCertDate();
				}

				if(certDate != null) {
				
					ArrayOfCountyCertificationDetail countyCertificationDetails = certDate.getCountyCertificationDetails();
					if(countyCertificationDetails != null && countyCertificationDetails.getCountyCertificationDetail() != null) {
						CountyCertificationDetail[] countyCertificationDetailArray = countyCertificationDetails.getCountyCertificationDetail();
						
						Date maxDate = null;
						
						for (CountyCertificationDetail countyCertificationDetail : countyCertificationDetailArray) {
							if(countyCertificationDetail.getCounty().equals(AtidsUtils.getCountyName(getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_NAME)))) {
								CertificationRange geographicIndexCertificationRange = countyCertificationDetail.getGeographicIndexCertificationRange();
								CertificationRange grantorGranteeIndexCertificationRange = countyCertificationDetail.getGrantorGranteeIndexCertificationRange();
								CertificationRange nameIndexCertificationRange = countyCertificationDetail.getNameIndexCertificationRange();
								
								if(geographicIndexCertificationRange != null && geographicIndexCertificationRange.getThroughDate() != null) {
									maxDate = geographicIndexCertificationRange.getThroughDate().getTime();
								}
								if(maxDate == null && grantorGranteeIndexCertificationRange != null && grantorGranteeIndexCertificationRange.getThroughDate() != null) {
									Date tempDate = grantorGranteeIndexCertificationRange.getThroughDate().getTime();
									if(maxDate == null) {
										maxDate = tempDate;
//									} else {
//										if(tempDate != null) {
//											maxDate = maxDate.before(tempDate)?tempDate:maxDate;
//										}
									}
								}
								if(maxDate == null && nameIndexCertificationRange != null && nameIndexCertificationRange.getThroughDate() != null) {
									Date tempDate = nameIndexCertificationRange.getThroughDate().getTime();
									if(maxDate == null) {
										maxDate = tempDate;
//									} else {
//										if(tempDate != null) {
//											maxDate = maxDate.before(tempDate)?tempDate:maxDate;
//										}
									}
								}
								break;
							}
							
						}
						
						if(maxDate != null) {
							CertificationDateManager.cacheCertificationDate(dataSite, new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(maxDate));
							getSearch().getSa().updateCertificationDateObject(dataSite, maxDate);
						} else {
							CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName() + " because maxDate cannot be determined");		
						}
						
					}
				} else {
					CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName() + " because certDate is null from ATIDS");
				}
				
			} else {
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			}

        } catch (Exception e) {
        	CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
        }
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
	
	@Override
    protected DownloadImageResult saveImage(ImageI image)
    		throws ServerResponseException {
    	
    	if(image != null) {
    		String allParams = image.getLink(0);
    		
    		if(allParams.contains("&imageToken=")) {
    		
    			String imageToken = allParams.replaceAll(".*&imageToken=([^&]+).*", "$1");
	    		if(StringUtils.isNotEmpty(imageToken)) {
		    		for (int tryCount = 0; tryCount < 3; tryCount++) {
		    			try {
		        			
		        			byte[] imageBytes = getConnection().getImage(imageToken);
		        			
		        			if(imageBytes != null) { 
		        			
		    					DownloadImageResult downloadImageResult = new DownloadImageResult(
		    							DownloadImageResult.Status.OK, imageBytes, image.getContentType());
		    					
		    					afterDownloadImage(true);
		    					
		    					logger.debug(
		        						searchId + " success for image download for token " + 
		        						imageToken + " tryCount: " + 
		        						tryCount + " fullLink " + allParams);
		    					
		    					return downloadImageResult;
		        			}
		    				
		    			} catch (Exception e) {
		    				logger.error(
		    						searchId + " error for image download for token " + 
		    						imageToken + " tryCount: " + 
		    						tryCount + " fullLink " + allParams, 
		    						e);
		    			} 
					}
		    		logger.error(
		    				searchId + " error for image download for token " + 
		    				imageToken + " finalCount " + " fullLink " + allParams);
	    		} else {
	    			logger.error(
		    				searchId + " error for image download for token " + 
		    				imageToken + " fullLink " + allParams);
	    		}
    		
    		} else {
    			ServerResponse serverResponse = null;
    			
    			Integer moduleId = null;
    			LinkParser linkParser = new LinkParser("?" + allParams);
    			TSServerInfoModule module = null;
    			SearchDataWrapper searchDataWrapper = new SearchDataWrapper();
    			if(allParams.contains("&Book=")) {
    				moduleId = TSServerInfo.MODULE_IDX39;
					module = getCurrentClassServerInfo().getModuleForSearch(
							moduleId, 
							searchDataWrapper);
					module.setData(0, linkParser.getParamValue("Book"));
					module.setData(1, linkParser.getParamValue("BookSuffix"));
					module.setData(2, linkParser.getParamValue("Page"));
					module.setData(3, linkParser.getParamValue("PageSuffix"));
					module.setData(4, linkParser.getParamValue("Source"));
					module.setData(5, linkParser.getParamValue("fakeName"));
    			} else if(allParams.contains("&NumberSuffix=")) {
    				moduleId = TSServerInfo.MODULE_IDX38;
    				
					module = getCurrentClassServerInfo().getModuleForSearch(
							moduleId, 
							searchDataWrapper);
					module.setData(0, linkParser.getParamValue("Number"));
					module.setData(1, linkParser.getParamValue("Source"));
					module.setData(2, linkParser.getParamValue("Year"));
					module.setData(3, linkParser.getParamValue("NumberSuffix"));
					module.setData(4, linkParser.getParamValue("SeriesCode"));
					module.setData(5, linkParser.getParamValue("fakeName"));
    			}
    			
    			if(module != null) {
    			
	    			for (int i = 0; i < 3; i++) {
	    				serverResponse = new ServerResponse();
	    				try{
	    					
	    					module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
	    					searchDataWrapper.setImage(image);
	    					
	    					serverResponse = searchBy(false, module, searchDataWrapper);
	    					
	    					if(serverResponse.getImageResult() == null ||  !DownloadImageResult.Status.OK.equals(serverResponse.getImageResult().getStatus())) {
	    						if(serverResponse.getImageResult() != null) {
	    							
	    							File testFile = new File(image.getPath());
	    							if(testFile.exists() && testFile.length() > 0) {
	    								return new DownloadImageResult( DownloadImageResult.Status.OK, FileUtils.readFileToByteArray(testFile), image.getContentType() );
	    							}
	    						}
	
	    						serverResponse.setImageResult(new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() ));
	    						
	    					} else {
	    						return serverResponse.getImageResult();
	    					}
	    				} catch (Exception e) {
	    					e.printStackTrace();
	    					
	    					serverResponse.setImageResult(new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() ));
	    				}
	    			}
    			} else {
    				logger.error(
		    				searchId + " error for image download for fullLink " + allParams + " - no module id found");
    			}
    		}
    		
    		return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
    	}
    	
    	
    	
    	return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], "");
    }

	private AtidsConnWrapper getConnection() {
		if(connection != null) {
			return connection;
		}
		try {
			connection = new AtidsConnWrapper(getDataSite(), getSearch());
		} catch (Exception e) {
			logger.error("Error while creating connection to web service");
		}
		return connection;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		Search search = getSearch();
		
		TSServerInfoModule m = null;
		
		FilterResponse rejectAlreadySavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		
		GenericMultipleLegalFilter defaultLegalFilter = new GenericMultipleLegalFilter(searchId);
		defaultLegalFilter.setUseLegalFromSearchPage(true);
		defaultLegalFilter.setEnableLotUnitFullEquivalence(true);
		defaultLegalFilter.setMarkIfCandidatesAreEmpty(true);
		defaultLegalFilter.setThreshold(new BigDecimal(0.7));
		defaultLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.ATIDS_LOOK_UP_DATA);
		
		GenericLegal defaultSingleLegalFilter = (GenericLegal)LegalFilterFactory.getDefaultLegalFilter(searchId);
		defaultSingleLegalFilter.setEnableLotUnitFullEquivalence(true);
		LastTransferDateFilter lastTransferDateFilter = new LastTransferDateFilter(searchId);
    	    	
		GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		defaultNameFilter.setIgnoreMiddleOnEmpty(true);
		defaultNameFilter.setUseArrangements(false);
		defaultNameFilter.setInitAgain(true);
		
		
//		GenericMultipleAddressFilter addressFilter 	= new GenericMultipleAddressFilter(searchId);
//		for (AddressI address : getSearchAttributes().getForUpdateSearchAddressesNotNull(getServerID())) {
//			if(StringUtils.isNotBlank(address.getStreetName())) {
//				addressFilter.addNewFilterFromAddress(address);
//			}
//		}
		
//		String[] sourcesForAoTaxLike = new String[]{"OR"};
		String[] sourcesForRoLike = new String[]{"OR","CN"};
		
		InstrumentGenericIterator instrumentGenericIterator = null; 
		InstrumentGenericIterator bpGenericIterator = null;
		
		boolean lookupWasDoneWithInstrument = false;
		
//		for (String sourceForRoLike : sourcesForRoLike) {
			{
				instrumentGenericIterator = getInstrumentIterator(true);
				instrumentGenericIterator.setInstrumentTypes(new String[]{"CN"});
				
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			"Search with references from AO/Tax like documents");
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
				m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				if (isUpdate()) {
					m.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				m.addIterator(instrumentGenericIterator);
				
				if(!lookupWasDoneWithInstrument) {
					lookupWasDoneWithInstrument = !instrumentGenericIterator.createDerrivations().isEmpty();
				}
				
				modules.add(m);
			}
			
			{ 
				bpGenericIterator = getInstrumentIterator(false);
				bpGenericIterator.setInstrumentTypes(new String[]{"OR"});
				bpGenericIterator.setRemoveLeadingZerosBP(true);
				
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			"Search with references from AO/Tax like documents");
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
				if (isUpdate()) {
					m.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				m.addIterator(bpGenericIterator);
				
				if(!lookupWasDoneWithInstrument) {
					lookupWasDoneWithInstrument = !bpGenericIterator.createDerrivations().isEmpty();
				}
				
				modules.add(m);
			}
//		}
		
		
		if(!lookupWasDoneWithInstrument) {
			addNameSearch(
					modules, 
					serverInfo, 
					SearchAttributes.OWNER_OBJECT, 
					"Name PI module - searching with owner(s) name", 
					null, 
					new FilterResponse[]{ defaultNameFilter, defaultSingleLegalFilter/*, addressFilter*/ }, 
					null, 
					new DocsValidator[]{
							defaultSingleLegalFilter.getValidator(), 
							lastTransferDateFilter.getValidator(),
							rejectAlreadySavedDocuments.getValidator()}, 
					false);
//			addNameSearch(
//					modules, 
//					serverInfo, 
//					SearchAttributes.OWNER_OBJECT, 
//					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, 
//					null, 
//					new FilterResponse[]{ defaultNameFilter, defaultSingleLegalFilter/*, addressFilter*/ }, 
//					null, 
//					new DocsValidator[]{
//							defaultSingleLegalFilter.getValidator(), 
//							lastTransferDateFilter.getValidator(),
//							rejectAlreadySavedDocuments.getValidator()},
//					null);
		}
		
		{
			
			LegalDescriptionIterator it = getLegalDescriptionIterator(!lookupWasDoneWithInstrument);
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX));
			m.clearSaKeys();

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_68);
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_69);
			m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
			m.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_LOT);
			m.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_BLOCK);
			m.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_SECTION);
			m.setSaKey(8, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(9, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			
//			m.addFilter(defaultLegalFilter);
//			m.addFilter(addressFilter);
			m.addIterator(it);
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
						new FilterResponse[]{defaultNameFilter, defaultLegalFilter, lastTransferDateFilter, rejectAlreadySavedDocuments/*, addressFilter*/}, 
						null, 
						new DocsValidator[]{
								defaultLegalFilter.getValidator(),
								lastTransferDateFilter.getValidator(),
								rejectAlreadySavedDocuments.getValidator()},
						null);
			}
		}

		/**
		 *  P6 OCR last transfer - book page search
		 */
		{
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
			m.clearSaKeys();
			OcrFakeIterator ocrFakeIterator = new OcrFakeIterator(searchId);
			ocrFakeIterator.setInitAgain(true);
			m.addIterator(ocrFakeIterator);
//			m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
//			m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
//			m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
//			m.forceValue(4, "PB");
//			m.addFilter(defaultLegalFilter);
			modules.add(m);
			
//			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38));
//			m.clearSaKeys();
//			m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
//			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
//			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
//			m.forceValue(1, "CN");
//			m.addFilter(defaultLegalFilter);
//			modules.add(m);
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
					new FilterResponse[]{defaultNameFilter, defaultLegalFilter, lastTransferDateFilter, rejectAlreadySavedDocuments/*, addressFilter*/}, 
					null, 
					new DocsValidator[]{
							defaultLegalFilter.getValidator(),
							lastTransferDateFilter.getValidator(),
							rejectAlreadySavedDocuments.getValidator()},
					null);
	    	
		}

		/**
		 * Buyer Search
		 */
//		if(isUpdate && hasBuyer()) {
//			
//			FilterResponse nameFilterBuyer 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, getSearch().getID(), null );
//			((GenericNameFilter) nameFilterBuyer).setIgnoreMiddleOnEmpty(true);
//			((GenericNameFilter) nameFilterBuyer).setUseArrangements(false);
//			((GenericNameFilter) nameFilterBuyer).setInitAgain(true);
//			
//			addNameSearch(
//					modules, 
//					serverInfo, 
//					SearchAttributes.BUYER_OBJECT, 
//					TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS,
//					null, 
//					new FilterResponse[]{nameFilterBuyer, DoctypeFilterFactory.getDoctypeBuyerFilter( searchId ), lastTransferDateFilter, rejectAlreadySavedDocuments}, 
//					null, 
//					new DocsValidator[]{
//							defaultLegalFilter.getValidator(),
//							lastTransferDateFilter.getValidator(),
//							rejectAlreadySavedDocuments.getValidator()});
//		}
		
		/**
		 * Try to find the PLAT by performing same search like on legal module but this time on document info module
		 */
		{
			
			LegalDescriptionIterator it = getLegalDescriptionIterator(!lookupWasDoneWithInstrument);
			it.setCheckIfDocumentExists(true);
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX44));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			TSServerInfoConstants.VALUE_PARAM_PLATBOOK_PLATPAGE);
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_68);
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_69);
			m.forceValue(4, "*");

			if (isUpdate()) {
				m.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			m.addFilter(DoctypeFilterFactory.getDoctypeFilter(searchId, 0.8, 
					new String[] {DocumentTypes.PLAT}, 
					FilterResponse.STRATEGY_TYPE_HIGH_PASS));
			m.addIterator(it);
			modules.add(m);
		
			/**
			 * Try to find the PLAT by performing a search with Plat Information by Name 
			 */
			if(StringUtils.isNotBlank(search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME))) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX40));
				if (isUpdate()) {
					m.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				m.addFilter(getInstrumentFilter(it));
				m.addFilter(DoctypeFilterFactory.getDoctypeFilter(searchId, 0.8, 
						new String[] {DocumentTypes.PLAT}, 
						FilterResponse.STRATEGY_TYPE_HIGH_PASS));
				modules.add(m);
			}
		}
		
			/**
			 * Search with instrument/bp from AO,TAX-like that are not already saved
			 */
			{
				instrumentGenericIterator = getInstrumentIterator(true);
//				instrumentGenericIterator.setInstrumentTypes(new String[]{sourceForRoLike});
				
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX43));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			"Search with unsaved references from AO/Tax like documents");
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				m.forceValue(1, "*");
				m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				if (isUpdate()) {
					m.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				m.addFilter(defaultLegalFilter);
				m.addIterator(instrumentGenericIterator);
				modules.add(m);
			}
			{ 
				bpGenericIterator = getInstrumentIterator(false);
//				bpGenericIterator.setInstrumentTypes(new String[]{sourceForRoLike});
				
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX44));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			"Search with unsaved references from AO/Tax like documents");
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				m.forceValue(4, "*");
				if (isUpdate()) {
					m.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				m.addFilter(defaultLegalFilter);
				m.addIterator(bpGenericIterator);
				modules.add(m);
			}
//		/**
//		 * Search with references from RO-like that are not already saved
//		 */
//		{
//			instrumentGenericIterator = getInstrumentIterator(true);
//			//instrumentGenericIterator.setInstrumentTypes(new String[]{sourceForRoLike});
////			instrumentGenericIterator.setForceInstrumentTypes(sourcesForRoLike);
//			instrumentGenericIterator.setLoadFromRoLike(true);
//			instrumentGenericIterator.setDsToLoad(new String[]{dataSite.getSiteTypeAbrev()});
//			instrumentGenericIterator.setUseInstrumentType(true);
//			
//			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX43));
//			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
//	    			"Search with unsaved references from RO-like documents");
//			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
//			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
//			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
//			if (isUpdate) {
//				m.addFilter(new BetweenDatesFilterResponse(searchId));
//			}
//			m.addFilter(defaultLegalFilter);
//			m.addIterator(instrumentGenericIterator);
//			modules.add(m);
//		}
//		{
//			bpGenericIterator = getInstrumentIterator(false);
//			//bpGenericIterator.setInstrumentTypes(new String[]{sourceForRoLike});
////			bpGenericIterator.setForceInstrumentTypes(sourcesForRoLike);
//			bpGenericIterator.setLoadFromRoLike(true);
//			bpGenericIterator.setDsToLoad(new String[]{dataSite.getSiteTypeAbrev()});
//			bpGenericIterator.setUseInstrumentType(true);
//			
//			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX44));
//			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
//	    			"Search with unsaved references from RO-like documents");
//			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
//			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
//			m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
//			if (isUpdate) {
//				m.addFilter(new BetweenDatesFilterResponse(searchId));
//			}
//			m.addFilter(defaultLegalFilter);
//			m.addIterator(bpGenericIterator);
//			modules.add(m);
//		}

//		for (String sourceForRoLike : sourcesForRoLike) {
			/**
			 * Search with references from RO-like that are not already saved (force "*" if type not on reference)
			 */
			{
				instrumentGenericIterator = getInstrumentIterator(true);
				instrumentGenericIterator.setForceInstrumentTypes(new String[]{"*"});
				instrumentGenericIterator.setLoadFromRoLike(true);
				instrumentGenericIterator.setDsToLoad(new String[]{dataSite.getSiteTypeAbrev()});
				instrumentGenericIterator.setUseInstrumentType(true);
				
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX43));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			"Search with unsaved references from RO-like documents");
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
				m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
				if (isUpdate()) {
					m.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				m.addFilter(defaultLegalFilter);
				m.addIterator(instrumentGenericIterator);
				modules.add(m);
			}
			{
				bpGenericIterator = getInstrumentIterator(false);
				bpGenericIterator.setForceInstrumentTypes(new String[]{"*"});
				bpGenericIterator.setLoadFromRoLike(true);
				bpGenericIterator.setDsToLoad(new String[]{dataSite.getSiteTypeAbrev()});
				bpGenericIterator.setUseInstrumentType(true);
				
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX44));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			"Search with unsaved references from RO-like documents");
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
				if (isUpdate()) {
					m.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				m.addFilter(defaultLegalFilter);
				m.addIterator(bpGenericIterator);
				modules.add(m);
			}
//		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}

	private FilterResponse getInstrumentFilter(final LegalDescriptionIterator it) {
		
		GenericInstrumentFilter genericInstrumentFilter = new GenericInstrumentFilter(searchId) {

			private static final long	serialVersionUID	= 1L;
			
			@Override
			public void init() {
				clearFilters();
				List<LegalStruct> list = it.getList();
				if(list != null && !list.isEmpty()) {
					for (LegalStruct str : list) {
						if(StringUtils.isNotBlank(str.getPlatBook()) && StringUtils.isNotBlank(str.getPlatPage())) {
							HashMap<String, String> filter = new HashMap<String, String>();
							filter.put(GENERIC_INSTRUMENT_FILTER_KEY_BOOK, str.getPlatBook());
							filter.put(GENERIC_INSTRUMENT_FILTER_KEY_PAGE, str.getPlatPage());
							addDocumentCriteria(filter);
						}
					}
				}
			}
			
			@Override
			public BigDecimal getScoreOneRow(ParsedResponse row) {
				if(getNoOfFilters() > 0) {
					return super.getScoreOneRow(row);
				} 
				return ATSDecimalNumberFormat.ZERO; 
			}
			
			@Override
			public String getFilterCriteria() {
				if(getNoOfFilters() == 0) {
					return "InstrumentFilter. There is no searched legal to validate against.";
				}
				return "InstrumentFilter. Allowing documents with the same instrument/book-page as the searched legal";
			}
			
		};
		genericInstrumentFilter.setInitAgain(true);
		return genericInstrumentFilter;
	}

	protected InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId) {

			private static final long serialVersionUID = 5399351945130601258L;
			
			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				
//				if(year < 1900) { 
//					return "";
//				}
				
				String yearAsString = Integer.toString(year);
				
				
				String cleanedInstNo = null;
				
				if(instno.length() <= 7) {
					if(CountyConstants.FL_DeSoto_STRING.equals(this.getSearch().getCountyId())) {
						if(year > 1000 && year < 2000) {
							if(instno.startsWith(yearAsString.substring(2))){
								cleanedInstNo = instno.substring(2).replaceFirst("^0+", "");
							} else if (instno.length() == 6) {
								cleanedInstNo = instno.substring(2).replaceFirst("^0+", "");
							}
						}
					}
				} else if(instno.length() >= 11){
					//removed all garbage and keep max instrument allowed
					cleanedInstNo = instno.substring(instno.length() - 7).replaceFirst("^0+", "");
				} else {
					
					if(year > 1000) {
						//remove just year
						if(instno.startsWith(yearAsString)) {
							cleanedInstNo = instno.substring(4).replaceFirst("^0+", "");
						} else if(instno.startsWith(yearAsString.substring(2))){
							cleanedInstNo = instno.substring(2).replaceFirst("^0+", "");
						}
					}
				}
				
				if(cleanedInstNo == null) {
					cleanedInstNo = instno.replaceFirst("^0+", "");
				}
				return org.apache.commons.lang.StringUtils.right(cleanedInstNo, 7).replaceFirst("^0+", "");
			}
			
			@Override
			protected void processEnableInstrumentNo(List<InstrumentI> result, HashSet<String> listsForNow,
					DocumentsManagerI manager, InstrumentI instrumentI) {
				if(!instrumentI.hasBookPage()) {
					super.processEnableInstrumentNo(result, listsForNow, manager, instrumentI);
				}
			}
			
			@Override
			protected List<DocumentI> getDocumentsWithInstrumentsFlexible(DocumentsManagerI manager,
					InstrumentI instrumentI) {
				//return super.getDocumentsWithInstrumentsFlexible(manager, instrumentI);
				return checkFlexibleInclusion(instrumentI, manager, false);
			}
			
		};
		if(instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
			instrumentGenericIterator.setRemoveLeadingZerosBP(true);
		}
		return instrumentGenericIterator;
	}
	
	private LegalDescriptionIterator getLegalDescriptionIterator(boolean lookupWasDoneWithName) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookupWasDoneWithName, false, getDataSite()) {

			private static final long serialVersionUID = -4741635379234782109L;
			
			private Map<String, Integer> initialSiteDocuments = new HashMap<String, Integer>();
			private Set<String> firstTime = new HashSet<String>(); 
			private Map<LegalStruct, Integer> initialDocumentsBeforeRunningStruct = new HashMap<LegalStruct, Integer>();
			private Set<LegalStruct> processedForNextStruct = new HashSet<LegalStruct>();
			
			
			@SuppressWarnings("unchecked")
			protected List<DocumentI> loadLegalFromRoDocs(Search global, DocumentsManagerI m) {
				List<DocumentI> listRodocs = new ArrayList<DocumentI>();
				
				if(AdditionalInfoKeys.AK_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments()) ||
						AdditionalInfoKeys.AR_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments()) ||
						AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments())) {
					List<DocumentI> listRodocsSaved = (List<DocumentI>) global.getAdditionalInfo(getCheckAlreadyFilledKeyWithDocuments());
					if(listRodocsSaved != null && !listRodocsSaved.isEmpty()) {
						listRodocs.addAll(listRodocsSaved);
					}
				}
				
				if(listRodocs.isEmpty()) {
					if(getRoDoctypesToLoad() == null) {
						listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(this, m,global,true));
						if(listRodocs.isEmpty()){
							listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(this, m, global, false));
						}
						if(listRodocs.isEmpty()){
							listRodocs.addAll(m.getRoLikeDocumentList(true));
						}
					} else {
						listRodocs.addAll(m.getDocumentsWithDocType(true, getRoDoctypesToLoad()));
					}
					if(AdditionalInfoKeys.AK_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments()) ||
							AdditionalInfoKeys.AR_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments()) || 
							AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments())) {
						global.setAdditionalInfo(getCheckAlreadyFilledKeyWithDocuments(), listRodocs);	
					}
					
				}
				
				DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
				
				boolean nameWasPI = false;
				
				for( DocumentI reg: listRodocs){
					if(!reg.isOneOf(
							DocumentTypes.PLAT,
							DocumentTypes.RESTRICTION,
							DocumentTypes.EASEMENT,
							DocumentTypes.MASTERDEED,
							DocumentTypes.COURT,
							DocumentTypes.LIEN,
							DocumentTypes.CORPORATION,
							DocumentTypes.AFFIDAVIT,
							DocumentTypes.CCER,
							DocumentTypes.TRANSFER)
							
							|| isTransferAllowed(reg)
							) {
						if(SearchType.PI.equals(reg.getSearchType())){
							nameWasPI = true;
							break;
						}
					}
				}
				
				if(isLookUpWasWithNames() && !nameWasPI){
					legalStruct = getDataStructForCurrentOwner(m,global);
				} else{
					
					for( DocumentI reg: listRodocs){
						if(!reg.isOneOf(
								DocumentTypes.PLAT,
								DocumentTypes.RESTRICTION,
								DocumentTypes.EASEMENT,
								DocumentTypes.MASTERDEED,
								DocumentTypes.COURT,
								DocumentTypes.LIEN,
								DocumentTypes.CORPORATION,
								DocumentTypes.AFFIDAVIT,
								DocumentTypes.CCER,
								DocumentTypes.TRANSFER)
								
								|| isTransferAllowed(reg)
								) {
							for (PropertyI prop: reg.getProperties()){
								if(prop.hasLegal()){
									LegalI legal = prop.getLegal();
									treatLegalFromSavedDocument(reg.prettyPrint(), legal, false, null);
								}
							}
						}
					}
				}
				return listRodocs;
			}
			
			
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public boolean hasNext(long searchId) {
				boolean hasNext = super.hasNext(searchId);
				
				if(hasNext && isLoadedFromSearchPage()) {
					
					StatesIterator myStrategy = super.getStrategy();
					if(myStrategy instanceof DefaultStatesIterator) {
						DefaultStatesIterator legalStrategy = (DefaultStatesIterator)myStrategy;
						
						if(legalStrategy.getCurrentIndex() >= 0) {
						
							LegalStruct currentStr = (LegalStruct)legalStrategy.current();
							
							if(currentStr != null && !processedForNextStruct.contains(currentStr)) {
								Integer initialDocuments = initialDocumentsBeforeRunningStruct.get(currentStr);
								int currentDocuments = getDocumentsManagerDocSize();
								
								if(initialDocuments == null || currentDocuments > initialDocuments) {
									//so only if I have new documents added I need to check and clean the next iterations
									List currentList = legalStrategy.getList();
									int currentIndex = legalStrategy.getCurrentIndex();
									
									
									//need to copy already processed iterations
									List updatedList = new ArrayList();
									List restOfList = new ArrayList();
									for (int i = 0; i < currentList.size(); i++) {
										if(i > currentIndex) {
											restOfList.add(currentList.get(i));
										} else {
											updatedList.add(currentList.get(i));
										}
									}
									
									if(restOfList.size() > 0) {
									
										//now let's see if the restOfList needs to cleaned for already run iterations
										
										for (Object object : restOfList) {
											LegalStruct toCheckStruct = (LegalStruct)((LegalStruct)object).clone();
											toCheckStruct.setPlatBookType(currentStr.getPlatBookType());
											if(!toCheckStruct.equals(currentStr)) {
												//these structures have only plat book type in common and my type already brought results then I do not need new iterations
												updatedList.add(object);
											}
										}
										
										legalStrategy.replaceData(updatedList, currentIndex);
									
										//after altering the list, let's recheck again if we still have next 	
										hasNext = super.hasNext(searchId);
									
									}
									//mark current structure processed
									processedForNextStruct.add(currentStr);
								}
							}
						
						}
					}
					
					
				}
				
				
				
				/*
				if(hasNext) {
				
					Object current = super.getStrategy().peekAtNext();
					
					if(current != null && current instanceof LegalStruct) {
						LegalStruct pds = (LegalStruct)current;
						
						if(org.apache.commons.lang.StringUtils.isNotBlank(pds.getPlatBookType())) {
							if(!firstTime.contains(pds.getPlatBookType()) && firstTime.size() > 0) {
								//initialSiteDocuments.put(pds.platType, getDocumentsManagerDocSize());
								int minInitialSiteDocuments = Integer.MAX_VALUE;
								for (String typesDone : firstTime) {
									if(initialSiteDocuments.get(typesDone) < minInitialSiteDocuments) {
										minInitialSiteDocuments = initialSiteDocuments.get(typesDone);
									}
								}
								
								
								int newSiteDocuments = getDocumentsManagerDocSize();
								
								if(minInitialSiteDocuments < newSiteDocuments) {
									hasNext = false;
								}
								
							} else {
								
							}
						}
					}
				}
				*/
				
				return hasNext;
			}
			
			protected int getDocumentsManagerDocSize() {
				DocumentsManagerI docManager = getSearch().getDocManager();
				int newSiteDocuments = 0;
				try {
					docManager.getAccess();
					newSiteDocuments = docManager.getDocumentsWithDataSource(false, dataSite.getSiteTypeAbrev()).size();
				} finally {
					docManager.releaseAccess();
				}
				return newSiteDocuments;
			}
			
			@Override
			public void loadSecondaryPlattedLegal(LegalI legal, LegalStruct legalStruct) {
				
				SubdivisionI subdivision = legal.getSubdivision();
				
				legalStruct.setLot(subdivision.getLot());
				legalStruct.setBlock(subdivision.getBlock());
				legalStruct.setSection(subdivision.getSection());
				
				if(subdivision instanceof SubdivisionDetailedI) {
					SubdivisionDetailedI subdivisionDetailedI = (SubdivisionDetailedI)subdivision;
					legalStruct.setPlatBookSuffix(subdivisionDetailedI.getPlatBookSuffix());
					legalStruct.setPlatPageSuffix(subdivisionDetailedI.getPlatPageSuffix());
					legalStruct.setPlatBookType(subdivisionDetailedI.getPlatBookType());
				}
				
			}
			
			@Override
			protected void loadDerrivation(TSServerInfoModule module,
					LegalStruct str) {
				
				if(!initialDocumentsBeforeRunningStruct.containsKey(str)) {
					initialDocumentsBeforeRunningStruct.put(str, getDocumentsManagerDocSize());
				}
				
//				firstTime = false;
				for (Object functionObject : module.getFunctionList()) {
					if (functionObject instanceof TSServerInfoFunction) {
						TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
						switch (function.getIteratorType()) {
						case FunctionStatesIterator.ITERATOR_TYPE_LOT:
							function.setParamValue(str.getLot());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_BLOCK:
							function.setParamValue(str.getBlock());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE:
							function.setParamValue(str.getPlatBook());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE:
							function.setParamValue(str.getPlatPage());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE:
							if(org.apache.commons.lang.StringUtils.isNotBlank(str.getPlatBookType())) {
								if(!firstTime.contains(str.getPlatBookType())) {
									initialSiteDocuments.put(str.getPlatBookType(), getDocumentsManagerDocSize());
								}
								firstTime.add(str.getPlatBookType());
							}
							function.setParamValue(org.apache.commons.lang.StringUtils.defaultString(str.getPlatBookType()));
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_GENERIC_68:
							function.setParamValue(org.apache.commons.lang.StringUtils.defaultString(str.getPlatBookSuffix()));
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_GENERIC_69:
							function.setParamValue(org.apache.commons.lang.StringUtils.defaultString(str.getPlatPageSuffix()));
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_SECTION:
							function.setParamValue(org.apache.commons.lang.StringUtils.defaultString(str.getSection()));
							break;
						}
					}
				}
			}
			
			@Override
			public void processSubdivisionName(Search global, String originalLot, String originalBlock,
					String originalUnit, Set<String> temporarySubdivisionsForCondoSearch) {
			}
			@Override
			protected void processSubdivisionLotBlock(String subdivName, String lot, String block) {
			}
			@Override
			protected void processSubdivisionTractPlatBookPage(String subdivName, String platBook, String platPage,
					String tract) {
			}
			
			@Override
			public boolean isTransferAllowed(RegisterDocumentI doc) {
				
				if(doc != null && doc.isOneOf(DocumentTypes.TRANSFER)) {
					String[] realTransferSubcategories = DocumentTypes.getRealTransferSubcategories(
							Integer.parseInt(getSearch().getStateId()), 
							Integer.parseInt(getSearch().getCountyId()));
					if(doc.isOneOfSubcategory(realTransferSubcategories)) {
						return true;
					}
				}
				
				return false;
			}
			
//			@Override
//			protected String cleanPlatBook(String platBook) {
//				if(platBook == null) {
//					return "";
//				}
//				return platBook.trim().replaceFirst("^[A-Z]+", "");
//			}
			
		};
		
		it.setAdditionalInfoKey(AdditionalInfoKeys.ATIDS_LOOK_UP_DATA);
		it.setEnableTownshipLegal(false);
		it.setEnableSubdividedLegal(true);
		it.setEnableSubdivision(false);
		it.setLoadFromSearchPage(false);
		it.setLoadFromSearchPageIfNoLookup(true);
		it.setForceSubdividedIterationWithoutBlock(false);
		it.setPlatTypes(new String[]{"PB", "CB", "OR", "CN", "UN"});
		//it.setRoDoctypesToLoad(new String[]{"MORTGAGE", "TRANSFER", "RELEASE"});
		
		return it;
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
			DocsValidator[] docsValidatorsCrossref,
			Boolean grantor) {
		
		TSServerInfoModule	m = null; 
		List<NameI> newNames = new ArrayList<NameI>();
		
		
		{
			if(grantor == null) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			} else {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX41));
		    	if(grantor) {
		    		m.forceValue(9, "Grantor");
		    	} else {
		    		m.forceValue(9, "Grantee");
		    	}
			}
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			extraInformation);
			m.setSaObjKey(key);
			m.clearSaKeys();
			m.forceValue(5, "Yes");
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setSaKey(7, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setSaKey(8, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			
			if(filters != null) {
				for (FilterResponse filterResponse : filters) {
					m.addFilter(filterResponse);
				}
			}
			addFilterForUpdate(m, true);
			if(docsValidators != null) {
				for (DocsValidator docsValidator : docsValidators) {
					m.addValidator(docsValidator);
				}
			}
			if(docsValidatorsCrossref != null) {
				for (DocsValidator docsValidator : docsValidatorsCrossref) {
					m.addCrossRefValidator(docsValidator);
				}
			}
			m.addCrossRefValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator());
			
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, false, searchId, new String[] {"L;F;" });
			nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
			nameIterator.setInitAgain(true);		//initialize again after all parameters are set
			if ( searchedNames!=null ) {
				nameIterator.setSearchedNames( searchedNames );
			}
			newNames.addAll(nameIterator.getSearchedNames()) ;
			
			m.addIterator(nameIterator);
			modules.add(m);
			
		}
		
		{ 
			if(grantor == null) {
		    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BUSINESS_NAME_MODULE_IDX));
			} else {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX42));
		    	if(grantor) {
		    		m.forceValue(5, "Grantor");
		    	} else {
		    		m.forceValue(5, "Grantee");
		    	}
			}
	    	
	    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			extraInformation);
			m.setSaObjKey(key);
			m.clearSaKeys();
			m.forceValue(2, "Yes");
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			m.setSaKey(3, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.setSaKey(4, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			
			if(filters != null) {
				for (FilterResponse filterResponse : filters) {
					m.addFilter(filterResponse);
				}
			}
			addFilterForUpdate(m, true);
			if(docsValidators != null) {
				for (DocsValidator docsValidator : docsValidators) {
					m.addValidator(docsValidator);
				}
			}
			if(docsValidatorsCrossref != null) {
				for (DocsValidator docsValidator : docsValidatorsCrossref) {
					m.addCrossRefValidator(docsValidator);
				}
			}
			m.addCrossRefValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator());
			
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, false, searchId, new String[] {"L;F;" });
			nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
			nameIterator.setInitAgain(true);		//initialize again after all parameters are set
			if ( searchedNames!=null ) {
				nameIterator.setSearchedNames( searchedNames );
			}
			newNames.addAll(nameIterator.getSearchedNames()) ;
			
			m.addIterator(nameIterator);
			modules.add(m);
		
		}
		
		if(searchedNames == null) {
			searchedNames = new ArrayList<NameI>();
			searchedNames.addAll(newNames);
		}
		
		return searchedNames;
	}
	
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			//personal name
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantor");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				
				module.forceValue(5, "Yes");

				String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
				if (date != null) {
					module.getFunction(7).forceValue(date);
				}
				module.getFunction(8).forceValue(endDate);
				
				GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
				defaultNameFilter.setIgnoreMiddleOnEmpty(true);
				defaultNameFilter.setUseArrangements(false);
				module.addFilter(defaultNameFilter);
				
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
						new String[] {"L;F;" });
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
				nameIterator.clearSearchedNames();
				nameIterator.setInitAgain(true);
				module.addIterator(nameIterator);
				
				module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				modules.add(module);
			}
			
			//commercial name
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BUSINESS_NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantor");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				
				module.forceValue(2, "Yes");
				
				String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
				if (date != null) {
					module.getFunction(3).forceValue(date);
				}
				module.getFunction(4).forceValue(endDate);
				
				GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
				defaultNameFilter.setIgnoreMiddleOnEmpty(true);
				defaultNameFilter.setUseArrangements(false);
				module.addFilter(defaultNameFilter);
				
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
						new String[] {"L;F;" });
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
				nameIterator.clearSearchedNames();
				nameIterator.setInitAgain(true);
				
				module.addIterator(nameIterator);
				module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				modules.add(module);
			}
			
			

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				
				//personal name
				{
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
					module.setIndexInGB(id);
					module.setTypeSearchGB("grantee");
					module.clearSaKeys();
					module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
					
					module.forceValue(5, "Yes");
					
					String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
					date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
					if (date != null) {
						module.getFunction(7).forceValue(date);
					}
					module.getFunction(8).forceValue(endDate);
										
					module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
					module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
					
					GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
					defaultNameFilter.setIgnoreMiddleOnEmpty(true);
					defaultNameFilter.setUseArrangements(false);
					module.addFilter(defaultNameFilter);
					
					nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
							new String[] {"L;F;" });
					nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
					nameIterator.clearSearchedNames();
					nameIterator.setInitAgain(true);
					module.addIterator(nameIterator);
					
					module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
					module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					modules.add(module);
				}
				
				//commercial name
				{
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BUSINESS_NAME_MODULE_IDX));
					module.setIndexInGB(id);
					module.setTypeSearchGB("grantee");
					module.clearSaKeys();
					module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
					
					module.forceValue(2, "Yes");
					
					String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
					date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
					if (date != null) {
						module.getFunction(3).forceValue(date);
					}
					module.getFunction(4).forceValue(endDate);
										
					module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
					
					GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
					defaultNameFilter.setIgnoreMiddleOnEmpty(true);
					defaultNameFilter.setUseArrangements(false);
					module.addFilter(defaultNameFilter);
					
					nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
							new String[] {"L;F;" });
					nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
					nameIterator.clearSearchedNames();
					nameIterator.setInitAgain(true);
					module.addIterator(nameIterator);
					
					module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
					module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					modules.add(module);
				}
				
			}
		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		ParsedResponse parsedResponse = Response.getParsedResponse();
    	
    	switch(viParseID){
	    	case ID_DETAILS :
			case ID_SAVE_TO_TSD :
				DocumentI document = parsedResponse.getDocument();
				
				if(document!= null) {
					msSaveToTSDFileName = document.getId() + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				}
				break;
    	}
    	
	}
	
	/**
     * Looks for the a document having the same instrumentNo
     * @param instrumentNo
     * @param documentToCheck if not null will only compare its instrument with saved documents
     * @param data
     * @return
     */
    public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	
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
    				if(!checkFlexibleInclusion(documentToCheck, documentManager, false).isEmpty()) {
    					return true;
    				}
    			}
    		}
    		
			boolean isInstrumentSaved = super.isInstrumentSaved(instrumentNo, documentToCheck, data, checkMiServerId);
			if (!isInstrumentSaved) {

				DocumentI documentToCheckClone = null;
				if (documentToCheck != null) {
					documentToCheckClone = documentToCheck.clone();
					documentToCheckClone.setDocSubType(null);

					// check if doc is already saved on RO2
					Integer[] RO2Type = { GWTDataSite.R2_TYPE };
					List<DocumentI> ro2Documents = documentManager.getDocumentsWithSiteType(false, RO2Type);

					for (DocumentI ro2Document : ro2Documents) {
						DocumentI ro2DocumentClone = null;

						if (ro2Document != null) {
							ro2DocumentClone = ro2Document.clone();
							ro2DocumentClone.setDocSubType(null);

							String ro2DocInstNo = ro2DocumentClone.getInstno();
							String ro2CleanedInstNo = "";
							String year = String.valueOf(ro2DocumentClone.getYear());

							if (StringUtils.isNotEmpty(ro2DocInstNo) && StringUtils.isNotEmpty(year)) {
								if (ro2DocInstNo.length() >= year.length() && year.equals(ro2DocInstNo.substring(0, year.length()))) {
									ro2CleanedInstNo = ro2DocInstNo.substring(4).replaceFirst("^0+", "");
								} else if (ro2DocInstNo.length() >= 2 && year.endsWith(ro2DocInstNo.substring(0, 2))) {
									ro2CleanedInstNo = ro2DocInstNo.substring(2).replaceFirst("^0+", "");
								}
								if (!ro2CleanedInstNo.isEmpty()) {
									ro2DocumentClone.setInstno(ro2CleanedInstNo);
								}

								isInstrumentSaved = documentToCheckClone.flexibleEquals(ro2DocumentClone);
								if (isInstrumentSaved) {
									break;
								}
							}
						}
					}
				}
			}
			
			return isInstrumentSaved;
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }

	private List<DocumentI> checkFlexibleInclusion(InstrumentI documentToCheck, DocumentsManagerI documentManager, boolean checkType) {
		
		List<DocumentI> checkDocumentFlexible = checkDocumentFlexible(documentToCheck, documentManager, checkType);
		
		if(!checkDocumentFlexible.isEmpty()) { 
			return checkDocumentFlexible;
		} else if(documentToCheck.hasInstrNo()) {
			//need to do an ugly check
			List<DocumentI> allAtiDocuments = documentManager.getDocumentsWithDataSource(false, getDataSite().getSiteTypeAbrev());
			for (DocumentI documentI : allAtiDocuments) {
				if(documentI.getInstno().endsWith(documentToCheck.getInstno())) {
					InstrumentI cloneToCheck = documentToCheck.clone();
					if(cloneToCheck instanceof RegisterDocumentI && documentI instanceof RegisterDocumentI) {
						((RegisterDocumentI)cloneToCheck).setRecordedDate(((RegisterDocumentI)documentI).getRecordedDate());
					}
					cloneToCheck.setYear(documentI.getYear());
					cloneToCheck.setInstno(GenericATIDSFunctions.generateSpecificInstrument(cloneToCheck, getDataSite()));
					
					checkDocumentFlexible.addAll(checkDocumentFlexible(cloneToCheck, documentManager, checkType));
					
				}
			}
		}
		
		return checkDocumentFlexible;
	}

	private List<DocumentI> checkDocumentFlexible(InstrumentI documentToCheck, DocumentsManagerI documentManager, boolean checkType) {
		List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck);
		
		List<DocumentI> alike = new ArrayList<DocumentI>();
		
		if(almostLike != null && !almostLike.isEmpty()) {
			
			if(!checkType) {
				return almostLike;
			}
			if(org.apache.commons.lang.StringUtils.isNotBlank(documentToCheck.getBookType())) {
				for (DocumentI documentI : almostLike) {
					if(org.apache.commons.lang.StringUtils.isNotBlank(documentI.getBookType()) 
							&& documentI.getBookType().equals(documentToCheck.getBookType())) {
						alike.add(documentI);
					}
					
				}
			}
		}
		return alike;
	}
    
    @Override
    protected boolean fakeDocumentAlreadyExists(ServerResponse response, String htmlContent, boolean forceOverritten, DocumentsManagerI manager, DocumentI documentToCheck){
    	if(documentToCheck.isFake() || "MISCELLANEOUS".equals(documentToCheck.getDocType())){    	
//			List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, documentToCheck);
//			if(almostLike != null && !almostLike.isEmpty()) {
//				if(org.apache.commons.lang.StringUtils.isNotBlank(documentToCheck.getBookType())) {
//					for (DocumentI documentI : almostLike) {
//						if(org.apache.commons.lang.StringUtils.isNotBlank(documentI.getBookType()) 
//								&& documentI.getBookType().equals(documentToCheck.getBookType())) {
//							return true;
//						}
//					}
//					return false;
//				} else {
//					return true;
//				}
//			}
    		return !checkFlexibleInclusion(documentToCheck, manager, true).isEmpty();
    	}
    	
		return false;
	}

	@Override
	protected DocumentI getAlreadySavedDocument(DocumentsManagerI manager, DocumentI doc) {
		DocumentI alreadySaved = super.getAlreadySavedDocument(manager, doc);
		if(alreadySaved != null) {
			return alreadySaved;
		}
		
		DocumentI documentToCheck = doc.clone();
		documentToCheck.setDocType(DocumentTypes.MISCELLANEOUS);
		documentToCheck.setDocSubType(DocumentTypes.MISCELLANEOUS);
		
		List<DocumentI> almostLike = checkFlexibleInclusion(documentToCheck, manager, true);
		
		if(almostLike.size() > 0) {
			return almostLike.get(0);
		}
				
//				manager.getDocumentsWithInstrumentsFlexible(false, documentToCheck);
//		if(almostLike != null && !almostLike.isEmpty()) {
//			if(org.apache.commons.lang.StringUtils.isNotBlank(documentToCheck.getBookType())) {
//				for (DocumentI documentI : almostLike) {
//					if(org.apache.commons.lang.StringUtils.isNotBlank(documentI.getBookType()) 
//							&& documentI.getBookType().equals(documentToCheck.getBookType())) {
//						return documentI;
//					}
//				}
//			}
//		}
		return null;
	}
	
	@Override
	protected int[] getModuleIdsForSavingLegal() {
		return new int[]{TSServerInfo.SUBDIVISION_MODULE_IDX, TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX, 
				TSServerInfo.SECTION_LAND_MODULE_IDX, TSServerInfo.SURVEYS_MODULE_IDX};
	}
	@Override
	protected int[] getModuleIdsForSavingAddress() {
		return new int[]{};
	}
	@Override
	protected int[] getModuleIdsForSavingName() {
		return new int[]{TSServerInfo.NAME_MODULE_IDX, TSServerInfo.BUSINESS_NAME_MODULE_IDX};
	}
	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		
		if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 3) {
			String lastName = module.getFunction(0).getParamValue();
			String firstName = module.getFunction(1).getParamValue();
			String middleName = module.getFunction(2).getParamValue();
			if (!StringUtils.isEmpty(lastName)) {
				name.setLastName(lastName);
				if (!StringUtils.isEmpty(firstName)) {
					name.setFirstName(firstName);
				}
				if (!StringUtils.isEmpty(middleName)) {
					name.setMiddleName(middleName);
				}
				return name;
			}
		} else if (module.getModuleIdx() == TSServerInfo.BUSINESS_NAME_MODULE_IDX && module.getFunctionCount() > 1) {
			String companyName = module.getFunction(0).getParamValue();
			if (!StringUtils.isEmpty(companyName)) {
				name.setLastName(companyName);
				name.setCompany(true);
				return name;
			}	
		}
			
		return null;
	}
	
	@Override
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		LegalI legal = new Legal();

		if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX 
			 && module.getFunctionCount() > 7) {
			
			SubdivisionDetailedI subdivision = new SubdivisionDetailed();

			subdivision.setPlatBook(module.getFunction(0).getParamValue().trim());
			subdivision.setPlatBookSuffix(module.getFunction(1).getParamValue().trim());
			subdivision.setPlatPage(module.getFunction(2).getParamValue().trim());
			subdivision.setPlatPageSuffix(module.getFunction(3).getParamValue().trim());
			subdivision.setPlatBookType(module.getFunction(4).getParamValue().trim());
			
			subdivision.setLot(module.getFunction(5).getParamValue().trim());
			subdivision.setBlock(module.getFunction(6).getParamValue().trim());
			subdivision.setSection(module.getFunction(7).getParamValue().trim());
			
			legal = new Legal();
			legal.setSubdivision(subdivision);
		} else if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX 
				 && module.getFunctionCount() > 7) {
			
				SubdivisionDetailedI subdivision = new SubdivisionDetailed();
	
				subdivision.setPlatInstrument(module.getFunction(0).getParamValue().trim());
				subdivision.setPlatInstrumentSuffix(module.getFunction(1).getParamValue().trim());
				subdivision.setPlatInstrumentYear(module.getFunction(2).getParamValue().trim());
				subdivision.setPlatInstrumentCode(module.getFunction(3).getParamValue().trim());
				subdivision.setPlatBookType(module.getFunction(4).getParamValue().trim());
				
				subdivision.setLot(module.getFunction(5).getParamValue().trim());
				subdivision.setBlock(module.getFunction(6).getParamValue().trim());
				subdivision.setSection(module.getFunction(7).getParamValue().trim());
				
				legal = new Legal();
				legal.setSubdivision(subdivision);
		} else if (module.getModuleIdx() == TSServerInfo.SECTION_LAND_MODULE_IDX 
				 && module.getFunctionCount() >= 8) {
			TownShipI townShip = new TownShip();
			
			townShip.setSection(module.getFunction(1).getParamValue().trim());
			townShip.setTownship(module.getFunction(2).getParamValue().trim());
			townShip.setFirstDirection(module.getFunction(3).getParamValue().trim());
			townShip.setRange(module.getFunction(4).getParamValue().trim());
			townShip.setSecondDirection(module.getFunction(5).getParamValue().trim());
			
			String code = module.getFunction(8).getParamValue().trim();
			townShip.setQuarterValue(code);
			
			legal = new Legal();
			legal.setTownShip(townShip);
		} else if (module.getModuleIdx() == TSServerInfo.SURVEYS_MODULE_IDX 
				 && module.getFunctionCount() >= 5) {
			TownShipI townShip = new TownShip();
			
			townShip.setSection(module.getFunction(1).getParamValue().trim());
			townShip.setTownship(module.getFunction(2).getParamValue().trim());
			townShip.setFirstDirection(module.getFunction(3).getParamValue().trim());
			townShip.setRange(module.getFunction(4).getParamValue().trim());
			townShip.setSecondDirection(module.getFunction(5).getParamValue().trim());
			
//			String code = module.getFunction(8).getParamValue().trim();
//			townShip.setQuarterValue(code);
			
			legal = new Legal();
			legal.setTownShip(townShip);
		}

		return legal;
	}
	
	protected String getInstrumentNumberForSavingInFinalResults(DocumentI doc) {
		return (doc.getInstno() + doc.getDocType() + doc.getBook() + doc.getPage()).toUpperCase();
	}
	
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		List<TSServerInfoModule> list = new ArrayList<TSServerInfoModule>();
		
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		String instrNo = restoreDocumentDataI.getInstrumentNumber();
		int year = restoreDocumentDataI.getYear();
		TSServerInfoModule module = null;
		
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(book) && ro.cst.tsearch.utils.StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.MODULE_IDX44);
			module.forceValue(0, book);
			module.forceValue(2, page);
			module.forceValue(4, "*");
			module.getFilterList().clear();
			Date recordedDate = restoreDocumentDataI.getRecordedDate();
			if (recordedDate!=null) {
				ExactDateFilterResponse dateFilter = new ExactDateFilterResponse(searchId, recordedDate);
				module.addFilter(dateFilter);
			}
			list.add(module);
		}
		
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrNo)	&& year != SimpleChapterUtils.UNDEFINED_YEAR) {
			module = getDefaultServerInfo().getModule(TSServerInfo.MODULE_IDX43);
			module.forceValue(0, instrNo);
			module.forceValue(1, "*");
			module.forceValue(2, Integer.toString(year));
			Date recordedDate = restoreDocumentDataI.getRecordedDate();
			if (recordedDate!=null) {
				ExactDateFilterResponse dateFilter = new ExactDateFilterResponse(searchId, recordedDate);
				module.addFilter(dateFilter);
			}
			list.add(module);
		}
		
		module = new TSServerInfoModule(0, TSServerInfo.FAKE_MODULE_IDX);
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE, restoreDocumentDataI);
		list.add(module);
		
		return list;
	}
	
	@Override
	protected ServerResponse performRequest(String page, int methodType, String action, 
			int parserId, String imagePath, String vbRequest, Map<String, Object> extraParams)
			throws ServerResponseException {
		if(page.matches("/FK____\\d+_\\d+")) {
			ServerResponse response = new ServerResponse();
			response.setError(ServerResponse.NOT_VALID_DOC_ERROR);
			return response;
		} else {
			return super.performRequest(page, methodType, action, parserId, imagePath,
					vbRequest, extraParams);
		}
	}
	
	@Override
    public void countOrder() {
		getSearch().countOrder(getSearch().getSa().getAtribute(SearchAttributes.ATIDS_FILE_REFERENCE_ID), 
				getDataSite().getCityCheckedInt());
	}
	
	@Override
	public void performAdditionalProcessingAfterRunningAutomatic() {
		super.performAdditionalProcessingAfterRunningAutomatic();
		
		Search search = getSearch();
		@SuppressWarnings("unchecked")
		Set<Integer> additionalInfo = (Set<Integer>) search.getAdditionalInfo(AdditionalInfoKeys.MISSING_LAND_SEARCH_ID_SET);
		if(additionalInfo != null) {
			
			boolean landSearchPerformed = false;
			int[] moduleIds = new int[]{TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX, TSServerInfo.SUBDIVISION_MODULE_IDX, 
				TSServerInfo.SECTION_LAND_MODULE_IDX, TSServerInfo.SURVEYS_MODULE_IDX};
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
	
	@Override
	public void addDocumentAdditionalPostProcessing(DocumentI doc, ServerResponse response) {
		super.addDocumentAdditionalPostProcessing(doc, response);
		doc.setIncludeImage(true);
		doc.setManualIncludeImage(true);
	}
}

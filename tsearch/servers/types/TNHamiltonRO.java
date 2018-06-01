package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.datacontract.schemas._2004._07.register_webservices.ArrayOfDocumentParty;
import org.datacontract.schemas._2004._07.register_webservices.ArrayOfDocumentProperty;
import org.datacontract.schemas._2004._07.register_webservices.ArrayOfDocumentRefersTo;
import org.datacontract.schemas._2004._07.register_webservices.ArrayOfSearchResult;
import org.datacontract.schemas._2004._07.register_webservices.DocumentParty;
import org.datacontract.schemas._2004._07.register_webservices.DocumentProperty;
import org.datacontract.schemas._2004._07.register_webservices.DocumentRefersTo;
import org.datacontract.schemas._2004._07.register_webservices.SearchResult;
import org.datacontract.schemas._2004._07.register_webservices.SearchResults;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.http2.TNHamiltonROConnection;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.Ccer;
import com.stewart.ats.base.document.Corporation;
import com.stewart.ats.base.document.Court;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.Lien;
import com.stewart.ats.base.document.Mortgage;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.Transfer;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameMortgageGrantee;
import com.stewart.ats.base.name.NameMortgageGranteeI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;

public class TNHamiltonRO extends TSServerROLike implements TSServerROLikeI {
    
    protected static final Category logger= Logger.getLogger(TNHamiltonRO.class);
    
    public static final long serialVersionUID = 1l;
    
    private transient TNHamiltonROConnection connection = null;
    
    private static final Map<String, Integer> bpType = new HashMap<String, Integer>() {
		private static final long serialVersionUID = 1L;
	{
    	put("GI", 1);
    	put("P0", 2);
    	put("P1", 3);
    	put("P2", 4);
    	put("P3", 5);
    }}; 
    
    private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}
    
    public TNHamiltonRO(long searchId) {
        super(searchId);
        resultType = MULTIPLE_RESULT_TYPE;
    }

    public TNHamiltonRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
    }
    
    private TNHamiltonROConnection getConnection() {
		if(connection != null) {
			return connection;
		}
		try {
			connection = new TNHamiltonROConnection(getDataSite(), getSearch());
		} catch (Exception e) {
			logger.error("Error while creating connection to web service", e);
		}
		return connection;
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
    
    @Override
	protected ServerResponse SearchBy(boolean bResetQuery, TSServerInfoModule module, Object sd)
			throws ServerResponseException {
		
		getSearch().clearClickedDocuments();
        if(!Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS))) {
        	//this is needed because, sometimes, doing a search for image will 
        	//destroy the document viewed but not yet saved in parent site.
        	getSearch().removeAllInMemoryDocs();
        }
        
        logSearchBy(module);
		
        SearchResults result = null;
        TNHamiltonROConnection connection = getConnection();
        if(connection == null) {
        	ServerResponse serverResponse = new ServerResponse();
    		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
    		
    		if(result == null) {
        		String errorMessage = "Site Error. Could not create connection to official server.";
        		serverResponse.setError(errorMessage);
        		parsedResponse.setError(errorMessage);
            	return serverResponse;
        	}
        }
		switch (module.getModuleIdx()) {
			case TSServerInfo.NEXT_LINK_MODULE_IDX:
				String seqStored = module.getParamValue(0);
				//int fromItemID = Integer.parseInt(module.getParamValue(1));
				String linkType = module.getParamValue(2);
				TSServerInfoModule storedModule = (TSServerInfoModule) getSearch().getAdditionalInfo(getCurrentServerName() + ":params:" + seqStored);
				
				if(storedModule != null) {
					if(storedModule.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
						int storedFromItemId = Integer.parseInt(storedModule.getParamValue(12));
						if("next".equals(linkType)) {
							storedModule.setParamValue(12, Integer.toString(storedFromItemId + 175));
						} else if("prev".equals(linkType)) {
							storedModule.setParamValue(12, Integer.toString(storedFromItemId - 175));
						}
						module = storedModule;
						result = connection.searchByPersonName(module);
					} else if(storedModule.getModuleIdx() == TSServerInfo.ADDRESS_MODULE_IDX) {
						int storedFromItemId = Integer.parseInt(storedModule.getParamValue(4));
						if("next".equals(linkType)) {
							storedModule.setParamValue(4, Integer.toString(storedFromItemId + 175));
						} else if("prev".equals(linkType)) {
							storedModule.setParamValue(4, Integer.toString(storedFromItemId - 175));
						}
						module = storedModule;
						result = connection.searchByProperty(module);
					} else if(storedModule.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX) {
						int storedFromItemId = Integer.parseInt(storedModule.getParamValue(5));
						if("next".equals(linkType)) {
							storedModule.setParamValue(5, Integer.toString(storedFromItemId + 175));
						} else if("prev".equals(linkType)) {
							storedModule.setParamValue(5, Integer.toString(storedFromItemId - 175));
						}
						module = storedModule;
						result = connection.searchByProperty(module);
					} else if(storedModule.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX) {
						int storedFromItemId = Integer.parseInt(storedModule.getParamValue(8));
						if("next".equals(linkType)) {
							storedModule.setParamValue(8, Integer.toString(storedFromItemId + 175));
						} else if("prev".equals(linkType)) {
							storedModule.setParamValue(8, Integer.toString(storedFromItemId - 175));
						}
						module = storedModule;
						result = connection.searchByProperty(module);
					}
				}
				
				break;

			case TSServerInfo.NAME_MODULE_IDX:
				result = connection.searchByPersonName(module);
				break;
			case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
				result = connection.searchByBookPage(module);
				break;
			case TSServerInfo.INSTR_NO_MODULE_IDX:
				result = connection.searchByInstrument(module);
				break;
			case TSServerInfo.ADDRESS_MODULE_IDX:
			case TSServerInfo.PARCEL_ID_MODULE_IDX:
			case TSServerInfo.SUBDIVISION_MODULE_IDX:
				result = connection.searchByProperty(module);
				break;
			case TSServerInfo.IMG_MODULE_IDX:
				
	    		ServerResponse response = new ServerResponse();
	    		ImageI image = new Image();
	    		Set<String> list = new HashSet<String>();
	    		list.add("&fakeName=" + module.getFunction(0).getParamValue() + 
	    				"&documentID=" + module.getFunction(1).getParamValue() + 
	    				"&documentTypeID=" + module.getFunction(2).getParamValue());
	    		image.setLinks(list);
	    		image.setContentType("image/tiff");
	    		response.setImageResult(saveImage(image));
		    		
		    	return response;
				
		}
		
		ServerResponse serverResponse = processResponse(module, result);
		
		return serverResponse;
	}
    
    private ServerResponse processResponse(TSServerInfoModule module, SearchResults result) {
    	ServerResponse serverResponse = new ServerResponse();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		
		if(result == null) {
    		String errorMessage = "Site Error.";
    		serverResponse.setError(errorMessage);
    		parsedResponse.setError(errorMessage);
        	return serverResponse;
    	}
		
		if(!result.isSetIsSuccessful()) {
			String errorMessage = "Site Error. Search failed!";
    		serverResponse.setError(errorMessage);
    		parsedResponse.setError(errorMessage);
        	return serverResponse;
		}
		
		if(result.getItemCount() == 0) {
			return serverResponse;
		}
		
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
    	StringBuilder htmlContent = new StringBuilder();
		String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
		
		ArrayOfSearchResult resultList = result.getResultList();
		SearchResult[] searchResultArray = resultList.getSearchResultArray();
		for (int i = 0; i < searchResultArray.length; i++) {
			SearchResult searchResult = searchResultArray[i];
			
			ParsedResponse currentResponse = new ParsedResponse();
			
			RegisterDocumentI registerDocumentI = getDocument(searchResult);
			
			currentResponse.setDocument(registerDocumentI);
			
			String checkBox = "checked";
			if (isInstrumentSaved("gogo", registerDocumentI, null) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
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
			
			StringBuilder footer = new StringBuilder();
			
			
			
			if(registerDocumentI.hasImage()) {
				footer.append("<br><a href=\"")
						.append(createPartialLink(TSConnectionURL.idPOST, TSServerInfo.IMG_MODULE_IDX)) 
						.append(getShortImageLink(
								searchResult.getDocumentID(),
								searchResult.getDocumentTypeID()))
						.append("\" title=\"View Image\" target=\"_blank\">View Image</a>");
			}
			
			for(InstrumentI parsedReference : registerDocumentI.getParsedReferences()) {
				
				if(parsedReference.hasBookPage()) {
					footer.append("<br><a href=\"")
						.append(createPartialLink(TSConnectionURL.idPOST, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX)) 
						.append("&book_no=").append(parsedReference.getBook()) 
						.append("&page_no=").append(parsedReference.getPage())  
						.append("&bkpg_bk=").append(bpType.get(parsedReference.getBookType()))
						.append("\" title=\"View Document ")
						.append(parsedReference.getBookType())
						.append(" ").append(parsedReference.getBook())
						.append(" ").append(parsedReference.getPage())
						.append("\">View Document ")
						.append(parsedReference.getBookType())
						.append(" ").append(parsedReference.getBook())
						.append(" ").append(parsedReference.getPage())
						.append("</a>");
					
					/*
					TSServerInfoModule moduleCrossref = getDefaultServerInfo()
							.getModuleForSearch(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, new SearchDataWrapper());
					moduleCrossref.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
					moduleCrossref.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
					moduleCrossref.getFunction(0).setData(parsedReference.getBook());
					moduleCrossref.getFunction(1).setData(parsedReference.getPage());
					moduleCrossref.getFunction(2).setData(bpType.get(parsedReference.getBookType()).toString());
					
					
					ParsedResponse prChild = new ParsedResponse();
					LinkInPage linkInPage = new LinkInPage(moduleCrossref, TSServer.REQUEST_SEARCH_BY_REC);
					linkInPage.setOnlyLink("Book/Page " + parsedReference.getBookType() + " " + parsedReference.getBook() + " " + parsedReference.getPage() + " was already searched for.");
					prChild.setPageLink(linkInPage);
					*/
					
					
					
					ParsedResponse prChild = new ParsedResponse();
					
					String linkString = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX)
						+ "&" + getLinkPrefix(TSConnectionURL.idPOST)
						+ "&book_no=" + parsedReference.getBook() 
						+ "&page_no=" + parsedReference.getPage()  
						+ "&bkpg_bk=" + bpType.get(parsedReference.getBookType())
						+ "&isSubResult=true";
					
					
					
					LinkInPage pl = new LinkInPage(linkString,linkString,TSServer.REQUEST_SAVE_TO_TSD);
					prChild.setPageLink(pl);
					
					
					
					currentResponse.addOneResultRowOnly(prChild);
					
				}
			}
			
			
			String asHtml = registerDocumentI.asHtml(); 
			currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + asHtml + footer.toString()  + "</td></tr>");
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + asHtml + "</td></tr>");
			currentResponse.setSearchId(searchId);
			currentResponse.setUseDocumentForSearchLogRow(true);
			responses.add(currentResponse);
			
		}
		
		
		
		
		long noResults = responses.size();
    	SearchLogger.info("Found <span class='number'>" + noResults + "</span> <span class='rtype'>intermediate</span> results.<br/>",searchId);
    	
    	
    	
    	
    	if(noResults > 0) {
    		
    		String linkNext = null;
    		String linkPrevious = null;
    		
    		if(searchResultArray.length == 175 && result.getItemCount() != searchResultArray.length) {
    			//we have next link, store this module
    			
    			int seq = getSeq();
    			getSearch().setAdditionalInfo(getCurrentServerName() + ":params:" + seq, module);
    			
    			if(result.getItemCount() > result.getFromItemID() + searchResultArray.length) {
    				linkNext = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.NEXT_LINK_MODULE_IDX) + "&seq=" + seq + "&fromItemID=" + result.getFromItemID() +  "&linkType=next";
    				parsedResponse.setNextLink("<a href=" + linkNext + ">Next</a>");
    			}
    			
    			if(result.getFromItemID() > 1) {
    				//we also have previous link
    				linkPrevious = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.NEXT_LINK_MODULE_IDX) + "&seq=" + seq + "&fromItemID=" + result.getFromItemID() +  "&linkType=prev";
    			}
    			
    			
    		}
    		
    		
			parsedResponse.setResultRows(new Vector<ParsedResponse>(responses));
			parsedResponse.setOnlyResponse(htmlContent.toString());
			serverResponse.setResult(htmlContent.toString());
			
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
            	StringBuilder header = new StringBuilder(parsedResponse.getHeader());
               	String footer = parsedResponse.getFooter();                           	
            	header.append(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST"));
            	header.append("\n<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n");
            	if(linkNext != null || linkPrevious != null) {
            		header.append("<td><td colspan=2 align=center>");
            		if(linkPrevious != null) {
            			header.append("&nbsp;<a href=\"" + linkPrevious + "\"><- Previous</a>&nbsp;");
            		}
            		
            		header.append("Showing results from " + result.getFromItemID() + " to " + (result.getFromItemID() + searchResultArray.length - 1))
            			.append(" (from a total of ").append(result.getItemCount()).append(") ");
            		
            		if(linkNext != null) {
            			header.append("&nbsp;<a href=\"" + linkNext + "\">Next -></a>&nbsp;");
            		}
            		
            		header.append("</td></tr>");
            	} else {
            		header.append("<td><td colspan=2 align=center>")
            			.append("Showing results from " + result.getFromItemID() + " to " + (result.getFromItemID() + searchResultArray.length - 1))
            			.append(" (from a total of ").append(result.getItemCount()).append(") ")
            			.append("</td></tr>");
            	}
            	header.append("<tr><th rowspan=1>").append(SELECT_ALL_CHECKBOXES).append("</th>")
            		.append("<td align=\"center\">Document Content</td></tr>");

            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 
            				ID_SEARCH_BY_NAME, (Integer)numberOfUnsavedDocument);
            	} else {
            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, -1);
            	}

            	
            	parsedResponse.setHeader(header.toString());
            	parsedResponse.setFooter(footer);
            	
            	StringBuilder sb = new StringBuilder();
        		
        		sb.append(StringUtils.createCollapsibleHeader())
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
		
		return serverResponse;
	}
    
    @Override
    protected DownloadImageResult saveImage(ImageI image)
    		throws ServerResponseException {
    	
    	if(image != null) {
    		String allParams = image.getLink(0);
    		String documentID = allParams.replaceAll(".*&documentID=([^&]+).*", "$1");
    		String documentTypeID = allParams.replaceAll(".*&documentTypeID=([^&]+).*", "$1");
    		
    		try {
    			
    			byte[] imageBytes = getConnection().getImage(
    					Integer.parseInt(documentID), 
    					Integer.parseInt(documentTypeID)
    					);
    			
    			if(imageBytes != null) { 
    			
					DownloadImageResult downloadImageResult = new DownloadImageResult(
							DownloadImageResult.Status.OK, imageBytes, image.getContentType());
					
					afterDownloadImage(true);
					
					return downloadImageResult;
    			}
				
			} catch (Exception e) {
				logger.error(searchId + " error for " + allParams, e);
			} 
    		
    	}
    	
    	return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
    }
    
    private RegisterDocumentI getDocument(SearchResult searchResult) {
		
		InstrumentI inst = new Instrument();
		if(searchResult.isSetBookNo()) {
			inst.setBook(Integer.toString(searchResult.getBookNo()));
		} 
		if(searchResult.isSetPageNo()) {
			inst.setPage(Integer.toString(searchResult.getPageNo()));
		}
		if(searchResult.isSetBookType()) {
			inst.setBookType(searchResult.getBookType());
		}
		if(StringUtils.isNotEmpty(searchResult.getInstrumentNo())) {
			inst.setInstno(searchResult.getInstrumentNo().replaceAll(" ", "-"));
		}
		
		Date fileDate = null;
		
		if(searchResult.isSetFileDate()) {
			String fileDateString = searchResult.getFileDate().trim();
			if(fileDateString.contains(" ")) {
				String date = fileDateString.replaceAll("(.+?) .*", "$1");
				fileDate = Util.dateParser3(date);
				inst.setDate(fileDate);
				Calendar cal = Calendar.getInstance();
				cal.setTime(fileDate);
				inst.setYear(cal.get(Calendar.YEAR));
			}
		}
		
		String serverDocType = "MISCELLANEOUS";
		
		if(StringUtils.isNotEmpty(searchResult.getDocumentTypeDesc())) {
			serverDocType = searchResult.getDocumentTypeDesc();
			String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
	    	inst.setDocType(docCateg);
	    	String stype = DocumentTypes.getDocumentSubcategory(serverDocType, searchId);
	    	if("MISCELLANEOUS".equals(stype)&&!"MISCELLANEOUS".equals(docCateg)){
	    		stype = docCateg;
	    	}
	    	inst.setDocSubType(stype);
		}
		
		RegisterDocument docR = new RegisterDocument( DocumentsManager.generateDocumentUniqueId(searchId, inst) );
		docR.setInstrument(inst);
		
		docR.setServerDocType(serverDocType);
    	docR.setType(SimpleChapterUtils.DType.ROLIKE);
    	
    	docR.setDataSource(getDataSite().getSiteTypeAbrev());
    	docR.setSiteId((int)dataSite.getServerId());
    	
    	if(fileDate != null) {
    		docR.setRecordedDate(fileDate);
    	}
    	
    	ArrayOfDocumentProperty documentPropertyList = searchResult.getDocumentPropertyList();
    	
    	if(documentPropertyList != null && documentPropertyList.sizeOfDocumentPropertyArray() > 0) {
    		
    		LinkedHashSet<PropertyI> atsProperties = new LinkedHashSet<PropertyI>();
    		for (DocumentProperty documentProperty : documentPropertyList.getDocumentPropertyArray()) {
    			PropertyI atsProperty = Property.createEmptyProperty();
    			
    			AddressI address = atsProperty.getAddress();
    			String addressString = documentProperty.getAddress();
    			if(StringUtils.isNotEmpty(addressString)) {
    				StandardAddress stdAddr = new StandardAddress(addressString);
    				address.setNumber(stdAddr.getAddressElement(StandardAddress.STREET_NUMBER));
    				address.setStreetName(stdAddr.getAddressElement(StandardAddress.STREET_NAME));
					address.setPreDiretion(stdAddr.getAddressElement(StandardAddress.STREET_PREDIRECTIONAL));
					address.setSuffix(stdAddr.getAddressElement(StandardAddress.STREET_SUFFIX));
					address.setIdentifierNumber(stdAddr.getAddressElement(StandardAddress.STREET_SEC_ADDR_RANGE));
					address.setPreDiretion(stdAddr.getAddressElement(StandardAddress.STREET_POSTDIRECTIONAL));
    			}
    			if(StringUtils.isNotEmpty(documentProperty.getCityName())) {
    				address.setCity(documentProperty.getCityName());
    			}
    			
    			
    			LegalI legal = atsProperty.getLegal();
    			
    			if(StringUtils.isNotEmpty(documentProperty.getPropDesc())) {
    				legal.setFreeForm(documentProperty.getPropDesc());
    			}
    			SubdivisionI subdivision = new Subdivision();
    			legal.setSubdivision(subdivision);
    			if(StringUtils.isNotEmpty(documentProperty.getBlock())) {
    				subdivision.setBlock(documentProperty.getBlock());
    			}
    			if(StringUtils.isNotEmpty(documentProperty.getLot())) {
    				subdivision.setLot(documentProperty.getLot().replaceAll("\\bPT\\b", "").trim());
    			}
    			if(StringUtils.isNotEmpty(documentProperty.getSubdivision())) {
    				subdivision.setName(documentProperty.getSubdivision());
    			}
    			if(StringUtils.isNotEmpty(documentProperty.getUnit())) {
    				subdivision.setUnit(documentProperty.getUnit());
    			}
    			
    			if(StringUtils.isNotEmpty(documentProperty.getMGP())) {
    				String mgp = documentProperty.getMGP();
    				mgp = mgp.replaceAll("^[\\s\\.-]*(.*?)[\\s\\.-]*$", "$1").replaceAll("[\\.\\s]+", "-").replaceAll("[-]{2,}", "");
    				if(!mgp.isEmpty()){
    					PinI pin = atsProperty.getPin();
    					pin.addPin(PinI.PinType.PID, mgp);
    				}
    				
    			}
    			
    			logger.debug("documentProperty.getSubOrDisp() : " + documentProperty.getSubOrDisp());
    			
    			atsProperties.add(atsProperty);
    			
    		}
    		docR.setProperties(atsProperties);
    	}
    	
    	
    	
    	PartyI grantors = new com.stewart.ats.base.parties.Party(PType.GRANTOR);
		PartyI grantees = new com.stewart.ats.base.parties.Party(PType.GRANTEE);
		
		
		ArrayOfDocumentParty documentPartyList = searchResult.getDocumentPartyList();
		
		if(documentPartyList != null) {
			DocumentParty[] documentPartyArray = documentPartyList.getDocumentPartyArray();
			if(documentPartyArray != null) {
				for (int indexParty = 0; indexParty < documentPartyArray.length; indexParty++) {
					DocumentParty documentParty = documentPartyArray[indexParty];
					if(documentParty != null) {
						String partyName = documentParty.getPartyName();
						partyName = NameCleaner.cleanFreeformName(partyName);
						
						String[] names = { "", "", "", "", "", "" };
						String[] suffixes, type, otherType;
								
						partyName = partyName.replaceAll("(?is)\\b[FAN]\\s*/\\s*K\\s*/\\s*A\\b", "\n");
						partyName = partyName.replaceAll("(?is)\\bADM\\b", "");//means ADMINISTRATOR
						partyName = partyName.replaceAll("(?is)\\bSU[CB]\\s+(TRUSTEE)\\b", " $1");
						
						String[] nameItems = partyName.split("\\s*/\\s*");
						for (int i = 0; i < nameItems.length; i++){
							nameItems[i] = nameItems[i].replaceAll("(?is)\\s+(TR(?:USTEE)?S?)(,.*)", "$2 $1");
							names = StringFormats.parseNameNashville(nameItems[i], true);
										
							type = GenericFunctions.extractNameType(names);
							otherType = GenericFunctions.extractNameOtherType(names);
							suffixes = GenericFunctions.extractNameSuffixes(names);
							
							
							if(StringUtils.isNotEmpty(names[2])) {
								NameI name = new Name();
								name.setLastName(names[2]);
								name.setFirstName(names[0]);
								name.setMiddleName(names[1]);
								name.setSufix(suffixes[0]);
								name.setNameType(type[0]);
								name.setNameOtherType(otherType[0]);
								name.setCompany(NameUtils.isCompany(names[2]));
								
								if("1".equals(documentParty.getPartyType())) {	//grantor
									grantors.add(name);
								} else if("2".equals(documentParty.getPartyType())) { //grantee
									grantees.add(name);
								}

							}
							
							if(StringUtils.isNotEmpty(names[5])) {
								NameI name = new Name();
								name.setLastName(names[5]);
								name.setFirstName(names[3]);
								name.setMiddleName(names[4]);
								name.setSufix(suffixes[1]);
								name.setNameType(type[1]);
								name.setNameOtherType(otherType[1]);
								name.setCompany(NameUtils.isCompany(names[5]));
								
								if("1".equals(documentParty.getPartyType())) {	//grantor
									grantors.add(name);
								} else if("2".equals(documentParty.getPartyType())) { //grantee
									grantees.add(name);
								}
							}
						}
					}
				}
			}
		}
			
		docR.setGrantor(grantors);
		docR.setGrantee(grantees);
    	
		
		ArrayOfDocumentRefersTo documentRefersToList = searchResult.getDocumentRefersToList();
		if(documentRefersToList != null && documentRefersToList.sizeOfDocumentRefersToArray() > 0) {
			DocumentRefersTo[] documentRefersToArray = documentRefersToList.getDocumentRefersToArray();
			
			Set<InstrumentI> crossRefs = new HashSet<InstrumentI>();
			
			for (int i = 0; i < documentRefersToArray.length; i++) {
				DocumentRefersTo documentRefersTo = documentRefersToArray[i];
				String referInfo = documentRefersTo.getReferInfo();
				Pattern referInfoPattern = Pattern.compile("(\\w+) (\\w+) (\\w+)");
				Matcher m = referInfoPattern.matcher(referInfo);
				if(m.matches()) {
					Instrument instrCrossRef = new Instrument();
					instrCrossRef.setBook(m.group(2));
					instrCrossRef.setPage(m.group(3));
					instrCrossRef.setBookType(m.group(1));
					crossRefs.add(instrCrossRef);
				}
			}
			
			docR.setParsedReferences(crossRefs);
		}
		
		String pageCount = searchResult.getPageCount();
		if(pageCount != null) {
			try {
				int pageCountInt = Integer.parseInt(pageCount);
				if(pageCountInt > 0) {
					getSearch().addImagesToDocument(
							docR,
							getShortImageLink(
									searchResult.getDocumentID(), 
									searchResult.getDocumentTypeID()
									));
				}
			} catch (NumberFormatException e) {
			}
		}
		
		if (docR.getDocType().equals(DocumentTypes.MORTGAGE)){
			Mortgage mortgage = new Mortgage(docR);
			BigDecimal amount = searchResult.getAmount();
			if(amount != null) {
				mortgage.setMortgageAmount(amount.doubleValue());
			}
			PartyI granteesLander = new com.stewart.ats.base.parties.Party(PType.GRANTEE);
			grantees = mortgage.getGrantee();
			for (NameI name : grantees.getNames()) {
				NameMortgageGranteeI nameGrantee = new NameMortgageGrantee(name);
				nameGrantee.setTrustee(false);
				granteesLander.add( nameGrantee );
			}
			mortgage.setGrantee(granteesLander);
			
			
			docR = mortgage;
		} else if (docR.getDocType().equals(DocumentTypes.TRANSFER)) {
			Transfer transfer = new Transfer(docR);
			BigDecimal amount = searchResult.getAmount();
			if(amount != null) {
				transfer.setSalePrice(amount.doubleValue());
				transfer.setConsiderationAmount(amount.doubleValue());
			}
			docR = transfer;
		} else if(docR.getDocType().equals(DocumentTypes.COURT)){
			Court court = new Court(docR);
			BigDecimal amount = searchResult.getAmount();
			if(amount != null) {
				court.setConsiderationAmount(amount.doubleValue());
				court.setConsiderationAmountFreeForm(amount.toString());
			}
			docR = court;
		} else if(docR.getDocType().equals(DocumentTypes.LIEN)){
			Lien lien = new Lien(docR);
			BigDecimal amount = searchResult.getAmount();
			if(amount != null) {
				lien.setConsiderationAmount(amount.doubleValue());
			}
			docR  = lien;
		} else if(docR.getDocType().equals(DocumentTypes.CCER)){
			docR =  new Ccer(docR); 
		} else if (docR.getDocType().equals(DocumentTypes.CORPORATION)) {
			docR = new Corporation(docR);
		}
		
		
		
		return docR;
	}

    private String getShortImageLink(int documentID, int documentTypeID) {
		return  "&documentID=" + documentID + 
				"&documentTypeID=" + documentTypeID +  
				"&fakeName=fake.tiff";
	
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
    protected ServerResponse performRequest(String page, int methodType, String action, int parserId, String imagePath,
    		String vbRequest, Map<String, Object> extraParams) throws ServerResponseException {

    	if(page.startsWith("/FK____")) {
			ServerResponse response = new ServerResponse();
			response.setError(ServerResponse.NOT_VALID_DOC_ERROR);
			return response;
		}
    	
    	String dispatcher = vbRequest.replaceAll(".*&dispatcher=([^&]+).*", "$1");
		int poz = dispatcher.indexOf("&");
    	if(poz>0){
    		dispatcher = dispatcher.substring(0,poz);
    	}
    	int dispatcherInt = -2;
    	try{
    		dispatcherInt = Integer.parseInt(dispatcher);
    	} catch(Exception e){
    		e.printStackTrace();
    	}
    	
    	if(dispatcherInt == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) {
    		TSServerInfoModule moduleCrossref = getDefaultServerInfo()
    				.getModuleForSearch(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, new SearchDataWrapper());
    		moduleCrossref.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
    		moduleCrossref.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
    		moduleCrossref.getFunction(0).setData(vbRequest.replaceAll(".*&book_no=([^&]+).*", "$1"));
    		moduleCrossref.getFunction(1).setData(vbRequest.replaceAll(".*&page_no=([^&]+).*", "$1"));
    		moduleCrossref.getFunction(2).setData(vbRequest.replaceAll(".*&bkpg_bk=([^&]+).*", "$1"));
    		
    		ServerResponse response = SearchBy(moduleCrossref, null);
    		
    		ParsedResponse parsedResponse = response.getParsedResponse();
    		@SuppressWarnings("rawtypes")
			Vector resultRows = parsedResponse.getResultRows();
    		for (Object object : resultRows) {
    			ParsedResponse pr = (ParsedResponse)object;
    			
    			ServerResponse internal = new ServerResponse();
    			internal.setParsedResponse(pr);
    			
    			solveResponse(page, parserId, action, internal, new RawResponseWrapper((String)pr.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE)), imagePath);
    			
			}
    		return response;
    	} else if(dispatcherInt == TSServerInfo.NEXT_LINK_MODULE_IDX) {
    		TSServerInfoModule moduleCrossref = getDefaultServerInfo()
    				.getModuleForSearch(TSServerInfo.NEXT_LINK_MODULE_IDX, new SearchDataWrapper());
    		moduleCrossref.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
    		moduleCrossref.getFunction(0).setData(vbRequest.replaceAll(".*&seq=([^&]+).*", "$1"));
    		moduleCrossref.getFunction(1).setData(vbRequest.replaceAll(".*&fromItemID=([^&]+).*", "$1"));
    		moduleCrossref.getFunction(2).setData(vbRequest.replaceAll(".*&linkType=([^&]+).*", "$1"));
    		
    		ServerResponse response = SearchBy(moduleCrossref, null);
    		return response;
    	}
    		
    	return new ServerResponse();
    }
    
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded)
            throws ServerResponseException {
        msLastLink = vsRequest;
        return performRequest("", miGetLinkActionType, "GetLink", ID_GET_LINK, null, vsRequest, null);
    }
        
    
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		serverInfo.setModulesForAutoSearch(l);
	}

	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}

	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI document) {
		// TODO Auto-generated method stub
		return null;
	}
    
}

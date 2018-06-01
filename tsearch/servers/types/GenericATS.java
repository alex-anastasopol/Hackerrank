package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.connection.database.GenericATSConn;
import ro.cst.tsearch.database.rowmapper.ProductsMapper;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.FirstResultsFilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressDocumentFilter;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.misc.PriorFileAtsFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.tags.StatusSelect;
import ro.cst.tsearch.templates.CompileTemplateResult;
import ro.cst.tsearch.templates.TemplateUtils;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.PriorFileAtsDocumentI;
import com.stewart.ats.base.document.PriorFileDocument;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.misc.SelectableStatement;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;

public class GenericATS extends TSServerROLike {

	public static final int DOCUMENT_SIZE_LIMIT = 1;
	protected static final String PREVIOUSLY_INVALIDATED_DOCUMENTS = "PREVIOUSLY_INVALIDATED_DOCUMENTS";
	
	public static final double RECOVER_FILTER_STREET_NAME_LIMIT = 0.9;
    public static final double RECOVER_FILTER_SUBDIVISION_NAME_LIMIT = 0.85;
    public static final double RECOVER_FILTER_LOT_LIMIT = 0.7;
    public static final double RECOVER_FILTER_BLOCK_LIMIT = 0.7;
    public static final double RECOVER_FILTER_STREET_NUMBER_LIMIT = 0.7;
    
    public static final int WEIGHT_STREET_NAME = 100000;
    public static final int WEIGHT_SUBDIVISION_NAME = 200000;
    public static final int WEIGHT_LOT = 10000;
    public static final int WEIGHT_BLOCK = 10000;
    public static final int WEIGHT_STRET_NO = 20000;
    
    public static final int MAX_SCORE = WEIGHT_STREET_NAME + 
		    WEIGHT_SUBDIVISION_NAME + 
		    WEIGHT_LOT + 
		    WEIGHT_BLOCK + 
		    WEIGHT_STRET_NO;
	
	private static final long serialVersionUID = 1L;

	public GenericATS(long searchId) {
		super(searchId);
		internalPreloadProducts();
	}

	public GenericATS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		internalPreloadProducts();
	}

	/**
	 * Creates the multiple product select list for PS and the default selection for PS and Automatic
	 */
	private void internalPreloadProducts() {
		if (defaultProducts == null) {
			List<ProductsMapper> products = Products.getProductList();
			StringBuilder sb = new StringBuilder("<select name=\"product\" size=\"5\" multiple=\"\" ><option value=\"0\">ALL</option>");

			defaultProducts = new ArrayList<String>();

			for (ProductsMapper productsMapper : products) {
				sb.append("<option value=\"").append(productsMapper.getProductId()).append("\" ");
				if (!(Products.isOneOfUpdateProductType(productsMapper.getProductId()) || productsMapper.getProductId() == Products.OE_PRODUCT)) {
					sb.append(" selected ");
					defaultProducts.add(Integer.toString(productsMapper.getProductId()));
				}
				sb.append(">").append(productsMapper.getAlias()).append("</option>");
			}
			sb.append("</select>");

			htmlProduct = sb.toString();

		}
	}
	
	/**
	 * Loads the <b>product</b> field with default values on given <code>module</code>
	 * @param module the module to be filled
	 * @param originalFunctionId the function that originally has the <b>product</b> field on the given module
	 * @param ignoreCurrentProduct if true, only same search product id will be included, else the rest except me will be included
	 */
	private void internalLoadProducts(TSServerInfoModule module, int originalFunctionId, boolean ignoreCurrentProduct) {
		if(defaultProducts!= null && !defaultProducts.isEmpty()) {
			String currentProduct = getSearch().getSa().getAtribute(SearchAttributes.SEARCH_PRODUCT);
			module.getFunction(originalFunctionId).setHtmlformat(htmlProduct);
			if(ignoreCurrentProduct) {
				boolean loadedMyProduct = false;
				for (int i = 0; i < defaultProducts.size(); i++) {
					if(currentProduct.equals(defaultProducts.get(i))) {
						continue;
					}
					if(!loadedMyProduct) {
						module.forceValue(originalFunctionId, defaultProducts.get(i));
						loadedMyProduct = true;
					} else {
						int newFctId = module.addFunction();
						TSServerInfoFunction newFct = new TSServerInfoFunction(module.getFunction(originalFunctionId));
						newFct.setParamValue(defaultProducts.get(i));
						module.setFunction(newFctId, newFct);
					}
				}	
			} else {
				module.forceValue(originalFunctionId, currentProduct);
			}
		}
	}
	
	private transient GenericATSConn connection = null;
	private static List<String> defaultProducts;
	private static String htmlProduct;
		
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo defaultServerInfo = super.getDefaultServerInfo();
		
		TSServerInfoModule tsServerInfoModule = defaultServerInfo.getModule(TSServerInfo.OLD_ACCOUNT_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(2).setHtmlformat(htmlProduct);
		}
		
		tsServerInfoModule = defaultServerInfo.getModule(TSServerInfo.SERIAL_ID_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(2).setHtmlformat(htmlProduct);
		}
		
		tsServerInfoModule = defaultServerInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(2).setHtmlformat(htmlProduct);
		}
		
		tsServerInfoModule = defaultServerInfo.getModule(TSServerInfo.NAME_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(3).setHtmlformat(htmlProduct);
		}
		
		tsServerInfoModule = defaultServerInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(8).setHtmlformat(htmlProduct);
		}
		
		tsServerInfoModule = defaultServerInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(4).setHtmlformat(htmlProduct);
		}
		
		
		return defaultServerInfo;
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
		
        ServerResponse serverResponse = null;
        
        Map<Long, PriorFileAtsDocumentI> result = null;
		switch (module.getModuleIdx()) {
			case TSServerInfo.ADDRESS_MODULE_IDX:
				result = getConnection().getDocumentsByAddress(module);
				break;
			case TSServerInfo.SUBDIVISION_MODULE_IDX:
				result = getConnection().getDocumentsByLegal(module);
				break;
			case TSServerInfo.PARCEL_ID_MODULE_IDX:
				result = getConnection().getDocumentsByParcel(module);
				break;
			case TSServerInfo.NAME_MODULE_IDX:
				result = getConnection().getDocumentsByName(module);
				break;
			case TSServerInfo.SERIAL_ID_MODULE_IDX:
				result = getConnection().getDocumentsByFileNo(module);
				break;
			case TSServerInfo.OLD_ACCOUNT_MODULE_IDX:
				result = getConnection().getDocumentsBySearchId(module);
				break;
		}
		
		serverResponse = processResponse(module, result);		
		return serverResponse;
	}
	
	private GenericATSConn getConnection(){
		if(connection == null) {
			connection = new GenericATSConn(getSearch());
		}
		return connection;
	}
	
	@Override
	protected String CreateSaveToTSDFormEnd(String name, int parserId,
			int numberOfUnsavedRows) {
		if (name == null) {
            name = SAVE_DOCUMENT_BUTTON_LABEL;
        }
    	        
        String s = "";
        
    	if (numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0) {
        	s = "<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" " +"onclick=\"javascript:submitForm();\" >\r\n";
    	}
        
        
        return s+"</form>\n";
	}
	
	
	private ServerResponse processResponse(TSServerInfoModule module, Map<Long, PriorFileAtsDocumentI> result) {
		ServerResponse serverResponse = new ServerResponse();
		ParsedResponse parsedResponse = serverResponse.getParsedResponse();
		
		if(result == null) {
    		String errorMessage = "Site returned error for this request.";
    		serverResponse.setError(errorMessage);
    		parsedResponse.setError(errorMessage);
        	return serverResponse;
    	}
		
		
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
    	StringBuilder htmlContent = new StringBuilder();
    	String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
    	for (PriorFileAtsDocumentI documentI : result.values()) {
    		ParsedResponse currentResponse = new ParsedResponse();
    		
    		documentI.setDataSource(getDataSite().getSiteTypeAbrev());
    		documentI.setSiteId(getServerID());
    		if(isParentSite()) {
    			documentI.setSavedFrom(SavedFromType.PARENT_SITE);
			} else {
				documentI.setSavedFrom(SavedFromType.AUTOMATIC);
			}
    		
    		//parseStatements(documentI, null, null, searchId);
    		
    		currentResponse.setDocument(documentI);
    		
    		String checkBox = "checked";
			if (isInstrumentSaved("gogo", documentI, null) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
    			checkBox = "saved";
    		} else {
    			checkBox = "<input type='checkbox' name='docLink' value='" + linkPrefix + "FK____" + documentI.getId() + "'>";
    			
    			LinkInPage linkInPage = new LinkInPage(
    					linkPrefix + "FK____" + documentI.getId(), 
    					linkPrefix + "FK____" + documentI.getId(), 
    					TSServer.REQUEST_SAVE_TO_TSD);
    			
    			if(getSearch().getInMemoryDoc(linkPrefix + "FK____" + documentI.getId())==null){
    				getSearch().addInMemoryDoc(linkPrefix + "FK____" + documentI.getId(), currentResponse);
    				
    				/**
        			 * Save module in key in additional info. The key is instrument number that should be always available. 
        			 */
        			String keyForSavingModules = getKeyForSavingInIntermediary(documentI.getInstno());
        			getSearch().setAdditionalInfo(keyForSavingModules, module);
    			}
    			
    			currentResponse.setPageLink(linkInPage);
    			
    		}
			String imageLink =
					"<br><a href=\"" + "/title-search/DocumentDataRetreiver?indexDbId=" + documentI.getIndexId() + "&OperationType=GET_INDEX&searchId="
							+ searchId + "&tLogSearchId=" + documentI.getDocno() +
							"\" title=\"View TSRI Log\" target=\"_blank\">View TSRI Log</a>";
					
			
			String asHtml = documentI.asHtml(); 
			currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + asHtml + (imageLink!=null?imageLink:"")  + "</td></tr>");
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + asHtml + "</td></tr>");
			currentResponse.setSearchId(searchId);
			currentResponse.setUseDocumentForSearchLogRow(true);
			responses.add(currentResponse);
    		
    	}
    	
    	long noResults = responses.size();
    	SearchLogger.info("Found <span class='number'>" + noResults + "</span> <span class='rtype'>intermediate</span> results.<br/>",searchId);
    	
    	
    	if(noResults > 0) {
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
	
	public static void parseStatements(PriorFileAtsDocumentI documentI, String templateContents,
			HashMap<String, HashMap<String, Boolean>> statementsMap, long searchId) {
		try {
			HashMap<String, Boolean> reqMap = statementsMap != null ? statementsMap.get(PriorFileDocument.REQUIREMENTS) : null;
			HashMap<String, Boolean> legalMap = statementsMap != null ? statementsMap.get(PriorFileDocument.LEGAL_DESC) : null;
			HashMap<String, Boolean> excMap = statementsMap != null ? statementsMap.get(PriorFileDocument.EXCEPTIONS) : null;
			
			if(documentI.getRequirements() != null) { documentI.getRequirements().clear(); }
			if(documentI.getExceptionsList() != null) { documentI.getExceptionsList().clear(); }
			if(documentI.getLegalDescriptions() != null) { documentI.getLegalDescriptions().clear(); }
			
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			
			if(StringUtils.isEmpty(templateContents)) {
				CompileTemplateResult contents = TemplateUtils.getPriorFileAts(global, documentI, documentI.getId());
				
				if(contents == null) {
					return;
				}
				
				templateContents = contents.getTemplateContent();
			}

			Matcher m = Pattern.compile("(?is)<Requirements[^>]*>(.*)</Requirements>").matcher(templateContents);
			if(m.find()) {
				Matcher m1 = Pattern.compile("(?is)<!--(.*?)-->").matcher(m.group(1));
				while(m1.find()) {
					if(StringUtils.isNotEmpty(m1.group(1))) {
						documentI.addRequirements(SelectableStatement.splitTextIntoStatements(m1.group(1), reqMap));
					}
				}
			}
			
			m = Pattern.compile("(?is)<\\s*RequirementsNotes\\s+mleid\\s*=\\s*\"Additional Requirements\"\\s*>\\s*<!--(.*?)-->").matcher(templateContents);
			if(m.find()) {
				documentI.addRequirements(SelectableStatement.splitTextIntoStatements(m.group(1), reqMap));
			}
			
			m = Pattern.compile("(?is)<Legal[^>]*>(.*)</Legal>").matcher(templateContents);
			if(m.find()) {
				Matcher m1 = Pattern.compile("(?is)<!--(.*?)-->").matcher(m.group(1));
				while(m1.find()) {
					if(StringUtils.isNotEmpty(m1.group(1))) {
						documentI.addLegalDescriptions(SelectableStatement.splitTextIntoStatements(m1.group(1), legalMap));
					}
				}
			}
			
			m = Pattern.compile("(?is)LegalDescription=\"<!--(.*?)-->\"").matcher(templateContents);
			if(m.find()) {
				documentI.addLegalDescriptions(SelectableStatement.splitTextIntoStatements(m.group(1), legalMap));
			}
			
			m = Pattern.compile("(?is)<\\s*OrdDPLglDesc\\s+mleid\\s*=\\s*\"Legal Description\"\\s*>\\s*<!--(.*?)-->").matcher(templateContents);
			if(m.find()) {
				documentI.addLegalDescriptions(SelectableStatement.splitTextIntoStatements(m.group(1), legalMap));
			}
			
			
			
			m = Pattern.compile("(?is)<Exceptions>(.*)</Exceptions>").matcher(templateContents);
			if(m.find()) {
				Matcher m1 = Pattern.compile("(?is)<!--(.*?)-->").matcher(m.group(1));
				while(m1.find()) {
					if(StringUtils.isNotEmpty(m1.group(1))) {
						documentI.addExceptionsList(SelectableStatement.splitTextIntoStatements(m1.group(1), excMap));
					}
				}
			}
			m = Pattern.compile("(?is)Exceptions=\"<!--(.*?)-->\"").matcher(templateContents);
			if(m.find()) {
				documentI.addExceptionsList(SelectableStatement.splitTextIntoStatements(m.group(1), excMap));
			}
			
			m = Pattern.compile("(?is)<\\s*Exceptions\\s+mleid\\s*=\\s*\"Exceptions\"\\s*>\\s*<!--(.*?)-->").matcher(templateContents);
			if(m.find()) {
				documentI.addExceptionsList(SelectableStatement.splitTextIntoStatements(m.group(1), excMap));
			}

		}catch (Exception e) {
			if(e.getMessage().equals("There is no \".ats\" template saved for this base file!")) {
				logger.error("There is no \".ats\" template saved for this base file!");
			} else {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		Search global = getSearch();
		
    	List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
    	int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
			TSServerInfoModule module = null;
		
			SearchAttributes sa = global.getSa();
			String platBook = sa.getAtribute(SearchAttributes.LD_BOOKNO);
			String platPage = sa.getAtribute(SearchAttributes.LD_PAGENO);
			String lot = sa.getAtribute(SearchAttributes.LD_LOTNO);
			String block = sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			String subdivisionName = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
			String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);
			
			DocsValidator firstResultsFilterForPrior = new FirstResultsFilterResponse(searchId, DOCUMENT_SIZE_LIMIT).getValidator();
			DocsValidator defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
			DocsValidator addressFilter = new GenericAddressDocumentFilter("", new BigDecimal(0.77d), searchId).getValidator();
			
			DocsValidator priorFileAtsValidator = null;
			boolean isPriorFileAtsFilterNeeded = false;
			String useForThisState = ServerConfig.getUsePriorAtsFilterForStates();
			if (StringUtils.isNotEmpty(useForThisState)){
				String[] states = useForThisState.split("\\s*,\\s*");
				for (String state: states){
					if (state.equalsIgnoreCase(dataSite.getStateAbbreviation())){
						isPriorFileAtsFilterNeeded = true;
						priorFileAtsValidator = new PriorFileAtsFilter(searchId).getValidator();
						break;
					}
				}
			}
			
			// (1)
			if(hasPin()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.addExtraInformation(
						TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						"Parcel module - searching with parcel id for prior searches (excluding base searches)");
				ContinueIterator iterator = new ContinueIterator(searchId);
				iterator.ignoreBase();
				module.addIterator(iterator);
				module.forceValue(1, Integer.toString(StatusSelect.STATUS_D_AND_NOT_T));
				if (isPriorFileAtsFilterNeeded){
					module.addValidator(priorFileAtsValidator);
				}
				module.addValidator(firstResultsFilterForPrior);
				
				internalLoadProducts(module, 2, false);
				moduleList.add(module);
			}
			
			// (2)
			if(hasStreet() && hasStreetNo()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.addExtraInformation(
						TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						"Address module - searching with address for prior searches (excluding base searches)");
				ContinueIterator iterator = new ContinueIterator(searchId);
				iterator.ignoreBase();
				module.addIterator(iterator);
				module.forceValue(3, Integer.toString(StatusSelect.STATUS_D_AND_NOT_T));
				if (isPriorFileAtsFilterNeeded){
					module.addValidator(priorFileAtsValidator);
				}
				module.addValidator(addressFilter);
				module.addValidator(defaultLegalFilter);
				module.addValidator(firstResultsFilterForPrior);
				
				internalLoadProducts(module, 4, false);
				moduleList.add(module);
			}
			
			// (3)
			if(StringUtils.isNotEmpty(platBook) 
					&& StringUtils.isNotEmpty(platPage)
					&& (StringUtils.isNotEmpty(lot) || StringUtils.isNotEmpty(block))) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
				module.addExtraInformation(
						TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						"Legal module - searching with plat book/page + lot and/or block for prior searches (excluding searches searches)");
				ContinueIterator iterator = new ContinueIterator(searchId);
				iterator.ignoreBase();
				module.addIterator(iterator);
				module.clearSaKeys();
				module.forceValue(1, lot);
				module.forceValue(2, block);
				module.forceValue(5, platBook);
				module.forceValue(6, platPage);
				module.forceValue(7, Integer.toString(StatusSelect.STATUS_D_AND_NOT_T));
				if (isPriorFileAtsFilterNeeded){
					module.addValidator(priorFileAtsValidator);
				}
				module.addValidator(addressFilter);
				module.addValidator(defaultLegalFilter);
				module.addValidator(firstResultsFilterForPrior);
				
				internalLoadProducts(module, 8, false);
				moduleList.add(module);
			}
			
			// (4)
			if(StringUtils.isNotEmpty(subdivisionName) 
					&& (StringUtils.isNotEmpty(lot) || StringUtils.isNotEmpty(block))) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
				module.addExtraInformation(
						TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						"Legal module - searching with subdivision name + lot and/or block for prior searches (excluding base searches)");
				ContinueIterator iterator = new ContinueIterator(searchId);
				iterator.ignoreBase();
				module.addIterator(iterator);
				module.clearSaKeys();
				module.forceValue(0, subdivisionName);
				module.forceValue(1, lot);
				module.forceValue(2, block);
				module.forceValue(7, Integer.toString(StatusSelect.STATUS_D_AND_NOT_T));
				if (isPriorFileAtsFilterNeeded){
					module.addValidator(priorFileAtsValidator);
				}
				module.addValidator(addressFilter);
				module.addValidator(defaultLegalFilter);
				module.addValidator(firstResultsFilterForPrior);
				
				internalLoadProducts(module, 8, false);
				moduleList.add(module);
			}
			
			if(Products.OE_PRODUCT != global.getProductId()) {
			
				// (1prim)
				if(hasPin()) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
					module.addExtraInformation(
							TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
							"Parcel module - searching with parcel id for prior searches (excluding base searches)");
					ContinueIterator iterator = new ContinueIterator(searchId);
					iterator.ignoreBase();
					module.addIterator(iterator);
					module.forceValue(1, Integer.toString(StatusSelect.STATUS_D_AND_NOT_T));
					if (isPriorFileAtsFilterNeeded){
						module.addValidator(priorFileAtsValidator);
					}
					module.addValidator(firstResultsFilterForPrior);
					
					internalLoadProducts(module, 2, true);
					moduleList.add(module);
				}
				
				// (2prim)
				if(hasStreet() && hasStreetNo()) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.addExtraInformation(
							TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
							"Address module - searching with address for prior searches (excluding base searches)");
					ContinueIterator iterator = new ContinueIterator(searchId);
					iterator.ignoreBase();
					module.addIterator(iterator);
					module.forceValue(3, Integer.toString(StatusSelect.STATUS_D_AND_NOT_T));
					if (isPriorFileAtsFilterNeeded){
						module.addValidator(priorFileAtsValidator);
					}
					module.addValidator(addressFilter);
					module.addValidator(defaultLegalFilter);
					module.addValidator(firstResultsFilterForPrior);
					
					internalLoadProducts(module, 4, true);
					moduleList.add(module);
				}
				
				// (3prim)
				if(StringUtils.isNotEmpty(platBook) 
						&& StringUtils.isNotEmpty(platPage)
						&& (StringUtils.isNotEmpty(lot) || StringUtils.isNotEmpty(block))) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
					module.addExtraInformation(
							TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
							"Legal module - searching with plat book/page + lot and/or block for prior searches (excluding searches searches)");
					ContinueIterator iterator = new ContinueIterator(searchId);
					iterator.ignoreBase();
					module.addIterator(iterator);
					module.clearSaKeys();
					module.forceValue(1, lot);
					module.forceValue(2, block);
					module.forceValue(5, platBook);
					module.forceValue(6, platPage);
					module.forceValue(7, Integer.toString(StatusSelect.STATUS_D_AND_NOT_T));
					if (isPriorFileAtsFilterNeeded){
						module.addValidator(priorFileAtsValidator);
					}
					module.addValidator(addressFilter);
					module.addValidator(defaultLegalFilter);
					module.addValidator(firstResultsFilterForPrior);
					
					internalLoadProducts(module, 8, true);
					moduleList.add(module);
				}
				
				// (4prim)
				if(StringUtils.isNotEmpty(subdivisionName) 
						&& (StringUtils.isNotEmpty(lot) || StringUtils.isNotEmpty(block))) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
					module.addExtraInformation(
							TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
							"Legal module - searching with subdivision name + lot and/or block for prior searches (excluding base searches)");
					ContinueIterator iterator = new ContinueIterator(searchId);
					iterator.ignoreBase();
					module.addIterator(iterator);
					module.clearSaKeys();
					module.forceValue(0, subdivisionName);
					module.forceValue(1, lot);
					module.forceValue(2, block);
					module.forceValue(7, Integer.toString(StatusSelect.STATUS_D_AND_NOT_T));
					if (isPriorFileAtsFilterNeeded){
						module.addValidator(priorFileAtsValidator);
					}
					module.addValidator(addressFilter);
					module.addValidator(defaultLegalFilter);
					module.addValidator(firstResultsFilterForPrior);
					
					internalLoadProducts(module, 8, true);
					moduleList.add(module);
				}
			
			}
					
			FirstResultsFilterResponse firstResultsFilterForStarterFilter = new FirstResultsFilterResponse(searchId, DOCUMENT_SIZE_LIMIT);
			firstResultsFilterForStarterFilter.setApplyToPriorFiles(false);
			DocsValidator firstResultsFilterForStarter = firstResultsFilterForStarterFilter.getValidator();
			
			// (5)
			if (StringUtils.isNotEmpty(platBook) && StringUtils.isNotEmpty(platPage)) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
				module.addExtraInformation(
						TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						"Legal module - searching with plat book/page + lot and/or block for base searches (only base searches)");
				module.clearSaKeys();
				ContinueIterator iterator = new ContinueIterator(searchId);
				iterator.ignorePrior();
				module.addIterator(iterator);
				module.forceValue(5, platBook);
				module.forceValue(6, platPage);
				module.forceValue(7, Integer.toString(StatusSelect.STATUS_STARTER));
				if (isPriorFileAtsFilterNeeded){
					module.addValidator(priorFileAtsValidator);
				}
				module.addValidator(addressFilter);
				module.addValidator(defaultLegalFilter);
				module.addValidator(firstResultsFilterForStarter);
				
				internalLoadProducts(module, 8, false);
				moduleList.add(module);
			}
			
			// (6)
			if(StringUtils.isNotEmpty(subdivisionName)) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
				module.addExtraInformation(
						TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						"Legal module - searching with subdivision name + lot and/or block for base searches (only base searches)");
				module.clearSaKeys();
				ContinueIterator iterator = new ContinueIterator(searchId);
				iterator.ignorePrior();
				module.addIterator(iterator);
				module.forceValue(0, subdivisionName);
				module.forceValue(7, Integer.toString(StatusSelect.STATUS_STARTER));
				if (isPriorFileAtsFilterNeeded){
					module.addValidator(priorFileAtsValidator);
				}
				module.addValidator(addressFilter);
				module.addValidator(defaultLegalFilter);
				module.addValidator(firstResultsFilterForStarter);
				
				internalLoadProducts(module, 8, false);
				moduleList.add(module);
			}
			// (7)
			if(StringUtils.isNotEmpty(streetName)) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.addExtraInformation(
						TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						"Address module - searching with street name (only base searches)");
				module.clearSaKeys();
				ContinueIterator iterator = new ContinueIterator(searchId);
				iterator.ignorePrior();
				module.addIterator(iterator);
				module.forceValue(1, streetName);
				module.forceValue(3, Integer.toString(StatusSelect.STATUS_STARTER));
				if (isPriorFileAtsFilterNeeded){
					module.addValidator(priorFileAtsValidator);
				}
				module.addValidator(addressFilter);
				module.addValidator(defaultLegalFilter);
				module.addValidator(firstResultsFilterForPrior);
				
				internalLoadProducts(module, 4, false);
				moduleList.add(module);
			}
			
			if(Products.OE_PRODUCT != global.getProductId()) {
			
				// (5prim)
				if (StringUtils.isNotEmpty(platBook) && StringUtils.isNotEmpty(platPage)) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
					module.addExtraInformation(
							TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
							"Legal module - searching with plat book/page + lot and/or block for base searches (only base searches)");
					module.clearSaKeys();
					ContinueIterator iterator = new ContinueIterator(searchId);
					iterator.ignorePrior();
					module.addIterator(iterator);
					module.forceValue(5, platBook);
					module.forceValue(6, platPage);
					module.forceValue(7, Integer.toString(StatusSelect.STATUS_STARTER));
					if (isPriorFileAtsFilterNeeded){
						module.addValidator(priorFileAtsValidator);
					}
					module.addValidator(addressFilter);
					module.addValidator(defaultLegalFilter);
					module.addValidator(firstResultsFilterForStarter);
					
					internalLoadProducts(module, 8, true);
					moduleList.add(module);
				}
				
				// (6prim)
				if(StringUtils.isNotEmpty(subdivisionName)) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
					module.addExtraInformation(
							TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
							"Legal module - searching with subdivision name + lot and/or block for base searches (only base searches)");
					module.clearSaKeys();
					ContinueIterator iterator = new ContinueIterator(searchId);
					iterator.ignorePrior();
					module.addIterator(iterator);
					module.forceValue(0, subdivisionName);
					module.forceValue(7, Integer.toString(StatusSelect.STATUS_STARTER));
					if (isPriorFileAtsFilterNeeded){
						module.addValidator(priorFileAtsValidator);
					}
					module.addValidator(addressFilter);
					module.addValidator(defaultLegalFilter);
					module.addValidator(firstResultsFilterForStarter);
					
					internalLoadProducts(module, 8, true);
					moduleList.add(module);
				}
				// (7prim)
				if(StringUtils.isNotEmpty(streetName)) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.addExtraInformation(
							TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
							"Address module - searching with street name (only base searches)");
					module.clearSaKeys();
					ContinueIterator iterator = new ContinueIterator(searchId);
					iterator.ignorePrior();
					module.addIterator(iterator);
					module.forceValue(1, streetName);
					module.forceValue(3, Integer.toString(StatusSelect.STATUS_STARTER));
					if (isPriorFileAtsFilterNeeded){
						module.addValidator(priorFileAtsValidator);
					}
					module.addValidator(addressFilter);
					module.addValidator(defaultLegalFilter);
					module.addValidator(firstResultsFilterForPrior);
					
					internalLoadProducts(module, 4, true);
					moduleList.add(module);
				}
			}
			
			// (8) implemented in performAdditionalProcessingAfterRunningAutomatic
			
		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}

	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
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
	public void performAdditionalProcessingBeforeRunningAutomatic() {
		getSearch().removeAdditionalInfo(PREVIOUSLY_INVALIDATED_DOCUMENTS); 
		super.performAdditionalProcessingBeforeRunningAutomatic();
	}
	
	@Override
	public void performAdditionalProcessingAfterRunningAutomatic() {
		super.performAdditionalProcessingAfterRunningAutomatic();

		Search search = getSearch();
		DocumentsManagerI documentsManagerI = search.getDocManager();
		try {
			documentsManagerI.getAccess();
			List<DocumentI> allPriorFilesList = documentsManagerI.getDocumentsWithDataSource(false, getDataSite().getSiteTypeAbrev());
			List<DocumentI> automaticPriorFilesList = new ArrayList<DocumentI>();
			for (DocumentI documentI : allPriorFilesList) {
				if(documentI.isSavedFrom(SavedFromType.AUTOMATIC) && documentI.getDocSubType().equals(DocumentTypes.PRIORFILE_BASE_SEARCH)){
					automaticPriorFilesList.add(documentI);
				}
			}
			
			if(automaticPriorFilesList.isEmpty()) {
				@SuppressWarnings("unchecked")
				LinkedHashMap<String, PriorFileAtsDocumentI> storedResponses = (LinkedHashMap<String, PriorFileAtsDocumentI>)
					getSearch().getAdditionalInfo(PREVIOUSLY_INVALIDATED_DOCUMENTS); 
				if(storedResponses != null) {
					Collection<PriorFileAtsDocumentI> failedResponses = storedResponses.values();
					
					FilterResponse rejectSavedDocumentsFilterResponse = new RejectAlreadySavedDocumentsFilterResponse(searchId);
					FilterResponse pinValidator = PINFilterFactory.getDefaultPinFilter(search.getID());
					
					GenericAddressDocumentFilter streetFilter = new GenericAddressDocumentFilter(searchId);
					streetFilter.setMiServerID(miServerID);
					streetFilter.disableAll();
					streetFilter.setMarkIfCandidatesAreEmpty(true);
					streetFilter.setEnableName(true);
					streetFilter.setThreshold(new BigDecimal(RECOVER_FILTER_STREET_NAME_LIMIT));
					streetFilter.init();
				
					GenericNameFilter legalFilterResponse = (GenericNameFilter)NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
					legalFilterResponse.setMarkIfCandidatesAreEmpty(true);
					legalFilterResponse.setThreshold(new BigDecimal(RECOVER_FILTER_SUBDIVISION_NAME_LIMIT));
					legalFilterResponse.init();
					
					GenericLegal lotFilter = new GenericLegal(searchId);
					lotFilter.disableAll();
					lotFilter.setMarkIfCandidatesAreEmpty(true);
					lotFilter.setEnableLot(true);
					lotFilter.init();
				
					GenericLegal blockFilter = new GenericLegal(searchId);
					blockFilter.disableAll();
					blockFilter.setMarkIfCandidatesAreEmpty(true);
					blockFilter.setEnableBlock(true);
					blockFilter.init();
			
					GenericAddressFilter numberFilter = new GenericAddressFilter(searchId);
					numberFilter.disableAll();
					numberFilter.setMiServerID(miServerID);
					numberFilter.setEnableNumber(true);
					numberFilter.setMarkIfCandidatesAreEmpty(true);
					numberFilter.init();
					StringBuilder toLog = new StringBuilder();
					List<ServerResponse> failedResponsesList = new Vector<ServerResponse>();
					for (PriorFileAtsDocumentI documentI : failedResponses) {
						
						ParsedResponse parserRespons = new ParsedResponse();
						parserRespons.setDocument(documentI);
						
						
						if(rejectSavedDocumentsFilterResponse.getScoreOneRow(parserRespons) == ATSDecimalNumberFormat.ONE &&
								pinValidator.getScoreOneRow(parserRespons).doubleValue() >= pinValidator.getThreshold().doubleValue()) {
							
							toLog.append("<br>Analyzing invalidated document ").append(documentI.getInstno() + "/" + documentI.getDocno()).append("<br>");
							
						
							double scoreStreetName = 0;
							double scoreSubdivisionName = 0;
							double scoreLot = 0;
							double scoreBlock = 0;
							double scoreStreetNumber = 0;
							BigDecimal tempDecimal = null;
							
							tempDecimal = streetFilter.getScoreOneRow(parserRespons);
							if(tempDecimal == ATSDecimalNumberFormat.NA) {
								scoreStreetName = streetFilter.getThreshold().doubleValue() - 0.01;
								toLog.append("Street Name matching uncertain - missing information(default score used:")
									.append(scoreStreetName).append(")<br>");
							} else {
								scoreStreetName = tempDecimal.doubleValue();
								toLog.append("Street Name matching score = ").append(ATSDecimalNumberFormat.format(tempDecimal)).append("<br>");
							}
							if(scoreStreetName < RECOVER_FILTER_STREET_NAME_LIMIT) {
								toLog.append("Street Name matching fails the restore limit<br>");
							} else {
								toLog.append("Street Name matching succeeds to pass the restore limit<br>");
							}
							
							
							tempDecimal = legalFilterResponse.getScoreOneRow(parserRespons);
							if(tempDecimal == ATSDecimalNumberFormat.NA) {
								scoreSubdivisionName = legalFilterResponse.getThreshold().doubleValue() - 0.01;
								toLog.append("Subdivision Name matching uncertain - missing information (default score used:").
									append(scoreSubdivisionName).append(")<br>");
							} else {
								scoreSubdivisionName = tempDecimal.doubleValue();
								toLog.append("Subdivision Name matching score = ").append(ATSDecimalNumberFormat.format(tempDecimal)).append("<br>");
							}
							if(scoreSubdivisionName < RECOVER_FILTER_SUBDIVISION_NAME_LIMIT) {
								toLog.append("Subdivision Name matching fails the restore limit<br>");
							} else {
								toLog.append("Subdivision Name matching succeeds to pass the restore limit<br>");
							}
							
							//if we at least have the same address name or subdivision name we proceed
							if(scoreStreetName >= RECOVER_FILTER_STREET_NAME_LIMIT || 
									scoreSubdivisionName >= RECOVER_FILTER_SUBDIVISION_NAME_LIMIT) {
								
								double finalScore = 0;
								
								tempDecimal = lotFilter.getScoreOneRow(parserRespons);
								if(tempDecimal == ATSDecimalNumberFormat.NA) {
									scoreLot = lotFilter.getThreshold().doubleValue() - 0.01;
									toLog.append("Lot matching uncertain - missing information(default score used:").append(scoreLot).append(")<br>");
								} else {
									scoreLot = tempDecimal.doubleValue();
									toLog.append("Lot matching score = ").
										append(ATSDecimalNumberFormat.format(tempDecimal)).append("<br>");
								}

								tempDecimal = blockFilter.getScoreOneRow(parserRespons);
								if(tempDecimal == ATSDecimalNumberFormat.NA) {
									scoreBlock = blockFilter.getThreshold().doubleValue() - 0.01;
									toLog.append("Block matching uncertain - missing information(default score used:")
										.append(scoreBlock).append(")<br>");
								} else {
									scoreBlock = tempDecimal.doubleValue();
									toLog.append("Block matching score = ")
										.append(ATSDecimalNumberFormat.format(tempDecimal)).append("<br>");
								}
							
								tempDecimal = numberFilter.getScoreOneRow(parserRespons);
								if(tempDecimal == ATSDecimalNumberFormat.NA) {
									scoreStreetNumber = numberFilter.getThreshold().doubleValue() - 0.01;
									toLog.append("Street Number matching uncertain - missing information(default score used:")
										.append(scoreStreetNumber).append(")<br>");
								} else {
									scoreStreetNumber = tempDecimal.doubleValue();
									toLog.append("Street Number matching score = ")
										.append(ATSDecimalNumberFormat.format(tempDecimal)).append("<br>");
								}
								
								
								
								finalScore = WEIGHT_STREET_NAME * scoreStreetName + 
									WEIGHT_SUBDIVISION_NAME * scoreSubdivisionName + 
									WEIGHT_LOT * scoreLot + 
									WEIGHT_BLOCK * scoreBlock + 
									WEIGHT_STRET_NO * scoreStreetNumber
									;
							
								ServerResponse serverResponse = new ServerResponse();
								serverResponse.setParsedResponse(parserRespons);
								serverResponse.setBestScore(new BigDecimal(finalScore/MAX_SCORE));
								
								toLog.append("<br>")
									.append("Final score for document <b>").append(documentI.getInstno()).append(" / ").append(documentI.getDocno())
									.append("</b> is ").append(ATSDecimalNumberFormat.format(serverResponse.getBestScore()))
									.append("<br>");
								
								failedResponsesList.add(serverResponse);
							
							}
						}
					}
					
					
					
					Collections.sort(failedResponsesList, new Comparator<ServerResponse>() {

						@Override
						public int compare(ServerResponse o1, ServerResponse o2) {
							int score = -1 * ATSDecimalNumberFormat.format(o1.getBestScore()).compareTo(ATSDecimalNumberFormat.format(o2.getBestScore()));
							if (score == 0 ){
								score = ((PriorFileAtsDocumentI)o1.getParsedResponse().getDocument()).getRecordedDate()
									.compareTo(((PriorFileAtsDocumentI)o2.getParsedResponse().getDocument()).getRecordedDate());
							}
							return score;
						}
					});
					
					SearchLogger.info(toLog.toString(), searchId);
					
					for (ServerResponse serverResponse : failedResponsesList) {
						if(automaticPriorFilesList.size() < DOCUMENT_SIZE_LIMIT) {
							DocumentI document = serverResponse.getParsedResponse().getDocument();
							automaticPriorFilesList.add(document);
							addDocumentInATS(serverResponse, serverResponse.getParsedResponse().getResponse());
							SearchLogger.info("<br><span class='saved'>Resaved document from datasource " +  getDataSite().getSiteTypeAbrev() + 
									" <b>" + document.getInstno() + " / " + document.getDocno() +  "</b></span> with score " +  
									ATSDecimalNumberFormat.format(serverResponse.getBestScore()) + "<br>", searchId);
						}
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			documentsManagerI.releaseAccess();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void performAdditionalProcessingWhenInvalidatingDocument(
			ServerResponse response) {
		Search search = getSearch();
		ASThread thread = ASMaster.getSearch(search);
		if(thread != null) {
			TSServerInfoModule currentModule = thread.getCrtModule();
			if(currentModule != null) {
				if(Boolean.TRUE.equals(currentModule.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_RESAVE_DOCS))) {
					return;
				}
			}
		}
		
		LinkedHashMap<String, PriorFileAtsDocumentI> storedResponses = (LinkedHashMap<String,PriorFileAtsDocumentI>)
			getSearch().getAdditionalInfo(PREVIOUSLY_INVALIDATED_DOCUMENTS); 
		if(storedResponses == null) {
			storedResponses = new LinkedHashMap<String, PriorFileAtsDocumentI>();
			search.setAdditionalInfo(PREVIOUSLY_INVALIDATED_DOCUMENTS, storedResponses);
		}
		ParsedResponse parsedResponse = response.getParsedResponse();
		if(parsedResponse != null) {
			DocumentI document = parsedResponse.getDocument();
			if(document != null && document instanceof PriorFileAtsDocumentI & document.getDocSubType().equals(DocumentTypes.PRIORFILE_BASE_SEARCH)) {
				storedResponses.put(document.getDocno(), (PriorFileAtsDocumentI)document);
			}
		}
	}

/*
	
	@Override
	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent,
			boolean forceOverritten) {
		
		
		ADD_DOCUMENT_RESULT_TYPES result = super.addDocumentInATS(response, htmlContent, forceOverritten);
		
		try {
			if(result.equals(ADD_DOCUMENT_RESULT_TYPES.ADDED)) {
				String instno = response.getParsedResponse().getDocument().getDocno();
				long searchIdToOpen = Long.parseLong(instno);
				
				String[] retFinal 	 = new String[4];
				//retFinal[0] 		 = templateName;
				
				String content = null;
				
				
				//we need this to force creation of files
				Search searchToOpenFromDisk = SearchManager.getSearchFromDisk(searchIdToOpen);
				
				if(searchToOpenFromDisk == null) {
					throw new Exception("Cannot open base file!");
				}
				
				
				
				
//				if(searchToOpenFromDisk.getAgent() == null) {
//					throw new Exception("There is no \".ats\" template saved for this base file!");
//				}
//				
//				List<CommunityTemplatesMapper> userTemplates = UserUtils.getUserTemplates(
//						searchToOpenFromDisk.getAgent().getID().longValue(),
//						searchToOpenFromDisk.getProductId());
//				
//				
//				
//				
//				for (CommunityTemplatesMapper communityTemplatesMapper : userTemplates) {
//				
//					if(!communityTemplatesMapper.getPath().toLowerCase().endsWith(".ats")) {
//						continue;
//					}
				
				HashMap<String, String> generatedTemp = searchToOpenFromDisk.getGeneratedTemp();
				for (String string : generatedTemp.keySet()) {
					String	templateNewPath = searchToOpenFromDisk.getGeneratedTemp().get(string);
				
//					String	templateNewPath = searchToOpenFromDisk.getGeneratedTemp().get(communityTemplatesMapper.getId() + "");
					if(StringUtils.isEmpty(templateNewPath)) {
						continue;
					}
					File templateFile = new File(templateNewPath);
					if(templateFile.exists()) {
						RandomAccessFile rand = new RandomAccessFile(templateFile, "rw");
						byte[] b = new byte[(int) rand.length()];
						rand.readFully(b);
						rand.close();
						content = new String(b);
			
						content = content.replaceAll("https?://ats(?:prdinet|stginet|preinet)?[0-9]*\\.advantagetitlesearch\\.com(?::\\d+)?",
								ServerConfig.getAppUrl());
						
						//retFinal[0] = communityTemplatesMapper.getName();
						
						try {
							HashMap template = UserUtils.getTemplate(Integer.parseInt(string));
							if(template != null) {
								retFinal[0] = (String) template.get(UserUtils.TEMPLATE_NAME);
							}
						} catch (Exception e) {
							logger.error("Error while tring to load old template from database. Using just filename and not template name", e);
						}
						
						if(StringUtils.isEmpty(retFinal[0])) {
							retFinal[0] = FilenameUtils.getName(templateNewPath);
						}
						
						
						String tempContent = TemplateBuilder.replaceImageLinksInTemplate(content, searchToOpenFromDisk, false, true);
						
						if(!tempContent.equalsIgnoreCase(content)) {
						
							// download images before conversion process
							try {
								searchToOpenFromDisk.downloadImages(false);
							    //just close the last opened <div> tag
							    SearchLogger.info("</div>\n", searchToOpenFromDisk.getID());
							} catch (Exception e) {
								logger.error("Error while downloading images in the original search " +  searchToOpenFromDisk.getID(), e);
							}
							
							
							if( UtilForGwtServer.uploadImagesToSSF(searchToOpenFromDisk.getID(),false,false)<0 ){
								SearchLogger.info("<b>Could NOT RESERVE SSF TRANSACTION ID</b> for uploading images", searchToOpenFromDisk.getID());
							}
							
							
//							tempContent = TemplateBuilder.replaceImageLinksInTemplate(content, searchToOpenFromDisk, false, true);
//							
//							File templateFileBackup = new File(templateNewPath + "_backup");
//							if(!templateFileBackup.exists()) {
//								FileUtils.copyFile(templateFile, templateFileBackup);
//							}
//							
//							FileUtils.write(templateFile, tempContent);
							
							AsynchSearchSaverThread.getInstance().saveSearchContext( searchToOpenFromDisk );
							
							
							
						}
						break;
					}
				}
			}
		} catch(Exception e) {
			logger.error("Error while cleaning links for prior file search", e);
		}
		return result;
	}
*/
	
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI document) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static class ContinueIterator extends GenericRuntimeIterator<String> {

		private static final long serialVersionUID = 1L;
		
		private boolean checkPrior = true;
		private boolean checkBase = true;

		public ContinueIterator(long searchId) {
			super(searchId);
			setInitAgain(true);
		}

		@Override
		protected List<String> createDerrivations() {
			List<String> derivations = new ArrayList<String>();
			derivations.add("Choose your destiny!");
			Search search = getSearch();
			DocumentsManagerI docManager = search.getDocManager();
			try {
				docManager.getAccess();
				
				List<DocumentI> documentsWithDataSource = docManager.getDocumentsWithDataSource(false, "ATS");
				
				for (DocumentI documentI : documentsWithDataSource) {
					if (documentI instanceof PriorFileAtsDocumentI) {
						PriorFileAtsDocumentI atsDoc = (PriorFileAtsDocumentI) documentI;
						if( (checkPrior && !atsDoc.isBase()) || (checkBase && atsDoc.isBase()) ) {
							derivations.clear();
							break;
						}
					}
				}
				
				
			} finally {
				docManager.releaseAccess();
			}
			return derivations;
		}

		@Override
		protected void loadDerrivation(TSServerInfoModule module, String state) {
			//nothing to do, since it's a fake continue iterator
		}
		
		public void ignoreBase(){
			checkBase = false;
		}
		public void ignorePrior() {
			checkPrior = false;
		}
	}
	
	@Override
	public void addDocumentAdditionalPostProcessing(DocumentI doc, ServerResponse response) {
		super.addDocumentAdditionalPostProcessing(doc, response);
		String searchIdSourceAsString = doc.getDocno();
		long searchIdSource = Long.parseLong(searchIdSourceAsString);
		Search searchSource = SearchManager.getSearchFromDisk(searchIdSource);
        if(searchSource != null && doc instanceof PriorFileAtsDocumentI) {
        	Date certificationDate = searchSource.getSa().getCertificationDate().getDate();
        	Search search = getSearch();
        	if(certificationDate != null) {
        		((PriorFileAtsDocumentI)doc).setRecordedDate(certificationDate);
//        		((PriorFileAtsDocumentI)doc).updateStartViewDateAndReturn(search.getDocManager());
        	}
			if(isParentSite()) {
				try {
					search.mergeSearchWith(searchSource, "document " + doc.shortPrint());
				} catch (Exception e) {
					logger.error("Cannot mergeSearch " + searchId + " With " + searchIdSourceAsString, e);
				}
			}
        }
	}
}

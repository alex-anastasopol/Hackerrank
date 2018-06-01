package ro.cst.tsearch.servers.types;

import java.io.File;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.tags.TextareaTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.LinkParser;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFromDocumentFilterForNext;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.misc.NoImageNoExceptionFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ParcelIdIterator;
import ro.cst.tsearch.search.iterator.PlatBookPageIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
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
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StreetNameCorrespondences;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.PriorFileDocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.warning.Warning;
import com.stewart.ats.base.warning.WarningManager;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;


public class XXStewartPriorPF extends TSServer implements TSServerROLikeI {

    static final long serialVersionUID = 10000000;
    private static final Pattern INSTRUMENTNO_ALREADY_FOLLOWED = Pattern.compile("policyToSave=(\\w[\\d-]+)");
    private static final String FIELD_SEPARATOR = "____@@";
    protected static final String PREVIOUSLY_INVALIDATED_RESPONSES = "PREVIOUSLY_INVALIDATED_RESPONSES";
    protected boolean treatAllDocumentsTheSame = false;
    
    public static final int DOCUMENT_SIZE_LIMIT = 3;
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
    
    public XXStewartPriorPF(long searchId) {
    	super(searchId);
        resultType = MULTIPLE_RESULT_TYPE;
    }
    
    public XXStewartPriorPF(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
    }
    
    @Override
    public void setServerID(int serverID) {
    	super.setServerID(serverID);
    	String stateAbbreviation = getDataSite().getStateAbbreviation();
    	if("CO".equals(stateAbbreviation) || "AK".equals(stateAbbreviation)) {
    		treatAllDocumentsTheSame = true;
    	}
    }

    public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.GENERIC_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.setData(10, getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
			tsServerInfoModule.setData(11, getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
			tsServerInfoModule.setDefaultValue(10, getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
			tsServerInfoModule.setDefaultValue(11, getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
			
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
				
			}
			
			ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
			String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();
			
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(
							siteName, TSServerInfo.GENERIC_MODULE_IDX, nameToIndex.get(functionName));
					if(comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
			
			
			
			
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if(tsServerInfoModule != null) {
			tsServerInfoModule.setData(10, getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
			tsServerInfoModule.setData(11, getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
			tsServerInfoModule.setDefaultValue(10, getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
			tsServerInfoModule.setDefaultValue(11, getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
		}
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}    
    

    protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

        List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

        Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
        
	        TSServerInfoModule m = null;
	        GenericLegal defaultLegalFilter = (GenericLegal) LegalFilterFactory.getDefaultLegalFilter(searchId);
	        defaultLegalFilter.setEnableLotUnitFullEquivalence(true);
	        GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
	        FilterResponse cityFilter 		= CityFilterFactory.getCityFilter(searchId, 0.6d);
	        addressFilter.setEnableUnit(false);
	        addressFilter.setTryAddressFromDocument(true);
	        
	        DocsValidator rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId).getValidator();
	        DocsValidator rejectNoImageNoExceptionDocuments = new NoImageNoExceptionFilter(searchId).getValidator();
	        DocsValidator pinValidator = PINFilterFactory.getDefaultPinFilter(searchId).getValidator();
	        
	        //parcel id search
	        {
			    m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			    m.clearSaKeys();
				m.forceValue(10, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
				m.forceValue(11, global.getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
				m.setIteratorType(14, FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE);
				m.addFilter(cityFilter);
				m.addValidator(rejectSavedDocuments);
				m.addValidator(rejectNoImageNoExceptionDocuments);
				m.addValidator(pinValidator);
				m.addValidator(addressFilter.getValidator());
				m.addValidator(defaultLegalFilter.getValidator());
				ParcelIdIterator it = new ParcelIdIterator(searchId);
				it.setCheckDocumentSource("PF");
				m.addIterator(it);
				l.add(m);
	        }
	        
	        
			// plat book - page search
	        {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				m.clearSaKeys();
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_PLATBOOK_PLATPAGE);
		    	m.forceValue(10, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
				m.forceValue(11, global.getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
				m.setIteratorType(23, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				m.setIteratorType(24, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				m.addFilter(cityFilter);
		    	m.addValidator(rejectSavedDocuments);
		    	m.addValidator(rejectNoImageNoExceptionDocuments);
		    	m.addValidator(pinValidator);
		    	m.addValidator(addressFilter.getValidator());
		    	m.addValidator(defaultLegalFilter.getValidator());
		    	PlatBookPageIterator iterator = new PlatBookPageIterator(searchId);
		    	m.addIterator(iterator);
		    	l.add(m);
	        }
	        
	        // plat volume (book) - page search
	        {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
				m.clearSaKeys();
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_PLATVOLUME_PLATPAGE);
		    	m.forceValue(10, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
				m.forceValue(11, global.getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
				m.setIteratorType(22, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				m.setIteratorType(24, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				m.addFilter(cityFilter);
		    	m.addValidator(rejectSavedDocuments);
		    	m.addValidator(rejectNoImageNoExceptionDocuments);
		    	m.addValidator(pinValidator);
		    	m.addValidator(addressFilter.getValidator());
		    	m.addValidator(defaultLegalFilter.getValidator());
		    	PlatBookPageIterator iterator = new PlatBookPageIterator(searchId);
		    	m.addIterator(iterator);
		    	l.add(m);
	        }
			
			if(!StringUtils.isEmpty(getSearchAttributes().getAtribute( SearchAttributes.LD_SUBDIV_NAME ))) {
				
				GenericNameFilter legalFilterResponse = (GenericNameFilter)NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
				legalFilterResponse.setThreshold(new BigDecimal(RECOVER_FILTER_SUBDIVISION_NAME_LIMIT));
				legalFilterResponse.init();
				
	        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
	        	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	        			TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK);
	        	m.clearSaKeys();
	        	m.forceValue(10, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
				m.forceValue(11, global.getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
	        	m.setSaKey(15, SearchAttributes.LD_SUBDIV_NAME);
	        	m.setSaKey(16, SearchAttributes.LD_LOTNO);
	        	m.setSaKey(17, SearchAttributes.LD_SUBDIV_BLOCK);
	        	//m.setSaKey(19, SearchAttributes.LD_SUBDIV_UNIT);
	        	m.addFilter(cityFilter);
	        	m.addValidator(legalFilterResponse.getValidator());
	        	m.addValidator(rejectSavedDocuments);
	        	m.addValidator(rejectNoImageNoExceptionDocuments);
	        	m.addValidator(pinValidator);
	        	m.addValidator(addressFilter.getValidator());
	        	m.addValidator(defaultLegalFilter.getValidator());
	        	l.add(m);
	        }
	        
	        if(hasStreet()) {
	        	
	        	AddressFromDocumentFilterForNext addressFilterForNext = new AddressFromDocumentFilterForNext(
	        			global.getSa().getAtribute(SearchAttributes.P_STREET_NO_NAME),searchId);
	        	addressFilterForNext.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
	        	addressFilterForNext.setThreshold(new BigDecimal(0.77));
	        	
	        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
	        	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_RESAVE_DOCS, Boolean.TRUE);
	        	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	        			TSServerInfoConstants.VALUE_PARAM_ADDRESS_NO_NAME);
	        	m.clearSaKeys();
	        	m.forceValue(10, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
				m.forceValue(11, global.getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
	        	m.setSaKey(12, SearchAttributes.P_STREET_NO_NAME);
	        	m.addFilterForNext(addressFilterForNext);
	        	m.addFilter(cityFilter);
	        	m.addValidator(rejectSavedDocuments);
	        	m.addValidator(rejectNoImageNoExceptionDocuments);
	        	m.addValidator(pinValidator);
	        	m.addValidator(addressFilter.getValidator());
	        	m.addValidator(defaultLegalFilter.getValidator());
	        	l.add(m);
	        	
	        	String streetName = global.getSa().getAtribute(SearchAttributes.P_STREETNAME);
	        	String streetNo = global.getSa().getAtribute(SearchAttributes.P_STREETNO);
	        	if (StreetNameCorrespondences.getInstance(searchId).hasCorrespondence(streetName)){
	        		
	        		String streetCorresp = StreetNameCorrespondences.getInstance(searchId).getCorrespondent(streetName);
	        		
	        		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
	        		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_RESAVE_DOCS, Boolean.TRUE);
	        		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	            			TSServerInfoConstants.VALUE_PARAM_ADDRESS_NO_NAME);
	        		m.clearSaKeys();
	            	m.forceValue(10, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
	    			m.forceValue(11, global.getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
	            	m.getFunction(12).forceValue(streetNo + " " + streetCorresp);
	            	m.addFilterForNext(addressFilterForNext);
	            	m.addFilter(cityFilter);
	            	m.addValidator(rejectSavedDocuments);
	            	m.addValidator(rejectNoImageNoExceptionDocuments);
	            	m.addValidator(pinValidator);
	            	m.addValidator(addressFilter.getValidator());
	            	m.addValidator(defaultLegalFilter.getValidator());
	            	l.add(m);
	        	}
	        }
	        
	        //B4428: 1. Do not perform name search in automatic
	        /*	
	        //name modules with names from search page.
	    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
	    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.forceValue(10, getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
			m.forceValue(11, getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
			m.addFilter(cityFilter);
			m.addFilter(addressFilter);
			m.addValidator(rejectSavedDocuments);
			m.addValidator(defaultLegalFilter.getValidator());
			m.addValidator(NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m).getValidator());
			m.setIteratorType(27, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableSkipModuleNameIterator(searchId, new String[] {"L;F;", "L;f;"}, "PF");
			m.addIterator(nameIterator);
			l.add(m);
			*/
		}
        serverInfo.setModulesForAutoSearch(l);

    }

    protected void setModulesForGoBackOneLevelSearch(
            TSServerInfo serverInfo) {
        List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
        

        serverInfo.setModulesForGoBackOneLevelSearch(l);
    }

    protected void ParseResponse(String sAction, ServerResponse Response,
            int viParseID) throws ServerResponseException {
    	String rsResponce = Response.getResult();
    	ParsedResponse parsedResponse = Response.getParsedResponse();
    	switch(viParseID){
    		case ID_SEARCH_BY_PARCEL:
    			StringBuilder outputTable = new StringBuilder();
				
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response,
						rsResponce, outputTable);
				if(smartParsedResponses != null && smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					
		            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();                           	
		            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
		            	footer = "\n</table>";
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
					
					
				} else {
					parsedResponse.setError("<font color=\"red\">No results found.</font> Please try again.");
					return;
				}
    			break;
    		
    			
    		case ID_GET_IMAGE:
    			if(rsResponce.contains("Image is currently unavailable")) {
    				parsedResponse.setError("<font color=\"red\">Image is currently unavailable.</font> Please try again.");
    				parsedResponse.setFooter("");
    			}
				return;
    			
    		case ID_DETAILS:
    		case ID_SAVE_TO_TSD:
    		case ID_GET_LINK:
    			if(rsResponce.contains("Invalid Operation")) {
		    		parsedResponse.setError("<font color=\"red\">Invalid Operation.</font>  Please search again.");
					return;
    			}
    			String contents = getDetailedContent(rsResponce, sAction, Response.getRawQuerry());
    			contents = "<table border=\"0\" align=\"center\" cellspacing=\"0\" cellpadding=\"0\"><tr><td>" +					  
			           contents +
			           "</td></tr></table>"; 
			           
			           
			           
    			parsedResponse.setResponse(contents);
    			String keyCode = getFileNameFromLink(Response.getQuerry() + "&");
    			
    			if(viParseID == ID_SAVE_TO_TSD) {
    				 msSaveToTSDFileName = keyCode + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = "<form>" + contents + CreateFileAlreadyInTSD();                
	                smartParseDetails(Response,contents);
    			} else {
    				String originalLink = sAction + "&dummy=" + keyCode + "&" + "shortened=true"; //Response.getQuerry();
                    String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
                    
                	contents = addSaveToTsdButton(contents, sSave2TSDLink, ID_DETAILS);
                    mSearch.addInMemoryDoc(sSave2TSDLink, contents);
                    Response.getParsedResponse().setResponse(contents);
                    Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
    			}
    			
    			break;
    			
    	}

    }
    
    private String getDetailedContent(String rsResponce, String action, String extraQuery) {
		if(!rsResponce.contains("<!DOCTYPE")) {
			return rsResponce;
		}
		try {
			Parser parser = Parser.createParser(rsResponce, null);
			NodeList nodeList = parser.parse(null);
			NodeList interestingColumnsList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("td"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("style","width: 75%; vertical-align: top; padding-left: 35px;"));
			if(interestingColumnsList.size() > 0) {
				
				NodeList fileNumber = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"),true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_ContentPlaceHolder1_lblFileNumber"));
				String agentFileNo = "";
				if(fileNumber.size() == 1) {
					agentFileNo = fileNumber.elementAt(0).toPlainTextString().trim();
				}
				
				TableColumn column = (TableColumn)interestingColumnsList.elementAt(0);
				TableTag firstTable = null;
				for (int i = 0; i < column.getChildCount(); i++) {
					if(firstTable == null && column.getChild(i) instanceof TableTag) {
						firstTable = (TableTag)column.getChild(i);
						
					}
					if (column.getChild(i) instanceof Div) {
						column.removeChild(i);
						break;
					}
				}
				List<InputTag> hiddenFields = new Vector<InputTag>();
				if(firstTable != null) {
					for (int i = 1; i < firstTable.getRowCount(); i++) {
						TableRow tableRow = firstTable.getRow(i);
						InputTag hiddenDocLink = new InputTag();
						hiddenDocLink.setAttribute("type","hidden");
						hiddenDocLink.setAttribute("name", "docLink");
						if(tableRow.getColumnCount() == 3) {
							TableColumn policyColumn = tableRow.getColumns()[0];
							String tmpPolicyNumber = policyColumn.toPlainTextString().trim();
							hiddenDocLink.setAttribute(
									"value",
									getLinkPrefix(TSConnectionURL.idGET) + action + "&" + extraQuery + "&policyToSave=" + tmpPolicyNumber + "&");
							hiddenFields.add(hiddenDocLink);
							
							
							
							
							NodeList linkToImageList = tableRow.getColumns()[2].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
							
							if(linkToImageList.size() > 0) {
								LinkTag imageLinkTag = (LinkTag)linkToImageList.elementAt(0);
								
								String imageLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.INSTR_NO_MODULE_IDX) + 
									"&tmpPolicyNumber=" + tmpPolicyNumber + "&ats-prefix1-txtFileNumber=" + URLEncoder.encode(agentFileNo, "UTF-8");
								
								if (!ServerConfig.isImageDisablePerCommunity(getDataSite().getSiteTypeInt(),getCommunityId())) {
									imageLinkTag.setLink(imageLink);
								} else {
									imageLinkTag.setText("<span>");
									Tag endTag = Span.class.newInstance();
									endTag.setTagName("/span");
									imageLinkTag.setEndTag(endTag);
								}
								
								
							}
						
							
						}
						
					}
				}
				StringBuilder resultHtml = new StringBuilder(column.getChildrenHTML());
				for (InputTag inputTag : hiddenFields) {
					resultHtml.append("\n").append(inputTag.toHtml());
				}
				
				if(fileNumber.size() == 1) {
					return "<div>Agency File Number:" + fileNumber.elementAt(0).toHtml() + "</div>" + resultHtml.toString();
				}
				return resultHtml.toString();
			}
		} catch (Exception e) {
			logger.error("Error while getting detailed information for SPF",e);
			return null;
		}
		return null;
	}
    
    public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		DocumentI document = null;
		StringBuilder justResponse = new StringBuilder(detailsHtml);
		
		try {
			ResultMap map = new ResultMap();
			String queryCache = StringEscapeUtils.unescapeHtml(URLDecoder.decode(response.getQuerry() + "&","UTF-8"));
			String tmpShortLegal = org.apache.commons.lang.StringUtils.substringBetween(queryCache,"tmpShortLegal=", "&");
			String tmpFullLegal = org.apache.commons.lang.StringUtils.substringBetween(response.getRawQuerry(),"tmpFullLegal=", "&");
			if(tmpFullLegal != null) {
				tmpFullLegal = URLDecoder.decode(tmpFullLegal, "UTF-8");
			}
			String tmpParsedAddress = org.apache.commons.lang.StringUtils.substringBetween(response.getRawQuerry(),"tmpParsedAddress=", "&");
			if(tmpParsedAddress != null) {
				tmpParsedAddress = URLDecoder.decode(URLDecoder.decode(tmpParsedAddress,"UTF-8"),"UTF-8");
			}

			map.put("tmpPolicyToSave", org.apache.commons.lang.StringUtils.substringBetween(
					queryCache,"policyToSave=", "&"));
			
			
			if(tmpShortLegal!=null) {
				tmpShortLegal = URLDecoder.decode(tmpShortLegal,"UTF-8");
				map.put("tmpShortLegal", tmpShortLegal);
			}
			
			if(tmpFullLegal!=null) {
				tmpFullLegal = URLDecoder.decode(tmpFullLegal,"UTF-8");
				map.put("tmpFullLegal", tmpFullLegal);
			}
			
			String imageLink = parseAndFillResultMap(justResponse, map);
			//String tmpAgencyFileNumber = (String)map.get("tmpAgencyFileNumber");
			
			if(StringUtils.isNotEmpty(tmpParsedAddress)) {
				tmpParsedAddress = URLDecoder.decode(tmpParsedAddress,"UTF-8");
				String[] addressParts = (" " + tmpParsedAddress + " ").split(FIELD_SEPARATOR);
				if(addressParts[1].contains("&")) {
					String newStreetName = addressParts[0] + " " + addressParts[1];
					
					if(newStreetName.matches("\\s*(\\d+)\\s*&\\s*(\\d+)\\s+(.+)")) {
						newStreetName = newStreetName.replaceAll("\\s*(\\d+)\\s*&\\s*(\\d+)\\s+(.+)", "$1 $3 & $2 $3");
					}
					String[] newStreets = newStreetName.split("&");
					Vector<PropertyIdentificationSet> allPis = new Vector<PropertyIdentificationSet>();
					
					for (String string : newStreets) {
						PropertyIdentificationSet pis = new PropertyIdentificationSet();
						String address = ro.cst.tsearch.servers.functions.XXStewartPriorPF.cleanAddress(string);
						String[] addressTokens = StringFormats.parseAddress(address);
						pis.setAtribute("StreetNo", addressTokens[0]);
						pis.setAtribute("StreetName", addressTokens[1]);
						pis.setAtribute("City", addressParts[2]);
						pis.setAtribute("Zip", addressParts[3].trim());
						
						allPis.add(pis);
					}
					
					for (int i = 0; i < allPis.size(); i++) {
						PropertyIdentificationSet pis = allPis.get(i);
						if(StringUtils.isEmpty((String)pis.getAtribute("StreetName"))){
							if(i+1 < allPis.size()) {
								PropertyIdentificationSet pisToCopyFrom = allPis.get(i+1);
								pis.setAtribute("StreetName", (String)pisToCopyFrom.getAtribute("StreetName"));
							}
						}
					}
					
					map.put("PropertyIdentificationSet", allPis);
					
				} else {
					map.put("PropertyIdentificationSet.StreetNo", addressParts[0].trim());
					map.put("PropertyIdentificationSet.StreetName", addressParts[1]);
					map.put("PropertyIdentificationSet.City", addressParts[2]);
					map.put("PropertyIdentificationSet.Zip", addressParts[3].trim());
				}
			}
			
			String exceptions = (String)map.get("tmpExceptions");
			
			map.removeTempDef();
			
			Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
			
			document = bridge.importData();
			document.setServerDocType((String)map.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName()));
			document.setSearchType(DocumentI.SearchType.CS);
			document.setNote((String)map.get("OtherInformationSet.Remarks"));
			((PriorFileDocumentI)document).setExceptions(exceptions);
			if(imageLink != null) {
				getSearch().addImagesToDocument(document, imageLink + "&fakeName=name.pdf");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(fillServerResponse) {
			response.getParsedResponse().setOnlyResponse(justResponse.toString());
			if(document!=null) {
				response.getParsedResponse().setDocument(document);
			}
		}
		
		return document;
	}
    
    
    protected String parseAndFillResultMap(StringBuilder detailsHtml, ResultMap map) {

    	map.put("OtherInformationSet.SrcType","PF");
    	String imageLink = null;
    	NodeList nodeList = null;
    	try {
    		Parser parser = Parser.createParser(detailsHtml.toString(), null);
    		nodeList = parser.parse(null);
			
			NodeList fileNumber = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"),true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_ContentPlaceHolder1_lblFileNumber"));
			if(fileNumber.size() == 1) {
				map.put("tmpAgencyFileNumber", fileNumber.elementAt(0).toPlainTextString().trim());
				map.put("OtherInformationSet.Remarks", "Agency File#: " + fileNumber.elementAt(0).toPlainTextString().trim());
			}
			
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"),true);
			NodeList policyInfoTable = tableList.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding","2"));
			if(policyInfoTable != null) {
				TableTag tablePolicy = (TableTag)policyInfoTable.elementAt(0);
				Vector<Integer> toRemoveRows = new Vector<Integer>();
				for (int i = 1; i < tablePolicy.getRowCount(); i++) {
					TableRow tableRow = tablePolicy.getRow(i);
					if(tableRow.getColumnCount() == 3) {
						TableColumn policyColumn = tableRow.getColumns()[0];
						String policyColumnTextPlain = policyColumn.toPlainTextString().trim();
						if(policyColumnTextPlain.equals(map.get("tmpPolicyToSave"))) {
							map.put("SaleDataSet.InstrumentNumber", policyColumnTextPlain);
							map.put("SaleDataSet.RecordedDate", tableRow.getColumns()[1].toPlainTextString().trim());
							NodeList imageList = tableRow.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"),true);
							if(imageList.size() > 0) {
								LinkTag imageLinkTag  = (LinkTag)imageList.elementAt(0);
								
								imageLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.INSTR_NO_MODULE_IDX) + 
									"&tmpPolicyNumber=" + policyColumnTextPlain + "&ats-prefix1-txtFileNumber=" + map.get("tmpAgencyFileNumber");
								imageLinkTag.setLink(imageLink);
								
								
							}
							
							if(policyColumnTextPlain.startsWith("O")){
								map.put("SaleDataSet.DocumentType", "Owner policy");
							} else if(policyColumnTextPlain.startsWith("M")) {
								map.put("SaleDataSet.DocumentType", "Mortgage policy");
							} else if(policyColumnTextPlain.startsWith("U")) {
								map.put("SaleDataSet.DocumentType", "Lender policy");
							}
						} 
					} else {
						toRemoveRows.add(i);
					}
				}
				for (int i = 0; i < toRemoveRows.size(); i++) {
					tablePolicy.removeChild(toRemoveRows.elementAt(i));
				}
			}
			NodeList theRestOfTheTables = tableList.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding","0"))
				.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("border")));
			if(theRestOfTheTables.size() > 0) {
				TableTag mainTable = (TableTag)theRestOfTheTables.elementAt(0);
				NodeList realValues = null;
				for (int i = 0; i < mainTable.getRowCount(); i++) {
					TableRow tableRow = mainTable.getRow(i);
					TableColumn tableColumn = tableRow.getColumns()[0];
					if(tableColumn.toPlainTextString().trim().equalsIgnoreCase("Party Information")) {
						i++;
						realValues = mainTable.getRow(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("textarea"),true);
						if(realValues.size() > 0) {
							TextareaTag textArea = (TextareaTag)realValues.elementAt(0);
							TextNode textNode = (TextNode)textArea.getChildren().elementAt(0);
							textNode.setText(StringEscapeUtils.unescapeHtml(StringEscapeUtils.unescapeHtml(
									textArea.toPlainTextString().trim())));
							String rawParty = StringEscapeUtils.unescapeHtml(textArea.toPlainTextString().trim());
							ro.cst.tsearch.servers.functions.XXStewartPriorPF.partyNames(rawParty, map);
						}
					} else if(tableColumn.toPlainTextString().trim().equalsIgnoreCase("Property Tax ID Number")) {
						i++;
						realValues = mainTable.getRow(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("textarea"),true);
						if(realValues.size() > 0) {
							String rawInfo = realValues.elementAt(0).toPlainTextString().trim();
							String[] rawInfoAsRows = rawInfo.split("\n");
							rawInfo = "";
							for (String string : rawInfoAsRows) {
								if(string.trim().matches("\\d[\\d\\w-]+\\d")) {
									rawInfo += "," + string.trim();
								}
							}
							if(rawInfo.length() > 0) {
								rawInfo = rawInfo.substring(1);
							}
							map.put("PropertyIdentificationSet.ParcelID", rawInfo);
						}
					} else if(tableColumn.toPlainTextString().trim().equalsIgnoreCase("Subdivision")) {
						i++;
						realValues = mainTable.getRow(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("textarea"),true);
						if(realValues.size() > 0) {
							String rawInfo = StringEscapeUtils.unescapeHtml(realValues.elementAt(0).toPlainTextString().trim());
							ro.cst.tsearch.servers.functions.XXStewartPriorPF.parseDetailedLegal(rawInfo, map);
						}
					} else if(tableColumn.toPlainTextString().trim().equalsIgnoreCase("Condominium")) {
						i++;
						realValues = mainTable.getRow(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("textarea"),true);
						if(realValues.size() > 0) {
							String rawInfo = realValues.elementAt(0).toPlainTextString().trim();
							ro.cst.tsearch.servers.functions.XXStewartPriorPF.parseCondominium(rawInfo, map);
						}
					} else if(tableColumn.toPlainTextString().trim().equalsIgnoreCase("Exceptions")) {
						i++;
						realValues = mainTable.getRow(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("textarea"),true);
						if(realValues.size() > 0) {
							String rawInfo = realValues.elementAt(0).toPlainTextString().trim();
							map.put("tmpExceptions", rawInfo);
						}
					}
					
				}
			}
			
			NodeList textareaList = nodeList.extractAllNodesThatMatch(new TagNameFilter("textarea"),true);
			for (int indexArea = 0; indexArea < textareaList.size(); indexArea++) {
				TextareaTag textareaTag = (TextareaTag)textareaList.elementAt(indexArea);
				//textareaTag.removeAttribute("cols");
				//textareaTag.setAttribute("style", "width: 100%;");
				textareaTag.setAttribute("cols", "100");
			}
			
			detailsHtml.delete(0, detailsHtml.length());
	    	detailsHtml.append(nodeList.toHtml());
    	} catch (Exception e) {
			logger.error(getSearch().getID() + ": Error while parsing XXStewartPriorPF detail page ", e);
		}
    	
		return imageLink;
    }

	@Override
    public Collection<ParsedResponse> smartParseIntermediary(ServerResponse serverResponse, String rsResponse,
    		StringBuilder newTable) {
    	Collection<ParsedResponse> responses = new Vector<ParsedResponse>();

		String linkPrefix = getLinkPrefix(TSConnectionURL.idGET);
		try {
			Parser parser = Parser.createParser(rsResponse, null);
			NodeList nodeList = parser.parse(null);
			NodeList spanList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true);
			NodeList succesNodes = spanList.extractAllNodesThatMatch(
					new HasAttributeFilter("id","ctl00_ContentPlaceHolder1_informationBox_lblTextMessage1"));
			if(succesNodes.size() == 0 || 
					!(succesNodes.toHtml().contains("Search was successful. Results were found.") || succesNodes.toHtml().contains("Results found."))) {
				return null;
			}
			NodeList resultTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_ContentPlaceHolder1_grdSearchResults"));
			TableRow[] trList = null;
			if(resultTableList.size() > 0) {
				TableTag resultTable = (TableTag)resultTableList.elementAt(0);
				trList = resultTable.getRows();
			}
				
			if(trList == null || trList.length == 0) {
				return null;
			}
			newTable.append("<table BORDER='1' CELLPADDING='2'>\n");
			String rowAsString = null;
			for (int i = 0; i < trList.length; i++) {
				ResultMap resultMap = new ResultMap();
				ParsedResponse currentResponse = new ParsedResponse();
				TableRow row = trList[i];
				
				
				NodeList tablesInRow = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("td")).elementAt(0).getChildren()
					.extractAllNodesThatMatch(new TagNameFilter("table"));
				NodeList tdAddress = tablesInRow.extractAllNodesThatMatch(new HasAttributeFilter(
							"id"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"),true)
					.extractAllNodesThatMatch(new TagNameFilter("td"),true);
				
				if(i == trList.length - 1 && tablesInRow.size() == 1) {
					TableTag pageLinksTableList = (TableTag)tablesInRow.elementAt(0);
					String formatTemplate = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.NEXT_LINK_MODULE_IDX) + 
						"&__EVENTTARGET={0}&ctl00$ContentPlaceHolder1$ScriptManager1=ctl00$ContentPlaceHolder1$DisplaySection_SearchResults|{0}";
					
					
					
					NodeList pageLinksList = pageLinksTableList.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"),true);
					boolean foundSelected = false;
					for (int j = 0; j < pageLinksList.size(); j++) {
						LinkTag pageLink = (LinkTag)pageLinksList.elementAt(j);
						String controler = org.apache.commons.lang.StringUtils.substringBetween(pageLink.getLink(), "'", "'");
						pageLink.setLink(
								MessageFormat.format(formatTemplate, controler));
						if(foundSelected) {
							if (j != pageLinksList.size() - 1) {
								serverResponse.getParsedResponse().setNextLink("<a href='" + pageLink.getLink() + "' />");
								foundSelected = false;
							}
						}
						String style = pageLink.getAttribute("style");
						if(StringUtils.isNotEmpty(style) && style.contains("color:Red;")) {
							foundSelected = true;
						}
							
					}
					
					Map<String, String> hiddenParams = HttpSite.fillAndValidateConnectionParams(
							rsResponse, 
							ro.cst.tsearch.connection.http2.XXStewartPriorPF.REQ_PARAM_NAMES_SEARCH, 
							ro.cst.tsearch.connection.http2.XXStewartPriorPF.FORM_NAME);
					
					getSearch().setAdditionalInfo(
							ro.cst.tsearch.connection.http2.XXStewartPriorPF.PARAMETERS_NAVIGATION_LINK, 
							hiddenParams);
					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
						rowAsString = row.toHtml();
						currentResponse.setOnlyResponse(rowAsString);
						currentResponse.setAttribute(ParsedResponse.SERVER_NAVIGATION_LINK, true);
						responses.add (currentResponse);
						newTable.append(rowAsString);
					}
				} else {
				
					String tmpParsedAddress = null; 
					String shortLegal = null;
					String fullLegal = null;
					String tmpPolicyToSave = "";
					if(tdAddress.size() == 5) {
						String address = StringEscapeUtils.unescapeHtml(tdAddress.elementAt(0).toPlainTextString());
						if(!address.toLowerCase().contains("property") && !address.toUpperCase().contains("PROPERTIES")) {
							address = ro.cst.tsearch.servers.functions.XXStewartPriorPF.cleanAddress(address);
							String city = tdAddress.elementAt(1).toPlainTextString();
							String countyName = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
							if(city.toLowerCase().contains(countyName.toLowerCase())) {
								if(city.contains(",")) {
									city = city.replaceAll(",.*", "").trim();
								} else if(address.contains(",")) {
									city = address.replaceAll("[^,]*,", "").trim();
									address = address.replaceAll(",.*", "").trim();
								}
								
							}
							
							city = city.replaceAll("(?is)City\\s+of\\s+", "").trim();
							
							String[] addressTokens = StringFormats.parseAddress(address);
							resultMap.put("PropertyIdentificationSet.StreetNo", addressTokens[0]);
							resultMap.put("PropertyIdentificationSet.StreetName", addressTokens[1]);
							resultMap.put("PropertyIdentificationSet.City", city);
							resultMap.put("PropertyIdentificationSet.Zip", tdAddress.elementAt(3).toPlainTextString());
							
							tmpParsedAddress = 
								addressTokens[0] + FIELD_SEPARATOR +
								addressTokens[1] + FIELD_SEPARATOR +
								city + FIELD_SEPARATOR +
								tdAddress.elementAt(3).toPlainTextString().trim();
						} else {
							String city = tdAddress.elementAt(1).toPlainTextString();
							String countyName = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
							if(city.toLowerCase().contains(countyName.toLowerCase())) {
								if(city.contains(",")) {
									city = city.replaceAll(",.*", "").trim();
								}
							}
							city = city.replaceAll("(?is)City\\s+of\\s+", "").trim();
							resultMap.put("PropertyIdentificationSet.City", city);
							resultMap.put("PropertyIdentificationSet.Zip", tdAddress.elementAt(3).toPlainTextString());
							
							tmpParsedAddress = 
								FIELD_SEPARATOR +
								FIELD_SEPARATOR +
								city + FIELD_SEPARATOR +
								tdAddress.elementAt(3).toPlainTextString().trim();
						}
					}
					if(tablesInRow.size() > 2) {
						NodeList shortLegals = tablesInRow.elementAt(2).getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("table"),true)
							.extractAllNodesThatMatch(new TagNameFilter("td"),true);
						if(shortLegals.size() > 0) {
							shortLegal = shortLegals.elementAt(0).toPlainTextString().trim();
						}
					}
					if(StringUtils.isEmpty(shortLegal)) {
						NodeList legalSpans = tablesInRow.extractAllNodesThatMatch(new TagNameFilter("span"),true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_ContentPlaceHolder1_grdSearchResults_ctl03_lblLegalDescription"));
						if(legalSpans.size() > 0) {
							fullLegal = legalSpans.elementAt(0).toPlainTextString().trim();
							if(!StringUtils.isEmpty(fullLegal)) {
								resultMap.put("tmpFullLegal",fullLegal);
							}
						}
					} else {
						ro.cst.tsearch.servers.functions.XXStewartPriorPF.parseShortLegal(shortLegal, resultMap);
						
						resultMap.put("tmpShortLegal",shortLegal);
					}
					NodeList tdAgencyFileNoList = row.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("style","vertical-align:top; text-decoration: underline; font-weight: bold"), true)
						.extractAllNodesThatMatch(new TagNameFilter("td"),true);
					String agentFileNo = null;
					String claimNumber = null;
					if(tdAgencyFileNoList.size() > 0) {
						agentFileNo = tdAgencyFileNoList.elementAt(0).toPlainTextString();
						if(agentFileNo.contains("Agency File Number:")) {
							agentFileNo = agentFileNo.replaceAll("Agency File Number:", "").trim();
						}
					}
					NodeList smallInfoTables = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"),true)
						.extractAllNodesThatMatch(new HasAttributeFilter("style","font-family: Verdana; font-size: x-small"))
						.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("cellspacing")));
					if(smallInfoTables.size() > 0) {
						TableTag smallInfoTable = (TableTag)smallInfoTables.elementAt(0);
						TableRow[] smallInfoRows = smallInfoTable.getRows();
						for (int j = 1; j < smallInfoRows.length; j++) {
							TableColumn[] smallInfoColumns = smallInfoRows[j].getColumns();
							if(smallInfoColumns.length == 5) {
								String tmpPolicyNumber = smallInfoColumns[0].toPlainTextString().trim();
								
								
								NodeList linkToImageList = smallInfoColumns[3]
										.getChildren()
										.extractAllNodesThatMatch(
												new TagNameFilter("a"));
								if (linkToImageList.size() > 0) {
									LinkTag imageLinkTag = (LinkTag) linkToImageList.elementAt(0);

									String imageLink = CreatePartialLink(TSConnectionURL.idGET)
											+ "/Search/" + imageLinkTag.extractLink();
									

									if (!ServerConfig.isImageDisablePerCommunity(getDataSite().getSiteTypeInt(),getCommunityId())) {
										imageLinkTag.setLink(imageLink);
									} else {
										imageLinkTag.setText("<span>");
										Tag endTag = Span.class.newInstance();
										endTag.setTagName("/span");
										imageLinkTag.setEndTag(endTag);
									}
								}
								
								if ( !serverResponse.isParentSiteSearch() ) {
									 tmpPolicyToSave += "&policyToSave=" + tmpPolicyNumber;
								}
								if(StringUtils.isEmpty(claimNumber)) {
									claimNumber = smallInfoColumns[4].toPlainTextString().trim();
								}
								if(StringUtils.isNotEmpty(agentFileNo)) {
									
									if(StringUtils.isNotEmpty(claimNumber)){
										HashMap<String, Warning> allWarnings = WarningManager.getInstance().getAllWarnings();
										Warning warning = allWarnings.get("CLAIM_NUMBER_FOUND");
										if(warning != null) {
											String text = warning.getText();
											text = text.replace("@@claim_number@@", claimNumber);
											text = text.replace("@@agency_number@@", agentFileNo);
											text = text.replace("@@policy_number@@", tmpPolicyNumber);
											getSearch().getSearchFlags().addPermanentWarning(text);
										}
									}
								}
								
							}
						}
					}
					NodeList links = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"),true);

					if(links.size() > 0) {
						LinkTag detailedLink = (LinkTag)links.elementAt(links.size() - 1);
						String newLink = "/Search/" + detailedLink.extractLink() + "&dummy=" + URLEncoder.encode(agentFileNo, "UTF-8");
						if(StringUtils.isNotEmpty(shortLegal)) {
							newLink += "&tmpShortLegal=" + URLEncoder.encode(URLEncoder.encode(shortLegal, "UTF-8"), "UTF-8");
						}
						if(StringUtils.isNotEmpty(fullLegal)) {
							newLink += "&tmpFullLegal=" + URLEncoder.encode(URLEncoder.encode(fullLegal, "UTF-8"), "UTF-8");
						}
						if(StringUtils.isNotEmpty(tmpParsedAddress)){
							newLink += "&tmpParsedAddress=" + URLEncoder.encode(URLEncoder.encode(tmpParsedAddress, "UTF-8"), "UTF-8");
						}
						if(StringUtils.isNotEmpty(tmpPolicyToSave)) {
							newLink += tmpPolicyToSave;
						}
						LinkInPage linkInPage = new LinkInPage(
								linkPrefix + newLink, 
								linkPrefix + newLink, 
		    					TSServer.REQUEST_SAVE_TO_TSD);
						currentResponse.setPageLink(linkInPage);
						detailedLink.removeAttribute("target");
						detailedLink.setLink(CreatePartialLink(TSConnectionURL.idGET) + newLink);
						
					} 
					
					if(StringUtils.isNotEmpty(agentFileNo)) {
						
						resultMap.put("OtherInformationSet.Remarks", "Agency File#: " + agentFileNo);
						rowAsString = row.toHtml();
						resultMap.removeTempDef();
	    				currentResponse.setParentSite(serverResponse.isParentSiteSearch());
						Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
						
						DocumentI document = (RegisterDocumentI)bridge.importData();
						document.setServerDocType((String)resultMap.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName()));
						currentResponse.setDocument(document);
						
						currentResponse.setOnlyResponse(rowAsString);
						responses.add(currentResponse);
						newTable.append(rowAsString);
					}
				
				}
			}
			
			
			newTable.append("</table>\n");
		} catch (Exception e) {
			logger.error("Error while parsing intermediary results!" , e);
		}
		
		return responses;
    	
    }


	protected String getFileNameFromLink(String url) {
        return org.apache.commons.lang.StringUtils.substringBetween(url + "&","dummy=", "&");
    }

	protected String CreateSaveToTSDFormHeader() {
    	return CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
    }
	
	protected String CreateSaveToTSDFormEnd(String name, int parserId, int numberOfUnsavedRows) {
    	
    	if (name == null) {
            name = SAVE_DOCUMENT_BUTTON_LABEL;
        }
        
        String s = "";
        
        
        if( !isRoLike(miServerID, true) && (parserId == ID_DETAILS ||parserId == ID_DETAILS1 ||parserId == ID_DETAILS2 )){
        	if(numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0) {
	        	s = "<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" + onclick=\"javascript:submitFormByGet();\">\r\n";
        	}
            
        }  else {
        	if (numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0) {
	        	s = "<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" " +"onclick=\"javascript:submitForm();\" >\r\n";
        	}
        }
        
        return s+"</form>\n";
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
		return build.toString();
	}
	
	protected DownloadImageResult saveImage(ImageI image)
		throws ServerResponseException {
		ServerResponse serverResponse = null;
		for (int i = 0; i < 3; i++) {
			serverResponse = new ServerResponse();
			try{
				LinkParser linkParser = new LinkParser(image.getLink(0));
				SearchDataWrapper searchDataWrapper = new SearchDataWrapper();
				TSServerInfoModule module = getCurrentClassServerInfo().getModuleForSearch(
						Integer.parseInt(linkParser.getParamValue(URLConnectionReader.PRM_DISPATCHER)), 
						searchDataWrapper);
				
				module.setData(3,linkParser.getParamValue("ats-prefix1-txtFileNumber"));
				module.setData(33, linkParser.getParamValue("tmpPolicyNumber"));
				if(module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
					module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
				}
				searchDataWrapper.setImage(image);
				
				TSInterface  stewartPriorFilesServer = TSServersFactory.GetServerInstance( miServerID, searchId);
				
				serverResponse = stewartPriorFilesServer.SearchBy(module, searchDataWrapper);
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
		if(serverResponse == null) {
			serverResponse = new ServerResponse();
			serverResponse.setImageResult(new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() ));
		}
		return serverResponse.getImageResult();
	}
	
	public Collection<ParsedResponse> getParsedResponsesFromLinkInPage(LinkInPage linkObj,
			int overrideActionType) throws ServerResponseException {
		Collection<ParsedResponse> result = new Vector<ParsedResponse>();
		ServerResponse sr;
		
		if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
			if (overrideActionType != -1) {
	            sr = performLinkInPage(linkObj, overrideActionType);
	        } else {
	            sr = performLinkInPage(linkObj);
	        }
	        
	        if(!sr.getParsedResponse().isError() && linkObj != null) {
	        	mSearch.addValidatedLink(linkObj.getLink().toLowerCase());
	        }
	        
	        // added to 
	        if(sr != null){
	        	result.add(sr.getParsedResponse());
	        }	
		} else {
			/*LinkInPage linkInPage = new LinkInPage(
					linkPrefix + newLink, 
					linkPrefix + newLink, 
					TSServer.REQUEST_SAVE_TO_TSD);*/
			String originalLink = linkObj.getLink() + "&";
			String[] allPoliciesToSave = org.apache.commons.lang.StringUtils.substringsBetween(
					originalLink, "policyToSave=", "&");
			String agencyFileNumber = org.apache.commons.lang.StringUtils.substringBetween(originalLink, "dummy=", "&");
			List<String> policiesToSave = new ArrayList<String>();
			for (String singlePolicy : allPoliciesToSave) {
				if(singlePolicy.startsWith("O-") || treatAllDocumentsTheSame){
					policiesToSave.add(singlePolicy);
				} else {
					SearchLogger.info("<br>Ignoring policy: " + singlePolicy + " from AgencyFile#: " + agencyFileNumber + " because is a mortgage policy<br>", searchId);
				}
			}
			if(policiesToSave.size() == 0) {
				return result;
			} else {
				String prefix = originalLink.replaceAll("policyToSave=[^&]*&", "");
				for (String policyToSave : policiesToSave) {
					LinkInPage newLinkInPage = new LinkInPage(
							prefix + "policyToSave=" + policyToSave + "&",
							prefix + "policyToSave=" + policyToSave + "&",
							linkObj.getActionType());
					if (overrideActionType != -1) {
			            sr = performLinkInPage(newLinkInPage, overrideActionType);
			        } else {
			            sr = performLinkInPage(newLinkInPage);
			        }
			        
			        if(!sr.getParsedResponse().isError() && newLinkInPage != null) {
			        	mSearch.addValidatedLink(newLinkInPage.getLink().toLowerCase());
			        }
			        
			        // added to 
			        if(sr != null){
			        	result.add(sr.getParsedResponse());
			        }	
				}
			}
			
		}
        
        return result;
	}
	
	@Override
	public ServerResponse GetLink(String vsRequest, boolean vbEncoded)
			throws ServerResponseException {
		if(vsRequest.contains("Link=")) {
			return super.GetLink(vsRequest, vbEncoded);
		} else {
			LinkParser linkParser = new LinkParser(vsRequest);
			SearchDataWrapper searchDataWrapper = new SearchDataWrapper();
			TSServerInfoModule module = getCurrentClassServerInfo().getModuleForSearch(
					Integer.parseInt(linkParser.getParamValue(URLConnectionReader.PRM_DISPATCHER)), 
					searchDataWrapper);
			module.setData(0,linkParser.getParamValue("ctl00$ContentPlaceHolder1$ScriptManager1"));
			module.setData(4,linkParser.getParamValue("__EVENTTARGET"));
			setInNextLinkSequence(true);
			setDoNotLogSearch(true);
			try {
				return performAction(REQUEST_SEARCH_BY, vsRequest, module, searchDataWrapper);
			} finally {
				setInNextLinkSequence(false);
				setDoNotLogSearch(false);
			}
		}
	}
	
	@Override
	public String getPrettyFollowedLink(String initialFollowedLnk) {
		String instrNum = getInformationFromResponse(initialFollowedLnk, INSTRUMENTNO_ALREADY_FOLLOWED);
		if (StringUtils.isEmpty(instrNum)){
			return super.getPrettyFollowedLink(initialFollowedLnk);
		} else {
			return "<br/><span class='followed'>Instrument " + instrNum +
			" has already been saved.</span><br/>";
		}
	}
	
	public static String getInformationFromResponse(String response, Pattern pattern){
		Matcher ma = pattern.matcher(response);
		if (ma.find()){
			return ma.group(1);
		}
		return "";
	}
	
	@Override
	protected String preProcessLink(String link){
    	String proc = link.toLowerCase();
    	proc = proc.replaceFirst("fileid=[^&]+&", "");
    	
    	return super.preProcessLink(link);
    }
	
	@Override
	public void performAdditionalProcessingBeforeRunningAutomatic() {
		getSearch().removeAdditionalInfo(PREVIOUSLY_INVALIDATED_RESPONSES); 
		super.performAdditionalProcessingBeforeRunningAutomatic();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void performAdditionalProcessingAfterRunningAutomatic() {
		super.performAdditionalProcessingAfterRunningAutomatic();
		Search search = getSearch();
		DocumentsManagerI documentsManagerI = search.getDocManager();
		try {
			documentsManagerI.getAccess();
			List<DocumentI> allPriorFilesList = documentsManagerI.getDocumentsWithDataSource(false, "PF");
			List<DocumentI> automaticPriorFilesList = new ArrayList<DocumentI>();
			for (DocumentI documentI : allPriorFilesList) {
				if(documentI.isSavedFrom(SavedFromType.AUTOMATIC)){
					automaticPriorFilesList.add(documentI);
				}
			}
			
			if(automaticPriorFilesList.size() < DOCUMENT_SIZE_LIMIT) {
				LinkedHashMap<String,ServerResponse> storedResponses = (LinkedHashMap<String,ServerResponse>)
					getSearch().getAdditionalInfo(PREVIOUSLY_INVALIDATED_RESPONSES); 
				if(storedResponses != null) {
					Collection<ServerResponse> failedResponses = storedResponses.values();
					
					FilterResponse noImageNoFilterResponse = new NoImageNoExceptionFilter(search.getID());
					FilterResponse rejectSavedDocumentsFilterResponse = new RejectAlreadySavedDocumentsFilterResponse(searchId);
					DocsValidator pinValidator = PINFilterFactory.getDefaultPinFilter(search.getID()).getValidator();
					
					GenericAddressFilter streetFilter = new GenericAddressFilter(searchId);
					streetFilter.setMiServerID(miServerID);
					streetFilter.disableAll();
					streetFilter.setMarkIfCandidatesAreEmpty(true);
					streetFilter.setEnableName(true);
					streetFilter.setThreshold(new BigDecimal(RECOVER_FILTER_STREET_NAME_LIMIT));
					streetFilter.setTryAddressFromDocument(true);
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
					for (ServerResponse serverResponse : failedResponses) {
						
						
						ParsedResponse parserRespons = serverResponse.getParsedResponse();
						
						if(rejectSavedDocumentsFilterResponse.getScoreOneRow(serverResponse.getParsedResponse()) == ATSDecimalNumberFormat.ONE &&
								noImageNoFilterResponse.getScoreOneRow(serverResponse.getParsedResponse()) == ATSDecimalNumberFormat.ONE &&
								pinValidator.isValid(serverResponse)) {
							
							toLog.append("<br>")
								.append("Analyzing invalidated document " + serverResponse.getParsedResponse().getDocument().getInstno())
								.append("<br>");
							
						
							double scoreStreetName = 0;
							double scoreSubdivisionName = 0;
							double scoreLot = 0;
							double scoreBlock = 0;
							double scoreStreetNumber = 0;
							BigDecimal tempDecimal = null;
							
							tempDecimal = streetFilter.getScoreOneRow(parserRespons);
							if(tempDecimal == ATSDecimalNumberFormat.NA) {
								scoreStreetName = streetFilter.getThreshold().doubleValue() - 0.01;
								toLog.append("Street Name matching uncertain - missing information(default score used:" +
										scoreStreetName + 
										")<br>");
							} else {
								scoreStreetName = tempDecimal.doubleValue();
								toLog.append("Street Name matching score = " + ATSDecimalNumberFormat.format(tempDecimal)).append("<br>");
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
									append(scoreSubdivisionName + ")<br>");
							} else {
								scoreSubdivisionName = tempDecimal.doubleValue();
								toLog.append("Subdivision Name matching score = " + ATSDecimalNumberFormat.format(tempDecimal)).append("<br>");
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
									toLog.append("Lot matching uncertain - missing information(default score used:" + scoreLot + ")<br>");
								} else {
									scoreLot = tempDecimal.doubleValue();
									toLog.append("Lot matching score = " + ATSDecimalNumberFormat.format(tempDecimal)).
										append("<br>");
								}

								tempDecimal = blockFilter.getScoreOneRow(parserRespons);
								if(tempDecimal == ATSDecimalNumberFormat.NA) {
									scoreBlock = blockFilter.getThreshold().doubleValue() - 0.01;
									toLog.append("Block matching uncertain - missing information(default score used:" + scoreBlock + ")<br>");
								} else {
									scoreBlock = tempDecimal.doubleValue();
									toLog.append("Block matching score = " + ATSDecimalNumberFormat.format(tempDecimal)).
										append("<br>");
								}
							
								tempDecimal = numberFilter.getScoreOneRow(parserRespons);
								if(tempDecimal == ATSDecimalNumberFormat.NA) {
									scoreStreetNumber = numberFilter.getThreshold().doubleValue() - 0.01;
									toLog.append("Street Number matching uncertain - missing information(default score used:" + scoreStreetNumber + ")<br>");
								} else {
									scoreStreetNumber = tempDecimal.doubleValue();
									toLog.append("Street Number matching score = " + ATSDecimalNumberFormat.format(tempDecimal)).
										append("<br>");
								}
								
								
								
								finalScore = WEIGHT_STREET_NAME * scoreStreetName + 
									WEIGHT_SUBDIVISION_NAME * scoreSubdivisionName + 
									WEIGHT_LOT * scoreLot + 
									WEIGHT_BLOCK * scoreBlock + 
									WEIGHT_STRET_NO * scoreStreetNumber
									;
							
								serverResponse.setBestScore(new BigDecimal(finalScore/MAX_SCORE));
								
								toLog.append("<br>")
									.append("Final score for document " + serverResponse.getParsedResponse().getDocument().getInstno())
									.append(" is " + ATSDecimalNumberFormat.format(serverResponse.getBestScore()))
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
								boolean o1HasInstrumentNumber = o1!= null && o1.getParsedResponse() != null && o1.getParsedResponse().getInstrumentNumber() !=null;
								boolean o2HasInstrumentNumber = o2!= null && o2.getParsedResponse() != null && o2.getParsedResponse().getInstrumentNumber() !=null;
								if (o1HasInstrumentNumber && o2HasInstrumentNumber){
									String i1 = o1.getParsedResponse().getInstrumentNumber();
									String i2 = o2.getParsedResponse().getInstrumentNumber();
									String stringPrefix = "O";
									if ( i1.startsWith(stringPrefix) && i2.startsWith(stringPrefix)) {score = 0;}
									else{
										if ( i1.startsWith(stringPrefix) && !i2.startsWith(stringPrefix)){ 
											score=-1;
										}else{
											score =1;
										}
									}
								}
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
							SearchLogger.info("<br><span class='saved'>Resaved document <b>" + document.getInstno() + "</b></span> with score " +  
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
		
		LinkedHashMap<String,ServerResponse> storedResponses = (LinkedHashMap<String,ServerResponse>)
			getSearch().getAdditionalInfo(PREVIOUSLY_INVALIDATED_RESPONSES); 
		if(storedResponses == null) {
			storedResponses = new LinkedHashMap<String, ServerResponse>();
			search.setAdditionalInfo(PREVIOUSLY_INVALIDATED_RESPONSES, storedResponses);
		}
		ParsedResponse parsedResponse = response.getParsedResponse();
		if(parsedResponse != null) {
			DocumentI document = parsedResponse.getDocument();
			if(document != null) {
				if(document.getInstno().startsWith("O-") || treatAllDocumentsTheSame) {
					storedResponses.put(document.getInstno(), response);
				}
			}
		}
	}
	
}


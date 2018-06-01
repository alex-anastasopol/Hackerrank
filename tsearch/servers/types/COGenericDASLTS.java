package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.iterator.instrument.InstrumentCOTSInterator;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageTransformation;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.warning.Warning;
import com.stewart.ats.base.warning.WarningInfo;

public class COGenericDASLTS extends GenericDASLTS {

	private static final long serialVersionUID = -4452448341765544186L;

	protected boolean addAoLookUpSearches(TSServerInfo serverInfo,
			List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef,
			long searchId, boolean isUpdate) {

		boolean atLeastOne = false;
		BetweenDatesFilterResponse betweenDatesFilterResponse = BetweenDatesFilterResponse
				.getDefaultIntervalFilter(searchId);

		InstrumentGenericIterator instrumentBPInterator = getInstrumentIterator();
		instrumentBPInterator.enableBookPage();
		TSServerInfoModule m = new TSServerInfoModule(
				serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
		m.clearSaKeys();
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		m.addIterator(instrumentBPInterator);
		if (isUpdate) {
			m.addFilter(betweenDatesFilterResponse);
		}
		if (instrumentBPInterator.createDerrivations().size() > 0) {
			atLeastOne = true;
			modules.add(m);
		}

		InstrumentGenericIterator instrumentInstrumentInterator = getInstrumentIterator();
		instrumentInstrumentInterator.enableInstrumentNumber();
		m = new TSServerInfoModule(
				serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
		m.clearSaKeys();
		m.setIteratorType(0,
				FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		m.addIterator(instrumentInstrumentInterator);
		if (isUpdate) {
			m.addFilter(betweenDatesFilterResponse);
		}
		if (instrumentInstrumentInterator.createDerrivations().size() > 0) {
			atLeastOne = true;
			modules.add(m);
		}

		InstrumentGenericIterator instrumentDocNoInterator = getInstrumentIterator();
		instrumentDocNoInterator.enableDocumentNumber();
		m = new TSServerInfoModule(
				serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_DOCNO);
		m.clearSaKeys();
		m.setIteratorType(0,
				FunctionStatesIterator.ITERATOR_TYPE_DOCNO_LIST_FAKE);
		m.addIterator(instrumentDocNoInterator);
		if (isUpdate) {
			m.addFilter(betweenDatesFilterResponse);
		}
		if (instrumentDocNoInterator.createDerrivations().size() > 0) {
			atLeastOne = true;
			modules.add(m);
		}
		return atLeastOne;

	}
	
	@Override
	public InstrumentGenericIterator getInstrumentIterator() {
		return new InstrumentCOTSInterator(searchId);
	}

	protected String prepareInstrumentNoForCounty(InstrumentI inst) {
		return inst.getInstno().replaceFirst("^0+", "");
	}

	public COGenericDASLTS(long searchId) {
		super(searchId);
	}

	public COGenericDASLTS(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	@Override
	protected TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(
			ServerResponse response, String htmlContent) {
		TSServer.ADD_DOCUMENT_RESULT_TYPES result = super.addDocumentInATS(
				response, htmlContent);
		if (result == TSServer.ADD_DOCUMENT_RESULT_TYPES.ADDED) {
			DocumentI doc = response.getParsedResponse().getDocument();
			String query = response.getQuerry();
			if (query.contains("&saveSomeWithCrossRef=true")) {
				query = query.replaceAll("&saveSomeWithCrossRef=true", "");
				boolean saveWithCrossRef = doc.isOneOf("MORTGAGE", "LIEN", "CCER");
				if (!saveWithCrossRef) {
					query += "&saveWithoutCrossRef=true";
				}
				response.setQuerry(query);
			}
			DownloadImageResult imageResult = null;
			try {
				imageResult = saveImage(ImageTransformation
						.imageToImageLinkInPage(doc.getImage()));
			} catch (ServerResponseException e) {
				e.printStackTrace();
			}

			if (imageResult == null
					|| (!DownloadImageResult.Status.OK.equals(imageResult
							.getStatus()))) {
				DocumentsManagerI docManager = getSearch().getDocManager();
				try {
					docManager.getAccess();
					DocumentI realdoc = docManager.getDocument(doc.getId());
					if (realdoc != null) {
						realdoc.setIncludeImage(false);
						realdoc.setImage(null);
					}
				} finally {
					docManager.releaseAccess();
				}
			}
			
			if (isParentSite()){
				if (StringUtils.isNotEmpty(doc.getInstno())){
					if (response.getQuerry().contains("parentSite=true")){
						TSServerInfoModule module = this.getDefaultServerInfo().getModuleForSearch(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX, new SearchDataWrapper());
						module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
						String instr = doc.getInstno();
						module.forceValue(0, instr);
						
						String tempInstrLink = this.createPartialLink(TSConnectionURL.idPOST, TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX) + 
								"DL___" + instr + "&isSubResult=true";
										
						if(module != null) {
							ParsedResponse prChild = new ParsedResponse();
							LinkInPage linkInPage = new LinkInPage(module, TSServer.REQUEST_SEARCH_BY_REC);
							if(tempInstrLink != null) {
								linkInPage.setOnlyLink(tempInstrLink);
							}
							prChild.setPageLink(linkInPage);
							response.getParsedResponse().addOneResultRowOnly(prChild);
						}
					}
				} else {
					if (StringUtils.isNotEmpty(doc.getBook()) && StringUtils.isNotEmpty(doc.getPage())){
						if (response.getQuerry().contains("parentSite=true")){
							TSServerInfoModule module = this.getDefaultServerInfo().getModuleForSearch(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX, new SearchDataWrapper());
							module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
							String book = doc.getBook();
							String page = doc.getPage();
							
							module.forceValue(2, book);
							module.forceValue(3, page);
							String bp = book + "_" + page;
							String tempBookPageLink = this.createPartialLink(TSConnectionURL.idPOST, TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX) + 
									"DL___" + bp + "&book=" + book + "&page=" + page + "&isSubResult=true";
							if(module != null) {
								ParsedResponse prChild = new ParsedResponse();
								LinkInPage linkInPage = new LinkInPage(module, TSServer.REQUEST_SEARCH_BY_REC);
								if(tempBookPageLink != null) {
									linkInPage.setOnlyLink(tempBookPageLink);
								}
								prChild.setPageLink(linkInPage);
								response.getParsedResponse().addOneResultRowOnly(prChild);
							}
						}
					}
				}
			}
		}
		return result;
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd)
			throws ServerResponseException {

		if ( module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX 
				|| module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if (!modules.isEmpty()) {
				return super.searchByMultipleInstrument(modules, sd, null);
			}
		}
		
		ServerResponse response = super.searchByMultipleInstrument(module, sd);
		if (response!=null) {
			return response;
		}
		
		return super.SearchBy(module, sd);
	}

	@Override
	protected ArrayList<NameI> addNameSearch(List<TSServerInfoModule> modules,
			TSServerInfo serverInfo, String key,
			ArrayList<NameI> searchedNames, FilterResponse... filters) {

		if (getDataSite().getStateAbbreviation().equals("CO")
				&& getDataSite().getCountyName().equals("Pitkin")) {
			ArrayList<FilterResponse> new_filters = new ArrayList<FilterResponse>();

			for (FilterResponse filter : filters) {
				if (!(filter instanceof LastTransferDateFilter)) {
					new_filters.add(filter);
				}
			}

			return super.addNameSearch(modules, serverInfo, key, searchedNames,
					(FilterResponse[]) new_filters
							.toArray(new FilterResponse[] {}));
		}

		return super.addNameSearch(modules, serverInfo, key, searchedNames,
				filters);
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
	protected String CreateSaveToTSDFormEnd(String name, int parserId,
			int numberOfUnsavedRows) {
		if (name == null) {
            name = SAVE_DOCUMENT_BUTTON_LABEL;
        }
    	        
        String s = "";
        
    	if (numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0) {
        	s = "<input  type=\"checkbox\" title=\"Save selected document(s) with cross-references\" " +
        		" onclick=\"javascript: if(document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "'))\r\n " +
        		" if(this.checked) { " +
	        	" document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "').value='" + 
	        			RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITH_CROSSREF +
	        	"' } else { " +
 	        	" document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "').value='" + 
 	        			RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF +
	        	"' } \"> Save with cross-references<br>\r\n" +
	        	"<input  type=\"checkbox\" title=\"Perform related search for mortgages, liens and CCERs\" " +
        		" onclick=\"javascript: if(document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE_COMBINED + "'))\r\n " +
        		" if(this.checked) { " +
	        	" document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE_COMBINED + "').value='" + 
	        			RequestParamsValues.PARENT_SITE_SAVE_TYPE_COMBINED_CROSSREF +
	        	"' } else { " +
 	        	" document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE_COMBINED + "').value='" + 
 	        			RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF +
	        	"' } \"> Perform related search<br>\r\n" +		
	        	"<input type=\"checkbox\" name=\"" + RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS + 
	        			"\" id=\"" + RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS + 
	        			"\" title=\"Save search parameters from selected document(s) for further use\" > Save with search parameters<br>\r\n" + 
        		"<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" " +"onclick=\"javascript:submitForm();\" >\r\n";
    	}
        
        
        return s+"</form>\n";
	}
	
	@Override
	public void performAdditionalProcessingAfterRunningAutomatic() {
		super.performAdditionalProcessingAfterRunningAutomatic();
		
		Search search = getSearch();
		@SuppressWarnings("unchecked")
		Set<Integer> additionalInfo = (Set<Integer>) search.getAdditionalInfo(AdditionalInfoKeys.PERFORMED_WITH_NO_ERROR_MODULE_ID_SET);
		if(additionalInfo != null) {
			
			boolean legalPerformed = false;
			int[] moduleIds = new int[]{TSServerInfo.SUBDIVISION_MODULE_IDX};
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
		
		
	}

}

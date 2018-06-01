package ro.cst.tsearch.servers.types;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.LinkParser;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsForUpdateFilter;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.ParcelIdIterator;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.functions.GenericDTSFunctionsRO;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author mihaib
 * 
 */
@SuppressWarnings("deprecation")
public class GenericDTSServerRO extends TSServerROLike implements TSServerROLikeI {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final static String SEARCH_PATH = "/fr1/SimpleQuery.asp"; // path
	
	private static final Category logger = Logger.getLogger(GenericDTSServerRO.class);

	private static final Pattern iPattern = Pattern
			.compile("(?is)<i>(?:Bkwd|Fwd)</i>\\s*<a\\s+href\\s*=\\s*'([^']+)'[^>]*>([^<]*)<");

	private static final Pattern certDatePattern = Pattern.compile("(?ism)The Data is Current Thru:</SPAN>(.*?)</font>");

	//private static String[] docTypeForPlats = {"CCR", "COR", "DDC", "DEA", "DEC", "DIS", "EAS", "ESE", "MAP", "PLAT", "PSUV", "VAC"};
	
	public GenericDTSServerRO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		Search global = getSearch();
		int searchType = global.getSearchType();

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
			TSServerInfoModule module = null;
			
			SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
			
			PinFilterResponse  pinFilter = new PinFilterResponse(SearchAttributes.LD_PARCELNO, searchId){

				private static final long serialVersionUID = -5980159941802276097L;
				
				@Override
				protected Set<String> prepareCandPin(Set<String> candPins){
					Set<String> output = new HashSet<String>();
					for(String pin: candPins){
						pin = pin.trim().toLowerCase();
						output.add(pin);
						if(pin.endsWith("-0000") && pin.length() == 21){
							output.add(pin.replaceAll("(\\d{2}-\\d{2}-\\d{2}-\\d{3})-(\\d{3})-(\\d{4})", "$1"));;
						}
					}
					return output;
				}
				
				@Override
			    public String getFilterName(){
			    	return "Filter by PIN; If candidate pin ends with -0000, then the candidate will contains only first 9 digits. ";
			    }
				
			};
			pinFilter.setStartWith(true);
			pinFilter.setIgNoreZeroes(false);
			
			GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
			FilterResponse cityFilter 		= CityFilterFactory.getCityFilter(searchId, 0.6d);
	        GenericLegal genericLegalFilter = new GenericLegal(searchId);
	        //genericLegalFilter.setEnableSection(false);
			
	        DocsValidator genericLegalValidator = genericLegalFilter.getValidator();
	        DocsValidator[] docsValidators = new DocsValidator[]{pinFilter.getValidator(), addressFilter.getValidator(), 
	        											cityFilter.getValidator(), genericLegalValidator};
	        
	        FilterResponse subdivisionNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
	        subdivisionNameFilter.setThreshold(new BigDecimal(0.90d));
	        
	        FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
			        
			//parcel id search
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIteratorType(18, FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE);
				module.setParamValue(3, "ADDRESS");
				for (DocsValidator docsValidator : docsValidators) {
					module.addValidator(docsValidator);
				}
				
				for (DocsValidator docsValidator : docsValidators) {
					module.addCrossRefValidator(docsValidator);
				}
				ParcelIdIterator it = new ParcelIdIterator(searchId){
					private static final long serialVersionUID = 2750089650252801002L;
	
					@Override
					protected List<String> createDerrivations() {
						List<String> derivationList = super.createDerrivations();
						for (int i = 0; i < derivationList.size(); i++){
							if (derivationList.get(i).contains("-")){
								derivationList.set(i, derivationList.get(i).replaceAll("-0{4}", ""));
							}
						}
						for (int i = 0; i < derivationList.size(); i++){
							if (!derivationList.get(i).contains("-")){
								derivationList.remove(i);
							}
						}
						
						return derivationList;
					}
					@Override
					protected String preparePin(String pin) {
						if (pin.contains("-")){
							return super.preparePin(pin);
						}
						if (pin.length() == 12){
							pin = pin.replaceAll("(\\d{2})(\\d{2})(\\d{2})(\\d{3})(\\d{3})", "$1-$2-$3-$4-$5");
						} else if (pin.length() == 16){
							pin = pin.replaceAll("(\\d{2})(\\d{2})(\\d{2})(\\d{3})(\\d{3})(\\d{4})", "$1-$2-$3-$4-$5-$6");
						}
						return pin;
					}
				};
				module.addIterator(it);
				modules.add(module);
	        }
			
			//address search
			if (hasStreet() && hasStreetNo()){
				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setParamValue(6, sa.getAtribute(SearchAttributes.P_STREETNO));
				module.setParamValue(9, sa.getAtribute(SearchAttributes.P_STREETNAME));
				module.setParamValue(12, sa.getAtribute(SearchAttributes.P_CITY));
				module.setParamValue(3, "ADDRESS");
				
				for (DocsValidator docsValidator : docsValidators) {
					if (!docsValidator.getValidatorName().contains("PIN")){
						module.addValidator(docsValidator);
					}
				}
				
				for (DocsValidator docsValidator : docsValidators) {
					if (!docsValidator.getValidatorName().contains("PIN")){
						module.addCrossRefValidator(docsValidator);
					}
				}
	
				modules.add(module);
	        }
			
			//legal description search
			
			{
				addLegalSearchModuleSLB(serverInfo, modules, TSServerInfo.NAME_MODULE_IDX, searchId, docsValidators);
				addLegalSearchModuleSLU(serverInfo, modules, TSServerInfo.NAME_MODULE_IDX, searchId, docsValidators);
				addLegalSearchModuleSTR(serverInfo, modules, TSServerInfo.NAME_MODULE_IDX, searchId, docsValidators);
			}
			
			//owner search
			ConfigurableNameIterator nameIterator = null;
			FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module);

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
			    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);				
			((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
			((GenericNameFilter) defaultNameFilter).setUseArrangements(false);
			((GenericNameFilter) defaultNameFilter).setInitAgain(true);
			module.addFilter(rejectSavedDocuments);
			module.addFilter(defaultNameFilter);
			if (isUpdate()) {
				module.addFilter(new RejectAlreadySavedDocumentsForUpdateFilter(searchId));
			}
			
			for (DocsValidator docsValidator : docsValidators) {
				module.addValidator(docsValidator);
			}
			
			for (DocsValidator docsValidator : docsValidators) {
				module.addCrossRefValidator(docsValidator);
			}
			
			module.addValidator((new LastTransferDateFilter(searchId)).getValidator());
			
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);	
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId, new String[] {"L f;;"});
			module.addIterator(nameIterator);
			modules.add(module);
			
			
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			    module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH );
			    module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
			    addBetweenDateTest(module, false, false, false);
				modules.add(module);
				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			    
				//module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			    module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH );
			    module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH );
			    module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
			    
			    OcrOrBootStraperIterator ocrBPIteratoriterator = new OcrOrBootStraperIterator(searchId) {
			    	private static final long serialVersionUID = 1L;
			    	
			    	@Override
			    	public List<Instrument> extractInstrumentNoList(TSServerInfoModule initial) {
			    		List<Instrument> extractInstrumentNoList = super.extractInstrumentNoList(initial);
			    		
			    		for (Instrument instrument : extractInstrumentNoList) {
							String bookNo = instrument.getBookNo();
							if(StringUtils.isNotEmpty(bookNo) && !bookNo.startsWith("PB")){
								instrument.setBookNo("PB" + bookNo);
							}
							String pageNo = instrument.getPageNo();
							if(StringUtils.isNotEmpty(pageNo) && !pageNo.startsWith("P")) {
								instrument.setPageNo("P" + pageNo);
							}
						}
			    		setSearchIfPresent(false);
			    		return extractInstrumentNoList;
			    	}
			    	
			    };
			    
			    ocrBPIteratoriterator.setInitAgain(true);
		    	module.addIterator(ocrBPIteratoriterator);
			    
			    addBetweenDateTest(module, false, false, false);
				modules.add(module);
			}
			
			{	
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);				
				((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) defaultNameFilter).setUseArrangements(false);
				((GenericNameFilter) defaultNameFilter).setInitAgain(true);
				module.addFilter(rejectSavedDocuments);
				module.addFilter(defaultNameFilter);
				if (isUpdate()) {
					module.addFilter(new RejectAlreadySavedDocumentsForUpdateFilter(searchId));
				}
				
				for (DocsValidator docsValidator : docsValidators) {
					module.addValidator(docsValidator);
				}
				
				for (DocsValidator docsValidator : docsValidators) {
					module.addCrossRefValidator(docsValidator);
				}
				
				module.addValidator((new LastTransferDateFilter(searchId)).getValidator());
				
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);	
				
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
							.getConfigurableNameIterator(module, searchId, new String[] {"L f;;"});
				
				ArrayList<NameI> searchedNames = null;
				if (nameIterator != null) {
					searchedNames = nameIterator.getSearchedNames();
				} else {
					searchedNames = new ArrayList<NameI>();
				}
				// get your values at runtime
				nameIterator.setInitAgain(true);
				nameIterator.setSearchedNames(searchedNames);
				
				module.addIterator(nameIterator);
				modules.add(module);
				
			}
			
			
			//buyer search
			if(hasBuyer()) {
				ConfigurableNameIterator buyerNameIterator = null;
				FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT , searchId , module);
				DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator();
				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				    			TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
				module.setSaObjKey(SearchAttributes.BUYER_OBJECT);				
				((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) nameFilter).setUseArrangements(false);
				((GenericNameFilter) nameFilter).setInitAgain(true);
				module.addFilter(nameFilter);
				if (isUpdate()) {
					module.addFilter(new RejectAlreadySavedDocumentsForUpdateFilter(searchId));
				}
				FilterResponse doctTypeFilter = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
				module.addFilter(doctTypeFilter);
				module.addValidator(recordedDateNameValidator);
				module.addCrossRefValidator(recordedDateNameValidator);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);	
				buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
							.getConfigurableNameIterator(module, searchId, new String[] {"L f;;"});
				module.addIterator(buyerNameIterator);
				modules.add(module);
			}
			
			
			{				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_PL_EAS_RES_MASDEED);
				module.setParamValue(3, "PLATTED");
				module.setParamValue(4, "Subdivision");
				module.setParamValue(5, "LIKE");
				module.setParamValue(7, "LotNumBegin");
				module.setParamValue(8, "IS");
				module.setParamValue(10, "Block");
				module.setParamValue(11, "IS");
				module.setParamValue(13, "LotNumEnd");
				module.setParamValue(14, "IS");
				module.setParamValue(16, "ParcelNum");
				module.setParamValue(17, "LIKE");
				module.setParamValue(21, "DocTypeRadio");
				
				PlatIterator it = new PlatIterator(searchId);
				
				module.addFilter(subdivisionNameFilter);
				module.addValidator(genericLegalValidator);
			
				for (DocsValidator docsValidator : docsValidators) {
					module.addCrossRefValidator(docsValidator);
				}
				module.addIterator(it);
				module.addFilter(pinFilter);
				modules.add(module);
			}
			
			{//transaction history search
	    		
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
				module.clearSaKeys();
				InstrumentGenericIterator instrumentNoInterator = new InstrumentGenericIterator(searchId);
				module.addIterator(instrumentNoInterator);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				module.addFilter(pinFilter);
				
				//add module only if we have something to search with
				if(!instrumentNoInterator.createDerrivations().isEmpty()) {
					for (DocsValidator docsValidator : docsValidators) {
						module.addValidator(docsValidator);
					}
					for (DocsValidator docsValidator : docsValidators) {
						module.addCrossRefValidator(docsValidator);
					}
					modules.add(module);
				}
			}
	
			{
				// search by crossRef list from RO documents
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		        		TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
				module.addFilter(rejectSavedDocuments);
				module.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);		    		    
				module.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_NOT_AGAIN);	    
				module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				module.addFilter(new GenericInstrumentFilter(searchId));
				module.addFilter(pinFilter);
				module.addFilter(genericLegalFilter);
				modules.add(module);	
			}
		}
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	private void addLegalSearchModuleSLB(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId, 
			DocsValidator[] docsValidators){
		
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK);
		module.clearSaKeys();
		
		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);

		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		it.setEnableSubdividedLegal(true);
		
		module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE);
		module.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_LOT);
		module.setIteratorType(12, FunctionStatesIterator.ITERATOR_TYPE_BLOCK);
		
		module.setSaKey(19, SearchAttributes.FROMDATE_MMM_DD_YYYY);
		module.setSaKey(20, SearchAttributes.TODATE_MMM_DD_YYYY);
		
		module.addFilter(rejectSavedDocuments);
		for (DocsValidator docsValidator : docsValidators) {
			module.addValidator(docsValidator);
		}
		for (DocsValidator docsValidator : docsValidators) {
			module.addCrossRefValidator(docsValidator);
		}
		module.addIterator(it);
		modules.add(module);
		
	}
	
	private void addLegalSearchModuleSLU(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId, 
			DocsValidator[] docsValidators){
		
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_UNIT);
		module.clearSaKeys();
		
		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		it.setEnableSubdividedLegal(true);
		
		module.setSaKey(19, SearchAttributes.FROMDATE_MMM_DD_YYYY);
		module.setSaKey(20, SearchAttributes.TODATE_MMM_DD_YYYY);
		module.addFilter(rejectSavedDocuments);
		for (DocsValidator docsValidator : docsValidators) {
			module.addValidator(docsValidator);
		}
		for (DocsValidator docsValidator : docsValidators) {
			module.addCrossRefValidator(docsValidator);
		}
		module.addIterator(it);
		modules.add(module);
		
	}
	
	private void addLegalSearchModuleSTR(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId, 
			DocsValidator[] docsValidators){
		
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE);
		module.clearSaKeys();
		
		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		it.setEnableTownshipLegal(true);
		
		module.setSaKey(19, SearchAttributes.FROMDATE_MMM_DD_YYYY);
		module.setSaKey(20, SearchAttributes.TODATE_MMM_DD_YYYY);
		
		module.addFilter(rejectSavedDocuments);
		for (DocsValidator docsValidator : docsValidators) {
			module.addValidator(docsValidator);
		}
		for (DocsValidator docsValidator : docsValidators) {
			module.addCrossRefValidator(docsValidator);
		}
		module.addIterator(it);
		modules.add(module);
		
	}
	
	 static protected class LegalDescriptionIterator extends GenericRuntimeIterator<PersonalDataStruct> {
			
			private static final long serialVersionUID = 23238623427069L;
			
			private boolean enableSubdividedLegal = false;
			private boolean enableTownshipLegal = false;
			
			LegalDescriptionIterator(long searchId) {
				super(searchId);
			}
			
			public boolean isEnableSubdividedLegal() {
				return enableSubdividedLegal;
			}
			
			public void setEnableSubdividedLegal(boolean enableSubdividedLegal) {
				this.enableSubdividedLegal = enableSubdividedLegal;
			}

			public boolean isEnableTownshipLegal() {
				return enableTownshipLegal;
			}

			public void setEnableTownshipLegal(boolean enableTownshipLegal) {
				this.enableTownshipLegal = enableTownshipLegal;
			}
			
			@SuppressWarnings("unchecked")
			List<PersonalDataStruct> createDerivationInternal(long searchId){
				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				DocumentsManagerI m = global.getDocManager();
				
				String key = "IL_RO_LOOK_UP_DATA";
				if (isEnableSubdividedLegal()){
					key += "_SLB";
				} else if (isEnableTownshipLegal()) {
					key+= "_STR";
				}
				
				List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)global.getAdditionalInfo(key);
				
				String aoAndTrLots = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
				String []allAoAndTrlots = new String[0];

				if(!StringUtils.isEmpty(aoAndTrLots)){
					Vector<LotInterval> lots = LotMatchAlgorithm.prepareLotInterval(aoAndTrLots);
					HashSet<String> lotExpanded = new LinkedHashSet<String>();
					for (Iterator<LotInterval> iterator = lots.iterator(); iterator.hasNext();) {
						lotExpanded.addAll(((LotInterval) iterator.next()).getLotList());
					}
					allAoAndTrlots = lotExpanded.toArray(allAoAndTrlots);
				}

				if(legalStructList == null){
					legalStructList = new ArrayList<PersonalDataStruct>();
					
					try{
						
						m.getAccess();
						List<RegisterDocumentI> listRodocs = m.getRoLikeDocumentList(true);
						DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_ASC);
						for( RegisterDocumentI reg: listRodocs){
							if(!reg.isOneOf(DocumentTypes.PLAT,
									DocumentTypes.RESTRICTION,
									DocumentTypes.EASEMENT,
									DocumentTypes.MASTERDEED,
									DocumentTypes.COURT,
									DocumentTypes.LIEN,
									DocumentTypes.CORPORATION,
									DocumentTypes.AFFIDAVIT, 
									DocumentTypes.CCER,
									DocumentTypes.MISCELLANEOUS)) {
								for (PropertyI prop: reg.getProperties()){
									if(prop.hasLegal()){
										LegalI legal = prop.getLegal();
										
										if(legal.hasSubdividedLegal() && isEnableSubdividedLegal()){
											
											PersonalDataStruct legalStructItem = new PersonalDataStruct();
											SubdivisionI subdiv = legal.getSubdivision();
											
											String subName = subdiv.getName();
											String block = subdiv.getBlock();
											String lot = subdiv.getLot();
											String unit = subdiv.getUnit();
											String[] lots = lot.split("\\s+");
											for (int i = 0; i < lots.length; i++){
												legalStructItem = new PersonalDataStruct();
												
												legalStructItem.subName = subName;
												legalStructItem.block = StringUtils.isEmpty(block) ? "" : block;
												legalStructItem.unit = unit;
												legalStructItem.lot = lots[i];
											
												if( !testIfExist(legalStructList, legalStructItem, "subdivision") ){
													legalStructList.add(legalStructItem);
												}
											}
										}
										if (legal.hasTownshipLegal() && isEnableTownshipLegal()){
											PersonalDataStruct legalStructItem = new PersonalDataStruct();
											TownShipI township = legal.getTownShip();
													
											String sec = township.getSection();
											String tw = township.getTownship();
											String rg = township.getRange();
											String qtr = township.getQuarterValue();
													
											legalStructItem.section = StringUtils.isEmpty(sec) ? "" : sec;
											legalStructItem.township = StringUtils.isEmpty(tw) ? "" : tw;
											legalStructItem.range = StringUtils.isEmpty(rg) ? "" : rg;
											legalStructItem.qtr = StringUtils.isEmpty(qtr) ? "" : qtr;
											
											if( !testIfExist(legalStructList, legalStructItem, "sectional") ){
												legalStructList.add(legalStructItem);
											}
										}
									}
								}
						}
					}
						if (!legalStructList.isEmpty()){
							global.setAdditionalInfo(key, legalStructList);
						} else {
							return new ArrayList<PersonalDataStruct>();
						}
						
					}
					finally{
						m.releaseAccess();
					}
				}
				return legalStructList;
			}
			@Override
			protected List<PersonalDataStruct> createDerrivations(){
				return createDerivationInternal(searchId);
			}
			
			@Override
			protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
				
					if (StringUtils.isNotEmpty(str.subName) && (StringUtils.isNotEmpty(str.lot) || StringUtils.isNotEmpty(str.unit))
							&& TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK
								.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION))){
						module.setData(6, str.subName);
						module.setData(12, str.block);
						if (StringUtils.isNotEmpty(str.lot)){
							module.setData(9, str.lot);
						} else if (StringUtils.isNotEmpty(str.unit)){
							module.setData(9, str.unit);
						}
						module.setParamValue(3, "PLATTED");
						module.setParamValue(4, "Subdivision");
						module.setParamValue(5, "LIKE");
						module.setParamValue(7, "LotNumBegin");
						module.setParamValue(8, "IS");
						module.setParamValue(10, "Block");
						module.setParamValue(11, "IS");
						module.setParamValue(13, "LotNumEnd");
						module.setParamValue(14, "IS");
						module.setParamValue(16, "ParcelNum");
						module.setParamValue(17, "LIKE");
					} else if (StringUtils.isEmpty(str.subName) && StringUtils.isNotEmpty(str.unit)
							&& TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_UNIT
									.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION))){						    	
						module.setData(6, str.unit);
						module.setData(9, str.section);
						module.setData(12, str.township);
						module.setData(12, str.range);
						module.setData(18, str.qtr);
						
						module.setParamValue(3, "GOVTLOTS");
						module.setParamValue(4, "UnitNum");
						module.setParamValue(5, "IS");
						module.setParamValue(7, "Section");
						module.setParamValue(8, "IS");
						module.setParamValue(10, "Town");
						module.setParamValue(11, "IS");
						module.setParamValue(13, "Range");
						module.setParamValue(14, "IS");
						module.setParamValue(16, "SmallLegal2");//Qtr
						module.setParamValue(17, "IS");
					} else if (StringUtils.isEmpty(str.subName) && StringUtils.isEmpty(str.unit)
							 && StringUtils.isNotEmpty(str.qtr)
							 && TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE
												.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION))){
						module.setData(6, str.section);
						module.setData(9, str.township);
						module.setData(12, str.range);
						module.setData(18, str.qtr);
						
						module.setParamValue(3, "UNPLATTED");
						module.setParamValue(4, "Section");
						module.setParamValue(5, "IS");
						module.setParamValue(7, "Town");
						module.setParamValue(8, "IS");
						module.setParamValue(10, "Range");
						module.setParamValue(11, "IS");
						module.setParamValue(13, "SmallLegal3");//Qtr Qtr 
						module.setParamValue(14, "IS");
						module.setParamValue(16, "SmallLegal2");//Qtr
						module.setParamValue(17, "IS");
					}
			}
		}
	 
	 protected static class PersonalDataStruct implements Cloneable{
			String subName 	= "";
			String lot		= "";
			String block 	= "";
			String unit 	= "";
			String section	= "";
			String township	= "";
			String range	= "";
			String qtr		= "";
			String instrType = "";
			
			@Override
			protected Object clone() throws CloneNotSupportedException {
				return super.clone();
			}
			
			public boolean equalsSubdivision(PersonalDataStruct struct) {
				return this.block.equals(struct.block) && this.lot.equals(struct.lot) 
							&& this.subName.equals(struct.subName) && this.unit.equals(struct.unit)
							&& this.section.equals(struct.section) && this.township.equals(struct.township) 
							&& this.range.equals(struct.range) && this.qtr.equals(struct.qtr);
			}
			
			public boolean equalsSectional(PersonalDataStruct struct) {
				return this.section.equals(struct.section) && this.township.equals(struct.township) 
								&& this.range.equals(struct.range) && this.qtr.equals(struct.qtr);
			}
			
			public boolean equalsPlat(PersonalDataStruct struct) {
				return this.subName.equals(struct.subName) && this.instrType.equals(struct.instrType);
			}
		}
	 
	 private static boolean testIfExist(List<PersonalDataStruct> legalStruct2, PersonalDataStruct l, String string) {
			
			if("subdivision".equalsIgnoreCase(string)){
				for(PersonalDataStruct p:legalStruct2){
					if(l.equalsSubdivision(p)){
						return true;
					}
				}
			} else if("sectional".equalsIgnoreCase(string)){
				for(PersonalDataStruct p:legalStruct2){
					if(l.equalsSectional(p)){
						return true;
					}
				}
			} 
			return false;
		}
	 
	 protected class PlatIterator extends GenericRuntimeIterator<PersonalDataStruct> {
			
			
			private static final long serialVersionUID = 793434519L;
			
			PlatIterator(long searchId) {
				super(searchId);
			}
			
			@SuppressWarnings("unchecked")
			List<PersonalDataStruct> createDerivationInternal(long searchId){
				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)global.getAdditionalInfo("IL_RO_LOOK_UP_DATA_SLB");
				
				List<PersonalDataStruct> newlist = new ArrayList<PersonalDataStruct>();
				PersonalDataStruct legalStructItem = new PersonalDataStruct();
				
				if (legalStructList == null){
					return new ArrayList<PersonalDataStruct>();
				} else {
					for(PersonalDataStruct struct:legalStructList){
						String subName = struct.subName;
						if (StringUtils.isNotEmpty(subName)){
							legalStructItem = new PersonalDataStruct();
							legalStructItem.subName = subName;
							legalStructItem.instrType = "CCR, COR, DDC, DEA, DEC, DIS, EAS, ESE, MAP, PLAT, PSUV, VAC";
							if(!testIfExist(newlist, legalStructItem, "plat")){
								newlist.add(legalStructItem);
							}
						}
					}
				}
				return newlist;
			}
			
			@Override
			protected List<PersonalDataStruct> createDerrivations(){
				return createDerivationInternal(searchId);
			}
			
			@Override
			protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
				
				switch(module.getModuleIdx()){
					case TSServerInfo.NAME_MODULE_IDX:
						if (TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_PL_EAS_RES_MASDEED
								.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION))){
							if (StringUtils.isNotEmpty(str.subName)){
								module.setData(1, str.subName);
								module.setData(23, str.instrType);
							}
						}
					break;
				}
			}

		}
	 
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd)
			throws ServerResponseException {

		
		if (!isParentSite() && "1".equals(module.getFunction(3).getParamValue()) && "".equals(module.getFunction(1).getParamValue())) {
			return new ServerResponse();
		}
		
		return super.SearchBy(module, sd);
	}
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId, true, true).getValidator();
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setVisible(true);
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			String date = gbm.getDateForSearch(id, "MMM dd, yyyy", searchId);
			if (date != null){
				module.getFunction(19).forceValue(date);
			}
			
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			module.addIterator(nameIterator);
			module.addValidator(defaultLegalValidator);
			module.addValidator(addressHighPassValidator);
			module.addValidator(pinValidator);
			module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(
					searchId, 0.90d, module).getValidator());
			module.addValidator(DateFilterFactory.getDateFilterForGoBack(
					SearchAttributes.GB_MANAGER_OBJECT, searchId, module)
					.getValidator());

			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(serverInfo
						.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setVisible(true);
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				date = gbm.getDateForSearchBrokenChain(id, "MMM dd, yyyy",
						searchId);
				if (date != null){
					module.getFunction(19).forceValue(date);
				}
				
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId,
								new String[] { "L;F;" });
				module.addIterator(nameIterator);
				module.addValidator(defaultLegalValidator);
				module.addValidator(addressHighPassValidator);
				module.addValidator(pinValidator);
				module.addValidator(NameFilterFactory
						.getDefaultTransferNameFilter(searchId, 0.90d, module)
						.getValidator());
				module.addValidator(DateFilterFactory.getDateFilterForGoBack(
						SearchAttributes.GB_MANAGER_OBJECT, searchId, module)
						.getValidator());

				modules.add(module);

			}

		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);

	}	

	/**
	 * @param rsResponse
	 * @param viParseID
	 */
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		int iTmp;
		String rsResponse = Response.getResult();
		
		if (Response.getQuerry().contains("Names Summary") && viParseID != ID_DETAILS1) {
			if (Response.isParentSiteSearch()) {
				viParseID = ID_DETAILS1;
			}
		}
		
		if (rsResponse.indexOf("could not be found") >= 0) {
			Response.getParsedResponse().setError(
							"The image could not be found. Most likely this is because the document has never been scanned into the computer system due to its age. To see this document you will need go to the office of the Davidson County Register of Deeds.");
			throw new ServerResponseException(Response);
		}
		if (rsResponse.matches("(?is).*The selection criteria was too general and would have returned \\d+ records.*")) {
			Response.getParsedResponse().setError(
							"The selection criteria was too general and would have returned too many records");
			return;
		}			

		if (rsResponse.indexOf("THE MINIMUM SEARCH CRITERIA WAS NOT PROVIDED") >= 0) {
			return;
		}

		if (rsResponse.indexOf("NO RECORDS MATCH THE SPECIFIED SEARCH CRITERIA") >= 0) {
			Response.getParsedResponse().setError("NO RECORDS MATCH THE SPECIFIED SEARCH CRITERIA.");
			return;
		}

		if (rsResponse.indexOf("Error Retrieving Detail Data") >= 0) {
			return;
		}

		if (rsResponse.indexOf("Error Trapped") >= 0){
			return;
		}

		if (rsResponse.indexOf("NO RECORDS RETRIEVED") >= 0) {
			Response.getParsedResponse().setError("NO RECORDS RETRIEVED FOR THE SPECIFIED SEARCH CRITERIA.");
			return;
		}
		if (rsResponse.indexOf("YOU CAN NOW VIEW PAGES FROM BOOK") >= 0) {
			return;
		}
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_PARCEL:
		case ID_SAVE_TO_TSD:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_DETAILS:
		{
			if (rsResponse.indexOf("Search Criteria") >= 0) { 

				rsResponse = rsResponse.replaceAll("\\r\\n", "");
				// Apartments Condominiums
				rsResponse = rsResponse.replaceAll("(?i)Apartments|Condominiums", "");
				// cut all until the first hr
				int i = rsResponse.indexOf("<hr>");
				if (i > -1)
					rsResponse = rsResponse.substring(i);

				rsResponse = rsResponse.substring(0, rsResponse.indexOf("<table border=1 width='100%'><tr><td><b>Search Criteria:</b>"));			
				rsResponse = rsResponse.replaceAll("(?is)<tr[^>]*>\\s*(<td[^>]*>&nbsp;</td>)\\1+</tr>", "");
				rsResponse = rsResponse.replaceAll("(?is)\\(\\s*<a[^>]+>([^<]+)</a>\\s*\\)", "($1)");
				
				rsResponse = rsResponse.replaceAll("<A HREF='",
						"<A target=\"_blank\" HREF='" + CreatePartialLink(TSConnectionURL.idGET) + "/fr1/");
				rsResponse = rsResponse.replaceAll("<a href='", "<a HREF='" + CreatePartialLink(TSConnectionURL.idGET) + "/fr1/");

				rsResponse = rsResponse.replaceAll("<a HREF=(.*?)'(.*?)'>", "<a HREF='$1$2'>");

				if (Response.getRawQuerry().indexOf("Names+Summary") >= 0) {
					// automatic name search --> first the name summary list is
					// retrieved
					String querry = Response.getQuerry();

					querry = querry.replaceAll("Names Summary", "Detail Data");
					if ((iTmp = rsResponse.indexOf("<form")) != -1) {
						int endIdx = rsResponse.indexOf("/form>", iTmp) + 6;
						rsResponse = rsResponse.substring(iTmp, endIdx);

						rsResponse = rsResponse.replaceAll(
										"<input TYPE=\"checkbox\" NAME=\"Names\" Value=\"(.*?)\" alt=\"Select Name\">",
										"<a href='"
												+ CreatePartialLink(TSConnectionURL.idPOST)
												+ sAction + "&"	+ querry
												+ "&Names=$1&automaticNameSearch=true'>View</a>");
					}
				} else {
					
				}
			}

			if (viParseID == ID_SAVE_TO_TSD) {
				String sInstrumentNo = getInstrNoFromResponse(rsResponse);
				logger.info("Instrument NO:" + sInstrumentNo);

				msSaveToTSDFileName = sInstrumentNo + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD(true);

				try {
					org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList mainTableList = tableParser.parse(new TagNameFilter("table"));
					if (mainTableList != null && mainTableList.size() > 0){
						TableTag mainTable = (TableTag) mainTableList.elementAt(0);
					
						TableColumn tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Instrument:").getParent();
						String instr = HtmlParser3.getValueFromCell(tc, "", false);
						instr = instr.replaceAll("(?is)Instrument:", "").replaceAll("(?is)&nbsp;", " ").trim();
														
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Recorded:").getParent();
						String recDate = HtmlParser3.getValueFromCell(tc, "", false);
						recDate = recDate.replaceAll("(?is)Recorded:", "").replaceAll("(?is)&nbsp;", " ").trim();
							
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Pages:").getParent();
						String noOfPages = HtmlParser3.getValueFromCell(tc, "", false);
						noOfPages = noOfPages.replaceAll("(?is)Pages:", "").replaceAll("(?is)&nbsp;", " ").trim();
							
						StringBuffer imageLink = new StringBuffer();
						
						if (StringUtils.isNotEmpty(noOfPages)){
							String year = recDate.replaceAll("(?is)\\A\\d+/\\d+/(\\d+).*", "$1");
							/*int yearInt = 2011;
							try {
								yearInt = Integer.parseInt(year);
							} catch (Exception e) {
								logger.error("Couldn't parse to int the recorded year for: " + this);
							}*/
							//if (yearInt > 1979){
								imageLink.append(CreatePartialLink(TSConnectionURL.idGET))
												.append("/recording/LoadImage.asp&InstrID=").append(instr).append("&Year=").append(year);
								Response.getParsedResponse().addImageLink(new ImageLinkInPage(imageLink.toString(), instr + ".tif"));
							//}
						}
					}
				} catch (Exception e) {
					logger.error("Image link can't be found on : " + this);
				}
				ParsedResponse pr = Response.getParsedResponse();
				// save any coss ref link before removing it
				String resultForCross = rsResponse;
				
				rsResponse = rsResponse.replaceAll("asp\\?", "asp\\&");
				rsResponse = rsResponse.replaceAll("a href=\"(.*?)\"", "a href=\'$1\'");
				rsResponse = rsResponse.replaceAll("a href", "a HREF");
				rsResponse = rsResponse.replaceAll("<a HREF='[^']*'><nobr>Show Detail For All Marginals</nobr></a>", "");
				rsResponse = rsResponse.replaceAll("<nobr> <a HREF='[^']*'>Show Detail For All Marginals</a></nobr>", "");

				smartParseDetails(Response, rsResponse);
                
				// removing "Marginal" link
				rsResponse = rsResponse.replaceAll("<a.*?>(.*?)</a>", "$1");
				pr.setOnlyResponse(rsResponse);
				
				Matcher crossRefLinkMatcher = iPattern.matcher(resultForCross);
				while(crossRefLinkMatcher.find()) {
					ParsedResponse prChild = new ParsedResponse();
					String link = crossRefLinkMatcher.group(1) + "&isSubResult=true";
					LinkInPage pl = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
					prChild.setPageLink(pl);
					Response.getParsedResponse().addOneResultRowOnly(prChild);
				}

			} else { // not saving to TSD
				if (Response.getRawQuerry().indexOf("Names+Summary") < 0) {
					
					try {
						 
						StringBuilder outputTable = new StringBuilder();
						ParsedResponse parsedResponse = Response.getParsedResponse();
																			
						Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
												
						if(smartParsedResponses.size() > 0) {
							parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
							parsedResponse.setOnlyResponse(outputTable.toString());
			            }
						
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
			break;
		case ID_DETAILS1:
			rsResponse = rsResponse.replaceAll("(?is)(<form[^>]*>)", "$1"
					+ new LinkParser(CreatePartialLink(TSConnectionURL.idPOST) + sAction + "&automaticNameSearch=true")
									.toStringParam("<input TYPE='hidden' NAME='", "' VALUE='", "'>\n"));
			rsResponse = rsResponse.replaceAll("(?is).*?(<form.*?</form[^>]*>).*", "$1");
			
			rsResponse = rsResponse.replaceAll("(?is)<form[^>]*>",
							"<form name=\"action2\" id=\"action2\" method=\"GET\" action=\""
									+ CreatePartialLink(TSConnectionURL.idGET) + "\" >");
			
			rsResponse = rsResponse.replaceAll("(?is)<input[^>]*Summary\\s*Data[^>]*>", "");
			rsResponse = rsResponse.replaceAll("(?is)(<input[^>]*Detail\\s*Data[^\'\"]*[\"\']{1})([^>]*>)",
												"$1" + " class=\"button\"" + "$2");
			
			parser.Parse(Response.getParsedResponse(), rsResponse,
					Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET),
					TSServer.ID_SEARCH_BY_NAME);
			
			break;
		case ID_GET_LINK:
			if (Response.getQuerry().indexOf("automaticNameSearch") >= 0
					|| Response.getQuerry().indexOf("submit") >= 0 ) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
		default:
			break;
		}
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		Search search = this.getSearch();
		searchId = search.getID();
		
		/**
		 * We need to find what was the original search module
		 * in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if(objectModuleSource != null) {
			if(objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			} 
		} else {
			objectModuleSource = search.getAdditionalInfo(
					this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}
		
		try {
			table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>");
			
			String[] tables = table.split("<hr>");
			for (String tabel : tables) {
				if (StringUtils.isNotEmpty(tabel)){
					org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(tabel, null);
					NodeList mainTableList = tableParser.parse(new TagNameFilter("table"));
					if (mainTableList != null && mainTableList.size() > 0){
						TableTag mainTable = (TableTag) mainTableList.elementAt(0);
				
						TableColumn tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Instrument:").getParent();
						String instr = HtmlParser3.getValueFromCell(tc, "", false);
						instr = instr.replaceAll("(?is)Instrument:", "").replaceAll("(?is)&nbsp;", " ").trim();
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Old Doc Ref No:").getParent();
						String oldDocRef = HtmlParser3.getValueFromCell(tc, "", false);
						oldDocRef = oldDocRef.replaceAll("(?is)Old Doc Ref No:", "").replaceAll("(?is)&nbsp;", " ").trim();
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Book/Page:").getParent();
						String bookPage = HtmlParser3.getValueFromCell(tc, "", false);
						bookPage = bookPage.replaceAll("(?is)Book/Page:", "").replaceAll("(?is)&nbsp;", " ").trim();
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Recorded:").getParent();
						String recDate = HtmlParser3.getValueFromCell(tc, "", false);
						recDate = recDate.replaceAll("(?is)Recorded:", "").replaceAll("(?is)&nbsp;", " ").trim();
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Consideration:").getParent();
						String consideration = HtmlParser3.getValueFromCell(tc, "", false);
						consideration = consideration.replaceAll("(?is)Consideration:", "").replaceAll("(?is)&nbsp;", " ").replaceAll("[$,]+", "").trim();
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Document Type:").getParent();
						String docType = HtmlParser3.getValueFromCell(tc, "", false);
						docType = docType.replaceAll("(?is)Document Type:", "").replaceAll("(?is)&nbsp;", " ").trim();
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Document Date:").getParent();
						String instrDate = HtmlParser3.getValueFromCell(tc, "", false);
						instrDate = instrDate.replaceAll("(?is)Document Date:", "").replaceAll("(?is)&nbsp;", " ").trim();
						
						String grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantor:"), "", true);
						if (StringUtils.isEmpty(grantors)){
							grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Debtor:"), "", true);
						}
						String grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantee:"), "", true);
						if (StringUtils.isEmpty(grantees)){
							grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Secured Party:"), "", true);
						}
						
						StringBuffer imageLink = new StringBuffer();
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Pages:").getParent();
						String noOfPages = HtmlParser3.getValueFromCell(tc, "", false);
						noOfPages = noOfPages.replaceAll("(?is)Pages:", "").replaceAll("(?is)&nbsp;", " ").trim();
						
						
						String legalDesc = HtmlParser3.getValueFromNextCell(mainTableList, "Legal Description:", "", true);
						legalDesc = legalDesc.replaceAll("(?is)</?i>", "").replaceAll("(?is)<br>", ", ");
							
						String key = instr + "_" + docType.replaceAll("\\s+", "_");
		
						ParsedResponse currentResponse = responses.get(key);							 
						if(currentResponse == null) {
							currentResponse = new ParsedResponse();
							responses.put(key, currentResponse);
						}
						String year = "";
						if (StringUtils.isNotEmpty(noOfPages)){
							year = recDate.replaceAll("(?is)\\A\\d+/\\d+/(\\d+).*", "$1");
							/*int yearInt = 2011;
							try {
								yearInt = Integer.parseInt(year);
							} catch (Exception e) {
								logger.error("Couldn't parse to int the recorded year for: " + this);
							}*/
							//if (yearInt > 1979){
								imageLink.append(CreatePartialLink(TSConnectionURL.idGET))
										.append("/recording/LoadImage.asp&InstrID=").append(instr).append("&Year=").append(year);
							//}
							currentResponse.addImageLink(new ImageLinkInPage(imageLink.toString(), instr + ".tif"));
						}
								
						RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();				
						
						ResultMap resultMap = new ResultMap();
								
						String link = CreatePartialLink(TSConnectionURL.idGET) + "/fr1/SimpleQuery.asp&Instrs=" + key;
						if(document == null) {	//first time we find this document
	
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDesc);
									
							String rowHtml =  mainTable.toHtml();
									
							resultMap.put("tmpPartyGtor", grantors);
							resultMap.put("tmpPartyGtee", grantees);
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
							String[] bp = bookPage.split("\\s+");
							if (bp.length == 2){
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bp[0]);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), bp[1]);
							}
							resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate.replaceAll("(?is)\\A\\s*([\\d/]+)\\s+.*", "$1").trim());
							try {
								GenericDTSFunctionsRO.parseNamesRO(resultMap, searchId);
								GenericDTSFunctionsRO.parseLegal(resultMap, searchId);
							} catch (Exception e) {
								e.printStackTrace();
							}
							resultMap.removeTempDef();
				    				
							@SuppressWarnings("unchecked")
							Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) resultMap.get("PropertyIdentificationSet");
							if (pisVector != null && !pisVector.isEmpty()){
								for (PropertyIdentificationSet everyPis : pisVector){
									currentResponse.addPropertyIdentificationSet(everyPis);
								}
							}
				    				
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
																	
							Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
							document = (RegisterDocumentI) bridge.importData();
									
							currentResponse.setDocument(document);
							String checkBox = "checked";
							HashMap<String, String> data = new HashMap<String, String>();
							data.put("type", docType);
							data.put("year", year);
									
							if (isInstrumentSaved(instr, document, data) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
								checkBox = "saved";
							} else {
								numberOfUncheckedElements++;
								LinkInPage linkInPage = new LinkInPage(link, link, 
						    					TSServer.REQUEST_SAVE_TO_TSD);
								currentResponse.setPageLink(linkInPage);
								checkBox = "<input type='checkbox' name='docLink' value='"
											+ link + "'>Select for saving to TS Report";
								/**
								 * Save module in key in additional info. The key is instrument number that should be always available. 
								 */
								String keyForSavingModules = this.getKeyForSavingInIntermediary(instr);
								search.setAdditionalInfo(keyForSavingModules, moduleSource);
							}
							
							if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ){
								rowHtml = rowHtml.replaceFirst(
										"</TR></Table>",
										"</TR><TR><TD COLSPAN='100'><br><br>&nbsp;&nbsp;&nbsp;"
											+ "<a id=\"imageLink\" href=\"" + imageLink.toString() + "\" target=\"_blank\" align=\"center\">View Image</a><br><br>"
											+ "</TD></TR><TR><TD COLSPAN='100'></TD></TR></table>");
							
							}
							mSearch.addInMemoryDoc(link, rowHtml);
							
							rowHtml = rowHtml.replaceFirst(
									"(?is)</TR></Table>",
									"</TR><TR><TD COLSPAN='100'>" + checkBox + "</TD></TR><TR><TD COLSPAN='100'><hr></TD></TR></table>");
							currentResponse.setOnlyResponse(rowHtml);
							newTable.append(currentResponse.getResponse());
							intermediaryResponse.add(currentResponse);
								
						}
			
						newTable.append("</table>");
						outputTable.append(newTable);
						SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
					}
				}
			}
				
			String prevLink = "", nextLink = "";
			NodeList formList = htmlParser.parse(new TagNameFilter("form"));
			if (formList != null && formList.size() > 0){
				FormTag form = (FormTag) formList.elementAt(0);
				if (form != null){
					String action = form.getAttribute("action");
					String link = CreatePartialLink(TSConnectionURL.idPOST) + "/fr1/" + action + "?";
					
					Map<String,String> paramsForNav = new HashMap<String, String>();
					NodeList inputs = form.getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);
					for (int j = 0; j < inputs.size(); j++){
						InputTag input = (InputTag) inputs.elementAt(j);
						if ("hidden".equals(input.getAttribute("type"))){
							if (input.getAttribute("name") != null){
								if (input.getAttribute("value") != null){
									paramsForNav.put(input.getAttribute("name"), input.getAttribute("value"));
								} else {
									paramsForNav.put(input.getAttribute("name"), "");
								}
							}
						} else if ("submit".equals(input.getAttribute("type"))){
							if (input.getAttribute("name") != null){
								String submit = input.getAttribute("value");
								Matcher m = Pattern.compile("Detail Data \\d*-\\?").matcher(submit);
								if (m.find()) {
									nextLink = "<a href=\"" + link + "navig=Next&submit=" + submit + "\">Next</a>";
									response.getParsedResponse().setNextLink(nextLink);
								} else {
									prevLink = "<a href=\"" + link + "navig=Prev&submit=" + submit + "\">Previous</a>";
								}
							} 
							
						}
					}
					if (!paramsForNav.isEmpty()){
						mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsNav:", paramsForNav);
					}
				}
			}
			
			String header1 = "<TH width=\"5%\" align=\"justify\">" + SELECT_ALL_CHECKBOXES + "Check\\Uncheck All</TH>";
			
			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
								+ "<br>" + prevLink + "&nbsp;&nbsp;&nbsp;" + nextLink + "<br><br>" 
					+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
			response.getParsedResponse().setFooter("</table>" +  
							"<br>" + prevLink + "&nbsp;&nbsp;&nbsp;" + nextLink + "<br><br>" +  CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String rsResponse, ResultMap resultMap) {
		
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
			
			org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainTableList = tableParser.parse(new TagNameFilter("table"));
			if (mainTableList != null && mainTableList.size() > 0){
				TableTag mainTable = (TableTag) mainTableList.elementAt(0);
		
				TableColumn tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Instrument:").getParent();
				String instr = HtmlParser3.getValueFromCell(tc, "", false);
				instr = instr.replaceAll("(?is)Instrument:", "").replaceAll("(?is)&nbsp;", " ").trim();
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Old Doc Ref No:").getParent();
				String oldDocRef = HtmlParser3.getValueFromCell(tc, "", false);
				oldDocRef = oldDocRef.replaceAll("(?is)Old Doc Ref No:", "").replaceAll("(?is)&nbsp;", " ").trim();
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Book/Page:").getParent();
				String bookPage = HtmlParser3.getValueFromCell(tc, "", false);
				bookPage = bookPage.replaceAll("(?is)Book/Page:", "").replaceAll("(?is)&nbsp;", " ").trim();
				String[] bp = bookPage.split("\\s+");
				if (bp.length == 2){
					resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bp[0]);
					resultMap.put(SaleDataSetKey.PAGE.getKeyName(), bp[1]);
				}
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Recorded:").getParent();
				String recDate = HtmlParser3.getValueFromCell(tc, "", false);
				recDate = recDate.replaceAll("(?is)Recorded:", "").replaceAll("(?is)&nbsp;", " ").trim();
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate.replaceAll("(?is)\\A\\s*([\\d/]+)\\s+.*", "$1").trim());
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Document Type:").getParent();
				String docType = HtmlParser3.getValueFromCell(tc, "", false);
				docType = docType.replaceAll("(?is)Document Type:", "").replaceAll("(?is)&nbsp;", " ").trim();
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Consideration:").getParent();
				String consideration = HtmlParser3.getValueFromCell(tc, "", false);
				consideration = consideration.replaceAll("(?is)Consideration:", "").replaceAll("(?is)&nbsp;", " ").replaceAll("[$,]+", "").trim();
				if (DocumentTypes.isMortgageDocType(docType, searchId)){
					resultMap.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), consideration);
				} else {
					resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), consideration);
				}
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Document Date:").getParent();
				String instrDate = HtmlParser3.getValueFromCell(tc, "", false);
				instrDate = instrDate.replaceAll("(?is)Document Date:", "").replaceAll("(?is)&nbsp;", " ").trim();
				resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrDate);
				
				String grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantor:"), "", true);
				if (StringUtils.isEmpty(grantors)){
					grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Debtor:"), "", true);
				}
				String grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantee:"), "", true);
				if (StringUtils.isEmpty(grantees)){
					grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Secured Party:"), "", true);
				}
				
				resultMap.put("tmpPartyGtor", grantors);
				resultMap.put("tmpPartyGtee", grantees);
				
				String legalDesc = HtmlParser3.getValueFromNextCell(mainTableList, "Legal Description:", "", true);
				legalDesc = legalDesc.replaceAll("(?is)</?i>", "").replaceAll("(?is)<br>", ", ");
				
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDesc);
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Comments:").getParent();
				String comments = HtmlParser3.getValueFromCell(tc, "", false);
				comments = comments.replaceAll("(?is)Comments:", "");
				comments = comments.replaceAll("(?is)</?i>", "").replaceAll("(?is)<br>", ", ").replaceAll("(?is)&nbsp;", " ");
				resultMap.put("tmpComments", comments);
				
				//extract cross refs from legal description
				Set<String> crossRefs = new HashSet<String>();
				Matcher matcher = iPattern.matcher(rsResponse);
				while (matcher.find()){
					String instrCrossRef = matcher.group(2).trim();
					instrCrossRef = instrCrossRef.replaceAll("(?is)\\A(?:\\w+\\s+)?([A-Z\\d-]+)\\s+\\([^\\)]*\\)\\s*$", "$1");
					if (!instrCrossRef.startsWith("R")){
						if (matcher.group(2).trim().startsWith("U")){
							instrCrossRef = org.apache.commons.lang.StringUtils.leftPad(instrCrossRef, instrCrossRef.length() + 1, 'U');
						} else {
							instrCrossRef = org.apache.commons.lang.StringUtils.leftPad(instrCrossRef, instrCrossRef.length() + 1, 'R');
						}
					}
					crossRefs.add(instrCrossRef);
					//line.add(instrCrossRef);
					//bodyCR.add(line);
				}
				Pattern pat = Pattern.compile("(?is)ExtDesc:.*?\\s+(R[\\d-]{4,})");//R65004136
				Matcher mat = pat.matcher(legalDesc);
				if (mat.find()){
					String instrCrossRef = mat.group(1).trim();
					String lastPart = instrCrossRef.replaceAll("(?is)\\A[^-]+-(\\d+)", "$1");
					lastPart = org.apache.commons.lang.StringUtils.leftPad(lastPart, 6, '0');
					boolean yearModified = false;
					int indexOfDash = instrCrossRef.indexOf("-");
					if (indexOfDash > 1){
						String year = instrCrossRef.substring(1, indexOfDash);
						if (year.length() == 2){
							int yearInt = Integer.parseInt(year);
							if (yearInt < 65){
								year = "20" + year;
								instrCrossRef = instrCrossRef.replaceAll("(?is)\\A([A-Z])[^-]+-\\d+", "$1" + year + lastPart);
								yearModified = true;
							}
						}
					} 
					if (!yearModified){
						instrCrossRef = instrCrossRef.replaceAll("(?is)\\A([^-]+)-\\d+", "$1" + lastPart);
					}
					crossRefs.add(instrCrossRef);
					//line.add(instrCrossRef);
					//bodyCR.add(line);
				}
				pat = Pattern.compile("(?is)\\b([A-Z][\\d-]+)");//U87000977
				mat = pat.matcher(comments);
				if (mat.find()){
					String instrCrossRef = mat.group(1).trim();
					String lastPart = instrCrossRef.replaceAll("(?is)\\A[^-]+-(\\d+)", "$1");
					lastPart = org.apache.commons.lang.StringUtils.leftPad(lastPart, 6, '0');
					instrCrossRef = instrCrossRef.replaceAll("(?is)\\A([^-]+)-\\d+", "$1" + lastPart);

					crossRefs.add(instrCrossRef);
					//line.add(instrCrossRef);
					//bodyCR.add(line);
				}
				if (!crossRefs.isEmpty()){
					@SuppressWarnings("rawtypes")
					List<List> bodyCR = new ArrayList<List>();
					for (String string : crossRefs) {
						List<String> line = new ArrayList<String>();
						line.add(string);
						bodyCR.add(line);
					}
					String [] header = {"InstrumentNumber"};		   
					Map<String,String[]> map = new HashMap<String,String[]>();		   
					map.put("InstrumentNumber", new String[]{"InstrumentNumber", ""});
					   
					ResultTable cr = new ResultTable();	
					cr.setHead(header);
					cr.setBody(bodyCR);
					cr.setMap(map);		   
					resultMap.put("CrossRefSet", cr);
				}
				
				GenericDTSFunctionsRO.parseNamesRO(resultMap, searchId);
				GenericDTSFunctionsRO.parseLegal(resultMap, searchId);
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getInstrNoFromResponse(String sTmp) {
		int iTmp = sTmp.indexOf("Instrument");
		iTmp = sTmp.indexOf("/B>", iTmp);
		iTmp = sTmp.indexOf(">", iTmp) + 1;
		String sInstrumentNo = sTmp.substring(iTmp, sTmp.indexOf("<", iTmp))
				.trim();
		return sInstrumentNo;
	}

	protected String getFileNameFromLink(String url) {

		String rez = url.replaceAll(".*Instrs=(.*?)(?=&|$)", "$1");

		if (rez.trim().length() > 10)
			rez = rez.replaceAll("&parentSite=true", "");

		return rez.trim() + ".html";
	}
	
	@Override
	protected void setCertificationDate() {

		try {
			
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
				String html = "";
				String getBaseLink = getBaseLink();
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				try{
					html = site.process(new HTTPRequest(getBaseLink)).getResponseAsString();
				} catch(RuntimeException e){
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(site);
				}  
				Matcher certDateMatcher = certDatePattern.matcher(html);
				if(certDateMatcher.find()) {
					String date = certDateMatcher.group(1).trim();
					date = date.replaceAll("(?is)</?font[^>]*>", "");
					Date d = CertificationDateManager.sdfIn.parse(date);
		            date = CertificationDateManager.sdfOut.format(d);
		            
					CertificationDateManager.cacheCertificationDate(dataSite, date);
					getSearch().getSa().updateCertificationDateObject(dataSite, d);
				} else {
					CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because pattern not found");
				}	
			}
        } catch (Exception e) {
        	CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
        }
	}

	/*
	 * Given a book and a page number, it forms a link that starts a search for
	 * the respective values.
	 */
	@SuppressWarnings("unused")
	private String GetLinkBookPage(String book, String page) {
		String link_str = new String();
		String default_link = CreatePartialLink(TSConnectionURL.idGET);
		default_link.substring(0, default_link.indexOf("&Link="));
		link_str = " <a HREF='" + default_link + SEARCH_PATH + "&Instrs=&Book="
				+ book + "&Page=" + page
				+ "&SUBMIT=Detail Data&SortDir=ASC&StartDate=&EndDate=" + "'>"
				+ book + " " + page + "</a> ";

		return link_str;
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		module.setParamValue(4, "Detail Data");
		if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module.forceValue(1, book);
			module.forceValue(2, page);
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module.forceValue(3, restoreDocumentDataI.getDocumentNumber());
		} else {
			module = null;
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
	
	 /**
     * treat the case in which the user clicked on an image link, and download it only once  
     */
    @Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded) throws ServerResponseException {
    	
    	if (vsRequest.toLowerCase().contains("simplequery")){//when points to a crossrefs link from doc index, or next/prev link
    		return super.GetLink(vsRequest, vbEncoded); 
    	}
    	String instr = StringUtils.extractParameterFromUrl(vsRequest, "InstrID");
    	instr = instr.replaceFirst("(?is)\\A[A-Z]\\d+(\\d{6}).*", "$1");
    	String year = StringUtils.extractParameterFromUrl(vsRequest, "year");
    	
     	// construct fileName
    	String folderName = getCrtSearchDir() + "Register" + File.separator;
		new File(folderName).mkdirs();
    	String fileName = folderName + instr + ".tif";
    	
    	/**
    	 * ILWillRO takes images from PI
    	 */
    	
    	// retrieve the image
   		ILWillImageRetriever.INSTANCE.retrieveImage(instr, fileName, "", year, searchId);
    	
		// write the image to the client web-browser
		boolean imageOK = writeImageToClient(fileName, "image/tiff");
		
		// image not retrieved
		if(!imageOK){ 
	        // return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);			
		}
		// return solved response
		return ServerResponse.createSolvedResponse();    	

    }

    @Override
    protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException{

    	String link = image.getLink();
    	
    	if(StringUtils.isEmpty(link)){
    		return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );
    	}

    	String instr = StringUtils.extractParameterFromUrl(link, "InstrID");
    	instr = instr.replaceFirst("(?is)\\A[A-Z]\\d+(\\d{6}).*", "$1");
    	String year = StringUtils.extractParameterFromUrl(link, "year");
    	
		String fileName = image.getPath();
    	/**
    	 * ILWillRO takes images from PI
    	 */
    	
    	// retrieve the image
		if(ILWillImageRetriever.INSTANCE.retrieveImage(instr, fileName, "", year,searchId)){
			byte b[] = FileUtils.readBinaryFile(fileName);
			//already counted in retrieveImage
			//afterDownloadImage(true);
    		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
		}
		return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );
    }
    
    @Override
	public void cleanOCRData(OCRParsedDataStruct ocrData) {	
		if (getDataSite().getCountyId() == CountyConstants.IL_Will)
			for (Instrument instr : ocrData.getInstrumentVector())
			{
				instr.setInstrumentNo(instr.getInstrumentNo().replaceAll("-", "0"));
			}
	}
}
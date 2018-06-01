package ro.cst.tsearch.servers.types;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
import java.util.Map.Entry;
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
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
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
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.ParcelIdIterator;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
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
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.InstrumentI;
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
 * @author olivia
 * 
 */
@SuppressWarnings("deprecation")
public class OHLickingRO extends GenericDTSServerRO implements TSServerROLikeI {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Category logger = Logger.getLogger(GenericDTSServerRO.class);

//	private static final Pattern iPattern = Pattern.compile("(?is)<i>(?:Bkwd|Fwd)</i>\\s*<a\\s+href\\s*=\\s*\\\"simplequery.asp\\?instrs=([^\\\"]+)\\\"[^>]*>[^<]*</a>");
	private static final Pattern iPattern = Pattern.compile("(?is)<i>(?:Bkwd|Fwd)</i>\\s*<a(?: target=\\\"_blank\\\")?\\s+href\\s*=\\s*['\\\"](?:[A-Z&\\?=\\d/-]+)?simplequery\\.asp[\\?&]instrs=(\\d+)['\\\"]>[^<]+</a>");
	private static final Pattern bkPgPattern = Pattern.compile("(?is)<i>(?:Bkwd|Fwd)</i>\\s*(?:\\b(?:OR\\b|D\\b|M\\b|F\\b)?\\s*(\\d+\\s+\\d+))");
	private static final Pattern iPatternRefLink = Pattern.compile("(?is)<i>(?:Bkwd|Fwd)</i>\\s*<a\\s+href\\s*=\\s*[']/[^\\?]+\\?[^/]+([^']+)'\\s*>[^<]+</a>");
	
//	private static final Pattern certDatePattern = Pattern.compile("(?ism)The Data is Current Thru:</SPAN>(.*?)</font>");

	//private static String[] docTypeForPlats = {"CCR", "COR", "DDC", "DEA", "DEC", "DIS", "EAS", "ESE", "MAP", "PLAT", "PSUV", "VAC"};
	
	public OHLickingRO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		Search global = getSearch();
		int searchType = global.getSearchType();
		boolean isUpdate = (isUpdate() || global.getSa().isDateDown());
		boolean validateWithDates = applyDateFilter();

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
						if(pin.contains(".") || pin.contains("-")) {
							pin = pin.replaceAll("[-]", "");
							output.add(pin);
							if (pin.length() == 14) {
								output.add("0" + pin);
								pin = pin.replaceAll("[-\\.]", "");
								output.add("0" + pin);
							}
						}
					}
					return output;
				}
			};
			pinFilter.setStartWith(false);
//			pinFilter.setIgNoreStrartingZeroes(true);
			
			GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
//			FilterResponse cityFilter 		= CityFilterFactory.getCityFilter(searchId, 0.6d);
	        GenericLegal genericLegalFilter = new GenericLegal(searchId);
	        	genericLegalFilter.setEnableSection(false);
	        DocsValidator genericLegalValidator = genericLegalFilter.getValidator();
			GenericMultipleLegalFilter defaultLegalFilter = new GenericMultipleLegalFilter(searchId);
				defaultLegalFilter.setUseLegalFromSearchPage(true);
				defaultLegalFilter.setThreshold(new BigDecimal(0.7));
//				defaultLegalFilter.disableAll();
				defaultLegalFilter.setEnableLot(true);
				defaultLegalFilter.setEnableBlock(true);
				defaultLegalFilter.setEnableSection(true);
				defaultLegalFilter.setEnableTownship(true);
				defaultLegalFilter.setEnableRange(true);
//				defaultLegalFilter.setEnableLotUnitFullEquivalence(true);
				defaultLegalFilter.setMarkIfCandidatesAreEmpty(true);
			DocsValidator multipleLegalValidator = defaultLegalFilter.getValidator();
			
	        DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
//	        DocsValidator[] docsValidators = new DocsValidator[]{pinFilter.getValidator(), addressFilter.getValidator(), cityFilter.getValidator()};
	        DocsValidator[] docsValidators = new DocsValidator[]{pinFilter.getValidator(), addressFilter.getValidator()};
	        FilterResponse subdivisionNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
	        subdivisionNameFilter.setThreshold(new BigDecimal(0.90d));
	        
	        FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
	        
			{//transaction history search
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
				module.clearSaKeys();
				InstrumentGenericIterator instrumentNoInterator = new InstrumentGenericIterator(searchId) { 
					private static final long serialVersionUID = 2750089650252801002L;
					
					@Override
					protected String cleanInstrumentNo(InstrumentI instrument){
						String instrNo = instrument.getInstno();
						Date recDate = instrument.getDate();
						if (recDate != null) {
							SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
							String date = sdf.format(recDate);
							if (StringUtils.isNotEmpty(date)) {
								if (instrNo != null) {
									if (instrNo.length() <= 7) {
										return (date + org.apache.commons.lang.StringUtils.leftPad(instrNo, 7, "0"));
									}
								}
							}
						}
						return cleanInstrumentNo(instrument.getInstno(), instrument.getYear());
					}
				};
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				
				//add module only if we have something to search with
				if(!instrumentNoInterator.createDerrivations().isEmpty()) {
					module.addFilter(pinFilter);
					for (DocsValidator docsValidator : docsValidators) {
						module.addValidator(docsValidator);
					}
					module.addValidator(genericLegalValidator);
					for (DocsValidator docsValidator : docsValidators) {
						module.addCrossRefValidator(docsValidator);
					}
					module.addCrossRefValidator(genericLegalValidator);
					module.addIterator(instrumentNoInterator);
					modules.add(module);
				}
			}
			        
			
			{//parcel id search
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
				module.clearSaKeys();
				module.setParamValue(4, "ParcelNum");
				module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE);
				
				for (DocsValidator docsValidator : docsValidators) {
					module.addValidator(docsValidator);
				}
				module.addValidator(multipleLegalValidator);
				for (DocsValidator docsValidator : docsValidators) {
					module.addCrossRefValidator(docsValidator);
				}
				module.addCrossRefValidator(multipleLegalValidator);
				
				ParcelIdIterator it = new ParcelIdIterator(searchId) {
					private static final long serialVersionUID = 2750089650252801002L;
	
					@Override
					protected List<String> createDerrivations() {
						List<String> derivationList = super.createDerrivations();
						for (int i = 0; i < derivationList.size(); i++){
							if (!derivationList.get(i).contains("-")){
								String tmpPid = derivationList.get(i);
								if (tmpPid.matches("\\d{13}")) {
									tmpPid = tmpPid.replaceFirst("(\\d{2})(\\d{6})(\\d{2})(\\d{3})", "$1-$2-$3.$4");
								} else if (tmpPid.matches("\\d{11}\\.\\d{3}")) {
									tmpPid = tmpPid.replaceFirst("(\\d{3})(\\d{6})(\\d{2})(\\.\\d{3})", "$1-$2-$3$4");
									derivationList.add(tmpPid);
									if (tmpPid.startsWith("0")) {
										tmpPid = tmpPid.replaceFirst("^0([\\d\\.-]+)", "$1");
										derivationList.add(tmpPid);
									}
								}
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
						if (pin.length() == 15) {
							if (pin.startsWith("0")) {
								pin = pin.substring(1);
							}
						} 
						if (pin.length() == 14) {
							pin = pin.replaceAll("(\\d{2})(\\d{6})(\\d{2})\\.(\\d{3})", "$1-$2.$3.$4");
						} else if (pin.length() == 13) {
							pin = pin.replaceAll("(\\d{2})(\\d{6})(\\d{2})(\\d{3})", "0$1-$2.$3.$4");
						} else if (pin.length() == 10) {
							pin = pin.replaceAll("(\\d{2})(\\d{6})(\\d{2})", "$1-$2.$3.000");
						}
						return pin;
					}
				};
				
				module.addIterator(it);
				module.addFilter(rejectSavedDocuments);
				if(validateWithDates) {
					module.addValidator( recordedDateValidator );
					module.addCrossRefValidator( recordedDateValidator );
		        }
				modules.add(module);
	        }
			
			//address search
			if (hasStreet() && hasStreetNo()) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
				module.clearSaKeys();
				module.setParamValue(4, "AddressNum");
				module.setParamValue(5, sa.getAtribute(SearchAttributes.P_STREETNO));
				module.setParamValue(6, "Street");
				module.setParamValue(7, sa.getAtribute(SearchAttributes.P_STREETNAME));
				module.setParamValue(8, "City");
				module.setParamValue(9, sa.getAtribute(SearchAttributes.P_CITY));
				
				if (StringUtils.isNotEmpty(getSearchAttribute(SearchAttributes.P_STREETNAME))) {
					module.addFilter(rejectSavedDocuments);
					module.addFilter(subdivisionNameFilter);
					for (DocsValidator docsValidator : docsValidators) {
//						if (!docsValidator.getValidatorName().contains("PIN")){
//							module.addValidator(docsValidator);
//						}
						module.addValidator(docsValidator);
					}
					module.addValidator(multipleLegalValidator);
					
					for (DocsValidator docsValidator : docsValidators) {
//						if (!docsValidator.getValidatorName().contains("PIN")){
//							module.addCrossRefValidator(docsValidator);
//						}
						module.addCrossRefValidator(docsValidator);
					}
					module.addCrossRefValidator(multipleLegalValidator);
					
					modules.add(module);
				}
	        }
			
			//legal description search
//			{
//				if (StringUtils.isNotEmpty(getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME))) {
//					addLegalSearchModuleSLB(serverInfo, modules, TSServerInfo.ADV_SEARCH_MODULE_IDX, searchId, docsValidators);
//				}
//				
//				addLegalSearchModuleSLU(serverInfo, modules, TSServerInfo.ADV_SEARCH_MODULE_IDX, searchId, docsValidators);
//				addLegalSearchModuleSTR(serverInfo, modules, TSServerInfo.ADV_SEARCH_MODULE_IDX, searchId, docsValidators);
//			}
			
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
				module.addFilter(subdivisionNameFilter);
				if (isUpdate()) {
					module.addFilter(new RejectAlreadySavedDocumentsForUpdateFilter(searchId));
				}
				
				for (DocsValidator docsValidator : docsValidators) {
					module.addValidator(docsValidator);
				}
				module.addValidator(multipleLegalValidator);
				for (DocsValidator docsValidator : docsValidators) {
					module.addCrossRefValidator(docsValidator);
				}
				module.addCrossRefValidator(multipleLegalValidator);
				
				module.addValidator((new LastTransferDateFilter(searchId)).getValidator());
				
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);	
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
							.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
				module.addIterator(nameIterator);
				modules.add(module);
			

			//instrument search
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.clearSaKeys();
			    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			    module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH );
			    module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
			    addBetweenDateTest(module, false, false, false);
				modules.add(module);
				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.clearSaKeys();
				//module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			    module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH );
			    module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH );
			    module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
			    
			    OcrOrBootStraperIterator ocrBPIteratoriterator = new OcrOrBootStraperIterator(searchId);
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
				module.addFilter(pinFilter);
				module.addFilter(subdivisionNameFilter);
				//module.addFilter(genericLegalFilter);
				module.addFilter(defaultLegalFilter);
				
				if (isUpdate()) {
					module.addFilter(new RejectAlreadySavedDocumentsForUpdateFilter(searchId));
				}
				if(validateWithDates) {
			        module.addValidator(recordedDateValidator);
			        module.addCrossRefValidator(recordedDateValidator);
			    }

				module.addValidator((new LastTransferDateFilter(searchId)).getValidator());
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);	
				
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
							.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
				
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
				FilterResponse doctTypeFilter = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
				module.setSaObjKey(SearchAttributes.BUYER_OBJECT);	
				
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
				module.addIterator(buyerNameIterator);
				
				((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) nameFilter).setUseArrangements(false);
				((GenericNameFilter) nameFilter).setInitAgain(true);
				module.addFilter(rejectSavedDocuments);
				module.addFilter(nameFilter);
				module.addFilter(doctTypeFilter);
				module.addFilter(pinFilter);
				module.addFilter(subdivisionNameFilter);
				//module.addFilter(genericLegalFilter);
				module.addFilter(defaultLegalFilter);
				
				if (isUpdate()) {
					module.addFilter(new RejectAlreadySavedDocumentsForUpdateFilter(searchId));
				}
				if(validateWithDates) {
			        module.addValidator(recordedDateValidator);
			        module.addCrossRefValidator(recordedDateValidator);
			    }
				
				modules.add(module);
			}
			
			{				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_PL_EAS_RES_MASDEED);
				module.clearSaKeys();
				module.setParamValue(4, "Subdivision");
				module.setParamValue(6, "LotNumBegin");
				module.setParamValue(8, "Block");
				module.setParamValue(10, "UnitNum");
				
				PlatIterator it = new PlatIterator(searchId);
				module.addFilter(subdivisionNameFilter);
				module.addFilter(pinFilter);
				module.addValidator(genericLegalValidator);
				
				for (DocsValidator docsValidator : docsValidators) {
					module.addCrossRefValidator(docsValidator);
				}
				module.addIterator(it);
				modules.add(module);
				
			}
			
			// search by crossRef - instr# list from RO documents
			{
				InstrumentGenericIterator instrumentGenericIterator = getInstrumentIterator(false);
				instrumentGenericIterator.setLoadFromRoLike(true);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, true);
				
				if (!instrumentGenericIterator.createDerrivations().isEmpty()) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
					module.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);
					module.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_NOT_AGAIN);
					module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
					module.addIterator(instrumentGenericIterator);
					
					module.addFilter(rejectSavedDocuments);
					module.addFilter(pinFilter);
					module.addFilter(subdivisionNameFilter);
//					module.addFilter(genericLegalFilter);
					module.addFilter(defaultLegalFilter);
					
					if(validateWithDates) {
				        module.addValidator(recordedDateValidator);
				        module.addCrossRefValidator(recordedDateValidator);
				    }
					if (isUpdate) {
						module.addFilter(new BetweenDatesFilterResponse(searchId));
					}
					
					modules.add(module);
				}
				
				InstrumentGenericIterator bpGenericIterator = getInstrumentIterator(false);
				bpGenericIterator.setRemoveLeadingZerosBP(true);
				bpGenericIterator.setLoadFromRoLike(true);
				
				if (!bpGenericIterator.createDerrivations().isEmpty()) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
					module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
					module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, true);
					module.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);
					module.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_NOT_AGAIN);
					module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
					module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
					module.addIterator(bpGenericIterator);
					
					module.addFilter(rejectSavedDocuments);
					module.addFilter(pinFilter);
//					module.addFilter(genericLegalFilter);
					module.addFilter(defaultLegalFilter);
					
					if(validateWithDates) {
				        module.addValidator(recordedDateValidator);
				        module.addCrossRefValidator(recordedDateValidator);
				    }
					if (isUpdate) {
						module.addFilter(new BetweenDatesFilterResponse(searchId));
					}
					
					modules.add(module);
				}
			}
			
		}
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	private void addLegalSearchModuleSLB(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId, DocsValidator[] docsValidators) {
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
//		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK);
		module.clearSaKeys();
		
		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);

		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		it.setEnableSubdividedLegal(true);
		
		module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE);
		module.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_LOT);
		module.setIteratorType(9, FunctionStatesIterator.ITERATOR_TYPE_BLOCK);
		
		module.setSaKey(12, SearchAttributes.FROMDATE_MMM_DD_YYYY);
		module.setSaKey(13, SearchAttributes.TODATE_MMM_DD_YYYY);
		
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
	
	private void addLegalSearchModuleSLU(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId, DocsValidator[] docsValidators) {
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.clearSaKeys();
		
		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);

		if (StringUtils.isNotEmpty(getSearchAttribute(SearchAttributes.LD_SUBDIV_UNIT))) {
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
			it.setEnableSubdividedLegal(true);

			module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE);
			module.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_LOT);
			module.forceValue(9, getSearchAttribute(SearchAttributes.LD_SUBDIV_UNIT));
			
			module.setSaKey(12, SearchAttributes.FROMDATE_MMM_DD_YYYY);
			module.setSaKey(13, SearchAttributes.TODATE_MMM_DD_YYYY);
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
	}
	
	private void addLegalSearchModuleSTR(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId, DocsValidator[] docsValidators){
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.clearSaKeys();
		
		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		it.setEnableTownshipLegal(true);
		
		if (StringUtils.isNotEmpty(getSearchAttribute(SearchAttributes.LD_SUBDIV_SEC)) || 
			StringUtils.isNotEmpty(getSearchAttribute(SearchAttributes.LD_SUBDIV_TWN)) ||
			StringUtils.isNotEmpty(getSearchAttribute(SearchAttributes.LD_SUBDIV_RNG))) {
				module.setSaKey(12, SearchAttributes.FROMDATE_MMM_DD_YYYY);
				module.setSaKey(13, SearchAttributes.TODATE_MMM_DD_YYYY);
				
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
				
				String key = "OH_RO_LOOK_UP_DATA";
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
			protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str) {
					if (StringUtils.isNotEmpty(str.subName) && (StringUtils.isNotEmpty(str.lot) || StringUtils.isNotEmpty(str.unit))
							&& TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION))) {
						module.setData(5, str.subName);
						module.setData(9, str.block);
						if (StringUtils.isNotEmpty(str.lot)){
							module.setData(7, str.lot);
						} else if (StringUtils.isNotEmpty(str.unit)){
//							module.setData(7, str.unit);
							module.setData(11, str.unit);
						}
						module.setParamValue(4, "Subdivision");
						module.setParamValue(6, "LotNumBegin");
						module.setParamValue(8, "Block");
						module.setParamValue(10, "UnitNum");
						
					} else if (StringUtils.isEmpty(str.subName) && StringUtils.isNotEmpty(str.unit)
							&& TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_UNIT.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION))) {						    	
						module.setData(5, str.unit);
						module.setData(7, str.section);
						module.setData(9, str.township);
						module.setData(11, str.range);
//						module.setData(18, str.qtr);
						
						module.setParamValue(4, "UnitNum");
						module.setParamValue(6, "Section");
						module.setParamValue(8, "Town");
						module.setParamValue(10, "Range");
						
					} else if (StringUtils.isEmpty(str.subName) && StringUtils.isEmpty(str.unit) 
							 && StringUtils.isNotEmpty(str.qtr)
							 && TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION))) {
						module.setData(5, str.section);
						module.setData(7, str.township);
						module.setData(9, str.range);
						module.setData(11, str.qtr);
						
						module.setParamValue(4, "Section");
						module.setParamValue(6, "Town");
						module.setParamValue(8, "Range");
						module.setParamValue(10, "Acreage <=");
					}
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
	 
	 
	   public InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
			InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId) {

				private static final long serialVersionUID = 5399351945130601258L;

				@Override
				public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
					if (StringUtils.isNotEmpty(state.getInstno())) {
						if (filterCriteria != null) {
							filterCriteria.put("InstrumentNumber", state.getInstno());
						}
					}
					return state.getInstno();
				}
				
				@Override
				protected String cleanPage(String input) {
					if (org.apache.commons.lang.StringUtils.isNotEmpty(input)){
						if (input.matches("(?is)[A-Z]\\d")){
							return (input.substring(0, 1) + "0" + input.substring(1, 2));
						}
					}
					return super.cleanPage(input);
				}
			};

			if (instrumentType) {
				instrumentGenericIterator.enableInstrumentNumber();
			} else {
				instrumentGenericIterator.enableBookPage();
			}
			instrumentGenericIterator.setDoNotCheckIfItExists(true);
			return instrumentGenericIterator;
		}
	 
	 protected class PlatIterator extends GenericRuntimeIterator<PersonalDataStruct> {
			
			private static final long serialVersionUID = 793434519L;
			
			PlatIterator(long searchId) {
				super(searchId);
			}
			
			@SuppressWarnings("unchecked")
			List<PersonalDataStruct> createDerivationInternal(long searchId){
				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)global.getAdditionalInfo("OH_RO_LOOK_UP_DATA_SLB");
				
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
	 
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
//		if (!isParentSite() && "1".equals(module.getFunction(3).getParamValue()) && "".equals(module.getFunction(1).getParamValue())) {
//			return new ServerResponse();
//		}
		
		return super.SearchBy(module, sd);
	}
	
//	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
//		ConfigurableNameIterator nameIterator = null;
//		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
//		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId, true, true).getValidator();
//		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
//
//		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
//		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
//		TSServerInfoModule module;
//		GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
//
//		for (String id : gbm.getGbTransfers()) {
//
//			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
//			module.setVisible(true);
//			module.setIndexInGB(id);
//			module.setTypeSearchGB("grantor");
//			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
//			module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
//			String date = gbm.getDateForSearch(id, "MMM dd, yyyy", searchId);
//			if (date != null){
//				module.getFunction(19).forceValue(date);
//			}
//			
//			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
//			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
//					.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
//			module.addIterator(nameIterator);
//			module.addValidator(defaultLegalValidator);
//			module.addValidator(addressHighPassValidator);
//			module.addValidator(pinValidator);
//			module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(
//					searchId, 0.90d, module).getValidator());
//			module.addValidator(DateFilterFactory.getDateFilterForGoBack(
//					SearchAttributes.GB_MANAGER_OBJECT, searchId, module)
//					.getValidator());
//
//			modules.add(module);
//
//			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
//				module = new TSServerInfoModule(serverInfo
//						.getModule(TSServerInfo.NAME_MODULE_IDX));
//				module.setVisible(true);
//				module.setIndexInGB(id);
//				module.setTypeSearchGB("grantee");
//				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
//				module.addFilter(NameFilterFactory.getDefaultNameFilter(
//						SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
//				date = gbm.getDateForSearchBrokenChain(id, "MMM dd, yyyy",
//						searchId);
//				if (date != null){
//					module.getFunction(19).forceValue(date);
//				}
//				
//				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
//				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
//						.getConfigurableNameIterator(module, searchId,
//								new String[] { "L;F;" });
//				module.addIterator(nameIterator);
//				module.addValidator(defaultLegalValidator);
//				module.addValidator(addressHighPassValidator);
//				module.addValidator(pinValidator);
//				module.addValidator(NameFilterFactory
//						.getDefaultTransferNameFilter(searchId, 0.90d, module)
//						.getValidator());
//				module.addValidator(DateFilterFactory.getDateFilterForGoBack(
//						SearchAttributes.GB_MANAGER_OBJECT, searchId, module)
//						.getValidator());
//
//				modules.add(module);
//
//			}
//
//		}
//		serverInfo.setModulesForGoBackOneLevelSearch(modules);
//
//	}	

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
				
				rsResponse = rsResponse.replaceAll("<A HREF='","<a target=\"_blank\" href='" + CreatePartialLink(TSConnectionURL.idGET) + "/recordings/");
				rsResponse = rsResponse.replaceAll("<a href='", "<a HREF='" + CreatePartialLink(TSConnectionURL.idGET) + "/recordings/");

				rsResponse = rsResponse.replaceAll("<a HREF=(.*?)'(.*?)'>", "<a HREF='$1$2'>");
				

				if (Response.getRawQuerry().indexOf("Names+Summary") >= 0) {
					// automatic name search --> first the name summary list is retrieved
					String querry = Response.getQuerry();

					querry = querry.replaceAll("Names Summary", "Detail Data");
					if ((iTmp = rsResponse.indexOf("<form")) != -1) {
						int endIdx = rsResponse.indexOf("/form>", iTmp) + 6;
						rsResponse = rsResponse.substring(iTmp, endIdx);

						rsResponse = rsResponse.replaceAll("<input TYPE=\"checkbox\" NAME=\"Names\" Value=\"(.*?)\" alt=\"Select Name\">",
							"<a href='" + CreatePartialLink(TSConnectionURL.idPOST) + sAction + "&"	+ querry + "&Names=$1&automaticNameSearch=true'>View</a>");
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
								imageLink.append(CreatePartialLink(TSConnectionURL.idGET))
//												.append("/recording/LoadImage.asp&InstrID=").append(instr).append("&Year=").append(year);
									.append("/recordings/LoadImage.asp&InstrID=").append(instr).append("&Year=").append(year);
								Response.getParsedResponse().addImageLink(new ImageLinkInPage(imageLink.toString(), instr + ".tif"));
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
				
				Matcher crossRefLinkMatcher = iPatternRefLink.matcher(resultForCross);
				while(crossRefLinkMatcher.find()) {
					ParsedResponse prChild = new ParsedResponse();
					String link = CreatePartialLink(TSConnectionURL.idGET) + crossRefLinkMatcher.group(1) + "&isSubResult=true";
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
//			System.out.println(rsResponse);
			rsResponse = rsResponse.replaceAll("(?is)(<form[^>]*>)", "$1"
					+ new LinkParser(CreatePartialLink(TSConnectionURL.idPOST) + sAction + "?automaticNameSearch=true")
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
					if (mainTableList != null && mainTableList.size() > 0) {
						String instr = "";
						String bookPage = "";
						String docRefs = "";
						String recDate = "";
						String year = "";
						String consideration = "";
						String docType = "";
						String instrDate = "";
						String grantors = "";
						String grantees = "";
						String noOfPages = "";
						String legalDesc = "";
						
						TableTag mainTable = (TableTag) mainTableList.elementAt(0);
				
						TableColumn tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Instrument:").getParent();
						if (tc != null) {
							instr = HtmlParser3.getValueFromCell(tc, "", false);
							instr = instr.replaceAll("(?is)Instrument:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Marginal:").getParent();
						if (tc != null) {
							docRefs = HtmlParser3.getValueFromCell(tc, "", false);
							docRefs = docRefs.replaceAll("(?is)Marginal:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Volume Page:").getParent();
						if (tc != null) {
							bookPage = HtmlParser3.getValueFromCell(tc, "", false);
							bookPage = bookPage.replaceAll("(?is)Volume Page:\\s*", "").replaceAll("(?is)&nbsp;", " ").trim();
							bookPage = bookPage.replaceAll("(?is)\\b(?:OR\\b|D\\b|M\\b)?\\s*(\\d+\\s+\\d+)", "$1").trim().replaceAll("\\s+", " ");
						}
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Recorded:").getParent();
						if (tc != null) {
							recDate = HtmlParser3.getValueFromCell(tc, "", false);
							recDate = recDate.replaceAll("(?is)Recorded:", "").replaceAll("(?is)&nbsp;", " ").trim();
							if (StringUtils.isNotEmpty(recDate)) {
								year = recDate.replaceAll("(?is)\\A\\d+/\\d+/(\\d+).*", "$1");
							}
						}
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Consideration:").getParent();
						if (tc != null) {
							consideration = HtmlParser3.getValueFromCell(tc, "", false);
							consideration = consideration.replaceAll("(?is)Consideration:", "").replaceAll("(?is)&nbsp;", " ").replaceAll("[$,]+", "").trim();
						}
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Document Type:").getParent();
						if (tc != null) {
							docType = HtmlParser3.getValueFromCell(tc, "", false);
							docType = docType.replaceAll("(?is)Document Type:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Prepared:").getParent();
						if (tc != null) {
							instrDate = HtmlParser3.getValueFromCell(tc, "", false);
							instrDate = instrDate.replaceAll("(?is)Prepared:\\s*", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Pages:").getParent();
						if (tc != null) {
							noOfPages = HtmlParser3.getValueFromCell(tc, "", false);
							noOfPages = noOfPages.replaceAll("(?is)Pages:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantor:"), "", true);
						if (StringUtils.isEmpty(grantors)){
							grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Debtor:"), "", true);
						}
						grantors = grantors.replaceAll("(?is)&nbsp;", " ");
						
						grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantee:"), "", true);
						if (StringUtils.isEmpty(grantees)){
							grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Secured Party:"), "", true);
						}
						grantees = grantees.replaceAll("(?is)&nbsp;", " ");
						
						legalDesc = HtmlParser3.getValueFromNextCell(mainTableList, "Legal Description:", "", true);
						if (StringUtils.isNotEmpty(legalDesc) && legalDesc!= null) {
							legalDesc = legalDesc.replaceAll("(?is)</?i>", "").replaceAll("(?is)<br>", ", ").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String key = instr + "_" + docType.replaceAll("\\s+", "_");
						ParsedResponse currentResponse = responses.get(key);							 
						if(currentResponse == null) {
							currentResponse = new ParsedResponse();
							responses.put(key, currentResponse);
						}
						
						String imageLink = "";
						if (StringUtils.isNotEmpty(noOfPages) && !"0".equals(noOfPages)) {
							if (HtmlParser3.findNode(mainTable.getChildren(), "Display Doc") != null) {
//								imageLink = CreatePartialLink(TSConnectionURL.idGET) + "/recording/LoadImage.asp?InstrID=" + instr;
								imageLink = CreatePartialLink(TSConnectionURL.idGET) + "/recordings/LoadImage.asp?InstrID=" + instr;
								currentResponse.addImageLink(new ImageLinkInPage(imageLink, instr + ".tif"));
							}
						}
								
						RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();				
						
						ResultMap resultMap = new ResultMap();
								
						String link = CreatePartialLink(TSConnectionURL.idPOST) + "/recording/SimpleQuery.asp?Instrs=" + key;
						if(document == null) {	//first time we find this document
							String rowHtml =  mainTable.toHtml();
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
							String[] bp = bookPage.split("\\s+");
							if (bp.length == 2) {
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bp[0]);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), bp[1]);
							}
							
							resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
							if (StringUtils.isNotEmpty(recDate)) {
								resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate.replaceAll("(?is)\\A\\s*([\\d/]+)\\s+.*", "$1").trim());
							}
							
							if (StringUtils.isNotEmpty(consideration)) {
								resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), consideration);
							}
									
							resultMap.put("tmpPartyGtor", grantors);
							resultMap.put("tmpPartyGtee", grantees);
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDesc);
							try {
								GenericDTSFunctionsRO.parseNamesRO(resultMap, searchId);
								//GenericDTSFunctionsRO.parseLegal(resultMap, searchId);
								GenericDTSFunctionsRO.parseMultipleLegal(resultMap, searchId);
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							resultMap.removeTempDef();
				    				
							@SuppressWarnings("unchecked")
							Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) resultMap.get("PropertyIdentificationSet");
							if (pisVector != null && !pisVector.isEmpty()){
								String tmpPropDesc = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
								for (PropertyIdentificationSet everyPis : pisVector){
									//everyPis.setAtribute("PropertyDescription", tmpPropDesc);
									currentResponse.addPropertyIdentificationSet(everyPis);
								}
							}
				    		
							resultMap.remove(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
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
								LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
								currentResponse.setPageLink(linkInPage);
								checkBox = "<input type='checkbox' name='docLink' value='" 	+ link +  "'>Select for saving to TS Report";
								/**
								 * Save module in key in additional info. The key is instrument number that should be always available. 
								 */
								String keyForSavingModules = this.getKeyForSavingInIntermediary(instr);
								search.setAdditionalInfo(keyForSavingModules, moduleSource);
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
					String link = CreatePartialLink(TSConnectionURL.idPOST) + "/recordings/" + action + "?";
					
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
				String instr = "";
				String bookPage = "";
				String recDate = "";
				String instrDate = "";
				String docType = "";
				String consideration = "";
				TableColumn tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Instrument:").getParent();
				if (tc != null) {
					instr = HtmlParser3.getValueFromCell(tc, "", false);
					instr = instr.replaceAll("(?is)Instrument:\\s*", "").replaceAll("(?is)&nbsp;", " ").trim();
					if (StringUtils.isNotEmpty(instr)) {
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
					}
				}
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Volume Page:").getParent();
				if (tc != null) {
					bookPage = HtmlParser3.getValueFromCell(tc, "", false);
					bookPage = bookPage.replaceAll("(?is)Volume Page:\\s*", "").replaceAll("(?is)&nbsp;", " ").trim();
					bookPage = bookPage.replaceAll("(?is)\\b(?:OR\\b|D\\b|M\\b)?\\s*(\\d+\\s+\\d+)", "$1").trim().replaceAll("\\s+", " ");
					if (StringUtils.isNotEmpty(bookPage)) {
						String[] bp = bookPage.split("\\s+");
						if (bp.length == 2){
							resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bp[0]);
							resultMap.put(SaleDataSetKey.PAGE.getKeyName(), bp[1]);
						}
					}
				}
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Recorded:").getParent();
				if (tc != null) {
					recDate = HtmlParser3.getValueFromCell(tc, "", false);
					recDate = recDate.replaceAll("(?is)Recorded:\\s*", "").replaceAll("(?is)&nbsp;", " ").trim();
					if (StringUtils.isNotEmpty(recDate)) {
						String year = recDate.replaceAll("(?is)\\A\\d+/\\d+/(\\d+).*", "$1");
						recDate = recDate.replaceAll("(?is)\\A\\s*([\\d/]+)\\s+.*", "$1").trim();
						if (StringUtils.isNotEmpty(recDate)) {
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
						}
					}
				}
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Document Type:").getParent();
				if (tc != null) {
					docType = HtmlParser3.getValueFromCell(tc, "", false);
					docType = docType.replaceAll("(?is)Document Type:\\s*", "").replaceAll("(?is)&nbsp;", " ").trim();
					if (StringUtils.isNotEmpty(docType)) {
						resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
					}
				}
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Consideration:").getParent();
				if (tc != null) {
					consideration = HtmlParser3.getValueFromCell(tc, "", false);
					consideration = consideration.replaceAll("(?is)Consideration:\\s*", "").replaceAll("(?is)&nbsp;", " ").replaceAll("[$,]+", "").trim();
					if (StringUtils.isNotEmpty(consideration)) {
						if (DocumentTypes.isMortgageDocType(docType, searchId)){
							resultMap.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), consideration);
						} else {
							resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), consideration);
						}
					}
				}
				
				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Prepared:").getParent();
				if (tc != null) {
					instrDate = HtmlParser3.getValueFromCell(tc, "", false);
					instrDate = instrDate.replaceAll("(?is)Prepared:\\s*", "").replaceAll("(?is)&nbsp;", " ").trim();
					if (StringUtils.isNotEmpty(instrDate)) {
						resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrDate);
					}
				}

				
				String grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantor:"), "", true);
				if (StringUtils.isEmpty(grantors)){
					grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Debtor:"), "", true);
				} 
				grantors = grantors.replaceAll("(?is)&nbsp;", " ");
				
				String grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantee:"), "", true);
				if (StringUtils.isEmpty(grantees)){
					grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Secured Party:"), "", true);
				}
				grantees = grantees.replaceAll("(?is)&nbsp;", " ");
				
				resultMap.put("tmpPartyGtor", grantors);
				resultMap.put("tmpPartyGtee", grantees);
				
				GenericDTSFunctionsRO.parseNamesRO(resultMap, searchId);
				
				String legalDesc = HtmlParser3.getValueFromNextCell(mainTableList, "Legal Description:", "", true);
				legalDesc = legalDesc.replaceAll("(?is)</?i>", "").replaceAll("(?is)<br>", ", ");
				legalDesc = legalDesc.replaceAll("(?is)&nbsp;", " ").trim();
				if (StringUtils.isNotEmpty(legalDesc)) {
					resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDesc);
					GenericDTSFunctionsRO.parseMultipleLegal(resultMap, searchId);
				}
				resultMap.remove(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
//				tc = (TableColumn) HtmlParser3.findNode(mainTable.getChildren(), "Comments:").getParent();
//				String comments = HtmlParser3.getValueFromCell(tc, "", false);
//				comments = comments.replaceAll("(?is)Comments:", "");
//				comments = comments.replaceAll("(?is)</?i>", "").replaceAll("(?is)<br>", ", ").replaceAll("(?is)&nbsp;", " ");
//				resultMap.put("tmpComments", comments);
				
				//extract cross refs from legal description
				List<List> bodyCR = new ArrayList<List>();
				List<String> line = new ArrayList<String>();
				
				Matcher matcher = iPattern.matcher(rsResponse);
				while (matcher.find()) {
					String crossRefInfo = matcher.group(1);
					if (crossRefInfo != null) {
						if (crossRefInfo.matches("\\d{15}")) {
							line = new ArrayList<String>();
							line.add("");
							line.add("");
							line.add(crossRefInfo);
							bodyCR.add(line);
						}
					} 
				}
				
				matcher = bkPgPattern.matcher(rsResponse);
				while (matcher.find()) {
					String crossRefInfo = matcher.group(1).trim();
					if (crossRefInfo != null) {
						if (crossRefInfo.matches("\\d+\\s+\\d+")) {
							line = new ArrayList<String>();
							crossRefInfo = crossRefInfo.replaceFirst("\\s+", " ");
							String[] bkPg = crossRefInfo.split("\\s");
							if (bkPg.length == 2) {
								line.add(bkPg[0]);
								line.add(bkPg[1]);
							} else {
								line.add("");
								line.add("");
							}
							line.add("");
							bodyCR.add(line);
						}
					}
				}
				
				if (!bodyCR.isEmpty()){
					String[] header = { "Book", "Page", "InstrumentNumber" };
					ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
					resultMap.put("CrossRefSet", rt);
				}
			}
			
		} catch(Exception e) {
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
    	if (vsRequest.toLowerCase().contains("simplequery")) {//when points to a crossrefs link from doc index, or next/prev link
    		return super.GetLink(vsRequest, vbEncoded); 
    	}
    	String instr = StringUtils.extractParameterFromUrl(vsRequest, "InstrID");
    	instr = instr.trim();
     	// construct fileName
    	String folderName = getCrtSearchDir() + "Register" + File.separator;
		new File(folderName).mkdirs();
    	String fileName = folderName + instr + ".tif";
    	
    	// retrieve the image
 		retrieveImage(instr, fileName, searchId);
    	
		// write the image to the client web-browser
		boolean imageOK = writeImageToClient(fileName, "image/tiff");
		
		// image not retrieved
		if(!imageOK) { 
	        // return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);			
		}
		
		// return solved response
		return ServerResponse.createSolvedResponse();
    }

    public boolean retrieveImage(String inst, String fileName, long searchId){
    	byte[] imageBytes = null;

		HttpSite site = HttpManager.getSite("OHLickingRO", searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http2.OHLickingRO)site).getImage(inst, fileName);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}

		ServerResponse resp = new ServerResponse();
		if (imageBytes == null) {
			return false;
		}
		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, "image/tiff"));

		if (!ro.cst.tsearch.utils.FileUtils.existPath(fileName)) {
			FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), fileName);
		}

		return true;
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
		if (retrieveImage(instr, fileName, searchId)) {
			byte b[] = FileUtils.readBinaryFile(fileName);
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
    
	//@Override
	protected void logSearchBy(TSServerInfoModule module, Map<String, String> params) {
		if (module.isVisible() || "GB_MANAGER_OBJECT".equals(module.getSaObjKey())) {// B 4511

			// get parameters formatted properly
			Map<String, String> moduleParams = params;
			if (moduleParams == null) {
				moduleParams = module.getParamsForLog();
			}
			Search search = getSearch();
			// determine whether it's an automatic search
			boolean automatic = (search.getSearchType() != Search.PARENT_SITE_SEARCH) || (GPMaster.getThread(searchId) != null);
			boolean imageSearch = module.getLabel().equalsIgnoreCase("image search") || module.getModuleIdx() == TSServerInfo.IMG_MODULE_IDX;

			// create the message
			StringBuilder sb = new StringBuilder();
			SearchLogFactory sharedInstance = SearchLogFactory.getSharedInstance();
			SearchLogPage searchLogPage = sharedInstance.getSearchLogPage(searchId);
			sb.append("</div>");

			Object additional = GetAttribute("additional");
			if (Boolean.TRUE != additional) {
				searchLogPage.addHR();
				sb.append("<hr/>");
			}
			int fromRemoveForDB = sb.length();

			// searchLogPage.
			sb.append("<span class='serverName'>");
			String serverName = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName();
			sb.append(serverName);
			sb.append("</span> ");

			sb.append(automatic ? "automatic" : "manual");
			Object info = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
			if (StringUtils.isNotEmpty(module.getLabel())) {

				if (info != null) {
					sb.append(" - " + info + "<br>");
				}
				sb.append(" <span class='searchName'>");
				sb.append(module.getLabel());
			} else {
				sb.append(" <span class='searchName'>");
				if (info != null) {
					sb.append(" - " + info + "<br>");
				}
			}
			sb.append("</span> by ");
			
			boolean firstTime = true;
			boolean skipThisParam = false;
			
			for (Entry<String, String> entry : moduleParams.entrySet()) {
				String value = entry.getValue();
				String key = entry.getKey();
				key = key.replaceFirst(":", "");
				value = value.replaceFirst(":", "");
				
				Matcher mat = Pattern.compile("Legal type\\[(\\d+)\\]").matcher(key);
				String idxParam = "";
				if (!key.contains("Legal")) {
					skipThisParam = false;
				}
				if (mat.find()) {
					key = value;
					idxParam = mat.group(1).trim();
					value = moduleParams.get("Legal Description[" + idxParam + "]");
					skipThisParam = false;
				} else {
					mat = Pattern.compile("Legal Description\\[\\d\\]").matcher(key);
					mat.reset();
					if (mat.find()) {
						skipThisParam = true;
					}
				}
				 
				if (value != null && !skipThisParam) {
					if (!firstTime) {
						sb.append(", ");
					} else {
						firstTime = false;
					}
					sb.append(key + " = <b>" + value + "</b>");
				}
				
			}
			
			int toRemoveForDB = sb.length();
			// log time when manual is starting
			if (!automatic || imageSearch) {
				sb.append(" ");
				sb.append(SearchLogger.getTimeStamp(searchId));
			}
			sb.append(":<br/>");

			// log the message
			SearchLogger.info(sb.toString(), searchId);
			ModuleShortDescription moduleShortDescription = new ModuleShortDescription();
			moduleShortDescription.setDescription(sb.substring(fromRemoveForDB, toRemoveForDB));
			moduleShortDescription.setSearchModuleId(module.getModuleIdx());
			search.setAdditionalInfo(TSServerInfoConstants.TS_SERVER_INFO_MODULE_DESCRIPTION, moduleShortDescription);
			String user = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getAttribute(1).toString();
			SearchLogger.info(ro.cst.tsearch.utils.StringUtils.createCollapsibleHeader(), searchId);
			searchLogPage.addModuleSearchParameters(serverName, additional, info, moduleParams, module.getLabel(), automatic, imageSearch, user);
		}
	}
}
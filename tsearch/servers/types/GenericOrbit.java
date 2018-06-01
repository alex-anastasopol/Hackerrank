package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
//import org.codehaus.jettison.json.JSONObject;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
//import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.DuplicateInstrumentFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringCleaner;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;

@SuppressWarnings("deprecation")
public class GenericOrbit extends TSServerROLike {

	private static final long serialVersionUID = 8641836011176455822L;	
	
	public static final int INSTRUMENT_SEARCH = 0;
	public static final int NAME_SEARCH = 1;
	public static final int LEGAL_SEARCH_LEVEL1 = 2;
	public static final int LEGAL_SEARCH_LEVEL2 = 3;
	public static final int COURT_SEARCH = 4;
	
	public static final String STR_PATTERN = "(\\d+)-(\\d+)-(\\d+)(?:\\s*(?:[NSEW]+|ALL|PT)\\b.*)?";
	public static final String NAME_PATTERN1 = ",\\s*(H/W|W/H|A[SM]P|DBA|[FN]KA|SGL|H/H)\\b";
	public static final String BOOK_PAGE_PATTERN = "(\\d+)/(\\d+)";
	public static final String IMAGE_LINK_PATTERN = "(?is)<img[^>]+onclick\\s*=\\s*\"ViewDocument\\('([^']+)'\\)";
	
	public static final String DOCUMENT_NUMBER_DETAILS_TEXT = "DocumentNumberSearch/DocumentSubdivisionDetail";
	public static final String NAME_DETAILS_TEXT = "GrantorGranteeSearch/GrantorGranteeDetail";
	public static final String LEGAL_DETAILS_TEXT = "SubdivisionSearch/Detail";
	public static final String COURT_DETAILS_TEXT = "CourtNameSearch/Detail";

	public GenericOrbit(long searchId) {
		super(searchId);
	}

	public GenericOrbit(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		setNotLoggable(msiServerInfoDefault);
		
		return msiServerInfoDefault;
	}
	
	protected void setNotLoggable(TSServerInfo serverInfo) {
		int[] modules = new int[]{TSServerInfo.INSTR_NO_MODULE_IDX, TSServerInfo.NAME_MODULE_IDX, TSServerInfo.SUBDIVISION_MODULE_IDX};
		
		for (int moduleIndex: modules) {
			TSServerInfoModule tsServerInfoModule = serverInfo.getModule(moduleIndex);
			if (tsServerInfoModule!=null) {
				if (tsServerInfoModule.getFunctionCount() > 0) {
					tsServerInfoModule.getFunction(0).setLoggable(false);
				}
			}
		}
	}
	
	protected ArrayList<NameI> addNameSearch(
			List<TSServerInfoModule> modules,
			TSServerInfo serverInfo,
			String key,
			String extraInformation,
			ArrayList<NameI> searchedNames, 
			FilterResponse[] filters,
			DocsValidator[] docsValidators,
			DocsValidator[] docsValidatorsCrossref) {
		
		List<NameI> newNames = new ArrayList<NameI>();
		
		newNames.addAll(addNameSearch(modules, key, extraInformation, searchedNames, filters, docsValidators, docsValidatorsCrossref, 
				new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX)), true, false));
		
		newNames.addAll(addNameSearch(modules, key, extraInformation, searchedNames, filters, docsValidators, docsValidatorsCrossref, 
				new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX)), false, false));
		
		newNames.addAll(addNameSearch(modules, key, extraInformation, searchedNames, filters, docsValidators, docsValidatorsCrossref, 
				new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38)), false, true));
		
		newNames.addAll(addNameSearch(modules, key, extraInformation, searchedNames, filters, docsValidators, docsValidatorsCrossref, 
				new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38)), false, false));
		
		if(searchedNames == null) {
			searchedNames = new ArrayList<NameI>();
		}
		searchedNames.addAll(newNames);
		
		return searchedNames;
		
	}
	
	protected ArrayList<NameI> addNameSearch(
			List<TSServerInfoModule> modules, 
			String key, 
			String extraInformation,
			ArrayList<NameI> searchedNames, 
			FilterResponse[] filters,
			DocsValidator[] docsValidators,
			DocsValidator[] docsValidatorsCrossref,
			TSServerInfoModule module,
			boolean isGrantor,
			boolean isPerson) {
		
		ArrayList<NameI> newNames = new ArrayList<NameI>();
		
		if (module==null) {
			return newNames;
		}
		
		if (module.getModuleIdx() != TSServerInfo.NAME_MODULE_IDX && module.getModuleIdx() != TSServerInfo.MODULE_IDX38) {
			return newNames;
		}
		
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, extraInformation);
		module.setSaObjKey(key);
		module.clearSaKeys();
		
		if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {		//Grantor or Grantee Search
			module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			module.setSaKey(1, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			module.setSaKey(2, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			module.forceValue(0, "true");
			if (isGrantor) {
				module.forceValue(3, "1");
			} else {
				module.forceValue(3, "2");
			}
			module.forceValue(5, "false");
		} else if (module.getModuleIdx() == TSServerInfo.MODULE_IDX38) {	//Court Name Search
			if (isPerson) {
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			} else {
				module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			}
			module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			module.setSaKey(1, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			module.forceValue(7, "1");
			module.forceValue(8, "false");
		}
    	
		DocsValidator dateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator();
		
		if(filters != null) {
			for (FilterResponse filterResponse : filters) {
				module.addFilter(filterResponse);
			}
		}
		if(docsValidators != null) {
			for (DocsValidator docsValidator : docsValidators) {
				module.addValidator(docsValidator);
			}
		}
		if(applyDateFilter()) {
			module.addValidator( dateValidator );
		}
		if(docsValidatorsCrossref != null) {
			for (DocsValidator docsValidator : docsValidatorsCrossref) {
				module.addCrossRefValidator(docsValidator);
			}
		}
		if(applyDateFilter()) {
			module.addCrossRefValidator( dateValidator );
		}
		
		module.addFilterForNext(new NameFilterForNext(searchId));
		
		ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(module, false, searchId, new String[] {"L;F;" });
		if (module.getModuleIdx() == TSServerInfo.MODULE_IDX38) {
			if (isPerson) {
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
			} else {
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
			}
		}
		nameIterator.setInitAgain(true);		//initialize again after all parameters are set
		
		if (searchedNames!=null) {
			nameIterator.setSearchedNames(searchedNames);
		}
		newNames.addAll(nameIterator.getSearchedNames());
		
		module.addIterator(nameIterator);
		modules.add(module);
		
		return newNames;
	}
	
	//to be overridden in subclasses, if necessary
	protected InstrumentGenericIterator getInstrumentNumberIterator() {
		return new InstrumentGenericIterator(searchId, dataSite);
	}
	
	private LegalDescriptionIterator getLegalDescriptionIterator(boolean lookupWasDoneWithName) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookupWasDoneWithName, false, getDataSite()) {

			private static final long serialVersionUID = 6904762874540799155L;

			@Override
			protected Set<LegalStruct> keepOnlyGoodLegals(Set<LegalStruct> legals){
				Set<LegalStruct> good = new HashSet<LegalStruct>();
				for (LegalStruct str : legals){
					if (!StringUtils.isEmpty(str.getAddition())&&(!StringUtils.isEmpty(str.getLot())||!StringUtils.isEmpty(str.getBlock()))) {
						good.add(str);
					}
				}
				return good;
			}
			
			@Override
			public void processSubdivisionName(Search global, String originalLot,
					String originalBlock, String originalUnit,
					Set<String> temporarySubdivisionsForCondoSearch) {
				if (legalStruct.size()>0) {		//if we have legal description from look-up, we do not add the legal description from Search Page 
					return;
				}
				super.processSubdivisionName(global, originalLot, originalBlock, originalUnit, temporarySubdivisionsForCondoSearch);
			}
			
			public List<LegalStruct> createDerrivations() {
				
				List<LegalStruct> result = super.createDerrivations();
				
				List<LegalStruct> all = new ArrayList<LegalStruct>();
				for (LegalStruct legalStruct: result) {
					String subdivisionName = legalStruct.getAddition();
					String[] subdivisions = null;
					if (CountyConstants.KS_Johnson_STRING.equals(getSearch().getCountyId())) {
						subdivisions = KSJohnsonRO.getSubdivisions(subdivisionName, searchId);
					} else {
						subdivisions = new String[]{subdivisionName};
					}
					for (String subd: subdivisions) {
						LegalStruct clone = (LegalStruct)legalStruct.clone();
						clone.setAddition(subd);
						if (!all.contains(clone)) {
							all.add(clone);
						}
					}
				}
				
				if (all.isEmpty()) {
					printSubdivisionException();
				}
				
				return all;
			}
			
			@Override
			protected void loadDerrivation(TSServerInfoModule module, LegalStruct str) {
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
						case FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE:
							function.setParamValue(str.getAddition());
							break;
						}
					}
				}
			}

		};
		
		it.setEnableSubdividedLegal(true);
		it.setEnableSubdivision(true);
		it.setEnableTownshipLegal(false);
		
		return it;
	}
	
	private NameFilterForNext getNameFilterForNext(String key, long searchId,boolean useSubdivisionName,TSServerInfoModule module, 
			boolean ignoreSuffix, int stringCleaner) {
		
		NameFilterForNext nffn = new NameFilterForNext(key, searchId, useSubdivisionName, module, ignoreSuffix, stringCleaner) {

			private static final long	serialVersionUID	= -3931518786974019039L;
			
			@SuppressWarnings("unchecked")
			@Override
			public void init(){
				
 				if(useSubdivisionNameAsReference){
					Set<String> allSubdivisionNames = (Set<String>)getSearch().getAdditionalInfo(AdditionalInfoKeys.SUBDIVISION_NAME_SET);
					if (allSubdivisionNames!=null && allSubdivisionNames.size()>0) {		//we have legal from look up
						setThreshold(BigDecimal.valueOf(0.9d));
						setRef.clear();		//clear references from Search Page
						for (String s: allSubdivisionNames) {
							setRef.add(new Name("","", StringCleaner.cleanString(stringCleaner, s)));
						}
						try {
							DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
							long miServerId = dataSite.getServerId();
							for (LegalI legal : sa.getForUpdateSearchLegalsNotNull(miServerId)) {
								setRef.add(new Name("","", StringCleaner.cleanString(stringCleaner, legal.getSubdivision().getName() )));
							}
							
							
						} catch (Exception e) {
							logger.error("Error loading names for Update saved from Parent Site", e);
						}
					}
				}
				
				if (setRef.size()==0) {
					super.init();
				}
			
			}
			
		};
		
		return nffn;
	}
	
	protected void printSubdivisionException () {
		 Search search = getSearch();
		 if(search!=null && !search.isSubdivisionExceptionPrintedRO() && search.getSearchType() == Search.AUTOMATIC_SEARCH &&
				 "false".equals(search.getSa().getAtribute(SearchAttributes.SEARCHFINISH))) {
				 search.setSubdivisionExceptionPrintedRO(true);
				 IndividualLogger.info("Will not search with subdivision name because either subdivision " +
				 		"is missing or both lot and block are mising.", searchId);
			     SearchLogger.info("<font color=\"red\"><BR>Will not search with subdivision name because either subdivision " +
					 	"is missing or both lot and block are mising.</font><BR>", searchId);	 
		 }
	 }
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		if(getSearch().getSearchType() == Search.AUTOMATIC_SEARCH) {
			
			setNotLoggable(serverInfo);
		
			TSServerInfoModule module;
			
			RejectAlreadySavedDocumentsFilterResponse alreadyFilter = new RejectAlreadySavedDocumentsFilterResponse(searchId){
				/**
				 * 
				 */
				private static final long serialVersionUID = -5786000984528375627L;

				@Override
				public InstrumentI formatInstrument(InstrumentI instrument){
					
					if (TSServer.getCrtTSServerName(miServerID).contains("Platte")){
						String instrumentNo = instrument.getInstno();
						if (instrumentNo.matches("(?is)\\A\\d+.*") && instrumentNo.length() > 4){
							instrumentNo = instrumentNo.substring(0, 4) 
			    					+ org.apache.commons.lang.StringUtils.leftPad(instrumentNo.substring(4, instrumentNo.length()), 6, '0');
							instrument.setInstno(instrumentNo);
						}
					}

					return instrument;
				}
				
				@Override
				public BigDecimal getScoreOneRow(ParsedResponse row) {
					
					//task 8104
					if (TSServer.getCrtTSServerName(miServerID).contains("Clay")) {
						DocumentI document = row.getDocument();
						if(document != null) {
							Search search = getSearch();
							DocumentsManagerI managerI = search.getDocManager();
							try {
								managerI.getAccess();
								
								InstrumentI clone = document.getInstrument().clone();
								
								clone = formatInstrument(clone);
								
								if(isIgnoreDocumentCategory()) {
									clone.setDocType("");
									clone.setDocSubType("");
								} else if (isIgnoreDocumentSubcategory()) {
									clone.setDocSubType("");
								}
								
								if(managerI.getDocumentsWithInstrumentsFlexible(false,clone).size() >= 1) {
									return ATSDecimalNumberFormat.ZERO;
								}
								
								String instno = clone.getInstno();
								if (instno.matches("\\d{6,7}")) {
									instno = instno.substring(instno.length()-5);
									clone.setInstno(instno);
									List<DocumentI> documents = managerI.getDocumentsList();
						    		for (DocumentI eachDocument: documents) {
						    			String newInstno = eachDocument.getInstno();
						    			if (newInstno.matches("[A-Z]\\d{5}")) {
						    				newInstno = newInstno.substring(newInstno.length()-5);
						    				DocumentI newDoc = document.clone();
						    				newDoc.setInstno(newInstno);
						    				if (newDoc.flexibleEquals(clone)) {
						    					return ATSDecimalNumberFormat.ZERO;
						    				}
						    			}	
						    		}
								}
								
							} catch (Throwable t) {
								logger.error("Error computing score for RejectAlreadySavedDocumentsFilterResponse", t);
							} finally {
								managerI.releaseAccess();
							}
						}
						return ATSDecimalNumberFormat.ONE;
					} else {
						return super.getScoreOneRow(row);
					}
					
				}
				
			};
			alreadyFilter.setIgnoreDocumentCategory(true);
			
			FilterResponse legalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
			DocsValidator legalValidator = legalFilter.getValidator();
			DocsValidator alreadyValidator = alreadyFilter.getValidator();
			
			GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			defaultNameFilter.setIgnoreMiddleOnEmpty(true);
			defaultNameFilter.setUseNameEquivalenceForFilter(true);
			defaultNameFilter.setInitAgain(true);
			
			int cleaner = StringCleaner.MO_CLAY_OR_SUB;
			FilterResponse subdivisionFilter = NameFilterFactory.getNameFilterForSubdivisionWithCleaner(searchId,cleaner);
			subdivisionFilter.setThreshold(BigDecimal.valueOf(0.8d));
			((GenericNameFilter)subdivisionFilter).setUseSubdivisionNameAsReference(true);
			DocsValidator subdivisionValidator = subdivisionFilter.getValidator();
			
			boolean lookupWasDoneWithInstrument = false;
			
			//Instrument module - searching with instrument list extracted from Assessor/Tax documents
			InstrumentGenericIterator instrumentGenericIterator = getInstrumentNumberIterator();
			instrumentGenericIterator.enableInstrumentNumber();
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
			module.setSaKey(1, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setSaKey(2, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			module.forceValue(0, "true");
			module.forceValue(6, "false");
			module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			module.addValidator(alreadyValidator);
			if(applyDateFilter()) {
				DocsValidator dateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator();
				module.addValidator( dateValidator );
				module.addCrossRefValidator( dateValidator );
			}
			module.addIterator(instrumentGenericIterator);
			if(!lookupWasDoneWithInstrument) {
				lookupWasDoneWithInstrument = !instrumentGenericIterator.createDerrivations().isEmpty();
			}
			modules.add(module);
			
			//Instrument module - searching with book-page list extracted from Assessor/Tax documents
			InstrumentGenericIterator bookPageGenericIterator = new InstrumentGenericIterator(searchId, dataSite);
			bookPageGenericIterator.enableBookPage();
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, ro.cst.tsearch.servers.info.TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
			module.setSaKey(1, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setSaKey(2, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			module.forceValue(0, "true");
			module.forceValue(6, "false");
			module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			module.addValidator(alreadyValidator);
			if(applyDateFilter()) {
				DocsValidator dateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator();
				module.addValidator( dateValidator );
				module.addCrossRefValidator( dateValidator );
			}
			module.addIterator(bookPageGenericIterator);
			if(!lookupWasDoneWithInstrument) {
				lookupWasDoneWithInstrument = !bookPageGenericIterator.createDerrivations().isEmpty();
			}
			modules.add(module);
			
			//if no references found on AO/Tax like documents, search with owners for finding Legal
			if(!lookupWasDoneWithInstrument) {
				addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, null,
						new FilterResponse[]{defaultNameFilter, alreadyFilter},
						new DocsValidator[]{legalValidator, subdivisionValidator},
						new DocsValidator[]{legalValidator, subdivisionValidator});
			}
			
			//search with legal
			LegalDescriptionIterator it = getLegalDescriptionIterator(!lookupWasDoneWithInstrument);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setSaKey(2, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE);
			module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LOT);
			module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_BLOCK);
			module.addFilter(legalFilter);
			NameFilterForNext subdivisionFilterForNext = getNameFilterForNext("", searchId, true, module, false,cleaner);
			subdivisionFilterForNext.setThreshold(BigDecimal.valueOf(0.8d));
			subdivisionFilterForNext.setUseSubdivisionNameAsCandidat(true);
			subdivisionFilterForNext.setUseSubdivisionNameAsReference(true);
			subdivisionFilterForNext.setInitAgain(true);
			module.addFilterForNext(subdivisionFilterForNext);
			module.addValidators(legalValidator, alreadyValidator);
			if(applyDateFilter()) {
				DocsValidator dateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
				module.addValidator( dateValidator );
				module.addCrossRefValidator( dateValidator );
			}
			module.addIterator(it);
			modules.add(module);
			
			ArrayList<NameI> searchedNames = null;
			
			//search with owners
			if (hasOwner()) {
				searchedNames = addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, null,
						new FilterResponse[]{defaultNameFilter, alreadyFilter},
						new DocsValidator[]{legalValidator, subdivisionValidator},
						new DocsValidator[]{legalValidator, subdivisionValidator});
			}

			//OCR last transfer - instrument number search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setSaKey(2, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			module.forceValue(0, "true");
			module.forceValue(6, "false");
			module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			module.getFunction(3).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
			module.addValidator(alreadyValidator);
			if(applyDateFilter()) {
				DocsValidator dateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator();
				module.addValidator( dateValidator );
				module.addCrossRefValidator( dateValidator );
			}
			modules.add(module);
			
			//OCR last transfer - book and page search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setSaKey(2, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			module.forceValue(0, "true");
			module.forceValue(6, "false");
			module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			module.getFunction(4).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			module.getFunction(5).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			module.addValidator(alreadyValidator);
			if(applyDateFilter()) {
				DocsValidator dateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator();
				module.addValidator( dateValidator );
				module.addCrossRefValidator( dateValidator );
			}
			modules.add(module);
			
			//search with extra owners from search page (for example added by OCR)	
			addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, searchedNames,
					new FilterResponse[]{defaultNameFilter, alreadyFilter},
					new DocsValidator[]{legalValidator, subdivisionValidator},
					new DocsValidator[]{legalValidator, subdivisionValidator});
			
			//search with buyers
			if(hasBuyer()) {
				FilterResponse nameFilterBuyer = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, getSearch().getID(), null);
				addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS, null,
						new FilterResponse[]{nameFilterBuyer, DoctypeFilterFactory.getDoctypeBuyerFilter(searchId), alreadyFilter},
						new DocsValidator[]{},
						new DocsValidator[]{});
			}
			
		}
				
		serverInfo.setModulesForAutoSearch(modules);
	}

	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		if (Search.GO_BACK_ONE_LEVEL_SEARCH==getSearch().getSearchType()) {
			
			setNotLoggable(serverInfo);
			
			ConfigurableNameIterator nameIterator = null;
			String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

			SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
			List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
			TSServerInfoModule module;
			GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

			for (String id : gbm.getGbTransfers()) {
				
				String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);

				{
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));	//Grantor or Grantee Search
					module.setIndexInGB(id);
					module.setTypeSearchGB("grantor");
					module.clearSaKeys();
					module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

					if (date != null) {
						module.getFunction(1).forceValue(date);
					}
					module.setValue(2, endDate);
					module.forceValue(0, "true");
					module.forceValue(3, "1");		//grantor
					module.forceValue(5, "false");

					module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
					nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
					module.addIterator(nameIterator);
					module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
					module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					modules.add(module);
				}
				
				{
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));	//Grantor or Grantee Search
					module.setIndexInGB(id);
					module.setTypeSearchGB("grantor");
					module.clearSaKeys();
					module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

					if (date != null) {
						module.getFunction(1).forceValue(date);
					}
					module.setValue(2, endDate);
					module.forceValue(0, "true");
					module.forceValue(3, "2");		//grantee
					module.forceValue(5, "false");

					module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
					nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
					module.addIterator(nameIterator);
					module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
					module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					modules.add(module);
				}
				
				{
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38));	//Court Name Search, person name
					module.setIndexInGB(id);
					module.setTypeSearchGB("grantor");
					module.clearSaKeys();
					module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

					if (date != null) {
						module.getFunction(0).forceValue(date);
					}
					module.setValue(1, endDate);
					module.forceValue(7, "1");
					module.forceValue(8, "false");

					module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
					module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
					nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
					nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
					nameIterator.clearSearchedNames();
					nameIterator.setInitAgain(true);
					module.addIterator(nameIterator);
					module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
					module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					modules.add(module);
				}
				
				{
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38));	//Court Name Search, company name
					module.setIndexInGB(id);
					module.setTypeSearchGB("grantor");
					module.clearSaKeys();
					module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

					if (date != null) {
						module.getFunction(0).forceValue(date);
					}
					module.setValue(1, endDate);
					module.forceValue(7, "1");
					module.forceValue(8, "false");

					module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
					nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
					nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
					nameIterator.clearSearchedNames();
					nameIterator.setInitAgain(true);
					module.addIterator(nameIterator);
					module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
					module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					modules.add(module);
				}

				if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
					
					{
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));	//Grantor or Grantee Search
						module.setIndexInGB(id);
						module.setTypeSearchGB("grantee");
						module.clearSaKeys();
						module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

						if (date != null) {
							module.getFunction(1).forceValue(date);
						}
						module.setValue(2, endDate);
						module.forceValue(0, "true");
						module.forceValue(3, "1");		//grantor
						module.forceValue(5, "false");

						module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
						module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
						nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
						module.addIterator(nameIterator);
						module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
						module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
						modules.add(module);
					}
					
					{
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));	//Grantor or Grantee Search
						module.setIndexInGB(id);
						module.setTypeSearchGB("grantee");
						module.clearSaKeys();
						module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

						if (date != null) {
							module.getFunction(1).forceValue(date);
						}
						module.setValue(2, endDate);
						module.forceValue(0, "true");
						module.forceValue(3, "2");		//grantee
						module.forceValue(5, "false");

						module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
						module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
						nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
						module.addIterator(nameIterator);
						module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
						module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
						modules.add(module);
					}
					
					{
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38));	//Court Name Search, person name
						module.setIndexInGB(id);
						module.setTypeSearchGB("grantee");
						module.clearSaKeys();
						module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

						if (date != null) {
							module.getFunction(0).forceValue(date);
						}
						module.setValue(1, endDate);
						module.forceValue(7, "1");
						module.forceValue(8, "false");

						module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
						module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
						module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
						nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
						nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
						nameIterator.clearSearchedNames();
						nameIterator.setInitAgain(true);
						module.addIterator(nameIterator);
						module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
						module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
						modules.add(module);
					}
					
					{
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38));	//Court Name Search, company name
						module.setIndexInGB(id);
						module.setTypeSearchGB("grantee");
						module.clearSaKeys();
						module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

						if (date != null) {
							module.getFunction(0).forceValue(date);
						}
						module.setValue(1, endDate);
						module.forceValue(7, "1");
						module.forceValue(8, "false");

						module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
						module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
						nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
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
		
	}

	private String getDetails(String rsResponse, StringBuilder accountId, HashMap<String, String> data, int mode) {
		
		String details = "";
		
		if(!rsResponse.contains("<form")){
			details = rsResponse;
		}
		
        try {
			
			if ("".equals(details)) {
				org.htmlparser.Parser htmlParser = null; htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
				NodeList nodeList = htmlParser.parse(null);

				NodeList divList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true);
				for (int i=0;i<divList.size();i++) {
					Div div = (Div)divList.elementAt(i);
					String attr = div.getAttribute("style"); 
					if (attr!=null && attr.contains("border-top")) {
						div.removeAttribute("style");
						details = div.toHtml();
						break;
					}
				}
				
				details = details.replaceAll("(?is)&cent;","");
		        
		        if (mode==COURT_SEARCH) {
		        	StringBuffer sb = new StringBuffer();
		        	Matcher ma = Pattern.compile("(?is)(<a[^>]+href=\")([^\"]+)(\"[^>]*>[^<]*</a>)").matcher(details);
		        	while (ma.find()) {
		        		if (ma.group(2).startsWith("/"+COURT_DETAILS_TEXT)) {
		        			ma.appendReplacement(sb, ma.group(1) + CreatePartialLink(TSConnectionURL.idGET) + ma.group(2) + ma.group(3));
		        		} else {
		        			ma.appendReplacement(sb, "");
		        		}
		        	}
		        	ma.appendTail(sb);
		        	details = sb.toString();
		        } else {
		        	details = details.replaceAll("(?is)<a[^>]*>.*?</a>", "");
		        }
				
		        details = details.replaceAll("(?is)<input[^>]*>", "");
		        
		        details = details.replaceAll("(?is)<table[^>]*>\\s*<tr[^>]*>\\s*<td>\\s*<div[^>]*>\\s*<span[^>]*>\\s*</span>\\s*</div>\\s*</td>\\s*</tr>\\s*</table>", "");		//remove empty table
		        
		        Matcher maImgLink = Pattern.compile("(?is)<input[^>]+onclick\\s*=\\s*\"ViewDocument\\('([^']+)'\\)").matcher(rsResponse);
				if (maImgLink.find()) {
					if (mode==COURT_SEARCH) {
						details += "<br>";
					}
					details += "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/Search/ViewDocument?para=" + maImgLink.group(1) + "\" target=\"_blank\">View Image</a><br><br>";
				}
			}
			
			
			org.htmlparser.Parser htmlParser2 = org.htmlparser.Parser.createParser(details, null);
			NodeList nodeList2 = htmlParser2.parse(null);
			
			String type = "";
			String docno = "";
			String book = "";
			String page = "";
			String date = "";
			type = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList2, "Document Type:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
			book = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList2, "Document Book:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
			page = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList2, "Document Page:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
			if (mode==COURT_SEARCH) {
				docno = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList2, "Primary Document ID:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
				date = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList2, "Docketed Date:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
			} else {
				docno = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList2, "Document Number:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
				date = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList2, "Recorded Date:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
			}
			
			String year = "";
			int index = date.lastIndexOf("/");
			if (index>-1 && index<date.length()-1) {
				year = date.substring(index+1);
			}
			
			accountId.append(docno);
			data.put("type", type);
			data.put("docno", docno);
			data.put("book", book);
			data.put("page", page);
			data.put("year", year);
			
	    } catch (Exception e) {
			e.printStackTrace();
		}	
        
		return details;
		
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		boolean isCourt = isCourt(Response);
		
		String url = "";
		URI uri = Response.getLastURI();
		if (uri!=null) {
			try {
				url = uri.getURI();
			} catch (URIException e) {
				e.printStackTrace();
			}
		} else {
			url = Response.getQuerry();
		}
		if (url==null) {
			url = "";
		}
		
		switch(viParseID) {

		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_SEARCH_BY_MODULE38:				//Court Name Search
			
			if (rsResponse.indexOf("No matches were found. ") > -1) {
				Response.getParsedResponse().setError(NO_DATA_FOUND);
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size()==0) {
				return;
			}

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			if (viParseID==ID_SEARCH_BY_SUBDIVISION_NAME && getSearch().getSearchType()!=Search.PARENT_SITE_SEARCH) {
				parsedResponse.setAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE, true);
			}
			
			Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
			String footer = "";
			StringBuilder nextLink = new StringBuilder();
			String navigationLinks = getNavigationLinks(rsResponse, nextLink);
			if (nextLink.length()>0) {
				parsedResponse.setNextLink(nextLink.toString());
			}

			if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
				footer = "</table>" + navigationLinks + "</td></tr></table>"
					+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
			} else {
				footer = "</table>" + navigationLinks + "</td></tr></table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
			}

			parsedResponse.setFooter(footer);
			
			break;
			
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			
			StringBuilder serialNumber = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(rsResponse, serialNumber, data, isCourt?COURT_SEARCH:INSTRUMENT_SEARCH);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				try {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + URLDecoder.decode(originalLink, "UTF-8");
					
					if (isInstrumentSaved(serialNumber.toString(), null, data, false)){
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
					
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
			} else {
				smartParseDetails(Response, details);
				String detailsWithLinks = details;
				
				String imageLinkPatt = "(?is)<a[^>]+href=\"[^\"]+\"[^>]*>View\\s+Image</a>";
				Matcher ma = Pattern.compile(imageLinkPatt).matcher(details);
				if (ma.find()) {
					String link = CreatePartialLink(TSConnectionURL.idGET) + "/viewimage.asp?docno=" + serialNumber;
					if (isCourt) {
						link += "&isCourt=true";
					}
					parsedResponse.addImageLink(new ImageLinkInPage(link, serialNumber + ".pdf"));
					details = details.replaceAll(imageLinkPatt, "");
				}
				
				//remove links
				details = details.replaceAll("(?is)<a[^>]+href=[^>]*>([^<]*)</a>", "$1");
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				saveRelatedDocuments(Response, detailsWithLinks, isCourt);
				
			}
			break;	
			
		case ID_GET_LINK:

			boolean isDetail = false;
			boolean isSubdivInterm = false;
			isDetail = url.contains(DOCUMENT_NUMBER_DETAILS_TEXT) || 
			   		   url.contains(NAME_DETAILS_TEXT) || 
			   		   url.contains(LEGAL_DETAILS_TEXT) ||
			   		   url.contains(COURT_DETAILS_TEXT);
			if (!isDetail) {
				if (url.contains(ro.cst.tsearch.connection.http3.GenericOrbit.LEGAL_SEARCH_TEXT)) {
					isSubdivInterm = true;
				}
			}
			
			ParseResponse(sAction, Response, isDetail?ID_DETAILS:(isSubdivInterm?ID_SEARCH_BY_SUBDIVISION_NAME:ID_SEARCH_BY_NAME));
			
		}
		
	}
	
	@SuppressWarnings({ "unchecked" })
	protected void saveRelatedDocuments(ServerResponse Response, String details, boolean isCourt) {
		
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		if (isCourt) {
			//cross-references as links
			Matcher ma = Pattern.compile("(?is)<a[^>]+href=\\\"[^\"]+Link=(/" + COURT_DETAILS_TEXT + "[^\"]+)\"[^>]*>[^>]+</a>").matcher(details);
			while (ma.find()) {
				String link = CreatePartialLink(TSConnectionURL.idGET) + ma.group(1);
				ParsedResponse prChild = new ParsedResponse();
				LinkInPage pl = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
				prChild.setPageLink(pl);
				parsedResponse.addOneResultRowOnly(prChild);
			}
		} else {
			Vector<CrossRefSet> vectorCRS = (Vector<CrossRefSet>)parsedResponse.infVectorSets.get("CrossRefSet");
			String srcType = null;
			OtherInformationSet ois = (OtherInformationSet) parsedResponse.infSets.get("OtherInformationSet");
			if (ois!=null) {
				srcType = ois.getAtribute(OtherInformationSetKey.SRC_TYPE.getShortKeyName()); 
			}
	        if (srcType == null){
	        	srcType = "";
	        }
	        
	        Set<InstrumentI> crossRefs = ro.cst.tsearch.extractor.xml.Bridge.extractCrossRefs(vectorCRS, srcType, searchId);
	        Iterator<InstrumentI> it = crossRefs.iterator();
	        while (it.hasNext()) {
	        	InstrumentI instr = it.next();
	        	String instno = instr.getInstno();
	        	String book = instr.getBook();
	        	String page = instr.getPage();
	        	String link = getDetailsLink(instno, book, page);
	    		if (!StringUtils.isEmpty(link)) {
	        		link = CreatePartialLink(TSConnectionURL.idGET) + link;
					ParsedResponse prChild = new ParsedResponse();
					LinkInPage pl = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
					prChild.setPageLink(pl);
					Response.getParsedResponse().addOneResultRowOnly(prChild);
				}
			}
		}
		
    }
	
	protected String getDetailsLink(String instno, String book, String page) {
		String link = "";
		
		HTTPRequest req = null;
		String response = "";
		
		if (!StringUtils.isEmpty(instno)) {
			req = new HTTPRequest(ro.cst.tsearch.connection.http3.GenericOrbit.DOCUMENT_NUMBER_SEARCH_TEXT + 
					"?SubdivisionDocNumberType=true&IsRemainDataDocumentNumberForm=false&DocumentID=" + instno, HTTPRequest.GET);
		} else if (!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)) {
			req = new HTTPRequest(ro.cst.tsearch.connection.http3.GenericOrbit.DOCUMENT_NUMBER_SEARCH_TEXT + 
					"?SubdivisionDocNumberType=true&IsRemainDataDocumentNumberForm=false&DocBook=" + book + "&DocPage=" + page, HTTPRequest.GET);
		}
		
		if (req!=null) {
			String serverName = getCrtTSServerName(miServerID);
			HttpSite3 site = HttpManager3.getSite(serverName, searchId);
			try {
				response = site.process(req).getResponseAsString();
			} finally {
				HttpManager3.releaseSite(site);
			}
		}	
		
		if (!StringUtils.isEmpty(response)) {
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
				NodeList nodeList = htmlParser.parse(null);

				NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "searchTable"), true);
				
				if (mainTableList.size()>=0) {
					HashSet<String> set = new HashSet<String>();
					TableTag tbl = (TableTag) mainTableList.elementAt(0);
					for (int i=1;i<tbl.getRowCount();i++) {
						set.add(tbl.getRow(i).getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" + 
								tbl.getRow(i).getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" + 
								tbl.getRow(i).getColumns()[7].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" + 
								tbl.getRow(i).getColumns()[8].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim());
					}
					if (set.size()==1) {		//exactly one result
						Matcher maDetLink = Pattern.compile("(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>").matcher(tbl.getRow(1).getColumns()[5].toHtml());
						if (maDetLink.find()) {
							link = maDetLink.group(1);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
		
		return link;
	}
	
	protected String getNavigationLinks(String response, StringBuilder nextLink) {

		try {
			
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(response, null);
			NodeList nodeList = parser.parse(null);
			NodeList mainDivList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "pager"), true);
			
			if (mainDivList.size()>0) {
				Div mainDiv = (Div)mainDivList.elementAt(0);
				mainDiv.setAttribute("align", "center");
				String div = "<br>" + mainDiv.toHtml();
				String partialLink = CreatePartialLink(TSConnectionURL.idGET);
				div = div.replaceAll("(?is)href=\"([^\"]+)\"", "href=\"" + partialLink + "$1\"");
				
				Matcher ma = Pattern.compile("(?is)<a[^>]+(href=\"[^\"]+\")[^>]*>Next</a>").matcher(div);
				if (ma.find()) {
					nextLink.append("<a ").append(ma.group(1).replaceAll("(?is)&amp;", "&")).append(">");
				}
				
				return div;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return "";
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);

			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "searchTable"), true);
			
			if (mainTableList.size()==0) {
				return intermediaryResponse;
			}

			TableTag tbl = (TableTag) mainTableList.elementAt(0);
			
			//group the rows which have the same Doc Number, Doc Type and Recorded
			LinkedHashMap<String, List<TableRow>> rowsMap = new  LinkedHashMap<String, List<TableRow>>();
			
			TableRow[] rows  = tbl.getRows();
			if (tbl.getRowCount()<=1) {
				return intermediaryResponse;
			}
			
			int mode = -1;
			int len = rows.length;
			for (int i=1;i<len;i++) {
				
				TableRow row = rows[i];
					
				String key = "";
				if (row.getColumnCount()==6) {				//Property Search; these columns do not need to be concatenated
					key = row.getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" + 	//Book
				          row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" + 	//Page
						  row.getColumns()[3].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();			//Subdivision
					List<TableRow> value = new ArrayList<TableRow>();
					value.add(row);
					rowsMap.put(key, value);
					mode = LEGAL_SEARCH_LEVEL1;
				} else {
					if (row.getColumnCount()==7) {
						mode = COURT_SEARCH;				//Court Name Search
					} else if (row.getColumnCount()==12) {
						if (rows[0].getHeaders()[10].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim().equalsIgnoreCase("Block")) {
							mode = LEGAL_SEARCH_LEVEL2;		//Property Search level 2
						} else {								
							mode = NAME_SEARCH;				//Grantor or Grantee Search	
						}
					} else if (row.getColumnCount()==13) {
						mode = INSTRUMENT_SEARCH;			//Document Number Search
					}	
					if (mode==NAME_SEARCH) {
						key = row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" +		//Doc Number
							  row.getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" + 	//Doc Type
							  row.getColumns()[7].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();			//Recorded
					} else if (mode==INSTRUMENT_SEARCH || mode==LEGAL_SEARCH_LEVEL2) {
						key = row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" + 	//Doc Number
							  row.getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" + 	//Doc Type
							  row.getColumns()[7].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" + 	//Book/Page
							  row.getColumns()[8].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();			//Recorded
					} else if (mode==COURT_SEARCH) {
						key = row.getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" +		//Doc Type
							  row.getColumns()[3].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim() + "_" + 	//Docketed
							  row.getColumns()[4].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();			//Primary Doc ID
					}
					if (!"".equals(key)) {
						if (rowsMap.containsKey(key)) {			//row already added
							List<TableRow> value = rowsMap.get(key);
							value.add(row);
						} else {								//add new row
							List<TableRow> value = new ArrayList<TableRow>();
							value.add(row);
							rowsMap.put(key, value);
						}
					}
				}  
			}
			
			int i=1;
			Iterator<Entry<String, List<TableRow>>> it = rowsMap.entrySet().iterator();
			while (it.hasNext()) {
				
				String detailsLink = "";
				String subdivLink = "";
			
				Map.Entry<String, List<TableRow>> pair = (Map.Entry<String, List<TableRow>>)it.next();
				List<TableRow> value = pair.getValue();
				
				StringBuilder sb = new StringBuilder();
				
				if (mode==COURT_SEARCH) {
					sb.append(getComposedTD(value, 1, false));																																	//Name
					
					sb.append("<td style=\"vertical-align: middle;\">").append(value.get(0).getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim()).append("</td>");			//Doc Type
					
					sb.append("<td style=\"vertical-align: middle;\">").append(value.get(0).getColumns()[3].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim()).append("</td>");			//Docketed
					
					String docNumber = value.get(0).getColumns()[4].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					Matcher maDetLink = Pattern.compile("(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>").matcher(value.get(0).getColumns()[1].toHtml());
					if (maDetLink.find()) {
						detailsLink = CreatePartialLink(TSConnectionURL.idGET) + maDetLink.group(1);
						docNumber = "<a href=\"" + detailsLink + "\">" + docNumber + "</a>";
					}
					sb.append("<td style=\"vertical-align: middle;\">").append(docNumber).append("</td>");																						//Primary Doc ID
					
					sb.append("<td style=\"vertical-align: middle;\">").append(value.get(0).getColumns()[5].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim()).append("</td>");			//Sat./Rel.
					
					String imageLink = "";
					Matcher maImgLink = Pattern.compile(IMAGE_LINK_PATTERN).matcher(value.get(0).getColumns()[6].toHtml());
					if (maImgLink.find()) {
						imageLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/Search/ViewDocument?para=" + maImgLink.group(1) + "\" target=\"_blank\">View</a>";
					}
					sb.append("<td style=\"vertical-align: top;text-align: center;\">").append(imageLink).append("</td>");																		//Image
				} else if (mode==LEGAL_SEARCH_LEVEL1) {
					String book = value.get(0).getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					sb.append("<td style=\"vertical-align: top;\">").append(book).append("</td>");																								//Book
					
					String page = value.get(0).getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					sb.append("<td style=\"vertical-align: top;\">").append(page).append("</td>");																								//Page
					
					sb.append("<td style=\"vertical-align: top;\">").append(value.get(0).getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim()).append("</td>");			//Property Type
					
					String subdivision = value.get(0).getColumns()[3].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					Matcher maDetLink = Pattern.compile("(?is)<a[^>]+onclick\\s*=\\s*\"GetSubdivisionName\\s*\\(\\s*'([^']+)'\\s*,\\s*'([^']+)'\\s*,\\s*'([^']+)'\\s*\\)\\s*;\\s*\"[^>]*>")
							.matcher(value.get(0).getColumns()[3].toHtml().replaceAll("(?is)&quot;", "'"));
					if (maDetLink.find()) {
						String query = response.getQuerry() + "&";
						query = "SubdivisionSearch/Search?" + query.replaceAll("(?is)([?&]SubdivisionName=).*?(&)", "$1" + maDetLink.group(1) + "$2") + 
								"BookSelected=" + book + "&PageSelected=" + page;
						subdivLink = CreatePartialLink(TSConnectionURL.idGET) + query;
						subdivision = "<a href=\"" + subdivLink + "\">" + subdivision + "</a>";
					}
					sb.append("<td style=\"vertical-align: top;\">").append(subdivision).append("</td>");																						//Subdivision
					
					sb.append("<td style=\"vertical-align: top;\">").append(value.get(0).getColumns()[4].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim()).append("</td>");			//Comment
					
					String imageLink = "";
					Matcher maImgLink = Pattern.compile(IMAGE_LINK_PATTERN).matcher(value.get(0).getColumns()[5].toHtml());
					if (maImgLink.find()) {
						imageLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/Search/ViewDocument?para=" + maImgLink.group(1) + "\" target=\"_blank\">View</a>";
					}
					sb.append("<td style=\"vertical-align: top;text-align: center;\">").append(imageLink).append("</td>");																		//Image
				} else {
					String docNumber = value.get(0).getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					Matcher maDetLink = Pattern.compile("(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>").matcher(value.get(0).getColumns()[5].toHtml());
					if (maDetLink.find()) {
						detailsLink = CreatePartialLink(TSConnectionURL.idGET) + maDetLink.group(1);
						docNumber = "<a href=\"" + detailsLink + "\">" + docNumber + "</a>";
					}
					sb.append("<td style=\"vertical-align: middle;\">").append(docNumber).append("</td>");																						//Doc Number
					
					sb.append("<td style=\"vertical-align: middle;\">").append(value.get(0).getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim()).append("</td>");			//Doc Type
					
					sb.append(getComposedTD(value, 3, false));																																	//Ref Doc
					
					sb.append("<td style=\"vertical-align: middle;\">").append(value.get(0).getColumns()[4].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim()).append("</td>");			//Doc Amount
					
					sb.append(getComposedTD(value, 5, false));																																	//Grantor
					
					sb.append(getComposedTD(value, 6, false));																																	//Grantee
					
					if (mode==NAME_SEARCH) {
						sb.append("<td style=\"vertical-align: middle;\">").append(value.get(0).getColumns()[7].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim()).append("</td>");		//Recorded
						
						sb.append(getComposedTD(value, 8, true));																																//Lot
						
						sb.append(getComposedTD(value, 9, true));																																//Block
						
						sb.append(getComposedTD(value, 10, true)); 																																//Subdivision
						
						String imageLink = "";
						Matcher maImgLink = Pattern.compile("(?is)<img[^>]+onclick\\s*=\\s*\"ViewDocument\\('([^']+)'\\)").matcher(value.get(0).getColumns()[11].toHtml());
						if (maImgLink.find()) {
							imageLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/Search/ViewDocument?para=" + maImgLink.group(1) + "\" target=\"_blank\">View</a>";
						}
						sb.append("<td style=\"vertical-align: middle;text-align: center;\">").append(imageLink).append("</td>");																//Image
					} else if (mode==INSTRUMENT_SEARCH || mode==LEGAL_SEARCH_LEVEL2) {
						sb.append("<td style=\"vertical-align: middle;\">").append(value.get(0).getColumns()[7].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim()).append("</td>");		//Book/Page
						
						sb.append("<td style=\"vertical-align: middle;\">").append(value.get(0).getColumns()[8].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim()).append("</td>");		//Recorded
						
						sb.append(getComposedTD(value, 9, true));																																//Lot
						
						sb.append(getComposedTD(value, 10, true));																																//Block
						
						if (mode==INSTRUMENT_SEARCH) {
							sb.append(getComposedTD(value, 11, true));																															//Subdivision
							
							String imageLink = "";
							Matcher maImgLink = Pattern.compile("(?is)<img[^>]+onclick\\s*=\\s*\"ViewDocument\\('([^']+)'\\)").matcher(value.get(0).getColumns()[12].toHtml());
							if (maImgLink.find()) {
								imageLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/Search/ViewDocument?para=" + maImgLink.group(1) + "\" target=\"_blank\">View</a>";
							}
							sb.append("<td style=\"vertical-align: middle;text-align: center;\">").append(imageLink).append("</td>");															//Image
						} else {
							
							String imageLink = "";
							Matcher maImgLink = Pattern.compile("(?is)<img[^>]+onclick\\s*=\\s*\"ViewDocument\\('([^']+)'\\)").matcher(value.get(0).getColumns()[11].toHtml());
							if (maImgLink.find()) {
								imageLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/Search/ViewDocument?para=" + maImgLink.group(1) + "\" target=\"_blank\">View</a>";
							}
							sb.append("<td style=\"vertical-align: middle;text-align: center;\">").append(imageLink).append("</td>");															//Image
						}
					}
				}
				
				String htmlRow = sb.toString();
				
				ResultMap m = ro.cst.tsearch.servers.functions.GenericOrbit.parseIntermediaryRow(htmlRow, mode, searchId, getDataSite().getCountyId());
				m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
				
				String rowType = "1";
				if (i%2==0) {
					rowType = "2";
				}
					
				ParsedResponse currentResponse = new ParsedResponse();
					
				currentResponse.setPageLink(new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD));
				
				String type = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName()));
				String docno = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));
				String book = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.BOOK.getKeyName()));
				String page = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.PAGE.getKeyName()));
				String date = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.RECORDED_DATE.getKeyName()));
				String year = "";
				int index = date.lastIndexOf("/");
				if (index>-1 && index<date.length()-1) {
					year = date.substring(index+1);
				}
				
				String checkBox = "checked";
				
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", type);
				data.put("docno", docno);
				data.put("book", book);
				data.put("page", page);
				data.put("year", year);
				
				if (isInstrumentSaved(docno, null, data, false)) {
					checkBox = "saved";
				} else {
					numberOfUncheckedElements++;
					LinkInPage linkInPage = new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD);
					checkBox = "<input type='checkbox' name='docLink' value='" + detailsLink + "'>";
					currentResponse.setPageLink(linkInPage);
				}
				String rowHtml = "<tr class=\"row" + rowType + "\">";
				if (mode!=LEGAL_SEARCH_LEVEL1) {
					rowHtml +=  "<td align=\"center\">" + checkBox + "</td>";
				}
				rowHtml += htmlRow + "</tr>";
									
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
				currentResponse.setOnlyResponse(rowHtml);
				 
				Bridge bridge = new Bridge(currentResponse, m, searchId);
					
				DocumentI document = (RegisterDocumentI)bridge.importData();				
				currentResponse.setDocument(document);
				
				if (mode==LEGAL_SEARCH_LEVEL1 && getSearch().getSearchType()!=Search.PARENT_SITE_SEARCH) {
					LinkInPage linkInPage = new LinkInPage(subdivLink, subdivLink, TSServer.REQUEST_GO_TO_LINK);
					currentResponse.setPageLink(linkInPage);
				}
				
				intermediaryResponse.add(currentResponse);
				
				i++;
				
			}
			
			outputTable.append(table);
			if (mode==LEGAL_SEARCH_LEVEL1) {
				numberOfUncheckedElements = 0;
			}
			SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
			String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
			
			if (mode==COURT_SEARCH) {
				header += "<table width=\"100%\"><tr><td><table width=\"100%\">" + 
						"<tr bgcolor=\"#7DA7D9\"><th>" + SELECT_ALL_CHECKBOXES + "</th><th>Name</th><th>Doc Type</th>" + 
						"<th>Docketed</th><th>Primary Doc ID</th><th>Sat./Rel.</th><th>Image</th></tr>";
			} else if (mode==LEGAL_SEARCH_LEVEL1) {
				header += "<table width=\"100%\"><tr><td><table width=\"100%\">" + 
						"<tr bgcolor=\"#7DA7D9\"><th width=\"7%\">Book</th><th width=\"7%\">Page</th><th width=\"5%\">Property Type</th>" + 
						"<th width=\"45%\">Subdivision</th><th width=\"30%\">Comment</th><th width=\"6%\">Image</th></tr>";
			} else {
				header += "<table width=\"100%\"><tr><td><table width=\"100%\">" + "<tr bgcolor=\"#7DA7D9\"><th>" + SELECT_ALL_CHECKBOXES + "</th>" + 
						"<th>Doc Number</th><th>Doc Type</th><th>Ref Doc</th>"; 
				if (mode==LEGAL_SEARCH_LEVEL2) {
					header += "<th>Amount</th>";
				} else {
					header += "<th>Doc Amount</th>";
				}
				header += "<th>Grantor</th><th>Grantee</th>";
				if (mode==INSTRUMENT_SEARCH || mode==LEGAL_SEARCH_LEVEL2) {
					header += "<th>Book/Page</th>";
				}
				header += "<th>Recorded</th><th>Lot</th><th>Block</th>";
				if (mode!=LEGAL_SEARCH_LEVEL2) {
					header += "<th>Subdivision</th>";
				}
				header += "<th>Image</th></tr>";
			}
			response.getParsedResponse().setHeader(header);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	private String getComposedTD(List<TableRow> value, int index, boolean keepDuplicates) {
		StringBuilder sb = new StringBuilder();
		sb.append("<td style=\"vertical-align: middle;\"><table>");
		sb.append("<tr><td>").append(value.get(0).getColumns()[index].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim()).append("</td></tr>");
		for (int j=1;j<value.size();j++) {
			String s1 = value.get(j).getColumns()[index].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
			if (keepDuplicates && "".equals(s1)) {
				s1 = "&nbsp;";
			}
			String s2 = "<tr><td>" + s1 + "</td></tr>";
			if (keepDuplicates || sb.indexOf(s2)==-1) {
				sb.append(s2);
			}
		}
		sb.append("</table></td>");
		return sb.toString();
	}
	
	protected boolean isCourt(ServerResponse response) {
		boolean isCourt = false;
		String url = "";
		URI uri = response.getLastURI();
		if (uri!=null) {
			try {
				url = uri.getURI();
			} catch (URIException e) {
				e.printStackTrace();
			}
		} else {
			url = response.getQuerry();
		}
		if (url==null) {
			url = "";
		}
		if (url.contains(COURT_DETAILS_TEXT)) {
			isCourt = true;
		}
		return isCourt;
	}
		
	@SuppressWarnings("rawtypes")
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		
		boolean isCourt = isCourt(response);
		
		int countyId = getDataSite().getCountyId();
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
			
			String documentType = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Document Type:"), "", false, false);
			documentType = StringUtils.prepareStringForHTML(documentType);
			map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), documentType.trim());
			
			String documentNumber = "";
			if (isCourt) {
				documentNumber = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Primary Document ID:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
			} else {
				documentNumber = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Document Number:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
			}
			map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
			
			if (isCourt) {
				String caseNumber = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Serial or Case Number:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
				if (!StringUtils.isEmpty(caseNumber)) {
					map.put(CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(), caseNumber);
				}
			}
			
			String documentBook = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Document Book:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
			map.put(SaleDataSetKey.BOOK.getKeyName(), documentBook);
			
			String documentPage = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Document Page:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
			map.put(SaleDataSetKey.PAGE.getKeyName(), documentPage);
			
			String amount = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Amount:"), "", false, false).replaceAll("(?is)&nbsp;", " ").replaceAll("[$,]", "").trim();
			if (!"".equals(amount)) {
				String docCateg = DocumentTypes.getDocumentCategory(documentType, searchId);
				if (docCateg.equals(DocumentTypes.LIEN)) {
					map.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), amount);
				} else if (docCateg.equals(DocumentTypes.MORTGAGE)) {
					map.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), amount);
				}
			}
			
			String datedDate = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Dated Date:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
			map.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), datedDate);
			
			String recordedDate = "";
			if (isCourt) {
				recordedDate = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Docketed Date:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
			} else {
				recordedDate = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Recorded Date:"), "", false, false).replaceAll("(?is)&nbsp;", " ").trim(); 
			}
			map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
			
			StringBuilder sbGrantor = new StringBuilder();
			NodeList grantorNodeList = null;
			if (isCourt) {
				grantorNodeList = HtmlParser3.findNodeList(nodeList, "Creditor:"); 
			} else {
				grantorNodeList = HtmlParser3.findNodeList(nodeList, "Grantor:");
			}
			if (grantorNodeList!=null) {
				for (int i=0;i<grantorNodeList.size();i++) {
					String grantor = HtmlParser3.getValueFromAbsoluteCell(0, 1, (TextNode)grantorNodeList.elementAt(i), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
					sbGrantor.append(ro.cst.tsearch.servers.functions.GenericOrbit.cleanName(grantor)).append(" / ");
				}
			}
			
			StringBuilder sbGrantee = new StringBuilder();
			NodeList granteeNodeList = null;
			if (isCourt) {
				granteeNodeList = HtmlParser3.findNodeList(nodeList, "Debtor:");
			} else {
				granteeNodeList = HtmlParser3.findNodeList(nodeList, "Grantee:");
			}
			if (granteeNodeList!=null) {
				for (int i=0;i<granteeNodeList.size();i++) {
					String grantee = HtmlParser3.getValueFromAbsoluteCell(0, 1, (TextNode)granteeNodeList.elementAt(i), "", false, false).replaceAll("(?is)&nbsp;", " ").trim();
					sbGrantee.append(ro.cst.tsearch.servers.functions.GenericOrbit.cleanName(grantee)).append(" / ");
				}
			}
			
			TableTag refTable = null;
			TableTag legalTable = null;
			
			if (isCourt) {
				NodeList divList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("style", "margin-top: 5px"));
				Div div = null;
				for (int i=0;i<divList.size();i++) {
					Div eachDiv = (Div)divList.elementAt(i);
					if (eachDiv.toPlainTextString().contains("Reference Document")) {
						div = eachDiv;
						break;
					}
				}
				if (div!=null) {
					NodeList tablesList = div.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if (tablesList.size()>0) {
						refTable = (TableTag)tablesList.elementAt(0);
					}		
				}
			} else {
				NodeList tablesList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "searchTable"));
				for (int i=0;i<tablesList.size()&&(refTable==null||legalTable==null);i++) {
					TableTag table = (TableTag)tablesList.elementAt(i);
					if (table.getRowCount()>1 && table.getRow(0).getHeaderCount()>0) {
						String label = table.getRow(0).getHeaders()[0].toPlainTextString().trim();
						if (label.equals("Ref Document ID")) {
							refTable = table;
						} else if (label.equals("Lot")) {
							legalTable = table;
						}
					}
				}
			}
			
			if (refTable!=null) {
				List<List> tablebodyRef = new ArrayList<List>();
				List<String> list;
				for (int i=1; i<refTable.getRowCount(); i++) {
					String instrumentNumber = "";
					String book = "";
					String page = "";
					String referenceDocumentType = "";
					String year = "";
					String month = "";
					String day = "";
					if (isCourt) {
						if (refTable.getRow(i).getColumnCount()>4) {
							referenceDocumentType = refTable.getRow(i).getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
							instrumentNumber = refTable.getRow(i).getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
							String docketedDate = refTable.getRow(i).getColumns()[4].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
							if (!StringUtils.isEmpty(docketedDate)) {
								SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
						        Date date = formatter.parse(docketedDate);
						        Calendar cal = Calendar.getInstance();
						        cal.setTime(date);
						        year = Integer.toString(cal.get(Calendar.YEAR));
						        month = Integer.toString(cal.get(Calendar.MONTH) + 1);
								day = Integer.toString(cal.get(Calendar.DAY_OF_MONTH));  
							}
							list = new ArrayList<String>();
							list.add(instrumentNumber);
							list.add("");
							list.add("");
							list.add(referenceDocumentType);
							list.add(year);
							list.add(month);
							list.add(day);
							tablebodyRef.add(list);
						}
					} else {
						if (refTable.getRow(i).getColumnCount()>0) {
							instrumentNumber = refTable.getRow(i).getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
							if (refTable.getRow(i).getColumnCount()>2) {
								book = refTable.getRow(i).getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
								page = refTable.getRow(i).getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
							}
							if (!"".equals(instrumentNumber)||!"".equals(book)||!"".equals(page)) {
								Matcher ma = Pattern.compile(BOOK_PAGE_PATTERN).matcher(instrumentNumber);
								if (ma.matches()) {
									instrumentNumber = "";
									book = ma.group(1);
									page = ma.group(2);
								}
							}	
							list = new ArrayList<String>();
							list.add(instrumentNumber);
							list.add(book);
							list.add(page);
							list.add("");
							list.add("");
							list.add("");
							list.add("");
							tablebodyRef.add(list);
						}
					}
				}
				if (tablebodyRef.size()>0) {
					String[] headerRef = {CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName(), CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName(),
							CrossRefSetKey.CROSS_REF_TYPE.getShortKeyName(), CrossRefSetKey.YEAR.getShortKeyName(), CrossRefSetKey.MONTH.getShortKeyName(),
							CrossRefSetKey.DAY.getShortKeyName()};
					ResultTable crossRef = GenericFunctions2.createResultTable(tablebodyRef, headerRef);
					map.put("CrossRefSet", crossRef);
				}
			}
			
			if (legalTable!=null && legalTable.getRowCount()>1) {
				List<List> bodyPIS = new ArrayList<List>();
				List<String> line;
				for (int i=1;i<legalTable.getRowCount();i++) {
					line = new ArrayList<String>();
					String lot = legalTable.getRow(i).getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					String block = legalTable.getRow(i).getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					String subdivisionName = legalTable.getRow(i).getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					String section = "";
					String township = "";
					String range = "";
					subdivisionName = StringUtils.prepareStringForHTML(subdivisionName);
					Matcher strMa = Pattern.compile(ro.cst.tsearch.servers.types.GenericOrbit.STR_PATTERN).matcher(subdivisionName);
					if (strMa.matches()) {
						section = strMa.group(3).replaceFirst("^0+", "");
						township = strMa.group(2).replaceFirst("^0+", "");
						range = strMa.group(1).replaceFirst("^0+", "");
//						subdivisionName = "";
					} else {
						subdivisionName = cleanSubdivisionName(subdivisionName, searchId, countyId);
					}
					line.add(subdivisionName);
					line.add(lot);
					line.add(block);
					line.add(section);
					line.add(township);
					line.add(range);
					bodyPIS.add(line);
				}
				if (bodyPIS.size() > 0) {
					String[] header = {PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(),
									   PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(),
						       		   PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(),
							           PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(),
							           PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(),
							           PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName()};
					ResultTable rt = GenericFunctions2.createResultTable(bodyPIS, header);
					map.put("PropertyIdentificationSet", rt);
				}
			}
			
			String tmpGrantor = sbGrantor.toString().replaceFirst(" / $", "");
			if (!StringUtils.isEmpty(tmpGrantor)) {
				map.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpGrantor);
			}
			String tmpGrantee = sbGrantee.toString().replaceFirst(" / $", "");
			if (!StringUtils.isEmpty(tmpGrantee)) {
				map.put(SaleDataSetKey.GRANTEE.getKeyName(), tmpGrantee);
			}
			
			if (isCourt) {
				ro.cst.tsearch.servers.functions.GenericOrbit.parseCourtNames(map);
			} else {
				GenericFunctions1.parseGrantorGranteeSetOrbit(map, searchId);
				if (CountyConstants.MO_Clay==countyId||CountyConstants.MO_Jackson==countyId||CountyConstants.KS_Johnson==countyId) {
					GenericFunctions1.setGranteeLanderTrustee1(map, searchId);
				}
				if (CountyConstants.MO_Jackson==countyId) {
					GenericFunctions1.correctBookPage(map, searchId);
				}
			}
			
			map.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String cleanSubdivisionName(String subdivisionName, long searchId, int countyId) {
		if (StringUtils.isEmpty(subdivisionName)) {
			return "";
		}
		if (CountyConstants.MO_Clay==countyId||CountyConstants.MO_Platte==countyId||CountyConstants.KS_Wyandotte==countyId) {
			subdivisionName = StringFormats.SubdivisionMOClayOR(subdivisionName);
		} else if (CountyConstants.MO_Jackson==countyId) {
			subdivisionName = GenericFunctions1.cleanSubdivIntermOR(subdivisionName, searchId);
		} else if (CountyConstants.KS_Johnson==countyId) {
			subdivisionName = GenericFunctions1.convertFromRomanToArab(subdivisionName);
		}
		subdivisionName = subdivisionName.replaceFirst("\\*\\*.*", "").trim();
		return subdivisionName;
	}
	
	/**
	 * 
	 * @param docNo
	 * @param fileName
	 * @param site
	 * @return
	 */
	protected String retrieveImage(String para, String docNo, String fileName){
		return GenericOrbit.retrieveImage(para, docNo, fileName, miServerID, searchId);
	}
	
	/**
	 * Function was rewritten. Check SVN History for other versions
	 * @param docNo
	 * @param fileName
	 * @param site
	 * @return
	 */
	protected static String retrieveImage(String para, String docNo, String fileName, int miServerID, long searchId){
		
		fileName = fileName.replaceFirst("(?i)\\.tiff?$", ".pdf");
		
		// check if the file already exists
		if(FileUtils.existPath(fileName)){
			return fileName;
		}
		
		// create the file folders
		fileName = fileName.replace("/", File.separator);
		fileName = fileName.replace("\\", File.separator);
		FileUtils.CreateOutputDir(fileName);
		
		// determine county abbreviation
		String serverName = getCrtTSServerName(miServerID);
		
		HttpSite3 site = HttpManager3.getSite(serverName, searchId);
		
		try {
			
			if (StringUtils.isEmpty(para) || "isOld".equals(para)) {
				Date today = new Date();
				SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
				String todayString = formatter.format(today);
				HTTPRequest req0 = new HTTPRequest(ro.cst.tsearch.connection.http3.GenericOrbit.DOCUMENT_NUMBER_SEARCH_TEXT + 
						"?SubdivisionDocNumberType=true&IsRemainDataDocumentNumberForm=false&DocumentID=" + docNo + 
						"&ToDate_DocNumber=" + todayString, HTTPRequest.GET);
				String response = site.process(req0).getResponseAsString();
				List<String> links = RegExUtils.getMatches(IMAGE_LINK_PATTERN, response, 1);
				HashSet<String> set = new HashSet<String>();
				for (String s: links) {
					set.add(s);
				}
				set = keepOnlyGoodLink(set, docNo);
				if (set.size()==1) {	//exactly one result
					para = set.iterator().next();
				}
			}
			
			if ("isCourt".equals(para) || "isOld".equals(para)) {
				HTTPRequest req0 = new HTTPRequest(ro.cst.tsearch.connection.http3.GenericOrbit.COURT_SEARCH_TEXT + 
						"?IsUseSoundex=1&IsRemainDataCourtNameForm=false&DocumentId=" + docNo, HTTPRequest.GET);
				String response = site.process(req0).getResponseAsString();
				List<String> links = RegExUtils.getMatches(IMAGE_LINK_PATTERN, response, 1);
				HashSet<String> set = new HashSet<String>();
				for (String s: links) {
					set.add(s.replaceFirst(".+?,", ""));
				}
				set = keepOnlyGoodLink(set, docNo);
				if (set.size()==1) {	//exactly one result
					para = links.get(0);
				}
			}
			
			if (StringUtils.isEmpty(para)) {
				return null;
			}
				
			String link = "/Search/ViewDocument";			
			HTTPRequest req = new HTTPRequest(link, HTTPRequest.POST);
			req.setPostParameter("para", para);
			String page = site.process(req).getResponseAsString();
			
			SearchLogger.info("Trying to download image for document "+ docNo +" from ORBIT <br/>",searchId);
			
			Matcher ma = Pattern.compile("(?is)\"(http.+?\\.pdf)\"").matcher(page);
			if (!ma.matches()) {
	            return null;
			}	        
	        
			link = ma.group(1).replaceAll("(?is)\\\\\\\\", "\\\\");
			req = new HTTPRequest(link, HTTPRequest.GET);
			
			HTTPResponse finalResponse = site.process(req);
			if(finalResponse.getContentType().contains("application/pdf")) {
				try {
					org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(fileName), finalResponse.getResponseAsByte());
				} catch (IOException e) {
					logger.error("Cannot write image content to " + fileName, e);
				}
			} else {
				return null;
			}
			
		} finally {
			HttpManager3.releaseSite(site);
		}
			
		if (FileUtils.existPath(fileName)) {
			return fileName;
		}
		return null;
	
	}
	
	private static HashSet<String> keepOnlyGoodLink(HashSet<String> set, String docNo) {
		if (set.size()<=1) {
			return set;
		}
		HashSet<String> newSet = new HashSet<String>();
		Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			String link = it.next();
			if (link.matches("(?i).*?,INST\\|" + docNo + "\\|.*")) {
				newSet.add(link);
			}
		}
		return newSet;
	}

	@Override
	public ServerResponse GetLink(String vsRequest, boolean vbEncoded) throws ServerResponseException {
		
		if(!vsRequest.contains("/Search/ViewDocument?para=")){
			return super.GetLink(vsRequest, vbEncoded);
		}
			
		String para = StringUtils.extractParameter(vsRequest, "\\?para=([^&]*)");
		
		boolean imageOK = false;
		
		// get docNo
		Matcher ma = Pattern.compile("(?is),INST\\|([^|]+)\\|").matcher(para);
		String docNo = "";
		if (ma.find()) {
			docNo = ma.group(1);
			// create output folder and determine file name
			String folderName = getSearch().getImageDirectory() + File.separator;
			new File(folderName).mkdirs();
	    	String fileName = folderName + docNo + ".pdf";
	    	
	    	// retrieve image
	    	String newFileName = retrieveImage(para, docNo, fileName);
	    	if (newFileName!=null) {
	    		if (newFileName.matches("(?i).+\\.pdf$")) {
	    			// write the image to the client web-browser
	    			imageOK = writeImageToClient(newFileName, "application/pdf");
	    		} else if (newFileName.matches("(?i).+\\.tiff?$")) {
	    			// write the image to the client web-browser
	    			imageOK = writeImageToClient(newFileName, "image/tiff");
	    		}
	    	}
	    	
		}
		
		// image not retrieved
		if(!imageOK){ 
	        
			// return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);
			
		} else {
			
			// return solved response
			return ServerResponse.createSolvedResponse();    	
    	}
	    	
	}

	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
		
    	String fileName = image.getPath();
    	String docNo = StringUtils.extractParameter(image.getLink(), "\\?docno=([^&]*)");
    	String isSpecial = "";
    	if ("".equals(docNo)) {
    		docNo = StringUtils.extractParameter(image.getLink(), "&docNo=([^&]*)");		//for old searches
    		isSpecial = "isOld";
    	} else {
    		String isCourt = StringUtils.extractParameter(image.getLink(), "&isCourt=([^&]*)");
        	if ("true".equals(isCourt)) {
        		isSpecial = "isCourt";
        	}
    	}
    	
    	// retrieve image
    	String newFileName = retrieveImage(isSpecial, docNo, fileName); 
    	if(newFileName!=null){
    		if (newFileName.matches("(?i).+\\.pdf$")) {
    			image.setContentType("application/pdf");
    		} else if (newFileName.matches("(?i).+\\.tiff?$")) {
    			image.setContentType("image/tiff");
    		}
    		afterDownloadImage(true);
    		byte b[] = FileUtils.readBinaryFile(newFileName);
    		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
    	}
    	return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );

	}
	
	/**
     * Looks for the a document having the same instrumentNo
     * @param instrumentNo
     * @param documentToCheck if not null will only compare its instrument with saved documents
     * @param data
     * @return
     */
    public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
    		return false;
    	}
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null)
    				return true;
    		} else {
    			//used for not saving a document already saved from RO, but with instrNo in other form
    			if (TSServer.getCrtTSServerName(miServerID).contains("Platte")){
    				if (instrumentNo.matches("(?is)\\A\\d+.*") && instrumentNo.length() > 4){
    					instrumentNo = instrumentNo.substring(0, 4) 
    					+ org.apache.commons.lang.StringUtils.leftPad(instrumentNo.substring(4, instrumentNo.length()), 6, '0');
    				}
    			}
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
		    		try {
		    			instr.setYear(Integer.parseInt(data.get("year")));
		    		} catch (NumberFormatException nfe) {}
	    		}
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    		
	    			if(almostLike.size() > 0) {
	    				return true;
	    			}
	    			
	    		}
	    		//task 8104
	    		if (TSServer.getCrtTSServerName(miServerID).contains("Clay")) {
	    			if (instrumentNo.matches("\\d{6,7}")) {
	    				instrumentNo = instrumentNo.substring(instrumentNo.length()-5);
	    				instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
			    		if(data != null) {
				    		instr.setBook(data.get("book"));
				    		instr.setPage(data.get("page"));
				    		instr.setDocno(data.get("docno"));
				    		try {
				    			instr.setYear(Integer.parseInt(data.get("year")));
				    		} catch (NumberFormatException nfe) {}
			    		}
			    		List<DocumentI> documents = documentManager.getDocumentsList();
			    		for (DocumentI document: documents) {
			    			String instNo = document.getInstno();
			    			if (instNo.matches("[A-Z]\\d{5}")) {
			    				instNo = instNo.substring(instNo.length()-5);
			    				DocumentI newDoc = document.clone();
			    				newDoc.setInstno(instNo);
			    				if (newDoc.flexibleEquals(instr)) {
			    					return true;
			    				}
			    			}	
			    		}
	    			}
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }
    
    @Override
	public List<TSServerInfoModule> getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();
		String documentNumber = restoreDocumentDataI.getDocumentNumber();
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		
		//Document Number Search, instrument number
		if (StringUtils.isNotEmpty(instrumentNumber)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.forceValue(0, "true");
			module.forceValue(3, instrumentNumber);
			module.forceValue(6, "false");
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("InstrumentNumber", instrumentNumber);
			GenericInstrumentFilter instrumentNumberfilter = new GenericInstrumentFilter(searchId, filterCriteria);
			DuplicateInstrumentFilterResponse duplicateInstrFilter = new DuplicateInstrumentFilterResponse(searchId);
			module.getFilterList().clear();
			module.addFilter(instrumentNumberfilter);
			module.addFilter(duplicateInstrFilter);
			modules.add(module);
		}
		
		//Document Number Search, document number
		if (StringUtils.isNotEmpty(documentNumber)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.forceValue(0, "true");
			module.forceValue(3, documentNumber);
			module.forceValue(6, "false");
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("InstrumentNumber", documentNumber);
			GenericInstrumentFilter instrumentNumberfilter = new GenericInstrumentFilter(searchId, filterCriteria);
			DuplicateInstrumentFilterResponse duplicateInstrFilter = new DuplicateInstrumentFilterResponse(searchId);
			module.getFilterList().clear();
			module.addFilter(instrumentNumberfilter);
			module.addFilter(duplicateInstrFilter);
			modules.add(module);
		}
				
		//Document Number Search, book-page
		if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.forceValue(0, "true");
			module.forceValue(4, book);
			module.forceValue(5, page);
			module.forceValue(6, "false");
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			GenericInstrumentFilter bookPageNumberfilter = new GenericInstrumentFilter(searchId, filterCriteria);
			DuplicateInstrumentFilterResponse duplicateInstrFilter = new DuplicateInstrumentFilterResponse(searchId);
			module.getFilterList().clear();
			module.addFilter(bookPageNumberfilter);
			module.addFilter(duplicateInstrFilter);
			modules.add(module);
		}
		
		//Court Name Search, instrument number
		if (StringUtils.isNotEmpty(instrumentNumber)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.MODULE_IDX38));
			module.forceValue(6, instrumentNumber);
			module.forceValue(7, "1");
			module.forceValue(8, "false");
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("InstrumentNumber", instrumentNumber);
			GenericInstrumentFilter instrumentNumberfilter = new GenericInstrumentFilter(searchId, filterCriteria);
			DuplicateInstrumentFilterResponse duplicateInstrFilter = new DuplicateInstrumentFilterResponse(searchId);
			module.getFilterList().clear();
			module.addFilter(instrumentNumberfilter);
			module.addFilter(duplicateInstrFilter);
			modules.add(module);
		}
		
		//Court Name Search, document number
		if (StringUtils.isNotEmpty(documentNumber)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.MODULE_IDX38));
			module.forceValue(6, documentNumber);
			module.forceValue(7, "1");
			module.forceValue(8, "false");
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("InstrumentNumber", documentNumber);
			GenericInstrumentFilter instrumentNumberfilter = new GenericInstrumentFilter(searchId, filterCriteria);
			DuplicateInstrumentFilterResponse duplicateInstrFilter = new DuplicateInstrumentFilterResponse(searchId);
			module.getFilterList().clear();
			module.addFilter(instrumentNumberfilter);
			module.addFilter(duplicateInstrFilter);
			modules.add(module);
		}
		
		return modules;
	}
    
    protected int[] getModuleIdsForSavingName() {
		return new int[]{TSServerInfo.NAME_MODULE_IDX, TSServerInfo.MODULE_IDX38};
	}
    
    @Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 4) {
			String[] names = StringFormats.parseNameNashville(module.getFunction(4).getParamValue(), true);
			name.setLastName(names[2]);
			name.setFirstName(names[0]);
			name.setMiddleName(names[1]);
			return name;
		} else if (module.getModuleIdx() == TSServerInfo.MODULE_IDX38 && module.getFunctionCount() > 5) {
			String lastName = module.getFunction(2).getParamValue();
			String firstName = module.getFunction(3).getParamValue();
			String middleName = module.getFunction(4).getParamValue();
			String companyName = module.getFunction(5).getParamValue();
			if (!StringUtils.isEmpty(lastName) && !StringUtils.isEmpty(firstName)) {
				name.setLastName(lastName);
				name.setFirstName(firstName);
				name.setMiddleName(middleName);
				return name;
			} else if (!StringUtils.isEmpty(companyName)) {
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
		if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX && module.getFunctionCount() > 5) {
			SubdivisionI subdivision = new Subdivision();
			subdivision.setName(module.getFunction(3).getParamValue());
			subdivision.setLot(module.getFunction(4).getParamValue());
			subdivision.setBlock(module.getFunction(5).getParamValue());
			legal.setSubdivision(subdivision);
		}
		return legal;
	}
    
    @Override
	protected void setCertificationDate() {
    	// on MO/KS - we do not parse CD from DataSource
//		try {
//			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
//				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
//				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
//			} else {
//				int countyId = dataSite.getCountyId();
//				String serverName = getCrtTSServerName(miServerID);
//				HttpSite3 site = HttpManager3.getSite(serverName, searchId);
//				HTTPRequest req = new HTTPRequest("/Search/GetArangeDate?RegionID=" + 
//						ro.cst.tsearch.connection.http3.GenericOrbit.getRegionCode(countyId) + "&CountyID=" + 
//						ro.cst.tsearch.connection.http3.GenericOrbit.getCountyCode(countyId), HTTPRequest.GET);;
//				String response = "";
//				try {
//					response = site.process(req).getResponseAsString();
//				} finally {
//					HttpManager3.releaseSite(site);
//				}
//				if (!StringUtils.isEmpty(response)) {
//					JSONObject jsonObject = new JSONObject(response);
//					String date = (String) jsonObject.get("ToDate");
//					CertificationDateManager.cacheCertificationDate(dataSite, date);
//					getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
//				}
//				
//			}
//		} catch (Exception e) {
//			logger.error(e.getMessage());
//		}
	}
    
}

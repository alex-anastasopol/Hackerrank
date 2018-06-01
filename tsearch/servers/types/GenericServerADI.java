package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http3.GenericConnADI;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.bean.LegalSKLDIteratorEntry;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsForUpdateFilter;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeAdvancedFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.data.PlatBookPage;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.iterator.legal.LegalSKLDIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.servlet.parentsite.ParentSiteActions;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FormatNumber;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.PDFUtils;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.Ccer;
import com.stewart.ats.base.document.Corporation;
import com.stewart.ats.base.document.Court;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.Fields;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.Lien;
import com.stewart.ats.base.document.Mortgage;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.Transfer;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameMortgageGrantee;
import com.stewart.ats.base.name.NameMortgageGranteeI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;

/**
 * @author MihaiB
 */

public class GenericServerADI extends TSServerROLike{

	private static final long										serialVersionUID	= -9080561494803145195L;

	private static final Pattern									PAGE_SPLITTER		= Pattern.compile("(?i)(?:(?:\\r)?\\n)+.*?\\bPAGE\\s*:\\s*\\d+\\s*");
	private static final Pattern									ITEM_SPLITTER		= Pattern.compile("(?i)\\d+[\\-]{72}");

	private static final Pattern									END_SEARCH_FOOTER	= Pattern.compile("(?i)[\\-]{77}\\s*\\*\\*\\s*END OF SEARCH\\s*\\*\\*");

	private static final Pattern									EXTRACT_PLAT_INFO	= Pattern.compile("(?is)=+.*?=+(.*?)0{2,}1-+");

	private static final Pattern									NAME_FROM_PLAT		= Pattern.compile("(?is)\\bOWNER\\s*:\\s*(.*?)\\r\\n");

	public static final String										TYPE_OF_LEGAL_KEY	= "typeOfSearch=";
	public static final String										PB_PG_ABS_KEY		= "primaryLegal=";

	private static final FormatNumber								TWO_DECIMALS		= new FormatNumber(FormatNumber.TWO_DECIMALS);

	private static HashMap<Integer, String>							COUNTY_ABBREVIATION	= new HashMap<Integer, String>();

	public static final HashSet<String>								mapIdModuleSaKeys	= new HashSet<String>();

	protected static Map<String, Map<CountySpecificInfo, String>>	parentSiteInfo		= new HashMap<String, Map<CountySpecificInfo, String>>();

	private String													documentTypeSelect	= "";

	private String													sourceTypeSelect	= "";
	
	public static enum ImageSourceType{
		A,	//Acreage/Appraisal Maps
		H,	//
		R,	//Real Property Records
		S,	//Subdivision Plats
		Y	//Represent a digital copy of a County Backplant
		
	}
	
	public static enum SearchResultKeys {
		id,
		documentType,
		imagePageCount,
		fileDate,
		documentNumber,
		volumePage,
		remark,
		company,
		amount,
		party1,
		moreParty1,
		party2,
		moreParty2,
		legals,
		moreLegal,
		references,
		moreReference,
		messages
	}

	public static enum LegalKeys {
		primaryLegal,
		secondaryLegal,
		acreage,
		remark
	}
	
	public static enum ReferenceKeys {
		type,
		documentNumber,
		volumePage,
		documentType,
		fileDate
	}
	
	public static enum MapEditRecordKeys {
		primaryLegalLow,
		editName,
		sectionPhase,
		forceError,
		fileDate,
		vacateDate,
		documentNumber,
		replatPrimaryLegal,
		legalType,
		replatType,
		masterRemarks,
		alternateNames,
		alternateReferences,
		details
	}
	
	protected static enum CountySpecificInfo {
		DOCUMENT_TYPE_SELECT,
		SOURCE_TYPE_SELECT
	}
	
	static {
		COUNTY_ABBREVIATION.put(CountyConstants.TX_Bastrop, "bs");// 021
		COUNTY_ABBREVIATION.put(CountyConstants.TX_Burnet, "br");// 053
		COUNTY_ABBREVIATION.put(CountyConstants.TX_Caldwell, "cl");// 055
		COUNTY_ABBREVIATION.put(CountyConstants.TX_Hays, "hy");// 209
		COUNTY_ABBREVIATION.put(CountyConstants.TX_Llano, "ln");// 299
		COUNTY_ABBREVIATION.put(CountyConstants.TX_Travis, "tr");// 453
		COUNTY_ABBREVIATION.put(CountyConstants.TX_Williamson, "wm");// 491
		
		mapIdModuleSaKeys.add(SearchAttributes.LD_TAD_SUBDIVISION_OR_ACREAGE);
		mapIdModuleSaKeys.add(SearchAttributes.LD_TAD_PLAT_BOOK_PAGE);
		
		loadParentSiteData();
	}
	
	public GenericServerADI(long searchId) {
		super(searchId);
		if(StringUtils.isBlank(sourceTypeSelect)) {
			initFields();
		}
	}

	public GenericServerADI(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {

		initFields();
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);

		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();

		String countyName = COUNTY_ABBREVIATION.get(getDataSite().getCountyId());
		
		if (tsServerInfoModule != null) {
			if (tsServerInfoModule.getFunctionCount() > 5) {
				tsServerInfoModule.getFunction(5).setValue(countyName);
				tsServerInfoModule.getFunction(5).setDefaultValue(countyName);
			}
			
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.NAME_MODULE_IDX,
							nameToIndex.get(functionName));
					if (StringUtils.isNotEmpty(comment)) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
			
			setupSelectBox(tsServerInfoModule.getFunction(10), documentTypeSelect);
			setupSelectBox(tsServerInfoModule.getFunction(11), sourceTypeSelect);
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);
		if (tsServerInfoModule != null) {
			if (tsServerInfoModule.getFunctionCount() > 6) {
				tsServerInfoModule.getFunction(6).setValue(countyName);
				tsServerInfoModule.getFunction(6).setDefaultValue(countyName);

				setupSelectBox(tsServerInfoModule.getFunction(12), documentTypeSelect);
				setupSelectBox(tsServerInfoModule.getFunction(13), sourceTypeSelect);
				
				HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
				for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
					nameToIndex.put(tsServerInfoModule.getFunction(i).getParamName(), i);
				}
				PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
				for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					String functionName = htmlControl.getCurrentTSSiFunc().getParamName();
					if (StringUtils.isNotEmpty(functionName)) {
						String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.SUBDIVISION_MODULE_IDX,
								nameToIndex.get(functionName));
						if (StringUtils.isNotEmpty(comment)) {
							htmlControl.setFieldNote(comment);
						}
					}
				}
			}
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if (tsServerInfoModule != null) {
			if (tsServerInfoModule.getFunctionCount() > 1) {
				tsServerInfoModule.getFunction(1).setValue(countyName);
				tsServerInfoModule.getFunction(1).setDefaultValue(countyName);

				HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
				for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
					nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
				}
				PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
				for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					String functionName = htmlControl.getCurrentTSSiFunc().getName();
					if (StringUtils.isNotEmpty(functionName)) {
						String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.INSTR_NO_MODULE_IDX,
								nameToIndex.get(functionName));
						if (StringUtils.isNotEmpty(comment)) {
							htmlControl.setFieldNote(comment);
						}
					}
				}
				setupSelectBox(tsServerInfoModule.getFunction(6), documentTypeSelect);
				setupSelectBox(tsServerInfoModule.getFunction(7), sourceTypeSelect);
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.CONDOMIN_MODULE_IDX);
		if (tsServerInfoModule != null) {
			if (tsServerInfoModule.getFunctionCount() > 2) {
				tsServerInfoModule.getFunction(2).setValue(countyName);
				tsServerInfoModule.getFunction(2).setDefaultValue(countyName);

				HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
				for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
					nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
				}
				PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
				for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					String functionName = htmlControl.getCurrentTSSiFunc().getName();
					if (StringUtils.isNotEmpty(functionName)) {
						String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.CONDOMIN_MODULE_IDX,
								nameToIndex.get(functionName));
						if (StringUtils.isNotEmpty(comment)) {
							htmlControl.setFieldNote(comment);
						}
					}
				}
			}
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SECTION_LAND_MODULE_IDX);
		if (tsServerInfoModule != null) {
			if (tsServerInfoModule.getFunctionCount() > 2) {
				tsServerInfoModule.getFunction(2).setValue(countyName);
				tsServerInfoModule.getFunction(2).setDefaultValue(countyName);

				HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
				for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
					nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
				}
				PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
				for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					String functionName = htmlControl.getCurrentTSSiFunc().getName();
					if (StringUtils.isNotEmpty(functionName)) {
						String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.SECTION_LAND_MODULE_IDX,
								nameToIndex.get(functionName));
						if (StringUtils.isNotEmpty(comment)) {
							htmlControl.setFieldNote(comment);
						}
					}
				}
			}
		}
		return msiServerInfoDefault;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		if (global.getSearchType() == Search.AUTOMATIC_SEARCH) {

			boolean isUpdate = (isUpdate()) || global.getSa().isDateDown();

			TSServerInfoModule module = null;

			FilterResponse nameFilterOwner 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, global.getID(), null );
			((GenericNameFilter)nameFilterOwner).setUseSynonymsForCandidates(true);
			((GenericNameFilter)nameFilterOwner).setUseSynonymsBothWays(true);
			((GenericNameFilter)nameFilterOwner).setIgnoreMiddleOnEmpty(true);
			nameFilterOwner.setInitAgain(true);
			FilterResponse nameFilterBuyer 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, global.getID(), null );
			((GenericNameFilter)nameFilterBuyer).setUseSynonymsForCandidates(true);
			((GenericNameFilter)nameFilterBuyer).setUseSynonymsBothWays(true);
			((GenericNameFilter)nameFilterBuyer).setIgnoreMiddleOnEmpty(true);
			nameFilterBuyer.setInitAgain(true);
			
			DocsValidator rejectSavedDocuments = null;
			RejectAlreadySavedDocumentsForUpdateFilter rejectAlreadySavedDocumentsForUpdateFilter = null;
	    	if(isUpdate()) {
	    		rejectAlreadySavedDocumentsForUpdateFilter = new RejectAlreadySavedDocumentsForUpdateFilter(searchId);
	    		rejectSavedDocuments = rejectAlreadySavedDocumentsForUpdateFilter.getValidator();
	    	}
			
//			DocsValidator betweenDatesValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
			GenericMultipleLegalFilter genericMultipleLegalFilter = new GenericMultipleLegalFilter(searchId);
			genericMultipleLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.ADI_LOOK_UP_DATA);
			genericMultipleLegalFilter.setUseLegalFromSearchPage(true);
			genericMultipleLegalFilter.setEnableAbs(true);
// 			DocsValidator genericMultipleLegalValidator = genericMultipleLegalFilter.getValidator();
			LastTransferDateFilter lastTransferDateFilter = new LastTransferDateFilter(searchId);

			InstrumentGenericIterator instrumentGenericIterator = null;
			InstrumentGenericIterator bpGenericIterator = null;
			{
				instrumentGenericIterator = getInstrumentIterator(true);

				if (!instrumentGenericIterator.createDerrivations().isEmpty()) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
					module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
							TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
					module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
					module.getFunction(1).forceValue(COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));
					module.addFilter(new GenericInstrumentFilter(searchId));
					module.addFilter(genericMultipleLegalFilter);
					module.addIterator(instrumentGenericIterator);

					modules.add(module);
				}
			}
			{
				bpGenericIterator = getInstrumentIterator(false);
				bpGenericIterator.setRemoveLeadingZerosBP(true);

				if (!bpGenericIterator.createDerrivations().isEmpty()) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
					module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
							TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
					module.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_NOT_AGAIN);
					module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_PAGE_AS_INSTRUMENT_FAKE);
					module.getFunction(1).forceValue(COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));
					module.addFilter(new GenericInstrumentFilter(searchId));
					module.addFilter(genericMultipleLegalFilter);
					module.addIterator(bpGenericIterator);
					modules.add(module);
				}
			}
			{
				/**
				 * Searching with platted legal. We must have plat book/plat page and block/lot
				 */
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
				module.clearSaKeys();

//				LegalDescriptionIterator it = getLegalDescriptionIterator();
								
				LegalSKLDIterator it = getPlattedLegalIterator();
				module.addIterator(it);

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_PAGE_AS_INSTRUMENT_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67);
				module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_68);

				module.getFunction(3).forceValue("SS");
				module.getFunction(6).forceValue(COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));

				module.setSaKey(7, SearchAttributes.FROMDATE_MMDD);
				module.setSaKey(8, SearchAttributes.FROMDATE_YEAR);
				module.setSaKey(9, SearchAttributes.TODATE_MMDD);
				module.setSaKey(10, SearchAttributes.TODATE_YEAR);

// 				module.addFilter(genericMultipleLegalFilter);
// 				module.addValidator(genericMultipleLegalValidator);
// 				module.addValidator(lastTransferDateFilter);
// 				module.addValidator(rejectSavedDocuments);
// 				module.addCrossRefValidator(genericMultipleLegalValidator);
// 				module.addCrossRefValidator(lastTransferDateFilter);
// 				module.addCrossRefValidator(betweenDatesValidator);
				
				module.addCrossRefValidator(genericMultipleLegalFilter.getValidator());
				
				modules.add(module);
			}
			{
				/**
				 * Searching with unplatted legal. We must have abstract number.
				 */
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
				module.clearSaKeys();

				LegalDescriptionIterator it = getUnplattedLegalIterator();
				it.setEnableTownshipLegal(true);
				module.addIterator(it);

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_ARB);

				module.getFunction(3).forceValue("AA");
				module.getFunction(6).forceValue(COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));

				module.setSaKey(7, SearchAttributes.FROMDATE_MMDD);
				module.setSaKey(8, SearchAttributes.FROMDATE_YEAR);
				module.setSaKey(9, SearchAttributes.TODATE_MMDD);
				module.setSaKey(10, SearchAttributes.TODATE_YEAR);

				// module.addFilter(genericMultipleLegalFilter);
				// module.addValidator(genericMultipleLegalValidator);
				// module.addValidator(lastTransferDateFilter);
				// module.addValidator(rejectSavedDocuments);
				// module.addCrossRefValidator(genericMultipleLegalValidator);
				// module.addCrossRefValidator(lastTransferDateFilter);
				// module.addCrossRefValidator(betweenDatesValidator);
				
				module.addCrossRefValidator(genericMultipleLegalFilter.getValidator());
				
				modules.add(module);
			}

			ArrayList<NameI> searchedNames = addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, 
					"Name Search - Searching by Owner Names for PROPERTY RECORDS",
					null, 
					new FilterResponse[] { nameFilterOwner, genericMultipleLegalFilter, lastTransferDateFilter, rejectAlreadySavedDocumentsForUpdateFilter }, 
					new DocsValidator[] {}, 
					new DocsValidator[] { genericMultipleLegalFilter.getValidator(), lastTransferDateFilter.getValidator(), rejectSavedDocuments },
					"P");

			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
				module.getFunction(1).forceValue(COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
				addBetweenDateTest(module, false, false, false);
				module.addFilter(new GenericInstrumentFilter(searchId));
				modules.add(module);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
				module.getFunction(1).forceValue(COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);

				OcrOrBootStraperIterator ocrBPIteratoriterator = new OcrOrBootStraperIterator(searchId, getDataSite()) {
					private static final long serialVersionUID = 1L;

					@SuppressWarnings("deprecation")
					@Override
					public List<ro.cst.tsearch.propertyInformation.Instrument> extractInstrumentNoList(TSServerInfoModule initial) {
						List<ro.cst.tsearch.propertyInformation.Instrument> extractInstrumentNoList = super.extractInstrumentNoList(initial);

						for (ro.cst.tsearch.propertyInformation.Instrument instrument : extractInstrumentNoList) {
							String bookNo = instrument.getBookNo();
							String pageNo = instrument.getPageNo();
							String instrNo = instrument.getInstrumentNo();

							if (StringUtils.isNotEmpty(bookNo) && StringUtils.isNotEmpty(pageNo) && StringUtils.isEmpty(instrNo)) {
								instrument.setInstrumentNo(bookNo + "/" + pageNo);
							}
						}
						setSearchIfPresent(false);
						return extractInstrumentNoList;
					}
				};

				ocrBPIteratoriterator.setInitAgain(true);
				module.addIterator(ocrBPIteratoriterator);

				module.addFilter(new GenericInstrumentFilter(searchId));
				addBetweenDateTest(module, false, false, false);
				modules.add(module);
			}

			addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, 
					"Name Search - Searching by Owner Names for PROPERTY RECORDS",
					searchedNames == null ? new ArrayList<NameI>() : searchedNames, 
					new FilterResponse[] { nameFilterOwner, genericMultipleLegalFilter, lastTransferDateFilter, rejectAlreadySavedDocumentsForUpdateFilter }, 
					new DocsValidator[] {}, 
					new DocsValidator[] { genericMultipleLegalFilter.getValidator(), lastTransferDateFilter.getValidator(), rejectSavedDocuments },
					"P");

			DocTypeAdvancedFilter docTypeOwnerAdvancedFilter = DoctypeFilterFactory.getDoctypeFilterForGeneralIndexOwnerNameSearch( searchId )
				.setForcePassIfNoReferences(true)
				.setIsUpdate(isUpdate());
			
			addNameSearch( modules, serverInfo, SearchAttributes.OWNER_OBJECT, 
					"Name Search - Searching by Owner Names for NON PROPERTY RECORDS",
					null, 
					new FilterResponse[] { nameFilterOwner, docTypeOwnerAdvancedFilter, rejectAlreadySavedDocumentsForUpdateFilter},
					new DocsValidator[] {},
					new DocsValidator[] {docTypeOwnerAdvancedFilter.getValidator(), rejectSavedDocuments},
					"N"
			);
			
			DocTypeAdvancedFilter doctypeBuyerAdvancedFilter = DoctypeFilterFactory.getDoctypeFilterForGeneralIndexBuyerNameSearch( searchId )
				.setForcePassIfNoReferences(true)
				.setDocTypesForGoodDocuments(new String[]{DocumentTypes.RELEASE})
				.setIsUpdate(isUpdate());
			addNameSearch( modules, serverInfo, SearchAttributes.BUYER_OBJECT,
					"Name Search - Searching by Buyer Names for NON PROPERTY RECORDS",
					null, 
					new FilterResponse[] { nameFilterBuyer, rejectAlreadySavedDocumentsForUpdateFilter, doctypeBuyerAdvancedFilter},
					new DocsValidator[] {},
					new DocsValidator[] {rejectSavedDocuments},
					"N"
			);
			
			// search by crossRef list from TAD documents
			{
				instrumentGenericIterator = getInstrumentIterator(true);
				instrumentGenericIterator.setLoadFromRoLike(true);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				module.getFunction(1).forceValue(COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));
				module.addFilter(new GenericInstrumentFilter(searchId));
				module.addFilter(genericMultipleLegalFilter);
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				
				module.addIterator(instrumentGenericIterator);
				modules.add(module);
			}
			
			{
				bpGenericIterator = getInstrumentIterator(false);
				bpGenericIterator.setLoadFromRoLike(true);

				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_PAGE_AS_INSTRUMENT_FAKE);
				module.getFunction(1).forceValue(COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));
				module.addFilter(new GenericInstrumentFilter(searchId));
				module.addFilter(genericMultipleLegalFilter);

				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				module.addIterator(bpGenericIterator);
				modules.add(module);
			}
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	public InstrumentGenericIterator getInstrumentIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

			private static final long serialVersionUID = 5399351945130601258L;

			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				if (isLoadFromRoLike()) {
					return instno;
				}
				if (StringUtils.isNotEmpty(instno)) {
					
					switch (getDataSite().getCountyId()) {
					case CountyConstants.TX_Caldwell:
						if(year != -1) {
							String yearString = Integer.toString(year);
							if(instno.length() > 4) {
								String startInstr = instno.substring(0, instno.length() - 4);
								if(yearString.endsWith(startInstr)) {
									return yearString + StringUtils.leftPad(instno.substring(instno.length() - 4), 5, "0");	
								}
							} else {
								return yearString + StringUtils.leftPad(instno, 5, "0");
							}
						}
						return instno;

					default:
						if (year != -1) {
							String yearString = Integer.toString(year);
							if (year > 1999) {
								instno = instno.replaceFirst(yearString, "");
								if (instno.length() > 6) {
									instno = instno.substring(instno.length() - 6);
								}
								return (yearString + StringUtils.leftPad(instno, 6, "0"));
							} else {
								return (yearString + instno);
							}
						} else if (year == -1 && instno.length() == 10) {
							return instno;
						}
						break;
					}
					
					
					
				}
				return "";
			}

			@Override
			public String getInstrumentNoFromBookAndPage(InstrumentI state, HashMap<String, String> filterCriteria) {
				String book = state.getBook();
				String page = state.getPage();
				if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
					if (filterCriteria != null) {
						filterCriteria.put("Book", book);
						filterCriteria.put("Page", page);
					}
					return book + "/" + page;
				}
				return "";
			}

			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				if (StringUtils.isNotEmpty(state.getInstno())) {
					if (filterCriteria != null) {
						filterCriteria.put("InstrumentNumber", state.getInstno());
					}
				}
				return state.getInstno();
			}

		};

		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
			instrumentGenericIterator.setRemoveLeadingZerosBP(true);
		}
		return instrumentGenericIterator;
	}

	protected ArrayList<NameI> addNameSearch(List<TSServerInfoModule> modules, TSServerInfo serverInfo, String key, String extraInformation,
			ArrayList<NameI> searchedNames, FilterResponse[] filters, DocsValidator[] docsValidators, DocsValidator[] docsValidatorsCrossref, String partyType) {
		return addNameSearch(modules, serverInfo, key, extraInformation, searchedNames, filters, docsValidators, docsValidatorsCrossref, partyType, null);
	}

	protected ArrayList<NameI> addNameSearch(List<TSServerInfoModule> modules, TSServerInfo serverInfo, String key, String extraInformation,
			ArrayList<NameI> searchedNames, FilterResponse[] filters, DocsValidator[] docsValidators, DocsValidator[] docsValidatorsCrossref,
			String partyType,
			TSServerInfoModule module) {

		if (module == null) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		}
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, extraInformation);
		module.setSaObjKey(key);
		module.clearSaKeys();
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		module.getFunction(5).forceValue(COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));

		module.setSaKey(6, SearchAttributes.FROMDATE_MMDD);
		module.setSaKey(7, SearchAttributes.FROMDATE_YEAR);
		module.setSaKey(8, SearchAttributes.TODATE_MMDD);
		module.setSaKey(9, SearchAttributes.TODATE_YEAR);
		
		module.forceValue(12, partyType);

		for (FilterResponse filterResponse : filters) {
			module.addFilter(filterResponse);
		}
		addFilterForUpdate(module, true);
		for (DocsValidator docsValidator : docsValidators) {
			module.addValidator(docsValidator);
		}
		for (DocsValidator docsValidator : docsValidatorsCrossref) {
			module.addCrossRefValidator(docsValidator);
		}

		module.addCrossRefValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());

		ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, false, searchId,
				new String[] { "L;F;" });
		nameIterator.setInitAgain(true); // initialize again after all
											// parameters are set
		if (searchedNames != null) {
			nameIterator.setSearchedNames(searchedNames);
		}
		searchedNames = nameIterator.getSearchedNames();

		module.addIterator(nameIterator);
		modules.add(module);

		return searchedNames;
	}

	private LegalSKLDIterator getPlattedLegalIterator() {
		LegalSKLDIterator it = new LegalSKLDIterator(searchId, getDataSite()) {

			private static final long	serialVersionUID	= -1542710680555125130L;
			
			@Override
			protected void loadDerrivation(TSServerInfoModule module,
					LegalSKLDIteratorEntry state) {
				if(state.isSubdivisionComplete()) {
					for (Object functionObject : module.getFunctionList()) {
						if (functionObject instanceof TSServerInfoFunction) {
							TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
							if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_PAGE_AS_INSTRUMENT_FAKE) {
								function.setParamValue(state.getMapIdBook() + "/" + state.getMapIdPage());
							} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67) {	//secondary low
								if (StringUtils.isNotEmpty(state.getLot())) {
									function.setParamValue(state.getBlock() + "/" + state.getLot());
								} else {
									function.setParamValue(state.getBlock() + "/*");
								}
							} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_GENERIC_68) {	//secondary low
								if (StringUtils.isNotEmpty(state.getLotHigh())) {
									function.setParamValue(state.getBlock() + "/" + state.getLotHigh());
								}
							} 
						}
					}
				}
				
			}
			
		};
		it.setCompactLots(true);
		return it;
	}
	
	private LegalDescriptionIterator getUnplattedLegalIterator() {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, false, false, getDataSite()) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@SuppressWarnings("unchecked")
			public List<LegalStruct> createDerrivations() {
				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				DocumentsManagerI m = global.getDocManager();

				if ("true".equalsIgnoreCase(global.getSa().getAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND))) {
					return new ArrayList<LegalStruct>();
				}
				
				List<LegalSKLDIteratorEntry> derivations = (List<LegalSKLDIteratorEntry>) global.getAdditionalInfo("LegalSKLDIteratorList"); 
				
				if(derivations != null && !derivations.isEmpty()) {
					//no need to UnplattedLegal
					return new ArrayList<LegalStruct>();
				}

				legalStruct = (Set<LegalStruct>) global.getAdditionalInfo(getAdditionalInfoKey());

				String originalLot = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
				String originalBlock = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);

				Set<PlatBookPage> platBookPageFromUser = new HashSet<PlatBookPage>();

				if (legalStruct == null) {
					legalStruct = new HashSet<LegalStruct>();
					legalSources = new HashMap<String, Set<String>>();
					try {
						m.getAccess();

						initialSiteDocuments = m.getDocumentsWithDataSource(false, dataSite.getSiteTypeAbrev()).size();
						List<DocumentI> listRodocs = loadLegalFromRoDocs(global, m);

						legalStruct = keepOnlyGoodLegals(legalStruct);

						if (isLoadFromSearchPage() || (legalStruct.isEmpty() && isLoadFromSearchPageIfNoLookup())) {

							if (isEnableSubdividedLegal()) {

								// Bug 6738
								if (legalStruct.size() == 1 && StringUtils.isNotEmpty(originalLot) && StringUtils.isNotEmpty(originalBlock)) {
									for (LegalStruct dataStruct1 : legalStruct) {
										if (StringUtils.isNotEmpty(dataStruct1.getPlatBook()) && StringUtils.isNotEmpty(dataStruct1.getPlatPage())) {
											for (LotInterval lotInterval : LotMatchAlgorithm.prepareLotInterval(originalLot)) {
												List<String> allLots = lotInterval.getLotList();
												if (allLots.size() > 0) {
													for (String l : allLots) {
														LegalStruct dataStruct2 = new LegalStruct(false);
														dataStruct2.setPlatBook(dataStruct1.getPlatBook());
														dataStruct2.setPlatPage(dataStruct1.getPlatPage());
														dataStruct2.setLot(l);
														dataStruct2.setBlock(originalBlock);

														if (!testIfExist(legalStruct, dataStruct2, searchId)) {
															legalStruct.add(dataStruct2);
														}
													}
												}
											}
											legalStruct = keepOnlyGoodLegals(legalStruct);
										}
									}
								}
								String platBook = cleanPlatBook(getSearchAttribute(SearchAttributes.LD_BOOKNO));
								String platPage = getSearchAttribute(SearchAttributes.LD_PAGENO);

								if (StringUtils.isNotEmpty(platBook) && StringUtils.isNotEmpty(platPage)) {

									for (LotInterval lotInterval : LotMatchAlgorithm.prepareLotInterval(originalLot)) {
										List<String> allLots = lotInterval.getLotList();
										if (allLots.size() > 0) {
											for (String l : allLots) {

												LegalStruct legalStruct1 = new LegalStruct(false);

												legalStruct1.setPlatBook(platBook);
												legalStruct1.setPlatPage(platPage);
												legalStruct1.setLot(l);
												legalStruct1.setBlock(originalBlock);

												if (!incompleteData(legalStruct1)) {
													String keyForLegal = getKeyForLegal(legalStruct1.getPlatBook(), legalStruct1.getPlatPage());
													Set<String> foundSources = getLegalSourcesNotNull().get(keyForLegal);
													if (foundSources == null) {
														foundSources = new HashSet<String>();
														getLegalSourcesNotNull().put(keyForLegal, foundSources);
													}
													foundSources.add("Search Page");

													if (!testIfExist(legalStruct, legalStruct1, searchId)) {
														legalStruct.add(legalStruct1);
													}
												}
											}
										}
									}

								}
							}
							
							if(isEnableTownshipLegal()) {
								String absNo = getSearchAttribute(SearchAttributes.LD_ABS_NO);
								
								if (StringUtils.isNotEmpty(absNo)) {
									LegalStruct legalStruct1 = new LegalStruct(true);
									legalStruct1.setAbs(absNo);
									legalStruct.add(legalStruct1);
								}
							}
							
							{
								for (DocumentI documentI : listRodocs) {
									if (!documentI.isOneOf(DocumentTypes.PLAT, DocumentTypes.RESTRICTION, DocumentTypes.EASEMENT, DocumentTypes.MASTERDEED,
											DocumentTypes.COURT, DocumentTypes.LIEN, DocumentTypes.CORPORATION, DocumentTypes.AFFIDAVIT, DocumentTypes.CCER)) {
										for (PropertyI prop : documentI.getProperties()) {
											if (prop.hasLegal()) {
												LegalI legal = prop.getLegal();
												if (legal.hasSubdividedLegal() && isEnableSubdivision()) {
													SubdivisionI subdiv = legal.getSubdivision();
													String block = subdiv.getBlock();
													String lot = subdiv.getLot();
													String platBook = subdiv.getPlatBook();
													String platPage = subdiv.getPlatPage();
													if (StringUtils.isNotEmpty(platBook) && StringUtils.isNotEmpty(platPage)) {
														if (StringUtils.isNotEmpty(lot) || StringUtils.isNotEmpty(block)
																|| StringUtils.isNotEmpty(originalBlock) || StringUtils.isNotEmpty(originalLot)) {

															LegalStruct legalStruct1 = new LegalStruct(false);
															legalStruct1.setLot(StringUtils.isEmpty(lot) ? "" : lot);
															legalStruct1.setBlock(StringUtils.isEmpty(block) ? "" : block);
															legalStruct1.setPlatBook(platBook);
															legalStruct1.setPlatPage(platPage);
															if (!testIfExist(legalStruct, legalStruct1, searchId)) {
																legalStruct.add(legalStruct1);
															}
														}
													}
												}
												if (legal.hasTownshipLegal() && isEnableTownshipLegal()) {
													if (StringUtils.isNotEmpty(legal.getTownShip().getAbsNumber())) {
														LegalStruct legalStruct1 = new LegalStruct(true);
														legalStruct1.setAbs(legal.getTownShip().getAbsNumber());
														legalStruct.add(legalStruct1);
													}
												}
											}
										}
									}
								}
							}
							legalStruct = keepOnlyGoodLegals(legalStruct);
						}
						try {
							DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
							long miServerId = dataSite.getServerId();

							for (LegalI legal : global.getSa().getForUpdateSearchLegalsNotNull(miServerId)) {
								treatLegalFromSavedDocument("Legal Saved From Parent Site as Search Parameters", legal, true, platBookPageFromUser);
							}
						} catch (Exception e) {
							logger.error("Error loading names for Update saved from Parent Site", e);
						}
						legalStruct = keepOnlyGoodLegals(legalStruct);

						performValidationOnList();

						global.setAdditionalInfo(getAdditionalInfoKey(), legalStruct);
					} finally {
						m.releaseAccess();
					}

					Set<String> foundKeys = getDifferentLegals(legalStruct);
					boolean foundMultipleKeys = foundKeys.size() > 1;
					if (foundMultipleKeys) {

						PlatBookPage orderPlatBP = new PlatBookPage(cleanPlatBook(global.getSa().getValidatedPlatBook()), global.getSa().getValidatedPlatPage());

						if (!orderPlatBP.isEmpty()) {
							platBookPageFromUser.add(orderPlatBP);
						}
						String[] multipleLegalsLogging = getMultipleLegalSources(foundKeys);

						if (!platBookPageFromUser.isEmpty()) {
							Set<LegalStruct> onlyWithPlat = getOnlyStructuresWithPlat(platBookPageFromUser);
							if (onlyWithPlat.isEmpty()) {
								global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
								global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegalsLogging[1]);
								SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " + multipleLegalsLogging[0]
										+ " (no valid plat book-page iteration from order or saved)<br><div>", searchId);
								legalStruct.clear();

								global.setAdditionalInfo(getAdditionalInfoKey(), legalStruct);
								return new ArrayList<LegalStruct>(legalStruct);
							} else {
								SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " + multipleLegalsLogging[0]
										+ ", but continuing automatic search only with " + Arrays.toString(platBookPageFromUser.toArray())
										+ " (from order and/or saved)<br><div>", searchId);
								legalStruct.clear();
								legalStruct.addAll(onlyWithPlat);

								legalStruct = keepOnlyGoodLegals(legalStruct);
								global.setAdditionalInfo(getAdditionalInfoKey(), legalStruct);

							}
						} else {

							Set<LegalStruct> validatedDerivations = getOnlyStructuresMatchingValidatedData(legalStruct);
							// from validated we need to check if we still have
							// multiple legal

							if (validatedDerivations.isEmpty()) {
								global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
								global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegalsLogging[1]);
								SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " + multipleLegalsLogging[0]
										+ " (failed validation with search page info and no plat book-page from order or saved available)<br><div>", searchId);

								legalStruct.clear();
								global.setAdditionalInfo(getAdditionalInfoKey(), legalStruct);
								return new ArrayList<LegalStruct>(legalStruct);
							} else {
								foundKeys = getDifferentLegals(validatedDerivations);

								if (foundKeys.size() > 1) {
									global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
									global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegalsLogging[1]);
									SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " + multipleLegalsLogging[0]
											+ " (failed validation with search page info and no plat book-page from order or saved available)<br><div>",
											searchId);

								} else {

									SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " + multipleLegalsLogging[0]
											+ ", but continuing automatic search only with " + Arrays.toString(foundKeys.toArray())
											+ " (validated with search page info)<br><div>", searchId);
									legalStruct.clear();
									legalStruct.addAll(validatedDerivations);

									legalStruct = keepOnlyGoodLegals(legalStruct);
									global.setAdditionalInfo(getAdditionalInfoKey(), legalStruct);
								}
							}
						}
					}
					if (!foundMultipleKeys && !legalStruct.isEmpty()) {
						String platBook = cleanPlatBook(getSearchAttribute(SearchAttributes.LD_BOOKNO));
						String platPage = getSearchAttribute(SearchAttributes.LD_PAGENO);

						if (StringUtils.isEmpty(platPage) || StringUtils.isEmpty(platBook)) {

							LegalStruct legalStructToAdd = null;

							Set<String> lots = new HashSet<String>();
							Set<String> blocks = new HashSet<String>();

							boolean updatedPlatBookPage = false;

							for (LegalStruct someStruct : legalStruct) {
								if (StringUtils.isNotEmpty(someStruct.getPlatBook()) && StringUtils.isNotEmpty(someStruct.getPlatPage())) {
									if (!updatedPlatBookPage) {
										global.getSa().setAtribute(SearchAttributes.LD_BOOKNO, someStruct.getPlatBook());
										global.getSa().setAtribute(SearchAttributes.LD_PAGENO, someStruct.getPlatPage());
										updatedPlatBookPage = true;

										/*
										 * Add an iteration with new things
										 * added in search page
										 */
										legalStructToAdd = new LegalStruct(false);
										legalStructToAdd.setPlatBook(someStruct.getPlatBook());
										legalStructToAdd.setPlatPage(someStruct.getPlatPage());
										legalStructToAdd.setBlock(getSearchAttribute(SearchAttributes.LD_SUBDIV_BLOCK));
										legalStructToAdd.setLot(getSearchAttribute(SearchAttributes.LD_LOTNO));

										if (StringUtils.isEmpty(legalStructToAdd.getBlock()) && StringUtils.isEmpty(legalStructToAdd.getLot())) {
											legalStructToAdd = null;
										}
										if (!isLoadFromSearchPage()) {
											legalStructToAdd = null; // do not
																		// add
																		// new
																		// legal
																		// from
																		// search
																		// page
										}
									}

									if (StringUtils.isNotEmpty(someStruct.getLot())) {
										lots.add(someStruct.getLot());
									}
									if (StringUtils.isNotEmpty(someStruct.getBlock())) {
										blocks.add(someStruct.getBlock());
									}
								}
							}

							if (updatedPlatBookPage) {
								if (StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_LOTNO))
										&& StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_SUBDIV_BLOCK))) {
									if (lots.size() > 0) {
										String lotFull = null;
										for (String lot : lots) {
											if (lotFull == null) {
												lotFull = lot;
											} else {
												lotFull += ", " + lot;
											}
										}
										global.getSa().setAtribute(SearchAttributes.LD_LOTNO, lotFull);
									}
									if (blocks.size() == 1) {
										global.getSa().setAtribute(SearchAttributes.LD_SUBDIV_BLOCK, blocks.iterator().next());
									}
								}
								if (legalStructToAdd != null) {
									if (!testIfExist(legalStruct, legalStructToAdd, searchId)) {
										legalStruct.add(legalStructToAdd);
									}
								}
							}
						}
					}
				}

				if (isCheckIfDocumentExists()) {

					Set<LegalStruct> fullList = new LinkedHashSet<LegalStruct>();

					try {
						m.getAccess();
						for (LegalStruct pds : legalStruct) {
							InstrumentI instrumentI = new Instrument();

							if (pds.isPlated()) {
								instrumentI.setBook(pds.getPlatBook());
								instrumentI.setPage(pds.getPlatPage());

								List<DocumentI> almostLike = m.getDocumentsWithInstrumentsFlexible(false, instrumentI);

								if (almostLike.isEmpty()) {
									fullList.add(pds);
								}
							} else {
								fullList.add(pds);
							}
						}
					} finally {
						m.releaseAccess();
					}

					if (fullList.size() != legalStruct.size()) {
						legalStruct = fullList;
						global.setAdditionalInfo(getAdditionalInfoKey(), legalStruct);
					}
				}

//				boolean containsPlatBookPage = false;
				for (LegalStruct item : legalStruct) {
					if (item.isPlated()) {
//						containsPlatBookPage = true;
						break;
					}
				}
				return new ArrayList<LegalStruct>(legalStruct);
			}

			protected Set<LegalStruct> keepOnlyGoodLegals(Set<LegalStruct> legals) {
				Set<LegalStruct> good = new HashSet<LegalStruct>();
				LegalStruct strTemp = new LegalStruct(false);
				
				for (LegalStruct str : legals) {
					if (!incompleteData(str)) {
						if (StringUtils.isEmpty(str.getBlock())){
							strTemp = str;
						} else{
							good.add(str);
						}
					}
				}
				boolean mustAddIt = false;
				for (LegalStruct str : good) {
					if (str.getPlatBook().equals(strTemp.getPlatBook()) 
							&& str.getPlatPage().equals(strTemp.getPlatPage())
							&& str.getLot().equals(strTemp.getLot())
							&& StringUtils.isEmpty(str.getBlock())){
						mustAddIt = true;
					}
				}
				if (mustAddIt || good.isEmpty()){
					good.add(strTemp);
				}
				return good;
			}
			
			@Override
			protected Set<String> getDifferentLegals(Set<LegalStruct> legalStruct) {
				if(isEnableSubdividedLegal()) {
					return super.getDifferentLegals(legalStruct);
				}
				if(isEnableTownshipLegal()) {
					Set<String> foundKeys = new HashSet<String>();
					for (LegalStruct personalDataStruct : legalStruct) {
						if(StringUtils.isNotEmpty(personalDataStruct.getAbs())) {
							String key = "Abs = " + personalDataStruct.getAbs();
							if(!foundKeys.contains(key)) {
								foundKeys.add(key);	
							}
							
						}
					}
					return foundKeys;
				}
				return new HashSet<String>();
			}

			private boolean incompleteData(LegalStruct str) {
				if (str == null) {
					return true;
				}

				boolean emptyPlatBook = StringUtils.isEmpty(str.getPlatBook());
				boolean emptyPlatPage = StringUtils.isEmpty(str.getPlatPage());

				boolean emptyAbs = StringUtils.isEmpty(str.getAbs());

				if (!str.sectional) {
					return (emptyPlatBook && emptyPlatPage);
				}

				return emptyAbs;
			}

			protected boolean testIfExist(Set<LegalStruct> legalStruct2, LegalStruct l, long searchId) {
				if(isEnableSubdividedLegal()) {
					for (LegalStruct p : legalStruct2) {
						if (p.getPlatBook().equals(l.getPlatBook()) && p.getPlatPage().equals(l.getPlatPage()) && p.getLot().equals(l.getLot())) {
							return true;
						}
					}
				}
				if(isEnableTownshipLegal()) {
					for (LegalStruct p : legalStruct2) {
						if (p.getAbs().equals(l.getAbs())) {
							return true;
						}
					}
				}
				return false;
			}

			@Override
			protected void loadDerrivation(TSServerInfoModule module, LegalStruct str) {
				for (Object functionObject : module.getFunctionList()) {
					if (functionObject instanceof TSServerInfoFunction) {
						TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
						switch (function.getIteratorType()) {
						case FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE: {
							String platBook = str.getPlatBook();
							String platPage = str.getPlatPage();
							function.setParamValue(platBook + "/" + platPage);
							break;
						}
						case FunctionStatesIterator.ITERATOR_TYPE_BLOCK: {
							String lot = str.getLot();
							String block = str.getBlock();
							function.setParamValue(block + "/" + lot);
							break;
						}
						case FunctionStatesIterator.ITERATOR_TYPE_ARB: {
							function.setParamValue(str.getAbs());
							break;
						}
						}
					}
				}
			}

			@Override
			protected void treatOnlySubdivision(String sourceKey, LegalI legal, boolean useAlsoSubdivisionName, String subdivisionName) {
				if (isEnableSubdivision() && legal.hasSubdividedLegal()) {
					processSubdivisionLotBlock(legal.getSubdivision().getName(), legal.getSubdivision().getLot(), legal.getSubdivision().getBlock());
				}
			}

			@Override
			protected void treatOnlySubdivisionLegalForCurrentOwner(final Set<LegalStruct> ret, PartyI owner, RegisterDocumentI doc) {
				if (isEnableSubdivision()) {
					for (PropertyI prop : doc.getProperties()) {
						if (prop.hasSubdividedLegal()) {
							SubdivisionI sub = prop.getLegal().getSubdivision();
							LegalStruct struct = new LegalStruct(false);
							struct.setLot(sub.getLot());
							struct.setBlock(sub.getBlock());
							struct.setAddition(sub.getName());

							boolean nameMatched = false;

							if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)) {
								nameMatched = true;
							}

							if (nameMatched && StringUtils.isNotEmpty(struct.getAddition())
									&& (StringUtils.isNotEmpty(struct.getLot()) || StringUtils.isNotEmpty(struct.getBlock()))) {
								ret.add(struct);
							}
						}
					}
				}
			}

			@Override
			protected void performValidationOnList() {
				Set<LegalStruct> tempSet = new HashSet<LegalStruct>();
				if (isEnableSubdivision()) {
					for (LegalStruct personalDataStruct : legalStruct) {
						if (personalDataStruct.isSubdivision()) {
							tempSet.add(personalDataStruct);
						}
					}

					if (tempSet.size() > 0) {
						legalStruct.clear();
						legalStruct.addAll(tempSet);
						return;
					}
				}
				if(isEnableTownshipLegal()) {
					for (LegalStruct personalDataStruct : legalStruct) {
						if (personalDataStruct.isAbs()) {
							tempSet.add(personalDataStruct);
						}
					}

					if (tempSet.size() > 0) {
						legalStruct.clear();
						legalStruct.addAll(tempSet);
						return;
					}
				}
			}

		};
		it.setCheckAlreadyFilledKeyWithDocuments(AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR);
		it.setAdditionalInfoKey(AdditionalInfoKeys.ADI_LOOK_UP_DATA);
		it.setEnableTownshipLegal(false);
		it.setEnableSubdividedLegal(false);
		it.setEnableSubdivision(false);

		return it;
	}

	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		DocTypeSimpleFilter doctypeFilter = (DocTypeSimpleFilter) DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
		String doctypes[] = { DocumentTypes.MISCELLANEOUS, DocumentTypes.LIEN, DocumentTypes.COURT, DocumentTypes.MORTGAGE, DocumentTypes.AFFIDAVIT,
				DocumentTypes.RELEASE };
		doctypeFilter.setDocTypes(doctypes);
		for (String id : gbm.getGbTransfers()) {

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

			String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
			if (StringUtils.isNotEmpty(date) && date.matches("(?is)\\d+/\\d+/\\d+")) {
				module.getFunction(6).forceValue(StringUtils.strip(date.replaceFirst("(?is)(\\d+)/(\\d+)/\\d+", "$1$2"), "0"));
				module.getFunction(7).forceValue(date.replaceFirst("(?is)\\d+/\\d+/(\\d+)", "$1"));
			}

			FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
			((GenericNameFilter) nameFilter).setUseSynonymsBothWays(true);
			nameFilter.setInitAgain(true);

			FilterResponse transferNameFilter = NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module);
			((GenericNameFilter) transferNameFilter).setUseSynonymsBothWays(true);
			transferNameFilter.setInitAgain(true);

			module.addFilter(nameFilter);
			module.addFilter(transferNameFilter);
			module.addFilter(doctypeFilter);
			module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());

			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.getFunction(5).forceValue(COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			module.addIterator(nameIterator);
			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

				date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
				if (StringUtils.isNotEmpty(date) && date.matches("(?is)\\d+/\\d+/\\d+")) {
					module.getFunction(6).forceValue(StringUtils.strip(date.replaceFirst("(?is)(\\d+)/(\\d+)/\\d+", "$1$2"), "0"));
					module.getFunction(7).forceValue(date.replaceFirst("(?is)\\d+/\\d+/(\\d+)", "$1"));
				}

				nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
				((GenericNameFilter) nameFilter).setUseSynonymsBothWays(true);
				nameFilter.setInitAgain(true);

				transferNameFilter = NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module);
				((GenericNameFilter) transferNameFilter).setUseSynonymsBothWays(true);
				transferNameFilter.setInitAgain(true);

				module.addFilter(nameFilter);
				module.addFilter(transferNameFilter);
				module.addFilter(doctypeFilter);
				module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());

				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.getFunction(5).forceValue(COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
				module.addIterator(nameIterator);
				modules.add(module);
			}
		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();

		if (!isParentSite()
				&& (dontMakeTheSearch(module, searchId) || "true".equalsIgnoreCase(global.getSa().getAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND)))) {
			return new ServerResponse();
		}

		List<TSServerInfoModule> modules = getMultipleModules(module, sd);
		if (modules.size() > 1) {
			List<ServerResponse> serverResponses = new ArrayList<ServerResponse>();
			boolean firstSearchBy = true;

			for (TSServerInfoModule mod : modules) {
				// if (verifyModule(mod)) {

				if (firstSearchBy) {
					firstSearchBy = false;
				} else {
					mod.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
				}
				module = transformDateRange(mod);
				ServerResponse res = super.SearchBy(mod, sd);
				if (res != null) {
					serverResponses.add(res);
				}
				// }
			}
			if (!serverResponses.isEmpty()) {
				return mergeResults(serverResponses);
			}
		}

		module = transformDateRange(module);

		return super.SearchBy(module, sd);
	}

	protected ServerResponse mergeResults(List<ServerResponse> serverResponses) {

		ServerResponse response = new ServerResponse();
		Vector<ParsedResponse> rows = new Vector<ParsedResponse>();
		//StringBuffer headRows = new StringBuffer();
		for (ServerResponse res : serverResponses) {
			try {
				if (res.getParsedResponse().getResultRows().size() != 0) {
					rows.addAll(res.getParsedResponse().getResultRows());
				}
				//headRows.append(res.getParsedResponse().getAttribute(SEARCH_CRITERIA)).append("<br/>");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (rows.size() == 0) {
			return ServerResponse.createWarningResponse(NO_DATA_FOUND);
		}

		response.getParsedResponse().setResultRows(rows);

		String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		//header += headRows.toString();
		header += "\n<table width=\"50%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + "<tr><th rowspan=1>" + SELECT_ALL_CHECKBOXES + "</th>"
				+ "<td align=\"center\">Document Content</td></tr>";

		int nrUnsavedDoc = rows.size();

		String footer = "\n</table><br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, nrUnsavedDoc);

		response.getParsedResponse().setHeader(header);
		response.getParsedResponse().setFooter(footer);

		response.getParsedResponse().setOnlyResponse("");
		response.setResult("");

		return response;
	}

	/**
	 * @param module
	 */
	public TSServerInfoModule transformDateRange(TSServerInfoModule module) {
		List<TSServerInfoFunction> functionList = module.getFunctionList();
		for (TSServerInfoFunction tsServerInfoFunction : functionList) {
			if (tsServerInfoFunction.getParamName().contains("from_md")) {
				String fromMd = tsServerInfoFunction.getParamValue();
				fromMd = StringUtils.stripStart(fromMd.replaceAll("(?is)/", ""), "0");
				tsServerInfoFunction.setParamValue(fromMd);
			} else if (tsServerInfoFunction.getParamName().contains("to_md")) {
				String toMd = tsServerInfoFunction.getParamValue();
				toMd = StringUtils.stripStart(toMd.replaceAll("(?is)/", ""), "0");
				tsServerInfoFunction.setParamValue(toMd);
			}
		}

		return module;
	}

	public boolean dontMakeTheSearch(TSServerInfoModule module, long searchId) {

		switch (module.getModuleIdx()) {

		case TSServerInfo.SUBDIVISION_MODULE_IDX: {
			if (module.getFunctionCount() > 4) {

				String legalSearchType = module.getFunction(3).getParamValue();
				if (StringUtils.isNotEmpty(legalSearchType) && "SS".equals(legalSearchType)) {
					String plat = module.getFunction(0).getParamValue().trim();
					if ("/".equals(plat)) {
						plat = "";
					}
					String lotb = module.getFunction(1).getParamValue().trim();
					if ("/".equals(lotb) || lotb.matches("(?is)\\w+/")) {
						lotb = "";
					}
					if (StringUtils.isEmpty(plat) || StringUtils.isEmpty(lotb)) {
						return true;
					}
				} else if (StringUtils.isNotEmpty(legalSearchType) && "AA".equals(legalSearchType)) {
					if (StringUtils.isEmpty(module.getFunction(0).getParamValue().trim())) {
						return true;
					}
				}
			}
		}
		}
		return false;
	}

	void solveBinaryResponse(int viParseID, String rsFunctionName, ServerResponse Response, RawResponseWrapper rw, String imagePath,
			Map<String, Object> extraParams) throws ServerResponseException {

		if ("application/pdf".equalsIgnoreCase(rw.getContentType())) {
			try {
				if (Response.getLastURI().toString().contains("image")) {
					super.solveBinaryResponse(viParseID, rsFunctionName, Response, rw, imagePath, extraParams);
				} else {
					String pdfAsString = PDFUtils.extractTextFromPDF(rw.getBinaryResponse());
					Response.setResult(pdfAsString);
					// System.out.println(pdfAsString);

					// try {
					// FileWriter fw = new FileWriter(new
					// File("D:\\TAD search by doc.txt"), true);
					// fw.write(pdfAsString);
					// fw.write("\n");
					// fw.close();
					// }
					// catch (IOException e) {
					// e.printStackTrace();
					// }
					super.solveHtmlResponse("", viParseID, rsFunctionName, Response, pdfAsString, extraParams);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected ServerResponse performRequest(String page, int methodType, String action, int parserId, String imagePath, String vbRequest,
			Map<String, Object> extraParams) throws ServerResponseException {
		if (page.matches("/FK____\\d+")) {
			ServerResponse response = new ServerResponse();
			response.setError(ServerResponse.NOT_VALID_DOC_ERROR);
			return response;
		} else {
			return super.performRequest(page, methodType, action, parserId, imagePath, vbRequest, extraParams);
		}
	}

	private void checkErrorMessages(ParsedResponse parsedResponse,
			String response) {
		
		if(response.contains("An internal server error occured")) {
			parsedResponse.setError("<font color=\"red\">Data Source error: An internal server error occured</font>");
			return;
		}
		if(response.contains("Site error. Please try again")) {
			parsedResponse.setError("<font color=\"red\">Site error. Please try again.</font>");
			return;
		}
		
		if(response.contains(" is offline ") || response.contains(" is down ")) {
			parsedResponse.setError("<font color=\"red\">" + response + "</font>");
			return;
		}
		
		if(response.contains("504 Gateway Time-out")) {
			parsedResponse.setError("<font color=\"red\">Site error (504 Gateway Time-out). Please try again.</font>");
			return;
		}
		
		if(response.contains("Something unexpected happened while serving the page")) {
			parsedResponse.setError("<font color=\"red\">Site error (Something unexpected happened while serving the page at /v1/search/reference).</font>");
			return;
		}
		
		if(response.contains("INVALID LEGAL ENTERED")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font><br><br>INVALID LEGAL ENTERED<br><br> Please change search criteria and try again.");
			return;
    	}
    	if(response.contains("field cannot be equal")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: Field cannot be equal to High Field<br>Please change search criteria and try again.");
			return;
    	}
    	if(response.contains("Primary legal must be entered")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: Primary legal must be entered<br>Please change search criteria and try again.");
			return;
    	}
    	if(response.contains("required in Low field")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: rick required in Low field, in the same position as High field.<br>Please change search criteria and try again.");
			return;
    	}
    	if(response.contains("required in High field")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: rick required in High field, in the same position as Low field.<br>Please change search criteria and try again.");
			return;
    	}
    	if(response.contains("HIGH OR LOW SUBDIVISION SECONDARY")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: HIGH OR LOW SUBDIVISION SECONDARY LEGAL INVALID AS KEYED<br>Please change search criteria and try again.");
			return;
    	}
    	if(response.contains("INVALID SUBDIVISION SECONDARY LEGAL")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: INVALID SUBDIVISION SECONDARY LEGAL<br>Please change search criteria and try again.");
			return;
    	}
    	
    	if(response.contains("Low field cannot equal to High field")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: Low field cannot equal to High field<br>Please change search criteria and try again.");
			return;
    	}
    	
    	if(response.contains("HIGH OR LOW ACREAGE SECONDARY")) {
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: HIGH OR LOW ACREAGE SECONDARY LEGAL INVALID AS KEYED<br>Please change search criteria and try again.");
			return;
    	}
    	if(response.contains("MAP EDIT NOT FOUND FOR THE NAME GIVEN")) {
//    		parsedResponse.setResultRows(new Vector());
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: MAP EDIT NOT FOUND FOR THE NAME GIVEN<br>Please change search criteria and try again.");
			return;
    	}
    	if(response.contains("SUBDIVISION LEGAL KEYED NOT FOUND IN MAP EDIT")) {
//    		parsedResponse.setResultRows(new Vector());
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: SUBDIVISION LEGAL KEYED NOT FOUND IN MAP EDIT<br>Please change search criteria and try again.");
			return;
    	}
    	if(response.contains("SURVEY LEGAL KEYED DOES NOT EXIST IN MAP EDIT")) {
//    		parsedResponse.setResultRows(new Vector());
    		parsedResponse.setError("<font color=\"red\">No results found</font> <br>ServerResponse: SURVEY LEGAL KEYED DOES NOT EXIST IN MAP EDIT<br>Please change search criteria and try again.");
			return;
    	}
    	if(response.contains("Invalid Doctype Entered")) {
    		parsedResponse.setError("No results found <br><font color=\"black\">ServerResponse: Invalid Doctype Entered</font><br>Please change search criteria and try again.");
			return;
    	}
    	
    	if(response.contains("Incomplete results found - No End Of Search Received")) {
    		parsedResponse.setError("Incomplete results found - No End Of Search Received - please try your search again or rescrict search parameters");
			return;
    	}
    	if(response.contains("You must key in a secondary legal")) {
    		parsedResponse.setError("No results found <br><font color=\"black\">ServerResponse: You must key in a secondary legal</font><br>Please change search criteria and try again.");
			return;
    	}
    	
    	if(response.contains("Exception received: ")) {
    		parsedResponse.setError("There was an error with your request. Please try your search again or change search parameters");
			return;
    	}
    	
    	try {
    		if(!response.startsWith("{") && !response.startsWith("[")) {
    			new JSONObject(response);
    		}
    	} catch (Exception e) {
    		parsedResponse.setError("<font color=\"red\">Site message: " + response + "</font>");
			return;
    	}
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		ParsedResponse parsedResponse = Response.getParsedResponse();
		String response = Response.getResult();

		if (response.contains("** NO MATCHES FOUND FOR SEARCH CRITERIA ENTERED **") 
				|| response.contains("** NO RECORDS SELECTED **")) {
			Response.getParsedResponse().setOnlyResultRows(null);
			return;
		}
		
		if(viParseID != ID_SAVE_TO_TSD) {
			checkErrorMessages(parsedResponse, response);
		}
    	if(StringUtils.isNotEmpty(parsedResponse.getError())) {
    		Response.setError(parsedResponse.getError());
    		parsedResponse.setResponse("");
    		Response.setResult("");
    		return;
    	}
    	
		JSONObject jsonObject = null;
		JSONArray jsonArray = null;
		PropertyI preParsedProperty = null;
		try {
			switch (viParseID) {
			case ID_SEARCH_BY_INSTRUMENT_NO:
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_SUBDIVISION_NAME:
				jsonObject = new JSONObject(response);
				break;
			case ID_SEARCH_BY_MODULE19:
			case ID_SEARCH_BY_MODULE20:
				jsonArray = new JSONArray(response);
				break;
			}
		} catch (JSONException e) {
			Response.getParsedResponse().setError("Cannot parse response.");
			return;
		}
		
		

		switch (viParseID) {
//		case ID_SEARCH_BY_NAME:
//
//			try {
//
//				StringBuilder outputTable = new StringBuilder();
//
//				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediaryInstrument(Response, jsonObject.getJSONArray("searchResults"), outputTable);
//
//				if (smartParsedResponses.size() > 0) {
//					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
//					parsedResponse.setOnlyResponse(outputTable.toString());
//
//					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
//						String header = parsedResponse.getHeader();
//						String footer = parsedResponse.getFooter();
//						header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
//
////						Matcher mat = SEARCH_HEADER.matcher(response);
////						if (mat.find()) {
////							String headRow = mat.group(1);
////							headRow = headRow.replaceAll("(?is)((?:\\r)?\\n)", "$1<br>").replaceAll("(?is)\\s", "&nbsp;");
////							parsedResponse.setAttribute(SEARCH_CRITERIA, headRow);
////							header += headRow;
////						}
//						header += "\n<table width=\"50%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + "<tr><th rowspan=1>" + SELECT_ALL_CHECKBOXES
//								+ "</th>" + "<td>Document Content</td></tr>";
//
//						Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
//						if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
//							footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
//						} else {
//							footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
//						}
//
//						parsedResponse.setHeader(header);
//						parsedResponse.setFooter(footer);
//					}
//				}
//
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			break;

//		case ID_SEARCH_BY_SUBDIVISION_NAME:
//			try {
//
//				StringBuilder outputTable = new StringBuilder();
//
//				Collection<ParsedResponse> smartParsedResponses = new Vector<ParsedResponse>();
//
//				if (Response.getQuerry().contains("type=SS")) {
//					smartParsedResponses = smartParseIntermediarySubdivision(Response, response, outputTable);
//				} else if (Response.getQuerry().contains("type=AA")) {
//					smartParsedResponses = smartParseIntermediaryAcreage(Response, response, outputTable);
//				}
//
//				if (smartParsedResponses.size() > 0) {
//					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
//					parsedResponse.setOnlyResponse(outputTable.toString());
//
//					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
//						String header = parsedResponse.getHeader();
//						String footer = parsedResponse.getFooter();
//						header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
//
////						Matcher mat = SEARCH_HEADER.matcher(response);
////						if (mat.find()) {
////							String headRow = mat.group(1);
////							headRow = headRow.replaceAll("(?is)((?:\\r)?\\n)", "$1<br>").replaceAll("(?is)\\s", "&nbsp;");
////							parsedResponse.setAttribute(SEARCH_CRITERIA, headRow);
////							header += headRow;
////						}
//						header += "\n<table width=\"50%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + "<tr><th rowspan=1>" + SELECT_ALL_CHECKBOXES
//								+ "</th>" + "<td>Document Content</td></tr>";
//
//						Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
//						if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
//							footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
//						} else {
//							footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
//						}
//
//						parsedResponse.setHeader(header);
//						parsedResponse.setFooter(footer);
//					}
//				}
//
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			break;
		
		case ID_SEARCH_BY_SUBDIVISION_NAME:
			
			try {
				preParsedProperty = parseMapEditRecord(jsonObject.getJSONObject("mapEditRecord"));
			} catch (Exception e) {
				logger.error("Cannt parse mapEditRecord in ID_SEARCH_BY_SUBDIVISION_NAME for searchId " + searchId , e);
			}
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_INSTRUMENT_NO:
			
			SearchType searchType = SearchType.NA;
			switch (viParseID) {
			case ID_SEARCH_BY_INSTRUMENT_NO:
				searchType = SearchType.IN;
				break;
			case ID_SEARCH_BY_SUBDIVISION_NAME:
				if(Response.getQuerry().contains("legal_type=SS")) {
					searchType = SearchType.LS;
				} else if(Response.getQuerry().contains("legal_type=AA")) {
					searchType = SearchType.LU;
				}
				break;
			case ID_SEARCH_BY_NAME:
				searchType = SearchType.GI;
				if(Response.getQuerry().contains("prop_nonprop=P")) {
					searchType = SearchType.GT;
				}
				break;
			}
			
			try {

				StringBuilder outputTable = new StringBuilder();

				Collection<ParsedResponse> smartParsedResponses = parseIntermediary(Response, jsonObject.getJSONArray("searchResults"), searchType, preParsedProperty, outputTable);

				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());

					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
						String header = parsedResponse.getHeader();
						String footer = parsedResponse.getFooter();
						header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");

//						Matcher mat = SEARCH_HEADER.matcher(response);
//						if (mat.find()) {
//							String headRow = mat.group(1);
//							headRow = headRow.replaceAll("(?is)((?:\\r)?\\n)", "$1<br>").replaceAll("(?is)\\s", "&nbsp;");
//							parsedResponse.setAttribute(SEARCH_CRITERIA, headRow);
//							header += headRow;
//						}
						header += "\n<table width=\"50%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + "<tr><th rowspan=1>" + SELECT_ALL_CHECKBOXES
								+ "</th>" + "<td>Document Content</td></tr>";

						Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
						if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
							footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
						} else {
							footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
						}

						parsedResponse.setHeader(header);
						parsedResponse.setFooter(footer);
					}
				}
			} catch (Exception e) {
				logger.error("Cannot parse searchResults in ID_SEARCH_BY_SUBDIVISION_NAME for searchId " + searchId , e);
			}
			break;

		case ID_SEARCH_BY_MODULE19:
		case ID_SEARCH_BY_MODULE20:
				
			if (jsonArray == null || jsonArray.length() == 0){
				Response.getParsedResponse().setOnlyResultRows(null);
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			boolean isSubdivisionNameLookup = false;
			
			if (viParseID == ID_SEARCH_BY_MODULE19){
				isSubdivisionNameLookup = true;
			}
			
			Collection<ParsedResponse> smartParsedResponses = smartParseLookup(Response, jsonArray, response, outputTable, isSubdivisionNameLookup);
				
			if (smartParsedResponses.size() > 0) {
				String footer = parsedResponse.getFooter();     
	               	
				String header = "<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"/title-search" + URLMaping.PARENT_SITE_ACTIONS + "\"" + " method=\"POST\" > "
						+ "<input type=\"hidden\" name=\""+ TSOpCode.OPCODE + "\" value=\""+ ParentSiteActions.SUBDIVISION_NAME_LOOKUP + "\">"
						+ "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\">" 
						+ "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + mSearch.getSearchID() + "\"> "; 
	               		
	               		
				header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
				
				if (isSubdivisionNameLookup){
					header += "<tr><th>Select</th><th>Edit Name</th><th>Primary Legal</th><th>File Date</th><th>Vacate Date</th><th>Alternate</th><th>Force Error</th></tr>\n";
				} else{
					header += "<tr><th>Select</th><th>Primary Legal</th><th>Edit Name</th><th>File Date</th><th>Vacate Date</th><th>Force Error</th></tr>\n";
				}
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					
				footer = "\n</table>" + "<input  type=\"submit\" class=\"button\" name=\"Button\" value=\"Select Legal\">\r\n</form>\n" ;
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setHeader(header);
				parsedResponse.setFooter(footer);
				
				parsedResponse.setAttribute(PARENT_SITE_LOOKUP_MODE, true);
			} else {
				Response.getParsedResponse().setOnlyResultRows(null);
				return;
			}
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			DocumentI document = parsedResponse.getDocument();

			if (document != null) {
				msSaveToTSDFileName = document.getId() + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
			}

			break;

		case ID_GET_LINK:
			if (Response.getQuerry().indexOf("automaticNameSearch") >= 0 || Response.getQuerry().indexOf("submit") >= 0) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;

		default:
			break;
		}
	}
	
	private PropertyI parseMapEditRecord(JSONObject jsonObject) throws JSONException {
		PropertyI prop = Property.createEmptyProperty();

		String mapVolPage = jsonObject.getString(MapEditRecordKeys.primaryLegalLow.toString());
		if (StringUtils.isNotBlank(mapVolPage)) {
			String[] vp = mapVolPage.split("/");
			if (vp.length == 2) {
				prop.getLegal().getSubdivision().setPlatBook(vp[0].replaceAll("(?is)\\*", "").trim());
				prop.getLegal().getSubdivision().setPlatPage(vp[1].replaceAll("(?is)\\*", "").trim());
			} else {
				prop.getLegal().getTownShip().setAbsNumber(mapVolPage);
			}
		}
		
		if(jsonObject.has(MapEditRecordKeys.documentNumber.toString())) {
			String documentNumber = jsonObject.getString(MapEditRecordKeys.documentNumber.toString());
			if(StringUtils.isNotBlank(documentNumber)) {
				prop.getLegal().getSubdivision().setPlatInstrument(documentNumber);
			}
		}
		
		if(jsonObject.has(MapEditRecordKeys.fileDate.toString())) {
			String fileDate = jsonObject.getString(MapEditRecordKeys.fileDate.toString());
			if(StringUtils.isNotBlank(fileDate)) {
				Date date = Util.dateParser3(fileDate);
				if (date != null) {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(date);
					if(calendar.get(Calendar.YEAR) != 1900 
							&& calendar.get(Calendar.MONTH) != 0 
							&& calendar.get(Calendar.DAY_OF_MONTH) != 1) {
						prop.getLegal().getSubdivision().setPlatInstrumentYear(Integer.toString(calendar.get(Calendar.YEAR)));
					}
				}
			}
		}

		String editName = jsonObject.getString(MapEditRecordKeys.editName.toString());
		if(StringUtils.isNotBlank(editName)) {
			
			Pattern SEC_PAT = Pattern.compile("(?is)\\bSEC\\s*(\\d+)");
			Matcher mat = SEC_PAT.matcher(editName);
			if (mat.find()) {
				prop.getLegal().getTownShip().setSection(mat.group(1));
				editName = editName.replace(mat.group(), "");
			}
			
			Pattern PHASE_PAT = Pattern.compile("(?is)\\bPH\\s*([\\dA-Z]+)");
			mat = PHASE_PAT.matcher(editName);
			if (mat.find()) {
				prop.getLegal().getSubdivision().setPhase(mat.group(1));
				editName = editName.replace(mat.group(), "");
			}
			
			editName = editName.replaceFirst("(?is)\\s*,\\s*", " ").trim();
			prop.getLegal().getSubdivision().setName(editName);
			
		}

		if (jsonObject.has(MapEditRecordKeys.masterRemarks.toString())) {
			prop.getLegal().setFreeForm(jsonObject.getString(MapEditRecordKeys.masterRemarks.toString()));
		}

		return prop;
	}

	private Collection<ParsedResponse> smartParseLookup(ServerResponse response, JSONArray jsonArray, String rsResponce, StringBuilder outputTable, boolean isSubdivisionNameLookup) {
	    	Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
	    	
	    	outputTable.append("<table BORDER='1' CELLPADDING='2'>");
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					StringBuffer row = new StringBuffer();
					String primaryLegal = (String) jsonObject.get("primaryLegal");
					String editName = (String) jsonObject.get("editName");
					String fileDate = (String) jsonObject.get("fileDate");
					
					boolean allEmpty = true;
					@SuppressWarnings("rawtypes")
					Iterator keys = jsonObject.keys();
					while (keys.hasNext()) {
						String type = (String)keys.next();
						if(StringUtils.isNotBlank(jsonObject.get(type).toString())) {
							allEmpty = false;
							break;
						}
					}
					
					if(allEmpty) {
						continue;
					}
					
					if (isSubdivisionNameLookup){
						row.append("<td>").append(editName).append("</td>");
						row.append("<td>").append(primaryLegal).append("</td>");
						row.append("<td>").append(fileDate).append("</td>");
						String vacateDate = (String) jsonObject.get("vacateDate");
						String alternate = (String) jsonObject.get("alternate");
						String forceError = (String) jsonObject.get("forceError");
						
						row.append("<td>").append(StringUtils.isNotEmpty(vacateDate) ? vacateDate : "&nbsp;").append("</td>");
						row.append("<td>").append(StringUtils.isNotEmpty(alternate) ? alternate : "&nbsp;").append("</td>");
						row.append("<td>").append(StringUtils.isNotEmpty(forceError) ? forceError : "&nbsp;").append("</td>");
						
					} else{
						row.append("<td>").append(primaryLegal).append("</td>");
						row.append("<td>").append(editName).append("</td>");
						row.append("<td>").append(fileDate).append("</td>");
						
						String vacateDate = (String) jsonObject.get("vacateDate");
						String forceError = (String) jsonObject.get("forceError");
						row.append("<td>").append(StringUtils.isNotEmpty(vacateDate) ? vacateDate : "&nbsp;").append("</td>");
						row.append("<td>").append(StringUtils.isNotEmpty(forceError) ? forceError : "&nbsp;").append("</td>");
					}
					
					
					String typeOfLegalSearch = "";
					if (primaryLegal.matches("(?is)\\w+/\\w+")){
						typeOfLegalSearch = "SS";
					} else{
						typeOfLegalSearch = "AA";
					}
					
					String value = "primaryLegal=" + primaryLegal + "/____@@typeOfSearch=" + typeOfLegalSearch;
					
					ParsedResponse currentResponse = new ParsedResponse();

//					String legalLink = createPartialLink(TSConnectionURL.idGET, TSServerInfo.ARCHIVE_DOCS_MODULE_IDX) + 
//							"&primaryLegal=" + primaryLegal + 
//							"&editName=" + editName + 
//							"&fileDate=" +  fileDate;
					
				
					String radioStart = "<input type=\"radio\" name=\"doclink\" value=\"";
					currentResponse.setOnlyResponse("<tr><td>" + radioStart + URLEncoder.encode(value, "UTF-8") + "\"> " + 
							"</td>" + row.toString() + "</tr>");
					outputTable.append(currentResponse.getResponse());
					responses.add(currentResponse);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			outputTable.append("</table>");
			return responses;
		}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		} else {
			objectModuleSource = getSearch().getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}
		outputTable.append("<table BORDER='1' CELLPADDING='2'>");

		table = table.replaceAll(END_SEARCH_FOOTER.toString(), "");

		LinkedHashMap<String, LinkedHashSet<String>> bulkDocuments = new LinkedHashMap<String, LinkedHashSet<String>>();
		table = table
				.replaceAll(
						"(?is)(\\*{5}------------------------------------------------------------------------\\s*((?:\\r)?\\n)\\s*\\*\\*DUPLICATE OF ENTRY.*?((?:\\r)?\\n))",
						"");

		String[] pages = table.split(PAGE_SPLITTER.toString());
		if (pages.length > 0) {
			for (String page : pages) {
				page = page.trim();
				if (StringUtils.isEmpty(page)) {
					continue;
				}
				String[] items = page.split(ITEM_SPLITTER.toString());
				for (String item : items) {
					if (item.contains(" REQUESTOR: ")) {
						continue;
					}
					item = item.replaceFirst("(?is)\\A\\s*((?:\\r)?\\n)", "");
					String[] rows = item.split("(?is)((?:\\r)?\\n)");
					String key = "";
					if (rows.length > 0) {
						key = rows[0];
						key = key.replaceFirst("(?is).*(\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+)", "$1").replaceFirst("(?is)\\s{5,}\\d{3}\\s*$", "").trim();
						key = key.replaceAll("(?is)\\s+", "_");
					}
					if (rows.length > 1) {
						if (rows[1].matches("(?is).*?\\s+([A-Z\\d]+\\s*/\\s*[A-Z\\d]+)\\s*$")) {
							String row1 = rows[1];
							row1 = row1.replaceFirst("(?is).*?\\s+([A-Z\\d]+\\s*/\\s*[A-Z\\d]+)\\s*$", "$1").trim();
							if (row1.matches("(?is)([A-Z\\d]+\\s*/\\s*[A-Z\\d]+)")) {
								key += "_" + row1;
							}
						}
					}
					if (StringUtils.isNotEmpty(key)) {
						if (!bulkDocuments.containsKey(key)) {
							LinkedHashSet<String> allLines = new LinkedHashSet<String>();
							for (int i = 0; i < rows.length; i++) {
								allLines.add(rows[i]);
							}
							bulkDocuments.put(key, allLines);
						} else if (bulkDocuments.containsKey(key)) {
							LinkedHashSet<String> allLines = bulkDocuments.get(key);
							if (rows.length > 1) {
								for (int i = 1; i < rows.length; i++) {
									allLines.add(rows[i]);
								}
							}
							bulkDocuments.put(key, allLines);
						}
					}
				}
			}
		}
		Set<Entry<String, LinkedHashSet<String>>> bdEntrySet = bulkDocuments.entrySet();
		for (Entry<String, LinkedHashSet<String>> element : bdEntrySet) {
			InstrumentI instr = new Instrument();

			ParsedResponse currentResponse = new ParsedResponse();
			LinkedHashSet<String> bulkDocument = element.getValue();
			int lineCounter = 0;

			String nameUndefinedLine1 = "", nameUndefinedLine2 = "";
			StringBuffer party1 = new StringBuffer();
			StringBuffer party2 = new StringBuffer();
			StringBuffer refBy = new StringBuffer();
			StringBuffer others = new StringBuffer();

			String amount = "", serverDocType = "", doctype = "", subDocType = "";
			String numberOfPages = "";
			// String info = "";

			StringBuffer documentIndex = new StringBuffer();
			for (String line : bulkDocument) {
				documentIndex.append(line.replaceAll("(?is)\\s", "&nbsp;")).append("\r\n").append("<br>");
				if (lineCounter == 0) {
					// ANDERSON,JASON + 12/22/2009 JDGABS 2009092730 001

					nameUndefinedLine1 = line.replaceFirst("(?is)(.+)(\\s+\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+.*)", "$1");
					nameUndefinedLine1 = nameUndefinedLine1.replaceFirst("(?is)\\+\\s*$", "").trim();

					if (line.matches("(?is).*?\\s+(\\d{3})\\s*$")) {
						numberOfPages = line.replaceFirst("(?is).*?\\s+(\\d{3})\\s*$", "$1");
					}

					line = line.replaceFirst("(?is).*(\\s*\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+)", "$1").replaceFirst("(?is)\\s{5,}\\d{3}\\s*$", "");

					if (line.matches("(?is)\\A\\s*\\d+\\s*/\\s*\\d+\\s*/\\s*\\d+\\s+.*")) {
						String instrumentDate = line.replaceFirst("(?is)\\A\\s*(\\d+\\s*/\\s*\\d+\\s*/\\s*\\d+)\\s+.*", "$1");
						line = line.replaceFirst("(?is)\\A\\s*\\d+\\s*/\\s*\\d+\\s*/\\s*\\d+(\\s+.*)", "$1");
						if (StringUtils.isNotEmpty(instrumentDate)) {
							Date date = Util.dateParser3(instrumentDate);
							if (date != null) {
								instr.setDate(date);
								Calendar calendar = Calendar.getInstance();
								calendar.setTime(date);
								instr.setYear(calendar.get(Calendar.YEAR));
							}
						}
					}
					if (line.matches("(?is)\\s+[A-Z]{3,}\\s+.*") || line.matches("(?is)\\s+[A-Z]{3,}\\s*$")) {
						serverDocType = line.replaceFirst("(?is)\\s+([A-Z]{3,})\\s+.*", "$1").trim();
						line = line.replaceFirst("(?is)\\s+[A-Z]{3,}(\\s+.*)", "$1");
						if (StringUtils.isNotEmpty(serverDocType)) {
							doctype = DocumentTypes.getDocumentCategory(serverDocType, searchId);
							instr.setDocType(doctype.trim());
							subDocType = DocumentTypes.getDocumentSubcategory(serverDocType, searchId);
							instr.setDocSubType(subDocType);
							// info += doctype.trim();
						}
					}
					if (line.matches("(?is)\\s*(?:[A-Z]+)?\\d{3,}\\s*$")) {
						String instrumentNumber = line.replaceFirst("(?is)\\s*((?:[A-Z]+)?\\d{3,})\\s*$", "$1");
						line = line.replaceFirst("(?is)\\s*(?:[A-Z]+)?\\d{3,}\\s*$", "");
						if (StringUtils.isNotEmpty(instrumentNumber)) {
							instr.setInstno(instrumentNumber.trim());
							// info += "###" + instrumentNumber.trim();
						}
					}
					lineCounter++;
				} else if (lineCounter == 1) {
					if (!line.startsWith("     ") && !line.trim().startsWith("Ref ")) {
						String possibleName = line.replaceFirst("(?is)(.+)\\s{4,}.*", "$1");

						if (!possibleName.trim().matches("(?is)[\\d-]+")) {
							nameUndefinedLine2 = possibleName.trim();
							nameUndefinedLine2 = nameUndefinedLine2.replaceFirst("(?is)\\+\\s*$", "").trim();
							line = line.replace(possibleName, "");
						}
					} else if (line.trim().startsWith("Ref ")) {
						refBy.append(line.trim()).append("\n");
						continue;
					}
					if (line.matches("(?is).*\\b\\w+\\s*/\\s*\\d+\\s*$")) {
						String bookPage = line.replaceFirst("(?is).*\\b(\\w+\\s*/\\s*\\d+)\\s*$", "$1");
						line = line.replaceFirst("(?is)\\b\\w+\\s*/\\s*\\d+\\s*$", "");
						if (StringUtils.isNotEmpty(bookPage)) {
							String[] bp = bookPage.trim().split("\\s*/\\s*");
							if (bp.length == 2) {
								instr.setBook(bp[0]);
								instr.setPage(bp[1]);
							}
							// info += "###" + bookPage.trim();
						}
					}
					if (line.matches("(?is).*\\s+\\$\\s*[\\d,\\.]+\\s.*")) {
						amount = line.replaceFirst("(?is).*\\s+(\\$\\s*[\\d,\\.]+)\\s+.*", "$1");
						line = line.replaceFirst("(?is)(.*)\\s+\\$\\s*[\\d,\\.]+", "$1");
						if (StringUtils.isNotEmpty(amount)) {
							amount = amount.replaceAll("(?is)[$,]+", "");
						}
					} else if (StringUtils.isNotBlank(line.trim())) {
						others.append(line.trim()).append("\n");
					}
					lineCounter++;
				} else if (line.trim().startsWith("Ref ")) {
					refBy.append(line.trim()).append("\n");
				} else {
					others.append(line.trim()).append("\n");
				}
			}

			// try {
			// File file = new File("D:\\TAD doctype.txt");
			// String fileContent = FileUtils.readFileToString(file);
			// if (!fileContent.contains(doctype + "#")) {
			// FileWriter fw = new FileWriter(file, true);
			// fw.write(info);
			// fw.write("\n");
			// fw.close();
			// }
			// } catch (IOException e) {
			// e.printStackTrace();
			// }

			RegisterDocument document = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr));
			document.setInstrument(instr);
			document.setType(SimpleChapterUtils.DType.ROLIKE);
			document.setRecordedDate(instr.getDate());
			document.setServerDocType(serverDocType);
			document.setDataSource(dataSite.getSiteTypeAbrev());
			document.setSiteId(getServerID());
			document.setSearchType(SearchType.GI);
			
			if (isParentSite()){
    			document.setSavedFrom(SavedFromType.PARENT_SITE);
			} else{
				document.setSavedFrom(SavedFromType.AUTOMATIC);
			}

			if (doctype.equals("POA") || doctype.equals("CONASN")) {
				party1.append(nameUndefinedLine1);
				party2.append(nameUndefinedLine2);
			} else {
				party1.append(nameUndefinedLine2);
				party2.append(nameUndefinedLine1);
			}

			boolean isCAD_MAP_IMAGE = false;

			PartyI grantors = new com.stewart.ats.base.parties.Party(PType.GRANTOR);
			PartyI grantees = new com.stewart.ats.base.parties.Party(PType.GRANTEE);

			String names[] = null;
			String[] gtors = party1.toString().replaceAll(";", "&").split("\n");

			for (int i = 0; i < gtors.length; i++) {
				if (StringUtils.isNotBlank(gtors[i].trim())) {
					if (gtors[i].trim().toLowerCase().contains("cad map image")) {
						isCAD_MAP_IMAGE = true;
					}
					names = StringFormats.parseNameNashville(gtors[i].trim());

					grantors = addName(grantors, names);
				}
			}
			document.setGrantor(grantors);

			String[] gtee = party2.toString().replaceAll(";", "&").split("\n");
			for (int i = 0; i < gtee.length; i++) {
				if (StringUtils.isNotBlank(gtee[i].trim())) {
					if (gtee[i].trim().toLowerCase().contains("cad map image")) {
						isCAD_MAP_IMAGE = true;
					}
					names = StringFormats.parseNameNashville(gtee[i].trim());
					grantees = addName(grantees, names);
				}
			}
			document.setGrantee(grantees);

			if (StringUtils.isNotEmpty(doctype)) {
				document = setStuffToDocument(0, doctype, document);
			}
			if (refBy.length() > 0) {
//				parseReferences(refBy, document, currentResponse);
			}

			currentResponse.setDocument(document);
			String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);

			String checkBox = "checked";
			if (isInstrumentSaved("gogo", document, null, false) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
				checkBox = "saved";
			} else {
				checkBox = DOCLINK_CHECKBOX_START + linkPrefix + "FK____" + document.getId() + "'>";

				LinkInPage linkInPage = new LinkInPage(linkPrefix + "FK____" + document.getId(), linkPrefix + "FK____" + document.getId(),
						TSServer.REQUEST_SAVE_TO_TSD);

				if (getSearch().getInMemoryDoc(linkPrefix + "FK____" + document.getId()) == null) {
					getSearch().addInMemoryDoc(linkPrefix + "FK____" + document.getId(), currentResponse);

					/**
					 * Save module in key in additional info. The key is
					 * instrument number that should be always available.
					 */
					String keyForSavingModules = getKeyForSavingInIntermediary(document.getInstno());
					getSearch().setAdditionalInfo(keyForSavingModules, moduleSource);
				}
				currentResponse.setPageLink(linkInPage);
			}
			setImageLinkToDocument(document, null, serverDocType, numberOfPages, isCAD_MAP_IMAGE);

			currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + documentIndex.toString() + "</td></tr>");
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + documentIndex.toString() + "</td></tr>");
			currentResponse.setUseDocumentForSearchLogRow(true);

			intermediaryResponse.add(currentResponse);

			outputTable.append(currentResponse.getResponse());

		}
		outputTable.append("</table>");

		return intermediaryResponse;
	}

	/**
	 * @param jsonObject
	 * @param document
	 * @param currentResponse
	 * @throws JSONException 
	 */
	public void parseReferences(JSONObject jsonObject, RegisterDocument document, ParsedResponse currentResponse) throws JSONException {

		Instrument instrCrossRef = new Instrument();
		
		String instrument = jsonObject.getString(ReferenceKeys.documentNumber.toString());
		if(StringUtils.isNotBlank(instrument)) {
			instrCrossRef.setInstno(instrument);
		} else {
			instrument = null;
		}
		String book = null;
		String page = null;
		
		if(jsonObject.has(ReferenceKeys.volumePage.toString())) {
			String volumePage = jsonObject.getString(ReferenceKeys.volumePage.toString());
			if(StringUtils.isNotBlank(volumePage)) {
				String[] split = volumePage.split("\\s*/\\s*");
				if(split.length == 2) {
					instrCrossRef.setBook(split[0]);
					instrCrossRef.setPage(split[1]);
					book = instrCrossRef.getBook();
					page = instrCrossRef.getPage();
				}
			}
		}
		
		Calendar fileCalendar = null;
		if(jsonObject.has(ReferenceKeys.fileDate.toString())) {
			String fileDateAsString = jsonObject.getString(ReferenceKeys.fileDate.toString());
			if(fileDateAsString.matches("\\d{1,2}/\\d{2}/\\d{4}")) {
				Date date = Util.dateParser3(fileDateAsString);
				if (date != null) {
					instrCrossRef.setDate(date);
					fileCalendar = Calendar.getInstance();
					fileCalendar.setTime(date);
					instrCrossRef.setYear(fileCalendar.get(Calendar.YEAR));
				}
			}
		}
		
		String serverDocType = null;
		if(jsonObject.has(ReferenceKeys.documentType.toString())) {
			serverDocType = jsonObject.getString(ReferenceKeys.documentType.toString());
			if (StringUtils.isNotEmpty(serverDocType)) {
				instrCrossRef.setDocType(DocumentTypes.getDocumentCategory(serverDocType, searchId));
				instrCrossRef.setDocSubType(DocumentTypes.getDocumentSubcategory(serverDocType, searchId));
			}
		}

		// String fromMd = getSearchAttribute(SearchAttributes.FROMDATE_MMDD);
		// String fromYear = getSearchAttribute(SearchAttributes.FROMDATE_YEAR);
		// String toMd = getSearchAttribute(SearchAttributes.TODATE_MMDD);
		// String toYear = getSearchAttribute(SearchAttributes.TODATE_YEAR);

		if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {

			TSServerInfoModule module = this.getDefaultServerInfo().getModuleForSearch(TSServerInfo.INSTR_NO_MODULE_IDX, new SearchDataWrapper());
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Save with crossreferences");

			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CROSSREF_DOC_SOURCE, document.getDocType());
			
			module.forceValue(0, book + "/" + page);
			
			if(StringUtils.isNotBlank(serverDocType)) {
				module.forceValue(6, serverDocType);
			}
			
			if(fileCalendar != null) {
				String day = fileCalendar.get(Calendar.DATE) < 10 ? "0" + fileCalendar.get(Calendar.DATE):fileCalendar.get(Calendar.DATE) + "";
				
				module.forceValue(2, (fileCalendar.get(Calendar.MONTH) + 1) + day);
				module.forceValue(4, (fileCalendar.get(Calendar.MONTH) + 1) + day);
				
				module.forceValue(3, fileCalendar.get(Calendar.YEAR) + "");
				module.forceValue(5, fileCalendar.get(Calendar.YEAR) + "");
				
			}
			
			GenericInstrumentFilter genericInstrumentFilter = new GenericInstrumentFilter(searchId);
			HashMap<String, String> filterCriteria = new HashMap<>();
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			genericInstrumentFilter.addDocumentCriteria(filterCriteria);
			module.addFilter(genericInstrumentFilter);
			
			// module.forceValue(2, StringUtils.strip(fromMd.replaceAll("(?is)/", ""), "0"));
			// module.forceValue(3, fromYear);
			// module.forceValue(4, StringUtils.strip(toMd.replaceAll("(?is)/", ""), "0"));
			// module.forceValue(5, toYear);

			String tempBookPageLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.INSTR_NO_MODULE_IDX) + "&book=" + book + "&page=" + page
					+ "&dummy=" + module.getFunction(0).getValue() + "&isSubResult=true";

			LinkInPage linkInPage = new LinkInPage(module, TSServer.REQUEST_SEARCH_BY_REC);
			linkInPage.setOnlyLink(tempBookPageLink);

			// String dummy = instrCrossRef.getBook() + "/" + instrCrossRef.getBook();
			
			// String link = createPartialLink(TSConnectionURL.idGET, TSServerInfo.INSTR_NO_MODULE_IDX) +
			// "&book=" + instrCrossRef.getBook() + "&page=" + instrCrossRef.getPage() + "&dummy=" + dummy + "&isSubResult=true";

			// String link = CreatePartialLink(TSConnectionURL.idGET)
			
			ParsedResponse prChild = new ParsedResponse();
			prChild.setPageLink(linkInPage);
			currentResponse.addOneResultRowOnly(prChild);
		}
		if (StringUtils.isNotEmpty(instrument)) {

			TSServerInfoModule module = this.getDefaultServerInfo().getModuleForSearch(TSServerInfo.INSTR_NO_MODULE_IDX, new SearchDataWrapper());
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Save with crossreferences");

			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CROSSREF_DOC_SOURCE, document.getDocType());
			
			module.forceValue(0, instrument);
			if(StringUtils.isNotBlank(serverDocType)) {
				module.forceValue(6, serverDocType);
			}
			
			if(fileCalendar != null) {
				String day = fileCalendar.get(Calendar.DATE) < 10 ? "0" + fileCalendar.get(Calendar.DATE):fileCalendar.get(Calendar.DATE) + "";
				
				module.forceValue(2, (fileCalendar.get(Calendar.MONTH) + 1) + day);
				module.forceValue(4, (fileCalendar.get(Calendar.MONTH) + 1) + day);
				
				module.forceValue(3, fileCalendar.get(Calendar.YEAR) + "");
				module.forceValue(5, fileCalendar.get(Calendar.YEAR) + "");
				
			}
			
			GenericInstrumentFilter genericInstrumentFilter = new GenericInstrumentFilter(searchId);
			HashMap<String, String> filterCriteria = new HashMap<>();
			filterCriteria.put("InstrumentNumber", module.getFunction(0).getValue());
			genericInstrumentFilter.addDocumentCriteria(filterCriteria);
			module.addFilter(genericInstrumentFilter);
			
			// module.forceValue(2, StringUtils.strip(fromMd.replaceAll("(?is)/", ""), "0"));
			// module.forceValue(3, fromYear);
			// module.forceValue(4, StringUtils.strip(toMd.replaceAll("(?is)/", ""), "0"));
			// module.forceValue(5, toYear);

			String tempInstrumentLink = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.INSTR_NO_MODULE_IDX) + "&instrument=" + instrument + "&dummy="
					+ module.getFunction(0).getValue() + "&isSubResult=true";

			LinkInPage linkInPage = new LinkInPage(module, TSServer.REQUEST_SEARCH_BY_REC);
			linkInPage.setOnlyLink(tempInstrumentLink);

			ParsedResponse prChild = new ParsedResponse();
			prChild.setPageLink(linkInPage);

			currentResponse.addOneResultRowOnly(prChild);
		}

		document.getParsedReferences().add(instrCrossRef);
	}

	public Collection<ParsedResponse> smartParseIntermediaryAcreage(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		} else {
			objectModuleSource = getSearch().getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}

		outputTable.append("<table BORDER='1' CELLPADDING='2'>");

		table = table.replaceAll(END_SEARCH_FOOTER.toString(), "");

		LinkedHashMap<String, LinkedHashSet<String>> bulkDocuments = new LinkedHashMap<String, LinkedHashSet<String>>();
		table = table
				.replaceAll(
						"(?is)(\\*{5}------------------------------------------------------------------------\\s*((?:\\r)?\\n)\\s*\\*\\*DUPLICATE OF ENTRY.*?((?:\\r)?\\n))",
						"");

		String[] pages = table.split(PAGE_SPLITTER.toString());
		if (pages.length > 0) {
			for (String page : pages) {
				page = page.trim();
				if (StringUtils.isEmpty(page)) {
					continue;
				}
				String[] items = page.split(ITEM_SPLITTER.toString());
				for (String item : items) {
					if (item.contains(" REQUESTOR: ")) {
						continue;
					}
					item = item.replaceFirst("(?is)\\A\\s*((?:\\r)?\\n)", "");
					String[] rows = item.split("(?is)((?:\\r)?\\n)");
					String key = "";
					if (rows.length > 0) {
						key = rows[0];
						key = key.replaceFirst("(?is).*(\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+)", "$1").replaceFirst("(?is)\\s{5,}\\d{3}\\s*$", "").trim();
						key = key.replaceAll("(?is)\\s+", "_");
					}
					if (rows.length > 1) {
						if (rows[1].matches("(?is).*?\\s+([A-Z\\d]+\\s*/\\s*[A-Z\\d]+)\\s*$")) {
							String row1 = rows[1];
							row1 = row1.replaceFirst("(?is).*?\\s+([A-Z\\d]+\\s*/\\s*[A-Z\\d]+)\\s*$", "$1").trim();
							if (row1.matches("(?is)([A-Z\\d]+\\s*/\\s*[A-Z\\d]+)")) {
								key += "_" + row1;
							}
						}
					}
					if (StringUtils.isNotEmpty(key)) {
						if (!bulkDocuments.containsKey(key)) {
							LinkedHashSet<String> allLines = new LinkedHashSet<String>();
							for (int i = 0; i < rows.length; i++) {
								allLines.add(rows[i]);
							}
							bulkDocuments.put(key, allLines);
						} else if (bulkDocuments.containsKey(key)) {
							LinkedHashSet<String> allLines = bulkDocuments.get(key);
							if (rows.length > 0) {
								allLines.add("<hr>");
								for (int i = 0; i < rows.length; i++) {
									allLines.add(rows[i]);
								}
							}
							bulkDocuments.put(key, allLines);
						}
					}
				}
			}
		}
		Set<Entry<String, LinkedHashSet<String>>> bdEntrySet = bulkDocuments.entrySet();
		for (Entry<String, LinkedHashSet<String>> element : bdEntrySet) {
			InstrumentI instr = new Instrument();

			ParsedResponse currentResponse = new ParsedResponse();
			LinkedHashSet<String> bulkDocument = element.getValue();
			int lineCounter = 0;

			String nameUndefinedLine1 = "", nameUndefinedLine2 = "";
			StringBuffer party1 = new StringBuffer();
			StringBuffer party2 = new StringBuffer();
			StringBuffer refBy = new StringBuffer();
			StringBuffer legal = new StringBuffer();
			StringBuffer others = new StringBuffer();

			String amount = "", serverDocType = "", doctype = "", subDocType = "";
			String numberOfPages = "";

			// String info = "";
			boolean foundHR = false;
			StringBuffer documentIndex = new StringBuffer();
			for (String line : bulkDocument) {
				documentIndex.append(line.replaceAll("(?is)\\s", "&nbsp;")).append("\r\n").append("<br>");
				if (lineCounter == 0) {
					// DROZD,B & 01/18/1952 DED 5201104 007

					nameUndefinedLine1 = line.replaceFirst("(?is)(.+)(\\s+\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+.*)", "$1");
					nameUndefinedLine1 = nameUndefinedLine1.replaceFirst("(?is)\\+\\s*$", "").trim();

					if (line.matches("(?is).*?\\s+(\\d{3})\\s*$")) {
						numberOfPages = line.replaceFirst("(?is).*?\\s+(\\d{3})\\s*$", "$1");
					}

					line = line.replaceFirst("(?is).*(\\s*\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+)", "$1").replaceFirst("(?is)\\s{5,}\\d{3}\\s*$", "");

					if (line.matches("(?is)\\A\\s*\\d+\\s*/\\s*\\d+\\s*/\\s*\\d+\\s+.*")) {
						String instrumentDate = line.replaceFirst("(?is)\\A\\s*(\\d+\\s*/\\s*\\d+\\s*/\\s*\\d+)\\s+.*", "$1");
						line = line.replaceFirst("(?is)\\A\\s*\\d+\\s*/\\s*\\d+\\s*/\\s*\\d+(\\s+.*)", "$1");
						if (StringUtils.isNotEmpty(instrumentDate)) {
							Date date = Util.dateParser3(instrumentDate);
							if (date != null) {
								instr.setDate(date);
								Calendar calendar = Calendar.getInstance();
								calendar.setTime(date);
								instr.setYear(calendar.get(Calendar.YEAR));
							}
						}
					}
					if (line.matches("(?is)\\s+[A-Z]{3,}\\s+.*") || line.matches("(?is)\\s+[A-Z]{3,}\\s*$")) {
						serverDocType = line.replaceFirst("(?is)\\s+([A-Z]{3,})\\s+.*", "$1").trim();
						line = line.replaceFirst("(?is)\\s+[A-Z]{3,}(\\s+.*)", "$1");
						if (StringUtils.isNotEmpty(serverDocType)) {
							doctype = DocumentTypes.getDocumentCategory(serverDocType, searchId);
							instr.setDocType(doctype.trim());
							subDocType = DocumentTypes.getDocumentSubcategory(serverDocType, searchId);
							instr.setDocSubType(subDocType);
							// info += doctype;
						}
					}
					if (line.matches("(?is)\\s*(?:[A-Z]+)?\\d{2,}\\s*$")) {
						String instrumentNumber = line.replaceFirst("(?is)\\s*((?:[A-Z]+)?\\d{2,})\\s*$", "$1");
						line = line.replaceFirst("(?is)\\s*(?:[A-Z]+)?\\d{2,}\\s*$", "");
						if (StringUtils.isNotEmpty(instrumentNumber)) {
							instr.setInstno(instrumentNumber.trim());
							// info += "###" + instrumentNumber.trim();
						}
					}
					lineCounter++;
				} else if (lineCounter == 1) {
					if (!line.startsWith("     ") && !line.trim().startsWith("Ref ") && !line.trim().startsWith("B/L")) {
						String possibleName = line.replaceFirst("(?is)(.+)\\s{4,}.*", "$1");

						if (!possibleName.trim().matches("(?is)[\\d-]+")) {
							nameUndefinedLine2 = possibleName.trim();
							nameUndefinedLine2 = nameUndefinedLine2.replaceFirst("(?is)\\+\\s*$", "").trim();
							line = line.replace(possibleName, "");
						}
					} else if (line.trim().startsWith("Ref ")) {
						refBy.append(line.trim()).append("\n");
						continue;
					} else if (line.trim().startsWith("B/L")) {
						legal.append(line.trim()).append("\n");
						continue;
					}
					if (line.matches("(?is).*\\b\\w+\\s*/\\s*\\d+\\s*$")) {
						String bookPage = line.replaceFirst("(?is).*\\b(\\w+\\s*/\\s*\\d+)\\s*$", "$1");
						line = line.replaceFirst("(?is)\\b\\w+\\s*/\\s*\\d+\\s*$", "");
						if (StringUtils.isNotEmpty(bookPage)) {
							String[] bp = bookPage.trim().split("\\s*/\\s*");
							if (bp.length == 2) {
								instr.setBook(bp[0]);
								instr.setPage(bp[1]);
							}
							// info += "###" + bookPage.trim();
						}
					}
					if (line.matches("(?is).*\\s+\\$\\s*[\\d,\\.]+\\s.*")) {
						amount = line.replaceFirst("(?is).*\\s+(\\$\\s*[\\d,\\.]+)\\s+.*", "$1");
						line = line.replaceFirst("(?is)(.*)\\s+\\$\\s*[\\d,\\.]+", "$1");
						if (StringUtils.isNotEmpty(amount)) {
							amount = amount.replaceAll("(?is)[$,]+", "");
						}
					} else if (StringUtils.isNotBlank(line.trim())) {
						others.append(line.trim()).append("\n");
					}
					lineCounter++;
				} else if (line.trim().startsWith("Ref ")) {
					refBy.append(line.trim()).append("\n");
				} else if (line.trim().startsWith("B/L")) {
					legal.append(line.trim()).append("\n");
				} else if (line.trim().contains("<hr>")) {
					foundHR = true;
				} else {
					if (foundHR) {
						if (line.matches("(?is)(.+)(\\s+\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+.*)")) {
							String name = line.replaceFirst("(?is)(.+)(\\s+\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+.*)", "$1");
							nameUndefinedLine1 += "\n" + name.replaceFirst("(?is)\\+\\s*$", "").trim();
						} else if (line.matches("(?is)(.+)\\b(\\w+\\s*/\\s*\\d+)")) {
							String name = line.replaceFirst("(?is)(.+)\\b(\\w+\\s*/\\s*\\d+)", "$1");
							nameUndefinedLine1 += "\n" + name.replaceFirst("(?is)\\+\\s*$", "").trim();
						} else {
							others.append(line.trim()).append("\n");
						}
					} else {
						others.append(line.trim()).append("\n");
					}
				}
			}

			// try {
			// File file = new File("D:\\TAD doctype.txt");
			// String fileContent = FileUtils.readFileToString(file);
			// if (!fileContent.contains(doctype + "#")) {
			// FileWriter fw = new FileWriter(file, true);
			// fw.write(info);
			// fw.write("\n");
			// fw.close();
			// }
			// } catch (IOException e) {
			// e.printStackTrace();
			// }

			RegisterDocument document = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr));
			document.setInstrument(instr);
			document.setType(SimpleChapterUtils.DType.ROLIKE);
			document.setRecordedDate(instr.getDate());
			document.setServerDocType(serverDocType);
			document.setDataSource(dataSite.getSiteTypeAbrev());
			document.setSiteId(getServerID());
			document.setSearchType(SearchType.LU);
			
			if (isParentSite()){
    			document.setSavedFrom(SavedFromType.PARENT_SITE);
			} else{
				document.setSavedFrom(SavedFromType.AUTOMATIC);
			}

			party1.append(nameUndefinedLine1);
			party2.append(nameUndefinedLine2);

			boolean isCAD_MAP_IMAGE = false;

			PartyI grantors = new com.stewart.ats.base.parties.Party(PType.GRANTOR);
			PartyI grantees = new com.stewart.ats.base.parties.Party(PType.GRANTEE);

			String names[] = null;
			String[] gtors = party1.toString().replaceAll(";", "&").split("\n");

			for (int i = 0; i < gtors.length; i++) {
				if (StringUtils.isNotBlank(gtors[i].trim())) {
					if (gtors[i].trim().toLowerCase().contains("cad map image")) {
						isCAD_MAP_IMAGE = true;
					}
					names = StringFormats.parseNameNashville(gtors[i].trim());

					grantors = addName(grantors, names);
				}
			}
			document.setGrantor(grantors);

			String[] gtee = party2.toString().replaceAll(";", "&").split("\n");
			for (int i = 0; i < gtee.length; i++) {
				if (StringUtils.isNotBlank(gtee[i].trim())) {
					if (gtee[i].trim().toLowerCase().contains("cad map image")) {
						isCAD_MAP_IMAGE = true;
					}
					names = StringFormats.parseNameNashville(gtee[i].trim());
					grantees = addName(grantees, names);
				}
			}
			document.setGrantee(grantees);

			if (StringUtils.isNotEmpty(doctype)) {
				document = setStuffToDocument(0, doctype, document);
			}
			if (legal.length() > 0) {
//				parseLegal(document, legal);
			}

			if (refBy.length() > 0) {
//				parseReferences(refBy, document, currentResponse);
			}

			currentResponse.setDocument(document);
			String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);

			String checkBox = "checked";
			if (isInstrumentSaved("gogo", document, null, false) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
				checkBox = "saved";
			} else {
				checkBox = DOCLINK_CHECKBOX_START + linkPrefix + "FK____" + document.getId() + "'>";

				LinkInPage linkInPage = new LinkInPage(linkPrefix + "FK____" + document.getId(), linkPrefix + "FK____" + document.getId(),
						TSServer.REQUEST_SAVE_TO_TSD);

				if (getSearch().getInMemoryDoc(linkPrefix + "FK____" + document.getId()) == null) {
					getSearch().addInMemoryDoc(linkPrefix + "FK____" + document.getId(), currentResponse);

					/**
					 * Save module in key in additional info. The key is
					 * instrument number that should be always available.
					 */
					String keyForSavingModules = getKeyForSavingInIntermediary(document.getInstno());
					getSearch().setAdditionalInfo(keyForSavingModules, moduleSource);
				}
				currentResponse.setPageLink(linkInPage);
			}
			setImageLinkToDocument(document, null, serverDocType, numberOfPages, isCAD_MAP_IMAGE);

			currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + documentIndex.toString() + "</td></tr>");
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + documentIndex.toString() + "</td></tr>");
			currentResponse.setUseDocumentForSearchLogRow(true);

			intermediaryResponse.add(currentResponse);

			outputTable.append(currentResponse.getResponse());
		}

		outputTable.append("</table>");

		return intermediaryResponse;
	}

	/**
	 * @param jsonObject
	 * @param document
	 * @param preParsedProperty 
	 * @throws JSONException 
	 */
	private void parseLegal(JSONObject jsonObject, RegisterDocument document, PropertyI preParsedProperty) throws JSONException {

		PropertyI prop = null;
		if(preParsedProperty == null) {
			prop = Property.createEmptyProperty();
		} 
		// let's see if we can reuse the preParsed property using primaryLegal as key	
		String mapVolPage = jsonObject.getString(LegalKeys.primaryLegal.toString());
		if(StringUtils.isNotBlank(mapVolPage)) {
			String[] vp = mapVolPage.split("/");
			if(vp.length == 2) {
				
				String platBook = vp[0].replaceAll("(?is)\\*", "").trim();
				String pagePage = vp[1].replaceAll("(?is)\\*", "").trim();
				
				if(prop == null
						&& platBook.equals(preParsedProperty.getLegal().getSubdivision().getPlatBook())
						&& pagePage.equals(preParsedProperty.getLegal().getSubdivision().getPlatPage())) {
					prop = preParsedProperty.clone();
					if(!document.getDocType().equals(DocumentTypes.PLAT)) {
						prop.getLegal().setFreeForm(null);
					}
				} else {
					
					if(prop == null) {
						prop = Property.createEmptyProperty();
					}
					
					prop.getLegal().getSubdivision().setPlatBook(platBook);
					prop.getLegal().getSubdivision().setPlatPage(pagePage);
				}
			} else {
				if(prop == null
						&& mapVolPage.equals(preParsedProperty.getLegal().getTownShip().getAbsNumber())) {
					prop = preParsedProperty.clone();
					if(!document.getDocType().equals(DocumentTypes.PLAT)) {
						prop.getLegal().setFreeForm(null);
					}
				} else {
					if(prop == null) {
						prop = Property.createEmptyProperty();
					}
					prop.getLegal().getTownShip().setAbsNumber(mapVolPage);
				}
			}
		}
		
		// ------------------- 
		
		double acreage = jsonObject.getDouble(LegalKeys.acreage.toString());
		prop.setAcres(acreage);
		
		String blockLot = jsonObject.getString(LegalKeys.secondaryLegal.toString());
		if(StringUtils.isNotBlank(blockLot)) {
			String[] bl = blockLot.split("/");
			if (bl.length == 2) {
				prop.getLegal().getSubdivision().setBlock(bl[0].replaceAll("(?is)\\*", "").trim());
				prop.getLegal().getSubdivision().setLot(bl[1].replaceAll("(?is)\\*", "").trim());
			}
		}
		
		if(jsonObject.has(LegalKeys.remark.toString())) {
			String remarks = jsonObject.getString(LegalKeys.remark.toString());
			if(StringUtils.isNotBlank(remarks)) {
				prop.getLegal().setFreeForm(remarks);
			}
		}
		
		document.addProperty(prop);
	}

	public Collection<ParsedResponse> smartParseIntermediarySubdivision(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		} else {
			objectModuleSource = getSearch().getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}
		outputTable.append("<table BORDER='1' CELLPADDING='2'>");

		table = table.replaceAll(END_SEARCH_FOOTER.toString(), "");

		LinkedHashMap<String, LinkedHashSet<String>> bulkDocuments = new LinkedHashMap<String, LinkedHashSet<String>>();
		table = table
				.replaceAll(
						"(?is)(\\*{5}------------------------------------------------------------------------\\s*((?:\\r)?\\n)\\s*\\*\\*DUPLICATE OF ENTRY.*?((?:\\r)?\\n))",
						"");

		String platInfo = "";
		Matcher mat = EXTRACT_PLAT_INFO.matcher(table);
		if (mat.find()) {
			platInfo = mat.group(1);
			platInfo = platInfo.replaceAll("(?is)\\bB/L.*?((?:\\r)?\\n)\\s*Valid.*?((?:\\r)?\\n)", "");
		}
		String[] pages = table.split(PAGE_SPLITTER.toString());
		if (pages.length > 0) {
			for (String page : pages) {
				page = page.trim();
				if (StringUtils.isEmpty(page)) {
					continue;
				}
				String[] items = page.split(ITEM_SPLITTER.toString());
				boolean foundMAP = false;
				boolean foundCMT = false;
				for (String item : items) {
					if (item.contains(" REQUESTOR: ")) {
						continue;
					}
					item = item.replaceFirst("(?is)\\A\\s*((?:\\r)?\\n)", "");
					String[] rows = item.split("(?is)((?:\\r)?\\n)");
					String key = "";
					if (rows.length > 0) {
						key = rows[0];
						key = key.replaceFirst("(?is).*(\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+)", "$1").replaceFirst("(?is)\\s{5,}\\d{3}\\s*$", "").trim();
						key = key.replaceAll("(?is)\\s+", "_");
					}

					if (StringUtils.isNotEmpty(key)) {
						if ((foundCMT || foundMAP) && (key.contains("_MAP_") || key.contains("_CMT_"))) {
							if (key.contains("_MAP_")) {
								key = key.replaceFirst("(?is)_MAP_", "_CMT_");
							} else if (key.contains("_CMT_")) {
								key = key.replaceFirst("(?is)_CMT_", "_MAP_");
							}
						}
						if (!bulkDocuments.containsKey(key)) {
							LinkedHashSet<String> allLines = new LinkedHashSet<String>();

//							if (!(foundCMT || foundMAP) && (key.contains("_MAPCMT_") || key.contains("_MAP_") || key.contains("_CMT_"))) {
//								allLines.add(platInfo);
//								allLines.add("<hr>");
//							}
							for (int i = 0; i < rows.length; i++) {
								allLines.add(rows[i]);
							}
							bulkDocuments.put(key, allLines);
						} else if (bulkDocuments.containsKey(key)) {
							LinkedHashSet<String> allLines = bulkDocuments.get(key);
							if (key.contains("_MAP_") || key.contains("_CMT_")) {
								for (int i = 0; i < rows.length; i++) {
									allLines.add(rows[i]);
								}
							} else {
								if (rows.length > 1) {
									for (int i = 1; i < rows.length; i++) {
										allLines.add(rows[i]);
									}
								}
							}
							bulkDocuments.put(key, allLines);
						}
					}
					if (key.contains("_MAP_")) {
						foundMAP = true;
					} else if (key.contains("_CMT_")) {
						foundCMT = true;
					}
				}
			}
		}
		boolean isPlatInfo = false;

		Set<Entry<String, LinkedHashSet<String>>> bdEntrySet = bulkDocuments.entrySet();
		for (Entry<String, LinkedHashSet<String>> element : bdEntrySet) {
			InstrumentI instr = new Instrument();

			ParsedResponse currentResponse = new ParsedResponse();
			LinkedHashSet<String> bulkDocument = element.getValue();
			int lineCounter = 0;

			String nameUndefinedLine1 = "", nameUndefinedLine2 = "";
			StringBuffer party1 = new StringBuffer();
			StringBuffer party2 = new StringBuffer();
			StringBuffer refBy = new StringBuffer();
			StringBuffer legal = new StringBuffer();
			StringBuffer others = new StringBuffer();

			String amount = "", serverDocType = "", doctype = "", subDocType = "";

			// String info = "";
			String numberOfPages = "";

			StringBuffer documentIndex = new StringBuffer();
			for (String line : bulkDocument) {
				documentIndex.append(line.replaceAll("(?is)(\r\n)", "$1<br>").replaceAll("(?is)\\s", "&nbsp;")).append("\r\n").append("<br>");

				if (lineCounter == 0 && !isPlatInfo) {

					mat.reset();
					mat = NAME_FROM_PLAT.matcher(platInfo);
					if (mat.find()) {
						party1.append(mat.group(1).replaceAll("(?is);", "\n")).append("\n");
						isPlatInfo = true;
						continue;
					}
				}
				if (line.contains("<hr>")) {
					continue;
				}
				if (lineCounter == 0) {
					// VILLAGE OAKS VENTU 04/04/1972 DOT 7605

					nameUndefinedLine1 = line.replaceFirst("(?is)(.+)(\\s+\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+.*)", "$1");
					nameUndefinedLine1 = nameUndefinedLine1.replaceFirst("(?is)\\+\\s*$", "").trim();

					if (nameUndefinedLine1.startsWith("OUT OF") && line.contains(" MAPCMT ")) {
						String grantee = platInfo.trim().replaceFirst("(?is)(.+)(\\s+\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+.*)", "$1");
						party2.append(grantee.trim()).append("\n");
						nameUndefinedLine1 = "";
					}

					if (line.matches("(?is).*?\\s+(\\d{3})\\s*$")) {
						numberOfPages = line.replaceFirst("(?is).*?\\s+(\\d{3})\\s*$", "$1");
					}
					line = line.replaceFirst("(?is).*(\\s*\\d{2}\\s*/\\s*\\d+\\s*/\\s*\\d+)", "$1").replaceFirst("(?is)\\s{5,}\\d{3}\\s*$", "");

					if (line.matches("(?is)\\A\\s*\\d+\\s*/\\s*\\d+\\s*/\\s*\\d+\\s+.*")) {
						String instrumentDate = line.replaceFirst("(?is)\\A\\s*(\\d+\\s*/\\s*\\d+\\s*/\\s*\\d+)\\s+.*", "$1");
						line = line.replaceFirst("(?is)\\A\\s*\\d+\\s*/\\s*\\d+\\s*/\\s*\\d+(\\s+.*)", "$1");
						if (StringUtils.isNotEmpty(instrumentDate)) {
							Date date = Util.dateParser3(instrumentDate);
							if (date != null) {
								instr.setDate(date);
								Calendar calendar = Calendar.getInstance();
								calendar.setTime(date);
								instr.setYear(calendar.get(Calendar.YEAR));
							}
						}
					}
					if (line.matches("(?is)\\s+[A-Z]{3,}\\s+.*") || line.matches("(?is)\\s+[A-Z]{3,}\\s*$")) {
						serverDocType = line.replaceFirst("(?is)\\s+([A-Z]{3,})\\s+.*", "$1").trim();
						line = line.replaceFirst("(?is)\\s+[A-Z]{3,}(\\s+.*)", "$1");
						if (StringUtils.isNotEmpty(serverDocType)) {
							doctype = DocumentTypes.getDocumentCategory(serverDocType, searchId);
							instr.setDocType(doctype.trim());
							subDocType = DocumentTypes.getDocumentSubcategory(serverDocType, searchId);
							instr.setDocSubType(subDocType);
							// info += doctype;
						}
					}
					if (line.matches("(?is)\\s*(?:[A-Z]+)?\\d{3,}\\s*$")) {
						String instrumentNumber = line.replaceFirst("(?is)\\s*((?:[A-Z]+)?\\d{3,})\\s*$", "$1");
						line = line.replaceFirst("(?is)\\s*(?:[A-Z]+)?\\d{3,}\\s*$", "");
						if (StringUtils.isNotEmpty(instrumentNumber)) {
							instr.setInstno(instrumentNumber.trim());
							// info += "###" + instrumentNumber.trim();
						}
					}
					lineCounter++;
				} else if (lineCounter == 1) {
					if (!line.startsWith("     ") && !line.trim().startsWith("Ref ") && !line.trim().startsWith("B/L")) {
						String possibleName = line.replaceFirst("(?is)(.+)\\s{4,}.*", "$1");

						if (!possibleName.trim().matches("(?is)[\\d-]+")) {
							nameUndefinedLine2 = possibleName.trim();
							nameUndefinedLine2 = nameUndefinedLine2.replaceFirst("(?is)\\+\\s*$", "").trim();
							line = line.replace(possibleName, "");
						}
					} else if (line.trim().startsWith("Ref ")) {
						refBy.append(line.trim()).append("\n");
						continue;
					} else if (line.trim().startsWith("B/L")) {
						legal.append(line.trim()).append("\n");
					}
					if (line.matches("(?is).*\\b\\w+\\s*/\\s*\\d+\\s*$")) {
						String bookPage = line.replaceFirst("(?is).*\\b([A-Z\\d]+\\s*/\\s*\\d+)\\s*$", "$1");
						line = line.replaceFirst("(?is)\\b\\w+\\s*/\\s*\\d+\\s*$", "");
						if (StringUtils.isNotEmpty(bookPage)) {
							String[] bp = bookPage.trim().split("\\s*/\\s*");
							if (bp.length == 2) {
								instr.setBook(bp[0]);
								instr.setPage(bp[1]);
							}
							// info += "###" + bookPage.trim();
						}
					}
					if (line.matches("(?is).*\\s+\\$\\s*[\\d,\\.]+\\s.*")) {
						amount = line.replaceFirst("(?is).*\\s+(\\$\\s*[\\d,\\.]+)\\s+.*", "$1");
						line = line.replaceFirst("(?is)(.*)\\s+\\$\\s*[\\d,\\.]+", "$1");
						if (StringUtils.isNotEmpty(amount)) {
							amount = amount.replaceAll("(?is)[$,]+", "");
						}
					} else if (StringUtils.isNotBlank(line.trim())) {
						others.append(line.trim()).append("\n");
					}
					lineCounter++;
				} else if (line.trim().startsWith("Ref ")) {
					refBy.append(line.trim()).append("\n");
				} else if (line.trim().startsWith("B/L")) {
					legal.append(line.trim()).append("\n");
				} else {
					others.append(line.trim()).append("\n");
				}
			}

			// try {
			// File file = new File("D:\\TAD doctype.txt");
			// String fileContent = FileUtils.readFileToString(file);
			// if (!fileContent.contains(doctype + "#")) {
			// FileWriter fw = new FileWriter(file, true);
			// fw.write(info);
			// fw.write("\n");
			// fw.close();
			// }
			// } catch (IOException e) {
			// e.printStackTrace();
			// }

			RegisterDocument document = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr));
			document.setInstrument(instr);
			document.setType(SimpleChapterUtils.DType.ROLIKE);
			document.setRecordedDate(instr.getDate());
			document.setServerDocType(serverDocType);
			document.setDataSource(dataSite.getSiteTypeAbrev());
			document.setSiteId(getServerID());
			document.setSearchType(SearchType.LS);
			
			if (isParentSite()){
    			document.setSavedFrom(SavedFromType.PARENT_SITE);
			} else{
				document.setSavedFrom(SavedFromType.AUTOMATIC);
			}

			if (StringUtils.isNotEmpty(nameUndefinedLine1)) {
				party1.append(nameUndefinedLine1);
			}
			party2.append(nameUndefinedLine2);

			boolean isCAD_MAP_IMAGE = false;

			PartyI grantors = new com.stewart.ats.base.parties.Party(PType.GRANTOR);
			PartyI grantees = new com.stewart.ats.base.parties.Party(PType.GRANTEE);

			String names[] = null;
			String[] gtors = party1.toString().replaceAll(";", "&").split("\n");

			for (int i = 0; i < gtors.length; i++) {
				if (StringUtils.isNotBlank(gtors[i].trim())) {
					if (gtors[i].trim().toLowerCase().contains("cad map image")) {
						isCAD_MAP_IMAGE = true;
					}
					names = StringFormats.parseNameNashville(gtors[i].trim());

					grantors = addName(grantors, names);
				}
			}
			document.setGrantor(grantors);

			String[] gtee = party2.toString().replaceAll(";", "&").split("\n");
			for (int i = 0; i < gtee.length; i++) {
				if (StringUtils.isNotBlank(gtee[i].trim())) {
					if (gtee[i].trim().toLowerCase().contains("cad map image")) {
						isCAD_MAP_IMAGE = true;
					}
					names = StringFormats.parseNameNashville(gtee[i].trim());
					grantees = addName(grantees, names);
				}
			}
			document.setGrantee(grantees);

			if (StringUtils.isNotEmpty(doctype)) {
				document = setStuffToDocument(0, doctype, document);
			}

			if (legal.length() > 0) {
//				parseLegal(document, legal);
			}

			 
			Set<InstrumentI> crossRefs = new HashSet<InstrumentI>();
			if (refBy.length() > 0) {
//				parseReferences(refBy, document, currentResponse);
				crossRefs = document.getParsedReferences();
			}

			if (StringUtils.isNotEmpty(platInfo) && (doctype.equals("MAPCMT") || doctype.equals("MAP") || doctype.equals("CMT"))) {
				Matcher refsMat = Pattern.compile("(?is)\\bOR:\\s*(\\d+)").matcher(platInfo);
				if (refsMat.find()) {
					Instrument instrCrossRef = new Instrument();
					instrCrossRef.setInstno(refsMat.group(1));
					crossRefs.add(instrCrossRef);
				}
				Matcher crossLineMat = Pattern.compile("(?is)\\bDR\\s+(.*?)\\r").matcher(platInfo);
				if (crossLineMat.find()) {
					String cross = crossLineMat.group(1);
					refsMat.reset();
					refsMat = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d+)\\b").matcher(cross);
					while (refsMat.find()) {
						Instrument instrCrossRef = new Instrument();
						instrCrossRef.setBook(refsMat.group(1));
						instrCrossRef.setPage(refsMat.group(2));
						crossRefs.add(instrCrossRef);
					}
				}
			}
			if (!crossRefs.isEmpty()) {
				document.setParsedReferences(crossRefs);
			}

			String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);

			String checkBox = "checked";
			if (isInstrumentSaved("gogo", document, null, false) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
				checkBox = "saved";
			} else {
				checkBox = DOCLINK_CHECKBOX_START + linkPrefix + "FK____" + document.getId() + "'>";

				LinkInPage linkInPage = new LinkInPage(linkPrefix + "FK____" + document.getId(), linkPrefix + "FK____" + document.getId(),
						TSServer.REQUEST_SAVE_TO_TSD);

				if (getSearch().getInMemoryDoc(linkPrefix + "FK____" + document.getId()) == null) {
					getSearch().addInMemoryDoc(linkPrefix + "FK____" + document.getId(), currentResponse);

					/**
					 * Save module in key in additional info. The key is
					 * instrument number that should be always available.
					 */
					String keyForSavingModules = getKeyForSavingInIntermediary(document.getInstno());
					getSearch().setAdditionalInfo(keyForSavingModules, moduleSource);
				}
				currentResponse.setPageLink(linkInPage);
			}
			setImageLinkToDocument(document, null, serverDocType, numberOfPages, isCAD_MAP_IMAGE);

			currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + documentIndex.toString() + "</td></tr>");
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + documentIndex.toString() + "</td></tr>");
			currentResponse.setUseDocumentForSearchLogRow(true);

			currentResponse.setDocument(document);
			intermediaryResponse.add(currentResponse);

			outputTable.append(currentResponse.getResponse());
		}

		outputTable.append("</table>");

		return intermediaryResponse;
	}

	/**
	 * @param document
	 * @param doctype
	 * @param numberOfPages
	 * @param numberOfPages2 
	 * @param documentIndex
	 * @param isCAD_MAP_IMAGE
	 * @param instr
	 */
	public void setImageLinkToDocument(RegisterDocument document, String atiId, String doctype, String numberOfPages, boolean isCAD_MAP_IMAGE) {
		InstrumentI instr = null;
		if (document == null || (instr = document.getInstrument()) == null) {
			return ;
		}

		String instrumentImage = instr.getInstno();
		if (StringUtils.isEmpty(instrumentImage)) {
			instrumentImage = instr.getBook() + "/" + instr.getPage();
		}
		try {
			if (StringUtils.isNotEmpty(numberOfPages)) {
				int noOfPages = Integer.parseInt(numberOfPages);
				if (noOfPages > 0 && StringUtils.isNotEmpty(doctype)) {
					doctype = doctype.trim();

					String sourceType = "r";
					if (("MAPCMT".equals(doctype) || "CMT".equals(doctype) || (doctype.equals("MAP") && !isCAD_MAP_IMAGE))
							&& (dataSite.getCountyId() == CountyConstants.TX_Travis)) {
						sourceType = ImageSourceType.S.toString();
					} else {
						if (doctype.equals("MAP") && isCAD_MAP_IMAGE) {
							sourceType = ImageSourceType.A.toString();
						} else if ("BCKPLT".equals(doctype)){
							sourceType = ImageSourceType.Y.toString();
						} else if ("HSTTBI".equals(doctype)){
							sourceType = ImageSourceType.H.toString();
						} else if ("STR".equals(doctype)){
							sourceType = "9";
						}
					}
					String internalLink = null;
					if(StringUtils.isNotBlank(atiId)) {
						internalLink = "/v1/image/integrated?county=" + COUNTY_ABBREVIATION.get(getDataSite().getCountyId()) + "&id=" + atiId + "&instrumentFake=" + instrumentImage + ".pdf";
					} else {
						internalLink = "/v1/image/manual?county=" + COUNTY_ABBREVIATION.get(getDataSite().getCountyId()) + 
								"&image_source_type=" + sourceType + "&instrument=" + instrumentImage + "&instrumentFake=" + instrumentImage + ".pdf";
					}
					getSearch().addImagesToDocument(document, internalLink);
				}
			}
		} catch (Exception e) {
			logger.error("Error while checking image for " + instrumentImage, e);
		}
	}

	protected Collection<ParsedResponse> parseIntermediary(ServerResponse response, JSONArray jsonArray, SearchType searchType, PropertyI preParsedProperty, StringBuilder outputTable) {

		
		Map<RegisterDocumentI, ParsedResponse> docToParsedRes = new LinkedHashMap<RegisterDocumentI, ParsedResponse>(); 
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
		
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		} else {
			objectModuleSource = getSearch().getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}

		outputTable.append("<table BORDER='1' CELLPADDING='2'>");

		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				JSONObject docAsJsonObject = jsonArray.getJSONObject(i);
				
				ParsedResponse currentResponse = new ParsedResponse();
				
				RegisterDocument document = getDocumentFromSearchResult(docAsJsonObject, currentResponse, preParsedProperty);
				document.setSearchType(searchType);
				
				ParsedResponse alreadyParsed = docToParsedRes.get(document);
				if(alreadyParsed != null) {
					
					DocumentI document2 = alreadyParsed.getDocument();
					if(document2 instanceof RegisterDocumentI) {
						RegisterDocumentI alreadySavedDoc = (RegisterDocumentI)document2;
						document.mergeDocumentsInformation(alreadySavedDoc, searchId, false, false);
						
						
						String documentIndex = alreadySavedDoc.asHtml();
						String imageLinkAsHtml = null;
						if(alreadySavedDoc.hasImage()) {
							imageLinkAsHtml = "<br><br><a href=\"" + 
										CreatePartialLink(TSConnectionURL.idGET) + alreadySavedDoc.getImage().getLink(0) + 
										"\" title=\"View Image\" target=\"_blank\">View Image</a>";
							
						}
						String checkBox = "checked";
						if (isInstrumentSaved("gogoADI", alreadySavedDoc, null) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
							checkBox = "saved";
						} else {
							checkBox = DOCLINK_CHECKBOX_START + linkPrefix + "FK____" + alreadySavedDoc.getId() + "'>";
						}
						alreadyParsed.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + documentIndex + (imageLinkAsHtml != null? imageLinkAsHtml:"") + "</td></tr>");
						alreadyParsed.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + documentIndex + "</td></tr>");
						
					}
					
				} else {
					docToParsedRes.put(document, currentResponse);
					
					String checkBox = "checked";
					if (isInstrumentSaved("gogoADI", document, null) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
						checkBox = "saved";
					} else {
						checkBox = DOCLINK_CHECKBOX_START + linkPrefix + "FK____" + document.getId() + "'>";
	
						LinkInPage linkInPage = new LinkInPage(
								linkPrefix + "FK____" + document.getId(), 
								linkPrefix + "FK____" + document.getId(),
								TSServer.REQUEST_SAVE_TO_TSD);
	
						if (getSearch().getInMemoryDoc(linkPrefix + "FK____" + document.getId()) == null) {
							getSearch().addInMemoryDoc(linkPrefix + "FK____" + document.getId(), currentResponse);
							/**
							 * Save module in key in additional info. The key is
							 * instrument number that should be always available.
							 */
							String keyForSavingModules = getKeyForSavingInIntermediary(document.getInstno());
							getSearch().setAdditionalInfo(keyForSavingModules, moduleSource);
						}
						currentResponse.setPageLink(linkInPage);
					}
					
					String documentIndex = document.asHtml();
					String imageLinkAsHtml = null;
					if(document.hasImage()) {
						imageLinkAsHtml = "<br><br><a href=\"" + 
									CreatePartialLink(TSConnectionURL.idGET) + document.getImage().getLink(0) + 
									"\" title=\"View Image\" target=\"_blank\">View Image</a>";
						
					}
					currentResponse.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + documentIndex + (imageLinkAsHtml != null? imageLinkAsHtml:"") + "</td></tr>");
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + documentIndex + "</td></tr>");
					currentResponse.setUseDocumentForSearchLogRow(true);
					currentResponse.setDocument(document);
		
					intermediaryResponse.add(currentResponse);
		
					outputTable.append(currentResponse.getResponse());
				
				}
				
			} catch (Exception e) {
				logger.error("Cannot parse row for searchId " + searchId , e);
			}
		}

		outputTable.append("</table>");

		return intermediaryResponse;
	}

	protected RegisterDocument getDocumentFromSearchResult(JSONObject docAsJsonObject, ParsedResponse currentResponse, PropertyI preParsedProperty) throws JSONException {
		
		String adiId = null;
		if(docAsJsonObject.has(SearchResultKeys.id.toString())) {
			adiId = docAsJsonObject.getString(SearchResultKeys.id.toString());
		}
		
		
		Instrument instrument = new Instrument();
		if(docAsJsonObject.has(SearchResultKeys.documentNumber.toString())) {
			instrument.setInstno(docAsJsonObject.getString(SearchResultKeys.documentNumber.toString()));
		}
		
		if(docAsJsonObject.has(SearchResultKeys.volumePage.toString())) {
			String volumePage = docAsJsonObject.getString(SearchResultKeys.volumePage.toString());
			if(StringUtils.isNotBlank(volumePage)) {
				String[] split = volumePage.split("\\s*/\\s*");
				if(split.length == 2) {
					instrument.setBook(split[0]);
					instrument.setPage(split[1]);
				}
			}
		}
		
		if(docAsJsonObject.has(SearchResultKeys.fileDate.toString())) {
			String fileDateAsString = docAsJsonObject.getString(SearchResultKeys.fileDate.toString());
			if(StringUtils.isNotBlank(fileDateAsString)) {
				Date date = Util.dateParser3(fileDateAsString);
				if (date != null) {
					instrument.setDate(date);
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(date);
					instrument.setYear(calendar.get(Calendar.YEAR));
				}
			}
		}
		
		String serverDocType = null;
		if(docAsJsonObject.has(SearchResultKeys.documentType.toString())) {
			serverDocType = docAsJsonObject.getString(SearchResultKeys.documentType.toString());
			if (StringUtils.isNotEmpty(serverDocType)) {
				instrument.setDocType(DocumentTypes.getDocumentCategory(serverDocType, searchId));
				instrument.setDocSubType(DocumentTypes.getDocumentSubcategory(serverDocType, searchId));
			}
		}
		
		String numberOfPages = null;
		if(docAsJsonObject.has(SearchResultKeys.imagePageCount.toString())) {
			numberOfPages = docAsJsonObject.getString(SearchResultKeys.imagePageCount.toString()).replaceFirst("^0+", "");
		}
		double amount = 0.0;
		if(docAsJsonObject.has(SearchResultKeys.amount.toString())) {
			amount = docAsJsonObject.getDouble(SearchResultKeys.amount.toString());
		}
		
		
		StringBuffer party1 = new StringBuffer();
		StringBuffer party2 = new StringBuffer();
		
		if(docAsJsonObject.has(SearchResultKeys.party1.toString())) {
			JSONArray jsonParty1Array = docAsJsonObject.getJSONArray(SearchResultKeys.party1.toString());
			for (int j = 0; j < jsonParty1Array.length(); j++) {
				JSONObject jsonPartyObject = jsonParty1Array.getJSONObject(j);
				if(jsonPartyObject.has("party")) {
					if (j>0) {
						party1.append("\n");
					}
					party1.append(jsonPartyObject.getString("party"));
				}
			}
		}
		if(docAsJsonObject.has(SearchResultKeys.party2.toString())) {
			JSONArray jsonParty2Array = docAsJsonObject.getJSONArray(SearchResultKeys.party2.toString());
			for (int j = 0; j < jsonParty2Array.length(); j++) {
				JSONObject jsonPartyObject = jsonParty2Array.getJSONObject(j);
				if(jsonPartyObject.has("party")) {
					if (j>0) {
						party2.append("\n");
					}
					party2.append(jsonPartyObject.getString("party"));
				}
			}
		}
		
		
		RegisterDocument document = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, instrument));
		document.setInstrument(instrument);
		document.setType(SimpleChapterUtils.DType.ROLIKE);
		document.setRecordedDate(instrument.getDate());
		document.setServerDocType(serverDocType);
		document.setDataSource(dataSite.getSiteTypeAbrev());
		document.setSiteId(getServerID());
		document.setSearchType(SearchType.IN);
		
		if (isParentSite()){
			document.setSavedFrom(SavedFromType.PARENT_SITE);
		} else{
			document.setSavedFrom(SavedFromType.AUTOMATIC);
		}

		PartyI grantors = new com.stewart.ats.base.parties.Party(PType.GRANTOR);
		PartyI grantees = new com.stewart.ats.base.parties.Party(PType.GRANTEE);
		
		StringBuilder remarks = new StringBuilder();
		
		if(docAsJsonObject.has(SearchResultKeys.remark.toString())) {
			remarks.append(docAsJsonObject.getString(SearchResultKeys.remark.toString()));
		}
		
		boolean isCAD_MAP_IMAGE = false;
		
		String names[] = null;
		String[] gtors = party1.toString().replaceFirst("(?is)\n\\s*$", "").replaceAll(";", "&").split("\n");

		for (int j = 0; j < gtors.length; j++) {
			String grantor = gtors[j].trim();
			if (StringUtils.isNotBlank(grantor)) {
				String lowerCaseGrantor = grantor.toLowerCase();
				
				if(lowerCaseGrantor.contains("out of ")) {
					if(remarks.length() > 0) {
						remarks.append("; ");
					}
					remarks.append(grantor);
				} else {
					if (lowerCaseGrantor.contains("cad map image")) {
						isCAD_MAP_IMAGE = true;
					} 
					names = StringFormats.parseNameNashville(grantor);
					grantors = addName(grantors, names);
				}
			}
		}
		
		document.setGrantor(grantors);
		
		String[] gtee = party2.toString().replaceFirst("(?is)\n\\s*$", "").replaceAll(";", "&").split("\n");
		for (int j = 0; j < gtee.length; j++) {
			if (StringUtils.isNotBlank(gtee[j].trim())) {
				if (gtee[j].trim().toLowerCase().contains("cad map image")) {
					isCAD_MAP_IMAGE = true;
				}

				names = StringFormats.parseNameNashville(gtee[j].trim());
				grantees = addName(grantees, names);
			}
		}
		if(document.getDocType().equals(DocumentTypes.PLAT)) {
			document.setGranteeFreeForm("County of " + getDataSite().getCountyName());
			document.setFieldModified(Fields.GRANTEE);
		} else {
			document.setGrantee(grantees);
		}
		
		if (StringUtils.isNotEmpty(instrument.getDocType())) {
			document = setStuffToDocument(amount, instrument.getDocType(), document);
		}
		
		boolean addedLegal = false;
		if(docAsJsonObject.has(SearchResultKeys.legals.toString())) {
			JSONArray jsonLegalsArray = docAsJsonObject.getJSONArray(SearchResultKeys.legals.toString());
			for (int index = 0; index < jsonLegalsArray.length(); index++) {
				parseLegal(jsonLegalsArray.getJSONObject(index), document, preParsedProperty);
				addedLegal = true;
			}
		}
		if(!addedLegal && preParsedProperty != null) {
			document.getProperties().add(preParsedProperty);
		}
		if(document.getProperties().isEmpty()) {
			document.getProperties().add(Property.createEmptyProperty());
		}
		
		if(docAsJsonObject.has(SearchResultKeys.references.toString())) {
			JSONArray jsonRefsArray = docAsJsonObject.getJSONArray(SearchResultKeys.references.toString());
			for (int index = 0; index < jsonRefsArray.length(); index++) {
				parseReferences(jsonRefsArray.getJSONObject(index), document, currentResponse);
			}
		}
		
		
		if(remarks.length() > 0) {
			document.setInfoForSearchLog(remarks.toString());
		}
				
		setImageLinkToDocument(document, adiId, serverDocType, numberOfPages, isCAD_MAP_IMAGE);
		
		return document;
	}

	/**
	 * @param amount
	 * @param doctype
	 * @param document
	 * @return
	 */
	public RegisterDocument setStuffToDocument(double price, String docCateg, RegisterDocument document) {
		PartyI grantees;
		if (docCateg.equals(DocumentTypes.TRANSFER)) {
			Transfer transfer = new Transfer(document);
			transfer.setSalePrice(price);
			transfer.setConsiderationAmount(price);
			transfer.setConsiderationAmountFreeForm(TWO_DECIMALS.getNumber(price));

			document = transfer;
		} else if (docCateg.equals(DocumentTypes.MORTGAGE)) {
			Mortgage mortgage = new Mortgage(document);

			PartyI granteesLander = new com.stewart.ats.base.parties.Party(PType.GRANTEE);
			grantees = mortgage.getGrantee();
			for (NameI name : grantees.getNames()) {
				NameMortgageGranteeI nameGrantee = new NameMortgageGrantee(name);
				nameGrantee.setTrustee(false);
				granteesLander.add(nameGrantee);
			}
			mortgage.setGrantee(granteesLander);

			mortgage.setMortgageAmount(price);

			document = mortgage;
		} else if (docCateg.equals(DocumentTypes.LIEN)) {
			Lien lien = new Lien(document);

			lien.setConsiderationAmount(price);
			lien.setConsiderationAmountFreeForm(TWO_DECIMALS.getNumber(price));

			document = lien;
		} else if (docCateg.equals(DocumentTypes.COURT)) {
			Court court = new Court(document);

			court.setConsiderationAmount(price);
			court.setConsiderationAmountFreeForm(TWO_DECIMALS.getNumber(price));
			document = court;
		} else if (docCateg.equals(DocumentTypes.CCER)) {
			document = new Ccer(document);
		} else if (docCateg.equals(DocumentTypes.CORPORATION)) {
			document = new Corporation(document);
		}
		return document;
	}

	/**
	 * @param grantors
	 * @param names
	 */
	public PartyI addName(PartyI party, String[] names) {
		String[] type = GenericFunctions.extractAllNamesType(names);
		String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
		String[] suffixes = GenericFunctions.extractNameSuffixes(names);

		NameI atsName = null;
		;

		if (StringUtils.isNotEmpty(names[2])) {
			atsName = new Name();

			if (NameUtils.isCompany(names[2])) {
				atsName.setLastName(names[2]);
				atsName.setCompany(true);
			} else {
				atsName.setFirstName(names[0]);
				atsName.setMiddleName(names[1]);
				atsName.setLastName(names[2]);

				atsName.setSufix(suffixes[0]);
				atsName.setNameType(type[0]);
				atsName.setNameOtherType(otherType[0]);

			}
			if (atsName != null) {
				party.add(atsName);
			}
		}

		if (StringUtils.isNotEmpty(names[5])) {
			atsName = new Name();

			if (NameUtils.isCompany(names[5])) {
				atsName.setLastName(names[5]);
				atsName.setCompany(true);
			} else {
				atsName.setFirstName(names[3]);
				atsName.setMiddleName(names[4]);
				atsName.setLastName(names[5]);

				atsName.setSufix(suffixes[1]);
				atsName.setNameType(type[1]);
				atsName.setNameOtherType(otherType[1]);
			}
			if (atsName != null) {
				party.add(atsName);
			}
		}
		return party;
	}

	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data) {
		if (documentToCheck == null) {
			return false;
		}
		DocumentsManagerI documentManager = getSearch().getDocManager();
		try {
			documentManager.getAccess();
			InstrumentI instToCheck = documentToCheck.getInstrument();
			for (DocumentI e : documentManager.getDocumentsWithDataSource(false, getDataSite().getSiteTypeAbrev())) {
				InstrumentI savedInst = e.getInstrument();
				if (savedInst.getInstno().equals(instToCheck.getInstno())
						&& (savedInst.getBook().equals(instToCheck.getBook()) && savedInst.getPage().equals(instToCheck.getPage()))
						&& savedInst.getDocno().equals(instToCheck.getDocno()) && e.getServerDocType().equals(documentToCheck.getServerDocType())
						&& savedInst.getYear() == instToCheck.getYear()) {
	    									
					((RegisterDocumentI) documentToCheck).mergeDocumentsInformation((RegisterDocumentI) e, searchId, true, false);
					
					return true;
				}
			}
		} finally {
			documentManager.releaseAccess();
		}
		return false;
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
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.forceValue(0, book + "/" + page);
			module.forceValue(1, COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));
			module.forceValue(8, "false");
			if(StringUtils.isNotBlank(restoreDocumentDataI.getDoctypeForSearch())) {
				module.forceValue(6, restoreDocumentDataI.getDoctypeForSearch());
			}
			
			Date recordedDate = restoreDocumentDataI.getRecordedDate();
			if (recordedDate!=null) {
				
				Calendar fileCalendar = Calendar.getInstance();
				fileCalendar.setTime(recordedDate);
				
				String day = fileCalendar.get(Calendar.DATE) < 10 ? "0" + fileCalendar.get(Calendar.DATE):fileCalendar.get(Calendar.DATE) + "";
				
				module.forceValue(2, (fileCalendar.get(Calendar.MONTH) + 1) + day);
				module.forceValue(4, (fileCalendar.get(Calendar.MONTH) + 1) + day);
				
				module.forceValue(3, fileCalendar.get(Calendar.YEAR) + "");
				module.forceValue(5, fileCalendar.get(Calendar.YEAR) + "");
				
			}
			
			GenericInstrumentFilter genericInstrumentFilter = new GenericInstrumentFilter(searchId);
			HashMap<String, String> filterCriteria = new HashMap<>();
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			genericInstrumentFilter.addDocumentCriteria(filterCriteria);
			module.addFilter(genericInstrumentFilter);
			
			list.add(module);
		}
		
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(instrNo)	&& year != SimpleChapterUtils.UNDEFINED_YEAR) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.forceValue(0, instrNo);
			module.forceValue(1, COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));
			module.forceValue(8, "false");
			
			if(StringUtils.isNotBlank(restoreDocumentDataI.getDoctypeForSearch())) {
				module.forceValue(6, restoreDocumentDataI.getDoctypeForSearch());
			}
			
			Date recordedDate = restoreDocumentDataI.getRecordedDate();
			if (recordedDate!=null) {
				
				Calendar fileCalendar = Calendar.getInstance();
				fileCalendar.setTime(recordedDate);
				
				String day = fileCalendar.get(Calendar.DATE) < 10 ? "0" + fileCalendar.get(Calendar.DATE):fileCalendar.get(Calendar.DATE) + "";
				
				module.forceValue(2, (fileCalendar.get(Calendar.MONTH) + 1) + day);
				module.forceValue(4, (fileCalendar.get(Calendar.MONTH) + 1) + day);
				
				module.forceValue(3, fileCalendar.get(Calendar.YEAR) + "");
				module.forceValue(5, fileCalendar.get(Calendar.YEAR) + "");
				
			}
			
			GenericInstrumentFilter genericInstrumentFilter = new GenericInstrumentFilter(searchId);
			HashMap<String, String> filterCriteria = new HashMap<>();
			filterCriteria.put("InstrumentNumber", module.getFunction(0).getValue());
			genericInstrumentFilter.addDocumentCriteria(filterCriteria);
			module.addFilter(genericInstrumentFilter);
			
			list.add(module);
		}
		
		return list;
		
	}

	@Override
	protected void setCertificationDate() {
		try {
			
			if (!CertificationDateManager.isCertificationDateInCache(dataSite)){
				
				HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
				HTTPRequest req = new HTTPRequest(getDataSite().getServerHomeLink() + "/v1/plant_date?county=" + COUNTY_ABBREVIATION.get(getDataSite().getCountyId()));
				HTTPResponse res = null;
				try {
					res = ((GenericConnADI) site).process(req);
					
					if(res != null && res.getReturnCode() == HttpStatus.SC_OK && res.getContentType().contains("json")) {
						JSONArray jsonArray = new JSONArray(res.getResponseAsString());
						if(jsonArray.length() > 0) {
							JSONObject jsonObject = jsonArray.getJSONObject(0);
							String postDate = jsonObject.getString("postDate");
							if(StringUtils.isNotBlank(postDate)) {
								Date date = Util.dateParser3(postDate);
								if (date != null) {
									CertificationDateManager.cacheCertificationDate(dataSite, postDate);
									getSearch().getSa().updateCertificationDateObject(dataSite, date);
								}
							}
						}
					} else {
						logger.error("Cannot setCertificationDate because unexpected result for searchId " + searchId);	
					}
					
				} catch (Exception e) {
					logger.error("Cannot setCertificationDate for searchId " + searchId, e);
				} finally {
					HttpManager3.releaseSite(site);
				}
				
			} else {
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			}

        } catch (Exception e) {
        	CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
        }
	}

	@Override
	protected String CreateSaveToTSDFormHeader(int action, String method) {
		String s = "<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"" + msRequestSolverName + "\"" + " method=\"" + method + "\" > "
				+ "<input type=\"hidden\" name=\"dispatcher\" value=\"" + action + "\">" + "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID
				+ "\">" + "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + mSearch.getSearchID() + "\"> "
				+ "<input type=\"hidden\" name=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" id=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" "
				+ "value=\"" + RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF + "\"> " + "<input type=\"hidden\" name=\""
				+ RequestParams.PARENT_SITE_SAVE_TYPE_COMBINED + "\" id=\"" + RequestParams.PARENT_SITE_SAVE_TYPE_COMBINED + "\" " + "value=\""
				+ RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF + "\">";
		return s;
	}

	@Override
	protected String CreateSaveToTSDFormEnd(String name, int parserId, int numberOfUnsavedRows) {
		if (name == null) {
			name = SAVE_DOCUMENT_BUTTON_LABEL;
		}

		String s = "";

		if (numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0) {
			s = "<input  type=\"checkbox\" title=\"Save selected document(s) with cross-references\" " + " onclick=\"javascript: if(document.getElementById('"
					+ RequestParams.PARENT_SITE_SAVE_TYPE + "'))\r\n " + " if(this.checked) { " + " document.getElementById('"
					+ RequestParams.PARENT_SITE_SAVE_TYPE + "').value='" + RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITH_CROSSREF + "' } else { "
					+ " document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "').value='"
					+ RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF + "' } \"> Save with cross-references<br>\r\n"
					+ "<input type=\"checkbox\" name=\"" + RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS + "\" id=\""
					+ RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS
					+ "\" title=\"Save search parameters from selected document(s) for further use\" > Save with search parameters<br>\r\n"
					+ "<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" " + "onclick=\"javascript:submitForm();\" >\r\n";
		}
		return s + "</form>\n";
	}

	@Override
	public String getSaveSearchParametersButton(ServerResponse response) {
		if (response == null || response.getParsedResponse() == null) {
			return null;
		}

		Object possibleModule = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);

		if (!(possibleModule instanceof TSServerInfoModule)) {
			return null;
		}

		Search search = getSearch();
		int moduleIdx = ((TSServerInfoModule) possibleModule).getModuleIdx();

		if (moduleIdx == TSServerInfo.SUBDIVISION_MODULE_IDX || moduleIdx == TSServerInfo.NAME_MODULE_IDX) {

			String key = "SSP_" + System.currentTimeMillis();

			/**
			 * Store this for future use (do not worry, it will not be saved)
			 */
			search.setAdditionalInfo(key, possibleModule);
			return "<input type=\"button\" name=\"ButtonSSP\" value=\"Save Search Parameters\" onClick=\"saveSearchedParametersAJAX('" + key + "','"
					+ getServerID() + "')\" class=\"button\" title=\"Save Last Searched Parameters\">";
		} else {
			return null;
		}
	}

	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 2) {
			String first = module.getFunction(1).getParamValue();
			String middle = module.getFunction(2).getParamValue();
			String last = module.getFunction(0).getParamValue();

			if (StringUtils.isEmpty(last)) {
				return null;
			}

			name.setLastName(last);
			name.setFirstName(first);
			name.setMiddleName(middle);
			return name;
		}

		return null;
	}

	@Override
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		LegalI legal = null;
		SubdivisionI subdivision = null;
		TownShipI townShip = null;

		if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX && module.getFunctionCount() > 4) {

			subdivision = new Subdivision();

			String legalSearchType = module.getFunction(3).getParamValue();
			if (StringUtils.isNotEmpty(legalSearchType) && "SS".equals(legalSearchType)) {

				String pbpg = module.getFunction(0).getParamValue().trim();
				if (StringUtils.isNotEmpty(pbpg)) {
					String[] pp = pbpg.split("\\s*/\\s*");
					if (pp.length > 1) {
						subdivision.setPlatBook(pp[0]);
						subdivision.setPlatPage(pp[1]);
					}
				}
				String lotBlock = module.getFunction(1).getParamValue();
				if (StringUtils.isNotEmpty(lotBlock)) {
					String[] lb = lotBlock.split("\\s*/\\s*");
					if (lb.length > 1) {
						subdivision.setBlock(lb[0]);
						subdivision.setLot(lb[1]);
					}
				}
			}

			townShip = new TownShip();
			if (StringUtils.isNotEmpty(legalSearchType) && "AA".equals(legalSearchType)) {
				townShip.setAbsNumber(module.getFunction(0).getParamValue().trim());
			}

		}
		if (subdivision != null) {
			legal = new Legal();
			legal.setSubdivision(subdivision);
		}

		if (townShip != null) {
			if (legal == null) {
				legal = new Legal();
			}
			legal.setTownShip(townShip);
		}

		return legal;
	}

	public String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&").append(getLinkPrefix(iActionType));
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}

	@Override
	protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {

		if (image != null) {
			String link = image.getLink(0);

			if (StringUtils.isEmpty(link)) {
				return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
			}

			HttpSite3 site = null;
			link = link.replaceFirst("(?is)\\.pdf", "");
			try {
				site = HttpManager3.getSite(getCurrentServerName(), searchId);
				HTTPRequest req = new HTTPRequest("https://api.austindata.com/" + link);
				HTTPResponse res = null;
				try {
					res = site.process(req);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager3.releaseSite(site);
				}
				if (res == null) {
					return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
				}
				if (res.is == null) {
					return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
				}

				byte[] imageBytes = res.getResponseAsByte();
				if (imageBytes != null && imageBytes.length > 0 && "application/pdf".equals(res.getContentType())) {
					image.setExtension("pdf");
					image.setContentType("application/pdf");
					DownloadImageResult downloadImageResult = new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType());

					afterDownloadImage(true);

					return downloadImageResult;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
	}
	
	@Override
	public String getPrettyFollowedLink(String initialFollowedLnk) {
		
		String instrNo = RegExUtils.getFirstMatch("(?is)(?:&|\\?)instrument=(.*?)(?:$|&)", initialFollowedLnk, 1);
		
		if(StringUtils.isBlank(instrNo)) {
			String book = RegExUtils.getFirstMatch("(?is)(?:&|\\?)book=(.*?)(?:$|&)", initialFollowedLnk, 1);
			String page = RegExUtils.getFirstMatch("(?is)(?:&|\\?)page=(.*?)(?:$|&)", initialFollowedLnk, 1);	
			if(StringUtils.isNotBlank(book) && StringUtils.isNotBlank(page)) {
				return "<br/><span class='followed'>Book-Page " + book + "-" + page + " has already been saved.</span><br/>";	
			}
		} else {
			return "<br/><span class='followed'>Instrument " + instrNo + " has already been saved.</span><br/>";
		}
		
		return "<br/><span class='followed'>" + preProcessLink(initialFollowedLnk) + "</span><br/>";
	}
	
	protected void downloadParentSiteData() {
		
		String folderPath = ServerConfig.getModuleDescriptionFolder(ServerConfig.getRealPath() + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if(!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		File f = new File(folderPath + File.separator + GenericServerADI.class.getSimpleName() + ".xml");
		
		
		HTTPRequest req = new HTTPRequest(getDataSite().getServerHomeLink() + "v1/document_type");
		HTTPResponse res = null;
		HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
		try {
			res = site.process(req);
		
		
			StringBuilder doctypes = new StringBuilder();
			if(res != null && res.getReturnCode() == HttpStatus.SC_OK && res.getContentType().contains("json")) {
				try {
					JSONArray jsonArray = new JSONArray(res.getResponseAsString());
					doctypes.append("<select name=\"document_type\">\n");
					doctypes.append("<option value=\"\">Any</option>\n");
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject jsonObject = jsonArray.getJSONObject(i);
						doctypes.append("<option value=\"").append(jsonObject.getString("documentType")).append("\">").append(jsonObject.getString("description")).append("</option>\n");
					}
					doctypes.append("</select>\n");
				} catch (JSONException e) {
					logger.error("JSONException on parsing source_type", e);
				}
			}
			
			
			StringBuilder allParentSiteData = new StringBuilder();
			
			for (Integer countyId : COUNTY_ABBREVIATION.keySet()) {
				
				allParentSiteData.append("<county id=\"").append(COUNTY_ABBREVIATION.get(countyId)).append("\">\n");
				
				req = new HTTPRequest(getDataSite().getServerHomeLink() + "v1/source_type?county=" + COUNTY_ABBREVIATION.get(countyId));
				res = null;
				
				res = site.process(req);
				
				if(res != null && res.getReturnCode() == HttpStatus.SC_OK && res.getContentType().contains("json")) {
					try {
						JSONArray jsonArray = new JSONArray(res.getResponseAsString());
						allParentSiteData.append("<select name=\"source_type\">\n");
						allParentSiteData.append("<option value=\"\">Any</option>\n");
						for (int i = 0; i < jsonArray.length(); i++) {
							JSONObject jsonObject = jsonArray.getJSONObject(i);
							allParentSiteData.append("<option value=\"").append(jsonObject.getString("source")).append("\">").append(jsonObject.getString("description")).append("</option>\n");
						}
						allParentSiteData.append("</select>\n");
						
						allParentSiteData.append(doctypes);
						
					} catch (JSONException e) {
						logger.error("JSONException on parsing source_type", e);
					}
				}
				
				allParentSiteData.append("</county>\n");
			}
			FileUtils.writeStringToFile(f, allParentSiteData.toString());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager3.releaseSite(site);
		}
		
	}

	public static void loadParentSiteData() {
		String folderPath = ServerConfig.getModuleDescriptionFolder(ServerConfig.getRealPath() + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if(!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			String xml = FileUtils.readFileToString(new File(folderPath + File.separator + GenericServerADI.class.getSimpleName() + ".xml"));
			Pattern countyPattern = Pattern.compile("(?ism)<county id=\"(.*?)\">(.*?)</county>");
			Matcher countyM = countyPattern.matcher(xml);
			while(countyM.find()) {
				String countyName = countyM.group(1);
				String controls = countyM.group(2);
				
				String documentTypeSelect = ro.cst.tsearch.utils.StringUtils.extractParameter(controls, "(?ism)(<select name=\"document_type\".*?</select>)");
				String sourceTypeSelect = ro.cst.tsearch.utils.StringUtils.extractParameter(controls, "(?ism)(<select name=\"source_type\".*?</select>)");
				
				Map<CountySpecificInfo,String> info = new HashMap<CountySpecificInfo, String>();
				info.put(CountySpecificInfo.DOCUMENT_TYPE_SELECT, documentTypeSelect);
	    		info.put(CountySpecificInfo.SOURCE_TYPE_SELECT,sourceTypeSelect);
	    		
	    		parentSiteInfo.put(countyName, info);
			}
		} catch (Exception e) {
			e.printStackTrace();	
		}
	}
	
	protected void initFields() {
		
//		downloadParentSiteData();
		
		try {
			String countyName = COUNTY_ABBREVIATION.get(getDataSite().getCountyId());
			
			if(parentSiteInfo.containsKey(countyName)) {
				documentTypeSelect = parentSiteInfo.get(countyName).get(CountySpecificInfo.DOCUMENT_TYPE_SELECT);
				sourceTypeSelect = parentSiteInfo.get(countyName).get(CountySpecificInfo.SOURCE_TYPE_SELECT);
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			byte[] contentAsBytes = FileUtils.readFileToByteArray(new File("D://property.pdf"));
			InputStream is = new ByteArrayInputStream(contentAsBytes);

			String file = PDFUtils.extractTextFromPDF(is);
			System.out.println(file);
			is.close();

			// String pdfAsString =
			// PDFUtils.extractTextFromPDF(rw.getBinaryResponse());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

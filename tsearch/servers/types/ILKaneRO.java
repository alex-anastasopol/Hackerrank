package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DateFormatUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.htmlparser.Node;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.w3c.dom.Document;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.misc.CrossReferenceToInvalidatedFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.SubdivisionFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.ConfigurableSkipModuleNameIteratorIfNoLegalFound;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MatchEquivalents;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.SubdivisionMatcher;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class ILKaneRO extends TSServer{

	static final long		serialVersionUID		= 10000000;
	private boolean			downloadingForSave;
	private static String[]	INSTRUMENT_TYPE			= { "DCCO", "DCLN", "EASE" };
	private static String[]	INSTRUMENT_TYPE_PLAT	= { "PLAT", "SRVY", "SMAP" };
	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param mid
	 */
	public ILKaneRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public ILKaneRO(long searchId){
		super(searchId);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		return msiServerInfoDefault;
	}
	
	public static void splitResultRows(
			Parser p,
			ParsedResponse pr,
			String htmlString,
			int pageId,
			String linkStart,
			int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException
			{
			
			p.splitResultRows(
				pr,
				htmlString,
				pageId,
				"<tr style",
				"</tr>",
				linkStart,
				action);
		}
	
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		ConfigurableNameIterator nameIterator = null;
		String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

			String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
			if (date != null) {
				module.getFunction(39).forceValue(date);
			}
			module.setValue(40, endDate);

			module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));

			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L;F;"});
			module.addIterator(nameIterator);

			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				if (date != null) {
					module.getFunction(39).forceValue(date);
				}
				module.setValue(40, endDate);

				module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));

				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
				module.addIterator(nameIterator);

				modules.add(module);

			}
		}

		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}
	
	private void addIteratorModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId){
		//FilterResponse unitAddressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.9d, true);
		AddressFilterKane addressFilter = new AddressFilterKane( new BigDecimal(0.9d ),searchId);
		FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK);
		module.clearSaKeys();
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		addressFilter.setEnableUnit(true);
		addressFilter.setEnableMissingUnit(true);
		module.addValidator(addressFilter.getValidator());
		module.addValidator(defaultLegalFilter.getValidator());
		module.addIterator(it);
		modules.add(module);
	}
	
	private void addIteratorModuleSTR(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId){

        
        FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
        AddressFilterKane addressFilter = new AddressFilterKane(new BigDecimal(0.9d), searchId);
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE);
		module.clearSaKeys();
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		addressFilter.setEnableUnit(true);
		addressFilter.setEnableMissingUnit(true);
		module.addValidator(addressFilter.getValidator());
		module.addValidator(defaultLegalFilter.getValidator());
		module.addIterator(it);
		modules.add(module);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes","deprecation" })
	protected List extractInstrumentNoList(List original) {
		List instr = new ArrayList();

		for (int i = 0; i < original.size(); i++)
		{
			Instrument instrCrt = (Instrument) original.get(i);
			if (!StringUtils.isStringBlank(instrCrt.getInstrumentNo()))
			{
				instr.add(instrCrt);
			}
		}

		return instr;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		Search search = getSearch();
		int searchType = search.getSearchType();

		if (searchType == Search.AUTOMATIC_SEARCH) {

			TSServerInfoModule m = null;

			FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
			FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
			FilterResponse crossReferenceToInvalidated = new CrossReferenceToInvalidatedFilter(searchId);

			SubdivisionFilter subdivisionNameFilter = new SubdivisionFilter(searchId);
			subdivisionNameFilter.setThreshold(new BigDecimal(0.90d));
			subdivisionNameFilter.setLoadFromAdditionalInfo(true);

			DocsValidator subdivisionNameValidator = subdivisionNameFilter.getValidator();
			FilterResponse lotFilter = LegalFilterFactory.getDefaultLotFilter(searchId);
			FilterResponse blockFilter = LegalFilterFactory.getDefaultBlockFilter(searchId);
			AddressFilterKane addressFilter = new AddressFilterKane(new BigDecimal(0.8d), searchId);
			addressFilter.setEnableUnit(true);

			String stNo = getSearchAttribute(SearchAttributes.P_STREETNO);
			String stDir = getSearchAttribute(SearchAttributes.P_STREETDIRECTION);
			String stName = getSearchAttribute(SearchAttributes.P_STREETNAME);
			String address = (StringUtils.isNotEmpty(stNo) ? stNo + " " : "")
					+ (StringUtils.isNotEmpty(stDir) ? stDir + " " : "")
					+ (StringUtils.isNotEmpty(stName) ? stName : "");

			// P1 instrument list search from AO (ISI or NDB) for finding Legal for P4 or P5
			InstrumentIterator instrumentIterator = new InstrumentIterator(searchId);
			instrumentIterator.enableInstrumentNumber();

			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_LIST_AO_INSTR);
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.INSTR_LIST);
			m.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.addFilter(new GenericInstrumentFilter(searchId));
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			m.addIterator(instrumentIterator);
			l.add(m);

			// P2 if no document numbers found on AO ===> Address Search for finding Legal for P4 or P5
			if (instrumentIterator.createDerrivations().isEmpty() && hasStreet()) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_ADDRESS_NO_DIR_NAME);
				m.setSaObjKey(SearchAttributes.P_STREET_NO_NAME);
				m.clearSaKeys();
				m.setSaKey(39, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				m.setSaKey(40, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				m.addValidator(addressFilter.getValidator());
				m.forceValue(19, address.trim());
				l.add(m);
			}

			// P3 if no document numbers found on AO and no Docs found when searching with
			// Address Search on RO ===> Search by Owner for finding Legal for P4 or P5
			if (instrumentIterator.createDerrivations().isEmpty() && hasOwner()) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_ADVANCED_SEARCH_OWNERS_FOR_LEGAL);
				m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				m.clearSaKeys();
				FilterResponse defaultNameFilter = NameFilterFactory.getPerfectlyMatchNameFilter(
						SearchAttributes.OWNER_OBJECT, searchId, m);
				defaultNameFilter.setThreshold(new BigDecimal(1d));
				((GenericNameFilter) defaultNameFilter).setInitAgain(true);
				String[] docTypes = { "TRANSFER", "MORTGAGE" };
				FilterResponse doctTypeFilter = DoctypeFilterFactory.getDoctypeFilter(searchId, 0.8d, docTypes, FilterResponse.STRATEGY_TYPE_HIGH_PASS);
				m.addFilter(rejectSavedDocuments);
				m.addFilter(doctTypeFilter);
				m.addValidator(defaultNameFilter.getValidator());
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				m.setData(2, "Or");
				m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				ConfigurableSkipModuleNameIteratorIfNoLegalFound nameIterator = (ConfigurableSkipModuleNameIteratorIfNoLegalFound) ModuleStatesIteratorFactory
						.getConfigurableSkipModuleNameIteratorifNoLegalFound(searchId, new String[] { "L;F;" }, "RO");
				nameIterator.setAllowMcnPersons(false);
				m.addIterator(nameIterator);
				l.add(m);
			}

			// P4 search with subdivision/block/lot from RO documents
			addIteratorModule(serverInfo, l, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId);

			// P5 search with sec/twp/rng from RO documents
			addIteratorModuleSTR(serverInfo, l, TSServerInfo.MODULE_IDX38, searchId);

			ConfigurableNameIterator nameIterator = null;
			FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(
					SearchAttributes.OWNER_OBJECT, searchId, m);

			// P6 name modules with names from search page.
			if (hasOwner()) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_ADVANCED_SEARCH_OWNERS);
				m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				m.clearSaKeys();
				m.setSaKey(39, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				m.setSaKey(40, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

				((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) defaultNameFilter).setUseArrangements(false);
				((GenericNameFilter) defaultNameFilter).setInitAgain(true);
				m.addFilter(rejectSavedDocuments);
				m.addFilter(defaultNameFilter);
				m.addFilter(new LastTransferDateFilter(searchId));
				m.addFilter(crossReferenceToInvalidated);
				addFilterForUpdate(m, true);
				m.addValidator(defaultLegalFilter.getValidator());
				m.addValidator(subdivisionNameValidator);
				m.addValidator(defaultNameFilter.getValidator());
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				m.setData(2, "Or");
				m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				m.setIteratorType(39, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(m, searchId, new String[] { "L;F;" });
				m.addIterator(nameIterator);
				l.add(m);
			}

			// P7 search by buyers
			if (hasBuyer() && !InstanceManager.getManager().getCurrentInstance(searchId)
					.getCrtSearchContext().isProductType(SearchAttributes.SEARCH_PROD_REFINANCE)) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_ADVANCED_SEARCH_BUYERS);
				m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
				m.clearSaKeys();
				m.setSaKey(39, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				m.setSaKey(40, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

				FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, searchId, m);
				((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) nameFilter).setUseArrangements(false);
				((GenericNameFilter) nameFilter).setInitAgain(true);
				// nameFilter.setSkipUnique(false);
				FilterResponse doctTypeFilter = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
				String[] docTypes = { "LIEN", "COURT" };
				((DocTypeSimpleFilter) doctTypeFilter).setDocTypes(docTypes);
				m.addFilter(rejectSavedDocuments);
				m.addFilter(doctTypeFilter);
				m.addFilter(nameFilter);
				addFilterForUpdate(m, true);
				m.addFilter(crossReferenceToInvalidated);
				m.addValidator(nameFilter.getValidator());
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				m.setData(2, "Or");
				m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				m.setIteratorType(39, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
				ConfigurableNameIterator buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(m, searchId, new String[] { "L;F;" });
				buyerNameIterator.setAllowMcnPersons(false);
				m.addIterator(buyerNameIterator);
				l.add(m);
			}

			// P8 subdivision search for plats
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_ADVANCED_SEARCH_SUBDIVISION_PL_EAS_RES_MASDEED);
			m.setSaObjKey(SearchAttributes.LD_SUBDIV_NAME);
			m.clearSaKeys();
			PlatIteratorD itd = new PlatIteratorD(searchId);
			m.setSaKey(39, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(40, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.addFilter(lotFilter);
			m.addFilter(blockFilter);
			m.addIterator(itd);
			l.add(m);

			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_ADVANCED_SEARCH_SUBDIVISION_PL_EAS_RES_MASDEED);
			m.setSaObjKey(SearchAttributes.LD_SUBDIV_NAME);
			m.clearSaKeys();
			PlatIterator it = new PlatIterator(searchId);
			m.setSaKey(39, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(40, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.addIterator(it);
			l.add(m);

			// subdivision search for plats with plat doc from subdivision codes link
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_PLAT_DOC);
			m.setSaObjKey(SearchAttributes.LD_SUBDIV_NAME);
			m.clearSaKeys();
			PlatIteratorForDocs ite = new PlatIteratorForDocs(searchId);
			m.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.addIterator(ite);
			l.add(m);

			// P9 search by crossRef list from RO documents
			InstrumentGenericIterator crossRefIterator = new InstrumentGenericIterator(searchId);
			crossRefIterator.enableInstrumentNumber();
			crossRefIterator.setLoadFromRoLike(true);

			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
			m.clearSaKeys();
			m.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			m.addFilter(rejectSavedDocuments);
			m.addFilter(new GenericInstrumentFilter(searchId));
			m.addIterator(crossRefIterator);
			l.add(m);

			// P10 OCR last transfer - instrument search
			// if you modify the iterator, check getNumberOfDocsAllowedForThisModule in TSServer
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			m.clearSaKeys();
			m.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
			m.addFilter(new GenericInstrumentFilter(searchId));
			m.addCrossRefValidator(defaultLegalFilter.getValidator());
			l.add(m);

			// P11 name module with names added by OCR
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_ADVANCED_SEARCH_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.clearSaKeys();

			((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
			m.addFilter(rejectSavedDocuments);
			m.addFilter(defaultNameFilter);
			m.addFilter(new LastTransferDateFilter(searchId));
			m.addFilter(crossReferenceToInvalidated);
			m.addValidator(defaultLegalFilter.getValidator());
			m.addValidator(subdivisionNameValidator);
			m.setSaKey(39, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(40, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			m.setData(2, "Or");
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			m.setIteratorType(39, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			ArrayList<NameI> searchedNames = null;
			if (nameIterator != null) {
				searchedNames = nameIterator.getSearchedNames();
			} else {
				searchedNames = new ArrayList<NameI>();
			}
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L;F;" });
			// get your values at runtime
			nameIterator.setInitAgain(true);
			nameIterator.setSearchedNames(searchedNames);
			m.addIterator(nameIterator);
			l.add(m);

			// UCC search
			// P12 name modules with names from search page.
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.TAX_BILL_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_ADVANCED_SEARCH_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.clearSaKeys();
			FilterResponse defaultNameFilterP = NameFilterFactory.getDefaultNameFilter(
					SearchAttributes.OWNER_OBJECT, searchId, m);
			((GenericNameFilter) defaultNameFilterP).setIgnoreMiddleOnEmpty(true);

			m.addFilter(rejectSavedDocuments);
			m.addFilter(defaultNameFilterP);
			m.addValidator(defaultNameFilterP.getValidator());
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIteratorU = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, true, searchId,
					new String[] { "L;F;" });
			nameIteratorU.setInitAgain(true);
			m.addIterator(nameIteratorU);
			l.add(m);

			// UCC buyer
			if (hasBuyer() && !InstanceManager.getManager().getCurrentInstance(searchId)
					.getCrtSearchContext().isProductType(SearchAttributes.SEARCH_PROD_REFINANCE)) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.TAX_BILL_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_ADVANCED_SEARCH_BUYERS);
				m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
				m.clearSaKeys();

				FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.BUYER_OBJECT, searchId, m);
				((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
				m.addFilter(rejectSavedDocuments);
				m.addFilter(nameFilter);
				m.addValidator(nameFilter.getValidator());
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				ConfigurableNameIterator buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(m, searchId, new String[] { "L;F;" });
				buyerNameIterator.setAllowMcnPersons(false);
				m.addIterator(buyerNameIterator);
				l.add(m);
			}

			String[] relatedSourceDoctype = new String[] {
					DocumentTypes.MORTGAGE,
					DocumentTypes.LIEN,
					DocumentTypes.CCER
			};

			// P13 Related Search - Search By Remarks with Instrument Number list from all [MORTGAGE, LIEN, CCER]
			InstrumentGenericIterator relatedInstrumentIterator = new InstrumentGenericIterator(searchId);
			relatedInstrumentIterator.enableInstrumentNumber();
			relatedInstrumentIterator.setLoadFromRoLike(true);
			relatedInstrumentIterator.setRoDoctypesToLoad(relatedSourceDoctype);
			relatedInstrumentIterator.setDoNotCheckIfItExists(true);
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX40));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					"Searching (related) with Instrument Number list from all " + Arrays.toString(relatedSourceDoctype));
			m.clearSaKeys();
			m.setSaKey(4, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			m.addIterator(relatedInstrumentIterator);
			m.addFilter(rejectSavedDocuments);
			m.addValidator(defaultLegalFilter.getValidator());
			l.add(m);
		}
		serverInfo.setModulesForAutoSearch(l);
	}

	static protected class LegalDescriptionIterator extends GenericRuntimeIterator<PersonalDataStruct> {
		
		private static final long serialVersionUID = 47656891817117069L;
		
		private Set<String> allSubdivisionNames = null;
		private String subdivisionSetKey = AdditionalInfoKeys.SUBDIVISION_NAME_SET;
		
		LegalDescriptionIterator(long searchId) {
			super(searchId);
			allSubdivisionNames = new HashSet<String>();
		}
		
		
		public List<DocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch){
			final List<DocumentI> ret = new ArrayList<DocumentI>();
			
			List<RegisterDocumentI> listRodocs = m.getRealRoLikeDocumentList();
			DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
			
			SearchAttributes sa	= search.getSa();
			PartyI owner 		= sa.getOwners();
			
			for(RegisterDocumentI doc:listRodocs){
				boolean found = false;
				
				for(PropertyI prop:doc.getProperties()) {
					if ( ((doc.isOneOf("MORTGAGE","RELEASE") || this.isTransferAllowed(doc)) && applyNameMatch) 
							|| ((doc.isOneOf("MORTGAGE") || this.isTransferAllowed(doc)) && !applyNameMatch) ) {
						
						if(prop.hasSubdividedLegal()) {
							String subdivName = prop.getLegal().getSubdivision().getName();
							boolean nameMatched = false;
							
							if(applyNameMatch){
								if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
										GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
										GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
										GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
										nameMatched = true;
								}
							}
							if( (nameMatched || !applyNameMatch) && (StringUtils.isNotEmpty(subdivName))) {
								ret.add(doc);
								found = true;
								break;
							}
						}
						
						if(prop.hasTownshipLegal()) {
							TownShipI sub = prop.getLegal().getTownShip();
							String section = sub.getSection();
							boolean nameMatched = false;
							
							if(applyNameMatch) {
								if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
										GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
										GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
										GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
										nameMatched = true;
								}
							}
							if( (nameMatched || !applyNameMatch) && !StringUtils.isEmpty(section)) {
								ret.add(doc);
								found = true;
								break;
							}
						}
					}
				}
				if(found) {
					break;
				}
			}
			return ret;
		}
		
		public boolean isTransferAllowed(RegisterDocumentI doc) {
			return doc != null && doc.isOneOf(DocumentTypes.TRANSFER);
		}
		
		public boolean isTransferAllowed(DocumentI doc) {
			if(doc instanceof TransferI) {
				return isTransferAllowed((TransferI)doc);
			}
			return false;
		}
		
		protected List<DocumentI> loadLegalFromRoDocs(Search global, DocumentsManagerI m) {
			List<DocumentI> listRodocs = new ArrayList<DocumentI>();
			
			listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(m,global,true));
			if(listRodocs.isEmpty()){
				listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(m, global, false));
			}
			if(listRodocs.isEmpty()){
				listRodocs.addAll(m.getRealRoLikeDocumentList(true));
			} 
			
			return listRodocs;
		}
		
		
		@SuppressWarnings("unchecked")
		protected  List<PersonalDataStruct> createDerivationInternal(long searchId){
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI m = global.getDocManager();
			List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)global.getAdditionalInfo("IL_KANE_RO_LOOK_UP_DATA");
			
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
			boolean hasLegal = false;
			if(legalStructList==null){
				legalStructList = new ArrayList<PersonalDataStruct>();
				Set<AddressI> addresses = new HashSet<AddressI>();
				try{
					
					m.getAccess();
					//List<RegisterDocumentI> listRodocs = m.getRoLikeDocumentList(true);
					List<DocumentI> listRodocs = loadLegalFromRoDocs(global, m);
					DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_ASC);
					for( DocumentI reg: listRodocs) {
						if(!reg.isOneOf(DocumentTypes.PLAT,	DocumentTypes.RESTRICTION, DocumentTypes.EASEMENT, DocumentTypes.MASTERDEED, DocumentTypes.COURT,
								DocumentTypes.LIEN, DocumentTypes.CORPORATION, DocumentTypes.AFFIDAVIT, DocumentTypes.CCER, DocumentTypes.TRANSFER)
							|| isTransferAllowed(reg)) 
						{
							for (PropertyI prop: reg.getProperties()){
								if(prop.hasLegal()){
									LegalI legal = prop.getLegal();
										
									if(legal.hasSubdividedLegal()){
										hasLegal = true;
										PersonalDataStruct legalStructItem = new PersonalDataStruct();
										SubdivisionI subdiv = legal.getSubdivision();
										
										String subCode = subdiv.getName();
										String block = subdiv.getBlock();
										String lot = subdiv.getLot();
										//lot = StringFormats.ReplaceIntervalWithEnumeration(lot);//B4568
										String[]lots = lot.split("\\s{1,}");
										if (StringUtils.isNotEmpty(subCode)){
											if (lots.length > 1) {
												for (int i = 0; i < lots.length; i++){
													legalStructItem = new PersonalDataStruct();
													legalStructItem.subCode = subCode;
													legalStructItem.block = StringUtils.isEmpty(block) ? "NONE" : block;
													legalStructItem.lot = lots[i];
													if( !testIfExist(legalStructList, legalStructItem, "subdivision") ){
														legalStructList.add(legalStructItem);
													}
												}
											}else {
												legalStructItem.subCode = StringUtils.isEmpty(subCode) ? "" : subCode;
												legalStructItem.block = StringUtils.isEmpty(block) ? "NONE" : block;
												legalStructItem.lot = lot;
												if( !testIfExist(legalStructList, legalStructItem, "subdivision") ){
													legalStructList.add(legalStructItem);
												}
											}
										}
									}
									if (legal.hasTownshipLegal()){
										PersonalDataStruct legalStructItem = new PersonalDataStruct();
										TownShipI township = legal.getTownShip();
										
										String sec = township.getSection();
										String tw = township.getTownship();
										String rg = township.getRange();
										String qValue = township.getQuarterValue();
										if (StringUtils.isNotEmpty(sec)){
											legalStructItem.section = StringUtils.isEmpty(sec) ? "" : sec;
											legalStructItem.township = StringUtils.isEmpty(tw) ? "" : tw;
											legalStructItem.range = StringUtils.isEmpty(rg) ? "" : rg;
											legalStructItem.qValue = StringUtils.isEmpty(qValue) ? "" : qValue;
											
											if( !testIfExist(legalStructList, legalStructItem, "sectional") ){
												legalStructList.add(legalStructItem);
											}
										}
									}
								}
								if(prop.hasAddress()){
									addresses.add(prop.getAddress());
								}
							}
						}
					}
					global.setAdditionalInfo("IL_KANE_RO_LOOK_UP_ADDRESS_DATA", addresses);
					global.setAdditionalInfo("IL_KANE_RO_LOOK_UP_DATA", legalStructList);
					if(legalStructList.size() > 0){
							boostrapSubdivisionData(legalStructList, global, true);
							boostrapSectionalData(legalStructList, global);
					}
				}
				finally{
					m.releaseAccess();
				}
			} else {
				for(PersonalDataStruct struct:legalStructList){
					if (StringUtils.isNotEmpty(struct.subCode)){
						hasLegal = true;
					}
				}
				if (hasLegal){
					legalStructList = new ArrayList<PersonalDataStruct>();
				}
			}
			return legalStructList;
		}
		
		protected List<PersonalDataStruct> createDerrivations(){
			
			List<PersonalDataStruct> dataStructList = createDerivationInternal(searchId);
			for (PersonalDataStruct dataStruct : dataStructList){
				if (org.apache.commons.lang.StringUtils.isNotEmpty(dataStruct.subCode)){
					saveSubdivisionName(dataStruct.subCode);
				}
			}
			
			if (allSubdivisionNames != null && allSubdivisionNames.size() > 0) {
				getSearch().setAdditionalInfo(subdivisionSetKey, allSubdivisionNames);
			}
			return dataStructList;
		}
		
		protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
			Search search = getSearch();
			SearchAttributes sa = search.getSa();
			
	        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	        String startDate = "01/01/1960", endDate = "";
	        try{
	            
	            Date start = sdf.parse(sa.getAtribute(SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY));
	            Date end = sdf.parse(sa.getAtribute(SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY));
	            
	            sdf.applyPattern("MM/dd/yyyy");
	            startDate = sdf.format(start);
	            endDate = sdf.format(end);
	            
	        }catch (ParseException e){}
	        
			switch(module.getModuleIdx()){
				case TSServerInfo.SUBDIVISION_MODULE_IDX:
					module.setData(0, str.subCode);
					module.setData(1, str.block);
					module.setData(2, str.lot);
					module.setData(7, startDate);
					module.setData(8, endDate);
					if (StringUtils.isEmpty(str.subCode)){
						module.setSkipModule(true);
					}
				break;
				case TSServerInfo.MODULE_IDX38:// search by subdivision/township/range
						module.setData(0, str.section);
						module.setData(1, str.township);
						module.setData(2, str.range);
						module.setData(10, startDate);
						module.setData(11, endDate);
						String[] qValues = str.qValue.split(",");
						for (String qValue : qValues){
							if (qValue.trim().matches("NE")){
								module.setData(3, "true");
							}
							if (qValue.trim().matches("NW")){
								module.setData(4, "true");
							}
							if (qValue.trim().matches("SE")){
								module.setData(5, "true");
							}
							if (qValue.trim().matches("SW")){
								module.setData(6, "true");
							}
						}
				break;
			}
		}
		
		public void saveSubdivisionName(String subdivisionCode) {
			if (subdivisionCode != null){
				allSubdivisionNames.add(subdivisionCode.toUpperCase().trim());
			}
		}
	}
	
	protected static class PersonalDataStruct implements Cloneable{
		String block = "";
		String section="";
		String township="";
		String range="";
		String lot	=	"";
		String instrType = "";
		String subCode = "";
		String subName = "";
		String platDoc = "";
		String qValue = "";
		
		@Override
		protected Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
		
		public boolean equalsSubdivision(PersonalDataStruct struct) {
			return this.block.equals(struct.block) && this.lot.equals(struct.lot) && this.subCode.equals(struct.subCode);
		}
		
		public boolean equalsSectional(PersonalDataStruct struct) {
			return this.section.equals(struct.section) && this.township.equals(struct.township) && this.range.equals(struct.range) && this.qValue.equals(struct.qValue);
		}
		
		public boolean equalsPlat(PersonalDataStruct struct) {
			return this.subCode.equals(struct.subCode) && this.instrType.equals(struct.instrType);
		}
		public boolean equalsPlatDoc(PersonalDataStruct struct) {
			return this.subCode.equals(struct.subCode) && this.platDoc.equals(struct.platDoc);
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
		} else if("plat".equalsIgnoreCase(string)){
			for(PersonalDataStruct p:legalStruct2){
				if(l.equalsPlat(p)){
					return true;
				}
			}
		} else if("platDoc".equalsIgnoreCase(string)){
			for(PersonalDataStruct p:legalStruct2){
				if(l.equalsPlatDoc(p)){
					return true;
				}
			}
		}
		return false;
	}
	
	private static void boostrapSubdivisionData(List<PersonalDataStruct> legalStruct1, Search search, boolean boostrapBlock) {
		
		String aoAndTrLots = search.getSa().getAtribute(SearchAttributes.LD_LOTNO);
		String []allAoAndTrlots = new String[0];
		
		if(!StringUtils.isEmpty(aoAndTrLots)){
			allAoAndTrlots = aoAndTrLots.split("[ /t/r/n,-]+");
		}
		
		Set<String> allLots = new HashSet<String>();
		allLots.addAll(Arrays.asList(allAoAndTrlots));
		
		
		SearchAttributes sa = search.getSa();
		if(boostrapBlock){
			for(PersonalDataStruct legalStruct:legalStruct1){
				if(!StringUtils.isEmpty(legalStruct.block)){
					sa.setAtribute( SearchAttributes.LD_SUBDIV_BLOCK, legalStruct.block);
				}
			}
		}
		
		for(PersonalDataStruct legalStruct:legalStruct1){
			if(!StringUtils.isEmpty(legalStruct.lot)){
				allLots.add(legalStruct.lot);
			}
		}
		
		String finalLot = "";
		for(String lot:allLots){
			finalLot = finalLot + lot+",";
		}
		
		if(finalLot.length()>1){
			finalLot = finalLot.substring(0,finalLot.length()-1);
			search.getSa().setAtribute(SearchAttributes.LD_LOTNO, finalLot);
		}
		
	}
	
	private static void boostrapSectionalData(List<PersonalDataStruct> legalStruct1, Search search) {
		SearchAttributes sa = search.getSa();
		PersonalDataStruct legalStruct = legalStruct1.get(0);
		if(!StringUtils.isEmpty(legalStruct.section)){
			sa.setAtribute( SearchAttributes.LD_SUBDIV_SEC, legalStruct.section);
		}
		
		if(!StringUtils.isEmpty(legalStruct.township)){
			sa.setAtribute( SearchAttributes.LD_SUBDIV_TWN, legalStruct.township);
		}
		
		if(!StringUtils.isEmpty(legalStruct.range)){
			sa.setAtribute( SearchAttributes.LD_SUBDIV_RNG, legalStruct.range);
		}
	}
	
	protected class PlatIterator extends GenericRuntimeIterator<PersonalDataStruct> {
		
	
		private static final long serialVersionUID = 793434519L;
		
		PlatIterator(long searchId) {
			super(searchId);
		}
		
		@SuppressWarnings("unchecked")
		List<PersonalDataStruct> createDerivationInternal(long searchId){
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)global.getAdditionalInfo("IL_KANE_RO_LOOK_UP_DATA");
			
			List<PersonalDataStruct> newlist = new ArrayList<PersonalDataStruct>();
			PersonalDataStruct legalStructItem = new PersonalDataStruct();
			
			for(PersonalDataStruct struct:legalStructList){
				  String subdivCode = struct.subCode;
				  String subName = getSubdivisions(subdivCode);
				  if (StringUtils.isNotEmpty(subdivCode)){
					  for (int i = 0; i < INSTRUMENT_TYPE_PLAT.length; i++){
						  legalStructItem = new PersonalDataStruct();
						  legalStructItem.subCode = subdivCode;
						  legalStructItem.subName = subName;
						  legalStructItem.instrType = INSTRUMENT_TYPE_PLAT[i];
						  if( !testIfExist(newlist, legalStructItem, "plat") ){
							  newlist.add(legalStructItem);
							}
					  }
				  }
			  }
				
			return newlist;
		}
		
		protected List<PersonalDataStruct> createDerrivations(){
			return createDerivationInternal(searchId);
		}
		
		protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
			
			switch(module.getModuleIdx()){
				case TSServerInfo.ADV_SEARCH_MODULE_IDX:
					if(module.getSaObjKey().equals(SearchAttributes.LD_SUBDIV_NAME)){
						//String subCode = str.subCode;
						String subName = str.subName;
						subName = subName.replaceAll("[',]+", "");
						module.setData(1, subName);
						module.setData(2, "Or");
						module.setData(3, subName);
	                    module.setData(26, str.instrType);
					}
				break;
			}
		}

	}
	
	protected class PlatIteratorD extends GenericRuntimeIterator<PersonalDataStruct> {
		
		
		private static final long serialVersionUID = 793434519L;
		
		PlatIteratorD(long searchId) {
			super(searchId);
		}
		
		@SuppressWarnings("unchecked")
		List<PersonalDataStruct> createDerivationInternal(long searchId){
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)global.getAdditionalInfo("IL_KANE_RO_LOOK_UP_DATA");
			
			List<PersonalDataStruct> newlist = new ArrayList<PersonalDataStruct>();
			PersonalDataStruct legalStructItem = new PersonalDataStruct();
			
			for(PersonalDataStruct struct:legalStructList){
				  String subdivCode = struct.subCode;
				  String subName = getSubdivisions(subdivCode);
				  if (StringUtils.isNotEmpty(subdivCode)){
					  for (int i = 0; i < INSTRUMENT_TYPE.length; i++){
						  legalStructItem = new PersonalDataStruct();
						  legalStructItem.subCode = subdivCode;
						  legalStructItem.subName = subName;
						  legalStructItem.instrType = INSTRUMENT_TYPE[i];
						  if( !testIfExist(newlist, legalStructItem, "plat") ){
							  newlist.add(legalStructItem);
							}
					  }
				  }
			}
				
			return newlist;
		}
		
		protected List<PersonalDataStruct> createDerrivations(){
			return createDerivationInternal(searchId);
		}
		
		protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
			
			switch(module.getModuleIdx()){
				case TSServerInfo.ADV_SEARCH_MODULE_IDX:
					if(module.getSaObjKey().equals(SearchAttributes.LD_SUBDIV_NAME)){
						//String subCode = str.subCode;
						String subName = str.subName;
						subName = subName.replaceAll("[',]+", "");
						module.setData(1, subName);
						module.setData(2, "Or");
						module.setData(3, subName);
	                    module.setData(26, str.instrType);
					}
				break;
			}
		}

	}
	
	protected class PlatIteratorForDocs extends GenericRuntimeIterator<PersonalDataStruct> {
		
		
		private static final long serialVersionUID = 1679434519L;
		
		PlatIteratorForDocs(long searchId) {
			super(searchId);
		}
		
		@SuppressWarnings("unchecked")
		List<PersonalDataStruct> createDerivationInternal(long searchId){
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)global.getAdditionalInfo("IL_KANE_RO_LOOK_UP_DATA");
			
			List<PersonalDataStruct> newlist = new ArrayList<PersonalDataStruct>();
			PersonalDataStruct legalStructItem = new PersonalDataStruct();
			
			for (PersonalDataStruct struct:legalStructList){
				  String subCode = struct.subCode;
				  if (StringUtils.isNotEmpty(subCode)){
					  String platDoc = DBManager.getILKanePlatDocBySubCode(subCode);
					  if (StringUtils.isNotEmpty(platDoc)){
						  legalStructItem.subCode = StringUtils.isEmpty(subCode) ? "" : subCode;
						  legalStructItem.platDoc = StringUtils.isEmpty(platDoc) ? "" : platDoc;
						  if( !testIfExist(newlist, legalStructItem, "platDoc") ){
							  newlist.add(legalStructItem);
						  }
					  }
				  }				
			}
			
			return newlist;
		}
		
		protected List<PersonalDataStruct> createDerrivations(){
			return createDerivationInternal(searchId);
		}
		
		protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
			
			switch(module.getModuleIdx()){
				case TSServerInfo.INSTR_NO_MODULE_IDX:
					if(module.getSaObjKey().equals(SearchAttributes.LD_SUBDIV_NAME)){
						String platDoc = str.platDoc;
						module.setData(0, platDoc);
					}
				break;
			}
		}

	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);
		String dataSiteLink = dataSite.getLink();
		HtmlParser3 htmlParser3;
		String documentPath = "";
		String documentNumber = "";
		ImageLinkInPage ilip;

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_SUBDIVISION_NAME:// the two search by subdiv/block/lot modules: for auto and with suggestions
		case ID_SEARCH_BY_MODULE38:// s/t/r
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_MODULE39:// doctype
		case ID_SEARCH_BY_MODULE40:// remarks
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_MODULE41:// UCC bnumber
		case ID_SEARCH_BY_MODULE42:// associated doc
		case ID_SEARCH_BY_MODULE53:// advanced search
		case ID_SEARCH_BY_SALES:// ucc search

			String intermediariesHtml = getHTMLIntermediaries(rsResponse);
			if (intermediariesHtml.isEmpty()) {
				parsedResponse.setError("<font color=\"red\">No results found</font>");
				return;
			}

			if (intermediariesHtml.indexOf("Number of results: 1000") != -1) {
				intermediariesHtml = "<div id=\"maxResultsMessage\"><b><font color=\"red\"> The official site has more than 1000 Records Returned,"
						+ " but displays only 1000 Records.</font></b><br><br></div>" + intermediariesHtml;
			}

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, intermediariesHtml, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}
			if (smartParsedResponses.size() == 0) {
				return;
			}

			Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);

			String header = parsedResponse.getHeader();
			String footer = parsedResponse.getFooter();
			header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + header;
			if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
				footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
			} else {
				footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
			}

			parsedResponse.setHeader(header);
			parsedResponse.setFooter(footer);

			break;

		case ID_DETAILS:

			String details = "";
			details = getDetails(rsResponse);

			Document doc = Tidy.tidyParse(details);
			String docNo = HtmlParserTidy.getValueFromTagByAttr(doc, "class", "lrsDetailsViewDocNum", "div");
			docNo = docNo.replaceAll("\\s*<[^>]*>\\s*", "").replaceAll("(?is)Document\\s*#?\\s*:?\\b(\\d+)$", "$1");

			if (sAction.indexOf("documentId=") != -1) {// if not UCC search
				try {
					String imageLink = "";
					imageLink = dataSiteLink + "Search/GetDocPath?documentId=" + sAction.replaceFirst("(?is).*documentId=(\\d+).*", "$1");
					String imageLinkL1Contents = getLinkContents(imageLink);
					JSONObject jsonObject;

					jsonObject = (new JSONObject(imageLinkL1Contents));
					documentPath = jsonObject.getString("DocumentPath").trim();
					documentNumber = jsonObject.getString("DocumentNumber").trim();
					try {
						imageLink = "/Search/GetDocumentImage?documentPath=" + URLEncoder.encode(documentPath, "UTF-8") + "&docNumber=" + documentNumber;
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					ilip = new ImageLinkInPage(imageLink, documentNumber + ".tif");
					parsedResponse.addImageLink(ilip);
					details = details.replaceFirst("(?is)</table>\\s*$", Matcher.quoteReplacement("<tr><td align=\"center\"><a target=\"_blank\" href=\""
							+ (CreatePartialLink(TSConnectionURL.idGET)
									+ dataSiteLink.substring(0, dataSiteLink.lastIndexOf("/")) + imageLink) + "\">View Image</a></td></tr></table>"));
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			if ((!downloadingForSave)) {
				org.w3c.dom.Document finalDoc = Tidy.tidyParse(details);
				String serverDocType = HtmlParserTidy.getValueFromTagByAttr(doc, "class", "lrsDetailsViewType", "div")
						.replaceAll("\\s*<[^>]*>\\s*", "")
						.replaceAll("(?is)Type\\s*:?\\b\\s*(.*)\\s*$", "$1");
				String remarks = HtmlParserTidy.getValueFromTagById(finalDoc, "lblRemarks", "span").trim();
				remarks = remarks.replaceAll("(?is)</?span[^>]*>", "");
				remarks = remarks.replaceAll("(?is)<br>", "");
				ResultMap m = new ResultMap();
				String documentType = "";
				try {
					documentType = ro.cst.tsearch.servers.functions.ILKaneRO.parseDocTypeILKaneRO(m, serverDocType, remarks, searchId);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (StringUtils.isNotEmpty(serverDocType)) {
					documentType = DocumentTypes.getDocumentCategory(documentType, searchId);
				}
				String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + docNo + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				if (StringUtils.isNotEmpty(documentType)) {
					docNo += documentType;
				}

				if (FileAlreadyExist(docNo + ".html")) {
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}

				parsedResponse.setResponse(details);
			} else {
				details = details.replaceFirst("(?is)<tr><td align=\"center\"><a target=\"_blank\" href=[^>]*>View Image</a></td></tr>\\s*(</table>\\s*)$",
						"$1");// remove image link from docIndex
				details = details.replaceAll("View Image", "");
				smartParseDetails(Response, details);
				msSaveToTSDFileName = docNo + ".html";
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;

		case ID_SEARCH_BY_MODULE43:// Document Books
			parsedResponse.setResponse(processDocBooksResponse(sAction, rsResponse, linkStart));
			break;

		case ID_SEARCH_BY_MODULE44:// Plat Books
			parsedResponse.setResponse(processPlatBookResponse(sAction, rsResponse, linkStart));
			break;
		case ID_SEARCH_BY_MODULE45:// Subdivisions(Tract)
			parsedResponse.setResponse(processSubdivTractResponse(sAction, rsResponse, linkStart));
			break;
		case ID_SEARCH_BY_MODULE46:// Sections(Tract)
			parsedResponse.setResponse(processSectionsTractResponse(sAction, rsResponse, linkStart));
			break;
		case ID_SEARCH_BY_MODULE47:// All Misc Docs
			parsedResponse.setResponse(processAllMiscDocsResponse(sAction, rsResponse, linkStart));
			break;
		case ID_SEARCH_BY_MODULE48:// Entry Book Search
			htmlParser3 = new HtmlParser3(rsResponse);

			String book = "";
			String page = "";
			Node documentPathNode = htmlParser3.getNodeById("DocumentPath");
			if (documentPathNode != null) {
				documentPath = documentPathNode.toHtml().replaceFirst("(?is).*?\\bvalue=\"([^\"]*)\".*", "$1");
				documentNumber = documentPath.substring(documentPath.lastIndexOf("\\") + 1, documentPath.lastIndexOf("."));
				book = documentPath.substring(0, documentPath.lastIndexOf("\\"));
				book = book.substring(book.lastIndexOf("\\") + 1).trim();
				page = book.replaceFirst(".*-\\s*(.*)", "$1");
				book = book.replaceFirst("(.*?)\\s*-.*", "$1");

				try {
					documentPath = URLEncoder.encode(documentPath, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				String imageLink = linkStart + dataSiteLink + sAction.substring(1) + "?documentPath=" + documentPath + "&DocNumber=" + documentNumber;

				String response = "<table border=\"1\" align=\"center\" style=\"min-width:200px;\"><tr>"
						+ "<td><input name=\"docLink\" id=\"" + book + "-" + page + "\" " + "type=\"checkbox\" value=\"" + imageLink + "\"></td>"
						+ "<td><a name=\"imageLinkRO\" href=\"" + imageLink + "\">View Image</a></td></tr></table>";

				response = "<form name=\"savetiff\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + response
						+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL
						+ "\" onClick=\"javascript:submitFormMultiDocSave();\">"
						+ "<input type=\"hidden\" name=\"image_type\" value=\"tiff\"/>"
						+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId + "\"/>"
						+ "<input type=\"hidden\" name=\"parentSiteSaveType\" id=\"parentSiteSaveType\" value=\"1\" disabled=\"\">"
						+ "<input type=\"hidden\" name=\"dispatcher\" value=\""
						+ URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION + "\"/>"
						+ "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\"/>"
						+ "<input type=\"hidden\" name=\"searchType\" value=\"" + DocumentTypes.MISCELLANEOUS + "\"/>"
						+ "<input type=\"hidden\" name=\"searchTypeModule\" value=\"IM\"/>"
						+ "</form>";
				parsedResponse.setResponse(response);
			}

			break;
		case ID_SEARCH_BY_MODULE49:// Search By Subdivision/Block/Lot(with suggestions) - based on subdivision selection;
			String lastLink = (Response.getLastURI() == null ? "" : Response.getLastURI().toString());
			String extraSearchParams = "&oAdditional.DateFiledFrom=" + StringUtils.extractParameterFromUrl(lastLink, "oAdditional.DateFiledFrom")
					+ "&oAdditional.DateFiledTo=" + StringUtils.extractParameterFromUrl(lastLink, "oAdditional.DateFiledTo")
					+ "&oAdditional.DocNumberFrom=" + StringUtils.extractParameterFromUrl(lastLink, "oAdditional.DocNumberFrom")
					+ "&oAdditional.DocNumberTo=" + StringUtils.extractParameterFromUrl(lastLink, "oAdditional.DocNumberTo")
					+ "&byType=" + StringUtils.extractParameterFromUrl(lastLink, "byType");

			parsedResponse.setResponse(processSubdBlockLotResponse(sAction, rsResponse, linkStart, extraSearchParams));
			break;

		case ID_GET_LINK:
			if (sAction.indexOf("/Search/DetailsModal") != -1 || sAction.indexOf("/Search/UCCDetails") != -1) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else if (sAction.indexOf("/Search/FilterSubdivisionCodes") != -1) {
				ParseResponse(sAction, Response, ID_BROWSE_SCANNED_INDEX_PAGES);
			} else if (sAction.indexOf("/Search/GetDocPathSubCode") != -1) {
				GetLink(sAction, true);
			} else if (sAction.indexOf("/Search/GetDocumentBookPages") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE43);
			} else if (sAction.indexOf("/Search/GetPlatBookPages") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE44);
			} else if (sAction.indexOf("/Search/GetSubSubdivisions") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE45);
			} else if (sAction.indexOf("/Search/GetSubBlocks") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE45);
			} else if (sAction.indexOf("/Search/GetSubPages") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE45);
			} else if (sAction.indexOf("/Search/GetSecSections") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE46);
			} else if (sAction.indexOf("/Search/GetSecQuadrants") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE46);
			} else if (sAction.indexOf("/Search/GetSecPages") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE46);
			} else if (sAction.indexOf("/Search/GetVolumes") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE47);
			} else if (sAction.indexOf("/Search/GetPages") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE47);
			} else if (sAction.indexOf("/Search/GetBlocks") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE49);
			} else if (sAction.indexOf("/Search/GetLots") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE49);
			} else if (sAction.indexOf("/Search/Criteria?criteria=Subdivision") != -1) {
				// redirect(from module ID_SEARCH_BY_MODULE49) to search by subdivision intermediaries after selection of subd code/block/lot
				ParseResponse(sAction, Response, ID_SEARCH_BY_SUBDIVISION_NAME);
			}
			break;
		case ID_SAVE_TO_TSD:

			downloadingForSave = true;
			if (!(sAction.contains("/Search/GetDocumentImageMisc") || sAction.contains("/Search/EntryBookSearch"))) {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			downloadingForSave = false;
			break;

		case ID_BROWSE_SCANNED_INDEX_PAGES:
			String response = "";

			rsResponse = rsResponse.replaceAll("(?is)<a[^>]*>(\\s*plat\\s+book\\s*)</a>", "$1");
			rsResponse = rsResponse.replaceAll("(?is)\\s*<script[^>]*>.*?</script>\\s*", "");
			rsResponse = rsResponse.replaceFirst("(?is)(<table[^>]*id=\"sub_codes\")", "$1 border=\"1\"");
			rsResponse = rsResponse.replaceAll("(?is)(<table\\s+class=\"bookTable\")[^>]*>", "$1 cellpadding=\"2\" cellspacing=\"5\">");
			rsResponse = rsResponse.replaceFirst("(?is)<tr[^>]*>\\s*<th[^>]*>\\s*Subdivision Name.*?Remarks.*?</th>.*?</tr>",
					"<tr><th>Subdivision Name</th><th>Code</th><th>Plat Volume</th><th>Plat Page</th><th>Remarks</th><th>Doc #</th><th>&nbsp;</th></tr>");
			rsResponse = rsResponse.replaceAll("(?is)\\s*<th[^>]*class=\"bookTH\"[^>]*>.*?<table[^>]*class=\"booktable\"[^>]*>"
					+ ".*?<td[^>]*>(.*?)</td>.*?<td[^>]*>(.*?)</td>.*?<td[^>]*>(.*?)</td>.*?<td[^>]*>(.*?)</td>"
					+ ".*?<td[^>]*>(.*?)</td>.*?</table>.*?</th>\\s*",
					"<td class=\"platBook\">$1</td><td class=\"platPage\">$2</td><td>$3</td><td class=\"docNo\">$4</td><td>$5</td>");
			rsResponse = rsResponse.replaceAll("(?is)(<a[^>]*name=\"codeImage\"[^>]*)", "$1 href=\"" + 1 + "\"");
			Pattern pat = Pattern
					.compile("(?is)(<td\\s+class=\"platBook\">(.*?)</td><td\\s+class=\"platPage\">(.*?)</td><td>.*?</td><td\\s+class=\"docNo\">)(.*?)</td>");
			Matcher mat = pat.matcher(rsResponse);
			while (mat.find()) {
				String platVolume = mat.group(2).trim();
				String platBook = mat.group(3).trim();
				String docNumber = mat.group(4).replaceFirst("(?is).*?<a[^>]*>\\s*(.*?)\\s*</a>.*", "$1").replaceAll("&nbsp;", "").trim();
				String imageLink = linkStart + dataSiteLink + "Search/GetDocPathSubCode?docNumber=" + docNumber + "&platVolume=" + platVolume + "&platPage="
						+ platBook;
				rsResponse = rsResponse.replaceFirst("(?is)<a[^>]*\\bid=[^>]*>(.*?" + docNumber + ".*?</a>)", "<a target=\"_blank\" href=\"" + imageLink
						+ "\">$1");
			}
			htmlParser3 = new HtmlParser3(rsResponse);
			Node subNode = htmlParser3.getNodeById("subCodesPartial");

			if (subNode != null) {
				String[] letters = { "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q",
						"R", "S", "T", "U", "V", "W", "X", "Y", "Z" };
				StringBuilder filterLetters = new StringBuilder("<div class=\"center\" style=\"margin-bottom: 3px;\"> ");
				for (int i = 0; i < letters.length; i++) {
					filterLetters.append("&nbsp;&nbsp;<a href=\"" + linkStart + "/Search/FilterSubdivisionCodes?letter="
							+ letters[i] + "\">" + letters[i] + "</a>");
				}

				filterLetters.append("</div>");
				response = "<table align=\"center\"><tr><td align=\"center\">"
						+ filterLetters.toString()
						+ "</td></tr><tr><td align=\"center\">"
						+ subNode.toHtml().replaceAll("(?is)<a[^>]*href=\"/Search/Advanced[^>]*>", "")
						+ "</td></tr></table>";

				parsedResponse.setResponse(response);
			}

			break;
		}
	}

	private String processDocBooksResponse(String sAction, String rsResponse, String linkStart) {
		HtmlParser3 htmlParser3;
		String dataSiteLink = dataSite.getLink();
		String response = "";
		if (sAction.indexOf("/Search/GetDocumentBookPages") != -1) {// doc page extracted after selecting doc book
			htmlParser3 = new HtmlParser3(rsResponse);
			Node node = htmlParser3.getNodeById("page");
			if (node != null) {
				response = node.toHtml();
				String book = StringUtils.extractParameterFromUrl(sAction, "mainName");
				String imageLink = "";
				Pattern pattern = Pattern.compile("(?is)(<tr\\s*[^>]*id=\"([^\"]*)\"[^>]*>.*?)<a[^>]*page=\\\"([^\\\"]*)\\\"[^>]*>(.*?</tr>)");
				Matcher matcher = pattern.matcher(response);

				while (matcher.find()) {// add image link
					try {
						String docNo = matcher.group(3);
						imageLink = linkStart + dataSiteLink + "Search/GetDocumentImageMisc?docNumber=" + docNo + "&documentPath=";
						imageLink = Matcher.quoteReplacement(imageLink + URLEncoder.encode(matcher.group(2), "UTF-8"));
						response = response.replaceFirst("(?is)<a\\s+id=\"viewDoc\"[^>]*>", "<a href=\"" + imageLink + "\" name=\"imageLinkRO\" >");

					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}

				response = response.replaceFirst("(?is)<table[^>]*>",
						"<div align=\"center\"><strong>Book: " + book + "</strong></div><table border=\"1\" align=\"center\" style=\"min-width:200px;\">");
				response = response.replaceFirst("(?is)(<th>\\s*Page\\s*</th>)",
						"<th></th>$1");

				// add checkboxes
				response = response.replaceAll("(?is)(<td[^>]*>(.*?)</td>\\s*<td[^>]*>\\s*<a\\s+href=\"([^\"]*)\"[^>]*>)",
						"<td><input name=\"docLink\" id=\"" + book + "-$2" + "\" " + "type=\"checkbox\" value=\"$3\"></td>$1");

				response = "<form name=\"savetiff\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + response
						+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL
						+ "\" onClick=\"javascript:submitFormMultiDocSave();\">"
						+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId + "\"/>"
						+ "<input type=\"hidden\" name=\"parentSiteSaveType\" id=\"parentSiteSaveType\" value=\"1\" disabled=\"\">"
						+ "<input type=\"hidden\" name=\"image_type\" value=\"tiff\"/>"
						+ "<input type=\"hidden\" name=\"dispatcher\" value=\""
						+ URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION + "\"/>"
						+ "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\"/>"
						+ "<input type=\"hidden\" name=\"searchType\" value=\"" + DocumentTypes.MISCELLANEOUS + "\"/>"
						+ "<input type=\"hidden\" name=\"searchTypeModule\" value=\"IM\"/>"
						+ "</form>";

			}
		} else {// doc books
			htmlParser3 = new HtmlParser3(rsResponse);
			Node node = htmlParser3.getNodeById("main");
			if (node != null) {
				String getPages = "<script type=\"text/javascript\"> function getPages()"
						+ "{var sel = document.getElementById('booksSelect'); "
						+ "var val = sel.selectedIndex; "
						+ "var book = sel.options[val].value; "
						+ "var url = \"" + linkStart + "/Search/GetDocumentBookPages?mainName=\"+ book;"
						+ "if (book == \"\" ){"
						+ "alert(\"Please choose a book! \")}"
						+ "else {window.location.href = url;  }}</script>";

				response = node.toHtml();
				response = response.replaceAll("(?is)\\s*<th>.*?</th>\\s*", "");
				response = response.replaceAll("(?is)\\s*</?(?:table|thead|tbody|tr)[^>]*>\\s*", "");
				response = response.replaceAll("(?is)<td[^>]*>\\s*(.*?)\\s*</td>", "<option>$1</option>");
				response = getPages
						+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
						+ "<tr><td><strong>Folder</strong></td></tr><tr><td>"
						+ "<select id=\"booksSelect\" style=\"min-width:200px;\""
						+ "size=\"30\" onchange=\"getPages()\">"
						+ response + "</select></td></tr></table>";
			}
		}
		return response;
	}

	private String processPlatBookResponse(String sAction, String rsResponse, String linkStart) {
		String response = "";
		String dataSiteLink = dataSite.getLink();
		HtmlParser3 htmlParser3;
		if (sAction.indexOf("/Search/GetPlatBookPages") != -1) {// page extracted after selecting book
			htmlParser3 = new HtmlParser3(rsResponse);
			Node node = htmlParser3.getNodeById("page");
			if (node != null) {
				response = node.toHtml();
				String imageLink = "";
				Pattern pattern = Pattern.compile("(?is)(<tr\\s*[^>]*id=\"([^\"]*)\"[^>]*>.*?)<a[^>]*page=\\\"([^\\\"]*)\\\"[^>]*>(.*?</tr>)");
				Matcher matcher = pattern.matcher(response);

				while (matcher.find()) {// add image link
					try {
						String docNo = matcher.group(3);
						imageLink = linkStart + dataSiteLink + "Search/GetDocumentImageMisc?docNumber=" + docNo + "&documentPath=";
						imageLink = Matcher.quoteReplacement(imageLink + URLEncoder.encode(matcher.group(2), "UTF-8"));
						response = response.replaceFirst("(?is)<a\\s+id=\"viewDoc\"[^>]*>", "<a href=\"" + imageLink + "\" name=\"imageLinkRO\">");

					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}

				response = response.replaceFirst("(?is)<table[^>]*>",
						"<table border=\"1\" align=\"center\" style=\"min-width:200px;\">");
				response = response.replaceFirst("(?is)(<th>\\s*Page\\s*</th>)",
						"<th></th>$1");

				// add checkboxes
				response = response.replaceAll("(?is)(<td[^>]*>(.*?)</td>\\s*<td[^>]*>\\s*<a\\s+href=\"([^\"]*)\"[^>]*>)",
						"<td><input name=\"docLink\" id=\"$2" + "\" " + "type=\"checkbox\" value=\"$3\"></td>$1");
				response = response.replaceAll("(?is)(<input\\s*name=\\s*\")\\s*BOOK\\s*(\\d+)\\s*PAGE\\s*(\\d+)([a-z]?\\s*\")",
						"$1$2-$3$4");

				response = "<form name=\"savetiff\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + response
						+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL
						+ "\" onClick=\"javascript:submitFormMultiDocSave();\">"
						+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId + "\"/>"
						+ "<input type=\"hidden\" name=\"image_type\" value=\"tiff\"/>"
						+ "<input type=\"hidden\" name=\"parentSiteSaveType\" id=\"parentSiteSaveType\" value=\"1\" disabled=\"\">"
						+ "<input type=\"hidden\" name=\"dispatcher\" value=\""
						+ URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION + "\"/>"
						+ "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\"/>"
						+ "<input type=\"hidden\" name=\"searchType\" value=\"" + DocumentTypes.PLAT + "\"/>"
						+ "<input type=\"hidden\" name=\"searchTypeModule\" value=\"IM\"/>"
						+ "</form>";
			}
		} else {// books
			htmlParser3 = new HtmlParser3(rsResponse);
			Node node = htmlParser3.getNodeById("main");
			if (node != null) {
				String getPages = "<script type=\"text/javascript\"> function getPages()"
						+ "{var sel = document.getElementById('booksSelect'); "
						+ "var val = sel.selectedIndex; "
						+ "var book = sel.options[val].value; "
						+ "var url = \"" + linkStart + "/Search/GetPlatBookPages?mainName=\"+ book;"
						+ "if (book == \"\" ){"
						+ "alert(\"Please choose a book! \")}"
						+ "else {window.location.href = url;  }}</script>";

				response = node.toHtml();
				response = response.replaceAll("(?is)\\s*<th>.*?</th>\\s*", "");
				response = response.replaceAll("(?is)\\s*</?(?:table|thead|tbody|tr)[^>]*>\\s*", "");
				response = response.replaceAll("(?is)<td[^>]*>\\s*(.*?)\\s*</td>", "<option>$1</option>");
				response = getPages
						+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
						+ "<tr><td><strong>Folder</strong></td></tr><tr><td>"
						+ "<select id=\"booksSelect\" style=\"min-width:200px;\""
						+ "size=\"30\" onchange=\"getPages()\">"
						+ response + "</select></td></tr></table>";

			}
		}
		return response;
	}

	private String processSubdivTractResponse(String sAction, String rsResponse, String linkStart) {
		HtmlParser3 htmlParser3;
		String dataSiteLink = dataSite.getLink();
		String response = "";
		if (sAction.indexOf("/Search/GetSubPages") != -1) {
			htmlParser3 = new HtmlParser3(rsResponse);
			Node node = htmlParser3.getNodeById("page");
			if (node != null) {
				response = node.toHtml();
				String imageLink = "";
				Pattern pattern = Pattern.compile("(?is)(<tr\\s*[^>]*id=\"([^\"]*)\"[^>]*>.*?)<a[^>]*page=\\\"([^\\\"]*)\\\"[^>]*>(.*?</tr>)");
				Matcher matcher = pattern.matcher(response);

				while (matcher.find()) {// add image link
					try {
						String docNo = matcher.group(3);
						imageLink = linkStart + dataSiteLink + "Search/GetDocumentImageMisc?docNumber=" + docNo + "&documentPath=";
						imageLink = Matcher.quoteReplacement(imageLink + URLEncoder.encode(matcher.group(2), "UTF-8"));
						response = response.replaceFirst("(?is)<a\\s+id=\"viewDoc\"[^>]*>", "<a href=\"" + imageLink + "\" name=\"imageLinkRO\">");

					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}

				String town = StringUtils.extractParameterFromUrl(sAction, "townName");
				String subdivision = "";
				try {
					subdivision = URLDecoder.decode(StringUtils.extractParameterFromUrl(this.msLastLink, "subdivisionName"), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				String block = StringUtils.extractParameterFromUrl(this.msLastLink, "blockname");
				response = response.replaceFirst("(?is)<table[^>]*>",
						"<div align=\"center\"><strong>Town: " + town + "</strong></div>"
								+ "<div align=\"center\"><strong>Subdivision: " + subdivision + "</strong></div>"
								+ "<div align=\"center\"><strong>Block: " + block + "</strong></div>"
								+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">");
				response = response.replaceFirst("(?is)(<th>\\s*Page\\s*</th>)",
						"<th></th>$1");

				// add checkboxes
				response = response.replaceAll("(?is)(<td[^>]*>(.*?)</td>\\s*<td[^>]*>\\s*<a\\s+href=\"([^\"]*)\"[^>]*>)",
						"<td><input name=\"docLink\" id=\"$2" + "\" " + "type=\"checkbox\" value=\"$3\"></td>$1");

				response = "<form name=\"savetiff\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + response
						+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL
						+ "\" onClick=\"javascript:submitFormMultiDocSave();\">"
						+ "<input type=\"hidden\" name=\"image_type\" value=\"tiff\"/>"
						+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId + "\"/>"
						+ "<input type=\"hidden\" name=\"parentSiteSaveType\" id=\"parentSiteSaveType\" value=\"1\" disabled=\"\">"
						+ "<input type=\"hidden\" name=\"dispatcher\" value=\""
						+ URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION + "\"/>"
						+ "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\"/>"
						+ "<input type=\"hidden\" name=\"searchType\" value=\"" + DocumentTypes.MISCELLANEOUS + "\"/>"
						+ "<input type=\"hidden\" name=\"searchTypeModule\" value=\"IM\"/>"
						+ "</form>";

			}
		} else if (sAction.indexOf("/Search/GetSubSubdivisions") != -1) {// get subdivisions
			Object resultsObj;
			JSONArray jsonResults = new JSONArray();

			try {
				JSONObject jsonObject = (new JSONObject(rsResponse));
				if ((resultsObj = jsonObject.get("aaData")) instanceof JSONArray) {
					jsonResults = (JSONArray) resultsObj;
				}

				for (int i = 0; i < jsonResults.length(); i++) {
					String subdivision = jsonResults.getString(i).trim();
					response += "<option>" + subdivision.replaceFirst("\\[\"([^\"]*)\"\\]", "$1") + "</option>";
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			String town = StringUtils.extractParameterFromUrl(sAction, "townName");
			String getBlocks = "<script type=\"text/javascript\"> function getBlocks()"
					+ "{var sel = document.getElementById('subdivisionsSelect'); "
					+ "var val = sel.selectedIndex; "
					+ "var subdivision = sel.options[val].value; "
					+ "var url = \"" + linkStart + "/Search/GetSubBlocks?townName=" + town + "&subdivisionName=\"+subdivision;"
					+ "if (subdivision == \"\" ){"
					+ "alert(\"Please choose a subdivision! \")}"
					+ "else {window.location.href = url;  }}</script>";

			response = getBlocks
					+ "<div align=\"center\"><strong>Town: " + town + "</strong></div>"
					+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
					+ "<tr><td><strong>Subdivision</strong></td></tr><tr><td>"
					+ "<select id=\"subdivisionsSelect\" style=\"min-width:200px;\""
					+ "size=\"30\" onchange=\"getBlocks()\">"
					+ response + "</select></td></tr></table>";

		} else if (sAction.indexOf("/Search/GetSubBlocks") != -1) {// get blocks
			Object resultsObj;
			JSONArray jsonResults = new JSONArray();

			try {
				JSONObject jsonObject = (new JSONObject(rsResponse));
				if ((resultsObj = jsonObject.get("aaData")) instanceof JSONArray) {
					jsonResults = (JSONArray) resultsObj;
				}

				for (int i = 0; i < jsonResults.length(); i++) {
					String block = jsonResults.getString(i).trim();
					response += "<option>" + block.replaceFirst("\\[\"([^\"]*)\"\\]", "$1") + "</option>";
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			String town = StringUtils.extractParameterFromUrl(sAction, "townName");
			String subdivision = StringUtils.extractParameterFromUrl(this.msLastLink, "subdivisionName");
			String getPages = "<script type=\"text/javascript\"> function getPages()"
					+ "{var sel = document.getElementById('blocksSelect'); "
					+ "var val = sel.selectedIndex; "
					+ "var block = sel.options[val].value; "
					+ "var url = \"" + linkStart + "/Search/GetSubPages?townName=" + town + "&subdivisionName=" + subdivision + "&blockName=\"+block;"
					+ "if (block == \"\" ){"
					+ "alert(\"Please choose a block! \")}"
					+ "else {window.location.href = url;  }}</script>";
			try {
				subdivision = URLDecoder.decode(subdivision, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			response = getPages
					+ "<div align=\"center\"><strong>Town: " + town + "</strong></div>"
					+ "<div align=\"center\"><strong>Subdivision: " + subdivision + "</strong></div>"
					+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
					+ "<tr><td><strong>Block</strong></td></tr><tr><td>"
					+ "<select id=\"blocksSelect\" style=\"min-width:200px;\""
					+ "size=\"30\" onchange=\"getPages()\">"
					+ response + "</select></td></tr></table>";

		} else {// get towns
			htmlParser3 = new HtmlParser3(rsResponse);
			Node node = htmlParser3.getNodeById("subTowns");
			if (node != null) {
				String getSubdivisions = "<script type=\"text/javascript\"> function getSubdivisions()"
						+ "{var sel = document.getElementById('booksSelect'); "
						+ "var val = sel.selectedIndex; "
						+ "var town = sel.options[val].value; "
						+ "var url = \"" + linkStart + "/Search/GetSubSubdivisions?townName=\"+ town;"
						+ "if (town == \"\" ){"
						+ "alert(\"Please choose a town! \")}"
						+ "else {window.location.href = url;  }}</script>";

				response = node.toHtml();
				response = response.replaceAll("(?is)\\s*<th>.*?</th>\\s*", "");
				response = response.replaceAll("(?is)\\s*</?(?:table|thead|tbody|tr)[^>]*>\\s*", "");
				response = response.replaceAll("(?is)<td[^>]*>\\s*(.*?)\\s*</td>", "<option>$1</option>");
				response = getSubdivisions
						+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
						+ "<tr><td><strong>Town</strong></td></tr><tr><td>"
						+ "<select id=\"booksSelect\" style=\"min-width:200px;\""
						+ "size=\"30\" onchange=\"getSubdivisions()\">"
						+ response + "</select></td></tr></table>";
			}
		}
		return response;
	}
	
	private String processSectionsTractResponse(String sAction, String rsResponse, String linkStart) {
		HtmlParser3 htmlParser3;
		String dataSiteLink = dataSite.getLink();
		String response = "";
		if (sAction.indexOf("/Search/GetSecPages") != -1) {
			htmlParser3 = new HtmlParser3(rsResponse);
			Node node = htmlParser3.getNodeById("page");
			if (node != null) {
				response = node.toHtml();
				String imageLink = "";
				Pattern pattern = Pattern.compile("(?is)(<tr\\s*[^>]*id=\"([^\"]*)\"[^>]*>.*?)<a[^>]*page=\\\"([^\\\"]*)\\\"[^>]*>(.*?</tr>)");
				Matcher matcher = pattern.matcher(response);

				while (matcher.find()) {// add image link
					try {
						String docNo = matcher.group(3);
						imageLink = linkStart + dataSiteLink + "Search/GetDocumentImageMisc?docNumber=" + docNo + "&documentPath=";
						imageLink = Matcher.quoteReplacement(imageLink + URLEncoder.encode(matcher.group(2), "UTF-8"));
						response = response.replaceFirst("(?is)<a\\s+id=\"viewDoc\"[^>]*>", "<a href=\"" + imageLink + "\" name=\"imageLinkRO\">");

					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}

				String township ="";
				String section = StringUtils.extractParameterFromUrl(this.msLastLink, "section");
				String quadrant = StringUtils.extractParameterFromUrl(this.msLastLink, "quadrant");
				try {
					township = URLDecoder.decode(StringUtils.extractParameterFromUrl(this.msLastLink, "township"), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				response = response.replaceFirst("(?is)<table[^>]*>",
						"<div align=\"center\"><strong>Township: " + township + "</strong></div>"
								+ "<div align=\"center\"><strong>Section: " + section + "</strong></div>"
								+ "<div align=\"center\"><strong>Quadrant: " + quadrant + "</strong></div>"
								+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">");
				response = response.replaceFirst("(?is)(<th>\\s*Page\\s*</th>)",
						"<th></th>$1");

				// add checkboxes
				response = response.replaceAll("(?is)(<td[^>]*>(.*?)</td>\\s*<td[^>]*>\\s*<a\\s+href=\"([^\"]*)\"[^>]*>)",
						"<td><input name=\"docLink\" id=\"$2" + "\" " + "type=\"checkbox\" value=\"$3\"></td>$1");

				response = "<form name=\"savetiff\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + response
						+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL
						+ "\" onClick=\"javascript:submitFormMultiDocSave();\">"
						+ "<input type=\"hidden\" name=\"image_type\" value=\"tiff\"/>"
						+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId + "\"/>"
						+ "<input type=\"hidden\" name=\"parentSiteSaveType\" id=\"parentSiteSaveType\" value=\"1\" disabled=\"\">"
						+ "<input type=\"hidden\" name=\"dispatcher\" value=\""
						+ URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION + "\"/>"
						+ "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\"/>"
						+ "<input type=\"hidden\" name=\"searchType\" value=\"" + DocumentTypes.MISCELLANEOUS + "\"/>"
						+ "<input type=\"hidden\" name=\"searchTypeModule\" value=\"IM\"/>"
						+ "</form>";

			}
		} else if (sAction.indexOf("/Search/GetSecSections") != -1) {// get sections
			Object resultsObj;
			JSONArray jsonResults = new JSONArray();

			try {
				JSONObject jsonObject = (new JSONObject(rsResponse));
				if ((resultsObj = jsonObject.get("aaData")) instanceof JSONArray) {
					jsonResults = (JSONArray) resultsObj;
				}

				for (int i = 0; i < jsonResults.length(); i++) {
					String section = jsonResults.getString(i).trim();
					response += "<option>" + section.replaceFirst("\\[\"([^\"]*)\"\\]", "$1") + "</option>";
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			String township = StringUtils.extractParameterFromUrl(sAction, "township");
			// http://lrs.kanecountyrecorder.net/Search/GetSecQuadrants?township=38-7%20SUGAR%20GROVE&section=S03
			String getQuadrants = "<script type=\"text/javascript\"> function getQuadrants()"
					+ "{var sel = document.getElementById('sectionsSelect'); "
					+ "var val = sel.selectedIndex; "
					+ "var section = sel.options[val].value; "
					+ "var url = \"" + linkStart + "/Search/GetSecQuadrants?township=" + township + "&section=\"+section;"
					+ "if (section == \"\" ){"
					+ "alert(\"Please choose a section! \")}"
					+ "else {window.location.href = url;  }}</script>";

			try {
				township = URLDecoder.decode(township, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			response = getQuadrants
					+ "<div align=\"center\"><strong>Township: " + township + "</strong></div>"
					+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
					+ "<tr><td><strong>Section</strong></td></tr><tr><td>"
					+ "<select id=\"sectionsSelect\" style=\"min-width:200px;\""
					+ "size=\"30\" onchange=\"getQuadrants()\">"
					+ response + "</select></td></tr></table>";

		} else if (sAction.indexOf("/Search/GetSecQuadrants") != -1) {//get quadrants
			Object resultsObj;
			JSONArray jsonResults = new JSONArray();

			try {
				JSONObject jsonObject = (new JSONObject(rsResponse));
				if ((resultsObj = jsonObject.get("aaData")) instanceof JSONArray) {
					jsonResults = (JSONArray) resultsObj;
				}

				for (int i = 0; i < jsonResults.length(); i++) {
					String quadrant = jsonResults.getString(i).trim();
					response += "<option>" + quadrant.replaceFirst("\\[\"([^\"]*)\"\\]", "$1") + "</option>";
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			String township = StringUtils.extractParameterFromUrl(sAction, "township");
			String section = StringUtils.extractParameterFromUrl(this.msLastLink, "section");
			//http://lrs.kanecountyrecorder.net/Search/GetSecPages?townShip=38-7+SUGAR+GROVE&section=S03&quadrant=NW
			String getPages = "<script type=\"text/javascript\"> function getPages()"
					+ "{var sel = document.getElementById('quadrantsSelect'); "
					+ "var val = sel.selectedIndex; "
					+ "var quadrant = sel.options[val].value; "
					+ "var url = \"" + linkStart + "/Search/GetSecPages?townShip=" + township + "&section=" + section + "&quadrant=\"+quadrant;"
					+ "if (quadrant == \"\" ){"
					+ "alert(\"Please choose a quadrant! \")}"
					+ "else {window.location.href = url;  }}</script>";
			try {
				section = URLDecoder.decode(section, "UTF-8");
				township = URLDecoder.decode(township, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			response = getPages
					+ "<div align=\"center\"><strong>Township: " + township + "</strong></div>"
					+ "<div align=\"center\"><strong>Section: " + section + "</strong></div>"
					+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
					+ "<tr><td><strong>Block</strong></td></tr><tr><td>"
					+ "<select id=\"quadrantsSelect\" style=\"min-width:200px;\""
					+ "size=\"30\" onchange=\"getPages()\">"
					+ response + "</select></td></tr></table>";

		} else {// get township
			htmlParser3 = new HtmlParser3(rsResponse);
			Node node = htmlParser3.getNodeById("secTownship");
			if (node != null) {
				// http://lrs.kanecountyrecorder.net/Search/GetSecSections?township=38-7%20SUGAR%20GROVE
				String getSubdivisions = "<script type=\"text/javascript\"> function getSections()"
						+ "{var sel = document.getElementById('sectionsSelect'); "
						+ "var val = sel.selectedIndex; "
						+ "var township = sel.options[val].value; "
						+ "var url = \"" + linkStart + "/Search/GetSecSections?township=\"+ township;"
						+ "if (township == \"\" ){"
						+ "alert(\"Please choose a township! \")}"
						+ "else {window.location.href = url;  }}</script>";

				response = node.toHtml();
				response = response.replaceAll("(?is)\\s*<th>.*?</th>\\s*", "");
				response = response.replaceAll("(?is)\\s*</?(?:table|thead|tbody|tr)[^>]*>\\s*", "");
				response = response.replaceAll("(?is)<td[^>]*>\\s*(.*?)\\s*</td>", "<option>$1</option>");
				response = getSubdivisions
						+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
						+ "<tr><td><strong>Township</strong></td></tr><tr><td>"
						+ "<select id=\"sectionsSelect\" style=\"min-width:200px;\""
						+ "size=\"30\" onchange=\"getSections()\">"
						+ response + "</select></td></tr></table>";
			}
		}
		return response;
	}
	
	private String processAllMiscDocsResponse(String sAction, String rsResponse, String linkStart) {
		HtmlParser3 htmlParser3;
		String dataSiteLink = dataSite.getLink();
		String response = "";
		if (sAction.indexOf("/Search/GetPages") != -1) {// get pages
			htmlParser3 = new HtmlParser3(rsResponse);
			Node node = htmlParser3.getNodeById("page");
			if (node != null) {
				response = node.toHtml();
				String imageLink = "";
				Pattern pattern = Pattern.compile("(?is)(<tr\\s*[^>]*id=\"([^\"]*)\"[^>]*>.*?)<a[^>]*page=\\\"([^\\\"]*)\\\"[^>]*>(.*?</tr>)");
				Matcher matcher = pattern.matcher(response);

				while (matcher.find()) {// add image link
					try {
						String docNo = matcher.group(3);
						imageLink = linkStart + dataSiteLink + "Search/GetDocumentImageMisc?docNumber=" + docNo + "&documentPath=";
						imageLink = Matcher.quoteReplacement(imageLink + URLEncoder.encode(matcher.group(2), "UTF-8"));
						response = response.replaceFirst("(?is)<a\\s+id=\"viewDoc\"[^>]*>", "<a href=\"" + imageLink + "\" name=\"imageLinkRO\">");

					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}

				String folder = "";
				String volumeName = "";
				try {
					folder = URLDecoder.decode(StringUtils.extractParameterFromUrl(this.msLastLink, "mainName"), "UTF-8");
					volumeName = URLDecoder.decode(StringUtils.extractParameterFromUrl(this.msLastLink, "volumeName"), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				String book = volumeName.replaceFirst("(?s)\\s*(.*?)\\s*-.*", "$1");
				response = response.replaceFirst("(?is)<table[^>]*>",
						"<div align=\"center\"><strong>Folder: " + folder + "</strong></div>"
								+ "<div align=\"center\"><strong>VolumeName: " + volumeName + "</strong></div>"
								+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">");
				response = response.replaceFirst("(?is)(<th>\\s*Page\\s*</th>)",
						"<th></th>$1");

				// add checkboxes
				response = response.replaceAll("(?is)(<td[^>]*>(.*?)</td>\\s*<td[^>]*>\\s*<a\\s+href=\"([^\"]*)\"[^>]*>)",
						"<td><input name=\"docLink\" " + "id=\"" + book + "-$2" + "\" " + "type=\"checkbox\" value=\"$3\"></td>$1");

				response = "<form name=\"savetiff\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + response
						+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL
						+ "\" onClick=\"javascript:submitFormMultiDocSave();\">"
						+ "<input type=\"hidden\" name=\"image_type\" value=\"tiff\"/>"
						+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId + "\"/>"
						+ "<input type=\"hidden\" name=\"parentSiteSaveType\" id=\"parentSiteSaveType\" value=\"1\" disabled=\"\">"
						+ "<input type=\"hidden\" name=\"dispatcher\" value=\""
						+ URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION + "\"/>"
						+ "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\"/>"
						+ "<input type=\"hidden\" name=\"searchType\" value=\"" + DocumentTypes.MISCELLANEOUS + "\"/>"
						+ "<input type=\"hidden\" name=\"searchTypeModule\" value=\"IM\"/>"
						+ "</form>";

			}
		} else if (sAction.indexOf("/Search/GetVolumes") != -1) {// get volumes
			Object resultsObj;
			JSONArray jsonResults = new JSONArray();

			try {
				JSONObject jsonObject = (new JSONObject(rsResponse));
				if ((resultsObj = jsonObject.get("aaData")) instanceof JSONArray) {
					jsonResults = (JSONArray) resultsObj;
				}

				for (int i = 0; i < jsonResults.length(); i++) {
					String volume = jsonResults.getString(i).trim();
					response += "<option>" + volume.replaceFirst("\\[\"([^\"]*)\"\\]", "$1") + "</option>";
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			// http://lrs.kanecountyrecorder.net/Search/GetPages?mainName=BLUE+PRINT+BOOKS&volumeName=BLUEPRINT+BOOK+02
			String folder = StringUtils.extractParameterFromUrl(sAction, "mainName");
			String getPages = "<script type=\"text/javascript\"> function getPages()"
					+ "{var sel = document.getElementById('volumesSelect'); "
					+ "var val = sel.selectedIndex; "
					+ "var volume = sel.options[val].value; "
					+ "var url = \"" + linkStart + "/Search/GetPages?mainName=" + folder + "&volumeName=\"+volume;"
					+ "if (volume == \"\" ){"
					+ "alert(\"Please choose a volume! \")}"
					+ "else {window.location.href = url;  }}</script>";

			try {
				folder = URLDecoder.decode(folder, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			response = getPages
					+ "<div align=\"center\"><strong>Folder: " + folder + "</strong></div>"
					+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
					+ "<tr><td><strong>VolumeName</strong></td></tr><tr><td>"
					+ "<select id=\"volumesSelect\" style=\"min-width:200px;\""
					+ "size=\"30\" onchange=\"getPages()\">"
					+ response + "</select></td></tr></table>";

		} else {// get folder
			htmlParser3 = new HtmlParser3(rsResponse);
			Node node = htmlParser3.getNodeById("main");
			// http://lrs.kanecountyrecorder.net/Search/GetVolumes?mainName=BLUE%20PRINT%20BOOKS&_=1390570700570
			if (node != null) {
				String getVolumes = "<script type=\"text/javascript\"> function getVolumes()"
						+ "{var sel = document.getElementById('folderSelect'); "
						+ "var val = sel.selectedIndex; "
						+ "var folder = sel.options[val].value; "
						+ "var url = \"" + linkStart + "/Search/GetVolumes?mainName=\"+ folder;"
						+ "if (folder == \"\" ){"
						+ "alert(\"Please choose a folder! \")}"
						+ "else {window.location.href = url;  }}</script>";

				response = node.toHtml();
				response = response.replaceAll("(?is)\\s*<th>.*?</th>\\s*", "");
				response = response.replaceAll("(?is)\\s*</?(?:table|thead|tbody|tr)[^>]*>\\s*", "");
				response = response.replaceAll("(?is)<td[^>]*>\\s*(.*?)\\s*</td>", "<option>$1</option>");
				response = getVolumes
						+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
						+ "<tr><td><strong>Folder</strong></td></tr><tr><td>"
						+ "<select id=\"folderSelect\" style=\"min-width:200px;\""
						+ "size=\"30\" onchange=\"getVolumes()\">"
						+ response + "</select></td></tr></table>";
			}
		}
		return response;
	}
	
	private static int seq = 0;	
	protected synchronized static int getSeq(){
		return seq++;
	}
	
	protected String getDetails(String response) {
		response = response.replaceFirst("(?is)<style[^>]*>.*?</style>", "");// UCC details start with this tag
		response = response.trim();
		// if from memory - use it as is
		if (!org.apache.commons.lang.StringUtils.startsWithIgnoreCase(response, "<div")) {
			return response;
		}

		response = response.replaceAll("(?is)style\\s*=\\s*\"[^\"]*\"", "");
		String details = "<table border=\"1\" align=\"center\" style=\"min-width:300px;\"><tr><td align=\"center\">" + response + "</td></tr></table>";
		details = details.replaceAll("(?is)</?a[^>]*>", "");
		// details = details.replaceAll("(?is)<span\\s*class=\"[^\"]*\"([^>]*>.*?)</span>", "<strong$1</strong>");
		details = details.replaceAll("(?is)<div class=\"lrsdetailsodd\"[^>]*>.*?Help.*?Is there incorrect.*?</div>", "");
		return details;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap m) {
		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		ro.cst.tsearch.servers.functions.ILKaneRO.parseAndFillResultMap(detailsHtml, m, searchId);
		return null;
	}
	
	/**
	 * Create list of subdivisions that match
	 * @return
	 */
	
	private String getSubdivisions(String subdivisionCode){
		
		//subdivisionCode = "FWDCTCOND";
		String subdivisionName = "";
		try 
		{
			subdivisionName = MatchEquivalents.getInstance(searchId).getEquivalentILKaneSubdiv
									(SubdivisionMatcher.getInstance(SubdivisionMatcher.IL_KANE, searchId).getSubdivisionName(subdivisionCode));
	        if (StringUtils.isEmpty(subdivisionName)) {
	        	subdivisionName = subdivisionCode;
	        }
	        
		} catch (Exception e) {
    		e.printStackTrace();
    		subdivisionName = subdivisionCode;
    	}
		
		return subdivisionName;
	}

	@Override
	protected ServerResponse SearchBy(boolean resetQuery, TSServerInfoModule module, Object sd)throws ServerResponseException {
		ServerResponse sr = super.SearchBy(resetQuery, module, sd);
		
		if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH){
			if (module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION) == TSServerInfoConstants.VALUE_PARAM_ADVANCED_SEARCH_OWNERS_FOR_LEGAL) {
				mSearch.setAdditionalInfo("OwnersForLegal", true);
			}
			if (module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION) != TSServerInfoConstants.VALUE_PARAM_ADVANCED_SEARCH_OWNERS_FOR_LEGAL
					&& mSearch.getAdditionalInfo("OwnersForLegal") != null){
				if (mSearch.getAdditionalInfo("OwnersForLegal").equals(true)){
					getSearch().clearValidatedLinks();
					mSearch.setAdditionalInfo("OwnersForLegal", false);
				}
				
			}
		}
		
		return sr;
	}
	
	@Override
	protected void setCertificationDate() {
		try {

			if (CertificationDateManager.isCertificationDateInCache(dataSite)) {
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else {
				String html = "";
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				try {
					html = site.process(new HTTPRequest(dataSite.getLink() + "Search/EffectiveDates")).getResponseAsString();
				} catch (RuntimeException e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(site);
				}

				HtmlParser3 htmlParser3 = new HtmlParser3(html);
				NodeList nodeList = htmlParser3.getNodeList().extractAllNodesThatMatch(new RegexFilter("(?s)Last\\s+Document\\s+on\\s+Index"), true);

				Node dateNode = nodeList.elementAt(0).getParent().getNextSibling();
				String date = dateNode.toPlainTextString().replaceAll("\\s*/\\s*", "/");
				date = date.replaceAll(".*?\\s+(\\d{2}/\\d{2}/\\d{4})", "$1");

				date = DateFormatUtils.format(Util.dateParser3(date), "MM/dd/yyyy");

				CertificationDateManager.cacheCertificationDate(dataSite, date);

				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			}

		} catch (Exception e) {
			CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
		}
	}
	
	static private final class FilterAndAddressPair{
		private final GenericAddressFilter filter;

		private final AddressI addres;
		
		
		FilterAndAddressPair(GenericAddressFilter filter, AddressI addres) {
			super();
			this.filter = filter;
			this.addres = addres;
		}
		
		GenericAddressFilter getFilter() {
			return filter;
		}

		AddressI getAddres() {
			return addres;
		} 
		
		@Override
		public int hashCode() {
			String number = org.apache.commons.lang.StringUtils.defaultString(addres.getNumber());
			String name = org.apache.commons.lang.StringUtils.defaultString(addres.getStreetName());
			String unit = org.apache.commons.lang.StringUtils.defaultString(addres.getIdentifierNumber());
			String direction = org.apache.commons.lang.StringUtils.defaultString(addres.getPreDiretion());
			String postDirection = org.apache.commons.lang.StringUtils.defaultString(addres.getPostDirection());
			String suffix = org.apache.commons.lang.StringUtils.defaultString(addres.getSuffix());
			
			return (number+name+unit+direction+postDirection+suffix).hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			
			if(obj==null||!(obj instanceof FilterAndAddressPair )){
				return false;
			}
			
			FilterAndAddressPair other = (FilterAndAddressPair)obj;
			if(other.addres==null){
				return false;
			}
			
			String number = org.apache.commons.lang.StringUtils.defaultString(addres.getNumber());
			String name = org.apache.commons.lang.StringUtils.defaultString(addres.getStreetName());
			String unit = org.apache.commons.lang.StringUtils.defaultString(addres.getIdentifierNumber());
			String direction = org.apache.commons.lang.StringUtils.defaultString(addres.getPreDiretion());
			String postDirection = org.apache.commons.lang.StringUtils.defaultString(addres.getPostDirection());
			String suffix = org.apache.commons.lang.StringUtils.defaultString(addres.getSuffix());
			
			String numberO = org.apache.commons.lang.StringUtils.defaultString(other.addres.getNumber());
			String nameO = org.apache.commons.lang.StringUtils.defaultString(other.addres.getStreetName());
			String unitO = org.apache.commons.lang.StringUtils.defaultString(other.addres.getIdentifierNumber());
			String directionO = org.apache.commons.lang.StringUtils.defaultString(other.addres.getPreDiretion());
			String postDirectionO = org.apache.commons.lang.StringUtils.defaultString(other.addres.getPostDirection());
			String suffixO = org.apache.commons.lang.StringUtils.defaultString(other.addres.getSuffix());
			
			return (number.equals(numberO) && name.equals(nameO) 
					&& unit.equals(unitO) && direction.equals(directionO)
					&&postDirection.equals(postDirectionO) && suffix.equals(suffixO));
			
		}
	}
	
	static  private class AddressFilterKane extends FilterResponse{
		
		static final long serialVersionUID = -4477883665720754759L;

		Set<FilterAndAddressPair> filters = new HashSet<FilterAndAddressPair>();
		
		boolean enableUnit = true;
		boolean enableMissingUnit = false;
		
		
		AddressFilterKane(BigDecimal threshold, long searchId) {
			super("",searchId);
			this.threshold = threshold;
			strategyType = STRATEGY_TYPE_BEST_RESULTS;
			setInitAgain(true);
		}
		
		void setEnableMissingUnit(boolean b) {
			this.enableMissingUnit = b;
		}

		void setEnableUnit(boolean b) {
			this.enableUnit = b;
		} 

		@SuppressWarnings("unchecked")
		@Override
		public void init() {
			super.init();
			filters.clear();
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI manager = global.getDocManager();
			
			//add  address from search page
			GenericAddressFilter fr = createGenericAddressFilter();
			fr.init();
			filters.add(new FilterAndAddressPair(fr, sa.getAddress()));
			
			//add address from AO and TAX like documents
			try{
				manager.getAccess();
				List<DocumentI> docs = manager.getDocumentsWithType(DType.ASSESOR,DType.TAX,DType.CITYTAX);
				if(docs!=null){
					for(DocumentI doc:docs){
						Set<PropertyI> properties = doc.getProperties();
						if(properties!=null){
							for(PropertyI p:properties){
								if(p.hasAddress()){
									AddressI address =  p.getAddress();
									if(!org.apache.commons.lang.StringUtils.isBlank(address.getNumber())){
										fr = createGenericAddressFilter();
										fr.init(address.shortFormString());
										filters.add(new FilterAndAddressPair(fr, address));
									}
								}
							}
						}
					}
				}
			}finally{
				manager.releaseAccess();
			}
			
			//add address from RO documents found at look-up
			Set<AddressI> addresset = (Set<AddressI>)global.getAdditionalInfo("IL_KANE_RO_LOOK_UP_ADDRESS_DATA");
			if(addresset !=null){
				for(AddressI a:addresset){
					fr = createGenericAddressFilter();
					fr.init(a.shortFormString());
					filters.add(new FilterAndAddressPair(fr, a));
				}
			}
			
		}
		
		@Override
		public BigDecimal getScoreOneRow(ParsedResponse row) {
			
			ArrayList<Double> scoreList = new ArrayList<Double>();
			
			if(filters.size()>0){
				for(FilterAndAddressPair pair:filters){
					scoreList.add(pair.getFilter().getScoreOneRow(row).doubleValue());
				}
				
				if(allPass(scoreList,threshold.doubleValue())||allNotPass(scoreList,threshold.doubleValue())){
					return new BigDecimal(scoreList.get(0));
				}
				
			}
			
			return threshold;
		}
		
		private boolean allNotPass(ArrayList<Double> scoreList, double threshold) {
			for(double d:scoreList){
				if( d>=threshold ){
					return false;
				}
			}
			return true;
		}

		private static  boolean allPass(ArrayList<Double> scoreList, double threshold) {
			for(double d:scoreList){
				if( d<threshold ){
					return false;
				}
			}
			return true;
		}

		private GenericAddressFilter createGenericAddressFilter(){
			GenericAddressFilter fr = new GenericAddressFilter("",searchId);
			fr.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
			fr.setThreshold(this.threshold);
			fr.setEnableMissingUnit(this.enableMissingUnit);
			fr.setEnableUnit(this.enableUnit);
			return fr;
		}
		
		@Override
	    public String getFilterName(){
	    	return "Filter by Address(Generic Address Filter)";
	    }
		 
		@Override
		public String getFilterCriteria(){
	    	return enableMissingUnit ? "Addr Unit='"  +  getReferenceAddressString() + "'" : "Addr='" + getReferenceAddressString() + "'";
	    }

		private String getReferenceAddressString() {
			String str= "";
			for(FilterAndAddressPair pair:filters){
				str += pair.getAddres().shortFormString()+" | ";
			}
			if(str.endsWith(" | ")){
				str = str.substring(0, str.lastIndexOf(" | "));
			}
			return str;
		}
		
		
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if (restoreDocumentDataI == null) {
			return null;
		}
		TSServerInfoModule module = null;
		module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if (StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
		} else if (StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
		} else {
			module = null;
		}
		return module;
	}
	
	protected String getHTMLIntermediaries(String response) {
		JSONObject jsonObject;
		Object resultsObj;
		JSONArray jsonResults = new JSONArray();
		int totalRecords = 0;
		StringBuilder intermHtmlTable = new StringBuilder();
		int numberOfResults = 0;
		int resultLength = 0;

		try {
			jsonObject = (new JSONObject(response));
			if ((resultsObj = jsonObject.get("aaData")) instanceof JSONArray) {
				jsonResults = (JSONArray) resultsObj;
			}

			totalRecords = jsonObject.getInt("iTotalRecords");
			if (totalRecords == 0) {
				return "";
			}

			intermHtmlTable.append("<div id=\"resultsCount\">Number of results: " + totalRecords + "</div>");

			numberOfResults = jsonResults.length();
			JSONArray jsonResult = jsonResults.getJSONArray(0);
			resultLength = jsonResult.length();
			if (resultLength <= 5) {// for UCC search results !
				// for UCC name search: 1. doc no, (2. -only for debtor name - amendment), 3 doctype 4 name 5 address

				intermHtmlTable.append("<table id=\"intermediaries\" border=\"1\" style=\"min-width:500px;\"><tr>" +
						"<th>Document</th>" +
						(resultLength == 5 ? "<th>Amendment</th>" : "") +
						"<th>Type</th>" +
						"<th>Name</th>" +
						"<th>Address</th>" +
						"</tr>");
			} else {
				intermHtmlTable.append("<table id=\"intermediaries\" border=\"1\" style=\"min-width:500px;\"><tr>" +
						"<th>Doc Num</th>" +
						"<th>Date Filed</th>" +
						"<th>Type</th>" +
						"<th>Grantor</th>" +
						"<th>Grantee</th>" +
						"<th>Sub<br>Lot<br>Block</th>" +
						"<th>Sec<br>Twn<br>Rng</th>" +
						"<th>Assoc Doc</th>" +
						"<th>Remarks</th>" +
						"</tr>");
			}
			
			for (int i = 0; i < numberOfResults; i++) {

				jsonResult = jsonResults.getJSONArray(i);
				resultLength = jsonResult.length();
				if (resultLength <= 5) {// for UCC search results !
					String docNumber = jsonResult.getString(0).trim();
					String detailsLink = "Search/UCCDetails?docNumber=" + docNumber;

					intermHtmlTable.append("<tr>" +
							"<td><a href=\"" + detailsLink + "\">" + docNumber + "</a></td>" +
							(resultLength == 5 ? "<td>" + jsonResult.getString(1) + "</td>" : "") +
							"<td>" + jsonResult.getString((resultLength == 5 ? 2 : 1)) + "</td>" +
							"<td>" + jsonResult.getString((resultLength == 5 ? 3 : 2)) + "</td>" +
							"<td>" + jsonResult.getString((resultLength == 5 ? 4 : 3)) + "</td>" +
							"</tr>");
				} else {

					String docId = jsonResult.getString(8).trim();
					String docNumber = jsonResult.getString(9).trim();

					String detailsLink = "Search/DetailsModal?documentId=" + docId + "&docNumber=" + docNumber;

					intermHtmlTable.append("<tr>" +
							"<td><a href=\"" + detailsLink + "\">" + docNumber + "</a></td>" +
							"<td>" + jsonResult.getString(11) + "</td>" +
							"<td>" + jsonResult.getString(12) + "</td>" +
							"<td>" + jsonResult.getString(13) + "</td>" +
							"<td>" + jsonResult.getString(14) + "</td>" +
							"<td>" + jsonResult.getString(15) + "</td>" +
							"<td>" + jsonResult.getString(16).replaceAll("(?is)\\|[A-Z]*", "") + "</td>" +
							"<td>" + jsonResult.getString(17) + "</td>" +
							"<td>" + jsonResult.getString(18) + "</td>" +
							"</tr>");
				}
			}
				intermHtmlTable.append("</table>");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return intermHtmlTable.toString();
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse Response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			table = table.replaceAll("(?is)\\s*\\bnull\\b\\s*", "");// remove "null" strings
			
			
			int numberOfUncheckedElements = 0;
			HtmlParser3 htmlParser3 = new HtmlParser3(table);
			Node maxResultsNode = htmlParser3.getNodeById("maxResultsMessage");
			String header = "";
			Node mainTable = htmlParser3.getNodeById("intermediaries");
			TableTag tableTag = (TableTag) mainTable;
			TableRow[] rows = tableTag.getRows();

			if (rows[0] != null) {
				header = rows[0].toHtml().replaceAll("</?tr>", "").trim();
			}

			for (int i = 1; i < rows.length; i++) {

				boolean isAlreadySaved = false;
				TableRow row = rows[i];
				
				
				
				LinkTag linkTag = (LinkTag) row.getColumns()[0].getFirstChild();
				String link = linkTag.extractLink().trim().replaceAll("\\s", "%20");
				link = CreatePartialLink(TSConnectionURL.idGET) + link;
				linkTag.setLink(link);

				String htmlRow = row.toHtml();
				if (row.getColumnCount() <= 5) {
					htmlRow = htmlRow.replaceAll("(?s)\\s*,\\s*,\\s*", "");// remove empty addresses(UCC intermediaries)
				}
				ParsedResponse currentResponse = new ParsedResponse();
				int action = TSServer.REQUEST_SAVE_TO_TSD;
				ResultMap m = ro.cst.tsearch.servers.functions.ILKaneRO.parseintermediaryResultsILKaneRO(htmlRow, searchId);
				m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

				// checkboxes

				String checkBox = "checked";
				String instrNo = StringUtils.transformNull((String) m.get(SaleDataSetKey.INSTRUMENT_NUMBER
						.getKeyName())).trim();
				String recDate = StringUtils.transformNull((String) m.get(SaleDataSetKey.RECORDED_DATE
						.getKeyName())).trim();
				HashMap<String, String> data = new HashMap<String, String>();

				if (recDate.length() >= 4) {
					String year = recDate.substring(recDate.length() - 4, recDate.length());
					data.put("year", year);
				}

				data.put("instrno", instrNo);
				// doctype is just a code for first 11 modules; for UCC modules comes as full doc type
				String docType = StringUtils.transformNull((String) m.get(SaleDataSetKey.DOCUMENT_TYPE
						.getKeyName())).trim();
				if (!docType.isEmpty()) {
					data.put("type", docType);
				}
				if (isInstrumentSaved(instrNo, null, data, false)) {
					isAlreadySaved = true;
					checkBox = "saved";
				} else {
					numberOfUncheckedElements++;
					checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>";
				}

				htmlRow = "<tr><td align=\"center\">" + checkBox + "</td>" + htmlRow.replaceAll("(?is)</?tr[^>]*>", "") + "</tr>";
				Bridge bridge = new Bridge(currentResponse, m, searchId);

				RegisterDocumentI document = (RegisterDocumentI) bridge.importData();
				currentResponse.setDocument(document);

				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
				currentResponse.setOnlyResponse(htmlRow);
				if (!isAlreadySaved) {
					currentResponse.setPageLink(new LinkInPage(link, link, action));
				}
				intermediaryResponse.add(currentResponse);
			}
			ParsedResponse parsedResponse = Response.getParsedResponse();
			parsedResponse.setHeader((maxResultsNode == null ? "" : maxResultsNode.toHtml()) +
					"<table border=\"1\" style=\"min-width:500px;\"><tr >"
					+ "<th>" + SELECT_ALL_CHECKBOXES + "</th>" + header + "</tr>");
			parsedResponse.setFooter("</table>");

			outputTable.append(table);
			SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}
		return intermediaryResponse;
	}
	
	/**
	 * treat the case in which the user clicked on an image link, and download it only once
	 */
	@Override
	public ServerResponse GetLink(String link, boolean vbEncoded) throws ServerResponseException {
		try {
			if (link.contains("/Search/GetDocumentImage") || link.contains("/Search/GetDocPathSubCode")
					|| link.contains("/Search/EntryBookSearch")) {
				if (link.contains("/Search/GetDocPathSubCode")) {
					int index = link.indexOf("Link=");
					String url = link;
					if (index != -1) {
						url = link.substring(link.indexOf("Link=") + 5);
					}

					String imageLinkContents = getLinkContents(url);
					JSONObject jsonObject;

					jsonObject = (new JSONObject(imageLinkContents));

					String documentPath = jsonObject.getString("DocumentPath").trim();
					String docNumber = jsonObject.getString("DocumentNumber").trim();
					try {
						link = link.substring(0, link.indexOf("Link=") + 5) + dataSite.getLink() + "/Search/GetDocumentImage?documentPath="
								+ URLEncoder.encode(documentPath, "UTF-8") + "&docNumber=" + docNumber;

					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}

				}

				String docNumber = StringUtils.extractParameterFromUrl(link, "docNumber");
				docNumber = docNumber.trim();
				// construct fileName
				String folderName = getCrtSearchDir() + "Register" + File.separator;
				new File(folderName).mkdirs();
				String fileName = folderName + docNumber + ".tif";

				// retrieve the image
				retrieveImage(link, fileName, searchId);

				// write the image to the client web-browser
				boolean imageOK = writeImageToClient(fileName, "image/tiff");

				// image not retrieved
				if (!imageOK) {
					// return error message
					ParsedResponse pr = new ParsedResponse();
					pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
					throw new ServerResponseException(pr);
				}

				// return solved response
				return ServerResponse.createSolvedResponse();
			} else {
				return super.GetLink(link, vbEncoded);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return null;
	}

	public boolean retrieveImage(String imageLink, String fileName, long searchId) {
		byte[] imageBytes = null;

		HttpSite site = HttpManager.getSite("ILKaneRO", searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http2.ILKaneRO) site).getImage(imageLink);
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
	protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {

		String link = image.getLink(0);
		link = link.replaceFirst(dataSite.getLink(), "");
		String documentPath = StringUtils.extractParameterFromUrl(link, "documentPath");
		try {// some may be already encoded
			documentPath = URLDecoder.decode(documentPath, "UTF-8");
			documentPath = URLEncoder.encode(documentPath, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		link = dataSite.getLink() + link.replaceFirst("(documentPath=).*?(&|$)", "$1" + documentPath + "$2");
		byte[] imageBytes = null;

		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http2.ILKaneRO) site).getImage(link);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}

		ServerResponse resp = new ServerResponse();

		if (imageBytes != null) {
			afterDownloadImage(true);
		} else {
			return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
		}

		String imageName = image.getPath();
		if (ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			imageBytes = ro.cst.tsearch.utils.FileUtils.readBinaryFile(imageName);
			return new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType());
		}

		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType()));

		if (!ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), image.getPath());
		}

		DownloadImageResult dres = resp.getImageResult();

		return dres;
	}
	
	public class InstrumentIterator extends InstrumentGenericIterator {

		private static final long	serialVersionUID	= 1L;

		public InstrumentIterator(long searchId) {
			super(searchId);
		}

		@Override
		public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
			String instrNo = state.getInstno().replaceFirst("^0+", "").trim();
			String result = "";
			if (instrNo.matches("\\d{2,}K\\d{3,}")) {
				result = instrNo;
			} else {
				int year = state.getYear();
				result = String.valueOf(year) + "K" + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
			}
			return result;
		}
	}
	
	private String processSubdBlockLotResponse(String sAction, String rsResponse, String linkStart, String extraSearchParams) {

		String response = "";
		if (sAction.indexOf("/Search/GetBlocks") != -1) {// get blocks
			JSONArray jsonResults = new JSONArray();
			try {

				jsonResults = (new JSONArray(rsResponse));
				for (int i = 0; i < jsonResults.length(); i++) {
					String block = jsonResults.getString(i);
					// {"Selected":false,"Text":"","Value":""}
					response += "<option id=\""
							+ block.replaceFirst("(?is).*?\\bValue\\s*\"\\s*:\\s*\"([^\"]*)\".*", "$1") + "\">"
							+ block.replaceFirst("(?is).*?\\bText\\s*\"\\s*:\\s*\"([^\"]*)\".*", "$1") + "</option>";
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			// http://lrs.kanecountyrecorder.net/Search/GetLots?code=RACCOND+8&block=NONE
			String subdivisionCode = StringUtils.extractParameterFromUrl(sAction, "code");
			String getLots = "<script type=\"text/javascript\"> function getLots()"
					+ "{var sel = document.getElementById('blocksSelect'); "
					+ "var val = sel.selectedIndex; "
					+ "var block = sel.options[val].id; "
					+ "var url = \"" + linkStart + "/Search/GetLots?code=" + subdivisionCode + "&block=\"+block+\"" + extraSearchParams + "\";"
					+ "window.location.href = url;}</script>";

			try {
				subdivisionCode = URLDecoder.decode(subdivisionCode, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			response = getLots
					+ "<div align=\"center\"><strong>Subdivision Code: " + subdivisionCode + "</strong></div>"
					+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
					+ "<tr><td><strong>Block</strong></td></tr><tr><td>"
					+ "<select id=\"blocksSelect\" style=\"min-width:200px;\""
					+ "size=\"30\" onchange=\"getLots()\">"
					+ response + "</select></td></tr></table>";

		} else if (sAction.indexOf("/Search/GetLots") != -1) {// get lots
			JSONArray jsonResults = new JSONArray();
			try {

				jsonResults = (new JSONArray(rsResponse));
				for (int i = 0; i < jsonResults.length(); i++) {
					String lot = jsonResults.getString(i);
					// {"Selected":false,"Text":"1","Value":"1"}
					response += "<option id=\""
							+ lot.replaceFirst("(?is).*?\\bValue\\s*\"\\s*:\\s*\"([^\"]*)\".*", "$1") + "\">"
							+ lot.replaceFirst("(?is).*?\\bText\\s*\"\\s*:\\s*\"([^\"]*)\".*", "$1") + "</option>";
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			String subdivisionCode = StringUtils.extractParameterFromUrl(sAction, "code");
			String block = StringUtils.extractParameterFromUrl(this.msLastLink, "block");
			// HTTP POST : ?oSubdivision.SubdivisionName=RANDSQSC3&subSearchType=0&oSubdivision.BlockName=NONE&oSubdivision.LotName=1
			// http://lrs.kanecountyrecorder.net/Search/Criteria?criteria=Subdivision
			try {
				subdivisionCode = URLDecoder.decode(subdivisionCode, "UTF-8");
				block = URLDecoder.decode(block, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			String getSubdivisionModule = "<script type=\"text/javascript\">"
					+ "function getSubdivisionModule() "
					+ "{"

					+ "var sel = document.getElementById('lotsSelect');"
					+ "var val = sel.selectedIndex;"
					+ "var lot = sel.options[val].value;"
					+ "var subdivision = \"" + subdivisionCode + "\";"
					+ "var block = \"" + block + "\";"
					// search type is only 'by code"(value is 0)' in this module - subdivision name cannot be typed;
					// to type subd name use module: Search By Subdivision/Block/Lot for Automatic - 2854:
					+ "var searchType = \"0\";"
					+ "var path =  \"" + CreatePartialLink(TSConnectionURL.idPOST) + "/Search/Criteria?criteria=Subdivision"
					+ "&oSubdivision.LotName=\"+lot+\"&oSubdivision.BlockName=\"+block+\"&oSubdivision.SubdivisionName=\"+"
					+ "subdivision+\"&subSearchType=\"+searchType+\"" + extraSearchParams + "\";"
					+ "var form = document.createElement(\"form\");"
					+ "form.setAttribute(\"method\", \"post\");"
					+ "form.setAttribute(\"action\", path);"
					+ "document.body.appendChild(form);"
					+ "form.submit();"
					+ "}"
					+ "</script>";

			response = getSubdivisionModule
					+ "<div align=\"center\"><strong>Subdivision Code: " + subdivisionCode + "</strong></div>"
					+ "<div align=\"center\"><strong>Block: " + block + "</strong></div>"
					+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
					+ "<tr><td><strong>Lot</strong></td></tr><tr><td>"
					+ "<select id=\"lotsSelect\" style=\"min-width:200px;\""
					+ "size=\"30\" onchange=\"getSubdivisionModule()\">"
					+ response + "</select></td></tr></table>";

		}
		else {
			String getBlocks = "<script type=\"text/javascript\"> function getBlocks()"
					+ "{var sel = document.getElementById('subdivisionsSelect'); "
					+ "var val = sel.selectedIndex; "
					+ "var subdivision = sel.options[val].id; "
					+ "var url = \"" + linkStart + "/Search/GetBlocks?code=\"+ subdivision+\"" + extraSearchParams + "\";"
					+ "if (subdivision == \"\" ){"
					+ "alert(\"Please choose a subdivision! \")}"
					+ "else {window.location.href = url;  }}</script>";

			// http://lrs.kanecountyrecorder.net/Search/GetBlocks?code=RACCOND+8

			response = rsResponse.replaceAll("(?is)<a[^>]*\\bid=\"([^\"]*)\"[^>]*>\\s*(.*?)\\s*</a>\\s*<br[^>]*>", "<option id=\"$1\">$2</option>");
			response = getBlocks
					+ "<table border=\"1\" align=\"center\" style=\"min-width:200px;\">"
					+ "<tr><td><strong>Subdivision</strong></td></tr><tr><td>"
					+ "<select id=\"subdivisionsSelect\" style=\"min-width:200px;\""
					+ "size=\"30\" onchange=\"getBlocks()\">"
					+ response + "</select></td></tr></table>";
		}
		return response;
	}
}

package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.BridgeConn;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.fsearch.FileSearch;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.GranteeDoctypeFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class TNShelbyRO extends TSServer implements TSServerROLikeI {

	static final long serialVersionUID = 10000000;

	private static final int ID_SEARCH_BY_LOCAL = 22; // used in ParseResponse
	// to identify what
	// parse algorithm to
	// apply
	private static final String HOST = "http://shelby.bisonline.com";
	private static final String LOCAL_SEARCH_ACTION = "local_search_action";

	private final String IMAGES_SITE = "shelby.bisonline.com/";
	private final String IMAGES_SITE_IP = "216.184.67.243";
	private final int ID_DETAILS1 = 20; // used in ParseResponse to identify
	// what parse algorithm to apply
	private final int ID_DETAILS2 = 21; // used in ParseResponse to identify
	// what parse algorithm to apply

	private static Map<String, String>	days = new HashMap<String, String>();
	private boolean downloadingForSave = false;
	private String msLastdb = "1";

	static SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
	private static final Pattern certDatePattern = Pattern.compile("(?ism)<b>Date</b>.*?<b>(.*?)</b>");
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		Search search = getSearch();
		int searchType = search.getSearchType();
		if(searchType == Search.AUTOMATIC_SEARCH) {
			TSServerInfoModule m = null;
	
			SearchAttributes sa = search.getSa();
			DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
			DocsValidator pinValidator = getPinFilter().getValidator();
			DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
			DocsValidator lastTransferDateValidator = (new LastTransferDateFilter(searchId)).getValidator();
			DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
			DocsValidator subdivisionNameValidator = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId).getValidator();
			boolean validateWithDates = sa.isUpdate() || sa.isDateDown();
			
			// instrument list search from AO
			InstrumentGenericIterator instrumentIterator = new InstrumentGenericIterator(searchId, getDataSite());
			instrumentIterator.setLoadFromRoLike(false);
			instrumentIterator.enableInstrumentNumber();
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_DOCNO);
			m.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			m.addIterator(instrumentIterator);
			if (validateWithDates) {
				m.addValidator(recordedDateValidator);
			}
			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			//m.addCrossRefValidator(pinValidator);			//a reference to a safe document is safe
			m.addCrossRefValidator(recordedDateValidator);
	
			l.add(m);
	
			// book page list search from AO/Tax
			InstrumentGenericIterator bookPageIterator = new InstrumentGenericIterator(searchId, getDataSite());
			bookPageIterator.setLoadFromRoLike(false);
			bookPageIterator.enableBookPage();
			m = new TSServerInfoModule(serverInfo .getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
			m.clearSaKeys();
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			m.addIterator(bookPageIterator);
			if (validateWithDates) {
				m.addValidator(recordedDateValidator);
			}
			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			//m.addCrossRefValidator(pinValidator);		//a reference to a safe document is safe
			m.addCrossRefValidator(recordedDateValidator);
			l.add(m);
	
			// search by parcel ID
			if (hasPin()) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				m.clearSaKey(1);
				m.clearSaKey(4);
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(recordedDateValidator);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				l.add(m);
			}
	
			String streetNo = sa.getAtribute(SearchAttributes.P_STREETNO).trim();
			boolean emptyNumber = "".equals(streetNo) || "0".equals(streetNo);
			// search by address
			if (hasStreet() && !emptyNumber) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_DEFAULT_NOT_EMPTY);
				m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_DEFAULT_EMPTY);
				m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_DEFAULT_EMPTY);
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(recordedDateValidator);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
	
				l.add(m);
			}
	
			ConfigurableNameIterator nameIterator = null;
			
			// search by name
			if (hasOwner()) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
				DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
				m.setSaKey(7, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				m.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
				
				
				GenericNameFilter nameFilterOwner = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT,
						searchId, m);
				nameFilterOwner.setUseSynonymsForCandidates(true);
				nameFilterOwner.setEnableTrusteeCheck(true);
				nameFilterOwner.setEnableTrusteeDoctype(false);
				
				m.addFilter(nameFilterOwner);
				
				GranteeDoctypeFilterResponse granteeDoctypeFilterResponse = new GranteeDoctypeFilterResponse(searchId);
				granteeDoctypeFilterResponse.setUseSynonymsForCandidates(true);
				granteeDoctypeFilterResponse.setEnableTrusteeCheck(true);
				granteeDoctypeFilterResponse.setEnableTrusteeDoctype(false);
				
				
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(granteeDoctypeFilterResponse.getValidator());
				m.addValidator(lastTransferDateValidator);
				m.addValidator(recordedDateNameValidator);
				addFilterForUpdate(m, false);
				
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateNameValidator);
				
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(m, searchId, new String[] { "L;F;" });
				
				m.addIterator(nameIterator);
				
				l.add(m);
			}
	
			// special search by parcel id (tampenia cu % ...)
			/*
			 * vezi cautarea cu Subd Name=NORTHAVEN, lot=15 cand se aduc 500 de
			 * documente !!!
			 */
			if (searchWithSubdivision()) {
				m = new TSServerInfoModule(serverInfo
						.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				m.clearSaKey(0);
				m.setIteratorType(1,
						FunctionStatesIterator.ITERATOR_TYPE_LOT_INTERVAL);
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(lastTransferDateValidator);
				m.addValidator(recordedDateValidator);
				m.setIteratorType(ModuleStatesIterator.TYPE_DEFAULT);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				l.add(m);
	
				if (!hasBookAndPage()) {
					m = new TSServerInfoModule(serverInfo
							.getModule(TSServerInfo.NAME_MODULE_IDX));
					m
							.addExtraInformation(
									TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
									TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_PLAT);
					m.clearSaKeys();
					m.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
					m.getFunction(4).setData("PLAT");
					m.addValidator(defaultLegalValidator);
					// 3061 m.addValidator( addressHighPassValidator );
					m.addValidator(pinValidator);
					m.addValidator(recordedDateValidator);
					m.addValidator(subdivisionNameValidator);
					FilterResponse sectionFilter = new GenericLegal("", searchId);
					((GenericLegal) sectionFilter).disableAll();
					((GenericLegal) sectionFilter).setEnableSection(true);
					sectionFilter.setThreshold(new BigDecimal("0.7"));
	
					m.addFilter(sectionFilter);
					m.addCrossRefValidator(defaultLegalValidator);
					// 3061 m.addCrossRefValidator( addressHighPassValidator );
					m.addCrossRefValidator(pinValidator);
					m.addCrossRefValidator(recordedDateValidator);
					m.addCrossRefValidator(subdivisionNameValidator);
					l.add(m);
				}
	
				m = new TSServerInfoModule(serverInfo
						.getModule(TSServerInfo.NAME_MODULE_IDX));
				m
						.addExtraInformation(
								TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
								TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_EASEMENT);
				m.clearSaKeys();
				m.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
				m.getFunction(4).setData("ESMT");
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(recordedDateValidator);
				m.addValidator(subdivisionNameValidator);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				m.addCrossRefValidator(subdivisionNameValidator);
				l.add(m);
	
				m = new TSServerInfoModule(serverInfo
						.getModule(TSServerInfo.NAME_MODULE_IDX));
				m
						.addExtraInformation(
								TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
								TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_RESTRICTION);
				m.clearSaKeys();
				m.getFunction(0).setSaKey(SearchAttributes.LD_SUBDIV_NAME);
				m.getFunction(4).setData("SRES");
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(recordedDateValidator);
				m.addValidator(subdivisionNameValidator);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				m.addCrossRefValidator(subdivisionNameValidator);
				l.add(m);
			} else {
				printSubdivisionException();
			}
	
			// BUYER SEARCH
			if (sa.hasBuyer()) {
				
				DocTypeSimpleFilter doctypeFilter = (DocTypeSimpleFilter) DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
				String doctypes[] = { 
						DocumentTypes.MISCELLANEOUS, 
						DocumentTypes.LIEN, 
						DocumentTypes.COURT, 
						DocumentTypes.MORTGAGE, 
						DocumentTypes.AFFIDAVIT,
						DocumentTypes.RELEASE };
				doctypeFilter.setDocTypes(doctypes);
				
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
				DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
				m.setSaKey(7, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				m.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
				GenericNameFilter nameFilterOwner = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT,
						searchId, m);
				nameFilterOwner.setUseSynonymsForCandidates(true);
				nameFilterOwner.setEnableTrusteeCheck(true);
				nameFilterOwner.setEnableTrusteeDoctype(false);
				m.addFilter(nameFilterOwner);
				m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
				m.addValidator(doctypeFilter.getValidator());
				if (validateWithDates) {
					m.addValidator(recordedDateNameValidator);
				}
				addFilterForUpdate(m, false);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateNameValidator);
				m.addIterator(ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L;F;" }));
				l.add(m);
			}
	
			// search by crossRef book and page list from RO docs
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
			m.clearSaKeys();
			bookPageIterator = new InstrumentGenericIterator(searchId, getDataSite());
			bookPageIterator.setLoadFromRoLike(true);
			bookPageIterator.enableBookPage();
//			m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_NOT_AGAIN);
			m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			m.addValidator(defaultLegalValidator);
			m.addValidator(addressHighPassValidator);
			m.addValidator(pinValidator);
			m.addValidator(recordedDateValidator);
			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
			m.addCrossRefValidator(recordedDateValidator);
			l.add(m);
	
			// search by crossRef instr# list from RO docs
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
			instrumentIterator = new InstrumentGenericIterator(searchId, getDataSite());
			instrumentIterator.setLoadFromRoLike(true);
			instrumentIterator.enableInstrumentNumber();
			m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			m.addValidator(defaultLegalValidator);
			m.addValidator(addressHighPassValidator);
			m.addValidator(pinValidator);
			m.addValidator(recordedDateValidator);
			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
			m.addCrossRefValidator(recordedDateValidator);
			l.add(m);
	
			// OCR last transfer - instrument search
			m = new TSServerInfoModule(serverInfo
					.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			m.addExtraInformation(
					TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_FAKE_DOCUMENT, Boolean.TRUE);
			m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			m.getFunction(0).setSaKey("");
			m.getFunction(0).setIteratorType(
					FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
			m.getFunction(1).setParamValue("INUM");
			m.getFunction(2).setParamValue("execute search");
			if (validateWithDates) {
				m.addValidator(recordedDateValidator);
			}
			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
			m.addCrossRefValidator(recordedDateValidator);
			l.add(m);
	
			// OCR last transfer - book / page search or JUST bootstraping the last
			// transfer
			m = new TSServerInfoModule(serverInfo
					.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			m.addExtraInformation(
					TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_FAKE_DOCUMENT, Boolean.TRUE);
			m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			m.clearSaKey(0);
			m.clearSaKey(1);
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
			m.getFunction(3).setParamValue("BP");
			m.getFunction(4).setParamValue("execute search");
			if (validateWithDates) {
				m.addValidator(recordedDateValidator);
			}
			m.addValidator(defaultLegalValidator);
			m.addValidator(pinValidator);
			m.addCrossRefValidator(defaultLegalValidator);
			m.addCrossRefValidator(addressHighPassValidator);
			m.addCrossRefValidator(pinValidator);
			m.addCrossRefValidator(recordedDateValidator);
			l.add(m);
	
			{
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
				DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
				m.setSaKey(7, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				m.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
				
				GenericNameFilter nameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
				nameFilter.setEnableTrusteeCheck(true);
				nameFilter.setEnableTrusteeDoctype(false);
				nameFilter.setInitAgain(true);
				nameFilter.setUseSynonymsForCandidates(true);
				
				
				GranteeDoctypeFilterResponse granteeDoctypeFilterResponse = new GranteeDoctypeFilterResponse(searchId);
				granteeDoctypeFilterResponse.setUseSynonymsForCandidates(true);
				granteeDoctypeFilterResponse.setEnableTrusteeCheck(true);
				granteeDoctypeFilterResponse.setEnableTrusteeDoctype(false);
				
				m.addFilter(nameFilter);
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(granteeDoctypeFilterResponse.getValidator());
				m.addValidator(lastTransferDateValidator);
				m.addValidator(recordedDateNameValidator);
				m.setStopAfterModule(true);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateNameValidator);
		
				ArrayList<NameI> searchedNames = null;
				if (nameIterator != null) {
					searchedNames = nameIterator.getSearchedNames();
				} else {
					searchedNames = new ArrayList<NameI>();
				}
		
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(m, searchId, false, new String[] { "L;F;" });
				nameIterator.setInitAgain(true);
				nameIterator.setSearchedNames(searchedNames);
				m.addIterator(nameIterator);
				l.add(m);
			}

		}
		serverInfo.setModulesForAutoSearch(l);
	}

	@Override
	public void processOcredInstrument(InstrumentI in, boolean ocrReportedBookPage) {
		if (!ocrReportedBookPage){
			String inst = org.apache.commons.lang.StringUtils.defaultString(in.getInstno());
			if(inst.contains("-")){				
				inst = inst.replaceAll("[-]+", "");
				
				in.setInstno(inst);
			}
		}
		
	}
	
	public PinFilterResponse getPinFilter() {
		PinFilterResponse filter = new PinFilterResponse(SearchAttributes.LD_PARCELNO, searchId) {
			private static final long serialVersionUID = 1L;
			@Override
			protected Set<String> getRefPin() {
				if(parcelNumber!=null){
					return parcelNumber;
				}
				Set<String> ret = new HashSet<String>();
				ret.addAll(Arrays.asList(sa.getAtribute(saKey).trim().split(",")));
				
				DocumentsManagerI documentsManagerI = getSearch().getDocManager();
				try {
					documentsManagerI.getAccess();
					List<DocumentI> assesorDocuments = documentsManagerI.getDocumentsWithType(DType.ASSESOR);
					for (DocumentI documentI : assesorDocuments) {
						for(RegisterDocumentI ref : documentI.getReferences()) {
							InstrumentI cloneRef = ref.getInstrument().clone();
							cloneRef.setDocSubType(DocumentTypes.MISCELLANEOUS);
							List<DocumentI> documentsWithInstrumentsFlexible = documentsManagerI.getDocumentsWithInstrumentsFlexible(false, cloneRef);
							
							DocumentI savedDocument = documentsWithInstrumentsFlexible.isEmpty()?null:documentsWithInstrumentsFlexible.get(0);
							if(savedDocument != null) {
								Set<PropertyI> properties = savedDocument.getProperties();
								if(properties != null && !properties.isEmpty()) {
									for (PropertyI propertyI : properties) {
										PinI pin = propertyI.getPin();
										if(pin != null && pin.getPin(PinType.PID) != null) {
											ret.add(pin.getPin(PinType.PID));
										}
									}
								}
							}
						}
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					documentsManagerI.releaseAccess();
				}
				parcelNumber = ret;
				return ret;
			}
			
		};
		filter.setStartWith(true);
		filter.setIgNoreZeroes(true);
		return filter;
	}

	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory
				.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = getPinFilter().getValidator();
		DocsValidator addressHighPassValidator = AddressFilterFactory
				.getAddressHighPassFilter(searchId, 0.8d).getValidator();

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(
				searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa
				.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {
			module = new TSServerInfoModule(serverInfo
					.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			GenericNameFilter nameFilterGrantor = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(
					SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
			nameFilterGrantor.setEnableTrusteeCheck(true);
			nameFilterGrantor.setEnableTrusteeDoctype(false);
			module.addFilter(nameFilterGrantor);
			
			String date = gbm.getDateForSearch(id, "MMddyyyy", searchId);
			if (date != null)
				module.getFunction(7).forceValue(date);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;" });
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
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				date = gbm
						.getDateForSearchBrokenChain(id, "MMddyyyy", searchId);
				if (date != null)
					module.getFunction(7).forceValue(date);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId,
								new String[] { "L;F;" });
				module.addIterator(nameIterator);
				module.addValidator(defaultLegalValidator);
				module.addValidator(addressHighPassValidator);
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

	public TNShelbyRO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
		super.setGoBackOneLevel(true);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public ServerResponse GetLink(String vsRequest, boolean vbEncoded)
			throws ServerResponseException {
		ServerResponse rtrnResponse = new ServerResponse();

		String sTmp = TSServer.getParameter(msPrmNameLink, vsRequest);

		RawResponseWrapper rrw = FileSearch.getFile(sTmp);

		if (rrw != null) {
			// logger.debug("sTmp =" + sTmp);
			solveResponse(LOCAL_SEARCH_ACTION, ID_GET_LINK, "GetLink",
					rtrnResponse, rrw, null);
		} else if (sTmp.indexOf(".php") == -1) {
			// try to get the file
			// request file
			getTSConnection().setHostName(IMAGES_SITE);
			getTSConnection().setHostIP(IMAGES_SITE_IP);
			rtrnResponse = super.GetLink(vsRequest, vbEncoded);
			getTSConnection().setHostName(msiServerInfo.getServerAddress());
			getTSConnection().setHostIP(msiServerInfo.getServerIP());

		} else {

			ServerResponse rtnResponse;
			if (vsRequest.indexOf("imgView") != -1
					&& TSServer.getParameter("db", vsRequest) != null) {

				synchronized (HOST) {

					String link = vsRequest
							.substring(vsRequest.indexOf("Link") + 5);
					vsRequest = vsRequest.substring(0, vsRequest
							.indexOf("Link"));

					String instnum = TSServer.getParameter("id", link);

					// search with instnum
					link = "http://shelby.bisonline.com/p5.php";

					HashMap<String, String> reqprops = new HashMap<String, String>();
					reqprops.put("Referer",
							"http://shelby.bisonline.com/instnum.html");
					reqprops
							.put(
									"Accept",
									"image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
					reqprops.put("Content-Type",
							"application/x-www-form-urlencoded");
					reqprops.put("Host", "shelby.bisonline.com");
					reqprops
							.put("User-Agent",
									"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.1.4322)");

					HTTPRequest req = new HTTPRequest(link);
					req.setMethod(HTTPRequest.POST);
					req.setHeaders(reqprops);
					req.setPostParameter("searchtype", "INUM");
					req.setPostParameter("search", "execute search");

					if (instnum.length() - 4 > 0) {
						req.setPostParameter("instnum", instnum.substring(0,
								instnum.length() - 4));
					}

					HTTPResponse res;
					HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
					try {
						res = site.process(req);
						/*res = HTTPSiteManager.pairHTTPSiteForTSServer(
								"TNShelbyRO", searchId, miServerID)
								.process(req);*/
						res.getResponseAsString();

					} catch (Exception e) {

						/*HTTPSiteManager.pairHTTPSiteForTSServer("TNShelbyRO",
								searchId, miServerID).destroySession();*/

						ServerResponse Response = new ServerResponse();
						Response
								.getParsedResponse()
								.setError(
										"Too many queries per time unit. Please try again later to download image.");

						throw new ServerResponseException(Response);
					} finally {
						HttpManager.releaseSite(site);
					}

					// wait
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					vsRequest += "Link=" + sTmp + "&id=" + instnum;

					// redirect to normal
					rtnResponse = super.GetLink(vsRequest, vbEncoded);

					// wait, don't panic
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				rtrnResponse = rtnResponse;
			} else
				rtrnResponse = super.GetLink(vsRequest, vbEncoded);
		}

		return rtrnResponse;
	}

	public ServerResponse FollowLink(String vsRequest, String imagePath)
			throws ServerResponseException {
		String sTmp = TSServer.getParameter(msPrmNameLink, vsRequest);

		RawResponseWrapper rrw = FileSearch.getFile(sTmp);

		if (rrw != null) {
			ServerResponse rtrnResponse = new ServerResponse();
			solveResponse(LOCAL_SEARCH_ACTION, ID_GET_LINK, "FollowLink",
					rtrnResponse, rrw, null);
			return rtrnResponse;

		} else {

			ServerResponse rtnResponse;
			if (vsRequest.indexOf("imgView") != -1) {

				synchronized (HOST) {
					String link = vsRequest
							.substring(vsRequest.indexOf("Link") + 5);
					vsRequest = vsRequest.substring(0, vsRequest
							.indexOf("Link"));

					String instnum = TSServer.getParameter("id", link);

					// search with instnum
					link = "http://shelby.bisonline.com/p5.php";

					HashMap<String, String> reqprops = new HashMap<String, String>();
					reqprops.put("Referer",
							"http://shelby.bisonline.com/instnum.html");
					reqprops
							.put(
									"Accept",
									"image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
					reqprops.put("Content-Type",
							"application/x-www-form-urlencoded");
					reqprops.put("Host", "shelby.bisonline.com");
					reqprops
							.put("User-Agent",
									"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.1.4322)");

					HTTPRequest req = new HTTPRequest(link);
					req.setMethod(HTTPRequest.POST);
					req.setHeaders(reqprops);
					req.setPostParameter("searchtype", "INUM");
					req.setPostParameter("search", "execute search");
					req.setPostParameter("instnum", instnum.substring(0,
							instnum.length() - 4));

					HTTPResponse res;
					HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
					try {
						
						res = site.process(req);
						/*res = HTTPSiteManager.pairHTTPSiteForTSServer(
								"TNShelbyRO", searchId, miServerID)
								.process(req);*/
						res.getResponseAsString();

					} catch (Exception e) {

						ServerResponse Response = new ServerResponse();
						Response
								.getParsedResponse()
								.setError(
										"Too many queries per time unit. Please try again later to download image..");

						throw new ServerResponseException(Response);
					} finally {
						HttpManager.releaseSite(site);
					}

					// wait
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					vsRequest += "Link=" + sTmp + "&id=" + instnum;

					// redirect to normal
					rtnResponse = super.FollowLink(vsRequest, imagePath);

					// wait
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				return rtnResponse;
			}

			return super.FollowLink(vsRequest, imagePath);
		}
	}

	@Override
	public ServerResponse simpleSaveToTSD(String vsRequest,
			boolean checkForDocType, Map<String, Object> extraParams) throws ServerResponseException {

		// logger.debug("vsRequest in simpleSaveToTSD =" + vsRequest);
		String link = TSServer.getParameter(msPrmNameLink, vsRequest);
		// logger.debug("sTmp in save =" + link);

		SaleDataSet sds = new SaleDataSet();
		sds.setAtribute("Grantor", mSearch.getSa().getAtribute(
				SearchAttributes.LD_SUBDIVISION));

		RawResponseWrapper rrw = FileSearch.getFile(link, sds);
		if (rrw != null) {
			// logger.debug("local save =" + link);
			ServerResponse Response = new ServerResponse();
			solveResponse(LOCAL_SEARCH_ACTION, ID_SAVE_TO_TSD, "SaveToTSD",
					Response, rrw, null);

			return Response;
		} else {
			return super.simpleSaveToTSD(vsRequest, false, extraParams);
		}

	}

	public ServerResponse SearchBy(TSServerInfoModule module, Object sd)
			throws ServerResponseException {
		String instrNo = "";
		String bookNo = "";
		String pageNo = "";
		String link = "";

		
		int ServerInfoModuleID = module.getModuleIdx();
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(
				searchId).getCrtSearchContext().getSa();
		
		Date start = sa.getStartDate();
		Date end = sa.getEndDate();
		
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");

		if (ServerInfoModuleID == TSServerInfo.MODULE_IDX40) {// Search by Instrument Type - All: get current / prev day param
			String moduleDayParamName = module.getFunction(1).getParamValue().trim();
			String today = format.format(Calendar.getInstance().getTime());

			String dayParamSetDate = org.apache.commons.lang.StringUtils.defaultString(days.get(moduleDayParamName + "SetDate"));
			String moduleDayParamValue = days.get(moduleDayParamName);

			if (moduleDayParamValue == null || (!dayParamSetDate.equals(today))) {// renew these parameters if they weren't set today
				String instrTypeAllForm = getLinkContents(dataSite.getLink());
				HtmlParser3 htmlParser3 = new HtmlParser3(instrTypeAllForm);
				Node instTypeAllTBL = htmlParser3.getNodeById("InstTypeAllTBL");

				if (instTypeAllTBL != null) {
					days.put(moduleDayParamName, parseDateParam(instTypeAllTBL.toHtml(), moduleDayParamName).trim());
					days.put(moduleDayParamName + "SetDate", today);
				}

				moduleDayParamValue = days.get(moduleDayParamName);
			}

			if (moduleDayParamValue == null) {//fail-safe case
				if (moduleDayParamName.equals("CurrentDay")) {
					moduleDayParamValue = today;
				} else if (moduleDayParamName.equals("PreviousDay")) {
					Calendar previousDayCal = Calendar.getInstance();
					previousDayCal.add(Calendar.DATE, -1);
					while (previousDayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
							|| previousDayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
						// if prev day is saturday or sunday, make it friday
						previousDayCal.add(Calendar.DATE, -1);
					}
					moduleDayParamValue = format.format(previousDayCal.getTime());
				}
			}
			
			if (moduleDayParamValue != null) {
				module.forceValue(1, moduleDayParamValue);
			}
		}

		/*
		if (ServerInfoModuleID == TSServerInfo.NAME_MODULE_IDX ) {
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH || getSearch().getSearchType() == Search.GO_BACK_ONE_LEVEL_SEARCH) {
				String sDate = module.getFunction(7).getParamValue();//B4776
				String eDate = module.getFunction(8).getParamValue();
				if (StringUtils.isEmpty(sDate)){
					sDate = format.format(start);
				}
				if (StringUtils.isEmpty(eDate)){
					eDate = format.format(end);
				}
				module.forceValue(7, sDate);
				module.forceValue(8, eDate);
			} else {
				module.forceValue(7, format.format(start));
				module.forceValue(8, format.format(end));
			}
			
		}else */
		if(ServerInfoModuleID == TSServerInfo.PARCEL_ID_MODULE_IDX 
				||	ServerInfoModuleID == TSServerInfo.ADDRESS_MODULE_IDX){
			
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH  || getSearch().getSearchType() == Search.GO_BACK_ONE_LEVEL_SEARCH) {
				String sDate = module.getFunction(5).getParamValue();//B4776
				String eDate = module.getFunction(6).getParamValue();
				if (StringUtils.isEmpty(sDate)){
					sDate = format.format(start);
				}
				if (StringUtils.isEmpty(eDate)){
					eDate = format.format(end);
				}
				module.forceValue(5, sDate);
				module.forceValue(6, eDate);
			} else {
				module.forceValue(5, format.format(start));
				module.forceValue(6, format.format(end));
			}
		}
		
		if (ServerInfoModuleID == TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX) {
			bookNo = module.getFunction(0).getParamValue().trim();
			pageNo = module.getFunction(1).getParamValue().trim();
			link = FileSearch.getIndexLink(bookNo, pageNo);
		}

		if (ServerInfoModuleID == TSServerInfo.INSTR_NO_MODULE_IDX) {
			instrNo = module.getFunction(0).getParamValue().trim();
			link = FileSearch.getIndexLink(instrNo);
		}

		SaleDataSet sds = new SaleDataSet();
		sds.setAtribute("Grantor", mSearch.getSa().getAtribute(
				SearchAttributes.LD_SUBDIVISION));

		RawResponseWrapper rrw = FileSearch.getFile(link, sds);

		String htmlResponse = (rrw == null) ? "" : rrw.getTextResponse();

		ServerResponse Response = new ServerResponse();

		if (!htmlResponse.equals("")) {
			solveResponse(LOCAL_SEARCH_ACTION, ID_SEARCH_BY_LOCAL, "SearchBy",
					Response, rrw, null);
		} else {
			if (ServerInfoModuleID != TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX) {
				Response = super.SearchBy(module, sd);
			}
		}
		if (ServerInfoModuleID == TSServerInfo.NAME_MODULE_IDX) {
			bookNo = module.getFunction(0).getParamValue().trim();
			pageNo = module.getFunction(1).getParamValue().trim();
			link = FileSearch.getIndexLink(bookNo, pageNo);
		}

		return Response;
	}

	private String parseDateParam(String html, String day) {
		String dateParam = null;
		try {
			String inputSrc = "";
			if (day.equals("PreviousDay")) {
				inputSrc = "previous_day_button";
			} else if (day.equals("CurrentDay")) {
				inputSrc = "current_day_button";
			}
			HtmlParser3 htmlParser3 = new HtmlParser3(html);
			NodeList nodeList = htmlParser3.getNodeList();
			NodeList forms = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true);
			for (int i = 0; i < forms.size(); i++) {
				html = forms.elementAt(i).toHtml();
				Pattern datePattern = Pattern
						.compile("(?is)<form[^>]*InstTypeAllForm[^>]*>.*?<input[^>]*name=(?:\"|')searchDate(?:\"|')[^>]*value=(?:\"|')([^\"']*)(?:\"|')[^>]*>.*?<input[^>]*"
								+ inputSrc + "[^>]*>.*?</form>");
				Matcher matcher = datePattern.matcher(html);
				if (matcher.find() && matcher.group(1).matches("\\d+/\\d+/\\d+")) {
					dateParam = matcher.group(1);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dateParam;
	}

	/*
	 * Pretty prints a link that was already followed when creating TSRIndex
	 * (non-Javadoc)
	 * 
	 * @see
	 * ro.cst.tsearch.servers.types.TSServer#getPrettyFollowedLink(java.lang
	 * .String)
	 */
	public String getPrettyFollowedLink(String initialFollowedLnk) {
		if (initialFollowedLnk.matches("(?i).*[^a-z]+instnum=([a-z0-9]+)[^a-z0-9].*")) {
			String retStr = initialFollowedLnk
					.replaceFirst("(?i).*[^a-z]+instnum=([a-z0-9]+)[^a-z0-9].*",
							"Instrument " + "$1" + " has already been processed from a previous search in the log file.");
			retStr = "<br/><span class='followed'>" + retStr + "</span><br/>";

			return retStr;
		}

		return "<br/><span class='followed'>Link already followed</span>:"
				+ initialFollowedLnk + "<br/>";
	}

	/**
	 * @param rsResponce
	 * @param viParseID
	 */
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {

		String sTmp1;
		String sTmp;
		String sForm;

		msServerID = "p1=079&p2=1";

		StringBuffer sBfr = new StringBuffer();
		String rsResponce = Response.getResult();
		String initialResponse = rsResponce;
		String sFileLink = "";

		rsResponce = rsResponce.replaceFirst("(?is)\\s*<table[^>]*>.*?Home.*?Contact.*Glossary.*?</table>\\s*", "");
		rsResponce = rsResponce.replaceFirst("(?is)<table[^>]*>.*Search Parameters:.*?</table>\\s*<table[^>]*>.*?</table>", "");
		StringBuilder pagingDivSB = new StringBuilder("<div id=\"pagingDiv\" align=\"center\" width=\"75%\">");
		int pageNumber = 0;
		
		if (rsResponce.indexOf("tr") == -1) {

			//HTTPSiteManager.pairHTTPSiteForTSServer("TNShelbyRO", searchId,
			//		miServerID).destroySession();

			if (rsResponce.contains("You must do a")
					&& rsResponce.contains("to view a document")) {
				Response.getParsedResponse().setError(
						"Invalid search parameters.");
			} else {
				Response
						.getParsedResponse()
						.setError(
								"Too many queries per time unit. Please try again later.");
			}

			throw new ServerResponseException(Response);
		} else {
			rsResponce = rsResponce.replaceAll(
					"(?is)<\\s*tr[^>]>\\s*<\\s*a\\s+", "<tr><a ");
		}

		int iTmp, iTmp1;

		switch (viParseID) {

		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_MODULE38:// search by Recent and Comparable Sales
		case ID_SEARCH_BY_MODULE39:// Search By Instrument Type
		case ID_SEARCH_BY_MODULE40:// Search by Instrument Type - All
		case ID_SEARCH_BY_BOOK_AND_PAGE:
			
			Pattern pagingPattern = Pattern
					.compile("(?is)(<a[^>]*href=(?:\"|'))([^'\"]*CurrentPage=[^'\"]*)((?:\"|')[^>]*>\\s*(?:NEXT|PREV|\\d+)\\s*</a>)");
			Matcher matcher = pagingPattern.matcher(rsResponce);
			
			while (matcher.find()) {
				pagingDivSB.append("<b>" + matcher.group(1) + CreatePartialLink(TSConnectionURL.idGET) + matcher.group(2)
						+ matcher.group(3) + "</b>&nbsp;");
				if (++pageNumber % 50 == 0) {
					pagingDivSB.append("<br />");
				}
			}
			pagingDivSB.append("</div>");
			String pagingDiv = pagingDivSB.toString();
			
			if (rsResponce.matches("(?is).*Image\\s+Not\\s+Found.*")) {
				rsResponce = "<table><th><b>Image Not Found.</b></th></table>";
				Response.getParsedResponse().setOnlyResponse(rsResponce);
				return;
			} else if (rsResponce
					.matches("(?is).*NO\\s*HITS\\s*FOUND\\s*FOR.*")) {
				rsResponce = "<table><th><b>No Hits Found.</b></th></table>";
				Response.getParsedResponse().setOnlyResponse(rsResponce);
				return;
			}
			
			if (viParseID == ID_SEARCH_BY_MODULE38) {
				iTmp = rsResponce.indexOf("<table width=95% valign=center align=center");
			}
			else {
				iTmp = rsResponce.indexOf("% align=center border=3>");
			}
			if (iTmp == -1) {
				iTmp = rsResponce
						.indexOf("<table align='center' border='3' width=95%>");
			}

			if (iTmp == -1) {
				return;
			}// no result

			if (rsResponce.indexOf("# Hits: 0</") >= 0) {
				// no result
				return;
			}
			
			int iTmpEnd = rsResponce.lastIndexOf("</table>") + 8;
			if (iTmp < iTmpEnd) {
				sBfr.append("<table width=100"
						+ rsResponce.substring(iTmp, iTmpEnd).replaceAll(
								"width=90%", ""));
			}
			
			sTmp1 = "<FORM ACTION=";
			iTmp = sBfr.indexOf(sTmp1);
			while (iTmp > -1) {
				// while we still have a property details form then get it's
				// information
				// get from form until the imput information ends
				sForm = sBfr.substring(iTmp, sBfr.indexOf("</form>", iTmp));
				// create link
				sTmp = createLinkFromForm(sForm, "View");
				sBfr.replace(iTmp, sBfr.indexOf("</td>", iTmp), sTmp);

				// go to next form
				iTmp = sBfr.indexOf(sTmp1, iTmp);
			}

			// /////////////////
			String link = CreatePartialLink(TSConnectionURL.idGET);

			rsResponce = sBfr.toString().replaceAll("</form>", "");

			rsResponce = rsResponce.replaceAll("(?i)</?form.*?>", "");
			rsResponce = rsResponce.replaceAll("(?i)</?font.*?>", "");

			if (viParseID == ID_DETAILS1
					|| viParseID == ID_SEARCH_BY_BOOK_AND_PAGE
					|| viParseID == ID_SEARCH_BY_ADDRESS
					|| viParseID == ID_SEARCH_BY_PARCEL
					|| viParseID == ID_SEARCH_BY_INSTRUMENT_NO
					|| viParseID == ID_SEARCH_BY_MODULE38
					|| viParseID == ID_SEARCH_BY_MODULE39
					|| viParseID == ID_SEARCH_BY_MODULE40) {

				
			if (!downloadingForSave) {
				rsResponce = createTableAndFooter(rsResponce, viParseID, pagingDiv);
				}
			}

			rsResponce = rsResponce.replaceAll("imgView.php\\?" + "([^>]*)>",
					link + "imgView.php&" + "$1" + ">");

			rsResponce = rsResponce.replaceAll("pdetail.php\\?" + "([^>]*)>",
					link + "pdetail.php&" + "$1" + ">");

			rsResponce = rsResponce.replace("</a></a>", "</a>");
			rsResponce = rsResponce.replaceAll("(?i)\\s+target='_self'", "");
			
			//kill link duplicates
			rsResponce = rsResponce.replaceAll("(?is)<a[^>]+>\\s*(<td)", "$1  ");//if modify this, then check vsRowSeparator from splitResultsRows
			rsResponce = rsResponce.replaceAll("(?is)(<a[^>]+>)\\s*(\\1)", "$2");

			String linkStart = getLinkPrefix(TSConnectionURL.idGET);
			int pageId;
			switch (viParseID) {
			case ID_SEARCH_BY_NAME:
				pageId = Parser.PAGE_ROWS_NAME;
				break;
			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_BOOK_AND_PAGE:
			case ID_SEARCH_BY_INSTRUMENT_NO:
			case ID_SEARCH_BY_MODULE38:
			case ID_SEARCH_BY_MODULE39:
			case ID_SEARCH_BY_MODULE40:
				pageId = Parser.PAGE_ROWS_SUBDIV_CODE;
				break;
			default:
				pageId = Parser.PAGE_ROWS;
			}

			int action = (viParseID == ID_SEARCH_BY_NAME) ? TSServer.REQUEST_GO_TO_LINK_REC
					: TSServer.REQUEST_SAVE_TO_TSD;
			
			parser.setTsserver(this);
			
			if (viParseID == ID_SEARCH_BY_INSTRUMENT_NO || viParseID == ID_SEARCH_BY_MODULE38 || viParseID == ID_SEARCH_BY_MODULE39
					|| viParseID == ID_SEARCH_BY_MODULE40) {
				rsResponce = ro.cst.tsearch.utils.Tidy.tidyParse(rsResponce, null).replaceAll("(?is)(<tr>)\\s*(<td)", "$1$2");
			}
			rsResponce = rsResponce.replaceFirst("<tr>", "<tr id=\"header\">");
			
			parser.Parse(Response.getParsedResponse(), rsResponce, pageId,
					linkStart, action);
			
			break;

		case ID_SEARCH_BY_NAME:
		case ID_DETAILS1: // parse search result

			if (rsResponce.matches("(?is).*Image\\s*Not\\s*Found.*")) {
				rsResponce = "<table><th><b>Image Not Found.</b></th></table>";
				Response.getParsedResponse().setOnlyResponse(rsResponce);
				return;
			} else if (rsResponce
					.matches("(?is).*NO\\s*HITS\\s*FOUND\\s*FOR.*")) {
				rsResponce = "<table><th><b>No Hits Found.</b></th></table>";
				Response.getParsedResponse().setOnlyResponse(rsResponce);
				return;
			}

			if (pageNumber == 0) {
				pagingPattern = Pattern 
						.compile("(?is)(<a[^>]*href=(?:\"|'))([^'\"]*p3\\.php\\?param1=[^'\"]*)((?:\"|')[^>]*>\\s*(?:NEXT|PREV|\\d+))\\s*<");
				matcher = pagingPattern.matcher(rsResponce);
				pageNumber = 0;
				while (matcher.find()) {
					pagingDivSB.append( matcher.group(1) + CreatePartialLink(TSConnectionURL.idGET) + matcher.group(2)
							+ matcher.group(3) + "</a>"+"&nbsp;");
					if (++pageNumber % 50 == 0) {
						pagingDivSB.append("<br />");
					}
				}
			}
			pagingDivSB.append("</div>");
			pagingDiv = pagingDivSB.toString();
			pagingDiv = pagingDiv.replaceAll("(?is)(<a[^>]*>.*?</a>)", "<b>$1</b>") ;
			
			// cut the main table
			int istart,
			iend;
			if (viParseID == ID_DETAILS1) {
				istart = rsResponce
						.indexOf("<table width=95% align=center border=3>");
				iend = rsResponce.lastIndexOf("</table>");
			} else {
				istart = rsResponce
						.indexOf("<table width=90% align=center border=1>");
				if (istart == -1) {
					istart = rsResponce
							.indexOf("<table width=90% align=center border=3>");
					iend = rsResponce.lastIndexOf("</table>");
				} else {
					iend = rsResponce.lastIndexOf("</table>");
					iend = rsResponce.lastIndexOf("</table>", iend - 1);
				}
			}

			// clean html code
			if (istart > -1 && iend > -1)
				rsResponce = rsResponce.substring(istart, iend
						+ "</table>".length());

			rsResponce = rsResponce.replaceAll("target='_self'", "");
			rsResponce = rsResponce.replaceAll("(?is)<form.*?>", "");
			rsResponce = rsResponce.replaceAll("(?is)</form>", "");
			rsResponce = rsResponce.replaceAll("(?is)<font.*?>", "");
			rsResponce = rsResponce.replaceAll("(?is)</font>", "");

			// split main table and modify links
			StringBuffer mainTable = new StringBuffer(
					"<table width=\"100%\" align=\"center\" border=\"1\">");

			istart = 0;
			while (true) {

				istart = rsResponce.indexOf("<tr", istart + 1);
				iend = rsResponce.indexOf("</tr>", istart);

				if (istart == -1 || iend == -1)
					break;

				iend += "</tr>".length();

				String oneLine = rsResponce.substring(istart, iend);

				oneLine = oneLine.replaceAll("[\n|\r|\n\r|\t]", " ");

				//clean unnecessary links
				oneLine = oneLine.replaceAll("\\s+", " ");
				oneLine = oneLine.replaceAll("<tr>(<a href=\".*?)<td\\s", "<tr><td ");
				oneLine = oneLine.replaceAll("</a></a>", "</a>");
				oneLine = oneLine.replaceAll("</a><a href=\\\"imgView.php\\?id=.*?\\\" >", "");
				
				oneLine = oneLine.replaceAll("searchtype=\\&",
						"searchtype=Pure Alpha Search&");
				oneLine = oneLine.replaceAll("\\s+", " ");

				oneLine = oneLine.replaceAll("<a href=\"", "<a href=\""
						+ CreatePartialLink(TSConnectionURL.idGET));
				oneLine = oneLine.replaceAll("(?is)\\.php\\?", ".php&");

				mainTable.append("\n");
				mainTable.append(oneLine);
			}

			mainTable.append("\n");
			mainTable.append("</table> ");
			if (viParseID == ID_DETAILS1) {
				Pattern pTr;
				Matcher matTr = null;
				pTr = Pattern.compile("(?is)<tr.*?/tr[^>]*>");
				matTr = pTr.matcher(mainTable);
				if (matTr.find()) {
					mainTable.delete(matTr.start(), matTr.end());
				}
				pTr = Pattern.compile("(?is)(?is)<tr.*?<td");
				matTr = pTr.matcher(mainTable);
				if (matTr.find()) {
					mainTable.delete(matTr.start(), matTr.end());
					mainTable.insert(matTr.start(), "<tr><td></td><td");
				}
			}

			rsResponce = pagingDiv + mainTable.toString();

			// add multiselect checkboxes
			if (viParseID == ID_DETAILS1
					|| viParseID == ID_SEARCH_BY_BOOK_AND_PAGE
					|| viParseID == ID_SEARCH_BY_ADDRESS
					|| viParseID == ID_SEARCH_BY_PARCEL) {
				rsResponce = rsResponce.replaceAll("(?is)<td></td>\\s*(<td bgcolor=silver>Details</td>)", "$1");
				rsResponce = createTableAndFooter(rsResponce, viParseID, pagingDiv );
			}

			rsResponce = rsResponce.replaceAll("imgView.php\\?" + "([^>]*)>",
					CreatePartialLink(TSConnectionURL.idGET) + "imgView.php&"
							+ "$1" + ">");

			linkStart = getLinkPrefix(TSConnectionURL.idGET);

			switch (viParseID) {
			case ID_SEARCH_BY_NAME:
				pageId = Parser.PAGE_ROWS_NAME;
				break;
			case ID_SEARCH_BY_PARCEL:
				pageId = Parser.PAGE_ROWS_SUBDIV_CODE;
				break;
			default:
				pageId = Parser.PAGE_ROWS;
			}

			action = (viParseID == ID_SEARCH_BY_NAME) ? TSServer.REQUEST_GO_TO_LINK_REC
					: TSServer.REQUEST_SAVE_TO_TSD;

			parser.setTsserver(this);
			parser.Parse(Response.getParsedResponse(), rsResponce, pageId,
					linkStart, action);
			
			break;
		case ID_DETAILS2:
			if (rsResponce.matches("(?is).*Image\\s*Not\\s*Found.*")) {
				rsResponce = "<table><th><b>Image Not Found.</b></th></table>";
				Response.getParsedResponse().setOnlyResponse(rsResponce);
				return;
			} else if (rsResponce
					.matches("(?is).*NO\\s*HITS\\s*FOUND\\s*FOR.*")) {
				rsResponce = "<table><th><b>No Hits Found.</b></th></table>";
				Response.getParsedResponse().setOnlyResponse(rsResponce);
				return;
			}


			rsResponce = rsResponce.substring(rsResponce
					.indexOf("Detail Information For ") + 23);
			if (rsResponce.indexOf("<table width=95% ") == -1)
				rsResponce = repairFile(Response);
			sBfr.append("<table width=95%><tr><td>"
					+ rsResponce.substring(0, rsResponce.indexOf("</a>"))
					+ " .");
			// test if we have any document image
			iTmp = rsResponce.indexOf("imgView.php");

			String qry = "";
			if (iTmp > -1) {

				String year = TSServer.getParameter("year", Response
						.getQuerry());
				String db = TSServer.getParameter("db", Response.getQuerry());
				Pattern p = Pattern.compile("imgView.php\\?&?id=([^\"]*)\"");
				Matcher m = p.matcher(rsResponce);
				if (m.find()) {

					qry = m.group(1);
				}
				qry += "&year=" + year
						+ "&db=" + db
						+ "&ck=" + getSearch().getID() 
						+ "TNShelbyRO";

				sFileLink = "<A target=\"_blank\" HREF='"
						+ CreatePartialLink(TSConnectionURL.idGET)
						+ "imgView.php&id=" + qry + "'> View Image </a>";

				sBfr.append("<br>" + sFileLink);
			}
			sBfr.append("</td></tr></table>");

			try {
				rsResponce = rsResponce.substring(rsResponce
						.indexOf("<table width=95%"));
			} catch (Exception e) {
				// e.printStackTrace();
				rsResponce = "<table><tr><td><a class=\"Header\">No details on parent site for "
						+ rsResponce;
				rsResponce = rsResponce.substring(0, rsResponce
						.indexOf("</table>") + 8);

				parser.Parse(Response.getParsedResponse(), rsResponce,
						Parser.NO_PARSE);
				return;
			}

			if (rsResponce.indexOf("</body>") > -1)
				sBfr
						.append(rsResponce.substring(rsResponce
								.indexOf("</body>")));
			else
				sBfr.append(rsResponce);

			// test if we have forms
			iTmp = sBfr.indexOf("<FORM ");
			while (iTmp > -1) {
				// while we still have a property details form then convert it
				// in link
				// get from form until the imput information ends
				iTmp1 = sBfr.indexOf("</form>", iTmp);
				if (iTmp1 == -1)
					iTmp1 = sBfr.indexOf("</td>", iTmp);
				else
					iTmp1 += 7;
				sForm = sBfr.substring(iTmp, iTmp1);
				// replace form with a link
				sBfr.replace(iTmp, iTmp1, "<td>"
						+ createLinkFromForm(sForm, "View"));
				iTmp = sBfr.indexOf("<FORM ");
			}

			rsResponce = "<hr>"
					+ sBfr
							.toString()
							.replaceAll(
									"<FONT[a-zA-Z0-9/=,_\\<\\?\\.'\\s\\\"\\r\\n]+>",
									"");
			rsResponce = rsResponce.replaceAll(
					"<img[a-zA-Z0-9/=,_\\<\\?\\.'\\s\\\"\\r\\n]+>", "");
			rsResponce = rsResponce.replaceAll("align=center", "");
			rsResponce = rsResponce.replaceAll("</FONT>", "");
			rsResponce = rsResponce.replaceAll("bgcolor=BLACK", "");
			rsResponce = rsResponce.replaceAll(
					"<TEXTAREA[a-zA-Z0-9/=,_\\<\\?\\.'\\s\\\"\\r\\n]+>", "");
			rsResponce = rsResponce.replaceAll("</textarea>", "");

			// find Instrument No. in html
			String instrNo = getInstrNoFromResponse(rsResponce);

			// find Year in html
			String year = getYearFromResponse(rsResponce);
			
			String book = getBookNoFromResponse(rsResponce).trim();
			String page = getPageNoFromResponse(rsResponce).trim();

			String documentType = getDocTypeFromResponse(rsResponce);

			String docName = instrNo + documentType;

			// download view file if any;
			if (!(sFileLink.equals(""))) {
				String imageFileName = instrNo + documentType + ".tiff";

				link = "imgView.php&id=" + qry;

				qry = CreatePartialLink(TSConnectionURL.idGET);
				String ins = getInstrNoFromResponse(rsResponce);

				Response.getParsedResponse().addImageLink(
						new ImageLinkInPage(qry + link, imageFileName));

				int pnt = rsResponce.indexOf(".");
				if (pnt != -1) {
					rsResponce = rsResponce.substring(0, pnt + 1) + "(Insnum= "
							+ ins + ")"
							+ rsResponce.substring(pnt, rsResponce.length());

				}
			}

			rsResponce = rsResponce.replaceAll("'", "\"");

			String docFullName = docName.replaceAll("\\s", "") + ".html";
			if (!downloadingForSave) {

				String originalLink = getOriginalLink(instrNo, year);
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST)
						+ originalLink + "&dummy=" + instrNo + documentType.replaceAll("\\s+", "");
				
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", documentType);
				data.put("year", year);
				data.put("book", book);
				data.put("page", page);
				
				if (super.isInstrumentSaved(instrNo, null, data)){ 
					rsResponce += CreateFileAlreadyInTSD();
				} else {
					// replace any existing form end
					rsResponce = rsResponce.replaceAll("</FORM>", "");
					rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink,
							viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
				}

				Response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponce,
						Parser.NO_PARSE);

			} else { // saving

				msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
				msSaveToTSDFileName = docFullName;

				Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);

				parser.setTsserver(this);
				// save any coss ref link before removing it
				parser.Parse(Response.getParsedResponse(), rsResponce,
						Parser.PAGE_DETAILS,
						getLinkPrefix(TSConnectionURL.idPOST),
						TSServer.REQUEST_SAVE_TO_TSD);

				// if we have error on instrument number, erase it completely
				try {
					Vector saleDataSets = (Vector) (Response
							.getParsedResponse().infVectorSets
							.get("SaleDataSet"));
					SaleDataSet saleDataSet = (SaleDataSet) saleDataSets
							.elementAt(0);

					String instrumentNo = saleDataSet
							.getAtribute("InstrumentNumber");

					if ("-".equals(instrumentNo)) {
						saleDataSet.setAtribute("InstrumentNumber", saleDataSet
								.getAtribute("Book")
								+ "-" + saleDataSet.getAtribute("Page"));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				// remove cross ref links
				rsResponce = rsResponce
						.replaceAll(
								"<td><A HREF=[a-zA-Z0-9/=&,\\-_\\(\\)\\<\\?\\.\\>'\\s\\\"\\r\\n]+</a>",
								"<td>");
				Response.getParsedResponse().setOnlyResponse(rsResponce);
			}

			break;
		case ID_GET_LINK:
			if (sAction.startsWith("/pdetail.php") /*
													 * ||
													 * sAction.startsWith("/p4.php"
													 * )
													 */) {
				msLastdb = StringUtils.getTextBetweenDelimiters("db=", "&",
						Response.getQuerry());
				ParseResponse(sAction, Response, ID_DETAILS2);
			} else if (sAction.startsWith("/p2.php?CurrentPage")) {// paging link
				ParseResponse(sAction, Response, ID_SEARCH_BY_INSTRUMENT_NO);
			} else if (sAction.startsWith("/p3.php?param1")) {// paging link for name search
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else {
				ParseResponse(sAction, Response, ID_DETAILS1);
			}
			break;
		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			if (sAction.equals(LOCAL_SEARCH_ACTION)) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_LOCAL);
			} else {
				ParseResponse(sAction, Response, ID_DETAILS2);
			}
			downloadingForSave = false;
			break;
		case ID_SEARCH_BY_LOCAL:
			if (rsResponce.matches("(?is).*Image\\s*Not\\s*Found.*")) {
				rsResponce = "<table><th><b>Image Not Found.</b></th></table>";
				Response.getParsedResponse().setOnlyResponse(rsResponce);
				return;
			} else if (rsResponce
					.matches("(?is).*NO\\s*HITS\\s*FOUND\\s*FOR.*")) {
				rsResponce = "<table><th><b>No Hits Found.</b></th></table>";
				Response.getParsedResponse().setOnlyResponse(rsResponce);
				return;
			}

			// logger.debug("sAction = " + sAction);
			rsResponce = rsResponce.replaceAll("<html><body>", "");
			rsResponce = rsResponce.replaceAll("</body></html>", "");

			rsResponce = rsResponce.replaceAll("<a href=\"",
					"<a target=\"_blank\" href=\""
							+ CreatePartialLink(TSConnectionURL.idGET));

			instrNo = StringUtils.getTextBetweenDelimiters("Instrument #",
					" .", rsResponce).trim();
			docName = instrNo;
			docFullName = docName + ".html";

			if (!downloadingForSave) {
				String originalLink = FileSearch.getIndexLink(instrNo);
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET)
						+ originalLink;

				if (FileAlreadyExist(docFullName) && isFromRO(docFullName)) {
					rsResponce += CreateFileAlreadyInTSD();
				} else {
					rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink,
							viParseID);
				}

				Response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponce,
						Parser.NO_PARSE);

			} else { // saving

				msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
				msSaveToTSDFileName = docFullName;

				Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);

				String fileExt = "tif";
				int i = 0;

				String imageLink = StringUtils.getTextBetweenDelimiters(0,
						"href=\"", "\">", rsResponce).trim();

				while (!imageLink.equals("")) {

					String imageFileName = docName + "_" + i + "." + fileExt;
					i++;

					Response.getParsedResponse().addImageLink(
							new ImageLinkInPage(imageLink, imageFileName));
					imageLink = StringUtils.getTextBetweenDelimiters(i,
							"href=\"", "\">", rsResponce).trim();
				}

				parser.setTsserver(this);
				parser.Parse(Response.getParsedResponse(), rsResponce,
						Parser.PAGE_DETAILS);
			}
			break;
		default:
			break;
		}
	}

	private String getYearFromResponse(String rsResponce) {

		// get Instrument No.
		String year = StringUtils.getTextBetweenDelimiters("year=", "&",
				rsResponce); // ;rsResponce.substring(iTmp,
		// iTmp
		// +
		// 4);

		return year;
	}

	@SuppressWarnings("unused")
	private String getPageNoFromResponse(String response) {
		String page = "";
		String pageNoDelimStart = ">Page #";
		String pageNoDelimEnd = "<";

		try {
			page = StringUtils.getTextBetweenDelimiters(pageNoDelimStart,
					pageNoDelimEnd, response);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return page;
	}

	private String getBookNoFromResponse(String response) {
		String book = "";
		String bookNoDelimStart = "Book #";
		String bookNoDelimEnd = "<";

		try {
			book = StringUtils.getTextBetweenDelimiters(bookNoDelimStart,
					bookNoDelimEnd, response);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return book;
	}

	private String getInstrNoFromResponse(String rsResponce) {

		int iTmp;
		
		if(rsResponce.indexOf("Instrument # ") < 0) {
			String book = getBookNoFromResponse(rsResponce).trim();
			String page = getPageNoFromResponse(rsResponce).trim();
			return org.apache.commons.lang.StringUtils.leftPad(book, 4, '0') 
				+ org.apache.commons.lang.StringUtils.leftPad(page, 3, '0');
		} else {
			iTmp = rsResponce.indexOf("Instrument # ") + 13;
		}

		// get Instrument No.
		String instrNo = rsResponce.substring(iTmp, rsResponce.indexOf(" .",
				iTmp));

		if (instrNo.indexOf("Page") != -1 || instrNo.length() == 0) {
			iTmp = rsResponce.indexOf("Book and Page # ") + 16;
			instrNo = rsResponce
					.substring(iTmp, rsResponce.indexOf(" .", iTmp));
			instrNo = instrNo.replaceAll("#", "");

		}

		if (instrNo.equals("-")) {
			instrNo = StringUtils.getTextBetweenDelimiters("Book #", "<",
					rsResponce).trim()
					+ "-"
					+ StringUtils.getTextBetweenDelimiters(">Page #", "<",
							rsResponce).trim();
		}

		return instrNo.trim();
	}

	private List<String> getInstrNoForInstSearch(String response) {
		String header = "Inst #";
		List<String> columnValuesFromHTMLTableForAGivenHeader = StringUtils
				.getColumnValuesFromHTMLTableForAGivenHeader(response, header);
		Pattern finalPattern = Pattern.compile("(?i)>([A-Z0-9-]+)<");
		ArrayList<String> returnList = new ArrayList<String>();
		
		for (String string2 : columnValuesFromHTMLTableForAGivenHeader) {
			Matcher matcher = finalPattern.matcher(string2);
			matcher.find();
			returnList.add(matcher.group(1));
		}
		return returnList;
	}
	
	

	private String getDocTypeFromResponse(String rsResponse) {
		String docType = StringUtils.getTextBetweenDelimiters("Doc Type:",
				"</", rsResponse);
		docType = docType.replaceAll("(?is)\\s+(\\w+)", "AAAAAA$1");//B 4647 WARRANTY DEED it becomes WARRANTYDEED because of the next line
		docType = docType.replaceAll("[^A-Za-z0-9\\-]", "");
		docType = docType.replaceAll("(?is)AAAAAA", " ");
		return docType.trim();
	}

	private String getDocType(String rsResponse) {
		String docType = "";
		String header = "Inst Code";
		List<String> columnValuesFromHTMLTableForAGivenHeader = StringUtils
				.getColumnValuesFromHTMLTableForAGivenHeader(rsResponse, header);
		String string = columnValuesFromHTMLTableForAGivenHeader.get(0);
		Pattern finalPattern = Pattern.compile("(?i)>([A-Z]+)<");
		Matcher matcher = finalPattern.matcher(string);
		if (matcher.find()) {
			docType = matcher.group(1);
		}
		return docType;
	}

	/**
	 * @param sTmp
	 * @return
	 */
	private String getOriginalLink(String instrNo, String year) {
		return "/pdetail.php&instnum=" + instrNo + "&year=" + year + "&db="
				+ msLastdb;
	}

	// ///////////////////quick fix
	private String repairFile(ServerResponse r) {

		try {

			String rez = null;

			String qry = r.getQuerry();
			qry = qry.replaceAll("year=", "year=2003");
			qry = qry.replaceAll("db=[0-9]", "db=0");

			r.setQuerry(qry);

			String link = HOST + "/pdetail.php?" + qry;

			HTTPRequest req = new HTTPRequest(link);
			req.setMethod(HTTPRequest.GET);

			HTTPResponse res;
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				/*
				res = HTTPSiteManager.pairHTTPSiteForTSServer("TNShelbyRO",
						searchId, miServerID).process(req);*/
				res = site.process(req);
				rez = res.getResponseAsString();
			} catch (Exception e) {
				e.printStackTrace();
				rez = "";
			}finally {
				// always release the HttpSite
				HttpManager.releaseSite(site);
			}

			rez = rez.substring(rez.indexOf("Detail Information For ") + 23);

			return rez;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// ////////////////////////////////////////////

	protected DownloadImageResult saveImage(ImageLinkInPage image)
			throws ServerResponseException {
		getTSConnection().setHostName(IMAGES_SITE);
		getTSConnection().setHostIP(IMAGES_SITE_IP);
		DownloadImageResult res = super.saveImage(image);
		// undo the modifications
		getTSConnection().setHostName(msiServerInfo.getServerAddress());
		getTSConnection().setHostIP(msiServerInfo.getServerIP());
		return res;
	}

	protected String getFileNameFromLink(String link) {
		String instNo = "";
		int dummyIndex = link.indexOf("dummy=");
		if (dummyIndex < 0) {
			instNo = StringUtils
					.getTextBetweenDelimiters("instnum=", "&", link).trim();

			if (instNo.equals("")) {
				instNo = FileSearch.getInstrNoFromLink(link
						.substring((getLinkPrefix(TSConnectionURL.idGET))
								.length()));
			}
		} else {
			int amp = link.indexOf("&", dummyIndex);
			if (amp < 0) {
				instNo = link.substring(dummyIndex + 6);
			} else {
				instNo = link.substring(dummyIndex + 6, amp);
			}
		}

		return instNo + ".html";
	}

	public static void splitResultRows(Parser p, ParsedResponse pr,
			String htmlString, int pageId, String linkStart, int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException {
		p.splitResultRows(pr, htmlString, pageId, "<tr><td", "</table",
				linkStart, action);
	}

	protected boolean isFromRO(String doc) {

		Search sx = InstanceManager.getManager().getCurrentInstance(searchId)
				.getCrtSearchContext();

		@SuppressWarnings("rawtypes")
		Hashtable hashRO = sx.getRODocs();

		if (hashRO == null) // daca nu am nici un doc de pe RO...
			return false;

		if (hashRO.toString().indexOf(doc) != -1) // daca doc este de pe RO...
			return true;

		return false;
	}

	public TSServerInfoModule getInstrumentModule(TSServerInfo serverInfo,
			String instrumentNo) {
		return null;
	}

	public TSServerInfoModule getBookPageModule(TSServerInfo serverInfo,
			String book, String page) {
		return null;
	}

	public String createTableAndFooter(String htmlTable, int viParseID, String pagingDiv){
		StringBuilder refactoredHtmlSB = new StringBuilder();
		int count = 0; 
		String tableHeader = "";
		String a="";
		boolean instrumentNotSaved=false;
		boolean noResults = false;
		List<Boolean> instrSaved = new ArrayList<Boolean>();
		List<String> columnValuesFromHTMLTableForAGivenHeader = StringUtils
				.getTableRows(htmlTable);
		if (columnValuesFromHTMLTableForAGivenHeader.size() > 0) {
			tableHeader = columnValuesFromHTMLTableForAGivenHeader.get(0);
		}
		
		for (String tableRow : columnValuesFromHTMLTableForAGivenHeader) {
			if (tableRow.equals(tableHeader)) {
				tableHeader = tableHeader.replaceFirst("^(.*?)(<td[^>]*bgcolor=silver>.*?Details.*?</td>)",
						"$1<td width=2% bgcolor=silver>" + SELECT_ALL_CHECKBOXES + "</td>$2");
				refactoredHtmlSB.append(tableHeader);
				continue;
			}
				
			
			instrumentNotSaved=false;
			// there will be only one instrument number
			String instrNo = getInstrNoForInstSearch(tableHeader + tableRow).get(0);
			
			// find Year in html
			String year = getYearFromResponse(tableHeader+tableRow);

			// String docName = instrNo + documentType;
			HashMap<String, String> data = new HashMap<String, String>();
			String docType = getDocType(tableHeader+tableRow);
			data.put("type", docType);
			data.put("year", year);
			String replacement = "<td width=2%><input type=\"checkbox\" name=\"docLink\" value=\"$3\" ></td>$1$2$4";
			
			if (!super.isInstrumentSaved(instrNo, null, data)) {
				count++;
				instrumentNotSaved = true;
			} else {
				replacement = "<td width=2%>Saved to TSRI</td>";
			}

			tableRow = tableRow.replaceAll(
					"(<td.*?bgcolor=silver>.*?Details.*?</td>)",
					"<td width=2% bgcolor=silver>&nbsp;</td>$1");
					
			
			tableRow = tableRow
					.replaceAll(
							"(<td.*?>)(<A HREF='(.*?)'>View</a>)(</td>)",
							replacement);
			if (instrumentNotSaved){
				tableRow = tableRow.replaceAll("(?i)(<td[^>]*>)(<A HREF=\"([^\"]*)\"[^>]*><b>INDEX\\.?</b></a>)(</td>)",
												replacement);

				tableRow = tableRow.replaceAll("(?i)(<td[^>]*>)(<A HREF=\"([^\"]*)\"[^>]*><b>INDEX\\.?</b></a></a>)(</td>)",
												replacement);
			}else {
				tableRow = tableRow.replaceAll("(?i)(<td[^>]*><A HREF=\"([^\"]*)\"[^>]*><b>INDEX\\.?</b></a></td>)",
						replacement + "$1");

				tableRow = tableRow.replaceAll("(?i)(<td[^>]*><A HREF=\"([^\"]*)\"[^>]*><b>INDEX\\.?</b></a></a></td>)",
										replacement + "$1");				
			}
			instrSaved.add(instrumentNotSaved);
			refactoredHtmlSB.append(tableRow);
		}
		String refactoredHtml = refactoredHtmlSB.toString(); 
		if (tableHeader.equals(refactoredHtml)) {
			noResults = true;
		}
		
		a = pagingDiv + "<table width=\"100%\" align=\"center\" border=\"1\">" + refactoredHtml + "</table>";
		boolean alldocsNotSaved = false;
		for(boolean isNotSaved:instrSaved){
			if (isNotSaved){
				alldocsNotSaved = true;
				break;
			} else {
				alldocsNotSaved = false;
			}
		}
		
		String htmlFooter = alldocsNotSaved==true ? CreateSaveToTSDFormEnd(
				SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID,count):CreateFileAlreadyInTSD();
				
		a = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION,"POST")  
					+ a 			   
					+ htmlFooter;
		
		
		return (noResults ? "" : a);
	}
	
	
	@Override
	protected void setCertificationDate() {
		
		try {
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
		        TSConnectionURL t = new TSConnectionURL();
		        TSServerInfo info = TSServersFactory.GetServerInfo((int) TSServersFactory.getSiteId("TN", "Shelby", "RO"),searchId);
		        t.setHostName(info.getServerAddress());
		        t.setHostIP(info.getServerIP());
	        
	            BridgeConn bc = new BridgeConn(t, "", "/index.php",
	                    TSConnectionURL.idGET,
	                    "ShelbyRegister-certification date", 
	                    searchId, miServerID);
	            bc.process();
	
	            Matcher certDateMatcher = certDatePattern.matcher(t.getResponseWrapper().getTextResponse());
	            if(certDateMatcher.find()) {
	            	String date = certDateMatcher.group(1).trim();
	            	
	            	CertificationDateManager.cacheCertificationDate(dataSite, date);
	            	getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
	            }
			}
        } catch (ServerResponseException sre) {
            logger.error(sre.getServerResponse().getError());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		
		if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
			module.forceValue(1, "INUM");
			module.forceValue(2, "execute search");
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(3, "BP");
			module.forceValue(4, "execute search");
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
			module.forceValue(1, "INUM");
			module.forceValue(2, "execute search");
		} else {
			module = null;
		}
		return module;
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		if(document == null || StringUtils.isEmpty(document.getInstrumentNumber()) || document.getYear() <= 0) {
			return null;
		}
		//p1=079&p2=1&searchId=3516803&ActionType=2&Link=imgView.php&id=AE58621987&year=1987&db=0&ck=3516803TNShelbyRO
		if(document.getInstrumentNumber().endsWith(Integer.toString(document.getYear()))) {
			return "p1=079&p2=1&searchId=" + getSearch().getID() + "&ActionType=2&Link=imgView.php&id=" + 
					document.getInstrumentNumber() + "&year=" + document.getYear() + "&db=0";
		} else {
			return "p1=079&p2=1&searchId=" + getSearch().getID() + "&ActionType=2&Link=imgView.php&id=" + 
					document.getInstrumentNumber() + document.getYear() + "&year=" + document.getYear() + "&db=0";
		}
	}
	
	@Override
	public void smartParseForOldSites(ParsedResponse pr, ResultMap resultMap, String htmlString, int parserId){
		if(parserId == Parser.PAGE_DETAILS) {
			try {
				ro.cst.tsearch.servers.functions.TNShelbyRO.parseName(resultMap, searchId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

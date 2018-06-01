package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.functions.TNDavidsonRO.parseName;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.Text;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.CookieManager;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.LinkParser;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.misc.NoIndexingInfoFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.ParcelIdIterator;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.data.PlatBookPage;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.CompanyNameExceptions;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.Cookie;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.MatchEquivalents;
import ro.cst.tsearch.utils.MostCommonName;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author costin
 * 
 */
public class TNDavidsonRO extends TSServerROLike implements TSServerROLikeI {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String FILE_REFERER = "http://www.registerofdeeds.nashville.org/recording/SimpleQuery.asp"; // name
	
	public static final String TABLE_TEXT = "<table border=1 width='100%'><tr><td><b>Search Criteria:</b>";
	private static final Pattern CROSSREF_PAT = Pattern.compile("(?is)(\\bhref=[\\\"|'])(simplequery\\.asp\\?instrs=[^'|^\\\"]+)");

	private static int miDateModule;

	private static final Cookie cookie = new Cookie();

	/*
	 * constants for doc types (taken from a multiselection from official
	 * site/advance search)
	 */
	public static final String DOC_TYPE_BOND = "6";

	public static final String DOC_TYPE_CHARTER = "9";

	public static final String DOC_TYPE_DUP = "14";

	public static final String DOC_TYPE_JDG = "15";

	public static final String DOC_TYPE_LIEN = "5";

	public static final String DOC_TYPE_MILITARY_DISCHARGE = "2";

	public static final String DOC_TYPE_MINERAL_INTEREST = "1";

	public static final String DOC_TYPE_NOTICE_OF_COMPLETION = "7";

	public static final String DOC_TYPE_NOTICE_OF_UNDERGROUND_UTILITIES = "4";

	public static final String DOC_TYPE_PLAT = "8";

	public static final String DOC_TYPE_POWER_OF_ATTORNEY = "10";

	public static final String DOC_TYPE_RELEASE = "11";

	public static final String DOC_TYPE_TRUST_DEED = "13";

	public static final String DOC_TYPE_UCC = "12";

	public static final String DOC_TYPE_WARRANTY_DEED = "3";

	// ------------------------------------------
	private static final Category loggerCookie = Logger.getLogger(Log.DETAILS_PREFIX + Cookie.class.getName());

	private static final Category logger = Logger.getLogger(TNDavidsonRO.class);

	private static final Pattern certDatePattern = Pattern.compile("(?ism)The Data is Current Thru:</SPAN>(.*?)</font>");
	

	public TNDavidsonRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (searchType == Search.AUTOMATIC_SEARCH) {
			TSServerInfoModule m;
	
			SearchAttributes sa = global.getSa();
	
			boolean searchWithSubdivision = searchWithSubdivision();
	
			FilterResponse addressHighPassFilterResponse = new AddressSubdivisionFilter(searchId, 0.8d);
	
			DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
			DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId, true, true).getValidator();
			DocsValidator addressHighPassValidator = addressHighPassFilterResponse.getValidator();
			DocsValidator lastTransferDateValidator = (new LastTransferDateFilter(searchId)).getValidator();
			DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
			DocsValidator buyerDoctypeValidator = DoctypeFilterFactory.getDoctypeFilter(searchId, 0.8, new String[] { "LIEN", "COURT" },
																								FilterResponse.STRATEGY_TYPE_HIGH_PASS).getValidator();
			DocsValidator subdivisionNameValidator = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId).getValidator();
			
			{
				InstrumentGenericIterator instrumentIterator = new InstrumentGenericIterator(searchId, getDataSite());
				instrumentIterator.enableInstrumentNumber();
				instrumentIterator.setLoadFromRoLike(false);
				
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				//m.addValidator(pinValidator);
				m.addValidator(recordedDateValidator);
				m.setSaObjKey(SearchAttributes.INSTR_LIST);
				m.clearSaKey(1);
				m.clearSaKey(2);
				
				m.forceValue(3, "Summary Data");
				m.forceValue(4, "ASC");
				
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				m.addIterator(instrumentIterator);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				l.add(m);
			}
			{
				InstrumentGenericIterator bpIterator = new InstrumentGenericIterator(searchId, getDataSite());
				bpIterator.enableBookPage();
				bpIterator.setLoadFromRoLike(false);
				
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				//m.addValidator(pinValidator);
				m.addValidator(recordedDateValidator);
				m.clearSaKey(0);
				
				m.forceValue(3, "Summary Data");
				m.forceValue(4, "ASC");
				
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
				m.addIterator(bpIterator);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				l.add(m);
			}
			
			{
				// search by parcel ID
				ParcelIdIterator pidIterator = new ParcelIdIterator(searchId){
					
					/**
					 * 
					 */
					private static final long serialVersionUID = 838306283571390235L;
	
					@Override
					protected String preparePin(String pin){
						pin = pin.replaceAll("(?is)\\p{Punct}", "");
						if (pin.length() > 12){
							return pin.substring(0, 12);
						}
						return pin;
					}
				};
			
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_PARCEL_ID);
				
				m.clearSaKey(0);
				m.clearSaKey(1);
				m.clearSaKey(3);
				m.clearSaKey(4);
				m.clearSaKey(8);
				m.clearSaKey(10);
				m.clearSaKey(12);
				m.clearSaKey(17);
				m.clearSaKey(18);
				
				m.forceValue(2, "Summary Data");
				m.forceValue(5, "ParcelNum");
				m.forceValue(14, "ASC");
				m.forceValue(17, "");
				m.forceValue(18, "DateTime");
				
				m.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE);
				m.addIterator(pidIterator);

				m.addValidator(defaultLegalValidator);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				l.add(m);
			}
	
			ConfigurableNameIterator nameIterator = null;
			//search by name
			if (hasOwner()){
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
				m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				
				DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
					
				m.clearSaKey(3);
				m.clearSaKey(4);
				m.clearSaKey(6);
				m.clearSaKey(8);
				m.clearSaKey(10);
				m.clearSaKey(12);
				
				m.forceValue(2, "Summary Data");
				m.forceValue(14, "ASC");
				m.forceValue(17, "");
				m.forceValue(18, "DateTime");
				
				m.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m));
				
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L;f;", "L;m;" });
				m.addIterator(nameIterator);
					
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(recordedDateNameValidator);
				m.addValidator(lastTransferDateValidator);
				addFilterForUpdate(m, false);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateNameValidator);
				
				l.add(m);
			}
	
//			if (!searchWithSubdivision){
//				printSubdivisionException();
//			}
	
			// search by subdiv collected
			// cautarea dupa subdivizie trebuie facuta la sfirsitul cautarilor (ca
			// sa 'colectez' cit mai multe subdivizii), dar inainte de cautarea dupa
			// buyer ca sa nu caut dupa dupa subdiviziile de la buyer
//			String subdivName = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
//			boolean subdivIsMCN = MostCommonName.isMCLastName(subdivName) || MostCommonName.isMCFirstName(subdivName);
//			
//			if (subdivIsMCN){
//				SearchLogger.logWithServerName("I will not search with subdivision name = " + subdivName + " because is a most common last/first name", searchId, SearchLogger.ERROR_MESSAGE, getDataSite());
//			}
//			
			LegalDescriptionIterator ldiP = getLegalDescriptionIteratorForSubdivision(false);
//			
//			if (searchWithSubdivision && !subdivIsMCN && CompanyNameExceptions.allowed(subdivName, searchId, this)) 
			{
	
				DocsValidator noIndexingValidator = new NoIndexingInfoFilter(searchId).getValidator();
				
				// searching for plats - max interval
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_PLAT);
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CLEAR_VISITED_AND_VALIDATED_LINKS, true);
				m.addFilter(addressHighPassFilterResponse);
	
				m.clearSaKey(1);
				m.clearSaKey(3);
				m.clearSaKey(4);
				m.clearSaKey(6);
				m.clearSaKey(8);
				m.clearSaKey(10);
				m.clearSaKey(12);
				
				m.forceValue(2, "Detail Data");
				m.forceValue(13, DOC_TYPE_PLAT); // Plat -> val="8"
				m.forceValue(14, "ASC");
				m.forceValue(17, "");
				m.forceValue(18, "DateTime");
				
				
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67);
				m.addIterator(ldiP);
				m.addValidator(defaultLegalValidator);
				
				String[] docTypesPlatToPass = { 
						DocumentTypes.RESTRICTION, 
						DocumentTypes.PLAT, 
						DocumentTypes.EASEMENT, 
						DocumentTypes.CCER, 
						DocumentTypes.BY_LAWS };
				DocsValidator doctypeValidatorPlat = DoctypeFilterFactory.getDoctypeFilter(searchId, 0.8d, docTypesPlatToPass, FilterResponse.STRATEGY_TYPE_HIGH_PASS)
																									.getValidator();
				m.addValidator(doctypeValidatorPlat);
				// 3061 m.addValidator( addressHighPassValidator );
				m.addValidator(pinValidator);
				m.addValidator(recordedDateValidator);
				m.addValidator(subdivisionNameValidator);
	
				m.addCrossRefValidator(defaultLegalValidator);
				// 3061 m.addCrossRefValidator( addressHighPassValidator );
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				m.addCrossRefValidator(subdivisionNameValidator);
				m.addValidator(noIndexingValidator);
				m.addCrossRefValidator(noIndexingValidator);
				l.add(m);
	
				
				LegalDescriptionIterator ldiT = getLegalDescriptionIteratorForSubdivision(false);
				// searching for warranty_deeds
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.setVisible(true);
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_WARRANTY_DEED);
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CLEAR_VISITED_AND_VALIDATED_LINKS, true);
				m.addFilter(addressHighPassFilterResponse);
	
				m.addValidator(defaultLegalValidator);
				
				String[] docTypesWDToPass = { 
						DocumentTypes.RESTRICTION, 
						DocumentTypes.PLAT, 
						DocumentTypes.EASEMENT, 
						DocumentTypes.CCER, 
						DocumentTypes.TRANSFER };
				DocsValidator doctypeValidatorWD = DoctypeFilterFactory
						.getDoctypeFilter(searchId, 0.8d, docTypesWDToPass,FilterResponse.STRATEGY_TYPE_HIGH_PASS).getValidator();
				
				m.addValidator(doctypeValidatorWD);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(recordedDateValidator);
				m.addValidator(subdivisionNameValidator);
	
				m.clearSaKey(1);
				m.clearSaKey(3);
				m.clearSaKey(4);
				m.clearSaKey(6);
				m.clearSaKey(8);
				m.clearSaKey(10);
				m.clearSaKey(12);
				
				m.forceValue(2, "Detail Data");
				m.forceValue(13, DOC_TYPE_WARRANTY_DEED); // Warranty Deed -> val="3"
				m.forceValue(14, "ASC");
				m.forceValue(17, "");
				m.forceValue(18, "DateTime");
				
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67);
				m.addIterator(ldiT);
				
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				m.addCrossRefValidator(subdivisionNameValidator);
				m.addValidator(noIndexingValidator);
				m.addCrossRefValidator(noIndexingValidator);
				l.add(m);
			}
	
			LegalDescriptionIterator ldiSLU = getLegalDescriptionIteratorForSubdivision(false);
//			if (searchWithSubdivision) 
			{
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_UNIT);
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CLEAR_VISITED_AND_VALIDATED_LINKS, true);
				m.setVisible(true);
				m.addFilter(addressHighPassFilterResponse);
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(recordedDateValidator);
				m.addValidator(lastTransferDateValidator);
	
				m.clearSaKey(0);
				m.clearSaKey(1);
				m.clearSaKey(3);
				m.clearSaKey(4);
				m.clearSaKey(6);
				m.clearSaKey(8);
				m.clearSaKey(10);
				m.clearSaKey(12);
				
				m.forceValue(2, "Detail Data");
				m.forceValue(14, "ASC");
				m.forceValue(17, "");
				m.forceValue(18, "DateTime");
				
				m.forceValue(5, "Subdivision");
				m.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67);
				m.forceValue(7, "LotNumBegin");
				m.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_LOT_INTERVAL);
				m.forceValue(11, "UnitNum");
				m.setIteratorType(12, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_68);
				
				m.addIterator(ldiSLU);
				
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				l.add(m);
			}
			
			if (hasBuyer()){
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
				m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
					
				DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
	
				m.clearSaKey(3);
				m.clearSaKey(4);
				m.clearSaKey(6);
				m.clearSaKey(8);
				m.clearSaKey(10);
				m.clearSaKey(12);
					
				m.forceValue(2, "Summary Data");
				m.forceValue(14, "ASC");
				m.forceValue(17, "");
				m.forceValue(18, "DateTime");
				
				m.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, searchId, m));
				
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				ConfigurableNameIterator buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[] { "L;f;", "L;m;" });
				m.addIterator(buyerNameIterator);
				
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(recordedDateNameValidator);
				m.addValidator(buyerDoctypeValidator);
				addFilterForUpdate(m, false);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateNameValidator);
				
				l.add(m);
			}
	
			// OCR last transfer - instrument search
			{
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
				m.addValidator(recordedDateValidator);
				m.addValidator(defaultLegalValidator);
				
				m.clearSaKey(1);
				m.clearSaKey(2);
				
				m.forceValue(3, "Summary Data");
				m.forceValue(4, "ASC");
				
				m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
				m.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
				
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				l.add(m);
			}
			
			// OCR last transfer - book / page search		
			{
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
				m.addValidator(recordedDateValidator);
				m.addValidator(defaultLegalValidator);
				
				m.clearSaKey(0);
				
				m.forceValue(3, "Summary Data");
				m.forceValue(4, "ASC");
				
				m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
				m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
				m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
				
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				l.add(m);
			}
			
			//OCR names
			{
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
				m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				
				DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
					
				m.clearSaKey(3);
				m.clearSaKey(4);
				m.clearSaKey(6);
				m.clearSaKey(8);
				m.clearSaKey(10);
				m.clearSaKey(12);
				
				m.forceValue(2, "Summary Data");
				m.forceValue(14, "ASC");
				m.forceValue(17, "");
				m.forceValue(18, "DateTime");
				
				GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
				m.addFilter(nameFilter);
				
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				
				ArrayList<NameI> searchedNames = null;
				if (nameIterator != null) {
					searchedNames = nameIterator.getSearchedNames();
				} else {
					searchedNames = new ArrayList<NameI>();
				}
		
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, false,
																										new String[] {"L;f;", "L;m;"});
				// get your values at runtime
				nameIterator.setInitAgain(true);
				nameIterator.setSearchedNames(searchedNames);
				m.addIterator(nameIterator);
					
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				m.addValidator(pinValidator);
				m.addValidator(recordedDateNameValidator);
				m.addValidator(lastTransferDateValidator);
				addFilterForUpdate(m, false);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateNameValidator);
				
				l.add(m);
			}
			
			{
				InstrumentGenericIterator instrumentROIterator = new InstrumentGenericIterator(searchId, getDataSite());
				instrumentROIterator.enableInstrumentNumber();
				instrumentROIterator.setLoadFromRoLike(true);
				
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				//m.addValidator(pinValidator);
				m.addValidator(recordedDateValidator);
				m.setSaObjKey(SearchAttributes.INSTR_LIST);
				m.clearSaKey(1);
				m.clearSaKey(2);
				
				m.forceValue(3, "Summary Data");
				m.forceValue(4, "ASC");
				
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
				m.addIterator(instrumentROIterator);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				l.add(m);
			}
			{
				InstrumentGenericIterator bpROIterator = new InstrumentGenericIterator(searchId, getDataSite());
				bpROIterator.enableBookPage();
				bpROIterator.setLoadFromRoLike(true);
				
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
				m.addValidator(defaultLegalValidator);
				m.addValidator(addressHighPassValidator);
				//m.addValidator(pinValidator);
				m.addValidator(recordedDateValidator);
				m.setSaObjKey(SearchAttributes.INSTR_LIST);
				m.clearSaKey(0);
				
				m.forceValue(3, "Summary Data");
				m.forceValue(4, "ASC");
				
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
				m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
				m.addIterator(bpROIterator);
				m.addCrossRefValidator(defaultLegalValidator);
				m.addCrossRefValidator(addressHighPassValidator);
				m.addCrossRefValidator(pinValidator);
				m.addCrossRefValidator(recordedDateValidator);
				l.add(m);
			}
		
		}
		serverInfo.setModulesForAutoSearch(l);
	}

	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId, true, true).getValidator();
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();

		SearchAttributes sa = getSearchAttributes();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()){

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			
			module.clearSaKey(3);
			module.clearSaKey(4);
			module.clearSaKey(6);
			module.clearSaKey(8);
			module.clearSaKey(10);
			module.clearSaKey(12);
			
			module.forceValue(2, "Summary Data");
			module.forceValue(14, "ASC");
			module.forceValue(17, "");
			module.forceValue(18, "DateTime");
			
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			String date = gbm.getDateForSearch(id, "MMM dd, yyyy", searchId);
			if (date != null){
				module.getFunction(15).forceValue(date);
			}
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
																									new String[] { "L;F;" });
			module.addIterator(nameIterator);
			module.addValidator(defaultLegalValidator);
			module.addValidator(addressHighPassValidator);
			module.addValidator(pinValidator);
			module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
			module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());

			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				
				module.clearSaKey(3);
				module.clearSaKey(4);
				module.clearSaKey(6);
				module.clearSaKey(8);
				module.clearSaKey(10);
				module.clearSaKey(12);
				
				module.forceValue(2, "Summary Data");
				module.forceValue(14, "ASC");
				module.forceValue(17, "");
				module.forceValue(18, "DateTime");
				
				module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				date = gbm.getDateForSearchBrokenChain(id, "MMM dd, yyyy", searchId);
				if (date != null){
					module.getFunction(15).forceValue(date);
				}
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
																										new String[] { "L;F;" });
				module.addIterator(nameIterator);
				module.addValidator(defaultLegalValidator);
				module.addValidator(addressHighPassValidator);
				module.addValidator(pinValidator);
				module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
				module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());

				modules.add(module);
			}
		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);

	}
	
	private LegalDescriptionIterator getLegalDescriptionIteratorForSubdivision(boolean lookupWasDoneWithName) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookupWasDoneWithName, false, getDataSite()) {

			private static final long serialVersionUID = -4741635379234782109L;
			
			private Map<LegalStruct, Integer> initialDocumentsBeforeRunningStruct = new HashMap<LegalStruct, Integer>();
			private Set<String> allSubdivisionNames = new HashSet<String>();
			
			public List<DocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch, int siteType){
				final List<DocumentI> ret = new ArrayList<DocumentI>();
				
				List<RegisterDocumentI> listRodocs = m.getRealRoLikeDocumentList();
				DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
				
				SearchAttributes sa	= search.getSa();
				PartyI owner 		= sa.getOwners();
				
				for (RegisterDocumentI doc : listRodocs){
					boolean found = false;
					for (PropertyI prop : doc.getProperties()){
						if (((doc.isOneOf("MORTGAGE", "TRANSFER", "RELEASE") || isTransferAllowed(doc)) && applyNameMatch)
								|| ((doc.isOneOf("MORTGAGE") || isTransferAllowed(doc)) && !applyNameMatch)){
							if (prop.hasSubdividedLegal()){
								SubdivisionI sub = prop.getLegal().getSubdivision();
								LegalStruct lglStruct = new LegalStruct(false);
				
								lglStruct.setAddition(sub.getName());
								lglStruct.setLot(sub.getLot());
								lglStruct.setUnit(sub.getUnit());
				
								boolean nameMatched = false;
				
								if (applyNameMatch){
									if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) 
											|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)
											|| GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) 
											|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
										nameMatched = true;
									}
								}
				
								if ((nameMatched || !applyNameMatch)
										&& StringUtils.isNotEmpty(lglStruct.getAddition())
										&& (StringUtils.isNotEmpty(lglStruct.getUnit()) || StringUtils.isNotEmpty(lglStruct.getLot()))){
									ret.add(doc);
									found = true;
									break;
								}
							}
						}
					}
					if (found){
						break;
					}	
				}
				return ret;
			}
			
			@SuppressWarnings("unchecked")
			protected List<DocumentI> loadLegalFromRoDocs(Search global, DocumentsManagerI m) {
				List<DocumentI> listRodocs = new ArrayList<DocumentI>();
				
				if (AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments())) {
					List<DocumentI> listRodocsSaved = (List<DocumentI>) global.getAdditionalInfo(getCheckAlreadyFilledKeyWithDocuments());
					if (listRodocsSaved != null && !listRodocsSaved.isEmpty()) {
						listRodocs.addAll(listRodocsSaved);
					}
				}
				legalStruct = new HashSet<LegalStruct>();
				
				if (listRodocs.isEmpty()) {
					if (getRoDoctypesToLoad() == null) {
						listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(m, global, true, GWTDataSite.RO_TYPE));
						if (listRodocs.isEmpty()){
							listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(m, global, false, GWTDataSite.RO_TYPE));
						}
					} else {
						listRodocs.addAll(m.getDocumentsWithDocType(true, getRoDoctypesToLoad()));
					}
					if (AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(getCheckAlreadyFilledKeyWithDocuments())) {
						global.setAdditionalInfo(getCheckAlreadyFilledKeyWithDocuments(), listRodocs);	
					}
				}
				if (listRodocs.isEmpty()){
					try {
						SearchAttributes sa = getSearchAttributes();
						String subdivision = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
						String unit = sa.getAtribute(SearchAttributes.LD_SUBDIV_UNIT);
						if (org.apache.commons.lang.StringUtils.isNotEmpty(subdivision) && org.apache.commons.lang.StringUtils.isNotEmpty(unit)) {
							LegalStruct lglStruct = new LegalStruct(false);
							lglStruct.setAddition(subdivision);
							lglStruct.setUnit(unit);
							lglStruct.setLot(sa.getAtribute(SearchAttributes.LD_LOTNO));

							legalStruct.add(lglStruct);
						}
					} catch (Exception e) {
					}
				} else{
					DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
					
					for (DocumentI reg : listRodocs){
						if (!reg.isOneOf(
								DocumentTypes.PLAT,
								DocumentTypes.RESTRICTION,
								DocumentTypes.EASEMENT,
								DocumentTypes.MASTERDEED,
								DocumentTypes.COURT,
								DocumentTypes.LIEN,
								DocumentTypes.CORPORATION,
								DocumentTypes.AFFIDAVIT,
								DocumentTypes.CCER,
								DocumentTypes.TRANSFER)
									
								|| isTransferAllowed(reg)){
							for (PropertyI prop : reg.getProperties()){
								if (prop.hasLegal()){
									LegalI legal = prop.getLegal();
									treatLegalFromSavedDocument(reg.prettyPrint(), legal, true, null);
								}
							}
						}
					}
				}
				legalStruct = keepOnlyGoodLegals(legalStruct);
				
				if (legalStruct.size() == 1){
					for (LegalStruct str : legalStruct){
						if (org.apache.commons.lang.StringUtils.isNotEmpty(str.getAddition())){
							allSubdivisionNames.add(str.getAddition());
						}
					}
				}
				if (legalStruct.isEmpty()){
					setLoadFromSearchPage(false);
					setLoadFromSearchPageIfNoLookup(false);
				}
				return listRodocs;
			}
			
			protected int getDocumentsManagerDocSize() {
				DocumentsManagerI docManager = getSearch().getDocManager();
				int newSiteDocuments = 0;
				try {
					docManager.getAccess();
					newSiteDocuments = docManager.getDocumentsWithDataSource(false, dataSite.getSiteTypeAbrev()).size();
				} finally {
					docManager.releaseAccess();
				}
				return newSiteDocuments;
			}
						
			@Override
			protected void loadDerrivation(TSServerInfoModule module, LegalStruct str) {
				
				if (!initialDocumentsBeforeRunningStruct.containsKey(str)) {
					initialDocumentsBeforeRunningStruct.put(str, getDocumentsManagerDocSize());
				}
				
				for (Object functionObject : module.getFunctionList()) {
					if (functionObject instanceof TSServerInfoFunction) {
						TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
						switch (function.getIteratorType()) {
							case FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67:
								function.setParamValue(org.apache.commons.lang.StringUtils.defaultString(str.getAddition()));
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_GENERIC_68:
								function.setParamValue(org.apache.commons.lang.StringUtils.defaultString(str.getUnit()));
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_LOT_INTERVAL:
								function.setParamValue(org.apache.commons.lang.StringUtils.defaultString(str.getLot()));
								break;
						}
					}
				}
			}
			
			@Override
			public void processSubdivisionName(Search global, String originalLot, String originalBlock,
					String originalUnit, Set<String> temporarySubdivisionsForCondoSearch) {
			}
			@Override
			protected void processSubdivisionLotBlock(String subdivName, String lot, String block) {
			}
			@Override
			protected void processSubdivisionTractPlatBookPage(String subdivName, String platBook, String platPage,
					String tract) {
			}
			
			@Override
			protected String treatOnlySubdividedLegal(String sourceKey, LegalI legal,
					boolean useAlsoSubdivisionName, String subdivisionName, Set<PlatBookPage> platBookPageFromUser) {
				
				if (isEnableSubdividedLegal() && legal.hasSubdividedLegal()){
					SubdivisionI subdiv = legal.getSubdivision();

					String unit = subdiv.getUnit();
					String lot = subdiv.getLot();
										
					LegalStruct legalStruct1 = new LegalStruct(false);
					
					if (useAlsoSubdivisionName) {
						subdivisionName = subdiv.getName();
						if (StringUtils.isNotEmpty(subdivisionName) && (StringUtils.isNotEmpty(unit) || StringUtils.isNotEmpty(lot))){
							
							legalStruct1.setAddition(subdivisionName);
							legalStruct1.setUnit(unit);
							legalStruct1.setLot(lot);
								
							saveSubdivisionName(subdivisionName);
							legalStruct.add(legalStruct1);
						}
					}
				}
				return subdivisionName;
			}
			
			@Override
			public Set<LegalStruct> keepOnlyGoodLegals(Set<LegalStruct> legals){
				Set<LegalStruct> good = new HashSet<LegalStruct>();
				for (LegalStruct str : legals){
					if (!incompleteData(str)){
						good.add(str);
					}
				}
				return good;
			}
			
			private boolean incompleteData(LegalStruct str){
				
				if (str == null){
					return true;
				}
				boolean emptySubdivisionName = StringUtils.isEmpty(str.getAddition());
				boolean emptyUnit = StringUtils.isEmpty(str.getUnit());
				boolean emptyLot = StringUtils.isEmpty(str.getLot());
				
				return (emptySubdivisionName && (emptyUnit || emptyLot));
			}
			
			@Override
			public boolean isTransferAllowed(RegisterDocumentI doc) {
				
				if (doc != null && doc.isOneOf(DocumentTypes.TRANSFER)) {
					String[] realTransferSubcategories = DocumentTypes.getRealTransferSubcategories(
							Integer.parseInt(getSearch().getStateId()), 
							Integer.parseInt(getSearch().getCountyId()));
					if (doc.isOneOfSubcategory(realTransferSubcategories)) {
						return true;
					}
				}
				return false;
			}
		};
		
		it.setEnableTownshipLegal(false);
		it.setEnableSubdividedLegal(true);
		it.setEnableSubdivision(true);
		it.setLoadFromSearchPage(false);
		it.setLoadFromSearchPageIfNoLookup(false);
		
		return it;
	}

	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		List<TSServerInfoModule> modules = getMultipleModules(module, sd);
		
		if (modules.size() > 1){
			List<ServerResponse> serverResponses = new ArrayList<ServerResponse>();
			Vector<ParsedResponse> prs = new Vector<ParsedResponse>();
			boolean firstSearchBy = true;
			String header = "", footer = "";
			for (TSServerInfoModule mod : modules){
				if (verifyModule(mod)){
					if (firstSearchBy){
						firstSearchBy = false;
					} else{
						mod.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, true);
					}
					ServerResponse res = super.SearchBy(mod, sd);
					if (res != null){
						res.getParsedResponse().setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, mod);
						serverResponses.add(res);
						if (res.getParsedResponse().getResultRows().size() > 0){
							prs.addAll(res.getParsedResponse().getResultRows());
							header = res.getParsedResponse().getHeader();
							footer = res.getParsedResponse().getFooter();
						}
					}
				}
			}
			if (prs.size() > 0){
				ServerResponse serverResponse = new ServerResponse();
				serverResponse.getParsedResponse().setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, module);
				serverResponse.getParsedResponse().setResultRows(prs);
				serverResponse.setResult("");
				serverResponse.getParsedResponse().setHeader(header);
				serverResponse.getParsedResponse().setFooter(footer);
				solveHtmlResponse("", module.getParserID(), "SearchBy", serverResponse, serverResponse.getResult());
				
				return serverResponse;
			} else{
				return ServerResponse.createEmptyResponse();
			}
		} else{
			
			TSServerInfoModule secondModuleGhertzoiala = null;
			boolean bResetQueryParam = true;

			if (sd instanceof SearchDataWrapper) {
				msiServerInfo.getModule(miDateModule).setData((SearchDataWrapper) sd);
			}
			switch (module.getModuleIdx()) {
			
				case TSServerInfo.ARCHIVE_DOCS_MODULE_IDX:
					secondModuleGhertzoiala = getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.SECOND_ARCHIVE_DOCS_MODULE_IDX, (SearchDataWrapper)sd);
					break;
				case TSServerInfo.SECOND_ARCHIVE_DOCS_MODULE_IDX:
					secondModuleGhertzoiala = module;
					module = getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.ARCHIVE_DOCS_MODULE_IDX, (SearchDataWrapper)sd);
					break;
			}
			if (secondModuleGhertzoiala != null) {
				ServerResponse serverResponseOne = super.SearchBy(bResetQueryParam, module, sd);
				ServerResponse serverResponseTwo = super.SearchBy(bResetQueryParam, secondModuleGhertzoiala, sd);
				
				return mergeResponses(serverResponseTwo, serverResponseOne );
			} else {
				return super.SearchBy(bResetQueryParam, module, sd);
			}
		}
       
	}

	private boolean verifyModule(TSServerInfoModule mod) {
    	
    	if (mod == null)
    		return false;
    	
    	if (mod.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
    		if (mod.getFunctionCount() > 2 
    				&& (StringUtils.isNotEmpty(mod.getFunction(0).getParamValue())
    						|| (StringUtils.isNotEmpty(mod.getFunction(1).getParamValue()) && StringUtils.isNotEmpty(mod.getFunction(2).getParamValue())))) {
				return true;
			} 
			return false;
		}
    	if (mod.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
    		if (mod.getFunctionCount() > 0 && StringUtils.isNotEmpty(mod.getFunction(0).getParamValue())) {
				return true;
			} 
			return false;
		}
    	System.err.println(this.getClass() + "I shouldn't be here!!!");
		return false;
	}

	private ServerResponse mergeResponses(ServerResponse serverResponseOne, ServerResponse serverResponseTwo) {
		String responseOneString = serverResponseOne.getParsedResponse().getResponse().replaceAll("<html><body[^>]>", "").replaceFirst("</body></html>", "");
		String responseTwoString = serverResponseTwo.getParsedResponse().getResponse().replaceAll("<html><body[^>]>", "").replaceFirst("</body></html>", "");
		
		String fullResponseString = responseOneString + responseTwoString ;
		serverResponseOne.setResult(fullResponseString);
		serverResponseOne.getParsedResponse().setOnlyResponse(fullResponseString);
		
		return serverResponseOne;
	}

	/* Pretty prints a link that was already followed when creating TSRIndex
 * (non-Javadoc)
 * @see ro.cst.tsearch.servers.types.TSServer#getPrettyFollowedLink(java.lang.String)
 */	
    public String getPrettyFollowedLink (String initialFollowedLnk){
    	if (initialFollowedLnk.matches("(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)[&=a-z]+crossRefSource[=]([a-z]*)[^a-z]*.*")){
    		String retStr = initialFollowedLnk.replaceFirst(
					    				"(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)[&=a-z]+crossRefSource[=]([a-z]*)[^a-z]*.*", 
					    				"Instrument " + "$2" +
					    				" has already been processed from a previous search in the log file.");
    		retStr =  "<br/><span class='followed'>" + retStr + "</span><br/>";
    		
    		return retStr;
    	}else if (initialFollowedLnk.matches("(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)[^&]*.*")){
    		String retStr = initialFollowedLnk.replaceFirst(
						    				"(?i).*[^a-z]+(instrs|marginals)[=]([0-9]+)[^&]*.*", 
						    				"Instrument " + "$2" + 
						    				" has already been processed from a previous search in the log file.");
    		retStr =  "<br/><span class='followed'>" + retStr + "</span><br/>";
    		
    		return retStr;
    	}
    	
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
	
	/**
	 * @see TSInterface#GetLink(java.lang.String) the only link for nashvile is
	 *      the link to file.
	 */
	public ServerResponse GetLink(String vsRequest, boolean vbEncoded)
			throws ServerResponseException {
		ServerResponse rtnResponse;
		if (vsRequest.indexOf("LoadImage") != -1) {
			// try to get the file
			// request file
			getTSConnection().setHostName("www.registerofdeeds.nashville.org");
			getTSConnection().setHostIP("170.190.30.10");

			getTSConnection().SetReferer(FILE_REFERER);
			rtnResponse = super.GetLink(vsRequest, vbEncoded);
			getTSConnection().SetReferer("");
			getTSConnection().setHostName(msiServerInfo.getServerAddress());
			getTSConnection().setHostIP(msiServerInfo.getServerIP());
		} else {
			getTSConnection().SetReferer(FILE_REFERER);
			boolean containsIndexSearch = vsRequest.contains("/recording/SPVIndexSearch.asp");
			boolean containsBookSearch = vsRequest.contains("/recording/SPVBookSearch.asp");
			if (containsIndexSearch || containsBookSearch) {
				ServerResponse responseIndex = null;
				ServerResponse responseBook = null;
				if (containsIndexSearch) {
					responseIndex = super.GetLink(vsRequest, vbEncoded);
					responseBook = super.GetLink(vsRequest.replaceAll("SPVIndexSearch", "SPVBookSearch"), vbEncoded);
				} else if (containsBookSearch) {
					responseIndex = super.GetLink(vsRequest.replaceAll("SPVBookSearch", "SPVIndexSearch"), vbEncoded);
					responseBook = super.GetLink(vsRequest, vbEncoded);
				}
				rtnResponse = mergeResponses(responseIndex, responseBook);
			} else {
				rtnResponse = super.GetLink(vsRequest, vbEncoded);
			}
			getTSConnection().SetReferer("");
		}

		return rtnResponse;
	}

	public static String cleanResp(String resp) {
    	if (!StringUtils.isEmpty(resp)) {
    		 resp = resp.replaceAll("\\r\\n", "");
             //Apartments Condominiums
    		 resp = resp.replaceAll("(?i)Apartments|Condominiums", "");
             //cut all until the first hr
             int i = resp.indexOf("<hr>");
             if (i > -1) {
            	 resp = resp.substring(i);
             }
            	
             int j = resp.indexOf(TABLE_TEXT);
             //keep all the information until the last table and cut all from here
             if (j>-1) {
            	 resp = resp.substring( 0, j); 
             }
		}
    	return resp;
    }
	
	/**
	 * @param rsResponce
	 * @param viParseID
	 */
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponce = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		{
			if (rsResponce.indexOf("could not be found") >= 0) {
				Response.getParsedResponse()
						.setError("The image could not be found. Most likely this is because the document has never been scanned " 
									+ "into the computer system due to its age. To see this document you will need go to the office of " 
									+ "the Davidson County Register of Deeds.");
				throw new ServerResponseException(Response);
			}
			if (rsResponce.matches("(?is).*The selection criteria was too general and would have returned \\d+ records.*")) {
				Response.getParsedResponse().setError("The selection criteria was too general and would have returned too many records");
				return;
			}			
			if (rsResponce.indexOf("THE MINIMUM SEARCH CRITERIA WAS NOT PROVIDED") >= 0) {
				return;
			}
			if (rsResponce.indexOf("NO RECORDS MATCH THE SPECIFIED SEARCH CRITERIA") >= 0) {
				return;
			}
			if (rsResponce.indexOf("Error Retrieving Detail Data") >= 0) {
				return;
			}
			if (rsResponce.indexOf("Error Trapped") >= 0){ // fix for a parser bug reported by ATS on 10/30/2006
				return;
			}
			if (rsResponce.indexOf("SECURITY VIOLATION DETECTED") > -1) {

				loggerCookie.debug("before enter synchronized region " + cookie);
				synchronized (cookie) {
					loggerCookie.debug("after enter synchronized region " + cookie);
					cookie.resetValue();
					loggerCookie.debug("before exit synchronized region" + cookie);
				}
				loggerCookie.debug("after exit synchronized region" + cookie);

				CookieManager.addCookie(Integer.toString(miServerID), null);

				Response.getParsedResponse()
						.setError("SECURITY VIOLATION DETECTED"
										+ "<br>More than one user is currently logged on with this UserID "
										+ "which is a violation of the application's security.");
				throw new ServerResponseException(Response);
			}
			if (rsResponce.indexOf("NO RECORDS RETRIEVED") >= 0) {
				return;
			}
			if (rsResponce.indexOf("YOU CAN NOW VIEW PAGES FROM BOOK") >= 0) {
				return;
			}

			if (rsResponce.indexOf("Search Criteria") > -1) {
        		
        		if (Response.getRawQuerry().toLowerCase().indexOf("submit=detail+data") > -1){
        			ParseResponse(sAction, Response, ID_DETAILS);
        			return;
        		}
        		rsResponce = cleanResp(rsResponce);
				
        		if (Response.getRawQuerry().indexOf("Names+Summary") > -1){
        			try {
						StringBuilder outputTable = new StringBuilder();
						Collection<ParsedResponse> smartParsedResponses = smartParseNamesSummary(Response, rsResponce, outputTable);
												
						if (smartParsedResponses.size() > 0) {
							parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
							parsedResponse.setOnlyResponse(outputTable.toString());
			            }
						
					} catch(Exception e) {
						e.printStackTrace();
					}
        		} else{
        			try {
						StringBuilder outputTable = new StringBuilder();
						Collection<ParsedResponse> smartParsedResponses = smartParseSummaryData(Response, rsResponce, outputTable, false);
												
						if (smartParsedResponses.size() > 0) {
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
		case ID_DETAILS:
        	if (rsResponce.indexOf("Search Criteria") > -1) {
        		
        		rsResponce = cleanResp(rsResponce);
        		//remove links to AO 
        		rsResponce = rsResponce.replaceAll("(?is)\\b(PrpId:\\s*)<a[^>]*>(.*?)</a>", "$1$2");
        		try {
					 
					StringBuilder outputTable = new StringBuilder();
					Collection<ParsedResponse> smartParsedResponses = smartParseDetailData(Response, rsResponce, outputTable);
					
					if (smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
		            }
					
				} catch(Exception e) {
					e.printStackTrace();
				}
        	}
        	
        	break;
        case ID_SAVE_TO_TSD:
        	
        	if (rsResponce.toLowerCase().contains("<html")){
        		ParseResponse(sAction, Response, ID_DETAILS);
        	}
        	DocumentI document = parsedResponse.getDocument();
        	
        	if (document == null){
	        	if (Response.getParsedResponse().getResultRows().size() == 1){
	        		parsedResponse = (ParsedResponse) Response.getParsedResponse().getResultRows().get(0);
	        		Response.setParsedResponse(parsedResponse);
	        		document = parsedResponse.getDocument();
	        	} else{
	        		return;
	        	}
        	}

			if (document != null) {
				msSaveToTSDFileName = document.getId() + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
			}
        	break;
		case ID_BROWSE_SCANNED_INDEX_PAGES:
			if (rsResponce.indexOf("SECURITY VIOLATION DETECTED") > -1) {
				return;
			}
			rsResponce = rsResponce.replaceAll("(?is)<script.*?</script>", "");
			rsResponce = rsResponce.replaceAll("onclick='PGCLK\\(\\)'", "");
			//rsResponce = rsResponce.replaceAll("onchange='BT\\(\\)'", "");
			rsResponce = rsResponce
					.replaceAll(
							"<html><body BACKGROUND[^>]*>",
							"<body>"
									+ "<h2>Davidson Archive Search</h2>"
									+ "<script language='javascript'>"
									
									+ "function BT() {\n"
									+ "	var frmNdx = document.frmBooks;\n"
									+ "var i = frmNdx.BookType.selectedIndex;\n"
									// + "alert(frmNdx.searchId.value);\n"
									+ "var k = frmNdx.searchId.value;\n"
									+ "var stri = \"&BookType=\" + frmNdx.BookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strk = \"&searchId=\" + k;\n"
									+ "var strURL = \"URLConnectionReader?p1=019&p2=1\";\n"
									
									
									+ "	var frmIndex = document.frmIndexes;\n"
									
									+ "var strIndexi = ''; " +
									" if (frmIndex.IndexBookType) {" +
										" var i = frmIndex.IndexBookType.selectedIndex;\n" + 
										" if( i>= 0 &&  frmIndex.IndexBookType.options[i]) {" + 
										"    strIndexi = \"&IndexBookType=\" + frmIndex.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
									+ "var strIndexj = ''; " +
									" if (frmIndex.IndexDateRange) {" +
										" var j = frmIndex.IndexDateRange.selectedIndex;\n" + 
										" if( j>=0 && frmIndex.IndexDateRange.options[j]) {" +
										"    strIndexj = \"&IndexDateRange=\" + frmIndex.IndexDateRange.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
										
									+ "var strIndexm = '';" +
									" if (frmIndex.IndexAlphaRange) {" +
										" var m = frmIndex.IndexAlphaRange.selectedIndex;\n" +
										" if( m>=0 && frmIndex.IndexAlphaRange.options[m]) {" +
										"    strIndexm = \"&IndexAlphaRange=\" + frmIndex.IndexAlphaRange.options[m].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
										
									+ "var strIndexl = '';" +
									" if (frmIndex.IndexPageRange) {" +
										" var l = frmIndex.IndexPageRange.selectedIndex;\n" +
										" if( l>=0 && frmIndex.IndexPageRange.options[l]) {" +
										"    strIndexl = \"&IndexPageRange=\" + frmIndex.IndexPageRange.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
									
									
									// +
									// "alert(\"Opening\" + strURL + strk + \"&ActionType=2&Link=/recording/SPVBookSearch.asp\" + stri + strj)\n"
									+ "window.location.href = strURL + strk + \"&ActionType=2&Link=/recording/SPVBookSearch.asp\" + stri + strIndexi + strIndexj + strIndexm + strIndexl;\n"
									+ "}\n"
									
									+ "function BK() {\n"
									+ "	var frmNdx = document.frmBooks;\n"
									+ "var i = frmNdx.BookType.selectedIndex;\n"
									+ "var j = frmNdx.Book.selectedIndex;\n"
									// + "alert(frmNdx.searchId.value);\n"
									+ "var k = frmNdx.searchId.value;\n"
									+ "var stri = \"&BookType=\" + frmNdx.BookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strj = \"&Book=\" + frmNdx.Book.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strk = \"&searchId=\" + k;\n"
									+ "var strURL = \"URLConnectionReader?p1=019&p2=1\";\n"
									
									+ "	var frmIndex = document.frmIndexes;\n"
									
									+ "var strIndexi = ''; " +
									" if (frmIndex.IndexBookType) {" +
										" var i = frmIndex.IndexBookType.selectedIndex;\n" + 
										" if( i>= 0 &&  frmIndex.IndexBookType.options[i]) {" + 
										"    strIndexi = \"&IndexBookType=\" + frmIndex.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
									+ "var strIndexj = ''; " +
									" if (frmIndex.IndexDateRange) {" +
										" var j = frmIndex.IndexDateRange.selectedIndex;\n" + 
										" if( j>=0 && frmIndex.IndexDateRange.options[j]) {" +
										"    strIndexj = \"&IndexDateRange=\" + frmIndex.IndexDateRange.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
										
									+ "var strIndexm = '';" +
									" if (frmIndex.IndexAlphaRange) {" +
										" var m = frmIndex.IndexAlphaRange.selectedIndex;\n" +
										" if( m>=0 && frmIndex.IndexAlphaRange.options[m]) {" +
										"    strIndexm = \"&IndexAlphaRange=\" + frmIndex.IndexAlphaRange.options[m].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
										
									+ "var strIndexl = '';" +
									" if (frmIndex.IndexPageRange) {" +
										" var l = frmIndex.IndexPageRange.selectedIndex;\n" +
										" if( l>=0 && frmIndex.IndexPageRange.options[l]) {" +
										"    strIndexl = \"&IndexPageRange=\" + frmIndex.IndexPageRange.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
									
									// +
									// "alert(\"Opening\" + strURL + strk + \"&ActionType=2&Link=/recording/SPVBookSearch.asp\" + stri + strj)\n"
									+ "window.location.href = strURL + strk + \"&ActionType=2&Link=/recording/SPVBookSearch.asp\" + stri + strj + strIndexi + strIndexj + strIndexm + strIndexl;\n"
									+ "}\n"

									+ "function PG() {\n"
									+ "	var frmNdx = document.frmBooks;\n"
									+ "var i = frmNdx.BookType.selectedIndex;\n"
									+ "var j = frmNdx.Book.selectedIndex;\n"
									+ "var l = frmNdx.Page.selectedIndex;\n"
									// + "alert(frmNdx.searchId.value);\n"
									+ "var k = frmNdx.searchId.value;\n"
									+ "var stri = \"&BookType=\" + frmNdx.BookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strj = \"&Book=\" + frmNdx.Book.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strl = \"&Page=\" + frmNdx.Page.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strk = \"&searchId=\" + k;\n"
									+ "var strURL = \"URLConnectionReader?p1=019&p2=1\";\n"
									
									+ "	var frmIndex = document.frmIndexes;\n"
									
									+ "var strIndexi = ''; " +
									" if (frmIndex.IndexBookType) {" +
										" var i = frmIndex.IndexBookType.selectedIndex;\n" + 
										" if( i>= 0 &&  frmIndex.IndexBookType.options[i]) {" + 
										"    strIndexi = \"&IndexBookType=\" + frmIndex.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
									+ "var strIndexj = ''; " +
									" if (frmIndex.IndexDateRange) {" +
										" var j = frmIndex.IndexDateRange.selectedIndex;\n" + 
										" if( j>=0 && frmIndex.IndexDateRange.options[j]) {" +
										"    strIndexj = \"&IndexDateRange=\" + frmIndex.IndexDateRange.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
										
									+ "var strIndexm = '';" +
									" if (frmIndex.IndexAlphaRange) {" +
										" var m = frmIndex.IndexAlphaRange.selectedIndex;\n" +
										" if( m>=0 && frmIndex.IndexAlphaRange.options[m]) {" +
										"    strIndexm = \"&IndexAlphaRange=\" + frmIndex.IndexAlphaRange.options[m].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
										
									+ "var strIndexl = '';" +
									" if (frmIndex.IndexPageRange) {" +
										" var l = frmIndex.IndexPageRange.selectedIndex;\n" +
										" if( l>=0 && frmIndex.IndexPageRange.options[l]) {" +
										"    strIndexl = \"&IndexPageRange=\" + frmIndex.IndexPageRange.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
									"}\n"
									
									// ++ strIndexi + strIndexj + strIndexm + strIndexl
									// "alert(window.document.forms[0].Page.options[window.document.forms[0].Page.selectedIndex].text);\n" 
									+ "open(strURL + strk + \"&ActionType=2&Link=/recording/LoadImage.asp\" + stri + strj + strl );\n"
									+ "}\n"
									
									+ "function SaveTSDBooks() {\n"
									+ "var rPageSelect = document.frmBooks.Page.selectedIndex;\n"

									+ "if (rPageSelect == -1) {\n"
									+ "    alert(\"Cannot save! No page selected!\");\n"
									+ "    }\n"
									+ "else {\n"
									+ "    document.frmBooks.realPage.value=document.frmBooks.Page.options[rPageSelect].text; " 
									+ "    rPageSelect = document.frmIndexes.IndexBookType.selectedIndex;\n"
									+ "    if(rPageSelect >= 0) {" +
										"  document.frmBooks.IndexBookType.value=document.frmIndexes.IndexBookType.options[rPageSelect].text; " +
									"}" 
									+ "    document.frmBooks.submit();"
									+ "    }\n" + "}\n" + "</script>");

			rsResponce = rsResponce.replaceAll("action='SPVBookSearch\\.asp'", "action='MultiDocSave'");

			rsResponce = rsResponce.replaceAll("</form></body></html>", "")
					+ "<br></br><input name=\"Button\" type=\"button\" class=\"button\" value=\"Save Selected Book-Page to TSD\" onClick=\"SaveTSDBooks();\">"
					+ "<input type=\"hidden\" name=\"searchId\" value=\""
					+ searchId
					+ "\">"
					+ "<input type=\"hidden\" name=\"realPage\" />"
					+ "<input type=\"hidden\" name=\"IndexBookType\" />"
					+ "<input type=\"hidden\" name=\"serverId\" value=\"19\"/> \n</form>";
			parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);
			break;
			
		case ID_SEARCH_BY_MODULE19:
			if (rsResponce.indexOf("SECURITY VIOLATION DETECTED") > -1) {
				return;
			}
			rsResponce = rsResponce.replaceAll("(?is)<script.*?</script>", "");
			rsResponce = rsResponce.replaceAll("onclick='IPRCLK\\(\\)'", "");
			//rsResponce = rsResponce.replaceAll("onchange='BT\\(\\)'", "");
			rsResponce = rsResponce
					.replaceAll(
							"<html><body BACKGROUND[^>]*>",
							"<body>"
									+ "<script language='javascript'>"
									
									+ "function IBT() {\n"
									+ "	var frmNdx = document.frmIndexes;\n"
									+ "var i = frmNdx.IndexBookType.selectedIndex;\n"
									// + "alert(frmNdx.searchId.value);\n"
									+ "var k = frmNdx.searchId.value;\n"
									+ "var stri = \"&IndexBookType=\" + frmNdx.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strk = \"&searchId=\" + k;\n"
									+ "var strURL = \"URLConnectionReader?p1=019&p2=1\";\n"
									
									
									+ "	var frmBooks = document.frmBooks;\n"
									+ "var ifrmBooks = frmBooks.BookType.selectedIndex;\n"
									+ "var jfrmBooks = frmBooks.Book.selectedIndex;\n"
									+ "var lfrmBooks = frmBooks.Page.selectedIndex;\n"
									// + "alert(frmNdx.searchId.value);\n"
									
									
									+ "var kfrmBooks = frmBooks.searchId.value;\n" + 
									" var strifrmBooks = ''; " +
										"if (frmBooks.BookType) {" +
											" var i = frmBooks.BookType.selectedIndex;\n" + 
											" if( i >= 0 && frmBooks.BookType.options[i]) {\n" + 
											"    strifrmBooks = \"&BookType=\" + frmBooks.BookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\"); " +
											" }" +
										"}\n" + 
									" var strjfrmBooks = ''; " +
										"if (frmBooks.Book) {" +
											"var j = frmBooks.Book.selectedIndex;\n" + 
											" if( j >= 0 && frmBooks.Book.options[j]) {\n" +
											"    strjfrmBooks = \"&Book=\" + frmBooks.Book.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
											" }" +
										"}\n" +
									" var strlfrmBooks = ''; " +
										"if (frmBooks.Page) {" +
										" var l = frmBooks.Page.selectedIndex;\n" + 
										" if ( l >= 0 && frmBooks.Page.options[l]) {\n" +
											"    strlfrmBooks = \"&Page=\" + frmBooks.Page.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
										"}\n"
									+ "var strkfrmBooks = \"&searchId=\" + k;\n"
									+ "var strURL = \"URLConnectionReader?p1=019&p2=1\";\n"
									
									// +
									// "alert(\"Opening\" + strURL + strk + \"&ActionType=2&Link=/recording/SPVBookSearch.asp\" + stri + strj)\n"
									+ "window.location.href = strURL + strk + \"&ActionType=2&Link=/recording/SPVIndexSearch.asp\" + stri + strifrmBooks + strjfrmBooks + strlfrmBooks;\n"
									+ "}\n"
									
									+ "function IDR() {\n"
									+ "	var frmNdx = document.frmIndexes;\n"
									+ "var i = frmNdx.IndexBookType.selectedIndex;\n"
									+ "var j = frmNdx.IndexDateRange.selectedIndex;\n"
									// + "alert(frmNdx.searchId.value);\n"
									+ "var k = frmNdx.searchId.value;\n"
									+ "var stri = \"&IndexBookType=\" + frmNdx.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strj = \"&IndexDateRange=\" + frmNdx.IndexDateRange.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strk = \"&searchId=\" + k;\n"
									
									+ "	var frmBooks = document.frmBooks;\n"
									+ "var ifrmBooks = frmBooks.BookType.selectedIndex;\n"
									+ "var jfrmBooks = frmBooks.Book.selectedIndex;\n"
									+ "var lfrmBooks = frmBooks.Page.selectedIndex;\n"
									// + "alert(frmNdx.searchId.value);\n"
									
									
									+ " var kfrmBooks = frmBooks.searchId.value;\n" + 
									" var strifrmBooks = ''; " +
										"if (frmBooks.BookType) {" +
											" var i = frmBooks.BookType.selectedIndex;\n" + 
											" if( i >= 0 && frmBooks.BookType.options[i]) {\n" + 
											"    strifrmBooks = \"&BookType=\" + frmBooks.BookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\"); " +
											" }" +
										"}\n" + 
									" var strjfrmBooks = ''; " +
										"if (frmBooks.Book) {" +
											"var j = frmBooks.Book.selectedIndex;\n" + 
											" if( j >= 0 && frmBooks.Book.options[j]) {\n" +
											"    strjfrmBooks = \"&Book=\" + frmBooks.Book.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
											" }" +
										"}\n" +
									" var strlfrmBooks = ''; " +
										"if (frmBooks.Page) {" +
										" var l = frmBooks.Page.selectedIndex;\n" + 
										" if ( l >= 0 && frmBooks.Page.options[l]) {\n" +
											"    strlfrmBooks = \"&Page=\" + frmBooks.Page.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
										"}\n"
									
									
									
									+ "var strURL = \"URLConnectionReader?p1=019&p2=1\";\n"
									// +
									// "alert(\"Opening\" + strURL + strk + \"&ActionType=2&Link=/recording/SPVBookSearch.asp\" + stri + strj)\n"
									+ "window.location.href = strURL + strk + \"&ActionType=2&Link=/recording/SPVIndexSearch.asp\" + stri + strj + strifrmBooks + strjfrmBooks + strlfrmBooks;\n"
									+ "}\n"
									
									+ "function IAR() {\n"
									+ "	var frmNdx = document.frmIndexes;\n"
									+ "var i = frmNdx.IndexBookType.selectedIndex;\n"
									+ "var j = frmNdx.IndexDateRange.selectedIndex;\n"
									+ "var m = frmNdx.IndexAlphaRange.selectedIndex;\n"
									//+ "alert(i + ' ' + j + ' ' + m);\n"
									+ "var k = frmNdx.searchId.value;\n"
									+ "var stri = \"&IndexBookType=\" + frmNdx.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strj = \"&IndexDateRange=\" + frmNdx.IndexDateRange.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strm = \"&IndexAlphaRange=\" + frmNdx.IndexAlphaRange.options[m].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strk = \"&searchId=\" + k;\n"
									+ "var frmBooks = document.frmBooks;\n"
									
									+ " var kfrmBooks = frmBooks.searchId.value;\n" + 
									" var strifrmBooks = ''; " +
										"if (frmBooks.BookType) {" +
											" var i = frmBooks.BookType.selectedIndex;\n" + 
											" if( i >= 0 && frmBooks.BookType.options[i]) {\n" + 
											"    strifrmBooks = \"&BookType=\" + frmBooks.BookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\"); " +
											" }" +
										"}\n" + 
									" var strjfrmBooks = ''; " +
										"if (frmBooks.Book) {" +
											"var j = frmBooks.Book.selectedIndex;\n" + 
											" if( j >= 0 && frmBooks.Book.options[j]) {\n" +
											"    strjfrmBooks = \"&Book=\" + frmBooks.Book.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
											" }" +
										"}\n" +
									" var strlfrmBooks = ''; " +
										"if (frmBooks.Page) {" +
										" var l = frmBooks.Page.selectedIndex;\n" + 
										" if ( l >= 0 && frmBooks.Page.options[l]) {\n" +
											"    strlfrmBooks = \"&Page=\" + frmBooks.Page.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");" +
										" }" +
										"}\n"
									
									
									+ "var strURL = \"URLConnectionReader?p1=019&p2=1\";\n"
									// +
									// "alert(\"Opening\" + strURL + strk + \"&ActionType=2&Link=/recording/SPVBookSearch.asp\" + stri + strj)\n"
									+ "window.location.href = strURL + strk + \"&ActionType=2&Link=/recording/SPVIndexSearch.asp\" + stri + strj + strm  + strifrmBooks + strjfrmBooks + strlfrmBooks;\n"
									+ "}\n"

									+ "function IPR() {\n"
									+ "	var frmNdx = document.frmIndexes;\n"
									+ "var i = frmNdx.IndexBookType.selectedIndex;\n"
									+ "var j = frmNdx.IndexDateRange.selectedIndex;\n"
									+ "var m = frmNdx.IndexAlphaRange.selectedIndex;\n"
									+ "var l = frmNdx.IndexPageRange.selectedIndex;\n"
									// + "alert(frmNdx.searchId.value);\n"
									+ "var k = frmNdx.searchId.value;\n"
									+ "var stri = \"&IndexBookType=\" + frmNdx.IndexBookType.options[i].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strj = \"&DateRange=\" + frmNdx.IndexDateRange.options[j].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strm = \"&AlphaRange=\" + frmNdx.IndexAlphaRange.options[m].text.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strl = \"&IndexPage=\" + frmNdx.IndexPageRange.options[l].value.replace(/&/g,\"%26\").replace(/#/g,\"%23\");\n"
									+ "var strk = \"&searchId=\" + k;\n"
									+ "var strURL = \"URLConnectionReader?p1=019&p2=1\";\n"
									// +
									// "alert(window.document.forms[0].Page.options[window.document.forms[0].Page.selectedIndex].text);\n"
									+ "open(strURL + strk + \"&ActionType=2&Link=/recording/LoadImage.asp\" + stri + strj + strm + strl);\n"
									+ "}\n"
									
									+ "function SaveTSDIndex() {\n"
									+ "var rPageSelect = document.frmIndexes.IndexPageRange.selectedIndex;\n"

									+ "if (rPageSelect == -1) {\n"
									+ "    alert(\"Cannot save! No page selected!\");\n"
									+ "    }\n"
									+ "else {\n"
									+ "    document.frmIndexes.realPage.value=document.frmIndexes.IndexPageRange.options[rPageSelect].text; document.frmIndexes.submit();"
									+ "    }\n" + "}\n" + "</script>");

			rsResponce = rsResponce.replaceAll("action='SPVIndexSearch\\.asp'", "action='MultiDocSave'");

			rsResponce = rsResponce.replaceAll("</form></body></html>", "")
					+ "<br></br><input name=\"Button\" type=\"button\" class=\"button\" value=\"Save Selected Index Book-Page to TSD\" onClick=\"SaveTSDIndex();\">"
					+ "<input type=\"hidden\" name=\"searchId\" value=\""
					+ searchId
					+ "\">"
					+ "<input type=\"hidden\" name=\"realPage\" />"
					+ "<input type=\"hidden\" name=\"serverId\" value=\"19\"/> \n</form>";
			parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);
			break;
		case ID_GET_LINK:
			if (sAction.indexOf("SPVBookSearch.asp") != -1) {
				ParseResponse(sAction, Response, ID_BROWSE_SCANNED_INDEX_PAGES);
			} else if (sAction.indexOf("SPVIndexSearch.asp") != -1) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE19);
			} else if (Response.getQuerry().indexOf("automaticNameSearch") >= 0) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else if (Response.getQuerry().indexOf("Summary Data") > -1){
                ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
            } else {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
		default:
			break;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public Collection<ParsedResponse> smartParseDetailData(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		Search search = this.getSearch();
		searchId = search.getID();
		
		/**
		 * We need to find what was the original search module
		 * in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			} 
		} else {
			objectModuleSource = search.getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}
		
		try {
			table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList formList = htmlParser.parse(new TagNameFilter("form"));
			
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>");
			String prevLink = "", nextLink = "";
			
			String[] tables = table.split("<hr>");
			for (String tabel : tables) {
				if (StringUtils.isNotBlank(tabel)){
					org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(tabel, null);
					NodeList mainTableList = tableParser.parse(new TagNameFilter("table"));
					if (mainTableList != null && mainTableList.size() > 0){
						TableTag mainTable = (TableTag) mainTableList.elementAt(0);
				
						String instr = "";
						Text instrumentNode = HtmlParser3.findNode(mainTable.getChildren(), "Instrument:");
						if (instrumentNode != null && instrumentNode.getParent() != null){
							TableColumn tc = (TableColumn) instrumentNode.getParent();
							instr = HtmlParser3.getValueFromCell(tc, "", false);
							instr = instr.replaceAll("(?is)Instrument:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String bookPage = "";
						Text bookPageNode = HtmlParser3.findNode(mainTable.getChildren(), "Volume Page:");
						if (bookPageNode != null && bookPageNode.getParent() != null){
							TableColumn tc = (TableColumn) bookPageNode.getParent();
							bookPage = HtmlParser3.getValueFromCell(tc, "", false);
							bookPage = bookPage.replaceAll("(?is)Volume Page:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String recDate =  "";
						Text recordedNode = HtmlParser3.findNode(mainTable.getChildren(), "Recorded:");
						if (recordedNode != null && recordedNode.getParent() != null){
							TableColumn tc = (TableColumn) recordedNode.getParent();
							recDate = HtmlParser3.getValueFromCell(tc, "", false);
							recDate = recDate.replaceAll("(?is)Recorded:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String consideration = "";
						Text considerationNode = HtmlParser3.findNode(mainTable.getChildren(), "Consideration:");
						if (considerationNode != null && considerationNode.getParent() != null){
							TableColumn tc = (TableColumn) considerationNode.getParent();
							consideration = HtmlParser3.getValueFromCell(tc, "", false);
							consideration = consideration.replaceAll("(?is)Consideration:", "").replaceAll("(?is)&nbsp;", " ").replaceAll("[$,]+", "").trim();
						}

						String serverDocType = "";
						Text serverDoctypeNode = HtmlParser3.findNode(mainTable.getChildren(), "Document Type:");
						if (serverDoctypeNode != null && serverDoctypeNode.getParent() != null){
							TableColumn tc = (TableColumn) serverDoctypeNode.getParent();
							serverDocType = HtmlParser3.getValueFromCell(tc, "", false);
							serverDocType = serverDocType.replaceAll("(?is)Document Type:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						Text documentDateNode = HtmlParser3.findNode(mainTable.getChildren(), "Prepared:");
						String instrDate = "";
						if (documentDateNode != null && documentDateNode.getParent() != null){
							TableColumn tc = (TableColumn) documentDateNode.getParent();
							instrDate = HtmlParser3.getValueFromCell(tc, "", false);
							instrDate = instrDate.replaceAll("(?is)Prepared:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						String tmpDebtor = "";
						String grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantor:"), "", true);
						if (StringUtils.isEmpty(grantors)){
							grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Debtor:"), "", true);
							tmpDebtor = "Debtor";
							if (StringUtils.isEmpty(grantors)){
								grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Plat:"), "", true);
							}
						}
						if (StringUtils.isNotEmpty(grantors)){
							grantors = grantors.replaceAll("(?is)</?nobr>", "").replaceAll("(?is)<br>", " / ").replaceAll("(?is)&nbsp;", " ");
							grantors = grantors.replaceAll("(?is)\\s*/\\s*", " / ");
						}
						
						String grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantee:"), "", true);
						if (StringUtils.isEmpty(grantees)){
							grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Secured Party:"), "", true);
							if (StringUtils.isEmpty(grantees)){
								grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Developer Name:"), "", true);
							}
						}
						if (StringUtils.isNotEmpty(grantees)){
							grantees = grantees.replaceAll("(?is)</?nobr>", "").replaceAll("(?is)<br>", " / ").replaceAll("(?is)&nbsp;", " ");
							grantees = grantees.replaceAll("(?is)\\s*/\\s*", " / ");
						}
						
						String remarks = HtmlParser3.getValueFromNextCell(mainTableList, "Remarks:", "", true);
						String marginal = HtmlParser3.getValueFromNextCell(mainTableList, "Marginal:", "", true);
						
						String legalDesc = HtmlParser3.getValueFromNextCell(mainTableList, "Legal Description:", "", true);
						legalDesc = legalDesc.replaceAll("(?is)</?i>", "").replaceAll("(?is)<br>", ", ");
							
						String key = instr + "_" + serverDocType.replaceAll("\\s+", "_");
						if (StringUtils.isBlank(instr)){
							key = bookPage + "_" + serverDocType.replaceAll("\\s+", "_");
						}
		
						ParsedResponse currentResponse = responses.get(key);							 
						if (currentResponse == null) {
							currentResponse = new ParsedResponse();
							responses.put(key, currentResponse);
						}
						StringBuilder imageLink = new StringBuilder();
						Text nodeDisplayDoc = HtmlParser3.findNode(mainTable.getChildren(), "Display Doc");
						if (nodeDisplayDoc != null){
							if (nodeDisplayDoc.getParent() != null){
								Node parentNode = nodeDisplayDoc.getParent();
								if (parentNode instanceof LinkTag){
									String link = ((LinkTag) parentNode).getLink();
									
									imageLink.append(CreatePartialLink(TSConnectionURL.idGET)).append("/recording/").append(link);
									
									currentResponse.addImageLink(new ImageLinkInPage(imageLink.toString(), instr + ".tif"));
								}
							}
						}
								
						StringBuilder linkDetailData = new StringBuilder();
						if (formList != null && formList.size() > 0){
							Node pageMenuNode = formList.extractAllNodesThatMatch(new HasAttributeFilter("id", "PageMenu"), true).elementAt(0);
							FormTag pageMenuForm = (FormTag) pageMenuNode;
							if (pageMenuForm != null){
								String action = pageMenuForm.getAttribute("action");
								String link = CreatePartialLink(TSConnectionURL.idPOST) + action + "?";
								linkDetailData.append(link);
								
								NodeList inputs = pageMenuForm.getFormInputs();
								if (inputs != null){
									Map<String,String> paramsForNav = new HashMap<String, String>();
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
						}
						linkDetailData.append("Submit=Detail Data");
						if (StringUtils.isNotBlank(key)){
							linkDetailData.append("&Instrs=").append(key);
						}
						String link = linkDetailData.toString();
						
						RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();				
						ResultMap resultMap = new ResultMap();
						
						List<List> body = new ArrayList<List>();
						
						if (StringUtils.isNotBlank(marginal)){
							String[] marginals = marginal.split("\\s*,\\s*");
							List<String> line = null;
							for (String crossref : marginals) {
								line = new ArrayList<String>();
								crossref = crossref.replaceAll("(?is)</?a[^>]*>", "").replaceAll("(?is)</?i[^>]*>", "")
													.replaceAll("(?is)</?nobr[^>]*>", "");
//								crossref = crossref.replaceAll("(?is)\\(\\w+\\)$", "");
								
								Matcher mat = Pattern.compile("(?is)(?:Bkwd|Fwd)\\s+(?:[A-Z]\\s+)?(\\d{2,5})\\w{3}(?!\\s*\\d)").matcher(crossref);
								if (mat.find()){
									line.add("");
									String book = crossref.replaceAll("(?is)(?:Bkwd|Fwd)\\s+(?:[A-Z]\\s+)?(\\d{2,5})\\w{3}(?!\\s*\\d)", "$1");
									book = book.replaceAll("(?is)\\(\\w+\\)$", "");
									line.add(book.trim());
									String page = crossref.replaceAll("(?is)(?:Bkwd|Fwd)\\s+(?:[A-Z]\\s+)?(\\d{2,5})(\\w{3}(?!\\s*\\d))", "$2");
									page = page.replaceAll("(?is)\\(\\w+\\)$", "");
									line.add(page.trim());
									line.add("");
									line.add("");
									line.add("");
								} else{
									mat = Pattern.compile("(?is)(?:Bkwd|Fwd)\\s+(?:(?:I|M)\\s+)?(\\w{7,}).*").matcher(crossref);
									if (mat.find()){
										crossref = crossref.replaceAll("(?is)(?:Bkwd|Fwd)\\s+(?:(?:I|M)\\s+)?(\\w{7,}).*", "$1");
										crossref = crossref.replaceAll("(?is)\\bRERECORD\\b", "");
										crossref = crossref.replaceAll("(?is)\\(\\w+\\)$", "");
										line.add(crossref.trim());
										line.add("");
										line.add("");
										line.add("");
										line.add("");
										line.add("");
									}
								}
								if (!line.isEmpty()){
									body.add(line);
								}
							}
						}
						if (StringUtils.isNotBlank(remarks)){
							String[] remark = remarks.split("\\s*,\\s*");
							List<String> line = null;
							for (String crossref : remark) {
								line = new ArrayList<String>();
								crossref = crossref.replaceAll("(?is)</?a[^>]*>", "").replaceAll("(?is)</?i[^>]*>", "")
													.replaceAll("(?is)</?nobr[^>]*>", "");
//								crossref = crossref.replaceAll("(?is)\\(\\w+\\)$", "");
								
								Matcher mat = Pattern.compile("(?is)\\w+\\s+\\d+").matcher(crossref);
								if (mat.find()){
									crossref = crossref.replaceAll("(?is)\\w+\\s+(\\d+)", "$1");
									line.add(crossref.trim());
									line.add("");
									line.add("");
									line.add("");
									line.add("");
									line.add("");
								}
								if (!line.isEmpty()){
									body.add(line);
								}
							}
						}
						if (!body.isEmpty()){
							String[] header = { "InstrumentNumber", "Book", "Page", "Month", "Day", "Year"};
							resultMap.put("CrossRefSet", GenericFunctions2.createResultTable(body, header));
						}
						if (document == null) {	//first time we find this document
							String rowHtml =  mainTable.toHtml();
							
							resultMap.put("tmpDebtor", tmpDebtor);
							resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), grantors);
							resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), grantees);
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
							bookPage = bookPage.replaceFirst("(?is)\\A\\s*(\\d+)([A-Z]\\d+)\\s*$", "$1 $2");
							String[] bp = bookPage.split("\\s+");
							if (bp.length == 2){
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bp[0]);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), bp[1]);
							}
							resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
							resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrDate);
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate.replaceAll("(?is)\\A\\s*([\\d/]+)\\s+.*", "$1").trim());
							resultMap.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), consideration);
							resultMap.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), consideration);
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDesc);
							
							if (StringUtils.isNotBlank(remarks)){
								remarks = remarks.replaceAll("(?is)&nbsp;", " ").trim();
								resultMap.put("tmpRemarks", remarks);
							}
							if (StringUtils.isNotBlank(marginal)){
								resultMap.put("tmpMarginal", marginal.replaceAll("(?is)</?a[^>]*>", "").replaceAll("(?is)</?i[^>]*>", "")
																		.replaceAll("(?is)</?nobr[^>]*>", ""));
							}
							GenericFunctions1.liensDocTypeChangeNashvilleRO(resultMap, searchId);
							GenericFunctions1.checkMortgageAmount(resultMap, searchId);
							GenericFunctions1.checkConsiderationAmount(resultMap, searchId);
							GenericFunctions1.checkInstrumentDate(resultMap, searchId);
							GenericFunctions1.reparseDocTypeTNDavidsonRO(resultMap, searchId);
							GenericFunctions1.legalDescriptionNashvilleRO(resultMap, searchId);
							
							try {
								parseName(resultMap, searchId);
							} catch (Exception e) {
								e.printStackTrace();
							}
							resultMap.removeTempDef();
				    				
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
							currentResponse.setUseDocumentForSearchLogRow(true);
																	
							Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
							document = (RegisterDocumentI) bridge.importData();
							
							if (moduleSource != null){
								document.setSearchType(SearchType.valueOf(moduleSource.getSearchType()));
							} else if (response.getQuerry().contains("submit=Detail Data ")){
								document.setSearchType(SearchType.CS);
							}
							
//							try {
//								parseLegalDetailData(legalDesc, document, searchId);
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
									
							currentResponse.setDocument(document);
							String checkBox = "checked";
									
							if (isAlreadySaved(instr, document) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
								checkBox = "saved";
							} else {
								numberOfUncheckedElements++;
								LinkInPage linkInPage = new LinkInPage(link, link,TSServer.REQUEST_SAVE_TO_TSD);
								currentResponse.setPageLink(linkInPage);
								checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>Select for saving to TS Report";
								/**
								 * Save module in key in additional info. The key is instrument number that should be always available. 
								 */
								String keyForSavingModules = this.getKeyForSavingInIntermediary(instr);
								search.setAdditionalInfo(keyForSavingModules, moduleSource);
							}
							
							Matcher crossRefLinkMatcher = CROSSREF_PAT.matcher(rowHtml);
							while (crossRefLinkMatcher.find()) {
								ParsedResponse prChild = new ParsedResponse();
								String crossLink = CreatePartialLink(TSConnectionURL.idGET) + "/recording/" +  crossRefLinkMatcher.group(2) + "&isSubResult=true";
								LinkInPage pl = new LinkInPage(crossLink, crossLink, TSServer.REQUEST_SAVE_TO_TSD);
								prChild.setPageLink(pl);
								currentResponse.addOneResultRowOnly(prChild);
							}
//							crossRefLinkMatcher = MARGINAL_PAT.matcher(rowHtml);
//							while (crossRefLinkMatcher.find()) {
//								ParsedResponse prChild = new ParsedResponse();
//								String crossLink = CreatePartialLink(TSConnectionURL.idGET) + crossRefLinkMatcher.group(2) + "&isSubResult=true";
//								LinkInPage pl = new LinkInPage(crossLink, crossLink, TSServer.REQUEST_SAVE_TO_TSD);
//								prChild.setPageLink(pl);
//								currentResponse.addOneResultRowOnly(prChild);
//							}
							rowHtml = rowHtml.replaceAll("(?is)(\\bhref=[\\\"|'])(simplequery\\.asp\\?instrs=[^>]+[\\\"|']>)", "$1" + CreatePartialLink(TSConnectionURL.idGET) + "/recording/$2");
							rowHtml = rowHtml.replaceAll("(?is)(\\bhref=[\\\"|'])(simplequery\\.asp\\?Marginals=[^>]+[\\\"|']>)", "$1" + CreatePartialLink(TSConnectionURL.idGET) + "/recording/$2");
							
							if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ){
								rowHtml = rowHtml.replaceFirst("(?is)<a href([^>]*)>Display Doc</a>", "<A id=\"imageLink\" target=\"_blank\" HREF=\"" + imageLink.toString() + "\">Display Doc</A>");
							
							}
							mSearch.addInMemoryDoc(link, currentResponse);
							
							rowHtml = rowHtml.replaceFirst("(?is)</TR></Table>",
											"</TR><TR><TD COLSPAN='100'>" + checkBox + "</TD></TR><TR><TD COLSPAN='100'><hr></TD></TR></table>");
							currentResponse.setOnlyResponse(rowHtml);
//							currentResponse.setResponse(rowHtml.replaceAll("(?is)</?a[^>]*>", ""));
							newTable.append(currentResponse.getResponse());
							intermediaryResponse.add(currentResponse);
						}
			
						newTable.append("</table>");
						outputTable.append(newTable);
						SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
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
	
	public Collection<ParsedResponse> smartParseNamesSummary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		/**
		 * We need to find what was the original search module
		 * in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if(objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			} 
		} else {
			objectModuleSource = getSearch().getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}
		
		try {
			table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>");
			String action = "";
			StringBuilder hiddenInputs = new StringBuilder();
			NodeList mainFormList = htmlParser.parse(new TagNameFilter("form"));
			if (mainFormList != null && mainFormList.size() > 0){
				FormTag mainForm = (FormTag) mainFormList.elementAt(0);
				if (mainForm != null){
					action += mainForm.getAttribute("action");
					NodeList inputsList = mainForm.getFormInputs();
					if (inputsList != null){
						StringBuilder linkDetail = new StringBuilder();
						linkDetail.append(CreatePartialLink(TSConnectionURL.idPOST)).append("/recording/").append(action).append("?");
						for (int i = 0; i < inputsList.size(); i++) {
							InputTag input = (InputTag) inputsList.elementAt(i);
							String type = input.getAttribute("type");
							if (StringUtils.isNotBlank(type)){
								if ("hidden".equalsIgnoreCase(type) && !input.getAttribute("name").equalsIgnoreCase("Submit")){
									String inputHtml = input.toHtml();
									if ("SortDir".equalsIgnoreCase(input.getAttribute("name"))){
										inputHtml = inputHtml.replaceFirst("(?is)\\bdescending\\b", "DESC");
										inputHtml = inputHtml.replaceFirst("(?is)\\bascending\\b", "ASC");
									}
									hiddenInputs.append(inputHtml).append("\r\n");
									String value = input.getAttribute("value");
									if (value.equalsIgnoreCase("descending")){
										value = "DESC";
									} else if (value.equalsIgnoreCase("ascending")){
										value = "ASC";
									}
									linkDetail.append(input.getAttribute("name")).append("=").append(value).append("&");
								} 
							}
						}
						for (int i = 0; i < inputsList.size(); i++) {
							InputTag input = (InputTag) inputsList.elementAt(i);
							String type = input.getAttribute("type");
							if (StringUtils.isNotBlank(type)){		
								if ("checkbox".equalsIgnoreCase(type)){
									StringBuilder linkToDetail = new StringBuilder(linkDetail);
									linkToDetail.append("SUBMIT=Detail Data").append("&Names=").append(input.getAttribute("Value"));
									String link = "<a href=\"" + linkToDetail.toString() + "\" title=\"Detail Data\">";
									ParsedResponse currentResponse = new ParsedResponse();
									StringBuilder row = new StringBuilder();
									String grantor = input.getNextSibling().getText();
									row.append("<tr><td>").append(input.toHtml()).append(link).append(grantor).append("</a></td></tr>");
									
									ResultMap resultMap = new ResultMap();
									resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), grantor.replaceAll("(?is)\\(\\d+\\)", ""));
									resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
									try {
										parseName(resultMap, searchId);
									} catch (Exception e) {
										e.printStackTrace();
									}
									resultMap.removeTempDef();
						    				
									currentResponse.setUseDocumentForSearchLogRow(true);
									
									Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
									RegisterDocumentI document = (RegisterDocumentI) bridge.importData();
									
									if (moduleSource != null){
										document.setSearchType(SearchType.valueOf(moduleSource.getSearchType()));
									}
									LinkInPage linkInPage = new LinkInPage(linkToDetail.toString(), linkToDetail.toString(), TSServer.REQUEST_GO_TO_LINK_REC);
									currentResponse.setPageLink(linkInPage);
									
									currentResponse.setDocument(document);
									currentResponse.setOnlyResponse(row.toString());
									newTable.append(currentResponse.getResponse());
									intermediaryResponse.add(currentResponse);
								}
							}
						}
					}
				}
			}
			newTable.append("</table>");
			outputTable.append(newTable);
			
			String header1 = "<TH colspan=\"2\" width=\"5%\" align=\"justify\">Place a Check Mark by the names you would like more detailed information on.</TH>";
			
			response.getParsedResponse().setHeader(CreateSummaryOrDetailFormHeader("GET", "/recording/" + action, hiddenInputs)
								+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
			response.getParsedResponse().setFooter("</table>" +  CreateSummaryOrDetailFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	public Collection<ParsedResponse> smartParseSummaryData(ServerResponse response, String table, StringBuilder outputTable, boolean useSummaryDataAsDetail) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		Search search = this.getSearch();
		searchId = search.getID();
		
		/**
		 * We need to find what was the original search module
		 * in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if (objectModuleSource != null) {
			if(objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			} 
		} else {
			objectModuleSource = search.getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
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
			NodeList formList = htmlParser.parse(new TagNameFilter("form"));
			
			String[] tables = table.split("<hr>");
			for (String tabel : tables) {
				if (StringUtils.isNotBlank(tabel)){
					org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(tabel, null);
					NodeList mainTableList = tableParser.parse(new TagNameFilter("table"));
					if (mainTableList != null && mainTableList.size() > 0){
						TableTag mainTable = (TableTag) mainTableList.elementAt(0);
				
						String instr = "";
						Text instrumentNode = HtmlParser3.findNode(mainTable.getChildren(), "Instr:");
						if (instrumentNode != null && instrumentNode.getParent() != null){
							TableColumn tc = (TableColumn) instrumentNode.getParent();
							instr = HtmlParser3.getValueFromCell(tc, "", false);
							instr = instr.replaceAll("(?is)Instr:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String bookPage = "";
						Text bookPageNode = HtmlParser3.findNode(mainTable.getChildren(), "Vol/Page:");
						if (bookPageNode != null && bookPageNode.getParent() != null){
							TableColumn tc = (TableColumn) bookPageNode.getParent();
							bookPage = HtmlParser3.getValueFromCell(tc, "", false);
							bookPage = bookPage.replaceAll("(?is)Vol/Page:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String recDate = "";
						Text recDateNode = HtmlParser3.findNode(mainTable.getChildren(), "Rec:");
						if (recDateNode != null && recDateNode.getParent() != null){
							TableColumn tc = (TableColumn) recDateNode.getParent();
							recDate = HtmlParser3.getValueFromCell(tc, "", false);
							recDate = recDate.replaceAll("(?is)Rec:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String serverDocType = "";
						Text serverDocTypeNode = HtmlParser3.findNode(mainTable.getChildren(), "Type:");
						if (serverDocTypeNode != null && serverDocTypeNode.getParent() != null){
							TableColumn tc = (TableColumn) serverDocTypeNode.getParent();
							serverDocType = HtmlParser3.getValueFromCell(tc, "", false);
							serverDocType = serverDocType.replaceAll("(?is)Type:", "").replaceAll("(?is)&nbsp;", " ").trim();
						}
						
						String grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantor:"), "", true);
						if (StringUtils.isEmpty(grantors)){
							grantors = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Debtor:"), "", true);
						}
						if (StringUtils.isNotEmpty(grantors)){
							grantors = grantors.replaceAll("(?is)</?nobr>", "").replaceAll("(?is)<br>", " / ").replaceAll("(?is)&nbsp;", " ");
							grantors = grantors.replaceAll("(?is)\\s*/\\s*", " / ");
						}
						
						String grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Grantee:"), "", true);
						if (StringUtils.isEmpty(grantees)){
							grantees = HtmlParser3.getValueFromNearbyCell(2, HtmlParser3.findNode(mainTable.getChildren(), "Secured Party:"), "", true);
						}
						if (StringUtils.isNotEmpty(grantees)){
							grantees = grantees.replaceAll("(?is)</?nobr>", "").replaceAll("(?is)<br>", " / ").replaceAll("(?is)&nbsp;", " ");
							grantees = grantees.replaceAll("(?is)\\s*/\\s*", " / ");
						}
						
						String legalDesc = HtmlParser3.getValueFromNearbyCell(0, HtmlParser3.findNode(mainTable.getChildren(), "Legal:"), "", true);
						legalDesc = legalDesc.replaceAll("(?is)Legal:", "").replaceAll("(?is)</?i>", "").replaceAll("(?is)<br>", ", ")
												.replaceAll("(?is)</?b>", "");
							
						String key = instr + "_" + serverDocType.replaceAll("\\s+", "_");
		
						ParsedResponse currentResponse = responses.get(key);
						if (currentResponse == null) {
							currentResponse = new ParsedResponse();
							responses.put(key, currentResponse);
						}
						StringBuilder imageLink = new StringBuilder();
						Text nodeDisplayDoc = HtmlParser3.findNode(mainTable.getChildren(), "Display Doc");
						if (nodeDisplayDoc != null){
							if (nodeDisplayDoc.getParent() != null){
								Node parentNode = nodeDisplayDoc.getParent();
								if (parentNode instanceof LinkTag){
									String link = ((LinkTag) parentNode).getLink();
									
									imageLink.append(CreatePartialLink(TSConnectionURL.idGET)).append("/recording/").append(link);
									
									currentResponse.addImageLink(new ImageLinkInPage(imageLink.toString(), instr + ".tif"));
								}
							}
						}
						RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();				
						
						ResultMap resultMap = new ResultMap();
								
						StringBuilder linkDetailData = new StringBuilder();
						if (formList != null && formList.size() > 0){
							FormTag form = (FormTag) formList.elementAt(0);
							if (form != null){
//								linkDetailData.append(CreatePartialLink(TSConnectionURL.idPOST));
								String action = form.getAttribute("action");
								if (StringUtils.isNotBlank(action)){
									linkDetailData.append("/recording/").append(action).append("?");
								}
								NodeList inputs = form.getFormInputs();
								if (inputs != null){
									for (int i = 0; i < inputs.size(); i++) {
										InputTag input = (InputTag) inputs.elementAt(i);
										String type = input.getAttribute("type");
										if (StringUtils.isNotBlank(type) && "hidden".equalsIgnoreCase(type)){
											String name = input.getAttribute("name");
											String value = input.getAttribute("value");
											if (name.equalsIgnoreCase("UserQuery")){
												Map<String,String> params = new HashMap<String, String>();
												params.put(name, value);
												int seq = getSeq();
												
												mSearch.setAdditionalInfo(getCurrentServerName() + ":UserQuery:" + seq, params);
												linkDetailData.append(name).append("=").append(seq).append("&");
												continue;
											}
											linkDetailData.append(name).append("=").append(value).append("&");
										}
									}
								}
							}
						}
						linkDetailData.append("SUBMIT=Detail Data");
						if (StringUtils.isNotBlank(instr)){
							linkDetailData.append("&Instrs=").append(instr);
						}
						String link = CreatePartialLink(TSConnectionURL.idPOST) + linkDetailData.toString();
						if (document == null) {	//first time we find this document
									
							String rowHtml =  mainTable.toHtml();
							rowHtml = rowHtml.replaceAll("(?is)<input[^>]*>", "");
							//"<a href=\"" + link + "\">Detail Data</a>"
							if (useSummaryDataAsDetail){
								rowHtml = rowHtml.replaceAll("(?is)(<b>\\s*Instr\\s*:\\s*</b>)\\s*<a href=[^>]+>([^<]+)</a>\\s*(</td>)", "$1$2$3");
								rowHtml = rowHtml.replaceAll("(?is)(<b>\\s*Vol/Page\\s*:\\s*</b>)\\s*<a href=[^>]+>([^<]+)</a>\\s*(</td>)", "$1$2$3");
							} else{
								rowHtml = rowHtml.replaceAll("(?is)(<b>\\s*Instr\\s*:\\s*</b>)([^<]+)(</td>)", "$1<a href=\"" + link + "\">$2</a>$3");
								rowHtml = rowHtml.replaceAll("(?is)(<b>\\s*Vol/Page\\s*:\\s*</b>)([^<]+)(</td>)", "$1<a href=\"" + link + "\">$2</a>$3");
							}
									
							resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), grantors);
							resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), grantees);
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
							String[] bp = bookPage.split("\\s+");
							if (bp.length == 2){
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bp[0]);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), bp[1]);
							} else if (bp.length == 3){
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), bp[0] + bp[1]);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), bp[2]);
							}
							resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate.replaceAll("(?is)\\A\\s*([\\d/]+)\\s+.*", "$1").trim());
							try {
								parseName(resultMap, searchId);
							} catch (Exception e) {
								e.printStackTrace();
							}
							resultMap.removeTempDef();
				    				
							currentResponse.setUseDocumentForSearchLogRow(true);
							
							Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
							document = (RegisterDocumentI) bridge.importData();
							
							if (moduleSource != null){
								document.setSearchType(SearchType.valueOf(moduleSource.getSearchType()));
							}
							
							try {
								parseLegalSummaryData(legalDesc, document, searchId);
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							currentResponse.setDocument(document);
							String checkBox = "checked";
							if (isAlreadySaved(instr, document) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
								checkBox = "saved";
							} else {
								numberOfUncheckedElements++;
								
								if (useSummaryDataAsDetail){
									LinkInPage linkInPage = response.getParsedResponse().getPageLink();
									if (linkInPage != null){
										linkInPage.setActionType(TSServer.REQUEST_SAVE_TO_TSD);
										link = linkInPage.getLink();
										currentResponse.setPageLink(linkInPage);
									}
								} else{
									LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_GO_TO_LINK_REC);
									currentResponse.setPageLink(linkInPage);
								}
								checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>Select for saving to TS Report";
								/**
								 * Save module in key in additional info. The key is instrument number that should be always available. 
								 */
								String keyForSavingModules = this.getKeyForSavingInIntermediary(instr);
								search.setAdditionalInfo(keyForSavingModules, moduleSource);
							}
							
							if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ){
								rowHtml = rowHtml.replaceFirst( "(?is)<a href([^>]*)>Display Doc</a>" , "<A id=\"imageLink\" target=\"_blank\" HREF=\"" + imageLink.toString() + "\">Display Doc</A>");
							
							}
//							if (useSummaryDataAsDetail){
//								currentResponse.setOnlyResponse(rowHtml);
//								mSearch.addInMemoryDoc(link, currentResponse);
//							} 
//							else{
								mSearch.addInMemoryDoc(linkDetailData + "&UseSummaryData=true", currentResponse);
								mSearch.addInMemoryDoc(linkDetailData + "&UseSummaryDataResult=true", rowHtml);
//							}
							
							rowHtml = rowHtml.replaceFirst("(?is)</TR></Table>", 
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
				
			String header1 = "<TH width=\"5%\" align=\"justify\">" + SELECT_ALL_CHECKBOXES + "Check\\Uncheck All</TH>";
			
			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
								+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
			response.getParsedResponse().setFooter("</table>" +  CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	protected String CreateSummaryOrDetailFormHeader(String method, String action, StringBuilder hiddenInputs){
    	String s = "<form name=\"SummaryOrDetail\" id=\"SummaryOrDetail\" action= \"" + CreatePartialLink(TSConnectionURL.idGET) + "\"" + " method=\"" + method + "\" > "
                + new LinkParser(CreatePartialLink(TSConnectionURL.idGET) + action).toStringParam("<input TYPE='hidden' NAME='", "' VALUE='", "'>\n")
                + hiddenInputs;
    	return s;
    }
   	
   	protected String CreateSummaryOrDetailFormEnd(String name, int parserId) {
    	        
        String s = 
        		"<input type=\"submit\" class=\"button\" name=\"SUBMIT\" value=\"Summary Data\" >" +
        		"<input type=\"submit\" class=\"button\" name=\"SUBMIT\" value=\"Detail Data\" >\r\n";
        
        
        return s + "</form>\n";
	}
   	
   	public void parseLegalSummaryData(String legal, DocumentI document, long searchId) throws Exception{
		
		if (StringUtils.isNotEmpty(legal)){
			
			Set<PropertyI> properties = document.getProperties();
			
			legal = legal.replaceAll("(?is)&nbsp;", " ");
			legal = GenericFunctions.replaceNumbers(legal);
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			
//			String platBook = "", platPage = ""; //subdivisionName = "";
			PropertyI property = Property.createEmptyProperty();
			SubdivisionI subdivision = property.getLegal().getSubdivision();
			AddressI address = property.getAddress();
//	        TownShipI township = property.getLegal().getTownShip();
	        PinI pin = property.getPin();
	        legal = legal.replaceAll("(?is)\\b(\\d+)([A-Z]+)\\b", "$1 $2");
				
	        Pattern STREET_PAT = Pattern.compile("(?is)\\bSt\\s*:\\s*(.*?)((\\s+\\w+:)|\\s*$)");
	        Matcher mat = STREET_PAT.matcher(legal);
	        if (mat.find()){
	        	String streetName = mat.group(1);
	        	address.setStreetName(streetName);
			}
	        Pattern STREET_NUMBER_PAT = Pattern.compile("(?is)\\bAdrNm\\s*:\\s*(.*?)((\\s+\\w+:)|\\s*$)");
	        mat = STREET_NUMBER_PAT.matcher(legal);
	        if (mat.find()){
	        	String streetNo = mat.group(1);
	        	address.setNumber(streetNo);
			}
	        
	        Pattern LOT_PAT = Pattern.compile("(?is)\\bLt\\s*:\\s*(.*?)((\\s+\\w+:)|\\s*$)");
	        mat = LOT_PAT.matcher(legal);
	        if (mat.find()){
	        	String lot = mat.group(1);
	        	lot = LegalDescription.cleanValues(lot, false, true);
	        	subdivision.setLot(lot);
	        	legal = legal.replaceAll(mat.group(), "");
			}
	        
	        Pattern BLK_PAT = Pattern.compile("(?is)\\bBl\\s*:\\s*(.*?)((\\s+\\w+:)|\\s*$)");
	        mat = BLK_PAT.matcher(legal);
	        if (mat.find()){
	        	String block = mat.group(1);
	        	block = LegalDescription.cleanValues(block, false, true);
	        	subdivision.setBlock(block);
	        	legal = legal.replaceAll(mat.group(), "");
			}
	        
	        Pattern PARCELID_PAT = Pattern.compile("(?is)\\bPrpId\\s*:\\s*#?\\s*(.*?)((\\s+\\w+:)|\\s*$)");
	        mat = PARCELID_PAT.matcher(legal);
	        if (mat.find()){
	        	String parcelId = mat.group(1);
	        	pin.addPin(PinType.PID, parcelId);
	        	legal = legal.replaceAll(mat.group(), "");
			}
	        Pattern UNIT_PAT = Pattern.compile("(?is)\\bUn\\s*:\\s*(.*?)((\\s+\\w+:)|\\s*$)");
	        mat = UNIT_PAT.matcher(legal);
	        if (mat.find()){
	        	String unit = mat.group(1);
	        	subdivision.setUnit(unit);
			}
	        
	        Pattern ACREAGE_PAT = Pattern.compile("(?is)\\bAc\\s+([\\d\\.]+)\\b");
	        mat = ACREAGE_PAT.matcher(legal);
	        if (mat.find()){
	        	String acreage = mat.group(1);
	        	subdivision.setAcreage(acreage);
			}
	        
	        Pattern SEC_PAT = Pattern.compile("(?is)\\b(?:SECTION NO?|SECTION|SEC NO?|SEC) (\\d+).*?");
	        mat = SEC_PAT.matcher(legal);
	        if (mat.find()){
	        	String section = mat.group(1);
	        	section = Roman.normalizeRomanNumbersExceptTokens(section, exceptionTokens); // convert roman numbers to arabics	
	        	subdivision.setSection(section);
	        	legal = legal.replaceAll(mat.group(), "");
			}
	        
	        Pattern SUBD_PAT = Pattern.compile("(?is)\\bSub\\s*:\\s*(.*?)((\\s+\\w+:)|\\s*$)");
	        mat = SUBD_PAT.matcher(legal);
	        if (mat.find()){
	        	String subdivisionName = mat.group(1);
	        	subdivisionName = subdivisionName.replaceAll("(?is),\\s*$", "");
	        	
	        	subdivision.setName(subdivisionName.trim());
			}
//	        legal = legal.replaceAll("(?is)\\*", "");
//	        legal = legal.replaceAll("(?is)\\bPcl\\s*#\\s*", "");
//	        legal = legal.replaceAll("(?s)\\bUn\\b", "");
//	        
//	        legal = legal.replaceAll("(?is)(.*?)\\s(CONDOMINIUM|CONDOS?|SEC\\sNO\\s\\d+(\\sPART\\s\\d+)?|SECTION\\s(NO\\s)?\\d+(\\sPART\\s\\d+)?|SEC\\s\\d+(\\s(PT|PHASE|PH)\\s\\d+)?|PHASE\\s\\d+|AMENDED|RESUBD|PLAT\\s(NO\\s)?\\d+)(.*?)$", "$1");
	        
				
	        properties.add(property);
		}
	}

   	public boolean isAlreadySaved(String instrumentNo, DocumentI doc){
		
		DocumentI docToCheck = null;
		if (doc != null){
			docToCheck = doc.clone();
			docToCheck.setDocSubType(null);
		}

		return isInstrumentSaved(instrumentNo, docToCheck, null, false);
	}

   	public void parseLegalDetailData(String legal, DocumentI document, long searchId) throws Exception{

		if (StringUtils.isNotEmpty(legal)){
			
			Set<PropertyI> properties = document.getProperties();
			
			legal = legal.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)\\bSEE RECORD\\b", " ");
			legal = GenericFunctions.replaceNumbers(legal);
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			
			String[] legals = legal.split("\\s+/\\s+");
			
			String subdivisionName = "", platBook = "", platPage = "";
			for (String eachLegal : legals) {
				PropertyI property = Property.createEmptyProperty();
				SubdivisionI subdivision = property.getLegal().getSubdivision();
//	        	TownShipI township = property.getLegal().getTownShip();
	        	PinI pin = property.getPin();
				
	        	if (eachLegal.contains("Sub:")){
					Pattern SUBD_PAT = Pattern.compile("(?is)Sub: ([^,]+),");
					Matcher mat = SUBD_PAT.matcher(eachLegal);
					if (mat.find()){
						subdivisionName = mat.group(1);
						subdivisionName = subdivisionName.replaceAll("(?is)(.*?)\\s(CONDOMINIUM|CONDOS?|SEC\\sNO\\s\\d+(\\sPART\\s\\d+)?|SECTION\\s(NO\\s)?\\d+(\\sPART\\s\\d+)?|SEC\\s\\d+(\\s(PT|PHASE|PH)\\s\\d+)?|PHASE\\s\\d+|AMENDED|RESUBD|PLAT\\s(NO\\s)?\\d+)(.*?)$", "$1");
					} else{
						subdivisionName = eachLegal.replaceAll("(?is)Sub:", "").trim();
					}
					subdivision.setName(subdivisionName);
	        	}
				
				Pattern PBG_PAT = Pattern.compile("(?is)\\bCabSld: (?:P|C)B (\\d+) (?:P(?:G|B) )?(\\d+(-?\\w+)?).*?");
				Matcher mat = PBG_PAT.matcher(eachLegal);
				if (mat.find()){
					platBook = mat.group(1);
					platPage = mat.group(2);
					platPage = platPage.replaceAll("(?is)(\\w+)\\s*-\\s*(\\w+)", "$1");
				}
				
				if (StringUtils.isNotBlank(platBook) && StringUtils.isNotBlank(platPage)){
					subdivision.setPlatBook(platBook);
					subdivision.setPlatPage(platPage);
				}
				
				Pattern LOT_PAT = Pattern.compile("(?i)\\b(Lt|Lts|Lot): ([^,]*).*?");
				mat = LOT_PAT.matcher(eachLegal);
				if (mat.find()){
					String lot = mat.group(2);
					lot = LegalDescription.cleanValues(lot, false, true);
					subdivision.setLot(lot);
				}
				
				if (eachLegal.contains("PrpId:")){
					Pattern PARCELID_PAT = Pattern.compile("(?is)\\bPrpId: ([^,]*).*?");
					mat = PARCELID_PAT.matcher(eachLegal);
					if (mat.find()){
						String parcelId = mat.group(1);
						pin.addPin(PinType.PID, parcelId);
					} else{
						String parcelId = eachLegal.replaceAll("(?is)PrpId:", "").trim();;
						pin.addPin(PinType.PID, parcelId);
					}
				}
				
				Pattern UNIT_PAT = Pattern.compile("(?is)\\bUn: ([^,]*).*?");
				mat = UNIT_PAT.matcher(eachLegal);
				if (mat.find()){
					String unit = mat.group(1);
					subdivision.setUnit(unit);
				}
				Pattern SEC_PAT = Pattern.compile("(?is)\\bbSub: .*?\\b(?:SECTION NO?|SECTION|SEC NO?|SEC) (\\d+).*?");
				mat = SEC_PAT.matcher(eachLegal);
				if (mat.find()){
					String section = mat.group(1);
					section = Roman.normalizeRomanNumbersExceptTokens(section, exceptionTokens); // convert roman numbers to arabics	
					subdivision.setSection(section);
				}
				
				properties.add(property);
			}
		}
	}
   	
   	@Override
	public DownloadImageResult saveImage(ImageI image) throws ServerResponseException {

		DownloadImageResult res = null;
		logger.error("a intrat pe saveImage: " + searchId);
		if (image != null) {
    		String imageLink = image.getLink(0);
    		
    		if (StringUtils.isNotEmpty(imageLink)) {
    			logger.error("linkul: " + imageLink + " searchId: " + searchId);
    			byte[] imageBytes = null;
	    		
    			String baseLink = getBaseLink();
    			imageLink = baseLink.substring(0, baseLink.indexOf("/recording/")) + imageLink.substring(imageLink.indexOf("/recording/"));
    			imageLink = imageLink.replaceFirst("(?is)LoadImage\\.asp&", "LoadImage.asp?");
    			logger.error("linkul pt request: " + imageLink + " searchId: " + searchId);
	    			
    			HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
    			HTTPResponse resp = null;
    			try {
    				HTTPRequest req = new HTTPRequest(imageLink, HTTPRequest.GET);
    				resp = ((ro.cst.tsearch.connection.http3.TNDavidsonRO) site).process(req);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager3.releaseSite(site);
				}
				
    			if (resp != null){
    				if ("image/tiff".equalsIgnoreCase(resp.getContentType())){
    					imageBytes = resp.getResponseAsByte();
    				}
    			}
    			if (imageBytes != null){ 
    				res = new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType());
    				afterDownloadImage(true);
	    					
    				if (res.getStatus() == DownloadImageResult.Status.OK){
    					File f = new File(image.getPath());
    					if (!f.exists()){
    						try {
								FileUtils.writeByteArrayToFile(f, res.getImageContent());
							} catch (IOException e) { }
    					}
    					logger.error("imaginea este : " + res.getStatus() + " searchId: " + searchId);
    					return res;
	        		}
	    		}
    		}
		}
		return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
	}

	protected String getFileNameFromLink(String url) {
		/*
		 * int start; start = url.indexOf("Instrs"); start = url.indexOf("=",
		 * start) + 1;
		 * 
		 * String rez = url.substring(start);
		 */

		String rez = url.replaceAll(".*Instrs=(.*?)(?=&|$)", "$1");

		if (rez.trim().length() > 10)
			rez = rez.replaceAll("&parentSite=true", "");

		return rez.trim() + ".html";
	}


	protected boolean validFakeInstrumentDate(String filledDate) {
		String tsu = mSearch.getSa().getAtribute(SearchAttributes.SEARCHUPDATE);
		if (tsu != null && tsu.trim().toLowerCase().equals("true") || mSearch.getSa().isDateDown()) {
			return false;
		}
		return true;
	}

	/**
	 * Filter by address, but also allow the subdivision name to be in place of
	 * address
	 * 
	 * @author radu bacrau
	 */
	public class AddressSubdivisionFilter extends FilterResponse {

		private static final long serialVersionUID = -2065906455691581970L;

		private FilterResponse addressFilter;

		public AddressSubdivisionFilter(long searchId, double thresh) {
			super(searchId);
			addressFilter = AddressFilterFactory.getAddressHighPassFilter(
					searchId, thresh);
			setThreshold(new BigDecimal(thresh));
		}

		@Override
		public String getFilterName() {
			return addressFilter.getFilterName();
		}

		@Override
		public String getFilterCriteria() {
			return addressFilter.getFilterCriteria() + " or Addr='" + getRefSubdivName() + "'";
		}

		private String getRefSubdivName() {
			return getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME);
		}

		public BigDecimal getScoreOneRow(ParsedResponse row) {
			// first check if address match
			BigDecimal score = addressFilter.getScoreOneRow(row);
			if (score.compareTo(threshold) > 0) {
				return score;
			}
			// check if we have reference subdivision and candidate addresses
			String refSubdiv = getRefSubdivName();
			if (StringUtils.isEmpty(refSubdiv) || row.getPropertyIdentificationSetCount() == 0) {
				return score;
			}
			// match subdivision name with addresses
			for (int i = 0; i < row.getPropertyIdentificationSetCount(); i++) {
				String candSubdiv = MatchEquivalents.getInstance(searchId).getEquivalent(row.getPropertyIdentificationSet(i)
						.getAtribute("StreetName"));
				if (StringUtils.isEmpty(candSubdiv)) {
					continue;
				}
				BigDecimal newScore = new BigDecimal(GenericNameFilter
						.calculateMatchForLast(refSubdiv, candSubdiv, threshold.doubleValue(), null));
				if (newScore.compareTo(score) > 0) {
					score = newScore;
				}
			}
			// return maximum score
			return score;
		}
	}
	
	@Override
	protected void setCertificationDate() {

		try {		
	        logger.debug("Intru pe get Certification Date - davidson");
	        
	        if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
				
				HttpSite3 site = null;
				try {
					site = HttpManager3.getSite(getCrtTSServerName(miServerID), searchId);
					if (site != null) {
						HTTPRequest request = new HTTPRequest(getBaseLink());
						HTTPResponse response = ((ro.cst.tsearch.connection.http3.TNDavidsonRO) site).process(request);
						if (response != null){
							String resp = response.getResponseAsString();
							
							Matcher certDateMatcher = certDatePattern.matcher(resp);
							if(certDateMatcher.find()) {
								String date = certDateMatcher.group(1).trim();
								Date d = CertificationDateManager.sdfIn.parse(date);
					            	
								date = CertificationDateManager.sdfOut.format(d);
					            	
								CertificationDateManager.cacheCertificationDate(dataSite, date);
								getSearch().getSa().updateCertificationDateObject(dataSite, d);
							}
						}
					}
				} finally {
					// always release the HttpSite
					HttpManager3.releaseSite(site);
				}
			}
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
	}
	
	@Override
   	public String specificClean(String htmlContent){
   		htmlContent = htmlContent.replaceAll("(?is)</?a[^>]*>", "");
   		htmlContent = htmlContent.replaceAll("(?is)<input[^>]*>\\s*Select for saving to TS Report", "");
   		htmlContent = htmlContent.replaceAll("(?is)<input[^>]*>", "");
    	return htmlContent;
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
		module.setParamValue(3, "Detail Data");
		module.setParamValue(4, "DESC");
		if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module.forceValue(1, book);
			module.forceValue(2, page);
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
		} else {
			module = null;
		}
		return module;
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		if(document == null || StringUtils.isEmpty(document.getInstrumentNumber())) {
			return null;
		}
		return "p1=019&p2=1&searchId=" + getSearch().getID() + "&ActionType=2&Link=/recording/LoadImage.asp&InstrID=" + document.getInstrumentNumber();
	}

}
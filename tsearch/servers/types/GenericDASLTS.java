package ro.cst.tsearch.servers.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.w3c.dom.Document;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface.DaslResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.GenericMultipleAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.misc.PropertyTypeFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.SubdivisionFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
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
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;
import ro.cst.tsearch.utils.gargoylesoftware.HtmlElementHelper;

import com.gargoylesoftware.htmlunit.Page;
import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

/**
 * @author Cristi Stochina
 */
@SuppressWarnings("deprecation")
public class GenericDASLTS extends TSServerDASLAdapter implements TSServerROLikeI {

	private static final long serialVersionUID = 2904039303111523636L;

	private static final ServerPersonalData pers = new ServerPersonalData();
	
	private static final String TS_FAKE_RESPONSE = StringUtils.fileReadToString(FAKE_FOLDER + "DASLFakeResponse.xml");

	protected static final String DASL_TEMPLATE_CODE = "DASL_TS";
	
	private static final HashSet<String> tp3Counties = new HashSet<String>();
	static {
		tp3Counties.add("garfield");
		tp3Counties.add("routt");
		tp3Counties.add("sanmiguel");
		tp3Counties.add("summit");
	}
	
	static {
		pers.setTemplatePrefixName(DASL_TEMPLATE_CODE);
		{
			int id = 0;
			pers.addXPath(id, "//TitleDocument", ID_SEARCH_BY_ADDRESS);
			pers.addXPath(id, "//TitleDocument", ID_DETAILS);
			pers.addXPath(id, "//TitleDocument", ID_SEARCH_BY_BOOK_AND_PAGE);
			pers.addXPath(id, "//TitleDocument", ID_SEARCH_BY_INSTRUMENT_NO);
		}
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();
		
		
		for (int moduleIndex : new int[]{
				TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, 
				TSServerInfo.INSTR_NO_MODULE_IDX,
				TSServerInfo.NAME_MODULE_IDX,
				TSServerInfo.ADDRESS_MODULE_IDX,
				TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX,
				TSServerInfo.SUBDIVISION_MODULE_IDX}) {
		
			TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(moduleIndex);
			if(tsServerInfoModule != null) {
				
				HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
				for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
					nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
					
				}
				
				PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
				for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					
					
					String functionName = htmlControl.getCurrentTSSiFunc().getName();
					if(StringUtils.isNotEmpty(functionName)) {
						String comment = moduleWrapperManager.getCommentForSiteAndFunction(
								siteName, moduleIndex, nameToIndex.get(functionName));
						if(comment != null) {
							htmlControl.setFieldNote(comment);
						}
					}
				}
			}
		}
		
		/*
		for (int moduleIndex : new int[]{TSServerInfo.PARCEL_ID_MODULE_IDX, TSServerInfo.DASL_GENERAL_SEARCH_MODULE_IDX}) {
			TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(moduleIndex);
			if(tsServerInfoModule != null) {
				PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
				for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(
							siteName, moduleIndex, 0);
					if(comment != null) {
						htmlControl.setFieldNote(comment);
					}
					break;
				}
			}
		}
		*/
		
		return msiServerInfoDefault;
	}
	
	
	@Override
	protected HashMap<String, Object> fillTemplatesParameters(Map<String, String> params) {
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		Search search = currentInstance.getCrtSearchContext();
		HashMap<String, Object> templateParams = super.fillTemplatesParameters(params);
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				currentInstance.getCommunityId(),
				miServerID);
		String sateName = dat.getName();
		String stateAbrev = sateName.substring(0, 2);
		String countyName = currentInstance.getCurrentCounty().getName();
		
		String APN = params.get("APN");
		templateParams.put(AddDocsTemplates.DASLAPN, APN);
		String chain = params.get("chain");
		templateParams.put(AddDocsTemplates.DASLPropertyChainOption, chain);
		String includeTax = params.get("includeTax");
		templateParams.put(AddDocsTemplates.DASLIncludeTaxFlag, includeTax);
		
		String daslImageId = params.get("DASLimageId");
		if(StringUtils.isEmpty(daslImageId)){
			daslImageId = "";
		}
		
		templateParams.put(AddDocsTemplates.DASLimageId, daslImageId);
		
		String role1 = params.get("role1");
		templateParams.put(AddDocsTemplates.DASLPartyRole_1, role1);
		String role2 = params.get("role2");
		templateParams.put(AddDocsTemplates.DASLPartyRole_2, role2);
		
		String firstName1 = params.get("firstName1");
		if (firstName1 != null) {
			firstName1 = firstName1.replaceAll("[*]", "");
		}
		templateParams.put(AddDocsTemplates.DASLFirstName_1, firstName1);
		String firstName2 = params.get("firstName2");
		templateParams.put(AddDocsTemplates.DASLFirstName_2, firstName2);

		String middleName1 = params.get("middleName1");
		if (middleName1 != null) {
			middleName1 = middleName1.replaceAll("[*]", "");
		}
		templateParams.put(AddDocsTemplates.DASLMiddleName_1, middleName1);
		String middleName2 = params.get("middleName2");
		templateParams.put(AddDocsTemplates.DASLMiddleName_2, middleName2);

		String lastName1 = params.get("lastName1");
		if (lastName1 != null) {
			lastName1 = lastName1.replaceAll("[*]", "");
		}
		templateParams.put(AddDocsTemplates.DASLLastName_1, lastName1);

		String lastName2 = params.get("lastName2");
		templateParams.put(AddDocsTemplates.DASLLastName_2, lastName2);

		String nickName = params.get("nickName");
		templateParams.put(AddDocsTemplates.DASLNickName, nickName);

		String withProperty = params.get("withProperty");
		templateParams.put(AddDocsTemplates.DASLWithProperty, withProperty);

		String soundIndex = params.get("sounddex");
		templateParams.put(AddDocsTemplates.DASLSoundIndex, soundIndex);

		String fromDate = params.get("fromDate");
		templateParams.put(AddDocsTemplates.DASLPropertySearchFromDate, fromDate);
		templateParams.put(AddDocsTemplates.DASLPartySearchFromDate, fromDate);

		String toDate = params.get("toDate");
		templateParams.put(AddDocsTemplates.DASLPropertySearchToDate, toDate);
		templateParams.put(AddDocsTemplates.DASLPartySearchToDate, toDate);

		String searchPropType = params.get("DASLPropertySearchType");
		templateParams.put(AddDocsTemplates.DASLPropertySearchType, searchPropType);

		String searchPartyType = params.get("DASLPartySearchType");
		templateParams.put(AddDocsTemplates.DASLPartySearchType, searchPartyType);

		String DASLImageSearchType = params.get("DASLImageSearchType");
		templateParams.put(AddDocsTemplates.DASLImageSearchType, DASLImageSearchType);

		String lot = params.get("lot");
		templateParams.put(AddDocsTemplates.DASLLot, lot);

		String lotThrough = params.get("lotThrough");
		templateParams.put(AddDocsTemplates.DASLLotThrough, lotThrough);
		
		String sublot = params.get("sublot");
		templateParams.put(AddDocsTemplates.DASLSubLot, sublot);

		String block = params.get("block");
		templateParams.put(AddDocsTemplates.DASLBlock, block);

		String platBook = params.get("platBook");
		templateParams.put(AddDocsTemplates.DASLPlatBook, platBook);

		String platPage = params.get("platPage");
		templateParams.put(AddDocsTemplates.DASLPlatPage, platPage);

		String platYear = params.get("platYear");
		templateParams.put(AddDocsTemplates.DASLPlatDocumentYear, platYear);
		
		String book = params.get("book");
		templateParams.put(AddDocsTemplates.DASLBook, book);

		String page = params.get("page");
		templateParams.put(AddDocsTemplates.DASLPage, page);

		String docno = params.get("docno");
		templateParams.put(AddDocsTemplates.DASLDocumentNumber, docno);

		String platDocNo = params.get("platDocNo");
		templateParams.put(AddDocsTemplates.DASLPlatDocumentNumber, platDocNo);

		String section = params.get("section");
		templateParams.put(AddDocsTemplates.DASLSection, section);

		String township = params.get("township");
		templateParams.put(AddDocsTemplates.DASLTownship, township);

		String range = params.get("range");
		templateParams.put(AddDocsTemplates.DASLRange, range);

		String quarterOrder = params.get("quarterOrder1");
		templateParams.put(AddDocsTemplates.DASLQuarterOrder1, quarterOrder);

		String quaterValue = params.get("quarterValue1");
		templateParams.put(AddDocsTemplates.DASLQuaterValue1, quaterValue);

		quarterOrder = params.get("quarterOrder2");
		templateParams.put(AddDocsTemplates.DASLQuarterOrder2, quarterOrder);

		quaterValue = params.get("quarterValue2");
		templateParams.put(AddDocsTemplates.DASLQuaterValue2, quaterValue);
		
		quarterOrder = params.get("quarterOrder3");
		templateParams.put(AddDocsTemplates.DASLQuarterOrder3, quarterOrder);

		quaterValue = params.get("quarterValue3");
		templateParams.put(AddDocsTemplates.DASLQuaterValue3, quaterValue);
		
		quarterOrder = params.get("quarterOrder4");
		templateParams.put(AddDocsTemplates.DASLQuarterOrder4, quarterOrder);

		quaterValue = params.get("quarterValue4");
		templateParams.put(AddDocsTemplates.DASLQuaterValue4, quaterValue);
		
		String arb = params.get("arb");
		templateParams.put(AddDocsTemplates.DASLARB, arb);

		String reference = search.getOrderNumber();

		templateParams.put(AddDocsTemplates.DASLClientReference, reference);
		templateParams.put(AddDocsTemplates.DASLClientTransactionReference, reference);

		String nr = params.get("number");
		templateParams.put(AddDocsTemplates.DASLStreetNumber, nr);

		String name = params.get("name");
		templateParams.put(AddDocsTemplates.DASLStreetName, name);

		String suffix = params.get("suffix");
		templateParams.put(AddDocsTemplates.DASLStreetSuffix, suffix);

		String dir = params.get("direction");
		templateParams.put(AddDocsTemplates.DASLStreetDirection, dir);

		templateParams.put(AddDocsTemplates.DASLStateAbbreviation, stateAbrev);

		templateParams.put(AddDocsTemplates.DASLCountyFIPS, dat.getCountyFIPS());

		templateParams.put(AddDocsTemplates.DASLStateFIPS, dat.getStateFIPS());

		templateParams.put(AddDocsTemplates.DASLYearFiled,  params.get("year"));
		
		templateParams.put(AddDocsTemplates.DASLRealPartySearchType1,  params.get("partySearchType1"));
		
		templateParams.put(AddDocsTemplates.DASLUnit,  params.get("unit"));
		
		templateParams.put(AddDocsTemplates.DASLSubdivisionUnit,  params.get("subivUnit"));
		
		templateParams.put(AddDocsTemplates.DASLUnitPrefix,  params.get("unitPrefix"));
		
		templateParams.put(AddDocsTemplates.DASLCity,  params.get("city"));
		
		templateParams.put(AddDocsTemplates.DASLZip,  params.get("zipp"));
		
		templateParams.put(AddDocsTemplates.DASLDocumentSearchType, params.get("DASLDocumentSearchType"));
		
		templateParams.put(AddDocsTemplates.DASLPreviousARB, params.get("prevArb"));
		
		templateParams.put(AddDocsTemplates.DASLPreviousParcel, params.get("prevParcel"));
		
		templateParams.put(AddDocsTemplates.DASLParcel, params.get("parcel"));
		
		templateParams.put(AddDocsTemplates.DASLAbstractNumber, params.get("abstractNumber"));
		
		templateParams.put(AddDocsTemplates.DASLAbstractName, params.get("abstractName"));
		
		templateParams.put(AddDocsTemplates.DASLSubdivision, params.get("subdivision"));
		
		templateParams.put(AddDocsTemplates.DASLAddition, params.get("addiction"));
		
		/* --- this applies only for image search ---- */
		templateParams.put(AddDocsTemplates.DASLProviderId, "87");
		if("CO".equalsIgnoreCase(stateAbrev)){
			if(tp3Counties.contains(countyName.toLowerCase().replaceAll("\\s+", ""))){
				templateParams.put(AddDocsTemplates.DASLProviderId, "8");
			}
		}
		/* --- this applies only for image search ---- */
		
		templateParams.put(AddDocsTemplates.DASLClientId, SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericTSSite", "DaslClienId"));
		
		return templateParams;
	}
	
	protected void ParseResponse(String sAction, ServerResponse response, int viParseID) throws ServerResponseException {
		super.ParseResponse(sAction, response, viParseID);
	}
	
	public GenericDASLTS(long searchId) {
		super(searchId);
	}

	public GenericDASLTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected ServerPersonalData getServerPersonalData() {
		return pers;
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
		int functionBook = 0;
		int functionPage = 1;
		int functionDocNo = 2;
		int fuctionDASLSearchType = 3;
		
		TSServerInfo info = getDefaultServerInfo();
		TSServerInfoModule module = info.getModule(TSServerInfo.IMG_MODULE_IDX);
		String bookAlias = module.getFunction(functionBook/* Book */).getParamAlias();
		String pageAlias = module.getFunction(functionPage/* Page */).getParamAlias();
		String docNumberAlias = module.getFunction(functionDocNo/* DocNumber */).getParamAlias();
		
		String book = "";
		String page = "";
		String docNumber = "";
		
		String link = image.getLink();
		int poz = link.indexOf("?");
		
		if(poz>0){
			link = link.substring(poz+1);
		}
		
		String[] allParameters = link.split("[&=]");
		
		for(int i=0;i<allParameters.length-1;i+=2){
			if(bookAlias.equalsIgnoreCase(allParameters[i])){
				book = allParameters[i+1];
			}
			else if(pageAlias.equalsIgnoreCase(allParameters[i])){
				page = allParameters[i+1];
			}
			else if(docNumberAlias.equalsIgnoreCase(allParameters[i])){
				docNumber = allParameters[i+1];
			}
		}
		
		module.getFunction(functionBook).setParamValue(book);
		module.getFunction(functionPage).setParamValue(page);
		module.getFunction(functionDocNo).setParamValue(docNumber);
		module.getFunction(fuctionDASLSearchType).setParamValue("DT");

		String imageName = image.getPath();
    	if(FileUtils.existPath(imageName)){
    		byte b[] = FileUtils.readBinaryFile(imageName);
    		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
    	}
		
    	//####this is used temporary for the plat from TXGenericSWData
		if (link.contains("/client/maps") && !FileUtils.existPath(imageName)){
			Page dataSheet = HtmlElementHelper.getHtmlPageByURL(link);
			byte[] contentAsBytes = dataSheet.getWebResponse().getContentAsBytes();
			return new DownloadImageResult(DownloadImageResult.Status.OK, contentAsBytes,
					image.getContentType());
		}
		//####
		
		String fileName = mSearch.getImagesTempDir() + docNumber + "_" + book + "_" + page + ".";
		if (image.getContentType().contains("tif")) {
			fileName += "tif";
	    } else if (image.getContentType().contains("pdf")) {
	    	fileName += "pdf";
	    }
		if(FileUtils.existPath(fileName)){
    		byte b[] = FileUtils.readBinaryFile(fileName);
    		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
    	}
		
		return searchBy(module, image, null).getImageResult();
	}
	
	
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		SearchAttributes sa = global.getSa();
		
		sa.setAtribute(SearchAttributes.LD_TS_SUBDIV_NAME, sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME));
		sa.setAtribute(SearchAttributes.LD_TS_PLAT_BOOK, sa.getAtribute(SearchAttributes.LD_BOOKNO));
		sa.setAtribute(SearchAttributes.LD_TS_PLAT_PAGE, sa.getAtribute(SearchAttributes.LD_PAGENO));
		
		sa.setAtribute(SearchAttributes.LD_TS_LOT, sa.getAtribute(SearchAttributes.LD_LOTNO));
		sa.setAtribute(SearchAttributes.LD_TS_BLOCK, sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK));
		
		if(dontMakeTheSearch(module, searchId) && !isParentSite()){
			return new ServerResponse();
		}
		
		if (module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX 
				|| module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if (!modules.isEmpty()) {
				return super.searchByMultipleInstrument(modules, sd, null);
			}
		}
		
		ServerResponse response = searchByMultipleInstrument(module, sd);
		if (response!=null) {
			return response;
		}
		
		if (module.getModuleIdx() == TSServerInfo.DASL_GENERAL_SEARCH_MODULE_IDX) {
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if (!modules.isEmpty()) {
				List<ServerResponse> responses = new ArrayList<ServerResponse>();
				for (TSServerInfoModule mod: modules) {
					String book = mod.getParamValue(0);
					String page = mod.getParamValue(1);
					String doc = mod.getParamValue(2);
					if ((!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page))||!StringUtils.isEmpty(doc)) {
						mod.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, Boolean.TRUE);
						responses.add(searchBy(mod, sd, null));
					}
				}
				return mergeResponses(responses);
			} else {
				String book = module.getParamValue(0);
				String page = module.getParamValue(1);
				String doc = module.getParamValue(2);
				if ((StringUtils.isEmpty(book)||StringUtils.isEmpty(page))&&StringUtils.isEmpty(doc)) {
					return new ServerResponse();
				}
			}
		}
		
		return searchBy(module, sd, null);
		
	}
	
	protected ServerResponse searchByMultipleInstrument(TSServerInfoModule module, Object sd) throws ServerResponseException {
		ServerResponse response = null;
		
		if (module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) {
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if (!modules.isEmpty()) {
				List<ServerResponse> responses = new ArrayList<ServerResponse>();
				for (TSServerInfoModule mod: modules) {
					String book = mod.getParamValue(0);
					String page = mod.getParamValue(1);
					if (!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page)) {
						mod.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, Boolean.TRUE);
						responses.add(searchBy(mod, sd, null));
					}
				}
				return mergeResponses(responses);
			}
		}
		
		if (module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX) {
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if (!modules.isEmpty()) {
				List<ServerResponse> responses = new ArrayList<ServerResponse>();
				for (TSServerInfoModule mod: modules) {
					String doc = mod.getParamValue(0);
					if (!StringUtils.isEmpty(doc)) {
						mod.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, Boolean.TRUE);
						responses.add(searchBy(mod, sd, null));
					}
				}
				return mergeResponses(responses);
			}
		}
		
		return response;
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean contains(Vector vector, ParsedResponse parsedResponse) {
		Vector v = parsedResponse.getResultRows();
		if (v.size()>0) {
			DocumentI document = ((ParsedResponse)v.elementAt(0)).getDocument();
			if (document!=null) {
				Iterator it = vector.iterator();
				while (it.hasNext()) {
					DocumentI documentV = ((ParsedResponse)it.next()).getDocument();
					if (documentV!=null && document.flexibleEquals(documentV)) {
							return true;
						}
				}
			}
		}
		return false;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected ServerResponse mergeResponses(List<ServerResponse> responses) {
		if (responses==null || responses.size()==0) {
			return new ServerResponse();
		}
		if (responses.size()==1) {
			return responses.get(0);
		}
		Vector vector = new Vector();
		String footer = "";
		for (ServerResponse response: responses) {
			ParsedResponse parsedResponse = response.getParsedResponse();
			if (!contains(vector, parsedResponse)) {
				Vector v = parsedResponse.getResultRows();
				Iterator it = v.iterator();
				while (it.hasNext()) {
					vector.add(it.next());
				}
				if (footer=="") {
					footer = parsedResponse.getFooter();
				} else if (footer.contains(CreateFileAlreadyInTSD()) && !parsedResponse.getFooter().contains(CreateFileAlreadyInTSD())) {
					footer = parsedResponse.getFooter();
				}
			} 
		}
		ServerResponse newResponse = responses.get(0);
		newResponse.getParsedResponse().setResultRows(vector);
		newResponse.getParsedResponse().setFooter(footer);
		return newResponse;
	}
	
	protected ServerResponse searchBy(TSServerInfoModule module, Object sd, String fakeResponse) throws ServerResponseException {
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		DaslResponse daslResponse = null;
		
		boolean isMultiplePINSearch = false;
		if (global.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) != null){
			if (Boolean.TRUE.equals(global.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN))){
				isMultiplePINSearch = true;
			}
		}
		if( module.getModuleIdx() != TSServerInfo.IMG_MODULE_IDX 
				&& (!Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS)))
				&& !isMultiplePINSearch){
			global.removeAllInMemoryDocs();
		}
		
		global.clearClickedDocuments();
		if(fakeResponse==null){
			// log the search in the SearchLogger
			logSearchBy(module);
		}
		// get search parameters
		Map<String, String> params = getNonEmptyParams(module, null);

		int moduleIDX = module.getModuleIdx();
		int parserID = module.getParserID();

		if (moduleIDX == TSServerInfo.DASL_GENERAL_SEARCH_MODULE_IDX) {
			return makeAdditionalImageViewSearch(params, false, false);
		}
		else if (moduleIDX == TSServerInfo.IMG_MODULE_IDX) {
			return imageSearch(module, sd, "");
		}
		else{
			if ( ( daslResponse = super.performSearch(params, moduleIDX, fakeResponse))==null ){
				String mess = "</br><font color=\"red\">Not enough data entered for a search to be performed!</font></br>";
				SearchLogger.info(mess, searchId);
				return ServerResponse.createErrorResponse("Not enough data entered for a search to be performed!");
			}
		}
		if(daslResponse != null) {
			daslResponse.setAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE, module);
		}
		ServerResponse serverResponse = processDaslResponse(daslResponse,moduleIDX,parserID);
		
		if ((moduleIDX == TSServerInfo.INSTR_NO_MODULE_IDX || moduleIDX == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX)
				&& serverResponse.getParsedResponse().getResultsCount() == 0){
			boolean doNotRemoveInMemoryDocs = Boolean.TRUE.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS));
			return makeAdditionalImageViewSearch(params, true, doNotRemoveInMemoryDocs);
		}
		
		processRelated(module, serverResponse);
		
		return serverResponse;

	}


	public ServerResponse makeAdditionalImageViewSearch(Map<String, String> params, boolean isNotFakeImageViewSearch, boolean doNotRemoveInMemoryDocs) throws ServerResponseException {
		
		ServerResponse serverResponse = new ServerResponse();
		
		TSInterface server = TSServersFactory.GetServerInstance((int)TSServersFactory.getSiteId(dataSite.getStateAbbreviation(), dataSite.getCountyName(), "TS"), "", "", mSearch.getID());
		
		TSServerInfoModule mod = server.getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.IMG_MODULE_IDX, new SearchDataWrapper());
		if (org.apache.commons.lang.StringUtils.isNotEmpty(params.get("docno"))){
			mod.setParamValue(2, params.get("docno"));
		} else if (org.apache.commons.lang.StringUtils.isNotEmpty(params.get("book")) && org.apache.commons.lang.StringUtils.isNotEmpty(params.get("page"))){
			mod.setParamValue(0, params.get("book"));
			mod.setParamValue(1, params.get("page"));
		}
		
		ImageLinkInPage ilip = new ImageLinkInPage(isNotFakeImageViewSearch);
		
		if (isNotFakeImageViewSearch) {
			SearchLogger.info("</br><font color=\"black\">No document found. One more try will be made using Image View Search module.</font></br>", searchId);
		}
		
		String imageName = "";
		if (!isNotFakeImageViewSearch) {
			String docno = params.get("docno");
			String book = params.get("book");
			String page = params.get("page");
			imageName = (docno!=null?docno:"") + "_" + (book!=null?book:"") + "_" + (page!=null?page:"");
		}
		
		ServerResponse sResp = imageSearch(mod, ilip, imageName);

		List<String> fakeResponses = new ArrayList<String>();
		List<TSServerInfoModule> fakeModules = new ArrayList<TSServerInfoModule>();
		
		if (sResp.getImageResult() != null){
			if (sResp.getImageResult().getStatus() == DownloadImageResult.Status.OK){
				String book = mod.getParamValue(0);
				String page = mod.getParamValue(1);
				String docNo = mod.getParamValue(2);
				
				String grantor  = "";
				String grantee = "";
				
				String doc = TS_FAKE_RESPONSE.replaceAll("@@Grantee@@", grantee);
				doc = doc.replaceAll("@@Grantor@@", grantor);
				doc = doc.replaceAll("@@Book@@", book == null ? "" : book);
				doc = doc.replaceAll("@@Page@@", page == null ? "" : page);
				doc = doc.replaceAll("@@DocNo@@", docNo == null ? "" : docNo);
				doc = doc.replaceAll("@@Date@@", "01/01/1960");
				doc = doc.replaceAll("@@Type@@", "");
				
				fakeResponses.add(doc);
				TSServerInfoModule fakeModule = getDefaultServerInfo().getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
				if (!isNotFakeImageViewSearch || doNotRemoveInMemoryDocs) {
					fakeModule.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_DO_NOT_REMOVE_IN_MEMORY_DOCS, Boolean.TRUE);
				}
				fakeModules.add(fakeModule);
			}
		}
		if(!fakeModules.isEmpty()){
			return searchByMultipleInstrument(fakeModules, ilip, fakeResponses);
		}
		
		return serverResponse;
	}
	protected Map<InstrumentI, DocumentI> processRelated(TSServerInfoModule module, ServerResponse serverResponse) {
		
		Map<InstrumentI, DocumentI> temporaryDocuments = new HashMap<InstrumentI, DocumentI>();
		
		if(isRelatedModule(module)) {
			
			String modBook = null;
			String modPage = null;
			String modInstNo = null;
			
			boolean bookPage = org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(2).getParamValue())
					&& org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(3).getParamValue());
			boolean instrNo = org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(0).getParamValue());
			
			if(bookPage) {
				modBook = module.getFunction(2).getParamValue().trim();
				modPage = module.getFunction(3).getParamValue().trim();
			}
			if(instrNo) {
				modInstNo = module.getFunction(0).getParamValue().trim();
			}
			
			if(bookPage || instrNo) {
			
				Vector resultRows = serverResponse.getParsedResponse().getResultRows();
				if(resultRows != null && !resultRows.isEmpty()) {
					for (Object object : resultRows) {
						if (object instanceof ParsedResponse) {
							ParsedResponse parsedResponse = (ParsedResponse) object;
							DocumentI document = parsedResponse.getDocument();
							
							Set<InstrumentI> parsedReferences = document.getParsedReferences();
							boolean foundReference = false;
							if(parsedReferences != null && !parsedReferences.isEmpty()) {
								for (InstrumentI instrumentI : parsedReferences) {
									if(bookPage) {
										if(modBook.equals(instrumentI.getBook()) && modPage.equals(instrumentI.getPage())) {
											foundReference = true;
											break;
										}
									}
									if(instrNo) {
										if(modInstNo.equals(instrumentI.getInstno())) {
											foundReference = true;
											break;
										}
									}
								}
							}
							if(!foundReference) {
								InstrumentI newReference = new Instrument();
								if(bookPage) {
									newReference.setBook(modBook);
									newReference.setPage(modPage);
								}
								if(instrNo) {
									newReference.setInstno(modInstNo);
								}
								document.addParsedReference(newReference);
								temporaryDocuments.put(document.getInstrument(), document);
							}
							
						}
					}
				}
			}
		}
		
		return temporaryDocuments;
	}
	
	protected boolean isRelatedModule(TSServerInfoModule firstModule) {
		return firstModule.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX && firstModule.getFunctionCount() == 5;
	}
	
	protected static boolean dontMakeTheSearch(TSServerInfoModule module,	long searchId) {
		if(module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX ){
			if(StringUtils.isEmpty(module.getParamValue(0))||StringUtils.isEmpty(module.getParamValue(1))){
				return true;
			}
		} 
		if(module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX){
			String instr = module.getParamValue(0);
			if(instr!=null){
				if(instr.replaceAll("[^a-zA-Z0-9]", "").length()==0){
					return true;
				}
			}
			if(StringUtils.isEmpty(instr)){
				return true;
			}
		}
		if("true".equalsIgnoreCase(InstanceManager.getManager().getCurrentInstance(searchId)
				.getCrtSearchContext().getSa().getAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND))
				&& (module.getModuleIdx() != TSServerInfo.NAME_MODULE_IDX
    				|| (SearchAttributes.BUYER_OBJECT.equals(module.getSaObjKey()))
    				)) {
			return true;
		}
		return false;
	}

	protected boolean addBookPageSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId,  boolean isUpdate){
		if(inst.hasBookPage()){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			module.setData(0, inst.getBook().replaceFirst("^0+", ""));
			module.setData(1, inst.getPage().replaceFirst("^0+", ""));
			if (isUpdate) {
				module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId)); 
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	private static boolean addDocNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, boolean isUpdate){
		if ( inst.hasDocNo() ){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			String  instNo = inst.getDocno().replaceFirst("^0+", "");
			instNo = appendBeginingZero(6-instNo.length(),instNo);
			if(inst.hasYear()){
				instNo = inst.getYear() + instNo;
			}
			instNo  = instNo .replaceFirst("^0+", "");
			module.setData(0,  instNo);
			if (isUpdate) {
				module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	protected static String appendBeginingZero(int dif, String str){
		StringBuilder buf = new StringBuilder(str); 
		for(int i=0;i<dif;i++){
			buf.insert(0, "0");
		}
		return buf.toString();
	}
	
	protected String prepareInstrumentNoForCounty(InstrumentI inst){
		String instNo = inst.getInstno().replaceFirst("^0+", "");
		instNo = appendBeginingZero(6-instNo.length(),instNo);
		if(inst.hasYear()){
			if(inst.getYear()>=2000 && instNo.length()==6){
				instNo = inst.getYear() + instNo;
			} else if(inst.getYear()>=2000 && instNo.length()==7){
				instNo = instNo.substring(1);
				instNo = appendBeginingZero(6-instNo.length(),instNo);
				instNo = inst.getYear()+instNo;
			} else if(instNo.length()==7){
				String year = instNo.substring(0,2);
				instNo = instNo.substring(2);
				instNo = appendBeginingZero(6-instNo.length(),instNo);
				instNo = year+instNo;
			} else if (instNo.length() == 6) {
				instNo = inst.getYear() + instNo;
			}
		}
		instNo  = instNo .replaceFirst("^0+", "");
		return instNo;
	}
	
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, boolean isUpdate){
		if ( inst.hasInstrNo() ){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			String  instNo = prepareInstrumentNoForCounty(inst);
			
			module.setData(0,  instNo);
			if (isUpdate) {
				module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	protected  boolean addAoLookUpSearches(TSServerInfo serverInfo, List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef,long searchId,  boolean isUpdate){
		boolean atLeastOne = false;
		for(InstrumentI inst:allAoRef){
			try {
				boolean test = addBookPageSearch(inst, serverInfo, modules, searchId, isUpdate);
				atLeastOne = atLeastOne || test;
				
				test = addDocNoSearch(inst, serverInfo, modules, searchId, isUpdate);
				atLeastOne = atLeastOne || test;
				
				test = addInstNoSearch(inst, serverInfo, modules, searchId, isUpdate);
				atLeastOne = atLeastOne || test;
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		return atLeastOne;
	}
	
	protected static Set<InstrumentI> getAllAoReferences(Search search){
		Set<InstrumentI> allAoRef = new HashSet<InstrumentI>();
		DocumentsManagerI manager = search.getDocManager();
		try{
			manager.getAccess();
			List<DocumentI> list = manager.getDocumentsWithType( true, DType.ASSESOR, DType.TAX );
			for(DocumentI assessor:list){
				if (HashCountyToIndex.isLegalBootstrapEnabled(search.getCommId(), assessor.getSiteId())) {
					for(RegisterDocumentI reg : assessor.getReferences()){
						allAoRef.add(reg.getInstrument());
					}
					allAoRef.addAll(assessor.getParsedReferences());
				}
			}
		}
		finally {
			manager.releaseAccess();
		}
		return allAoRef;
	}
	
	
	
	protected void addOCRSearch(List<TSServerInfoModule> modules,TSServerInfo serverInfo, FilterResponse ...filters){
		// OCR last transfer - book / page search
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
	    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    module.clearSaKeys();
	    module.getFunction(0).setIteratorType( FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH );
	    module.getFunction(1).setIteratorType( FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH );
	    for(int i=0;i<filters.length;i++){
	    	module.addFilter(filters[i]);
	    }
	    addBetweenDateTest(module, false, false, false);
		modules.add(module);
		
	    // OCR last transfer - instrument search
	    module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
	    module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
	    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    module.clearSaKeys();
	    module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	    module.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_YEAR);
	    for(int i=0;i<filters.length;i++){
	    	module.addFilter(filters[i]);
	    }
	    addBetweenDateTest(module, false, false, false);
		modules.add(module);
	}
	
	
	protected  String createLinkForImage(HashMap<String,String> value){
		 HashMap<String, String> map = (HashMap<String, String>)value;
		 String book = map .get("book");
		 String page = map.get("page");
		 String docno = map.get("docno");
		 String type = map.get("type");
		 if(type ==null){
			 type ="";
		 }
		 
		 TSServerInfoModule imgModule = getDefaultServerInfoWrapper().getModule(TSServerInfo.IMG_MODULE_IDX);
		 
		 StringBuilder build = new StringBuilder("");//<a href=\"
		 build .append(createPartialLink(TSConnectionURL.idDASL,TSServerInfo.IMG_MODULE_IDX));
		 build .append("DASLIMAGE&");
		 
		 build .append( imgModule.getParamAlias( 4 ) ); /*type*/
		 build .append("=");
		 build .append(type);
		 build .append("&");
		 
		 build .append( imgModule.getParamAlias( 0 ) ); /*book*/
		 build .append("=");
		 build .append(book);
		 build .append("&");
		 
		 build .append( imgModule.getParamAlias( 1 ) ); /*page*/
		 build .append("=");
		 build .append(page);
		 build .append("&");
		 
		 build .append( imgModule.getParamAlias( 2 ) ); /*docno*/
		 build .append("=");
		 build .append(docno);
		 return build.toString();
	}
	
	protected ArrayList<NameI>  addNameSearch( List<TSServerInfoModule> modules, TSServerInfo serverInfo,String key, ArrayList<NameI> searchedNames, FilterResponse ...filters ) {
		
		TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		module.clearSaKeys();
		module.setSaObjKey(key);

		for (int i = 0; i < filters.length; i++) {
			module.addFilter(filters[i]);
		}
		addBetweenDateTest(module, false, true, true);
		addFilterForUpdate(module, true);
		
		module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		
		ConfigurableNameIterator nameIterator = getConfiguredNameIterator(searchedNames, module);
		
		searchedNames = nameIterator.getSearchedNames() ;
		module.addIterator( nameIterator );
		
		modules.add( module );
		return searchedNames;
	}

	public ConfigurableNameIterator getConfiguredNameIterator(
			ArrayList<NameI> searchedNames, TSServerInfoModule module) {
		ConfigurableNameIterator nameIterator;
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, getNameSearchDerivations());
		nameIterator.setAllowMcnPersons( true );
		
		if ( searchedNames!=null ) {
			nameIterator.setInitAgain( true );
			nameIterator.setSearchedNames( searchedNames );
		}
		return nameIterator;
	}
	
	protected void addIteratorModule( TSServerInfo serverInfo, List<TSServerInfoModule> modules,int code, long searchId, boolean isUpdate, boolean lookUpWasWithNames, boolean legalFromLastTransferOnly){
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.clearSaKeys();
		module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		LegalDescriptionIterator it = getLegalDescriptionIterator(searchId, lookUpWasWithNames, legalFromLastTransferOnly);
		module.addIterator(it);
		if (isUpdate) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}
		//GenericMultipleLegalFilter legalFilter = getGenericMultipleLegalFilter();
		//legalFilter.setUseLegalFromSearchPage(true);
		//module.addFilter( new SubdivisionFilter(searchId) );
		//module.addFilter( legalFilter );
		modules.add(module);
	}


	public LegalDescriptionIterator getLegalDescriptionIterator(long searchId,
			boolean lookUpWasWithNames, boolean legalFromLastTransferOnly) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookUpWasWithNames, legalFromLastTransferOnly, getDataSite());
		it.setEnableTownshipLegal(false);
		return it;
	}
	
	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		
		ConfigurableNameIterator nameIterator = null;
		Search search = getSearch();
		int searchType = search.getSearchType();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		if(searchType == Search.GO_BACK_ONE_LEVEL_SEARCH) {
			SearchAttributes sa = search.getSa();	
			
		    TSServerInfoModule module;	
		    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
	
		    boolean useNameForLookUp = false;
			if(!addAoLookUpSearches(serverInfo, new ArrayList<TSServerInfoModule>(), getAllAoReferences(search), searchId, true)
					&& !(StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_LOTNO)) && StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK))) ){
				useNameForLookUp = true;
			}
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, useNameForLookUp, false, getDataSite());
			it.createDerrivations();	//i do this to load "TS_LOOK_UP_DATA" for legal filtering.
			
			FilterResponse doctypeFilter = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
	
		    for (String id : gbm.getGbTransfers()) {
				  		   	    	 
		    	module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		    	module.setIndexInGB(id);
		    	module.setTypeSearchGB("grantor");
		    	module.clearSaKeys();
		    	module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		    	String date=gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
		    	if (date!=null) 
		    		module.getFunction(0).forceValue(date);
		    	
		    	FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
				((GenericNameFilter)nameFilter).setUseSynonymsBothWays(true);
				((GenericNameFilter)nameFilter).setIgnoreMiddleOnEmpty(true);
				nameFilter.setInitAgain(true);
				
				FilterResponse transferNameFilter = NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module);
				((GenericNameFilter)transferNameFilter).setUseSynonymsBothWays(true);
				((GenericNameFilter)transferNameFilter).setIgnoreMiddleOnEmpty(true);
				transferNameFilter.setInitAgain(true);
		    	
			    module.addFilter( nameFilter );
				module.addFilter( transferNameFilter );
				module.addFilter( doctypeFilter );
		    	module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
				module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
				module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		    	nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, getNameSearchDerivations() );
			 	module.addIterator(nameIterator);
			 	modules.add(module);
			    
			     
			 	if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			 		module =new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
			 		module.setIndexInGB(id);
			 		module.setTypeSearchGB("grantee");
			 		module.clearSaKeys();
			 		module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			 		date=gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
			 		if (date!=null) 
			 			module.getFunction(0).forceValue(date);
			 		
			 		nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
					((GenericNameFilter)nameFilter).setUseSynonymsBothWays(true);
					((GenericNameFilter)nameFilter).setIgnoreMiddleOnEmpty(true);
					nameFilter.setInitAgain(true);
					
					transferNameFilter = NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module);
					((GenericNameFilter)transferNameFilter).setUseSynonymsBothWays(true);
					((GenericNameFilter)transferNameFilter).setIgnoreMiddleOnEmpty(true);
					transferNameFilter.setInitAgain(true);
			 		
			 		module.addFilter( nameFilter );
			 		module.addFilter( transferNameFilter );
			 		module.addFilter( doctypeFilter );
			 		module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
			 		module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
			 		module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
			 		nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, getNameSearchDerivations() );
			 		module.addIterator(nameIterator);			
			 		modules.add(module);
			 	}
		    }	 
		}
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);
		
	}


	public String[] getNameSearchDerivations() {
		return new String[]{"L;F;" };
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		if(searchType == Search.AUTOMATIC_SEARCH) {
			boolean isTX = ("TX".equals(global.getCrtState(false)));
			
			Set<InstrumentI> allAoRef = getAllAoReferences(global);
			
			FilterResponse nameFilterOwner 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, global.getID(), null );
			((GenericNameFilter)nameFilterOwner).setUseSynonymsBothWays(true);
			((GenericNameFilter)nameFilterOwner).setIgnoreMiddleOnEmpty(true);
			nameFilterOwner.setInitAgain(true);
			 
			FilterResponse nameFilterBuyer 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, global.getID(), null );
			((GenericNameFilter)nameFilterBuyer).setIgnoreMiddleOnEmpty(true);
			((GenericNameFilter)nameFilterBuyer).setUseSynonymsBothWays(true);
			GenericMultipleLegalFilter legalFilter = getGenericMultipleLegalFilter();
			legalFilter.setUseLegalFromSearchPage(true);
						
			GenericMultipleAddressFilter addressFilter 	= new GenericMultipleAddressFilter(searchId);
			GenericMultipleAddressFilter addressFilter1 = new GenericMultipleAddressFilter(searchId);
			
			for (AddressI address : getSearchAttributes().getForUpdateSearchAddressesNotNull(getServerID())) {
				if(StringUtils.isNotEmpty(address.getStreetName())) {
					addressFilter.addNewFilterFromAddress(address);
					addressFilter1.addNewFilterFromAddress(address);
				}
			}
			
			LastTransferDateFilter lastTransferDateFilter 	= new LastTransferDateFilter(searchId);
			PropertyTypeFilter propertyTypeFilter = new PropertyTypeFilter(searchId);
			
			FilterResponse nameFilterOwner1 = NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, global.getID(), null );
			((GenericNameFilter)nameFilterOwner1).setUseSynonymsBothWays(true);
			((GenericNameFilter)nameFilterOwner1).setIgnoreMiddleOnEmpty(true);
			nameFilterOwner1.setInitAgain(true);
			
			GenericMultipleLegalFilter legalFilter1 = getGenericMultipleLegalFilter();
			legalFilter1.setUseLegalFromSearchPage(true);
			
			FilterResponse[] filtersO 	= { nameFilterOwner, legalFilter, addressFilter, lastTransferDateFilter, propertyTypeFilter, new SubdivisionFilter(searchId) };
			FilterResponse[] filtersO1 	= { nameFilterOwner1, legalFilter1, addressFilter1, new SubdivisionFilter(searchId) };
			
			String lot = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			String block = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			
			boolean useNameForLookUp = false;
			if(!addAoLookUpSearches(serverInfo, modules, allAoRef, searchId, isUpdate())
					&& !(StringUtils.isEmpty(lot) && StringUtils.isEmpty(block)) ){
				addNameSearch(  modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersO1  );
				useNameForLookUp = true;
				SearchLogger.info( "<font color='red'><b> No valid transaction History Detected and Not enough info for Subdivision Search. We must perform Name Look Up.</b></font></br>", searchId );
			} 
			
			addIteratorModule(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId, isUpdate(), useNameForLookUp, false);
			
			ArrayList<NameI> searchedNames = addNameSearch(  modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersO  );
			
			addOCRSearch( modules, serverInfo, legalFilter );
			
			addNameSearch( modules, serverInfo,SearchAttributes.OWNER_OBJECT, searchedNames==null?new ArrayList<NameI>():searchedNames, filtersO );
			
			addIteratorModule(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId, isUpdate(), useNameForLookUp, true);
			
			InstrumentGenericIterator instrumentBPIterator = getInstrumentIterator();
			instrumentBPIterator.enableBookPage();
			instrumentBPIterator.setLoadFromRoLike(true);
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
			module.clearSaKeys();
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			module.addIterator(instrumentBPIterator);
			if (isUpdate()) {
				module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
			}
			module.addFilter( legalFilter );
			modules.add(module);
			
			
			InstrumentGenericIterator instrumentNoIterator = getInstrumentIterator();
			instrumentNoIterator.enableInstrumentNumber();
			instrumentNoIterator.setLoadFromRoLike(true);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
			module.clearSaKeys();
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			module.addIterator(instrumentNoIterator);
			if (isUpdate()) {
				module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
			}
			module.addFilter( legalFilter );
			modules.add(module);
			
			//plat search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_PLAT_BOOK_PAGE);
			module.setSaObjKey(SearchAttributes.LD_BOOKNO);
			module.clearSaKeys();
			module.addFilter(DoctypeFilterFactory.getDoctypeFilter(searchId, 0.8, 
					new String[] {DocumentTypes.PLAT}, 
					FilterResponse.STRATEGY_TYPE_HIGH_PASS));
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, useNameForLookUp, false, getDataSite());
			module.addIterator(it);
			if (!isUpdate()) {
				modules.add(module);
			}
			
			//plat search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_PLAT_DOC);
			module.setSaObjKey(SearchAttributes.LD_BOOKNO);
			module.clearSaKeys();
			module.addFilter(DoctypeFilterFactory.getDoctypeFilter(searchId, 0.8, 
					new String[] {DocumentTypes.PLAT}, 
					FilterResponse.STRATEGY_TYPE_HIGH_PASS));
			it = new LegalDescriptionIterator(searchId, useNameForLookUp, false, getDataSite());
			module.addIterator(it);
			if (!isUpdate()) {
				modules.add(module);
			}
			
			addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, 
					new ArrayList<NameI>(), 
					nameFilterBuyer, 
					DoctypeFilterFactory.getDoctypeFilterForGeneralIndexBuyerNameSearch( searchId ) );
			
			String[] relatedSourceDoctype = new String[]{DocumentTypes.MORTGAGE, DocumentTypes.LIEN, DocumentTypes.CCER};
			/*
			//keep it for 2011
			String[] relatedFilterDoctype = new String[]{
					DocumentTypes.RELEASE,
					DocumentTypes.ASSIGNMENT, 
					DocumentTypes.ASSUMPTION, 
					DocumentTypes.MODIFICATION, 
					DocumentTypes.SUBORDINATION, 
					DocumentTypes.SUBSTITUTION,
					DocumentTypes.CCER};
			*/
			
			InstrumentGenericIterator instrumentRelatedBPIterator = getInstrumentIterator();
			instrumentRelatedBPIterator.enableBookPage();
			instrumentRelatedBPIterator.setLoadFromRoLike(true);
			instrumentRelatedBPIterator.setRoDoctypesToLoad(relatedSourceDoctype);
			instrumentRelatedBPIterator.setDoNotCheckIfItExists(true);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Searching with book/page list from all " + Arrays.toString(relatedSourceDoctype));
			module.clearSaKeys();
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
			module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
			module.addIterator(instrumentRelatedBPIterator);
			if (isUpdate()) {
				module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
			}
			//keep it for 2011
			//module.addFilter(DoctypeFilterFactory.getDoctypeFilter(searchId, 0.8, relatedFilterDoctype,	FilterResponse.STRATEGY_TYPE_HIGH_PASS));
			module.addFilter(legalFilter);
			module.addFilter(propertyTypeFilter);
			modules.add(module);
			
			
			InstrumentGenericIterator instrumentRelatedNoIterator = getInstrumentIterator();
			instrumentRelatedNoIterator.enableInstrumentNumber();
			instrumentRelatedNoIterator.setLoadFromRoLike(true);
			instrumentRelatedNoIterator.setRoDoctypesToLoad(relatedSourceDoctype);
			instrumentRelatedNoIterator.setDoNotCheckIfItExists(true);
			instrumentRelatedNoIterator.setInitAgain(true);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Searching with Instrument Number list from all " + Arrays.toString(relatedSourceDoctype));
			module.clearSaKeys();
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
			module.addIterator(instrumentRelatedNoIterator);
			if (isUpdate()) {
				module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
			}
			//keep it for 2011
			//module.addFilter(DoctypeFilterFactory.getDoctypeFilter(searchId, 0.8, relatedFilterDoctype, FilterResponse.STRATEGY_TYPE_HIGH_PASS));
			module.addFilter(legalFilter);
			module.addFilter(propertyTypeFilter);
			modules.add(module);
			
		}
		prepareAPN(searchId);
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	protected GenericMultipleLegalFilter getGenericMultipleLegalFilter() {
		GenericMultipleLegalFilter legalFilter = new GenericMultipleLegalFilter(searchId);
		HashSet<String> ignoreLotWhenServerDoctypeIs = new HashSet<String>();
		ignoreLotWhenServerDoctypeIs.add("TUBCARD");
		legalFilter.setIgnoreLotWhenServerDoctypeIs(ignoreLotWhenServerDoctypeIs);
		return legalFilter;
	}

	protected InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator instrumentBPIterator = new InstrumentGenericIterator(searchId);
		return instrumentBPIterator;
	}
	
	public static String prepareAPN( long searchId ){
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
		
		String apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNONDB ).replaceAll("[.-]", "");
		if(StringUtils.isEmpty(apn)){
			apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNO ).replaceAll("[.-]", "");
		}
		
		search.getSa().setAtribute(SearchAttributes.LD_PARCELNO3,apn);
		return apn;
	}

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void specificParseLegalDescription(Document doc, ParsedResponse item, ResultMap resultMap) {
		
		String remarks = XmlUtils.findNodeValue(doc, "/TitleDocument/LegalDescription/Remarks");
		
		Vector pisVector = (Vector) item.infVectorSets.get("PropertyIdentificationSet");
		if (pisVector == null) {
        	pisVector = new Vector();
        	item.infVectorSets.put("PropertyIdentificationSet", pisVector);
        }
		
		PropertyIdentificationSet pis = null;
		
		if(pisVector.size()==0||pisVector.size()>1){
			pis = new PropertyIdentificationSet();
			pisVector.add(pis);
		}else{
			pis = (PropertyIdentificationSet)pisVector.get(0);
		}
		
		parseLegal(remarks, pis);
		
	}
	
	
	private static PropertyIdentificationSet parseLegal(String rawLegal, PropertyIdentificationSet pis) {
		String lot = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "(?i)Lots?\\s*([-\\w]+)");
		
		if(StringUtils.isEmpty(pis.getAtribute("SubdivisionLotNumber"))){
			if(StringUtils.isNotEmpty(lot)) {
				pis.setAtribute("SubdivisionLotNumber", Roman.normalizeRomanNumbers(lot));
			}else{
				lot = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "(?i)\\bLT\\s+([-\\w]+)");
				if(StringUtils.isNotEmpty(lot)) {
						pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), Roman.normalizeRomanNumbers(lot));
						rawLegal = rawLegal.replaceAll("(?i)\\bLT\\s+([-\\w]+)", "");
				}
			}
		}
		
		if(StringUtils.isEmpty(pis.getAtribute("SubdivisionBlock"))){
			String block = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "(?i)Block\\s*([-\\w]+)");
			if(StringUtils.isNotEmpty(block)) {
				pis.setAtribute("SubdivisionBlock", block);
			}else{
				block = ro.cst.tsearch.utils.StringUtils.extractParameter(rawLegal, "(?i)\\bBLK\\s+\\s*([-\\w]+)");
				if(StringUtils.isNotEmpty(block)) {
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), block);
					rawLegal = rawLegal.replaceAll("(?i)\\bBLK\\s+\\s*([-\\w]+)", "");
				}
			}
		}
		
		return pis;
	}
	
	
	@Override
	public void specificParseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap) {
		defaultParseGrantorGrantee(doc, item, resultMap);
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		TSServerInfoModule imgModule = getDefaultServerInfo().getModule(TSServerInfo.IMG_MODULE_IDX);
		imgModule.forceValue(0, org.apache.commons.lang.StringUtils.defaultString(document.getBook()));
		imgModule.forceValue(1, org.apache.commons.lang.StringUtils.defaultString(document.getPage()));
		imgModule.forceValue(2, 
				org.apache.commons.lang.StringUtils.defaultIfEmpty(
						document.getDocumentNumber(), 
						document.getInstrumentNumber()));
		imgModule.forceValue(3, "true");
		
		
		return imgModule;
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(2, "BOOKPAGE");
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getDocumentNumber());
			module.forceValue(1, "DOCUMENT");
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
			module.forceValue(1, "DOCUMENT");
		}
		return module;
	}
	
	@Override
    protected String CreateSaveToTSDFormHeader(int action, String method) {
    	String s = "<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"" + msRequestSolverName + "\"" + " method=\"" + method + "\" > "
                + "<input type=\"hidden\" name=\"dispatcher\" value=\""+ action + "\">"
                + "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\">" 
                + "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + mSearch.getSearchID() + "\"> "
                + "<input type=\"hidden\" name=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" id=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" " +
                	"value=\"" + RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITH_CROSSREF + "\">";
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
        	s = "<input  type=\"checkbox\" checked title=\"Save selected document(s) with cross-references\" " +
        		" onclick=\"javascript: if(document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "'))\r\n " +
        		" if(this.checked) { " +
	        	" document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "').value='" + 
	        			RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITH_CROSSREF +
	        	"' } else { " +
 	        	" document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "').value='" + 
 	        			RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF +
	        	"' } \"> Save with cross-references<br>\r\n" +
	        	"<input type=\"checkbox\" name=\"" + RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS + 
	        			"\" id=\"" + RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS + 
	        			"\" title=\"Save search parameters from selected document(s) for further use\" > Save with search parameters<br>\r\n" + 
        		"<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" " +"onclick=\"javascript:submitForm();\" >\r\n";
    	}
        
        
        return s+"</form>\n";
	}
	

	@Override
	public void specificParseAddress(Document doc, ParsedResponse item,
			ResultMap resultMap) {
	}
	
	
	@Override
	protected String modifyXMLResponse(String xml,int moduleIDX) {
		xml = super.modifyXMLResponse(xml, moduleIDX);
		if(xml.contains("BASE_EDIT")){
			
			int pos1 = xml.indexOf("<Stewart.REI.Request>");
			int pos = xml.indexOf("</Stewart.REI.Request>");
			if(pos>0 && pos1>0 && pos1<pos){
				xml = xml.substring(0,pos1)+xml.substring(pos+"</Stewart.REI.Request>".length());
			}
			xml = xml.replaceAll("(?i)[<]TitleDocument[>]", "");
			xml = xml.replaceAll("(?i)[<][/]TitleDocument[>]", "");
			xml = xml.replaceAll("(?i)[<][/]Instrument[>]", "");
			xml = xml.replaceAll("(?i)[<]Instrument[>]", "");
			xml = xml.replaceAll("(?i)[<]LegalDescription[>]", "<TitleDocument><BaseEdit/><LegalDescription>");
			xml = xml.replaceAll("(?i)[<][/]LegalDescription[>]", "</LegalDescription><Instrument></Instrument></TitleDocument>");
		} else  if(xml.contains("<SearchType>SPEEDLIST</SearchType>")) {
			xml = xml.replaceFirst("(?i)<LegalDescription>", "<LegalDescription><Instrument></Instrument>");
		}
		
		return xml;
	}
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo,
			DocumentI documentToCheck, HashMap<String, String> data) {
		
		boolean firstTry = super.isInstrumentSaved(instrumentNo, documentToCheck, data);
		
		if(firstTry) {
			return true;
		}
		
		if(documentToCheck == null) {
			return false;
		}
		DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		InstrumentI instToCheck = documentToCheck.getInstrument();
    		for(DocumentI e: documentManager.getDocumentsWithDataSource(false, "TS")){
    			InstrumentI savedInst = e.getInstrument();
    			if( savedInst.getInstno().equals(instToCheck.getInstno())  
    					&& (savedInst.getBook().equals(instToCheck.getBook()) && savedInst.getPage().equals(instToCheck.getPage()))  
    					&& savedInst.getDocno().equals(instToCheck.getDocno())
    					&& e.getServerDocType().equals(documentToCheck.getServerDocType())
    					&& savedInst.getYear() == instToCheck.getYear()
    			){
    				return true;
    			}
    		}
    	} finally {
    		documentManager.releaseAccess();
    	}
		return false;
	}
	
	@Override
	public void performAdditionalProcessingAfterRunningAutomatic() {
		super.performAdditionalProcessingAfterRunningAutomatic();
		
		Search search = getSearch();
		
		if (isUpdate()){
			SearchAttributes sa = search.getSa();
			
			if (!sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
				Set<InstrumentI> allAoRef = getAllAoReferences(search);
				
				if (allAoRef.size() > 0){
					
					SearchLogger.info("\n</div><hr/><div><BR>Run additional searches to get Certification Date. <BR></div>\n", searchId);
					TSServerInfo serverInfo = getCurrentClassServerInfo();
					for(InstrumentI inst : allAoRef){
						try {
							if (inst.hasBookPage()){
								String originalB = inst.getBook();
								String originalP = inst.getPage();
								
								String book = originalB.replaceFirst("^0+", "");
								String page = originalP.replaceFirst("^0+", "");
								
								TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
								module.setData(0, book);
								module.setData(1, page);
								module.setData(2, "BOOKPAGE");
									
								ServerResponse response = SearchBy(module, null);
								
								if (sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
									SearchLogger.info("\n</div><div><BR>Certification Date found!<BR><hr/></div>\n", searchId);
									break;
								}
								
							} else if (inst.hasDocNo()){
								TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
								String  instNo = inst.getDocno().replaceFirst("^0+", "");
								instNo = appendBeginingZero(6-instNo.length(),instNo);
								
								if(inst.hasYear()){
									instNo = inst.getYear() + instNo;
								}
								
								instNo  = instNo .replaceFirst("^0+", "");
								module.setData(0, instNo);
								module.setData(2, "DOCUMENT");
								
								ServerResponse response = SearchBy(module, null);
								
								if (sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
									SearchLogger.info("\n</div><div><BR>Certification Date found!<BR><hr/></div>\n", searchId);
									break;
								}
		
							} else if (inst.hasInstrNo()){						
								
								String  instNo = prepareInstrumentNoForCounty(inst);
								
								TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
								
								module.setData(0,  instNo);
								module.setData(1, "DOCUMENT");
								
								ServerResponse response = SearchBy(module, null);
								
								if (sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
									SearchLogger.info("\n</div><div><BR>Certification Date found!<BR><hr/></div>\n", searchId);
									break;
								}
							}
						}catch(Exception e) {
							e.printStackTrace();
						}
					}
					if (!sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
						SearchLogger.info("\n</div><div><BR>Certification Date still not found!<BR><hr/></div>\n", searchId);
					}
				}
			}
		}
	}
	
	public static boolean retrieveImage(String book, String page, String docNo, String fileName, Search mSearch, String msSiteRealPath, boolean justImageLookUp){
		String stateAbv = InstanceManager.getManager().getCurrentInstance(mSearch.getID()).getCurrentState().getStateAbv();
		String county = InstanceManager.getManager().getCurrentInstance(mSearch.getID()).getCurrentCounty().getName();
    	//do not retrieve the image twice
    	if(FileUtils.existPath(fileName)){
    		return true;
    	}
    	 
	 	TSInterface server = TSServersFactory.GetServerInstance((int)TSServersFactory.getSiteId(stateAbv, county, "TS"), "", "", mSearch.getID());
		
		TSServerInfoModule module = server.getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.IMG_MODULE_IDX, new SearchDataWrapper());
	
		ServerResponse res = null;
		
		server.setServerForTsd(mSearch, msSiteRealPath);
		
		if(!StringUtils.isEmpty(docNo)){
			module.forceValue(2, docNo);
			try{
				res = ((TSServerDASL)server).searchBy(module, new ImageLinkInPage(justImageLookUp),null);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		if(res!=null){
			DownloadImageResult dres = res.getImageResult();
			if(dres .getStatus() == DownloadImageResult.Status.OK){
				FileUtils.writeByteArrayToFile(dres.getImageContent(), fileName);
				return true;
			}
		}
		
		module = server.getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.IMG_MODULE_IDX, new SearchDataWrapper());
		
		server.setServerForTsd(mSearch, msSiteRealPath);
		if(!(StringUtils.isEmpty(book) || StringUtils.isEmpty(page))){
			module.setData( 0, book );
			module.setData( 1, page );
			try{
				res = ((TSServerDASL)server).searchBy(module, new ImageLinkInPage(justImageLookUp),null);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		if(res!=null){
			DownloadImageResult dres = res.getImageResult();
			if(dres .getStatus() == DownloadImageResult.Status.OK){
				FileUtils.writeByteArrayToFile(dres.getImageContent(), fileName);
				return true;
			}
		}
		
		
		
		return false;
    }
}

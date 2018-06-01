package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.propertyInformation.Family;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericMultipleAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeAdvancedFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.SubdivisionFilter;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author cristi stochina
 */
public class GenericDaslAcs extends TSServerDASLAdapter{

	protected static final String DASL_TEMPLATE_CODE = "DASL_ACS";
	private static final ServerPersonalData pers = new ServerPersonalData();
	
	private static final String ACS_FAKE_RESPONSE = StringUtils.fileReadToString(FAKE_FOLDER+"DASLFakeResponse.xml");
	
	private static Set<String> usePinFromNDB = new HashSet<String>();
	static {
		usePinFromNDB.add( "TXCollinAC" );
	}
	
	static {
		pers.setTemplatePrefixName(DASL_TEMPLATE_CODE);
		pers.addXPath(0, "//TitleDocument", ID_DETAILS);
	}
	
	@Override
	@SuppressWarnings("deprecation")
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
		int functionBook = 0;
		int functionPage = 1;
		int functionDocNo = 2;
		int functionYear = 4;
		int functionMonth = 5;
		int functionDay = 6;
		int functionDASLImageSearchType = 3;
		
		TSServerInfo info = getDefaultServerInfo();
		TSServerInfoModule module = info.getModule(TSServerInfo.IMG_MODULE_IDX);
		
		try{
			
			String link = image.getLink();
			HashMap<String, String> map = HttpUtils.getParamsFromLink( link );
			
			String book 	 = map.get( "book" ) ;
			String page 	 = map.get( "page" ) ;
			String docNumber = map.get( "docno");
			String year 	 = map.get( "year" ) ;
			String month     = map.get( "month" ) ;
			String day     = map.get( "day" ) ;
			
			module.getFunction(functionBook).setParamValue(book==null?"":book);
			module.getFunction(functionPage).setParamValue(page==null?"":page);
			module.getFunction(functionDocNo).setParamValue(docNumber==null?"":docNumber);
			module.getFunction(functionYear).setParamValue(year==null?"":year);
			module.getFunction(functionMonth).setParamValue(month==null?"":month);
			module.getFunction(functionDay).setParamValue(day==null?"":day);
			module.getFunction(functionDASLImageSearchType).setParamValue("true");
			
			String imageName = image.getPath();
	    	if(FileUtils.existPath(imageName)){
	    		byte b[] = FileUtils.readBinaryFile(imageName);
	    		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
	    	}
			
			return searchBy(module, image, null).getImageResult();
		}
		catch(Exception e){
			return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected ServerResponse searchBy(TSServerInfoModule module, Object sd, String fakeResponse) throws ServerResponseException {
		
		int moduleIdx = module.getModuleIdx() ;
		
		switch (moduleIdx) {
			case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
				TSServerInfoModule mod = getDefaultServerInfo().getModule(TSServerInfo.IMG_MODULE_IDX);
				String book = module.getParamValue(0);
				String page = module.getParamValue(1);
				mod.forceValue( 0,  book);
				mod.forceValue( 1,  page);
				mod.forceValue( 3, "true" );
				ImageLinkInPage im = new ImageLinkInPage(true);
				ServerResponse sr = super.searchBy(mod, im, fakeResponse);
				DownloadImageResult imageResult = sr.getImageResult();
				
				if(imageResult!=null && imageResult.getStatus() == DownloadImageResult.Status.OK){
					TSServerInfoModule modFake =  getDefaultServerInfo().getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
					
					String doc = ACS_FAKE_RESPONSE;
					
					String grantor = "";
					String grantee = "";
					String docno = "";
					String date = "01/01/1960";
					String serverDoctype = "MISCELLANEOUS";
					
					RestoreDocumentDataI restoreDocumentDataI = (RestoreDocumentDataI)module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE);
					if(restoreDocumentDataI != null) {
						grantor = restoreDocumentDataI.getGrantor();
						grantee = restoreDocumentDataI.getGrantee();
						docno = restoreDocumentDataI.getInstrumentNumber();
						date = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(restoreDocumentDataI.getRecordedDate());
						serverDoctype = restoreDocumentDataI.getDoctypeForSearch();
					}
					
					doc = doc.replaceAll("@@Grantor@@", grantor);
					doc = doc.replaceAll("@@Grantee@@", grantee);
					doc = doc.replaceAll("@@Book@@", book==null?"":book);
					doc = doc.replaceAll("@@Page@@", page==null?"":page);
					doc = doc.replaceAll("@@DocNo@@", docno);
					doc = doc.replaceAll("@@Date@@", date);
					doc = doc.replaceAll("@@Type@@-@@Type@@", serverDoctype);
					
					return super.searchBy(modFake, sd, doc);
				}else{
					ParsedResponse pr = new ParsedResponse();
					sr = new ServerResponse();
					sr.setParsedResponse(pr);
					sr.setResult("<b>&nbsp;&nbsp; Image not found</b>");
					pr.setResponse("<b>Image not found</b>");
					solveHtmlResponse(mod.getModuleIdx()+"", mod.getParserID(), "SearchBy", sr, sr.getResult());
					return sr;
				}
			
			case TSServerInfo.INSTR_NO_MODULE_IDX:
				mod = getDefaultServerInfo().getModule(TSServerInfo.IMG_MODULE_IDX);
				String docNo = module.getParamValue(0);
				String year = module.getParamValue(2);
				String month = module.getParamValue(3);
				String day = module.getParamValue(4);
				
				mod.forceValue( 2,  docNo);
				mod.forceValue( 3,  "true");
				mod.forceValue( 4,  year);
				mod.forceValue( 5,  month);
				mod.forceValue( 6,  day);
				
				im = new ImageLinkInPage(true);
				sr = super.searchBy(mod, im, fakeResponse);
				imageResult = sr.getImageResult();
				
				if(imageResult!=null && imageResult.getStatus() == DownloadImageResult.Status.OK){
					TSServerInfoModule modFake =  getDefaultServerInfo().getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
					
					String doc = ACS_FAKE_RESPONSE;
					
					String grantor = "";
					String grantee = "";
					String bookToAdd = "";
					String pageToAdd = "";
					String serverDoctype = "MISCELLANEOUS";
					
					RestoreDocumentDataI restoreDocumentDataI = (RestoreDocumentDataI)module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE);
					if(restoreDocumentDataI != null) {
						grantor = restoreDocumentDataI.getGrantor();
						grantee = restoreDocumentDataI.getGrantee();
						bookToAdd = restoreDocumentDataI.getBook();
						pageToAdd = restoreDocumentDataI.getPage();
						serverDoctype = restoreDocumentDataI.getDoctypeForSearch();
					}
					
					
					
					doc = doc.replaceAll("@@Grantor@@", grantor);
					doc = doc.replaceAll("@@Grantee@@", grantee);
					doc = doc.replaceAll("@@Book@@", bookToAdd);
					doc = doc.replaceAll("@@Page@@", pageToAdd);
					doc = doc.replaceAll("@@DocNo@@", docNo);
					doc = doc.replaceAll("@@Date@@", month+"/"+day+"/"+year);
					doc = doc.replaceAll("@@Type@@-@@Type@@", serverDoctype);
					
					return super.searchBy(modFake, sd, doc);
				}else{
					ParsedResponse pr = new ParsedResponse();
					sr.setParsedResponse(pr);
					sr.setResult("<b>&nbsp;&nbsp; Image not found</b>");
					pr.setResponse("<b>Image not found</b>");
					solveHtmlResponse(mod.getModuleIdx()+"", mod.getParserID(), "SearchBy", sr, sr.getResult());
					return sr;
				}
			
			default:
			break;		
		}
		
		return super.searchBy(module, sd, fakeResponse);
	}
	
	@Override
	protected String createLinkForImage(HashMap<String, String> value) {
		HashMap<String, String> map = (HashMap<String, String>) value;
		String book = map.get("book");
		String page = map.get("page");
		String docno = map.get("docno");
		String type = map.get("type");
		String year = map.get("year");
		String month = map.get("month");
		String day = map.get("day");
		
		if (type == null) {
			type = "";
		} else {
			type = DocumentTypes.getDocumentCategory(type, searchId);
		}

		StringBuilder build = new StringBuilder("");
		build.append(createPartialLink(TSConnectionURL.idDASL, TSServerInfo.IMG_MODULE_IDX));
		build.append("DASLIMAGE&");

		build.append("type");
		build.append("=");
		build.append(type);
		build.append("&");

		build.append("docno");
		build.append("=");
		build.append(docno);
		build.append("&");

		build.append("book");
		build.append("=");
		build.append(book);
		build.append("&");

		build.append("DASLImageSearchType");
		build.append("=");
		build.append("DT");
		build.append("&");

		build.append("page");
		build.append("=");
		build.append(page);
		build.append("&");
		
		build.append("day");
		build.append("=");
		build.append(day);
		build.append("&");
		
		build.append("month");
		build.append("=");
		build.append(month);
		build.append("&");
		
		build.append("year");
		build.append("=");
		build.append(year);
		
		return build.toString();
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

		templateParams.put(AddDocsTemplates.DASLCountyFIPS, dat.getCountyFIPS() );

		templateParams.put(AddDocsTemplates.DASLStateFIPS, dat.getStateFIPS() );

		templateParams.put(AddDocsTemplates.DASLYearFiled,  params.get("year") );
		
		templateParams.put(AddDocsTemplates.DASLMonthFiled,  params.get("month") );
		
		templateParams.put(AddDocsTemplates.DASLDayFiled,  params.get("day") );
		
		templateParams.put(AddDocsTemplates.DASLRealPartySearchType1,  params.get("partySearchType1") );
		
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
		
		templateParams.put(AddDocsTemplates.DASLClientId, SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericACSite", "DaslClienId"));
		
		return templateParams;
	}
	
	@Override
	protected ServerPersonalData getServerPersonalData() {
		return pers;
	}
	
	public GenericDaslAcs(long searchId) {
		super(searchId);
		mSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
	}
	
	public GenericDaslAcs(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		mSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
	}
	
	
	private static final long serialVersionUID = -3329209176659976717L;

	@Override
	public void specificParseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap) {
		super.defaultParseGrantorGrantee(doc, item, resultMap);
	}

	private static final Pattern PAT_REMARKS = Pattern.compile("([0-9]+)\\s+([0-9]+)");
	@Override
	public void specificParseLegalDescription(Document doc, ParsedResponse item, ResultMap resultMap) {
		super.defaultParseLegalDescription(doc, item, resultMap);
		
		Vector pisVector = (Vector) item.infVectorSets.get("PropertyIdentificationSet");
		if (pisVector == null) {
        	pisVector = new Vector();
        	item.infVectorSets.put("PropertyIdentificationSet", pisVector);
        }
		
		
		/* -- APN parsing -- */
		String apn = XmlUtils.findNodeValue(doc, "/TitleDocument/LegalDescription/APN");
		if(StringUtils.isNotEmpty(apn)){
			if(pisVector.size()==1){
				PropertyIdentificationSet pis =  (PropertyIdentificationSet)pisVector.get(0);
				pis.setAtribute("ParcelID", apn.trim());
			}else{
				PropertyIdentificationSet pis = new PropertyIdentificationSet();
				pis.setAtribute("ParcelID", apn.trim());
				pisVector.add(pis);
			}
		}
		/* -- End APN parsing -- */
		
		/* -- crossrefs parsing -- */
		String remarks = XmlUtils.findNodeValue(doc, "/TitleDocument/Instrument/Remarks");
		
		Vector<CrossRefSet> references = new Vector<CrossRefSet>();
		item.setCrossRefSet(references);
		
		Matcher mat = PAT_REMARKS.matcher(remarks);
		if(mat.find()){
			String book = mat.group(1);
			String page = mat.group(2);
			
			CrossRefSet set = new CrossRefSet();
			set.setAtribute("Book", book);
			set.setAtribute("Page", page);
			references.add(set);	
		}
		/* -- end crossrefs parsing -- */
	}
	
	public static String updateXMLResponse(String xml, int moduleIDX) {
		xml = xml.replaceAll(">([0]+)([0-9a-zA-Z]+)<[/]", ">$2</");
		xml = xml.replaceAll(">[0]*([0-9a-zA-Z]+)\\s+[0]*([0-9a-zA-Z]+)<[/]", ">$1 $2</");
		xml = xml.replaceAll(">([0]+)<[/]", "> </");		
		return xml;
	}
	
	@Override
	protected String modifyXMLResponse(String xml, int moduleIDX) {
		return updateXMLResponse(xml, moduleIDX);
	}

	@Override
	public void specificParseAddress(Document doc, ParsedResponse item, ResultMap resultMap) {
		super.defaultParseAddress(doc, item, resultMap);
	}
	
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		
		ConfigurableNameIterator nameIterator = null;
		Search search = getSearch();
		int searchType = search.getSearchType();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		FilterResponse doctypeFilter = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
		
		if(searchType == Search.GO_BACK_ONE_LEVEL_SEARCH) {
			SearchAttributes sa = search.getSa();	
			
		    TSServerInfoModule module;	
		    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
	
		    for (String id : gbm.getGbTransfers()) {
				  		   	    	 
		    	module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		    	module.setIndexInGB(id);
		    	module.setTypeSearchGB("grantor");
		    	module.clearSaKeys();
		    	module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		    	String date=gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
		    	if (date!=null) 
		    		module.getFunction(0).forceValue(date);
			    module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				module.addFilter( NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter( doctypeFilter );
		    	module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
				module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
				module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		    	nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;" } );
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
			 		module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			 		module.addFilter( NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			 		module.addFilter( doctypeFilter );
			 		module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
			 		module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
			 		module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
			 		nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;" } );
			 		module.addIterator(nameIterator);			
			 		modules.add(module);
			 	}
		    }	 
		}
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);
		
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		TSServerInfoModule module = null;
		SearchAttributes sa = getSearch().getSa();
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		FilterResponse nameFilterOwner 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, mSearch.getID(), null );
		nameFilterOwner.setInitAgain(true);
		
		FilterResponse nameFilterBuyer 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, mSearch.getID(), null );
		
		GenericMultipleAddressFilter addressFilter 	= new GenericMultipleAddressFilter(searchId);
		GenericMultipleLegalFilter legalFilter = new GenericMultipleLegalFilter(searchId);;
		legalFilter.setUseLegalFromSearchPage(true);
		
		LastTransferDateFilter lastTransferDateFilter 	= new LastTransferDateFilter(searchId);
		
		PinFilterResponse  pinFilter = PINFilterFactory.getPinFilter(searchId, SearchAttributes.LD_PARCELNO2, true, true);
		
		FilterResponse[] filtersO 	= { new AcsOwnerDocTypeFilter(searchId), legalFilter, addressFilter, lastTransferDateFilter, pinFilter, new SubdivisionFilter(searchId) };
		
		addPinSearch( prepareAPNPerCounty(searchId), modules, serverInfo, mSearch );
		
		/* -- search by address -- */
		String strNo = sa.getAtribute(SearchAttributes.P_STREETNO);
		String strName = sa.getAtribute(SearchAttributes.P_STREETNAME);		
		if( !StringUtils.isEmpty(strNo) && !StringUtils.isEmpty(strName) ){
			module = new TSServerInfoModule ( serverInfo.getModule( TSServerInfo.ADDRESS_MODULE_IDX ) );
			module.clearSaKeys();
			module.forceValue( 0, strNo );
			module.forceValue( 2, strName );
			module.addFilter( addressFilter );
			module.addFilter( legalFilter );
			module.addFilter( pinFilter );
			//module.addFilter( new SubdivisionFilter(searchId) );
			modules.add(module);				
		}
		
		String subdivName = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		String platBook = sa.getAtribute(SearchAttributes.LD_BOOKNO);
		String platPage = sa.getAtribute(SearchAttributes.LD_PAGENO);
		String allLots = sa.getAtribute(SearchAttributes.LD_LOTNO);
		allLots = (allLots==null?"":allLots).trim();
		
		String block = sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
		block = (block==null?"":block).trim();
		
		String unit = sa.getAtribute(SearchAttributes.P_STREETUNIT);
		
		if(allLots.length()==0 && StringUtils.isNotEmpty(unit)){
			allLots = unit;
			if(block.length()==0){
				block = sa.getAtribute(SearchAttributes.BUILDING);
			}
		}
		
		Vector<LotInterval> lots = LotMatchAlgorithm.prepareLotInterval(allLots);
		List<String> allAoAndTrlots = new ArrayList<String>();
		if(allLots!=null && allLots.trim().length()>0){
			for (Iterator<LotInterval> iterator = lots.iterator(); iterator.hasNext();) {
				allAoAndTrlots.addAll(((LotInterval) iterator.next()).getLotList());
			}
		}
		
		/* -- search by subdivided legal -- */
		if( StringUtils.isNotEmpty(platBook) && StringUtils.isNotEmpty(platPage) 
				&& ( allAoAndTrlots.size()>0 ||  StringUtils.isNotEmpty(block) ) ){
			for( String lot:allAoAndTrlots ){
				module = new TSServerInfoModule ( serverInfo.getModule( TSServerInfo.SUBDIVISION_MODULE_IDX ) );
				module.clearSaKeys();
				module.forceValue( 2, lot );
				module.forceValue( 3, block );
				module.forceValue( 4, platBook );
				module.forceValue( 5, platPage );
				module.forceValue( 7, "SUBDIVIDED" );
				
				module.addFilter( addressFilter );
				module.addFilter( legalFilter );
				module.addFilter( pinFilter );
			}
			modules.add( module );				
		}
		
		/* -- search by legal with subdivision name -- */
		if( StringUtils.isNotEmpty(subdivName) && ( allAoAndTrlots.size()>0 ||  StringUtils.isNotEmpty(block) ) ){
			
			String originalSubdivName = subdivName;
			subdivName = subdivName.replaceAll("#[0-9]+", "");
			if(!originalSubdivName.equals(subdivName)) {
				Set<String> allSubdivisionNames = new HashSet<String>();
				allSubdivisionNames.add(subdivName.trim());
				mSearch.setAdditionalInfo(AdditionalInfoKeys.SUBDIVISION_NAME_SET,
						allSubdivisionNames);
			}
			
			subdivName = subdivName.trim();
			
			for( String lot:allAoAndTrlots ){
				
				module = new TSServerInfoModule ( serverInfo.getModule( TSServerInfo.SUBDIVISION_MODULE_IDX ) );
				module.clearSaKeys();
				module.forceValue( 2, lot );
				module.forceValue( 3, block );
				module.forceValue( 7, "SUBDIVIDED_W_NAME" );
				module.forceValue( 8, subdivName );
				
				module.addFilter( addressFilter );
				module.addFilter( legalFilter );
				module.addFilter( pinFilter );
				module.addFilter( new SubdivisionFilter(searchId) );
				 
				modules.add(module);		
			}
		}
		
		ArrayList<NameI> searchedNames = addNameSearch(modules, serverInfo,SearchAttributes.OWNER_OBJECT, null, filtersO  );
		
		addOCRSearch( modules, serverInfo, legalFilter );
		
		addNameSearch( modules, serverInfo,SearchAttributes.OWNER_OBJECT, searchedNames==null?new ArrayList<NameI>():searchedNames, filtersO );
		
		addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, new ArrayList<NameI>(), nameFilterBuyer, DoctypeFilterFactory.getDoctypeBuyerFilter( searchId ) );
		
		serverInfo.setModulesForAutoSearch( modules );	
	}
	
	protected void addOCRSearch(List<TSServerInfoModule> modules,TSServerInfo serverInfo, FilterResponse ...filters){
		// OCR last transfer - book / page search
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
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
	
	protected  static  String prepareAPNPerCounty( long searchId ){
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
		String county = ci.getCurrentCounty().getName();
		
		String apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNONDB ).replaceAll("[.-]", "");
		if(StringUtils.isEmpty(apn)){
			apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNO ).replaceAll("[.-]", "");
		}
		
		
		if("collin".equalsIgnoreCase(county)){
			//R149800203101
			if(apn.length()>=13){
				apn = apn.substring(0,1)+"-"+apn.substring(1,5)+"-"+apn.substring(5,8)+"-"+apn.substring(8,12)+"-"+apn.substring(12);
			}
		}else if("kaufman".equalsIgnoreCase(county)){
			apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNO2 ).trim();
			if(!apn.toLowerCase().startsWith("r") && apn.length()>0){
				apn = "R" + apn.trim();
			}
		}
		
		search.getSa().setAtribute(SearchAttributes.LD_PARCELNO2, apn);
		return apn;
	}
	
	private void addPinSearch(String pin,List<TSServerInfoModule> modules,TSServerInfo serverInfo,Search mSearch){
		if( !StringUtils.isEmpty(pin) ){
			TSServerInfoModule module = new TSServerInfoModule ( serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX) ) ;
			module.clearSaKeys();
			module.forceValue( 1, pin );  
			addBetweenDateTest(module, false, false, false);
			modules.add(module);	
		}
	}
	
	protected ArrayList<NameI>  addNameSearch( List<TSServerInfoModule> modules, TSServerInfo serverInfo,String key, ArrayList<NameI> searchedNames, FilterResponse ...filters ) {
		Search global 		= getSearch();
		SearchAttributes sa = global.getSa();
		ConfigurableNameIterator nameIterator = null;
		
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
		
		module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
		module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
		nameIterator.setAllowMcnPersons( true );
		
		if ( searchedNames!=null ) {
			nameIterator.setInitAgain( true );
			nameIterator.setSearchedNames( searchedNames );
		}
		
		searchedNames = nameIterator.getSearchedNames() ;
		module.addIterator( nameIterator );
		
		modules.add( module );
		return searchedNames;
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
	
	protected int[] getModuleIdsForSavingLegal() {
		return new int[]{TSServerInfo.SUBDIVISION_MODULE_IDX};
	}
	protected int[] getModuleIdsForSavingAddress() {
		return new int[]{TSServerInfo.ADDRESS_MODULE_IDX};
	}
	
	@Override
	public List<TSServerInfoModule> getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();
		Date recordedDate = restoreDocumentDataI.getRecordedDate();
		TSServerInfoModule module = null;
		
		
		if(StringUtils.isNotEmpty(instrumentNumber) && recordedDate != null) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE, restoreDocumentDataI);
			module.forceValue(0, instrumentNumber);
			module.forceValue(1, "DOCUMENT");
			Calendar cal = Calendar.getInstance();
			cal.setTime(recordedDate);
			module.forceValue(2, Integer.toString(cal.get(Calendar.YEAR)));
			module.forceValue(3, Integer.toString(cal.get(Calendar.MONTH) + 1));
			module.forceValue(4, Integer.toString(cal.get(Calendar.DATE)));
			modules.add(module);
		} 
		if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_RESTORE_DOCUMENT_SOURCE, restoreDocumentDataI);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(2, "BOOKPAGE");
			modules.add(module);
		} 
		return modules;
	}
	
	
	private static class AcsOwnerDocTypeFilter extends DocTypeAdvancedFilter{

		private static final long serialVersionUID = -4147637844131052993L;
		
		public AcsOwnerDocTypeFilter(long searchId) {
			super(searchId);
		}
		
		@Override
		public BigDecimal getScoreOneRow(ParsedResponse row) {
			BigDecimal score =  super.getScoreOneRow(row);
			
			if( score.compareTo(threshold)<0 ){
				try{
					m.getAccess();
					List<RegisterDocumentI> allDocs = m.getRoLikeDocumentList();
					DocumentI doc  = row.getDocument();
					if(doc instanceof RegisterDocumentI){
						RegisterDocumentI regDoc = (RegisterDocumentI)doc;
						if("RELEASE".equalsIgnoreCase(regDoc.getDocType())){ 
							Set<InstrumentI> references = regDoc.getParsedReferences();
							for(InstrumentI ref:references){
								for(RegisterDocumentI cur:allDocs){
									if(cur.hasYear() && !cur.getInstno().isEmpty() 
											&& ( ( cur.getInstno().equals(ref.getBook()) && ref.getPage().equalsIgnoreCase(String.valueOf(cur.getYear())) )
												  /*||
												  (cur.getInstno().equals(ref.getPage()) && ref.getBook().equalsIgnoreCase(String.valueOf(cur.getYear())))*/
											    )	
									) {
										return BigDecimal.ONE;
									}
								}
							}
						}
					}
					
				}finally{
					m.releaseAccess();
				}	
			}
			return score;
		}
		
	}
	
}

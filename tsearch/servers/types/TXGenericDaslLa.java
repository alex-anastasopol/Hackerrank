package ro.cst.tsearch.servers.types;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface.DaslResponse;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIteratorI;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;
import ro.cst.tsearch.utils.gargoylesoftware.HtmlElementHelper;

import com.gargoylesoftware.htmlunit.Page;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

/**
 * @author Cristian Stochina
 */

public class TXGenericDaslLa extends TSServerDASLAdapter implements TSServerROLikeI{

	private static final long serialVersionUID = -5192143125290670794L;

	protected static final String DASL_TEMPLATE_CODE = "DASL_LA_TX";

	private static final String LA_FAKE_RESPONSE = StringUtils.fileReadToString(FAKE_FOLDER+"DASLFakeResponse.xml");
	
	private static final Pattern PATT_MULTIPLE_IMAGES_NAME = Pattern.compile("(?i)<ProviderImageId>([0-9]+_[0-9]+_[01]_[0-9]+)</ProviderImageId>[ \n\r\t]*<InstrumentType>[^<]*PLAT[^<]*</InstrumentType>");
	
	private static final Pattern SECCOND_PATT_MULTIPLE_IMAGES_NAME = Pattern.compile("(?i)<ProviderImageId>([0-9]+_[0-9]+_[01]_[0-9]+)</ProviderImageId>");
	
	private static final ServerPersonalData pers = new ServerPersonalData();
	
	static {
		pers.setTemplatePrefixName(DASL_TEMPLATE_CODE);
		{
			int id = 0;
			pers.addXPath(id, "//TitleDocument", ID_DETAILS);
			pers.addXPath(id, "//TitleDocument", ID_SEARCH_BY_BOOK_AND_PAGE);
			pers.addXPath(id, "//TitleDocument", ID_SEARCH_BY_INSTRUMENT_NO);
		}
	}

	public TXGenericDaslLa(long searchId) {
		super(searchId);
	}

	public TXGenericDaslLa(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	@Override
	protected ServerPersonalData getServerPersonalData() {
		return pers;
	}

	protected void ParseResponse(String sAction, ServerResponse response,
			int viParseID) throws ServerResponseException {
		super.ParseResponse(sAction, response, viParseID);
	}

	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image)
			throws ServerResponseException {
		int functionBook = 0;
		int functionPage = 1;
		int functionDocNo = 2;
		int functionImageSearch = 3;
		int fuctionYear = 4;
		int functionIsPlat = 6;
		
		TSServerInfo info = getDefaultServerInfo();
		TSServerInfoModule module = info.getModule(TSServerInfo.IMG_MODULE_IDX);
		String bookAlias = module.getFunction(functionBook/* Book */).getParamAlias();
		String pageAlias = module.getFunction(functionPage/* Page */).getParamAlias();
		String docNumberAlias = module.getFunction(functionDocNo/* DocNumber */).getParamAlias();
		String yearAlias = module.getFunction(fuctionYear/* Year */).getParamAlias();
		String typeAlias = module.getFunction(functionImageSearch/* Year */).getParamAlias();
		typeAlias = typeAlias==null?"":typeAlias;
		String book = "";
		String page = "";
		String docNumber = "";
		String year = "";
		String type = "";
		
		String link = image.getLink();
		int poz = link.indexOf("?");

		if (poz > 0) {
			link = link.substring(poz + 1);
		}

		String[] allParameters = link.split("[&=]");

		for (int i = 0; i < allParameters.length - 1; i += 2) {
			if (bookAlias.equalsIgnoreCase(allParameters[i])) {
				book = allParameters[i + 1];
			} else if (pageAlias.equalsIgnoreCase(allParameters[i])) {
				page = allParameters[i + 1];
			} else if (docNumberAlias.equalsIgnoreCase(allParameters[i])) {
				docNumber = allParameters[i + 1];
			} else if (yearAlias.equalsIgnoreCase(allParameters[i])) {
				year = allParameters[i + 1];
			}else if(typeAlias.equalsIgnoreCase(allParameters[i])){
				type = allParameters[i + 1];
			}
		}

		boolean isPlat = "PLAT".equalsIgnoreCase(type);
		module.setParamValue(functionBook,book);
		module.setParamValue(functionPage,page);
		module.setParamValue(functionDocNo,docNumber);
		module.setParamValue(functionImageSearch,"true");
		module.setParamValue(fuctionYear,year);
		if(isPlat){
			module.forceValue(fuctionYear,"");
			module.forceValue(functionIsPlat, "Yes");
		}
		
		String imageName = image.getPath();
		if (FileUtils.existPath(imageName)) {
			byte b[] = FileUtils.readBinaryFile(imageName);
			return new DownloadImageResult(DownloadImageResult.Status.OK, b,
					image.getContentType());
		}
		//####this is used temporary for the plat from TXNuecesAO
		if (link.contains("MapImages") && !FileUtils.existPath(imageName)){
			Page dataSheet = HtmlElementHelper.getHtmlPageByURL(link);
			byte[] contentAsBytes = dataSheet.getWebResponse().getContentAsBytes();
			return new DownloadImageResult(DownloadImageResult.Status.OK, contentAsBytes,
					image.getContentType());
		}
		//####

		return searchBy(module, image, null).getImageResult();
	}

	protected String createLinkForImage(HashMap<String, String> value) {
		HashMap<String, String> map = (HashMap<String, String>) value;
		String book = map.get("book");
		String page = map.get("page");
		String docno = map.get("docno");
		String type = map.get("type");
		String year = map.get("year");

		if (type == null) {
			type = "";
		}

		if (year == null) {
			year = "";
		}

		TSServerInfoModule imgModule = getDefaultServerInfoWrapper().getModule(
				TSServerInfo.IMG_MODULE_IDX);

		StringBuilder build = new StringBuilder("");// <a href=\"
		build.append(createPartialLink(TSConnectionURL.idDASL,
				TSServerInfo.IMG_MODULE_IDX));
		build.append("DASLIMAGE&");

		build.append(imgModule.getParamAlias(4)); /* year */
		build.append("=");
		build.append(year);
		build.append("&");

		build.append(imgModule.getParamAlias(3)); /* type */
		build.append("=");
		build.append(type);
		build.append("&");

		build.append(imgModule.getParamAlias(0)); /* book */
		build.append("=");
		build.append(book);
		build.append("&");

		build.append(imgModule.getParamAlias(1)); /* page */
		build.append("=");
		build.append(page);
		build.append("&");

		build.append(imgModule.getParamAlias(2)); /* docno */
		build.append("=");
		build.append(docno);
		return build.toString();
	}

	@Override
	protected HashMap<String, Object> fillTemplatesParameters(
			Map<String, String> params) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		CurrentInstance currentInstance = InstanceManager.getManager()
				.getCurrentInstance(searchId);
		Search search = currentInstance.getCrtSearchContext();
		HashMap<String, Object> templateParams = super
				.fillTemplatesParameters(params);
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				currentInstance.getCommunityId(), miServerID);
		String sateName = dat.getName();
		String stateAbrev = sateName.substring(0, 2);

		String chain = params.get("chain");
		templateParams.put(AddDocsTemplates.DASLPropertyChainOption, chain);
		String includeTax = params.get("includeTax");
		templateParams.put(AddDocsTemplates.DASLIncludeTaxFlag, includeTax);

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
		if(fromDate==null){
			templateParams.put(AddDocsTemplates.DASLPropertySearchFromDate,fromDate);
			templateParams.put(AddDocsTemplates.DASLPartySearchFromDate, fromDate);
		}else{
			Date fromDateDate = Util.dateParser3( fromDate );
			templateParams.put(AddDocsTemplates.DASLPropertySearchFromDate,sdf.format(fromDateDate));
			templateParams.put(AddDocsTemplates.DASLPartySearchFromDate, sdf.format(fromDateDate));
		}
		
		String toDate = params.get("toDate");
		if(toDate==null){
			templateParams.put(AddDocsTemplates.DASLPropertySearchToDate, toDate);
			templateParams.put(AddDocsTemplates.DASLPartySearchToDate, toDate);
		}else{
			Date toDateDate = Util.dateParser3( toDate );
			templateParams.put(AddDocsTemplates.DASLPropertySearchToDate, sdf.format(toDateDate));
			templateParams.put(AddDocsTemplates.DASLPartySearchToDate, sdf.format(toDateDate));
		}
		
		String searchPropType = params.get("DASLPropertySearchType");
		templateParams.put(AddDocsTemplates.DASLPropertySearchType,
				searchPropType);

		String searchPartyType = params.get("DASLPartySearchType");
		templateParams.put(AddDocsTemplates.DASLPartySearchType,
				searchPartyType);

		String DASLImageSearchType = params.get("DASLImageSearchType");
		templateParams.put(AddDocsTemplates.DASLImageSearchType,
				DASLImageSearchType);

		String lot = params.get("lot");
		templateParams.put(AddDocsTemplates.DASLLot, lot);

		String lotThrough = params.get("lotThrough");
		templateParams.put(AddDocsTemplates.DASLLotThrough, lotThrough);
		
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

		String type = params.get("type");
		if (!StringUtils.isEmpty(type)) {
			templateParams.put(AddDocsTemplates.DASLDocType, type);
		}

		String divisionNo = params.get("division");
		templateParams.put(AddDocsTemplates.DASL_DIVISION_NO, divisionNo);

		String ncbNo = params.get("ncb");
		templateParams.put(AddDocsTemplates.DASL_NCB_NO, ncbNo);

		String docNoTh = params.get("docNoTh");
		templateParams.put(AddDocsTemplates.DASLDocThrough, docNoTh);

		String lotTh = params.get("lotThrough");
		templateParams.put(AddDocsTemplates.DASL_LOT_THROUGH, lotTh);

		String blockTh = params.get("blockThrough");
		templateParams.put(AddDocsTemplates.DASL_BLOCK_THROUGH, blockTh);

		String tract = params.get("tract");
		templateParams.put(AddDocsTemplates.DASL_TRACT, tract);

		String tractTh = params.get("tractThrough");
		templateParams.put(AddDocsTemplates.DASL_TRACT_THROUGH, tractTh);

		String reference = search.getOrderNumber();
		templateParams.put(AddDocsTemplates.DASLClientReference, reference);
		templateParams.put(AddDocsTemplates.DASLClientTransactionReference,
				reference);

		templateParams.put(AddDocsTemplates.DASLStateAbbreviation, stateAbrev);

		templateParams
				.put(AddDocsTemplates.DASLCountyFIPS, dat.getCountyFIPS());

		templateParams.put(AddDocsTemplates.DASLStateFIPS, dat.getStateFIPS());

		templateParams.put(AddDocsTemplates.DASLYearFiled, params.get("year"));

		templateParams.put(AddDocsTemplates.DASLRealPartySearchType1, params
				.get("partySearchType1"));

		templateParams.put(AddDocsTemplates.DASLDocumentSearchType, params
				.get("DASLDocumentSearchType"));

		templateParams.put(AddDocsTemplates.DASLAbstractNumber, params
				.get("abstractNo"));

		String isPlat = params.get("isPlat");
		
		if("null".equalsIgnoreCase(isPlat)||StringUtils.isEmpty(isPlat)){
			isPlat = "No";
		}
		
		if("PLAT".equalsIgnoreCase(params.get("type"))){
			isPlat = "Yes";
		}
		
		templateParams.put(AddDocsTemplates.DASLIsPlat,isPlat);
		
		templateParams.put(AddDocsTemplates.DASLimageId, params.get("DASLimageId"));
		
		templateParams.put(AddDocsTemplates.DASLClientId, SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericLASite", "DaslClienId"));
		
		return templateParams;
	}

	@Override
	protected DaslResponse performImageSearch(String book, String page,
			String docno, String year, String month, String day,
			String type, String isPlat, String DASLImageSearchType, int moduleIDX) {

		int yearI = -1;
		try {
			yearI = Integer.parseInt(year);
		} catch (Exception e) {
		}

		//there is no need to do that anymore. if still found a case that didn't work, also check to work the doc 980189990, book 7685, page 1102 on TXBexarTP
//		if (yearI > 0 && yearI <= 2000) {
//			if (!StringUtils.isEmpty(docno) && docno.length() == 9 && yearI < 2000) {
//				docno = docno.replaceAll("\\A[9][3-9]0*", "");
//			}
//		}

		String stateAbrexCnty = getDataSite().getStateAbbreviation()+getDataSite().getCountyName();
		if("TXNueces".equals(stateAbrexCnty) ){
			//pad intrument number
			if(StringUtils.isNotEmpty(docno) && !docno.matches("^"+year+"\\d+") && docno.length()<10){
				docno = org.apache.commons.lang.StringUtils.leftPad(docno, 10, "0");
			}
		}
		
		isPlat = StringUtils.isEmpty(isPlat) ? "NO" : isPlat;
		
		return super.performImageSearch(book, page, docno, year, month,
				day, type, isPlat, DASLImageSearchType, moduleIDX);
	}

	@Override
	public void specificParseLegalDescription(Document doc,
			ParsedResponse item, ResultMap resultMap) {
		NodeList remarks = XmlUtils.getAllNodesForPath(doc,
				"TitleDocument/Instrument/Remarks");
		if (remarks.getLength() == 0) {
			remarks = XmlUtils.getAllNodesForPath(doc,
					"TitleDocument/LegalDescription/RemarksCopy");
		}
		resultMap.put("PropertyIdentificationSet.PropertyDescription", null);
		for (int i = 0; i < remarks.getLength(); i++) {
			if (remarks.item(i) != null) {
				try {
					resultMap.put("PropertyIdentificationSet.PropertyDescription", remarks.item(i).getFirstChild().getNodeValue());
					GenericFunctions.legalRemarksTXGenericLA(resultMap, searchId);
					GenericFunctions.copyThroughValuesSanAntonio(resultMap, searchId);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		defaultParseLegalDescription(doc, item, resultMap);
	}

	@Override
	public void specificParseGrantorGrantee(Document doc,
			ParsedResponse item, ResultMap resultMap) {
		defaultParseGrantorGrantee(doc, item, resultMap);
	}
	
	private static boolean aoOrTrIsPlated(Search search) {
		DocumentsManagerI m = search.getDocManager();
		try{
			m.getAccess();
			for(DocumentI doc:m.getDocumentsWithType(DType.ASSESOR,DType.TAX)){
				for(PropertyI prop:doc.getProperties()){
					if(prop.hasSubdividedLegal()){
						return true;
					}
				}
			}
		}finally{
			m.releaseAccess();
		}
		return false;
	}
	
	private static boolean dontMakeTheSearch(TSServerInfoModule module,	long searchId) {
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
		
		switch(module.getModuleIdx()){
			
			case TSServerInfo.SUBDIVISION_MODULE_IDX:					
				boolean test = doNotMakeSearch(module,"subdivided",false);
				if(!test){
					search.setAdditionalInfo("BASE_SEARCH_DONE","true");
					search.setAdditionalInfo("BASE_SUBDIVIDED_SEARCH_DONE","true");
				}
				return test;
				case TSServerInfo.ARB_MODULE_IDX:
					test =  doNotMakeSearch(module,"acreage",false);
					if(!test){
						if(search.getAdditionalInfo("BASE_SUBDIVIDED_SEARCH_DONE")!=null && aoOrTrIsPlated(search)){
							test = true;
						}else{
							search.setAdditionalInfo("BASE_ACREAGE_SEARCH_DONE","true");
							search.setAdditionalInfo("BASE_SEARCH_DONE","true");
						}
					}
					return test;
				case TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX:{
					Set<String> searched = (Set<String>)search.getAdditionalInfo("SEARCHED_PLATS");
					if(searched == null){
						searched = new HashSet<String>();
						search.setAdditionalInfo("SEARCHED_PLATS",searched);
					}
					
					String book = module.getParamValue(0);
					String page = module.getParamValue(1);
					test = (StringUtils.isEmpty(book)||StringUtils.isEmpty(page));
					
					if(test){
						return test;
					}
					
					if(searched.contains(book+"__"+page)){
						return true;
					}else{
						searched.add(book+"__"+page);
						return false;
					}
					
				}
				case TSServerInfo.INSTR_NO_MODULE_IDX:
					return StringUtils.isEmpty(module.getParamValue(0)) ; 
		}		
		return false;
	}
	
	private static boolean doNotMakeSearch(TSServerInfoModule module, String what,boolean ignoreQOandQV) {
		 
		String lot = "";
		String block = "";
		String platBook = "";
		String platPage = "";
		String acreage = "";
		
		if("subdivided".equals(what)){
			lot  = module.getParamValue(2);
			block  = module.getParamValue(3);
			platBook  = module.getParamValue(4);
			platPage = module.getParamValue(5);
		}
		else if("acreage".equals(what)){
			acreage = module.getParamValue(2);
			block = module.getParamValue(4);
		} 
		
		boolean emptyLot = StringUtils.isEmpty(lot);
		boolean emptyBlock = StringUtils.isEmpty(block);
		boolean emptyPlatBook = StringUtils.isEmpty(platBook);
		boolean emptyPlatPage = StringUtils.isEmpty(platPage);
	
		boolean emptyAcreage = StringUtils.isEmpty(acreage);
		boolean isPlated = !( emptyPlatBook || emptyPlatPage) && !(emptyLot && emptyBlock) ;
		
		boolean subdivided = !isPlated;
		boolean acreageBool = emptyAcreage;
		
		if("subdivided".equals(what)){
			return subdivided;
		}
		
		else if("acreage".equals(what)){
			return acreageBool||emptyBlock;
		}
		
		return subdivided  && acreageBool;
	}
	
	
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module,Object sd)throws ServerResponseException {
		 
		if(!isParentSite() && dontMakeTheSearch(module, searchId) ){
			return new ServerResponse();
		}
		
		if (module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX) {
			
			String folderName = getCrtSearchDir() + "Register" + File.separator;
			new File(folderName).mkdirs();
			
			String book = module.getParamValue(0);
			book = book == null ? "":book;
			String page = module.getParamValue(1);
			page = page == null ? "":page;
			String docNo = module.getParamValue(2);
			docNo = docNo == null ? "": docNo;
			boolean skipImage = false;
			if (module.getFunctionCount() > 3){
				skipImage = Boolean.parseBoolean(module.getParamValue(3) == null ? "false" : module.getParamValue(3));
			}
			
			String key =  book+"_"+page+"_"+docNo+"_PLAT" ;
			String fileName = folderName + key + ".tiff";
			
			boolean imageDownloaded = (new File(fileName)).exists();
			
			if (skipImage){
				imageDownloaded = true;
			}
			
			if(!imageDownloaded){
				imageDownloaded =retrieveImage(book, page,docNo, fileName, true);
			}
			
			if (imageDownloaded) {
				String grantor = "County of "+ InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
				grantor = grantor == null ? "" : grantor;
				String grantee = mSearch.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME);
				grantee = grantee == null ? "" : grantee;

				grantee = StringUtils.HTMLEntityEncode(grantee);
				grantor = StringUtils.HTMLEntityEncode(grantor);
				
				String doc = LA_FAKE_RESPONSE.replaceAll("@@Grantee@@", grantee);
				doc = doc.replaceAll("@@Grantor@@", grantor);
				doc = doc.replaceAll("@@Book@@", book == null ? "" : book);
				doc = doc.replaceAll("@@Page@@", page == null ? "" : page);
				doc = doc.replaceAll("@@DocNo@@",  docNo == null ? "" : docNo );
				doc = doc.replaceAll("@@Date@@", "01/01/1960");
				doc = doc.replaceAll("@@Type@@", "PLAT");

				return searchBy(getDefaultServerInfo().getModule(TSServerInfo.NAME_MODULE_IDX), sd, doc);
			} else {
				SearchLogger.info("Found <span class='number'>"+ "0" + "</span> <span class='rtype'>intermediate</span> results.<br/>",searchId);
				ServerResponse sr = new ServerResponse();
				ParsedResponse pr = new ParsedResponse();
				sr.setParsedResponse(pr);
				sr.setResult("<b>Could not download image</b>");
				solveHtmlResponse(module.getModuleIdx() + "", module.getParserID(), "SearchBy", sr, sr.getResult());
				return sr;
			}
		}
		
		if(module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX ||
				module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX ||
				module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX){
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if (!modules.isEmpty()) {
				return super.searchByMultipleInstrument(modules, sd, null);
			}
		}
		return super.SearchBy( module, sd);
	}

	private boolean retrieveImage(String book, String page, String docNo, String fileName,boolean isPlat) {
		String county = InstanceManager.getManager().getCurrentInstance(mSearch.getID()).getCurrentCounty().getName();
    	//    	do not retrieve the image twice
    	if(FileUtils.existPath(fileName)){
    		return true;
    	}
		 
	 	TSInterface server = TSServersFactory.GetServerInstance((int)TSServersFactory.getSiteId("TX", county, "TP"), "", "", mSearch.getID());
		
		TSServerInfoModule module = server.getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.IMG_MODULE_IDX, new SearchDataWrapper());
	
		ServerResponse res = null;
		
		server.setServerForTsd(mSearch, msSiteRealPath);
		if(!(StringUtils.isEmpty(book)||StringUtils.isEmpty(page))||!StringUtils.isEmpty(docNo)){
			module.setData( 0, book );
			module.setData( 1, page );
			module.setData( 2, docNo );
			if(isPlat){
				module.forceValue(6, "Yes");
			}
			try{
				res = ((TSServerDASL)server).searchBy(module, new ImageLinkInPage(true),null);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	
		
		if(res!=null){
			DownloadImageResult dres = res.getImageResult();
			if(dres .getStatus() == DownloadImageResult.Status.OK){
				return true;
			}
		}
		
		return false;
	}
	
	protected  DaslResponse downloadImagesUsingImageID(HashMap<String, String> params, DaslResponse daslResponse, int moduleIDX,  boolean isDocNo){
		
		DaslResponse daslResponseNew = null;
		String goodFileName = getGoodImageId(daslResponse.xmlResponse);
		String DASLImageSearchType = params.get("DASLImageSearchType");
		
		HashMap<String, String> paramsN = new HashMap<String, String>();
		
		if(!StringUtils.isEmpty(goodFileName)){
			paramsN.put("DASLimageId", goodFileName);
			paramsN.put("DASLImageSearchType", DASLImageSearchType );
			paramsN.put("DASLSearchType", "IMG");
			paramsN.put("isPlat", params.get("isPlat"));
			
			// create XML query
			String xmlQuery = buildSearchQuery(paramsN, moduleIDX);
			if ( StringUtils.isEmpty(xmlQuery ) ) {
				return null;
			}
			daslResponseNew = getDaslSite().performSearch( xmlQuery, searchId );
		}
		
		return daslResponseNew;
	}
	
	private static String getGoodImageId(String xmlResponse){
		if(StringUtils.isEmpty(xmlResponse)){
			return "";
		}
		Matcher mat = PATT_MULTIPLE_IMAGES_NAME.matcher( xmlResponse );
		List<String> nameList= new ArrayList<String>();
		while( mat.find() ){
			nameList.add( mat.group(1) );
		}
		
		if(nameList.size()==0){
			mat = SECCOND_PATT_MULTIPLE_IMAGES_NAME.matcher( xmlResponse );
			while( mat.find() ){
				nameList.add( mat.group(1) );
			}
		}
		
		if(nameList.size()>0){
			return nameList.get(0);
		}
		
		return "";
	}
	
	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		
		ConfigurableNameIterator nameIterator = null;
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
	    
	    FilterResponse doctypeFilter = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);

	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	    	module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
	    	module.setIndexInGB(id);
	    	module.setTypeSearchGB("grantor");
	    	module.clearSaKeys();
	    	module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
	    	String date=gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
	    	if (date!=null){ 
	    		module.getFunction(0).forceValue(date);
	    	}
	    	GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
	    	nameFilter.setUseSynonymsForCandidates(true);
	    	nameFilter.setUseDoubleMetaphoneForLast(true);
		    
		    module.addFilter(nameFilter);
			module.addFilter( NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			module.addFilter( doctypeFilter );
	    	module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
			module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
			module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
			module.setIteratorType( 10,	FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE);
			
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			nameIterator.setAllowMcnPersons( true );
			nameIterator.setPersonalTypeName("INDIVIDUAL");
			nameIterator.setCompanyTypeName("BUSINESS");
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
				
				nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
		    	nameFilter.setUseSynonymsForCandidates(true);
			    module.addFilter(nameFilter);
			    nameFilter.setUseDoubleMetaphoneForLast(true);
			    
				module.addFilter( NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter( doctypeFilter );
				module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
				
				module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
				module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
				module.setIteratorType( 10,	FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
				nameIterator.setAllowMcnPersons( true );
				nameIterator.setPersonalTypeName("INDIVIDUAL");
				nameIterator.setCompanyTypeName("BUSINESS");
			 	module.addIterator(nameIterator);			
				modules.add(module);
		 	}
	    }
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}
	
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
	 	List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		if(searchType == Search.AUTOMATIC_SEARCH) {
			
			Set<InstrumentI> allAoRef = getAllAoAndTaxReferences(global);
			
			String lot = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			String block = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			String ncb = global.getSa().getAtribute(SearchAttributes.LD_NCB_NO);
			String platBook = global.getSa().getAtribute(SearchAttributes.LD_BOOKNO);
			String platPage = global.getSa().getAtribute(SearchAttributes.LD_PAGENO);
			
			boolean doAStrictNameSearch = (allAoRef.size()==0 && !(StringUtils.isEmpty(lot) && StringUtils.isEmpty(block))
					&& StringUtils.isEmpty(ncb) && (StringUtils.isEmpty(platBook)||StringUtils.isEmpty(platPage)) );
			
			if(doAStrictNameSearch){
				FilterResponse[] filtersForStrictOwnerSearch = { NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, global.getID(), null ), 
						new StrictLotBlockLegalFilter(searchId)};
				addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersForStrictOwnerSearch);
				SearchLogger.info( "<font color='red'><b> No transaction History Detected and Not enough info for Subdivision Search. We must perform Name Look Up on TP3.</b></font></br>", searchId );
			}
			 
			addAoAndTaxReferenceSearches(serverInfo, modules, allAoRef, searchId, isUpdate());
			
			PinFilterResponse pinFilter = PINFilterFactory.getPinFilter(searchId, SearchAttributes.LD_PARCELNO3, true, false);
			pinFilter.setIgNoreStrartingZeroes(true);
			
			FilterResponse legalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
			
			addIteratorModule(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId, isUpdate(), legalFilter, pinFilter);
			
			addIteratorModule(serverInfo, modules, TSServerInfo.ARB_MODULE_IDX, searchId, isUpdate(), legalFilter, pinFilter);
			
			addIteratorModule(serverInfo, modules, TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX, searchId, isUpdate());
			
			FilterResponse nameFilterOwner 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, global.getID(), null );
			nameFilterOwner.setInitAgain(true);
			((GenericNameFilter) nameFilterOwner).setIgnoreMiddleOnEmpty(true);
			((GenericNameFilter)nameFilterOwner).setUseSynonymsForCandidates(true);
			((GenericNameFilter)nameFilterOwner).setUseDoubleMetaphoneForLast(true);
			
			FilterResponse[] filtersO 	= { nameFilterOwner,pinFilter, legalFilter,  new LastTransferDateFilter(searchId)};
			 
			ArrayList<NameI> searchedNames = addNameSearch(  modules, serverInfo, SearchAttributes.OWNER_OBJECT, null, filtersO  );
			
			addOCRSearch( modules, serverInfo, legalFilter, pinFilter);
			
			addNameSearch( modules, serverInfo,SearchAttributes.OWNER_OBJECT, searchedNames==null?new ArrayList<NameI>():searchedNames, filtersO);
			
			if(!global.isRefinance()){
				FilterResponse nameFilterBuyer 	= NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, global.getID(), null );
				((GenericNameFilter)nameFilterBuyer).setUseSynonymsForCandidates(true);
				((GenericNameFilter)nameFilterOwner).setUseDoubleMetaphoneForLast(true);
				nameFilterBuyer.setInitAgain(true);
				addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, searchedNames, nameFilterBuyer, DoctypeFilterFactory.getDoctypeBuyerFilter( searchId ) );
			}
			
		}
		serverInfo.setModulesForAutoSearch(modules);
	 }
	 
	 
	 protected ArrayList<NameI>  addNameSearch( List<TSServerInfoModule> modules, TSServerInfo serverInfo,String key, ArrayList<NameI> searchedNames, FilterResponse ...filters ) {
						
			ConfigurableNameIterator nameIterator = null;
			
			TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
			module.clearSaKeys();
			module.setSaObjKey(key);

			CommunityAttributes ca = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();
			Integer years = null;
			if(SearchAttributes.OWNER_OBJECT.equals(key)) {
				years = ServerConfig.getOwnerNameSearchFilterAllow(ca);
			} else if(SearchAttributes.BUYER_OBJECT.equals(key)) {
				years = ServerConfig.getBuyerNameSearchFilterAllow(ca);
			}
			
			if(years != null) {
				Date toDate = getSearch().getSa().getEndDate();
				Calendar cal = Calendar.getInstance();
				cal.setTime(toDate);
				cal.add(Calendar.YEAR, - years);
				
				if (ServerConfig.useFromDateAtOwnerSearch(ca)){
					module.forceValue(0, new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(cal.getTime()));
				} else{
					module.addFilter(new BetweenDatesFilterResponse(searchId, cal.getTime()));
					module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
					module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
				}
			}

			for (int i = 0; i < filters.length; i++) {
				if(filters[i]!=null){
					module.addFilter(filters[i]);
				}
			}
			addBetweenDateTest(module, false, true, true);
			
			module.setIteratorType(2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
			module.setIteratorType(4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
			module.forceValue(6,  "");
			module.forceValue(9,  "GRANTOR");
			module.setIteratorType(10,	FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE);
			
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			nameIterator.setAllowMcnPersons( true );
			nameIterator.setPersonalTypeName("INDIVIDUAL");
			nameIterator.setCompanyTypeName("BUSINESS");
			
			if ( searchedNames!=null ) {
				nameIterator.setInitAgain( true );
				nameIterator.setSearchedNames( searchedNames );
			}
			
			searchedNames = nameIterator.getSearchedNames() ;
			module.addIterator( nameIterator );
			modules.add( module );
			return searchedNames;
		}
		
		protected void addOCRSearch(List<TSServerInfoModule> modules,TSServerInfo serverInfo, FilterResponse ...filters){
			// OCR last transfer - book / page search
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			OcrOrBootStraperIterator ocrBookPageIterator = new OcrOrBootStraperIterator(searchId, getDataSite()){
				private static final long serialVersionUID = -8527627674643716081L;
				
				@Override
				public Object current() {
		         
					TSServerInfoModule crtState = (TSServerInfoModule) super.current();
					Instrument instr = ((Instrument) getStrategy().current());
					if (instr.getRealdoctype().equalsIgnoreCase("plat")){
						crtState.setSkipModule(true);
						return crtState;
					}
					return crtState;
				}
			};
		    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		    module.clearSaKeys();
		    module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH );
		    module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH );
		    for(int i=0;i<filters.length;i++){
		    	module.addFilter(filters[i]);
		    }
		    addBetweenDateTest(module, false, false, false);
		    ocrBookPageIterator.setInitAgain(true);
			module.addIterator(ocrBookPageIterator);
			modules.add(module);
			
			// OCR last transfer - book / page search plat type
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			OcrOrBootStraperIterator ocrPlatIterator = new OcrOrBootStraperIterator(searchId, getDataSite()){
				private static final long serialVersionUID = -8527627674643716081L;
				
				@Override
				public Object current() {
		         
					TSServerInfoModule crtState = (TSServerInfoModule) super.current();
					Instrument instr = ((Instrument) getStrategy().current());
					if (!instr.getRealdoctype().equalsIgnoreCase("plat")){
						crtState.setSkipModule(true);
						return crtState;
					}
					return crtState;
				}
			};
			
			module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
			module.clearSaKeys();
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
			for(int i = 0; i < filters.length;i++){
				module.addFilter(filters[i]);
			}
			addBetweenDateTest(module, false, false, false);
			
			ocrPlatIterator.setInitAgain(true);
			module.addIterator(ocrPlatIterator);
			modules.add(module);
			
		    // OCR last transfer - instrument search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		    module.clearSaKeys();
		    module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		    for(int i=0;i<filters.length;i++){
		    	module.addFilter(filters[i]);
		    }
		    addBetweenDateTest(module, false, false, false);
			modules.add(module);
		}
	 
	 private static Set<InstrumentI> getAllAoAndTaxReferences(Search search){
		Set<InstrumentI> allAoRef = new HashSet<InstrumentI>();
		DocumentsManagerI manager = search.getDocManager();
		try{
			manager.getAccess();
			List<DocumentI> list = manager.getDocumentsWithType( true, DType.ASSESOR, DType.TAX );
			for(DocumentI assessor:list){
				if (ro.cst.tsearch.servers.HashCountyToIndex.isLegalBootstrapEnabled(search.getCommId(), assessor.getSiteId())) {
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
		return removeEmptyReferences(allAoRef);
	 }
	 
	 private static Set<InstrumentI> removeEmptyReferences(Set<InstrumentI> allAo){
		 Set<InstrumentI> ret = new HashSet<InstrumentI>();
		 for(InstrumentI i:allAo){
			 if(i.hasBookPage()||i.hasInstrNo()){
				 ret.add(i);
			 }
		 }
		 return ret;
	 }
	 
		private  boolean addAoAndTaxReferenceSearches(TSServerInfo serverInfo, List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef,long searchId,  boolean isUpdate){
			boolean atLeastOne = false;
			final Set<String> searched = new HashSet<String>();
			
			for(InstrumentI inst:allAoRef){
				boolean temp = addBookPageSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
				atLeastOne = atLeastOne || temp;
				temp = addInstNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
				atLeastOne = atLeastOne || temp;
			}
			return atLeastOne;
		}
		
		
		private static boolean addBookPageSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched,boolean isUpdate){
			String county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			 if(inst.hasBookPage()){
				String originalB = inst.getBook();
				String originalP = inst.getPage();
				int bookL = originalB.length();
				int pageL = originalP.length();
				
				String book = originalB.replaceFirst("^0+", "");
				String page = originalP.replaceFirst("^0+", "");
				if(!searched.contains(book+"_"+page)){
					searched.add(book+"_"+page);
				}else{
					return false;
				}
				
				if("Comal".equalsIgnoreCase(county)){
					if(bookL==5 && pageL==5){
						TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
						module.setData(0, originalB+originalP);
						//module.setData(2, year);
						if (isUpdate) {
							module.addFilter(new BetweenDatesFilterResponse(searchId));
						}
						modules.add(module);
					}
				}
				
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.setData(0, book);
				module.setData(1, page);
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				modules.add(module);
				return true;
			}
			return false;
		}
		
		
		private boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
			if ( inst.hasInstrNo() ){
				inst = prepareInstrumentForSearch(inst);
			
				String instr = inst.getInstno().replaceFirst("^0+", "");
				String year = String.valueOf(inst.getYear());
				if(!searched.contains(instr+year)){
					searched.add(instr+year);
				}else{
					return false;
				}
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.setData(0, instr);
				//module.setData(2, year);
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				modules.add(module);
				return true;
			}
			return false;
		}
		
		private InstrumentI prepareInstrumentForSearch(final InstrumentI inst){
			
			InstrumentI ret = inst.clone();
			
			int countyId = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getCountyId().intValue();
			
			if(countyId==CountyConstants.TX_Bexar){
				final String form = "0000000";
				if(inst.hasInstrNo()){
					String instNo = inst.getInstno().replaceFirst("^0+", "");
					int yearI = inst.getYear();
					String year = "";
					
					if(yearI >= 2000){
						year = yearI+"";
					}else if(yearI>1000){
						year = yearI+"";
						year = year.substring(year.length()-2,year.length());
					}
					if(instNo.length()<=form.length()){
						instNo = year + form.substring(0,form.length()-instNo.length())+instNo;
					}
					
					ret.setInstno(instNo);
				}
			}else if (countyId==CountyConstants.TX_Nueces){
				final String form = "000000";
				if(inst.hasInstrNo()){
					String instNo = inst.getInstno().replaceFirst("^0+", "");
					int yearI = inst.getYear();
					String year = "";
					
					if(yearI >= 1995){
						year = yearI+"";
						if(instNo.length()<=form.length()){
							instNo = year + form.substring(0,form.length()-instNo.length())+instNo;
						}
					}
					
					ret.setInstno(instNo);
				}
			}else if (countyId==CountyConstants.TX_Comal){
				final String form = "00000000";
				if(inst.hasInstrNo()){
					String instNo = inst.getInstno().replaceFirst("^0+", "");
					int yearI = inst.getYear();
					String year = "";
					
					if(yearI >= 1996){
						year = yearI+"";
						if(instNo.length()<=form.length()){
							instNo = year + form.substring(0,form.length()-instNo.length())+instNo;
						}
					}
					
					ret.setInstno(instNo);
				}
			} else if (countyId==CountyConstants.TX_Guadalupe) {
				if(inst.hasInstrNo()){
					String instNo = inst.getInstno().replaceFirst("^0+", "");
					int yearI = inst.getYear();
					if (yearI!=SimpleChapterUtils.UNDEFINED_YEAR && instNo.length()<6) {
						instNo = Integer.toString(yearI) + org.apache.commons.lang.StringUtils.leftPad(instNo, 6, "0");
					}
					ret.setInstno(instNo);
				}
			}
			
			return ret;
		}
		
		private void addIteratorModule( TSServerInfo serverInfo, List<TSServerInfoModule> modules,int code, long searchId, boolean isUpdate, FilterResponse ... filters){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
			module.clearSaKeys();
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
			module.addIterator(it);
			if (isUpdate && TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX!=module.getModuleIdx()) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			if(filters!=null){
				for(FilterResponse filter:filters){
					module.addFilter(filter);
				}
			}
			modules.add(module);
		}
	 
		
		static protected class LegalDescriptionIterator extends GenericRuntimeIterator<PersonalDataStruct> implements LegalDescriptionIteratorI {
			
			private static final long serialVersionUID = 8989586891817117069L;
			
			LegalDescriptionIterator(long searchId) {
				super(searchId);
			}
			
			@Override
			public boolean isTransferAllowed(RegisterDocumentI doc) {
				return doc != null && doc.isOneOf(DocumentTypes.TRANSFER);
			}
			
			@Override
			public void loadSecondaryPlattedLegal(LegalI legal, ro.cst.tsearch.search.iterator.data.LegalStruct legalStruct) {
				legalStruct.setLot(legal.getSubdivision().getLot());
				legalStruct.setBlock(legal.getSubdivision().getBlock());
			}
			
			public static boolean isTP(long searchId) {
				int siteType = -1;
				try {
					siteType = HashCountyToIndex.getCrtServer(searchId, false).getSiteTypeInt();
				} catch (BaseException be) {}
				return (siteType == GWTDataSite.TP_TYPE);
			}
			
			@SuppressWarnings("unchecked")
			List<PersonalDataStruct> createDerivationInternal(long searchId){
				
				boolean isTP = isTP(searchId); 
				
				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				DocumentsManagerI m = global.getDocManager();
				List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)global.getAdditionalInfo("TX_LA_LOOK_UP_DATA");
				
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
				
				String blockAOorTR = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
				String ncbNo =  global.getSa().getAtribute(SearchAttributes.LD_NCB_NO);
				String platBookAOorTR = global.getSa().getAtribute(SearchAttributes.LD_BOOKNO);
				String platPageAOorTR = global.getSa().getAtribute(SearchAttributes.LD_PAGENO);
				
				CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
				String county = ci.getCurrentCounty().getName();
				
				if(legalStructList==null){
					legalStructList = new ArrayList<PersonalDataStruct>();
					
					try{
						m.getAccess();
						List<DocumentI> listRodocs = new ArrayList<DocumentI>();
						
						listRodocs.addAll(ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator.getGoodDocumentsOrForCurrentOwner(this, m,global,true));
						if(listRodocs.isEmpty()){
							listRodocs.addAll(ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator.getGoodDocumentsOrForCurrentOwner(this, m, global, false));
						}
						if(listRodocs.isEmpty()){
							listRodocs.addAll(m.getRoLikeDocumentList(true));
						}
						
						DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
						for( DocumentI reg: listRodocs){
							if(legalStructList.size()>0){
								break;
							}
							for (PropertyI prop: reg.getProperties()){
								if(prop.hasLegal()){
									LegalI legal = prop.getLegal();
									if(legal.hasSubdividedLegal()){
										PersonalDataStruct legalStructItem = new PersonalDataStruct();
										SubdivisionI subdiv = legal.getSubdivision();
										
										String block = subdiv.getBlock();
										String lot = subdiv.getLot();
										String lotThrough = subdiv.getLotThrough();
										String platBook = subdiv.getPlatBook();
										String platPage = subdiv.getPlatPage();
										String platInst = subdiv.getPlatInstrument();
										String unit = subdiv.getUnit();
										legalStructItem.unit = StringUtils.isEmpty(unit)?"":unit;
										legalStructItem.acreage = subdiv.getAcreage();
										legalStructItem.block = StringUtils.isEmpty(block)?"":block;
										legalStructItem.lotThrough = StringUtils.isEmpty(lotThrough)?"":lotThrough;
										legalStructItem.platBook = StringUtils.isEmpty(platBook)?"":platBook;
										legalStructItem.platPage = StringUtils.isEmpty(platPage)?"":platPage;
										legalStructItem.platInst = StringUtils.isEmpty(platInst)?"":platInst;
										
										if(prop.hasPin()){
											legalStructItem.parcel =prop.getPin().getPin(PinType.PID);
										}
										
										if(StringUtils.isEmpty(lot)){
											if(!StringUtils.isEmpty(unit)){
												legalStructItem.lot = unit;
												if(!unit.startsWith("U")){
													PersonalDataStruct legalStructItemA = null;
													try{
														legalStructItemA = (PersonalDataStruct)legalStructItem.clone();
													}catch (CloneNotSupportedException e) {
														e.printStackTrace();
													}
													if(legalStructItemA!=null){
														legalStructItemA.lot = "U"+unit;
														prepareForCounties(legalStructItemA,county);
														if( !testIfExist(legalStructList,legalStructItemA,"subdivdied",county) ){
															legalStructList.add(legalStructItemA);
														}
													}
												}
											}
										}else{
											legalStructItem.lot = lot;
										}
										
										if(StringUtils.isEmpty(legalStructItem.lot)){//try to complete with the unit from search page
											String unitFromSearchPage = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_UNIT);
											if(!StringUtils.isEmpty(unitFromSearchPage)){
												unitFromSearchPage = unitFromSearchPage.replace("#", "");
											}
											legalStructItem.lot = unitFromSearchPage;
										}
										
										prepareForCounties(legalStructItem,county);
										
										if(StringUtils.isEmpty(legalStructItem.acreage)){
											if( !testIfExist(legalStructList,legalStructItem, "subdivided",county) ){
												legalStructList.add(legalStructItem);
											}
										}else{
											if( !testIfExist(legalStructList,legalStructItem, "acreage",county) ){
												legalStructList.add(legalStructItem);
											}
										}
									}
								}
							}
						}
						global.setAdditionalInfo("TX_LA_LOOK_UP_DATA",legalStructList);
						if(legalStructList.size()>0){
							boostrapSubdividedData(legalStructList, global, true);
						}
					}
					finally{
						m.releaseAccess();
					}
				}
				
				if( legalStructList.size()>1 && !(isPlatedMultyLot(legalStructList))){
					global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
				}
				
				if( isPlatedMultyLot(legalStructList)&& allAoAndTrlots.length > 0 ){
					addLegalStructItemsUsingAoAndTrLots(legalStructList,allAoAndTrlots, county, isTP);
				}
				
				if(legalStructList.size()==0){
					if (isTP) {
						StringBuilder sb = new StringBuilder();
						HashSet<String> newAllAoAndTrlots = new HashSet<String>();
						for (String lot:allAoAndTrlots) {
							if (lot.matches("\\d+")) {
								sb.append(lot).append(" ");
							} else {
								newAllAoAndTrlots.add(lot);
							}
						}
						if(!StringUtils.isEmpty(ncbNo) ) {
							for (LotInterval interval: LotMatchAlgorithm.prepareLotInterval(sb.toString())) {
								PersonalDataStruct legalStructItem = new PersonalDataStruct();
								int lot = interval.getLow();
								int lotThrough = interval.getHigh();
								legalStructItem.lot = Integer.toString(lot);
								if (lot!=lotThrough) {
									legalStructItem.lotThrough = Integer.toString(lotThrough);
								}
								legalStructItem.block = blockAOorTR;
								legalStructItem.platBook = "N";
								legalStructItem.platPage = ncbNo;
								legalStructList.add(legalStructItem);
							}
						}
						if(!StringUtils.isEmpty(platBookAOorTR)&&!StringUtils.isEmpty(platPageAOorTR)) {
							for (LotInterval interval: LotMatchAlgorithm.prepareLotInterval(sb.toString())) {
								PersonalDataStruct legalStructItem = new PersonalDataStruct();
								int lot = interval.getLow();
								int lotThrough = interval.getHigh();
								legalStructItem.lot = Integer.toString(lot);
								if (lot!=lotThrough) {
									legalStructItem.lotThrough = Integer.toString(lotThrough);
								}
								legalStructItem.block = blockAOorTR;
								legalStructItem.platBook = platBookAOorTR;
								legalStructItem.platPage = platPageAOorTR;
								legalStructList.add(legalStructItem);
							}
						}
						allAoAndTrlots = newAllAoAndTrlots.toArray(new String[newAllAoAndTrlots.size()]);
					}
					if(!StringUtils.isEmpty(ncbNo) ){
						for(String lot:allAoAndTrlots){
							PersonalDataStruct legalStructItem = new PersonalDataStruct();
							legalStructItem.lot = lot;
							legalStructItem.block = blockAOorTR;
							legalStructItem.platBook = "N";
							legalStructItem.platPage = ncbNo;
							legalStructList.add(legalStructItem);
						}
					}
					if(!StringUtils.isEmpty(platBookAOorTR)&&!StringUtils.isEmpty(platPageAOorTR)){
						for(String lot:allAoAndTrlots){
							PersonalDataStruct legalStructItem = new PersonalDataStruct();
							legalStructItem.lot = lot;
							legalStructItem.block = blockAOorTR;
							legalStructItem.platBook = platBookAOorTR;
							legalStructItem.platPage = platPageAOorTR;
							legalStructList.add(legalStructItem);
						}
					}
				}
				cleanLegals(legalStructList);
				return legalStructList;
			}
			
			protected List<PersonalDataStruct> createDerrivations(){
				return createDerivationInternal(searchId);
			}
			
			protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
				
				switch(module.getModuleIdx()){
					case TSServerInfo.SUBDIVISION_MODULE_IDX:
						module.setData(2, str.lot);
						if (isTP(searchId) && StringUtils.isNotEmpty(str.lotThrough)) {
							module.setData(7, str.lotThrough);
						}
						module.setData(3, str.block);
						module.setData(4, str.platBook);
						module.setData(5, str.platPage);
					break;
					case TSServerInfo.ARB_MODULE_IDX:
						module.setData(4, str.block);
						module.setData(6, str.block);
						module.setData(2, str.acreage);
					break;
					case TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX:
						module.setData(0, str.platBook);
						module.setData(1, str.platPage);
					break;
				}
			}
			
		}
		
		private static void cleanLegals(List<PersonalDataStruct>  legalStructList){
			for(PersonalDataStruct p:legalStructList){
				p.acreage = p.acreage==null?"":p.acreage.replaceFirst("^0+", "");
				p.block = p.block==null?"":p.block.replaceFirst("^0+", "");
				p.lot = p.lot==null?"":p.lot.replaceFirst("^0+", "");
				p.lotThrough = p.lotThrough==null?"":p.lotThrough.replaceFirst("^0+", "");
				p.platBook = p.platBook==null?"":p.platBook.replaceFirst("^0+", "");
				p.platPage = p.platPage==null?"":p.platPage.replaceFirst("^0+", "");
				p.platInst = p.platInst==null?"":p.platInst.replaceFirst("^0+", "");
				p.unit= p.unit==null?"":p.unit.replaceFirst("^0+", "");
			}
		}
		
		private static void prepareForCounties(PersonalDataStruct str,String county){
			
		}
		
		private static boolean testIfExist(List<PersonalDataStruct> legalStruct2, PersonalDataStruct l, String string,String county) {
			prepareForCounties(l,county);
			
			if("acreage".equalsIgnoreCase(string)){
				for(PersonalDataStruct p:legalStruct2){
					prepareForCounties(p,county);
					if(p.isAcreage()){
						if(l.equalsAcreage(p)){
							return true;
						}
					}
				}
			}else if("subdivided".equalsIgnoreCase(string)){
				for(PersonalDataStruct p:legalStruct2){
					if(p.isPlated()){
						prepareForCounties(p,county);
						if(l.equalsSubdivided(p)){
							return true;
						}
					}
				}
			}
			return false;
		}
		
		private static void addLegalStructItemsUsingAoAndTrLots(List<PersonalDataStruct> legalStructList, String[] allAoAndTrlots,String county, boolean isTP) {
			PersonalDataStruct first =  getFirstPlatedStruct(legalStructList);
			if(first!=null){
				if (isTP) {
					StringBuilder sb = new StringBuilder();
					HashSet<String> newAllAoAndTrlots = new HashSet<String>();
					for (String lot: allAoAndTrlots) {
						if (lot.matches("\\d+")) {
							sb.append(lot).append(" ");
						} else {
							newAllAoAndTrlots.add(lot);
						}
						
					}
					String lots = LegalDescription.cleanValues(sb.toString(), false, true);
					for (LotInterval interval: LotMatchAlgorithm.prepareLotInterval(lots)) {
						int lot = interval.getLow();
						int lotThrough = interval.getHigh();
						PersonalDataStruct n = null;
						try {
							n = (PersonalDataStruct)first.clone();
						} catch (CloneNotSupportedException e) {
							e.printStackTrace();
						}
						if(n!=null){
							n.lot = Integer.toString(lot);
							if (lot!=lotThrough) {
								n.lotThrough = Integer.toString(lotThrough);
							}	
							addLegalStructAtInterval(legalStructList, n);
						}
					}
					allAoAndTrlots = newAllAoAndTrlots.toArray(new String[newAllAoAndTrlots.size()]);
				}
				for (String lot:allAoAndTrlots){
					PersonalDataStruct n = null;
					try {
						n = (PersonalDataStruct)first.clone();
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
					if(n!=null){
						n.lot = lot;
						if(!testIfExist(legalStructList,n, "subdivided",county)){
							legalStructList.add(n);
						}
					}
				}
			}
		}
		
		/** 
		 * add an item extending lot interval of another item, if possible
		 * if not possible, add the new item separately
		 **/
		private static void addLegalStructAtInterval(List<PersonalDataStruct> list, PersonalDataStruct newItem) {
			
			if (list.size()==0) {
				list.add(newItem);
				return;
			}
			
			String newItemLot = newItem.lot;
			String newItemLotThrough = newItem.lotThrough;
			if (newItemLotThrough.equals("")) {
				newItemLotThrough = newItemLot;
			}
			
			if (!newItemLot.matches("\\d+") || !newItemLotThrough.matches("\\d+")) {
				list.add(newItem);
				return;
			}
			
			boolean found = false;
			for (PersonalDataStruct current: list) {
				String currentItemLot = current.lot;
				String curentItemLotThrough = current.lotThrough;
				if (curentItemLotThrough.equals("")) {
					curentItemLotThrough = currentItemLot;
				}
				if (equalExceptLot(current, newItem) && currentItemLot.matches("\\d+") && curentItemLotThrough.matches("\\d+")) {
					String concatenated =  newItemLot + "-" + newItemLotThrough + " " +
										   currentItemLot + "-" + curentItemLotThrough;
					Vector<LotInterval> intervals = LotMatchAlgorithm.prepareLotInterval(concatenated);
					if (intervals.size()==1) {	//there is only an interval
						found = true;
						LotInterval lotInterval = intervals.elementAt(0);
						int low = lotInterval.getLow();
						int high = lotInterval.getHigh();
						current.lot = Integer.toString(low);
						if (low!=high) {
							current.lotThrough = Integer.toString(high);
						}
						break;	
					} 
				}
			}
			if (!found) {
				list.add(newItem);
			}
			
		}
		
		public static boolean equalExceptLot(PersonalDataStruct p1, PersonalDataStruct p2) {
			return ( (p1.block).equals(p2.block) &&
					 (p1.unit).equals(p2.unit) &&
					 (p1.platBook).equals(p2.platBook) &&
					 (p1.platPage).equals(p2.platPage) &&
					 (p1.platInst).equals(p2.platInst) &&
					 (p1.acreage).equals(p2.acreage) &&
					 (p1.parcel).equals(p2.parcel) ); 
		}
		
		private static PersonalDataStruct getFirstPlatedStruct(List<PersonalDataStruct> list){
			for(PersonalDataStruct struct:list){
				if(struct.isPlated()){
					return struct;
				}
			}
			return null;
		}
		
		private static boolean isPlatedMultyLot(List<PersonalDataStruct> legalStructList) {
			boolean isPlatedMultyLot = true;
			
			if(legalStructList == null || legalStructList.size()==0){
				isPlatedMultyLot = false;
			}
			
			for(PersonalDataStruct p:legalStructList){
				if(!p.isPlated()){
					isPlatedMultyLot =  false;
					break;
				}
			}
			
			if(isPlatedMultyLot){
				PersonalDataStruct first =  getFirstPlatedStruct(legalStructList);
				
				if(first==null){
					isPlatedMultyLot =  false;
				}else{
					for(PersonalDataStruct p:legalStructList){
						if(!p.block.equalsIgnoreCase(first.block)||!p.platBook.equalsIgnoreCase(first.platBook)||!p.platPage.equalsIgnoreCase(first.platPage)){
							isPlatedMultyLot =  false;
							break;
						}
					}
				}
			}
			
			return isPlatedMultyLot ;
		}
		
		private static void boostrapSubdividedData(List<PersonalDataStruct> legalStruct1, Search search, boolean boostrapPlatsAndBlock) {
			
			String aoAndTrLots = search.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			String []allAoAndTrlots = new String[0];
			
			if(!StringUtils.isEmpty(aoAndTrLots)){
				allAoAndTrlots = aoAndTrLots.split("[ /t/r/n,]+");
			}
			
			Set<String> allLots = new HashSet<String>();
			allLots.addAll(Arrays.asList(allAoAndTrlots));
			
			SearchAttributes sa = search.getSa();
			if(boostrapPlatsAndBlock){
				for(PersonalDataStruct legalStruct:legalStruct1){
					if(!StringUtils.isEmpty(legalStruct.platBook)){
						sa.setAtribute( SearchAttributes.LD_BOOKNO, legalStruct.platBook);
					}
					if(!StringUtils.isEmpty(legalStruct.platPage)){
						sa.setAtribute( SearchAttributes.LD_PAGENO, legalStruct.platPage);
					}
					if(!StringUtils.isEmpty(legalStruct.block)){
						sa.setAtribute( SearchAttributes.LD_SUBDIV_BLOCK, legalStruct.block);
					}
					if(!StringUtils.isEmpty(legalStruct.parcel)){
						String parcel = sa.getAtribute(SearchAttributes.LD_PARCELNO3);
						if(StringUtils.isEmpty(parcel)){
							sa.setAtribute( SearchAttributes.LD_PARCELNO3, legalStruct.parcel);
						}else{
							sa.setAtribute( SearchAttributes.LD_PARCELNO3, parcel+","+legalStruct.parcel);
						}
						
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
		 
		protected static class PersonalDataStruct implements Cloneable{
			String block = "";
			String unit = 	"";
			String platBook	=	"";
			String platPage=	"";
			String platInst=	"";
			String acreage="";
			String lot	=	"";
			String lotThrough	=	"";
			String parcel	=	"";
			
			@Override
			protected Object clone() throws CloneNotSupportedException {
				return super.clone();
			}
			
			public boolean equalsSubdivided(PersonalDataStruct struct) {
				return this.block.equals(struct.block)&&this.lot.equals(struct.lot)&&this.platBook.equals(struct.platBook)
				&&this.platPage.equals(struct.platPage)&&this.unit.equals(struct.unit);
			}
			
			public boolean equalsAcreage(PersonalDataStruct struct) {
				return this.acreage.equals(struct.acreage);
			}

			public boolean isPlated() {
				return !StringUtils.isEmpty(platBook)&&!StringUtils.isEmpty(platPage)&&(!StringUtils.isEmpty(lot)||!StringUtils.isEmpty(block));
			}

			public boolean isAcreage() {
				return !StringUtils.isEmpty(acreage);
			}
			
		}
		
		private static class StrictLotBlockLegalFilter extends GenericLegal{
			
			private static final long serialVersionUID = -3671581176563719603L;

			public StrictLotBlockLegalFilter(long searchId) {
				super(searchId);	
			}
			
			@Override
			public BigDecimal getScoreOneRow(ParsedResponse row) {
				
				DocumentI doc = row.getDocument();
				
				Set<PropertyI> allProps= doc.getProperties();
				
				boolean foundGood = false;
				
				for(PropertyI prop:allProps){
					if(prop.hasSubdividedLegal()){
						SubdivisionI sub = prop.getLegal().getSubdivision();
						if(sub.hasLot() || sub.hasBlock()){
							foundGood = true;
						}
					}
				}
				if(!foundGood){
					return BigDecimal.ZERO;
				}
				
				return super.getScoreOneRow(row);
			}
			@Override
			public String getFilterCriteria(){
				String filterCriteria = super.getFilterCriteria();
				filterCriteria += "(reject docs with empty lot and block)";
				return filterCriteria; 
				
			}
			
		}

		@Override
		public void specificParseAddress(Document doc, ParsedResponse item,
				ResultMap resultMap) {
		}
		
		public boolean isInstrumentSaved(String instrumentNo,
				DocumentI documentToCheck, HashMap<String, String> data) {
			
			boolean firstTry = super.isInstrumentSaved(instrumentNo, documentToCheck, data);
			
			if(firstTry) {
				return true;
			}
			
			if(documentToCheck == null) {
				return false;
			}
			
			if (mSearch.getCountyId().equals(CountyConstants.TX_Bexar_STRING)){
				
				DocumentsManagerI documentManager = getSearch().getDocManager();
		    	try {
		    		documentManager.getAccess();
		    		InstrumentI instToCheck = documentToCheck.getInstrument();
		    		for(DocumentI e: documentManager.getDocumentsWithDataSource(false, "RO")){
		    			InstrumentI savedInst = e.getInstrument();
		    			
		    			String savedDocCateg = DocumentTypes.getDocumentCategory(e.getServerDocType(), searchId); 
		    			if( (savedInst.getInstno().equals(instToCheck.getInstno()) || instToCheck.getInstno().equals(savedInst.getInstno()))  
		    					&& (savedInst.getBook().equals(instToCheck.getBook()) && savedInst.getPage().equals(instToCheck.getPage()))  
		    					&& savedInst.getDocno().equals(instToCheck.getDocno())
		    					&& savedDocCateg.equals(documentToCheck.getDocType())
		    					&& savedInst.getYear() == instToCheck.getYear()
		    			){
		    				return true;
		    			}
		    		}
		    	} finally {
		    		documentManager.releaseAccess();
		    	}
			}
			return false;
		}
		
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if (restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();
		String documentNumber = restoreDocumentDataI.getDocumentNumber();
		Date recDate = restoreDocumentDataI.getRecordedDate();
		
		SearchAttributes sa = getSearchAttributes();
		
		if(StringUtils.isEmpty(instrumentNumber)) {
			instrumentNumber = documentNumber;
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY);
		if(StringUtils.isNotEmpty(instrumentNumber)) {
			TSServerInfoModule module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, instrumentNumber);
			module.forceValue(1, "DOCUMENT");
			
			if(recDate != null) {
				module.forceValue(4, dateFormat.format(recDate));
				module.forceValue(5, dateFormat.format(recDate));
			} else {
				module.forceValue(4, sa.getAtribute(SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY));
				module.forceValue(5, sa.getAtribute(SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY));
			}

			return module;
		
		
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			TSServerInfoModule module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(2, "BOOKPAGE");
			
			
			if(recDate != null) {
				module.forceValue(4, dateFormat.format(recDate));
				module.forceValue(5, dateFormat.format(recDate));
			} else {
				module.forceValue(4, sa.getAtribute(SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY));
				module.forceValue(5, sa.getAtribute(SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY));
			}

			return module;
		}

		return null;
	}
	
	@Override
	public void performAdditionalProcessingAfterRunningAutomatic() {
		super.performAdditionalProcessingAfterRunningAutomatic();
		
		Search search = getSearch();
		
		if (isUpdate()){
			SearchAttributes sa = search.getSa(); 	
			
			if (!sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
			
				Set<InstrumentI> allAoRef = getAllAoAndTaxReferences(search);
				if (allAoRef.size() > 0){
				
					SearchLogger.info("\n</div><hr/><div><BR>Run additional searches to get Certification Date. <BR></div>\n", searchId);
					TSServerInfo serverInfo = getCurrentClassServerInfo();
					
					Calendar now = Calendar.getInstance();
					now.add(Calendar.YEAR, -40);
					String fromDate = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(now.getTime());
					
					for(InstrumentI inst : allAoRef){
						try {
							if(inst.hasBookPage()){
								String originalB = inst.getBook();
								String originalP = inst.getPage();
								
								String book = originalB.replaceFirst("^0+", "");
								String page = originalP.replaceFirst("^0+", "");
								
								String county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
								
								if("Comal".equalsIgnoreCase(county)){
									if(originalB.length() == 5 && originalP.length() == 5){
										
										TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
										module.setData(0, originalB+originalP);
										
										module.setData(2, "DOCUMENT");
										module.setData(4, fromDate);
										module.setData(5, sa.getAtribute(SearchAttributes.TODATE));
										
										ServerResponse response = SearchBy(module, null);
									}
								} else{
															
									TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
									module.setData(0, book);
									module.setData(1, page);
									module.setData(2, "BOOKPAGE");
									module.setData(4, fromDate);
									module.setData(5, sa.getAtribute(SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY));
									
									ServerResponse response = SearchBy(module, null);
								}
								
								if (sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
									SearchLogger.info("\n</div><div><BR>Certification Date found!<BR><hr/></div>\n", searchId);
									break;
								}
								
							} else if (inst.hasInstrNo()){
								inst = prepareInstrumentForSearch(inst);
								String instr = inst.getInstno().replaceFirst("^0+", "");
								
								TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
								module.setData(0, instr);
								module.setData(2, "DOCUMENT");
								module.setData(4, fromDate);
								module.setData(5, sa.getAtribute(SearchAttributes.TODATE));
								
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
	
	@Override
	protected int getMaxNoOfDocumentsFromDaslToAnalyze() {
		return Search.MAX_NO_OF_DOCUMENTS_FROM_DASL_TP3_TO_ANALYZE;
	}
}

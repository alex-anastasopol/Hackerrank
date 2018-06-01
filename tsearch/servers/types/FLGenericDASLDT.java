package ro.cst.tsearch.servers.types;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.dasl.DTError;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.functions.CAGenericDT;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.connection.titlepoint.PropertyInsightImageConn;
import com.stewart.ats.connection.titlepoint.PropertyInsightImageConn.ImageDownloadResponse;
import com.stewart.datatree.DataTreeAccount;
import com.stewart.datatree.DataTreeConn;
import com.stewart.datatree.DataTreeImageException;
import com.stewart.datatree.DataTreeManager;
import com.stewart.datatree.DataTreeStruct;

/**
 * @author cristi stochina
 */
public class FLGenericDASLDT extends TSServerDASLAdapter implements TSServerROLikeI {

	transient protected List<DataTreeStruct> datTreeList;
	
	private static final long serialVersionUID = 174434789892L;

	protected static final String DASL_TEMPLATE_CODE = "DASL_DT_FL";
	private static final ServerPersonalData pers = new ServerPersonalData();

	static {
		pers.setTemplatePrefixName(DASL_TEMPLATE_CODE);
		{
			int id = 0;
			pers.addXPath(id, "//Property", ID_SEARCH_BY_ADDRESS);
			pers.addXPath(id, "//TitleDocument", ID_DETAILS);
			pers.addXPath(id, "//TitleDocument", ID_SEARCH_BY_BOOK_AND_PAGE);
			pers.addXPath(id, "//TitleDocument", ID_SEARCH_BY_INSTRUMENT_NO);
		}
	}

	public FLGenericDASLDT(long searchId) {
		super(searchId);
		disableImageFromTheOriginalSite = true;
		datTreeList = initDataTreeStruct();
	}

	public FLGenericDASLDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		disableImageFromTheOriginalSite = true;
		datTreeList = initDataTreeStruct();
	}

	protected List<DataTreeStruct> initDataTreeStruct(){
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				InstanceManager.getManager().getCommunityId(searchId), 
				miServerID);
		return DataTreeManager.getProfileDataUsingStateAndCountyFips(dat.getCountyFIPS(), dat.getStateFIPS());
	}
	
	@Override
	public void specificParseLegalDescription(Document doc, ParsedResponse item, ResultMap resultMap) {
		defaultParseLegalDescription(doc, item, null);
	}
	
	@Override
	public void specificParseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap) {
		//in case of Danger comment the body of this function and un-comment the parsing names statements from /rules/GenericDASLDT.xml
		try {
			NodeList partyInfo = XmlUtils.xpathQuery(doc, "/TitleDocument/Instrument/PartyInfo");
			
			ArrayList<List> bodyGtor = new ArrayList<List>();
			ArrayList<List> bodyGtee = new ArrayList<List>();

			for (int i = 0; i < partyInfo.getLength(); i++) {
				if(partyInfo.item(i) != null ) {
					String[] names = { "", "", "", "", "", "" };
					
					String partyRole = XmlUtils.findNodeValue(partyInfo.item(i), "PartyRole");
					String vestingType = XmlUtils.findNodeValue(partyInfo.item(i), "VestingType");
					String firstName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/FirstName");
					String midName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/MiddleName");
					String lastName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/LastName");
					String fullName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/FullName");
					
					firstName = firstName.replaceAll("\\bDECD\\b", "").replaceAll("\\bIND\\s+EXEC\\b", "").replaceAll("\\s+MRS\\b", "");
					midName = midName.replaceAll("\\bDECD\\b", "").replaceAll("\\bIND\\s+EXEC\\b", "").replaceAll("\\s+MRS\\b", "")
																.replaceAll("(?is)\\A\\s*-\\s*$", "");
					lastName = lastName.replaceAll("\\bDECD\\b", "");
					
					if (i < partyInfo.getLength() - 1){
						String partyRoleNext = XmlUtils.findNodeValue(partyInfo.item(i+1), "PartyRole");
						if (partyRole.toLowerCase().equals(partyRoleNext.toLowerCase())){
							String firstNameNext = XmlUtils.findNodeValue(partyInfo.item(i+1), "Party/FirstName");
							String midNameNext = XmlUtils.findNodeValue(partyInfo.item(i+1), "Party/MiddleName");
							String lastNameNext = XmlUtils.findNodeValue(partyInfo.item(i+1), "Party/LastName");
							String fullNameNext = XmlUtils.findNodeValue(partyInfo.item(i+1), "Party/FullName");
							if (StringUtils.isEmpty(lastNameNext) && StringUtils.isEmpty(midNameNext) && StringUtils.isNotEmpty(firstNameNext) 
									&& StringUtils.isEmpty(fullNameNext)){
								if (firstNameNext.matches("\\A(?:ESTATE )?(?:OF|ETAL?|TRUSTEE)\\s*")){
									firstNameNext = firstNameNext.replaceAll("(?is)\\bETA\\b$", "ETAL");
									midName += " " + firstNameNext;
								} else {
									midName += " & " + firstNameNext;
								}
							} else if (lastNameNext.length() == 1  && StringUtils.isEmpty(midNameNext) && StringUtils.isNotEmpty(firstNameNext)){
								midName += " & " + firstNameNext + " " + lastNameNext;
							}
						}
					}
					
					if (StringUtils.isEmpty(lastName) && StringUtils.isEmpty(midName) && StringUtils.isNotEmpty(firstName) 
							&& firstName.matches("\\A(?:ESTATE )?(?:OF|ETAL?|TRUSTEE|\\bPR\\b)\\s*")){
						continue;
					}
					if (StringUtils.isNotEmpty(lastName) && StringUtils.isEmpty(midName) && StringUtils.isEmpty(firstName) 
							&& lastName.matches("\\A(?:ESTATE )?(?:OF|ETAL?|TRUSTEE|\\bPR\\b)\\s*")){
						continue;
					}
					if (StringUtils.isNotEmpty(lastName) && StringUtils.isEmpty(midName) && StringUtils.isNotEmpty(firstName) 
							&& firstName.matches("\\A(?:ETAL)\\s*")){
						continue;
					}
					
					String[] parts = fullName.split("###");
					if (parts.length > 1){
						for (String part:parts){
							names = StringFormats.parseNameDesotoRO(part, true);
							parseNames(part, names, partyRole, vestingType, bodyGtor, bodyGtee);
						}
					} else {		
						if (StringUtils.isNotEmpty(fullName)){
							names = StringFormats.parseNameNashville(fullName, true);
							parseNames(fullName, names, partyRole, vestingType, bodyGtor, bodyGtee);
							
						} else {
							String name = (lastName + " " + firstName + " " + midName).trim();
							String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
							String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
							if ("FL".equalsIgnoreCase(crtState) && "Alachua".equalsIgnoreCase(crtCounty)) {
								name = name.replaceFirst("^([A-Z]+)\\s+([A-Z]+\\s[A-Z])\\s+([A-Z]+\\s+[A-Z])$", "$1 $2 & $1 $3");//MCGRAW FRANK W MARY J (book 1218, page 754)
							}
							names = StringFormats.parseNameNashville(name, true);
							parseNames(name, names, partyRole, vestingType, bodyGtor, bodyGtee);
						}
					}
				}
			}
			ResultTable rtGtor = new ResultTable();
			ResultTable rtGtee = new ResultTable();
			
			rtGtor = GenericFunctions.storeOwnerInSet(bodyGtor, true);
			resultMap.put("GrantorSet", rtGtor);
			rtGtee = GenericFunctions2.storeOwnerInSet(bodyGtee, true);
			resultMap.put("GranteeSet", rtGtee);
			
			CAGenericDT.fixGrantorGranteeSetDT(resultMap, searchId);
			GenericFunctions2.setGrantorGranteeDT(resultMap, searchId);
			
			try {
				GenericFunctions1.setGranteeLanderTrustee2(resultMap, searchId);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
		} catch (Exception e) {
			logger.error("Exception in parsing of names in FLGenericDASLDT " + searchId, e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void parseNames(String allName, String[] names, String partyRole, String vestingType, ArrayList<List> bodyGtor, ArrayList<List> bodyGtee) {
		
		String[] suffixes, type, otherType;
		
		type = GenericFunctions.extractAllNamesType(names);
		otherType = GenericFunctions.extractAllNamesOtherType(names);
		suffixes = GenericFunctions.extractNameSuffixes(names);
		if (("Grantor".equalsIgnoreCase(partyRole) || "Party1".equalsIgnoreCase(vestingType)) 
				&& (!"Grantee".equalsIgnoreCase(partyRole) || !"Party2".equalsIgnoreCase(vestingType))){
			GenericFunctions.addOwnerNames(allName, names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), bodyGtor);
		} else if ("Grantee".equalsIgnoreCase(partyRole) || "Party2".equalsIgnoreCase(vestingType)){
			GenericFunctions.addOwnerNames(allName, names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), bodyGtee);
		}
		
	}
	
	@Override
	protected HashMap<String, Object> fillTemplatesParameters(Map<String, String> params) {
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		Search search = currentInstance.getCrtSearchContext();
		HashMap<String, Object> templateParams = super.fillTemplatesParameters(params);
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				currentInstance.getCurrentCommunity().getID().intValue(), 
				miServerID);
		String sateName = dat.getName();
		String stateAbrev = sateName.substring(0, 2);

		String APN = params.get("APN");
		templateParams.put(AddDocsTemplates.DASLAPN, APN);
		String chain = params.get("chain");
		templateParams.put(AddDocsTemplates.DASLPropertyChainOption, chain);
		String includeTax = params.get("includeTax");
		templateParams.put(AddDocsTemplates.DASLIncludeTaxFlag, includeTax);

		String role1 = params.get("role1");
		templateParams.put(AddDocsTemplates.DASLPartyRole_1, role1);
		String role2 = params.get("role2");
		templateParams.put(AddDocsTemplates.DASLPartyRole_2, role2);

		String firstName1 = params.get("firstName1");
		
		templateParams.put(AddDocsTemplates.DASLFirstName_1, firstName1);
		String firstName2 = params.get("firstName2");
		templateParams.put(AddDocsTemplates.DASLFirstName_2, firstName2);

		String middleName1 = params.get("middleName1");
		
		templateParams.put(AddDocsTemplates.DASLMiddleName_1, middleName1);
		String middleName2 = params.get("middleName2");
		templateParams.put(AddDocsTemplates.DASLMiddleName_2, middleName2);

		String lastName1 = params.get("lastName1");
		
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

		String parcel = params.get("parcel");
		templateParams.put(AddDocsTemplates.DASLParcel, parcel);
		
		String pcl = params.get("pcl");
		if(StringUtils.isNotEmpty(pcl)){
			if(pcl.length()==11){
				String a = pcl.substring(0,5);
				String b = pcl.substring(5,8);
				String c = pcl.substring(8,11);
				templateParams.put(AddDocsTemplates.DASLPcl, a);
				templateParams.put(AddDocsTemplates.DASLPcl1, b);
				templateParams.put(AddDocsTemplates.DASLPcl2, c);
			}
		}
		
		String quarterOrder = params.get("quarterOrder");
		templateParams.put(AddDocsTemplates.DASLQuarterOrder, quarterOrder);

		String quaterValue = params.get("quarterValue");
		templateParams.put(AddDocsTemplates.DASLQuaterValue, quaterValue);

		String arb = params.get("arb");
		templateParams.put(AddDocsTemplates.DASLARB, arb);

		String reference = search.getOrderNumber();
		if (reference.length() > 15) {
			/* for DT max 15 characters*/
			reference = reference.substring(reference.length() - 15); 
		}

		int commId = search.getCommId();
    	String stateAbv = InstanceManager.getManager().getCurrentInstance(search.getSearchID()).getCurrentState().getStateAbv();
    	
    	if (((commId == 3 || commId == 4 || commId ==10)&&stateAbv.equals("FL"))){
    		reference="test";
    	}
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
		
		templateParams.put(AddDocsTemplates.DASLClientId, SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericDTSite", "DaslClienId"));
	    
		return templateParams;
	}

	@Override
	protected ServerPersonalData getServerPersonalData() {
		return pers;
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
		} else {
			type = DocumentTypes.getDocumentCategory(type, searchId);
		}

		TSServerInfoModule imgModule = getDefaultServerInfoWrapper().getModule(TSServerInfo.IMG_MODULE_IDX);
		StringBuilder build = new StringBuilder("");// <a href=\"
		build.append(createPartialLink(TSConnectionURL.idDASL, TSServerInfo.IMG_MODULE_IDX));
		build.append("DASLIMAGE&");

		build.append(imgModule.getFunction(5/* type */).getParamAlias());
		build.append("=");
		build.append(type);
		build.append("&");

		build.append(imgModule.getFunction(0/* docno */).getParamAlias());
		build.append("=");
		build.append(docno);
		build.append("&");

		build.append(imgModule.getFunction(2/* book */).getParamAlias());
		build.append("=");
		build.append(book);
		build.append("&");

		build.append(imgModule.getFunction(1/* DASLImageSearchType */).getParamAlias());
		build.append("=");
		build.append("DT");
		build.append("&");

		build.append(imgModule.getFunction(3/* page */).getParamAlias());
		build.append("=");
		build.append(page);
		build.append("&");

		build.append(imgModule.getFunction(4/* year */).getParamAlias());
		build.append("=");
		build.append(year);
		
		return build.toString();
	}

	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		Search search = getSearch();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		if(search.getSearchType() == Search.GO_BACK_ONE_LEVEL_SEARCH) {
		
			ConfigurableNameIterator nameIterator = null;
			SearchAttributes sa = search.getSa();	
			
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
		    	if (date!=null) { 
			    	module.getFunction(0).forceValue(date);
		    	}
		    	FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
				((GenericNameFilter)nameFilter).setUseSynonymsBothWays(true);
				nameFilter.setInitAgain(true);
				
				FilterResponse transferNameFilter = NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module);
				((GenericNameFilter)transferNameFilter).setUseSynonymsBothWays(true);
				transferNameFilter.setInitAgain(true);
			     
			    module.addFilter( nameFilter );
				module.addFilter( transferNameFilter );
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
					if (date!=null) {
						module.getFunction(0).forceValue(date);
					}
					
					nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
					((GenericNameFilter)nameFilter).setUseSynonymsBothWays(true);
					nameFilter.setInitAgain(true);
					
					transferNameFilter = NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module);
					((GenericNameFilter)transferNameFilter).setUseSynonymsBothWays(true);
					transferNameFilter.setInitAgain(true);
					
					module.addFilter( nameFilter );
					module.addFilter( transferNameFilter );
					module.addFilter( doctypeFilter );
				    module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
					module.setIteratorType( 2,  FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
					module.setIteratorType( 4,  FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
				    nameIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;" } );
					module.addIterator(nameIterator);			
					modules.add(module);
			    }
		    } 
		}
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}
	
	
	protected static final Logger logger = Logger.getLogger(DataTreeManager.class);
	
	protected static boolean downloadImageFromDataTree(InstrumentI i, List<DataTreeStruct> list, String path , String commId, String month, String day) 
			throws DataTreeImageException{
		
		logger.info("(((((((((( downloadImageFromDataTree ---  instrument="+i+" path="+path+" list="+list);
		
		DataTreeAccount acc = DataTreeManager.getDatatTreeAccount(commId);
		List<DataTreeStruct> toSearch = new ArrayList<DataTreeStruct>(2);
		
		/*if("PLAT".equals(i.getDocType())){
			for(DataTreeStruct struct:list){
				if("ASSESSOR_MAP".equalsIgnoreCase(struct.getDataTreeDocType())){
					toSearch.add(struct);
				}
			}
		}
		else{*/
		for(DataTreeStruct struct:list){
			if("DAILY_DOCUMENT".equalsIgnoreCase(struct.getDataTreeDocType())){
				toSearch.add(struct);
			}
		}
		/*}*/
		
		logger.info("(((((((((( downloadImageFromDataTree ---  instrument="+i+" path="+path+" toSearch="+toSearch);
		
		boolean imageDownloaded = false;
		List<DataTreeImageException> exceptions = new ArrayList<DataTreeImageException>();
		
		for (DataTreeStruct struct : toSearch) {
			try {
				if ((imageDownloaded = DataTreeManager.downloadImageFromDataTree(acc, struct, i, path, month, day))) {
					break;
				}
			} catch (DataTreeImageException e) {
				exceptions.add(e);
			}
		}
		logger.info("(((((((((( downloadImageFromDataTree ---  return =" + imageDownloaded);
		
		if(toSearch.size() == exceptions.size() && !exceptions.isEmpty()) {
			boolean throwException = DataTreeConn.logDataTreeImageException(i, Long.MIN_VALUE, exceptions, true);
			if(throwException) {
				throw exceptions.get(0);
			}
		}
		
		
		return imageDownloaded;
	}
	
	/**
	 * 
	 * @param instr
	 * @param flag -> true to clean instrNo, false to unclean instrNo
	 * @return instrument number 
	 */
	
	protected String cleanInstrNo(InstrumentI instr, boolean clean_flag){
		return instr.getInstno();
	}	
	
	@SuppressWarnings({ "deprecation" })
	protected boolean downloadImageFromRV(InstrumentI i, ImageLinkInPage image){
		return downloadImageFromRV(i,image,true);
	}
	
	@SuppressWarnings({ "deprecation"})
	protected boolean downloadImageFromRV(InstrumentI i, ImageLinkInPage image, boolean searchWithDoctype){
		String county =InstanceManager.getManager().getCurrentInstance(mSearch.getID()).getCurrentCounty().getName();
		TSInterface server = TSServersFactory.GetServerInstance((int)TSServersFactory.getSiteId("FL", county, "RV"), "", "", searchId);
		TSServerInfoModule module = server.getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.IMG_MODULE_IDX, new SearchDataWrapper());
		server.setServerForTsd(mSearch, msSiteRealPath);
		  
		if(StringUtils.isEmpty(i.getBook())||StringUtils.isEmpty(i.getPage())){
			module.setData(2, cleanInstrNo(i,true)); 
		} else{
			module.setData(0,i.getBook());
			module.setData(1,i.getPage()); 
		}
		if(searchWithDoctype) {
			module.setData(4,i.getDocType());
		}
		ServerResponse res=null; 
		try{
			res = server.SearchBy(module, image); }
		catch(Exception e){ e.printStackTrace(); } 
		if( res==null ){ 
			return false; 
		}
		if( FileUtils.existPath(image.getPath())){ 
			return true;
		}
		return false;
	}
	
	public static ImageDownloadResponse downloadImageFromPropertyInsight( String imageFilePath,  String query, long searchId) throws IOException{
		
    	PropertyInsightImageConn conn = new PropertyInsightImageConn("http://titlepoint.com/TitlePointServices/TpsImage.asmx", searchId);
		byte imageBytes[] = null;
		
    	
		ImageDownloadResponse resp = conn.getDocumentsByParameters2( query );
		
		if(resp==null){
			return new ImageDownloadResponse();
		}
		
		imageBytes = resp.imageContent;
			
    	if(imageBytes!=null && imageBytes.length>0){
    		
    		//image was downloaded -> mark this as soon as possible
    		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    		search.countNewImage(GWTDataSite.PI_TYPE);
    		
			org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(imageFilePath), imageBytes);
		}
    	
    	File file = new File(imageFilePath);
    	resp.success =  (file.exists() && !file.isDirectory());
    	
    	return resp;
	}
	
	@SuppressWarnings("deprecation")
	protected boolean downloadImageFromOtherSite(String instrument, String book, String page, String year, String type, ImageLinkInPage image) {
		
		logger.info("******** downloadImageFromOtherSite ---  instrument="+instrument+" book="+book+" page="+page+" year="+year+" type="+type+" image="+image);
		
		if(mSearch == null){
			mSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		}
				
		InstrumentI i = new Instrument();
		int yearI = -1;
		try{yearI = Integer.parseInt(year);}catch(Exception e){}
		i.setYear(yearI);
		i.setBook(book);
		i.setPage(page);
		i.setInstno(instrument);
		i.setDocType(type);
		
		String path = image.getPath();
		
		if(StringUtils.isEmpty(path)){
			
			String name= "i_"+instrument+"b_"+book+"p_"+page+"y_"+year+"t_"+type+".tiff";
			name = mSearch.getImageDirectory() + name;
			image.setPath(name);
			image.setContentType("IMAGE/TIFF");
			if ((new File(name)).exists()){
				return true;
			}
		} else {
			
			if( FileUtils.existPath(image.getPath()) ){
				return true;
			}
			
			try {
				int lastIndexOfSeparator = path.lastIndexOf(File.separator);
				int lastIndexOfExtension = path.lastIndexOf(".");
				if(lastIndexOfSeparator > 0 && lastIndexOfExtension > lastIndexOfSeparator) {
					String lastPath = path.substring(lastIndexOfSeparator  + File.separator.length(), lastIndexOfExtension);
					if(lastPath.matches("[\\d_]+")) {
						
						String name= "i_"+instrument+"b_"+book+"p_"+page+"y_"+year+"t_"+type+".tiff";
						name = mSearch.getImageDirectory() + name;
						
						File tempFile = new File(name);
						if(tempFile.exists()) {
							File newFile = new File(image.getPath());
							org.apache.commons.io.FileUtils.copyFile(tempFile, newFile);
							if(newFile.exists()) {
								return true;
							}
						}
						
					}
				}
			} catch (Exception e) {
				logger.error("Could not optimize loading image for path " + image.getPath(), e);
			}
		}
		
		boolean result = downloadImageFromOtherSiteImpl(i,image, String.valueOf(getCommunityId()));
		return result;
	}
	
	public static String getBasePiQuery(long searchId){
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		
		ro.cst.tsearch.servers.parentsite.State  state = currentInstance.getCurrentState();
		ro.cst.tsearch.servers.parentsite.County county = currentInstance.getCurrentCounty();
    	
    	String stateFIPS = String.valueOf(state.getStateFips());
    	String countyFips = String.valueOf(county.getCountyFips());
    	
    	countyFips = "000".substring(countyFips.length())+countyFips;
    	stateFIPS = "00".substring(stateFIPS.length())+stateFIPS;
    	
    	return "FIPS="+stateFIPS+countyFips;
	}
	
	public static String getPiQuery(InstrumentI i, long searchId){
		
		String query = getBasePiQuery(searchId);
    	
    	if( StringUtils.isNotEmpty(i.getBook())&& StringUtils.isNotEmpty(i.getPage())){
    		if(DocumentTypes.PLAT.equalsIgnoreCase(i.getDocType())){
    			query += ",Type=Map,SubType=All,Book="+i.getBook()+","+"Page="+i.getPage();
    		}
    		else{
    			query += ",Type=Rec,SubType=All,Book="+i.getBook()+","+"Page="+i.getPage();
    		}
    	}
    	else if( StringUtils.isNotEmpty(i.getInstno()) ){
    		if(DocumentTypes.PLAT.equalsIgnoreCase(i.getDocType())){
    			query += ",Type=Map,SubType=Assessor,Inst="+i.getInstno()+",Year="+i.getYear();
    		}
    		else{
    			query += ",Type=Rec,SubType=All,Inst="+i.getInstno()+",Year="+i.getYear();
    		}
    	}
		
		return query;
	}
	
	protected boolean downloadImageFromOtherSiteImpl(InstrumentI instrument, ImageLinkInPage image, String commId) {
		boolean ret = false;
		
		if( FileUtils.existPath(image.getPath()) ){
			return true;
		}
		
		for(int i=0;i<2&&!ret;i++){
			try {
				if(ret = downloadImageFromPropertyInsight(  image.getPath(),getPiQuery(instrument,searchId), searchId).success ){
					SearchLogger.info("<br/>Image(searchId="+searchId+" )book="+
							instrument.getBook()+"page="+
							instrument.getPage()+"inst="+
							instrument.getInstno()+" was taken from PropertyInsight<br/>", searchId);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(!ret){
			if(datTreeList==null){
				datTreeList = initDataTreeStruct();
			}
			
			try {
				ret= downloadImageFromDataTree(instrument, datTreeList, image.getPath(),commId, null, null);
				if(ret) {
					SearchLogger.info("<br/>Image(searchId="+searchId+" )book="+
							instrument.getBook()+"page="+
							instrument.getPage()+"inst="+
							instrument.getInstno()+" was taken from DataTree<br/>", searchId);
				}
			} catch (DataTreeImageException e) {
				logger.error("Error while getting image ", e);
				SearchLogger.info(
						"<br/>FAILED to take Image(searchId="+searchId+" ) book=" +
						instrument.getBook()+" page="+instrument.getPage()+" inst="+
						instrument.getInstno()+" from DataTree. "+
						"Official Server Message: [" + e.getLocalizedMessage() + " (" + e.getStatus() + ") ]<br/>", searchId);
			}
			afterDownloadImage(ret);
		}
		
		
		
		return ret;
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
		int functionBook = 2;
		int functionPage = 3;
		int functionDocNo = 0;
		int fuctionDASLSearchType = 1;
		int functionType = 5;
		int functionYear = 4;
		
		TSServerInfo info = getDefaultServerInfo();
		TSServerInfoModule module = info.getModule(TSServerInfo.IMG_MODULE_IDX);
		String yearAlias = module.getFunction(functionYear).getParamAlias();
		String bookAlias = module.getFunction(functionBook/* Book */).getParamAlias();
		String pageAlias = module.getFunction(functionPage/* Page */).getParamAlias();
		String docNumberAlias = module.getFunction(functionDocNo/* DocNumber */).getParamAlias();
		
		String book = "";
		String page = "";
		String docNumber = "";
		String year ="";
		
		String link = image.getLink();
		int poz = link.indexOf("?");
		
		if(poz>0){
			link = link.substring(poz+1);
		}
		
		String[] allParameters = link.split("[&=]");
		
		for(int i=0;i<allParameters.length-1;i+=2){
			if(yearAlias.equalsIgnoreCase(allParameters[i])){
				year = allParameters[i+1];
			}
			else if(bookAlias.equalsIgnoreCase(allParameters[i])){
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
		module.getFunction(functionYear).setParamValue(year);
		module.getFunction(fuctionDASLSearchType).setParamValue("DT");
		String typeAlias = module.getFunction(functionType/* Book */).getParamAlias();
		String type = link.replaceAll(".*" + typeAlias + "=([^&]*)", "$1");
		poz = type.indexOf("&");
		if (poz > 0) {
			type = type.substring(0, poz);
		}
		module.getFunction(functionType).setParamValue(type);
		
		String imageName = image.getPath();
    	if(FileUtils.existPath(imageName)){
    		byte b[] = FileUtils.readBinaryFile(imageName);
    		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
    	}
		
		return searchBy(module, image, null).getImageResult();
	}

	public static String updateXMLResponse(String xml, int moduleIDX) {
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		Calendar cal =Calendar.getInstance();
		Date start = cal.getTime();
		
		if (moduleIDX != TSServerInfo.IMG_MODULE_IDX) {
			
			
			if(moduleIDX == TSServerInfo.RELATED_MODULE_IDX) {
				Pattern bpPattern = Pattern.compile("(?is)<BookNumber>\\s*(\\d+)\\s*</BookNumber>\\s*<PageNumber>\\s*(\\d+)\\s*</PageNumber>");
				Matcher bpMatcher = bpPattern.matcher(xml);
				if(bpMatcher.find()) {
					//Mihai's Version
					// (?is)(<BookNumber>([^<]*)</BookNumber>\s*<PageNumber>([^<]*)</PageNumber>.*?)<Book>\2</Book>\s*<Page>\3</Page>(.*?</Instrument>)\s*<RelatedInstruments>(.*?)</RelatedInstruments>
					xml = xml.replaceAll("(?is)<Book>\\s*" + bpMatcher.group(1) + "\\s*</Book>\\s*<Page>\\s*" + bpMatcher.group(2) + "\\s*</Page>", "");
					
					xml = xml.replaceAll("(?is)</Instrument>\\s*<RelatedInstruments>", "");
					xml = xml.replaceAll("</RelatedInstruments>", "</Instrument>");
				} else {
					Pattern instrPattern = Pattern.compile("(?is)<DocumentNumber>\\s*(\\d+)\\s*</DocumentNumber>\\s*<DocumentYear>\\s*(\\d+)\\s*</DocumentYear>") ;
					Matcher instrMatcher = instrPattern.matcher(xml);
					if(instrMatcher.find()) {
						xml = xml.replaceAll("(?is)<DocumentNumber>\\s*" + instrMatcher.group(1) + "\\s*</DocumentNumber>\\s*<DocumentYear>\\s*" + instrMatcher.group(2) + "\\s*</DocumentYear>", "");
						
						xml = xml.replaceAll("(?is)</Instrument>\\s*<RelatedInstruments>", "");
						xml = xml.replaceAll("</RelatedInstruments>", "</Instrument>");
					}
				}
			}
			
			
			String tr[] = xml.split("[<][/]?[ ]*TitleRecord[ ]*[>]");
			// daslResponse.xmlResponse=tr[0];
			StringBuilder xmlResponseBuild = new StringBuilder(tr[0]);

			for (int i = 1; i < tr.length - 1; i++) {

				String tt[] = tr[i].split("[<][/]?[ ]*TitleDocument[ ]*[>]");

				String titlerecord = "";
				String temp = "";
				if ( !StringUtils.isEmpty((temp = getUtilForDataTrace(tt[0]))) ) {
					int size = tt.length - 2;
					if (tt[tt.length - 1].contains("Instrument")) {
						size = tt.length - 1;
					}
					for (int j = 0; j <= size; j++) {
						titlerecord = "<TitleDocument>" + tt[j] + temp + "</TitleDocument>";
					}
				} else {
					titlerecord = tr[i];
				}
				if (!StringUtils.isEmpty(tr[i]) && !tr[i].contains("TitleSearchReport")) {
					xmlResponseBuild.append("<TitleRecord>");
					xmlResponseBuild.append(titlerecord);
					xmlResponseBuild.append("</TitleRecord>");
				} else if (tr[i].contains("TitleSearchReport")) { // this is a tag upper then TitleRecord
					xmlResponseBuild.append(tr[i]);
				}
			}
			if (tr.length > 1) {
				xmlResponseBuild.append(tr[tr.length - 1]);
			}
			xml = xmlResponseBuild.toString();

		}
		xml = xml.replaceAll("<TitleRecord>\\s*(</TitleData>\\s*<TitleData>)\\s*</TitleRecord>", "$1");//B 3007
		xml = xml.replaceAll("<TitleRecord>\\s*(</TitleData>\\s*<PropertyChainData>\\s*<PropertyChainTitleReport>)\\s*</TitleRecord>", "$1");//B 5698
		xml = xml.replaceAll("</TitleComment>\\s*(</TitleData>\\s*<PropertyChainData>\\s*<PropertyChainTitleReport>)\\s*</TitleRecord>", "</TitleComment></TitleRecord>$1");
		
		xml = xml.replaceAll("<TitleData>\\s*</TitleRecord>\\s*<TitleRecord>","<TitleData><TitleRecord>");
		xml = xml.replaceAll("</TitleRecord>\\s*<TitleRecord>\\s*</TitleData>", "</TitleRecord></TitleData>");

		xml = xml.replace("<Date><Date>00/00/0000</Date>", "<Date><Date>"+format.format(start)+"</Date>");
		xml = xml.replace("<RecordedDate><Date>00/00/0000</Date>", "<RecordedDate><Date>"+format.format(start)+"</Date>");
		xml = xml.replace("<PostedDate><Date>00/00/0000</Date>", "<PostedDate><Date>"+format.format(start)+"</Date>");
		
		return xml;
	}
	
	@Override
	protected String modifyXMLResponse(String xml, int moduleIDX) {
		return updateXMLResponse(xml, moduleIDX);
	}

	private static String getUtilForDataTrace(String str) {

		int start = str.indexOf("<ParentDocumentNumber>");
		int stop = str.indexOf("</ParentDocumentNumber>");

		String temp = "";

		if (start >= 0 && stop >= 0 && start < stop) {
			temp = str.substring(start, stop + "</ParentDocumentNumber>".length());
		}

		start = str.indexOf("<ParentBookPage>");
		stop = str.indexOf("</ParentBookPage>");

		if (start >= 0 && stop >= 0 && start < stop) {
			if (temp == null) {
				temp = str.substring(start, stop + "</ParentBookPage>".length());
			} else {
				temp = temp + str.substring(start, stop + "</ParentBookPage>".length());
			}
		}

		Pattern pat = Pattern.compile("<Date>[ \n\t\r]*<Date>([^>]+)</Date>[ \n\t\r]*</Date>");
		
		java.util.regex.Matcher mat = pat.matcher(str);
		
		if(mat.find()){
			String dateStr = mat.group(1);
			Date date = Util.dateParser3(dateStr);
			if(date!=null){
				temp+= "<RecordedDate><Date>"+dateStr+"</Date></RecordedDate>";
			}
		}
		
		return temp;
	}

	protected ArrayList<NameI>  addNameSearch( List<TSServerInfoModule> modules, TSServerInfo serverInfo,String key, ArrayList<NameI> searchedNames, List<FilterResponse> filters ) {
		Search global 		= getSearch();
		
		SearchAttributes sa = global.getSa();
		ConfigurableNameIterator nameIterator = null;
		
		TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
		module.clearSaKeys();
		module.setSaObjKey(key);

		for (int i = 0; i < filters.size(); i++) {
			if(filters.get(i)!=null){
				module.addFilter(filters.get(i));
			}
		}
		addBetweenDateTest(module, false, true, true);
		
		addFilterForUpdate(module, true);
		
		module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		module.forceValue(5, "NICKNAME");
		
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }, 25);
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
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
			
		TSServerInfoModule module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		module.forceValue(0, book);
		module.forceValue(1, page);
		module.forceValue(2,"B_P");
		
		return module;
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		TSServerInfoModule imgModule = getDefaultServerInfo().getModule(TSServerInfo.IMG_MODULE_IDX);
		imgModule.forceValue(0, 
				org.apache.commons.lang.StringUtils.defaultIfEmpty(
						document.getDocumentNumber(), 
						document.getInstrumentNumber()));
		imgModule.forceValue(1, "DT");
		imgModule.forceValue(2, org.apache.commons.lang.StringUtils.defaultString(document.getBook()));
		imgModule.forceValue(3, org.apache.commons.lang.StringUtils.defaultString(document.getPage()));
		imgModule.forceValue(4, Integer.toString(document.getYear()));
		imgModule.forceValue(5, org.apache.commons.lang.StringUtils.defaultString(document.getCategory()));
		return imgModule;
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
	protected String parseErrorMessage(String message) {
		StringBuilder errorMessage = new StringBuilder();
		String[] lines = message.split("\n");
		
		// default first line
		errorMessage.append("Provider error!<br><ul>");
		
		for(String line : lines) {
			if(line.contains("The search failed")) { // first line
				// replace first line with this one
				errorMessage.setLength(0);
				errorMessage.append(line.replaceFirst(".*The search failed", "The search failed"));
				errorMessage.append("<ul>");
			} else if(line.matches("[A-Z]{2}\\d{2}\\s+[A-Z]\\s+.*")) {
				String errorCode = line.replaceFirst("([A-Z]{2}\\d{2})\\s+.*", "$1");
				DTError error = DBManager.getDTErrorForCode(errorCode);
				// if an alternate message is available, use it
				if(error != null && !StringUtils.isEmpty(error.getAlternate_message())) {
					errorMessage.append("<li>" + error.getAlternate_message() + " (error code " + 
							error.getErrorCode() + ")</li>");
				} else {
					String[] parts = line.split("\\s+", 3);
					if(parts.length == 3) {
						errorMessage.append("<li>" + parts[2] + " (error code " + 
								parts[0] + ")</li>");
					} else {
						errorMessage.append("<li>" + line + "</li>");
					}
				}
			} else {
				errorMessage.append("<li>" + line + "</li>");
			}
		}
		
		errorMessage.append("</ul>");
		
		return errorMessage.toString();
	}
	
	@Override
	protected int[] getModuleIdsForSavingLegal() {
		return new int[]{
				TSServerInfo.SUBDIVISION_MODULE_IDX,
				TSServerInfo.ADDRESS_MODULE_IDX,
				TSServerInfo.SECTION_LAND_MODULE_IDX,
				TSServerInfo.ARB_MODULE_IDX
		};
	}
	
	@Override
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		LegalI legal = null;
		
		legal = super.getLegalFromModule(module);
		if(legal != null) {
			return legal;
		}
		
		if(module.getModuleIdx() == TSServerInfo.SECTION_LAND_MODULE_IDX) {
			TownShipI townShip = new TownShip();
			townShip.setSection(module.getFunction(0).getParamValue().trim());
			townShip.setTownship(module.getFunction(1).getParamValue().trim());
			townShip.setRange(module.getFunction(2).getParamValue().trim());
			try {
				townShip.setQuarterOrder(Integer.parseInt(module.getFunction(3).getParamValue().trim()));
			} catch (Exception e) {
			}
			townShip.setQuarterValue(module.getFunction(4).getParamValue().trim());
			
			legal = new Legal();
			legal.setTownShip(townShip);
		}
		
		if(module.getModuleIdx() == TSServerInfo.ARB_MODULE_IDX) {
			TownShipI townShip = new TownShip();
			townShip.setSection(module.getFunction(0).getParamValue().trim());
			townShip.setTownship(module.getFunction(1).getParamValue().trim());
			townShip.setRange(module.getFunction(2).getParamValue().trim());
			try {
				townShip.setQuarterOrder(Integer.parseInt(module.getFunction(3).getParamValue().trim()));
			} catch (Exception e) {
			}
			townShip.setQuarterValue(module.getFunction(4).getParamValue().trim());
			townShip.setArb(module.getFunction(5).getParamValue().trim());
			
			legal = new Legal();
			legal.setTownShip(townShip);
		}
		
		return legal;
	}
}

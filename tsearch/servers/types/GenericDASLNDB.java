package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.templates.InstrumentStruct;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;

/**
 * @author cristi stochina
 */
@SuppressWarnings("deprecation")
public class GenericDASLNDB extends TSServerDASLAdapter {
	
	protected  static final String DASL_TEMPLATE_CODE  = "DASL_NDB";
	static ServerPersonalData personalData = new ServerPersonalData();
	static{
		personalData.setTemplatePrefixName(DASL_TEMPLATE_CODE);
		personalData.addXPath( 0 , "//PropertySummary/Property", ID_DETAILS );
		
		personalData.setTemplatePrefixName(DASL_TEMPLATE_CODE);
		personalData.addXPath( 1 , "//TitleRecord/TitleDocument", ID_DETAILS );
	}
	
	private static final long serialVersionUID = 74565133426610710L;
	public GenericDASLNDB(long searchId) {
		super(searchId);
		resultType = UNIQUE_RESULT_TYPE;
	}

	public GenericDASLNDB(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = UNIQUE_RESULT_TYPE;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();
		
		
		for (int moduleIndex : new int[]{
				TSServerInfo.NAME_MODULE_IDX,
				TSServerInfo.ADDRESS_MODULE_IDX,
				TSServerInfo.PARCEL_ID_MODULE_IDX, 
				TSServerInfo.DASL_GENERAL_SEARCH_MODULE_IDX}) {
		
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
		
		return msiServerInfoDefault;
	}

	
	protected  HashMap<String, Object> fillTemplatesParameters (Map<String, String> params){
		DataSite data = HashCountyToIndex.getDateSiteForMIServerID(
				InstanceManager.getManager().getCommunityId(searchId),
				miServerID);
		HashMap <String, Object> templateParams = super.fillTemplatesParameters(params);
		templateParams.put( AddDocsTemplates.DASLStreetName, params.get("StreetName") );
		templateParams.put( AddDocsTemplates.DASLStreetNumber, params.get("StreetNumber") );
		templateParams.put( AddDocsTemplates.DASLStreetSuffix, params.get("StreetSuffix") );
		templateParams.put( AddDocsTemplates.DASLLastName, params.get("LastName") );
		templateParams.put( AddDocsTemplates.DASLMiddleName, params.get("MiddleName") );
		templateParams.put( AddDocsTemplates.DASLFirstName, params.get("FirstName") );
		templateParams.put( AddDocsTemplates.DASLID, params.get("ID") );
		templateParams.put( AddDocsTemplates.DASLCountyFIPS, data.getCountyFIPS());
		templateParams.put( AddDocsTemplates.DASLStateFIPS, data.getStateFIPS());
		templateParams.put( AddDocsTemplates.DASLParcelId,params.get("parcelId")) ;
		templateParams.put( AddDocsTemplates.DASLSearchType,params.get("DASLSearchType")) ;
		templateParams.put( AddDocsTemplates.DASLStreetDirection, params.get("StreetDirection"));
		templateParams.put( AddDocsTemplates.DASLStreetPostDirection, params.get("StreetPostDirection")) ;
		templateParams.put( AddDocsTemplates.DASLStreetFraction,params.get("StreetFraction")) ;
		templateParams.put( AddDocsTemplates.DASLAddressUnitValue,params.get("AddressUnitValue")) ;
		templateParams.put(AddDocsTemplates.DASLClientId, SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericNDBSite", "DaslClienId"));
		return templateParams;
	}

	@Override
	protected ServerPersonalData getServerPersonalData() {
		return personalData;
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		FilterResponse adressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true, true);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module );
		FilterResponse nameFilterHybridDoNotSkipUnique = null;
		FilterResponse cityFilter = CityFilterFactory.getCityFilterDefault(searchId);
		FilterResponse multiplePINFilter = new MultiplePinFilterResponse(searchId);
		
		State state = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState();
		String stateAbrev = state.getStateAbv();
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (Search.AUTOMATIC_SEARCH == searchType){
			// P0 - search by multiple PINs
			if ("IL".equals(stateAbrev)){
				Collection<String> pins = getSearchAttributes().getPins(-1);
				if(pins.size() > 1){			
					for(String pin: pins){
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
						module.clearSaKeys();
						module.getFunction(0).forceValue(pin);
						modules.add(module);	
					}			
					// set list for automatic search 
					serverInfo.setModulesForAutoSearch(modules);
					resultType = MULTIPLE_RESULT_TYPE;
					return;
				}
			}
		}
				
		// P1 : search by PIN
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_NDB);	
		if(!StringUtils.isEmpty(pin)){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setData( 0 , pin );  
			modules.add(module);		
		}
		if( pin.indexOf( "-" ) > 0 || pin.indexOf(".")>0 || pin.indexOf("/")>0){
			pin = pin.replaceAll( "[-./]", "" );
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setData( 0 , pin );  
			modules.add(module);
		}
		String strNo   = getSearchAttribute(SearchAttributes.P_STREETNO);
		String strSuf  = getSearchAttribute(SearchAttributes.P_STREETSUFIX);
		String strUnit = getSearchAttribute(SearchAttributes.P_STREETUNIT_CLEANED);
		String strPostDirection = getSearchAttribute(SearchAttributes.P_STREET_POST_DIRECTION);
		
		// construct the list of street names
		String tmpName = getSearchAttribute(SearchAttributes.P_STREETNAME).trim();
		Set<String> strNames = new LinkedHashSet<String>(); 
		if(!StringUtils.isEmpty(tmpName)){
			strNames.add(tmpName);
		}
		
		//we have cases when they put "." in the name of the street St.Jhons
		tmpName = tmpName.replace(".", " ").replaceAll("\\s{2,}", " ").trim();
		if(!StringUtils.isEmpty(tmpName)){
			strNames.add(tmpName);
		}		
		
		// P2: search by address
		if(!StringUtils.isEmpty(strNo))	{
			for(String strName: strNames){
				
				// search with unit if present
				if(!StringUtils.isEmpty(strUnit)){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.clearSaKeys();
					module.setData( 0 , strNo );
					module.setData( 1 , strName );
					module.setData( 6 , strUnit );
					module.addFilter( adressFilter );
					module.addFilter( cityFilter );
					module.addFilter( nameFilterHybrid );
					if ("IL".equals(stateAbrev)){
						module.addFilter(multiplePINFilter);
					}
					module.addFilter( LegalFilterFactory.getDefaultLegalFilter(searchId));
					modules.add(module);
				}
				
				// search with suffix if present
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.clearSaKeys();
				module.setData( 0 , strNo );
				module.setData( 1 , strName );
				module.setData( 2 , strSuf );
				module.addFilter( adressFilter );
				module.addFilter( cityFilter );
				module.addFilter( nameFilterHybrid );
				if ("IL".equals(stateAbrev)){
					module.addFilter(multiplePINFilter);
				}
				module.addFilter( LegalFilterFactory.getDefaultLegalFilter(searchId));
				modules.add(module);
				
				// search without suffix
				if(!StringUtils.isEmpty(strSuf)){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.clearSaKeys();
					module.setData( 0 , strNo );
					module.setData( 1 , strName );
					module.addFilter( adressFilter );
					module.addFilter( cityFilter );
					module.addFilter( nameFilterHybrid );
					if ("IL".equals(stateAbrev)){
						module.addFilter(multiplePINFilter);
					}
					module.addFilter( LegalFilterFactory.getDefaultLegalFilter(searchId));
					modules.add(module);				
				}
				
				
				// search with post direction
				if(!StringUtils.isEmpty(strPostDirection)){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.clearSaKeys();
					module.setData( 0 , strNo );
					module.setData( 1 , strName );
					module.setData( 5 , strPostDirection);
					module.addFilter( adressFilter );
					module.addFilter( cityFilter );
					module.addFilter( nameFilterHybrid );
					if ("IL".equals(stateAbrev)){
						module.addFilter(multiplePINFilter);
					}
					module.addFilter( LegalFilterFactory.getDefaultLegalFilter(searchId));
					modules.add(module);				
				}
				
				if(!(StringUtils.isEmpty(strNo)&&StringUtils.isEmpty(strSuf))){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.clearSaKeys();
					module.setData( 1 , strName );
					module.addFilter( adressFilter );
					module.addFilter( cityFilter );
					module.addFilter( nameFilterHybrid );
					if ("IL".equals(stateAbrev)){
						module.addFilter(multiplePINFilter);
					}
					module.addFilter( LegalFilterFactory.getDefaultLegalFilter(searchId));
					modules.add(module);	
				}
				
				// eliminate direction from street name
				String DIR = "NORTH|SOUTH|EAST|WEST|N|S|E|W|NORTHEAST|NORTHWEST|SOUTHEAST|SOUTHWEST|NE|NW|SE|SW";			
				String strName1 = strName.toUpperCase().replaceFirst("^(" + DIR + ")\\s(.+)", "$2");
				if(!strName.equalsIgnoreCase(strName1)){
					module =  new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.clearSaKeys();
					module.setData( 0 , strNo );
					module.setData( 1 , strName1 );
					module.setData( 6 , strUnit );
					module.addFilter( adressFilter );
					module.addFilter( cityFilter );
					module.addFilter( nameFilterHybrid );
					if ("IL".equals(stateAbrev)){
						module.addFilter(multiplePINFilter);
					}
					module.addFilter( LegalFilterFactory.getDefaultLegalFilter(searchId));
					modules.add(module);				
				}
				
				//take the last word and use it as suffix
				String []names = strName.split("\\s+");
				if(names.length>=2){
					if(Normalize.isSuffix(names[names.length-1])
							||Normalize.isSpecialSuffix(names[names.length-1])
							||Normalize.isIstateSuffix(names[names.length-1])){
						
						StringBuilder newStreetName = new StringBuilder(names[0]);
						newStreetName.append(" "); 
						for(int i=1;i<names.length-1;i++){
							newStreetName.append(names[i]);
							newStreetName.append(" "); 
						} 
						
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
						module.clearSaKeys();
						module.setData( 0 , strNo );
						module.setData( 1 , newStreetName.toString().trim() );
						module.setData( 2 , names[names.length-1] );
						module.setData( 6 , strUnit );
						module.addFilter( adressFilter );
						module.addFilter( cityFilter );
						module.addFilter( nameFilterHybrid );
						if ("IL".equals(stateAbrev)){
							module.addFilter(multiplePINFilter);
						}
						module.addFilter( LegalFilterFactory.getDefaultLegalFilter(searchId));
						modules.add(module);	
						
						//without suffix
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
						module.clearSaKeys();
						module.setData( 0 , strNo );
						module.setData( 1 , newStreetName.toString().trim() );
						//module.setData( 2 , names[names.length-1] );
						module.setData( 6 , strUnit );
						module.addFilter( adressFilter );
						module.addFilter( cityFilter );
						module.addFilter( nameFilterHybrid );
						if ("IL".equals(stateAbrev)){
							module.addFilter(multiplePINFilter);
						}
						module.addFilter( LegalFilterFactory.getDefaultLegalFilter(searchId));
						modules.add(module);	
					}
				}
				
				//keep only the first word from the street name
				int idx = strName.indexOf(" ");
				String strName2 = strName;
				if(idx > 5){
					strName2 = strName.substring(0, idx);
				}
				if(!strName.equalsIgnoreCase(strName2)){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.clearSaKeys();
					module.setData( 0 , strNo );
					module.setData( 1 , strName2 );
					module.setData( 6 , strUnit );
					module.addFilter( adressFilter );
					module.addFilter( cityFilter );
					module.addFilter( nameFilterHybrid );
					if ("IL".equals(stateAbrev)){
						module.addFilter(multiplePINFilter);
					}
					module.addFilter( LegalFilterFactory.getDefaultLegalFilter(searchId));
					modules.add(module);				
				}
			}
		}
		
		// P3: Search by owners
		if( hasOwner()){ 
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();			
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			if( strNames.size()>0 ){
				module.addFilter( adressFilter );
			}
			module.addFilter( LegalFilterFactory.getDefaultLegalFilter(searchId));
			module.addFilter( nameFilterHybridDoNotSkipUnique );
			
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;M","L;F;","L;M;"})
					);
			
			modules.add(module);	
		}
		
		// set list for automatic search 
		serverInfo.setModulesForAutoSearch( modules );
	}	
	

	private boolean addInstrumentToList(SaleDataSet set, Vector<SaleDataSet> tempVector, Search global, List<InstrumentStruct> list, ParsedResponse originalParseResponce, ParsedResponse transHistParseResponse){
		boolean isTransfer = false;
		if(set!=null){
			if( notDuplicate(tempVector,set) ){
				tempVector.add(set);
				if(transHistParseResponse!=null){
					for(RegisterDocumentI regDoc:transHistParseResponse.getDocument().getReferences()){
						originalParseResponce.getDocument().addReference(regDoc);
						originalParseResponce.getDocument().addParsedReference(regDoc.getInstrument());
					}
				}
				
				String oldListInstr = global.getSa().getAtribute(SearchAttributes.LD_INSTRNO);
				String newListInst = oldListInstr;
				
				String oldListBookPage = global.getSa().getAtribute(SearchAttributes.LD_BOOKPAGE);
				String newListBookPage = oldListBookPage;
				
				String book = set.getAtribute("Book");
				String page = set.getAtribute("Page");
				String docNo = set.getAtribute("InstrumentNumber");
				
				
				String docType = set.getAtribute("DocumentType");
				docType = (docType == null) ?"":docType ;

				docType =  DocumentTypes.getDocumentCategory(docType, searchId);
				
				if("TRANSFER".equals(docType )){
					isTransfer = true;
				}
				
				if(StringUtils.isEmpty(book)&&StringUtils.isEmpty(page) ){
					if(!StringUtils.isEmpty(docNo )){
						if (HashCountyToIndex.isLegalBootstrapEnabled(global.getCommId(), miServerID)) {
							if(StringUtils.isNotEmpty(newListInst)) {
								newListInst += "," + docNo;
							} else {
								newListInst = docNo;
							}
						}
						InstrumentStruct inst = new InstrumentStruct();
						inst.instNo = docNo;
						inst.doctype = docType;
						list.add(inst);
					}
				}
				else{
					if (HashCountyToIndex.isLegalBootstrapEnabled(global.getCommId(), miServerID)) { 
						if(StringUtils.isNotEmpty(newListInst)) {
							newListBookPage += "," + book+"-"+page;
						} else {
							newListBookPage = book+"-"+page;
						}
					}
					
					InstrumentStruct inst = new InstrumentStruct();
					inst.book = book;
					inst.page = page;
					inst.doctype = docType;
					list.add(inst);	
				}
				
				String financeBook = set.getAtribute("FinanceBook");
				String financePage = set.getAtribute("FinancePage");
				String financeDocNo = set.getAtribute("FinanceInstrumentNumber");
				
				
				if(StringUtils.isEmpty(financeBook)&&StringUtils.isEmpty(financePage) ){
					if(!StringUtils.isEmpty(financeDocNo )){
						if (HashCountyToIndex.isLegalBootstrapEnabled(global.getCommId(), miServerID)) {
							if(StringUtils.isNotEmpty(newListInst)) {
								newListInst += "," + financeDocNo;
							} else {
								newListInst = financeDocNo;
							}
						}
						InstrumentStruct inst = new InstrumentStruct();
						inst.instNo = financeDocNo;
						inst.doctype = docType;
						list.add(inst);
					}
				}
				else{
					if (HashCountyToIndex.isLegalBootstrapEnabled(global.getCommId(), miServerID)) { 
						if(StringUtils.isNotEmpty(newListBookPage)) {
							newListBookPage += "," + financeBook+"-"+financePage;
						} else {
							newListBookPage = financeBook+"-"+financePage;
						}
					}
					InstrumentStruct inst = new InstrumentStruct();
					inst.book = financeBook;
					inst.page = financePage;
					inst.doctype = docType;
					list.add(inst);	
				}
				
				global.getSa().setAtribute(SearchAttributes.LD_INSTRNO, newListInst);
				global.getSa().setAtribute(SearchAttributes.LD_BOOKPAGE, newListBookPage);
				
			}
		}
		return isTransfer;
	}
	
	boolean saleTransferIsReliable(){
		String county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String state = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
		
		if( "TX".equalsIgnoreCase(state) ){
			if( "Comal".equalsIgnoreCase(county) || "Nueces".equalsIgnoreCase(county) ){
				return false;
			}
		}
		else if( "CO".equalsIgnoreCase(state) ){
			if( "Denver".equalsIgnoreCase(county) || "Eagle".equalsIgnoreCase(county) || "Boulder".equalsIgnoreCase(county) || "Jefferson".equalsIgnoreCase(county) ){
				return false;
			}
		}
		else if( "AK".equalsIgnoreCase(state) ){
			if( county.toLowerCase().startsWith("anchorage")){
				return false;
			}
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		ParsedResponse originalParseResponse = Response.getParsedResponse();
		
		switch (viParseID){
			case ID_SAVE_TO_TSD:
				{
		        	StringBuilder buildTemp =  new StringBuilder(removeFormatting(originalParseResponse.getResponse()));
		        	Node doc = (Node) originalParseResponse.getAttribute(ParsedResponse.DASL_RECORD);
		        	String instrNo = getInstrFromXML(doc, null);
		        	
		        	// set file name
		            msSaveToTSDFileName = instrNo + ".html";            
		            originalParseResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
		            
	            	String propertyId = ((PropertyIdentificationSet)originalParseResponse.getPropertyIdentificationSet(0)).getAtribute("ParcelIDParcel"); 
	            	
	            	if(!StringUtils.isEmpty(propertyId )){
	            		
	            		getCurrentClassServerInfo().getModule(TSServerInfo.DASL_GENERAL_SEARCH_MODULE_IDX).setVisible(false); //bug 9115
		            	TSServerInfoModule transactionModule = getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.DASL_GENERAL_SEARCH_MODULE_IDX, new SearchDataWrapper());
		            	transactionModule.getFunction(0).setData(propertyId);
		    			ServerResponse res=null;
		    			try{
		    				res = SearchBy(transactionModule, new SearchDataWrapper());
		    			}
		    			catch(Exception e){
		    				e.printStackTrace();
		    			}
		    			
		    			Vector<SaleDataSet> saleDataSetVector = ((Vector<SaleDataSet>)originalParseResponse.infVectorSets.get( "SaleDataSet" ));
    					if(saleDataSetVector ==null){
    						saleDataSetVector= new Vector<SaleDataSet>();
    						originalParseResponse.infVectorSets.put("SaleDataSet", saleDataSetVector);
    					}
    					
    					DocumentI asDoc = originalParseResponse.getDocument();
						SaleDataSet originalSaleDataSet = null;
						RegisterDocumentI originalReference = null;
    					Set<InstrumentI> parsedTemp = null;
    					
    					Boolean setTransfer = false;
    					List<InstrumentStruct> list = new ArrayList<InstrumentStruct>();
    					Vector<SaleDataSet> tempVector = new Vector<SaleDataSet>();
    					
    					if(asDoc.getReferences() != null && asDoc.getReferences().size() > 0) {
    						originalReference = asDoc.getReferences().iterator().next();
    					}
    					
    					if(saleDataSetVector.size()>0 && !saleTransferIsReliable()){
    						saleDataSetVector.clear();
    						if(asDoc!=null){
    							Set<RegisterDocumentI> references = asDoc.getReferences();
    							Set<InstrumentI> parsedRefs = asDoc.getParsedReferences();
    							if(references!=null && references.size()>0){
    								references.clear();
    							}
    							if(parsedRefs!=null && parsedRefs.size()>0){
    								parsedRefs.clear();
    							}
    						}
    					}
    					
    					if(saleDataSetVector.size()>0){
    						originalSaleDataSet = saleDataSetVector.get(0);
    					}
    					
						if(asDoc!=null){
							Set<RegisterDocumentI> references = asDoc.getReferences();
							parsedTemp = new HashSet<InstrumentI>(asDoc.getParsedReferences());
							
							if(references!=null && references.size()>0){
								if( parsedTemp.size()==0 ){
									for(RegisterDocumentI regDoc:references){
										parsedTemp.add(regDoc.getInstrument());
									}
								}
							}
						}
    					
		    			if(res!=null){
		    				Vector<ParsedResponse> vec = res.getParsedResponse().getResultRows();
		    				
		    				for(int i=0;i<vec.size();i++){
		    					ParsedResponse transHistParseResponse = (ParsedResponse)vec.get(i);
		    					
		    					Vector<SaleDataSet> vec2 = ((Vector<SaleDataSet>)transHistParseResponse.infVectorSets.get( "SaleDataSet" ));
		    					if(vec2!=null && vec2.size()>0){
			    					SaleDataSet set= vec2.elementAt(0);
			    					
			    					if(!setTransfer){
				    					if( setTransfer = addInstrumentToList(set, tempVector, global, list, originalParseResponse, transHistParseResponse) ){
				    					}
			    					}else{
			    						addInstrumentToList(set, tempVector, global, list, originalParseResponse, transHistParseResponse) ;
			    					}
			    					
			    					buildTemp.append(removeFormatting(transHistParseResponse.getResponse()));
			    				}
		    				}
		    				
		    				if( saleDataSetVector.size()>0 && saleTransferIsReliable() ){
		    					if(!setTransfer){
		    						if( setTransfer = addInstrumentToList(originalSaleDataSet, tempVector, global, list, originalParseResponse, null) ){
		    						}
		    					}else{
		    						addInstrumentToList(originalSaleDataSet, tempVector, global, list, originalParseResponse, null);
		    					}
		    					for(InstrumentI refToBeAdded:parsedTemp){
		    						if(!foundIn(asDoc.getParsedReferences(),refToBeAdded)){
		    							asDoc.addParsedReference(refToBeAdded);
		    						}
		    					}
	    					}
		    				
		    				
		    				
		    				
		    				boolean removedOriginal = false;
		    				
		    				if(saleDataSetVector.indexOf(originalSaleDataSet) >= 0 || tempVector.indexOf(originalSaleDataSet) >= 0) {
		    					removedOriginal = true;
		    					saleDataSetVector.remove(originalSaleDataSet);
		    					tempVector.remove(originalSaleDataSet);
		    				}
		    				
		    				
		    				for(SaleDataSet set:tempVector){
		    					if(notDuplicate(saleDataSetVector,set)){
		    						saleDataSetVector.add(set);
		    					}
		    				}
		    				if(removedOriginal) {
		    					if(notDuplicate(saleDataSetVector, originalSaleDataSet)) {
		    						saleDataSetVector.add(originalSaleDataSet);
		    					} else {
		    						asDoc.getReferences().remove(originalReference);
		    						asDoc.getParsedReferences().remove(originalReference.getInstrument());
		    					}
		    				}
		    				
    						global.setAdditionalInfo("NDB_INSTRUMENT_LIST", list);
		    			}
	            	}
		         // save to TSD
	            	//buildTemp.append(CreateFileAlreadyInTSD(true));
	            	String html = buildTemp.toString(); 
		            msSaveToTSDResponce = html ;        
		            DBManager.updateDocumentIndex(originalParseResponse.getDocument().getIndexId(),html, global);
		            parser.Parse(originalParseResponse, html, Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
				}
		    break;
			case ID_DETAILS:
				{
					Vector<ParsedResponse> newResultRows = new Vector<ParsedResponse>();
					Set<String> docs = new HashSet<String>();
					boolean transactionHistSearch = false;
			    	// parse all records
		            for ( ParsedResponse item: ( Vector<ParsedResponse> ) originalParseResponse.getResultRows() ) {            	
		            	item.setParentSite(originalParseResponse.isParentSite());
		            	// parse
		            	String result[] = parse(item,viParseID);
		            	String itemHtml = result[0];
		            	String shortType = result[1];
		            	item.setResponse(itemHtml);
		            	//            	 get parsed XML document
		            	Node doc = (Node) item.getAttribute(ParsedResponse.DASL_RECORD);
		            	
		            	// determine instrument number - skip row if it has none
		            	String instrNo="";
		            	HashMap<String, String> data = new HashMap<String, String>();
		            	try{
		            		instrNo = getInstrFromXML(doc, data);            		
		            	}catch(RuntimeException e){
		            		e.printStackTrace();
		            		logger.warn(searchId + ": Document from dasl NDB has NO Instrument number. It has been skipped!");         		
		            		continue;
		            	}
		            	// do not add the document twice
		            	if( "".equals(instrNo) || docs.contains(instrNo) ){
		            		continue;
		            	}
		            	docs.add(instrNo);
		            	// add row
		            	newResultRows.add(item);
		            	// create links
		            	String originalLink = "DL___" + instrNo;            	
		            	String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;   
		                
//		            	String checkbox = "";
		            	String radiobutton = "";
		            	if(data.get("type") == null) {
		            		data.put("type", "ASSESSOR");
		            		data.put("dataSource", "NB");
		            	}
		                //if (FileAlreadyExist(instrNo + ".html") ) {
		                if (isInstrumentSaved(data.get("APN"), originalParseResponse.getDocument(), data) ) {
//		                	checkbox = "saved";
		                	radiobutton  = "saved";
		                } else {
		                	if (item.getResponse().matches("(?is).*<\\s*td[^>]*>[^<]*<\\s*b[^>]*>\\s*transaction\\s*<\\s*/b\\s*>\\s*<\\s*/td\\s*>.*"))
		                	{// if Transaction History Search
			                	transactionHistSearch = true;
		                		mSearch.addInMemoryDoc(sSave2TSDLink, item);	
		                	}
		                	else
		                	{
//			                	checkbox = "<input type='checkbox' name='docLink' value='" + sSave2TSDLink + "'>";
		                		radiobutton = "<input type='radio' name='docLink' value='" + sSave2TSDLink + "'>";
			                	mSearch.addInMemoryDoc(sSave2TSDLink, item);
		                	}
		                }
		                
	                	if (item.getResponse().matches("(?is).*<\\s*td[^>]*>[^<]*<\\s*b[^>]*>\\s*transaction\\s*<\\s*/b\\s*>\\s*<\\s*/td\\s*>.*"))
	                	{// if Transaction History Search
		                	transactionHistSearch = true;
	                		itemHtml =	"<tr> <td align=\"center\"><b>" + shortType +
        					"</b></td><td>" + itemHtml + "</td><tr>";
	                	}
	                	else
	                	{
//	                		itemHtml =	"<tr> <td valign=\"center\" align=\"center\">" + checkbox + "</td> <td align=\"center\"><b>" + shortType + 
//	                					"</b></td><td>" + itemHtml + "</td><tr>";
	                		itemHtml =	"<tr> <td valign=\"center\" align=\"center\">" + radiobutton + "</td> <td align=\"center\"><b>" + shortType + 
                					"</b></td><td>" + itemHtml + "</td><tr>";
	                	}
		                Matcher mat = patImageLink.matcher(itemHtml);
		                if(mat.find()){
		                	String imageLink = createLinkForImage(data);
		                	itemHtml = itemHtml.replaceAll(LINK_TO_IMAGE_REGEX,  "<a href=\"" + imageLink + "\">View Image</a>");
		                	if(item.getImageLinksCount() == 0){
		                		item.addImageLink(new ImageLinkInPage (imageLink, instrNo + ".tiff" ));
		                	}  
		                }   
		                item.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
		                parser.Parse(item, itemHtml,Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD); 
		            }
		            // set the result rows - does not contain instruments without instr no
		            originalParseResponse.setResultRows(newResultRows);	            
		            // set proper header and footer for parent site search , NDB has unique results and need this in automatic when user is asked what to choose
		           
	            	String header = originalParseResponse.getHeader();
	               	String footer = originalParseResponse.getFooter();                           	
	            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
	            	// TODO
	            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" +
	            			"<tr bgcolor=\"#cccccc\">\n<th width=\"1%\"><div >All</div></th> <th width=\"1%\">Type</th> \n<th width=\"98%\" align=\"left\">Document</th> \n</tr>";

	            	// if Transaction History Search
	            	if (transactionHistSearch){
	            		footer =   "\n</table>\n";
	            	}
	            	else{
	            		String aoMess = "<font color='red' > <b> &nbsp;&nbsp;Select just one document !</b> </font>";
	            		footer =   "\n</table>\n"+aoMess+ "<br/>"+ CreateSaveToTSDFormEnd(SAVE_DOCUMENT_BUTTON_LABEL, viParseID, -1);
	            	}
	            	originalParseResponse.setHeader(header);
	            	originalParseResponse.setFooter(footer);
				}
			break;
		}
	}
	
	private boolean foundIn(Set<InstrumentI> parsedReferences,	InstrumentI refToBeAdded) {
		
		for(InstrumentI i:parsedReferences){
			if(i.flexibleEquals(refToBeAdded, true)){
				return true;
			}
		}
		
		return false;
	}

	private boolean notDuplicate(Vector<SaleDataSet> vec,SaleDataSet set){
		for(SaleDataSet curset:vec){
			String book = curset.getAtribute("Book");
			String page = curset.getAtribute("Page");
			String docNo = curset.getAtribute("InstrumentNumber");		
			if(!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page)){
				if(book.equals(set.getAtribute("Book"))&&page.equals(set.getAtribute("Page"))){
					return false;
				}
			}
			if(!StringUtils.isEmpty(docNo)){
				if (CountyConstants.OH_Franklin == dataSite.getCountyId()){
					String recordedDate = curset.getAtribute("RecordedTime");
					String instrNumberOrig = set.getAtribute("InstrumentNumber");
					
					if (org.apache.commons.lang.StringUtils.isNotEmpty(recordedDate) && org.apache.commons.lang.StringUtils.isNotEmpty(instrNumberOrig)){
						recordedDate = recordedDate.replaceAll("\\p{Punct}", "");
						String docNoClean = org.apache.commons.lang.StringUtils.stripStart(docNo.replaceFirst(recordedDate, ""), "0");
						
						if (docNo.equals(instrNumberOrig) || docNoClean.equals(instrNumberOrig)){
							return false;
						}
					}
				} else{
					if(docNo.equals( set.getAtribute("InstrumentNumber"))){
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public void specificParseLegalDescription(Document doc, ParsedResponse item, ResultMap resultMap) {
	}
	
	@Override
	public void specificParseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap) {
	}

	@Override
	public void specificParseAddress(Document doc, ParsedResponse item,
			ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected String modifyXMLResponse(String xml,int moduleIDX) {
		xml = xml.replaceAll("(?i)<FullName>([^/]+)/([^/]+)</FullName>", "<FullName>$1 &amp; $2</FullName>");
		
		return super.modifyXMLResponse(xml, moduleIDX);
	}

	public static String formatPID(String pid, String countyId) {
		if(countyId.equals(CountyConstants.CA_Alameda_STRING)) {
			pid = pid.replaceFirst("^(\\d{3})(\\d{4})(\\d{3})$", "$1-$2-$3");
			pid = pid.replaceFirst("(?i)^(\\d{3}[A-Z])(\\d{4})(\\d{3})$", "$1-$2-$3");
			pid = pid.replaceFirst("^(\\d{3})(\\d{4})(\\d{3})(\\d{2})$", "$1-$2-$3-$4");
			return pid;
		} else if(countyId.equals(CountyConstants.CA_Kern_STRING)) {
			pid = pid.replaceFirst("^(\\d{3})(\\d{3})(\\d{2})(\\d{2})$", "$1-$2-$3-$4");
			return pid;
		} else if(countyId.equals(CountyConstants.CA_San_Francisco_STRING)) {
			pid = pid.replaceFirst("^(\\d{4})(\\d{3})$", "$1-$2");
			return pid;
		} else if(countyId.equals(CountyConstants.CA_Siskiyou_STRING)) {
			pid = pid.replaceFirst("^(\\d{3})(\\d{3})(\\d{3})$", "$1-$2-$3");
			return pid;
		} else if (countyId.equals(CountyConstants.FL_Hernando_STRING)) {
			pid = pid.replaceAll("\\s+", "-");
			return pid;
		} else if (countyId.equals(CountyConstants.FL_Polk_STRING)) {
			pid = pid.replaceFirst("^(\\d{6})-(\\d{6})-(\\d{6})$", "$1$2$3");
			return pid;
		} else if(countyId.equals(CountyConstants.SC_Greenville_STRING)) {
			String newPid = pid.replaceAll("-", "");
			if(newPid.matches("\\d{14}")) {
				newPid = newPid.replaceFirst("(\\d{2})(\\d{3})(\\d{2})(\\d{2})(\\d{3})(\\d{2})",
						"$1-$2-$3-$4-$5-$6");
				return newPid;
			}
		} else if(countyId.equals(CountyConstants.TN_Cocke_STRING)) {
			pid = pid.replaceFirst("^(\\d{3})(\\d{3}\\.\\d{2})$", "$1-$2");
			pid = pid.replaceFirst("(?i)^(\\d{3}[A-Z])([A-Z])(\\d{3}\\.\\d{2})$", "$1-$2-$3");
			return pid;
		} else if(countyId.equals(CountyConstants.TN_Williamson_STRING)) {
			pid = pid.replaceFirst("(?i)^(\\d{3}[A-Z])([A-Z])(\\d{3}\\.\\d{2})$", "$1-$2-$3");
			return pid;
		}
		
		return pid;
	}
	
	@Override
	public String getSaveSearchParametersButton(ServerResponse response) {
		return null;
	}
	
	@Override
	protected int getResultType(){
		if(mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) == Boolean.TRUE ||
				(mSearch.getSa().getPins(-1).size() > 1 && (Search.AUTOMATIC_SEARCH == mSearch.getSearchType()))){
			return MULTIPLE_RESULT_TYPE;
		} else {
			return UNIQUE_RESULT_TYPE;
		}
	}
	
	@Override
    public boolean anotherSearchForThisServer(ServerResponse sr) {
		return  mSearch.getSa().getPins(-1).size() > 1 &&
			    mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) != Boolean.TRUE;
	}
}

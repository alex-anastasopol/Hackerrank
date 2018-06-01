package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.database.rowmapper.RequestCount;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeAdvancedFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.server.UploadImage;
import com.stewart.datatree.DataTreeAccount;
import com.stewart.datatree.DataTreeConn;
import com.stewart.datatree.DataTreeImageException;
import com.stewart.datatree.DataTreeManager;
import com.stewart.datatree.DataTreeStruct;

/**
 * @author Cristian Stochina
 */
public class FLSubdividedBasedDASLDT extends FLGenericDASLDT implements DTLikeAutomatic{
 
	private static final String DT_FAKE_RESPONSE = StringUtils.fileReadToString(FAKE_FOLDER+"DASLFakeResponse.xml");
	
	private static final long serialVersionUID = -7007802612257531559L;

	public FLSubdividedBasedDASLDT(long searchId) {
		super(searchId);
	}

	public FLSubdividedBasedDASLDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	public static String createTempSaveFileName(InstrumentI i , final String SEARCH_DIR){
		String folderName = SEARCH_DIR+ "Register" + File.separator;
		new File(folderName).mkdirs();
		String key ="";
		if(i.hasBookPage()){
			key = i.getBook()+"_"+i.getPage()+"_"+i.getDocType();
		}else if(i.hasInstrNo()){
			key = i.getInstno()+"_"+i.getDocType()+"_"+i.getYear();
		}
		else if(i.hasDocNo()){
			key = i.getDocno()+"_"+i.getDocType()+"_"+i.getYear();
		}else{
			throw new RuntimeException("-createTempSaveFileName- Please pase a valid Instrument");
		}
    	return  folderName + key + ".tif";
	}
	
	@Override 
	protected TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent) {
		ParsedResponse pr = response.getParsedResponse();
    	Search search=InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext();
    	DocumentsManagerI manager = search.getDocManager();
    	DocumentI doc = pr.getDocument() ;
    	if(doc !=null){
	        try{
	        	manager.getAccess();
	        	DocumentI atsDoc = manager.getDocument(doc.getInstrument());
	        	if( atsDoc!=null){
		        	if(doc  instanceof RegisterDocumentI && atsDoc instanceof RegisterDocumentI){
		        		RegisterDocumentI regDoc = (RegisterDocumentI)doc;
		        		RegisterDocumentI regAtsDoc = (RegisterDocumentI)atsDoc;
		        		
		        		int legalSizeATS = calculateLegalSize(regAtsDoc);
		        		int legalSize = calculateLegalSize(regDoc);
		        		int nameSizeAts = calculateNameSize(regAtsDoc);
		        		int nameSize = calculateNameSize(regDoc);
		        		
		        		if(Boolean.TRUE.equals(mSearch.getAdditionalInfo("RESAVE_DOCUMENT"))&&!response.getParsedResponse().isParentSite()){
		        			regDoc.setSearchType(regAtsDoc.getSearchType());
		        			ImageI imageRegAtsDoc = regAtsDoc.getImage();
		        			if(imageRegAtsDoc!=null){
		        				if(imageRegAtsDoc.isOcrDone()){
		        					ImageI imageRegDoc = regDoc.getImage();
		        					if(imageRegDoc !=null){
		        						regDoc.getImage().setOcrDone(true);
		        					}else{
		        						regDoc.setImage(regAtsDoc.getImage());
				        				pr.resetImages();
		        					}
		        				}
		        			}
		        		}
		        		
		        		if(legalSizeATS>legalSize || nameSizeAts> nameSize){
		        			return TSServer.ADD_DOCUMENT_RESULT_TYPES.ALREADY_EXISTS;
		        		}
		        	}
	        	}
	        }finally{
	        	manager.releaseAccess();
	        }
    	}
		
        return addDocumentInATS(response, htmlContent, false);
    }
	
	static protected List<PersonalDataStruct> keepOnlyGoodLegals(List<PersonalDataStruct> legals){
		List<PersonalDataStruct> good = new ArrayList<PersonalDataStruct>();
		for(PersonalDataStruct str:legals){
			if(!incompleteData(str)){
				good.add(str);
			}
		}
		return good;
	}
	
	private static boolean incompleteData(PersonalDataStruct str) {
		if( str==null ){
			return true;
		}
		
		boolean emptyLot = StringUtils.isEmpty(str.lot);
		boolean emptyBlock = StringUtils.isEmpty(str.block);
		boolean emptyPlatInst = StringUtils.isEmpty(str.platInst);
		boolean emptyPlatBook = StringUtils.isEmpty(str.getPlatBook());
		boolean emptyPlatPage = StringUtils.isEmpty(str.getPlatPage());
		
		boolean emptySection = StringUtils.isEmpty(str.getSection());
		boolean emptyRange = StringUtils.isEmpty(str.getRange());
		boolean emptyTownship = StringUtils.isEmpty(str.getTownship());
		
		boolean emptyArblot = StringUtils.isEmpty(str.arbLot);
		boolean emptyArbBlock = StringUtils.isEmpty(str.arbBlock);
		boolean emptyArbBook = StringUtils.isEmpty(str.arbBook);
		boolean emptyArbPage = StringUtils.isEmpty(str.arbPage);
		
		boolean emptyNcbNumber = org.apache.commons.lang.StringUtils.isEmpty(str.ncbNumber);
		boolean emptySubdivisionName = org.apache.commons.lang.StringUtils.isEmpty(str.subdivisionName);
		boolean emptyTract = StringUtils.isEmpty(str.tract);
		
		if("sectional".equalsIgnoreCase(str.type)||"arb".equalsIgnoreCase(str.type)){
			if(!emptySection){
				return (emptySection || emptyTownship || emptyRange);
			}else{
				return (  (emptyArbBook || emptyArbPage) || (emptyArblot && emptyArbBlock) );
			}
		}else if("subdivided".equalsIgnoreCase(str.type)){
			if (!emptyNcbNumber){
				return (emptyNcbNumber && emptySubdivisionName);
			} else if (!emptyTract){
				return emptyTract;
			} else{
				return ((emptyPlatBook || emptyPlatPage) && emptyPlatInst) || (emptyBlock && emptyLot);
			}
		}
		
		return   true;
	}
	
	private static int calculateNameSize(RegisterDocumentI regAtsDoc){
		int nameSize = 0;
		
		PartyI grantee = regAtsDoc.getGrantee();
		if(grantee!=null){
			nameSize +=grantee.size();
		}
		
		PartyI grantor = regAtsDoc.getGrantor();
		
		if(grantor!=null){
			nameSize+=grantor.size();
		}
		
		return nameSize;
	}
	
	private static int calculateLegalSize(RegisterDocumentI regAtsDoc){
		int legalSizeATS = 0;
		try{
			for(PropertyI prop:regAtsDoc.getProperties()){
				if(prop.getLegal().hasSubdividedLegal()){
					legalSizeATS++;
				}
				if(prop.getLegal().hasTownshipLegal()){
					legalSizeATS++;
				}
			}
		}catch(Exception e){}
		return legalSizeATS;
	}
	
	@SuppressWarnings("unchecked")
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		if(isParentSite()){
			mSearch.setAdditionalInfo("RESAVE_DOCUMENT",Boolean.FALSE);
		}
		
		if(!isParentSite() 
				&& (dontMakeTheSearch(module, searchId, false)|| "true".equalsIgnoreCase(global.getSa().getAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND)))
			){
			return new ServerResponse();
		}
		
		if (TSServerInfo.NAME_MODULE_IDX == module.getModuleIdx()){//B 4382
			mSearch.setAdditionalInfo("RESAVE_DOCUMENT",Boolean.FALSE);
			if (isParentSite()) {
				String lastName = module.getParamValue(4);
				int maxLastNameLengthThatShouldBeSplit = 25;
				if (StringUtils.isNotEmpty(lastName) && lastName.length() > maxLastNameLengthThatShouldBeSplit){
					String temp = lastName.substring(0, maxLastNameLengthThatShouldBeSplit);
					String first = lastName.substring(maxLastNameLengthThatShouldBeSplit, lastName.length());
					module.setParamValue(2, first);
					module.setParamValue(4, temp);
				}
			}
			
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			ServerResponse ret = null;
			
			if(!modules.isEmpty()) {
				ret = super.searchByMultipleInstrument(modules,sd, null);
			}else{
				ret = super.SearchBy(module, sd);
			} 
			return ret;
		}
		
		if(TSServerInfo.IMG_MODULE_IDX == module.getModuleIdx() ){
			String imageName = "";
			if( (imageName = (String)global.getAdditionalInfo("IMAGE_FAKE_DT"))!=null ){
				writeImageToClient(imageName, "image/tiff");
				return new ServerResponse();
			}
		}
		 
		global.setAdditionalInfo("IMAGE_FAKE_DT", null);
		
		if( TSServerInfo.INSTR_NO_MODULE_IDX == module.getModuleIdx()) {
			if(SearchAttributes.INSTR_LIST.equalsIgnoreCase(module.getSaObjKey())){
				mSearch.setAdditionalInfo("RESAVE_DOCUMENT",Boolean.TRUE);
			}
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			ServerResponse ret = null;
			if(!modules.isEmpty()) {
				ret =  super.searchByMultipleInstrument(modules,sd, null);
			}else{
				ret = super.SearchBy(module, sd);
			} 
			return ret;
		}
		
		if( TSServerInfo.BOOK_AND_PAGE_MODULE_IDX == module.getModuleIdx()) {
			if(SearchAttributes.INSTR_LIST.equalsIgnoreCase(module.getSaObjKey())){
				mSearch.setAdditionalInfo("RESAVE_DOCUMENT",Boolean.TRUE);
			}
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			ServerResponse ret = null;
			if(!modules.isEmpty()) {
				ret = super.searchByMultipleInstrument(modules,sd, null);
			}else{
				ret = super.SearchBy(module, sd);
			}
			return ret;
		}
		
		if(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX != module.getModuleIdx()&& TSServerInfo.INSTR_NO_MODULE_IDX != module.getModuleIdx()){
			mSearch.setAdditionalInfo("RESAVE_DOCUMENT",Boolean.FALSE);
		}
		//search plats 
		if ( TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX == module.getModuleIdx() ) {
			mSearch.setAdditionalInfo("RESAVE_DOCUMENT",Boolean.FALSE);
			DataTreeAccount acc = DataTreeManager.getDatatTreeAccount(String.valueOf(getCommunityId()));
			String book = module.getParamValue( 0 );
			String page = module.getParamValue( 1 );
			String docNo = module.getParamValue( 2 );
			String year = module.getParamValue(3);
			String docType = module.getParamValue(4);
			String dataTreeType = module.getParamValue(5);
	    	
			int yearI =-1; try{yearI=Integer.parseInt(year);}catch(Exception e){}
			InstrumentI i = new Instrument(book,page,docType,"",yearI);
			i.setInstno(docNo);
			i.setYear(yearI);
			i.setDocType(dataTreeType);
			
	    	
	    	List<DataTreeStruct> searched = new ArrayList<DataTreeStruct>();
	    	for(DataTreeStruct  temp:datTreeList){
	    		if(dataTreeType.equalsIgnoreCase(temp.getDataTreeDocType())){
	    			searched.add(temp);
	    		}
	    	}
	    	
	    	boolean isAssesorMap = "ASSESSOR_MAP".equalsIgnoreCase(dataTreeType);
	    	String key = (book==null?"":book)+"-"+(page==null?"":page)+"-"+(docNo==null?"":docNo);
	    	
	    	HashSet<String> searchedImages = (HashSet<String>)global.getAdditionalInfo("SEARCHED_IMAGES");
	    	if(searchedImages ==null){
	    		searchedImages  = new HashSet<String>();
	    		global.setAdditionalInfo("SEARCHED_IMAGES",searchedImages);
	    	}
	    	
	    	if( !isParentSite() && searchedImages.contains(key)){
	    		return new ServerResponse();
	    	}
	    	
	    	logSearchBy(module);
	    	
	    	String fileName = createTempSaveFileName(i,  getCrtSearchDir() );
	    	global.setAdditionalInfo("IMAGE_FAKE_DT", fileName);
	    	boolean imageDownloaded = false;
	    	
	    	List<DataTreeImageException> exceptions = new ArrayList<DataTreeImageException>();
	    	
	    	for(DataTreeStruct struct:searched){
	    		try {
	    			//count request on datatree
					mSearch.countRequest(getDataSite().getSiteTypeInt(), RequestCount.TYPE_MISC_COUNT);
					
					if(DataTreeManager.downloadImageFromDataTree(acc, struct, i, fileName, null, null)){
						imageDownloaded = true;
						int type = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID).getCityCheckedInt();
						afterDownloadImage(imageDownloaded,type);
						break;
					}
				} catch (DataTreeImageException e) {
					exceptions.add(e);
				}
	    	}
			if(!imageDownloaded && searched.size() == exceptions.size() && !exceptions.isEmpty()) {
				DataTreeConn.logDataTreeImageException(i, searchId, exceptions, true);
			}
	    	
			if(imageDownloaded ){
				SearchLogger.info( "<b><font color=\"red\">Image " +i.prettyPrint()+ " Downloaded from Data Tree</font></b>", searchId );  
				
				if( !isParentSite() ){
					searchedImages.add(key);
		    	}
				
				String grantor  = "County of "+InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
				grantor=grantor==null?"":grantor;
				String grantee = mSearch.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME);
				grantee=grantee==null?"":grantee;
				
				grantee = StringUtils.HTMLEntityEncode(grantee);
				grantor = StringUtils.HTMLEntityEncode(grantor);
				
				if(!"PLAT".equals(docType)){
					grantee="";
					grantor="";
				}
				
				String doc = DT_FAKE_RESPONSE.replace("@@Grantee@@", grantee);
				doc = doc.replace("@@Grantor@@", grantor);
				doc = doc.replace("@@Date@@", "01/01/1960");
				doc = doc.replace("@@Type@@-@@Type@@", dataTreeType);
				
				if(isAssesorMap){
					doc = doc.replace("@@Book@@", "");
					doc = doc.replace("@@Page@@", "");
					doc = doc.replace("@@DocNo@@", key);
				}else{
					doc = doc.replace("@@Book@@", book==null?"":book);
					doc = doc.replace("@@Page@@", page==null?"":page);
					doc = doc.replace("@@DocNo@@", docNo==null?"":docNo);
				}
				
				return searchBy(getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX), sd, doc);
			}
			else{
				SearchLogger.info( "<b><font color=\"red\">Could not download image " +i.prettyPrint()+ " from Data Tree</font></b>", searchId );
				ServerResponse sr = new ServerResponse();
				ParsedResponse pr = new ParsedResponse();
				sr.setParsedResponse(pr);
				sr.setResult("<b>Could not download image</b>");
				solveHtmlResponse(module.getModuleIdx()+"", module.getParserID(), "SearchBy", sr, sr.getResult());
				return sr;
			}
		}
		
		if( TSServerInfo.RELATED_MODULE_IDX == module.getModuleIdx()) {
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			ServerResponse ret = null;
			if(!modules.isEmpty()) {
				ret = super.searchByMultipleInstrument(modules, sd, null);
			} else {
				ret = super.SearchBy(module, sd);
			}
			return ret;
		}
		
		return super.SearchBy(module, sd);
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
			
				@SuppressWarnings("rawtypes")
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
								if(org.apache.commons.lang.StringUtils.isNotBlank(module.getFunction(4).getParamValue())) {
									try {
										newReference.setYear(Integer.parseInt(module.getFunction(4).getParamValue().trim()));
									} catch (Exception e) {
									}
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
		return firstModule.getModuleIdx() == TSServerInfo.RELATED_MODULE_IDX && firstModule.getFunctionCount() == 6;
	}
	
	protected static Set<InstrumentI> getAllAoAndTaxReferences(Search search){
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
	
	
	static String getStartingZerosForCounty(long searchId){
		String countyName = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		if("Flagler".equalsIgnoreCase(countyName)){
			return "0000";
		}
		return "000";
	}
	
	protected boolean addAoLookUpSearchesBookPageAtDocNo(TSServerInfo serverInfo, List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef,long searchId,  boolean isUpdate, boolean isTimeShare){
		boolean atLeastOne = false;
		final Set<String> searched = new HashSet<String>();
		int stop = isTimeShare?5:Integer.MAX_VALUE;
		int i=0;
		
		DocumentsManagerI manager = getSearch().getDocManager();
		try{
			manager.getAccess();
		
			for(InstrumentI inst:allAoRef){
				List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, inst);
				if(almostLike.isEmpty()) {
					i++;
					boolean t = addBookPageSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
					atLeastOne = atLeastOne || t;
					
					if( inst.hasInstrNo() ){
						String instNo = inst.getInstno();
						if(instNo.length()==10 && !instNo.startsWith(getStartingZerosForCounty(searchId))){ //Book Page
							InstrumentI newInst = inst.clone();
							newInst.setBook(instNo.substring(0,6));
							newInst.setPage(instNo.substring(6,10));
							boolean temp = addBookPageSearch(newInst, serverInfo, modules, searchId, searched, isUpdate);
							atLeastOne = atLeastOne || temp;
						}
						else{
							boolean temp = addInstNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
							atLeastOne = atLeastOne || temp;
						}
					}
					
					if ( inst.hasDocNo()  ){
						String docNo = inst.getDocno();
						if(docNo.length()==10 && !docNo.startsWith(getStartingZerosForCounty(searchId))){ //Book Page
							InstrumentI newInst = inst.clone();
							newInst.setBook(docNo.substring(0,6));
							newInst.setPage(docNo.substring(6,10));
							boolean temp = addBookPageSearch(newInst, serverInfo, modules, searchId, searched, isUpdate);
							atLeastOne = atLeastOne || temp;
						}
						else{
							boolean temp = addDocNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
							atLeastOne = atLeastOne || temp;
						}
					}
					if(i>=stop){
						break;
					}
				}
			}
		}
		finally {
			manager.releaseAccess();
		}
		return atLeastOne;
	}
	
	protected boolean addBookPageSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched,boolean isUpdate){
		if(inst.hasBookPage()){
			String book = inst.getBook().replaceFirst("^0+", "");
			String page = inst.getPage().replaceFirst("^0+", "");
			if(!searched.contains(book+"_"+page)){
				searched.add(book+"_"+page);
			}else{
				return false;
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
	
	protected boolean addDocNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId,Set<String> searched, boolean isUpdate){
		if ( inst.hasDocNo() ){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			String instr = inst.getDocno().replaceFirst("^0+", "");
			String year = String.valueOf(inst.getYear());
			if(!searched.contains(instr+year)){
				searched.add(instr+year);
			}else{
				return false;
			}
			module.setData(0, cleanInstrNo(inst, false));
			module.setData(2, year);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
		if ( inst.hasInstrNo() ){
			String instr = inst.getInstno().replaceFirst("^0+", "");
			String year = String.valueOf(inst.getYear());
			if(!searched.contains(instr+year)){
				searched.add(instr+year);
			}else{
				return false;
			}
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.setData(0, cleanInstrNo(inst, false));
			module.setData(2, year);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	public boolean addAoLookUpSearches(TSServerInfo serverInfo, List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef,long searchId,  boolean isUpdate, boolean isTimeShare){
		return addAoLookUpSearchesBookPageAtDocNo(serverInfo, modules, allAoRef, searchId, isUpdate, isTimeShare);
	}

	@Override
	public void addIteratorModule( TSServerInfo serverInfo, List<TSServerInfoModule> modules,int code, long searchId, boolean isUpdate, boolean isTimeShare){
		addIteratorModuleGlobal(serverInfo, modules, code, searchId, isUpdate, isTimeShare);
	}
		
	public static void addIteratorModuleGlobal( TSServerInfo serverInfo, List<TSServerInfoModule> modules,int code, long searchId, boolean isUpdate, boolean isTimeShare){
		
		County county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
		String countyName = county.getName();
		
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.clearSaKeys();
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
		module.addIterator(it);
		if (isUpdate) {
			module.addFilter(new BetweenDatesFilterResponse(searchId));
		}
		PinFilterResponse pinFilter = getPinFilter(searchId,true);
		if(pinFilter!=null){
			module.addFilter(pinFilter);
		}
		
		if(isTimeShare){
			module.addFilter(new IncompleteLegalFilter("", searchId));
		}
		if (code == TSServerInfo.SECTION_LAND_MODULE_IDX){
			GenericNameFilter nameFilterOwner = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			nameFilterOwner.setInitAgain(true);
			nameFilterOwner.setUseSynonymsBothWays(false);
			nameFilterOwner.setIgnoreMiddleOnEmpty(true);
			module.addFilter(nameFilterOwner);
		}
		
		//B 5728
		if("Gilchrist".equalsIgnoreCase(countyName)&&code == TSServerInfo.SUBDIVISION_MODULE_IDX){
			module.addFilter(LegalFilterFactory.getDefaultLegalFilter(searchId));
		}
		
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		if (StringUtils.isNotEmpty(search.getSa().getAtribute(SearchAttributes.LD_SUBLOT))) {
			try {
				final String coutyState = StateCountyManager.getInstance().getSTCounty(Long.parseLong(search.getCountyId()));

				RejectAlreadySavedDocumentsFilterResponse rejectAlreadySavedFilter = new RejectAlreadySavedDocumentsFilterResponse(searchId) {

					private static final long	serialVersionUID	= -7039182937380487641L;

					@Override
					protected InstrumentI formatInstrument(InstrumentI instr) {
						super.formatInstrument(instr);

						if (coutyState.equalsIgnoreCase("FLSarasota")) {
							FLSarasotaDT.processInstrumentNo(instr);
						} else if (coutyState.equalsIgnoreCase("COLarimer")) {
							COLarimerDT.processInstrumentNo(instr);
						}

						return instr;
					}

				};
				module.addFilter(rejectAlreadySavedFilter);
				module.addFilter(LegalFilterFactory.getDefaultSubLotFilter(searchId));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		modules.add(module);
	}
	
	protected static PinFilterResponse getPinFilter(long searchId, boolean actOnlyOnIncompleteLegal) {
		String county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
		String state = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getState().getStateAbv();
		Search serach = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		if("FL".equalsIgnoreCase(state)){
			if("lee".equals(county)
					||"sarasota".equals(county)
					||"palm beach".equals(county)
					||"pasco".equals(county)
					||"duval".equals(county)
					||"lake".equals(county)
					||"volusia".equals(county)/*||"brevard".equals(county)*/
					||"miami-dade".equals(county)
					||"collier".equals(county)
					||"pinellas".equals(county)
					||"hillsborough".equals(county)){
				return new PinFilter(SearchAttributes.LD_PARCELNO, searchId, actOnlyOnIncompleteLegal);
			}else if("broward".equals(county)){
				DocumentsManagerI m = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
				try{
					m.getAccess();
					if(m.getDocumentsWithType(DType.TAX).size()>0){
						return new PinFilter(SearchAttributes.LD_PARCELNO, searchId, actOnlyOnIncompleteLegal);
					}
				}finally{
					m.releaseAccess();
				}
			}else if("polk".equals(county)){
				String apn = serach.getSa().getAtribute(SearchAttributes.LD_PARCELNO);
				Set<String> allPinRefs = new HashSet<String>();
				if(!StringUtils.isEmpty(apn)){
					allPinRefs.add(apn.replaceAll("[- ]", ""));
				}
				
				apn = serach.getSa().getAtribute(SearchAttributes.LD_PARCELNONDB);
				if(!StringUtils.isEmpty(apn)){
					allPinRefs.add(apn.replaceAll("[- ]", ""));
				}
				
				DocumentsManagerI m = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
				try{
					m.getAccess();
					List<DocumentI> docs = m.getDocumentsWithType(DType.TAX,DType.ASSESOR);
					for(DocumentI d:docs){
						for(PropertyI prop:d.getProperties()){
							PinI pin = prop.getPin();
							for(PinType type:PinType.values()){
								String str = pin.getPin(type);
								if(!StringUtils.isEmpty(str)){
									allPinRefs.add(str.replaceAll("[- ]", ""));
								}
							}
						}
					}
				}finally{
					m.releaseAccess();
				}
				PinFilter filter = new PinFilter(SearchAttributes.LD_PARCELNO, searchId, actOnlyOnIncompleteLegal);
				filter.setParcelNumber(allPinRefs);
				return filter;
			}
		}
		return null;
	}

	protected void ParseResponse(String sAction, ServerResponse response, int viParseID) throws ServerResponseException {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int module = -1;
		try{ module = Integer.parseInt(sAction);} catch(Exception e){}
		
		switch(module){
			case TSServerInfo.SUBDIVISION_MODULE_IDX:
				
			break;
			case TSServerInfo.SECTION_LAND_MODULE_IDX:
				
			break; 
			case TSServerInfo.ARB_MODULE_IDX:
				
			break;
			case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
			case TSServerInfo.INSTR_NO_MODULE_IDX:
			
			break;
			
		}
		switch(viParseID){
			case ID_SAVE_TO_TSD:
				DocumentI doc = response.getParsedResponse().getDocument();
				String imageName = "";
				if( (imageName = (String)global.getAdditionalInfo("IMAGE_FAKE_DT"))!=null ){
					global.setAdditionalInfo("IMAGE_FAKE_DT",null);
					SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
					Date sdate = mSearch.getStartDate();
					
					String basePath = ServerConfig.getImageDirectory()+File.separator+format.format(sdate)+File.separator+searchId;
		    		File file= new File(basePath);
		    		if(!file.exists()){
		    			file.mkdirs();
		    		}
		    			    		
		        	String tiffFileName = doc.getId()+".tiff";
		        	String path 	= basePath+File.separator+tiffFileName;
		        	
		        	boolean test = true;
		        	try {
						FileUtils.copyFile(new File(imageName), new File(path) );
					} catch (IOException e) {
						test=false;
						e.printStackTrace();
					}
					if(test){
						UploadImage.updateImage(doc, path, tiffFileName, "tiff", searchId);
					}
				}
			break;
		}
		super.ParseResponse(sAction, response, viParseID);
	}
	
	static class HasLotFilter extends FilterResponse{

		private static final long serialVersionUID = -8480920715217639973L;
		
		public HasLotFilter(String key,long searchId){
			super(key, searchId);
			super.setThreshold(new BigDecimal(0.8));
		}
		
		public HasLotFilter(SearchAttributes sa1, String key,long searchId){
			super(sa1, key, searchId);
			super.setThreshold(new BigDecimal(0.8));
		}
		
		public BigDecimal getScoreOneRow(ParsedResponse row){
			DocumentI doc = row.getDocument();
			for (PropertyI pro:doc.getProperties()){ 
				if(pro.hasSubdividedLegal()){
					SubdivisionI subdv = pro.getLegal().getSubdivision();
					if(StringUtils.isNotEmpty(subdv.getLot())){
						return ATSDecimalNumberFormat.ONE;
					}
				}else if(pro.hasTownshipLegal()){
					TownShipI town = pro.getLegal().getTownShip();
					if(StringUtils.isNotEmpty(town.getArbLot())){
						//return ATSDecimalNumberFormat.ONE;
					} 
				}
			}
			return ATSDecimalNumberFormat.ZERO;
		}
		
		@Override
		public String getFilterName() {
			return "EmptyLotFilter";
		}
		
		public String getFilterCriteria() {
			return "EMPTYLOT";
		};
	}
	
	public static void setModulesForAutoSearchGlobal(TSServerInfo serverInfo, long searchId, DTLikeAutomatic dtAuto) {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		dtAuto.prepareApnPerCounty(searchId);
		
		if(global.getSearchType() == Search.AUTOMATIC_SEARCH) {
		
			boolean isUpdate = (global.isSearchProductTypeOfUpdate()) || global.getSa().isDateDown();
			boolean isTimeShare = !StringUtils.isEmpty(global.getSa().getAtribute(SearchAttributes.WEEK));
			//boolean isRefinance = (Products.REFINANCE_PRODUCT == global.getSearchProduct());
			
			String block = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			String lot = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			
			
			TSServerInfoModule module = null;
			
			dtAuto.addAoLookUpSearches(serverInfo, modules, getAllAoAndTaxReferences(global), searchId, isUpdate,isTimeShare);
			
			GenericNameFilter nameFilterOwner 	=  (GenericNameFilter)NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, global.getID(), null );
			nameFilterOwner.setInitAgain(true);
			if(dtAuto instanceof TSServer) {
				nameFilterOwner.setUseSynonymsBothWays(((TSServer)dtAuto).getDataSite().getSiteTypeInt() != GWTDataSite.DT_TYPE);
			} else {
				nameFilterOwner.setUseSynonymsBothWays(true);
			}
			nameFilterOwner.setIgnoreMiddleOnEmpty(true);
			//6132 nameFilterOwner.setIgnoreEmptyMiddleOnCandidat(false);
			FilterResponse[] filtersO 	= { 
					nameFilterOwner, 
					getLegalOrDocTypeFilter( searchId , false), 
					getPinFilter(searchId,true), 
					new LastTransferDateFilter(searchId), 
					new PropertyTypeFilter(searchId)};
			List<FilterResponse> filtersOList = Arrays.asList(filtersO);
			
			FilterResponse[] filtersCrossrefs 	= { 
					getLegalOrDocTypeFilter( searchId , false), 
					getPinFilter(searchId,true),
					new PropertyTypeFilter(searchId)};
			
			if(StringUtils.isNotEmpty(lot)||StringUtils.isNotEmpty(block)){
				List<FilterResponse> grantorGranteeFilterList = new ArrayList<FilterResponse>();
				
				grantorGranteeFilterList.add(nameFilterOwner);
				grantorGranteeFilterList.add(getLegalOrDocTypeFilter( searchId , false));
				grantorGranteeFilterList.add(getPinFilter(searchId,true)); 
				grantorGranteeFilterList.add(new LastTransferDateFilter(searchId)); 
				grantorGranteeFilterList.add(new PropertyTypeFilter(searchId));
				grantorGranteeFilterList.add(new HasLotFilter("", searchId));
				
				dtAuto.addGrantorGranteeSearch( modules, serverInfo, SearchAttributes.OWNER_OBJECT, grantorGranteeFilterList);
			}
			
			dtAuto.addIteratorModule(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId, isUpdate, isTimeShare);
			
			if(!isTimeShare){
				dtAuto.addIteratorModule(serverInfo, modules, TSServerInfo.ARB_MODULE_IDX, searchId, isUpdate,isTimeShare);
				
				dtAuto.addIteratorModule(serverInfo, modules, TSServerInfo.SECTION_LAND_MODULE_IDX, searchId, isUpdate,isTimeShare);
				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, dtAuto.prepareApnPerCounty(searchId));
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				PinFilterResponse pinFilter = getPinFilter(searchId,true);
				if(pinFilter!=null){
					module.addFilter(pinFilter);
				}
				modules.add(module);
				
				ArrayList<NameI> searchedNames = dtAuto.addNameSearch(  modules, serverInfo, SearchAttributes.OWNER_OBJECT, null,  filtersOList );
				
				dtAuto.addOCRSearch( modules, serverInfo, getLegalOrDocTypeFilter(searchId , true));
				
				dtAuto.addNameSearch( modules, serverInfo,SearchAttributes.OWNER_OBJECT, searchedNames==null?new ArrayList<NameI>():searchedNames, filtersOList);
				
			}else{
				List<FilterResponse> filtersOListTS = new ArrayList<FilterResponse>();
				filtersOListTS.add( new DocTypeAdvancedFilter(searchId));
				filtersOListTS.addAll(filtersOList);
				
				ArrayList<NameI> searchedNames = dtAuto.addNameSearch(  modules, serverInfo, SearchAttributes.OWNER_OBJECT, null,  filtersOListTS );
				
				dtAuto.addOCRSearch( modules, serverInfo, getLegalOrDocTypeFilter(searchId , true));
				
				dtAuto.addNameSearch( modules, serverInfo,SearchAttributes.OWNER_OBJECT, searchedNames==null?new ArrayList<NameI>():searchedNames, filtersOListTS);
			}
			
			if( isUpdate ){
				
				GenericNameFilter nameFilterBuyer 	=  ( GenericNameFilter ) NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, global.getID(), null );
				nameFilterBuyer.setIgnoreMiddleOnEmpty( true );
				nameFilterBuyer.setUseSynonymsBothWays(true);
				nameFilterBuyer.setInitAgain(true);
				
				FilterResponse[] filtersB 	= { nameFilterBuyer, new DocTypeAdvancedFilter(searchId)};
				dtAuto.addNameSearch( modules, serverInfo, SearchAttributes.BUYER_OBJECT, null, Arrays.asList(filtersB));
			}
			  
			GenericRuntimeIterator<InstrumentI> instrumentIterator = dtAuto.getInstrumentIterator(false);
			
			if(instrumentIterator!=null){
				final int []REFERECE_IDXS = { TSServerInfo.INSTR_NO_MODULE_IDX, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX};
				for(int moduleIdx:REFERECE_IDXS){
					module = new TSServerInfoModule(serverInfo.getModule(moduleIdx));
					module.clearSaKeys();
					module.addIterator(dtAuto.getInstrumentIterator(false));
					for(FilterResponse filter:filtersCrossrefs){
						module.addFilter(filter);
					}
					if (isUpdate) {
						module.addFilter(new BetweenDatesFilterResponse(searchId));
					}
					modules.add(module);
				}
			}
			
			//plat map fake search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.LD_BOOKNO);
			module.clearSaKeys();
			PlatIterator it = new PlatIterator(searchId, dtAuto);
			module.addIterator(it);
			if (!isUpdate) {
				modules.add(module);
			}
			
			//parcel map fake search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.LD_PARCELNO_MAP);
			module.clearSaKeys();
			it = new PlatIterator(searchId, dtAuto);
			module.addIterator(it);
			if (!isUpdate) {
				modules.add(module);
			}
			
			//parcel map fake search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.LD_PARCELNO_CONDO);
			module.clearSaKeys();
			it = new PlatIterator(searchId, dtAuto);
			module.addIterator(it);
			if (!isUpdate) {
				modules.add(module);
			}
			
			dtAuto.addAssesorMapSearch(serverInfo, modules, isUpdate);
			
			//subdivision map fake search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.LD_SUBDIVISION);
			module.clearSaKeys();
			it = new PlatIterator(searchId, dtAuto);
			module.addIterator(it);
			if (!isUpdate) {
				modules.add(module);
			}
			
			dtAuto.addRelatedSearch(serverInfo, modules);
		}
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		setModulesForAutoSearchGlobal(serverInfo, searchId, this);
	}
	
	public static class PersonalDataStruct implements Cloneable{
		String type = "";
		String block = "";
		String unit = 	"";
		private String platBook	=	"";
		private String platPage=	"";
		String platInst=	"";
		private String section="";
		private String township="";
		private String range="";
		private String arb="";
		String arbLot="";
		String arbBlock="";
		String arbBook="";
		String arbPage="";
		String quarterOrder="";
		String quarterValue="";
		String lot	=	"";
		String lotThrough = "";
		String subLot = "";
		String platInstrYear = "";
		String ncbNumber = "";
		String subdivisionName = "";
		String tract = "";
		
		public PersonalDataStruct(String type){
			this.type = type;
		}
		
		@Override
		protected Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
		
		private static boolean equalsIgnoreStartLetter(char letter, String a, String b){
			String letString = (letter+"").toUpperCase();
			a = a.toUpperCase();
			b = b.toUpperCase();
			
			if(a.equals(b)){
				return true;
			}
			
			if(a.length()>1 && a.startsWith(letString)){
				a = a.substring(1);
			}
			
			if(b.length()>1 && b.startsWith(letString)){
				b = b.substring(1);
			}
			
			return a.equals(b);
		}
		
		public boolean equalsSubdivided(PersonalDataStruct struct) {
			boolean checkSublot = true;
			
			//ignore sublot equivalence if any of the sublots is empty
			if(StringUtils.isNotEmpty(struct.subLot) && StringUtils.isNotEmpty(this.subLot))
				checkSublot = this.subLot.equals(struct.subLot);
				
			return 
				equalsIgnoreStartLetter('B',this.block,struct.block)
				&&(equalsIgnoreStartLetter('L',this.lot,struct.lot)||equalsIgnoreStartLetter('U',this.lot,struct.lot))
				&&(equalsIgnoreStartLetter('P',this.getPlatBook(),struct.getPlatBook())||equalsIgnoreStartLetter('C',this.getPlatBook(),struct.getPlatBook())||equalsIgnoreStartLetter('U',this.getPlatBook(),struct.getPlatBook()))
				&&this.getPlatPage().equals(struct.getPlatPage())
				&&this.unit.equals(struct.unit)
				&&checkSublot
			&&this.platInstrYear.equals(struct.platInstrYear);
		}
		
		public boolean equalsStrictSubdivided(PersonalDataStruct struct) {
			boolean checkSublot = true;
			
			//ignore sublot equivalence if any of the sublots is empty
			if(StringUtils.isNotEmpty(struct.subLot) && StringUtils.isNotEmpty(this.subLot))
				checkSublot = this.subLot.equals(struct.subLot);
				
			return 
				this.block.equals(struct.block)
				&&(this.lot.equals(struct.lot)||this.lot.equals(struct.lot))
				&&(this.getPlatBook().equals(struct.getPlatBook())||this.getPlatBook().equals(struct.getPlatBook())||this.getPlatBook().equals(struct.getPlatBook()))
				&&this.getPlatPage().equals(struct.getPlatPage())
				&&this.unit.equals(struct.unit)
				&&checkSublot
			&&this.platInstrYear.equals(struct.platInstrYear);
		}
		
		public boolean equalsSectional(PersonalDataStruct struct) {
			return this.getSection().equals(struct.getSection())&&this.getTownship().equals(struct.getTownship())&&this.getRange().equals(struct.getRange())
			&&this.quarterOrder.equals(struct.quarterOrder)&&this.quarterValue.equals(struct.quarterValue);
		}
		
		public boolean equalsSectionalAndPlat(PersonalDataStruct struct) {
			return this.getSection().equals(struct.getSection()) && this.getTownship().equals(struct.getTownship()) && this.getRange().equals(struct.getRange())
						&& this.quarterOrder.equals(struct.quarterOrder) && this.quarterValue.equals(struct.quarterValue)
						&& this.platBook.equals(struct.platBook) && this.platPage.equals(struct.platPage);
		}
		
		public boolean equalsArb(PersonalDataStruct struct) {
			return this.getSection().equals(struct.getSection())&&this.getTownship().equals(struct.getTownship())&&this.getRange().equals(struct.getRange())
			&&this.quarterOrder.equals(struct.quarterOrder)&&this.getArb().equals(struct.getArb());
		}

		public boolean equalsArbExtended(PersonalDataStruct struct) {
			return this.arbBlock.equals(struct.arbBlock)&&this.arbLot.equals(struct.arbLot)&&this.arbBook.equals(struct.arbBook)
			&&this.arbPage.equals(struct.arbPage);
		}
		
		public boolean isPlated() {
			return !StringUtils.isEmpty(getPlatBook())&&!StringUtils.isEmpty(getPlatPage())&&(!StringUtils.isEmpty(lot)||!StringUtils.isEmpty(block)||!StringUtils.isEmpty(subLot));
		}

		public boolean isArb() {
			return !StringUtils.isEmpty(getSection())&&!StringUtils.isEmpty(getTownship())&&!StringUtils.isEmpty(getRange())&&!StringUtils.isEmpty(quarterOrder)
			&&!StringUtils.isEmpty(getArb());
		}

		public boolean isArbExtended() {
			return !StringUtils.isEmpty(arbBlock)&&!StringUtils.isEmpty(arbLot)&&!StringUtils.isEmpty(arbBook)&&!StringUtils.isEmpty(arbPage);
		}
		
		public boolean isSectional() {
			return !StringUtils.isEmpty(getSection())&&!StringUtils.isEmpty(getTownship())&&!StringUtils.isEmpty(getRange())&&!StringUtils.isEmpty(quarterOrder)
			&&StringUtils.isEmpty(getArb());
		}

		public String getSection() {
			return section;
		}

		public void setSection(String section) {
			this.section = section;
		}

		public String getTownship() {
			return township;
		}

		public void setTownship(String township) {
			this.township = township;
		}

		public String getRange() {
			return range;
		}

		public void setRange(String range) {
			this.range = range;
		}

		public String getBlock() {
			return block;
		}

		public void setBlock(String block) {
			this.block = block;
		}

		public String getNcbNumber() {
			return ncbNumber;
		}

		public void setNcbNumber(String ncbNumber) {
			this.ncbNumber = ncbNumber;
		}
		
		public String getTract(){
			return tract;
		}
		
		public void setTract(String tract){
			this.tract = tract;
		}
		
		public String getSubdivisionName() {
			return subdivisionName;
		}

		public void setSubdivisionName(String subdivisionName) {
			this.subdivisionName = subdivisionName;
		}
		
		public String getLot() {
			return lot;
		}

		public void setLot(String lot) {
			this.lot = lot;
		}

		public String getPlatBook() {
			return platBook;
		}

		public void setPlatBook(String platBook) {
			this.platBook = platBook;
		}

		public String getPlatPage() {
			return platPage;
		}

		public void setPlatPage(String platPage) {
			this.platPage = platPage;
		}

		public String getArb() {
			return arb;
		}

		public void setArb(String arb) {
			this.arb = arb;
		}
	}
	
	public static void preparePersonalStructForCounty(PersonalDataStruct str, long searchId){
		County countyObj = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
		String county = countyObj.getName().toLowerCase();
		String state = countyObj.getState().getStateAbv();
		
		if("FL".equalsIgnoreCase(state)){
			boolean testSpecialLotAndBlock = "orange".equals(county.toLowerCase())||"lake".equals(county.toLowerCase())||"osceola".equals(county.toLowerCase())||"seminole".equals(county.toLowerCase());
			if(testSpecialLotAndBlock){
				if( !StringUtils.isEmpty(str.lot) && !str.lot.startsWith("L") && !str.lot.startsWith("U") ){
					if( !(str.lot.length()>=2 && org.apache.commons.lang.StringUtils.isAlpha(str.lot.substring(0,1)))){
						str.lot = "L" + str.lot;			
					}
				}
				if( !StringUtils.isEmpty(str.block) ){
					if( !(str.block.length()>=2 && org.apache.commons.lang.StringUtils.isAlpha(str.block.substring(0,1)))){
						str.block = "B" + str.block;			
					}
				}
			}
		}
	}
	
	protected static void addLegalStructItemsUsingAoAndTrLots(List<PersonalDataStruct> legalStructList, String[] allAoAndTrlots,long searchId, boolean isDTG) {
		PersonalDataStruct first =  getFirstPlatedStruct(legalStructList);
		if(first!=null){
			if (isDTG) {
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
				if (org.apache.commons.lang.StringUtils.isNotEmpty(lots)){
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
					if(!testIfExist(legalStructList,n,searchId)){
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
	protected static void addLegalStructAtInterval(List<PersonalDataStruct> list, PersonalDataStruct newItem) {
		
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
		return ( (p1.type).equals(p2.type) &&
				 (p1.block).equals(p2.block) &&
				 (p1.unit).equals(p2.unit) &&
				 (p1.getPlatBook()).equals(p2.getPlatBook()) &&
				 (p1.getPlatPage()).equals(p2.getPlatPage()) &&
				 (p1.platInst).equals(p2.platInst) &&
				 (p1.getSection()).equals(p2.getSection()) &&
				 (p1.getTownship()).equals(p2.getTownship()) &&
				 (p1.getRange()).equals(p2.getRange()) &&
				 (p1.getArb()).equals(p2.getArb()) &&
				 (p1.arbLot).equals(p2.arbLot) &&
				 (p1.arbBlock).equals(p2.arbBlock) &&
				 (p1.arbBook).equals(p2.arbBook) &&
				 (p1.arbPage).equals(p2.arbPage) &&
				 (p1.quarterOrder).equals(p2.quarterOrder) &&
				 (p1.quarterValue).equals(p2.quarterValue) &&
				 (p1.subLot).equals(p2.subLot) &&
				 (p1.platInstrYear).equals(p2.platInstrYear) ); 
	}
	
	private static PersonalDataStruct getFirstPlatedStruct(List<PersonalDataStruct> list){
		for(PersonalDataStruct struct:list){
			if(struct.isPlated()){
				return struct;
			}
		}
		return null;
	}
	
	private static PersonalDataStruct getFirstArbExtendedStruct(List<PersonalDataStruct> list){
		for(PersonalDataStruct struct:list){
			if(struct.isArbExtended()){
				return struct;
			}
		}
		return null;
	}
	
	private static PersonalDataStruct getFirstSectionalStruct(List<PersonalDataStruct> list){
		for(PersonalDataStruct struct:list){
			if(!struct.isPlated()){
				return struct;
			}
		}
		return null;
	}
	
	protected static boolean isPlatedMultyLotAndIsSectionalMultyQV(List<PersonalDataStruct> legalStructList){
		List<PersonalDataStruct>  list1 = new ArrayList<PersonalDataStruct>();
		List<PersonalDataStruct> list2 = new ArrayList<PersonalDataStruct>();
		
		for(PersonalDataStruct p :legalStructList){
			if(p.isPlated()){
				list1.add(p);
			}else if(p.isSectional()||p.isArb()){
				list2.add(p);
			}
		}
		
		return isPlatedMultyLot(list1) && isSectionalMultyQv(list2);
	}
	
	protected static boolean isSectionalMultyQv(List<PersonalDataStruct> legalStructList){
		boolean isSectionalMultyQv = true;
		
		if(legalStructList == null || legalStructList.size()==0){
			isSectionalMultyQv = false;
		}
		
		for(PersonalDataStruct p:legalStructList){
			if(p.isPlated()&&!p.isSectional()){
				isSectionalMultyQv = false;
				break;
			}
		}
		
		if(isSectionalMultyQv){
			PersonalDataStruct first =  getFirstSectionalStruct(legalStructList);
			
			if(first==null){
				isSectionalMultyQv =  false;
			}else{
				for(PersonalDataStruct p:legalStructList){
					if(!p.getSection().equalsIgnoreCase(first.getSection())||!p.getTownship().equalsIgnoreCase(first.getTownship())||!p.getRange().equalsIgnoreCase(first.getRange())
							||!p.quarterOrder.equalsIgnoreCase(first.quarterOrder)){
						isSectionalMultyQv =  false;
						break;
					}
				}
			}
		}
		
		return isSectionalMultyQv;
	}
	
	protected static boolean isArbExtended(List<PersonalDataStruct> legalStructList){
		boolean isArbExtended = true;
		
		if(legalStructList == null || legalStructList.size()==0){
			isArbExtended = false;
		}
		
		for(PersonalDataStruct p:legalStructList){
			if(!p.isArbExtended()&&p.isPlated()){
				isArbExtended =  false;
				break;
			}
		}
		
		if(isArbExtended){
			PersonalDataStruct first =  getFirstArbExtendedStruct(legalStructList);
			
			if(first==null){
				isArbExtended =  false;
			}else{
				for(PersonalDataStruct p:legalStructList){
					if((!p.arbBlock.equalsIgnoreCase(first.arbBlock)&&StringUtils.isEmpty(first.arbBlock)&&StringUtils.isEmpty(p.arbBlock))
							||!p.arbBook.equalsIgnoreCase(first.arbBook)||!p.arbPage.equalsIgnoreCase(first.arbPage)){
						isArbExtended =  false;
						break;
					}
				}
			}
		}
		return isArbExtended ;
	}
	
	protected static boolean isPlatedMultyLot(List<PersonalDataStruct> legalStructList) {
		boolean isPlatedMultyLot = true;
		
		if(legalStructList == null || legalStructList.size()==0){
			isPlatedMultyLot = false;
		}
		
		for(PersonalDataStruct p:legalStructList){
			if(!p.isPlated()&&p.isSectional()){
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
					if((!p.block.equalsIgnoreCase(first.block)&&StringUtils.isEmpty(first.block)&&StringUtils.isEmpty(p.block))
							|| !platBookFlexibleCheck(p.getPlatBook(), first.getPlatBook())
							|| !platPageFlexibleCheck(p.getPlatPage(), first.getPlatPage())){
						isPlatedMultyLot =  false;
						break;
					}
				}
			}
		}
		return isPlatedMultyLot ;
	}
	
	private static boolean platBookFlexibleCheck(String book1, String book2) {
		if(org.apache.commons.lang.StringUtils.isBlank(book1)) {
			if(org.apache.commons.lang.StringUtils.isBlank(book2)) {
				return true;
			} else {
				return false;
			}
		} else {
			if(org.apache.commons.lang.StringUtils.isBlank(book2)) {
				return false;
			} else {
				book1 = book1.toUpperCase();
				book2 = book2.toUpperCase();
				if(book1.equals(book2)) {
					return true;
				} else if(org.apache.commons.lang.StringUtils.endsWith(book1, book2)) {
					book1 = book1.substring(0, book1.length() - book2.length());
					if(book1.length() == 1 && Character.isLetter(book1.charAt(0))) {
						return true;
					}
				} else if(org.apache.commons.lang.StringUtils.endsWith(book2, book1)) {	
					book2 = book2.substring(0, book2.length() - book1.length());
					if(book2.length() == 1 && Character.isLetter(book2.charAt(0))) {
						return true;
					}
				}
				return false;
			}
		}
	}
	
	private static boolean platPageFlexibleCheck(String page1, String page2) {
		if (org.apache.commons.lang.StringUtils.isBlank(page1)) {
			if (org.apache.commons.lang.StringUtils.isBlank(page2)) {
				return true;
			} else {
				return false;
			}
		} else {
			if (org.apache.commons.lang.StringUtils.isBlank(page2)) {
				return false;
			} else {
				page1 = page1.toUpperCase();
				page2 = page2.toUpperCase();
				if (page1.equals(page2)) {
					return true;
				} else if(org.apache.commons.lang.StringUtils.startsWith(page1, page2)) {
					page1 = page1.substring(page2.length(), page1.length());
					if (page1.length() == 1 && Character.isLetter(page1.charAt(0))) {
						return true;
					}
				} else if(org.apache.commons.lang.StringUtils.startsWith(page2, page1)) {	
					page2 = page2.substring(page1.length(), page2.length());
					if (page2.length() == 1 && Character.isLetter(page2.charAt(0))) {
						return true;
					}
				}
				return false;
			}
		}
	}

	protected static boolean testIfExistsForCounty(List<PersonalDataStruct> tempPersonal, PersonalDataStruct struct,long searchId, String county){
		if(county.equalsIgnoreCase("broward")){
			return FLBrowardDT.testIfExistsBroward(tempPersonal, struct, searchId);
		}
		
		return false;
	}
	
	protected static boolean testIfExist(List<PersonalDataStruct> legalStruct2, PersonalDataStruct l,long searchId) {
		preparePersonalStructForCounty(l,searchId);
		
		if("arb".equalsIgnoreCase(l.type)){
			for(PersonalDataStruct p:legalStruct2){
				preparePersonalStructForCounty(p,searchId);
				if(p.isArb()){
					if(l.equalsArb(p)){
						return true;
					}
				}
			}
		}else if("sectional".equalsIgnoreCase(l.type)){
			for(PersonalDataStruct p:legalStruct2){
				if(p.isSectional()||p.isArb()){
					preparePersonalStructForCounty(p,searchId);
					if(l.equalsSectional(p)){
						return true;
					}
				}
			}
		}else if("subdivided".equalsIgnoreCase(l.type)){
			for(PersonalDataStruct p:legalStruct2){
				if(p.isPlated()){
					preparePersonalStructForCounty(p,searchId);
					if(l.equalsSubdivided(p)){
						return true;
					}
				}
			}
		}
		return false;
	}

	protected static class PlatIterator extends GenericRuntimeIterator<PersonalDataStruct> {
	
		private String extraStringForsection = "";
	
		private static final long serialVersionUID = 823434519L;
		
		DTLikeAutomatic dtLike;
		
		PlatIterator(long searchId, DTLikeAutomatic dtLike) {
			super(searchId);
			this.dtLike = dtLike;
		}
		
		protected List<PersonalDataStruct> createDerrivations(){
			List<PersonalDataStruct> list = LegalDescriptionIterator.createDerivationInternal(searchId);

			return list;
		}
		
		protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
			
			switch(module.getModuleIdx()){
				case TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX:
					if(module.getSaObjKey().equals(SearchAttributes.LD_BOOKNO)){
						dtLike.addPlatMapSearch(module,str);
					}else if(module.getSaObjKey().equals(SearchAttributes.LD_BOOKNO_1)){
						dtLike.addAssesorMapSearch(module, str, extraStringForsection);
					}else if(module.getSaObjKey().equals(SearchAttributes.LD_SUBDIVISION)){
						dtLike.addSubdivisionMapSearch(module,str);
					}else if(module.getSaObjKey().equals(SearchAttributes.LD_PARCELNO_MAP)){
						dtLike.addParcelMapSearch(module, str);
					}else if(module.getSaObjKey().equals(SearchAttributes.LD_PARCELNO_CONDO)){
						dtLike.addCondoMapSearch(module, str);
					}
				break;
			}
		}

		public void setExtraStringForSection(String str) {
			extraStringForsection = str;
		}
	}
	
	
	static  protected class InstrumentsIterator extends GenericRuntimeIterator<InstrumentI> {
		 
		private static final long serialVersionUID = 823434519L;
		private County county ;
		
		
		InstrumentsIterator(long searchId) {
			super(searchId);
			county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
		}
		
		
		private static boolean isEnabledForCounty(County county){
			if("Flagler".equalsIgnoreCase(county.getName())||"Larimer".equalsIgnoreCase(county.getName())){
				return false;
			}
			return true;
		}
		
		protected List<InstrumentI> createDerrivations(){
			
			DocumentsManagerI docM = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
			List<InstrumentI> instruments = new ArrayList<InstrumentI>();
			
			if(isEnabledForCounty(county)){
				try{
					docM.getAccess();
					List<DocumentI> docs = docM.getDocumentsWithDataSource(false,"DT");
					for(DocumentI doc: docs){
						if(doc instanceof RegisterDocumentI){
							RegisterDocumentI regDoc = (RegisterDocumentI)doc;
							if(regDoc.getGrantee()==null||regDoc.getGrantor()==null||
									regDoc.getGrantee().size()==0||regDoc.getGrantor().size()==0){
								instruments.add(regDoc.getInstrument().clone());
							}
						}
					}
				}finally{
					docM.releaseAccess();
				}
			}
			
			return instruments;
		}
		
		protected void loadDerrivation(TSServerInfoModule module, InstrumentI str){
			InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().clearVisitedLinks();
			InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().clearValidatedLinks();
			InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().clearClickedDocuments();
			InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().clearRecursivedAnalisedLink();
			InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().clearRecursiveAnalisedLinks();
			InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().removeAllVisitedDocs();
			switch(module.getModuleIdx()){
				case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
					if(!str.hasYear()||!str.hasInstrNo()){
						module.setData(0,str.getBook());
						module.setData(1,str.getPage());
					}
				break;
				case TSServerInfo.INSTR_NO_MODULE_IDX:
					module.setData(0,str.getInstno());
					module.setData(2,str.getYear()+"");
				break;
			}
		}

	}
	
	protected static List<RegisterDocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch, int siteType, String stateAbbrev){
		final List<RegisterDocumentI> ret = new ArrayList<RegisterDocumentI>();
		
		List<RegisterDocumentI> listRodocs = m.getRoLikeDocumentList();
		DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
		
		SearchAttributes sa	= search.getSa();
		PartyI owner 		= sa.getOwners();
		
		
		for(RegisterDocumentI doc:listRodocs){
			boolean found = false;
			for(PropertyI prop:doc.getProperties()){
				if((doc.isOneOf("MORTGAGE","TRANSFER","RELEASE")&& applyNameMatch)
						|| (doc.isOneOf("MORTGAGE","TRANSFER")&& !applyNameMatch)){
					if(prop.hasSubdividedLegal()){
						SubdivisionI sub = prop.getLegal().getSubdivision();
						PersonalDataStruct ret1 = new PersonalDataStruct("subdivided");
						ret1.lot = sub.getLot();
						ret1.block = sub.getBlock();
						ret1.setPlatBook(sub.getPlatBook());
						ret1.setPlatPage(sub.getPlatPage());
						ret1.platInst = sub.getPlatInstrument();
						ret1.platInstrYear = sub.getPlatInstrumentYear();
						
						boolean nameMatched = false;
						
						if(applyNameMatch){
							if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
									GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						
						if( (nameMatched||!applyNameMatch) && !StringUtils.isEmpty(ret1.lot) &&  !StringUtils.isEmpty(ret1.getPlatBook()) && !StringUtils.isEmpty(ret1.getPlatPage()) ){
							ret.add(doc);
							found = true;
							break;
						}
					}
					else if(prop.hasTownshipLegal()){
						TownShipI sub = prop.getLegal().getTownShip();
						PersonalDataStruct ret1 = new PersonalDataStruct("sectional");
						ret1.setSection(sub.getSection());
						ret1.setTownship(sub.getTownship());
						ret1.setRange(sub.getRange());
						int qo = sub.getQuarterOrder();
						String qv = sub.getQuarterValue();
						if(qo>0){
							ret1.quarterOrder = qo+"";
						}
						ret1.quarterValue = qv;
						ret1.setArb(sub.getArb());
						ret1.arbLot = sub.getArbLot();
						ret1.arbBlock = sub.getArbBlock();
						ret1.arbPage = sub.getArbPage();
						ret1.arbBook = sub.getArbBook();
						boolean nameMatched = false;
						
						if(applyNameMatch){
							if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
									GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						
						if((nameMatched||!applyNameMatch) && isCompleteLegal(ret1, qo, qv, siteType, stateAbbrev)){
							ret.add(doc);
							found = true;
							break;
						}
					}
				}
			}
			if(found){
				break;
			}
		}
		
		return ret;
	}

	protected static boolean isCompleteLegal(PersonalDataStruct ret1, int qo, String qv, int siteType, String stateAbbrev){
		if ((siteType != -1 && siteType == GWTDataSite.DG_TYPE) && (StringUtils.isNotEmpty(stateAbbrev) && "FL".equals(stateAbbrev))){
			return (StringUtils.isNotEmpty(ret1.getSection()) && StringUtils.isNotEmpty(ret1.getTownship()) && StringUtils.isNotEmpty(ret1.getRange()));
		} else{
			return ((StringUtils.isNotEmpty(qv)||qo>0||StringUtils.isNotEmpty(ret1.getArb())) 
						&& !StringUtils.isEmpty(ret1.getSection())&&!StringUtils.isEmpty(ret1.getTownship())&&!StringUtils.isEmpty(ret1.getRange()))
					|| (!StringUtils.isEmpty(ret1.arbBook)&&!StringUtils.isEmpty(ret1.arbPage) && !(StringUtils.isEmpty(ret1.arbLot)&&StringUtils.isEmpty(ret1.arbBlock)));
			}
	}
	
	
	static protected class LegalDescriptionIterator extends GenericRuntimeIterator<PersonalDataStruct> {
		
		private static final long serialVersionUID = 8989586891817117069L;
		
		LegalDescriptionIterator(long searchId) {
			super(searchId);
		}
		
		public boolean isDTG(){
			int siteType = -1;
			try {
				siteType = HashCountyToIndex.getCrtServer(searchId, false).getSiteTypeInt();
			} catch (BaseException be) {}
			return (siteType == GWTDataSite.DG_TYPE);
		}		 
		
		@SuppressWarnings("unchecked")
		static  List<PersonalDataStruct> createDerivationInternal(long searchId){
			
			boolean isDTG;
			int siteType = -1;
			String stateAbbrev = "";
			try {
				siteType = HashCountyToIndex.getCrtServer(searchId, false).getSiteTypeInt();
			} catch (BaseException be) {
				logger.error("Exception when trying to provide siteType on FLSubdividedBasedDASLDT: " + searchId);
			}
			isDTG = (siteType == GWTDataSite.DG_TYPE);
			
			if (org.apache.commons.lang.StringUtils.isEmpty(stateAbbrev)){
				try {
					stateAbbrev = HashCountyToIndex.getCrtServer(searchId, false).getStateAbbreviation();
				} catch (BaseException e) {
					logger.error("Exception when trying to provide stateAbbrev on FLSubdividedBasedDASLDT: " + searchId);
				}
			}
			
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI m = global.getDocManager();
			List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)global.getAdditionalInfo("FL_DT_LOOK_UP_DATA");
			Set<String>  multipleLegals = (Set<String>)global.getAdditionalInfo("FL_MULTIPLE_LEGAL_INSTR");
			if(multipleLegals==null ){
				multipleLegals = new HashSet<String>();
			}
			
			String platBookAO = global.getSa().getAtribute(SearchAttributes.LD_BOOKNO);
			String platPageAO = global.getSa().getAtribute(SearchAttributes.LD_PAGENO);
			
			String subLotNo = global.getSa().getAtribute(SearchAttributes.LD_SUBLOT);
			
			String aoAndTrLots = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			HashSet<String> allAoAndTrlots = new HashSet<String>();
			
			String aoAndTrBloks = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			HashSet<String> allAoAndTrBloks = new HashSet<String>();
			
			String week = global.getSa().getAtribute(SearchAttributes.WEEK);
			boolean isTimeShare = !StringUtils.isEmpty(week);
			
			if(aoAndTrLots.contains(",")||aoAndTrLots.contains(" ")||aoAndTrLots.contains("-")||aoAndTrLots.contains(";")){
				if(!StringUtils.isEmpty(aoAndTrLots)){
					for (LotInterval interval:LotMatchAlgorithm.prepareLotInterval(aoAndTrLots)) {
						allAoAndTrlots.addAll(interval.getLotList());
					}
				}
			}else{
				if(!StringUtils.isEmpty(aoAndTrLots)){
					allAoAndTrlots.add(aoAndTrLots);
				}
			}
			
			if(!StringUtils.isEmpty(aoAndTrBloks)){
				for (LotInterval interval:LotMatchAlgorithm.prepareLotInterval(aoAndTrBloks)) {
					allAoAndTrBloks.addAll(interval.getLotList());
				}
			}
			
			CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
			String county = ci.getCurrentCounty().getName();
			
			boolean first = false;
			if(legalStructList==null){
				first = true;
				legalStructList = new ArrayList<PersonalDataStruct>();
				
				try{
					m.getAccess();
					List<RegisterDocumentI> listRodocs = getGoodDocumentsOrForCurrentOwner(m,global,true, siteType, stateAbbrev);
					
					if(listRodocs==null||listRodocs.size()==0){
						listRodocs = getGoodDocumentsOrForCurrentOwner(m, global, false, siteType, stateAbbrev);
					}
					
					if(listRodocs==null||listRodocs.size()==0){
						for(DocumentI doc:m.getDocumentsWithDataSource(true, "DT","DG")){
							if(doc instanceof RegisterDocumentI){
								listRodocs.add((RegisterDocumentI)doc);
							}
						}
					}
						
					DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
					
					boolean atLeastOneFromDtDg = false;
					
					
					for( RegisterDocumentI reg: listRodocs){
						if(!reg.isOneOf(DocumentTypes.PLAT,
										DocumentTypes.RESTRICTION,
										DocumentTypes.EASEMENT,
										DocumentTypes.MASTERDEED,
										DocumentTypes.COURT,
										DocumentTypes.LIEN,
										DocumentTypes.CORPORATION,
										DocumentTypes.AFFIDAVIT, 
										DocumentTypes.CCER)
							||( county.equalsIgnoreCase("duval")
									&& !reg.isOneOf(DocumentTypes.RESTRICTION,
												DocumentTypes.EASEMENT,
												DocumentTypes.MASTERDEED,
												DocumentTypes.COURT,
												DocumentTypes.LIEN,
												DocumentTypes.CORPORATION,
												DocumentTypes.AFFIDAVIT, 
												DocumentTypes.CCER) )
						){
							
							List<PersonalDataStruct> tempLegalStructListPerDocument = new ArrayList<PersonalDataStruct>();
							for (PropertyI prop: reg.getProperties()){
								if(prop.hasLegal()){
									LegalI legal = prop.getLegal();
									
									if(legal.hasSubdividedLegal()){
										
										PersonalDataStruct legalStructItem = new PersonalDataStruct("subdivided");
										SubdivisionI subdiv = legal.getSubdivision();
										
										String block = subdiv.getBlock();
										String lot = subdiv.getLot();
										String lotThrough = subdiv.getLotThrough();
										String subLot = subdiv.getSubLot();
										String platBook = subdiv.getPlatBook();
										String platPage = subdiv.getPlatPage();
										String platInst = subdiv.getPlatInstrument();
										String unit = subdiv.getUnit();
										legalStructItem.unit = StringUtils.isEmpty(unit)?"":unit;
										legalStructItem.subLot = org.apache.commons.lang.StringUtils.defaultString(subLot);
										legalStructItem.block = StringUtils.isEmpty(block)?"":block;
										legalStructItem.lotThrough = StringUtils.isEmpty(lotThrough)?"":lotThrough;
										legalStructItem.setPlatBook(StringUtils.isEmpty(platBook)?"":platBook);
										legalStructItem.setPlatPage(StringUtils.isEmpty(platPage)?"":platPage);
										legalStructItem.platInst = StringUtils.isEmpty(platInst)?"":platInst;
										legalStructItem.platInstrYear = subdiv.getPlatInstrumentYear();
										
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
														preparePersonalStructForCounty(legalStructItemA,searchId);
														multipleLegals.add(reg.prettyPrint());
														tempLegalStructListPerDocument.add(legalStructItemA);
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
										
										preparePersonalStructForCounty(legalStructItem,searchId);
										multipleLegals.add(reg.prettyPrint());
										tempLegalStructListPerDocument.add(legalStructItem);
									}
									
									if(legal.hasTownshipLegal()){
										PersonalDataStruct legalStructItem = new PersonalDataStruct("sectional");
										TownShipI township = legal.getTownShip();
										
										String arb = township.getArb();
										String sec = township.getSection();
										String tw = township.getTownship();
										String rg = township.getRange();
										int qo = township.getQuarterOrder();
										String qv = township.getQuarterValue();
										String arbLot = township.getArbLot();
										String arbBlock = township.getArbBlock();
										String arbBook = township.getArbBook();
										String arbPage = township.getArbPage();
										
										legalStructItem.setArb(StringUtils.isEmpty(arb)?"":arb);
										legalStructItem.setSection(StringUtils.isEmpty(sec)?"":sec);
										legalStructItem.setTownship(StringUtils.isEmpty(tw)?"":tw);
										legalStructItem.setRange(StringUtils.isEmpty(rg)?"":rg);
										
										legalStructItem.arbLot = StringUtils.isEmpty(arbLot)?"":arbLot;
										legalStructItem.arbBlock = StringUtils.isEmpty(arbBlock)?"":arbBlock;
										legalStructItem.arbBook = StringUtils.isEmpty(arbBook)?"":arbBook;
										legalStructItem.arbPage = StringUtils.isEmpty(arbPage)?"":arbPage;
										
										legalStructItem.quarterValue = StringUtils.isEmpty(qv)?"":qv;
										legalStructItem.quarterOrder = String.valueOf(qo<=0?"":qo);
										
										multipleLegals.add(reg.prettyPrint());
										tempLegalStructListPerDocument.add(legalStructItem);
									}
								}
							}
							if(allAoAndTrlots.size()>0){
								List<PersonalDataStruct> aoAndTrMatches = new ArrayList<PersonalDataStruct>();
								for(PersonalDataStruct item:tempLegalStructListPerDocument){
									if(!StringUtils.isEmpty(item.lot)){
										for(String lt:allAoAndTrlots){
											if(GenericLegal.computeScoreInternal("lot", lt, item.lot, false, false)>=0.8
													||GenericLegal.computeScoreInternal("lot", item.lot, lt, false, false)>=0.8){
												aoAndTrMatches.add(item);
											}
										}
									}
								}
								if(aoAndTrMatches.size()>0){
									tempLegalStructListPerDocument.clear();
									tempLegalStructListPerDocument.addAll(aoAndTrMatches);
								}
							}
							
							if(!atLeastOneFromDtDg && !tempLegalStructListPerDocument.isEmpty()) {
								atLeastOneFromDtDg = "DT".equals(reg.getDataSource()) || "DG".equals(reg.getDataSource());
							}
							
							for(PersonalDataStruct item:tempLegalStructListPerDocument){
								if(!testIfExist(legalStructList, item, searchId)){
									if (isDTG) {
										addLegalStructAtInterval(legalStructList, item);
									} else {
										legalStructList.add(item);
									}
								}
							}
						}
					}
					if(isTimeShare){
						for(PersonalDataStruct pers:legalStructList){
							String unit = global.getSa().getAtribute(SearchAttributes.P_STREETUNIT);
							String lot = week;
							String block = unit;
							if(!block.toUpperCase().startsWith("U")){
								block = "U" + block;
							}
							pers.lot = lot;
							pers.block = block;
							
							String building = global.getSa().getAtribute(SearchAttributes.BUILDING);
							if(!StringUtils.isEmpty(building)){
								pers.lot = block;
								pers.subLot = lot;
								pers.block = building;
							}
						}
						
						List<PersonalDataStruct> tempList = new ArrayList<PersonalDataStruct>(legalStructList);
						legalStructList.clear();
						
						for(PersonalDataStruct legalStructItem:tempList){
							if( !testIfExist(legalStructList,legalStructItem,searchId) ){
								legalStructList.add(legalStructItem);
							}
						}
						
					}
					
					legalStructList = keepOnlyGoodLegals(legalStructList);
					
					try {
						DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
						long miServerId = dataSite.getServerId();
						
						List<PersonalDataStruct> tempLegalStructListPerDocument = new ArrayList<PersonalDataStruct>();
						
						for (LegalI legal : global.getSa().getForUpdateSearchLegalsNotNull(miServerId)) {
							if(legal.hasSubdividedLegal()){
								
								PersonalDataStruct legalStructItem = new PersonalDataStruct("subdivided");
								SubdivisionI subdiv = legal.getSubdivision();
								
								String block = subdiv.getBlock();
								String lot = subdiv.getLot();
								String sublot = subdiv.getSubLot();
								String lotThrough = subdiv.getLotThrough();
								String platBook = subdiv.getPlatBook();
								String platPage = subdiv.getPlatPage();
								String platInst = subdiv.getPlatInstrument();
								String platInstYear = subdiv.getPlatInstrumentYear();
								String unit = subdiv.getUnit();
								legalStructItem.unit = StringUtils.isEmpty(unit)?"":unit;
								
								legalStructItem.block = StringUtils.isEmpty(block)?"":block;
								legalStructItem.subLot = org.apache.commons.lang.StringUtils.defaultString(sublot);
								legalStructItem.lotThrough = StringUtils.isEmpty(lotThrough)?"":lotThrough;
								legalStructItem.setPlatBook(StringUtils.isEmpty(platBook)?"":platBook);
								legalStructItem.setPlatPage(StringUtils.isEmpty(platPage)?"":platPage);
								legalStructItem.platInst = StringUtils.isEmpty(platInst)?"":platInst;
								legalStructItem.platInstrYear = StringUtils.isEmpty(platInstYear)?"":platInstYear;
								
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
												preparePersonalStructForCounty(legalStructItemA,searchId);
												multipleLegals.add("Saved Search Parameters from Parent Site");
												tempLegalStructListPerDocument.add(legalStructItemA);
											}
										}
									}
								}else{
									legalStructItem.lot = lot;
								}
								
								preparePersonalStructForCounty(legalStructItem,searchId);
								multipleLegals.add("Saved Search Parameters from Parent Site");
								tempLegalStructListPerDocument.add(legalStructItem);
							}
							
							if(legal.hasTownshipLegal()){
								PersonalDataStruct legalStructItem = new PersonalDataStruct("sectional");
								TownShipI township = legal.getTownShip();
								
								String arb = township.getArb();
								String sec = township.getSection();
								String tw = township.getTownship();
								String rg = township.getRange();
								int qo = township.getQuarterOrder();
								String qv = township.getQuarterValue();
								String arbLot = township.getArbLot();
								String arbBlock = township.getArbBlock();
								String arbBook = township.getArbBook();
								String arbPage = township.getArbPage();
								
								legalStructItem.setArb(StringUtils.isEmpty(arb)?"":arb);
								legalStructItem.setSection(StringUtils.isEmpty(sec)?"":sec);
								legalStructItem.setTownship(StringUtils.isEmpty(tw)?"":tw);
								legalStructItem.setRange(StringUtils.isEmpty(rg)?"":rg);
								legalStructItem.quarterValue = StringUtils.isEmpty(qv)?"":qv;
								legalStructItem.quarterOrder = String.valueOf(qo<=0?"":qo);
								
								legalStructItem.arbLot = StringUtils.isEmpty(arbLot)?"":arbLot;
								legalStructItem.arbBlock = StringUtils.isEmpty(arbBlock)?"":arbBlock;
								legalStructItem.arbBook = StringUtils.isEmpty(arbBook)?"":arbBook;
								legalStructItem.arbPage = StringUtils.isEmpty(arbPage)?"":arbPage;
								
								multipleLegals.add("Saved Search Parameters from Parent Site");
								tempLegalStructListPerDocument.add(legalStructItem);
							}
						}
						for (PersonalDataStruct item : tempLegalStructListPerDocument) {
							if(!testIfExist(legalStructList, item, searchId)){
								legalStructList.add(item);
							}
						}
						
						
					} catch (Exception e) {
						logger.error("Error loading names for Update saved from Parent Site", e);
					}
					
					legalStructList = keepOnlyGoodLegals(legalStructList);
					
					
					ArrayList<PersonalDataStruct> tempPersonal = new ArrayList<PersonalDataStruct>();
					
					for(PersonalDataStruct pers:legalStructList){
						String lotsAll =  pers.lot;
						if(StringUtils.isNotEmpty(lotsAll)){
							String[] lots = lotsAll.split("[ ,\t\n\r]");
							
							for(String s:lots){
								try{
									PersonalDataStruct legalStructItem = (PersonalDataStruct)pers.clone();
									legalStructItem.lot = s;
									if( !testIfExist(tempPersonal, legalStructItem, searchId) ){
										tempPersonal.add(legalStructItem);
									}
								}
								catch(CloneNotSupportedException e){
									e.printStackTrace();
								}
							}
						}else{
							if( !testIfExist(tempPersonal, pers, searchId) ){
								tempPersonal.add(pers);
							}
						}
					}
					
					if(StringUtils.isNotEmpty(platBookAO)&&StringUtils.isNotEmpty(platPageAO)){
						
						if (isDTG) {
							StringBuilder sb = new StringBuilder();
							HashSet<String> newAllAoAndTrlots = new HashSet<String>();  
							for (String lot:allAoAndTrlots) {
								if (lot.matches("\\d+")) {
									sb.append(lot).append(" ");
								} else {
									newAllAoAndTrlots.add(lot);
								}
								
							}
							allAoAndTrlots = new HashSet<String>();
							for (LotInterval interval: LotMatchAlgorithm.prepareLotInterval(sb.toString())) {
								int lot = interval.getLow();
								int lotThrough = interval.getHigh();
								if(lot == 0) {
									continue;
								}
								PersonalDataStruct struct = new PersonalDataStruct("subdivided");
								struct.lot = Integer.toString(lot);
								if (lot!=lotThrough) {
									struct.lotThrough = Integer.toString(lotThrough);
								}
								struct.subLot = subLotNo;
								struct.block = aoAndTrBloks;
								struct.setPlatBook(platBookAO);
								struct.setPlatPage(platPageAO);
								if( !testIfExist(tempPersonal, struct, searchId) ){
									tempPersonal.add(struct);
									multipleLegals.add("Search Page");
								}
							}
							allAoAndTrlots.clear();
							allAoAndTrlots.addAll(newAllAoAndTrlots);
						} 
						for(String lot:allAoAndTrlots){
							PersonalDataStruct struct = new PersonalDataStruct("subdivided");
							struct.lot = lot;
							struct.subLot = subLotNo;
							struct.block = aoAndTrBloks;
							struct.setPlatBook(platBookAO);
							struct.setPlatPage(platPageAO);
							if( !testIfExist(tempPersonal, struct, searchId) && !testIfExistsForCounty(tempPersonal, struct, searchId, county) ){
								tempPersonal.add(struct);
								multipleLegals.add("Search Page");
							}
						}
					}
					
					legalStructList = tempPersonal;
					
					legalStructList = keepOnlyGoodLegals(legalStructList);
					
					
					if(!atLeastOneFromDtDg) {
						List<PersonalDataStruct> newIterations = new ArrayList<FLSubdividedBasedDASLDT.PersonalDataStruct>();
						for(PersonalDataStruct pers:legalStructList){
							if(pers.isPlated() && !pers.getPlatBook().startsWith("P")) {
								try {
									PersonalDataStruct persClone = (PersonalDataStruct)pers.clone();
									persClone.setPlatBook("P" + persClone.getPlatBook());
									newIterations.add(persClone);
								} catch (CloneNotSupportedException e) {
									e.printStackTrace();
								}
							}
						}
						
						if(!newIterations.isEmpty()) {
							for (PersonalDataStruct legalStructItem : newIterations) {
								boolean atLeastOneEqual = false;
								
								for(PersonalDataStruct p:legalStructList){
									
									if(p.isPlated()){
										preparePersonalStructForCounty(p,searchId);
										if(legalStructItem.equalsStrictSubdivided(p)){
											atLeastOneEqual = true;
											break;
										}
									}
								}
								
								if( !atLeastOneEqual ){
									legalStructList.add(legalStructItem);
								}
							}
						}
						
					}
					
					
					global.setAdditionalInfo("FL_DT_LOOK_UP_DATA",legalStructList);
					
					if(legalStructList.size()>0){
						if( isPlatedMultyLot(legalStructList) ){
							boostrapSubdividedData(legalStructList, global, true);
						}else{
							boostrapSubdividedData(legalStructList, global, false);
						}
						if( isSectionalMultyQv(legalStructList)||isArbExtended(legalStructList) || legalStructList.size()==1 ){
							boostrapSectionalData(legalStructList, global);
						}
					}
					if(isTimeShare && legalStructList.size()>0){
						List<DocumentI> docList = m.getDocumentsWithDataSource(false, "DT");
						List<String> docIds = new ArrayList<String>();
						for(DocumentI doc:docList){
							docIds.add(doc.getId());
						}
						m.remove(docIds);
					}
				}
				finally{
					m.releaseAccess();
				}
			}
			
			
			if( legalStructList.size()>1 && 
					!(isPlatedMultyLot(legalStructList)||isSectionalMultyQv(legalStructList)||isPlatedMultyLotAndIsSectionalMultyQV(legalStructList))){
				global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
				global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegals.toString());
				global.setAdditionalInfo("FL_MULTIPLE_LEGAL_INSTR",multipleLegals);
				if(first){
					SearchLogger.info("<br/><b>Questionable multiple legals found in "+multipleLegals.toString()+"</b>", searchId);
				}
			}
			
			if( isPlatedMultyLot(legalStructList)&& allAoAndTrlots.size() > 0 &&!isTimeShare ){
				addLegalStructItemsUsingAoAndTrLots(legalStructList,allAoAndTrlots.toArray(new String[allAoAndTrlots.size()]), searchId, isDTG);
			}
			
			//expand legals by Sublot
			legalStructList = expandLegalStructItemsSublot(legalStructList, searchId);
			
			return legalStructList;
		}
		
		private static List<PersonalDataStruct> expandLegalStructItemsSublot(List<PersonalDataStruct> legalStructList, long searchId) {
			List<PersonalDataStruct> res = new ArrayList<FLSubdividedBasedDASLDT.PersonalDataStruct>();
			for(PersonalDataStruct pds : legalStructList){
				if(StringUtils.isNotEmpty(pds.subLot)){
					PersonalDataStruct p;
					try {
						p = (PersonalDataStruct) pds.clone();
						p.subLot = "";
						res.add(p);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				res.add(pds);
			}
			
			res = keepOnlyGoodLegals(res);
			
			return res;
		}

		protected List<PersonalDataStruct> createDerrivations(){
			return createDerivationInternal(searchId);
		}
		
		protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
			
			boolean isNevadaSearch = false;
			try {
				isNevadaSearch = module.getTSInterface().toString().startsWith("NV");
			} catch (Exception e) {	}
			
			
			switch(module.getModuleIdx()){
				case TSServerInfo.SUBDIVISION_MODULE_IDX:
					module.setData(2, str.lot);
					if (isDTG() && StringUtils.isNotEmpty(str.lotThrough)) {
						module.setData(20, str.lotThrough);
					}
					module.setData(3, str.block);
					module.setData(4, str.getPlatBook());
					module.setData(5, str.getPlatPage());
					module.setData(6, str.platInst);
					module.forceValue(10, str.subLot);
					if(StringUtils.isNotEmpty(str.platInstrYear)){
						module.forceValue(11, str.platInstrYear);
					}
				break;
				case TSServerInfo.SECTION_LAND_MODULE_IDX:
					module.setData(0, str.getSection());
					module.setData(1, str.getTownship());
					module.setData(2, str.getRange());
					module.setData(3, str.quarterOrder);
					module.setData(4, str.quarterValue);
				break;
				case TSServerInfo.ARB_MODULE_IDX:{
					if (isNevadaSearch){
						module.setData(5, str.arbBook + "-" + str.arbPage + "-" + str.arbLot);
					} else{
						if(org.apache.commons.lang.StringUtils.isNotBlank(str.arbBook)){
							module.setData(0, str.arbBook);
							module.setData(1, str.arbPage);
							module.setData(2, str.arbBlock);
							module.setData(3, str.arbLot);
						}else{
							module.setData(0, str.getSection());
							module.setData(1, str.getTownship());
							module.setData(2, str.getRange());
							module.setData(3, str.quarterOrder);
							module.setData(4, str.quarterValue);
							module.setData(5, str.getArb());
						}
					}
				}
				break;
			}
		}
		
	}
	
	protected static String[] getSubdivisionVector(DocumentsManagerI m){
		final String[] ret = new String[5];
		
		RegisterDocumentI tr = m.getLastRealTransfer();
		if(tr!=null){
			for(PropertyI prop:tr.getProperties()){
				if(prop.hasSubdividedLegal()){
					SubdivisionI sub = prop.getLegal().getSubdivision();
					
					ret[0] = sub.getPlatBook();
					ret[1] = sub.getPlatPage();
				}
				if (prop.hasTownshipLegal()){
				TownShipI town = prop.getLegal().getTownShip();
					ret[2] = town.getTownship();
					ret[3] = town.getRange();
					ret[4] = town.getSection();
				}
			}
		}else{
			tr = m.getLastMortgageForOwner();
			if(tr!=null){
				for(PropertyI prop:tr.getProperties()){
					if(prop.hasSubdividedLegal()){
						SubdivisionI sub = prop.getLegal().getSubdivision();
						ret[0] = sub.getPlatBook();
						ret[1] = sub.getPlatPage();
					}
					if (prop.hasTownshipLegal()){
						TownShipI town = prop.getLegal().getTownShip();
						ret[2] = town.getTownship();
						ret[3] = town.getRange();
						ret[4] = town.getSection();
					}
				}
			}
		}
		
		return ret;
	}

	
	protected void addAssesorMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addAssesorMapSearch(TSServerInfoModule module, PersonalDataStruct str, String additionalForSection){
		addAssesorMapSearch( module, str );
	}
	
	public void addAssesorMapSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules, boolean isUpdate){
		//assesor map  fake search
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
		module.setSaObjKey(SearchAttributes.LD_BOOKNO_1);
		module.clearSaKeys();
		PlatIterator it = new PlatIterator(searchId, this);
		module.addIterator(it);
		if (!isUpdate) {
			modules.add(module);
		}
	}
	
	public void addSubdivisionMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	public void addParcelMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	public void addCondoMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	protected void uncheckPlats(DocumentsManagerI m){
		try{
			m.getAccess();
			List<DocumentI> list = m.getDocuments("PLAT");
			if(list!=null){
				for(DocumentI doc:list){
					if(!doc.isFake()){
						String serverDocType = doc.getServerDocType();
						boolean noNeedToDeleteIt = false;
						for (String plat : FLGenericDTG.PLAT_CONDO) {
							if (plat.equals(serverDocType)){
								noNeedToDeleteIt = true;
								break;
							}
						}
						if (noNeedToDeleteIt){
							continue;
						}
						doc.setIncludeImage(false);
						doc.setImage(null);
					}
				}
			}
		}
		finally{
			m.releaseAccess();
		}
	}
	
	protected boolean deletePlat(DocumentsManagerI m,String book, String page){
		boolean test = false;
		if(!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page)){
			try{
				m.getAccess();
				List<DocumentI> list = m.getDocuments("PLAT");
				if(list!=null){
					for(DocumentI doc:list){
						if(doc.hasBookPage()&&!doc.isFake()){
							String serverDocType = doc.getServerDocType();
							boolean noNeedToDeleteIt = false;
							for (String plat : FLGenericDTG.PLAT_CONDO) {
								if (plat.equals(serverDocType)){
									noNeedToDeleteIt = true;
									break;
								}
							}
							if (noNeedToDeleteIt){
								continue;
							}
							String book1 = doc.getBook();
							if((book1.equals(book)||book1.equals(book.replaceFirst("[P]", ""))||book1.equals(book.replaceFirst("[U]", "")))&&doc.getPage().equals(page)){
								test = (m.remove(doc.getId())!=null);
							}
						}
					}
				}
			}
			finally{
				m.releaseAccess();
			}
		}
		return test;
	}
	
	protected void addDefaultPlatMapSearch(TSServerInfoModule module,PersonalDataStruct str, String type) {
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
	
		String book  = str.getPlatBook();
		String page  = str.getPlatPage();
		
		DocumentsManagerI m = search.getDocManager();
		
		uncheckPlats(m);
		
		if( StringUtils.isEmpty(book)||StringUtils.isEmpty(page) ){
			try{
				m.getAccess();
				String[] subdivVector = getSubdivisionVector(m);
				if(StringUtils.isEmpty(book)){
					book = subdivVector[0];
				}
				if(StringUtils.isEmpty(page)){
					page = subdivVector[1];
				}
			}finally{
				m.releaseAccess();
			}
		}
		
		if(StringUtils.isEmpty(book)||StringUtils.isEmpty(page)){
			try{
				m.getAccess();
				List<DocumentI> list = m.getDocumentsWithType(true, DType.ASSESOR, DType.TAX, DType.CITYTAX);
				boolean found = false;
				for(DocumentI doc:list){
					for(PropertyI prop:doc.getProperties()){
						if(prop.hasSubdividedLegal()){
							String book1 = prop.getLegal().getSubdivision().getPlatBook();
							String page1 =  prop.getLegal().getSubdivision().getPlatPage();
							if(!StringUtils.isEmpty(book1)&&!StringUtils.isEmpty(page1)){
								book  = book1;
								page = page1;
								found = true;
								break;
							}
						}
					}
					if(found){
						break;
					}
				}
			}finally{
				m.releaseAccess();
			}
		}
		
		if(deletePlat(m, book, page)){
			InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().clearValidatedLinks();		
			InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().clearVisitedLinks();
			InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().removeAllVisitedDocs();
		}
		
		if(!StringUtils.isEmpty(book) && book.length()>=2){
			book = book.replaceFirst("[a-zA-Z]", "");
		}
		
		if(!StringUtils.isEmpty(page) && page.length()>=2){
			page = page.replaceAll("[a-zA-Z]", "");
		}
		if (StringUtils.isEmpty(page)){
			page ="";
		}
		module.forceValue(0,book);
		module.forceValue(1,page.split("[ -]")[0]);
		module.forceValue(4,"PLAT");
		module.forceValue(5,type);
		return;
	}
	
	public void addPlatMapSearch(TSServerInfoModule module,PersonalDataStruct str) {
		addDefaultPlatMapSearch(module, str, "PLAT_MAP");
	}
	
	public static boolean dontMakeTheSearch(TSServerInfoModule module,	long searchId, boolean ignoreYear) {
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
		
		boolean isFloridaDTGSearch = false, isCO = false;
		try {
			isFloridaDTGSearch = (module.getTSInterface().toString().startsWith("FL") && module.getTSInterface().toString().endsWith("DG"));
			isCO = (module.getTSInterface().toString().startsWith("CO") && module.getTSInterface().toString().endsWith("DG"));
		} catch (Exception e) {	}
		
		switch(module.getModuleIdx()){
			
			case TSServerInfo.SUBDIVISION_MODULE_IDX:					
					boolean test = doNotMakeSearch(module,"subdivided",false);
					if(!test){
						search.setAdditionalInfo("BASE_SEARCH_DONE","true");
						search.setAdditionalInfo("BASE_SUBDIVIDED_SEARCH_DONE","true");
					}
					return test;
				case TSServerInfo.SECTION_LAND_MODULE_IDX:
					if (isFloridaDTGSearch || isCO){
						test = doNotMakeSearch(module, "sectional", true);
					} else{
						test = doNotMakeSearch(module,"sectional",false);
					}
					if(!test){
						if(search.getAdditionalInfo("BASE_ARB_SEARCH_DONE")!=null || (search.getAdditionalInfo("BASE_SUBDIVIDED_SEARCH_DONE")!=null && aoOrTrIsPlated(search))){
							test = true;
						}else{
							search.setAdditionalInfo("BASE_SEARCH_DONE","true");
						}
					}
					return test;
				case TSServerInfo.ARB_MODULE_IDX:
					test =  doNotMakeSearch(module,"arb",false);
					if(!test){
						if(search.getAdditionalInfo("BASE_SUBDIVIDED_SEARCH_DONE")!=null && aoOrTrIsPlated(search)){
							test = true;
						}else{
							search.setAdditionalInfo("BASE_ARB_SEARCH_DONE","true");
							search.setAdditionalInfo("BASE_SEARCH_DONE","true");
						}
					}
					return test;
				case TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX:
					return StringUtils.isEmpty(module.getParamValue(0))||StringUtils.isEmpty(module.getParamValue(1));
				case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
					return StringUtils.isEmpty(module.getParamValue(0))||StringUtils.isEmpty(module.getParamValue(1));
				case TSServerInfo.PARCEL_ID_MODULE_IDX:					
					test =  StringUtils.isEmpty(module.getParamValue(0)) || search.getAdditionalInfo("BASE_SEARCH_DONE")!=null ;
					if(!test){
						search.setAdditionalInfo("BASE_SEARCH_DONE","true");
					}
					return test;
				case TSServerInfo.INSTR_NO_MODULE_IDX:
					return StringUtils.isEmpty(module.getParamValue(0)) 
						|| (!ignoreYear && (StringUtils.isEmpty(module.getParamValue(1)) || "-1".equalsIgnoreCase(module.getParamValue(2)))); 
		}		
		return false;
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

	protected static void boostrapSectionalData(List<PersonalDataStruct> legalStruct1, Search search) {
		SearchAttributes sa = search.getSa();
		PersonalDataStruct legalStruct = legalStruct1.get(0);
		if(!StringUtils.isEmpty(legalStruct.getSection())){
			sa.setAtribute( SearchAttributes.LD_SUBDIV_SEC, legalStruct.getSection());
		}
		
		if(!StringUtils.isEmpty(legalStruct.getTownship())){
			sa.setAtribute( SearchAttributes.LD_SUBDIV_TWN, legalStruct.getTownship());
		}
		
		if(!StringUtils.isEmpty(legalStruct.getRange())){
			sa.setAtribute( SearchAttributes.LD_SUBDIV_RNG, legalStruct.getRange());
		}
		if(!StringUtils.isEmpty(legalStruct.quarterOrder)){
			sa.setAtribute( SearchAttributes.QUARTER_ORDER, legalStruct.quarterOrder);
		}
		
		if(!StringUtils.isEmpty(legalStruct.quarterValue)){
			sa.setAtribute( SearchAttributes.QUARTER_VALUE, legalStruct.quarterValue);
		}
		
		if(!StringUtils.isEmpty(legalStruct.getArb())){
			sa.setAtribute( SearchAttributes.ARB, legalStruct.getArb());
		}
		
		if(!StringUtils.isEmpty(legalStruct.arbLot)){
			sa.setAtribute( SearchAttributes.ARB_LOT, legalStruct.arbLot);
		}
		
		if(!StringUtils.isEmpty(legalStruct.arbBlock)){
			sa.setAtribute( SearchAttributes.ARB_BLOCK, legalStruct.arbBlock);
		}
		
		if(!StringUtils.isEmpty(legalStruct.arbBook)){
			sa.setAtribute( SearchAttributes.ARB_BOOK, legalStruct.arbBook);
		}
		
		if(!StringUtils.isEmpty(legalStruct.arbPage)){
			sa.setAtribute( SearchAttributes.ARB_PAGE, legalStruct.arbPage);
		}
	}

	protected static void boostrapSubdividedData(List<PersonalDataStruct> legalStruct1, Search search, boolean boostrapPlatsAndBlock) {
		
		String aoAndTrLots = search.getSa().getAtribute(SearchAttributes.LD_LOTNO);
		String []allAoAndTrlots = new String[0];
		
		if(!StringUtils.isEmpty(aoAndTrLots)){
			LotInterval li = new LotInterval(aoAndTrLots);
			aoAndTrLots = li.getLotList().toString();
			aoAndTrLots = aoAndTrLots.replaceAll("(?is)[\\[\\]]+", "");
			allAoAndTrlots = aoAndTrLots.split("[ /t/r/n,-]+");
		}
		
		Set<String> allLots = new TreeSet<String>();
		allLots.addAll(Arrays.asList(allAoAndTrlots));
		
		
		SearchAttributes sa = search.getSa();
		if(boostrapPlatsAndBlock){
			for(PersonalDataStruct legalStruct:legalStruct1){
				if(!StringUtils.isEmpty(legalStruct.getPlatBook())){
					sa.setAtribute( SearchAttributes.LD_BOOKNO, legalStruct.getPlatBook());
				}
				if(!StringUtils.isEmpty(legalStruct.getPlatPage())){
					sa.setAtribute( SearchAttributes.LD_PAGENO, legalStruct.getPlatPage());
				}
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

	private static boolean doNotMakeSearch(TSServerInfoModule module, String what,boolean ignoreQOandQV) {
		
		boolean isFloridaDTGSearch = false;
		boolean isCADTGSearch = false;
		boolean isCADTGWithoutPlatPage = false;
		try {
			isFloridaDTGSearch = (module.getTSInterface().toString().startsWith("FL") && module.getTSInterface().toString().endsWith("DG"));
		} catch (Exception e) {	}
		try {
			isCADTGSearch = (module.getTSInterface().toString().startsWith("CA") && module.getTSInterface().toString().endsWith("DG"));
		} catch (Exception e) {	}
		try {
			isCADTGWithoutPlatPage = (isCADTGSearch && module.getTSInterface().toString().contains("San Diego"));
		} catch (Exception e) {	}
		
		String lot = "";
		String block = "";
		String platBook = "";
		String platPage = "";
		String platInst = "";
		
		String ncbNumber = "";
		String subdivision = "";
		String tract = "";
		
		String sec = "";
		String tw = "";
		String rg = "";
		String qo = "";
		String qv = "";
		String arb = "";
		
		if("subdivided".equals(what)){
			lot  = module.getParamValue(2);
			block  = module.getParamValue(3);
			platBook  = module.getParamValue(4);
			platPage = module.getParamValue(5);
			platInst = module.getParamValue(6);
			
			if (module.getFunctionCount() > 21){
				ncbNumber = module.getParamValue(21);
			}
			
			//for CA
			if (isCADTGSearch && module.getFunctionCount() > 21){
				tract = module.getParamValue(21);
			}
			if (module.getFunctionCount() > 12){
				subdivision = module.getParamValue(12);
			}
		}
		else if("sectional".equals(what)){
			sec  = module.getParamValue(0);
			tw  = module.getParamValue(1);
			rg = module.getParamValue(2);
			qo = module.getParamValue(3);
			qv = module.getParamValue(4);
		}
		else if("arb".equals(what)){
			sec  = module.getParamValue(0);
			tw  = module.getParamValue(1);
			rg = module.getParamValue(2);
			qo = module.getParamValue(3);
			qv = module.getParamValue(4);
			arb = module.getParamValue(5);
		}
		
		boolean emptyLot = StringUtils.isEmpty(lot);
		boolean emptyBlock = StringUtils.isEmpty(block);
		boolean emptyPlatBook = StringUtils.isEmpty(platBook);
		boolean emptyPlatPage = StringUtils.isEmpty(platPage);
		boolean emptyPlatInst = StringUtils.isEmpty(platInst);
		
		boolean emptySection = StringUtils.isEmpty(sec);
		boolean emptyRange = StringUtils.isEmpty(rg);
		boolean emptyTownship = StringUtils.isEmpty(tw);
		boolean emptyQO = StringUtils.isEmpty(qo);
		boolean emptyQV = StringUtils.isEmpty(qv);
		
		boolean emptyArb = StringUtils.isEmpty(arb);
		
		boolean emptyNcb = StringUtils.isEmpty(ncbNumber);
		boolean emptySubdivision = StringUtils.isEmpty(subdivision);
		boolean emptyTract = StringUtils.isEmpty(tract);
		
		boolean isPlated = !( (emptyPlatBook || emptyPlatPage) && emptyPlatInst) && !(emptyLot && emptyBlock) ;
		
		if (isCADTGWithoutPlatPage){
			isPlated = !(emptyPlatBook && emptyPlatInst) && !(emptyLot && emptyBlock);
		}
		
		boolean subdivided = !isPlated;
		
		if (!emptyNcb && !emptySubdivision){
			subdivided = (emptyNcb && emptySubdivision);
		}
		
		if (isCADTGSearch && !emptyTract){
			subdivided = emptyTract;
		}
		boolean sectional = (emptySection|| emptyRange|| emptyTownship || emptyQO || emptyQV) || !emptyArb /*|| isPlated*/;
		if(ignoreQOandQV){
			sectional = (emptySection|| emptyRange|| emptyTownship) || !emptyArb /*|| isPlated*/;
		}
		boolean arb1 = emptyArb /*|| isPlated*/;
		
		if("subdivided".equals(what)){
			return subdivided;
		}
		else if("sectional".equals(what)){
			return sectional;
		}
		else if("arb".equals(what)){
			if (isFloridaDTGSearch){
				if (emptyArb){
					return emptyArb;
				} else{
					return arb1 && emptySection;
				}
			} else{
				return arb1&&emptySection;
			}
		}
		
		return subdivided && sectional && arb1;
	}
	
	@Override
	public  String prepareApnPerCounty( long searchId ){
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
		String county = ci.getCurrentCounty().getName();
		
		String apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNONDB ).replaceAll("[.-]", "");
		if(StringUtils.isEmpty(apn)){
			apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNO ).replaceAll("[.-]", "");
		}
		
		if("broward".equalsIgnoreCase(county.toLowerCase())){ //5-2-4  - from TR
			apn =  search.getSa().getAtribute( SearchAttributes.LD_PARCELNO ).replaceAll("[.-]", "");
			if(apn.length()>=12){
				apn = apn.substring(0,5)+"-"+apn.substring(5,7)+"-"+apn.substring(7);
			}
		}else if("brevard".equalsIgnoreCase(county.toLowerCase())){//2-2-2-2-5-1-4-2 or 3-2-2-2-5-1-4-2
			
			DocumentsManagerI doc = search.getDocManager();
			try{
				doc.getAccess();
				ArrayList<DocumentI> docs= new ArrayList<DocumentI>(doc.getDocumentsWithType(true,DType.TAX));
				if(docs.size()>0){
					DocumentI taxDoc = docs.get(0);
					for(PropertyI prop:taxDoc.getProperties()){
						String geo = prop.getPin(PinType.GEO_NUMBER);
						if(!StringUtils.isEmpty(geo) && geo.length() >=8 ){
							if(org.apache.commons.lang.StringUtils.isAlpha(geo.substring(2,3))){
								geo = geo.substring(0,9);
								geo = geo.substring(0,3)+"-"+geo.substring(3,5)+"-"+geo.substring(5,7)+"-"+geo.substring(7,9);
							}
							else{
								geo = geo.substring(0,8);
								geo = geo.substring(0,2)+"-"+geo.substring(2,4)+"-"+geo.substring(4,6)+"-"+geo.substring(6,8);
							}
							if(apn.length()>=12){
								apn = apn.substring(apn.length() - 12);  
								apn = apn.substring(0,5)+"-"+apn.substring(5,6)+"-"+apn.substring(6,10)+"-"+apn.substring(10,12);
								apn = geo+"-"+apn;
							}
							break;
						}
					}
					if(apn.length()==20 && !apn.contains("-")){
						apn = apn.substring(0,2)+"-"+apn.substring(2,4)+"-"+apn.substring(4,6)+"-"+apn.substring(6,8)
							+"-"+apn.substring(8,13)+"-"+apn.substring(13,14)+"-"+apn.substring(14,18)+"-"+apn.substring(18);
					}
				}
			}
			finally{
				doc.releaseAccess();
			}
		}else if("hillsborough".equalsIgnoreCase(county.toLowerCase())){
			// P1 : search by PID2 search
			apn = search.getSa().getAtribute(SearchAttributes.LD_PARCELNO3);
			apn = apn.replaceAll("[.-]", "");
		}else if("columbia".equalsIgnoreCase(county.toLowerCase())){
			// P1 : search by PID2 search
			apn = apn.replaceAll("^0+", "");
		}else if ("polk".equalsIgnoreCase(county.toLowerCase())){
			DocumentsManagerI doc = search.getDocManager();
			try{
				doc.getAccess();
				ArrayList<DocumentI> docs= new ArrayList<DocumentI>(doc.getDocumentsWithType(true,DType.TAX));
				if(docs.size()>0){
					DocumentI taxDoc = docs.get(0);
					for(PropertyI prop:taxDoc.getProperties()){
						String pid = prop.getPin(PinType.PID);
						if(!StringUtils.isEmpty(pid)){
							apn = pid.replaceAll("[ -]", "");
							break;
						}
					}
				}
			}
			finally{
				doc.releaseAccess();
			}
		}else if("manatee".equalsIgnoreCase(county.toLowerCase())){
			apn = apn.replaceAll("^0+", "");
		}
		
		search.getSa().setAtribute(SearchAttributes.LD_PARCELNO3,apn);
		return apn;
	}
	
	public String getPrettyFollowedLink (String initialFollowedLnk){
    	if (initialFollowedLnk.matches("(?i).*DL[_]{2,}([0-9]+)[_]{1,}([0-9]+)[^a-z]*([a-z]+).*")){
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*DL[_]{2,}([0-9]+)[_]{1,}([0-9]+)[^a-z]*([a-z]*)[^a-z]*.*", 
    				"Book " + "$1" + " Page " + "$2" + " " + "$3" + " has already been processed from a previous search in the log file. ");
    		retStr =  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    		return retStr;
    	}
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
	
	
	private static class PinFilter extends PinFilterResponse {
		private static final long serialVersionUID = -90373723192342L;
		
		private boolean actOnlyOnIncompleteLegal = false;
		
		public PinFilter(String sakey, long searchId, boolean actOnlyOnIncompleteLegal){
			super(sakey, searchId);
			setStartWith(true);
			setIgNoreZeroes(true);
			this.actOnlyOnIncompleteLegal = actOnlyOnIncompleteLegal;
		}
		
		public BigDecimal getScoreOneRow(ParsedResponse row) {	
			BigDecimal  ret = super.getScoreOneRow(row);
			if( actOnlyOnIncompleteLegal ){
				if(ret.compareTo(threshold)>0){
					return ret;
				}
				DocumentI doc = row.getDocument();
				if(doc instanceof RegisterDocumentI){
					RegisterDocumentI regDoc = (RegisterDocumentI)doc;
					for(PropertyI prop:regDoc.getProperties()){
						if(prop.hasSubdividedLegal()){
							SubdivisionI sub = prop.getLegal().getSubdivision();
							if(!StringUtils.isEmpty(sub.getBlock())||!StringUtils.isEmpty(sub.getLot())){
								return BigDecimal.ONE;
							}
						}
						if(prop.hasTownshipLegal()){
							TownShipI tw = prop.getLegal().getTownShip();
							if(tw.getQuarterOrder()>=0||!StringUtils.isEmpty(tw.getQuarterValue())){
								return BigDecimal.ONE;
							}
						}
					}
				}
			}
			return ret;
		}
		
		@Override
		public String getFilterCriteria(){
			return "PIN=" + getRefPin()+" ONLY_ON_INCOMPLETE_LEGAL=" + actOnlyOnIncompleteLegal;
		}
	}
	
	
	protected static class IncompleteLegalFilter extends FilterResponse {
		private static final long serialVersionUID = -90373723192342L;
		
	
		public IncompleteLegalFilter(String sakey, long searchId){
			super(sakey, searchId);
			threshold = new BigDecimal("0.90");
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void computeScores(Vector rows) {
			for (int i = 0; i < rows.size(); i++){
	            IndividualLogger.info("Processing result " + i + " of total " + rows.size(),searchId);
				ParsedResponse row = (ParsedResponse)rows.get(i);
				BigDecimal score = null;
				if(rows.size() == 1 && isSkipUnique()){
					score = ATSDecimalNumberFormat.ONE;
				} else if(rows.size() > getMinRowsToActivate()){
					score = getScoreOneRow(row);
				} else {
					score = ATSDecimalNumberFormat.ONE;
				}
				scores.put(row.getResponse(), score);
				if (score.compareTo(bestScore) > 0)
				{
					bestScore = score;
				}
				IndividualLogger.info("ROW SCORE:" + score,searchId);
				logger.debug("\n\n ROW SCORE : [" + score + "]\nROW HTML: [" + row.getResponse() + "]\n");
			}
			if(BigDecimal.ZERO.doubleValue()==bestScore.doubleValue()){
				for (int i = 0; i < rows.size(); i++){
					ParsedResponse row = (ParsedResponse)rows.get(i);
					scores.put(row.getResponse(), BigDecimal.ONE);
				}
			}
		}
		
		public BigDecimal getScoreOneRow(ParsedResponse row) {	
			DocumentI doc = row.getDocument();
			if(doc instanceof RegisterDocumentI){
				RegisterDocumentI regDoc = (RegisterDocumentI)doc;
				for(PropertyI prop:regDoc.getProperties()){
					if(prop.hasSubdividedLegal()){
						SubdivisionI sub = prop.getLegal().getSubdivision();
						if(!StringUtils.isEmpty(sub.getBlock())||!StringUtils.isEmpty(sub.getLot())){
							return BigDecimal.ONE;
						}
					}
				}
			}
			return BigDecimal.ZERO;
		}
		
		@Override
		public String getFilterCriteria(){
			return "INCOMPLETE LEGAL filter but PASS ALL when no complete legal found ";
		}
	}
	
public static FilterResponse getLegalOrDocTypeFilter(long searchId,boolean forceLegalWhenPossible){
		LegalOrDocTypeFilter fr = new LegalOrDocTypeFilter( searchId, forceLegalWhenPossible);
		fr.setEnableSectionJustForUnplated(false);
		fr.setUseLegalFromSearchPage(true);
		fr.setThreshold(new BigDecimal("0.7"));
		return fr;
}
	
	private static class LegalOrDocTypeFilter extends GenericMultipleLegalFilter {
	
		protected String []docTypes = { "MISCELLANEOUS", "LIEN", "COURT"};
	
		private static final long serialVersionUID = -9046337112L;

		private List<RegisterDocumentI> allDocs = new ArrayList<RegisterDocumentI>();
		
		private final DocumentsManagerI m;
		
		private final Search search;
		
		@SuppressWarnings("unused")
		private boolean forceLegalWhenPossible;
		
		public LegalOrDocTypeFilter(long searchId, boolean forceLegalWhenPossible){
			super(searchId);
			this.forceLegalWhenPossible = forceLegalWhenPossible;
			m = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
			setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
			setThreshold(new BigDecimal("0.8"));
			search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		}
		
		private BigDecimal getDoctypeScoreInternal(ParsedResponse row){
			try{
				String docType = row.getSaleDataSet(0).getAtribute("DocumentType").trim();
		        if( DocumentTypes.isOfDocType(docType, docTypes,searchId) ){
		        	return new BigDecimal(1.0);
		        }
		        return new BigDecimal(0.0);
	        }
	        catch(Exception e){
	        	e.printStackTrace();
	        }	
			return new BigDecimal(0.0);
		}
		
		@SuppressWarnings("rawtypes")
		public void computeScores(Vector rows)
		{
			allDocs.clear();
			for (int i = 0; i < rows.size(); i++){
				DocumentI doc = ((ParsedResponse)rows.get(i)).getDocument();
				if(doc instanceof RegisterDocumentI){
					allDocs.add((RegisterDocumentI)doc);
				}
			}
			super.computeScores(rows);
		}
		
		public BigDecimal getScoreOneRow(ParsedResponse row) {	
			BigDecimal  ret = BigDecimal.ONE;
			if("true".equalsIgnoreCase(search.getSa().getAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND))){
				ret = getDoctypeScoreInternal(row);
				if( ret.compareTo(threshold)<0 ){
					DocumentI doc  = row.getDocument();
					if(doc instanceof RegisterDocumentI){
						RegisterDocumentI regDoc = (RegisterDocumentI)doc;
						if(!regDoc.isOneOf(docTypes)){
							boolean test = false;
							for(PropertyI prop:regDoc.getProperties()){
								if(prop.hasLegal()){
									test = true;
									break;
								}
							}
							if( !test ){
								return BigDecimal.ONE;
							}
							if("RELEASE".equalsIgnoreCase(regDoc.getDocType())){
								Set<InstrumentI> references = regDoc.getParsedReferences();
								
								for(InstrumentI instr:references){
									for(RegisterDocumentI cur:allDocs){
										if(cur.flexibleEquals(instr,true)&& cur.isOneOf(docTypes) ){
											return BigDecimal.ONE;
										}
									}
									try{
										m.getAccess();
										if(m.getRegisterDocuments(instr, true).size()>0){
											return BigDecimal.ONE;
										}
									}finally{
										m.releaseAccess();
									}
								}
							}
						}
					}
				}
			}else{
				return super.getScoreOneRow(row);
			}
			return ret;
		}
		
		 public String getFilterCriteria(){
			 if( "true".equalsIgnoreCase(search.getSa().getAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND)) ){
		    	String ret = "";
		    	for(int i=0;i<docTypes.length;i++ ){
		    		ret+= docTypes[i]+" ; ";
		    	}
		    	return ret +"but keep all documents without legal and good RELEASES";
		    }else{
		    	return super.getFilterCriteria();
		    }
		 }
	}
	
	protected static class PropertyTypeFilter extends FilterResponse {

		private final Search search;
		
		public PropertyTypeFilter(long searchId) {
			super(searchId);
			search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			setThreshold(new BigDecimal(0.8));
		}

		private static final long serialVersionUID = -1877359332961695340L;
	
		@SuppressWarnings("unchecked") // for Vector<PropertyIdentificationSet>
		public BigDecimal getScoreOneRow(ParsedResponse row) {
			
			List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)search.getAdditionalInfo("FL_DT_LOOK_UP_DATA");
			
			if(legalStructList==null){
				return BigDecimal.ONE;
			}
			
			boolean refIsPlatedOnDT = getFirstPlatedStruct(legalStructList)!=null && getFirstSectionalStruct(legalStructList)==null;
			boolean refIsSectionalOnDT = getFirstPlatedStruct(legalStructList)==null && getFirstSectionalStruct(legalStructList)!=null;
			double finalScore = 1.00d;
			
			boolean candIsPlatedOnDT = false;
			boolean candIsSectionalOnDT = false;
			
			// compute the actual score
			for(PropertyIdentificationSet pis : (Vector<PropertyIdentificationSet>) row.getPropertyIdentificationSet()){
				
				// check if empty
				if(pis == null) { 
					continue; 
				}			
				
				String section  = pis.getAtribute("SubdivisionSection");
			    String township = pis.getAtribute("SubdivisionTownship");
			    String range    = pis.getAtribute("SubdivisionRange");
			    String lot      = pis.getAtribute("SubdivisionLotNumber");
			    String block    = pis.getAtribute("SubdivisionBlock");
			    String platBook = pis.getAtribute("PlatBook"); 
	            String platNo 	= pis.getAtribute("PlatNo");
	            String unit 	= pis.getAtribute("SubdivisionUnit");
	            String qo		= pis.getAtribute("QuarterOrder");
	            String qv		= pis.getAtribute("QuarterValue");
	            
	            if(StringUtils.isEmpty(lot)){
	            	lot = unit;
	            }
	            
	            boolean isPropertyPlated =    !(StringUtils.isEmpty(platBook) || StringUtils.isEmpty(platNo)) &&  StringUtils.isEmpty(section) && StringUtils.isEmpty(township) && StringUtils.isEmpty(range); 
	            boolean isPropertySectional = !(StringUtils.isEmpty(section) && StringUtils.isEmpty(township) && StringUtils.isEmpty(range))&& !(StringUtils.isEmpty(qo)&&StringUtils.isEmpty(qv))  && StringUtils.isEmpty(platBook) && StringUtils.isEmpty(platNo) && StringUtils.isEmpty(lot) && StringUtils.isEmpty(block);
	            
	            candIsPlatedOnDT = candIsPlatedOnDT || isPropertyPlated;
	            candIsSectionalOnDT = candIsSectionalOnDT || isPropertySectional;
			}		
			
			if(  (refIsPlatedOnDT && candIsSectionalOnDT && !candIsPlatedOnDT) || (refIsSectionalOnDT && candIsPlatedOnDT && !candIsSectionalOnDT ) ){
				finalScore = 0.00d;
			}
			
			return new BigDecimal(finalScore);
		}	
	
		@Override
	    public String getFilterName(){
	    	return "Filter by Property Type ";
	    }
		
		@Override
		public String getFilterCriteria(){		
	    	return "(PLATED/UNPLATED) but pass ALL when multiple legals or no legal found at Book Page LOOK UP searches";
	    }
	}

	@Override
	public ArrayList<NameI> addNameSearch(List<TSServerInfoModule> modules,
			TSServerInfo serverInfo, String key, ArrayList<NameI> object,
			List<FilterResponse> filtersOList) {
		return super.addNameSearch(modules, serverInfo, key, object, filtersOList);
	}

	@Override
	public void addOCRSearch(List<TSServerInfoModule> modules, TSServerInfo serverInfo, FilterResponse ... filters) {
		 super.addOCRSearch(modules, serverInfo, filters);
	}

	@Override
	public InstrumentGenericIterator getInstrumentIterator(boolean reference) {
		return null;
	}

	@Override
	public void addGrantorGranteeSearch(List<TSServerInfoModule> modules,
			TSServerInfo serverInfo, String ownerObject,
			List<FilterResponse> filtersOList) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addRelatedSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		
		TSServerInfoModule module = null;
		String[] relatedSourceDoctype = new String[]{
				DocumentTypes.MORTGAGE, 
				DocumentTypes.LIEN, 
				DocumentTypes.CCER
				};
		
		
		InstrumentGenericIterator instrumentRelatedBPIterator = getInstrumentIterator();
		instrumentRelatedBPIterator.enableBookPage();
		instrumentRelatedBPIterator.setLoadFromRoLike(true);
		instrumentRelatedBPIterator.setRoDoctypesToLoad(relatedSourceDoctype);
		instrumentRelatedBPIterator.setDoNotCheckIfItExists(true);
		
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.RELATED_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				"Searching (related) with book/page list from all " + Arrays.toString(relatedSourceDoctype));
		module.clearSaKeys();
		module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		module.addIterator(instrumentRelatedBPIterator);
		if (isUpdate()) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}
		module.addFilter(getLegalOrDocTypeFilter(searchId , true));
		module.addFilter(new PropertyTypeFilter(searchId));
		modules.add(module);
		
		
		addRelatedInstrumentNoSearch(serverInfo, modules, isUpdate(), relatedSourceDoctype);
		
	}

	public void addRelatedInstrumentNoSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules,
			boolean isUpdate, String[] relatedSourceDoctype) {
		TSServerInfoModule module;
		InstrumentGenericIterator instrumentRelatedNoIterator = getInstrumentIterator();
		instrumentRelatedNoIterator.enableInstrumentNumber();
		instrumentRelatedNoIterator.setLoadFromRoLike(true);
		instrumentRelatedNoIterator.setRoDoctypesToLoad(relatedSourceDoctype);
		instrumentRelatedNoIterator.setDoNotCheckIfItExists(true);
		instrumentRelatedNoIterator.setInitAgain(true);
		
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.RELATED_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				"Searching (related) with Instrument Number list from all " + Arrays.toString(relatedSourceDoctype));
		module.clearSaKeys();
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
		module.addIterator(instrumentRelatedNoIterator);
		if (isUpdate) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}
		module.addFilter(getLegalOrDocTypeFilter(searchId , true));
		module.addFilter(new PropertyTypeFilter(searchId));
		modules.add(module);
	}

	protected InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator instrumentBPIterator = new InstrumentGenericIterator(searchId, getDataSite());
		return instrumentBPIterator;
	}
	
	@Override
	public void performAdditionalProcessingAfterRunningAutomatic() {
		super.performAdditionalProcessingAfterRunningAutomatic();
		
		Search search = getSearch();
		
		if (isUpdate()){
			SearchAttributes sa = search.getSa();
			
			if (!sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
			
				SearchLogger.info("\n</div><div><BR>Run additional searches to get Certification Date. <BR></div>\n", searchId);
				TSServerInfo serverInfo = getCurrentClassServerInfo();
				Set<InstrumentI> allAoRef = getAllAoAndTaxReferences(search);
				
				for(InstrumentI inst : allAoRef){
					try {
						if (inst.hasBookPage()){
							String book = inst.getBook();
							String page = inst.getPage();
							
							TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
							module.setData(0, book);
							module.setData(1, page);
							module.setData(2, "B_P");
								
							SearchBy(module, null);
							
							if (sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
								SearchLogger.info("\n</div><div><BR>Certification Date found!<BR></div>\n", searchId);
								break;
							}
							
						} else if (inst.hasDocNo()){
							
							
							String docNo = inst.getDocno();
							if(docNo.length() == 10 && !docNo.startsWith(getStartingZerosForCounty(searchId))){ //Book Page
								String book = docNo.substring(0,6);
								String page = docNo.substring(6,10);
										
								TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
								module.setData(0, book);
								module.setData(1, page);
								module.setData(2, "B_P");
								
								SearchBy(module, null);
								
							} else{
								String year = String.valueOf(inst.getYear());

								TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
								module.setData(0, cleanInstrNo(inst, false));
								module.setData(1, "INST");
								module.setData(2, year);
								
								SearchBy(module, null);
							}
							
							if (sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
								SearchLogger.info("\n</div><div><BR>Certification Date found!<BR></div>\n", searchId);
								break;
							}
						} else if (inst.hasInstrNo()){
						
							String  instNo = inst.getInstno();
							
							if(instNo.length() == 10 && !instNo.startsWith(getStartingZerosForCounty(searchId))){ //Book Page
									
								String book = instNo.substring(0,6);
								String page = instNo.substring(6,10);
										
								TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
								module.setData(0, book);
								module.setData(1, page);
								module.setData(2, "B_P");
								
								SearchBy(module, null);
								
							} else{
								String year = String.valueOf(inst.getYear());

								TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
								module.setData(0, cleanInstrNo(inst, false));
								module.setData(1, "INST");
								module.setData(2, year);
								
								SearchBy(module, null);
							}
							
							if (sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
								SearchLogger.info("\n</div><div><BR>Certification Date found!<BR></div>\n", searchId);
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
	
	@Override
	protected Object getRequestCountType(int moduleIDX) {
		switch (moduleIDX) {
		case TSServerInfo.PARCEL_ID_MODULE_IDX:
			return RequestCount.TYPE_PIN_COUNT;
		case TSServerInfo.SUBDIVISION_MODULE_IDX:
		case TSServerInfo.SECTION_LAND_MODULE_IDX:
		case TSServerInfo.ARB_MODULE_IDX:
			return RequestCount.TYPE_LEGAL_COUNT;
		case TSServerInfo.ADDRESS_MODULE_IDX:
			return RequestCount.TYPE_ADDR_COUNT;
		case TSServerInfo.GENERIC_MODULE_IDX:
		case TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX:
		case TSServerInfo.RELATED_MODULE_IDX:
			return RequestCount.TYPE_MISC_COUNT;
		case TSServerInfo.NAME_MODULE_IDX:
			return RequestCount.TYPE_NAME_COUNT;
		case TSServerInfo.IMG_MODULE_IDX:
			return RequestCount.TYPE_IMAGE_COUNT;
		case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
		case TSServerInfo.INSTR_NO_MODULE_IDX:
			return RequestCount.TYPE_INSTRUMENT_COUNT;
		}
		
		try{
			throw new Exception("Bad module Id for counting request on " + getDataSite().getSTCounty());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

}

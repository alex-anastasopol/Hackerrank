package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.parser.SimpleHtmlParser.Input;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

/**
 * @author mihaib
*/

public class MOCassRO extends TSServer implements TSServerROLikeI{

	private static final long serialVersionUID = 1L;
	private boolean downloadingForSave;
	private static final Pattern INTERM_TABLE_PAT = Pattern.compile("(?is)(<table[^>]*>\\s*<tr[^>]*>\\s*<td[^>]*>(?:\\s*<strong>\\s*)?(?:<font[^>]*>)?[nbsp;&\\s]+Detail.*?</table>)");
	private static final Pattern PAGES_PAT = Pattern.compile("(?is)<option\\s+value.*?selected[^>]*>([^<]+)<");
	private static final Pattern PRIOR_PAT = Pattern.compile("(?is)<a\\s+class='list'\\s+href\\s*=\\s*\\'([^\\']+)\\'\\s*>Prior\\s*Page");
	private static final Pattern NEXT_PAT = Pattern.compile("(?is)<a\\s+class='list'\\s+href\\s*=\\s*\\'([^\\']+)\\'\\s*>Next\\s*Page");
	private static final Pattern FRAME_DOC_PAT = Pattern.compile("(?is)<FRAME\\s+SRC\\s*=\\s*\\\"([^\\\"]+)\\\"\\s*NAME\\s*=\\s*\\\"FrmDes");
	private static final Pattern IMG_LINK_PAT = Pattern.compile("(?is)target\\s*=\\s*\\\"\\s*FrmImg\\s*\\\"\\s*href\\s*=\\s*\"\\s*javascript:top\\.FrmImg\\.document\\.location\\.replace\\s*\\(\\s*'([^']+)");
	private static final Pattern TABLE_PAT = Pattern.compile("(?is)<table.*?</tr>\\s*</table>");
	public static final Pattern SORT_LINK_PAT = Pattern.compile("(?is)<a\\s+href\\s*=\\s*\\\"([^\\\"]+)");
	
	private static final Pattern CERT_DATE_PAT = Pattern.compile("(?is)Verified\\s+as\\s+of\\s*([\\d/]+)");
	
	//private static String[] INSTRUMENT_TYPE_PLAT = {"PLAT", "PLT1824", "PLT2436", "SURV", "SUV LARGE", "LS", "PLAN", "RB", "AROW", "EASE", "ROW", "REST", "RST", "COV", "RESA"};
	private static String docType = "PLAT,PLT1824,PLT2436,SURV,SUV LARGE,LS,PLAN,RB,AROW,EASE,ROW,REST,RST,COV,RESA";
	public MOCassRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public MOCassRO(long searchId){
		super(searchId);
	}
	

	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
			
		if(tsServerInfoModule != null) {
			
            setupSelectBox(tsServerInfoModule.getFunction(4), RecSetSize);
            setupSelectBox(tsServerInfoModule.getFunction(5), PageSize);
            tsServerInfoModule.getFunction(6).setParamType(TSServerInfoFunction.idCheckBox);
            tsServerInfoModule.getFunction(6).setHtmlformat(
					"<INPUT TYPE=\"checkbox\" NAME=\"ShowProperties\" VALUE=\"YES\"  >");
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(0), search_by);
            setupSelectBox(tsServerInfoModule.getFunction(4), RecSetSize);
            setupSelectBox(tsServerInfoModule.getFunction(5), PageSize);
            tsServerInfoModule.getFunction(6).setParamType(TSServerInfoFunction.idCheckBox);
            tsServerInfoModule.getFunction(6).setHtmlformat(
					"<INPUT TYPE=\"checkbox\" NAME=\"ShowProperties\" VALUE=\"YES\"  >");
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(0), search_by);
            setupSelectBox(tsServerInfoModule.getFunction(4), RecSetSize);
            setupSelectBox(tsServerInfoModule.getFunction(5), PageSize);
            tsServerInfoModule.getFunction(7).setParamType(TSServerInfoFunction.idCheckBox);
            tsServerInfoModule.getFunction(7).setHtmlformat(
					"<INPUT TYPE=\"checkbox\" NAME=\"ShowProperties\" VALUE=\"YES\"  >");
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if(tsServerInfoModule != null) {
            setupSelectBox(tsServerInfoModule.getFunction(1), RecSetSize);
            setupSelectBox(tsServerInfoModule.getFunction(2), PageSize);
            tsServerInfoModule.getFunction(4).setParamType(TSServerInfoFunction.idCheckBox);
            tsServerInfoModule.getFunction(4).setHtmlformat(
					"<INPUT TYPE=\"checkbox\" NAME=\"ShowProperties\" VALUE=\"YES\"  >");
		}
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(0), BookType);
            setupSelectBox(tsServerInfoModule.getFunction(2), RecSetSize);
            setupSelectBox(tsServerInfoModule.getFunction(3), PageSize);
            tsServerInfoModule.getFunction(4).setParamType(TSServerInfoFunction.idCheckBox);
            tsServerInfoModule.getFunction(4).setHtmlformat(
					"<INPUT TYPE=\"checkbox\" NAME=\"ShowProperties\" VALUE=\"YES\"  >");
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		ConfigurableNameIterator nameIterator = null;
		String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm = (GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	  		 module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     
		     
		     String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
		     if (date!=null) {
		    	 module.getFunction(2).forceValue(date);
		     }
		     module.setValue(3, endDate);
		     
		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		     module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
		  	 module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));	
		     
		     module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;", "L;f;"} );
		 	 module.addIterator(nameIterator);
		 	 	
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
		    	 module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 date=gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				 if (date!=null) 
					 module.getFunction(2).forceValue(date);
				 module.setValue(3, endDate);
				 
				 module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			  	 module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));		
				 
				 module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;", "L;f;"} );
				 module.addIterator(nameIterator);
				 		
				 modules.add(module);
			 
		     }
	    }	 
	    
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);	
	}
	
	 private static Set<InstrumentI> getAllAoAndTaxReferences(Search search){
		 DocumentsManagerI manager = search.getDocManager();	
		 Set<InstrumentI> allAoRef = new HashSet<InstrumentI>();
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
		
		
		private static boolean addBookPageSearch(InstrumentI inst, TSServerInfo serverInfo, List<TSServerInfoModule> modules, long searchId, Set<String> searched, boolean isUpdate){

			 if(inst.hasBookPage()){
				String originalB = inst.getBook();
				String originalP = inst.getPage();
				
				String book = originalB.replaceFirst("^0+", "");
				String page = originalP.replaceFirst("^0+", "");
				if(!searched.contains(book+"_"+page)){
					searched.add(book+"_"+page);
				}else{
					return false;
				}
				
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.setData(1, book + "/" + page);
				
				modules.add(module);
				return true;
			}
			return false;
		}
		
		private boolean addInstNoSearch(InstrumentI inst, TSServerInfo serverInfo, List<TSServerInfoModule> modules, long searchId, Set<String> searched, boolean isUpdate){
			if ( inst.hasInstrNo() ){
			
				String instr = inst.getInstno().replaceFirst("^0+", "");
				if(!searched.contains(instr)){
					searched.add(instr);
				}else{
					return false;
				}
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.setData(0, instr);
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				modules.add(module);
				return true;
			}
			return false;
		}
		
		private void addIteratorModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId){
			GenericLegal legalFilter = (GenericLegal)LegalFilterFactory.getDefaultLegalFilter(searchId);
			legalFilter.disableAll();
			legalFilter.setEnableLot(true);
			legalFilter.setEnableBlock(true);
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK);
			module.clearSaKeys();
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
			module.addValidator(legalFilter.getValidator());
			module.addIterator(it);
			modules.add(module);
		}
		
		private void addIteratorModuleSTR(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId){
        
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE);
			module.clearSaKeys();
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
			module.addIterator(it);
			modules.add(module);
		}
		
		static protected class LegalDescriptionIterator extends GenericRuntimeIterator<PersonalDataStruct> {
			
			private static final long serialVersionUID = 9238625486117069L;
			
			LegalDescriptionIterator(long searchId) {
				super(searchId);
			}
			
			@SuppressWarnings("unchecked")
			static  List<PersonalDataStruct> createDerivationInternal(long searchId){
				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				DocumentsManagerI m = global.getDocManager();
				List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)global.getAdditionalInfo("MO_CASS_RO_LOOK_UP_DATA");
				
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
					
					try{
						
						m.getAccess();
						List<RegisterDocumentI> listRodocs = m.getRoLikeDocumentList(true);
						DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_ASC);
						for( RegisterDocumentI reg: listRodocs){
								for (PropertyI prop: reg.getProperties()){
									if(prop.hasLegal()){
										LegalI legal = prop.getLegal();
										
										if(legal.hasSubdividedLegal()){
											hasLegal = true;
											PersonalDataStruct legalStructItem = new PersonalDataStruct();
											SubdivisionI subdiv = legal.getSubdivision();
											
											String subName = subdiv.getName();
											String block = subdiv.getBlock();
											String lot = subdiv.getLot();
											String[]lots = lot.split("  ");
											if (StringUtils.isNotEmpty(subName)){
												for (int i = 0; i < lots.length; i++){
													legalStructItem = new PersonalDataStruct();
													legalStructItem.subName = subName;
													legalStructItem.block = StringUtils.isEmpty(block) ? "" : block;
													legalStructItem.lot = lots[i];
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
											if (StringUtils.isNotEmpty(sec)){
												legalStructItem.section = StringUtils.isEmpty(sec) ? "" : sec;
												legalStructItem.township = StringUtils.isEmpty(tw) ? "" : tw;
												legalStructItem.range = StringUtils.isEmpty(rg) ? "" : rg;
												
												if( !testIfExist(legalStructList, legalStructItem, "sectional") ){
													legalStructList.add(legalStructItem);
												}
											}
										}
									}
								}
						}

						global.setAdditionalInfo("MO_CASS_RO_LOOK_UP_DATA", legalStructList);
						
					}
					finally{
						m.releaseAccess();
					}
				} else {
					for(PersonalDataStruct struct:legalStructList){
						if (StringUtils.isNotEmpty(struct.subName)){
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
				return createDerivationInternal(searchId);
			}
			
			protected void loadDerrivation(TSServerInfoModule module, PersonalDataStruct str){
				FilterResponse lotFilter = LegalFilterFactory.getDefaultLotFilter(searchId);
				FilterResponse blockFilter = LegalFilterFactory.getDefaultBlockFilter(searchId);
				if (StringUtils.isEmpty(str.block) && 
								module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION).equals(TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK)){
						module.setVisible(false);
				} else {	
						module.setData(1, str.subName + "/" + str.block);
						module.setData(0, "Subdivision/Block");
						module.addValidator(blockFilter.getValidator());
						module.setVisible(true);
				}
				if (StringUtils.isEmpty(str.lot) && 
							module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION).equals(TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK)){
						module.setVisible(false);
				} else {
					module.setData(1, str.subName + "/" + str.lot);
					module.setData(0, "Subdivision/Lot");
					module.addValidator(lotFilter.getValidator());
					module.setVisible(true);
				}
				if (StringUtils.isNotEmpty(str.section) && 
							module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION).equals(TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE)){
					module.setData(1, str.section + "/" + str.township + "/" + str.range);
					module.setData(0, "Section/Township/Range");
					module.setVisible(true);

				}
			}
		}
		
		protected static class PersonalDataStruct implements Cloneable{
			String subName 	= "";
			String lot		= "";
			String block 	= "";
			String section	= "";
			String township	= "";
			String range	= "";
			
			@Override
			protected Object clone() throws CloneNotSupportedException {
				return super.clone();
			}
			
			public boolean equalsSubdivision(PersonalDataStruct struct) {
				return this.block.equals(struct.block) && this.lot.equals(struct.lot) && this.subName.equals(struct.subName);
			}
			
			public boolean equalsSectional(PersonalDataStruct struct) {
				return this.section.equals(struct.section) && this.township.equals(struct.township) && this.range.equals(struct.range);
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
			} 
			return false;
		}
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		Set<InstrumentI> allAoRef = getAllAoAndTaxReferences(global);
		
		Search search = getSearch();
		TSServerInfoModule m = null;
		SearchAttributes sa = search.getSa();
		
        FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
        FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
        FilterResponse subdivisionNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
        subdivisionNameFilter.setThreshold(new BigDecimal(0.90d));
        DocsValidator subdivisionNameValidator = subdivisionNameFilter.getValidator();
        GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
        addressFilter.setEnableUnit(true);
        
        // P1   instrument list search from AO and TR for finding Legal
        addAoAndTaxReferenceSearches(serverInfo, modules, allAoRef, searchId, isUpdate());
        
        //P2     search with subdivision/block from RO documents
		addIteratorModule(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId);
		
		//P3    search with sec/twp/rng from RO documents
		addIteratorModuleSTR(serverInfo, modules, TSServerInfo.SUBDIVISION_MODULE_IDX, searchId);
        
        ConfigurableNameIterator nameIterator = null;
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter( 
				SearchAttributes.OWNER_OBJECT , searchId , m );
		
		//P4     name modules with names from search page.
		if (hasOwner()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.clearSaKeys();
			m.forceValue(2, "01/01/1960");
			m.setSaKey(3, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			
			((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
			((GenericNameFilter) defaultNameFilter).setUseArrangements(false);
			((GenericNameFilter) defaultNameFilter).setInitAgain(true);
			m.addFilter(rejectSavedDocuments);
			m.addFilter(defaultNameFilter);
			m.addFilter(new LastTransferDateFilter(searchId));
			addFilterForUpdate(m, true);
			//m.addFilter(crossReferenceToInvalidated);
			m.addValidator(defaultLegalFilter.getValidator());
			
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L;f;", "L;m;"});
			m.addIterator(nameIterator);
			modules.add(m);
    	}
		
		//P5    search by buyers
		if (hasBuyer() && !global.isProductType(SearchAttributes.SEARCH_PROD_REFINANCE)){
        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
        	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        			TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
        	m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
        	m.clearSaKeys();
        	m.forceValue(2, "01/01/1960");
			m.setSaKey(3, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			
			m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
        	
        	FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT , searchId , m );
        	((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
        	((GenericNameFilter) nameFilter).setUseArrangements(false);
        	((GenericNameFilter) nameFilter).setInitAgain(true);
			//nameFilter.setSkipUnique(false);
			FilterResponse doctTypeFilter = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
			//{ "CONS", "FINS", "LEV", "LEVY", "ORDI", "ORDR", "PLEA", "RATI", "RESO", "STAT", "ABS", "ABST", 
			//"JUDG", "BOND", "LIEN", "SL", "UCC1", "FTX", "STX", "FCON", "FNST", "MECH", "LISP", "UC&R", "VU", "WL", "WOL"}
			String[] docTypes = { "LIEN"	,	"COURT"};
			((DocTypeSimpleFilter)doctTypeFilter).setDocTypes(docTypes);
        	m.addFilter(rejectSavedDocuments);
			m.addFilter(nameFilter);
			addFilterForUpdate(m, true);
			m.addValidator(doctTypeFilter.getValidator());
			m.addValidator(defaultLegalFilter.getValidator());
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			ConfigurableNameIterator buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L;f;", "L;m;"});
			buyerNameIterator.setAllowMcnPersons(false);
			m.addIterator(buyerNameIterator);
			modules.add(m);
        }

		//P5    search by sub name for plats, easements and restrictions
		String subdivisionName =  sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		if( StringUtils.isNotEmpty(subdivisionName) ){
			m = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX) );
			m.clearSaKeys();
			m.forceValue(2, "01/01/1960");
			m.setSaKey(3, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.setData( 0, subdivisionName );
			m.setData( 1, docType );
			
			m.addValidator(subdivisionNameValidator);
			m.addValidator(defaultLegalFilter.getValidator());
			
			modules.add(m);
		} else {
			printSubdivisionException();
		}
		
		//P6    OCR last transfer - instrument search
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.clearSaKeys();
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		m.addFilter(new GenericInstrumentFilter(searchId));
		m.addCrossRefValidator(defaultLegalFilter.getValidator());
		modules.add(m);
		
		
		//P7    name module with names added by OCR
    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
    	m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
    	m.clearSaKeys();

		((GenericNameFilter)defaultNameFilter).setIgnoreMiddleOnEmpty(true);
    	m.addFilter(rejectSavedDocuments);
		m.addFilter(defaultNameFilter);
		m.addFilter(new LastTransferDateFilter(searchId));
		m.addValidator(defaultLegalFilter.getValidator());
		m.forceValue(2, "01/01/1960");
		m.setSaKey(3, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);		
		ArrayList<NameI> searchedNames = null;
		if (nameIterator != null) {
			searchedNames = nameIterator.getSearchedNames();
		} else {
			searchedNames = new ArrayList<NameI>();
		}
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId,
						new String[] {"L;f;", "L;m;"});
		// get your values at runtime
		nameIterator.setInitAgain(true);
		nameIterator.setSearchedNames(searchedNames);
		m.addIterator(nameIterator);
		modules.add(m);

	    serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		
		if (viParseID == ID_SEARCH_BY_NAME) {
			Matcher mat = SORT_LINK_PAT.matcher(initialResponse);
			int pageNo = 1;
			String currentPage = StringUtils.extractParameterFromUrl(Response.getQuerry(), "PageNumber");
			if (StringUtils.isNotEmpty(currentPage)){
				pageNo = Integer.parseInt(currentPage);
			}
			if (pageNo == 1){
				while (mat.find()){
					if (mat.group(1).contains("OrderBy=file_num")){
						String url = mat.group(1);
						url = url.replaceAll("\r", "").replaceAll("\n", "").replaceAll("\t", "").replaceAll("Accending=DESC", "Accending=ASC");
						HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
						try {
							initialResponse = ((ro.cst.tsearch.connection.http2.MOCassRO)site).getPage(url); 
						}catch(Exception e) {
							e.printStackTrace();
						}finally {
							HttpManager.releaseSite(site);
						}
						break;
					}
				}
			}
		}
		String rsResponse = initialResponse;
	
		//String linkStart = CreatePartialLink(TSConnectionURL.idGET);

		switch (viParseID) {
		
			case ID_SEARCH_BY_NAME :
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					
					String table = "";
					Matcher intermTableMatcher = INTERM_TABLE_PAT.matcher(rsResponse);
					if(!intermTableMatcher.find() || rsResponse.indexOf("No record matches your search for") > 0) {
						rsResponse ="<table><th><b>No records found! Please try again.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
						return;
					}else {
						table = intermTableMatcher.group(1);
					}
													
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, table, outputTable);
											
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
		            }
					
	
				}catch(Exception e) {
					e.printStackTrace();
				}
				break;
				
			case ID_DETAILS :
				
				String details = "";
				details = getDetails(rsResponse, Response);
				
				String docNo = "", book = "", page = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);
					docNo = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(mainList, "File No"), "").trim();
					String bookPage = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(mainList, "Book/Page"), "").trim();
					String[] bp = bookPage.split("/");
					if (bp.length == 2){
						book = bp[0].trim();
						page =  bp[1].trim();
					}
					docNo += book + page;
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (Response.getParsedResponse().getImageLinksCount() == 0){
					Pattern pat = Pattern.compile("<input.*?value=\\\"([^\\\"]*)\\\"");
					Matcher mat = pat.matcher(details);
					if (mat.find()){
						Response.getParsedResponse().addImageLink(new ImageLinkInPage(CreatePartialLink(TSConnectionURL.idGET) + mat.group(1), docNo + ".pdf"));
						details = details.replaceAll("(?is)</?form>", "");
						details = details.replaceAll("(?is)<input[^>]+>", "");
					}
				}
				if ((!downloadingForSave))
				{	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + docNo + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					//HashMap<String, String> data = new HashMap<String, String>();
					
					if (isInstrumentSaved(docNo, null, null)) 
					{
	                	details += CreateFileAlreadyInTSD();
					}
					else 
					{
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
	                
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
	            } 
				else 
	            {      
					details = details.replaceAll("View Image", "");
					String resultForCross = details;
					details = details.replaceAll("(?is)<a[^>]*>\\s*View\\s*Detail\\s*</a>", "");
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = docNo + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
	                
	                Pattern crossRefLinkPattern = Pattern.compile("(?ism)<a[^>]*?href=\\\"(.*?)\\\">\\s*View\\s+Detail\\s*</a>");
	                Matcher crossRefLinkMatcher = crossRefLinkPattern.matcher(resultForCross);
	                while(crossRefLinkMatcher.find()) {
		                ParsedResponse prChild = new ParsedResponse();
		                String link = crossRefLinkMatcher.group(1) + "&isSubResult=true";
		                LinkInPage pl = new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD);
		                prChild.setPageLink(pl);
		                Response.getParsedResponse().addOneResultRowOnly(prChild);
	                }

	                
				}
				
				break;	
			
			case ID_GET_LINK :
				if (sAction.indexOf("details.asp") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} else if (sAction.indexOf("search_entry") != -1) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				
				break;
			case ID_SAVE_TO_TSD :

				//if (rsResponse.indexOf("FrmDes") != -1){
				//	ParseResponse(sAction, Response, ID_DETAILS);
				//} else {
					downloadingForSave = true;
					ParseResponse(sAction, Response, ID_DETAILS);
					downloadingForSave = false;
				//}
				break;

			
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			
			table = table.replaceAll("(?is)<a class=\\\"filter_list\\\"[^>]+>", "")
						 .replaceAll("(?is)<a href=\"new_sch.asp[^>]+>","");
	
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(null);
			TableTag mainTable = (TableTag)mainTableList.elementAt(0);
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>");
			
			TableRow[] rows = mainTable.getRows();
			newTable.append(rows[0].toHtml().replaceAll("(?is)(<tr[^>]*>)", "$1 <td width=\"5%\" align=\"justify\" bgcolor=\"#003090\">" + SELECT_ALL_CHECKBOXES + "</td>"));
			//int i = 0;
			for(int i = 0; i < rows.length; i++ ) {
			//for(TableRow row : rows ) {
				TableRow row = rows[i];
				if(row.getColumnCount() > 0) {
					
					TableColumn[] cols = row.getColumns();
					
					NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
					if (aList.size() == 0) {
						continue;
					} else {
						
							String lnk = "/or_wb1/" + ((LinkTag) aList.elementAt(0)).getLink().replaceAll("\\s", "");
							String documentNumber = StringUtils.extractParameterFromUrl(lnk, "file_num");
							String tmpBook = cols[4].toPlainTextString().replaceAll("&nbsp;", " ").trim();
							String tmpPage = cols[5].toPlainTextString().replaceAll("&nbsp;", " ").trim();
							String key = documentNumber;
							if (StringUtils.isNotEmpty(tmpBook)){
								key += "_" + tmpBook + "_" + tmpPage;
							}
							
							ParsedResponse currentResponse = responses.get(key);							 
							if(currentResponse == null) {
								currentResponse = new ParsedResponse();
								responses.put(key, currentResponse);
							}
							
							RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();
							String tmpPartyGtor = "", tmpPartyGtee = "";
							if (cols[0].toPlainTextString().contains("*")){
								tmpPartyGtor = cols[1].toPlainTextString().replaceAll("&nbsp;", " ").trim();
							} else {
								tmpPartyGtee = cols[1].toPlainTextString().replaceAll("&nbsp;", " ").trim();
							}
							//String serverDocType = cols[3].toPlainTextString().replaceAll("&nbsp;", " ").trim();
							ResultMap resultMap = new ResultMap();
							
							String link = CreatePartialLink(TSConnectionURL.idGET) + lnk;
							if(document == null) {	//first time we find this document
								
								//String recDate = cols[2].toPlainTextString().replaceAll("&nbsp;", " ").trim();
								
								String legalDescription = cols[6].toPlainTextString();
								resultMap.put("PropertyIdentificationSet.PropertyDescription", legalDescription);
								
								int count = 1;
								
								
								while((i + 1)  < rows.length) {
									TableColumn[] newCols = rows[i+1].getColumns();
									//i++;
			    					if(newCols.length == 10) {	//we must have exactly 10 tds
			    						aList = newCols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
			    						if(aList.size() > 0) {
			    							LinkTag lk = (LinkTag)aList.elementAt(0);
			    							String newDocumentNumber = StringUtils.extractParameterFromUrl(lk.getLink(), "file_num");
			    							String tmpNewBook = newCols[4].toPlainTextString().replaceAll("&nbsp;", " ").trim();
			    							String tmpNewPage = newCols[5].toPlainTextString().replaceAll("&nbsp;", " ").trim();
			    							if(newDocumentNumber.equals(documentNumber) && tmpNewBook.equals(tmpBook) && tmpNewPage.equals(tmpPage)) {
			    								i++;
			    								if (newCols[0].toPlainTextString().contains("*")){
			    									tmpPartyGtor += "/" + newCols[1].toPlainTextString().replaceAll("&nbsp;", " ").trim();
			    								} else {
			    									tmpPartyGtee += "/" + newCols[1].toPlainTextString().replaceAll("&nbsp;", " ").trim();
			    								}
			    								
			    								count++;
			    							} else {
			    								break;
			    							}
			    						}
			    					} else {
			    						break;
			    					}
			    				}
								;
								String rowHtml =  row.toHtml().replaceFirst("(?is)<a[^>]+>View</a>","<a href=\"" + link + "\">View</a>" );
								tmpPartyGtor = org.apache.commons.lang.StringUtils.strip(tmpPartyGtor, "/");
								tmpPartyGtee = org.apache.commons.lang.StringUtils.strip(tmpPartyGtee, "/");
								rowHtml = rowHtml.replaceAll("(?is)(<tr[^>]*>\\s*<td[^>]+>.*?</td>)\\s*<td[^>]+>.*?</td>", 
										"$1 <td width=\"20%\" align=\"justify\" ><font face=\"Verdana\" size=\"1\" color=\"BLACK\">" + tmpPartyGtor +
										"</font></td>\r\n<td width=\"20%\" align=\"justify\" ><font face=\"Verdana\" size=\"1\" color=\"BLACK\">" +
										tmpPartyGtee + "</font></td>\r\n");
								//ParsedResponse currentResponse = new ParsedResponse();
								tmpPartyGtor = StringEscapeUtils.unescapeHtml(tmpPartyGtor);
								tmpPartyGtee = StringEscapeUtils.unescapeHtml(tmpPartyGtee);
								resultMap.put("tmpPartyGtor", tmpPartyGtor);
								resultMap.put("tmpPartyGtee", tmpPartyGtee);
			    				resultMap.put("SaleDataSet.DocumentNumber", documentNumber);
			    				try {
									ro.cst.tsearch.servers.functions.MOCassRO
											.parseNameInterMOCassRO(resultMap, getSearch().getID());
								} catch (Exception e) {
									e.printStackTrace();
								}
			    				resultMap.removeTempDef();
			    				
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>" + 
										row.toHtml() + "</table>");
								
								resultMap = parseIntermediaryRow(resultMap, row, searchId);
								
								Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
								document = (RegisterDocumentI)bridge.importData();
								
								//DocumentI document = (RegisterDocumentI)bridge.importData();				
								
								currentResponse.setDocument(document);
								String checkBox = "checked";
								if (isInstrumentSaved(documentNumber, document, null) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
					    			checkBox = "saved";
					    		} else {
					    			numberOfUncheckedElements++;
					    			LinkInPage linkInPage = new LinkInPage(
					    					CreatePartialLink(TSConnectionURL.idGET) + lnk, 
					    					CreatePartialLink(TSConnectionURL.idGET) + lnk, 
					    					TSServer.REQUEST_SAVE_TO_TSD);
					    			checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\"" + CreatePartialLink(TSConnectionURL.idGET) + lnk + "\">";
			            			currentResponse.setPageLink(linkInPage);
					    		}
								rowHtml = rowHtml.replaceAll("(?is)<tr[^>]*>", 
											"<tr><td  align=\"justify\" width=\"5%\" nowrap><font face=\"Verdana\" size=\"1\" rowspan=" + count + ">" + checkBox + "</td>");
								currentResponse.setOnlyResponse(rowHtml);
								newTable.append(currentResponse.getResponse());
								
								count++;
								intermediaryResponse.add(currentResponse);
							
							} else {
								//i++;
								tmpPartyGtor = StringEscapeUtils.unescapeHtml(tmpPartyGtor);
								tmpPartyGtee = StringEscapeUtils.unescapeHtml(tmpPartyGtee);
								resultMap.put("tmpPartyGtor", tmpPartyGtor);
								resultMap.put("tmpPartyGtee", tmpPartyGtee);
			    				resultMap.put("SaleDataSet.DocumentNumber", documentNumber);
			    				resultMap.put("OtherInformationSet.SrcType","RO");
			    				try {
									ro.cst.tsearch.servers.functions.MOCassRO
											.parseNameInterMOCassRO(resultMap, getSearch().getID());
								} catch (Exception e) {
									e.printStackTrace();
								}
								resultMap.removeTempDef();
			    				Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
			    				RegisterDocumentI documentTemp = (RegisterDocumentI)bridge.importData();
			    			
			    				for(NameI nameI : documentTemp.getGrantee().getNames()) {
			    					if(!document.getGrantee().contains(nameI)) {
			    						document.getGrantee().add(nameI);
			    					}
			    				}
			    				for(NameI nameI : documentTemp.getGrantor().getNames()) {
			    					if(!document.getGrantor().contains(nameI)) {
			    						document.getGrantor().add(nameI);
			    					}
			    				}
			    				String rawServerResponse = (String)currentResponse.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE);
			    				
			    				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rawServerResponse);
			    				
			    				String responseHtml = currentResponse.getResponse();
			    				String countString = StringUtils.extractParameter(responseHtml, "rowspan=(\\d)+");
			    				try {
			    					int count = Integer.parseInt(countString);
			    					responseHtml = responseHtml.replaceAll("rowspan=(\\d)+", "rowspan=" + (count + 1));
			    					
			    					currentResponse.setOnlyResponse(responseHtml);
			    				} catch (Exception e) {
									e.printStackTrace();
								}
			    				
			    				
							}
						}

				}
			}
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
				
				String footer = proccessLinks(response) + "<br><br><br>";
				String header0 = "<div>";
				Matcher pagesMat = PAGES_PAT.matcher(response.getResult());
				if (pagesMat.find()){
					header0 = "Page: " + pagesMat.group(1);
				}
				header0 += "&nbsp;&nbsp;"+ footer  + "</div>";
				String header1 = rows[0].toHtml();
				header1 = header1.replaceAll("(?is)</td>\\s*(<td[^>]*>.*?)Party Name(.*?</td>)", "$1 Grantor $2 \r\n $1 Grantee $2");
				header1 = header1.replaceAll("(?is)(<tr[^>]*>)", "$1 <td width=\"5%\" align=\"justify\" bgcolor=\"#003090\">" + SELECT_ALL_CHECKBOXES + "</td>");
				;
				
				response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") + header0
											+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
				response.getParsedResponse().setFooter("</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
			}	
		
		newTable.append("</table>");
		outputTable.append(newTable);
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	protected ResultMap parseIntermediaryRow(ResultMap resultMap, TableRow row, long searchId) {
		return ro.cst.tsearch.servers.functions.MOCassRO.parseIntermediaryRow(resultMap, row, searchId);
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.MOCassRO.parseAndFillResultMap(detailsHtml, map, searchId);
		return null;
	}
	
	private String proccessLinks(ServerResponse response) {
		String nextLink = "", prevLink = "";
		String footer = "";
		
		try {
			//String qry = response.getQuerry();
			String rsResponse = response.getResult();
			Matcher priorMat = PRIOR_PAT.matcher(rsResponse);
			if (priorMat.find()){
				prevLink = CreatePartialLink(TSConnectionURL.idGET) + "/or_wb1/" + priorMat.group(1);
				prevLink = prevLink.replaceAll("(?is)OrderBy=[^&]*", "OrderBy=file_num");
			}
			
			Matcher nextMat = NEXT_PAT.matcher(rsResponse);
			if (nextMat.find()){
				nextLink = CreatePartialLink(TSConnectionURL.idGET) + "/or_wb1/" + nextMat.group(1);
				nextLink = nextLink.replaceAll("(?is)OrderBy=[^&]*", "OrderBy=file_num");
			}
			
			if (StringUtils.isNotEmpty(prevLink)){
				footer = "<a href=\"" + prevLink + "\">Prior Page</a>&nbsp;&nbsp;&nbsp;";
			}
			if (StringUtils.isNotEmpty(nextLink)){
				footer += "&nbsp;&nbsp;&nbsp;<a href=\"" + nextLink + "\">Next Page</a>";
			}
			
			response.getParsedResponse().setNextLink( "<a href='"+nextLink+"'>Next</a>" );
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return footer;
	}
	
	@SuppressWarnings("deprecation")
	protected String getDetails(String response, ServerResponse Response){
		
		// if from memory - use it as is
		if(!(response.contains("<html") || response.contains("<HTML"))){
			return response;
		}
		String resp = "";
		Matcher detailsMat = FRAME_DOC_PAT.matcher(response);
		if (detailsMat.find()){
			String src = detailsMat.group(1);
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				resp = ((ro.cst.tsearch.connection.http2.MOCassRO)site).getPage(src); 
				response = resp;
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				HttpManager.releaseSite(site);
			}
		}
		String docNo = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);
			docNo = HtmlParser3.getValueFromSecondCell(HtmlParser3.findNode(mainList, "File No"), "").trim();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String details = "";
		
		if (StringUtils.isNotEmpty(resp)){
			Matcher matTable = TABLE_PAT.matcher(resp);
			while (matTable.find()){
				if (matTable.group(0).contains("Document Detail") || matTable.group(0).contains("Legal")){
					details += matTable.group(0);
				}
			}
		}
		
			Matcher imageMat = IMG_LINK_PAT.matcher(response);
			if (imageMat.find()){
				String src = imageMat.group(1);
				if (response.indexOf("Get Image") != -1){
					Response.getParsedResponse().addImageLink(new ImageLinkInPage(CreatePartialLink(TSConnectionURL.idGET) + src, docNo + ".pdf"));
					
					if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ){
						HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
						try {
							resp = ((ro.cst.tsearch.connection.http2.MOCassRO)site).getPage(src);
							Form form = new SimpleHtmlParser(resp).getForm("courtform");
							HashMap<String, String> params = new HashMap<String, String>();
							//HTTPRequest req = new HTTPRequest(form.action, HTTPRequest.POST);
							if (form != null){
							List<Input> inputs = form.inputs;
								for(Input inp : inputs){
									//if (inp.name.contains("page") || inp.name.contains("WaterMarkText") || inp.name.contains("id")){
										//req.setPostParameter(inp.name, inp.value);
										params.put(inp.name, inp.value);
									//}
								}
			
								mSearch.setAdditionalInfo("MOCassRO:image:", params);
								String linkStart = CreatePartialLink(TSConnectionURL.idPOST);
								details += "<br><br><br>" 
										+ "<a href=\"" + linkStart + "/servlets-examples/servlet/PdfServlet27" + "&img=img" + "\" target=\"_blank\">View Image</a><br><br>";
							}
						} catch(Exception e) {
							e.printStackTrace();
						}finally {
							HttpManager.releaseSite(site);
						}
					}
					details += "<form><input type=\"hidden\" name=\"hideme\" value=\"" + src + "\" /> </form>";
				}
		}
		Pattern crossLinkPat = Pattern.compile("(?is)<a.*?href\\s*=\\s*'([^']*)'>\\s*View\\s*Detail\\s*</a>");
		Matcher crossLinkMat = crossLinkPat.matcher(details);
		while (crossLinkMat.find()){
			String aLink = crossLinkMat.group(0);
			details = details.replace(aLink, 
					"<a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/or_wb1/" + crossLinkMat.group(1) + "\">View Detail</a>");
		}
		
		details = details.replaceAll("(?is)(<tr>\\s*<td[^>]*>Linked.*?</tr>\\s*)<tr>\\s*<td>&nbsp;</td>\\s*</tr>", 
				"<tr><td><table id=\"crossRef\">$1</table></td></tr>");
						
		return details;
	}
		
	@Override
	protected void setCertificationDate() {

		if(false) {
			try {
				 
			String countyName = dataSite.getCountyName();
	        logger.debug("Intru pe get Certification Date - " + countyName);
	
	        if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
		        String html = "";
	    		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
	    		try{
	    			html = site.process(new HTTPRequest("http://207.14.218.122/or_wb1/or_sch_1.asp")).getResponseAsString();
	    		} catch(RuntimeException e){
	    			e.printStackTrace();
	    		} finally {
	    			HttpManager.releaseSite(site);
	    		}   
	        	
	    		Matcher certDateMatcher = CERT_DATE_PAT.matcher(html);
	            if(certDateMatcher.find()) {
	            	String date = certDateMatcher.group(1).trim();
	            	
	            	CertificationDateManager.cacheCertificationDate(dataSite, date);
	            	getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
	            }
			}
	        } catch (Exception e) {
	            logger.error(e.getMessage());
	        }
		}
	}
	
	
	@SuppressWarnings("deprecation")
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {

			String link = image.getLink();
			link = link.replaceAll(".*?(details_img)", "$1");
			byte[] imageBytes = null;

	    	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				imageBytes = ((ro.cst.tsearch.connection.http2.MOCassRO)site).getImage(link); 
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				HttpManager.releaseSite(site);
			}
			
			ServerResponse resp = new ServerResponse();
				
			if(imageBytes != null) {
				afterDownloadImage(true);
			} else {
				return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
			}
			
			String imageName = image.getPath();
			if(FileUtils.existPath(imageName)){
				imageBytes = FileUtils.readBinaryFile(imageName);
			   		return new DownloadImageResult( DownloadImageResult.Status.OK, imageBytes, image.getContentType() );
			}
			    	
			resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes,
						((ImageLinkInPage)image).getContentType()));

			DownloadImageResult dres = resp.getImageResult();
			
			//System.out.println("image");

		return dres;
	}
	
	private static final String RecSetSize = 
		"<select name=\"RecSetSize\">" +
			"<option value=\"100\"   >Show first 100 records</option>" + 
			"<option value=\"500\"   >Show first 500 records</option>" +
			"<option value=\"1000\"  >Show first 1,000 records</option>" +
			"<option value=\"2000\"  selected  >Show first 2,000 records</option>" +
		"</select>";
	
	private static final String PageSize = 
		"<select name=\"PageSize\">" +
			"<option value=\"20\"   >20" +
			"<option value=\"30\"   >30" +
			"<option value=\"50\"   >50" +
			"<option value=\"100\"  selected  >100" +
		"</select>";
	
	private static final String search_by = 
		"<select name=\"search_by\">" +
			"<option value=\"Subdivision Name\">Subdivision Name&nbsp;&nbsp;</option>" +
			"<option value=\"Subdivision/Lot\">Subdivision/Lot&nbsp;&nbsp;</option>" +
			"<option value=\"Subdivision/Block\">Subdivision/Block</option>" +
			"<option value=\"Subdivision/Lot/Block\">Subdivision/Lot/Block</option>" +
			"<option value=\"Section/Township/Range\">Section/Township/Range</option>" +
		"</select>";
	
	private static final String BookType = 
		"<select name=\"search_by\">" +
			"<option value=\"O\">Official Records Book</option>" +
		"</select>";
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		
		TSServerInfoModule module = null;
		if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		    module.forceValue(0, "O");
			module.forceValue(1, book + "/" + page);
			module.forceValue(2, "2000");
			module.forceValue(3, "100");
			module.forceValue(4, "Dynamic Book/Page");
			module.forceValue(5, "YES");
			
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
			module.getFilterList().clear();
			module.addFilter(filter);
			
		} else if(StringUtils.isNotEmpty(instrumentNumber)) {
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			filterCriteria.put("InstrumentNumber", instrumentNumber);
			module.forceValue(0, instrumentNumber);
			module.forceValue(1, "2000");;
			module.forceValue(2, "100");
			module.forceValue(3, "File Number");
			module.forceValue(4, "YES");
			GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
			module.getFilterList().clear();
			module.addFilter(filter);
		} 
		return module;
	}
}

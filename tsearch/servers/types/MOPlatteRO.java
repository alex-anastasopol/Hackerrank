package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
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
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI.IType;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;

/**
 * @author mihaib
*/

@SuppressWarnings("deprecation")
public class MOPlatteRO extends TSServerROLike{

	private static final long serialVersionUID = 1L;
	private boolean downloadingForSave;
	private static String partLink = "";
	private static final Pattern FOR_NEXT_PAT = Pattern.compile("(?is)style=\\\"[^\\\"]+\\\">[^<]+</a>\\s*.*?href=\\\"([^\\\"]+)");
	
	private static final Pattern IMG_LINK_PAT = Pattern.compile("(?is)(DisplayDocument\\.aspx[^']+)");
	
	private static String docTypeForPlats = "DE PLAT,DE ESMT,DE REST"; //DE PLAT,DE SURVEY,DE R/W,DE ESMT,DE REST,DE DISCLM,DE COV";
	private static String docTypeForBuyer = "DE JUDGMT,DT JUDGMT,DT LIEN,DT TRUST,DT UCC,TR JUDGMT";
	
	public static final String HTML_RESPONSE = "<table>" +
    		"<tr><td><b> TYPE:  </b><i>@@TYPE@@</i></td><td><b> RECORDED: </b>@@RECORDED@@</td><td><b> POSTED: </b></td></tr>"+ 
    		"<tr><td><b> BOOK:  </b><i>@@BOOK@@</i></td><td><b> PAGE: </b>@@PAGE@@</td><td><b> INSTRUMENT: </b>@@INSTRUMENT@@</td></tr>" +
    		"<tr><td></td><td></td><td></td></tr>" +
    		"<tr><td></td><td colspan=\"2\"></td></tr>" +
    		"<tr></tr>" +
    		"<tr></tr>" +
    		"<tr></tr>" +
    		"<tr><td><b>ORDER: </b></td><td><b>QUARTERVALUE: </b></td><td><b>ARB: </b></td></tr>" +
    		"<tr><td><b>THRUORDER: </b></td><td><b>THRUQUARTERVALUE: </b></td><td><b>THRUAREA: </b></td></tr>" +
    		"<tr><td><b>THRUPARCEL: </b></td></tr>" +
    		"<tr>&nbsp;</tr>" +
    		"<tr><td colspan=\"3\"><table cellspacing=\"0\" cellpadding=\"0\" border=\"1\">" +
    		"<tr><th>Party Role</th><th>Vesting Type</th><th>Last</th><th>First</th><th>Middle</th></tr>" +
    		"<tr><td>&nbsp;Grantee&nbsp;</td><td>&nbsp;&nbsp;</td><td>&nbsp;&nbsp;</td><td>&nbsp;&nbsp;</td><td>&nbsp;&nbsp;</td></tr>" +
    		"<tr><td>&nbsp;Grantor&nbsp;</td><td>&nbsp;&nbsp;</td><td colspan=\"3\">&nbsp;County of Platte&nbsp;</td></tr></table></td></tr>" +
    		"<tr></tr>" +
    		"<tr><td colspan=\"3\"><b>REMARKS:</b></td></tr>" +
    		"<tr><td><b> REL BOOK:  </b><i></i></td><td><b> REL PAGE: </b></td><td><b> REL DOC: </b></td></tr>" +
    		"<tr><td><b> PARENT BOOK:  </b><i></i></td><td><b> PARENT PAGE: </b></td><td><b> PARENT DOC: </b></td></tr>" +
    		"<tr><td><a href=\"@@LINK@@\">View image</a></td></tr>";
	
	public static String DETAILS_ADDRESS = "/details.aspx";
	public static String IMAGE_ADDRESS = "/image.aspx";

	public MOPlatteRO(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
		prepareLink();
	}

	public MOPlatteRO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,	int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
		prepareLink();
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
			
		if(tsServerInfoModule != null) {
            setupSelectBox(tsServerInfoModule.getFunction(2), NAME_TYPE);
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
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
	 
	 private void addIteratorModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId){
			FilterResponse lotFilter = LegalFilterFactory.getDefaultLotFilter(searchId);
			FilterResponse blockFilter = LegalFilterFactory.getDefaultBlockFilter(searchId);
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK);
			module.clearSaKeys();
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
			it.setEnableSubdividedLegal(true);
			module.addValidator(blockFilter.getValidator());
			module.addValidator(lotFilter.getValidator());
			module.addIterator(it);
			modules.add(module);
			
		}
	 
	 private void addIteratorModuleSTR(TSServerInfo serverInfo, List<TSServerInfoModule> modules, int code, long searchId){
	        
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE);
			
			FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module);
			((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
        	((GenericNameFilter) nameFilter).setUseArrangements(false);
        	((GenericNameFilter) nameFilter).setInitAgain(true);
        	FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
        	
			module.clearSaKeys();
			//module.addFilterForNext(new NameFilterForNext(searchId));
			LegalDescriptionIterator it = new LegalDescriptionIterator(searchId);
			it.setEnableTownshipLegal(true);
			module.addFilter(nameFilter);
			module.addFilter(defaultLegalFilter);
			module.addIterator(it);
			modules.add(module);
	}
		
	 static protected class LegalDescriptionIterator extends GenericRuntimeIterator<PersonalDataStruct> {
			
			private static final long serialVersionUID = 9238625486117069L;
			private boolean enableSubdividedLegal = false;
			private boolean enableTownshipLegal = false;
			
			LegalDescriptionIterator(long searchId) {
				super(searchId);
			}
			
			public boolean isEnableSubdividedLegal() {
				return enableSubdividedLegal;
			}
			
			public void setEnableSubdividedLegal(boolean enableSubdividedLegal) {
				this.enableSubdividedLegal = enableSubdividedLegal;
			}

			public boolean isEnableTownshipLegal() {
				return enableTownshipLegal;
			}

			public void setEnableTownshipLegal(boolean enableTownshipLegal) {
				this.enableTownshipLegal = enableTownshipLegal;
			}
			
			@SuppressWarnings("unchecked")
			List<PersonalDataStruct> createDerivationInternal(long searchId){
				Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				DocumentsManagerI m = global.getDocManager();
				String key = "MO_PLATTE_RO_LOOK_UP_DATA";
				if (isEnableSubdividedLegal()) key += "_SPL";
				else if (isEnableTownshipLegal()) key+= "_STR";
				List<PersonalDataStruct>  legalStructList = (List<PersonalDataStruct>)global.getAdditionalInfo(key);
				
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
										
										if(legal.hasSubdividedLegal() && isEnableSubdividedLegal()){
											hasLegal = true;
											PersonalDataStruct legalStructItem = new PersonalDataStruct();
											SubdivisionI subdiv = legal.getSubdivision();
											
											String subName = subdiv.getName();
											String block = subdiv.getBlock();
											String lot = subdiv.getLot();
											String[] lots = lot.split("  ");
											if (StringUtils.isNotEmpty(subName)){
												for (int i = 0; i < lots.length; i++){
													legalStructItem = new PersonalDataStruct();
													subName = subName.replaceAll("(?is)\\bADD(ITION)?\\b", "").replaceAll("(?is)(\\d+)[ST|ND|RD|TH]+\\b", "$1").trim();
													legalStructItem.subName = subName;
													legalStructItem.block = StringUtils.isEmpty(block) ? "" : block;
													legalStructItem.lot = lots[i];
													if( !testIfExist(legalStructList, legalStructItem, "subdivision") ){
														legalStructList.add(legalStructItem);
													}
												}
											}
										}
										if (legal.hasTownshipLegal() && isEnableTownshipLegal()){
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

						global.setAdditionalInfo(key, legalStructList);
						
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
				
				if (module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION).equals(TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT_BLOCK)){
					if (StringUtils.isNotEmpty(str.subName)){
						if (StringUtils.isNotEmpty(str.lot)){
							if (StringUtils.isNotEmpty(str.block)){
								module.setData(3, "LT " + str.lot + " BL " + str.block + " " + str.subName);
								module.addValidator(lotFilter.getValidator());
								module.addValidator(blockFilter.getValidator());
								module.setVisible(true);
							} else {
								module.setData(3, "LT " + str.lot + " " + str.subName);
								module.addValidator(lotFilter.getValidator());
								module.setVisible(true);
							}
						} else {
							if (StringUtils.isNotEmpty(str.block)){
								module.setData(3, "BL " + str.block + " " + str.subName);
								module.addValidator(blockFilter.getValidator());
								module.setVisible(true);
							} else {
								module.setVisible(false);
							}
						}
					} else {
						module.setVisible(false);
					}
				} else if (module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION).equals(TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_SECTION_TWP_RANGE)){
					if (StringUtils.isNotEmpty(str.section)){
						module.setData(3, "STR " + str.section + "-" + str.township + "-" + str.range);
						module.setVisible(true);
					} else {
						module.setVisible(false);
					}
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
		
        FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId){
			/**
			 * 
			 */
			private static final long serialVersionUID = -57860345458375627L;

			@Override
			public InstrumentI formatInstrument(InstrumentI instrument){
				
				String instrumentNo = instrument.getInstno();
				if (instrumentNo.matches("(?is)\\A\\d+.*") && instrumentNo.length() > 4){
					instrumentNo = instrumentNo.substring(0, 4) 
									+ org.apache.commons.lang.StringUtils.stripStart(instrumentNo.substring(4, instrumentNo.length()), "0");
					instrument.setInstno(instrumentNo);
				}

				return instrument;
			}
		};
        FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
        ((GenericLegal) defaultLegalFilter).setEnableLot(true);
        
        FilterResponse subdivisionNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
        subdivisionNameFilter.setThreshold(new BigDecimal(0.90d));
        
        DocsValidator subdivisionNameValidator = subdivisionNameFilter.getValidator();
        
        // P1   instrument list search from AO and TR for finding Legal
        addAoAndTaxReferenceSearches(serverInfo, modules, allAoRef, searchId, isUpdate());
        
        //P2     search with lot/block/subdivision from RO documents
		addIteratorModule(serverInfo, modules, TSServerInfo.INSTR_NO_MODULE_IDX, searchId);
		
		//P3    search with sec/twp/rng from RO documents
		addIteratorModuleSTR(serverInfo, modules, TSServerInfo.INSTR_NO_MODULE_IDX, searchId);
		
		//P4    search by sub name for plats, easements and restrictions
		String subdivisionName =  sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		if( StringUtils.isNotEmpty(subdivisionName) ){
			subdivisionName = subdivisionName.replaceAll("(?is)\\bADD(ITION)?\\b", "").replaceAll("(?is)(\\d+)[ST|ND|RD|TH]+\\b", "$1").trim();
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT);			
			m.clearSaKeys();
			m.forceValue(3, subdivisionName);
			m.forceValue(4, "01/01/1960");
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.forceValue(6, docTypeForPlats);
			m.addFilterForNext(new NameFilterForNext(searchId));
			m.addFilter(subdivisionNameFilter);
			m.addValidator(subdivisionNameValidator);
			m.addValidator(defaultLegalFilter.getValidator());
			
			modules.add(m);
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			TSServerInfoConstants.VALUE_PARAM_SUBDIVISION_LOT);			
			m.clearSaKeys();
			m.forceValue(0, subdivisionName);
			m.forceValue(4, "01/01/1960");
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.forceValue(6, docTypeForPlats);
			m.addFilterForNext(new NameFilterForNext(searchId));
			m.addFilter(subdivisionNameFilter);
			m.addValidator(subdivisionNameValidator);
			m.addValidator(defaultLegalFilter.getValidator());
			
			modules.add(m);
		} else {
			printSubdivisionException();
		}
        
        ConfigurableNameIterator nameIterator = null;
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter( 
				SearchAttributes.OWNER_OBJECT , searchId , m );
		
		//P5     name modules with names from search page.
		if (hasOwner()){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.clearSaKeys();
			m.forceValue(4, "01/01/1960");
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			
			((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
			((GenericNameFilter) defaultNameFilter).setUseArrangements(false);
			((GenericNameFilter) defaultNameFilter).setInitAgain(true);
			m.addFilter(rejectSavedDocuments);
			m.addFilter(defaultNameFilter);
			m.addFilter(new LastTransferDateFilter(searchId));
			m.addFilterForNext(new NameFilterForNext(searchId));
			addFilterForUpdate(m, true);
			//m.addFilter(crossReferenceToInvalidated);
			m.addValidator(defaultLegalFilter.getValidator());
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L;f;", "L;m;"});
			m.addIterator(nameIterator);
			modules.add(m);
    	}
		
		//P5    search by buyers
		if (hasBuyer() && !InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().isProductType(SearchAttributes.SEARCH_PROD_REFINANCE)){
        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
        	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
        			TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS);
        	m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
        	m.clearSaKeys();
        	m.forceValue(4, "01/01/1960");
			m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			m.forceValue(6, docTypeForBuyer);
        	
        	FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT , searchId , m );
        	((GenericNameFilter) nameFilter).setIgnoreMiddleOnEmpty(true);
        	((GenericNameFilter) nameFilter).setUseArrangements(false);
        	((GenericNameFilter) nameFilter).setInitAgain(true);
			//nameFilter.setSkipUnique(false);
			FilterResponse doctTypeFilter = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);
			String[] docTypes = { "LIEN"	,	"COURT"};
			((DocTypeSimpleFilter)doctTypeFilter).setDocTypes(docTypes);
        	m.addFilter(rejectSavedDocuments);
			m.addFilter(nameFilter);
			m.addFilterForNext(new NameFilterForNext(searchId));
			addFilterForUpdate(m, true);
			//m.addValidator(doctTypeFilter.getValidator());
			m.addValidator(defaultLegalFilter.getValidator());
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			ConfigurableNameIterator buyerNameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L;f;", "L;m;"});
			buyerNameIterator.setAllowMcnPersons(false);
			m.addIterator(buyerNameIterator);
			modules.add(m);
        }		
		
		//P6    OCR last transfer - instrument search
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.clearSaKeys();
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		m.addFilter(new GenericInstrumentFilter(searchId));
		m.addFilter(defaultLegalFilter);
		modules.add(m);
		//OCR last transfer - book page search
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.clearSaKeys();
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		m.addFilter(new GenericInstrumentFilter(searchId));
		m.addFilter(defaultLegalFilter);
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
		m.forceValue(4, "01/01/1960");
		m.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);		
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
		    	 module.getFunction(4).forceValue(date);
		     }
		     module.setValue(5, endDate);
		     
		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		     module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		     module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;", "L;f;"} );
		 	 module.addIterator(nameIterator);
		 	 
	         
             module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
		  	 module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));		
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
		    	 module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 date=gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				 if (date!=null) 
					 module.getFunction(4).forceValue(date);
				 module.setValue(5, endDate);
				 
				 module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			     module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;", "L;f;"} );
				 module.addIterator(nameIterator);
				 
				 module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			  	 module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));				
				 modules.add(module);
			 
		     }
	    }	 
	    
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);	
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
				
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.setData(1, book);
				module.setData(2, page);
				module.forceValue(4, "01/01/1960");
				module.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				HashMap<String, String> filterCriteria = new HashMap<String, String>();
				filterCriteria.put("Book", book);
				filterCriteria.put("Page", page);
				GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
				module.getFilterList().clear();
				module.addFilter(filter);
				
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
				module.forceValue(4, "01/01/1960");
				module.setSaKey(5, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				modules.add(module);
				return true;
			}
			return false;
		}
		
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		
		String rsResponse = initialResponse;

		if (rsResponse.indexOf("No Records Found") != -1){
			Response.getParsedResponse().setError("No results found");
			return;
		} else if (rsResponse.indexOf("ERROR: Please specify a instrument number, book or legal description for the search") != -1){
			Response.getParsedResponse().setError("ERROR: Please specify a instrument number, book or legal description for the search");
			return;
		} else if (rsResponse.indexOf("Please specify both a starting and ending date for a date range search") != -1){
			Response.getParsedResponse().setError("Please specify both a starting and ending date for a date range search");
			return;
		} else if (rsResponse.indexOf("Date span can not exceed a one year period") != -1){
			Response.getParsedResponse().setError("Date span can not exceed a one year period");
			return;
		} else if (rsResponse.indexOf("You must provide an instrument type for a date range search") != -1){
			Response.getParsedResponse().setError("You must provide an instrument type for a date range search");
			return;
		}
		
		switch (viParseID) {	
			case ID_SEARCH_BY_NAME :
			case ID_SEARCH_BY_INSTRUMENT_NO :

				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
																		
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
											
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
		            }
					
				} catch(Exception e) {
					e.printStackTrace();
				}
				break;
				
			case ID_DETAILS :
				
				String details = "";
				details = getDetails(rsResponse, Response);
				
				String docNo = "", book = "", page = "", imageLink = "", docType = "", year = "", instrDate = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);
					docNo = HtmlParser3.getValueFromNextCell(mainList, "Document No.", "", false).trim();
					book = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "Document No."), "", true).trim();
					page = HtmlParser3.getValueFromAbsoluteCell(2, 1, HtmlParser3.findNode(mainList, "Document No."), "", true).trim();
					docType = HtmlParser3.getValueFromAbsoluteCell(-1, 1, HtmlParser3.findNode(mainList, "Document No."), "", true).trim();
					docType = docType.replaceAll("(?is)\\s*-\\s*Unknown\\b", "").trim();
					docType = docType.replaceAll("(?is)\\s+", " ").trim();
					instrDate = HtmlParser3.getValueFromNextCell(mainList, "Dated date", "", false).trim();
					year = instrDate.replaceAll("(?is)\\d+/\\d+/(\\d{4})", "$1");
					LinkTag aList = (LinkTag) HtmlParser3.getNodeByID("imageLink", mainList, true);
					if (aList != null){
						imageLink = aList.getLink();
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (Response.getParsedResponse().getImageLinksCount() == 0){
					if (StringUtils.isNotEmpty(imageLink)){
						Response.getParsedResponse().addImageLink(new ImageLinkInPage(imageLink, docNo + ".pdf"));
					}
				}
				if ((!downloadingForSave))
				{	
	                String qry_aux = Response.getRawQuerry();
	                if (org.apache.commons.lang.StringUtils.isNotEmpty(qry_aux)){
	                	qry_aux = "dummy=" + docNo;
	                } else{
	                	qry_aux = "dummy=" + docNo + "&" + qry_aux;
	                }
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
    				data.put("type", docType);
	    			data.put("book", book);
	    			data.put("page", page);
	    			data.put("year", year);
	    			data.put("date", instrDate);
	    				
					if (isInstrumentSaved(docNo, null, data)){
	                	details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
	            } 
				else 
	            {      
					details = details.replaceAll("(?is)<a[^>]*>\\s*View Image\\s*</a>", "");
					String resultForCross = details;
					details = details.replaceAll("(?is)</?a[^>]*>", "");
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = docNo + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
	                
	                Pattern crossRefLinkPattern = Pattern.compile("(?ism)<a[^>]*?href=[\\\"|'](.*?)[\\\"|']>");
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
				if (sAction.indexOf("REALSummary.aspx") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} else if (sAction.indexOf("navigation") != -1) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				
				break;
			case ID_SAVE_TO_TSD :
				if (sAction.contains(DETAILS_ADDRESS)) {		//fake document
					ParsedResponse pr = Response.getParsedResponse();
					String html = pr.getResponse();
					if (html.contains("checkbox")) {
						html = html.replaceFirst("(?is)<td[^>]+>\\s*<input[^>]+>\\s*</td>\\s*<td[^>]+>\\s*<b>RO</b>\\s*</td>", "");
					}
			        String instrNo = pr.getDocument().getInstno();
			        if (StringUtils.isEmpty(instrNo)) {
			        	instrNo = pr.getDocument().getBook() + "_" + pr.getDocument().getPage();
			        }
		        	
		        	// set file name
		            msSaveToTSDFileName = instrNo + ".html";     
		            pr.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
		       
		            // save to TSD
		            msSaveToTSDResponce = html + CreateFileAlreadyInTSD(true);            
		            parser.Parse(pr, html, Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
				} else {
					downloadingForSave = true;
					ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				}
				break;

			
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		Search search = this.getSearch();
		searchId = search.getID();
		
		/**
		 * We need to find what was the original search module
		 * in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if(objectModuleSource != null) {
			if(objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			} 
		} else {
			objectModuleSource = search.getAdditionalInfo(
					this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}
		
		try {
			table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(new TagNameFilter("div")).extractAllNodesThatMatch(new HasAttributeFilter("id", "ResultsPanel"), true);
			
			TableTag mainTable = null;
			if (nodeList != null){
				mainTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
			}
			
			String linksTable = "";
			NodeList aNavList = nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true).extractAllNodesThatMatch(new HasAttributeFilter("id"), true);
			if (aNavList != null){
				linksTable = aNavList.toHtml();
			}
			
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>");
			
			String header1 = "";
			if (mainTable != null){
				TableRow[] rows = mainTable.getRows();
				
				boolean isBookPageSearch = false;
				if (response.getQuerry().contains("SEARCH_BOOK") || response.getQuerry().contains("SEARCH_INSTRUMENT_NUMBER")
						|| response.getQuerry().contains("RLBP") || table.toLowerCase().contains("search by book")){
					isBookPageSearch = true;
				}
				for(int i = 0; i < rows.length; i++ ) {
					TableRow row = rows[i];
					if (row.getHeaderCount() > 5){
						newTable.append(row.toHtml().replaceAll("(?is)(<TR[^>]*>)", "$1 <TH width=\"5%\" align=\"justify\" bgcolor=\"#003090\">" + SELECT_ALL_CHECKBOXES + "</TH><TH>View</TH>"));
					}
					if(row.getColumnCount() > 5) {
						
						TableColumn[] cols = row.getColumns();
						
						NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
						if (aList.size() == 0) {
							continue;
						} else {
							
							String lnk = partLink + ((LinkTag) aList.elementAt(0)).getLink().replaceAll("\\s", "");
							String nameFromLink = ((LinkTag) aList.elementAt(0)).getChildrenHTML().trim();
							String documentNumber = cols[8].toPlainTextString().replaceAll("&nbsp;", " ").trim();
							String serverDocType = cols[14].toPlainTextString().replaceAll("&nbsp;", " ").trim();
							String tmpBook = cols[10].toPlainTextString().replaceAll("&nbsp;", " ").trim();
							String tmpPage = cols[12].toPlainTextString().replaceAll("&nbsp;", " ").trim();
								
							if (isBookPageSearch){
								documentNumber = nameFromLink;
								tmpBook = cols[2].toPlainTextString().replaceAll("&nbsp;", " ").trim();
								tmpPage = cols[4].toPlainTextString().replaceAll("&nbsp;", " ").trim();
								serverDocType = cols[12].toPlainTextString().replaceAll("&nbsp;", " ").trim();
							}
	
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
							if (isBookPageSearch){
								tmpPartyGtor = cols[8].toPlainTextString().replaceAll("&nbsp;", " ").trim();
								tmpPartyGtee = cols[10].toPlainTextString().replaceAll("&nbsp;", " ").trim();
							} else{
								if (cols[2].toPlainTextString().contains("GTOR")){
									tmpPartyGtor = nameFromLink;
									tmpPartyGtee = cols[4].toPlainTextString().replaceAll("&nbsp;", " ").trim();
								} else if (cols[2].toPlainTextString().contains("GTEE")){
									tmpPartyGtee = nameFromLink;
									tmpPartyGtor = cols[4].toPlainTextString().replaceAll("&nbsp;", " ").trim();
								}
							}
								
							ResultMap resultMap = new ResultMap();
								
							String link = CreatePartialLink(TSConnectionURL.idGET) + lnk;
							if(document == null) {	//first time we find this document
									
								String recDate = cols[6].toPlainTextString().replaceAll("&nbsp;", " ").trim();
								
								String legalDescription = "";
									
								if (isBookPageSearch){
									recDate = cols[6].toPlainTextString().replaceAll("&nbsp;", " ").trim();
									legalDescription = cols[14].toPlainTextString();
								} else{
									legalDescription = cols[16].toPlainTextString();
								}
								resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDescription);
									
								int count = 1;
									
								String rowHtml =  row.toHtml().replaceAll("(?is)</?a[^>]*>", "");
								rowHtml = rowHtml.replaceFirst("(?is)(<TR[^>]*>)\\s*(<TD[^>]*>)","$1\n<TD><a href=\"" + link + "\">View</a></TD>$2");
									
								tmpPartyGtor = StringEscapeUtils.unescapeHtml(tmpPartyGtor);
								tmpPartyGtee = StringEscapeUtils.unescapeHtml(tmpPartyGtee);
								resultMap.put("tmpPartyGtor", tmpPartyGtor);
								resultMap.put("tmpPartyGtee", tmpPartyGtee);
								resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"RO");
								resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpPartyGtor);
								resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), tmpPartyGtee);
								resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
								//resultMap.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), documentNumber);
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), tmpBook);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), tmpPage);
								resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
								resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
								try {
									ro.cst.tsearch.servers.functions.MOPlatteRO.parseNameInterMOPlatteRO(resultMap, searchId);
									ro.cst.tsearch.servers.functions.MOPlatteRO.parseLegalMOPlatteRO(resultMap, searchId);
								} catch (Exception e){
									e.printStackTrace();
								}
				    			resultMap.removeTempDef();
				    				
				    			@SuppressWarnings("unchecked")
				    			Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) resultMap.get("PropertyIdentificationSet");
				    			if (pisVector != null && !pisVector.isEmpty()){
				    				for (PropertyIdentificationSet everyPis : pisVector){
				    					currentResponse.addPropertyIdentificationSet(everyPis);
				    				}
				    			}
				    				
				    			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>" + 
																	row.toHtml() + "</table>");
																	
				    			Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
				    			document = (RegisterDocumentI) bridge.importData();
									
				    			currentResponse.setDocument(document);
				    			String checkBox = "checked";
				    			HashMap<String, String> data = new HashMap<String, String>();
				    			data.put("type", serverDocType);
				    			data.put("book", tmpBook);
				    			data.put("page", tmpPage);
									
				    			if (isInstrumentSaved(documentNumber, document, data) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))){
				    				checkBox = "saved";
				    				LinkInPage linkInPage = new LinkInPage(CreatePartialLink(TSConnectionURL.idGET) + lnk,
						    					                               CreatePartialLink(TSConnectionURL.idGET) + lnk);
				    				currentResponse.setPageLink(linkInPage);
						    	} else{
						    		numberOfUncheckedElements++;
						    		LinkInPage linkInPage = new LinkInPage(
						    				CreatePartialLink(TSConnectionURL.idGET) + lnk, 
						    				CreatePartialLink(TSConnectionURL.idGET) + lnk, 
						    				TSServer.REQUEST_SAVE_TO_TSD);
						    		checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\"" + link + "\">";
						    		currentResponse.setPageLink(linkInPage);
				            			
						    		/**
						    		 * Save module in key in additional info. The key is instrument number that should be always available. 
						    		 */
						    		String keyForSavingModules = this.getKeyForSavingInIntermediary(documentNumber);
						    		search.setAdditionalInfo(keyForSavingModules, moduleSource);
						    	}
				    			rowHtml = rowHtml.replaceAll("(?is)(<TR[^>]*>)", 
												"$1<TD  align=\"justify\" width=\"5%\" nowrap><font face=\"Verdana\" size=\"1\" rowspan=" + count + ">" + checkBox + "</TD>");
				    			currentResponse.setOnlyResponse(rowHtml);
				    			newTable.append(currentResponse.getResponse());
									
				    			count++;
				    			intermediaryResponse.add(currentResponse);
								
							} else{
									
								tmpPartyGtor = StringEscapeUtils.unescapeHtml(tmpPartyGtor);
								tmpPartyGtee = StringEscapeUtils.unescapeHtml(tmpPartyGtee);
								resultMap.put("tmpPartyGtor", tmpPartyGtor);
								resultMap.put("tmpPartyGtee", tmpPartyGtee);
								resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"RO");
								resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpPartyGtor);
								resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), tmpPartyGtee);
								resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
								resultMap.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), documentNumber);
								resultMap.put(SaleDataSetKey.BOOK.getKeyName(), tmpBook);
								resultMap.put(SaleDataSetKey.PAGE.getKeyName(), tmpPage);
				    				
								try {
									ro.cst.tsearch.servers.functions.MOPlatteRO.parseNameInterMOPlatteRO(resultMap, searchId);
									ro.cst.tsearch.servers.functions.MOPlatteRO.parseLegalMOPlatteRO(resultMap, searchId);
								} catch (Exception e) {
									e.printStackTrace();
								}
								@SuppressWarnings("unchecked")
								Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) resultMap.get("PropertyIdentificationSet");
								if (pisVector != null && !pisVector.isEmpty()){
									for (PropertyIdentificationSet everyPis : pisVector){
										currentResponse.addPropertyIdentificationSet(everyPis);
									}
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
				header1 = rows[1].toHtml();
			}
			Form form = new SimpleHtmlParser(table).getForm("FORM_CRITERIA");
			String action = form.action;
			
			Map<String, String> params = form.getParams();
			params.remove("__EVENTTARGET");
			params.remove("ResetButton");
			params.remove("SortOrder");
			params.remove("SearchButton");
			int seq = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			
			String nextLink = "";
			linksTable = StringEscapeUtils.unescapeHtml(linksTable).replaceAll("(?is)href=[\\\"|']?javascript:__doPostBack\\('([^']+)',\\s*'[^']*'\\)[\\\"|']", 
									"href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + partLink + action + "?navigation=$1&seq=" + seq + "\"");
			
			linksTable = linksTable.replaceAll("(?is)</a>", "</a>&nbsp;&nbsp;&nbsp;&nbsp;");
			
			Matcher matcher = FOR_NEXT_PAT.matcher(linksTable);
			if (matcher.find()){
				nextLink = matcher.group(1);
				nextLink = nextLink.replaceAll("(?is)\\bstyle=\\s*\\\"[^\\\"]*\\\"", "");
				nextLink = nextLink.replaceAll("(?is)\\bid=\\s*\\\"[^\\\"]+\\\"", "");
				nextLink = nextLink.replaceAll("(?is)\\s{2,}", " ");
				nextLink = nextLink.replaceAll("(?is)&amp;", "&");
				
				boolean isBookPageAutomaticSearch = getSearch().getSearchType() == Search.AUTOMATIC_SEARCH &&
													moduleSource.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX &&
													moduleSource.getFunctionCount() > 2 && 
													org.apache.commons.lang.StringUtils.isNotEmpty(moduleSource.getParamValue(1)) && 
													org.apache.commons.lang.StringUtils.isNotEmpty(moduleSource.getParamValue(2));
				
				if (!isBookPageAutomaticSearch ) {
					response.getParsedResponse().setNextLink("<a href=\"" + nextLink + "\">Next</a>");
				}
			}
						
			header1 = header1.replaceAll("(?is)(<TR[^>]*>)", "$1 <TH width=\"5%\" align=\"justify\">" + SELECT_ALL_CHECKBOXES + "</TH><TH>View</TH>");
				
			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
								 + "<br>" + linksTable
					+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
			response.getParsedResponse().setFooter("</table>" + linksTable + "<br>" +  CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
		
		newTable.append("</table>");
		outputTable.append(newTable);
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	/**
	 * @return
	 */
	public String prepareLink() {
		
		if (org.apache.commons.lang.StringUtils.isEmpty(partLink)){
			String baseLink = getBaseLink().replaceAll("(?is)https?://", "");
			
			try {
				partLink = baseLink.substring(baseLink.indexOf("/"), baseLink.lastIndexOf("/") + 1);
			} catch (Exception e) {
				partLink = "/iRecordWeb2.0/";
				logger.error("Partial link cannot be extracted from baseLink\n " + e);
			}
		}
		return partLink;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.MOPlatteRO.parseAndFillResultMap(response, detailsHtml, map, searchId);
		return null;
	}
	
	protected String getDetails(String response, ServerResponse Response){
		
		// if from memory - use it as is
		if(!response.toLowerCase().contains("<html")){
			return response;
		}
		
		String details = "";
		String docNo = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(new TagNameFilter("table"));
			docNo = HtmlParser3.getValueFromNextCell(mainList, "Document No.", "", false).trim();
			if (mainList != null && mainList.size() > 0){
				
				for (int i = 0; i < mainList.size(); i++){
					if (mainList.elementAt(i).toHtml().contains("Document recording information")){
						TableTag table = (TableTag) mainList.elementAt(i); 
						table.getChildren().remove(1);
						table.removeAttribute("width");
						table.setAttribute("id", "detailsPage");
						table.setAttribute("align", "center");
						details = table.toHtml();
						break;
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		details = details.replaceAll("(?is)(HREF\\s*=\\s*[\\\"|']?)", "$1" + CreatePartialLink(TSConnectionURL.idGET) + partLink);
		
		Matcher imageMat = IMG_LINK_PAT.matcher(StringEscapeUtils.unescapeHtml(response));
		if (imageMat.find()){
			String src = imageMat.group(1);
			src = StringUtils.prepareStringForHTML(src);
			String imageLink = CreatePartialLink(TSConnectionURL.idGET) + partLink + src;
			if (response.indexOf("value=\"Click Here To View Document\"") != -1){
				Response.getParsedResponse().addImageLink(new ImageLinkInPage(imageLink, docNo + ".pdf"));
					
				if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ){
					details += "<br><br><br>&nbsp;&nbsp;&nbsp;"
								+ "<a id=\"imageLink\" href=\"" + imageLink + "\" target=\"_blank\" align=\"center\">View Image</a><br><br>";
				
				}
			}
		}		
		return details;
	}
		
	@Override
	protected void setCertificationDate() {

//		try {
//			 
//		String countyId = getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY);
//		
//		if(CertificationDateManager.isCertificationDateInCache(countyId)) {
//			return;
//		}
//		
//		String countyName = County.getCounty(Integer.parseInt(countyId)).getName().toLowerCase();
//		
//        logger.debug("Intru pe get Certification Date - " + countyName);
//
//        String html = "";
//        String getBaseLink = getBaseLink();
//    	String link = getBaseLink.substring(0, getBaseLink.indexOf("Login.aspx")) + "REALSearchByName.aspx";
//        HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
//        try{
//        	html = site.process(new HTTPRequest(link)).getResponseAsString();
//        } catch(RuntimeException e){
//        	e.printStackTrace();
//        } finally {
//        	HttpManager.releaseSite(site);
//    	}  
//        	
//        if(StringUtils.isNotEmpty(html)) {
//        	try {
//				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(html, null);
//				NodeList mainList = htmlParser.parse(null);
//
//				String date = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(mainList, "Certification Date"), "", true).trim();
//				if (date.contains("Certification Date:")){
//					Pattern certDatePat = Pattern.compile("(?is)\\b([\\d/]+)\\b");
//					Matcher mat = certDatePat.matcher(date);
//					if (mat.find()){
//						date = mat.group(1).trim();
//						CertificationDateManager.cacheCertificationDate(countyId, date);
//					}
//				}
//				
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//            	
//        }
//		 
//        } catch (Exception e) {
//            logger.error(e.getMessage());
//        }
	}
	
	
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {

			String link = image.getLink();
			
			if  (link.contains(IMAGE_ADDRESS)) {
				RegisterDocument docum = getDocumentFromLink(link);
				return getImage(docum);
			}

			byte[] imageBytes = null;

	    	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				imageBytes = ((ro.cst.tsearch.connection.http2.MOPlatteRO)site).getImage(link); 
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
			
		return dres;
	}
	
	@Override
	protected DownloadImageResult searchForImage(DocumentI doc) throws ServerResponseException {
			
		return MOPlatteImageRetriever.getSingletonObject().retrieveImage(dataSite, doc, searchId);
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();

		TSServerInfoModule module = null;
		if(StringUtils.isNotEmpty(instrumentNumber)) {
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			filterCriteria.put("InstrumentNumber", instrumentNumber);
			module.forceValue(0, instrumentNumber);
			module.forceValue(7, "Search");
			GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
			module.getFilterList().clear();
			module.addFilter(filter);
		} else {
			String book = restoreDocumentDataI.getBook();
			String page = restoreDocumentDataI.getPage();
			
			if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
				HashMap<String, String> filterCriteria = new HashMap<String, String>();
				module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
				filterCriteria.put("Book", book);
				filterCriteria.put("Page", page);
				module.forceValue(1, book);
				module.forceValue(2, page);
				module.forceValue(7, "Search");
				GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
				module.getFilterList().clear();
				module.addFilter(filter);
			}
		}
		return module;
	}
	
	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 1) {
			String usedName = module.getFunction(0).getParamValue();
			if(StringUtils.isEmpty(usedName)) {
				return null;
			}
			String firstMiddName = module.getFunction(1).getParamValue();
			if(StringUtils.isNotEmpty(firstMiddName)) {
				usedName += " " + firstMiddName;
			}
			String[] names = null;
			if(NameUtils.isCompany(usedName)) {
				names = new String[]{"", "", usedName, "", "", ""};
			} else {
				names = StringFormats.parseNameNashville(usedName, true);
			}
			name.setLastName(names[2]);
			name.setFirstName(names[0]);
			name.setMiddleName(names[1]);
			return name;
		}
		return null;
	}
	
	private static final String NAME_TYPE = 
		"<select name=\"NameType\">" +
			"<option value=\"RBTN_GRANTOR\">Grantor</option>" + 
			"<option value=\"RBTN_GRANTEE\">Grantee</option>" +
			"<option value=\"RBTN_ALL\" selected>Both</option>" +
		"</select>";
	
	/**
     * Looks for the a document having the same instrumentNo
     * @param instrumentNo
     * @param documentToCheck if not null will only compare its instrument with saved documents
     * @param data
     * @return
     */
    public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data){
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
    		return false;
    	}
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null) {
    				return true;
    			}
    			if(documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck).size() > 0) {
    				return true;
    			}
    		} else {
    			
    			InstrumentI instrOrig = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
	    			instrOrig.setBook(data.get("book"));
	    			instrOrig.setPage(data.get("page"));
	    			instrOrig.setDocType(DocumentTypes.getDocumentCategory(data.get("type"), searchId));
	    			instrOrig.setDocSubType(DocumentTypes.getDocumentSubcategory(data.get("type"), searchId));
	    		}
	    		
	    		
    			//used for not saving a document already saved from OR, but with instrNo in other form
    			if (instrumentNo.matches("(?is)\\A\\d+.*") && instrumentNo.length() > 4){
    				instrumentNo = instrumentNo.substring(0, 4) 
    					+ org.apache.commons.lang.StringUtils.stripStart(instrumentNo.substring(4, instrumentNo.length()), "0");
    			}
    			
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		//instr.setDocno(data.get("docno"));
	    		}
	    		
	    		try {
	    			instr.setYear(Integer.parseInt(data.get("year")));
	    			instrOrig.setYear(Integer.parseInt(data.get("year")));
	    		} catch (Exception e) {}
	    		
	    		if(documentManager.getDocument(instr) != null || documentManager.getDocument(instrOrig) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    		
	    			if(almostLike.size() > 0) {
	    				return true;
	    			}
	    			
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }
    
    public DownloadImageResult getImage(RegisterDocumentI docum) {
		DownloadImageResult res = null;
		
		Image image = new Image();
		image.setType(IType.TIFF);
		image.setExtension("tiff");
		image.setContentType("image/tiff");
			
		String imageDirectory = mSearch.getImageDirectory();
		FileUtils.CreateOutputDir(imageDirectory);
		String fileName = docum.getId() + "." + image.getExtension();
	    String path = imageDirectory + File.separator + fileName;
	        	
	    image.setPath(path);
	    image.setFileName(fileName);
	    image.setSaved(true);
			
	    docum.setImage(image);
	       	
	    try {
	    	res = searchForImage(docum);
		} catch (ServerResponseException e) {
			e.printStackTrace();
			return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0],"" );
		}
		
		return res;
	 }
    
    @Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
        if (module.getModuleIdx()==TSServerInfo.MODULE_IDX38) {	//DT Image Search
        	
        	logSearchBy(module);
        	
        	String book = module.getParamValue(0);
        	String page = module.getParamValue(1);
        	String docno = module.getParamValue(2);
        	String year = module.getParamValue(3);
        	String type = module.getParamValue(4);
        	
        	InstrumentI instr = new Instrument();
        	instr.setBook(book);
    		instr.setPage(page);
    		instr.setInstno(docno);
    		int yearInt = -1;
    		try {
    			yearInt = Integer.parseInt(year);
    		} catch (NumberFormatException nfe) {}
    		instr.setYear(yearInt);
    		instr.setDocType(type);
    		
    		RegisterDocumentI docum = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr));
    		docum.setInstrument(instr);
    		
    		String county = "County of Platte";
    		NameI name = new Name();	
    		name.setLastName(county);
    		name.setCompany(true);
    		PartyI grantor = new Party(PType.GRANTOR);
    		grantor.add(name);
    		docum.setGrantor(grantor);
    		
    		docum.setDataSource(dataSite.getSiteTypeAbrev());
    		docum.setSearchType(SearchType.IN);
    		docum.setServerDocType(type);
    		docum.setSiteId(getServerID());
        	
    		
    		docum = DocumentsManager.createRegisterDocument(getSearch().getID(), type, (RegisterDocument) docum, null);
    		
        	DownloadImageResult result = getImage(docum);
        	
        	if (result!=null && result.getStatus()==DownloadImageResult.Status.OK) {
        		
        		String link = "?book=" + book + "&page=" + page + "&instno=" + docno + "&year=" + year + "&type=" + type;
        		
        		String imageLink = CreatePartialLink(TSConnectionURL.idPOST) + IMAGE_ADDRESS + link;
        		
        		mSearch.setAdditionalInfo(IMAGE_ADDRESS + link, docum.getImage().getPath());
        		docum.getImage().getLinks().add(imageLink);
        		
        		ServerResponse response = new ServerResponse();
				
				String html = HTML_RESPONSE;
				html = html.replace("@@TYPE@@", type==null?"":type);
				html = html.replace("@@RECORDED@@", yearInt==-1?"":year);
				html = html.replace("@@BOOK@@", book==null?"":book);
				html = html.replace("@@PAGE@@", page==null?"":page);
				html = html.replace("@@INSTRUMENT@@", docno==null?"":docno);
				html = html.replace("@@LINK@@", docno==null?"":imageLink);
				
				String detailsLink = CreatePartialLink(TSConnectionURL.idPOST) + DETAILS_ADDRESS + link;
				
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setDocument(docum);
				
				String instrumentno = "";
				if (!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)) {
					instrumentno = book + "_" + page;
				} else if (!StringUtils.isEmpty(docno)) {
					instrumentno = docno;
				}
				
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("book", book);
    			data.put("page", page);
    			data.put("instrno", docno);
    			data.put("year", year);
    			data.put("type", type);
    			
				String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") +
					"<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">" + 
					"<tr bgcolor=\"#cccccc\"><th width=\"1%\" align=\"center\">" + SELECT_ALL_CHECKBOXES + 
					"</th> <th width=\"1%\">Type</th><th width=\"98%\" align=\"left\">Document</th></tr>";
    			String footer = "</table></table>";
    			
    			String checkbox = "";
				if (isInstrumentSaved(instrumentno, docum, data)) {
        			checkbox = "saved";
        			footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_MODULE38, -1);
        		} else {
        			checkbox = "<input type='checkbox' name='docLink' value='" + detailsLink + "'>";
        			LinkInPage linkInPage = new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD);
					currentResponse.setPageLink(linkInPage);
					footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_MODULE38, 1);
					if(mSearch.getInMemoryDoc(detailsLink)==null){
        				mSearch.addInMemoryDoc(detailsLink, currentResponse);
        			}
        		}
				
				html = "<tr><td valign=\"center\" align=\"center\">" + checkbox + "</td><td align=\"center\"><b>RO</b></td><td>" + html + "</td></tr>";
				
				currentResponse.setResponse(html);
				currentResponse.setUseDocumentForSearchLogRow(true);
				
				Vector<ParsedResponse> rows = new Vector<ParsedResponse>();
				rows.add(currentResponse);
				
				ParsedResponse parsedResponse = response.getParsedResponse();
				parsedResponse.setResultRows(rows);
				parsedResponse.setHeader(header);
				parsedResponse.setFooter(footer);
				parsedResponse.setParentSite(isParentSite());
				
				response.setParentSiteSearch(isParentSite());
				
				return response;
        		
        	} else {
        		ServerResponse response = new ServerResponse();
                response.getParsedResponse().setError("Image(searchId=" + searchId + " )book=" + book + 
                	"page=" + page + "inst=" + docno + " was not found on DataTree");
                response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
                //logInitialResponse(response);
                return response;
        	}
        }
		return SearchBy(true, module, sd);
    }
    
    public RegisterDocument getDocumentFromLink(String link) {
    	
    	String book = StringUtils.extractParameter(link, "book=([^&?]*)");
    	String page = StringUtils.extractParameter(link, "page=([^&?]*)");
    	String docno = StringUtils.extractParameter(link, "instno=([^&?]*)");
    	String year = StringUtils.extractParameter(link, "year=([^&?]*)");
    	String type = StringUtils.extractParameter(link, "type=([^&?]*)");
    	
    	InstrumentI instr = new Instrument();
    	instr.setBook(book);
		instr.setPage(page);
		instr.setInstno(docno);
		int yearInt = -1;
		try {
			yearInt = Integer.parseInt(year);
		} catch (NumberFormatException nfe) {}
		instr.setYear(yearInt);
		instr.setDocType(type);
		
		RegisterDocument docum = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr));
		docum.setInstrument(instr);
		
		return docum;
    } 
    
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded)	throws ServerResponseException {
       if (vsRequest.contains(IMAGE_ADDRESS)) {
    	   vsRequest = vsRequest.replaceFirst("(?is).*Link=", "");
    	   String imageName = (String)mSearch.getAdditionalInfo(vsRequest);
		   if(imageName!=null) {
			   writeImageToClient(imageName, "image/tiff");
			   return null;
		   } else {
			   RegisterDocument docum = getDocumentFromLink(vsRequest);
			   DownloadImageResult imageResult = getImage(docum);
			   if (imageResult!=null && imageResult.getStatus()==DownloadImageResult.Status.OK) {
				   ServerResponse resp = new ServerResponse();
				   resp.setImageResult(imageResult);
				   return resp;
			   }
		   } 
		   ServerResponse response = new ServerResponse();
		   response.getParsedResponse().setError(URLConnectionReader.ERROR_MESSAGE_DOWNLOAD_IMAGE);
           response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
           return response;
	   } else {
    	   return super.GetLink(vsRequest, vbEncoded);
       }
    }
   
}

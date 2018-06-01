package ro.cst.tsearch.search.iterator.legal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.SubdivisionFilter;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.data.PlatBookPage;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.types.GenericDASLTS;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class LegalDescriptionIterator extends GenericRuntimeIterator<LegalStruct> implements LegalDescriptionIteratorI {
	
	private static final Pattern LETTERS_NUMBERS_PATTERN = Pattern.compile("([a-zA-Z]+)-?(\\d+)");
	
	private static final long serialVersionUID = 8989586891817117069L;
	
	protected Set<LegalStruct> legalStruct;
	protected Map<String, Set<String>> legalSources;
	
	private final boolean lookUpWasWithNames;
	
	protected int initialSiteDocuments = -1;
	protected boolean firstTime = true;
	private boolean loadedFromSearchPage = false;
	
	private final boolean forcelegalfromLastMortgageOrLastTransfer;
	private boolean enableSubdividedLegal = true;
	private boolean enableTownshipLegal = true;
	private boolean enableSubdivision = true;
	private boolean treatTractAsArb = false;
	private boolean loadFromSearchPage = true;
	private boolean loadFromSearchPageIfNoLookup = false;
	private boolean forceSubdividedIterationWithoutBlock = true;
	private String additionalInfoKey = AdditionalInfoKeys.TS_LOOK_UP_DATA;
	private String subdivisionSetKey = AdditionalInfoKeys.SUBDIVISION_NAME_SET;
	private County currentCounty;
	private String[] roDoctypesToLoad = null;
	private String checkAlreadyFilledKeyWithDocuments = null;
	private boolean useAddictionInsteadOfSubdivision = false;
	private boolean checkIfDocumentExists = false;
	private String[] platTypes = null;
	
	
	/**
	 * This will store all subdivision names found for a smart multiple subdivision name filter ;)
	 */
	private Set<String> allSubdivisionNames = null;
	private RejectPlatBookPageFilter rejectPlatBookPageFilter = null;
	
	
	public LegalDescriptionIterator(long searchId, boolean lookUpWasWithNames, boolean legalFromLastTransferOnly, DataSite dataSite) {
		super(searchId);
		this.lookUpWasWithNames = lookUpWasWithNames;
		this.forcelegalfromLastMortgageOrLastTransfer = legalFromLastTransferOnly;
		currentCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
		allSubdivisionNames = new HashSet<String>();
		this.dataSite = dataSite;
	}
	
	
	
	@SuppressWarnings("unchecked")
	public List<LegalStruct> createDerrivations(){
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		DocumentsManagerI m = global.getDocManager();
		
		if("true".equalsIgnoreCase(global.getSa().getAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND))) {
			return new ArrayList<LegalStruct>();
		}
		rejectPlatBookPageFilter = new RejectPlatBookPageFilter(searchId);
		legalStruct = (Set<LegalStruct>)global.getAdditionalInfo(additionalInfoKey);
		
		String originalLot = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
		String originalBlock = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
		String originalUnit = global.getSa().getAtribute(SearchAttributes.P_STREETUNIT);
		String originalTract = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_TRACT);
		Set<String> temporarySubdivisionsForCondoSearch = new HashSet<String>();
		
		if(forcelegalfromLastMortgageOrLastTransfer){
			if(legalStruct==null){
				legalStruct = new HashSet<LegalStruct>();
				legalSources = new HashMap<String, Set<String>>();
			}
			
			if(!hasPlatBookPlatPageLegal(legalStruct)){
				try{
					m.getAccess();
					legalStruct.addAll(getDataStructForCurrentOwner(m,global));
					legalStruct = keepOnlyGoodLegals(legalStruct);
					return new ArrayList<LegalStruct>(legalStruct);
				}finally{
					m.releaseAccess();
				}
			}
			
			return new ArrayList<LegalStruct>();
		}
		
		Set<PlatBookPage> platBookPageFromUser = new HashSet<PlatBookPage>();
		
		if(legalStruct==null){
			legalStruct = new HashSet<LegalStruct>();
			legalSources = new HashMap<String, Set<String>>();
			try{
				m.getAccess();
				
				initialSiteDocuments = m.getDocumentsWithDataSource(false, dataSite.getSiteTypeAbrev()).size();	
				
				
				List<DocumentI> listRodocs = loadLegalFromRoDocs(global, m);
				
				legalStruct = keepOnlyGoodLegals(legalStruct);
				
				if(isLoadFromSearchPage() || (legalStruct.isEmpty() && isLoadFromSearchPageIfNoLookup()) ) {
				
					loadedFromSearchPage = true;
					
					if(isEnableSubdividedLegal()){
						
						//Bug 6738
						if(legalStruct.size()==1 && StringUtils.isNotEmpty(originalLot) && StringUtils.isNotEmpty(originalBlock)){
							for(LegalStruct dataStruct1:legalStruct){
								if( (StringUtils.isEmpty(dataStruct1.getLot())||StringUtils.isEmpty(dataStruct1.getBlock()))
										&& StringUtils.isNotEmpty(dataStruct1.getPlatBook()) && StringUtils.isNotEmpty(dataStruct1.getPlatPage())
										){
									for (LotInterval lotInterval: LotMatchAlgorithm.prepareLotInterval(originalLot)) {
										List<String> allLots = lotInterval.getLotList();
										if(allLots.size()>0){
											for(String l:allLots){
												LegalStruct dataStruct2 = new LegalStruct(false);
												dataStruct2.setPlatBook(dataStruct1.getPlatBook());
												dataStruct2.setPlatPage(dataStruct1.getPlatPage());
												dataStruct2.setLot(l);
												dataStruct2.setBlock(originalBlock);
												legalStruct.add(dataStruct2);
											}
										}	
									}
									legalStruct = keepOnlyGoodLegals(legalStruct);	
								}
							}
						}
						
						String platBook = cleanPlatBook(getSearchAttribute(SearchAttributes.LD_BOOKNO));
						String platPage = getSearchAttribute(SearchAttributes.LD_PAGENO);
						
						if(!StringUtils.isEmpty(platBook) && !StringUtils.isEmpty(platPage)){
							
							for (LotInterval lotInterval: LotMatchAlgorithm.prepareLotInterval(originalLot)) {
								List<String> allLots = lotInterval.getLotList();
								if(allLots.size()>0){
									for(String l:allLots){
										
										LegalStruct legalStruct1 = new LegalStruct(false);
										
										legalStruct1.setPlatBook(platBook);
										legalStruct1.setPlatPage(platPage);
										legalStruct1.setLot(l);
										legalStruct1.setBlock(originalBlock);
										
										if(!incompleteData(legalStruct1)) {
											String keyForLegal = getKeyForLegal(legalStruct1.getPlatBook(), legalStruct1.getPlatPage());
											Set<String> foundSources = getLegalSourcesNotNull().get(keyForLegal);
											if(foundSources == null) {
												foundSources = new HashSet<String>();
												getLegalSourcesNotNull().put(keyForLegal, foundSources);
											}
											foundSources.add("Search Page");
											legalStruct.add(legalStruct1);
											
											if(StringUtils.isNotEmpty(legalStruct1.getLot())&&StringUtils.isNotEmpty(legalStruct1.getBlock())){
												forceSubdividedIterationWithoutBlock(legalStruct1);
											} else if(isTreatTractAsArb()  && StringUtils.isNotEmpty(originalTract) 
													&& (StringUtils.isEmpty(legalStruct1.getLot()) || StringUtils.isEmpty(legalStruct1.getBlock()))){
												addTractAsArbLegalStruct(originalTract, legalStruct1.getPlatBook(), legalStruct1.getPlatPage(), 
																getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME));
											}
										} else if(search.getSa().isCondo() && StringUtils.isNotEmpty(originalUnit)) {
											String lotOrUnit = originalUnit;
											String blockOrBuilding = null;
											Matcher matcher = LETTERS_NUMBERS_PATTERN.matcher(lotOrUnit);
											if(matcher.matches()) {
												lotOrUnit = matcher.group(2);
												blockOrBuilding = matcher.group(1);
											} else {
												blockOrBuilding = "";
											}
											
											legalStruct1.setLot(lotOrUnit);
											legalStruct1.setBlock(blockOrBuilding);
											
											if(!incompleteData(legalStruct1)) {
												String keyForLegal = getKeyForLegal(legalStruct1.getPlatBook(), legalStruct1.getPlatPage());
												Set<String> foundSources = getLegalSourcesNotNull().get(keyForLegal);
												if(foundSources == null) {
													foundSources = new HashSet<String>();
													getLegalSourcesNotNull().put(keyForLegal, foundSources);
												}
												foundSources.add("Search Page");
												legalStruct.add(legalStruct1);
												
												if(StringUtils.isNotEmpty(legalStruct1.getLot())&&StringUtils.isNotEmpty(legalStruct1.getBlock())){
													forceSubdividedIterationWithoutBlock(legalStruct1);
												} else if(isTreatTractAsArb()  && StringUtils.isNotEmpty(originalTract) 
														&& (StringUtils.isEmpty(legalStruct1.getLot()) || StringUtils.isEmpty(legalStruct1.getBlock()))){
													addTractAsArbLegalStruct(originalTract, legalStruct1.getPlatBook(), legalStruct1.getPlatPage(), legalStruct1.getAddition());
												}
											}
											
										}
										
									}
								}	
							}
							
						}
					}
					
					/**
					 * -  I) Avand in vedere ca TS este foarte prost indexat va trebuii sa imbunatatim felul cum facem cautarea dupa subdivision name in felul urmator:
					 * imediat dupa look-up (care a fost facut fie cu cautare book/page/inst fie cu nume)
					 * 1) se cauta cu subdivision name,lot, block de la look-up daca am si lot sau block
					 * 2) se cauta cu subdivision name din order lot si block  din search page  daca am si lot sau block
					 * 3) se cauta cu subdivision name,lot, block din search page daca am si lot sau block
					 */
					//if(!hasPlatBookPlatPageLegal(legalStruct)){
					{	
						//1) se cauta cu subdivision name,lot, block de la look-up daca am si lot sau block
						for( DocumentI reg: listRodocs){
							if(reg.isOneOf("MORTGAGE","TRANSFER")){
								for (PropertyI prop: reg.getProperties()){
									if(prop.hasLegal()){
										LegalI legal = prop.getLegal();
										String addiction = "";
										if(legal.hasSubdividedLegal() && isEnableSubdivision()){
											SubdivisionI subdiv = legal.getSubdivision();
											String block = subdiv.getBlock();
											String lot = subdiv.getLot();
											addiction =subdiv.getName();
											if(!StringUtils.isEmpty(addiction)){
												temporarySubdivisionsForCondoSearch.add(addiction);	
												if(StringUtils.isNotEmpty(lot)||StringUtils.isNotEmpty(block)||
													StringUtils.isNotEmpty(originalBlock)||StringUtils.isNotEmpty(originalLot)){
												
													LegalStruct legalStruct1 = new LegalStruct(false);
													legalStruct1.setLot(StringUtils.isEmpty(lot)?"":lot);
													legalStruct1.setBlock(StringUtils.isEmpty(block)?"":block);
													legalStruct1.setAddition(addiction);
													legalStruct.add(legalStruct1);
													
													if(StringUtils.isNotEmpty(legalStruct1.getLot())&&StringUtils.isNotEmpty(legalStruct1.getBlock())){
														forceSubdividedIterationWithoutBlock(legalStruct1);
													}
													
													saveSubdivisionName(addiction);		//store this for the filter ;)
												}  else {
													if(isTreatTractAsArb() && StringUtils.isNotEmpty(originalTract)){
														addTractAsArbLegalStruct(originalTract, subdiv.getPlatBook(), subdiv.getPlatPage(), addiction);
														
														saveSubdivisionName(addiction);
													}
												}
											}
										}
										if(legal.hasTownshipLegal() && isEnableTownshipLegal()){
											if(!StringUtils.isEmpty(addiction)&&
													(!StringUtils.isEmpty(legal.getTownShip().getSection())||
															!StringUtils.isEmpty(legal.getTownShip().getSection())||
															!StringUtils.isEmpty(legal.getTownShip().getTownship())
													)){
												LegalStruct legalStruct1 = new LegalStruct(true);
												legalStruct1.setAddition(addiction);
												legalStruct.add(legalStruct1);
											}
										}
									}
								}
							}
						}
					}
					if(isEnableSubdivision()) {
						processSubdivisionName(global, originalLot, originalBlock,
								originalUnit, temporarySubdivisionsForCondoSearch);
						
					}
					if (isTreatTractAsArb() && StringUtils.isNotEmpty(originalTract)){
						Set<LegalStruct> legalStructTemp = new HashSet<LegalStruct> (legalStruct);
						for (LegalStruct str : legalStructTemp){
							
							String orderSubdivisionName = global.getSa().getValidatedSubdivisionName();
							if(StringUtils.isNotEmpty(orderSubdivisionName)) {
								if(StringUtils.isEmpty(originalBlock) || StringUtils.isEmpty(originalLot)){
									processSubdivisionTractPlatBookPage(orderSubdivisionName, str.getPlatBook(), str.getPlatPage(), originalTract);
								}
							}
							
							String subdivisionName = getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME);
							if(StringUtils.isNotEmpty(subdivisionName)) {
								if(StringUtils.isEmpty(originalBlock) || StringUtils.isEmpty(originalLot)){
									processSubdivisionTractPlatBookPage(subdivisionName, str.getPlatBook(), str.getPlatPage(), originalTract);
								}
							}
						}
					}
					legalStruct = keepOnlyGoodLegals(legalStruct);
				
				}
				
								
				
				
				//if(global.getSa().isUpdate()) {
					
				try {
					DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
					long miServerId = dataSite.getServerId();
					for (LegalI legal : global.getSa().getForUpdateSearchLegalsNotNull(miServerId)) {
						treatLegalFromSavedDocument("Legal Saved From Parent Site as Search Parameters", legal, true, platBookPageFromUser);
					}
				} catch (Exception e) {
					logger.error("Error loading names for Update saved from Parent Site", e);
				}
				legalStruct = keepOnlyGoodLegals(legalStruct);
				
				
				performValidationOnList();
					
				//}
				
				global.setAdditionalInfo(additionalInfoKey,legalStruct);
				
				if(allSubdivisionNames != null && allSubdivisionNames.size() > 0) {
					global.setAdditionalInfo(subdivisionSetKey,allSubdivisionNames);
				}
				
				/*
				if(legalStruct.size()>0){
					if( isPlatedMultyLot(legalStruct) ){
						//boostrapSubdividedData(legalStruct, global, true);
					}else{
						//boostrapSubdividedData(legalStruct, global, false);
					}
					//if( isSectionalMultyQv(legalStruct) || legalStruct.size()==1 ){
						//boostrapSectionalData(legalStruct, global);
					//}
				}
				*/
			}
			finally{
				m.releaseAccess();
			}
			
			if(legalStruct.isEmpty()) {
				if(CountyConstants.AK_Anchorage_Borough_STRING.equals(global.getCountyId())) {
					//if they are different let's check if the plat book-page from AO is the one available in search page
					m.getAccess();
					try {
						List<DocumentI> aoDocuments = m.getDocumentsWithDataSource(true, "AO");
						for (DocumentI documentI : aoDocuments) {
							if(documentI instanceof AssessorDocumentI) {
								AssessorDocumentI aoDoc = (AssessorDocumentI)documentI;
								if( aoDoc.getProperty() != null 
										&& aoDoc.getProperty().getLegal() != null 
										&& aoDoc.getProperty().getLegal().getSubdivision() != null) {										
									SubdivisionI subdivision = aoDoc.getProperty().getLegal().getSubdivision();
									String aoPlatBook = cleanPlatBook(subdivision.getPlatBook());
									String aoPlatPage = subdivision.getPlatPage();
									String aoLot = subdivision.getLot();
									String aoBlock = subdivision.getBlock();
									
									if(StringUtils.isNotEmpty(aoPlatBook) 
											&& StringUtils.isNotEmpty(aoPlatPage)
											&& (StringUtils.isNotEmpty(aoLot) || StringUtils.isNotEmpty(aoBlock))){
										LegalStruct legalStruct1 = new LegalStruct(false);
										legalStruct1.setLot(org.apache.commons.lang.StringUtils.defaultString(aoLot));
										legalStruct1.setBlock(org.apache.commons.lang.StringUtils.defaultString(aoBlock));
										legalStruct1.setPlatBook(aoPlatBook);
										legalStruct1.setPlatPage(aoPlatPage);
										
										String keyForLegal = getKeyForLegal(legalStruct1.getPlatBook(), legalStruct1.getPlatPage());
										Set<String> foundSources = getLegalSourcesNotNull().get(keyForLegal);
										if(foundSources == null) {
											foundSources = new HashSet<String>();
											getLegalSourcesNotNull().put(keyForLegal, foundSources);
										}
										foundSources.add("AO document");
										
										legalStruct.add(legalStruct1);
									} else if(isTreatTractAsArb() && StringUtils.isNotEmpty(aoPlatBook) 
											&& StringUtils.isNotEmpty(aoPlatPage)
											&& (StringUtils.isEmpty(aoLot) || StringUtils.isEmpty(aoBlock))){
										
										String aoSubName = subdivision.getName();
										addTractAsArbLegalStruct(originalTract, aoPlatBook, aoPlatPage, aoSubName);
									}
								}
								break;
							}
						}
					} finally {
						m.releaseAccess();
					}
				}
			}
			
			
			
			Set<String> foundKeys = getDifferentLegals(legalStruct);
			boolean foundMultipleKeys = foundKeys.size() > 1;
			if( foundMultipleKeys ){
				
				PlatBookPage orderPlatBP = new PlatBookPage(
						cleanPlatBook(global.getSa().getValidatedPlatBook()), 
						global.getSa().getValidatedPlatPage());
				
				
				if(!orderPlatBP.isEmpty()) {
					platBookPageFromUser.add(orderPlatBP);
				}
				
				
				String[] multipleLegalsLogging = getMultipleLegalSources(foundKeys);
				
				
				if(!platBookPageFromUser.isEmpty()) {
					Set<LegalStruct> onlyWithPlat = getOnlyStructuresWithPlat(platBookPageFromUser);	
					if(onlyWithPlat.isEmpty()) {
						global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
						global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegalsLogging[1]);
						SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " + multipleLegalsLogging[0]
								+ " (no valid plat book-page iteration from order or saved)<br><div>" , searchId);
						legalStruct.clear();
						allSubdivisionNames.clear();
						
						global.setAdditionalInfo(additionalInfoKey,legalStruct);
						
						if(allSubdivisionNames != null && allSubdivisionNames.size() > 0) {
							allSubdivisionNames.clear();
						}
						return new ArrayList<LegalStruct>(legalStruct);
					} else {
						SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " + 
								multipleLegalsLogging[0] + ", but continuing automatic search only with " + Arrays.toString(platBookPageFromUser.toArray()) + " (from order and/or saved)<br><div>" , searchId);
						legalStruct.clear();
						legalStruct.addAll(onlyWithPlat);
						
						//but we still need to add subdivision name search from validated data
						allSubdivisionNames.clear();
						
						processSubdivisionName(global, originalLot, originalBlock, originalUnit, new HashSet<String>());
						
						if(allSubdivisionNames.size() > 0) {
							global.setAdditionalInfo(subdivisionSetKey, allSubdivisionNames);
						}
						
						legalStruct = keepOnlyGoodLegals(legalStruct);
						global.setAdditionalInfo(additionalInfoKey, legalStruct);
						
					}
				} else {
					
					Set<LegalStruct> validatedDerivations = getOnlyStructuresMatchingValidatedData(legalStruct);
					//from validated we need to check if we still have multiple legal
					
					if(validatedDerivations.isEmpty()) {
						global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
						global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegalsLogging[1]);
						SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " 
								+ multipleLegalsLogging[0] + " (failed validation with search page info and no plat book-page from order or saved available)<br><div>" , searchId);
						
						legalStruct.clear();
						allSubdivisionNames.clear();
						
						global.setAdditionalInfo(additionalInfoKey,legalStruct);
						
						if(allSubdivisionNames != null && allSubdivisionNames.size() > 0) {
							allSubdivisionNames.clear();
						}
						return new ArrayList<LegalStruct>(legalStruct);
					} else {
					
						foundKeys = getDifferentLegals(validatedDerivations);
						
						if( foundKeys.size() > 1 ){
							global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
							global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegalsLogging[1]);
							SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " 
									+ multipleLegalsLogging[0] + " (failed validation with search page info and no plat book-page from order or saved available)<br><div>" , searchId);
							
						} else {
							
							SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " 
									+ multipleLegalsLogging[0] + ", but continuing automatic search only with " 
									+ Arrays.toString(foundKeys.toArray()) + " (validated with search page info)<br><div>" , searchId);
							legalStruct.clear();
							legalStruct.addAll(validatedDerivations);
							
							//but we still need to add subdivision name search from validated data
							allSubdivisionNames.clear();
							
							processSubdivisionName(global, originalLot, originalBlock, originalUnit, new HashSet<String>());
							
							if(allSubdivisionNames.size() > 0) {
								global.setAdditionalInfo(subdivisionSetKey, allSubdivisionNames);
							}
							
							legalStruct = keepOnlyGoodLegals(legalStruct);
							global.setAdditionalInfo(additionalInfoKey, legalStruct);
						}
					}
				}
			}
			
			if ("CO".equalsIgnoreCase(currentCounty.getState().getStateAbv())) {
				legalStruct = cleanCOEmptyBlock(legalStruct);
				
			}
			if( !foundMultipleKeys && !legalStruct.isEmpty()) {
				String platBook = cleanPlatBook(getSearchAttribute(SearchAttributes.LD_BOOKNO));
				String platPage = getSearchAttribute(SearchAttributes.LD_PAGENO);
				
				if(StringUtils.isEmpty(platPage) || StringUtils.isEmpty(platBook)) {
					
					LegalStruct legalStructToAdd = null;
					
					Set<String> lots = new HashSet<String>();
					Set<String> blocks = new HashSet<String>();
					
					boolean updatedPlatBookPage = false;
					
					for (LegalStruct someStruct : legalStruct) {
						if( StringUtils.isNotEmpty(someStruct.getPlatBook()) && 
								StringUtils.isNotEmpty(someStruct.getPlatPage())) {
							if(!updatedPlatBookPage) {
								global.getSa().setAtribute(SearchAttributes.LD_BOOKNO, someStruct.getPlatBook());
								global.getSa().setAtribute(SearchAttributes.LD_PAGENO, someStruct.getPlatPage());
								updatedPlatBookPage = true;
								
								
								/*
								 * Add an iteration with new things added in search page
								 */
								legalStructToAdd = new LegalStruct(false);
								legalStructToAdd.setPlatBook(someStruct.getPlatBook());
								legalStructToAdd.setPlatPage(someStruct.getPlatPage());
								legalStructToAdd.setBlock(getSearchAttribute(SearchAttributes.LD_SUBDIV_BLOCK));
								legalStructToAdd.setLot(getSearchAttribute(SearchAttributes.LD_LOTNO));
								
								if(StringUtils.isEmpty(legalStructToAdd.getBlock()) && StringUtils.isEmpty(legalStructToAdd.getLot())) {
									legalStructToAdd = null;
								}
								if(!isLoadFromSearchPage()) {
									legalStructToAdd = null;	//do not add new legal from search page
								}
							}
							
							if(StringUtils.isNotEmpty(someStruct.getLot())) {
								lots.add(someStruct.getLot());
							}
							if(StringUtils.isNotEmpty(someStruct.getBlock())) {
								blocks.add(someStruct.getBlock());
							}
							
						}
					}
					
					if(updatedPlatBookPage) {
						
						if(StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_LOTNO)) && 
								StringUtils.isEmpty(getSearchAttribute(SearchAttributes.LD_SUBDIV_BLOCK))) {
							if(lots.size() > 0) {
								String lotFull = null;
								for (String lot : lots) {
									if(lotFull == null) {
										lotFull = lot;
									} else {
										lotFull += ", " + lot;
									}
								}
								global.getSa().setAtribute(SearchAttributes.LD_LOTNO, lotFull);
							}
							
							if(blocks.size() == 1) {
								global.getSa().setAtribute(SearchAttributes.LD_SUBDIV_BLOCK, blocks.iterator().next());
							}
						}
						
						if(legalStructToAdd != null) {
							legalStruct.add(legalStructToAdd);
							if ("CO".equalsIgnoreCase(currentCounty.getState().getStateAbv())) {
								legalStruct = cleanCOEmptyBlock(legalStruct);
							}
						}
						
					}
					
					
				}
			}
			
			if(getPlatTypes() != null) {
				
				Set<LegalStruct> fullList = new LinkedHashSet<LegalStruct>();
				
				for (LegalStruct pds : legalStruct) {
					if(org.apache.commons.lang.StringUtils.isNotBlank(pds.getPlatBookType())) {
						fullList.add(pds);
					} else {
						for (String platType : getPlatTypes()) {
							LegalStruct pdsClone = (LegalStruct) pds.clone();
							pdsClone.setPlatBookType(platType);
							fullList.add(pdsClone);
						}
					}
				}
				
				legalStruct = fullList;
				global.setAdditionalInfo(additionalInfoKey, legalStruct);
			}
			
		}
		
		if(isCheckIfDocumentExists()) {
			
			Set<LegalStruct> fullList = new LinkedHashSet<LegalStruct>(); 
			
			try{
				m.getAccess();
				for (LegalStruct pds : legalStruct) {
					InstrumentI instrumentI = new Instrument();
					
					if(pds.isPlated()) {
						instrumentI.setBook(pds.getPlatBook());
						instrumentI.setPage(pds.getPlatPage());
						
						List<DocumentI> almostLike = m.getDocumentsWithInstrumentsFlexible(false, instrumentI);
						
						if(almostLike.isEmpty()) {
							fullList.add(pds);
						}
						
					} else {
						fullList.add(pds);
					}
				}
			}finally{
				m.releaseAccess();
			}
			
			if(fullList.size() != legalStruct.size()) {
				legalStruct = fullList;
				global.setAdditionalInfo(additionalInfoKey, legalStruct);
			}
		}
		
		boolean containsPlatBookPage = false;
		for (LegalStruct item : legalStruct) {
			if(item.isPlated()) {
				containsPlatBookPage = true;
				break;
			}
		}
		if(!containsPlatBookPage) {
			rejectPlatBookPageFilter = null;
		}
		
		
		
		
		return new ArrayList<LegalStruct>(legalStruct);
	}



	protected List<DocumentI> loadLegalFromRoDocs(Search global, DocumentsManagerI m) {
		List<DocumentI> listRodocs = new ArrayList<DocumentI>();
		
		if(AdditionalInfoKeys.AK_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(checkAlreadyFilledKeyWithDocuments) ||
				AdditionalInfoKeys.AR_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(checkAlreadyFilledKeyWithDocuments) ||
				AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(checkAlreadyFilledKeyWithDocuments)) {
			List<DocumentI> listRodocsSaved = (List<DocumentI>) global.getAdditionalInfo(checkAlreadyFilledKeyWithDocuments);
			if(listRodocsSaved != null && !listRodocsSaved.isEmpty()) {
				listRodocs.addAll(listRodocsSaved);
			}
		}
		
		if(listRodocs.isEmpty()) {
			if(roDoctypesToLoad == null) {
				listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(this, m,global,true));
				if(listRodocs.isEmpty()){
					listRodocs.addAll(getGoodDocumentsOrForCurrentOwner(this, m, global, false));
				}
				if(listRodocs.isEmpty()){
					listRodocs.addAll(m.getRealRoLikeDocumentList(true));
				}
			} else {
				listRodocs.addAll(m.getDocumentsWithDocType(true, roDoctypesToLoad));
			}
			if(AdditionalInfoKeys.AK_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(checkAlreadyFilledKeyWithDocuments) ||
					AdditionalInfoKeys.AR_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(checkAlreadyFilledKeyWithDocuments) || 
					AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR.equals(checkAlreadyFilledKeyWithDocuments)) {
				global.setAdditionalInfo(checkAlreadyFilledKeyWithDocuments, listRodocs);	
			}
			
		}
		
		DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
		
		
		if(lookUpWasWithNames){
			legalStruct = getDataStructForCurrentOwner(m,global);
		} else{
			
			for( DocumentI reg: listRodocs){
				if(!reg.isOneOf(
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
						
						|| isTransferAllowed(reg)
						) {
					for (PropertyI prop: reg.getProperties()){
						if(prop.hasLegal()){
							LegalI legal = prop.getLegal();
							treatLegalFromSavedDocument(reg.prettyPrint(), legal, false, null);
						}
					}
				}
			}
		}
		return listRodocs;
	}



	public boolean isTransferAllowed(DocumentI doc) {
		if(doc instanceof TransferI) {
			return isTransferAllowed((TransferI)doc);
		}
		return false;
	}



	public void saveSubdivisionName(String addiction) {
		if(addiction != null) {
			allSubdivisionNames.add(addiction.toUpperCase().trim());
		}
	}



	public void processSubdivisionName(Search global, String originalLot,
			String originalBlock, String originalUnit,
			Set<String> temporarySubdivisionsForCondoSearch) {
		//2) se cauta cu subdivision name din order lot si block  din search page  daca am si lot sau block
		String orderSubdivisionName = global.getSa().getValidatedSubdivisionName();
		if(StringUtils.isNotEmpty(orderSubdivisionName)) {
			processSubdivisionLotBlock(orderSubdivisionName, originalLot, originalBlock);
			if(StringUtils.isNotEmpty(originalBlock)&&StringUtils.isNotEmpty(originalLot)){
				processSubdivisionLotBlock(orderSubdivisionName, originalLot, "");
			}
			temporarySubdivisionsForCondoSearch.add(orderSubdivisionName);
		}
		//3) se cauta cu subdivision name,lot, block din search page daca am si lot sau block
		String subdivisionName = getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME);
		processSubdivisionLotBlock(subdivisionName, originalLot, originalBlock);
		if(StringUtils.isNotEmpty(originalBlock)&&StringUtils.isNotEmpty(originalLot)){
			processSubdivisionLotBlock(subdivisionName, originalLot, "");
		}
		if(temporarySubdivisionsForCondoSearch.isEmpty() && StringUtils.isNotEmpty(subdivisionName)) {
			temporarySubdivisionsForCondoSearch.add(subdivisionName);
		}
		
		if(!hasSubdividedLegal(legalStruct) && StringUtils.isNotEmpty(originalUnit)) {
			for (String subdivisionForCondo : temporarySubdivisionsForCondoSearch) {
				processSubdivisionLotBlock(subdivisionForCondo, originalUnit, "");
			}
		}
	}



	/**
	 * Tries to validate derivations following the next rules:<br>
	 * 1. First try against user validated lot/block (last seen in search page by user)<br>
	 * 2. If nothing passes check if condo<br>
	 * 3. In condo and has validated unit (at address), use unit as lot.<br> 
	 * 3.1 If unit is Lnnn or L-nnn use nnn as lot and L as block to validate derivations<br>
	 * Please note that this method uses <b>POSITIVE VALIDATION</b> (something not empty must match) 
	 * @param derivations original derivations to be validated
	 * @return validated derivations or empty
	 */
	protected Set<LegalStruct> getOnlyStructuresMatchingValidatedData(Set<LegalStruct> derivations) {
		Set<LegalStruct> modifiedLegalStruct = new HashSet<LegalStruct>();
		
		Search search = getSearch();
		
		String lotOrUnit = search.getSa().getValidatedLot();
		String blockOrBuilding = search.getSa().getValidatedBlock();
		
		if(StringUtils.isNotEmpty(lotOrUnit) || StringUtils.isNotEmpty(blockOrBuilding)) {
			validateDerivationsInternal(derivations, modifiedLegalStruct, lotOrUnit, blockOrBuilding);
		}
		
		if(modifiedLegalStruct.isEmpty() && search.getSa().isCondo()) {
			lotOrUnit = search.getSa().getValidatedAddressUnit();
			if(StringUtils.isNotEmpty(lotOrUnit)) {
				Matcher matcher = LETTERS_NUMBERS_PATTERN.matcher(lotOrUnit);
				if(matcher.matches()) {
					lotOrUnit = matcher.group(2);
					blockOrBuilding = matcher.group(1);
				} else {
					blockOrBuilding = "";
				}
				
				validateDerivationsInternal(derivations, modifiedLegalStruct, lotOrUnit, blockOrBuilding);
			}
			
		}
		
		
		return modifiedLegalStruct;
	}



	public void validateDerivationsInternal(
			Set<LegalStruct> derivations,
			Set<LegalStruct> modifiedLegalStruct, String lotOrUnit,
			String blockOrBuilding) {
		for (LegalStruct struct : derivations) {
			
			if(!struct.isPlated()) {
				//this test is for platted only - multiple LD
				continue;
			}
			
			//positive validation - means exact match (no match on missing in candidates).
			if(StringUtils.isNotEmpty(lotOrUnit)) {
				
				if(StringUtils.isEmpty(struct.getLot())) {
					continue;
				}
				if(lotOrUnit.equalsIgnoreCase(struct.getLot())) {
					//nothing to do here, same lot
				} else {
					boolean matchLotOrUnit = false;
					
					LotInterval lotInterval = new LotInterval(lotOrUnit);
					List<String> allLots = lotInterval.getLotList();
					
					for (String singleLotOrUnit : allLots) {
						if(singleLotOrUnit.equalsIgnoreCase(struct.getLot())) {
							matchLotOrUnit = true;
							break;
						}
					}
					if(!matchLotOrUnit) {
						continue;
					}
				}
			}
			
			if(StringUtils.isNotEmpty(blockOrBuilding)) {
				if(StringUtils.isEmpty(struct.getBlock())) {
					continue;
				}
				if(!blockOrBuilding.equalsIgnoreCase(struct.getBlock())) {
					continue;
				}
			}
			
			modifiedLegalStruct.add(struct);
		}
	}



	protected String[] getMultipleLegalSources(Set<String> foundKeys) {
		String[] result = new String[2];
		for (String foundKey : foundKeys) {
			if (result[0] != null) {
				result[0] += "; Key: [" + foundKey + "]";
			} else {
				result[0] = "Key: [" + foundKey + "]";
			}
			
			Set<String> foundSourcesForKey = getLegalSourcesNotNull().get(foundKey);
			String toShowAsString = null;
			if(foundSourcesForKey != null) {
				for (String foundSourceForKey : foundSourcesForKey) {
					if(toShowAsString == null) {
						toShowAsString = " found in \"" + foundSourceForKey + "\"";
						if(result[1] == null) {
							result[1] = foundSourceForKey;
						} else {
							result[1] += ", " + foundSourceForKey;
						}
					} else {
						toShowAsString += ", \"" + foundSourceForKey + "\"";
						result[1] += ", " + foundSourceForKey;
					}
				}
			}
			if(toShowAsString != null) {
				result[0] += toShowAsString;
			}
			
		}
		return result;
	}



	protected Set<LegalStruct> getOnlyStructuresWithPlat(Set<PlatBookPage> platBookPageFromUser) {
		Set<LegalStruct> modifiedLegalStruct = new HashSet<LegalStruct>();
		if(platBookPageFromUser != null && !platBookPageFromUser.isEmpty()) {
			for (LegalStruct struct : legalStruct) {
				PlatBookPage pbp = new PlatBookPage(struct.getPlatBook(), struct.getPlatPage());
				if(platBookPageFromUser.contains(pbp)){
					modifiedLegalStruct.add(struct);
				}
			}
		}
		return modifiedLegalStruct;
	}



	protected Set<String> getDifferentLegals(Set<LegalStruct> legalStruct) {
		Set<String> foundKeys = new HashSet<String>();
		for (LegalStruct personalDataStruct : legalStruct) {
			if(StringUtils.isNotEmpty(personalDataStruct.getPlatBook()) 
					&& StringUtils.isNotEmpty(personalDataStruct.getPlatPage())) {
				String key = getKeyForLegal(personalDataStruct.getPlatBook().trim(), personalDataStruct.getPlatPage().trim());
				if(!foundKeys.contains(key)) {
					foundKeys.add(key);	
				}
				
			}
		}
		return foundKeys;
	}



	public String getKeyForLegal(String platBook, String platPage) {
		String key = "PlatBook = " + platBook + 
			" / PlatPage = " + platPage;
		return key;
	}



	protected void performValidationOnList() {
	}



	protected void treatLegalFromSavedDocument(String sourceKey, LegalI legal, boolean useAlsoSubdivisionName, Set<PlatBookPage> platBookPageFromUser) {
		
		String subdivisionName = "";
		
		subdivisionName = treatOnlySubdividedLegal(sourceKey, legal, useAlsoSubdivisionName, subdivisionName, platBookPageFromUser);
		treatOnlyTownshipLegal(sourceKey, legal, useAlsoSubdivisionName, subdivisionName);
		treatOnlySubdivision(sourceKey, legal, useAlsoSubdivisionName, subdivisionName);
	}

	protected String treatOnlySubdividedLegal(String sourceKey, LegalI legal,
			boolean useAlsoSubdivisionName, String subdivisionName, Set<PlatBookPage> platBookPageFromUser) {
		if(isEnableSubdividedLegal() && legal.hasSubdividedLegal()){
			SubdivisionI subdiv = legal.getSubdivision();
			
			String block = subdiv.getBlock();
			String lot = subdiv.getLot();
			String platBook = cleanPlatBook(subdiv.getPlatBook());
			String platPage = subdiv.getPlatPage();
			String platInst = subdiv.getPlatInstrument();
			
			LegalStruct legalStruct1 = new LegalStruct(false);
			
			legalStruct1.setPlatBook(StringUtils.isEmpty(platBook)?"":platBook);
			legalStruct1.setPlatPage(StringUtils.isEmpty(platPage)?"":platPage);
			legalStruct1.setPlatInst(StringUtils.isEmpty(platInst)?"":platInst);
			
			loadSecondaryPlattedLegal(legal, legalStruct1); 
			
			
			if(!incompleteData(legalStruct1)) {
				String keyForLegal = getKeyForLegal(legalStruct1.getPlatBook(), legalStruct1.getPlatPage());
				Set<String> foundSources = getLegalSourcesNotNull().get(keyForLegal);
				if(foundSources == null) {
					foundSources = new HashSet<String>();
					getLegalSourcesNotNull().put(keyForLegal, foundSources);
				}
				foundSources.add(sourceKey);
				if(platBookPageFromUser != null) {
					PlatBookPage platBookPage = new PlatBookPage(legalStruct1.getPlatBook(), legalStruct1.getPlatPage());
					if(!platBookPage.isEmpty()) {
						platBookPageFromUser.add(platBookPage);
					}
				}
				legalStruct.add(legalStruct1);
				
				if(StringUtils.isNotEmpty(legalStruct1.getLot())&&StringUtils.isNotEmpty(legalStruct1.getBlock())){
					forceSubdividedIterationWithoutBlock(legalStruct1);
				}
			}
			
			if(useAlsoSubdivisionName) {
				subdivisionName = subdiv.getName();
				if(StringUtils.isNotEmpty(subdivisionName) && 
						(StringUtils.isNotEmpty(lot)||StringUtils.isNotEmpty(block))){
					
						legalStruct1 = new LegalStruct(false);
						legalStruct1.setAddition(subdivisionName);
						
						loadSecondaryPlattedLegal(legal, legalStruct1); 
						
						saveSubdivisionName(subdivisionName);
						if(platBookPageFromUser != null) {
							PlatBookPage platBookPage = new PlatBookPage(legalStruct1.getPlatBook(), legalStruct1.getPlatPage());
							if(!platBookPage.isEmpty()) {
								platBookPageFromUser.add(platBookPage);
							}
						}
						legalStruct.add(legalStruct1);
						
						if(StringUtils.isNotEmpty(legalStruct1.getLot())&&StringUtils.isNotEmpty(legalStruct1.getBlock())){
							forceSubdividedIterationWithoutBlock(legalStruct1);
						} else if(isTreatTractAsArb() && (StringUtils.isEmpty(legalStruct1.getLot()) || StringUtils.isEmpty(legalStruct1.getBlock()))){
							LegalStruct legalStruct3 = new LegalStruct(false);
							legalStruct3.setPlatBook(legalStruct1.getPlatBook());
							legalStruct3.setPlatPage(legalStruct1.getPlatPage());
							legalStruct3.setArb(subdiv.getTract());
							legalStruct.add(legalStruct3);
							
							LegalStruct legalStruct4 = new LegalStruct(false);
							legalStruct4.setAddition(legalStruct1.getAddition());
							legalStruct4.setArb(subdiv.getTract());
							legalStruct.add(legalStruct4);
						}
					}
			}
		}
		return subdivisionName;
	}


	@Override
	public void loadSecondaryPlattedLegal(LegalI legal, LegalStruct legalStruct) {
		legalStruct.setLot(legal.getSubdivision().getLot());
		legalStruct.setBlock(legal.getSubdivision().getBlock());
	}

	protected void forceSubdividedIterationWithoutBlock(LegalStruct legalStruct1) {
		if(isForceSubdividedIterationWithoutBlock()) {
			if(StringUtils.isNotEmpty(legalStruct1.getLot())&&StringUtils.isNotEmpty(legalStruct1.getBlock())){ 
				LegalStruct legalStruct2 = (LegalStruct)legalStruct1.clone();
				legalStruct2.setBlock("");
				legalStruct.add(legalStruct2);
			}
		}
	}

	protected String cleanPlatBook(String platBook) {
		if(platBook == null) {
			return "";
		}
		return platBook.trim();
	}

	protected void treatOnlyTownshipLegal(String sourceKey, LegalI legal,
			boolean useAlsoSubdivisionName, String subdivisionName) {
		if(isEnableTownshipLegal() && legal.hasTownshipLegal()){
			TownShipI township = legal.getTownShip();
			
			String arb = township.getArb();
			String sec = township.getSection();
			String tw = township.getTownship();
			String rg = township.getRange();
			int qo = township.getQuarterOrder();
			String qv = township.getQuarterValue();
			
			LegalStruct legalStruct1 = new LegalStruct(true);
			legalStruct1.setArb(StringUtils.isEmpty(arb)?"":arb);
			legalStruct1.setSection(StringUtils.isEmpty(sec)?"":sec);
			legalStruct1.setTownship(StringUtils.isEmpty(tw)?"":tw);
			legalStruct1.setRange(StringUtils.isEmpty(rg)?"":rg);
			legalStruct1.setQuarterValue(StringUtils.isEmpty(qv)?"":qv);
			legalStruct1.setQuarterOrder(String.valueOf(qo<=0?"":qo));
			legalStruct.add(legalStruct1);
			
			if(useAlsoSubdivisionName) {
				if(!StringUtils.isEmpty(subdivisionName)&&
						(!StringUtils.isEmpty(legal.getTownShip().getSection())||
								!StringUtils.isEmpty(legal.getTownShip().getRange())||
								!StringUtils.isEmpty(legal.getTownShip().getTownship())
						)){
					legalStruct1 = new LegalStruct(true);
					legalStruct1.setAddition(subdivisionName);
					legalStruct.add(legalStruct1);
				}
			
			}
			
		}
	}

	protected void treatOnlySubdivision(String sourceKey, LegalI legal,
			boolean useAlsoSubdivisionName, String subdivisionName) {
	}

	

	protected void processSubdivisionLotBlock(String subdivName, String lot, String block) {
		if(legalStruct == null) {
			return;
		}
		if(StringUtils.isNotEmpty(subdivName)&&!(StringUtils.isEmpty(lot)&&StringUtils.isEmpty(block))){
			saveSubdivisionName(subdivName);
			for (LotInterval lotInterval: LotMatchAlgorithm.prepareLotInterval(lot)) {
				List<String> allLots = lotInterval.getLotList();
				if(allLots.size()>0){
					for(String l:allLots){
						LegalStruct legalStruct1 = new LegalStruct(false);
						legalStruct1.setLot(StringUtils.isEmpty(l)?"":l);
						legalStruct1.setBlock(StringUtils.isEmpty(block)?"":block);
						legalStruct1.setAddition(StringUtils.isEmpty(subdivName)?"":subdivName);
						legalStruct.add(legalStruct1);
					}
				}else{
					LegalStruct legalStruct1 = new LegalStruct(false);
					legalStruct1.setBlock(StringUtils.isEmpty(block)?"":block);
					legalStruct1.setAddition(StringUtils.isEmpty(subdivName)?"":subdivName);
					legalStruct.add(legalStruct1);
				}
			}
		}
	}
	
	protected void processSubdivisionTractPlatBookPage(String subdivName, String platBook, String platPage, String tract) {
		if(legalStruct == null) {
			return;
		}
		addTractAsArbLegalStruct(tract, platBook, platPage, subdivName);
		
		saveSubdivisionName(subdivName);
			
	}
	
	private void setAddictionPerCounty(TSServerInfoModule module, String countyName, String state, String subdiv){
		
		if(module!=null){
			if("AR".equalsIgnoreCase(state)){
				if("Pulaski".equalsIgnoreCase(countyName)){
					module.setData(20, subdiv);
					if(!StringUtils.isEmpty(subdiv)){
						module.addFilter(new AddictionFilter(searchId, subdiv));
						initFilter(module);
					}
				} else if( isUseAddictionInsteadOfSubdivision()) {
					module.setData(20, subdiv);
				} else{
					module.setData(19, subdiv);
				}
			} else if( isUseAddictionInsteadOfSubdivision()) {
				module.setData(20, subdiv);
			} else{
				module.setData(19, subdiv);
				/*if(!StringUtils.isEmpty(subdiv)){
					module.addFilter(NameFilterFactory.getDefaultNameFilterForSubdivision(searchId, allSubdivisionNames));
					initFilter(module);
				}*/
			}
		}
	}
	private void setArbPerCounty(TSServerInfoModule module, String countyName, String state, String subdiv, String arb){
		
		if(module != null){
			if("AK".equalsIgnoreCase(state) && isTreatTractAsArb()){
				if(countyName.toLowerCase().contains("anchorage")){
					if (StringUtils.isNotEmpty(arb)){
						module.setData(22, arb);
					}
				}
			}
		}
	}
	
	protected void loadDerrivation(TSServerInfoModule module, LegalStruct str){
		
		switch(module.getModuleIdx()){
			case TSServerInfo.SUBDIVISION_MODULE_IDX:
				module.setData(2, str.getLot());
				module.setData(3, str.getBlock());
				module.setData(4, str.getPlatBook());
				module.setData(5, str.getPlatPage());
				module.setData(8, str.getSection());
				module.setData(9, str.getTownship());
				module.setData(10, str.getRange());
				setAddictionPerCounty(module,currentCounty.getName(),currentCounty.getState().getStateAbv(),str.getAddition());
				setArbPerCounty(module, currentCounty.getName(), currentCounty.getState().getStateAbv(), str.getAddition(), str.getArb());
				module.setData(24, str.getParcel());
				
				
				if(AdditionalInfoKeys.TS_LOOK_UP_DATA.equals(additionalInfoKey)) {
					if(StringUtils.isEmpty(str.getPlatBook()) 
							&& StringUtils.isEmpty(str.getPlatPage())
							&& StringUtils.isNotEmpty(str.getAddition())) {
						
						GenericMultipleLegalFilter legalFilter = new GenericMultipleLegalFilter(searchId);
						HashSet<String> ignoreLotWhenServerDoctypeIs = new HashSet<String>();
						ignoreLotWhenServerDoctypeIs.add("TUBCARD");
						legalFilter.setIgnoreLotWhenServerDoctypeIs(ignoreLotWhenServerDoctypeIs);
						legalFilter.setUseLegalFromSearchPage(true);
						legalFilter.disableAll();
						legalFilter.setEnableLot(true);
						legalFilter.setEnableBlock(true);
						if(rejectPlatBookPageFilter != null) {
							module.addFilter(rejectPlatBookPageFilter);
						}
						module.addFilter(new SubdivisionFilter(searchId));
						module.addFilter(legalFilter);
					}
					initFilter(module);
				}
				
				
			break;
			case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
				module.setData(0, str.getPlatBook().replaceFirst("^0+", ""));
				module.setData(1, str.getPlatPage().replaceFirst("^0+", ""));
			break;
			case TSServerInfo.INSTR_NO_MODULE_IDX:
				module.setData(0, str.getPlatBook().replaceFirst("^0+", "")+"-"+str.getPlatPage().replaceFirst("^0+", ""));
			break;
			case TSServerInfo.PARCEL_ID_MODULE_IDX:
					module.setData(0, GenericDASLTS.prepareAPN(searchId) );
			break;
			default:
				for (Object functionObject : module.getFunctionList()) {
					if (functionObject instanceof TSServerInfoFunction) {
						TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
						switch(function.getIteratorType()){
							case FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE:
								function.setParamValue(str.getPlatBook() + "-" + str.getPlatPage());
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_LOT:
								function.setParamValue(str.getLot());
								break;
							case FunctionStatesIterator.ITERATOR_TYPE_BLOCK:
								function.setParamValue(str.getBlock());
								break;
						}
					}
				}
				break;
		}
	}
	
	boolean hasPlatBookPlatPageLegal(Set<LegalStruct> legals){
		for(LegalStruct str:legals){
			if(!StringUtils.isEmpty(str.getPlatBook())){
				return true;
			}
		}
		return false;
	}
	
	boolean hasSubdividedLegal(Set<LegalStruct> legals){
		for(LegalStruct str:legals){
			if(!StringUtils.isEmpty(str.getPlatBook()) || !StringUtils.isEmpty(str.getAddition())){
				return true;
			}
		}
		return false;
	}
	
	static class AddictionFilter extends FilterResponse{
		private static final long serialVersionUID = 4076715304481196010L;
		private String ref;
		
		public AddictionFilter(long searchId, String ref) {
			super(searchId);
			this.ref=ref;
		}
		
		@Override
		public BigDecimal getScoreOneRow(ParsedResponse row) {
			
			if(StringUtils.isEmpty(ref)){
				return BigDecimal.ONE;
			}
			
			DocumentI doc = row.getDocument();
			
			String  subdivCand = "";
			
			try{
				for(PropertyI prop:doc .getProperties()){
					subdivCand = prop.getLegal().getSubdivision().getName();
					break;
				}
			}catch(Exception e){}
			
			if(StringUtils.isEmpty(subdivCand)){
				return BigDecimal.ONE;
			}
			
			if(subdivCand.replaceAll("[^a-zA-Z1-9]", "").equalsIgnoreCase(ref.replaceAll("[^a-zA-Z0-9]", ""))){
				return BigDecimal.ONE;
			}
			
			return BigDecimal.ZERO;
		}
		
	}
	
	protected Set<LegalStruct> getDataStructForCurrentOwner(DocumentsManagerI m, Search search){
		final Set<LegalStruct> ret = new HashSet<LegalStruct>();
		
		List<RegisterDocumentI> listRodocs = m.getRoLikeDocumentList();
		DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
		
		String lot = search.getSa().getAtribute(SearchAttributes.LD_LOTNO);
		String sec = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_SEC);
		
		SearchAttributes sa	= search.getSa();
		PartyI owner 		= sa.getOwners();
		
		if(!StringUtils.isEmpty(lot)){
			for(RegisterDocumentI doc:listRodocs){
				treatOnlySubdividedLegalForCurrentOwner(ret, owner, doc);
				treatOnlySubdivisionLegalForCurrentOwner(ret, owner, doc);
			}
		} else if(!StringUtils.isEmpty(sec) && StringUtils.isEmpty(lot)){
			treatOnlyTownshipLegalForCurrentOwner(ret, listRodocs, owner);
		}
		
		return ret;
	}

	protected void treatOnlySubdividedLegalForCurrentOwner(
			final Set<LegalStruct> ret, PartyI owner,
			RegisterDocumentI doc) {
		if(isEnableSubdividedLegal()) {
			for(PropertyI prop:doc.getProperties()){
				if(doc.isOneOf("MORTGAGE","TRANSFER","RELEASE") && prop.hasSubdividedLegal()){
					SubdivisionI sub = prop.getLegal().getSubdivision();
					LegalStruct ret1 = new LegalStruct(false);
					loadSecondaryPlattedLegal(prop.getLegal(), ret1);
					ret1.setPlatBook(sub.getPlatBook());
					ret1.setPlatPage(sub.getPlatPage());
					
					boolean nameMatched = false;
					
					if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
							GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
							GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
							GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
							nameMatched = true;
					}
					
					if( nameMatched 
							&& (StringUtils.isNotEmpty(ret1.getLot()) || StringUtils.isNotEmpty(ret1.getBlock())) 
							&& StringUtils.isNotEmpty(ret1.getPlatBook()) 
							&& StringUtils.isNotEmpty(ret1.getPlatPage()) ){
						String sourceKey = doc.prettyPrint();
						
						String keyForLegal = getKeyForLegal(ret1.getPlatBook(), ret1.getPlatPage());
						Set<String> foundSources = getLegalSourcesNotNull().get(keyForLegal);
						if(foundSources == null) {
							foundSources = new HashSet<String>();
							getLegalSourcesNotNull().put(keyForLegal, foundSources);
						}
						foundSources.add(sourceKey);
						ret.add(ret1);
						
						if(StringUtils.isNotEmpty(ret1.getLot()) && StringUtils.isNotEmpty(ret1.getBlock())){
							LegalStruct ret2 = (LegalStruct)ret1.clone();
							ret2.setBlock("");
							ret.add(ret2);
						}
						
					}
				}
			}
		}
	}



	public Map<String, Set<String>> getLegalSourcesNotNull() {
		if(legalSources == null) {
			legalSources = new HashMap<String, Set<String>>();
		}
		return legalSources;
	}
	
	protected void treatOnlySubdivisionLegalForCurrentOwner(
			final Set<LegalStruct> ret, PartyI owner,
			RegisterDocumentI doc) {
	}

	protected void treatOnlyTownshipLegalForCurrentOwner(
			final Set<LegalStruct> ret,
			List<RegisterDocumentI> listRodocs, PartyI owner) {
		if(isEnableTownshipLegal()) {
			for(RegisterDocumentI doc:listRodocs){
				if(doc.isOneOf("TRANSFER","MORTGAGE","RELEASE")){
					for(PropertyI prop:doc.getProperties()){
						if( prop.hasTownshipLegal()){
							TownShipI sub = prop.getLegal().getTownShip();
							LegalStruct ret1 = new LegalStruct(true);
							ret1.setAddition(sub.getAddition());
							ret1.setParcel(sub.getParcel());
							ret1.setSection(sub.getSection());
							ret1.setTownship(sub.getTownship());
							ret1.setRange(sub.getRange());
							
							boolean nameMatched = false;
							
							if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
									GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
							
							if( nameMatched && !StringUtils.isEmpty(ret1.getAddition()) && !StringUtils.isEmpty(ret1.getSection())&&!StringUtils.isEmpty(ret1.getTownship())&&!StringUtils.isEmpty(ret1.getRange())){
								ret.add(ret1);
							}
						}
					}
				}
			}
		}
	}
	
	protected Set<LegalStruct> keepOnlyGoodLegals(Set<LegalStruct> legals){
		Set<LegalStruct> good = new HashSet<LegalStruct>();
		for(LegalStruct str:legals){
			if(!incompleteData(str)){
				good.add(str);
			}
		}
		return good;
	}
	
	private static boolean incompleteData(LegalStruct str) {
		if( str==null ){
			return true;
		}
		
		boolean emptyLot = StringUtils.isEmpty(str.getLot());
		//set ignore block due to 7246: CO Summit TS -- legal search issue
		//boolean emptyBlock = StringUtils.isEmpty(str.block);
		boolean emptyPlatBook = StringUtils.isEmpty(str.getPlatBook());
		boolean emptyPlatPage = StringUtils.isEmpty(str.getPlatPage());
		
		boolean emptySection = StringUtils.isEmpty(str.getSection());
		boolean emptyRange = StringUtils.isEmpty(str.getRange());
		boolean emptyTownship = StringUtils.isEmpty(str.getTownship());
		boolean emptyArb = StringUtils.isEmpty(str.getArb());
		boolean emptyAddition = StringUtils.isEmpty(str.getAddition());
		
		if(!str.sectional){
			if (!emptyArb){
				return ((emptyPlatBook && emptyPlatPage && emptyArb) || (emptyArb && emptyAddition)) ;
			}
			return   ( (emptyPlatBook||emptyPlatPage) && emptyAddition) || (emptyLot) ;
		}
		
		return emptyAddition && (emptySection || emptyTownship || emptyRange) ;
	}
	
	private Set<LegalStruct> cleanCOEmptyBlock(Set<LegalStruct> legals){		//bug 6924	
		Set<LegalStruct> good = new HashSet<LegalStruct>();
		for(LegalStruct str:legals){
				if (StringUtils.isNotEmpty(str.getLot()) && StringUtils.isEmpty(str.getBlock()) &&
					StringUtils.isNotEmpty(str.getAddition()) && hasLotBlockAddition(legals,str.getLot(),str.getAddition())){
					//do nothing
				}	 
				else if (StringUtils.isNotEmpty(str.getLot()) && StringUtils.isEmpty(str.getBlock()) &&
						 StringUtils.isNotEmpty(str.getPlatBook()) && StringUtils.isNotEmpty(str.getPlatPage()) 
						 && hasLotBlockPlatBookPlatPage(legals,str.getLot(),str.getPlatBook(),str.getPlatPage())){
					//do nothing
				}
				else {
					good.add(str);
				}
			}	
		return good;
	}
	
	private boolean hasLotBlockAddition(Set<LegalStruct> legals, String lot, String addition){
		
		for(LegalStruct str:legals){
			if (lot.equalsIgnoreCase(str.getLot()) && StringUtils.isNotEmpty(str.getBlock()) &&  addition.equalsIgnoreCase(str.getAddition()))
				return true;
		}
		return false;
	}
	
	private boolean hasLotBlockPlatBookPlatPage(Set<LegalStruct> legals, String lot, String platbook, String platpage){
		
		for(LegalStruct str:legals){
			if (lot.equalsIgnoreCase(str.getLot()) && StringUtils.isNotEmpty(str.getBlock()) 
					&&  platbook.equalsIgnoreCase(str.getPlatBook()) &&  platpage.equalsIgnoreCase(str.getPlatPage()))
				return true;
		}
		return false;
	}
	
	/*
	private static boolean isPlatedMultyLot(Set<PersonalDataStruct> legalStructList) {
		boolean isPlatedMultyLot = true;
		
		if(legalStructList == null || legalStructList.size()==0){
			return false;
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
					if(!p.block.equalsIgnoreCase(first.block)||!p.platBook.equalsIgnoreCase(first.platBook)||!p.platPage.equalsIgnoreCase(first.platPage)){
						isPlatedMultyLot =  false;
						break;
					}
				}
			}
		}
		return isPlatedMultyLot ;
	}
	
	
	private static PersonalDataStruct getFirstPlatedStruct(Set<PersonalDataStruct> list){
		for(PersonalDataStruct struct:list){
			if(struct.isPlated()){
				return struct;
			}
		}
		return null;
	}
	*/
	
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

	public void setTreatTractAsArb(boolean treatTractAsArb){
		this.treatTractAsArb = treatTractAsArb;
	}
	
	//B 6527
	public boolean isTreatTractAsArb(){
		return treatTractAsArb;
	}
	
	public boolean isEnableSubdivision() {
		return enableSubdivision;
	}

	public void setEnableSubdivision(boolean enableSubdivision) {
		this.enableSubdivision = enableSubdivision;
	}

	public String getAdditionalInfoKey() {
		return additionalInfoKey;
	}

	public void setAdditionalInfoKey(String additionalInfoKey) {
		this.additionalInfoKey = additionalInfoKey;
	}
	
	public String[] getRoDoctypesToLoad() {
		return roDoctypesToLoad;
	}

	public void setRoDoctypesToLoad(String[] roDoctypesToLoad) {
		this.roDoctypesToLoad = roDoctypesToLoad;
	}

	public String getCheckAlreadyFilledKeyWithDocuments() {
		return checkAlreadyFilledKeyWithDocuments;
	}

	public void setCheckAlreadyFilledKeyWithDocuments(
			String checkAlreadyFilledKeyWithDocuments) {
		this.checkAlreadyFilledKeyWithDocuments = checkAlreadyFilledKeyWithDocuments;
	}
	
	public void addTractAsArbLegalStruct(String tract, String pb, String pp, String subName){
		
		if (StringUtils.isNotEmpty(pb) && StringUtils.isNotEmpty(pp)){
			LegalStruct legalStruct1 = new LegalStruct(false);
			legalStruct1.setPlatBook(pb);
			legalStruct1.setPlatPage(pp);
			legalStruct1.setArb(tract);
			legalStruct.add(legalStruct1);
			
			if (tract.matches("(?i)[A-Z]\\d+")){
				String tractFreakedOut = tract.replaceFirst("(?i)([A-Z])(\\d+)", "$1-$2");
				legalStruct1 = new LegalStruct(false);
				legalStruct1.setPlatBook(pb);
				legalStruct1.setPlatPage(pp);
				legalStruct1.setArb(tractFreakedOut);
				legalStruct.add(legalStruct1);
			}
		}
		
		if (StringUtils.isNotEmpty(subName)){
			LegalStruct legalStruct2 = new LegalStruct(false);
			legalStruct2.setAddition(subName);
			legalStruct2.setArb(tract);
			legalStruct.add(legalStruct2);
			
			if (tract.matches("(?i)[A-Z]\\d+")){
				String tractFreakedOut = tract.replaceFirst("(?i)([A-Z])(\\d+)", "$1-$2");
				legalStruct2 = new LegalStruct(false);
				legalStruct2.setAddition(subName);
				legalStruct2.setArb(tractFreakedOut);
				legalStruct.add(legalStruct2);
			}
		}
		
	}
	public static List<DocumentI> getGoodDocumentsOrForCurrentOwner(LegalDescriptionIteratorI legalDescriptionIteratorI, DocumentsManagerI m, Search search, boolean applyNameMatch){
		final List<DocumentI> ret = new ArrayList<DocumentI>();
		
		List<RegisterDocumentI> listRodocs = m.getRealRoLikeDocumentList();
		DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
		
		SearchAttributes sa	= search.getSa();
		PartyI owner 		= sa.getOwners();
		
		
		for(RegisterDocumentI doc:listRodocs){
			boolean found = false;
			for(PropertyI prop:doc.getProperties()){
				if(((doc.isOneOf("MORTGAGE","RELEASE") || legalDescriptionIteratorI.isTransferAllowed(doc)) && applyNameMatch)
						|| ((doc.isOneOf("MORTGAGE") || legalDescriptionIteratorI.isTransferAllowed(doc)) && !applyNameMatch)){
					if(prop.hasSubdividedLegal()){
						SubdivisionI sub = prop.getLegal().getSubdivision();
						LegalStruct ret1 = new LegalStruct(false);
						
						ret1.setPlatBook(sub.getPlatBook());
						ret1.setPlatPage(sub.getPlatPage());
						
						legalDescriptionIteratorI.loadSecondaryPlattedLegal(prop.getLegal(), ret1);
						
						boolean nameMatched = false;
						
						if(applyNameMatch){
							if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
									GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						
						if( (nameMatched||!applyNameMatch) 
								&& (StringUtils.isNotEmpty(ret1.getLot()) || StringUtils.isNotEmpty(ret1.getBlock())) 
								&& StringUtils.isNotEmpty(ret1.getPlatBook()) 
								&& StringUtils.isNotEmpty(ret1.getPlatPage()) ){
							ret.add(doc);
							found = true;
							break;
						}
					}
					else if(prop.hasTownshipLegal()){
						TownShipI sub = prop.getLegal().getTownShip();
						LegalStruct ret1 = new LegalStruct(true);
						ret1.setSection(sub.getSection());
						ret1.setTownship(sub.getTownship());
						ret1.setRange(sub.getRange());
						int qo = sub.getQuarterOrder();
						String qv = sub.getQuarterValue();
						if(qo>0){
							ret1.setQuarterOrder(qo+"");
						}
						ret1.setQuarterValue(qv);
						ret1.setArb(sub.getArb());
						boolean nameMatched = false;
						
						if(applyNameMatch){
							if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
									GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						
						if( (nameMatched||!applyNameMatch) && (StringUtils.isNotEmpty(qv)||qo>0||StringUtils.isNotEmpty(ret1.getArb())) 
								&& !StringUtils.isEmpty(ret1.getSection())&&!StringUtils.isEmpty(ret1.getTownship())&&!StringUtils.isEmpty(ret1.getRange())){
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

	public boolean isTransferAllowed(RegisterDocumentI doc) {
		return doc != null && doc.isOneOf(DocumentTypes.TRANSFER);
	}



	public boolean isUseAddictionInsteadOfSubdivision() {
		return useAddictionInsteadOfSubdivision;
	}

	public void setUseAddictionInsteadOfSubdivision(
			boolean useAddictionInsteadOfSubdivision) {
		this.useAddictionInsteadOfSubdivision = useAddictionInsteadOfSubdivision;
	}

	public boolean isLoadFromSearchPage() {
		return loadFromSearchPage;
	}

	/**
	 * Load information from search page not just from lookup<br>
	 * Default is to load
	 * @param loadFromSearchPage
	 */
	public void setLoadFromSearchPage(boolean loadFromSearchPage) {
		this.loadFromSearchPage = loadFromSearchPage;
	}

	public boolean isLoadFromSearchPageIfNoLookup() {
		return loadFromSearchPageIfNoLookup;
	}
	
	/**
	 * Load information from search page only if nothing found after lookup<br>
	 * Search page data will be considered only as backup<br><br>
	 * Default is set to <code>false</code>.
	 * Please check the other flag {@link LegalDescriptionIterator.isLoadFromSearchPage} to turn off default search page
	 * @return
	 */
	public void setLoadFromSearchPageIfNoLookup(boolean loadFromSearchPageIfNoLookup) {
		this.loadFromSearchPageIfNoLookup = loadFromSearchPageIfNoLookup;
	}

	public boolean isForceSubdividedIterationWithoutBlock() {
		return forceSubdividedIterationWithoutBlock;
	}

	public void setForceSubdividedIterationWithoutBlock(boolean forceSubdividedIterationWithoutBlock) {
		this.forceSubdividedIterationWithoutBlock = forceSubdividedIterationWithoutBlock;
	}

	public String[] getPlatTypes() {
		return platTypes;
	}

	public void setPlatTypes(String[] platTypes) {
		this.platTypes = platTypes;
	}

	public boolean isCheckIfDocumentExists() {
		return checkIfDocumentExists;
	}

	public void setCheckIfDocumentExists(boolean checkIfDocumentExists) {
		this.checkIfDocumentExists = checkIfDocumentExists;
	}

	public boolean isLoadedFromSearchPage() {
		return loadedFromSearchPage;
	}

	public void setLoadedFromSearchPage(boolean loadedFromSearchPage) {
		this.loadedFromSearchPage = loadedFromSearchPage;
	}

	public boolean isLookUpWasWithNames() {
		return lookUpWasWithNames;
	}
	
}

class RejectPlatBookPageFilter extends FilterResponse {

	private static final long serialVersionUID = 1L;

	public RejectPlatBookPageFilter(long searchId) {
		super(searchId);
		setThreshold(ATSDecimalNumberFormat.ONE);
	}
	
	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		
		DocumentI document = row.getDocument();
		if(document != null && document instanceof RegisterDocumentI) {
			RegisterDocumentI regDoc = (RegisterDocumentI) document;
			for (PropertyI property : regDoc.getProperties()) {
				if(property.hasSubdividedLegal() && 
						(StringUtils.isNotEmpty(property.getLegal().getSubdivision().getPlatBook()) 
						|| StringUtils.isNotEmpty(property.getLegal().getSubdivision().getPlatPage()))) {
					return ATSDecimalNumberFormat.ZERO;
					
				}
			}
			
		}
		return  ATSDecimalNumberFormat.ONE;
	}
	
	@Override
    public String getFilterName(){
    	return "Filter by Plat Book-Page Information";
    }
    
    @Override
	public String getFilterCriteria(){
    	return "Plat Book-Page Information -> 'Reject if available'";
    }
	
}

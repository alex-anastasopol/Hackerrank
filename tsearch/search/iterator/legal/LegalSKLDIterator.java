package ro.cst.tsearch.search.iterator.legal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.bean.LegalSKLDIteratorEntry;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.data.PlatBookPage;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.Interval;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class LegalSKLDIterator extends GenericRuntimeIterator<LegalSKLDIteratorEntry> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	private static final Pattern LETTERS_NUMBERS_PATTERN = Pattern.compile("([a-zA-Z]+)-?(\\d+)");
	//private static final Pattern NUMBERS_LETTERS_PATTERN = Pattern.compile("(\\d+)-?([a-zA-Z]+)");
	
	private boolean compactLots = true;
	
	public LegalSKLDIterator(long searchId, DataSite dataSite) {
		super(searchId, dataSite);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<LegalSKLDIteratorEntry> createDerrivations() {
		Search global = getSearch();
		List<LegalSKLDIteratorEntry> derivations = (List<LegalSKLDIteratorEntry>) global.getAdditionalInfo("LegalSKLDIteratorList"); 
		
		if(derivations == null) {
			
			String lotSP = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			String platBookSP = global.getSa().getAtribute(SearchAttributes.LD_BOOKNO);
			String platPageSP = global.getSa().getAtribute(SearchAttributes.LD_PAGENO);
			String blockSP = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			
			derivations = new Vector<LegalSKLDIteratorEntry>();
			Map<PlatBookPage, Set<String>> legalSources = new HashMap<PlatBookPage, Set<String>>();

			DocumentsManagerI documentsManagerI = global.getDocManager();
			try {
				documentsManagerI.getAccess();
				
				List<DocumentI> listRodocs = getGoodDocumentsOrForCurrentOwner(documentsManagerI,global,true);
				
				if(listRodocs==null||listRodocs.size()==0){
					listRodocs = getGoodDocumentsOrForCurrentOwner(documentsManagerI, global, false);
				}
				
				if(listRodocs==null||listRodocs.size()==0){
					listRodocs = documentsManagerI.getDocumentsWithDataSource(true, getDataSite().getSiteTypeAbrev());
				}
				
				boolean addedPlattedFromSearchPage = false;
				
				
				for (LotInterval interval: LotMatchAlgorithm.prepareLotInterval(lotSP)) {
					
					List<String> lotList = interval.getLotList();
					for (String lot : lotList) {
						
						LegalSKLDIteratorEntry legalSKLDIteratorEntry = new LegalSKLDIteratorEntry();
						legalSKLDIteratorEntry.setMapIdBook(platBookSP);
						legalSKLDIteratorEntry.setMapIdPage(platPageSP);
						legalSKLDIteratorEntry.setBlock(blockSP);
						legalSKLDIteratorEntry.setLot(lot);
						
						if(legalSKLDIteratorEntry.isSubdivisionComplete()) {
							if(!derivations.contains(legalSKLDIteratorEntry)) {
								derivations.add(legalSKLDIteratorEntry);
								addedPlattedFromSearchPage = true;
								PlatBookPage keyForLegal = new PlatBookPage(
										legalSKLDIteratorEntry.getMapIdBook().trim(), 
										legalSKLDIteratorEntry.getMapIdPage().trim());;
								Set<String> foundSources = legalSources.get(keyForLegal);
								if(foundSources == null) {
									foundSources = new HashSet<String>();
									legalSources.put(keyForLegal, foundSources);
								}
								foundSources.add("Search Page");
							}
						}
					}
					
				}
				
				if(!addedPlattedFromSearchPage && global.getSa().isCondo()) {
					String unit = global.getSa().getAtribute(SearchAttributes.P_STREETUNIT);
					if(StringUtils.isNotEmpty(unit)) {
						String building = null;
						
						Matcher matcher = LETTERS_NUMBERS_PATTERN.matcher(unit);
						if(matcher.matches()) {
							unit = matcher.group(2);
							building = matcher.group(1);
						} else {
							building = "";
						}
						
						LegalSKLDIteratorEntry legalSKLDIteratorEntry = new LegalSKLDIteratorEntry();
						legalSKLDIteratorEntry.setMapIdBook(global.getSa().getAtribute(SearchAttributes.LD_BOOKNO));
						legalSKLDIteratorEntry.setMapIdPage(global.getSa().getAtribute(SearchAttributes.LD_PAGENO));
						legalSKLDIteratorEntry.setBlock(building);
						legalSKLDIteratorEntry.setLot(unit);
						
						if(legalSKLDIteratorEntry.isSubdivisionComplete()) {
							if(!derivations.contains(legalSKLDIteratorEntry)) {
								derivations.add(legalSKLDIteratorEntry);
								
								PlatBookPage keyForLegal = new PlatBookPage(
										legalSKLDIteratorEntry.getMapIdBook().trim(), 
										legalSKLDIteratorEntry.getMapIdPage().trim());;
								Set<String> foundSources = legalSources.get(keyForLegal);
								if(foundSources == null) {
									foundSources = new HashSet<String>();
									legalSources.put(keyForLegal, foundSources);
								}
								foundSources.add("Search Page");
							}
						}
					
					}
				}
				
				for (DocumentI documentI : listRodocs) {
					if(!documentI.isOneOf(
							DocumentTypes.PLAT,
							DocumentTypes.RESTRICTION,
							DocumentTypes.EASEMENT,
							DocumentTypes.MASTERDEED,
							DocumentTypes.COURT,
							DocumentTypes.LIEN,
							DocumentTypes.CORPORATION,
							DocumentTypes.AFFIDAVIT, 
							DocumentTypes.CCER)) {
						for (PropertyI propertyI : documentI.getProperties()) {
							SubdivisionI subdivisionI = propertyI.getLegal().getSubdivision();
							LegalSKLDIteratorEntry legalSKLDIteratorEntry = new LegalSKLDIteratorEntry();
							legalSKLDIteratorEntry.setSubdivision(subdivisionI);
							if(legalSKLDIteratorEntry.isSubdivisionComplete()) {
								if(!derivations.contains(legalSKLDIteratorEntry)) {
									derivations.add(legalSKLDIteratorEntry);
									
									PlatBookPage keyForLegal = new PlatBookPage(
											legalSKLDIteratorEntry.getMapIdBook().trim(), 
											legalSKLDIteratorEntry.getMapIdPage().trim());
									Set<String> foundSources = legalSources.get(keyForLegal);
									if(foundSources == null) {
										foundSources = new HashSet<String>();
										legalSources.put(keyForLegal, foundSources);
									}
									foundSources.add(documentI.prettyPrint());
									
									if(legalSKLDIteratorEntry.isSubdivisionComplete()) {
										if(!derivations.contains(legalSKLDIteratorEntry)) {
											derivations.add(legalSKLDIteratorEntry);
										}
									}
									
								}
							}					
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				documentsManagerI.releaseAccess();
			}
			
			
			//if(global.getSa().isUpdate()) {
			Set<PlatBookPage> platBookPageFromUser = new HashSet<PlatBookPage>();
			try {
				DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
				long miServerId = dataSite.getServerId();
				for (LegalI legal : global.getSa().getForUpdateSearchLegalsNotNull(miServerId)) {
					SubdivisionI subdivisionI = legal.getSubdivision();
					LegalSKLDIteratorEntry legalSKLDIteratorEntry = new LegalSKLDIteratorEntry();
					legalSKLDIteratorEntry.setSubdivision(subdivisionI);
					if(legalSKLDIteratorEntry.isSubdivisionComplete()) {
						if(!derivations.contains(legalSKLDIteratorEntry)) {
							derivations.add(legalSKLDIteratorEntry);
							platBookPageFromUser.add(new PlatBookPage(
									legalSKLDIteratorEntry.getMapIdBook().trim(), 
									legalSKLDIteratorEntry.getMapIdPage().trim()));
							PlatBookPage keyForLegal = new PlatBookPage(
									legalSKLDIteratorEntry.getMapIdBook().trim(), 
									legalSKLDIteratorEntry.getMapIdPage().trim());
							Set<String> foundSources = legalSources.get(keyForLegal);
							if(foundSources == null) {
								foundSources = new HashSet<String>();
								legalSources.put(keyForLegal, foundSources);
							}
							foundSources.add("Legal Saved From Parent Site as Search Parameters");
							
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error loading names for Update saved from Parent Site", e);
			}
			
			
			Set<PlatBookPage> foundKeys = getDifferentLegals(derivations);
			if( foundKeys.size() > 1 ){
				
				PlatBookPage orderPlatBP = new PlatBookPage(
						global.getSa().getValidatedPlatBook(), 
						global.getSa().getValidatedPlatPage());
				
				
				if(!orderPlatBP.isEmpty()) {
					platBookPageFromUser.add(orderPlatBP);
				}
				
				
				String[] multipleLegalsLogging = getMultipleLegalSources(legalSources, foundKeys);
				
				if(!platBookPageFromUser.isEmpty()) {
					List<LegalSKLDIteratorEntry> onlyWithPlat = getOnlyStructuresWithPlat(derivations, platBookPageFromUser);	
					if(onlyWithPlat.isEmpty()) {
						global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
						global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegalsLogging[1]);
						SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " + 
								multipleLegalsLogging[0] + " (no valid plat book-page iteration from order or saved)<br><div>" , searchId);
					} else {
						SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " 
								+ multipleLegalsLogging[0] + ", but continuing automatic search only with " 
								+ Arrays.toString(platBookPageFromUser.toArray()) + " (from order and/or saved)<br><div>" , searchId);
						derivations.clear();
						derivations.addAll(onlyWithPlat);
					}
				} else {
					
					List<LegalSKLDIteratorEntry> validatedDerivations = getOnlyStructuresMatchingValidatedData(derivations);
					//from validated we need to check if we still have multiple legal
					
					if(validatedDerivations.isEmpty()) {
						global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND, "true");
						global.getSa().setAtribute(SearchAttributes.ATS_MULTIPLE_LEGAL_INSTRUMENTS, multipleLegalsLogging[1]);
						SearchLogger.info("</div><br/><b>Questionable multiple legals found</b> " 
								+ multipleLegalsLogging[0] + " (failed validation with search page info and no plat book-page from order or saved available)<br><div>" , searchId);
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
							derivations.clear();
							derivations.addAll(validatedDerivations);
						}
					}
				}
			} else {
				if( !derivations.isEmpty()) {
					
					LegalSKLDIteratorEntry legalSKLDIteratorEntry = null;
					
					String platBook = getSearchAttribute(SearchAttributes.LD_BOOKNO);
					String platPage = getSearchAttribute(SearchAttributes.LD_PAGENO);
					
					Set<String> lots = new HashSet<String>();
					Set<String> blocks = new HashSet<String>();
					
					if(StringUtils.isEmpty(platPage) || StringUtils.isEmpty(platBook)) {
						
						boolean updatedPlatBookPage = false;
						
						LegalSKLDIteratorEntry[] array = derivations.toArray(new LegalSKLDIteratorEntry[derivations.size()]);
						
						for (LegalSKLDIteratorEntry someStruct : array) {
							if( StringUtils.isNotEmpty(someStruct.getMapIdBook()) && 
									StringUtils.isNotEmpty(someStruct.getMapIdPage())) {
								if(!updatedPlatBookPage) {
									global.getSa().setAtribute(SearchAttributes.LD_BOOKNO, someStruct.getMapIdBook());
									global.getSa().setAtribute(SearchAttributes.LD_PAGENO, someStruct.getMapIdPage());
									updatedPlatBookPage = true;
									
									for (LotInterval interval: LotMatchAlgorithm.prepareLotInterval(lotSP)) {
										List<String> lotList = interval.getLotList();
										for (String lot : lotList) {
											
											/*
											 * Add an iteration with new things added in search page
											 */
											legalSKLDIteratorEntry = new LegalSKLDIteratorEntry();
											legalSKLDIteratorEntry.setMapIdBook(platBookSP);
											legalSKLDIteratorEntry.setMapIdPage(platPageSP);
											legalSKLDIteratorEntry.setBlock(blockSP);
											
											legalSKLDIteratorEntry.setLot(lot);

											if(legalSKLDIteratorEntry.isSubdivisionComplete()  && !derivations.contains(legalSKLDIteratorEntry)) {
												//good legal
												derivations.add(legalSKLDIteratorEntry);
											} else {
												//bad legal
												legalSKLDIteratorEntry = null;
											}
										}
										
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
							
							if(legalSKLDIteratorEntry != null) {
								derivations.add(legalSKLDIteratorEntry);
							}
							
						}
						
					}
				}
			}
			
			if(isCompactLots()) {
				derivations = compactLots(derivations);
			}
			
			global.setAdditionalInfo("LegalSKLDIteratorList", derivations);
		
		}
		
		return derivations;
	}
	
	/**
	 * Used to compact lots, for example for multiple lots like 15,16,17 we will only search once with lot low 15 and lot high 17
	 * @param derivations original derivation list
	 * @return a list with new derivations 
	 */
	private List<LegalSKLDIteratorEntry> compactLots(List<LegalSKLDIteratorEntry> derivations) {
		
		List<LegalSKLDIteratorEntry> compactedDerivations = new ArrayList<LegalSKLDIteratorEntry>();
		
		Map<LegalSKLDIteratorEntry, List<String>> intermediary = new HashMap<LegalSKLDIteratorEntry, List<String>>();
		for (LegalSKLDIteratorEntry legalSKLDIteratorEntry : derivations) {
			LegalSKLDIteratorEntry tempEntry = new LegalSKLDIteratorEntry();
			tempEntry.setBlock(legalSKLDIteratorEntry.getBlock());
			tempEntry.setMapIdBook(legalSKLDIteratorEntry.getMapIdBook());
			tempEntry.setMapIdPage(legalSKLDIteratorEntry.getMapIdPage());
			
			List<String> lots = intermediary.get(tempEntry);
			if(lots == null) {
				lots = new ArrayList<String>();
				intermediary.put(tempEntry, lots);
			}
			if(StringUtils.isNotEmpty(legalSKLDIteratorEntry.getLot())) {
				String low = legalSKLDIteratorEntry.getLot();
				String high = legalSKLDIteratorEntry.getLotHigh();
				lots.add(low);
				if(StringUtils.isNotEmpty(high)) {
					if(low.matches("\\d+") && high.matches("\\d+")) {
						int lowInt = Integer.parseInt(low);
						int highInt = Integer.parseInt(high);
						for (int i = lowInt + 1; i <= highInt; i++) {
							lots.add(Integer.toString(i));
						}
						
					} else {
						lots.add(high);
					}
				}
			} else {
				//only plat bp and block
				compactedDerivations.add(legalSKLDIteratorEntry);
			}
			
		}
		
		
		
		for (LegalSKLDIteratorEntry legalSKLDIteratorEntry : intermediary.keySet()) {
			
			List<String> lots = intermediary.get(legalSKLDIteratorEntry);
			List<Interval> intervals = StringUtils.compactEnumerationToInterval(lots);
			
			for (Interval interval : intervals) {
				LegalSKLDIteratorEntry clone = legalSKLDIteratorEntry.clone();
				
				if(clone != null && interval != null) {
					if(StringUtils.isNotEmpty(interval.getLow())) {
						clone.setLot(interval.getLow());
					}
					if(StringUtils.isNotEmpty(interval.getHigh())) {
						clone.setLotHigh(interval.getHigh());
					}
					compactedDerivations.add(clone);	
				}
			}
		}
		
		return compactedDerivations;
	}

	private String[] getMultipleLegalSources(Map<PlatBookPage, Set<String>> legalSources, Set<PlatBookPage> foundKeys) {
		String[] result = new String[2];
		for (PlatBookPage foundKey : foundKeys) {
			if (result[0] != null) {
				result[0] += "; Key: [" + foundKey + "]";
			} else {
				result[0] = "Key: [" + foundKey + "]";
			}
			if(legalSources != null) {
				Set<String> foundSourcesForKey = legalSources.get(foundKey);
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
		}
		return result;
	}
	
	private List<LegalSKLDIteratorEntry> getOnlyStructuresWithPlat(List<LegalSKLDIteratorEntry> derivations, Set<PlatBookPage> platBookPageFromUser ) {
		List<LegalSKLDIteratorEntry> modifiedLegalStruct = new ArrayList<LegalSKLDIteratorEntry>();
		for (PlatBookPage platBookPage : platBookPageFromUser) {
			if(!platBookPage.isEmpty()) {
				for (LegalSKLDIteratorEntry struct : derivations) {
					if(platBookPage.getBook().equalsIgnoreCase(struct.getMapIdBook())
							&& platBookPage.getPage().equalsIgnoreCase(struct.getMapIdPage())) {
						modifiedLegalStruct.add(struct);
					}
				}
			}
		}
		return modifiedLegalStruct;
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
	private List<LegalSKLDIteratorEntry> getOnlyStructuresMatchingValidatedData(List<LegalSKLDIteratorEntry> derivations) {
		List<LegalSKLDIteratorEntry> modifiedLegalStruct = new ArrayList<LegalSKLDIteratorEntry>();
		
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

	private void validateDerivationsInternal(
			List<LegalSKLDIteratorEntry> derivations,
			List<LegalSKLDIteratorEntry> modifiedLegalStruct, String lotOrUnit,
			String blockOrBuilding) {
		for (LegalSKLDIteratorEntry struct : derivations) {
			
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
	
	
	private Set<PlatBookPage> getDifferentLegals(List<LegalSKLDIteratorEntry> derivations) {
		Set<PlatBookPage> foundKeys = new HashSet<PlatBookPage>();
		for (LegalSKLDIteratorEntry personalDataStruct : derivations) {
			if(StringUtils.isNotEmpty(personalDataStruct.getMapIdBook()) 
					&& StringUtils.isNotEmpty(personalDataStruct.getMapIdPage())) {
				PlatBookPage pbp = new PlatBookPage(personalDataStruct.getMapIdBook().trim(), personalDataStruct.getMapIdPage().trim());
				if(!foundKeys.contains(pbp)) {
					foundKeys.add(pbp);	
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

	protected List<DocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch){
		final List<DocumentI> ret = new ArrayList<DocumentI>();
		
		List<DocumentI> listRodocs = m.getDocumentsWithDataSource(true, getDataSite().getSiteTypeAbrev());
		DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
		
		SearchAttributes sa	= search.getSa();
		PartyI owner 		= sa.getOwners();
		
		
		for(DocumentI docI:listRodocs){
			RegisterDocumentI doc = (RegisterDocumentI)docI;

			for(PropertyI prop:doc.getProperties()){
				if((doc.isOneOf(DocumentTypes.MORTGAGE, DocumentTypes.TRANSFER, DocumentTypes.RELEASE)&& applyNameMatch)
						|| (doc.isOneOf(DocumentTypes.MORTGAGE, DocumentTypes.TRANSFER) && !applyNameMatch)){
					if(prop.hasSubdividedLegal()){
						SubdivisionI sub = prop.getLegal().getSubdivision();
						boolean nameMatched = false;						
						if(applyNameMatch){
							if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
									GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						if( (nameMatched||!applyNameMatch) && 
								(StringUtils.isNotEmpty(sub.getLot()) || StringUtils.isNotEmpty(sub.getBlock()) ) &&  
								StringUtils.isNotEmpty(sub.getPlatBook()) && 
								StringUtils.isNotEmpty(sub.getPlatPage()) ){
							ret.add(doc);
							break;
						}
					}
				}
			}
		}
	
		
		return ret;
	}
	
	
	

	@Override
	protected void loadDerrivation(TSServerInfoModule module,
			LegalSKLDIteratorEntry state) {
		if(state.isSubdivisionComplete()) {
			for (Object functionObject : module.getFunctionList()) {
				if (functionObject instanceof TSServerInfoFunction) {
					TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
					if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE) {
						function.setParamValue(state.getMapIdBook());
					} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE) {
						function.setParamValue(state.getMapIdPage());
					} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_LOT) {
						module.setData(1, state.getLot());
						if (StringUtils.isNotEmpty(state.getLotHigh())) {
							module.setData(2, state.getLotHigh());
							module.setData(4, state.getBlock());
						}
					} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BLOCK) {
						function.setParamValue(state.getBlock());
					}
				}
			}
		}
		
	}

	public boolean isCompactLots() {
		return compactLots;
	}

	public void setCompactLots(boolean compactLots) {
		this.compactLots = compactLots;
	}
	
}



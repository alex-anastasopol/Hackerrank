package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;

/**
 * This class should be used for new RO-like sites<br>
 * It contains methods and settings specific for RO-like sites
 * @author Andrei
 *
 */
public abstract class TSServerROLike extends TSServer implements TSServerROLikeI {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TSServerROLike(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public TSServerROLike(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
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
	        	"' } \"> Save with cross-references" + addRestrictionToolTip() + "<br>\r\n" +
	        	"<input type=\"checkbox\" name=\"" + RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS + 
	        			"\" id=\"" + RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS + 
	        			"\" title=\"Save search parameters from selected document(s) for further use\" > Save with search parameters<br>\r\n" + 
        		"<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" " +"onclick=\"javascript:submitForm();\" >\r\n";
    	}
        
        
        return s+"</form>\n";
	}
	
	public String addRestrictionToolTip(){
		return "";
	}
	public String getKeyForSavingInIntermediary(String input) {
		return AdditionalInfoKeys.MODULE_PREFIX_KEY + "_" + miServerID + "_" + input;
	}
	public String getKeyForSavingInIntermediaryNextLink(String input) {
		return AdditionalInfoKeys.MODULE_PREFIX_KEY + "_" + miServerID + "_" + input;
	}
	public String getKeyForSavingInFinalResults(String input) {
		return AdditionalInfoKeys.MODULE_PREFIX_KEY + "_" + miServerID + "_" + input;
	}
	
	/**
	 * @param doc <- the document 
	 * @return returns instrument number by default, override it if the documents don't have any instrument number
	 */
	
	protected String getInstrumentNumberForSavingInFinalResults(DocumentI doc){
		return doc.getInstno();
	}
	
	@Override
	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(
			ServerResponse response, String htmlContent, boolean forceOverritten) {

		ADD_DOCUMENT_RESULT_TYPES result = super.addDocumentInATS(response, htmlContent, forceOverritten);
		
		try {
			if(response.isParentSiteSearch() && result.equals(ADD_DOCUMENT_RESULT_TYPES.ADDED)) {
				Object possibleModule = getSearch().getAdditionalInfo(
						getKeyForSavingInFinalResults(getInstrumentNumberForSavingInFinalResults(response.getParsedResponse().getDocument())));
				Object possibleFlagForSavingSearchParams = response.getParsedResponse().getAttribute(RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS);
				if(Boolean.TRUE.equals(possibleFlagForSavingSearchParams) && 
						possibleModule != null && possibleModule instanceof TSServerInfoModule) {
					TSServerInfoModule originalSourceModule = (TSServerInfoModule)possibleModule;
					
					if(ArrayUtils.contains(getModuleIdsForSavingLegal(), originalSourceModule.getModuleIdx())) {
						RegisterDocumentI registerDocumentI = (RegisterDocumentI) response.getParsedResponse().getDocument();
						
						Set<LegalI> allLegalsToCheck  = new HashSet<LegalI>();
						for (PropertyI propertyI : registerDocumentI.getProperties()) {
							if(propertyI.hasLegal()) {
								allLegalsToCheck.add(propertyI.getLegal());
							}
						}
						
						List<LegalI> newLegalsFound = findLegalsForUpdate(originalSourceModule, allLegalsToCheck);
						
						if(newLegalsFound == null) {
							//do it old style
							for (PropertyI propertyI : registerDocumentI.getProperties()) {
								
								if(propertyI.hasLegal()){
					    			LegalI legal = propertyI.getLegal();
					    			String fullLegal = null;
					    			if(legal.hasTownshipLegal()){
					    				String temp = legal.getTownShip().shortFormString();
					    				if(StringUtils.isNotEmpty(temp)) {
					    					fullLegal = temp;
					    				}
					    			}
					    			if(legal.hasSubdividedLegal()){
					    				String temp = legal.getSubdivision().shortFormString();
						        		if(StringUtils.isNotEmpty(temp)) {
						        			if(StringUtils.isNotEmpty(fullLegal)) {
						        				fullLegal = temp + " | " + fullLegal;
						        			} else {
						        				fullLegal = temp;
						        			}
					    				}
					    			}
					    			getSearchAttributes().addForUpdateSearchLegal(propertyI.getLegal(), getServerID());
									SearchLogger.info("<br><font color='green'><b>Saving</b></font> legal: [" + fullLegal +  "] from document " + registerDocumentI.prettyPrint() + " for future automatic search<br>", searchId);
					    		}
							}
						} else {
							// do it new way
							List<LegalI> alreadySavedLegals = getSearchAttributes().getForUpdateSearchLegalsNotNull(getServerID());

							StringBuilder fullLegal = new StringBuilder();
							for (LegalI legalI : newLegalsFound) {
								
								ArrayList<LegalI> alreadySavedSubdividedLegal = new ArrayList<LegalI>();
								
								ArrayList<LegalI> alreadySavedTownshipLegal = new ArrayList<LegalI>(); 
								
								for (LegalI alreadySavedLegal : alreadySavedLegals) {
															
									if(alreadySavedLegal.hasSubdividedLegal()  ){
										alreadySavedSubdividedLegal.add(alreadySavedLegal);
									}
									if (alreadySavedLegal.hasTownshipLegal()){
										alreadySavedTownshipLegal.add(alreadySavedLegal);
									}
									
								}
								
								boolean addSubdivision = alreadySavedSubdividedLegal.isEmpty();
								boolean addTownship = alreadySavedTownshipLegal.isEmpty();
									
								if(legalI.hasSubdividedLegal() && !addSubdivision){
									for (LegalI alreadySavedLegal : alreadySavedSubdividedLegal)
										if(!alreadySavedLegal.getSubdivision().equals(legalI.getSubdivision())) {
											addSubdivision = true;
										} else{
											addSubdivision = false;
											break;
										}
								}
								
								if (legalI.hasTownshipLegal() && !addTownship){
									for (LegalI alreadySavedLegal : alreadySavedTownshipLegal)
										if(!alreadySavedLegal.getTownShip().equals(legalI.getTownShip())) {
											addTownship = true;
										} else{
											addTownship = false;
											break;
										}
								}
								
								if(addSubdivision && legalI.hasSubdividedLegal()) {
									LegalI toAdd = new Legal();
									toAdd.setSubdivision(legalI.getSubdivision());
									if(fullLegal.length() > 0) {
										fullLegal.append(" | ");
									}
									fullLegal.append(legalI.getSubdivision().shortFormString());
									alreadySavedLegals.add(toAdd);
									getSearchAttributes().addForUpdateSearchLegal(toAdd, getServerID());
								}
								
								if(addTownship && legalI.hasTownshipLegal()) {
									LegalI toAdd = new Legal();
									toAdd.setTownShip(legalI.getTownShip());
									if(fullLegal.length() > 0) {
										fullLegal.append(" | ");
									}
									fullLegal.append(legalI.getTownShip().shortFormString());
									alreadySavedLegals.add(toAdd);
									getSearchAttributes().addForUpdateSearchLegal(toAdd, getServerID());
								}
							}
							
							if(fullLegal.length() == 0) {
								SearchLogger.info("<br><font color='red'>NO</font> legal was saved from document " + registerDocumentI.prettyPrint() + " for future automatic search<br>", searchId);
							} else {
								SearchLogger.info("<br><font color='green'><b>Saving</b></font> legal: [" + fullLegal.toString() + "] from document " + registerDocumentI.prettyPrint() + " for future automatic search<br>", searchId);
							}
							
							
						}
					} else if(ArrayUtils.contains(getModuleIdsForSavingName(), originalSourceModule.getModuleIdx())) {
						RegisterDocumentI registerDocumentI = (RegisterDocumentI) response.getParsedResponse().getDocument();
						
						Set<NameI> allNamesToCheck  = new HashSet<NameI>(registerDocumentI.getGrantor().getNames());
						allNamesToCheck.addAll(registerDocumentI.getGrantee().getNames());
						
						/**
						 * Not using grantee name just yet, but support is possible
						 * All names will be added to grantor
						 */
						List<NameI> alreadySavedNames = getSearchAttributes().getForUpdateSearchGrantorNamesNotNull(getServerID());
						List<NameI> newNamesFound = findNamesForUpdate(originalSourceModule, allNamesToCheck);
						List<NameI> newNamesAdded = new ArrayList<NameI>();
						
						for (NameI candName : newNamesFound) {
							NameI toAdd = candName;
							String candString =  candName.getFirstName() + candName.getMiddleName() + candName.getSufix() + candName.getLastName();
							for (NameI reference : alreadySavedNames) {
								if(
										GenericNameFilter.isMatchGreaterThenScore(candName, reference, NameFilterFactory.NAME_FILTER_THRESHOLD)
										&&(candName.isCompany()==reference.isCompany())
								) {
									/*
									 * found same name - do not save it
									 */
									String refString = reference.getFirstName() + reference.getMiddleName() + reference.getSufix() + reference.getLastName();
									if(refString.length() <= candString.length()){
										if(newNamesAdded.contains(reference)) {
											newNamesAdded.remove(reference);
										}
										
										reference.setLastName(candName.getLastName());
										reference.setFirstName(candName.getFirstName());
										reference.setMiddleName(candName.getMiddleName());
										reference.setCompany(candName.isCompany());
										reference.setSufix(candName.getSufix());
										reference.setPrefix(candName.getPrefix());
										
										newNamesAdded.add(reference);
										
										toAdd = null;
										break;	//no need to check other cases
									}
								} 
							}
							if(toAdd != null) {
								alreadySavedNames.add(toAdd);
								newNamesAdded.add(toAdd);
							}
						}
						
						if(newNamesAdded.size() == 0) {
							SearchLogger.info("<br><font color='red'>NO</font> name was saved from document " + registerDocumentI.prettyPrint() + " for future automatic search<br>", searchId);
						} else {
							for (NameI nameI : newNamesAdded) {
								SearchLogger.info("<br><font color='green'><b>Saving</b></font> name: [" + nameI.getFullName() + "] from document " + registerDocumentI.prettyPrint() + " for future automatic search<br>", searchId);
							}
						}
						
						
						
					} else if(ArrayUtils.contains(getModuleIdsForSavingAddress(), originalSourceModule.getModuleIdx())) {
						RegisterDocumentI registerDocumentI = (RegisterDocumentI) response.getParsedResponse().getDocument();
						for (PropertyI propertyI : registerDocumentI.getProperties()) {
							if(propertyI.hasAddress()) {
								getSearchAttributes().addForUpdateSearchAddress(propertyI.getAddress(), getServerID());
								SearchLogger.info("<br><font color='green'><b>Saving</b></font> address: [" + 
										propertyI.getAddress().shortFormString() +  "] from document " + registerDocumentI.prettyPrint() + " for future automatic search<br>", searchId);
							}
						}
						
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while saving data for Update for " + searchId, e);
		}
		return result;
	}
	
	protected int[] getModuleIdsForSavingLegal() {
		return new int[]{TSServerInfo.SUBDIVISION_MODULE_IDX};
	}
	protected int[] getModuleIdsForSavingAddress() {
		return new int[]{TSServerInfo.ADDRESS_MODULE_IDX};
	}
	protected int[] getModuleIdsForSavingName() {
		return new int[]{TSServerInfo.NAME_MODULE_IDX};
	}

	/**
	 * Must return a NameI object that represents the name used in this module when searching
	 * @param module the original module used in parent site 
	 * @return the name as an object (com.stewart.ats.base.name.NameI)
	 */
	protected NameI getNameFromModule(TSServerInfoModule module) {
		return null;
	}
	
	/**
	 * Finds the names that should be saved from this module
	 * @param module
	 * @param candidates
	 * @return names to be saved or empty list
	 */
	protected List<NameI> findNamesForUpdate(TSServerInfoModule module, Set<NameI> candidates) {
		
		GenericNameFilter filter = new GenericNameFilter(searchId);
		NameI reference = getNameFromModule(module);
		if(reference == null) {
			return new ArrayList<NameI>();
		}
		filter.getRefNames().add(reference);
		filter.setIgnoreMiddleOnEmpty(true);
		filter.setStrategyType(FilterResponse.STRATEGY_TYPE_HYBRID);
		filter.setThreshold(ATSDecimalNumberFormat.ONE);
		filter.setUseSynonymsBothWays(true);
		filter.setUseSynonymsForCandidates(true);
		filter.init();
		
		TreeMap<String, List<NameI>> map = new TreeMap<String, List<NameI>>(); 
		
		for (NameI nameI : candidates) {
			
			NameI nameClone = nameI.clone();
			if(!reference.isCompany()) {
				if(StringUtils.isEmpty(reference.getFirstName())) {
					nameClone.setFirstName("");
				}
				if(StringUtils.isEmpty(reference.getMiddleName())) {
					nameClone.setMiddleName("");
				}
			}
			
			BigDecimal score = filter.getScoreForName(nameClone);
			
			if(score.doubleValue() >= NameFilterFactory.NAME_FILTER_THRESHOLD_FOR_HYBRID) {
				String scoreFormated = ATSDecimalNumberFormat.format(score);
				List<NameI> names = map.get(scoreFormated);
				if(names == null) {
					names = new ArrayList<NameI>();
					map.put(scoreFormated, names);
				}
				names.add(nameI);
			}
		}
		
		if(map.size() > 0) {
			return map.get(map.lastKey());
		}
		
		return new ArrayList<NameI>();
	}
	
	/**
	 * Must return a LegalI object that represents the legal used in this module when searching
	 * @param module the original module used in parent site 
	 * @return the name as an object (com.stewart.ats.base.name.NameI)
	 */
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		return null;
	}
	
	protected AddressI getAddressFromModule(TSServerInfoModule module) {		
		return null;
	}
	
	protected List<LegalI> findLegalsForUpdate(TSServerInfoModule module, Set<LegalI> allLegalsToCheck) {
		LegalI reference = getLegalFromModule(module);
		if(reference == null) {
			return null;
		}
		
		List<LegalI> goodLegals = new ArrayList<LegalI>();
		
		if(reference.hasSubdividedLegal()) {
			SubdivisionI refSubdivision = reference.getSubdivision();
			for (LegalI legalToCheck : allLegalsToCheck) {
				if(legalToCheck.hasSubdividedLegal()) {
					SubdivisionI candSubdivision = legalToCheck.getSubdivision();
					boolean hasRefPlatBook = false;
					if(StringUtils.isNotEmpty(refSubdivision.getPlatBook())) {
						hasRefPlatBook = true;
						if(StringUtils.isNotEmpty(candSubdivision.getPlatBook())
								&& !refSubdivision.getPlatBook().equals(candSubdivision.getPlatBook())) {
							continue;
						}
					}
					
					boolean hasRefPlatPage = false;
					if(StringUtils.isNotEmpty(refSubdivision.getPlatPage())) {
						if(StringUtils.isNotEmpty(candSubdivision.getPlatPage())
								&& !refSubdivision.getPlatPage().equals(candSubdivision.getPlatPage())) {
							continue;
						}
						hasRefPlatPage = true;
					}
					
					if(!hasRefPlatBook 
							&& !hasRefPlatPage
							&& StringUtils.isNotEmpty(refSubdivision.getName()) &&
							!candSubdivision.getName().startsWith(refSubdivision.getName())) {
						continue;
					}
					
					if(StringUtils.isNotEmpty(refSubdivision.getLot())) {
						if(StringUtils.isNotEmpty(candSubdivision.getLot())) {
							if(!refSubdivision.getLot().equals(candSubdivision.getLot())) {
								if(StringUtils.isNotEmpty(refSubdivision.getLotThrough())) {
									try {
										int startLot = Integer.parseInt(refSubdivision.getLot().trim());
										int endLot = Integer.parseInt(refSubdivision.getLotThrough().trim());
										int candLot = Integer.parseInt(candSubdivision.getLot().trim());
										if(candLot >= startLot && candLot <= endLot) {
											//same lot, so let the next test
										} else {
											//lot is different
											continue;
										}
									} catch (Exception e) {
										logger.error("Error while trying to get lot interval", e);
										//lot is different
										continue;
									}
								} else {
									//lot is different
									continue;
								}
							} else {
								//same lot, so let the next test
							}
						}
					}
					
					if(StringUtils.isNotEmpty(refSubdivision.getBlock())) {
						if(StringUtils.isNotEmpty(candSubdivision.getBlock())) {
							if(!refSubdivision.getBlock().equals(candSubdivision.getBlock())) {
								if(StringUtils.isNotEmpty(refSubdivision.getBlockThrough())) {
									try {
										int startBlock = Integer.parseInt(refSubdivision.getBlock().trim());
										int endBlock = Integer.parseInt(refSubdivision.getBlockThrough().trim());
										int candBlock = Integer.parseInt(candSubdivision.getBlock().trim());
										if(candBlock >= startBlock && candBlock <= endBlock) {
											//same block, so let the next test
										} else {
											//block is different
											continue;
										}
									} catch (Exception e) {
										logger.error("Error while trying to get block interval", e);
										//block is different
										continue;
									}
								} else {
									//block is different
									continue;
								}
							} else {
								//same block, so let the next test
							}
						}
					}
					LegalI newLegalToAdd = new Legal();
					newLegalToAdd.setSubdivision(candSubdivision);
					//be sure we only add plated legal since the search was plated
					goodLegals.add(newLegalToAdd);
				}
			}
		}
		if(reference.hasTownshipLegal()) {

			TownShipI refTownship = reference.getTownShip();
			for (LegalI legalToCheck : allLegalsToCheck) {
				if(legalToCheck.hasTownshipLegal()) {
					TownShipI candTownship = legalToCheck.getTownShip();
					
					if(StringUtils.isNotEmpty(refTownship.getSection()) 
							&& StringUtils.isNotEmpty(candTownship.getSection())
							&& !refTownship.getSection().equals(candTownship.getSection())) {
						continue;
					}
					
					if(StringUtils.isNotEmpty(refTownship.getTownship()) 
							&& StringUtils.isNotEmpty(candTownship.getTownship())
							&& !refTownship.getTownship().equals(candTownship.getTownship())) {
						continue;
					}
					
					if(StringUtils.isNotEmpty(refTownship.getRange()) 
							&& StringUtils.isNotEmpty(candTownship.getRange())
							&& !refTownship.getRange().equals(candTownship.getRange())) {
						continue;
					}
					
					if(StringUtils.isNotEmpty(refTownship.getArb()) 
							&& StringUtils.isNotEmpty(candTownship.getArb())
							&& !refTownship.getArb().equals(candTownship.getArb())) {
						continue;
					}
					
					if(StringUtils.isNotEmpty(refTownship.getAddition()) &&
							!candTownship.getAddition().startsWith(refTownship.getAddition())) {
						continue;
					}
					
					LegalI newLegalToAdd = new Legal();
					newLegalToAdd.setTownShip(candTownship);
					//be sure we only add plated legal since the search was unplated
					goodLegals.add(newLegalToAdd);
				}
			}
		
		}
		
		return goodLegals;
	}
	
	public String saveSearchedParameters(TSServerInfoModule module) {
		
		String globalResult = null;
		
		if(ArrayUtils.contains(getModuleIdsForSavingLegal(), module.getModuleIdx())) {
			
			LegalI legalI = getLegalFromModule(module);
			StringBuilder fullLegal = new StringBuilder();
			
			if(legalI != null) {
			
				List<LegalI> alreadySavedLegals = getSearchAttributes().getForUpdateSearchLegalsNotNull(getServerID());
				
				
				ArrayList<LegalI> alreadySavedSubdividedLegal = new ArrayList<LegalI>();
				
				ArrayList<LegalI> alreadySavedTownshipLegal = new ArrayList<LegalI>(); 
				
				for (LegalI alreadySavedLegal : alreadySavedLegals) {
											
					if(alreadySavedLegal.hasSubdividedLegal()  ){
						alreadySavedSubdividedLegal.add(alreadySavedLegal);
					}
					if (alreadySavedLegal.hasTownshipLegal()){
						alreadySavedTownshipLegal.add(alreadySavedLegal);
					}
					
				}
				
				boolean addSubdivision = alreadySavedSubdividedLegal.isEmpty();
				boolean addTownship = alreadySavedTownshipLegal.isEmpty();
					
				if(legalI.hasSubdividedLegal() && !addSubdivision){
					for (LegalI alreadySavedLegal : alreadySavedSubdividedLegal)
						if(!alreadySavedLegal.getSubdivision().equals(legalI.getSubdivision())) {
							addSubdivision = true;
						} else{
							addSubdivision = false;
						}
				}
				
				if (legalI.hasTownshipLegal() && !addTownship){
					for (LegalI alreadySavedLegal : alreadySavedTownshipLegal)
						if(!alreadySavedLegal.getTownShip().equals(legalI.getTownShip())) {
							addTownship = true;
						} else{
							addTownship = false;
						}
				}
				
				if(addSubdivision && legalI.hasSubdividedLegal()) {
					LegalI toAdd = new Legal();
					toAdd.setSubdivision(legalI.getSubdivision());
					if(fullLegal.length() > 0) {
						fullLegal.append(" | ");
					}
					fullLegal.append(legalI.getSubdivision().shortFormString());
					alreadySavedLegals.add(toAdd);
					getSearchAttributes().addForUpdateSearchLegal(toAdd, getServerID());
				}
				
				if(addTownship && legalI.hasTownshipLegal()) {
					LegalI toAdd = new Legal();
					toAdd.setTownShip(legalI.getTownShip());
					if(fullLegal.length() > 0) {
						fullLegal.append(" | ");
					}
					fullLegal.append(legalI.getTownShip().shortFormString());
					alreadySavedLegals.add(toAdd);
					getSearchAttributes().addForUpdateSearchLegal(toAdd, getServerID());
				}
			}
			
			if(fullLegal.length() == 0) {
				SearchLogger.info("<br><font color='red'>NO</font> legal was saved from searched parameters for future automatic search<br>", searchId);
				globalResult = "NO legal was saved from searched parameters for future automatic search";
			} else {
				SearchLogger.info("<br><font color='green'><b>Saving</b></font> legal: [" + fullLegal.toString() + "] from searched parameters for future automatic search<br>", searchId);
				globalResult = "Saved legal: [" + fullLegal.toString() + "] from searched parameters for future automatic search";
			}
			
		} else if(ArrayUtils.contains(getModuleIdsForSavingName(), module.getModuleIdx())) {
			List<NameI> alreadySavedNames = getSearchAttributes().getForUpdateSearchGrantorNamesNotNull(getServerID());
			List<NameI> newNamesAdded = new ArrayList<NameI>();
			
			NameI candName = getNameFromModule(module);
			if(candName != null) {
				NameI toAdd = candName;
				String candString =  candName.getFirstName() + candName.getMiddleName() + candName.getSufix() + candName.getLastName();
				for (NameI reference : alreadySavedNames) {
					if(
							GenericNameFilter.isMatchGreaterThenScore(candName, reference, NameFilterFactory.NAME_FILTER_THRESHOLD)
							&&(candName.isCompany()==reference.isCompany())
					) {
						/*
						 * found same name - do not save it
						 */
						String refString = reference.getFirstName() + reference.getMiddleName() + reference.getSufix() + reference.getLastName();
						if(refString.length() <= candString.length()){
							if(newNamesAdded.contains(reference)) {
								newNamesAdded.remove(reference);
							}
							
							reference.setLastName(candName.getLastName());
							reference.setFirstName(candName.getFirstName());
							reference.setMiddleName(candName.getMiddleName());
							reference.setCompany(candName.isCompany());
							reference.setSufix(candName.getSufix());
							reference.setPrefix(candName.getPrefix());
							
							newNamesAdded.add(reference);
						}
						toAdd = null;
						break;	//no need to check other cases
					} 
				}
				if(toAdd != null) {
					alreadySavedNames.add(toAdd);
					newNamesAdded.add(toAdd);
				}
			}
			
			if(newNamesAdded.size() == 0) {
				SearchLogger.info("<br><font color='red'>NO</font> name was saved from searched parameters for future automatic search<br>", searchId);
				globalResult = "NO name was saved from searched parameters for future automatic search";
			} else {
				for (NameI nameI : newNamesAdded) {
					SearchLogger.info("<br><font color='green'><b>Saving</b></font> name: [" + nameI.getFullName() + "] from searched parameters for future automatic search<br>", searchId);
					if(globalResult == null) {
						globalResult = "Saving name: [" + nameI.getFullName() + "] from searched parameters for future automatic search";
					} else {
						globalResult += "<br> Saving name: [" + nameI.getFullName() + "] from searched parameters for future automatic search";
					}
				}
			}
			
		} else if(ArrayUtils.contains(getModuleIdsForSavingAddress(), module.getModuleIdx())) {
			AddressI addressI = getAddressFromModule(module);
			if(addressI != null) {
				List<AddressI> alreadySavedAddresses = getSearchAttributes().getForUpdateSearchAddressesNotNull(getServerID());
				if(alreadySavedAddresses.isEmpty()) {
					alreadySavedAddresses.add(addressI);
					SearchLogger.info("<br><font color='green'><b>Saving</b></font> address: [" + addressI.shortFormString() + "] from searched parameters for future automatic search<br>", searchId);
					globalResult = "Saving address: [" + addressI.shortFormString() + "] from searched parameters for future automatic search";
				} else {
					boolean alreadySaved = false;
					for (AddressI savedAddress : alreadySavedAddresses) {
						if(savedAddress.equals(addressI)) {
							alreadySaved = true;
							break;
						}
					}
					if(!alreadySaved) {
						alreadySavedAddresses.add(addressI);
						SearchLogger.info("<br><font color='green'><b>Saving</b></font> address: [" + addressI.shortFormString() + "] from searched parameters for future automatic search<br>", searchId);
						globalResult = "Saving address: [" + addressI.shortFormString() + "] from searched parameters for future automatic search";
					} else {
						SearchLogger.info("<br><font color='red'>NO</font> address was saved from searched parameters for future automatic search<br>", searchId);
						globalResult = "NO address was saved from searched parameters for future automatic search";
					}
				}
			}
		}
		
		return globalResult;
	}
	
	@Override
	public abstract Object getRecoverModuleFrom(RestoreDocumentDataI document);


}

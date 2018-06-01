package ro.cst.tsearch.servers.types;
import java.io.File;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.extractor.xml.XMLUtils;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DASLSimpleInstrumentInfo;
import ro.cst.tsearch.servers.bean.DASLSimplePartyInfo;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.functions.CAGenericDT;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ParsedResponseDateComparator;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.servlet.parentsite.ParentSiteActions;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.templates.GlobalTemplateFactory;
import ro.cst.tsearch.templates.TemplatesException;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.base.address.Address;
import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author cristi stochina
 * @author radu bacrau
 */
@SuppressWarnings("deprecation")
public abstract  class TSServerDASLAdapter extends TSServerDASL {
	
	boolean downloadingForSave = false;
	public static final long serialVersionUID = 10000003478437280L;
	
	// read the rules maximum once per TSServer
	//we do not want this to be serialized
	protected transient Map<Integer,Map<Integer, RecordRules>> cachedRecordRules = new HashMap<Integer, Map<Integer,RecordRules>>();
	
	
	protected TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(
			ServerResponse response, String htmlContent) {
		ParsedResponse parsed = response.getParsedResponse();
		if (parsed != null) {
			DocumentI doc = parsed.getDocument();
			if (doc != null) {
				if (doc instanceof RegisterDocumentI) {
					RegisterDocumentI regDoc = (RegisterDocumentI) doc;
					if ((!regDoc.hasYear() || 1960 == regDoc.getYear())
							&& regDoc.getProperties().size() == 0
							&& regDoc.getGrantee().size() == 0
							&& regDoc.getGrantor().size() == 0
							&& DocumentTypes.MISCELLANEOUS.equalsIgnoreCase(regDoc.getDocType())) {
						DocumentsManagerI man = getSearch().getDocManager();

						try {
							man.getAccess();
							if (regDoc.hasInstrumentNo()) {
								if (man.containsInstrumentNo(regDoc.getInstno())) {
									return TSServer.ADD_DOCUMENT_RESULT_TYPES.ALREADY_EXISTS;
								}
							}
							if (regDoc.hasBookPage()) {
								if (man.containsBookPage(regDoc.getBook(),
										regDoc.getPage())) {
									return TSServer.ADD_DOCUMENT_RESULT_TYPES.ALREADY_EXISTS;
								}
							}
						} finally {
							man.releaseAccess();
						}

					}
				}
			}
		}

		return super.addDocumentInATS(response, htmlContent);
	}
	
	@Override
	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(
			ServerResponse response, String htmlContent, boolean forceOverritten) {

		ADD_DOCUMENT_RESULT_TYPES result = super.addDocumentInATS(response, htmlContent, forceOverritten);
		
		try {
			if(response.isParentSiteSearch() && result.equals(ADD_DOCUMENT_RESULT_TYPES.ADDED)) {
				
				Object possibleModule = getSearch().getAdditionalInfo(
						AdditionalInfoKeys.MODULE_PREFIX_KEY + "_" + miServerID + "_" + response.getParsedResponse().getDocument().prettyPrint());
				
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
						
						if(newLegalsFound != null) {
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
						
						} else {
							for (PropertyI propertyI : registerDocumentI.getProperties()) {
								
								if(propertyI.hasLegal()){
					    			LegalI legal = propertyI.getLegal();
					    			StringBuilder fullLegal = new StringBuilder();
					    			if(legal.hasTownshipLegal()){
					    				String temp = legal.getTownShip().shortFormString();
					    				if(StringUtils.isNotEmpty(temp)) {
					    					if(fullLegal.length() > 0) {
												fullLegal.append(" | ");
											}
					    					fullLegal.append(temp);
					    				}
					    			}
					    			if(legal.hasSubdividedLegal()){
					    				String temp = legal.getSubdivision().shortFormString();
						        		if(StringUtils.isNotEmpty(temp)) {
						        			if(fullLegal.length() > 0) {
												fullLegal.append(" | ");
											}
						        			fullLegal.append(temp);
					    				}
					    			}
					    			getSearchAttributes().addForUpdateSearchLegal(propertyI.getLegal(), getServerID());
									SearchLogger.info("<br><font color='green'><b>Saving</b></font> legal: [" + fullLegal +  
											"] from document " + registerDocumentI.prettyPrint() + 
											" for future automatic search<br>", searchId);
					    		}
							}
						}
					} else if(ArrayUtils.contains(getModuleIdsForSavingName(), originalSourceModule.getModuleIdx())) {
						RegisterDocumentI registerDocumentI = (RegisterDocumentI) response.getParsedResponse().getDocument();

						Set<NameI> allNamesToCheck  = new HashSet<NameI>(registerDocumentI.getGrantee().getNames());
						
						try {
							Object obj = getSearch().getAdditionalInfo("Grantor_FIX_for" + registerDocumentI.getId());
							if (obj != null && obj instanceof Set<?>) {
								Set<?> names = (Set<?>) obj;
								for (Object object : names) {
									allNamesToCheck.add((NameI) object);
								}
							} else {
								allNamesToCheck.addAll(registerDocumentI.getGrantor().getNames());
							}
						} catch (Exception e) {
							logger.error("Internal error while getting grantors", e);
						}
						
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
							SearchLogger.info("<br><font color='red'>NO</font> name was saved from document " + registerDocumentI.prettyPrint() + " for future automatic search<br>", searchId);
						} else {
							for (NameI nameI : newNamesAdded) {
								SearchLogger.info("<br><font color='green'><b>Saving</b></font> name: [" + nameI.getFullName() + "] from document " + registerDocumentI.prettyPrint() + " for future automatic search<br>", searchId);
							}
						}
						
						
						SearchLogger.info("<br>Saving names from document " + registerDocumentI.prettyPrint() + " for future automatic search<br>", searchId);
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
			logger.error("Error while saving data for Update", e);
		}
		return result;
	}
	
	/**
	 * Must return a NameI object that represents the name used in this module when searching
	 * @param module the original module used in parent site 
	 * @return the name as an object (com.stewart.ats.base.name.NameI)
	 */
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 5) {
			String usedFirstName = module.getFunction(2).getParamValue();
			String usedMiddleName = module.getFunction(3).getParamValue();
			String usedLastName = module.getFunction(4).getParamValue();
			
			if(StringUtils.isEmpty(usedLastName)) {
				return null;
			}
			
			/**
			 * There are cases where we have to enter last + first in the last field
			 */
			if(usedLastName.contains(" ") && StringUtils.isEmpty(usedFirstName)) {
				String[] names = null;
				if(NameUtils.isCompany(usedLastName)) {
					names = new String[]{"", "", usedLastName, "", "", ""};
				} else {
					names = StringFormats.parseNameNashville(usedLastName, true);
				}
				
				names = StringFormats.parseNameNashville(usedLastName, true);
				name.setLastName(names[2]);
				name.setFirstName(names[0]);
				name.setMiddleName(names[1]);
			} else {
				name.setLastName(usedLastName);
				name.setFirstName(usedFirstName);
				name.setMiddleName(usedMiddleName);
			}
			return name;
		}
		return null;
	}
	
	/**
	 * Must return a Set<NameI> object that represents all the names used in this module when searching
	 * @param module the original module used in parent site 
	 * @return Set<NameI>
	 */
	protected List<NameI> getNamesFromModule(TSServerInfoModule module) {

		List<TSServerInfoModule> modules = (List<TSServerInfoModule>) this.getSearch().getAdditionalInfo("modulesSearched");

		if (modules == null)
			return null;

		List<NameI> namesList = new ArrayList<NameI>();

		for (TSServerInfoModule mod : modules) {
			NameI name = new Name();
			if (mod.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && mod.getFunctionCount() > 5) {
				String usedFirstName = mod.getFunction(2).getParamValue();
				String usedMiddleName = mod.getFunction(3).getParamValue();
				String usedLastName = mod.getFunction(4).getParamValue();

				if (StringUtils.isEmpty(usedLastName)) {
					continue;
				}

				/**
				 * There are cases where we have to enter last + first in the last field
				 */
				if (usedLastName.contains(" ") && StringUtils.isEmpty(usedFirstName)) {
					String[] names = null;
					if (NameUtils.isCompany(usedLastName)) {
						names = new String[] { "", "", usedLastName, "", "", "" };
					} else {
						names = StringFormats.parseNameNashville(usedLastName, true);
					}

					names = StringFormats.parseNameNashville(usedLastName, true);
					name.setLastName(names[2]);
					name.setFirstName(names[0]);
					name.setMiddleName(names[1]);
				} else {
					name.setLastName(usedLastName);
					name.setFirstName(usedFirstName);
					name.setMiddleName(usedMiddleName);
				}
				namesList.add(name);
			}
		}

		if (namesList.isEmpty())
			return null;
		else
			return namesList;
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
			String scoreFormated = ATSDecimalNumberFormat.format(score);
			List<NameI> names = map.get(scoreFormated);
			if(names == null) {
				names = new ArrayList<NameI>();
				map.put(scoreFormated, names);
			}
			names.add(nameI);
		}
		
		if(map.size() > 0) {
			return map.get(map.lastKey());
		}
		
		return new ArrayList<NameI>();
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
					
					String refLot = refSubdivision.getLot();
					if("ALL".equals(refLot)) {
						refLot = "";
					}
					String candLot = candSubdivision.getLot();
					if("ALL".equals(candLot)) {
						candLot = "";
					}
					
					if(StringUtils.isNotEmpty(refLot)) {
						if(StringUtils.isNotEmpty(candLot)) {
							if(!refLot.equals(candLot)) {
								if(StringUtils.isNotEmpty(refSubdivision.getLotThrough())) {
									try {
										int startLot = Integer.parseInt(refLot.trim());
										int endLot = Integer.parseInt(refSubdivision.getLotThrough().trim());
										int candLotInt = Integer.parseInt(candLot.trim());
										if(candLotInt >= startLot && candLotInt <= endLot) {
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
					
					
					String refBlock = refSubdivision.getBlock();
					if("ALL".equals(refBlock)) {
						refBlock = "";
					}
					String candBlock = candSubdivision.getBlock();
					if("ALL".equals(candBlock)) {
						candBlock = "";
					}
					if(StringUtils.isNotEmpty(refBlock)) {
						if(StringUtils.isNotEmpty(candBlock)) {
							if(!refBlock.equals(candBlock)) {
								if(StringUtils.isNotEmpty(refSubdivision.getBlockThrough())) {
									try {
										int startBlock = Integer.parseInt(refBlock.trim());
										int endBlock = Integer.parseInt(refSubdivision.getBlockThrough().trim());
										int candBlockInt = Integer.parseInt(candBlock.trim());
										if(candBlockInt >= startBlock && candBlockInt <= endBlock) {
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
	
	/**
	 * Must return a LegalI object that represents the legal used in this module when searching
	 * @param module the original module used in parent site 
	 * @return the legal as an object (com.stewart.ats.base.legal.LegalI)
	 */
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		LegalI legal = null;
		
		if(module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX && module.getFunctionCount() > 5) {
			SubdivisionI subdivision = new Subdivision();
			
			subdivision.setLot(module.getFunction(2).getParamValue().trim());
			subdivision.setBlock(module.getFunction(3).getParamValue().trim());
			subdivision.setPlatBook(module.getFunction(4).getParamValue().trim());
			subdivision.setPlatPage(module.getFunction(5).getParamValue().trim());
			if(module.getFunctionCount() > 19) {
				subdivision.setName(module.getFunction(19).getParamValue().trim());
			}
			
			legal = new Legal();
			legal.setSubdivision(subdivision);
		} else if(module.getModuleIdx() == TSServerInfo.SECTION_LAND_MODULE_IDX && module.getFunctionCount() > 4) {
			TownShipI townShip = new TownShip();
			
			townShip.setSection(module.getFunction(0).getParamValue().trim());
			townShip.setTownship(module.getFunction(1).getParamValue().trim());
			townShip.setRange(module.getFunction(2).getParamValue().trim());
			
			try {
				townShip.setQuarterOrder(Integer.parseInt(module.getFunction(3).getParamValue().trim()));
			} catch (Exception e) {}
			townShip.setQuarterValue(module.getFunction(4).getParamValue().trim());
			
			legal = new Legal();
			legal.setTownShip(townShip);
		} else if(module.getModuleIdx() == TSServerInfo.ARB_MODULE_IDX && module.getFunctionCount() > 5) {
			TownShipI townShip = new TownShip();
			
			townShip.setSection(module.getFunction(0).getParamValue().trim());
			townShip.setTownship(module.getFunction(1).getParamValue().trim());
			townShip.setRange(module.getFunction(2).getParamValue().trim());
			
			try {
				townShip.setQuarterOrder(Integer.parseInt(module.getFunction(3).getParamValue().trim()));
			} catch (Exception e) {}
			townShip.setQuarterValue(module.getFunction(4).getParamValue().trim());
			townShip.setArb(module.getFunction(5).getParamValue().trim());
			
			legal = new Legal();
			legal.setTownShip(townShip);
		}
		
		return legal;
	}
	
	/**
	 * Must return an AddressI object that represents the address used in this module when searching
	 * @param module the original module used in parent site 
	 * @return the address as an object (com.stewart.ats.base.address.AddressI)
	 */
	protected AddressI getAddressFromModule(TSServerInfoModule module) {
		AddressI addressI = null;
		
		if(module.getModuleIdx() == TSServerInfo.ADDRESS_MODULE_IDX && module.getFunctionCount() > 3) {
			addressI = new Address();
			addressI.setNumber(module.getFunction(0).getParamValue().trim());
			addressI.setPreDiretion(module.getFunction(1).getParamValue().trim());
			addressI.setStreetName(module.getFunction(2).getParamValue().trim());
		}
		
		return addressI;
	}
	
	
	/**
	 * @author cristi stochina
	 */
	protected static class ServerPersonalData{
		
		private HashMap<Integer,String> xpathVecFinal = new HashMap<Integer,String>();
		private HashMap<Integer,String> xpathVecInter  = new HashMap<Integer,String>();	
		private String templateName = "";
		
		public void addXPath(int key,String xpath , int parserID){
			if(parserID==ID_DETAILS){
				xpathVecFinal .put(key,xpath);
			}
			else{
				xpathVecInter .put(key,xpath);
			}
		}
		public String getXPath( int key, int parserID){
			if(parserID==ID_DETAILS){
				return xpathVecFinal.get(key);
			}
			return xpathVecInter.get(key);
		}
		
		public void  setTemplatePrefixName(String templateName){
			this.templateName = templateName;
		}
		
		public String getTemplatePrefixName(){
			return this.templateName ;
		}
		
		public int size(int parserID){
			if(parserID==ID_DETAILS){
				return xpathVecFinal.size();
			}
			return xpathVecInter.size();
		}
	}

	public TSServerDASLAdapter(long searchId) {
		super(searchId);
	}
	
	public TSServerDASLAdapter(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	
	protected  boolean bookAndPageExists(String book, String page){
		if(StringUtils.isEmpty(book)||StringUtils.isEmpty(page)){
			return false;
		}
		
		try{
			Search currentSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		        
	        DocumentsManagerI manager = currentSearch.getDocManager();
	        try{
	        	manager.getAccess();
	        	Collection<DocumentI> allChapters =  manager.getDocumentsList( true );
		        for( DocumentI doc:allChapters)
		        {
					if(book.equalsIgnoreCase(doc.getBook())&&page.equalsIgnoreCase(doc.getPage())){
						return true;
					}
		        }
	        }
		    finally{
		    	manager.releaseAccess();
		    }
		}
		catch(Exception e){}
		return false;
	}
	
	private boolean containsBoookPage(HashMap<String, String> data, Set<String> docs){
		String book = data.get("book");
		String page = data.get("page");
		
		for(String cur:docs){
			if(!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page)){
				if(cur.contains(book+"_"+page)){
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean containsInstrumentNo(String instrNo, Vector<ParsedResponse> results) {
		if (results != null) {
			for(ParsedResponse item: ( Vector<ParsedResponse> ) results) {
				String candInstrNo = item.getDocument().getInstno();
				if(!StringUtils.isEmpty(candInstrNo)) {
					if (instrNo.equals(candInstrNo)) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	private boolean hasOnlyInstrumentNo(HashMap<String, String> data){
		String docNo = data.get("docno");
		String apn = data.get("APN");
		String book = data.get("book");
		String page = data.get("page");
		String type = data.get("type");
		
		if(!StringUtils.isEmpty(docNo) && StringUtils.isEmpty(apn)
				&& StringUtils.isEmpty(book) && StringUtils.isEmpty(page)
				&& StringUtils.isEmpty(type)) {
					return true;
			}
		return false;
	}
	
	public static boolean isRelatedDoctype(String type) {
		String[] relatedDoctype = new String[]{
				DocumentTypes.RELEASE,
				DocumentTypes.ASSIGNMENT, 
				DocumentTypes.ASSUMPTION, 
				DocumentTypes.MODIFICATION, 
				DocumentTypes.SUBORDINATION, 
				DocumentTypes.SUBSTITUTION,
				DocumentTypes.CCER};
		for (int i=0;i<relatedDoctype.length;i++)
			if (type.equalsIgnoreCase(relatedDoctype[i]))
				return true;
		return false;
	}
	
	@SuppressWarnings("unchecked")
	protected void ParseResponse(String sAction, ServerResponse response, int viParseID) throws ServerResponseException {
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(InstanceManager.getManager().getCommunityId(searchId), miServerID) ;
		int cityChecked = dataSite.getCityCheckedInt() ;
		boolean isAllSubdivisions = Integer.toString(TSServerInfo.ARCHIVE_DOCS_MODULE_IDX).equals(sAction);
		boolean isSubdivisionLookUp = Integer.toString(TSServerInfo.TYPE_NAME_MODULE_IDX).equals(sAction) || isAllSubdivisions;
		
		ParsedResponse pr = response.getParsedResponse();		
		switch (viParseID){
			case ID_SAVE_TO_TSD:
				{
		        	String html = removeFormatting(pr.getResponse());
		        	Node doc = (Node) pr.getAttribute(ParsedResponse.DASL_RECORD);
		        	if("true".equalsIgnoreCase((String)pr.getAttribute("FAKE"))){
	        			DocumentI docI = pr.getDocument();
	        			if( docI!=null ){
	        				docI.setFake(true);
	        			}
		        	}
		        	String instrNo="";
	            	try{
	            		instrNo = XmlUtils.findFastNodeValue(doc,"PropertyAPN");
	            		instrNo = (instrNo==null)?"":instrNo;
	            	}catch(RuntimeException e){}
	            	
	            	boolean isTax = !StringUtils.isEmpty(instrNo);
	            	serverTypeDirectoryOverride = (isTax) ? "County Tax" : null;
	        		
	            	try{
	            		if(!isTax){
	            			instrNo = getInstrFromXML(doc, null,isSubdivisionLookUp);  
	            		}
	            		else{
	            			pr.setAttribute("DASL_TAX_DOCUMENT", "true");
	            		}
	            	}catch(RuntimeException e){}
	            	instrNo = instrNo.replaceAll("[ \t\r\n]+","");
		        	
		        	// set file name
		            msSaveToTSDFileName = instrNo + ".html";     
		            pr.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
		       
		            // save to TSD
		            msSaveToTSDResponce = html + CreateFileAlreadyInTSD(true);            
		            parser.Parse(pr, html, Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
				}
		    break;
			case ID_SEARCH_BY_BOOK_AND_PAGE: //for lookup
			case ID_SEARCH_BY_INSTRUMENT_NO: //for lookup
			case ID_DETAILS:
				{
					//bug 6797 comment 1 issue 6
					String resp = response.getResult();
					if (isAllSubdivisions) {
						if (resp.indexOf("<Subdivision>") == -1) {
							// there are no subdivisions returned
							pr.setError("<font color=\"red\">No Subdivisions found for this county!</font>");
							return;
						}
					}
					if (response.getParsedResponse().getResponse().contains("No Data Found"))
						return;
					
					Vector<ParsedResponse> newResultRows = new Vector<ParsedResponse>();
					//we must clear the docs because the same document might have been found in a different search and invalidated.
					//it that happens he will not be analyzed the second time and generate B3350
					Set<String> docs = new HashSet<String>(); //getDocs();
					boolean isAlreadySaved = false;
					long countSavedRows = 0;
					HashMap<String, ParsedResponse> uniqueResponses = new HashMap<String, ParsedResponse>();
					HashMap<String, String> mapperUniqueKeyInstrumentNumber = new HashMap<String, String>();
					
					for ( ParsedResponse item: ( Vector<ParsedResponse> ) pr.getResultRows() ) {
						
						// get parsed XML document
		            	Node doc = (Node) item.getAttribute(ParsedResponse.DASL_RECORD);
		           
		            	XmlUtils.mergeLogicalLegalDescriptions(doc);
		            	
		            	// determine instrument number - skip row if it has none
		            	String instrNo="";
		            	try{
		            		instrNo = XmlUtils.findFastNodeValue(doc,"PropertyAPN");
		            		instrNo = (instrNo==null)?"":instrNo;
		            	}
		            	catch(RuntimeException e){}
		            	
		            	boolean isTax = !StringUtils.isEmpty(instrNo);
		            	boolean mergeableNodeSites = (cityChecked == GWTDataSite.DT_TYPE || cityChecked==GWTDataSite.TS_TYPE || cityChecked==GWTDataSite.LA_TYPE||cityChecked==GWTDataSite.TP_TYPE)
		            			&&(!isSubdivisionLookUp);
		            	mergeableNodeSites = false;
						if( !isTax && mergeableNodeSites ){
	            			DASLSimpleInstrumentInfo simpleResponse = loadSimpleResponse(doc);
	            			String uniqueKey = simpleResponse.getUniqueKey();
	            			if(uniqueKey != null) {
	            				ParsedResponse initialParsedResponse = uniqueResponses.get(uniqueKey);
	            				if(initialParsedResponse != null) {
	            					//copy the Remarks tag from Instrument to LegalDescription tag
	            		            Node initialNode = mergeForTexas(doc, initialParsedResponse,dataSite);
	            		            mergeNodes(doc,initialNode, dataSite);	
	            				} else {
	            					if(getServerID() == 346923 && item.getDocument() != null) {
	            						if(mapperUniqueKeyInstrumentNumber.containsKey(item.getDocument().getInstno())) {
	            							DocumentI document = item.getDocument();
	            							if(document instanceof RegisterDocumentI) {
	            								RegisterDocumentI regD = (RegisterDocumentI)document;
	            								if(regD.getProperties().isEmpty() 
	            										&& regD.getGrantor().getNames().isEmpty()
	            										&& regD.getGrantee().getNames().isEmpty()) {
	            									
	            									continue;	//skip element if already found same instrumentNo and this one is empty	
	            								} else {
	            									mapperUniqueKeyInstrumentNumber.put(item.getDocument().getInstno(), uniqueKey);
	    	            							uniqueResponses.put(uniqueKey, item);
	            								}
	            							}
	            							
	            						} else {
	            							mapperUniqueKeyInstrumentNumber.put(item.getDocument().getInstno(), uniqueKey);
	            							uniqueResponses.put(uniqueKey, item);
	            						}
	            					} else {
	            						uniqueResponses.put(uniqueKey, item);
	            					}
	            				}
	            			} else {
	            				uniqueResponses.put(String.valueOf(countSavedRows++), item);
	            			}
		            	} else {
		            		uniqueResponses.put(String.valueOf(countSavedRows++), item);
		            	}
						//System.out.println("Processing response " + i + " of " + prs.size() + " took " + (System.currentTimeMillis() - start) + " miliseconds ");
					}
//					try {
//						if(this instanceof TXGenericDaslLa) {
//							for(Entry<String,ParsedResponse> e : uniqueResponses.entrySet()) {
//								//System.out.println(XmlUtils.createXML((Node)uniqueResponses.entrySet().iterator().next().getValue().getAttribute(ParsedResponse.DASL_RECORD)));
//								ParsedResponse item = e.getValue();
//								Node doc = (Node) item.getAttribute(ParsedResponse.DASL_RECORD);
//								XmlUtils.mergeLogicalLegalDescriptions(doc);
//							}
//						}
//					}catch(Exception e) {
//						e.printStackTrace();
//					}
					
					boolean isCO_TS_subdivisionLookUp = "CO".equals(dataSite.getStateAbrev()) &&
														"TS".equals(dataSite.getSiteTypeAbrev()) &&
														isSubdivisionLookUp;
					
					countSavedRows = 0;
			    	// parse all records
//		            for ( ParsedResponse item: uniqueResponses.values() ) {
					
					
					Map<DocumentI, ParsedResponse> docToParsedRes = new LinkedHashMap<DocumentI, ParsedResponse>(); 
					
		            for ( ParsedResponse item: ( Vector<ParsedResponse> ) pr.getResultRows() ) {
		            	item.setParentSite(pr.isParentSite());
		            	// parse
		            	String result[] = parse(item,viParseID);
		            	
		            	//task 7547
		            	if (isCO_TS_subdivisionLookUp) {
		            		Matcher matcher = Pattern.compile("(?is)<td>\\s*<b>\\s*ACTION_TYPE:\\s*</b>(.*?)</td>").matcher(result[0]);
		            			if (matcher.find()) {
		            				String action_type = matcher.group(1).trim();
		            				if (!"plat".equalsIgnoreCase(action_type)) {
		            					continue;
		            				}
		            			}
		            	}
		            	
		            	//fake instrument number if necesary bug 6148
		            	fakeIstrumentNo(item);
		            	
		            	String itemHtml = result[0];
		            	String shortType = result[1];
		            	item.setResponse( itemHtml );
		            	
		            	//            	 get parsed XML document
		            	Node doc = (Node) item.getAttribute(ParsedResponse.DASL_RECORD);
		           
		            	// determine instrument number - skip row if it has none
		            	String instrNo="";
		            	String shortInstrumentNo = "";
		            	try{
		            		instrNo = XmlUtils.findFastNodeValue(doc,"PropertyAPN");
		            		instrNo = (instrNo==null)?"":instrNo;
		            		shortInstrumentNo = instrNo;
		            		if (StringUtils.isEmpty(item.getDocument().getInstno())){
		            			item.getDocument().setInstno(shortInstrumentNo);
		            		}		            		
		            	}
		            	catch(RuntimeException e){}
		            	
		            	boolean isTax = !StringUtils.isEmpty(instrNo);
		            	serverTypeDirectoryOverride = (isTax) ? "County Tax" : null;
		        		
		            	if( !isTax && 
		            			(cityChecked == GWTDataSite.DT_TYPE 
		            			|| cityChecked == GWTDataSite.AC_TYPE 
		            			|| cityChecked == GWTDataSite.TS_TYPE
		            			/*|| cityChecked==36*/ ) ){
		            		if((TSServerInfo.NAME_MODULE_IDX+"").equals(sAction)){
		            			try{
		            				//item.getOtherInformationSet().setAtribute("SrcType", "GI");
		            				DocumentI document = item.getDocument();
		            				if(document instanceof RegisterDocumentI){
		            					RegisterDocumentI regDoc = (RegisterDocumentI) document;
		            					regDoc.setSearchType(SearchType.GI);
		            				}
		            			}catch(Exception e){}
		            		}
		            		else if(sAction!=null && !sAction.contains("DL___")){
		            			try{
		            				//item.getOtherInformationSet().setAtribute("SrcType", "PI");
				            		DocumentI document = item.getDocument();
			            			if(document instanceof RegisterDocumentI){
			            				RegisterDocumentI regDoc = (RegisterDocumentI) document;
			            				TSServerInfoModule module = mSearch.getSearchRecord().getModule();
			            				if (this instanceof GenericDASLTS && module != null && TSServerInfo.DASL_GENERAL_SEARCH_MODULE_IDX==module.getModuleIdx()) {
			            					if (StringUtils.isNotEmpty(module.getSearchType())) {
			            						regDoc.setSearchType(SearchType.valueOf(module.getSearchType()));
			            					} else {
				            					regDoc.setSearchType(SearchType.PI);
				            				}
			            				}
			            			}
		            			}catch(Exception e){}
		            		}
		            	}
		            	
		            	HashMap<String, String> data = new HashMap<String, String>();
		            	try{
		            		if(!isTax){
		            			instrNo = getInstrFromXML(doc, data,isSubdivisionLookUp);  
		            			String type1 = data.get("type");
		            			if(!StringUtils.isEmpty(type1)){
		            				shortInstrumentNo = instrNo.replaceFirst(type1, "");
		            			}
		            			else{
		            				shortInstrumentNo = instrNo;
		            			}
		            		}
		            	}catch(RuntimeException e){
		            		logger.warn(searchId + ": Document from dasl TSServerDASLAdapter has NO Instrument number. It has been skipped!");;           		
		            		continue;
		            	}
		            	
		            	item.setAttribute("dataFromDoc", data);
		            	instrNo = instrNo.replaceAll("[ \t\r\n]+","");
		            	instrNo = instrNo.replaceAll("'", ""); // Task 7221
		            	
		            	// do not add the document twice
		            	if( 	
		            			StringUtils.isEmpty(instrNo) 	|| 
		            			instrNo.length()<3 				||
		            			(docs.contains(instrNo)&&!isParentSite()) 			|| 
		            			//we make an exception for miscellaneous because they can be Fake documents
		            			("MISCELLANEOUS".equals(data.get("type")) && containsBoookPage(data,docs) && !isParentSite()) ||
		            			//(hasOnlyInstrumentNo(data) && containsInstrumentNo(instrNo, pr.getResultRows())) ||
		            			(hasOnlyInstrumentNo(data) && containsInstrumentNo(item.getInstrumentNumber(), pr.getResultRows())) ||
		            			instrNo.startsWith("0_0")||
		            			instrNo.startsWith("_")||
		            			instrNo.startsWith("-")
		            		){
		            			if(!isSubdivisionLookUp){
		            				continue;
		            			}
		            	}
		            	docs.add(instrNo);

		            	// create links
		            	String originalLink = "DL___" + instrNo;  
		            	if(isTax){
		            		originalLink = "DASLTAXDOCUMENT" + originalLink;
		            	}
		            	
		            	String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;   
		            	   
		            	// set file name
			            msSaveToTSDFileName = instrNo + ".html";     
			            
			            pr.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
		            	String checkbox = "";
		            		            	
		            	DocumentI docSaved = item.getDocument();
		            	if (docSaved == null) {
		            		docSaved = pr.getDocument();
		            	}
		            	
		            	ParsedResponse alreadyParsed = docToParsedRes.get(docSaved);
						if(alreadyParsed != null && docSaved instanceof RegisterDocumentI && !isSubdivisionLookUp) {
		            	
							DocumentI document2 = alreadyParsed.getDocument();
							if(document2 instanceof RegisterDocumentI) {
								
								alreadyParsed.setUseDocumentForSearchLogRow(true);
								
								RegisterDocumentI alreadySavedDoc = (RegisterDocumentI)document2;
								((RegisterDocumentI)docSaved).mergeDocumentsInformation(alreadySavedDoc, searchId, false, true);
								
								
								//----
								DocumentI docSavedClone = docSaved.clone();
				            	processInstrumentBeforeAdd(docSavedClone);
				            	 
			            		if (isInstrumentSaved(shortInstrumentNo, docSavedClone, data, false)) {
			            			checkbox = "saved";
			            			countSavedRows++;
			            			isAlreadySaved = true;
			            		} else {
			            			checkbox = "<input type='checkbox' name='docLink' value='" + sSave2TSDLink + "'>";
			            			if(isSubdivisionLookUp){
			            				checkbox = "<input type='radio' name='docLink' value='" + sSave2TSDLink + "'>";
			            			}
			            			if(mSearch.getInMemoryDoc(sSave2TSDLink)==null){
			            				mSearch.addInMemoryDoc(sSave2TSDLink, item);
			            				//item.setAttribute(
				        				//		TSServerInfoConstants.EXTRA_PARAM_MODULE_INDEX_SOURCE, 
				        				//		sAction);
			            			}
			            			try{
			            				getSearch().setAdditionalInfo(AdditionalInfoKeys.MODULE_PREFIX_KEY + "_" + miServerID + "_" + docSaved.prettyPrint() , 
			            					response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE));
			            			} catch (Exception e) {
										logger.error("Error tring to set module source for: " + shortInstrumentNo, e);
									}
			            		}
			            		
			            		String imageLinkAsHtml = null;
			            		Matcher mat = patImageLink.matcher(itemHtml);
				                if(mat.find()){
				                	
			                		String imageLink = createLinkForImage(data);
			                		
			                		imageLinkAsHtml = isSubdivisionLookUp?"":"<a href=\"" + imageLink + "\" target=\"_blank\" >View Image</a>";
			                		
				                	itemHtml = itemHtml.replaceAll(LINK_TO_IMAGE_REGEX,  imageLinkAsHtml);
				                	if(item.getImageLinksCount() == 0){
				                		item.addImageLink(new ImageLinkInPage (imageLink, getFileName(instrNo)));
				                	}
				                	
				                }
				                
//				                itemHtml =	"<tr> <td valign=\"center\" align=\"center\">" + checkbox + "</td> <td align=\"center\"><b>" + shortType + 
//			                			"</b></td><td>" + itemHtml + "</td><tr>";
			            		
								//-------
								
								String documentIndex = alreadySavedDoc.asHtml();
								
								
								alreadyParsed.setOnlyResponse("<tr><td align=\"center\" >" + checkbox + "</td><td align=\"center\"><b>" + shortType + 
			                			"</b></td><td>" + documentIndex + (imageLinkAsHtml != null? imageLinkAsHtml:"") + "</td></tr>");
								alreadyParsed.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + documentIndex + "</td></tr>");
								
							}
							
						} else {
		            	
							// add row
			            	newResultRows.add(item);
							
							docToParsedRes.put(docSaved, item);
							
			            	DocumentI docSavedClone = docSaved.clone();
			            	processInstrumentBeforeAdd(docSavedClone);
			            	 
		            		if (isInstrumentSaved(shortInstrumentNo, docSavedClone, data, false)) {
		            			checkbox = "saved";
		            			countSavedRows++;
		            			isAlreadySaved = true;
		            		} else {
		            			checkbox = "<input type='checkbox' name='docLink' value='" + sSave2TSDLink + "'>";
		            			if(isSubdivisionLookUp){
		            				checkbox = "<input type='radio' name='docLink' value='" + sSave2TSDLink + "'>";
		            			}
		            			if(mSearch.getInMemoryDoc(sSave2TSDLink)==null){
		            				mSearch.addInMemoryDoc(sSave2TSDLink, item);
		            				//item.setAttribute(
			        				//		TSServerInfoConstants.EXTRA_PARAM_MODULE_INDEX_SOURCE, 
			        				//		sAction);
		            			}
		            			try{
		            				getSearch().setAdditionalInfo(AdditionalInfoKeys.MODULE_PREFIX_KEY + "_" + miServerID + "_" + docSaved.prettyPrint() , 
		            					response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE));
		            			} catch (Exception e) {
									logger.error("Error tring to set module source for: " + shortInstrumentNo, e);
								}
		            		}
	            		
	            		if(isAllSubdivisions) {
	            			Pattern subdivisionPattern = Pattern.compile("(<b>Subdivision:</b>)(.*?)(<br>)");
	            			Matcher subdivisionMatcher = subdivisionPattern.matcher(itemHtml);
	            			StringBuffer htmlBuffer = new StringBuffer("<tr><td>");
	            			
	            			while(subdivisionMatcher.find()) {
	            				
	            				try {
	            				
	            					String subdivisionName = StringEscapeUtils.unescapeHtml(subdivisionMatcher.group(2).replaceAll("&nbsp;", " ")).trim();

		            				String secondaryLegalLink = createPartialLink(TSConnectionURL.idDASL, TSServerInfo.TYPE_NAME_MODULE_IDX) + 
		            						DASLFINAL+
				    						"&subdivision=" + URLEncoder.encode(subdivisionName, "UTF-8");
		            				
		            				htmlBuffer.append(subdivisionMatcher.group(1))
		            					.append(" <a href=\"").append(secondaryLegalLink).append("\">")
		            					.append(subdivisionName)
		            					.append("</a><br><br>");
	            				} catch (Exception e) {
	            					logger.error("Some error while parsing " + subdivisionMatcher.group());
	            				}
	            			}
	            			htmlBuffer.append("</td><tr>");
	            			            			
	            			itemHtml = htmlBuffer.toString(); 
	            			
	            		} else {
			                itemHtml =	"<tr> <td valign=\"center\" align=\"center\">" + checkbox + "</td> <td align=\"center\"><b>" + shortType + 
			                			"</b></td><td>" + itemHtml + "</td><tr>"; 
			                Matcher mat = patImageLink.matcher(itemHtml);
			                if(mat.find()){
			                	
		                		String imageLink = createLinkForImage(data);
			                	itemHtml = itemHtml.replaceAll(LINK_TO_IMAGE_REGEX,  isSubdivisionLookUp?"":"<a href=\"" + imageLink + "\" target=\"_blank\" >View Image</a>");
			                	if(item.getImageLinksCount() == 0){
			                		item.addImageLink(new ImageLinkInPage (imageLink, getFileName(instrNo)));
			                	}
			                	
			                }
	            		}
		               /* if (FileAlreadyExist(instrNo + ".html")) 
						{
		                	itemHtml += CreateFileAlreadyInTSD();
						}
						else 
						{
							itemHtml = addSaveToTsdButton(itemHtml, sSave2TSDLink, viParseID);
							mSearch.addInMemoryDoc(sSave2TSDLink, item);
						}*/
		                item.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
		                parser.Parse(item, itemHtml,Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
		                
		                if (isRelatedDoctype(item.getDocument().getDocType()))		//B 6923
		                	viParseID =  ID_SAVE_TO_TSD;
						}
		            }
		            
		            newResultRows.clear();
		            newResultRows.addAll(docToParsedRes.values());
		            
		            if (isCO_TS_subdivisionLookUp) {
		            	pr.setResultRows(newResultRows);
		            }
		            
		            updateSearchDataAfterSearch(newResultRows,viParseID);
		            
		            if(!"ALREADY_IN_TSD".equals(pr.getAttribute("ALREADY_IN_TSD"))&&!isSubdivisionLookUp){
		            	// set the result rows - does not contain instruments without instr no
		            	Collections.sort(newResultRows,new ParsedResponseDateComparator());
		            	pr.setResultRows(newResultRows);	  
		            }
		            
		            // set proper header and footer for parent site search , NDB has unique results and need this in automatic when user is asked what to choose
		            if ( mSearch.getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	
		            	String header = pr.getHeader();
		               	String footer = pr.getFooter();   
		            	
		               	if(isAllSubdivisions){
		               		header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
			            	footer = "\n</table></form>\n" ;
		               	} else if(isSubdivisionLookUp){
		            		
			               	header += "<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"/title-search" + URLMaping.PARENT_SITE_ACTIONS + "\"" + " method=\"POST\" > "
			                + "<input type=\"hidden\" name=\""+ TSOpCode.OPCODE + "\" value=\""+ ParentSiteActions.SUBDIVISION_NAME_LOOKUP + "\">"
			                + "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\">" 
			                + "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + mSearch.getSearchID() + "\"> "; 
			               		
			               		
			            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n";
			            	footer = "\n</table>" + "<input  type=\"submit\" class=\"button\" name=\"Button\" value=\"Select Subdivision\">\r\n</form>\n" ;;
		            	}else{
		            		header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
			            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n<tr bgcolor=\"#cccccc\">\n" +
			            			"<th width=\"1%\"><div>" + SELECT_ALL_CHECKBOXES + "</div></th>" +
			            			" <th width=\"1%\">Type</th> \n<th width=\"98%\" align=\"left\">Document</th> \n</tr>";
			            	
			            	if (isAlreadySaved && pr.getResultsCount() == countSavedRows) {
			            		footer = "\n</table>" + CreateFileAlreadyInTSD();       
			            	} else { 
			            		
			            		footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);        
			            		
			            	}
		            	}
		            	
		            	
		            	pr.setHeader(header);
		            	pr.setFooter(footer);
		            }
				}
			break;
			case ID_BROWSE_BACKSCANNED_PLATS:	//TP site, Plat Search module, when no data was found
			case 102:							//DT site, Fake Document Image Search (Data Tree) module, when no data was found
			{
	        	response.getParsedResponse().setResultRows(new Vector());
			}
	    break;
		}
	}

	protected String getFileName(String instrNo) {
		return instrNo + ".tiff" ;
	}

	protected void fakeIstrumentNo(ParsedResponse item) {
	}

	/**
	 * @param doc
	 * @param initialParsedResponse
	 * @return
	 */
	private Node mergeForTexas(Node doc, ParsedResponse initialParsedResponse, DataSite dataSite) {
		Node initialNode = (Node)initialParsedResponse.getAttribute(ParsedResponse.DASL_RECORD);
		
		try{
			if (dataSite.getStateAbrev().equalsIgnoreCase("TX") && (dataSite.getCityCheckedInt()==GWTDataSite.LA_TYPE||dataSite.getCityCheckedInt()==GWTDataSite.TP_TYPE)){
				String sourceQuery = "Instrument/Remarks";
				String destinationQuery = "LegalDescription";
				
				String destinationQuery1 = "Instrument/PartyInfo";
				XmlUtils.copyNodeValue(doc, sourceQuery, destinationQuery1, "RemarksCopy");
				XmlUtils.copyNodeValue(doc, sourceQuery, destinationQuery, "RemarksCopy");
				XmlUtils.copyNodeValue(initialNode, sourceQuery, destinationQuery, "RemarksCopy");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return initialNode;
	}

	protected void mergeNodes(Node sourceNode, Node destinationNode, DataSite dataSite) {
		if (dataSite.getStateAbrev().equalsIgnoreCase("TX") && (dataSite.getCityCheckedInt()==GWTDataSite.LA_TYPE||dataSite.getCityCheckedInt()==GWTDataSite.TP_TYPE)){
			mergeRemarks(sourceNode, destinationNode, dataSite);
		}
		NodeList parties = XmlUtils.xpathQuery(destinationNode, "Instrument/PartyInfo");
		NodeList partiesToAdd = XmlUtils.xpathQuery(sourceNode, "Instrument/PartyInfo");
		
		if(parties.getLength() > 0) {
			HashSet<DASLSimplePartyInfo> destinationParties = new HashSet<DASLSimplePartyInfo>();
			
			for (int i = 0; i < parties.getLength(); i++) {
				DASLSimplePartyInfo partyInfo = new DASLSimplePartyInfo();
				Node currentNode = parties.item(i);
				try {
					partyInfo.setPartyRole(XmlUtils.findFastNodeValue(currentNode,"PartyRole"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					partyInfo.setVestingType(XmlUtils.findFastNodeValue(currentNode,"VestingType"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					partyInfo.setFirstName(XmlUtils.findFastNodeValue(currentNode,"Party/FirstName"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					partyInfo.setLastName(XmlUtils.findFastNodeValue(currentNode,"Party/LastName"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					partyInfo.setMiddleName(XmlUtils.findFastNodeValue(currentNode,"Party/MiddleName"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				destinationParties.add(partyInfo);
			}
			
			
			NodeList instrumentNode = XmlUtils.xpathQuery(destinationNode, "Instrument");
			
			
			for (int i = 0; i < partiesToAdd.getLength(); i++) {
				DASLSimplePartyInfo partyInfo = new DASLSimplePartyInfo();
				Node currentNode = partiesToAdd.item(i);
				try {
					partyInfo.setPartyRole(XmlUtils.findFastNodeValue(currentNode,"PartyRole"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					partyInfo.setVestingType(XmlUtils.findFastNodeValue(currentNode,"VestingType"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					partyInfo.setFirstName(XmlUtils.findFastNodeValue(currentNode,"Party/FirstName"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					partyInfo.setLastName(XmlUtils.findFastNodeValue(currentNode,"Party/LastName"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					partyInfo.setMiddleName(XmlUtils.findFastNodeValue(currentNode,"Party/MiddleName"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(!destinationParties.contains(partyInfo)) {
					if(instrumentNode.getLength() >0  && instrumentNode.item(0) != null) {
						instrumentNode.item(0).appendChild(currentNode.cloneNode(true));
					}
				}
			}
		}
		if (dataSite.getStateAbrev().equalsIgnoreCase("TX") && (dataSite.getCityCheckedInt()==GWTDataSite.LA_TYPE || dataSite.getCityCheckedInt()==GWTDataSite.TP_TYPE)){
			XmlUtils.mergeLegalDescriptionsNodes(sourceNode, destinationNode);
		}	
	}

	private void mergeRemarks(Node sourceNode, Node destinationNode, DataSite dataSite){
		mergeNodes(sourceNode, destinationNode, "Instrument/Remarks", "Instrument/Remarks","Instrument");
	}
	
	
	
	private void mergeNodes(Node sourceNode, Node destinationNode, String sourceNodePath, String destinationNodePath, String theChildNodePath ){
		NodeList parties = XmlUtils.xpathQuery(destinationNode, sourceNodePath);
		NodeList partiesToAdd = XmlUtils.xpathQuery(sourceNode, destinationNodePath);
		NodeList instrumentNode = XmlUtils.xpathQuery(destinationNode, theChildNodePath);
		Set<String> alreadyFound = new HashSet<String>();
		for (int i = 0; i < parties.getLength(); i++) {
			Node currentNode = parties.item(i);
			String xmlRepresentation = XmlUtils.createXML(currentNode).toLowerCase();
			if(!alreadyFound.contains(xmlRepresentation)) {
				alreadyFound.add(xmlRepresentation);
			} else {
				if(instrumentNode.getLength() >0  && instrumentNode.item(0) != null) {
					instrumentNode.item(0).removeChild(currentNode);
				}
			}
		}
		
		for (int i = 0; i < partiesToAdd.getLength(); i++) {
			Node currentNode = partiesToAdd.item(i);
			String xmlRepresentation = XmlUtils.createXML(currentNode).toLowerCase();
			if(!alreadyFound.contains(xmlRepresentation)) {
				if(instrumentNode.getLength() >0  && instrumentNode.item(0) != null) {
					instrumentNode.item(0).appendChild(currentNode.cloneNode(true));
				}
				alreadyFound.add(xmlRepresentation);
			}
		}
	}
	
	
	private DASLSimpleInstrumentInfo loadSimpleResponse(Node doc) {
		DASLSimpleInstrumentInfo simpleResponse = new DASLSimpleInstrumentInfo();
		simpleResponse.setBook(XmlUtils.findFastNodeValue(doc, "Instrument/Book"));
		simpleResponse.setPage(XmlUtils.findFastNodeValue(doc, "Instrument/Page"));
		simpleResponse.setType(XmlUtils.findFastNodeValue(doc, "Instrument/Type"));
		simpleResponse.setIntrumentNo(XmlUtils.findFastNodeValue(doc, "Instrument/DocumentNumber"));
		
		String recordedDate = XmlUtils.findNodeValue( doc, "Instrument/RecordedDate/Date" );
		if( StringUtils.isEmpty(recordedDate) ){
			recordedDate = XmlUtils.findNodeValue( doc, "Instrument/PostedDate/Date" );
			if( StringUtils.isEmpty(recordedDate) ){
				recordedDate = XmlUtils.findNodeValue( doc, "Instrument/RecordedDate" );
			}
		}
		
		simpleResponse.setDateString(recordedDate);
		return simpleResponse;
	}

	@Override
	protected void updateSearchDataAfterSearch(Vector<ParsedResponse> newResultRows,int viParseID) {}

	@Override
	Map<Integer, RecordRules> getRecordRules(int parserId) {
		
		if(parserId==ID_SAVE_TO_TSD){
			parserId = ID_DETAILS;
		}
		if(parserId==ID_GET_IMAGE){
			return new HashMap<Integer, RecordRules>();
		}
		// try to get it from cache first
		if(cachedRecordRules.get(parserId) != null){
			return cachedRecordRules.get(parserId);
		}
		DataSite dat =HashCountyToIndex.getDateSiteForMIServerID(
				InstanceManager.getManager().getCommunityId(searchId),
				miServerID);
		String parserName = dat.getParserFilenameSufix();
		int siteType = dat.getCityCheckedInt();
		
		try{
			Map<Integer, RecordRules> rrs = new LinkedHashMap<Integer, RecordRules>();
			{ 	
				String styleFileName ="";
				String rulesFileName ="";
				
				if( parserId == ID_DETAILS ||  (parserId == ID_SEARCH_BY_INSTRUMENT_NO||parserId == ID_SEARCH_BY_BOOK_AND_PAGE) ){ //final document	
					styleFileName = RESOURCE_FOLDER + parserName + ".xsl";
					rulesFileName = RULES_FOLDER + parserName + ".xml";
				}
				else{//intermediar page
					styleFileName = RESOURCE_FOLDER +File.separator+ "inter"+File.separator+ parserName + ".xsl";
					rulesFileName = RULES_FOLDER  + File.separator + "inter" + File.separator +  parserName + ".xml";
				}
				
				String style = FileUtils.readTextFile(styleFileName);
				String shortType = Search.getServerAbbrevTypeFromCityChecked(siteType);
				String longType = Search.getServerTypeFromCityChecked(siteType);
				Document rules = XMLUtils.read(new File(rulesFileName), RULES_FOLDER);
				String overrideType = null;
				
				ServerPersonalData data = (ServerPersonalData)getServerPersonalData();
				for(int id=0;id<data.size(parserId);id++){
					String xpath = data.getXPath(id, parserId) ;
					rrs.put(id, new RecordRules(id, style, shortType, longType, xpath, rules, overrideType));
				}
			}
			// put it into cache
			cachedRecordRules.put(parserId, rrs) ;
			return rrs;
		}catch(Exception e){
			//temporary we comment this cod we will see the xml in page till the parsing is ready
			e.printStackTrace();
			throw new RuntimeException (e);
		}
	}
	
	//make sure that parameters are compatible with XML standard
	private static void  prepareParametersForXML(HashMap<String, Object> inputs){
		Set<Map.Entry<String, Object>> set = inputs.entrySet();
		for(Map.Entry<String, Object> entry:set){
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof String ) {
				String  valuestr = (String ) value;
				valuestr = StringEscapeUtils.escapeHtml(valuestr);
				inputs.put(key, valuestr);
			}
		}
	}
	
	@Override
	protected String buildSearchQuery(Map<String, String> params, int moduleIdx) {
		String xmlret = "";
		
		HashMap<String, Object> inputs = fillTemplatesParameters(params);
		prepareParametersForXML(inputs);
		
		List<CommunityTemplatesMapper> userTemplates = null;
		try {
			userTemplates = GlobalTemplateFactory.getInstance().getTemplates();
		} catch (Exception e) {
			System.err.print(e.getMessage());
			return xmlret;
		}
		
		CommunityTemplatesMapper templateInfo = getDASLTemplate(userTemplates);
		try{
			xmlret =  AddDocsTemplates.completeNewTemplatesV2ForTextFilesOnly(inputs, "", templateInfo,null, false, null, null, new HashMap<String,String>());
		}
		catch(TemplatesException e){
			e.printStackTrace();
		}
		return  xmlret ;
	}
	
	private CommunityTemplatesMapper getDASLTemplate(List<CommunityTemplatesMapper> userTemplates) {
		if (userTemplates != null) {
			for (CommunityTemplatesMapper communityTemplatesMapper : userTemplates) {
				String test = communityTemplatesMapper.getPath();
				if (test == null) {
					test = "";
				}
				if (test.contains(getDASLTemplateName())) {
					return communityTemplatesMapper;
				}	
			}
			
		}
		return null;
	}
	
	protected String getDASLTemplateName() {
		return getServerPersonalData().getTemplatePrefixName();
	}
	
	@Override
	protected HashMap<String, Object> fillTemplatesParameters(Map<String, String> params) {
		return emptyDASLTemplatesParameters();
	}
	
	protected  static boolean isIntermediaryResult(int viParseID){
		return (viParseID != ID_DETAILS 
				&& viParseID != ID_SAVE_TO_TSD 
				&&  viParseID!=ID_GET_IMAGE
				&& viParseID!=ID_SEARCH_BY_BOOK_AND_PAGE
				&& viParseID!=ID_SEARCH_BY_INSTRUMENT_NO);
	}
	
	public static  HashMap<String, Object>emptyDASLTemplatesParameters( ){
		HashMap <String, Object> templateParams = new HashMap<String, Object>();
		templateParams.put( AddDocsTemplates.DASLPcl, "");
		templateParams.put( AddDocsTemplates.DASLPcl1, "");
		templateParams.put( AddDocsTemplates.DASLImEmail, "");
		templateParams.put( AddDocsTemplates.DASLPcl2, "");
		templateParams.put(AddDocsTemplates.DASLDocType, "");
		templateParams.put(AddDocsTemplates.DASLDocThrough, "");
		templateParams.put(AddDocsTemplates.DASLSSN4, "");
		templateParams.put( AddDocsTemplates.DASLSubdivision,"");
		templateParams.put( AddDocsTemplates.DASLStreetName, "" );
		templateParams.put( AddDocsTemplates.DASLStreetNumber, "");
		templateParams.put( AddDocsTemplates.DASLStreetSuffix, "");
		templateParams.put( AddDocsTemplates.DASLLastName, "");
		templateParams.put( AddDocsTemplates.DASLMiddleName, "");
		templateParams.put( AddDocsTemplates.DASLFirstName, "");
		templateParams.put( AddDocsTemplates.DASLID, "");
		templateParams.put( AddDocsTemplates.DASLCountyFIPS, "" );
		templateParams.put( AddDocsTemplates.DASLStateFIPS, "");
		templateParams.put( AddDocsTemplates.DASLClientTransactionReference,"");
		templateParams.put( AddDocsTemplates.DASLSearchType,"");
		templateParams.put( AddDocsTemplates.DASLPropertySearchFromDate,"");
		templateParams.put( AddDocsTemplates.DASLPropertySearchToDate,"");
		templateParams.put( AddDocsTemplates.DASLAPN,"");
		templateParams.put( AddDocsTemplates.DASLStreetDirection,"");
		templateParams.put( AddDocsTemplates.DASLStateAbbreviation,"");
		templateParams.put( AddDocsTemplates.DASLCounty,"");
		templateParams.put( AddDocsTemplates.DASLIncludeTaxFlag,"");
		templateParams.put( AddDocsTemplates.DASLPropertyChainOption,"");
		templateParams.put( AddDocsTemplates.DASLPartySearchType,"");
		templateParams.put( AddDocsTemplates.DASLPartySearchFromDate,"");
		templateParams.put( AddDocsTemplates.DASLPartySearchToDate,"");
		templateParams.put( AddDocsTemplates.DASLPartyRole_1,"");
		templateParams.put( AddDocsTemplates.DASLFirstName_1,"");
		templateParams.put( AddDocsTemplates.DASLMiddleName_1,"");
		templateParams.put( AddDocsTemplates.DASLLastName_1,"");
		templateParams.put( AddDocsTemplates.DASLPartyRole_2,"");
		templateParams.put( AddDocsTemplates.DASLFirstName_2,"");
		templateParams.put( AddDocsTemplates.DASLMiddleName_2,"");
		templateParams.put( AddDocsTemplates.DASLLastName_2,"");
		templateParams.put( AddDocsTemplates.DASLPartyRole_3,"");
		templateParams.put( AddDocsTemplates.DASLFirstName_3,"");
		templateParams.put( AddDocsTemplates.DASLMiddleName_3,"");
		templateParams.put( AddDocsTemplates.DASLLastName_3,"");
		templateParams.put( AddDocsTemplates.DASLPartyRole_4,"");
		templateParams.put( AddDocsTemplates.DASLFirstName_4,"");
		templateParams.put( AddDocsTemplates.DASLMiddleName_4,"");
		templateParams.put( AddDocsTemplates.DASLLastName_4,"");
		templateParams.put( AddDocsTemplates.DASLPartyRole_5,"");
		templateParams.put( AddDocsTemplates.DASLFirstName_5,"");
		templateParams.put( AddDocsTemplates.DASLMiddleName_5,"");
		templateParams.put( AddDocsTemplates.DASLLastName_5,"");
		templateParams.put( AddDocsTemplates.DASLPartyRole_6,"");
		templateParams.put( AddDocsTemplates.DASLFirstName_6,"");
		templateParams.put( AddDocsTemplates.DASLMiddleName_6,"");
		templateParams.put( AddDocsTemplates.DASLLastName_6,"");
		templateParams.put( AddDocsTemplates.DASLNickName,"");
		templateParams.put( AddDocsTemplates.DASLWithProperty ,"");
		templateParams.put( AddDocsTemplates.DASLSoundIndex,"");
		templateParams.put( AddDocsTemplates.DASLBook,"");
		templateParams.put( AddDocsTemplates.DASLPage,"");
		templateParams.put( AddDocsTemplates.DASLDocumentNumber ,"");
		templateParams.put( AddDocsTemplates.DASLPropertySearchType,"");
		templateParams.put( AddDocsTemplates.DASLClientReference,"");
		templateParams.put( AddDocsTemplates.DASLLot,"");
		templateParams.put( AddDocsTemplates.DASLLotThrough,"");
		templateParams.put( AddDocsTemplates.DASLBuilding,"");
		templateParams.put( AddDocsTemplates.DASLUnit,"");
		templateParams.put( AddDocsTemplates.DASLSubLot,"");
		templateParams.put( AddDocsTemplates.DASLBlock,"");
		templateParams.put( AddDocsTemplates.DASLPlatBook,"");
		templateParams.put( AddDocsTemplates.DASLPlatPage,"");
		templateParams.put( AddDocsTemplates.DASLPlatDocumentYear,"");
		templateParams.put( AddDocsTemplates.DASLPlatDocumentNumber,"");
		templateParams.put( AddDocsTemplates.DASLImageSearchType,"");
		templateParams.put( AddDocsTemplates.DASLimageId,"");
		templateParams.put( AddDocsTemplates.DASLSection,"");
		templateParams.put( AddDocsTemplates.DASLTownship,"");
		templateParams.put( AddDocsTemplates.DASLRange,"");
		templateParams.put( AddDocsTemplates.DASLQuarterOrder,"");
		templateParams.put( AddDocsTemplates.DASLQuaterValue,"");
		templateParams.put( AddDocsTemplates.DASLARB,"");
		templateParams.put( AddDocsTemplates.DASLYearFiled,"");
		templateParams.put( AddDocsTemplates.DASLParcelId,"");
		templateParams.put( AddDocsTemplates.DASLPlatName, "");
		templateParams.put( AddDocsTemplates.DASLStreetFraction, "");
		templateParams.put( AddDocsTemplates.DASLStreetPostDirection, "");
		templateParams.put( AddDocsTemplates.DASLAddressUnitValue,"");
		templateParams.put( AddDocsTemplates.DASLParcel,"");
		templateParams.put( AddDocsTemplates.DASL_B_P_H,"");
		templateParams.put( AddDocsTemplates.DASLPlatLabel,"");
		templateParams.put( AddDocsTemplates.DASL_TRACT,"");
		templateParams.put( AddDocsTemplates.DASL_BLOCK_THROUGH,"");
		templateParams.put( AddDocsTemplates.DASL_LOT_THROUGH,"");
		templateParams.put( AddDocsTemplates.DASL_NCB_NO,"");
		templateParams.put( AddDocsTemplates.DASL_DIVISION_NO,"");
		templateParams.put( AddDocsTemplates.DASL_TRACT_THROUGH,"");
		templateParams.put( AddDocsTemplates.DASLTitleOfficer,"");
		templateParams.put(AddDocsTemplates.DASLRealPartySearchType1, "");
		templateParams.put(AddDocsTemplates.DASLUnitPrefix, "");
		templateParams.put(AddDocsTemplates.DASLZip, "");
		templateParams.put(AddDocsTemplates.DASLCity, "");
		templateParams.put(AddDocsTemplates.DASLStateAbbreviation, "");
		templateParams.put(AddDocsTemplates.DASLDocumentSearchType, "");
		templateParams.put(AddDocsTemplates.DASLSubdivisionUnit, "");
		templateParams.put(AddDocsTemplates.DASLAddition, "");
		templateParams.put(AddDocsTemplates.DASLOwnerFullName, "");
		templateParams.put(AddDocsTemplates.DASLAbstractNumber, "");
		templateParams.put(AddDocsTemplates.DASLAbstractName, "");
		templateParams.put(AddDocsTemplates.DASLPreviousParcel, "");
		templateParams.put(AddDocsTemplates.DASLQuarterOrder1, "");
		templateParams.put(AddDocsTemplates.DASLQuaterValue1, "");
		templateParams.put(AddDocsTemplates.DASLQuarterOrder2, "");
		templateParams.put(AddDocsTemplates.DASLQuaterValue2, "");
		templateParams.put(AddDocsTemplates.DASLQuarterOrder3, "");
		templateParams.put(AddDocsTemplates.DASLQuaterValue3, "");
		templateParams.put(AddDocsTemplates.DASLQuarterOrder4, "");
		templateParams.put(AddDocsTemplates.DASLQuaterValue4, "");
		templateParams.put(AddDocsTemplates.DASLClientId, "");
		templateParams.put(AddDocsTemplates.DASLIndexOnly, "");
		templateParams.put(AddDocsTemplates.DASLMonthFiled, "");
		templateParams.put(AddDocsTemplates.DASLDayFiled, "");
		return templateParams;
	}
	
	protected String getInstrFromXML(Node doc, HashMap<String, String> data){
		return getInstrFromXML(doc, data, false);
	}
	
	protected String getInstrFromXML(Node doc, HashMap<String, String> data, boolean isSubdivisionLookUp) {
		String APN = null;
		String year= null;
		String month = null;
		String book= null;
		String page= null;
		String documentNr= null;
		String type= null;
		String book1= null;
		String page1= null;
		String documentNr1= null;
		String day = null;
		
		for(Node n1: XmlUtils.getChildren(doc)){
			String first= n1.getNodeName();
			if(first.equals("BaseStarterRecord")){
				String ret =  XmlUtils.findFastNodeValue(n1, "OrderNumber");
				if(!StringUtils.isEmpty(ret)){
					return ret;
				}
			}
			else if(first.equals("APN")){
				APN=  XmlUtils.getNodeValue(n1);
				if(data!=null){
					data.put("APN", APN);
				}
			}
			else if(first.equals("Instrument")){
				
				SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyy");
				java.util.Calendar cal = java.util.Calendar.getInstance();
				cal.set(java.util.Calendar.YEAR, 1960);
				
				book = XmlUtils.findFastNodeValue(n1, "Book");
				page = XmlUtils.findFastNodeValue(n1, "Page");
				documentNr = XmlUtils.findFastNodeValue(n1, "DocumentNumber");
				String recordedDate = XmlUtils.findFastNodeValue( n1, "RecordedDate/Date" );
				
				if(StringUtils.isEmpty(documentNr)&& StringUtils.isEmpty(book)){
					String bookPage =  XmlUtils.findFastNodeValue(n1, "FinanceBookPage");
					if (StringUtils.isNotEmpty(bookPage)){
						int poz = bookPage.indexOf("-");
						if(poz>0){
							book = bookPage.substring(0,poz);
							if(poz<bookPage.length()-1){
								page = bookPage.substring(poz+1);
							}
						}
					}
					documentNr =  XmlUtils.findFastNodeValue(n1, "FinanceInstrumentNumber");
					recordedDate = XmlUtils.findFastNodeValue(n1, "FinanceRecordedDate");
				}
				
				
				if( !StringUtils.isEmpty(recordedDate) ){
					try{ 
						Date d = df.parse(recordedDate); 
						cal.setTime(d);
					}catch(ParseException pr){}
				}
				else{
					recordedDate = XmlUtils.findFastNodeValue( n1, "PostedDate/Date" );
					if( !StringUtils.isEmpty(recordedDate) ){
						try{ 
							Date d = df.parse(recordedDate); 
							cal.setTime(d);}
						catch(ParseException pr){}
					}else{
						recordedDate = XmlUtils.findFastNodeValue( n1, "RecordedDate" );
						if( !StringUtils.isEmpty(recordedDate) ){
							try{ 
								Date d = df.parse(recordedDate); 
								cal.setTime(d);}
							catch(ParseException pr){}
						}
					}
				}
				
				
				
				book = (book==null)?"":book;
				page = (page==null)?"":page;
				documentNr = (documentNr==null)?"":documentNr;
				
				// Bug 8018: ignore book-page 1-1 when instrument nr is valid
				if(!StringUtils.isEmpty(documentNr) && book.equals("1") && page.equals("1")) {
					book = page = "";
				}
				
				type = XmlUtils.findFastNodeValue(n1, "Type");
				if(type == null){
					type ="";
				}
				
				int poz = type.lastIndexOf('-');
				if(poz>0&&poz+1<type.length()-1){
					type = type.substring(poz+1);
				}
				type = type.replaceAll("[^A-Za-z0-9\\-]", "");
				
				year = cal.get(java.util.Calendar.YEAR)+"";
				month = cal.get(java.util.Calendar.MONTH)+1+"";
				day = cal.get(java.util.Calendar.DAY_OF_MONTH)+"";
				
				if(data!=null){
					data.put("year", year);
					data.put("book", book);
					data.put("page", page);
					data.put("docno", documentNr);
					data.put("APN", APN);
					data.put("type", type);
					data.put("month", month);
					data.put("day", day);
				}
				
			}
			else if(first.equals("TitleDocument")){
				book1 = XmlUtils.findFastNodeValue(n1, "Instrument/Book");
				page1 = XmlUtils.findFastNodeValue(n1, "Instrument/Page");
				documentNr1 = XmlUtils.findFastNodeValue(n1, "Instrument/DocumentNumber");
			}
		}
		
		if(isSubdivisionLookUp){
			Random rand = new Random();
			documentNr = rand.nextInt(Integer.MAX_VALUE)+"-"+System.currentTimeMillis();
		}
		
		String ret = APN;
		
		if(StringUtils.isEmpty(ret)&&!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page)){
			ret = book+"_"+page;
		}
		else if(StringUtils.isEmpty(ret)&& documentNr != null && !StringUtils.isEmpty(documentNr.replaceAll("[_-]", ""))) {
			ret = documentNr;
		}
		
		if(StringUtils.isEmpty(ret)){
			if(!StringUtils.isEmpty(book1)&&!StringUtils.isEmpty(page1)){
				 ret = book1+"_"+page1;
			}
			else if(!StringUtils.isEmpty(documentNr1)){
				ret = documentNr1.replaceAll("[_-]", "");
			}
		}
		
		if( StringUtils.isEmpty(ret) ){
			return "";
		}
		type = (type==null)?"":type;
		ret = ret+DocumentTypes.getDocumentCategory( type,searchId)+year+DocumentTypes.getDocumentSubcategory(type, searchId);
		return ret;
	}
	
	@Override
	protected ServerPersonalData getServerPersonalData() {
		throw new RuntimeException("Please implement getServerPersonalData() function");
	}

	/**
     * Sets the serverID.
     * @param serverID The serverID to set
     */
    public void setServerID(int serverID) {
    	super.setServerID(serverID);
        setRangeNotExpanded(true);
    }
	
    
    protected  String createLinkForImage(HashMap<String, String> values){
    	return null;
    }

    
    /**
     *  Called only on Automatic when "click" an intermediary result
     */
	@Override
	protected ServerResponse performRequest(String page, int methodType, String action, int parserId, String imageLink, String vbRequest, Map<String, Object> extraParams) throws ServerResponseException {
		String dispatcher = action.replaceAll(".*"+"dispatcher"+"=([^&]*)", "$1");
		int poz = dispatcher.indexOf("&");
    	if(poz>0){
    		dispatcher = dispatcher.substring(0,poz);
    	}
    	int dispatcherInt = -2;
    	try{
    		if(!"SaveToTSD".equals(action)){
    			dispatcherInt = Integer.parseInt(dispatcher);
    		}
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    	
        ServerResponse Response = new ServerResponse();
        if(action.contains(DASLFINAL) && dispatcherInt > 0 ){
        	TSServerInfo info = getDefaultServerInfo();
        	TSServerInfoModule module = info.getModule(dispatcherInt);
            String params = action.substring(action.indexOf(DASLFINAL)+DASLFINAL.length()+1,action.indexOf("dispatcher"));
            HashMap<String,String> map = HttpUtils.getConparamsFromString(params,false);
        	
            Set<String> set = map.keySet();
            int i=0;
            for(Object key:set){
            	String value = (String)map.get(key);
            	module.getFunction(i).setParamValue(value);
            	i++;
            }   
            Response  = SearchBy(module, null);
        }
        return Response ;
	}

	@Override
	protected  DownloadImageResult saveImage(ImageLinkInPage image)throws ServerResponseException {
		return null;
	}

	@Override
	protected String modifyXMLResponse(String xml,int moduleIDX) {
		return xml.replaceAll("([^a-zA-Z])NULL([^a-zA-Z])", "$1$2");
	}

	@Override
	protected int changeParserIdBasedOnXMLResponse(String xml, int moduleIDX,int oldParserId) {
		return oldParserId;
	}  
	
	protected String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		build.append("&");
		build.append(getLinkPrefix(iActionType));
		return build.toString();
	}
	
	@Override	
	public final void parseLegalDescriptions(Document doc, ParsedResponse item, ResultMap resultMap){
		specificParseLegalDescription(doc,item, resultMap);
	}
	
	@Override	
	public final void parseAddress(Document doc, ParsedResponse item, ResultMap resultMap){
		specificParseAddress(doc, item, resultMap);
	}
	
	
	private static final Pattern PAT_ADDRESS = Pattern.compile("([0-9]+)\\s+([0-9]+)\\s+([0-9a-zA-Z -]+)");
	
	protected final void defaultParseAddress(Document doc, ParsedResponse item, ResultMap resultMap){
		String fullAddress = XmlUtils.findNodeValue(doc, "/TitleDocument/Instrument/PropertyAddress/FullStreetAddress");
		Vector pisVector = (Vector) item.infVectorSets.get("PropertyIdentificationSet");
		if (pisVector == null) {
        	pisVector = new Vector();
        	item.infVectorSets.put("PropertyIdentificationSet", pisVector);
        }
		
		if(StringUtils.isNotEmpty(fullAddress)){
			
			Matcher mat = PAT_ADDRESS.matcher(fullAddress);
			
			if(mat.find()){
				String no = mat.group(1);
				String unit = mat.group(2);
				String streetName = mat.group(3);
				
				fullAddress = no + " " + streetName + " " + (unit.contains("#")?unit:"#"+unit);
			}
			
			if( (pisVector.size()==0||pisVector.size()>1)){
				PropertyIdentificationSet pis = new PropertyIdentificationSet();
				
				pis.setAtribute("StreetName", fullAddress);
				pisVector.add(pis);
			}else if(pisVector.size()==1){
				PropertyIdentificationSet pis = (PropertyIdentificationSet)pisVector.get(0);
				pis.setAtribute("StreetName", fullAddress);
			}
		}
		
	}
	
	public final void defaultParseLegalDescription(Document doc, ParsedResponse item, ResultMap resultMap) {
		try {
			NodeList legals = XmlUtils.xpathQuery(doc, "/TitleDocument/LegalDescription");
			if (legals.getLength() == 0){//B 5051
				legals = XmlUtils.xpathQuery(doc, "/TitleDocument/Instrument/LegalDescriptions");
			}
			if (legals.getLength() == 0){//B 7656
				legals = XmlUtils.xpathQuery(doc, "/TitleDocument/Instrument/LegalDescription");
			}
			Vector pisVector = (Vector) item.infVectorSets.get("PropertyIdentificationSet");
			if (pisVector == null) {
	        	pisVector = new Vector();
	        	item.infVectorSets.put("PropertyIdentificationSet", pisVector);
	        }
	        pisVector.clear(); 
	        
			for (int i = 0; i < legals.getLength(); i++) {
				if(legals.item(i) != null ) {
					PropertyIdentificationSet pis = new PropertyIdentificationSet();

					
					String propertyId = findLegalDescriptionNode(legals.item(i), "PropertyID");
					String propertyType = findLegalDescriptionNode(legals.item(i), "PropertyType");
					
					String acreage = findLegalDescriptionNode(legals.item(i), "Acreage");
					String subLot = findLegalDescriptionNode(legals.item(i), "Sublot");
					String lot = findLegalDescriptionNode(legals.item(i), "LotBlock/Lot");
					String block = findLegalDescriptionNode(legals.item(i), "LotBlock/Block");
					String lotThrough = findLegalDescriptionNode(legals.item(i), "LotBlock/LotThrough");
					String blockThrough = findLegalDescriptionNode(legals.item(i), "LotBlock/BlockThrough");
					
					String pb = findLegalDescriptionNode(legals.item(i), "Plat/Plat_Book");
					String pg = findLegalDescriptionNode(legals.item(i), "Plat/Plat_Page");
					
					String parcel = findLegalDescriptionNode(legals.item(i), "Parcel");
					String area = findLegalDescriptionNode(legals.item(i), "Area");
					
					String subdivisionName = findLegalDescriptionNode(legals.item(i), "Plat/Plat_Name");
					
					String docNo = findLegalDescriptionNode(legals.item(i), "Plat/Plat_DocumentNumber");
					String quarOrd = findLegalDescriptionNode(legals.item(i), "Quarters/Order");
					String quarVal = findLegalDescriptionNode(legals.item(i), "Quarters/QuaterValue");
					
		 			String thruQuarOrd = findLegalDescriptionNode(legals.item(i), "ThruQuarters/Order");
					String thruQuarVal = findLegalDescriptionNode(legals.item(i), "ThruQuarters/QuaterValue");
					String arb = findLegalDescriptionNode(legals.item(i), "ARB");

					
					NodeList sections = XmlUtils.xpathQuery(legals.item(i), "Sections/Value");
					String sec = "";
					for (int j = 0; j < sections.getLength(); j++) {
						if(sections.item(j) != null ) {
							sec = XmlUtils.getNodeValue(sections.item(j)) + " ";
						}
					}
					
					NodeList thruSections = XmlUtils.xpathQuery(legals.item(i), "ThruSections/Value");
					String thruSec = "";
					for (int j = 0; j < thruSections.getLength(); j++) {
						if(thruSections.item(j) != null ) {
							thruSec = XmlUtils.getNodeValue(thruSections.item(j)) + " ";
						}
					}

					if (!StringUtils.isEmpty(parcel)){
						pis.setAtribute("SubdivisionParcel", parcel);
					}
					
					if (!StringUtils.isEmpty(area)){
						pis.setAtribute("Area", area);
					}
					
					String twp = findLegalDescriptionNode(legals.item(i), "Township");
					String rng = findLegalDescriptionNode(legals.item(i), "Range");
					
					String thruTwp = findLegalDescriptionNode(legals.item(i), "ThruTownship");
					String thruRng = findLegalDescriptionNode(legals.item(i), "ThruRange");
					String thruArea = findLegalDescriptionNode(legals.item(i), "ThruArea");
					String thruParcel = findLegalDescriptionNode(legals.item(i), "ThruParcel");
					
					if (!StringUtils.isEmpty(thruParcel)){
						pis.setAtribute("ThruParcel", thruParcel);
					}
					if (!StringUtils.isEmpty(thruArea)){
						pis.setAtribute("ThruArea", thruArea);
					}
					if (!StringUtils.isEmpty(thruQuarOrd)){
						pis.setAtribute("ThruQuarterOrder", thruQuarOrd.replaceAll("\\s+", ""));
					}
					if (!StringUtils.isEmpty(thruQuarVal)){
						pis.setAtribute("ThruQuarterValue", thruQuarVal.replaceAll("\\s+", ""));
					}
					
//					 ThruQuarters, ThruParcel, ThruArea, " from DASL response
					
					String currentValue;
					if (!StringUtils.isEmpty(propertyId)){
						currentValue = pis.getAtribute("ParcelID");
						if (!StringUtils.isEmpty(currentValue)){
							propertyId = currentValue + " " + propertyId;
						}
						pis.setAtribute("ParcelID", propertyId);
					}

					if (!StringUtils.isEmpty(propertyType)){
						pis.setAtribute("PropertyType", propertyType);
					}
					
					if (!StringUtils.isEmpty(acreage )){
						pis.setAtribute("Acreage", acreage);
					}
					
					if (!StringUtils.isEmpty(lot)){
						pis.setAtribute("SubdivisionLotNumber", lot);
					}
					if (!StringUtils.isEmpty(subLot)){
						pis.setAtribute("SubLot", subLot);
					}
					if (!StringUtils.isEmpty(block)){
						pis.setAtribute("SubdivisionBlock", block);
					}
					
					if (!StringUtils.isEmpty(lotThrough)){
						pis.setAtribute("SubdivisionLotThrough", lotThrough);
					}
					if (!StringUtils.isEmpty(blockThrough)){
						pis.setAtribute("SubdivisionBlockThrough", blockThrough);
					}
					if (!StringUtils.isEmpty(pb)){
						pis.setAtribute("PlatBook", pb.replaceAll("\\s+", ""));
					}
										
					if (!StringUtils.isEmpty(pg)){
						pis.setAtribute("PlatNo", pg.replaceAll("\\s+", ""));
					}
					
					if (!StringUtils.isEmpty(subdivisionName)){
						pis.setAtribute("SubdivisionName", subdivisionName.replaceAll("\\s{2,}", " ").trim());
					}
					
					if (!StringUtils.isEmpty(docNo)){
						pis.setAtribute("PlatInstr", docNo.replaceAll("\\s+", ""));
					}
					if (!StringUtils.isEmpty(quarOrd)){
						pis.setAtribute("QuarterOrder", quarOrd.replaceAll("\\s+", "").replaceAll("\\A\\s*0+", ""));
					}
					if (!StringUtils.isEmpty(quarVal)){
						pis.setAtribute("QuarterValue", quarVal.replaceAll("\\s+", "").replaceAll("\\A\\s*0+", ""));
					}
					if (!StringUtils.isEmpty(arb)){
						pis.setAtribute("ARB", arb.replaceAll("\\s+", ""));
					}
					NodeList remarks = XmlUtils.xpathQuery(doc, "/TitleDocument/Remarks");
					if (remarks.getLength() == 0) {
						if (!StringUtils.isEmpty(sec)){
							pis.setAtribute("SubdivisionSection", sec.trim());
						}
						if (!StringUtils.isEmpty(twp)){
							pis.setAtribute("SubdivisionTownship", twp.replaceAll("\\s+", ""));
						}
						if (!StringUtils.isEmpty(rng)){
							pis.setAtribute("SubdivisionRange", rng.replaceAll("\\s+", ""));
						}
						
						if (!StringUtils.isEmpty(thruSec)){
							pis.setAtribute("ThruSection", thruSec.trim());
						}
						if (!StringUtils.isEmpty(thruTwp)){
							pis.setAtribute("ThruTownship", thruTwp.replaceAll("\\s+", ""));
						}
						if (!StringUtils.isEmpty(thruRng)){
							pis.setAtribute("ThruRange", thruRng.replaceAll("\\s+", ""));
						}
					}
					
						
					pisVector.add(pis);					
				}
			}
			if (resultMap != null){
				removeResultmapValues(pisVector, resultMap);
			}
		} catch (Exception e) {
			logger.error("Exception in parsing of legal special fields in DASLAdapter " + searchId, e);
		}		

	}
	
	protected String findLegalDescriptionNode(Node legalNode, String name) {
		try {
			return XmlUtils.findFastNodeValue(legalNode,name);
		}catch(Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	@Override	
	public final void parseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap){
		specificParseGrantorGrantee(doc,item, resultMap);
	}
	
	public final void defaultParseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap) {
		try {
			NodeList partyInfo = XmlUtils.xpathQuery(doc, "/TitleDocument/Instrument/PartyInfo");
			
			ArrayList<List> bodyGtor = new ArrayList<List>();
			ArrayList<List> bodyGtee = new ArrayList<List>();
			String nameSplitted = "";
			for (int i = 0; i < partyInfo.getLength(); i++) {
				if(partyInfo.item(i) != null ) {
					String[] names = { "", "", "", "", "", "" };
					
					String partyRole = XmlUtils.findNodeValue(partyInfo.item(i), "PartyRole");
					String firstName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/FirstName");
					String midName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/MiddleName");
					String lastName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/LastName");
					String fullName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/FullName");
					
					firstName = firstName.replaceAll("\\bDECD\\b", "").replaceAll("\\bIND\\s+EXEC\\b", "")
									.replaceAll("\\s+(AND\\s+)?MRS\\b", "");
					midName = midName.replaceAll("\\bDECD\\b", "").replaceAll("\\bIND\\s+EXEC\\b", "")
									.replaceAll("\\s+(AND\\s+)?MRS\\b", "");
					lastName = lastName.replaceAll("\\bDECD\\b", "").replaceAll("(?is)\\s*&\\s*$", "")
									.replaceAll("(?is)\\s+[A|F]KA\\s*$", "").replaceAll("\\s+(AND\\s+)?MRS\\b", "");
					
					fullName = fullName.replaceAll("\\bATT[N|Y]:?\\b", "").replaceAll("(?is)\\A\\s*\\*?N[\\s/]+A\\*?\\s*$", "");
					
					//COPuebloTS B 5457 2.
					if (StringUtils.isEmpty(fullName) && StringUtils.isNotEmpty(lastName) && NameUtils.isNotCompany(lastName)
							&& StringUtils.isEmpty(firstName) && StringUtils.isEmpty(midName)){
						fullName = lastName;
						lastName = "";
					}
					
					//TXParkerTS:200200456767
					if (lastName.matches(".*\\bTRUSTEES?\\s+OF\\s+[A-Z]+\\s*$") && StringUtils.isEmpty(firstName) && StringUtils.isEmpty(firstName)){
						nameSplitted = lastName.replaceAll(".*\\bTRUSTEES\\s+OF\\s+([A-Z]+)\\s*$", "$1");
						lastName = lastName.replaceAll("\\b(TRUSTEES)\\s+OF\\s+(?:[A-Z]+)\\s*$", "$1");
					}
					if (lastName.matches("\\A\\s*REV\\s+FAM\\s+TRUST\\s*$") && StringUtils.isEmpty(firstName) && StringUtils.isEmpty(firstName)
											&& StringUtils.isNotEmpty(nameSplitted)){
						lastName = nameSplitted + " " + lastName;
					}
					
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
								midName += " & " + firstNameNext + " " + lastNameNext; //Bexar TP 20050149862
							}
						}
					}
					if (StringUtils.isEmpty(lastName) && StringUtils.isEmpty(midName) && StringUtils.isNotEmpty(firstName)){
						continue;
					}
					if (StringUtils.isNotEmpty(lastName) && StringUtils.isEmpty(midName) && StringUtils.isNotEmpty(firstName) 
							&& firstName.matches("\\A(?:ETAL)\\s*")){
						continue;
					}
					if (lastName.length() == 1 && StringUtils.isEmpty(midName) && StringUtils.isNotEmpty(firstName)){
						continue;
					}
					if (StringUtils.isEmpty(lastName) && StringUtils.isEmpty(midName) && StringUtils.isEmpty(firstName)
							&& StringUtils.isNotEmpty(RegExUtils
									.getFirstMatch("(?is)\\b(LO?TS?\\b.*?\\bBL?O?C?K|UNIT\\b.*?\\bPHASE)\\b", fullName, 1))) {
						continue;
					}
					//B 4809
					if (NameUtils.isNotCompany(fullName) && fullName.matches("\\s*(\\w+\\s+\\w+\\s+\\w+)\\s+(\\w+\\s+\\w+\\s+\\w+)\\s*")){
						fullName = fullName.replaceAll("\\s*(\\w+\\s+\\w+\\s+\\w+)\\s+(\\w+\\s+\\w+\\s+\\w+)\\s*", "$1 ### $2");
					}
					
					String[] parts = fullName.split("###");
					if (parts.length > 1){
						for (String part:parts){
							names = StringFormats.parseNameDesotoRO(part, true);
							parseNames(part, names, partyRole, bodyGtor, bodyGtee);
							
						}
					} else {		
						if (StringUtils.isNotEmpty(fullName)){
							if (fullName.toLowerCase().contains("an individual")){
								fullName = fullName.replaceAll("(?is)\\bAN\\s+INDIVIDUAL\\b", "").trim();//Pulaski 6010
								names = StringFormats.parseNameDesotoRO(fullName, true);
								parseNames(fullName, names, partyRole, bodyGtor, bodyGtee);
							} else {
								names = StringFormats.parseNameNashville(fullName, true);
								parseNames(fullName, names, partyRole, bodyGtor, bodyGtee);
							}
							
							if (fullName.contains("&") && NameUtils.isNotCompany(fullName)){//CO Pueblo TS, B5457 3.
								names = StringFormats.parseNameNashville(fullName, true);
								parseNames(fullName, names, partyRole, bodyGtor, bodyGtee);
							}
							
						} else {
							String name = (lastName + " " + firstName + " " + midName).trim();
							if (NameUtils.isCompany(name)){
								names[2] = name;
							} else {
								names = StringFormats.parseNameNashville(name, true);
							}
							parseNames(name, names, partyRole, bodyGtor, bodyGtee);
						}
					}
				}
			}
			
			ResultTable rtGtor = new ResultTable();
			ResultTable rtGtee = new ResultTable();
			
			rtGtor = GenericFunctions.storeOwnerInSet(bodyGtor, true);
			//GenericFunctions1.cleanMERS(rtGtor.getBodyRef());
			resultMap.put("GrantorSet", rtGtor);
			
			rtGtee = GenericFunctions2.storeOwnerInSet(bodyGtee, true);
			//GenericFunctions1.cleanMERS(rtGtee.getBodyRef());
			resultMap.put("GranteeSet", rtGtee);
			
			CAGenericDT.fixGrantorGranteeSetDT(resultMap, searchId);
			GenericFunctions2.setGrantorGranteeDT(resultMap, searchId);
			
			try {
				GenericFunctions1.setGranteeLanderTrustee2(resultMap, searchId);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			
		} catch (Exception e) {
			logger.error("Exception in parsing of names in DASLAdapter " + searchId, e);
		}		

	}
	
	@SuppressWarnings("rawtypes")
	public void parseNames(String allName, String[] names, String partyRole, ArrayList<List> bodyGtor, ArrayList<List> bodyGtee) {
		
		String[] suffixes, type, otherType;
		
		type = GenericFunctions.extractAllNamesType(names);
		otherType = GenericFunctions.extractAllNamesOtherType(names);
		suffixes = GenericFunctions.extractNameSuffixes(names);
		if ("Grantor".equalsIgnoreCase(partyRole)){
			GenericFunctions.addOwnerNames(allName, names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), bodyGtor);
		} else if ("Grantee".equalsIgnoreCase(partyRole)){
			GenericFunctions.addOwnerNames(allName, names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), bodyGtee);
		}
		
	}
	/**
	 * There are some cases when the same value of a attribute is parsed from two places, eg from the specified tag and from remarks. 
	 * In this case it was decided that the values should be concatenated. This is implemented only for PropertyidentificationSet
	 * If there are more sets that need this treatment this tag should be 
	 * @param pisVector
	 * @param resultMap
	 */
	private void removeResultmapValues(Vector<PropertyIdentificationSet> pisVector,ResultMap resultMap ){
		for (PropertyIdentificationSet propertyIdentificationSet : pisVector) {
			Iterator iterator = propertyIdentificationSet.keyIterator();
			while(iterator.hasNext()){
				String key = (String) iterator.next();
				String value = propertyIdentificationSet.getAtribute(key);
				if (!StringUtils.isEmpty(value)){
					String rmKey = "PropertyIdentificationSet."+key;
					String resultMapValue = (String) resultMap.get(rmKey);
					if (!StringUtils.isEmpty(resultMapValue)){
						resultMap.put(rmKey, null);
						String cleanValues = ro.cst.tsearch.extractor.legal.LegalDescription.cleanValues(value+" "+resultMapValue, false, true);
						//we put these in  set because it will overwrite what is written in the vector.
						propertyIdentificationSet.setAtribute(key, cleanValues);
					}
				}
			}
		}
	}
	
	/**
	 *If you have no need of this specific parsing just implement the method..else call {@link #defaultParseLegalDescription(Document, ParsedResponse, ResultMap)}}  
	 * @param resultMap TODO
	 */
	public abstract void specificParseLegalDescription(Document doc, ParsedResponse item, ResultMap resultMap);
	
	public abstract void specificParseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap);

	public abstract void specificParseAddress(Document doc, ParsedResponse item, ResultMap resultMap);
	
	protected int[] getModuleIdsForSavingLegal() {
		return new int[]{TSServerInfo.SUBDIVISION_MODULE_IDX, TSServerInfo.ADDRESS_MODULE_IDX};
	}
	protected int[] getModuleIdsForSavingAddress() {
		return new int[]{};
	}
	protected int[] getModuleIdsForSavingName() {
		return new int[]{TSServerInfo.NAME_MODULE_IDX};
	}
	
	@Override
	public String getSaveSearchParametersButton(ServerResponse response) {
		if(response == null 
				|| response.getParsedResponse() == null) {
			return null;
		}
		
		Object possibleModule = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		
		if(!(possibleModule instanceof TSServerInfoModule)) {
			return null;
		}
		
		Search search = getSearch();
		int moduleIdx = ((TSServerInfoModule)possibleModule).getModuleIdx();
		
		if(/*"CO".equals(getDataSite().getStateAbrev()) &&*/ 
				(moduleIdx == TSServerInfo.SUBDIVISION_MODULE_IDX || 
				moduleIdx == TSServerInfo.ADDRESS_MODULE_IDX || 
				moduleIdx == TSServerInfo.NAME_MODULE_IDX) ) {
		
			String key = "SSP_" + System.currentTimeMillis();
			
			/**
			 * Store this for future use (do not worry, it will not be saved)
			 */
			search.setAdditionalInfo(key, possibleModule);
			return "<br/><input type=\"button\" name=\"ButtonSSP\" value=\"Save Search Parameters\" onClick=\"saveSearchedParametersAJAX('" + 
				key + "','" + getServerID() + "')\" class=\"button\" title=\"Save Last Searched Parameters\">";
		} else {
			return null;
		}
	}
	
	public String saveSearchedParameters(TSServerInfoModule module) {
		
		String globalResult = null;
		
		if( TSServerInfo.SUBDIVISION_MODULE_IDX ==  module.getModuleIdx() ) {
			
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
				SearchLogger.info("<br><font color='red'>NO</font> legal was saved from searched parameters for future automatic search<br>", searchId);
				globalResult = "NO legal was saved from searched parameters for future automatic search";
			} else {
				SearchLogger.info("<br><font color='green'><b>Saving</b></font> legal: [" + fullLegal.toString() + "] from searched parameters for future automatic search<br>", searchId);
				globalResult = "Saved legal: [" + fullLegal.toString() + "] from searched parameters for future automatic search";
			}
			
		} else if(ArrayUtils.contains(getModuleIdsForSavingName(), module.getModuleIdx())) {
			List<NameI> alreadySavedNames = getSearchAttributes().getForUpdateSearchGrantorNamesNotNull(getServerID());
			List<NameI> newNamesAdded = new ArrayList<NameI>();
			
			List<NameI> candNames = getNamesFromModule(module);
			if (candNames != null && candNames.size() > 1) {
				// multiple names added in parent site
				for (NameI name : candNames) {
					NameI candName = name;
					if (candName != null) {
						NameI toAdd = candName;
						String candString = candName.getFirstName() + candName.getMiddleName() + candName.getSufix() + candName.getLastName();
						for (NameI reference : alreadySavedNames) {
							if (GenericNameFilter.isMatchGreaterThenScore(candName, reference, NameFilterFactory.NAME_FILTER_THRESHOLD)
									&& (candName.isCompany() == reference.isCompany())) {
								/*
								 * found same name - do not save it
								 */
								String refString = reference.getFirstName() + reference.getMiddleName() + reference.getSufix() + reference.getLastName();
								if (refString.length() <= candString.length()) {
									if (newNamesAdded.contains(reference)) {
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
								break; // no need to check other cases
							}
						}
						if (toAdd != null) {
							alreadySavedNames.add(toAdd);
							newNamesAdded.add(toAdd);
						}
					}
				}
			} else {
				NameI candName = getNameFromModule(module);
				if (candName != null) {
					NameI toAdd = candName;
					String candString = candName.getFirstName() + candName.getMiddleName() + candName.getSufix() + candName.getLastName();
					for (NameI reference : alreadySavedNames) {
						if (GenericNameFilter.isMatchGreaterThenScore(candName, reference, NameFilterFactory.NAME_FILTER_THRESHOLD)
								&& (candName.isCompany() == reference.isCompany())) {
							/*
							 * found same name - do not save it
							 */
							String refString = reference.getFirstName() + reference.getMiddleName() + reference.getSufix() + reference.getLastName();
							if (refString.length() <= candString.length()) {
								if (newNamesAdded.contains(reference)) {
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
							break; // no need to check other cases
						}
					}
					if (toAdd != null) {
						alreadySavedNames.add(toAdd);
						newNamesAdded.add(toAdd);
					}
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
						globalResult += "\nSaving name: [" + nameI.getFullName() + "] from searched parameters for future automatic search";
					}
				}
			}
			
		} else if( TSServerInfo.ADDRESS_MODULE_IDX ==  module.getModuleIdx()) {
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

}

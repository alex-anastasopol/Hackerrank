package ro.cst.tsearch.servers.types;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.PersonalDataStruct;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author mihaib
 * 
 */

public class MOStLouisCityDTG extends MOGenericDTG implements DTLikeAutomatic {

	private static final long serialVersionUID = -39475234234410L;

	public MOStLouisCityDTG(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public MOStLouisCityDTG(long searchId) {
		super(searchId);
	}
	
	@Override
	protected List<RegisterDocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch, int siteType, String stateAbbrev){
		final List<RegisterDocumentI> ret = new ArrayList<RegisterDocumentI>();
		
		List<RegisterDocumentI> listRodocs = m.getRoLikeDocumentList();
		DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
		
		SearchAttributes sa	= search.getSa();
		PartyI owner 		= sa.getOwners();
		
		
		for (RegisterDocumentI doc : listRodocs){
			boolean found = false;
			for (PropertyI prop : doc.getProperties()){
				if ((doc.isOneOf("MORTGAGE", "TRANSFER", "RELEASE")&& applyNameMatch)
						|| (doc.isOneOf("MORTGAGE", "TRANSFER")&& !applyNameMatch)){
					if (prop.hasSubdividedLegal()){
						SubdivisionI sub = prop.getLegal().getSubdivision();
						PersonalDataStruct ret1 = new PersonalDataStruct("subdivided");
						ret1.lot = sub.getLot();
						ret1.block = sub.getBlock();
						ret1.setNcbNumber(sub.getNcbNumber());
						ret1.setSubdivisionName(sub.getName());
						
						boolean nameMatched = false;
						
						if (applyNameMatch){
							if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						
						if((nameMatched || !applyNameMatch) 
								&& !StringUtils.isEmpty(ret1.lot) 
								&& !StringUtils.isEmpty(ret1.getNcbNumber()) 
								&& !StringUtils.isEmpty(ret1.getSubdivisionName())){
							ret.add(doc);
							found = true;
							break;
						}
					} else if (prop.hasTownshipLegal()){
						TownShipI sub = prop.getLegal().getTownShip();
						PersonalDataStruct ret1 = new PersonalDataStruct("sectional");
						ret1.setSection(sub.getSection());
						ret1.setTownship(sub.getTownship());
						ret1.setRange(sub.getRange());
						int qo = sub.getQuarterOrder();
						String qv = sub.getQuarterValue();
						if (qo > 0){
							ret1.quarterOrder = qo+"";
						}
						ret1.quarterValue = qv;
						ret1.setArb(sub.getArb());
						ret1.arbLot = sub.getArbLot();
						ret1.arbBlock = sub.getArbBlock();
						ret1.arbPage = sub.getArbPage();
						ret1.arbBook = sub.getArbBook();
						boolean nameMatched = false;
						
						if (applyNameMatch){
							if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD)
									|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						
						if ((nameMatched || !applyNameMatch) && FLSubdividedBasedDASLDT.isCompleteLegal(ret1, qo, qv, siteType, stateAbbrev)){
							ret.add(doc);
							found = true;
							break;
						}
					}
				}
			}
			if (found){
				break;
			}
		}
		
		return ret;
	}
	
	@Override
	 protected LegalI getLegalFromModule(TSServerInfoModule module){
		 LegalI legal = null;
		 SubdivisionI subdivision = null;
		 TownShipI townShip = null;
			
		 if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX && module.getFunctionCount() > 21){
			 subdivision = new Subdivision();
				
			 String subdivisionName = module.getFunction(12).getParamValue().trim();
			 subdivision.setName(subdivisionName);
			 subdivision.setLot(module.getFunction(2).getParamValue().trim());
			 subdivision.setBlock(module.getFunction(3).getParamValue().trim());
			 subdivision.setPlatBook(module.getFunction(4).getParamValue().trim());
			 subdivision.setPlatPage(module.getFunction(5).getParamValue().trim());
			 subdivision.setNcbNumber(module.getFunction(21).getParamValue().trim());
		 }
		 if (module.getModuleIdx() == TSServerInfo.SECTION_LAND_MODULE_IDX && module.getFunctionCount() > 3){
			 townShip = new TownShip();
				
			 townShip.setSection(module.getFunction(0).getParamValue().trim());
			 townShip.setTownship(module.getFunction(1).getParamValue().trim());
			 townShip.setRange(module.getFunction(2).getParamValue().trim());
			
		 }
		 if (subdivision != null){
			 legal = new Legal();
			 legal.setSubdivision(subdivision);
		 }
		 
		 if (townShip != null){
			 if (legal == null){
				 legal = new Legal();
			 }
			 legal.setTownShip(townShip);
		 }
		 
		 return legal;
	}
	
	@Override
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
					boolean hasRefCityBlock = false;
					if(StringUtils.isNotEmpty(refSubdivision.getNcbNumber())) {
						hasRefCityBlock = true;
						if(StringUtils.isNotEmpty(candSubdivision.getNcbNumber())
								&& !refSubdivision.getNcbNumber().equals(candSubdivision.getNcbNumber())) {
							continue;
						}
					}
					
					boolean hasRefSubdivision = false;
					if(StringUtils.isNotEmpty(refSubdivision.getName())) {
						if(StringUtils.isNotEmpty(candSubdivision.getName())
								&& !refSubdivision.getName().equals(candSubdivision.getName())) {
							continue;
						}
						hasRefSubdivision = true;
					}
					
					if(!hasRefCityBlock 
							&& !hasRefSubdivision
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
}

package ro.cst.tsearch.search.filter.newfilters.legal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.bean.LegalSKLDIteratorEntry;
import ro.cst.tsearch.search.bean.UnplattedSKLDIteratorEntry;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.data.LegalStructDTG;
import ro.cst.tsearch.search.iterator.data.LegalStructPI;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

public class GenericMultipleLegalFilter extends FilterResponse {

	
	private static final long serialVersionUID = 1L;

	// control if a certain field is used or not
	protected boolean enableSection = true;
	protected boolean enableTownship = true;
	protected boolean enableRange = true;
	protected boolean enableLot = true;
	protected boolean enableBlock = true;
	protected boolean enableArb = true;
	protected boolean enablePlatBook = true;
	protected boolean enablePlatPage = true;
	protected boolean enableUnit = true;
	protected boolean enablePhase = true;
	protected boolean enableSectionJustForUnplated = false;
	protected boolean enableLotUnitFullEquivalence = false;
	protected boolean enableDistrict = false;
	protected boolean enableAbs = false;
	
	
	protected boolean markIfCandidatesAreEmpty = false;
	
	protected boolean useLegalFromSearchPage = false;
	
	List<GenericLegal> filterList = new ArrayList<GenericLegal>();
	private String additionalInfoKey = AdditionalInfoKeys.TS_LOOK_UP_DATA;
	protected HashSet<String> ignoreLotWhenServerDoctypeIs = null;
	protected boolean ignoreLotAndBlockForPreferentialDoctype = false;
	
	
	public void disableAll(){
		enableSection = false;
		enableTownship = false;
		enableRange = false;
		enableLot = false;
		enableBlock = false;
		enablePlatBook = false;
		enablePlatPage = false;
		enableUnit = false;
		enablePhase = false;
		enableSectionJustForUnplated = false;
		enableDistrict = false;
		enableAbs = false;
	}
	
	public GenericMultipleLegalFilter(long searchId) {
		super(searchId);
		setInitAgain(true);
		setThreshold(new BigDecimal(0.7));
	}

	public boolean isEnableSection() {
		return enableSection;
	}

	public void setEnableSection(boolean enableSection) {
		this.enableSection = enableSection;
	}

	public boolean isEnableTownship() {
		return enableTownship;
	}

	public void setEnableTownship(boolean enableTownship) {
		this.enableTownship = enableTownship;
	}

	public boolean isEnableRange() {
		return enableRange;
	}

	public void setEnableRange(boolean enableRange) {
		this.enableRange = enableRange;
	}

	public boolean isEnableLot() {
		return enableLot;
	}

	public void setEnableLot(boolean enableLot) {
		this.enableLot = enableLot;
	}

	public boolean isEnableBlock() {
		return enableBlock;
	}

	public void setEnableBlock(boolean enableBlock) {
		this.enableBlock = enableBlock;
	}

	public boolean isEnableArb() {
		return enableArb;
	}

	public void setEnableArb(boolean enableArb) {
		this.enableArb = enableArb;
	}

	public boolean isEnablePlatBook() {
		return enablePlatBook;
	}

	public void setEnablePlatBook(boolean enablePlatBook) {
		this.enablePlatBook = enablePlatBook;
	}

	public boolean isEnablePlatPage() {
		return enablePlatPage;
	}

	public void setEnablePlatPage(boolean enablePlatPage) {
		this.enablePlatPage = enablePlatPage;
	}

	public boolean isEnableUnit() {
		return enableUnit;
	}

	public void setEnableUnit(boolean enableUnit) {
		this.enableUnit = enableUnit;
	}

	public boolean isEnablePhase() {
		return enablePhase;
	}

	public void setEnablePhase(boolean enablePhase) {
		this.enablePhase = enablePhase;
	}

	public boolean isEnableSectionJustForUnplated() {
		return enableSectionJustForUnplated;
	}

	public void setEnableSectionJustForUnplated(boolean enableSectionJustForUnplated) {
		this.enableSectionJustForUnplated = enableSectionJustForUnplated;
	}

	public boolean isEnableLotUnitFullEquivalence() {
		return enableLotUnitFullEquivalence;
	}

	public void setEnableLotUnitFullEquivalence(boolean enableLotUnitFullEquivalence) {
		this.enableLotUnitFullEquivalence = enableLotUnitFullEquivalence;
	}

	public boolean isMarkIfCandidatesAreEmpty() {
		return markIfCandidatesAreEmpty;
	}

	public void setMarkIfCandidatesAreEmpty(boolean markIfCandidatesAreEmpty) {
		this.markIfCandidatesAreEmpty = markIfCandidatesAreEmpty;
	}
	
	public boolean getIgnoreLotAndBlockForPreferentialDoctype() {
		return ignoreLotAndBlockForPreferentialDoctype;
	}

	public void setIgnoreLotAndBlockForPreferentialDoctype(boolean ignoreLotAndBlockForPreferentialDoctype) {
		this.ignoreLotAndBlockForPreferentialDoctype = ignoreLotAndBlockForPreferentialDoctype;
	}
	
	public boolean isEnableDistrict() {
		return enableDistrict;
	}

	public void setEnableDistrict(boolean enableDistrict) {
		this.enableDistrict = enableDistrict;
	}
	
	public boolean isEnableAbs() {
		return enableAbs;
	}

	public void setEnableAbs(boolean enableAbs) {
		this.enableAbs = enableAbs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init() {
		filterList.clear();
		
		GenericLegal filterLegal = null;
		Search global = getSearch();
		Set<String> allKeys = new HashSet<String>();
		if(useLegalFromSearchPage){
			StringBuilder findSameFilterKey = new StringBuilder();
			String section = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_SEC);
			String township = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_TWN);
			String range = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_RNG);
			String arb = global.getSa().getAtribute(SearchAttributes.ARB);
			String district = global.getSa().getAtribute(SearchAttributes.LD_DISTRICT);
			String abs = global.getSa().getAtribute(SearchAttributes.LD_ABS_NO);
			
			String lot = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			String block = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			String platBook = global.getSa().getAtribute(SearchAttributes.LD_BOOKNO);
			String platPage = global.getSa().getAtribute(SearchAttributes.LD_PAGENO);
			
			if(isEnableSection() && ro.cst.tsearch.utils.StringUtils.isNotEmpty(section)) {
				findSameFilterKey.append("Section:").append(section);
			}
			if(isEnableTownship() && ro.cst.tsearch.utils.StringUtils.isNotEmpty(township)) {
				findSameFilterKey.append("Township:").append(township);
			}
			if(isEnableRange() && ro.cst.tsearch.utils.StringUtils.isNotEmpty(range)) {
				findSameFilterKey.append("Range:").append(range);
			}
			if(isEnableArb() && ro.cst.tsearch.utils.StringUtils.isNotEmpty(arb)) {
				findSameFilterKey.append("Arb:").append(arb);
			}
			if(isEnableAbs() && ro.cst.tsearch.utils.StringUtils.isNotEmpty(abs)) {
				findSameFilterKey.append("Abs:").append(abs);
			}
			
			if(isEnableLot() && ro.cst.tsearch.utils.StringUtils.isNotEmpty(lot)) {
				findSameFilterKey.append("Lot:").append(lot);
			}
			if(isEnableBlock() && ro.cst.tsearch.utils.StringUtils.isNotEmpty(block)) {
				findSameFilterKey.append("Block:").append(block);
			}
			if(isEnablePlatBook() && ro.cst.tsearch.utils.StringUtils.isNotEmpty(platBook)) {
				findSameFilterKey.append("PlatBook:").append(platBook);
			}
			if(isEnablePlatPage() && ro.cst.tsearch.utils.StringUtils.isNotEmpty(platPage)) {
				findSameFilterKey.append("PlatPage:").append(platPage);
			}
			if(isEnableDistrict() && StringUtils.isNotEmpty(district)) {
				findSameFilterKey.append("District:").append(district);
			}
			
			if(findSameFilterKey.length() > 0 && allKeys.add(findSameFilterKey.toString())) {
				SearchAttributes saFromLegal = new SearchAttributes(searchId);
				saFromLegal.setAtribute(SearchAttributes.LD_LOTNO, global.getSa().getAtribute(SearchAttributes.LD_LOTNO));
				saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_BLOCK, global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK));
				saFromLegal.setAtribute(SearchAttributes.LD_BOOKNO, global.getSa().getAtribute(SearchAttributes.LD_BOOKNO));
				saFromLegal.setAtribute(SearchAttributes.LD_PAGENO, global.getSa().getAtribute(SearchAttributes.LD_PAGENO));
				
				saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_RNG, range);
				saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_TWN, township);
				saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_SEC, section);
				saFromLegal.setAtribute(SearchAttributes.ARB, arb);
				saFromLegal.setAtribute(SearchAttributes.LD_ABS_NO, abs);
				saFromLegal.setAtribute(SearchAttributes.LD_DISTRICT, district);
				
				filterLegal = getLegalFilter(saFromLegal);
		    	
				filterList.add(filterLegal);
			}
			
			
		}
		
		
		List<LegalSKLDIteratorEntry> list = 
			(List<LegalSKLDIteratorEntry>)global.getAdditionalInfo("LegalSKLDIteratorList");
		if(list != null) {
			for (LegalSKLDIteratorEntry legalSKLDIteratorEntry : list) {
				String key = legalSKLDIteratorEntry.getLot() + legalSKLDIteratorEntry.getBlock() + 
					legalSKLDIteratorEntry.getMapIdBook() + legalSKLDIteratorEntry.getMapIdPage();
				if(StringUtils.isNotBlank(key) 
						&& !"ALL".equalsIgnoreCase(legalSKLDIteratorEntry.getLot()) 
						&& !"ALL".equalsIgnoreCase(legalSKLDIteratorEntry.getBlock()) ) {
					
					StringBuilder findSameFilterKey = new StringBuilder();
					if(isEnableLot() && StringUtils.isNotBlank(legalSKLDIteratorEntry.getLot())) {
						findSameFilterKey.append("Lot:").append(legalSKLDIteratorEntry.getLot());
					}
					if(isEnableBlock() && StringUtils.isNotBlank(legalSKLDIteratorEntry.getBlock()) ) {
						findSameFilterKey.append("Block:").append(legalSKLDIteratorEntry.getBlock());
					}
					if(isEnablePlatBook() && StringUtils.isNotBlank(legalSKLDIteratorEntry.getMapIdBook()) ) {
						findSameFilterKey.append("PlatBook:").append(legalSKLDIteratorEntry.getMapIdBook());
					}
					if(isEnablePlatPage() && StringUtils.isNotBlank(legalSKLDIteratorEntry.getMapIdPage())) {
						findSameFilterKey.append("PlatPage:").append(legalSKLDIteratorEntry.getMapIdPage());
					}
					if(allKeys.add(findSameFilterKey.toString())) {
						SearchAttributes saFromLegal = new SearchAttributes(searchId);
						saFromLegal.setAtribute(SearchAttributes.LD_LOTNO, legalSKLDIteratorEntry.getLot());
						saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_BLOCK, legalSKLDIteratorEntry.getBlock());
						saFromLegal.setAtribute(SearchAttributes.LD_BOOKNO, legalSKLDIteratorEntry.getMapIdBook());
						saFromLegal.setAtribute(SearchAttributes.LD_PAGENO, legalSKLDIteratorEntry.getMapIdPage());
						filterLegal = new GenericLegal(saFromLegal, "", searchId);
						filterLegal.disableAll();
						filterLegal.setThreshold(new BigDecimal("0.7"));
						filterLegal.setEnableLot(isEnableLot());
						filterLegal.setEnableBlock(isEnableBlock());
						filterLegal.setEnablePlatBook(isEnablePlatBook());
				    	filterLegal.setEnablePlatPage(isEnablePlatPage());
						filterLegal.setMarkIfCandidatesAreEmpty(true);
						filterLegal.setEnableLotUnitFullEquivalence(isEnableLotUnitFullEquivalence());
						filterLegal.getScoreOneRow(null);
						filterList.add(filterLegal);
					}
				}
			}
		}
			
		
		List<UnplattedSKLDIteratorEntry> listUnplattedSKLDIteratorEntries = 
			(List<UnplattedSKLDIteratorEntry>)global.getAdditionalInfo("UnplattedSKLDIteratorList");
		if(listUnplattedSKLDIteratorEntries != null) {
			for (UnplattedSKLDIteratorEntry legalSKLDIteratorEntry : listUnplattedSKLDIteratorEntries) {
				
				String key = legalSKLDIteratorEntry.getRange() + legalSKLDIteratorEntry.getTownship() + legalSKLDIteratorEntry.getSection();
				if(StringUtils.isNotBlank(key)) {
					SearchAttributes saFromLegal = new SearchAttributes(searchId);
					saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_RNG, legalSKLDIteratorEntry.getRange());
					saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_TWN, legalSKLDIteratorEntry.getTownship());
					saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_SEC, legalSKLDIteratorEntry.getSection());
					filterLegal = new GenericLegal(saFromLegal, "", searchId);
					filterLegal.disableAll();
					filterLegal.setThreshold(new BigDecimal("0.7"));
			    	filterLegal.setEnableSection(isEnableSection());
			    	filterLegal.setEnableTownship(isEnableTownship());
			    	filterLegal.setEnableRange(isEnableRange());
					filterLegal.setMarkIfCandidatesAreEmpty(true);
					filterLegal.setEnableLotUnitFullEquivalence(isEnableLotUnitFullEquivalence());
					filterLegal.getScoreOneRow(null);
					filterList.add(filterLegal);
				}
			}
		}
		
		
		Object additionalInfo = global.getAdditionalInfo(additionalInfoKey);
		
		if(additionalInfo instanceof Set) {
			Set dataSet = (Set)additionalInfo;
			if(!dataSet.isEmpty()) {
				Object dataStructObject = dataSet.iterator().next();
				if(dataStructObject instanceof LegalStruct) {

					List<GenericLegal> filterListTemp = new ArrayList<GenericLegal>();
					for (LegalStruct struct : (Set<LegalStruct>)dataSet) {
						String key = struct.getSection() + struct.getTownship() + struct.getRange();
						String key1 = struct.getLot() + struct.getBlock()+ struct.getPlatBook() + struct.getPlatPage();
						
						if(StringUtils.isNotBlank(key)||StringUtils.isNotBlank(key1)) {
							SearchAttributes saFromLegal = new SearchAttributes(searchId);
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_RNG, struct.getRange());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_TWN, struct.getTownship());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_SEC, struct.getSection());
							saFromLegal.setAtribute(SearchAttributes.LD_LOTNO, struct.getLot());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_BLOCK, struct.getBlock());
							saFromLegal.setAtribute(SearchAttributes.LD_BOOKNO,struct.getPlatBook());
							saFromLegal.setAtribute(SearchAttributes.LD_PAGENO, struct.getPlatPage());
							saFromLegal.setAtribute(SearchAttributes.ARB, struct.getArb());
							saFromLegal.setAtribute(SearchAttributes.LD_DISTRICT, struct.getDistrict());
							saFromLegal.setAtribute(SearchAttributes.LD_ABS_NO, struct.getAbs());
							
							filterLegal = getLegalFilter(saFromLegal);
							
							StringBuilder findSameFilterKey = new StringBuilder();
							if(isEnableSection()) {
								findSameFilterKey.append("Section:").append(struct.getSection());
							}
							if(isEnableTownship()) {
								findSameFilterKey.append("Township:").append(struct.getTownship());
							}
							if(isEnableRange()) {
								findSameFilterKey.append("Range:").append(struct.getRange());
							}
							if(isEnableLot()) {
								findSameFilterKey.append("Lot:").append(struct.getLot());
							}
							if(isEnableBlock()) {
								findSameFilterKey.append("Block:").append(struct.getBlock());
							}
							if(isEnablePlatBook()) {
								findSameFilterKey.append("PlatBook:").append(struct.getPlatBook());
							}
							if(isEnablePlatPage()) {
								findSameFilterKey.append("PlatPage:").append(struct.getPlatPage());
							}
							if(isEnableArb()) {
								findSameFilterKey.append("Arb:").append(struct.getArb());
							}
							if(isEnableDistrict()) {
								findSameFilterKey.append("District:").append(struct.getDistrict());
							}
							if(isEnableAbs()) {
								findSameFilterKey.append("Abs:").append(struct.getAbs());
							}
							if(allKeys.add(findSameFilterKey.toString())) {
								filterListTemp.add(filterLegal);
							}
						}
					}
					filterList.addAll(filterListTemp);
				
				} else if(dataStructObject instanceof FLSubdividedBasedDASLDT.PersonalDataStruct) {
					List<GenericLegal> filterListTemp = new ArrayList<GenericLegal>();
					for (FLSubdividedBasedDASLDT.PersonalDataStruct struct : (Set<FLSubdividedBasedDASLDT.PersonalDataStruct>)dataSet) {
						String key = struct.getSection() + struct.getTownship() + struct.getRange();
						String key1 = struct.getLot() + struct.getBlock()+ struct.getPlatBook() + struct.getPlatPage();
						
						if(StringUtils.isNotBlank(key)||StringUtils.isNotBlank(key1)) {
							SearchAttributes saFromLegal = new SearchAttributes(searchId);
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_RNG, struct.getRange());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_TWN, struct.getTownship());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_SEC, struct.getSection());
							saFromLegal.setAtribute(SearchAttributes.LD_LOTNO, struct.getLot());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_BLOCK, struct.getBlock());
							saFromLegal.setAtribute(SearchAttributes.LD_BOOKNO,struct.getPlatBook());
							saFromLegal.setAtribute(SearchAttributes.LD_PAGENO, struct.getPlatPage());
							saFromLegal.setAtribute(SearchAttributes.ARB, struct.getArb());
							
							filterLegal = getLegalFilter(saFromLegal);
							
							StringBuilder findSameFilterKey = new StringBuilder();
							if(isEnableSection()) {
								findSameFilterKey.append("Section:").append(struct.getSection());
							}
							if(isEnableTownship()) {
								findSameFilterKey.append("Township:").append(struct.getTownship());
							}
							if(isEnableRange()) {
								findSameFilterKey.append("Range:").append(struct.getRange());
							}
							if(isEnableLot()) {
								findSameFilterKey.append("Lot:").append(struct.getLot());
							}
							if(isEnableBlock()) {
								findSameFilterKey.append("Block:").append(struct.getBlock());
							}
							if(isEnablePlatBook()) {
								findSameFilterKey.append("PlatBook:").append(struct.getPlatBook());
							}
							if(isEnablePlatPage()) {
								findSameFilterKey.append("PlatPage:").append(struct.getPlatPage());
							}
							if(isEnableArb()) {
								findSameFilterKey.append("Arb:").append(struct.getArb());
							}
							if(allKeys.add(findSameFilterKey.toString())) {
								filterListTemp.add(filterLegal);
							}
						}
					}
					filterList.addAll(filterListTemp);
				} else if(dataStructObject instanceof LegalStructDTG){
					List<GenericLegal> filterListTemp = new ArrayList<GenericLegal>();
					for (LegalStructDTG struct : (Set<LegalStructDTG>)dataSet){
						String key = struct.getSection() + struct.getTownship() + struct.getRange() + struct.getArb();
						String key1 = struct.getLot() + struct.getBlock()+ struct.getPlatBook() + struct.getPlatPage();
						
						if (StringUtils.isNotBlank(key) || StringUtils.isNotBlank(key1)){
							SearchAttributes saFromLegal = new SearchAttributes(searchId);
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_RNG, struct.getRange());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_TWN, struct.getTownship());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_SEC, struct.getSection());
							saFromLegal.setAtribute(SearchAttributes.LD_LOTNO, struct.getLot());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_BLOCK, struct.getBlock());
							saFromLegal.setAtribute(SearchAttributes.LD_BOOKNO,struct.getPlatBook());
							saFromLegal.setAtribute(SearchAttributes.LD_PAGENO, struct.getPlatPage());
							saFromLegal.setAtribute(SearchAttributes.ARB, struct.getArb());
							
							filterLegal = getLegalFilter(saFromLegal);
							
							StringBuilder findSameFilterKey = new StringBuilder();
							if (isEnableSection()){
								findSameFilterKey.append("Section:").append(struct.getSection());
							}
							if (isEnableTownship()){
								findSameFilterKey.append("Township:").append(struct.getTownship());
							}
							if (isEnableRange()){
								findSameFilterKey.append("Range:").append(struct.getRange());
							}
							if (isEnableLot()){
								findSameFilterKey.append("Lot:").append(struct.getLot());
							}
							if (isEnableBlock()){
								findSameFilterKey.append("Block:").append(struct.getBlock());
							}
							if (isEnablePlatBook()){
								findSameFilterKey.append("PlatBook:").append(struct.getPlatBook());
							}
							if (isEnablePlatPage()){
								findSameFilterKey.append("PlatPage:").append(struct.getPlatPage());
							}
							if (isEnableArb()){
								findSameFilterKey.append("Arb:").append(struct.getArb());
							}
							if (allKeys.add(findSameFilterKey.toString())){
								filterListTemp.add(filterLegal);
							}
						}
					}
					filterList.addAll(filterListTemp);
				} else if(dataStructObject instanceof LegalStructPI){
					List<GenericLegal> filterListTemp = new ArrayList<GenericLegal>();
					for (LegalStructPI struct : (Set<LegalStructPI>)dataSet){
						String key = struct.getSection() + struct.getTownship() + struct.getRange() + struct.getArb();
						String key1 = struct.getLot() + struct.getBlock()+ struct.getMapBook() + struct.getMapPage();
						
						if (StringUtils.isNotBlank(key) || StringUtils.isNotBlank(key1)){
							SearchAttributes saFromLegal = new SearchAttributes(searchId);
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_RNG, struct.getRange());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_TWN, struct.getTownship());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_SEC, struct.getSection());
							saFromLegal.setAtribute(SearchAttributes.LD_LOTNO, struct.getLot());
							saFromLegal.setAtribute(SearchAttributes.LD_SUBDIV_BLOCK, struct.getBlock());
							saFromLegal.setAtribute(SearchAttributes.LD_BOOKNO,struct.getMapBook());
							saFromLegal.setAtribute(SearchAttributes.LD_PAGENO, struct.getMapPage());
							saFromLegal.setAtribute(SearchAttributes.ARB, struct.getArb());
							
							filterLegal = getLegalFilter(saFromLegal);
							
							StringBuilder findSameFilterKey = new StringBuilder();
							if (isEnableSection()){
								findSameFilterKey.append("Section:").append(struct.getSection());
							}
							if (isEnableTownship()){
								findSameFilterKey.append("Township:").append(struct.getTownship());
							}
							if (isEnableRange()){
								findSameFilterKey.append("Range:").append(struct.getRange());
							}
							if (isEnableLot()){
								findSameFilterKey.append("Lot:").append(struct.getLot());
							}
							if (isEnableBlock()){
								findSameFilterKey.append("Block:").append(struct.getBlock());
							}
							if (isEnablePlatBook()){
								findSameFilterKey.append("PlatBook:").append(struct.getMapBook());
							}
							if (isEnablePlatPage()){
								findSameFilterKey.append("PlatPage:").append(struct.getMapPage());
							}
							if (isEnableArb()){
								findSameFilterKey.append("Arb:").append(struct.getArb());
							}
							if (allKeys.add(findSameFilterKey.toString())){
								filterListTemp.add(filterLegal);
							}
						}
					}
					filterList.addAll(filterListTemp);
				}
			}
		}
		
		compressSimilarFilters();
		
	}

	protected GenericLegal getLegalFilter(SearchAttributes saFromLegal) {
		GenericLegal filterLegal;
		filterLegal = new GenericLegal(saFromLegal, "", searchId);
		filterLegal.disableAll();
		filterLegal.setThreshold(new BigDecimal("0.7"));
		filterLegal.setEnableSection(isEnableSection());
		filterLegal.setEnableTownship(isEnableTownship());
		filterLegal.setEnableRange(isEnableRange());
		filterLegal.setEnableLot(isEnableLot());
		filterLegal.setEnableBlock(isEnableBlock());
		filterLegal.setEnablePlatBook(isEnablePlatBook());
		filterLegal.setEnablePlatPage(isEnablePlatPage());
		filterLegal.setEnableArb(isEnableArb());
		filterLegal.setEnableAbs(isEnableAbs());
		filterLegal.setMarkIfCandidatesAreEmpty(true);
		filterLegal.setIgnoreLotWhenServerDoctypeIs(getIgnoreLotWhenServerDoctypeIs());
		filterLegal.setIgnoreLotAndBlockForPreferentialDoctype(getIgnoreLotAndBlockForPreferentialDoctype());
		filterLegal.setEnableLotUnitFullEquivalence(isEnableLotUnitFullEquivalence());
		filterLegal.setEnableDistrict(isEnableDistrict());
		filterLegal.getScoreOneRow(null);
		return filterLegal;
	}
	
	private void compressSimilarFilters() {
		List<GenericLegal> tempList = new ArrayList<GenericLegal>();
		for (int i = 0; i < filterList.size(); i++) {
			GenericLegal legalFilter = filterList.get(i);
			boolean isContaintedInList = false;
			for (int j = i + 1; j < filterList.size(); j++) {
				GenericLegal legalFilterInner = filterList.get(j);
				if(legalFilter != legalFilterInner) {	
					//not the same object :)
					if(legalFilter.isContainedIn(legalFilterInner)) {
						isContaintedInList = true;
						break;
					}
				}
			}
			if(!isContaintedInList) {
				boolean isContaintedInListTemp = false;
				for (GenericLegal genericLegal : tempList) {
					if(legalFilter.isContainedIn(genericLegal)) {
						isContaintedInListTemp = true;
						break;
					}
				}
				if(!isContaintedInListTemp) {
					tempList.add(legalFilter);
				}
			}
		}
		filterList.clear();
		filterList.addAll(tempList);
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		BigDecimal result = ATSDecimalNumberFormat.NA;
		for (GenericLegal filterLegal : filterList) {
			BigDecimal resultFilterLegal = filterLegal.getScoreOneRow(row);
			if(resultFilterLegal.doubleValue() >= getThreshold().doubleValue()) {
				return resultFilterLegal;
			} else if (resultFilterLegal == ATSDecimalNumberFormat.NA) {
				//ignore
			} else {	//less that the threshold
				if(result.doubleValue() < resultFilterLegal.doubleValue()) {
					result = resultFilterLegal;
				}
			}
		}
		if(result.equals(ATSDecimalNumberFormat.NA)) {
			if(isMarkIfCandidatesAreEmpty()) {
				return getThreshold();
			} else {
				return ATSDecimalNumberFormat.ONE;
			}
		}
		return result;
	}
	
	@Override
    public String getFilterName(){
    	return "Filter by Legal";
    }
	
	@Override
	public String getFilterCriteria(){

		String retVal = null;
		for (GenericLegal filterLegal : filterList) {
			if(retVal == null) {
				retVal = filterLegal.getFilterCriteria();
			} else {
				retVal += " or " + filterLegal.getFilterCriteria();
			}
		}
    	
		if(retVal != null) {
			if(getIgnoreLotWhenServerDoctypeIs() != null && getIgnoreLotWhenServerDoctypeIs().size() > 0) {
				retVal += " (Ignoring lot for documents with server doctype = " + getIgnoreLotWhenServerDoctypeIs().toString() + ")";
			}
			return retVal;
		}
		
    	return "Legal='<no legal to test against>'";
    }

	public boolean isUseLegalFromSearchPage() {
		return useLegalFromSearchPage;
	}

	public void setUseLegalFromSearchPage(boolean useLegalFromSearchPage) {
		this.useLegalFromSearchPage = useLegalFromSearchPage;
	}
	
	/**
     * if any row has the score < threshold the NOT_PERFECT_MATCH_WARNING_FIRST error is set
     * and the next link append results will be disabled
     */
    @SuppressWarnings("rawtypes")
	protected void analyzeResult(ServerResponse sr, Vector rez) throws ServerResponseException
    {           
    	if(getFilterForNextFollowLinkLimit() >= 0) {
    		if(getFilterForNextFollowLinkLimit() == 0) {
    			if( rez.size() != getFilterForNextFollowLinkLimit()) {
    				//filtered --> stop, do not go to next results
		            sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST);
    			}
    		} else {
		        if( rez.size() < getFilterForNextFollowLinkLimit() 
		        		|| rez.size() == 0  )
		        {
		            //filtered --> stop, do not go to next results
		            sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST);
		        }
    		}
    	} else {
    		super.analyzeResult(sr, rez);
    	}
    }

	public String getAdditionalInfoKey() {
		return additionalInfoKey;
	}

	public void setAdditionalInfoKey(String additionalInfoKey) {
		this.additionalInfoKey = additionalInfoKey;
	}
	public HashSet<String> getIgnoreLotWhenServerDoctypeIs() {
		return ignoreLotWhenServerDoctypeIs;
	}

	public void setIgnoreLotWhenServerDoctypeIs(
			HashSet<String> ignoreLotWhenServerDoctypeIs) {
		this.ignoreLotWhenServerDoctypeIs = ignoreLotWhenServerDoctypeIs;
	}
    
}

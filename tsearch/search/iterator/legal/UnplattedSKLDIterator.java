package ro.cst.tsearch.search.iterator.legal;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.bean.UnplattedSKLDIteratorEntry;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class UnplattedSKLDIterator extends GenericRuntimeIterator<UnplattedSKLDIteratorEntry> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private LegalSKLDIterator legalSKLDIterator = null;
	
	public UnplattedSKLDIterator(long searchId) {
		super(searchId);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<UnplattedSKLDIteratorEntry> createDerrivations() {
		
		if(legalSKLDIterator != null) {
			if(legalSKLDIterator.size() > 0) {
				return new Vector<UnplattedSKLDIteratorEntry>();
			}
		}
		
		Search global = getSearch();
		SearchAttributes sa = global.getSa();
		
		
		if(StringUtils.isNotEmpty(sa.getAtribute(SearchAttributes.LD_LOTNO)) 
			|| StringUtils.isNotEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK))
			|| StringUtils.isNotEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME))
			|| StringUtils.isNotEmpty(sa.getAtribute(SearchAttributes.LD_BOOKNO))
			|| StringUtils.isNotEmpty(sa.getAtribute(SearchAttributes.LD_PAGENO))
			|| "true".equalsIgnoreCase(sa.getAtribute(SearchAttributes.IS_CONDO))) {
			return new Vector<UnplattedSKLDIteratorEntry>();
		}
		
		List<UnplattedSKLDIteratorEntry> derivations = 
			(List<UnplattedSKLDIteratorEntry>) global.getAdditionalInfo("UnplattedSKLDIteratorList");
		if(derivations == null) {
			derivations = new Vector<UnplattedSKLDIteratorEntry>();
			DocumentsManagerI documentsManagerI = global.getDocManager();
			try {
				documentsManagerI.getAccess();
				//List<DocumentI> availableSKDocuments = documentsManagerI.getDocumentsWithDataSource(false, "SK");
				
				List<DocumentI> listRodocs = getGoodDocumentsOrForCurrentOwner(documentsManagerI,global,true);
				
				if(listRodocs==null||listRodocs.size()==0){
					listRodocs = getGoodDocumentsOrForCurrentOwner(documentsManagerI, global, false);
				}
				
				if(listRodocs==null||listRodocs.size()==0){
					listRodocs = documentsManagerI.getDocumentsWithDataSource(true, "SK");
				}
				
				
				for (DocumentI documentI : listRodocs) {
					if(!documentI.isOneOf("PLAT","RESTRICTION","EASEMENT","MASTERDEED","COURT","LIEN","CORPORATION","AFFIDAVIT", "CCER")) {
						for (PropertyI propertyI : documentI.getProperties()) {
							TownShipI townShipI = propertyI.getLegal().getTownShip();
							UnplattedSKLDIteratorEntry entry = new UnplattedSKLDIteratorEntry();
							entry.setSubdivision(townShipI);
							if(!derivations.contains(entry) && entry.isEntryValid()) {
								derivations.add(entry);
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
				
				
				try {
					DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
					long miServerId = dataSite.getServerId();
					for (LegalI legal : global.getSa().getForUpdateSearchLegalsNotNull(miServerId)) {
						TownShipI townShipI = legal.getTownShip();
						UnplattedSKLDIteratorEntry entry = new UnplattedSKLDIteratorEntry();
						entry.setSubdivision(townShipI);
						if(!derivations.contains(entry) && entry.isEntryValid()) {
							derivations.add(entry);
						}
					}
				} catch (Exception e) {
					logger.error("Error loading names for Update saved from Parent Site", e);
				}
				
				
				
			//}
			
			global.setAdditionalInfo("UnplattedSKLDIteratorList", derivations);
		}
		
		return derivations;
	}
	
	protected static List<DocumentI> getGoodDocumentsOrForCurrentOwner(DocumentsManagerI m, Search search, boolean applyNameMatch){
		final List<DocumentI> ret = new ArrayList<DocumentI>();
		
		List<DocumentI> listRodocs = m.getDocumentsWithDataSource(true, "SK");;
		DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
		
		SearchAttributes sa	= search.getSa();
		PartyI owner 		= sa.getOwners();
		
		for(DocumentI docI:listRodocs){
			RegisterDocumentI doc = (RegisterDocumentI)docI;
			if((doc.isOneOf("TRANSFER","MORTGAGE","RELEASE")&&applyNameMatch)
					||(doc.isOneOf("TRANSFER","MORTGAGE")&&!applyNameMatch)){
				for(PropertyI prop:doc.getProperties()){
					if(prop.hasTownshipLegal()){

						TownShipI townShipI = prop.getLegal().getTownShip();
						UnplattedSKLDIteratorEntry entry = new UnplattedSKLDIteratorEntry();
						entry.setSubdivision(townShipI);
						boolean nameMatched = false;
						if(applyNameMatch){
							if(GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)||
									GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) ||
									GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
									nameMatched = true;
							}
						}
						if( (nameMatched||!applyNameMatch) && entry.isEntryValid()){
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
			UnplattedSKLDIteratorEntry state) {
		if(state.isEntryValid()) {
			for (Object functionObject : module.getFunctionList()) {
				if (functionObject instanceof TSServerInfoFunction) {
					TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
					if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_ARB 
							&& StringUtils.isNotEmpty(state.getArb())) {
						function.setParamValue(state.getArb());
					} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_TOWNSHIP 
							&& StringUtils.isNotEmpty(state.getTownship())) {
						function.setParamValue(state.getTownship());
					} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_RANGE 
							&& StringUtils.isNotEmpty(state.getRange())) {
						function.setParamValue(state.getRange());
					} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_SECTION 
							&& StringUtils.isNotEmpty(state.getSection())) {
						function.setParamValue(state.getSection());
					}
				}
			}
		}
		
	}

	public void setPlattedIterator(LegalSKLDIterator legalSKLDIterator) {
		this.legalSKLDIterator = legalSKLDIterator;
		
	}

}

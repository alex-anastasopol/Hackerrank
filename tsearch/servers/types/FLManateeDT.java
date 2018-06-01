package ro.cst.tsearch.servers.types;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author Cristian Stochina
 */
public class FLManateeDT extends FLSubdividedBasedDASLDT{

	private static final long serialVersionUID = 3477834L;

	public FLManateeDT(long searchId) {
		super(searchId);
	}
	
	public FLManateeDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
		if ( inst.hasInstrNo() ){
			String instr = inst.getInstno().replaceFirst("^0+", "");
//			String year = String.valueOf(inst.getYear());
			if(!searched.contains(instr)){
				searched.add(instr);
			}else{
				return false;
			}
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.setData(0, cleanInstrNo(inst, false));
//			module.setData(2, year);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
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
//		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
		module.addIterator(instrumentRelatedNoIterator);
		if (isUpdate) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}
		module.addFilter(getLegalOrDocTypeFilter(searchId , true));
		module.addFilter(new PropertyTypeFilter(searchId));
		modules.add(module);
	}
	
	@Override
	public void addAssesorMapSearch(TSServerInfoModule module, PersonalDataStruct str) {
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
	
		String tw  = str.getTownship();
		String rg  = str.getRange();
		String sec = str.getSection();
		
		DocumentsManagerI m = search.getDocManager();
		
		uncheckPlats(m);
		
		if(StringUtils.isEmpty(tw)||StringUtils.isEmpty(rg)||StringUtils.isEmpty(sec)){
			
			try{
				m.getAccess();
				String[] subdivVector = getSubdivisionVector(m);
				if(StringUtils.isEmpty(tw)){
					tw = subdivVector[2];
				}
				if(StringUtils.isEmpty(rg)){
					rg = subdivVector[3];
				}
				if(StringUtils.isEmpty(sec)){
					sec = subdivVector[3];
				}
			}finally{
				m.releaseAccess();
			}
		}
		if(StringUtils.isEmpty(tw)){
			tw = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_TWN);
		}
		if(StringUtils.isEmpty(rg)){
			rg = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_RNG);
		}
		if(StringUtils.isEmpty(sec)){
			sec = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_SEC);
		}
		
		if(!StringUtils.isEmpty(tw)){
			tw = tw.replaceAll("[a-zA-Z]", "");
		}
		
		if(!StringUtils.isEmpty(rg) && rg.length()>2){
			rg = rg.replaceAll("[a-zA-Z]", "");
		}
		
		if(!StringUtils.isEmpty(sec) && sec.length()>2){
			sec = sec.replaceAll("[a-zA-Z]", "");
		}
		
		
		deletePlat(m, tw, rg);
		
		module.forceValue(0,tw);
		module.forceValue(1,rg);
		module.forceValue(2,sec);
		module.forceValue(4,"PLAT");
		module.forceValue(5,"ASSESSOR_MAP");
		
		return;
	}

	@Override
	public void addPlatMapSearch(TSServerInfoModule module,PersonalDataStruct str) {}
	
	@Override
	public void addParcelMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addSubdivisionMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addCondoMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
}

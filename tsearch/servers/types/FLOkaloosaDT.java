package ro.cst.tsearch.servers.types;

import java.util.List;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import com.stewart.ats.base.search.DocumentsManagerI;


public class FLOkaloosaDT extends FLSubdividedBasedDASLDT{

	private static final long serialVersionUID = 3477834L;

	public FLOkaloosaDT(long searchId) {
		super(searchId);
	}
	
	public FLOkaloosaDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public void addAssesorMapSearch(TSServerInfoModule module, PersonalDataStruct str, String additionalForSection) {
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
		
		String apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNO ).replaceAll("[.-]", "");
		
		if(!StringUtils.isEmpty(apn) && StringUtils.isEmpty(tw)){
			apn = apn.replaceAll("[a-zA-Z]", "");
			if(apn.length()>=6){
				sec = apn.substring(0,2);
				tw = apn.substring(2,4);
				rg = apn.substring(4,6);
			}
		}
		
		if(!StringUtils.isEmpty(tw)){
			tw = tw.replaceAll("[a-zA-Z]", "");
			tw = tw.replaceFirst("^0+", "");
		}
		
		if(!StringUtils.isEmpty(rg) && rg.length()>2){
			rg = rg.replaceAll("[a-zA-Z]", "");
			rg = rg.replaceFirst("^0+", "");
		}
		
		if(!StringUtils.isEmpty(sec) && sec.length()>2){
			sec = sec.replaceAll("[a-zA-Z]", "");
			sec = sec.replaceFirst("^0+", "");
		}
		deletePlat(m, sec+additionalForSection, tw);
		module.forceValue(0,sec+additionalForSection);
		module.forceValue(1,tw);
		module.forceValue(2,rg);
		module.forceValue(4,"PLAT");
		module.forceValue(5,"ASSESSOR_MAP");
		
		return;
	}
	
	@Override
	public void addAssesorMapSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules, boolean isUpdate) {
		
		String []directions ={"SE", "SW", "NW", "NE"};
		
		for(String str:directions){
			//assesor map  fake search
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.LD_BOOKNO_1);
			module.clearSaKeys();
			PlatIterator it = new PlatIterator(searchId, this);
			it.setExtraStringForSection(str);
			module.addIterator(it);
			if (!isUpdate) {
				modules.add(module);
			}
		}
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

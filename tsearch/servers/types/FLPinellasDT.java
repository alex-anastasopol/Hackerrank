package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import com.stewart.ats.base.search.DocumentsManagerI;


public class FLPinellasDT extends FLSubdividedBasedDASLDT{

	private static final long serialVersionUID = 3477834L;

	public FLPinellasDT(long searchId) {
		super(searchId);
	}
	
	public FLPinellasDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
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
			m = search.getDocManager();
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
		
		String apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNONDB ).replaceAll("[.-]", "");
		
		if(!StringUtils.isEmpty(apn) && StringUtils.isEmpty(tw)){
			apn = apn.replaceAll("[a-zA-Z]", "");
			if(apn.length()>=6){
				sec = apn.substring(0,2);
				tw = apn.substring(2,4);
				rg = apn.substring(4,6);
			}
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

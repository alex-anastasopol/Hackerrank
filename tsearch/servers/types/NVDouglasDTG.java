package ro.cst.tsearch.servers.types;

import java.io.FileNotFoundException;
import java.io.IOException;


import java.util.List;
import java.util.Set;

import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

import com.stewart.ats.base.document.InstrumentI;

/**
 * @author cristian stochina
 */
public class NVDouglasDTG extends NVGenericDTG {

	private static final long serialVersionUID = 3024488184071095461L;

	public NVDouglasDTG(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public NVDouglasDTG(long searchId) {
		super(searchId);
	}


	@Override
	protected String prepareInstrumentYearForReferenceSearch(InstrumentI inst) {
		return "";
	}
	
	@Override
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
		if (inst.hasInstrNo()){
			String instr = inst.getInstno().replaceFirst("^0+", "");
			
			String year1 = String.valueOf(inst.getYear());
			if (!searched.contains(instr + year1)){
				searched.add(instr + year1);
			} else{
				return false;
			}
			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.setData(0, instr);
			if (isUpdate){
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			module.setData(1, year1);
			modules.add(module);
			return true;
		}
		return false;
	}
}

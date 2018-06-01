package ro.cst.tsearch.servers.types;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

import com.stewart.ats.base.document.InstrumentI;

/**
 * @author cristian stochina
 */
public class OHLorainDTG extends OHGenericDTG {

	private static final long serialVersionUID = 3024488184071095461L;

	public OHLorainDTG(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	public OHLorainDTG(long searchId) {
		super(searchId);
	}

	@Override
	public InstrumentGenericIterator getInstrumentOrBookTypeIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, dataSite) {

			private static final long serialVersionUID = 5399351945130601258L;
			@Override
			protected String cleanInstrumentNo(String instr, int year) {
				if (instr.length() >= 10){
					instr = instr.substring(4).replaceAll("^0+", "");
				}
				
				//20060139851
				return instr;
			}
			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				return state.getInstno().trim();
			}
			
		};
		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
			instrumentGenericIterator.setRemoveLeadingZerosBP(true);
		}

		return instrumentGenericIterator;
	}
	
	@Override
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
		if ( inst.hasInstrNo() ){   
			String instr = StringUtils.defaultString(inst.getInstno());
			String year1 = String.valueOf(inst.getYear());
			
			if(instr.length()>=10){
				year1 = instr.substring(0,4);
				instr = instr.substring(4).replaceAll("^0+", "");
			}
			
			//20060139851
			 
			if(!searched.contains(instr+year1)){
				searched.add(instr+year1);
			}else{
				return false;
			}
			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.setData(0, instr);
			module.setData(1, year1);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	@Override
	public void processOcredInstrument(InstrumentI in, boolean ocrReportedBookPage) {
		if(!ocrReportedBookPage){
		
			String inst = StringUtils.defaultString(in.getInstno()).replaceAll("^0+", "");
			if(inst.length()>=10){				
				String yearStr = inst.substring(0,4);
				inst = inst.substring(4);
				
				if(super.validYear(yearStr)){
					in.setBook("");
					in.setPage("");
					in.setInstno(inst.replaceAll("^0+", ""));
					in.setYear(Integer.parseInt(yearStr));
				}
			}
		}
		
	}
	

	
}

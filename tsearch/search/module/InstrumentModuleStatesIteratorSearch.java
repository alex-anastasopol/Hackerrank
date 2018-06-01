package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * InstrumentModuleStatesIteratorSearch
 *
 */
public class InstrumentModuleStatesIteratorSearch extends ModuleStatesIterator {
	private static final Category logger = Category.getInstance(InstrumentModuleStatesIteratorSearch.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + InstrumentModuleStatesIteratorSearch.class.getName());
	
	/**
	 * Instrument number list.
	 */	
	private List instrList = new ArrayList();
	
	/**
	 * Default constructor.
	 */
	public InstrumentModuleStatesIteratorSearch(long searchId)
	{
		super(searchId);
	}

	
	protected void initInitialState(TSServerInfoModule initial){
		super.initInitialState(initial);
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		instrList = extractInstrumentNoList(sa.getAtribute(initial.getSaObjKey()));
		if(!searchIfPresent){
			instrList = ModuleStatesIterator.removeAlreadyRetrieved(instrList, searchId);
		}		
	}

	protected void setupStrategy() {
		StatesIterator si ;
		si = new DefaultStatesIterator(instrList);
		setStrategy(si);
	}
	
	public Object current(){
		Instrument instr = ((Instrument) getStrategy().current());
		TSServerInfoModule crtState = new TSServerInfoModule(initialState);

		for (int i =0; i< crtState.getFunctionCount(); i++){
			TSServerInfoFunction fct = crtState.getFunction(i);
			if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH) {
				String instrNo = instr.getInstrumentNo();  
				fct.setParamValue(instrNo);
			}
		}
		return  crtState ;
	}
	
	private List extractInstrumentNoList(String originalInstrNo)
	{
		List instrList = new ArrayList();

		if(StringUtils.isStringBlank(originalInstrNo)) {
			return instrList;
		}

		String [] instrs = originalInstrNo.split(",");

		if(instrs == null) {
			return instrList;
		}

		for (int i = 0; i < instrs.length; i++) {
			if(!StringUtils.isStringBlank(instrs[i])) {
				Instrument crtInst = new Instrument();
				crtInst.setInstrumentNo(instrs[i].trim());
				instrList.add(crtInst);
			}
		}		
		
		return instrList;
	}
}

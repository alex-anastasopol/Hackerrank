/*
 * Created on Apr 1, 2004
 *
 */
package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.propertyInformation.InstrumentConstants;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * Instrument number iterator.
 * 
 * @author catalinc
 */
public class InstrumentNoModuleStatesIterator extends ModuleStatesIterator
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Category logger = Logger.getLogger(InstrumentNoModuleStatesIterator.class.getName());
	private static final Category loggerDetails = Logger.getLogger(Log.DETAILS_PREFIX + InstrumentNoModuleStatesIterator.class.getName());
	
	/**
	 * Instrument number list.
	 */	
	protected List instrList = new ArrayList();
	
	/**
	 * Default constructor.
	 */
	public InstrumentNoModuleStatesIterator(long searchId)
	{
		super(searchId);
	}

	
	protected void initInitialState(TSServerInfoModule initial){
		super.initInitialState(initial);
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		instrList = extractInstrumentNoList((List) sa.getObjectAtribute(initial.getSaObjKey()));
		try {
	 	   	for (Object instrument : instrList) {
				System.err.println("no:" + ((Instrument)instrument).getInstrumentNo());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(!searchIfPresent){
			instrList = ModuleStatesIterator.removeAlreadyRetrieved(instrList, searchId);
		}
	}

	protected void setupStrategy() {
		StatesIterator si ;
		
		ListIterator iterator = instrList.listIterator();
		while (iterator.hasNext()) {
			Instrument instr = ((Instrument) iterator.next());
			if ("".equals(instr.getInstrumentNo()))
				iterator.remove();
		}
		
		si = new DefaultStatesIterator(instrList);
		setStrategy(si);
	}
	
	public Object current(){
		Instrument instr = ((Instrument) getStrategy().current());
		TSServerInfoModule crtState = new TSServerInfoModule(initialState);

		List<FilterResponse> allFilters = crtState.getFilterList();
		GenericInstrumentFilter gif = null;
		HashMap<String, String> filterCriteria = null;
		if(allFilters != null) {
			for (FilterResponse filterResponse : allFilters) {
				if (filterResponse instanceof GenericInstrumentFilter) {
					gif = (GenericInstrumentFilter) filterResponse;
					filterCriteria = new HashMap<String, String>();
					gif.clearFilters();
				}
			}
		}
		
		for (int i =0; i< crtState.getFunctionCount(); i++){
			TSServerInfoFunction fct = crtState.getFunction(i);
			if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE){
				String instrNo = instr.getInstrumentNo();  
				fct.setParamValue(instrNo);
				if(filterCriteria != null) {
					filterCriteria.put("InstrumentNumber", instrNo);
				}
			}
		}
		Boolean simulateCrossRef = (Boolean)crtState.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF);
        if(Boolean.TRUE.equals(simulateCrossRef)){
        	String crossRefSource = (String)instr.getExtraInfo(InstrumentConstants.CROSS_REF_SOURCE_TYPE);
        	if(!StringUtils.isEmpty(crossRefSource)) {
        		crtState.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CROSSREF_DOC_SOURCE, crossRefSource);
        		initialState.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_CROSSREF_DOC_SOURCE, crossRefSource);
        	}
        }
        
        if(gif != null) {
			gif.addDocumentCriteria(filterCriteria);
		}
		//logger.debug(" crtState for " + initialState + " =  " + crtState);
		return  crtState ;
	}
	
	protected List extractInstrumentNoList(List original)
	{
		List instr = new ArrayList();

		for (int i = 0; i < original.size(); i++)
		{
			Instrument instrCrt = (Instrument)original.get(i);
			if(!StringUtils.isStringBlank(instrCrt.getInstrumentNo()))
			{
				instr.add(instrCrt);
			}
		}		
		
		return instr;
	}
}

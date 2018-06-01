/*
 * Created on Jun 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Category;

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
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class BookPageListModuleStatesIterator extends ModuleStatesIterator {
	
	private static final Category logger = Category.getInstance(BookPageListModuleStatesIterator.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + BookPageListModuleStatesIterator.class.getName());
	
	private List instrList = new ArrayList();
	
	public BookPageListModuleStatesIterator (long searchId){
		super(searchId);
	}

	protected void initInitialState(TSServerInfoModule initial){
		super.initInitialState(initial);
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		instrList = (List) sa.getObjectAtribute(initial.getSaObjKey()); 
		if(!searchIfPresent){
			instrList = ModuleStatesIterator.removeAlreadyRetrieved(instrList, searchId);
		}
		if (logger.isDebugEnabled())
			logger.debug("lista instrumenete = " + instrList);
	}


	protected void setupStrategy() {
		StatesIterator si ;
		
		ListIterator iterator = instrList.listIterator();
		while (iterator.hasNext()) {
			Instrument instr = ((Instrument) iterator.next());
			if ("".equals(instr.getBookNo()) && "".equals(instr.getPageNo()))
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
			if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE){
				String bookNo = instr.getBookNo().matches("0+$") ? "" : instr.getBookNo();
				fct.setParamValue(bookNo);
				if(filterCriteria != null) {
					filterCriteria.put("Book", bookNo);
				}
			}
			if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE){
				String pageNo = instr.getPageNo().matches("0+$") ? "" : instr.getPageNo();  				
				fct.setParamValue(pageNo);
				if(filterCriteria != null) {
					filterCriteria.put("Page", pageNo);
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
}

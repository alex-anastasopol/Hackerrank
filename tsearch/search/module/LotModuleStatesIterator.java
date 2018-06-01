package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;

public class LotModuleStatesIterator extends ModuleStatesIterator {

	private static final long serialVersionUID = 100000000L;
	
	private static final Category logger = Category.getInstance(BookPageListModuleStatesIterator.class.getName());
	
	private List lotList = new ArrayList();
	
	public LotModuleStatesIterator (long searchId){
		super(searchId);
	}

	protected void initInitialState(TSServerInfoModule initial){
		
		super.initInitialState(initial);
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		String lot = sa.getAtribute(SearchAttributes.LD_LOTNO);
		
		Vector lots = LotMatchAlgorithm.prepareLotInterval(lot);
		
		for (int i = 0; i < lots.size(); i++) {
			
			LotInterval lotInterval = (LotInterval) lots.elementAt(i);
			
			/*for (int j = lotInterval.getLow(); j <= lotInterval.getHigh(); j++) {
				lotList.add(String.valueOf(j));
			}*/
			
			lotList.addAll(lotInterval.getLotList());
		}
		
		if (logger.isDebugEnabled())
			logger.debug("lista loturi = " + lotList);
	}
	
	protected void setupStrategy() {
		StatesIterator si ;
		si = new DefaultStatesIterator(lotList);
		setStrategy(si);
	}
	
	public Object current(){
		
		String lot = (String) getStrategy().current();
		
		TSServerInfoModule crtState = new TSServerInfoModule(initialState);

		for (int i =0; i< crtState.getFunctionCount(); i++){
			
			TSServerInfoFunction fct = crtState.getFunction(i);
			
			if (SearchAttributes.LD_LOTNO.equals(fct.getSaKey())){
				fct.setParamValue(lot);
				break;
			}
		}

		return  crtState ;
	}	
}
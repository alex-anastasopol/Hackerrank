package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringEquivalents;

public class SubdivisionIterator extends ModuleStatesIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<String> subdivisions = new ArrayList<String>();

	public SubdivisionIterator(long searchId) {
		super(searchId);
	}
	
	protected void initInitialState(TSServerInfoModule initial){
		
		super.initInitialState(initial);
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		String subdivision = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		
		subdivisions.addAll(StringEquivalents.getInstance().getEquivalents(subdivision));
		
	}

	protected void setupStrategy() {
		StatesIterator si ;
		si = new DefaultStatesIterator(subdivisions);
		setStrategy(si);
	}
	
	public Object current(){
		
		String subdivision = (String) getStrategy().current();
		
		TSServerInfoModule crtState = new TSServerInfoModule(initialState);

		for (int i =0; i< crtState.getFunctionCount(); i++){
			
			TSServerInfoFunction fct = crtState.getFunction(i);
			
			if (SearchAttributes.LD_SUBDIV_NAME.equals(fct.getSaKey())){
				fct.setSaKey("");
				fct.setParamValue(subdivision);
				break;
			}
		}

		return  crtState ;
	}	
	

}

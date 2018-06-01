package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;

/**
 * @author Cristian Stochina
 */
public class FLoridaDtIterator extends ModuleStatesIterator {

	private static final long serialVersionUID = 1L;

	public FLoridaDtIterator(long searchId) {
		super(searchId);
	}

	private List lotList = new ArrayList();

	protected void initInitialState(TSServerInfoModule initial) {

		super.initInitialState(initial);

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		String lot = sa.getAtribute(SearchAttributes.LD_LOTNO);

		Vector<LotInterval> lots = LotMatchAlgorithm.prepareLotInterval(lot);

		for (int i = 0; i < lots.size(); i++) {
			LotInterval lotInterval = (LotInterval) lots.elementAt(i);
			lotList.addAll(lotInterval.getLotList());
		}

	}

	protected void setupStrategy() {
		StatesIterator si = new DefaultStatesIterator(lotList);
		setStrategy(si);
	}

	public Object current() {

		String lot = (String) getStrategy().current();
		TSServerInfoModule crtState = new TSServerInfoModule(initialState);

		for (int i = 0; i < crtState.getFunctionCount(); i++) {

			TSServerInfoFunction fct = crtState.getFunction(i);

			if (SearchAttributes.LD_LOTNO.equals(fct.getSaKey())) {
				fct.setParamValue(lot);
				break;
			}
		}

		return crtState;
	}

}

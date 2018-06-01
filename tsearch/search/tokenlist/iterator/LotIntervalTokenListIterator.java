/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.tokenlist.iterator;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.strategy.StrategyBasedIterator;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class LotIntervalTokenListIterator extends StrategyBasedIterator implements StatesIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private String initial;
	

	public LotIntervalTokenListIterator(String s){
		this.initial = s;
		String[] arrays = s.split("[ ]+");
		List<String> l = new ArrayList<String>();
		for (int i = 0; i < arrays.length; i++) {
			l.addAll(StringUtils.parseLotInterval(arrays[i], new String[]{"-"}));
		}
		setStrategy( new DefaultStatesIterator(l));
	}
	
}

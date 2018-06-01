/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.tokenlist.iterator;

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
public class LotTokenListIterator extends StrategyBasedIterator implements StatesIterator {

	private String initial;
	

	public LotTokenListIterator(String s){
		this.initial = s;
		List l = StringUtils.parseIntegers(s, new String[]{" ", "-"});
		setStrategy( new DefaultStatesIterator(l));
	}
	
}

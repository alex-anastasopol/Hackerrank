/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.tokenlist.iterator;

import java.util.ArrayList;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.strategy.OneStateIterator;
import ro.cst.tsearch.search.strategy.StrategyBasedIterator;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class LastWordInitialTokenListIterator extends StrategyBasedIterator implements StatesIterator {

	private static final Category logger = Category.getInstance(LastWordInitialTokenListIterator.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + LastWordInitialTokenListIterator.class.getName());


	public LastWordInitialTokenListIterator(String s){
		
		ArrayList l = new ArrayList(StringUtils.splitString(s));
		if (l.size()>1){
			String last = (String) l.remove(l.size()-1);
			last = last.substring(0,1);
			l.add(last);		
		}
		
		s = StringUtils.join(l, " ");
		if (!StringUtils.isStringBlank(s)){
			setStrategy(new OneStateIterator(s));
		}else{
			setStrategy( new DefaultStatesIterator());
		}
	}
}

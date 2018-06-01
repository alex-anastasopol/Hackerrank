/*
 * Created on Jun 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TwoStatesIterator extends StrategyBasedIterator {

	public TwoStatesIterator (Object element){
		this(new Object(), element, true);
	}
	public TwoStatesIterator (Object element, boolean asc){
		this(new Object(), element, asc);
	}
	
	public TwoStatesIterator (Object first, Object second){
		this(first, second, true);
	}
	
	public TwoStatesIterator (Object first, Object second, boolean asc){
	
		List states = new ArrayList();
		if (asc){
			states.add(first);
			states.add(second);
		}else{
			states.add(second);
			states.add(first);
		}

		setStrategy(new DefaultStatesIterator(states));
	}
}

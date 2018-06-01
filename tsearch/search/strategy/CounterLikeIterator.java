/*
 * Created on Jun 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.strategy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.StatesIterator;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CounterLikeIterator implements StatesIterator {
	private List list;
	private int crtIdx;

	long searchId=-1;
	
	protected static final Category logger = Category.getInstance(CounterLikeIterator.class.getName());

	private boolean reset = false;
	private boolean hasNoStates = false;

	public CounterLikeIterator(List list,long saerchId) {
		this.searchId = saerchId;
		if ((list == null) || (list.size() == 0)) {
			throw new IllegalArgumentException(" List must have at least one item ");
		}
		this.list = list;
		//logger.debug(logger.getName());
		if (logger.isDebugEnabled())
			logger.debug("new " + this);
		reset(searchId);
	}

	public void reset(long searchId) {
		if (reset) {
			return;
		}
		hasNoStates = false;
		if (logger.isDebugEnabled())
			logger.debug(" Reset " + this);
		StatesIterator si1 = (StatesIterator) list.get(0);
		si1.reset(searchId);
		if (!si1.hasNext(searchId)) {
			hasNoStates = true;
			return;
		}
		for (int i = 1; i < list.size(); i++) {
			StatesIterator si = (StatesIterator) list.get(i);
			si.reset(searchId);
			if (si.hasNext(searchId)) {
				si.goToNext();
			} else {
				hasNoStates = true;
				return;
			}
		}
		crtIdx = 0;
		reset = true;
	}

	public boolean hasNext(long searchId) {
		if (hasNoStates) {
			return false;
		}
		if (logger.isDebugEnabled())
			logger.debug(" begin HasNext( ) for " + this);
		//logger.debug( "has Next for counterlike " );
		while (crtIdx < list.size() - 1) {
			//resetarea contorului pina la primul hasNext = true sau pina la ultimul element din lista
			StatesIterator crtItem = (StatesIterator) list.get(crtIdx);
			if (crtItem.hasNext(searchId)) {
				return true;
			} else {
				crtItem.reset(searchId);
				crtItem.goToNext();
				crtIdx++;
			}
		}
		//logger.debug(" verific  HasNext pt " + list.get(crtIdx));
		boolean rez = ((StatesIterator) list.get(crtIdx)).hasNext(searchId);
		if (logger.isDebugEnabled())
			logger.debug(" intorc HasNext=" + rez + " for " + this);
		return rez; //has next pt ultimul element din lista 
	}

	public void goToNext() {
		if (logger.isDebugEnabled())
			logger.debug(" Go to next for " + this);
		reset = false;
		StatesIterator crtItem = (StatesIterator) list.get(crtIdx);
		crtItem.goToNext();
		crtIdx = 0;
	}
	
	@Override
	public Object peekAtNext() {
		return null;
	}

	public Object current() {
		//logger.debug( "current for counterlike " + list);
		return list;
	}

	public String toString() {
		return " CounterLikeIterator (crtIdx=" + crtIdx + ")for " + list;
	}

	public static void main(String[] args) {
		/*List states = new ArrayList();
		states.add("0");
		states.add("1");
		states.add("2");
		states.add("3");
		states.add("4");

		DefaultStatesIterator df1 = new DefaultStatesIterator(states);
		DefaultStatesIterator df2 = new DefaultStatesIterator(states);
		DefaultStatesIterator df3 = new DefaultStatesIterator(states);
		DefaultStatesIterator df4 = new DefaultStatesIterator(states);

		List counter = new ArrayList();
		counter.add(df1);
		counter.add(df2);
		counter.add(df3);
		counter.add(df4);

		CounterLikeIterator ci = new CounterLikeIterator(counter);
		ci.reset(searchId);
		while (ci.hasNext(searchId)) {
			ci.goToNext();
			List l = (List) ci.current();
			for (Iterator iter = l.iterator(); iter.hasNext();) {
				DefaultStatesIterator dfi = (DefaultStatesIterator) iter.next();
				System.err.println(dfi.current() + " ");
			}
			System.err.println(" ");
		}*/

	}

}

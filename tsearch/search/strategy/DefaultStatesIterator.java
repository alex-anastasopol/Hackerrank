/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.StatesIterator;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DefaultStatesIterator implements StatesIterator{

	protected static final Category logger = Category.getInstance(DefaultStatesIterator.class.getName());
	
	
	List states;
	int crtIdx;

	public DefaultStatesIterator() {
		this(new ArrayList());
	}

	public DefaultStatesIterator(Collection l) {
		states = new ArrayList();
		states.addAll( l);
		crtIdx = -1 ;
	}
	public void reset(long searchId){
		//logger.debug("Reset " + this);
		crtIdx = -1 ;
	}

	public boolean hasNext(long searchId) {
		boolean rez = (crtIdx + 1 < states.size()); 
		//logger.debug( "Has Next =" + rez + " for " + this );
		return rez ;
	}


	public void goToNext(){
		//logger.debug( " Go to Next  for " + this );
		crtIdx++;
	}
	
	@Override
	public Object peekAtNext() {
		if(crtIdx + 1 < states.size()) {
			return states.get(crtIdx + 1);
		}
		return null;
	}

	public Object current() {
		return states.get(crtIdx);
	}
	
	public String toString (){
		return " DefaultStatesIterator (crtIdx =  " + crtIdx + ") for " + states ;
	}
	
	public int getCurrentIndex() {
		return crtIdx;
	}
	
	public List getList() {
		return Collections.unmodifiableList(states);
	}
	
	/**
	 * This will change the list and the position
	 * @param newList
	 * @param newPosition
	 */
	public void replaceData(List newList, int newPosition) {
		states.clear();
		states.addAll(newList);
		
		crtIdx = newPosition;
		
		if(crtIdx < 0) {
			crtIdx = -1;
		}
		
	}

}

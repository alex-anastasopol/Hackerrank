/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.strategy;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.StatesIterator;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class OneStateIterator implements StatesIterator{

	protected static final Category logger = Category.getInstance(OneStateIterator.class.getName());
	
	protected Object initialState;
	protected Object crtState = null;
	
	public OneStateIterator  (Object initial) {
		this.initialState = initial; 
	}

	public void reset(long searchId){
		//logger.debug( " Reset " + this );
		crtState = null;
	}

	public boolean hasNext(long searchId) {
		boolean rez = (crtState == null); 
		//logger.debug( "Has Next =" + rez + "  for " + this );
		return rez ;
	}
	
	@Override
	public Object peekAtNext() {
		return null;
	}


	public void goToNext(){
		//logger.debug( " Go to Next  for " + this );
		//logger.debug(" in goToNext for "+ initialState);
		crtState  = initialState;
	}

	public Object current() {
		//logger.debug(" crtState  for "+ initialState +  " =  " + crtState);
		return crtState;
	}
	
	public String toString (){
		return " OneStateIterator (crtState = " + crtState + ") for " + initialState ;
	}


}

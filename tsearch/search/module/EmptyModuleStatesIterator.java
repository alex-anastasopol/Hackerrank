/*
 * Created on Jun 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.module;

import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class EmptyModuleStatesIterator extends ModuleStatesIterator {
	

	public EmptyModuleStatesIterator (long searchId){
		super(searchId);
	}
	
	protected void setupStrategy(){
		setStrategy(new DefaultStatesIterator ());
	}
	


}

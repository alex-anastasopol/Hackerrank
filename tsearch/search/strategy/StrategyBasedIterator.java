/*
 * Created on Jun 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.strategy;


import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.search.StatesIterator;

/**
 * @author elmarie
 *
 */
public class StrategyBasedIterator implements StatesIterator{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final Category logger = Logger.getLogger(StrategyBasedIterator.class);

	protected StatesIterator strategy ;
	protected int beginIndex = this.getClass().getPackage().getName().length()+1;
	protected String className = this.getClass().getName().substring(beginIndex);
	
	protected boolean reset = false;
	
	public  StrategyBasedIterator(){
		setStrategy(new DefaultStatesIterator());
	}

	 public static int DERIV_DEFAULT = 0; // cele ca si pana acum
	 public static int DERIV_AO_TNREALESTATE = 1; // fi mi last_name
	
	// the de-serialization DOES USE this field
	protected int derivType  = DERIV_DEFAULT;
	
	/*
    public int getDerivType() {
        return derivType;
    }
 
    public void setDerivType(int derivType) {
        this.derivType = derivType;
    }
    */
	 
	protected void setStrategy(StatesIterator strategy){
		//logger.debug(logger.getName());;
		//logger.debug(className + " set strategy = " + strategy);
		this.strategy = strategy;
	}

	
	protected StatesIterator getStrategy(){
		return strategy;
	}
	
	public void reset(long searchId){
		//logger.debug(this + " reset ");
		if (isReset()){
			return;
		}
		strategy.reset(searchId);	
		setReset(true);
	}
	
	public boolean hasNext(long searchId) {
		//logger.debug(this + " hasNext( " + d + ")");
		return strategy.hasNext(searchId);
	}
	
	@Override
	public Object peekAtNext() {
		return null;
	}

	public void goToNext(){
		//logger.debug(this + " go to Next ");
		setReset(false);
		strategy.goToNext();
	}

	public Object current(){
		//logger.debug(this + " current () ");
		return  strategy.current() ;
	}
	
	public Object getCurrentStrategyItem(){
		return  strategy.current() ;
	}
	
	public String toString(){
		return strategy.toString();
	}
	/**
	 * @return
	 */
	protected boolean isReset() {
		return reset;
	}

	/**
	 * @param b
	 */
	protected void setReset(boolean b) {
		reset = b;
	}


}

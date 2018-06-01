/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.strategy;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.search.StatesIterator;

import org.apache.log4j.Category;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class  AddFromListIterator extends StrategyBasedIterator  {

	protected static final Category logger= Category.getInstance(AddFromListIterator.class.getName());
	
	private Object crt = null;
	private Object lastCriteria = null;
	
	private Object initialObj ;	
	private List tokensList ;	
	private List initialList ;
	private List usedTokens = new ArrayList();	

	private boolean reset = false;

	public AddFromListIterator(){
	}
	
	public AddFromListIterator(Object initialObj,List initialList1,long searchId){ 
		if (logger.isDebugEnabled())
			logger.debug(" new  AddFromListIterator(" + initialObj + ", initialList = " + initialList1 +")");
		init(initialObj, initialList1,searchId);
	}
	
	protected void init(Object initialObj,List initialList1,long searchId){

		this.initialObj = initialObj;
		this.initialList = new ArrayList(initialList1);

		/*if ((initialList == null )||(initialList.size() == 0)){
			throw new IllegalArgumentException(" Tokens list must have at least one item ");
		}*/
		setupStrategy(searchId);
	}

	private void setupStrategy(long searchId){
		reset(searchId);
	}
	
	public void reset(long searchId){
		//logger.debug("reset");
		if (isReset()){
			return;
		}
		lastCriteria = initialObj;
		crt = null;
		tokensList = new ArrayList(initialList);

		changeStrategy(searchId);

		setReset(true);
	}
	
	public boolean hasNext(long searchId){
		if (!super.hasNext(searchId)){
			changeStrategy(searchId);
		}
		return super.hasNext(searchId);
	}



	public void retainTheCurrentStateAndChangeStrategy(long searchId) {
		lastCriteria = current();
		usedTokens.add(crt);
		changeStrategy(searchId);
	}

	
	public Object current(){
		return getCurrentFromStrategy();
	}	


	private void changeStrategy(long searchId){
		if (!tokensList.isEmpty()){
			crt =  tokensList.get(0);
			tokensList.remove(crt);
			setStrategy (getNewStrategy(lastCriteria, crt,searchId));
		}else{
			setStrategy (new DefaultStatesIterator());
		}
	}

	protected abstract StatesIterator getNewStrategy(Object lastCriteria1, Object crtToken,long searchId);
		//return new CounterLikeIterator(Arrays.asList(new StatesIterator[]{ new OneStateIterator (lastCriteria1), crtToken}));
	

	protected abstract Object getCurrentFromStrategy();
		/*List l = (List) getStrategy().current();
		List ls = new ArrayList();
		Iterator iter = l.iterator();
		while (iter.hasNext()) {
			Token token = (Token) (((StatesIterator) iter.next()).current());
			ls.add(token.getString());
		}
		return (new TokenList(ls)).getString();*/

	public Object getLastCriteria(){
		return lastCriteria;
	}

	public List getUsedTokens(){
		return usedTokens;
	}

}

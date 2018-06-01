/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.tokenlist.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.AddFromListIterator;
import ro.cst.tsearch.search.strategy.CounterLikeIterator;
import ro.cst.tsearch.search.strategy.OneStateIterator;
import ro.cst.tsearch.search.token.Token;
import ro.cst.tsearch.search.token.iterator.TokenIterator;
import ro.cst.tsearch.search.tokenlist.TokenList;

import org.apache.log4j.Category;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class StreetSPTokenListIterator extends AddFromListIterator  {
	
	protected static final Category logger= Category.getInstance(StreetSPTokenListIterator.class.getName());
	private boolean pre;
	private int tokenIteratorType;

	public StreetSPTokenListIterator(Token streetName,List tokens, boolean pre, int tokenIteratorType,long searchId){
		this.pre = pre;
		this.tokenIteratorType = tokenIteratorType;
		init(streetName, tokens,searchId);
		//logger.debug("pre = " + pre);
	}


	protected StatesIterator getNewStrategy(Object lastCriteria1, Object crtToken,long searchId){
		StatesIterator tokenIter = TokenIterator.getInstance (tokenIteratorType, (Token) crtToken);
		StatesIterator streetIter = new OneStateIterator (lastCriteria1);
		StatesIterator[] si; 
		//logger.debug("pre = " + pre);
		if (pre){
			si = new StatesIterator[]{tokenIter, streetIter};
		}else{
			si = new StatesIterator[]{streetIter, tokenIter};
		}
		return new CounterLikeIterator(Arrays.asList(si),searchId);
	}
	

	protected Object getCurrentFromStrategy(){
		List l = (List) getStrategy().current();
		List ls = new ArrayList();
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			ls.add(((StatesIterator) iter.next()).current());
		}
		return  new Token(TokenList.getString(ls));
	}	
	
	public List getUsedDirections(){
		List l = new ArrayList();
		for (Iterator iter = getUsedTokens().iterator(); iter.hasNext();) {
			Token token = (Token) iter.next();
			if (token.getType() == Token.TYPE_STREET_DIRECTION){
				l.add(token);
			}
		}
		return l;
	}
}

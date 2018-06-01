/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.token.iterator;


import org.apache.log4j.Category;

import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.strategy.OneStateIterator;
import ro.cst.tsearch.search.strategy.StrategyBasedIterator;
import ro.cst.tsearch.search.strategy.TwoStatesIterator;
import ro.cst.tsearch.search.token.Token;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TokenIterator extends StrategyBasedIterator implements StatesIterator{

	public static final int ZERO_STATES = 0;
	public static final int ONE_STATE = 1;
	public static final int TWO_STATES = 2;
	public static final int ADDRESS_ABBREV = 3;
	public static final int ADDRESS_ABBREV_ALL = 4;
	
	protected static final Category logger = Category.getInstance(TokenIterator.class.getName());

	protected Token initialState;
	
	public TokenIterator(){
	}
	
	public TokenIterator  (Token initial) {
		this(initial, ONE_STATE);
	}

	public TokenIterator  (Token initial, int type) {
		this.initialState = new Token(initial);
		if (logger.isDebugEnabled())
			logger.debug("new " + className + " for "+ initialState);
		if (type == ZERO_STATES){
			setStrategy(new DefaultStatesIterator());
		}else if (type == TWO_STATES){
			Token second = new Token(initialState);
			second.setString("");
			setStrategy(new TwoStatesIterator(initialState, second, true));
		}else{
			setStrategy(new OneStateIterator(initialState)); 
		}
	}
	

	public Object current() {
		//logger.debug(" for initial="+this.initialState);
		//logger.debug("super.current = " + super.current());
		return new Token((Token) super.current());
	}
	
	public String toString (){
		return getStrategy().toString() ;
	}

	public static TokenIterator getInstance(int tokenIteratorType, Token token) {
		if (tokenIteratorType == ADDRESS_ABBREV){
			return new AddresAbrevTokenIterator(token);
		}else if (tokenIteratorType == ADDRESS_ABBREV_ALL){
			return new AddresAbrevTokenIterator(token, false);
		}else {
			return new TokenIterator(token, tokenIteratorType);
		}
	}


}

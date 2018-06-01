/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.token.iterator;

import java.util.Collections;
import java.util.List;

import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.token.AddressAbrev;
import ro.cst.tsearch.search.token.Token;

import org.apache.log4j.Category;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AddresAbrevTokenIterator extends TokenIterator implements StatesIterator {
	
	protected static final Category logger= Category.getInstance(AddresAbrevTokenIterator.class.getName());
	private Token initialState;
	
	public AddresAbrevTokenIterator(Token initial, boolean shortestOnly) {
		this.initialState = new Token(initial);
		List states = AddressAbrev.getAllAbbrevs(initialState.getString(), shortestOnly);
		Collections.sort(states, Collections.reverseOrder());
		if (logger.isDebugEnabled())
			logger.debug(" AddresAbrevTokenIterator for " +  initialState + " with " + states.size() + " states ");
		setStrategy(new DefaultStatesIterator(states)); 
	}

	public AddresAbrevTokenIterator(Token initial) {
		this(initial, true);
	}
	
	public AddresAbrevTokenIterator(String initial) {
		this (new Token(initial));
	}


	public Object current() {
		Token crtState = new Token(initialState); 
		crtState.setString((String) getStrategy().current());
		return crtState;
	}

	
}

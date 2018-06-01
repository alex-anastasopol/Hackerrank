package ro.cst.tsearch.search;

import java.io.Serializable;

/**
 * @author elmarie
 */
public interface StatesIterator extends Serializable {
    
    static final long serialVersionUID = 10000000;

	public void reset(long searchId);

	public boolean hasNext(long searchId);

	public void goToNext();

	public Object current();
	
	public Object peekAtNext();
	
	
}

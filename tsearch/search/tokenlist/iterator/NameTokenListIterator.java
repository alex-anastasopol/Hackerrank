/*
 * Created on Jun 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.tokenlist.iterator;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.strategy.StrategyBasedIterator;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.search.tokenlist.TokenList;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class NameTokenListIterator extends StrategyBasedIterator {

	private static final Category logger = Category.getInstance(NameTokenListIterator.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + NameTokenListIterator.class.getName());

	protected NameTokenList spouse;
	protected NameTokenList owner;

	protected boolean firstNameNotEmpty = false;
	protected boolean lastNameNotEmpty	= true;
	
	private boolean addMCNDefault = false;

	public NameTokenListIterator(NameTokenList owner, NameTokenList spouse) {

		this.owner = owner;
		this.spouse = spouse;

		if (logger.isDebugEnabled())
			logger.debug(
			" new  NameTokenListIterator( owner = "
				+ owner
				+ ", spouse ="
				+ spouse
				+ ")");
	}

	public void init() {
	    /* setupStrategy1() and setupStrategy() were identical
	    if (derivType == DERIV_AO_TNREALESTATE)
	        setupStrategy1(); 
	    else
	        setupStrategy();
	    */
		setupStrategy();
	}

	protected void setupStrategy() {

		List lastName = owner.getLastName();
		List firstName = owner.getFirstName();
		List middleName = owner.getMiddleName();
		List fm = new ArrayList(firstName);
		fm.addAll(middleName);

		List mf = new ArrayList(middleName);
		mf.addAll(firstName);

		List middleInitial = NameTokenList.getInitials(middleName);
		List firstInitial = NameTokenList.getInitials(firstName);

		List spouseFirstName = spouse.getFirstName();
		List spouseMiddleName = spouse.getMiddleName();
		List spouseLastName = spouse.getLastName();
		List sfm = new ArrayList(spouseFirstName);
		sfm.addAll(spouseMiddleName);

		List l = new ArrayList();

		boolean emptyMiddle =
			(StringUtils.isStringBlank(TokenList.getString(middleName)));
		boolean emptySpouseMiddle =
			(StringUtils.isStringBlank(TokenList.getString(spouseMiddleName)));

		if (!emptyMiddle) {
			put(l, fm, lastName);
			put(l, mf, lastName);
		}
		put(l, firstName, lastName);
		//put(l, middleName, lastName, type);

		if (!emptySpouseMiddle) {
			put(l, sfm, spouseLastName);
		}
		put(l, spouseFirstName, spouseLastName);
		put(l, new ArrayList(), lastName);

		setStrategy(new DefaultStatesIterator(l));

	}
	/*
	protected void setupStrategy1() {

		List lastName = owner.getLastName();
		List firstName = owner.getFirstName();
		List middleName = owner.getMiddleName();
		List fm = new ArrayList(firstName);
		fm.addAll(middleName);

		List mf = new ArrayList(middleName);
		mf.addAll(firstName);

		List middleInitial = NameTokenList.getInitials(middleName);
		List firstInitial = NameTokenList.getInitials(firstName);

		List spouseFirstName = spouse.getFirstName();
		List spouseMiddleName = spouse.getMiddleName();
		List spouseLastName = spouse.getLastName();
		List sfm = new ArrayList(spouseFirstName);
		sfm.addAll(spouseMiddleName);

		List l = new ArrayList();

		boolean emptyMiddle =
			(StringUtils.isStringBlank(TokenList.getString(middleName)));
		boolean emptySpouseMiddle =
			(StringUtils.isStringBlank(TokenList.getString(spouseMiddleName)));

		if (!emptyMiddle) {
			put(l, fm, lastName);
			put(l, mf, lastName);
		}
		put(l, firstName, lastName);
		//put(l, middleName, lastName, type);

		if (!emptySpouseMiddle) {
			put(l, sfm, spouseLastName);
		}
		put(l, spouseFirstName, spouseLastName);
		put(l, new ArrayList(), lastName);

		setStrategy(new DefaultStatesIterator(l));

	}
	*/

	private void put(List l, List first, List last) {
		if (firstNameNotEmpty){
			if (StringUtils.isStringBlank (TokenList.getString(first))){
				return;
			}
		}		
		if (lastNameNotEmpty){
			if (StringUtils.isStringBlank (TokenList.getString(last))){
				return;
			}
		}		

		NameTokenList ntl =
			new NameTokenList(
				TokenList.getString(last),
				TokenList.getString(first),
				"");
		l.add(ntl);
	}

	/**
	 * @param b
	 */
	public void setFirstNameNotEmpty(boolean b) {
		firstNameNotEmpty = b;
	}

	/**
	 * @return
	 */
	public boolean isLastNameNotEmpty() {
		return lastNameNotEmpty;
	}

	/**
	 * @param b
	 */
	public void setLastNameNotEmpty(boolean b) {
		lastNameNotEmpty = b;
	}

	public void setAddMCNDefault(boolean add){
		addMCNDefault = add;
	}

	public boolean getAddMCNDefault(){
		return addMCNDefault;
	}
}

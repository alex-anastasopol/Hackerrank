/*
 * Created on Jun 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.module;

import org.apache.log4j.Category;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.Decision;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.token.iterator.TokenIterator;
import ro.cst.tsearch.search.tokenlist.AddressTokenList;
import ro.cst.tsearch.search.tokenlist.iterator.AddressTokenListIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AddressModuleStatesIterator extends ModuleStatesIterator {
	
	private static final Category logger = Category.getInstance(AddressModuleStatesIterator.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + AddressModuleStatesIterator.class.getName());
	
	protected AddressTokenList refAddress;
	
	protected int abbrevIteratorType = TokenIterator.ADDRESS_ABBREV_ALL; //TokenIterator.ADDRESS_ABBREV;

	public AddressModuleStatesIterator (long searchId){
		super(searchId);
	}

	protected void initInitialState(TSServerInfoModule initial){
		super.initInitialState(initial);

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();

		refAddress = new AddressTokenList(
												sa.getAtribute(SearchAttributes.P_STREETNAME), 
												sa.getAtribute(SearchAttributes.P_STREETDIRECTION), 
												sa.getAtribute(SearchAttributes.P_STREETSUFIX), 
												sa.getAtribute(SearchAttributes.P_STREETNO)
											);
		if (logger.isDebugEnabled())
			logger.debug("refAddress =" + refAddress);
	}

	protected void setupStrategy() {
		StatesIterator si ;
		if (StringUtils.isStringBlank(refAddress.getStreetNameAsString())){
			si = new DefaultStatesIterator();
		}else{
			si = new AddressTokenListIterator(refAddress, abbrevIteratorType);
		}
		setStrategy(si);
	}
	
	public Object current(){
		AddressTokenList atl = ((AddressTokenList) getStrategy().current());
		TSServerInfoModule crtState = new TSServerInfoModule(initialState);

		for (int i =0; i< crtState.getFunctionCount(); i++){
			TSServerInfoFunction fct = crtState.getFunction(i);
			if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_ST_N0_FAKE){
				fct.setParamValue(atl.getStreetNoAsString());
			}
			if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_ST_NAME_FAKE){
				fct.setParamValue(atl.getStreetNameAsString());
			}
		}

		if (logger.isDebugEnabled())
			logger.debug(" crtState for " + initialState + " =  " + crtState);
		return  crtState ;
	}
	
	public boolean timeToStop(Decision d,long searchId){

		if ((d == null)||(d.getServerResponse()==null)){ 
			return false;
		}

		ServerResponse sr = d.getServerResponse();
		ParsedResponse pr = sr.getParsedResponse();

		if (pr.isUnique() || (pr.getResultsCount() == ParsedResponse.UNKNOW_RESULTS_COUNT)) {
			if (logger.isDebugEnabled())
				logger.debug("obtained unique or unknown =>stop");
			return true;
		}else if (pr.isMultiple()){
			if (logger.isDebugEnabled())
				logger.debug("obtained multiple =>stop");
			return true;
		}
		else {
			return ( (AddressTokenListIterator) getStrategy()).timeToStop(d,searchId); 			
		}
	}

	/**
	 * @return
	 */
	public AddressTokenList getRefAddress() {
		return refAddress;
	}

}

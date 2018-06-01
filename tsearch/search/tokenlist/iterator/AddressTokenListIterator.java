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

import ro.cst.tsearch.search.Decision;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.OneStateIterator;
import ro.cst.tsearch.search.strategy.StrategyBasedIterator;
import ro.cst.tsearch.search.token.Token;
import ro.cst.tsearch.search.tokenlist.AddressTokenList;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.Log;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AddressTokenListIterator extends StrategyBasedIterator {

	private static final Category logger = Category.getInstance(AddressTokenListIterator.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + AddressTokenListIterator.class.getName());
	
	
	private AddressTokenList refAddress;
	
	private boolean prefixProcessed = false;
	private boolean suffixProcessed = false;
	
	
	private int abbrevIteratorType;
		

	public AddressTokenListIterator(AddressTokenList refAddress, int abbrevIteratorType){
		this.refAddress = refAddress;
		this.abbrevIteratorType = abbrevIteratorType;
		if (logger.isDebugEnabled())
			logger.debug(" new  AddressTokenListIterator(" + refAddress + ")");


		setupStrategy();
	}

	
	protected void setupStrategy(){
	
		setStrategy(new OneStateIterator(new Token(refAddress.getStreetNameAsString())));
	}

	public Object current(){
		String crtStreetName = ((Token) super.current()).getString();
		return new AddressTokenList(crtStreetName, "","" ,refAddress.getStreetNoAsString());
	}

	
	public boolean hasNext(long searchId){
		boolean rez = super.hasNext(searchId); 
		if (!rez){
			StatesIterator si = (StatesIterator) getStrategy();
			
			Token lastCriteria = new Token(refAddress.getStreetNameAsString());
			List remainedDirections = new ArrayList(refAddress.getDirections());
			if (si instanceof StreetSPTokenListIterator){
				StreetSPTokenListIterator sfi = (StreetSPTokenListIterator) si;
				lastCriteria = (Token) sfi.getLastCriteria();
				remainedDirections.removeAll(sfi.getUsedDirections());
			}
			List allAbrev = new ArrayList(refAddress.getStreetSufixes());
			allAbrev.addAll(remainedDirections);

			if (! prefixProcessed){
				if (logger.isDebugEnabled())
					logger.debug("try prefixe");
				setStrategy(new StreetSPTokenListIterator(lastCriteria ,remainedDirections, true, abbrevIteratorType,searchId));
				prefixProcessed = true;
				rez = hasNext(searchId);
			}else if (!suffixProcessed){
				if (logger.isDebugEnabled())
					logger.debug("try sufixe");
				setStrategy(new StreetSPTokenListIterator(lastCriteria, allAbrev, false , abbrevIteratorType,searchId));
				suffixProcessed = true;
				rez = hasNext(searchId);
			}
		}
		return rez;
	}
	
	
	public boolean timeToStop(Decision d,long searchId){
		if ((d == null)||(d.getParsedResponse()==null)){
			return false ;
		}

		ParsedResponse pr = d.getParsedResponse();
		
		if (pr.isUnique()){
			return true;
		}
		
		StatesIterator si = (StatesIterator) getStrategy();

		if (si instanceof OneStateIterator){
			return false;
		}else{
			StreetSPTokenListIterator spi = (StreetSPTokenListIterator) si;
			if (pr.isMultiple()){
				if (logger.isDebugEnabled())
					logger.debug("obtained multiple...another sufix/prefix must be added...");
				spi.retainTheCurrentStateAndChangeStrategy(searchId);
			}else if (pr.isNone()){
				if (!strategy.hasNext(searchId)){
					suffixProcessed = true;
				}
			}
			return false; //normally shouldn't arrive here
		}
	}
}

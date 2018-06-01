
package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.strategy.StrategyBasedIterator;

import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;

/**
 * @author mihaib
 *
 *
 */
public class CombinedNameIterator extends StrategyBasedIterator {

	private static final long serialVersionUID = -5046483774397014595L;
	
	
	Set<NameI> derivNames = new LinkedHashSet<NameI>();
	int numberOfYearsAllowed = 1;	

	public CombinedNameIterator(Set<NameI> derivNames, int numberOfYearsAllowed){

		this.derivNames = derivNames;
		this.numberOfYearsAllowed = numberOfYearsAllowed;

	}

	public void init(){
		setupStrategy();
	}

	protected void setupStrategy(){

		List<CombinedObject> l = new ArrayList<CombinedObject>();

		for (NameI name : derivNames){
			for(int i = 0; i < numberOfYearsAllowed; i++){
				l.add(new CombinedObject(name, i));
			}	
		}
		setStrategy(new DefaultStatesIterator(l));
	}

	public class CombinedObject{
		
		
		private NameI 	nameObject 		= new Name();
		private int 	numberToCutFromYear 		= 0;
		
		public CombinedObject(NameI name, int numberToCutFromYear){
			this.nameObject = name;
			this.numberToCutFromYear = numberToCutFromYear;
		}
		
		public void setNameObject(Name nameObject){
			this.nameObject = nameObject;
		}
		
		public NameI getNameObject(){
			return nameObject;
		}
		
		public void setNumberToCutFromYear(int numberToCutFromYear){
			this.numberToCutFromYear = numberToCutFromYear;
		}
		
		public int getNumberToCutFromYear(){
			return numberToCutFromYear;
		}
	}
}

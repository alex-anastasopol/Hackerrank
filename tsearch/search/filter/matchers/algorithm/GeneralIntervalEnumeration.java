package ro.cst.tsearch.search.filter.matchers.algorithm;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Should manage a string like "1, 2 3-5" and provide an enumeration like "1 2 3 4 5". For "1, 102-J" should return "1 102J J102 102-J J-102"<br>
 * Works in close connection with {@link LotInterval}
 *  
 * @author Andrei
 *
 */
public class GeneralIntervalEnumeration {

	private Set<String> allElements = new LinkedHashSet<String>();
	
	public GeneralIntervalEnumeration(String lot) {
		
		String[] lotIntervals = lot.trim().replaceAll("\\s*-\\s*", "-").split("[,\\s]+");
		for (String string : lotIntervals) {
			LotInterval lotInterval = new LotInterval(string);
			allElements.addAll(lotInterval.getLotList());
		}
	}

	public List<String> getEnumerationList() {
		return getEnumerationList(100);
	}

	public List<String> getEnumerationList(int maxEntries) {
		List<String> result = new ArrayList<String>();
		int size = 0;
		for (String singleElement : allElements) {
			if(size > maxEntries) {
				break;
			}
			result.add(singleElement);
		}
		return result;
	}
	
	public static void main(String[] args) {
		GeneralIntervalEnumeration li = null;
    	
    	li = new GeneralIntervalEnumeration(" ");
    	System.err.println(li.getEnumerationList());
    	
    	li = new GeneralIntervalEnumeration("1 2");
    	System.err.println(li.getEnumerationList());
    	li = new GeneralIntervalEnumeration("101 -J ");
    	System.err.println(li.getEnumerationList());
    	li = new GeneralIntervalEnumeration("101J");
    	System.err.println(li.getEnumerationList());
    	
    	li = new GeneralIntervalEnumeration("1,101 -J ,12 K");
    	System.err.println(li.getEnumerationList());
    	
    	li = new GeneralIntervalEnumeration("1- 2");
    	System.err.println(li.getEnumerationList());
    	li = new GeneralIntervalEnumeration("1-3");
    	System.err.println(li.getEnumerationList());
    	
    	
    	li = new GeneralIntervalEnumeration("1 2 3");
    	System.err.println(li.getEnumerationList());
    	
    	li = new GeneralIntervalEnumeration("1, 2 4");
    	System.err.println(li.getEnumerationList());
    	
    	li = new GeneralIntervalEnumeration("1, 2 3- 5");
    	System.err.println(li.getEnumerationList());
	}
}

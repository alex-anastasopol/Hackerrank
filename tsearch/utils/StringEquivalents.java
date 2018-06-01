package ro.cst.tsearch.utils;

import java.util.LinkedList;
import java.util.Vector;

/**
 * This class provides a list of equivalent Strings for a given String
 * The result is created using an internal list of equivalents
 * 
 * @author andrei
 *
 */
public class StringEquivalents {
	private Vector<String[]> equivList = null;
	private static StringEquivalents stringEquivalents = null;
	
	private StringEquivalents(){
        equivList = new Vector<String[]>();
        equivList.add( new String[] {"","the"} );
        equivList.add( new String[] {"place", "pl"} );
        equivList.add( new String[] {"", "ev"} );
        equivList.add( new String[] {"", "est"} );
        equivList.add( new String[] {"", "estates"} );
        equivList.add( new String[] {"park", "pk"} );
        equivList.add( new String[] {"st.", "st"} );
     
        equivList.add( new String[] {"1", "I"} );
        equivList.add( new String[] {"first", "1st"} );
        equivList.add( new String[] {"second", "2nd"} );
        equivList.add( new String[] {"third", "3rd"} );
        equivList.add( new String[] {"fourth", "4th"} );
        equivList.add( new String[] {"fifth", "5th"} );
        equivList.add( new String[] {"sixth", "6th"} );
        equivList.add( new String[] {"seventh", "7th"} );
        equivList.add( new String[] {"eighth", "8th"} );
        equivList.add( new String[] {"ninth", "9th"} );
        equivList.add( new String[] {"tenth", "10th"} );
        equivList.add( new String[] {"eleventh", "11th"} );
        equivList.add( new String[] {"twelfth", "12th"} );
        equivList.add( new String[] {"thirteenth", "13th"} );
        equivList.add( new String[] {"fourteenth", "14th"} );
        equivList.add( new String[] {"fifteenth", "15th"} );
        equivList.add( new String[] {"sixteenth", "16th"} );
        equivList.add( new String[] {"seventeenth", "17th"} );
        equivList.add( new String[] {"eighteenth", "18th"} );
        equivList.add( new String[] {"nineteenth", "19th"} );
        equivList.add( new String[] {"twentieth", "20th"} );
        equivList.add( new String[] {",", ""} );
        equivList.add( new String[] {"HEIGHTS", "HGTS", "HTS"});
        
        equivList.add( new String[] {"ONE", "1"} );
        equivList.add( new String[] {"TWO", "2"} );
        equivList.add( new String[] {"THREE", "3"} );
        equivList.add( new String[] {"FOUR", "4"} );
        equivList.add( new String[] {"FIVE", "5"} );
        equivList.add( new String[] {"SIX", "6"} );
        equivList.add( new String[] {"SEVEN", "7"} );
        equivList.add( new String[] {"EIGHT", "8"} );
        equivList.add( new String[] {"NINE", "9"} );
        equivList.add( new String[] {"TEN", "10"} );
        equivList.add( new String[] {"ELEVEN", "11"} );
        equivList.add( new String[] {"TWELVE", "12"} );
        equivList.add( new String[] {"THIRTEEN", "13"} );
        equivList.add( new String[] {"FOURTEEN", "14"} );
        equivList.add( new String[] {"FIFTEEN", "15"} );
        equivList.add( new String[] {"SIXTEEN", "16"} );
        equivList.add( new String[] {"SEVENTEEN", "17"} );
        equivList.add( new String[] {"EIGHTEEN", "18"} );
        equivList.add( new String[] {"NINETEEN", "19"} );
        equivList.add( new String[] {"TWENTY", "20"} );
        equivList.add( new String[] {"THIRTY", "30"} );
        equivList.add( new String[] {"FORTY", "40"} );
        equivList.add( new String[] {"FIFTY", "50"} );
        equivList.add( new String[] {"SIXTY", "60"} );
        equivList.add( new String[] {"SEVENTY", "70"} );
        equivList.add( new String[] {"EIGHTY", "80"} );
        equivList.add( new String[] {"NINETY", "90"} );
	}
	
	public static StringEquivalents getInstance() {
		if(stringEquivalents == null){
			stringEquivalents = new StringEquivalents();
		}
		return stringEquivalents;
	}
	
	/**
	 * Returns a vector with equivalent elements generated from the equivalent list
	 * @param originalString
	 * @return the vector with the equivalents
	 */
	public Vector<String> getEquivalents(String originalString){
		Vector<String> equivalents = new Vector<String>();
		String tokens[] = originalString.toLowerCase().split("[ ]+");
		LinkedList<String[]> bag = new LinkedList<String[]>();
		bag.add(tokens);
		
		for (int i = 0; i < tokens.length; i++) {
			int numberOfElementsToRemove = bag.size();
			for (int j = 0; j < numberOfElementsToRemove; j++){
				String currentToken = tokens[i];
				String[] currentEquivalents = getEquivalentForSingleToken(currentToken); 
				if( currentEquivalents != null && currentEquivalents.length > 1 ){					
					String[] bagElement = bag.removeFirst();
					
					for (int k = 0; k < currentEquivalents.length; k++) {
						String[] newElem = StringUtils.replaceElementInArrayAtPosition(bagElement, currentEquivalents[k],i);
						bag.addLast(newElem);
					}
				}
			}
		}
		StringBuilder temp = new StringBuilder();
		for (String[] strings : bag) {
			temp = new StringBuilder();
			for (String token : strings) {
				if(token.length()>0)
					temp.append(token + " ");
			}
			equivalents.add(temp.toString().trim().toUpperCase());
		}
		return equivalents;
	}

	/**
	 * Returns the equivalent list for the given token
	 * @param currentToken
	 * @return
	 */
	private String[] getEquivalentForSingleToken(String currentToken) {
		for (String[] equivListElem : equivList) {
			for (int i = 0; i < equivListElem.length; i++) {
				if(equivListElem[i].equalsIgnoreCase(currentToken))
					return equivListElem;
			}
		}
		return null;
	}

	
}

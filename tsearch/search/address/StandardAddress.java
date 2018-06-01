package ro.cst.tsearch.search.address;
import java.util.ResourceBundle;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.utils.LowPriorityFileLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

/**
 * Standard address
 * 
 * Current implementation does not contain city - for a complete list of address elements look at AddressInterface
 * maybe make named constants for the address parts in the interface class to make it more clear
 * make sure this treats the highways, rural things and so on right
 * add in the exceptions at the ???
 * see about returning clones if the people plan to modify the stuff we return
 */
public class StandardAddress implements AddressInterface {

	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	private static boolean useNewVersionFlag = Boolean.parseBoolean(rbc.getString("use.new.address.matcher").trim());
	private static boolean useNewVersion(){ return useNewVersionFlag; }
    
	static final long serialVersionUID = 10000000;
	protected static final Category logger= Logger.getLogger(StandardAddress.class);
	
	public static final int STREET_NUMBER			= 0;
	public static final int STREET_PREDIRECTIONAL	= 1;
	public static final int STREET_NAME				= 2;
	public static final int STREET_SUFFIX			= 3;
	public static final int STREET_POSTDIRECTIONAL	= 4;
	public static final int STREET_SEC_ADDR_IDENT	= 5;
	public static final int STREET_SEC_ADDR_RANGE	= 6;	
	
	/**
	 * Address elements.
	 */
	public String [] addressElements = null;
	/**
	 * Original address input string.
	 */
	public String addrInputString = null;

	/*
	 * Used to log addresses into a file
	 */
	private static LowPriorityFileLogger addressLogger =  new LowPriorityFileLogger(ServerConfig.getTsLogsPathPrefix() + "tslogs/created-addresses-old.txt",100);

	/**
	 * Basic constructor that builds the address out of the given string.
	 */
	public StandardAddress(String addressString) {
	
		// save a copy of original input address string
		addrInputString = addressString;

		// first init the address elements
		addressElements = new String[7];

		if(useNewVersion()){
			ro.cst.tsearch.search.address2.StandardAddress newStandardAddress =
				new ro.cst.tsearch.search.address2.StandardAddress(addressString);
			for(int i=0; i<7; i++)
				addressElements[i] = newStandardAddress.getAddressElement(i);
			//inputAddressLogger.logString(addressString);
			return;
 		}
		
		// normalize the address
		Normalize standardForm = new Normalize();
		String normalAddress = new String(standardForm.trim(addressString));

		// split the address up to fit it in the elements
		//String [] parts = normalAddress.split(" ");
		String[] parts = AddressStringUtils.splitString(normalAddress);	// catalinc

		int elementIndex = 0;
		int index = 0;
		// indian style ;-)
		while (elementIndex < 7 && index < parts.length) {
			// see what element we are processing
			switch (elementIndex) {
				case 0: // we are processing primary address number
					if (standardForm.isNumber(parts[index])) {	// we must have a number on this position
						// all is good, put it in
						// no append because we can only have one street number ???
						addressElements[elementIndex] = parts[index];
						index++; elementIndex++;		// increase the indices
					} else {
						// we were supposed to have a number, but we don;t
						elementIndex++;				// increase just the elementIndex
					}
					break;
				case 1:	// we are processing the predirection
					if (standardForm.isDirectional(parts[index])) {	// it must be a direction to match
						// put in
						addressElements[elementIndex] = parts[index];
						index++; elementIndex++;		// increase the indices
					} else {
						// not the directional we were expecting, move on
						elementIndex++;
					}
					break;
				case 2:	// street name
					// uncomment this to guarantee at least one token in there
					// int index2 = index + 1;				// here we must look for the "end"
					int index2 = index;
					// the stop cases here have to be the beginning cases for the statements below, or the
					// method might leave address parts out, or just fail
					while (index2 < parts.length &&
						!standardForm.isSuffix(parts[index2]) &&		// case 3 suffix
						!standardForm.isDirectional(parts[index2]) &&		// case 4 directional
						parts[index2].compareTo("#") != 0 &&			// case 5 sec. ident
							!standardForm.isIdentifier(parts[index2]) &&
						parts[index2].length() != 1						// case 6 sec range , catalinc parts[index] => parts[index2]
						)				
						index2++;
					// theoretically all we got in there is the main address now
					if (index != index2) {
						String addressMain = new String(parts[index]);
						for (int i = index + 1; i < index2; i++)
							addressMain = addressMain.concat(" " + parts[i]);
						if (addressElements[2] == null) addressElements[2] = addressMain;
						else addressElements[2] = addressElements[2].concat(" " + addressMain);
					}
					// increment the indices
					elementIndex++; index = index2;
					break;
				case 3:	// we are processing the suffix
					if (standardForm.isSuffix(parts[index])) {	// it must be a suffix to match
						// see if the next one is also a suffix, and if so, count this one as part of the
						// main address name
						if (index + 1 < parts.length && standardForm.isSuffix(parts[index+1])) {
							while (index + 1 < parts.length && standardForm.isSuffix(parts[index+1])) {
								// as long as we have one more suffix left, put in main address part
								if (addressElements[2]==null) addressElements[2] = parts[index];
								else addressElements[2] = addressElements[2].concat(" " + parts[index]);
								index++;
							}
							// we know we have suffix next to us
							elementIndex = 3;
						} else {
							// put in, taking care if we already have one
							if (addressElements[elementIndex] != null) {
								// clearly a mistake, add this suffix at the end of name
								if (addressElements[2]==null) addressElements[2] = 
									addressElements[elementIndex];
								else addressElements[2] = addressElements[2].concat(
									" " + addressElements[elementIndex]);
								addressElements[elementIndex] = null;
							}
							addressElements[elementIndex] = parts[index];
							index++; elementIndex++;		// increase the indices
						}
					} else {
						// not the suffix we were expecting, move on
						elementIndex++;
					}
					break;
				case 4:	// we are processing the postdirectional
					// maybe make a case here that if the main address part is "Street" put on at the beginning ???
					if (standardForm.isDirectional(parts[index])) {	// it must be a direction to match
						// put in, again taking care if there already is one
						if (addressElements[elementIndex] != null) {
							if (addressElements[2]==null) addressElements[2] = 
								addressElements[elementIndex];
							else addressElements[2] = addressElements[2].concat(
								" " + addressElements[elementIndex]);
							addressElements[elementIndex] = null;
						}
						addressElements[elementIndex] = parts[index];
						index++; elementIndex++;		// increase the indices
					} else {
						// not the directional we were expecting, move on
						elementIndex++;
					}
					break;
				case 5:	// we are processing the secondary address info
					if (standardForm.isIdentifier(parts[index])|| 
						parts[index].compareTo("#") == 0) {	// it must be an identifier to match
						// put in
						addressElements[elementIndex++] = parts[index++];
						// copy the secondary address range as well, if it is there
						// copy EVERYTHING to the end now
						if (index < parts.length) {
							addressElements[elementIndex] = parts[index++];
							while (index < parts.length && !parts[index].matches("[A-Z0-9]{3,}"))
								addressElements[elementIndex] = 
									addressElements[elementIndex].concat(parts[index++]);
						}
						// we don't have any more elements or parts
						if(index == parts.length)	// catalinc
							{index++; elementIndex++;		// increase the indices, signal the end
							}
						else
							{
								elementIndex = 2; 
							}
					} else {
						// not the secondary address info we were expecting
						// try to pass it on to the secondary address range
						elementIndex = 6; // go to sec addr info
					}
					break;
				case 6: // secondary address range, see if it  number or is single letter. if not, put
					// at name
					if (parts[index].length() == 1
						|| parts[index].matches("[0-9A-Z]+[-][0-9A-Z]+")	// catalinc ZZZ-ZZZ
						|| parts[index].matches("[A-Z]+[0-9]+")				// catalinc	Z09
						) {			
						// should DIE here if we already have secondary address info
						// addressElements[6] = parts[index];
						addressElements[6] = (addressElements[6] == null ? parts[index] : addressElements[6].concat(parts[index]));	// catalinc
						 
						// continue reprocessing at predirection
						index++; elementIndex = 1;
					} else {
						// we just let the main case handle the logic
						elementIndex = 2;	// so go back to address name
						//elementIndex = 0;	// catalinc
					}
					break;
			}
		}
		
		// check the special cases that would be a pain to model in the structure above
		
		// address has no number but it can be found at the end
		if (addressElements[0] == null && addressElements[2] != null) {
			String [] nameparts = addressElements[2].split(" ");
			// can only do this case if we have more than one element, and it is a pure number
			if (nameparts.length > 1 && nameparts[nameparts.length -1].matches("(^([0-9]*)$)")) {
				addressElements[0] = nameparts[nameparts.length -1];
				String newName = new String(nameparts[0]);
				for (int i = 1; i < nameparts.length -1; i++)
					newName = newName.concat(" " + nameparts[i]);
				addressElements[2] = newName;
			}
		}
		
		// address has no number but it can be found inside address 
		if(addressElements[0] == null && addressElements[2] != null) {
			String nameparts[] = addressElements[2].split(" ");
			if(nameparts.length > 1) {
				for(int i = 0; i < nameparts.length; i++) {
					if(nameparts[i].matches("(^([0-9]+)$)")) {
						// ok ,we have a number
						addressElements[0] = nameparts[i];	// get the number
						String newName = new String("");	// rebuild address
						for(int j = 0; j < nameparts.length; j++) {
							if(j!=i)
								newName = newName.concat(nameparts[j]) + " ";
						}
						addressElements[2] = newName.trim();
						break;
					}
				}
			}
		}
		
		// address has no unit but it can be fount inside address
		if(addressElements[6] == null && addressElements[2] != null) {
			String nameparts[] = addressElements[2].split(" ");
			if(nameparts.length > 1) {
				for(int i = 0; i < nameparts.length; i++) {
					if(nameparts[i].matches("(^([0-9]+[A-Z]+)$)")) {
						// ok ,we have sec addr info
						addressElements[6] = nameparts[i];	// get the number
						String newName = new String("");	// rebuild address
						for(int j = 0; j < nameparts.length; j++) {
							if(j!=i)
								newName = newName.concat(nameparts[j]) + " ";
						}
						addressElements[2] = newName.trim();;
						break;
					}
				}
			}
		}
		// all the parts are "fine" now ... really ? ;-)
		
		// log the newly constructed address
		addressLogger.logString("|" + addressString + "| -> |" + this + "|");
		
	}

	/**
	 * Get address in terms of the defined elements, in standard order.
	 */
	public String [] getAddressElements(){              
		// simply return the address elements
		return addressElements;
	}

	/**
	 * Get a specific combination of address elements
	 */
    public String [] getAddressElements(String perm){ 
		// return the address elements in terms of the specific arrangement
		String [] ret = new String[perm.length()];
		for (int i = 0; i < perm.length(); i++)
			ret[i] = addressElements[perm.charAt(i) - '0'];
		return ret;
	}

	/**
	 * Get specific element.
	 */
    public String getAddressElement(int elementNumber){ 
		return (addressElements[elementNumber] == null 
			? "" : addressElements[elementNumber]);
	}

    /**
	 * Set specific element.
	 */
    public void setAddressElement(int elementNumber, String value){ 
    	addressElements[elementNumber] = value;
    	
	}
    
	/**
	 * Get the entire address with all known elements as one string
	 */
    public String getAddress(){ 
		String ret = new String();
		int i = 0;
		for (; i < 7; i++) if (addressElements[i] != null) break;
		ret = addressElements[i];
		for (i++; i < 7; i++) if (addressElements[i] != null) ret = ret.concat(" " + addressElements[i]);
		return ret;
	}

	/**
	 * Get the entire address with all known elements as one string
	 */
   public String getAddressMod(){ 
		String ret;
		int i = 0;
		ret = addressElements[i]==null?new String():addressElements[i];
		for (i++; i < 7; i++) ret = ret.concat(":" + (addressElements[i]==null?new String(""):addressElements[i]));
		return ret;
	}

	/**
	 * You know....
	 */
	public String toString() {
		return getAddressMod();
	}

	/**
	 * Returns original input string for address.
	 */
	public String getAddrInputString() {
		return addrInputString;
	}

	/**
	 * Returns standard address element.
	 */
	public String getStandardAddressElement(int index) {
		if(index < 0 || index > 6) 
			throw new IllegalArgumentException("element index must be between 0 and 6, given index=" + index);
		return (addressElements[index] == null ? "" : addressElements[index]);
	}

	/**
	 * Clears the field from that index (sets the value to null)
	 * @param index the given index
	 */
	public void clear(int index) {
		if(index < 0 || index > 6) 
			throw new IllegalArgumentException("element index must be between 0 and 6, given index = " + index);
		this.addressElements[index] = null;
		
	}
	
	public boolean isEmpty() {
		for (int i = 0; i < addressElements.length; i++) {
			if(StringUtils.isNotEmpty(addressElements[i])) {
				return false;
			}
		}
		return true;
	}
	
}

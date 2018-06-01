package ro.cst.tsearch.search.address;

import java.io.Serializable;

/**

Common address components are:
1. primary address number				-- 101
2. predirectional						-- W
3. street name							-- Main
4. suffix								-- ST
5. postdirectional						-- S
6. secondary address indentifier		-- APT
7. and secondary address range			-- 12

Arbitrary arrangements can be specified as strings: "6713" = "APT 12 101 Main"

The address can have one or more of these parts empty, in which case in the array-return functions the 
    corresponding string will be null; in string-return functions the element will simply not appear
*/

public interface AddressInterface extends Serializable {
    
    static final long serialVersionUID = 10000000;

	/**
	 * Get address in terms of the defined elements, in standard order.
	 */
	public String [] getAddressElements();
	/**
	 * Get a specific combination of address elements.
	 */
	public String [] getAddressElements(String perm);
	/**
	 * Get specific element.
	 */ 
	public String getAddressElement(int elementNumber);
	/**
	 * Get the entire address with all known elements as one string.
	 */ 
	public String getAddress();
	/**
	 * Get original address string.
	 */	
	public String getAddrInputString(); 
}

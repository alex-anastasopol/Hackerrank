package ro.cst.tsearch.search.name;

/**
 * Name
 * 
 * Person name holder class.
 * Name elements (identified by now ...)
 * 
 * 	- NAME_PREFIX	Mrs. 
 * 	- FIRST_NAME	John.
 * 	- MIDDLE_NAME	Michael
 * 	- LAST_NAME		Douglas
 * 	- NAME_SUFIX	Sr.
 * 	- NAME_DEGREE	Phd.
 * 
 * @author catalinc
 * @version 1.0
 */
public class Name {
	
	/**
	 * Title name, eg: Mrs. Ms.	
	 * <b>optional</b>
	 */
	public static final int NAME_PREFIX 	= 0;	
	/**
	 * Person first name.
	 */
	public static final int FIRST_NAME		= 1;
	/**
	 * Person middle name.
	 */
	public static final int MIDDLE_NAME		= 2;
	/**
	 * Person last name.
	 */
	public static final int LAST_NAME		= 3;
	/**
	 * Name sufix, eg: Jr. Sr. III
	 * <b>optional</b>
	 */
	public static final int NAME_SUFFIX		= 4;
	/**
	 * Name degree, eg: B.S. PhD
	 * <b>optional</b>
	 */
	public static final int  NAME_DEGREE	= 5;
	/**
	 * Name elements.
	 */
	private String[] nameElements			= new String[]{"","","","","",""};
	/**
	 * Company name flag.
	 */
	private boolean company					= false;

	/**
	 * Empty constructor.
	 */
	public Name() {
		reset();
	}

	/**
	 * Basic constructor.
	 * 
	 * @param firstName
	 * @param middleName
	 * @param lastName
	 */
	public Name(String firstName, String middleName, String lastName) {
		reset();
		setNameElement(FIRST_NAME,firstName);
		setNameElement(MIDDLE_NAME,firstName);
		setNameElement(LAST_NAME,firstName);
	}

	/**
	 * Extended constructor.
	 * 
	 * @param namePrefix
	 * @param firstName
	 * @param middleName
	 * @param lastName
	 * @param nameSufix
	 * @param nameDegrees
	 */
	public Name(String namePrefix,
				String firstName,
				String middleName,
				String lastName,
				String nameSufix,
				String nameDegree) {
		reset();
		setNameElement(NAME_PREFIX,namePrefix);
		setNameElement(FIRST_NAME,firstName);
		setNameElement(MIDDLE_NAME,firstName);
		setNameElement(LAST_NAME,firstName);
		setNameElement(NAME_SUFFIX,nameSufix);
		setNameElement(NAME_DEGREE,nameDegree);				
	}

	/**
	 * Reset name elements.
	 */
	public void reset() {
		nameElements = new String[] {"","","","","",""};
		company = false;
	}

	/**
	 * Sets element name at specified index.
	 * 
	 * @param index
	 * @param element
	 */
	public void setNameElement(int index,String element) {
		if(index < 0 || index >= 6)
			throw new IllegalArgumentException("index must be between 0 and 6");
		
		nameElements[index] = element;
	}
	
	/**
	 * Get element name at specified index.
	 * 
	 * @param index
	 * @return String
	 */
	public String getNameElement(int index) {
		if(index < 0 || index >= 6)
			throw new IllegalArgumentException("index must be between 0 and 6");
		
		return nameElements[index];		
	}
	
	/**
	 * Normalized form:	PREFIX:FIRST:MIDDLE:LAST:SUFFIX:DEGREE
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String s = "";
		for(int i = 0; i < nameElements.length; i++) {
			s += nameElements[i] 
				+ (i == nameElements.length - 1 ? "" : ":");
		}
		return s;
	}
	
	/**
	 * Test this name for emtpyness , all 6 name tokens must be "".
	 */
	public boolean isEmpty() {
		for(int i = 0; i < 6; i++) {
			if(!nameElements[i].equals("")) {
				return false;		
			}
		}
		return true;
	}
	/**
	 * @return true if name denotes a company.
	 */
	public boolean isCompany() {
		return company;
	}

	/**
	 * @param b company to be set.
	 */
	public void setCompany(boolean b) {
		company = b;
	}

} 

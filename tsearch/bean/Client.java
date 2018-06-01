/*
 * Created on May 27, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.bean;

/**
 * @author nae
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class Client {
	
	private String Name = " ";
	private String streetNo=" ";
	private String streetName=" ";
	private String City=" ";
	private String State=" ";
	private String Zip=" ";
	private String FileCaseNr=" ";
	private String MortgagorFirstName =" ";	
	private String MortgagorLastName = " ";
	  
	
	/**
	 * @return
	 */
	public String getName() {
		return Name;
	}

	/**
	 * @return
	 */
	public String getState() {
		return State;
	}

	/**
	 * @return
	 */
	public String getStreetName() {
		return streetName;
	}

	/**
	 * @return
	 */
	public String getStreetNo() {
		return streetNo;
	}

	/**
	 * @return
	 */
	public String getZip() {
		return Zip;
	}

	/**
	 * @param string
	 */
	public void setName(String string) {
		Name = string;
	}

	/**
	 * @param string
	 */
	public void setState(String string) {
		State = string;
	}

	/**
	 * @param string
	 */
	public void setStreetName(String string) {
		streetName = string;
	}

	/**
	 * @param string
	 */
	public void setStreetNo(String string) {
		streetNo = string;
	}

	/**
	 * @param string
	 */
	public void setZip(String string) {
		Zip = string;
	}

	/**
	 * @return
	 */
	public String getCity() {
		return City;
	}

	/**
	 * @param string
	 */
	public void setCity(String string) {
		City = string;
	}

	/**
	 * @return
	 */
	public String getFileCaseNr() {
		return FileCaseNr;
	}

	/**
	 * @return
	 */
	public String getMortgagorFirstName() {
		return MortgagorFirstName;
	}

	/**
	 * @return
	 */
	public String getMortgagorLastName() {
		return MortgagorLastName;
	}

	/**
	 * @param string
	 */
	public void setFileCaseNr(String string) {
		FileCaseNr = string;
	}

	/**
	 * @param string
	 */
	public void setMortgagorFirstName(String string) {
		MortgagorFirstName = string;
	}

	/**
	 * @param string
	 */
	public void setMortgagorLastName(String string) {
		MortgagorLastName = string;
	}

}

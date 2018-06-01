package ro.cst.tsearch.propertyInformation;

import org.apache.log4j.Category;
//import ro.cst.tsearch.search.tokenlist.AddressTokenList;
//import ro.cst.tsearch.search.tokenlist.TokenList;
import ro.cst.tsearch.utils.StringUtils;

public class Address {
	private String miStNo = "0"; 
	private String msStDirection="",
					msStName="",
					msStSuffix="",
					msStPostDirection="",
					msCity="",
					msStateAbbr="",
					msZip="",
					miUnit = "";
					
	public static final int FORMAT_DEFAULT = 0; 
	public static final int FORMAT_STREET_ONLY = 1; 
	public static final int FORMAT_CITY_STATE_ZIP = 2; 
	public static final int FORMAT_LOG = 3; 

	protected static final Category logger = Category.getInstance(Address.class.getName());	
	
	public Address(){}
	
	//!!!!!!
	//Street number it can't be an int because sometimes contains a letter. B 4450,  e.g. ILKaneIS : 41W483 BEITH RD
	//!!!!!!
	
	public Address(String stNo, String stDirection, String stName, String stSuffix,String postDirection, String unit, String city, String stateAbbr, String zip) {
		/*int no = 0;
		try {
			no = Integer.parseInt(stNo);
		} catch (NumberFormatException e) {
			//logger.error("Invalid street no: [" + stNo + "]");
		}*/
		init(stNo, stDirection, stName, stSuffix, postDirection, unit, city, stateAbbr, zip);
	}
	
	public Address(String stNo, String stDirection, String stName, String stSuffix, String unit, String city, String stateAbbr, String zip) {
		/*int no = 0;
		try {
			no = Integer.parseInt(stNo);
		} catch (NumberFormatException e) {
			//logger.error("Invalid street no: [" + stNo + "]");
		}*/
		init(stNo, stDirection, stName, stSuffix, unit, city, stateAbbr, zip);
	}
	/*
	public Address(int stNo, String stDirection, String stName, String stSuffix, String unit, String city, String stateAbbr, String zip) {
		init(stNo, stDirection, stName, stSuffix, unit, city, stateAbbr, zip);
	}
	*/
	/*
	public Address(AddressTokenList street, String city, String stateAbbr, String zip) {
		init(street, city, stateAbbr, zip);
	}
	*/
	/*
	public Address(AddressTokenList street) {
		init(street, "", "", "");
	}
	*/

	private void init(	String stNo, String stDirection, String stName, String stSuffix, String postDirection, String unit, String city, String stateAbbr, String zip) {
		this.miStNo = stNo;
		this.msStDirection = stDirection;
		this.msStName = stName;
		this.msStPostDirection =  postDirection;
		this.msStSuffix =  stSuffix;
		
		this.miUnit = unit;
		this.msCity = city;
		this.msStateAbbr = stateAbbr;
		this.msZip = zip;
	}
	
	private void init(	String stNo, String stDirection, String stName, String stSuffix, String unit, String city, String stateAbbr, String zip) {
		init(stNo, stDirection, stName, stSuffix, "", unit, city, stateAbbr, zip);
	}
	/*
	private void init(AddressTokenList street, String city, String stateAbbr, String zip) {
		String stNo = TokenList.getString(street.getStreetNo()); 
		String stDirection = TokenList.getString(street.getDirections()); 
		String stName = TokenList.getString(street.getStreetName()); 
		String stSuffix = TokenList.getString(street.getStreetSufixes());
		
		init(Integer.parseInt(stNo), stDirection, stName, stSuffix, "", city, stateAbbr, zip);
	}
	*/
	
	public boolean flexibleEquals(Object o,long searchId) {
		if (o == this)
			return true;
		if (!(o instanceof Address))
			return false;
		Address a = (Address) o;
		
		if(a.isEmptyAddress()) return true;	// no candidate address
		
		return (a.miStNo.equals(miStNo)  
				|| a.miStNo.equals("0") 
				|| "0".equals(miStNo))
			&& (a.miUnit.equals(miUnit)  
				|| StringUtils.isStringBlank(miUnit) 
				|| StringUtils.isStringBlank(a.miUnit))
			&&  StringUtils.flexibleEqualsIgnoreCaseAndBlank(a.msStName, msStName,searchId) 			
			
			;
	}
	
	public boolean hasStreetName(){
		return 	!StringUtils.isStringBlank(msStName);
	}

	public String toString(){
		return "Address(" +
			miStNo + "::" +
			msStDirection + "::" +
			msStName + ":: " +
			msStSuffix + "::" +
			msStPostDirection + "::" +
			miUnit + "::" +
			msCity + "::" +
			msStateAbbr + "::" +
			msZip + "" +
			")";
	}
	
	public String toString(int format){
		
		String stNo = (!miStNo.equals("0")) ? miStNo + "" :"";
		String stDirection = msStDirection;
		String stName = msStName.toUpperCase();
		String stSuffix = msStSuffix;
		String unit = miUnit;//(miUnit != 0) ? "#" + miUnit + "" :"";
		String city = StringUtils.capitalize(msCity);
		String stateAbbr = msStateAbbr;
		String zip = msZip;
		
		String rez = "";
		if (format == FORMAT_STREET_ONLY) {
			rez += (!StringUtils.isStringBlank(stNo)) ? stNo:"";			
			rez += (!StringUtils.isStringBlank(stDirection)) ? " " + stDirection :"";		
			if (StringUtils.isStringBlank(stNo)){rez = rez.trim();}
			rez += (!StringUtils.isStringBlank(stName)) ? " " + stName  :"";			
			rez += (!StringUtils.isStringBlank(stSuffix)) ? " " + stSuffix  :"";	
			rez += (!StringUtils.isStringBlank(msStPostDirection)) ? " " +msStPostDirection :"";
			rez += (!StringUtils.isStringBlank(unit)) ?" " + unit :"";			
		}else if (format == FORMAT_CITY_STATE_ZIP) { 
			rez += (!StringUtils.isStringBlank(city)) ? city + ", " :"";			
			rez += (!StringUtils.isStringBlank(stateAbbr)) ? stateAbbr + " " :"";			
			rez += (!StringUtils.isStringBlank(zip)) ? zip + "" :"";			
		} else if (format == FORMAT_LOG){			
			rez += stNo;
			if(!"".equals(stDirection)){ rez += " " + stDirection; }
			if(!"".equals(stName)){ rez += " " + stName; }
			if(!"".equals(stSuffix)){ rez += " " + stSuffix; }
			if(!"".equals(msStPostDirection)){ rez += " " + msStPostDirection; }
			if(!"".equals(unit)){ rez += " " + unit; }
			if(!"".equals(city)){
				if(!"".equals(rez)){ rez += ", ";}
				rez += city; 
			}
			if(!"".equals(stateAbbr)){ 
				if(!"".equals(rez)){ rez += ", ";}
				rez += stateAbbr; 
			}
			if(!"".equals(zip)){ rez += " " + zip; }
			rez = rez.trim();
		}
		else{ //default format
			String st = toString (FORMAT_STREET_ONLY);
			String csz = toString (FORMAT_CITY_STATE_ZIP);
			rez += (!StringUtils.isStringBlank(st)) ? st + ", " :"";			
			rez += (!StringUtils.isStringBlank(csz)) ? csz + "" :"";			
		}
		return rez;
	}

	public boolean isEmptyAddress() {
		return ((miStNo.equals("0") || StringUtils.isStringBlank(miStNo))
			&& StringUtils.isStringBlank(miUnit)
			&& StringUtils.isStringBlank(msStDirection)
			&& StringUtils.isStringBlank(msStName)
			&& StringUtils.isStringBlank(msStSuffix));
	}
	/**
	 * Returns the no.
	 * @return int
	 */
	/*
	public int getNo(){
		return miStNo;
	}
	*/
	/**
	 * Returns the unit.
	 * @return int
	 */
	/*
	public String getUnit(){
		return miUnit;
	}
	*/
	/**
	 * Returns the city.
	 * @return String
	 */
	/*
	public String getCity(){
		return msCity;
	}
	*/

	/**
	 * Returns the direction.
	 * @return String
	 */
	/*
	public String getDirection(){
		return msStDirection;
	}
	*/

	/**
	 * Returns the name.
	 * @return String
	 */
	/*
	public String getName(){
		return msStName;
	}
	*/
	/**
	 * Returns the stateAbv.
	 * @return String
	 */
	/*
	public String getStateAbv(){
		return msStateAbbr;
	}
	*/
	/**
	 * Returns the street.
	 * @return String
	 */
	/*
	public String getStreet(){
		return msStSuffix;
	}
	*/
	/**
	 * Returns the zip.
	 * @return String
	 */
	/*
	public String getZip(){
		return msZip;
	}
	*/
	/**
	 * Sets the no.
	 * @param no The no to set
	 */
	/*
	public void setNo(int no){
		miStNo= no;
	}
	*/
	/**
	 * Sets the unit.
	 * @param unit The unit to set
	 */
	/*
	public void setUnit(String unit){
		miUnit= unit;
	}
	*/
	/**
	 * Sets the city.
	 * @param city The city to set
	 */
	/*
	public void setCity(String city){
		msCity= city;
	}
	*/
	/**
	 * Sets the direction.
	 * @param direction The direction to set
	 */
	/*
	public void setDirection(String direction){
		msStDirection= direction;
	}
	*/

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	/*
	public void setName(String name){
		msStName= name;
	}
	*/
	/**
	 * Sets the stateAbv.
	 * @param stateAbv The stateAbv to set
	 */
	/*
	public void setStateAbv(String stateAbv){
		msStateAbbr= stateAbv;
	}
    */
	/**
	 * Sets the street.
	 * @param street The street to set
	 */
	/*
	public void setStreet(String street){
		msStSuffix= street;
	}
	*/

	/**
	 * Sets the zip.
	 * @param zip The zip to set
	 */
	/*
	public void setZip(String zip)
	{
		msZip= zip;
	}
	*/

/*	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + miStNo;
		result = PRIME * result + ((miUnit == null) ? 0 : miUnit.hashCode());
		result = PRIME * result + ((msCity == null) ? 0 : msCity.hashCode());
		result = PRIME * result + ((msStDirection == null) ? 0 : msStDirection.hashCode());
		result = PRIME * result + ((msStName == null) ? 0 : msStName.hashCode());
		result = PRIME * result + ((msStSuffix == null) ? 0 : msStSuffix.hashCode());
		result = PRIME * result + ((msStateAbbr == null) ? 0 : msStateAbbr.hashCode());
		result = PRIME * result + ((msZip == null) ? 0 : msZip.hashCode());
		return result;
	}*/

//	@Override
	public boolean isEquals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Address other = (Address) obj;
		if (miStNo != other.miStNo)
			return false;
		if (miUnit == null) {
			if (other.miUnit != null)
				return false;
		} else if (!miUnit.equals(other.miUnit))
			return false;
		if (msCity == null) {
			if (other.msCity != null)
				return false;
		} else {			
			if (!other.msCity.matches(".*\\b(UNINCORPORATED|UNKNOWN)\\b.*") && !msCity.matches(".*\\b(UNINCORPORATED|UNKNOWN)\\b.*")
					&& !msCity.equals(other.msCity))
				return false;
		}
		if (msStDirection == null) {
			if (other.msStDirection != null)
				return false;
		} else if (!msStDirection.equals(other.msStDirection))
			return false;
		if (msStName == null) {
			if (other.msStName != null)
				return false;
		} else if (!msStName.equals(other.msStName))
			return false;
		if (msStSuffix == null) {
			if (other.msStSuffix != null)
				return false;
		} else if (!msStSuffix.equals(other.msStSuffix))
			return false;
		if (msStateAbbr == null) {
			if (other.msStateAbbr != null)
				return false;
		} else if (!msStateAbbr.equals(other.msStateAbbr))
			return false;
		if (msZip == null) {
			if (other.msZip != null)
				return false;
		} else if (!msZip.equals(other.msZip))
			return false;
		return true;
	}
}

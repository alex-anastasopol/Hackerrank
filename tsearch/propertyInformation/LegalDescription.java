package ro.cst.tsearch.propertyInformation;

public class LegalDescription 
{
	private String msInstrumentNo="";
	private String msParcelNo="";
	private String msSubdivisionName="";
	private int miBookNo=0;
	private int miPageNo=0;
	private int miLotNo=0;
	/**
	 * Returns the bookNo.
	 * @return int
	 */
	public int getBookNo(){
		return miBookNo;
	}

	/**
	 * Returns the lotNo.
	 * @return int
	 */
	public int getLotNo(){
		return miLotNo;
	}

	/**
	 * Returns the pageNo.
	 * @return int
	 */
	public int getPageNo(){
		return miPageNo;
	}

	/**
	 * Returns the instrumentNo.
	 * @return String
	 */
	public String getInstrumentNo(){
		return msInstrumentNo;
	}

	/**
	 * Returns the parcelNo.
	 * @return String
	 */
	public String getParcelNo(){
		return msParcelNo;
	}

	/**
	 * Returns the subdivisionName.
	 * @return String
	 */
	public String getSubdivisionName(){
		return msSubdivisionName;
	}

	/**
	 * Sets the bookNo.
	 * @param bookNo The bookNo to set
	 */
	public void setBookNo(int bookNo){
		miBookNo= bookNo;
	}

	/**
	 * Sets the lotNo.
	 * @param lotNo The lotNo to set
	 */
	public void setLotNo(int lotNo){
		miLotNo= lotNo;
	}

	/**
	 * Sets the pageNo.
	 * @param pageNo The pageNo to set
	 */
	public void setPageNo(int pageNo){
		miPageNo= pageNo;
	}

	/**
	 * Sets the instrumentNo.
	 * @param instrumentNo The instrumentNo to set
	 */
	public void setInstrumentNo(String instrumentNo){
		msInstrumentNo= instrumentNo;
	}

	/**
	 * Sets the parcelNo.
	 * @param parcelNo The parcelNo to set
	 */
	public void setParcelNo(String parcelNo){
		msParcelNo= parcelNo;
	}

	/**
	 * Sets the subdivisionName.
	 * @param subdivisionName The subdivisionName to set
	 */
	public void setSubdivisionName(String subdivisionName){
		msSubdivisionName= subdivisionName;
	}

}

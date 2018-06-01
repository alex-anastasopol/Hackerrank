package ro.cst.tsearch.propertyInformation;

import java.sql.Date;

public class TaxTransactionDetails 
{
	//////////////////////////////////////////////////////////////////////////////
	public static class TaxTypes
	{
		public static final TaxTypes PAID=new TaxTypes("Paid"),
										REFUNDED=new TaxTypes("Refunded");
		//////////////////////////////////////////////////////////////////////////
		private final String mName;
		//////////////////////////////////////////////////////////////////////////
		private TaxTypes(String value){mName=value;}
		//////////////////////////////////////////////////////////////////////////
		public String toString(){return mName;}
	}
	//////////////////////////////////////////////////////////////////////////////
	private String TransactionNo="";
	private Date mDate=null;
	private TaxTypes mType=TaxTypes.PAID;
	private double mdAmount=0;
	
	/**
	 * Returns the dAmount.
	 * @return double
	 */
	public double getDAmount(){
		return mdAmount;
	}

	/**
	 * Returns the date.
	 * @return Date
	 */
	public Date getDate(){
		return mDate;
	}

	/**
	 * Returns the type.
	 * @return TaxTypes
	 */
	public TaxTypes getType(){
		return mType;
	}

	/**
	 * Returns the transactionNo.
	 * @return String
	 */
	public String getTransactionNo(){
		return TransactionNo;
	}

	/**
	 * Sets the dAmount.
	 * @param dAmount The dAmount to set
	 */
	public void setDAmount(double dAmount){
		mdAmount= dAmount;
	}

	/**
	 * Sets the date.
	 * @param date The date to set
	 */
	public void setDate(Date date){
		mDate= date;
	}

	/**
	 * Sets the type.
	 * @param type The type to set
	 */
	public void setType(TaxTypes type){
		mType= type;
	}

	/**
	 * Sets the transactionNo.
	 * @param transactionNo The transactionNo to set
	 */
	public void setTransactionNo(String transactionNo){
		TransactionNo= transactionNo;
	}

}

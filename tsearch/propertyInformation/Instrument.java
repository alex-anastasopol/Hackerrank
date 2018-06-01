package ro.cst.tsearch.propertyInformation;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.stewart.ats.base.document.InstrumentI;

@Deprecated
public class Instrument {
	
	static final long serialVersionUID = 10000000;
	
	public final static int TYPE_DEFAULT 		= 0;	
	public final static int TYPE_INSTRUMENT_NO 	= 1;
	public final static int TYPE_BOOK_PAGE 		= 2;
	public final static int TYPE_PARCEL_ID 		= 3;	
	public final static int TYPE_BOOK_PAGE_TYPE = 4;
	public final static int TYPE_INSTRUMENT_REF_TYPE = 5;
	
	public final static int SALES_DATA					= 0;
	public final static int NO_ORIGIN_DEFINED			= 1;
	
	private int origin	= NO_ORIGIN_DEFINED;
	
	public static class DocTypes implements Serializable
	{
		static final long serialVersionUID = 10000000;
		@SuppressWarnings("unused")		// not actually used. remained here so that searches can be de-serialized
		@Deprecated
		private transient String mName;
	}
	
	private String realdoctype="";
	@SuppressWarnings("unused")		// not actually used. remained here so that searches can be de-serialized
	@Deprecated
	private transient Vector<String> mPIS = null; 
	@SuppressWarnings("unused")		// not actually used. remained here so that searches can be de-serialized
	@Deprecated
	private transient Vector<Instrument> mCrossReferences = null;
	@SuppressWarnings("unused")		// not actually used. remained here so that searches can be de-serialized
	@Deprecated
	private transient String msGrantor = null;
	@SuppressWarnings("unused")		// not actually used. remained here so that searches can be de-serialized
	@Deprecated
	private transient String msGrantee = null;
	@SuppressWarnings("unused")		// not actually used. remained here so that searches can be de-serialized
	@Deprecated
	private transient List<Owner> grantors = null;
	@SuppressWarnings("unused")		// not actually used. remained here so that searches can be de-serialized
	@Deprecated
	private transient List<Owner> grantees = null;

	private String msInstrumentNo="";
	private String bookNo 	= ""; 
	private String pageNo 	= "";
	private String parcelID = ""; 
	private String bookPageType = "";
	private String instrumentRefType = "";
	
	private HashMap<String, Object> extraInfoMap = new HashMap<String, Object>();
	
	/**
	 * @return Returns the bookPageType.
	 */
	public String getBookPageType() {
		return bookPageType;
	}
	/**
	 * @param bookPageType The bookPageType to set.
	 */
	public void setBookPageType(String bookPageType) {
		this.bookPageType = bookPageType;
	}
	/**
	 * @return Returns the instrumentRefType.
	 */
	public String getInstrumentRefType() {
		return instrumentRefType;
	}
	/**
	 * @param instrumentRefType The instrumentRefType to set.
	 */
	public void setInstrumentRefType(String instrumentRefType) {
		this.instrumentRefType = instrumentRefType;
	}
	private String extraInstrType = "";
	private int instrType	= TYPE_DEFAULT;
	private boolean overwrite = false;
	
	@SuppressWarnings("unused")		// not actually used. remained here so that searches can be de-serialized
	@Deprecated
	private transient DocTypes mDocType = null;
	private Date mFileDate=null,
	mInstrumentDate=null;
	private double mdTransValue=0,
	mdMortgageValue=0,
	mdTaxValue=0;
	
	/**
	 * Returns the dMortgageValue.
	 * @return double
	 */
	public double getDMortgageValue(){
		return mdMortgageValue;
	}
	
	/**
	 * Returns the dTransValue.
	 * @return double
	 */
	public double getDTransValue(){
		return mdTransValue;
	}
	
	/**
	 * Returns the fileDate.
	 * @return Date
	 */
	public Date getFileDate(){
		return mFileDate;
	}
	
	
	/**
	 * Returns the instrumentDate.
	 * @return Date
	 */
	public Date getInstrumentDate()
	{
		return mInstrumentDate;
	}
	
	
	/**
	 * Returns the instrumentNo.
	 * @return String
	 */
	public String getInstrumentNo(){
		return msInstrumentNo;
	}
	
	/**
	 * Returns the taxValue.
	 * @return double
	 */
	public double getTaxValue(){
		return mdTaxValue;
	}
	
	/**
	 * Sets the dMortgageValue.
	 * @param dMortgageValue The dMortgageValue to set
	 */
	public void setDMortgageValue(double dMortgageValue){
		mdMortgageValue= dMortgageValue;
	}
	
	/**
	 * Sets the dTransValue.
	 * @param dTransValue The dTransValue to set
	 */
	public void setDTransValue(double dTransValue){
		mdTransValue= dTransValue;
	}
	
	/**
	 * Sets the fileDate.
	 * @param fileDate The fileDate to set
	 */
	public void setFileDate(Date fileDate){
		mFileDate= fileDate;
	}
	
	
	/**
	 * Sets the instrumentDate.
	 * @param instrumentDate The instrumentDate to set
	 */
	public void setInstrumentDate(Date instrumentDate){
		mInstrumentDate= instrumentDate;
	}
	
	
	/**
	 * Sets the instrumentNo.
	 * @param instrumentNo The instrumentNo to set
	 */
	public void setInstrumentNo(String instrumentNo){
		msInstrumentNo= instrumentNo;
	}
	
	/**
	 * Sets the taxValue.
	 * @param taxValue The taxValue to set
	 */
	public void setTaxValue(double taxValue){
		mdTaxValue= taxValue;
	}
		
	public String toString(){
		return  "Instrument(" + msInstrumentNo +")[book=" + bookNo+";page="+pageNo+"]";
	}
	
	/**
	 * @return
	 */
	public String getBookNo() {
		return bookNo;
	}
	
	/**
	 * @return
	 */
	public String getPageNo() {
		return pageNo;
	}
	
	/**
	 * @param string
	 */
	public void setBookNo(String string) {
		bookNo = string;
	}
	
	/**
	 * @param string
	 */
	public void setPageNo(String string) {
		pageNo = string;
	}
	
	/**
	 * @return
	 */
	public String getParcelID() {
		return parcelID;
	}
	
	/**
	 * @param string
	 */
	public void setParcelID(String string) {
		parcelID = string;
	}
	
	/**
	 * @return
	 */
	public int getInstrumentType() {
		return instrType;
	}
	
	/**
	 * @param i
	 */
	public void setInstrumentType(int i) {
		instrType = i;
	}
	
	/**
	 * @return
	 */
	public boolean isOverwrite() {
		return overwrite;
	}
	
	/**
	 * @param b
	 */
	public void setOverwrite(boolean b) {
		overwrite = b;
	}
	
	/**
	 * @return
	 */
	public String getRealdoctype() {
		return realdoctype;
	}
	
	/**
	 * @param string
	 */
	public void setRealdoctype(String string) {
		realdoctype = string;
	}
	
	/**
	 * @return
	 */
	public String getExtraInstrType() {
		return extraInstrType;
	}
	
	/**
	 * @param string
	 */
	public void setExtraInstrType(String string) {
		extraInstrType = string;
	}
	
	public int getOrigin() {
		return origin;
	}
	
	public void setOrigin(int i) {
		origin = i;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj==null)
			return false;
		if (obj instanceof Instrument) {
			Instrument inst = (Instrument) obj;
			if(inst.getInstrumentType()!=this.getInstrumentType())
				return false;
			else {
				if(this.getInstrumentType() == TYPE_INSTRUMENT_NO){
					return this.getInstrumentNo().equals(inst.getInstrumentNo());
				} else if(this.getInstrumentType() == TYPE_BOOK_PAGE){
					return 
						this.getBookNo().equals(inst.getBookNo()) && 
						this.getPageNo().equals(inst.getPageNo()) &&
						this.getBookPageType().equals(inst.getBookPageType()) && 
						this.getInstrumentRefType().equals(inst.getInstrumentRefType());
				} else if(this.getInstrumentType() == TYPE_DEFAULT){
					return 
					this.getBookNo().equals(inst.getBookNo()) && 
					this.getPageNo().equals(inst.getPageNo()) &&
					this.getBookPageType().equals(inst.getBookPageType()) && 
					this.getInstrumentRefType().equals(inst.getInstrumentRefType()) &&
					this.getInstrumentNo().equals(inst.getInstrumentNo());
			}else
					return super.equals(inst);
			}
			
		} else
			return false;
	}
	
	public Object setExtraInfo(String name, Object value){
		return extraInfoMap.put(name, value);
	}
	public Object getExtraInfo(String name){
		if(extraInfoMap==null)
			return null;
		return extraInfoMap.get(name);
	}
	
	@Deprecated
	public InstrumentI toInstrumet(){
		InstrumentI i = new  com.stewart.ats.base.document.Instrument(msInstrumentNo);
		i.setBook(bookNo);
		i.setPage(pageNo);
		if(extraInfoMap.containsKey("Year")) {
			i.setYear((Integer)getExtraInfo("Year"));
		}
		return i;
	}
}

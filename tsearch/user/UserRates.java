package ro.cst.tsearch.user;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Hashtable;



public class UserRates{
	
	private BigDecimal ID ;
	private Date fromDate;
	private double ATS2CRATEINDEX;
	private double C2ARATEINDEX;
	
    private Hashtable<BigDecimal,BigDecimal> ats2CommRates = new Hashtable<BigDecimal,BigDecimal>();
    private Hashtable<BigDecimal,BigDecimal> comm2AgentRates = new Hashtable<BigDecimal,BigDecimal>();
    private Hashtable<BigDecimal, BigDecimal> payrateIdCounty = new Hashtable<BigDecimal, BigDecimal>();
    
	public UserRates(){
	}
	
    public void setIdCounty( BigDecimal payrateId, BigDecimal countyId )
    {
        payrateIdCounty.put( countyId, payrateId );
    }
    
    public BigDecimal getIdCounty( BigDecimal countyId )
    {
        BigDecimal payrateId = payrateIdCounty.get( countyId );
        if( payrateId == null )
        {
            return getID();
        }
        
        return payrateId;
    }
    
    public void addRate( int rateType, BigDecimal rateValue, BigDecimal countyId )
    {
        switch( rateType )
        {
        case UserAttributes.A2CRATE:
            ats2CommRates.put( countyId, rateValue );
            break;
        case UserAttributes.C2ARATE:
            comm2AgentRates.put( countyId, rateValue );
            break;
        }
    }

    public BigDecimal getRate( int rateType, BigDecimal countyId )
    {
        BigDecimal returnValue = null;
        
        switch( rateType )
        {
        case UserAttributes.A2CRATE:
            returnValue = ats2CommRates.get( countyId );
            break;
        case UserAttributes.C2ARATE:
            returnValue = comm2AgentRates.get( countyId );
            break;
        }
        
        if( returnValue == null )
        {
            returnValue = new BigDecimal(1);
        }
        
        return returnValue;
    }
    
	/**
	 * @return
	 */
	public double getATS2CRATEINDEX() {
		return ATS2CRATEINDEX;
	}

	/**
	 * @return
	 */
	public double getC2ARATEINDEX() {
		return C2ARATEINDEX;
	}

	/**
	 * @return
	 */
	public Date getFromDate() {
		return fromDate;
	}

	/**
	 * @return
	 */
	public BigDecimal getID() {
		return ID;
	}

	/**
	 * @param integer
	 */
	public void setATS2CRATEINDEX(double val) {
		ATS2CRATEINDEX = val;
	}

	/**
	 * @param integer
	 */
	public void setC2ARATEINDEX(double val) {
		C2ARATEINDEX = val;
	}

	/**
	 * @param date
	 */
	public void setFromDate(Date date) {
		fromDate = date;
	}

	/**
	 * @param decimal
	 */
	public void setID(BigDecimal decimal) {
		ID = decimal;
	}
	/* (non-Javadoc)
	 * @see ro.cst.tsearch.data.DataAttribute#getAttrCount()
	 */

}

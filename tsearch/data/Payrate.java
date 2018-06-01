package ro.cst.tsearch.data;

import static ro.cst.tsearch.utils.DBConstants.TABLE_PAYRATE;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.servers.types.CertificationDateManager;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.SearchLogger;

public class Payrate {
	
	public static final String FIELD_ID = "id";
	public static final String FIELD_START_DATE = "start_date";
	public static final String FIELD_END_DATE = "end_date";
	public static final String FIELD_COMMUNITY_ID = "community_id";
	public static final String FIELD_COUNTY_ID = "county_id";
	
	public static final String FIELD_FULL_SEARCH_A2C = "search_cost";
	public static final String FIELD_FULL_SEARCH_C2A = "search_value";
	public static final String FIELD_UPDATE_A2C = "update_cost";
	public static final String FIELD_UPDATE_C2A = "update_value";
	public static final String FIELD_CRTOWNER_A2C = "currentowner_cost";
	public static final String FIELD_CRTOWNER_C2A = "currentowner_value";
	public static final String FIELD_REFINANCE_A2C = "refinance_cost";
	public static final String FIELD_REFINANCE_C2A = "refinance_value";
	public static final String FIELD_CONSTRUCTION_A2C = "construction_cost";
	public static final String FIELD_CONSTRUCTION_C2A = "construction_value";
	public static final String FIELD_COMMERCIAL_A2C = "commercial_cost";
	public static final String FIELD_COMMERCIAL_C2A = "commercial_value";
	public static final String FIELD_OE_A2C = "oe_cost";
	public static final String FIELD_OE_C2A = "oe_value";
	public static final String FIELD_LIENS_A2C = "liens_cost";
	public static final String FIELD_LIENS_C2A = "liens_value";
	public static final String FIELD_ACREAGE_A2C = "acreage_cost";
	public static final String FIELD_ACREAGE_C2A = "acreage_value";
	public static final String FIELD_SUBLOT_A2C = "sublot_cost";
	public static final String FIELD_SUBLOT_C2A = "sublot_value";
	public static final String FIELD_INDEX_A2C = "index_a2c";
	public static final String FIELD_INDEX_C2A = "index_c2a";
	public static final String FIELD_FVS_A2C = "fvs_cost";
	public static final String FIELD_FVS_C2A = "fvs_value";

	private long id = 0;
	private Date dueDate = Util.getDefaultDueDate();
	private Date cityDueDate = Util.getDefaultDueDate();// !!!!!
	 
	private Date startDate = new Date();
	private Date endDate = new Date();
	private double searchValue = PayrateConstants.SEARCH_VALUE;
	private double updateValue = PayrateConstants.UPDATE_VALUE;
	private double currentOwnerValue = PayrateConstants.CURRENTOWNER_VALUE;
	private double refinanceValue = PayrateConstants.REFINANCE_VALUE;
	private double constructionValue   = PayrateConstants.CONSTRUCTION_VALUE;
	private double commercialValue	   = PayrateConstants.COMMERCIAL_VALUE;
	private double oeValue				   = PayrateConstants.OE_VALUE;
	private double liensValue			   = PayrateConstants.LIENS_VALUE;
	private double acreageValue		   = PayrateConstants.ACREAGE_VALUE;
	private double sublotValue			   = PayrateConstants.SUBLOT_VALUE;
	private double indexC2A				= PayrateConstants.INDEX_C2AGENT;
	private double fvsValue				= PayrateConstants.FVS_VALUE;
	
	
	
	private double searchCost = PayrateConstants.SEARCH_COST;
	private double updateCost = PayrateConstants.UPDATE_COST;
	private double currentOwnerCost = PayrateConstants.CURRENTOWNER_COST;
	private double refinanceCost = PayrateConstants.REFINANCE_COST;
	
	private double constructionCost   	= PayrateConstants.CONSTRUCTION_COST;
	private double commercialCost	    = PayrateConstants.COMMERCIAL_COST;
	private double oeCost				   		= PayrateConstants.OE_COST;
	private double liensCost			   		= PayrateConstants.LIENS_COST;
	private double acreageCost		   		= PayrateConstants.ACREAGE_COST;
	private double sublotCost			    = PayrateConstants.SUBLOT_COST;
	private double indexA2C					= PayrateConstants.INDEX_A2C;
	private double fvsCost					= PayrateConstants.FVS_COST;
	
	private long commId = Long.MIN_VALUE;
	private long countyId = Long.MIN_VALUE;
	private long cityId = Long.MIN_VALUE;
	private String countyName = "";
	private String stateAbv = "";
	private String cityName = "";

	private String certificationDateOffset = CertificationDateManager.DEFAULT_CERTIFICATION_DATE_OFFSET+"";
	private int officialStartDateOffset = SearchAttributes.YEARS_BACK;
	
	public long getCommId() {
		return commId;
	}

	public Date getEndDate() {
		return endDate;
	}

	public long getId() {
		return id;
	}

	public double getCurrentOwnerValue()
	{
	    return currentOwnerValue;
	}
	public String getFormattedCownerValue()
	{
	    return ATSDecimalNumberFormat.format( new BigDecimal(currentOwnerValue) );
	}
	
	public double getRefinanceValue()
	{
	    return refinanceValue;
	}
	public String getFormattedRefinanceValue()
	{
	    return ATSDecimalNumberFormat.format( new BigDecimal(refinanceValue) );
	}
	
	public double getCurrentOwnerCost()
	{
	    return currentOwnerCost;
	}
	public String getFormattedCownerCost()
	{
	    return ATSDecimalNumberFormat.format( new BigDecimal(currentOwnerCost) );
	}
	
	public double getRefinanceCost()
	{
	    return refinanceCost;
	}
	public String getFormattedRefinanceCost()
	{
	    return ATSDecimalNumberFormat.format( new BigDecimal(refinanceCost) );
	}
	
	public double getSearchCost() {
		return searchCost;
	}
	public String getFormattedSearchCost() {
		return ATSDecimalNumberFormat.format(new BigDecimal(searchCost));
	}

	public double getSearchValue() {
		return searchValue;
	}
	public String getFormattedSearchValue() {
		return ATSDecimalNumberFormat.format(new BigDecimal(searchValue));
	}

	public Date getStartDate() {
		return startDate;
	}
	public String getFormattedStartDate() {
		return new SimpleDateFormat("MMM dd, yyyy").format(startDate);
	}

	public double getUpdateCost() {
		return updateCost;
	}
	public String getFormattedUpdateCost() {
		return ATSDecimalNumberFormat.format(new BigDecimal(updateCost));
	}

	public double getUpdateValue() {
		return updateValue;
	}
	public String getFormattedUpdateValue() {
		return ATSDecimalNumberFormat.format(new BigDecimal(updateValue));
	}
	
	
	
	public double getConstructionValue(){
		return constructionValue;
	}	
	public String getFormattedConstructionValue (){
		return ATSDecimalNumberFormat.format(new BigDecimal(constructionValue));
	}
	
	public double getConstructionCost(){
		return constructionCost;
	}		
	public String getFormattedConstructionCost(){
		return ATSDecimalNumberFormat.format(new BigDecimal(constructionCost));
	}
	
	
    public double getCommercialValue(){
    	return commercialValue;    	
    }	 
    public String getFormattedCommercialValue(){
    	return ATSDecimalNumberFormat.format(new BigDecimal(commercialValue));
    }
    
    public double getCommercialCost(){
    	return commercialCost;    	
    }	
    public String getFormattedCommercialCost(){
    	return ATSDecimalNumberFormat.format(new BigDecimal(commercialCost));
    }    
    
    public double getOEValue(){
    	return oeValue;
    }
   public String getFormattedOEValue(){
	   return ATSDecimalNumberFormat.format(new BigDecimal(oeValue));
   }
   
   public double getOECost(){
	   return oeCost;
   }
   public String getFormattedOECost(){
	   return ATSDecimalNumberFormat.format(new BigDecimal(oeCost));
   }   
   
	
   public double getLiensValue(){
	   return liensValue;
   }	
   public String getFormattedLiensValue(){
	   return ATSDecimalNumberFormat.format(new BigDecimal(liensValue));
   }
   
   public double getLiensCost(){
	   return liensCost;
   }
   public String getFormattedLiensCost(){
	   return ATSDecimalNumberFormat.format(new BigDecimal(liensCost));
   }      
   
   public double getAcreageValue(){
	   return acreageValue;
   }   
   public String getFormattedAcreageValue(){
	   return ATSDecimalNumberFormat.format(new BigDecimal(acreageValue));
   }
	
   public double getAcreageCost(){
	   return acreageCost;
   }
   public String getFormattedAcreageCost(){
	   return ATSDecimalNumberFormat.format(new BigDecimal(acreageCost));
   }   
   
   public double getSublotValue(){
	   return sublotValue;
   }
   public String getFormattedSublotValue(){
	   return ATSDecimalNumberFormat.format(new BigDecimal(sublotValue));
   }

   public double getSublotCost(){
	   return sublotCost;
   }
   public String getFormattedSublotCost(){
	   return ATSDecimalNumberFormat.format(new BigDecimal(sublotCost));
   }   
   
   
	public void setCommId(long l) {
		commId = l;
	}

	public void setEndDate(Date date) {
		endDate = date;
	}

	public void setId(long l) {
		id = l;
	}

	public void setSearchCost(double d) {
		searchCost = d;
	}

	public void setSearchValue(double d) {
		searchValue = d;
	}

	public void setStartDate(Date date) {
		startDate = date;
	}

	public void setUpdateCost(double d) {
		updateCost = d;
	}

	public void setUpdateValue(double d) {
		updateValue = d;
	}
	
	public void setCurrentOwnerValue( double d )
	{
	    currentOwnerValue = d;
	}
	
	public void setCurrentOwnerCost( double d )
	{
	    currentOwnerCost = d;
	}
	
	public void setRefinanceValue( double d )
	{
	    refinanceValue = d;
	}
	
	public void setRefinanceCost( double d )
	{
	    refinanceCost = d;
	}
	
	public void setConstructionValue(double d){
		constructionValue = d;
	}	
	public void setConstructionCost(double d){
		constructionCost = d;		
	}
	
	public void setCommercialValue(double d){
		commercialValue = d;
	}
	public void setCommercialCost(double d){
		commercialCost = d;
	}
	
	public void setOEValue(double d){
		oeValue = d;
	}
	public void setOECost(double d){
		oeCost = d;
	}
	
	public void setLiensValue(double d){
		liensValue = d;
	}
	public void setLiensCost(double d){
		liensCost = d;
	}
	
	public void setAcreageValue(double d){
		acreageValue = d;
	}
   public void setAcreageCost(double d){
	   acreageCost = d;
   }
   
   public void setSublotValue(double d){
	   sublotValue = d;
   }
   public void setSublotCost(double d){
	   sublotCost = d;
   }
   
	public long getCountyId() {
		return countyId;
	}

	public void setCityID(long l) {
		cityId = l;
	}
	public long getCityID() {
			return cityId;
		}

		public void setCountyId(long l) {
			countyId = l;
		}
	public String getCountyName() {
		return countyName;
	}

	public String getStateAbv() {
		return stateAbv;
	}

	public void setCountyName(String string) {
		countyName = string;
	}
	
	public String getCityName() {
			return cityName;
		}

	public void setCityName(String string) {
		cityName = string;
	}
	
	public void setStateAbv(String string) {
		stateAbv = string;
	}

	public String getCountyFullName() {
		return countyName + " " + stateAbv;
	}
	public Date getDueDate() {
		return dueDate;
	}
	public String getFormattedDueDate() {
		return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(dueDate);
	}
	public String getFormattedCityDueDate() {
		return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(cityDueDate);
	}
	public void setDueDate(Date date) {
		dueDate = date;
	}
	public void setCityDueDate(Date date) {
			cityDueDate = date;
		}
	public Date getCityDueDate() {
				return cityDueDate;
			}
	
	public void setCityDue(Date date) {
				cityDueDate = date;
			}
		public String getCityDue() {
					return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(cityDueDate);
				}
	
	public void setCertificationDateOffset(String offset) {
		this.certificationDateOffset = offset;
	}
	
	public String getCertificationDateOffset() {
			try {
				return Integer.parseInt(this.certificationDateOffset)+"";
			}catch(NumberFormatException nfe) {
				return "";
			}
	}
	
	public int getOfficialStartDateOffset() {
		return officialStartDateOffset;
	}

	public void setOfficialStartDateOffset(int officialStartDateOffset) {
		this.officialStartDateOffset = officialStartDateOffset;
	}

	/**
	 * @return the indexC2A
	 */
	public double getIndexC2A() {
		return indexC2A;
	}
	
	public String getFormattedIndexC2A(){
		return ATSDecimalNumberFormat.format(new BigDecimal(indexC2A));
	}

	/**
	 * @param indexC2A the indexC2A to set
	 */
	public void setIndexC2A(double indexC2A) {
		this.indexC2A = indexC2A;
	}

	/**
	 * @return the indexA2C
	 */
	public double getIndexA2C() {
		return indexA2C;
	}
	
	public String getFormattedIndexA2C(){
		return ATSDecimalNumberFormat.format(new BigDecimal(indexA2C));
	}

	/**
	 * @param indexA2C the indexA2C to set
	 */
	public void setIndexA2C(double indexA2C) {
		this.indexA2C = indexA2C;
	}

	public double getFvsCost() {
		return fvsCost;
	}

	public void setFvsCost(double fvsCost) {
		this.fvsCost = fvsCost;
	}

	public String getFormattedFVSCost(){
		return ATSDecimalNumberFormat.format(new BigDecimal(fvsCost));
	}
	
	public double getFvsValue() {
		return fvsValue;
	}

	public void setFvsValue(double fvsValue) {
		this.fvsValue = fvsValue;
	}
	
	public String getFormattedFVSValue(){
		return ATSDecimalNumberFormat.format(new BigDecimal(fvsValue));
	}
	
	public double getProductValue(int product){
		switch(product){
		case 1: //case Products.FULL_SEARCH_PRODUCT: // "Sale";
			return searchValue;
		case 2: //case Products.CURRENT_OWNER_PRODUCT: // "Foreclosure";
			return currentOwnerValue;
		case 3: //case Products.CONSTRUCTION_PRODUCT: // "Construction";
			return constructionValue;
		case 4: //case Products.COMMERCIAL_PRODUCT: // "Commercial";
			return commercialValue;	
		case 5: //case Products.REFINANCE_PRODUCT: // "Refinance";
			return refinanceValue;
		case 6: //Products.OE_PRODUCT: // "HELOC";
			return oeValue;			
		case 7: //Products.LIENS_PRODUCT: // "Liens";
			return liensValue;
		case 8: //Products.ACREAGE_PRODUCT: // "Acreage";
			return acreageValue;		
		case 9: //Products.SUBLOT_PRODUCT: // "MISC";
			return sublotValue;	
		case 10: //Products.UPDATE_PRODUCT: // "Update";
			return updateValue;
		case 12: //Products.FVS_PRODUCT: // "FVS";
			return fvsValue;
		default:
			return searchValue; 
		}
	}
	
	public String getPayrateContent(boolean ats2community){
		StringBuilder strP= new StringBuilder(
				"\nStartDate:\t"+SearchLogger.getCurDateTimeCST()+"\n");
		
		if(ats2community) {
			strP.append("SearchCost:\t"+this.getSearchCost()+"\n");
			strP.append("UpdateCost:\t"+this.getUpdateCost()+"\n");
			strP.append("CurrentOwnerCost:\t"+this.getCurrentOwnerCost()+"\n");
			strP.append("RefinanceCost:\t"+this.getRefinanceCost()+"\n");
			strP.append("ConstructionCost:\t"+this.getConstructionCost()+"\n");
			strP.append("CommercialCost:\t"+this.getCommercialCost()+"\n");
			strP.append("OECost:\t"+this.getOECost()+"\n");
			strP.append("LiensCost:\t"+this.getLiensCost()+"\n");
			strP.append("AcreageCost:\t"+this.getAcreageCost()+"\n");
			strP.append("SublotCost:\t"+this.getSublotCost()+"\n");
			strP.append("IndexCost:\t"+this.getIndexA2C()+"\n");
			strP.append("FVSCost:\t" + this.getFvsCost() + "\n");
		} else {
			strP.append("SearchValue:\t"+this.getSearchValue()+"\n");
			strP.append("UpdateValue:\t"+this.getUpdateValue()+"\n");
			strP.append("CurrentOwnerValue:\t"+this.getCurrentOwnerValue()+"\n");
			strP.append("RefinanceValue:\t"+this.getRefinanceValue()+"\n");
			strP.append("ConstructionValue:\t"+this.getConstructionValue()+"\n");
			strP.append("CommercialValue:\t"+this.getCommercialValue()+"\n");
			strP.append("OEValue:\t"+this.getOEValue()+"\n");
			strP.append("LiensValue:\t"+this.getLiensValue()+"\n");
			strP.append("AcreageValue:\t"+this.getAcreageValue()+"\n");
			strP.append("SublotValue:\t"+this.getSublotValue()+"\n");
			strP.append("IndexValue:\t"+this.getIndexC2A()+"\n");
			strP.append("FVSValue:\t" + this.getFvsValue() + "\n");
		}
		
		
		strP.append("\n");
		
		return strP.toString();
	}
	public static Map<Integer, Payrate> getLatestPayratesForCommunity(int commId) {
		List<Integer> all = new ArrayList<Integer>();
		return getLatestPayratesForCommunity(commId,all,all);
	}
	public static Map<Integer, Payrate> getLatestPayratesForCommunity(int commId,Collection<Integer> states, Collection<Integer> counties) {
		
		String countyFilter = "";
		if(counties.size()>0 && !counties.contains(-1)) {
			countyFilter = " AND a.county_id IN(";
			for(Integer county : counties ) {
				countyFilter += ", " + county;
			}
			countyFilter = countyFilter.replaceFirst(",","");
			countyFilter += ") ";
		}
		else if(states.size()>0 && !states.contains(-1)) {
			String statesList = "";
			for(Integer state: states) {
				statesList += ", " + state;
			}
			statesList = statesList.replaceFirst(",","");
			countyFilter = " AND a.county_id IN( SELECT id FROM ts_county WHERE state_id IN ("+statesList+") ) ";
		}
		
		String sql = 
			" select " +
			" a.id, a.start_date, a.end_date, " + 
			"'', " +
			"'', " + 
			"a.community_id, a.county_id, " +
			"a.search_value, a.search_cost, a.update_value, a.update_cost, " + 
			"a.currentowner_value, a.currentowner_cost, a.refinance_value, a.refinance_cost, " + 
			"a.construction_value, a.construction_cost, a.commercial_value, a.commercial_cost, " +
			"a.oe_value, a.oe_cost, a.liens_value, a.liens_cost, " +
			"a.acreage_value, a.acreage_cost, a.sublot_value, a.sublot_cost, a.fvs_value, a.fvs_cost " +
		"from "+ TABLE_PAYRATE +" a " + 
		"where community_id = " + commId +
		countyFilter +
		" order by start_date desc, id desc";
            
	    DBConnection conn = null;
	    DatabaseData data;
	    Map<Integer,Payrate> fromPayrates = new HashMap<Integer, Payrate>();
	    HashMap<Long, Boolean> fromFound = null;
	    try {   
	        conn = ConnectionPool.getInstance().requestConnection();
	        data = conn.executeSQL(sql);;
	        fromFound = new HashMap<Long, Boolean>();
	        
	        for (int i = 0; i < data.getRowNumber(); i++) {
	        	Long countyId = (Long)data.getValue(7,i);
	        	if(countyId!=null && fromFound.get(countyId)==null){
					Payrate payr = new Payrate();
	            	payr.setId(Long.parseLong(data.getValue(1,i).toString()));
	            	payr.setCommId(((Float)data.getValue(6,i)).longValue());
	            	payr.setCountyId(countyId);
	                payr.setSearchValue(((Float)data.getValue(8,i)).doubleValue());
	                payr.setSearchCost(((Float)data.getValue(9,i)).doubleValue());	                
	                payr.setUpdateValue(((Float)data.getValue(10,i)).doubleValue());	                	                
	                payr.setUpdateCost(((Float)data.getValue(11,i)).doubleValue());
	                	                
	                payr.setCurrentOwnerValue( ((Float)data.getValue(12,i)).doubleValue() );
	                payr.setCurrentOwnerCost( ((Float)data.getValue(13,i)).doubleValue() );
	                payr.setRefinanceValue( ((Float)data.getValue(14,i)).doubleValue() );
	                payr.setRefinanceCost( ((Float)data.getValue(15,i)).doubleValue() );
	                payr.setConstructionValue( ((Float)data.getValue(16,i)).doubleValue() );
	                payr.setConstructionCost( ((Float)data.getValue(17,i)).doubleValue() );
	                payr.setCommercialValue( ((Float)data.getValue(18,i)).doubleValue() );
	                payr.setCommercialCost( ((Float)data.getValue(19,i)).doubleValue() );	
	                payr.setOEValue( ((Float)data.getValue(20,i)).doubleValue() );
	                payr.setOECost( ((Float)data.getValue(21,i)).doubleValue() );	 
	                payr.setLiensValue( ((Float)data.getValue(22,i)).doubleValue() );
	                payr.setLiensCost( ((Float)data.getValue(23,i)).doubleValue() );	
	                payr.setAcreageValue( ((Float)data.getValue(24,i)).doubleValue() );
	                payr.setAcreageCost( ((Float)data.getValue(25,i)).doubleValue() );	  
	                payr.setSublotValue( ((Float)data.getValue(26,i)).doubleValue() );
	                payr.setSublotCost( ((Float)data.getValue(27,i)).doubleValue() );	 
	                payr.setFvsValue(((Float) data.getValue(Payrate.FIELD_FVS_C2A, i)));
	                payr.setFvsCost(((Float) data.getValue(Payrate.FIELD_FVS_A2C, i)));
	                
	                fromFound.put(countyId, true);
	                fromPayrates.put(countyId.intValue(), payr);
	        	}
			}
	    } catch (Exception e) {
	    	e.printStackTrace();
	    } finally {
	        if (conn != null) {
	            try {
	                ConnectionPool.getInstance().releaseConnection(conn);
	            } catch (Exception e) {}
	        }
	    }
		return fromPayrates;
	}
	
}

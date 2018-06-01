package ro.cst.tsearch.data;

import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.servers.parentsite.CountyWithState;
import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.utils.DBConstants;

import com.stewart.ats.user.UserI;
import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;


public class RateCSVLine implements ParameterizedRowMapper<RateCSVLine>{
	private long userId;
	private String operatingAccountId;
	private String login;
	private String firstName;
	private String lastName;
	private double userRateIndex;
	private double salePrice;
	private double currentOwnerPrice;
	private double constructionPrice;
	private double commercialPrice;
	private double refinancePrice;
	private double oePrice;
	private double lienPrice;
	private double acreagePrice;
	private double sublotPrice;
	private double updatePrice;
	private double fvsPrice;
	private String countyName;
	private int countyId;
	private String stateAbrev;
	private int stateId;
	
	
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public String getOperatingAccountId() {
		return operatingAccountId;
	}
	public void setOperatingAccountId(String operatingAccountId) {
		this.operatingAccountId = operatingAccountId;
	}
	public String getLogin() {
		return login;
	}
	public void setLogin(String login) {
		this.login = login;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public double getUserRateIndex() {
		return userRateIndex;
	}
	public void setUserRateIndex(double userRateIndex) {
		this.userRateIndex = userRateIndex;
	}
	public double getSalePrice() {
		return salePrice;
	}
	public void setSalePrice(double salePrice) {
		this.salePrice = salePrice;
	}
	public double getCurrentOwnerPrice() {
		return currentOwnerPrice;
	}
	public void setCurrentOwnerPrice(double currentOwnerPrice) {
		this.currentOwnerPrice = currentOwnerPrice;
	}
	public double getConstructionPrice() {
		return constructionPrice;
	}
	public void setConstructionPrice(double constructionPrice) {
		this.constructionPrice = constructionPrice;
	}
	public double getCommercialPrice() {
		return commercialPrice;
	}
	public void setCommercialPrice(double commercialPrice) {
		this.commercialPrice = commercialPrice;
	}
	public double getRefinancePrice() {
		return refinancePrice;
	}
	public void setRefinancePrice(double refinancePrice) {
		this.refinancePrice = refinancePrice;
	}
	public double getOePrice() {
		return oePrice;
	}
	public void setOePrice(double oePrice) {
		this.oePrice = oePrice;
	}
	public double getLienPrice() {
		return lienPrice;
	}
	public void setLienPrice(double lienPrice) {
		this.lienPrice = lienPrice;
	}
	public double getAcreagePrice() {
		return acreagePrice;
	}
	public void setAcreagePrice(double acreagePrice) {
		this.acreagePrice = acreagePrice;
	}
	public double getSublotPrice() {
		return sublotPrice;
	}
	public void setSublotPrice(double sublotPrice) {
		this.sublotPrice = sublotPrice;
	}
	public double getUpdatePrice() {
		return updatePrice;
	}
	public void setUpdatePrice(double updatePrice) {
		this.updatePrice = updatePrice;
	}
	public double getFvsPrice() {
		return fvsPrice;
	}
	public void setFvsPrice(double fvsPrice) {
		this.fvsPrice = fvsPrice;
	}
	public String getCountyName() {
		return countyName;
	}
	public void setCountyName(String countyName) {
		this.countyName = countyName;
	}
	public int getCountyId() {
		return countyId;
	}
	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}
	public String getStateAbrev() {
		return stateAbrev;
	}
	public void setStateAbrev(String stateAbrev) {
		this.stateAbrev = stateAbrev;
	}
	public int getStateId() {
		return stateId;
	}
	public void setStateId(int stateId) {
		this.stateId = stateId;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(acreagePrice);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(commercialPrice);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(constructionPrice);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(currentOwnerPrice);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lienPrice);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(oePrice);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(refinancePrice);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(salePrice);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + stateId;
		temp = Double.doubleToLongBits(sublotPrice);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(updatePrice);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int) (userId ^ (userId >>> 32));
		temp = Double.doubleToLongBits(userRateIndex);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RateCSVLine other = (RateCSVLine) obj;
		if (Double.doubleToLongBits(acreagePrice) != Double
				.doubleToLongBits(other.acreagePrice))
			return false;
		if (Double.doubleToLongBits(commercialPrice) != Double
				.doubleToLongBits(other.commercialPrice))
			return false;
		if (Double.doubleToLongBits(constructionPrice) != Double
				.doubleToLongBits(other.constructionPrice))
			return false;
		if (Double.doubleToLongBits(currentOwnerPrice) != Double
				.doubleToLongBits(other.currentOwnerPrice))
			return false;
		if (Double.doubleToLongBits(lienPrice) != Double
				.doubleToLongBits(other.lienPrice))
			return false;
		if (Double.doubleToLongBits(oePrice) != Double
				.doubleToLongBits(other.oePrice))
			return false;
		if (Double.doubleToLongBits(refinancePrice) != Double
				.doubleToLongBits(other.refinancePrice))
			return false;
		if (Double.doubleToLongBits(salePrice) != Double
				.doubleToLongBits(other.salePrice))
			return false;
		if (stateId != other.stateId)
			return false;
		if (Double.doubleToLongBits(sublotPrice) != Double
				.doubleToLongBits(other.sublotPrice))
			return false;
		if (Double.doubleToLongBits(updatePrice) != Double
				.doubleToLongBits(other.updatePrice))
			return false;
		if (userId != other.userId)
			return false;
		if (Double.doubleToLongBits(userRateIndex) != Double
				.doubleToLongBits(other.userRateIndex))
			return false;
		return true;
	}
	@Override
	public RateCSVLine mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		RateCSVLine line = new RateCSVLine();
		line.setUserId(resultSet.getLong(DBConstants.FIELD_USER_ID));
		line.setLogin(resultSet.getString(DBConstants.FIELD_USER_LOGIN));
		line.setLastName(resultSet.getString(DBConstants.FIELD_USER_LAST_NAME));
		line.setFirstName(resultSet.getString(DBConstants.FIELD_USER_FIRST_NAME));
		line.setUserRateIndex(resultSet.getDouble("userRateIndex"));
//		line.setAcreagePrice(resultSet.getDouble("acreagePrice"));
//		line.setCommercialPrice(resultSet.getDouble("commercialPrice"));
//		line.setConstructionPrice(resultSet.getDouble("constructionPrice"));
//		line.setCurrentOwnerPrice(resultSet.getDouble("currentOwnerPrice"));
//		line.setLienPrice(resultSet.getDouble("lienPrice"));
//		line.setOePrice(resultSet.getDouble("oePrice"));
//		line.setRefinancePrice(resultSet.getDouble("refinancePrice"));
//		line.setSalePrice(resultSet.getDouble("salePrice"));
//		line.setSublotPrice(resultSet.getDouble("sublotPrice"));
//		line.setUpdatePrice(resultSet.getDouble("updatePrice"));
		line.setCountyName(resultSet.getString(DBConstants.FIELD_COUNTY_NAME));
		line.setCountyId(resultSet.getInt(DBConstants.FIELD_COUNTY_ID));
		line.setStateAbrev(resultSet.getString(DBConstants.FIELD_STATE_ABV));
		line.setStateId(resultSet.getInt(DBConstants.FIELD_COUNTY_STATE_ID));
		return line;
	}
	
	public static List<RateCSVLine> loadInfoFor(int[] stateIdArray, int commId, boolean isTSAdmin, int[] countyIdArray, int[] userIds){
			
		boolean allStates = false;
		boolean allCounties = false;
		String stateSelect = " where ( ";
		String countySelect = "";
		HashSet<Integer> countiesAsSet = new HashSet<Integer>();
		for( int j = 0 ; j < stateIdArray.length ; j ++ )
        {
        	if(stateIdArray[j] == -2) {
        		return new ArrayList<RateCSVLine>();
        	}
            if( stateIdArray[j] <= 0 ) {
                allStates = true;
                break;
            }
            stateSelect += " c.STATE_ID = " + stateIdArray[j];
            if( j < stateIdArray.length - 1 ) {
                stateSelect += " or ";
            }
        }
		if(!allStates) {
            stateSelect += " ) ";
        } else {
            stateSelect = " ";
        }
		
		for( int j = 0 ; j < countyIdArray.length ; j ++ )
        {
        	if(countyIdArray[j] == -2) {
        		return new ArrayList<RateCSVLine>();
        	}
            if( countyIdArray[j] <= 0 ) {
                allCounties = true;
                break;
            }
            countiesAsSet.add(countyIdArray[j]);
            countySelect += " c.ID = " + countyIdArray[j];
            if( j < countyIdArray.length - 1 ) {
            	countySelect += " or ";
            }
        }
		
		if(allCounties) {
        	countySelect = " ";
        }
		
		
		List<Long> validUserIds = null;  
		HashSet<Long> validUsersAsSet = new HashSet<Long>();
		
        UserManagerI userManagerI = UserManager.getInstance();
        try {
			userManagerI.getAccess();
				validUserIds = userManagerI.validateUsersForAssignModule(userIds, commId, 
						GroupAttributes.ABS_ID,
						GroupAttributes.CA_ID,
						GroupAttributes.CCA_ID,
						GroupAttributes.TA_ID,
						GroupAttributes.AG_ID);

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			userManagerI.releaseAccess();
		}
		for (Long long1 : validUserIds) {
			validUsersAsSet.add(long1);
		}
		String usersAsString = null;
		if(validUserIds != null) {
			if(validUserIds.size() == 0) {
				usersAsString = "-2121";
			} else {
				usersAsString = org.apache.commons.lang.StringUtils.join(validUserIds.toArray(),",");
			}
		}
		
		
		Map<Integer, Payrate> payrates = Payrate.getLatestPayratesForCommunity(commId);
		
		String sqlRates = "select rate.user_id, rate.county_id, ifnull(rate.ats2crateindex,1) a2cRate, ifnull(rate.c2arateindex,1) c2aRate  "
				+ "from ts_user_rating rate  "
				+ "join (select max(ur.id) rateId, ur.user_id, ur.county_id "
				+ "from ts_user_rating ur "
				+ "where ur.user_id in (select user_id from ts_user where comm_id = " + commId + ") "
				+ "and ur.county_id in (select id from " + DBConstants.TABLE_COUNTY + " c " + stateSelect + (!allCounties?(allStates?" where ":" and ") + countySelect:"") + ") "
				+ "and ur.user_id in (" + usersAsString + ") "
				+ "group by user_id, county_id) subsel1 on rate.id = subsel1.rateId ";
		
		List<RateCSVLine> result = new ArrayList<RateCSVLine>();
    	LinkedHashMap<RateCSVLine, String> foundMap = new LinkedHashMap<RateCSVLine, String>();

    	List<UserI> allUsersInThisCommunity = new ArrayList<UserI>();
    	try {
    		userManagerI.getAccess();
    		allUsersInThisCommunity = userManagerI.getUsersByCommunity(commId);
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			userManagerI.releaseAccess();
		}
    	List<CountyWithState> allCountiesSelected = DBManager.getAllCountiesForState(stateIdArray);
    	
		try {
        	SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
        	
        	List<UserCountyRate> allDatabaseUserRatesPerCounty = sjt.query(sqlRates, new UserCountyRate());
        	HashMap<UserCounty, UserCountyRate> hashWithAllInfoForUserRateId = new HashMap<UserCounty, UserCountyRate>();
        	for (UserCountyRate userCountyRate : allDatabaseUserRatesPerCounty) {
				hashWithAllInfoForUserRateId.put(userCountyRate.getUserCounty(), userCountyRate);
			}
        	
        	
        	
        	Collections.sort(allUsersInThisCommunity);
        	for (UserI userI : allUsersInThisCommunity) {
        		if(validUsersAsSet.contains(userI.getUserId())) {
					for (CountyWithState countyWithState : allCountiesSelected) {
						if(allCounties || countiesAsSet.contains(countyWithState.getCountyId())) {
							RateCSVLine rateCSVLine = new RateCSVLine();
							rateCSVLine.setUserId(userI.getUserId());
							rateCSVLine.setOperatingAccountId(userI.getOperatingAccountId());
							rateCSVLine.setLastName(userI.getLastName());
							rateCSVLine.setFirstName(userI.getFirstName());
							rateCSVLine.setLogin(userI.getUserName());
							rateCSVLine.setCountyId(countyWithState.getCountyId());
							rateCSVLine.setCountyName(countyWithState.getCountyName());
							rateCSVLine.setStateId(countyWithState.getStateId());
							rateCSVLine.setStateAbrev(countyWithState.getStateAbrv());
							
							//start getting userRate
							UserCounty uc = new UserCounty();
							uc.setCountyId(countyWithState.getCountyId());
							uc.setUserId(userI.getUserId());
							UserCountyRate userCountyRate = hashWithAllInfoForUserRateId.get(uc);
							double a2cRate = 1;
							double c2aRate = 1;
							
							if(userCountyRate == null) {
								rateCSVLine.setUserRateIndex(1);
							} else {
								if(isTSAdmin) {
									rateCSVLine.setUserRateIndex(a2cRate = userCountyRate.getA2cRate());
								} else {
									rateCSVLine.setUserRateIndex(c2aRate = userCountyRate.getC2aRate());
								}
							}
							//finished setting userRate
							
							//startSetting county payrate
							Payrate payrate = payrates.get(rateCSVLine.getCountyId());
							if(payrate == null) {
								payrate = new Payrate();
							}
							if(isTSAdmin) {
								rateCSVLine.setAcreagePrice(payrate.getAcreageCost()*a2cRate);
								rateCSVLine.setCommercialPrice(payrate.getCommercialCost()*a2cRate);
								rateCSVLine.setConstructionPrice(payrate.getConstructionCost()*a2cRate);
								rateCSVLine.setCurrentOwnerPrice(payrate.getCurrentOwnerCost()*a2cRate);
								rateCSVLine.setLienPrice(payrate.getLiensCost()*a2cRate);
								rateCSVLine.setOePrice(payrate.getOECost()*a2cRate);
								rateCSVLine.setRefinancePrice(payrate.getRefinanceCost()*a2cRate);
								rateCSVLine.setSalePrice(payrate.getSearchCost()*a2cRate);
								rateCSVLine.setSublotPrice(payrate.getSublotCost()*a2cRate);
								rateCSVLine.setUpdatePrice(payrate.getUpdateCost()*a2cRate);
								rateCSVLine.setFvsPrice(payrate.getFvsCost()*a2cRate);
							} else {
								rateCSVLine.setAcreagePrice(payrate.getAcreageValue()*c2aRate);
								rateCSVLine.setCommercialPrice(payrate.getCommercialValue()*c2aRate);
								rateCSVLine.setConstructionPrice(payrate.getConstructionValue()*c2aRate);
								rateCSVLine.setCurrentOwnerPrice(payrate.getCurrentOwnerValue()*c2aRate);
								rateCSVLine.setLienPrice(payrate.getLiensValue()*c2aRate);
								rateCSVLine.setOePrice(payrate.getOEValue()*c2aRate);
								rateCSVLine.setRefinancePrice(payrate.getRefinanceValue()*c2aRate);
								rateCSVLine.setSalePrice(payrate.getSearchValue()*c2aRate);
								rateCSVLine.setSublotPrice(payrate.getSublotValue()*c2aRate);
								rateCSVLine.setUpdatePrice(payrate.getUpdateValue()*c2aRate);
								rateCSVLine.setFvsPrice(payrate.getFvsValue()*c2aRate);
							}
							String found = foundMap.get(rateCSVLine);
							if(found == null) {
								found = rateCSVLine.getCountyName();
							} else {
								found += ", " + rateCSVLine.getCountyName();
							}
							foundMap.put(rateCSVLine, found);
							result.add(rateCSVLine);
							//finished county payrate
						}
					}
        		}
			}
        	
        	
        	
        	//at this point i have the lines as keys
        	//i need to update county name and that is all
        	Map<String, Integer> userStateSet = new HashMap<String, Integer>();
        	for (RateCSVLine rateCSVLine : foundMap.keySet()) {
				String userStateKey = rateCSVLine.getUserId() + "_" + rateCSVLine.getStateId();
				Integer keysFound = userStateSet.get(userStateKey);
				if(keysFound == null) {
					userStateSet.put(userStateKey, new Integer(1));
				} else {
					userStateSet.put(userStateKey, keysFound + 1);
				}
				rateCSVLine.setCountyName(foundMap.get(rateCSVLine));
				rateCSVLine.setCountyId(-1);
			}
        	result = new ArrayList<RateCSVLine>();
        	for (RateCSVLine rateCSVLine : foundMap.keySet()) {
        		String userStateKey = rateCSVLine.getUserId() + "_" + rateCSVLine.getStateId();
				Integer keysFound = userStateSet.get(userStateKey);
				if(keysFound != null && keysFound == 1) {
					rateCSVLine.setCountyName("All Selected Counties");
				}
				result.add(rateCSVLine);
        	}
        	
        } catch (Exception e) {
            e.printStackTrace();
        } 
        
        
        return result;
	}
	public String[] getLineForFile() {
		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setRoundingMode(RoundingMode.HALF_DOWN);
		numberFormat.setMinimumFractionDigits(0);
		numberFormat.setMaximumFractionDigits(0);
		
		String[] line = new String[19];
		line[0] = String.valueOf(getUserId());
		line[1] = getOperatingAccountId();
		line[2] = getLogin();
		line[3] = getFirstName();
		line[4] = getLastName();
		line[5] = String.valueOf(getUserRateIndex());
		line[6] = numberFormat.format(getSalePrice()); 
		line[7] = numberFormat.format(getCurrentOwnerPrice());
		line[8] = numberFormat.format(getConstructionPrice());
		line[9] = numberFormat.format(getCommercialPrice());
		line[10] = numberFormat.format(getRefinancePrice());
		line[11] = numberFormat.format(getOePrice());
		line[12] = numberFormat.format(getLienPrice());
		line[13] = numberFormat.format(getAcreagePrice());
		line[14] = numberFormat.format(getSublotPrice());
		line[15] = numberFormat.format(getUpdatePrice());
		line[16] = numberFormat.format(getFvsPrice());
		line[17] = getStateAbrev();
		line[18] = getCountyName();
		return line;
	}
	
	public static String[] getHeader(boolean isTSAdmin, int commId){
		String[] line = new String[19];
		line[0] = "UserID";
		line[1] = "AccountID";
		line[2] = "Login Name";
		line[3] = "First Name";
		line[4] = "Last Name";
		if(isTSAdmin) {
			line[5] = String.valueOf("ATS2Community Rate");
		} else {
			line[5] = String.valueOf("Community2Agent Rate");
		}
		Products products = CommunityProducts.getProduct(commId);
		line[6] = products.getProductName(Products.FULL_SEARCH_PRODUCT); 
		line[7] = products.getProductName(Products.CURRENT_OWNER_PRODUCT);
		line[8] = products.getProductName(Products.CONSTRUCTION_PRODUCT);
		line[9] = products.getProductName(Products.COMMERCIAL_PRODUCT);
		line[10] = products.getProductName(Products.REFINANCE_PRODUCT);
		line[11] = products.getProductName(Products.OE_PRODUCT);
		line[12] = products.getProductName(Products.LIENS_PRODUCT);
		line[13] = products.getProductName(Products.ACREAGE_PRODUCT);
		line[14] = products.getProductName(Products.SUBLOT_PRODUCT);
		line[15] = products.getProductName(Products.UPDATE_PRODUCT);
		line[16] = products.getProductName(Products.FVS_PRODUCT);
		line[17] = "State";
		line[18] = "County";
		return line;
	}
}

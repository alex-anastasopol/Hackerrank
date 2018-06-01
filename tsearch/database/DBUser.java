package ro.cst.tsearch.database;

import static ro.cst.tsearch.utils.DBConstants.TABLE_USER_COUNTY;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.CountyStateUser;
import ro.cst.tsearch.database.rowmapper.UserFilterMapper;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserSimpleMapper;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.warning.WarningManager;
import com.stewart.ats.user.User;
import com.stewart.ats.user.UserAttributesI;
import com.stewart.ats.user.UserFilters;
import com.stewart.ats.user.UserI;
import com.stewart.ats.user.UserManager;
import com.stewart.ats.user.UserManagerI;
import com.stewart.ats.user.UserRestrictions;
import com.stewart.ats.user.UserRestrictionsI;
import com.stewart.ats.user.profile.client.StateCountyItem;

public class DBUser {
	
	private static final Logger logger = Logger.getLogger(DBUser.class);
	
	public static String getAllCountiesForStateRatesFilterSql( int[] stateIdArray, int[] countyIdArray,
			String[] c2aFilter, String[] a2cFilter, int[] userIds, int commId, String sortBy, String sortOrder, int allowedFilter, int offset, int limit, int code, boolean doCountOnly) {
		
        String stateSelect = " and ( ";
        String countySelect = " and ( ";
        
        boolean allStates = false;
        boolean allCounties = false;
        boolean allC2AFilters = false;
        boolean allA2CFilters = false;
        List<Long> validUserIds = null;        
        UserManagerI userManagerI = UserManager.getInstance();
        try {
			userManagerI.getAccess();
			if(code == 1) {
				validUserIds = userManagerI.validateUsersForAssignModule(userIds, commId,
						GroupAttributes.ABS_ID,
						GroupAttributes.CA_ID,
						GroupAttributes.CCA_ID,
						GroupAttributes.TA_ID);
			} else {
				validUserIds = userManagerI.validateUsersForAssignModule(userIds, commId, 
						GroupAttributes.ABS_ID,
						GroupAttributes.CA_ID,
						GroupAttributes.CCA_ID,
						GroupAttributes.TA_ID,
						GroupAttributes.AG_ID);
			}
		} catch (Throwable t) {
			logger.error("Error while validating users", t);
		} finally {
			userManagerI.releaseAccess();
		}
		
		String usersAsString = null;
		if(validUserIds != null) {
			if(validUserIds.size() == 0) {
				usersAsString = "-2121";
			} else {
				usersAsString = org.apache.commons.lang.StringUtils.join(validUserIds.toArray(),",");
			}
		}
        
        for( int j = 0 ; j < stateIdArray.length ; j ++ )
        {
        	if(stateIdArray[j] == -2) {
        		return "";
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
        
        for( int j = 0 ; j < countyIdArray.length ; j ++ )
        {
        	if(countyIdArray[j] == -2) {
        		return "";
        	}
            if( countyIdArray[j] <= 0 ) {
                allCounties = true;
                break;
            }
            countySelect += " c.ID = " + countyIdArray[j];
            if( j < countyIdArray.length - 1 ) {
            	countySelect += " or ";
            }
        }
        
        if(!allStates) {
            stateSelect += " ) ";
        } else {
            stateSelect = " ";
        }
        
        if(!allCounties) {
            countySelect += " ) ";
        } else {
        	countySelect = " ";
        }
        
        if(Util.isValueInArray("-1", c2aFilter)) {
        	allC2AFilters = true;
        }
        if(Util.isValueInArray("-1", a2cFilter)) {
        	allA2CFilters = true;
        }
        
        StringBuilder stm = new StringBuilder();
        
        
    	/*select c.id, c.name, s.STATEABV
    	from ts_user_rating r  
    	join ts_county c on c.id = r.county_id
    	join ts_state s on c.state_id = s.id
    	JOIN  (select max(ri.start_date) start_date, ri.county_id
    	from ts_user_rating ri 
    	where ri.user_id = 1172 group by ri.county_id) j1 
    	on j1.start_date = r.start_date  
    	where r.user_id = 1172
    	and r.county_id = j1.county_id and r.C2ARATEINDEX in (4)*/
    	
        String allowedFilterStr = "";
        if(allowedFilter != -1) {
        	if(allowedFilter==0) {
        		/* Filter allowed counties */
        		allowedFilterStr =
        			" and ( " +
        			" (u.gid = 1 OR u.gid = 2 OR u.gid = 7) OR "+
        			"( (1 = (SELECT COUNT(DISTINCT county_id) FROM ts_user_county tuc WHERE tuc.user_id = u.user_id AND county_id = c.id) ) OR " +
        			"(0 = (SELECT COUNT(*) FROM ts_user_county tuc WHERE tuc.user_id = u.user_id) ))" +
        			" ) ";
        			;
        	}
        	if(allowedFilter==1) {
        		/* Filter forbidden counties */
        		allowedFilterStr =
	        		" and ( " +
	        		" (u.gid != 1 AND u.gid != 2 AND u.gid != 7) AND "+
	    			"((0 = (SELECT COUNT(DISTINCT county_id) FROM ts_user_county tuc WHERE tuc.user_id = u.user_id AND county_id = c.id)) AND " +
	    			"(0 < (SELECT COUNT(*) FROM ts_user_county tuc WHERE tuc.user_id = u.user_id) ))" +
	    			" ) ";
    			;
        		
        	}/*
        	  allowedFilterStr = " and ( ( c.ID " +
        	  					((allowedFilter==1)?"NOT":"")+
        	  					" IN (SELECT DISTINCT county_id FROM ts_user_county tuc WHERE tuc.user_id = u.user_id) ) "+
        	  					((allowedFilter==0)?" OR (0 = (SELECT COUNT(*)  FROM ts_user_county WHERE user_id =  u.user_id)) ":"") +
        	  					((allowedFilter==1)?" AND (0 < (SELECT COUNT(*)  FROM ts_user_county WHERE user_id =  u.user_id)) ":"") +
        	  					" ) ";*/
        }
       
        if(allC2AFilters && allA2CFilters) {
        	String what = doCountOnly 
        					? " count(*) "
        					: " c.id countyId, c.name countyName, s.STATEABV stateAbrv, u.user_id, concat(u.last_name, ' ', u.first_name, ifnull(concat(' ',u.middle_name),'')) userName ";
        	
        	stm.append("select  " + 
        			what + 
        			" from " + DBConstants.TABLE_COUNTY + " c, ts_state s, ts_user u  " + 
    	        	" where c.state_id = s.id and u.user_id in (" + usersAsString + ") ");
        	stm.append(stateSelect);
        	stm.append(countySelect);
        	stm.append(allowedFilterStr);
        } else {
	    	stm.append("select c.id countyId, c.name countyName, s.STATEABV stateAbrv , u.user_id,concat(u.last_name, ' ', u.first_name, ifnull(concat(' ',u.middle_name),'')) userName " + 
	    			" from ts_user u, ts_user_rating r " +   
	    			" join " + DBConstants.TABLE_COUNTY + " c on c.id = r.county_id " + 
	    			" join ts_state s on c.state_id = s.id " + 
		        	"JOIN  (select max(ri.start_date) start_date, ri.county_id " + 
		        	"from ts_user_rating ri " + 
		        	"where ri.user_id in (" + usersAsString + ") group by ri.county_id) j1 " +  
		        	"on j1.start_date = r.start_date  " + 
		        	"where u.user_id = r.user_id and r.user_id in (" + usersAsString + ") ");
	    	stm.append(stateSelect);
	    	stm.append(countySelect);
	    	stm.append("and r.county_id = j1.county_id ");
	    	stm.append(allowedFilterStr);

        }
    	
    	
    	
    	
    	for (int i = 0; i < c2aFilter.length && !allC2AFilters; i++) {
			if (i==0) {
				stm.append(" AND r.C2ARATEINDEX in (?");
			} else {
				stm.append(",?");
			}
			if(i==c2aFilter.length - 1) {
				stm.append(") ");
			}
		}

        for (int i = 0; i < a2cFilter.length && !allA2CFilters; i++) {
        	if (i==0) {
        		stm.append(" AND r.ATS2CRATEINDEX in (?");
			} else {
				stm.append(",?");
			}
			if(i==a2cFilter.length - 1) {
				stm.append(") ");
			}
		}
        if(!doCountOnly) {
        	stm.append(" order by " + DBManager.sqlColumnName(sortBy) + " " + DBManager.sqlOrderType(sortOrder));  
                
        	if(offset != -1 && limit != -1 ) {
        		stm.append(" LIMIT "+offset+","+limit+" ");
        	}
        }
    	if(doCountOnly && (!allA2CFilters || !allC2AFilters)) {
    		stm = new StringBuilder("select count(*) from ( " + stm.toString() + ") csu ");
    	}
        
        return stm.toString();
	}
	
	public static int getAllCountiesForStateRatesFilterCount( int[] stateIdArray, int[] countyIdArray,
			String[] c2aFilter, String[] a2cFilter, int[] userIds, int commId, String sortBy, String sortOrder, int allowedFilter, int offset, int limit, int code) {
		int count = 0;
		try{
			boolean allA2CFilters = false;
			boolean allC2AFilters = false;
			
	        if(Util.isValueInArray("-1", c2aFilter)) {
	        	allC2AFilters = true;
	        }
	        if(Util.isValueInArray("-1", a2cFilter)) {
	        	allA2CFilters = true;
	        }
	        
			String stm = getAllCountiesForStateRatesFilterSql(stateIdArray, countyIdArray, c2aFilter, a2cFilter, userIds, commId, sortBy, sortOrder, allowedFilter, offset, limit, code, true);
			
			if(stm.isEmpty()) {
				return 0;
			}
	        
        	SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
        	PreparedStatementCreatorFactory pscFactory = new PreparedStatementCreatorFactory(stm.toString());
        	List<Object> parameters = new ArrayList<Object>();
        	for (int i = 0; i < c2aFilter.length && !allC2AFilters; i++) {
        		pscFactory.addParameter(new SqlParameter(Types.FLOAT));
        		parameters.add(c2aFilter[i]);
	        }
	        for (int i = 0; i < a2cFilter.length && !allA2CFilters; i++) {
	        	pscFactory.addParameter(new SqlParameter(Types.FLOAT));
	        	parameters.add(a2cFilter[i]);
	        }
	        
	        count = sjt.getJdbcOperations().queryForInt(stm,parameters.toArray());
	        
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return count;
	}
	
	public static List<CountyStateUser> getAllCountiesForStateRatesFilter( int[] stateIdArray, int[] countyIdArray,
			String[] c2aFilter, String[] a2cFilter, int[] userIds, int commId, String sortBy, String sortOrder, int allowedFilter, int offset, int limit, int code)
    {   
		boolean allA2CFilters = false;
		boolean allC2AFilters = false;
		
        if(Util.isValueInArray("-1", c2aFilter)) {
        	allC2AFilters = true;
        }
        if(Util.isValueInArray("-1", a2cFilter)) {
        	allA2CFilters = true;
        }
        
		String stm = getAllCountiesForStateRatesFilterSql(stateIdArray, countyIdArray, c2aFilter, a2cFilter, userIds, commId, sortBy, sortOrder, allowedFilter, offset, limit, code, false);
		
		if(stm.isEmpty()) {
			return new ArrayList<CountyStateUser>();
		}
        
        try {
        	SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
        	PreparedStatementCreatorFactory pscFactory = new PreparedStatementCreatorFactory(stm.toString());
        	List<Object> parameters = new ArrayList<Object>();
        	for (int i = 0; i < c2aFilter.length && !allC2AFilters; i++) {
        		pscFactory.addParameter(new SqlParameter(Types.FLOAT));
        		parameters.add(c2aFilter[i]);
        	}
        	for (int i = 0; i < a2cFilter.length && !allA2CFilters; i++) {
        		pscFactory.addParameter(new SqlParameter(Types.FLOAT));
        		parameters.add(a2cFilter[i]);
        	}
        	
        	List<CountyStateUser> result = sjt.getJdbcOperations().query(
        			pscFactory.newPreparedStatementCreator(parameters),
        			new CountyStateUser());
        	
        	if(code > 0) {
	        	UserManagerI userManager = UserManager.getInstance();
	        	HashMap<Long, String> cacheAllowedAbstractors = new HashMap<Long, String>();
	        	HashMap<Long, String> cacheAllowedAgents = new HashMap<Long, String>();
	        	try {
					userManager.getAccess();
					for (CountyStateUser countyStateUser : result) {
						UserI user = userManager.getUser(countyStateUser.getUserId());
						if(code == 1) {
							String cachedInfoAbstractors = cacheAllowedAbstractors.get(user.getUserId());
							String cachedInfoAgents = cacheAllowedAgents.get(user.getUserId());
							UserRestrictionsI restrictionsI = user.getRestriction();
							if(cachedInfoAbstractors == null) {
								if(restrictionsI.hasAbstractorAssigned()) {
									cachedInfoAbstractors = "";
									int[] allowedArray = restrictionsI.getAllowedAbstractors();
									
									String[] tempArray = new String[allowedArray.length];
									for (int i = 0; i < allowedArray.length; i++) {
										UserI allowedUserI = userManager.getUser((long)allowedArray[i]);
										tempArray[i] = allowedUserI.getLastName() + " " + allowedUserI.getFirstName() + " - " + allowedUserI.getUserName();
									}
									
									Arrays.sort(tempArray);
									
									cachedInfoAbstractors = StringUtils.HTMLEntityEncode(org.apache.commons.lang.StringUtils.join(tempArray,", "));
									
								} else {
									cachedInfoAbstractors = "All Abstractors";
								}
								cacheAllowedAbstractors.put(user.getUserId(), cachedInfoAbstractors);
							} 
							countyStateUser.setRestrictionAllowedAbstractors(cachedInfoAbstractors);
							
							
							if(cachedInfoAgents == null ) {
								if(restrictionsI.hasAgentAllowed()) {
									int[] allowedArray = restrictionsI.getAllowedAgentsAsArray();
									cachedInfoAgents = "";
									
									String[] tempArray = new String[allowedArray.length];
									for (int i = 0; i < allowedArray.length; i++) {
										UserI allowedUserI = userManager.getUser((long)allowedArray[i]);
										tempArray[i] = allowedUserI.getLastName() + " " + allowedUserI.getFirstName() + " - " + allowedUserI.getUserName();
									}
									
									Arrays.sort(tempArray);
									
									cachedInfoAgents = StringUtils.HTMLEntityEncode(org.apache.commons.lang.StringUtils.join(tempArray,", "));
									
								} else {
									countyStateUser.setRestrictionAllowedAgents("All Agents");
									cachedInfoAgents = "All Agents";
								}
								cacheAllowedAgents.put(user.getUserId(), cachedInfoAgents);
							} 
							countyStateUser.setRestrictionAllowedAgents(cachedInfoAgents);
							
						}
						
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					userManager.releaseAccess();
				}
        	
        	}
        	
        	return result;
        	
        } catch (Exception e) {
            e.printStackTrace();
        } 
        
        
        return new ArrayList<CountyStateUser>();
    
    }
	
	public static List<CountyStateUser> getAllCountiesForStateRatesFilter( int[] stateIdArray, int[] countyIdArray,
			String[] c2aFilter, String[] a2cFilter, int[] userIds, int commId, String sortBy, String sortOrder, int code)
    {
		return getAllCountiesForStateRatesFilter(stateIdArray,countyIdArray,c2aFilter, a2cFilter, userIds,commId,sortBy,sortOrder,-1,-1,-1,code);		
    }
	
	public static int getAssignedCountiesForStateFilterCount( int[] stateIdArray, int[] countyIdArray,
			int[] userIds, int commId, String sortBy, String sortOrder, int allowedFilter, int offset, int limit)
    {   
		
		int count = 0;
		try {
			String stm = getAssignedCountiesForStateFilterSql(stateIdArray, countyIdArray, userIds, commId, sortBy, sortOrder,allowedFilter, offset, limit, true);
			if(stm.isEmpty()) {
				return 0;
			}
			count = DBManager.getSimpleTemplate().getJdbcOperations().queryForInt(stm);
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return count;
    }
	
	public static String getAssignedCountiesForStateFilterSql( int[] stateIdArray, int[] countyIdArray,
			int[] userIds, int commId, String sortBy, String sortOrder, int allowedFilter, int offset, int limit, boolean doCountOnly)
    {   
		String stateSelect = "and ( ";
        String countySelect = " and ( ";
        
        boolean allStates = false;
        boolean allCounties = false;
        
        List<Long> validUserIds = null;
        UserManagerI userManagerI = UserManager.getInstance();
        try {
			userManagerI.getAccess();
			validUserIds = userManagerI.validateUsersForAssignModule(userIds, commId, 
					GroupAttributes.ABS_ID,
					GroupAttributes.CA_ID,
					GroupAttributes.CCA_ID,
					GroupAttributes.TA_ID);
		} catch (Throwable t) {
			logger.error("Error while validating users", t);
		} finally {
			userManagerI.releaseAccess();
		}
		
		String usersAsString = null;
		if(validUserIds != null) {
			if(validUserIds.size() == 0) {
				usersAsString = "-2121";
			} else {
				usersAsString = org.apache.commons.lang.StringUtils.join(validUserIds.toArray(),",");
			}
		}
        for( int j = 0 ; j < stateIdArray.length ; j ++ )
        {
        	if(stateIdArray[j] == -2) {
        		return "";
        	}
        	
            if( stateIdArray[j] <= 0 )
            {
                allStates = true;
                break;
            }
            stateSelect += " c.STATE_ID = " + stateIdArray[j];
            if( j < stateIdArray.length - 1 )
            {
                stateSelect += " or ";
            }
        }
        for( int j = 0 ; j < countyIdArray.length ; j ++ )
        {
        	if(countyIdArray[j] == -2) {
        		return "";
        	}
            if( countyIdArray[j] <= 0 ) {
                allCounties = true;
                break;
            }
            countySelect += " c.ID = " + countyIdArray[j];
            if( j < countyIdArray.length - 1 ) {
            	countySelect += " or ";
            }
        }
        
        if(!allStates) {
            stateSelect += " ) ";
        } else {
            stateSelect = "";
        }
        if(!allCounties) {
            countySelect += " ) ";
        } else {
        	countySelect = " ";
        }
        
        String allowedFilterStr = "";
        if(allowedFilter != -1) {
        	if(allowedFilter==0) {
        		/* Filter allowed counties */
        		allowedFilterStr =
        			 " and ( "+
        				      " ( "+
        				      " (1 = (SELECT COUNT(DISTINCT filterValue) FROM ts_user_filters WHERE user_id = u.user_id AND filterValue = c.id AND type = 13 ) ) OR "+
        				      " (0 = (SELECT COUNT(filterValue) FROM ts_user_filters WHERE user_id = u.user_id AND type = 13 ) ) "+
        				      " ) and "+
        				      " ( "+
        				        	 " ( (1 = (SELECT COUNT(DISTINCT county_id) FROM ts_user_county tuc WHERE tuc.user_id = u.user_id AND county_id = c.id) ) OR "+
        				     	" (0 = (SELECT COUNT(*) FROM ts_user_county tuc WHERE tuc.user_id = u.user_id) )) "+
        				      " ) "+
        				  " ) "
        			;
        	}
        	if(allowedFilter==1) {
        		/* Filter forbidden or not assigned counties */
        		allowedFilterStr =
        				" and ( "+
        				      " ( "+
        				      " (0 = (SELECT COUNT(DISTINCT filterValue) FROM ts_user_filters WHERE user_id = u.user_id AND filterValue = c.id AND type = 13 ) ) AND "+
        				      " (0 < (SELECT COUNT(filterValue) FROM ts_user_filters WHERE user_id = u.user_id AND type = 13 ) ) "+
        				      " ) or "+
        				      " ( "+
        				      	" (u.gid != 1 AND u.gid != 2 AND u.gid != 7) AND "+
        				        " ((0 = (SELECT COUNT(DISTINCT county_id) FROM ts_user_county tuc WHERE tuc.user_id = u.user_id AND county_id = c.id)) AND "+
        				        " (0 < (SELECT COUNT(*) FROM ts_user_county tuc WHERE tuc.user_id = u.user_id) )) "+
        				      ") "+
        				    ") ";
    			;
        		
        	}
        }
        	
		String stm = "select  " +
		" distinct c.id countyId, c.name countyName, s.STATEABV stateAbrv, " +
		" u.user_id, concat(u.last_name, ' ', u.first_name, ifnull(concat(' ',u.middle_name),'')) userName "+
		" from " + DBConstants.TABLE_COUNTY + " c " + 
		" JOIN ts_state s on c.state_id = s.id " +
		" JOIN ts_user u " + 
		" LEFT JOIN ts_user_county uc on uc.county_id = c.id and u.user_id = uc.user_id " +
		" where u.user_id in (" + usersAsString + ") " + stateSelect + countySelect + allowedFilterStr;
		
		if(!doCountOnly) {
			stm+=" order by " + DBManager.sqlColumnName(sortBy) + " " + DBManager.sqlOrderType(sortOrder);
			
			if(offset != -1 && limit != -1 ) {
        		stm += " LIMIT "+offset+","+limit+" ";
        	}
		}
		if(doCountOnly) {
        	stm = "select count(*) from ( "+ stm + " ) t";
        }
		return stm;
    }
	
	public static List<CountyStateUser> getAssignedCountiesForStateFilter( int[] stateIdArray, int[] countyIdArray,
			int[] userIds, int commId, String sortBy, String sortOrder, int allowedFilter, int offset, int limit)
    {   
        
		String stm = getAssignedCountiesForStateFilterSql(stateIdArray, countyIdArray, userIds, commId, sortBy, sortOrder,allowedFilter, offset,limit,false);
		if(stm.isEmpty()) {
			return new ArrayList<CountyStateUser>();
		}
        Products products = CommunityProducts.getProduct(commId);
        
        try {
        	SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
        	//boolean hasRestrictions = sjt.queryForInt(
        			//"select count(*) > 0 from " + TABLE_USER_COUNTY +" where USER_ID = ?" , userId) > 0;
        	//if(hasRestrictions) {

        	
        	List<CountyStateUser> result = sjt.query(stm, new CountyStateUser());
        	
        	
        	UserManagerI userManager = UserManager.getInstance();
        	HashMap<Long, String> cacheProducts = new HashMap<Long, String>();
        	HashMap<Long, String> cacheAgents = new HashMap<Long, String>();
        	HashMap<Long, String> cacheSubcategories = new HashMap<Long, String>();
        	HashMap<Long, String> cacheWarnings = new HashMap<Long, String>();
        	WarningManager warningManager = WarningManager.getInstance();
        	try {
				userManager.getAccess();
				for (CountyStateUser countyStateUser : result) {
					UserI user = userManager.getUser(countyStateUser.getUserId());
					
					String cachedProducts = cacheProducts.get(user.getUserId());
					String cachedAgents = cacheAgents.get(user.getUserId());
					String cachedSubcategories = cacheSubcategories.get(user.getUserId());
					String cachedWarnings = cacheWarnings.get(user.getUserId());
					
					UserRestrictionsI restrictionsI = user.getRestriction();
					if(cachedProducts == null) {
						if(restrictionsI.hasProductsAssigned()) {
							cachedProducts = "";
							int[] allowedArray = restrictionsI.getOrdersProductsAsArray();
							String[] tempArray = new String[allowedArray.length];
							for (int i = 0; i < allowedArray.length; i++) {
								tempArray[i] = products.getProductName(allowedArray[i]);
							}
							
							Arrays.sort(tempArray);
									
							cachedProducts = StringUtils.HTMLEntityEncode(org.apache.commons.lang.StringUtils.join(tempArray,", "));
						} else {
							cachedProducts = "All Products";
						}
						cacheProducts.put(user.getUserId(), cachedProducts);
					} 
					countyStateUser.setOrdersProducts(cachedProducts);
					
					
					if(cachedAgents == null ) {
						if(restrictionsI.hasAgentAssigned()) {
							int[] allowedArray = restrictionsI.getOrdersAgentsAsArray();
							cachedAgents = "";
							String[] tempArray = new String[allowedArray.length];
							for (int i = 0; i < allowedArray.length; i++) {
								UserI allowedUserI = userManager.getUser((long)allowedArray[i]);
								tempArray[i] = allowedUserI.getLastName() + " " + allowedUserI.getFirstName() + " - " + allowedUserI.getUserName();
							}
							
							Arrays.sort(tempArray);
							
							cachedAgents = StringUtils.HTMLEntityEncode(org.apache.commons.lang.StringUtils.join(tempArray,", "));
						} else {
							countyStateUser.setRestrictionAllowedAgents("All Agents");
							cachedAgents = "All Agents";
						}
						cacheAgents.put(user.getUserId(), cachedAgents);
					} 
					countyStateUser.setOrdersAgents(cachedAgents);
					
					if(cachedSubcategories == null) {
						if(restrictionsI.hasSubcategories()) {
							cachedSubcategories = "";
							String[] allowedArray = restrictionsI.getOrdersSubcategoriesAsArray();
							Arrays.sort(allowedArray);
							cachedSubcategories = org.apache.commons.lang.StringUtils.join(allowedArray,", ");
							if(cachedSubcategories.length() > 0) {
								cachedSubcategories = StringUtils.HTMLEntityEncode(cachedSubcategories);
							}
						} else {
							cachedSubcategories = "No Subcategories";
						}
						cacheSubcategories.put(user.getUserId(), cachedSubcategories);
					} 
					countyStateUser.setOrdersSubcategories(cachedSubcategories);
					
					if(cachedWarnings == null) {
						if(restrictionsI.hasWarningsAssigned()) {
							cachedWarnings = "";
							int[] allowedArray = restrictionsI.getOrdersWarningsAsArray();
							String[] tempArray = new String[allowedArray.length];
							for (int i = 0; i < allowedArray.length; i++) {
								tempArray[i] = warningManager.getWarningForId(allowedArray[i]).getName();
							}
							Arrays.sort(tempArray);
							
							cachedWarnings = StringUtils.HTMLEntityEncode(org.apache.commons.lang.StringUtils.join(tempArray,", "));
							
						} else {
							cachedWarnings = "No warnings";
						}
						cacheWarnings.put(user.getUserId(), cachedWarnings);
					} 
					countyStateUser.setOrdersWarnings(cachedWarnings);
					
				}
					
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				userManager.releaseAccess();
			}
			
        	
        	return result;
/*
        	} else {
        		return sjt.query("select c.id countyId, c.name countyName, s.STATEABV stateAbrv " + 
        			" from ts_county c, ts_state s  " + 
    	        	" where c.state_id = s.id " + stateSelect + 
    				" order by " + DBManager.sqlColumnName(sortBy) + " " + DBManager.sqlOrderType(sortOrder), new CountyStateUser());
        	}*/        	
        	
        } catch (Exception e) {
            e.printStackTrace();
        } 
        
        
        return new ArrayList<CountyStateUser>();
    }
	
	@SuppressWarnings("unchecked")
	public static List<StateCountyItem> getStateCountyItems( int[] stateIdArray, 
			String[] c2aFilter, String[] a2cFilter, String userId, String sortBy, String sortOrder)
    {   
        String stateSelect = "and ( ";
        
        boolean allStates = (stateIdArray.length == 0);
        boolean allC2AFilters = false;
        boolean allA2CFilters = false;
        
        for( int j = 0 ; j < stateIdArray.length ; j ++ )
        {
        	if(stateIdArray[j] == -2) {
        		return new ArrayList<StateCountyItem>();
        	}
        	
            if( stateIdArray[j] <= 0 )
            {
                allStates = true;
                break;
            }
            stateSelect += " c.STATE_ID = " + stateIdArray[j];
            if( j < stateIdArray.length - 1 )
            {
                stateSelect += " or ";
            }
        }
        
        if(!allStates) {
            stateSelect += " ) ";
        } else {
            stateSelect = "";
        }
        
        if(Util.isValueInArray("-1", c2aFilter)) {
        	allC2AFilters = true;
        }
        if(Util.isValueInArray("-1", a2cFilter)) {
        	allA2CFilters = true;
        }
        
        StringBuilder stm = new StringBuilder();
        
        
    	/*select c.id, c.name, s.STATEABV
    	from ts_user_rating r  
    	join ts_county c on c.id = r.county_id
    	join ts_state s on c.state_id = s.id
    	JOIN  (select max(ri.start_date) start_date, ri.county_id
    	from ts_user_rating ri 
    	where ri.user_id = 1172 group by ri.county_id) j1 
    	on j1.start_date = r.start_date  
    	where r.user_id = 1172
    	and r.county_id = j1.county_id and r.C2ARATEINDEX in (4)*/
    	
        /*
        if(allC2AFilters && allA2CFilters) {
        	stm.append("select c.id countyId, c.name countyName, s.STATEABV stateAbrv " + 
        			" from ts_county c, ts_state s  " + 
    	        	" where c.state_id = s.id ");
        	stm.append(stateSelect);
        } else {
	    	stm.append("select c.id countyId, c.name countyName, s.STATEABV stateAbrv " + 
	    			" from ts_user_rating r " +   
	    			" join ts_county c on c.id = r.county_id " + 
	    			" join ts_state s on c.state_id = s.id " + 
		        	"JOIN  (select max(ri.start_date) start_date, ri.county_id " + 
		        	"from ts_user_rating ri " + 
		        	"where ri.user_id = ? group by ri.county_id) j1 " +  
		        	"on j1.start_date = r.start_date  " + 
		        	"where r.user_id = ? ");
	    	stm.append(stateSelect);
	    	stm.append("and r.county_id = j1.county_id ");
        }
        */
        stm.append(" select c.id countyId, c.name countyName, s.STATEABV, " + 
        		" (tempGogu.county_id is not null) allowed, ifnull(temp2.c2arateindex,1) c2arate, ifnull(temp2.ats2crateindex,1) ats2crate " +  
        		" from " + DBConstants.TABLE_COUNTY + " c JOIN ts_state s ON c.state_id = s.id " + 
        		" LEFT JOIN (select county_id from ts_user_county uc where uc.user_id = ?) tempGogu " +
        			" on  c.id  = tempGogu.county_id " + 
        		" LEFT JOIN (select r.start_date, r.c2arateindex, r.ats2crateindex, r.id, r.county_id " +
    						" from ts_user_rating r JOIN " +
    						" (select max(ri.start_date) start_date, ri.county_id " +
    							" from ts_user_rating ri where ri.user_id = ? group by ri.county_id) temp1 " +
    						" on r.start_date = temp1.start_date and r.county_id = temp1.county_id " +  
    						" where r.user_id = ? ) temp2 " +
        			" on c.id = temp2.county_id");
        stm.append(" where true ");
    	stm.append(stateSelect);
    	
    	
    	
    	for (int i = 0; i < c2aFilter.length && !allC2AFilters; i++) {
			if (i==0) {
				stm.append(" AND temp2.C2ARATEINDEX in (?");
			} else {
				stm.append(",?");
			}
			if(i==c2aFilter.length - 1) {
				stm.append(") ");
			}
		}

        for (int i = 0; i < a2cFilter.length && !allA2CFilters; i++) {
        	if (i==0) {
        		stm.append(" AND temp2.ATS2CRATEINDEX in (?");
			} else {
				stm.append(",?");
			}
			if(i==c2aFilter.length - 1) {
				stm.append(") ");
			}
		}
        	  
        stm.append(" order by " + DBManager.sqlColumnName(sortBy) + " " + DBManager.sqlOrderType(sortOrder));
        try {
        	SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
        	PreparedStatementCreatorFactory pscFactory = new PreparedStatementCreatorFactory(stm.toString());
        	List<Object> parameters = new ArrayList<Object>();
        	pscFactory.addParameter(new SqlParameter(Types.BIGINT));
        	parameters.add(userId);
        	pscFactory.addParameter(new SqlParameter(Types.BIGINT));
        	parameters.add(userId);
        	pscFactory.addParameter(new SqlParameter(Types.BIGINT));
        	parameters.add(userId);
        	for (int i = 0; i < c2aFilter.length && !allC2AFilters; i++) {
        		pscFactory.addParameter(new SqlParameter(Types.FLOAT));
        		parameters.add(c2aFilter[i]);
        	}
        	for (int i = 0; i < a2cFilter.length && !allA2CFilters; i++) {
        		pscFactory.addParameter(new SqlParameter(Types.FLOAT));
        		parameters.add(a2cFilter[i]);
        	}
        	
        	return sjt.getJdbcOperations().query(
        			pscFactory.newPreparedStatementCreator(parameters),
        			new RowMapper() {
					
						@Override
						public StateCountyItem mapRow(ResultSet resultSet, int rowNum) throws SQLException {
							StateCountyItem item = new StateCountyItem();
							item.setCountyId(resultSet.getInt("countyId"));
							item.setCountyName(resultSet.getString("countyName")); 
							item.setStateAbrv(resultSet.getString("STATEABV"));
							item.setAllowedHere(resultSet.getBoolean("allowed"));
							item.setA2cRate(resultSet.getFloat("ats2crate"));
							item.setC2aRate(resultSet.getFloat("c2arate"));
							
							return item;
						}
					});
        	
        } catch (Exception e) {
            logger.error("Error while reading state/county/rate info for user " + userId, e);
        } 
        
        
        return new ArrayList<StateCountyItem>();
    }
	
	public static void deleteAllowedCountyListForUser( String user, Vector<String> countyIds )
    {
        DBConnection conn = null;
        
        if( countyIds == null )
        {
            return;
        }
        
        String commaSepparatedCountyIds = "";
        for( int i = 0 ; i < countyIds.size() ; i ++ )
        {
            commaSepparatedCountyIds += countyIds.elementAt( i );
            if( i != countyIds.size() - 1 )
            {
                commaSepparatedCountyIds += ", ";
            }
        }
        
        if( "".equals( commaSepparatedCountyIds ) )
        {
            return;
        }
        
        try{
            conn = ConnectionPool.getInstance().requestConnection();
            
            String stm = "delete from "+ TABLE_USER_COUNTY + " where user_id = ? and COUNTY_ID in ( " + StringUtils.makeValidNumberList(commaSepparatedCountyIds) + " )" ;
            
    		PreparedStatement pstmt = conn.prepareStatement( stm );
    		pstmt.setString( 1, user);
    		pstmt.executeUpdate();
    		pstmt.close();
    		
            conn.commit();
        } catch (Exception be) {
            logger.error(be);
            be.printStackTrace();
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
    }
	
	public static void addAllowedCounty( BigDecimal userId, BigDecimal countyId )
    {
        DBConnection conn = null;
        
        if( countyId.longValue() == -1 )
        {
            return;
        }
        
        try{
            conn = ConnectionPool.getInstance().requestConnection();
            
            conn.executeSQL( "insert into "+ TABLE_USER_COUNTY +"( USER_ID, COUNTY_ID ) values( '" + userId.longValue() + "', '" + countyId.longValue() + "' )" );
            conn.commit();
        } catch (Exception be) {
            logger.error(be);
            be.printStackTrace();
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
    }

	/**
	 * Used to reset allowed counties for one user
	 */
	private static final String RESET_ALLOWED_COUNTIES_SQL = "DELETE FROM " + DBConstants.TABLE_USER_COUNTY + " WHERE " + 
		DBConstants.FIELD_USER_COUNTY_USER_ID + " = ?";
	
	/**
	 * Reset allowed counties for the specified user
	 * @param userId
	 */
	public static void resetAllowedCountiesForUser(String userId) {
		try {
			DBManager.getSimpleTemplate().update(RESET_ALLOWED_COUNTIES_SQL,userId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Used to get all users for that community
	 */
	private static final String GET_SELECT_FOR_USERS_SQL = "SELECT " + 
		DBConstants.FIELD_USER_ID + ", " + 
		DBConstants.FIELD_USER_LOGIN + ", " + 
		DBConstants.FIELD_USER_FIRST_NAME + ", " + 
		DBConstants.FIELD_USER_GID + ", " +
		DBConstants.FIELD_USER_LAST_NAME + " FROM " + 
		DBConstants.TABLE_USER + " WHERE " +
		DBConstants.FIELD_USER_COMM_ID + "= ? AND " + DBConstants.FIELD_USER_HIDDEN + " = 0";
	
	/**
	 * Creates a select object containing all the users in the community represented by commId 
	 * @param commId the id of the community
	 * @return
	 */
	public static String getSelectForUsers (long commId, String name, boolean multipleSelect, boolean showAllOption, int code){
		if(commId<0)
			return "<select name=" + name + " >\r\n" +
					"<option value=\"-1\" selected>No user\r\n" +
					"</option>\r\n" + 
					"</select>\r\n";
		try {
			
			ParameterizedRowMapper<UserSimpleMapper> mapper = new ParameterizedRowMapper<UserSimpleMapper>() {
			
				public UserSimpleMapper mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					return new UserSimpleMapper(
							rs.getLong(DBConstants.FIELD_USER_ID),
							rs.getString(DBConstants.FIELD_USER_LOGIN),
							rs.getString(DBConstants.FIELD_USER_FIRST_NAME),
							rs.getString(DBConstants.FIELD_USER_LAST_NAME),
							rs.getInt(DBConstants.FIELD_USER_GID));
				}
			
			};
			
			
			List<UserSimpleMapper> result = DBManager.getSimpleTemplate().query(GET_SELECT_FOR_USERS_SQL, mapper, commId);
			StringBuilder sb = new StringBuilder();
			sb.append("<select name=\"" + name + "\" ");
			if(multipleSelect) {
				sb.append(" multiple ");
			}
			sb.append(" size='5'>\r\n");
			if(showAllOption) {
				sb.append("<option value=\"-1\" selected>Select user</option>\r\n");
			}
			
			
			
			for (UserSimpleMapper usm : result) {
				if(code == 1) {
					if(usm.getUserType()!=GroupAttributes.AG_ID) {
						sb.append(usm.printOption()+"\r\n");
					}
				} else {
					sb.append(usm.printOption()+"\r\n");
				}
			}
			sb.append("</select>\r\n");
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "<select name=" + name + " >\r\n" +
			"<option value=\"-1\" selected>No user\r\n" +
			"</option>\r\n" + 
			"</select>\r\n";
	}

	/**
	 * Copies all allowed conties from one user to another
	 * @param sourceUserId
	 * @param destinationUserId
	 */
	public static void copyAllowedCounties(String sourceUserId, String destinationUserId) {
		resetAllowedCountiesForUser(destinationUserId);
		String sql = "INSERT INTO " + DBConstants.TABLE_USER_COUNTY + " SELECT " + Long.parseLong(destinationUserId) + ", " + 
		DBConstants.FIELD_USER_COUNTY_COUNTY_ID + " FROM " + 
		DBConstants.TABLE_USER_COUNTY + " WHERE " + 
		DBConstants.FIELD_USER_COUNTY_USER_ID + " = " + Long.parseLong(sourceUserId);
		try {
			DBManager.getSimpleTemplate().update(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void setATS2CommunityRating(UserAttributes currentUser, UserAttributes user, String rate, int countyId ){
		float c2aRate = getRateFromUser(user, UserAttributes.USER_C2ARATEINDEX, countyId);
		try {
			String sql = "INSERT INTO " + DBConstants.TABLE_USER_RATING + "(" +
                UserAttributes.USER_ID + "," + " county_id, " + 
                UserAttributes.USER_RATINGFROMDATE + "," + UserAttributes.USER_C2ARATEINDEX + "," + 
                UserAttributes.USER_ATS2CRATEINDEX + ") VALUES ( " +                           
                user.getID() + "," + countyId + ","
                + "STR_TO_DATE( '" + new FormatDate(FormatDate.TIMESTAMP).getDate( Calendar.getInstance().getTime() ) 
                + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )" + ","
                + c2aRate + ", ? )";
			DBManager.getSimpleTemplate().update(sql,rate);
			
			user.addRate( UserAttributes.A2CRATE, new BigDecimal( rate ), new BigDecimal(countyId) );
            user.addRate( UserAttributes.C2ARATE, new BigDecimal( c2aRate ), new BigDecimal(countyId) );
            
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void setCommunity2AgentRating(UserAttributes currentUser, UserAttributes user, String rate, int countyId ){
		float a2cRate = getRateFromUser(user, UserAttributes.USER_ATS2CRATEINDEX, countyId);
		try {
			String sql = "INSERT INTO " + DBConstants.TABLE_USER_RATING + "(" +
                UserAttributes.USER_ID + "," + " county_id, " + 
                UserAttributes.USER_RATINGFROMDATE + "," + UserAttributes.USER_C2ARATEINDEX + "," + 
                UserAttributes.USER_ATS2CRATEINDEX + ") VALUES ( " +                           
                user.getID() + "," + countyId + ","
                + "STR_TO_DATE( '" + new FormatDate(FormatDate.TIMESTAMP).getDate( Calendar.getInstance().getTime() ) 
                + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )" + ","
                + "?, " + a2cRate + ")";
			DBManager.getSimpleTemplate().update(sql,rate);
			
			user.addRate( UserAttributes.A2CRATE, new BigDecimal( a2cRate ), new BigDecimal(countyId) );
            user.addRate( UserAttributes.C2ARATE, new BigDecimal( rate ), new BigDecimal(countyId) );
            
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static float getRateFromUser (UserAttributes user, String rateField, long countyId){
		String sql = " select " + rateField  + " from " + DBConstants.TABLE_USER_RATING + " where "
		  + UserAttributes.USER_ID + "=" + user.getID() + " AND county_id = " + countyId + " AND " 
		  + UserAttributes.USER_RATINGFROMDATE + "=" +			   
		  "( select max("+ UserAttributes.USER_RATINGFROMDATE +") from " + DBConstants.TABLE_USER_RATING
		  + " where " + UserAttributes.USER_ID + "=" + user.getID() 
		  + " AND county_id = " + countyId + ")";
		
		float rate = 1;
		try {
			rate = DBManager.getSimpleTemplate().queryForObject(sql, Float.class);
		} catch (EmptyResultDataAccessException e) {
			logger.info("No database result fom getRateFromUser(user, " + rateField + "," + countyId + ").. Using default rate 1...");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rate;
		
	}

	public static List<UserI> getAllUsers(List<Long> userIds) {
		
		StringBuilder  sql = new StringBuilder("SELECT * FROM ts_user tu LEFT JOIN ts_user_settings tus on tus.user_id = tu.user_id ");
		for(String type: ro.cst.tsearch.user.UserManager.dashboardFilterTypes.keySet()) {  
			sql.append(" LEFT JOIN ( SELECT " 
			 			+ UserFilterMapper.FIELD_USER_ID + "," 
		 				+ " GROUP_CONCAT( CONVERT( "+ 
		 				( (type.equals("CompanyAgent") || type.equals("CategAndSubcateg"))?
 								UserFilterMapper.FIELD_FILTER_VALUE_STRING:
 								UserFilterMapper.FIELD_FILTER_VALUE_LONG)
 								+",CHAR) " + "SEPARATOR ',') AS report" +type
		 				+ " FROM ts_user_filters  "
		 				+ " WHERE " + UserFilterMapper.FIELD_TYPE + " = " + ro.cst.tsearch.user.UserManager.dashboardFilterTypes.get(type)
						+ " GROUP BY " + UserFilterMapper.FIELD_USER_ID + "," + UserFilterMapper.FIELD_TYPE + ") "
					+ " AS userFilters" + type 
					+ " ON tu.user_id = userFilters" + type + ".user_id ");
		}
		
		sql.append(" LEFT JOIN ( SELECT " 
 			+ UserFilterMapper.FIELD_USER_ID + "," 
				+ " GROUP_CONCAT( CONVERT( "+ 
						UserFilterMapper.FIELD_FILTER_VALUE_LONG
						+",CHAR) " + "SEPARATOR ',') AS " + UserRestrictions.FIELD_ALLOWED_ABSTRACTORS
				+ " FROM ts_user_filters  "
				+ " WHERE " + UserFilterMapper.FIELD_TYPE + " = " + UserFilters.TYPE_RESTRICTION_ABSTRACTOR
			+ " GROUP BY " + UserFilterMapper.FIELD_USER_ID + "," + UserFilterMapper.FIELD_TYPE + ") "
		+ " AS userFilters" + UserRestrictions.FIELD_ALLOWED_ABSTRACTORS 
		+ " ON tu.user_id = userFilters" + UserRestrictions.FIELD_ALLOWED_ABSTRACTORS + ".user_id ");
		
		sql.append(" LEFT JOIN ( SELECT " 
 			+ UserFilterMapper.FIELD_USER_ID + "," 
				+ " GROUP_CONCAT( CONVERT( "+ 
						UserFilterMapper.FIELD_FILTER_VALUE_LONG
						+",CHAR) " + "SEPARATOR ',') AS " + UserRestrictions.FIELD_ALLOWED_AGENTS
				+ " FROM ts_user_filters  "
				+ " WHERE " + UserFilterMapper.FIELD_TYPE + " = " + UserFilters.TYPE_RESTRICTION_AGENT
			+ " GROUP BY " + UserFilterMapper.FIELD_USER_ID + "," + UserFilterMapper.FIELD_TYPE + ") "
		+ " AS userFilters" + UserRestrictions.FIELD_ALLOWED_AGENTS 
		+ " ON tu.user_id = userFilters" + UserRestrictions.FIELD_ALLOWED_AGENTS + ".user_id ");
		
		sql.append(" LEFT JOIN ( SELECT " 
	 			+ UserFilterMapper.FIELD_USER_ID + "," 
					+ " GROUP_CONCAT( CONVERT( "+ 
							UserFilterMapper.FIELD_FILTER_VALUE_LONG
							+",CHAR) " + "SEPARATOR ',') AS " + UserRestrictions.FIELD_ASSIGNED_AGENTS
					+ " FROM ts_user_filters  "
					+ " WHERE " + UserFilterMapper.FIELD_TYPE + " = " + UserFilters.TYPE_ASSIGN_ORDERS_AGENT
				+ " GROUP BY " + UserFilterMapper.FIELD_USER_ID + "," + UserFilterMapper.FIELD_TYPE + ") "
			+ " AS userFilters" + UserRestrictions.FIELD_ASSIGNED_AGENTS 
			+ " ON tu.user_id = userFilters" + UserRestrictions.FIELD_ASSIGNED_AGENTS + ".user_id ");
		
		sql.append(" LEFT JOIN ( SELECT " 
	 			+ UserFilterMapper.FIELD_USER_ID + "," 
					+ " GROUP_CONCAT( CONVERT( "+ 
							UserFilterMapper.FIELD_FILTER_VALUE_LONG
							+",CHAR) " + "SEPARATOR ',') AS " + UserRestrictions.FIELD_ASSIGNED_COUNTIES
					+ " FROM ts_user_filters  "
					+ " WHERE " + UserFilterMapper.FIELD_TYPE + " = " + UserFilters.TYPE_ASSIGN_ORDERS_COUNTY
				+ " GROUP BY " + UserFilterMapper.FIELD_USER_ID + "," + UserFilterMapper.FIELD_TYPE + ") "
			+ " AS userFilters" + UserRestrictions.FIELD_ASSIGNED_COUNTIES 
			+ " ON tu.user_id = userFilters" + UserRestrictions.FIELD_ASSIGNED_COUNTIES + ".user_id ");
		
		sql.append(" LEFT JOIN (SELECT " + DBConstants.FIELD_USER_COUNTY_USER_ID + 
			" , GROUP_CONCAT(CONVERT(" + DBConstants.FIELD_USER_COUNTY_COUNTY_ID + ",CHAR) " +
			" SEPARATOR ',' ) AS allowedCounties FROM " + DBConstants.TABLE_USER_COUNTY + 
			" GROUP BY " +  DBConstants.FIELD_USER_COUNTY_USER_ID  + 
			" ) AS uct ON tu.user_id =  uct." + DBConstants.FIELD_USER_COUNTY_USER_ID);  
	
		if(userIds != null && userIds.size() > 0) {
			String listIds = " WHERE tu.user_id in (";
			for (Long userId : userIds) {
				listIds += userId + ",";
			}
			listIds = listIds.substring(0, listIds.length() - 1) + ")";
			sql.append(listIds);
		}
		List<UserI> users = null;
		try {
			users = DBManager.getSimpleTemplate().query(sql.toString(), new User());
		} catch (Throwable t) {
			logger.error("DBUser Error while reading all users with getAllUsers", t);
		}
		return users;
	}
	
	private static final String SQL_NUMBER_OF_SEARCHES_FOR_USER = "select count(*) from " + DBConstants.TABLE_SEARCH + " s where " +
		" s." + DBConstants.FIELD_SEARCH_ABSTRACT_ID + " = ? and " + 
		" (s." + DBConstants.FIELD_SEARCH_STATUS + " = 0 OR s." + DBConstants.FIELD_SEARCH_STATUS + " = 1)";  
		
	public static int getNumberOfSearchesForUser(long userId) {
		try {
			return DBManager.getSimpleTemplate().queryForInt(SQL_NUMBER_OF_SEARCHES_FOR_USER, userId);
		} catch (Exception e) {
			logger.info("Error while reading getNumberOfSearchesForUser " + userId, e);
		}
		return -1;
	}
	
	private static final String SQL_RANDOM_TOKEN_FOR_USER = "select " + 
		DBConstants.FIELD_USER_RANDOM_TOKEN + " from " + 
		DBConstants.TABLE_USER + " where " +
		DBConstants.FIELD_USER_ID + " = ?";   
	public static String getRandomToken(long userId) {
		try {
			return DBManager.getSimpleTemplate().queryForObject(SQL_RANDOM_TOKEN_FOR_USER, String.class, userId);
		} catch (Exception e) {
			logger.info("Error while reading getRandomToken " + userId, e);
		}
		return null;
	}
	
	public static final String SQL_UPDATE_PASSWORD = "update " + 
		DBConstants.TABLE_USER + " set " + 
		DBConstants.FIELD_USER_PASSWORD_ENCRYPTED + " = ?, " + 
		DBConstants.FIELD_USER_NOTIFICATION_EXPIRE_PASS_SENT + " = 0, " +
		DBConstants.FIELD_USER_LAST_PASSWORD_CHANGE_DATE + " = ? where " + 
		DBConstants.FIELD_USER_ID + " = ?";
	
	
	
	public static void main(String[] args) {
		List<Long> list = new ArrayList<Long>();
		list.add(1615l);
		getAllUsers(null);
	}

	private static final String SQL_INVALIDATE_USERS_BASED_ON_INACTIVITY = "update " +
		DBConstants.TABLE_USER + " set " + 
		DBConstants.FIELD_USER_HIDDEN + " = 1 WHERE " + 
		DBConstants.FIELD_USER_HIDDEN + " = 0 AND " + 
		DBConstants.FIELD_USER_INTERACTIVE + " = 1 AND " + 
		DBConstants.FIELD_USER_LAST_PASSWORD_CHANGE_DATE + " + INTERVAL ? DAY < NOW()";
	
	public static int invalidateUsersBasedOnInactivity(int timeoutInDays) {
		return DBManager.getSimpleTemplate().update(SQL_INVALIDATE_USERS_BASED_ON_INACTIVITY, timeoutInDays);
	}
	

	private static final String SQL_UPDATE_HIDDEN_STATUS_FOR_USER = "UPDATE " + 
		DBConstants.TABLE_USER + " SET " + 
		UserAttributes.USER_HIDDEN + " = ? WHERE " + 
		DBConstants.FIELD_USER_ID + " = ?";
	
	public static int updateHideStatus(boolean hideUser, long userId) {
		return DBManager.getSimpleTemplate().update(SQL_UPDATE_HIDDEN_STATUS_FOR_USER, hideUser?1:0, userId);
		
	}
	
	private static final String SQL_QUERY_ABOUT_TO_EXPIRE = " SELECT (" + 
		DBConstants.FIELD_USER_LAST_PASSWORD_CHANGE_DATE + " + INTERVAL ? DAY < now()) aboutToExpire FROM " + 
		DBConstants.TABLE_USER + " WHERE " + 
		DBConstants.FIELD_USER_INTERACTIVE + " = 1 AND " + 
		DBConstants.FIELD_USER_ID + " = ?";
	
	public static boolean isAboutToExpire(long userId, int activationIntervalInDays) {
		return DBManager.getSimpleTemplate().queryForInt(SQL_QUERY_ABOUT_TO_EXPIRE, activationIntervalInDays, userId) == 1;
	}
	
	/*public static boolean isUserExpired(long userId, int activationIntervalInDays) {
		return DBManager.getSimpleTemplate().queryForInt(SQL_QUERY_ABOUT_TO_EXPIRE, activationIntervalInDays, userId) == 1;
	}*/

	private static final String SQL_QUERY_USERS_ABOUT_TO_EXPIRE = "select " +
		DBConstants.FIELD_USER_LOGIN + "," + 
		DBConstants.FIELD_USER_EMAIL + " FROM " +
		DBConstants.TABLE_USER + " WHERE " + 
		DBConstants.FIELD_USER_HIDDEN + " = 0 AND " + 
		DBConstants.FIELD_USER_INTERACTIVE + " = 1 AND " + 
		DBConstants.FIELD_USER_NOTIFICATION_EXPIRE_PASS_SENT + " = 0 AND " + 
		DBConstants.FIELD_USER_LAST_PASSWORD_CHANGE_DATE + " + INTERVAL ? DAY < NOW()";
	
	public static List<UserAttributesI> getUsersAboutToExpire(int activationIntervalInDays) {
		return DBManager.getSimpleTemplate().query(SQL_QUERY_USERS_ABOUT_TO_EXPIRE, new ParameterizedRowMapper<UserAttributesI>() {

			@Override
			public UserAttributesI mapRow(ResultSet resultSet, int arg1) throws SQLException {
				UserAttributesI userAttributesI = new com.stewart.ats.user.UserAttributes();
				userAttributesI.setUserName(resultSet.getString(DBConstants.FIELD_USER_LOGIN));
				userAttributesI.setEmail(resultSet.getString(DBConstants.FIELD_USER_EMAIL));
				return userAttributesI;
			}
		}, activationIntervalInDays);
		
	}

	private static final String SQL_UPDATE_NOTIFIED_USER_FOR_EXPIRATION = "UPDATE " + 
		DBConstants.TABLE_USER + " SET " + 
		DBConstants.FIELD_USER_NOTIFICATION_EXPIRE_PASS_SENT + " = ? WHERE " + 
		DBConstants.FIELD_USER_LOGIN + " = ?";
	
	public static int setNotifiedUserForExpiration(boolean setNotified, String userName) {
		return DBManager.getSimpleTemplate().update(SQL_UPDATE_NOTIFIED_USER_FOR_EXPIRATION, setNotified?1:0, userName);
		
	}


}

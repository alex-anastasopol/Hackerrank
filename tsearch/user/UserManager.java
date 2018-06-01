package ro.cst.tsearch.user;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityManager;
import ro.cst.tsearch.data.DataAttribute;
import ro.cst.tsearch.data.DataException;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.database.rowmapper.UserFilterMapper;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.servers.parentsite.Company;
import ro.cst.tsearch.utils.CSTCalendar;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MultipartParameterParser;
import ro.cst.tsearch.utils.ParameterNotFoundException;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.PasswordGenerator;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SecurityUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.user.UserManagerI;

public class UserManager {

	protected static final Category logger = Logger.getLogger(UserManager.class);
	public static final String TITLE_VIEW_USER_PAGE = "View User Profile";

	private UserManager() {
	}

	public static UserAttributes getTSAdminUser() {
		logger.info("Getting Automatic Search User...");
		UserAttributes tsuser = null;

		DBConnection conn = null;
		try {

			String sql = "SELECT * FROM TS_USER WHERE LOGIN = '" + UserAttributes.TSADMIN + "'";

			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(sql);

			if (data.getRowNumber() == 0 || data.getRowNumber() > 1) {
				return null;
			} else {
				tsuser = new UserAttributes(data, 0);
				fillUserRating(tsuser, conn);
			}

		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		return tsuser;
	}

	/**
	 * Should be moved to interface com.stewart.ats.user.UserFilter
	 */
	@Deprecated
	public static Hashtable<String, Integer> dashboardFilterTypes = new Hashtable<String, Integer>();
	static {
		dashboardFilterTypes.put("State", 1);
		dashboardFilterTypes.put("County", 2);
		dashboardFilterTypes.put("Abstractor", 3);
		dashboardFilterTypes.put("CompanyAgent", 4);
		dashboardFilterTypes.put("Agent", 5);
		dashboardFilterTypes.put("Status", 6);
		dashboardFilterTypes.put("ProductType", 7);
		dashboardFilterTypes.put("Warning", 8);
		dashboardFilterTypes.put("CategAndSubcateg", 9);
		// do not add what is already used in com.stewart.ats.user.UserFilter
	}

	public static String getUserSettingsJoin() {
		String join = " LEFT JOIN " + DBConstants.TABLE_USER_SETTINGS + " USING(" + DBConstants.FIELD_USER_ID + ")";
		/*
		for (String type : dashboardFilterTypes.keySet())
			join += " LEFT JOIN ( SELECT "
					+ UserFilterMapper.FIELD_USER_ID
					+ ","
					+ " GROUP_CONCAT( CONVERT( "
					+ (!type.equals("CompanyAgent") ? UserFilterMapper.FIELD_FILTER_VALUE_LONG
							: UserFilterMapper.FIELD_FILTER_VALUE_STRING) + ",CHAR) " + "SEPARATOR ',') AS report"
					+ type + " FROM ts_user_filters  " + " WHERE " + UserFilterMapper.FIELD_TYPE + " = "
					+ dashboardFilterTypes.get(type) + " GROUP BY " + UserFilterMapper.FIELD_USER_ID + ","
					+ UserFilterMapper.FIELD_TYPE + ") " + " AS userFilters" + type + " USING(user_id) ";
		*/
		return join;
	}

	private static final String SQL_GET_RANDOM_NUMBER = " SELECT " + UserAttributes.USER_RANDOM_TOKEN + " FROM "
			+ DBConstants.TABLE_USER + " WHERE login = ?";
	private static final String SQL_IS_LOGIN_VALID = " SELECT REPLACE(REPLACE(" + UserAttributes.USER_PASSWORD + ", '\r', ''), '\n', '') = ? FROM "
			+ DBConstants.TABLE_USER + "  WHERE login = ?";
	private static final String SQL_IS_LOGIN_VALID_WITH_EXPIRATION = " SELECT REPLACE(REPLACE(" + UserAttributes.USER_PASSWORD
			+ ", '\r', ''), '\n', '') = ? FROM " + DBConstants.TABLE_USER + "  WHERE (login = ?) and ( (interactive = 0) OR ("
			+ DBConstants.FIELD_USER_LAST_PASSWORD_CHANGE_DATE + " + INTERVAL ? DAY >= now()) )";
	private static final String SQL_GET_ENCODED_PASSWORD = " SELECT REPLACE(REPLACE(" + UserAttributes.USER_PASSWORD + ", '\r', ''), '\n', '') " + UserAttributes.USER_PASSWORD + " FROM "
			+ DBConstants.TABLE_USER + "  WHERE user_id = ?";

	/**
	 * Tests if the entered combination is a valid user in ats
	 * 
	 * @param login
	 *            username
	 * @param password
	 * @return true only if the user and password match
	 */
	public static boolean checkCredentials(String login, String password, boolean checkExpirationDate) {
		if(URLMaping.INSTANCE_DIR.startsWith("local")){
			return true;
		}
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		if (StringUtils.isEmpty(login) || StringUtils.isEmpty(password))
			return false;
		try {
			String randomToken = sjt.queryForObject(SQL_GET_RANDOM_NUMBER, String.class, login);
			randomToken = StringUtils.intercalateCharacters(new String[] { password, randomToken });
			if (!checkExpirationDate) {
				return sjt.queryForObject(SQL_IS_LOGIN_VALID, Boolean.class, SecurityUtils.getInstance().encryptSHA512(
						randomToken), login);
			} else {
				
//				String encryptedPass = SecurityUtils.getInstance().encryptSHA512(randomToken);
//				
//				System.err.println("SQL_IS_LOGIN_VALID_WITH_EXPIRATION: " + SQL_IS_LOGIN_VALID_WITH_EXPIRATION);
//				System.err.println("randomToken: " + randomToken);
//				System.err.println("Encr: " + encryptedPass);
//				
//				String databasePass = sjt.queryForObject("SELECT PASSWORD FROM ts_user  WHERE (login = ?) and ( (interactive = 0) OR (lastPassChangeDate + INTERVAL ? DAY >= now()) )", String.class, login, 90);
//				
//				System.err.println("pass: " + databasePass);
//				
//				System.err.println("Res1: " + sjt.queryForObject(SQL_IS_LOGIN_VALID_WITH_EXPIRATION, Boolean.class, SecurityUtils
//						.getInstance().encryptSHA512(randomToken), login, 90));
//				
//				System.err.println("Size " + encryptedPass.length() + " and " + databasePass.length() + " and are equal " + encryptedPass.equals(databasePass));
//				
//				
				return sjt.queryForObject(SQL_IS_LOGIN_VALID_WITH_EXPIRATION, Boolean.class, SecurityUtils
						.getInstance().encryptSHA512(randomToken), login, DBManager.getConfigByNameAsInt(
						"user.service.expire.user.interval.days", 90));
			}
		} catch (Throwable t) {
			logger.error("Problem while checking login", t);
		}
		return false;
	}

	public static String getEncodedPassword(long userId) {
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		try {
			return sjt.queryForObject(SQL_GET_ENCODED_PASSWORD, String.class, userId);
		} catch (Throwable t) {
			logger.error("Problem while checking login", t);
		}
		return null;
	}

	public static UserAttributes getUser(String login, String passwd) {

		logger.info("Trying to login for " + login);
		UserAttributes user = null;
		DBConnection conn = null;

		boolean localMachine = URLMaping.INSTANCE_DIR.startsWith("local");
		
		if (checkCredentials(login, passwd, true)) {
			try {
				conn = ConnectionPool.getInstance().requestConnection();

				String sql = "SELECT user_id, login, 'passwd' passwd, last_name, first_name, middle_name, company, email, a_email, "
						+ "phone, a_phone, icq_number, aol_screen_name, yahoo_messager, waddress, wcity, "
						+ "wstate, wzcode, wzcountry, wcompany, edithimself, gid, last_login, deleted_flag, "
						+ "umessage, last_community, timestamp, pcard_id, wcard_id, dateofbirth, place, paddress, "
						+ "plocation, hphone, mphone, pager, instant_messenger, messenger_number, hcity, hstate, "
						+ "hzipcode, hcountry, comm_id, company_id, agent_id, streetno, streetdirection, streetname, "
						+ "streetsuffix, streetunit, state_id, distribution_mode, distribution_type, address, "
						+ "deliv_templates, deliv_templates_backup, hidden_flag, assign_mode, rowsperpage, "
						+ "search_log, profile_read_only, single_seat, distrib_ats, interactive, outsource  "
						+ "," + UserAttributes.SEND_IMAGES_SURECLOSE + " "
						+ "FROM "
						+ DBConstants.TABLE_USER
						+ " WHERE "
						+ DBConstants.TABLE_USER
						+ "."
						+(localMachine?"login = ? ": (UserAttributes.USER_HIDDEN + "=0  AND login = ? "));
				// DigestUtils.sha512(" ");
				PreparedStatement pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, login);
				DatabaseData data = conn.executePrepQuery(pstmt);
				pstmt.close();

				if (data.getRowNumber() == 0 || data.getRowNumber() > 1)
					return null;
				else {
					user = new UserAttributes(data, 0);
					fillUserRating(user, conn);
				}

			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} finally {
				try {
					ConnectionPool.getInstance().releaseConnection(conn);
				} catch (BaseException e) {
					e.printStackTrace();
				}
			}

		} else {
			System.err.println("checkCredentials:  false");
		}

		return user;
	}

	public static UserRates getUserRates(long userId, long paramCountyId, Date date) {

		UserRates userRates = null;
		// compatibility mode
		String stm = " select " + UserAttributes.USER_RATE_ID + "," + UserAttributes.USER_C2ARATEINDEX + ","
				+ UserAttributes.USER_ATS2CRATEINDEX + "," + "DATE_FORMAT(" + UserAttributes.USER_RATINGFROMDATE
				+ ", '%e-%c-%Y %H:%i:%S')" + " from " + DBConstants.TABLE_USER_RATING + " where "
				+ UserAttributes.USER_RATE_ID + "=" + " (select max(" + UserAttributes.USER_RATE_ID + ") from "
				+ DBConstants.TABLE_USER_RATING + " where " + UserAttributes.USER_ID + "=" + userId + " AND "
				+ " county_id = " + paramCountyId + " and "
				+ UserAttributes.USER_RATINGFROMDATE + "=" + "( select max( " + UserAttributes.USER_RATINGFROMDATE
				+ ")" + " from " + DBConstants.TABLE_USER_RATING + " where " + UserAttributes.USER_ID + "=" + userId
				+ " AND county_id = " + paramCountyId + " AND " + UserAttributes.USER_RATINGFROMDATE + "<="
				+ "STR_TO_DATE( '" + new FormatDate(FormatDate.TIMESTAMP).getDate(date) + "' , '"
				+ FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )" + "))";
		if (logger.isDebugEnabled())
			logger.debug(stm);

		DBConnection conn = null;
		try {

			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(stm);

			if (data.getRowNumber() > 0) {
				userRates = new UserRates();
				userRates.setID(new BigDecimal(data.getValue(1, 0).toString()));

				double c2aRateIndex = 1;
				double a2cRateIndex = 1;

				try {
					c2aRateIndex = Double.parseDouble(data.getValue(2, 0).toString());
				} catch (Exception e) {

				}

				try {
					a2cRateIndex = Double.parseDouble(data.getValue(3, 0).toString());
				} catch (Exception e) {

				}

				userRates.setC2ARATEINDEX(c2aRateIndex);
				userRates.setATS2CRATEINDEX(a2cRateIndex);
				userRates.setFromDate(FormatDate.getDateFromFormatedStringGMT((String) data.getValue(4, 0),
						FormatDate.TIMESTAMP));
			}

			// get the new rates
			stm = " select t1." + UserAttributes.USER_RATE_ID + ", t1." + UserAttributes.USER_C2ARATEINDEX + ","
					+ " t1." + UserAttributes.USER_ATS2CRATEINDEX + "," + "DATE_FORMAT(t1."
					+ UserAttributes.USER_RATINGFROMDATE + ", '%e-%c-%Y %H:%i:%S'), " + " t1.county_id " + " from "
					+ DBConstants.TABLE_USER_RATING + " t1 where t1." + UserAttributes.USER_ID + "=" + " " + userId
					+ " and t1.county_id = " + paramCountyId + "  AND " + " t1." + UserAttributes.USER_RATE_ID + " = "
					+ " (select max(t2." + UserAttributes.USER_RATE_ID + ") from " + DBConstants.TABLE_USER_RATING
					+ " t2 " + " where t2." + UserAttributes.USER_ID + "=" + userId
					+ " AND t2.county_id = t1.county_id )";

			data = conn.executeSQL(stm);

			if (data.getRowNumber() > 0) {
				if (userRates == null) {
					userRates = new UserRates();
				}

				for (int i = 0; i < data.getRowNumber(); i++) {
					BigDecimal payrateId = new BigDecimal(data.getValue(1, i).toString());

					BigDecimal c2aRate = new BigDecimal(1);
					BigDecimal a2cRate = new BigDecimal(1);

					try {
						c2aRate = new BigDecimal(data.getValue(2, i).toString());
					} catch (Exception e) {

					}

					try {
						a2cRate = new BigDecimal(data.getValue(3, i).toString());
					} catch (Exception e) {
						// e.printStackTrace();
					}

					// String dateFrom = (String) data.getValue( 4, i );
					BigDecimal countyId = new BigDecimal(data.getValue(5, i).toString());

					userRates.setIdCounty(payrateId, countyId);
					userRates.addRate(UserAttributes.C2ARATE, c2aRate, countyId);
					userRates.addRate(UserAttributes.A2CRATE, a2cRate, countyId);
				}
			}

		} catch (BaseException e) {
			e.printStackTrace();
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		return userRates;

	}

	/**
	 * The caller must close the connection sent to this method as parameter.
	 */
	public static UserAttributes fillUserRating(UserAttributes ua, DBConnection conn) {

		// old user rates, used for compatibility only
		Date currentdate = new Date(System.currentTimeMillis());
		String stm = " select " + UserAttributes.USER_C2ARATEINDEX + "," + UserAttributes.USER_ATS2CRATEINDEX + ","
				+ "DATE_FORMAT(" + UserAttributes.USER_RATINGFROMDATE + ", '%e-%c-%Y %H:%i:%S')" + " from "
				+ DBConstants.TABLE_USER_RATING + " where " + UserAttributes.USER_RATE_ID + "=" + " (select max("
				+ UserAttributes.USER_RATE_ID + ") from " + DBConstants.TABLE_USER_RATING + " where "
				+ UserAttributes.USER_ID + "=" + ua.getID() + " AND " + UserAttributes.USER_RATINGFROMDATE + "="
				+ "( select max( " + UserAttributes.USER_RATINGFROMDATE + ")" + " from "
				+ DBConstants.TABLE_USER_RATING + " where " + UserAttributes.USER_ID + "=" + ua.getID() + " AND "
				+ UserAttributes.USER_RATINGFROMDATE + "<=" + "STR_TO_DATE( '"
				+ new FormatDate(FormatDate.TIMESTAMP).getDate(currentdate) + "' , '"
				+ FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )" + ")) and county_id = -1";

		try {

			DatabaseData data = conn.executeSQL(stm);

			if (data.getRowNumber() > 0) {
				ua.setC2ARATEINDEX(new BigDecimal(data.getValue(1, 0).toString()));
				ua.setATS2CRATEINDEX(new BigDecimal(data.getValue(2, 0).toString()));
				if (data.getValue(3, 0) != null)
					ua.setRATINGFROMDATE(FormatDate.getDateFromFormatedStringGMT((String) data.getValue(3, 0),
							FormatDate.TIMESTAMP));
				else
					ua.setRATINGFROMDATE(new Date(System.currentTimeMillis()));
			}

		} catch (BaseException e) {
			e.printStackTrace();
		}

		// fill in the new rates
		stm = " select t1." + UserAttributes.USER_C2ARATEINDEX + "," + " t1." + UserAttributes.USER_ATS2CRATEINDEX
				+ "," + "DATE_FORMAT(t1." + UserAttributes.USER_RATINGFROMDATE + ", '%e-%c-%Y %H:%i:%S'), "
				+ " t1.county_id " + " from " + DBConstants.TABLE_USER_RATING + " t1 where t1."
				+ UserAttributes.USER_ID + "=" + " " + ua.getID() + " AND " + " t1." + UserAttributes.USER_RATE_ID
				+ " = " + " (select max(t2." + UserAttributes.USER_RATE_ID + ") from " + DBConstants.TABLE_USER_RATING
				+ " t2 " + " where t2." + UserAttributes.USER_ID + "=" + ua.getID()
				+ " AND t2.county_id = t1.county_id )";

		try {

			DatabaseData data = conn.executeSQL(stm);

			if (data.getRowNumber() > 0) {
				for (int i = 0; i < data.getRowNumber(); i++) {
					BigDecimal c2aRate = new BigDecimal(1);
					BigDecimal a2cRate = new BigDecimal(1);
					try {
						c2aRate = new BigDecimal(data.getValue(1, i).toString());
					} catch (Exception e) {
						// e.printStackTrace();
					}

					try {
						a2cRate = new BigDecimal(data.getValue(2, i).toString());
					} catch (Exception e) {
						// e.printStackTrace();
					}
					// String dateFrom = (String) data.getValue( 3, i );
					BigDecimal countyId = new BigDecimal(data.getValue(4, i).toString());

					ua.addRate(UserAttributes.C2ARATE, c2aRate, countyId);
					ua.addRate(UserAttributes.A2CRATE, a2cRate, countyId);
				}
			}

		} catch (BaseException e) {
			e.printStackTrace();
		}

		return ua;

	}

	public static UserAttributes[] getUsersInGroup(int gid, BigDecimal commId, boolean loadAlloweData) {

		UserAttributes[] user = null;

		DBConnection conn = null;
		try {

			String sql = "select * from " + DBConstants.TABLE_USER + " where " + UserAttributes.USER_GROUP + "=" + gid
					+ " AND " + UserAttributes.USER_COMMID + "=" + commId + " AND " + UserAttributes.USER_HIDDEN
					+ "=0 " + " ORDER BY " + UserAttributes.USER_FIRSTNAME;

			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(sql);

			logger.info("Got " + data.getRowNumber() + " agents.");

			if (data.getRowNumber() == 0)
				return null;
			else {
				user = new UserAttributes[data.getRowNumber()];
				for (int i = 0; i < data.getRowNumber(); i++) {
					user[i] = new UserAttributes(data, i,loadAlloweData);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		return user;
	}

	public static UserAttributes[] getUsersInGroup(int gid, BigDecimal commId, String agency) {

		UserAttributes[] user = null;

		DBConnection conn = null;
		try {

			String sql = "select * from " + DBConstants.TABLE_USER + " where " + UserAttributes.USER_GROUP + "=" + gid
					+ " AND " + UserAttributes.USER_COMMID + "=" + commId + " AND " + UserAttributes.USER_HIDDEN
					+ "=0 " + " AND ? LIKE ucase(left(COMPANY,3)) " + " ORDER BY " + UserAttributes.USER_FIRSTNAME;

			conn = ConnectionPool.getInstance().requestConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, agency);
			DatabaseData data = conn.executePrepQuery(pstmt);
			pstmt.close();

			logger.info("Got " + data.getRowNumber() + " agents.");

			if (data.getRowNumber() == 0)
				return null;
			else {
				user = new UserAttributes[data.getRowNumber()];
				for (int i = 0; i < data.getRowNumber(); i++) {
					user[i] = new UserAttributes(data, i);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		return user;
	}

	public static AgentAttributes getAgentById(BigDecimal id) {

		AgentAttributes agent = null;

		DBConnection conn = null;
		try {

			String sql = "select * from TS_AGENT where " + AgentAttributes.AGENT_ID + "=" + id + " and "
					+ AgentAttributes.AGENT_TYPE + "=" + AgentAttributes.AGNT_TYPE;

			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(sql);

			logger.info("Got " + data.getRowNumber() + " agents.");

			if (data.getRowNumber() == 0)
				return null;
			else {
				agent = new AgentAttributes(data, 0);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		return agent;

	}

	public static UserAttributes getUser(String login, boolean ignoreCase) {
		//SELECT * FROM `table` WHERE LOWER(`Value`) = LOWER("DickSavagewood")
		UserAttributes user = null;
		
		if(ignoreCase){
			login = login.toLowerCase();
		}
		
		DBConnection conn = null;
		try {
			conn = ConnectionPool.getInstance().requestConnection();

			// TODO: do the validation to detect attacks
			String sql = "SELECT * " + "FROM " + DBConstants.TABLE_USER + getUserSettingsJoin() + (ignoreCase?" WHERE LOWER(login) = ?":" WHERE login = ?");
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, login);
			DatabaseData data = conn.executePrepQuery(pstmt);
			pstmt.close();

			// logger.info("Got " + data.getRowNumber() + " users.");
			if (data.getRowNumber() == 0)
				return null;
			else {
				user = new UserAttributes(data, 0);
				fillUserRating(user, conn);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		return user;
	}

	public static UserAttributes getUser(BigDecimal id) {

		UserAttributes user = null;

		DBConnection conn = null;
		try {

			String sql = "SELECT * " + "FROM " + DBConstants.TABLE_USER + getUserSettingsJoin() + " WHERE "
					+ DBConstants.TABLE_USER + "." + UserAttributes.USER_ID + "= '" + id + "' ";
			// logger.info(sql);

			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(sql);

			// logger.info("Got " + data.getRowNumber() + " users.");
			if (data.getRowNumber() == 0)
				return null;
			else {
				user = new UserAttributes(data, 0);
				fillUserRating(user, conn);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		return user;
	}

	public static Company getCompany(UserAttributes user) {
		return getCompany(user.getCOMPANYID());
	}

	public static Company getCompany(BigDecimal companyID) {

		if (companyID == null)
			return null;

		Company company = null;

		DBConnection conn = null;
		try {

			String sql = "select * from " + DBConstants.TABLE_COMPANY + " where ID = " + companyID.intValue();

			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(sql);

			if (data.getRowNumber() == 0)
				return null;
			else {
				company = new Company(data, 0);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		return company;
	}

	public static Company addCompany(Company company) throws DataException, BaseException {

		try {
			SimpleJdbcInsert sji = DBManager.getSimpleJdbcInsert().withTableName(DBConstants.TABLE_COMPANY)
					.usingGeneratedKeyColumns(Company.COMPANY_ID);

			SqlParameterSource parameters = new BeanPropertySqlParameterSource(company);
			Number newId = sji.executeAndReturnKey(parameters);
			company.setAttribute(Company.ID, newId.longValue());
		} catch (Exception e) {
			if (e.getMessage().trim().endsWith(") violated")) {
				throw new DataException("Company already exist");
			} else {
				throw new DataException("SQLException:" + e.getMessage());
			}
		}

		return company;
	}

	/*
	 * verify the first name atributte of the user
	 * 
	 * @param Hashtable hash
	 * 
	 * @param MultipartParameterParser mpp
	 */

	public static void buildParameters(Hashtable hash, MultipartParameterParser mpp, ParameterParser pp)
			throws ParameterNotFoundException {

		BigDecimal user_id = null;
		
		if (mpp != null)
			user_id = mpp.getMultipartBigDecimalParameter(UserAttributes.USER_ID, new BigDecimal(-1));
		else
			user_id = pp.getBigDecimalParameter(UserAttributes.USER_ID, new BigDecimal(-1));

		// logger.info("USER ID" + user_id);
		hash.put(UserAttributes.USER_ID, user_id);
		String user_login = "";

		try {

			if (mpp != null)
				user_login = mpp.getMultipartStringParameter(UserAttributes.USER_LOGIN);
			else
				user_login = pp.getStringParameter(UserAttributes.USER_LOGIN);

			// logger.info("FIRST NAME" + user_login);
			if (user_login.equals("") || StringUtils.isStringBlank(user_login) || user_login.equals("N/A"))
				throw new ParameterNotFoundException("Invalid Login!");

		} catch (ParameterNotFoundException pe) {
			throw new ParameterNotFoundException("Invalid Login!");
		}
		hash.put(UserAttributes.USER_LOGIN, user_login);
		String first_name = "";
		try {
			if (mpp != null)
				first_name = mpp.getMultipartStringParameter(UserAttributes.USER_FIRSTNAME);
			else
				first_name = pp.getStringParameter(UserAttributes.USER_FIRSTNAME);

			if(StringUtils.isEmpty(first_name)) {
				first_name = "";
			}
			/*
			if (first_name.equals("") || StringUtils.isStringBlank(first_name) || first_name.equals(""))
				throw new ParameterNotFoundException("Invalid First Name!");
			*/
		} catch (ParameterNotFoundException pe) {
			//throw new ParameterNotFoundException("Invalid First Name!");
			first_name = "";		
		}
		hash.put(UserAttributes.USER_FIRSTNAME, first_name);

		String last_name = "";
		try {
			if (mpp != null)
				last_name = mpp.getMultipartStringParameter(UserAttributes.USER_LASTNAME);
			else
				last_name = pp.getStringParameter(UserAttributes.USER_LASTNAME);

			if (last_name.equals("") || StringUtils.isStringBlank(last_name) || last_name.equals("N/A"))
				throw new ParameterNotFoundException("Invalid Last Name!");

		} catch (ParameterNotFoundException pe) {
			throw new ParameterNotFoundException("Invalid Last Name!");
		}
		hash.put(UserAttributes.USER_LASTNAME, last_name);
		String phone = "";
		try {
			if (mpp != null)
				phone = mpp.getMultipartStringParameter(UserAttributes.USER_PHONE);
			else
				phone = pp.getStringParameter(UserAttributes.USER_PHONE);
			if (phone.equals("") || StringUtils.isStringBlank(phone))
				throw new ParameterNotFoundException("Invalid Phone Number!");

		} catch (ParameterNotFoundException pe) {
			throw new ParameterNotFoundException("Invalid Phone Number!");
		}
		hash.put(UserAttributes.USER_PHONE, phone);
		String email = "";
		try {
			if (mpp != null)
				email = mpp.getMultipartStringParameter(UserAttributes.USER_EMAIL);
			else
				email = pp.getStringParameter(UserAttributes.USER_EMAIL);
			if (email.equals("") || StringUtils.isStringBlank(email))
				throw new ParameterNotFoundException("Invalid Email Address!");

		} catch (ParameterNotFoundException pe) {
			throw new ParameterNotFoundException("Invalid Email Address!");
		}
		hash.put(UserAttributes.USER_EMAIL, email);
		String company = "";
		try {
			if (mpp != null)
				company = mpp.getMultipartStringParameter(UserAttributes.USER_COMPANY);
			else
				company = pp.getStringParameter(UserAttributes.USER_COMPANY);
			if (company.equals("") || StringUtils.isStringBlank(company) || company.equals("N/A"))
				throw new ParameterNotFoundException("Invalid Company Name!");

		} catch (ParameterNotFoundException pe) {
			throw new ParameterNotFoundException("Invalid Company Name!");
		}
		hash.put(UserAttributes.USER_COMPANY, company.trim());
		String city = "";
		try {
			if (mpp != null)
				city = mpp.getMultipartStringParameter(UserAttributes.USER_WCITY);
			else
				city = pp.getStringParameter(UserAttributes.USER_WCITY);

			if (city.equals("") || StringUtils.isStringBlank(city) || city.equals("N/A"))
				throw new ParameterNotFoundException("Invalid City Name!");

		} catch (ParameterNotFoundException pe) {
			throw new ParameterNotFoundException("Invalid City Name!");
		}
		hash.put(UserAttributes.USER_WCITY, city);

		setPassword(hash, mpp, pp);
		hash.put(UserAttributes.USER_MIDDLENAME, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_MIDDLENAME, "") : pp.getStringParameter(UserAttributes.USER_MIDDLENAME, ""));
		hash.put(UserAttributes.USER_PCARD_ID, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_PCARD_ID, "N/A") : pp.getStringParameter(UserAttributes.USER_PCARD_ID, "N/A"));
		hash.put(UserAttributes.USER_WCARD_ID, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_WCARD_ID, "N/A") : pp.getStringParameter(UserAttributes.USER_WCARD_ID, "N/A"));
		hash.put(UserAttributes.USER_ALTPHONE, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_ALTPHONE, "N/A") : pp.getStringParameter(UserAttributes.USER_ALTPHONE, "N/A"));
		hash.put(UserAttributes.USER_ALTEMAIL, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_ALTEMAIL, "N/A") : pp.getStringParameter(UserAttributes.USER_ALTEMAIL, "N/A"));
		hash.put(UserAttributes.USER_ICQ, (mpp != null) ? mpp.getMultipartStringParameter(UserAttributes.USER_ICQ,
				"N/A") : pp.getStringParameter(UserAttributes.USER_ICQ, "N/A"));
		hash.put(UserAttributes.USER_YAHOO, (mpp != null) ? mpp.getMultipartStringParameter(UserAttributes.USER_YAHOO,
				"N/A") : pp.getStringParameter(UserAttributes.USER_YAHOO, "N/A"));
		hash.put(UserAttributes.USER_AOL, (mpp != null) ? mpp.getMultipartStringParameter(UserAttributes.USER_AOL,
				"N/A") : pp.getStringParameter(UserAttributes.USER_AOL, "N/A"));
		hash.put(UserAttributes.USER_WADDRESS, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_WADDRESS, "N/A") : pp.getStringParameter(UserAttributes.USER_WADDRESS, "N/A"));
		hash.put(UserAttributes.USER_WSTATE, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_WSTATE, "N/A") : pp.getStringParameter(UserAttributes.USER_WSTATE, "N/A"));
		hash.put(UserAttributes.USER_WZCODE, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_WZCODE, "N/A") : pp.getStringParameter(UserAttributes.USER_WZCODE, "N/A"));
		hash.put(UserAttributes.USER_WCOMPANY, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_WCOMPANY, "N/A") : pp.getStringParameter(UserAttributes.USER_WCOMPANY, "N/A"));
		hash.put(UserAttributes.USER_WCOUNTRY, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_WCOUNTRY, "N/A") : pp.getStringParameter(UserAttributes.USER_WCOUNTRY, "N/A"));
		String brthDate = mpp.getMultipartStringParameter(UserAttributes.USER_DATEOFBIRTH);
		int[] datex = CSTCalendar.getInitDateFromString(brthDate, "MDY");
		long datelong = CSTCalendar.getCalendar(datex[2], datex[1], datex[0]).getTimeInMillis();
		hash.put(UserAttributes.USER_DATEOFBIRTH, new Long(datelong));

		hash.put(UserAttributes.USER_PLACE, (mpp != null) ? mpp.getMultipartStringParameter(UserAttributes.USER_PLACE,
				"N/A") : pp.getStringParameter(UserAttributes.USER_PLACE, "N/A"));

		/*
		 * hash.put(UserAttributes.USER_PADDRESS,
		 * (mpp!=null)?mpp.getMultipartStringParameter
		 * (UserAttributes.USER_PADDRESS,"N/A"):
		 * pp.getStringParameter(UserAttributes.USER_PADDRESS,"N/A"));
		 */
		hash.put(UserAttributes.USER_STREETNO, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_STREETNO, "N/A") : pp.getStringParameter(UserAttributes.USER_STREETNO, "N/A"));

		hash.put(UserAttributes.USER_STREETDIRECTION, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_STREETDIRECTION, "N/A") : pp.getStringParameter(
				UserAttributes.USER_STREETDIRECTION, "N/A"));

		hash.put(UserAttributes.USER_STREETNAME, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_STREETNAME, "N/A") : pp.getStringParameter(UserAttributes.USER_STREETNAME, "N/A"));

		hash.put(UserAttributes.USER_STREETSUFFIX, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_STREETSUFFIX, "N/A") : pp.getStringParameter(UserAttributes.USER_STREETSUFFIX,
				"N/A"));

		hash.put(UserAttributes.USER_STREETUNIT, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_STREETUNIT, "N/A") : pp.getStringParameter(UserAttributes.USER_STREETUNIT, "N/A"));

		hash.put(UserAttributes.USER_STATE_ID, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_STATE_ID, "N/A") : pp.getStringParameter(UserAttributes.USER_STATE_ID, "N/A"));

		hash.put(UserAttributes.USER_PLOCATION, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_PLOCATION, "N/A") : pp.getStringParameter(UserAttributes.USER_PLOCATION, "N/A"));
		hash.put(UserAttributes.USER_HPHONE, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_HPHONE, "N/A") : pp.getStringParameter(UserAttributes.USER_HPHONE, "N/A"));
		hash.put(UserAttributes.USER_MPHONE, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_MPHONE, "N/A") : pp.getStringParameter(UserAttributes.USER_MPHONE, "N/A"));
		hash.put(UserAttributes.USER_PAGER, (mpp != null) ? mpp.getMultipartStringParameter(UserAttributes.USER_PAGER,
				"N/A") : pp.getStringParameter(UserAttributes.USER_PAGER, "N/A"));
		hash.put(UserAttributes.USER_INSTANT_MESSENGER, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_INSTANT_MESSENGER, "N/A") : pp.getStringParameter(
				UserAttributes.USER_INSTANT_MESSENGER, "N/A"));
		hash.put(UserAttributes.USER_MESSENGER_NUMBER, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_MESSENGER_NUMBER, "N/A") : pp.getStringParameter(
				UserAttributes.USER_MESSENGER_NUMBER, "N/A"));
		hash.put(UserAttributes.USER_HCITY, (mpp != null) ? mpp.getMultipartStringParameter(UserAttributes.USER_HCITY,
				"N/A") : pp.getStringParameter(UserAttributes.USER_HCITY, "N/A"));
		hash.put(UserAttributes.USER_HSTATE, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_HSTATE, "N/A") : pp.getStringParameter(UserAttributes.USER_HSTATE, "N/A"));
		hash.put(UserAttributes.USER_HZIPCODE, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_HZIPCODE, "N/A") : pp.getStringParameter(UserAttributes.USER_HZIPCODE, "N/A"));
		hash.put(UserAttributes.USER_HCOUNTRY, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_HCOUNTRY, "N/A") : pp.getStringParameter(UserAttributes.USER_HCOUNTRY, "N/A"));

		hash.put(UserAttributes.USER_DISTRIBUTION_TYPE, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_DISTRIBUTION_TYPE, "0") : pp.getStringParameter(
				UserAttributes.USER_DISTRIBUTION_TYPE, "0"));

		hash.put(UserAttributes.USER_ADDRESS, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_ADDRESS, "N/A") : pp.getStringParameter(UserAttributes.USER_ADDRESS, "N/A"));
		hash.put(UserAttributes.USER_C2ARATEINDEX, (mpp != null) ? new BigDecimal(mpp.getMultipartStringParameter(
				UserAttributes.USER_C2ARATEINDEX, "1")) : new BigDecimal(pp.getStringParameter(
				UserAttributes.USER_C2ARATEINDEX, "1")));
		hash.put(UserAttributes.USER_ATS2CRATEINDEX, (mpp != null) ? new BigDecimal(mpp.getMultipartStringParameter(
				UserAttributes.USER_ATS2CRATEINDEX, "1")) : new BigDecimal(pp.getStringParameter(
				UserAttributes.USER_ATS2CRATEINDEX, "1")));

		String ratingDate = mpp.getMultipartStringParameter(UserAttributes.USER_RATINGFROMDATE, "N/A");
		long ratingDateLong = System.currentTimeMillis();
		if (!ratingDate.equals("N/A")) {
			int[] ratingdatex = CSTCalendar.getInitDateFromString(ratingDate, "MDY");
			ratingDateLong = CSTCalendar.getCalendar(ratingdatex[2], ratingdatex[1], ratingdatex[0]).getTimeInMillis();
		}
		hash.put(UserAttributes.USER_RATINGFROMDATE, new Date(ratingDateLong));

		hash.put(UserAttributes.USER_DISTRIBUTION_MODE, (mpp != null) ? mpp.getMultipartStringParameter(
				UserAttributes.USER_DISTRIBUTION_MODE, "1") : pp.getStringParameter(
				UserAttributes.USER_DISTRIBUTION_MODE, "1"));
		
		try {
			String[] templatesArray = mpp.getRequest().getParameterValues(UserAttributes.USER_TEMPLATES);
			if(templatesArray==null) {
				templatesArray = new String[0];
			}
			String templates = (Arrays.toString(templatesArray)).replaceAll("[\\[\\]]", "");
			if("null".equals(templates)) {
				templates = "";
			}
			hash.put(UserAttributes.USER_TEMPLATES, (mpp != null) ? templates : pp.getStringParameter(UserAttributes.USER_TEMPLATES, ""));
		

			Map<Long,String[]> userTemplateProducts = new HashMap<Long, String[]>();
			for(String templateId : templatesArray) {
				templateId = templateId.trim();
				userTemplateProducts.put(Long.parseLong(templateId), mpp.getRequest().getParameterValues(RequestParams.USER_TEMPLATES_PRODUCT+"_"+templateId) );
			}
			
			hash.put(RequestParams.USER_TEMPLATES_PRODUCT, userTemplateProducts);
			
			Map<Long,Integer> userTemplateExportFormat = new HashMap<Long, Integer>();
			for(String templateId : templatesArray) {
				templateId = templateId.trim();
				String exportFormatString = mpp.getRequest().getParameter(RequestParams.USER_TEMPLATES_EXPORT_FORMAT+"_"+templateId);
				int exportFormat = 0;
				if (exportFormatString!=null) {	//template is doc or html, so it has a value for format
					try {
						exportFormat = Integer.parseInt(exportFormatString);
					} catch (NumberFormatException  nfe){}
				}
				userTemplateExportFormat.put(Long.parseLong(templateId), exportFormat );
			}
			
			hash.put(RequestParams.USER_TEMPLATES_EXPORT_FORMAT, userTemplateExportFormat);
		}catch(Exception e) {
			e.printStackTrace();
		}
		// comunity
		hash.put(UserAttributes.USER_COMMID, (mpp != null) ? mpp
				.getMultipartStringParameter(UserAttributes.USER_COMMID) : pp
				.getStringParameter(UserAttributes.USER_COMMID));
		// group
		hash.put(UserAttributes.USER_GROUP, (mpp != null) ? mpp.getMultipartStringParameter(UserAttributes.USER_GROUP)
				: pp.getStringParameter(UserAttributes.USER_GROUP));
		
		hash.put(UserAttributes.USER_PROFILE_READ_ONLY, (mpp != null) ? new Integer(mpp.getMultipartStringParameter(
				UserAttributes.USER_PROFILE_READ_ONLY, "0")) : new Integer(pp.getStringParameter(
				UserAttributes.USER_PROFILE_READ_ONLY, "0")));
		
		hash.put(UserAttributes.SEND_IMAGES_SURECLOSE, (mpp != null) ? new Integer(mpp.getMultipartStringParameter(
				UserAttributes.SEND_IMAGES_SURECLOSE, "0")) : new Integer(pp.getStringParameter(
				UserAttributes.SEND_IMAGES_SURECLOSE, "0")));
		
		hash.put(UserAttributes.SEND_REPORT_SURECLOSE, (mpp != null) ? new Integer(mpp.getMultipartStringParameter(
				UserAttributes.SEND_REPORT_SURECLOSE, "0")) : new Integer(pp.getStringParameter(
				UserAttributes.SEND_REPORT_SURECLOSE, "0")));
		
		/*
		 * erAttributes ua = UserUtils.getUserFromLogin(user_id); UserAttributes
		 * cua = InstanceManager.getCurrentInstance().getCurrentUser();
		 */
		File uPhoto = null;
		if (mpp != null)
			uPhoto = mpp.getFileParameter(UserAttributes.USER_PHOTO, null);
		if (uPhoto != null) {
			hash.put(UserAttributes.USER_PHOTO, uPhoto);
		}
		File uResume = null;
		if (mpp != null)
			uResume = mpp.getFileParameter(UserAttributes.USER_RESUME, null);
		if (uResume != null) {
			hash.put(UserAttributes.USER_RESUME, uResume);
		}

		try {
			hash
					.put(UserAttributes.USER_ASSIGN_MODE, (mpp != null) ? mpp.getMultipartStringParameter(
							UserAttributes.USER_ASSIGN_MODE, "0") : pp.getStringParameter(
							UserAttributes.USER_ASSIGN_MODE, "0"));
		} catch (Exception e) {
			hash.put(UserAttributes.USER_ASSIGN_MODE, "1");
		}

		try {
			hash.put(UserAttributes.USER_SINGLE_SEAT, (mpp != null) ? mpp
					.getMultipartStringParameter(UserAttributes.USER_SINGLE_SEAT) : pp
					.getStringParameter(UserAttributes.USER_SINGLE_SEAT));
		} catch (Exception e) {
			hash.put(UserAttributes.USER_SINGLE_SEAT, "0");
		}

		try {
			hash.put(UserAttributes.USER_DISTRIB_ATS, (mpp != null) ? mpp
					.getMultipartStringParameter(UserAttributes.USER_DISTRIB_ATS) : pp
					.getStringParameter(UserAttributes.USER_DISTRIB_ATS));
		} catch (Exception e) {
			hash.put(UserAttributes.USER_DISTRIB_ATS, "0");
		}

		try {
			hash.put(UserAttributes.USER_DISTRIB_LINK, (mpp != null) ? mpp
					.getMultipartStringParameter(UserAttributes.USER_DISTRIB_LINK) : pp
					.getStringParameter(UserAttributes.USER_DISTRIB_LINK));
		} catch (Exception e) {
			hash.put(UserAttributes.USER_DISTRIB_LINK, "0");
		}

		try {
			hash.put(UserAttributes.USER_INTERACTIVE, (mpp != null) ? mpp
					.getMultipartStringParameter(UserAttributes.USER_INTERACTIVE) : pp
					.getStringParameter(UserAttributes.USER_INTERACTIVE));
		} catch (Exception e) {
			hash.put(UserAttributes.USER_INTERACTIVE, "FALSE");
		}
		
		try {
			hash.put(UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED, (mpp != null) ? mpp
					.getMultipartStringParameter(UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED) : pp
					.getStringParameter(UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED));
		} catch (Exception e) {
			hash.put(UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED, "TRUE");
		}

		try {
			hash.put(UserAttributes.USER_AUTO_UPDATE, (mpp != null) ? mpp
					.getMultipartStringParameter(UserAttributes.USER_AUTO_UPDATE) : pp
					.getStringParameter(UserAttributes.USER_AUTO_UPDATE));
		} catch (Exception e) {
			hash.put(UserAttributes.USER_AUTO_UPDATE, "0");
		}
		
		try {
			hash.put(UserAttributes.USER_OTHER_FILES_ON_SSF, (mpp != null) ? mpp
					.getMultipartStringParameter(UserAttributes.USER_OTHER_FILES_ON_SSF) : pp
					.getStringParameter(UserAttributes.USER_OTHER_FILES_ON_SSF));
		} catch (Exception e) {
			hash.put(UserAttributes.USER_OTHER_FILES_ON_SSF, "0");
		}
		
		try {
			hash.put(UserAttributes.USER_OUTSOURCE, (mpp != null) ? mpp
					.getMultipartStringParameter(UserAttributes.USER_OUTSOURCE) : pp
					.getStringParameter(UserAttributes.USER_OUTSOURCE));
		} catch (Exception e) {
			hash.put(UserAttributes.USER_OUTSOURCE, UserAttributes.OS_DISABLED);
		}
	}

	private static void setNewPassword(Hashtable hash, MultipartParameterParser mpp, ParameterParser pp) throws ParameterNotFoundException{
		String oldPasswd = "";
		String newPassword = "";
		String confirmedPassword = "";
		BigDecimal userId = mpp.getMultipartBigDecimalParameter(UserAttributes.USER_ID, null);
		hash.put(UserAttributes.USER_ID, userId);
		if (mpp != null)
			newPassword = mpp.getMultipartStringParameter(UserAttributes.USER_NEW_PASSWORD,"");
		else
			newPassword = pp.getStringParameter(UserAttributes.USER_NEW_PASSWORD,"");
		if (mpp != null)
			confirmedPassword = mpp.getMultipartStringParameter(UserAttributes.USER_CONFIRMED_PASSWORD, "");
		else
			confirmedPassword = pp.getStringParameter(UserAttributes.USER_CONFIRMED_PASSWORD, oldPasswd);
		
		if (StringUtils.isEmpty(newPassword) && newPassword.length() < 6) {
			throw new ParameterNotFoundException("Password must have at least six characters long!");
		}

		if (newPassword != null && !newPassword.equals("") && confirmedPassword.equals(newPassword)) {
			hash.put(UserAttributes.USER_PASSWORD, newPassword);
		} else if (StringUtils.isNotEmpty( newPassword) &&  !confirmedPassword.equals(newPassword)) {
			throw new ParameterNotFoundException("Passwords do not match!");
		}

	}
	
	private static void setPassword(Hashtable hash, MultipartParameterParser mpp, ParameterParser pp)
			throws ParameterNotFoundException {
		String oldPasswd = "";
		// if(ua!=null)
		// oldPasswd = ua.getPASSWD();
		String password = "";
		if (mpp != null)
			password = mpp.getMultipartStringParameter(UserAttributes.USER_PASSWD, oldPasswd);
		else
			password = pp.getStringParameter(UserAttributes.USER_PASSWD, oldPasswd);
		String cpassword = "";
		if (mpp != null)
			cpassword = mpp.getMultipartStringParameter(UserAttributes.USER_CONFIRMED_PASSWORD, oldPasswd);
		else
			cpassword = pp.getStringParameter(UserAttributes.USER_CONFIRMED_PASSWORD, oldPasswd);
		if (!password.equals("") && password.length() < 6) {
			throw new ParameterNotFoundException("Password must have at least six characters long!");
		}

		if (password != null && !password.equals("") && cpassword.equals(password)) {
			hash.put(UserAttributes.USER_PASSWD, password);
		} else if (password != null && !password.equals("") && !cpassword.equals(password)) {
			throw new ParameterNotFoundException("Passwords do not match!");
		}
	}

	public static String manipulateUser(ParameterParser pp, MultipartParameterParser mpp, int operationType,
			long searchId) throws ParameterNotFoundException, IOException {
		String ret = "OK";
		try {
			Hashtable hash = new Hashtable();
			if (operationType!= TSOpCode.CHANGE_PASSWORD){
				buildParameters(hash, mpp, pp);
			}
			
			UserAttributes currentUser;
			UserAttributes ua = new UserAttributes();
			switch (operationType) {
			case TSOpCode.USER_ADD_APPLY:
			case TSOpCode.USER_ADD_SUBMIT:
				manipulateUser(ua, hash, 1, searchId);
				break;
			case TSOpCode.USER_EDIT_APPLY:
			case TSOpCode.USER_EDIT_SUBMIT:
				manipulateUser(ua, hash, 2, searchId);
				break;
			case TSOpCode.USER_EDIT_DELPHOTO:
				currentUser = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
				if (UserUtils.isTSAdmin(currentUser)
						|| UserUtils.isTSCAdmin(currentUser)
						|| (0 == currentUser.getID().compareTo((BigDecimal) hash.get(UserAttributes.USER_ID)))
						|| (UserUtils.isCommAdmin(currentUser) && currentUser.getCOMMID().compareTo(
								new BigDecimal((String) hash.get(UserAttributes.USER_COMMID))) == 0)) {
					manipulateUser(ua, hash, TSOpCode.USER_EDIT_DELPHOTO, searchId);
				}
				break;
			case TSOpCode.USER_EDIT_DELRESUME:
				currentUser = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
				if (UserUtils.isTSAdmin(currentUser)
						|| UserUtils.isTSCAdmin(currentUser)
						|| (0 == currentUser.getID().compareTo((BigDecimal) hash.get(UserAttributes.USER_ID)))
						|| (UserUtils.isCommAdmin(currentUser) && currentUser.getCOMMID().compareTo(
								new BigDecimal((String) hash.get(UserAttributes.USER_COMMID))) == 0)) {
					manipulateUser(ua, hash, TSOpCode.USER_EDIT_DELRESUME, searchId);
				}
				break;
			case TSOpCode.CHANGE_PASSWORD:
				setNewPassword(hash, mpp, pp);
				ua.setPASSWD((String) hash.get(UserAttributes.USER_PASSWORD));
				ua.setID((BigDecimal) hash.get(UserAttributes.USER_ID));
				DataAttribute userWithNewPassword = ua;
				UserAttributes userWithOldPassword = getUser(ua.getID());
				if (!StringUtils.isEmpty((String) userWithNewPassword.getAttribute(UserAttributes.PASSWD))) {
					if (UserManager.checkCredentials((String) userWithOldPassword.getAttribute(UserAttributes.LOGIN),
							(String) userWithNewPassword.getAttribute(UserAttributes.PASSWD), false)
							|| !PasswordGenerator.validatePassword((String) userWithNewPassword
									.getAttribute(UserAttributes.PASSWD), DBManager.getConfigByNameAsInt(
									"password.random.length", 6))) {
						ret = "Invalid password! Your password must be different from the current one, contain at least a digit and an upper case letter and be at least "
								+ DBManager.getConfigByNameAsInt("password.random.length", 6)
								+ " characters in length!";
					}
					updatePasswordForUser(ua, searchId);
				}
				break;
			}
		} catch (Exception e) {
			ret = e.getMessage();
			e.printStackTrace();
			return ret;
		}

		try {
		} catch (Exception e) {
		}
		return ret;
	}

	public static UserAttributes manipulateUser(UserAttributes ua, Hashtable hash, int operationType, long searchId)
			throws DataException, BaseException {

		if (operationType != 1) {
			// check if that user his the right to update his profile
			if ((!(UserUtils.isCommAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
					|| UserUtils.isTSCAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser()) || UserUtils
					.isTSAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())))
					&& UserUtils.getUserFromId((BigDecimal) hash.get(UserAttributes.USER_ID)).getPROFILE_READ_ONLY() == 1) {
				throw new BaseException("User profile is read-only!");
			}
		}
		
		UserAttributes old = UserUtils.getUserFromId((BigDecimal) hash.get(UserAttributes.USER_ID));
		
		//task 8330
		ua.setLastModified(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getID().intValue());

		ua.setID((BigDecimal) hash.get(UserAttributes.USER_ID));
		ua.setLOGIN((String) hash.get(UserAttributes.USER_LOGIN));
		ua.setPASSWD((String) hash.get(UserAttributes.USER_PASSWD));
		ua.setFIRSTNAME((String) hash.get(UserAttributes.USER_FIRSTNAME));
		ua.setLASTNAME((String) hash.get(UserAttributes.USER_LASTNAME));
		ua.setMIDDLENAME((String) hash.get(UserAttributes.USER_MIDDLENAME));
		ua.setEMAIL((String) hash.get(UserAttributes.USER_EMAIL));
		ua.setCOMMID(new BigDecimal((String) hash.get(UserAttributes.USER_COMMID)));
		ua.setCOMPANYID(new BigDecimal(1));
		
		ua.setGROUP(new BigDecimal((String) hash.get(UserAttributes.USER_GROUP)));
		String company = (String) hash.get(UserAttributes.USER_COMPANY);
		if(company != null) {
			company = company.trim();
		}
		ua.setCOMPANY(company);
		ua.setALTEMAIL((String) hash.get(UserAttributes.USER_ALTEMAIL));
		ua.setPHONE((String) hash.get(UserAttributes.USER_PHONE));
		ua.setALTPHONE((String) hash.get(UserAttributes.USER_ALTPHONE));
		ua.setICQ((String) hash.get(UserAttributes.USER_ICQ));
		ua.setAOL((String) hash.get(UserAttributes.USER_AOL));
		ua.setYAHOO((String) hash.get(UserAttributes.USER_YAHOO));

		ua.setWADDRESS((String) hash.get(UserAttributes.USER_WADDRESS));
		ua.setWCITY((String) hash.get(UserAttributes.USER_WCITY));
		ua.setWSTATE((String) hash.get(UserAttributes.USER_WSTATE));
		ua.setWZCODE((String) hash.get(UserAttributes.USER_WZCODE));
		ua.setWCONTRY((String) hash.get(UserAttributes.USER_WCOUNTRY));
		ua.setWCOMPANY((String) hash.get(UserAttributes.USER_WCOMPANY));

		ua.setLASTLOGIN(new BigDecimal(System.currentTimeMillis()));

		// ua.setRESUME(hash.get(UserAttributes.USER_RESUME ));
		// ua.setPHOTO(hash.get( UserAttributes.USER_));
		// new attributes
		ua.setPCARD_ID((String) hash.get(UserAttributes.USER_PCARD_ID)); // personal
																			// card
																			// id
		ua.setWCARD_ID((String) hash.get(UserAttributes.USER_WCARD_ID)); // work
																			// card
																			// id
		ua.setDATEOFBIRTH((Long) hash.get(UserAttributes.USER_DATEOFBIRTH));
		ua.setPLACE((String) hash.get(UserAttributes.USER_PLACE));
		// ua.setPADDRESS((String)hash.get( UserAttributes.USER_PADDRESS));
		// //old version without agents
		ua.setADDRESS((String) hash.get(UserAttributes.USER_ADDRESS));
		ua.setC2ARATEINDEX((BigDecimal) (hash.get(UserAttributes.USER_C2ARATEINDEX)));
		ua.setATS2CRATEINDEX((BigDecimal) (hash.get(UserAttributes.USER_ATS2CRATEINDEX)));
		ua.setRATINGFROMDATE((Date) hash.get(UserAttributes.USER_RATINGFROMDATE));
		ua.setSTREETNO((String) hash.get(UserAttributes.USER_STREETNO));
		ua.setSTREETDIRECTION((String) hash.get(UserAttributes.USER_STREETDIRECTION));
		ua.setSTREETNAME((String) hash.get(UserAttributes.USER_STREETNAME));
		ua.setSTREETSUFFIX((String) hash.get(UserAttributes.USER_STREETSUFFIX));
		ua.setSTREETUNIT((String) hash.get(UserAttributes.USER_STREETUNIT));
		ua.setSTATE_ID((String) hash.get(UserAttributes.USER_STATE_ID));

		ua.setPLOCATION((String) hash.get(UserAttributes.USER_PLOCATION));
		ua.setHPHONE((String) hash.get(UserAttributes.USER_HPHONE));
		ua.setMPHONE((String) hash.get(UserAttributes.USER_MPHONE));
		ua.setPAGER((String) hash.get(UserAttributes.USER_PAGER));
		ua.setINSTANT_MESSENGER((String) hash.get(UserAttributes.USER_INSTANT_MESSENGER));
		ua.setMESSENGER_NUMBER((String) hash.get(UserAttributes.USER_MESSENGER_NUMBER));
		ua.setHCITY((String) hash.get(UserAttributes.USER_HCITY));
		ua.setHSTATE((String) hash.get(UserAttributes.USER_HSTATE));
		ua.setHZIPCODE((String) hash.get(UserAttributes.USER_HZIPCODE));
		ua.setHCOUNTRY((String) hash.get(UserAttributes.USER_HCOUNTRY));

		ua.setDISTRIBUTION_TYPE((String) hash.get(UserAttributes.USER_DISTRIBUTION_TYPE));
		ua.setDISTRIBUTION_MODE((String) hash.get(UserAttributes.USER_DISTRIBUTION_MODE));
		ua.setTEMPLATES((String) hash.get(UserAttributes.USER_TEMPLATES));

		ua.setDELETED(new BigDecimal(0));
		File resume = (File) hash.get(UserAttributes.USER_RESUME);
		File photo = (File) hash.get(UserAttributes.USER_PHOTO);

		ua.setASSIGN_MODE((String) hash.get(UserAttributes.USER_ASSIGN_MODE));
		ua.setSINGLE_SEAT((String) hash.get(UserAttributes.USER_SINGLE_SEAT));
		ua.setDISTRIB_ATS((String) hash.get(UserAttributes.USER_DISTRIB_ATS));
		ua.setDISTRIB_LINK((String) hash.get(UserAttributes.USER_DISTRIB_LINK));
		ua.setPROFILE_READ_ONLY((Integer) hash.get(UserAttributes.USER_PROFILE_READ_ONLY));
		ua.setSEND_IMAGES_FORCLOSURE((Integer) hash.get(UserAttributes.SEND_IMAGES_SURECLOSE));
		ua.setSEND_REPORT_FORCLOSURE((Integer) hash.get(UserAttributes.SEND_REPORT_SURECLOSE));
		ua.setINTERACTIVE(Boolean.parseBoolean((String) hash.get(UserAttributes.USER_INTERACTIVE)));
		ua.setAUTO_ASSIGN_SEARCH_LOCKED(Boolean.parseBoolean((String) hash.get(UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED)));
		ua.setAUTO_UPDATE((String) hash.get(UserAttributes.USER_AUTO_UPDATE));
		ua.setOTHER_FILES_ON_SSF((String) hash.get(UserAttributes.USER_OTHER_FILES_ON_SSF));
		ua.setOUTSOURCE((String) hash.get(UserAttributes.USER_OUTSOURCE));
		switch (operationType) {
		case 1:
			return addUser(ua, resume, photo, searchId,hash);
		case TSOpCode.USER_EDIT_DELPHOTO:
			return deleteUserPhoto(ua);
		case TSOpCode.USER_EDIT_DELRESUME:
			return deleteUserResume(ua);
		case TSOpCode.CHANGE_PASSWORD:
			return updatePasswordForUser(ua, searchId);
		default:
			return updateUser(ua, old, resume, photo, searchId,hash);
		}
	}

	public static UserAttributes addUser(UserAttributes ua, File resume, File photo, long searchId)
	throws DataException, BaseException {
		return addUser(ua, resume, photo, searchId,null);
	}
	public static UserAttributes addUser(UserAttributes ua, File resume, File photo, long searchId,Map userSubmittedData)
			throws DataException, BaseException {

		String stm = "INSERT INTO " + DBConstants.TABLE_USER + "(" + UserAttributes.USER_LASTNAME + ", "
				+ UserAttributes.USER_FIRSTNAME + ", " + UserAttributes.USER_MIDDLENAME + ", "
				+ UserAttributes.USER_LOGIN + ", " + UserAttributes.USER_PASSWD + ", " + UserAttributes.USER_COMPANY
				+ ", " + UserAttributes.USER_EMAIL + ", " + UserAttributes.USER_ALTEMAIL + ", "
				+ UserAttributes.USER_PHONE + ", " + UserAttributes.USER_ALTPHONE + ", " + UserAttributes.USER_ICQ
				+ ", " + UserAttributes.USER_AOL + ", " + UserAttributes.USER_YAHOO + ", "
				+ UserAttributes.USER_WADDRESS + ", " + UserAttributes.USER_WCITY + ", " + UserAttributes.USER_WSTATE
				+ ", " + UserAttributes.USER_WZCODE + ", " + UserAttributes.USER_WCOUNTRY
				+ ", "
				+ UserAttributes.USER_WCOMPANY
				+ ", "
				+ UserAttributes.USER_EDITEDBY
				+ ", "
				+ UserAttributes.USER_GROUP
				+ ", "
				+ UserAttributes.USER_LASTLOGIN
				+ ", "
				+ UserAttributes.USER_DELETED
				+ ", "
				+ UserAttributes.USER_UMESSAGES
				+ ", "
				+ UserAttributes.USER_LASTCOMM
				+ ", "
				+ UserAttributes.USER_PCARD_ID
				+ ", "
				+ UserAttributes.USER_WCARD_ID
				+ ", "
				+ UserAttributes.USER_DATEOFBIRTH
				+ ", "
				+ UserAttributes.USER_PLACE
				+ ", "
				// + UserAttributes.USER_PADDRESS
				+ UserAttributes.USER_STREETNO + ", " + UserAttributes.USER_STREETDIRECTION + ", "
				+ UserAttributes.USER_STREETNAME + ", " + UserAttributes.USER_STREETSUFFIX + ", "
				+ UserAttributes.USER_STREETUNIT + ", " + UserAttributes.USER_STATE_ID + ", "
				+ UserAttributes.USER_PLOCATION + ", " + UserAttributes.USER_HPHONE + ", " + UserAttributes.USER_MPHONE
				+ ", " + UserAttributes.USER_PAGER + ", " + UserAttributes.USER_INSTANT_MESSENGER + ", "
				+ UserAttributes.USER_MESSENGER_NUMBER + ", " + UserAttributes.USER_HCITY + ", "
				+ UserAttributes.USER_HSTATE + ", " + UserAttributes.USER_HZIPCODE + ", "
				+ UserAttributes.USER_HCOUNTRY + ", " + UserAttributes.USER_COMMID + ", "
				+ UserAttributes.USER_COMPANYID + ", " + UserAttributes.USER_DISTRIBUTION_TYPE + ", "
				+ UserAttributes.USER_DISTRIBUTION_MODE + ", " + UserAttributes.USER_ADDRESS + ", "
				+ UserAttributes.USER_ASSIGN_MODE + ", " + UserAttributes.USER_PROFILE_READ_ONLY + ", "
				+ UserAttributes.USER_SINGLE_SEAT + ", " + UserAttributes.USER_DISTRIB_ATS + ", "
				+ UserAttributes.USER_DISTRIB_LINK 
				+ ", " + UserAttributes.USER_INTERACTIVE 
				+ ", " + UserAttributes.USER_OUTSOURCE 
				+ ", " + UserAttributes.USER_RANDOM_TOKEN 
				+ ", " + UserAttributes.USER_PASSWORD 
				+ ", " + DBConstants.FIELD_USER_LAST_PASSWORD_CHANGE_DATE
				+ ", " + UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED
				+ ", " + UserAttributes.USER_AUTO_UPDATE
				+ ", " + UserAttributes.USER_OTHER_FILES_ON_SSF
				+ ", " + UserAttributes.SEND_IMAGES_SURECLOSE
				+ ", " + UserAttributes.SEND_REPORT_SURECLOSE
				+ ", " + UserAttributes.USER_MODIFIED_BY
				+ ", " + UserAttributes.USER_DATE_MODIFIED
				+ " ) VALUES ( " + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?,"
				+ "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?,"
				+ "?," + "?," + "?," + "?,"
				+ "?,"
				// + ua.getAttribute(UserAttributes.PADDRESS)
				+ "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?,"
				+ "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?," + "?,"
				+ " now(), ?, ?, ?, ?, ?, ?, ? "
				// + ","
				// + ua.getC2ARATEINDEX()
				// + ","
				// + ua.getATS2CRATEINDEX()
				+ ")";

		DBConnection conn = null;
		PreparedStatement pStmt = null;
		try {

			// searchId is -1 in case it is called from web services
			if (searchId != -1) {
				if (!(UserUtils.isCommAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
						|| UserUtils.isTSCAdmin(InstanceManager.getManager().getCurrentInstance(searchId)
								.getCurrentUser()) || UserUtils.isTSAdmin(InstanceManager.getManager()
						.getCurrentInstance(searchId).getCurrentUser()))) {
					throw new BaseException("Insufficient priviledges");
				}
				if (!PasswordGenerator.validatePassword((String) ua.getAttribute(UserAttributes.PASSWD), DBManager
						.getConfigByNameAsInt("password.random.length", 6))) {
					throw new BaseException(
							"Invalid password! Your password must contain at least a digit and an upper case letter and be at least "
									+ DBManager.getConfigByNameAsInt("password.random.length", 6)
									+ " characters in length!");
				}
			}

			conn = ConnectionPool.getInstance().requestConnection();

			PreparedStatement pstmt = null;

			String tempSql = "SELECT COUNT(*) from " + DBConstants.TABLE_USER + " where "
					+ DBConstants.FIELD_USER_LOGIN + " =? ";

			if (DBManager.getSimpleTemplate().queryForInt(tempSql, ua.getAttribute(UserAttributes.LOGIN)) > 0)
				throw new DataException("Duplicate entry: User already exists!");
			
			if (!ua.getAttribute(UserAttributes.LOGIN).toString().matches("^[\\w\\.@]+$")) {// 9720
				throw new BaseException("Invalid Username! Allowed characters are letters, digits, \'.\', \'_\' and \'@\' .\n");
			}

			pstmt = conn.prepareStatement(stm);
			int k = 1;

			try {
				pstmt.setString(k++, ua.getAttribute(UserAttributes.LASTNAME).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.FIRSTNAME).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.MIDDLENAME).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.LOGIN).toString());
				pstmt.setString(k++, "");
				pstmt.setString(k++, ua.getAttribute(UserAttributes.COMPANY).toString().trim());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.EMAIL).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.ALTEMAIL).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.PHONE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.ALTPHONE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.ICQ).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.AOL).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.YAHOO).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WADDRESS).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WCITY).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WSTATE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WZCODE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WCONTRY).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WCOMPANY).toString());
				// pstmt.setString( k++ ,
				// ua.getAttribute(UserAttributes.EDITEDBY).toString() );
				pstmt.setNull(k++, java.sql.Types.VARCHAR);
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.GROUP), java.sql.Types.BIGINT);
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.LASTLOGIN), java.sql.Types.BIGINT);
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.DELETED), java.sql.Types.BIGINT);
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.UMESSAGES), java.sql.Types.BIGINT);
				pstmt.setLong(k++, 0);
				pstmt.setString(k++, ua.getAttribute(UserAttributes.PCARD_ID).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WCARD_ID).toString());
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.DATEOFBIRTH), java.sql.Types.BIGINT);
				pstmt.setString(k++, ua.getAttribute(UserAttributes.PLACE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.STREETNO).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.STREETDIRECTION).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.STREETNAME).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.STREETSUFFIX).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.STREETUNIT).toString());
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.STATE_ID), java.sql.Types.BIGINT);
				pstmt.setString(k++, ua.getAttribute(UserAttributes.PLOCATION).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.HPHONE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.MPHONE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.PAGER).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.INSTANT_MESSENGER).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.MESSENGER_NUMBER).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.HCITY).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.HSTATE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.HZIPCODE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.HCOUNTRY).toString());
				pstmt.setObject(k++, ua.getCOMMID(), java.sql.Types.BIGINT);
				pstmt.setObject(k++, ua.getCOMPANYID(), java.sql.Types.BIGINT);
				pstmt.setString(k++, ua.getDISTRIBUTION_TYPE().toString());
				pstmt.setString(k++, ua.getDISTRIBUTION_MODE().toString());
				pstmt.setString(k++, ua.getADDRESS().toString());
				pstmt.setInt(k++, ua.getASSIGN_MODE());
				pstmt.setInt(k++, ua.getPROFILE_READ_ONLY());
				pstmt.setInt(k++, ua.getSINGLE_SEAT());
				pstmt.setInt(k++, ua.getDISTRIB_ATS());
				pstmt.setInt(k++, ua.getDISTRIB_LINK());
				pstmt.setBoolean(k++, ua.isINTERACTIVE());
				pstmt.setString(k++, ua.getOUTSOURCE());
				String timeStamp = String.valueOf(System.currentTimeMillis());
				String random_token = StringUtils.intercalateCharacters(new String[] {
						ua.getAttribute(UserAttributes.LOGIN).toString(), timeStamp,
						ua.getAttribute(UserAttributes.LASTNAME).toString() });
				String password = StringUtils.intercalateCharacters(new String[] {
						ua.getAttribute(UserAttributes.PASSWD).toString(), random_token });

				pstmt.setString(k++, random_token);
				pstmt.setString(k++, SecurityUtils.getInstance().encryptSHA512(password));
				pstmt.setBoolean(k++, ua.isAUTO_ASSIGN_SEARCH_LOCKED());
				pstmt.setInt(k++, ua.getAUTO_UPDATE());
				pstmt.setInt(k++, ua.getOTHER_FILES_ON_SSF());
				pstmt.setInt(k++, ua.getSEND_IMAGES_FORCLOSURE());
				pstmt.setInt(k++, ua.getSEND_REPORT_FORCLOSURE());
				pstmt.setInt(k++, ua.getMODIFIED_BY());
				pstmt.setTimestamp(k++, new Timestamp(ua.getDATE_MODIFIED().getTime()));
			} catch (NullPointerException e) {
			}

			pstmt.executeUpdate();
			conn.commit();
			pstmt.close();

			long uid = DBManager.getLastId(conn);

			ua.setAttribute(UserAttributes.ID, new Long(uid));

			try {
				
				Map<Long,String[]> userTemplatesProductEnable  = new HashMap<Long, String[]>();
				if(userSubmittedData!=null) {
					userTemplatesProductEnable = (Map<Long, String[]>) userSubmittedData.get(RequestParams.USER_TEMPLATES_PRODUCT);
				}
				Map<Long,Integer> userTemplatesExportFormat  = new HashMap<Long, Integer>();
				if(userSubmittedData!=null) {
					userTemplatesExportFormat = (Map<Long, Integer>) userSubmittedData.get(RequestParams.USER_TEMPLATES_EXPORT_FORMAT);
				}
				// deletes existing records for this user
				String tDelQuery = "DELETE FROM " + DBConstants.TABLE_USER_TEMPLATES + " WHERE "
						+ DBConstants.FIELD_USER_TEMPLATES_USER_ID + "=" + uid;
				conn.executeSQL(tDelQuery);

				// insert new added templates
				String templateList = ua.getAttribute(UserAttributes.TEMPLATES, DataAttribute.ORA_ESCAPED).toString();
				String[] templates = null;
				boolean hasTemplates = false;
				if (!templateList.equals(""))
					hasTemplates = true;

				// if there are any templates selected by the user, add them
				// into database
				if (hasTemplates) {
					templates = templateList.split(UserAttributes.USER_TEMPLATES_DELIM);
					String tInsQuery = "INSERT INTO " + DBConstants.TABLE_USER_TEMPLATES + "("
							+ DBConstants.FIELD_USER_TEMPLATES_TEMPLATE_ID + ","
							+ DBConstants.FIELD_USER_TEMPLATES_USER_ID + "," + DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT + ","
							+ DBConstants.FIELD_USER_TEMPLATES_EXPORT_FORMAT + ")" + " VALUES ";

					for (int i = 0; i < templates.length; i++) {
						long enableProduct = UserUtils.calculateTemplateEnabledOnProduct(userTemplatesProductEnable.get(Long.parseLong(templates[i].trim())));
						Integer expFor = userTemplatesExportFormat.get(Long.parseLong(templates[i].trim()));
						int exportFormat = 0;
						if (expFor!=null) {
							exportFormat = expFor.intValue();
						}
						tInsQuery += "(" + Long.parseLong(templates[i].trim()) + "," + uid + "," + enableProduct + "," + exportFormat + "),";
					}
					// get string without last comma
					tInsQuery = tInsQuery.substring(0, tInsQuery.length() - 1);
					conn.executeSQL(tInsQuery);
						
					}
				}catch(Throwable t) {
					t.printStackTrace();
				}

			if (photo != null) {
				stm = "INSERT INTO " + DBConstants.TABLE_USER_PHOTO + " ( " + UserAttributes.USER_ID + ","
						+ UserAttributes.USER_PHOTO + " ) " + " VALUES ( ? , ? )";

				FileInputStream fis = new FileInputStream(photo);
				pStmt = conn.prepareStatement(stm);
				pStmt.setLong(1, uid);
				pStmt.setBinaryStream(2, fis, (int) photo.length());
				pStmt.executeUpdate();
				pStmt.close();
				if (fis != null)
					fis.close();
				conn.commit();

				if (photo.delete() == true) {
					logger.info("File Photo " + photo.getName() + " succesufully deleted from WEB SERVER!");
				} else {
					logger.info("Cannot delete File Photo " + photo.getName() + " from WEB SERVER!");
				}
			}

			if (resume != null) {
				String resume_name = resume.getName();
				long resume_size = resume.length();
				stm = "INSERT INTO " + DBConstants.TABLE_USER_RESUME + " VALUES(?,?,?,?,?)";
				pStmt = conn.prepareStatement(stm);
				FileInputStream fis = new FileInputStream(resume);
				pStmt.setLong(1, uid);
				pStmt.setString(2, resume_name);
				pStmt.setLong(3, resume_size);
				pStmt.setBinaryStream(4, fis, (int) resume_size);
				pStmt.setDate(5, null);
				pStmt.executeUpdate(stm);
				pStmt.close();
				if (fis != null)
					fis.close();
				conn.commit();
				if (resume.delete() == true) {
					logger.info("File " + resume.getName() + " succesufully deleted from WEB SERVER!");
				} else {
					logger.info("Cannot delete File " + resume.getName() + " from WEB SERVER!");
				}

			}
			stm = "INSERT INTO " + DBConstants.TABLE_USER_RATING + "(" + UserAttributes.USER_ID + ","
					+ UserAttributes.USER_RATINGFROMDATE + "," + UserAttributes.USER_C2ARATEINDEX + ","
					+ UserAttributes.USER_ATS2CRATEINDEX + ")" + " VALUES(?," + getDatabaseRatingDate(ua) + ","
					+ "?, ?" + ")";

			DBManager.getSimpleTemplate().update(stm, (Long) ua.getAttribute(UserAttributes.ID),
					ua.getAttribute(UserAttributes.C2ARATEINDEX), ua.getAttribute(UserAttributes.ATS2CRATEINDEX));
		} catch (Exception e) {
			e.printStackTrace();
			if (e.getMessage().contains("Duplicate entry")) {

				throw new DataException("User already exists!");

			} else {
				throw new DataException("SQLException:" + e.getMessage());
			}
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		UserManagerI userManagerI = com.stewart.ats.user.UserManager.getInstance();
		try {
			userManagerI.getAccess();
			userManagerI.refreshInfo(ua.getID().longValue());
		} catch (Throwable t) {
			logger.error("Error while trying to update cache information for user: " + ua.getID().longValue(), t);
		} finally {
			if (userManagerI != null) {
				userManagerI.releaseAccess();
			}
		}
		
		if (UserUtils.isTSAdmin(ua)) {
			EmailClient email = new EmailClient();
			email.addTo(MailConfig.getAddressTSAdminChanged());
			email.setSubject("Title Search Administrator user created on server " + URLMaping.INSTANCE_DIR);
			email.addContent(ua.getUserFullName() + " (" + ua.getLOGIN() + ") with Title Search Administrator role created on server " + 
				URLMaping.INSTANCE_DIR + " by " + ua.getLastModified() + ".");
			email.sendAsynchronous();
		}

		return ua;
	}

	public static void updateUserLite(UserAttributes ua) throws DataException, BaseException {

		String stm = "UPDATE " + DBConstants.TABLE_USER + " SET " + UserAttributes.USER_LASTNAME + "=?,"
				+ UserAttributes.USER_FIRSTNAME + "=?," + UserAttributes.USER_MIDDLENAME + "=?,"
				+ UserAttributes.USER_EMAIL + "=?," + UserAttributes.USER_PHONE + "=?," + UserAttributes.USER_WADDRESS
				+ "=?," + UserAttributes.USER_WCITY + "=?," + UserAttributes.USER_WSTATE + "=?,"
				+ UserAttributes.USER_WZCODE + "=?" + " WHERE " + UserAttributes.USER_ID + "=?";

		DBConnection conn = null;
		try {
			int k = 1;
			logger.info(stm);

			conn = ConnectionPool.getInstance().requestConnection();

			PreparedStatement pstmt = conn.prepareStatement(stm);

			pstmt.setString(k++, ua.getAttribute(UserAttributes.LASTNAME).toString());
			pstmt.setString(k++, ua.getAttribute(UserAttributes.FIRSTNAME).toString());
			pstmt.setString(k++, ua.getAttribute(UserAttributes.MIDDLENAME).toString());
			pstmt.setString(k++, ua.getAttribute(UserAttributes.EMAIL).toString());
			pstmt.setString(k++, ua.getAttribute(UserAttributes.PHONE).toString());
			pstmt.setString(k++, ua.getAttribute(UserAttributes.WADDRESS).toString());
			pstmt.setString(k++, ua.getAttribute(UserAttributes.WCITY).toString());
			pstmt.setString(k++, ua.getAttribute(UserAttributes.WSTATE).toString());
			pstmt.setString(k++, ua.getAttribute(UserAttributes.WZCODE).toString());
			pstmt.setObject(k++, ua.getAttribute(UserAttributes.ID), java.sql.Types.BIGINT);

			pstmt.executeUpdate();
			pstmt.close();

			conn.commit();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}
	}

	protected static Object transformNull(Object obj) {
		return (obj != null ? obj : "");
	}

	public static UserAttributes updatePasswordForUser(UserAttributes userWithNewPassword, long searchId)
			throws BaseException {
		BigDecimal uid = userWithNewPassword.getID();

		String stm = "UPDATE " + DBConstants.TABLE_USER + " SET " + UserAttributes.USER_RANDOM_TOKEN // 1
				+ "=" + "? " + ", " + UserAttributes.USER_PASSWORD // 2
				+ "=" + "? " + ", " + DBConstants.FIELD_USER_NOTIFICATION_EXPIRE_PASS_SENT // 3
				+ "=" + "? " + ", " + DBConstants.FIELD_USER_INTERACTIVE // 4
				+ "=" + "? " + ", " + DBConstants.FIELD_USER_HIDDEN // 5
				+ "=" + "? " + ", " + DBConstants.FIELD_USER_LAST_PASSWORD_CHANGE_DATE // 
				+ "=" + " now() "
				// }
				+ " WHERE " + UserAttributes.USER_ID // 6
				+ "=" + "? " + " ";
		UserAttributes userWithOldPassword = getUser(uid);
		Object[] args = null;
		String timeStamp = String.valueOf(System.currentTimeMillis());
		String random_token = StringUtils.intercalateCharacters(new String[] {
				userWithOldPassword.getAttribute(UserAttributes.LOGIN).toString(), timeStamp,
				userWithOldPassword.getAttribute(UserAttributes.LASTNAME).toString() });
		String password = StringUtils.intercalateCharacters(new String[] {
				userWithNewPassword.getAttribute(UserAttributes.PASSWD).toString(), random_token });

		String encryptSHA512 = SecurityUtils.getInstance().encryptSHA512(password);
		args = new Object[] { random_token, //UserAttributes.USER_RANDOM_TOKEN
							  encryptSHA512, //UserAttributes.USER_PASSWORD
							  "0", //DBConstants.FIELD_USER_NOTIFICATION_EXPIRE_PASS_SENT
							  "1", //DBConstants.FIELD_USER_INTERACTIVE
							  "0", //DBConstants.FIELD_USER_HIDDEN
							  userWithNewPassword.getID()
							  };

		DBManager.getSimpleTemplate().update(stm, args);

		return userWithNewPassword;
	}

	public static UserAttributes updateUser(UserAttributes ua, long searchId) throws DataException, BaseException {
		return updateUser(ua, ua, null, null, searchId);
	}

	public static UserAttributes updateUser(UserAttributes ua, UserAttributes old, File resume, File photo, long searchId)
	throws DataException, BaseException {
		return updateUser(ua,old,resume,photo,searchId,null);		
	}
	
	public static UserAttributes updateUser(UserAttributes ua, UserAttributes old, File resume, File photo, long searchId, Map userSubmittedData)
			throws DataException, BaseException {

		BigDecimal uid = ua.getID();

		// TODO: do the validation to detect attacks
		String stm = "UPDATE " + DBConstants.TABLE_USER + " SET " + UserAttributes.USER_LASTNAME + "=" + "?" + ", "
				+ UserAttributes.USER_FIRSTNAME + "=" + "?" + ", " + UserAttributes.USER_MIDDLENAME + "=" + "?" + ", "
				+ UserAttributes.USER_COMPANY + "=" + "?" + ", " + UserAttributes.USER_EMAIL + "=" + "?" + ", "
				+ UserAttributes.USER_ALTEMAIL + "=" + "?" + ", " + UserAttributes.USER_PHONE + "=" + "?" + ", "
				+ UserAttributes.USER_ALTPHONE + "=" + "?" + ", " + UserAttributes.USER_ICQ + "=" + "?" + ", "
				+ UserAttributes.USER_AOL + "=" + "?" + ", " + UserAttributes.USER_YAHOO + "=" + "?" + ", "
				+ UserAttributes.USER_WADDRESS + "=" + "?" + ", " + UserAttributes.USER_WCITY + "=" + "?" + ", "
				+ UserAttributes.USER_WSTATE + "=" + "?" + ", " + UserAttributes.USER_WZCODE + "=" + "?" + ", "
				+ UserAttributes.USER_WCOUNTRY + "=" + "?" + ", " + UserAttributes.USER_WCOMPANY + "=" + "?" + ", "
				+ UserAttributes.USER_PASSWD + "=" + "?" + ", " + UserAttributes.USER_GROUP + "=" + "?" + ", "
				+ UserAttributes.USER_LASTLOGIN + "=" + "?" + ", " + UserAttributes.USER_DELETED + "=" + "?" + ", "
				+ UserAttributes.USER_LASTCOMM + "=" + "?" + ", " + UserAttributes.USER_UMESSAGES + "=" + "?" + ", "
				+ UserAttributes.USER_PCARD_ID + "=" + "?" + ", " + UserAttributes.USER_WCARD_ID + "=" + "?" + ", "
				+ UserAttributes.USER_DATEOFBIRTH + "=" + "?" + ", " + UserAttributes.USER_PLACE + "=" + "?" + ", "
				/*
				 * + UserAttributes.USER_PADDRESS + "='" +
				 * ua.getAttribute(UserAttributes.PADDRESS,
				 * DataAttribute.ORA_ESCAPED)
				 */
				+ UserAttributes.USER_STREETNO + "=" + "?" + ", " + UserAttributes.USER_STREETDIRECTION + "=" + "?"
				+ ", " + UserAttributes.USER_STREETNAME + "=" + "?" + ", " + UserAttributes.USER_STREETSUFFIX + "="
				+ "?" + ", " + UserAttributes.USER_STREETUNIT + "=" + "?" + ", " + UserAttributes.USER_STATE_ID + "="
				+ "?" + ", " + UserAttributes.USER_PLOCATION + "=" + "?" + ", " + UserAttributes.USER_HPHONE + "="
				+ "?" + ", " + UserAttributes.USER_MPHONE + "=" + "?" + ", " + UserAttributes.USER_PAGER
				+ "="
				+ "?"
				+ ", "
				+ UserAttributes.USER_INSTANT_MESSENGER
				+ "="
				+ "?"
				+ ", "
				+ UserAttributes.USER_MESSENGER_NUMBER
				+ "="
				+ "?"
				+ ", "
				+ UserAttributes.USER_HCITY
				+ "="
				+ "?"
				+ ", "
				+ UserAttributes.USER_HSTATE
				+ "="
				+ "?"
				+ ", "
				+ UserAttributes.USER_HZIPCODE
				+ "="
				+ "?"
				+ ", "
				+ UserAttributes.USER_HCOUNTRY
				+ "="
				+ "?"
				+ ", "
				// + UserAttributes.USER_COMMID
				// + "="
				// + ua.getAttribute(UserAttributes.COMMID,
				// DataAttribute.ORA_ESCAPED)
				// + ","
				+ UserAttributes.USER_COMPANYID + "=" + "?" + ", " + UserAttributes.USER_DISTRIBUTION_TYPE + "=" + "?"
				+ ", " + UserAttributes.USER_DISTRIBUTION_MODE + "=" + "?" + ", " + UserAttributes.USER_ADDRESS + "="
				+ "? " + ", " + UserAttributes.USER_ASSIGN_MODE + "=" + "? " + ", "
				+ UserAttributes.USER_PROFILE_READ_ONLY + "=" + "? " + ", " + UserAttributes.USER_SINGLE_SEAT + "="
				+ "? " + ", " + UserAttributes.USER_DISTRIB_ATS + "=" + "? " + ", " + UserAttributes.USER_DISTRIB_LINK
				+ "=" + "? " 
				+ ", " + UserAttributes.USER_INTERACTIVE + "=" + "? " 
				+ ", " + UserAttributes.USER_OUTSOURCE + "=" + "? ";
		if (!StringUtils.isEmpty((String) ua.getAttribute(UserAttributes.PASSWD))) {
			stm += 
				", " + UserAttributes.USER_RANDOM_TOKEN + "= ? " + 
				", " + UserAttributes.USER_PASSWORD + "= ? " + 
				", " + DBConstants.FIELD_USER_NOTIFICATION_EXPIRE_PASS_SENT + " = 0 " + 
				", " + DBConstants.FIELD_USER_LAST_PASSWORD_CHANGE_DATE + "=" + " now() ";
		}
		stm += ", " + UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED + "=" + " ? ";
		stm += ", " + UserAttributes.USER_AUTO_UPDATE + "=" + " ? ";
		stm += ", " + UserAttributes.USER_OTHER_FILES_ON_SSF + "=" + " ? ";
		stm += ", " + UserAttributes.SEND_IMAGES_SURECLOSE + "=" + " ? ";
		stm += ", " + UserAttributes.SEND_REPORT_SURECLOSE + "=" + " ? ";
		stm += ", " + UserAttributes.USER_MODIFIED_BY + "=" + " ? ";
		stm += ", " + UserAttributes.USER_DATE_MODIFIED + "=" + " ? ";
		stm += " WHERE " + UserAttributes.USER_ID + "=" + "? " + " ";

		DBConnection conn = null;
		try {

			if (!(UserUtils.isCommAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
					|| UserUtils.isTSCAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser()) || UserUtils
					.isTSAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser()))) {
				ua.setGROUP(new BigDecimal((InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
						.getAttribute(UserAttributes.GROUP).toString()));
			}
			
			conn = ConnectionPool.getInstance().requestConnection();

			PreparedStatement pstmt = null, pstmt1 = null;
			pstmt = conn.prepareStatement(stm);
			int k = 1;
			try {
				pstmt.setString(k++, ua.getAttribute(UserAttributes.LASTNAME).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.FIRSTNAME).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.MIDDLENAME).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.COMPANY).toString().trim());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.EMAIL).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.ALTEMAIL).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.PHONE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.ALTPHONE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.ICQ).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.AOL).toString());

				pstmt.setString(k++, ua.getAttribute(UserAttributes.YAHOO).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WADDRESS).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WCITY).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WSTATE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WZCODE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WCONTRY).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WCOMPANY).toString());
				pstmt.setString(k++, "");
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.GROUP), java.sql.Types.BIGINT);
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.LASTLOGIN), java.sql.Types.BIGINT);

				pstmt.setLong(k++, 0);
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.LASTCOMM), java.sql.Types.BIGINT);
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.UMESSAGES), java.sql.Types.BIGINT);
				pstmt.setString(k++, ua.getAttribute(UserAttributes.PCARD_ID).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.WCARD_ID).toString());
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.DATEOFBIRTH), java.sql.Types.BIGINT);
				pstmt.setString(k++, ua.getAttribute(UserAttributes.PLACE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.STREETNO).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.STREETDIRECTION).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.STREETNAME).toString());

				pstmt.setString(k++, ua.getAttribute(UserAttributes.STREETSUFFIX).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.STREETUNIT).toString());
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.STATE_ID), java.sql.Types.BIGINT);
				pstmt.setString(k++, ua.getAttribute(UserAttributes.PLOCATION).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.HPHONE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.MPHONE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.PAGER).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.INSTANT_MESSENGER).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.MESSENGER_NUMBER).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.HCITY).toString());

				pstmt.setString(k++, ua.getAttribute(UserAttributes.HSTATE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.HZIPCODE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.HCOUNTRY).toString());
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.COMPANYID), java.sql.Types.BIGINT);
				pstmt.setString(k++, ua.getAttribute(UserAttributes.DISTRIBUTION_TYPE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.DISTRIBUTION_MODE).toString());
				pstmt.setString(k++, ua.getAttribute(UserAttributes.ADDRESS).toString());
				// pstmt.setString( k++ ,
				// ua.getAttribute(UserAttributes.ASSIGN_MODE).toString() );
				pstmt.setInt(k++, ua.getASSIGN_MODE());
				pstmt.setInt(k++, ua.getPROFILE_READ_ONLY());
				pstmt.setInt(k++, ua.getSINGLE_SEAT());
				pstmt.setInt(k++, ua.getDISTRIB_ATS());
				pstmt.setInt(k++, ua.getDISTRIB_LINK());
				pstmt.setBoolean(k++, ua.isINTERACTIVE());
				pstmt.setString(k++, ua.getOUTSOURCE());

				if (!StringUtils.isEmpty((String) ua.getAttribute(UserAttributes.PASSWD))) {

					if (UserManager.checkCredentials((String) ua.getAttribute(UserAttributes.LOGIN), (String) ua
							.getAttribute(UserAttributes.PASSWD), false)
							|| !PasswordGenerator.validatePassword((String) ua.getAttribute(UserAttributes.PASSWD),
									DBManager.getConfigByNameAsInt("password.random.length", 6))) {
						throw new BaseException(
								"Invalid password! Your password must be different from the current one, contain at least a digit and an upper case letter and be at least "
										+ DBManager.getConfigByNameAsInt("password.random.length", 6)
										+ " characters in length!");
					}

					String timeStamp = String.valueOf(System.currentTimeMillis());
					String random_token = StringUtils.intercalateCharacters(new String[] {
							ua.getAttribute(UserAttributes.LOGIN).toString(), timeStamp,
							ua.getAttribute(UserAttributes.LASTNAME).toString() });
					String password = StringUtils.intercalateCharacters(new String[] {
							ua.getAttribute(UserAttributes.PASSWD).toString(), random_token });

					pstmt.setString(k++, random_token);
					pstmt.setString(k++, SecurityUtils.getInstance().encryptSHA512(password));
				}
				pstmt.setBoolean(k++, ua.isAUTO_ASSIGN_SEARCH_LOCKED());
				pstmt.setInt(k++, ua.getAUTO_UPDATE());
				pstmt.setInt(k++, ua.getOTHER_FILES_ON_SSF());
				pstmt.setInt(k++, ua.getSEND_IMAGES_FORCLOSURE());
				pstmt.setInt(k++, ua.getSEND_REPORT_FORCLOSURE());
				pstmt.setInt(k++, ua.getMODIFIED_BY());
				pstmt.setTimestamp(k++, new Timestamp(ua.getDATE_MODIFIED().getTime()));
			} catch (NullPointerException e) {
			}

			if (UserUtils.isCommAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
					|| UserUtils.isTSCAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
					|| UserUtils.isTSAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())) {
				pstmt.setObject(k++, ua.getAttribute(UserAttributes.ID), java.sql.Types.BIGINT);
			} else {
				pstmt.setObject(k++, InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser()
						.getAttribute(UserAttributes.ID), java.sql.Types.BIGINT);
				if (!(ua.getAttribute(UserAttributes.ID).toString().equals(InstanceManager.getManager()
						.getCurrentInstance(searchId).getCurrentUser().getAttribute(UserAttributes.ID).toString()))) {
					throw new BaseException("Parameter modification attempt detected");
				}
			}

			pstmt.executeUpdate();
			conn.commit();
			pstmt.close();

			// adds selected tempaltes into ts_user_templates
			if (UserUtils.isCommAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
					|| UserUtils.isTSCAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
					|| UserUtils.isTSAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())) {

				try {
					
				Map<Long,String[]> userTemplatesProductEnable  = new HashMap<Long, String[]>();
				if(userSubmittedData!=null) {
					userTemplatesProductEnable = (Map<Long, String[]>) userSubmittedData.get(RequestParams.USER_TEMPLATES_PRODUCT);
				}
				Map<Long,Integer> userTemplatesExportFormat  = new HashMap<Long, Integer>();
				if(userSubmittedData!=null) {
					userTemplatesExportFormat = (Map<Long, Integer>) userSubmittedData.get(RequestParams.USER_TEMPLATES_EXPORT_FORMAT);
				}
				// deletes existing records for this user
				String tDelQuery = "DELETE FROM " + DBConstants.TABLE_USER_TEMPLATES + " WHERE "
						+ DBConstants.FIELD_USER_TEMPLATES_USER_ID + "=" + uid;
				conn.executeSQL(tDelQuery);

				// insert new added templates
				String templateList = ua.getAttribute(UserAttributes.TEMPLATES, DataAttribute.ORA_ESCAPED).toString();
				String[] templates = null;
				boolean hasTemplates = false;
				if (!templateList.equals(""))
					hasTemplates = true;

				// if there are any templates selected by the user, add them
				// into database
				if (hasTemplates) {
					templates = templateList.split(UserAttributes.USER_TEMPLATES_DELIM);
					String tInsQuery = "INSERT INTO " + DBConstants.TABLE_USER_TEMPLATES + "("
							+ DBConstants.FIELD_USER_TEMPLATES_TEMPLATE_ID + ","
							+ DBConstants.FIELD_USER_TEMPLATES_USER_ID + "," + DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT + "," 
							+ DBConstants.FIELD_USER_TEMPLATES_EXPORT_FORMAT + ")" + " VALUES ";

					for (int i = 0; i < templates.length; i++) {
						long enableProduct = UserUtils.calculateTemplateEnabledOnProduct(userTemplatesProductEnable.get(Long.parseLong(templates[i].trim())));
						Integer expFor = userTemplatesExportFormat.get(Long.parseLong(templates[i].trim()));
						int exportFormat = 0;
						if (expFor!=null) {
							exportFormat = expFor.intValue();
						}
						tInsQuery += "(" + Long.parseLong(templates[i].trim()) + "," + uid + "," + enableProduct + "," + exportFormat + "),";
					}
					// get string without last comma
					tInsQuery = tInsQuery.substring(0, tInsQuery.length() - 1);
					conn.executeSQL(tInsQuery);
						
					}
				}catch(Throwable t) {
					t.printStackTrace();
				}
			}

			conn.setAutoCommit(false);

			// ResultSet resultBlob = null;
			if (photo != null) {
				if (UserUtils.hasPhoto(ua)) {
					String qry = " DELETE from " + DBConstants.TABLE_USER_PHOTO + " WHERE " + UserAttributes.USER_ID
							+ "=" + uid;
					conn.executeSQL(qry);
				}

				stm = "INSERT INTO " + DBConstants.TABLE_USER_PHOTO + " (user_id, photo) " + " VALUES(?,?)";

				pstmt = conn.prepareStatement(stm);
				pstmt.setLong(1, uid.longValue());
				FileInputStream fis = new FileInputStream(photo);
				pstmt.setBinaryStream(2, fis, (int) photo.length());

				pstmt.executeUpdate();
				pstmt.close();
				if (fis != null)
					fis.close();

				if (photo.delete() == true) {
					logger.info("File " + photo.getName() + " succesufully deleted from WEB SERVER!");
				} else {
					logger.info("Cannot delete File " + photo.getName() + " from WEB SERVER!");
				}
			}

			if (resume != null) {
				String resume_name = resume.getName();
				long resume_size = resume.length();
				if (UserUtils.hasResume(ua)) {
					String qry = " DELETE from " + DBConstants.TABLE_USER_RESUME + " WHERE " + UserAttributes.USER_ID
							+ "=" + uid;
					conn.executeSQL(qry);
				}

				stm = "INSERT INTO " + DBConstants.TABLE_USER_RESUME + " (user_id, resume_name, resume_size, resume) "
						+ " VALUES(?,?,?,?)";
				// + uid
				// + ","
				// + "empty_blob())";
				pstmt = conn.prepareStatement(stm);
				pstmt.setLong(1, uid.longValue());
				pstmt.setString(2, resume_name);
				pstmt.setLong(3, resume_size);
				FileInputStream fis = new FileInputStream(resume);
				pstmt.setBinaryStream(4, fis, (int) resume_size);

				pstmt.executeUpdate();
				pstmt.close();
				if (fis != null)
					fis.close();

				if (resume.delete() == true) {
					logger.info("File " + resume.getName() + " succesufully deleted from WEB SERVER!");
				} else {
					logger.info("Cannot delete File " + resume.getName() + " from WEB SERVER!");
				}
			}

		} catch (IOException e) {
			throw new BaseException("IOException:" + e.getMessage());
		} catch (BaseException e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} catch (SQLException e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		if (InstanceManager.getManager().getCurrentInstance(searchId) != null) {
			if (InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser() != null
					&& (InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getID().equals(ua
							.getID()))) {
				InstanceManager.getManager().getCurrentInstance(searchId).setCurrentUser(ua);
			}
			if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext() != null) {
				if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent() != null
						&& InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent()
								.getID().equals(ua.getID())) {
					InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().setAgent(ua);
				}
			}
		}

		UserManagerI userManagerI = com.stewart.ats.user.UserManager.getInstance();
		try {
			userManagerI.getAccess();
			userManagerI.refreshInfo(ua.getID().longValue());
		} catch (Throwable t) {
			logger.error("Error while trying to update cache information for user: " + ua.getID().longValue(), t);
		} finally {
			if (userManagerI != null) {
				userManagerI.releaseAccess();
			}
		}

		if (old!=null) {
			int oldGroup =  old.getGROUP().intValue();
			int newGroup =  ua.getGROUP().intValue();
			if (oldGroup==GroupAttributes.TA_ID && newGroup!=GroupAttributes.TA_ID) {	//user demoted
				String role = GroupUtils.getGroupTitle(ua);
				EmailClient email = new EmailClient();
				email.addTo(MailConfig.getAddressTSAdminChanged());
				email.setSubject("User demoted from Title Search Administrator role on server " + URLMaping.INSTANCE_DIR);
				email.addContent(ua.getUserFullName() + " (" + ua.getLOGIN() + ") was demoted from Title Search Administrator role to " + role + " role on server " + 
					URLMaping.INSTANCE_DIR + " by " + ua.getLastModified() + ".");
				email.sendAsynchronous();
			} else if (oldGroup!=GroupAttributes.TA_ID && newGroup==GroupAttributes.TA_ID) {	//user promoted
				String role = GroupUtils.getGroupTitle(old);
				EmailClient email = new EmailClient();
				email.addTo(MailConfig.getAddressTSAdminChanged());
				email.setSubject("User promoted to Title Search Administrator role on server " + URLMaping.INSTANCE_DIR);
				email.addContent(ua.getUserFullName() + " (" + ua.getLOGIN() + ") was promoted to Title Search Administrator role from " + role + " role on server " + 
					URLMaping.INSTANCE_DIR + " by " + ua.getLastModified() + ".");
				email.sendAsynchronous();
			}
		}
		
		return ua;
	}

	public static UserAttributes deleteUserResume(UserAttributes ua) throws BaseException, DataException {
		DBConnection conn = null;
		try {
			if (UserUtils.hasResume(ua)) {
				BigDecimal uid = ua.getID();
				String qry = " DELETE from " + DBConstants.TABLE_USER_RESUME + " WHERE " + UserAttributes.USER_ID + "="
						+ uid;
				conn = ConnectionPool.getInstance().requestConnection();
				conn.executeSQL(qry);
				conn.commit();
			}
		} catch (BaseException e) {
			e.printStackTrace();
			throw new BaseException("SQLException:" + e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DataException("SQLException:" + e.getMessage());
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}
		return ua;
	}

	public static UserAttributes deleteUserPhoto(UserAttributes ua) throws BaseException, DataException {
		DBConnection conn = null;
		try {
			if (UserUtils.hasPhoto(ua)) {
				BigDecimal uid = ua.getID();
				String qry = " DELETE from " + DBConstants.TABLE_USER_PHOTO + " WHERE " + UserAttributes.USER_ID + "="
						+ uid;

				conn = ConnectionPool.getInstance().requestConnection();
				conn.executeSQL(qry);
				conn.commit();
			}
		} catch (BaseException e) {
			e.printStackTrace();
			throw new BaseException("SQLException:" + e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DataException("SQLException:" + e.getMessage());
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}
		return ua;
	}

	public static void updateUserDefaultSearchPageCounties(UserAttributes ua, long searchId) throws DataException,
			BaseException {

		String updateOrInsertUserSettings = "INSERT INTO " + DBConstants.TABLE_USER_SETTINGS + "("
				+ UserAttributes.USER_ID + "," + UserAttributes.USER_SEARCH_PAGE_STATE + ","
				+ UserAttributes.USER_SEARCH_PAGE_COUNTY + ")" + " VALUES( " + "?, " + "?, " + "? " + ")"
				+ " ON DUPLICATE KEY UPDATE " + UserAttributes.USER_SEARCH_PAGE_STATE + "=" + "?, "
				+ UserAttributes.USER_SEARCH_PAGE_COUNTY + "=" + "? ";

		DBConnection conn = null;
		conn = ConnectionPool.getInstance().requestConnection();
		PreparedStatement pstmt1 = null;
		try {
			pstmt1 = conn.prepareStatement(updateOrInsertUserSettings);

			int k = 1;

			if (UserUtils.isCommAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
					|| UserUtils.isTSCAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
					|| UserUtils.isTSAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())) {
				pstmt1.setObject(k++, ua.getAttribute(UserAttributes.ID), java.sql.Types.BIGINT);
			} else {
				pstmt1.setObject(k++, InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser()
						.getAttribute(UserAttributes.ID), java.sql.Types.BIGINT);
				if (!(ua.getAttribute(UserAttributes.ID).toString().equals(InstanceManager.getManager()
						.getCurrentInstance(searchId).getCurrentUser().getAttribute(UserAttributes.ID).toString()))) {
					throw new BaseException("Parameter modification attempt detected");
				}
			}

			pstmt1.setObject(k++, ua.getMyAtsAttributes().getSEARCH_PAGE_STATE(), java.sql.Types.BIGINT);
			pstmt1.setObject(k++, ua.getMyAtsAttributes().getSEARCH_PAGE_COUNTY(), java.sql.Types.BIGINT);
			pstmt1.setObject(k++, ua.getMyAtsAttributes().getSEARCH_PAGE_STATE(), java.sql.Types.BIGINT);
			pstmt1.setObject(k++, ua.getMyAtsAttributes().getSEARCH_PAGE_COUNTY(), java.sql.Types.BIGINT);

			pstmt1.executeUpdate();
			conn.commit();
			pstmt1.close();

		} catch (BaseException e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} catch (SQLException e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}
	}

	public static void updateUserDefaultFilterCounties(UserAttributes ua, long searchId) throws DataException,
			BaseException {
		DBConnection conn = null;
		conn = ConnectionPool.getInstance().requestConnection();
		try {
			// update the dashboard filter values
			if (ua.getID().intValue() > 0) {
				String sqlDelete = "DELETE FROM " + UserFilterMapper.TABLE_USER_FILTERS + " WHERE "
						+ UserFilterMapper.FIELD_USER_ID + " = " + ua.getID() + " AND " + UserFilterMapper.FIELD_TYPE
						+ " = " + dashboardFilterTypes.get("State") + " AND " + UserFilterMapper.FIELD_TYPE + " = "
						+ dashboardFilterTypes.get("County");
				conn.executeSQL(sqlDelete);
			}
			for (String type : dashboardFilterTypes.keySet()) {

				String[] filterValues = null;

				if (type.equalsIgnoreCase("state") || type.equalsIgnoreCase("county"))
					continue;
				if (type.equalsIgnoreCase("state"))
					filterValues = Util.extractStringArrayFromString(ua.getMyAtsAttributes().getReportState());
				if (type.equalsIgnoreCase("county"))
					filterValues = Util.extractStringArrayFromString(ua.getMyAtsAttributes().getReportCounty());

				for (String value : filterValues) {
					String sql = "INSERT INTO "
							+ UserFilterMapper.TABLE_USER_FILTERS
							+ "("
							+ UserFilterMapper.FIELD_USER_ID
							+ ","
							+ (type.equalsIgnoreCase("companyAgent") ? UserFilterMapper.FIELD_FILTER_VALUE_STRING
									: UserFilterMapper.FIELD_FILTER_VALUE_LONG) + "," + UserFilterMapper.FIELD_TYPE
							+ ") " + "VALUES(" + ua.getID() + "," + "'" + value + "'" + ","
							+ dashboardFilterTypes.get(type) + ")";
					conn.executeSQL(sql);
				}
			}

			conn.commit();
		} catch (Exception e) {
			logger.warn("Cannot update My ATS filter attributes");
			e.printStackTrace();
		} finally {
			ConnectionPool.getInstance().releaseConnection(conn);
		}
	}

	public static void updateUserRating(UserAttributes ua) {

		// boolean mustBeC2ARATEInserted = false;
		// boolean mustBeAts2RateInserted = false;
		String qry = "";

		qry = " select " + UserAttributes.USER_C2ARATEINDEX + "," + UserAttributes.USER_ATS2CRATEINDEX + " from "
				+ DBConstants.TABLE_USER_RATING + " where " + UserAttributes.USER_ID + "=" + ua.getID() + " AND "
				+ UserAttributes.USER_RATINGFROMDATE + "=" + "( select max(" + UserAttributes.USER_RATINGFROMDATE
				+ ") from " + DBConstants.TABLE_USER_RATING + " where " + UserAttributes.USER_ID + "=" + ua.getID()
				+ " AND " + UserAttributes.USER_RATINGFROMDATE + "<=" + getDatabaseRatingDate(ua) + ")";
		if (logger.isDebugEnabled())
			logger.debug(qry);

		DBConnection conn = null;
		try {

			conn = ConnectionPool.getInstance().requestConnection();
			// DatabaseData data = conn.executeSQL(qry);

			// String stm = "";
			/*
			 * if(data.getRowNumber()>=0){ if((data.getRowNumber()!=0) &&
			 * ((BigDecimal
			 * )data.getValue(1,0)).compareTo(ua.getC2ARATEINDEX())!=0)
			 * mustBeC2ARATEInserted=true; if((data.getRowNumber()!=0)&&(new
			 * BigDecimal
			 * (data.getValue(2,0).toString())).compareTo(ua.getATS2CRATEINDEX
			 * ())!=0) mustBeAts2RateInserted=true; boolean hasRight2 =
			 * InstanceManager
			 * .getCurrentInstance().getCurrentUser().getGROUP().intValue
			 * ()==GroupAttributes.CA_ID ||
			 * InstanceManager.getCurrentInstance().
			 * getCurrentUser().getGROUP().intValue()==GroupAttributes.TA_ID;
			 * if( hasRight2 && ((data.getRowNumber()==0) ||
			 * mustBeC2ARATEInserted || mustBeAts2RateInserted)) { /*stm =
			 * "update " + DBConstants.TABLE_USER_RATING + " set " +
			 * (mustBeC2ARATEInserted?(UserAttributes.USER_C2ARATEINDEX + "=" +
			 * ua.getC2ARATEINDEX() + ","):"") +
			 * (mustBeAts2RateInserted?(UserAttributes.USER_ATS2CRATEINDEX + "="
			 * + ua.getATS2CRATEINDEX() + ","):"") +
			 * UserAttributes.USER_RATINGFROMDATE + "=" +
			 * getDatabaseRatingDate(ua);
			 */
			// long rateId = DBManager.getNextSeq(DBConstants.SEQ_USER_RATE);
			/*
			 * stm = "INSERT INTO " + DBConstants.TABLE_USER_RATING + "(" +
			 * UserAttributes.USER_ID + "," + UserAttributes.USER_RATINGFROMDATE
			 * + "," + UserAttributes.USER_C2ARATEINDEX + "," +
			 * UserAttributes.USER_ATS2CRATEINDEX + ")" + " VALUES(" +
			 * ua.getID() + "," + getDatabaseRatingDate(ua) + "," +
			 * ua.getAttribute(UserAttributes.C2ARATEINDEX)+ "," +
			 * ua.getAttribute(UserAttributes.ATS2CRATEINDEX) + ")"; } } if
			 * (logger.isDebugEnabled()) logger.debug(stm); if(!stm.equals(""))
			 * conn.executeSQL(stm);
			 */
		} catch (BaseException e) {
			e.printStackTrace();
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

	}

	public static UserRates[] getUserHistoryRates(String userId) {

		Date date = new Date(System.currentTimeMillis());
		String stm = " select " + UserAttributes.USER_RATE_ID + "," + UserAttributes.USER_C2ARATEINDEX + ","
				+ UserAttributes.USER_ATS2CRATEINDEX + "," + "DATE_FORMAT(" + UserAttributes.USER_RATINGFROMDATE
				+ ", '%e-%c-%Y %H:%i:%S')" + " from " + DBConstants.TABLE_USER_RATING + " where "
				+ UserAttributes.USER_ID + "=?" + " and " + UserAttributes.USER_RATINGFROMDATE + "<="
				+ "STR_TO_DATE( '" + new FormatDate(FormatDate.TIMESTAMP).getDate(date) + "' , '"
				+ FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )" + " order by "
				+ UserAttributes.USER_RATINGFROMDATE + " desc " + "," + UserAttributes.USER_RATE_ID + " desc ";

		DBConnection conn = null;
		try {

			conn = ConnectionPool.getInstance().requestConnection();

			PreparedStatement pstmt = conn.prepareStatement(stm);
			pstmt.setString(1, userId);
			DatabaseData data = conn.executePrepQuery(pstmt);
			pstmt.close();

			if (data.getRowNumber() > 0) {
				UserRates[] userRates = new UserRates[data.getRowNumber()];
				for (int i = 0; i < data.getRowNumber(); i++) {
					userRates[i] = new UserRates();
					userRates[i].setID((BigDecimal) data.getValue(1, i));
					userRates[i].setC2ARATEINDEX(((BigDecimal) data.getValue(2, i)).doubleValue());
					userRates[i].setATS2CRATEINDEX(((BigDecimal) data.getValue(3, i)).doubleValue());
					userRates[i].setFromDate(FormatDate.getDateFromFormatedStringGMT((String) data.getValue(4, i),
							FormatDate.TIMESTAMP));
				}
				return userRates;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		return null;

	}

	public static String getDatabaseRatingDate(UserAttributes ua) {
		return "STR_TO_DATE( '" + new FormatDate(FormatDate.TIMESTAMP).getDate(ua.getRATINGFROMDATE()) + "' , '"
				+ FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
	}

	public static UserAttributes[] getUsers(UserFilter filter, boolean special) throws BaseException {

		UserAttributes[] uas;

		String stm = "SELECT " + UserAttributes.USER_ID + ", " + UserAttributes.USER_LASTNAME + ", "
				+ UserAttributes.USER_FIRSTNAME + ", " + UserAttributes.USER_MIDDLENAME + ", "
				+ UserAttributes.USER_LOGIN + ", " + UserAttributes.USER_PASSWD + ", " + UserAttributes.USER_COMPANY
				+ ", " + UserAttributes.USER_EMAIL + ", " + UserAttributes.USER_ALTEMAIL + ", "
				+ UserAttributes.USER_PHONE + ", " + UserAttributes.USER_ALTPHONE + ", " + UserAttributes.USER_ICQ
				+ ", " + UserAttributes.USER_AOL + ", " + UserAttributes.USER_YAHOO + ", "
				+ UserAttributes.USER_WADDRESS + ", " + UserAttributes.USER_WCITY + ", " + UserAttributes.USER_WSTATE
				+ ", " + UserAttributes.USER_WZCODE + ", " + UserAttributes.USER_WCOUNTRY + ", "
				+ UserAttributes.USER_WCOMPANY + ", " + UserAttributes.USER_EDITEDBY + ", " + UserAttributes.USER_GROUP
				+ ", " + UserAttributes.USER_LASTLOGIN + ", " + UserAttributes.USER_DELETED + ", "
				+ UserAttributes.USER_LASTCOMM + ", " + UserAttributes.USER_PCARD_ID + ", "
				+ UserAttributes.USER_WCARD_ID + ", " + UserAttributes.USER_DATEOFBIRTH + ", "
				+ UserAttributes.USER_PLACE + ", " + UserAttributes.USER_PADDRESS + ", "
				+ UserAttributes.USER_PLOCATION + ", " + UserAttributes.USER_HPHONE + ", " + UserAttributes.USER_MPHONE
				+ ", " + UserAttributes.USER_PAGER + ", " + UserAttributes.USER_INSTANT_MESSENGER + ", "
				+ UserAttributes.USER_MESSENGER_NUMBER + ", " + UserAttributes.USER_HCITY + ", "
				+ UserAttributes.USER_HSTATE + ", " + UserAttributes.USER_HZIPCODE + ", "
				+ UserAttributes.USER_HCOUNTRY + ", " + UserAttributes.USER_DISTRIBUTION_TYPE + ", "
				+ UserAttributes.USER_DISTRIBUTION_MODE + ", " + UserAttributes.USER_TEMPLATES + ", "
				+ UserAttributes.USER_ADDRESS + ", " + UserAttributes.USER_ASSIGN_MODE + ", "
				+ UserAttributes.USER_SINGLE_SEAT + ", " + UserAttributes.USER_DISTRIB_ATS + ", "
				+ UserAttributes.USER_DISTRIB_LINK 
				+ ", " + UserAttributes.USER_INTERACTIVE 
				+ ", " + UserAttributes.USER_OUTSOURCE
				+ ", " + UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED
				+ ", " + UserAttributes.USER_AUTO_UPDATE
				+ ", " + UserAttributes.USER_OTHER_FILES_ON_SSF
				// + ", "
				// + UserAttributes.USER_C2ARATEINDEX
				// + ", "
				// + UserAttributes.USER_ATS2CRATEINDEX
				+ " " + " FROM " + DBConstants.TABLE_USER + " WHERE " + UserAttributes.USER_DELETED + "=0";
		;
		// use the sort criteria from received UserFilter
		if (filter.getLikeFlag())
			stm = stm + " AND " + UserAttributes.USER_LOGIN + filter.getSortLike();

		stm += " AND " + UserAttributes.USER_HIDDEN + "=0 ";

		if (special)
			stm = stm + " ORDER BY " + filter.getSortCriteria() + " " + filter.getSortOrder();

		DBConnection conn = null;
		try {

			conn = ConnectionPool.getInstance().requestConnection();

			PreparedStatement pstmt = conn.prepareStatement(stm);
			int k = 1;

			if (filter.getLikeFlag()) {
				pstmt.setString(k++, filter.getSortLikeValue());
			}

			DatabaseData result = conn.executePrepQuery(pstmt);
			pstmt.close();

			int resultRows = result.getRowNumber();
			uas = new UserAttributes[resultRows];
			for (int i = 0; i < resultRows; i++) {
				uas[i] = new UserAttributes(result, i);
			}

			return uas;

		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 *
	 */
	public static UserAttributes[] getUsers(UserFilter filter, boolean special, String root) throws BaseException {

		UserAttributes[] uas;

		String stm = "SELECT " + UserAttributes.USER_ID + ", " + UserAttributes.USER_LASTNAME + ", "
				+ UserAttributes.USER_FIRSTNAME + ", " + UserAttributes.USER_MIDDLENAME + ", "
				+ UserAttributes.USER_LOGIN + ", " + UserAttributes.USER_PASSWD + ", " + UserAttributes.USER_COMPANY
				+ ", " + UserAttributes.USER_EMAIL + ", " + UserAttributes.USER_ALTEMAIL + ", "
				+ UserAttributes.USER_PHONE + ", " + UserAttributes.USER_ALTPHONE + ", " + UserAttributes.USER_ICQ
				+ ", " + UserAttributes.USER_AOL + ", " + UserAttributes.USER_YAHOO + ", "
				+ UserAttributes.USER_WADDRESS + ", " + UserAttributes.USER_WCITY + ", " + UserAttributes.USER_WSTATE
				+ ", " + UserAttributes.USER_WZCODE + ", " + UserAttributes.USER_WCOUNTRY + ", "
				+ UserAttributes.USER_WCOMPANY + ", " + UserAttributes.USER_EDITEDBY + ", " + UserAttributes.USER_GROUP
				+ ", " + UserAttributes.USER_LASTLOGIN + ", " + UserAttributes.USER_DELETED + ", "
				+ UserAttributes.USER_LASTCOMM + ", " + UserAttributes.USER_PCARD_ID + ", "
				+ UserAttributes.USER_WCARD_ID + ", " + UserAttributes.USER_DATEOFBIRTH + ", "
				+ UserAttributes.USER_PLACE + ", " + UserAttributes.USER_PADDRESS + ", "
				+ UserAttributes.USER_PLOCATION + ", " + UserAttributes.USER_HPHONE + ", " + UserAttributes.USER_MPHONE
				+ ", " + UserAttributes.USER_PAGER + ", " + UserAttributes.USER_INSTANT_MESSENGER + ", "
				+ UserAttributes.USER_MESSENGER_NUMBER + ", " + UserAttributes.USER_HCITY + ", "
				+ UserAttributes.USER_HSTATE + ", " + UserAttributes.USER_HZIPCODE + ", "
				+ UserAttributes.USER_HCOUNTRY + ", " + UserAttributes.USER_DISTRIBUTION_TYPE + ", "
				+ UserAttributes.USER_DISTRIBUTION_MODE + ", " + UserAttributes.USER_TEMPLATES + ", "
				+ UserAttributes.USER_ADDRESS + ", " + UserAttributes.USER_ASSIGN_MODE + ", "
				+ UserAttributes.USER_SINGLE_SEAT + ", " + UserAttributes.USER_DISTRIB_ATS + ", "
				+ UserAttributes.USER_DISTRIB_LINK
				+ ", "
				+ UserAttributes.USER_INTERACTIVE
				+ ", " + UserAttributes.USER_OUTSOURCE
				+ ", " + UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED
				+ ", " + UserAttributes.USER_AUTO_UPDATE
				+ ", " + UserAttributes.USER_OTHER_FILES_ON_SSF
				// + ", "
				// + UserAttributes.USER_C2ARATEINDEX
				// + ", "
				// + UserAttributes.USER_ATS2CRATEINDEX
				+ " " + " FROM " + DBConstants.TABLE_USER + " WHERE " + UserAttributes.USER_DELETED + "=0" + " AND "
				+ UserAttributes.USER_HIDDEN + "=0 ";

		if (special)
			stm = stm + " ORDER BY " + filter.getSortCriteria();

		DBConnection conn = null;
		try {

			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData result = conn.executeSQL(stm);

			int resultRows = result.getRowNumber();
			uas = new UserAttributes[resultRows];

			for (int i = 0; i < resultRows; i++) {
				uas[i] = new UserAttributes(result, i);
			}

			return uas;

		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}
	}

	public static Vector getAllUsers(UserFilter uf, boolean special, int pageNumber, int userpages, String showHidden)
			throws BaseException {

		String stm = "SELECT DISTINCT " + DBConstants.TABLE_USER + "." + UserAttributes.USER_ID + ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_LASTNAME + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_FIRSTNAME + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_MIDDLENAME
				+ ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_LOGIN + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_PASSWD + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_COMPANY + ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_EMAIL + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_ALTEMAIL + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_PHONE + ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_ALTPHONE + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_ICQ + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_AOL + ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_YAHOO + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_WADDRESS + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_WCITY + ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_WSTATE + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_WZCODE + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_WCOUNTRY
				+ ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_WCOMPANY + ", " + DBConstants.TABLE_USER
				+ "." + UserAttributes.USER_EDITEDBY + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_GROUP
				+ ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_LASTLOGIN + ", " + DBConstants.TABLE_USER
				+ "." + UserAttributes.USER_DELETED + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_LASTCOMM + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_PCARD_ID
				+ ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_WCARD_ID + ", " + DBConstants.TABLE_USER
				+ "." + UserAttributes.USER_DATEOFBIRTH + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_PLACE + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_PADDRESS + ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_PLOCATION + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_HPHONE + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_MPHONE + ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_PAGER + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_INSTANT_MESSENGER + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_MESSENGER_NUMBER + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_HCITY + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_HSTATE + ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_HZIPCODE + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_HCOUNTRY + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_DISTRIBUTION_TYPE + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_DISTRIBUTION_MODE + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_ADDRESS + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_ASSIGN_MODE
				+ ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_SINGLE_SEAT + ", " + DBConstants.TABLE_USER
				+ "." + UserAttributes.USER_DISTRIB_ATS + ", " + DBConstants.TABLE_USER + "."
				+ UserAttributes.USER_DISTRIB_LINK 
				+ ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_INTERACTIVE 
				+ ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_OUTSOURCE
				+ ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_AUTO_ASSIGN_SEARCH_LOCKED
				+ ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_AUTO_UPDATE
				+ ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_OTHER_FILES_ON_SSF
				// + ", "
				// + DBConstants.TABLE_USER
				// + "."
				// + UserAttributes.USER_C2ARATEINDEX
				// + ", "
				// + DBConstants.TABLE_USER
				// + "."
				// + UserAttributes.USER_ATS2CRATEINDEX
				+ " " + " FROM " + DBConstants.TABLE_USER;
		if (uf.getFindFlag())
			stm = stm + uf.getJoinTables()
			/*
			 * + ", " + DBConstants.TABLE_USER_COMMUNITY
			 */;
		stm = stm + " WHERE " + DBConstants.TABLE_USER + "." + UserAttributes.USER_DELETED + "= 0";
		/*
		 * if (uf.getFindFlag() && (!uf.getJoinCondition().equals(""))) stm =
		 * stm + uf.getJoinCondition();
		 */
		// use the sort criteria from received UserFilter
		if (uf.getFindFlag())
			stm = stm + uf.getUserFind();
		if (uf.getLikeFlag())
			stm = stm + " AND " + uf.getSortLike();

		if ("no".equals(showHidden)) {
			stm += " AND " + DBConstants.TABLE_USER + "." + UserAttributes.USER_HIDDEN + "=0 ";
		} else if ("hidden".equals(showHidden)) {
			stm += " AND " + DBConstants.TABLE_USER + "." + UserAttributes.USER_HIDDEN + "=1 ";
		} else if ("all".equals(showHidden)) {
		}

		if (special)
			stm = stm + " ORDER BY " + uf.getSortCriteria() + " " + uf.getSortOrder();

		DBConnection conn = null;
		try {

			conn = ConnectionPool.getInstance().requestConnection();

			PreparedStatement pstmt = conn.prepareStatement(stm);
			int k = 1;

			if (uf.getFindFlag() && !uf.getFindUserValues().isEmpty()) {
				for (Iterator iter = uf.getFindUserValues().iterator(); iter.hasNext();) {
					String s = (String) iter.next();
					pstmt.setString(k++, s);
				}
			}

			if (uf.getLikeFlag()) {
				pstmt.setString(k++, uf.getSortLikeValue());
			}

			DatabaseData result = conn.executePrepQuery(pstmt);
			pstmt.close();

			int resultRows = result.getRowNumber();
			Vector vector = new Vector();
			int fromPage = pageNumber * userpages;
			// DataConstants.USERS_ROWNUM;
			int toPage;
			if ((toPage = fromPage + userpages /* DataConstants.USERS_ROWNUM */
			) - resultRows > 0)
				toPage = resultRows;
			vector.add(new Integer(pageNumber));
			for (int i = fromPage; i < toPage; i++) {
				vector.add(new UserAttributes(result, i));
			}

			if (resultRows % userpages == 0)
				vector.add(new Integer((resultRows / userpages)
				/* DataConstants.USERS_ROWNUM */
				- pageNumber - 1));
			else
				vector.add(new Integer((resultRows / userpages)
				/* DataConstants.USERS_ROWNUM */
				- pageNumber));

			return vector;
		} catch (Exception e) {
			throw new BaseException("SQLException:" + e.getMessage());
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 *Delete checked users
	 * 
	 * @param Hashtable
	 *            userDel
	 */

	public static String deleteUser(Hashtable hashd) {
		String ret = "OK";

		Enumeration delusers = hashd.keys();
		while (delusers.hasMoreElements()) {
			String userLogin = (String) delusers.nextElement();
			if (userLogin.equals(""))
				continue;
			try {
				UserAttributes ua = UserManager.getUser(userLogin, false);
				if (ua.getGROUP().intValue() == GroupAttributes.CA_ID) {
					ret = "The User " + UserUtils.getUserFullName(ua)
							+ " is Community Administrator and cannot be deleted!";
					return ret;
				}
				if (ua.getGROUP().intValue() == GroupAttributes.TA_ID) {
					ret = "The User " + UserUtils.getUserFullName(ua)
							+ " is Title-Search Administrator and cannot be deleted!";
					return ret;
				}
				if(ua.isAdministratorToACommunity()) {
					ret = "The User " + UserUtils.getUserFullName(ua)
							+ " is Administrator to a community and cannot be deleted!";
					return ret;
				}
				if (!hasActivity(ua)) {
					logger.info("User deleted! ...");
					UserManager.deleteFromDB(ua);
				} else {
					ret = "You cannot delete " + UserUtils.getUserFullName(ua)
							+ " because searches were performed or searches are in progress using this account! ";
					return ret;
				}

			} catch (Exception e) {
				// ret = "One of the users can not be deleted";
				ret = e.getMessage();
				e.printStackTrace();
			}
		}
		return ret;
	}

	/**
	 * Hide checked users
	 * 
	 * @param hashd
	 * @return
	 */
	public static String hideUser(Hashtable hashd) {
		String ret = "OK";
		Enumeration users = hashd.keys();
		while (users.hasMoreElements()) {
			String userLogin = (String) users.nextElement();
			if (userLogin.equals(""))
				continue;
			try {
				UserAttributes ua = UserManager.getUser(userLogin, false);

				switch (ua.getGROUP().intValue()) {
				case GroupAttributes.TA_ID:
					return "User '" + UserUtils.getUserFullName(ua)
							+ "' is the Title Search Administrator and cannot be hidden!";
				case GroupAttributes.CCA_ID:
					return "User " + UserUtils.getUserFullName(ua)
							+ "' is a Special Community Administrator and cannot be hidden!";
				}
				CommunityAttributes ca = CommunityManager.getCommunity(ua.getCOMMID().longValue());
				int userId = ua.getID().intValue();
				if (userId == ca.getCOMM_ADMIN().intValue()) {
					return "User '" + UserUtils.getUserFullName(ua)
							+ "' is the Community Administrator and cannot be hidden!";
				}
				// hide user
				DBManager.getSimpleTemplate().update(
						"UPDATE " + DBConstants.TABLE_USER + " SET " + UserAttributes.USER_HIDDEN
								+ "=1 WHERE user_id=?", userId);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		return ret;
	}

	/**
	 * Unhide checked users
	 * 
	 * @param hashd
	 * @return
	 */
	public static String unHideUser(Hashtable hashd) {
		String ret = "OK";
		Enumeration users = hashd.keys();
		while (users.hasMoreElements()) {
			String userLogin = (String) users.nextElement();
			if (userLogin.equals(""))
				continue;
			try {
				// unhide user
				int userId = UserManager.getUser(userLogin, false).getID().intValue();
				DBManager.getSimpleTemplate().update(
						"UPDATE " + DBConstants.TABLE_USER + " SET " + UserAttributes.USER_HIDDEN
								+ "=0 WHERE user_id=?", userId);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		return ret;
	}

	public static UserAttributes deleteFromDB(UserAttributes user) throws DataException {

		String stm = "DELETE FROM " + DBConstants.TABLE_USER + " WHERE " + UserAttributes.USER_ID + "= "
				+ user.getID().intValue();
		if (logger.isDebugEnabled())
			logger.debug(stm);

		DBConnection conn = null;
		try {

			conn = ConnectionPool.getInstance().requestConnection();
			conn.executeSQL(stm);

		} catch (Exception e) {
			if (e.getMessage().trim().endsWith(") violated")) {
				throw new DataException("User already exist");
			} else {
				throw new DataException("SQLException:" + e.getMessage());
			}
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		return user;
	}

	public static boolean hasActivity(UserAttributes user) throws DataException {

		String stm = "SELECT 1 FROM " + DBConstants.TABLE_SEARCH + " WHERE " + DBConstants.ABSTRACTOR_ID + "="
				+ user.getID() + " OR " + DBConstants.AGENT_ID + "=" + user.getID();
		if (logger.isDebugEnabled())
			logger.debug(stm);

		DBConnection conn = null;
		try {

			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(stm);
			if (data.getRowNumber() > 0)
				return true;

		} catch (Exception e) {
			throw new DataException("SQLException:" + e.getMessage());
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	public static MyAtsAttributes loadMyAtsAttributesForUser(BigDecimal userId) {
		String sql = "SELECT * FROM " + DBConstants.TABLE_USER_SETTINGS;
		for (String type : UserManager.dashboardFilterTypes.keySet())
			sql += " LEFT JOIN ( SELECT "
					+ UserFilterMapper.FIELD_USER_ID
					+ ","
					+ " GROUP_CONCAT( CONVERT( "
					+ (!type.equals("CompanyAgent") ? UserFilterMapper.FIELD_FILTER_VALUE_LONG
							: UserFilterMapper.FIELD_FILTER_VALUE_STRING) + ",CHAR) " + "SEPARATOR ',') AS report"
					+ type + " FROM ts_user_filters  " + " WHERE " + UserFilterMapper.FIELD_TYPE + " = "
					+ UserManager.dashboardFilterTypes.get(type) + " GROUP BY " + UserFilterMapper.FIELD_USER_ID + ","
					+ UserFilterMapper.FIELD_TYPE + ") " + " AS userFilters" + type + " USING(user_id) ";
		sql += " WHERE " + DBConstants.FIELD_USER_ID + " = ? ";

		try {
			return DBManager.getSimpleTemplate().queryForObject(sql, new MyAtsAttributes(), userId);
		} catch (EmptyResultDataAccessException noSettings) {
			return new MyAtsAttributes(userId);
		} catch (Exception e) {
			e.printStackTrace();
			return new MyAtsAttributes(userId);
		}

	}

	public static String setMyATSAttributes(ParameterParser pp, MultipartParameterParser mpp, long searchId) {
		String ret = "OK";
		try {
			String login = null;
			BigDecimal user_id = null;

			// get the user id
			if (mpp != null)
				login = mpp.getMultipartStringParameter(UserAttributes.USER_ID, "");
			else
				login = pp.getStringParameter(UserAttributes.USER_ID, "");

			try {
				user_id = UserManager.getUser(login, false).getID();
			} catch (Exception e) {
				user_id = UserManager.getUser(new BigDecimal(login)).getID();
			}
			if (user_id.compareTo(new BigDecimal(-1)) == 0) {
				user_id = (BigDecimal) InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser()
						.getID();
			}

			// check if that user his the right to set the myATS attributes of
			// other users
			if (!(UserUtils.isCommAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
					|| UserUtils.isTSCAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser()) || UserUtils
					.isTSAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser()))) {

				user_id = (BigDecimal) InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser()
						.getID();
			}

			UserAttributes ua = UserManager.getUser(user_id);

			// check if that user his the right to set the myATS attributes of
			// other users
			if ((!(UserUtils.isCommAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())
					|| UserUtils.isTSCAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser()) || UserUtils
					.isTSAdmin(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser())))
					&& ua.getMyAtsAttributes().getMY_ATS_READ_ONLY() == 1) {
				throw new BaseException("My ATS settings are read-only!");
			}

			// set the user attributes

			ua.getMyAtsAttributes().setSEARCH_PAGE_STATE(
					new BigDecimal(pp.getStringParameter(UserAttributes.USER_SEARCH_PAGE_STATE, "-1")));

			ua.getMyAtsAttributes().setSEARCH_PAGE_COUNTY(
					new BigDecimal(pp.getStringParameter(UserAttributes.USER_SEARCH_PAGE_COUNTY, "-1")));

			ua.getMyAtsAttributes().setSEARCH_PAGE_AGENT(
					new BigDecimal(pp.getStringParameter(UserAttributes.USER_SEARCH_PAGE_AGENT, "-1")));

			ua.getMyAtsAttributes().setTSR_SORTBY(pp.getStringParameter(UserAttributes.USER_TSR_SORTBY, "SORTBY_INST"));

			ua.getMyAtsAttributes().setTSR_UPPER_LOWER(pp.getIntParameter(UserAttributes.USER_TSR_UPPER_LOWER, -1));

			ua.getMyAtsAttributes().setTSR_NAME_FORMAT(pp.getIntParameter(UserAttributes.USER_TSR_NAME_FORMAT, -1));

			ua.getMyAtsAttributes().setTSR_COLORING(
					new BigDecimal(pp.getStringParameter(UserAttributes.USER_TSR_COLORING, "-1")));

			ua.getMyAtsAttributes().setReportState(
					new String(pp.getMultipleStringParameter(UserAttributes.USER_DASHBOARD_STATE, ",-1,")));

			ua.getMyAtsAttributes().setReportCounty(
					new String(pp.getMultipleStringParameter(UserAttributes.USER_DASHBOARD_COUNTY, ",-1,")));

			ua.getMyAtsAttributes().setReportAbstractor(
					new String(pp.getMultipleStringParameter(UserAttributes.USER_DASHBOARD_ABSTRACTOR, ",-1,")));

			ua.getMyAtsAttributes().setReportCompanyAgent(
					new String(pp.getMultipleStringParameter(UserAttributes.USER_DASHBOARD_AGENCY, ",-1,")));

			ua.getMyAtsAttributes().setReportAgent(
					new String(pp.getMultipleStringParameter(UserAttributes.USER_DASHBOARD_AGENT, ",-1,")));

			ua.getMyAtsAttributes().setReportStatus(
					new String(pp.getMultipleStringParameter(UserAttributes.USER_DASHBOARD_STATUS, "10,11")));

			ua.getMyAtsAttributes().setReportDefaultView(
					new String(pp.getStringParameter(UserAttributes.USER_DASHBOARD_VIEW, URLMaping.REPORTS_INTERVAL)));

			ua.getMyAtsAttributes().setReportSortBy(pp.getStringParameter(UserAttributes.USER_DASHBOARD_SORTBY, "TSR"));

			ua.getMyAtsAttributes().setReportSortDir(
					pp.getStringParameter(UserAttributes.USER_DASHBOARD_SORTDIR, "DESC"));

			ua.getMyAtsAttributes().setDASHBOARD_START_INTERVAL(
					pp.getStringParameter(UserAttributes.USER_DASHBOARD_START_INTERVAL, "now-30"));

			ua.getMyAtsAttributes().setDASHBOARD_END_INTERVAL(
					pp.getStringParameter(UserAttributes.USER_DASHBOARD_END_INTERVAL, "now"));

			ua.getMyAtsAttributes().setDASHBOARD_ROWS_PER_PAGE(
					new BigDecimal(pp.getStringParameter(UserAttributes.USER_DASHBOARD_ROWS_PER_PAGE, "-1")));

			ua.getMyAtsAttributes().setDEFAULT_HOMEPAGE(
					pp.getStringParameter(UserAttributes.USER_DEFAULT_HOMEPAGE, URLMaping.REPORTS_INTERVAL));

			ua.getMyAtsAttributes().setMY_ATS_READ_ONLY(pp.getIntParameter(UserAttributes.USER_MY_ATS_READ_ONLY, 0));

			ua.getMyAtsAttributes().setReceive_notification(
					pp.getIntParameter(UserAttributes.USER_RECEIVE_NOTIFICATION, 1));

			ua.getMyAtsAttributes().setSearch_log_link(pp.getIntParameter(UserAttributes.USER_SEARCH_LOG_LINK, 0));

			ua.getMyAtsAttributes().setInvoiceEditEmail(pp.getIntParameter(UserAttributes.USER_INVOICE_EDIT_EMAIL, 0));

			ua.getMyAtsAttributes().setInvoiceEditEmail(pp.getIntParameter(UserAttributes.USER_INVOICE_EDIT_EMAIL, 0));

			ua.getMyAtsAttributes().setPaginate_tsrindex(
					new BigDecimal(pp.getStringParameter(UserAttributes.USER_TSR_PAGINATE, "0")));

			ua.getMyAtsAttributes().setLegalCase(pp.getIntParameter(UserAttributes.USER_LEGAL_CASE, 0));
			ua.getMyAtsAttributes().setVestingCase(pp.getIntParameter(UserAttributes.USER_VESTING_CASE, 0));
			ua.getMyAtsAttributes().setAddressCase(pp.getIntParameter(UserAttributes.USER_ADDRESS_CASE, 0));
			ua.getMyAtsAttributes().setAgentsSelectWidth(pp.getIntParameter(UserAttributes.USER_DASHBOARD_AGENT_SELECT_WIDTH, MyAtsAttributes.DEFAULT_AGENTS_SELECT_WIDTH));
			ua.getMyAtsAttributes().setStartViewDateValue(pp.getIntParameter(UserAttributes.USER_START_VIEW_DATE_VALUE, -1));
			
			String deleteOldSettings = "DELETE FROM " + DBConstants.TABLE_USER_SETTINGS + " WHERE "
					+ UserAttributes.USER_ID + " = ?";

			DBManager.getSimpleTemplate().update(deleteOldSettings, user_id);

			SimpleJdbcInsert sji = DBManager.getSimpleJdbcInsert().withTableName(DBConstants.TABLE_USER_SETTINGS)
					.usingGeneratedKeyColumns("user_settings_id");

			sji.executeAndReturnKey(new BeanPropertySqlParameterSource(ua.getMyAtsAttributes()));

			try {
				// update the dashboard filter values
				if (ua.getID().intValue() > 0) {
					String sqlDelete = "DELETE FROM " + UserFilterMapper.TABLE_USER_FILTERS + " WHERE "
							+ UserFilterMapper.FIELD_USER_ID + " = ? AND " + UserFilterMapper.FIELD_TYPE
							+ " not in (?,?);";
					DBManager.getSimpleTemplate().update(sqlDelete, ua.getID(),
							dashboardFilterTypes.get("ProductType"), dashboardFilterTypes.get("Warning"));
				}
				for (String type : dashboardFilterTypes.keySet()) {

					String[] filterValues = null;

					if (type.equalsIgnoreCase("state"))
						filterValues = Util.extractStringArrayFromString(ua.getMyAtsAttributes().getReportState());
					if (type.equalsIgnoreCase("county"))
						filterValues = Util.extractStringArrayFromString(ua.getMyAtsAttributes().getReportCounty());
					if (type.equalsIgnoreCase("abstractor"))
						filterValues = Util.extractStringArrayFromString(ua.getMyAtsAttributes().getReportAbstractor());
					if (type.equalsIgnoreCase("companyAgent"))
						filterValues = Util.extractStringArrayFromString(ua.getMyAtsAttributes()
								.getReportCompanyAgent());
					if (type.equalsIgnoreCase("agent"))
						filterValues = Util.extractStringArrayFromString(ua.getMyAtsAttributes().getReportAgent());
					if (type.equalsIgnoreCase("status"))
						filterValues = Util.extractStringArrayFromString(ua.getMyAtsAttributes().getReportStatus());
					if (filterValues == null)
						continue;
					for (String value : filterValues) {
						String sql = "INSERT INTO "
								+ UserFilterMapper.TABLE_USER_FILTERS
								+ "("
								+ UserFilterMapper.FIELD_USER_ID
								+ ","
								+ (type.equalsIgnoreCase("companyAgent") ? UserFilterMapper.FIELD_FILTER_VALUE_STRING
										: UserFilterMapper.FIELD_FILTER_VALUE_LONG) + "," + UserFilterMapper.FIELD_TYPE
								+ ") " + "VALUES(" + " ?," + " ?," + " ?" + ")";
						try {
							DBManager.getSimpleTemplate()
									.update(sql, ua.getID(), value, dashboardFilterTypes.get(type));
						} catch (Exception e) {
							// e.printStackTrace();
						}
					}
				}

			} catch (Exception e) {
				logger.warn("Cannot update My ATS filter attributes");
				e.printStackTrace();
			}

			if (InstanceManager.getManager().getCurrentInstance(searchId) != null) {
				if (InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser() != null
						&& (InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getID()
								.equals(ua.getID()))) {
					InstanceManager.getManager().getCurrentInstance(searchId).setCurrentUser(ua);
				}
				if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext() != null) {
					if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent() != null
							&& InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext()
									.getAgent().getID().equals(ua.getID())) {
						InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().setAgent(ua);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			ret = e.getMessage();
		}

		return ret;
	}

	//not to be used any more since all servers were updated
	/*
	public static void rewritePasswordEncoded() {

		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();

		int length = sjt.queryForInt("select length(password) from ts_user where login = 'TSAdmin'");
		if (length > 0)
			return;

		List<Map<String, Object>> dbinfo = sjt.queryForList("SELECT login, last_name, passwd from ts_user");
		for (Map<String, Object> map : dbinfo) {
			try {

				String timeStamp = String.valueOf(System.currentTimeMillis());
				String random_token = StringUtils.intercalateCharacters(new String[] { (String) map.get("login"),
						timeStamp, (String) map.get("last_name") });
				String password = StringUtils.intercalateCharacters(new String[] { (String) map.get("passwd"),
						random_token });
				sjt.update("UPDATE ts_user set password = ?, randomToken = ? where login = ?", SecurityUtils
						.getInstance().encryptSHA512(password), random_token, (String) map.get("login"));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	*/

	public static void main(String[] args) {

		//rewritePasswordEncoded();
		/*
		for (Provider provider : Security.getProviders()) {
			// System.out.println(provider);
		}
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			System.out.println(md);
			// md.update("1".getBytes());
			System.out.println(new sun.misc.BASE64Encoder().encode(md.digest("1".getBytes())));
			// md.update("2".getBytes());
			System.out.println(new sun.misc.BASE64Encoder().encode(md.digest("2".getBytes())));
			// md.update("1".getBytes());
			System.out.println(new sun.misc.BASE64Encoder().encode(md.digest("1".getBytes())));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		/*
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		long startTime = System.currentTimeMillis();
		List<Long> allUserIds = sjt.query("select user_id from ts_user", new ParameterizedRowMapper<Long>() {

			@Override
			public Long mapRow(ResultSet arg0, int arg1) throws SQLException {
				return arg0.getLong("user_id");
			}
		});
		System.err.println("Loading all user_ids took: " + (System.currentTimeMillis() - startTime) + " milliseconds");
		for (Long user_id : allUserIds) {
			long startUserTime = System.currentTimeMillis();
			UserAttributes ua = getUser(new BigDecimal(user_id));
			System.out.println("User: " + ua.getLOGIN() + " took: " + (System.currentTimeMillis() - startUserTime) + " milliseconds");
		}
		System.err.println("Loading all USERS took: " + (System.currentTimeMillis() - startTime) + " milliseconds");
		*/
		
		String login = "aalecu_ca";
		String password = "1A2S3D4F";
		
		System.err.println("login: " + login);
		System.err.println("password: " + password);
		
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		
		try {
			String randomToken = sjt.queryForObject(SQL_GET_RANDOM_NUMBER, String.class, login);
			randomToken = StringUtils.intercalateCharacters(new String[] { password, randomToken });
			
				
				String encryptedPass = SecurityUtils.getInstance().encryptSHA512(randomToken);
				
				System.err.println("SQL_IS_LOGIN_VALID_WITH_EXPIRATION: " + SQL_IS_LOGIN_VALID_WITH_EXPIRATION);
				System.err.println("randomToken: " + randomToken);
				System.err.println("Encr: " + encryptedPass);
				
				String databasePass = sjt.queryForObject("SELECT PASSWORD FROM ts_user  WHERE (login = ?) and ( (interactive = 0) OR (lastPassChangeDate + INTERVAL ? DAY >= now()) )", String.class, login, 90);
				
				System.err.println("pass: " + databasePass);
				
				System.err.println("Res1: " + sjt.queryForObject(SQL_IS_LOGIN_VALID_WITH_EXPIRATION, Boolean.class, SecurityUtils
						.getInstance().encryptSHA512(randomToken), login, 90));
				
				System.err.println("Size " + encryptedPass.length() + " and " + databasePass.length() + " and are equal " + encryptedPass.equals(databasePass));
				
				
				System.out.println(sjt.queryForObject(SQL_IS_LOGIN_VALID_WITH_EXPIRATION, Boolean.class, SecurityUtils
						.getInstance().encryptSHA512(randomToken), login, DBManager.getConfigByNameAsInt(
						"user.service.expire.user.interval.days", 90)));
			
		} catch (Throwable t) {
			logger.error("Problem while checking login", t);
		}
		
	}

	public static Category getLogger() {
		return logger;
	}
	
	

}

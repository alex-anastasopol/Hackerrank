/**
 * author: vladb
 */
package ro.cst.tsearch.database;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import com.stewart.ats.testTools.AutomaticTest;
import com.stewart.ats.testTools.AutomaticTestInstance;
import com.stewart.ats.testTools.PresenceTest;
import com.stewart.ats.testTools.PresenceTestInstance;
import com.stewart.ats.testTools.PresenceTestResult;
import com.stewart.ats.testTools.PresenceTestSite;

public class DBTestTools {
	
	private static final String TABLE_PRESENCE_TEST							= "presence_test";
	private static final String FIELD_PRESENCE_TEST_ID						= "id";
	private static final String FIELD_PRESENCE_TEST_NAME 					= "name";
	
	private static final String TABLE_PRESENCE_TEST_SITE					= "presence_test_site";
	private static final String FIELD_PRESENCE_TEST_SITE_ID					= "id";
	private static final String FIELD_PRESENCE_TEST_SITE_NAME				= "name";
	private static final String FIELD_PRESENCE_TEST_SITE_COUNTY_ID			= "county_id";
	private static final String FIELD_PRESENCE_TEST_SITE_P2					= "p2";
	private static final String FIELD_PRESENCE_TEST_SITE_TYPE				= "site_type";
	
	private static final String	TABLE_PRESENCE_TEST_ITEM					= "presence_test_item";
	private static final String FIELD_PRESENCE_TEST_ITEM_ID					= "id";
	private static final String FIELD_PRESENCE_TEST_ITEM_TEST				= "test_id";
	private static final String FIELD_PRESENCE_TEST_ITEM_SITE				= "site_id";
	
	private static final String	TABLE_PRESENCE_TEST_INSTANCE				= "presence_test_instance";
	private static final String FIELD_PRESENCE_TEST_INSTANCE_ID				= "id";
	private static final String FIELD_PRESENCE_TEST_INSTANCE_NAME			= "name";
	private static final String FIELD_PRESENCE_TEST_INSTANCE_DATE			= "date";
	private static final String FIELD_PRESENCE_TEST_INSTANCE_TOTAL			= "total_tests";
	private static final String FIELD_PRESENCE_TEST_INSTANCE_PASSED			= "passed_tests";
	private static final String FIELD_PRESENCE_TEST_INSTANCE_FINISHED		= "finished";
	
	private static final String	TABLE_PRESENCE_TEST_RESULT					= "presence_test_result";
	private static final String	FIELD_PRESENCE_TEST_RESULT_ID				= "id";
	private static final String	FIELD_PRESENCE_TEST_RESULT_ITEM				= "item_id";
	private static final String	FIELD_PRESENCE_TEST_RESULT_INSTANCE			= "instance_id";
	private static final String	FIELD_PRESENCE_TEST_RESULT_DATE				= "date";
	private static final String	FIELD_PRESENCE_TEST_RESULT_STATUS			= "status";
	private static final String	FIELD_PRESENCE_TEST_RESULT_MESSAGE			= "message";
	
	private static final String	TABLE_AUTOMATIC_TEST						= "automatic_test";
	private static final String	FIELD_AUTOMATIC_TEST_ID						= "id";
	private static final String	FIELD_AUTOMATIC_TEST_NAME					= "name";
	private static final String	FIELD_AUTOMATIC_TEST_REF_SEARCH				= "reference_search_id";
	private static final String	FIELD_AUTOMATIC_TEST_STATE					= "state";
	private static final String	FIELD_AUTOMATIC_TEST_COUNTY					= "county";
	
	private static final String TABLE_AUTOMATIC_TEST_INSTANCE				= "automatic_test_instance";
	private static final String FIELD_AUTOMATIC_TEST_INSTANCE_ID			= "id";
	private static final String FIELD_AUTOMATIC_TEST_INSTANCE_TEST			= "test_id";
	private static final String FIELD_AUTOMATIC_TEST_INSTANCE_SEARCH		= "search_id";
	private static final String FIELD_AUTOMATIC_TEST_INSTANCE_DATE			= "date";
	private static final String FIELD_AUTOMATIC_TEST_INSTANCE_DIFF_COUNT	= "difference_count";
	private static final String FIELD_AUTOMATIC_TEST_INSTANCE_IS_REF		= "is_reference";
	
	// ----------- methods for getting the number of elements from tables ----------------
	
	public static int getPresenceTestCount() {
		String sql = "select count(*) from " + TABLE_PRESENCE_TEST;
		return DBManager.getSimpleTemplate().queryForInt(sql);
	}
	
	public static int getPresenceTestCount(String testName) {
		String sql = "select count(*) from " + TABLE_PRESENCE_TEST + " where "
			+ FIELD_PRESENCE_TEST_NAME + " = ?";
		return DBManager.getSimpleTemplate().queryForInt(sql, testName);
	}
	
	public static int getPresenceTestItemCount() {
		String sql = "select count(*) from " + TABLE_PRESENCE_TEST_ITEM;
		return DBManager.getSimpleTemplate().queryForInt(sql);
	}

	public static int getPresenceTestSiteCount() {
		String sql = "select count(*) from " + TABLE_PRESENCE_TEST_SITE;
		return DBManager.getSimpleTemplate().queryForInt(sql);
	}
	
	public static int getPresenceTestInstanceCount(String namePart) {
		String sql = "select count(*) from " + TABLE_PRESENCE_TEST_INSTANCE
			+ " where " + FIELD_PRESENCE_TEST_INSTANCE_NAME + " like '%" + namePart + "%'";
		return DBManager.getSimpleTemplate().queryForInt(sql);
	}

	public static int getPresenceTestResultCount() {
		String sql = "select count(*) from " + TABLE_PRESENCE_TEST_RESULT;
		return DBManager.getSimpleTemplate().queryForInt(sql);
	}
	
	public static int getAutomaticTestInstanceCount(String namePart) {
		String sql = "select count(*) from " + TABLE_AUTOMATIC_TEST_INSTANCE + " a"
			+ " join " + TABLE_AUTOMATIC_TEST + " b"
			+ " on a." + FIELD_AUTOMATIC_TEST_INSTANCE_TEST
			+ " = b." + FIELD_AUTOMATIC_TEST_ID
			+ " where b." + FIELD_AUTOMATIC_TEST_NAME + " like '%" + namePart + "%'";
		return DBManager.getSimpleTemplate().queryForInt(sql);
	}
	
	// ------------ methods for getting next id -------------------
	
	public static int getPresenceTestNextId() {
		String sql = "select max(" + FIELD_PRESENCE_TEST_ID + ") from " + TABLE_PRESENCE_TEST;
		return DBManager.getSimpleTemplate().queryForInt(sql) + 1;
	}
	
	public static int getPresenceTestItemNextId() {
		String sql = "select max(" + FIELD_PRESENCE_TEST_ITEM_ID + ") from " + TABLE_PRESENCE_TEST_ITEM;
		return DBManager.getSimpleTemplate().queryForInt(sql) + 1;
	}

	public static int getPresenceTestSiteNextId() {
		String sql = "select max(" + FIELD_PRESENCE_TEST_SITE_ID + ") from " + TABLE_PRESENCE_TEST_SITE;
		return DBManager.getSimpleTemplate().queryForInt(sql) + 1;
	}
	
	public static int getPresenceTestInstanceNextId() {
		String sql = "select max(" + FIELD_PRESENCE_TEST_INSTANCE_ID + ") from " + TABLE_PRESENCE_TEST_INSTANCE;
		return DBManager.getSimpleTemplate().queryForInt(sql) + 1;
	}

	public static int getPresenceTestResultNextId() {
		String sql = "select max(" + FIELD_PRESENCE_TEST_RESULT_ID + ") from " + TABLE_PRESENCE_TEST_RESULT;
		return DBManager.getSimpleTemplate().queryForInt(sql) + 1;
	}
	
	public static int getAutomaticTestNextId() {
		String sql = "select max(" + FIELD_AUTOMATIC_TEST_ID + ") from " + TABLE_AUTOMATIC_TEST;
		return DBManager.getSimpleTemplate().queryForInt(sql) + 1;
	}
	
	public static int getAutomaticTestInstanceNextId() {
		String sql = "select max(" + FIELD_AUTOMATIC_TEST_INSTANCE_ID + ") from " + TABLE_AUTOMATIC_TEST_INSTANCE;
		return DBManager.getSimpleTemplate().queryForInt(sql) + 1;
	}
	
	// --------------- methods for inserting data -------------------
	
	public static void insertPresenceTest(int id, String testName) {
		String sql = "insert into " + TABLE_PRESENCE_TEST + "(" + FIELD_PRESENCE_TEST_ID
			+ " , "+ FIELD_PRESENCE_TEST_NAME + ") values(?, ?)";
		DBManager.getSimpleTemplate().update(sql, id, testName);
	}

	public static void insertPresenceTestSite(int id, String name, int countyId, int siteType, String p2) {
		String sql = "insert into " + TABLE_PRESENCE_TEST_SITE + "("
			+ FIELD_PRESENCE_TEST_SITE_ID + " , "
			+ FIELD_PRESENCE_TEST_SITE_NAME + " , "
			+ FIELD_PRESENCE_TEST_SITE_COUNTY_ID + " , "
			+ FIELD_PRESENCE_TEST_SITE_TYPE + " , "
			+ FIELD_PRESENCE_TEST_SITE_P2 + ") values(?, ?, ?, ?, ?)";
		DBManager.getSimpleTemplate().update(sql, id, name, countyId, siteType, p2);
	}

	public static void insertPresenceTestItem(int id, int testId, int siteId) {
		String sql = "insert into " + TABLE_PRESENCE_TEST_ITEM + "("
			+ FIELD_PRESENCE_TEST_ITEM_ID + " , "
			+ FIELD_PRESENCE_TEST_ITEM_TEST + " , "
			+ FIELD_PRESENCE_TEST_ITEM_SITE + ") values(?, ?, ?)";
		DBManager.getSimpleTemplate().update(sql, id, testId, siteId);
	}
	
	public static void insertPresenceTestInstance(int id, String name, int total_tests, int passed_tests, String date, int finished) {
		String sql = "insert into " + TABLE_PRESENCE_TEST_INSTANCE + "("
			+ FIELD_PRESENCE_TEST_INSTANCE_ID + " , "
			+ FIELD_PRESENCE_TEST_INSTANCE_NAME + " , "
			+ FIELD_PRESENCE_TEST_INSTANCE_TOTAL + " , "
			+ FIELD_PRESENCE_TEST_INSTANCE_PASSED + ", "
			+ FIELD_PRESENCE_TEST_INSTANCE_DATE + ", "
			+ FIELD_PRESENCE_TEST_INSTANCE_FINISHED + ") values(?, ?, ?, ?, ?, ?)";
		DBManager.getSimpleTemplate().update(sql, id, name, total_tests, passed_tests, date, finished);
	}

	public static void insertPresenceTestResult(int id, int item_id, int instance_id, String date, String status, String message) {
		String sql = "insert into " + TABLE_PRESENCE_TEST_RESULT + "("
			+ FIELD_PRESENCE_TEST_RESULT_ID + " , "
			+ FIELD_PRESENCE_TEST_RESULT_ITEM + " , "
			+ FIELD_PRESENCE_TEST_RESULT_INSTANCE + " , "
			+ FIELD_PRESENCE_TEST_RESULT_DATE + " , "
			+ FIELD_PRESENCE_TEST_RESULT_STATUS + " , "
			+ FIELD_PRESENCE_TEST_RESULT_MESSAGE + ") values(?, ?, ?, ?, ?, ?)";
		DBManager.getSimpleTemplate().update(sql, id, item_id, instance_id, date, status, message);
	}
	
	public static void insertAutomaticTest(int id, String name, long ref_search_id, String state, String county) {
		String sql = "insert into " + TABLE_AUTOMATIC_TEST + "("
			+ FIELD_AUTOMATIC_TEST_ID + ", "
			+ FIELD_AUTOMATIC_TEST_NAME + ", "
			+ FIELD_AUTOMATIC_TEST_REF_SEARCH + ", "
			+ FIELD_AUTOMATIC_TEST_STATE + ", "
			+ FIELD_AUTOMATIC_TEST_COUNTY + ") values(?, ?, ?, ?, ?)";
		DBManager.getSimpleTemplate().update(sql, id, name, ref_search_id, state, county);
	}
	
	public static void insertAutomaticTestInstance(int id, int testId, long searchId, 
			String date, int diffCount, int isReference) {
		String sql = "insert into " + TABLE_AUTOMATIC_TEST_INSTANCE + "("
			+ FIELD_AUTOMATIC_TEST_INSTANCE_ID + ", "
			+ FIELD_AUTOMATIC_TEST_INSTANCE_TEST + ", "
			+ FIELD_AUTOMATIC_TEST_INSTANCE_SEARCH + ", "
			+ FIELD_AUTOMATIC_TEST_INSTANCE_DATE + ", "
			+ FIELD_AUTOMATIC_TEST_INSTANCE_DIFF_COUNT + ", "
			+ FIELD_AUTOMATIC_TEST_INSTANCE_IS_REF + ") values(?, ?, ?, ?, ?, ?)";
		DBManager.getSimpleTemplate().update(sql, id, testId, searchId, date, diffCount, isReference);
	}
	
	public static void setPresenceTestInstance(int id, int passedTests) {
		String sql = "update " + TABLE_PRESENCE_TEST_INSTANCE + " set "
			+ FIELD_PRESENCE_TEST_INSTANCE_PASSED + " = ? where "
			+ FIELD_PRESENCE_TEST_INSTANCE_ID + " = ?";
		DBManager.getSimpleTemplate().update(sql, passedTests, id);
	}
	
	public static void setPresenceTestInstanceFinished(int id) {
		String sql = "update " + TABLE_PRESENCE_TEST_INSTANCE + " set "
		+ FIELD_PRESENCE_TEST_INSTANCE_FINISHED + " = 1 where "
		+ FIELD_PRESENCE_TEST_INSTANCE_ID + " = ?";
		DBManager.getSimpleTemplate().update(sql, id);
	}
	
	// ----------------- methods for deleting data ------------------------
	
	public static void removePresenceTest(String testName) {
		String sql = "delete from " + TABLE_PRESENCE_TEST + " where "
			+ FIELD_PRESENCE_TEST_NAME + " = ?";
		DBManager.getSimpleTemplate().update(sql, testName);
	}
	
	public static void removePresenceTestInstance(String instanceName) {
		String sql = "delete from " + TABLE_PRESENCE_TEST_INSTANCE + " where "
			+ FIELD_PRESENCE_TEST_INSTANCE_NAME + " = ?";
		DBManager.getSimpleTemplate().update(sql, instanceName);
	}
	
	public static void removeAutomaticTest(String testName) {
		String sql = "delete from " + TABLE_AUTOMATIC_TEST + " where "
			+ FIELD_AUTOMATIC_TEST_NAME + " = ?";
		DBManager.getSimpleTemplate().update(sql, testName);
	}
	
	// ----------------- methods for getting data -------------------------
	
	public static int getPresenceTestId(String name) {
		String sql = "select " + FIELD_PRESENCE_TEST_ID + " from " + TABLE_PRESENCE_TEST + " where "
			+ FIELD_PRESENCE_TEST_NAME + " = ?";
		List<Map<String, Object>> resultList = DBManager.getSimpleTemplate().queryForList(sql, name);
		
		if(resultList.isEmpty()) {
			return -1;
		}
		
		return (Integer) resultList.get(0).get(FIELD_PRESENCE_TEST_ID);	
	}
	
	public static int getPresenceTestSiteId(int countyId, int siteType, String p2) {
		String sql = "select " + FIELD_PRESENCE_TEST_SITE_ID + " from " + TABLE_PRESENCE_TEST_SITE + " where " 
			+ FIELD_PRESENCE_TEST_SITE_COUNTY_ID + " = ? and " 
			+ FIELD_PRESENCE_TEST_SITE_TYPE + " = ? and "
			+ FIELD_PRESENCE_TEST_SITE_P2 + " = ?";
		List<Map<String, Object>> resultList = DBManager.getSimpleTemplate().queryForList(sql, countyId, siteType, p2);
		
		if(resultList.isEmpty()) {
			return -1;
		}
		
		return (Integer) resultList.get(0).get(FIELD_PRESENCE_TEST_SITE_ID);
	}
	
	public static int getPresenceTestItemId(int testId, int siteId) {
		String sql = "select " + FIELD_PRESENCE_TEST_ITEM_ID + " from " + TABLE_PRESENCE_TEST_ITEM + " where "
			+ FIELD_PRESENCE_TEST_ITEM_TEST + " = ? and "
			+ FIELD_PRESENCE_TEST_ITEM_SITE + " = ?";
		List<Map<String, Object>> resultList = DBManager.getSimpleTemplate().queryForList(sql, testId, siteId);
		
		if(resultList.isEmpty()) {
			return -1;
		}
		
		return (Integer) resultList.get(0).get(FIELD_PRESENCE_TEST_ITEM_ID);	
	}
	
	/**
	 * gets first rowCount rows sorted by date starting at offset offset from presence_test_instance table
	 */
	public static List<PresenceTestInstance> getPresenceTestInstancesSortedByDate(int offset, int rowCount, String namePart) {
		String sql = "select * from " + TABLE_PRESENCE_TEST_INSTANCE
			+ " where " + FIELD_PRESENCE_TEST_INSTANCE_NAME + " like '%" + namePart + "%'"
			+ " order by " + FIELD_PRESENCE_TEST_INSTANCE_DATE + " desc "
			+ " limit " + offset + ", " + rowCount;
		ParameterizedRowMapper<PresenceTestInstance> mapper = new ParameterizedRowMapper<PresenceTestInstance>() {
			@Override
			public PresenceTestInstance mapRow(ResultSet rs, int arg1)
					throws SQLException {
				PresenceTestInstance ptInstance = new PresenceTestInstance();
				ptInstance.setId(rs.getInt(FIELD_PRESENCE_TEST_INSTANCE_ID));
				ptInstance.setName(rs.getString(FIELD_PRESENCE_TEST_INSTANCE_NAME));
				ptInstance.setDate(rs.getString(FIELD_PRESENCE_TEST_INSTANCE_DATE));
				ptInstance.setTotalTests(rs.getInt(FIELD_PRESENCE_TEST_INSTANCE_TOTAL));
				ptInstance.setPassedTests(rs.getInt(FIELD_PRESENCE_TEST_INSTANCE_PASSED));
				ptInstance.setFinished(rs.getInt(FIELD_PRESENCE_TEST_INSTANCE_FINISHED) > 0);
				
				return ptInstance;
			}
		};
		return DBManager.getSimpleTemplate().query(sql, mapper);
	}
	
	/*
	 * gets from the DB the list of results corresponding to a presence test instance
	 */
	public static List<PresenceTestResult> getPresenceTestResultsForInstance(int instanceId) {
		String sql = "select "
			+ "a." + FIELD_PRESENCE_TEST_RESULT_ID + ", "
			+ "a." + FIELD_PRESENCE_TEST_RESULT_ITEM + ", "
			+ "a." + FIELD_PRESENCE_TEST_RESULT_INSTANCE + ", "
			+ "a." + FIELD_PRESENCE_TEST_RESULT_DATE + ", "
			+ "a." + FIELD_PRESENCE_TEST_RESULT_STATUS + ", "
			+ "a." + FIELD_PRESENCE_TEST_RESULT_MESSAGE + ", "
			+ "c." + FIELD_PRESENCE_TEST_SITE_NAME
			+ " from " + TABLE_PRESENCE_TEST_RESULT + " a "
			+ " join " + TABLE_PRESENCE_TEST_ITEM + " b on "
			+ " a." + FIELD_PRESENCE_TEST_RESULT_ITEM + " = b." + FIELD_PRESENCE_TEST_ITEM_ID
			+ " join " + TABLE_PRESENCE_TEST_SITE + " c on "
			+ " b." + FIELD_PRESENCE_TEST_ITEM_SITE + " = c." + FIELD_PRESENCE_TEST_SITE_ID
			+ " where a." + FIELD_PRESENCE_TEST_RESULT_INSTANCE + " = " + instanceId;
		ParameterizedRowMapper<PresenceTestResult> mapper = new ParameterizedRowMapper<PresenceTestResult>() {
			@Override
			public PresenceTestResult mapRow(ResultSet rs, int arg1)
					throws SQLException {
				PresenceTestResult ptResult = new PresenceTestResult();
				ptResult.setId(rs.getInt(FIELD_PRESENCE_TEST_RESULT_ID));
				ptResult.setItemId(rs.getInt(FIELD_PRESENCE_TEST_RESULT_ITEM));
				ptResult.setInstanceId(rs.getInt(FIELD_PRESENCE_TEST_RESULT_INSTANCE));
				ptResult.setSiteName(rs.getString(FIELD_PRESENCE_TEST_SITE_NAME));
				ptResult.setDate(rs.getString(FIELD_PRESENCE_TEST_RESULT_DATE));
				ptResult.setStatus(rs.getString(FIELD_PRESENCE_TEST_RESULT_STATUS));
				ptResult.setMessage(rs.getString(FIELD_PRESENCE_TEST_RESULT_MESSAGE));
				
				return ptResult;
			}
			
		};
		return DBManager.getSimpleTemplate().query(sql, mapper);
	}
	
	/*
	 * gets from DB the list of presence tests with name that match namePart
	 */
	public static List<PresenceTest> getPresenceTestsThatMatch(String namePart) {
		String sql = "select * from " + TABLE_PRESENCE_TEST
			+ " where " + FIELD_PRESENCE_TEST_NAME + " like '%" + namePart + "%'"
			+ " order by " + FIELD_PRESENCE_TEST_ID + " desc";
		ParameterizedRowMapper<PresenceTest> mapper = new ParameterizedRowMapper<PresenceTest>() {
			@Override
			public PresenceTest mapRow(ResultSet rs, int arg1)
					throws SQLException {
				PresenceTest presenceTest = new PresenceTest();
				presenceTest.setId(rs.getInt(FIELD_PRESENCE_TEST_ID));
				presenceTest.setName(rs.getString(FIELD_PRESENCE_TEST_NAME));
				
				return presenceTest;
			}
			
		};
		return DBManager.getSimpleTemplate().query(sql, mapper);
	}
	
	/*
	 * gets the site list corresponding to the presence test with id presenceTestId
	 */
	public static List<PresenceTestSite> getPresenceTestSites(int presenceTestId) {
		String sql = "select c.* from " + TABLE_PRESENCE_TEST + " a"
			+ " join " + TABLE_PRESENCE_TEST_ITEM + " b"
			+ " on b." + FIELD_PRESENCE_TEST_ITEM_TEST + " = a." + FIELD_PRESENCE_TEST_ID
			+ " join " + TABLE_PRESENCE_TEST_SITE + " c"
			+ " on b." + FIELD_PRESENCE_TEST_ITEM_SITE + " = c." + FIELD_PRESENCE_TEST_SITE_ID
			+ " where a." + FIELD_PRESENCE_TEST_ID + " = " + presenceTestId;
		ParameterizedRowMapper<PresenceTestSite> mapper = new ParameterizedRowMapper<PresenceTestSite>() {
			@Override
			public PresenceTestSite mapRow(ResultSet rs, int arg1)
					throws SQLException {
				PresenceTestSite ptSite = new PresenceTestSite();
				ptSite.setId(rs.getInt(FIELD_PRESENCE_TEST_SITE_ID));
				ptSite.setName(rs.getString(FIELD_PRESENCE_TEST_SITE_NAME));
				ptSite.setCountyId(rs.getLong(FIELD_PRESENCE_TEST_SITE_COUNTY_ID));
				ptSite.setSiteType(rs.getInt(FIELD_PRESENCE_TEST_SITE_TYPE));
				ptSite.setP2(rs.getInt(FIELD_PRESENCE_TEST_SITE_P2));
				
				return ptSite;
			}
		};
		return DBManager.getSimpleTemplate().query(sql, mapper);
	}
	
	public static AutomaticTest getAutomaticTest(String name) {
		String sql = "select * from " + TABLE_AUTOMATIC_TEST + " where "
			+ FIELD_AUTOMATIC_TEST_NAME + " = ?";
		ParameterizedRowMapper<AutomaticTest> mapper = new ParameterizedRowMapper<AutomaticTest>() {
			@Override
			public AutomaticTest mapRow(ResultSet rs, int arg1)
					throws SQLException {
				AutomaticTest autoTest = new AutomaticTest();
				autoTest.setId(rs.getInt(FIELD_AUTOMATIC_TEST_ID));
				autoTest.setName(rs.getString(FIELD_AUTOMATIC_TEST_NAME));
				autoTest.setRefSearchId(rs.getInt(FIELD_AUTOMATIC_TEST_REF_SEARCH));
				autoTest.setState(rs.getString(FIELD_AUTOMATIC_TEST_STATE));
				autoTest.setCounty(rs.getString(FIELD_AUTOMATIC_TEST_COUNTY));
				
				return autoTest;
			}
		};
		List<AutomaticTest> resultList = DBManager.getSimpleTemplate().query(sql, mapper, name);
		if(!resultList.isEmpty()) {
			return resultList.get(0);
		}
		
		return null;
	}
	
	/*
	 * gets from DB the list of automatic tests with name that match namePart
	 */
	public static List<AutomaticTest> getAutomaticTestsThatMatch(String namePart) {
		String sql = "select * from " + TABLE_AUTOMATIC_TEST
			+ " where " + FIELD_AUTOMATIC_TEST_NAME + " like '%" + namePart + "%'"
			+ " order by " + FIELD_AUTOMATIC_TEST_ID + " desc";
		ParameterizedRowMapper<AutomaticTest> mapper = new ParameterizedRowMapper<AutomaticTest>() {
			@Override
			public AutomaticTest mapRow(ResultSet rs, int arg1)
					throws SQLException {
				AutomaticTest autoTest = new AutomaticTest();
				autoTest.setId(rs.getInt(FIELD_AUTOMATIC_TEST_ID));
				autoTest.setName(rs.getString(FIELD_AUTOMATIC_TEST_NAME));
				autoTest.setRefSearchId(rs.getInt(FIELD_AUTOMATIC_TEST_REF_SEARCH));
				autoTest.setState(rs.getString(FIELD_AUTOMATIC_TEST_STATE));
				autoTest.setCounty(rs.getString(FIELD_AUTOMATIC_TEST_COUNTY));
				
				return autoTest;
			}
			
		};
		return DBManager.getSimpleTemplate().query(sql, mapper);
	}
	
	/**
	 * gets the reference search id for an automatic test
	 */
	public static long getReferenceSearchId(int automaticTestId) {
		String sql = "select " + FIELD_AUTOMATIC_TEST_INSTANCE_SEARCH 
			+ " from " + TABLE_AUTOMATIC_TEST_INSTANCE + " where "
			+ FIELD_AUTOMATIC_TEST_INSTANCE_TEST + " = ? and "
			+ FIELD_AUTOMATIC_TEST_INSTANCE_IS_REF + " = 1";
		List<Map<String, Object>> resultList = DBManager.getSimpleTemplate().queryForList(sql, automaticTestId);
		
		if(resultList.isEmpty()) {
			return -1;
		}
		
		return ((BigInteger) resultList.get(0).get(FIELD_AUTOMATIC_TEST_INSTANCE_SEARCH)).longValue();
		
	}
	
	/**
	 * gets first rowCount rows sorted by date starting at offset offset from automatic_test_instance table
	 */
	public static List<AutomaticTestInstance> getAutomaticTestInstancesSortedByDate(int offset, int rowCount, String namePart) {
		String sql = "select a." + FIELD_AUTOMATIC_TEST_INSTANCE_ID + ", "
			+ "b." + FIELD_AUTOMATIC_TEST_NAME + ", "
			+ "b." + FIELD_AUTOMATIC_TEST_STATE + ", "
			+ "b." + FIELD_AUTOMATIC_TEST_COUNTY + ", "
			+ "a." + FIELD_AUTOMATIC_TEST_INSTANCE_DATE + ", "
			+ "a." + FIELD_AUTOMATIC_TEST_INSTANCE_DIFF_COUNT + ", "
			+ "a." + FIELD_AUTOMATIC_TEST_INSTANCE_IS_REF
			+ " from " + TABLE_AUTOMATIC_TEST_INSTANCE + " a"
			+ " join " + TABLE_AUTOMATIC_TEST + " b"
			+ " on a." + FIELD_AUTOMATIC_TEST_INSTANCE_TEST + " = b." + FIELD_AUTOMATIC_TEST_ID
			+ " where b." + FIELD_AUTOMATIC_TEST_NAME + " like '%" + namePart + "%'"
			+ " order by " + FIELD_AUTOMATIC_TEST_INSTANCE_DATE + " desc "
			+ " limit " + offset + ", " + rowCount;
		ParameterizedRowMapper<AutomaticTestInstance> mapper = new ParameterizedRowMapper<AutomaticTestInstance>() {
			@Override
			public AutomaticTestInstance mapRow(ResultSet rs, int arg1)
					throws SQLException {
				AutomaticTestInstance atInstance = new AutomaticTestInstance();
				atInstance.setId(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_ID));
				atInstance.setName(rs.getString(FIELD_AUTOMATIC_TEST_NAME));
				atInstance.setState(rs.getString(FIELD_AUTOMATIC_TEST_STATE));
				atInstance.setCounty(rs.getString(FIELD_AUTOMATIC_TEST_COUNTY));
				atInstance.setDate(rs.getString(FIELD_AUTOMATIC_TEST_INSTANCE_DATE));
				atInstance.setDifferences(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_DIFF_COUNT));
				atInstance.setIsReference(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_IS_REF));
				
				return atInstance;
			}
		};
		
		return DBManager.getSimpleTemplate().query(sql, mapper);
	}
	
	public static AutomaticTestInstance getAutomaticTestInstance(int testInstanceId) {
		String sql = "select * from " + TABLE_AUTOMATIC_TEST_INSTANCE
			+ " where " + FIELD_AUTOMATIC_TEST_INSTANCE_ID + " = ?";
		ParameterizedRowMapper<AutomaticTestInstance> mapper = new ParameterizedRowMapper<AutomaticTestInstance>() {
			@Override
			public AutomaticTestInstance mapRow(ResultSet rs, int arg1)
					throws SQLException {
				AutomaticTestInstance atInstance = new AutomaticTestInstance();
				atInstance.setId(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_ID));
				atInstance.setTestId(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_TEST));
				atInstance.setSearchId(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_SEARCH));
				atInstance.setDate(rs.getString(FIELD_AUTOMATIC_TEST_INSTANCE_DATE));
				atInstance.setDifferences(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_DIFF_COUNT));
				atInstance.setIsReference(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_IS_REF));
				
				return atInstance;
			}
		};
		List<AutomaticTestInstance> resultList = DBManager.getSimpleTemplate().query(sql, mapper, testInstanceId);
		
		if(resultList.isEmpty()) {
			return null;
		}
		
		return resultList.get(0);
	}
	
	public static AutomaticTestInstance getAutomaticTestInstance(int testId, int isRef) {
		String sql = "select * from " + TABLE_AUTOMATIC_TEST_INSTANCE
			+ " where " + FIELD_AUTOMATIC_TEST_INSTANCE_TEST + " = ? and "
			+ FIELD_AUTOMATIC_TEST_INSTANCE_IS_REF + " = ?";
		ParameterizedRowMapper<AutomaticTestInstance> mapper = new ParameterizedRowMapper<AutomaticTestInstance>() {
			@Override
			public AutomaticTestInstance mapRow(ResultSet rs, int arg1)
					throws SQLException {
				AutomaticTestInstance atInstance = new AutomaticTestInstance();
				atInstance.setId(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_ID));
				atInstance.setTestId(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_TEST));
				atInstance.setSearchId(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_SEARCH));
				atInstance.setDate(rs.getString(FIELD_AUTOMATIC_TEST_INSTANCE_DATE));
				atInstance.setDifferences(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_DIFF_COUNT));
				atInstance.setIsReference(rs.getInt(FIELD_AUTOMATIC_TEST_INSTANCE_IS_REF));
				
				return atInstance;
			}
		};
		List<AutomaticTestInstance> resultList = DBManager.getSimpleTemplate().query(sql, mapper, testId, isRef);
		
		if(resultList.isEmpty()) {
			return null;
		}
		
		return resultList.get(0);
	}
}
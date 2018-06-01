package ro.cst.tsearch.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.bean.dsma.DSMAConstants;
import ro.cst.tsearch.database.rowmapper.LoadInfoMapper;
import ro.cst.tsearch.database.rowmapper.SearchStatisticsMapper;
import ro.cst.tsearch.database.rowmapper.TimeGraphicMapper;
import ro.cst.tsearch.utils.DBConstants;

public class DBLoadInfo {
	
	private static final Logger logger = Logger.getLogger(DBLoadInfo.class);
	
	private static final String SQL_SELECT_LOAD = 
		" SELECT " + 
		" " + DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + " " + DBConstants.TIME_GRAPHIC_VALUE + ", " +  
		" timestamp " + DBConstants.TIME_GRAPHIC_TIME + ", " +
		" server_name " + DBConstants.TIME_GRAPHIC_SERVER + 
		" FROM " + DBConstants.TABLE_LOAD_INFO + 
		" where " + DBConstants.FIELD_LOAD_INFO_TYPE + " = ? and " +
		" date_add(timestamp, Interval ? day) > (now() )";
	private static final String SQL_SELECT_CPU = 
		" SELECT " + 
		" " + DBConstants.FIELD_LOAD_INFO_CPU + " " + DBConstants.TIME_GRAPHIC_VALUE + ", " +  
		" timestamp " + DBConstants.TIME_GRAPHIC_TIME + ", " +
		" server_name " + DBConstants.TIME_GRAPHIC_SERVER + 
		" FROM " + DBConstants.TABLE_LOAD_INFO + 
		" where " + DBConstants.FIELD_LOAD_INFO_TYPE + " = ? and " +
		" date_add(timestamp, Interval ? day) > (now() )";
	private static final String SQL_SELECT_MEMORY = 
		" SELECT " + 
		" " + DBConstants.FIELD_LOAD_INFO_MEMORY + " " + DBConstants.TIME_GRAPHIC_VALUE + ", " +  
		" timestamp " + DBConstants.TIME_GRAPHIC_TIME + ", " +
		" server_name " + DBConstants.TIME_GRAPHIC_SERVER + 
		" FROM " + DBConstants.TABLE_LOAD_INFO + 
		" where " + DBConstants.FIELD_LOAD_INFO_TYPE + " = ? and " +
		" date_add(timestamp, Interval ? day) > (now() )";
	private static final String SQL_SELECT_NETWORK = 
		" SELECT " + 
		" " + DBConstants.FIELD_LOAD_INFO_NETWORK + " " + DBConstants.TIME_GRAPHIC_VALUE + ", " +  
		" timestamp " + DBConstants.TIME_GRAPHIC_TIME + ", " +
		" server_name " + DBConstants.TIME_GRAPHIC_SERVER + 
		" FROM " + DBConstants.TABLE_LOAD_INFO + 
		" where " + DBConstants.FIELD_LOAD_INFO_TYPE + " = ? and " +
		" date_add(timestamp, Interval ? day) > (now() )";
	
	private static final String SQL_SELECT_ORDER_DAY = 
		" SELECT " + 
		" count(*) " + DBConstants.TIME_GRAPHIC_VALUE + ", " +  
		" str_to_date(concat(day(sdate),'-',month(sdate),'-',year(sdate),' ',hour(sdate),':',floor(minute(sdate)/?)*?,':0')," +
					" \"%e-%c-%Y %H:%i:%S\") " + DBConstants.TIME_GRAPHIC_TIME + ", " +
		" aux_server_name " + DBConstants.TIME_GRAPHIC_SERVER + 
		" FROM " + DBConstants.TABLE_SEARCH + 
		" where date_add(" + DBConstants.FIELD_SEARCH_SDATE + ", Interval ? hour) > (now() ) " +
		" and " + DBConstants.FIELD_SEARCH_STATUS + " != " + DBConstants.SEARCH_NOT_SAVED +
		" Group by aux_server_name, year(sdate), month(sdate), day(sdate), hour(sdate), floor(minute(sdate)/?)";
	
	private static final String SQL_SELECT_ORDER = 
		" SELECT " + 
		" " + DBConstants.FIELD_SEARCH_STATISTICS_ORDER_COUNT + " " + DBConstants.TIME_GRAPHIC_VALUE + ", " +  
		" timestamp " + DBConstants.TIME_GRAPHIC_TIME + ", " +
		" server_name " + DBConstants.TIME_GRAPHIC_SERVER + 
		" FROM " + DBConstants.TABLE_SEARCH_STATISTICS + 
		" where " + DBConstants.FIELD_LOAD_INFO_TYPE + " = ? and " +
		" date_add(timestamp, Interval ? day) > (now() )";
	
	
	private static final String SQL_SELECT_LOAD_HOUR = 
		" SELECT " + 
		" " + DBConstants.FIELD_USAGE_INFO_LOAD_FACTOR + " " + DBConstants.TIME_GRAPHIC_VALUE + ", " +  
		" timestamp " + DBConstants.TIME_GRAPHIC_TIME + ", " +
		" server_name " + DBConstants.TIME_GRAPHIC_SERVER + 
		" FROM " + DBConstants.TABLE_USAGE_INFO  +
		" where date_add(timestamp, Interval ? hour) > (now() )";
	private static final String SQL_SELECT_CPU_HOUR = 
		" SELECT " + 
		" " + DBConstants.FIELD_USAGE_INFO_CPU + " " + DBConstants.TIME_GRAPHIC_VALUE + ", " +  
		" timestamp " + DBConstants.TIME_GRAPHIC_TIME + ", " +
		" server_name " + DBConstants.TIME_GRAPHIC_SERVER + 
		" FROM " + DBConstants.TABLE_USAGE_INFO  +
		" where date_add(timestamp, Interval ? hour) > (now() )";
	private static final String SQL_SELECT_MEMORY_HOUR = 
		" SELECT " + 
		" " + DBConstants.FIELD_USAGE_INFO_MEMORY + " " + DBConstants.TIME_GRAPHIC_VALUE + ", " +  
		" timestamp " + DBConstants.TIME_GRAPHIC_TIME + ", " +
		" server_name " + DBConstants.TIME_GRAPHIC_SERVER + 
		" FROM " + DBConstants.TABLE_USAGE_INFO  +
		" where date_add(timestamp, Interval ? hour) > (now() )";
	private static final String SQL_SELECT_NETWORK_HOUR = 
		" SELECT " + 
		" " + DBConstants.FIELD_USAGE_INFO_NETWORK + " " + DBConstants.TIME_GRAPHIC_VALUE + ", " +  
		" timestamp " + DBConstants.TIME_GRAPHIC_TIME + ", " +
		" server_name " + DBConstants.TIME_GRAPHIC_SERVER + 
		" FROM " + DBConstants.TABLE_USAGE_INFO  +
		" where date_add(timestamp, Interval ? hour) > (now() )";

	/**
	 * Sql query used to precalculate information used for the day load graphic in DMSA 
	 */
	private static final String SQL_SELECT_ALL_LOAD_DAY = 
		" SELECT " + 
		" avg(" + DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ") " + DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_CPU + ") " + DBConstants.FIELD_LOAD_INFO_CPU + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_MEMORY + ") " + DBConstants.FIELD_LOAD_INFO_MEMORY + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_MEMORY_FREE + ") " + DBConstants.FIELD_LOAD_INFO_MEMORY_FREE + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_NETWORK + ") " + DBConstants.FIELD_LOAD_INFO_NETWORK + ", " +
		" str_to_date(concat(" +
			"day(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),'-'," +
			"month(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),'-'," +
			"year(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),' '," +
			"hour(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),':'," +
			"floor(minute(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + ")/?)*?,':0'),\"%e-%c-%Y %H:%i:%S\") " + 
			DBConstants.FIELD_LOAD_INFO_TIMESTAMP + ", " +
		"server_name " + DBConstants.FIELD_LOAD_INFO_SERVER_NAME + 
		" FROM " + DBConstants.TABLE_USAGE_INFO  +
		" where date_add(timestamp, Interval 1 day) > (now()) " +
		" group by " + 
		DBConstants.FIELD_LOAD_INFO_SERVER_NAME + ", year(timestamp), month(timestamp), day(timestamp), hour(timestamp), floor(minute(timestamp)/?) ";
	/**
	 * Sql query used to update information used for the day load graphic in DMSA 
	 */
	private static final String SQL_INSERT_ALL_LOAD_DAY =
		"INSERT INTO " + DBConstants.TABLE_LOAD_INFO + 
		"(" + 
			DBConstants.FIELD_LOAD_INFO_CPU + ", " + 
			DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ", " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY +  ", " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY_FREE +  ", " + 
			DBConstants.FIELD_LOAD_INFO_NETWORK +  ", " + 
			DBConstants.FIELD_LOAD_INFO_SERVER_NAME +  ", " + 
			DBConstants.FIELD_LOAD_INFO_TYPE +  ", " + 
			DBConstants.FIELD_LOAD_INFO_TIMESTAMP +  
		") VALUES (?,?,?,?,?,?,?,?) " + 
		" ON duplicate KEY UPDATE " + 
			DBConstants.FIELD_LOAD_INFO_CPU + "=?, " + 
			DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + "=?, " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY +  "=?, " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY_FREE +  "=?, " + 
			DBConstants.FIELD_LOAD_INFO_NETWORK +  "=? ";
	
	/**
	 * Sql query used to precalculate information used for the week load graphic in DMSA 
	 */
	private static final String SQL_SELECT_ALL_SEARCH_STATISTICS_WEEK = 
		" SELECT " + 
		" count(" + DBConstants.FIELD_SEARCH_ID + ") " + DBConstants.FIELD_SEARCH_STATISTICS_ORDER_COUNT + ", " +
		" str_to_date(concat(" +
			"day(" + DBConstants.FIELD_SEARCH_SDATE + "),'-'," +
			"month(" + DBConstants.FIELD_SEARCH_SDATE + "),'-'," +
			"year(" + DBConstants.FIELD_SEARCH_SDATE + "),' '," +
			"hour(" + DBConstants.FIELD_SEARCH_SDATE + "),':00:00'),\"%e-%c-%Y %H:%i:%S\") " + 
			DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + ", " +
			DBConstants.FIELD_SEARCH_SERVER_NAME + " " + DBConstants.FIELD_SEARCH_STATISTICS_SERVER_NAME + 
		" FROM " + DBConstants.TABLE_SEARCH  +
		" where date_add(" + DBConstants.FIELD_SEARCH_SDATE + ", Interval 7 day) > (now()) " +
		" and " + DBConstants.FIELD_SEARCH_STATUS + " != " + DBConstants.SEARCH_NOT_SAVED +
		" group by " + DBConstants.FIELD_SEARCH_SERVER_NAME + 
			", year(" + DBConstants.FIELD_SEARCH_SDATE + 
			"), month(" + DBConstants.FIELD_SEARCH_SDATE + 
			"), day(" + DBConstants.FIELD_SEARCH_SDATE + 
			"), hour(" + DBConstants.FIELD_SEARCH_SDATE + ") ";
	
	/**
	 * Sql query used to update information used for the week load graphic in DMSA 
	 */
	private static final String SQL_INSERT_ALL_LOAD_WEEK =
		"INSERT INTO " + DBConstants.TABLE_LOAD_INFO + 
		"(" + 
			DBConstants.FIELD_LOAD_INFO_CPU + ", " + 
			DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ", " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY +  ", " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY_FREE +  ", " + 
			DBConstants.FIELD_LOAD_INFO_NETWORK +  ", " + 
			DBConstants.FIELD_LOAD_INFO_SERVER_NAME +  ", " + 
			DBConstants.FIELD_LOAD_INFO_TYPE +  ", " + 
			DBConstants.FIELD_LOAD_INFO_TIMESTAMP +  
		") VALUES (?,?,?,?,?,?,?,?) " + 
		" ON duplicate KEY UPDATE " + 
			DBConstants.FIELD_LOAD_INFO_CPU + "=?, " + 
			DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + "=?, " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY +  "=?, " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY_FREE +  "=?, " + 
			DBConstants.FIELD_LOAD_INFO_NETWORK +  "=? ";
	
	/**
	 * Sql query used to precalculate information used for the month load graphic in DMSA 
	 */
	private static final String SQL_SELECT_ALL_LOAD_MONTH = 
		" SELECT " + 
		" avg(" + DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ") " + DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_CPU + ") " + DBConstants.FIELD_LOAD_INFO_CPU + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_MEMORY + ") " + DBConstants.FIELD_LOAD_INFO_MEMORY + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_MEMORY_FREE + ") " + DBConstants.FIELD_LOAD_INFO_MEMORY_FREE + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_NETWORK + ") " + DBConstants.FIELD_LOAD_INFO_NETWORK + ", " +
		" str_to_date(concat(" +
			"day(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),'-'," +
			"month(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),'-'," +
			"year(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),' '," +
			"floor(hour(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + ")/?)*?,':00:00'),\"%e-%c-%Y %H:%i:%S\") " + 
			DBConstants.FIELD_LOAD_INFO_TIMESTAMP + ", " +
		"server_name " + DBConstants.FIELD_LOAD_INFO_SERVER_NAME + 
		" FROM " + DBConstants.TABLE_LOAD_INFO  +
		" where date_add(timestamp, Interval 1 month) > (now()) " +
		" and " + DBConstants.FIELD_LOAD_INFO_TYPE + " = " + DBConstants.LOAD_USAGE_DAY + 
		" group by " + 
		DBConstants.FIELD_LOAD_INFO_SERVER_NAME + ", year(timestamp), month(timestamp), day(timestamp), floor(hour(timestamp)/?) ";
	
	/**
	 * Sql query used to update information used for the month load graphic in DMSA 
	 */
	private static final String SQL_INSERT_ALL_LOAD_MONTH =
		"INSERT INTO " + DBConstants.TABLE_LOAD_INFO + 
		"(" + 
			DBConstants.FIELD_LOAD_INFO_CPU + ", " + 
			DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ", " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY +  ", " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY_FREE +  ", " + 
			DBConstants.FIELD_LOAD_INFO_NETWORK +  ", " + 
			DBConstants.FIELD_LOAD_INFO_SERVER_NAME +  ", " + 
			DBConstants.FIELD_LOAD_INFO_TYPE +  ", " + 
			DBConstants.FIELD_LOAD_INFO_TIMESTAMP +  
		") VALUES (?,?,?,?,?,?,?,?) " + 
		" ON duplicate KEY UPDATE " + 
			DBConstants.FIELD_LOAD_INFO_CPU + "=?, " + 
			DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + "=?, " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY +  "=?, " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY_FREE +  "=?, " + 
			DBConstants.FIELD_LOAD_INFO_NETWORK +  "=? ";
	
	/**
	 * Sql query used to update information used for the year load graphic in DMSA 
	 */
	private static final String SQL_INSERT_ALL_LOAD_YEAR =
		"INSERT INTO " + DBConstants.TABLE_LOAD_INFO + 
		"(" + 
			DBConstants.FIELD_LOAD_INFO_CPU + ", " + 
			DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ", " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY +  ", " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY_FREE +  ", " + 
			DBConstants.FIELD_LOAD_INFO_NETWORK +  ", " + 
			DBConstants.FIELD_LOAD_INFO_SERVER_NAME +  ", " + 
			DBConstants.FIELD_LOAD_INFO_TYPE +  ", " + 
			DBConstants.FIELD_LOAD_INFO_TIMESTAMP +  
		") VALUES (?,?,?,?,?,?,?,?) " + 
		" ON duplicate KEY UPDATE " + 
			DBConstants.FIELD_LOAD_INFO_CPU + "=?, " + 
			DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + "=?, " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY +  "=?, " + 
			DBConstants.FIELD_LOAD_INFO_MEMORY_FREE +  "=?, " + 
			DBConstants.FIELD_LOAD_INFO_NETWORK +  "=? ";
	
	/**
	 * Sql query used to precalculate information used for the week load graphic in DMSA 
	 */
	private static final String SQL_SELECT_ALL_LOAD_WEEK = 
		" SELECT " + 
		" avg(" + DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ") " + DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_CPU + ") " + DBConstants.FIELD_LOAD_INFO_CPU + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_MEMORY + ") " + DBConstants.FIELD_LOAD_INFO_MEMORY + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_MEMORY_FREE + ") " + DBConstants.FIELD_LOAD_INFO_MEMORY_FREE + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_NETWORK + ") " + DBConstants.FIELD_LOAD_INFO_NETWORK + ", " +
		" str_to_date(concat(" +
			"day(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),'-'," +
			"month(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),'-'," +
			"year(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),' '," +
			"hour(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),':00:00'),\"%e-%c-%Y %H:%i:%S\") " + 
			DBConstants.FIELD_LOAD_INFO_TIMESTAMP + ", " +
		"server_name " + DBConstants.FIELD_LOAD_INFO_SERVER_NAME + 
		" FROM " + DBConstants.TABLE_LOAD_INFO  +
		" where date_add(timestamp, Interval 7 day) > (now()) " +
		" and " + DBConstants.FIELD_LOAD_INFO_TYPE + " = " + DBConstants.LOAD_USAGE_DAY +
		" group by " + 
		DBConstants.FIELD_LOAD_INFO_SERVER_NAME + ", year(timestamp), month(timestamp), day(timestamp), hour(timestamp) ";
	
	/**
	 * Sql query used to update information used for the week load graphic in DMSA 
	 */
	private static final String SQL_INSERT_SEARCH_STATISTICS =
		"INSERT INTO " + DBConstants.TABLE_SEARCH_STATISTICS + 
		"(" + 
			DBConstants.FIELD_SEARCH_STATISTICS_ORDER_COUNT + ", " + 
			DBConstants.FIELD_SEARCH_STATISTICS_SERVER_NAME +  ", " + 
			DBConstants.FIELD_SEARCH_STATISTICS_TYPE +  ", " + 
			DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP +  
		") VALUES (?,?,?,?) " + 
		" ON duplicate KEY UPDATE " + 
			DBConstants.FIELD_SEARCH_STATISTICS_ORDER_COUNT + " =? ";
	/**
	 * Sql query used to precalculate information used for the week load graphic in DMSA 
	 */
	private static final String SQL_SELECT_ALL_SEARCH_STATISTICS_MONTH = 
		" SELECT " + 
		" avg(" + DBConstants.FIELD_SEARCH_STATISTICS_ORDER_COUNT + ") " + DBConstants.FIELD_SEARCH_STATISTICS_ORDER_COUNT + ", " +
		" str_to_date(concat(" +
			"day(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + "),'-'," +
			"month(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + "),'-'," +
			"year(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + "),' '," +
			"floor(hour(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + ")/?)*?,':00:00'),\"%e-%c-%Y %H:%i:%S\") " + 
			DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + ", " +
			DBConstants.FIELD_SEARCH_STATISTICS_SERVER_NAME + " " + DBConstants.FIELD_SEARCH_STATISTICS_SERVER_NAME + 
		" FROM " + DBConstants.TABLE_SEARCH_STATISTICS  +
		" where date_add(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + ", Interval 1 month) > (now()) " +
		" and " + DBConstants.FIELD_SEARCH_STATISTICS_TYPE + " = " + DBConstants.LOAD_USAGE_WEEK +
		" group by " + DBConstants.FIELD_SEARCH_STATISTICS_SERVER_NAME + 
			", year(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + 
			"), month(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + 
			"), day(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + 
			"), floor(hour(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + ")/?) ";
	
	/**
	 * Sql query used to precalculate information used for the week load graphic in DMSA 
	 */
	private static final String SQL_SELECT_ALL_SEARCH_STATISTICS_YEAR = 
		" SELECT " + 
		" avg(" + DBConstants.FIELD_SEARCH_STATISTICS_ORDER_COUNT + ") " + DBConstants.FIELD_SEARCH_STATISTICS_ORDER_COUNT + ", " +
		" str_to_date(concat(" +
			"day(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + "),'-'," +
			"month(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + "),'-'," +
			"year(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + "),' '," +
			"hour(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + "),':00:00'),\"%e-%c-%Y %H:%i:%S\") " + 
			DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + ", " +
			DBConstants.FIELD_SEARCH_STATISTICS_SERVER_NAME + " " + DBConstants.FIELD_SEARCH_STATISTICS_SERVER_NAME + 
		" FROM " + DBConstants.TABLE_SEARCH_STATISTICS  +
		" where date_add(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + ", Interval 1 year) > (now()) " +
		" and " + DBConstants.FIELD_SEARCH_STATISTICS_TYPE + " = " + DBConstants.LOAD_USAGE_MONTH +
		" group by " + DBConstants.FIELD_SEARCH_STATISTICS_SERVER_NAME + 
			", year(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + 
			"), month(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + 
			"), day(" + DBConstants.FIELD_SEARCH_STATISTICS_TIMESTAMP + 
			") ";
	
	/**
	 * Sql query used to precalculate information used for the year load graphic in DMSA 
	 */
	private static final String SQL_SELECT_ALL_LOAD_YEAR = 
		" SELECT " + 
		" avg(" + DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ") " + DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_CPU + ") " + DBConstants.FIELD_LOAD_INFO_CPU + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_MEMORY + ") " + DBConstants.FIELD_LOAD_INFO_MEMORY + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_MEMORY_FREE + ") " + DBConstants.FIELD_LOAD_INFO_MEMORY_FREE + ", " +
		" avg(" + DBConstants.FIELD_LOAD_INFO_NETWORK + ") " + DBConstants.FIELD_LOAD_INFO_NETWORK + ", " +
		" str_to_date(concat(" +
			"day(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),'-'," +
			"month(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),'-'," +
			"year(" + DBConstants.FIELD_LOAD_INFO_TIMESTAMP + "),' 00:00:00'),\"%e-%c-%Y %H:%i:%S\") " + 
			DBConstants.FIELD_LOAD_INFO_TIMESTAMP + ", " +
		" server_name " + DBConstants.FIELD_LOAD_INFO_SERVER_NAME + 
		" FROM " + DBConstants.TABLE_LOAD_INFO  +
		" where date_add(timestamp, Interval 1 year) > (now()) " +
		" and " + DBConstants.FIELD_LOAD_INFO_TYPE + " = " + DBConstants.LOAD_USAGE_MONTH + 
		" group by " + 
		DBConstants.FIELD_LOAD_INFO_SERVER_NAME + ", year(timestamp), month(timestamp), day(timestamp) ";
	
	
	public static void updateLoadInformationLastDay(){
		try {
			long startTime = System.currentTimeMillis();
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<LoadInfoMapper> toBeInserted = sjt.query(SQL_SELECT_ALL_LOAD_DAY, new LoadInfoMapper(), 10, 10, 10);
			for (LoadInfoMapper loadInfoMapper : toBeInserted) {
				sjt.update(SQL_INSERT_ALL_LOAD_DAY, 
						loadInfoMapper.getCpu(), 
						loadInfoMapper.getLoadFactor(),
						loadInfoMapper.getMemory(),
						loadInfoMapper.getMemoryFree(),
						loadInfoMapper.getNetwork(),
						loadInfoMapper.getServerName(),
						DBConstants.LOAD_USAGE_DAY,
						loadInfoMapper.getTimestamp(),
						loadInfoMapper.getCpu(), 
						loadInfoMapper.getLoadFactor(),
						loadInfoMapper.getMemory(),
						loadInfoMapper.getMemoryFree(),
						loadInfoMapper.getNetwork()
				);
			}
			if(logger.isDebugEnabled()) {
				logger.debug("updateLoadInformationLastDay took " + ((System.currentTimeMillis()-startTime)/1000) + " seconds");
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void updateLoadInformationLastWeek(){
		try {
			long startTime = System.currentTimeMillis();
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<LoadInfoMapper> toBeInserted = sjt.query(SQL_SELECT_ALL_LOAD_WEEK, new LoadInfoMapper());
			for (LoadInfoMapper loadInfoMapper : toBeInserted) {
				sjt.update(SQL_INSERT_ALL_LOAD_WEEK, 
						loadInfoMapper.getCpu(), 
						loadInfoMapper.getLoadFactor(),
						loadInfoMapper.getMemory(),
						loadInfoMapper.getMemoryFree(),
						loadInfoMapper.getNetwork(),
						loadInfoMapper.getServerName(),
						DBConstants.LOAD_USAGE_WEEK,
						loadInfoMapper.getTimestamp(),
						loadInfoMapper.getCpu(), 
						loadInfoMapper.getLoadFactor(),
						loadInfoMapper.getMemory(),
						loadInfoMapper.getMemoryFree(),
						loadInfoMapper.getNetwork()
				);
			}
			if(logger.isDebugEnabled()) {
				logger.debug("updateLoadInformationLastWeek took " + ((System.currentTimeMillis()-startTime)/1000) + " seconds");
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void updateLoadInformationLastMonth(){
		try {
			long startTime = System.currentTimeMillis();
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<LoadInfoMapper> toBeInserted = sjt.query(SQL_SELECT_ALL_LOAD_MONTH, new LoadInfoMapper(), 4, 4, 4);
			for (LoadInfoMapper loadInfoMapper : toBeInserted) {
				sjt.update(SQL_INSERT_ALL_LOAD_MONTH, 
						loadInfoMapper.getCpu(), 
						loadInfoMapper.getLoadFactor(),
						loadInfoMapper.getMemory(),
						loadInfoMapper.getMemoryFree(),
						loadInfoMapper.getNetwork(),
						loadInfoMapper.getServerName(),
						DBConstants.LOAD_USAGE_MONTH,
						loadInfoMapper.getTimestamp(),
						loadInfoMapper.getCpu(), 
						loadInfoMapper.getLoadFactor(),
						loadInfoMapper.getMemory(),
						loadInfoMapper.getMemoryFree(),
						loadInfoMapper.getNetwork()
				);
			}
			if(logger.isDebugEnabled()) {
				logger.debug("updateLoadInformationLastMonth took " + ((System.currentTimeMillis()-startTime)/1000) + " seconds");
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void updateLoadInformationLastYear(){
		try {
			long startTime = System.currentTimeMillis();
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<LoadInfoMapper> toBeInserted = sjt.query(SQL_SELECT_ALL_LOAD_YEAR, new LoadInfoMapper());
			for (LoadInfoMapper loadInfoMapper : toBeInserted) {
				sjt.update(SQL_INSERT_ALL_LOAD_YEAR, 
						loadInfoMapper.getCpu(), 
						loadInfoMapper.getLoadFactor(),
						loadInfoMapper.getMemory(),
						loadInfoMapper.getMemoryFree(),
						loadInfoMapper.getNetwork(),
						loadInfoMapper.getServerName(),
						DBConstants.LOAD_USAGE_YEAR,
						loadInfoMapper.getTimestamp(),
						loadInfoMapper.getCpu(), 
						loadInfoMapper.getLoadFactor(),
						loadInfoMapper.getMemory(),
						loadInfoMapper.getMemoryFree(),
						loadInfoMapper.getNetwork()
				);
			}
			if(logger.isDebugEnabled()) {
				logger.debug("updateLoadInformationLastYear took " + ((System.currentTimeMillis()-startTime)/1000) + " seconds");
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void updateSearchStatisticsLastWeek(){
		try {
			long startTime = System.currentTimeMillis();
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<SearchStatisticsMapper> toBeInserted = sjt.query(SQL_SELECT_ALL_SEARCH_STATISTICS_WEEK, 
					new SearchStatisticsMapper());
			for (SearchStatisticsMapper mapper : toBeInserted) {
				sjt.update(SQL_INSERT_SEARCH_STATISTICS, 
						mapper.getSearchOrderCount(), 
						mapper.getServerName(),
						DBConstants.LOAD_USAGE_WEEK,
						mapper.getTimestamp(),
						mapper.getSearchOrderCount()
				);
			}
			if(logger.isDebugEnabled()) {
				logger.debug("updateSearchStatisticsLastWeek took " + 
						((System.currentTimeMillis()-startTime)/1000) + " seconds");
			}
		} catch (RuntimeException e) {
			logger.error("Error in updateSearchStatisticsLastWeek", e);
		}
		
	}
	
	public static void updateSearchStatisticsLastMonth(){
		try {
			long startTime = System.currentTimeMillis();
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<SearchStatisticsMapper> toBeInserted = sjt.query(SQL_SELECT_ALL_SEARCH_STATISTICS_MONTH, 
					new SearchStatisticsMapper(), 4, 4, 4);
			for (SearchStatisticsMapper mapper : toBeInserted) {
				sjt.update(SQL_INSERT_SEARCH_STATISTICS, 
						mapper.getSearchOrderCount(), 
						mapper.getServerName(),
						DBConstants.LOAD_USAGE_MONTH,
						mapper.getTimestamp(),
						mapper.getSearchOrderCount()
				);
			}
			if(logger.isDebugEnabled()) {
				logger.debug("updateSearchStatisticsLastMonth took " + 
						((System.currentTimeMillis()-startTime)/1000) + " seconds");
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void updateSearchStatisticsLastYear(){
		try {
			long startTime = System.currentTimeMillis();
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<SearchStatisticsMapper> toBeInserted = sjt.query(SQL_SELECT_ALL_SEARCH_STATISTICS_YEAR, 
					new SearchStatisticsMapper());
			for (SearchStatisticsMapper mapper : toBeInserted) {
				sjt.update(SQL_INSERT_SEARCH_STATISTICS, 
						mapper.getSearchOrderCount(), 
						mapper.getServerName(),
						DBConstants.LOAD_USAGE_YEAR,
						mapper.getTimestamp(),
						mapper.getSearchOrderCount()
				);
			}
			if(logger.isDebugEnabled()) {
				logger.debug("updateSearchStatisticsLastYear took " + 
						((System.currentTimeMillis()-startTime)/1000) + " seconds");
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * Returns a list containing load information for the given period
	 * @param days
	 * @return
	 */
	public static List<TimeGraphicMapper> getLoadInfo(int type, int days){
		List<TimeGraphicMapper> list = new ArrayList<TimeGraphicMapper>();
		try {
			long startTime = System.currentTimeMillis();
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			int chartType = days*24;
			if(type == DSMAConstants.LOAD_GRAPH) {
				if(chartType == DSMAConstants.INTERVAL_DAY) {
					list = sjt.query(SQL_SELECT_LOAD, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_DAY, days);
				} else if(chartType == DSMAConstants.INTERVAL_WEEK) {
					list = sjt.query(SQL_SELECT_LOAD, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_WEEK, days);
				} else if(chartType == DSMAConstants.INTERVAL_MONTH) {
					list = sjt.query(SQL_SELECT_LOAD, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_MONTH, days);
				} else if(chartType == DSMAConstants.INTERVAL_YEAR) {
					list = sjt.query(SQL_SELECT_LOAD, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_YEAR, days);
				}
			} else if(type == DSMAConstants.CPU_GRAPH) {
				//list = sjt.query(SQL_SELECT_CPU, new TimeGraphicMapper(), days);
				if(chartType == DSMAConstants.INTERVAL_DAY) {
					list = sjt.query(SQL_SELECT_CPU, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_DAY, days);
				} else if(chartType == DSMAConstants.INTERVAL_WEEK) {
					list = sjt.query(SQL_SELECT_CPU, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_WEEK, days);
				} else if(chartType == DSMAConstants.INTERVAL_MONTH) {
					list = sjt.query(SQL_SELECT_CPU, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_MONTH, days);
				} else if(chartType == DSMAConstants.INTERVAL_YEAR) {
					list = sjt.query(SQL_SELECT_CPU, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_YEAR, days);
				}
			} else if(type == DSMAConstants.MEM_GRAPH) {
				//list = sjt.query(SQL_SELECT_MEMORY, new TimeGraphicMapper(), days);
				if(chartType == DSMAConstants.INTERVAL_DAY) {
					list = sjt.query(SQL_SELECT_MEMORY, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_DAY, days);
				} else if(chartType == DSMAConstants.INTERVAL_WEEK) {
					list = sjt.query(SQL_SELECT_MEMORY, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_WEEK, days);
				} else if(chartType == DSMAConstants.INTERVAL_MONTH) {
					list = sjt.query(SQL_SELECT_MEMORY, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_MONTH, days);
				} else if(chartType == DSMAConstants.INTERVAL_YEAR) {
					list = sjt.query(SQL_SELECT_MEMORY, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_YEAR, days);
				}
			} else if(type == DSMAConstants.NETWORK_GRAPH) {
				//list = sjt.query(SQL_SELECT_NETWORK, new TimeGraphicMapper(), days);
				if(chartType == DSMAConstants.INTERVAL_DAY) {
					list = sjt.query(SQL_SELECT_NETWORK, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_DAY, days);
				} else if(chartType == DSMAConstants.INTERVAL_WEEK) {
					list = sjt.query(SQL_SELECT_NETWORK, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_WEEK, days);
				} else if(chartType == DSMAConstants.INTERVAL_MONTH) {
					list = sjt.query(SQL_SELECT_NETWORK, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_MONTH, days);
				} else if(chartType == DSMAConstants.INTERVAL_YEAR) {
					list = sjt.query(SQL_SELECT_NETWORK, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_YEAR, days);
				}
			} else if(type == DSMAConstants.ORDER_GRAPH) {
				if(chartType == DSMAConstants.INTERVAL_DAY) {
					list = sjt.query(SQL_SELECT_ORDER_DAY, new TimeGraphicMapper(), 10, 10, 24, 10);
				} else if(chartType == DSMAConstants.INTERVAL_WEEK) {
					list = sjt.query(SQL_SELECT_ORDER, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_WEEK, days);
				} else if(chartType == DSMAConstants.INTERVAL_MONTH) {
					list = sjt.query(SQL_SELECT_ORDER, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_MONTH, days);
				} else if(chartType == DSMAConstants.INTERVAL_YEAR) {
					list = sjt.query(SQL_SELECT_ORDER, new TimeGraphicMapper(), DBConstants.LOAD_USAGE_YEAR, days);
				}
			}
			if(logger.isDebugEnabled()) {
				logger.debug("getLoadInfo for type " + type + " and days " + days + " took " + ((System.currentTimeMillis()-startTime)/1000) + " seconds");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	
	public static List<TimeGraphicMapper> getLoadInfoHour(int type, int hours){
		List<TimeGraphicMapper> list = new ArrayList<TimeGraphicMapper>();
		try {
			long startTime = System.currentTimeMillis();
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			if(type == DSMAConstants.LOAD_GRAPH)
				list = sjt.query(SQL_SELECT_LOAD_HOUR, new TimeGraphicMapper(), hours);
			else if(type == DSMAConstants.CPU_GRAPH)
				list = sjt.query(SQL_SELECT_CPU_HOUR, new TimeGraphicMapper(), hours);
			else if(type == DSMAConstants.MEM_GRAPH)
				list = sjt.query(SQL_SELECT_MEMORY_HOUR, new TimeGraphicMapper(), hours);
			else if(type == DSMAConstants.NETWORK_GRAPH)
				list = sjt.query(SQL_SELECT_NETWORK_HOUR, new TimeGraphicMapper(), hours);
			if(logger.isDebugEnabled()) {
				logger.debug("getLoadInfoHour for type " + type + " and hours " + hours + " took " + ((System.currentTimeMillis()-startTime)) + " miliseconds");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	} 
	
	
	private static final String SQL_DISTINCT_SERVERS = 
		"SELECT DISTINCT " + 
		DBConstants.FIELD_USAGE_INFO_SERVER_NAME + " FROM " + 
		DBConstants.TABLE_USAGE_INFO + " ORDER BY " + 
		DBConstants.FIELD_USAGE_INFO_SERVER_NAME;
	
	private static final String SQL_DISTINCT_ORDERS_SERVERS = 
		"SELECT DISTINCT " + 
		DBConstants.FIELD_SEARCH_STATISTICS_SERVER_NAME + " FROM " + 
		DBConstants.TABLE_SEARCH_STATISTICS + " ORDER BY " + 
		DBConstants.FIELD_SEARCH_STATISTICS_SERVER_NAME;
	
	/**
	 * Returns a list containing all the server names that have info in the database (table ts_usage_info)
	 * @return
	 */
	public static List<String> getDistinctServers(boolean useSearchStatistics){
		List<String> servers = new ArrayList<String>();
		try {
			long startTime = System.currentTimeMillis();
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			String sql = useSearchStatistics?SQL_DISTINCT_ORDERS_SERVERS:SQL_DISTINCT_SERVERS;
			List<Map<String, Object>> data = sjt.queryForList(sql);
			for (Map<String, Object> map : data) {
				servers.add((String)map.get(DBConstants.FIELD_USAGE_INFO_SERVER_NAME));
			}
			if(logger.isDebugEnabled()) {
				logger.debug("getDistinctServers took " + ((System.currentTimeMillis()-startTime)/1000) + " seconds");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return servers;
	}
	
	private static final String SQL_MAX_LOAD_SERVERS = 
		"SELECT max( " +
		DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ") " +
		DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR + ", " +
		DBConstants.FIELD_USAGE_INFO_SERVER_NAME + " FROM " + 
		DBConstants.TABLE_USAGE_INFO + " GROUP BY " + 
		DBConstants.FIELD_USAGE_INFO_SERVER_NAME + " ORDER BY " + 
		DBConstants.FIELD_USAGE_INFO_SERVER_NAME + " ASC ";
	
	/**
	 * Returns a hashmap containing all the server names as keys and the correction factor that will be applied as value 
	 * that have info in the database (table ts_usage_info)
	 * @param type 
	 * @return
	 */
	public static HashMap<String, Float> getCorrectionFactors(int type){
		HashMap<String, Float> servers = new HashMap<String, Float>();
		long startTime = System.currentTimeMillis();
		try {
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			if(type == DSMAConstants.ORDER_GRAPH) {
				List<String> serverNames = getDistinctServers(true); 
				for (String string : serverNames) {
					servers.put(string, 1f);	
				}
			} else {
				List<Map<String, Object>> data = sjt.queryForList(SQL_MAX_LOAD_SERVERS);
				for (Map<String, Object> map : data) {
					if(type == -1 /*DSMAConstants.LOAD_GRAPH*/) {
						servers.put(
								(String)map.get(DBConstants.FIELD_USAGE_INFO_SERVER_NAME),
								100/(Float)map.get(DBConstants.FIELD_LOAD_INFO_LOAD_FACTOR));
					} else if(type == DSMAConstants.CPU_GRAPH || type == DSMAConstants.MEM_GRAPH || type == DSMAConstants.LOAD_GRAPH || type == DSMAConstants.NETWORK_GRAPH) {
						servers.put(
								(String)map.get(DBConstants.FIELD_USAGE_INFO_SERVER_NAME),
								100f);
					} else if(type == DSMAConstants.ORDER_GRAPH) {
						servers.put(
								(String)map.get(DBConstants.FIELD_USAGE_INFO_SERVER_NAME),
								1f);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(logger.isDebugEnabled()) {
			logger.debug("getCorrectionFactors for type " + type + " took " + ((System.currentTimeMillis()-startTime)) + " miliseconds");
		}
		return servers;
	}
	
	public static Logger getLogger() {
		return logger;
	}

	public static void main(String[] args) {
		DBLoadInfo.updateLoadInformationLastYear();
	}

}

package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;

public class OrderProcessedMapper implements ParameterizedRowMapper<OrderProcessedMapper> {

	public static final String TABLE_ORDER_PROCESSED = "order_processed";
	public static final String FIELD_ID	= "id";
	public static final String FIELD_FILE_NAME = "file_name";
	public static final String FIELD_FILE_CONTENT = "file_content";
	public static final String FIELD_ORDER_TYPE = "order_type";
	public static final String FIELD_PROCESS_DATE = "process_date";
	
	public static final int TYPE_NDEX_ORDER_XLS = 1;
	
	@Override
	public OrderProcessedMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static long markFileAlreadyProcessed(String fileName, byte[] byteArray) {
		SimpleJdbcInsert insert = DBManager.getSimpleJdbcInsert().
				withTableName(TABLE_ORDER_PROCESSED).usingGeneratedKeyColumns(FIELD_ID);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(FIELD_PROCESS_DATE, Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
		params.put(FIELD_FILE_NAME, fileName);
		params.put(FIELD_FILE_CONTENT, byteArray);
		params.put(FIELD_ORDER_TYPE, TYPE_NDEX_ORDER_XLS);
		Number key = insert.executeAndReturnKey(params);
		return key.longValue();
		
	}

	public static boolean isFileAlreadyProcessed(String fileName) {
		return DBManager.getSimpleTemplate().queryForInt(
				"select count(*) from " + TABLE_ORDER_PROCESSED + 
				" where " + FIELD_FILE_NAME + " = ?", fileName) > 0;
	}

}

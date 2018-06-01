package ro.cst.tsearch.utils.test;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;


public class DueDatePayDateIncreaser {
	public static void main(String[] args) {
		String sql = "SELECT * FROM " + DBConstants.TABLE_TAX_DATES + " where " + DBConstants.FIELD_TAX_DATES_PAY_DATE + " = '10/01/2009'";
		try {
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			
			
			List<Map<String, Object>> allToBeUpdated =  sjt.queryForList(sql);
			System.out.println(allToBeUpdated);
			
			for (Map<String, Object> map : allToBeUpdated) {
				System.out.println(map.get("name") + " updated DD rows: " + sjt.update(DBManager.SQL_INCREASE_DUE_DATE_BY_YEARS, 1, map.get("name")));
				System.out.println(map.get("name") + " updated PD rows: " + sjt.update(DBManager.SQL_INCREASE_PAY_DATE_BY_YEARS, 1, map.get("name")));
			}
			allToBeUpdated =  sjt.queryForList(sql);
			System.out.println(allToBeUpdated);
			/*
			if(fieldType == IncreaseDueOrPayDate.FIELD_DUE_DATE_YEAR_INCREASE) {
				return sjt.update(SQL_INCREASE_DUE_DATE_BY_YEARS, amount, key);
			} else if(fieldType == IncreaseDueOrPayDate.FIELD_PAY_DATE_YEAR_INCREASE) {
				return sjt.update(SQL_INCREASE_PAY_DATE_BY_YEARS, amount, key);
			}*/
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}

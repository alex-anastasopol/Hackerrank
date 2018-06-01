package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class TaxDatesMapper implements ParameterizedRowMapper<TaxDatesMapper> {

	public static final String TABLE_TAX_DATES = "ts_tax_dates";
	public static final String FIELD_NAME = "name";
	public static final String FIELD_DUE_DATE = "dueDate";
	public static final String FIELD_PAY_DATE = "payDate";
	public static final String FIELD_TAX_YEAR_MODE = "tax_year_mode";
	
	
	private String name;
	private String dueDateString;
	private String payDateString;
	private int taxYearMode;
	
	@Override
	public TaxDatesMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		TaxDatesMapper taxDatesMapper = new TaxDatesMapper();
		taxDatesMapper.setName(resultSet.getString(FIELD_NAME));
		taxDatesMapper.setDueDateString(resultSet.getString(FIELD_DUE_DATE));
		taxDatesMapper.setPayDateString(resultSet.getString(FIELD_PAY_DATE));
		taxDatesMapper.setTaxYearMode(resultSet.getInt(FIELD_TAX_YEAR_MODE));
		return taxDatesMapper;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDueDateString() {
		return dueDateString;
	}

	public void setDueDateString(String dueDateString) {
		this.dueDateString = dueDateString;
	}

	public String getPayDateString() {
		return payDateString;
	}

	public void setPayDateString(String payDateString) {
		this.payDateString = payDateString;
	}

	public int getTaxYearMode() {
		return taxYearMode;
	}

	public void setTaxYearMode(int taxYearMode) {
		this.taxYearMode = taxYearMode;
	}

}

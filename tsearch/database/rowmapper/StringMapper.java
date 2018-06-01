package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class StringMapper implements ParameterizedRowMapper<StringMapper>{

	public static final String STRING_KEY = "STRING_KEY";
	
	private String stringValue = null;
	
	@Override
	public StringMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		StringMapper mapper = new StringMapper();
		mapper.setStringValue(resultSet.getString(STRING_KEY));
		return mapper;
	}

	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

}

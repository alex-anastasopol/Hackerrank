package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class DistrictsNVClarkMapper implements ParameterizedRowMapper<DistrictsNVClarkMapper> {

	public static final String TABLE_DISTRICTS_NV_CLARK = "ts_districts_nv_clark";
	
	public static final String FIELD_DISTRICT = "district";
	public static final String FIELD_NAME = "name";
	public static final String FIELD_CODE = "code";
	
	private String district;
	private String name;
	private String code;
	
	@Override
	public DistrictsNVClarkMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		DistrictsNVClarkMapper districtsNVClarkMapper = new DistrictsNVClarkMapper();
		districtsNVClarkMapper.setDistrict(resultSet.getString(FIELD_DISTRICT));
		districtsNVClarkMapper.setName(resultSet.getString(FIELD_NAME));
		districtsNVClarkMapper.setCode(resultSet.getString(FIELD_CODE));
	
		return districtsNVClarkMapper;
	}
	
	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
		
}

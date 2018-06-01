package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;

public class NameMapper implements ParameterizedRowMapper<NameMapper> {

	private long searchId;
	private NameI name;
	private String color; 
	@Override
	public NameMapper mapRow(ResultSet rs, int rowNum) throws SQLException {
		NameMapper nameMapper = new NameMapper();
		NameI name = new Name();
		nameMapper.setSearchId(rs.getLong(DBConstants.FIELD_PROPERTY_OWNER_SEARCH_ID));
		
		try {
			name.setLastName(rs.getString(DBConstants.FIELD_PROPERTY_OWNER_LAST_NAME));
			name.setFirstName(rs.getString(DBConstants.FIELD_PROPERTY_OWNER_FIRST_NAME));
			name.setMiddleName(rs.getString(DBConstants.FIELD_PROPERTY_OWNER_MIDDLE_NAME));
			name.setSufix(rs.getString(DBConstants.FIELD_PROPERTY_OWNER_SUFFIX));
			name.setCompany(rs.getBoolean(DBConstants.FIELD_PROPERTY_OWNER_IS_COMPANY));
			if(rs.getString(DBConstants.FIELD_PROPERTY_OWNER_COLOR)!=null) {
				nameMapper.setColor(rs.getString(DBConstants.FIELD_PROPERTY_OWNER_COLOR));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		nameMapper.setName(name);
		return nameMapper;
	}

	/**
	 * @return the searchId
	 */
	public long getSearchId() {
		return searchId;
	}

	/**
	 * @param searchId the searchId to set
	 */
	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	/**
	 * @return the name
	 */
	public NameI getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(NameI name) {
		this.name = name;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
	
}

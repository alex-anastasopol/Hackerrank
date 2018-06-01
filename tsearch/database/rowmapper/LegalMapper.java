package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class LegalMapper implements ParameterizedRowMapper<LegalMapper> {

	private long legalId;
	private long searchId;
	private long subdivisionId;
	private long townshipId;
	private String freeForm;
	private String apn;
		
	@Override
	public LegalMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		LegalMapper legal = new LegalMapper();
		legal.setLegalId(resultSet.getLong(DBConstants.FIELD_LEGAL_ID));
		legal.setSearchId(resultSet.getLong(DBConstants.FIELD_LEGAL_SEARCH_ID));
		legal.setSubdivisionId(resultSet.getLong(DBConstants.FIELD_LEGAL_SUBDIVISION_ID));
		legal.setTownshipId(resultSet.getLong(DBConstants.FIELD_LEGAL_TOWNSHIP_ID));
		legal.setFreeForm(resultSet.getString(DBConstants.FIELD_LEGAL_FREEFORM));
		try {
			legal.setApn(resultSet.getString(DBConstants.FIELD_LEGAL_APN));
		} catch (Exception e) {
			
		}
		return legal;
	}

	public long getLegalId() {
		return legalId;
	}

	public void setLegalId(long legalId) {
		this.legalId = legalId;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public long getSubdivisionId() {
		return subdivisionId;
	}

	public void setSubdivisionId(long subdivisionId) {
		this.subdivisionId = subdivisionId;
	}

	public long getTownshipId() {
		return townshipId;
	}

	public void setTownshipId(long townshipId) {
		this.townshipId = townshipId;
	}

	public String getFreeForm() {
		return freeForm;
	}

	public void setFreeForm(String freeForm) {
		this.freeForm = freeForm;
	}

	public String getApn() {
		return apn;
	}

	public void setApn(String apn) {
		this.apn = apn;
	}
	
	

}

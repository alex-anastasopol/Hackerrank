package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class CountyDefaultLegalMapper implements ParameterizedRowMapper<CountyDefaultLegalMapper> {

	private long templateId;
	private long countyId;
	private String defaultLd;
	private boolean overWriteOCRLegal;
	private String defaultLdCondo;
	private boolean overWriteOCRLegalCondo;
	
	
	@Override
	public CountyDefaultLegalMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		CountyDefaultLegalMapper cim = new CountyDefaultLegalMapper();
		cim.setTemplateId(resultSet.getLong(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_ID));
		try {
			cim.setCountyId(resultSet.getLong(DBConstants.FIELD_COUNTY_COMMUNITY_COUNTY_ID));
		}
		catch(Exception e) {}
		cim.setDefaultLd(resultSet.getString(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD));
		cim.setOverWriteOCRLegal(resultSet.getBoolean(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL));
		cim.setDefaultLdCondo(resultSet.getString(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD_CONDO));
		cim.setOverWriteOCRLegalCondo(resultSet.getBoolean(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL_CONDO));
		return cim;
	}

	public CountyDefaultLegalMapper() {}
	
	public CountyDefaultLegalMapper(String countyId) {
		this.countyId = Long.parseLong(countyId);
	}

	public long getTemplateId() {
		return templateId;
	}

	public void setTemplateId(long templateId) {
		this.templateId = templateId;
	}

	public long getCountyId() {
		return countyId;
	}

	public void setCountyId(long countyId) {
		this.countyId = countyId;
	}

	public String getDefaultLd() {
		return defaultLd;
	}

	public void setDefaultLd(String defaultLd) {
		this.defaultLd = defaultLd;
	}
	
	public boolean isOverWriteOCRLegal() {
		return overWriteOCRLegal;
	}

	public void setOverWriteOCRLegal(boolean overWriteOCRLegal) {
		this.overWriteOCRLegal = overWriteOCRLegal;
	}

	public String getDefaultLdCondo() {
		return defaultLdCondo;
	}

	public void setDefaultLdCondo(String defaultLdCondo) {
		this.defaultLdCondo = defaultLdCondo;
	}
	
	public boolean isOverWriteOCRLegalCondo() {
		return overWriteOCRLegalCondo;
	}

	public void setOverWriteOCRLegalCondo(boolean overWriteOCRLegalCondo) {
		this.overWriteOCRLegalCondo = overWriteOCRLegalCondo;
	}

}

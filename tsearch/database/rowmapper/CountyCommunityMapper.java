package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.servers.types.CertificationDateManager;
import ro.cst.tsearch.utils.DBConstants;

public class CountyCommunityMapper implements
		ParameterizedRowMapper<CountyCommunityMapper> {

	private int countyId;
	private int communityId;
	private int defaultStartDateOffset;
	private int defaultCertificationDateOffset;
	private int templateId;
	
	public int getTemplateId() {
		return templateId;
	}
	public void setTemplateId(int templateId) {
		this.templateId = templateId;
	}
	public int getCountyId() {
		return countyId;
	}
	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}
	public int getCommunityId() {
		return communityId;
	}
	public void setCommunityId(int communityId) {
		this.communityId = communityId;
	}
	public int getDefaultStartDateOffset() {
		return defaultStartDateOffset;
	}
	public void setDefaultStartDateOffset(int defaultStartDateOffset) {
		this.defaultStartDateOffset = defaultStartDateOffset;
	}
	public int getDefaultCertificationDateOffset() {
		return defaultCertificationDateOffset;
	}
	public void setDefaultCertificationDateOffset(int defaultCertificationDateOffset) {
		this.defaultCertificationDateOffset = defaultCertificationDateOffset;
	}
	@Override
	public CountyCommunityMapper mapRow(ResultSet resultSet, int rowNum)
			throws SQLException {
		CountyCommunityMapper countyCommunityMapper = new CountyCommunityMapper();
		countyCommunityMapper.setCommunityId(
				resultSet.getInt(DBConstants.FIELD_COUNTY_COMMUNITY_COMMUNITY_ID));
		countyCommunityMapper.setCountyId(
				resultSet.getInt(DBConstants.FIELD_COUNTY_COMMUNITY_COUNTY_ID));
		
		if(resultSet.getObject(DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_CERTIFICATION_DATE_OFFSET) != null) {
			countyCommunityMapper.setDefaultCertificationDateOffset(
					resultSet.getInt(DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_CERTIFICATION_DATE_OFFSET));
		} else {
			countyCommunityMapper.setDefaultCertificationDateOffset(CertificationDateManager.CERTIFICATION_DATE_OFFSET_FOR_EMPTY_INPUT);
		}
		
		if(resultSet.getObject(DBConstants.FIELD_COUNTY_COMMUNITY_TEMPLATE_ID) != null) {
			countyCommunityMapper.setTemplateId(
					resultSet.getInt(DBConstants.FIELD_COUNTY_COMMUNITY_TEMPLATE_ID));
		} else {
			countyCommunityMapper.setTemplateId(-1);
		}
		
		countyCommunityMapper.setDefaultStartDateOffset(
				resultSet.getInt(DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_OFFICIAL_START_DATE_OFFSET));
		return countyCommunityMapper;
	}

}

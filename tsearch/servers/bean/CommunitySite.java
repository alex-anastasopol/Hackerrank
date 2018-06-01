package ro.cst.tsearch.servers.bean;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.DBConstants;

public class CommunitySite implements ParameterizedRowMapper<CommunitySite> {

	private int commId;
	private int countyId;
	private int siteType;
	private int cityTypeP2;
	private int enableStatus;
	
	public int getCommId() {
		return commId;
	}

	public void setCommId(int commId) {
		this.commId = commId;
	}

	public int getCountyId() {
		return countyId;
	}

	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}

	public int getSiteType() {
		return siteType;
	}

	public void setSiteType(int siteType) {
		this.siteType = siteType;
	}

	public int getCityTypeP2() {
		return cityTypeP2;
	}

	public void setCityTypeP2(int cityTypeP2) {
		this.cityTypeP2 = cityTypeP2;
	}

	public int getEnableStatus() {
		return enableStatus;
	}

	public void setEnableStatus(int enableStatus) {
		this.enableStatus = enableStatus;
	}

	@Override
	public CommunitySite mapRow(ResultSet rs, int rowNum) throws SQLException {
		CommunitySite cs = new CommunitySite();
		cs.setCommId(rs.getInt(DBConstants.FIELD_COMMUNITY_SITES_COMMUNITY_ID));
		cs.setCountyId(rs.getInt(DBConstants.FIELD_COMMUNITY_SITES_COUNTY_ID));
		cs.setSiteType(rs.getInt(DBConstants.FIELD_COMMUNITY_SITES_SITE_TYPE));
		cs.setCityTypeP2(rs.getInt(DBConstants.FIELD_COMMUNITY_SITES_CITY_TYPE_P2));
		cs.setEnableStatus(rs.getInt(DBConstants.FIELD_COMMUNITY_SITES_ENABLE_STATUS));
		return cs;
	}

	public long getServerId() {
		return TSServersFactory.getSiteIdfromCountyandServerTypeId(getCountyId(), getSiteType() + 1);
	}

}

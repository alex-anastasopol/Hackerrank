package ro.cst.tsearch.servers.bean;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;


public class SiteTypeEntry implements Comparable<SiteTypeEntry>, ParameterizedRowMapper<SiteTypeEntry> {
	
	private int siteType = -1;

	private String siteAbrev = "";

	private int p2 = -1;

	private String description = "";

	public SiteTypeEntry() {
		
	}
	
	public SiteTypeEntry(int siteType, String siteAbrev, int p2,
			String description) {
		super();
		this.siteType = siteType;
		this.siteAbrev = siteAbrev;
		this.p2 = p2;
		this.description = description;
	}

	public int compareTo(SiteTypeEntry e) {
		if (this.siteType == e.siteType) {
			return 0;
		} else if (this.siteType < e.siteType) {
			return -1;
		}

		return 1;
	}

	public String getDescription() {
		return description;
	}

	public int getP2() {
		return p2;
	}

	public String getSiteAbrev() {
		return siteAbrev;
	}

	public int getSiteType() {
		return siteType;
	}

	@Override
	public SiteTypeEntry mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		SiteTypeEntry entry = new SiteTypeEntry();
		entry.description = resultSet.getString(DBConstants.FIELD_MAP_SITE_TO_P2_DESCRIPTION);
		entry.siteType = resultSet.getInt(DBConstants.FIELD_MAP_SITE_TO_P2_SITE_TYPE);
		entry.siteAbrev = resultSet.getString(DBConstants.FIELD_MAP_SITE_TO_P2_SITE_ABREV);
		entry.p2 = resultSet.getInt(DBConstants.FIELD_MAP_SITE_TO_P2_P2);
		return entry;
	}
}

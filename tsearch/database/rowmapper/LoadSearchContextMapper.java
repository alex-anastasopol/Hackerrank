package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class LoadSearchContextMapper implements ParameterizedRowMapper<LoadSearchContextMapper>{

	private Date sDate;
	private Date sDateBackup;
	private Date unformattedSDate;
	private byte[] document;
	private int version;
	private int toDisk;
	
	public Date getSDate() {
		return sDate;
	}

	public void setSDate(Date sDate) {
		this.sDate = sDate;
	}

	public Date getSDateBackup() {
		return sDateBackup;
	}

	public void setSDateBackup(Date sDateBackup) {
		this.sDateBackup = sDateBackup;
	}

	public Date getUnformattedSDate() {
		return unformattedSDate;
	}

	public void setUnformattedSDate(Date unformattedSDate) {
		this.unformattedSDate = unformattedSDate;
	}

	public byte[] getDocument() {
		return document;
	}

	public void setDocument(byte[] document) {
		this.document = document;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public int getToDisk() {
		return toDisk;
	}

	public void setToDisk(int toDisk) {
		this.toDisk = toDisk;
	}

	@Override
	public LoadSearchContextMapper mapRow(ResultSet rs, int rowNum) throws SQLException {
		LoadSearchContextMapper contextMapper = new LoadSearchContextMapper();
		contextMapper.setDocument(rs.getBytes(DBConstants.FIELD_SEARCH_DATA_CONTEXT));
		contextMapper.setSDate(rs.getTimestamp(DBConstants.FIELD_SEARCH_SDATE));
		contextMapper.setSDateBackup(rs.getTimestamp("sdate_backup"));
		contextMapper.setUnformattedSDate(rs.getTimestamp("unformated_sdate"));
		contextMapper.setToDisk(rs.getInt(DBConstants.FIELD_SEARCH_FLAGS_TO_DISK));
		contextMapper.setVersion(rs.getInt(DBConstants.FIELD_SEARCH_DATA_VERSION));
		return contextMapper;
	}

}

package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class DocumentIndexMapper implements ParameterizedRowMapper<DocumentIndexMapper> {

	private int id;
	private long searchId;
	private byte[] document;
	private Date sDate;
	private Date sDateBackup;
	private Date unformattedSDate;
		
	@Override
	public DocumentIndexMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		DocumentIndexMapper documentIndex = new DocumentIndexMapper();
		documentIndex.setId(resultSet.getInt(DBConstants.FIELD_DOCUMENT_INDEX_ID));
		documentIndex.setSearchId(resultSet.getLong(DBConstants.FIELD_DOCUMENT_INDEX_SEARCHID));
		documentIndex.setDocument(resultSet.getBytes(DBConstants.FIELD_DOCUMENT_INDEX_BLOB));
		documentIndex.setSDate(resultSet.getTimestamp(DBConstants.FIELD_SEARCH_SDATE));
		documentIndex.setSDateBackup(resultSet.getTimestamp("sdate_backup"));
		documentIndex.setUnformattedSDate(resultSet.getTimestamp("unformated_sdate"));
		return documentIndex;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchid) {
		this.searchId = searchid;
	}

	public byte[] getDocument() {
		return document;
	}

	public void setDocument(byte[] document) {
		this.document = document;
	}

	public Date getSDate() {
		return sDate;
	}

	public void setSDate(Date sdate) {
		this.sDate = sdate;
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
	
	

}

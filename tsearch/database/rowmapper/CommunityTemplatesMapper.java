package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import com.stewart.ats.tsrindex.client.template.TemplatesInitUtils;

import ro.cst.tsearch.servlet.DBFileView;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.utils.DBConstants;

public class CommunityTemplatesMapper implements ParameterizedRowMapper<CommunityTemplatesMapper> {
	
	private long id;
	private String name;
	private String shortName;
	private String lastUpdate;
	private String path;
	private int communityId;
	private String fileContent;

	@Override
	public CommunityTemplatesMapper mapRow(ResultSet rs, int rownum) throws SQLException {
		CommunityTemplatesMapper ctm = new CommunityTemplatesMapper();
		ctm.setId(rs.getLong(DBConstants.FIELD_COMMUNITY_TEMPLATES_ID));
		ctm.setCommunityId(rs.getInt(DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID));
		ctm.setName(rs.getString(DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME));
		ctm.setShortName(rs.getString(DBConstants.FIELD_COMMUNITY_TEMPLATES_SHORT_NAME));
		ctm.setLastUpdate(rs.getString(DBConstants.FIELD_COMMUNITY_TEMPLATES_LAST_UPDATE));
		String fileName = rs.getString(DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME);
		ctm.setPath(fileName);
		
		boolean contentIsAvailable = false;
		try {
			rs.findColumn(DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT);
			contentIsAvailable = true;
		} catch (Exception e) {
		}
		
		if(contentIsAvailable) {
			String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
			if (AddDocsTemplates.docDocumentsExtensions.get(fileExtension) != null) {
				ctm.setFileContent(DBFileView.getFileUrl((int) ctm.getId()));
			} else {
				ctm.setFileContent(DBFileView.convertFromBlobToString(
						rs.getBlob(DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT)));
			}
		}
		return ctm;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the shortName
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * @param shortName the shortName to set
	 */
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	/**
	 * @return the lastUpdate
	 */
	public String getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * @param lastUpdate the lastUpdate to set
	 */
	public void setLastUpdate(String lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the communityId
	 */
	public int getCommunityId() {
		return communityId;
	}

	/**
	 * @param communityId the communityId to set
	 */
	public void setCommunityId(int communityId) {
		this.communityId = communityId;
	}

	/**
	 * @return the fileContent
	 */
	public String getFileContent() {
		return fileContent;
	}

	/**
	 * @param fileContent the fileContent to set
	 */
	public void setFileContent(String fileContent) {
		this.fileContent = fileContent;
	}

	/**
	 * Convenience method that determines if this template is a Code Book Library by checking name and path for
	 * markers like <b>BoilerPlates</b> and/or <b>CBLibrary</b>
	 * @return true if this template contains necessary markers
	 */
	public boolean isCodeBookLibrary() {
		try {
			return getPath().contains(TemplatesInitUtils.TEMPLATE_BP_CONTAINS) 
						|| getPath().contains(TemplatesInitUtils.TEMPLATE_CB_CONTAINS);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}

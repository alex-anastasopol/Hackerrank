package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import com.stewart.ats.tsrindex.client.template.TemplatesInitUtils;

import ro.cst.tsearch.utils.DBConstants;

public class CommunityUserTemplatesMapper implements ParameterizedRowMapper<CommunityUserTemplatesMapper> {
		
		private long templateId;
		private int commId;
		private String name;
		private String shortName;
		private String path;
		private long userId;
		private long enableProduct;
		private int exportFormat;

		@Override
		public CommunityUserTemplatesMapper mapRow(ResultSet rs, int rownum) throws SQLException {
			CommunityUserTemplatesMapper utm = new CommunityUserTemplatesMapper();
			try {
				utm.setTemplateId(rs.getLong(DBConstants.FIELD_COMMUNITY_TEMPLATES_ID));
				utm.setCommId(rs.getInt(DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID));
				utm.setName(rs.getString(DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME));
				utm.setShortName(rs.getString(DBConstants.FIELD_COMMUNITY_TEMPLATES_SHORT_NAME));
				utm.setPath(rs.getString(DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME));
				utm.setUserId(rs.getLong(DBConstants.FIELD_USER_TEMPLATES_USER_ID));
				
				long enable = (1<<13)-2050;
				if(rs.getObject(DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT) != null) {
					enable = rs.getLong(DBConstants.FIELD_USER_TEMPLATES_ENABLE_PRODUCT);
				}
				utm.setEnableProduct(enable);
				utm.setExportFormat(rs.getInt(DBConstants.FIELD_USER_TEMPLATES_EXPORT_FORMAT));
			}catch(Throwable t) {
				t.printStackTrace();
			}
			return utm;
		}

		public long getTemplateId() {
			return templateId;
		}

		public void setTemplateId(long templateId) {
			this.templateId = templateId;
		}

		public int getCommId() {
			return commId;
		}

		public void setCommId(int commId) {
			this.commId = commId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getShortName() {
			return shortName;
		}

		public void setShortName(String shortName) {
			this.shortName = shortName;
		}
		
		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public long getUserId() {
			return userId;
		}

		public void setUserId(long userId) {
			this.userId = userId;
		}

		public long getEnableProduct() {
			return enableProduct;
		}

		public void setEnableProduct(long enableProduct) {
			this.enableProduct = enableProduct;
		}
		
		public int getExportFormat() {
			return exportFormat;
		}

		public void setExportFormat(int exportFormat) {
			this.exportFormat = exportFormat;
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
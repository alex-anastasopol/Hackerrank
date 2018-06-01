package ro.cst.tsearch.servlet.update;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Vector;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Category;

import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.templates.UpdateTemplates;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servlet.community.UploadPolicyDoc;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileUtils;

public class UpdateArea extends BaseServlet {

	private static final long serialVersionUID = 1L;
	protected static final Category logger = Category
			.getInstance(UpdateArea.class.getName());

	public static final String UPDATE_CHOICE = "updateChoice";

	public static final int UPDATE_MOVE_SYSTEM_TEMPLATES_TO_DATABASE = 1;
	private static String htmlResponse = "";

	public void doRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		int choiceId = Integer.parseInt(request.getParameter(UPDATE_CHOICE));

		// reset html response
		htmlResponse = "";

		switch (choiceId) {
		case UPDATE_MOVE_SYSTEM_TEMPLATES_TO_DATABASE:

			try {
				moveTemplatesToDatabase();
			} catch (UpdateAreaException uae) {
				logger
						.error("S-a produs o eroare la procesul de update date...");
				uae.printStackTrace();
			}
			break;

		default:
			break;
		}

		sendRedirect(request, response, URLMaping.path
				+ URLMaping.UPDATE_AREA_UPDATE);
	}

	/**
	 * Moves templates to database from filesystem
	 * 
	 * @throws UpdateAreaException
	 */
	private void moveTemplatesToDatabase() throws UpdateAreaException {

		// alter table ts_community_templates if no content column is found
		String sql = "SHOW COLUMNS FROM "
				+ DBConstants.TABLE_COMMUNITY_TEMPLATES + " where Field = '"
				+ DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT + "'";
		Vector data = DBManager.executeSelect(sql);
		if (data.size() > 0) {
			logger.info("Coloana "
					+ DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT
					+ " deja exista in tabela "
					+ DBConstants.TABLE_COMMUNITY_TEMPLATES);
			htmlResponse += "<li>Column "
					+ DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT
					+ " already exists in table "
					+ DBConstants.TABLE_COMMUNITY_TEMPLATES
					+ ".Alter table process aborted.</li>";
		} else {
			sql = "alter table add column content longblob;";
			DBManager.executeSelect(sql);
			logger.info("Adaugat coloana "
					+ DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT
					+ "  in tabela " + DBConstants.TABLE_COMMUNITY_TEMPLATES);
			htmlResponse += "<li>Added column "
					+ DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT
					+ "  in table " + DBConstants.TABLE_COMMUNITY_TEMPLATES
					+ "</li>";

		}

		// drop table
		// sql = "DROP TABLE IF EXISTS ts_user_templates`" ;

		// DBManager.executeSelect(sql);

		// create table
		sql = "	CREATE TABLE IF NOT EXISTS  `ts_user_templates` (      "
				+ "`id` bigint(20) unsigned NOT NULL auto_increment,   "
				+ "`user_id` bigint(20) unsigned NOT NULL,  "
				+ "`template_id` bigint(20) unsigned NOT NULL,	"
				+ "PRIMARY KEY  (`id`),						"
				+ "KEY `fk_ts_user_user_id` (`user_id`),	"
				+ "KEY `fk_ts_community_templates_template_id` (`template_id`),   "
				+ "CONSTRAINT `FK_ts_user_templates$ts_community_templates` FOREIGN KEY (`template_id`) REFERENCES `ts_community_templates` (`template_id`) ON DELETE CASCADE ON UPDATE CASCADE,    "
				+ "CONSTRAINT `FK_ts_user_templates$ts_user` FOREIGN KEY (`user_id`) REFERENCES `ts_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE  "
				+ ") ENGINE=InnoDB DEFAULT CHARSET=latin1;";
		DBManager.executeSelect(sql);

		htmlResponse += "<li>" + sql + "</li>";
		// insert into table ts_community_templates content that is null and a
		// file associated to it exists on the file system
		sql = "select * from " + DBConstants.TABLE_COMMUNITY_TEMPLATES
				+ " where " + DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT
				+ " is null" + " or "
				+ DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT + "=''";
		data = DBManager.executeSelect(sql);

		long templateId;
		String filePath;
		HashMap map;
		DBConnection conn = null;
		PreparedStatement ps = null;
		FileInputStream fis = null;
		File file;
		// loops trough values, and adds the content to database
		for (int i = 0; i < data.size(); i++) {
			try {
				conn = ConnectionPool.getInstance().requestConnection();
				map = (HashMap) data.get(i);

				templateId = Long.parseLong(map.get(
						DBConstants.FIELD_COMMUNITY_TEMPLATES_ID).toString());
				filePath = UploadPolicyDoc.FINAL_TEMPLATES
						+ File.separator
						+ map
								.get(DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID)
						+ File.separator
						+ map
								.get(DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME);
				file = new File(filePath);

				try {
					fis = new FileInputStream(file);

					sql = "UPDATE " + DBConstants.TABLE_COMMUNITY_TEMPLATES
							+ " SET "
							+ DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT
							+ "= ? " + " WHERE "
							+ DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + "="
							+ templateId;

					ps = conn.prepareStatement(sql);
					ps.setBinaryStream(1, (InputStream) fis, (int) file
							.length());
					ps.executeUpdate();

					logger.info("Updatat template id " + templateId
							+ ".Template : " + filePath);
					htmlResponse += "<li><b>Updated template id " + templateId
							+ ".Template : <font color='#de2020'> " + filePath
							+ "</font></b></li>";
					fis.close();

				} catch (FileNotFoundException fnfe) {
					logger.error("Fisierul " + filePath
							+ " nu este disponibil pe sistem");
					htmlResponse += "<li><i>File " + filePath
							+ " is not available on the system</i></li>";
				} catch (IOException io) {
				}
			} catch (SQLException se) {
				se.printStackTrace();
			} catch (BaseException be) {
				be.printStackTrace();
			} finally {
				try {
					ConnectionPool.getInstance().releaseConnection(conn);
				} catch (BaseException e) {
					e.printStackTrace();
				}
			}

		}

		// sets file content to empty when is null
		sql = "update " + DBConstants.TABLE_COMMUNITY_TEMPLATES + " set "
				+ DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT + "=''"
				+ " where " + DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT
				+ " is null";
		DBManager.executeSelect(sql);

		// gets templates inserted for every user
		sql = "select " + DBConstants.FIELD_USER_ID + ","
				+ DBConstants.FIELD_USER_DELIV_TEMPLATES + " from "
				+ DBConstants.TABLE_USER
				+ " where deliv_templates is not null ";
		data = DBManager.executeSelect(sql);

		// deletes and insert into table ts_user_templates
		String whereDelete;
		long userId;
		String[] templateList;
		HashMap<String, Long> fieldsAndValues;
		String templateDelim = ":";

		for (int i = 0; i < data.size(); i++) {
			map = (HashMap) data.get(i);
			userId = Long.parseLong(map.get(DBConstants.FIELD_USER_ID)
					.toString());
			templateList = (map.get(DBConstants.FIELD_USER_DELIV_TEMPLATES)
					.toString()).split(templateDelim);

			whereDelete = DBConstants.FIELD_USER_TEMPLATES_USER_ID + "='"
					+ userId + "'";
			DBManager.executeDelete(DBConstants.TABLE_USER_TEMPLATES,
					whereDelete);
			htmlResponse += "<li>Delete templates for user: " + userId
					+ "</li>";
			for (int j = 0; j < templateList.length; j++)
				if (templateList[j].matches("\\d*")
						&& !templateList[j].equals("")) {
					fieldsAndValues = new HashMap<String, Long>();
					fieldsAndValues.put(
							DBConstants.FIELD_USER_TEMPLATES_USER_ID, userId);
					fieldsAndValues.put(
							DBConstants.FIELD_USER_TEMPLATES_TEMPLATE_ID, Long
									.parseLong(templateList[j]));
					DBManager.executeInsert(DBConstants.TABLE_USER_TEMPLATES,
							fieldsAndValues);
					logger.info("inserez user " + userId + ", template "
							+ templateList[j]);
					htmlResponse += "<li>Insert into table"
							+ DBConstants.TABLE_USER_TEMPLATES + ": user "
							+ userId + ", template " + templateList[j]
							+ "</li>";
				}

		}

		// deletes all templates on the system and regenerate files from the
		// database
		File templatePath = new File(UploadPolicyDoc.getPath());

		FileUtils.deleteDir(templatePath);
		if (!templatePath.isDirectory())
			templatePath.mkdir();

		// regenerate all the templates based on the records in the database
		UpdateTemplates.updateTemplates(UploadPolicyDoc.getPath());

	}

	/**
	 * Retrieves message in html format
	 * 
	 * @return
	 */
	public static String getHtmlResponse() {
		String response = "<table width='70%' align='center'>" + "<tr><td><ul>"
				+ htmlResponse;
		response += "</ul></td></tr></table>";
		htmlResponse = "";
		return response;
	}

}

package ro.cst.tsearch.servlet.repair;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Category;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.URLMaping;

public class FixTemplates extends BaseServlet {

	private static final long serialVersionUID = 1L;
	protected static final Category logger = Category
			.getInstance(FixTemplates.class.getName());

	public static final String FIX_CHOICE = "fixChoice";

	/**
	 * This is fucking stupid but I do not have time to fix it
	 */
	@Deprecated
	public static String retMsg = "retMsg";
	/**
	 * This is fucking stupid but I do not have time to fix it
	 */
	@Deprecated
	private static String msgContent = "";

	public static final int COMMUNITY_TEMPLATES_REDISTRIBUTE = 1;
	public static final int COMMUNITY_TEMPLATES_ELIMINATE_NULL = 2;

	public void doRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		int choiceId = Integer.parseInt(request.getParameter(FIX_CHOICE));

		switch (choiceId) {
		case COMMUNITY_TEMPLATES_REDISTRIBUTE:
			redistributeTemplates();
			break;
		case COMMUNITY_TEMPLATES_ELIMINATE_NULL:
			removeNullTemplates();
			break;
		default:
			break;

		}
		sendRedirect(request, response, URLMaping.path
				+ URLMaping.COMMUNITY_PAGE_ADMIN);

	}

	/**
	 * Redistribute templates to all user
	 * 
	 * @throws FixTemplatesException
	 */
	private void redistributeTemplates() {

		String sql = "";

		// delete all existing
		sql = "delete from " + DBConstants.TABLE_USER_TEMPLATES;
		DBManager.executeSelect(sql);
		// set foreign checks to 0
		sql = "SET FOREIGN_KEY_CHECKS=0";
		DBManager.executeSelect(sql);

		// inserts data into table
		sql = "insert into " + DBConstants.TABLE_USER_TEMPLATES
				+ " (user_id,template_id) " + " select  u."
				+ DBConstants.FIELD_USER_ID + ",ct."
				+ DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + " from "
				+ DBConstants.TABLE_COMMUNITY_TEMPLATES + " ct inner join "
				+ DBConstants.TABLE_USER + " u on ct."
				+ DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID + " = u."
				+ DBConstants.FIELD_USER_COMM_ID + " order by ct."
				+ DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID;
		DBManager.executeSelect(sql);
		// set foreign checkt to 1
		sql = "SET FOREIGN_KEY_CHECKS=0";
		DBManager.executeSelect(sql);

		msgContent = "Users templates have been updated.All users now have all templates assigned within their community";

	}

	/**
	 * remove null templates
	 * 
	 */
	private void removeNullTemplates() {

		String sql = "";

		// delete all null templates
		sql = "delete from " + DBConstants.TABLE_COMMUNITY_TEMPLATES
				+ " where " + DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME
				+ " is null";
		DBManager.executeSelect(sql);

		// sets the messag
		msgContent = "All records null were deleted !!!";
	}

	/**
	 * Get return message
	 * 
	 * @return
	 */
	public static String getRetMsg() {
		String retContent = msgContent;
		msgContent = "";
		return retContent;
	}
}

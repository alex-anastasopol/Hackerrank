/*
 * Created on March 31, 2014
 *
 */
package ro.cst.tsearch.servlet.user;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.user.UserInfoATS2ReportBean;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.user.GroupAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserFilter;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.TSOpCode;

/**
 * @author Olivia
 *
 * used to generate a Report with Users filtered by specific criteria (Role, Hidden Status, Community,..)
 */
public class UserListServlet extends HttpServlet{
	
	
	private static final long	serialVersionUID	= -2974323928821483675L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String operCode 	= request.getParameter(TSOpCode.OPCODE);
		
		int opCode = -1;
		try {
			opCode = Integer.parseInt(operCode);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (opCode == TSOpCode.USERS_LIST_REPORT) {
			String commId 		= request.getParameter(CommunityAttributes.COMMUNITY_ID);
			String commName		= request.getParameter(CommunityAttributes.COMMUNITY_NAME);
			String txtName 		= request.getParameter("userToFindTxt");
			String usersRole 	= request.getParameter(UserAttributes.USER_GROUP);
			String hiddenStatus = request.getParameter(UserAttributes.USER_HIDDEN);
			
			UserFilter uf = new UserFilter(UserFilter.SORT_FULLNAME, UserFilter.SORT_ASC); 

			try {
				List<UserInfoATS2ReportBean> userData = getListOfUsersForReport(uf, true, commId, txtName, hiddenStatus, usersRole);
				String path = exportXlsReport(userData, usersRole, hiddenStatus, commName, txtName);
				
				if (StringUtils.isNotEmpty(path)) {
					File f = new File(path);
					response.setHeader(	"Content-Disposition", " attachment; filename=\"" + FilenameUtils.getName(path) + "\"");
					response.setContentType(".xls");
					response.setContentLength((int)f.length());
					
					InputStream in=new BufferedInputStream(new FileInputStream(f));
					OutputStream out=response.getOutputStream();
					
					byte[] buff=new byte[100];
					int n;
					while ( (n=in.read(buff)) > 0) {
					 	 out.write(buff, 0, n);
					}
					in.close();					 
					out.close();				   	
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}	
	
	public static List<UserInfoATS2ReportBean> getListOfUsersForReport (UserFilter uf, boolean special, String community, String textToFind, String showHidden, String userRole) {
		String stm = "SELECT DISTINCT "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_ID 			+ ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_LASTNAME 		+ ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_FIRSTNAME		+ ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_MIDDLENAME		+ ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_LOGIN			+ ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_COMPANY		+ ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_EMAIL			+ ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_HIDDEN			+ ", "
				+ DBConstants.TABLE_USER + "." + UserAttributes.USER_LASTLOGIN		+ ", "
				+ "g." + GroupAttributes.GROUP_NAME		
				+ " FROM " + DBConstants.TABLE_USER + " join " + 
						DBConstants.TABLE_GROUP + " g on " + DBConstants.TABLE_USER + "." + UserAttributes.USER_GROUP + " = g." + GroupAttributes.GROUP_ID + " ";

		if (uf.getFindFlag())
			stm = stm + uf.getJoinTables();

		stm = stm + " WHERE ";
		
		if ("-1".equals(community)) {
			stm = stm + community + " = -1";
		} else {
			stm = stm + DBConstants.TABLE_USER + "." 
					+ CommunityAttributes.COMMUNITY_ID + "="
					+ Long.parseLong(community);
		}
			

		//use the sort criteria from received UserFilter
		if (uf.getFindFlag())
			stm = stm + uf.getUserFind();
		if (uf.getLikeFlag())
			stm = stm + " AND " + uf.getSortLike();
			
		if("no".equals(showHidden)){
			stm += " AND " + DBConstants.TABLE_USER + "." + UserAttributes.USER_HIDDEN + "=0 ";
		} else if("hidden".equals(showHidden)){
			stm += " AND " + DBConstants.TABLE_USER + "." + UserAttributes.USER_HIDDEN + "=1 ";
		} else if("all".equals(showHidden)){ 
			
		}
		
		if (StringUtils.isNotBlank(userRole) && !"-1".equals(userRole)) {
			stm += " AND " + DBConstants.TABLE_USER + "." + UserAttributes.USER_GROUP + "=" + userRole; 
		}
			
		if (!"(All)".equals(textToFind)) {
			stm += " AND ( " + 
					    "LOWER(" + DBConstants.TABLE_USER + "." + UserAttributes.USER_LOGIN + ") LIKE LOWER('%" + textToFind + "%')" + 
						  " OR " +
					    "LOWER(CONCAT(" + DBConstants.TABLE_USER + "." + UserAttributes.USER_FIRSTNAME + ", " + DBConstants.TABLE_USER + "." + UserAttributes.USER_LASTNAME + ")) LIKE LOWER('%" + textToFind + "%')" +
					")";
		}
		
		if (special) {
			stm = stm + " ORDER BY " + uf.getSortCriteria() + " " + uf.getSortOrder();
		}
		
		List<UserInfoATS2ReportBean> userData = DBManager.getSimpleTemplate().query(stm, new UserInfoATS2ReportBean());
		
		return userData;
	}
	
	public String exportXlsReport(List<UserInfoATS2ReportBean> userData, String userRole, String hiddenStatus, String community, String txtName) {
		String templateName = "usersXLStemplate_ccadmin.jrxml";
		String fileLocation = null;

		// MAKE SURE THE COLUMNS DON'T OVERLAP IN TEMPLATE
		String pathTemplate = ServerConfig.getRealPath() + File.separator + "WEB-INF" + File.separator + "classes"
				+ File.separator + "ro" + File.separator + "cst" + File.separator + "tsearch" + File.separator
				+ "reports" + File.separator + "templates" + File.separator + templateName;

		String hiddenState = "";
		if ("no".equals(hiddenStatus)) {
			hiddenState = "Visible users";
		} else if ("hidden".equals(hiddenStatus)) {
			hiddenState = "Hidden users (in red)";
		} else {
			hiddenState = "All users (hidden in red)";
		}

		String role = "";
		switch (Integer.parseInt(userRole)) {
		case GroupAttributes.AG_ID:
			role = "Agent";
			break;
		case GroupAttributes.ABS_ID:
			role = "Abstractor";
			break;
		case GroupAttributes.CA_ID:
			role = "Community Administrator";
			break;
		case GroupAttributes.CCA_ID:
			role = "All Communities Administrator";
			break;
		case GroupAttributes.AM_ID:
			role = "Executive";
			break;
		default:
			role = "All";
			break;
		}

		Map<String, Object> mapParameter = new HashMap<String, Object>();
		mapParameter.put("CommunityName", community);
		mapParameter.put("HiddenStatus", hiddenState);
		mapParameter.put("Roles", role);
		mapParameter.put("FindUser", (!"(All)".equals(txtName)) ? txtName : " ");

		try {
			mapParameter.put("TotalUsers", Integer.toString(userData.size()));

			String reportFileName = community + "_UsersReport.xls";

			String path = ServerConfig.getTsrCreationTempFolder();
			String directoryName = "userReports" + Long.toString(System.currentTimeMillis());
			File tmpDir = new File("");
			if (StringUtils.isNotEmpty(path)) {
				path += File.separator + directoryName + File.separator;
				tmpDir = new File(path);
				if (!tmpDir.isDirectory())
					tmpDir.mkdir();
			}
			fileLocation = path + File.separator + reportFileName;

			JasperReport jRep = JasperCompileManager.compileReport(pathTemplate);

			if(userData.isEmpty()) {
				userData.add(new UserInfoATS2ReportBean("", "", "", "", "", "", "", false));
			}
			
			JasperPrint jPrint = JasperFillManager.fillReport(jRep, mapParameter, new JRBeanCollectionDataSource(userData));
			JExcelApiExporter excelExporter = new JExcelApiExporter();
			excelExporter.setParameter(JRExporterParameter.JASPER_PRINT, jPrint);
			excelExporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, fileLocation);
			excelExporter.setParameter(JRXlsAbstractExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
			excelExporter.setParameter(JRXlsAbstractExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
			excelExporter.exportReport();

		} catch (Exception e) {
			fileLocation = null;
			e.printStackTrace();
		}

		return fileLocation;
	}
}

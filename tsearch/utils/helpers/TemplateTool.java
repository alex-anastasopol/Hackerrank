package ro.cst.tsearch.utils.helpers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;


public class TemplateTool {
	
	private String sourceServerName = null;
	private Set<Integer> commIds = new LinkedHashSet<Integer>();
	private Set<Integer> templateIds = new LinkedHashSet<Integer>();
	private Set<String> tagsToFind = new LinkedHashSet<String>();
	private Set<String> tagsToMatch = new LinkedHashSet<String>();
	
	private String getSourceServerName(){
		return sourceServerName;
	}
	private void setSourceServerName(String sourceServerName){
		this.sourceServerName = sourceServerName;
	}
	private Set<Integer> getCommIds(){
		return commIds;
	}
	private void setCommIds(Set<Integer> commIds){
		this.commIds = commIds;
	}
	private Set<Integer> getTemplateIds(){
		return templateIds;
	}
	private void setTemplateIds(Set<Integer> templateIds){
		this.templateIds = templateIds;
	}
	private Set<String> getTagsToFind(){
		return tagsToFind;
	}
	private void setTagsToFind(Set<String> tagsToFind){
		this.tagsToFind = tagsToFind;
	}
	
	private Set<String> getTagsToMatch() {
		return tagsToMatch;
	}
	private void setTagsToMatch(Set<String> tagsToMatch) {
		this.tagsToMatch = tagsToMatch;
	}

	public static void main(String[] args) {
		
		TemplateTool templateTool = new TemplateTool();
		
		templateTool.setSourceServerName("atsprd01");
		
		
		templateTool.getTagsToFind().add("firstJullyDateInt");
		//templateTool.getTagsToFind().add("|OTHER-FILE|");
		//templateTool.getTagsToMatch().add("\\|\\s*Other-File\\s*\\|");
		
		//templateTool.getCommIds().add(3);
		
//		templateTool.dumpTemplates("SELECT * from ts_community_templates");
//		
//		
		templateTool.dumpTemplates("SELECT ct.template_id, ct.comm_id, c.comm_name, cat.categ_name, ct.name, ct.path, ct.last_update, ct.short_name, ct.content "
				+ "FROM ts_community_templates ct "
				+ "join ts_community c on c.comm_id = ct.comm_id "
				+ "join ts_category cat on cat.categ_id = c.categ_id "
				+ "where cat.categ_name not in ('Test', 'Closed') order by comm_id");
//		
//		System.out.println(templateTool.verifyTemplates());
	}
	
	
	public void dumpTemplates(String sql) {
		Connection conn = null;
		Statement stm = null;
		try {
			conn = SyncTool.getConnectionServer(sourceServerName);
			stm = conn.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(conn == null || stm == null) {
			throw new RuntimeException("Could not initialize connection to " + sourceServerName);
		}
		Calendar cal = Calendar.getInstance();
		String thisDay = FormatDate.getDateFormat(FormatDate.PATTERN_yyyyMMdd).format(cal.getTime());
		String pathOnDisk = "C:\\dump\\" + thisDay + "\\" + sourceServerName + "\\";
		
		System.out.println("Dumping to " + pathOnDisk + "... ");
		
		try {
			System.out.println("Executing " + sql);
			ResultSet rs = stm.executeQuery(sql);
			System.out.println("Finished sql with " + rs.getFetchSize() + " fetched rows");
			
			while (rs.next()){				
				ResultSetMetaData metaData = rs.getMetaData();
				
				String template_id = "", comm_id = "", name = "";
				byte[] content = null;
				
				for (int i = 1; i <= metaData.getColumnCount(); i++){
					
					if("template_id".equals(metaData.getColumnName(i))) {
						template_id = rs.getString(i);
					} else if("comm_id".equals(metaData.getColumnName(i))) {
						comm_id = rs.getString(i);
					} else if("path".equals(metaData.getColumnName(i))) {
						name = rs.getString(i);
					} else if("content".equals(metaData.getColumnName(i))) {
						content = rs.getBytes(i);
					}
				}
				
				if (content != null){
					File file = new File(pathOnDisk + "\\" + comm_id + "\\" + template_id + "__" + name);
					FileUtils.writeByteArrayToFile(file, content);
					
					System.out.println("Dumped file " + file.getAbsolutePath());
				}
			}
			
			System.out.println("Finished with succes");
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Finished with error");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Finished with error");
		}
		
	}
	
	public String verifyTemplates(){
		
		Connection conn = null;
		Statement stm = null;
		try {
			conn = SyncTool.getConnectionServer(sourceServerName);
			stm = conn.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(conn == null || stm == null) {
			throw new RuntimeException("Could not initialize connection to " + sourceServerName);
		}
		Calendar cal = Calendar.getInstance();
		String thisDay = FormatDate.getDateFormat(FormatDate.PATTERN_yyyyMMdd).format(cal.getTime());
		String pathOnDisk = "C:\\dump\\" + thisDay + "\\" + sourceServerName + "\\";
		
		String sql = "SELECT * FROM " + DBConstants.TABLE_COMMUNITY_TEMPLATES;
		
//		String commId = "";
//		if (commIds != null && !commIds.isEmpty()){
//			for (Integer cnt : commIds) {
//				if(commId.isEmpty()) {
//					commId = " WHERE community_id in (" + cnt;		
//				} else {
//					commId += ", " + cnt;
//				}
//			}
//			
//			commId += ") ";
//			sql += commId;
//		}
//		
//		String templateId = "";
//		if (templateIds != null && !templateIds.isEmpty()){
//			for (Integer cnt : templateIds) {
//				if(templateId.isEmpty()) {
//					templateId = " template_id in (" + cnt;		
//				} else {
//					templateId += ", " + cnt;
//				}
//			}
//			
//			templateId += ") ";
//			if (commId.isEmpty()){
//				sql += " WHERE ";
//			} else{
//				sql += " AND template_id in (" + templateIds.toArray() + ")";
//			}
//		}
		
		StringBuffer resultContainig = new StringBuffer();
		int foundContaining = 0;
		
		StringBuffer resultMatching = new StringBuffer();
		int foundMatching = 0;
		
		try {
			System.out.println("Executing " + sql);
			ResultSet rs = stm.executeQuery(sql);
			System.out.println("Finished sql with " + rs.getFetchSize() + " fetched rows");
			
			while (rs.next()){				
				ResultSetMetaData metaData = rs.getMetaData();
				
				String template_id = "", comm_id = "", name = "";
				byte[] content = null;
				
				for (int i = 1; i <= metaData.getColumnCount(); i++){
					
					if("template_id".equals(metaData.getColumnName(i))) {
						template_id = rs.getString(i);
					} else if("comm_id".equals(metaData.getColumnName(i))) {
						comm_id = rs.getString(i);
					} else if("path".equals(metaData.getColumnName(i))) {
						name = rs.getString(i);
					} else if("content".equals(metaData.getColumnName(i))) {
						content = rs.getBytes(i);
					}
				}
				if (content != null){
					String contentAsString = IOUtils.toString(new ByteArrayInputStream(content));
					if (StringUtils.isNotEmpty(contentAsString)){
						for (String tag :  tagsToFind){
							if (contentAsString.contains(tag)){
								File file = new File(pathOnDisk + "\\" + comm_id + "\\" + name);
								FileUtils.writeByteArrayToFile(file, content);
								
								String line = getLineThatContainTag(file, tag, false);
								//resultContainig.append(template_id).append(",");
								resultContainig.append("TemplateID: ").append(template_id).append(" from comm_id: ").append(comm_id).append(" and path: ").append(name)
										.append(" contains tag:").append(tag).append(" in line: ").append(line).append("\n");
								foundContaining++;
							}
						}
						
						for (String tag :  tagsToMatch){
							if (contentAsString.matches("(?is).*" + tag + ".*")){
								File file = new File(pathOnDisk + "\\" + comm_id + "\\" + name);
								FileUtils.writeByteArrayToFile(file, content);
								
								String line = getLineThatContainTag(file, tag, true);
								
								//resultMatching.append(template_id).append(",");
								
								resultMatching.append("TemplateID: ").append(template_id).append(" from comm_id: ").append(comm_id).append(" and path: ").append(name)
										.append(" match tag:").append(tag).append(" in line: ").append(line).append("\n");
								foundMatching++;
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		StringBuffer result = new StringBuffer();
		if (resultContainig.length() > 0){
			result.append(resultContainig).append("Found Containing: ").append(foundContaining);
		}
		
		if (resultMatching.length() > 0){
			result.append("\n\n").append(resultMatching).append("Found Matching: ").append(foundMatching);
		}
		
		if (result.length() == 0){
			return "No template found with tags specified: " + tagsToFind.toArray();
		}
		
		return result.toString();
	}
	
	public String getLineThatContainTag(File file, String tag, boolean useRegex) throws FileNotFoundException, IOException {
		String line = "";
		StringBuffer allLines = new StringBuffer();
		
		BufferedReader in = new BufferedReader(new FileReader(file));
		
		if (useRegex){
			while ((line = in.readLine()) != null){
				if (line.matches("(?is).*" + tag + ".*")){
					allLines.append(line).append("\n");
				}
			}
		} else{
			while ((line = in.readLine()) != null){
				if (line.contains(tag)){
					allLines.append(line).append("\n");
				}
			}
		}
		
		in.close();
		
		return allLines.toString();
	}
	
}

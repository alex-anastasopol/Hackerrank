package ro.cst.tsearch.utils.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.XStreamManager;
import ro.cst.tsearch.utils.ZipUtils;

import com.Ostermiller.util.CSVParser;
import com.stewart.ats.base.search.DocumentsManagerI;

public class TemplateOnOrderFinder {
	
	private XStreamManager xstream = XStreamManager.getInstance();
	
	private String filePath;
	private String[][] originalRows;
	private List<String[]> parsedRows;
	
	public TemplateOnOrderFinder(String filePath) {
		this.filePath = filePath;
	}

	public static void main(String[] args) {
		
		BaseServlet.REAL_PATH = "F:\\resin\\webapps\\title-search\\web\\";
		
//		TemplateOnOrderFinder finder = new TemplateOnOrderFinder("F:\\ATSFolder\\FL.csv");
//		TemplateOnOrderFinder finder = new TemplateOnOrderFinder("F:\\ATSFolder\\FLFailedCleaned.csv");
		TemplateOnOrderFinder finder = new TemplateOnOrderFinder("F:\\ATSFolder\\TN\\TNrun.csv");
		
		boolean operationOk = finder.readInput();
		if(!operationOk) {
			System.err.println("readInput for file " + finder.filePath + " failed");
			return ;
		}
		finder.findProblems();
		
//		finder.findTemplates();
		
//		long searchId = 4146641;
//		String contextPath = "D:\\bugs\\templates\\" + searchId + File.separator;
//		
//		try {
//			ZipUtils.unzipContext( FileUtils.readFileToByteArray(new File("D:\\bugs\\templates\\" + searchId + "_v000036.zip")), contextPath, searchId );
//			
//			File file =new File(contextPath + "__search.xml");
//        	
//        	if( file.length()< 40 * 1024 * 1024){
//				
//				Search search = (Search)xstream.fromXML( FileUtils.readFileToString(new File(contextPath + "__search.xml")) );
//				
//				HashMap<String, String> generatedTemp = search.getGeneratedTemp();
//				boolean foundSavedATS = false;
//				for (String key : generatedTemp.keySet()) {
//					if(key.endsWith("ats") || generatedTemp.get(key).toLowerCase().endsWith(".ats")) {
//						foundSavedATS = true;
//						break;
//					}
//				}
//				if(foundSavedATS) {
//					System.out.println("Stam bine de tot");
//				} else {
//					System.out.println("am belit-o");
//				}
//				
//			} 
//			
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
				
		
		
	}
	
//	private void findProblems() {
//		List<String[]> withoutTemplate = new ArrayList<>();
//		List<String[]> withProblems = new ArrayList<>();
//		int i = 0;
//		for (String[] row : originalRows) {
//			if(i==0) {
//				i++;
//				continue;
//			}
//			System.out.println("Trying row " + i + " out of " + originalRows.length);
//			
//			try {
//				if(!hasATSSaved(
//						row[0], 								//search_id
//						Integer.parseInt(row[4]),				//version
//						Integer.parseInt(row[5]),				//year
//						Integer.parseInt(row[6]),				//month	
//						Integer.parseInt(row[7]))) {				//day
//					withoutTemplate.add(row);
//					System.err.println("and failed " + Arrays.toString(row));
//				}
//			} catch (Exception e) {
//				System.err.println("Failed--------------------------------------------------");
//				e.printStackTrace();
//				withProblems.add(row);
//			}
//			i++;
//		}
//		
//		System.out.println("--------------------------------------------------------------------");
//		System.out.println("--------------------------------------------------------------------");
//		System.out.println("--------------------------------------------------------------------");
//		System.out.println("--------------------------------------------------------------------");
//		
//		if(withoutTemplate.size() > 0) {
//			for (String[] strings : withoutTemplate) {
//				System.err.println("\tNot template for " + Arrays.toString(strings));	
//			}
//		} else {
//			System.out.println("NO order without template found");
//		}
//		
//		if(withProblems.size() > 0) {
//			for (String[] strings : withProblems) {
//				System.err.println("\tSomething is wrong with " + Arrays.toString(strings));	
//			}
//		} else {
//			System.out.println("All orders were processed without errors");
//		}
//		
//	}
	
	private void findProblems() {
		
		Connection drDBConn = null;
		try {
			drDBConn = SyncTool.getDrDBConn();
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(drDBConn == null) {
			System.err.println("Cannot get DB connection");
			return;
		}
		
		List<String[]> withoutTemplate = new ArrayList<>();
		List<String[]> withProblems = new ArrayList<>();
		
		List<String[]> all = new ArrayList<>();
		
		int i = 0;
		for (String[] row : originalRows) {
			System.out.println("Trying row " + (++i) + " out of " + originalRows.length + " with SID: " + row[0]);
			
			String[] filledRow = new String[row.length + 2];
			for (int j = 0; j < row.length; j++) {
				filledRow[j] = row[j];
			}
			filledRow[row.length] = "";
			filledRow[row.length + 1] = "";
					
			try {
				if(!hasATSSaved(
						row[0], 								//search_id
						Integer.parseInt(row[4]),				//version
						Integer.parseInt(row[5]),				//year
						Integer.parseInt(row[6]),				//month	
						Integer.parseInt(row[7]),				//day
						Integer.parseInt(row[9]),				//toDisk
						drDBConn
						)) {			
					withoutTemplate.add(row);
				} else {
					filledRow[row.length] = "OK";
					filledRow[row.length + 1] = "Update/No docs found";
				}
			} catch (Exception e) {
				System.err.println("Failed--------------------------------------------------");
				e.printStackTrace();
				withProblems.add(row);
			}
			
			all.add(filledRow);
		}
		
		System.out.println("--------------------------------------------------------------------");
		System.out.println("--------------------------------------------------------------------");
		System.out.println("--------------------------------------------------------------------");
		System.out.println("--------------------------------------------------------------------");
		
		if(withoutTemplate.size() > 0) {
			for (String[] strings : withoutTemplate) {
				System.err.println("\tNot template for " + Arrays.toString(strings));	
			}
		} else {
			System.out.println("NO order without template found");
		}
		
		if(withProblems.size() > 0) {
			for (String[] strings : withProblems) {
				System.err.println("\tSomething is wrong with " + Arrays.toString(strings));	
			}
		} else {
			System.out.println("All orders were processed without errors");
		}
		
		System.out.println("--------------------------------------------------------------------");
		System.out.println("--------------------------------------------------------------------");
		System.out.println("--------------------------------------------------------------------");
		System.out.println("--------------------------------------------------------------------");
		
		
		for (String[] strings : all) {
			System.out.println(Arrays.toString(strings));
		}
		
	}

//	private boolean hasATSSaved(String searchId, int version, int year, int month, int day) throws NumberFormatException, IOException {
//		String startContext = "\\\\192.168.219.15\\PIC_ATS_Data03\\search_context";
//		String contextPath = startContext + File.separator + 
//				year + File.separator + 
//				org.apache.commons.lang.StringUtils.leftPad(Long.toString(month), 2, '0') + File.separator + 
//				org.apache.commons.lang.StringUtils.leftPad(Long.toString(day), 2, '0') + File.separator + searchId;
//		
//		String correctPath = null;
//		
//		if(new File(contextPath).isDirectory()) {
//			correctPath = contextPath;
//		} else {
//			Calendar cal = new GregorianCalendar(year, month - 1 , day);
//			cal.add(Calendar.DAY_OF_MONTH, 1);
//			contextPath = startContext + File.separator + 
//					cal.get(Calendar.YEAR) + File.separator + 
//					org.apache.commons.lang.StringUtils.leftPad(Integer.toString((cal.get(Calendar.MONTH) + 1)), 2, '0') + File.separator + 
//							org.apache.commons.lang.StringUtils.leftPad(Integer.toString(cal.get(Calendar.DAY_OF_MONTH)), 2, '0') + File.separator + searchId;
//			if(new File(contextPath).isDirectory()) {
//				correctPath = contextPath;
//			} else {
//				cal.add(Calendar.DAY_OF_MONTH, -2);
//				contextPath = startContext + File.separator + 
//						cal.get(Calendar.YEAR) + File.separator + 
//						(cal.get(Calendar.MONTH) + 1) + File.separator + 
//						cal.get(Calendar.DAY_OF_MONTH) + File.separator + searchId;
//				if(new File(contextPath).isDirectory()) {
//					correctPath = contextPath;
//				} else {
//					throw new RuntimeException("Cannot find folder on disk");
//				}
//			}
//		}
//		
//		File orderZip = new File(correctPath + File.separator + searchId + "_v" + org.apache.commons.lang.StringUtils.leftPad(Long.toString(version), 6, '0') + ".zip");
//		
//		if(!orderZip.exists()) {
//			throw new RuntimeException("Cannot find file " + orderZip.getPath());
//		}
//		
//			
//		String unzipFolder = "F:\\ATSFolder\\temp\\" + searchId + File.separator;
//		
//		ZipUtils.unzipContext( FileUtils.readFileToByteArray(orderZip), unzipFolder, Long.parseLong(searchId) );
//		
//		Search search = (Search)xstream.fromXML( FileUtils.readFileToString(new File(unzipFolder + "__search.xml")) );
//		
//		HashMap<String, String> generatedTemp = search.getGeneratedTemp();
//		for (String key : generatedTemp.keySet()) {
//			if(key.endsWith("ats") || generatedTemp.get(key).toLowerCase().endsWith(".ats")) {
//				return true;
//			}
//		}
//			
//		return false;
//	}

	private boolean hasATSSaved(String searchId, int version, int year, int month, int day, int toDisk, Connection conn) throws NumberFormatException, IOException {
		String startContext = "\\\\192.168.219.15\\PIC_ATS_Data03\\search_context";
		String contextPath = startContext + File.separator + 
				year + File.separator + 
				org.apache.commons.lang.StringUtils.leftPad(Long.toString(month), 2, '0') + File.separator + 
				org.apache.commons.lang.StringUtils.leftPad(Long.toString(day), 2, '0') + File.separator + searchId;
		
		String correctPath = null;
		String correctArchive = null;
		
		if(new File(contextPath).isDirectory()) {
			correctPath = contextPath;
		} else {
			Calendar cal = new GregorianCalendar(year, month - 1 , day);
			cal.add(Calendar.DAY_OF_MONTH, 1);
			contextPath = startContext + File.separator + 
					cal.get(Calendar.YEAR) + File.separator + 
					org.apache.commons.lang.StringUtils.leftPad(Integer.toString((cal.get(Calendar.MONTH) + 1)), 2, '0') + File.separator + 
					org.apache.commons.lang.StringUtils.leftPad(Integer.toString(cal.get(Calendar.DAY_OF_MONTH)), 2, '0') + File.separator + searchId;
			if(new File(contextPath).isDirectory()) {
				correctPath = contextPath;
			} else {
				cal.add(Calendar.DAY_OF_MONTH, -2);
				contextPath = startContext + File.separator + 
						cal.get(Calendar.YEAR) + File.separator + 
						org.apache.commons.lang.StringUtils.leftPad(Integer.toString((cal.get(Calendar.MONTH) + 1)), 2, '0') + File.separator + 
						org.apache.commons.lang.StringUtils.leftPad(Integer.toString(cal.get(Calendar.DAY_OF_MONTH)), 2, '0') + File.separator + searchId;
				if(new File(contextPath).isDirectory()) {
					correctPath = contextPath;
				} else {
					correctPath = null;
					if(toDisk == 1) {
						String startArchiveFolder = "\\\\192.168.219.15\\PIC_ATS_Data\\archives"  + File.separator;
						String archiveFile = startArchiveFolder + year + "_" + 
								org.apache.commons.lang.StringUtils.leftPad(Long.toString(month), 2, '0') + "_" + org.apache.commons.lang.StringUtils.leftPad(Long.toString(day), 2, '0') +  
								"/" + searchId + 
								"/"+ searchId + ".zip";
						if(new File(archiveFile).exists()) {
							correctArchive = archiveFile;
						} else {
							cal.add(Calendar.DAY_OF_MONTH, 2);
							archiveFile = startArchiveFolder + cal.get(Calendar.YEAR) + "_" + 
									org.apache.commons.lang.StringUtils.leftPad(Integer.toString((cal.get(Calendar.MONTH) + 1)), 2, '0') + "_" + 
									org.apache.commons.lang.StringUtils.leftPad(Integer.toString(cal.get(Calendar.DAY_OF_MONTH)), 2, '0') +  
									"/" + searchId + 
									"/"+ searchId + ".zip";
							if(new File(archiveFile).exists()) {
								correctArchive = archiveFile;
							} else {
								cal.add(Calendar.DAY_OF_MONTH, -2);
								archiveFile = startArchiveFolder + cal.get(Calendar.YEAR) + "_" + 
										org.apache.commons.lang.StringUtils.leftPad(Integer.toString((cal.get(Calendar.MONTH) + 1)), 2, '0') + "_" + 
										org.apache.commons.lang.StringUtils.leftPad(Integer.toString(cal.get(Calendar.DAY_OF_MONTH)), 2, '0') +  
										"/" + searchId + 
										"/"+ searchId + ".zip";
								if(new File(archiveFile).exists()) {
									correctArchive = archiveFile;
								} else {
									throw new RuntimeException("Cannot find archive on disk");
								}
							}
						}
						
					} else {
						
						try {
					       	Statement stmt = conn.createStatement();
					       	
					  	   
					       	String s ="select * from ts_search_data1 where searchId = "+searchId;
					       	ResultSet query = stmt.executeQuery(s);
					       	if(query.next()) {
					       		Blob blob = query.getBlob("context");
					       		FileUtils.writeByteArrayToFile(new File("F:\\ATSFolder\\tempdb\\" + searchId + ".zip"), IOUtils.toByteArray(blob.getBinaryStream()));
					       		correctArchive = "F:\\ATSFolder\\tempdb\\" + searchId + ".zip";
					       	} else {
					       		throw new RuntimeException("Cannot find data on database");
					       	}
					       	
						} catch (Exception e) {
							throw new RuntimeException(e);	
						}
						
					}
				}
			}
		}
		
		File orderZip = null;
		if(correctPath != null) {
			orderZip = new File(correctPath + File.separator + searchId + "_v" + org.apache.commons.lang.StringUtils.leftPad(Long.toString(version), 6, '0') + ".zip");
		} else {
			orderZip = new File(correctArchive);
		}
		
		if(!orderZip.exists()) {
			throw new RuntimeException("Cannot find file " + orderZip.getPath());
		}
		
			
		String unzipFolder = "F:\\ATSFolder\\temp\\" + searchId + File.separator;
		
		ZipUtils.unzipContext( FileUtils.readFileToByteArray(orderZip), unzipFolder, Long.parseLong(searchId) );
		
		Search search = (Search)xstream.fromXML( FileUtils.readFileToString(new File(unzipFolder + "__search.xml")) );
		
		HashMap<String, String> generatedTemp = search.getGeneratedTemp();
		for (String key : generatedTemp.keySet()) {
			if(key.endsWith("ats") || generatedTemp.get(key).toLowerCase().endsWith(".ats")) {
				return true;
			}
		}
		
		if(search.isProductType(Products.UPDATE_PRODUCT)) {
			DocumentsManagerI docManager = search.getDocManager();
			try {
				docManager.getAccess();
				
				if(!ro.cst.tsearch.templates.TemplateUtils.foundRoDocs(docManager, true)) {
					return true;
				}
				
			} finally {
				docManager.releaseAccess();
			}
		}
			
		return false;
	}

	private boolean readInput() {
		try {
			CSVParser parser = new CSVParser(new FileInputStream(filePath));
			originalRows = parser.getAllValues();
			if(originalRows == null) {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void findTemplates() {
		List<String[]> withoutTemplate = new ArrayList<>();
		List<String[]> withProblems = new ArrayList<>();
		List<String[]> withTemplateNotBlank = new ArrayList<>();
		Connection drDBConn = null;
		try {
			drDBConn = SyncTool.getDrDBConn();
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(drDBConn == null) {
			System.err.println("Cannot get DB connection");
			return;
		}
		
		int i = 0;
		for (String[] row : originalRows) {
			System.out.println("Trying row " + (++i) + " out of " + originalRows.length);
			
			String sql = "select ct.name, ct.path from ts_search s join ts_user_templates ut on s.agent_id = ut.user_id "
					+ "join ts_community_templates ct on ct.template_id = ut.template_id "
					+ "where ct.path like '%.ats' and s.id = " + row[0] + " and ((1<<s.search_type) & ut.enableProduct) != 0";
			
			try {
				Statement stmt = drDBConn.createStatement();
				ResultSet resultSet = stmt.executeQuery(sql);
				
				if(resultSet.next()) {
					String path = resultSet.getString("path");
					String name = resultSet.getString("name");
					
					if(name.equals("SO Blank Template")) {
						//TODO: success
					} else if(path.contains("Blank")) {
						System.out.println("\tFound name: " + name + " and path: " + path + " but cannot determine if it's good");
					} else {
						System.err.println("\tFound name: " + name + " and path: " + path + " and they don't contain \"blank\"");
						withTemplateNotBlank.add(row);
					}
				} else {
					withoutTemplate.add(row);
				}
			} catch (Exception e) {
				withProblems.add(row);
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
		}
		
		System.err.println("--------------------------------------------------------------------");
		System.err.println("--------------------------------------------------------------------");
		System.err.println("--------------------------------------------------------------------");
		System.err.println("--------------------------------------------------------------------");
		
		if(withoutTemplate.size() > 0) {
			for (String[] strings : withoutTemplate) {
				System.err.println("\tNo template for " + Arrays.toString(strings));	
			}
		} else {
			System.out.println("NO order without template found - which is good");
		}
		System.err.println("--------------------------------------------------------------------");
		System.err.println("--------------------------------------------------------------------");
		
		if(withTemplateNotBlank.size() > 0) {
			for (String[] strings : withTemplateNotBlank) {
				System.err.println("\tNo blank template for " + Arrays.toString(strings));	
			}
		} else {
			System.out.println("NO order without blank template found - which is good");
		}
		System.err.println("--------------------------------------------------------------------");
		System.err.println("--------------------------------------------------------------------");
		
		if(withProblems.size() > 0) {
			for (String[] strings : withProblems) {
				System.err.println("\tSomething is wrong with " + Arrays.toString(strings));	
			}
		} else {
			System.out.println("All orders were processed without errors - which is good");
		}
		System.err.println("--------------------------------------------------------------------");
		System.err.println("--------------------------------------------------------------------");
		
	}
}

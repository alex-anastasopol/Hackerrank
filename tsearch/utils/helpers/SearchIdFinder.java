package ro.cst.tsearch.utils.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.community.Products;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;

public class SearchIdFinder {
	
	private static final int IDX_SEARCH_ID = 0;
	private static final int IDX_SERVER_NAME = 1;
	private static final int IDX_COMM_NAME = 2;
	private static final int IDX_FILE_ID = 3;
	private static final int IDX_COUNTY = 4;
	private static final int IDX_SEARCH_TYPE = 5;
	
	private static final int IDX_SDATE = 6;
	private static final int IDX_TSR_DATE = 7;
	private static final int IDX_COUNT_DATE = 8;
	private static final int IDX_OUT_OF_RANGE = 9;
	
	private static final String SQL_FIND_SEARCH_BETWEEN = 
			//"select c.comm_name, s.file_id, cnt.name, DATE_FORMAT(s.sdate, '%m/%d/%Y %h:%i %p'), if(s.tsr_date is null, '', DATE_FORMAT(s.tsr_date, '%m/%d/%Y %h:%i %p')) " +
			"select c.comm_name, s.file_id, cnt.name, search_type, s.id " +
			"from ts_search s " +
			"join ts_community c on c.comm_id = s.comm_id " +
			"join ts_property p on s.property_id = p.id " +
			"join ts_county cnt on p.county_id = cnt.id " +
			"where s.sdate between ? and ? and cnt.id in ($cnt$) and search_type != 10 and s.comm_id = 71";
	
	private static final String SQL_FIND_SEARCH = 
			//"select c.comm_name, s.file_id, cnt.name, DATE_FORMAT(s.sdate, '%m/%d/%Y %h:%i %p'), if(s.tsr_date is null, '', DATE_FORMAT(s.tsr_date, '%m/%d/%Y %h:%i %p')) " +
			"select c.comm_name, s.file_id, cnt.name, search_type, DATE_FORMAT(s.sdate, '%m/%d/%Y %h:%i %p'), if(s.tsr_date is null, '', DATE_FORMAT(s.tsr_date, '%m/%d/%Y %h:%i %p')) " +
			"from ts_search s " +
			"join ts_community c on c.comm_id = s.comm_id " +
			"join ts_property p on s.property_id = p.id " +
			"join ts_county cnt on p.county_id = cnt.id " +
			"where s.id = ?";
	
	private static final String SQL_FIND_SEARCH_OUT_OF_RANGE = 
			//"select c.comm_name, s.file_id, cnt.name, DATE_FORMAT(s.sdate, '%m/%d/%Y %h:%i %p'), if(s.tsr_date is null, '', DATE_FORMAT(s.tsr_date, '%m/%d/%Y %h:%i %p')) " +
			"select c.comm_name, s.file_id, cnt.name, search_type, DATE_FORMAT(s.sdate, '%m/%d/%Y %h:%i %p'), if(s.tsr_date is null, '', DATE_FORMAT(s.tsr_date, '%m/%d/%Y %h:%i %p')) " +
			", DATE_FORMAT(date_counted, '%m/%d/%Y %h:%i %p') " +
			"from ts_search s " +
			"join ts_community c on c.comm_id = s.comm_id " +
			"join ts_property p on s.property_id = p.id " +
			"join ts_county cnt on p.county_id = cnt.id " +
			"join ts_order_count oc on oc.file_id = s.file_id and oc.community_id = s.comm_id and oc.county_id = p.county_id " +
			"where s.id = ? and  datasource = 13";
	
	private static final String SQL_FIND_ORDER_COUNT = 
			"select *, DATE_FORMAT(date_counted, '%c/%e/%Y') cdf, com.comm_name  "
			+ "from ts_order_count oc join ts_county c on oc.county_id = c.id "
			+ "join ts_community com on oc.community_id = com.comm_id "
			+ "where REPLACE(REPLACE(REPLACE(file_id, '-',''), '_',''), '~','') like ? and datasource = ?";
	
	
	private String filePath;
	private String[][] originalRows;
	private List<String[]> parsedRows;
	
	private Map<String, String[]> updates;
	
	public SearchIdFinder(String filePath) {
		this.filePath = filePath;
	}

	public static void main(String[] args) {
		SearchIdFinder finder = new SearchIdFinder("D:\\documents\\reports\\DT\\DT_mar_completed.csv");
		
//		String newFilePath = finder.readAndGiveResults();
		
		
		boolean operationOk = finder.readInput();
		if(!operationOk) {
			System.err.println("readInput failed");
			return ;
		}
		
		finder.readUpdates("D:\\documents\\reports\\DT\\updates.csv");
		
		CSVPrinter csvPrinter = finder.getResultWriter();
		
//		operationOk = finder.updateInfoInternalDTOrigReport(csvPrinter);
//		if(!operationOk) {
//			System.err.println("updateInfoInternal failed");
//			return ;
//		}
		
		finder.updateByUpdates(csvPrinter);
		
		System.out.println("Output file is complete ");
		
//		Calendar calStart = Calendar.getInstance();
//		calStart.set(2013, 0, 1, 0, 0, 0);
//		
//		Calendar calEnd = Calendar.getInstance();
//		calEnd.set(2013, 0, 10, 23, 59, 59);
//		
//		String report = finder.goOnServerBetween("ats03db", calStart, calEnd);
//		
//		System.out.println("Output file is [" + report + "]");
		
	}
	
	

	private boolean readUpdates(String filePath) {
		try {
			CSVParser parser = new CSVParser(new FileInputStream(filePath));
			String[][] updatesRows = parser.getAllValues();
			if(updatesRows == null) {
				return false;
			}
			updates = new HashMap<String, String[]>();
			for (String[] columns : updatesRows) {
				
				String foundCountyName = columns[3].toUpperCase().trim().replaceAll("[^A-Z]", "");
				String key = columns[2].toUpperCase().replaceAll("[^A-Z0-9]+", "") + "_" + foundCountyName;
				updates.put(key, columns);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private String goOnServerBetween(String serverName, Calendar calStart, Calendar calEnd) {
		try {
			
			parsedRows = new ArrayList<String[]>();
			
			Connection connection = SyncTool.getConnectionServer(serverName);
			
			
			PreparedStatement preparedStatement = connection.prepareStatement(
					SQL_FIND_SEARCH_BETWEEN.replace(
							"$cnt$", 
							"3207,3211,3212,3214,3217,3219,3222,3223,3231,3232,3233,3234,3240,3241,3246,3248,3249,3250,3253,3254,3255,3256,3257,3258,3259,3261,3262,3263,3266,3270"));
			preparedStatement.setTimestamp(1, new Timestamp(calStart.getTimeInMillis()));
			preparedStatement.setTimestamp(2, new Timestamp(calEnd.getTimeInMillis()));
			
			ResultSet resultSet = preparedStatement.executeQuery();
			
			parsedRows.add(new String[] {"ATS SearchID", "ATS Community", "ATS Community", "ATS FileID", "County", "Product", "Estimated File Reference"});
			
			while(resultSet.next()) {
				String[] defaultRow = new String[] {"", "gamma", "", "", "", "", ""};
				defaultRow[IDX_SEARCH_ID] = resultSet.getString(5);
				defaultRow[IDX_COMM_NAME] = resultSet.getString(1);
				defaultRow[IDX_FILE_ID] = resultSet.getString(2);
				defaultRow[IDX_COUNTY] = resultSet.getString(3);
				
				defaultRow[IDX_SEARCH_TYPE] = Products.getProductListNames()[resultSet.getInt(4)];
				
				defaultRow[6] = "ATS" + resultSet.getString(5);
				
				defaultRow[IDX_SDATE] = resultSet.getString(4);
//				defaultRow[IDX_TSR_DATE] = resultSet.getString(5);
				
				
				parsedRows.add(defaultRow);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String newFilePath = saveInfo();
		return newFilePath;
		
	}

	public String readAndGiveResults() {
		boolean operationOk = readInput();
		if(!operationOk) {
			return null;
		}
		operationOk = updateInfoInternal();
		if(!operationOk) {
			return null;
		}
		String newFilePath = saveInfo();
		return newFilePath;
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
	
	
	
	
	private boolean updateInfoInternal() {
		
		parsedRows = new ArrayList<String[]>();
		
		HashMap<String, Integer> servers = new HashMap<String, Integer>();
		HashMap<String, Integer> comms = new HashMap<String, Integer>();
		
		Set<String> found = new HashSet<String>(); 
		
		try {
		
			Connection connection = null;
			
			Connection connection03 = null;
			Connection connection04 = null;
			Connection connection01 = null;
			Connection connectionLocal = null;
			
			ResultSet resultSet = null;
			
			
			
			
			for (int i = 0; i < originalRows.length; i++) {
				
				String[] originalRow = originalRows[i];
				
				String[] defaultRow = new String[] {originalRow[0], "", "", "", "", "", "", "", "" , "" ,""};
				
				String searchId = originalRow[0];
//				if(searchId.startsWith("ATS")) {
					searchId = searchId.replaceFirst("ATS", "");
					
					if(found.contains(searchId)) {
						continue;
					}
					found.add(searchId);
					
					defaultRow[IDX_SEARCH_ID] = searchId; 
					
					if(searchId.endsWith("3") || searchId.endsWith("4")) {
						
						if(connection03 != null && !connection03.isClosed()) {
							connection = connection03;
						} else {
//							connection03 = SyncTool.getAts02DBConn();
							connection = connection03;
						}
						defaultRow[IDX_SERVER_NAME] = "gamma";
//					} else if(searchId.endsWith("4")){
//						if(connection04 != null && !connection04.isClosed()) {
//							connection = connection04;
//						} else {
//							connection04 = SyncTool.getAts03DBConn();
//							connection = connection04;
//						}
//						
//						defaultRow[IDX_SERVER_NAME] = "ats04";
					} else if(searchId.endsWith("1")){
						if(connection01 != null && !connection01.isClosed()) {
							connection = connection01;
						} else {
//							connection01 = SyncTool.getAts01DBConn();
							connection = connection01;
						}
						defaultRow[IDX_SERVER_NAME] = "beta";
					} else {
						if(connectionLocal != null && !connectionLocal.isClosed()) {
							connection = connectionLocal;
						} else {
							connectionLocal = SyncTool.getLocalDBConn();
							connection = connectionLocal;
						}
						
						defaultRow[IDX_SERVER_NAME] = "test";
					}
					
					PreparedStatement preparedStatement = connection.prepareStatement(SQL_FIND_SEARCH_OUT_OF_RANGE);
					
					try {
					
						preparedStatement.setInt(1, Integer.parseInt(searchId));
						
						
						resultSet = preparedStatement.executeQuery();
						if(resultSet.next()) {
							defaultRow[IDX_COMM_NAME] = resultSet.getString(1);
							defaultRow[IDX_FILE_ID] = resultSet.getString(2);
							defaultRow[IDX_COUNTY] = resultSet.getString(3);
							
							defaultRow[IDX_SEARCH_TYPE] = Products.getProductListNames()[resultSet.getInt(4)];
							
							defaultRow[IDX_SDATE] = resultSet.getString(5);
							defaultRow[IDX_TSR_DATE] = resultSet.getString(6);
							defaultRow[IDX_COUNT_DATE] = resultSet.getString(7);
							defaultRow[IDX_OUT_OF_RANGE] = "1";
							
							Integer cntServer = servers.get(defaultRow[IDX_SERVER_NAME]);
							if(cntServer == null) {
								cntServer = 1;
							} else {
								cntServer ++;
							}
							servers.put(defaultRow[IDX_SERVER_NAME], cntServer);
							
							Integer cntComm = comms.get(defaultRow[IDX_COMM_NAME]);
							if(cntComm == null) {
								cntComm = 1;
							} else {
								cntComm ++;
							}
							comms.put(defaultRow[IDX_COMM_NAME], cntServer);
							
							
						} else {
							defaultRow[IDX_OUT_OF_RANGE] = "0";
						}
					
					} catch (NumberFormatException e) {
						System.err.println("Problem parsing " + searchId);
					}
					
//				} 
				
//				System.out.println("Finished row " + i + " for searchId " + defaultRow[0] + ", sdate " + defaultRow[IDX_SDATE]  + ", tsrdate " + defaultRow[IDX_TSR_DATE]);
				System.out.println("Finished row " + i + " for searchId " + defaultRow[0]);
				parsedRows.add(defaultRow);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		for (String server : servers.keySet()) {
			System.err.println("Server " + server + " has " + servers.get(server));
		}
		
		for (String comm : comms.keySet()) {
			System.err.println("Comm " + comm + " has " + comms.get(comm));
		}
		
		return true;
	}
	
	
	private boolean updateInfoInternalDTOrigReport(CSVPrinter csvPrinter) {

//		parsedRows = new ArrayList<String[]>();

		try {

			Connection connection = SyncTool.getPreDBConn();

			ResultSet resultSet = null;

			System.out.println("Total rows " + originalRows.length);
			
			for (int i = 0; i < originalRows.length; i++) {

				String[] originalRow = originalRows[i];

				String fileId = originalRow[1].replaceFirst("#", "").trim();
				String originalCountyName = originalRow[0].toUpperCase().replace("SAINT ", "ST ").trim().replaceAll("[^A-Z]", "");
				String originalHitDate = originalRow[2].trim();

				String[] defaultRow = new String[] {
						originalRow[0], // 0
						originalRow[1], // 1
						originalRow[2], // 2
						fileId, // 3
						"", // 4
						"", // 5
						"", // 6
						"", // 7
						"", // 8
						"", // 9
						"", // 10
						"",
						"" };

				

				PreparedStatement preparedStatement = connection.prepareStatement(SQL_FIND_ORDER_COUNT);

				preparedStatement.setString(1, fileId);
				preparedStatement.setInt(2, 13);

				resultSet = preparedStatement.executeQuery();

				String foundCountyDifferent = null;
				
				boolean foundSomething = false;
				
				while (resultSet.next()) {

					foundSomething = true;
					
					String foundCountyName = resultSet.getString("name").toUpperCase().trim().replaceAll("[^A-Z]", "");

					if (originalCountyName.equals(foundCountyName)) {
						String foundCountedDate = resultSet.getString("cdf");

						defaultRow[4] = resultSet.getString("file_id");
						defaultRow[5] = "1";
						defaultRow[6] = foundCountedDate;

						if (originalHitDate.equals(foundCountedDate)) {
							// exact hit on county and date
							defaultRow[7] = "1"; // exact hit
						} else {
							defaultRow[7] = "0"; // different date
						}
						
						String commName = resultSet.getString("comm_name");
						defaultRow[8] = commName;
						
						String productType = resultSet.getInt("product_id")==10?"Update":"NotUpdate";
						defaultRow[9] = productType;
												
						break;
					} else {
						if(foundCountyDifferent == null) {
							foundCountyDifferent = foundCountyName;
						} else {
							foundCountyDifferent += ", " + foundCountyName;
						}
					}
				}

				if(foundCountyDifferent != null) {
					defaultRow[10] = foundCountyDifferent;
				}
				
				System.out.println("Finished row " + i + " for fileid " + fileId + " foundSomething = " + foundSomething);
				
				csvPrinter.println(defaultRow);
				
//				parsedRows.add(defaultRow);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	
	private void updateByUpdates(CSVPrinter csvPrinter) {
		System.out.println("Total rows " + originalRows.length);
		int foundUpdates = 0;
		for (int i = 0; i < originalRows.length; i++) {

			String[] originalRow = originalRows[i];

			String fileId = originalRow[3].trim();
			String originalCountyName = originalRow[0].toUpperCase().replace("SAINT ", "ST ").trim().replaceAll("[^A-Z]", "");
			String originalHitDate = originalRow[2].trim();

			String[] defaultRow = new String[] {
					originalRow[0], // 0
					originalRow[1], // 1
					originalRow[2], // 2
					originalRow[3], // 3
					originalRow[4], // 4
					originalRow[5], // 5
					originalRow[6], // 6
					originalRow[7], // 7
					originalRow[8], // 8
					originalRow[9], // 9
					originalRow[10], // 10
					""	// 11
					};
			if(StringUtils.isBlank(originalRow[4])) {
				String[] updateInfoColumns = updates.get(fileId + "_" + originalCountyName);
				if(updateInfoColumns != null) {
					//found an update, finally
					defaultRow[4] = updateInfoColumns[2];
					defaultRow[5] = "1";
					defaultRow[8] = updateInfoColumns[1];
					defaultRow[9] = "Update";
					defaultRow[10] = "";
					defaultRow[11] = updateInfoColumns[0];
					foundUpdates++;
				}
			}
			
			csvPrinter.println(defaultRow);
		}
		
		System.out.println("Updated " + foundUpdates + " rows ");
	}
	
	private String saveInfo() {
		String outputFileName = filePath;
		String outputFileNameNoExtension = outputFileName.substring(0,outputFileName.lastIndexOf("."));
		String outputFileNameJustExtension = outputFileName.substring(outputFileName.lastIndexOf("."));
		String baseName = "_completed";
		int version = 0;
		File outputFile = new File(outputFileNameNoExtension + baseName + outputFileNameJustExtension);
		while(outputFile.exists()) {
			version++;
			outputFile = new File(outputFileNameNoExtension + baseName + "(" + version + ")" + outputFileNameJustExtension);
		}
		try {
			CSVPrinter printer = new CSVPrinter(new FileWriter(outputFile));
			printer.println(parsedRows.toArray(new String[parsedRows.size()][]));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return outputFile.getPath();
	}
	
	private CSVPrinter getResultWriter() {
		String outputFileName = filePath;
		String outputFileNameNoExtension = outputFileName.substring(0,outputFileName.lastIndexOf("."));
		String outputFileNameJustExtension = outputFileName.substring(outputFileName.lastIndexOf("."));
		String baseName = "_completed";
		int version = 0;
		File outputFile = new File(outputFileNameNoExtension + baseName + outputFileNameJustExtension);
		while(outputFile.exists()) {
			version++;
			outputFile = new File(outputFileNameNoExtension + baseName + "(" + version + ")" + outputFileNameJustExtension);
		}
		try {
			CSVPrinter printer = new CSVPrinter(new FileWriter(outputFile));
			System.out.println("Generated file name " + outputFile);
			return printer;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
}

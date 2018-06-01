package ro.cst.tsearch.utils.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import ro.cst.tsearch.utils.DBConstants;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;

public class StewartBillingReader {
	
	private String filePath;
	private String[][] rows;
	private	int columnsNumber;
	private int columnFileNo;
	private int columnProductionCommunity;
	private int columnOtherCommunity;
	private int productionCommId;
	private int month;
	private int year;
	public StewartBillingReader(String filePath, int columnsNumber, int columnFileNo, int columnProductionCommunity,
			int columnOtherCommunity, int productionCommId, int month, int year) {
		super();
		this.filePath = filePath;
		this.columnsNumber = columnsNumber;
		this.columnFileNo = columnFileNo;
		this.columnProductionCommunity = columnProductionCommunity;
		this.columnOtherCommunity = columnOtherCommunity;
		this.productionCommId = productionCommId;
		this.month = month;
		this.year = year;
	}
	/**
	 * @return the filePath
	 */
	public String getFilePath() {
		return filePath;
	}
	/**
	 * @return the rows
	 */
	public String[][] getRows() {
		return rows;
	}
	/**
	 * @return the columnsNumber
	 */
	public int getColumnsNumber() {
		return columnsNumber;
	}
	/**
	 * @return the columnFileNo
	 */
	public int getColumnFileNo() {
		return columnFileNo;
	}
	/**
	 * @return the columnProductionCommunity
	 */
	public int getColumnProductionCommunity() {
		return columnProductionCommunity;
	}
	/**
	 * @return the columnOtherCommunity
	 */
	public int getColumnOtherCommunity() {
		return columnOtherCommunity;
	}
	/**
	 * @return the productionCommId
	 */
	public int getProductionCommId() {
		return productionCommId;
	}
	public static void main(String[] args) {
		StewartBillingReader stewartBillingReader = new StewartBillingReader(
				"C:\\Documents and Settings\\AndreiA\\My Documents\\Stewart CDWS billing Jun 2009.csv",
				8,1,6,7,71,6,2009);
		String newFilePath = stewartBillingReader.readAndGiveResults();
		System.out.println("Output file is [" + newFilePath + "]");
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
			rows = parser.getAllValues();
			if(rows == null) {
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private static final String SQL_CHECK_SEARCH = 
		"SELECT s." + DBConstants.FIELD_SEARCH_ID + 
    	", s." + DBConstants.FIELD_SEARCH_TYPE + 
    	", s." + DBConstants.FIELD_SEARCH_SDATE +
    	", s." + DBConstants.FIELD_SEARCH_COMM_ID +
    	" FROM " + DBConstants.TABLE_SEARCH + 
    		" s JOIN " + DBConstants.TABLE_PROPERTY + 
    			" p ON s." + DBConstants.FIELD_SEARCH_PROPERTY_ID + " = p." + DBConstants.FIELD_PROPERTY_ID + 
    		" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + 
    			" f ON f." + DBConstants.FIELD_SEARCH_FLAGS_ID + " = s." + DBConstants.FIELD_SEARCH_ID + 
    	" WHERE " +
    	" TRIM(LEADING '0' FROM REPLACE(" +
    			" UPPER(REVERSE(SUBSTRING(REVERSE(SUBSTRING(abstr_fileno, LOCATE('-',abstr_fileno) + 1)),LOCATE('_',REVERSE(SUBSTRING(abstr_fileno, LOCATE('-',abstr_fileno) + 1)), 13) + 1)))," +
    			" '-'," +
    			" '')) " +

    	" LIKE ? " + 
    	" AND ((month(sdate)= ? and year(sdate) = ?) OR (tsr_date is not null and month(tsr_date) = ? and year(tsr_date) = ?))";
	
	private boolean updateInfoInternal() {
		Connection connection = null;
		ResultSet resultSet = null;
		try {
			connection = SyncTool.getPreDBConn();
			PreparedStatement preparedStatement = connection.prepareStatement(SQL_CHECK_SEARCH);
			preparedStatement.setInt(2, month);
			preparedStatement.setInt(3, year);
			preparedStatement.setInt(4, month);
			preparedStatement.setInt(5, year);
			for (String[] row : getRows()) {
				if(!row[getColumnFileNo()].trim().isEmpty()) {
					preparedStatement.setString(1, row[getColumnFileNo()].replaceFirst("^[0]+", "").replaceAll("-", ""));
					resultSet = preparedStatement.executeQuery();
					while(resultSet.next()) {
						if(resultSet.getInt(DBConstants.FIELD_SEARCH_COMM_ID) == getProductionCommId()) {
							row[getColumnProductionCommunity()] += "x"; 
						} else {
							row[getColumnOtherCommunity()] += "x";
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		
		return true;
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
			printer.println(rows);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return outputFile.getPath();
	}
	

}

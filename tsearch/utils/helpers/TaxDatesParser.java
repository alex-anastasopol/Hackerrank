package ro.cst.tsearch.utils.helpers;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.database.rowmapper.TaxDatesMapper;
import ro.cst.tsearch.utils.DBConstants;

import com.Ostermiller.util.ExcelCSVParser;

public class TaxDatesParser {

	
	private String serverName;
	private Map<String, TaxDatesMapper> csvData = 
		new HashMap<String, TaxDatesMapper>();
	private Map<String, TaxDatesMapper> dbData = 
		new HashMap<String, TaxDatesMapper>();
	
	public TaxDatesParser(String serverName) {
		this.serverName = serverName;
		try {
			Connection c = DatabaseSync.getConnectionServer(serverName);
			Statement stmt = c.createStatement();
			ResultSet resultSet = stmt.executeQuery("select * from " + TaxDatesMapper.TABLE_TAX_DATES);
			while(resultSet.next()) {
				TaxDatesMapper taxDatesMapper = new TaxDatesMapper();
				taxDatesMapper = taxDatesMapper.mapRow(resultSet, 0);
				dbData.put(taxDatesMapper.getName(), taxDatesMapper);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public static void main(String[] args) {
		TaxDatesParser taxDatesParser = new TaxDatesParser("ats04db");
		taxDatesParser.checkAndUpdateFile(new File("D:\\work\\tax_dates.csv"), false);
	}
	
	public boolean checkAndUpdateFile(File f, boolean doUpdate) {
		if(f == null || !f.isFile() || !f.exists()) {
			return false;
		}
		int differentDueDates = 0;
		int differentPayDates = 0;
		
		StringBuilder dueDateTextInfo = new StringBuilder();
		StringBuilder dueDateSQL = new StringBuilder();
		StringBuilder payDateSQL = new StringBuilder();
		StringBuilder taxYearSQL = new StringBuilder();
		try {
			String[][] rawData = ExcelCSVParser.parse(new FileReader(f));
			
			for (String[] row : rawData) {
				if(row.length == 4) {
					Map<String, Object> params = new HashMap<String, Object>();
					params.put(DBConstants.FIELD_NAME_SYNONYMS_SYNONYM_KEY, row[0].trim());
					params.put(DBConstants.FIELD_NAME_SYNONYMS_SYNONYM_VALUE, row[1].trim());
					
					TaxDatesMapper taxDatesMapper = dbData.get(row[0]);
					if(taxDatesMapper == null) {
						System.err.println("Key: " + row[0] + " does not exist");
					} else {
						if(!row[1].equals(taxDatesMapper.getDueDateString())) {
							
							dueDateTextInfo.append("Key: " + row[0] + " has different dueDate: Database = [" + taxDatesMapper.getDueDateString() + "] and file = [" + row[1] + "]\n");
							dueDateSQL.append("UPDATE " + TaxDatesMapper.TABLE_TAX_DATES + " set " + TaxDatesMapper.FIELD_DUE_DATE + " = '" + row[1] + 
									"' WHERE " + TaxDatesMapper.FIELD_NAME + " = '" + row[0] + "';\n");
							differentDueDates++;
						}
						if(!row[2].equals(taxDatesMapper.getPayDateString())) {
							dueDateTextInfo.append("Key: " + row[0] + " has different payDate: Database = [" + taxDatesMapper.getPayDateString() + "] and file = [" + row[2] + "]\n");
							payDateSQL.append("UPDATE " + TaxDatesMapper.TABLE_TAX_DATES + " set " + TaxDatesMapper.FIELD_PAY_DATE + " = '" + row[2] + 
									"' WHERE " + TaxDatesMapper.FIELD_NAME + " = '" + row[0] + "';\n");
							differentPayDates++;
						}
						if(Integer.parseInt(row[3]) != taxDatesMapper.getTaxYearMode()) {
							taxYearSQL.append("UPDATE " + TaxDatesMapper.TABLE_TAX_DATES + " set " + TaxDatesMapper.FIELD_TAX_YEAR_MODE + " = " + row[3] + 
									" WHERE " + TaxDatesMapper.FIELD_NAME + " = '" + row[0] + "';\n");
						}
					}
					
					
				}
			}
			
		} catch (Exception e) {
			System.err.println("Parsing error for csv file " + f.getAbsolutePath());
			e.printStackTrace();
			return false;
		} 
		
		System.err.println(dueDateTextInfo.toString());
		System.out.println();
		System.out.println(dueDateSQL.toString());
		System.out.println();
		System.out.println(payDateSQL.toString());
		System.out.println();
		System.out.println(taxYearSQL.toString());
		
		if(differentDueDates > 0) {
			System.err.println("differentDueDates = " + differentDueDates);
		}
		if(differentPayDates > 0) {
			System.err.println("differentPayDates = " + differentPayDates);
		}
		
		
		return true;
	}
	
}

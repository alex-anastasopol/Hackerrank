package ro.cst.tsearch.utils.helpers;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.DistrictsNVClarkMapper;

import com.Ostermiller.util.ExcelCSVParser;

public class DistrictsNVClarkParser {

	private Map<String, DistrictsNVClarkMapper> dbData = 
		new HashMap<String, DistrictsNVClarkMapper>();
	
	public DistrictsNVClarkParser(String serverName) {
		try {
			Connection c = DatabaseSync.getConnectionServer(serverName);
			Statement stmt = c.createStatement();
			ResultSet resultSet = stmt.executeQuery("select * from " + DistrictsNVClarkMapper.TABLE_DISTRICTS_NV_CLARK);
			while(resultSet.next()) {
				DistrictsNVClarkMapper districtsNVClarkMapper = new DistrictsNVClarkMapper();
				districtsNVClarkMapper = districtsNVClarkMapper.mapRow(resultSet, 0);
				dbData.put(districtsNVClarkMapper.getCode(), districtsNVClarkMapper);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public static void main(String[] args) {
		DistrictsNVClarkParser districtsNVClarkParser = new DistrictsNVClarkParser("local");
		districtsNVClarkParser.checkAndUpdateFile(new File("D:\\DistrictsNVClark.csv"), false);
	}
	
	public boolean checkAndUpdateFile(File f, boolean doUpdate) {
		if(f == null || !f.isFile() || !f.exists()){
			return false;
		}
		int c = 0;
		try {
			String[][] rawData = ExcelCSVParser.parse(new FileReader(f));
			SimpleJdbcInsert insert = DBManager.getSimpleJdbcInsert().withTableName(DistrictsNVClarkMapper.TABLE_DISTRICTS_NV_CLARK);
			for (int i = 0; i < rawData.length; i++) {
				String[] row = rawData[i];
				if(row.length == 3) {
					if (!"".equals(row[2].trim())){
						Map<String, Object> params = new HashMap<String, Object>();
						params.put(DistrictsNVClarkMapper.FIELD_DISTRICT, row[0].trim());
						params.put(DistrictsNVClarkMapper.FIELD_NAME, row[1].trim());
						params.put(DistrictsNVClarkMapper.FIELD_CODE, row[2].trim());
						
						insert.execute(params);			
						c++;
					}
				}
			}
			
		} catch (Exception e) {
			System.err.println("Parsing error for csv file " + f.getAbsolutePath());
			e.printStackTrace();
			return false;
		} 
		System.err.println("numar de inregistrari = " + c);
				
		return true;
	}
	
}

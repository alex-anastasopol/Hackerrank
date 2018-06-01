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
import ro.cst.tsearch.database.rowmapper.HoaInfoMapper;

import com.Ostermiller.util.ExcelCSVParser;

public class HoaInfoParser {

	
	private String serverName;
	private Map<String, HoaInfoMapper> csvData = 
		new HashMap<String, HoaInfoMapper>();
	private Map<String, HoaInfoMapper> dbData = 
		new HashMap<String, HoaInfoMapper>();
	
	public HoaInfoParser(String serverName) {
		this.serverName = serverName;
		try {
			Connection c = DatabaseSync.getConnectionServer(serverName);
			Statement stmt = c.createStatement();
			ResultSet resultSet = stmt.executeQuery("select * from " + HoaInfoMapper.TABLE_HOA_INFO);
			while(resultSet.next()) {
				HoaInfoMapper hoaInfoMapper = new HoaInfoMapper();
				hoaInfoMapper = hoaInfoMapper.mapRow(resultSet, 0);
				dbData.put(hoaInfoMapper.getSubdivisionName(), hoaInfoMapper);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public static void main(String[] args) {
		HoaInfoParser hoaInfoParser = new HoaInfoParser("local");
		hoaInfoParser.checkAndUpdateFile(new File("D:\\hoa csv\\xx"), false);
	}
	
	public boolean checkAndUpdateFile(File f, boolean doUpdate) {
		if(f == null || !f.isFile() || !f.exists()){
			return false;
		}
		int c = 0;
		try {
			String[][] rawData = ExcelCSVParser.parse(new FileReader(f));
			SimpleJdbcInsert insert = DBManager.getSimpleJdbcInsert().withTableName(HoaInfoMapper.TABLE_HOA_INFO);
			for (int i = 0; i < rawData.length; i++) {
				String[] row = rawData[i];
				//if(row.length == 10) {
					Map<String, Object> params = new HashMap<String, Object>();
					params.put(HoaInfoMapper.FIELD_SUBDIVISION_NAME, row[0].trim());
					params.put(HoaInfoMapper.FIELD_PLAT_BOOK, row[1].trim());
					params.put(HoaInfoMapper.FIELD_PLAT_PAGE, row[2].trim());
					try {
						params.put(HoaInfoMapper.FIELD_CCR_DEC_BOOK, row[3].trim());
					} catch (Exception e) {
						params.put(HoaInfoMapper.FIELD_CCR_DEC_BOOK, "");
					}
					try {
						params.put(HoaInfoMapper.FIELD_CCR_DEC_PAGE, row[4].trim());
					} catch (Exception e) {
						params.put(HoaInfoMapper.FIELD_CCR_DEC_PAGE, "");
					}
					try {
						params.put(HoaInfoMapper.FIELD_HOA_NAME, row[5].trim());
					} catch (Exception e) {
						params.put(HoaInfoMapper.FIELD_HOA_NAME, "");
					}
					try {
						params.put(HoaInfoMapper.FIELD_MASTER_HOA, row[6].trim());
					} catch (Exception e) {
						params.put(HoaInfoMapper.FIELD_MASTER_HOA, "");
					}
					try {
						params.put(HoaInfoMapper.FIELD_ADD_HOA, row[7].trim());
					} catch (Exception e) {
						params.put(HoaInfoMapper.FIELD_ADD_HOA, "");
					}
					try {
						params.put(HoaInfoMapper.FIELD_LIEN_JDG_NOC, row[8].trim());
					} catch (Exception e) {
						params.put(HoaInfoMapper.FIELD_LIEN_JDG_NOC, "");
					}
					try {
						params.put(HoaInfoMapper.FIELD_NOTES, row[9].trim());
					} catch (Exception e) {
						params.put(HoaInfoMapper.FIELD_NOTES, "");
					}
					params.put(HoaInfoMapper.FIELD_COUNTY, "xxxWALTON");
					params.put(HoaInfoMapper.FIELD_COUNTYFIPS, "xxx131");
						
					insert.execute(params);	
					c++;
				//}
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

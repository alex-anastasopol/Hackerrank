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
import ro.cst.tsearch.database.rowmapper.HoaAndCondoMapper;

import com.Ostermiller.util.ExcelCSVParser;

@Deprecated
public class HoaCondoParser {

	
	private String serverName;
	private Map<String, HoaAndCondoMapper> csvData = 
		new HashMap<String, HoaAndCondoMapper>();
	private Map<String, HoaAndCondoMapper> dbData = 
		new HashMap<String, HoaAndCondoMapper>();
	
	public HoaCondoParser(String serverName) {
		this.serverName = serverName;
		try {
			Connection c = DatabaseSync.getConnectionServer(serverName);
			Statement stmt = c.createStatement();
			ResultSet resultSet = stmt.executeQuery("select * from " + HoaAndCondoMapper.TABLE_HOA_CONDO);
			while(resultSet.next()) {
				HoaAndCondoMapper hoaAndCondoMapper = new HoaAndCondoMapper();
				hoaAndCondoMapper = hoaAndCondoMapper.mapRow(resultSet, 0);
				dbData.put(hoaAndCondoMapper.getAssoc_name(), hoaAndCondoMapper);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public static void main(String[] args) {
		HoaCondoParser hoaCondoParser = new HoaCondoParser("local");
		hoaCondoParser.checkAndUpdateFile(new File("D:\\hoa_condo\\2011-Qtr1-HOA-2.csv"), false);
	}
	
	public boolean checkAndUpdateFile(File f, boolean doUpdate) {
		if(f == null || !f.isFile() || !f.exists()){
			return false;
		}
		int c = 1;
		try {
			String[][] rawData = ExcelCSVParser.parse(new FileReader(f));
			SimpleJdbcInsert insert = DBManager.getSimpleJdbcInsert().withTableName(HoaAndCondoMapper.TABLE_HOA_CONDO);
			for (int i = 1; i < rawData.length; i++) {
				String[] row = rawData[i];
				if(row.length == 31) {//for condo
					/*if (!"".equals(row[30].trim())){
						Map<String, Object> params = new HashMap<String, Object>();
						params.put(HoaAndCondoMapper.FIELD_ORIGINAL_ID, row[0].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_NAME, row[1].trim());
						params.put(HoaAndCondoMapper.FIELD_NO_OF_UNITS, row[2].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_ADDR_LINE1, row[3].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_ADDR_LINE2, row[4].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_CITY, row[5].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_STATE, row[6].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_ZIP, row[7].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_CREATION_DATE, row[8].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_NAME, row[9].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STREET_NUMBER, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STREET_PREFIX_DIR, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STREET_NAME, row[10].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STREET_SUFFIX, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STREET_SUFFIX_DIR,"");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_APT_TYPE, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_APT_NUMBER, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_CITY, row[11].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STATE, row[12].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_ZIP, row[13].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_ZIP4, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_PHONE, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_TITLE, row[14].trim());
						params.put(HoaAndCondoMapper.FIELD_PRESIDENT, row[15].trim());
						params.put(HoaAndCondoMapper.FIELD_VICE_PRESIDENT, row[16].trim());
						params.put(HoaAndCondoMapper.FIELD_SECRETARY, row[17].trim());
						params.put(HoaAndCondoMapper.FIELD_TREASURER, row[18].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_NAME, row[19].trim());
						params.put(HoaAndCondoMapper.FIELD_PARSED_FIRST_NAME, row[20].trim());
						params.put(HoaAndCondoMapper.FIELD_PARSED_LAST_NAME, row[21].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_PREFIX_NUMBER, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_PREFIX_DIR, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_STREET, row[22].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_STREET_SUFFIX, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_STREET_SUFFIX_DIR, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_APT_TYPE, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_APT_NUMBER, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_CITY, row[23].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_STATE, row[24].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ZIP, row[25].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ZIP_PLUS4, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_PHONE, row[26].trim());
						params.put(HoaAndCondoMapper.FIELD_ADDR_SCORE, row[27].trim());
						params.put(HoaAndCondoMapper.FIELD_ESTIMATED_INCOME, row[28].trim());
						params.put(HoaAndCondoMapper.FIELD_ESTIMATED_HOME_VALUE, row[29].trim());
						params.put(HoaAndCondoMapper.FIELD_COUNTY, row[30].trim());
						
						insert.execute(params);			
						c++;
					}*/
				}else if(row.length == 30) {//for hoa
					/*if (!"".equals(row[29].trim())){
						Map<String, Object> params = new HashMap<String, Object>();
						params.put(HoaAndCondoMapper.FIELD_ORIGINAL_ID, row[0].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_NAME, row[1].trim());
						params.put(HoaAndCondoMapper.FIELD_NO_OF_UNITS, "");
						params.put(HoaAndCondoMapper.FIELD_ASSOC_ADDR_LINE1, row[2].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_ADDR_LINE2, row[3].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_CITY, row[4].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_STATE, row[5].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_ZIP, row[6].trim());
						params.put(HoaAndCondoMapper.FIELD_ASSOC_CREATION_DATE, row[7].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_NAME, row[8].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STREET_NUMBER, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STREET_PREFIX_DIR, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STREET_NAME, row[9].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STREET_SUFFIX, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STREET_SUFFIX_DIR,"");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_APT_TYPE, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_APT_NUMBER, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_CITY, row[10].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_STATE, row[11].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_ZIP, row[12].trim());
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_ZIP4, "");
						params.put(HoaAndCondoMapper.FIELD_REGISTERED_AGENT_PHONE, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_TITLE, row[13].trim());
						params.put(HoaAndCondoMapper.FIELD_PRESIDENT, row[14].trim());
						params.put(HoaAndCondoMapper.FIELD_VICE_PRESIDENT, row[15].trim());
						params.put(HoaAndCondoMapper.FIELD_SECRETARY, row[16].trim());
						params.put(HoaAndCondoMapper.FIELD_TREASURER, row[17].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_NAME, row[18].trim());
						params.put(HoaAndCondoMapper.FIELD_PARSED_FIRST_NAME, row[19].trim());
						params.put(HoaAndCondoMapper.FIELD_PARSED_LAST_NAME, row[20].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_PREFIX_NUMBER, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_PREFIX_DIR, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_STREET, row[21].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_STREET_SUFFIX, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_STREET_SUFFIX_DIR, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_APT_TYPE, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ADDRESS_APT_NUMBER, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_CITY, row[22].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_STATE, row[23].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ZIP, row[24].trim());
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_ZIP_PLUS4, "");
						params.put(HoaAndCondoMapper.FIELD_BOARD_MEMBER_PHONE, row[25].trim());
						params.put(HoaAndCondoMapper.FIELD_ADDR_SCORE, row[26].trim());
						params.put(HoaAndCondoMapper.FIELD_ESTIMATED_INCOME, row[27].trim());
						params.put(HoaAndCondoMapper.FIELD_ESTIMATED_HOME_VALUE, row[28].trim());
						params.put(HoaAndCondoMapper.FIELD_COUNTY, row[29].trim());
						
						insert.execute(params);	
						c++;
					}*/
				}
			}
			
		} catch (Exception e) {
			System.err.println("Parsing error for csv file " + f.getAbsolutePath());
			e.printStackTrace();
			return false;
		} 
		System.err.println("numar de integistrari = " + c);
				
		return true;
	}
	
}

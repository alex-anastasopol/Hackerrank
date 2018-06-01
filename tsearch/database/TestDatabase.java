package ro.cst.tsearch.database;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class TestDatabase {
	
	private static final String SQL_UPDATE_USER_WCARD = " UPDATE TS_USER set wcard_id = ? where login = ?";
	
	public static class UserWcard {
		public String login;
		public String wcard;
	}
	
	public static Vector<UserWcard> readFile(String fileName){
		Vector<UserWcard> result = new Vector<UserWcard>();
		File csv = new File(fileName);
		try {
			RandomAccessFile raf = new RandomAccessFile(csv, "r");
			String line = null;
			String[] elems = null;
			while ((line = raf.readLine()) != null){
				elems = line.split(",");
				UserWcard wc = new UserWcard();
				wc.login = elems[1];
				wc.wcard = elems[0];
				result.add(wc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static void main(String[] args) {
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		List<Map<String, Object>> list = sjt.queryForList("SELECT * FROM ts_server" );
		for (Map<String, Object> map : list) {
			Set<String> keys = map.keySet();
			for (String key : keys) {
				System.out.println( key + " = " + map.get(key));
			}			
		}
		Vector<UserWcard> users = TestDatabase.readFile("D:\\bill.csv");
		System.out.println("Users length " + users.size());
		for (UserWcard userWcard : users) {
			System.out.println("Login: " + userWcard.login.toString() + " wcardId = " + userWcard.wcard.toString());
			int updated = sjt.update(SQL_UPDATE_USER_WCARD, userWcard.wcard.toString(), userWcard.login.toString());
			System.out.println("Updated " + updated + " rows");
			if(updated!=1) {
				System.err.println("ERRRRRRRRRRRRRRooooooooooooooooooorrrrrrrrrrrrrrrrr");
			}
		}
		
	}
}

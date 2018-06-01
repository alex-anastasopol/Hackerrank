package ro.cst.tsearch.utils.helpers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

public class SQLRunner {
	public static void main(String[] args) {
		Connection connection = null;
//		ResultSet resultSet = null;
		
		try {
			connection = SyncTool.getPreDBConn();
			
			String sql = "delete FROM ts_usage_disk WHERE type in (1,2) and timestamp < now() - INTERVAL ? DAY";
			
			System.out.println("Running sql " + sql);
			
			for (int i = 340; i >= 31; i--) {
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setInt(1, i);
				
				try {
					long startTime = System.currentTimeMillis();
					int affected = preparedStatement.executeUpdate();
					long endTime = System.currentTimeMillis();
					long time = ((endTime-startTime) / 1000);
					System.out.println("i = " + i + " affected " + affected + " rows and took " + time + " sec");
					TimeUnit.SECONDS.sleep(time/4);
				} catch (Exception e) {
					System.err.println("Problem running sql for " + i);
					e.printStackTrace();
				}
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}

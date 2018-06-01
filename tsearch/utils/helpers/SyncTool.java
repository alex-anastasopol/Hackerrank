package ro.cst.tsearch.utils.helpers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class SyncTool {

	public static Connection getConnection(String user, String password, String host, String port, String database) throws SQLException,
			ClassNotFoundException, IllegalAccessException, InstantiationException {
		String connectionString = "jdbc:mysql://" + host + ":" + port + "/" + database;
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		return DriverManager.getConnection(connectionString, user, password);
	}
	    
	public static Connection getDrDBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		return getConnection("", "", "", "3306", "tsearch_for_change");
	}

	public static Connection getStgDBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		return getConnection("", "", "", "3306", "tsearch_for_change");
	}

	public static Connection getPreDBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		return getConnection("", "", "", "3306", "tsearch_for_change");
	}

	public static Connection getAtsPrd03DBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		return getConnection("", "", "", "3306", "tsearch_for_change");
	}

	public static Connection getAtsPrd02DBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		return getConnection("", "", "", "3306", "tsearch_for_change");
	}

	public static Connection getAtsPrd01DBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		return getConnection("", "", "", "3306", "tsearch_for_change");
	}
	   
	public static Connection getLocalDBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		return getConnection("mysql", "mysql", "localhost", "3306", "tsearch_for_change");
	}
	
	    public static Connection getConnectionServer(String serverName) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
	    	Connection c= null;
	    	if(serverName==null || serverName.equals("")){
	    		throw new SQLException("Conexiune nespecificata");
	    	}else if(serverName.equals("atsprd01")){
	    		c = getAtsPrd01DBConn();
	    	}else if(serverName.equals("atsprd02")){
	    		c = getAtsPrd02DBConn();
	    	}else if(serverName.equals("atsprd03")){
	    		c = getAtsPrd03DBConn();
	    	}else if(serverName.equals("atspre")){
	    		c = getPreDBConn();
	    	}else if(serverName.equals("atsstg")){
	    		c = getStgDBConn();
	    	}else if(serverName.equals("atsdr")){
	    		c = getDrDBConn();
	    	}else if(serverName.equals("local")){
	    		c = getLocalDBConn();
	    	}
	    	return c;
	    }

		public static String formatString(String sirDeFormatat){
	    	if(sirDeFormatat==null)
	    		return "";
	    	String sir = sirDeFormatat.replaceAll("\\\\", "\\\\\\\\");
	    	//     Pattern.compile(regex).matcher(str).replaceAll(repl)
	    	sir = sir.replaceAll("'", "\\\\'");
	    	
	    	return sir;
	    }
	    public static String formatDate(Object dateObj) {
	    	try{
	    		if(dateObj==null)
	    			return null;
	    	String pm =  "'"+((java.util.Date)dateObj).toString()+"'";
	    	return pm;
	    	}catch(Exception e){
	    		e.printStackTrace();
	    		return null;
	    	}
	    }
	public static void skipDuplicateEntry(String serverName, boolean fast) throws Exception{
		Connection c = getConnectionServer(serverName);
		String query = "show slave status;";
		Statement stmt = c.createStatement();
		boolean error_exit = true;
		int nrTry =0;
		int nrTry2 = 0;
		try{
		while(error_exit && nrTry2<4){
			
			ResultSet ret1 = stmt.executeQuery(query);
			boolean isInReplication = ret1.next();
			if(isInReplication){
				String ioRunning = ret1.getString("Slave_IO_Running");
				String sqlRunning = ret1.getString("Slave_SQL_Running");
				String statusMaster = ret1.getString("Slave_IO_State");
				int readMasterLogPos = ret1.getInt("Read_Master_Log_Pos");
				int execMasterLogPos = ret1.getInt("Exec_Master_Log_Pos");
				int secondsBeforeMaster = ret1.getInt("Seconds_Behind_Master");
				String masterLogFile = ret1.getString("Master_Log_File");
				String execMasterFile = ret1.getString("Relay_Master_Log_File");
				//Waiting for master to send event
				if(sqlRunning.equalsIgnoreCase("No")){
					if(ret1.getInt("Last_Errno")==1062 || ret1.getInt("Last_Errno") == 1452 || ret1.getInt("Last_Errno") == 1105 || ret1.getInt("Last_Errno") == 1142){
						System.out.println("Last_Error: " + ret1.getString("Last_Error"));
						String skipCommand = "set global sql_slave_skip_counter=1;";
						stmt.executeQuery(skipCommand);
					}
					stmt.executeQuery("slave start;");
					nrTry2=0;
					nrTry++;
				}
				if(ioRunning.equalsIgnoreCase("No")){
					stmt.executeQuery("slave start;");
					nrTry2=0;
				}
				if(readMasterLogPos==execMasterLogPos && statusMaster.equalsIgnoreCase("Waiting for master to send event")){
					nrTry2++;
					error_exit = false;
					System.out.println(serverName + ": System synchronized at master pos:"+readMasterLogPos+ " duplicates:"+nrTry);
				} else if (readMasterLogPos!=execMasterLogPos) {
					System.out.println(
							serverName + " at " + Calendar.getInstance().getTime() +  ": Master pos:"+readMasterLogPos+ 
							" Reading pos:"+execMasterLogPos + 
							" difference: " + (readMasterLogPos - execMasterLogPos) +
//							" \nRelay_Log_File: " + ret1.getString("Relay_Log_File") +
							" Master_Log_File: " + masterLogFile + 
							" Relay_Master_Log_File: " + execMasterFile +
//							" Exec_Master_Log_Pos: " + ret1.getString("Exec_Master_Log_Pos") + 
							" Seconds_Behind_Master: " + secondsBeforeMaster + 
							" and hours " + (secondsBeforeMaster/3600f));
					if(secondsBeforeMaster > 100 || !masterLogFile.equals(execMasterFile)) {
						if(fast) {
							TimeUnit.SECONDS.sleep(10);
						} else {
							TimeUnit.SECONDS.sleep(60);
						}
					} else {
						nrTry2=5;
					}
				}
			} else {
				nrTry2=5;
				error_exit = false;
				System.err.println(serverName + ": Database not in replication");
			}
			System.out.println(serverName + ": duplicates = "+nrTry);
			if(fast) {
				TimeUnit.SECONDS.sleep(1);
			} else {
				TimeUnit.SECONDS.sleep(30);
			}
		}
		}catch (InterruptedException ie){
			System.err.println(serverName + ": Exception Ocured");
			ie.printStackTrace();
		} finally {
			c.close();
		}
	}
	
	public static void compareDatabases(String reference, String candidate) {
		Connection refConn = null;
		Connection candConn = null;
		try {
			refConn = getConnectionServer(reference);
			candConn = getConnectionServer(candidate);
			Statement refstmt0 = refConn.createStatement();
			Statement refstmt = refConn.createStatement();
			Statement candstmt = candConn.createStatement();
			ResultSet ret1 = refstmt0.executeQuery("show tables;");
			while(ret1.next()) {
				String table = ret1.getString(1);
				String sqlForTable = "select count(*) from " + table;
				ResultSet refRes = refstmt.executeQuery(sqlForTable);
				ResultSet candRes = candstmt.executeQuery(sqlForTable);
				int refSize = -1;
				int candSize = -1;
				if(refRes.next()) {
					refSize = refRes.getInt(1);
				}
				if(candRes.next()) {
					candSize = candRes.getInt(1);
				}
				if(refSize == candSize && refSize != -1) {
					System.out.println("Table " + table + " OK : rowcount" + refSize);
				} else {
					System.err.println("Table " + table + " FAIL CHECK : refSize = " + refSize + " and candSize = " +candSize );
				}
			}
			
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		//compareDatabases("ats03db", "ats04db");
		//if(true)
		//	return;
		//compareDatabases("ats04db",  "ats02db" );
			int i = 0;
			while(true) {
				i++;
				System.out.println("Start run no: " + i);
				/*try {
					skipDuplicateEntry("ats01db");
				} catch (Exception e1) {
					e1.printStackTrace();
				}*/
				try {
//					skipDuplicateEntry("ats02", false);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
				try {
//					skipDuplicateEntry("ats03db", true);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				try {
//					skipDuplicateEntry("ats04db", false);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				try {
					skipDuplicateEntry("atsdb", true);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				System.out.println("\tEnd run no: " + i + ". Sleeping ....");
				
				try {
					Thread.sleep(1000 * 60 * 5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				
				 
			}
		

	}

}

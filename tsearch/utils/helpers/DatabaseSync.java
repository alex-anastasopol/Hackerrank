package ro.cst.tsearch.utils.helpers;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSync {
    public static Connection getConnection(String user, String password, String host, String port, String database) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	String connectionString = "jdbc:mysql://"+host+":"+port+"/"+database;
    	Class.forName("com.mysql.jdbc.Driver").newInstance();
        return DriverManager.getConnection(connectionString, user, password);
    }
    public static Connection getAts01DBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	return null;
    }
    public static Connection getAts02DBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	return null;
    }
    public static Connection getAts03DBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	return null;
    }
    public static Connection getAts04DBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	return null;
    }
    public static Connection getBetaDBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	return null;
    }
    public static Connection getBetaConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	return null;
    }
    public static Connection getLocalDBConn() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	return null;
    }
    
    public static Connection getConnectionServer(String serverName) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    	Connection c= null;
    	/*ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.ORA_CONFIG);
    	String user = rbc.getString("database.user").trim();
    	String password = rbc.getString("database.passwd").trim();
    	String host = rbc.getString("database.machine").trim();
    	String port = rbc.getString("database.port").trim();
    	String databaseName = rbc.getString("database.sid").trim();
    	c = getConnection(user, password, host, port, databaseName);
    	*/
    	if(serverName==null || serverName.equals("")){
    		throw new SQLException("Conexiune nespecificata");
    	}else if(serverName.equals("ats01db")){
    		c = getAts01DBConn();
    	}else if(serverName.equals("ats02db")){
    		c = getAts02DBConn();
    	}else if(serverName.equals("ats03db")){
    		c = getAts03DBConn();
    	}else if(serverName.equals("ats04db")){
    		c = getAts04DBConn();
    	}else if(serverName.equals("atsdev")){
    		c = getBetaDBConn();
    	}else if(serverName.equals("beta")){
    		c = getBetaConn();
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

    public static String ruleazaSearchri(int searchId, String serverName, String destServer) throws Exception{
    	
    	Connection c = null;
    	Connection destConn = null;
    	try {
    	c =getConnectionServer(serverName);
    	c.setAutoCommit(false);
    	if(destServer != null) {
    		destConn = getConnectionServer(destServer);
    		destConn.setAutoCommit(false);
    	}
    	int nrTsSearch=0;
    	int nrTsFlags=0;
    	int nrTsData1=0;
    	int nrTsBlob=0;
    	int nrTsproperty=0;
    	int[] nrPropertyBuyer;
    	int[] nrPropertyOwner;
    	
    	nrTsSearch = executeQueryTS_search (searchId, c, destConn);
    	nrTsFlags = executeQueryTS_search_flags(searchId, c, destConn);
    	nrTsData1 = executeQueryTS_search_data1(searchId, c, destConn);
    	nrTsBlob = executeQueryTS_search_data_blob(searchId, c, destConn);
    	nrTsproperty = executeQueryTS_property(searchId , c, destConn);
    	nrPropertyBuyer=executeQueryTS_PropertyBuyer(searchId , c, destConn);
    	nrPropertyOwner=executeQueryTS_PropertyOwner(searchId , c, destConn);
    	executeQueryTS_documents_index(searchId, c, destConn);
    	executeQueryTS_image_count(searchId, c, destConn);
    	if(nrPropertyOwner[1]!=-1 && nrPropertyBuyer[1]!=-1 &&(nrTsSearch>-1 || nrTsSearch<3) && 
    			(nrTsFlags>-1 || nrTsFlags<3) && (nrTsData1>-1 || nrTsData1<3) && (nrTsBlob>-1 || nrTsBlob<3) && (nrTsproperty>-1 || nrTsproperty<3)){
    		c.commit();
    		destConn.commit();
    		if(nrTsproperty==2)
    			nrTsproperty=1;
    		if(nrTsBlob==2)
    			nrTsBlob=1;
    		if(nrTsData1==2)
    			nrTsData1=1;
    		if(nrTsFlags==2)
    			nrTsFlags=1;
    		if(nrTsSearch==2)
    			nrTsSearch=1;
    		return "Success: "+"Rows affected in property_owner="+nrPropertyOwner[0]+"<br />Rows affected in property_buyer="+nrPropertyBuyer[0]+
    		"<br />Rows affected in ts_search="+nrTsSearch+"<br />Rows affected in ts_search_flags="+nrTsFlags+"<br />Rows affected in ts_search_data1="+nrTsData1+
    		"<br />Rows affected in ts_search_data_blob="+nrTsBlob+"<br />Rows affected in ts_property="+nrTsproperty+"<br />This report is from the source machine";
    	}else{
    		c.rollback();
    		return "Error affected rows :"+"Rows affected in property_owner="+nrPropertyOwner[0]+"<br />Rows affected in property_buyer="+nrPropertyBuyer[0]+
    		"<br />Rows affected in ts_search="+nrTsSearch+"<br />Rows affected in ts_search_flags="+nrTsFlags+"<br />Rows affected in ts_search_data1="+nrTsData1+
    		"<br />Rows affected in ts_search_data_blob="+nrTsBlob+"<br />Rows affected in ts_property="+nrTsproperty+" OPERATION WAS ROLLED BACK";
    	}
    	}catch (Exception e){
    		
    		e.printStackTrace();
    		c.rollback();
    		destConn.rollback();
    		return e.getMessage()+" OPERATION WAS ROLLED BACK";
    	}finally{
    		c.close();
    		if(destConn != null)
    		destConn.close();
    	}
 	   
    }
     private static int executeQueryTS_documents_index(int searchId,
			Connection c, Connection destConn) throws Exception {
    	 ResultSet ret1 = null;
  	   PreparedStatement ptsm = getDestinationConnection(destConn, c).prepareStatement("");
  	   
  	   
         try {
         	Statement stmt = c.createStatement();
         	
    	   
         	String s ="select * from ts_documents_index where searchid="+searchId;
         	ret1 = stmt.executeQuery(s);
         	boolean este = ret1.next();
         	
         	
         	if(este==true){
         		getDestinationConnection(destConn, c).setAutoCommit(false);
         		
         		String sql = "insert into ts_documents_index(id, content,searchid, document ) values( " +
         				ret1.getLong("id")+","+
         				"'"+formatString(ret1.getString("content"))+"',"+
         				ret1.getLong("searchid")+","+
         				"?)" +
         						" on duplicate key update " +
         						
         	       				" content='"+formatString(ret1.getString("content"))+"',"+
         	       				" searchid="+ret1.getLong("searchid") +
         	       				", document=?"+
         	       				";";
         		ptsm = getDestinationConnection(destConn, c).prepareStatement(sql);
         		ptsm.setBlob(1, ret1.getBlob("document"));
         		ptsm.setBlob(2, ret1.getBlob("document"));
         		
         		int rowCount = ptsm.executeUpdate();
              System.out.println("executeQueryTS_documentIndex: rowCount = " + rowCount);
              return rowCount;
         		}
         	return 0;
         	}catch(Exception e){
          	e.printStackTrace();
          	return -1;
          }finally{
         		ret1.close();
         	 	ptsm.close();
         		}
		
	}
     
	private static int executeQueryTS_image_count(int searchId,
			Connection c, Connection destConn) throws Exception {
		ResultSet ret1 = null;
		PreparedStatement ptsm = null;
		
		ptsm = getDestinationConnection(destConn, c).prepareStatement("");

		try {
			Statement stmt = c.createStatement();

			String s = "select * from ts_image_count where searchId="
					+ searchId;
			ret1 = stmt.executeQuery(s);
			int rowCount = 0;
			getDestinationConnection(destConn, c).setAutoCommit(false);
			ptsm = getDestinationConnection(destConn, c).prepareStatement("DELETE FROM ts_image_count WHERE searchId = " + searchId);
			while(ret1.next()){
				

				String sql = "insert into ts_image_count(searchId, datasource, count ) values( "
						+ ret1.getLong("searchId")
						+ ","
						+ ret1.getInt("datasource")
						+ ","
						+ ret1.getInt("count")
						+ ");";
				ptsm = getDestinationConnection(destConn, c).prepareStatement(sql);
				
				rowCount += ptsm.executeUpdate();
				System.out.println("executeQueryTS_image_count: rowCount = "
						+ rowCount);
				
			}
			return rowCount;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			ret1.close();
			ptsm.close();
		}

	}
	private static Connection getDestinationConnection(
			Connection destConn, Connection c) {
		if(destConn != null)
			return destConn;
		return c;
	}
	public static void main(String [ ] args){
     	try{
     		
     		int[] searchIdArray = new int[] {6326204};

     		for (int i = 0; i < searchIdArray.length; i++) {
				int searchId = searchIdArray[i];
				System.out.println("Starting for " + searchId);
				try {
					ruleazaSearchri(searchId, "ats03db", "ats04db");
					System.out.println("Success for " + searchId);
				} catch (Exception e) {
					System.err.println("ERROR for " + searchId);
					e.printStackTrace();
				}
	     		
	     		
				
			}
     		System.out.println("Exiting ...");
     	}catch(Exception e){
     		e.printStackTrace();
     	}		
     			
     }
    
    
    
    
    public static int executeQueryTS_search (int searchId,Connection c, Connection destConn) throws Exception {
    	ResultSet ret1 = null;
    	Statement stmt = c.createStatement();
    	Statement stmt1 = null;
 	   if(destConn == null)
 		   stmt1 = c.createStatement();
 	   else
 		   stmt1 = destConn.createStatement();
        try {
        	
        	String s ="select * from ts_search where id="+searchId;
        	ret1 = stmt.executeQuery(s);
        	boolean este = ret1.next();
        	if(este==true){
        	String updateString = " insert into ts_search(" +
        			"id," +
        			"owner_id," +
        			"buyer_id," +
        			"agent_id," +
        			"abstract_id," +
        			"property_id," +
        			"search_type," +
        			"agent_fileno," +
        			"abstr_fileno," +
        			"sdate," +
        			"payrate_id," +
        			"tsr_file_link," +
        			"tsr_sent_to," +
        			"user_rating_id," +
        			"tsr_folder," +
        			"status," +
        			"comm_id," +
        			"tsr_date," +
        			"note_clob," +
        			"note_status," +
        			"agent_rating_id," +
        			"discount_ratio," +
        			"due_date," +
        			"time_elapsed," +
        			"legal_id," +
        			"aux_server_name) values(" +
        	ret1.getLong("id")+","+
        	ret1.getLong("owner_id")+","+
        	ret1.getLong("buyer_id")+","+
        	ret1.getLong("agent_id")+","+
        	ret1.getLong("abstract_id")+","+
        	ret1.getLong("property_id")+","+
        	ret1.getInt("search_type")+","+
        	
        	"'"+formatString(ret1.getString("agent_fileno"))+"',"+
        	"'"+formatString(ret1.getString("abstr_fileno"))+"',"+
        	formatDate(ret1.getObject("sdate"))+","+
        	ret1.getLong("payrate_id")+","+
        	"'"+formatString(ret1.getString("tsr_file_link"))+"',"+
        	"'"+formatString(ret1.getString("tsr_sent_to"))+"',"+
        	ret1.getLong("user_rating_id")+","+
        	"'"+formatString(ret1.getString("tsr_folder"))+"',"+
        	ret1.getInt("status")+","+
        	ret1.getLong("comm_id")+","+
        	formatDate(ret1.getObject("tsr_date"))+","+
        	"'"+formatString(ret1.getString("note_clob"))+"',"+
        	ret1.getInt("note_status")+","+
        	ret1.getLong("agent_rating_id")+","+
        	ret1.getFloat("discount_ratio")+","+
        	formatDate(ret1.getObject("due_date"))+","+
        	ret1.getInt("time_elapsed")+","+
        	ret1.getLong("legal_id")+","+
        	"'"+formatString(ret1.getString("aux_server_name"))+"') " +
        			
        	"on duplicate key update "+
        			"owner_id = " +ret1.getLong("owner_id")+","+
        			"buyer_id = " +ret1.getLong("buyer_id")+","+
        			"agent_id = " +ret1.getLong("agent_id")+","+
        			"abstract_id = " +ret1.getLong("abstract_id")+","+
        			"property_id = " +ret1.getLong("property_id")+","+
        			"search_type = " +ret1.getInt("search_type")+","+
        			"agent_fileno = " +"'"+formatString(ret1.getString("agent_fileno"))+"',"+
        			"abstr_fileno = " +"'"+formatString(ret1.getString("abstr_fileno"))+"',"+
        			"sdate = "+formatDate(ret1.getObject("sdate"))+","+
        			"payrate_id = " +ret1.getLong("payrate_id")+","+
        			"tsr_file_link = " +"'"+formatString(ret1.getString("tsr_file_link"))+"',"+
        			"tsr_sent_to = " +"'"+formatString(ret1.getString("tsr_sent_to"))+"',"+
        			"user_rating_id = " +ret1.getLong("user_rating_id")+","+
        			"tsr_folder = " +"'"+formatString(ret1.getString("tsr_folder"))+"',"+
        			"status = " +ret1.getInt("status")+","+
        			"comm_id = " +ret1.getLong("comm_id")+","+
        			"tsr_date = " +formatDate(ret1.getObject("tsr_date"))+","+
        			"note_clob = " +"'"+formatString(ret1.getString("note_clob"))+"',"+
        			"note_status = " +ret1.getInt("note_status")+","+
        			"agent_rating_id = " +ret1.getLong("agent_rating_id")+","+
        			"discount_ratio = " +ret1.getFloat("discount_ratio")+","+
        			"due_date = " +formatDate(ret1.getObject("due_date"))+","+
        			"time_elapsed = " +ret1.getInt("time_elapsed")+","+
        			"legal_id =" +ret1.getLong("legal_id")+","+
        			"aux_server_name ="+"'"+formatString(ret1.getString("aux_server_name"))+"';" ;
        	//executeUpdate(updateString);
        		return stmt1.executeUpdate(updateString);
        	}
        	return 0;
            }catch(Exception e){
            	e.printStackTrace();
            	return -1;
            }finally {
            	ret1.close();
            	stmt.close();
            }
                   
    }
   public  static int  executeQueryTS_search_flags(int searchId , Connection c, Connection destConn) throws Exception{
	   ResultSet ret1 = null;
	   Statement stmt = c.createStatement();
	   Statement stmt1 = null;
	   if(destConn == null)
		   stmt1 = c.createStatement();
	   else
		   stmt1 = destConn.createStatement();
       try {
       	
       	String s ="select * from ts_search_flags where search_id="+searchId;
       	 ret1 = stmt.executeQuery(s);
       	boolean este = ret1.next();
       	if(este==true){
       		String updateTs_search_flags = "insert into ts_search_flags(" +
       				"id," +
       				"invoice," +
       				"invoiced," +
       				"paid," +
       				"confirmed," +
       				"archived," +
       				"tsr_created," +
       				"checked_by," +
       				"search_id," +
       				"note_status," +
       				"legal_description_status," +
       				"was_opened," +
       				"paid_cadm," +
       				"invoiced_cadm," +
       				"confirmed_cadm," +
       				"invoice_cadm," +
       				"archived_cadm," +
       				"searchOrderStatus," +
       				"searchLogStatus," +
       				"searchIndexStatus," +
       				"starter," +
       				"objectVersionNumber, " +
       				"toDisk , " +
       				"isClosed, " +
       				"sourceCreationType," +
       				"forReview " + 
       				") values( " +
       				ret1.getInt("id") +","+
       				ret1.getInt("invoice") +","+
       				ret1.getInt("invoiced") +","+
       				ret1.getInt("paid") +","+
       				ret1.getInt("confirmed") +","+
       				ret1.getInt("archived") +","+
       				ret1.getInt("tsr_created") +","+
       				ret1.getInt("checked_by") +","+
       				ret1.getLong("search_id") +","+
       				ret1.getInt("note_status") +","+
       				ret1.getInt("legal_description_status") +","+
       				ret1.getInt("was_opened") +","+
       				ret1.getInt("paid_cadm") +","+
       				ret1.getInt("invoiced_cadm") +","+
       				ret1.getInt("confirmed_cadm") +","+
       				ret1.getInt("invoice_cadm") +","+
       				ret1.getInt("archived_cadm") +","+
       				ret1.getInt("searchOrderStatus") +","+
       				ret1.getInt("searchLogStatus") +","+
       				ret1.getInt("searchIndexStatus") +","+
       				ret1.getInt("starter")+","+
       				ret1.getInt("objectVersionNumber")+","+
       				ret1.getInt("toDisk")+","+
       				ret1.getInt("isClosed")+","+
       				ret1.getInt("sourceCreationType") + ","+
       				ret1.getInt("forReview")+
       				
       				
       				") " +
       				
       						" on duplicate key update " +
       						"id ="+ret1.getLong("id") +","+
       						"invoice =" +ret1.getInt("invoice")+","+
       						"invoiced=" +ret1.getInt("invoiced")+","+
       						"paid=" +ret1.getInt("paid")+","+
       						"confirmed=" +ret1.getInt("confirmed")+","+
       						"archived=" +ret1.getInt("archived")+","+
       						"tsr_created=" +ret1.getInt("tsr_created")+","+
       						"checked_by=" +ret1.getInt("checked_by")+","+
       						"note_status=" +ret1.getInt("note_status")+","+
       						"legal_description_status=" +ret1.getInt("legal_description_status")+","+
       						"was_opened=" +ret1.getInt("was_opened")+","+
       						"paid_cadm=" +ret1.getInt("paid_cadm")+","+
       						"invoiced_cadm=" +ret1.getInt("invoiced_cadm")+","+
       						"confirmed_cadm="+ret1.getInt("confirmed_cadm")+","+
       						"invoice_cadm="+ret1.getInt("invoice_cadm")+","+
       						"archived_cadm=" +ret1.getInt("archived_cadm")+","+
       						"searchOrderStatus=" +ret1.getInt("searchOrderStatus")+","+
       						"searchLogStatus=" +ret1.getInt("searchLogStatus")+","+
       						"searchIndexStatus=" +ret1.getInt("searchIndexStatus")+","+
       						"starter="+ret1.getInt("starter")+","+
       						"objectVersionNumber="+ret1.getInt("objectVersionNumber")+","+
       						"toDisk="+ret1.getInt("toDisk")+","+
       						"isClosed="+ret1.getInt("isClosed")+","+
       						"sourceCreationType="+ret1.getInt("sourceCreationType")
       						
       						
       						+ ";";
       		return stmt1.executeUpdate(updateTs_search_flags);
       		}
       return 0;
       	}catch(Exception e){
        	e.printStackTrace();
        	return -1;
        }finally{
       		ret1.close();
       		stmt.close();
       	}
        
   }
   public  static int executeQueryTS_search_data1(int searchId , Connection c, Connection destConn) throws Exception{
	   
	   ResultSet ret1 = null;
	   PreparedStatement ptsm = getDestinationConnection(destConn, c).prepareStatement("");
	   
	   
       try {
       	Statement stmt = c.createStatement();
       	
  	   
       	String s ="select * from ts_search_data1 where searchId="+searchId;
       	ret1 = stmt.executeQuery(s);
       	boolean este = ret1.next();
       	
       	
       	if(este==true){
       		getDestinationConnection(destConn, c).setAutoCommit(false);
       		
       		String updateTS_search_data1 = "insert into ts_search_data1(searchId,dateString,context,version) values( " +
       				ret1.getLong("searchId")+","+
       				"'"+formatString(ret1.getString("dateString"))+"',"+
       				"?,"+
       				ret1.getLong("version")+")" +
       				
       						" on duplicate key update " +
       						
       	       				"dateString='"+formatString(ret1.getString("dateString"))+"',"+
       	       				"context=?,"+
       	       				"version="+ret1.getLong("version")+";";
       		ptsm = getDestinationConnection(destConn, c).prepareStatement(updateTS_search_data1);
       		ptsm.setBlob(1, ret1.getBlob("context"));
       		ptsm.setBlob(2, ret1.getBlob("context"));
       		
       		int rowCount = ptsm.executeUpdate();
            System.out.println("executeQueryTS_search_data1: rowCount = " + rowCount);
            return rowCount;
       		}
       	return 0;
       	}catch(Exception e){
        	e.printStackTrace();
        	return -1;
        }finally{
       		ret1.close();
       	 	ptsm.close();
       		}
        
       	}
   public  static int executeQueryTS_search_data_blob(int searchId , Connection c, Connection destConn) throws Exception{
	   
	   ResultSet ret1 = null;
	   PreparedStatement ptsm = null;
       try {
       	Statement stmt = c.createStatement();
       	String s ="select * from ts_search_data_blob where search_id="+searchId;
       	ret1 = stmt.executeQuery(s);
       	boolean este = ret1.next();
       	c.setAutoCommit(false);
       	if(este==true){
       		Blob searchOrder = ret1.getBlob("searchOrder");
       		Blob searchLog = ret1.getBlob("searchLog");
       		Blob searchIndex = ret1.getBlob("searchIndex");
       		String updateTS_search_data_blob = "insert into ts_search_data_blob(" +
       				"id," +
       				"search_id," +
       				"legal_description," +
       				"note," +
       				"searchOrder," +
       				"searchLog," +
       				"searchIndex) values(" +
       				ret1.getLong("id")+","+
       				ret1.getLong("search_id")+","+
       				"'" +formatString(ret1.getString("legal_description"))+"',"+
       				"'" +formatString(ret1.getString("note"))+"',"+
       				"?,"+
       				"?,"+
       				"?) "+
       				
       				" on duplicate key update " +
       				"id=" +ret1.getLong("id")+","+
       				"legal_description=" +"'" +formatString(ret1.getString("legal_description"))+"',"+
       				"note=" +"'" +formatString(ret1.getString("note"))+"',"+
       				"searchOrder=?,"+
       				"searchLog=?,"+
       				"searchIndex=?";
       		
       		ptsm = getDestinationConnection(destConn, c).prepareStatement(updateTS_search_data_blob);
       		ptsm.setBlob(1, searchOrder);
       		ptsm.setBlob(2, searchLog);
       		ptsm.setBlob(3, searchIndex);
       		ptsm.setBlob(4, searchOrder);
       		ptsm.setBlob(5, searchLog);
       		ptsm.setBlob(6, searchIndex);
       		int rowCount = ptsm.executeUpdate();
            System.out.println("executeQueryTS_search_data_blob: rowCount = " + rowCount);
            return rowCount;
       		
       	}  
       return 0;
   }catch(Exception e){
   	e.printStackTrace();
	return -1;
   }finally{
	   ret1.close();
  	 	//ptsm.close();
   }
   
   }
   
   public  static int executeQueryTS_property(int searchId , Connection c, Connection destConn) throws Exception{
	   
	   ResultSet ret1 = null;
	   ResultSet ret2 = null;
	   Statement stmt = c.createStatement();
	   Statement stmt1 = null;
 	   if(destConn == null)
 		   stmt1 = c.createStatement();
 	   else
 		   stmt1 = destConn.createStatement();
       try {
       	
       	String s ="select property_id from ts_search where id="+searchId;
       	ret1 = stmt.executeQuery(s);
       	boolean este = ret1.next();
       	if(este){
       		String propertyQuery = "select * from ts_property where id="+ret1.getLong("property_id");
       		ret2 = stmt.executeQuery(propertyQuery);
       		boolean este2 = ret2.next();

       	if(este2==true){
       		c.setAutoCommit(false);
       		
       		String updateTS_search_property = "insert into ts_property(" +
       				"id," +
       				"address_no," +
       				"address_direction," +
       				"address_name," +
       				"address_suffix," +
       				"address_unit," +
       				"city," +
       				"county_id," +
       				"state_id," +
       				"zip," +
       				"instrument," +
       				"parcel_id," +
       				"platbook," +
       				"page," +
       				"subdivision," +
       				"lotno," +
       				"isBootstrapped" +
       				") values( " +
       				ret2.getLong("id")+","+
       				"'"+formatString(ret2.getString("address_no"))+"',"+
       				"'"+formatString(ret2.getString("address_direction"))+"',"+
       				"'"+formatString(ret2.getString("address_name"))+"',"+
       				"'"+formatString(ret2.getString("address_suffix"))+"',"+
       				"'"+formatString(ret2.getString("address_unit"))+"',"+
       				"'"+formatString(ret2.getString("city"))+"',"+
       				ret2.getLong("county_id")+","+
       				ret2.getLong("state_id")+","+
       				"'"+formatString(ret2.getString("zip"))+"',"+
       				"'"+formatString(ret2.getString("instrument"))+"',"+
       				"'"+formatString(ret2.getString("parcel_id"))+"',"+
       				"'"+formatString(ret2.getString("platbook"))+"',"+
       				"'"+formatString(ret2.getString("page"))+"',"+
       				"'"+formatString(ret2.getString("subdivision"))+"',"+
       				"'"+formatString(ret2.getString("lotno"))+"',"+
       				ret2.getInt("isBootstrapped")+")" +
       				
       				
       						" on duplicate key update " +
       						
       						
       	       				"address_no='"+formatString(ret2.getString("address_no"))+"',"+
       	       				"address_direction='"+formatString(ret2.getString("address_direction"))+"',"+
       	       				"address_name='"+formatString(ret2.getString("address_name"))+"',"+
       	       				"address_suffix='"+formatString(ret2.getString("address_suffix"))+"',"+
       	       				"address_unit='"+formatString(ret2.getString("address_unit"))+"',"+
       	       				"city='"+formatString(ret2.getString("city"))+"',"+
       	       				"county_id="+ret2.getLong("county_id")+","+
       	       				"state_id="+ret2.getLong("state_id")+","+
       	       				"zip='"+formatString(ret2.getString("zip"))+"',"+
       	       				"instrument='"+formatString(ret2.getString("instrument"))+"',"+
       	       				"parcel_id='"+formatString(ret2.getString("parcel_id"))+"',"+
       	       				"platbook='"+formatString(ret2.getString("platbook"))+"',"+
       	       				"page='"+formatString(ret2.getString("page"))+"',"+
       	       				"subdivision='"+formatString(ret2.getString("subdivision"))+"',"+
       	       				"lotno='"+formatString(ret2.getString("lotno"))+"',"+
       	       				"isBootstrapped="+ret2.getInt("isBootstrapped")+";";
       		
       		
       		int rowCount = stmt1.executeUpdate(updateTS_search_property);
            System.out.println("executeQueryTS_property: rowCount = " + rowCount);
            return rowCount;
       		}
       	
       	}
       	return 0;
       	}catch(Exception e){
        	e.printStackTrace();
        	return -1;
        }finally{
       		ret1.close();
       		ret2.close();
       		}
       
       	}
   
   public  static int[] executeQueryTS_PropertyOwner(int searchId , Connection c, Connection destConn) throws Exception{
	   int[] sumRowsAffected = new int[2];
	   ResultSet ret1 = null;
	   Statement stmt = c.createStatement();
	   Statement stmt1 = null;
	   if(destConn == null)
		   stmt1 = c.createStatement();
	   else
		   stmt1 = destConn.createStatement();
	   try{
		   String query = "select * from property_owner where searchId="+searchId;
		   ret1 = stmt.executeQuery(query);
		   int ret =0;
		   while(ret1.next()){
			   String insertQuery = "insert into property_owner(" +
			   		"id," +
			   		"lastName," +
			   		"firstName," +
			   		"middleName," +
			   		"suffix," +
			   		"prefix," +
			   		"isCompany," +
			   		"ssn4," +
			   		"color," +
			   		"searchId)" +
			   		" values(" +
			   		ret1.getLong("id")+","+
			   		"'" +formatString(ret1.getString("lastName"))+"',"+
			   		"'" +formatString(ret1.getString("firstName"))+"',"+
			   		"'" +formatString(ret1.getString("middleName"))+"',"+
			   		"'" +formatString(ret1.getString("suffix"))+"',"+
			   		"'" +formatString(ret1.getString("prefix"))+"',"+
			   		ret1.getInt("isCompany")+","+
			   		"'" +formatString(ret1.getString("ssn4"))+"',"+
			   		"'" +formatString(ret1.getString("color"))+"',"+
			   		ret1.getLong("searchId")+") " +
			   				" on duplicate key update " +
			   				" lastName='" +formatString(ret1.getString("lastName"))+"',"+
			   				" firstName='" +formatString(ret1.getString("firstName"))+"',"+
			   				" middleName='" +formatString(ret1.getString("middleName"))+"',"+
			   				" suffix='" +formatString(ret1.getString("suffix"))+"',"+
			   				" prefix='" +formatString(ret1.getString("prefix"))+"',"+
			   				" isCompany=" +ret1.getInt("isCompany")+","+
			   				" ssn4='" +formatString(ret1.getString("ssn4"))+"',"+
			   				" color='" +formatString(ret1.getString("color"))+"',"+
			   				" searchId="+ret1.getLong("searchId")+";";
			   ret=  stmt1.executeUpdate(insertQuery);
			   if(ret==1 || ret==2)
				   sumRowsAffected[0]++;
			   //System.out.println("propertyOwner="+ret);
		   }
		   sumRowsAffected[1]=1;
		   return sumRowsAffected;
		   
	   }catch(Exception e){
		   e.printStackTrace();
		   sumRowsAffected[1]=-1;
		   return sumRowsAffected;
	   }
	  
 }
   
   public  static int[] executeQueryTS_PropertyBuyer(int searchId , Connection c, Connection destConn) throws Exception{
	   int[] sumRowsAffected = new int[2];
	   ResultSet ret1 = null;
	   Statement stmt = c.createStatement();
	   Statement stmt1 = null;
 	   if(destConn == null)
 		   stmt1 = c.createStatement();
 	   else
 		   stmt1 = destConn.createStatement();
	   try{
		   String query = "select * from property_buyer where searchId="+searchId;
		   ret1 = stmt.executeQuery(query);
		   int ret =0;
		   while(ret1.next()){
			  
			   String insertQuery = "insert into property_buyer(" +
			   		"id," +
			   		"lastName," +
			   		"firstName," +
			   		"middleName," +
			   		"suffix," +
			   		"prefix," +
			   		"isCompany," +
			   		"ssn4," +
			   		"color," +
			   		"searchId)" +
			   		" values(" +
			   		ret1.getLong("id")+","+
			   		"'" +formatString(ret1.getString("lastName"))+"',"+
			   		"'" +formatString(ret1.getString("firstName"))+"',"+
			   		"'" +formatString(ret1.getString("middleName"))+"',"+
			   		"'" +formatString(ret1.getString("suffix"))+"',"+
			   		"'" +formatString(ret1.getString("prefix"))+"',"+
			   		ret1.getInt("isCompany")+","+
			   		"'" +formatString(ret1.getString("ssn4"))+"',"+
			   		"'" +formatString(ret1.getString("color"))+"',"+
			   		ret1.getLong("searchId")+") " +
			   				" on duplicate key update " +
			   				" lastName='" +formatString(ret1.getString("lastName"))+"',"+
			   				" firstName='" +formatString(ret1.getString("firstName"))+"',"+
			   				" middleName='" +formatString(ret1.getString("middleName"))+"',"+
			   				" suffix='" +formatString(ret1.getString("suffix"))+"',"+
			   				" prefix='" +formatString(ret1.getString("prefix"))+"',"+
			   				" isCompany=" +ret1.getInt("isCompany")+","+
			   				" ssn4='" +formatString(ret1.getString("ssn4"))+"',"+
			   				" color='" +formatString(ret1.getString("color"))+"',"+
			   				" searchId="+ret1.getLong("searchId")+";";
			   ret=  stmt1.executeUpdate(insertQuery);
			   if(ret==1 || ret==2)
			   sumRowsAffected[0]++;
			   //System.out.println("propertyBuyer="+ret);
		   }
		   sumRowsAffected[1]=1;
		   return sumRowsAffected;
	   }catch(Exception e){
		   e.printStackTrace();
		   sumRowsAffected[1]=-1;
		   return sumRowsAffected;
	   }
	   
 }

}

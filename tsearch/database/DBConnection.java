package ro.cst.tsearch.database;
 
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.FileLogger;
//import ro.cst.tsearch.utils.Log;

import org.apache.log4j.Logger;

import com.mchange.v2.c3p0.impl.NewProxyCallableStatement;

/** 
 * DBConnection class wrapp a java.sql.Connection class. 
 * Use Spring jdbc:
 * 	ro.cst.tsearch.database.DBManager.getSimpleTemplate()
 * 	ro.cst.tsearch.database.DBManager.getJdbcTemplate()
 *  ro.cst.tsearch.database.DBManager.getTransactionTemplate()
 *  ro.cst.tsearch.database.DBManager.getNamedParameterJdbcTemplate()
 *  ro.cst.tsearch.database.DBManager.getSimpleJdbcInsert()
 * */
@Deprecated
public class DBConnection {
	
	private static final Logger logger = Logger.getLogger(DBConnection.class);
    
	private Connection conn;
	private long transTime;
	
	public DBConnection( Connection conn ) {
		this.conn = conn;
	}

	public Connection getConnection() {
		return conn;
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		conn.setAutoCommit(autoCommit);
	}

	public void rollback() throws SQLException {
		conn.rollback();
	}

	public void commit() throws SQLException {
		//logger.debug("DBConnection - commit");
		conn.commit();
	}

	public Statement createStatement() throws SQLException {
		return conn.createStatement();
	}

	public PreparedStatement prepareStatement(String sqlPhrase) throws SQLException {
		return conn.prepareStatement(sqlPhrase);
	}
	
	public CallableStatement prepareCall(String sqlPhrase) throws SQLException {
		return conn.prepareCall(sqlPhrase);
	}

	/** Execute the SQL phrase specified by the parameter  */
	public DatabaseData executeSQL( String sqlPhrase ) throws BaseException {
		Statement stmt = null;		
        FileLogger.info( " SQL QUERY: " + sqlPhrase, FileLogger.SQL_FILE_PATH );
		try {						
			stmt = conn.createStatement();
			stmt.execute(sqlPhrase);
			ResultSet result = stmt.getResultSet();
						
			if (result != null) {				
							
				ResultSetMetaData metaData = result.getMetaData();
				if (metaData != null) {
					String[] columnNames = new String[metaData.getColumnCount()];

					for (int i = 0; i < metaData.getColumnCount(); i++) {
						columnNames[i] = metaData.getColumnLabel(i+1).toUpperCase();
					}

					DatabaseData returnData = new DatabaseData(columnNames);

					while (result.next()) {
						for (int j = 0; j < metaData.getColumnCount(); j++) {
							returnData.addValue(metaData.getColumnLabel(j+1).toUpperCase(),
																	result.getObject(j+1));
						}
					}					
					stmt.close();
					return returnData;

				}
			}
			stmt.close();
			return new DatabaseData(new String[0]);
		} catch ( SQLException ex ) {
            if( stmt != null )
            {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            
			throw new BaseException("SQLException: " + ex.getMessage());
		}
	}

	/**
	 * 
	 * @param call the callable statement to be executed
	 * @return the data produced by the execution of the call
	 * @throws BaseException
	 */
	public DatabaseData executeCallableStatementWithResult( NewProxyCallableStatement call ) throws BaseException {

		try {	
			boolean hadResults = call.execute(); 
			ResultSet result = call.getResultSet();
			if (hadResults) {				
							
				ResultSetMetaData metaData = result.getMetaData();
				if (metaData != null) {
					String[] columnNames = new String[metaData.getColumnCount()];

					for (int i = 0; i < metaData.getColumnCount(); i++) {
						columnNames[i] = metaData.getColumnLabel(i+1).toUpperCase();
					}

					DatabaseData returnData = new DatabaseData(columnNames);

					while (result.next()) {
						for (int j = 0; j < metaData.getColumnCount(); j++) {
							returnData.addValue(metaData.getColumnLabel(j+1).toUpperCase(), result.getObject(j+1));
						}
					}					
					call.close();
					return returnData;
				}
			}
			call.close();
			return new DatabaseData(new String[0]);
		} catch ( SQLException ex ) {
			throw new BaseException("SQLException: " + ex.getMessage());
		}
	}

	/** Executes the callable statement specified by the parameter */
	public void executeCallableStatement( NewProxyCallableStatement call) throws BaseException {
		try {
			call.execute();
			call.close();
		} catch ( SQLException ex ) {
			throw new BaseException("SQLException: " + ex.getMessage());
		}
	}
	
	/** 
	 * Execute the prepared SQL phrase specified by the parameter  
	 * !!! BE AWARE the connection stays open until it is closed explicitly 
	 */
	public DatabaseData executePrepQuery( PreparedStatement prepStm ) throws BaseException {
		try {
			ResultSet result;
			prepStm.execute();
			result = prepStm.getResultSet();
			if (result!=null) {
				ResultSetMetaData metaData = result.getMetaData();
				if (metaData != null) {
					String[] columnNames =
					new String[metaData.getColumnCount()];

					for (int i = 0; i < metaData.getColumnCount(); i++) {
						columnNames[i] = metaData.getColumnLabel(i+1).toUpperCase();
					}
					DatabaseData returnData = new DatabaseData(columnNames);

					while (result.next()) {
						for (int j = 0; j < metaData.getColumnCount(); j++) {
							returnData.addValue(metaData.getColumnLabel(j+1).toUpperCase(),
																	result.getObject(j+1));
						}
					}
					return returnData;

				}
			}
			return new DatabaseData(new String[0]);
		}

		catch ( SQLException ex ) {
			throw new BaseException("SQLException: " + ex.getMessage());
		}
	}


	public int executeUpdate(String sqlPhrase) throws BaseException {		
        FileLogger.info( " SQL QUERY: " + sqlPhrase, FileLogger.SQL_FILE_PATH );
		Statement stmt = null;
		int result = 0;
		try {
			stmt = conn.createStatement();
			result = stmt.executeUpdate(sqlPhrase);
			stmt.close();
		} catch ( SQLException ex ) {
            if( stmt != null )
            {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
			throw new BaseException("SQLException: " + ex.getMessage());
		}
		return result;
	}

	public void close() throws BaseException{
		try {
			commit();
		} catch (SQLException ex) {
			logger.error("Failed to set commit on close "+ex.getMessage());
			//ex.printStackTrace();
		} finally {
			try {
				if(!conn.isClosed() )
					conn.close();
			} catch ( SQLException e ) {
				logger.error ("Failed to close logical conn");
				//e.printStackTrace();
			}
		}
	}

	public long getTransTime(){
		return transTime;
	}

	public void setTransTime(long transTime){
		this.transTime = transTime;
	}
}


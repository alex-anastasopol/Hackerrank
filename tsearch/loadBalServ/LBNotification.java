package ro.cst.tsearch.loadBalServ;

import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Vector;

import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Category;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.URLMaping;

public class LBNotification {
	private static final Category logger = Category.getInstance(LBNotification.class.getName());
	public static int SERVER_ADD = 1;
	public static int SERVER_DEL = 2;
	public static int SERVER_ENABLE = 3;
	public static int SERVER_DISABLE = 4;
	public static int SERVER_UPDATE = 5;
	public static int MISC_MESSAGE = 10;
	
	public static boolean getEnable(){
		String sql = "SELECT * FROM " + DBConstants.TABLE_CONFIGS + " WHERE " + 
			DBConstants.FIELD_CONFIGS_NAME + "=\"lbs.notification.enable\"";
		boolean enable = false;
		DBConnection conn = null;
		try{
			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(sql);
			if(data.getRowNumber()>0){
				int value = 0;
				try{
					value = Integer.parseInt((String)data.getValue(DBConstants.FIELD_CONFIGS_VALUE, 0));
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
				if(value!=0)
					enable = true;
			} else {
				insertConfig("lbs.notification.enable","0");
			}
		} catch (Exception e) {
			e.printStackTrace();
			enable = false;
			logger.error("Error retrieving \"lbs.notification.enable\"... using no email notif");
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				logger.error(e);
			}
		}
		return enable;
	}
	/**
	 * Inserts an entry in the configuration table 
	 * @param field
	 * @param value
	 */
	private static void insertConfig(String field, String value) {
		String sql = "INSERT INTO " + DBConstants.TABLE_CONFIGS + " (" +
			DBConstants.FIELD_CONFIGS_NAME + ", " + 
			DBConstants.FIELD_CONFIGS_VALUE + ") VALUES ( ?, ? )";
		
		try {
			DBManager.getSimpleTemplate().update(sql,field,value);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
		}
	}

	public static void sendNotification(int mesType, UserAttributes ua, Object entry){
		String parSubject = "";
		String parMessageText = "";
		MimeMessage mail = null;
		FormatDate formatDate = new FormatDate(FormatDate.DISC_FORMAT_1);
		String emails = getEmails();
		
		if(mesType==SERVER_ADD){
			ServerInfoEntry sie = (ServerInfoEntry)entry;
			parSubject = "ATS Server Added on " + URLMaping.INSTANCE_DIR;
			parMessageText = "New ATS Server Added by " + ua.getLOGIN() + "("+ua.getUserLoginIp()+") at " + 
				formatDate.getLocalDate(Calendar.getInstance()) + "\n" +
				"Ip Address: " + sie.getIp() + "\n" +
				"Ip Mask: " + sie.getIpMask() + "\n" +
				"Host Name: " + sie.getHostName() + "\n" + 
				"Alias: " + sie.getAlias() + "\n" +
				"Path: " + sie.getPath() + "\n" +
				"Enabled: " + (sie.getEnabled()==0?"false":"true");
		} else if(mesType==SERVER_DEL){
			Vector deleted = (Vector)entry;
			parSubject = "ATS Server Deleted on " + URLMaping.INSTANCE_DIR;
			parMessageText = "ATS Server(s) Deleted\n" +
				"Host Name(s): ";
			for (Iterator iter = deleted.iterator(); iter.hasNext();) {
				String element = (String) iter.next();
				parMessageText += element + ", ";
			}
			parMessageText = parMessageText.substring(0,parMessageText.length()-2);
				
		} else if(mesType==SERVER_UPDATE){
			Vector updated = (Vector)entry;
			parSubject = "ATS Server Updated on " + URLMaping.INSTANCE_DIR;
			parMessageText = "ATS Application(s) Updated by " + ua.getLOGIN() + 
				"("+ua.getUserLoginIp()+") at " + 
				formatDate.getLocalDate(Calendar.getInstance()) + "\n";
				
			for (Iterator iter = updated.iterator(); iter.hasNext();) {
				ServerInfoEntry sie = (ServerInfoEntry) iter.next();
				parMessageText += "Host Name: "  + sie.getHostName() + (sie.getEnabled()==0?"  Disabled\n":"  Enabled\n");
			}
			parMessageText = parMessageText.substring(0,parMessageText.length()-1);
		} else if(mesType==SERVER_ENABLE){
			parSubject = "ATS Server Enabled on " + URLMaping.INSTANCE_DIR;
			
		} else if(mesType==SERVER_DISABLE){
			parSubject = "ATS Server Disabled on " + URLMaping.INSTANCE_DIR;
		} else if(mesType==MISC_MESSAGE){
			parSubject = ((String[])entry)[0];
			parMessageText = ((String[])entry)[1];
			emails = MailConfig.getExceptionEmail();
			
		} else {
			logger.error("No mail will be sent!");
			return;
		}
		
		try {
			
			if(emails.length()>0) {
				mail = Log.prepareMailMessage(
						MailConfig.getMailFrom(), 
						emails, 
						"", 
						"", 
						parSubject, 
						parMessageText);
				mail.setContent( parMessageText,"text/plain" );
	            Transport.send(mail);
			}
		} catch (Exception e) {
			logger.error("No mail will be sent!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the emails currently used by the notification service
	 * @return
	 */
	public static String getEmails(){
		String emails = "";
		String sql = "SELECT * FROM " + DBConstants.TABLE_CONFIGS + " WHERE " + 
			DBConstants.FIELD_CONFIGS_NAME + "=\"lbs.notification.email\"";

		DBConnection conn = null;
		try{
			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(sql);
			if(data.getRowNumber()>0){
				emails = (String)data.getValue(DBConstants.FIELD_CONFIGS_VALUE, 0);
			} else {
				insertConfig("lbs.notification.email","");
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error retrieving \"lbs.notification.email\"... using no email notif");
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				logger.error(e);
			}
		}
		return emails;
	}

	/**
	 * Sets the emails for the notification service
	 * @param emails
	 */
	public static void setEmails(String emails) {
		
		String sql = "UPDATE " + DBConstants.TABLE_CONFIGS + " SET " +
			DBConstants.FIELD_CONFIGS_VALUE + "= ? WHERE " + 
			DBConstants.FIELD_CONFIGS_NAME + "=\"lbs.notification.email\"";

		DBConnection conn = null;
		try{
			conn = ConnectionPool.getInstance().requestConnection();
			
			PreparedStatement pstmt = conn.prepareStatement( sql );
			pstmt.setString( 1, emails);
			int nr = pstmt.executeUpdate();
			pstmt.close();
			
			if(nr==0){
				insertConfig("lbs.notification.email", emails);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error retrieving \"lbs.notification.email\"... using no email notif");
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				logger.error(e);
			}
		}
		
		
	}

	/**
	 * Changes the email status from enable to disable and back
	 *
	 */
	public static void changeNotificationStatus() {
		String sql;
		if(getEnable())
			sql = "UPDATE " + DBConstants.TABLE_CONFIGS + " SET " + 
				DBConstants.FIELD_CONFIGS_VALUE + " = 0 WHERE " + 
				DBConstants.FIELD_CONFIGS_NAME + " = \"lbs.notification.enable\"";
		else
			sql = "UPDATE " + DBConstants.TABLE_CONFIGS + " SET " + 
				DBConstants.FIELD_CONFIGS_VALUE + " = 1 WHERE " + 
				DBConstants.FIELD_CONFIGS_NAME + " = \"lbs.notification.enable\"";
		
		DBConnection conn = null;
		try{
			conn = ConnectionPool.getInstance().requestConnection();
			int nr = conn.executeUpdate(sql);
			if(nr==0){
				insertConfig("lbs.notification.enable","1");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error setting \"lbs.notification.enable\"... ");
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				logger.error(e);
			}
		}
		
	}
}

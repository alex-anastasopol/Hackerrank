/*
 * Created on Oct 24, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.servlet;

/**
 * @author nae
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.applet.StringDataSource;
import ro.cst.tsearch.applet.UserEmailData;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.URLMaping;

import com.oreilly.servlet.ParameterParser;

public class SendScreenViaEmail extends HttpServlet{

private static final long serialVersionUID = -6295042228365976189L;
	
private static final Category logger = Category.getInstance(SendScreenViaEmail.class.getName());
String SEPARATOR=",";
private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
private boolean sendMailAsAttach( String parFrom,
								  String parTo,
								  String parCc,
								  String parBcc,
								  String parSubject,
								  String parMessageText,
								  String parMessage){
	try{
		Properties props = System.getProperties();
		props.put("mail.smtp.host", MailConfig.getMailSmtpHost());
		Session session = Session.getDefaultInstance(props,null);
		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(parFrom));
		if (parSubject.length() != 0)
				msg.setSubject(parSubject);
		else
				msg.setSubject("No Subject");
    
		msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(parTo));
    
		if (parCc != null) { 
				msg.setRecipients(javax.mail.Message.RecipientType.CC, InternetAddress.parse(parCc));
		}
    
		if (parBcc != null) { 
				msg.setRecipients(javax.mail.Message.RecipientType.BCC, InternetAddress.parse(parBcc));
		}

		logger.debug("Send mail message from " + parFrom + " to " + parTo 
				+ "; cc " + parCc + "; bcc " + parBcc + "; subject " + parSubject +"; host =" +props.get("mail.smtp.host"));
    
		BodyPart msgBodyPart = new MimeBodyPart();
		msgBodyPart.setText(parMessageText);
    
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(msgBodyPart);            
    
		msgBodyPart = new MimeBodyPart();
		DataSource source = new StringDataSource(parMessage,"text/html");
		msgBodyPart.setDataHandler(new DataHandler(source));
		msgBodyPart.setFileName(parSubject + ".html");
		multipart.addBodyPart(msgBodyPart);
    
		msg.setContent(multipart);

		Transport.send(msg);
		return true;
	}
	catch(Exception e){
		logger.error ("Send page via email failed ! Error message: " + e.getMessage () );
		return false;
	}
}


public void doPost(HttpServletRequest request,
					  HttpServletResponse response)
		throws IOException, ServletException
	  {
		// Trimite mail 
		//logger.info("------------------ BEGIN SendEmailServlet  POST ----------------");

		PrintWriter out = response.getWriter();
        
		String txtTo = request.getParameter("txtTo");
		String txtSubject = request.getParameter("txtSubject");
		String txtMessageText = request.getParameter("txtMessageText");
		String txtBody = request.getParameter("txtBody");
		String txtFrom=request.getParameter("txtFrom");
		String txtCc=request.getParameter("txtCc");
		String txtBcc=request.getParameter("txtBcc");

		logger.info("txtFrom:" + txtFrom + "\ntxtTo:" + txtTo + "\ntxtCc:" + txtCc + "\ntxtBcc:" + txtBcc + "\ntxtSubject:" + txtSubject + "\ntxtMessageText:" + txtMessageText);
		
		if (StringUtils.isBlank(txtFrom)) { 
			txtFrom=MailConfig.getMailFrom();
		}
		if(sendMailAsAttach(txtFrom,txtTo,txtCc,txtBcc,txtSubject,txtMessageText,txtBody))
			out.println("OK");
		else
			out.println ("Error");

		//logger.info("------------------ END SendEmailServlet POST ----------------");
	  }   

public void doGet(HttpServletRequest request, HttpServletResponse response) {
	// Intoarce informatii despre useri si adrese de mail

	//logger.info("------------------ BEGIN SendEmailServlet  GET ----------------");
    
	ParameterParser pp = new ParameterParser(request);
	String CurComId="";
	try{
		CurComId = pp.getStringParameter("comid");
	}catch(Exception e){
		logger.error("Error to get current community! " + e.getMessage());
	}
	
	//logger.info("Current community is " + CurComId);
	/*try{
		CurComId = InstanceManager.getCurrentInstance().getCurrentCommunity().getID().intValue();
	}*/

	DBConnection conn = null;
	DatabaseData ddUsers = null;
	UserEmailData[] users;
	try {
	    
		conn = ConnectionPool.getInstance().requestConnection();
		
		if (conn==null) {
			logger.info("SendEmailServlet: Cannot connect to the database !");
			return;
		}
    
		String sql = "SELECT vu.comm_id," +
						   " vu.user_id," +
						   " vu.login," +
						   " vu.last_name," +
						   " vu.first_name," +
						   " vu.email," + 
						   " vu.hidden_flag" +
					" FROM ts_user vu " +
					" WHERE vu.comm_id=" + CurComId + " AND vu.hidden_flag=0" +
					" ORDER BY vu.last_name";

		ddUsers = conn.executeSQL(sql);
		
		users = new UserEmailData[ddUsers.getRowNumber()];
	    
		String strEmail="";
		String strLogin="";
		String strFirstName="";
		String strLastName="";
		for (int i = 0; i < ddUsers.getRowNumber(); i++) {
			users[i] = new UserEmailData();

			if ((strLogin=(String)ddUsers.getValue("LOGIN", i))!=null)
				users[i].setUserLoginId(strLogin);
			else
				users[i].setUserLoginId("");
			if ((strEmail=(String)ddUsers.getValue("EMAIL", i))!=null)
				users[i].setUserEmailAddr(strEmail);
			else
				users[i].setUserEmailAddr("");

			strLastName=(String)ddUsers.getValue("LAST_NAME", i);
			if (strLastName==null) strLastName="";
			strFirstName=(String)ddUsers.getValue("FIRST_NAME", i);
			if (strFirstName==null) strFirstName="";

			/*logger.info(strLastName+"/"+strEmail);*/

			users[i].setUserName(strLastName.toUpperCase() + ", " + strFirstName);

		}
		
	} catch(Exception e) {
		e.printStackTrace();
		return;
	} finally {
		try {
		    ConnectionPool.getInstance().releaseConnection(conn);
		} catch(BaseException e) {
		    e.printStackTrace();
		}			
	}


	try{
		ObjectOutputStream obj = new ObjectOutputStream(response.getOutputStream());
		obj.writeObject(users);
		obj.flush();
		obj.close();            
	} 
	catch (Exception e) {
		e.printStackTrace();
		return;
	}
	//logger.info("------------------ END SendEmailServlet GET ----------------");
  }
 }
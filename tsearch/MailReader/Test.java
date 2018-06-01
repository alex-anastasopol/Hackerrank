package ro.cst.tsearch.MailReader;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.WorkbookParser;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import ro.cst.tsearch.tsr.UpdateFileCheckFilter;

public class Test {

	public static void main(String[] args) {
		// System.out.println(extractFileName("FW: TSR-UnknownFileNo_04292004_063306.tiff"));

		/*
		 * POP3Reader reader = new POP3Reader("mail.cst.ro", "razvan", "2004apr",null,true);
		 * 
		 * try{
		 * reader.connect();
		 * /*
		 * Message[] vectMessage = reader.getNewMessages();
		 * for (int i=0, n=vectMessage.length; i<n; i++) {
		 * System.out.println(i + ": " + vectMessage[i].getFrom()[0]
		 * + "\t" + vectMessage[i].getSubject()
		 * + "\t" + ((MimeMessage)vectMessage[i]).getMessageID()
		 * + "\t" + reader.getFolder().getUID(vectMessage[i])
		 * );
		 * }
		 * String[] vectSubject = reader.getSubjectsForNewMessages();
		 * for(int i=0; i<vectSubject.length; i++){
		 * System.out.println(i + ": " + vectSubject[i]);
		 * }
		 * 
		 * reader.disconnect();
		 * }
		 * catch(MessagingException e){
		 * System.out.println(e.getMessage());
		 * }
		 */

//		connectTest("imaps", "SIS!SVCATSOrder01", "Passw0rd", "pop3.stewart.com");
		sendText("SIS!SVCATSOrder01", "Passw0rd");
		
	}

	public static int connectTest(String SSL, final String user, final String pwd, String host){

		Properties props = System.getProperties();
//		props.setProperty("mail.store.protocol", SSL);
//		props.setProperty("mail.imaps.ssl.trust", host);
//		props.setProperty("mail.imaps.connectionpoolsize", "10");
		props.setProperty("mail.host", "smtp.stewart.com");
		props.setProperty("mail.smtp.host", "smtp.stewart.com");
		props.setProperty("mail.debug", "true");

		try {

			Session session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(user, pwd);
				}
			  });

			// session.setDebug(true);

//			Store store = session.getStore(SSL);
//			store.connect(host, user, pwd);
//			Folder inbox = store.getFolder("INBOX");
//
//			inbox.open(Folder.READ_ONLY);
//			int numMess = inbox.getMessageCount();
//			Message[] messages = inbox.getMessages();
//
//			for (Message m : messages) {
//
//				m.getAllHeaders();
//				m.getContent();
//			}
//
//			inbox.close(false);
//			store.close();
			
			System.out.println("Finishing with inbox");
			
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("atsorder@stewart.com"));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse("aalecu@gmail.com"));
			message.setSubject("Testing Subject6");
			message.setText("Dear Mail Crawler,"
				+ "\n\n No spam to my email, please!");
 
			Transport.send(message);
			
			System.out.println("Finishing send email");
			
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static int sendText(final String user, final String pwd){

		Properties props = System.getProperties();
		props.setProperty("mail.smtp.host", "smtp.stewart.com");
		props.setProperty("mail.debug", "true");

		try {

			Session session = Session.getInstance(props, null);

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("atsorder@stewart.com"));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse("aalecu@stewart.com"));
			message.setSubject("Testing Subject6");
			message.setText("Dear Mail Crawler,"
				+ "\n\n No spam to my email, please!");
 
			Transport.send(message);
			
			System.out.println("Finishing send email");
			
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}

package ro.cst.tsearch.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.ServerConfig;

public class Log {

    public static final Category logger = Logger.getLogger(Log.class);

    public static final String DETAILS_PREFIX = "details.";

    /**
     * Send email from ATS system to a specified address
     */
    public static void sendEmail(String to, String subj, Object obj) {
    	sendEmail(to, subj, obj, null);
    }
    public static void sendEmail(String to, String subj, Object obj, String[] fileNames) {
        String parFrom = MailConfig.getMailFrom();
        if (to == null)
            to = MailConfig.getMailLoggerToEmailAddress();
        if (to.equals(""))
            return;
        try {
            MimeMessage msg = prepareMailMessage(parFrom, to, "", "", subj, obj.toString(), fileNames);
            Transport.send(msg);
        } catch (AddressException ae) {
            logger.error("Error " + ae.getMessage() + " on trying to send " + obj);

        } catch (MessagingException me) {
            logger.error("Error" + me.getMessage() + " on trying to send " + obj);
        }
        logger.info("Logger Mail sent to " + to);

    }

    static void deleteMultipleFile(String filesName){
    	String[] files = filesName.split(",");
    	for (int i = 0; i < files.length; i++) {
    		deleteFile(files[i]);
    	}
    }
    static void attachMultipleFiles(String pFileNames, MimeMessage msg)
	throws MessagingException, IOException {
	
		//scrierea in fisier
		BodyPart msgBodyPart;
		File file = null;
		FileDataSource fds=	null;
		// attach the file to the message
		String[] files = pFileNames.split(",");
		for (int i = 0; i < files.length; i++) {
			msgBodyPart = new MimeBodyPart();
			file = new File(files[i]);
			if(file.exists()){
				fds = new FileDataSource(file);
				msgBodyPart.setDataHandler( new DataHandler(fds));
				msgBodyPart.setFileName(fds.getName());
				((Multipart) msg.getContent()).addBodyPart(msgBodyPart);
			}
		}
		file = null;
	}
    
    private static void attachFile(String pFileName, MimeMessage msg)
            throws MessagingException, IOException {
        BodyPart msgBodyPart;
        // attach the file to the message
        msgBodyPart = new MimeBodyPart();
        FileDataSource fds = new FileDataSource(pFileName);
        msgBodyPart.setDataHandler(new DataHandler(fds));
        msgBodyPart.setFileName(fds.getName());
        ((Multipart) msg.getContent()).addBodyPart(msgBodyPart);
        fds.getInputStream().close();
    }
    public static void deleteFile(String fileName){
        java.io.File file = new File (fileName);
        java.io.FileWriter out;
        try {
            out = new FileWriter (file);
            out.close (); // THIS IS THE IMPORTANT BIT            
            String parent = new File(fileName).getParent();
            if(new File(fileName).delete())
                System.out.println("File succesfully deleted!");
            else
                System.out.println("Problems to delete file");
            new File(parent).delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
   //end added by danutz 
    
    
    
    public static void sendEmail(Object obj) {
        String to = MailConfig.getMailLoggerToEmailAddress();
        String subj = MailConfig.getMailLoggerSubject() + " on " + ServerConfig.getAppUrl();
        sendEmail(to, subj, obj);

    }

    public static void sendEmail2(String subj, Object obj) {
        String to = MailConfig.getMailLoggerToEmailAddress();
        sendEmail(to, subj, obj);

    }

    public static void sendEmail(String to, Object obj) {
        String subj = MailConfig.getMailLoggerSubject() + " on " + ServerConfig.getAppUrl();
        sendEmail(to, subj, obj);

    }

    public static void sendExceptionViaEmail(Exception e) {
        Log.sendEmail(exceptionToString(e));
    }
    
    /**
     * Sends an email to the default email address <i>mail.logger.to</i><br>
     * The Subject contains the exception message + the name of the current server<br>
     * The text contains the exception (full stack trace) and the extra text
     * @param e exception to be logged
     * @param extraText text to be added in email
     */
    public static void sendExceptionViaEmail(Exception e, String extraText) {
        Log.sendEmail2(e.getMessage() + " on " + ServerConfig.getAppUrl(),
        		exceptionToString(e, extraText));
    }

    public static void sendExceptionViaEmail(String to, String subj, Exception e) {
        Log.sendEmail(to, subj, exceptionToString(e));
    }
    
    public static void sendExceptionViaEmail(String to, String subj, Exception e, String extraText) {
        Log.sendEmail(to, subj, exceptionToString(e, extraText));
    }

    public static void sendExceptionViaEmail(String to, Exception e) {
        Log.sendEmail(to, exceptionToString(e));
    }

    public static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.println();
        pw.close();
        return e + "\n" + /* Arrays.asList(e.getStackTrace()) */sw;
    }
    
    private static String exceptionToString(Exception e, String extraText) {
    	
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if(e != null) {
        	e.printStackTrace(pw);
        }
        pw.println();
        pw.println("--------------------------- Extra Info --------------------------");
        pw.println(extraText);
        pw.close();
        return sw.toString();
    }

    public static MimeMessage prepareMailMessage(String parFrom, String parTo, String parCc, String parBcc,
            String parSubject, String parMessageText) throws AddressException, MessagingException {
    
    	return prepareMailMessage(parFrom, parTo, parCc, parBcc, parSubject, parMessageText, null);
    }
    
    public static MimeMessage prepareMailMessage(String parFrom, String parTo, String parCc, String parBcc,
            String parSubject, String parMessageText, String[] parAttachFilename) throws AddressException, MessagingException {
        String mailHost = MailConfig.getMailSmtpHost();
        Properties props = System.getProperties();
        props.put("mail.smtp.host", mailHost);

        Session session = Session.getDefaultInstance(props, null);

        MimeMessage msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress(parFrom));
        if (parSubject != null && parSubject.length() != 0)
            msg.setSubject(parSubject);
        else
            msg.setSubject("No Subject");

        msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(parTo));
        //logger.info ("am parsat to" + parTo );

        if (parCc != null) {
            msg.setRecipients(javax.mail.Message.RecipientType.CC, InternetAddress.parse(parCc));
        }

        if (parBcc != null) {
            msg.setRecipients(javax.mail.Message.RecipientType.BCC, InternetAddress.parse(parBcc));
        }

        Multipart multipart = new MimeMultipart();
        
        if (parAttachFilename != null) {
        	
        	for (int i = 0; i < parAttachFilename.length; i++){
        		
	        	if(parAttachFilename[i] != null) {
		        	BodyPart attachementPart = new MimeBodyPart();
		        	attachementPart.setDataHandler(new DataHandler(new FileDataSource(parAttachFilename[i])));
		        	attachementPart.setFileName(parAttachFilename[i].replaceAll(".*\\\\", ""));
		        	multipart.addBodyPart(attachementPart);
	        	}
	        }
        	
        }
        
        msg.setContent(multipart);

        BodyPart msgBodyPart = new MimeBodyPart();
        msgBodyPart.setText(parMessageText);

        multipart.addBodyPart(msgBodyPart);

        return msg;
    }
	public static Category getLogger() {
		return logger;
	}

}
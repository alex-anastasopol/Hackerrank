package ro.cst.tsearch.MailReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimerTask;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.FileUtils;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.ProductsMapper;
import ro.cst.tsearch.emailOrder.MailOrder;
import ro.cst.tsearch.emailOrder.PlaceOrder;
import ro.cst.tsearch.exceptions.InvalidEmailOrderException;
import ro.cst.tsearch.loadBalServ.LoadBalancingStatus;
import ro.cst.tsearch.servlet.FileServlet;
import ro.cst.tsearch.utils.StringUtils;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.util.BASE64DecoderStream;

/**
 * @author radu bacrau
 */
public class MailOrderDaemonTimerTask extends TimerTask {
	
	private String attachmentPath = null;	
	private String succeededFolderName = null;
	private String failedFolderName = null;
	private int maxMessages = 0;
	private String supportAddress = null;
	private String ordersAddress = null;
	private String host = null;
	private String user = null;
	private String password = null;
	private String searchOrdersAddress = null;
	private String protocol = null;
	private boolean replyEnabled = false;
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
	private static Session session = null;
	private static IMAPStore store = null;
	private static IMAPFolder inboxFolder = null;
	private static IMAPFolder succeededOrdersFolder = null;
	private static IMAPFolder failedOrdersFolder = null;
	
	/**
	 * constructor
	 */
	public MailOrderDaemonTimerTask(
			String host, 
			String username, 
			String password, 
			String attachmentPath,
			String supportAddress,
			String searchOrdersAddress,
			String ordersAddress,
			String succeededFolderName,
			String failedFolderName,
			String protocol,
			int maxMessages,
			boolean replyEnabled
			) 
	{
		this.host = host;
		this.user = username;
		this.password = password;
		this.attachmentPath = attachmentPath;
		this.supportAddress = supportAddress;
		this.searchOrdersAddress = searchOrdersAddress;
		this.ordersAddress = ordersAddress;
		this.succeededFolderName = succeededFolderName;
		this.failedFolderName = failedFolderName;
		this.protocol = protocol;
		this.maxMessages = maxMessages;
		this.replyEnabled = replyEnabled;		
	}

	/**
	 * make sure we have a session, a connected store and an opened inbox folder
	 * @throws MessagingException
	 */
	private void checkConnection() throws MessagingException{
		// make sure we have a session
//		store = null;
		if(session == null) {
			Properties props = System.getProperties();
		    props.put("mail.smtp.host", getSmtpHost());
	    	session = Session.getInstance(props, null);
		}
    	
		// make sure we have a store
		if(store == null){
			store = (IMAPStore)session.getStore(protocol);
		}
		
		// make sure the store is connected
		if(!store.isConnected()){
			
			// close the inbox it is open
			if((inboxFolder != null)&&(inboxFolder.isOpen())){
				inboxFolder.close(false);
			}
			// conect the store
			store.connect(host, user, password);				
			// get the folders
			inboxFolder = (IMAPFolder)store.getFolder("INBOX");
			succeededOrdersFolder = (IMAPFolder)store.getFolder(succeededFolderName);
			failedOrdersFolder = (IMAPFolder)store.getFolder(new URLName(failedFolderName));
		}
		
		if(!inboxFolder.isOpen()){
			inboxFolder.open(Folder.READ_WRITE);
		}
//		if(succeededOrdersFolder.exists()) {
//			if(!succeededOrdersFolder.isOpen()) {
//				succeededOrdersFolder.open(Folder.READ_WRITE);
//			}
//		}
//		
//		if(failedOrdersFolder.exists()) {
//			if(!failedOrdersFolder.isOpen()) {
//				failedOrdersFolder.open(Folder.READ_WRITE);
//			}
//		}
	}
	
	/**
	 * A messaging error appeared when trying to open/close
	 * the session, store, folder or read messages
	 * Send warning to support
	 * @param me
	 */
	void processRunGenericException(MessagingException me, boolean messageAlreadySent){
		// close the session
		String errorMessage = "Error occured while trying to retrieve and process the email orders.\nException: " + StringUtils.exception2String(me);
		MailOrderDaemon.getLogger().error("Error occured while trying to retrieve and process the email orders", me);
		try{
			if(!messageAlreadySent){
				sendMailToSupport(null, errorMessage, "warning: email order daemon run not completed successfully" );
			}
			// close inbox
			if((inboxFolder != null)&&inboxFolder.isOpen()){
				inboxFolder.close(true);
			}
			// clear all connection-related variables
			if(store != null && !store.isConnected()){
				store.close();
			}
			session = null;
		}catch(MessagingException me2){
			MailOrderDaemon.getLogger().error("Problem in processRunGenericException", me2);
		}
	}
	
	/**
	 * The message was not moved to the right place
	 * Send mail support to move it - the message is marked as SEEN, so we will not process it again 
	 * @param me
	 * @param crtMessage
	 * @param succeeded
	 * @throws MessagingException
	 */
	private void processMesageCopyException(MessagingException me, Message crtMessage, boolean succeeded) throws MessagingException{
		String errorMessage;	
		me.printStackTrace();
		// setup values used for dislaying info
		String destFolder = null;
		String succString = null;
		if(succeeded){
			destFolder = succeededFolderName;
			succString = "SUCCEEDED";
		}else{
			destFolder = failedFolderName;
			succString = "FAILED";
		} 
		errorMessage = "Could not copy " + succString + " message from inbox to " + destFolder + ".\n";
		errorMessage += ("PLEASE do it manually.\n\n");
		errorMessage += ("Exception ="  + StringUtils.exception2String(me) + "\n");
		
		sendMailToSupport(crtMessage, errorMessage, "ACTION NEEDED: move message to " + destFolder);
		
		errorMessage += ("Message has the following subject: " + crtMessage.getSubject()  + " and was sent on " + crtMessage.getSentDate() + "\n");
		MailOrderDaemon.getLogger().error(errorMessage);
	}
	
	/**
	 * A problem appeared with the message - like the attachments were not downloaded
	 * send warning to support
	 * @param crtMessage
	 * @param e
	 * @throws MessagingException
	 */
	void processMessageGenericException(Message crtMessage, Exception me){
		me.printStackTrace();
		try{
			String errorMessage = "Message was not treated successfully.\nException: " + StringUtils.exception2String(me);
			MailOrderDaemon.getLogger().error(errorMessage);
			sendMailToSupport(crtMessage, errorMessage, "warning: message not treated successfully" );
		}catch(MessagingException me2){
			me2.printStackTrace();
			MailOrderDaemon.getLogger().error(StringUtils.exception2String(me2));
		}
	}
	/**
	 * The message was copied to the right place, but was not deleted
	 * Send mail to support to delete it manually
	 * @param me
	 * @param crtMessage
	 * @throws MessagingException
	 */
	private void processMessageDeleteException(MessagingException me, Message crtMessage, String folderName){
		try{
			String errorMessage;
			errorMessage = "Could not delete message from inbox after copying it to " + folderName + ".\n";
			errorMessage += ("PLEASE do it manually.");
			// send mail to support
			sendMailToSupport(crtMessage, errorMessage, "ACTION NEEDED: please delete message");
			//log the error
			errorMessage += ("Message has the following subject: " + crtMessage.getSubject()  + " and was sent on " + crtMessage.getSentDate() + "\n");
			errorMessage += ("Exception ="  + StringUtils.exception2String(me) + "\n");
			MailOrderDaemon.getLogger().error(errorMessage);
		}catch(MessagingException me2){
			me2.printStackTrace();
			MailOrderDaemon.getLogger().error(StringUtils.exception2String(me2));
		}
	}
	
	/**
	 * The attachement either had not pdf attachments or had no attachments at all
	 * Send reply, with cc to support 
	 * @param crtMessage
	 * @param attachementFileNames
	 * @param badAttachementNames
	 * @throws MessagingException
	 */
	private void processAttachementNameTypeProblem(Message crtMessage, String[] attachementFileNames, List<String> badAttachementNames){
		try{
			String subject = "";
			String body = "";
			if(!replyEnabled){
				subject = "EMAIL_ORDER: ";
			}
			if((attachementFileNames.length==0) && (badAttachementNames.size() == 0)){
				// no attachments at all
				subject += ("WARN: " + crtMessage.getSubject() + " - no attachments");
				body += "Your email message does not have attachments.\n";
			}else if(attachementFileNames.length==0){
				// no valid attachments - there are badly named attachments
				subject += ("WARN: " + crtMessage.getSubject() + " - unsupported attachment types");
				body += "Your email message has only unsupported attachment types.\n";
			}else if(badAttachementNames.size() != 0){
				// there are also bad attachments
				subject += ("WARN: " + crtMessage.getSubject() + " - unsupported attachment types");
				body += "Your email message has unsupported attachment types.\n";
			}
			
			if(!replyEnabled){
				body = "ATTENTION: This message was sent only to support because REPLY feature is disabled!\n\n" + body;
			}
			body += "This account is designated only for receiving orders as pdf/txto attachments.\n";
			body += "Thank you\n";
			//body += ("\nOriginal Email Subject: " + crtMessage.getSubject() + "\n");
			//body += ("Original Email Sent Date: " + crtMessage.getSentDate() + "\n");
			body += ("\n" + describeMessage(crtMessage));
			if(badAttachementNames.size() != 0){
				body += ("Ignored Attachments: " + printStringList(badAttachementNames) + "\n");
			}
			
			// from, to, cc, subject, body, attachement
			Address from, to[], cc[];
			from = new InternetAddress(searchOrdersAddress);
			if(replyEnabled){
				to = crtMessage.getFrom();
				cc = parseAddresses(supportAddress);
				MailOrderDaemon.getLogger().debug("Reply to be sent to (cc support): " + subject);
			}else{
				to = parseAddresses(supportAddress);
				cc = null;
				MailOrderDaemon.getLogger().debug("Email to be sent to support: " + subject);
			}

			// send the message
			sendMessage(from, to, cc, subject, body, null, null);
			
		}catch(MessagingException me2){
			me2.printStackTrace();
			MailOrderDaemon.getLogger().error(StringUtils.exception2String(me2));
		}
	}
	
	/**
	 * 
	 * @param e
	 * @param crtFileName
	 * @param crtMessage
	 * @param orderFileName
	 * @param fileNo
	 * @param agentEmail
	 */
	private void processInvalidEmailOrderException(InvalidEmailOrderException e, String crtFileName, Message crtMessage, String orderFileName, String fileNo, String agentEmail){
		
		e.printStackTrace();
		MailOrderDaemon.getLogger().error(StringUtils.exception2String(e));
		
		try{
			// fill in subject
			String subject = "";
			if(!replyEnabled){
				subject = "EMAIL_ORDER: ";
			}
		    subject += "ERR: " + crtMessage.getSubject() + " - order";
		    if(fileNo != null){
		    	subject += (" " + fileNo + " ");
		    }
		    subject += "not placed!";

		    // fill in body
		    String body = "";
		    body += "This order was not placed due to following reason(s):\n";
		    body += e.getCausesDescription();
		    body += "\nPlease review this order and resend it or contact your service provider.\n";
		    body += "Thank you\n";
			
			if(!replyEnabled){
				body = "ATTENTION: This message was sent only to support because REPLY feature is disabled!\n\n" + body;
			}
			//body += ("\nOriginal Email Subject: " + crtMessage.getSubject() + "\n");
			//body += ("Original Email Sent Date: " + crtMessage.getSentDate() + "\n");
			body += ("\n" + describeMessage(crtMessage));
			
			// from, to, cc, subject, body, attachement
			Address from, to[], cc[];
			from = new InternetAddress(searchOrdersAddress);
			if(replyEnabled){
				to = crtMessage.getFrom();
				cc = parseAddresses(supportAddress);
				MailOrderDaemon.getLogger().error("Reply sent (cc support): " + subject);
			}else{
				to = parseAddresses(supportAddress);
				cc = null;
				MailOrderDaemon.getLogger().error("Email sent to support: " + subject);
			}
			
			// prepare attachment file names
			String crtRealFileName = crtFileName.substring(attachmentPath.length()+1).replaceFirst("[0-9]+_[0-9]+_","");
			String [] diskFileNames = new String [] {crtFileName, orderFileName};
			String [] originalFileNames = new String [] {crtRealFileName, "orderFile.html"};
			
			// send the message
			sendMessage(from, to, cc, subject, body, diskFileNames, originalFileNames);
			
		}catch(MessagingException me2){
			me2.printStackTrace();
			MailOrderDaemon.getLogger().error(StringUtils.exception2String(me2));
		}
	}
	
	private boolean processOneMessage(Message crtMessage) throws MessagingException, IOException{
		// parse product type
		String crtSubject = crtMessage.getSubject();
		
		boolean succeeded = true;
		String infoMessage;
						
		// mark as unread. mark as read only after successfully downloading the attachments
		crtMessage.setFlag(Flag.SEEN, false);
		
		// get attachments
		String [] attachementFileNames = getAttachements(inboxFolder, crtMessage, attachmentPath);
				
		// mark message as seen - ats tried to process it, succeeded downloading the attachemens - do not try again
		crtMessage.setFlag(Flag.SEEN, true);
		
		// check for no attchments or for non-pdf attachments
		if((attachementFileNames.length == 0) || (badAttachementNames.size() !=0)){
			
			// send email to support about non-pdf attachments or no good attachemets at all
			processAttachementNameTypeProblem(crtMessage, attachementFileNames, badAttachementNames);
			
			// crt message had problems
			succeeded = false;
		}
		
		/*
		 * treat the pdf attachments
		 */ 

		if(attachementFileNames.length != 0){
			MailOrderDaemon.getLogger().debug("Message has #" + attachementFileNames.length + " attachments.");
		}
			
		for(int atchNo=0; atchNo<attachementFileNames.length; atchNo++){
			/*
			 * treat one attachement
			 */
			String orderFileName = null;
			String fileNo = null;
			
			String crtFileName = attachementFileNames[atchNo];
			String crtRealFileName = crtFileName.substring(attachmentPath.length()+1).replaceFirst("[0-9]+_[0-9]+_","");
			
			infoMessage = "Attachement #" + (atchNo+1) + "/" + attachementFileNames.length + " : " + crtRealFileName;
			MailOrderDaemon.getLogger().debug(infoMessage);
			
			MailOrder mailOrder = new MailOrder();
			boolean searchCreated = false;
			Search search = null;
			
			try{				
				
				// get search from pdf
				search = mailOrder.getSearch(crtFileName);
				
				boolean txto = crtFileName.matches("(?i)^.*\\.txto$");
				int productType = 1;
				if(!txto){
					productType = parseProductType(crtSubject, crtFileName);
				}else{
					productType = parseProductType(mailOrder.product);
				}
				
				// set product type
				search.getSa().setAtribute(SearchAttributes.SEARCH_PRODUCT, Integer.toString(productType));
				
				// place order
				PlaceOrder.placeOrder(search, mailOrder, mailOrder.setDates, "email");
				 
				// mark search as created
				searchCreated = true;
				
				// set orderFileName
//				orderFileName = search.getSearchDir() + "orderFile.html";	
//				if(!new File(orderFileName).exists()){
//					orderFileName = null;
//				}
				
				byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(search.getID(), FileServlet.VIEW_ORDER, false);
				if(orderFileAsByteArray != null && orderFileAsByteArray.length != 0 ) {
					File file = new File(ServerConfig.getTsrCreationTempFolder() + File.separator + "temp_orderFile" + search.getID() + ".html");
					if(file.exists()) {
						file.delete();
					}
					FileUtils.writeStringToFile(file, new String(orderFileAsByteArray));
					orderFileName = file.getPath();
				}
				
				// set fileNo
				fileNo = mailOrder.fileID;
				
			}catch(Exception e){
				
				// set orderFileName
//				if(search != null){
//					orderFileName = search.getSearchDir() + "orderFile.html";	
//					if(!new File(orderFileName).exists()){
//						orderFileName = null;
//					}	
//				}								
				// set fileNo
				if(mailOrder != null){
					fileNo = mailOrder.fileID;
				}
				
				
				if(search != null && orderFileName == null) {
					byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(search.getID(), FileServlet.VIEW_ORDER, false);
					if(orderFileAsByteArray != null && orderFileAsByteArray.length != 0 ) {
						File file = new File(ServerConfig.getTsrCreationTempFolder() + File.separator + "temp_orderFile" + search.getID() + ".html");
						if(file.exists()) {
							file.delete();
						}
						FileUtils.writeStringToFile(file, new String(orderFileAsByteArray));
						orderFileName = file.getPath();
					}
				}
				
				if(e instanceof InvalidEmailOrderException){
					processInvalidEmailOrderException((InvalidEmailOrderException)e, crtFileName, crtMessage, orderFileName, fileNo, null);
				}else{
					processGenericOrderPlacementException(crtMessage, crtFileName, e, orderFileName, fileNo);
				}	
					
				// current message had at least 1 problem
				succeeded = false;
			}
			
			if(searchCreated){
				// order was ok
				infoMessage = "Attachement was processed successfully.\nFile: " + crtRealFileName;
				MailOrderDaemon.getLogger().info(infoMessage.replace("\n", " "));
				// success: e-mail to orders address 
				sendMailToSupport(crtMessage, infoMessage, "SUCCESS: order " + fileNo + " placed", crtFileName, orderFileName, fileNo, ordersAddress);
			}
		}
		return succeeded;
	}

	
	/**
	 * this method is called whenever the task needs to run
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		if(!MailConfig.isMailOrderStart() || !LoadBalancingStatus.isCurrentServerEnabled()) {
			return;
		}
		
		boolean errorMessageSent = false;
		
		MailOrderDaemon.getLogger().error("\nMailOrderDaemonTask#run - start");
				
		try{
			// make/revive connection
			checkConnection();
			
			// get the messages
			Message [] messages =  IMAPReader.getNewMessages(inboxFolder, maxMessages);
			
			// log
			MailOrderDaemon.getLogger().debug("Processing #" + messages.length + " messages.");		
			
			// process all messages
			for(int msgNo = 0; msgNo<messages.length; msgNo++){		
				// error message was not sent
				errorMessageSent = false;
				
				// crt message
				Message crtMessage = messages[msgNo]; 
			
				// log
				MailOrderDaemon.getLogger().debug("Treating message #" + (msgNo+1) +"/" + messages.length + " subject=" + crtMessage.getSubject() + " sent on " + crtMessage.getSentDate());
				
				// process one message
				boolean succeeded = true;
				try{	
					//process the message
					succeeded = processOneMessage(crtMessage);
				}catch(Exception e){
					// there was a problem with the message - like could not retrieve attachments - send warning to support
					processMessageGenericException(crtMessage, e);	
					if(e instanceof FolderClosedException){
						// skip message processing by re-throwing the exception
						errorMessageSent = true;
						throw (FolderClosedException)e;
					}
					// at least one problem with current message
					succeeded = false;
				}
			
				// copy the message into the corresponding folder
				boolean messageCopied = false;
				try{					
					inboxFolder.copyMessages(new Message [] {crtMessage}, succeeded?succeededOrdersFolder:failedOrdersFolder);
					messageCopied = true;
				}catch(MessagingException me){
					me.printStackTrace();
					// message not copied - send mail to support to move it in the right place
					processMesageCopyException(me, crtMessage, succeeded);
				}
				// delete message that was successfully copied into the right folder
				if(messageCopied){
					try{
						// mark message as deleted
						crtMessage.setFlag(Flag.DELETED, true);
					}catch(MessagingException me){
						// message not marked as deleted: send mail to support to delete it, it is already copied to the right folder
						processMessageDeleteException(me, crtMessage, succeeded?succeededFolderName:failedFolderName);
					}
				}
			}
			
			// close inbox - this will delete the original messages that were marked as deleted
			inboxFolder.close(true);

		}catch(MessagingException me){
			// there was an error related to open folder, make connection, etc - send warning to support
			processRunGenericException(me, errorMessageSent);
		}
		
		MailOrderDaemon.getLogger().error("MailOrderDaemonTask#run - stop");
	}

	
	/**
	 * 
	 * @param subject
	 * @return the search type
	 */
	private int parseProductType(String subject, String fileName){
		int retVal = SearchAttributes.SEARCH_PROD_FULL;		
		productTypeString = Products.Product1Name;	
		subject = subject.toLowerCase();		
		boolean foundStandardForm = false;
		
		List<ProductsMapper> products = Products.getProductList();
		for (ProductsMapper prod : products) {
			String crtProd = prod.getAlias().toLowerCase();
			if (subject.contains(crtProd)){
				retVal = prod.getProductId();
				productTypeString = prod.getAlias();
				foundStandardForm = true;
				break;
			}
		}
		if(!foundStandardForm){
			if(subject.toLowerCase().matches(".*\\brefi\\b.*")){
				retVal = SearchAttributes.SEARCH_PROD_REFINANCE;
				productTypeString = "refinance";				
			}
			if(fileName.toLowerCase().matches(".*\\brefi\\b.*")){
				retVal = SearchAttributes.SEARCH_PROD_REFINANCE;
				productTypeString = "refinance";				
			}
		}
		MailOrderDaemon.getLogger().error("product="+productTypeString);
		return retVal;
	}
    
	/**
     * 
     * @param prodName
     * @return
     */
	private int parseProductType(String prodName){
		int retVal = SearchAttributes.SEARCH_PROD_FULL;
		productTypeString = Products.Product1Name;	
		
		List<ProductsMapper> products = Products.getProductList();
		for (ProductsMapper prod : products) {
			if (prod.getAlias().equalsIgnoreCase(prodName)){
				retVal = prod.getProductId();
				productTypeString = prod.getAlias();
				break;
			}
		}
		return retVal;
	}
	
	private List<String> badAttachementNames = null;
	private String productTypeString = "full search";
	
	/**
	 * 
	 * @param folder
	 * @param message
	 * @param path
	 * @return
	 * @throws MessagingException
	 * @throws IOException
	 */
	public String[] getAttachements(IMAPFolder folder, Message message, String path) throws MessagingException, IOException {
		
		badAttachementNames = new ArrayList<String>();
		
		System.out.println("Read attachments START.");
		ArrayList<String> messageList = new ArrayList<String>();
		
		Object messageContent = message.getContent();
		
		// check if message mime type is "appllication/pdf". in this case the whole email is a pdf
		if(messageContent instanceof BASE64DecoderStream){
			DataHandler dh = message.getDataHandler();
			String crtFileName = message.getFileName();
			if(dh.getContentType().toLowerCase().contains("application/pdf")){
				String fullpath = path + File.separator
									   + sdf.format(new Date(System.currentTimeMillis()))
			                           + "_"
			                           + folder.getUID(message)
			                           + "_"
			                           + crtFileName;
				dh.writeTo(new FileOutputStream(new File(fullpath)));
				System.out.println("wrote attachement file to disk OK.");
				messageList.add(fullpath);		
			}else{
				System.out.println("Message does not have attachments.");
				System.out.println("Read attachments STOP.");
				return new String[0];
			}
		} else {
			// check wheter the message is a MIME multipart, and select the pdfs
			Multipart mm;
			try{
				mm = (Multipart)message.getContent();
			} catch (ClassCastException e){
				System.out.println("Message does not have attachments.");
				System.out.println("Read attachments STOP.");
				return new String[0];
			}
			
			// check all MIME parts to scan for attachments
			for (int j=0; j<mm.getCount(); j++ ){
				MimeBodyPart mbp = (MimeBodyPart)mm.getBodyPart(j);
				String crtFileName = mbp.getFileName();
				if (crtFileName != null){
					if(crtFileName.matches("(?i)^.*\\.pdf$") || crtFileName.matches("(?i)^.*\\.txto$") ){
						System.out.println("writing attachement file to disk ...");
						DataHandler dh = mbp.getDataHandler();
						String fullpath = path + File.separator
								    	+ sdf.format(new Date(System.currentTimeMillis()))
									    + "_"
									    + folder.getUID(message)
									    + "_"
									    + crtFileName;
						dh.writeTo(new FileOutputStream(new File(fullpath)));
						System.out.println("wrote attachement file to disk OK.");
						messageList.add(fullpath);
					}else{
						badAttachementNames.add(crtFileName);
					}
				}
			}
		}
		
		System.out.println("Read attachments STOP.");
		String [] retVal = new String[messageList.size()];
		for(int i=0; i<messageList.size(); i++){
			retVal[i] = messageList.get(i);
		}
		return retVal;
		
	}
	
	/**
	 * 
	 * @param msg
	 * @return
	 */
	static String getStringFrom(Message msg){
		String from = "";
		try{
		  from = ((InternetAddress)msg.getFrom()[0]).getAddress();
		  String name = ((InternetAddress)msg.getFrom()[0]).getPersonal();
		  if(name != null){
			  from += (" (" + name + ")");
		  }
		}catch(Exception e){
			e.printStackTrace();
			MailOrderDaemon.getLogger().error(StringUtils.exception2String(e));
		}
		return from;
	}
	
	/**
	 * 
	 * @param msg
	 * @return
	 */
	static String describeMessage(Message msg){
		String info = "";
		try{
			info += ("Order message received from: " + getStringFrom(msg) + "\n");
			info += ("Order message subject: " + msg.getSubject() + "\n");
			info += ("Order message sent on: " + msg.getSentDate());
		}catch(Exception e){
			e.printStackTrace();
			MailOrderDaemon.getLogger().error(StringUtils.exception2String(e));
		}
		return info;
	}
	
	/**
	 * 
	 * @param list
	 * @return
	 */
	private static String printStringList(List<String> list){
		String retVal = "";
		if(list != null){
			if(list.size()>=1){
				retVal = list.get(0);
			}
			for(int i=1; i<list.size(); i++){
				retVal += (", " + list.get(i));
			}
		}
		return retVal;
	}
	
	
	/**
	 * The order was not placed. Send exception to support
	 * @param e
	 * @param crtFileName
	 * @param message
	 */
	private void processGenericOrderPlacementException(Message crtMessage, String crtFileName, Exception e, String orderFileName, String fileNo){
		e.printStackTrace();
		if(fileNo == null){
			fileNo = "";
		}
		try{
			String crtRealFileName = crtFileName.substring(attachmentPath.length()+1).replaceFirst("[0-9]+_[0-9]+_","");
			String errorMessage = "Attachement processing failed.\nFile: " + crtRealFileName + ".\nException: " + StringUtils.exception2String(e);
			String subject = "ERROR: order " + fileNo + " PLACEMENT FAILURE";									
			MailOrderDaemon.getLogger().error(errorMessage);
			sendMailToSupport(crtMessage, errorMessage, subject, crtFileName, orderFileName, fileNo);
		}catch(MessagingException me2){
			me2.printStackTrace();
			MailOrderDaemon.getLogger().error(StringUtils.exception2String(me2));
		}
	}
	
	/**
	 * @return 
	 * 
	 *
	 */
	public Session getSession(){
	    
		Properties props = System.getProperties();
		props.setProperty("mail.smtp.host", getSmtpHost());
		Session session = Session.getInstance(props, null);
		return session;
	}

	/**
	 * 
	 * @param origMsg
	 * @param info
	 * @param subject
	 * @throws MessagingException
	 */
	public void sendMailToSupport(Message origMsg, String info, String subject) throws MessagingException{

		Address from = new InternetAddress(searchOrdersAddress);
		Address [] to = parseAddresses(supportAddress);
		Address [] cc = null;
		String newSubject  = "EMAIL_ORDER: " + subject;
		if(origMsg != null){
			newSubject += ("(Subject="+origMsg.getSubject()+")");
		}
        if(info.endsWith("\n")){
        	info = info.substring(0,info.length()-1);
        }
        String body = info + "\n" + ((origMsg!=null)?describeMessage(origMsg):"");
        body = body +"\nProduct=" + productTypeString;
        
        sendMessage(from, to, cc, newSubject, body, null, null);
	}
	
	/**
	 * 
	 * @param origMsg
	 * @param info
	 * @param subject
	 * @param fileName
	 * @param orderFileName
	 * @param fileNo
	 * @throws MessagingException
	 */
	public void sendMailToSupport(
			Message origMsg, 
			String info, 
			String subject, 
			String fileName, 
			String orderFileName,
			String fileNo
	) throws MessagingException{
		sendMailToSupport(origMsg, info, subject, fileName, orderFileName, fileNo, supportAddress);
	}
	
	/**
	 * 
	 * @param origMsg
	 * @param info
	 * @param subject
	 * @param fileName
	 * @param orderFileName
	 * @param fileNo
	 * @param address
	 * @throws MessagingException
	 */
	public void sendMailToSupport(
			Message origMsg, 
			String info, 
			String subject, 
			String fileName, 
			String orderFileName,
			String fileNo,
			String address
	) throws MessagingException
	{
		String origFileName = fileName.substring(attachmentPath.length()+1).replaceFirst("[0-9]+_[0-9]+_","");
		
        Address from = new InternetAddress(searchOrdersAddress);
        Address [] to = parseAddresses(address);
        Address [] cc = null;
        
        if(info.endsWith("\n")){
        	info = info.substring(0,info.length()-1);
        }
        info = info + "\n" + ((origMsg!=null)?describeMessage(origMsg):"");
        if(fileNo != null){
        	info = info + "\nFileID=" + fileNo;
        }
        info = info +"\nProduct=" + productTypeString;
       
        subject = "EMAIL_ORDER: " + subject + ((origMsg!=null)?" (Subject =" + origMsg.getSubject() + ")":"") /*+ ((fileNo!=null)?" (FileID =" + fileNo + ")":""*/;

		// prepare attachment file names
		String [] diskFileNames = new String [] {fileName, orderFileName};
		String [] originalFileNames = new String [] {origFileName, "orderFile.html"};
		
		// send the message
		sendMessage(from, to, cc, subject, info, diskFileNames, originalFileNames);
	}
	
	/**
	 * Sends an e-mail message
	 * @param from
	 * @param to
	 * @param cc
	 * @param subject
	 * @param body
	 * @param crtFileName
	 * @param origFileName
	 */
	private void sendMessage(
			Address from, 
			Address [] to, 
			Address [] cc, 
			String subject, 
			String body, 
			String [] diskFileNames, 
			String [] originalFileNames)
	{
		try{
			
			// create message
			Message newMessage = new MimeMessage(getSession());
			
			// set from, to, cc
			if(cc == null){ cc = new InternetAddress[0];}
			
	        newMessage.setFrom(from);
	        for(Address addr : to){
	        	if(addr instanceof InternetAddress && ((InternetAddress)addr).getAddress().equals(searchOrdersAddress)) {
		        	continue;
	        	}
	        	newMessage.addRecipient(javax.mail.Message.RecipientType.TO, addr);
	        }
	        for(Address addr : cc){
	        	if(addr instanceof InternetAddress && ((InternetAddress)addr).getAddress().equals(searchOrdersAddress)) {
	        		continue;
	        	}
	        	newMessage.addRecipient(javax.mail.Message.RecipientType.CC, addr);
	        }

	        // set subject
	        newMessage.setSubject(subject);
	        
	        // create message body
	        if(diskFileNames == null){
	        	newMessage.setText(body);
	        }else{
	        	// create multipart
		        Multipart multipart = new MimeMultipart();
		        
		        // add body
		        BodyPart messageBodyPart = new MimeBodyPart();
		        messageBodyPart.setText(body);
		        multipart.addBodyPart(messageBodyPart);
		        
		        // add attachments
		        for(int i=0; i<diskFileNames.length; i++){
		        	if(diskFileNames[i] != null){
			        	BodyPart attachementPart = new MimeBodyPart();
			        	attachementPart.setDataHandler(new DataHandler(new FileDataSource(diskFileNames[i])));
			        	attachementPart.setFileName(originalFileNames[i]);
			        	multipart.addBodyPart(attachementPart);
		        	}
		        }
		        newMessage.setContent(multipart);
	        }
	        newMessage.saveChanges();
	        
	        // send new message
	        Transport.send(newMessage);

	        MailOrderDaemon.getLogger().debug("MailOrderDeamonTimerTask.sendMessage sent!");
	        
		}catch(MessagingException me){
			MailOrderDaemon.getLogger().error("MailOrderDeamonTimerTask.sendMessage", me);			
		}
	}
	
	/**
	 * constructs an array of Address (actually InterrnetAddress) from a string of addresses separated by comma
	 * @param addresses string of addresses separated by comma
	 * @return array of created InternetAddress
	 */
	static Address [] parseAddresses(String addresses) throws AddressException {
		String [] addressArray =  addresses.split(",");
		Address [] retVal = new Address[addressArray.length];
		int pos = 0;
		for(String address: addressArray){
			retVal[pos++] = new InternetAddress(address);
		}
		return retVal;
	}
	
	private String getSmtpHost() {
		String smtpHost = MailConfig.getMailOrderSmtpHost();
		if(org.apache.commons.lang.StringUtils.isBlank(smtpHost)) {
			smtpHost = MailConfig.getMailSmtpHost();
		}
		return smtpHost;
	}
}



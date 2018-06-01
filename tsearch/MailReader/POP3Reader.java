package ro.cst.tsearch.MailReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeBodyPart;

import com.sun.mail.pop3.POP3Folder;

public class POP3Reader {
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
	
	private String host = null;
	private int port = -1;
	private String username = null;
	private String password = null;
	private String lastUID = null;
	private boolean leaveMessageOnServer = true;

	private POP3Folder folder = null;

	/**
	 * constructor
     */
	public POP3Reader(String host, String username,String password, String lastUID){
		this.host = host;
		this.username = username;
		this.password = password;
		this.lastUID = lastUID;
	}

	/**
	 * constructor
     */
	public POP3Reader(String host, String username,String password, String lastUID,
						boolean leaveMessageOnServer){
		this(host,username,password,lastUID);
		this.leaveMessageOnServer = leaveMessageOnServer;
	} 
	
	/**
	 * constructor 
	 */
	public POP3Reader(String host, int port, String username,String password, String lastUID,
					  boolean leaveMessageOnServer){
		this(host,username,password,lastUID,leaveMessageOnServer);
		this.port = port;
	}
		

	/**
	 * this method connects to the pop3 server and initialises folder
 	 */
	public void connect() throws MessagingException{
		folder = null;
		try{
			Session ses = Session.getInstance(System.getProperties(),null);
			URLName urlName = new URLName("pop3", host, port, "INBOX", username, password);
			folder = (POP3Folder)ses.getFolder(urlName);
			folder.open(Folder.READ_WRITE);
		} catch ( MessagingException me){
			throw me;
		}
	}

	/**
	 * this method disconnects from the server
     * it removes any messages that are marked to be deleted
	 */
	public void disconnect() throws MessagingException{
		if (folder == null)
			return; 
		try{
			folder.close(true);
		} catch(MessagingException me){
			throw me;
		}	
	}

	/**
	 * get all the messages from server
	 */
	public Message[] getAllMessages() throws MessagingException {
		if (folder == null) throw new MessagingException("Not connected to POP3 server !!!");
		return folder.getMessages();
	}

	/**
	 * get the new messages from server
	 */
	public Message[] getNewMessages() throws MessagingException{
		int startMessageIdx = 0;
		Message[] vectRetMessage;
 
		try{
			Message[] vectMessage = getAllMessages();
			if (lastUID != null){
				for (int i=0; i<vectMessage.length; i++){
					if ( folder.getUID(vectMessage[i]).equals(lastUID) ){
						startMessageIdx = i+1; // this is the index of the first new message
						break;
					}
				}
				if (startMessageIdx > 0){
					vectRetMessage = new Message[vectMessage.length - startMessageIdx];
					//System.arraycopy(vectMessage,startMessageIdx,vectRetMessage,0,vectRetMessage.length);
					for (int i=0; i<vectRetMessage.length; i++){
						vectRetMessage[i] = vectMessage[startMessageIdx+i];
					}
				} else {
					vectRetMessage = vectMessage;
				}				
			}
			else{
				vectRetMessage = vectMessage;
			}

			if (!leaveMessageOnServer){
				// the lastUID stays the same in this case
				// but we mark the returned messages as deleted
				for (int i=startMessageIdx; i<vectMessage.length; i++)	
					vectMessage[i].setFlag( Flag.DELETED,true );
			} 
			else{
				// we update the last uid
				if (vectRetMessage.length > 0)
					lastUID = folder.getUID( vectRetMessage[vectRetMessage.length -1] );
			}

			return vectRetMessage;
		}
		catch(MessagingException me){
			throw me; 
		} 
		
	}

	/**
	 * returns a String array containing all the subjects
	 * make sure you do not call this method immediatly after calling getNewMessages()
	 * use this method if you are only interested in the message subject !!!  
	 */
	public String[] getSubjectsForNewMessages() throws MessagingException {
		Message[] vectMessage = getNewMessages();
		String[] vectRetSubject = new String[vectMessage.length];
		for(int i=0; i<vectRetSubject.length; i++){
			vectRetSubject[i] = vectMessage[i].getSubject();
		}			
		return vectRetSubject;
	}
	
	
	/**
	 * write the attachments of arrayMsg to specified path
	 * @param folder  
	 * @param arrayMsg
	 * @param path
	 * @return string array of filenames (path + name) written to disk
	 * @throws MessagingException
	 * @throws IOException
	 */
	public static String[] writeAttachmentsToDisk(POP3Folder folder, Message[] arrayMsg, String path) throws MessagingException, IOException{
		System.out.println("writeAttachmentsToDisk Start");
		System.out.println("writeAttachmentsToDisk " + arrayMsg.length + " messages");
		
		if (path.lastIndexOf( File.separator) != path.length() - 1)
			path += File.separator;
		
		String fullpath;
		Vector vect = new Vector();
		for (int i=0; i<arrayMsg.length; i++){
			String uid = folder.getUID(arrayMsg[i]);
			System.out.println("writeAttachmentsToDisk arrayMsg[" + i + "] UID:" + uid);
			System.out.println("writeAttachmentsToDisk arrayMsg[" + i + "] From:" + getAddressCsv(arrayMsg[i].getFrom()));
			System.out.println("writeAttachmentsToDisk arrayMsg[" + i + "] Subject:" + arrayMsg[i].getSubject());
			Multipart mm;
			try{
				mm = (Multipart)arrayMsg[i].getContent();
			} catch (ClassCastException e){
				System.out.println("writeAttachmentsToDisk arrayMsg[" + i + "] NOT a multipart message.");
				continue;
			}
			System.out.println("writeAttachmentsToDisk arrayMsg[" + i + "] Body parts count:" + mm.getCount());
			for (int j=0; j<mm.getCount(); j++ ){
				MimeBodyPart mbp = (MimeBodyPart)mm.getBodyPart(j);
				System.out.println("writeAttachmentsToDisk arrayMsg[" + i + "] Body part [" + j + "] file name: " + mbp.getFileName());
				if (mbp.getFileName() != null){
					System.out.println("writeAttachmentsToDisk writing file to disk ...");
					DataHandler dh = mbp.getDataHandler();
					fullpath = path
								+ sdf.format(new Date(System.currentTimeMillis()))
								+ "_"
								+ uid
								+ "_"
								+ mbp.getFileName();
					dh.writeTo(new FileOutputStream(new File(fullpath)));
					vect.add(fullpath);
					System.out.println("writeAttachmentsToDisk OK.");
				}
			}
		}
		System.out.println("writeAttachmentsToDisk Stop");
		return (String[])vect.toArray(new String[vect.size()]);
		
	}
	
	
	/**
	 * @param arrayAdr
	 * @return arrayAdr converted to CSV string
	 */
	public static String getAddressCsv(Address[] arrayAdr){
		if (arrayAdr == null)
			return "";
		if (arrayAdr.length == 0)
			return "";
		String retStr = arrayAdr[0].toString();
		for (int i=1; i<arrayAdr.length; i++){
			retStr += "," + arrayAdr[i].toString(); 
		}
		return retStr;
	}
	
	
	

	/**
	 * @return Returns the lastUID.
	 */
	public String getLastUID() {
		return lastUID;
	}

	/**
	 * @param lastUID The lastUID to set.
	 */
	public void setLastUID(String lastUID) {
		this.lastUID = lastUID;
	}

	/**
	 * @return Returns the folder.
	 */
	public POP3Folder getFolder() {
		return folder;
	}

	/**
	 * @param folder The folder to set.
	 */
	public void setFolder(POP3Folder folder) {
		this.folder = folder;
	}
	
	public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
    
        // Get the size of the file
        long length = file.length();
    
        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }
    
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];
    
        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }
    
        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }
    
        // Close the input stream and return bytes
        is.close();
        return bytes;
    }
	
	public static void main(String[] args){
		
		 if (args.length != 4){
			System.out.println("Usage: POP3Reader [mail server] [username] [password] [path for saving files]");
			return;
		}
		POP3Reader reader = new POP3Reader(args[0], args[1], args[2], 
											null, false/*leave messages on server */);
		
		try{
			reader.connect();
			Message[] messages = reader.getNewMessages();
			String[] filenames = POP3Reader.writeAttachmentsToDisk(reader.getFolder(), messages, args[3]);
		
			System.out.println("Files: ");
			for(int i=0; i< filenames.length; i++){
				System.out.println(filenames[i]);
			}
			
			reader.disconnect();
		} catch (Exception e){
			System.err.println(e.getMessage() );
			e.printStackTrace(); 
		}
	}	

}

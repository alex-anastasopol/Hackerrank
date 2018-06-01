package ro.cst.tsearch.MailReader;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.search.SearchTerm;

import com.sun.mail.imap.IMAPFolder;

/**
 * 
 * @author radu bacrau
 *
 */
public class IMAPReader {	

	/**
	 * get all the new messages from server
	 * @param folder
	 * @return
	 * @throws MessagingException
	 */
	public static Message[] getNewMessages(IMAPFolder folder) throws MessagingException{
		return getNewMessages(folder, -1);
	}

	/**
	 * Filters out the deleted messages
	 * @author radu bacrau
	 */
	private static class MySearchTerm extends SearchTerm{
		static final long serialVersionUID = 10000;
		public boolean match(Message msg){
			try{
				if(msg.isSet(Flag.SEEN)){
					return false;
				}
				if(msg.isSet(Flag.DELETED)){
					return false;
				}
				return true;
			}catch(Exception e){
				e.printStackTrace();
				return false;
			}
		}
	}
	
	/**
	 * get last maxMessages messages fromn server, filtered by MySearchTerm
	 * if maxMessages <0 then return all new messages
	 * @param folder
	 * @param maxMessages
	 * @return
	 * @throws MessagingException
	 */	
	public static Message[] getNewMessages(IMAPFolder folder, int maxMessages) throws MessagingException{
		Message [] messages = folder.search(new IMAPReader.MySearchTerm());
		if(messages.length > maxMessages){
			Message [] shortList = new Message[maxMessages];
			for(int i=0; i<maxMessages; i++){
				shortList[i] = messages[i];
				//shortList[i].setFlag(Flag.SEEN, false);
			}
			messages = shortList;
		}
		return messages;
	}

}

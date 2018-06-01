package ro.cst.tsearch.MailReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.TimerTask;
import java.util.Vector;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;

import ro.cst.tsearch.database.DBManager;

public class POP3DaemonTimerTask extends TimerTask {
	
	private POP3Reader reader = null;

	/**
	 * constructor
	 */
	public POP3DaemonTimerTask(String host, String username,String password,boolean leaveCopyOnServer){
		reader = new POP3Reader(host, username, password, null,leaveCopyOnServer);
	}
	
	/**
	 * extract file name from the headers attached to the mozilla like receipt
	 */
	private static String parseMozillaLikeReturnReceipt(Message m){
		//we need to retrive the parts from message
		//and parse the ones Content-Type: text/rfc822-headers;
		try {
			if (m.isMimeType("multipart/report")){
				try {

					Multipart mp = (Multipart)m.getContent();
					for (int i = 0; i< mp.getCount(); i++){
						BodyPart bp = mp.getBodyPart(i);
						if (bp.getContentType().trim().indexOf("text/rfc822-headers;") > -1) {
							MimeBodyPart mbp = null;
							try {
								//if there are problems, check if getContent returns something not castable to InputStream
								mbp = new MimeBodyPart((InputStream)bp.getContent());
								String[] subjHeaders = mbp.getHeader("Subject");
								for (i=0; i<=subjHeaders.length; i++){
									//I assume there can't be more then one TSR file name in one email
									String TSRName = extractFileName(subjHeaders[i]) ; 
									if (TSRName != null  ){
										return TSRName;
									}
								}
							} catch (Exception e){
								return null;
							}
							//get the headers
							
						}
					}
				} catch (IOException e){
					return null;
				}
			}
		} catch (MessagingException e){
			return null;
		}
		//we have another case of return receipt, we need a patch in here
		return null;
	}
	
	/**
	 * simple file name extraction function 
	 * @param str
	 * @return String
	 */
	private static String extractFileName(String str){
		try {
			int pos1 = str.indexOf("_");
			if (pos1<0){
				return null;
			}
			String prefixAndName = str.substring(0, pos1 + 1);
			int posMinus = prefixAndName.lastIndexOf("-");
			String prefix = str.substring(0, posMinus);
			String name = prefixAndName.substring(posMinus);
			if(prefix.length() > 3)
				prefix = prefix.substring(prefix.length()-3);
			
			int pos2 = str.indexOf(" ",pos1);
			if (pos2>0){
				return prefix + name + str.substring(pos1+1,pos2);
			} else {
				return prefix + name + str.substring(pos1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}	
		
	/**
	 * this method is called whenever the task needs to run
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		POP3Daemon.getLogger().debug("POP3DaemonTask#run - start");

		Vector<String> vectFileName = new Vector<String>();
		String fileName = null;
		try{
			reader.connect();

			Message[] vectMessage = reader.getNewMessages();
			if (vectMessage.length != 0){
				POP3Daemon.getLogger().debug(vectMessage.length + " new messages found");
				for(int i=0; i<vectMessage.length; i++){
					String msgSubject = vectMessage[i].getSubject();
					fileName = extractFileName(msgSubject);
					if (fileName == null){
						fileName = parseMozillaLikeReturnReceipt(vectMessage[i]);
					} 
					POP3Daemon.getLogger().debug(i + ": " + msgSubject + " / " + fileName);
					if (fileName != null){
						vectFileName.add(fileName);
					}
				}
				String[] arrayFileName = (String[])vectFileName.toArray(new String[vectFileName.size()]);
				if (arrayFileName.length>0){
					if (DBManager.updateSearchesConfirmation(arrayFileName) != 0){
						POP3Daemon.getLogger().debug("Database error ! Error updating confirmation field !");
					}
				}
			} else {
				POP3Daemon.getLogger().debug("No new messages found");
			}
			
			reader.disconnect();
		}
		catch(MessagingException e){
			POP3Daemon.getLogger().debug(e.getMessage());
		}
		catch( Exception e2 ){
			e2.printStackTrace();
		}
		
		POP3Daemon.getLogger().debug("POP3DaemonTask#run - stop");
	}
	
	
	public static void main(String[] args) {
		//thunderbird response
		String s = "Return Receipt (displayed) - FUL-a1_07182008_073632.pdf  andrei  a (and)";
		System.err.println(extractFileName(s));
		//outlook express response
		s = "FUL-AFWILG~571161_07182008_071020.pdf  Joy  Andreassen Agent AFW (Ste) - policies only";
		System.err.println(extractFileName(s));
	}
	

}

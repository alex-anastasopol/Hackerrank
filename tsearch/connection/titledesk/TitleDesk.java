package ro.cst.tsearch.connection.titledesk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.log4j.Logger;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.titledesk.webservice.*;
import com.stewart.titledesk.webservice.ATSReceiveStub.ReceiveATSFileResponse;

/**
 * @author radu bacrau
 * @author cristian stochina - upgrade connection from axis 1 to axis 2
  */
public class TitleDesk {

	private static Logger logger = Logger.getLogger(TitleDesk.class);
	
	private static final String TITLEDESK_ADDRESS_KEY  = "titledesk.address";
	private static final String TITLEDESK_USER_KEY     = "titledesk.user";
	private static final String TITLEDESK_PASSWORD_KEY = "titledesk.password";
	private static final String TITLEDESK_APPID_KEY    = "titledesk.appid";
	private static final String TITLEDESK_TIMEOUT_KEY  = "titledesk.timeout";
	private static final String TITLEDESK_RETRIES_KEY  = "titledesk.retries";
	private static final String TITLEDESK_WAIT_KEY     = "titledesk.wait";
	
	private static final String TITLEDESK_ADDRESS_VAL  = "https://www.etitledesk.com/Longbow/webservices/atsReceive.asmx";
	private static final String TITLEDESK_USER_VAL     = "User";
	private static final String TITLEDESK_PASSWORD_VAL = "Password";
	private static final String TITLEDESK_APPID_VAL     = "ATS";
	private static final int TITLEDESK_TIMEOUT_VAL = 300;
	private static final int TITLEDESK_RETRIES_VAL = 5;
	private static final int TITLEDESK_WAIT_VAL    = 30;

	
	/**
	 * Send files to TitleDesk server
	 * @param orderId
	 * @param files
	 * @return message to be displayed in notification email
	 */
	public static String sendFiles(String orderId, Set<String> fileNames, boolean isUpdate){
		String retMsg = "<b><u>Upload to TitleDesk Status:</u></b><br/>";
		try {
			// add all image files to upload list
			List <File> uploadFiles = new ArrayList<File>();
			for(String fileName : fileNames){
				String fileNameUcase = fileName.toUpperCase();
				logger.debug("OrderId: " + orderId + ". Checking fileName: " + fileNameUcase);
				if(	fileNameUcase.endsWith(".DOCX") || 
						fileNameUcase.endsWith(".DOC") ||
						fileNameUcase.endsWith(".PDF") || 
						fileNameUcase.endsWith(".ATS") || 
						fileNameUcase.endsWith(".LOG.HTML")) {
					File file = new File(fileName);
					if(file.exists()){
						logger.debug("OrderId: " + orderId + ". sending to upload: " + fileNameUcase);
						uploadFiles.add(file);
					}
				}
			}
			retMsg += sendFilesInternal(orderId, uploadFiles, isUpdate);
		}catch(RuntimeException e){
			logger.error(e);
			retMsg += "<font color='red'>Error appeared during upload!</font>";					
		}
		logger.debug("OrderId: " + orderId + ". Message: " + retMsg);
		return retMsg;
	}
	/**
	 * Reads a text file
	 * @param fileName
	 * @return file contents, null if error occured
	 * @throws RuntimeException if anything went wrong
	 */	
	private static String readTextFile(String fileName) throws RuntimeException {
		
		// open input file
		BufferedReader br;
		try{
			br = new BufferedReader(new FileReader(fileName));
		}catch(FileNotFoundException e){
			logger.error("Input File Not Found :" + fileName, e);
			throw new RuntimeException(e);
		}
		
		// read  the file
		StringBuilder sb = new StringBuilder();
		String line;
		boolean prevEmpty = false;
		try{
			while((line = br.readLine()) != null){
				line = line.trim();
				if(line.length() == 0){
					// empty line
					if(!prevEmpty){
						sb.append("\r\n");
						prevEmpty = true;
					}
				} else {
					// not empty line
					sb.append(line);
					sb.append("\r\n");
					prevEmpty = false;
				}
			}
		}catch(IOException e){
			logger.error("IOException", e);
			throw new RuntimeException(e);
		}
		finally{
			try { 
				br.close(); 
			} catch (IOException e) {
				logger.error("Reader Close Error", e);
			}
		}	
		
		// return contents
		return sb.toString();
	}
	
	/**
	 * Read a file to be sent to TitleDesk server
	 * @param fileName
	 * @return
	 */
	private static byte [] readFile(String fileName){
		if(fileName.toUpperCase().endsWith(".ATS")){
			return prepareAtsFile( readTextFile( fileName ) ).getBytes() ;
		} else {
			return FileUtils.readBinaryFile( fileName ) ;
		}		
	}
	
	/**
	 * Prepare an ATS  file to be sent to TitleDesk server
	 * @param fileName
	 * @return
	 */
	private static String prepareAtsFile(String contents){
		contents = contents.replaceAll("&lt;(/?(?:ExceptionsHardBreak|Parcel|ParcelNumber|RequirementsHardBreak))&gt;","<$1>");			
		contents = contents.replaceAll("(?s)>[ \t\n\r]+<","><");
		contents = contents.replaceAll("(?s)[ \t\n\r]+<","<");
		contents = contents.replaceAll("(?s)>[ \t\n\r]+",">");
		contents = contents.replaceAll("\\[&quot;\\]","\"");
		contents = contents.replaceAll("\\[&gt;\\]",">");
		contents = contents.replaceAll("\\[&lt;\\]","<");
		contents = contents.replaceAll("\\[\"\\]","\"");
		contents = contents.replaceAll("\\[>\\]",">");
		contents = contents.replaceAll("\\[<\\]","<");
        contents = contents.replace('\u201C', '"');
        contents = contents.replace('\u201D', '"');
        contents = contents.replace('\u2019', '\'');
        contents = contents.replace('\u2018', '\'');
        //clean empty parties from response
        contents = contents.replaceAll("(?s)(?i)<Party>[^<]*<PartyFirstName>[^<]*</PartyFirstName>[^<]*<PartyMiddleName>[^<]*</PartyMiddleName>[^<]*<PartyLastName>\\s*</PartyLastName>.+?</Party>", "");
        return contents;
	}
	
	
	/**
	 * Send files to TitleDesk server
	 * @param orderId
	 * @param files
	 * @return message to be displayed in notification email
	 */
	private static String sendFilesInternal(String orderId, List<File> files, boolean isUpdate){
		
		if(files.size() == 0){
			return "WARNING: Nothing to upload.";
		}
		
		// read parameters from configuration file, using defaults
		ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
		String address  = readStringParam(rbc, TITLEDESK_ADDRESS_KEY,  TITLEDESK_ADDRESS_VAL);
		String user     = readStringParam(rbc, TITLEDESK_USER_KEY,     TITLEDESK_USER_VAL);
		String password = readStringParam(rbc, TITLEDESK_PASSWORD_KEY, TITLEDESK_PASSWORD_VAL);
		String appId    = readStringParam(rbc, TITLEDESK_APPID_KEY,    TITLEDESK_APPID_VAL);
		int timeout     = readIntParam(rbc, TITLEDESK_TIMEOUT_KEY, TITLEDESK_TIMEOUT_VAL) * 1000;
		int retries     = readIntParam(rbc, TITLEDESK_RETRIES_KEY, TITLEDESK_RETRIES_VAL);
		int wait        = readIntParam(rbc, TITLEDESK_WAIT_KEY,    TITLEDESK_WAIT_VAL);
		
		ATSReceiveStub  stub = null;
		try {
			stub = new ATSReceiveStub(address);
			stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(100 * 1000);
		}
		catch (Exception e) {
			String msg = "Cannot create service to connect to TitleDesk";
			logger.error(msg, e);
			return "<font color='red'>Error:" + msg + "</font>";
		}
		ATSReceiveStub.ReceiveATSFile method = new ATSReceiveStub.ReceiveATSFile();
		stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(timeout);
		method.setAppID(appId);
		method.setUsername(user);
		method.setPassword(password);
		method.setOrderID(orderId);

		// upload the files
		List<String> succeeded = new ArrayList<String>();
		List<String> failed = new ArrayList<String>();
		for(File file: files){
			String fileName = file.getName();
			byte [] fileContents = readFile(file.getAbsolutePath());
			try {
				if(fileName.toLowerCase().endsWith(".ats")) {
					FileOutputStream fos = new FileOutputStream(new File(file.getAbsolutePath()));
					fos.write(fileContents);
					fos.close();
				}
			}catch(Exception e) {
				e.printStackTrace();
				logger.error("Error while writing byts for " + fileName, e);
			}
			if(isUpdate && fileName.endsWith(".ats")){
				fileName = fileName.replace(".ats", ".tsu");
			}
			boolean uploaded = false;
			for(int i=0; i<retries; i++){
				try{
					method.setFile(new DataHandler(new FileDataSource(new File(file.getAbsolutePath()))));
					method.setFilename(fileName);
					
					ReceiveATSFileResponse response = stub.receiveATSFile(method);
					String message = StringUtils.extractParameter(response.getReceiveATSFileResult(), "<order_status>([^<]+)</order_status>");
					
					// String message = "successfully transferred";
					if(message.toLowerCase().contains("successfully transferred") ||
					   message.toLowerCase().contains("successfully imported")){
						uploaded = true;
						break;
					} else {
						logger.error("OrderID: " + orderId + ". Upload failed with message: " + message);
						throw new RuntimeException("Upload failed with message: " + message);
					}
				}catch(Exception e){
					String msg = "Retry " + (i+1) + " of " + retries + ": failed to upload file " + file.getAbsolutePath() + "!"; 
					logger.warn("OrderID: " + orderId + ". " + msg, e);
				}
				try{
					TimeUnit.SECONDS.sleep(wait);
				}catch(InterruptedException ie){}
			}
			if(uploaded){
				succeeded.add(fileName);
				logger.info("OrderID: " + orderId + ". Succeeded to upload file: " + file.getAbsolutePath());
			}else {
				logger.error("OrderID: " + orderId + ". Failed to upload file: " + file.getAbsolutePath());
				failed.add(fileName);
			}
		}
		
		// construct the error message
		if(failed.size() == 0){
			return StringUtils.printHtmlList("All files were uploaded succesfully", succeeded);			
		} else {
			if(succeeded.size() == 0){
				return "<font color='red'>" + StringUtils.printHtmlList("All files failed to be uploaded", failed) + "</font>";
			}else{
				return StringUtils.printHtmlList("The following files were succesfully uploaded", succeeded) + 
					   "<br/><font color='red'>" + 
					   StringUtils.printHtmlList("The following files failed to be uploaded", failed) + 
					   "</font>";
			}
		}		
	}
	
	/**
	 * Read an integer from a bundle
	 * @param rbc
	 * @param key
	 * @param defaultVal
	 * @return value from bundle or defaultVal if key not found or found value not number
	 */
	private static int readIntParam(ResourceBundle rbc, String key, int defaultVal){
		try{				
			String string = rbc.getString(key);
			if(!string.matches("\\d+")){
				logger.warn("Configuration parameter: " + key + " not number: " + string + " !");
				return defaultVal;
			}
			return Integer.parseInt(string);
		}catch(Exception e){
			logger.warn("Configuration parameter: " + key + " is missing!");
			return defaultVal;
		}
	}
	
	/**
	 * Read a string from a bundle
	 * @param rbc
	 * @param key
	 * @param defaultVal
	 * @return value from bundle or defaultVal if key not found
	 */
	private static String readStringParam(ResourceBundle rbc, String key, String defaultVal){
		try{
			return rbc.getString(key);	
		}catch(Exception e){
			logger.warn("Configuration parameter: " + key + " is missing!");
			return defaultVal;			
		}
	}
	
	
	public static void main(String [] args){
		String text = readTextFile("c://input.ats");
		text = text.replaceAll("<!--", "").replaceAll("-->","");
		FileUtils.writeTextFile("c://input.ats", text);
		text = prepareAtsFile(text);
		FileUtils.writeTextFile("c:/output1.ats", text);
	}
}

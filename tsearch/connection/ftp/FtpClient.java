package ro.cst.tsearch.connection.ftp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.log4j.Logger;

public class FtpClient {

	private static final Logger logger = Logger.getLogger(FtpClient.class);

	private FTPClient ftp;

	private String server;

	private String username;

	private String password;

	private String workingDirectory;
	

	/**
	 * Construct the FtpClient instance
	 * Use binary as default transfer mode
	 */
	public FtpClient() {
		ftp = new FTPClient();
	}

	/**
	 * Connect FTP client to the server
	 * @param server
	 * @param username
	 * @param password
	 * @return
	 */
	public boolean connect(String server, String username, String password) {

		this.server = server;
		this.username = username;
		this.password = password;
		this.workingDirectory = null;
		return connectImpl();
	}

	private boolean connectImpl() {
		try {

			ftp.connect(server);
			ftp.login(username, password);

			logger.info("Connected to " + server + ".");
			logger.info(ftp.getReplyString());

			// check the reply code to verify success.
			int reply = ftp.getReplyCode();

			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				logger.error("FTP server refused connection.");
				return false;
			}

			try {
				ftp.setFileType(FTP.BINARY_FILE_TYPE);
			} catch (IOException e) {
				logger.error(e);
				return false;
			}

		} catch (IOException e) {
			logger.error("Cannot connect to server: " + server);
			return false;
		}

		return true;
	}

	/**
	 * Disconnect ftp client
	 *
	 */
	public void disconnect() {
		if (ftp.isConnected()) {
			try {
				ftp.disconnect();
			} catch (IOException ioe) {
				// do nothing
			}
		}
	}
	
	

	public static final class FTPFileStruct{
		final public File localFile;
		final public String remoteName;
		final public byte[] indexFileContent;
		
		public FTPFileStruct(File localFile, String remoteName, byte[] indexFileContent){
			this.localFile = localFile;
			this.remoteName = remoteName;
			this.indexFileContent = indexFileContent;
		}
		
	}
	
	/**
	 * Upload a list of files
	 * @param fileNames
	 * @param destFolder
	 * @return
	 */
	public String uploadFiles(List<FTPFileStruct> fileNames, String destFolder) {		
		
		boolean success = false;

		// try to create the folder and change to it
		for(int i=0; i<3 && !success; i++){
			
			// create destFolder
			try {
				ftp.changeWorkingDirectory("/");
				ftp.makeDirectory(destFolder);
			} catch (IOException e) {
				logger.warn("Cannot create: " + destFolder, e);
			}
			
			// change folder to destFolder 
			try {
				success = ftp.changeWorkingDirectory(destFolder);
			} catch (IOException e) {
				logger.error("Cannot change to " + destFolder, e);
			}			
		}
		
		if (!success) {
			logger.error("Cannot change to " + destFolder);
			return "<font color=\"red\">ERROR: upload failed! Cannot create remote folder: '" + destFolder + "'<font></br>";
		}

		// try to upload the files
		Set<String> yesFiles = new HashSet<String>();
		Set<String> noFiles = new HashSet<String>();
		for (FTPFileStruct fileStruct : fileNames) {
							
			String shortFileName = fileStruct.remoteName;
			success = false;
			
			for(int i=0; i<3 && !success; i++){
				InputStream is = null;
				try{
					if(fileStruct.localFile != null) {
						is = new FileInputStream(fileStruct.localFile);
					} else if(fileStruct.indexFileContent != null ) {
						is = new ByteArrayInputStream(fileStruct.indexFileContent);
					}
					if(is != null) {
						success = ftp.storeFile(shortFileName, is);
					}
				}catch(IOException e){
					logger.error(e);
				}finally{
					try{ is.close();} catch(Exception ignored){}
				}
			}
			
			if (success) {
				if(fileStruct.localFile != null) {
					logger.info("File " + fileStruct.localFile.getPath() + " uploaded correctly!");	
				} else if(fileStruct.indexFileContent != null ) {
					logger.info("Content for " + fileStruct.remoteName + " uploaded correctly!");
				}
				
				yesFiles.add(shortFileName);
			} else {
				if(fileStruct.localFile != null) {
					logger.error("File " + fileStruct.localFile.getPath() + " was NOT uploaded correctly!");
				} else if(fileStruct.indexFileContent != null ) {
					logger.info("Content for " + fileStruct.remoteName + " was NOT uploaded correctly!");
				}
				
				noFiles.add(shortFileName);
			}

		}
		
		if(noFiles.size() == 0){
			return 
				"All files were uploaded successfully: <br/>" + display(yesFiles);
		} else {
			return 
				"<br/>The following files were uploaded successfully: <br/>" + display(yesFiles) + "<br/>" + 
				"<font color=\"red\">The following files were <b>NOT</b>uploaded successfully:<br/>" + display(noFiles);
		}

	}
	
	public FTPClient getFtp() {
		return ftp;
	}
	
	/**
	 * Convenience 
	 * @param pathname
	 * @return
	 * @throws IOException
	 */
	public boolean changeWorkingDirectory(String pathname) throws IOException {
		boolean changeWorkingDirectory = ftp.changeWorkingDirectory(pathname);
		
		if(changeWorkingDirectory) {
			this.workingDirectory = pathname;
		}
		return changeWorkingDirectory;
	}

	/**
	 * Format a set of strings as a list
	 * @param files
	 * @return
	 */
	private static String display(Set<String> files){
		String retVal = "";
		for(String file: files){
			retVal += "<li>" + file + "</li>";
		}
		return retVal;
	}

	public static void main(String[] args) {

		String server = "ftpautoexam.propertyinfo.com";
		String username = "Portaltest";
		String password = "passw0rd1";
		String destFolder = "test-ats-9-17-2007";
		String fileNames[] = new String[] { "d:/TSR-FFF_09172007_143559.html",
				"d:/TSR-FFF_09172007_143559.pdf", "d:/out.xml",
				"d:/kits/HDRShop.exe" };

		/*
		FtpClient ftp = new FtpClient();
		try {
			ftp.connect(server, username, password);
			//ftp.uploadFiles(Arrays.asList(fileNames), destFolder);
		} finally {
			ftp.disconnect();
		}
		*/
		
		FTPSClient ftp = new FTPSClient(true);
		try {
			
			
			ftp.connect("ftp.bdfte.com", 990);
			
			
			
			boolean loggedIn = ftp.login("ATS", "A95@P7X$n");
			int replyCode = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(replyCode)) {
				System.out.println("wrong");
			}
			//System.out.println(ftp.getSystemType());
			ftp.execPBSZ(0);
			ftp.execPROT("P");
			
			//System.out.println("Connected");
			
			//System.out.println(ftp.printWorkingDirectory());
			
			ftp.setFileType(FTP.ASCII_FILE_TYPE);
			
			//System.out.println(ftp.enterRemotePassiveMode());
			
			//ftp.setFileType(FTP.BINARY_FILE_TYPE);
			
			
			//ftp.enterRemotePassiveMode();
			//ftp.enterLocalPassiveMode();
			/*
			System.out.println("good");
			FTPFile[] listDirectories = ftp.listDirectories();
			if(listDirectories != null) {
				System.out.println("ftp.listDirectories() has " + listDirectories.length);
				for (FTPFile ftpFile : listDirectories) {
					System.out.println(ftpFile.getName());	
				}
				
			} else {
				System.err.println("ftp.listDirectories() is null");
			}
			*/
			ftp.setListHiddenFiles(true);
			//ftp.enterRemotePassiveMode();
			
			ftp.enterLocalPassiveMode();
			
			
			FTPFile[] listFiles = ftp.listFiles();
			if(listFiles == null) {
				System.err.println("ftp.listFiles() is null");
			} else {
				System.out.println("ftp.listFiles() has " + listFiles.length);
				for (FTPFile ftpFile : listFiles) {
					System.out.println(ftpFile.getName());
				}
			}
			
			ftp.changeWorkingDirectory("OUT");
			ftp.enterLocalPassiveMode();
			listFiles = ftp.listFiles();
			if(listFiles == null) {
				System.err.println("ftp.listFiles() is null");
			} else {
				System.out.println("ftp.listFiles() has " + listFiles.length);
				for (FTPFile ftpFile : listFiles) {
					System.out.println(ftpFile.getName());
				}
			}
			
			
			/*
			ftp.enterRemotePassiveMode();
			ftp.enterLocalPassiveMode();
			
			String[] listNames = ftp.listNames();
			if(listNames == null) {
				System.err.println("ftp.listNames() is null");
			} else {
				System.out.println("ftp.listNames() has " + listNames.length);
				for (String name : listNames) {
					System.out.println(name);
				}
			}
			*/
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				ftp.disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
		
		System.err.println("");
	}

	public boolean reconnect() {
		try {
			disconnect();
			boolean connected = connectImpl();
			if(connected && this.workingDirectory != null) {
				ftp.changeWorkingDirectory(this.workingDirectory);
			}
			return connected;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
	}


}

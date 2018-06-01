package ro.cst.tsearch.connection.ftp;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamException;


public final class ATSConnFTP
{

    private FTPClient ftp;
    
    
    public ATSConnFTP (){
    	
    	ftp=new FTPClient();
    }

	/*public boolean downloadFile (String serverFile, String localFile) throws IOException, FTPConnectionClosedException {
		FileOutputStream out = new FileOutputStream(localFile);	
		
		boolean result = ftp.retrieveFile(serverFile, out);
		if (result==true) System.out.println(serverFile +" downloaded ");
		out.close();		
		return result;

}

	//Upload a file to the server 
	public boolean uploadFile (String localFile, String serverFile) throws IOException, FTPConnectionClosedException {
		FileInputStream in = new FileInputStream(localFile);
		boolean result = ftp.storeFile(serverFile, in);
		if (result==true) System.out.println(localFile +" uploaded ");
		in.close();
		return result;
	}*/
    public  boolean  downloadFile (String serverFile, String localFile) throws FTPConnectionClosedException, CopyStreamException ,IOException {
		FileOutputStream out = new FileOutputStream(localFile);
		boolean result =false;
		try{
			result= ftp.retrieveFile(serverFile, out);
		}
		catch(FTPConnectionClosedException ftperror){
			System.out.println("the FTP server prematurely closes the connection " + ftperror);
			
		}
		catch(CopyStreamException iocopyerror){
			System.out.println("An I/O error occurs while actually transferring the file " + iocopyerror);
		}
		catch(IOException ioerror){
			System.out.println("An I/O error occurs while either sending a command to the server " +
					"or receiving a reply from the server" + ioerror);
		}
		
		if (result==true) System.out.println(serverFile +" downloaded ");
		out.close();		
		return result;

}

	//Upload a file to the server 
	public boolean uploadFile (String queryString, String serverFile) throws FTPConnectionClosedException, CopyStreamException ,IOException{
		ByteArrayInputStream bain = new ByteArrayInputStream(queryString.getBytes());
		boolean result =false;
		try{
			//	result= ftp.storeFile(serverFile, in);
			result= ftp.storeFile(serverFile, bain);
		}
		catch(FTPConnectionClosedException ftperror){
			System.out.println("the FTP server prematurely closes the connection " + ftperror);
			result=false;
		}
		catch(CopyStreamException iocopyerror){
			System.out.println("An I/O error occurs while actually transferring the file " + iocopyerror);
			result=false;
		}
		catch(IOException ioerror){
			System.out.println("An I/O error occurs while either sending a command to the server " +
					"or receiving a reply from the server" + ioerror);
			result=false;
		}
		
		if (result==true) System.out.println(serverFile +" upload ");
		return result;
	}

	public void connectServer(String server, String user, String pass){
        try
        {
        	int reply;
            ftp.connect(server);
            System.out.println("Connected to " + server + ".");

            // After connection attempt, you should check the reply code to verify
            // success.
            reply = ftp.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply))
            {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                //System.exit(1); 
            }
        }
        catch (IOException e)
        {
            if (ftp.isConnected())
            {
                try
                {
                    ftp.disconnect();
                }
                catch (IOException f)
                {
                    // do nothing
                }
            }
            System.err.println("Could not connect to server.");
            e.printStackTrace();
            System.exit(1);
        }
        try
        {
        	if (!ftp.login(user,pass))
        	{
        		ftp.logout();
        		System.exit(1);
        	}
        }catch (IOException e) {e.printStackTrace();
        	
        }
        try
        {
        	System.out.println("Remote system is " + ftp.getSystemName());
        }catch (IOException e) {e.printStackTrace();
    	
        }
	}
	
	public void setPassiveMode(boolean setPassive) {
		if (setPassive)
			ftp.enterLocalPassiveMode();		
		else
			ftp.enterLocalActiveMode();
	}
	
	public void ascii () throws IOException {
		boolean result = ftp.setFileType(FTP.ASCII_FILE_TYPE);
		if (result) System.out.println("Ascii transfert mode enabled");
	}
	
	public void setbinary () throws IOException {
		boolean result=ftp.setFileType(FTP.BINARY_FILE_TYPE);
		if (result) System.out.println("Binary transfert mode enabled");
	}

	public void deleteFile( String file) throws IOException {
		boolean result=ftp.deleteFile(file);
		if (result) System.out.println("File "+file+" deleted");
	}	


	
	public void deleteDirectory(String directory) throws IOException {
		int reply=ftp.rmd(directory);
		if (FTPReply.isPositiveCompletion(reply))
			System.out.println("Directory "+directory+" deleted");
		else 
			System.out.println("Can't delete the folder "+directory+" ...");
	}
	
	public int ChangeDirectory (String path) throws IOException {
		return ftp.cwd(path);
	}
	
	public int ExecCommand (String command, String arg) throws IOException {
		return ftp.sendCommand(command,arg);
	}
	
	public  FTPFile[] getListFiles()throws IOException {
		return ftp.listFiles();
	}

	public int Cdup () throws IOException {
		return ftp.cdup();
	}
	public void closesession () throws IOException {
		ftp.logout();
		System.out.println("Logout");
	}
	
	public boolean isDirectoryExist (String directory) throws IOException {
		int reply;
		reply = this.ChangeDirectory(directory);
		if (!FTPReply.isPositiveCompletion(reply))
		{
			System.err.println("Directory "+directory+" doesn't exist");
			return false;
		}
		else
		{
			System.out.println("Directory "+directory+" exist");
			this.Cdup();
			return true;			
		}
	}
	
	
	public void DisconnectFTP () throws IOException {
		ftp.disconnect();
		System.out.println("Disconnect");
	}	
 
	
}


package ro.cst.tsearch.monitor;

import java.util.*;
import java.io.*;

import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import pt.ipb.agentapi.AgentObject;
import pt.ipb.agentapi.MessageException;
import pt.ipb.snmp.type.smi.Unsigned;
import pt.ipb.snmp.type.smi.VarBind;
import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.URLMaping;
 


import org.apache.log4j.Logger;

public class DiskFree extends AgentObject {
    
	private static final Logger logger = Logger.getLogger(DiskFree.class);
	
    private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
    private static long lastNotificationTime = 0;

    public DiskFree(String oid) {
        super(oid);
    }    

    public VarBind get(String oid) throws MessageException {
        
        long freeSpace = 0;
        
        logger.info(oid + " START diskFree [" + freeSpace + "].");
        try {
             
            freeSpace = getFreeSpace(rbc.getString("Resin_Partition"));
            
            if (freeSpace < 500 * 1024) {
                 
                if (System.currentTimeMillis() - lastNotificationTime > 3600000) {
                
	                MimeMessage mail = Log.prepareMailMessage(
	                		MailConfig.getMailFrom(),
	                        MailConfig.getMailLoggerToEmailAddress(), "", "", 
	                        "Important - Disk space is too low on " + ServerConfig.getAppUrl() + "!",
	                        "Disk space is too low on " + ServerConfig.getAppUrl() 
	                        + " - [" + freeSpace + "] Kbytes left.\n");
	                
	                Transport.send(mail);
	                
	                lastNotificationTime = System.currentTimeMillis();
                }
                
            } else {
                lastNotificationTime = System.currentTimeMillis();
            }
        } catch (Exception e) {} 

        logger.info(oid + " END diskFree [" + freeSpace + "].");
        return new VarBind(new String(getOID()), new Unsigned(freeSpace));
    }

    public static long getFreeSpace(String path) throws Exception {

        if (System.getProperty("os.name").startsWith("Windows")) {
            return getFreeSpaceOnWindows(path);
        }
        if (System.getProperty("os.name").startsWith("Linux")) {
            return getFreeSpaceOnLinux(path);
        }
        if (System.getProperty("os.name").startsWith("SunOS")) {
            return getFreeSpaceOnSunOS(path);
        }

        throw new UnsupportedOperationException("The method getFreeSpace(String path) has not "
                + "been implemented for this operating system [" + System.getProperty("os.name") + "].");
    }

    private static long getFreeSpaceOnWindows(String path) throws Exception {
        long bytesFree = -1;

        File script = new File(System.getProperty("java.io.tmpdir"), "script.bat");
        PrintWriter writer = new PrintWriter(new FileWriter(script, false));
        writer.println("dir \"" + path + "\"");
        writer.close();

        // get the output from running the .bat file
        String[] execCmd = new String[1];
        execCmd[0] = script.getAbsolutePath();
               
        ClientProcessExecutor cpe = new ClientProcessExecutor( execCmd, true, true );
        cpe.start();
        String outputText = cpe.getCommandOutput();


        // parse the output text for the bytes free info
        StringTokenizer tokenizer = new StringTokenizer(outputText, "\n");
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken().trim();
            // see if line contains the bytes free information
            if (line.endsWith("bytes free")) {
                tokenizer = new StringTokenizer(line, " ");
                tokenizer.nextToken();
                tokenizer.nextToken();
                bytesFree = Long.parseLong(tokenizer.nextToken().replaceAll(",", ""));
            }
        }
        return bytesFree / 1024;
    }

    private static long getFreeSpaceOnLinux(String path) throws Exception {
        long bytesFree = -1;

        String[] execCmd = new String[1];
        execCmd[0] = "df " + "/" + path;
        
        ClientProcessExecutor cpe = new ClientProcessExecutor( execCmd, true, true );
        cpe.start();

        String outputText = cpe.getCommandOutput();

        // parse the output text for the bytes free info
        StringTokenizer tokenizer = new StringTokenizer(outputText, "\n");
        tokenizer.nextToken();
        if (tokenizer.hasMoreTokens()) {
            String line2 = tokenizer.nextToken();
            StringTokenizer tokenizer2 = new StringTokenizer(line2, " ");
            if (tokenizer2.countTokens() >= 4) {
                tokenizer2.nextToken();
                tokenizer2.nextToken();
                tokenizer2.nextToken();
                bytesFree = Long.parseLong(tokenizer2.nextToken());
                return bytesFree;
            }

            return bytesFree;
        }

        throw new Exception("Can not read the free space of " + path + " path");
    }

    private static long getFreeSpaceOnSunOS(String path) throws Exception {
        long bytesFree = -1;

        String[] execCmd = new String[1];
        execCmd[0] = "df -b " + "/" + path;
        
        ClientProcessExecutor cpe = new ClientProcessExecutor( execCmd, true, true );
        cpe.start();
        
        String outputText = cpe.getCommandOutput();

        // parse the output text for the bytes free info
        StringTokenizer tokenizer = new StringTokenizer(outputText, "\n");
        tokenizer.nextToken();
        if (tokenizer.hasMoreTokens()) {
            String line2 = tokenizer.nextToken();
            StringTokenizer tokenizer2 = new StringTokenizer(line2, " ");
            if (tokenizer2.countTokens() >= 2) {
                tokenizer2.nextToken();
                bytesFree = Long.parseLong(tokenizer2.nextToken());
                return bytesFree;
            }

            return bytesFree; 
        }

        throw new Exception("Can not read the free space of " + path + " path");
    }

    public static void main(String args[]) {
        try {
            logger.info("Free space of " + rbc.getString("Resin_Partition") 
                    + " is " + getFreeSpace(rbc.getString("Resin_Partition")) * 1024 + " bytes.");
        } catch (Exception ex) {            
            ex.printStackTrace();
        }
    }
}
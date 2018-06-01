package ro.cst.tsearch.monitor;

import java.io.File;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import pt.ipb.agentapi.AgentObject;
import pt.ipb.agentapi.MessageException;
import pt.ipb.snmp.type.smi.Unsigned;
import pt.ipb.snmp.type.smi.VarBind;
import ro.cst.tsearch.utils.URLMaping;

import org.apache.log4j.Logger;

public class LogSize extends AgentObject {
    
	private static final Logger logger = Logger.getLogger(LogSize.class);
	
    private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
    
    public LogSize(String oid) {
        super(oid);
    }    

    public VarBind get(String oid) throws MessageException {
        
        long logSize = 0;
        
        logger.info(oid + " START logSize [" + logSize + "].");
        try {
            
            StringTokenizer st = new StringTokenizer(rbc.getString("Log_Folder"), "|");
            while (st.hasMoreTokens()) {
                logSize += getLogSize(st.nextToken().trim());
            }
                        
        } catch (Exception e) {}

        logger.info(oid + " END logSize [" + logSize + "].");        
        return new VarBind(new String(getOID()), new Unsigned(logSize));
    }

    private static long getLogSize(String path) throws Exception {
        
        File logFolder = new File(path);
        if (logFolder != null && logFolder.isDirectory()) {
            
            long totalSize = 0;
            
            File[] logFiles = logFolder.listFiles();
            if (logFiles != null && logFiles.length > 0) {
                
                for (int i = 0; i < logFiles.length; i++) {
                    
                    totalSize += logFiles[i].length();
                }
                
            }
            
            return totalSize;
        }
        
        throw new Exception("Can not read the log size of " + path + " path");
        
    }
    
    public static void main(String args[]) {
        try {
            
            long logSize = 0;
            
            StringTokenizer st = new StringTokenizer(rbc.getString("Log_Folder"), "|");
            while (st.hasMoreTokens()) {
                logSize += getLogSize(st.nextToken().trim());
            }
            
            logger.info("Free space of " + rbc.getString("Log_Folder") 
                    + " is " + logSize + " bytes.");
            
        } catch (Exception ex) {            
            ex.printStackTrace();
        }
    }
    
}

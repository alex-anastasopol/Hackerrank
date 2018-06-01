package ro.cst.tsearch.monitor;

import org.apache.log4j.Logger;

import pt.ipb.agentapi.AgentObject;
import pt.ipb.agentapi.MessageException;
import pt.ipb.snmp.type.smi.Unsigned;
import pt.ipb.snmp.type.smi.VarBind;
import ro.cst.tsearch.data.User;

public class ActiveUsers extends AgentObject {
    
	private static final Logger logger = Logger.getLogger(ActiveUsers.class);
	
    public ActiveUsers(String oid) {
        super(oid);
    }

    public VarBind get(String oid) throws MessageException {        

    	//must divide the activeUserList hashtable size by 2, as the hashtable contains two different entries
    	//for each user
    	long activeUsers = 0;
    	
    	logger.info(oid + " START activeUsers [" + activeUsers + "].");
        activeUsers = (long) User.getActiveUsers().size()  / 2;

    	logger.info(oid + " END activeUsers [" + activeUsers + "].");        
        return new VarBind(new String(getOID()), new Unsigned(activeUsers));
    }

}

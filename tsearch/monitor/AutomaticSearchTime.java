package ro.cst.tsearch.monitor;

import org.apache.log4j.Logger;

import pt.ipb.agentapi.AgentObject;
import pt.ipb.agentapi.MessageException;
import pt.ipb.snmp.type.smi.Unsigned;
import pt.ipb.snmp.type.smi.VarBind;

public class AutomaticSearchTime extends AgentObject {
   
	private static final Logger logger = Logger.getLogger(AutomaticSearchTime.class);
	
    public AutomaticSearchTime(String oid) {
        super(oid);
    }
    
    private static Time hitTime = new Time();
    public static void update(long time) {
        hitTime.update(time);        
    }

    public VarBind get(String oid) throws MessageException {        
    	long averageTime = 0;
    	
    	logger.info(oid + " START automaticSearchTime [" + averageTime + "].");
        averageTime = (long) AutomaticSearchTime.hitTime.read() / 1000;
        
        AutomaticSearchTime.hitTime.reset();
        
    	logger.info(oid + " END automaticSearchTime [" + averageTime + "].");
        return new VarBind(new String(getOID()), new Unsigned(averageTime));
    }
}

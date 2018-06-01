package ro.cst.tsearch.monitor;

import org.apache.log4j.Logger;

import pt.ipb.agentapi.AgentObject;
import pt.ipb.agentapi.MessageException;
import pt.ipb.snmp.type.smi.Unsigned;
import pt.ipb.snmp.type.smi.VarBind;

public class FilterResponseTime extends AgentObject {
    
	private static final Logger logger = Logger.getLogger(FilterResponseTime.class);
	
    public FilterResponseTime(String oid) {
        super(oid);
    }
    
    private static Time hitTime = new Time();
    public static void update(long time) {
        hitTime.update(time);        
    }

    public VarBind get(String oid) throws MessageException {        
    	long averageTime = 0;

    	logger.info(oid + " START filterResponseTime [" + averageTime + "].");
    	averageTime = (long) FilterResponseTime.hitTime.read();
        
        FilterResponseTime.hitTime.reset();
        
        logger.info(oid + " END filterResponseTime [" + averageTime + "].");        
        return new VarBind(new String(getOID()), new Unsigned(averageTime));
    }
}
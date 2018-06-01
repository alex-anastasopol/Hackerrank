package ro.cst.tsearch.monitor;

import pt.ipb.agentapi.AgentObject;
import pt.ipb.agentapi.MessageException;
import pt.ipb.snmp.type.smi.Unsigned;
import pt.ipb.snmp.type.smi.VarBind;
import ro.cst.tsearch.servlet.BaseServlet;

import org.apache.log4j.Logger;

public class BaseServletHitTime extends AgentObject {

	private static final Logger logger = Logger.getLogger(BaseServletHitTime.class);
	
    public BaseServletHitTime(String oid) {
        super(oid);
    }

    public VarBind get(String oid) throws MessageException {        
    	long averageTime = 0;

    	logger.info(oid + " START hitTime [" + averageTime + "].");
        averageTime = (long) BaseServlet.hitTime.read() / 1000;
        BaseServlet.hitTime.reset();
        
        logger.info(oid + " END hitTime [" + averageTime + "].");
        
        return new VarBind(new String(getOID()), new Unsigned(averageTime));
    }
    
}

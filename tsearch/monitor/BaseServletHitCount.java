package ro.cst.tsearch.monitor;

import org.apache.log4j.Logger;

import pt.ipb.agentapi.AgentObject;
import pt.ipb.agentapi.MessageException;
import pt.ipb.snmp.type.smi.Unsigned;
import pt.ipb.snmp.type.smi.VarBind;
import ro.cst.tsearch.servlet.BaseServlet;



public class BaseServletHitCount extends AgentObject {    

	private static final Logger logger = Logger.getLogger(BaseServletHitCount.class);
	
    long lastCount = 0;
    
    public BaseServletHitCount(String oid) {
        super(oid);
    }

    public VarBind get(String oid) throws MessageException {        
        long hitCount = 0;
        
        logger.info(oid + " START hitCount [" + hitCount + "].");
        hitCount = BaseServlet.hitCounter.read() - lastCount;
        lastCount = BaseServlet.hitCounter.read();
        
        logger.info(oid + " END hitCount [" + hitCount + "].");
        return new VarBind(new String(getOID()), new Unsigned(hitCount));
    }

}
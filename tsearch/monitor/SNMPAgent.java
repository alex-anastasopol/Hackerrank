package ro.cst.tsearch.monitor;

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import pt.ipb.agentapi.AbstractAgent;
import pt.ipb.agentapi.Table;
import pt.ipb.agentapi.demo.AtTable;
import pt.ipb.agentapi.demo.IFTable;
import pt.ipb.agentapi.engine.Engine;
import pt.ipb.agentapi.engine.EngineFactory;
import pt.ipb.snmp.SnmpConstants;
import pt.ipb.snmp.SnmpProperties;
import ro.cst.tsearch.utils.URLMaping;

public class SNMPAgent extends AbstractAgent {

    public static final Logger logger = Logger.getLogger(SNMPAgent.class);

    
    public final static String BASE_SERVLET_HIT_COUNT_INSTANCE = ".1.3.6.1.2.1.1.14.0";
    public final static String BASE_SERVLET_HIT_TIME_INSTANCE = ".1.3.6.1.2.1.1.15.0";
    public final static String AUTOMATIC_SEARCH_TIME_INSTANCE = ".1.3.6.1.2.1.1.16.0";
    public final static String CONVERT_TO_PDF_TIME_INSTANCE = ".1.3.6.1.2.1.1.17.0";
    public final static String FILTER_RESPONSE_TIME_INSTANCE = ".1.3.6.1.2.1.1.18.0";
    public final static String PARSER_TIME_INSTANCE = ".1.3.6.1.2.1.1.8.0";
    public final static String TSD_TIME_INSTANCE = ".1.3.6.1.2.1.1.9.0";
    public final static String TSCONNECTION_TIME_INSTANCE = ".1.3.6.1.2.1.1.10.0";
    public final static String DISK_FREE_INSTANCE = ".1.3.6.1.2.1.1.11.0";
    public final static String LOG_SIZE_INSTANCE = ".1.3.6.1.2.1.1.12.0";
    public final static String ACTIVE_USERS_INSTANCE = ".1.3.6.1.2.1.1.13.0";
    public final static String IN_NETWORK_TRAFFIC = ".1.3.6.1.2.1.1.19.0";
    public final static String OUT_NETWORK_TRAFFIC = ".1.3.6.1.2.1.1.20.0";
    public final static String IN_LOCALHOST_TRAFFIC = ".1.3.6.1.2.1.1.21.0";
    public final static String OUT_LOCALHOST_TRAFFIC = ".1.3.6.1.2.1.1.22.0";
    
    public final static String IFTABLE = ".1.3.6.1.2.1.2.2";
    public final static String SNMP_TARGET_MIB = ".1.3.6.1.6.3.12";
    public final static String SNMP_TARGET_OBJECTS = new String(SNMP_TARGET_MIB + ".1");
    public final static String SNMP_TARGET_ADDR_TABLE = new String(SNMP_TARGET_OBJECTS + ".2");
    
    public static boolean started = false;

    public SNMPAgent() {
        super();
    }

    public void setObjects() {

        try
        {
	        Table ct = new Table(IFTABLE, new IFTable());
	        addObject(ct);
	
	        ct = new Table(AtTable.ATTABLE, new AtTable());
	        addObject(ct);
	
	        SnmpTargetAddrTable model = new SnmpTargetAddrTable("0.0.0.0.0.0.0.0.0.0.0");
	        Table c = new Table(SNMP_TARGET_ADDR_TABLE, model);
	        addObject(c);
	
	        BaseServletHitCount bstc = new BaseServletHitCount(BASE_SERVLET_HIT_COUNT_INSTANCE);
	        addObject(bstc);
	        
	        BaseServletHitTime bsht = new BaseServletHitTime(BASE_SERVLET_HIT_TIME_INSTANCE);
	        addObject(bsht);
	        
	        AutomaticSearchTime ast = new AutomaticSearchTime(AUTOMATIC_SEARCH_TIME_INSTANCE);
	        addObject(ast);
	        
	        ConvertToPdfTime ctpt = new ConvertToPdfTime(CONVERT_TO_PDF_TIME_INSTANCE);
	        addObject(ctpt);
	        
	        FilterResponseTime frt = new FilterResponseTime(FILTER_RESPONSE_TIME_INSTANCE);
	        addObject(frt);
	        
	        ParserTime pt = new ParserTime(PARSER_TIME_INSTANCE);
	        addObject(pt);
	        
	        TSDTime tt = new TSDTime(TSD_TIME_INSTANCE);
	        addObject(tt);
	
	        TSConnectionTime tct = new TSConnectionTime(TSCONNECTION_TIME_INSTANCE);
	        addObject(tct);
	        
	        DiskFree df = new DiskFree(DISK_FREE_INSTANCE);
	        addObject(df);
	        
	        LogSize ls = new LogSize(LOG_SIZE_INSTANCE);
	        addObject(ls);
	        
	        ActiveUsers au = new ActiveUsers(ACTIVE_USERS_INSTANCE);
	        addObject(au);
	        
	        NetworkTrafficIn inTraffic = new NetworkTrafficIn( IN_NETWORK_TRAFFIC );
	        addObject( inTraffic );
	        
	        NetworkTrafficOut outTraffic = new NetworkTrafficOut( OUT_NETWORK_TRAFFIC );
	        addObject( outTraffic );
	        
	        NetworkTrafficIn inLocalhostTraffic = new NetworkTrafficIn( IN_LOCALHOST_TRAFFIC );
	        addObject( inLocalhostTraffic );
	        
	        NetworkTrafficOut outLocalhostTraffic = new NetworkTrafficOut( OUT_LOCALHOST_TRAFFIC );
	        addObject( outLocalhostTraffic );    
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
    
    public static synchronized final void initSNMP() {

        // if the init.snmp.start is defined and has "false" propery, then do not run the init snmp task
        boolean start = true;
        try{	
        	start = Boolean.parseBoolean( rbc.getString( "init.snmp.start" ).trim() );
        }catch(Exception e){        	
        }
        if(!start){
        	System.err.println("init.snmp.start=false");
        	return;
        }
        
        // The engine for this agent
        Engine snmpEngine = null;

        // We have to define the properties to pass to the engine
        // It is also possible to read them from a file.
        
        if( started )
        {
            logger.info( "SNMP Engine already started" );
            return;
        }
        
        logger.info( "SNMP Engine not started, starting NOW..." );
        
        try {
            
            SnmpProperties p = new SnmpProperties();
            p.setCommunity("public");
            p.setWriteCommunity("private");
            p.setPort(10161);
            p.setVersion(SnmpConstants.SNMPv2c);

            SNMPAgent agent = new SNMPAgent();

            logger.info("Trying to start SNMP Engine");
            // EngineFactory gets the engine class name from
            // the system property pt.ipb.agentpi.engine.Engine
            // Defaults to pt.ipb.agentapi.engine.snmp.JoeSnmpEngine
            snmpEngine = EngineFactory.createEngine();
            snmpEngine.setProperties(p);
            snmpEngine.addAgent(agent);
            agent.addMessageListener(snmpEngine.createAgentListener());

            snmpEngine.open();

            logger.info("SNMP Engine Ready");
            
            started = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
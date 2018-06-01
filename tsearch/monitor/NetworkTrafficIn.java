package ro.cst.tsearch.monitor;

import java.util.*;
import java.io.*;

import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import pt.ipb.agentapi.AgentObject;
import pt.ipb.agentapi.MessageException;
import pt.ipb.snmp.type.smi.Unsigned;
import pt.ipb.snmp.type.smi.VarBind;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.URLMaping;
 
import org.apache.log4j.Logger;

public class NetworkTrafficIn extends AgentObject
{
    /*
     * SNMP agent for network monitoring on SunOS
     */
	private static final Logger logger = Logger.getLogger(NetworkTrafficIn.class);
	private long previous_received_traffic = -1;
	private String currentOID;
	
    public NetworkTrafficIn(String oid)
    {
        super(oid);
        currentOID = oid;
    }
    public VarBind get(String oid) throws MessageException
    {
        long last_network_traffic = 0;
        long traffic = 0;
     
        logger.info( oid + " [START] NetworkTrafficIn = " + last_network_traffic );
        
        //if OS is Solaris, get the network stats
        if( System.getProperty("os.name").startsWith("SunOS") )
        {
            last_network_traffic = getInboundTraffic();
        }
        
        if( previous_received_traffic == -1 )
        {
            traffic = 0;
        }
        else
        {
            traffic = last_network_traffic - previous_received_traffic;
        }
        
        previous_received_traffic = last_network_traffic;
               
        //return the last 5 min average if not localhost traffic
        if( currentOID.equals( SNMPAgent.IN_NETWORK_TRAFFIC ) )
        {
            traffic = traffic / 300;
        }
        
        logger.info( oid + " [END] NetworkTrafficIn = " + last_network_traffic + " - " + previous_received_traffic + "=" + traffic );
        
        return new VarBind(new String(getOID()), new Unsigned(traffic));
    }
    
    private long getInboundTraffic()
    {
        long return_value = 0;
        String interfaceName;
        
        if( currentOID.equals(SNMPAgent.IN_NETWORK_TRAFFIC) )
        {
            interfaceName = "bge0";
        }
        else
        {
            interfaceName = "lo0";
        }
        
        try
        {
            String[] execCmd = new String[1];
            execCmd[0] = "netstat -k";
            
            ClientProcessExecutor cpe = new ClientProcessExecutor( execCmd, true, true );
            cpe.start();

            String outputText = cpe.getCommandOutput();
	        
	        int indexOfInterface = outputText.indexOf( interfaceName );
	        if( indexOfInterface == -1 )
	        {
	            return 0;
	        }
	        
	        outputText = outputText.substring( indexOfInterface );
	        
	        // parse the output text for the number of received bytes 
	        StringTokenizer tokenizer = new StringTokenizer(outputText, "\n");
	                
	        //for each line in the output, we search for the ats or atsdev host name
	        while( tokenizer.hasMoreTokens() )
	        {
	            String line2 = tokenizer.nextToken();
	            StringTokenizer tokenizer2 = new StringTokenizer(line2, " ");
	            while ( tokenizer2.hasMoreTokens() )
	            {
	                String attrName = tokenizer2.nextToken();
	                String value;
	                
	                if( tokenizer2.hasMoreTokens() )
	                {
	                    value = tokenizer2.nextToken();
	                }
	                else
	                {
	                    continue;
	                }

	                if( attrName.equals( "rbytes" ) && currentOID.equals( SNMPAgent.IN_NETWORK_TRAFFIC ) )
	                {
	                    return_value = Long.parseLong( value );
		                //traffic in bytes
		                return return_value ;
	                }
	                else if( attrName.equals( "ipackets" ) && currentOID.equals( SNMPAgent.IN_LOCALHOST_TRAFFIC ) )
	                {
	                    return_value = Long.parseLong( value );
		                //traffic in bytes
		                return return_value ;	                    
	                }
	            }
	        }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        
        return return_value;
    }
}
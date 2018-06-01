/*
 * Created on Apr 23, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.connection;
 
import java.net.Socket;

import ro.cst.tsearch.connection.ConnectionException;

import org.apache.log4j.Logger;

/**
 * @author 
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AliveTester {
	
	private static final Logger logger = Logger.getLogger(AliveTester.class);
	
	public static final int HTTP_PORT=80;
	public static final int MAX_RETRY_COUNT=3;
	private ConnectionException ce=new ConnectionException();
	public boolean canConnect(String IP)
	{
		return canConnect(IP,HTTP_PORT);
	}
	public boolean canConnect(String IP,int port)
	{
		boolean rez=false;
		ce=null;
		try
		{
			Socket s=new Socket(IP,port);
			if(s!=null && s.isConnected()) rez=true;
			s.close();
		}
		catch(Exception ex)
		{	
			    ce=new ConnectionException();
				ce.setStackTrace(ex.getStackTrace());
				StringBuffer sb=new StringBuffer();
				sb.append("Error while connecting to IP:["+IP+"] port:["+port+"]\n");
				sb.append(ex.getMessage());
				ce.setLog(sb.toString());
		}
		return rez;
	}
	public boolean tolerantCanConnect(String IP)
	{
		 return tolerantCanConnect(IP,HTTP_PORT);
	}
	public boolean tolerantCanConnect(String IP,int port)
	{ 
		 boolean r;
		 int i;		 
		 r=false;
		 i=0;		 
		 while(!r && i<MAX_RETRY_COUNT)
		 {
		  logger.info("Trying ["+i+"] ["+IP+":"+port+"]");	
		  r=r || canConnect(IP,port);	
		  i++; 	 
		 }
		return r;
	}
	public static void testsite(AliveTester at,String addr,int port)
	{
		boolean r=at.tolerantCanConnect(addr,port);
		logger.info("Testing ["+addr+":"+port+"]");
		if(r==false) logger.error("Error :"+at.getError().getLog());
		else
		logger.info("OK!");
	}
	public static void testsite(AliveTester at,String addr)
	{
		testsite(at,addr,HTTP_PORT);
	}
	public static void main(String args[])
	{
		AliveTester at=new AliveTester();	
		//Shelby	
		logger.info("Shelby");	
		testsite(at,"www.assessor.shelby.tn.us");
		testsite(at,"epayments.cityofmemphis.org");
		testsite(at,"shelby.bisonline.com");
		testsite(at,"www.shelbycountytrustee.com");		
		//Davidson
		logger.info("Davidson");
		testsite(at,"www3.nashville.org");
		testsite(at,"www.nashville.gov");
		testsite(at,"www.registerofdeeds.nashville.org");
		//Knox
		logger.info("Knox");
		testsite(at,"www.kgis.org");
		testsite(at,"www.knoxcounty.org");
		testsite(at,"www.ci.knoxville.tn.us");		
		testsite(at,"www.knoxrod.org");
	}
	/**
	 * @return
	 */
	public ConnectionException getError() {
		return ce;
	}

	/**
	 * @param exception
	 */
	public void setError(ConnectionException exception) {
		ce = exception;
	}

}

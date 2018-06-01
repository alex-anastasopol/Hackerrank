package ro.cst.tsearch.ps2txt;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.log4j.Logger;
/*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
/**
 * @author cozmin
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ps2txtsrv {
	
	private static final Logger logger = Logger.getLogger(ps2txtsrv.class);
	
	public static final int port=6004;
	public boolean running=true;
	public void do_server()
	{
		ServerSocket ss=null; 
				try
				{
					ss=new ServerSocket(port,20);
					ss.setReuseAddress(true);
					ss.setSoTimeout(2000);
					ThreadGroup tg=new ThreadGroup("PS workers");
				
					while(running==true)
					{				
					logger.info("Waiting...");
					Socket s=null;
					while(running==true && s==null)
					{
						try{
							  s=ss.accept();
						   }
						catch(SocketTimeoutException ex)
						{
							s=null;
						}
					}
					
					
					logger.info("New client!");
					if(tg.activeCount()<20)
					   {
							ps2txtworker w=new ps2txtworker(tg,"worker",s,this);
							w.start();				  
							logger.info("Handler started!"); 	    
					   }
					}
				  
				}
				catch(Throwable t)
				{
					t.printStackTrace();				  
				}
				finally{
					if(ss!=null)
					   try {
						ss.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}	 
	}
	public static void main(String args[])
	{				
				ps2txtsrv srv=new ps2txtsrv();
				srv.do_server();		
	}
}

/*
 * Created on Sep 14, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.ps2txt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

/**
 * @author cozmin
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments 
 */
public class ps2txtkiller {
	
	private static final Logger logger = Logger.getLogger(ps2txtkiller.class);
	
	public static void main(String args[]) throws UnknownHostException, IOException
	{
		///send an end package
		Socket s=new Socket("etitle.cst-us.com",ps2txtsrv.port);			
		ObjectOutputStream os=new ObjectOutputStream(s.getOutputStream());		
		ObjectInputStream is=new ObjectInputStream(s.getInputStream());		
		ps2txtpacket pkt;
					//send end					
							pkt=new ps2txtpacket();
							pkt.setType(ps2txtpacket.SRV_STOP);
							os.writeObject(pkt);
							os.flush();				
		s.close();	
		logger.info("ps2txt Server stopped!");				
	}
}

package ro.cst.tsearch.ps2txt;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
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
public class ps2txtworker extends Thread {
	private static final Logger logger = Logger.getLogger(ps2txtworker.class);
	
	Socket s;
	ObjectInputStream is=null;
	ObjectOutputStream os=null;
	ps2txtsrv srv;
	public ps2txtworker(ThreadGroup tg,String name,Socket sock,ps2txtsrv sv)
	{
		super(tg,name);		
		srv=sv;
		s=sock;		
		try{
			logger.info("Input stream ...");
			is=new ObjectInputStream(s.getInputStream());
			logger.info("output stream ...");
			os=new ObjectOutputStream(s.getOutputStream());
		}
		catch(IOException e)
		{}
	}
	private synchronized File getnewFile() throws Exception
	{
		sleep(1000);
		SimpleDateFormat sd=new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
		String name=sd.format(new Date());
		logger.info(name);
		File f=new File("file_"+name+".ps");
		return f;
	}
	private String convert2txt(File fps) throws Exception	
	{
		String res=null;
		String txtname=null;
		if(fps!=null && fps.exists())
		{
			res=fps.getAbsolutePath()+".txt";
			String cmd="ps2ascii "+fps.getAbsolutePath()+" "+res;
            String[] execCmd = new String[1];
            execCmd[0] = cmd;
            
			logger.info("Executing :"+cmd);
            
            ClientProcessExecutor cpe = new ClientProcessExecutor(execCmd, true, true);
            cpe.start();
            
		    if( cpe.getReturnValue() !=0 ) logger.info("Error!");
			logger.info("Done Executing ! :"+cmd);
		}
		return res;
	}
	private void handler() throws Exception
	{
		
		logger.info("In Handler!!!");
		ps2txtpacket pkt=(ps2txtpacket)is.readObject();
		
		if(pkt.getType().equals(ps2txtpacket.SRV_STOP))
		{
			srv.running=false;
			return;
		}
		
		if(pkt.getType().equals(ps2txtpacket.PS_START))
		{
			logger.info("received start");
			//read ps data and dump it to a file
			File fps=getnewFile();
			BufferedOutputStream bo=new BufferedOutputStream(new FileOutputStream(fps));
			pkt=(ps2txtpacket)is.readObject();
			while(pkt.getType().equals(ps2txtpacket.PS_DATA))
			{
				bo.write(pkt.getContent());
				bo.flush();
				pkt=(ps2txtpacket)is.readObject();
			}			
			bo.close();
			///launch convert routine
			logger.info("launch convert");
			String restxt=convert2txt(fps);
			logger.info("Done convert :"+restxt);
			///send result file
			File restxtfile=null; 
			if(restxt!=null)
			{
				restxtfile=new File(restxt);
				if(restxtfile.exists())
				{
					BufferedInputStream bi=new BufferedInputStream(new FileInputStream(restxtfile));
					//send start txt
					pkt=new ps2txtpacket();
					pkt.setType(ps2txtpacket.TXT_START);
					os.writeObject(pkt);
					os.flush();
					/// send data txt 
					byte buf[]=new byte[8192];
					int i=bi.read(buf);
					while(i>0)
					{
						pkt=new ps2txtpacket();
					    pkt.setType(ps2txtpacket.TXT_DATA);
					    pkt.setContent(buf,i);
					    os.writeObject(pkt);
					    os.flush();
						i=bi.read(buf);
					}	
					bi.close();
				}
//				send stop txt
								  pkt=new ps2txtpacket();
									pkt.setType(ps2txtpacket.TXT_STOP);									
								  os.writeObject(pkt);
								  os.flush();
			}			
		}
	}
	public void run()
	{
		try{
			logger.info("Begin thread!");
			handler();
			s.close();						
			logger.info("End thread!");
		}
		catch(Throwable t)
		{			
			t.printStackTrace();
		}
		finally
		{
			if(s!=null && !s.isClosed())
			  try {
				s.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

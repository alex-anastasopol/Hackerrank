package ro.cst.tsearch.processExecutor.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerExecutor {
	
	//private static 				FileOutputStream fileOut = null;
	public static final String 	BIND_IP 	= "127.0.0.1"; 
	public static final int 	BIND_PORT 	= 5131;
	//private static Object 		accessToLog = new Object();
	
	/*public static void log(String mes){
		String date = ServerExecutor.getDate();
		synchronized (accessToLog){
			if(fileOut!=null){
				try{
					fileOut.write((date+"\t"+mes+"\n").getBytes());
				}
				catch(Exception e){
					e.printStackTrace(System.err);
				}
			}
		} 
	}*/
	
	public static String getDate(){
		Calendar cal = Calendar.getInstance();
		Date date = cal.getTime();
		return date .toString() ;
	}
	
	public static void main(String args[]){
		
        int port = BIND_PORT;
        
	    if( args != null && args.length != 0 )
        {
            try
            {
                port = Integer.parseInt( args[0] );
            }catch( Exception e ) {}
        }
        
        System.err.println( port );
        
		/*File file =new File ("/ts/links/resin/tslogs/ServerExecutor.log");
		if (file.exists()){
			file.renameTo(new File("/ts/links/resin/tslogs/ServerExecutor_bak.log"));
		}
		 
		try{
			fileOut = new FileOutputStream ("/ts/links/resin/tslogs/ServerExecutor.log");
		}
		catch(Exception e){
			//e.printStackTrace();
			System.err.println("  --- ServerExecutor:: Nu pot creea stream pe fisierul /ts/links/resin/tslogs/ServerExecutor.log");
			try{
				fileOut = new FileOutputStream ("ServerExecutor.log");
			}
			catch(Exception e1){
				e1.printStackTrace(System.err); 
				fileOut=null;
			}
		}*/
		
		ServerSocket serverSok =null;
		try{
			serverSok = new ServerSocket( port );
		}
		catch(IOException ioE){
			ioE.printStackTrace(System.err);
			System.exit(-1);
		}
		
		ExecutorService myPool = Executors.newCachedThreadPool();
		        
		while ( true ) {
			  try {
				 
				 // ServerExecutor.log(" ----------  START ServerExecutor  ::  Accept clienti .....");
				  Socket curentClientSok = serverSok.accept( );
				  
				  //ServerExecutor.log(">>>>>>>>>>>  ServerExecutor :: Am acceptat un client "+ curentClientSok );   
		          Runnable sarcina = new PoolTask( curentClientSok ) ;
	
		          myPool.execute( sarcina );
			  }
			  catch(Exception e){
				  e.printStackTrace(System.err);
				  //ServerExecutor.log("::::ERROR:::: "+ e.getMessage());
				  continue;
			  }
		 }
	
	}
	
	
}

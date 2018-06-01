package ro.cst.tsearch.connection.http;

import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

public class HTTPCleanerConections {
	
	private static final int 	MAX_NR_MANAGERS = 10000;
	private static final int 	COUNT_DELETE_CLOSE_CONNECTIONS = 5;
	private static final long 	TIME_TO_SLEEP =  1000 * 60 * 5;
	private static final long 	CONNECTION_IDLE_TIME  = 1000 * 60 * 2;
	private static int curentCountDeleteCloseConnections = 0;
	private static Vector <MultiThreadedHttpConnectionManager> managers=new Vector<MultiThreadedHttpConnectionManager> ();
	private static Vector <PoolingClientConnectionManager> managers4 = new Vector<PoolingClientConnectionManager> ();
	
	@Deprecated
	public static synchronized int size(){
		return managers.size();
	}
	
	public static synchronized int getSize(){
		return managers.size() + managers4.size();
	}
	
	public static synchronized boolean addManager(MultiThreadedHttpConnectionManager manager){
		if(managers.size()<MAX_NR_MANAGERS ){
			managers.add(manager);
			return true;
		}
		return false;
	}
	
	public static synchronized boolean addManager(PoolingClientConnectionManager manager){
		if(managers4.size()<MAX_NR_MANAGERS ){
			managers4.add(manager);
			return true;
		}
		return false;
	}
	
	private static synchronized void closeIdleConnections(){	
		Vector<Integer> vecRemove=new Vector<Integer>();
		Vector<Integer> vecRemove4 =new Vector<Integer>();
		
		if(curentCountDeleteCloseConnections >= COUNT_DELETE_CLOSE_CONNECTIONS){
			
			
			for(int i=0;i<managers.size();i++){
				MultiThreadedHttpConnectionManager manager = managers.get(i);
				if(manager != null){
					manager.closeIdleConnections(CONNECTION_IDLE_TIME);	
					manager.deleteClosedConnections();
					curentCountDeleteCloseConnections = 0;
				}
				else{
					System.err.print(" >>>> --- CLEAN IDLE CONNECTIONS DAEMON START ---- \n");
					vecRemove.add(Integer.parseInt(i+""));
				}
			}	
			
			for(int i=0;i<managers4.size();i++){
				PoolingClientConnectionManager manager = managers4.get(i);
				if(manager != null){
					manager.closeIdleConnections(CONNECTION_IDLE_TIME, TimeUnit.MILLISECONDS);	
					manager.closeExpiredConnections();
					curentCountDeleteCloseConnections = 0;
				}
				else{
					System.err.print(" >>>> --- CLEAN IDLE CONNECTIONS DAEMON START ---- \n");
					vecRemove4.add(Integer.parseInt(i+""));
				}
			}	
		}
		else{
			for(int i=0;i<managers.size();i++){
				MultiThreadedHttpConnectionManager manager = managers.get(i);
				if(manager != null){
					manager.closeIdleConnections(CONNECTION_IDLE_TIME);	
				}
				else{
					System.err.print(" >>>> --- CLEAN IDLE CONNECTIONS DAEMON START ---- \n");
					vecRemove.add(Integer.parseInt(i+""));
				}
			}
			
			for(int i=0;i<managers4.size();i++){
				PoolingClientConnectionManager manager = managers4.get(i);
				if(manager != null){
					manager.closeIdleConnections(CONNECTION_IDLE_TIME, TimeUnit.MILLISECONDS);	
				}
				else{
					System.err.print(" >>>> --- CLEAN IDLE CONNECTIONS DAEMON START ---- \n");
					vecRemove4.add(Integer.parseInt(i+""));
				}
			}
			curentCountDeleteCloseConnections++;
		}
		
		
		for(int i=0;i<vecRemove.size();i++){
				managers.remove(vecRemove.get(i).intValue());
		}
		
		for(int i=0;i<vecRemove4.size();i++){
			managers4.remove(vecRemove4.get(i).intValue());
		}

		
	}
	
	static{
	
		Thread daemonCleaner=(new Thread(
				new Runnable(){
					public void run() {
						
						try{
							Thread.sleep(TIME_TO_SLEEP);
						}
						catch(InterruptedException e){
							e.printStackTrace();
						}
						
						while(true){
							try{
								System.err.print(" >>>> --- CLEAN IDLE CONNECTIONS DAEMON START ---- \n");
								closeIdleConnections();
								System.err.print(" >>>> --- CLEAN IDLE CONNECTIONS DAEMON STOP ---- \n");
								Thread.sleep(TIME_TO_SLEEP);
							}
							catch(InterruptedException e){
								e.printStackTrace();
								continue;
							}
						}
					}
		}			
		));
		
		daemonCleaner.setDaemon(true);
		daemonCleaner.start();		
		System.err.print("!!!!! --- CLEAN CONNECTION DAEMON WAS STARTED ---- !!!!!\n");
	}
	
}

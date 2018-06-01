package ro.cst.tsearch.threads;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.utils.TSOpCode;

public class MonitoringService implements Runnable {
	private static final Category logger = Logger.getLogger(MonitoringService.class);
	
	private static int lbsPortTCP = ServerConfig.getInteger("lbs.monitoring.port.tcp");
	private static int atsPortTCP = ServerConfig.getInteger("ats.monitoring.port.tcp");
	private static String address = ServerConfig.getString("lbs.monitoring.address");
	
	public static void init() {
		if(!ServerConfig.getBoolean("lbs.monitoring.enable", false)){
			System.err.println("lbs.monitoring.enable=false");
			logger.warn("Monitoring service not enabled");
			return;
		}
		
		MonitoringService monServ = new MonitoringService();
		
		Thread t = new Thread(monServ,"MonitoringService");
		t.start();
		
	}
	
	
	public void run() {
		try {
			ServerSocket server = new ServerSocket();
			server.bind(new InetSocketAddress(address,atsPortTCP));
			
			ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2,10,1,TimeUnit.SECONDS,
					new ArrayBlockingQueue<Runnable>(10,true));
			logger.debug("MonitoringService Waiting for TCP connections on: " + address + ":" + atsPortTCP);
			while (true) {
				try {
					Socket sock = server.accept();
					MonitoringWorker monWorker = new MonitoringWorker(sock);
					threadPool.submit(monWorker);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Sends a priority load update to the monitoring service
	 * @param currentLoad
	 */
	public static void sendLoadUpdate(float currentLoad) {
		Socket socket = null;
		ObjectOutputStream oos = null;
		try {
			SocketAddress sockAddres = new InetSocketAddress(InetAddress.getByName(address),lbsPortTCP);
			socket = new Socket();
			socket.connect(sockAddres, 1000);
			oos = new ObjectOutputStream(socket.getOutputStream());
			String toSend = TSOpCode.LBS_MONITORING_UPDATE_LOAD + ";"+currentLoad;
			oos.writeObject(toSend);
			oos.flush();
		} catch (Exception e) {
			logger.error("MonitoringService - sendLoadUpdate ", e);
		} finally{
//			logger.info("sendLoadUpdate before oos close");
//			if(oos!=null) try{oos.close();} catch (Exception e) {
//				logger.error("Error closing oos", e);
//			}
//			logger.info("sendLoadUpdate before socket close");
//			if(socket!=null) try{socket.close();} catch (Exception e) {
//				logger.error("Error closing socket", e);
//			}
			
		}
	}
	
	public static String getServersStatus(){
		Socket socket = null;
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		String result = null;
		try {
			logger.info("getServersStatus");
			SocketAddress sockAddres = new InetSocketAddress(InetAddress.getByName(address),lbsPortTCP);
			socket = new Socket();
			socket.connect(sockAddres, 1000);
			oos = new ObjectOutputStream(socket.getOutputStream());
			String toSend = TSOpCode.LBS_MONITORING_SRV_STATUS+"";
			oos.writeObject(toSend);
			oos.flush();
			ois = new ObjectInputStream(socket.getInputStream());
			result = (String)ois.readObject();
		} catch (Exception e) {
			logger.error("MonitoringService - sendLoadUpdate: ", e);
		} finally{
			if(ois!=null) try{ois.close();} catch (Exception e) {}
			if(oos!=null) try{oos.close();} catch (Exception e) {}
			
			logger.info("getServersStatus before close");
			if(socket!=null) try{socket.close();} catch (Exception e) {}
		}
		return result;
	}
	
	public static void main(String[] args) {
		sendLoadUpdate(1.2f);
		sendLoadUpdate(1.3f);
		sendLoadUpdate(1.4f);
		
	}

}

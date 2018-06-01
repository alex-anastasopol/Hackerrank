package ro.cst.tsearch.threads;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.MonitoringLoadBean;
import ro.cst.tsearch.loadBalServ.ServerInfoSingleton;
import ro.cst.tsearch.utils.TSOpCode;

public class MonitoringWorker implements Runnable{

	private static final Category logger = Logger.getLogger(MonitoringWorker.class);
	private Socket socket = null;
	
	public MonitoringWorker(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		ObjectInputStream ois = null;
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
			int opCode = ois.readInt();
			if(opCode==TSOpCode.LBS_MONITORING_LOAD){
				
				MonitoringLoadBean monitoringLoadBean = new MonitoringLoadBean();
				monitoringLoadBean.setLoad(ServerInfoSingleton.getLatestLocalLoad());
				monitoringLoadBean.setDeadlockFree(
						!ServerInfoSingleton.getInstance().isDeadlockCurrentlyDetected());
				monitoringLoadBean.setDatabaseUp(ServerInfoSingleton.getInstance().isDatabaseWorking());
				
				oos.writeObject(monitoringLoadBean);
				oos.flush();
				//logger.debug("MonitoringWorker: Sending load " + load);
			} else if(opCode==TSOpCode.LBS_MONITORING_SESSIONS) {
				logger.error("MonitoringWorked: LBS_MONITORING_SESSIONS - NOT IMPLEMENTED");
			} else if(opCode==TSOpCode.LBS_MONITORING_DBCONN) {
				logger.error("MonitoringWorked: LBS_MONITORING_DBCONN - NOT IMPLEMENTED");
			} else if(opCode==TSOpCode.LBS_MONITORING_ADDRESS) {	
				logger.error("MonitoringWorked: LBS_MONITORING_ADDRESS - NOT IMPLEMENTED");
			} else {
				logger.error("MonitoringWorked: opCode unknown (" + opCode + ")");
			}
			
			
		} catch (Exception e) {
			logger.error("Error while running MonitoringWorker", e);
		} finally {
			if(socket!=null) try {socket.close();} catch (Exception e){}
			if(ois!=null) try {ois.close();} catch (Exception e){}
			if(oos!=null) try {oos.close();} catch (Exception e){}
		}
		
	}

}

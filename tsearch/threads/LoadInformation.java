package ro.cst.tsearch.threads;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.LoadConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.loadBalServ.ServerInfoSingleton;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.NetworkData;
import ro.cst.tsearch.utils.URLMaping;

public class LoadInformation extends TimerTask {

	private class LoadMapper {
		private float cpu;
		private float mem;
		private float net;
		public LoadMapper(float cpu, float mem, float net) {
			super();
			this.cpu = cpu;
			this.mem = mem;
			this.net = net;
		}
		public float getCpu() {
			return cpu;
		}
		public float getMem() {
			return mem;
		}
		public float getNet() {
			return net;
		}
	}

	private static final Category logger = Logger.getLogger(LoadInformation.class);
	
	private static final long KB_RATIO			= 1024;
	private static final long MB_RATIO			= 1024*1024;
	
	private static final long TIMER_DELAY		= 1000; 			//1 second
	private static int probeWindowsize 			= ServerConfig.getInteger("probe.windowsize");
	private static int probePercentchange		= ServerConfig.getInteger("probe.percentchange");
	private static int networkBandwidth			= ServerConfig.getInteger("network.bandwidth");
	private static final boolean isEnabledCPUPrstat = ServerConfig.getBoolean("lbs.enable.cpu.prstat", false);
	
	private static final String RAMTotal 		= "RAMTotal";
	private static final String RAMFree 		= "RAMFree";
	public static float maxMemory				= ServerConfig.getInteger("load.information.max.memory");
	
	public LoadInformation(){
		
	}
	
	public static void init() {
		
        setLoginTime();
        
        long samplePeriod = ServerConfig.getInteger("probe.samplerate");
        samplePeriod*= 1000;		//we must translate seconds into millis
        
		LoadInformation loadInfo = new LoadInformation();
		
		Timer timerNow = new Timer("LoadInformation");
   	 	timerNow.schedule((TimerTask) loadInfo, TIMER_DELAY, samplePeriod);

	}
	
	private static void setLoginTime() {
		
		String now = "str_to_date( '" + 
				new FormatDate(FormatDate.TIMESTAMP).getDate(Calendar.getInstance().getTime()) + "' , '" + 
				FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
		
		String SQL_UPDATE_LOGIN_TIME = "UPDATE " + 
				DBConstants.TABLE_SERVER + " SET " + 
				DBConstants.FIELD_SERVER_LOGIN_TIME + " =  " + now  +
				" WHERE " + 
				DBConstants.FIELD_SERVER_ALIAS + " =? OR " + 
				DBConstants.FIELD_SERVER_ID + "= ?";
		
		try{
			DBManager.getSimpleTemplate().update(SQL_UPDATE_LOGIN_TIME, URLMaping.INSTANCE_DIR, ServerConfig.getServerId());
		} catch (Exception e) {
			logger.error("setLoginTime problem for " + DBConstants.FIELD_SERVER_LOGIN_TIME + " [" + now + "]", e);
		}
		
	}

	public void run() {
		
        if(!LoadConfig.getLoadInformationEnable()){
        	return;
        }
		
		Sigar sigar = null;
		if(ServerConfig.isEnableSigar()) {
			sigar = new Sigar();
		} else {
			logger.error("Sigar is not enabled");
		}
		
		float cpu = 0, cputemp = 0;
		float mem = 0, memtemp = 0;
		float net = 0, nettemp = 0;
		HashMap<String, Long> memHash = new HashMap<String, Long>(); 
		try {
			int limit = 0;
			for (int i = 0; i < probeWindowsize; i++) {
				if(!isEnabledCPUPrstat) {
					cputemp = getCPUUsage(sigar);
//					logger.info("CPU Info at first try is " + cputemp);
					limit = 0;
					while(cputemp < 0){
						if(limit>=probeWindowsize) {
							logger.error("Skipping LoadInformation because CPU load could not be determined");
							return;
						}
						
						try {
							if(cputemp < 0) {
								Thread.sleep(1000);
							}
						} catch (InterruptedException e) {
							System.err.println("---> Exception caught and ignored");
							e.printStackTrace();
							System.err.println("Exception caught and ignored <----");
						}
						
						logger.error("No data for CPU---- Try again!!!");
						cputemp = getCPUUsage(sigar);
						logger.info("CPU Info after sleep is " + cputemp);
						limit++;
					}
					//logger.info("Some data for CPU!!!");
					cpu += cputemp;
				} else {
					cpu += getCPUUsageLocal();		
				}
				//----------------
				memtemp = getMemoryUsage(sigar, memHash);
				limit = 0;
				while(memtemp<0){
					if(limit>=probeWindowsize)
						return;
					try {
						if(memtemp<=0) {
							Thread.sleep(1000);
						}
					} catch (InterruptedException e) {
						System.err.println("---> Exception caught and ignored");
						e.printStackTrace();
						System.err.println("Exception caught and ignored <----");
					}
					logger.error("No data for Memory---- Try again!!!");
					memtemp = getMemoryUsage(sigar, memHash);
					limit++;
				}
				mem += memtemp;
				//----------------
				nettemp = getNetworkUsage(sigar);
				limit = 0;
				while(nettemp<0){
					if(limit>=probeWindowsize)
						return;
					try {
						if(nettemp<=0) {
							Thread.sleep(1000);
						}
					} catch (InterruptedException e) {
						System.err.println("---> Exception caught and ignored");
						e.printStackTrace();
						System.err.println("Exception caught and ignored <----");
					}
					logger.error("No data for Network---- Try again!!!");
					nettemp = getNetworkUsage(sigar);
					limit++;
				}
				net += nettemp;
				if(i+i < probeWindowsize) {
					Thread.sleep(10);
				}
			}
			//logger.info("Dtrace data aquiered!");
//			logger.debug("CPU load computed " + cpu);
			cpu /= probeWindowsize;		//used
			mem /= probeWindowsize;		//used
			net /= probeWindowsize;		//absolute value of traffic - needs conversion to percent
			
			if(sigar == null ) {
				//---- converting net from percent to KB
				net *= LoadConfig.getNetworkBandwidthDefault()/100;		//determining bytes used
				net = networkBandwidth - net;		//determining the free internet bandwidth
				if(net<0) net = 0;					//if by any chance we apparently use more than the bandwidth
				net /= (1024*8);
			} else {
				//original net is received in KB
//				net = 1 - 100 * net * 8 / (networkBandwidthDefault/100/100 * 1024);
				net = net/LoadConfig.getNetworkBandwidthDefault();	//used
			}
			int sessions = getNumberOfSessions();
			int dbConn = getNumberOfDBConnections();
						
//			String sqlSelect = "SELECT CPU, MEMORY, NETWORK from " + DBConstants.TABLE_USAGE_INFO + 
//				" WHERE " + DBConstants.FIELD_USAGE_INFO_ID + "=  (select max(" + 
//				DBConstants.FIELD_USAGE_INFO_ID +") FROM " + DBConstants.TABLE_USAGE_INFO + " WHERE " + 
//				DBConstants.FIELD_USAGE_INFO_SERVER_NAME + " =?)";
			
			String sqlSelect = "SELECT CPU, MEMORY_FREE MEMORY, NETWORK from " + DBConstants.TABLE_SERVER + 
					" WHERE " + DBConstants.FIELD_SERVER_ALIAS + " = ?";
			
			
			SimpleJdbcTemplate simpleTemplate = DBManager.getSimpleTemplate();
			
			List<LoadMapper> lastSavedData = simpleTemplate.query(sqlSelect, new ParameterizedRowMapper<LoadMapper>() {

				@Override
				public LoadMapper mapRow(ResultSet rs, int rowNum) throws SQLException {
					LoadMapper loadMapper = new LoadMapper(rs.getFloat("CPU"), rs.getFloat("MEMORY"), rs.getFloat("NETWORK"));
					return loadMapper;
				}
			}, URLMaping.INSTANCE_DIR);
			
			
			float currentLoad = ServerInfoSingleton.getLoadAffinity(cpu, mem, net);
			
			
			String sqlInsert = "INSERT INTO " + DBConstants.TABLE_USAGE_INFO + 
				" (CPU, MEMORY, NETWORK, TIMESTAMP, SERVER_CONN, DB_CONN, SERVER_NAME, MEMORY_FREE, LOAD_FACTOR) VALUES ( " + 
				(cpu) + ", " +
				(mem) + ", " +
				(net) + ", now(), " +
				sessions + ", " +
				dbConn + ", \"" + 
				URLMaping.INSTANCE_DIR + "\", " +
				memHash.get(RAMFree) + ", " +
				currentLoad + ") ";
			
			String sqlInsertServers = "UPDATE " + 
				DBConstants.TABLE_SERVER + " SET " + 
				DBConstants.FIELD_USAGE_INFO_CPU + " = " + (cpu) + ", " +
				DBConstants.FIELD_USAGE_INFO_MEMORY_FREE + " = " + (mem) + ", " +
				DBConstants.FIELD_USAGE_INFO_NETWORK + " = " + (net) + " WHERE " + 
				DBConstants.FIELD_SERVER_ALIAS + " = \"" + URLMaping.INSTANCE_DIR + "\"";
			
			
			//cpu - float
			//MEMORY_FREE - float
			//netDB - float
			simpleTemplate.update(sqlInsert);
			simpleTemplate.update(sqlInsertServers);
			
			if(lastSavedData!=null && !lastSavedData.isEmpty()){
				checkAndUpdate(cpu, mem, net,
						(lastSavedData.get(0).getCpu()),
						(lastSavedData.get(0).getMem()),
						(lastSavedData.get(0).getNet()));
			}
			logger.debug("Finishing LoadInformation sequence... with cpu: " + cpu + ", mem: " + mem + ", net: " + net + ", load: " + currentLoad);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Something happend in the LoadInformation sequesce...", e);
		}

	}


	/**
	 * Compares current data with latest from the database and might send update to other services
	 * Takes into account the load and also high values of params
	 * @param cpu
	 * @param mem
	 * @param net
	 * @param cpuDB
	 * @param memDB
	 * @param netDB
	 * @return
	 */
	private boolean checkAndUpdate(float cpu, float mem, float net, float cpuDB, float memDB, float netDB) {
		if(probePercentchange<=0)
			return false;
		//TODO: check for problems.....
		//ServerInfoSingleton serverInfo = ServerInfoSingleton.getInstance();
		//if(!serverInfo.isEnabled() || serverInfo.isAtLeastOneServer())
		//	return false;
		
		
		float currentLoad = ServerInfoSingleton.getLoadAffinity(cpu, mem, net);
		float dbLoad = ServerInfoSingleton.getLoadAffinity(cpuDB, memDB, netDB);
		if(currentLoad>dbLoad){
			if(currentLoad > dbLoad*(100+probePercentchange)/100){
				logger.info("SENDING priority update -> load affinity increased from " + 
						dbLoad +  " to " + currentLoad);
				MonitoringService.sendLoadUpdate(currentLoad);
				return true;
			}
		} else if(currentLoad<dbLoad) {
			if(currentLoad < dbLoad*(100-probePercentchange)/100 ) {
				logger.info("SENDING priority update -> load affinity decreased from " + 
						dbLoad +  " to " + currentLoad);
				MonitoringService.sendLoadUpdate(currentLoad);
				return true;
			}
		}
		//logger.debug("NO update necessary!");
		return false;
	}

	private float getCPUUsage(Sigar sigar) {
		if(sigar != null) {
			try {
				CpuPerc cpuPerc = sigar.getCpuPerc();
				double combined = cpuPerc.getCombined();
//				logger.debug("CurrentCpuLoad is " + combined + 
//						", Double.valueOf(sigar.getCpuPerc().getCombined()) " + Double.valueOf(combined) + 
//						", Double.valueOf(sigar.getCpuPerc().getCombined()).floatValue() " + Double.valueOf(combined).floatValue() + 
//						", Double.valueOf(sigar.getCpuPerc().getCombined()).doubleValue() " + Double.valueOf(combined).doubleValue());
				if(combined == Double.NaN) {
					logger.error("CPU is Nan");
					return -1;
				} else if(combined == 0) {
					double idle = cpuPerc.getIdle();
//					logger.debug("CPU combined is 0 and idle is " + idle);
					if(idle > 0) {
						return 0;
					}
				}
				
				
				return Double.valueOf(combined).floatValue();
			} catch (SigarException e) {
				logger.error("Cannot get CurrentCpuLoad", e);
				e.printStackTrace();
			}
			return -1;
		} else {
			final String KERNEL = "KERNEL";
			final String PROCESS = "PROCESS";
			final String IDLE = "IDLE";
			
			long cpuInKernel = 0;
			long cpuInProcess = 0;
			long cpuIdle = 0;
			
			String fileName = 	
				BaseServlet.REAL_PATH + File.separator + "WEB-INF" + File.separator + "classes" + 
				File.separator + "ro" + File.separator + "cst" + File.separator + "tsearch" + 
				File.separator + "AutomaticTester" + File.separator + "dtrace_scripts" + File.separator + "cpu.d";
			String output = executeCommand ("dtrace -s " + fileName);
			if(output==null)
				return -1;
			//System.out.println(output);
			String[] array = output.split("[ \n]+");
			for (int i = 0; i < array.length; i++) {
				if(array[i].equals(KERNEL)){
					cpuInKernel = Long.parseLong(array[++i]);
				} else if(array[i].equals(PROCESS)){
					cpuInProcess = Long.parseLong(array[++i]);
				} else if(array[i].equals(IDLE)){
					cpuIdle = Long.parseLong(array[++i]);
				}
			}
			
			long cpuAll = cpuIdle + cpuInKernel + cpuInProcess;
			
			if(cpuAll==0)
				return 0;
			return (cpuInKernel + cpuInProcess)/((float)cpuAll);
		}
		
	}
	
	private float getCPUUsageLocal() {
		String output = executeCommand ("prstat -t 1 2");
		String[] lines = output.split("\n");
		String[] cols = null;
		String cpu = "";
		float cpuUsage = 0;
		int times = 0;
		for (int i = 1; i < lines.length; i++) {
			cols = lines[i].split("[ ]+");
			if(cols[0].equals("Total:")){
				times++;
				continue;
			}
			if(cols[0].equals("NPROC"))
				continue;
			cpu = cols[cols.length-1];
			try {
				cpuUsage += Float.parseFloat(cpu.replaceAll("%", ""));
			} catch (Exception e) {
			}
		}
		cpuUsage/=100;
		if(times==0)
			return cpuUsage;
		return cpuUsage/times;
	}


	private float getMemoryUsage(Sigar sigar, HashMap<String, Long> memHash) {
		
		if(sigar != null) {
			try {
				memHash.clear();
				
				Mem mem = sigar.getMem();
				memHash.put(RAMTotal, mem.getTotal() / MB_RATIO);
				memHash.put(RAMFree, mem.getActualFree() / MB_RATIO);
				
				return Double.valueOf(mem.getUsedPercent()/100).floatValue();
				
			} catch (SigarException e) {
				logger.error("Cannot get CurrentCpuLoad", e);
				e.printStackTrace();
			}
			return 0;
		} else {
		
			long ramTotal = 0;
			long ramFree = 0;
			memHash.clear();
			
			String fileName = 	
				BaseServlet.REAL_PATH + File.separator + "WEB-INF" + File.separator + "classes" + 
				File.separator + "ro" + File.separator + "cst" + File.separator + "tsearch" + 
				File.separator + "AutomaticTester" + File.separator + "dtrace_scripts" + 
				File.separator + "memory.d";
			String output = executeCommand ("dtrace -s " + fileName);
			if(output==null) {
				logger.error("getMemoryUsage failed when running dtrace -s " + fileName);
				return -1;
			}
			//System.out.println(output);
			
			String[] array = output.split("[ \n]+");
			for (int i = 0; i < array.length; i++) {
				if(array[i].equals(RAMTotal)){
					ramTotal = Long.parseLong(array[++i]);
					memHash.put(RAMTotal, ramTotal);
				} else if(array[i].equals(RAMFree)){
					ramFree = Long.parseLong(array[++i]);
					memHash.put(RAMFree, ramFree);
				}
			}
			return (ramTotal-ramFree)/((float)ramTotal);
		
		}
	}
	

	private float getNetworkUsage(Sigar sigar) {
		
		if(sigar != null) {
			
			try {
				NetworkData networkData = new NetworkData(sigar);
				Long[] m = networkData.getMetric();
				long totalrx = m[0];
				long totaltx = m[1];
				
				return (totalrx + totaltx) / (float)KB_RATIO;
			} catch (Exception e) {
				logger.error("Exception while getting network data", e);
				e.printStackTrace();
			}
            return -1;
		} else {
			final String ALLNet = "allnet";
			
			//this values will be in KBytes
			float allnet = 0;
			
			String fileName = 	
				BaseServlet.REAL_PATH + File.separator + "WEB-INF" + File.separator + "classes" + 
				File.separator + "ro" + File.separator + "cst" + File.separator + "tsearch" + 
				File.separator + "AutomaticTester" + File.separator + "dtrace_scripts" + 
				File.separator + "network.d";
			String output = executeCommand ("perl " + fileName + " 1 2");
			if(output==null)
				return -1;
			//System.out.println(output);
			
			String[] array = output.split("[ \n]+");
			for (int i = 0; i < array.length; i++) {
				if(array[i].equals(ALLNet)){
					try {
						allnet = Float.parseFloat(array[++i]);
					} catch(Exception e){
						e.printStackTrace();
					}
				}
			}
			return allnet;
		}
	}
	
	

	private String executeCommand(String command) {
		
        String[] commandArray = command.split(" ");
        String output = null;
        
        try {
        	ClientProcessExecutor cpe = new ClientProcessExecutor(commandArray,true,true);
        	cpe.start();
        	output = cpe.getErrorOutput();
        	/*if(command.contains("cpu")){
        		System.err.println("Command: " + command);
        		System.err.println("Err" + output);
        	}*/
        	//if(output==null || output.length()==0)	//if no err output
        		output = cpe.getCommandOutput();	//get normal output
    		/*if(command.contains("cpu"))
        		System.err.println("Output: " + output);*/
        	if(output==null || output.length()==0)
        		return null;
        	return output;
        } catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}


	@SuppressWarnings("rawtypes")
	private int getNumberOfSessions() {
		Collection users = User.getActiveUsers().values();
		int loggedUsers = 0;
    	for (Iterator iterator = users.iterator(); iterator.hasNext();)
    	{ 			
    		Object item = iterator.next();
    		if (item instanceof User)
    		{
				loggedUsers++;
   			}
        }
		return loggedUsers;
	}


	private int getNumberOfDBConnections() {
		return ConnectionPool.getInstance().getConnectionsInUse();
	}
	
	public static Category getLogger() {
		return logger;
	}
	
	
}

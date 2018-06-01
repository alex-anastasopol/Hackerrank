package ro.cst.tsearch.servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManagerMonitor;
import ro.cst.tsearch.MailReader.FTPReaderDaemon;
import ro.cst.tsearch.MailReader.MailOrderDaemon;
import ro.cst.tsearch.data.MaintenanceMessage;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBSearch;
import ro.cst.tsearch.monitor.SNMPAgent;
import ro.cst.tsearch.threads.CommAdminNotifier;
import ro.cst.tsearch.threads.DeleteContext;
import ro.cst.tsearch.threads.DeleteErrorRequest;
import ro.cst.tsearch.threads.DeleteLocalContext;
import ro.cst.tsearch.threads.DeleteOCRFiles;
import ro.cst.tsearch.threads.DiskInformation;
import ro.cst.tsearch.threads.FVSRunnerDaemon;
import ro.cst.tsearch.threads.GenericCountyRecorderRO;
import ro.cst.tsearch.threads.LoadAverageDeamon;
import ro.cst.tsearch.threads.LoadInformation;
import ro.cst.tsearch.threads.MonitoringFolderService;
import ro.cst.tsearch.threads.MonitoringService;
import ro.cst.tsearch.threads.RefreshService;
import ro.cst.tsearch.threads.ReplicationMonitor;
import ro.cst.tsearch.threads.deadlock.DefaultDeadlockListener;
import ro.cst.tsearch.threads.deadlock.ServerInfoDeadlockListener;
import ro.cst.tsearch.threads.deadlock.ThreadDeadlockDetector;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.archive.ArchiveService;
import com.stewart.ats.filereplication.FileReplicationManager;
import com.stewart.ats.user.UserService;
import com.stewart.datatree.DataTreeManager;

/**
 * @author elmarie
 */

public class InitContext extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final Category logger = Logger.getLogger(InitContext.class);
	public static long serverStartTime = 0;
    
    public  void init() {
    	
    	serverStartTime = System.currentTimeMillis();
    	try { initLog4J(); } catch (Exception e) {e.printStackTrace();}
    	logger.info("InitContext: initLog4J() took " + (System.currentTimeMillis() - serverStartTime));
    	
    	long startCountTime = System.currentTimeMillis();
    	BaseServlet.REAL_PATH = getServletContext().getRealPath("/");
    	logger.info("InitContext: BaseServlet.REAL_PATH took " + (System.currentTimeMillis() - startCountTime));

		startCountTime = System.currentTimeMillis();
		try { initConnectionPool(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: initConnectionPool() took " + (System.currentTimeMillis() - startCountTime));
		
		startCountTime = System.currentTimeMillis();
		DataTreeManager.init();
		logger.info("InitContext: DataTreeManager.init() took " + (System.currentTimeMillis() - startCountTime));
		
		startCountTime = System.currentTimeMillis();
		try { SearchManagerMonitor.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: SearchManagerMonitor.init() took " + (System.currentTimeMillis() - startCountTime));

		startCountTime = System.currentTimeMillis();
		try { SNMPAgent.initSNMP(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: SNMPAgent.initSNMP() took " + (System.currentTimeMillis() - startCountTime));

		startCountTime = System.currentTimeMillis();
		try { DeleteContext.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: DeleteContext.init()() took " + (System.currentTimeMillis() - startCountTime));

		startCountTime = System.currentTimeMillis();
		try { DeleteLocalContext.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: DeleteLocalContext.init() took " + (System.currentTimeMillis() - startCountTime));
		
		//starts the thread that reads current load information data
		startCountTime = System.currentTimeMillis();
		try { LoadInformation.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: LoadInformation.init() took " + (System.currentTimeMillis() - startCountTime));
		
		//starts the thread that reads current disk status
		startCountTime = System.currentTimeMillis();
		try { DiskInformation.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: DiskInformation.init() took " + (System.currentTimeMillis() - startCountTime));
		
		//starts the thread that will wait for request from the LB application and tell current server status
		startCountTime = System.currentTimeMillis();
		try { MonitoringService.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: MonitoringService.init() took " + (System.currentTimeMillis() - startCountTime));
		
		//starts the thread that precalculates information for System Availability Graphics
		startCountTime = System.currentTimeMillis();
		try { LoadAverageDeamon.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: LoadAverageDeamon.init() took " + (System.currentTimeMillis() - startCountTime));
		
		//starts the thread that caches the search context on local hard disk
		startCountTime = System.currentTimeMillis();
		try { ArchiveService.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: ArchiveService.init() took " + (System.currentTimeMillis() - startCountTime));
		
		//starts the thread that looks for expired passwords and send notifications
		startCountTime = System.currentTimeMillis();
		try { UserService.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: UserService.init() took " + (System.currentTimeMillis() - startCountTime));
		
		startCountTime = System.currentTimeMillis();
		try { MaintenanceMessage.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: MaintenanceMessage.init() took " + (System.currentTimeMillis() - startCountTime));

		startCountTime = System.currentTimeMillis();
		try { CommAdminNotifier.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: CommAdminNotifier.init() took " + (System.currentTimeMillis() - startCountTime));

		startCountTime = System.currentTimeMillis();
		try { Search.initSitesConfiguration();} catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: Search.initSitesConfiguration() took " + (System.currentTimeMillis() - startCountTime));
		
		//cleans database for dummy searches
		startCountTime = System.currentTimeMillis();
		try { DBSearch.deleteSearchByStatus(DBConstants.SEARCH_NOT_SAVED, 0, 30);} catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: DBSearch.deleteSearchByStatus() took " + (System.currentTimeMillis() - startCountTime));
		
		//starts the thread that deletes the logs of DASL XML request/responses that failed
		startCountTime = System.currentTimeMillis();
		try { 
			DeleteErrorRequest.init(); 		//let's not create a new thread for now, since this is not that important 
			//ErrorRequestBean.deleteEntriesOlderThan(30);
		} catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: DeleteErrorRequest.init() took " + (System.currentTimeMillis() - startCountTime));
		
		/*
		//moved to DeleteOCRFiles.init();
		try {
			OcrFileMapper.deleteEntriesOlderThan(ServerConfig.getOCRDeleteDatabaseDays());
		} catch (Exception e) {e.printStackTrace();}
		*/

		startCountTime = System.currentTimeMillis();
		try { DeleteOCRFiles.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: DeleteOCRFiles.init() took " + (System.currentTimeMillis() - startCountTime));
		
		//starts the thread that monitors replication and send email if replication fail + tries to start it
		startCountTime = System.currentTimeMillis();
		try { ReplicationMonitor.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: ReplicationMonitor.init() took " + (System.currentTimeMillis() - startCountTime));
		
		//starts the thread that refreshes some caches
		startCountTime = System.currentTimeMillis();
		try { RefreshService.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: RefreshService.init() took " + (System.currentTimeMillis() - startCountTime));
		
		//starts file replication tasks... Getter, Setter and fileStatus manager
		startCountTime = System.currentTimeMillis();
		try { FileReplicationManager.getManager(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: FileReplicationManager.getManager() took " + (System.currentTimeMillis() - startCountTime));

		startCountTime = System.currentTimeMillis();
		try {
			ThreadDeadlockDetector deadlockDetector = new ThreadDeadlockDetector(120000);
			deadlockDetector.addListener(new DefaultDeadlockListener());
			deadlockDetector.addListener(new ServerInfoDeadlockListener());
		} catch (Exception e) {
			logger.error("Exception while setting thread detector", e);
		}
		logger.info("InitContext: ThreadDeadlockDetector() took " + (System.currentTimeMillis() - startCountTime));
		
//		try { TDIReaderDeamon.initAndStartDeamon(); } catch (Exception e) {e.printStackTrace();}

		startCountTime = System.currentTimeMillis();
		try { FTPReaderDaemon.initAndStartDeamons(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: FTPReaderDaemon.initAndStartDeamons() took " + (System.currentTimeMillis() - startCountTime));
		
		startCountTime = System.currentTimeMillis();
		try { FVSRunnerDaemon.initAndStartDaemons(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: FVSRunnerDaemon.initAndStartDaemons() took " + (System.currentTimeMillis() - startCountTime));
		
		startCountTime = System.currentTimeMillis();
		try { GenericCountyRecorderRO.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: GenericCountyRecorderRO.init() took " + (System.currentTimeMillis() - startCountTime));
		
		startCountTime = System.currentTimeMillis();
		try { MonitoringFolderService.init(); } catch (Exception e) {e.printStackTrace();}
		logger.info("InitContext: MonitoringFolderService.init() took " + (System.currentTimeMillis() - startCountTime));
		
		startCountTime = System.currentTimeMillis();
		try {
			MailOrderDaemon.main(new String[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("InitContext: MailOrderDaemon.main() took " + (System.currentTimeMillis() - startCountTime));
		
		logger.info("InitContext: All took " + (System.currentTimeMillis() - serverStartTime));
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) {}
	
	private void initLog4J(){
		String prefix =  getServletContext().getRealPath("/");
		String path = getInitParameter("log4j-init-file-path");
		// if the log4j-init-file is not set, then no point in trying
		if(path != null) {
			PropertyConfigurator.configureAndWatch(prefix+path+URLMaping.LOG4J_CONFIG,5000);
			logger.info("Log4J system started with configuration file = " + prefix+path+URLMaping.LOG4J_CONFIG);
		} else {
			logger.info("Log4j file is null");
		}
	
	}
	
	private void initConnectionPool() {
		ConnectionPool.getInstance();
	}
}


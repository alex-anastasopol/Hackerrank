package ro.cst.tsearch.threads;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.loadBalServ.ServerInfoSingleton;
import ro.cst.tsearch.loadBalServ.ServerInfoSingleton.SaveStackThread;

/**
 * Designed to monitor a folder and based on certain files it will trigger actions from the same JVM context
 * 
 * @author Andrei
 *
 */
public class MonitoringFolderService extends TimerTask {

	public static void init() {
		
		MonitoringFolderService monitoringFolderService = new MonitoringFolderService();
		
		Timer timerNow = new Timer("MonitoringFolderService", true);
   	 	timerNow.schedule((TimerTask) monitoringFolderService, 60*1000, 60*1000);	//run task in 1 minute
	}
	
	@Override
	public void run() {
		if(ServerConfig.isMonitoredFolderEnable() && StringUtils.isNotBlank(ServerConfig.getMonitoredFolder())) {
			File folder = new File(ServerConfig.getMonitoredFolder());
			if(folder.isDirectory()) {
				File stackTrigger = new File(folder, "stack");
				if(stackTrigger.isFile()) {
					FileUtils.deleteQuietly(stackTrigger);
					
					SaveStackThread saveStackThread = ServerInfoSingleton.getInstance().saveThreadStack(0, 1);
					try {
						saveStackThread.join();
						
						String stackTraceContent = saveStackThread.getStackTraceContent().toString();
						
						FileUtils.writeStringToFile(new File(folder, "result_stack_" + new SimpleDateFormat("MMddyyyy_HHmmss").format(new Date()) + ".txt"), stackTraceContent);
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
				
				File killThreadFile = new File(folder, "killThread.txt");
				if(killThreadFile.isFile()) {
					
					File killThreadResult = new File(folder, "result_kill_thread_" + new SimpleDateFormat("MMddyyyy_HHmmss").format(new Date()) + ".txt");					
					try {
						String killThreadName = FileUtils.readFileToString(killThreadFile);
						
						try {
							FileUtils.writeStringToFile(killThreadResult, "Trying to kill thread: " + killThreadName + "\n\n", true);
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();

					 	while (root.getParent() != null) {
					 		root = root.getParent();
					 	}
					 	
					 	visit(root, 0, killThreadName, killThreadResult);
					 	
						
					} catch (Exception e) {
						e.printStackTrace();
						
						try {
							FileUtils.writeStringToFile(killThreadResult, e.getMessage() + "\n\n", true);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						
					}
					
					FileUtils.deleteQuietly(killThreadFile);
					
					
					
				}
			}
		}

	}
	
	public static void visit(ThreadGroup group, int level, String threadName, File killThreadResultFile) {
		// Get threads in `group'
		int numThreads = group.activeCount();
		Thread[] threads = new Thread[numThreads * 2];
		numThreads = group.enumerate(threads, false);

		// Enumerate each thread in `group'
		for (int i = 0; i < numThreads; i++) {
			// Get thread
			Thread thread = threads[i];
			try {
				FileUtils.writeStringToFile(killThreadResultFile, "Thread found_____________: " + thread.getName() + "\n", true);
			} catch (Exception e) {
			}
			if (threadName.equalsIgnoreCase(thread.getName())) {
				System.err.println("Will interrupt: " + thread.getName());
				try {
					FileUtils.writeStringToFile(killThreadResultFile, "I got you _____________: " + thread.getName() + "\n", true);
				} catch (Exception e) {
				}
				try {
					thread.interrupt();
					thread.stop();
					FileUtils.writeStringToFile(killThreadResultFile, "Thread isInterrupted_____________: " + thread.getName() + "\n", true);
					System.err.println("Thread isInterrupted: " + thread.isInterrupted());
					break;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// Get thread subgroups of `group'
		int numGroups = group.activeGroupCount();
		ThreadGroup[] groups = new ThreadGroup[numGroups * 2];
		numGroups = group.enumerate(groups, false);

		// Recursively visit each subgroup
		for (int i = 0; i < numGroups; i++) {
			visit(groups[i], level + 1, threadName, killThreadResultFile);
		}
	}

}

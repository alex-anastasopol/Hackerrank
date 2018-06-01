package ro.cst.tsearch.threads.deadlock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class DefaultDeadlockListener implements ThreadDeadlockDetector.Listener {
	
	private final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
	
	public void deadlockDetected(long[] threads) {
		System.err.println("Deadlocked Threads:");
		System.err.println("-------------------");
		
		ThreadInfo[] threadInfos = mbean.getThreadInfo(threads, true, true);
		for (ThreadInfo threadInfo : threadInfos) {
			System.err.println(threadInfo);
		}
		
		
	}
}

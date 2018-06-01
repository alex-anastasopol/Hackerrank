package ro.cst.tsearch.threads.deadlock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;

import ro.cst.tsearch.loadBalServ.ServerInfoSingleton;

/**
 * Check it out
 * http://www.javaspecialists.eu/archive/Issue130.html
 * and
 * http://www.javaspecialists.eu/archive/Issue160.html
 * @author AndreiA
 *
 */
public class ThreadDeadlockDetector {
	private final Timer threadCheck = new Timer("ThreadDeadlockDetector", true);
	private static final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
	private final Collection<Listener> listeners = new CopyOnWriteArraySet<Listener>();

	/**
	 * The number of milliseconds between checking for deadlocks. It may be
	 * expensive to check for deadlocks, and it is not critical to know so
	 * quickly.
	 */
	private static final int DEFAULT_DEADLOCK_CHECK_PERIOD = 10000;

	public ThreadDeadlockDetector() {
		this(DEFAULT_DEADLOCK_CHECK_PERIOD);
	}

	public ThreadDeadlockDetector(int deadlockCheckPeriod) {
		threadCheck.schedule(new TimerTask() {
			public void run() {
				checkForDeadlocks();
			}
		}, 10, deadlockCheckPeriod);
	}

	private void checkForDeadlocks() {
		long[] ids = findDeadlockedThreads();
		if (ids != null && ids.length > 0) {
			fireDeadlockDetected(ids);
		} else {
			ServerInfoSingleton.getInstance().signalDeadlock(null);
		}
	}

	private long[] findDeadlockedThreads() {
		// JDK 1.5 only supports the findMonitorDeadlockedThreads()
		// method, so you need to comment out the following three lines
		if (mbean.isSynchronizerUsageSupported())
			return mbean.findDeadlockedThreads();
		else
			return mbean.findMonitorDeadlockedThreads();
	}

	private void fireDeadlockDetected(long[] threads) {
		for (Listener l : listeners) {
			l.deadlockDetected(threads);
		}
	}

	
	@SuppressWarnings("unused")
	private Thread findMatchingThread(ThreadInfo inf) {
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.getId() == inf.getThreadId()) {
				return thread;
			}
		}
		throw new IllegalStateException("Deadlocked Thread not found");
	}

	public boolean addListener(Listener l) {
		return listeners.add(l);
	}

	public boolean removeListener(Listener l) {
		return listeners.remove(l);
	}

	/**
	 * This is called whenever a problem with threads is detected.
	 */
	public interface Listener {
		void deadlockDetected(long[] deadlockedThreads);
	}

	public static ThreadMXBean getMbean() {
		return mbean;
	}
	
	
}

package ro.cst.tsearch.threads.deadlock;

import ro.cst.tsearch.loadBalServ.ServerInfoSingleton;
import ro.cst.tsearch.threads.deadlock.ThreadDeadlockDetector.Listener;

public class ServerInfoDeadlockListener implements Listener {

	@Override
	public void deadlockDetected(long[] deadlockedThreads) {
		if(deadlockedThreads.length > 0) {
			ServerInfoSingleton.getInstance().signalDeadlock(deadlockedThreads);
		}

	}

}

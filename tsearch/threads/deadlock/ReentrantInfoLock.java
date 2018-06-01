package ro.cst.tsearch.threads.deadlock;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantInfoLock extends ReentrantLock {

	private static final long serialVersionUID = 947731188626383947L;

	public ReentrantInfoLock() {
		super();
	}

	public ReentrantInfoLock(boolean fair) {
		super(fair);
	}

	@Override
	public Thread getOwner() {
		return super.getOwner();
	}

	@Override
	public Collection<Thread> getQueuedThreads() {
		return super.getQueuedThreads();
	}

}

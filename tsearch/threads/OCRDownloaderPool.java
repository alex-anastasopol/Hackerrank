package ro.cst.tsearch.threads;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class OCRDownloaderPool {
	
	private ThreadPoolExecutor pool = null;
	
	private final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(
            50);
	
	private OCRDownloaderPool() {
		pool = new ThreadPoolExecutor(20, 40, 10, TimeUnit.SECONDS, queue);
	}
	
	private static class SingletonHolder {
		private static OCRDownloaderPool instance = new OCRDownloaderPool();
	}
	
	public static OCRDownloaderPool getInstance(){
		return SingletonHolder.instance;
	}
	
	public void runTask(Runnable task)
    {
		pool.execute(task);
    }

}

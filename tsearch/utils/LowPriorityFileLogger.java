package ro.cst.tsearch.utils;

import java.util.concurrent.ArrayBlockingQueue;
import ro.cst.tsearch.utils.FileLogger;

/**
 * A class offering low priority string logging.
 * If the internal queue is full, then the strings are dropped.
 * A low priority thread extracts elements from queue and writes them to the file.
 *
 * @author  Radu Bacrau
 */
public class LowPriorityFileLogger implements Runnable {
	private ArrayBlockingQueue<String> stringQueue;
	private String fileName;

    /**
     * Constructor - it opens the output file and also creates and 
     * starts the low priority daemon thread that writes to the file
     *
     * @param     fileName  the name of the file in which the log will be written
     * @param     maxCapacity  maximum elements to be held in the queue
     * @return    nothing
     */	
	public LowPriorityFileLogger(String paramFileName, int maxCapacity){
		stringQueue = new ArrayBlockingQueue<String>(maxCapacity);
		fileName = paramFileName;
		FileLogger.startLog(fileName);
		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);		
		thread.start();
	}

    /**
     * Logs a string 
     *
     * @param     message  the string to be logged
     * @return    nothing
     */	
	public void logString(String message){
		stringQueue.offer(message);
	}
	
    /**
     * low priority thread that writes the strings to the file 
     * @return    nothing
     */		
	public void run (){
		while(true){
			try{
				String element = stringQueue.take();
				FileLogger.info(element, fileName);
			}catch(InterruptedException ignored){
				
			}
		}
	}
}
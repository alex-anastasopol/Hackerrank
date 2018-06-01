package ro.cst.tsearch.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * Logs a message to a file
 * @author radu bacrau
 */
public class SimpleLogger {
	
	private static final Logger logger = Logger.getLogger(SimpleLogger.class);
	
	private String fileName = "";
	private boolean outputToConsole = false;
	
	/**
	 * Create a simple logger
	 * @param fileName
	 * @param append
	 * @throws IOException
	 */
	public SimpleLogger(String fileName, boolean append, boolean outputToConsole) throws IOException {		

		// copy parameters
		this.fileName = fileName;
		this.outputToConsole = outputToConsole;
		
		// delete the file and create it empty if instructed so
		File file = new File(fileName);
		if(!append && file.exists()){
			file.delete();
			file.createNewFile();
		}			
	}
	
	/**
	 * Log a message
	 * @param messge
	 */
	public void log(String messge) {
		PrintWriter pw = null;
		try{
			Date d = new Date(System.currentTimeMillis());
			messge = (new SimpleDateFormat("MM/dd/yyyy hh:mm:ssa").format(d)) + " : " + messge;
			pw = new PrintWriter(new FileOutputStream(fileName, true), true);
			pw.println(messge);			
		}catch(Exception e){
			e.printStackTrace();
			logger.error(e);
		} finally {
			if(pw != null){
				pw.close();
			}
		}
		if(outputToConsole){
			System.out.println(messge);
		}
	}
}

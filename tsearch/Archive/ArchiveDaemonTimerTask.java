package ro.cst.tsearch.Archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.log4j.Logger;

import ro.cst.tsearch.data.SearchToArchive;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.URLMaping;

public class ArchiveDaemonTimerTask extends TimerTask{

	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	private static final Logger logger = Logger.getLogger(ArchiveDaemonTimerTask.class);

	private int ageOfFiles = 100;
	/**
	 * @param age
	 */
	public ArchiveDaemonTimerTask(int age) {
		ageOfFiles = age;
	}
	
	public void run() {
		
		String contextPath= ArchiveDaemon.getREAL_PATH();

		String sourcePath = contextPath + "TSD/";
		String destPath = contextPath + "Archive/";
		String filePath, fileLink;
		int ii;
		File sf, df; 
		Vector<SearchToArchive> moved = new Vector<SearchToArchive>();

		//setting the date to which the tsrs to be archived 
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, 1 - ageOfFiles);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);

		//getting the tsrs to be moved to archive 
		SearchToArchive[] sta = DBManager.getSearchesToArchive(c.getTime().getTime());
		for (int i = 0; i < sta.length; i++){
			//extracting the tsr path
			logger.info("Processing " + sta[i]);
			fileLink = sta[i].getTSRlink();
			if (fileLink == null)
				fileLink = "";
			ii = fileLink.indexOf("TSD/");
			if (ii != -1){
				filePath = sourcePath + fileLink.substring(ii + 4);
				//moving the tsr to archive folder
				sf = new File (filePath);
				df = new File (destPath + sta[i].getTSRname());
				if (move(sf,df))
					if (df.isFile()){
						//modifying the link in reports to use archive
						sta[i].setTSRlink("javascript: getFromArchive('" + sta[i].getTSRname() + "');");
						moved.add(sta[i]);}
					else {
						logger.error("Could not found moved file: " + df.getAbsolutePath());
					}
				else {
					logger.error("Could not move file: " + sf + " to " + df);
				}
			}
			else {
				logger.error("Could not open file: " + fileLink);
				continue;
			} 
		}
		//changing status for archived searches
		DBManager.updateArchivedSearches(moved);
	}
	
	public static boolean move(File source, File dest) {
		 FileChannel in = null, out = null;
		 boolean ok = true;
		 try {          
			  in = new FileInputStream(source).getChannel();
			  out = new FileOutputStream(dest).getChannel();
			  MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
			  out.write(buf);
		 } catch(IOException ioe) {
		 	logger.error("IO Exception raised when copy source file =" 
		 		+ source.getAbsolutePath() + " to dest file =" + dest,ioe);
		 	ok = false; 
		 } finally {
		 	try {
				if (in != null)      in.close();
				if (out != null)     out.close();		 		
		 	} catch(IOException ioe) {
				logger.error("IO Exception raised when copy source file =" 
					+ source.getAbsolutePath() + " to dest file =" + dest,ioe);
		 		ok = false;
		 	}
		 }
		 return ok;
	}

}

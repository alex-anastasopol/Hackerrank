package ro.cst.tsearch.threads;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

public class DiskInformation extends TimerTask {

	private static final Category logger = Logger.getLogger(DiskInformation.class);
	
	private static HashMap<String, String> diskInfo = new HashMap<String, String>();
	
	private static float MB_RATIO = 1024 * 1024;
	private static float GB_RATIO = MB_RATIO * 1024;
	
	private static final long TIMER_DELAY		= 1000; 			//1 second
	private String diskNames;
	private String diskMaxSizes;
	
	public DiskInformation(){
		
	}
	public DiskInformation(String disk1, String diskMaxSizes){
		diskNames = disk1;
		this.diskMaxSizes = diskMaxSizes;
	}
	
	public static String getMaxDiskSize(String folderName){
		String max = diskInfo.get(folderName);
		if(max==null)
			return "unknown";
		return max;
	}
	
	public static void init() {
		
		boolean start = ServerConfig.isDiskInformationEnable(true);
        if(!start){
        	logger.error("load.information.disk.enable=false");
        	return;
        }
        
        long samplePeriod = 30;				//default is 30 minutes
        try {
        	samplePeriod = Long.parseLong(ServerConfig.getDiskInformationSampleInterval("30"));
        	
        } catch (Exception e){
        	logger.error("ERROR: Using default values");
        	e.printStackTrace();
        }
        samplePeriod*= 60*1000;		//we must translate seconds into millis
        
        String disk1 = ServerConfig.getDiskInformationFolders("");
        String diskMaxSizes = ServerConfig.getDiskInformationMaxSizes("");
        
		DiskInformation diskInfo = new DiskInformation(disk1, diskMaxSizes);
		
		Timer timerNow = new Timer();
   	 	timerNow.schedule((TimerTask) diskInfo, TIMER_DELAY, samplePeriod);

	}
	
	
	public void run() {
		logger.debug("Starting DiskInformation sequence...");
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		
		LinkedHashMap<String, DiskInfoBean> diskComputedData = getDiskUsageAndSendNotifications(diskNames, diskMaxSizes);
		if(diskComputedData.isEmpty()){
			logger.error("Skipping this turn ! Disk data not available!");
			return;
		}
		String sql = "INSERT INTO " + DBConstants.TABLE_USAGE_DISK + 
			" (DISK1, TIMESTAMP, SERVER_NAME, DISK1_NAME, type, disk1_max_value) VALUES ";
		for (String folderName : diskComputedData.keySet()) {
			DiskInfoBean diskInfoBean = diskComputedData.get(folderName);
			sql += "( " + 
				diskInfoBean.getDiskCapacityUsed() + ", now() , \"" + 
				URLMaping.INSTANCE_DIR + "\", \"" + 
				folderName + "\", 1, " + diskInfoBean.getDiskMaxSize() + "), ";
		}
		sql = sql.substring(0, sql.length() - 2);
		
		sjt.update(sql);
		
		/*
		
		TO BE Moved and corrected because something is wrong with the data computed
		
		String sqlSelectInfoForMonth = 
			" SELECT " + 
			" avg(" + DBConstants.FIELD_USAGE_DISK_DISK1 + ") " + DBConstants.FIELD_USAGE_DISK_DISK1 + ", " +
			" str_to_date(concat(" +
				"day(" + DBConstants.FIELD_USAGE_DISK_TIMESTAMP + "),'-'," +
				"month(" + DBConstants.FIELD_USAGE_DISK_TIMESTAMP + "),'-'," +
				"year(" + DBConstants.FIELD_USAGE_DISK_TIMESTAMP + "),' '," +
				"floor(hour(" + DBConstants.FIELD_USAGE_DISK_TIMESTAMP + ")/?)*?,':00:00'),\"%e-%c-%Y %H:%i:%S\") " + 
				DBConstants.FIELD_USAGE_DISK_TIMESTAMP + ", " +
			DBConstants.FIELD_USAGE_DISK_NAME + ", " + 
			DBConstants.FIELD_USAGE_DISK_MAX_VALUE + ", " + DBConstants.FIELD_USAGE_DISK_TYPE +
			" FROM " + DBConstants.TABLE_USAGE_DISK  +
			" where date_add(timestamp, Interval 1 month) > (now()) " +
			" and " + DBConstants.FIELD_USAGE_DISK_TYPE + " = 1 " +
			" and " + DBConstants.FIELD_USAGE_DISK_SERVER_NAME + " = '" + URLMaping.INSTANCE_DIR + "' " +
			" group by " + 
			DBConstants.FIELD_USAGE_DISK_NAME + ", year(timestamp), month(timestamp), day(timestamp), floor(hour(timestamp)/?) ";
		
		List<DiskInfoMapper> listForInsert = sjt.query(sqlSelectInfoForMonth, new DiskInfoMapper(), 4, 4, 4);
		
		String sqlInsertInfoForMonth =
			"INSERT INTO " + DBConstants.TABLE_USAGE_DISK + 
			"(DISK1, TIMESTAMP, SERVER_NAME, DISK1_NAME, type, disk1_max_value) VALUES (?,?,?,?,?,?) " + 
			" ON duplicate KEY UPDATE " + 
				DBConstants.FIELD_USAGE_DISK_DISK1 + " =? ";
		
		for (DiskInfoMapper diskInfoMapper : listForInsert) {
			sjt.update(sqlInsertInfoForMonth, 
					diskInfoMapper.getDiskPercent(),
					diskInfoMapper.getTimestamp(),
					URLMaping.INSTANCE_DIR,
					diskInfoMapper.getDiskName(),
					2,
					diskInfoMapper.getDiskMaxSpace(),
					diskInfoMapper.getDiskPercent()
					);
		}
		
		String sqlSelectInfoForYear = 
			" SELECT " + 
			" avg(" + DBConstants.FIELD_USAGE_DISK_DISK1 + ") " + DBConstants.FIELD_USAGE_DISK_DISK1 + ", " +
			" str_to_date(concat(" +
				"day(" + DBConstants.FIELD_USAGE_DISK_TIMESTAMP + "),'-'," +
				"month(" + DBConstants.FIELD_USAGE_DISK_TIMESTAMP + "),'-'," +
				"year(" + DBConstants.FIELD_USAGE_DISK_TIMESTAMP + "),' 00:00:00'),\"%e-%c-%Y %H:%i:%S\") " + 
				DBConstants.FIELD_USAGE_DISK_TIMESTAMP + ", " +
			DBConstants.FIELD_USAGE_DISK_NAME + ", " + 
			DBConstants.FIELD_USAGE_DISK_MAX_VALUE + ", " + DBConstants.FIELD_USAGE_DISK_TYPE +
			" FROM " + DBConstants.TABLE_USAGE_DISK  +
			" where date_add(timestamp, Interval 1 year) > (now()) " +
			" and " + DBConstants.FIELD_USAGE_DISK_TYPE + " = 2 " +
			" and " + DBConstants.FIELD_USAGE_DISK_SERVER_NAME + " = '" + URLMaping.INSTANCE_DIR + "' " +
			" group by " + 
			DBConstants.FIELD_USAGE_DISK_NAME + ", year(timestamp), month(timestamp), day(timestamp) ";
		
		listForInsert = sjt.query(sqlSelectInfoForYear, new DiskInfoMapper());
		
		for (DiskInfoMapper diskInfoMapper : listForInsert) {
			sjt.update(sqlInsertInfoForMonth, 
					diskInfoMapper.getDiskPercent(),
					diskInfoMapper.getTimestamp(),
					URLMaping.INSTANCE_DIR,
					diskInfoMapper.getDiskName(),
					3,
					diskInfoMapper.getDiskMaxSpace(),
					diskInfoMapper.getDiskPercent()
					);
		}
		*/
	}

	private LinkedHashMap<String, DiskInfoBean> getDiskUsageAndSendNotifications(String folderNames, String folderMaxSizes) {
//		String output = executeCommand("df -h ");
//		String[] array = output.split("[ \n\t]+");
		LinkedHashMap<String, DiskInfoBean> results = new LinkedHashMap<String, DiskInfoBean>();
		String[] folders = folderNames.split("\\s*;\\s*");
		String[] foldersMaxSize = folderMaxSizes.split("\\s*;\\s*");
		
		if(folders.length != foldersMaxSize.length) {
			return results;
		}

		for (int folderIndex = 0; folderIndex < folders.length; folderIndex++) {
			String folderName = folders[folderIndex];
			
			File file = new File(folderName);
			long totalSpace = file.getTotalSpace();
			long freeSpace = file.getUsableSpace();
			long usedSpace = totalSpace - freeSpace;
			
			if(totalSpace == 0l) {
				logger.error("Cannot read totalSpace for " + folderName);
				continue;
			}
			
			DiskInfoBean diskInfoBean = new DiskInfoBean();
			diskInfoBean.setDiskCapacityUsed((totalSpace - freeSpace + 0f) * 100 / totalSpace);
			diskInfoBean.setDiskMaxSize(totalSpace / MB_RATIO);
			
			try {
				
				
				if(usedSpace / MB_RATIO > Float.parseFloat(foldersMaxSize[folderIndex])) {
					//must send notification since available
					String notificationEmails = MailConfig.getMailDiskNotificationEmail();
					if(StringUtils.isNotEmpty(notificationEmails)) {
						EmailClient email = new EmailClient();
						email.addTo(notificationEmails);
						email.setSubject("Disk " + folderName + " almost full on " + URLMaping.INSTANCE_DIR);
						DecimalFormat decimalFormat = new DecimalFormat("##.00");
						email.addContent(folderName + " is " + decimalFormat.format(usedSpace / GB_RATIO) + " GB full which means we only have " + decimalFormat.format(freeSpace / GB_RATIO) + " GB free space left on device");
						email.sendNow();
					}
				}
			} catch (Exception e) {
				logger.error("Error while tring to send notification email for size excedeed for folder " + folderName, e);
			}
			results.put(folderName, diskInfoBean);
			
			
		}
		
		return results;
	}

//	private String executeCommand(String command) {
//		
//        String[] commandArray = command.split(" ");
//        String output = null;
//        
//        try {
//        	ClientProcessExecutor cpe = new ClientProcessExecutor(commandArray,true,true);
//        	cpe.start();
//        	output = cpe.getErrorOutput();
//        	if(output==null || output.length()==0)	//if no err output
//        		output = cpe.getCommandOutput();	//get normal output
//        	return output;
//        } catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return null;
//	}
	
	class DiskInfoBean {
		private float diskCapacityUsed;
		private float diskMaxSize;
		public float getDiskCapacityUsed() {
			return diskCapacityUsed;
		}
		public void setDiskCapacityUsed(float diskCapacityUsed) {
			this.diskCapacityUsed = diskCapacityUsed;
		}
		public float getDiskMaxSize() {
			return diskMaxSize;
		}
		public void setDiskMaxSize(float diskMaxSize) {
			this.diskMaxSize = diskMaxSize;
		}
	}
}

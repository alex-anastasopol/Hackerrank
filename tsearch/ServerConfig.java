package ro.cst.tsearch;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

public class ServerConfig {
	
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	
	public static String getString(String key) {
		return rbc.getString(key).trim();
	}

	public static int getInteger(String key){
		return Integer.parseInt(getString(key));
	}

	public static boolean getBoolean(String key){
		String value = getString(key).toLowerCase();
		return "true".equals(value) || "1".equals(value) || "enabled".equals(value);
	}

	public static String getString(String key, String defaultValue) {
		if(rbc.containsKey(key)){
			return rbc.getString(key).trim();
		} else {
			return defaultValue;
		}
	}
	
	public static boolean getBoolean(String key, boolean defaultValue){
		String value = getString(key, String.valueOf(defaultValue)).toLowerCase();
		return "true".equals(value) || "1".equals(value) || "enabled".equals(value);
	}
	
	public static int getInteger(String key, int defaultValue){
		String value = getString(key, String.valueOf(defaultValue));
		if(value.matches("[+-]?\\d+")){
			return Integer.parseInt(value);
		} else {
			return defaultValue;
		}
	}	
	
	/* ECOR INTEGRATION */
	public static boolean isEcoreIntegrationEnabled(){
		return getBoolean("ecore.integration.enabled", false);
	}
	
	public static String getEcorGatewayLink(){
		return getString("ecor.gateway.link");
	}
	
	public static String getEcorGatewayUser(){
		return getString("ecor.gateway.user");
	}
	
	public static String getEcorGatewayPassword(){
		return getString("ecor.gateway.password");
	}
	
	public static int getEcorGatewayTimeout(){
		return getInteger("ecor.gateway.timeout");
	}
	
	/* OMNIPAGE */
	public static boolean isOmnipageEnabled(){
		return getBoolean("omnipage.enabled");
	}
	
	public static String getOmnipageLink(){
		return getString("omnipage.link");
	}
	
	public static int getOmnipageTimeout(){
		return getInteger("omnipage.timeout");
	}
	
	public static String getOmnipageUser(){
		return getString("omnipage.user");
	}
	
	public static String getOmnipagePassword(){
		return getString("omnipage.password");
	}
	
	// miscellaneous
	public static boolean isOcrAllDocumentsAutomaticEnable(){
		return getBoolean("ocr.all.documents.automatic.enable", false);
		
	}
	
	/**
	 * returns true if OCR is enabled in automatic for a given state for all documents of given type 
	 */
	public static boolean isEnabledAutomaticOcrAllDocuments(String stateAbbreviation, String type) {
		return getBoolean("enable.automatic." + stateAbbreviation.toLowerCase() +  ".ocr.all." + type.toLowerCase(), false);
	}
	
	/**
	 * returns the number of months to look back into the past for documents to be OCRed in automatic for a given state and given document type 
	 */
	public static int getAutomaticOcrAllDocumentsInterval(String stateAbbreviation, String type) {
		return getInteger("automatic." + stateAbbreviation.toLowerCase() +  ".ocr.all." + type.toLowerCase() + ".interval", 24);
	}
	
	public static boolean isOcrAllDocumentsParentsiteEnable(){
		return getBoolean("ocr.all.documents.parentsite.enable", false);
	}
	
	public static boolean isOcrDocumentsUploadEnable(){
		return getBoolean("ocr.documents.upload.enable", false);
	}
	
	public static String getImageDirectory(){
		return getString("image.directory", "");
	}
	
	/**
	 * Should contain the protocol and port where this ATS instance resides
	 * @return http://server_name:9000
	 */
	public static String getAppUrl(){
		return getString("app.url", "");
	}
	
	/**
	 * Should contain the protocol and port where initial login link is set<br>
	 * Usually this should point to the LB url.
	 * @return http://server_name
	 */
	public static String getAppMainUrl(){
		return getString("app.main.url", getAppUrl());
	}
	
	public static String getGSCommand(){
		return getString("GS.CommandString", "");
	}
	
	/**
	 * Returns a temporary local that can always be clean at the start of the application
	 * @return
	 */
	public static String getTsrCreationTempFolder(){
		return getString("tsr.creation.temp.folder", "");
	}
	
	/**
	 * This should be the time slept between retries when creating TSR
	 * @return
	 */
	public static int getTsrFailRetrySeconds() {
		return getInteger("tsr.fail.retry.seconds", 60);
	}
	
	/**
	 * Reads configuration key "files.path" and if not found throws exception<br>
	 * '/' is replaced with OS specific File.separator
	 * @return location of the folder that stores application related files
	 */
	public static String getFilePath(){
		return getString("files.path").replace("/", File.separator);
	}
	
	/**
	 * Should return the value initialized by the web server, just before the "WEB-INF" folder
	 * @return
	 */
	public static String getRealPath() {
		return BaseServlet.REAL_PATH;
	}
	
	public static String getTemplatesPath() {
		return getString("templates.path","");
	}
	/**
	 * Reads configuration key "template.openoffice.restart.tokens" and if not found will return a null
	 * @return
	 */
	public static String getTemplateOpenofficeRestartTokens() {
		return getString("template.openoffice.restart.tokens", null);
	}
	/**
	 * Reads configuration key "server.executor.restart.tokens" and if not found will return a null
	 * @return
	 */
	public static String getServerExecutorRestartTokens() {
		return getString("server.executor.restart.tokens", null);
	}
	
	private static final Pattern portPattern = Pattern.compile( "(?is).*:(\\d+)" );
	public static int getAppPortFromAppUrl(int defaultValue){
		try {
			String url = rbc.getString("app.url" );
			Matcher portMatcher = portPattern.matcher(url);
        	if( portMatcher.find() ){
        		return Integer.parseInt(portMatcher.group(1));
        	}
		} catch (Exception e) {
			
		}
		return defaultValue;
	}
	
	public static final String getILCookLdriveServer(){
		return getString("ilcook.ldrive.server").trim();
	}
	public static final String getILCookLdriveUsername(){
		return getString("ilcook.ldrive.username").trim();
	}
	public static final String getILCookLdrivePassword(){
		return getString("ilcook.ldrive.password").trim();
	}

	public static int getNumberMaxTryLock(int i) {
		return getInteger("number.max.try.mutex",i);
	}
	public static int getServerId() {
		return getInteger("repl.tsr.instance.id");
	}

	/**
	 * Check if monitoring replication is enabled on this server
	 * Reads configuration key "replication.notification.enable" and if not found will return true
	 * @return
	 */
	public static boolean isReplicationNotification() {
		return getBoolean("replication.notification.enable", true);
	}
	/**
	 * Period in seconds for checking replication status 
	 * Reads configuration key "replication.notification.period" and if not found will return 180 (seconds)
	 * @return
	 */
	public static int getReplicationNotificationPeriod() {
		return getInteger("replication.notification.period", 180);
	}
	
	public static boolean isFileReplicationEnabled() {
		return getBoolean("file.replication.enabled", false);
	}

	public static int getFileReplicationMaxChuck(int defaultValue) {
		return getInteger("file.replication.max.chunk", defaultValue);
	}

	public static String getAllServersInstances() {
		return getString("replication.all.instances", ServerConfig.getServerId() + "");
	}

	public static int getFileGetterRunInterval(int defaultSeconds) {
		return getInteger("file.getter.run.interval", defaultSeconds);
	}
	
	public static int getFileCleanerRunInterval(int defaultSeconds) {
		return getInteger("file.cleaner.run.interval", defaultSeconds);
	}
	public static boolean isBurbProxyEnabled(){
		return getBoolean("burb.proxy.enable", false);
	}
	public static int getBurbProxyPort(int defaultValue){
		return getInteger("burb.proxy.port", defaultValue);
	}
	
	public static String getDevUsProxyAddress() {
		return getString("dev.up.proxy.address", "192.168.92.55");
	}
	public static int getDevUsProxyPort() {
		return getInteger("dev.up.proxy.port", 8080);
	}
	
	/**
	 * The folder where the search context will be loaded from the database 
	 * and kept until further usage
	 * @param defaultValue
	 * @return
	 */
	public static String getArchiveDestinationFolder() {
		return getString("archive.destination.folder");
	}
	
	public static boolean isArchiveDestinationSmbEnabled() {
		return getBoolean("archive.destination.folder.smb.enabled",false);
	}
	
	public static String getArchiveDestinationUser() {
		return getString("archive.destination.folder.user");
	}
	
	public static String getArchiveDestinationPassword() {
		return getString("archive.destination.folder.password");
	}
	
	public static String getArchiveDestinationDomain() {
		return getString("archive.destination.folder.domain");
	}
	
	/**
	 * The interval after an unused searched is archived
	 * @param defaultValue
	 * @return
	 */
	public static int getArchiveIntervalDays(int defaultValue) {
		return getInteger("archive.interval.days", defaultValue);
	}
	
	public static int getArchiveSearchContextCommId(int defaultValue) {
		return getInteger("archive.search.context.commId", defaultValue);
	}
	
	
	/**
	 * The GMT hour of day (24 hour day) on which the archive service will be run
	 * @param defaultValue
	 * @return
	 */
	public static int getArchiveRunStartHourGMT(int defaultValue) {
		return getInteger("archive.run.start.hour.gmt", defaultValue);
	}
	
	/**
	 * The interval in hours at which the archive service will be run
	 * @param defaultValue
	 * @return
	 */
	public static int getArchiveRunIntervalHours(int defaultValue) {
		return getInteger("archive.run.interval.hours", defaultValue);
	}
	
	/**
	 * The max chunk size that should be archived at a given time
	 * @param defaultValue
	 * @return
	 */
	public static int getArchiveMaxChunkSize(int defaultValue) {
		return getInteger("archive.max.chunk.size", defaultValue);
	}
	
	public static int getBlockUserInterval(int defaultValue) {
		return getInteger("block.user.interval", defaultValue);
	}
	
	public static String getDoctypeFilePath() {
		return getString("doctype.file.path");
	}
	
	public static String getInvoiceCreationPath(){
		return getString("invoice.creation.path");
	}

	public static long getUserManagerRefreshPeriod(int defaultValue) {
		return getInteger("user.manager.refresh.period", defaultValue);
	}
	
	public static String getSourceComputer() {
		return getString("source.computer","");
	}
	
	public static String getDataTreeLink(){
		return getString("datatree.link");
	}
	
	public static int getDataTreeSoTimeout() {
		return getInteger("datatree.so.timeout");
	}

	public static int getDataTreeConnectionTimeout() {
		return getInteger("datatree.connection.timeout");
	}
	
	public static int getDataTreeImageRequestTimeout() {
		return getInteger("datatree.image.request.timeout", 500) * 1000;
	}
	
	public static String getConnectionTemplatesPath(){
		return BaseServlet.REAL_PATH + getString("connectiontemplates.xml.path");
	}
	
	public static String getStewartOrdersAddress(){
		return getString("stewartorders.address");
	}
	
	public static int getStewartOrdersTimeout(){
		return getInteger("stewartorders.timeout");
	}
	
	public static String getStewartOrdersAppId(){
		return getString("stewartorders.appid");
	}
	
	public static int getStewartOrdersRetries(){
		return getInteger("stewartorders.retries");
	}
	
	public static int getStewartOrdersWait(){
		return getInteger("stewartorders.wait");
	}

//please do not activate this option on more than one production server it causes duplicate keys
	public static boolean isDataTreeUpdateProfileEnabled() {
		return getBoolean("datatree.update.profile", false);
	}

	public static String getSsfLink() {
		return getString("stewart.ssf.link");
	}

	public static int getSsfTimeOut() {
		return getInteger("stewart.ssf.timeout");
	}

	public static String getSsfAppId() {
		return getString("stewart.ssf.appId");
	}

	public static String getSsfPass() {
		return getString("stewart.ssf.pass");
	}

	public static String getSsfUser() {
		return getString("stewart.ssf.user");
	}

	public static boolean isStewartSsfImagePostEnabled() {
		return getBoolean("stewart.ssf.enable.image.post", false);
	}
	
	public static boolean isStewartSsfImageRetreiveEnabled() {
		return getBoolean("stewart.ssf.enable.image.retreive", false);
	}
	
	public static String getTesseractPath() {
		return getString("tesseract.path");
	}
	
	public static boolean getTesseractEnabledForCaptcha() {
		return getBoolean("tesseract.enabled.for.captcha");
	}
	
	public static String getModuleDescriptionFolder(String defaultValue) {
		return getString("module.description.folder.path", defaultValue);
	}
	
	
	public static String getUserFolder() {
		String userFolder = getString("USERS.PDFFolder", "");
		if ( !userFolder.isEmpty() && ! userFolder.endsWith(File.separator) )
            userFolder += File.separator;
		return userFolder;
	}
	
	public static void reload() {
		ResourceBundle.clearCache();
		rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	}

	public static String getSkldImagesUrl() {
		return getString("skld.images.url");
	}

	public static String getBlankTsrPath() {
		return getString("blank.tsr.path");
	}

	public static String getBlankPdfTsrPath() {
		return getString("blank.pdf.tsr.path");
	}
	
	public static String getDiskInformationMaxSizes(String defaultValue) {
		return getString("load.information.disk.disk1.notification.limit", defaultValue);
	}
	public static String getDiskInformationFolders(String defaultValue) {
		return getString("load.information.disk.disk1", defaultValue);
	}
	public static String getDiskInformationSampleInterval(String defaultValue) {
		return getString("load.information.disk.sample.interval", defaultValue);
	}
	public static Boolean isDiskInformationEnable(boolean defaultValue) {
		return getBoolean("load.information.disk.enable", defaultValue);
	}
	
	public static int getTipOfTheDayChangeInterval() {
		return getInteger("tip.of.the.day.change.interval");
	}
	/**
	 * 
	 * @return number of seconds, default 60s
	 */
	public static int getTipOfTheDayRehreshInterval() {
		return getInteger("tip.of.the.day.refresh.interval", 60);
	}
	
	public static String getTipOfTheDayFilePath() {
		return getString("tip.of.the.day.file.path");
	}

	public static boolean isDatatreeProxyEnabled(){
		return getBoolean("datatree.proxy.enable", false);
	}
	
	public static String getDatatreeProxyHost(String defaultValue){
		return getString("datatree.proxy.host", defaultValue);
	}
	
	public static int getDatatreeProxyPort(int defaultValue){
		return getInteger("datatree.proxy.port", defaultValue);
	}

	public static String getSureCloseLink() {
		return getString("sureclose.link");
	}
	
	public static String getNetDirectorFtpSite() {
		return getString("netdirector.ftp.site");
	}

	public static String getNetDirectorFtpPassword() {
		return getString("netdirector.ftp.password");
	}

	public static String getNetDirectorFtpUser() {
		return getString("netdirector.ftp.user");
	}
	
	private static Map<Integer, String> VALUE_FOR_IMAGE_DISABLE_PER_COMMUNITY = new HashMap<Integer, String>();
	private static Map<Integer, Set<String>> VALUE_FOR_IMAGE_DISABLE_PER_COMMUNITY_SET = new HashMap<Integer, Set<String>>();
	
	public static boolean isImageDisablePerCommunity(int siteType, int commId) {
		String key = "images.disable.site." + siteType + ".per.community";
		String value = getString(key, "");
		Set<String> setBySiteType = null;
		if(!value.equals(VALUE_FOR_IMAGE_DISABLE_PER_COMMUNITY.get(siteType))) {
			VALUE_FOR_IMAGE_DISABLE_PER_COMMUNITY.put(siteType, value);
			
			setBySiteType = new HashSet<String>();
			
			VALUE_FOR_IMAGE_DISABLE_PER_COMMUNITY_SET.put(siteType, setBySiteType);
			if(StringUtils.isNotEmpty(value)) {
				String[] parts = value.split(",");
				for (String string : parts) {
					setBySiteType.add(string);
				}
			}
		}
		
		setBySiteType = VALUE_FOR_IMAGE_DISABLE_PER_COMMUNITY_SET.get(siteType);
		
		if(setBySiteType != null && setBySiteType.contains(Integer.toString(commId))) {
			return true;
		}
		
		return false;
		
	}

	
	/**
	 * Get the path to the SERVER were TIMS results are dumped for ATS to read
	 * @return the path or null if not set
	 */
	public static String getTimsFolderToConnect() {
		return getString("tims.folder.to.connect", null);
	}
	
	/**
	 * Get the path to the FOLDER on the SERVER were TIMS results are dumped for ATS to read
	 * @return the path or null if not set
	 */
	public static String getTimsFolderToRead() {
		return getString("tims.folder.to.read", null);
	}

	public static String getTimsFolderUserName() {
		return getString("tims.folder.username", null);
	}

	public static String getTimsFolderPassword() {
		return getString("tims.folder.password", null);
	}

	/**
	 * Returns time in seconds
	 * @param defaultValue in case nothing is found configured
	 * @return time in seconds
	 */
	public static int getTimsMaxRunningTimeForResponse(int defaultValue) {
		return getInteger("tims.max.running.time.for.response", defaultValue);
	}

	/**
	 * Returns the folder to keep processed files
	 * @return
	 */
	public static String getTimsFolderToSaveProcessed() {
		return getString("tims.folder.to.save.processed", null);
	}
	
	/**
	 * Returns the folder to keep failed files
	 * @return
	 */
	public static String getTimsFolderToSaveFailed() {
		return getString("tims.folder.to.save.failed", null);
	}

	public static int getTimsReaderDeamonPeriod(int defaultValue) {
		return getInteger("tims.reader.deamon.period", defaultValue);
	}
	
	public static boolean isTimsReaderDeamonEnable() {
		return getBoolean("tims.reader.deamon.enable", false);
	}
	
	public static Integer getOwnerNameSearchFilterAllow(CommunityAttributes ca) {
		if(ca == null) {
			return null;
		}
		
		String key = "owner.name.search.filter.allow." + ca.getID().toString();
		if(rbc.containsKey(key)) {
			String value = getString(key);
			if(value.matches("[+-]?\\d+")) {
				return Integer.valueOf(value);
			}
		}
		return null;
	}
	
	public static Integer getBuyerNameSearchFilterAllow(CommunityAttributes ca) {
		if(ca == null) {
			return null;
		}
		
		String key = "buyer.name.search.filter.allow." + ca.getID().toString();
		if(rbc.containsKey(key)) {
			String value = getString(key);
			if(value.matches("[+-]?\\d+")) {
				return Integer.valueOf(value);
			}
		}
		return null;
	}

	public static boolean useFromDateAtOwnerSearch(CommunityAttributes ca) {
		if (ca == null) {
			return false;
		}
		
		String key = "name.search.use.fromdate." + ca.getID().toString();
		if (rbc.containsKey(key)) {
			return getBoolean(key, false);
		}
		return false;
	}
	
	public static String getOcrDirectory(){
		return getString("ocr.directory");
	}
	
	public static String getOcrExecutableFullpath() {
		return getString("ocr.executable.fullpath");
	}
	
	public static String getSmartviewerHtmlDefaultPath() {
		return getString("smartviewer.html.default.path", BaseServlet.REAL_PATH + File.separator + "WEB-INF"+File.separator+"classes"+File.separator+"resource"+File.separator+"dip"+File.separator+"smartviewer.html");
	}
	
	public static String getClosedCommunityName() {
		return getString("community.closed.name", "Closed");
	}
	
	public static int getFTPReaderNdexTimerTaskPeriod(int defaultValue) {
		return getInteger("ftp.ndex.reader.deamon.period", defaultValue);
	}

	public static boolean isFTPNdexEnable() {
		return getBoolean("ftp.ndex.enable",false);
	}
	public static boolean isFTPNdexExportEnable() {
		return getBoolean("ftp.ndex.export.enable",false);
	}
	
	public static String getFTPNdexUrl() {
		return getString("ftp.ndex.url","ftp.bdfte.com");
	}

	public static int getFTPNdexPort() {
		return getInteger("ftp.ndex.port", 990);
	}

	public static String getFTPNdexUsername() {
		return getString("ftp.ndex.user", "ATS");
	}

	public static String getFTPNdexPassword() {
		return getString("ftp.ndex.password", "A95@P7X$n");
	}

	public static String getFTPNdexAgent() {
		return getString("ftp.ndex.agent", "aalecu_ag");
	}
	
	public static boolean isNdexResizePDFPackage(){
		return getBoolean("ndex.resize.pdf.package", true);
	}
	
	public static int getNdexCommunityId(){
		return getInteger("ndex.community.id", 264);
	}
	
	public static int getFtsCommunityId(){
		return getInteger("fts.community.id", 71);
	}
	/**
	 * Task 9159 - Auto Updates in ATS (special March Rule)<br>
	 * Key: <b>fts.auto.update.threshold</b><br>
	 * Default value: <b>2013/03/01 00:00:00</b>
	 * @return date in current time-zone or null if not found
	 */
	public static Date getFtsAutoUpdateThreshold() {
		String valueAsString = getString("fts.auto.update.threshold", "2013/03/01 00:00:00");
		try {
			return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(valueAsString);
		} catch (ParseException e) {
			return null;
		}
	}

	public static boolean isCheckAgentFileNoInUpdate() {
		return getBoolean("check.agent.fileno.in.update", true);
	}	
	
	public static boolean isDeleteNdexSpreadsheetAfterProcess() {
		return getBoolean("delete.ndex.spreadsheet.after.process", false);
	}
	
	public static String getHtmlDocCommand() {
		return getString("HTMLDOC.CommandString", "");
	}

	public static boolean isUseAbstractorFileIdOnNetdirector() {
		return getBoolean("use.abstractor.file.id.on.netdirector", false);
	}
	
	public static int getOCRDeleteDatabaseDays() {
		return getInteger("ocr.delete.database.days", 30);
	}

	public static String getFilePathServerLogs(){
		return getString("files.path.serverlogs", "");
	}
	
	public static int getMaxNumberOfPropertiesAllowed() {
		return getInteger("max.number.properties.allowed", 50);
	}
	
	public static String getFilePathUserLogs(){
		return getString("files.path.userlogs", "/data/UserLogs");
	}
	
	public static int getFVSRunnerTimerTaskPeriod() {
		return getInteger("fvs.runner.deamon.period", 6000);
	}
	
	public static boolean isFVSRunnerEnabled() {
		return getBoolean("fvs.runner.enabled", false);
	}
	
	public static int getATIRetryRequestCount() {
		return getInteger("ati.retry.request.count", 4);
	}

	public static int getDGRetryRequestCount() {
		return getInteger("dg.retry.request.count", 4);
	}
	
	public static int getDGMaxConnectionsPerHost() {
		return getInteger("dg.max.connection.per.host", 20);
	}
	
	public static String getArchiveDestinationJcifsSmbLmCompatibility() {
		return getString("archive.destination.jcifs.smb.lmCompatibility", null);
	}
	
	public static String getAddressForSiteChanged() {
		return getString("address.for.site.changed", "");
	}

	public static int getWebServiceConnectTimeoutMillis() {
		return getInteger("web.service.connect.timeout.millis", 60000);
	}
	
	public static int getAllowUpdatePeriod() {
		return getInteger("parent.order.older.than", 12);
	}

	public static String getPreviewTSRIndexName() {
		return getString("preview.tsrindex.name", "Preview.doc");
	}

	/**
	 * Version that is kept in a separate field to differentiate between location of log
	 * @return
	 */
	public static int getLogInTableVersion() {
		return getInteger("log.in.table.version", 4);
	}

	/**
	 * How many entries are processed at once
	 * @return
	 */
	public static int getLogInTableMaxLogElements() {
		return getInteger("log.in.table.max.log.elements", 10);
	}

	public static boolean isEnableLogInSamba() {
		return getBoolean("log.in.samba.enable", true);
	}
	
	public static boolean isEnableLogOldField() {
		return getBoolean("log.old.field", false);
	}

	public static String getLogInSambaPath() {
		return getString("log.in.samba.path");
	}
	
	public static String getOcrFilesInSambaPath() {
		return getString("ocr.files.in.samba.path");
	}
	
	public static String getDocumentIndexInSharedDrivePath() {
		return getString("document.index.in.shared.drive.path");
	}
	public static String getDocumentIndexBackupLocalPath() {
		return getString("document.index.backup.local.path");
	}
	/**
	 * 0 - only database<br>
	 * 1 - database and shared drive<br>
	 * 2 - only shared drive
	 * @return
	 */
	public static int getDocumentIndexEnableStatus() {
		return getInteger("document.index.enable.status", 1);
	}
	public static String getThreadLogsInSharedDrivePath() {
		return getString("thread.logs.in.shared.drive.path");
	}
	public static String getThreadLogsBackupLocalPath() {
		return getString("thread.logs.backup.local.path");
	}
	
	public static String getErrorLogsInSharedDrivePath() {
		return getString("error.logs.in.shared.drive.path");
	}
	public static String getErrorLogsBackupLocalPath() {
		return getString("error.logs.backup.local.path");
	}
	
	public static String getSsfDocumentIndexInSharedDrivePath() {
		return getString("ssf.document.index.in.shared.drive.path");
	}
	public static String getSsfDocumentIndexBackupLocalPath() {
		return getString("ssf.document.index.backup.local.path");
	}
	/**
	 * 0 - only database<br>
	 * 1 - database and shared drive<br>
	 * 2 - only shared drive
	 * @return
	 */
	public static int getSsfDocumentIndexEnableStatus() {
		return getInteger("ssf.document.index.enable.status", 1);
	}
	
	public static String getTsLogsPathPrefix() {
		return getString("tslogs.path.prefix", "");
	}

	public static boolean isEnableSigar() {
		return getBoolean("enable.sigar.load", true);
	}

	public static boolean isEnableOrderOldField() {
		return getBoolean("order.old.field", false);
	}
	
	public static boolean isEnableTsrLogOldField() {
		return getBoolean("tsrlog.old.field", false);
	}
	
	/**
	 * 0 - only database<br>
	 * 1 - database and shared drive<br>
	 * 2 - only shared drive
	 * @return
	 */
	public static int getSearchContextEnableStatus() {
		return getInteger("search.context.enable.status", 1);
	}
	public static String getSearchContextInSharedDrivePath() {
		return getString("search.context.in.shared.drive.path");
	}
	public static String getSearchContextBackupLocalPath() {
		return getString("search.context.backup.local.path");
	}

	public static int getSearchContextVersionsToKeep() {
		return getInteger("search.context.versions.to.keep", 3);
	}
	
	/**
	 * if this returns empty, the filter is not added to the module; for more states add State Abbrev separated by comma. e.g. TX,FL 
	 * @return
	 */
	public static String getUsePriorAtsFilterForStates() {
		return getString("prioratsfilter.restriction.states", "TX");
	}
	
	/**
	 * agent username
	 * @return
	 */
	public static String getPriorAtsFilterAgentRestriction() {
		return getString("prioratsfilter.restriction.agent", "Title_Source");
	}
	
	/**
	 * Date Format as MM/DD/YYYY
	 * @return
	 */
	public static String getPriorAtsFilterDateRestriction() {
		return getString("prioratsfilter.restriction.date", "01/01/2014");
	}
	
	/**
	 * number of minutes how the is displayed in order window
	 * @return
	 */
	public static int getNoteDisplayValabilityPeriod(){
		return getInteger("note.display.valability.period", 60);
	}
	
	/**
	 * number of seconds between checks for note
	 * @return
	 */
	public static int getNoteCheckInterval(){
		return getInteger("note.check.interval", 60);
	}
	
	public static boolean isMonitoredFolderEnable() {
		return getBoolean("monitored.folder.enable", true);
	}
	
	public static String getMonitoredFolder() {
		return getString("monitored.folder.path", null);
	}
}

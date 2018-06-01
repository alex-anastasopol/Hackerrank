package ro.cst.tsearch.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.MailReader.FTPReaderDaemon;
import ro.cst.tsearch.bean.NdexPackage;

public class FTPWriterNdex {

	public static String writePackage(NdexPackage ndexPackage) {
		
		if(!ServerConfig.isFTPNdexExportEnable()) {
			return "<font color=\"red\">Ndex Export FTP is NOT enabled!</font>";
		}
		
		String url = ServerConfig.getFTPNdexUrl();
		int port = ServerConfig.getFTPNdexPort();
		String username = ServerConfig.getFTPNdexUsername();
		String password = ServerConfig.getFTPNdexPassword();
		
		FTPSClient ftp = new FTPSClient(true);

		StringBuilder result = new StringBuilder();
		
		try {
			ftp.connect(url, port);
			boolean loggedIn = ftp.login(username, password);
			int replyCode = ftp.getReplyCode();
			if (!loggedIn || !FTPReply.isPositiveCompletion(replyCode)) {
				FTPReaderDaemon.getLogger().error(
						"Could not connet to Ndex ftp using: " + username + " with " + password + " at " + url + ":"
								+ port);
			}
			ftp.execPBSZ(0);
			ftp.execPROT("P");
			ftp.setFileType(FTP.ASCII_FILE_TYPE);
			ftp.setListHiddenFiles(true);
			
			
			ftp.enterLocalPassiveMode();
			ftp.changeWorkingDirectory("OUT");
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
			
			String imageDirectory = ndexPackage.getSearch().getImageDirectory() + File.separator;
			
			String runSheetPath = ndexPackage.getRunSheetPath();
			boolean stored = ro.cst.tsearch.utils.StringUtils.isNotEmpty(runSheetPath)?
					ftp.storeFile(ndexPackage.getRunSheetRemoteName(), new FileInputStream(runSheetPath))
					:false;
			if(stored) {
				result.append("Successfully stored Runsheet (ATR) under name ").append(ndexPackage.getRunSheetRemoteName()).append("<br/>");
				try {
					FileUtils.copyFile(new File(runSheetPath), new File(imageDirectory + ndexPackage.getRunSheetRemoteName()));
				} catch(Exception e) {
					e.printStackTrace();
				}
				
			} else {
				result.append("<font color=\"red\">Failed to store Runsheet (ATR) under name ").append(ndexPackage.getRunSheetRemoteName()).append("</font><br/>");
			}
			
			String titleSearchPath = ndexPackage.getTitleSearchPath();
			stored = ro.cst.tsearch.utils.StringUtils.isNotEmpty(titleSearchPath)?
					ftp.storeFile(ndexPackage.getTitleSearchRemoteName(), new FileInputStream(titleSearchPath))
					:false;
			if(stored) {
				result.append("Successfully stored Title Search File (TS) under name ").append(ndexPackage.getTitleSearchRemoteName()).append("<br/>");
				try {
					FileUtils.copyFile(new File(titleSearchPath), new File(imageDirectory + ndexPackage.getTitleSearchRemoteName()));
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else {
				result.append("<font color=\"red\">Failed to store Title Search File (TS) under name ").append(ndexPackage.getTitleSearchRemoteName()).append("</font><br/>");
			}
			
			try {
				String titleSearchCopiesPath = ndexPackage.getTitleSearchCopiesPath();
				if(titleSearchCopiesPath != null) {
					stored = ftp.storeFile(ndexPackage.getTitleSearchCopiesRemoteName(), new FileInputStream(titleSearchCopiesPath));
					if(stored) {
						result.append("Successfully stored Title Search Copies File (TSC) under name ")
							.append(ndexPackage.getTitleSearchCopiesRemoteName()).append("<br/>");
						try {
							FileUtils.copyFile(new File(titleSearchCopiesPath), new File(imageDirectory + ndexPackage.getTitleSearchCopiesRemoteName()));
						} catch(Exception e) {
							e.printStackTrace();
						}
					} else {
						result.append("<font color=\"red\">Failed to store Title Search Copies File (TSC) under name ")
							.append(ndexPackage.getTitleSearchCopiesRemoteName()).append("</font><br/>");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				result.append("<font color=\"red\">Failed to store Title Search Copies File (TSC) under name ").append(ndexPackage.getTitleSearchCopiesRemoteName()).append("</font><br/>");
			}
			
			try {
				String assigmentsPath = ndexPackage.getAssigmentsPath();
				if(assigmentsPath != null) {
					stored = ftp.storeFile(ndexPackage.getAssigmentsRemoteName(), new FileInputStream(assigmentsPath));
					if(stored) {
						result.append("Successfully stored Assignment (ASSG) under name ")
							.append(ndexPackage.getAssigmentsRemoteName()).append("<br/>");
						try {
							FileUtils.copyFile(new File(assigmentsPath), new File(imageDirectory + ndexPackage.getAssigmentsRemoteName()));
						} catch(Exception e) {
							e.printStackTrace();
						}
					} else {
						result.append("<font color=\"red\">Failed to store Assignment (ASSG) under name ")
							.append(ndexPackage.getAssigmentsRemoteName()).append("</font><br/>");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				result.append("<font color=\"red\">Failed to store Assignment (ASSG) under name ")
					.append(ndexPackage.getAssigmentsRemoteName()).append("</font><br/>");
			}
			try {
				String federalTaxLiensPath = ndexPackage.getFederalTaxLiensPath();
				if(federalTaxLiensPath != null) {
					stored = ftp.storeFile(ndexPackage.getFederalTaxLiensRemoteName(), new FileInputStream(federalTaxLiensPath));
					if(stored) {
						result.append("Successfully stored Federal Tax Lien (FTL) under name ")
							.append(ndexPackage.getFederalTaxLiensRemoteName()).append("<br/>");
						try {
							FileUtils.copyFile(new File(federalTaxLiensPath), new File(imageDirectory + ndexPackage.getFederalTaxLiensRemoteName()));
						} catch(Exception e) {
							e.printStackTrace();
						}
					} else {
						result.append("<font color=\"red\">Failed to store Federal Tax Lien (FTL) under name ")
							.append(ndexPackage.getFederalTaxLiensRemoteName()).append("</font><br/>");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				result.append("<font color=\"red\">Failed to store Federal Tax Lien (FTL) under name ")
					.append(ndexPackage.getAssigmentsRemoteName()).append("</font><br/>");
			}
			
			/*
			List<DocumentI> list = ndexPackage.getTitleSearchCopies();
			if(list != null) {
				for (DocumentI document : list) {
					String remoteName = ndexPackage.getImageRemoteName(document);
					stored = ftp.storeFile(remoteName, ndexPackage.getImageInputStream(document));
					if(stored) {
						result.append("Successfully stored Title Search Copy (TSC) under name ").append(remoteName).append("<br/>");
					} else {
						result.append("<font color=\"red\">Failed to store Title Search Copy (TSC) under name ").append(remoteName).append("</font><br/>");
					}
				}
			}
			list = ndexPackage.getAssigments();
			if(list != null) {
				for (DocumentI document : list) {
					String remoteName = ndexPackage.getImageRemoteName(document);
					stored = ftp.storeFile(remoteName, ndexPackage.getImageInputStream(document));
					if(stored) {
						result.append("Successfully stored Assignment (ASSG) under name ").append(remoteName).append("<br/>");
					} else {
						result.append("<font color=\"red\">Failed to store Assignment (ASSG) under name ").append(remoteName).append("</font><br/>");
					}
				}
			}
			list = ndexPackage.getFederalTaxLiens();
			if(list != null) {
				for (DocumentI document : list) {
					String remoteName = ndexPackage.getImageRemoteName(document);
					stored = ftp.storeFile(remoteName, ndexPackage.getImageInputStream(document));
					if(stored) {
						result.append("Successfully stored Federal Tax Lien (FTL) under name ").append(remoteName).append("<br/>");
					} else {
						result.append("<font color=\"red\">Failed to store Federal Tax Lien (FTL) under name ").append(remoteName).append("</font><br/>");
					}
				}
			}
			*/
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(ftp.isConnected()) {
					ftp.disconnect();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		ndexPackage.cleanGeneratedFiles();
		
		return result.toString();
	}
	
}

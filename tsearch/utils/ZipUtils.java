package ro.cst.tsearch.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.generic.IOUtil;
import de.schlichtherle.io.ArchiveDetector;
import de.schlichtherle.io.File;
import de.schlichtherle.io.FileOutputStream;
import de.schlichtherle.util.zip.ZipEntry;
import de.schlichtherle.util.zip.ZipFile;
import de.schlichtherle.util.zip.ZipOutputStream;


public class ZipUtils{
	static final int BUFFER = 2048;
	
	protected static final Category saveContextLogger = Logger.getLogger(ZipUtils.class);
	
	public static String getTempzipfolder() {
		return ServerConfig.getTsrCreationTempFolder() + java.io.File.separator;
	}
	
	public static void unzipContext( byte[] searchContext, String contextPath, long searchId ){
		if( searchContext == null ){
			return;
		}
		
		try{
		
			contextPath = contextPath.replaceAll( searchId + "/*" , "");
			
			File destFolder = new File( contextPath + "/" + searchId + "/" );
			destFolder.mkdirs();
			
			InputStream is = new ByteArrayInputStream( searchContext );
			FileOutputStream os = new FileOutputStream( new java.io.File( contextPath + "/" + searchId + ".zip" ) );
			
			IOUtil.copy(is, os);
			
			File zipFile = new File( contextPath + "/" + searchId + ".zip" );
			
			zipFile.copyAllTo( destFolder );
			os.flush();
			os.close();
			
			//delete zip file
			zipFile.deleteAll();
		}
		catch( Exception e ){
			e.printStackTrace();
		}
	}
	
	public static boolean includeImageFile( String fileName, String[] includedImages ){
		boolean include = true;
		
		fileName = fileName.toLowerCase();
		
		if( fileName.endsWith( ".pdf" ) || fileName.endsWith( ".tiff" ) || fileName.endsWith( ".tif" ) ||
				fileName.endsWith( ".bin" ) || fileName.endsWith( ".jpg" ) || fileName.endsWith( ".bmp" ) )
		{
			include = false;
			
			if( includedImages != null ){
				for( int j = 0 ; j < includedImages.length ; j++ ){
					if(includedImages[j] == null)
						continue;
					System.err.println("fileName" + fileName);
					System.err.println("includedImages[j]" + includedImages[j].toLowerCase());
					if( fileName.contains( includedImages[j].toLowerCase() ) ||  includedImages[j].toLowerCase().contains( fileName )){
						include = true;
						break;
					}
				}
			}
		
		}
		return include;
	}
	
	public static byte[] zipContext( String contextPath, Search context){
		return zipContext(contextPath, context, false);
	}
	
	/**
	 * 
	 * @param sourceContext source search context
	 * @param destinationZip destination zip file
	 * @param includedImages list of files that will be added
	 */
	public static void zipContextExcludeImages( File sourceContext, File destinationZip,long searchId, String[] includedImages ){
		java.io.File[] filesInFolder = sourceContext.listFiles();
		
		for( int i = 0 ; i < filesInFolder.length ; i ++){
			if( filesInFolder[i].isDirectory() ){
				//recursive call
				zipContextExcludeImages( new File(filesInFolder[i]) , destinationZip, searchId, includedImages);
			}
			else{
				//copy files to archive
				if( includeImageFile(filesInFolder[i].getName(), includedImages) ){
					
					String folderName = filesInFolder[i].getParent().replace(
							java.io.File.separator + searchId , 
							java.io.File.separator + searchId + ".zip");
					
					ArchiveDetector defaultDetector = File.getDefaultArchiveDetector();
					
					File destFile = defaultDetector.createFile( new File( folderName ), filesInFolder[i].getName() );
					
					destFile.copyFrom( new File( filesInFolder[i] ) );
				}
			}
		}
	}
	
	
	public static void zipFolder(String sourceDirectoryPath, String folderName, ZipOutputStream zos){
		try {
			//create a new File object based on the directory we have to zip File           
			File zipDir = new File(sourceDirectoryPath);
	        if( !zipDir.exists() ){
				return;
			} 
	               
	        String[] dirList = zipDir.list(); 
	        byte[] readBuffer = new byte[2156]; 
	        int bytesIn = 0; 
	        
	        for(int i = 0; i < dirList.length; i++) { 
	            File f = new File(zipDir, dirList[i]); 
  	            //if the File object is a directory, add its content recursively 
	            if(f.isDirectory()) { 
	            	String filePath = f.getPath(); 
	            	zipFolder(filePath, folderName, zos); 
	            continue; 
	            } 

	            FileInputStream fis = new FileInputStream(f);  
		        
		        ZipEntry anEntry = new ZipEntry(
		        		f.getPath().substring(f.getPath().indexOf(folderName), 
		        		f.getPath().length()));   
		        zos.putNextEntry(anEntry);    
		            
		        while((bytesIn = fis.read(readBuffer)) != -1) {   
		        	zos.write(readBuffer, 0, bytesIn); 
		        } 
		        zos.closeEntry();
		                
		        fis.close(); 
	        }
	        
	    	return;
	    	
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * Set exception format
	 * @param sourceDirectoryPath	- path of folder with all templates, that needs to be archived
	 * @param destinationFolderName	- path of destination folder
	 * @param zipFile  - name of archive
	 * @return
	 */
	
	public static void zipFolder(String sourceDirectoryPath, String destinationFolderName, String zipFile) {
		ZipOutputStream zos;
		try {
			zos = new ZipOutputStream(new FileOutputStream(zipFile));
			ZipUtils.zipFolder(sourceDirectoryPath, destinationFolderName, zos);
			zos.close();
			
		} catch (Exception e) {
			saveContextLogger.error("Error on archiving folder:" + e);
		}
	}
	
	
	
	
	public static byte[] zipContext( String contextPath, Search context , boolean excludeImages ){
		byte[] zipData = null;
		
		File destinationZip = null;
		long searchId = context.getID();
		try{
			
			saveContextLogger.info( searchId + ": Starting zip save procedure");
			saveContextLogger.info( searchId + ": Checking for __search.xml");
			String searchFileName = context.getSearchDir() + "__search.xml";
			if(new File(searchFileName).exists()) {
				saveContextLogger.info( searchId + ": File " + searchFileName + " already exists");	
			} else {
				saveContextLogger.error( searchId + ": File " + searchFileName + " does NOT already exists");
				saveContextLogger.info( searchId + ": Saving the search again as a backup procedure" );
				Search.saveSearch(context);
				saveContextLogger.info( searchId + ": Saved... Checking for files again..." );
			}
			
			if(new File(searchFileName).exists()) {
				saveContextLogger.info( searchId + ": File " + searchFileName + " already exists");	
			} else {
				saveContextLogger.error( searchId + ": File " + searchFileName + " does NOT already exists");
				saveContextLogger.info( searchId + ": Saving the search again as a backup procedure" );
				Search.saveSearch(context);
			}
			
			
			//create file search context
			File sourceContext = new File( contextPath );
			if( !sourceContext.exists() ){
				saveContextLogger.error( searchId + ": Source context doen not exist " + contextPath);
				return null;
			}
			
			//create zip in the above directory
			contextPath = contextPath.replace("" + searchId, "");
			
			destinationZip = new File( contextPath + searchId + ".zip", ArchiveDetector.DEFAULT );
			destinationZip.mkdirs();
			
			//copy all context to zip if nothing is excluded
			//if( !excludeImages ){
			saveContextLogger.info( searchId + ": Started copying all from " + contextPath);
				destinationZip.copyAllFrom( sourceContext, ArchiveDetector.DEFAULT );
			saveContextLogger.info( searchId + ": Finished copying all from " + contextPath);
			/*}
			else{
				System.err.println("ZipUtils: searchId = " + searchId);
				System.err.println("ZipUtils: search is " + (context==null?"null":"not null"));
				
				zipContextExcludeImages(sourceContext, destinationZip, searchId, new String[0]);
			}*/
			
			//update contents
			File.update( destinationZip );		
		}
		catch( Exception e ){
			e.printStackTrace();
			saveContextLogger.error( searchId + ": Big F***ing problem " + ServerResponseException.getExceptionStackTrace( e, "\n" ));
		}
		finally{
			try{
				File.umount( destinationZip );
			}
			catch( Exception e2 ){
				e2.printStackTrace();
			}
		}
		
		//read zip contents and return the byte[]
		try{
			java.io.File zipFile = new java.io.File( contextPath + context.getID() + ".zip" );
			
			java.io.FileInputStream fileReader = new java.io.FileInputStream( zipFile );
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			IOUtil.copy( fileReader , baos);
			
			zipData = baos.toByteArray();
			
			//delete the zip file
			zipFile.delete();
		}
		catch( Exception e ){
			saveContextLogger.error( searchId + ": Big F***ing problem while reading the zip " + 
					ServerResponseException.getExceptionStackTrace( e, "\n" ));
			e.printStackTrace();
		}
		
		return zipData;
	}
	
	public static boolean zipFile(String sourceFourcePath, String destFilePath){
		
		try {
			String source = sourceFourcePath;
			
			
			File sourceFile = new File( sourceFourcePath );
			if( !sourceFile.exists() ){
				return false;
			} 
			String archivePath = sourceFile.getName();
			String target = destFilePath; 
			
			try {
				FileUtils.forceDelete(new java.io.File(target));
			} catch (Exception deleteException) {
			}
			
			FileOutputStream fileTarget = new FileOutputStream(target);
			
			ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fileTarget));
			
			
			FileInputStream fis = new FileInputStream(source);
			
			
			
			zos.putNextEntry(new ZipEntry(archivePath));
			
			int size = 0;
			byte[] buffer = new byte[1024];
			
			
			while ((size = fis.read(buffer)) > 0) {
				zos.write(buffer,0,size);
			}
			zos.flush();
			zos.close();
			fis.close();
			
			// Finish zip process
			zos.close();
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		
	}

	public static byte[] unZipFile(long searchId, byte[] toUnzip) {

		if(toUnzip == null) {
			return null;
		}
		
		try {
			Random randomGenerator = new Random();

			String fileName = "";
			int randomInt = randomGenerator.nextInt(100);
			File file = new File(getTempzipfolder() + searchId + "temp_unzip_file"
					+ randomInt + ".zip");
			file.createNewFile();
			System.out.println(file.getAbsolutePath());
			FileOutputStream toWrite = new FileOutputStream(file);
			toWrite.write(toUnzip);
			toWrite.close();
			ZipFile zipFile;
			try {
				zipFile = new ZipFile(file);
			} catch (Exception e) {
				file.delete();
				return toUnzip;
			}
			Enumeration entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				fileName = entry.getName();
				copyInputStream(zipFile.getInputStream(entry),
						new BufferedOutputStream(new FileOutputStream(entry
								.getName())));
			}
			zipFile.close();

			ByteArrayOutputStream logFileBaos = new ByteArrayOutputStream();
			IOUtil.copy(new FileInputStream(fileName), logFileBaos);
			file.delete();

			File file1 = new File(getTempzipfolder() + fileName);
			file1.delete();

			try {
				file1 = new File(fileName);
				file1.delete();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return logFileBaos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public static final void copyInputStream(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[1024];
		int len;

		while ((len = in.read(buffer)) >= 0)
			out.write(buffer, 0, len);

		in.close();
		out.close();
	}
	
	/**
	 * Used to zip a string (plain text) 
	 * @param textToZip
	 * @return the byte[] containing the archived input
	 */
	public static byte[] zipString(String textToZip){
		if(textToZip != null)
			return zipBytes(textToZip.getBytes());
		return null;
	}
	
	
	/**
	 * Used to zip a byte[] (plain text) 
	 * @param bytesToZip
	 * @return the byte[] containing the archived input
	 */
	public static byte[] zipBytes(byte[] bytesToZip){
		long timeStamp = System.currentTimeMillis();
		long randomLong = RandomGenerator.getInstance().getLong();
		String zipDocument = getTempzipfolder() + "zipDocument_" + randomLong + "_" + timeStamp + ".zip";
		ZipOutputStream zos = null;
		java.io.File zipDocumentFile = null;
		try {
			zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipDocument)));
			zos.putNextEntry(new ZipEntry("content.dat"));
			zos.write(bytesToZip);
			zos.flush();
			zos.close();
			zipDocumentFile = new java.io.File(zipDocument);
			return FileUtils.readFileToByteArray(zipDocumentFile);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(zipDocumentFile != null && zipDocumentFile.exists()){
				zipDocumentFile.delete();
			}
		}
		return null;
	}
	
	
	
	/**
	 * Used to unzip a byte[] (the bytes must have been zipped using ro.cst.tsearch.utils.ZipUtils.zipString(String) method)
	 * @param textToUnzip
	 * @return the original text or null if error occurred
	 */
	public static String unzipString(byte[] textToUnzip){
		long timeStamp = System.currentTimeMillis();;
		long randomLong = RandomGenerator.getInstance().getLong();
		String zipDocument = getTempzipfolder() + "zipDocument_" + randomLong + "_" + timeStamp + ".zip";
		java.io.File file = new java.io.File(zipDocument);
		ZipFile zipFile = null;
		InputStream is = null;
		try {
			FileUtils.writeByteArrayToFile(file, textToUnzip);
			zipFile = new ZipFile(file);
			
			while (zipFile.entries().hasMoreElements()) {
				ZipEntry zipEntry = (ZipEntry) zipFile.entries().nextElement();
				is = zipFile.getInputStream(zipEntry);
				return IOUtils.toString(is);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(is != null)
				try {
					is.close();
				} catch (Exception e) {e.printStackTrace();}
			if(zipFile!=null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			if(file!=null && file.exists())
				file.delete();
		}
		
		return null;
	}
	
	
	public static List<byte[]> unzipSortedFileData(byte[] zipData){
		long timeStamp = System.currentTimeMillis();
		long randomLong = RandomGenerator.getInstance().getLong();
		String zipDocument = getTempzipfolder() + "zipDocument_" + randomLong + "_" + timeStamp + ".zip";
		java.io.File file = new java.io.File(zipDocument);
		ZipFile zipFile = null;
		List<byte[]> allData = new ArrayList<byte[]>(); 
		List<ZipEntry> list = new ArrayList<ZipEntry>();
		
		try {
			FileUtils.writeByteArrayToFile(file, zipData);
			zipFile = new ZipFile(file);
			
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				list.add((ZipEntry) entries.nextElement());
			}
			
			Collections.sort(list,new Comparator<ZipEntry>(){
				@Override
				public int compare(ZipEntry o1, ZipEntry o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			
			for(ZipEntry entry:list){
				allData.add(IOUtils.toByteArray(zipFile.getInputStream(entry)));
			}
			
			return allData;
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(zipFile!=null)
				try {
					zipFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			
			if(file!=null && file.exists())
				file.delete();
		}
		return allData;
	}
	
	/**
	 * Used to unzip a byte[] (the bytes must have been zipped using ro.cst.tsearch.utils.ZipUtils.zipBytes(byte[]) method)
	 * @param textToUnzip
	 * @return the original text or null if error occurred
	 */
	public static byte[] unzipBytes(byte[] textToUnzip){
		long timeStamp = System.currentTimeMillis();;
		long randomLong = RandomGenerator.getInstance().getLong();
		String zipDocument = getTempzipfolder() + "zipDocument_" + randomLong + "_" + timeStamp + ".zip";
		java.io.File file = new java.io.File(zipDocument);
		ZipFile zipFile = null;
		InputStream is = null;
		try {
			FileUtils.writeByteArrayToFile(file, textToUnzip);
			zipFile = new ZipFile(file);
			
			while (zipFile.entries().hasMoreElements()) {
				ZipEntry zipEntry = (ZipEntry) zipFile.entries().nextElement();
				is = zipFile.getInputStream(zipEntry);
				return IOUtils.toByteArray(is);
			}
			
		} catch (IOException e) {
//			e.printStackTrace();
		} finally {
			if(is != null)
				try {
					is.close();
				} catch (Exception e) {e.printStackTrace();}
			if(zipFile!=null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			if(file!=null && file.exists())
				file.delete();
		}
		
		return null;
	}
}


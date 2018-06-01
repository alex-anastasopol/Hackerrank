package ro.cst.tsearch.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.exceptions.BaseException;


public class FileUtils
{
	protected static final Logger logger= Logger.getLogger(FileUtils.class);
	
	public static String ReplaceAllFileSeparators(String vsInput,String vsWith)
	{
		if (File.separator.equals("\\"))
			return vsInput.replaceAll(File.separator + File.separator, vsWith);
		else
			return vsInput.replaceAll(File.separator, vsWith);
	}
	
	public static String changeExtension(String fileName, String extension){
		int poz = fileName.lastIndexOf('.');
		if(poz>0){
			fileName = fileName.substring(0,poz);
		}
		return fileName + "." + extension;
	}
	
	/**
	 * @param dir
	 * @return true if all deletions were successful.
	 * Deletes all files and subdirectories under dir.
	 * If a deletion fails, the method stops attempting to delete and returns false.
	 */
	public static boolean deleteDir(File dir)
	{
		if (dir.exists())
		{
			if (dir.isDirectory())
			{
				String[] children= dir.list();
				for (int i= 0; i < children.length; i++)
				{
					boolean success= deleteDir(new File(dir, children[i]));
					if (!success)
					{
						return false;
					}
				}
			}
			// The directory is now empty so delete it
			if (!dir.delete())
			{
				logger.error( "Unable to delete dir: " + dir.getAbsolutePath() );
				return false;
			}else
				return true;
		}else
			return true;
		
	}
	
	public static boolean deleteFile(String fullName){
		try{
			File f = new File(fullName );
			return f.delete();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Deletes all files from this map, never throwing an exception<br>
	 * If file is a directory, delete it and all sub-directories
	 * @param fileNames - Set contains possible file names
	 * @return the number of files from the set that was deleted (folder counts a one)
	 */
	public static int deleteQuietlyAllFiles(Set<String> fileNames) {
		if(fileNames == null)
			return 0;
		int deletedFiles = 0;
		
		for (String fileName : fileNames) {
			if(org.apache.commons.io.FileUtils.deleteQuietly(new File(fileName))) {
				deletedFiles++;
			}
		}
		
		return deletedFiles;
	}
	
	public static void CreateOutputDir(String vsOutputFile)
	{
		vsOutputFile= vsOutputFile.substring(0, vsOutputFile.lastIndexOf(File.separator));
		File f= new File(vsOutputFile);
		if (!f.exists())
			f.mkdirs();
	}
	
	/**
	* @return true if file exists after the execution of the method
	*/
	public static boolean createDirectory(String dirPath) {
		File file = new File(dirPath);
		boolean success = file.exists();
		
		if(!success){
			success = file.mkdirs();
		}
		
		return success && file.isDirectory();
	}


	
	public static String removeFileExtention(String vsInputFile)
	{
		int dotIndex=vsInputFile.lastIndexOf('.');
		if (dotIndex>=0)
			return vsInputFile.substring(0,dotIndex);
		else
			throw new InvalidParameterException ("Can not find the file extention in following string: " + vsInputFile); 
	}
	/**
	 * @param vsInputFile
	 * @return
	 * extracts name of the file without extension from the entire path
	 */
	public static String getNameWithoutExtention (String vsInputFile) {
		int dotIndex = vsInputFile.lastIndexOf('.');
		if (dotIndex >= 0)
			vsInputFile = vsInputFile.substring(0, dotIndex);
	    dotIndex = vsInputFile.lastIndexOf(File.separator);
	    if (dotIndex >= 0)
	        vsInputFile = vsInputFile.substring(dotIndex + File.separator.length());
	    return vsInputFile;
	}
	
	public static String getNameWithoutExtentionSO (String vsInputFile) {
		int index = vsInputFile.lastIndexOf('.');
		if (index >= 0)
			vsInputFile = vsInputFile.substring(0, index);
	    
		index = vsInputFile.lastIndexOf("/");
		int index2 = vsInputFile.lastIndexOf("\\");
		if (index < index2)
		    index = index2;
	    
	    if (index >= 0)
	        vsInputFile = vsInputFile.substring(index + File.separator.length());
	    
	    return vsInputFile;
	}
	
	public static String getFileExtension(String fileName) {
	    
	    int index = fileName.lastIndexOf('.');
		if (index >= 0)
		    fileName = fileName.substring(index, fileName.length());
		
		return fileName;
	    
	}
	
	public static boolean existPath(String path)
	{
		if(StringUtils.isEmpty(path)){
			return false;
		}
		File f= new File(path);
		return f.exists(); 
	}
	
	public static boolean existPath(File file)
	{
		return file.exists(); 
	}
	
	/**
	 * @param file
	 * delete all files in directory
	 */
	public static void cleanFileDirectory(String file){
		String dirName =file.substring(0, file.lastIndexOf(File.separator));
		File f= new File(dirName);
		if(f.exists()){
			//String[] dirList = f.list();
			File[] dirList = f.listFiles();
			for(int i=0;i<dirList.length;i++){				
				dirList[i].delete();
						
			}
		}
			
			
	}
	
	public static String stripImagePathFromFilePath (String filePath) throws BaseException{
		String returnedPath = null;
		String basePath = filePath.substring(0, filePath.lastIndexOf(File.separator));
		String imageName = filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.lastIndexOf("."));		
		while (basePath.lastIndexOf(File.separator) == basePath.length()){
			basePath = basePath.substring(0, basePath.length()-1);
		}
		if (!(new File(basePath).isDirectory()))
			throw new BaseException("basepath should be a directory");
		File[] filesInBaseDir = new File(basePath).listFiles();
		for (int i = 0; i < filesInBaseDir.length; i++){
			String fileName =  filesInBaseDir[i].getName();
			String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
			if (fileName.startsWith(imageName) && !fileExtension.startsWith("htm"))
				returnedPath = basePath + File.separator + fileName;
		}
		return returnedPath;
	}
	
	public static boolean checkDirectory(String dirPath) {
		File dirFile = new File(dirPath);
		if (!dirFile.exists()){
			if (!dirFile.mkdir()) {
				return false;
			}
		}
		return true;
	}
	
	public static String readFilePreserveNewLines(String filePath)
	{
	    StringBuffer buffer = new StringBuffer("");
	    
	    try
	    {
	        BufferedReader in = new BufferedReader(new java.io.FileReader(filePath));
	        String str;
	        while ((str = in.readLine()) != null) 
	        {
	            buffer.append(str);
	            buffer.append("\r\n");
	        }
	        
	        in.close();
	    }
	    catch(Exception ex)
	    {
	        ex.printStackTrace();
	    }
	    
	    return buffer.toString();
	}
	
	public static String readFile(String filePath)
	{
	    return readFile(filePath, false);
	}
	
	public static String readFile(String filePath, boolean useNewLine)
	{
	    StringBuffer buffer = new StringBuffer("");
	    
	    try
	    {
	        BufferedReader in = new BufferedReader(new java.io.FileReader(filePath));
	        String str;
	        while ((str = in.readLine()) != null) 
	        {
	            String stringToAppend = str+ (useNewLine? "\n": "");
				buffer.append(stringToAppend);
	        }
	        
	        in.close();
	    }
	    catch(Exception ex)
	    {
	        ex.printStackTrace();
	    }
	    
	    return buffer.toString();
	}
	
	
	/**
	 * Read a binary file
	 * @param filePath
	 * @return read bytes
	 * @throws RuntimeException if anything goes wrong
	 */
	public static byte[] readBinaryFile(String filePath){
		InputStream is = null;
		try {
			File file = new File(filePath);
			int len = (int)file.length();
			byte [] contents = new byte[len];
			is = new FileInputStream(file);
			int readLen = is.read(contents);
			if(readLen != len){
				throw new RuntimeException("File " + filePath + " was not read correctly!");				
			}
			return contents;
		}catch(IOException e){
			throw new RuntimeException(e);
		}finally{
			if(is != null){
				try{
					is.close();
				}catch(IOException e2){}
			}			
		}		
	}
	
	/**
	 * close stream, catch and log the possible exception
	 * @param rdr
	 */
	private static void close(Reader rdr){
		try { 
			rdr.close(); 
		} catch (IOException e) {
			logger.error("Reader Close Error", e);
		}
	}
	/**
	 * close stream, catch and log the possible exception
	 * @param os
	 */
	private static void close(OutputStream os){
		try { 
			os.close(); 
		} catch (IOException e) {
			logger.error("Output Stream Close Error", e);
		}		
	}	
	
	/**
	 * close stream, catch and log the possible exception
	 * @param is
	 */
	private static void close(InputStream is){
		try { 
			is.close(); 
		} catch (IOException e) {
			logger.error("Input Stream Close Error", e);
		}
	}
	
	/**
	 * writes an input stream to a file then closes the input stream 
	 * input stream is always closed, all exceptions are caught and logged
	 * @param inputStream
	 * @param fileName
	 */
	public static void writeStreamToFile(InputStream inputStream, String fileName){
		try {
			OutputStream os = new BufferedOutputStream(new FileOutputStream(fileName));
			try {
				byte buffer[] = new byte[8192];
				int read = 0;
				do{
					read = inputStream.read(buffer);
					if(read != -1){
						os.write(buffer, 0, read);
					}
				} while (read != -1);				
			} catch (IOException e){
				logger.error("IOException", e);							
			} finally {
				close(os);
			}
		} catch (FileNotFoundException e){
			logger.error("Output File Not Found :" + fileName, e);		
		} finally {
			close(inputStream);
		}
	}
	
	/**
	 * Reads a text file
	 * @param fileName
	 * @param leaveNewLines
	 * @return file contents, null if error occured
	 * @throws RuntimeException if anything went wrong
	 */	
	public static String readTextFile(String fileName, boolean leaveNewLines) throws RuntimeException {
		
		// open input file
		BufferedReader br;
		try{
			br = new BufferedReader(new FileReader(fileName));
		}catch(FileNotFoundException e){
			logger.error("Input File Not Found :" + fileName, e);
			throw new RuntimeException(e);
		}
		
		// read  the file
		StringBuilder sb = new StringBuilder();
		String line;
		try{
			while((line = br.readLine()) != null){
				sb.append(line);
				if(leaveNewLines){
					sb.append("\r\n");
				}
			}
		}catch(IOException e){
			logger.error("IOException", e);
			throw new RuntimeException(e);
		}
		finally{
			close(br);
		}	
		
		// return contents
		return sb.toString();
	}
	
	/**
	 * Reads a text file
	 * @param fileName
	 * @return file contents, null if error occured
	 * @throws RuntimeException if anything went wrong
	 */
	public static String readTextFile(String fileName) throws RuntimeException {		
		return readTextFile(fileName, false);
	}
	
	/**
	 * Writes a text to a file
	 * @param fileName
	 * @param text
	 */
	public static void writeTextFile(String fileName, String text){
		FileOutputStream os;
		try{
			os = new FileOutputStream(fileName);
		}catch(FileNotFoundException e){
			logger.error("Output File Not Found :" + fileName, e);
			return;
		}
		try{
			os.write(text.getBytes());
		} catch(IOException e){
			logger.error("IOException", e);
		} finally {
			close(os);
		}
	}
	
	public static void appendToTextFile(String fileName, String text) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(fileName, true));
			out.write(text);
			out.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * 
	 * @param fileName
	 * @param regex
	 */
	public static void removeFilePart(String fileName, String regex){
		
		String text;
		try{
			text = readTextFile(fileName);
		}catch(RuntimeException e){
			return;
		}		
		text = text.replaceFirst(regex, "");
		writeTextFile(fileName, text);
	}
	
	/**
	 * Read a file from disk
	 * @param fileName name of the file to be read
	 * @return
	 * @throws RuntimeException if file not found, io error or file could not be closed
	 */
	public static String readXMLFile(String fileName) throws RuntimeException {

		
		BufferedReader in = null;
		try{
			// create buffered reader on input file
			in = new BufferedReader(new java.io.FileReader(fileName));
		}catch(FileNotFoundException fnfe){
			logger.error(fnfe);
			throw new RuntimeException(fnfe);
		}	
		
		try{
			// read the file
			StringBuffer buffer = new StringBuffer("");	    
	        String str;
	        while ((str = in.readLine()) != null){ 
	            buffer.append(str.trim());
	            buffer.append(System.getProperty("line.separator"));
	        }
	        return buffer.toString();	        
		}catch(IOException ioe){
			logger.error(ioe);
			throw new RuntimeException(ioe);
		}finally{
			// always close input file
			try{
				in.close();
			}catch(IOException ioe2){
				logger.error(ioe2);
				throw new RuntimeException(ioe2);
			}
		}
	}
	
	/**
	 * Loads a parameters file from a file
	 * @param fileName
	 * @param lowercaseNames
	 * @return map with parameter-value pairs 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Map<String,String> loadParams(String fileName, boolean lowercaseNames) throws FileNotFoundException, IOException{
		Map<String,String> params = new HashMap<String,String>();
    	BufferedReader br = new BufferedReader(new FileReader(fileName));
    	String line;
    	try {
	    	while((line = br.readLine()) != null){
	    		int idx = line.indexOf('=');
	    		if((idx != -1) && (idx != (line.length()-1))){
	    			String key = line.substring(0,idx).trim();
	    			String value = line.substring(idx+1).trim();
	    			if(!"".equals(key)){
	    				if(lowercaseNames){
	    					key = key.toLowerCase();
	    				}
	    				params.put(key, value);
	    			}
	    		}
	    	}
    	} finally {
    		br.close();
    	}
    	return params;
    }
	
	public static String readInputStream(InputStream is) throws IOException {
		
		BufferedInputStream bis = new  BufferedInputStream(is);
		
		try{	
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			byte[] buff = new byte[1024];
			
			int length;
			while ( (length = bis.read(buff)) != -1 )
	        {
				baos.write(buff, 0, length);
	        }
						
			baos.close();
			
			String retVal = new String(baos.toByteArray());
	
			return retVal;		
			
		}finally{			
			bis.close();
		}
	}
	
	/**
	 * Copy a file to another file
	 * @param srcFileName
	 * @param dstFileName
	 * @return
	 */
	public static boolean copy(String srcFileName, String dstFileName){
		FileChannel srcChannel = null;
		FileChannel dstChannel = null;
		try {
	        srcChannel = new FileInputStream(srcFileName).getChannel();
	        dstChannel = new FileOutputStream(dstFileName).getChannel();
	        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
	        return true;
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return false;
	    } finally{
	        // Close the channels
	    	try{ if(srcChannel != null) srcChannel.close(); } catch(IOException e){};
	    	try{ if(dstChannel != null) dstChannel.close(); } catch(IOException e){};
	    }
	}
	
	/**
	 * Returns a file name from a full path
	 * The file need not necessary be on the hard drive
	 * @param fullName could include directory path
	 * @return the file name
	 */
	public static String getFileName(String fullName){
		if(fullName==null)
			return null;
		int pos = fullName.lastIndexOf(File.separator);
		if(pos < 0)
			return fullName;
		return fullName.substring(pos+1);
		
	}
	
	/**
	 * Write a byte array to a file
	 * @param data
	 * @param fileName
	 */
	public static void writeByteArrayToFile(byte [] data, String fileName){
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			try {
				fos.write(data);
			} finally {
				fos.close();
			}
		}catch(IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public static String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a
		 * StringBuilder
		 * and returned as String.
		 */
		if (is != null) {

			StringBuilder sb = new StringBuilder();

			String line;

			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {
			return "";
		}
	} 
	
	public static void main(String[] args) {
		File dir = new File("\\\\HOUNA2\\WestWay_Common$\\STH\\Westway Park\\TDI Orders");
		
		System.out.println(new Date().toString());
		
		while(true) {
			long startTime = System.currentTimeMillis();
			File[] listFiles = dir.listFiles(new StartsWithFileFilter("test"));
			System.out.println("analized folder in " + ((System.currentTimeMillis() - startTime) / 1000 ) + " seconds and found " + listFiles.length);
			
			for (File file : listFiles) {
				//if(file.isFile() && file.getName().startsWith("test")) {
					//System.out.println("//\"" + file.getAbsolutePath().replace("\\", "/") + "\",");
					System.out.println("File: " + file.getAbsolutePath() + ", time " + new Date().toString());
				//}
			}
			
			System.out.println("analized folder in " + ((System.currentTimeMillis() - startTime) / 1000 ) + " seconds and found " + listFiles.length);
			System.out.println("_----------------------------_");
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Creates a temporary file with the given <b>content</b><br>
	 * Do not forget to delete the file after you finish using it.<br>
	 * Please note that when the application starts, all temporary files will be deleted
	 * @param content the content to be written (if null, an empty file is created)
	 * @return the file created or null if any errors occurred 
	 */
	public static File createTemporaryFileWithContent(byte[] content) {
		Random randomGenerator = new Random();
		int randomInt = randomGenerator.nextInt(100);
		File file = new File(ServerConfig.getTsrCreationTempFolder() + File.separator + "temp_file_with_content" + randomInt);
		while(file.exists()) {
			randomInt = randomGenerator.nextInt(100);
			file = new File(ServerConfig.getTsrCreationTempFolder() + File.separator + "temp_file_with_content" + randomInt);
		}
		if(content == null) {
			return file;
		}
		try {
			org.apache.commons.io.FileUtils.writeByteArrayToFile(file, content);
		} catch (IOException e) {
			logger.error("Error writing content to temporary file " + file.getName(), e);
			return null;
		}
		return file;
	}
	
	
}

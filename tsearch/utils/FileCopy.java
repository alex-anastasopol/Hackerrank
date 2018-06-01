package ro.cst.tsearch.utils;
import java.io.*;
import java.util.ResourceBundle;

import org.apache.log4j.Category;

import ro.cst.tsearch.generic.IOUtil;
import ro.cst.tsearch.templates.AddDocsTemplates;
public class FileCopy
{
	
	protected static final Category logger= Category.getInstance(FileCopy.class.getName());
	
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	public static boolean isUnix = "unix".equals(rbc.getString("so.info").trim());

	public static void copy(String source_name, String dest_name) throws IOException{
		File source_file= new File(source_name);
		copy(source_file, dest_name, false);
	}
	
	public static void copy(File source_file, String dest_name) throws IOException
	{
		copy(source_file, dest_name, false);
	}
	
	public static void copy(File source_file, String dest_name, boolean prepareFile) throws IOException
	{	RandomAccessFile rand=null;
		File destination_file=null;
		FileOutputStream destination=null;
		try
		{
		rand=new RandomAccessFile(source_file,"r");
		byte[] b=new byte[(int)rand.length()];
		rand.readFully(b);
		rand.close();
		destination_file=new File(dest_name);
		destination= new FileOutputStream(destination_file);
		
		if (!source_file.exists() || !source_file.isFile())
			throw new FileCopyException("FileCopy: no such source file: " + source_file.getName() );
		if (!source_file.canRead())
			throw new FileCopyException("FileCopy: source file " + "is unreadable: " + source_file.getName());
		
		if (destination_file.exists())
		{
			if (destination_file.isFile())
			{
				if (!destination_file.canWrite())
					throw new FileCopyException("FileCopy:	destination " + "file is unwriteable: " + dest_name);
			}
			else
				throw new FileCopyException("FileCopy: destination " + "is not a file: " + dest_name);
		}
		else
		{
			File parentdir= parent(destination_file);
			if (!parentdir.exists())
				throw new FileCopyException("FileCopy: destination " + "directory doesn't exist: " + dest_name);
			if (!parentdir.canWrite())
				throw new FileCopyException("FileCopy: destination " + "directory is unwriteable: " + dest_name);
		}
		
		if (prepareFile) {
			String str=new String(b);
			if (isUnix){
				destination.write(str.replaceAll("\r\n","\n").getBytes());
				destination.close();
				logger.error(">>>>>>>>>>>>>>> is UNIX FORMAT" );
			} else {
				destination.write(str.replaceAll("\n","\r\n").getBytes());
				destination.close();
				logger.error(">>>>>>>>>>>>>>> is WINDOWS FORMAT" );
			}  
           
		} 
		else destination.write(b);
		}
		finally
		{
			if (rand != null)
				try
				{
					rand.close(); 
				}
				catch (IOException e)
				{
					//nu mai e nimic de facut daca nu se pot inchide;
				}
			if (destination != null)
				try
				{
					destination.close();
				}
				catch (IOException e)
				{
					//nu mai e nimic de facut daca nu se pot inchide;
				}
		}
		
	}
	// File.getParent() can return null when the file is specified without
	// a directory or is in the root directory.
	// This method handles those cases.
	private static File parent(File f)
	{
		String dirname= f.getParent();
		if (dirname == null)
		{
			if (f.isAbsolute())
				return new File(File.separator);
			else
				return new File(System.getProperty("user.dir"));
		}
		return new File(dirname);
	}
	
	public static void copy(File source, File destination) {
	
	    if (source.exists()) {
		    
	        if (source.isFile()) {
		        
	            try {
	                IOUtil.copy(new FileInputStream(source), new FileOutputStream(destination));
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	            
		    } else {
		        
		        if (!destination.exists())
		            destination.mkdirs();
		        
		        File[] files = source.listFiles();
		        
		        for (int i = 0; i < files.length; i++) {
		            
		            File file = new File(destination.getPath() + File.separator + files[i].getName());
		            
		            FileCopy.copy(files[i], file);
		        }
		    }
	    }
	}
	
	public static void main(String[] args) {
	    
        //FileCopy.copy(new File("c:\\4196"), new File("c:\\4196_1111111"));
		
		try {
			
			FileInputStream source= new FileInputStream("C:\\Documents and Settings\\Catalin\\Desktop\\atsFields14.pxt");
			FileOutputStream dest= new FileOutputStream("C:\\Documents and Settings\\Catalin\\Desktop\\atsFields14.txt");
			
			byte[] buffer= new byte[1024];
			
			int numberOfLines = 0;
			while (true)
			{
				int bytes_read= source.read(buffer);
				if (bytes_read == -1)
					break;
				
				String buf = new String(buffer, 0, bytes_read);

				buffer = buf.replaceAll("\r\n", "\n").getBytes();
				dest.write(buffer, 0, buffer.length);
				
				/*int index = 0;
				while ((index = buf.indexOf("\r\n", index) + 1) != 0)
					numberOfLines++;*/
			}
			
			System.err.println("Number of lines = " + numberOfLines);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
}
class FileCopyException extends IOException
{
	public FileCopyException(String msg)
	{
		super(msg);
	}
}

package ro.cst.tsearch.utils.test;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.apache.commons.io.FileUtils;

public class StringUtils {
	/**
	 * Used with debug parameter set to true (e.g. -  -Ddebug=true) to put things in a file
	 */
	public static void write(String filename, String content){
		try {
			String absolutePath = (new File("")).getAbsolutePath();
			absolutePath+="\\resources\\output";
	        File file = new File(absolutePath+"\\"+filename);
	        boolean exists = file.exists();
	        String readFileToString;
	        StringBuffer buffer = new StringBuffer();
	        if (exists){
	        	readFileToString = FileUtils.readFileToString(file);
	        	buffer.append(readFileToString);
	        }
	        buffer.append(content);
	        FileUtils.writeStringToFile(file,buffer.toString());
	        
//	        BufferedWriter out = new BufferedWriter(new FileWriter("filename", true));
//	        out.write("aString");
//	        
//	        BufferedWriter out = new BufferedWriter(new FileWriter(resource.getFile()),true);
//	        File file = new File(resource.getFile());
	        
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	}
}

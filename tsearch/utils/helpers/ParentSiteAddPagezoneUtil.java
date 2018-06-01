package ro.cst.tsearch.utils.helpers;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;

public class ParentSiteAddPagezoneUtil {
	
	public static void main(String[] args) {
		File folder = new File("D:\\workspace2\\TS_main\\src\\resource\\ParentSite");
		RegexFileFilter filter = new RegexFileFilter("(FL|CO).*DT.xml");
		File[] listFiles = folder.listFiles((FileFilter)filter);
		
		Pattern pageZonePattern = Pattern.compile("<name>PageZone(\\d+)</name>");
		Pattern moduleIndex = Pattern.compile("<moduleIndex>36</moduleIndex>");
		
		
		String contentToAdd = null;
		try {
			contentToAdd = FileUtils.readFileToString(new File("C:\\Users\\Andrei\\Desktop\\toload.txt"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		for (File file : listFiles) {
			try {
				String fileContent = FileUtils.readFileToString(file);
				
				Matcher matcher = pageZonePattern.matcher(fileContent);
				
				int maxPageZone = 0;
				
				while(matcher.find()) {
					int currentPageZone = Integer.parseInt(matcher.group(1));
					if(maxPageZone < currentPageZone) {
						maxPageZone = currentPageZone;
					}
				}
				
				matcher = moduleIndex.matcher(fileContent);
				
				boolean okFromModule = !matcher.find();
				
				if(maxPageZone == 10 && okFromModule) {
					System.out.println("File " + file.getName() + " is ok to be updated automatically");
					
					
					fileContent = fileContent.replace("</tipp>", contentToAdd + "</tipp>");
					
					
					//FileUtils.write(file, fileContent);
					
				} else {
					System.err.println("File " + file.getName() + " needs manual attention because of maxPageZone = " + maxPageZone);
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
		}
		
	}
	
}

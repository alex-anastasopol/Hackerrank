package ro.cst.tsearch.utils;

import java.io.File;
import java.io.FileFilter;

/**
 * Used to get either just files or just directories or both 
 * @author Andrei
 *
 */
public class FileTypeFilter implements FileFilter {

	boolean acceptFiles = true;
	boolean acceptDirectories = true;
	
	
	/**
	 * Default behavior is to accept just files (not directories)
	 */
	public FileTypeFilter() {
		this.acceptFiles = true;
		this.acceptDirectories = false;
	}
	
	public FileTypeFilter(boolean acceptFiles, boolean acceptDirectories) {
		this.acceptFiles = acceptFiles;
		this.acceptDirectories = acceptDirectories;
	}
	
	@Override
	public boolean accept(File pathname) {
		
		if(acceptFiles && pathname.isFile()) {
			return true;
		}
		if(acceptDirectories && pathname.isDirectory()) {
			return true;
		}
		return false;
	}

}

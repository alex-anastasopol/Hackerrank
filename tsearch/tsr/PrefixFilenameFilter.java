package ro.cst.tsearch.tsr;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Class that filter all files from the specified folder starting with the given prefix
 * @author andrei alecu
 *
 */
public class PrefixFilenameFilter implements FilenameFilter {
	
	private String filePrefix = null;
	
	public PrefixFilenameFilter(String filePrefix) {
		this.filePrefix = filePrefix;
	}

	public boolean accept(File dir, String fileName) {
		if(filePrefix==null)
			return false;
		if(fileName.startsWith(filePrefix))
			return true;
		return false;
	}

}

package ro.cst.tsearch.connection.ftp;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class StartsWithFTPFileFilter implements FTPFileFilter {

	private Set<String> startWith;
	private Set<String> acceptedFiles = new HashSet<String>();
	
	public StartsWithFTPFileFilter(String startWith) {
		if(startWith == null) {
			throw new NullPointerException("startWith - cannot be null");
		}
		this.startWith = new HashSet<String>();
		this.startWith.add(startWith);
	}
	
	public StartsWithFTPFileFilter(Set<String> startWith) {
		if(startWith == null) {
			throw new NullPointerException("startWith - cannot be null");
		}
		this.startWith = new HashSet<String>();
		this.startWith.addAll(startWith);
	}
	@Override
	public boolean accept(FTPFile file) {
		if(startWith.size() == 0) {
			return false;
		}
		
		String fileName = file.getName();
		
		for (String startWithToken : startWith) {
			if(fileName.startsWith(startWithToken)) {
				if(file.isFile()) { 
					if(!acceptedFiles.contains(fileName)) {
						acceptedFiles.add(fileName);
						return true;
					}
				}
			}
		}
		
		
		
		return false;
	}

}

package ro.cst.tsearch.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;

public class StartsWithFileFilter implements FileFilter {

	private Set<String> startWith;
	private Set<String> acceptedFiles = new HashSet<String>();
	
	public StartsWithFileFilter(String startWith) {
		if(startWith == null) {
			throw new NullPointerException("startWith - cannot be null");
		}
		this.startWith = new HashSet<String>();
		this.startWith.add(startWith);
	}
	
	public StartsWithFileFilter(Set<String> startWith) {
		if(startWith == null) {
			throw new NullPointerException("startWith - cannot be null");
		}
		this.startWith = new HashSet<String>();
		this.startWith.addAll(startWith);
	}
	
	
	
	@Override
	public boolean accept(File file) {

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

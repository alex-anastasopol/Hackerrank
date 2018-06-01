package ro.cst.tsearch.utils;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.io.filefilter.FileFileFilter;

public class FileFilterUtil implements FileFilter {

	private String[] acceptedExtensions = new String[0];
	
	public FileFilterUtil(String[] acceptedExtensions){
		addAcceptedExtensions(acceptedExtensions);
	}
	
	public FileFilterUtil(FileFilterUtil ffu){
		this(ffu.getAcceptedExtensions());
	}
	
	public String[] getAcceptedExtensions(){
		return acceptedExtensions;
	}
	
	public void addAcceptedExtensions(String[] acceptedExtensions){
		String[] acceptedExtensionsTmp = new String[this.acceptedExtensions.length + acceptedExtensions.length];
		int i = 0;
		int j;
		for ( j= 0; j<this.acceptedExtensions.length; j++){
			acceptedExtensionsTmp[j] = this.acceptedExtensions[j];
		}
		for (j = 0, i=this.acceptedExtensions.length; j<acceptedExtensions.length; j++){
			if (acceptedExtensions[j].startsWith(".")){
				acceptedExtensions[j] = acceptedExtensions[j].replaceFirst(".", "");
			}
			acceptedExtensions[j] = acceptedExtensions[j].toUpperCase();
			acceptedExtensionsTmp[j+i] = acceptedExtensions[j];
		}
		this.acceptedExtensions = acceptedExtensionsTmp;
	}
	
	public FileFilterUtil(){
		
	}
	
	public boolean accept(File pathName){
		for(String i:this.acceptedExtensions){
			if (pathName.getName().toUpperCase().endsWith("." + i)) return true;
		}
		return false;
	}
}

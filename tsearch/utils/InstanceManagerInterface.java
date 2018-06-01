package ro.cst.tsearch.utils;

import ro.cst.tsearch.utils.CurrentInstance;

public interface InstanceManagerInterface {

	public Object getCurrentInstance(long searchId);
	
	public void setCurrentInstance(long searchId,CurrentInstance ci);
	    
}

package ro.cst.tsearch.utils;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.CurrentInstance;
import java.util.HashMap;

public class InstanceManager implements InstanceManagerInterface{
	
	private HashMap<Long,CurrentInstance> allActiveInstances = new HashMap<Long,CurrentInstance>(100);
	
	private static InstanceManager manager = new InstanceManager(); 
	
	
	public  synchronized CurrentInstance getCurrentInstance(long searchId) {
		Object current  = allActiveInstances.get(Long.valueOf(searchId));
		
		if( current == null ){
			current = new CurrentInstance();
			allActiveInstances.put(new Long(searchId),(CurrentInstance) current);
		}
		
		return (CurrentInstance)current;
	}
	
	public  synchronized void setCurrentInstance(long searchId,CurrentInstance ci) {
		allActiveInstances.put(Long.valueOf(searchId), ci);
	}
	
	public  synchronized void removeCurrentInstance(long searchId) {
		this.setCurrentInstance(searchId,null);
	}
	
	public static InstanceManager getManager(){
		return InstanceManager.manager;
	}
	
	public int getCommunityId(long searchId) {
		
		if (searchId==-15) {		//fake community ID user in ro.cst.tsearch.threads.GenericCountyRecorderRO
			return 3;
		}
	
		if(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity()==null) {
			return (int) DBManager.getCommunityForSearch(searchId);
		}
		return InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().intValue();
	}

	
}

package ro.cst.tsearch.bean;

import java.io.Serializable;

public class MonitoringLoadBean implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private float load;
	private boolean deadlockFree;
	private boolean databaseUp;
	public float getLoad() {
		return load;
	}
	public void setLoad(float load) {
		this.load = load;
	}
	public boolean isDeadlockFree() {
		return deadlockFree;
	}
	public void setDeadlockFree(boolean deadlockFree) {
		this.deadlockFree = deadlockFree;
	}
	public boolean isDatabaseUp() {
		return databaseUp;
	}
	public void setDatabaseUp(boolean databaseUp) {
		this.databaseUp = databaseUp;
	}
	@Override
	public String toString() {
		return "MonitoringLoadBean [databaseUp=" + databaseUp
				+ ", deadlockFree=" + deadlockFree + ", load=" + load + "]";
	}
	
	

}

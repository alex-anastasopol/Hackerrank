package ro.cst.tsearch.bean;

import java.util.Date;

public class DeadlockBean {
	private boolean deadlockCurrentlyDetected = false;
	private int deadlockCount = 0;
	private Date firstDeadlockFoundDate = null;
	private Date firstEmailSentDate = null;
	private int emailSentCount = 0;
	public boolean isDeadlockCurrentlyDetected() {
		return deadlockCurrentlyDetected;
	}
	public void setDeadlockCurrentlyDetected(boolean deadlockCurrentlyDetected) {
		this.deadlockCurrentlyDetected = deadlockCurrentlyDetected;
	}
	public int getDeadlockCount() {
		return deadlockCount;
	}
	public void setDeadlockCount(int deadlockCount) {
		this.deadlockCount = deadlockCount;
	}
	public int incrementDeadlockCount() {
		return deadlockCount++;
	}
	public Date getFirstDeadlockFoundDate() {
		return firstDeadlockFoundDate;
	}
	public void setFirstDeadlockFoundDate(Date firstDeadlockFoundDate) {
		this.firstDeadlockFoundDate = firstDeadlockFoundDate;
	}
	public Date getFirstEmailSentDate() {
		return firstEmailSentDate;
	}
	public void setFirstEmailSentDate(Date firstEmailSentDate) {
		this.firstEmailSentDate = firstEmailSentDate;
	}
	public int getEmailSentCount() {
		return emailSentCount;
	}
	public void setEmailSentCount(int emailSentCount) {
		this.emailSentCount = emailSentCount;
	}

}

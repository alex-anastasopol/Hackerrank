package ro.cst.tsearch.data;

import java.util.Date;

public class DueDate {

	private long id = 0;
	private long commId = Long.MIN_VALUE;
	private long countyId = Long.MIN_VALUE;
	private Date dueDate;
	
	public long getCommId() {
		return commId;
	}

	public long getCountyId() {
		return countyId;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public long getId() {
		return id;
	}

	public void setCommId(long l) {
		commId = l;
	}

	public void setCountyId(long l) {
		countyId = l;
	}

	public void setDueDate(Date date) {
		dueDate = date;
	}

	public void setId(long l) {
		id = l;
	}

}

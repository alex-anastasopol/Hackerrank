package ro.cst.tsearch.bean;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;

public class SearchLogEntry {

	@SuppressWarnings("unused")
	private static final Logger	logger				= Logger.getLogger(SearchLogEntry.class);

	private long				id;
	private long				searchId;
	private Date				loggedAt;
	private String				text;

	public SearchLogEntry(long searchId, String text) {
		super();
		this.searchId = searchId;
		this.text = text;
		this.loggedAt = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public Date getLoggedAt() {
		return loggedAt;
	}

	public void setLoggedAt(Date loggedAt) {
		this.loggedAt = loggedAt;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}

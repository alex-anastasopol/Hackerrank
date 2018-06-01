package ro.cst.tsearch.bean;

import ro.cst.tsearch.database.rowmapper.SearchUpdateMapper;

public class OrderToUpdateResult {
	private SearchUpdateMapper	toUpdateSearchInfo	= null;
	private SearchUpdateMapper	parentSearchInfo	= null;
	private int					finishedUpdates		= 0;
	private String				fileId;

	public SearchUpdateMapper getToUpdateSearchInfo() {
		return toUpdateSearchInfo;
	}

	public void setToUpdateSearchInfo(SearchUpdateMapper toUpdateSearchInfo) {
		this.toUpdateSearchInfo = toUpdateSearchInfo;
	}

	public SearchUpdateMapper getParentSearchInfo() {
		return parentSearchInfo;
	}

	public void setParentSearchInfo(SearchUpdateMapper parentSearchInfo) {
		this.parentSearchInfo = parentSearchInfo;
	}

	public int getFinishedUpdates() {
		return finishedUpdates;
	}

	public void setFinishedUpdates(int finishedUpdates) {
		this.finishedUpdates = finishedUpdates;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getFileId() {
		return fileId;
	}
}

package ro.cst.tsearch.bean;


public class OrdersExportATS2ReportBean{
	private String abstractor;
	private String owners;
	private String agent;
	private String county;
	private String propertyAddress;
	private String tsOrder;
	private String tsDone;
	private String totalTimeWorked;
	private String tsrFileId;
	private String status;
	private String note;
	private String logs;
	
	public OrdersExportATS2ReportBean() { 
		
	}
	
	public OrdersExportATS2ReportBean(String abstractor, 
			String owners, 
			String agent, 
			String county, 
			String propertyAddress, 
			String tsOrder, 
			String tsDone,
			String totalTimeWorked,
			String tsrFileId,
			String status,
			String note,
			String logs) {
		
		this.setAbstractor(abstractor);
		this.setOwners(owners);
		this.setAgent(agent);
		this.setCounty(county);
		this.setPropertyAddress(propertyAddress);
		this.setTsOrder(tsOrder);
		this.setTsDone(tsDone);
		this.setTotalTimeWorked(totalTimeWorked);
		this.setTsrFileId(tsrFileId);
		this.setStatus(status);
		this.setNote(note);
		this.setLogs(logs);
	}

	/**
	 * @return the abstractor
	 */
	public String getAbstractor() {
		return abstractor;
	}

	/**
	 * @param abstractor the abstractor to set
	 */
	public void setAbstractor(String abstractor) {
		this.abstractor = abstractor;
	}

	/**
	 * @return the owners
	 */
	public String getOwners() {
		return owners;
	}

	/**
	 * @param owners the owners to set
	 */
	public void setOwners(String owners) {
		this.owners = owners;
	}

	/**
	 * @return the agent
	 */
	public String getAgent() {
		return agent;
	}

	/**
	 * @param agent the agent to set
	 */
	public void setAgent(String agent) {
		this.agent = agent;
	}

	/**
	 * @return the county
	 */
	public String getCounty() {
		return county;
	}

	/**
	 * @param county the county to set
	 */
	public void setCounty(String county) {
		this.county = county;
	}

	/**
	 * @return the propertyAddress
	 */
	public String getPropertyAddress() {
		return propertyAddress;
	}

	/**
	 * @param propertyAddress the propertyAddress to set
	 */
	public void setPropertyAddress(String propertyAddress) {
		this.propertyAddress = propertyAddress;
	}

	/**
	 * @return the tsOrder
	 */
	public String getTsOrder() {
		return tsOrder;
	}

	/**
	 * @param tsOrder the tsOrder to set
	 */
	public void setTsOrder(String tsOrder) {
		this.tsOrder = tsOrder;
	}

	/**
	 * @return the tsDone
	 */
	public String getTsDone() {
		return tsDone;
	}

	/**
	 * @param tsDone the tsDone to set
	 */
	public void setTsDone(String tsDone) {
		this.tsDone = tsDone;
	}

	/**
	 * @return the tsrFileId
	 */
	public String getTsrFileId() {
		return tsrFileId;
	}

	/**
	 * @param tsrFileId the tsrFileId to set
	 */
	public void setTsrFileId(String tsrFileId) {
		this.tsrFileId = tsrFileId;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the note
	 */
	public String getNote() {
		return note;
	}

	/**
	 * @param note the note to set
	 */
	public void setNote(String note) {
		this.note = note;
	}

	/**
	 * @return the logs
	 */
	public String getLogs() {
		return logs;
	}

	/**
	 * @param logs the logs to set
	 */
	public void setLogs(String logs) {
		this.logs = logs;
	}

	/**
	 * @return the totalTimeWorked
	 */
	public String getTotalTimeWorked() {
		return totalTimeWorked;
	}

	/**
	 * @param totalTimeWorked the totalTimeWorked to set
	 */
	public void setTotalTimeWorked(String totalTimeWorked) {
		this.totalTimeWorked = totalTimeWorked;
	}
	
}

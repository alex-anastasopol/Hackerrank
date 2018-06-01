package ro.cst.tsearch.bean;

import java.util.Date;

public class InvoiceSolomonBeanEntry {

	private String transaction = "";
	private String transactionNoAgent = "";
	private String transactionAmt = "";
	private String transactionDescription = "";
	private String searchId = "";
	private Long agentId = null;
	
	private int invoiced;
	private Date doneTime;
	private boolean isTsrCreated;

	public InvoiceSolomonBeanEntry(){
		
	}
	public InvoiceSolomonBeanEntry(String transaction, String transactionNoAgent, String transactionAmt, String transactionDescription) {
		this.transaction = transaction;
		this.transactionNoAgent = transactionNoAgent;
		this.transactionAmt = transactionAmt;
		this.transactionDescription = transactionDescription;
	}
	
	public String getTransaction() {
		return transaction;
	}
	public void setTransaction(String transaction) {
		this.transaction = transaction;
	}
	public String getTransactionNoAgent() {
		return transactionNoAgent;
	}
	public void setTransactionNoAgent(String transactionNoAgent) {
		this.transactionNoAgent = transactionNoAgent;
	}
	public String getTransactionAmt() {
		return transactionAmt;
	}
	public void setTransactionAmt(String transactionAmt) {
		this.transactionAmt = transactionAmt;
	}
	public String getTransactionDescription() {
		return transactionDescription;
	}
	public void setTransactionDescription(String transactionDescription) {
		this.transactionDescription = transactionDescription;
	}
	public String getRecordIdentifier(){
		return "Transaction";
	}
	public String getSearchId() {
		return searchId;
	}
	public void setSearchId(String searchId) {
		this.searchId = searchId;
	}
	
	public Long getAgentId() {
		return agentId;
	}
	public void setAgentId(Long agentId) {
		this.agentId = agentId;
	}
	public String toString(){
		return getRecordIdentifier() + "," + getTransactionNoAgent() + "," + getTransaction() + "," + 
			getTransactionDescription() + "," + getTransactionAmt();
	}
	/**
	 * @return the invoiced
	 */
	public int getInvoiced() {
		return invoiced;
	}
	/**
	 * @param invoiced the invoiced to set
	 */
	public void setInvoiced(int invoiced) {
		this.invoiced = invoiced;
	}
	/**
	 * @return the doneTime
	 */
	public Date getDoneTime() {
		return doneTime;
	}
	/**
	 * @param doneTime the doneTime to set
	 */
	public void setDoneTime(Date doneTime) {
		this.doneTime = doneTime;
	}
	/**
	 * @return the isTsrCreated
	 */
	public boolean isTsrCreated() {
		return isTsrCreated;
	}
	/**
	 * @param isTsrCreated the isTsrCreated to set
	 */
	public void setTsrCreated(boolean isTsrCreated) {
		this.isTsrCreated = isTsrCreated;
	}
	
}

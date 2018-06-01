package ro.cst.tsearch.bean;

import java.math.BigDecimal;
import java.util.Vector;

public class InvoiceSolomonBean {
	
	public static String OPERATING_ID	=	"opID";
	public static String AGENT_ID	=	"agentId";
	
	public String operatingId = null;
	public Long agentId = null;
	public BigDecimal totalAmount = new BigDecimal(0);
	public String invoiceNumber = null;
	public Vector<InvoiceSolomonBeanEntry> entries = new Vector<InvoiceSolomonBeanEntry>();
	
	public InvoiceSolomonBean(){
		
	}
	public InvoiceSolomonBean(String operatingId, BigDecimal totalAmount, String invoiceNumber, Vector<InvoiceSolomonBeanEntry> entries) {
		super();
		this.operatingId = operatingId;
		this.totalAmount = totalAmount;
		this.invoiceNumber = invoiceNumber;
		this.entries = entries;
		computeTotalAmount();
	}
	public String getInvoiceNumber() {
		return invoiceNumber;
	}
	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}
	public String getOperatingId() {
		return operatingId;
	}
	public void setOperatingId(String operatingId) {
		this.operatingId = operatingId;
	}
	public BigDecimal getTotalAmount() {
		return totalAmount;
	}
	private void computeTotalAmount(){
		for (InvoiceSolomonBeanEntry entry : entries) {
			totalAmount = totalAmount.add(new BigDecimal(entry.getTransactionAmt()));
		}
	}
	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}
	public Vector<InvoiceSolomonBeanEntry> getEntries() {
		return entries;
	}
	public void setEntries(Vector<InvoiceSolomonBeanEntry> entries) {
		this.entries = entries;
		computeTotalAmount();
	}
	public void addEntry(InvoiceSolomonBeanEntry entry){
		totalAmount = totalAmount.add(new BigDecimal(entry.getTransactionAmt()));
		entries.add(entry);
	}
	public String getRecordIdentifier(){
		return "Invoice";
	}
	public String toString(){
		return getRecordIdentifier() + "," + getOperatingId() + "," + getTotalAmount() + "," + getInvoiceNumber();
	}
	public Long getAgentId() {
		return agentId;
	}
	public void setAgentId(Long agentId) {
		this.agentId = agentId;
	}
	
}

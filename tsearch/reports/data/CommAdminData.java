package ro.cst.tsearch.reports.data;

import java.util.Vector;

import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.NameI;

public class CommAdminData {
	private String abstractorName;
	private Vector<NameI> owners = new Vector<NameI>();
	private String agentName;
	private String countyName;
	private String propertyAddress;
	private String tsOrder;
	private String fileName; 
	private String searchStatus; 
	private String abstractorEmail;
	private String rowClass;
	private Long searchId;
	/**
	 * @return the abstractorName
	 */
	public String getAbstractorName() {
		return abstractorName;
	}
	/**
	 * @param abstractorName the abstractorName to set
	 */
	public void setAbstractorName(String abstractorName) {
		this.abstractorName = abstractorName;
	}
	/**
	 * @return the owners
	 */
	public Vector<NameI> getOwners() {
		return owners;
	}
	/**
	 * @param owners the owners to set
	 */
	public void setOwners(Vector<NameI> owners) {
		this.owners = owners;
	}
	/**
	 * @return the agentName
	 */
	public String getAgentName() {
		return agentName;
	}
	/**
	 * @param agentName the agentName to set
	 */
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}
	/**
	 * @return the countyName
	 */
	public String getCountyName() {
		return countyName;
	}
	/**
	 * @param countyName the countyName to set
	 */
	public void setCountyName(String countyName) {
		this.countyName = countyName;
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
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}
	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	/**
	 * @return the searchStatus
	 */
	public String getSearchStatus() {
		return searchStatus;
	}
	/**
	 * @param searchStatus the searchStatus to set
	 */
	public void setSearchStatus(String searchStatus) {
		this.searchStatus = searchStatus;
	}
	/**
	 * @return the abstractorEmail
	 */
	public String getAbstractorEmail() {
		return abstractorEmail;
	}
	/**
	 * @param abstractorEmail the abstractorEmail to set
	 */
	public void setAbstractorEmail(String abstractorEmail) {
		this.abstractorEmail = abstractorEmail;
	}
	/**
	 * @return the rowClass
	 */
	public String getRowClass() {
		return rowClass;
	}
	/**
	 * @param rowClass the rowClass to set
	 */
	public void setRowClass(String rowClass) {
		this.rowClass = rowClass;
	}
	/**
	 * @return the searchId
	 */
	public Long getSearchId() {
		return searchId;
	}
	/**
	 * @param searchId the searchId to set
	 */
	public void setSearchId(Long searchId) {
		this.searchId = searchId;
	}
	public String getOwnersName(){
		StringBuilder sb = new StringBuilder();
		for (NameI name : owners) {
			sb.append(StringUtils.HTMLEntityEncode(name.getFullName()) + "; ");
		}
		if(sb.length()>1)
			return sb.substring(0, sb.length() - 2);
		else
			return "";
	}
}

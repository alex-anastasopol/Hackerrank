package ro.cst.tsearch.reports.invoice;

public class InvoiceDuplicate {
	private long searchId;
	private String fileId;
	private long agentId;
	private int countyId;
	private int productType;
	private String streetNo;
	private String streetName;
	/**
	 * @return the searchId
	 */
	public long getSearchId() {
		return searchId;
	}
	/**
	 * @param searchId the searchId to set
	 */
	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}
	/**
	 * @return the fileId
	 */
	public String getFileId() {
		return fileId;
	}
	/**
	 * @param fileId the fileId to set
	 */
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}
	/**
	 * @return the agentId
	 */
	public long getAgentId() {
		return agentId;
	}
	/**
	 * @param agentId the agentId to set
	 */
	public void setAgentId(long agentId) {
		this.agentId = agentId;
	}
	/**
	 * @return the countyId
	 */
	public int getCountyId() {
		return countyId;
	}
	/**
	 * @param countyId the countyId to set
	 */
	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}
	/**
	 * @return the productType
	 */
	public int getProductType() {
		return productType;
	}
	/**
	 * @param productType the productType to set
	 */
	public void setProductType(int productType) {
		this.productType = productType;
	}
	/**
	 * @return the streetNo
	 */
	public String getStreetNo() {
		return streetNo;
	}
	/**
	 * @param streetNo the streetNo to set
	 */
	public void setStreetNo(String streetNo) {
		this.streetNo = streetNo;
	}
	/**
	 * @return the streetName
	 */
	public String getStreetName() {
		return streetName;
	}
	/**
	 * @param streetName the streetName to set
	 */
	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (agentId ^ (agentId >>> 32));
		result = prime * result + countyId;
		result = prime * result + ((fileId == null) ? 0 : fileId.hashCode());
		result = prime * result + productType;
		result = prime * result + (int) (searchId ^ (searchId >>> 32));
		result = prime * result
				+ ((streetNo == null) ? 0 : streetNo.hashCode());
		result = prime * result
				+ ((streetName == null) ? 0 : streetName.hashCode());
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InvoiceDuplicate other = (InvoiceDuplicate) obj;
		if (agentId != other.agentId)
			return false;
		if (countyId != other.countyId)
			return false;
		if (fileId == null) {
			if (other.fileId != null)
				return false;
		} else if (!fileId.equals(other.fileId))
			return false;
		if (productType != other.productType)
			return false;
		if (searchId != other.searchId)
			return false;
		if (streetNo == null) {
			if (other.streetNo != null)
				return false;
		} else if (!streetNo.equals(other.streetNo))
			return false;
		if (streetName == null) {
			if (other.streetName != null)
				return false;
		} else if (!streetName.equals(other.streetName))
			return false;
		return true;
	}
	
	
}

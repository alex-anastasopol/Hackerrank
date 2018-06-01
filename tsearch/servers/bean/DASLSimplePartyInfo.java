package ro.cst.tsearch.servers.bean;

public class DASLSimplePartyInfo {
	private String firstName;
	private String lastName;
	private String middleName;
	private String vestingType;
	private String partyRole;
	private String comment;
	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}
	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}
	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * @return the middleName
	 */
	public String getMiddleName() {
		return middleName;
	}
	/**
	 * @param middleName the middleName to set
	 */
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}
	/**
	 * @return the vestingType
	 */
	public String getVestingType() {
		return vestingType;
	}
	/**
	 * @param vestingType the vestingType to set
	 */
	public void setVestingType(String vestingType) {
		this.vestingType = vestingType;
	}
	/**
	 * @return the partyRole
	 */
	public String getPartyRole() {
		return partyRole;
	}
	/**
	 * @param partyRole the partyRole to set
	 */
	public void setPartyRole(String partyRole) {
		this.partyRole = partyRole;
	}
	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}
	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((firstName == null) ? 0 : firstName.hashCode());
		result = prime * result
				+ ((lastName == null) ? 0 : lastName.hashCode());
		result = prime * result
				+ ((middleName == null) ? 0 : middleName.hashCode());
		result = prime * result
				+ ((partyRole == null) ? 0 : partyRole.hashCode());
		result = prime * result
				+ ((vestingType == null) ? 0 : vestingType.hashCode());
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
		final DASLSimplePartyInfo other = (DASLSimplePartyInfo) obj;
		if (firstName == null) {
			if (other.firstName != null)
				return false;
		} else if (!firstName.equals(other.firstName))
			return false;
		if (lastName == null) {
			if (other.lastName != null)
				return false;
		} else if (!lastName.equals(other.lastName))
			return false;
		if (middleName == null) {
			if (other.middleName != null)
				return false;
		} else if (!middleName.equals(other.middleName))
			return false;
		if (partyRole == null) {
			if (other.partyRole != null)
				return false;
		} else if (!partyRole.equals(other.partyRole))
			return false;
		if (vestingType == null) {
			if (other.vestingType != null)
				return false;
		} else if (!vestingType.equals(other.vestingType))
			return false;
		return true;
	}
	

}

package ro.cst.tsearch.servers.bean.tims;

import org.apache.commons.lang.StringEscapeUtils;

import com.stewart.ats.base.name.NameI;

public class PayloadName {
	
	private boolean owner = true;
	
	private String _OrganizationName = "";
	private String NonPersonEntityIndicator = "N";
	private String _FirstName = "";
	private String _MiddleName = "";
	private String _LastName = "";
	
	private String _NameSuffix = "";
	private String _SSN = "";
	
	public PayloadName(NameI name) {
		this(name, true);
	}
	public PayloadName(NameI name, boolean owner) {
		this.owner = owner;
		if(name.isCompany()) {
			_OrganizationName = name.getLastName();
			NonPersonEntityIndicator = "Y";
		} else {
			NonPersonEntityIndicator = "N";
			_FirstName = name.getFirstName();
			_MiddleName = name.getMiddleName();
			_LastName = name.getLastName();
		}
	}
	
	public String toXml(){
		StringBuilder sb = new StringBuilder();
		if(owner) {
			sb.append("<SELLER ");
		} else {
			sb.append("<BORROWER ");
		}
		sb.append("_OrganizationName=\"").append(StringEscapeUtils.escapeXml(_OrganizationName)).append("\" ")
			.append("NonPersonEntityIndicator=\"").append(NonPersonEntityIndicator).append("\" ")
			.append("_FirstName=\"").append(StringEscapeUtils.escapeXml(_FirstName)).append("\" ")
			.append("_MiddleName=\"").append(StringEscapeUtils.escapeXml(_MiddleName)).append("\" ")
			.append("_LastName=\"").append(StringEscapeUtils.escapeXml(_LastName)).append("\" ")
			.append("_NameSuffix=\"").append(StringEscapeUtils.escapeXml(_NameSuffix)).append("\" ")
			.append("_SSN=\"").append(StringEscapeUtils.escapeXml(_SSN)).append("\"/>");
		return sb.toString();
	}
}

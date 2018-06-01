package ro.cst.tsearch.bean;

import ro.cst.tsearch.utils.StringUtils;

public class SearchLogRow {
	private String desc = "";
	private String date = "";
	private String gtors = "";
	private String gtees = "";
	private String docType = "";
	private String docNo = "";
	private String comments = "";
	private String ds = "";
	private String searchType = ""; // com.stewart.ats.base.document.DocumentI.SearchType

	public boolean isEmpty() {
		String criteria = (desc + date + gtors + gtees + docType + docNo + comments);
		return StringUtils.isEmpty(criteria);
	}

	public String getDesc() {
		return org.apache.commons.lang.StringUtils.defaultString(desc);
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getGtors() {
		return org.apache.commons.lang.StringUtils.defaultString(gtors);
	}

	public void setGtors(String gtors) {
		this.gtors = gtors;
	}

	public String getGtees() {
		return org.apache.commons.lang.StringUtils.defaultString(gtees);
	}

	public void setGtees(String gtees) {
		this.gtees = gtees;
	}

	public String getDocType() {
		return docType;
	}

	public void setDocType(String docType) {
		this.docType = docType;
	}

	public String getDocNo() {
		return docNo;
	}

	public void setDocNo(String docNo) {
		this.docNo = docNo;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getDs() {
		return ds;
	}

	public void setDs(String ds) {
		this.ds = ds;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = "\n";
		result.append(this.getClass().getName() + " Object {" + NEW_LINE);
		result.append(" DocNo: " + getDocNo() + NEW_LINE);
		result.append(" Date: " + getDate() + NEW_LINE);
		result.append(" Doc Type: " + getDocType() + NEW_LINE);
		result.append(" DS: " + getDs() + NEW_LINE);
		result.append(" Gtees: " + getGtees() + NEW_LINE);
		result.append(" Gtors: " + getGtors() + NEW_LINE);
		result.append(" Comments: " + getComments() + NEW_LINE);
		result.append("}");
		return result.toString();
	}
 
	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public String getLogIntermediary(int index) {
		StringBuilder sb = new StringBuilder();
		sb.append("<tr class='row")
			.append(((index%2)+1))
			.append("' id='")
			.append(String.valueOf(System.nanoTime()))
			.append("_passed'><td>")
			.append(index + 1)
			.append("<td>&nbsp;" ).append(getDs()).append("&nbsp;<br>&nbsp;").append(getSearchType())
			.append("&nbsp;</td><td halign='left'>").append(getDesc())
			.append("</td><td>&nbsp;").append(getDate())
			.append("&nbsp;</td><td>&nbsp;").append(getGtors())
			.append("&nbsp;</td><td>&nbsp;").append(getGtees())
			.append("&nbsp;</td><td>&nbsp;").append(getDocType())
			.append("&nbsp;</td><td>&nbsp;").append(getDocNo())
			.append("&nbsp;</td><td>&nbsp;").append(getComments())
			.append("&nbsp;</td>")
			.append("</tr>");
		
		return sb.toString();
	}
}

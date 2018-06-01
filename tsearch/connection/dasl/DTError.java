package ro.cst.tsearch.connection.dasl;

public class DTError {
	
	String errorCode;
	String level;
	String type;
	String summary;
	String explanation;
	String alternate_message;
	
	public String getErrorCode() {
		return errorCode;
	}
	public String getLevel() {
		return level;
	}
	public String getType() {
		return type;
	}
	public String getSummary() {
		return summary;
	}
	public String getExplanation() {
		return explanation;
	}
	public String getAlternate_message() {
		return alternate_message;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public void setLevel(String level) {
		this.level = level;
	}
	public void setType(String type) {
		this.type = type;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}
	public void setAlternate_message(String alternate_message) {
		this.alternate_message = alternate_message;
	}
}

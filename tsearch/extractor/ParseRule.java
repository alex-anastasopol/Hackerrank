package ro.cst.tsearch.extractor;

public class ParseRule{
	private String propertyIdentification;
	private String stringRule;
	private int stringLength[];
	
	public ParseRule(String propertyIdentification,String stringRule,int [] stringLength){
		this.propertyIdentification = propertyIdentification;
		this.stringRule = stringRule;
		this.stringLength = stringLength;
	}
	public String getStringRule() {
		return stringRule;
	}
	public void setStringRule(String stringRule) {
		this.stringRule = stringRule;
	}
	public String getPropertyIdentification() {
		return propertyIdentification;
	}
	public void setPropertyIdentification(String propertyIdentification) {
		this.propertyIdentification = propertyIdentification;
	}
	
	public int[] getStringLength() {
		return stringLength;
	}
	
	public void setStringLength(int[] stringLength) {
		this.stringLength = stringLength;
	}
}

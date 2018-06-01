package ro.cst.tsearch.parentsitedescribe;

import java.util.LinkedList;

public class Param {
	private String value="";
	private String name="";
	private String type="";
	private String saKey="";
	private int  ParcelID=0;
	private int iteratorType=-1;
	private String hiddenName="";
	private String hiddenValue="";
	private String validationType="";
	
	public void replace(Param p){
		if(p!=null){
			this.setName(p.getName());
			this.setType(p.getType());
			this.setValue(p.getValue());
			this.setIteratorType(p.getIteratorType());
			this.setSaKey(p.getSaKey());
			this.setParcelID(p.getParcelID());
			this.setHiddenName(p.getHiddenName());
			this.setHiddenValue(p.getHiddenValue());
		}
	}
	
	
	public Param toEscape(){
		Param tmp=this;
		tmp.value=escapeString(tmp.value);
		tmp.name=escapeString(tmp.name);
		tmp.type=escapeString(tmp.type);
		tmp.saKey=escapeString(tmp.saKey);
		tmp.hiddenName=escapeString(tmp.hiddenName);
		tmp.hiddenValue=escapeString(tmp.hiddenValue);
		tmp.validationType=escapeString(tmp.validationType);
		return tmp;
	}
	public String escapeString(String escape){

		escape=escape.replaceAll( "&amp;","&");
		escape=escape.replaceAll("&quot;","\"");
		escape=escape.replaceAll("&apos;","'");
		escape=escape.replaceAll( "&lt;","<");
		escape=escape.replaceAll( "&gt;",">");

		
		escape=escape.replaceAll("&", "&amp;");
		escape=escape.replaceAll("\"", "&quot;");
		escape=escape.replaceAll("'", "&apos;");
		escape=escape.replaceAll("<", "&lt;");
		escape=escape.replaceAll(">", "&gt;");
		
		
		
		
		return escape;
	}
	public String getHiddenName() {
		return hiddenName;
	}
	public void setHiddenName(String hiddenName) {
		this.hiddenName = hiddenName;
	}
	public String getHiddenValue() {
		return hiddenValue;
	}
	public void setHiddenValue(String hiddenValue) {
		this.hiddenValue = hiddenValue;
	}
	public int getIteratorType() {
		return iteratorType;
	}
	public void setIteratorType(int iteratorType) {
		this.iteratorType = iteratorType;
	}
	public int getParcelID() {
		return ParcelID;
	}
	public void setParcelID(int parcelID) {
		ParcelID = parcelID;
	}
	public String getSaKey() {
		return saKey;
	}
	public void setSaKey(String saKey) {
		this.saKey = saKey;
	}
	public Param() {
		super();
		value="";
		name="";
		type="";
		saKey="";
		ParcelID=0;
		iteratorType=-1;
		hiddenName="";
		hiddenValue="";
		validationType="";
		// TODO Auto-generated constructor stub
	}
		public Param(String  value, String name, String type) {
			super();
			this.value = value;
			this.name = name;
			this.type = type;
		}
		
		public Param getParam(){
			return this;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name =name;
		}
		
		public String getType() {
			return type;
		}
		
		public void setParam(Param para){
			this.name=para.name;
			this.type=para.type;
			this.value=para.value;
		}
		
		public void setType(String type) {
			this.type = type;
		}
		
		public String getValue() {
			return value;
		}
		
		public void setValue(String value) {
			this.value = value;
		}
		
		public boolean equal(Param obj2 ){
			if((this.name.compareTo(obj2.name)==0)&&(this.value==obj2.value)&&(this.type.compareTo(obj2.getType())==0)){
				return true;
			}
			else{
				return false;
			}
		}

		public String getValidationType() {
			return validationType;
		}

		public void setValidationType(String validationType) {
			this.validationType = validationType;
		}
		
}
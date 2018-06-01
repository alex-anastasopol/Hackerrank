package ro.cst.tsearch.search.name.companyex;

public class CompanyObject {
	public CompanyObject(){
		this.companyName="";
		this.companyValue="";
	}
	public CompanyObject(String companyName,String companyValue){
		this.companyName=companyName;
		this.companyValue=companyValue;
	}
private String companyName;
private String companyValue;
public String getCompanyName() {
	return companyName;
}
public void setCompanyName(String companyName) {
	this.companyName = companyName;
}
public String getCompanyValue() {
	return companyValue;
}
public void setCompanyValue(String companyValue) {
	this.companyValue = companyValue;
}
}

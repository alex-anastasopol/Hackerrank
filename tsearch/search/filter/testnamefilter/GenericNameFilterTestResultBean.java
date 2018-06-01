package ro.cst.tsearch.search.filter.testnamefilter;

import java.io.Serializable;

public class GenericNameFilterTestResultBean implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	String firstName, lastName, middleName, result;
	public void setFirstName(String s){
		firstName = s;
	}
	public String getFirstName(){
		return firstName;
	}
	public void setMiddleName(String s){
		middleName = s;
	}
	public String getMiddleName(){
		return middleName;
	}
	public void setLastName(String s){
		lastName = s;
	}
	public String getLastName(){
		return lastName;
	}
	public void setResult(String s){
		result = s;
	}
	public String getResult(){
		return result;
	}	
}

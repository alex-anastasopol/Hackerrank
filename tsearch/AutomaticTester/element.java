package ro.cst.tsearch.AutomaticTester;

import java.util.Vector;



/*Stores the elements that should contain the parameter of the link and it's value
 * 
 * 
 * */
public class element {
	
	//the name of the name
	String parameterName;
	
	//the values of the parameter Values
	Vector parameterValues  = new Vector();
	
	
	//gets the value of the parameter parameterName
	public String getParameterName(){
		
		return parameterName;
	}
	
	//set the value of the parameter parameterName
	public void setParameterName(String pname){
		
		parameterName = pname;
	}

	//get the value of the parameter Values vector
	public Vector getParameterValues(){
		
		return parameterValues;
	}
	
	//set the values of the parameters
	public void setParameterValues(String pvalues){
		
		parameterValues.add(pvalues);
	}
	
}

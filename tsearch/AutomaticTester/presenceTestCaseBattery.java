package ro.cst.tsearch.AutomaticTester;

import java.util.Vector;

/*
 * battery with test cases for one automatic search
 * */
public class presenceTestCaseBattery {
	
	
	private Vector battery = new Vector();
	
	
	void setBattery( Vector Battery ){
	//set a test battery	
		battery =  Battery;
	}
	
	public Vector loadTestBattery(){
	//loads a tet battery from the object 
		return battery; 
	}
	

}

package ro.cst.tsearch.AutomaticTester;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;

import de.schlichtherle.io.FileWriter;

/*
 * class used to enter test data for testing the cases when the pages retained are diffeerent from 
 * the pages response from the server  
 * 
 * */

public class testDataEnter {
	
	
	public static String testDataEnter(String enterData){
		
	
		String imputFromFile = "";
		
		
		//write the string to a file
		 try {
		        BufferedWriter out = new BufferedWriter(new FileWriter("DataTest"));
		        out.write(enterData);
		        out.close();
		    } catch (IOException e) {
		    	
		    	System.out.println(" Error at output data ");
		    }
		    
		
		//transform the file 
		String x = "transform";
		    
		
		//read the file 
		    try {
		        BufferedReader in = new BufferedReader(new FileReader("DataTest"));
		        String str;
		        while ((str = in.readLine()) != null) {
		        	imputFromFile = imputFromFile + str;
		        }
		        in.close();
		    } catch (IOException e) {
		    	
		    	System.out.println(" Error at data imput ");
		    }
		
		    
		return imputFromFile;
	}

	
}

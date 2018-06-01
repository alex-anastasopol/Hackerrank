package ro.cst.tsearch.AutomaticTester;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.DBConstants;



/*
 * class used to enter and extract test cases in the data base   
 * contains methods for operation with the data base
 * 
 * */

public class DatabaseTestCaseStoring {
	
	
	
	 private Document documentRead; 
	 //Contains the Dom of the parsed XML 

	 //loads a test case from the DB
	 private testCase loadTestCaseFromDb = new testCase();
	
     private Vector vecLoadTestCaseFromDb = new Vector();

	 
	 
	 
	 public Vector getTestCaseFromDb(){
	  //return the test case	  
		  
		  return vecLoadTestCaseFromDb;
	  }
	
	
	/*
	 * Extracts a vector of data from the data base and stores the vector in vDatabaseTestCase 
	 * the vector can be then acessed with a getter method 
	 * the method returns true if it performes in the expected way and false if it performs in an unexpected way
	 * */
	public  void  readFileFromDataBase(){
		
		DBConnection conn = null;
	   

	
		
		try {
		
			
		 
			
			conn = ConnectionPool.getInstance().requestConnection();
			
			String sql = " SELECT "
				+ " * " + " FROM "
				+ DBConstants.TABLE_PARENTSITE_TESTS //+ // 
				//+ " WHERE  " + DBConstants.FIELD_PARENTSITE_TESTS_TEST_ID 
				+ " limit 30" ;
			    //+ " limit 20" 
			PreparedStatement pstmt = conn.prepareStatement(sql);
			
			
			ResultSet rs = pstmt.executeQuery();
			
			int count = 0;
			
			while(rs.next()){
			
			count++;	
				
			ByteArrayInputStream  testD = new	ByteArrayInputStream( rs.getBytes(2) );
			int testId = rs.getInt(1);
			
			String strServerName = rs.getString(3);
			
			
			String enableOrDesable = rs.getString(5);
			
			
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        factory.setValidating(true);   
	        factory.setNamespaceAware(true);
	     
	        try{
	        
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        
	     
	        
	        documentRead = builder.parse( testD );     //read the DOM from the file with data from Automatic Test
	        
	        //documentReadTestID = builder.parse(testId); //read the id of the test case 

	        
	        }
	        catch(Exception e){
	        	
	        	System.out.println("readFileFromDataBase");
	        	
	        }
	        
	        testCase loadTestCaseFromDb = new testCase();
	        //======= write the data in the vector =====================
	        
	        //adds test cases to the vector 
	        //vDatabaseTestCase.add(documentRead);
	        loadTestCaseFromDb.setDocumentReadTestID(documentRead);
	        //getDatabaseTestCase().add(documentRead);
	        
	        //adds the id's of the cases
	        //adds test cases to the vector 
	        loadTestCaseFromDb.setVtestID(testId);
	        //getDatabaseTestID().add(testId);
	        
	        
	        loadTestCaseFromDb.setVServerNAME(strServerName);
	        //getDatabaseTestID().add(strServerName);
	        
	        loadTestCaseFromDb.setState(enableOrDesable);
	        //load the state of the case  
	       
	        vecLoadTestCaseFromDb.add(loadTestCaseFromDb);
	 
	        
	        //write a file for test 
	        //======================================================================
	        
	        try{
	        
	        //create a file  
			// File f = new File("fisierVerificare"+ count );
	        
	   	     //create a transformation object
			 //TransformerFactory tFactory = TransformerFactory.newInstance();

		    // Transformer transformer = tFactory.newTransformer();
		     
		     //makes the source object for the transformation 
		    // DOMSource source = new DOMSource(documentRead);
		 
		     //writes the result    
		    // StreamResult result = new StreamResult(f);
		     
		     //makes the transformation 
		     //transformer.transform(source, result);
		     
		     //writes to the data base
		     //DatabaseTestCaseStoring.editFileEntryInDB(f);
		     
		     //DatabaseTestCaseStoring.readFileFromDataBase();
	        
	        }catch(Exception e){
	        	
	        	System.out.println("Exception at writting a test file ");
	        }
		     
	        //======================================================================
	        
		///	return blob;
	
			}
			
		} catch (BaseException e) {
			e.printStackTrace();
			//return false;
		} catch (SQLException e) {
			e.printStackTrace();
			//return false;
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
			}
		}
		
	
		
	}

	
	
	/*
	 *  function used to enter data in the database 
	 *  receives a file as imput and enters the record in the data base
	 * 
	 * */
	public boolean editFileEntryInDB(File fileToStore1 , String classNAME) {
		//it should be added parameters to the file - the columns with the ServeName , TestCaseCreationDAte , EnabledOrDesabled

		DBConnection conn = null;
		
		String stm = "";
		
		try {
			
			conn = ConnectionPool.getInstance().requestConnection();
			conn.setAutoCommit(false);
			
			if (fileToStore1 != null) {

			//insert classNAME - ServerName - , TestCaseCreationDate , default Enable 
				stm = "INSERT INTO " + DBConstants.TABLE_PARENTSITE_TESTS
						+ " ( " + DBConstants.FIELD_PARENTSITE_TESTS_TEST_DATA + ", " + DBConstants.FIELD_SERVERNAME + ", " + DBConstants.FIELD_TEST_CASE_CREATION_DATE + ", " + DBConstants.FIELD_ENABLED_OR_DISABLED
						+ " ) " + " VALUES (?, ?, ?, ?) ";

		
				PreparedStatement pstmt = conn.prepareStatement(stm);
				
				FileInputStream fis1 = null;
			
				if (fileToStore1 != null) {
					fis1 = new FileInputStream(fileToStore1);
					
					pstmt.setBinaryStream(1, fis1, (int) fileToStore1.length());
		
					pstmt.setString( 2 , classNAME );
					
					//-------------------------------------
					//calculate the date
					  String resultat = "";

					  Date maDate = new Date(); 
					  Calendar c = Calendar.getInstance();
					  c.setTime(maDate);

					  resultat += c.get(Calendar.DATE);
					  resultat += "/" + c.get(Calendar.MONTH);
					  resultat += "/" + c.get(Calendar.YEAR); 
					
					pstmt.setString( 3 , resultat );
					//-------------------------------------
					
					//set enabled the string
					pstmt.setString( 4 , "1" );
					
				}

				pstmt.executeUpdate();
               
				pstmt.close();

				if (fis1 != null)
					fis1.close();

				conn.commit();
               //This is where i try to insert the new test case in the vector with test cases and start the thread
			     long id = DBManager.getLastId(conn);
			     PresenceTesterManager APM = PresenceTesterManager.getInstance(-2);
                 APM.addTestCase(id);
                 
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (BaseException e) {
			e.printStackTrace();
			return false;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
			}
		}
		return true;
	}
public testCase readFileFromDataBase(long id){
	DBConnection conn = null;
	testCase loadTestCaseFromDb = null;
	try {	
		loadTestCaseFromDb = new testCase();
		conn = ConnectionPool.getInstance().requestConnection();
		String sql = " SELECT "+ " * " + " FROM "+ DBConstants.TABLE_PARENTSITE_TESTS+" WHERE test_id="+ id + "";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		ResultSet rs = pstmt.executeQuery();
		rs.next();
		ByteArrayInputStream  testD = new	ByteArrayInputStream( rs.getBytes(2) );
		int testId = rs.getInt(1);
		String strServerName = rs.getString(3);
		  loadTestCaseFromDb.setVServerNAME(strServerName);
	//	String enableOrDesable = "1";
		String enableOrDesable = rs.getString(5); 
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);   
        factory.setNamespaceAware(true);
        
        
        try{
       	        
       	        DocumentBuilder builder = factory.newDocumentBuilder();
       	        documentRead = builder.parse( testD );     //read the DOM from the file with data from Automatic Test

       	    }
        catch (Exception e)
       	        {
       	        	System.out.println("readFileFromDataBase");
       	        }
        loadTestCaseFromDb.setDocumentReadTestID(documentRead);
        loadTestCaseFromDb.setVtestID(testId);
      
        loadTestCaseFromDb.setState(enableOrDesable);

	        
}catch (BaseException e) {
	e.printStackTrace();
	//return false;
} catch (SQLException e) {
	e.printStackTrace();
	//return false;
} finally {
	try {
		ConnectionPool.getInstance().releaseConnection(conn);
	} catch (BaseException e) {
	}
}return loadTestCaseFromDb;
}

}
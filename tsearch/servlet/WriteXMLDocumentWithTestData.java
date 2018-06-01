package ro.cst.tsearch.servlet;

import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ro.cst.tsearch.AutomaticTester.DatabaseTestCaseStoring;
import ro.cst.tsearch.AutomaticTester.PageAndIndexOfLink;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;

/*
 * Class used to write automatic search test cases data to xml files 
 * 
 * 
 * */

public class WriteXMLDocumentWithTestData {

	/*
	 * Function to add data from the test cases to xml files that would be saved in data base in order to perform 
	 * automatic testing using the automatic tests procedure
	 * Imput :
	 * Vector  V    - vector that represents the succession of pages and the indexes of the link that was cliked on 
	 * int modId       - the id of the module 
	 * functionList - list with functions , paramaters and values that can be used along with the module id to determine 
	 *                        the start module of the search 
	 * noOfElementsFunctionList - number of elements in the function list                    
	 * */
	
	public static void writeTestData( Vector v , int modId , List<TSServerInfoFunction> functionList , int noOfElementsFunctionList,String p1,String p2,String classNAME,String msSiteRealPath,long SearchID ){
		
		
		try {	
			
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	    documentBuilderFactory.setValidating(true);
	    documentBuilderFactory.setNamespaceAware(true);
	    documentBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage" , "http://www.w3.org/2001/XMLSchema");
	    
	    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

	    Document document = documentBuilder.newDocument();
	    
	    //appends the root
	    Element rootDocument = document.createElement("Document");
		document.appendChild(rootDocument);
		
		//creates an element to follow the root 
	    Element rootModule = (Element) document.createElement("module"); 
	    
	    //appends the root
	    rootDocument.appendChild( rootModule );
	    
	    //the id of the module as a string
	    String id = "" + modId;
	    
	    //sets the attribute of the module 
	    rootModule.setAttribute( "id" , id );
		
	    //creates an tag with function
	    
	    Element rootPath = (Element) document.createElement("Path"); 
	    rootDocument.appendChild( rootPath );
	    
	    try{
	    	
	    //adds the elements from the vector to the xml
	    for (int i = 0; i < v.size(); i++) {
	        
	    	//creates an tag with function
	    	Element rootPageAndIndex = (Element) document.createElement("PageAndIndex"); 
	    	rootPath.appendChild(rootPageAndIndex);
	    	
	    	//creates an element to follow the root 
	        Element rootIndex = (Element) document.createElement("index"); 
	        //appends the root
	        rootPageAndIndex.appendChild( rootIndex );
	        
	        PageAndIndexOfLink elm =  (PageAndIndexOfLink)(v.elementAt(i));
	        //the index of the link
	        String indexOfLinkInPage = "" + elm.getIndex()  ;
	        
	        //sets the attribute of the module 
	        rootIndex.setAttribute( "value" ,  indexOfLinkInPage );
	        
	        //the id of the module as a string
	        String contentsOfPage = ""  + elm.getPage() ;
	     
	        Element rootPage = (Element) document.createElement("Page"); 
	        
	        //sets the text
	        rootPage.setTextContent(contentsOfPage);
	        
	        //appends the root
	        rootPageAndIndex.appendChild( rootPage );
	         	
	    	}
	    
	    
	    Element rootFunctionList = (Element) document.createElement("FunctionList"); 
	    rootDocument.appendChild( rootFunctionList );
	    
	    //adds the elements fron the function list to the xml 
	    for (int i = 0; i < functionList.size(); i++) {
	    	
	    	Element rootFunctionParameterNameAndParameterValues = (Element) document.createElement("FunctionParameterNameAndParameterValues"); 
	    	rootFunctionList.appendChild( rootFunctionParameterNameAndParameterValues );
	    	
	    	//String functionParameterName =  functionList.get(i)  ;
	    	TSServerInfoFunction TSServerInfoFunctionData =  (TSServerInfoFunction) ( functionList.get(i) );
	        
	    	//get the name of the parameter
	        String functionParameterName = "" + TSServerInfoFunctionData.getParamName()  ;
	    	
	        //sets the ParameterName of the function as atributte
	    	rootFunctionParameterNameAndParameterValues.setAttribute( "ParameterName" , functionParameterName  );
	    	
	    	
	    	String functionParameterValues = "" + TSServerInfoFunctionData.getParamValue();
	    	
	    	//sets the ParameterValue of the function as atributte
	    	rootFunctionParameterNameAndParameterValues.setAttribute( "ParameterValues" ,  functionParameterValues );
	    	
	    }
	    
	    
	    //enter the paramters for the instantiation of the TSInterface
	    
	    Element rootTSInterfaceInstatiation = (Element) document.createElement("TSInterfaceInstatiation"); 
	    rootDocument.appendChild( rootTSInterfaceInstatiation );
	    
	    //p1-------------------------------------
	    
	    
    	Element rootTSInterfaceInstatiationP1 = (Element) document.createElement("TSInterfaceInstatiationP1"); 
    	rootTSInterfaceInstatiation.appendChild( rootTSInterfaceInstatiationP1 );
    	
        //sets the p1 as atributte
    	rootTSInterfaceInstatiationP1.setAttribute( "ParameterName" ,  "p1"  );
    	
    	//sets the parameter p1 as value
    	rootTSInterfaceInstatiationP1.setAttribute( "ParameterValues" , p1  );
	    
    	//p2---------------------------------------
	   
	   
    	Element rootTSInterfaceInstatiationP2 = (Element) document.createElement("TSInterfaceInstatiationP2"); 
    	rootTSInterfaceInstatiation.appendChild( rootTSInterfaceInstatiationP2 );
    	
        //sets the parameter p2 as attribute
    	rootTSInterfaceInstatiationP2.setAttribute( "ParameterName" ,  "p2"  );
    	
    	//sets the parameter p2 as a value
    	rootTSInterfaceInstatiationP2.setAttribute( "ParameterValues" , p2  );
    	
    	//searchID---------------------------------
    	
    	Element rootTSInterfaceInstatiationSerachID = (Element) document.createElement("TSInterfaceInstatiationSearchID"); 
    	rootTSInterfaceInstatiation.appendChild( rootTSInterfaceInstatiationSerachID );
    	
        //sets the Search ID as parameter
    	rootTSInterfaceInstatiationSerachID.setAttribute( "ParameterName" ,  "SearchID"  );
    	
    	String paramSearchID = "" + SearchID ;
    	
    	//sets the value as a parameter 
    	rootTSInterfaceInstatiationSerachID.setAttribute( "ParameterValues" ,  paramSearchID );
    	
    	//server name------------------------------- used for the instatiation of the TSInterface
    	
    	Element rootTSInterfaceInstatiationclassNAME = (Element) document.createElement("TSInterfaceInstatiationclassNAME"); 
    	rootTSInterfaceInstatiation.appendChild( rootTSInterfaceInstatiationclassNAME );
    	
        //sets the name of the class as parameter 
    	rootTSInterfaceInstatiationclassNAME.setAttribute( "ParameterName" ,  "classNAME"  );
    	
    	//sets the parameter value as attribute
    	rootTSInterfaceInstatiationclassNAME.setAttribute( "ParameterValues" ,  classNAME );
    	
    	
    	//msSiteRealPath--------------------------- used for the instantiation of TSInterfece
    	
    	Element rootTSInterfaceInstatiationMsSiteRealPath = (Element) document.createElement("TSInterfaceInstatiationMsSiteRealPath"); 
    	rootTSInterfaceInstatiation.appendChild( rootTSInterfaceInstatiationMsSiteRealPath );
    	
        //sets the Parameter Name as atributte
    	rootTSInterfaceInstatiationMsSiteRealPath.setAttribute( "ParameterName" ,  "msSiteRealPath"  );
    	
    	//sets the Parameter Value  as atributte
    	rootTSInterfaceInstatiationMsSiteRealPath.setAttribute( "ParameterValues" ,  msSiteRealPath );
    	
    	
	    
	    }
	    catch(Exception e )
	    {
	    	System.out.println("Exception at writting the data in the DOM");
	    }
	    
	   
	    
		 //create a file  
		 File f = new File("hello1234");
		 
		 //create a transformation object
		 TransformerFactory tFactory = TransformerFactory.newInstance();

	     Transformer transformer = tFactory.newTransformer();
	     
	     //makes the source object for the transformation 
	     DOMSource source = new DOMSource(document);
	 
	     //writes the result    
	     StreamResult result = new StreamResult(f);
	     
	     //makes the transformation 
	     transformer.transform(source, result);
	     
	     //writes to the data base
	     DatabaseTestCaseStoring dTcS = new DatabaseTestCaseStoring();
	     
	     dTcS.editFileEntryInDB(f,classNAME);
	     
	     
		
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	
	    	System.out.println("Exception at writting the data in the XML on Automatic Test Data Save Procedure");
	    
	    }
		
		
		}
	
		
}

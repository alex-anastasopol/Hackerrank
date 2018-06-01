package ro.cst.tsearch.AutomaticTester;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.servers.info.TSServerInfoFunction;

public class testDataFromDom {
	
	   
    Document documentRead; 
    //Contains the Dom of the parsed XML   
    
    //============================       class data
    
    	public testDataFromDom(Document docRead){
    		
    		documentRead = docRead;
    	}
    
		//get the function list 
		//data 1
		private ArrayList functionList = new ArrayList();
    
		//get the module ID
		//data 2
		private int moduleId;
    
		//get the succesion of pages and indexes as vector
		//data 3
		private Vector PagesVector = new Vector();
		
		//get the last page as a string
		//data 4
		private String sirLastPage;
		
		//first parameter of the TSinterface constructor
		//data 5
		private String p1;
		
		//seccond parameter of the TSinterface constructor
		//data 6
		private String p2;
		
		//third parameter of the TSinterface constructor
		//data 7 
		private long SearchID;
		
		//forth parameter of the TSinterface constructor
		//data 8 
		private String classNAME;
		
		//fifth parameter of the TSinterface constructor
		//data 9
		private String msSiteRealPath;
		
		
		//========================= geter methods ==============================
		
		public ArrayList  getFunctionList(){
		//returns the function list 	
			return functionList;
		}
		
		
		public int getModuleId(){
		//returns the id of the module 
			return moduleId;
		}
		
		
		public Vector getPagesVector(){
		//returns the vector with the pages and indexes of the links
			return PagesVector;
		}
		
		
		public String getLastPage(){
		//returns the last page when SAVE AS occurs
			return sirLastPage;
		}
		
		
		public String getP1(){
		//returns the p1 parameter	
			return p1;
		}
		
		
		public String getP2(){
		//returns the p2 parameter
			return p2;
		}
		
		
		public long getSearchID(){
		//returns the searchID
			return SearchID;
		}
		
		
		public String getClassNAME(){
		//returns the name of the server 
			return classNAME;
		}
		
		
		public String getMsSiteRealPath(){
		//returns the path to the work directory 
			return msSiteRealPath;
		}
		
	//============================= processing methods ======================================
		
    public void testDataReaderFromDom(){
    	 
     /*         Reads the XML file and parse the XML in a DOM  , then extract the relevant fields from the DOM object so that the data can be used to be saved in the data base 
     *           
     *           The test cases may be compared with the data that resulted from automatic search procedure 
     * */
   	 
       DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
       factory.setValidating(true);   
       factory.setNamespaceAware(true);
       
      try{
       		
         //read the DOM from the file with data from Automatic Test
          
          
          Node rootDocument = documentRead.getFirstChild();
          
          //gets the child nodes
          NodeList searchData = rootDocument.getChildNodes();
          
          //iterate through the list of nodes 
          for (int i = 0; i < searchData.getLength(); i++ )
			{
       	   //obtain the current node 
       	   Node currentNode = searchData.item(i);
       	   
       	   if (currentNode.getNodeType() == Node.ELEMENT_NODE )				
				{
       		  //obtain the name of the current node  
       		  String nodeName = currentNode.getNodeName();
       		  
       		  //if the node name is module
       		  if( "module".equals(nodeName) ){
       			 	        	        
       			  //get the list of attributes
       			  NamedNodeMap moduleAttributeList = currentNode.getAttributes();
       			  
       			  //get the value of the attribute
       			  String valueAttr = moduleAttributeList.item(0).getNodeValue();
       			  
       			  //set the module id 
       			  moduleId = Integer.parseInt(valueAttr.trim()); 
       			  
       		  }
       		  else
       			  //if the node name is path 
       			  if( "Path".equals(nodeName) ){	
       				 
       				  NodeList pagesAndIndexNo = currentNode.getChildNodes();
       				  
       				  for( int k = 0; k < pagesAndIndexNo.getLength() ; k++ )
	                        {
	                   
       					  //gets the values of the strings that represent the page 
       					    
       					  //extract the page  
	                          String pageString = pagesAndIndexNo.item( k ).getTextContent() ;
	                           
	                          //extract the index
	                          
	                          //get the list of attributes
	                          NamedNodeMap pagesAndIndexAttributeList = pagesAndIndexNo.item( k ).getChildNodes().item(0).getAttributes();
	                          
		        			  //get the value of the attribute - the index 
		        			  String valueAttrIndex = pagesAndIndexAttributeList.item(0).getNodeValue();
	                        
		        			  //get the index in a numeric form
		        			  int indexNo = Integer.parseInt(valueAttrIndex.trim());
		        			
	                          //create an object PageAndIndex with the page and the index obtained 
		        			  PageAndIndexOfLink pAndLink = new PageAndIndexOfLink(indexNo,pageString);
	                            
	                          //enter the PageAndIndex object in the Vector with PageAndIndex Objects
		        			  PagesVector.add(pAndLink);	                            
	                        }
       				  
       			  }
       			  else//the case with the function parameters data
       				  if( "FunctionList".equals(nodeName) ){
       					  
	        				  NodeList functionListValues = currentNode.getChildNodes();
	        				  
	        				  for( int k = 0; k < functionListValues.getLength() ; k++ )
		                        {
		                            NamedNodeMap moduleAttributeList = functionListValues.item( k ).getAttributes();
		                            
       			  				//get the value of the ParameterName attribute
       			  				String valueAttr0 = moduleAttributeList.item(0).getNodeValue();
       			  				
       			  				//get the value of the ParameterValue attribute
       			  				String valueAttr1 = moduleAttributeList.item(1).getNodeValue();
       			  				
       			  				//process the strings and enter the strings in a ArrayList
       			  				TSServerInfoFunction TSServerInfoFunctionData = new TSServerInfoFunction();
       			  				
       			  				//set the parameter name
       			  				TSServerInfoFunctionData.setParamName(valueAttr0);
       			  				
       			  				//set the parameter value
       			  				TSServerInfoFunctionData.setParamValue(valueAttr1);
		                         
       			  				
       			  				//set the value in the list 
       			  				functionList.add( ( (TSServerInfoFunction)TSServerInfoFunctionData ) );
       			  				
		                        }
       				  }
       				  else//the case with the TSInterface data
       					  if( "TSInterfaceInstatiation".equals(nodeName) ){
       						  
       						  NodeList tsInterfaceInstatiationList = currentNode.getChildNodes();
       						  
		        				  for( int k = 0; k < tsInterfaceInstatiationList.getLength() ; k++ )
			                        {
			                            NamedNodeMap moduleAttributeList = tsInterfaceInstatiationList.item( k ).getAttributes();
			                            
	        			  				//get the value of the ParameterName attribute
	        			  				String valueAttr0 = moduleAttributeList.item(0).getNodeValue();
	        			  				
	        			  				//get the value of the ParameterValue attribute
	        			  				String valueAttr1 = moduleAttributeList.item(1).getNodeValue();
	        			  				
	        			  				if(  valueAttr0.equals("p1")  )
	        			  					p1 = valueAttr1;
	        			  				else
	        			  					if( valueAttr0.equals("p2") )
	        			  						p2 = valueAttr1;
	        			  					else
	        			  						if( valueAttr0.equals("SearchID")  )
	        			  							SearchID = Long.parseLong(valueAttr1);
	        			  						else
	        			  							if( valueAttr0.equals("classNAME")  )
	        			  								classNAME = valueAttr1;
	        			  							else
	        			  								if( valueAttr0.equals("msSiteRealPath") )
	        			  									msSiteRealPath = valueAttr1;
	        			  				
			                      
			                        }
       						  
       						  
       					  }
				}
			}
      }
      catch(Exception e )
	    {
	    	System.out.println("Exception at reading the data in the DOM");
	    }
       
   } 


}

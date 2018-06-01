package ro.cst.tsearch.AutomaticTester;

import java.util.Vector;

import ro.cst.tsearch.servers.types.LinkProcessing;

public class resultCompare {
	/*
	 * Class used to compare the Strings that represent the pages that come as response of the server 
	 * The steps in performing the comaparison are outlined with the numbers
	 * 
	 * */
	
	
	
	/*Process the content of the final page so that the pages can be compared
	 * The imput is the string representing the form
	 * The output is the processed string 
	 * */
	public static String processContent(String content){
		//process the content if necessary
		String responseContent = "";
		
		//find the index of ReturnUrl
		int indexReturnUrl = content.indexOf("ReturnUrl");
	
		//find index of the " after the ReturnUrl
		int indexQ = content.indexOf( "\"" , indexReturnUrl );
		
		//create a substring
		if( indexReturnUrl > -1 &&  indexQ > -1){
		
		String subContent = content.substring( indexReturnUrl , indexQ );
		
		//extract the content of the substring  form the content of the page
		content = content.replace(  subContent , "" );
		}
		
		return content;
	}
	
	/*Compares two vectors to determine if they contain the same elements
	 * 
	 * */
	public static boolean compVectString(Vector v1,Vector v2){
		
		boolean com = true;
		
		if( v1.size() != v2.size() )
			
			return false;
		else
		{		
		
		boolean exist = true;
			
		for(int i=0 ; i < v1.size() ; i++){
		//test if element i does exist in the seccond vector
			String s1 = (String) v1.elementAt(i);
			
			exist = false;
			
			for(int j=0; j < v2.size() ; j++ ){
			
				String s2 = (String) v2.elementAt(j);
				
				if( s1.equals(s2) == true ){
				   
				  exist = true ;	
					
				   v2.setElementAt( null , j);
					
				   break;
				}
				
			}
			
			com = exist;
			
		}
		
		}
		
	   if( com == false )
		   System.out.println(" falssssssss ");
		
		return com;
	}
	
	/*
	 * Process the vector with the links
	 * vectorWithParametersAndValues - the vector with the parameters as imput
	 * 
	 */
	public static Vector processLinks( Vector vecLine ){
		
	    Vector lineVector = new Vector();
	    
		//9. Store in a vector with the unique parameters and values
		Vector vectorWithUniqueParameterName = new Vector();
		
		//for(int i=0; i < vectorWithParametersAndValues.size() ;i++){
		
			Vector lineWithNewElements = new Vector();
			
			//find the line
			//Vector vecLine = new Vector();
			
			//extract a line
			//vecLine = (Vector) vectorWithParametersAndValues.elementAt(i);
			
			//iterate on the line
			for(int j=0; j < vecLine.size() ;j++ ){
			
			  element elToAdd = new element();
			
			  //create a string array[]
			  String e[] = (String[]) vecLine.elementAt(j);
			 
			  
			  String pname = "";
			  
			  //add the values
			  if( e.length > 0 )
				  pname = e[0];
			  else
				  pname = "";
			  
			  
			  String pvalue = "";
			  
			  if(e.length > 1)
				  pvalue = e[1];   //line of the exception
			  else
				  pvalue = "";
			  
			  //create an element with the parameter names and values
			  elToAdd.setParameterName(pname);
			  elToAdd.setParameterValues(pvalue); 
			  
			  //add the element to the vector
			  lineWithNewElements.add(elToAdd);
			  
			  //verify if there are other parameters with the same name 
			  for(int k=j+1 ; k< vecLine.size()  ; k++){
				  
				  String elNext[] = (String[]) vecLine.elementAt(k);
				  
				  String pNextName = elNext[0];
				  
				  //if the value is already in the vector
				  if( pNextName.equals( pname ) == true  ){
					  
					  element elToAddNext = new element();
					  String vNextName = elNext[1];
					  
					  //set the values to the element added
					  elToAdd.setParameterValues(vNextName); 
					  
					  //add the element to the vector 
					  lineWithNewElements.add(elToAdd);
					  
					  //remove the next elements form the vector so that the elements will not be taken again
					  vecLine.remove(k);
					  
				  }//close if
					  
				  
				  
			  }//close for on k 
			  
			  
			}//close for on j 
			
		//	 vectorWithUniqueParameterName.add(lineWithNewElements);
			
	//	}
		
		
		
		
		return lineWithNewElements;
		
	}
	
	/*
	 * Extracts the content of a page by separating the links in the page 
	 * Vector vectorWithLinksPage - vector with the links of the page 
	 * page - the page from where the links are extracted 
	 * returns a string containing the content of the page 
	 * 
	 * */
	public static String extractContentOfthePage(Vector vectorWithLinksPage,String page){
	
		String content = page;
		
		for(int i=0; i < vectorWithLinksPage.size() ;i++){
		
			String regex = (String)vectorWithLinksPage.elementAt(i);
			
			//int valoare = content.indexOf(regex);
			
			content = content.replace(  regex , "" );
		
		}
		
		return content;
		
	}
	
	
	
	/*
	 * compares the content of the pages outside the links in the pages 
	 * if the contents of the pages are the same then 
	 *  
	 * */
	public static Vector extractParametersAndValues(Vector vectorWithLinksPage1  ){
		
		boolean isEq = true;
		
		//for the first page	
		Vector newVectorWithLinksPage1 = new Vector();	
			
		//5. From the links extract the substring until  "?"
		
		//for the first page
		for(int i=0 ; i<vectorWithLinksPage1.size() ; i++ ){	
			
			String linkPage1 = (String) vectorWithLinksPage1.elementAt(i);
			
			int beginIndex = linkPage1.indexOf("?");
		
			String link1 = linkPage1.substring(beginIndex+1);
			
			newVectorWithLinksPage1.add(link1);
		
		}
		
		//6.make the transformation          &amp; -> &
		
		String p = new String();
		String replaceStringPattern = "(?i)&amp;";
		Vector linksAfterTransformation = new Vector();
		//for the first page
		for(int i=0 ; i<newVectorWithLinksPage1.size() ; i++ ){
			
			p = (String) newVectorWithLinksPage1.elementAt(i);
			p = p.replaceAll(replaceStringPattern, "&" );
			linksAfterTransformation.add(p);
		
		}
		
		
			Vector vectorWithLinksAfterSplit = new Vector();
		
			
        //7. split the string over    "&" signs
			
		//for the first page 	
		for(int i=0 ; i < linksAfterTransformation.size() ; i++ ){
		
			p = (String) linksAfterTransformation.elementAt(i);
			
			String[] sir = p.split( "&");
			
			vectorWithLinksAfterSplit.add(sir);
    	
		}
		
		//8. Split on the  "=" sign the substrings
		
		//for the first page
		Vector vectorWithParametersAndValues = new Vector();
		
		
		for(int i=0 ; i < vectorWithLinksAfterSplit.size() ; i++ ){
		
			
			Vector parAndValLine = new Vector();
			
			//extract the vector of strings at each index
			String[] sir1 = (String[]) vectorWithLinksAfterSplit.elementAt(i);
			
			//iterate on the vectors with strings that resulted from the split
			for(int j=0 ; j < sir1.length ; j++){
					
				p = sir1[j];
				//split the strings in the vector after  "="  sign 
				String[] sirParAndValue = p.split( "=",2);
				
				parAndValLine.add(sirParAndValue);
				
			}
			
			//remove the elements with the name ReturnUrl
			for(int k=0;k<parAndValLine.size();k++){
				
				String[] comp = (String[] )parAndValLine.elementAt(k);
				
				if( comp[0].equals("ReturnUrl") == true  )
					parAndValLine.remove(k);
			}
			
			vectorWithParametersAndValues.add(parAndValLine);
		
		}
		
		return vectorWithParametersAndValues;
		
	}
	
	
	public static ComparisonResult  resultCompare(String searchPageForComparison , String HTMLcompare ){
	/*compares the strings response1 and response2 
	 * response1 represents the response of the server in the automatic test search 
	 * response2 represents the response of the server that was stored from the previous search
	 * returns false if the two pages are not equal
	 * - this method may return an object LineOfSearch
	 * */
		
		boolean testVar = true;
		
	    ComparisonResult cr = new ComparisonResult(searchPageForComparison , HTMLcompare );
		
	    cr.setLinksContentEqualFalse(testVar);
	    cr.setPageContentEqual(testVar);
	    cr.setPageContentEqual(testVar);
	    
		//1. test if the number of characters is equal 
		// if the number of caracters is equal continue , else return false
		int lengthSearchPageForComparison = searchPageForComparison.length();
		int lengthSearchPage = HTMLcompare.length();
		
		int dif = lengthSearchPageForComparison - lengthSearchPage;
		
		if( dif == 6 )
		{
			int indexDif = 0;
    		int i = 0;
    		for( i=0 ; i<lengthSearchPageForComparison ; i++){
    			if(  searchPageForComparison.charAt(i) != HTMLcompare.charAt(i) )
    				{      
    					indexDif = i;
    					break;
    				}
    			}
    		
    		String subStr = searchPageForComparison.substring(i, i+6);
    		
    		if( subStr.equals("amp%3B" ) == true ){
    		    searchPageForComparison = searchPageForComparison.replaceFirst( "amp%3B" , "");
    		    lengthSearchPageForComparison = searchPageForComparison.length();
    			lengthSearchPage = HTMLcompare.length();
    		}
		}
		
		
		
	//	if( lengthSearchPageForComparison == lengthSearchPage )
		{
			
		//2. Extract the links from the page 
			//Vector vectorWithLinksPage1 = resultCompare.extractAllTheLinksFromThePage(searchPageForComparison);
			Vector vectorWithLinksPage1 =  LinkProcessing.extractPattern1(searchPageForComparison) ;
		
			//Vector vectorWithLinksPage2 = resultCompare.extractAllTheLinksFromThePage(HTMLcompare);
			Vector vectorWithLinksPage2 =  LinkProcessing.extractPattern1(HTMLcompare) ;
			
			
		//3. Extract the substrings that represent the pages
		
		String pageContent1 = resultCompare.extractContentOfthePage( vectorWithLinksPage1 , searchPageForComparison );
		
		String pageContent2 = resultCompare.extractContentOfthePage( vectorWithLinksPage2 , HTMLcompare );
		
		//4. Compare the substrings and if the substrings resulted from the extraction of the strings representing the 
		//    links are not equal then the strings and the pages are not the same , return with boolean false value
		//    if the strings remained are equal then the links should be compared , continue
		int lengthContent1 = pageContent1.length();
		int lengthContent2 = pageContent2.length();
		
		if( lengthContent1 == lengthContent2 ){
			//process the last pages by extracting the form parameters
			pageContent1 = processContent(pageContent1);
			pageContent2 = processContent(pageContent2);
		
	String x = "";
		
		if( pageContent1.equals(pageContent2) == false ){
			
			int indexD = 0;
    		int i = 0;
    		for( i=0 ; i< pageContent2.length() ; i++){
    			if(  pageContent1.charAt(i) != pageContent2.charAt(i) )
    				{      
    					indexD = i;
    			
    				}
    			}
		//if the contents of the two pages are not equal 	
			//return false;
    		cr.setPageContentEqual(testVar);
    		cr.setPagesEqualFalse(testVar);
    		
    		//return testVar;
    		return cr; 
		}
		
		else
		{
		Vector vParVal = new Vector();
		
		Vector vParVal1 =  extractParametersAndValues(vectorWithLinksPage1  );
			
			//processLinks( vectorWithLinksPage1 ); 
		Vector vParVal2 = extractParametersAndValues( vectorWithLinksPage1  );
			
			//processLinks( vectorWithLinksPage2 );
			
		int noLinksV1 = vParVal1.size();
		int noLinksV2 = vParVal2.size();
		
		if( noLinksV1 != noLinksV2 ){
			//return false;
			testVar = false;
			cr.setPageContentEqual(testVar);
			cr.setLinksContentEqualFalse(testVar);
			//return testVar;
			return cr; 
		}
		else
		{
		
		for(int i = 0 ; i < noLinksV1 ; i++ ){
			
			Vector line1 = new Vector();
			Vector line2 = new Vector();
			
		    line1 = (Vector) vParVal1.elementAt(i);
		    line2 = (Vector) vParVal2.elementAt(i);
			
		    Vector rez1 = new Vector();
		    Vector rez2 = new Vector();
		    
		    rez1 = processLinks(  line1 );
		    rez2 = processLinks(  line2 );
			
		    
			
		    //iterate throught the elements of the Vector with data from the first page
		    //extract at every iteration a parameter and its list of values and determine
		    //if the parameter is present in the seccond page and if it is present 
		    //determine if the list of parameters are equal 
		    
		    boolean flag = true;
		    boolean flagEnter = false;
		    
		    for(int k = 0; k < (rez1.size()) && (flag == true) ;k++){
		    	
		    	 element comp1 = new element();
		    	 
		    	 //extract the first element
		    	 comp1 = (element)rez1.elementAt(k);
		    	 
		    	 //extract the name of the parameter
		    	 String par1 = comp1.getParameterName();
		    	 

		    	 flagEnter = false;
		    	 flag = false;
		    	 
		    	 //determine the position of the parameter in the seccond Vector
		    	 for(int j=0; j < rez2.size() ; j++ ){
		    		 //extract one element from the vector
		    		 element comp2 = (element)rez2.elementAt(j);
		    		 
		    		 //extract the name of the parameter
		    		 String par2 = comp2.getParameterName();
		    		 
		    		 //if the parameters of the element of the first page is equal with the parameter from the element of the seccond page
		    		 if( par1.equals(par2) == true  ){
		    			 
		    			 //there is a parameter with the same name for the given parameter
		    			 flagEnter = true;
		    			 
		    			 //set the flag true for entry in the loop 
		    			 flag = true;
		    			 
		    			 //compare the elements 
		    			 
		    			 //extract the vector from the parameter coresponding to the first page
		    			 		    	 
		    			 //extract the values of the parameters
		    			 Vector page1ParamValues =   comp1.getParameterValues(); 
		    			 
		    			 //extract the vector from the parameter coresponding to the seccond page
		    			 Vector page2ParamValues =   comp2.getParameterValues(); 
		    			 
		    			 flag = resultCompare.compVectString(page1ParamValues,page2ParamValues);
		    			 
		    			 if( flag == false ){
		    				 testVar = false;
		    				 cr.setLinksContentEqualFalse(testVar);
		    				 cr.setPageContentEqual(testVar);
		    			 }
		    			 
		    			 break;
		    			 //set the flag false for not equal
		    			 //flag = false;
		    			 
		    		 }
		    		 
		    		 
		    		 
		    	 }
		    	 
		    	 if( flagEnter == false){//the parameter does not exist in the second vector
		    		 testVar = false;
		    		 cr.setLinksContentEqualFalse(flag);		
		    		 cr.setLinksContentEqualFalse(testVar);
		    	 }
		    }
		    
		   
		    
		}
		
		}
		
				}
			}
		else{
			
			 testVar = false;
    			
    		 cr.setPageContentEqual(testVar);
    		 cr.setPagesEqualFalse(testVar);
			
			return cr;
			
		}
		
		}
//		else //if the strings don't have the same length
//			testVar =  false;
		
	
		
		//return testVar;
		return cr; 
	}
	
	

}

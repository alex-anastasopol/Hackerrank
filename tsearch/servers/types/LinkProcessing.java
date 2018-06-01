

package ro.cst.tsearch.servers.types;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/*
 * Class used for the extraction of links from a html page . The page may be the page from official server .
 * The page may be later compared with a different page that will be retrieved from the server at a later 
 * date and then comapared to see if the server changed .
 * 
 * */

public class LinkProcessing {
	
	
	
	//extracts the link with the index given from the html file that is given as entry data as a string
	//returns the index of the link 
	public static String getLink(int index,String HTML){
		
		String x = (String) ( extractPattern1(HTML).elementAt(index) ) ;
		
	
	     if( ! x.equals("")  )
	    	 
				return x;
		else
			
			return "";
		
		 
	}
	
	
	//extracts the index of a given link from a file
	//function that takes a File as entry data in the form of a string and the link in the form of a string 
	//returns the idex of the link in the file 
	public static int getIndex( String HTML , String link ){
		
		return extractPattern(HTML).indexOf(link);
		
	}
	
	public static String transform( String input ){
		
		String p = "";
		String replaceStringPattern = "(?i)&amp;";
		//used to perform the transformation
		String proba = new String();  //auxiliary for string 
		String sir1[] = null  ;                 //stores the results after split on = and processing with string URLencoder
		int count = 0;                            //used as a counter
		String sir2[] = null  ;                //used for the strings resulted from splits 
		
		String StrConcat = new String() ; //sir in care se introduc sirurile care se concateneaza
		//aici ar trebui introdusa transformarea 
        //&amp -> &
        p = input.replaceAll(replaceStringPattern, "&" );
        p = p.replaceAll(" ","%20");        
        /*
        //split the string over    "&" signs
    	String[] sir = p.split( "&");
    	
    	//encode every substring 
    	for(int i=0;i<sir.length;i++){
    		
    		proba = null;
    		
    		//split over "=" sign
    		  sir2 = sir[i].split("=");
    		  
    		  if( sir2.length > 1 ){
    			
    		  
    		  for(int j=0;j<sir2.length;j++){
    			  
    			  try {
    				//encode one side of equal   
    			if( j == 0 )
					proba  = URLEncoder.encode( sir2[j] , "UTF-8" );
    			else{
    				proba += URLEncoder.encode( sir2[j] , "UTF-8" );
    				sir[i] = proba;
    			}
    			
					if( j == 0 )  proba  += "=";
	    			 
				} catch (UnsupportedEncodingException e) {
						
						System.out.println("Encoding exception");
						
					e.printStackTrace();
				}

    		  }//inchis for
    		    
    		  }//if 
    		  else{
    			proba = sir2[0];
    			sir[i] = proba;
    			proba = null;
    		  }
    		  
    		  if(  i == 0 )
    			  p = sir[i];
    			  
    		  if( i > 0 ){
    			  p += "&";
    			  p += sir[i];   
    		  }
    		  
    	}//inchis for
    	*/
        
    	//String sir3[] = sir2;
    	
        return p;
		
	}
	
	
	/*
	 * Extracts the links from the html pages read as imput
	 * the imput String represents the page that is to be compared
	 * Returns a vector with the extracted links
	 * */
	public static Vector extractPattern( String HTML){
		
		String s = "";
		
		String regexString = "(?i)<[\\n\\t\\r]*a[^>]*href[\\n\\r\\t]*=[^\\\"]*\\\"([^\\\"]*)\\\"[^>]*>";
		//the regular expresion for the extraction of the links
		
		String subject = HTML;
		//contains the string
		
		Vector myVector = new Vector ();
		//the vector that will contain the links
		
		int step = 0;
		
		Pattern pat = Pattern.compile(regexString,Pattern.CASE_INSENSITIVE);
		//constructs the pattern object
		
		Matcher m = pat.matcher(subject);
		//match on the entry data and constructs the matching data
		
		while( m.find() == true ){//iterates through the pattern match by match
			
					s = 	m.group(1);
					//extracts in a string the first group 
			        //Retain the Link 
		            int pozitie = s.indexOf("Link=");
		            
		            if( pozitie > -1 ){
		            
		            	String LINK = s.substring(pozitie+5);
		            
		            	s = LINK;
		            
		            	String p = transform( s );
		            
		            	myVector.addElement( p );
					//adds an element to the vector
					
		            }
		}
		
		return myVector;
		//returns the vector with the extracted links
		
	}
	
	/*
	 * Extracts the links from the html pages read as imput
	 * the imput String represents the page that is to be compared
	 * Returns a vector with the extracted links
	 * */
	public static Vector extractPattern1( String HTML){
		
		String s = "";
		
		String regexString = "(?i)<[\\n\\t\\r]*a[^>]*href[\\n\\r\\t]*=[^\\\"]*\\\"([^\\\"]*)\\\"[^>]*>";
		//the regular expresion for the extraction of the links
		
		String subject = HTML;
		//contains the string
		
		Vector myVector = new Vector ();
		//the vector that will contain the links
		
		int step = 0;
		
		Pattern pat = Pattern.compile(regexString,Pattern.CASE_INSENSITIVE);
		//constructs the pattern object
		
		Matcher m = pat.matcher(subject);
		//match on the entry data and constructs the matching data
		
		while( m.find() == true ){//iterates through the pattern match by match
			
					s = 	m.group(1);
					//extracts in a string the first group 
					
	            int pozitie = s.indexOf("Link=");
		            
		            if( pozitie > -1 ){
					
			        //Retain the Link 
	
		            	//String p = transform( s );
		            
		            	myVector.addElement( s );
					//adds an element to the vector
		            	}
					
		            }
		
		
		return myVector;
		//returns the vector with the extracted links
		
	}
	
	

}

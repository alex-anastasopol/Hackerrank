
 package ro.cst.tsearch.wrapper2;

import java.io.*;
import java.util.StringTokenizer;

import org.apache.log4j.Category;

/** this class describes a pattern token;
 *  a patternToken includes my_token and 
 *  adds the matching criteria (EXACT,IGNORE_CASE,CLASS)
 ***/
public class patternToken implements Serializable
{
	protected static final Category logger= Category.getInstance(patternToken.class.getName());
	
	// Declare the 'magic number' for the class explicitly...
	   static final long serialVersionUID = 1004;
	   
	/** the token with all its attributes from HTML file **/
	public my_token token;
	/** matching type **/
	public int type;
	/** matching constant **/
	public static final int MATCH_EXACT=0;
	/** matching constant **/
	public static final int MATCH_IGNORE_CASE=1; 
	/** matching constant **/
	public static final int MATCH_TOKEN_CLASS=2;
	/** matching constant **/
	public static final int MATCH_TOKEN_ANY=3;
	/** default constructor **/
	public patternToken()
	{
		type=MATCH_EXACT;		
	}	
	/** constructor with type **/
	public patternToken(int t)
	{
		type=t;		
		if(t<MATCH_EXACT || t>MATCH_TOKEN_CLASS)
		{
	      //logger.info("Invalid match type - defualt MATCH_EXACT");
   	      type=MATCH_EXACT;
		}
	}	
	/** constructor with type & token**/
	public patternToken(int t,my_token m)
	{
		type=t;		
		if(t<MATCH_EXACT || t>MATCH_TOKEN_ANY)
		{
	      //logger.info("Invalid match type - defualt MATCH_EXACT");
   	      type=MATCH_EXACT;
		}
		token=m;
	}	
	/** get type of match **/
	public int getMatchType()
	{
		return type;
	}
	/** set type of match **/	
	public void setMatchType(int t)
	{
	   type=t;				   	      	
	}
	/** inc match type **/
	public boolean nextMatchType()
	{
		if(type<=MATCH_TOKEN_CLASS)
		    {
		    	type++;
		    	return true;
		    }
		return false;    
	}
	/** get token **/
	public my_token getToken()
	{
		return token;
	}
	/** set token **/	
	public void setToken(my_token m)
	{
	   token=m;
	}
	/** test match with other token **/
	public boolean match(my_token other)
	{
		//boolean rez;
		
		//logger.info("Matching : "+token.tk+" , "+other.tk);
		switch(type)
		{
			case MATCH_EXACT: if(token.cod==other.cod &&
			                     token.tk.equals(other.tk))
			                       return true;
			                  break;       
			case MATCH_IGNORE_CASE: if(token.cod==other.cod &&
			                           token.tk.equalsIgnoreCase(other.tk))                  
			                        return true;
			                  break;
			case MATCH_TOKEN_CLASS: if(token.cod==other.cod) return true;			
			                  break;
			case MATCH_TOKEN_ANY: return true;			                                  
		}
		return false;
	}
	
	public String toString()
	{
		String out=new String();
		switch(type)
		{
			case MATCH_EXACT: out+="EXACT(";
			                  break;       
			case MATCH_IGNORE_CASE: out+="IGNORE_CASE(";			                        
			                  break;
			case MATCH_TOKEN_CLASS: 
			                        out+="CLASS(";			                        
			                  break;
			case MATCH_TOKEN_ANY: 
									out+="ANY(";			                        
							  break;                  
		}
		
		String rez=new String();
         
         switch(token.cod)
         {
             case my_token.TK_TAG:rez+="HTMLtag";break;
    /** 2- numeric  */
	case my_token.TK_NUMERIC:rez+="Numeric";break;
   /** 3- alfanumeric 
         must contain at least a letter and a digit
    */
        case my_token.TK_ALFANUMERIC: rez+="AlfaNumeric";break;
    /** 4- punctuation ( ,:;'" )*/   
	case my_token.TK_PUNCTUATION: rez+="Punctuation";break;
    /** 5- string - not used */
	case my_token.TK_STRING: rez+="String - this isn't used - produces confusion";break;
    /** 6- alfabetic- only letters */    
         case my_token.TK_ALFABETIC: rez+="Alfabetic";break;
    /** 7- anything - shouldn't appear in parsed tokens */    
             case my_token.TK_ANY: rez+="Anything - not used";break;
    /** 8- special; like:<pre> &gt; &nbsp; </pre> */    
             case my_token.TK_SPECIAL:rez+="Special";break;
         }//switch                  
         rez+="("+token.tk+")";
		
		
		
		//out+=token.toString()+")";
		out+=rez+")";
		return out;
	}
	
	
	private void writeObject(java.io.ObjectOutputStream out)
     throws IOException
     {
     	  out.writeObject(token);
          out.writeInt(type);    	            
          
     }
     
    private void readObject(java.io.ObjectInputStream in)
     throws IOException, ClassNotFoundException
     {
     	token=(my_token)in.readObject();
     	type=in.readInt();     	
     }
     
	/** extrage primukl token  (pana la  ; )**/
		   private String get_first_Token(String s)
		   {
			   StringTokenizer t=new StringTokenizer(s,";");
			   return t.nextToken();
		   }
     public void dumpInfo(PrintWriter pw) throws Exception
     {
     	//write token
     	token.dumpInfo(pw);
     	//write type
     	pw.println(type+"; pattern matching type");
     }
     
     public void readInfo(BufferedReader br) throws Exception
     {
     	//read token
     	token=new my_token();
     	token.readInfo(br);
     	//read type
     	setMatchType(Integer.parseInt(get_first_Token(br.readLine()))); 
		//logger.info("Reading PT match type"+type);
     }
}
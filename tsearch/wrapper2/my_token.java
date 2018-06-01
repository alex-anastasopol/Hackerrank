
/*
 * author: 
 * 
 */
package ro.cst.tsearch.wrapper2;

import java.io.*;
import java.util.StringTokenizer;
import org.apache.log4j.Category;

/**this class contains information
about each token in a HTML page*/

public class my_token implements Serializable
{
	protected static final Category logger= Category.getInstance(my_token.class.getName());
	
	// Declare the 'magic number' for the class explicitly...
	   static final long serialVersionUID = 1005;
	   
    //constants
    //1- html tag
    //2- numeric
    //3- alfanumeric
    //4- punctuation
    //5- string - not used
    //6- alfabetic
    //7- anything - shouldn't appear in a results
    //8- special symbols (like '&npsp;','&gt;')
    
    /** 1- html tag */
	public static final int TK_TAG=1;
    /** 2- numeric  */
	public static final int TK_NUMERIC=2;
   /** 3- alfanumeric 
         must contain at least a letter and a digit
    */
	public static final int TK_ALFANUMERIC=3;
    /** 4- punctuation ( ,:;'" )*/   
	public static final int TK_PUNCTUATION=4; // ,:;'"...
    /** 5- string - not used */
	public static final int TK_STRING=5;//not used
    /** 6- alfabetic- only letters */    
	public static final int TK_ALFABETIC=6; //only letters
    /** 7- anything - shouldn't appear in parsed tokens */    
	public static final int TK_ANY=7; //anything
    /** 8- special; like:<pre> &gt; &nbsp; </pre> */    
	public static final int TK_SPECIAL=8; // &nbsp;...

    /** the token */
	public String tk;//the token
    /** the code is one of the constants defined in this class */    
	public int cod;//numeric code - is one of the constants above
    /** description: "Numeric","Punctuation","Alfanumeric" ...*/
	public String des;//Description:Numeric, Alfanumeric,...
    /** the index of the token in the
	HTML source (needed in the instructor class)    */
	public long idx_start;
	
	public  my_token()
	{
	}
        
        public my_token(String token, int type) {
            tk=token;
            cod=type;
        }
        
//the index of the token in the
	                      //HTML source (needed in the instructor class)
	                      
	                      
	private void writeObject(java.io.ObjectOutputStream out)
     throws IOException
     {
     	  out.writeObject(tk);
          out.writeInt(cod);    	  
          out.writeObject(des);    	  
          out.writeLong(idx_start); 
          
     }
    private void readObject(java.io.ObjectInputStream in)
     throws IOException, ClassNotFoundException
     {
     	tk=(String)in.readObject();
     	cod=in.readInt();
     	des=(String)in.readObject();
     	idx_start=in.readLong();
     }
     public String toString()
     {
         String rez=new String();
         
         switch(cod)
         {
             case TK_TAG:rez+="HTMLtag";break;
    /** 2- numeric  */
	case TK_NUMERIC:rez+="Numeric";break;
   /** 3- alfanumeric 
         must contain at least a letter and a digit
    */
        case TK_ALFANUMERIC: rez+="AlfaNumeric";break;
    /** 4- punctuation ( ,:;'" )*/   
	case TK_PUNCTUATION: rez+="Punctuation";break;
    /** 5- string - not used */
	case TK_STRING: rez+="String - this isn't used - produces confusion";break;
    /** 6- alfabetic- only letters */    
         case TK_ALFABETIC: rez+="Alfabetic";break;
    /** 7- anything - shouldn't appear in parsed tokens */    
             case TK_ANY: rez+="Anything - not used";break;
    /** 8- special; like:<pre> &gt; &nbsp; </pre> */    
             case TK_SPECIAL:rez+="Special";break;
         }//switch                  
         rez+="("+tk+")";
     //return rez;
     return tk;
     }  
	/** extrage primukl token  (pana la  ; )**/
		   private String get_first_Token(String s)
		   {
			   StringTokenizer t=new StringTokenizer(s,";");
			   return t.nextToken();
		   }
     public void dumpInfo(PrintWriter pw) throws Exception
     {
     	//write cod
     	//pw.println("Cod:"+cod);
		pw.println(cod+"; cod HTML token");
     	
     	//write des
     	//pw.println("Descriere :"+des);
		pw.println(des+"; Descriere HTML token");
     	
     	//write idx_start
     	//pw.println("IDx "+idx_start);
		pw.println(idx_start+"; index de start in fisierul original HTML - utilizat de instructor");
     	
     	//write tk  -- the token String
     	//pw.println("Token: "+tk);
		pw.println(tk);
     }
     
     public void readInfo(BufferedReader br) throws Exception
     {
     	
       //read cod
       cod=Integer.parseInt(get_first_Token(br.readLine()));
       //logger.info("Reading token cod "+cod);
	   //read des
     	des=get_first_Token(br.readLine());
     	
		//logger.info("Reading token des "+des);
	   //read idx_start
	   idx_start=(long)Integer.parseInt(get_first_Token(br.readLine()));
	   //logger.info("Reading token idx_start "+idx_start);
	   //read tk  -- the token String
	   tk=br.readLine();     	
	   //logger.info("Reading token tk "+tk);
     }                    
}

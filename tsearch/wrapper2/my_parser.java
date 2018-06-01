/*
 * author
 * 
 */
package ro.cst.tsearch.wrapper2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import org.apache.log4j.Category;
//this class parses HTML code
//a Vector object is available at the end
//of the parsing process, and can be obtained
//with the public function "getResult();"
//--this Vector is a vector of "my_token" objects


///////////////////////////////////////
//   Sensitive...under heavy work
////////////////////////////////////////



//the Constructors must have one parameter
//1. A String containing a valid path to a HTML file
//   or
//2. An InputStream object which provides HTML content
// after instantiating call the method parseEngine();

/**
 * this class parses HTML code;
   a Vector object is available at the end
   of the parsing process, and can be obtained
   with the public function "getResult();"
 *
 */
public class my_parser
{
	protected static final Category logger= Category.getInstance(my_parser.class.getName());
	
	//code for special html tags
    	  //1- href
    	  //2- form
    	  //3- input
    	  //4- image
    /** Special code for html "href" tag*/
    public static final int TAG_LINK=1; //href
    /** Special code for html "form" tag*/
    public static final int TAG_FORM=2; //web from
    /** Special code for html "input" tag*/
    public static final int TAG_INPUT=3; //input object    
    /** Special code for html "image" tag*/
    public static final int TAG_IMG=4; 
    


    /** this Buffered reader is used to access the text to be parsed */ 
    private BufferedReader br;//br(input stream)
    
    /** the result vector; contains objects of type ny_token
     *   @see my_token
     */
    public Vector vt;//vector tokens

    /** this constructor initializes the parser with input from a file 
     * @param s  file name
     */
    public my_parser(String s)
	{
		//logger.info("Constructor 1");
		try{
			br=new BufferedReader(new FileReader(s));
		}
		catch(IOException e)
		{
			logger.error("Cannot open file: "+s);
		}
		vt=new Vector();
	}
    /** this constructor initializes the parser with input from a InputStream 
     * @param s  InputStream
     * @see InputStream
     */
     public my_parser(InputStream is)
	{		
		br=new BufferedReader(new InputStreamReader(is));	
		vt=new Vector(10,1);
    }
    
    /** this method returns the result vector; it makes sense
     *  to call it after you called parseEngine();   
     *
     * @see #parseEngine()       
     */
    public Vector getResult()
    {
    	return vt;
    }
    
    /** detects if the character c is a punctuation sign
      *  using the ASCII code
      */
    private boolean isPunctuation(char c)
    {
    	int v;

    	v=(int)(c);

    	/*
    	 return (c=='<'||c=='>'||c==':'||c==';'||c=='.'||
    	         c==','||c=='"');
    	 */
    	 return (v>=33 && v<=47) || (v>=58 && v<=64 ) ||
    	        (v>=91 && v<=96) || (v>=123 && v<=126);
    }

    /** adds a token to the Result vector
     * @param s  the token
     * @param dim  length of the token
     * @param cod  the code (one of the constants defined in my_token class)
     * @param i_start  start index = the start position relative to the begining
     *   of the InputStream
     *
     * @see my_token
     */
    private void add_token(char s[],int dim,int cod,int i_start)
    {
    	 my_token mtk=new my_token();
    	 mtk.tk=new String(s,0,dim);
    	 mtk.cod=cod;
    	 mtk.idx_start=i_start;
    	//1- html tag
    	//2- numeric
    	//3- alfanumeric
    	//4- punctuation
    	//5- string (sir intre "") - nefolosit
    	//6- alfabetic (sir de litere)
    	//7- anything
    	//8- simbol special: &nbsp; &lt; ...
    	 switch(cod)
    	 {
    	 	case my_token.TK_TAG:mtk.des=new String("HTMLtag");
    	 	       break;
    	 	case my_token.TK_NUMERIC:mtk.des=new String("Numeric");
    	 	       break;
    	 	case my_token.TK_ALFANUMERIC:mtk.des=new String("Alfanumeric");
    	 	       break;
    	 	case my_token.TK_PUNCTUATION:mtk.des=new String("Punctuation");
    	 	       break;
    	 	case my_token.TK_STRING:mtk.des=new String("String");
    	 	       break;
    	 	case my_token.TK_ALFABETIC:mtk.des=new String("Alfabetic");
    	 	       break;
    	 	case my_token.TK_ANY:mtk.des=new String("Anything");
    	 	       break;
    	 	case my_token.TK_SPECIAL:mtk.des=new String("Simbol special");
    	 	       break;
    	 }
    	 if((vt.add(mtk))==false)
    	 {
    	 	//shouldn't happen
    	 	logger.error("Ooops Vector: add_token");
    	 }
    }


    /** parses a HTML tag if it is one of:
      *  1. hyperlink -> HREF
      *  2. form      -> ACTION
      *  3. input     -> VALUE
     * @param s - the HTML tag
     * @param cd - the code (1,2 or 3)
     * @param i_start - start index in the original stream
     *
     * @see #parseEngine()     
     */
    private int parse_httag(String s,int cd,int i_start)
    {
    	  int idx;
    	  char vlinie[],crttk[];
          int i,ltk;//i- counter s
    	          //ltk - length token
          int state;

          int my_i_start;

    	 idx=0;

    	 my_i_start=i_start;
    	//check cod
    	  //1- href
    	  //2- form
    	  //3- input
    	      	  
    	  String tmp=new String(s.toUpperCase());
    	  
    	 switch(cd)
    	 {
    	 	case TAG_LINK: idx=tmp.indexOf("HREF");    	 	
    	 	       idx+=4;//+length 'href'
    	 	   break;
    	 	case TAG_FORM: idx=tmp.indexOf("ACTION");
    	 	       idx+=6;//+length 'ACTION'
    	 	   break;
    	 	case TAG_INPUT: idx=tmp.indexOf("VALUE");
    	 	       idx+=5;//+length 'VALUE'
    	 	   break;
    	    case TAG_IMG: idx=tmp.indexOf("SRC");
    	 	       idx+=3;//+length 'SRC'
    	 	   break;  
    	 }    	 
    	 //extracts as a single HTML tag token the text
    	 //from the begining of the tag to the value
    	 //needed to be parsed
    	 //this means:
    	 // for form:
    	 // <form method='POST' action="script.php">
    	 // ^--------TK_TAG----------^

    	 vlinie=new char[s.length()];
    	 crttk=new char[s.length()];
    	 s.getChars(0,idx,crttk,0);
    	 add_token(crttk,idx,my_token.TK_TAG,my_i_start);
    	 //parsing the rest of the tag
    	 // = -> TK_PUNCTUATION
    	 // " -> TK_PUNCTUATION
    	 // script -> TK_ALFABETIC
    	 //.......
    	 ltk=0;
    	 i=idx;
    	 my_i_start+=idx;
    	 s.getChars(0,s.length(),vlinie,0);
    	 state=0;
    	 while(i<s.length())
    	 {

    	 	//start parse httag
    	 	if(state==0) //tests all posible token types
    			{
    			   ltk=0;
    			   if(Character.isDigit(vlinie[i]))
    			   {
    			   	 state=my_token.TK_NUMERIC; //numeric
    			   	 //i--;//pushback
    			   }
    			   else
    			   if(Character.isLetter(vlinie[i]))
    			   {
    			   	 state=my_token.TK_ALFABETIC;//alfa
    			   	 //i--;//pushback
    			   }
    			   else
    			   if(vlinie[i]=='&')//special-simbol
    			   {
    			   	 state=my_token.TK_SPECIAL;
    			   	 //i--;
    			   }
    			   else
    			   if(isPunctuation(vlinie[i]))
    			      {
    			        state=my_token.TK_PUNCTUATION;//punctuation
    			       // i--;
    			      }
    			   else
    			   if(Character.isWhitespace(vlinie[i]))
    			   {
    			   	  ;//ignore
    			   	  my_i_start+=1;
    			   }
    			   else //anything
    			   {
    			     state=my_token.TK_ANY;
    			     //i--;
    			   }
    		     }
    		     
    		     //disabled pushback in state 0 => speed
    		     //else
    		     
    		     if(state==my_token.TK_NUMERIC)//numeric
    		     {
    		     	 if(Character.isDigit(vlinie[i]))
    		     	 {
    		     	 	   crttk[ltk++]=vlinie[i];
    		     	 }
    			      else //alfanumeric
    			      if(Character.isLetter(vlinie[i]))
    			      {
    			      	state=my_token.TK_ALFANUMERIC;
    			      	i--;//pushback
    			      }
    			      else
    			      {
    			      	add_token(crttk,ltk,state,my_i_start);
    			      	my_i_start+=ltk;
    			      	i--;

    			      	state=0;
    			      }
    		      }
    		      else
    		      if(state==my_token.TK_ALFANUMERIC)
    		      {
    		      	if(Character.isLetterOrDigit(vlinie[i]))
    		      	{
    		      		crttk[ltk++]=vlinie[i];
    		      	}
    		      	else
    		      	{
    			      	add_token(crttk,ltk,state,my_i_start);
    			      	my_i_start+=ltk;
    		      		i--;//pushback;
    		      		state=0;
    		      	}
    		      }
    		      else
    		      if(state==my_token.TK_PUNCTUATION)//punctuation
    		      {
    		      	 crttk[ltk++]=vlinie[i];
    		      	 add_token(crttk,ltk,state,my_i_start);
    		      	 my_i_start+=ltk;
    		      	 state=0;
    		      }
    		      else
    		      if(state==my_token.TK_ALFABETIC)
    		      {
    		      	if(Character.isLetter(vlinie[i]))
    		     	 {
    		     	 	   crttk[ltk++]=vlinie[i];
    		     	 }
    		     	else //alfanumeric
    			     if(Character.isDigit(vlinie[i]))
    			      {
    			      	state=my_token.TK_ALFANUMERIC;
    			      	i--;//pushback
    			      }
    			    else
    			    {
    			    	add_token(crttk,ltk,state,my_i_start);
    			    	my_i_start+=ltk;
    			      	i--;
    			      	state=0;
    			    }

    		      }
    		      else
    		      if(state==my_token.TK_ANY)
    		      {
    		      	 crttk[ltk++]=vlinie[i];
    		      	 add_token(crttk,ltk,state,my_i_start);
    		      	 my_i_start+=ltk;
    		      	 state=0;
    		      }
    		      else
    		      if(state==my_token.TK_SPECIAL)
    		      {
    		      	 if(vlinie[i]=='&')
    		      	 {
    		      	   if(Character.isLetter(vlinie[i+1]))
    		      	    crttk[ltk++]=vlinie[i];
    		      	   else
    		      	   {
    		      	   	i--;
    		      	   	state=my_token.TK_PUNCTUATION;//punctuation
    		      	   }
    		      	 }
    		      	 else
    		      	 if(Character.isLetter(vlinie[i]))//adauga litere
    		      	 {
    		      	 	crttk[ltk++]=vlinie[i];
    		      	 }
    		      	 else //test ; terminator
    		      	 if(vlinie[i]==';')
    		      	 {
    		      	 	crttk[ltk++]=vlinie[i];
    		      	 	add_token(crttk,ltk,state,my_i_start);
    		      	 	my_i_start+=ltk;
    		      	 	state=0;
    		      	 }
    		      	 else
    		      	 {
    		      	   	 i--;//pushback
    		      	   	 state=my_token.TK_ANY;
    		      	 }
    		      }

    	 	//end httag
    	 	i++;
    	 }

    	 return my_i_start;
    }


    /** this function parses the HTML content;
     *  it uses the buffered reader for input;
     *  the input is scanned by lines (readline);
     *  The parsing algorithm is based on an ASM 
     *  (Automatic State Machine): 8 possibile states+
     *  1 initial state. Each character in the stream can
     *  either take the ASM into a differnt state (a token has been
     * identified and added to the result Vector) or maintain it in the
     * same state (the token is beeing parsed).
     *  The initial state is '0'. The aother states are coded accordingly to the
     * constants in my_token class (each token type has its own state).
     *  When a html tag is reached the function 'parse_httag' is called if necessary.It applies
     * the same algorithm.
     *     
     *  @see #parse_httag
     *  @see my_token
     */
    public void parseEngine()
    {
    	String linie;//input line
    	//char linie[]=new char[10];
    	char vlinie[],crttk[];//line to char array
    	                      // current token
    	String httag;
    	int i,ltk;//i- counter for line
    	          //ltk - length token
    	int cd;//code for html tag
    	       //1- href
    	       //2- input
    	       //3- form
    	int state; //machine state
        int BEGIN_COMMENT=1001;

    	int my_i_start;


    	crttk=new char[10000];
    	state=0;//initial
    	//1- html tag
    	//2- numeric
    	//3- alfanumeric
    	//4- punctuation
    	//5- string
    	//6- alfabetic
    	//7- anything
    	//8- special symbol  '&nbsp;'
    	try
    	{
    		ltk=0;
    		int limit;
    		my_i_start=0;
    		while((linie=br.readLine())!=null)
    	//	while((br.read(linie,0,4))!=-1)
    		{
    			limit=linie.length();
    	//		limit=linie.length;
    		vlinie=new char[limit];
    		linie.getChars(0,limit,vlinie,0);
    		//vlinie=linie;
    		i=0;
    		while(i<limit)
    		{

    			if(state==0) //test all possible tokens
    			{
    			   ltk=0;
    			   /*
    			   if(vlinie.length>=3 && i<=vlinie.length-3)
    			   if(vlinie[i]=='<' && vlinie[i+1]=='!' && vlinie[i+2]=='-' && vlinie[i+3]=='-')  //skip comment
    			   {
    			   	  logger.info("Begin comment");
    			   	   state=BEGIN_COMMENT;
    			   }
    			   else    			 
    			   */  
    			   if(vlinie[i]=='<')    			   
    			   {
    			   	state=my_token.TK_TAG; //html tag    			
    			   }    			     
    			   else
    			   if(Character.isDigit(vlinie[i]))
    			   {
    			   	 state=my_token.TK_NUMERIC; //numeric    			  
    			   }
    			   else
    			   if(Character.isLetter(vlinie[i]))
    			   {
    			   	 state=my_token.TK_ALFABETIC;//alfa    			   	
    			   }
    			   else
    			   if(vlinie[i]=='&')//posibil special-simbol
    			   {//de forma "&amp;"
    			   	 state=my_token.TK_SPECIAL;    			   	 
    			   }
    			   else
    			   if(isPunctuation(vlinie[i]))
    			      {
    			        state=my_token.TK_PUNCTUATION;//punctuation    			        
    			      }
    			   else
    			   if(Character.isWhitespace(vlinie[i]))
    			   {
    			   	  ;//ignore
    			   	  my_i_start++;
    			   }
    			   else //anything
    			   {
    			     state=my_token.TK_ANY;    			     
    			   }
    		     }
    		     
    		     
    		     //token states
    		     
    		     if(state==BEGIN_COMMENT)
    		     {
    		     //	logger.info("Skip comment");
    		     	if(vlinie.length>=3 && i<=vlinie.length-3)
    		     	if(vlinie[i]=='-' && vlinie[i+1]=='-' && vlinie[i+2]=='>')
    			      {
    			      	  i+=2;
    			      	  logger.info("End comment");
    			      	  state=0;
    			      	  my_i_start+=3;
    			      }
					my_i_start+=1;
    		     }
    		     else
    		     if(state==my_token.TK_TAG)//html tag
    		     {
    		     	  crttk[ltk++]=vlinie[i];
    		     	  if(vlinie[i]=='>')
    		     	  {
    		     	  	 //to_string
    		     	  	 httag=new String(crttk,0,ltk);
    		     	  	 //temp string to search for the 3 exceptions
    		     	  	 String tmp_str=new String(httag.toUpperCase());
    		     	  	 cd=0;
    		     	  	 //check href,input,form
    		     	  	 /*
    		     	  	 if(httag.indexOf("href")!=-1 || httag.indexOf("HREF")!=-1)
    		     	  	    cd=TAG_LINK;
    		     	  	 else
    		     	  	 if(httag.indexOf("action")!=-1 || httag.indexOf("ACTION")!=-1)
    		     	  	    cd=TAG_FORM;
    		     	  	 else
    		     	  	 if(httag.indexOf("value")!=-1 || httag.indexOf("VALUE")!=-1)
    		     	  	    cd=TAG_INPUT;
    		     	  	  */
    		     	  	 if(tmp_str.indexOf("HREF")!=-1)
    		     	  	    cd=TAG_LINK;
    		     	  	 else
    		     	  	 if(tmp_str.indexOf("ACTION")!=-1)
    		     	  	    cd=TAG_FORM;
    		     	  	 else
    		     	  	 if(tmp_str.indexOf("VALUE")!=-1)
    		     	  	    cd=TAG_INPUT;
    		     	  	 else
    		     	  	 if(tmp_str.indexOf("SRC")!=-1)
    		     	  	    cd=TAG_IMG;   
    		     	  	    
    		     	  	 //if the tag is one of the above
    		     	  	 //...hyperlink, form, input
    		     	  	 if(cd>0)
    		     	  	 {
    		     	  	 	my_i_start=parse_httag(httag,cd,my_i_start);
    		     	  	 }
    		     	  	 else //otherwise
    		     	  	 {
    		     	  	   add_token(crttk,ltk,state,my_i_start);
    		     	  	   my_i_start+=ltk;
    		     	  	 }
    		     	     state=0;
    		     	  }
    		     }
    		     else
    		     if(state==my_token.TK_NUMERIC)//numeric
    		     {
    		     	 if(Character.isDigit(vlinie[i]))
    		     	 {
    		     	 	   crttk[ltk++]=vlinie[i];
    		     	 }
    			      else //alfanumeric
    			      if(Character.isLetter(vlinie[i]))
    			      {
    			      	state=my_token.TK_ALFANUMERIC;
    			      	i--;//pushback
    			      }
    			      else
    			      {
    			      	add_token(crttk,ltk,state,my_i_start);
    			      	my_i_start+=ltk;
    			      	i--;
    			      	state=0;
    			      }
    		      }
    		      else
    		      if(state==my_token.TK_ALFANUMERIC)//alfanumeric
    		      {
    		      	if(Character.isLetterOrDigit(vlinie[i]))
    		      	{
    		      		crttk[ltk++]=vlinie[i];
    		      	}
    		      	else
    		      	{
    			      	add_token(crttk,ltk,state,my_i_start);
    			      	my_i_start+=ltk;
    		      		i--;//pushback;
    		      		state=0;
    		      	}
    		      }
    		      else
    		      if(state==my_token.TK_PUNCTUATION)//punctuation
    		      {
    		      	 crttk[ltk++]=vlinie[i];
    		      	 add_token(crttk,ltk,state,my_i_start);
    		      	 my_i_start+=ltk;
    		      	 state=0;
    		      }
    		      else
    		      if(state==my_token.TK_ALFABETIC)
    		      {
    		      	if(Character.isLetter(vlinie[i]))
    		     	 {
    		     	 	   crttk[ltk++]=vlinie[i];
    		     	 }
    		     	else //alfanumeric
    			     if(Character.isDigit(vlinie[i]))
    			      {
    			      	state=my_token.TK_ALFANUMERIC;
    			      	i--;//pushback
    			      }
    			    else
    			    {
    			    	add_token(crttk,ltk,state,my_i_start);
    			    	my_i_start+=ltk;
    			      	i--;
    			      	state=0;

    			    }

    		      }
    		      else
    		      if(state==my_token.TK_ANY)
    		      {
    		      	 crttk[ltk++]=vlinie[i];
    		      	 add_token(crttk,ltk,state,my_i_start);
    		      	 my_i_start+=ltk;
    		      	 state=0;
    		      }
    		      else
    		      if(state==my_token.TK_SPECIAL)
    		      {
    		      	 if(vlinie[i]=='&')//este un &
    		      	 {
    		      	   if(Character.isLetter(vlinie[i+1]))
    		      	    crttk[ltk++]=vlinie[i];
    		      	   else
    		      	   {
    		      	   	i--;
    		      	   	state=my_token.TK_PUNCTUATION;//punctuation
    		      	   }
    		      	 }
    		      	 else
    		      	 if(Character.isLetter(vlinie[i]))//adauga litere
    		      	 {
    		      	 	crttk[ltk++]=vlinie[i];
    		      	 }
    		      	 else //test ; terminator
    		      	 if(vlinie[i]==';')
    		      	 {
    		      	 	crttk[ltk++]=vlinie[i];
    		      	 	add_token(crttk,ltk,state,my_i_start);
    		      	 	my_i_start+=ltk;
    		      	 	state=0;
    		      	 }
    		      	 else
    		      	 {
    		      	   	 i--;//pushback
    		      	   	 state=my_token.TK_ANY;
    		      	 }
    		      }
               //  logger.info("state "+state);
                 //logger.info("caract: "+new Character(vlinie[i]));                 
    		      i++;
    		 }

    		    		my_i_start+=1;//de la readline
    		    		//logger.info("my_i"+my_i_start);      		 
    		}

    	}
    	catch(Exception e){
    		logger.error("Ooops parse exception:"+e); 
    		}
    }        

    /*
    //for debug
    //prints a portion of a char array on the screen
    private void printtoken(char s[],int dim)
    {
    	int i;

    	for(i=0;i<dim;i++)
    	  logger.infot(s[i]);
    	logger.info(" ");
    }


    //debug
    //prints results (tokens) on the screen
    public void showResults()
    {
    	int i;
    	my_token mtk;
        
    	for(i=0;i<vt.size();i++)
    	{
    		mtk=(my_token)vt.get(i);

    		logger.info((i+1)+". "+mtk.des+":["+mtk.tk+"]");
    		//reconstructing the original page
    		//logger.info(mtk.tk+" ");
    	}
		logger.info("Au fost :"+vt.size()+" tokeni");
    }
    
    public static void main(String args[])
    {
    	 String name_html="d:\\ex\\ex2.htm";
		 String name_rules="d:\\ex\\ShelbyRO.txt";
		 Vector v;
    	 my_parser p=new my_parser(name_html);
    	logger.info("Start parsing..."); 
     p.parseEngine();
	 logger.info("End parsing...");
	 logger.info("getting results...");
	    v=p.getResult();
	 logger.info("end getting results...");
     try{
     	wrapper wp=new wrapper(name_html,name_rules);     	
     	//wp.LoadHTMLfile();
		wp.LoadRulesfile();
		logger.info("Start extraction");
     	wp.parse_html_vector(v);
     	logger.info("The results are:");
		logger.info(wp.out);     	
     	
     }catch(Exception e){
		logger.error("Oooops extraction "+e);
		e.printStackTrace();
     }
    }*/
}


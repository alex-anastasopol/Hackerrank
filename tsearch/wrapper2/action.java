/*
 * 
 *  author:  
 *
 */
package ro.cst.tsearch.wrapper2;

import java.util.*;
import java.io.*;

import org.apache.log4j.Category;

/** this implements action description
 *  a rule consists of a series of actions
 **/
public class action implements Serializable
{
	protected static final Category logger= Category.getInstance(action.class.getName());
	
	// Declare the 'magic number' for the class explicitly...
	   static final long serialVersionUID = 1003;
	   
	/** skips to the first occurence of the pattern **/
	public static final int ACT_SKIPTO=0;
	/** skips while tokens match pattern **/
	public static final int ACT_SKIPWHILE=1;			
	
	/** type can be 0/1 -- skip_to/skip_while **/
	public int type; 	
	/** pattern to match - a Vector of patternToken objects**/	
	public Vector pattern;
	/** Vectorized pattern to match (used for quick-search) **/	
	public String V_pattern;	
	/** default constructor **/
	public action ()
	{
		type=0; //skip to
		pattern=new Vector();		
	}
	/** type only constructor **/
	public action(int t)
	{
		setType(t);
		pattern=new Vector();
	}
	/** type & pattern  constructor **/
	public action(int t,Vector p)
	{
		pattern=p;
		setType(t);
	}
	/** set rule type **/
	public void setType(int t)
	{
		type=t;
	}
	/** get rule type **/
	public int getType()
	{
		return type;		
	}
	/** set rule pattern **/
	public void setPattern(Vector p)
	{
		pattern=p;
	}
	/** add rule pattern token **/
	public void addToken(patternToken pt)
	{
		pattern.add(pt);
	}
	/** add rule pattern token **/
	public void insertToken(patternToken pt)
	{
		pattern.insertElementAt(pt,0);
	}
	/** get rule pattern **/
	public Vector getPattern()
	{
		return pattern;
	}
	/** vectorize = builds a String with pattern token codes (1-8)**/
	public void vectorize()
	{
		int i;
		patternToken p;
		V_pattern=new String();
		for(i=0;i<pattern.size();i++) 
		{
			p=(patternToken)pattern.get(i);
			V_pattern+=new Integer(p.token.cod).toString();
		}
	}
	/** overriden to be able to print the action **/
	public String toString()
	{
		String out=new String();
		/* orginal
		if(type==ACT_SKIPTO) out+="SKIP_TO[";
		else
		    out+="SKIP_WHILE[";
		*/
		///////////////////modified
		switch(type)
		{
			case action_constants.ACT_SKIPTO: out+="SKIP_TO["; break;
		    case action_constants.ACT_SKIPWHILE: out+="SKIP_WHILE[";break;
		    case action_constants.ACT_SKIPUNTIL: out+="SKIP_UNTIL[";break;
			case action_constants.ACT_SKIPNR: out+="SKIP_NR[";break;
			case action_constants.ACT_SKIPNRFW: out+="SKIP_NRFW[";break;
		}
		
		////////////////////////////
		for(int j=0;j<pattern.size();j++)
		{
			out+=((patternToken)pattern.get(j)).toString()+" ";
		}
		out+="]";
		return out;
	}
	
	/** overriden to be able to write the action to an object stream **/
	 private void writeObject(java.io.ObjectOutputStream out)
     throws IOException
     {
     	  out.writeInt(type);
          out.writeObject(pattern);    	  
          out.writeObject(V_pattern);    	  
          
     }
     /** overriden to be able to read the action from an object stream **/
    private void readObject(java.io.ObjectInputStream in)
     throws IOException, ClassNotFoundException
     {
     	type=in.readInt();
     	pattern=(Vector)in.readObject();
     	V_pattern=(String)in.readObject();
     }
     
	/** extrage primukl token  (pana la  ; )**/
	   private String get_first_Token(String s)
	   {
		   StringTokenizer t=new StringTokenizer(s,";");
		   return t.nextToken();
	   }
	   
     public void dumpInfo(PrintWriter pw) throws Exception
     {
     	int i,n;
     	 //write type
     	// pw.println("PT type: "+type);
		pw.println(type+"; action Type");
     	 //write  pattern
     	        //pattern size
     	        n=pattern.size();
     	        //pw.println("Pattern size: "+n);
		pw.println(n+"; pattern length (nr. of tokens in pattern)");
//		pattern data
     	        for(i=0;i<n;i++)
     	          {
     	          	patternToken pt=(patternToken)pattern.get(i);
     	          	pt.dumpInfo(pw);
     	          }     	 
     	 //write V_pattern
     	  pw.println(V_pattern+"; coded Pattern ");
     }
     
	public void readInfo(BufferedReader br) throws Exception
		 {
		 	int  i,n;
     	     //read type
     	     setType(Integer.parseInt(get_first_Token(br.readLine())));
     	     //logger.info("Reading action type"+type);
     	     //read pattern
     	         //pattern size
     	         pattern=new Vector();
     	         n=Integer.parseInt(get_first_Token(br.readLine()));
			     //logger.info("Reading action pattern size"+n);     	              	         
     	         //pattern data
     	         for(i=0;i<n;i++)
     	            {
     	               patternToken pt=new patternToken();
     	               pt.readInfo(br);
     	               pattern.add(pt);
     	            }
     	     //read V_pattern
     	     V_pattern=get_first_Token(br.readLine());
			//logger.info("Reading action V_pattern "+V_pattern);
		 }
}
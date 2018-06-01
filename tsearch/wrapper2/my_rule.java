/*
 *   author: 
 * 
 * 
 */
package ro.cst.tsearch.wrapper2;

import org.apache.log4j.Category;

import java.util.*;
import java.io.*;
/** this class contains an extraction rule **/
public class my_rule implements Serializable,Cloneable{
	
	protected static final Category logger= Category.getInstance(my_rule.class.getName());
	// Declare the 'magic number' for the class explicitly...
	   static final long serialVersionUID = 1002;
	
	/** rule name **/
	public String name;
	/** rule type **/
	public int type;
	
	public static final int RULE_EXTRACT=0;
	public static final int RULE_ITERAT=1;
	
	/** rule actions **/
	public Vector actions;
	
	/** empty instance **/
	public my_rule()
	{
	  	actions=new Vector();
    }
     
    /** named rule, no actions **/ 
    public my_rule(String n)
	{
	  	actions=new Vector();
	  	name=new String(n);	  		  		  	
    }
    /** named rule, set of actions **/ 
    public my_rule(String n,Vector act)
	{
	  	actions=act;
	  	name=new String(n);	  		  		  	
    }
    /** get rule name**/
    public String getName()
    {
    	return name;
    }
    /** get rule name**/
    public void setName(String n)
    {
    	name=new String(n);
    }
    /** get type **/
    public int getType()
    {
    	return type;
    }
    /** set type **/
    public void setType(int t)
    {
    	type=t;
    }
    /** add action (for backward rules)**/
    public void addAction(action a)
    {
    	actions.add(a);
    }
    /** insert action (for forward rules)**/
    public void insertAction(action a)
    {
    	actions.insertElementAt(a,0);
    }
    /** set rule actions **/
    public void setActions(Vector v)
    {
    	actions=new Vector(v);
    }
    /** get rule actions **/
    public Vector getActions()
    {
    	return actions;
    }
    /** set action at idx **/
    public void setActionAt(action a,int idx)
    {
    	actions.setElementAt(a,idx);
    }
    /** get action at**/
    public action getActionAt(int idx)
    {
    	return (action) actions.get(idx);
    }
    /** toString **/
    public String toString()
    {
        String out=new String();
        if(type==RULE_EXTRACT) out+="EXTRACT ";
        else
            out+="ITERATE ";
        for(int i=0;i<actions.size();i++)
        {
        	out+=((action)actions.get(i)).toString()+" ";
        }    
        return out;
    } 
    
    /** overriden method to write this object to an output Stream **/
    
    private void writeObject(java.io.ObjectOutputStream out)
     throws IOException
     {
     	  out.writeObject(name);
          out.writeInt(type);    	  
          out.writeObject(actions);    	  
          
     }
    
    /** overriden method to read this object from a input Stream **/ 
    private void readObject(java.io.ObjectInputStream in)
     throws IOException, ClassNotFoundException
     {
     	name=(String)in.readObject();
     	type=in.readInt();
     	actions=(Vector)in.readObject();
     }
   
    public void removeAction(int idx) {
        actions.removeElementAt(idx);
    }    
    
    public void insertActionAt(int idx,action a) {
        actions.insertElementAt(a,idx);
    }
    
    /**overriden clone to get a nice copy of this object **/
    public Object clone()
    {
    	try{
    		ByteArrayOutputStream bout=new ByteArrayOutputStream();
    		ObjectOutputStream out=new ObjectOutputStream(bout);
    		out.writeObject(this);
    		out.close();
    		
    		ByteArrayInputStream bin=new ByteArrayInputStream(bout.toByteArray());
    		ObjectInputStream in=new ObjectInputStream(bin);
    		Object ret=in.readObject();
    		in.close();
    		
    		return ret;    		    		
    	}
    	catch(Exception e)
    	{
    		logger.error("Ooops clone my_rule");
    		return null;
    	}
    }
	/** extrage primukl token  (pana la  ; )**/
	   private String get_first_Token(String s)
	   {
		   StringTokenizer t=new StringTokenizer(s,";");
		   return t.nextToken();
	   }
    public void dumpInfo(PrintWriter pw) throws Exception
    {
    	int i;
         //print rule name
         //pw.println("Rule Name: "+getName());         
		  pw.println(getName()+"; Rule name");
         //print rule type
         //pw.println("Rule type: "+getType());
		   pw.println(getType()+"; Rule type");
                  
         //print actions
              //print vector size()
              //pw.println("ACtion size:"+actions.size());
		       pw.println(actions.size()+"; length of Actions vector");
              //print vector data
              for(i=0;i<actions.size();i++)
                 {
                 	action a=(action)actions.get(i);
                 	a.dumpInfo(pw);
                 }
    }
    public void readInfo(BufferedReader br) throws Exception
    {
    	int i,n;
		//read rule name
         setName(get_first_Token(br.readLine()));
         //logger.info("Read rule name:"+name);
		//read rule type
         setType(Integer.parseInt(get_first_Token(br.readLine())));
		//logger.info("Read rule type:"+name);
		//read actions
		     //read actions size
		     actions=new Vector();
		     n=Integer.parseInt(get_first_Token(br.readLine()));
		    //logger.info("Read rule action size:"+n);		     
		     //read actions data
		     action a;
		     for(i=0;i<n;i++)
		     {
				a=new action();
		     	a.readInfo(br);
				actions.add(a);
		     }
		      
    }
}
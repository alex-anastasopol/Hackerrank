/*
 * author:
 * 
 */
package ro.cst.tsearch.wrapper2;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.log4j.Category;

/** an object of this type is a node in the selection tree
 */
public class my_node extends DefaultMutableTreeNode
//implements Serializable 
{
	protected static final Category logger= Category.getInstance(my_node.class.getName());
	
	// Declare the 'magic number' for the class explicitly...
	   static final long serialVersionUID = 1001;
	   
       /** maximum profile number -- for N profiles set value to N+1*/
       public static final int MAX_PROF=5;
       
       /** A selection is described by two coords:start,stop;
        *  The coords are stored in a 2D array;
        *  This is the start index in array
        */
       public static final int tk_start=0;
       
       /** the stop index */
       public static final int tk_stop=1;
       
       /**The same label in all profiles*/
	public String label;
        
        /**each node has different coords in different profiles;
             Ex:
                cds[2][tk_start]=the start coordinate for profile 2                          
         */
	public int[][] cds;
	/** Rule type 'Extraction' - for unique information (name , address,..) 
	 *            'Iterative'  - when you need to extract table rows,table cells,
	 *                           lists, etc.
	 *
	 *   0 = Extraction
	 *   1 = Iteration
	 *	 
	 ***/
	
	public int rule_type;
	
	/** node type is used in wrapper to populate objects with extracted
	 * data -- see node_constants
	 */
	 public int node_type;
	 
	 /** node type is used in wrapper to populate objects with extracted
	 * data (category object and basic_fields)-- see node_constants
	 */
	 public int node_second_type;
	
	/**
	 *  this is the code for PIS,SDS,TDS ,....
	 */
	 public int node_category_type;
	 
	 /**
	  * this is the code for basic field
	  */
	  public int node_basic_field;
	
	 
	 /**
	  *  flag to enable a node
	  *  if a node is disabled the parser does not traverse it
	  *  (or its children if there are any)
	  * 
	  */
	 
       public boolean enabled=true; 
        
        
     /** 
      *  Rules to extract this node
      */   
     public my_rule rule_fwd;
     public my_rule rule_bkd;
     
     //public int total_rules;
        /**is this node marked in the current profile? */
        public boolean marked[];
        
		
        /** This constructor is intended to be used only for 
         *  generating nodes for the first file
         * @param l - text label.
         * @param start - start index of selection
         * @param stop - stop index of selection
         */
	public my_node(String l,int start,int stop)
	{
                int i;
        enabled=true;        
		label=new String(l);
		cds=new int[MAX_PROF][2];
                marked=new boolean[MAX_PROF];
                marked[1]=true;
                for(i=2;i<MAX_PROF;i++)
                    marked[i]=false;
                cds[1][tk_start]=start;
                cds[1][tk_stop]=stop;
	}
		
        /** This method sets the coordinates for the node
         *  in the specified profile;
         *  "start","stop" are indexes in a Vector of parsed tokens
         *  @param profile - profile number (1=first profile)
         *  @param start - start of selection
         *  @param stop - stop of selection
         */
        public void setCoords(int profile,int start,int stop)         
        {
            cds[profile][tk_start]=start;
            cds[profile][tk_stop]=stop;
            marked[profile]=true;
        }
        /*
         *This method sets marked flag to 'false'
         *for the specified profile
         *
         *@param profile  the profile number for which the markers
         *  are deleted (2,3,4)
         */
         public void unmark(int profile)
         {
         	marked[profile]=false;       
         	cds[profile][tk_start]=0;
            cds[profile][tk_stop]=0;  	
         }
         /**
          *Get start token index          
          */
          public int getStartCoord(int profile)
          {
          	return cds[profile][tk_start];          	
          }
          
         /**
          *Get stop token index          
          */
          public int getStopCoord(int profile)
          {
          	return cds[profile][tk_stop];          	
          }
          /**
           *  gets Forward rule
           **/
          public my_rule getForward()
          {
          	  return rule_fwd;
          } 
          /**
           *  sets Forward rule
           **/
          public void setForward(my_rule r)
          {
          	  rule_fwd=r;
          } 
          /**
           *  gets Backward rule
           **/
          public my_rule getBackward()
          {
          	  return rule_bkd;
          } 
          /**
           *  sets Backward rule
           **/
          public void setBackward(my_rule r)
          {
          	  rule_bkd=r;
          } 
          /**
           *  gets Label/Name
           **/
          public String getLabel()
          {
          	  return label;
          } 
          
         /** sets the rule type (Extraction - 0; Iteration -1) **/
         public void setType(int t)
         {
         	if(t==0 || t==1)
         	  rule_type=t;
         }
         /** gets the rule type (Extraction - 0; Iteration -1) **/
         public int getType()
         {
         	return rule_type;         	
         }
         
         /** sets the node type -- see node_constants **/
         public void setNodeType(int t)
         {         	
         	  node_type=t;
         }
         /** gets the node type -- see node_constants**/
         public int getNodeType()
         {
         	return node_type;         	
         }
         
         /** sets the node SECOND type -- see node_constants **/
         public void setNodeSecondType(int t)
         {         	
         	node_second_type=t;
         }
         /** gets the node SECOND type -- see node_constants**/
         public int getNodeSecondType()
         {
         	return node_second_type;         	
         }
         
         
         
        /** overriden method to display the right thing in the TreeNode Label
         */         
	public String toString()
	{
		return label;
	}	
	
	public void setLabel(String lb)
	{
	    label=lb;
	}
	/**
	 * @return
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @return
	 */
	public int getNode_basic_field() {
		return node_basic_field;
	}

	/**
	 * @return
	 */
	public int getNode_category_type() {
		return node_category_type;
	}

	/**
	 * @return
	 */
	public int getNode_type() {
		return node_type;
	}

	/**
	 * @param b
	 */
	public void setEnabled(boolean b) {
		enabled = b;
	}

	/**
	 * @param i
	 */
	public void setNode_basic_field(int i) {
		node_basic_field = i;
	}

	/**
	 * @param i
	 */
	public void setNode_category_type(int i) {
		node_category_type = i;
	}

	/**
	 * @param i
	 */
	public void setNode_type(int i) {
		node_type = i;
	}
	/*
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
    */
    public void dumpInfo(PrintWriter pw) throws Exception
    {
    	int i,j;
    	//write label
    	//pw.println("START--------------------"+getLabel());
    	//pw.println("node label: "+getLabel());
		pw.println(getLabel()+"; This is the node label");
    	//write enabled
    	//pw.println("enabled :"+enabled);
		pw.println(enabled+"; This boolean tells if node is accessible or not");
    	//write marked
    	     //vector length
    	     //pw.println("marked length: "+marked.length);
		      pw.println(marked.length+"; This is the length of a boolean vector used in instructor interface");
    	     //vector data
    	     for(i=0;i<marked.length;i++)
		         pw.println(marked[i]+"; vector data");    	    
    	//write selection matrix cds
    	//pw.println("Profile Matrix");
    	for(i=0;i<MAX_PROF;i++)    	    		
		    for(j=0;j<2;j++)
		    {
		    	pw.println(cds[i][j]+"; data that stores indexes used to train the parser ");
		    }    	     	
		//pw.println("End Matrix");    
    	//write basic_field
    	pw.println(node_basic_field+"; basic_field= this value maps an Attribute like OwnerName or Parcel ID");    	
    	//write category_type
		pw.println(node_category_type+"; category = this value maps a class of Attributes like Property Information Set" );    	
       //write node_second_type
        pw.println(node_second_type+"; value with internal semnification ");
    	//write node_type
    	pw.println(node_type+"; node_type ");    	
    	//write rule_type
		pw.println(rule_type+"; internal use only");
		
		
    	//dump rule_fwd
    	if(rule_fwd!=null)
    	{
    	   pw.println("FWD RULE PRESENT");	 
    	   rule_fwd.dumpInfo(pw);
    	}   
    	else
		     pw.println("FWD RULE IS NULL");
    	//dump rule_bkd
		if(rule_bkd!=null)
		{
	       pw.println("BKD RULE PRESENT");	
		   rule_bkd.dumpInfo(pw);
		}
		else
		pw.println("BKD RULE IS NULL");
    	
    }
    /** extrage primukl token  (pana la  ; )**/
    private String get_first_Token(String s)
    {
    	StringTokenizer t=new StringTokenizer(s,";");
    	return t.nextToken();
    }
    
    public void readInfo(BufferedReader br) throws Exception 
    {
    	int i,j;
    	//read label    	
    	label=get_first_Token((br.readLine()).trim());
		//logger.info("Reading Label..."+label);
    	
    	
//		read enabled
			  enabled=(new Boolean(get_first_Token(br.readLine()))).booleanValue();
		//logger.info("Reading enabled..."+enabled);  
	//read marked
				   //vector length
				   marked=new boolean[Integer.parseInt(get_first_Token(br.readLine()))];
		         //logger.info("Reading length..."+marked.length);				   
				   //vector data
				   for(i=0;i<marked.length;i++)
					   marked[i]=(new Boolean(get_first_Token(br.readLine()))).booleanValue();    	    
  //read selection matrix cds
			  for(i=0;i<MAX_PROF;i++)    	    		
				  for(j=0;j<2;j++)
				  {
					 cds[i][j]=Integer.parseInt(get_first_Token(br.readLine()));
				  }    	     	
  //read basic_field
			  node_basic_field=Integer.parseInt(get_first_Token(br.readLine()));    	
			  //read category_type
			  node_category_type=Integer.parseInt(get_first_Token(br.readLine()));    	
			 //read node_second_type
			  node_second_type=Integer.parseInt(get_first_Token(br.readLine()));
			  //read node_type
			  node_type=Integer.parseInt(get_first_Token(br.readLine()));    	
			  //read rule_type
			  rule_type=Integer.parseInt(get_first_Token(br.readLine()));
			  
			  
//		read rule_fwd
           String test=br.readLine();
  
		 if(test.equals("FWD RULE PRESENT"))
		 {	  
		 	rule_fwd=new my_rule();
			  rule_fwd.readInfo(br);
		 }	  
			
			test=br.readLine();
			  
			  //read rule_bkd
		if(test.equals("BKD RULE PRESENT"))
		{		  
			rule_bkd=new my_rule();
			  rule_bkd.readInfo(br);
		}	  	
    }
}
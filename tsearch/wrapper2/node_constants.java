package ro.cst.tsearch.wrapper2;

/*
 * node_constants.java
 *
 * Created on August 5, 2003, 11:48 AM
 * author:
 */


public class node_constants {
    
    public static final int ntype_extract = 0;
    
    public static final int ntype_iterat = 1;
    
    public static final int ntype_categ = 2;
    
    public static final int ntype_basic = 3;
    
    public static final int categ_GRANTOR = 1;
    
    public static final int categ_ADDRESS = 2;
    
    public static final int basic_FULLNAME = 1;
    
    public static final int basic_STREET = 2;
    
    /** Creates a new instance of node_constants */
    public node_constants() {
    }
    
    public static String getString(int type)
    {
    	
       switch(type)
       {
         case  categ_GRANTOR: return new String("GRANTOR");
         case categ_ADDRESS:return new String("Address");
         
        
         default: return null; 
       }      
    }
}

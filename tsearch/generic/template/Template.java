package ro.cst.tsearch.generic.template;


import java.util.*;

/**
 * Template interface
 */
public interface Template {

    
    /**
     * get the delimitator used
     */ 
    public  String getReplacementDelimiter(); 
    

    /**
     *  get replacements 
     */   
    public  Hashtable getReplacements() ; 


    /**
     * Replace the template fields
     * @param bodyFileName is the template file that contains the 
     *          tokens define in hashtable
     * @param outFileName is the output file name 
     */
    public void saveTemplateToFile(String inFileName, String outFileName) 
        throws Exception ;

    /**
     * Replace the template fields and return the content like a string
     * @param bodyFileName is the template file that contains the 
     *          tokens define in hashtable
     */
    public String getTemplateAfterReplacements(String inFileName) throws Exception ;

         
}        

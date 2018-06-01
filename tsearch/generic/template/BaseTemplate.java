
package ro.cst.tsearch.generic.template;

//application
import ro.cst.tsearch.generic.Util;

//java
import java.util.*;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.log4j.Category;


abstract class BaseTemplate implements Template {    
    /**
     * get the delimitator used
     */ 
    public  String getReplacementDelimiter() { return "@@"; };
	private static final Category logger = Category.getInstance(BaseTemplate.class.getName());
    
    public String getValueAsString(Object obj) {
        if (obj==null) {
            return "";
        }
        return String.valueOf(obj);
    }

    protected Hashtable replacements = new Hashtable();
    /**
     *  get replacements 
     */   
    public Hashtable getReplacements() {
        return replacements;
    }
    public void setReplacements(Hashtable replacements) {
        this.replacements = replacements;
    }

    public void saveTemplateToFile(String inFileName, String outFileName) 
        throws Exception {
            
        BufferedReader  in = null;
        PrintWriter     out = null;
        try {
            Vector v  = new Vector();
            Hashtable replacements = getReplacements();
			logger.debug("Debug: GenericTemplate#saveTemplateToFile bodyFileName = " + inFileName);
			logger.debug("Debug: GenericTemplate#saveTemplateToFile outFileName = " + outFileName);

            //read file to buffer         
            in = new BufferedReader(new FileReader(inFileName));
            String line = in.readLine();                            
            while(line != null) {
                //replace the given tags
                //and add the modified line to vector
                v.addElement(
                    Util.replaceTags(
                                        getReplacementDelimiter(),
                                        line,
                                        replacements)                
                );
                
                String last=(String)v.lastElement();
                
                if(last.indexOf("Search Updates")>0 || last.indexOf("Full Searches")>0){
                	
                	last = last.replaceAll("[0-9]*[ ]*x[ ]*US[$][ ]*[0]*[.][0]*[ ]*\\+?", "");
                	last = last.replaceAll("[>][0][ ]*x[ ]*US[$][ ]*[0-9]*[.][0-9]*[ ]*\\+?", ">"); //replaces 0 x smth; matches if preceded by ">"
                	last = last.replaceAll("[ ][0][ ]*x[ ]*US[$][ ]*[0-9]*[.][0-9]*[ ]*\\+?", " ");	//replaces 0 x smth; matches if preceded by " "
                	
                	
                }
                
                line = in.readLine();
            }
            //close the input
            in.close();
            //write new string to file
            out = new PrintWriter(new FileWriter(outFileName));
            for (int i=0; i< v.size(); i++) {
                if (i==v.size()-1) {
                    //if last line 
                    out.print((String) v.get(i));
                } else {
                    out.println((String) v.get(i));
                }
            } 
            //close the output
            out.close();
            
        } catch (Exception e) {
			logger.debug( e.toString());
            throw new Exception("GenericTemplate#saveTemplateToFile Error trying to save the file "
                                + outFileName 
                                + " using the template " 
                                + inFileName  );
        } finally {
            //close the file handles
            try {
                in.close();
            } catch (Exception e){}
            try {
                out.close();
            } catch (Exception e){}
        }        
    }



    public String getTemplateAfterReplacements(String inFileName) 
        throws Exception {
        BufferedReader  in      = null;
        StringBuffer    out     = null;
        try {
            Vector v  = new Vector();
            Hashtable replacements = getReplacements();

            //read file to buffer         
            in = new BufferedReader(new FileReader(inFileName));
            String line = in.readLine();                            
            while(line != null) {
                //replace the given tags
                //and add the modified line to vector
                v.addElement(
                    Util.replaceTags(
                                        getReplacementDelimiter(),
                                        line,
                                        replacements)                
                );
                line = in.readLine();
            }
            //close the input
            in.close();
            out = new StringBuffer("");
            //write new string to file
            for (int i=0; i< v.size(); i++) {
                if (i==v.size()-1) {
                    //if last line 
                    out.append((String) v.get(i));
                } else {
                    out.append((String) v.get(i) + "\n");
                }
            } 
            return out.toString();
        } catch (Exception e) {
			logger.debug(e.toString());
            throw new Exception("GenericTemplate#getTemplateAfterReplacements Error trying to save the headline"
                                + " using the template " 
                                + inFileName  );

        } finally {
            //close the file handles
            try {
                in.close();
            } catch (Exception e){}
        }        
    }

         
}        
